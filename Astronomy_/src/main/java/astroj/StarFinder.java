package astroj;

import ij.ImagePlus;
import ij.astro.logging.AIJLogger;
import ij.process.ImageProcessor;

import java.awt.*;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;

public class StarFinder {
    private static final int[] EDGE_DETECTION_KERNEL_1 = new int[]{1, 0, -1, 0, 0, 0, -1, 0, 1};
    private static final int[] EDGE_DETECTION_KERNEL_2 = new int[]{0, -1, 0, -1, 4, -1, 0, -1, 0};
    private static final int[] EDGE_DETECTION_KERNEL_3 = new int[]{-1, -1, -1, -1, 8, -1, -1, -1, -1};
    private static final int[] AVERAGE = new int[]{1, 1, 1, 1, 0, 1, 1, 1, 1}; //todo rename
    final static int[] DIR_X_OFFSET = new int[]{0, 1, 1, 1, 0, -1, -1, -1};
    final static int[] DIR_Y_OFFSET = new int[]{-1, -1, 0, 1, 1, 1, 0, -1};
    private static final boolean DEBUG_DISPLAY = false;

    // todo improve based on DAOPHOT or IRAF starfinding algorithms, possibly can be easily improved using finding edges of the image
    // Works on whole image, not a ROI
    public static Set<Star> findStars(ImagePlus imp) {
        var kernals = new int[][]{EDGE_DETECTION_KERNEL_1, EDGE_DETECTION_KERNEL_2, EDGE_DETECTION_KERNEL_3};
        var stars = new TreeSet<Star>();

        //ip.sharpen();
        //ip.convolve3x3(kernals[i]);

        AIJLogger.multiLog(imp.getStatistics().stdDev * 0.1);

        var maximaProcess = findLocalMaxima(imp, imp.getStatistics().stdDev * 0.1, Double.MAX_VALUE, 0, false);//ImageProcessor.NO_THRESHOLD
        var ip = maximaProcess.ip;
        var maxima = maximaProcess.coordinateMaximas.descendingSet();

        AIJLogger.multiLog(maxima);

        var centroid = new Centroid();

        //todo some filtering is needed as many maxima overwhelm this
        //  radius check - quadtree solution? How to best deal with this, or grid to store maxima locations and only search around them
        for (CoordinateMaxima coordinateMaxima : maxima) {
            //if (true) break;

            var ap = new ApertureRoi(coordinateMaxima.x, coordinateMaxima.y, 4, 5, 6, Double.NaN, false);
            ap.setImage(imp);
            OverlayCanvas.getOverlayCanvas(imp).add(ap);

            var photom = new Photometer (imp.getCalibration());
            //photom.setCCD (ccdGain, ccdNoise, darkPerPix);
            photom.setRemoveBackStars(true);
            //photom.setMarkRemovedPixels(showRemovedPixels);
            //photom.setUsePlane(backIsPlane);

            photom.measure (imp,true, coordinateMaxima.x, coordinateMaxima.y, 10, 13, 14);

            var back = photom.backgroundBrightness();
            var source = photom.sourceBrightness();
            var serror = photom.sourceError();
            var mean = photom.meanBrightness();
            var fwhm = photom.getFWHM();

            var processed = centroid.measure(imp, coordinateMaxima.x, coordinateMaxima.y, 10, 13, 16, true, true, true);
            if (processed) {
                var roi = new AnnotateRoi(true, true, false, false, centroid.xCenter, centroid.yCenter, 1, "test", Color.YELLOW);
                roi.setImage(imp);
                OverlayCanvas.getOverlayCanvas(imp).add(roi);
            }
        }

        return stars;
    }

