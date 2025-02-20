package Astronomy.photometer;

import astroj.Centroid;
import ij.process.ImageProcessor;

import java.awt.*;
import java.awt.geom.Area;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.RecursiveTask;

public class RecursivePixelProcessor extends RecursiveTask<List<RecursivePixelProcessor.Point3D>> {
    private final int startX;
    private final int startY;
    private final int width;
    private final int height;
    private final Rectangle bounds;
    private final Region region;
    private final ImageProcessor ip;
    private final Accumulator accumulator;

    private static final int THRESHOLD = 100;

    public RecursivePixelProcessor(Region region, ImageProcessor ip, Accumulator accumulator) {
        this.bounds = clampBounds(ip, region.getBounds());
        this.startX = bounds.x;
        this.startY = bounds.y;
        this.width = bounds.width;
        this.height = bounds.height;
        this.region = region;
        this.ip = ip;
        this.accumulator = accumulator;
    }

    private RecursivePixelProcessor(int startX, int startY, int width, int height,
                                   Region region, ImageProcessor ip, Accumulator accumulator) {
        this.startX = startX;
        this.startY = startY;
        this.width = width;
        this.height = height;
        this.bounds = clampBounds(ip, region.getBounds());
        this.region = region;
        this.ip = ip;
        this.accumulator = accumulator;
    }

    @Override
    protected List<Point3D> compute() {
        List<Point3D> localPlanePoints = new ArrayList<>();

        // If the region is small enough, process it sequentially.
        if (width * height <= THRESHOLD) {
            for (int i = startX; i < startX + width; i++) {
                for (int j = startY; j < startY + height; j++) {
                    var d = ip.getPixelValue(i, j);
                    if (!Float.isNaN(d)) {
                        if (accumulator.accumulate(i, j, d)) {
                            if (region instanceof CircularRegion circularRegion) {
                                localPlanePoints.add(new Point3D(i + Centroid.PIXELCENTER - circularRegion.centerX,
                                        j + Centroid.PIXELCENTER - circularRegion.centerY, d));
                            } else {
                                localPlanePoints.add(new Point3D(i, j, d));
                            }
                        }
                    }
                }
            }

            return localPlanePoints;
        } else {
            // Split the region adaptively along its longer dimension.
            List<Point3D> result = new ArrayList<>();
            if (width >= height) {
                int midWidth = width / 2;
                RecursivePixelProcessor leftTask = new RecursivePixelProcessor(
                        startX, startY, midWidth, height,
                        region, ip, accumulator);
                RecursivePixelProcessor rightTask = new RecursivePixelProcessor(
                        startX + midWidth, startY, width - midWidth, height,
                        region, ip, accumulator);
                leftTask.fork();
                result.addAll(rightTask.compute());
                result.addAll(leftTask.join());
            } else {
                int midHeight = height / 2;
                RecursivePixelProcessor topTask = new RecursivePixelProcessor(
                        startX, startY, width, midHeight,
                        region, ip, accumulator);
                RecursivePixelProcessor bottomTask = new RecursivePixelProcessor(
                        startX, startY + midHeight, width, height - midHeight,
                        region, ip, accumulator);
                topTask.fork();
                result.addAll(bottomTask.compute());
                result.addAll(topTask.join());
            }
            return result;
        }
    }

    private static Rectangle clampBounds(ImageProcessor ip, Rectangle bounds) {
        Rectangle intersection = bounds.intersection(new Rectangle(0, 0, ip.getWidth(), ip.getHeight()));
        // If there's no intersection, make sure to return an empty rectangle.
        if (intersection.width < 0 || intersection.height < 0) {
            intersection.width = 0;
            intersection.height = 0;
        }
        return intersection;
    }

    public record Point3D(double x, double y, double z) {}

    /**
     * Must be thread safe
     */
    @FunctionalInterface
    public interface Accumulator {
        /**
         * Must be thread safe
         *
         * @param pixelValue the pixel value
         * @param i the x-coordinate of the pixel
         * @param j the y-coordinate of the pixel
         * @return if this pixel should be added to the list of returned pixels
         */
        boolean accumulate(int i, int j, double pixelValue);
    }

    public record AreaRegion(Area area) implements Region {
        @Override
        public Rectangle getBounds() {
            return area.getBounds();
        }
    }

    public record CircularRegion(double centerX, double centerY, double r1, double r2, double r3) implements Region {
        @Override
        public Rectangle getBounds() {
            if (!Double.isNaN(r2) && !Double.isNaN(r3) && (r3 > r1)) {
                return getBoundingRectangle(centerX, centerY, r3);
            } else {
                return getBoundingRectangle(centerX, centerY, r1);
            }
        }

        private static Rectangle getBoundingRectangle(double x, double y, double r) {
            var left = Math.floor(x - r);
            var right = Math.ceil(x + r);
            var bottom = Math.floor(y - r);
            var top = Math.ceil(y + r);

            return new Rectangle((int) left, (int) bottom, (int) (right - left), (int) (top - bottom));
        }
    }

    public interface Region {
        /**
         * @return the pixel bounds that contain this region
         */
        Rectangle getBounds();

        default PixelStorage createPixelStorage(ImageProcessor ip) {
            return new PixelStorage(clampBounds(ip, getBounds()));
        }
    }

    public record PixelStorage(double[] pixelArray, Rectangle bounds) {
        public PixelStorage(Rectangle bounds) {
            this(new double[bounds.height * bounds.width], bounds);
        }

        public void setVal(int x, int y, double v) {
            assert pixelArray[(y - bounds.y) * bounds.width + (x - bounds.x)] == 0 : "Attempting to write to already used index";
            pixelArray[(y - bounds.y) * bounds.width + (x - bounds.x)] = v;
        }

        public double sum() {
            return Arrays.stream(pixelArray).sequential().sum();
        }
    }
}
