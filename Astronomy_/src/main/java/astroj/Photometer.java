// Photometer.java

package astroj;

import Astronomy.photometer.RecursivePixelProcessor;
import ij.ImagePlus;
import ij.Prefs;
import ij.astro.io.prefs.Property;
import ij.measure.Calibration;
import ij.process.ColorProcessor;
import ij.process.ImageProcessor;
import util.AdaptiveSimpson;

import java.awt.*;
import java.awt.geom.Area;
import java.awt.geom.FlatteningPathIterator;
import java.awt.geom.PathIterator;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinWorkerThread;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.DoubleAccumulator;
import java.util.concurrent.atomic.LongAdder;

/**
 * Simple aperture photometer using a circular aperture and a background annulus with
 * up to twice the radius.
 *
 * @author F.V. Hessman, Georg-August-Universitaet Goettingen
 * @author Karen Collins (Univ. Louisvill/KY)
 * @author Karen Collins (Univ. Louisvill/KY)
 * @author Karen Collins (Univ. Louisvill/KY)
 * @author Karen Collins (Univ. Louisvill/KY)
 * @version 1.11
 * @date 2005-Feb-17
 * @date 2006-Apr-26
 * @changes Two explicit background radii (FVH)
 * @date 2006-Nov-16
 * @changes Corrected wrong tests for Double.NaN.
 * @date 2007-Jan-29
 * @changes Added support for calculation of errors via gain and RON of CCD.
 * @date 2009-May-11
 * @changes Modified to support ImageJ pixel position standard (centers on half-pixels).
 * @date 2010-Mar-18
 * @changes 1) Changed the "source error" calculation to include dark current and to use the formulation from MERLINE, W.
 * & HOWELL, S.B., 1995, EXP.  ASTRON., 6, 163 (same as in "Handbook of CCD Astronomy" by Steve B. Howell).
 * The resulting code is: serror = Math.sqrt((Math.abs(source*gain)+sourceCount*(1+(double)sourceCount/
 * (double)backCount)*(back*gain+dark+ron*ron))/gain);
 * @date 2010-Nov-24
 * @changes Added support for removal of stars from background region (>3 sigma from mean)
 * @date 2010-Nov-24
 * @changes Merged fix for aperture position bug and support for debug flag (from F.V. Hessman)
 * @date 2010-Mar-18
 * @changes 1) Corrected the "source error" calculation by taking gain^{-1} out of the sqrt operation to match MERLINE, W.
 * & HOWELL, S.B., 1995, EXP.  ASTRON., 6, 163 (same as in "Handbook of CCD Astronomy" by Steve B. Howell).
 * serror = Math.sqrt(Math.abs(source*gain)+sourceCount*(1.+(double)sourceCount/(double)backCount)
 * *(back*gain+dark+ron*ron+gain*gain*0.083521))/gain;
 */
public class Photometer {
    private static final double FUDGE_CONST = 2 * Math.sqrt(2);
    /**
     * Center position of aperture in pixels.
     */
    protected double xCenter = 0.0, yCenter = 0.0;
    /**
     * Radius of source aperture in pixels.
     */
    protected double radius = 0.0;
    /**
     * Mean background/pixel and integrated source, calibrated if possible.
     */
    protected double back = 0.0, back2 = 0.0, backMean = 0.0, prevBackMean = 0.0, backi = 0.0, backo = 0.0;
    protected double btot = 0;
    protected double back2Mean, backstdev = 0, source = 0;
    /**
     * Error in source based on photon statistics.
     */
    protected double serror;
    /**
     * Mean background/pixel and integrated source, uncalibrated.
     */
    protected double rawSource = 0, rawBack = 0;
    /**
     * Radii of background aperture.
     */
    protected double rBack1, rBack2;
    /**
     * Number of pixels in source and background apertures.
     */
    protected long sourceCount = 0, backCount = 0;
    protected double dSourceCount = 0.0, dBackCount = 0.0;
    /**
     * Peak level in ROI
     */
    protected float peak;
    protected double mean;
    /**
     * CCD gain [e- per ADU or count]
     */
    protected double fwhm = Double.NaN;
    protected double gain = 1.0;
    /**
     * CCD read-out-noise [e-]
     */
    protected double ron = 0.0;
    /**
     * CCD Dark Current per pixel [e-/pix]
     */
    protected double dark = 0.0;
    protected boolean removeBackStars = true;
    protected boolean markRemovedPixels = false;
    protected boolean isMultiAperture;
    protected boolean exact = true;
    protected boolean hasBack = true;
    /**
     * Debug flag
     */
    protected boolean debug = false;
    protected FittedPlane plane = null;
    protected boolean usePlane = false;
    protected OverlayCanvas ocanvas;
    protected BulkPixelRoi bulkPixelRoi;
    /**
     * Calibration object of client.
     */
    Calibration calib;
    private static final ForkJoinPool POOL =
            new ForkJoinPool(Runtime.getRuntime().availableProcessors(),
                    new PhotometerWorkerThreadFactory(), null, true);
    public static final Property<Boolean> USE_PARALLEL_PIXEL_PROCESS = new Property<>(true, Photometer.class);

    /**
     * Initializes Photometer without the client's Calibration.
     */
    public Photometer() {
        calib = null;
        radius = Double.NaN;
        rBack1 = Double.NaN;
        rBack2 = Double.NaN;
    }

    /**
     * Initializes Photometer without the client's Calibration.
     */
    public Photometer(boolean isMultiAperture) {
        calib = null;
        radius = Double.NaN;
        rBack1 = Double.NaN;
        rBack2 = Double.NaN;
        this.isMultiAperture = isMultiAperture;
    }

    /**
     * Initializes Photometer with the client ImagePlus's Calibration object.
     *
     * @param cal client's Calibration object
     */
    public Photometer(Calibration cal) {
        calib = cal;
        radius = Double.NaN;
        rBack1 = Double.NaN;
        rBack2 = Double.NaN;
    }

    /**
     * Initializes Photometer with the client ImagePlus's Calibration object.
     *
     * @param cal client's Calibration object
     */
    public Photometer(Calibration cal, boolean isMultiAperture) {
        calib = cal;
        radius = Double.NaN;
        rBack1 = Double.NaN;
        rBack2 = Double.NaN;
        this.isMultiAperture = isMultiAperture;
    }

    public void measure(ImagePlus imp, Aperture aperture, boolean exactPixels) {
        switch (aperture.getApertureShape()) {
            case CIRCULAR -> {
                if (aperture instanceof ApertureRoi apertureRoi) {
                    measure(imp, exactPixels, apertureRoi.xPos, apertureRoi.yPos, apertureRoi.r1, apertureRoi.r2, apertureRoi.r3);
                }
            }
            case FREEFORM_SHAPE -> {
                if (aperture instanceof ShapedApertureRoi apertureRoi) {
                    measure(imp, apertureRoi, exactPixels);
                }
            }
            case FREEFORM_PIXEL -> {
                if (aperture instanceof FreeformPixelApertureRoi apertureRoi) {
                    measure(imp, apertureRoi, exactPixels);
                }
            }
        }
    }

