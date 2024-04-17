// Centroid.java		

package astroj;

import ij.IJ;
import ij.ImagePlus;
import ij.Prefs;
import ij.process.ColorProcessor;
import ij.process.ImageProcessor;

import java.awt.*;

/**
 * Calculates the centroid position of an object within an ImageJ ROI.
 *
 * @author F.V. Hessman, Georg-August-Universit t G ttingen
 * @version 1.0
 * @date 2004-Dec-12
 *
 * @autor F.V. Hessman, Georg-August-Universit t G ttingen
 * @version 1.1
 * @date 2006-May-2
 * @changes Added FittedPlane background option.
 *
 * @version 1.2
 * @date 2006-Dec-14
 * @changes Widths are now Gaussian FWHM based on empirical calibration.
 *
 * @version 1.3
 * @date 2006-Dec-20
 * @changes Support for orientation angle and roundness.
 *
 * @version 1.4
 * @date 2007-Jan-30
 * @changes Optional no-positioning (if a given position should be used).
 *
 * @version 1.5
 * @date 2007-Apr-10
 * @changes Added status report to measure???() method "forgiving" error messages.
 *
 * @version 1.6
 * @date 2008-May-11
 * @changes Formal definition of ImageJ pixel center position (not uniformly treated: sometimes the edges,
 * not center, has integer position).
 *
 * @version 1.7
 * @date 2009-Jul-05
 * @changes Added calculation of variance within aperture
 *
 * @version 1.8
 * @date 2011-Mar-24
 * @changes Circular instead of rectangular window
 *
 * @version 1.9
 * @author K.A. Collins, University of Louisville
 * @date 2011-Mar-24
 * @changes Added centroid iterations
 *
 * @version 1.10
 * @author FVH
 * @date 2011-Apr-07
 * @changes Corrected code for the case reposition = false
 * 
 * @version 1.11
 * @author Matthew Craig, Karen Collins
 * @date 2011-Apr-07
 * @changes Added centroid method based on Howell, CCD Astronomy, 2nd Ed., p. 105,
 * Code written and contributed by Matthew Craig, Minnesota State University Moorhead
 */

public class Centroid {
    public static final double PIXELCENTER = 0.5;

    protected double xCenter, yCenter, radius, radius2, rBack1, rBack2, back, backMean;
    protected double xWidth, yWidth;
    protected double angle, ecc;
    protected float srcmax;
    protected double variance;
    //	public boolean forgiving = false;
    protected boolean debug;
    FittedPlane plane = null;
    boolean usePlane = false;
    boolean usePlaneLocal = false;
    boolean removeBackStars = true;
    boolean reposition = true;
    boolean useHowellCentroidMethod = true;

    /**
     * Default instantiation with constant background.
     */
    public Centroid() {
        usePlane = false;
        usePlaneLocal = usePlane;
        debug = Prefs.get("astroj.debug", false);
    }

    /**
     * Optional instantiation with planar background
     */
    public Centroid(boolean withPlane) {
        usePlane = withPlane;
        usePlaneLocal = usePlane;
        debug = Prefs.get("astroj.debug", false);
    }

    /**
     * Determine whether Centroid should reposition the aperture.
     */
    public void setPositioning(boolean flag) {
        reposition = flag;
    }

    /**
     * Set the current position of the aperture.
     */
    public void setPosition(double xx, double yy) {
        xCenter = xx;
        yCenter = yy;
    }

    /**
     * Finds the centroid of an object at a given position and radius.
     */
    public boolean measure(ImagePlus imp, double xx, double yy, double rr, double r1, double r2, boolean findCentroid, boolean useBackgroundPlane, boolean removeStars) {
        ImageProcessor ip = imp.getProcessor();
        reposition = findCentroid;
        usePlane = useBackgroundPlane;
        removeBackStars = removeStars;
        xCenter = xx;
        yCenter = yy;
        radius = rr;
        radius2 = radius * radius;
        rBack1 = r1;
        rBack2 = r2;
        usePlaneLocal = usePlane && (rBack2 > rBack1);
        int i = (int) (xx - rr);
        int j = (int) (yy - rr);
        int w = (int) (2. * rr);
        int h = w;
        Rectangle rct = new Rectangle(i, j, w, h);

        if (ip.getBitDepth() == 24) {
            Frame openFrame = imp.getWindow();
            if (openFrame instanceof AstroStackWindow asw) {
                ColorProcessor cp = asw.getcp();
                if (cp != null) {
                    ip = cp;
                }
            }
        }

        return measure(ip, rct);
    }