    /**
     * @param imp the image to run on
     * @param thresholdLower the threshold below which pixel values are skipped
     * @param thresholdUpper the threshold above which pixel values are skipped
     * @param border the number of pixels from the edge to ignore
     * @return
     */
    // Does not work on a ROI, but the whole image
    //todo make astrometry use this
    public static ProcessingMaxima findLocalMaxima(ImagePlus imp, double thresholdLower, double thresholdUpper, int border, boolean convolve) {
        var ip = imp.getProcessor().duplicate();
        var coordinates = new HashSet<CoordinateMaxima>();
        if (convolve) ip.convolve3x3(AVERAGE);

        final var doLowerThresholdCheck = thresholdLower != ImageProcessor.NO_THRESHOLD;
        final var doUpperThresholdCheck = thresholdUpper != Double.MAX_VALUE;

        ip.resetRoi();

        for (int x = border; x < ip.getWidth() - border; x++) {
            for (int y = border; y < ip.getHeight() - border; y++) {
                var v = ip.getValue(x, y);
                if (Double.isNaN(v)) continue;
                if (doLowerThresholdCheck && v < thresholdLower) continue;
                if (doUpperThresholdCheck && v > thresholdUpper) continue;
                if (v == ip.getMax()) coordinates.add(new CoordinateMaxima(v, x, y));
                boolean isInner = (y != 0 && y != ip.getHeight() - 1) && (x != 0 && x != ip.getWidth() - 1);

                var isMax = true;
                for (int d = 0; d < 8; d++) { // Compare with the 8 neighbor pixels
                    if (isInner || isWithin(ip, x, y, d)) {//todo can't some positions be skipped?
                        float vNeighbor = ip.getPixelValue(x + DIR_X_OFFSET[d], y + DIR_Y_OFFSET[d]);
                        if (vNeighbor > v) {
                            isMax = false;
                            break;
                        }
                    }
                }
                if (isMax) {
                    coordinates.add(new CoordinateMaxima(v, x, y));

                    if (DEBUG_DISPLAY) {
                        var roi = new AnnotateRoi(true, false, true, false, x, y, 0.6, "test", Color.BLUE);
                        roi.setImage(imp);
                        OverlayCanvas.getOverlayCanvas(imp).add(roi);
                        AIJLogger.multiLog("Found maxima: ", x, y);
                    }
                }
            }
        }

        return new ProcessingMaxima(new TreeSet<>(coordinates), ip); // Returns set sorted on local maxima value
    }

    //ip.drawEdges, sharpen, smooth seem useful for removing noise and enhancing stars

    /**
     * returns whether the neighbor in a given direction is within the image
     * NOTE: it is assumed that the pixel x,y itself is within the image!
     * Uses class variables width, height: dimensions of the image
     *
     * @param x         x-coordinate of the pixel that has a neighbor in the given direction
     * @param y         y-coordinate of the pixel that has a neighbor in the given direction
     * @param direction the direction from the pixel towards the neighbor (see makeDirectionOffsets)
     * @return true if the neighbor is within the image (provided that x, y is within)
     */
    private static boolean isWithin(ImageProcessor ip, int x, int y, int direction) {
        int xmax = ip.getWidth() - 1;
        int ymax = ip.getHeight() - 1;
        return switch (direction) {
            case 0 -> (y > 0);
            case 1 -> (x < xmax && y > 0);
            case 2 -> (x < xmax);
            case 3 -> (x < xmax && y < ymax);
            case 4 -> (y < ymax);
            case 5 -> (x > 0 && y < ymax);
            case 6 -> (x > 0);
            case 7 -> (x > 0 && y > 0);
            default -> false;
        };
    }

    public record CoordinateMaxima(double value, double x, double y) implements Comparable<CoordinateMaxima> {
        public CoordinateMaxima(CoordinateMaxima c, double newVal) {
            this(newVal, c.x(), c.y());
        }

        public double distanceTo(CoordinateMaxima crd2) {
            return Math.sqrt(squaredDistanceTo(crd2));
        }

        public double distanceTo(double x1, double y1) {
            return Math.sqrt(squaredDistanceTo(x1, y1));
        }

        public double squaredDistanceTo(CoordinateMaxima crd2) {
            return squaredDistanceTo(crd2.x, crd2.y);
        }

        public double squaredDistanceTo(double x1, double y1) {
            final var h = x - x1;
            final var v = y - y1;
            return h*h + v*v;
        }

        @Override
        public int compareTo(CoordinateMaxima o) {
            return Double.compare(value, o.value);
        }
    }

    public record ProcessingMaxima(TreeSet<CoordinateMaxima> coordinateMaximas, ImageProcessor ip) {}

    /**
     * A representation of a star in an image.
     *
     * @param avg average counts
     * @param peak the peak counts
     * @param xAxis the FWHM of the star in the x-axis
     * @param yAxis the FWHM of the star in the y-axis
     * @param xCenter the center location of the star along the x-axis
     * @param yCenter the center location of the star along the y-axis
     */
    public record Star(int peak, double avg, int xCenter, int yCenter, int xAxis, int yAxis) implements Comparable<Star> {
        @Override
        public int compareTo(Star s2) {
            return Double.compare(avg, s2.avg);
        }
    }
}
