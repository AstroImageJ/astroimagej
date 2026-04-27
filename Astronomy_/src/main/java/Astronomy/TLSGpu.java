package Astronomy;

import static Astronomy.Periodogram_.lowerBoundD;
import static Astronomy.Periodogram_.upperBoundD;
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.BooleanSupplier;
import java.util.function.IntConsumer;

import ij.IJ;
import org.jocl.CL;
import org.jocl.Pointer;
import org.jocl.Sizeof;
import org.jocl.cl_kernel;
import org.jocl.cl_mem;

/**
 * GPU-accelerated Transit Least Squares (TLS) periodogram using OpenCL / JOCL.
 *
 * <p>Performs the same grid search as {@link TLS#search} but offloads the
 * (period × duration × phase) inner loops to the GPU.  The post-processing
 * steps — SDE normalisation and sliding-trapezoid T0 refinement — remain on
 * the CPU because they are fast and depend on Java-only logic.
 *
 * <p>The kernel source is loaded from
 * {@code /Astronomy/tls_periodogram.cl} on the classpath.
 */
public class TLSGpu {

    private static final String KERNEL_RESOURCE = "/Astronomy/tls_periodogram.cl";
    private static final String KERNEL_NAME     = "tls_compute";

    /**
     * Pre-compiles the TLS OpenCL kernel for {@code backend} with the given bin
     * count and the user's {@code maxFracDur}.  {@code maxFracDur} determines
     * the compile-time size of the per-thread model-profile array — a kernel
     * compiled with one value cannot be reused for a larger one, so it is part
     * of the program cache key.  Subsequent calls to {@link #search} with the
     * same {@code (nBins, maxFracDur)} pair reuse the cache.  Safe to call
     * multiple times.
     */
    public static void warmUp(String backend, int nBins, double maxFracDur) {
        int maxHalfWin = computeMaxHalfWin(nBins, maxFracDur);
        PeriodogramGpuContext.getOrCreateContext(backend,
                buildKernelSource(nBins, maxHalfWin), programCacheKey(nBins, maxHalfWin));
    }

    /**
     * GPU equivalent of {@link TLS#search} — phase-binned, FP32.
     *
     * @param time             Time array (sorted ascending)
     * @param flux             Flux array (normalised to ~1.0)
     * @param minPeriod        Minimum period (days)
     * @param maxPeriod        Maximum period (days)
     * @param nPeriods         Number of period grid steps
     * @param minFracDur       Minimum fractional duration (0–1)
     * @param maxFracDur       Maximum fractional duration (0–1)
     * @param nDurations       Number of duration steps
     * @param nBins            Phase bin count (50–2000).  Resolution = period / nBins.
     * @param u1               Linear limb darkening coefficient
     * @param u2               Quadratic limb darkening coefficient
     * @param aRs              Scaled semi-major axis (a/Rs) — kept for T0 refinement
     * @param inc              Inclination (degrees)         — kept for T0 refinement
     * @param progressCallback Called once per period as the GPU result is processed (may be null)
     * @param backend          Backend label from {@link PeriodogramGpuContext#getAvailableBackends()}
     * @return TLS result with SDE-normalised signal and refined T0
     */
    public static TLS.Result search(double[] time, double[] flux,
                                    double minPeriod, double maxPeriod,
                                    int nPeriods, double minFracDur, double maxFracDur,
                                    int nDurations, int nBins, double u1, double u2,
                                    double aRs, double inc,
                                    IntConsumer progressCallback, String backend) {
        return search(time, flux, minPeriod, maxPeriod, nPeriods, minFracDur, maxFracDur,
                nDurations, nBins, u1, u2, aRs, inc, progressCallback, backend, () -> false);
    }

