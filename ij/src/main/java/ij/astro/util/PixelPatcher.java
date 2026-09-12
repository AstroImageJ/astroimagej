package ij.astro.util;

import ij.astro.gui.ToolTipProvider;
import ij.astro.io.pixel_maps.BpmFile;
import ij.astro.io.prefs.Property;
import ij.astro.logging.AIJLogger;
import ij.process.ImageProcessor;

import java.util.Collection;
import java.util.Map;

public interface PixelPatcher {
    Property<PatchType.Type> TYPE = new Property<>(PatchType.Type.PASS_THROUGH, PixelPatcher.class, t -> {
        if (t.disabled) {
            AIJLogger.log("Bad Pixel Map set to Pass Through as the previous option is disabled.");
            return PatchType.Type.PASS_THROUGH;
        }
        return t;
    });
    Property<Boolean> DISPLAY = new Property<>(true, PixelPatcher.class);
    /// If the BPM should be preserved for display.
    Property<Boolean> PRESERVE_BPM = new Property<>(true, PixelPatcher.class);
    Property<String> BPM_FILE_SOURCE = new Property<>("", PixelPatcher.class);
    Property<PatchTypeSource> BPM_MODE = new Property<>(PatchTypeSource.DISABLED, PixelPatcher.class);

    void patch(ImageProcessor ip, Mask mask);

    sealed interface PatchType {
        Property.PropertyLoadValidator<Integer> PIXEL_RANGE = (d) -> (d < 0) ? 0 : d;
        Property.PropertyChangeValidator<Integer> PIXEL_RANGE_CHANGE = (_, o, n) -> (n < 0) ? o : n;

        record PassThrough() implements PatchType {}
        record FitPlane() implements PatchType {}
        record FitGaussian(int minCount, int maxIter, double relErr, double absErr) implements PatchType {
            public static final Property<Integer> MIN_COUNT = new Property<>(20, FitGaussian.class);
            public static final Property<Integer> MAX_ITER = new Property<>(3000, FitGaussian.class);
            public static final Property<Double> REL_ERR = new Property<>(1e-8, FitGaussian.class);
            public static final Property<Double> ABS_ERR = new Property<>(1e-8, FitGaussian.class);

            public FitGaussian() {
                this(MIN_COUNT.get(), MAX_ITER.get(), REL_ERR.get(), ABS_ERR.get());
            }
        }
        record FitMoffat(int minCount, int maxIter, double relErr, double absErr) implements PatchType {
            public static final Property<Integer> MIN_COUNT = new Property<>(20, FitMoffat.class);
            public static final Property<Integer> MAX_ITER = new Property<>(3000, FitMoffat.class);
            public static final Property<Double> REL_ERR = new Property<>(1e-8, FitMoffat.class);
            public static final Property<Double> ABS_ERR = new Property<>(1e-8, FitMoffat.class);

            public FitMoffat() {
                this(MIN_COUNT.get(), MAX_ITER.get(), REL_ERR.get(), ABS_ERR.get());
            }
        }
        record NearestNeighbor(MergeType mergeType) implements PatchType {
            public static final Property<MergeType> MERGE_TYPE = new Property<>(MergeType.NEAREST_NEIGHBOR, NearestNeighbor.class);

            public NearestNeighbor() {
                this(MERGE_TYPE.get());
            }

            public enum MergeType implements ToolTipProvider {
                NEAREST_NEIGHBOR,
                MEDIAN,
                AVERAGE,
                ;


                @Override
                public String getToolTip() {
                    return "test";
                }
            }
        }
        record ConstantValue(double value) implements PatchType {
            public static final Property<Double> VALUE = new Property<>(Double.NaN, ConstantValue.class);

            public ConstantValue() {
                this(VALUE.get());
            }
        }
        record FloodFill(boolean useMedian) implements PatchType {
            public static final Property<Boolean> USE_MEDIAN = new Property<>(false, FloodFill.class);

            public FloodFill() {
                this(USE_MEDIAN.get());
            }
        }
        record AverageFill(int xRadius, int yRadius) implements PatchType {
            public static final Property<Integer> X_RADIUS = new Property<>(0, AverageFill.class);
            public static final Property<Integer> Y_RADIUS = new Property<>(0, AverageFill.class);

