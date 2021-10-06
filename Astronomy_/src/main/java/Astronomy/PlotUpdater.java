package Astronomy;

import astroj.IJU;
import flanagan.analysis.Regression;
import flanagan.analysis.Smooth;
import flanagan.math.Minimization;
import flanagan.math.MinimizationFunction;
import ij.IJ;
import ij.Prefs;
import ij.astro.logging.AIJLogger;
import ij.measure.ResultsTable;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Arrays;

import static Astronomy.MultiPlot_.*;

/**
 * Extracted version of {@link MultiPlot_#updatePlot(boolean[], boolean)} for the multithreaded optimizer.
 */
public class PlotUpdater {
    private static PlotUpdater INSTANCE;
    private int curve;
    private int targetStar;
    private boolean[] refStarEnabled;
    private boolean updateFit = true;
    private int initAvgCount;
    private boolean initAtLeastOne;
    private int initDetrendCount;
    private int initBaselineCount;
    private int initDepthCount;
    private int[] initDetrendVarsUsed;
    private double[] initDetrendY;
    private boolean initDetrendYNotConstant;
    private double[] initDetrendYE;
    private double[][] initDetrendYD;
    private double[] initDetrendX;

    double[][] source;
    double[][] srcvar;
    double[][] total;
    double[][] totvar;

    boolean[] localIsRefStar;

    //private final Minimization minimization = new Minimization();
    ThreadLocal<Minimization> minimizationThreadLocal = ThreadLocal.withInitial(Minimization::new);

    // Chunk 1 stuff here (shared between all threads)
    private PlotUpdater(int curve, int targetStar) {
        this.curve = curve;
        this.targetStar = targetStar;
        getStarData();
        setupData();
        conditionData();
        //todo add stuff
        // add check for fitter running only on Tstar and other conditions
        // //todo extract star and filter from this "rel_flux_Txx"
    }

    public synchronized static PlotUpdater getInstance(int curve, int targetStar) {
        if (INSTANCE == null || INSTANCE.curve != curve || INSTANCE.targetStar != targetStar)
            INSTANCE = new PlotUpdater(curve, targetStar);
        return INSTANCE;
    }

    public synchronized static void invalidateInstance() {
        INSTANCE = null;
    }

    public synchronized OptimizerResults fitCurveAndGetResults(boolean[] isRefStar) {
        localIsRefStar = isRefStar;
        return updateCurve();
    }

