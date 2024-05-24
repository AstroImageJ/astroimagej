package Astronomy;

import Astronomy.multiplot.KeplerSplineControl;
import Astronomy.multiplot.settings.MPOperator;
import astroj.IJU;
import flanagan.analysis.Regression;
import flanagan.math.Minimization;
import flanagan.math.MinimizationFunction;
import ij.IJ;
import ij.Prefs;
import ij.astro.logging.AIJLogger;
import ij.measure.ResultsTable;
import ij.util.ArrayUtil;
import org.hipparchus.linear.MatrixUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Locale;
import java.util.function.Function;
import java.util.regex.Pattern;

import static Astronomy.MultiPlot_.*;

/**
 * Extracted version of {@link MultiPlot_#updatePlot(boolean[], boolean)} for the multithreaded optimizer.
 */
public class CurveFitter {
    private static CurveFitter INSTANCE;
    private final int curve;
    private final int targetStar;
    double[][] source;
    double[][] srcvar;
    //private final Minimization minimization = new Minimization();
    ThreadLocal<Minimization> minimizationThreadLocal = ThreadLocal.withInitial(Minimization::new);
    private int initAvgCount;
    private boolean initAtLeastOne;
    private int initDetrendCount;
    private int firstCurve;
    private int initBaselineCount;
    private int initDepthCount;
    private int[] initDetrendVarsUsed;
    private double[] initDetrendY;
    private boolean initDetrendYNotConstant;
    private double[] initDetrendYE;
    private double[][] initDetrendYD;
    private double[] initDetrendX;
    private boolean doInstancedDetrendCalculation = false;
    public static int[] detrendIndex;
    public static String[] detrendlabel;

    // Chunk 1 stuff here (shared between all threads)
    private CurveFitter(int curve, int targetStar) {
        this.curve = curve;
        this.targetStar = targetStar;
        detrendIndex = MultiPlot_.detrendIndex[curve];
        detrendlabel = MultiPlot_.detrendlabel[curve];

        getStarData();
        setupData();
        // todo add check for fitter running only on Tstar and other conditions
        //      extract star and filter from this "rel_flux_Txx"
    }

    public synchronized static CurveFitter getInstance(int curve, int targetStar) {
        if (INSTANCE == null || INSTANCE.curve != curve || INSTANCE.targetStar != targetStar)
            INSTANCE = new CurveFitter(curve, targetStar);
        return INSTANCE;
    }

    public synchronized static void invalidateInstance() {
        INSTANCE = null;
    }

    public OptimizerResults fitCurveAndGetResults() {
        return fitCurveAndGetResults(isRefStar, MultiPlot_.detrendIndex[curve]);
    }

    public OptimizerResults fitCurveAndGetResults(boolean[] isRefStar) {
        doInstancedDetrendCalculation = true;
        return fitCurveAndGetResults(isRefStar, MultiPlot_.detrendIndex[curve]);
    }

    public OptimizerResults fitCurveAndGetResults(int[] detrendIndex) {
        return fitCurveAndGetResults(isRefStar, detrendIndex);
    }

    public OptimizerResults fitCurveAndGetResults(boolean[] isRefStar, int[] detrendIndex) {
        return updateCurve(isRefStar, detrendIndex);
    }

    private synchronized void getStarData() {
        if (table == null) return;
        int numRows = table.getCounter();
        if (numRows < 1) return;
        boolean goodErrData = true;
        source = new double[numAps][numRows];
        srcvar = new double[numAps][numRows];
        for (int ap = 0; ap < numAps; ap++) {
            for (int row = 0; row < numRows; row++) {
                source[ap][row] = 0.0;
                srcvar[ap][row] = 0.0;
            }
        }
        double value;
        double errval;
        double ratio;
        double factor;
        double oneOverFactor;
        int col = ResultsTable.COLUMN_NOT_FOUND;
        int errcol = ResultsTable.COLUMN_NOT_FOUND;
        int snrcol = ResultsTable.COLUMN_NOT_FOUND;
        int peakcol = ResultsTable.COLUMN_NOT_FOUND;

        for (int ap = 0; ap < numAps; ap++) {//todo skip unused target stars, shave down aray size to just be comp stars + target
            col = table.getColumnIndex("Source-Sky_" + (isRefStar[ap] ? "C" : "T") + (ap + 1));
            if (col == ResultsTable.COLUMN_NOT_FOUND) {
                IJ.beep();
                AIJLogger.log("Error: could not find data column 'Source-Sky_" + (isRefStar[ap] ? "C" : "T") + (ap + 1) + "'");
                return;
            } else {
                errcol = table.getColumnIndex("Source_Error_" + (isRefStar[ap] ? "C" : "T") + (ap + 1));
                if (errcol == ResultsTable.COLUMN_NOT_FOUND) {
                    goodErrData = false;
                }
                for (int row = 0; row < numRows; row++) {
                    value = table.getValueAsDouble(col, row);
                    source[ap][row] = value;
                    if (goodErrData) {
                        errval = table.getValueAsDouble(errcol, row);
                        srcvar[ap][row] = errval * errval;
                    }
                }
            }
        }
    }

