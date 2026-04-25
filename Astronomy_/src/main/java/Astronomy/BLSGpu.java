package Astronomy;

import static org.jocl.CL.CL_MEM_COPY_HOST_PTR;
import static org.jocl.CL.CL_MEM_READ_ONLY;
import static org.jocl.CL.CL_MEM_READ_WRITE;
import static org.jocl.CL.CL_MEM_WRITE_ONLY;
import static org.jocl.CL.CL_TRUE;
import static org.jocl.CL.clCreateBuffer;
import static org.jocl.CL.clCreateKernel;
import static org.jocl.CL.clEnqueueNDRangeKernel;
import static org.jocl.CL.clEnqueueReadBuffer;
import static org.jocl.CL.clFinish;
import static org.jocl.CL.clReleaseKernel;
import static org.jocl.CL.clReleaseMemObject;
import static org.jocl.CL.clSetKernelArg;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import ij.IJ;
import org.jocl.CL;
import org.jocl.Pointer;
import org.jocl.Sizeof;
import org.jocl.cl_kernel;
import org.jocl.cl_mem;

/**
 * GPU-accelerated Box Least Squares (BLS) periodogram using OpenCL / JOCL.
 *
 * <p><b>Algorithm — phase-binned BLS:</b>
 * <ol>
 *   <li>Each period thread folds all {@code nPoints} data points into
 *       {@code nBins} uniform phase bins — O(nPoints), one fmod per point.</li>
 *   <li>BLS is run on the binned representation using an O(1)-per-step sliding
 *       window — total O(nDurations × nBins) inner loop.</li>
 * </ol>
 * Per-thread work is independent of {@code nPoints} beyond the fold step, so
 * the kernel completes in &lt; 1 ms for typical datasets, never triggering
 * Windows' 2-second TDR watchdog.
 *
 * <p>The GPU receives the caller's arrays verbatim — no time normalisation
 * or flux mean-subtraction.  With the kernel now running entirely in FP64,
 * raw BJD values (~2.5e6) still carry ~11 digits of precision in the folded
 * phase, which is more than enough for 1000-bin resolution.  Keeping the
 * arithmetic identical to the CPU path guarantees bit-for-bit agreement of
 * the phase-fold binning, so CPU and GPU cannot land in different local
 * maxima of the (period, duration) surface due to rounding asymmetry.
 *
 * <p>The number of phase bins is compiled into the kernel as a preprocessor
 * constant.  Changing {@code nBins} triggers a recompile on first use; the
 * compiled program is cached per-(backend, nBins) pair for the lifetime of
 * the JVM.  The bin arrays themselves live in per-work-item slices of global
 * scratch memory rather than as {@code __private} stack arrays — at 1000 bins
 * the 12 KB-per-thread private footprint was too large for NVIDIA's OpenCL
 * compiler to spill reliably.
 */
public class BLSGpu {

    private static final String KERNEL_RESOURCE = "/Astronomy/bls_periodogram.cl";
    private static final String KERNEL_NAME     = "bls_compute";

    /** Holds the four per-period output arrays returned by {@link #search}. */
    public static final class BLSResult {
        public final double[] power;
        public final double[] bestDurations;
        public final double[] bestDepths;
        public final double[] bestPhases;

        BLSResult(double[] power, double[] bestDurations,
                  double[] bestDepths, double[] bestPhases) {
            this.power         = power;
            this.bestDurations = bestDurations;
            this.bestDepths    = bestDepths;
            this.bestPhases    = bestPhases;
        }
    }

    /**
     * Pre-compiles the BLS OpenCL kernel for {@code backend} with the given bin count.
     * Subsequent calls to {@link #search} with the same {@code nBins} reuse the cache.
     * Safe to call multiple times.
     */
    public static void warmUp(String backend, int nBins) {
        PeriodogramGpuContext.getOrCreateContext(backend,
                buildKernelSource(nBins), programCacheKey(nBins));
    }

