// ApertureRoi.java

package astroj;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.NoninvertibleTransformException;
import java.text.DecimalFormat;

import ij.IJ;
import ij.astro.types.Pair;
import ij.gui.Roi;

/**
 * A ROI consisting of three concentric circles (inner for object, outer annulus for background).
 * The ROI keeps track both of it's double position and a boolean flag which determines whether
 * the backround aperture should be displayed or not.
 */
public non-sealed class ApertureRoi extends Roi implements Aperture {
    protected double intCnts = Double.NaN;
    protected String intCntsText = "";
    protected String intCntsWithMagText = "";
    protected String aMagText = "";
    protected boolean intCntsBlank = true;
    protected String nameText = "";
    protected Font font = null;
    protected double xPos, yPos;
    protected double aMag = 99.99;
    protected double r1, r2, r3;
    protected boolean isCentroid = false;
    protected Color apColor;
    protected DecimalFormat upto3Places = new DecimalFormat("0.0##", IJU.dfs);
    protected boolean showAperture = true;
    protected boolean showSky = false;
    protected boolean showName = false;
    protected boolean showValues = false;
    GFormat g21 = new GFormat("2.1");
    GFormat g23 = new GFormat("2.3");
    AstroCanvas ac = null;
    boolean netFlipX = false, netFlipY = false, netRotate = false, showMag = true, showIntCntWithMag = true;
    boolean isPhantom = false;
    /**
     * RA/DEC position of the geometric center (xPos, yPos) of this aperture
     */
    protected Pair.DoublePair radec;
    private static boolean hasLoggedInversion = false;

    public ApertureRoi(double x, double y, double rad1, double rad2, double rad3, double integratedCnts, boolean isCentered) {
        super((int) x, (int) y, 1, 1);

        xPos = x;        // Roi CLASS HAS PRIVATE FLOATING POINT COORDINATES
        yPos = y;
        r1 = rad1;
        r2 = rad2;
        r3 = rad3;
        intCnts = integratedCnts;
        if (Double.isNaN(intCnts)) {
            intCntsText = "";
            intCntsWithMagText = "";
            intCntsBlank = true;
        } else {
            intCntsText = g23.format(intCnts);
            intCntsWithMagText = g21.format(intCnts);
            intCntsBlank = false;
        }

        isCentroid = isCentered;
        font = new Font("SansSerif", Font.PLAIN, 16);
    }

    public void setSize(double rad1, double rad2, double rad3) {
        r1 = rad1;
        r2 = rad2;
        r3 = rad3;
    }

    public boolean isPhantom() {
        return isPhantom;
    }

    public void setPhantom(boolean phantom) {
        isPhantom = phantom;
    }

    public void move(double dx, double dy) {
        xPos -= dx;
        yPos -= dy;
    }

    public void setLocation(double x, double y) {
        xPos = x;
        yPos = y;
    }

    /**
     * Sets the appearance of the ROI when it is displayed.
     */
    public void setAppearance(boolean aper, boolean isCentered, boolean sky, boolean name, boolean label, Color col, String ntext, Double integratedCnts) {
        showAperture = aper;
        isCentroid = isCentered;
        showSky = sky;
        showName = name;
        showValues = label;
        apColor = col;
        intCnts = integratedCnts;
        if (Double.isNaN(intCnts)) {
            intCntsText = "";
            intCntsWithMagText = "";
            intCntsBlank = true;
        } else {
            intCntsText = g23.format(intCnts);
            intCntsWithMagText = g21.format(intCnts);
            intCntsBlank = false;
        }
        nameText = ntext;
    }

    public double getIntCnts() {
        return intCnts;
    }

    public void setIntCnts(double cnts) {
        intCnts = cnts;
        if (Double.isNaN(intCnts)) {
            intCntsText = "";
            intCntsWithMagText = "";
            intCntsBlank = true;
        } else {
            intCntsText = g23.format(intCnts);
            intCntsWithMagText = g21.format(intCnts);
            intCntsBlank = false;
        }
    }

    public String getName() {
        return nameText;
    }

    public void setName(String ntext) {
        nameText = ntext;
    }

    public int getApNumber() {
        return IJU.parseInteger(nameText.substring(1), 0) - 1;
    }

    public boolean getIsRefStar() {
        return nameText.startsWith("C");
    }

    public double getXpos() {
        return xPos;
    }

    public void setXpos(double x) {
        xPos = x;
    }

    public double getYpos() {
        return yPos;
    }

    public void setYpos(double y) {
        yPos = y;
    }

    public double getRadius() {
        return r1;
    }

    public void setRadius(double rad1) {
        r1 = rad1;
    }

    public double getBack1() {
        return r2;
    }

    public void setBack1(double rad2) {
        r2 = rad2;
    }

    public double getBack2() {
        return r3;
    }

    public void setBack2(double rad3) {
        r3 = rad3;
    }

    public boolean getShowName() {
        return showAperture;
    }

    public void setShowName(boolean name) {
        showName = name;
    }

    public boolean getShowValues() {
        return showValues;
    }

    public void setShowValues(boolean label) {
        showValues = label;
    }

    public boolean getShowSky() {
        return showSky;
    }

    public void setShowSky(boolean sky) {
        showSky = sky;
    }

    @Override
    public boolean getIsCentroid() {
        return isCentroid;
    }

    @Override
    public boolean getIsComparisonStar() {
        return false;
    }

    public void setIsCentroid(boolean isCentered) {
        isCentroid = isCentered;
    }

    public Color getApColor() {
        return apColor;
    }

    public void setApColor(Color col) {
        apColor = col;
    }

    public double getAMag() {
        return aMag;
    }

    public void setAMag(double mag) {
        aMag = mag;
        aMagText = upto3Places.format(aMag);
    }

    /**
     * Lists the ROI contents.
     */
    void log() {
        IJ.log("ApertureRoi: x,y=" + xPos + "," + yPos + ", src-back=" + intCnts + ", r1=" + r1 + ", r2=" + r2 + ", r3=" + r3);
    }

    /**
     * Returns an array with the double position.
     */
    public double[] getCenter() {
        double[] d = new double[]{xPos, yPos};
        return d;
    }

    /**
     * Returns the measurement value associated with the ROI.
     */
    public double getMeasurement() {
        return intCnts;
    }

    /**
     * Returns true if the screen position (xs,ys) is within the currently displayed area of the ROI
     * (i.e. inner circle if showSky==false).
     */
    public boolean contains(int xs, int ys) {
        int xx, yy, ww, hh;
        if (showSky) {
            xx = (int) (xPos - r3);
            ww = (int) (xPos + r3) - xx;
            yy = (int) (yPos - r3);
            hh = (int) (yPos + r3) - yy;
        } else {
            xx = (int) (xPos - r1);
            ww = (int) (xPos + r1) - xx;
            yy = (int) (yPos - r1);
            hh = (int) (yPos + r1) - yy;
        }

        if (xs < xx || xs > xx + ww) {
            return false;
        }

        return ys >= yy && ys <= yy + hh;
    }

    /**
     * Displays the aperture either as a simple circle (showSky false) or as three circles (showSky true).
     */
    public void draw(Graphics g) {
        Graphics2D g2 = (Graphics2D) g;

        var defaultTransform = g2.getDeviceConfiguration().getDefaultTransform();
        var invertedDefaultTransform = new AffineTransform();

        try {
            invertedDefaultTransform = defaultTransform.createInverse();
        } catch (NoninvertibleTransformException e) {
            if (!hasLoggedInversion) {
                hasLoggedInversion = true;
                System.err.println("Failed to invert " + defaultTransform);
                e.printStackTrace();
            }
        }

        boolean aij = false;
        var aijTransform = new AffineTransform();
        if (ic instanceof AstroCanvas) {
            aij =  true;
            ac =(AstroCanvas)ic;
            g2.setTransform(ac.invCanvTrans);
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

        var toScreenSpace = new AffineTransform(aijTransform);
        toScreenSpace.concatenate(scaleTransform);
        toScreenSpace.concatenate(translateTransform);
        toScreenSpace.preConcatenate(invertedDefaultTransform);

        String value = showMag ?
                aMagText + (showIntCntWithMag && !intCntsBlank ? ", " + intCntsWithMagText : "") :
                intCntsText;
        g.setColor(apColor);
        FontMetrics metrics = g.getFontMetrics(font);
        int h = metrics.getHeight();
        int w = metrics.stringWidth(value) + 3;
        int descent = metrics.getDescent();
        g.setFont(font);

        var sx = xPos;
        var sy = yPos;
        var x1 = xPos - r1;
        var w1 = 2 * r1;
        var y1 = yPos - r1;
        var h1 = 2 * r1;

        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);

        var apertureShape = toScreenSpace.createTransformedShape(new Ellipse2D.Double(x1, y1, w1, h1));
        if (showAperture) {
            g2.draw(apertureShape);
            if (isCentroid) {
                var w1do4 = Math.round(w1 / 4.0);
                var h1do4 = Math.round(h1 / 4.0);
                g2.draw(toScreenSpace.createTransformedShape(new Line2D.Double(sx - w1do4, sy, sx + w1do4, sy)));
                g2.draw(toScreenSpace.createTransformedShape(new Line2D.Double(sx, sy - h1do4, sx, sy + h1do4)));
            }
        }

        Shape backgroundShape = null;
        if (showSky) {
            var x2 = xPos - r2;
            var x3 = xPos - r3;
            var w2 = 2 * r2;
            var w3 = 2 * r3;
            var y2 = yPos - r2;
            var y3 = yPos - r3;
            var h2 = 2 * r2;
            var h3 = 2 * r3;
            g2.draw(toScreenSpace.createTransformedShape(new Ellipse2D.Double(x2, y2, w2, h2)));
            backgroundShape = toScreenSpace.createTransformedShape(new Ellipse2D.Double(x3, y3, w3, h3));
            g2.draw(backgroundShape);
        }

        var b = (backgroundShape != null && showSky ? backgroundShape : apertureShape).getBounds();
        var offset = h / 4;
        if (showName && showValues && nameText != null && !nameText.isEmpty() && !value.isEmpty()) {
            g.drawString(nameText + "=" + value, (int) b.getMaxX() + 5, (int) (b.getMinY() + b.getHeight()/2 + offset));
        } else if (showName && nameText != null && !nameText.isEmpty()) {
            g.drawString(nameText, (int) b.getMaxX() + 5, (int) (b.getMinY() + b.getHeight()/2 + offset));
        } else if (showValues && !value.isEmpty()) {
            g.drawString(value, (int) b.getMaxX() + 5, (int) (b.getMinY() + b.getHeight()/2 + offset));
        }

        if (aij) {
            ((Graphics2D) g).setTransform(ac.canvTrans);
        }
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
    public Aperture.ApertureShape getApertureShape() {
        return Aperture.ApertureShape.CIRCULAR;
    }

    /**
     * Calculates the CORRECT screen x-position of a given decimal pixel position.
     */
//	public int fscreenX (double x) { return netFlipX ? (int)(ac.getWidth()-ac.screenXD(x)-1): ac.screenXD(x); } //-((ac.getWidth())%ac.getMagnification())
//
//	/**
//	 * Calculates the CORRECT screen y-position of a given decimal pixel position.
//	 */
//	public int fscreenY (double y) { return netFlipY ? (int)(ac.getHeight()-ac.screenYD(y)-1): ac.screenYD(y); } //-(ac.getHeight()%ac.getMagnification())

}