    //todo remove unneeded bits
    public void setupData() {
        n = table.getCounter();
        if (n < 1) {
            updatePlotRunning = false;
            if (table != null) table.setLock(false);
            return;
        }
        if (n > maxColumnLength) {
            maxColumnLength = Math.max(n, 2 * maxColumnLength);
            setupDataBuffers();
        }
        unfilteredColumns = table.getColumnHeadings().split("\t");
        vMarker1Value = Prefs.get("plot.vMarker1Value", vMarker1Value);
        vMarker2Value = Prefs.get("plot.vMarker2Value", vMarker2Value);
        if (!Arrays.equals(unfilteredColumns, oldUnfilteredColumns)) {
            updatePlotRunning = false;
            if (table != null) table.setLock(false);
            updateColumnLists();
        }

        for (int curve = 0; curve < maxCurves; curve++) {
            if (inputAverageOverSize[curve] < 1) {
                inputAverageOverSize[curve] = 1;
            }
        }

        holdExcludedHeadSamples = excludedHeadSamples;
        holdExcludedTailSamples = excludedTailSamples;
        if (excludedHeadSamples + excludedTailSamples >= n)  //Handle case for more samples excluded than in dataset
        {
            excludedHeadSamples = excludedHeadSamples < n ? excludedHeadSamples : n - 1;
            excludedTailSamples = n - excludedHeadSamples - 1;
        }
        excluded = excludedHeadSamples + excludedTailSamples;

        netT0 = (twoxPeriod && oddNotEven) ? T0 - period : T0;
        netPeriod = twoxPeriod ? 2 * period : period;

        int magSign = negateMag ? 1 : -1;

        marker[curve] = markerOf(markerIndex[curve]);
        residualSymbol[curve] = markerOf(residualSymbolIndex[curve]);
        color[curve] = colorOf(colorIndex[curve]);
        modelColor[curve] = colorOf(modelColorIndex[curve]);
        residualModelColor[curve] = colorOf(residualModelColorIndex[curve]);
        residualColor[curve] = colorOf(residualColorIndex[curve]);
        sigma[curve] = 0.0;
        baseline[curve] = 0;

        // GET DATA

        if (n > x[0].length) {
            nn = new int[maxCurves];
            xlabel2 = new String[maxCurves];
            x = new double[maxCurves][n];
            y = new double[maxCurves][n];
            yerr = new double[maxCurves][n];
            yop = new double[maxCurves][n];
            yoperr = new double[maxCurves][n];
            detrend = new double[maxCurves][maxDetrendVars][n];
            xc1 = new double[maxCurves][n];
            xc2 = new double[maxCurves][n];
            yc1 = new double[maxCurves][n];
            yc2 = new double[maxCurves][n];
            detrendXs = new double[maxCurves][n];
            detrendYs = new double[maxCurves][n];
            detrendYEs = new double[maxCurves][n];
        }

        xMultiplierFactor = Math.pow(10, xExponent);
        yMultiplierFactor = Math.pow(10, yExponent);

        int[] detrendVarsUsed = new int[maxCurves];

        nn[curve] = (n - excluded) / inputAverageOverSize[curve];
        nnr[curve] = (n - excluded) % inputAverageOverSize[curve];
        if (nnr[curve] > 0) nn[curve]++;
        detrendVarsUsed[curve] = 0;

        if (xlabel[curve].trim().isEmpty() || (xlabel[curve].equalsIgnoreCase("default") && xlabeldefault.trim().isEmpty())) {
            for (int j = 0; j < nn[curve]; j++)
                x[curve][j] = j + 1;
            xlabel2[curve] = "Sample Number";
        } else {
            if (xlabel[curve].equalsIgnoreCase("default")) {
                xcolumn[curve] = table.getColumnIndex(xlabeldefault);
                if (xcolumn[curve] == ResultsTable.COLUMN_NOT_FOUND) {
                    xcolumn[curve] = 0;
                    plotY[curve] = false;
                }
                xlabel2[curve] = xlabeldefault.trim();
            } else {
                xcolumn[curve] = table.getColumnIndex(xlabel[curve]);
                if (xcolumn[curve] == ResultsTable.COLUMN_NOT_FOUND) {
                    xcolumn[curve] = 0;
                    plotY[curve] = false;
                }
                xlabel2[curve] = xlabel[curve].trim();
            }

            if (plotY[curve]) {
                int bucketSize = inputAverageOverSize[curve];

                for (int j = 0; j < nn[curve]; j++) {
                    double xin;
                    int numNaN = 0;
                    if (nnr[curve] > 0 && j == nn[curve] - 1) {
                        bucketSize = nnr[curve];
                    } else {
                        bucketSize = inputAverageOverSize[curve];
                    }
                    x[curve][j] = 0;
                    for (int k = excludedHeadSamples; k < (bucketSize + excludedHeadSamples); k++) {
                        xin = table.getValueAsDouble(xcolumn[curve], j * inputAverageOverSize[curve] + k);
                        if (Double.isNaN(xin)) {
                            numNaN += 1;
                            if (numNaN == bucketSize) {
                                bucketSize = 1;
                                x[curve][j] = Double.NaN;
                                numNaN = 0;
                                break;
                            }
                        } else {
                            x[curve][j] += xin;
                        }
                    }
                    bucketSize -= numNaN;
                    x[curve][j] = x[curve][j] * xMultiplierFactor / (double) bucketSize;
                }
                if (xlabel2[curve].startsWith("J.D.-2400000")) {
                    for (int j = 0; j < nn[curve]; j++) {
                        x[curve][j] += 2400000;
                    }
                }
            }
        }

        if (plotY[curve]) {
            if (ylabel[curve].trim().isEmpty()) {
                for (int j = 0; j < nn[curve]; j++)
                    y[curve][j] = j + 1;
            } else {
                ycolumn[curve] = table.getColumnIndex(ylabel[curve]);
                if (ycolumn[curve] == ResultsTable.COLUMN_NOT_FOUND) {
                    ycolumn[curve] = 0;
                    plotY[curve] = false;
                }

                if (detrendFitIndex[curve] != 0) {
                    for (int v = 0; v < maxDetrendVars; v++) {
                        if (detrendIndex[v] > 1) {
                            detrendcolumn[curve][v] = table.getColumnIndex(detrendlabel[v]);

                            if (detrendcolumn[curve][v] == ResultsTable.COLUMN_NOT_FOUND) {
                                detrendIndex[v] = 0;
                            } else {
                                detrendVarsUsed[curve]++;
                            }
                        } else if (detrendIndex[v] == 1)  //Meridian Flip Detrend Selected
                        {
                            detrendVarsUsed[curve]++;
                        }
                    }
                }

                errcolumn[curve] = ResultsTable.COLUMN_NOT_FOUND;
                if (operatorBase.getOrCreateVariant(curve).get() == MPOperator.CUSTOM_ERROR) {  //custom error
                    errcolumn[curve] = table.getColumnIndex(oplabel[curve]);
                } else if (ylabel[curve].startsWith("rel_flux_T") || ylabel[curve].startsWith("rel_flux_C")) {
                    errcolumn[curve] = table.getColumnIndex("rel_flux_err_" + ylabel[curve].substring(9));
                } else if (ylabel[curve].startsWith("Source-Sky_")) {
                    errcolumn[curve] = table.getColumnIndex("Source_Error_" + ylabel[curve].substring(11));
                } else if (ylabel[curve].startsWith("tot_C_cnts")) {
                    errcolumn[curve] = table.getColumnIndex("tot_C_err" + ylabel[curve].substring(10));
                } else if (ylabel[curve].startsWith("Source_AMag_")) {
                    errcolumn[curve] = table.getColumnIndex("Source_AMag_Err_" + ylabel[curve].substring(12));
                }
                hasErrors[curve] = errcolumn[curve] != ResultsTable.COLUMN_NOT_FOUND;

                if (operatorBase.getOrCreateVariant(curve).get() != MPOperator.NONE) {
                    opcolumn[curve] = table.getColumnIndex(oplabel[curve]);
                    if (opcolumn[curve] == ResultsTable.COLUMN_NOT_FOUND) {
                        operatorBase.getOrCreateVariant(curve).set(MPOperator.NONE);
                    }
                }

                if (operatorBase.getOrCreateVariant(curve).get().isNormalOperator())// && showErrors[curve] == true)
                {
                    operrcolumn[curve] = ResultsTable.COLUMN_NOT_FOUND;
                    if (oplabel[curve].startsWith("rel_flux_T") || oplabel[curve].startsWith("rel_flux_C")) {
                        operrcolumn[curve] = table.getColumnIndex("rel_flux_err_" + oplabel[curve].substring(9));
                    } else if (oplabel[curve].startsWith("Source-Sky_")) {
                        operrcolumn[curve] = table.getColumnIndex("Source_Error_" + oplabel[curve].substring(11));
                    } else if (oplabel[curve].startsWith("tot_C_cnts")) {
                        operrcolumn[curve] = table.getColumnIndex("tot_C_err" + oplabel[curve].substring(10));
                    }
                    hasOpErrors[curve] = operrcolumn[curve] != ResultsTable.COLUMN_NOT_FOUND;
                } else {
                    hasOpErrors[curve] = false;
                }

                int bucketSize = inputAverageOverSize[curve];
                for (int j = 0; j < nn[curve]; j++) {
                    double yin;
                    double errin;
                    double opin = 0;
                    double operrin;
                    int numNaN = 0;
                    y[curve][j] = 0;
                    yerr[curve][j] = 0;
                    yop[curve][j] = 0;
                    for (int v = 0; v < maxDetrendVars; v++) {
                        detrend[curve][v][j] = 0;
                    }
                    xc1[curve][j] = 0;
                    xc2[curve][j] = 0;
                    yc1[curve][j] = 0;
                    yc2[curve][j] = 0;
                    // AVERAGE DATA IF APPLICABLE
                    if (nnr[curve] > 0 && j == nn[curve] - 1) {
                        bucketSize = nnr[curve];
                    } else {
                        bucketSize = inputAverageOverSize[curve];
                    }
                    for (int k = excludedHeadSamples; k < (bucketSize + excludedHeadSamples); k++) {
                        yin = table.getValueAsDouble(ycolumn[curve], j * inputAverageOverSize[curve] + k);
                        if (Double.isNaN(yin)) {
                            numNaN += 1;
                            if (numNaN == bucketSize) {
                                bucketSize = 1;
                                y[curve][j] = Double.NaN;
                                if (hasErrors[curve]) yerr[curve][j] = Double.NaN;
                                if (operatorBase.getOrCreateVariant(curve).get() != MPOperator.NONE) yop[curve][j] = Double.NaN;
                                if (detrendFitIndex[curve] != 0) {
                                    for (int v = 0; v < maxDetrendVars; v++) {
                                        if (detrendIndex[v] != 0) {
                                            detrend[curve][v][j] = Double.NaN;
                                        }
                                    }
                                }

                                if (operatorBase.getOrCreateVariant(curve).get() == MPOperator.CENTROID_DISTANCE) //calculate distance
                                {
                                    xc1[curve][j] = Double.NaN;
                                    xc2[curve][j] = Double.NaN;
                                    yc1[curve][j] = Double.NaN;
                                    yc2[curve][j] = Double.NaN;
                                }
                                if (hasOpErrors[curve]) yoperr[curve][j] = Double.NaN;
                                numNaN = 0;
                                break;
                            }
                        } else {
                            if (fromMag[curve]) {
                                yin = Math.pow(10, -yin / 2.5);
                            }
                            y[curve][j] += yin;

                            if (hasErrors[curve]) {
                                errin = table.getValueAsDouble(errcolumn[curve], j * inputAverageOverSize[curve] + k);
                                if (fromMag[curve]) {
                                    errin = yin * (-Math.pow(10, -errin / 2.5) + 1);
                                }
                                yerr[curve][j] += errin * errin;
                            }
                            if (operatorBase.getOrCreateVariant(curve).get() != MPOperator.NONE) {
                                opin = table.getValueAsDouble(opcolumn[curve], j * inputAverageOverSize[curve] + k);
                                if (fromMag[curve]) {
                                    opin = Math.pow(10, -opin / 2.5);
                                }
                                yop[curve][j] += opin;
                            }
                            if (detrendFitIndex[curve] != 0) {
                                for (int v = 0; v < maxDetrendVars; v++) {
                                    if (detrendIndex[v] > 1) {
                                        detrend[curve][v][j] += table.getValueAsDouble(detrendcolumn[curve][v], j * inputAverageOverSize[curve] + k);
                                    }
                                }
                            }
                            if (operatorBase.getOrCreateVariant(curve).get() == MPOperator.CENTROID_DISTANCE) //calculate distance
                            {
                                xc1[curve][j] += table.getValueAsDouble(xc1column[curve], j * inputAverageOverSize[curve] + k);
                                xc2[curve][j] += table.getValueAsDouble(xc2column[curve], j * inputAverageOverSize[curve] + k);
                                yc1[curve][j] += table.getValueAsDouble(yc1column[curve], j * inputAverageOverSize[curve] + k);
                                yc2[curve][j] += table.getValueAsDouble(yc2column[curve], j * inputAverageOverSize[curve] + k);
                            }
                            if (hasOpErrors[curve]) {
                                operrin = table.getValueAsDouble(operrcolumn[curve], j * inputAverageOverSize[curve] + k);
                                if (fromMag[curve]) {
                                    operrin = opin * (-Math.pow(10, -operrin / 2.5) + 1);
                                }
                                yoperr[curve][j] += operrin * operrin;
                            }
                        }
                    }
                    bucketSize -= numNaN;
                    y[curve][j] = y[curve][j] / bucketSize;  //*yMultiplierFactor

                    if (hasErrors[curve]) {
                        yerr[curve][j] = Math.sqrt(yerr[curve][j]) / bucketSize; //yMultiplierFactor*
                    } else {
                        yerr[curve][j] = 1.0;
                    }
                    if (detrendFitIndex[curve] != 0) {
                        for (int v = 0; v < maxDetrendVars; v++) {
                            if (detrendIndex[v] != 0) {
                                detrend[curve][v][j] /= bucketSize;
                            }
                        }
                    }
                    if (operatorBase.getOrCreateVariant(curve).get() != MPOperator.NONE) {
                        yop[curve][j] = yop[curve][j] / bucketSize; //*yMultiplierFactor
                        if (operatorBase.getOrCreateVariant(curve).get() == MPOperator.CENTROID_DISTANCE) {
                            xc1[curve][j] = xc1[curve][j] / bucketSize;  //*yMultiplierFactor
                            xc2[curve][j] = xc2[curve][j] / bucketSize;  //*yMultiplierFactor
                            yc1[curve][j] = yc1[curve][j] / bucketSize;  //*yMultiplierFactor
                            yc2[curve][j] = yc2[curve][j] / bucketSize;  //*yMultiplierFactor
                        }
                    }
                    if (hasOpErrors[curve]) {
                        yoperr[curve][j] = Math.sqrt(yoperr[curve][j]) / bucketSize;  //yMultiplierFactor*
                    } else {
                        yoperr[curve][j] = 1.0;
                    }

                    //APPLY OPERATOR/OPERROR FUNCTIONS TO YDATA AND YERROR

                    if (operatorBase.getOrCreateVariant(curve).get() == MPOperator.NONE)  //no operator
                    {

                    } else if (operatorBase.getOrCreateVariant(curve).get() == MPOperator.DIVIDE_BY)  //divide by
                    {
                        if (yop[curve][j] == 0) {
                            yerr[curve][j] = 1.0e+100;
                            y[curve][j] = 1.0e+100;
                        } else {
                            if (hasErrors[curve] || hasOpErrors[curve]) {
                                yerr[curve][j] = Math.sqrt(((yerr[curve][j] * yerr[curve][j]) / (yop[curve][j] * yop[curve][j])) + ((y[curve][j] * y[curve][j] * yoperr[curve][j] * yoperr[curve][j]) / (yop[curve][j] * yop[curve][j] * yop[curve][j] * yop[curve][j])));  //yMultiplierFactor*
                            }
                            y[curve][j] = y[curve][j] / yop[curve][j];  //*yMultiplierFactor
                        }
                    } else if (operatorBase.getOrCreateVariant(curve).get() == MPOperator.MULTIPLY_BY)  //multiply by
                    {
                        if (hasErrors[curve] || hasOpErrors[curve]) {
                            yerr[curve][j] = Math.sqrt(yop[curve][j] * yop[curve][j] * yerr[curve][j] * yerr[curve][j] + y[curve][j] * y[curve][j] * yoperr[curve][j] * yoperr[curve][j]); // /yMultiplierFactor;
                        }
                        y[curve][j] = y[curve][j] * yop[curve][j];  // /yMultiplierFactor;
                    } else if (operatorBase.getOrCreateVariant(curve).get() == MPOperator.SUBTRACT)  //subtract
                    {
                        if (hasErrors[curve] || hasOpErrors[curve]) {
                            yerr[curve][j] = Math.sqrt(yerr[curve][j] * yerr[curve][j] + yoperr[curve][j] * yoperr[curve][j]);
                        }
                        y[curve][j] = y[curve][j] - yop[curve][j];
                    } else if (operatorBase.getOrCreateVariant(curve).get() == MPOperator.ADD)  //add
                    {
                        if (hasErrors[curve] || hasOpErrors[curve]) {
                            yerr[curve][j] = Math.sqrt(yerr[curve][j] * yerr[curve][j] + yoperr[curve][j] * yoperr[curve][j]);
                        }
                        y[curve][j] = y[curve][j] + yop[curve][j];
                    } else if (operatorBase.getOrCreateVariant(curve).get() == MPOperator.CENTROID_DISTANCE)  //distance from x1,y1 to x2,y2
                    {
                        y[curve][j] = (usePixelScale ? pixelScale : 1.0) * Math.sqrt(((xc1[curve][j] - xc2[curve][j]) * (xc1[curve][j] - xc2[curve][j])) + ((yc1[curve][j] - yc2[curve][j]) * (yc1[curve][j] - yc2[curve][j])));
                    }
                }
                if (ylabel[curve].trim().startsWith("J.D.-2400000")) {
                    for (int j = 0; j < nn[curve]; j++) {
                        y[curve][j] += 2400000;
                    }
                }
            }
        }

        if (plotY[curve] && smooth[curve] && nn[curve] > 4) {
            var yMask = MatrixUtils.createRealVector(nn[curve]);
            double xfold;
            double halfPeriod = netPeriod / 2.0;
            var maskTransit = KeplerSplineControl.getInstance(curve).settings.maskTransit.get();
            var maskTrimmedData = KeplerSplineControl.getInstance(curve).settings.maskTrimmedData.get();
            for (int xx = 0; xx < nn[curve]; xx++) {
                if (showXAxisNormal) {
                    if ((maskTransit && x[curve][xx] > dMarker2Value + xOffset && x[curve][xx] < dMarker3Value + xOffset) ||
                            (maskTrimmedData && useDMarker1 && x[curve][xx] < dMarker1Value + xOffset) ||
                            (maskTrimmedData && useDMarker4 && x[curve][xx] > dMarker4Value + xOffset)) {
                        yMask.setEntry(xx,0.0);
                    } else {
                        yMask.setEntry(xx,1.0);
                    }
                } else {
                    xfold = ((x[curve][xx] - netT0) % netPeriod);
                    if (xfold > halfPeriod) { xfold -= netPeriod; } else if (xfold < -halfPeriod) xfold += netPeriod;
                    if (Math.abs(xfold) < duration / 48.0) {
                        yMask.setEntry(xx,0.0);
                    } else {
                        yMask.setEntry(xx,1.0);
                    }
                }
            }

            KeplerSplineControl.getInstance(curve).smoothData(x[curve], y[curve], yerr[curve], nn[curve], yMask);
        }

        dx = 0.0;

        xPlotMin = xMin;
        xPlotMax = xMax;

        xautoscalemin = Double.POSITIVE_INFINITY;
        xautoscalemax = Double.NEGATIVE_INFINITY;

        firstCurve = -1;

        if (plotY[curve]) {
            if (!showXAxisNormal) {
                if (showXAxisAsPhase) {
                    for (int j = 0; j < nn[curve]; j++) {
                        x[curve][j] = ((x[curve][j] - netT0) % netPeriod) / netPeriod;
                        if (x[curve][j] > 0.5) {
                            x[curve][j] -= 1.0;
                        } else if (x[curve][j] < -0.5) x[curve][j] += 1.0;
                    }
                } else if (showXAxisAsDaysSinceTc) {
                    double halfPeriod = netPeriod / 2.0;
                    for (int j = 0; j < nn[curve]; j++) {
                        x[curve][j] = ((x[curve][j] - netT0) % netPeriod);
                        if (x[curve][j] > halfPeriod) {
                            x[curve][j] -= netPeriod;
                        } else if (x[curve][j] < -halfPeriod) x[curve][j] += netPeriod;
                    }
                } else if (showXAxisAsHoursSinceTc) {
                    double halfPeriod = netPeriod / 2.0;
                    for (int j = 0; j < nn[curve]; j++) {
                        x[curve][j] = ((x[curve][j] - netT0) % netPeriod);
                        if (x[curve][j] > halfPeriod) {
                            x[curve][j] -= netPeriod;
                        } else if (x[curve][j] < -halfPeriod) x[curve][j] += netPeriod;
                        x[curve][j] *= 24;
                    }
                }
            }
        }

        residual[curve] = null;
        plottedResidual[curve] = null;
        yModel1Err[curve] = null;
        detrendYAverage[curve] = 0.0;
        if (plotY[curve]) {
            fitMin[curve] = (useDMarker1 ? dMarker1Value : Double.NEGATIVE_INFINITY) + xOffset;
            fitMax[curve] = (useDMarker4 ? dMarker4Value : Double.POSITIVE_INFINITY) + xOffset;
            fitLeft[curve] = dMarker2Value + xOffset;
            fitRight[curve] = dMarker3Value + xOffset;
            switch (detrendFitIndex[curve]) {
                case 2: // left of D2
                    fitMax[curve] = fitLeft[curve];
                    break;
                case 3: // right of D3
                    fitMin[curve] = fitRight[curve];
                    break;
                case 4: // outside D2 and D3
                    break;
                case 5: // inside D2 and D3
                    fitMin[curve] = fitLeft[curve];
                    fitMax[curve] = fitRight[curve];
                    break;
                case 6: // left of D3
                    fitMax[curve] = fitRight[curve];
                    break;
                case 7: // right of D2
                    fitMin[curve] = fitLeft[curve];
                    break;
                case 8: // use all data
                    break;
                case 9: // use all data to fit transit with simultaneaous detrend
                    break;
                default: // use all data
                    detrendFitIndex[curve] = 0;
                    break;
            }

            boolean atLeastOne = false;
            boolean detrendYNotConstant = false;
            detrendYDNotConstant = new boolean[maxDetrendVars];
            detrendYAverage[curve] = 0.0;

            for (int v = 0; v < maxDetrendVars; v++) {
                detrendYDNotConstant[v] = detrendFitIndex[curve] == 1;
            }
            if (detrendFitIndex[curve] != 0) {
                for (int v = 0; v < maxDetrendVars; v++) {
                    if (detrendIndex[v] != 0) {
                        atLeastOne = true;
                        break;
                    }
                }
            }
            if (atLeastOne || detrendFitIndex[curve] == 9) {
                double[] detrendAverage = new double[maxDetrendVars];
                int[] detrendPower = new int[maxDetrendVars];
                for (int v = 0; v < maxDetrendVars; v++) {
                    detrendAverage[v] = 0.0;
                    detrendPower[v] = 1;
                    int numNaNs = 0;
                    for (int j = 0; j < nn[curve]; j++) {
                        if (Double.isNaN(detrend[curve][v][j])) {
                            numNaNs++;
                        } else {
                            detrendAverage[v] += detrend[curve][v][j] / (double) nn[curve];
                        }
                    }
                    detrendAverage[v] = ((double) nn[curve] / ((double) nn[curve] - (double) numNaNs)) * detrendAverage[v];
                    for (int j = 0; j < nn[curve]; j++) {
                        detrend[curve][v][j] -= detrendAverage[v];
                    }

                    if (v > 0) {
                        for (int u = 0; u < v; u++) {
                            if (detrendIndex[u] == detrendIndex[v]) detrendPower[v]++;
                        }
                    }
                    if (detrendPower[v] > 1) {
                        detrendAverage[v] = 0.0;
                        numNaNs = 0;
                        for (int j = 0; j < nn[curve]; j++) {
                            if (Double.isNaN(detrend[curve][v][j])) {
                                numNaNs++;
                            } else {
                                detrendAverage[v] += detrend[curve][v][j];
                            }
                        }
                        detrendAverage[v] /= (nn[curve] - numNaNs);
                        for (int j = 0; j < nn[curve]; j++) {
                            detrend[curve][v][j] -= detrendAverage[v];
                        }
                    }
                }
                for (int v = 0; v < maxDetrendVars; v++) {
                    if (detrendPower[v] == 2) {
                        for (int j = 0; j < nn[curve]; j++) {
                            detrend[curve][v][j] *= detrend[curve][v][j];
                        }
                    } else if (detrendPower[v] > 2) {
                        for (int j = 0; j < nn[curve]; j++) {
                            detrend[curve][v][j] = Math.pow(detrend[curve][v][j], detrendPower[v]);
                        }
                    }
                }

                double meridianFlip = mfMarker1Value + xOffset;
                for (int v = 0; v < maxDetrendVars; v++) {
                    if (detrendIndex[v] == 1)    //Meridian Flip Detrend Selected
                    {
                        for (int j = 0; j < nn[curve]; j++) {
                            if (x[curve][j] < meridianFlip)  //meridian flip fitting data = -1.0 to left and 1.0 to right of flip
                            {
                                detrend[curve][v][j] = -1.0;
                            } else {
                                detrend[curve][v][j] = 1.0;
                            }
                        }
                    }
                }

                yAverage[curve] = 0.0;
                yDepthEstimate[curve] = 0.0;
                yBaselineAverage[curve] = 0.0;
                nFitTrim[curve] = 0;
                int avgCount = 0;
                int detrendCount = 0;
                int baselineCount = 0;
                int depthCount = 0;

                double[] detrendX = new double[nn[curve]];
                double[] detrendY = new double[nn[curve]];
                double[] detrendYE = new double[nn[curve]];
                double[] detrendConstantComparator = new double[Math.max(nn[curve], maxDetrendVars)];
                Arrays.fill(detrendConstantComparator, Double.NaN);
                if (detrendFitIndex[curve] != 1) {
                    double[][] detrendYD = new double[maxDetrendVars][nn[curve]];
                    boolean noNaNs = true;
                    if (detrendFitIndex[curve] == 4) {
                        for (int j = 0; j < nn[curve]; j++) {
                            noNaNs = true;
                            if (!Double.isNaN(y[curve][j])) {
                                for (int v = 0; v < maxDetrendVars; v++) {
                                    if (detrendIndex[v] != 0 && Double.isNaN(detrend[curve][v][j])) {
                                        noNaNs = false;
                                        break;
                                    }
                                }
                            } else {
                                noNaNs = false;
                            }
                            if (noNaNs) {
                                avgCount++;
                                yAverage[curve] += y[curve][j];
                                if ((x[curve][j] > fitMin[curve] && x[curve][j] < fitLeft[curve]) || (x[curve][j] > fitRight[curve] && x[curve][j] < fitMax[curve])) {
                                    detrendX[detrendCount] = x[curve][j];
                                    detrendY[detrendCount] = y[curve][j];
                                    detrendYE[detrendCount] = hasErrors[curve] || hasOpErrors[curve] ? yerr[curve][j] : 1;
                                    if (detrendY[0] != detrendY[detrendCount]) detrendYNotConstant = true;
                                    for (int v = 0; v < maxDetrendVars; v++) {
                                        detrendYD[v][detrendCount] = detrend[curve][v][j];

                                        if (Double.isNaN(detrendConstantComparator[v]) && !Double.isNaN(detrendYD[v][detrendCount])) {
                                            detrendConstantComparator[v] = detrendYD[v][detrendCount];
                                        }
                                        if (!Double.isNaN(detrendConstantComparator[v]) && detrendConstantComparator[v] != detrendYD[v][detrendCount]) {
                                            detrendYDNotConstant[v] = true;
                                        }
                                    }
                                    detrendCount++;
                                }
                            }
                        }
                        if (detrendVarsUsed[curve] > 0 && detrendYNotConstant && detrendCount > detrendVarsUsed[curve] + 2) { //need enough data to provide degrees of freedom for successful fit
                            xModel1[curve] = new double[2];
                            xModel2[curve] = new double[2];
                            xModel1[curve][0] = Math.max(fitMin[curve], xPlotMin);
                            xModel1[curve][1] = fitLeft[curve];
                            xModel2[curve][0] = fitRight[curve];
                            xModel2[curve][1] = Math.min(fitMax[curve], xPlotMax);
                            if (xModel1[curve][0] >= xModel1[curve][1]) xModel1[curve] = null;
                            if (xModel2[curve][0] >= xModel2[curve][1]) xModel2[curve] = null;
                        } else {
                            for (int v = 0; v < maxDetrendVars; v++) {
                                if (detrendIndex[v] > 0) {
                                    detrendFactor[curve][v] = 0.0;
                                }
                            }
                        }
                    } else {
                        for (int j = 0; j < nn[curve]; j++) {
                            noNaNs = true;
                            if (!Double.isNaN(y[curve][j])) {
                                for (int v = 0; v < maxDetrendVars; v++) {
                                    if (detrendIndex[v] != 0 && Double.isNaN(detrend[curve][v][j])) {
                                        noNaNs = false;
                                        break;
                                    }
                                }
                            } else {
                                noNaNs = false;
                            }
                            if (noNaNs) {
                                avgCount++;
                                yAverage[curve] += y[curve][j];
                                if (x[curve][j] > fitMin[curve]) {
                                    if (x[curve][j] < fitMax[curve]) {
                                        detrendX[detrendCount] = x[curve][j];
                                        detrendY[detrendCount] = y[curve][j];
                                        detrendYE[detrendCount] = hasErrors[curve] || hasOpErrors[curve] ? yerr[curve][j] : 1;
                                        if (detrendY[0] != detrendY[detrendCount]) detrendYNotConstant = true;
                                        for (int v = 0; v < maxDetrendVars; v++) {
                                            detrendYD[v][detrendCount] = detrend[curve][v][j];
                                            if (Double.isNaN(detrendConstantComparator[v]) && !Double.isNaN(detrendYD[v][detrendCount])) {
                                                detrendConstantComparator[v] = detrendYD[v][detrendCount];
                                            }
                                            if (!Double.isNaN(detrendConstantComparator[v]) && detrendConstantComparator[v] != detrendYD[v][detrendCount]) {
                                                detrendYDNotConstant[v] = true;
                                            }
                                        }
                                        detrendCount++;
                                    }
                                } else {
                                    nFitTrim[curve]++;
                                }
                            }
                        }
                        if (detrendVarsUsed[curve] > 0 && detrendYNotConstant && detrendCount > detrendVarsUsed[curve] + 2) //need enough data to provide degrees of freedom for successful fit
                        {
                            xModel1[curve] = new double[2];
                            xModel1[curve][0] = Math.max(fitMin[curve], xPlotMin);
                            xModel1[curve][1] = Math.min(fitMax[curve], xPlotMax);
                            xModel2[curve] = null;
                        } else {
                            for (int v = 0; v < maxDetrendVars; v++) {
                                if (detrendIndex[v] > 0) {
                                    detrendFactor[curve][v] = 0.0;
                                }
                            }
                        }
                    }
                    initAvgCount = avgCount;
                    initAtLeastOne = atLeastOne;
                    initDetrendCount = detrendCount;
                    initBaselineCount = baselineCount;
                    initDepthCount = depthCount;
                    initDetrendVarsUsed = detrendVarsUsed;
                    initDetrendY = detrendY;
                    initDetrendYNotConstant = detrendYNotConstant;
                    initDetrendYE = detrendYE;
                    initDetrendYD = detrendYD;
                    initDetrendX = detrendX;
                }
            }
        }
    }

