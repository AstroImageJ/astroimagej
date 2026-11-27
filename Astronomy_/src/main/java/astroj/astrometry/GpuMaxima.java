package astroj.astrometry;

import astroj.Astrometry;
import ij.IJ;
import ij.astro.io.prefs.Property;
import ij.plugin.filter.RankFilters;
import ij.process.FloatProcessor;
import uk.ac.manchester.tornado.api.TaskGraph;
import uk.ac.manchester.tornado.api.TornadoExecutionPlan;
import uk.ac.manchester.tornado.api.enums.DataTransferMode;
import uk.ac.manchester.tornado.api.enums.TornadoDeviceType;
import uk.ac.manchester.tornado.api.exceptions.TornadoBailoutRuntimeException;
import uk.ac.manchester.tornado.api.runtime.TornadoRuntimeProvider;
import uk.ac.manchester.tornado.drivers.opencl.OCLTargetDevice;

import java.awt.*;
import java.util.*;
import java.util.List;

public final class GpuMaxima {
    /// Value written to the type map for a local maximum
    public static final byte MAXIMUM = 1;
    /// Device+backend to run on. Empty string -> Auto mode
    public static final Property<String> GPU_DEVICE = new Property<>("", GpuMaxima.class);
    /// ID of the device that failed at runtime; it is not retried this session, but choosing another one is.
    private static volatile String failedId;
    private static List<Device> devices;
    private static String warnedMissing = "";
    private static Plan cached;

    private GpuMaxima() {
    }

    public static synchronized Device preferred() {
        return devices().isEmpty() ? null : devices().getFirst();
    }

    /// Discover all devices
    public static synchronized List<Device> devices() {
        if (devices != null) {
            return devices;
        }

        var found = new ArrayList<Device>();
        var seen = new HashMap<String, Integer>();
        try {
            var rt = TornadoRuntimeProvider.getTornadoRuntime();
            var count = rt.getNumBackends();

            if (count == 0) {
                IJ.log("[GPU star detection] No TornadoVM backends are installed - running on the CPU instead.");
            }

            for (int i = 0; i < count; i++) {
                var type = "backend " + i;
                try {
                    type = String.valueOf(rt.getBackendType(i));
                    var backend = rt.getBackend(i); // This can throw in some cases
                    for (int j = 0; j < backend.getNumDevices(); j++) {
                        var tornadoDevice = TornadoExecutionPlan.getDevice(i, j);
                        var physicalDevice = tornadoDevice.getPhysicalDevice();

                        var available = switch (physicalDevice) {
                            case OCLTargetDevice oclTargetDevice -> oclTargetDevice.isDeviceAvailable();
                            default -> true;
                        };

                        if (!available) {
                            continue;
                        }

                        // TornadoDevice.getDeviceName() is an index based id like "opencl-0-0"; the hardware name
                        // ("NVIDIA GeForce ...") is on the physical device
                        var name = physicalDevice.getDeviceName();
                        var base = type + "|" + name;
                        // Keep track of multiple of the same device
                        var n = seen.merge(base, 1, Integer::sum) - 1;

                        var isGpu = tornadoDevice.getDeviceType() == TornadoDeviceType.GPU;
                        long cu = 0;
                        long freq = 0;
                        long mem = 0;
                        try {
                            cu = physicalDevice.getDeviceMaxComputeUnits();
                            freq = Math.max(1, physicalDevice.getDeviceMaxClockFrequency());
                            mem = physicalDevice.getDeviceGlobalMemorySize();
                        } catch (Throwable _) {
                        }
                        found.add(new Device(i, j, type, name, n == 0 ? base : base + "#" + n, isGpu, cu * freq, mem));
                    }
                } catch (Throwable t) {
                    var cause = t instanceof ExceptionInInitializerError && t.getCause() != null ? t.getCause() : t;
                    var msg = new StringBuilder("[GPU star detection] ")
                            .append(type)
                            .append(" backend disabled: ")
                            .append(cause.getMessage());
                    if (type.equalsIgnoreCase("OPENCL")) {
                        msg.append(". Install an OpenCL runtime for your device");
                    }
                    IJ.log(msg.toString());
                }
            }

            if (found.isEmpty() && count > 0) {
                IJ.log("[GPU star detection] No usable GPU device - running on the CPU instead.");
            }
        } catch (Throwable t) {
            t.printStackTrace();
            IJ.log("[GPU star detection] Could not query TornadoVM - running on the CPU instead.");
        }

        devices = found
                .stream()
                .sorted(Comparator
                        .comparing(Device::gpu)
                        .thenComparingLong(Device::computeScore)
                        .thenComparingLong(Device::memory)
                        .thenComparing(d -> -d.backend() * 1000 - d.device())
                        .reversed() // Descending order
                ).toList();

        return devices;
    }

