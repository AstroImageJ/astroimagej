package Astronomy.multiaperture;

import java.awt.*;
import java.awt.geom.*;
import java.util.function.BiConsumer;

public class CompositeShape implements Shape {
    private final Shape result;
    private final Tracker tracker;

    public CompositeShape(Shape primary) {
        this.result = primary;
        this.tracker = new Tracker(true, null, primary, null);
    }

    public CompositeShape(ShapeCombination combination, Shape primary, Shape secondary) {
        this(combination, primary, secondary, new AffineTransform(), new AffineTransform());
    }

    public CompositeShape(ShapeCombination combination, Shape primary, Shape secondary,
                          AffineTransform transformPrimary, AffineTransform transformSecondary) {
        var transformedPrimary = new TransformedShape(primary, transformPrimary);
        var transformedSecondary = new TransformedShape(secondary, transformSecondary);
        tracker = new Tracker(false, combination, transformedPrimary, transformedSecondary);
        var primaryArea = new Area(transformedPrimary);
        var secondaryArea = new Area(transformedSecondary);
        result = combination.transform(primaryArea, secondaryArea);
    }

    public CompositeShape combine(ShapeCombination shapeCombination,
                                  Shape secondary) {
        return combine(shapeCombination, secondary, new AffineTransform());
    }

    public CompositeShape combine(ShapeCombination shapeCombination,
                                  Shape secondary, AffineTransform transformSecondary) {
        return combine(shapeCombination, new AffineTransform(), secondary, transformSecondary);
    }

    public CompositeShape combine(ShapeCombination shapeCombination, AffineTransform transformPrimary,
                                  Shape secondary, AffineTransform transformSecondary) {
        return new CompositeShape(shapeCombination, this, secondary, transformPrimary, transformSecondary);
    }

    public Tracker getTracker() {
        return tracker;
    }

    public Shape getResult() {
        return result;
    }

    @Override
    public Rectangle getBounds() {
        return result.getBounds();
    }

    @Override
    public Rectangle2D getBounds2D() {
        return result.getBounds2D();
    }

    @Override
    public boolean contains(double x, double y) {
        return result.contains(x, y);
    }

    @Override
    public boolean contains(Point2D p) {
        return result.contains(p);
    }

    @Override
    public boolean intersects(double x, double y, double w, double h) {
        return result.intersects(x, y, w, h);
    }

    @Override
    public boolean intersects(Rectangle2D r) {
        return result.intersects(r);
    }

    @Override
    public boolean contains(double x, double y, double w, double h) {
        return result.contains(x, y, w, h);
    }

    @Override
    public boolean contains(Rectangle2D r) {
        return result.intersects(r);
    }

    @Override
    public PathIterator getPathIterator(AffineTransform at) {
        return result.getPathIterator(at);
    }

    @Override
    public PathIterator getPathIterator(AffineTransform at, double flatness) {
        return result.getPathIterator(at, flatness);
    }

    public record Tracker(boolean primaryOnly, ShapeCombination combination, Shape primary, Shape secondary) {

        private Tracker(ShapeCombination combination, Shape primary, Shape secondary) {
            this(false, combination, primary, secondary);
        }
    }

    public enum ShapeCombination {
        SUBTRACT(Area::subtract),
        ADD(Area::add),
        EXCLUSIVE_OR(Area::exclusiveOr),
        INTERSECT(Area::intersect),
        ;

        private final BiConsumer<Area, Area> transform;

        ShapeCombination(BiConsumer<Area, Area> transform) {
            this.transform = transform;
        }

        public Area transform(Area a, Area b) {
            transform.accept(a, b);
            return a;
        }
    }
}