    private FluxData conditionData(int ap2reference, boolean[] localIsRefStar, int totCcntAP) {
        var rel_flux = new double[source[0].length];
        var rel_flux_err = new double[source[0].length];
        var tot_C_cnts = new double[source[0].length];
        var tot_C_err = new double[source[0].length];
        var rel_flux_snr = new double[source[0].length];

        for (int i = 0; i < source[0].length; i++) {
            var compSum = 0.0;
            var compVar = 0.0;
            for (int ap = 0; ap < localIsRefStar.length; ap++) {
                if (localIsRefStar[ap] && ap2reference != ap) {
                    compSum += source[ap][i];
                    compVar += srcvar[ap][i];
                }
            }
            if (compSum == 0) continue;
            rel_flux[i] = source[ap2reference][i] / compSum;
            var inverseFactor = 0D;
            if (source[ap2reference][i] == 0) {
                rel_flux[i] = Double.POSITIVE_INFINITY;
            } else {
                var factor = Math.sqrt(srcvar[ap2reference][i] / (source[ap2reference][i] * source[ap2reference][i]) + compVar / (compSum * compSum));
                rel_flux_err[i] = hasErrors[curve] || hasOpErrors[curve] ? (rel_flux[i] * factor) : 1;
                inverseFactor = 1 / factor;
            }
            if (doInstancedDetrendCalculation) {
                if (ap2reference == targetStar) {
                    tot_C_cnts[i] = totCcntAP < 0 ? 0.0 : compSum;
                    tot_C_err[i] = totCcntAP < 0 ? 0.0 : Math.sqrt(compVar);
                }
                rel_flux_snr[i] = inverseFactor;
            }
        }

        if (plotY[curve] && smooth[curve] && nn[curve] > 4) {
            var yMask = MatrixUtils.createRealVector(nn[curve]);
            double xfold;
            double halfPeriod = netPeriod / 2.0;
            for (int xx = 0; xx < nn[curve]; xx++) {
                if (showXAxisNormal) {
                    if (x[curve][xx] > dMarker2Value + xOffset && x[curve][xx] < dMarker3Value + xOffset){
                        yMask.setEntry(xx,0.0);
                    } else {
                        yMask.setEntry(xx,1.0);
                    }
                } else {
                    xfold = ((x[curve][xx] - netT0) % netPeriod);
                    if (xfold > halfPeriod) { xfold -= netPeriod; } else if (xfold < -halfPeriod) xfold += netPeriod;
                    if (Math.abs(xfold) < duration / 48.0) {
                        yMask.setEntry(xx,0.0);
                    } else {
                        yMask.setEntry(xx,1.0);
                    }
                }
            }

            KeplerSplineControl.getInstance(curve).smoothData(x[curve], rel_flux, rel_flux_err, nn[curve], yMask, false);
        }

        return new FluxData(averageAndTrimData(rel_flux), averageAndTrimData(rel_flux_err, true),
                averageAndTrimData(rel_flux_snr), averageAndTrimData(tot_C_cnts), averageAndTrimData(tot_C_err));
    }