    /**
     * Runs the phase-binned BLS grid search on the GPU.
     *
     * @param t          Time array (days, sorted ascending, any size)
     * @param f          Flux array (normalised to ~1.0)
     * @param periods    Pre-built period grid
     * @param nDurations Number of fractional-duration steps
     * @param minFracDur Minimum fractional duration (fraction of period)
     * @param maxFracDur Maximum fractional duration (fraction of period)
     * @param nBins      Phase/duration bin count (50–2000).  Resolution = period / nBins.
     * @param backend    Backend label from {@link PeriodogramGpuContext#getAvailableBackends()}
     * @return Per-period results: power, bestDuration, bestDepth, bestPhase
     */
    public static BLSResult search(double[] t, double[] f, double[] periods,
                                   int nDurations, double minFracDur, double maxFracDur,
                                   int nBins, String backend) {
        int nPoints  = t.length;
        int nPeriods = periods.length;

        /*
         * Pass the host arrays straight to the GPU.  Earlier versions subtracted
         * t[0] from every time and the flux mean from every flux in an attempt
         * to squeeze FP32 precision out of the phase fold — but the pipeline is
         * now FP64 end-to-end, so the subtractions no longer help, and they
         * introduce rounding that is not exactly replicated by the CPU path
         * (which folds raw t and sums raw f).  Passing the same inputs to both
         * paths makes their bin assignments bit-for-bit identical, preventing
         * CPU and GPU from landing in different local maxima of the (period,
         * duration) power surface.
         */
        double[] tD       = new double[nPoints];
        double[] fD       = new double[nPoints];
        double[] periodsD = new double[nPeriods];
        for (int i = 0; i < nPoints;  i++) { tD[i] = t[i];       fD[i] = f[i]; }
        for (int i = 0; i < nPeriods; i++) { periodsD[i] = periods[i]; }

        IJ.log("[BLS GPU] search() — nPoints=" + nPoints + ", nPeriods=" + nPeriods
                + ", nDurations=" + nDurations + ", bins=" + nBins + " (FP64)");
        long scratchBytes = (long) nPeriods * nBins * (Sizeof.cl_double + Sizeof.cl_int);
        IJ.log("[BLS GPU] Global bin scratch: " + (scratchBytes / (1024 * 1024)) + " MB");

        String kernelSrc = buildKernelSource(nBins);
        PeriodogramGpuContext.ContextHolder ctx =
                PeriodogramGpuContext.getOrCreateContext(backend, kernelSrc, programCacheKey(nBins));

        CL.setExceptionsEnabled(true);

        // --- Device buffers (double) ---------------------------------------
        cl_mem dT        = clCreateBuffer(ctx.context,
                CL_MEM_READ_ONLY | CL_MEM_COPY_HOST_PTR,
                (long) nPoints  * Sizeof.cl_double, Pointer.to(tD),       null);
        cl_mem dF        = clCreateBuffer(ctx.context,
                CL_MEM_READ_ONLY | CL_MEM_COPY_HOST_PTR,
                (long) nPoints  * Sizeof.cl_double, Pointer.to(fD),       null);
        cl_mem dPeriods  = clCreateBuffer(ctx.context,
                CL_MEM_READ_ONLY | CL_MEM_COPY_HOST_PTR,
                (long) nPeriods * Sizeof.cl_double, Pointer.to(periodsD), null);

        double[] hostPower    = new double[nPeriods];
        double[] hostDuration = new double[nPeriods];
        double[] hostDepth    = new double[nPeriods];
        double[] hostPhase    = new double[nPeriods];

        cl_mem dPower    = clCreateBuffer(ctx.context, CL_MEM_WRITE_ONLY,
                (long) nPeriods * Sizeof.cl_double, null, null);
        cl_mem dDuration = clCreateBuffer(ctx.context, CL_MEM_WRITE_ONLY,
                (long) nPeriods * Sizeof.cl_double, null, null);
        cl_mem dDepth    = clCreateBuffer(ctx.context, CL_MEM_WRITE_ONLY,
                (long) nPeriods * Sizeof.cl_double, null, null);
        cl_mem dPhase    = clCreateBuffer(ctx.context, CL_MEM_WRITE_ONLY,
                (long) nPeriods * Sizeof.cl_double, null, null);

        // Global scratch for Stage-1 bin arrays, stored in BIN-MAJOR order
        // (scratchBinFluxSum[bin * nPeriods + gid]) so warp lanes coalesce
        // on the Stage-2 bin reads — see the layout note in
        // bls_periodogram.cl.  Moved out of __private memory because
        // NVIDIA's spilling of 12 KB/thread private arrays produced
        // correct Stage-1 writes but incorrect Stage-2 reads of the same
        // cells.  Total byte count is nPeriods * nBins (unchanged by the
        // permutation).
        long scratchSlots = (long) nPeriods * nBins;
        cl_mem dScratchBinFluxSum = clCreateBuffer(ctx.context, CL_MEM_READ_WRITE,
                scratchSlots * Sizeof.cl_double, null, null);
        cl_mem dScratchBinN       = clCreateBuffer(ctx.context, CL_MEM_READ_WRITE,
                scratchSlots * Sizeof.cl_int,    null, null);

        try {
            cl_kernel kernel = clCreateKernel(ctx.program, KERNEL_NAME, null);
            try {
                int arg = 0;
                clSetKernelArg(kernel, arg++, Sizeof.cl_mem,    Pointer.to(dT));
                clSetKernelArg(kernel, arg++, Sizeof.cl_mem,    Pointer.to(dF));
                clSetKernelArg(kernel, arg++, Sizeof.cl_mem,    Pointer.to(dPeriods));
                clSetKernelArg(kernel, arg++, Sizeof.cl_mem,    Pointer.to(dPower));
                clSetKernelArg(kernel, arg++, Sizeof.cl_mem,    Pointer.to(dDuration));
                clSetKernelArg(kernel, arg++, Sizeof.cl_mem,    Pointer.to(dDepth));
                clSetKernelArg(kernel, arg++, Sizeof.cl_mem,    Pointer.to(dPhase));
                clSetKernelArg(kernel, arg++, Sizeof.cl_mem,    Pointer.to(dScratchBinFluxSum));
                clSetKernelArg(kernel, arg++, Sizeof.cl_mem,    Pointer.to(dScratchBinN));
                clSetKernelArg(kernel, arg++, Sizeof.cl_int,    Pointer.to(new int[]   {nPoints}));
                clSetKernelArg(kernel, arg++, Sizeof.cl_int,    Pointer.to(new int[]   {nPeriods}));
                clSetKernelArg(kernel, arg++, Sizeof.cl_int,    Pointer.to(new int[]   {nDurations}));
                clSetKernelArg(kernel, arg++, Sizeof.cl_double, Pointer.to(new double[]{minFracDur}));
                clSetKernelArg(kernel, arg++, Sizeof.cl_double, Pointer.to(new double[]{maxFracDur}));

                long[] globalSize = {roundUpToMultiple(nPeriods, 64)};
                long[] localSize  = {64};
                long t0 = System.currentTimeMillis();
                clEnqueueNDRangeKernel(ctx.queue, kernel, 1,
                        null, globalSize, localSize, 0, null, null);
                clFinish(ctx.queue);
                IJ.log("[BLS GPU] Kernel finished in " + (System.currentTimeMillis() - t0) + " ms.");

            } finally {
                clReleaseKernel(kernel);
            }

            clEnqueueReadBuffer(ctx.queue, dPower,    CL_TRUE, 0,
                    (long) nPeriods * Sizeof.cl_double, Pointer.to(hostPower),    0, null, null);
            clEnqueueReadBuffer(ctx.queue, dDuration, CL_TRUE, 0,
                    (long) nPeriods * Sizeof.cl_double, Pointer.to(hostDuration), 0, null, null);
            clEnqueueReadBuffer(ctx.queue, dDepth,    CL_TRUE, 0,
                    (long) nPeriods * Sizeof.cl_double, Pointer.to(hostDepth),    0, null, null);
            clEnqueueReadBuffer(ctx.queue, dPhase,    CL_TRUE, 0,
                    (long) nPeriods * Sizeof.cl_double, Pointer.to(hostPhase),    0, null, null);

        } finally {
            clReleaseMemObject(dT);
            clReleaseMemObject(dF);
            clReleaseMemObject(dPeriods);
            clReleaseMemObject(dPower);
            clReleaseMemObject(dDuration);
            clReleaseMemObject(dDepth);
            clReleaseMemObject(dPhase);
            clReleaseMemObject(dScratchBinFluxSum);
            clReleaseMemObject(dScratchBinN);
        }

        return new BLSResult(hostPower, hostDuration, hostDepth, hostPhase);
    }