    /**
     * Finds the background in an annulus defined by two radii and centered on position xC,yC
     */
    public boolean getBackground(ImageProcessor ip) {
        double di, dj, rad2;
        double r12 = rBack1 * rBack1;
        double r22 = rBack2 * rBack2;

        back = 0.0;
        int backCount = 0;
        srcmax = getSourceMax(ip, xCenter, yCenter, radius);
        if (rBack2 <= rBack1) {
            return true; //no background region, return with background = 0
        }

        int i1 = (int) (xCenter - rBack2);
        int i2 = (int) (xCenter + rBack2);
        int j1 = (int) (yCenter - rBack2);
        int j2 = (int) (yCenter + rBack2);

        double back2 = 0;
        double backstdev = 0;
        double back2Mean = 0;
        backMean = 0;
        double prevBackMean = 0;
        float val = 0;
        float[] pixels = null;
        double[] js = null;
        double[] is = null;
        var pCnt = 0;

        if (removeBackStars) {
            // Copy pixel data for evaluation
            int totalPixels = (i2 - i1 + 1) * (j2 - j1 + 1);
            pixels = new float[totalPixels];
            js = new double[totalPixels];
            is = new double[totalPixels];

            for (int j = j1; j <= j2; j++) { // REMOVE STARS FROM BACKGROUND
                dj = (double) j - yCenter + Centroid.PIXELCENTER;        // Center
                for (int i = i1; i <= i2; i++) {
                    di = (double) i - xCenter + Centroid.PIXELCENTER;    // Center
                    rad2 = di * di + dj * dj;
                    if (rad2 >= r12 && rad2 <= r22) {
                        val = ip.getPixelValue(i, j);
                        if (!Float.isNaN(val)) {
                            js[pCnt] = dj;
                            is[pCnt] = di;
                            pixels[pCnt++] = val;
                        }
                    }
                }
            }

            for (int iteration = 0; iteration < 9; iteration++) { //find iterative background mean value
                backstdev = Math.sqrt(back2Mean - backMean * backMean);
                back = 0.0;
                back2 = 0.0;
                backCount = 0;
                var backMeanPlus2StdDev = backMean + 2.0 * backstdev;
                var backMeanMinus2StdDev = backMean - 2.0 * backstdev;

                // REMOVE STARS FROM BACKGROUND
                for (int i = 0; i < pCnt; i++) {
                    val = pixels[i];
                    if (iteration == 0 || (val <= backMeanPlus2StdDev && val >= backMeanMinus2StdDev)) {
                        back += val;
                        back2 += val * val;
                        backCount++;
                    }
                }

                if (backCount > 0) {
                    back /= backCount;    // MEAN BACKGROUND
                    backMean = back;
                    back2Mean = back2 / (double) backCount;
                }
                if (Math.abs(prevBackMean - backMean) < 0.1) {
                    break;
                }
                prevBackMean = backMean;
            }
        }


        back = 0.0;
        backCount = 0;
        backstdev = Math.sqrt(back2Mean - backMean * backMean);
        if (usePlaneLocal) {
            plane = new FittedPlane((i2 - i1 + 1) * (j2 - j1 + 1));
        }

        // FIND THE SKY BACKGROUND

        var backMeanPlus2StdDev = backMean + 2.0 * backstdev;
        var backMeanMinus2StdDev = backMean - 2.0 * backstdev;
        // Check to see if we have the pixel data for the background already, if not check all pixels
        if (pixels == null) {
            for (int j = j1; j <= j2; j++) {
                dj = (double) j - yCenter + Centroid.PIXELCENTER;
                for (int i = i1; i <= i2; i++) {
                    di = (double) i - xCenter + Centroid.PIXELCENTER;
                    rad2 = di * di + dj * dj;
                    if (rad2 >= r12 && rad2 <= r22) {
                        val = ip.getPixelValue(i, j);
                        if (!removeBackStars || (val <= backMeanPlus2StdDev && val >= backMeanMinus2StdDev)) { // 2011-MAR-24 CORRECTED TO CIRCULAR REGION
                            back += val;
                            backCount++;
                            if (usePlaneLocal) {
                                plane.addPoint(di, dj, val);
                            }
                        }
                    }
                }
            }
        } else {
            for (int i = 0; i < pCnt; i++) {
                val = pixels[i];
                if (!removeBackStars || (val <= backMeanPlus2StdDev && val >= backMeanMinus2StdDev)) { // 2011-MAR-24 CORRECTED TO CIRCULAR REGION
                    back += val;
                    backCount++;
                    if (usePlaneLocal) {
                        plane.addPoint(is[i], js[i], val);
                    }
                }
            }
        }

        if (backCount > 0) {
            back /= backCount;
            backMean = back;
            if (usePlaneLocal && !plane.fitPlane()) {
                IJ.log("Centroid ERROR : cannot fit plane to annulus background, using average background instead.");
                usePlaneLocal = false;
            }
        } else {
            back = 0.0;
            backMean = 0.0;
        }

        srcmax -= (float) back;
        return true;
    }