    private double[] averageAndTrimData(double[] allData) {
        return averageAndTrimData(allData, false);
    }

    private double[] averageAndTrimData(double[] allData, boolean sumOfSquares) {
        int bucketSize = 0;
        var workingSource = new double[nn[curve]];
        if (excludedHeadSamples + excludedTailSamples >= n) { //Handle case for more samples excluded than in dataset
            excludedHeadSamples = excludedHeadSamples < n ? excludedHeadSamples : n - 1;
            excludedTailSamples = n - excludedHeadSamples - 1;
        }
        for (int j = 0; j < nn[curve]; j++) {
            if (nnr[curve] > 0 && j == nn[curve] - 1) {
                bucketSize = nnr[curve];
            } else {
                bucketSize = inputAverageOverSize[curve];
            }
            for (int k = excludedHeadSamples; k < (bucketSize + excludedHeadSamples); k++) {
                var i = j * inputAverageOverSize[curve] + k;
                workingSource[j] += sumOfSquares ? allData[i] * allData[i] : allData[i];
            }
            if (sumOfSquares) {
                workingSource[j] = Math.sqrt(workingSource[j]);
            }
            workingSource[j] /= bucketSize;
        }

        return workingSource;
    }

    private double[] markersTrimData(double[] workingSource) {
        fitMin[curve] = (useDMarker1 ? dMarker1Value : Double.NEGATIVE_INFINITY) + xOffset;
        fitMax[curve] = (useDMarker4 ? dMarker4Value : Double.POSITIVE_INFINITY) + xOffset;
        fitLeft[curve] = dMarker2Value + xOffset;
        fitRight[curve] = dMarker3Value + xOffset;
        switch (detrendFitIndex[curve]) {
            case 2: // left of D2
                fitMax[curve] = fitLeft[curve];
                break;
            case 3: // right of D3
                fitMin[curve] = fitRight[curve];
                break;
            case 4: // outside D2 and D3
                break;
            case 5: // inside D2 and D3
                fitMin[curve] = fitLeft[curve];
                fitMax[curve] = fitRight[curve];
                break;
            case 6: // left of D3
                fitMax[curve] = fitRight[curve];
                break;
            case 7: // right of D2
                fitMin[curve] = fitLeft[curve];
                break;
            case 8: // use all data
                break;
            case 9: // use all data to fit transit with simultaneaous detrend
                break;
            default: // use all data
                detrendFitIndex[curve] = 0;
                break;
        }

        var workingSource2 = new double[nn[curve]];

        //todo check on Source-Sky, Source_Error values, and detrend parameters for NaNs. If a NaN is found, bail out and give an appropriate error.
        for (int j = 0; j < workingSource2.length; j++) {
            workingSource2[j] = Double.NaN;
            if (detrendFitIndex[curve] != 1) {
                if (detrendFitIndex[curve] == 4) {
                    if ((x[curve][j] > fitMin[curve] && x[curve][j] < fitLeft[curve]) || (x[curve][j] > fitRight[curve] && x[curve][j] < fitMax[curve])) {
                        workingSource2[j] = workingSource[j];
                    }
                } else {
                    if (x[curve][j] > fitMin[curve]) {
                        if (x[curve][j] < fitMax[curve]) {
                            workingSource2[j] = workingSource[j];
                        }
                    }
                }
            }
        }

        return Arrays.copyOf(Arrays.stream(workingSource2).filter(Double::isFinite).toArray(), nn[curve]);
    }

    private CurveData preprocessData(boolean[] localIsRefStar) {
        int totCcntAP = -1;
        for (int ap = 0; ap < numAps; ap++) {
            if (!isRefStar[ap]) {
                totCcntAP = ap;
                break;
            }
        }

        var targetFlux = localIsRefStar != null ?
                conditionData(targetStar, localIsRefStar, totCcntAP) :
                // Raw input of light curve, no stars
                new FluxData(Arrays.copyOf(y[curve], nn[curve]), Arrays.copyOf(yerr[curve], nn[curve]), null, null, null);

        HashMap<ColumnInfo, double[]> instancedParamData = new HashMap<>();
        if (doInstancedDetrendCalculation) {
            if (detrendFitIndex[curve] != 0) {
                for (int v = 0; v < maxDetrendVars; v++) {
                    if (detrendIndex[v] > 1) {
                        for (ParamType type : ParamType.values()) {
                            if (!type.matches(detrendlabel[v])) continue;
                            var ap = type.getAperture(detrendlabel[v]);
                            if (type == ParamType.TOT_C_ERR || type == ParamType.TOT_C_CNTS) {
                                instancedParamData.put(new ColumnInfo(type, -1, v), type.getData(targetFlux));
                            } else {
                                if (ap == null) continue;
                                var data = ap == targetStar ? targetFlux : conditionData(ap, localIsRefStar, totCcntAP);
                                instancedParamData.put(new ColumnInfo(type, ap, v), type.getData(data));
                            }
                        }
                    }
                }
            }
        }

        return new CurveData(targetFlux.flux, targetFlux.err, instancedParamData);
    }