    /**
     * Cancellable variant of {@link #search(double[], double[], double, double, int, double, double, int, int, double, double, double, double, IntConsumer, String)}.
     *
     * <p>{@code cancelCheck} is polled between GPU dispatch slices (the period
     * grid is split into ~16 chunks via {@code global_work_offset}, each
     * followed by a {@code clFinish} + poll).  When it returns {@code true},
     * the unprocessed tail of every per-period output array is filled with
     * {@link Double#NaN} so downstream plotting / reductions ignore them.
     *
     * <p>Without this chunking the single {@code clEnqueueNDRangeKernel} call
     * runs to completion before the host can react, so the Stop button has
     * no effect until the whole kernel finishes.
     */
    public static TLS.Result search(double[] time, double[] flux,
                                    double minPeriod, double maxPeriod,
                                    int nPeriods, double minFracDur, double maxFracDur,
                                    int nDurations, int nBins, double u1, double u2,
                                    double aRs, double inc,
                                    IntConsumer progressCallback, String backend,
                                    BooleanSupplier cancelCheck) {

        // --- Build period grid (uniform frequency, same as TLS.java line 44-50) ---
        double minFreq = 1.0 / maxPeriod;
        double maxFreq = 1.0 / minPeriod;
        double[] periods = new double[nPeriods];
        for (int i = 0; i < nPeriods; i++) {
            double freq = minFreq + i * (maxFreq - minFreq) / Math.max(1, nPeriods - 1);
            periods[i] = 1.0 / freq;
        }

        // --- GPU grid search ------------------------------------------------
        double[] rawSnr        = new double[nPeriods];
        double[] bestDurations = new double[nPeriods];
        double[] bestDepths    = new double[nPeriods];
        gpuGridSearch(time, flux, periods, nPeriods, nDurations, nBins,
                      minFracDur, maxFracDur, u1, u2,
                      rawSnr, bestDurations, bestDepths, backend, cancelCheck);

        // Report progress in one sweep (GPU finishes in a burst)
        IJ.showProgress(1.0);
        if (progressCallback != null) {
            for (int i = 0; i < nPeriods; i++) progressCallback.accept(i);
        }

        // --- SDE normalisation (copy of TLS.java lines 107-133) --------------
        int bestPeriodIdx = 0;
        for (int i = 1; i < nPeriods; i++) {
            if (rawSnr[i] > rawSnr[bestPeriodIdx]) bestPeriodIdx = i;
        }
        int sdeWindow = Math.max(1, nPeriods / 100);
        List<Double> sdeForStats = new ArrayList<>();
        for (int i = 0; i < nPeriods; i++) {
            double v = rawSnr[i];
            // Skip the NaN-padded tail left behind by a user Stop so the
            // median / std below describe only real work.
            if (Double.isNaN(v)) continue;
            if (Math.abs(i - bestPeriodIdx) > sdeWindow) sdeForStats.add(v);
        }
        double[] sdeArr = sdeForStats.stream().mapToDouble(Double::doubleValue).toArray();
        double median = 0, std = 0;
        if (sdeArr.length > 0) {
            Arrays.sort(sdeArr);
            median = sdeArr[sdeArr.length / 2];
            double sumsq = 0;
            for (double v : sdeArr) sumsq += (v - median) * (v - median);
            std = Math.sqrt(sumsq / sdeArr.length);
        }
        double[] sde = new double[nPeriods];
        for (int i = 0; i < nPeriods; i++) {
            sde[i] = (std > 0) ? (rawSnr[i] - median) / std : 0.0;
        }

        // --- T0 refinement (mirrors TLS.java lines 134-255) ------------------
        double bestPeriod   = periods[bestPeriodIdx];
        double bestDuration = bestDurations[bestPeriodIdx];

        // Min-average phase bin (T0 refinement only — independent of search-grid nBins)
        int nT0Bins = 200;
        double[] phases = new double[flux.length];
        for (int i = 0; i < flux.length; i++) {
            double ph = (time[i] - time[0]) / bestPeriod;
            phases[i] = ph - Math.floor(ph);
        }
        List<List<Double>> binFluxes = new ArrayList<>();
        for (int i = 0; i < nT0Bins; i++) binFluxes.add(new ArrayList<>());
        for (int i = 0; i < flux.length; i++) {
            int bin = (int) Math.floor(phases[i] * nT0Bins);
            bin = Math.max(0, Math.min(nT0Bins - 1, bin));
            binFluxes.get(bin).add(flux[i]);
        }
        double minAvg = Double.POSITIVE_INFINITY;
        int minBin = -1;
        for (int i = 0; i < nT0Bins; i++) {
            List<Double> fbin = binFluxes.get(i);
            if (fbin.size() < 5) continue;
            double sum = 0;
            for (double v : fbin) sum += v;
            double avg = sum / fbin.size();
            if (avg < minAvg) { minAvg = avg; minBin = i; }
        }
        double minPhase = (minBin >= 0) ? (minBin + 0.5) / nT0Bins : 0.0;
        double t0MinAvg = time[0] + minPhase * bestPeriod;

        // Sliding trapezoid fit (Huber loss)
        double bestT0Sliding = t0MinAvg;
        double minLoss = Double.POSITIVE_INFINITY;
        double minTauFrac = 0.01, maxTauFrac = 0.99, tauFracStep = 0.01;
        int    nTau = (int) Math.round((maxTauFrac - minTauFrac) / tauFracStep) + 1;
        double phaseStep   = (bestDuration / bestPeriod) / 200.0;
        int    nPhaseSteps = (int) Math.ceil(1.0 / phaseStep);
        double delta = 0.002;

        double[] phasesForSliding = new double[flux.length];
        for (int i = 0; i < flux.length; i++) {
            double ph = (time[i] - time[0]) / bestPeriod;
            phasesForSliding[i] = ph - Math.floor(ph);
        }

        double bestDepthForSliding = findBestDepth(time, flux, bestPeriod, t0MinAvg, bestDuration, aRs, inc, u1, u2);

        // --- Sort phases once so each grid cell can binary-search the in-window points ---
        final int nDataT0 = flux.length;
        Integer[] sortIdxT0 = new Integer[nDataT0];
        for (int i = 0; i < nDataT0; i++) sortIdxT0[i] = i;
        final double[] phasesCaptureT0 = phasesForSliding;
        Arrays.sort(sortIdxT0, Comparator.comparingDouble(i -> phasesCaptureT0[i]));
        final double[] sortedPhasesT0 = new double[nDataT0];
        final double[] sortedFluxT0   = new double[nDataT0];
        for (int i = 0; i < nDataT0; i++) {
            sortedPhasesT0[i] = phasesForSliding[sortIdxT0[i]];
            sortedFluxT0[i]   = flux[sortIdxT0[i]];
        }

        final double durOverPeriodT0 = bestDuration / bestPeriod;
        final double windowT0        = 1.5 * durOverPeriodT0;
        final double deltaT0         = delta;
        final double phaseStepT0     = phaseStep;
        final int    nPhaseStepsT0   = nPhaseSteps;
        final double bestDepthT0     = bestDepthForSliding;
        final double bestPeriodT0    = bestPeriod;

        int nT0Threads = Math.max(1, Runtime.getRuntime().availableProcessors() - 2);
        ExecutorService t0Exec = Executors.newFixedThreadPool(nT0Threads);
        List<Future<double[]>> t0Futures = new ArrayList<>();
        long t0Start = System.currentTimeMillis();

        for (int tauIdx = 0; tauIdx < nTau; tauIdx++) {
            final double tauFrac = minTauFrac + tauIdx * tauFracStep;
            final double tauPhase = tauFrac * durOverPeriodT0;
            final double flatPhase = durOverPeriodT0 - 2 * tauPhase;
            if (flatPhase < 0) continue;
            final double tauPhaseF  = tauPhase;
            final double flatPhaseF = flatPhase;

            t0Futures.add(t0Exec.submit(() -> {
                double localBestLoss        = Double.POSITIVE_INFINITY;
                double localBestPhaseOffset = 0;

                for (int s = 0; s < nPhaseStepsT0; s++) {
                    double phaseOffset = s * phaseStepT0 * bestPeriodT0;
                    double C = phaseOffset / bestPeriodT0;
                    C -= Math.floor(C);

                    double sumLoss = 0.0;
                    int    nUsed   = 0;

                    if (windowT0 >= 0.5) {
                        for (int k = 0; k < nDataT0; k++) {
                            double ps = sortedPhasesT0[k] - C;
                            ps -= Math.floor(ps);
                            double mdl;
                            if      (ps < tauPhaseF)                mdl = -bestDepthT0 * (ps / tauPhaseF);
                            else if (ps < tauPhaseF + flatPhaseF)   mdl = -bestDepthT0;
                            else if (ps < 2*tauPhaseF + flatPhaseF) mdl = -bestDepthT0 * (1 - (ps - tauPhaseF - flatPhaseF) / tauPhaseF);
                            else                                     mdl = 0.0;
                            double rr = sortedFluxT0[k] - mdl;
                            double absR = Math.abs(rr);
                            sumLoss += (absR <= deltaT0) ? 0.5*rr*rr : deltaT0*(absR - 0.5*deltaT0);
                            nUsed++;
                        }
                    } else {
                        double lo = C - windowT0;
                        double hi = C + windowT0;
                        int r0a, r0b, r1a, r1b;
                        if (lo < 0) {
                            r0a = lowerBoundD(sortedPhasesT0, lo + 1);  r0b = nDataT0;
                            r1a = 0;                                    r1b = upperBoundD(sortedPhasesT0, hi);
                        } else if (hi >= 1.0) {
                            r0a = lowerBoundD(sortedPhasesT0, lo);      r0b = nDataT0;
                            r1a = 0;                                    r1b = upperBoundD(sortedPhasesT0, hi - 1);
                        } else {
                            r0a = lowerBoundD(sortedPhasesT0, lo);      r0b = upperBoundD(sortedPhasesT0, hi);
                            r1a = 0;                                    r1b = 0;
                        }
                        for (int k = r0a; k < r0b; k++) {
                            double ps = sortedPhasesT0[k] - C;
                            ps -= Math.floor(ps);
                            double mdl;
                            if      (ps < tauPhaseF)                mdl = -bestDepthT0 * (ps / tauPhaseF);
                            else if (ps < tauPhaseF + flatPhaseF)   mdl = -bestDepthT0;
                            else if (ps < 2*tauPhaseF + flatPhaseF) mdl = -bestDepthT0 * (1 - (ps - tauPhaseF - flatPhaseF) / tauPhaseF);
                            else                                     mdl = 0.0;
                            double rr = sortedFluxT0[k] - mdl;
                            double absR = Math.abs(rr);
                            sumLoss += (absR <= deltaT0) ? 0.5*rr*rr : deltaT0*(absR - 0.5*deltaT0);
                            nUsed++;
                        }
                        for (int k = r1a; k < r1b; k++) {
                            double ps = sortedPhasesT0[k] - C;
                            ps -= Math.floor(ps);
                            double mdl;
                            if      (ps < tauPhaseF)                mdl = -bestDepthT0 * (ps / tauPhaseF);
                            else if (ps < tauPhaseF + flatPhaseF)   mdl = -bestDepthT0;
                            else if (ps < 2*tauPhaseF + flatPhaseF) mdl = -bestDepthT0 * (1 - (ps - tauPhaseF - flatPhaseF) / tauPhaseF);
                            else                                     mdl = 0.0;
                            double rr = sortedFluxT0[k] - mdl;
                            double absR = Math.abs(rr);
                            sumLoss += (absR <= deltaT0) ? 0.5*rr*rr : deltaT0*(absR - 0.5*deltaT0);
                            nUsed++;
                        }
                    }

                    double cellLoss = (nUsed > 0) ? sumLoss / nUsed : Double.POSITIVE_INFINITY;
                    if (cellLoss < localBestLoss) {
                        localBestLoss        = cellLoss;
                        localBestPhaseOffset = phaseOffset;
                    }
                }
                return new double[] { localBestLoss, localBestPhaseOffset };
            }));
        }

        try {
            for (Future<double[]> fut : t0Futures) {
                double[] r = fut.get();
                if (r[0] < minLoss) {
                    minLoss       = r[0];
                    bestT0Sliding = time[0] + r[1];
                }
            }
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            IJ.log("[TLS T0] Parallel T0 search interrupted.");
        } catch (java.util.concurrent.ExecutionException ee) {
            IJ.log("[TLS T0] Parallel T0 search error: " + ee.getMessage());
        } finally {
            t0Exec.shutdown();
        }
        System.out.printf("[TLS GPU T0] Parallel T0 refinement: %d threads, %d ms%n",
                nT0Threads, System.currentTimeMillis() - t0Start);

        return new TLS.Result(periods, sde, bestDurations, bestDepths, bestT0Sliding, t0MinAvg);
    }

