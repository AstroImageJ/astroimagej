package Astronomy.multiaperture.io.transformers;

import Astronomy.multiaperture.TransformedShape;
import Astronomy.multiaperture.io.Section;
import Astronomy.multiaperture.io.Transformer;
import Astronomy.multiaperture.io.Transformers;
import astroj.Aperture;
import astroj.Aperture.ApertureShape;
import astroj.FreeformPixelApertureRoi;
import astroj.ShapedApertureRoi;
import ij.astro.types.MultiMap;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.util.List;

public class ApertureTransformer implements Transformer<Aperture> {
    @Override
    public Aperture load(Section section) {
        var s = getShape(section.getParameter(0, "apertureShape"));

        var view = section.createMapView();

        return switch (s) {
            case CIRCULAR -> throw new IllegalStateException("Not Yet Implemented");
            case FREEFORM_SHAPE -> {
                var ap = new ShapedApertureRoi();

                var compSec = getUniqueSection(view, "isComp", false);
                if (compSec != null) {
                    ap.setComparisonStar(readBool("isComp", compSec.getParameter(0, "isComp")));
                }

                var centroidSec = getUniqueSection(view, "centroid", false);
                if (centroidSec != null) {
                    ap.setCentroided(readBool("centroid", centroidSec.getParameter(0, "centroid")));
                }

                var radecSec = getUniqueSection(view, "radec", false);
                if (radecSec != null) {
                    ap.setRadec(readDouble("ra", radecSec.getParameter(0, "ra")),
                            readDouble("dec", radecSec.getParameter(1, "dec")));
                }

                var transforms = view.get("transform");
                if (!transforms.isEmpty()) {
                    var t = Transformers.read(AffineTransform.class, transforms.get(0));
                    for (int i = 1; i < transforms.size(); i++) {
                        t.concatenate(Transformers.read(AffineTransform.class, transforms.get(i)));
                    }

                    ap.setTransform(t);
                }

                var apShapeSec = getUniqueSection(view, "apertureShape");

                if (apShapeSec.getSubSections().size() != 1) {
                    throw new IllegalStateException("apertureShape section must contain one subsection");
                }

                ap.setApertureShape(Transformers.read(Shape.class, apShapeSec.getSubSections().get(0)));

                var backgroundShapeSec = getUniqueSection(view, "backgroundShape", false);
                if (backgroundShapeSec != null) {
                    var center = true;

                    if (backgroundShapeSec.hasParameters()) {
                        center = readBool("", backgroundShapeSec.getParameter(0, "centeredOnAperture"));
                    }

                    if (backgroundShapeSec.getSubSections().size() != 1) {
                        throw new IllegalStateException("backgroundShape section must contain one subsection");
                    }

                    ap.setBackgroundShape(Transformers.read(Shape.class, backgroundShapeSec.getSubSections().get(0)), center);
                }

                yield ap;
            }
            case FREEFORM_PIXEL -> {
                var ap = new FreeformPixelApertureRoi();

                var compSec = getUniqueSection(view, "isComp", false);
                if (compSec != null) {
                    ap.setComparisonStar(readBool("isComp", compSec.getParameter(0, "isComp")));
                }

                var centroidSec = getUniqueSection(view, "centroid", false);
                if (centroidSec != null) {
                    ap.centroidAperture(readBool("centroid", centroidSec.getParameter(0, "centroid")));
                }

                var back1Sec = getUniqueSection(view, "rBack1", false);
                var back2Sec = getUniqueSection(view, "rBack2", false);
                if (back1Sec != null && back2Sec != null) {
                    ap.setBack1(readDouble("rBack1", back1Sec.getParameter(0, "rBack1")));
                    ap.setBack2(readDouble("rBack2", back2Sec.getParameter(0, "rBack2")));
                    ap.setHasAnnulus(true);
                }

                var radecSec = getUniqueSection(view, "radec", false);
                if (radecSec != null) {
                    ap.setRadec(readDouble("ra", radecSec.getParameter(0, "ra")),
                            readDouble("dec", radecSec.getParameter(1, "dec")));
                }

                var pSecs = view.get("px");
                for (Section pSec : pSecs) {
                    var ts = pSec.getParameter(2, "isBackground");
                    var pType = switch (ts) {
                        case "background" -> true;
                        case "source" -> false;
                        default -> throw new IllegalStateException("Unknown px type: " + ts);
                    };

                    ap.addPixel(readInt("px", pSec.getParameter(0, "x")),
                            readInt("px", pSec.getParameter(1, "y")),
                            pType, false);
                }

                ap.update();
                yield ap;
            }
        };
    }