    private OptimizerResults updateCurve(boolean[] isRefStar, int[] detrendIndex) {
        var minimization = minimizationThreadLocal.get();
        var avgCount = initAvgCount;
        var atLeastOne = initAtLeastOne;
        var detrendCount = initDetrendCount;
        var baselineCount = initBaselineCount;
        var depthCount = initDepthCount;
        var detrendVarsUsed = Arrays.copyOf(initDetrendVarsUsed, initDetrendVarsUsed.length);
        var detrendY = Arrays.copyOf(initDetrendY, initDetrendY.length);
        var detrendYNotConstant = initDetrendYNotConstant;
        var detrendYE = Arrays.copyOf(initDetrendYE, initDetrendYE.length);
        var detrendX = Arrays.copyOf(initDetrendX, initDetrendX.length);
        var detrendYD = Arrays.copyOf(initDetrendYD, initDetrendYD.length);

        var yAverage = 0D;
        var yDepthEstimate = MultiPlot_.yDepthEstimate[curve];
        var yBaselineAverage = MultiPlot_.yBaselineAverage[curve];
        var detrendYAverage = MultiPlot_.detrendYAverage[curve];
        double[] start;
        double[] width;
        double[] step;
        var dof = MultiPlot_.dof[curve];
        int[] index;
        boolean[] isFitted = Arrays.copyOf(MultiPlot_.isFitted[curve], MultiPlot_.isFitted[curve].length);
        double[] coeffs;
        var detrendVars = Arrays.copyOf(MultiPlot_.detrendVars[curve], MultiPlot_.detrendVars[curve].length);
        var xModel1 = MultiPlot_.xModel1[curve] != null ? Arrays.copyOf(MultiPlot_.xModel1[curve], MultiPlot_.xModel1[curve].length) : null;
        var xModel2 = MultiPlot_.xModel2[curve];
        var yModel1 = MultiPlot_.yModel1[curve] != null ? Arrays.copyOf(MultiPlot_.yModel1[curve], MultiPlot_.yModel1[curve].length) : null;
        var yModel2 = MultiPlot_.yModel2[curve] != null ? Arrays.copyOf(MultiPlot_.yModel2[curve], MultiPlot_.yModel2[curve].length) : null;
        var yerr = Arrays.copyOf(MultiPlot_.yerr[curve], MultiPlot_.yerr[curve].length);
        var yModel1Err = MultiPlot_.yModel1Err[curve];
        var residual = MultiPlot_.residual[curve];
        var bestFit = Arrays.copyOf(MultiPlot_.bestFit[curve], MultiPlot_.bestFit[curve].length);
        var sigma = MultiPlot_.sigma[curve];
        var bic = MultiPlot_.bic[curve];
        var bp = MultiPlot_.bp[curve];
        var chi2dof = MultiPlot_.chi2dof[curve];
        var detrendFactor = Arrays.copyOf(MultiPlot_.detrendFactor[curve], MultiPlot_.detrendFactor[curve].length);
        var nTries = MultiPlot_.nTries[curve];
        var converged = MultiPlot_.converged[curve];
        var createDetrendModel = MultiPlot_.createDetrendModel;
        var detrend = Arrays.stream(MultiPlot_.detrend[curve]).map(double[]::clone).toArray($ -> MultiPlot_.detrend[curve].clone());
        var maxDetrendVars = MultiPlot_.maxDetrendVars;
        var maxFittedVars = 7 + maxDetrendVars;

        detrendVarsUsed[curve] = (int) Arrays.stream(detrendIndex).filter(i -> i != 0).count();

        if (detrendFitIndex[curve] != 0) {
            for (int v = 0; v < maxDetrendVars; v++) {
                if (detrendIndex[v] != 0) {
                    atLeastOne = true;
                    break;
                }
            }
        }

        if (operatorBase.getOrCreateVariant(curve).get() != MPOperator.NONE) {
            throw new MPOperatorError();
        }

        if (!plotY[curve]) return new OptimizerResults(Double.NaN, Double.NaN);

        var curveData = preprocessData(isRefStar);
        double[] y = new double[nn[curve]];
        for (int i = 0; i < curveData.rel_flux.length; i++) {
            y[i] = curveData.rel_flux[i];
            yerr[i] = curveData.err[i];
            yAverage += y[i];
        }
        detrendYE = markersTrimData(curveData.err);
        detrendY = markersTrimData(y);

        curveData.instancedParamData.forEach((columnInfo, data) -> detrend[columnInfo.detrendColumn] = data);

        /*var detrendVarAllNaNs = new boolean[maxDetrendVars];
        for (int v = 0; v < maxDetrendVars; v++) {
            detrendVarAllNaNs[v] = true;
            for (int j = 0; j < nn[curve]; j++) {
                if (!Double.isNaN(detrend[v][j])) {
                    detrendVarAllNaNs[v] = false;
                    break;
                }
            }
        }*/

        /*for (int v = 0; v < maxDetrendVars; v++) {
            if (detrendVarAllNaNs[v]) {
                //detrendIndex[v] = 0;
                detrendYDNotConstant[v] = false;
                //detrend[detrendIndex[v]] = new double[nn[curve]];
            }
            detrendYDNotConstant[v] = !detrendVarAllNaNs[v];
        }*/

        if (atLeastOne || detrendFitIndex[curve] == 9) {
            double[] detrendAverage = new double[maxDetrendVars];
            int[] detrendPower = new int[maxDetrendVars];
            for (int v = 0; v < maxDetrendVars; v++) {
                detrendAverage[v] = 0.0;
                detrendPower[v] = 1;
                int numNaNs = 0;
                for (int j = 0; j < nn[curve]; j++) {
                    if (Double.isNaN(detrend[v][j])) {
                        numNaNs++;
                    } else {
                        detrendAverage[v] += detrend[v][j] / (double) nn[curve];
                    }
                }
                detrendAverage[v] = ((double) nn[curve] / ((double) nn[curve] - (double) numNaNs)) * detrendAverage[v];
                for (int j = 0; j < nn[curve]; j++) {
                    detrend[v][j] -= detrendAverage[v];
                }

                if (v > 0) {
                    for (int u = 0; u < v; u++) {
                        if (detrendIndex[u] == detrendIndex[v]) detrendPower[v]++;
                    }
                }
                if (detrendPower[v] > 1) {
                    detrendAverage[v] = 0.0;
                    numNaNs = 0;
                    for (int j = 0; j < nn[curve]; j++) {
                        if (Double.isNaN(detrend[v][j])) {
                            numNaNs++;
                        } else {
                            detrendAverage[v] += detrend[v][j];
                        }
                    }
                    detrendAverage[v] /= (nn[curve] - numNaNs);
                    for (int j = 0; j < nn[curve]; j++) {
                        detrend[v][j] -= detrendAverage[v];
                    }
                }
            }
            for (int v = 0; v < maxDetrendVars; v++) {
                if (detrendPower[v] == 2) {
                    for (int j = 0; j < nn[curve]; j++) {
                        detrend[v][j] *= detrend[v][j];
                    }
                } else if (detrendPower[v] > 2) {
                    for (int j = 0; j < nn[curve]; j++) {
                        detrend[v][j] = Math.pow(detrend[v][j], detrendPower[v]);
                    }
                }
            }

            double meridianFlip = mfMarker1Value + xOffset;
            for (int v = 0; v < maxDetrendVars; v++) {
                if (detrendIndex[v] == 1)    //Meridian Flip Detrend Selected
                {
                    for (int j = 0; j < nn[curve]; j++) {
                        if (x[curve][j] < meridianFlip)  //meridian flip fitting data = -1.0 to left and 1.0 to right of flip
                        {
                            detrend[v][j] = -1.0;
                        } else {
                            detrend[v][j] = 1.0;
                        }
                    }
                }

                // Apply left/right marker trim
                detrendYD[v] = markersTrimData(detrend[v]);
            }

            if (detrendFitIndex[curve] != 1) {
                if (detrendCount > 0) {
                    for (int j = 0; j < nn[curve]; j++) {
                        //avgCount++;
                        //if (noNaNs) {
                        detrendYAverage += y[j];
                        if (detrendFitIndex[curve] == 9) {
                            if (x[curve][j] > fitLeft[curve] + (fitRight[curve] - fitLeft[curve]) / 4.0 && x[curve][j] < fitRight[curve] - (fitRight[curve] - fitLeft[curve]) / 4.0) {
                                yDepthEstimate += y[j];
                                depthCount++;
                            } else if (x[curve][j] < fitLeft[curve] || x[curve][j] > fitRight[curve]) {
                                yBaselineAverage += y[j];
                                baselineCount++;
                            }
                        }
                        //}
                    }
                    yAverage /= avgCount; //avgCount is always >= detrendCount, so > 0 here
                    detrendYAverage /= detrendCount;
                    if (baselineCount > 0) {
                        yBaselineAverage /= baselineCount;
                    } else {
                        yBaselineAverage = detrendYAverage * 1.005;
                    }
                    if (depthCount > 0) {
                        yDepthEstimate /= depthCount;
                    } else {
                        yDepthEstimate = detrendYAverage * 0.995;
                    }

                    for (int j = 0; j < detrendCount; j++) {
                        detrendY[j] -= detrendYAverage;//yAverage;
                    }

                    for (int v = 0; v < maxDetrendVars; v++) {
                        if (detrendIndex[v] != 0 && !detrendYDNotConstant[v] && detrendVarsUsed[curve] > 0) {
                            detrendVarsUsed[curve]--;
                        }
                    }
                    detrendVars = new double[detrendVarsUsed[curve]][detrendCount];
                    int varCount = 0;
                    for (int v = 0; v < maxDetrendVars; v++) {
                        if (detrendIndex[v] != 0 && detrendYDNotConstant[v]) {
                            detrendVars[varCount] = Arrays.copyOf(detrendYD[v], detrendCount);
                            varCount++;
                        }
                    }

                    // Update prior centers/widths
                    var priorCenter = new double[maxFittedVars];
                    var priorWidth = new double[maxFittedVars];
                    priorCenter[0] = 1.0;  //f0 = baseline flux
                    priorCenterStep[0] = 0.001;
                    priorWidth[0] = 0.005;
                    priorWidthStep[0] = 0.001;
                    fitStepStep[0] = 0.1;

                    priorCenter[1] = 0.010;    // depth = (r_p/r_*)^2
                    priorCenterStep[1] = 0.001;
                    priorWidth[1] = 0.010;
                    priorWidthStep[1] = 0.001;
                    fitStepStep[1] = 0.1;

                    priorCenter[2] = 10.0;   // a/r_*
                    priorCenterStep[2] = 1.0;
                    priorWidth[2] = 7;
                    priorWidthStep[2] = 1.0;
                    fitStepStep[2] = 0.1;

                    priorCenter[3] = 2456500;  // tc = transit center time
                    priorCenterStep[3] = 0.001;
                    priorWidth[3] = 0.015;
                    priorWidthStep[3] = 0.001;
                    fitStepStep[3] = 0.01;

                    priorCenter[4] = 88.0;  // inclination
                    priorCenterStep[4] = 1.0;
                    priorWidth[4] = 15;
                    priorWidthStep[4] = 1;
                    fitStepStep[4] = 1.0;

                    priorCenter[5] = 0.3;  // u1
                    priorCenterStep[5] = 0.1;
                    priorWidth[5] = 1.0;
                    priorWidthStep[5] = 0.1;
                    fitStepStep[5] = 0.1;

                    priorCenter[6] = 0.3;  // u2
                    priorCenterStep[6] = 0.1;
                    priorWidth[6] = 1.0;
                    priorWidthStep[6] = 0.1;
                    fitStepStep[6] = 0.1;

                    if (priorCenter.length > 7) {
                        for (int j = 7; j < priorCenter.length; j++) { //detrend1, detrend2, ...
                            priorCenter[j] = 0.0;
                            priorCenterStep[j] = 0.000001;
                            priorWidth[j] = 1.0;
                            priorWidthStep[j] = 0.01;
                            fitStepStep[j] = 0.1;
                        }
                    }

                    // Pull locked values
                    for (int d = 0; d < maxDetrendVars + 7; d++) {
                        priorCenter[d] = (Double) priorCenterSpinner[curve][d].getValue();
                    }

                    if (!lockToCenter[curve][0] && autoUpdatePrior[curve][0]) {
                        priorCenter[0] = yBaselineAverage;   //f0 = baseline flux
                        priorWidth[0] = Math.abs(yBaselineAverage / 5.0);
                    }

                    double rpOrstarEst = Math.abs((yBaselineAverage - yDepthEstimate) / yBaselineAverage);
                    if (!lockToCenter[curve][1] && autoUpdatePrior[curve][1]) {
                        priorCenter[1] = rpOrstarEst;          // depth = (r_p/r_*)^2
                        priorWidth[1] = Math.abs(rpOrstarEst / 2.0);
                    }

                    //Adapted from Winn 2010 equation 14
                    if (!lockToCenter[curve][2] && autoUpdatePrior[curve][2]) {
                        priorCenter[2] = (1 + Math.sqrt(priorCenter[1])) / (Math.sin(Math.PI * (dMarker3Value - dMarker2Value) / orbitalPeriod[curve])); // ar = a/r_*
                    }
                    if (!lockToCenter[curve][3] && autoUpdatePrior[curve][3]) {
                        if (showXAxisNormal) {
                            priorCenter[3] = (int) xPlotMinRaw + (dMarker2Value + dMarker3Value) / 2.0;   // tc = transit center time
                        } else {
                            priorCenter[3] = (dMarker2Value + dMarker3Value) / 2.0;   // tc = transit center time
                        }
                    }
                    if (!lockToCenter[curve][4] && !bpLock[curve] && autoUpdatePrior[curve][4]) {
                        priorCenter[4] = Math.round(10.0 * Math.acos((0.5 + Math.sqrt(rpOrstarEst)) / (priorCenter[2])) * 180.0 / Math.PI) / 10.0; // inclination
                    }
                    if (bpLock[curve]) {
                        var bp1 = (Double) bpSpinner[curve].getValue();
                        priorCenter[4] = (180.0 / Math.PI) * Math.acos(bp1 / bestFit[2]);
                    }
                    // End update

                    if ((detrendVarsUsed[curve] >= 0 || detrendFitIndex[curve] == 9 && useTransitFit[curve]) && detrendYNotConstant && detrendCount > (detrendFitIndex[curve] == 9 && useTransitFit[curve] ? 7 : 0) + detrendVarsUsed[curve] + 2) { //need enough data to provide degrees of freedom for successful fit
                        detrendX = Arrays.copyOf(detrendX, detrendCount);
                        detrendY = Arrays.copyOf(detrendY, detrendCount);
                        detrendYE = Arrays.copyOf(detrendYE, detrendCount);

                        int fittedDetrendParStart;

                        if (detrendFitIndex[curve] == 9) {
                            minimization.removeConstraints();

                            int nFitted = 0;
                            for (int p = 0; p < 7; p++) {
                                if (useTransitFit[curve] && bpLock[curve] && p==4) {
                                    isFitted[p] = false;
                                    continue;
                                }
                                if (useTransitFit[curve] && !lockToCenter[curve][p]) {
                                    isFitted[p] = true;
                                    nFitted++;
                                } else {
                                    isFitted[p] = false;
                                }
                            }

                            fittedDetrendParStart = nFitted;
                            for (int p = 7; p < maxFittedVars; p++) {
                                if (detrendIndex[p - 7] != 0 && detrendYDNotConstant[p - 7] && !lockToCenter[curve][p]) {
                                    isFitted[p] = true;
                                    nFitted++;
                                } else {
                                    isFitted[p] = false;
                                }
                            }

                            start = new double[nFitted];
                            width = new double[nFitted];
                            step = new double[nFitted];

                            index = new int[nFitted];
                            int fp = 0;  //fitted parameter
                            for (int p = 0; p < maxFittedVars; p++) {
                                if (isFitted[p]) {
                                    index[fp] = p;
                                    fp++;
                                }
                            }


                            dof = detrendX.length - nFitted;

                            // Fit against yAvg when no transit and no params
                            if (nFitted == 0 && !useTransitFit[curve]) {
                                start = new double[1];
                                width = new double[1];
                                step = new double[1];

                                index = new int[1];
                                start[0] = ArrayUtil.median(detrendYs[curve]);
                                minimization.addConstraint(0, -1, 0.0);
                            }

                            // 0 = f0 = baseline flux
                            // 1 = p0 = r_p/r_*
                            // 2 = ar = a/r_*
                            // 3 = tc = transit center time
                            // 4 = i = inclination
                            // 5 = u1 = quadratic limb darkening parameter 1
                            // 6 = u2 = quadratic limb darkening parameter 2
                            // 7+ = detrend parameters
                            for (fp = 0; fp < nFitted; fp++) {
                                if (index[fp] == 1) {
                                    start[fp] = Math.sqrt(priorCenter[1]);
                                    width[fp] = Math.sqrt(priorWidth[1]);
                                    step[fp] = Math.sqrt(getFitStep(curve, 1, priorWidth, priorCenter));
                                    minimization.addConstraint(fp, -1, 0.0);
                                } else if (index[fp] == 4) {
                                    if (bpLock[curve]) continue;
                                    start[fp] = priorCenter[4] * Math.PI / 180.0;  // inclination
                                    width[fp] = priorWidth[4] * Math.PI / 180.0;
                                    step[fp] = getFitStep(curve, 4, priorWidth, priorCenter) * Math.PI / 180.0;
                                    minimization.addConstraint(fp, 1, 90.0 * Math.PI / 180.0);
                                    minimization.addConstraint(fp, -1, 50.0 * Math.PI / 180.0);
                                } else {
                                    if (index[fp] == 0) minimization.addConstraint(fp, -1, 0.0);
                                    if (index[fp] == 2) minimization.addConstraint(fp, -1, 2.0);
                                    //if (index[fp] == 3) minimization.addConstraint(fp, -1, 0.0);
                                    if (index[fp] == 5) {
                                        minimization.addConstraint(fp, 1, 1.0);
                                        minimization.addConstraint(fp, -1, -1.0);
                                    }
                                    if (index[fp] == 6) {
                                        minimization.addConstraint(fp, 1, 1.0);
                                        minimization.addConstraint(fp, -1, -1.0);
                                    }
                                    start[fp] = priorCenter[index[fp]];
                                    width[fp] = priorWidth[index[fp]];
                                    step[fp] = getFitStep(curve, index[fp], priorWidth, priorCenter);
                                }
                                if (usePriorWidth[curve][index[fp]]) {
                                    width[fp] = MultiPlot_.priorWidth[curve][index[fp]];
                                    step[fp] = getFitStep(curve, index[fp]);
                                    minimization.addConstraint(fp, 1, start[fp] + width[fp]);
                                    minimization.addConstraint(fp, -1, start[fp] - width[fp]);
                                }
                            }

                            minimization.setNrestartsMax(1);
                            minimization.nelderMead(new FitLightCurveChi2(detrendY, dof, bp, detrendX, detrendYE, isFitted, detrendYAverage, priorCenter, detrendIndex, maxFittedVars, detrendVars),
                                    start, step, tolerance[curve], maxFitSteps[curve]);
                            coeffs = minimization.getParamValues();
                            nTries = minimization.getNiter() - 1;
                            converged = minimization.getConvStatus();
                            fp = 0;
                            for (int p = 0; p < maxFittedVars; p++) {
                                if (isFitted[p]) {
                                    bestFit[p] = coeffs[fp];
                                    fp++;
                                } else if (p < 7 && useTransitFit[curve] && lockToCenter[curve][p]) {
                                    if (p == 1) {
                                        bestFit[p] = Math.sqrt(priorCenter[p]);
                                    } else if (p == 4) {
                                        if (bpLock[curve]) {
                                            bestFit[4] = Math.acos(bp / bestFit[2]);
                                        } else {
                                            bestFit[p] = priorCenter[p] * Math.PI / 180.0;
                                        }
                                    } else {
                                        bestFit[p] = priorCenter[p];
                                    }
                                } else if (p >= 7 && p < 7 + maxDetrendVars && detrendIndex[p - 7] != 0 && detrendYDNotConstant[p - 7] && lockToCenter[curve][p]) {
                                    bestFit[p] = priorCenter[p];
                                } else if (p == 4 && bpLock[curve]) {
                                    bestFit[4] = Math.acos(bp/bestFit[2]);
                                } else {
                                    bestFit[p] = Double.NaN;
                                }
                            }

                            chi2dof = minimization.getMinimum();
                            bic = (chi2dof * dof) + nFitted * Math.log(detrendX.length);

                            fp = fittedDetrendParStart;
                            for (int p = 7; p < maxFittedVars; p++) {
                                if (isFitted[p]) {
                                    detrendFactor[p - 7] = coeffs[fp++];
                                } else if (lockToCenter[curve][p]) {
                                    detrendFactor[p - 7] = priorCenter[p];
                                }
                            }
                        } else if (useNelderMeadChi2ForDetrend) {
                            minimization.removeConstraints();
                            start = new double[detrendVars.length];
                            step = new double[detrendVars.length];
                            for (int i = 0; i < start.length; i++) {
                                start[i] = 0.0;
                                step[i] = 1.0;
                            }
                            double fTol = 1e-10;
                            int nMax = 20000;
                            minimization.nelderMead(new FitDetrendChi2(detrendY, detrendYE, detrendVars), start, step, fTol, nMax);
                            coeffs = minimization.getParamValues();

                            varCount = 0;
                            for (int v = 0; v < maxDetrendVars; v++) {
                                if (detrendIndex[v] != 0 && detrendYDNotConstant[v]) {
                                    detrendFactor[v] = coeffs[varCount];
                                    varCount++;
                                }
                            }
                        } else {  //use regression
                            Regression regression = new Regression(detrendVars, detrendY);
                            regression.linear();
                            coeffs = regression.getCoeff();

                            varCount = 1;
                            for (int v = 0; v < maxDetrendVars; v++) {
                                if (detrendIndex[v] != 0 && detrendYDNotConstant[v]) {
                                    detrendFactor[v] = coeffs[varCount];
                                    varCount++;
                                }
                            }
                        }


                        if (detrendFitIndex[curve] == 9 && useTransitFit[curve]) {
                            createDetrendModel = false;
                            xModel1 = detrendX;
                            /*int xModel2Len = plotSizeX + 1;
                            double xModel2Step = ((useDMarker4 && fitMax[curve] < xPlotMax ? fitMax[curve] : xPlotMax) - (useDMarker1 && fitMin[curve] > xPlotMin ? fitMin[curve] : xPlotMin)) / (xModel2Len - 1);
                            xModel2 = new double[xModel2Len];
                            xModel2[0] = useDMarker1 && fitMin[curve] > xPlotMin ? fitMin[curve] : xPlotMin;
                            for (int i = 1; i < xModel2Len; i++) {
                                xModel2[i] = xModel2[i - 1] + xModel2Step;
                            }*/


                            yModel1 = IJU.transitModel(xModel1, bestFit[0], bestFit[4], bestFit[1], bestFit[2], bestFit[3], orbitalPeriod[curve], forceCircularOrbit[curve] ? 0.0 : eccentricity[curve], forceCircularOrbit[curve] ? 0.0 : omega[curve], bestFit[5], bestFit[6], useLonAscNode[curve], lonAscNode[curve], true);

                            //yModel2 = IJU.transitModel(xModel2, bestFit[0], bestFit[4], bestFit[1], bestFit[2], bestFit[3], orbitalPeriod[curve], forceCircularOrbit[curve] ? 0.0 : eccentricity[curve], forceCircularOrbit[curve] ? 0.0 : omega[curve], bestFit[5], bestFit[6], useLonAscNode[curve], lonAscNode[curve], true);

                            // f0 = param[curve][0]; // baseline flux
                            // p0 = param[curve][1]; // r_p/r_*
                            // ar = param[curve][2]; // a/r_*
                            // tc = param[curve][3]; //transit center time
                            // incl = param[curve][4];  //inclination
                            // u1 = param[curve][5];
                            // u2 = param[curve][6];
                        } else {
                            createDetrendModel = true;
                        }
                    } else {
                        xModel1 = null;
                        xModel2 = null;
                        yModel1 = null;
                        yModel2 = null;
                    }

                } else {
                    xModel1 = null;
                    xModel2 = null;
                    yModel1 = null;
                    yModel2 = null;
                }
            } else {
                for (int j = 0; j < nn[curve]; j++) {
                    boolean noNaNs = true;
                    if (!Double.isNaN(y[j])) {
                        for (int v = 0; v < maxDetrendVars; v++) {
                            if (detrendIndex[v] != 0 && Double.isNaN(detrend[v][j])) {
                                noNaNs = false;
                                break;
                            }
                        }
                    } else {
                        noNaNs = false;
                    }
                    if (noNaNs) {
                        yAverage += y[j];
                        avgCount++;
                    }
                }
                if (avgCount > 0) {
                    yAverage /= avgCount;
                }
                xModel1 = new double[2];
                xModel1[0] = Math.max(fitMin[curve], xPlotMin);
                xModel1[1] = Math.min(fitMax[curve], xPlotMax);
                xModel2 = null;
                yModel1 = new double[2];
                yModel1[0] = yAverage;
                yModel1[1] = yAverage;
                yModel2 = null;
            }

            if (divideNotSubtract) {
                double trend;
                if (yAverage != 0.0) {
                    for (int j = 0; j < nn[curve]; j++) {
                        trend = yAverage;
                        for (int v = 0; v < maxDetrendVars; v++) {
                            if (detrendIndex[v] != 0 && detrendYDNotConstant[v]) {
                                trend += detrendFactor[v] * (detrend[v][j]);//-detrendAverage[v]);
                            }
                        }
                        trend /= yAverage;
                        if (trend != 0.0) {
                            y[j] /= trend;
                            if (hasErrors[curve] || hasOpErrors[curve]) {
                                yerr[j] /= trend;
                            }
                        }
                    }
                }
            } else {
                double trend;
                for (int j = 0; j < nn[curve]; j++) {
                    trend = 0.0;
                    for (int v = 0; v < maxDetrendVars; v++) {
                        if (detrendIndex[v] != 0 && detrendYDNotConstant[v]) {
                            trend += detrendFactor[v] * (detrend[v][j]);//-detrendAverage[v]);
                        }
                    }
                    if (hasErrors[curve] || hasOpErrors[curve]) {
                        yerr[j] /= (y[j] / (y[j] - trend));
                    }
                    y[j] -= trend;
                }
            }
        }

        var rms = calculateRms(curve, yModel1, yModel1Err, detrendYE, detrendX, x[curve], y, yerr, bestFit, detrendYAverage);
        return new OptimizerResults(rms, bic);
    }

