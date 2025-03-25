package Astronomy.multiaperture.io.transformers;

import Astronomy.multiaperture.CompositeShape;
import Astronomy.multiaperture.TransformedShape;
import Astronomy.multiaperture.io.Section;
import Astronomy.multiaperture.io.Transformer;
import Astronomy.multiaperture.io.Transformers;
import ij.astro.types.MultiMap;

import java.awt.*;
import java.awt.geom.*;
import java.util.List;

public class ShapeTransformer implements Transformer<Shape, Void> {
    public static final List<String> SHAPE_SECTION_NAMES = List.of("ellipse", "rectangle", "composite", "roundedRectangle", "path");
    private static final Section.Parameter<Double> X_PARAMETER = new Section.Parameter<>("x", 0, Double.class);
    private static final Section.Parameter<Double> Y_PARAMETER = new Section.Parameter<>("y", 1, Double.class);
    private static final Section.Parameter<Double> QUAD_CTRL_X_PARAMETER = new Section.Parameter<>("ctrlX", 0, Double.class);
    private static final Section.Parameter<Double> QUAD_CTRL_Y_PARAMETER = new Section.Parameter<>("ctrlY", 1, Double.class);
    private static final Section.Parameter<Double> QUAD_END_X_PARAMETER = new Section.Parameter<>("endX", 2, Double.class);
    private static final Section.Parameter<Double> QUAD_END_Y_PARAMETER = new Section.Parameter<>("endY", 3, Double.class);
    private static final Section.Parameter<Double> CUBIC_CTRL1_X_PARAMETER = new Section.Parameter<>("ctrl1X", 0, Double.class);
    private static final Section.Parameter<Double> CUBIC_CTRL1_Y_PARAMETER = new Section.Parameter<>("ctrl1Y", 1, Double.class);
    private static final Section.Parameter<Double> CUBIC_CTRL2_X_PARAMETER = new Section.Parameter<>("ctrl2X", 2, Double.class);
    private static final Section.Parameter<Double> CUBIC_CTRL2_Y_PARAMETER = new Section.Parameter<>("ctrl2Y", 3, Double.class);
    private static final Section.Parameter<Double> CUBIC_END_X_PARAMETER = new Section.Parameter<>("endX", 4, Double.class);
    private static final Section.Parameter<Double> CUBIC_END_Y_PARAMETER = new Section.Parameter<>("endY", 5, Double.class);

