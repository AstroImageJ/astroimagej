package astroj;

import Astronomy.multiaperture.FreeformPixelApertureHandler;
import ij.IJ;
import ij.Prefs;
import ij.astro.types.Pair;
import util.ColorUtil;

import java.awt.*;
import java.util.List;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static Astronomy.Aperture_.*;
public class FreeformPixelApertureRoi extends ApertureRoi {
    // HashSet relies on overridden hashcode in Pixel to only care about coordinates for #contains
    private final Set<Pixel> pixels = new HashSet<>(20);
    private final List<Segment> segments = new ArrayList<>(80);
    private static final Color SOURCE_PIXEL_COLOR = new Color(0, 137, 12);
    private static final Color BACKGROUND_PIXEL_COLOR = new Color(0, 114, 234);
    private static final Color CENTROID_RADIUS_COLOR = new Color(25, 205, 180);
    private final Photometer photometer;
    private boolean useOffsetPixelCenter = false;
    private SegmentLock segmentLock = null;
    private boolean comparisonStar = false;
    private boolean focusedAperture = false;
    private boolean hasAnnulus;
    private double centroidRadius = Double.NaN;

    public FreeformPixelApertureRoi() {
        this(Double.NaN);
    }

    private FreeformPixelApertureRoi(double integratedCnts) {
        super(Double.NaN, Double.NaN, Double.NaN, Double.NaN, Double.NaN, integratedCnts, false);
        showAperture = true;
        showSky = true;
        showName = true;
        photometer = createPhotometer();
    }

    public void addPixel(int x, int y, boolean isBackground) {
        addPixel(x, y, isBackground, true);
    }

    public void addPixel(int x, int y, boolean isBackground, boolean updateCenter) {
        removePixel(x, y, false);
        pixels.add(new Pixel(x, y, isBackground));
        if (updateCenter) {
            updateCenter();
            updateSegments();
            updatePhotometry();
        }
    }

    public void addPixel(Pixel pixel) {
        //removePixel(x, y, false);
        pixels.add(pixel);
        updateCenter();
        updateSegments();
        updatePhotometry();
    }

    public void removePixel(int x, int y) {
        removePixel(x, y, true);
    }

    public void removePixel(int x, int y, boolean updateCenter) {
        pixels.removeIf(pixel -> pixel.coordinatesMatch(x, y));
        if (updateCenter) {
            updateCenter();
            updateSegments();
            updatePhotometry();
        }
    }

    public void moveTo(double x1, double y1) {
        moveTo(x1, y1, false);
    }

    public void moveTo(double x1, double y1, boolean moveBackground) {
        move((int) (x1-xPos), (int) (y1-yPos), moveBackground);
    }

    public void moveTo(int x1, int y1) {
        moveTo(x1, y1, false);
    }

    public void moveTo(int x1, int y1, boolean moveBackground) {
        move((int) (x1-xPos), (int) (y1-yPos), moveBackground);
    }

    public void move(int dx, int dy) {
        move(dx, dy, false);
    }

    public void move(int dx, int dy, boolean moveBackground) {
        var stream = pixels.stream();

        var translatedPixels = stream.map(pixel -> {
            if (!moveBackground && pixel.isBackground()) {
                return pixel;
            }
            return new Pixel(pixel.x() + dx, pixel.y() + dy, pixel.isBackground());
        }).collect(Collectors.toSet());

        pixels.clear();
        pixels.addAll(translatedPixels);

        update();
    }

    public void update() {
        updateCenter();
        updateSegments();
        updatePhotometry();
    }

    @Override
    void log() {
        IJ.log("ArbitraryApertureRoi{pixels: %s}".formatted(pixels));
    }

    @Override
    public boolean contains(int xs, int ys) {
        return pixels.contains(new Pixel(xs, ys));
    }

    public boolean contains(int xs, int ys, boolean isBackground) {
        return pixels.stream().anyMatch(pixel -> pixel.coordinatesMatch(xs, ys) && pixel.isBackground() == isBackground);
    }

    public boolean hasBackground() {
        return pixels.stream().anyMatch(Pixel::isBackground);
    }

    public boolean hasSource() {
        return pixels.stream().anyMatch(Predicate.not(Pixel::isBackground));
    }

    public boolean hasPixels() {
        return !pixels.isEmpty();
    }

    public int pixelCount() {
        return pixels.size();
    }

    public void iteratePixels(PixelConsumer consumer) {
        for (Pixel pixel : pixels) {
            consumer.accept(pixel.x(), pixel.y(), pixel.isBackground());
        }
    }

    public Iterable<Pixel> iterable() {
        return pixels;
    }