    public static double calculateRms(int curve, double[] yModel1, double[] yModel1Err,
                                      double[] detrendYE, double[] detrendX, double[] x, double[] y,
                                      double[] yerr, double[] bestFit, double detrendYAverage) {
        var residual = MultiPlot_.residual[curve];
        var sigma = 0D;

        if (detrendFitIndex[curve] == 9 && useTransitFit[curve] && yModel1 != null) {
            int cnt = 0;
            int len = yModel1.length;
            residual = new double[len];
            if (hasErrors[curve] || hasOpErrors[curve]) yModel1Err = new double[len];
            for (int j = 0; j < Math.min(nn[curve], x.length); j++) {
                if (cnt < len && !Double.isNaN(x[j]) && !Double.isNaN(y[j]) && x[j] > fitMin[curve] && x[j] < fitMax[curve]) {
                    residual[cnt] = y[j] - yModel1[cnt];
                    if (hasErrors[curve] || hasOpErrors[curve]) yModel1Err[cnt] = yerr[j];
                    cnt++;
                }
            }

            for (double v : residual) {
                sigma += v * v;
            }
            sigma = Math.sqrt(sigma / cnt);
            return sigma / bestFit[0];
        } else {
            int cnt = 0;
            detrendYAverage = 0;
            double y2Ave = 0;
            if (detrendFitIndex[curve] == 4) {
                for (int j = 0; j < Math.min(nn[curve], x.length); j++) {
                    if (!Double.isNaN(y[j]) && ((x[j] > fitMin[curve] && x[j] < fitLeft[curve]) || (x[j] > fitRight[curve] && x[j] < fitMax[curve]))) {
                        y2Ave += y[j] * y[j];
                        detrendYAverage += y[j];
                        cnt++;
                    }
                }

            } else {
                sigma = 0.0;
                for (int j = 0; j < Math.min(nn[curve], x.length); j++) {
                    if (!Double.isNaN(x[j]) && !Double.isNaN(y[j]) && x[j] > fitMin[curve] && x[j] < fitMax[curve]) {
                        if (detrendFitIndex[curve] == 9 && useTransitFit[curve] && residual != null) {
                            sigma += residual[cnt] * residual[cnt];
                        } else {
                            y2Ave += y[j] * y[j];
                        }
                        detrendYAverage += y[j];
                        cnt++;
                    }
                }
            }

            y2Ave /= cnt;
            detrendYAverage /= cnt;

            if (normIndex[curve] != 0) {
                double normMin = (useDMarker1 ? dMarker1Value : Double.NEGATIVE_INFINITY) + xOffset;
                double normMax = (useDMarker4 ? dMarker4Value : Double.POSITIVE_INFINITY) + xOffset;
                double normLeft = dMarker2Value + xOffset;
                double normRight = dMarker3Value + xOffset;

                double normAverage = 0.0;
                double normCount = 0;
                double invVar = 0;
                switch (normIndex[curve]) {
                    case 1: // left of D2
                        normMax = normLeft;
                        break;
                    case 2: // right of D3
                        normMin = normRight;
                        break;
                    case 3: // outside D2 and D3
                        break;
                    case 4: // inside D2 and D3
                        normMin = normLeft;
                        normMax = normRight;
                        break;
                    case 5: // left of D3
                        normMax = normRight;
                        break;
                    case 6: // right of D2
                        normMin = normLeft;
                        break;
                    case 7: // use all data
                        break;
                    default:
                        normIndex[curve] = 0;
                        normMax = normMin;
                        break;
                }
                if (detrendFitIndex[curve] == 9 && useTransitFit[curve] && detrendX != null && detrendYE != null && yModel1 != null) {
                    int nnn = detrendX.length;
                    if (normIndex[curve] == 3) {
                        for (int j = 0; j < nnn; j++) {
                            if (!Double.isNaN(yModel1[j]) && !Double.isNaN(detrendX[j]) && ((detrendX[j] > normMin && detrendX[j] < normLeft) || (detrendX[j] > normRight && detrendX[j] < normMax))) {
                                invVar = 1 / (detrendYE[j] * detrendYE[j]);
                                normAverage += yModel1[j] * invVar;
                                normCount += invVar;
                            }
                        }
                    } else {
                        for (int j = 0; j < nnn; j++) {
                            if (!Double.isNaN(yModel1[j]) && !Double.isNaN(detrendX[j]) && detrendX[j] > normMin && detrendX[j] < normMax) {
                                invVar = 1 / (detrendYE[j] * detrendYE[j]);
                                normAverage += yModel1[j] * invVar;
                                normCount += invVar;
                            }
                        }
                    }

                    if (normCount == 0) {
                        normAverage = 0.0;
                        for (int j = 0; j < yModel1.length; j++) {
                            if (!Double.isNaN(yModel1[j]) && !Double.isNaN(detrendX[j])) {
                                invVar = 1 / (detrendYE[j] * detrendYE[j]);
                                normAverage += yModel1[j] * invVar;
                                normCount += invVar;
                            }
                        }
                    }
                } else if (useNelderMeadChi2ForDetrend) {
                    if (normIndex[curve] == 3) {
                        for (int j = 0; j < Math.min(nn[curve], x.length); j++) {
                            if (!Double.isNaN(y[j]) && !Double.isNaN(x[j]) && ((x[j] > normMin && x[j] < normLeft) || (x[j] > normRight && x[j] < normMax))) {
                                invVar = 1 / (yerr[j] * yerr[j]);
                                normAverage += y[j] * invVar;
                                normCount += invVar;
                            }
                        }
                    } else {
                        for (int j = 0; j < Math.min(nn[curve], x.length); j++) {
                            if (!Double.isNaN(y[j]) && !Double.isNaN(x[j]) && x[j] > normMin && x[j] < normMax) {
                                invVar = 1 / (yerr[j] * yerr[j]);
                                normAverage += y[j] * invVar;
                                normCount += invVar;
                            }
                        }
                    }

                    if (normCount == 0) {
                        normAverage = 0.0;
                        for (int j = 0; j < Math.min(nn[curve], x.length); j++) {
                            if (!Double.isNaN(y[j]) && !Double.isNaN(x[j])) {
                                invVar = 1 / (yerr[j] * yerr[j]);
                                normAverage += y[j] * invVar;
                                normCount += invVar;
                            }
                        }
                    }
                } else {
                    if (normIndex[curve] == 3) {
                        for (int j = 0; j < Math.min(nn[curve], x.length); j++) {
                            if (!Double.isNaN(y[j]) && !Double.isNaN(x[j]) && ((x[j] > normMin && x[j] < normLeft) || (x[j] > normRight && x[j] < normMax))) {
                                normAverage += y[j];
                                normCount += 1.0;
                            }
                        }
                    } else {
                        for (int j = 0; j < Math.min(nn[curve], x.length); j++) {
                            if (!Double.isNaN(y[j]) && !Double.isNaN(x[j]) && x[j] > normMin && x[j] < normMax) {
                                normAverage += y[j];
                                normCount += 1.0;
                            }
                        }
                    }

                    if (normCount == 0) {
                        normAverage = 0.0;
                        for (int j = 0; j < Math.min(nn[curve], x.length); j++) {
                            if (!Double.isNaN(y[j]) && !Double.isNaN(x[j])) {
                                normAverage += y[j];
                                normCount += 1.0;
                            }
                        }
                    }
                }

                if (normAverage == 0.0 || normCount == 0) {
                    normAverage = 1.0;
                } else {
                    normAverage /= normCount;
                }

                sigma = Math.sqrt(y2Ave - detrendYAverage * detrendYAverage);
                sigma /= normAverage;

                return sigma;
            }
        }

        return Double.NaN;
    }

