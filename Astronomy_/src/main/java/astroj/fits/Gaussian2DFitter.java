package astroj.fits;

import astroj.fits.PixelPatcherImpl.Pixel;
import astroj.fits.PixelPatcherImpl.Region;
import ij.process.ImageProcessor;
import org.hipparchus.linear.Array2DRowRealMatrix;
import org.hipparchus.linear.ArrayRealVector;
import org.hipparchus.linear.QRDecomposition;

import java.awt.*;
import java.util.Arrays;
import java.util.BitSet;

public final class Gaussian2DFitter {
    private final double a;
    private final double b;
    private final double c;
    private final double d;
    private final double e;
    private final double f;

    private final double background;

    private final double centerX;
    private final double centerY;
    private final double scale;

    private Gaussian2DFitter(double[] coefficients, double background, double centerX, double centerY, double scale) {
        a = coefficients[0];
        b = coefficients[1];
        c = coefficients[2];
        d = coefficients[3];
        e = coefficients[4];
        f = coefficients[5];
        this.background = background;
        this.centerX = centerX;
        this.centerY = centerY;
        this.scale = scale;
    }

    static Gaussian2DFitter fit(ImageProcessor ip, Region region) {
        var background = estimateBackground(ip, region);
        return fit(ip, region, background);
    }

    static Gaussian2DFitter fit(ImageProcessor ip, Region region, double background) {
        var bounds = region.bounds();

        if (bounds.width < 2 || bounds.height < 2) {
            throw new IllegalArgumentException("Gaussian region is too small");
        }

        var centerX = bounds.getCenterX();
        var centerY = bounds.getCenterY();

        var scale = 0.5 * Math.max(bounds.width, bounds.height);

        var badPixels = createBadPixelMask(region);

        var sampleCount = countSamples(ip, bounds, badPixels, background);

        if (sampleCount < 6) {
            throw new IllegalArgumentException("Not enough good pixels for a 2D Gaussian fit");
        }

        var design = new double[sampleCount][6];
        var observations = new double[sampleCount];

        var row = 0;
        for (int y = bounds.y; y < bounds.y + bounds.height; y++) {
            var v = (y - centerY) / scale;
            for (int x = bounds.x; x < bounds.x + bounds.width; x++) {
                var localX = x - bounds.x;
                var localY = y - bounds.y;

                if (badPixels.get(localY * bounds.width + localX)) {
                    continue;
                }

                var value = ip.getf(x, y);
                var signal = value - background;

                if (!(signal > 0) || !Double.isFinite(signal)) {
                    continue;
                }

                var u = (x - centerX) / scale;

                design[row][0] = u * u;
                design[row][1] = u * v;
                design[row][2] = v * v;
                design[row][3] = u;
                design[row][4] = v;
                design[row][5] = 1.0;

                observations[row] = Math.log(signal);

                row++;
            }
        }

        var matrix = new Array2DRowRealMatrix(design, false);
        var vector = new ArrayRealVector(observations, false);

        var solver = new QRDecomposition(matrix).getSolver();

        if (!solver.isNonSingular()) {
            throw new IllegalArgumentException("Gaussian fit is singular or poorly constrained");
        }

        var coefficients = solver.solve(vector).toArray();

        validateGaussian(coefficients);

        return new Gaussian2DFitter(coefficients, background, centerX, centerY, scale);
    }

    private static BitSet createBadPixelMask(Region region) {
        var bounds = region.bounds();
        var badPixels = new BitSet(bounds.width * bounds.height);

        for (Pixel pixel : region.pixels()) {
            var x = pixel.x() - bounds.x;
            var y = pixel.y() - bounds.y;

            if (x >= 0 && x < bounds.width && y >= 0 && y < bounds.height) {
                badPixels.set(y * bounds.width + x);
            }
        }

        return badPixels;
    }

    private static int countSamples(ImageProcessor ip, Rectangle bounds, BitSet badPixels, double background) {
        var count = 0;
        for (int y = bounds.y; y < bounds.y + bounds.height; y++) {
            var localY = y - bounds.y;
            for (int x = bounds.x; x < bounds.x + bounds.width; x++) {
                var localX = x - bounds.x;
                if (badPixels.get(localY * bounds.width + localX)) {
                    continue;
                }

                var signal = ip.getf(x, y) - background;
                if (signal > 0 && Double.isFinite(signal)) {
                    count++;
                }
            }
        }

        return count;
    }

    private static void validateGaussian(double[] coefficients) {
        var a = coefficients[0];
        var b = coefficients[1];
        var c = coefficients[2];

        var determinant = 4.0 * a * c - b * b;

        if (!(a < 0 && c < 0 && determinant > 0)) {
            throw new IllegalStateException("Fitted surface is not a valid 2D Gaussian");
        }
    }

    private static double estimateBackground(ImageProcessor ip, Region region) {
        var borderPixels = region.borderPixels();//todo estimate from entire region, not just border?

        if (borderPixels.isEmpty()) {
            throw new IllegalStateException("Cannot estimate Gaussian background: " + "region has no good border pixels");
        }

        var values = new double[borderPixels.size()];

        var count = 0;
        for (Pixel pixel : borderPixels) {
            var value = ip.getf(pixel.x(), pixel.y());

            if (Double.isFinite(value)) {
                values[count++] = value;
            }
        }

        if (count == 0) {
            throw new IllegalStateException("Cannot estimate Gaussian background");
        }

        Arrays.sort(values, 0, count);

        if ((count % 2) == 0) {
            return 0.5 * (values[count / 2 - 1] + values[count / 2]);
        }

        return values[count / 2];
    }

    public float valueAt(int x, int y) {
        var u = (x - centerX) / scale;
        var v = (y - centerY) / scale;
        var logSignal = a * u * u + b * u * v + c * v * v + d * u + e * v + f;
        return (float) (background + Math.exp(logSignal));
    }

    void apply(ImageProcessor ip, Region region) {
        for (Pixel pixel : region.pixels()) {
            ip.setf(pixel.x(), pixel.y(), valueAt(pixel.x(), pixel.y()));
            ip.markBadPixel(pixel.x(), pixel.y());
        }
        for (Pixel borderPixel : region.borderPixels()) {
            ip.markBadPixelSource(borderPixel.x(), borderPixel.y());
        }
    }

    public double background() {
        return background;
    }
}