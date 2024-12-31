package Astronomy.multiaperture.io.transformers;

import Astronomy.multiaperture.CompositeShape;
import Astronomy.multiaperture.CompositeShape.ShapeCombination;
import Astronomy.multiaperture.io.Section;
import Astronomy.multiaperture.io.Section.Parameter;
import Astronomy.multiaperture.io.Transformer;
import Astronomy.multiaperture.io.Transformers;
import ij.astro.types.MultiMap;

import java.awt.*;
import java.util.List;

import static Astronomy.multiaperture.CompositeShape.ShapeCombination.*;

public class CompositeShapeTransformer implements Transformer<CompositeShape, Void> {
    private static final Parameter<ShapeCombination> COMBINATION_PARAMETER =
            new Parameter<>("shapeCombination", 0, ShapeCombination.class, CompositeShapeTransformer::combinationFromString, CompositeShapeTransformer::combination);

    @Override
    public CompositeShape load(Void params, Section section) {
        var view = section.createMapView();

        var combination = section.getParameter(COMBINATION_PARAMETER);

        var pSec = getUniqueSection(view, "primary");
        var sSec = getUniqueSection(view, "secondary");

        if (pSec.getSubSections().size() != 1) {
            throw new IllegalStateException("primary section must contain one subsection, found " + pSec.getSubSections().size());
        }

        if (sSec.getSubSections().size() != 1) {
            throw new IllegalStateException("secondary section must contain one subsection, found " + sSec.getSubSections().size());
        }

        Shape pShape = Transformers.read(Shape.class, pSec.getSubSections().get(0));
        Shape sShape = Transformers.read(Shape.class, sSec.getSubSections().get(0));

        return new CompositeShape(combination, pShape, sShape);
    }

    @Override
    public Section write(Void params, CompositeShape shape) {
        var tracker = shape.getTracker();

        Section s;
        if (tracker.primaryOnly()) {
            s = Transformers.write(Shape.class, shape.getResult());
        } else {
            s = Section.createSection("composite", COMBINATION_PARAMETER, tracker.combination());

            var pri = new Section("primary");
            var sec = new Section("secondary");

            pri.addSubsection(Transformers.write(Shape.class, tracker.primary()));

            sec.addSubsection(Transformers.write(Shape.class, tracker.secondary()));

            s.addSubsection(pri);
            s.addSubsection(sec);
        }

        return s;
    }

    private static String combination(Parameter<ShapeCombination> parameter, ShapeCombination shapeCombination) {
        return switch (shapeCombination) {
            case SUBTRACT -> "subtract";
            case ADD -> "add";
            case EXCLUSIVE_OR -> "exclusiveOr";
            case INTERSECT -> "intersect";
        };
    }

    private static ShapeCombination combinationFromString(Parameter<ShapeCombination> parameter, String s) {
        return switch (s) {
            case "subtract" -> SUBTRACT;
            case "add" -> ADD;
            case "exclusiveOr" -> EXCLUSIVE_OR;
            case "intersect" -> INTERSECT;
            default -> throw new IllegalStateException("Unknown shape combination '%s' for parameter '%s'"
                    .formatted(s, parameter.name()));
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