    enum ParamType {
        REL_FLUX_SNR(FluxData::snr),
        TOT_C_CNTS(FluxData::totalCCounts),
        TOT_C_ERR(FluxData::totalCCountErr),
        REL_FLUX(FluxData::flux),
        REL_FLUX_ERR(FluxData::err);

        private final Pattern pattern;
        private final Function<FluxData, double[]> dataGetter;

        ParamType(Function<FluxData, double[]> dataGetter) {
            var name = this.name().toLowerCase(Locale.ENGLISH);
            var patternString = name + (name.startsWith("tot_c_") ? "" : "_" + "[ct]([0-9]+)");
            pattern = Pattern.compile(patternString);
            this.dataGetter = dataGetter;
        }

        public Integer getAperture(String detrendLabel) {
            detrendLabel = detrendLabel.toLowerCase(Locale.ENGLISH);
            var match = pattern.matcher(detrendLabel);
            try {
                if (match.matches()) {
                    return Integer.parseInt(match.groupCount() == 1 ? match.group(1) : match.group()) - 1;
                }
            } catch (NumberFormatException ignored) {
            }

            return null;
        }

        public boolean matches(String detrendLabel) {
            return pattern.matcher(detrendLabel.toLowerCase(Locale.ENGLISH)).matches();
        }

