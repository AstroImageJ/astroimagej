package astroj;

import Astronomy.multiaperture.CompositeShape;
import Astronomy.multiaperture.TransformedShape;
import ij.astro.types.Pair;

import java.awt.*;
import java.awt.geom.*;
import java.util.Objects;

import static Astronomy.multiaperture.CompositeShape.ShapeCombination;

public final class ShapedApertureRoi extends ApertureRoi implements Aperture {
    private Shape apertureShape;
    private Shape backgroundShape;
    private Area apertureArea;
    private Area backgroundArea;
    private Point2D center = new Point2D.Double(Double.NaN, Double.NaN);
    private Rectangle2D innerBackgroundBounds = null;
    private AffineTransform transform = new AffineTransform();
    private boolean centroided;
    private boolean isCompStar;
    private boolean centerBackground;
    private static final Stroke STROKE = new BasicStroke(3);
    private static final Color BACKGROUND_COLOR = new Color(0, 114, 234);
    private static final Color CENTROID_COLOR = new Color(25, 205, 180);
    private static final boolean FILL_SHAPE = true;
    private static final boolean SHOW_FLATTENED = false;

    /**
     * RA/DEC position of the geometric center (xPos, yPos) of this aperture
     */
    private Pair.DoublePair radec;

    public ShapedApertureRoi() {
        this(null);
        calculateCenter();
    }

    public ShapedApertureRoi(Shape apertureShape) {
        this(Double.NaN, Double.NaN, Double.NaN, Double.NaN, Double.NaN, Double.NaN, false);
        this.apertureShape = apertureShape;
        calculateCenter();
    }

    private ShapedApertureRoi(double x, double y, double rad1, double rad2, double rad3, double integratedCnts, boolean isCentered) {
        super(x, y, rad1, rad2, rad3, integratedCnts, isCentered);
        showAperture = true;
        showSky = true;
        showName = true;
    }

    @Override
    public void draw(Graphics g) {
        if (apertureShape == null || ic == null) {
            return;
        }

        boolean aij = false;
        var aijTransform = new AffineTransform();
        if (ic instanceof AstroCanvas) {
            aij =  true;
            ac =(AstroCanvas)ic;
            ((Graphics2D)g).setTransform(ac.invCanvTrans);
            aijTransform = ac.canvTrans;
            netFlipX = ac.getNetFlipX();
            netFlipY = ac.getNetFlipY();
            netRotate = ac.getNetRotate();
            showMag = ac.getShowAbsMag() && aMag<99.0;
            showIntCntWithMag = ac.getShowIntCntWithAbsMag();
        }

        // Create transform to screenspace coordinates
        var srcRect = ic.getSrcRect();

        var scaleTransform = AffineTransform.getScaleInstance(ic.getMagnification(), ic.getMagnification());
        // Stay in IJ space so no 0.5 pixel offset to pixel center
        var translateTransform = AffineTransform.getTranslateInstance(-srcRect.x, -srcRect.y);

        var toScreenSpaceTransformed = new AffineTransform(aijTransform);
        toScreenSpaceTransformed.concatenate(scaleTransform);
        toScreenSpaceTransformed.concatenate(translateTransform);
        toScreenSpaceTransformed.concatenate(transform);

        var toScreenSpace = new AffineTransform(aijTransform);
        toScreenSpace.concatenate(scaleTransform);
        toScreenSpace.concatenate(translateTransform);

        if (g instanceof Graphics2D g2) {
            //g2.setStroke(STROKE);
            g2.setColor(isCompStar ? Color.RED : Color.GREEN);

            // Draw aperture
            drawShape(g2, apertureShape, toScreenSpaceTransformed);
            g2.setColor(BACKGROUND_COLOR);
            drawShape(g2, backgroundShape, toScreenSpaceTransformed);

            // Draw Geometric Centroid
            if (centroided) {
                g2.setColor(Color.RED);

                var bounds = Objects.requireNonNull(getApertureArea()).getBounds2D();
                var delta = Math.min(bounds.getWidth(), bounds.getHeight())/4D;
                delta = delta <= 0 ? 2 : delta;
                g2.draw(toScreenSpace.createTransformedShape(new Line2D.Double(xPos, yPos - delta, xPos, yPos + delta)));
                g2.draw(toScreenSpace.createTransformedShape(new Line2D.Double(xPos - delta, yPos, xPos + delta, yPos)));
            }

            //todo draw photometric centroid, support offset - can be delayed

            // Debug draw of aperture bounding box for photometry consideration
            if (false) {
                g2.setColor(Color.CYAN);
                if (getApertureArea() != null) {
                    var apertureBound = getApertureArea().getBounds().intersection(new Rectangle(0, 0, imp.getWidth(), imp.getHeight()));
                    g2.draw(toScreenSpace.createTransformedShape(apertureBound));
                }

                if (getBackgroundArea() != null) {
                    var backgroundBound = getBackgroundArea().getBounds().intersection(new Rectangle(0, 0, imp.getWidth(), imp.getHeight()));
                    g2.draw(toScreenSpace.createTransformedShape(backgroundBound));
                }
            }

            // Debug draw of centroid radii
            if (false) {
                g2.setColor(CENTROID_COLOR);
                if (Double.isFinite(getRadius())) {
                    var r = getRadius();
                    g2.draw(toScreenSpace.createTransformedShape(new Ellipse2D.Double(xPos - r, yPos - r, 2*r, 2*r)));
                }

                if (Double.isFinite(getBack1())) {
                    var r = getBack1();
                    g2.draw(toScreenSpace.createTransformedShape(new Ellipse2D.Double(xPos - r, yPos - r, 2*r, 2*r)));
                }

                if (Double.isFinite(getBack2())) {
                    var r = getBack2();
                    g2.draw(toScreenSpace.createTransformedShape(new Ellipse2D.Double(xPos - r, yPos - r, 2*r, 2*r)));
                }
            }
        }

        if (aij) {
            ((Graphics2D) g).setTransform(ac.canvTrans);
        }
    }

