package Astronomy;

import static org.jocl.CL.CL_DEVICE_EXTENSIONS;
import static org.jocl.CL.CL_DEVICE_NAME;
import static org.jocl.CL.CL_DEVICE_TYPE_GPU;
import static org.jocl.CL.CL_PROGRAM_BUILD_LOG;
import static org.jocl.CL.CL_SUCCESS;
import static org.jocl.CL.clBuildProgram;
import static org.jocl.CL.clCreateCommandQueueWithProperties;
import static org.jocl.CL.clCreateContext;
import static org.jocl.CL.clCreateProgramWithSource;
import static org.jocl.CL.clGetDeviceIDs;
import static org.jocl.CL.clGetDeviceInfo;
import static org.jocl.CL.clGetPlatformIDs;
import static org.jocl.CL.clGetProgramBuildInfo;
import static org.jocl.CL.clReleaseCommandQueue;
import static org.jocl.CL.clReleaseContext;
import static org.jocl.CL.clReleaseProgram;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import ij.IJ;
import org.jocl.CL;
import org.jocl.Pointer;
import org.jocl.cl_command_queue;
import org.jocl.cl_context;
import org.jocl.cl_device_id;
import org.jocl.cl_platform_id;
import org.jocl.cl_program;

/**
 * Manages OpenCL device discovery and context/program caching for GPU periodogram computation.
 *
 * <p>Call {@link #getAvailableBackends()} to populate the UI dropdown. Pass the selected
 * backend string to {@link #getOrCreateContext(String)} to obtain a ready-to-use context
 * that includes a compiled program for whichever kernel set is requested.
 */
public class PeriodogramGpuContext {

    /** Sentinel value used when no GPU is found or JOCL is unavailable. */
    public static final String CPU_BACKEND = "CPU";

    /** Cache: backend label → context wrapper (built once per JVM session). */
    private static final Map<String, ContextHolder> contextCache = new ConcurrentHashMap<>();

    /** Cached result of device discovery so the dialog is cheap to open repeatedly. */
    private static volatile String[] cachedBackends = null;

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    /**
     * Returns the list of available compute backends.
     * Always contains at least {@code "CPU"}.
     * GPU entries have the form {@code "GPU — <device name> (OpenCL)"}.
     */
    public static synchronized String[] getAvailableBackends() {
        if (cachedBackends != null) return cachedBackends;
        cachedBackends = buildBackendList();
        return cachedBackends;
    }

    /**
     * Returns true if the given backend label refers to a GPU.
     */
    public static boolean isGpu(String backend) {
        return backend != null && backend.startsWith("GPU");
    }

    /**
     * Returns a {@link ContextHolder} for the named backend, building and caching it on
     * first call.  Throws {@link RuntimeException} if the OpenCL device cannot be
     * initialised or the kernel fails to compile.
     */
    public static ContextHolder getOrCreateContext(String backend, String kernelSource, String programCacheKey) {
        String cacheKey = backend + "|" + programCacheKey;
        return contextCache.computeIfAbsent(cacheKey, k -> {
            IJ.log("[GPU Periodogram] Building context for cache key: " + programCacheKey);
            return buildContext(backend, kernelSource);
        });
    }

    /**
     * Releases all cached OpenCL contexts/programs.  Call on plugin shutdown if desired.
     */
    public static void releaseAll() {
        for (ContextHolder h : contextCache.values()) h.release();
        contextCache.clear();
        cachedBackends = null;
    }

    // -------------------------------------------------------------------------
    // Device discovery
    // -------------------------------------------------------------------------

    private static String[] buildBackendList() {
        List<String> backends = new ArrayList<>();
        backends.add(CPU_BACKEND);
        try {
            CL.setExceptionsEnabled(false); // manual error checking during probe
            int[] nPlatforms = new int[1];
            int err = clGetPlatformIDs(0, null, nPlatforms);
            if (err != CL_SUCCESS || nPlatforms[0] == 0) return backends.toArray(new String[0]);

            cl_platform_id[] platforms = new cl_platform_id[nPlatforms[0]];
            clGetPlatformIDs(platforms.length, platforms, null);

            for (cl_platform_id platform : platforms) {
                int[] nDevices = new int[1];
                err = clGetDeviceIDs(platform, CL_DEVICE_TYPE_GPU, 0, null, nDevices);
                if (err != CL_SUCCESS || nDevices[0] == 0) continue;

                cl_device_id[] devices = new cl_device_id[nDevices[0]];
                clGetDeviceIDs(platform, CL_DEVICE_TYPE_GPU, devices.length, devices, null);

                for (cl_device_id device : devices) {
                    String name = getDeviceString(device, CL_DEVICE_NAME).trim();
                    String exts = getDeviceString(device, CL_DEVICE_EXTENSIONS);
                    if (!exts.contains("cl_khr_fp64")) {
                        IJ.log("[GPU Periodogram] Skipping " + name + ": no double-precision support (cl_khr_fp64).");
                        continue;
                    }
                    backends.add("GPU \u2014 " + name + " (OpenCL)");
                }
            }
        } catch (Throwable t) {
            // JOCL not available or no GPU drivers — silently fall back to CPU-only list
            IJ.log("[GPU Periodogram] OpenCL unavailable: " + t.getMessage());
        }
        return backends.toArray(new String[0]);
    }

