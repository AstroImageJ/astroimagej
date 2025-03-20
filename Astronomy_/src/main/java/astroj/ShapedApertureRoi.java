package astroj;

import Astronomy.multiaperture.CenterReferencingTransform;
import Astronomy.multiaperture.CompositeShape;
import Astronomy.multiaperture.TransformedShape;
import ij.Prefs;
import ij.astro.logging.AIJLogger;
import util.ArrowShape;

import java.awt.*;
import java.awt.geom.*;
import java.util.Objects;

import static Astronomy.Aperture_.AP_PREFS_BACKPLANE;
import static Astronomy.Aperture_.AP_PREFS_REMOVEBACKSTARS;
import static Astronomy.MultiAperture_.SHAPED_AP_AREA_LOCKED;
import static Astronomy.multiaperture.CompositeShape.ShapeCombination;

public final class ShapedApertureRoi extends ApertureRoi implements Aperture {
    private Shape apertureShape;
    private Shape backgroundShape;
    private Area apertureArea;
    private Area backgroundArea;
    private Point2D center = new Point2D.Double(Double.NaN, Double.NaN);
    private Rectangle2D innerBackgroundBounds = null;
    private CenterReferencingTransform transform = new CenterReferencingTransform();
    private boolean isCompStar;
    private boolean centerBackground;
    private double ellipticalBaseRadius = Double.NaN;
    private static final Stroke STROKE = new BasicStroke(3);
    private static final Color BACKGROUND_COLOR = new Color(0, 114, 234);
    private static final Color CENTROID_COLOR = new Color(25, 205, 180);
    private static final Color PHANTOM_TARGET = new Color(196, 222, 155);
    private static final Color PHANTOM_COMPARISON = Color.PINK;
    private static final boolean FILL_SHAPE = true;
    private static final boolean DRAW_POINTING = true;
    private static final boolean SHOW_FLATTENED = false;
    private final boolean usePlane = Prefs.get(AP_PREFS_BACKPLANE, false);
    private final boolean removeStars = Prefs.get(AP_PREFS_REMOVEBACKSTARS, false);

    public ShapedApertureRoi() {
        this(null);
        calculateCenter();
    }

    public ShapedApertureRoi(Shape apertureShape) {
        this(Double.NaN, Double.NaN, Double.NaN, Double.NaN, Double.NaN, Double.NaN, false);
        this.apertureShape = apertureShape;
        transform.bind(apertureShape);
        calculateCenter();
        showSky = Prefs.get("aperture.skyoverlay", showSky);
    }

    private ShapedApertureRoi(double x, double y, double rad1, double rad2, double rad3, double integratedCnts, boolean isCentered) {
        super(x, y, rad1, rad2, rad3, integratedCnts, isCentered);
        showAperture = true;
        showSky = true;
        showName = true;
    }