            static {
                X_RADIUS.setLoadValidator(PIXEL_RANGE);
                X_RADIUS.setChangeValidator(PIXEL_RANGE_CHANGE);
                Y_RADIUS.setLoadValidator(PIXEL_RANGE);
                Y_RADIUS.setChangeValidator(PIXEL_RANGE_CHANGE);
            }

            public AverageFill() {
                this(X_RADIUS.get(), Y_RADIUS.get());
            }
        }
        record MedianFill(int xRadius, int yRadius) implements PatchType {
            public static final Property<Integer> X_RADIUS = new Property<>(0, MedianFill.class);
            public static final Property<Integer> Y_RADIUS = new Property<>(0, MedianFill.class);

            static {
                X_RADIUS.setLoadValidator(PIXEL_RANGE);
                X_RADIUS.setChangeValidator(PIXEL_RANGE_CHANGE);
                Y_RADIUS.setLoadValidator(PIXEL_RANGE);
                Y_RADIUS.setChangeValidator(PIXEL_RANGE_CHANGE);
            }

            public MedianFill() {
                this(X_RADIUS.get(), Y_RADIUS.get());
            }
        }
        //todo PSF option

        enum Type {
            AVERAGE_FILL,
            MEDIAN_FILL,
            FLOOD_FILL,
            FIT_PLANE,
            CONSTANT_VALUE,
            NEAREST_NEIGHBOR,
            PASS_THROUGH,
            FIT_GAUSSIAN(true),
            FIT_MOFFAT(true),
            ;

            public final boolean disabled;

            Type() {
                this(false);
            }

            Type(boolean disabled) {
                this.disabled = disabled;
            }

            public PatchType toPatchType() {
                return switch (this) {
                    case AVERAGE_FILL -> new AverageFill();
                    case MEDIAN_FILL -> new MedianFill();
                    case FLOOD_FILL -> new FloodFill();
                    case FIT_PLANE -> new FitPlane();
                    case CONSTANT_VALUE -> new ConstantValue();
                    case NEAREST_NEIGHBOR -> new NearestNeighbor();
                    case PASS_THROUGH -> new PassThrough();
                    case FIT_GAUSSIAN -> new FitGaussian();
                    case FIT_MOFFAT -> new FitMoffat();
                };
            }
        }
    }

    enum PatchTypeSource {
        DISABLED,
        LCO_FILE,
        BPM_FILE,
        ;
    }

    sealed interface Mask permits Mask.IPMask, Mask.ListMask {
        record IPMask(ImageProcessor mask) implements Mask {
            @Override
            public boolean isBadPixel(int x, int y) {
                return mask.getf(x, y) > 0;
            }

            @Override
            public PatchType getPatchType(int x, int y) {
                return isBadPixel(x, y) ? TYPE.get().toPatchType() : null;
            }

            @Override
            public boolean skip() {
                return TYPE.get().toPatchType() instanceof PatchType.PassThrough;
            }
        }

        record ListMask(Map<PatchType.Type, Collection<Pixel>> masks) implements Mask {
            public ListMask(BpmFile bpm) {
                this(bpm.patches());
            }

            @Override
            public boolean isBadPixel(int x, int y) {
                var pixel = new Pixel(x, y);
                for (var patchEntry : masks.entrySet()) {
                    if (patchEntry.getValue().contains(pixel)) {
                        return true;
                    }
                }

                return false;
            }

            @Override
            public PatchType getPatchType(int x, int y) {
                var pixel = new Pixel(x, y);
                for (var patchEntry : masks.entrySet()) {
                    if (patchEntry.getValue().contains(pixel)) {
                        return patchEntry.getKey().toPatchType();
                    }
                }

                return null;
            }

            @Override
            public boolean skip() {
                return masks().isEmpty() || masks.keySet().stream().allMatch(t -> t == PatchType.Type.PASS_THROUGH);
            }
        }

        boolean isBadPixel(int x, int y);

        PatchType getPatchType(int x, int y);

        boolean skip();
    }

    record Pixel(int x, int y) {}
}