    private void updateCenter() {
        if (pixels.isEmpty()) {
            xPos = Double.NaN;
            yPos = Double.NaN;
            r1 = Double.NaN;
            return;
        }

        var centroid = pixels.stream().filter(Predicate.not(Pixel::isBackground))
                .collect(Collectors.teeing(Collectors.averagingDouble(Pixel::x),
                        Collectors.averagingDouble(Pixel::y), Pair.DoublePair::new));
        xPos = centroid.first();
        yPos = centroid.second();

        if (pixels.size() > 1) {
            var radius = pixels.stream().filter(Predicate.not(Pixel::isBackground))
                    .mapToDouble(p -> (p.x - xPos + Centroid.PIXELCENTER) * (p.x - xPos + Centroid.PIXELCENTER) +
                            (p.y - yPos + Centroid.PIXELCENTER) * (p.y - yPos + Centroid.PIXELCENTER))
                    .max().orElse(Double.NaN);

            r1 = Math.sqrt(radius);
        } else {
            r1 = Centroid.PIXELCENTER;
        }
    }

    @Override
    public void draw(Graphics g) {
        if (!hasPixels()) {
            return;
        }

        useOffsetPixelCenter = false;

        boolean aij = false;
        if (ic instanceof AstroCanvas) {
            aij =  true;
            ac =(AstroCanvas)ic;
            ((Graphics2D)g).setTransform(ac.invCanvTrans);
            netFlipX = ac.getNetFlipX();
            netFlipY = ac.getNetFlipY();
            netRotate = ac.getNetRotate();
            showMag = ac.getShowAbsMag() && aMag<99.0;
            showIntCntWithMag = ac.getShowIntCntWithAbsMag();

            if (segmentLock == null || !segmentLock.test(ac)) {
                updateSegments();
            }
        }

        String value = showMag? aMagText+(showIntCntWithMag && !intCntsBlank?", "+intCntsWithMagText:""):intCntsText;
        g.setColor(getApColor());
        FontMetrics metrics = g.getFontMetrics (font);
        int h = metrics.getHeight();
        int w = metrics.stringWidth(value)+3;
        int descent = metrics.getDescent();
        g.setFont (font);

        /*for (Pixel pixel : pixels) {
            int xi = pixel.x();
            int yi = pixel.y();
            var x = netFlipX ? screenXD(xi+1) : screenXD(xi);
            var y = netFlipY ? screenYD(yi+1) : screenYD(yi);
            var ws = netFlipX ? screenXD(xi) - x : screenXD(xi+1) - x;
            var hs = netFlipY ? screenYD(yi) - y : screenYD(yi+1) - y;

            g.setColor(pixel.isBackground() ? BACKGROUND_PIXEL_COLOR : SOURCE_PIXEL_COLOR);
            g.fillRect(x, y, ws, hs);
        }*/

        if (g instanceof Graphics2D g2) {
            var oldStroke = g2.getStroke();

            g2.setStroke(new BasicStroke(3));

            var xShift = netFlipX ? 1 : -1;
            var yShift = netFlipY ? 1 : -1;

            for (Segment segment : segments) {
                g.setColor(getSegmentColor(segment));

                // Offset segments to avoid overlap of bordering regions
                // We cannot use a larger bias as it does not scale correctly with zooming
                var x0 = switch (segment.segmentSide()) {
                    case LEFT, BOTTOM, TOP -> screenX(segment.x0()) - xShift;
                    case RIGHT -> screenX(segment.x0()) + xShift;
                };
                var y0 = switch (segment.segmentSide()) {
                    case BOTTOM -> screenY(segment.y0()) + yShift;
                    case TOP, LEFT, RIGHT -> screenY(segment.y0()) - yShift;
                };
                var x1 = switch (segment.segmentSide()) {
                    case LEFT -> screenX(segment.x1()) - xShift;
                    case RIGHT, BOTTOM, TOP -> screenX(segment.x1()) + xShift;
                };
                var y1 = switch (segment.segmentSide()) {
                    case BOTTOM, LEFT, RIGHT -> screenY(segment.y1()) + yShift;
                    case TOP -> screenY(segment.y1()) - yShift;
                };

                g.drawLine(x0, y0, x1, y1);
            }

            g2.setStroke(oldStroke);
        }

        useOffsetPixelCenter = true;
        int sx = screenXD(xPos);
        int sy = screenYD(yPos);
        double x1d = netFlipX ? screenXD(xPos+r1) : screenXD(xPos-r1);
        int x1 = (int)Math.round(x1d);
        double w1d = netFlipX ? screenXD(xPos-r1)-x1 : screenXD(xPos+r1)-x1;
        int w1 = (int)Math.round(w1d);
        double y1d = netFlipY ? screenYD(yPos+r1) : screenYD(yPos-r1);
        int y1 = (int)Math.round(y1d);
        double h1d = netFlipY ? screenYD(yPos-r1)-y1 : screenYD(yPos+r1)-y1;
        int h1 = (int)Math.round(h1d);

        useOffsetPixelCenter = false;

        int xl = sx - (int)Math.round(w/2D);
        int yl = sy + (int)Math.round(h/3.0);

        // Show center point
        if (FreeformPixelApertureHandler.SHOW_ESTIMATED_CIRCULAR_APERTURE.get()) {
            g.setColor(Color.MAGENTA);
            int w1do4 = (int)Math.round(w1d/2.0);
            int h1do4 = (int)Math.round(h1d/2.0);
            g.drawLine(sx-w1do4, sy, sx+w1do4, sy);
            g.drawLine(sx, sy-h1do4, sx, sy+h1do4);

            // Draw apparent circular aperture
            g.drawOval(x1, y1, w1, h1);
        }

        // Show background annulus
        if (hasAnnulus) {
            g.setColor(transform(BACKGROUND_PIXEL_COLOR));

            useOffsetPixelCenter = true;
            int x2 = netFlipX ? screenXD(xPos+r2) : screenXD(xPos-r2);
            int w2 = netFlipX ? screenXD(xPos-r2)-x2 : screenXD(xPos+r2)-x2;
            int y2 = netFlipY ? screenYD(yPos+r2) : screenYD(yPos-r2);
            int h2 = netFlipY ? screenYD(yPos-r2)-y2 : screenYD(yPos+r2)-y2;

            int x3 = netFlipX ? screenXD(xPos+r3) : screenXD(xPos-r3);
            int w3 = netFlipX ? screenXD(xPos-r3)-x3 : screenXD(xPos+r3)-x3;
            int y3 = netFlipY ? screenYD(yPos+r3) : screenYD(yPos-r3);
            int h3 = netFlipY ? screenYD(yPos-r3)-y3 : screenYD(yPos+r3)-y3;
            useOffsetPixelCenter = false;

            g.drawOval(x2, y2, w2, h2);
            g.drawOval(x3, y3, w3, h3);
        }

        if (FreeformPixelApertureHandler.SHOW_CENTROID_RADIUS.get()) {
            g.setColor(transform(CENTROID_RADIUS_COLOR));

            useOffsetPixelCenter = true;
            int x2 = netFlipX ? screenXD(xPos+getCentroidRadius()) : screenXD(xPos-getCentroidRadius());
            int w2 = netFlipX ? screenXD(xPos-getCentroidRadius())-x2 : screenXD(xPos+getCentroidRadius())-x2;
            int y2 = netFlipY ? screenYD(yPos+getCentroidRadius()) : screenYD(yPos-getCentroidRadius());
            int h2 = netFlipY ? screenYD(yPos-getCentroidRadius())-y2 : screenYD(yPos+getCentroidRadius())-y2;
            useOffsetPixelCenter = false;

            g.drawOval(x2, y2, w2, h2);
        }

        g.setColor(ColorUtil.midpointColor(getApColor(), Color.BLACK, Color.WHITE));

        if (showName && showValues && nameText != null && !nameText.isEmpty() && value != null && !value.isEmpty()) {
            g.drawString(nameText + "=" + value, xl, yl);
        } else if(showName && nameText != null && !nameText.isEmpty()) {
            g.drawString(nameText, xl, yl);
        } else if(showValues && value != null && !value.isEmpty()) {
            g.drawString(value, xl, yl);
        }

        if (aij) {
            ((Graphics2D) g).setTransform(ac.canvTrans);
        }
    }

