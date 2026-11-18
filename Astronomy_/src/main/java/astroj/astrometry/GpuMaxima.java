package astroj.astrometry;

import astroj.Astrometry;
import ij.IJ;
import ij.plugin.filter.RankFilters;
import ij.process.FloatProcessor;
import uk.ac.manchester.tornado.api.TaskGraph;
import uk.ac.manchester.tornado.api.TornadoExecutionPlan;
import uk.ac.manchester.tornado.api.enums.DataTransferMode;
import uk.ac.manchester.tornado.api.exceptions.TornadoBailoutRuntimeException;

import java.awt.*;
import java.util.Arrays;

public final class GpuMaxima {
    /// Value written to the type map for a local maximum
    public static final byte MAXIMUM = 1;
    private static volatile boolean gpuFailed = false;
    private static Plan cached;

    private GpuMaxima() {
    }

    /// @param src          the slice as floats (read only)
    /// @param roi          the region of interest
    /// @param medianRadius \> 0 puts a circular median of that radius, over the whole image, in front of the search
    @SuppressWarnings("resource")
    public static synchronized Result find(float[] src, int width, int height, Rectangle roi, double medianRadius) {
        final Key key = new Key(width, height, roi.x, roi.y, roi.width, roi.height, medianRadius);
        final boolean cpu = gpuFailed || medianRadius > StarKernels.MAX_MEDIAN_RADIUS || !Astrometry.USE_GPU_ACCELERATION.get();
        try {
            return plan(key, cpu).run(src);
        } catch (TornadoBailoutRuntimeException bailout) {
            bailout.printStackTrace();
            IJ.log("[GPU star detection] Could not initialize GPU environment - running on the CPU instead.");
            gpuFailed = true;
            release();
            return plan(key, true).run(src);
        } catch (Throwable t) {
            if (cpu) {
                // the CPU run failed
                throw t;
            }
            var cause = t instanceof ExceptionInInitializerError && t.getCause() != null ? t.getCause() : t;
            cause.printStackTrace();
            IJ.log("[GPU star detection] " + cause + " - running on the CPU instead.");
            gpuFailed = true;
            release();
            return plan(key, true).run(src);
        }
    }

    /// Frees device memory and forgets the cached plan.
    public static synchronized void release() {
        if (cached != null) {
            cached.close();
            cached = null;
        }
    }

    private static Plan plan(Key key, boolean cpu) {
        if (cached != null && cached.cpu == cpu && cached.key.equals(key)) {
            return cached;
        }
        release();
        cached = new Plan(key, cpu);
        return cached;
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
        final boolean cpu;
        final boolean median;
        final byte[] types;
        /// median output, if enabled
        final float[] filtered;
        /// GPU only - Tornado binds to array objects, so the slice is staged here
        final float[] pixels;
        final int[] rows;
        final TornadoExecutionPlan plan;

        Plan(Key k, boolean cpu) {
            key = k;
            this.cpu = cpu;
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
            plan = new TornadoExecutionPlan(g.snapshot());
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
