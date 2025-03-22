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

public class ApertureTransformer implements Transformer<Aperture, Void> {
    private static final Section.Parameter<ApertureShape> SHAPE_PARAMETER =
            new Section.Parameter<>("aperture shape", 0, ApertureShape.class, ApertureTransformer::deserializeShape, ApertureTransformer::serializeShape);
    private static final Section.Parameter<Boolean> COMP_PARAMETER = new Section.Parameter<>("isComp", 0, Boolean.TYPE);
    private static final Section.Parameter<Boolean> CENTROID_PARAMETER = new Section.Parameter<>("isCentroided", 0, Boolean.TYPE);
    private static final Section.Parameter<Boolean> CENTER_PARAMETER = new Section.Parameter<>("centeredOnAperture", 0, Boolean.TYPE, ApertureTransformer::deserializeCenterParam, ApertureTransformer::serializeCenterParam);
    private static final Section.Parameter<Double> MAG_PARAMETER = new Section.Parameter<>("absolute magnitude", 0, Double.TYPE);
    private static final Section.Parameter<Double> RA_PARAMETER = new Section.Parameter<>("right ascension", 0, Double.TYPE);
    private static final Section.Parameter<Double> DEC_PARAMETER = new Section.Parameter<>("declination", 1, Double.TYPE);
    private static final Section.Parameter<Integer> INT_X_PARAMETER = new Section.Parameter<>("x", 0, Integer.TYPE);
    private static final Section.Parameter<Integer> INT_Y_PARAMETER = new Section.Parameter<>("y", 1, Integer.TYPE);
    private static final Section.Parameter<Boolean> BACKGROUND_PARAMETER = new Section.Parameter<>("isBackground", 2, Boolean.TYPE,
            (p, s) -> switch (s) {
                case "background" -> true;
                case "source" -> false;
                default -> throw new IllegalStateException("Unknown px type: " + s);
            }, (p, b) -> b ? "background" : "source");
    private static final Section.Parameter<Double> RADIUS_PARAMETER = new Section.Parameter<>("radius", 0, Double.TYPE);