    // -------------------------------------------------------------------------
    // Context construction
    // -------------------------------------------------------------------------

    private static ContextHolder buildContext(String backend, String kernelSource) {
        CL.setExceptionsEnabled(true);

        // Locate the device matching the backend label
        cl_device_id device = findDevice(backend);
        if (device == null) throw new RuntimeException("Cannot find OpenCL device for: " + backend);

        cl_context context = clCreateContext(null, 1, new cl_device_id[]{device}, null, null, null);
        cl_command_queue queue = clCreateCommandQueueWithProperties(context, device, null, null);

        cl_program program = clCreateProgramWithSource(context, 1, new String[]{kernelSource}, null, null);
        IJ.log("[GPU Periodogram] Compiling OpenCL kernel for " + backend
                + " (first run may take 30–90 s while the driver JIT-compiles)…");
        // NB: no -cl-mad-enable / -cl-fast-relaxed-math — those swap in lower-precision
        // mad/divide instructions that were causing GPU results to disagree with CPU for
        // shallow long-period transits.  IEEE-compliant arithmetic is worth the few ms.
        int buildErr = clBuildProgram(program, 0, null, "", null, null);
        if (buildErr != CL_SUCCESS) {
            byte[] log = new byte[65536];
            clGetProgramBuildInfo(program, device, CL_PROGRAM_BUILD_LOG,
                    log.length, Pointer.to(log), null);
            String logStr = new String(log).trim();
            clReleaseProgram(program);
            clReleaseCommandQueue(queue);
            clReleaseContext(context);
            throw new RuntimeException("OpenCL kernel build failed:\n" + logStr);
        }

        IJ.log("[GPU Periodogram] Kernel compiled OK for " + backend + ".");
        return new ContextHolder(context, queue, program, device);
    }

    private static cl_device_id findDevice(String backend) {
        try {
            int[] nPlatforms = new int[1];
            clGetPlatformIDs(0, null, nPlatforms);
            if (nPlatforms[0] == 0) return null;

            cl_platform_id[] platforms = new cl_platform_id[nPlatforms[0]];
            clGetPlatformIDs(platforms.length, platforms, null);

            for (cl_platform_id platform : platforms) {
                int[] nDevices = new int[1];
                int err = clGetDeviceIDs(platform, CL_DEVICE_TYPE_GPU, 0, null, nDevices);
                if (err != CL_SUCCESS || nDevices[0] == 0) continue;

                cl_device_id[] devices = new cl_device_id[nDevices[0]];
                clGetDeviceIDs(platform, CL_DEVICE_TYPE_GPU, devices.length, devices, null);

                for (cl_device_id device : devices) {
                    String name = getDeviceString(device, CL_DEVICE_NAME).trim();
                    if (backend.contains(name)) return device;
                }
            }
        } catch (Throwable t) {
            IJ.log("[GPU Periodogram] findDevice error: " + t.getMessage());
        }
        return null;
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    static String getDeviceString(cl_device_id device, int param) {
        long[] size = new long[1];
        clGetDeviceInfo(device, param, 0, null, size);
        byte[] buf = new byte[(int) size[0]];
        clGetDeviceInfo(device, param, buf.length, Pointer.to(buf), null);
        return new String(buf, 0, buf.length - 1); // strip trailing NUL
    }

    // -------------------------------------------------------------------------
    // Context holder (immutable after construction)
    // -------------------------------------------------------------------------

    public static final class ContextHolder {
        public final cl_context context;
        public final cl_command_queue queue;
        public final cl_program program;
        public final cl_device_id device;

        ContextHolder(cl_context context, cl_command_queue queue, cl_program program, cl_device_id device) {
            this.context = context;
            this.queue   = queue;
            this.program = program;
            this.device  = device;
        }

        public void release() {
            try { clReleaseProgram(program); }      catch (Throwable ignored) {}
            try { clReleaseCommandQueue(queue); }   catch (Throwable ignored) {}
            try { clReleaseContext(context); }      catch (Throwable ignored) {}
        }
    }
}