    public static ShapedApertureRoi fromApertureRoi(ApertureRoi roi) {
        return switch (roi.getApertureShape()) {
            case CIRCULAR -> {
                var ap = new ShapedApertureRoi();
                var r = roi.getRadius();

                ap.setEllipticalBaseRadius(r);
                ap.setApertureShape(new Ellipse2D.Double(roi.xPos - r, roi.yPos - r, 2*r, 2*r));

                if (roi.getBack1() < roi.getBack2()) {
                    ap.setBackgroundAnnulus(roi.getBack1(), roi.getBack2());
                }

                ap.setIsCentroid(roi.getIsCentroid());
                ap.setName(roi.getName());
                ap.setRadec(roi.radec);
                ap.setComparisonStar(roi.getIsComparisonStar());
                ap.setPhantom(roi.isPhantom());

                yield ap;
            }
            case FREEFORM_SHAPE -> (ShapedApertureRoi) roi;
            case FREEFORM_PIXEL -> {
                var ap = new ShapedApertureRoi();
                var source = new Area();
                var background = new Area();
                if (roi instanceof FreeformPixelApertureRoi pixelApertureRoi) {
                    var pixelBackground = false;
                    for (FreeformPixelApertureRoi.Pixel pixel : pixelApertureRoi.iterable()) {
                        if (pixel.isBackground()) {
                            pixelBackground = true;
                            background.add(new Area(new Rectangle(pixel.x(), pixel.y(), 1, 1)));
                        } else {
                            source.add(new Area(new Rectangle(pixel.x(), pixel.y(), 1, 1)));
                        }
                    }

                    if (pixelApertureRoi.hasAnnulus()) {
                        if (!pixelBackground) {
                            ap.setBackgroundAnnulus(pixelApertureRoi.getBack1(), pixelApertureRoi.getBack2());
                        } else {
                            AIJLogger.log("Cannot convert a Pixel aperture to Shaped due to mixed background modes");
                        }
                    } else {
                        ap.setApertureShape(source);
                        ap.setBackgroundShape(background, false);
                    }

                }

                yield ap;
            }
        };
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
            g2.setColor(isPhantom() ? (isCompStar ? PHANTOM_TARGET : PHANTOM_COMPARISON) : (isCompStar ? Color.RED : Color.GREEN));

            // Draw aperture
            drawShape(g2, apertureShape, toScreenSpaceTransformed);
            g2.setColor(BACKGROUND_COLOR);
            if (showSky) {
                drawShape(g2, backgroundShape, toScreenSpaceTransformed, isPhantom() ? (isCompStar ? PHANTOM_TARGET : PHANTOM_COMPARISON) : (isCompStar ? Color.RED : Color.GREEN));
            }

            // Draw Geometric Centroid
            if (isCentroid) {
                g2.setColor(Color.RED);

                var bounds = Objects.requireNonNull(apertureShape).getBounds2D();
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

                if (showSky && getBackgroundArea() != null) {
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

                if (showSky && Double.isFinite(getBack1())) {
                    var r = getBack1();
                    g2.draw(toScreenSpace.createTransformedShape(new Ellipse2D.Double(xPos - r, yPos - r, 2*r, 2*r)));
                }

                if (showSky && Double.isFinite(getBack2())) {
                    var r = getBack2();
                    g2.draw(toScreenSpace.createTransformedShape(new Ellipse2D.Double(xPos - r, yPos - r, 2*r, 2*r)));
                }
            }

            if (DRAW_POINTING) {
                g2.setColor(Color.ORANGE);
                var length = apertureShape.getBounds2D().getWidth()/2D;
                var width = length * 0.333333;
                drawShape(g2, new ArrowShape(xPos, yPos, length, width), toScreenSpaceTransformed);
                var theta = Math.toDegrees(Math.atan2(transform.getShearY(), transform.getScaleX()));
                width *= 2;
                drawShape(g2, new Arc2D.Double(xPos-width/2, yPos-width/2, width, width, 0, -theta, Arc2D.PIE), toScreenSpace);
            }
        }

        String value = showMag ?
                aMagText + (showIntCntWithMag && !intCntsBlank ? ", " + intCntsWithMagText : "") :
                intCntsText;
        g.setColor(isCompStar ? Color.RED : Color.GREEN);
        g.setFont(font);

        var b = (backgroundShape != null && showSky ?
                         toScreenSpaceTransformed.createTransformedShape(backgroundShape) :
                         toScreenSpaceTransformed.createTransformedShape(apertureShape)).getBounds();
        if (showName && showValues && nameText != null && !nameText.isEmpty() && value != null && !value.isEmpty()) {
            g.drawString(nameText + "=" + value, (int) b.getMaxX(), (int) (b.getMinY() + b.getHeight()/2));
        } else if (showName && nameText != null && !nameText.isEmpty()) {
            g.drawString(nameText, (int) b.getMaxX(), (int) (b.getMinY() + b.getHeight()/2));
        } else if (showValues && value != null && !value.isEmpty()) {
            g.drawString(value, (int) b.getMaxX(), (int) (b.getMinY() + b.getHeight()/2));
        }

        if (aij) {
            ((Graphics2D) g).setTransform(ac.canvTrans);
        }
    }