    @Override
    public Section write(Aperture aperture) {
        var s = Section.createSection("ap", getTypeName(aperture));

        switch (aperture.getApertureShape()) {
            case CIRCULAR -> {
                throw new IllegalStateException("Not Yet Implemented");
            }
            case FREEFORM_SHAPE -> {
                var ap = (ShapedApertureRoi) aperture;

                s.addSubsection(Section.createSection("isComp", Boolean.toString(ap.isComparisonStar())));

                s.addSubsection(Section.createSection("centroid", Boolean.toString(ap.getIsCentroid())));

                if (ap.hasRadec()) {
                    s.addSubsection(Section.createSection("radec",
                            Double.toString(ap.getRightAscension()), Double.toString(ap.getDeclination())));
                }

                if (!ap.getTransform().isIdentity()) {
                    s.addSubsection(Transformers.write(AffineTransform.class, ap.getTransform()));
                }

                var apShape = new Section("apertureShape");

                apShape.addSubsection(Transformers.write(Shape.class, ap.getShape()));

                s.addSubsection(apShape);

                if (ap.hasBackground()) {
                    var backgroundShape = Section.createSection("backgroundShape", Boolean.toString(ap.isCenterBackground()));

                    var bShape = ap.getBackgroundShape();

                    if (ap.isCenterBackground() && bShape instanceof TransformedShape transformedShape) {
                        // If just translation, the centering will override it anyways
                        // so we can decompose this into just a simple shape
                        if (transformedShape.getTransform().getType() == AffineTransform.TYPE_TRANSLATION) {
                            bShape = transformedShape.getOriginalShape();
                        }
                    }

                    backgroundShape.addSubsection(Transformers.write(Shape.class, bShape));

                    s.addSubsection(backgroundShape);
                }
            }
            case FREEFORM_PIXEL -> {
                var ap = (FreeformPixelApertureRoi) aperture;

                if (ap.isComparisonStar()) {
                    //noinspection ConstantValue
                    s.addSubsection(Section.createSection("isComp", Boolean.toString(ap.isComparisonStar())));
                }

                if (ap.getIsCentroid()) {
                    s.addSubsection(Section.createSection("centroid", Boolean.toString(ap.getIsCentroid())));
                }

                if (ap.hasAnnulus()) {
                    s.addSubsection(Section.createSection("rBack1", Double.toString(ap.getBack1())));
                    s.addSubsection(Section.createSection("rBack2", Double.toString(ap.getBack2())));
                }

                if (ap.hasRadec()) {
                    s.addSubsection(Section.createSection("radec",
                            Double.toString(ap.getRightAscension()), Double.toString(ap.getDeclination())));
                }

                for (FreeformPixelApertureRoi.Pixel pixel : ap.iterable()) {
                    s.addSubsection(Section.createSection("px",
                            Integer.toString(pixel.x()), Integer.toString(pixel.y()),
                            (pixel.isBackground() ? "background" : "source")));
                }
            }
        }

        return s;
    }

    private String getTypeName(Aperture aperture) {
        return switch (aperture.getApertureShape()) {
            case CIRCULAR -> "circular";
            case FREEFORM_SHAPE -> "custom_shaped";
            case FREEFORM_PIXEL -> "custom_pixel";
        };
    }

    private ApertureShape getShape(String shape) {
        return switch (shape) {
            case "circular" -> ApertureShape.CIRCULAR;
            case "custom_shaped", "customShaped" -> ApertureShape.FREEFORM_SHAPE;
            case "custom_pixel", "customPixel" -> ApertureShape.FREEFORM_PIXEL;
            default -> throw new IllegalStateException("Cannot handle unknown shape " + shape);
        };
    }

    private Section getUniqueSection(MultiMap<String, Section> view, String name) {
        return getUniqueSection(view, name, true);
    }

    private Section getUniqueSection(MultiMap<String, Section> view, String name, boolean required) {
        var l = view.get(name);
        var c = l == null ? 0 : l.size();

        if ((required && c != 1) || (!required && c > 1)) {
            throw new IllegalStateException("Aperture has %s %s(s)!".formatted(c, name));
        }

        return c == 0 ? null : l.get(0);
    }

    private List<Section> getRequiredSection(MultiMap<String, Section> view, String name) {
        if (!view.contains(name)) {
            throw new IllegalStateException("Aperture missing required section: " + name);
        }

        return view.get(name);
    }
}