    // -------------------------------------------------------------------------
    // GPU grid search
    // -------------------------------------------------------------------------

    private static void gpuGridSearch(double[] t, double[] f,
                                      double[] periods, int nPeriods, int nDurations, int nBins,
                                      double minFracDur, double maxFracDur,
                                      double u1, double u2,
                                      double[] outSnr, double[] outBestDurations,
                                      double[] outBestDepths, String backend,
                                      BooleanSupplier cancelCheck) {
        int nPoints = t.length;

        // Time normalisation (t[0] = 0) — keeps phase fold accurate for large BJD
        // by avoiding wasted precision on the integer part.  Full FP64 end-to-end.
        double tMin = t[0];
        double[] tD       = new double[nPoints];
        double[] fD       = new double[nPoints];
        double[] periodsD = new double[nPeriods];
        double sumF2D = 0.0;
        for (int i = 0; i < nPoints; i++) {
            tD[i] = t[i] - tMin;
            fD[i] = f[i];
            sumF2D += f[i] * f[i];
        }
        for (int i = 0; i < nPeriods; i++) periodsD[i] = periods[i];
        final double sumF2 = sumF2D;

        int maxHalfWin = computeMaxHalfWin(nBins, maxFracDur);
        IJ.log("[TLS GPU] search() — nPoints=" + nPoints + ", nPeriods=" + nPeriods
                + ", nDurations=" + nDurations + ", bins=" + nBins
                + ", maxHalfWin=" + maxHalfWin + " (FP64)");

        String kernelSrc = buildKernelSource(nBins, maxHalfWin);
        PeriodogramGpuContext.ContextHolder ctx =
                PeriodogramGpuContext.getOrCreateContext(backend, kernelSrc,
                        programCacheKey(nBins, maxHalfWin));

        CL.setExceptionsEnabled(true);

        cl_mem dT        = clCreateBuffer(ctx.context,
                CL_MEM_READ_ONLY | CL_MEM_COPY_HOST_PTR,
                (long) nPoints  * Sizeof.cl_double, Pointer.to(tD),       null);
        cl_mem dF        = clCreateBuffer(ctx.context,
                CL_MEM_READ_ONLY | CL_MEM_COPY_HOST_PTR,
                (long) nPoints  * Sizeof.cl_double, Pointer.to(fD),       null);
        cl_mem dPeriods  = clCreateBuffer(ctx.context,
                CL_MEM_READ_ONLY | CL_MEM_COPY_HOST_PTR,
                (long) nPeriods * Sizeof.cl_double, Pointer.to(periodsD), null);

        double[] hostSnr      = new double[nPeriods];
        double[] hostDepth    = new double[nPeriods];
        double[] hostDuration = new double[nPeriods];

        cl_mem dSnr      = clCreateBuffer(ctx.context, CL_MEM_WRITE_ONLY,
                (long) nPeriods * Sizeof.cl_double, null, null);
        cl_mem dDepth    = clCreateBuffer(ctx.context, CL_MEM_WRITE_ONLY,
                (long) nPeriods * Sizeof.cl_double, null, null);
        cl_mem dDuration = clCreateBuffer(ctx.context, CL_MEM_WRITE_ONLY,
                (long) nPeriods * Sizeof.cl_double, null, null);

        // Global scratch for Stage-1 bin arrays only, stored in BIN-MAJOR
        // order (scratchBinFluxSum[bin * nPeriods + gid]) so warp lanes
        // coalesce on the Stage-2 bin reads — see the layout note in
        // tls_periodogram.cl.  Moved out of __private memory because
        // NVIDIA's spilling of multi-KB private arrays produced correct
        // writes but unreliable reads of the same cells across loops.
        // The per-duration modelProfile still lives in private memory
        // (sized to maxHalfWin+1, small enough to stay in registers), so
        // it doesn't need global scratch and avoids the huge read traffic
        // that a global-memory modelProfile would incur.
        long scratchSlots       = (long) nPeriods * nBins;
        cl_mem dScratchBinFluxSum = clCreateBuffer(ctx.context, CL_MEM_READ_WRITE,
                scratchSlots * Sizeof.cl_double, null, null);
        cl_mem dScratchBinN       = clCreateBuffer(ctx.context, CL_MEM_READ_WRITE,
                scratchSlots * Sizeof.cl_int,    null, null);
        long scratchBytes = scratchSlots * (Sizeof.cl_double + Sizeof.cl_int);
        IJ.log("[TLS GPU] Global scratch: " + (scratchBytes / (1024 * 1024)) + " MB");

        boolean cancelled = false;
        int     processedPeriods = nPeriods;

        try {
            cl_kernel kernel = clCreateKernel(ctx.program, KERNEL_NAME, null);
            try {
                int arg = 0;
                clSetKernelArg(kernel, arg++, Sizeof.cl_mem,    Pointer.to(dT));
                clSetKernelArg(kernel, arg++, Sizeof.cl_mem,    Pointer.to(dF));
                clSetKernelArg(kernel, arg++, Sizeof.cl_mem,    Pointer.to(dPeriods));
                clSetKernelArg(kernel, arg++, Sizeof.cl_mem,    Pointer.to(dSnr));
                clSetKernelArg(kernel, arg++, Sizeof.cl_mem,    Pointer.to(dDepth));
                clSetKernelArg(kernel, arg++, Sizeof.cl_mem,    Pointer.to(dDuration));
                clSetKernelArg(kernel, arg++, Sizeof.cl_mem,    Pointer.to(dScratchBinFluxSum));
                clSetKernelArg(kernel, arg++, Sizeof.cl_mem,    Pointer.to(dScratchBinN));
                clSetKernelArg(kernel, arg++, Sizeof.cl_int,    Pointer.to(new int[]   {nPoints}));
                clSetKernelArg(kernel, arg++, Sizeof.cl_int,    Pointer.to(new int[]   {nPeriods}));
                clSetKernelArg(kernel, arg++, Sizeof.cl_int,    Pointer.to(new int[]   {nDurations}));
                clSetKernelArg(kernel, arg++, Sizeof.cl_double, Pointer.to(new double[]{minFracDur}));
                clSetKernelArg(kernel, arg++, Sizeof.cl_double, Pointer.to(new double[]{maxFracDur}));
                clSetKernelArg(kernel, arg++, Sizeof.cl_double, Pointer.to(new double[]{u1}));
                clSetKernelArg(kernel, arg++, Sizeof.cl_double, Pointer.to(new double[]{u2}));
                clSetKernelArg(kernel, arg++, Sizeof.cl_double, Pointer.to(new double[]{sumF2}));

                long paddedTotal = roundUpToMultiple(nPeriods, 64);
                // Aim for ~16 dispatch slices so the Stop button is noticed
                // within a few % of total kernel time while keeping per-slice
                // overhead negligible.  Each slice uses global_work_offset to
                // target its period sub-range; the kernel is unchanged (each
                // period's work is fully independent).
                long targetChunk = Math.max(64L, roundUpToMultiple(
                        Math.max(1, nPeriods / 16), 64));
                long[] localSize = {64};
                long t0 = System.currentTimeMillis();
                long dispatched = 0;
                for (long chunkStart = 0; chunkStart < paddedTotal; chunkStart += targetChunk) {
                    long thisChunk = Math.min(targetChunk, paddedTotal - chunkStart);
                    long[] gOffset = {chunkStart};
                    long[] gSize   = {thisChunk};
                    clEnqueueNDRangeKernel(ctx.queue, kernel, 1,
                            gOffset, gSize, localSize, 0, null, null);
                    clFinish(ctx.queue);
                    dispatched = Math.min(chunkStart + thisChunk, nPeriods);
                    if (cancelCheck.getAsBoolean()) {
                        IJ.log("[TLS GPU] Cancelled after ~" + dispatched + "/" + nPeriods + " periods.");
                        break;
                    }
                }
                cancelled = dispatched < nPeriods;
                processedPeriods = (int) dispatched;
                IJ.log("[TLS GPU] Kernel finished in "
                        + (System.currentTimeMillis() - t0) + " ms"
                        + (cancelled ? " (cancelled)" : "") + ".");
            } finally {
                clReleaseKernel(kernel);
            }

            clEnqueueReadBuffer(ctx.queue, dSnr,      CL_TRUE, 0,
                    (long) nPeriods * Sizeof.cl_double, Pointer.to(hostSnr),      0, null, null);
            clEnqueueReadBuffer(ctx.queue, dDepth,    CL_TRUE, 0,
                    (long) nPeriods * Sizeof.cl_double, Pointer.to(hostDepth),    0, null, null);
            clEnqueueReadBuffer(ctx.queue, dDuration, CL_TRUE, 0,
                    (long) nPeriods * Sizeof.cl_double, Pointer.to(hostDuration), 0, null, null);

            // Unprocessed tail of the write-only output buffers contains
            // uninitialised garbage — fill with NaN so downstream plotting
            // and the bestSDE reduction skip past it (mirrors the CPU Stop
            // behaviour where unfinished cells stay untouched / ignored).
            if (cancelled && processedPeriods < nPeriods) {
                Arrays.fill(hostSnr,      processedPeriods, nPeriods, Double.NaN);
                Arrays.fill(hostDepth,    processedPeriods, nPeriods, Double.NaN);
                Arrays.fill(hostDuration, processedPeriods, nPeriods, Double.NaN);
            }

        } finally {
            clReleaseMemObject(dT);
            clReleaseMemObject(dF);
            clReleaseMemObject(dPeriods);
            clReleaseMemObject(dSnr);
            clReleaseMemObject(dDepth);
            clReleaseMemObject(dDuration);
            clReleaseMemObject(dScratchBinFluxSum);
            clReleaseMemObject(dScratchBinN);
        }

        for (int pi = 0; pi < nPeriods; pi++) {
            outSnr[pi]           = hostSnr[pi];
            outBestDepths[pi]    = hostDepth[pi];
            outBestDurations[pi] = hostDuration[pi];
        }
    }

