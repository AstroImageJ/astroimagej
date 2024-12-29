package Astronomy.multiaperture.io.transformers;

import Astronomy.multiaperture.io.Section;
import Astronomy.multiaperture.io.Transformer;

import java.awt.geom.AffineTransform;

public class AffineTransformTransformer implements Transformer<AffineTransform> {
    @Override
    public AffineTransform load(Section section) {
        var t = section.getParameter(0, "transformType");

        return switch (t) {
            case "rotate" -> {
                var theta = readDouble("angle", section.getParameter(1, "angle"));
                var unit = section.getParameter(2, "unit");
                var thetaRadians = switch (unit) {
                    case "degrees", "d" -> Math.toRadians(theta);
                    case "radians", "rad", "r" -> theta;
                    default -> throw new IllegalStateException("Unknown unit for angles: " + unit);
                };

                yield AffineTransform.getRotateInstance(thetaRadians);
            }
            case "identity" -> new AffineTransform();
            case "scale" -> {
                var scaleX = readDouble("scaleX", section.getParameter(1, "scaleX"));
                var scaleY = readDouble("scaleY", section.getParameter(2, "scaleY"));

                yield AffineTransform.getScaleInstance(scaleX, scaleY);
            }
            case "translate" -> {
                var dx = readDouble("dx", section.getParameter(1, "dx"));
                var dy = readDouble("dy", section.getParameter(2, "dy"));

                yield AffineTransform.getTranslateInstance(dx, dy);
            }
            case "matrix" -> {
                if (section.getSubSections().size() != 2) {
                    throw new IllegalStateException("Transform matrix must have 2 subsections (rows)");
                }

                var firstRow = section.getSubSections().get(0);
                var secondRow = section.getSubSections().get(1);

                var m = new double[6];

                m[0] = readDouble("m00", firstRow.name());
                m[1] = readDouble("m10", firstRow.getParameter(0, "shearY"));
                m[2] = readDouble("m01", firstRow.getParameter(1, "shearX"));
                m[3] = readDouble("m11", secondRow.name());
                m[4] = readDouble("m02", secondRow.getParameter(0, "translateX"));
                m[5] = readDouble("m12", secondRow.getParameter(1, "translateY"));

                yield new AffineTransform(m);
            }
            default -> throw new IllegalStateException("Unknown transformation of type: " + t);
        };
    }

    @Override
    public Section write(AffineTransform transform) {
        var type = type(transform);

        // Decomposing is difficult, so just handle the simple transforms
        return switch (type) {
            case "rotation" -> {
                var theta = Math.atan2(transform.getShearY(), transform.getScaleX());
                yield Section.createSection("transform", "rotate", Double.toString(Math.toDegrees(theta)), "d");
            }
            case "identity" -> {
                yield Section.createSection("transform", "identity");
            }
            case "scale" -> {
                yield Section.createSection("transform", "scale",
                        Double.toString(transform.getScaleX()), Double.toString(transform.getScaleY()));
            }
            case "translation" -> {
                yield Section.createSection("transform", "translate",
                        Double.toString(transform.getTranslateX()), Double.toString(transform.getTranslateY()));
            }
            case "matrix" -> {
                var s = Section.createSection("transform", "matrix");
                var m = new double[6];
                transform.getMatrix(m);
                s.addSubsection(Section.createSection(Double.toString(m[0]), Double.toString(m[1]), Double.toString(m[2])));
                s.addSubsection(Section.createSection(Double.toString(m[3]), Double.toString(m[4]), Double.toString(m[5])));
                yield s;
            }
            default -> {
                throw new IllegalStateException("Unknown transform type: " + type);
            }
        };
    }

    private String type(AffineTransform affineTransform) {
        if (affineTransform.isIdentity() ||
                isOnlyType(affineTransform, AffineTransform.TYPE_IDENTITY)) {
            return "identity";
        }

        if (isOnlyType(affineTransform, AffineTransform.TYPE_UNIFORM_SCALE) ||
                isOnlyType(affineTransform, AffineTransform.TYPE_GENERAL_SCALE)) {
            return "scale";
        }

        if (isOnlyType(affineTransform, AffineTransform.TYPE_TRANSLATION)) {
            return "translation";
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
            return "rotation";
        }

        if (isOnlyType(affineTransform, AffineTransform.TYPE_GENERAL_TRANSFORM)) {
            return "matrix";
        }

        return "matrix";
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
}
