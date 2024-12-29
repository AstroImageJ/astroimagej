package Astronomy.multiaperture.io.transformers;

import Astronomy.multiaperture.CompositeShape;
import Astronomy.multiaperture.io.Section;
import Astronomy.multiaperture.io.Transformer;
import Astronomy.multiaperture.io.Transformers;
import ij.astro.types.MultiMap;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.util.List;

public class CompositeShapeTransformer implements Transformer<CompositeShape> {
    @Override
    public CompositeShape load(Section section) {
        var view = section.createMapView();

        var cs = section.getParameter(0, "combiner");
        var combination = switch (cs) {
            case "add" -> CompositeShape.ShapeCombination.ADD;
            case "subtract" -> CompositeShape.ShapeCombination.SUBTRACT;
            case "exclusiveOr" -> CompositeShape.ShapeCombination.EXCLUSIVE_OR;
            case "intersect" -> CompositeShape.ShapeCombination.INTERSECT;
            default -> throw new IllegalStateException("Unknown shape combination: " +cs);
        };

        var pSec = getUniqueSection(view, "primary");
        var sSec = getUniqueSection(view, "secondary");

        var pSecView = pSec.createMapView();
        var sSecView = sSec.createMapView();

        Shape pShape = null;
        Shape sShape = null;
        for (String shapeSectionName : ShapeTransformer.SHAPE_SECTION_NAMES) {
            var pShapeSec = getUniqueSection(pSecView, shapeSectionName, false);
            var sShapeSec = getUniqueSection(sSecView, shapeSectionName, false);

            if (pShapeSec != null) {
                if (pShape != null) {
                    throw new IllegalStateException("Primary section of composite cannot have multiple shapes!");
                }
                pShape = Transformers.read(Shape.class, pShapeSec);
            }

            if (sShapeSec != null) {
                if (sShape != null) {
                    throw new IllegalStateException("Secondary section of composite cannot have multiple shapes!");
                }
                sShape = Transformers.read(Shape.class, sShapeSec);
            }
        }

        if (pShape == null || sShape == null) {
            throw new IllegalStateException("composite missing primary or secondary shape");
        }

        AffineTransform pTransform = new AffineTransform();
        AffineTransform sTransform = new AffineTransform();

        var pTSecs = pSecView.get("transform");
        if (!pTSecs.isEmpty()) {
            var t = Transformers.read(AffineTransform.class, pTSecs.get(0));
            for (int i = 1; i < pTSecs.size(); i++) {
                t.concatenate(Transformers.read(AffineTransform.class, pTSecs.get(i)));
            }

            pTransform = t;
        }

        var sTSecs = sSecView.get("transform");
        if (!sTSecs.isEmpty()) {
            var t = Transformers.read(AffineTransform.class, sTSecs.get(0));
            for (int i = 1; i < sTSecs.size(); i++) {
                t.concatenate(Transformers.read(AffineTransform.class, sTSecs.get(i)));
            }

            sTransform = t;
        }

        return new CompositeShape(combination, pShape, sShape, pTransform, sTransform);
    }

    @Override
    public Section write(CompositeShape shape) {
        var tracker = shape.getTracker();

        Section s;
        if (tracker.primaryOnly()) {
            s = Transformers.write(Shape.class, shape.getResult());
        } else {
            s = Section.createSection("composite", combination(tracker.combination()));

            var pri = new Section("primary");
            var sec = new Section("secondary");

            pri.addSubsection(Transformers.write(Shape.class, tracker.primary()));
            if (!tracker.transformPrimary().isIdentity()) {
                pri.addSubsection(Transformers.write(AffineTransform.class, tracker.transformPrimary()));
            }

            sec.addSubsection(Transformers.write(Shape.class, tracker.secondary()));
            if (!tracker.transformSecondary().isIdentity()) {
                sec.addSubsection(Transformers.write(AffineTransform.class, tracker.transformSecondary()));
            }

            s.addSubsection(pri);
            s.addSubsection(sec);
        }

        return s;
    }

    private String combination(CompositeShape.ShapeCombination shapeCombination) {
        return switch (shapeCombination) {
            case SUBTRACT -> "subtract";
            case ADD -> "add";
            case EXCLUSIVE_OR -> "exclusiveOr";
            case INTERSECT -> "intersect";
        };
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
