package Astronomy.multiaperture.io.transformers;

import Astronomy.multiaperture.io.Section;
import Astronomy.multiaperture.io.Transformer;
import Astronomy.multiaperture.io.Transformers;

import java.awt.geom.AffineTransform;

public class AffineTransformTransformer implements Transformer<AffineTransform, Void> {
    private static final Section.Parameter<TransformationType> TYPE_PARAMETER =
            new Section.Parameter<>("transformType", 0, TransformationType.class,
                    AffineTransformTransformer::typeFromString, AffineTransformTransformer::typeToString);
    private static final Section.Parameter<Double> X_PARAMETER = new Section.Parameter<>("x", 1, Double.class);
    private static final Section.Parameter<Double> Y_PARAMETER = new Section.Parameter<>("y", 2, Double.class);
    private static final Section.Parameter<Double> THETA_PARAMETER = new Section.Parameter<>("theta", 1, Double.class);
    private static final Section.Parameter<String> THETA_UNIT_PARAMETER = new Section.Parameter<>("angle unit", 2, String.class);

    @Override
    public AffineTransform load(Void params, Section section) {
        var t = section.getParameter(TYPE_PARAMETER);

        return switch (t) {
            case ROTATE -> {
                var theta = section.getParameter(THETA_PARAMETER);
                var unit = section.getParameter(THETA_UNIT_PARAMETER);//todo type safe?
                var thetaRadians = switch (unit) {
                    case "degrees", "d" -> Math.toRadians(theta);
                    case "radians", "rad", "r" -> theta;
                    default -> throw new IllegalStateException("Unknown unit for angles: " + unit);
                };

                yield AffineTransform.getRotateInstance(thetaRadians);
            }
            case IDENTITY -> new AffineTransform();
            case SCALE -> {
                var scaleX = section.getParameter(X_PARAMETER);
                var scaleY = section.getParameter(Y_PARAMETER);

                yield AffineTransform.getScaleInstance(scaleX, scaleY);
            }
            case TRANSLATE -> {
                var dx = section.getParameter(X_PARAMETER);
                var dy = section.getParameter(Y_PARAMETER);

                yield AffineTransform.getTranslateInstance(dx, dy);
            }
            case GENERAL -> {
                var m = Transformers.read(double[].class, section, new FlatMatrixTransformer.Dimensions(3, 2));

                yield new AffineTransform(m);
            }
            default -> throw new IllegalStateException("Unknown transformation of type: " + t);
        };
    }

    @Override
    public Section write(Void params, AffineTransform transform) {
        var type = type(transform);

        // Decomposing is difficult, so just handle the simple transforms
        return switch (type) {
            case ROTATE -> {
                var theta = Math.atan2(transform.getShearY(), transform.getScaleX());
                yield Section.createSection("transform",
                        TYPE_PARAMETER, type, THETA_PARAMETER, Math.toDegrees(theta),
                        THETA_UNIT_PARAMETER, "d");
            }
            case IDENTITY -> {
                yield Section.createSection("transform", TYPE_PARAMETER, type);
            }
            case SCALE -> {
                yield Section.createSection("transform", TYPE_PARAMETER, type,
                        X_PARAMETER, transform.getScaleX(), Y_PARAMETER, transform.getScaleY());
            }
            case TRANSLATE -> {
                yield Section.createSection("transform", TYPE_PARAMETER, type,
                        X_PARAMETER, transform.getTranslateX(), Y_PARAMETER, transform.getTranslateY());
            }
            case GENERAL -> {
                var s = Section.createSection("transform", TYPE_PARAMETER, type);
                var m = new double[6];
                transform.getMatrix(m);

                s.addSubsection(Transformers.write(double[].class, m, new FlatMatrixTransformer.Dimensions(3, 2)));
                yield s;
            }
        };
    }

    private TransformationType type(AffineTransform affineTransform) {
        if (affineTransform.isIdentity() ||
                isOnlyType(affineTransform, AffineTransform.TYPE_IDENTITY)) {
            return TransformationType.IDENTITY;
        }

        if (isOnlyType(affineTransform, AffineTransform.TYPE_UNIFORM_SCALE) ||
                isOnlyType(affineTransform, AffineTransform.TYPE_GENERAL_SCALE)) {
            return TransformationType.SCALE;
        }

        if (isOnlyType(affineTransform, AffineTransform.TYPE_TRANSLATION)) {
            return TransformationType.TRANSLATE;
        }

        /*if (isOnlyType(affineTransform, AffineTransform.TYPE_FLIP)) {
            return "flip";
        }*/

        /*if (isOnlyType(affineTransform, AffineTransform.TYPE_MASK_SCALE)) {
            // Disabled as it is combination of other types
        }*/

        /*if (isOnlyType(affineTransform, AffineTransform.TYPE_MASK_ROTATION)) {
            // Disabled as it is combination of other types
        }*/

        if (isOnlyType(affineTransform, AffineTransform.TYPE_GENERAL_ROTATION) ||
                isOnlyType(affineTransform, AffineTransform.TYPE_QUADRANT_ROTATION)) {
            return TransformationType.ROTATE;
        }

        if (isOnlyType(affineTransform, AffineTransform.TYPE_GENERAL_TRANSFORM)) {
            return TransformationType.GENERAL;
        }

        return TransformationType.GENERAL;
    }

    private static boolean isOnlyType(AffineTransform transform, int type) {
        return transform.getType() == type;
    }


    private static boolean ofTypes(AffineTransform transform, int... types) {
        var mask = 0;
        for (int i : types) {
            mask |= i;
        }

        return (transform.getType() & mask) == mask;
    }

    private static TransformationType typeFromString(Section.Parameter<TransformationType> parameter, String s) {
        return switch (s) {
            case "rotate" -> TransformationType.ROTATE;
            case "identity" -> TransformationType.IDENTITY;
            case "scale" -> TransformationType.SCALE;
            case "translate" -> TransformationType.TRANSLATE;
            case "matrix" -> TransformationType.GENERAL;
            default -> throw new IllegalStateException("Unknown transformation of type '%s' for parameter '%s'"
                    .formatted(s, parameter.name()));
        };
    }

    private static String typeToString(Section.Parameter<TransformationType> parameter, TransformationType transformationType) {
        return switch (transformationType) {
            case IDENTITY -> "identity";
            case SCALE -> "scale";
            case ROTATE -> "rotate";
            case TRANSLATE -> "translate";
            case GENERAL -> "matrix";
        };
    }

    private enum TransformationType {
        IDENTITY,
        SCALE,
        ROTATE,
        TRANSLATE,
        GENERAL,
        //FLIP,
    }

}