    public float getSourceMax(ImageProcessor ip, double xCenter, double yCenter, double radius) {
        double di, dj, rad2;
        float val = 0;
        double rr2 = radius * radius;
        float sourceMax;

        int i1 = (int) (xCenter - radius);
        int i2 = (int) (xCenter + radius);
        int j1 = (int) (yCenter - radius);
        int j2 = (int) (yCenter + radius);

        sourceMax = Float.NEGATIVE_INFINITY;
        for (int j = j1; j <= j2; j++) {
            dj = (double) j - yCenter + Centroid.PIXELCENTER;
            for (int i = i1; i <= i2; i++) {
                di = (double) i - xCenter + Centroid.PIXELCENTER;
                rad2 = di * di + dj * dj;
                if (rad2 <= rr2) { // 2011-MAR-24 CORRECTED TO CIRCULAR REGION
                    val = ip.getPixelValue(i, j);
                    if (val > sourceMax) {
                        sourceMax = val;
                    }
                }
            }
        }
        return sourceMax;
    }


    /**
     * Finds the centroid (optional), width, and angle of an object contained within the current ROI.
     */
    public boolean measure(ImageProcessor ip, Rectangle rct) {
        float val;
        double dval;
        int i1 = rct.x;
        int i2 = i1 + rct.width;    // 2011-MAR-24 REMOVED -1;
        int j1 = rct.y;
        int j2 = j1 + rct.height;    // 2011-MAR-24 REMOVED -1;
        int num = 0;

        double xC = 0.0;
        double yC = 0.0;
        double wgt = 0.0;
        double avg = 0.0;
        double rad2 = 0.0;
        double di, dj;
        double xCenterStart = xCenter;
        double yCenterStart = yCenter;

        double I_total, I_bar, J_bar;
        double wgt_i, wgt_j;
        double I_i, J_j;
        xWidth = 0.0;
        yWidth = 0.0;
        angle = 0.0;
        ecc = 0.0;
        variance = 0.0;
        useHowellCentroidMethod = Prefs.get("aperture.useHowellCentroidMethod", useHowellCentroidMethod);

        boolean stillMoving = true;
        int iteration = 100;
        if (!getBackground(ip)) {
            return false;
        }
        if (!reposition) {
            iteration = 0;
        }
        while (stillMoving && iteration > 0) { //iterate and find centroid within aperture
            xC = 0.0;    //bug fix contibuted by Matthew Craig, Minnesota State University Moorhead 10/2/2013
            yC = 0.0;    //bug fix contibuted by Matthew Craig, Minnesota State University Moorhead 10/2/2013
            wgt = 0.0;   //bug fix contibuted by Matthew Craig, Minnesota State University Moorhead 10/2/2013
            num = 0;

            if (!useHowellCentroidMethod) {      // centroid using method written by F.V. Hessman, Georg-August-Universitat Gottingen, source unknown
//                IJ.log("Original centroid method");
                for (int j = j1; j <= j2; j++) {
                    dj = (double) j - yCenter + Centroid.PIXELCENTER;
                    for (int i = i1; i <= i2; i++) {
                        di = (double) i - xCenter + Centroid.PIXELCENTER;
                        rad2 = di * di + dj * dj;
                        if (rad2 <= radius2) { // 2011-MAR-24 CORRECTED TO CIRCULAR REGION
                            val = ip.getPixelValue(i, j);
                            if (!Float.isNaN(val)) {
                                if (usePlaneLocal) {
                                    back = plane.valueAt(di, dj);
                                }
                                dval = (val - back) / srcmax;
                                wgt += dval;
                                xC += dval * di;
                                yC += dval * dj;
                                num++;
//                                IJ.log(""+num+": j,i="+j+","+i+", dj,di"+dj+","+di+", val="+val+", wgt="+wgt+", back="+back+", srcmax="+srcmax);
                            }
                        }
                    }
                }
                if (num > 0) {
                    avg = wgt / (double) num;
                }
                if (wgt != 0.0) {
                    xC /= wgt;
                    yC /= wgt;
                } else {
//                    IJ.log("Centroid ERROR : no signal in centroid using rectangle i1,j1=" + i1 + "," + j1 + ", i2,j2=" + i2 + "," + j2);
                    xCenter = xCenterStart;
                    yCenter = yCenterStart;
                    xWidth = 0.0;
                    yWidth = 0.0;
                    angle = 0.0;
                    ecc = 0.0;
                    variance = 0.0;
                    return false;
                }
            } else {   // Centroid using method based on Howell, CCD Astronomy, 2nd Ed., p. 105
                // Code written and contributed by Matthew Craig, Minnesota State University Moorhead
//                IJ.log("Howell centroid method");
                I_total = 0.0;
                for (int j = j1; j <= j2; j++) {
                    for (int i = i1; i <= i2; i++) {
                        val = ip.getPixelValue(i, j);
                        if (!Float.isNaN(val)) {
                            if (usePlaneLocal) {
                                back = plane.valueAt((double) i - xCenter + Centroid.PIXELCENTER, (double) j - yCenter + Centroid.PIXELCENTER);
                            }
                            I_total += val - back;
                            num++;
                        }
                    }
                }

                I_bar = I_total / (i2 - i1 + 1);
                J_bar = I_total / (j2 - j1 + 1);
                wgt_i = 0;
                for (int i = i1; i <= i2; i++) {
                    I_i = 0;
                    di = (double) i - xCenter + Centroid.PIXELCENTER;
                    for (int j = j1; j <= j2; j++) {
                        val = ip.getPixelValue(i, j);
                        if (!Float.isNaN(val)) {
                            if (usePlaneLocal) {
                                back = plane.valueAt(di, (double) j - yCenter + Centroid.PIXELCENTER);
                            }
                            I_i += val - back;
                        }
                    }
                    dval = I_i - I_bar;
                    if (dval > 0) {
                        wgt_i += dval;
                        xC += dval * di;
                    }
                }

                wgt_j = 0;
                for (int j = j1; j <= j2; j++) {
                    J_j = 0;
                    dj = (double) j - yCenter + Centroid.PIXELCENTER;
                    for (int i = i1; i <= i2; i++) {
                        val = ip.getPixelValue(i, j);
                        if (!Float.isNaN(val)) {
                            if (usePlaneLocal) {
                                back = plane.valueAt((double) i - xCenter + Centroid.PIXELCENTER, dj);
                            }
                            J_j += val - back;
                        }
                    }
                    dval = J_j - J_bar;
                    if (dval > 0) {
                        wgt_j += dval;
                        yC += dval * dj;
                    }
                }

                if (num > 0) {
                    avg = I_total / srcmax / (double) num;
                }
                if ((wgt_i != 0.0) && (wgt_j != 0.0)) {
                    xC /= wgt_i;
                    yC /= wgt_j;
                } else {
//                    IJ.log("Centroid ERROR : no signal in centroid using rectangle i1,j1=" + i1 + "," + j1 + ", i2,j2=" + i2 + "," + j2);
                    xCenter = xCenterStart;
                    yCenter = yCenterStart;
                    xWidth = 0.0;
                    yWidth = 0.0;
                    angle = 0.0;
                    ecc = 0.0;
                    variance = 0.0;
                    return false;
                }
            }


            // CHECK RESULTS

            if (reposition && (Math.abs(xCenter + xC - xCenterStart) > rct.width || Math.abs(yCenter + yC - yCenterStart) > rct.height)) {
                xCenter = xCenterStart;
                yCenter = yCenterStart;
                xWidth = 0.0;
                yWidth = 0.0;
                angle = 0.0;
                ecc = 0.0;
                variance = 0.0;
                return false;  //error reporting should be in parent method
            }
            if (Math.abs(xC) < 0.01 && Math.abs(yC) < 0.01) {
                stillMoving = false;
                // if (debug) IJ.log("interation = "+iteration);
            }
            if (reposition) {
                xCenter += xC;
                yCenter += yC;
                i1 = (int) xCenter - rct.width / 2;
                i2 = i1 + rct.width;
                j1 = (int) yCenter - rct.height / 2;
                j2 = j1 + rct.height;
                if (!getBackground(ip)) {
                    xCenter = xCenterStart;
                    yCenter = yCenterStart;
                    xWidth = 0.0;
                    yWidth = 0.0;
                    angle = 0.0;
                    ecc = 0.0;
                    variance = 0.0;
                    return false;
                }
            }
            iteration--;
        }

        // COMPUTE THE MOMENT WIDTHS AND VARIANCE
        xWidth = 0.0;
        yWidth = 0.0;
        wgt = 0.0;
        double mxy = 0.0;
        for (int j = j1; j <= j2; j++) {
            dj = (double) j - yCenter + Centroid.PIXELCENTER;
            for (int i = i1; i <= i2; i++) {
                di = (double) i - xCenter + Centroid.PIXELCENTER;
                rad2 = di * di + dj * dj;
                if (rad2 <= radius2) { // 2011-MAR-24 CORRECTED TO CIRCULAR REGION
                    val = ip.getPixelValue(i, j);
                    if (!Float.isNaN(val)) {
                        if (usePlaneLocal) {
                            back = plane.valueAt(di, dj);
                        }
                        dval = (val - back) / srcmax;
                        wgt += dval;
                        xWidth += dval * di * di;
                        yWidth += dval * dj * dj;
                        mxy += dval * di * dj;
                        variance += (dval - avg) * (dval - avg);
                    }
                }
            }
        }

        if (wgt != 0.0) {
            xWidth /= wgt;    // m20
            yWidth /= wgt;    // m02
            mxy /= wgt;    // m11

            angle = 0.5 * 180.0 * Math.atan2(2.0 * mxy, xWidth - yWidth) / Math.PI;
            ecc = ((xWidth - yWidth) * (xWidth - yWidth) + 4.0 * mxy * mxy) / ((xWidth + yWidth) * (xWidth + yWidth));

            xWidth = Math.sqrt(xWidth < 0 ? -xWidth : xWidth);
            yWidth = Math.sqrt(yWidth < 0 ? -yWidth : yWidth);

            if (num > 1) {
                variance = variance * srcmax * srcmax / (double) (num - 1);
            }
        } else {
            if (reposition) {
                IJ.log("Centroid ERROR : no signal in aperture for moment widths!");
            }
            xCenter = xCenterStart;
            yCenter = yCenterStart;
            xWidth = 0.0;
            yWidth = 0.0;
            angle = 0.0;
            ecc = 0.0;
            variance = 0.0;
        }

        // FIND X-axis and Y-axis FWHM

//        float justUnderLeft = 0;
//        float justUnderRight = 0;
//        float justOverLeft = 0;
//        float justOverRight = 0;
//        float justUnderTop = 0;
//        float justUnderBottom = 0;
//        float justOverTop = 0;
//        float justOverBottom = 0;        
//        int xCenterPixel = (int)(xCenter);
//        int yCenterPixel = (int)(yCenter);
//        for (int i=i1; i <= xCenterPixel; i++)
//            {
//            val = ip.getPixelValue(i,yCenterPixel);
//            if (!Float.isNaN(val))
//                {
//                di = (double)i-xCenter+Centroid.PIXELCENTER;
//                if (usePlaneLocal)
//                    {
//                    back = plane.valueAt (i-xCenter+Centroid.PIXELCENTER, yCenterPixel-yCenter+Centroid.PIXELCENTER);
//                    }
//                if (val-back
//                }
//            }

        // NORMALIZE WIDTHS TO GIVE GAUSSIAN FWHM (EMPIRICAL FACTOR)

        xWidth /= 0.3602;
        yWidth /= 0.3602;
        return true;
    }

    public double x() {
        return xCenter;
    }

    public double y() {
        return yCenter;
    }

    public double r() {
        return radius;
    }

    public double background() {
        return backMean;
    }

    public double peak() {
        return srcmax;
    }

    public double width() {
        return xWidth;
    }

    public double height() {
        return yWidth;
    }

    public double orientation() {
        return angle;
    }

    public double roundness() {
        return 1.0 - ecc;
    }

    public double variance() {
        return variance;
    }


}
