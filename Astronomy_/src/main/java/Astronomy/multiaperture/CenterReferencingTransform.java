package Astronomy.multiaperture;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.PathIterator;
import java.awt.geom.Point2D;
import java.util.Objects;

public class CenterReferencingTransform extends AffineTransform {
    Tracker tracker;

    public CenterReferencingTransform() {
        this(new AffineTransform(), null);
    }

    public CenterReferencingTransform(AffineTransform original, Shape shape) {
        super(Objects.requireNonNull(original));
        bindToCenter(this, shape);
    }

    public AffineTransform original() {
        if (tracker != null) {
            return tracker.original();
        }

        return new AffineTransform(this);
    }

    @Override
    public boolean isIdentity() {
        return decompose(this).isIdentity();
    }

    public void bind(Shape shape) {
        // If the shape is unchanged, do nothing.
        if (shape != null && tracker != null && shape.equals(tracker.boundShape)) {
            return;
        }

        // Reset to the original transformation before rebinding.
        if (tracker != null) {
            this.setTransform(tracker.original);
        }

        // Rebind to the new shape.
        bindToCenter(this, shape);
    }


    private void bindToCenter(AffineTransform transform, Shape shape) {
        if (transform instanceof CenterReferencingTransform centerReferencingTransform && centerReferencingTransform.tracker != null) {
            tracker = new Tracker(new AffineTransform(centerReferencingTransform.tracker.original), shape);
        } else {
            tracker = new Tracker(new AffineTransform(transform), shape);
        }

        if (shape != null) {
            var c = calculateCenter(shape);
            if (transform instanceof CenterReferencingTransform centerReferencingTransform) {
                centerReferencingTransform.concatenateResult(AffineTransform.getTranslateInstance(-c.getX(), -c.getY()));
                centerReferencingTransform.preConcatenateResult(AffineTransform.getTranslateInstance(c.getX(), c.getY()));
            } else {
                transform.concatenate(AffineTransform.getTranslateInstance(-c.getX(), -c.getY()));
                transform.preConcatenate(AffineTransform.getTranslateInstance(c.getX(), c.getY()));
            }
        }
    }

    private static AffineTransform decompose(AffineTransform transform) {
        if (transform instanceof CenterReferencingTransform centerReferencingTransform) {
            return decompose(centerReferencingTransform.original());
        }

        return transform;
    }

    @Override
    public void concatenate(AffineTransform Tx) {
        super.concatenate(Tx);
        if (tracker != null) {
            tracker.original.concatenate(Tx);
        }
    }

    @Override
    public void preConcatenate(AffineTransform Tx) {
        super.preConcatenate(Tx);
        if (tracker != null) {
            tracker.original.preConcatenate(Tx);
        }
    }

    private void concatenateResult(AffineTransform Tx) {
        super.concatenate(Tx);
    }

    private void preConcatenateResult(AffineTransform Tx) {
        super.preConcatenate(Tx);
    }

    private Point2D calculateCenter(Shape shape) {
        if (shape == null) {
            return new Point2D.Double(Double.NaN, Double.NaN);
        }

        var pathIterator = shape.getPathIterator(null, 0.01);
        var coords = new double[6];

        double startX = 0, startY = 0; // Starting point of a subpath
        double lastX = 0, lastY = 0;  // Last point in the current segment

        var centerX = 0D;
        var centerY = 0D;
        var centerCnt = 0;

        while (!pathIterator.isDone()) {
            int segmentType = pathIterator.currentSegment(coords);

            switch (segmentType) {
                case PathIterator.SEG_MOVETO -> {
                    // Start a new subpath
                    startX = coords[0];
                    startY = coords[1];
                    lastX = startX;
                    lastY = startY;
                }
                case PathIterator.SEG_LINETO -> {
                    // Add the area of a triangle formed by the line and origin
                    double x = coords[0];
                    double y = coords[1];

                    centerX += x;
                    centerY += y;
                    centerCnt++;

                    lastX = x;
                    lastY = y;
                }
                case PathIterator.SEG_QUADTO -> {
                    // Quadratic Bézier curve
                    double ctrlX = coords[0], ctrlY = coords[1];
                    double x = coords[2], y = coords[3];
                    lastX = x;
                    lastY = y;
                }
                case PathIterator.SEG_CUBICTO -> {
                    // Cubic Bézier curve
                    double ctrl1X = coords[0], ctrl1Y = coords[1];
                    double ctrl2X = coords[2], ctrl2Y = coords[3];
                    double x = coords[4], y = coords[5];
                    lastX = x;
                    lastY = y;
                }
                case PathIterator.SEG_CLOSE -> {
                    // Close the path by adding the area of the closing segment
                    lastX = startX;
                    lastY = startY;
                }
            }

            pathIterator.next();
        }

        return new Point2D.Double(centerX/centerCnt, centerY/centerCnt);
    }

    private record Tracker(AffineTransform original, Shape boundShape) {}
}