    private synchronized void getStarData() {
        if (table == null) return;
        int numRows = table.getCounter();
        if (numRows < 1) return;
        boolean goodErrData = true;
        source = new double[numAps][numRows];
        srcvar = new double[numAps][numRows];
        total = new double[numAps][numRows];//todo only need 1 per row, don't make here
        totvar = new double[numAps][numRows];
        for (int ap = 0; ap < numAps; ap++) {
            for (int row = 0; row < numRows; row++) {
                source[ap][row] = 0.0;
                total[ap][row] = 0.0;
                srcvar[ap][row] = 0.0;
                totvar[ap][row] = 0.0;
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
            if (binSize[curve] < 1) {
                binSize[curve] = 1;
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

        nn[curve] = (n - excluded) / binSize[curve];
        nnr[curve] = (n - excluded) % binSize[curve];
        if (nnr[curve] > 0) nn[curve]++;
        detrendVarsUsed[curve] = 0;

        if (xlabel[curve].trim().length() == 0 || (xlabel[curve].equalsIgnoreCase("default") && xlabeldefault.trim().length() == 0)) {
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
                int bucketSize = binSize[curve];

                for (int j = 0; j < nn[curve]; j++) {
                    double xin;
                    int numNaN = 0;
                    if (nnr[curve] > 0 && j == nn[curve] - 1) {
                        bucketSize = nnr[curve];
                    } else {
                        bucketSize = binSize[curve];
                    }
                    x[curve][j] = 0;
                    for (int k = excludedHeadSamples; k < (bucketSize + excludedHeadSamples); k++) {
                        xin = table.getValueAsDouble(xcolumn[curve], j * binSize[curve] + k);
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
            if (ylabel[curve].trim().length() == 0) {
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
                        if (detrendIndex[curve][v] > 1) {
                            detrendcolumn[curve][v] = table.getColumnIndex(detrendlabel[curve][v]);

                            if (detrendcolumn[curve][v] == ResultsTable.COLUMN_NOT_FOUND) {
                                detrendIndex[curve][v] = 0;
                            } else {
                                detrendVarsUsed[curve]++;
                            }
                        } else if (detrendIndex[curve][v] == 1)  //Meridian Flip Detrend Selected
                        {
                            detrendVarsUsed[curve]++;
                        }
                    }
                }

                errcolumn[curve] = ResultsTable.COLUMN_NOT_FOUND;
                if (operatorIndex[curve] == 6) {  //custom error
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

                if (operatorIndex[curve] != 0) {
                    opcolumn[curve] = table.getColumnIndex(oplabel[curve]);
                    if (opcolumn[curve] == ResultsTable.COLUMN_NOT_FOUND) {
                        operatorIndex[curve] = 0;
                    }
                }

                if (operatorIndex[curve] > 0 && operatorIndex[curve] < 5)// && showErrors[curve] == true)
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

                int bucketSize = binSize[curve];
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
                    // BIN DATA IF APPLICABLE
                    if (nnr[curve] > 0 && j == nn[curve] - 1) {
                        bucketSize = nnr[curve];
                    } else {
                        bucketSize = binSize[curve];
                    }
                    for (int k = excludedHeadSamples; k < (bucketSize + excludedHeadSamples); k++) {
                        yin = table.getValueAsDouble(ycolumn[curve], j * binSize[curve] + k);
                        if (Double.isNaN(yin)) {
                            numNaN += 1;
                            if (numNaN == bucketSize) {
                                bucketSize = 1;
                                y[curve][j] = Double.NaN;
                                if (hasErrors[curve]) yerr[curve][j] = Double.NaN;
                                if (operatorIndex[curve] != 0) yop[curve][j] = Double.NaN;
                                if (detrendFitIndex[curve] != 0) {
                                    for (int v = 0; v < maxDetrendVars; v++) {
                                        if (detrendIndex[curve][v] != 0) {
                                            detrend[curve][v][j] = Double.NaN;
                                        }
                                    }
                                }

                                if (operatorIndex[curve] == 5) //calculate distance
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
                                errin = table.getValueAsDouble(errcolumn[curve], j * binSize[curve] + k);
                                if (fromMag[curve]) {
                                    errin = yin * (-Math.pow(10, -errin / 2.5) + 1);
                                }
                                yerr[curve][j] += errin * errin;
                            }
                            if (operatorIndex[curve] != 0) {
                                opin = table.getValueAsDouble(opcolumn[curve], j * binSize[curve] + k);
                                if (fromMag[curve]) {
                                    opin = Math.pow(10, -opin / 2.5);
                                }
                                yop[curve][j] += opin;
                            }
                            if (detrendFitIndex[curve] != 0) {
                                for (int v = 0; v < maxDetrendVars; v++) {
                                    if (detrendIndex[curve][v] > 1) {
                                        detrend[curve][v][j] += table.getValueAsDouble(detrendcolumn[curve][v], j * binSize[curve] + k);
                                    }
                                }
                            }
                            if (operatorIndex[curve] == 5) //calculate distance
                            {
                                xc1[curve][j] += table.getValueAsDouble(xc1column[curve], j * binSize[curve] + k);
                                xc2[curve][j] += table.getValueAsDouble(xc2column[curve], j * binSize[curve] + k);
                                yc1[curve][j] += table.getValueAsDouble(yc1column[curve], j * binSize[curve] + k);
                                yc2[curve][j] += table.getValueAsDouble(yc2column[curve], j * binSize[curve] + k);
                            }
                            if (hasOpErrors[curve]) {
                                operrin = table.getValueAsDouble(operrcolumn[curve], j * binSize[curve] + k);
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
                            if (detrendIndex[curve][v] != 0) {
                                detrend[curve][v][j] /= bucketSize;
                            }
                        }
                    }
                    if (operatorIndex[curve] != 0) {
                        yop[curve][j] = yop[curve][j] / bucketSize; //*yMultiplierFactor
                        if (operatorIndex[curve] == 5) {
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

                    if (operatorIndex[curve] == 0)  //no operator
                    {

                    } else if (operatorIndex[curve] == 1)  //divide by
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
                    } else if (operatorIndex[curve] == 2)  //multiply by
                    {
                        if (hasErrors[curve] || hasOpErrors[curve]) {
                            yerr[curve][j] = Math.sqrt(yop[curve][j] * yop[curve][j] * yerr[curve][j] * yerr[curve][j] + y[curve][j] * y[curve][j] * yoperr[curve][j] * yoperr[curve][j]); // /yMultiplierFactor;
                        }
                        y[curve][j] = y[curve][j] * yop[curve][j];  // /yMultiplierFactor;
                    } else if (operatorIndex[curve] == 3)  //subtract
                    {
                        if (hasErrors[curve] || hasOpErrors[curve]) {
                            yerr[curve][j] = Math.sqrt(yerr[curve][j] * yerr[curve][j] + yoperr[curve][j] * yoperr[curve][j]);
                        }
                        y[curve][j] = y[curve][j] - yop[curve][j];
                    } else if (operatorIndex[curve] == 4)  //add
                    {
                        if (hasErrors[curve] || hasOpErrors[curve]) {
                            yerr[curve][j] = Math.sqrt(yerr[curve][j] * yerr[curve][j] + yoperr[curve][j] * yoperr[curve][j]);
                        }
                        y[curve][j] = y[curve][j] + yop[curve][j];
                    } else if (operatorIndex[curve] == 5)  //distance from x1,y1 to x2,y2
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

        if (plotY[curve] && smooth[curve] && nn[curve] > 2 * smoothLen[curve]) {
            double[] xl = new double[nn[curve]];
            double[] yl = new double[nn[curve]];
            double[] xphase = new double[nn[curve]];
            double[] yphase = new double[nn[curve]];
            double xfold;
            int nskipped = 0;
            double xmax = Double.NEGATIVE_INFINITY;
            double xmin = Double.POSITIVE_INFINITY;
            double halfPeriod = netPeriod / 2.0;
            for (int xx = 0; xx < nn[curve]; xx++) {
                if (false) {//showXAxisNormal
                    yl[xx] = y[curve][xx];
                    xl[xx] = x[curve][xx] - (int) x[curve][0];
                } else {
                    xfold = ((x[curve][xx] - netT0) % netPeriod);
                    if (xfold > halfPeriod) {
                        xfold -= netPeriod;
                    } else if (xfold < -halfPeriod) xfold += netPeriod;
                    if (Math.abs(xfold) < duration / 48.0) {
                        nskipped++;
                    } else {
                        yphase[xx - nskipped] = y[curve][xx];
                        xphase[xx - nskipped] = x[curve][xx] - (int) x[curve][0];
                        if (x[curve][xx] > xmax) xmax = x[curve][xx];
                        if (x[curve][xx] < xmin) xmin = x[curve][xx];
                    }
                }
            }
            if (true) { //!showXAxisNormal
                xl = new double[nn[curve] - nskipped];
                yl = new double[nn[curve] - nskipped];
                for (int xx = 0; xx < nn[curve] - nskipped; xx++) {
                    yl[xx] = yphase[xx];
                    xl[xx] = xphase[xx];
                }
            }
            if (nn[curve] - nskipped > 2 * smoothLen[curve]) {
                double smoothVal;
                Smooth csm = new Smooth(xl, yl);
                csm.savitzkyGolay(smoothLen[curve]);
                csm.setSGpolyDegree(2);
                double yave = 0.0;
                for (int xx = 0; xx < nn[curve] - nskipped; xx++) {
                    yave += yl[xx];
                }
                yave /= (nn[curve] - nskipped);
                for (int xx = 0; xx < nn[curve]; xx++) {
                    if (x[curve][xx] > xmax) {
                        smoothVal = csm.interpolateSavitzkyGolay(xmax - (int) x[curve][0]);
                    } else if (x[curve][xx] < xmin) {
                        smoothVal = csm.interpolateSavitzkyGolay(xmin - (int) x[curve][0]);
                    } else {
                        smoothVal = csm.interpolateSavitzkyGolay(x[curve][xx] - (int) x[curve][0]);
                    }
                    y[curve][xx] = y[curve][xx] - smoothVal + yave;
                }
            }
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
                    if (detrendIndex[curve][v] != 0) {
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
                            if (detrendIndex[curve][u] == detrendIndex[curve][v]) detrendPower[v]++;
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
                    if (detrendIndex[curve][v] == 1)    //Meridian Flip Detrend Selected
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
                if (detrendFitIndex[curve] != 1) {
                    double[][] detrendYD = new double[maxDetrendVars][nn[curve]];
                    boolean noNaNs = true;
                    if (detrendFitIndex[curve] == 4) {
                        for (int j = 0; j < nn[curve]; j++) {
                            noNaNs = true;
                            if (!Double.isNaN(y[curve][j])) {
                                for (int v = 0; v < maxDetrendVars; v++) {
                                    if (detrendIndex[curve][v] != 0 && Double.isNaN(detrend[curve][v][j])) {
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
                                        if (detrendYD[v][0] != detrendYD[v][detrendCount]) {
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
                                if (detrendIndex[curve][v] > 0) {
                                    detrendFactor[curve][v] = 0.0;
                                }
                            }
                        }
                    } else {
                        for (int j = 0; j < nn[curve]; j++) {
                            noNaNs = true;
                            if (!Double.isNaN(y[curve][j])) {
                                for (int v = 0; v < maxDetrendVars; v++) {
                                    if (detrendIndex[curve][v] != 0 && Double.isNaN(detrend[curve][v][j])) {
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
                                            if (detrendYD[v][0] != detrendYD[v][detrendCount]) {
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
                                if (detrendIndex[curve][v] > 0) {
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

    // RMS/BIC aren't exact matches with this, but close enough
    private void conditionData() {
        for (int ap = 0; ap < source.length; ap++) {
            int bucketSize = 0;
            var workingSource = new double[nn[curve]];
            var workingSrcvar = new double[nn[curve]];
            if (excludedHeadSamples + excludedTailSamples >= n) { //Handle case for more samples excluded than in dataset
                excludedHeadSamples = excludedHeadSamples < n ? excludedHeadSamples : n - 1;
                excludedTailSamples = n - excludedHeadSamples - 1;
            }
            for (int j = 0; j < nn[curve]; j++) {
                if (nnr[curve] > 0 && j == nn[curve] - 1) {
                    bucketSize = nnr[curve];
                } else {
                    bucketSize = binSize[curve];
                }
                for (int k = excludedHeadSamples; k < (bucketSize + excludedHeadSamples); k++) {
                    var i = j * binSize[curve] + k;
                    workingSource[j] += source[ap][i];
                    workingSrcvar[j] += srcvar[ap][i];
                }
                workingSource[j] /= bucketSize;
                workingSrcvar[j] /= bucketSize;
            }

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
            var workingSrcvar2 = new double[nn[curve]];

            //todo check on Source-Sky, Source_Error values, and detrend parameters for NaNs. If a NaN is found, bail out and give an appropriate error.
            for (int j = 0; j < nn[curve]; j++) {
                if (detrendFitIndex[curve] != 1) {
                    if (detrendFitIndex[curve] == 4) {
                        if ((x[curve][j] > fitMin[curve] && x[curve][j] < fitLeft[curve]) || (x[curve][j] > fitRight[curve] && x[curve][j] < fitMax[curve])) {
                            workingSource2[j] = workingSource[j];
                            workingSrcvar2[j] = workingSrcvar[j];
                        }
                    } else {
                        if (x[curve][j] > fitMin[curve]) {
                            if (x[curve][j] < fitMax[curve]) {
                                workingSource2[j] = workingSource[j];
                                workingSrcvar2[j] = workingSrcvar[j];
                            }
                        }
                    }
                }
            }

            source[ap] = workingSource2;
            srcvar[ap] = workingSrcvar2;
        }
    }

    private synchronized OptimizerResults updateCurve() {
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
        var total = Arrays.copyOf(this.total, this.total.length);
        var totvar = Arrays.copyOf(this.totvar, this.totvar.length);
        double[] y = new double[detrendXs[curve].length];
        var yAverage = MultiPlot_.yAverage[curve];
        var yDepthEstimate = MultiPlot_.yDepthEstimate[curve];
        var yBaselineAverage = MultiPlot_.yBaselineAverage[curve];
        var detrendYAverage = MultiPlot_.detrendYAverage[curve];
        double[] start;
        double[] width;
        double[] step;
        var dof = MultiPlot_.dof[curve];
        int[] index;
        boolean[] isFitted = Arrays.copyOf(MultiPlot_.isFitted[curve], MultiPlot_.isFitted[curve].length);
        double[] coeffs = Arrays.copyOf(MultiPlot_.coeffs[curve], MultiPlot_.coeffs[curve].length);
        var detrendVars = Arrays.copyOf(MultiPlot_.detrendVars, MultiPlot_.detrendVars.length);
        var xModel1 = Arrays.copyOf(MultiPlot_.xModel1[curve], MultiPlot_.xModel1[curve].length);
        var xModel2 = MultiPlot_.xModel2[curve];
        var yModel1 = Arrays.copyOf(MultiPlot_.yModel1[curve], MultiPlot_.yModel1[curve].length);
        var yModel2 = Arrays.copyOf(MultiPlot_.yModel2[curve], MultiPlot_.yModel2[curve].length);
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

        if (detrendFitIndex[curve] != 0) {
            for (int v = 0; v < maxDetrendVars; v++) {
                if (detrendIndex[curve][v] != 0) {
                    atLeastOne = true;
                    break;
                }
            }
        }

        if (operatorIndex[curve] != 0) {
            IJ.error("Operator must be 0");
            return new OptimizerResults(Double.NaN, Double.NaN);
        }

        if (!plotY[curve]) return new OptimizerResults(Double.NaN, Double.NaN);

        for (int i= 0; i < detrendXs[curve].length; i++) {
            var compSum = 0.0;
            var compVar = 0.0;
            for (int ap = 0; ap < localIsRefStar.length; ap++) {
                if (localIsRefStar[ap] && targetStar != ap) {
                    compSum += source[ap][i];
                    compVar += srcvar[ap][i];
                }
            }
            total[curve][i] = compSum;
            totvar[targetStar][i] = compVar;
        }


        for (int i= 0; i < detrendXs[curve].length; i++) {
            y[i] = total[curve][i] == 0 ? Double.NaN : source[targetStar][i] / total[curve][i];
            if (source[curve][i] == 0 || total[curve][i] == 0) {
                detrendY[i] = Double.POSITIVE_INFINITY;
            } else {
                detrendYE[i] = hasErrors[curve] || hasOpErrors[curve] ? (y[i] * Math.sqrt(srcvar[targetStar][i] / (source[targetStar][i] * source[targetStar][i]) + totvar[targetStar][i] / (total[curve][i] * total[curve][i]))) : 1;
            }
            yerr = detrendYE;
            detrendY[i] = y[i];
        }

        if (atLeastOne || detrendFitIndex[curve] == 9) {
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
                        if (detrendIndex[curve][v] != 0 && !detrendYDNotConstant[v] && detrendVarsUsed[curve] > 0) {
                            detrendVarsUsed[curve]--;
                        }
                    }
                    detrendVars = new double[detrendVarsUsed[curve]][detrendCount];
                    int varCount = 0;
                    for (int v = 0; v < maxDetrendVars; v++) {
                        if (detrendIndex[curve][v] != 0 && detrendYDNotConstant[v]) {
                            detrendVars[varCount] = Arrays.copyOf(detrendYD[v], detrendCount);
                            varCount++;
                        }
                    }

                    // Update prior centers/widths //todo mark
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
                        priorCenter[4] = (180.0/Math.PI)*Math.acos(bp1/bestFit[2]);
                    }
                    // End update
                    AIJLogger.log(priorCenter);
                    AIJLogger.log(MultiPlot_.priorCenter[curve]);
                    
                    if ((detrendVarsUsed[curve] > 0 || detrendFitIndex[curve] == 9 && useTransitFit[curve]) && detrendYNotConstant && detrendCount > (detrendFitIndex[curve] == 9 && useTransitFit[curve] ? 7 : 0) + detrendVarsUsed[curve] + 2) { //need enough data to provide degrees of freedom for successful fit
                        detrendX = Arrays.copyOf(detrendX, detrendCount);
                        detrendY = Arrays.copyOf(detrendY, detrendCount);
                        detrendYE = Arrays.copyOf(detrendYE, detrendCount);

                        if (updateFit) {
                            int fittedDetrendParStart;
                            
                            if (detrendFitIndex[curve] == 9) {
                                minimization.removeConstraints();
                                
                                int nFitted = 0;
                                for (int p = 0; p < 7; p++) {
                                    if (useTransitFit[curve] && !lockToCenter[curve][p]) {
                                        isFitted[p] = true;
                                        nFitted++;
                                    } else {
                                        isFitted[p] = false;
                                    }
                                }
                                
                                fittedDetrendParStart = nFitted;
                                for (int p = 7; p < maxFittedVars; p++) {
                                    if (detrendIndex[curve][p - 7] != 0 && detrendYDNotConstant[p - 7] && !lockToCenter[curve][p]) {
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
                                

                                dof = detrendX.length - start.length;

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
                                        start[fp] = Math.sqrt(priorCenter[1]);//todo check FitLightCurveChi2
                                        width[fp] = Math.sqrt(priorWidth[1]);
                                        step[fp] = Math.sqrt(getFitStep(curve, 1));
                                        minimization.addConstraint(fp, -1, 0.0);
                                    } else if (index[fp] == 4) {
                                        start[fp] = priorCenter[4] * Math.PI / 180.0;  // inclination
                                        width[fp] = priorWidth[4] * Math.PI / 180.0;
                                        step[fp] = getFitStep(curve, 4) * Math.PI / 180.0;
                                        minimization.addConstraint(fp, 1, 90.0 * Math.PI / 180.0);
                                        minimization.addConstraint(fp, -1, 50.0 * Math.PI / 180.0);
                                    } else {
                                        if (index[fp] == 0) minimization.addConstraint(fp, -1, 0.0);
                                        if (index[fp] == 2) minimization.addConstraint(fp, -1, 2.0);
                                        if (index[fp] == 3) minimization.addConstraint(fp, -1, 0.0);
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
                                        step[fp] = getFitStep(curve, index[fp]);
                                    }
                                    if (usePriorWidth[curve][index[fp]]) {
                                        minimization.addConstraint(fp, 1, start[fp] + width[fp]);
                                        minimization.addConstraint(fp, -1, start[fp] - width[fp]);
                                    }
                                }

                                minimization.setNrestartsMax(1);
                                minimization.nelderMead(new FitLightCurveChi2(detrendY, dof, bp, detrendX, detrendYE, isFitted, detrendYAverage, priorCenter),
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
                                    } else if (p >= 7 && p < 7 + maxDetrendVars && detrendIndex[curve][p - 7] != 0 && detrendYDNotConstant[p - 7] && lockToCenter[curve][p]) {
                                        bestFit[p] = priorCenter[p];
                                    } else {
                                        bestFit[p] = Double.NaN;
                                    }
                                }
                                AIJLogger.log(bestFit);
                                /*if (useTransitFit[curve]) {//todo not used in PU, remove
                                    //Winn 2010 eqautions 14, 15, 16
                                    if (!bpLock[curve])
                                        bp = bestFit[2] * Math.cos(bestFit[4]) * (1.0 - (forceCircularOrbit[curve] ? 0.0 : eccentricity[curve] * eccentricity[curve])) / (1.0 + (forceCircularOrbit[curve] ? 0.0 : eccentricity[curve]) * Math.sin(forceCircularOrbit[curve] ? 0.0 : omega[curve]));
                                    double bp2 = bp * bp;
                                    t14[curve] = (orbitalPeriod[curve] / Math.PI * Math.asin(Math.sqrt((1.0 + bestFit[1]) * (1.0 + bestFit[1]) - bp2) / (Math.sin(bestFit[4]) * bestFit[2])) * Math.sqrt(1.0 - (forceCircularOrbit[curve] ? 0.0 : eccentricity[curve] * eccentricity[curve])) / (1.0 + (forceCircularOrbit[curve] ? 0.0 : eccentricity[curve]) * Math.sin(forceCircularOrbit[curve] ? 0.0 : omega[curve])));
                                    t23[curve] = (orbitalPeriod[curve] / Math.PI * Math.asin(Math.sqrt((1.0 - bestFit[1]) * (1.0 - bestFit[1]) - bp2) / (Math.sin(bestFit[4]) * bestFit[2])) * Math.sqrt(1.0 - (forceCircularOrbit[curve] ? 0.0 : eccentricity[curve] * eccentricity[curve])) / (1.0 + (forceCircularOrbit[curve] ? 0.0 : eccentricity[curve]) * Math.sin(forceCircularOrbit[curve] ? 0.0 : omega[curve])));
                                    tau[curve] = (t14[curve] - t23[curve]) / 2.0;
                                    double sin2tTpioP = Math.pow(Math.sin(t14[curve] * Math.PI / orbitalPeriod[curve]), 2);
                                    stellarDensity[curve] = 0.0189 / (orbitalPeriod[curve] * orbitalPeriod[curve]) * Math.pow(((1.0 + bestFit[1]) * (1.0 + bestFit[1]) - bp2 * (1 - sin2tTpioP)) / sin2tTpioP, 1.5);

                                    double midpointFlux = IJU.transitModel(new double[]{bestFit[3]}, bestFit[0], bestFit[4], bestFit[1], bestFit[2], bestFit[3], orbitalPeriod[curve], forceCircularOrbit[curve] ? 0.0 : eccentricity[curve], forceCircularOrbit[curve] ? 0.0 : omega[curve], bestFit[5], bestFit[6], useLonAscNode[curve], lonAscNode[curve])[0];
                                    transitDepth[curve] = (1 - (midpointFlux / bestFit[0])) * 1000;
                                }*/
                                
                                chi2dof = minimization.getMinimum();
                                bic = chi2dof * (detrendX.length - bestFit.length) + bestFit.length * Math.log(detrendX.length);

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
                                minimization.nelderMead(new FitDetrendChi2(detrendY, detrendYE), start, step, fTol, nMax);
                                coeffs = minimization.getParamValues();

                                varCount = 0;
                                for (int v = 0; v < maxDetrendVars; v++) {
                                    if (detrendIndex[curve][v] != 0 && detrendYDNotConstant[v]) {
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
                                    if (detrendIndex[curve][v] != 0 && detrendYDNotConstant[v]) {
                                        detrendFactor[v] = coeffs[varCount];
                                        varCount++;
                                    }
                                }
                            }
                        }
                        

                        if (detrendFitIndex[curve] == 9 && useTransitFit[curve]) {
                            createDetrendModel = false;
                            xModel1 = detrendX;
                            int xModel2Len = plotSizeX + 1;
                            double xModel2Step = ((useDMarker4 && fitMax[curve] < xPlotMax ? fitMax[curve] : xPlotMax) - (useDMarker1 && fitMin[curve] > xPlotMin ? fitMin[curve] : xPlotMin)) / (xModel2Len - 1);
                            xModel2 = new double[xModel2Len];
                            xModel2[0] = useDMarker1 && fitMin[curve] > xPlotMin ? fitMin[curve] : xPlotMin;
                            for (int i = 1; i < xModel2Len; i++) {
                                xModel2[i] = xModel2[i - 1] + xModel2Step;
                            }


                            yModel1 = IJU.transitModel(xModel1, bestFit[0], bestFit[4], bestFit[1], bestFit[2], bestFit[3], orbitalPeriod[curve], forceCircularOrbit[curve] ? 0.0 : eccentricity[curve], forceCircularOrbit[curve] ? 0.0 : omega[curve], bestFit[5], bestFit[6], useLonAscNode[curve], lonAscNode[curve]);

                            yModel2 = IJU.transitModel(xModel2, bestFit[0], bestFit[4], bestFit[1], bestFit[2], bestFit[3], orbitalPeriod[curve], forceCircularOrbit[curve] ? 0.0 : eccentricity[curve], forceCircularOrbit[curve] ? 0.0 : omega[curve], bestFit[5], bestFit[6], useLonAscNode[curve], lonAscNode[curve]);

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
                            if (detrendIndex[curve][v] != 0 && Double.isNaN(detrend[curve][v][j])) {
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
                            if (detrendIndex[curve][v] != 0 && detrendYDNotConstant[v]) {
                                trend += detrendFactor[v] * (detrend[curve][v][j]);//-detrendAverage[v]);
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
                        if (detrendIndex[curve][v] != 0 && detrendYDNotConstant[v]) {
                            trend += detrendFactor[v] * (detrend[curve][v][j]);//-detrendAverage[v]);
                        }
                    }
                    if (hasErrors[curve] || hasOpErrors[curve]) {
                        yerr[j] /= (y[j] / (y[j] - trend));
                    }
                   y[j] -= trend;
                }
            }
        }

        if (detrendFitIndex[curve] == 9 && useTransitFit[curve] && yModel1 != null) {
            int cnt = 0;
            int len = yModel1.length;
            residual = new double[len];
            if (hasErrors[curve] || hasOpErrors[curve]) yModel1Err = new double[len];
            for (int j = 0; j < nn[curve]; j++) {
                if (cnt < len && !Double.isNaN(x[curve][j]) && !Double.isNaN(y[j]) && x[curve][j] > fitMin[curve] && x[curve][j] < fitMax[curve]) {
                    residual[cnt] = y[j] - yModel1[cnt];
                    if (hasErrors[curve] || hasOpErrors[curve]) yModel1Err[cnt] = yerr[j];
                    cnt++;
                }
            }

            for (double v : residual) {
                sigma += v * v;
            }
            sigma = Math.sqrt(sigma / cnt);
            var rms = sigma / bestFit[0];
            /*AIJLogger.log("rms: " + rms * 1000);
            AIJLogger.log("bic: " + bic);*/
            return new OptimizerResults(rms, bic);
        }

        return new OptimizerResults(Double.NaN, Double.NaN);
    }

    public record OptimizerResults(double rms, double bic) {
        @Override
        public String toString() {
            var rmso = rms;
            var bico = bic;
            if (Double.isFinite(rms)) {
                BigDecimal bd = new BigDecimal(Double.toString(rms * 1000));
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
        double[] detrendX, detrendYE, priorCenter;
        boolean[] isFitted;
        double detrendYAverage;

        public FitLightCurveChi2(double[] detrendY, double dof, double bp, double[] detrendX, double[] detrendYE, boolean[] isFitted, double detrendYAverage, double[] priorCenter) {
            this.detrendY = detrendY;
            this.dof = dof;
            this.bp = bp;
            this.detrendX = detrendX;
            this.detrendYE = detrendYE;
            this.isFitted = isFitted;
            this.detrendYAverage = detrendYAverage;
            this.priorCenter = priorCenter;
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
                        incl = Math.acos((1.0 + p0) / ar);
                    }
                } else {
                    incl = Math.acos(bp/ar);
                }
                u1 = lockToCenter[curve][5] ? priorCenter[5] : param[fp < nPars ? fp++ : nPars - 1];  //quadratic limb darkening parameter 1
                u2 = lockToCenter[curve][6] ? priorCenter[6] : param[fp < nPars ? fp++ : nPars - 1];  //quadratic limb darkening parameter 2

                lcModel[curve] = IJU.transitModel(detrendX, f0, incl, p0, ar, tc, orbitalPeriod[curve], e, ohm, u1, u2, useLonAscNode[curve], lonAscNode[curve]);
            }

            int dp = 0;
            for (int p = 7; p < maxFittedVars; p++) {
                if (isFitted[p]) {
                    dPars[dp++] = param[fp++];
                } else if (detrendIndex[curve][p - 7] != 0 && detrendYDNotConstant[p - 7] && lockToCenter[curve][p]) {
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
                        residual -= (lcModel[curve][j] - detrendYAverage);
                        chi2 += ((residual * residual) / (detrendYE[j] * detrendYE[j]));
                    }
                }
            } else {
                for (int j = 0; j < numData; j++) {
                    residual = detrendY[j];// - param[0];
                    for (int i = 0; i < numDetrendVars; i++) {
                        residual -= detrendVars[i][j] * dPars[i];
                    }
                    chi2 += ((residual * residual) / (detrendYE[j] * detrendYE[j]));
                }
            }
            return chi2 / dof;
        }
    }

    public class FitDetrendOnly implements MinimizationFunction {
        double[] detrendY;

        public FitDetrendOnly(double[] detrendY) {
            this.detrendY = detrendY;
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

        public FitDetrendChi2(double[] detrendY, double[] detrendYE) {
            this.detrendY = detrendY;
            this.detrendYE = detrendYE;
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
}