    /// The device to run on, or `null` falling back to CPU.
    private static synchronized Device selected() {
        var all = devices();
        if (all.isEmpty()) {
            return null;
        }

        var want = GPU_DEVICE.get();
        if (want != null && !want.isEmpty()) {
            for (var d : all) {
                if (d.id().equals(want)) {
                    return d;
                }
            }
            if (!want.equals(warnedMissing)) {
                warnedMissing = want;
                var b = preferred() != null ? preferred().label() : "CPU";
                IJ.log("[GPU star detection] Selected device '" + want + "' not found - using " + b + " instead.");
            }
        }

        return preferred();
    }

    /// @param src          the slice as floats (read only)
    /// @param roi          the region of interest
    /// @param medianRadius \> 0 puts a circular median of that radius, over the whole image, in front of the search
    @SuppressWarnings("resource")
    public static synchronized Result find(float[] src, int width, int height, Rectangle roi, double medianRadius) {
        final Key key = new Key(width, height, roi.x, roi.y, roi.width, roi.height, medianRadius);
        var device = Astrometry.USE_GPU_ACCELERATION.get() && medianRadius <= StarKernels.MAX_MEDIAN_RADIUS ? usableDevice() : null;
        try {
            return plan(key, device).run(src);
        } catch (TornadoBailoutRuntimeException bailout) {
            bailout.printStackTrace();
            IJ.log("[GPU star detection] Could not initialize GPU environment - running on the CPU instead.");
            return fallBackToCpu(key, device, src);
        } catch (Throwable t) {
            if (device == null) {
                // the CPU run failed
                throw t;
            }
            var cause = t instanceof ExceptionInInitializerError && t.getCause() != null ? t.getCause() : t;
            cause.printStackTrace();
            IJ.log("[GPU star detection] " + cause + " - running on the CPU instead.");
            return fallBackToCpu(key, device, src);
        }
    }

    private static Result fallBackToCpu(Key key, Device failed, float[] src) {
        failedId = failed == null ? null : failed.id();
        release();
        return plan(key, null).run(src);
    }

    /// The selected device unless it already failed this session.
    private static Device usableDevice() {
        var d = selected();
        return d == null || d.id().equals(failedId) ? null : d;
    }

    /// Frees device memory and forgets the cached plan.
    public static synchronized void release() {
        if (cached != null) {
            cached.close();
            cached = null;
        }
    }

    /// @param device the device to run on, or null for the CPU
    private static Plan plan(Key key, Device device) {
        if (cached != null && Objects.equals(cached.device, device) && cached.key.equals(key)) {
            return cached;
        }
        release();
        cached = new Plan(key, device);
        return cached;
    }

    /// @param backend       index of the backend
    /// @param device        index of the device within the backend
    /// @param backendType   backend type name, e.g. OPENCL
    /// @param name          device name reported by the driver
    /// @param id            stable identity: type, name, and an ordinal if several identical devices exist
    /// @param gpu           reported as a GPU
    /// @param computeScore  estimated computing power
    /// @param memory        max global memory in bytes
    public record Device(int backend, int device, String backendType, String name, String id,
                         boolean gpu, long computeScore, long memory) {
        public String label() {
            return backendType + " - " + name;
        }
    }

