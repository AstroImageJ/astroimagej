package astroj.fits;

import java.util.Arrays;

import ij.measure.Minimizer;
import ij.measure.UserFunction;

public class Gaussian2DFitter {
    private double[] params;
    private final double[] xData;
    private final double[] yData;
    private final double[] zData;
    private boolean fitSuccessful;
    private double chiSquared;

    public Gaussian2DFitter(double[] xData, double[] yData, double[] zData) {
        this.xData = xData;
        this.yData = yData;
        this.zData = zData;
        performFit();
    }

    private void performFit() {
        var xs = Arrays.stream(xData).summaryStatistics();
        var ys = Arrays.stream(yData).summaryStatistics();
        var zs = Arrays.stream(zData).summaryStatistics();

        var initialParams = new double[6];
        initialParams[0] = zs.getMax(); // Amplitude
        initialParams[1] = (xs.getMax() - xs.getMin()) / 2 + xs.getMin(); // XCenter
        initialParams[2] = (ys.getMax() - ys.getMin()) / 2 + ys.getMin(); // YCenter
        initialParams[3] = (xs.getMax() - xs.getMin()) / 2; // SigmaX
        initialParams[4] = (ys.getMax() - ys.getMin()) / 2; // SigmaY
        initialParams[5] = zs.getMin(); // Baseline

        /*IJ.log("Amplitude: " + initialParams[0] + " XCenter: " + initialParams[1] +
                " YCenter: " + initialParams[2] + " SigmaX: " + initialParams[3] + " SigmaY: " + initialParams[4] +
                " Baseline: " + initialParams[5]);*/

        var dx = (xs.getMax() - xs.getMin()) * 2;
        var dy = (ys.getMax() - ys.getMin()) * 2;
        var dz = (zs.getMax() - zs.getMin()) * 2;
        var function = new Gaussian2DFunction(xs.getMin() - dx, xs.getMax() + dx, ys.getMin() - dy, ys.getMax() + dy, zs.getMin() - dz, zs.getMax() + dz);
        var minimizer = new Minimizer();
        minimizer.setFunction(function, 6);
        minimizer.setMaxIterations(10000);

        var initialParamVariations = new double[6];
        initialParamVariations[0] = Math.abs(initialParams[1]);
        initialParamVariations[1] = Math.abs(initialParams[1] - xs.getMin());
        initialParamVariations[2] = Math.abs(initialParams[2] - ys.getMin());
        initialParamVariations[3] = Math.abs(initialParams[3]);
        initialParamVariations[4] = Math.abs(initialParams[4]);
        initialParamVariations[5] = Math.abs(initialParams[5]);

        //IJ.log(Arrays.toString(initialParamVariations));

        fitSuccessful = minimizer.minimize(initialParams, initialParamVariations) == Minimizer.SUCCESS;
        //AIJLogger.log("chi2: " + minimizer.getFunctionValue());
        params = minimizer.getParams();
        chiSquared = minimizer.getFunctionValue();
    }

    public double getAmplitude() {
        return fitSuccessful ? params[0] : Double.NaN;
    }

    public double getXCenter() {
        return fitSuccessful ? params[1] : Double.NaN;
    }

    public double getYCenter() {
        return fitSuccessful ? params[2] : Double.NaN;
    }

    public double getSigmaX() {
        return fitSuccessful ? params[3] : Double.NaN;
    }

    public double getSigmaY() {
        return fitSuccessful ? params[4] : Double.NaN;
    }

    public double getBaseline() {
        return fitSuccessful ? params[5] : Double.NaN;
    }

    public boolean fitSuccessful() {
        return fitSuccessful;
    }

    public double fittedValue(double x, double y) {
        if (fitSuccessful) {
            return gaussian(x, y, params);
        }

        return Double.NaN;
    }

    private static double gaussian(double x, double y, double[] params) {
        var dx = x - params[1];
        var dy = y - params[2];
        var exponent = -(dx * dx) / (2 * params[3] * params[3]) - (dy * dy) / (2 * params[4] * params[4]);
        return params[0] * Math.exp(exponent) + params[5];
    }

    public double getChiSquared() {
        return chiSquared;
    }

    private class Gaussian2DFunction implements UserFunction {
        private final double minX;
        private final double maxX;
        private final double minY;
        private final double maxY;
        private final double minZ;
        private final double maxZ;

        private Gaussian2DFunction(double minX, double maxX, double minY, double maxY, double minZ, double maxZ) {
            this.minX = minX;
            this.maxX = maxX;
            this.minY = minY;
            this.maxY = maxY;
            this.minZ = minZ;
            this.maxZ = maxZ;
        }

        @Override
        public double userFunction(double[] params, double x) {
            if (params[0] <= 0 || params[3] <= 0 || params[4] <= 0) {
                return Double.NaN;
            }

            /*if (params[1] <= minX || params[2] <= minY || params[1] >= maxX || params[2] >= maxY) {
                return Double.NaN;
            }*/

            /*if (params[0] >= maxZ) {
                return Double.NaN;
            }*/

            double sumSqResiduals = 0;
            for (int i = 0; i < xData.length; i++) {
                var model = gaussian(xData[i], yData[i], params);
                var residual = zData[i] - model;
                sumSqResiduals += residual * residual;
            }
            return sumSqResiduals / 6;
        }
    }
}