    private Color getSegmentColor(Segment segment) {
        if (segment.isBackground()) {
            return transform(BACKGROUND_PIXEL_COLOR);
        }

        return getApColor();
    }

    @Override
    public Color getApColor() {
        return transform(Objects.requireNonNullElse(super.getApColor(), comparisonStar ? Color.RED : Color.GREEN));
    }

    private Color transform(Color color) {
        return focusedAperture ? color : ColorUtil.mixColorsWithContrast(color, Color.BLACK);
    }

    private void updatePhotometry() {
        if (true) {
            if (photometer != null && imp != null) {
                photometer.calib = imp.getCalibration();
                photometer.measure(imp, this, false);
                setIntCnts(photometer.sourceBrightness());
            }
        }
    }

    private void updateSegments() {
        segments.clear();
        for (Pixel pixel : pixels) {
            int xi = pixel.x();
            int yi = pixel.y();
            var xDelta = 1;
            var yDelta = !netFlipY ? -1 : 1;
            var xOffset = 1;
            var yOffset = !netFlipY ? 0 : 1;

            // Pixel above
            if (!contains(xi, yi + yDelta, pixel.isBackground())) {
                segments.add(new Segment(xi, yi + yOffset, xi + xDelta, yi + yOffset, pixel.isBackground(), SegmentSide.TOP));
            }

            // Pixel below
            if (!contains(xi, yi - yDelta, pixel.isBackground())) {
                segments.add(new Segment(xi, yi + yOffset - yDelta, xi + xDelta, yi + yOffset - yDelta, pixel.isBackground(), SegmentSide.BOTTOM));
            }

            // Pixel left
            if (!contains(xi - xDelta, yi, pixel.isBackground())) {
                segments.add(new Segment(xi, yi, xi, yi + 1, pixel.isBackground(), SegmentSide.LEFT));
            }

            // Pixel right
            if (!contains(xi + xDelta, yi, pixel.isBackground())) {
                segments.add(new Segment(xi + xDelta, yi, xi + xDelta, yi + 1, pixel.isBackground(), SegmentSide.RIGHT));
            }
        }

        //todo loop through segments, if segments have a matching coord and are of same type we can combine them

        segmentLock = new SegmentLock(netFlipX, netFlipY);
    }

