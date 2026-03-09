package astroj.fits;

import java.util.Arrays;

import ij.measure.Minimizer;
import ij.measure.UserFunction;

public class Moffat2DFitter {
    private double[] params;
    private final double[] xData;
    private final double[] yData;
    private final double[] zData;
    private final int maxIter;
    private final double relErr;
    private final double absErr;
    private boolean fitSuccessful;
    private double chiSquared;

    public Moffat2DFitter(double[] xData, double[] yData, double[] zData, int maxIter, double relErr, double absErr) {
        this.xData = xData;
        this.yData = yData;
        this.zData = zData;
        this.maxIter = maxIter;
        this.relErr = relErr;
        this.absErr = absErr;
        performFit();
    }

    private void performFit() {
        var xSumStat = Arrays.stream(xData).summaryStatistics();
        var ySumStat = Arrays.stream(yData).summaryStatistics();
        var zSumStat = Arrays.stream(zData).summaryStatistics();

        var initialParams = new double[6];
        initialParams[0] = zSumStat.getMax();
        initialParams[1] = (xSumStat.getMax() - xSumStat.getMin()) / 2 + xSumStat.getMin();
        initialParams[2] = (ySumStat.getMax() - ySumStat.getMin()) / 2 + ySumStat.getMin();
        initialParams[3] = (xSumStat.getMax() - xSumStat.getMin()) / 2;
        initialParams[4] = (ySumStat.getMax() - ySumStat.getMin()) / 2;
        initialParams[5] = zSumStat.getMin();

        var function = new Moffat2DFunction();
        var minimizer = new Minimizer();
        minimizer.setFunction(function, 6);
        minimizer.setMaxIterations(maxIter);

        var initialParamVariations = new double[6];
        initialParamVariations[0] = Math.abs(initialParams[1]);
        initialParamVariations[1] = Math.abs(initialParams[1] - xSumStat.getMin());
        initialParamVariations[2] = Math.abs(initialParams[2] - ySumStat.getMin());
        initialParamVariations[3] = Math.abs(initialParams[3]);
        initialParamVariations[4] = Math.abs(initialParams[4]);
        initialParamVariations[5] = Math.abs(initialParams[5]);

        minimizer.setMaxError(relErr, absErr);

        fitSuccessful = minimizer.minimize(initialParams, initialParamVariations) == Minimizer.SUCCESS;
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
            return moffat(x, y, params);
        }

        return Double.NaN;
    }

    private static double moffat(double x, double y, double[] params) {
        var amp = params[0];
        var dx = x - params[1];
        var dy = y - params[2];
        var alpha = params[3];
        var gamma = params[4];
        var baseline = params[5];
        return params[0] * (amp * Math.pow(1 + ((dx*dx + dy*dy) / (gamma * gamma)), -alpha)) + baseline;
    }

    public double getChiSquared() {
        return chiSquared;
    }

    private class Moffat2DFunction implements UserFunction {
        private final double dof;

        private Moffat2DFunction() {
            this.dof = xData.length - 6;
        }

        @Override
        public double userFunction(double[] params, double x) {
            if (params[0] <= 0 || params[3] <= 0 || params[4] <= 0) {
                return Double.NaN;
            }

            double sumSqResiduals = 0;
            for (int i = 0; i < xData.length; i++) {
                var model = moffat(xData[i], yData[i], params);
                var residual = zData[i] - model;
                sumSqResiduals += residual * residual;
            }

            return sumSqResiduals / dof;
        }
    }
}