    public void measure(ImagePlus imp, ShapedApertureRoi apertureRoi, boolean exactPixels) {
        var ip = imp.getProcessor();
        exact = exactPixels;
        xCenter = apertureRoi.xPos;
        yCenter = apertureRoi.yPos;
        radius = apertureRoi.getRadius();
        rBack1 = apertureRoi.getBack1();
        rBack2 = apertureRoi.getBack2();
        ocanvas = OverlayCanvas.getOverlayCanvas(imp);
        hasBack = apertureRoi.hasBackground();
        boolean usePlaneLocal = usePlane && hasBack;

        debug = Prefs.get("astroj.debug", false);

        if (ip.getBitDepth() == 24) {
            Frame openFrame = imp.getWindow();
            if (openFrame instanceof AstroStackWindow asw) {
                ColorProcessor cp = asw.getcp();
                if (cp != null) {
                    ip = cp;
                }
            }
        }

        // INTEGRATE STAR WITHIN APERTURE OF RADIUS radius, SKY OUTSIDE

        source = 0.0;
        btot = 0.0;
        back = 0.0;
        backi = 0.0;
        backo = 0.0;
        mean = 0.0;
        prevBackMean = 0;
        dSourceCount = 0.0;
        dBackCount = 0.0;
        sourceCount = 0;
        backCount = 0;
        back2 = 0;
        boolean fitPlaneError = false;

        var apertureArea = apertureRoi.getApertureArea();
        var backgroundArea = apertureRoi.getBackgroundArea();

        if (apertureArea == null) {
            return;
        }

        var sourceBounds = clampBounds(imp, apertureArea.getBounds());
        assert hasBack == (backgroundArea != null && !backgroundArea.isEmpty());
        var backgroundBounds = hasBack ? clampBounds(imp, backgroundArea.getBounds()) : new Rectangle();
        int totalPixels = sourceBounds.height * sourceBounds.width + (backgroundBounds.height * backgroundBounds.width);
        if (usePlaneLocal) {
            plane = new FittedPlane(totalPixels);
        }

        var sourceRegion = new RecursivePixelProcessor.AreaRegion(apertureArea);
        var backgroundRegion = new RecursivePixelProcessor.AreaRegion(backgroundArea);

        var localApertureArea = ThreadLocal.withInitial(apertureRoi::getApertureArea);
        var localBackgroundArea = ThreadLocal.withInitial(apertureRoi::getBackgroundArea);

        var localNegApArea = ThreadLocal.withInitial(() -> {
            var a = new Area(sourceRegion.getBounds());
            a.subtract(apertureArea);
            return a;
        });
        var localNegBackArea = ThreadLocal.withInitial(() -> {
            if (!hasBack) {
                return new Area();
            }

            var a = new Area(backgroundRegion.getBounds());
            a.subtract(backgroundArea);
            return a;
        });

        peak = Float.NEGATIVE_INFINITY;
        if (exact) {
            if (USE_PARALLEL_PIXEL_PROCESS.get()) {
                var sourceAdder = sourceRegion.createPixelStorage(ip);
                var dSourceCountAdder = sourceRegion.createPixelStorage(ip);
                var peakAccumulator = new DoubleAccumulator(Math::max, peak);


                var task = new RecursivePixelProcessor(sourceRegion, ip, (i, j, d) -> {
                    var fraction = 0D;
                    var area = localApertureArea.get();
                    assert area != null;
                    // Contains is not thread safe, even if pixel is local,
                    // so must be synchronized or use a new object
                    if (area.contains(i, j, 1, 1)) {
                        fraction = 1;
                    } else if (!localNegApArea.get().contains(i, j, 1, 1)) {
                        var pixel = new Area(new Rectangle(i, j, 1, 1));
                        // Intersection is not thread safe, even if pixel is local,
                        // so must be synchronized or use a new object
                        pixel.intersect(area);
                        fraction = integrateArea(pixel, false);
                    }

                    sourceAdder.setVal(i, j, fraction * d);
                    dSourceCountAdder.setVal(i, j, fraction);

                    if (fraction > 0.01) {
                        peakAccumulator.accumulate(d);
                    }

                    return false;
                });

                POOL.invoke(task);

                source = sourceAdder.sum();
                dSourceCount = dSourceCountAdder.sum();
                peak = (float) peakAccumulator.get();

                if (hasBack) {
                    assert backgroundArea != null;

                    var backAdder = backgroundRegion.createPixelStorage(ip);
                    var dBackCountAdder = backgroundRegion.createPixelStorage(ip);
                    var backCountAdder = new LongAdder();

                    task = new RecursivePixelProcessor(backgroundRegion, ip, (i, j, d) -> {
                        var fraction = 0D;
                        var area = localBackgroundArea.get();
                        assert area != null;
                        // Contains is not thread safe, even if pixel is local,
                        // so must be synchronized or use a new object
                        if (area.contains(i, j, 1, 1)) {
                            fraction = 1;
                        } else if (!localNegBackArea.get().contains(i, j, 1, 1)) {
                            var pixel = new Area(new Rectangle(i, j, 1, 1));
                            // Intersection is not thread safe, even if pixel is local,
                            // so must be synchronized or use a new object
                            pixel.intersect(area);
                            fraction = integrateArea(pixel, false);
                        }

                        if (!removeBackStars && !usePlaneLocal) {
                            backAdder.setVal(i, j, fraction * d);
                            dBackCountAdder.setVal(i, j, fraction);
                        } else if (fraction > 0) { // BACKGROUND
                            backCount++;
                            backAdder.setVal(i, j, fraction * d);
                            backCountAdder.increment();

                            return usePlaneLocal;
                        }

                        return false;
                    });

                    var allPlanePoints = POOL.invoke(task);

                    if (usePlaneLocal) {
                        for (RecursivePixelProcessor.Point3D p : allPlanePoints) {
                            plane.addPoint(p.x(), p.y(), p.z());
                        }
                    }

                    back = backAdder.sum();
                    back2 = back * back;
                    dBackCount = dBackCountAdder.sum();
                    backCount = backCountAdder.sum();
                }
            } else {
                var bounds = clampBounds(imp, apertureArea.getBounds()); // Integer bounds to ensure we get all pixels

                for (int i = bounds.x; i < bounds.x + bounds.width; i++) {
                    for (int j = bounds.y; j < bounds.y + bounds.height; j++) {
                        var d = ip.getPixelValue(i, j);
                        if (!Float.isNaN(d)) {
                            var fraction = 0D;
                            if (apertureArea.contains(i, j, 1, 1)) {
                                fraction = 1;
                            } else if (!localNegApArea.get().contains(i, j, 1, 1)) {
                                var pixel = new Area(new Rectangle(i, j, 1, 1));
                                pixel.intersect(apertureArea);

                                // Move overlapped shape to have corner on origin
                                //pixel.transform(AffineTransform.getTranslateInstance(-i, -j));

                                fraction = integrateArea(pixel, false);
                            }

                            source += fraction * d;
                            dSourceCount += fraction;

                            if (fraction > 0.01 && d > peak) {
                                peak = d;
                            }
                        }
                    }
                }

                if (hasBack) {
                    assert backgroundArea != null;
                    bounds = clampBounds(imp, backgroundArea.getBounds()); // Integer bounds to ensure we get all pixels

                    for (int i = bounds.x; i < bounds.x + bounds.width; i++) {
                        for (int j = bounds.y; j < bounds.y + bounds.height; j++) {
                            var d = ip.getPixelValue(i, j);
                            if (!Float.isNaN(d)) {
                                var fraction = 0D;
                                if (backgroundArea.contains(i, j, 1, 1)) {
                                    fraction = 1;
                                } else if (!localNegBackArea.get().contains(i, j, 1, 1)) {
                                    var pixel = new Area(new Rectangle(i, j, 1, 1));
                                    pixel.intersect(backgroundArea);

                                    // Move overlapped shape to have corner on origin
                                    //pixel.transform(AffineTransform.getTranslateInstance(-i, -j));

                                    fraction = integrateArea(pixel, false);
                                }

                                if (!removeBackStars && !usePlaneLocal) {
                                    back += fraction * d;
                                    dBackCount += fraction;
                                } else if (fraction > 0) { // BACKGROUND
                                    back += d;
                                    back2 += d * d;
                                    backCount++;
                                    if (usePlaneLocal) {
                                        plane.addPoint(i, j, d);
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (removeBackStars || usePlaneLocal) {
                dBackCount = backCount;
            }
        } else {
            if (USE_PARALLEL_PIXEL_PROCESS.get()) {
                var sourceAdder = sourceRegion.createPixelStorage(ip);
                var sourceCountAdder = new LongAdder();
                var peakAccumulator = new DoubleAccumulator(Math::max, peak);

                var task = new RecursivePixelProcessor(sourceRegion, ip, (i, j, d) -> {
                    var area = localApertureArea.get();
                    assert area != null;
                    // Contains is not thread safe, even if pixel is local,
                    // so must be synchronized or use a new object
                    if (area.contains(i + 0.5, j + 0.5)) {
                        sourceAdder.setVal(i, j, d);
                        sourceCountAdder.increment();

                        peakAccumulator.accumulate(peak);
                    }

                    return false;
                });

                POOL.invoke(task);

                source = sourceAdder.sum();
                sourceCount = sourceCountAdder.sum();
                peak = (float) peakAccumulator.get();

                if (hasBack) {
                    assert backgroundArea != null;

                    var backAdder = backgroundRegion.createPixelStorage(ip);
                    var backCountAdder = new LongAdder();

                    task = new RecursivePixelProcessor(backgroundRegion, ip, (i, j, d) -> {
                        var area = localBackgroundArea.get();
                        assert area != null;
                        // Contains is not thread safe, even if pixel is local,
                        // so must be synchronized or use a new object
                        if (area.contains(i + 0.5, j + 0.5)) {
                            backAdder.setVal(i, j, d);
                            backCountAdder.increment();
                            return usePlaneLocal;
                        }

                        return false;
                    });

                    var allPlanePoints = POOL.invoke(task);

                    if (usePlaneLocal) {
                        for (RecursivePixelProcessor.Point3D p : allPlanePoints) {
                            plane.addPoint(p.x(), p.y(), p.z());
                        }
                    }

                    back = backAdder.sum();
                    back2 = back * back;
                    backCount = backCountAdder.sum();
                }
            } else {
                var bounds = clampBounds(imp, apertureArea.getBounds()); // Integer bounds to ensure we get all pixels

                for (int i = bounds.x; i < bounds.x + bounds.width; i++) {
                    for (int j = bounds.y; j < bounds.y + bounds.height; j++) {
                        var d = ip.getPixelValue(i, j);
                        if (!Float.isNaN(d)) {
                            if (apertureArea.contains(i + 0.5, j + 0.5)) {
                                source += d;
                                sourceCount++;
                                if (d > peak) {
                                    peak = d;
                                }
                            }
                        }
                    }
                }

                if (hasBack) {
                    assert backgroundArea != null;
                    bounds = clampBounds(imp, backgroundArea.getBounds()); // Integer bounds to ensure we get all pixels

                    for (int i = bounds.x; i < bounds.x + bounds.width; i++) {
                        for (int j = bounds.y; j < bounds.y + bounds.height; j++) {
                            var d = ip.getPixelValue(i, j);
                            if (!Float.isNaN(d)) {
                                if (backgroundArea.contains(i + 0.5, j + 0.5)) {
                                    back += d;
                                    back2 += d * d;
                                    backCount++;
                                    if (usePlaneLocal) {
                                        plane.addPoint(i, j, d);
                                    }
                                }
                            }
                        }
                    }
                }
            }

            dSourceCount = sourceCount;
            dBackCount = backCount;
        }

        if (hasBack && (dBackCount > 0.0)) {
            back /= dBackCount;    // MEAN BACKGROUND
        }

        if (hasBack && removeBackStars && (dBackCount > 3.0)) {
            backMean = back;
            back2Mean = back2 / dBackCount;

            // Copy pixel data for evaluation
            var pixels = new float[totalPixels];
            var js = new int[totalPixels];
            var is = new int[totalPixels];
            var pCnt = 0;

            // Integer bounds to ensure we get all pixels

            if (USE_PARALLEL_PIXEL_PROCESS.get()) {
                var indexer = new AtomicInteger();

                var task = new RecursivePixelProcessor(backgroundRegion, ip, false, (i, j, d) -> {
                    var area = localBackgroundArea.get();
                    assert area != null;
                    // Contains is not thread safe, even if pixel is local,
                    // so must be synchronized or use a new object
                    if (area.contains(i + 0.5, j + 0.5)) {
                        if (!Double.isNaN(d)) {
                            var idx = indexer.getAndIncrement();
                            js[idx] = j;
                            is[idx] = i;
                            pixels[idx] = (float) d;
                        } else {
                            return markRemovedPixels; // Mark NaN pixels
                        }
                    }

                    return false;
                });

                var allPlanePoints = POOL.invoke(task);

                if (markRemovedPixels) {
                    for (RecursivePixelProcessor.Point3D p : allPlanePoints) {
                        addPixelRoi(imp, p.x(), p.y());
                    }
                }

                pCnt = indexer.get();
            } else {
                for (int i = backgroundBounds.x; i < backgroundBounds.x + backgroundBounds.width; i++) {
                    for (int j = backgroundBounds.y; j < backgroundBounds.y + backgroundBounds.height; j++) {
                        var d = ip.getPixelValue(i, j);
                        if (backgroundArea.contains(i + 0.5, j + 0.5)) {
                            if (!Float.isNaN(d)) {
                                js[pCnt] = j;
                                is[pCnt] = i;
                                pixels[pCnt++] = d;
                            } else if (markRemovedPixels) {
                                addPixelRoi(imp, i, j); // Mark NaN pixels
                            }
                        }
                    }
                }
            }

            for (int iteration = 0; iteration < 100; iteration++) {
                backstdev = Math.sqrt(back2Mean - backMean * backMean);
                back = 0.0;
                back2 = 0.0;
                backCount = 0;

                if (usePlaneLocal) {
                    plane = new FittedPlane(totalPixels);
                }
                if (markRemovedPixels && !isMultiAperture) {
                    ocanvas.removePixelRois();
                }

                // REMOVE STARS FROM BACKGROUND
                var backMeanPlus2Stdev = backMean + 2.0 * backstdev;
                var backMeanMinus2Stdev = backMean - 2.0 * backstdev;
                for (int i = 0; i < pCnt; i++) {
                    var d = pixels[i];
                    if ((d <= backMeanPlus2Stdev) && (d >= backMeanMinus2Stdev)) {
                        back += d; // FINAL BACKGROUND
                        back2 += d * d;
                        backCount++;
                        if (usePlaneLocal) {
                            plane.addPoint(is[i], js[i], d);
                        }
                    } else if (markRemovedPixels) {
                        addPixelRoi(imp,is[i], js[i]);
                    }
                }

                if (backCount > 0) {
                    back /= backCount;    // MEAN BACKGROUND
                    backMean = back;
                    back2Mean = back2 / backCount;
                }

                if (Math.abs(prevBackMean - backMean) < 0.0001) { //was 0.1 which did not work for for low background levels
                    break;
                }
                prevBackMean = backMean;
            }

            dBackCount = (double) backCount;
            if (markRemovedPixels) {
                AstroCanvas ac = (AstroCanvas) imp.getCanvas();
                ac.paint(ac.getGraphics());
            }
        }

        if (usePlaneLocal && !plane.fitPlane()) {
            //IJ.log("Photometer ERROR : cannot fit plane to background, using average background instead.");
            fitPlaneError = true;
        }

        btot = back * dSourceCount;

        if (usePlaneLocal && !fitPlaneError) {
            source = 0.0;
            back = 0.0;
            dSourceCount = 0.0;
            if (exact) {
                if (USE_PARALLEL_PIXEL_PROCESS.get()) {
                    var sourceAdder = sourceRegion.createPixelStorage(ip);
                    var backAdder = sourceRegion.createPixelStorage(ip);
                    var dSourceCountAdder = sourceRegion.createPixelStorage(ip);

                    var task = new RecursivePixelProcessor(sourceRegion, ip, (i, j, d) -> {
                        var fraction = 0D;
                        var area = localApertureArea.get();
                        assert area != null;
                        // Contains is not thread safe, even if pixel is local,
                        // so must be synchronized or use a new object
                        if (area.contains(i, j, 1, 1)) {
                            fraction = 1;
                        } else if (!localNegApArea.get().contains(i, j, 1, 1)) {
                            var pixel = new Area(new Rectangle(i, j, 1, 1));
                            // Intersection is not thread safe, even if pixel is local,
                            // so must be synchronized or use a new object
                            pixel.intersect(area);
                            fraction = integrateArea(pixel, false);
                        }

                        var b = plane.valueAt(i, j);

                        sourceAdder.setVal(i, j, (d - b) * fraction);
                        backAdder.setVal(i, j, b * fraction);
                        dSourceCountAdder.setVal(i, j, fraction);

                        return false;
                    });

                    POOL.invoke(task);

                    source = sourceAdder.sum();
                    back = backAdder.sum();
                    dSourceCount = dSourceCountAdder.sum();
                } else {
                    var bounds = clampBounds(imp, apertureArea.getBounds()); // Integer bounds to ensure we get all pixels

                    for (int i = bounds.x; i < bounds.x + bounds.width; i++) {
                        for (int j = bounds.y; j < bounds.y + bounds.height; j++) {
                            var d = ip.getPixelValue(i, j);
                            if (!Float.isNaN(d)) {
                                var fraction = 0D;
                                if (apertureArea.contains(i, j, 1, 1)) {
                                    fraction = 1;
                                } else if (!localNegApArea.get().contains(i, j, 1, 1)) {
                                    var pixel = new Area(new Rectangle(i, j, 1, 1));
                                    pixel.intersect(apertureArea);

                                    // Move overlapped shape to have corner on origin
                                    //pixel.transform(AffineTransform.getTranslateInstance(-i, -j));

                                    fraction = integrateArea(pixel, false);
                                }

                                dSourceCount += fraction;
                                var b = plane.valueAt(i, j);
                                back += b * fraction;
                                source += (d - b) * fraction;
                            }
                        }
                    }
                }

                if (dSourceCount > 0) {
                    back /= dSourceCount;
                }
            } else {
                if (USE_PARALLEL_PIXEL_PROCESS.get()) {
                    var sourceAdder = sourceRegion.createPixelStorage(ip);
                    var backAdder = sourceRegion.createPixelStorage(ip);
                    var sourceCountAdder = new LongAdder();

                    var task = new RecursivePixelProcessor(sourceRegion, ip, (i, j, d) -> {
                        var area = localApertureArea.get();
                        assert area != null;
                        // Contains is not thread safe, even if pixel is local,
                        // so must be synchronized or use a new object
                        if (area.contains(i + 0.5, j + 0.5)) {
                            var b = plane.valueAt(i, j);

                            sourceAdder.setVal(i, j, (d - b));
                            backAdder.setVal(i, j, b);
                            sourceCountAdder.increment();
                        }

                        return false;
                    });

                    POOL.invoke(task);

                    source = sourceAdder.sum();
                    back = backAdder.sum();
                    dSourceCount = sourceCountAdder.sum();
                } else {
                    var bounds = clampBounds(imp, apertureArea.getBounds()); // Integer bounds to ensure we get all pixels

                    for (int i = bounds.x; i < bounds.x + bounds.width; i++) {
                        for (int j = bounds.y; j < bounds.y + bounds.height; j++) {
                            var d = ip.getPixelValue(i, j);
                            if (!Float.isNaN(d)) {
                                if (apertureArea.contains(i + 0.5, j + 0.5)) {
                                    dSourceCount++;
                                    var b = plane.valueAt(i, j);
                                    back += b;
                                    source += (d - b);
                                }
                            }
                        }
                    }
                }

                if (dSourceCount > 0) {
                    back /= dSourceCount;
                }
            }
        } else {
            source -= btot;
        }

        if (dSourceCount > 0.0) {
            mean = source / dSourceCount;
        }

        double src = 0.0;
        double bck = 0.0;
        double sCnt = 0.0;
        double srcCnt = 0.0;
        double bckCnt = 0.0;

        // ERROR FROM GAIN (e-/count), RON (e-), DARK CURRENT (e-) AND POISSON STATISTICS
        // SEE MERLINE, W. & HOWELL, S.B., 1995, EXP. ASTRON., 6, 163
        src = (source < 0.0 || dSourceCount <= 0.0) ? 0.0 : source;
        bck = (back < 0.0 || dBackCount <= 0) ? 0.0 : back;
        sCnt = Math.max(dSourceCount, 0.0);
        srcCnt = (dSourceCount <= 0.0 || dBackCount <= 0) ? 0.0 : dSourceCount;
        bckCnt = (dBackCount <= 0) ? 1.0 : dBackCount;

        serror = Math.sqrt((src * gain) + sCnt * (1.0 + srcCnt / bckCnt) * (bck * gain + dark + ron * ron + gain * gain * 0.083521)) / gain;
        fwhm = IJU.radialDistributionFWHM(ip, apertureRoi.getXpos(), apertureRoi.getYpos(), radius, back);

        // CALIBRATE INTENSITIES IF POSSIBLE

        rawSource = source;
        rawBack = 0.0;
        if (calib != null && calib.calibrated()) {
            rawSource = calib.getRawValue(source);
            if (dBackCount > 0) {
                rawBack = calib.getRawValue(back);
            }
        }
    }

    public void measure(ImagePlus imp, FreeformPixelApertureRoi apertureRoi, boolean exactPixels) {
        var ip = imp.getProcessor();
        exact = exactPixels;
        xCenter = apertureRoi.xPos;
        yCenter = apertureRoi.yPos;
        radius = apertureRoi.getRadius();
        rBack1 = apertureRoi.getBack1();
        rBack2 = apertureRoi.getBack2();
        ocanvas = OverlayCanvas.getOverlayCanvas(imp);
        hasBack = apertureRoi.hasBackground();
        boolean usePlaneLocal = usePlane && hasBack;

        debug = Prefs.get("astroj.debug", false);

        if (ip.getBitDepth() == 24) {
            Frame openFrame = imp.getWindow();
            if (openFrame instanceof AstroStackWindow asw) {
                ColorProcessor cp = asw.getcp();
                if (cp != null) {
                    ip = cp;
                }
            }
        }

        final var useBackgroundAnnulus = apertureRoi.hasAnnulus() &&
                !Double.isNaN(apertureRoi.getBack1()) && !Double.isNaN(apertureRoi.getBack2()) &&
                apertureRoi.getBack1() < apertureRoi.getBack2();

        double r2b1 = 0;
        double r2b2 = 0;
        double xpix = 0;
        double ypix = 0;
        double r = 0;
        int i1 = 0;
        int i2 = 0;
        int j1 = 0;
        int j2 = 0;

        if (useBackgroundAnnulus) {
            r2b1 = apertureRoi.getBack1() * apertureRoi.getBack1();
            r2b2 = apertureRoi.getBack2() * apertureRoi.getBack2();

            xpix = apertureRoi.getXpos();
            ypix = apertureRoi.getYpos();

            r = apertureRoi.getBack2() + 2;

            i1 = (int) (xpix - r);
            if (i1 < 0) {
                i1 = 0;
            } else if (i1 >= ip.getWidth()) {
                i1 = ip.getWidth() - 1;
            }

            i2 = (int) (xpix + r) + 1;
            if (i2 < 0) {
                i2 = 0;
            } else if (i2 >= ip.getWidth()) {
                i2 = ip.getWidth() - 1;
            }

            j1 = (int) (ypix - r);
            if (j1 < 0) {
                j1 = 0;
            } else if (j1 >= ip.getHeight()) {
                j1 = ip.getHeight() - 1;
            }

            j2 = (int) (ypix + r) + 1;
            if (j2 < 0) {
                j2 = 0;
            } else if (j2 >= ip.getHeight()) {
                j2 = ip.getHeight() - 1;
            }
        }

        float d;

        // INTEGRATE STAR WITHIN APERTURE OF RADIUS radius, SKY OUTSIDE

        source = 0.0;
        btot = 0.0;
        back = 0.0;
        backi = 0.0;
        backo = 0.0;
        mean = 0.0;
        prevBackMean = 0;
        dSourceCount = 0.0;
        dBackCount = 0.0;
        sourceCount = 0;
        backCount = 0;
        back2 = 0;
        boolean fitPlaneError = false;

        int totalPixels = apertureRoi.pixelCount() + (useBackgroundAnnulus ? (i2 - i1 + 1) * (j2 - j1 + 1) : 0);
        if (usePlaneLocal) {
            plane = new FittedPlane(totalPixels);
        }

        peak = Float.NEGATIVE_INFINITY;
        if (exact) {
            for (FreeformPixelApertureRoi.Pixel pixel : apertureRoi.iterable()) {
                d = ip.getPixelValue(pixel.x(), pixel.y());
                if (!Float.isNaN(d)) {
                    if (!pixel.isBackground()) {
                        source += d;
                        dSourceCount++;
                    }
                    if (d > peak) {
                        peak = d;
                    }
                    if (hasBack) {
                        if (!removeBackStars && !usePlaneLocal) {
                            back += d;
                            dBackCount++;
                        } else if (pixel.isBackground()) { // BACKGROUND
                            back += d;
                            back2 += d * d;
                            backCount++;
                            if (usePlaneLocal) {
                                plane.addPoint(pixel.x(), pixel.y(), d);
                            }
                        }
                    }
                }
            }

            // Integrate over background annulus
            if (useBackgroundAnnulus) {
                for (int j = j1; j <= j2; j++) {
                    var dj = (double) j + Centroid.PIXELCENTER - ypix;        // pixel center
                    for (int i = i1; i <= i2; i++) {
                        var di = (double) i + Centroid.PIXELCENTER - xpix;    // pixel center
                        var r2 = di * di + dj * dj;                         // radius to pixel center
                        d = ip.getPixelValue(i, j);
                        if (!Float.isNaN(d)) {
                            /*var fraction = intarea(xpix, ypix, radius, i, i + 1, j, j + 1);
                            source += fraction * d;
                            dSourceCount += fraction;
                            if (fraction > 0.01 && d > peak) {
                                peak = d;
                            }*/
                            if (hasBack) {
                                // Pixel was added to background and overlaps annulus
                                if (apertureRoi.contains(i, j, true)) {
                                    continue;
                                }
                                if (!removeBackStars && !usePlaneLocal) {
                                    var fraction = intarea(xpix, ypix, rBack1, i, i + 1, j, j + 1);
                                    back -= fraction * d;
                                    dBackCount -= fraction;
                                    fraction = intarea(xpix, ypix, rBack2, i, i + 1, j, j + 1);
                                    back += fraction * d;
                                    dBackCount += fraction;
                                } else if (r2 >= r2b1 && r2 <= r2b2) { // BACKGROUND
                                    back += d;
                                    back2 += d * d;
                                    backCount++;
                                    if (usePlaneLocal) {
                                        plane.addPoint(di, dj, d);
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (removeBackStars || usePlaneLocal) {
                dBackCount = backCount;
            }
        } else {
            for (FreeformPixelApertureRoi.Pixel pixel : apertureRoi.iterable()) {
                d = ip.getPixelValue(pixel.x(), pixel.y());
                if (!Float.isNaN(d)) {
                    if (!pixel.isBackground()) { // SOURCE APERTURE
                        source += d;
                        sourceCount++;
                        if (d > peak) {
                            peak = d;
                        }
                    }
                    if (hasBack && pixel.isBackground()) { // BACKGROUND
                        back += d;
                        back2 += d * d;
                        backCount++;
                        if (usePlaneLocal) {
                            plane.addPoint(pixel.x(), pixel.y(), d);
                        }
                    }
                }
            }

            // Integrate over background annulus
            if (useBackgroundAnnulus) {
                for (int j = j1; j <= j2; j++) {
                    var dj = (double) j + Centroid.PIXELCENTER - ypix;        // Center;
                    for (int i = i1; i <= i2; i++) {
                        var di = (double) i + Centroid.PIXELCENTER - xpix;    // Center;
                        var r2 = di * di + dj * dj;
                        d = ip.getPixelValue(i, j);
                        if (!Float.isNaN(d)) {
                            /*if (r2 < r2ap) { // SOURCE APERTURE
                                source += d;
                                sourceCount++;
                                if (d > peak) {
                                    peak = d;
                                }
                            }*/
                            if (hasBack && r2 >= r2b1 && r2 <= r2b2) { // BACKGROUND
                                // Pixel was added to background and overlaps annulus
                                if (apertureRoi.contains(i, j, true)) {
                                    continue;
                                }
                                back += d;
                                back2 += d * d;
                                backCount++;
                                if (usePlaneLocal) {
                                    plane.addPoint(di, dj, d);
                                }
                            }
                        }
                    }
                }
            }

            dSourceCount = sourceCount;
            dBackCount = backCount;
        }

        if (hasBack && (dBackCount > 0.0)) {
            back /= dBackCount;    // MEAN BACKGROUND
        }

        if (hasBack && removeBackStars && (dBackCount > 3.0)) {
            backMean = back;
            back2Mean = back2 / dBackCount;

            // Copy pixel data for evaluation
            var pixels = new float[totalPixels];
            var js = new int[totalPixels];
            var is = new int[totalPixels];
            var pCnt = 0;

            for (FreeformPixelApertureRoi.Pixel pixel : apertureRoi.iterable()) {
                if (pixel.isBackground()) {
                    d = ip.getPixelValue(pixel.x(), pixel.y());
                    if (!Float.isNaN(d)) {
                        is[pCnt] = pixel.x();
                        js[pCnt] = pixel.y();
                        pixels[pCnt++] = d;
                    } else if (markRemovedPixels) {
                        addPixelRoi(imp, pixel.x(), pixel.y()); // Mark NaN pixels
                    }
                }
            }

            if (useBackgroundAnnulus) {
                for (int j = j1; j <= j2; j++) {
                    var dj = (double) j - ypix + Centroid.PIXELCENTER;        // Center
                    for (int i = i1; i <= i2; i++) {
                        var di = (double) i - xpix + Centroid.PIXELCENTER;    // Center
                        var r2 = di * di + dj * dj;
                        if (r2 >= r2b1 && r2 <= r2b2) {
                            d = ip.getPixelValue(i, j);
                            // Pixel was added to background and overlaps annulus
                            if (apertureRoi.contains(i, j, true)) {
                                continue;
                            }
                            if (!Float.isNaN(d)) {
                                js[pCnt] = j;
                                is[pCnt] = i;
                                pixels[pCnt++] = d;
                            } else if (markRemovedPixels) {
                                addPixelRoi(imp, i, j); // Mark NaN pixels
                            }
                        }
                    }
                }
            }

            for (int iteration = 0; iteration < 100; iteration++) {
                backstdev = Math.sqrt(back2Mean - backMean * backMean);
                back = 0.0;
                back2 = 0.0;
                backCount = 0;

                if (usePlaneLocal) {
                    plane = new FittedPlane(totalPixels);
                }
                if (markRemovedPixels && !isMultiAperture) {
                    ocanvas.removePixelRois();
                }

                // REMOVE STARS FROM BACKGROUND
                var backMeanPlus2Stdev = backMean + 2.0 * backstdev;
                var backMeanMinus2Stdev = backMean - 2.0 * backstdev;
                for (int i = 0; i < pCnt; i++) {
                    d = pixels[i];
                    if ((d <= backMeanPlus2Stdev) /*&& (d >= backMeanMinus2Stdev)*/) {
                        back += d; // FINAL BACKGROUND
                        back2 += d * d;
                        backCount++;
                        if (usePlaneLocal) {
                            plane.addPoint(is[i], js[i], d);
                        }
                    } else if (markRemovedPixels) {
                        addPixelRoi(imp,is[i], js[i]);
                    }
                }

                if (backCount > 0) {
                    back /= backCount;    // MEAN BACKGROUND
                    backMean = back;
                    back2Mean = back2 / backCount;
                }

                if (Math.abs(prevBackMean - backMean) < 0.0001) { //was 0.1 which did not work for for low background levels
                    break;
                }
                prevBackMean = backMean;
            }

            dBackCount = (double) backCount;
            if (markRemovedPixels) {
                AstroCanvas ac = (AstroCanvas) imp.getCanvas();
                ac.paint(ac.getGraphics());
            }
        }

        if (usePlaneLocal && !plane.fitPlane()) {
            //IJ.log("Photometer ERROR : cannot fit plane to background, using average background instead.");
            fitPlaneError = true;
        }

        btot = back * dSourceCount;

        if (usePlaneLocal && !fitPlaneError) {
            source = 0.0;
            back = 0.0;
            double b = 0.0;
            int srcCount = 0;
            dSourceCount = 0.0;
            if (exact) {
                for (FreeformPixelApertureRoi.Pixel pixel : apertureRoi.iterable()) {
                    d = ip.getPixelValue(pixel.x(), pixel.y());
                    if (!Float.isNaN(d)) {
                        dSourceCount += 1;
                        b = plane.valueAt(pixel.x(), pixel.y());
                        back += b;
                        source += (d - b);
                    }
                }

                if (useBackgroundAnnulus) {
                    for (int j = j1; j <= j2; j++) {
                        var dj = (double) j + Centroid.PIXELCENTER - ypix;        // Center;
                        for (int i = i1; i <= i2; i++) {
                            var di = (double) i + Centroid.PIXELCENTER - xpix;    // Center;
                            d = ip.getPixelValue(i, j);
                            if (!Float.isNaN(d)) {
                                var fraction = intarea(xpix, ypix, radius, i, i + 1, j, j + 1);
                                dSourceCount += fraction;
                                b = plane.valueAt(di, dj);
                                back += b * fraction;
                                source += (d - b) * fraction;
                            }
                        }
                    }
                }

                if (dSourceCount > 0) {
                    back /= dSourceCount;
                }
            } else {
                for (FreeformPixelApertureRoi.Pixel pixel : apertureRoi.iterable()) {
                    if (!pixel.isBackground()) {
                        d = ip.getPixelValue(pixel.x(), pixel.y());
                        if (!Float.isNaN(d)) {
                            srcCount++;
                            b = plane.valueAt(pixel.x(), pixel.y());
                            back += b;
                            source += (d - b);
                        }
                    }
                }

                if (useBackgroundAnnulus) {
                    for (int j = j1; j <= j2; j++) {
                    var dj = (double) j + Centroid.PIXELCENTER - ypix;        // Center;
                    for (int i = i1; i <= i2; i++) {
                        var di = (double) i + Centroid.PIXELCENTER - xpix;    // Center;
                        var r2 = di * di + dj * dj;
                        if (r2 < radius * radius) { // SOURCE APERTURE
                            d = ip.getPixelValue(i, j);
                            if (!Float.isNaN(d)) {
                                srcCount++;
                                b = plane.valueAt(di, dj);
                                back += b;
                                source += (d - b);
                            }
                        }
                    }
                }
                }

                dSourceCount = srcCount;
                if (srcCount > 0) {
                    back /= dSourceCount;
                }
            }
        } else {
            source -= btot;
        }

        if (dSourceCount > 0.0) {
            mean = source / dSourceCount;
        }

        double src = 0.0;
        double bck = 0.0;
        double sCnt = 0.0;
        double srcCnt = 0.0;
        double bckCnt = 0.0;

        // ERROR FROM GAIN (e-/count), RON (e-), DARK CURRENT (e-) AND POISSON STATISTICS
        // SEE MERLINE, W. & HOWELL, S.B., 1995, EXP. ASTRON., 6, 163
        src = (source < 0.0 || dSourceCount <= 0.0) ? 0.0 : source;
        bck = (back < 0.0 || dBackCount <= 0) ? 0.0 : back;
        sCnt = Math.max(dSourceCount, 0.0);
        srcCnt = (dSourceCount <= 0.0 || dBackCount <= 0) ? 0.0 : dSourceCount;
        bckCnt = (dBackCount <= 0) ? 1.0 : dBackCount;

        serror = Math.sqrt((src * gain) + sCnt * (1.0 + srcCnt / bckCnt) * (bck * gain + dark + ron * ron + gain * gain * 0.083521)) / gain;
        fwhm = IJU.radialDistributionFWHM(ip, apertureRoi.xPos, apertureRoi.yPos, apertureRoi.r1, back);

        // CALIBRATE INTENSITIES IF POSSIBLE

        rawSource = source;
        rawBack = 0.0;
        if (calib != null && calib.calibrated()) {
            rawSource = calib.getRawValue(source);
            if (dBackCount > 0) {
                rawBack = calib.getRawValue(back);
            }
        }
    }

    /**
     * Performs aperture photometry on the current image using given center and aperture radii.
     *
     * @param imp ImageProcessor
     * @param x   x-position of aperture center (pixels)
     * @param y   y-position of aperture center (pixels)
     * @param rad radius of source aperture (pixels)
     * @param rb1 inner radius of background annulus (pixels)
     * @param rb2 outer radius of background annulus (pixels)
     */
    public void measure(ImagePlus imp, boolean exactPixels, double x, double y, double rad, double rb1, double rb2) {
        ImageProcessor ip = imp.getProcessor();
        exact = exactPixels;
        xCenter = x;
        yCenter = y;
        radius = rad;
        rBack1 = rb1;
        rBack2 = rb2;
        ocanvas = OverlayCanvas.getOverlayCanvas(imp);
        hasBack = !Double.isNaN(rBack1) && !Double.isNaN(rBack2) && (rBack2 > rBack1);
        boolean usePlaneLocal = usePlane && hasBack;

        debug = Prefs.get("astroj.debug", false);

        if (Double.isNaN(radius)) {
            return;
        }
        if (ip.getBitDepth() == 24) {
            Frame openFrame = imp.getWindow();
            if (openFrame instanceof AstroStackWindow asw) {
                ColorProcessor cp = asw.getcp();
                if (cp != null) {
                    ip = cp;
                }
            }
        }

        double r = rBack2 + 2.0;
        if (!hasBack || (hasBack && (rBack2 < radius))) {
            r = radius + 2.0;
        }
        // r++;
        // GET TOTAL APERTURE BOUNDS

        double xpix = x;    // +Centroid.PIXELCENTER;	// POSITION RELATIVE TO PIXEL CENTER
        double ypix = y;    // +Centroid.PIXELCENTER;

        int i1 = (int) (xpix - r);
        if (i1 < 0) {
            i1 = 0;
        } else if (i1 >= ip.getWidth()) {
            i1 = ip.getWidth() - 1;
        }

        int i2 = (int) (xpix + r) + 1;
        if (i2 < 0) {
            i2 = 0;
        } else if (i2 >= ip.getWidth()) {
            i2 = ip.getWidth() - 1;
        }

        int j1 = (int) (ypix - r);
        if (j1 < 0) {
            j1 = 0;
        } else if (j1 >= ip.getHeight()) {
            j1 = ip.getHeight() - 1;
        }

        int j2 = (int) (ypix + r) + 1;
        if (j2 < 0) {
            j2 = 0;
        } else if (j2 >= ip.getHeight()) {
            j2 = ip.getHeight() - 1;
        }

        double r2b1 = hasBack ? rb1 * rb1 : 0.0;
        double r2b2 = hasBack ? rb2 * rb2 : 0.0;
        //if (!Double.isNaN(rb2) && rb2 > radius) r2b2 = rb2*rb2;

        double r2ap = radius * radius;
        double di, dj;

        // INTEGRATE STAR WITHIN APERTURE OF RADIUS radius, SKY OUTSIDE

        source = 0.0;
        btot = 0.0;
        back = 0.0;
        backi = 0.0;
        backo = 0.0;
        mean = 0.0;
        prevBackMean = 0;
        dSourceCount = 0.0;
        dBackCount = 0.0;
        sourceCount = 0;
        backCount = 0;
        back2 = 0;
        boolean fitPlaneError = false;

        int totalPixels = (i2 - i1 + 1) * (j2 - j1 + 1);
        if (usePlaneLocal) {
            plane = new FittedPlane(totalPixels);
        }

        var region = new RecursivePixelProcessor.CircularRegion(x, y, rad, rb1, rb2);

        peak = Float.NEGATIVE_INFINITY;
        if (exact) {
            if (USE_PARALLEL_PIXEL_PROCESS.get()) {
                var sourceAdder = region.createPixelStorage(ip);
                var dSourceCountAdder = region.createPixelStorage(ip);
                var peakAccumulator = new DoubleAccumulator(Math::max, peak);
                var backAdder = region.createPixelStorage(ip);
                var dBackCountAdder = region.createPixelStorage(ip);
                var backCountAdder = new LongAdder();

                var task = new RecursivePixelProcessor(region, ip, (i, j, d) -> {
                    var fraction = intarea(xpix, ypix, radius, i, i + 1, j, j + 1);
                    sourceAdder.setVal(i, j, fraction * d);
                    dSourceCountAdder.setVal(i, j, fraction);

                    if (fraction > 0.01) {
                        peakAccumulator.accumulate(d);
                    }

                    if (hasBack) {
                        var dy = j + Centroid.PIXELCENTER - ypix;
                        var dx = i + Centroid.PIXELCENTER - xpix;
                        var r2 = dx * dx + dy * dy;
                        if (!removeBackStars && !usePlaneLocal) {
                            fraction = intarea(xpix, ypix, rBack1, i, i + 1, j, j + 1);
                            var b = -(fraction * d);
                            var bc = -fraction;
                            fraction = intarea(xpix, ypix, rBack2, i, i + 1, j, j + 1);
                            backAdder.setVal(i, j, (fraction * d) + b);
                            dBackCountAdder.setVal(i, j, fraction + bc);
                        } else if (r2 >= r2b1 && r2 <= r2b2) { // BACKGROUND
                            backAdder.setVal(i, j, d);
                            backCountAdder.increment();

                            return usePlaneLocal;
                        }
                    }

                    return false;
                });

                var allPlanePoints = POOL.invoke(task);

                if (usePlaneLocal) {
                    for (RecursivePixelProcessor.Point3D p : allPlanePoints) {
                        plane.addPoint(p.x(), p.y(), p.z());
                    }
                }

                source = sourceAdder.sum();
                dSourceCount = dSourceCountAdder.sum();
                peak = (float) peakAccumulator.get();
                back = backAdder.sum();
                back2 = back * back;
                dBackCount = dBackCountAdder.sum();
                backCount = backCountAdder.sum();
            } else {
                for (int j = j1; j <= j2; j++) {
                    dj = (double) j + Centroid.PIXELCENTER - ypix;        // pixel center
                    for (int i = i1; i <= i2; i++) {
                        di = (double) i + Centroid.PIXELCENTER - xpix;    // pixel center
                        var r2 = di * di + dj * dj;                         // radius to pixel center
                        var d = ip.getPixelValue(i, j);
                        if (!Float.isNaN(d)) {
                            var fraction = intarea(xpix, ypix, radius, i, i + 1, j, j + 1);
                            source += fraction * d;
                            dSourceCount += fraction;
                            if (fraction > 0.01 && d > peak) {
                                peak = d;
                            }
                            if (hasBack) {
                                if (!removeBackStars && !usePlaneLocal) {
                                    fraction = intarea(xpix, ypix, rBack1, i, i + 1, j, j + 1);
                                    back -= fraction * d;
                                    dBackCount -= fraction;
                                    fraction = intarea(xpix, ypix, rBack2, i, i + 1, j, j + 1);
                                    back += fraction * d;
                                    dBackCount += fraction;
                                } else if (r2 >= r2b1 && r2 <= r2b2) { // BACKGROUND
                                    back += d;
                                    back2 += d * d;
                                    backCount++;
                                    if (usePlaneLocal) {
                                        plane.addPoint(di, dj, d);
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (removeBackStars || usePlaneLocal) {
                dBackCount = backCount;
            }
        } else {
            if (USE_PARALLEL_PIXEL_PROCESS.get()) {
                var sourceAdder = region.createPixelStorage(ip);
                var peakAccumulator = new DoubleAccumulator(Math::max, peak);
                var backAdder = region.createPixelStorage(ip);
                var backCountAdder = new LongAdder();
                var sourceCountAdder = new LongAdder();

                var task = new RecursivePixelProcessor(region, ip, (i, j, d) -> {
                    var dy = j + Centroid.PIXELCENTER - ypix;
                    var dx = i + Centroid.PIXELCENTER - xpix;
                    var r2 = dx * dx + dy * dy;
                    if (r2 < r2ap) { // SOURCE APERTURE
                        sourceAdder.setVal(i, j, d);
                        sourceCountAdder.increment();
                        peakAccumulator.accumulate(d);
                    }

                    if (hasBack && r2 >= r2b1 && r2 <= r2b2) { // BACKGROUND
                        backAdder.setVal(i, j, d);
                        backCountAdder.increment();

                        return usePlaneLocal;
                    }

                    return false;
                });

                var allPlanePoints = POOL.invoke(task);

                if (usePlaneLocal) {
                    for (RecursivePixelProcessor.Point3D p : allPlanePoints) {
                        plane.addPoint(p.x(), p.y(), p.z());
                    }
                }

                source = sourceAdder.sum();
                peak = (float) peakAccumulator.get();
                back = backAdder.sum();
                back2 = back * back;
                backCount = backCountAdder.sum();
            } else {
                for (int j = j1; j <= j2; j++) {
                    dj = (double) j + Centroid.PIXELCENTER - ypix;        // Center;
                    for (int i = i1; i <= i2; i++) {
                        di = (double) i + Centroid.PIXELCENTER - xpix;    // Center;
                        var r2 = di * di + dj * dj;
                        var d = ip.getPixelValue(i, j);
                        if (!Float.isNaN(d)) {
                            if (r2 < r2ap) { // SOURCE APERTURE
                                source += d;
                                sourceCount++;
                                if (d > peak) {
                                    peak = d;
                                }
                            }
                            if (hasBack && r2 >= r2b1 && r2 <= r2b2) { // BACKGROUND
                                back += d;
                                back2 += d * d;
                                backCount++;
                                if (usePlaneLocal) {
                                    plane.addPoint(di, dj, d);
                                }
                            }
                        }
                    }
                }
            }

            dSourceCount = sourceCount;
            dBackCount = backCount;
        }
        /*if (exact) {
            IJ.log("source="+source+"   exactSourceCount="+dSourceCount);
        } else {
            IJ.log("source="+source+"   sourceCount="+sourceCount);
        }*/
        if (hasBack && (dBackCount > 0.0)) {
            //IJ.log("source="+source+"   sourceCount="+sourceCount+"  sourceMean="+source/(double)sourceCount);
            //IJ.log("back="+back+"   backCount="+backCount+"  backMean="+back/(double)backCount);
            back /= dBackCount;    // MEAN BACKGROUND
        }

        //IJ.log("remove stars="+removeBackStars+"   background="+back+"    backcount="+backCount+"    backstdev="+backstdev);

        if (hasBack && removeBackStars && (dBackCount > 3.0)) {
            backMean = back;
            back2Mean = back2 / dBackCount;

            // Copy pixel data for evaluation
            var pixels = new float[totalPixels];
            var js = new int[totalPixels];
            var is = new int[totalPixels];
            var pCnt = 0;

            if (USE_PARALLEL_PIXEL_PROCESS.get()) {
                var indexer = new AtomicInteger();

                var task = new RecursivePixelProcessor(region, ip, false, (i, j, d) -> {
                    var dy = j + Centroid.PIXELCENTER - ypix;
                    var dx = i + Centroid.PIXELCENTER - xpix;
                    var r2 = dx * dx + dy * dy;
                    if (r2 >= r2b1 && r2 <= r2b2) {
                        if (!Double.isNaN(d)) {
                            var idx = indexer.getAndIncrement();
                            js[idx] = j;
                            is[idx] = i;
                            pixels[idx] = (float) d;
                        } else {
                            return markRemovedPixels; // Mark NaN pixels
                        }
                    }

                    return false;
                });

                var allPlanePoints = POOL.invoke(task);

                if (markRemovedPixels) {
                    for (RecursivePixelProcessor.Point3D p : allPlanePoints) {
                        addPixelRoi(imp, p.x(), p.y());
                    }
                }

                pCnt = indexer.get();
            } else {
                for (int j = j1; j <= j2; j++) {
                    dj = (double) j - ypix + Centroid.PIXELCENTER;        // Center
                    for (int i = i1; i <= i2; i++) {
                        di = (double) i - xpix + Centroid.PIXELCENTER;    // Center
                        var r2 = di * di + dj * dj;
                        if (r2 >= r2b1 && r2 <= r2b2) {
                            var d = ip.getPixelValue(i, j);
                            if (!Float.isNaN(d)) {
                                js[pCnt] = j;
                                is[pCnt] = i;
                                pixels[pCnt++] = d;
                            } else if (markRemovedPixels) {
                                addPixelRoi(imp, i, j); // Mark NaN pixels
                            }
                        }
                    }
                }
            }

            for (int iteration = 0; iteration < 100; iteration++) {
                backstdev = Math.sqrt(back2Mean - backMean * backMean);
                back = 0.0;
                back2 = 0.0;
                backCount = 0;

                if (usePlaneLocal) {
                    plane = new FittedPlane(totalPixels);
                }
                if (markRemovedPixels && !isMultiAperture) {
                    ocanvas.removePixelRois();
                }

                // REMOVE STARS FROM BACKGROUND
                var backMeanPlus2Stdev = backMean + 2.0 * backstdev;
                var backMeanMinus2Stdev = backMean - 2.0 * backstdev;
                for (int i = 0; i < pCnt; i++) {
                    var d = pixels[i];
                    if ((d <= backMeanPlus2Stdev) && (d >= backMeanMinus2Stdev)) {
                        back += d; // FINAL BACKGROUND
                        back2 += d * d;
                        backCount++;
                        if (usePlaneLocal) {
                            plane.addPoint(is[i], js[i], d);
                        }
                    } else if (markRemovedPixels) {
                        addPixelRoi(imp,is[i], js[i]);
                    }
                }

                if (backCount > 0) {
                    back /= backCount;    // MEAN BACKGROUND
                    backMean = back;
                    back2Mean = back2 / backCount;
                }
                //IJ.log("remove stars="+removeBackStars+"   background="+back+"    backcount="+backCount+"    backstdev="+backstdev);
                if (Math.abs(prevBackMean - backMean) < 0.0001) { //was 0.1 which did not work for for low background levels
                    //IJ.log("iteration="+iteration);
                    break;
                }
                prevBackMean = backMean;
            }

            dBackCount = (double) backCount;
            if (markRemovedPixels) {
                AstroCanvas ac = (AstroCanvas) imp.getCanvas();
                ac.paint(ac.getGraphics());
            }
        }

        if (usePlaneLocal && !plane.fitPlane()) {
            //IJ.log("Photometer ERROR : cannot fit plane to background, using average background instead.");
            fitPlaneError = true;
        }

        btot = back * dSourceCount;

        if (usePlaneLocal && !fitPlaneError) {
            source = 0.0;
            back = 0.0;
            dSourceCount = 0.0;
            if (exact) {
                if (USE_PARALLEL_PIXEL_PROCESS.get()) {
                    var sourceAdder = region.createPixelStorage(ip);;
                    var dSourceCountAdder = region.createPixelStorage(ip);;
                    var backAdder = region.createPixelStorage(ip);;

                    var task = new RecursivePixelProcessor(region, ip, (i, j, d) -> {
                        var fraction = intarea(xpix, ypix, radius, i, i + 1, j, j + 1);
                        dSourceCountAdder.setVal(i, j, fraction);

                        var b = plane.valueAt(i + Centroid.PIXELCENTER - xpix, j + Centroid.PIXELCENTER - ypix);
                        sourceAdder.setVal(i, j, (d - b) * fraction);

                        backAdder.setVal(i, j, b * fraction);

                        return false;
                    });

                    POOL.invoke(task);

                    source = sourceAdder.sum();
                    dSourceCount = dSourceCountAdder.sum();
                    back = backAdder.sum();
                } else {
                    for (int j = j1; j <= j2; j++) {
                        dj = (double) j + Centroid.PIXELCENTER - ypix;        // Center;
                        for (int i = i1; i <= i2; i++) {
                            di = (double) i + Centroid.PIXELCENTER - xpix;    // Center;
                            var d = ip.getPixelValue(i, j);
                            if (!Float.isNaN(d)) {
                                var fraction = intarea(xpix, ypix, radius, i, i + 1, j, j + 1);
                                dSourceCount += fraction;
                                var b = plane.valueAt(di, dj);
                                back += b * fraction;
                                source += (d - b) * fraction;
                            }
                        }
                    }
                }

                if (dSourceCount > 0) {
                    back /= dSourceCount;
                }
            } else {
                if (USE_PARALLEL_PIXEL_PROCESS.get()) {
                    var sourceAdder = region.createPixelStorage(ip);
                    var dSourceCountAdder = region.createPixelStorage(ip);
                    var backAdder = region.createPixelStorage(ip);

                    var task = new RecursivePixelProcessor(region, ip, (i, j, d) -> {
                        var r2 = ((double) i + Centroid.PIXELCENTER - xpix) * ((double) i + Centroid.PIXELCENTER - xpix) +
                                ((double) j + Centroid.PIXELCENTER - ypix) * ((double) j + Centroid.PIXELCENTER - ypix);

                        if (r2 < r2ap) { // SOURCE APERTURE
                            dSourceCountAdder.setVal(i, j, 1);

                            var b = plane.valueAt(i + Centroid.PIXELCENTER - xpix, j + Centroid.PIXELCENTER - ypix);
                            sourceAdder.setVal(i, j, (d - b));

                            backAdder.setVal(i, j, b);
                        }

                        return false;
                    });

                    POOL.invoke(task);

                    source = sourceAdder.sum();
                    dSourceCount = dSourceCountAdder.sum();
                    back = backAdder.sum();
                } else {
                    for (int j = j1; j <= j2; j++) {
                        dj = (double) j + Centroid.PIXELCENTER - ypix;        // Center;
                        for (int i = i1; i <= i2; i++) {
                            di = (double) i + Centroid.PIXELCENTER - xpix;    // Center;
                            var r2 = di * di + dj * dj;
                            if (r2 < r2ap) { // SOURCE APERTURE
                                var d = ip.getPixelValue(i, j);
                                if (!Float.isNaN(d)) {
                                    dSourceCount++;
                                    var b = plane.valueAt(di, dj);
                                    back += b;
                                    source += (d - b);
                                }
                            }
                        }
                    }
                }

                if (dSourceCount > 0) {
                    back /= dSourceCount;
                }
            }
        } else {
            source -= btot;
        }

        if (dSourceCount > 0.0) {
            mean = source / dSourceCount;
        }

        double src = 0.0;
        double bck = 0.0;
        double sCnt = 0.0;
        double srcCnt = 0.0;
        double bckCnt = 0.0;

        // ERROR FROM GAIN (e-/count), RON (e-), DARK CURRENT (e-) AND POISSON STATISTICS
        // SEE MERLINE, W. & HOWELL, S.B., 1995, EXP. ASTRON., 6, 163
        src = (source < 0.0 || dSourceCount <= 0.0) ? 0.0 : source;
        bck = (back < 0.0 || dBackCount <= 0) ? 0.0 : back;
        sCnt = (dSourceCount <= 0.0) ? 0.0 : dSourceCount;
        srcCnt = (dSourceCount <= 0.0 || dBackCount <= 0) ? 0.0 : dSourceCount;
        bckCnt = (dBackCount <= 0) ? 1.0 : dBackCount;

        serror = Math.sqrt((src * gain) + sCnt * (1.0 + srcCnt / bckCnt) * (bck * gain + dark + ron * ron + gain * gain * 0.083521)) / gain;
        fwhm = IJU.radialDistributionFWHM(ip, xCenter, yCenter, radius, back);

        // CALIBRATE INTENSITIES IF POSSIBLE

        rawSource = source;
        rawBack = 0.0;
        if (calib != null && calib.calibrated()) {
            rawSource = calib.getRawValue(source);
            if (dBackCount > 0) {
                rawBack = calib.getRawValue(back);
            }
        }
//        if (ip.getBitDepth()==24) ip.swapPixelArrays();
    }

    /**
     * Compute the area of overlap between a circle and a rectangle.
     */
    double intarea(double xc, double yc, double r, double xin0, double xin1, double yin0, double yin1) {
        // xc,yc = Center of the circle
        // r     = Radius of the circle
        // x0,y0 = Corner of the rectangle
        // x1,y1 = Opposite corner of the rectangle
        double x0 = xin0 - xc;  /* Shift the objects so that circle is at the origin. */
        double y0 = yin0 - yc;
        double x1 = xin1 - xc;
        double y1 = yin1 - yc;

        //var o = (oneside(x1, y0, y1, r) + oneside(y1, -x1, -x0, r) + oneside(-x0, -y1, -y0, r) + oneside(-y0, x0, x1, r));

        var r2 = r * r;
        var x02 = x0 * x0;
        var y02 = y0 * y0;
        var x12 = x1 * x1;
        var y12 = y1 * y1;
        var d = Math.min(Math.abs(x1 - x0), Math.abs(y1 - y0));
        if (r2 > (x02 + y02) && r2 > (x12 + y12) &&
                r2 > (x12 + y02) && r2 > (x02 + y12)) {
            /*if (o != 1) {
                System.out.printf("1: %s;%n", o);
            }*/
            return /*2*r <= d ? 0 :*/ 1;
        }

        if (r * FUDGE_CONST < d && r2 < (x02+y02) && r2 < (x12+y12) &&
                r2 < (x12+y02) && r2 < (x02+y12)) {
            /*if (Math.abs(o) *//*>= 10e-13*//* != 0) {
                System.out.printf("0: %s;%n", o);
            }*/
            return 0;
        }

        //return o;
        return (oneside(x1, y0, y1, r) + oneside(y1, -x1, -x0, r) + oneside(-x0, -y1, -y0, r) + oneside(-y0, x0, x1, r));
    }

    /**
     * Compute the area of intersection between a triangle and a circle.
     * The circle is centered at the origin and has a radius of r.  The
     * triangle has verticies at the origin and at (x,y0) and (x,y1).
     * This is a signed area.  The path is traversed from y0 to y1.  If
     * this path takes you clockwise the area will be negative.
     */
    double oneside(double x, double y0, double y1, double r) {
        // x = X coordinate of the two points
        // y0, y1 = Y coordinates of the two points
        // r = radius of the circle
        double yh;
        if (x == 0.0) {
            return (0.0);
        } else if (Math.abs(x) >= r) {
            return (arc(x, y0, y1, r));
        } else {
            yh = Math.sqrt(r * r - x * x);
            if (y0 <= -yh) {
                if (y1 <= -yh) {
                    return (arc(x, y0, y1, r));
                } else if (y1 <= yh) {
                    return (arc(x, y0, -yh, r) + chord(x, -yh, y1));
                } else {
                    return (arc(x, y0, -yh, r) + chord(x, -yh, yh) + arc(x, yh, y1, r));
                }
            } else if (y0 < yh) {
                if (y1 < -yh) {
                    return (chord(x, y0, -yh) + arc(x, -yh, y1, r));
                } else if (y1 <= yh) {
                    return (chord(x, y0, y1));
                } else {
                    return (chord(x, y0, yh) + arc(x, yh, y1, r));
                }
            } else {
                if (y1 < -yh) {
                    return (arc(x, y0, yh, r) + chord(x, yh, -yh) + arc(x, -yh, y1, r));
                } else if (y1 < yh) {
                    return (arc(x, y0, yh, r) + chord(x, yh, y1));
                } else {
                    return (arc(x, y0, y1, r));
                }
            }
        }
    }

    /**
     * compute the area within an arc of a circle.  The arc is defined by
     * the two points (x,y0) and (x,y1) in the following manner:  The circle
     * is of radius r and is positioned at the origin.  The origin and each
     * individual point define a line which intersect the circle at some
     * point.  The angle between these two points on the circle measured
     * from y0 to y1 defines the sides of a wedge of the circle.  The area
     * returned is the area of this wedge.  If the area is traversed clockwise
     * the the area is negative, otherwise it is positive.
     */
    double arc(double x, double y0, double y1, double r) {
        //x  = X coordinate of the two points
        //y0 = Y coordinate of the first point
        //y1 = Y coordinate of the second point
        //r  = radius of the circle

        // Using simplified form of (0.5 * r * r * (Math.atan(y1 / x) - Math.atan(y0 / x))),
        // simplified using arctan angle difference identity
        // https://en.wikipedia.org/wiki/List_of_trigonometric_identities#Tangents_and_cotangents_of_sums:~:text=Arctangent,pm%20%5Carctan%20y%7D
        // Changed 4/10/24 to improve performance by avoiding second call to atan by ~40%
        return 0.5 * r * r * Math.atan((x * (y1 - y0)) / (x * x + y1 * y0));
    }


    /**
     * compute the area of a triangle defined by the origin and two points,
     * (x,y0) and (x,y1).  This is a signed area.  If y1 > y0 then the area
     * will be positive, otherwise it will be negative.
     */
    double chord(double x, double y0, double y1) {
        // x = X coordinate of the two points
        // y0 = Y coordinate of the first point
        // y1 = Y coordinate of the second point
        return (0.5 * x * (y1 - y0));
    }

    private Rectangle clampBounds(ImagePlus imp, Rectangle bounds) {
        Rectangle intersection = bounds.intersection(new Rectangle(0, 0, imp.getWidth(), imp.getHeight()));
        // If there's no intersection, make sure to return an empty rectangle.
        if (intersection.width < 0 || intersection.height < 0) {
            intersection.width = 0;
            intersection.height = 0;
        }
        return intersection;
    }


    /**
     * Calculates the precise area of a {@link Area} object.
     *
     * @param area The Area object to calculate the area for.
     * @return The calculated area.
     */
    //todo care about winding rule on the iterator?
    private static double integrateArea(Area area, boolean flatten) {
        if (area.isEmpty()) {
            return 0;
        }

        var pathIterator = flatten ? new FlatteningPathIterator(area.getPathIterator(null), 0.01, 10) : area.getPathIterator(null);
        var coords = new double[6];

        double startX = 0, startY = 0; // Starting point of a subpath
        double lastX = 0, lastY = 0;  // Last point in the current segment

        double totalArea = 0.0;

        var tol = 1e-6;

        var linSeg = 0;
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
                    totalArea += triangleArea(lastX, lastY, x, y);
                    lastX = x;
                    lastY = y;
                    linSeg++;
                }
                case PathIterator.SEG_QUADTO -> {
                    // Quadratic Bézier curve
                    double ctrlX = coords[0], ctrlY = coords[1];
                    double x = coords[2], y = coords[3];
                    totalArea += AdaptiveSimpson.integrateQuadraticBezierAdaptive(lastX, lastY, ctrlX, ctrlY, x, y, tol);
                    lastX = x;
                    lastY = y;
                }
                case PathIterator.SEG_CUBICTO -> {
                    // Cubic Bézier curve
                    double ctrl1X = coords[0], ctrl1Y = coords[1];
                    double ctrl2X = coords[2], ctrl2Y = coords[3];
                    double x = coords[4], y = coords[5];
                    totalArea += AdaptiveSimpson.integrateCubicBezierAdaptive(lastX, lastY, ctrl1X, ctrl1Y, ctrl2X, ctrl2Y, x, y, tol);
                    lastX = x;
                    lastY = y;
                }
                case PathIterator.SEG_CLOSE -> {
                    // Close the path by adding the area of the closing segment
                    totalArea += triangleArea(lastX, lastY, startX, startY);
                    lastX = startX;
                    lastY = startY;
                    linSeg++;
                }
            }

            pathIterator.next();
        }

        return Math.abs(totalArea);
    }

    /**
     * Computes the signed area of a triangle given its vertices.
     */
    private static double triangleArea(double x1, double y1, double x2, double y2) {
        return 0.5 * (x1 * y2 - y1 * x2);
    }


    /**
     * Performs aperture photometry on the current image given a pre-calculated center and standard radii.
     *
     * @param imp ImageProcessor
     * @param x   x-position of aperture center (pixels)
     * @param y   y-position of aperture center (pixels)
     */
    public void measure(ImagePlus imp, double x, double y) {
        this.measure(imp, exact, x, y, radius, rBack1, rBack2);
    }

    /**
     * Gets the corresponding radius of the measurement aperture.
     */
    public double getApertureRadius(int n) {
        if (n == 0) {
            return radius;
        } else if (n == 1) {
            return rBack1;
        } else {
            return rBack2;
        }
    }

    /**
     * Sets the radio of the maximum radii of the source and background apertures.
     *
     * @param r1 the minimum background aperture radius.
     * @param r2 the maximum background aperture radius.
     */
    public void setBackgroundApertureRadii(double r1, double r2) {
        rBack1 = r1;
        rBack2 = r2;
    }

    /**
     * Sets the CCD gain [e- per ADU] and RON [e-] and dark current [e-/pix] (!! note, not e-/pix/sec)
     */
    public void setCCD(double g, double n, double d) {
        gain = (g <= 0) ? 1 : g;
        ron = (n < 0) ? 0 : n;
        dark = (d < 0) ? 0 : d;
    }

    public void setUsePlane(boolean use) {
        usePlane = use;
    }

    public void setRemoveBackStars(boolean removeStars) {
        removeBackStars = removeStars;
    }

    public void setMarkRemovedPixels(boolean markPixels) {
        markRemovedPixels = markPixels;
    }

    /**
     * Returns the current ratio of the source and background aperture radii.
     */
    public double getBackgroundApertureFactor() {
        return rBack2 / radius;
    }

    public double getSourceApertureRadius() {
        return radius;
    }

    public void setSourceApertureRadius(double r) {
        radius = r;
    }

    public double sourceBrightness() {
        return source;
    }

    public double backgroundBrightness() {
        return back;
    }

    public double sourceError() {
        return serror;
    }

    public double getFWHM() {
        return fwhm;
    }

    public double rawSourceBrightness() {
        return rawSource;
    }

    public double rawBackgroundBrightness() {
        return rawBack;
    }

    public double numberOfSourceAperturePixels() {
        return dSourceCount;
    }

    public double numberOfBackgroundAperturePixels() {
        return dBackCount;
    }

    public float peakBrightness() {
        return peak == Float.NEGATIVE_INFINITY ? 0 : peak;
    }

    public double meanBrightness() {
        return mean;
    }


    protected void addPixelRoi(ImagePlus imp, double x, double y) {
        if (bulkPixelRoi == null) {
            bulkPixelRoi = new BulkPixelRoi(imp);
            ocanvas.add(bulkPixelRoi);
        }

        bulkPixelRoi.addPixel(x, y);
    }

    private static class PhotometerWorkerThreadFactory implements ForkJoinPool.ForkJoinWorkerThreadFactory {
        private final AtomicInteger threadNumber = new AtomicInteger(1);

        @Override
        public ForkJoinWorkerThread newThread(ForkJoinPool pool) {
            var worker = ForkJoinPool.defaultForkJoinWorkerThreadFactory.newThread(pool);
            worker.setName("Photometer-" + threadNumber.getAndIncrement());
            return worker;
        }
    }

}