    /**
     * Loads the kernel source and prepends a {@code #define BLS_BINS <n>} so
     * the kernel's bin-array strides are sized for the requested resolution.
     * The .cl file uses an {@code #ifndef} guard so the prepended value wins.
     */
    private static String buildKernelSource(int nBins) {
        try (InputStream is = BLSGpu.class.getResourceAsStream(KERNEL_RESOURCE)) {
            if (is == null) throw new RuntimeException("BLS kernel not found: " + KERNEL_RESOURCE);
            String body = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            return "#define BLS_BINS " + nBins + "\n" + body;
        } catch (Exception e) {
            throw new RuntimeException("Failed to load BLS kernel: " + e.getMessage(), e);
        }
    }

    private static String programCacheKey(int nBins) {
        // v7: bin-major memory layout for scratchBinFluxSum / scratchBinN.
        // Stage-2 accesses are now coalesced across warp lanes (all lanes
        // share the same bin index per iteration), at the cost of a
        // non-coalesced Stage-1 scatter which is ~nPoints writes per
        // thread — dwarfed by the Stage-2 load volume.
        // (v6: __global scratch, thread-major; v5: FP64 raw inputs.)
        return "bls_binned_v7_binmajor_bins" + nBins;
    }

    private static long roundUpToMultiple(long value, long multiple) {
        return ((value + multiple - 1) / multiple) * multiple;
    }
}