    // -------------------------------------------------------------------------
    // Helpers — mirrors the private methods in TLS.java
    // -------------------------------------------------------------------------

    /** Brute-force depth scan to seed the sliding T0 fit (mirrors TLS.java lines 196-209). */
    private static double findBestDepth(double[] time, double[] flux,
                                        double bestPeriod, double t0,
                                        double bestDuration, double aRs, double inc,
                                        double u1, double u2) {
        double bestChi2 = Double.POSITIVE_INFINITY;
        double bestDepth = 0.0;
        for (double d : new double[]{0.001, 0.002, 0.005, 0.01, 0.02, 0.05, 0.1}) {
            double[] model = MandelAgolTransitModel.compute(time, bestPeriod, t0, bestDuration,
                    d, Math.sqrt(d), aRs, inc, u1, u2);
            double chi2 = 0;
            for (int i = 0; i < flux.length; i++) {
                double r = flux[i] - model[i];
                chi2 += r * r;
            }
            if (chi2 < bestChi2) { bestChi2 = chi2; bestDepth = d; }
        }
        return bestDepth;
    }

    /**
     * Loads the kernel source and prepends {@code #define TLS_BINS <n>} and
     * {@code #define MAX_HALF_WIN <m>} so the kernel's bin-array strides and
     * model-profile array are sized for the requested resolution and
     * maxFracDur.  The .cl file uses {@code #ifndef} guards so the prepended
     * values win over the defaults.
     */
    private static String buildKernelSource(int nBins, int maxHalfWin) {
        try (InputStream is = TLSGpu.class.getResourceAsStream(KERNEL_RESOURCE)) {
            if (is == null) throw new RuntimeException("TLS kernel not found: " + KERNEL_RESOURCE);
            String body = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            return "#define TLS_BINS " + nBins + "\n"
                 + "#define MAX_HALF_WIN " + maxHalfWin + "\n"
                 + body;
        } catch (Exception e) {
            throw new RuntimeException("Failed to load TLS kernel: " + e.getMessage(), e);
        }
    }