    private void drawShape(Graphics2D g2, Shape shape, AffineTransform transform) {
        if (shape == null) {
            return;
        }

        if (FILL_SHAPE) {
            // Draw fill
            var comp = g2.getComposite();
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.5f));
            g2.fill(transform.createTransformedShape(shape));
            g2.setComposite(comp);

            // Draw border as well
            g2.draw(transform.createTransformedShape(shape));
        } else {
            g2.draw(transform.createTransformedShape(shape));
        }

        if (SHOW_FLATTENED) {
            // Transform segments instead of shape
            var pathIterator = shape.getPathIterator(null, 0.01);
            var coords = new double[6];

            double startX = 0, startY = 0; // Starting point of a subpath
            double lastX = 0, lastY = 0;  // Last point in the current segment

            while (!pathIterator.isDone()) {
                int segmentType = pathIterator.currentSegment(coords);

                g2.setColor(Color.PINK);

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
                        g2.draw(transform.createTransformedShape(new Line2D.Double(lastX, lastY, x, y)));
                        lastX = x;
                        lastY = y;
                    }
                    case PathIterator.SEG_QUADTO -> {
                        g2.setColor(Color.ORANGE);
                        // Quadratic Bézier curve
                        double ctrlX = coords[0], ctrlY = coords[1];
                        double x = coords[2], y = coords[3];
                        g2.draw(transform.createTransformedShape(new QuadCurve2D.Double(lastX, lastY, ctrlX, ctrlY, x, y)));
                        lastX = x;
                        lastY = y;
                    }
                    case PathIterator.SEG_CUBICTO -> {
                        g2.setColor(Color.LIGHT_GRAY);
                        // Cubic Bézier curve
                        double ctrl1X = coords[0], ctrl1Y = coords[1];
                        double ctrl2X = coords[2], ctrl2Y = coords[3];
                        double x = coords[4], y = coords[5];
                        g2.draw(transform.createTransformedShape(new CubicCurve2D.Double(lastX, lastY, ctrl1X, ctrl1Y, ctrl2X, ctrl2Y, x, y)));
                        lastX = x;
                        lastY = y;
                    }
                    case PathIterator.SEG_CLOSE -> {
                        // Close the path by adding the area of the closing segment
                        g2.draw(transform.createTransformedShape(new Line2D.Double(lastX, lastY, startX, startY)));
                        lastX = startX;
                        lastY = startY;
                    }
                }

                pathIterator.next();
            }
        }
    }

    private void calculateCenter() {
        center = calculateCenter(apertureShape);
        updateTransformedCenter();
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

    public void transform(AffineTransform cx) {
        transform.concatenate(cx);
        updateTransformedCenter();
    }

    public void setTransform(AffineTransform transform) {
        this.transform = transform;
        updateTransformedCenter();
    }

    private void updateTransformedCenter() {
        var transCenter = transform.transform(center, null);
        xPos = transCenter.getX();
        yPos = transCenter.getY();
    }

    public void setApertureShape(Shape apertureShape) {
        this.apertureShape = apertureShape;
        apertureArea = null;
        calculateCenter();
    }

    public void setBackgroundAnnulus(double r1, double r2) {
        setBackgroundAnnulus(r1, r1, r2, r2);
    }

    public void setBackgroundAnnulus(double r1x, double r1y, double r2x, double r2y) {
        setBackgroundAnnulus(r1x, r1y, new AffineTransform(), r2x, r2y, new AffineTransform());
    }

    public void setBackgroundAnnulus(double r1x, double r1y, AffineTransform transformPrimary,
                                     double r2x, double r2y, AffineTransform transformSecondary) {
        setBackgroundShape(new CompositeShape(ShapeCombination.SUBTRACT,
                new Ellipse2D.Double(-r2x, -r2y, 2*r2x, 2*r2y),
                new Ellipse2D.Double(-r1x, -r1y, 2*r1x, 2*r1y), transformPrimary, transformSecondary), true);
    }

    public void setBackgroundShape(ShapeCombination combination, Shape a, Shape b, boolean centered) {
        var primary = new Area(a);
        var secondary = new Area(b);

        //setBackgroundShape(combination.transform(primary, secondary), centered);
        setBackgroundShape(new CompositeShape(combination, primary, secondary), centered);
        innerBackgroundBounds = secondary.getBounds2D();
    }

    /**
     * @param backgroundShape the new backgroundShape
     * @param centered if the background shape should be transformed to be on the center of the apertureShape
     */
    public void setBackgroundShape(Shape backgroundShape, boolean centered) {
        if (centered && apertureShape != null) {
            var c = calculateCenter(backgroundShape);
            var shift = AffineTransform.getTranslateInstance(center.getX() - c.getX(), center.getY() - c.getY());
            //backgroundShape = shift.createTransformedShape(backgroundShape);
            backgroundShape = new TransformedShape(backgroundShape, shift);
        }

        this.backgroundShape = backgroundShape;
        backgroundArea = null;
        innerBackgroundBounds = null;
        centerBackground = centered;
    }

    @Override
    public double getRadius() {
        if (getApertureArea() != null) {
            var bounds = getApertureArea().getBounds2D();
            return Math.max(bounds.getWidth(), bounds.getHeight())/2D;
        }

        return Double.NaN;
    }

    @Override
    public double getBack1() {
        if (getBackgroundArea() != null && getBackgroundArea().isSingular() && !getBackgroundArea().isEmpty()) {
            var bounds = getBackgroundArea().getBounds2D();
            return Math.min(bounds.getWidth(), bounds.getHeight())/2D;
        }

        if (innerBackgroundBounds != null) {
            return Math.max(innerBackgroundBounds.getWidth(), innerBackgroundBounds.getHeight())/2D;
        }

        return getRadius() * 1.2;
    }

    @Override
    public double getBack2() {
        if (getBackgroundArea() != null && !getBackgroundArea().isEmpty()) {
            var bounds = getBackgroundArea().getBounds2D();
            return Math.max(bounds.getWidth(), bounds.getHeight())/2D;
        }

        return getRadius() * 2;
    }

    public AffineTransform getTransform() {
        return transform;
    }

    public Shape getShape() {
        return apertureShape;
    }

    public Shape getBackgroundShape() {
        return backgroundShape;
    }

    public Area getApertureArea() {
        if (apertureShape == null) {
            return null;
        }

        if (apertureArea == null) {
            apertureArea = new Area(apertureShape);
        }

        return apertureArea.createTransformedArea(transform);
    }

    public Area getBackgroundArea() {
        if (backgroundShape == null) {
            return null;
        }

        if (backgroundArea == null) {
            backgroundArea = new Area(backgroundShape);
        }

        return backgroundArea.createTransformedArea(transform);
    }

    public boolean hasBackground() {
        return backgroundShape != null && !getBackgroundArea().isEmpty();
    }

    public boolean isComparisonStar() {
        return isCompStar;
    }

    public void setComparisonStar(boolean comparisonStar) {
        this.isCompStar = comparisonStar;
    }

    public void setRadec(double ra, double dec) {
        setRadec(new Pair.DoublePair(ra, dec));
    }

    public void setRadec(Pair.DoublePair radec) {
        this.radec = radec;
    }

    public boolean hasRadec() {
        return radec != null && !Double.isNaN(radec.first()) && !Double.isNaN(radec.second());
    }

    public double getRightAscension() {
        return radec == null ? Double.NaN : radec.first();
    }

    public double getDeclination() {
        return radec == null ? Double.NaN : radec.second();
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof ShapedApertureRoi that)) {
            return false;
        }
        if (!super.equals(o)) {
            return false;
        }

        return Objects.equals(apertureShape, that.apertureShape) && Objects.equals(transform, that.transform);
    }

    @Override
    public int hashCode() {
        int result = Objects.hashCode(apertureShape);
        result = 31 * result + Objects.hashCode(transform);
        return result;
    }

    @Override
    public ApertureShape getApertureShape() {
        return ApertureShape.FREEFORM_SHAPE;
    }

    public boolean isCentroided() {
        return centroided;
    }

    public void setCentroided(boolean centroided) {
        this.centroided = centroided;
    }

    public boolean isCenterBackground() {
        return centerBackground;
    }

    public void setCenterBackground(boolean centerBackground) {
        this.centerBackground = centerBackground;
    }
}