    @Override
    public Aperture load(Void params, Section section) {
        var s = section.getParameter(SHAPE_PARAMETER);

        var view = section.createMapView();

        return switch (s) {
            case CIRCULAR -> throw new IllegalStateException("Not Yet Implemented");
            case FREEFORM_SHAPE -> {
                var ap = new ShapedApertureRoi();

                var compSec = getUniqueSection(view, "isComp", false);
                if (compSec != null) {
                    ap.setComparisonStar(compSec.getParameter(COMP_PARAMETER));
                }

                var centroidSec = getUniqueSection(view, "centroid", false);
                if (centroidSec != null) {
                    ap.setIsCentroid(centroidSec.getParameter(CENTROID_PARAMETER));
                }

                var radecSec = getUniqueSection(view, "radec", false);
                if (radecSec != null) {
                    ap.setRadec(radecSec.getParameter(RA_PARAMETER), radecSec.getParameter(DEC_PARAMETER));
                }

                var baseRadius = getUniqueSection(view, "baseRadius", false);
                if (baseRadius != null) {
                    ap.setEllipticalBaseRadius(baseRadius.getParameter(RADIUS_PARAMETER));
                }

                var absMag = getUniqueSection(view, "absMag", false);
                if (absMag != null) {
                    ap.setAMag(absMag.getParameter(MAG_PARAMETER));
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
                    var center = false;

                    if (backgroundShapeSec.hasParameters()) {
                        center = backgroundShapeSec.getParameter(CENTER_PARAMETER);
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
                    ap.setComparisonStar(compSec.getParameter(COMP_PARAMETER));
                }

                var centroidSec = getUniqueSection(view, "centroid", false);
                if (centroidSec != null) {
                    ap.centroidAperture(centroidSec.getParameter(CENTROID_PARAMETER));
                }

                var back1Sec = getUniqueSection(view, "rBack1", false);
                var back2Sec = getUniqueSection(view, "rBack2", false);
                if (back1Sec != null && back2Sec != null) {
                    ap.setBack1(back1Sec.getParameter(RADIUS_PARAMETER));
                    ap.setBack2(back2Sec.getParameter(RADIUS_PARAMETER));
                    ap.setHasAnnulus(true);
                }

                var radecSec = getUniqueSection(view, "radec", false);
                if (radecSec != null) {
                    ap.setRadec(radecSec.getParameter(RA_PARAMETER), radecSec.getParameter(DEC_PARAMETER));
                }

                var pSecs = view.get("px");
                for (Section pSec : pSecs) {
                    ap.addPixel(pSec.getParameter(INT_X_PARAMETER),
                            pSec.getParameter(INT_Y_PARAMETER),
                            pSec.getParameter(BACKGROUND_PARAMETER), false);
                }

                ap.update();
                yield ap;
            }
        };
    }

    @Override
    public Section write(Void params, Aperture aperture) {
        var s = Section.createSection("ap", SHAPE_PARAMETER, aperture.getApertureShape());

        switch (aperture.getApertureShape()) {
            case CIRCULAR -> {
                throw new IllegalStateException("Not Yet Implemented");
            }
            case FREEFORM_SHAPE -> {
                var ap = (ShapedApertureRoi) aperture;

                s.addSubsection(Section.createSection("isComp", COMP_PARAMETER, ap.isComparisonStar()));

                s.addSubsection(Section.createSection("centroid", CENTROID_PARAMETER, ap.getIsCentroid()));

                if (ap.hasRadec()) {
                    s.addSubsection(Section.createSection("radec",
                            RA_PARAMETER, ap.getRightAscension(), DEC_PARAMETER, ap.getDeclination()));
                }

                if (Double.isFinite(ap.getEllipticalBaseRadius())) {
                    s.addSubsection(Section.createSection("baseRadius", RADIUS_PARAMETER, ap.getEllipticalBaseRadius()));
                }

                if (Double.isFinite(ap.getAMag()) && (ap.getAMag() != 99.99 || ap.getAMag() != 99.999)) {
                    s.addSubsection(Section.createSection("absMag", MAG_PARAMETER, ap.getAMag()));
                }

                if (!ap.getTransform().isIdentity()) {
                    s.addSubsection(Transformers.write(AffineTransform.class, ap.getTransform()));
                }

                var apShape = new Section("apertureShape");

                apShape.addSubsection(Transformers.write(Shape.class, ap.getShape()));

                s.addSubsection(apShape);

                if (ap.hasBackground()) {
                    var backgroundShape = Section.createSection("backgroundShape", CENTER_PARAMETER, ap.isCenterBackground());

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
                    s.addSubsection(Section.createSection("isComp", COMP_PARAMETER, ap.isComparisonStar()));
                }

                if (ap.getIsCentroid()) {
                    s.addSubsection(Section.createSection("centroid", CENTROID_PARAMETER, ap.getIsCentroid()));
                }

                if (ap.hasAnnulus()) {
                    s.addSubsection(Section.createSection("rBack1", RADIUS_PARAMETER, ap.getBack1()));
                    s.addSubsection(Section.createSection("rBack2", RA_PARAMETER, ap.getBack2()));
                }

                if (ap.hasRadec()) {
                    s.addSubsection(Section.createSection("radec",
                            RA_PARAMETER, ap.getRightAscension(), DEC_PARAMETER, ap.getDeclination()));
                }

                for (FreeformPixelApertureRoi.Pixel pixel : ap.iterable()) {
                    s.addSubsection(Section.createSection("px",
                            INT_X_PARAMETER, pixel.x(), INT_Y_PARAMETER, pixel.y(),
                            BACKGROUND_PARAMETER, pixel.isBackground()));
                }
            }
        }

        return s;
    }

    private static String serializeShape(Section.Parameter<ApertureShape> parameter, ApertureShape apertureShape) {
        return switch (apertureShape) {
            case CIRCULAR -> "circular";
            case FREEFORM_SHAPE -> "custom_shaped";
            case FREEFORM_PIXEL -> "custom_pixel";
        };
    }

    private static ApertureShape deserializeShape(Section.Parameter<ApertureShape> parameter, String shape) {
        return switch (shape) {
            case "circular" -> ApertureShape.CIRCULAR;
            case "custom_shaped", "customShaped" -> ApertureShape.FREEFORM_SHAPE;
            case "custom_pixel", "customPixel" -> ApertureShape.FREEFORM_PIXEL;
            default -> throw new IllegalStateException("Cannot handle unknown shape " + shape);
        };
    }

    private static String serializeCenterParam(Section.Parameter<Boolean> parameter, Boolean centered) {
        return centered ? "centeredOnAperture" : "";
    }

    private static boolean deserializeCenterParam(Section.Parameter<Boolean> parameter, String val) {
        return "centeredOnAperture".equals(val) || Boolean.parseBoolean(val);
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