    /// @param image pixels the maxima were found in: the median filtered slice, or the input itself when the median is off
    /// @param types MAXIMUM for every candidate inside the ROI (edge pixels excluded), else 0; same size as image
    /// @param gpu   whether the GPU did the work
    public record Result(float[] image, byte[] types, boolean gpu) {
    }

    private record Key(int width, int height, int rx, int ry, int rw, int rh, double medianRadius) {
    }

    private static final class Plan implements AutoCloseable {
        final Key key;
        /// `null` when running on the CPU
        final Device device;
        final boolean cpu;
        final boolean median;
        final byte[] types;
        /// median output, if enabled
        final float[] filtered;
        /// GPU only - Tornado binds to array objects, so the slice is staged here
        final float[] pixels;
        final int[] rows;
        final TornadoExecutionPlan plan;

        Plan(Key k, Device device) {
            key = k;
            this.device = device;
            this.cpu = device == null;
            median = k.medianRadius() > 0;
            final int n = k.width() * k.height();
            types = new byte[n];
            filtered = median ? new float[n] : null; // the median modifies the image, so we need a copy
            if (cpu) {
                pixels = null;
                rows = null;
                plan = null;
                return;
            }
            IJ.showStatus("Creating GPU kernel...");
            pixels = new float[n];
            rows = median ? StarKernels.medianRows(k.medianRadius()) : null;

            var g = new TaskGraph("astrometryMaxima");
            g.transferToDevice(DataTransferMode.EVERY_EXECUTION, (Object) pixels);
            float[] img = pixels;
            if (median) {
                g.transferToDevice(DataTransferMode.FIRST_EXECUTION, (Object) rows); // constant for this plan
                g.task("median", StarKernels::medianFilter, pixels, filtered, rows, k.width(), k.height(), (rows.length - 1) / 2);
                img = filtered;
            }
            g.task("markMaxima", StarKernels::markMaxima, img, types, k.width(), k.height(), k.rx(), k.ry(), k.rw(), k.rh());
            if (median) {
                g.transferToHost(DataTransferMode.EVERY_EXECUTION, filtered, types);
            } else {
                g.transferToHost(DataTransferMode.EVERY_EXECUTION, (Object) types);
            }
            plan = new TornadoExecutionPlan(g.snapshot())
                    .withDevice(TornadoExecutionPlan.getDevice(device.backend(), device.device()));
        }

        Result run(float[] src) {
            final int w = key.width(), h = key.height();
            if (src.length != w * h) {
                throw new IllegalArgumentException("slice size does not match " + w + "x" + h);
            }
            final float[] image;
            if (cpu) {
                float[] img = src;
                if (median) {
                    // IJ's version is faster on the CPU
                    System.arraycopy(src, 0, filtered, 0, src.length);
                    new RankFilters().rank(new FloatProcessor(w, h, filtered), key.medianRadius(), RankFilters.MEDIAN);
                    img = filtered;
                }
                StarKernels.markMaxima(img, types, w, h, key.rx(), key.ry(), key.rw(), key.rh());
                image = img;
            } else {
                System.arraycopy(src, 0, pixels, 0, src.length);
                plan.execute();
                image = median ? filtered : src; // nothing to download without median
                // Test GPU version matches IJ
                if (false) {
                    var test = Arrays.copyOf(src, src.length);
                    new RankFilters().rank(new FloatProcessor(w, h, test), key.medianRadius(), RankFilters.MEDIAN);
                    if (!Arrays.equals(filtered, test)) {
                        for (int i = 0, shown = 0; i < test.length && shown < 10; i++) {
                            if (Float.floatToIntBits(test[i]) != Float.floatToIntBits(filtered[i])) {
                                IJ.log("median mismatch at x=" + (i % w) + " y=" + (i / w) + " cpu=" + test[i] + " gpu=" + filtered[i]);
                                shown++;
                            }
                        }
                    }
                }
            }
            return new Result(image, types, !cpu);
        }

        @Override
        public void close() {
            try {
                if (plan != null) {
                    plan.close();
                }
            } catch (Throwable _) {
            }
        }
    }
}