    @Override
    protected boolean useLineSubpixelConvention() {
        return useOffsetPixelCenter;
    }

    public boolean isComparisonStar() {
        return comparisonStar;
    }

    public void setComparisonStar(boolean comparisonStar) {
        this.comparisonStar = comparisonStar;
    }

    public boolean isFocusedAperture() {
        return focusedAperture;
    }

    public void setFocusedAperture(boolean focusedAperture) {
        this.focusedAperture = focusedAperture;
    }

    public boolean hasAnnulus() {
        return hasAnnulus;
    }

    public void setHasAnnulus(boolean hasAnnulus) {
        this.hasAnnulus = hasAnnulus;
    }

    /**
     * Copies this ap's pixels to the specified aperture.
     * <p>
     * Does not update display.
     * <p>
     * Clears existing pixels.
     */
    public void copyPixels(FreeformPixelApertureRoi f, boolean copyBackground) {
        var s = pixels.stream();

        if (!copyBackground) {
            s = s.filter(Predicate.not(Pixel::isBackground));
        }

        f.pixels.clear();
        f.pixels.addAll(s.collect(Collectors.toSet()));
        f.updateCenter();
    }

    private Photometer createPhotometer() {
        var ccdGain = Prefs.get(AP_PREFS_CCDGAIN, 1);
        var ccdNoise = Prefs.get(AP_PREFS_CCDNOISE, 0);
        var ccdDark = Prefs.get(AP_PREFS_CCDDARK, 0);

        var localPhotom = new Photometer();
        localPhotom.setCCD(ccdGain, ccdNoise, ccdDark);
        localPhotom.setRemoveBackStars(false);
        localPhotom.setMarkRemovedPixels(false);
        localPhotom.setUsePlane(false);

        return localPhotom;
    }

    public void setCentroidRadius(double centroidRadius) {
        this.centroidRadius = centroidRadius;
    }

    @Override
    public double getBack1() {
        return hasAnnulus ? r2 : Double.NaN;
    }

    @Override
    public double getBack2() {
        return hasAnnulus ? r3 : Double.NaN;
    }

    public double getCentroidRadius() {
        return Double.isNaN(centroidRadius) ? r1 : centroidRadius;
    }

    private record Segment(int x0, int y0, int x1, int y1, boolean isBackground, SegmentSide segmentSide) {}

    private enum SegmentSide {
        TOP,
        BOTTOM,
        LEFT,
        RIGHT;
    }

    private record SegmentLock(boolean netFlipX, boolean netFlipY) {
        boolean test(AstroCanvas ac) {
            return ac.getNetFlipX() == netFlipX && ac.getNetFlipY() == netFlipY;
        }
    }

    /**
     * @param x x-coordinate of pixel in image space.
     * @param y y-coordinate of pixel in image space.
     * @param isBackground {@code true} if this is a background pixel, {@code false} is this is a source pixel.
     */
    public record Pixel(int x, int y, boolean isBackground) {
        public Pixel(int x, int y) {
            this(x, y, false);
        }

        public Pixel withBackground(boolean isBackground) {
            return new Pixel(x, y, isBackground);
        }

        public Pixel toggleBackground() {
            return new Pixel(x, y, !isBackground);
        }

        public boolean coordinatesMatch(int x, int y) {
            return this.x == x && this.y == y;
        }

        // See point2D for hashcode
        // Used by by the hashset storing pixels
        @Override
        public int hashCode() {
            int result = x;
            result = 31 * result + y;
            return result;
        }
    }

    @FunctionalInterface
    public interface PixelConsumer {
        void accept(int x, int y, boolean isBackground);
    }
}
