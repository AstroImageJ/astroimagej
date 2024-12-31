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

    @Override
    public Shape load(Void params, Section section) {
        var view = section.createMapView();
        var shape = switch (section.name()) {
            case "ellipse" -> {
                var centerSec = getUniqueSection(view, "center");
                var radiiSec = getUniqueSection(view, "radii");

                var centerX = readDouble("centerX", centerSec.getParameter(0, "x"));
                var centerY = readDouble("centerY", centerSec.getParameter(1, "y"));
                var radiusX = readDouble("radiusX", radiiSec.getParameter(0, "x"));
                var radiusY = readDouble("radiusY", radiiSec.getParameter(1, "y"));

                yield new Ellipse2D.Double(centerX - radiusX, centerY - radiusY, 2 * radiusX, 2 * radiusY);
            }
            case "rectangle" -> {
                var centerSec = getUniqueSection(view, "center");
                var radiiSec = getUniqueSection(view, "radii");

                var centerX = readDouble("centerX", centerSec.getParameter(0, "x"));
                var centerY = readDouble("centerY", centerSec.getParameter(1, "y"));
                var radiusX = readDouble("radiusX", radiiSec.getParameter(0, "x"));
                var radiusY = readDouble("radiusY", radiiSec.getParameter(1, "y"));

                yield new Rectangle2D.Double(centerX - radiusX, centerY - radiusY, 2 * radiusX, 2 * radiusY);
            }
            case "composite" -> {
                yield Transformers.read(CompositeShape.class, section);
            }
            case "roundedRectangle" -> {
                var centerSec = getUniqueSection(view, "center");
                var radiiSec = getUniqueSection(view, "radii");
                var cornerRadiiSec = getUniqueSection(view, "cornerRadii");

                var centerX = readDouble("centerX", centerSec.getParameter(0, "x"));
                var centerY = readDouble("centerY", centerSec.getParameter(1, "y"));
                var radiusX = readDouble("radiusX", radiiSec.getParameter(0, "x"));
                var radiusY = readDouble("radiusY", radiiSec.getParameter(1, "y"));
                var cornerRadiusX = readDouble("cornerRadiusX", cornerRadiiSec.getParameter(0, "x"));
                var cornerRadiusY = readDouble("cornerRadiusY", cornerRadiiSec.getParameter(1, "y"));

                yield new RoundRectangle2D.Double(centerX - radiusX, centerY - radiusY,
                        2 * radiusX, 2 * radiusY, 2 * cornerRadiusX, 2 * cornerRadiusY);

            }
            case "path" -> {
                var p = new Path2D.Double();
                for (Section subSection : section.getSubSections()) {
                    switch (subSection.name()) {
                        case "moveTo" -> {
                            var startX = readDouble("startX", subSection.getParameter(0, "x"));
                            var startY = readDouble("startY", subSection.getParameter(1, "y"));

                            p.moveTo(startX, startY);
                        }
                        case "lineTo" -> {
                            var endX = readDouble("endX", subSection.getParameter(0, "x"));
                            var endY = readDouble("endY", subSection.getParameter(1, "y"));

                            p.lineTo(endX, endY);
                        }
                        case "quadTo" -> {
                            var ctrlX = readDouble("ctrlX", subSection.getParameter(0, "x"));
                            var ctrlY = readDouble("ctrlY", subSection.getParameter(1, "y"));
                            var endX = readDouble("endX", subSection.getParameter(0, "x"));
                            var endY = readDouble("endY", subSection.getParameter(1, "y"));

                            p.quadTo(ctrlX, ctrlY, endX, endY);
                        }
                        case "cubicTo" -> {
                            var ctrl1X = readDouble("ctrl1X", subSection.getParameter(0, "x"));
                            var ctrl1Y = readDouble("ctrl1Y", subSection.getParameter(1, "y"));
                            var ctrl2X = readDouble("ctrl2X", subSection.getParameter(2, "x"));
                            var ctrl2Y = readDouble("ctrl2Y", subSection.getParameter(3, "y"));
                            var endX = readDouble("endX", subSection.getParameter(4, "x"));
                            var endY = readDouble("endY", subSection.getParameter(5, "y"));

                            p.curveTo(ctrl1X, ctrl1Y, ctrl2X, ctrl2Y, endX, endY);
                        }
                        case "close" -> {
                            p.closePath();
                        }
                        default -> throw new IllegalStateException("Unknown path element: " + subSection.name());
                    }
                }

                yield p;
            }
            default -> throw new IllegalStateException();
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
                    Double.toString(e.getCenterX()), Double.toString(e.getCenterY())));
            s.addSubsection(Section.createSection("radii",
                    Double.toString(e.getWidth()/2.0), Double.toString(e.getHeight()/2.0)));
        } else if (shape instanceof Rectangle2D r) {
            s = new Section("rectangle");

            s.addSubsection(Section.createSection("center",
                    Double.toString(r.getCenterX()), Double.toString(r.getCenterY())));
            s.addSubsection(Section.createSection("radii",
                    Double.toString(r.getWidth()/2.0), Double.toString(r.getHeight()/2.0)));
        } else if (shape instanceof RoundRectangle2D r) {
            s = new Section("roundedRectangle");

            s.addSubsection(Section.createSection("center",
                    Double.toString(r.getCenterX()), Double.toString(r.getCenterY())));
            s.addSubsection(Section.createSection("radii",
                    Double.toString(r.getWidth()/2.0), Double.toString(r.getHeight()/2.0)));
            s.addSubsection(Section.createSection("cornerRadii",
                    Double.toString(r.getArcWidth()/2.0), Double.toString(r.getArcHeight()/2.0)));
        } else if (shape instanceof CompositeShape t) {
            s = Transformers.write(CompositeShape.class, t);
        } else if (shape instanceof TransformedShape t) {
            s = Transformers.write(Shape.class, t.getOriginalShape());
            // Insert transform section at the top
            s.getSubSections().add(0, Transformers.write(AffineTransform.class, t.getTransform()));
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

                        s.addSubsection(Section.createSection("moveTo",
                                Double.toString(startX), Double.toString(startY)));

                        lastX = startX;
                        lastY = startY;
                    }
                    case PathIterator.SEG_LINETO -> {
                        // Add the area of a triangle formed by the line and origin
                        double x = coords[0];
                        double y = coords[1];

                        s.addSubsection(Section.createSection("lineTo",
                                Double.toString(x), Double.toString(y)));

                        lastX = x;
                        lastY = y;
                    }
                    case PathIterator.SEG_QUADTO -> {
                        // Quadratic Bézier curve
                        double ctrlX = coords[0], ctrlY = coords[1];
                        double x = coords[2], y = coords[3];

                        s.addSubsection(Section.createSection("quadTo",
                                Double.toString(ctrlX), Double.toString(ctrlY),
                                Double.toString(x), Double.toString(y)));

                        lastX = x;
                        lastY = y;
                    }
                    case PathIterator.SEG_CUBICTO -> {
                        // Cubic Bézier curve
                        double ctrl1X = coords[0], ctrl1Y = coords[1];
                        double ctrl2X = coords[2], ctrl2Y = coords[3];
                        double x = coords[4], y = coords[5];

                        s.addSubsection(Section.createSection("cubicTo",
                                Double.toString(ctrl1X), Double.toString(ctrl1Y),
                                Double.toString(ctrl2X), Double.toString(ctrl2Y),
                                Double.toString(x), Double.toString(y)));

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
