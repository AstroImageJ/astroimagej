package Astronomy.multiaperture;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.PathIterator;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

public class TransformedShape implements Shape {
    private final Shape result;
    private final Tracker tracker;

    public TransformedShape(Shape shape, AffineTransform transform) {
        while (shape instanceof TransformedShape transformedShape) {
            shape = transformedShape.getOriginalShape();
            transform.preConcatenate(transformedShape.getTransform());
        }
        var centerReferencingTransform = new CenterReferencingTransform(transform, shape);
        this.result = centerReferencingTransform.createTransformedShape(shape);
        this.tracker = new Tracker(shape, centerReferencingTransform);
    }

    public AffineTransform getTransform() {
        return tracker.transform();
    }

    public Shape getOriginalShape() {
        return tracker.shape();
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

    private record Tracker(Shape shape, AffineTransform transform) {}
}