    @Override
    public Shape load(Void params, Section section) {
        var view = section.createMapView();
        var shape = switch (section.name()) {
            case "ellipse" -> {
                var centerSec = getUniqueSection(view, "center");
                var radiiSec = getUniqueSection(view, "radii");

                var centerX = centerSec.getParameter(X_PARAMETER);
                var centerY = centerSec.getParameter(Y_PARAMETER);
                var radiusX = radiiSec.getParameter(X_PARAMETER);
                var radiusY = radiiSec.getParameter(Y_PARAMETER);

                yield new Ellipse2D.Double(centerX - radiusX, centerY - radiusY, 2 * radiusX, 2 * radiusY);
            }
            case "rectangle" -> {
                var centerSec = getUniqueSection(view, "center");
                var radiiSec = getUniqueSection(view, "radii");

                var centerX = centerSec.getParameter(X_PARAMETER);
                var centerY = centerSec.getParameter(Y_PARAMETER);
                var radiusX = radiiSec.getParameter(X_PARAMETER);
                var radiusY = radiiSec.getParameter(Y_PARAMETER);

                yield new Rectangle2D.Double(centerX - radiusX, centerY - radiusY, 2 * radiusX, 2 * radiusY);
            }
            case "composite" -> {
                yield Transformers.read(CompositeShape.class, section);
            }
            case "roundedRectangle" -> {
                var centerSec = getUniqueSection(view, "center");
                var radiiSec = getUniqueSection(view, "radii");
                var cornerRadiiSec = getUniqueSection(view, "cornerRadii");

                var centerX = centerSec.getParameter(X_PARAMETER);
                var centerY = centerSec.getParameter(Y_PARAMETER);
                var radiusX = radiiSec.getParameter(X_PARAMETER);
                var radiusY = radiiSec.getParameter(Y_PARAMETER);
                var cornerRadiusX = cornerRadiiSec.getParameter(X_PARAMETER);
                var cornerRadiusY = cornerRadiiSec.getParameter(Y_PARAMETER);

                yield new RoundRectangle2D.Double(centerX - radiusX, centerY - radiusY,
                        2 * radiusX, 2 * radiusY, 2 * cornerRadiusX, 2 * cornerRadiusY);

            }
            case "path" -> {
                var p = new Path2D.Double();
                for (Section subSection : section.getSubSections()) {
                    switch (subSection.name()) {
                        case "moveTo" -> {
                            var startX = subSection.getParameter(X_PARAMETER);
                            var startY = subSection.getParameter(Y_PARAMETER);

                            p.moveTo(startX, startY);
                        }
                        case "lineTo" -> {
                            var endX = subSection.getParameter(X_PARAMETER);
                            var endY = subSection.getParameter(Y_PARAMETER);

                            p.lineTo(endX, endY);
                        }
                        case "quadTo" -> {
                            var ctrlX = subSection.getParameter(QUAD_CTRL_X_PARAMETER);
                            var ctrlY = subSection.getParameter(QUAD_CTRL_Y_PARAMETER);
                            var endX = subSection.getParameter(QUAD_END_X_PARAMETER);
                            var endY = subSection.getParameter(QUAD_END_Y_PARAMETER);

                            p.quadTo(ctrlX, ctrlY, endX, endY);
                        }
                        case "cubicTo" -> {
                            var ctrl1X = subSection.getParameter(CUBIC_CTRL1_X_PARAMETER);
                            var ctrl1Y = subSection.getParameter(CUBIC_CTRL1_Y_PARAMETER);
                            var ctrl2X = subSection.getParameter(CUBIC_CTRL2_X_PARAMETER);
                            var ctrl2Y = subSection.getParameter(CUBIC_CTRL2_Y_PARAMETER);
                            var endX = subSection.getParameter(CUBIC_END_X_PARAMETER);
                            var endY = subSection.getParameter(CUBIC_END_Y_PARAMETER);

                            p.curveTo(ctrl1X, ctrl1Y, ctrl2X, ctrl2Y, endX, endY);
                        }
                        case "close" -> {
                            p.closePath();
                        }
                        case "transform" -> {
                            // NO-OP
                        }
                        default -> throw new IllegalStateException("Unknown path element: " + subSection.name());
                    }
                }

                yield p;
            }
            default -> throw new IllegalStateException("Unknown shape of type: " + section.name());
        };

        var transformerSecs = view.get("transform");

        if (!transformerSecs.isEmpty()) {
            var transform = Transformers.read(AffineTransform.class, transformerSecs.get(0));
            for (int i = 1; i < transformerSecs.size(); i++) {
                transform.concatenate(Transformers.read(AffineTransform.class, transformerSecs.get(i)));
            }

            return new TransformedShape(shape, transform);
        }

        return shape;
    }

