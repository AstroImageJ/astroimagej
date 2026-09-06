package Astronomy.multiaperture.io.transformers;

import static Astronomy.multiaperture.CompositeShape.ShapeCombination.ADD;
import static Astronomy.multiaperture.CompositeShape.ShapeCombination.EXCLUSIVE_OR;
import static Astronomy.multiaperture.CompositeShape.ShapeCombination.INTERSECT;
import static Astronomy.multiaperture.CompositeShape.ShapeCombination.SUBTRACT;

import java.awt.Shape;

import Astronomy.multiaperture.CompositeShape;
import Astronomy.multiaperture.CompositeShape.ShapeCombination;
import ij.astro.io.aij.AijFileCodec;
import ij.astro.io.aij.Section;
import ij.astro.io.aij.Section.Parameter;
import ij.astro.io.aij.Transformer;

public class CompositeShapeTransformer extends Transformer<CompositeShape, Void> {
    private static final Parameter<ShapeCombination> COMBINATION_PARAMETER =
            new Parameter<>("shapeCombination", 0, ShapeCombination.class, CompositeShapeTransformer::combinationFromString, CompositeShapeTransformer::combination);

    public CompositeShapeTransformer(AijFileCodec codec) {
        super(codec);
    }

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

        Shape pShape = codec.read(Shape.class, pSec.getSubSections().get(0));
        Shape sShape = codec.read(Shape.class, sSec.getSubSections().get(0));

        return new CompositeShape(combination, pShape, sShape);
    }

    @Override
    public Section write(Void params, CompositeShape shape) {
        var tracker = shape.getTracker();

        Section s;
        if (tracker.primaryOnly()) {
            s = codec.write(Shape.class, shape.getResult());
        } else {
            s = Section.createSection("composite", COMBINATION_PARAMETER, tracker.combination());

            var pri = new Section("primary");
            var sec = new Section("secondary");

            pri.addSubsection(codec.write(Shape.class, tracker.primary()));

            sec.addSubsection(codec.write(Shape.class, tracker.secondary()));

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
}