    private void drawShape(Graphics2D g2, Shape shape, AffineTransform transform) {
        drawShape(g2, shape, transform, null);
    }

    private void drawShape(Graphics2D g2, Shape shape, AffineTransform transform, Color secondary) {
        if (shape == null) {
            return;
        }

        if (FILL_SHAPE) {
            // Draw fill
            var comp = g2.getComposite();
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.25f));
            g2.fill(transform.createTransformedShape(shape));
            g2.setComposite(comp);

            var primary = g2.getColor();

            if (secondary != null) {
                g2.setColor(secondary);
            }

            // Draw border as well
            g2.draw(transform.createTransformedShape(shape));

            g2.setColor(primary);
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

    public void calculateCenter() {
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

    public void automaticTransform(boolean adjustShape, boolean adjustRotation) {
        automaticTransform(new Centroid(), false, adjustShape, adjustRotation);
    }

    public void automaticTransform(Centroid c, boolean centroidFound, boolean adjustShape, boolean adjustRotation) {
        if (!centroidFound && imp != null) {
            centroidFound = c.measure(imp, xPos, yPos, getRadius(), getBack1(), getBack2(), true, usePlane, removeStars);
        }

        if (centroidFound) {
            transform(adjustShape, c.roundness(), adjustRotation, c.orientation());
        }
    }

    /**
     * Transformation for elliptical-based apertures
     */
    public void transform(boolean adjustShape, double roundness, boolean adjustRotation, double angle) {
        if (adjustShape && Double.isFinite(ellipticalBaseRadius) && apertureShape instanceof Ellipse2D ellipse2D) {
            setApertureShape(createEllipse(xPos, yPos, ellipticalBaseRadius, roundness));
            setBackgroundShape(backgroundShape, centerBackground);
        }

        if (adjustRotation) {
            setTransform(AffineTransform.getRotateInstance(Math.toRadians(angle)));
        }
    }

    public void transform(AffineTransform cx) {
        transform.concatenate(cx);
        updateTransformedCenter();
    }

    public void setTransform(AffineTransform transform) {
        if (transform instanceof CenterReferencingTransform centerReferencingTransform) {
            centerReferencingTransform.bind(apertureShape);
            this.transform = centerReferencingTransform;
        } else {
            this.transform = new CenterReferencingTransform(transform, apertureShape);
        }

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
        transform.bind(apertureShape);
        calculateCenter();
    }

    public void setBackgroundAnnulus(double rInner, double rOuter) {
        setBackgroundAnnulus(rInner, rInner, rOuter, rOuter);
    }

    public void setBackgroundAnnulus(double rix, double riy, double rox, double roy) {
        setBackgroundAnnulus(rix, riy, new AffineTransform(), rox, roy, new AffineTransform());
    }

    public void setBackgroundAnnulus(double rix, double riy, AffineTransform transformOuter,
                                     double rox, double roy, AffineTransform transformInner) {
        setBackgroundShape(new CompositeShape(ShapeCombination.SUBTRACT,
                new Ellipse2D.Double(-rox, -roy, 2*rox, 2*roy),
                new Ellipse2D.Double(-rix, -riy, 2*rix, 2*riy), transformOuter, transformInner), true);
    }

    public void setBackgroundShape(ShapeCombination combination, Shape a, Shape b, boolean centered) {
        setBackgroundShape(new CompositeShape(combination, a, b), centered);
        innerBackgroundBounds = new Area(b).getBounds2D();
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

        // It is important to return a new instance each time for the Photometer in multithread mode
        return apertureArea.createTransformedArea(transform);
    }

    public Area getBackgroundArea() {
        if (backgroundShape == null) {
            return null;
        }

        if (backgroundArea == null) {
            backgroundArea = new Area(backgroundShape);
        }

        // It is important to return a new instance each time for the Photometer in multithread mode
        return backgroundArea.createTransformedArea(transform);
    }

    public boolean hasBackground() {
        return backgroundShape != null && !getBackgroundArea().isEmpty();
    }

    @Override
    public boolean getIsRefStar() {
        return isComparisonStar();
    }

    public boolean isComparisonStar() {
        return isCompStar;
    }

    @Override
    public boolean getIsComparisonStar() {
        return isComparisonStar();
    }

    public void setComparisonStar(boolean comparisonStar) {
        this.isCompStar = comparisonStar;
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

    public boolean isCenterBackground() {
        return centerBackground;
    }

    public void setCenterBackground(boolean centerBackground) {
        this.centerBackground = centerBackground;
    }

    public static Ellipse2D createEllipse(double x, double y, double r, double roundness) {
        if (SHAPED_AP_AREA_LOCKED.get()) {
            var sqRoundness = Math.sqrt(roundness);
            var a = Math.abs(r / sqRoundness);
            var b = Math.abs(r * sqRoundness);

            return new Ellipse2D.Double(x - a, y - b, 2 * a, 2 * b);
        } else {
            var a = r / (roundness);
            var b = r;

            return new Ellipse2D.Double(x - a, y - b, 2 * a, 2 * b);
        }
    }

    public void adjustRadii(double r, double r2, double r3, double roundness) {
        if (isElliptical()) {
            if (r != ellipticalBaseRadius) {
                roundness = Double.isFinite(roundness) ? roundness : 1;

                setApertureShape(createEllipse(xPos, yPos, r, roundness));

                setEllipticalBaseRadius(r);
            }

            if (hasAnnulus()) {
                setBackgroundAnnulus(r2, r3);
            } else {
                setBackgroundShape(backgroundShape, centerBackground);
            }
        }
    }

    public boolean hasAnnulus() {
        if (centerBackground && backgroundShape != null) {
            if (getInnerShape(backgroundShape) instanceof CompositeShape compositeShape) {
                var tracker = compositeShape.getTracker();

                if (tracker.combination() == ShapeCombination.SUBTRACT) {
                    return getInnerShape(tracker.primary()) instanceof Ellipse2D &&
                            getInnerShape(tracker.secondary()) instanceof Ellipse2D;
                }
            }
        }

        return false;
    }

    public boolean isElliptical() {
        return getInnerShape(apertureShape) instanceof Ellipse2D && Double.isFinite(ellipticalBaseRadius);
    }

    private Shape getInnerShape(Shape shape) {
        if (shape instanceof TransformedShape transformedShape) {
            return getInnerShape(transformedShape.getOriginalShape());
        }

        return shape;
    }

    @Override
    public double getAngle() {
        return Math.toDegrees(Math.atan2(transform.getShearY(), transform.getScaleX()));
    }

    @Override
    public void move(double dx, double dy) {
        moveTo(xPos + dx, yPos + dy, true);
    }

    public void moveTo(double x, double y, boolean moveBackground) {
        apertureShape = moveShape(apertureShape, x - xPos, y - yPos);
        apertureArea = null;
        transform.bind(apertureShape);

        if (moveBackground) {
            backgroundShape = moveShape(backgroundShape, x - xPos, y - yPos);
            backgroundArea = null;
            innerBackgroundBounds = null;
        } else if (centerBackground) {
            if (backgroundShape instanceof TransformedShape transformedShape) {
                var c = calculateCenter(transformedShape.getOriginalShape());
                var shift = AffineTransform.getTranslateInstance(center.getX() - c.getX(), center.getY() - c.getY());
                backgroundShape = new TransformedShape(transformedShape.getOriginalShape(), shift);
                backgroundArea = null;
                innerBackgroundBounds = null;
            }
        }

        calculateCenter();
    }

    private Shape moveShape(Shape shape, double dx, double dy) {
        if (shape instanceof Ellipse2D.Double e) {
            return new Ellipse2D.Double(e.x + dx, e.y + dy, e.width, e.height);
        }

        if (shape instanceof Ellipse2D.Float e) {
            return new Ellipse2D.Float((float) (e.x + dx), (float) (e.y + dy), e.width, e.height);
        }

        if (shape instanceof TransformedShape transformedShape) {
            return new TransformedShape(moveShape(transformedShape.getOriginalShape(), dx, dy),
                    transformedShape.getTransform());
        }

        if (shape instanceof CompositeShape compositeShape) {
            return new CompositeShape(compositeShape.getTracker().combination(),
                    moveShape(compositeShape.getTracker().primary(), dx, dy),
                    moveShape(compositeShape.getTracker().secondary(), dx, dy));
        }

        if (shape instanceof Rectangle2D.Double r) {
            return new Rectangle2D.Double(r.x + dx, r.y + dy, r.width, r.height);
        }

        if (shape instanceof Rectangle2D.Float r) {
            return new Rectangle2D.Float((float)(r.x + dx), (float)(r.y + dy), r.width, r.height);
        }

        if (shape instanceof RoundRectangle2D.Double rr) {
            return new RoundRectangle2D.Double(rr.x + dx, rr.y + dy, rr.width, rr.height,
                    rr.getArcWidth(), rr.getArcHeight());
        }

        if (shape instanceof RoundRectangle2D.Float rr) {
            return new RoundRectangle2D.Float((float)(rr.x + dx), (float)(rr.y + dy), rr.width, rr.height,
                    rr.arcwidth, rr.archeight);
        }

        var t = AffineTransform.getTranslateInstance(dx, dy);
        return t.createTransformedShape(shape);
    }

    /**
     * Should only be called with translations
     */
    private Shape typePreservingTransform(Shape shape, AffineTransform transform) {
        if (transform.isIdentity()) {
            return shape;
        }

        assert transform.getType() == AffineTransform.TYPE_TRANSLATION : "Type preserving transform is only for translations";

        if (shape instanceof Ellipse2D ellipse2D) {
            var points = new double[]{ellipse2D.getX(), ellipse2D.getY(), ellipse2D.getMaxX(), ellipse2D.getMaxY()};
            transform.transform(points, 0, points, 0, points.length / 2);
            return new Ellipse2D.Double(points[0], points[1], points[2]-points[0], points[3]-points[1]);
        } else if (shape instanceof Rectangle2D rectangle2D) {
            var points = new double[]{rectangle2D.getX(), rectangle2D.getY(), rectangle2D.getMaxX(), rectangle2D.getMaxY()};
            transform.transform(points, 0, points, 0, points.length / 2);
            return new Rectangle2D.Double(points[0], points[1], points[2]-points[0], points[3]-points[1]);
        } else if (shape instanceof RoundRectangle2D roundRectangle2D) {
            var points = new double[]{roundRectangle2D.getX(), roundRectangle2D.getY(), roundRectangle2D.getMaxX(), roundRectangle2D.getMaxY()};
            transform.transform(points, 0, points, 0, points.length / 2);
            return new RoundRectangle2D.Double(points[0], points[1], points[2]-points[0], points[3]-points[1], roundRectangle2D.getArcWidth(), roundRectangle2D.getArcHeight());
        } else if (shape instanceof CompositeShape compositeShape) {
            return new CompositeShape(compositeShape.getTracker().combination(),
                    typePreservingTransform(compositeShape.getTracker().primary(), transform),
                    typePreservingTransform(compositeShape.getTracker().secondary(), transform));
        } else if (shape instanceof TransformedShape transformedShape) {
            return new TransformedShape(typePreservingTransform(transformedShape.getOriginalShape(), transform), transformedShape.getTransform());
        } else { // Already lost type information, so we can proceed with just a transform
            return transform.createTransformedShape(shape);
        }
    }

    public double getEllipticalBaseRadius() {
        return ellipticalBaseRadius;
    }

    public void setEllipticalBaseRadius(double ellipticalBaseRadius) {
        this.ellipticalBaseRadius = ellipticalBaseRadius;
    }

    public double estimateRoundness() {
        if (isElliptical()) {
            var bounds = apertureShape.getBounds2D();
            var a = Math.max(bounds.getWidth(), bounds.getHeight()) / 2;
            var b = Math.min(bounds.getWidth(), bounds.getHeight()) / 2;

            return Math.sqrt(b / a);
        }

        return Double.NaN;
    }
}
