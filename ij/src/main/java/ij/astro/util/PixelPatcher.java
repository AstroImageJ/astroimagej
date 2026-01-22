package ij.astro.util;

import ij.astro.gui.ToolTipProvider;
import ij.astro.io.prefs.Property;
import ij.process.ImageProcessor;

public interface PixelPatcher {
    Property<PatchType.Type> TYPE = new Property<>(PatchType.Type.CONSTANT_VALUE, PixelPatcher.class);

    void patch(ImageProcessor ip, ImageProcessor mask, PatchType patchType);

    sealed interface PatchType {
        Property.PropertyLoadValidator<Integer> PIXEL_RANGE = (d) -> (d < 0) ? 0 : d;
        Property.PropertyChangeValidator<Integer> PIXEL_RANGE_CHANGE = (_, o, n) -> (n < 0) ? o : n;

        record PassThrough() implements PatchType {}
        record FitPlane() implements PatchType {}
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
        //todo gaussian fit option

        enum Type {
            AVERAGE_FILL,
            MEDIAN_FILL,
            FLOOD_FILL,
            FIT_PLANE,
            CONSTANT_VALUE,
            NEAREST_NEIGHBOR,
            PASS_THROUGH,
            ;

            public PatchType toPatchType() {
                return switch (this) {
                    case AVERAGE_FILL -> new AverageFill();
                    case MEDIAN_FILL -> new MedianFill();
                    case FLOOD_FILL -> new FloodFill();
                    case FIT_PLANE -> new FitPlane();
                    case CONSTANT_VALUE -> new ConstantValue();
                    case NEAREST_NEIGHBOR -> new NearestNeighbor();
                    case PASS_THROUGH -> new PassThrough();
                };
            }
        }
    }
}
