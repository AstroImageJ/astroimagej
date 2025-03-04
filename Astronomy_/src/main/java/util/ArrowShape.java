package util;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

public class ArrowShape implements Shape {
    private final Path2D arrow;

    /**
     * Creates an arrow shape.
     *
     * @param x           The x-coordinate of the arrow's center.
     * @param y           The y-coordinate of the arrow's center.
     * @param arrowLength The length of the arrow's shaft in pixels.
     * @param headSize    The size (width) of the arrow's head in pixels.
     *
     * The arrow is drawn horizontally to the right from the center (x, y) with its tip at (x - arrowLength, y).
     */
    public ArrowShape(double x, double y, double arrowLength, double headSize) {
        // Build the arrow shape relative to origin.
        // The shaft extends from (0, 0) upward to (-arrowLength, 0).
        // The arrow head is a triangle with its tip at (-arrowLength, 0) and base centered at (-arrowLength + headSize, 0).
        arrow = new Path2D.Double();

        // Draw the shaft.
        arrow.moveTo(0, 0);
        arrow.lineTo(-arrowLength, 0);

        // Draw the arrow head
        arrow.moveTo(-arrowLength, 0);
        arrow.lineTo(-arrowLength + headSize, -headSize / 2);
        arrow.lineTo(-arrowLength + headSize, headSize / 2);
        arrow.closePath();

        AffineTransform transform = AffineTransform.getTranslateInstance(x, y);
        arrow.transform(transform);
    }

    @Override
    public Rectangle2D getBounds2D() {
        return arrow.getBounds2D();
    }

    @Override
    public Rectangle getBounds() {
        return arrow.getBounds();
    }

    @Override
    public boolean contains(double x, double y) {
        return arrow.contains(x, y);
    }

    @Override
    public boolean contains(Point2D p) {
        return arrow.contains(p);
    }

    @Override
    public boolean intersects(double x, double y, double w, double h) {
        return arrow.intersects(x, y, w, h);
    }

    @Override
    public boolean intersects(Rectangle2D r) {
        return arrow.intersects(r);
    }

    @Override
    public boolean contains(double x, double y, double w, double h) {
        return arrow.contains(x, y, w, h);
    }

    @Override
    public boolean contains(Rectangle2D r) {
        return arrow.contains(r);
    }

    @Override
    public java.awt.geom.PathIterator getPathIterator(AffineTransform at) {
        return arrow.getPathIterator(at);
    }

    @Override
    public java.awt.geom.PathIterator getPathIterator(AffineTransform at, double flatness) {
        return arrow.getPathIterator(at, flatness);
    }
}