        public double[] getData(FluxData data) {
            return dataGetter.apply(data);
        }
    }

    private record FluxData(double[] flux, double[] err, double[] snr, double[] totalCCounts, double[] totalCCountErr) {
    }

    record CurveData(double[] rel_flux, double[] err, HashMap<ColumnInfo, double[]> instancedParamData) {
    }

    record ColumnInfo(ParamType type, int ap, int detrendColumn) {
    }

    public record OptimizerResults(double rms, double bic) {
        public OptimizerResults(double rms, double bic) {
            this.rms = round(rms);
            this.bic = bic;
        }

        private static double round(double val) {
            var valo = val;
            if (Double.isFinite(val)) {
                BigDecimal bd = new BigDecimal(Double.toString(val * 1000));
                valo = bd.setScale(6, RoundingMode.HALF_UP).doubleValue();
            }
            return valo;
        }

        @Override
        public String toString() {
            var rmso = rms;
            var bico = bic;
            if (Double.isFinite(rms)) {
                BigDecimal bd = new BigDecimal(Double.toString(rms));
                rmso = bd.setScale(6, RoundingMode.HALF_UP).doubleValue();
            }
            if (Double.isFinite(bic)) {
                BigDecimal bdb = new BigDecimal(Double.toString(bic));
                bico = bdb.setScale(4, RoundingMode.HALF_UP).doubleValue();
            }

            return "OptimizerResults{" +
                    "rms=" + rmso +
                    ", bic=" + bico +
                    '}';
        }
    }

    public class FitLightCurveChi2 implements MinimizationFunction {
        double[] detrendY;
        double dof;
        double chi2;
        double bp;
        double[] detrendX, detrendYE, priorCenter, lcModel;
        boolean[] isFitted;
        double detrendYAverage;
        int[] detrendIndex;
        int maxFittedVars;
        double[][] detrendVars;

        public FitLightCurveChi2(double[] detrendY, double dof, double bp, double[] detrendX, double[] detrendYE, boolean[] isFitted, double detrendYAverage, double[] priorCenter, int[] detrendIndex, int maxFittedVars, double[][] detrendVars) {
            this.detrendY = detrendY;
            this.dof = dof;
            this.bp = bp;
            this.detrendX = detrendX;
            this.detrendYE = detrendYE;
            this.isFitted = isFitted;
            this.detrendYAverage = detrendYAverage;
            this.priorCenter = priorCenter;
            this.detrendIndex = detrendIndex;
            this.maxFittedVars = maxFittedVars;
            this.detrendVars = detrendVars;
        }

        public double function(double[] param) {
            int numData = detrendY.length;
            int numDetrendVars = detrendVars.length;
            int nPars = param.length;
            double[] dPars = new double[detrendVars.length];
            if (dof < 1) dof = 1;

            chi2 = 0;
            double residual;
            int fp = 0;

            double f0 = priorCenter[0]; // baseline flux
            double p0 = priorCenter[1]; // r_p/r_*
            double ar = priorCenter[2]; // a/r_*
            double tc = priorCenter[3]; //transit center time
            double incl = priorCenter[4];  //inclination
            double u1 = priorCenter[5];  //quadratic limb darkening parameter 1
            double u2 = priorCenter[6];  //quadratic limb darkening parameter 2
            double e = forceCircularOrbit[curve] ? 0.0 : eccentricity[curve];
            double ohm = forceCircularOrbit[curve] ? 0.0 : omega[curve];
            double b = 0.0;
            if (useTransitFit[curve]) {
                f0 = lockToCenter[curve][0] ? priorCenter[0] : param[fp < nPars ? fp++ : nPars - 1]; // baseline flux
                p0 = lockToCenter[curve][1] ? Math.sqrt(priorCenter[1]) : param[fp < nPars ? fp++ : nPars - 1]; // r_p/r_*
                ar = lockToCenter[curve][2] ? priorCenter[2] : param[fp < nPars ? fp++ : nPars - 1]; // a/r_*
                tc = lockToCenter[curve][3] ? priorCenter[3] : param[fp < nPars ? fp++ : nPars - 1]; //transit center time
                if (!bpLock[curve]) {
                    incl = lockToCenter[curve][4] ? priorCenter[4] * Math.PI / 180.0 : param[fp < nPars ? fp++ : nPars - 1];  //inclination
                    b = Math.cos(incl) * ar;
                    if (b > 1.0 + p0) {  //ensure planet transits or grazes the star
                        return Double.POSITIVE_INFINITY;
                    }
                } else {
                    incl = Math.acos(bp / ar);
                }
                u1 = lockToCenter[curve][5] ? priorCenter[5] : param[fp < nPars ? fp++ : nPars - 1];  //quadratic limb darkening parameter 1
                u2 = lockToCenter[curve][6] ? priorCenter[6] : param[fp < nPars ? fp++ : nPars - 1];  //quadratic limb darkening parameter 2

                lcModel = IJU.transitModel(detrendX, f0, incl, p0, ar, tc, orbitalPeriod[curve], e, ohm, u1, u2, useLonAscNode[curve], lonAscNode[curve], true);
            }

            int dp = 0;
            for (int p = 7; p < maxFittedVars; p++) {
                if (isFitted[p]) {
                    dPars[dp++] = param[fp++];
                } else if (detrendIndex[p - 7] != 0 && detrendYDNotConstant[p - 7] && lockToCenter[curve][p]) {
                    dPars[dp++] = priorCenter[p];
                }
            }


            if (useTransitFit[curve]) {
                if (!lockToCenter[curve][2] && (ar < (1.0 + p0))) {
                    chi2 = Double.POSITIVE_INFINITY;  //boundary check that planet does not orbit within star
                } else if ((!lockToCenter[curve][2] || !lockToCenter[curve][4]) && ((ar * Math.cos(incl) * (1.0 - e * e) / (1.0 + e * Math.sin(ohm * Math.PI / 180.0))) >= 1.0 + p0)) {
                    if (!lockToCenter[curve][4] && autoUpdatePrior[curve][4]) {
                        priorCenter[4] = Math.round(10.0 * Math.acos((0.5 + p0) * (1.0 + e * Math.sin(ohm * Math.PI / 180.0)) / (ar * (1.0 - e * e))) * 180.0 / Math.PI) / 10.0;
                        if (Double.isNaN(priorCenter[4])) priorCenter[4] = 89.9;
                        //priorCenterSpinner[curve][4].setValue(priorCenter[4]);
                    }
                    chi2 = Double.POSITIVE_INFINITY; //boundary check that planet passes in front of star
                } else if ((!lockToCenter[curve][5] || !lockToCenter[curve][6]) && (((u1 + u2) > 1.0) || ((u1 + u2) < 0.0) || (u1 > 1.0) || (u1 < 0.0) || (u2 < -1.0) || (u2 > 1.0))) {
                    chi2 = Double.POSITIVE_INFINITY;
                } else {
                    for (int j = 0; j < numData; j++) {
                        residual = detrendY[j];// - param[0];
                        for (int i = 0; i < numDetrendVars; i++) {
                            residual -= detrendVars[i][j] * dPars[i];
                        }
                        residual -= (lcModel[j] - detrendYAverage);
                        chi2 += ((residual * residual) / (detrendYE[j] * detrendYE[j]));
                    }
                }
            } else {
                for (int j = 0; j < numData; j++) {
                    residual = detrendY[j];// - param[0];
                    for (int i = 0; i < numDetrendVars; i++) {
                        residual -= detrendVars[i][j] * dPars[i];
                    }
                    if (numDetrendVars == 0 && param.length == 1) {
                        residual -= param[0];
                    }
                    chi2 += ((residual * residual) / (detrendYE[j] * detrendYE[j]));
                }
            }
            return chi2 / dof;
        }
    }

    public class FitDetrendOnly implements MinimizationFunction {
        double[] detrendY;
        double[][] detrendVars;

        public FitDetrendOnly(double[] detrendY, double[][] detrendVars) {
            this.detrendY = detrendY;
            this.detrendVars = detrendVars;
        }

        public double function(double[] param) {
            double sd = 0.0;
            double residual;
            int numData = detrendY.length;
            int numVars = detrendVars.length;
            for (int j = 0; j < numData; j++) {
                residual = detrendY[j] - param[0];
                for (int i = 0; i < numVars; i++) {
                    residual -= detrendVars[i][j] * param[i + 1];
                }
                sd += residual * residual;
            }
            return Math.sqrt(sd / (double) numData);
        }
    }

    public class FitDetrendChi2 implements MinimizationFunction {
        double[] detrendY, detrendYE;
        double[][] detrendVars;

        public FitDetrendChi2(double[] detrendY, double[] detrendYE, double[][] detrendVars) {
            this.detrendY = detrendY;
            this.detrendYE = detrendYE;
            this.detrendVars = detrendVars;
        }

        public double function(double[] param) {
            double chi2 = 0.0;
            double residual;
            int numData = detrendY.length;
            int numVars = detrendVars.length;
            int dof = numData - param.length;
            if (dof < 1) dof = 1;
            for (int j = 0; j < numData; j++) {
                residual = detrendY[j];// - param[0];
                for (int i = 0; i < numVars; i++) {
                    residual -= detrendVars[i][j] * param[i];
                }
                chi2 += ((residual * residual) / (detrendYE[j] * detrendYE[j]));
            }
            return chi2 / (double) dof;
        }
    }

    public static class MPOperatorError extends RuntimeException {
        public MPOperatorError() {
            super("Operator must be none");
        }
    }
}