    /**
     * Matches the kernel's halfWin clamp: we size the private modelProfile
     * array to {@code ceil(0.5 * maxFracDur * nBins)}, but never larger than
     * {@code (nBins-1)/2} (the hard cap to prevent b1==b2 aliasing).
     */
    private static int computeMaxHalfWin(int nBins, double maxFracDur) {
        int hw = (int) Math.ceil(0.5 * maxFracDur * nBins);
        int cap = (nBins - 1) / 2;
        if (hw < 0)   hw = 0;
        if (hw > cap) hw = cap;
        return hw;
    }

    private static String programCacheKey(int nBins, int maxHalfWin) {
        // v8: bin-major layout for scratchBinFluxSum / scratchBinN.  All
        // Stage-2 bin reads (ph, b1, b2) now coalesce across warp lanes
        // because every lane shares the same bin index per iteration.
        // Stage-1 histogram scatter is the only remaining non-coalesced
        // access, but it's dominated by Stage-2 load volume.
        // (v7: thread-major __global scratch; modelProfile private, hw-sized.)
        return "tls_binned_v8_binmajor_bins" + nBins + "_hw" + maxHalfWin;
    }

    private static long roundUpToMultiple(long value, long multiple) {
        return ((value + multiple - 1) / multiple) * multiple;
    }
}