    @Override
    public Section write(Void params, Shape shape) {
        Section s;

        if (shape instanceof Ellipse2D e) {
            s = new Section("ellipse");

            s.addSubsection(Section.createSection("center",
                    X_PARAMETER, e.getCenterX(), Y_PARAMETER, e.getCenterY()));
            s.addSubsection(Section.createSection("radii",
                    X_PARAMETER, e.getWidth()/2.0, Y_PARAMETER, e.getHeight()/2.0));
        } else if (shape instanceof Rectangle2D r) {
            s = new Section("rectangle");

            s.addSubsection(Section.createSection("center",
                    X_PARAMETER, r.getCenterX(), Y_PARAMETER, r.getCenterY()));
            s.addSubsection(Section.createSection("radii",
                    X_PARAMETER, r.getWidth()/2.0, Y_PARAMETER, r.getHeight()/2.0));
        } else if (shape instanceof RoundRectangle2D r) {
            s = new Section("roundedRectangle");

            s.addSubsection(Section.createSection("center",
                    X_PARAMETER, r.getCenterX(), Y_PARAMETER, r.getCenterY()));
            s.addSubsection(Section.createSection("radii",
                    X_PARAMETER, r.getWidth()/2.0, Y_PARAMETER, r.getHeight()/2.0));
            s.addSubsection(Section.createSection("cornerRadii",
                    X_PARAMETER, r.getArcWidth()/2.0, Y_PARAMETER, r.getArcHeight()/2.0));
        } else if (shape instanceof CompositeShape t) {
            s = Transformers.write(CompositeShape.class, t);
        } else if (shape instanceof TransformedShape t) {
            var o = t.getOriginalShape();
            while (o instanceof TransformedShape transformedShape && transformedShape.getTransform().isIdentity()) {
                o = transformedShape.getOriginalShape();
            }
            s = Transformers.write(Shape.class, o);
            // Insert transform section at the top
            if (!t.getTransform().isIdentity()) {
                s.getSubSections().add(0, Transformers.write(AffineTransform.class, t.getTransform()));
            }
        } else {
            s = new Section("path");

            var pathIterator = shape.getPathIterator(null);
            var coords = new double[6];

            double startX = 0, startY = 0; // Starting point of a subpath
            double lastX = 0, lastY = 0;  // Last point in the current segment

            while (!pathIterator.isDone()) {
                int segmentType = pathIterator.currentSegment(coords);

                switch (segmentType) {
                    case PathIterator.SEG_MOVETO -> {
                        // Start a new subpath
                        startX = coords[0];
                        startY = coords[1];

                        s.addSubsection(Section.createSection("moveTo", X_PARAMETER, startX, Y_PARAMETER, startY));

                        lastX = startX;
                        lastY = startY;
                    }
                    case PathIterator.SEG_LINETO -> {
                        // Add the area of a triangle formed by the line and origin
                        double x = coords[0];
                        double y = coords[1];

                        s.addSubsection(Section.createSection("lineTo", X_PARAMETER, startX, Y_PARAMETER, startY));

                        lastX = x;
                        lastY = y;
                    }
                    case PathIterator.SEG_QUADTO -> {
                        // Quadratic Bézier curve
                        double ctrlX = coords[0], ctrlY = coords[1];
                        double x = coords[2], y = coords[3];

                        s.addSubsection(Section.createSection("quadTo",
                                QUAD_CTRL_X_PARAMETER, ctrlX, QUAD_CTRL_X_PARAMETER, ctrlY,
                                QUAD_END_X_PARAMETER, x, QUAD_END_Y_PARAMETER, y));

                        lastX = x;
                        lastY = y;
                    }
                    case PathIterator.SEG_CUBICTO -> {
                        // Cubic Bézier curve
                        double ctrl1X = coords[0], ctrl1Y = coords[1];
                        double ctrl2X = coords[2], ctrl2Y = coords[3];
                        double x = coords[4], y = coords[5];

                        s.addSubsection(Section.createSection("cubicTo",
                                CUBIC_CTRL1_X_PARAMETER, ctrl1X, CUBIC_CTRL1_Y_PARAMETER, ctrl1Y,
                                CUBIC_CTRL2_X_PARAMETER, ctrl2X, CUBIC_CTRL2_Y_PARAMETER, ctrl2Y,
                                CUBIC_END_X_PARAMETER, x, CUBIC_END_Y_PARAMETER, y));

                        lastX = x;
                        lastY = y;
                    }
                    case PathIterator.SEG_CLOSE -> {
                        s.addSubsection(Section.createSection("close"));

                        // Close the path by adding the area of the closing segment
                        lastX = startX;
                        lastY = startY;
                    }
                }

                pathIterator.next();
            }
        }

        return s;
    }

    private Section getUniqueSection(MultiMap<String, Section> view, String name) {
        return getUniqueSection(view, name, true);
    }

    private Section getUniqueSection(MultiMap<String, Section> view, String name, boolean required) {
        var l = view.get(name);
        var c = l == null ? 0 : l.size();

        if ((required && c != 1) || (!required && c > 1)) {
            throw new IllegalStateException("Composite shape has %s %s(s)!".formatted(c, name));
        }

        return c == 0 ? null : l.get(0);
    }

    private List<Section> getRequiredSection(MultiMap<String, Section> view, String name) {
        if (!view.contains(name)) {
            throw new IllegalStateException("Composite shape missing required section: " + name);
        }

        return view.get(name);
    }
}
