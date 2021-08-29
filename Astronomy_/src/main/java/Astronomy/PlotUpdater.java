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

import java.util.Arrays;

import static Astronomy.MultiPlot_.*;

/**
 * Extracted version of {@link MultiPlot_#updatePlot(boolean[], boolean)} for the multithreaded optimizer.
 */
public class PlotUpdater {
    private static PlotUpdater INSTANCE;
    //todo curve and comp. star/param setting
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
        //todo add stuff
        // add check for fitter running only on Tstar and other conditions
        // //todo extract star and filter from this "rel_flux_Txx"
    }

    public static PlotUpdater getInstance(int curve, int targetStar) {
        if (INSTANCE == null || INSTANCE.curve != curve || INSTANCE.targetStar != targetStar)
            INSTANCE = new PlotUpdater(curve, targetStar);
        return INSTANCE;
    }

    public static void invalidateInstance() {//todo call this at start of optimization, then use getInstance to everything
        INSTANCE = null;
    }

    public PlotResults fitCurveAndGetResults(boolean[] isRefStar) {
        localIsRefStar = isRefStar;

        return updateCurve();
    }

    private void getStarData() {
        if (table == null) return;
        int numRows = table.getCounter();
        if (numRows < 1) return;
        boolean goodErrData = true;
        source = new double[numAps][numRows];
        srcvar = new double[numAps][numRows];
        total = new double[numAps][numRows];
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

        for (int ap = 0; ap < numAps; ap++) {
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
                                    //detrendYAverage[curve] += y[curve][j];//todo move to chunk2
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
                        for (int j = 0; j < nn[curve]; j++) { //todo check on Source-Sky, Source_Error values, and detrend parameters for NaNs. If a NaN is found, bail out and give an appropriate error.
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

    //todo relflux calc
    //todo make threadsafe
    //todo remove unneeded parts
    //todo specific minimization objects
    private PlotResults updateCurve() {
        var minimization = minimizationThreadLocal.get();
        var avgCount = initAvgCount;
        var atLeastOne = initAtLeastOne;
        var detrendCount = initDetrendCount;
        var baselineCount = initBaselineCount;
        var depthCount = initDepthCount;
        var detrendVarsUsed = initDetrendVarsUsed;
        var detrendY = initDetrendY;
        var detrendYNotConstant = initDetrendYNotConstant;
        var detrendYE = initDetrendYE;
        var detrendX = initDetrendX;
        var detrendYD = initDetrendYD;

        //atLeastOne = false;
        //detrendYNotConstant = false;
        //detrendYDNotConstant = new boolean[maxDetrendVars];
        //detrendYAverage[curve] = 0.0;

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

        if (!plotY[curve]) return new PlotResults(Double.NaN, Double.NaN); //todo add filtering on return for NaN

        if (atLeastOne || detrendFitIndex[curve] == 9) {
            if (detrendFitIndex[curve] != 1) {
                if (detrendCount > 0) {
                    yAverage[curve] /= avgCount; //avgCount is always >= detrendCount, so > 0 here
                    detrendYAverage[curve] /= detrendCount;
                    if (baselineCount > 0) {
                        yBaselineAverage[curve] /= baselineCount;
                    } else {
                        yBaselineAverage[curve] = detrendYAverage[curve] * 1.005;
                    }
                    if (depthCount > 0) {
                        yDepthEstimate[curve] /= depthCount;
                    } else {
                        yDepthEstimate[curve] = detrendYAverage[curve] * 0.995;
                    }

                    for (int j = 0; j < detrendCount; j++) {
                        detrendY[j] -= detrendYAverage[curve];//yAverage[curve];
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
                    
                    if ((detrendVarsUsed[curve] > 0 || detrendFitIndex[curve] == 9 && useTransitFit[curve]) && detrendYNotConstant && detrendCount > (detrendFitIndex[curve] == 9 && useTransitFit[curve] ? 7 : 0) + detrendVarsUsed[curve] + 2) { //need enough data to provide degrees of freedom for successful fit
                        detrendXs[curve] = Arrays.copyOf(detrendX, detrendCount);
                        //detrendYs[curve] = Arrays.copyOf(detrendY, detrendCount);
                        //detrendYEs[curve] = Arrays.copyOf(detrendYE, detrendCount);

                        getStarData();

                        for (int i= 0; i< detrendXs[curve].length; i++) {
                            var compSum = 0.0;
                            var compVar = 0.0;
                            for (int ap = 0; ap < localIsRefStar.length; ap++) {
                                if (localIsRefStar[ap] && targetStar != ap) {
                                    compSum += source[ap][i];
                                    compVar += srcvar[ap][i];
                                }
                            }
                            total[curve][i] = compSum;
                            totvar[curve][i] = compVar;
                        }

                        for (int i= 0; i< detrendXs[curve].length; i++) {
                            detrendYs[curve][i] = total[curve][i] == 0 ? Double.NaN : source[targetStar][i] / total[curve][i];
                            if (source[curve][i] == 0 || total[curve][i] == 0) {
                                detrendYEs[curve][i] = Double.POSITIVE_INFINITY;
                            } else {
                                detrendYEs[curve][i] = detrendYs[curve][i] * Math.sqrt(srcvar[targetStar][i]*srcvar[targetStar][i] / (source[targetStar][i] * source[targetStar][i]) + totvar[curve][i] / (total[curve][i] * total[curve][i]));
                            }
                        }

                        for (int j = 0; j < nn[curve]; j++) {
                            //if (noNaNs) {
                                detrendYAverage[curve] += y[curve][j];
                                if (detrendFitIndex[curve] == 9) {
                                    if (x[curve][j] > fitLeft[curve] + (fitRight[curve] - fitLeft[curve]) / 4.0 && x[curve][j] < fitRight[curve] - (fitRight[curve] - fitLeft[curve]) / 4.0) {
                                        yDepthEstimate[curve] += y[curve][j];
                                        depthCount++;
                                    } else if (x[curve][j] < fitLeft[curve] || x[curve][j] > fitRight[curve]) {
                                        yBaselineAverage[curve] += y[curve][j];
                                        baselineCount++;
                                    }
                                }
                            //}
                        }


                        if (updateFit) {
                            int fittedDetrendParStart;
                            
                            if (detrendFitIndex[curve] == 9) {
                                minimization.removeConstraints();
                                
                                int nFitted = 0;
                                for (int p = 0; p < 7; p++) {
                                    if (useTransitFit[curve] && !lockToCenter[curve][p]) {
                                        isFitted[curve][p] = true;
                                        nFitted++;
                                    } else {
                                        isFitted[curve][p] = false;
                                    }
                                }
                                
                                fittedDetrendParStart = nFitted;
                                for (int p = 7; p < maxFittedVars; p++) {
                                    if (detrendIndex[curve][p - 7] != 0 && detrendYDNotConstant[p - 7] && !lockToCenter[curve][p]) {
                                        isFitted[curve][p] = true;
                                        nFitted++;
                                    } else {
                                        isFitted[curve][p] = false;
                                    }
                                }

                                start[curve] = new double[nFitted];
                                width[curve] = new double[nFitted];
                                step[curve] = new double[nFitted];

                                index[curve] = new int[nFitted];
                                int fp = 0;  //fitted parameter
                                for (int p = 0; p < maxFittedVars; p++) {
                                    if (isFitted[curve][p]) {
                                        index[curve][fp] = p;
                                        fp++;
                                    }
                                }
                                

                                dof[curve] = detrendXs[curve].length - start[curve].length;

                                // 0 = f0 = baseline flux
                                // 1 = p0 = r_p/r_*
                                // 2 = ar = a/r_*
                                // 3 = tc = transit center time
                                // 4 = i = inclination
                                // 5 = u1 = quadratic limb darkening parameter 1
                                // 6 = u2 = quadratic limb darkening parameter 2
                                // 7+ = detrend parameters
                                for (fp = 0; fp < nFitted; fp++) {
                                    if (index[curve][fp] == 1) {
                                        start[curve][fp] = Math.sqrt(priorCenter[curve][1]);
                                        width[curve][fp] = Math.sqrt(priorWidth[curve][1]);
                                        step[curve][fp] = Math.sqrt(getFitStep(curve, 1));
                                        minimization.addConstraint(fp, -1, 0.0);
                                    } else if (index[curve][fp] == 4) {
                                        start[curve][fp] = priorCenter[curve][4] * Math.PI / 180.0;  // inclination
                                        width[curve][fp] = priorWidth[curve][4] * Math.PI / 180.0;
                                        step[curve][fp] = getFitStep(curve, 4) * Math.PI / 180.0;
                                        minimization.addConstraint(fp, 1, 90.0 * Math.PI / 180.0);
                                        minimization.addConstraint(fp, -1, 50.0 * Math.PI / 180.0);
                                    } else {
                                        if (index[curve][fp] == 0) minimization.addConstraint(fp, -1, 0.0);
                                        if (index[curve][fp] == 2) minimization.addConstraint(fp, -1, 2.0);
                                        if (index[curve][fp] == 3) minimization.addConstraint(fp, -1, 0.0);
                                        if (index[curve][fp] == 5) {
                                            minimization.addConstraint(fp, 1, 1.0);
                                            minimization.addConstraint(fp, -1, -1.0);
                                        }
                                        if (index[curve][fp] == 6) {
                                            minimization.addConstraint(fp, 1, 1.0);
                                            minimization.addConstraint(fp, -1, -1.0);
                                        }
                                        start[curve][fp] = priorCenter[curve][index[curve][fp]];
                                        width[curve][fp] = priorWidth[curve][index[curve][fp]];
                                        step[curve][fp] = getFitStep(curve, index[curve][fp]);
                                    }
                                    if (usePriorWidth[curve][index[curve][fp]]) {
                                        minimization.addConstraint(fp, 1, start[curve][fp] + width[curve][fp]);
                                        minimization.addConstraint(fp, -1, start[curve][fp] - width[curve][fp]);
                                    }
                                }

                                minimization.setNrestartsMax(1);
                                minimization.nelderMead(new FitLightCurveChi2(), start[curve], step[curve], tolerance[curve], maxFitSteps[curve]);
                                coeffs[curve] = minimization.getParamValues();
                                nTries[curve] = minimization.getNiter() - 1;
                                converged[curve] = minimization.getConvStatus();
                                fp = 0;
                                for (int p = 0; p < maxFittedVars; p++) {
                                    if (isFitted[curve][p]) {
                                        bestFit[curve][p] = coeffs[curve][fp];
                                        fp++;
                                    } else if (p < 7 && useTransitFit[curve] && lockToCenter[curve][p]) {
                                        if (p == 1) {
                                            bestFit[curve][p] = Math.sqrt(priorCenter[curve][p]);
                                        } else if (p == 4) {
                                            if (bpLock[curve]) {
                                                bestFit[curve][4] = Math.acos(bp[curve] / bestFit[curve][2]);
                                            } else {
                                                bestFit[curve][p] = priorCenter[curve][p] * Math.PI / 180.0;
                                            }
                                        } else {
                                            bestFit[curve][p] = priorCenter[curve][p];
                                        }
                                    } else if (p >= 7 && p < 7 + maxDetrendVars && detrendIndex[curve][p - 7] != 0 && detrendYDNotConstant[p - 7] && lockToCenter[curve][p]) {
                                        bestFit[curve][p] = priorCenter[curve][p];
                                    } else {
                                        bestFit[curve][p] = Double.NaN;
                                    }
                                }
                                if (useTransitFit[curve]) {
                                    //Winn 2010 eqautions 14, 15, 16
                                    if (!bpLock[curve])
                                        bp[curve] = bestFit[curve][2] * Math.cos(bestFit[curve][4]) * (1.0 - (forceCircularOrbit[curve] ? 0.0 : eccentricity[curve] * eccentricity[curve])) / (1.0 + (forceCircularOrbit[curve] ? 0.0 : eccentricity[curve]) * Math.sin(forceCircularOrbit[curve] ? 0.0 : omega[curve]));
                                    double bp2 = bp[curve] * bp[curve];
                                    t14[curve] = (orbitalPeriod[curve] / Math.PI * Math.asin(Math.sqrt((1.0 + bestFit[curve][1]) * (1.0 + bestFit[curve][1]) - bp2) / (Math.sin(bestFit[curve][4]) * bestFit[curve][2])) * Math.sqrt(1.0 - (forceCircularOrbit[curve] ? 0.0 : eccentricity[curve] * eccentricity[curve])) / (1.0 + (forceCircularOrbit[curve] ? 0.0 : eccentricity[curve]) * Math.sin(forceCircularOrbit[curve] ? 0.0 : omega[curve])));
                                    t23[curve] = (orbitalPeriod[curve] / Math.PI * Math.asin(Math.sqrt((1.0 - bestFit[curve][1]) * (1.0 - bestFit[curve][1]) - bp2) / (Math.sin(bestFit[curve][4]) * bestFit[curve][2])) * Math.sqrt(1.0 - (forceCircularOrbit[curve] ? 0.0 : eccentricity[curve] * eccentricity[curve])) / (1.0 + (forceCircularOrbit[curve] ? 0.0 : eccentricity[curve]) * Math.sin(forceCircularOrbit[curve] ? 0.0 : omega[curve])));
                                    tau[curve] = (t14[curve] - t23[curve]) / 2.0;
                                    double sin2tTpioP = Math.pow(Math.sin(t14[curve] * Math.PI / orbitalPeriod[curve]), 2);
                                    stellarDensity[curve] = 0.0189 / (orbitalPeriod[curve] * orbitalPeriod[curve]) * Math.pow(((1.0 + bestFit[curve][1]) * (1.0 + bestFit[curve][1]) - bp2 * (1 - sin2tTpioP)) / sin2tTpioP, 1.5);

                                    double midpointFlux = IJU.transitModel(new double[]{bestFit[curve][3]}, bestFit[curve][0], bestFit[curve][4], bestFit[curve][1], bestFit[curve][2], bestFit[curve][3], orbitalPeriod[curve], forceCircularOrbit[curve] ? 0.0 : eccentricity[curve], forceCircularOrbit[curve] ? 0.0 : omega[curve], bestFit[curve][5], bestFit[curve][6], useLonAscNode[curve], lonAscNode[curve])[0];
                                    transitDepth[curve] = (1 - (midpointFlux / bestFit[curve][0])) * 1000;
                                }
                                
                                chi2dof[curve] = minimization.getMinimum();
                                bic[curve] = chi2dof[curve] * (detrendXs[curve].length - bestFit[curve].length) + bestFit[curve].length * Math.log(detrendXs[curve].length);

                                fp = fittedDetrendParStart;
                                for (int p = 7; p < maxFittedVars; p++) {
                                    if (isFitted[curve][p]) {
                                        detrendFactor[curve][p - 7] = coeffs[curve][fp++];
                                    } else if (lockToCenter[curve][p]) {
                                        detrendFactor[curve][p - 7] = priorCenter[curve][p];
                                    }
                                }
                            } else if (useNelderMeadChi2ForDetrend) {
                                minimization.removeConstraints();
                                start[curve] = new double[detrendVars.length];
                                step[curve] = new double[detrendVars.length];
                                for (int i = 0; i < start[curve].length; i++) {
                                    start[curve][i] = 0.0;
                                    step[curve][i] = 1.0;
                                }
                                double fTol = 1e-10;
                                int nMax = 20000;
                                minimization.nelderMead(new FitDetrendChi2(), start[curve], step[curve], fTol, nMax);
                                coeffs[curve] = minimization.getParamValues();

                                varCount = 0;
                                for (int v = 0; v < maxDetrendVars; v++) {
                                    if (detrendIndex[curve][v] != 0 && detrendYDNotConstant[v]) {
                                        detrendFactor[curve][v] = coeffs[curve][varCount];
                                        varCount++;
                                    }
                                }
                            } else {  //use regression
                                Regression regression = new Regression(detrendVars, detrendYs[curve]);
                                regression.linear();
                                coeffs[curve] = regression.getCoeff();

                                varCount = 1;
                                for (int v = 0; v < maxDetrendVars; v++) {
                                    if (detrendIndex[curve][v] != 0 && detrendYDNotConstant[v]) {
                                        detrendFactor[curve][v] = coeffs[curve][varCount];
                                        varCount++;
                                    }
                                }
                            }
                        }
                        

                        if (detrendFitIndex[curve] == 9 && useTransitFit[curve]) {
                            createDetrendModel = false;
                            xModel1[curve] = detrendXs[curve];
                            int xModel2Len = plotSizeX + 1;
                            double xModel2Step = ((useDMarker4 && fitMax[curve] < xPlotMax ? fitMax[curve] : xPlotMax) - (useDMarker1 && fitMin[curve] > xPlotMin ? fitMin[curve] : xPlotMin)) / (xModel2Len - 1);
                            xModel2[curve] = new double[xModel2Len];
                            xModel2[curve][0] = useDMarker1 && fitMin[curve] > xPlotMin ? fitMin[curve] : xPlotMin;
                            for (int i = 1; i < xModel2Len; i++) {
                                xModel2[curve][i] = xModel2[curve][i - 1] + xModel2Step;
                            }


                            yModel1[curve] = IJU.transitModel(xModel1[curve], bestFit[curve][0], bestFit[curve][4], bestFit[curve][1], bestFit[curve][2], bestFit[curve][3], orbitalPeriod[curve], forceCircularOrbit[curve] ? 0.0 : eccentricity[curve], forceCircularOrbit[curve] ? 0.0 : omega[curve], bestFit[curve][5], bestFit[curve][6], useLonAscNode[curve], lonAscNode[curve]);

                            yModel2[curve] = IJU.transitModel(xModel2[curve], bestFit[curve][0], bestFit[curve][4], bestFit[curve][1], bestFit[curve][2], bestFit[curve][3], orbitalPeriod[curve], forceCircularOrbit[curve] ? 0.0 : eccentricity[curve], forceCircularOrbit[curve] ? 0.0 : omega[curve], bestFit[curve][5], bestFit[curve][6], useLonAscNode[curve], lonAscNode[curve]);

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
                        xModel1[curve] = null;
                        xModel2[curve] = null;
                        yModel1[curve] = null;
                        yModel2[curve] = null;
                    }

                } else {
                    xModel1[curve] = null;
                    xModel2[curve] = null;
                    yModel1[curve] = null;
                    yModel2[curve] = null;
                }
            } else {
                for (int j = 0; j < nn[curve]; j++) {
                    boolean noNaNs = true;
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
                        yAverage[curve] += y[curve][j];
                        avgCount++;
                    }
                }
                if (avgCount > 0) {
                    yAverage[curve] /= avgCount;
                }
                xModel1[curve] = new double[2];
                xModel1[curve][0] = Math.max(fitMin[curve], xPlotMin);
                xModel1[curve][1] = Math.min(fitMax[curve], xPlotMax);
                xModel2[curve] = null;
                yModel1[curve] = new double[2];
                yModel1[curve][0] = yAverage[curve];
                yModel1[curve][1] = yAverage[curve];
                yModel2[curve] = null;
            }

            if (divideNotSubtract) {
                double trend;
                if (yAverage[curve] != 0.0) {
                    for (int j = 0; j < nn[curve]; j++) {
                        trend = yAverage[curve];
                        for (int v = 0; v < maxDetrendVars; v++) {
                            if (detrendIndex[curve][v] != 0 && detrendYDNotConstant[v]) {
                                trend += detrendFactor[curve][v] * (detrend[curve][v][j]);//-detrendAverage[v]);
                            }
                        }
                        trend /= yAverage[curve];
                        if (trend != 0.0) {
                            y[curve][j] /= trend;
                            if (hasErrors[curve] || hasOpErrors[curve]) {
                                yerr[curve][j] /= trend;
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
                            trend += detrendFactor[curve][v] * (detrend[curve][v][j]);//-detrendAverage[v]);
                        }
                    }
                    if (hasErrors[curve] || hasOpErrors[curve]) {
                        yerr[curve][j] /= (y[curve][j] / (y[curve][j] - trend));
                    }
                    y[curve][j] -= trend;
                }
            }
        }

        if (detrendFitIndex[curve] == 9 && useTransitFit[curve] && yModel1[curve] != null) {
            int cnt = 0;
            int len = yModel1[curve].length;
            residual[curve] = new double[len];
            if (hasErrors[curve] || hasOpErrors[curve]) yModel1Err[curve] = new double[len];
            for (int j = 0; j < nn[curve]; j++) {
                if (cnt < len && !Double.isNaN(x[curve][j]) && !Double.isNaN(y[curve][j]) && x[curve][j] > fitMin[curve] && x[curve][j] < fitMax[curve]) {
                    residual[curve][cnt] = y[curve][j] - yModel1[curve][cnt];
                    if (hasErrors[curve] || hasOpErrors[curve]) yModel1Err[curve][cnt] = yerr[curve][j];
                    cnt++;
                }
            }

            //todo not use MP sigma
            for (int i = 0; i < residual[curve].length; i++) {
                sigma[curve] += residual[curve][i] * residual[curve][i];
            }
            sigma[curve] = Math.sqrt(sigma[curve] / residual[curve].length);
            var rms = sigma[curve] / bestFit[curve][0];
            AIJLogger.log("rms: " + rms);
            return new PlotResults(rms, bic[curve]);
        }

        return new PlotResults(Double.NaN, Double.NaN);
    }

    public record PlotResults(double rms, double bic) {}

    //todo threadsafe
    public class FitLightCurveChi2 implements MinimizationFunction {
        public double function(double[] param) {
            int numData = detrendYs[curve].length;
            int numDetrendVars = detrendVars.length;
            int nPars = param.length;
            double[] dPars = new double[detrendVars.length];
            if (dof[curve] < 1) dof[curve] = 1;

            chi2[curve] = 0;
            double residual;
            int fp = 0;

            double f0 = priorCenter[curve][0]; // baseline flux
            double p0 = priorCenter[curve][1]; // r_p/r_*
            double ar = priorCenter[curve][2]; // a/r_*
            double tc = priorCenter[curve][3]; //transit center time
            double incl = priorCenter[curve][4];  //inclination
            double u1 = priorCenter[curve][5];  //quadratic limb darkening parameter 1
            double u2 = priorCenter[curve][6];  //quadratic limb darkening parameter 2
            double e = forceCircularOrbit[curve] ? 0.0 : eccentricity[curve];
            double ohm = forceCircularOrbit[curve] ? 0.0 : omega[curve];
            double b = 0.0;
            if (useTransitFit[curve]) {
                f0 = lockToCenter[curve][0] ? priorCenter[curve][0] : param[fp < nPars ? fp++ : nPars - 1]; // baseline flux
                p0 = lockToCenter[curve][1] ? Math.sqrt(priorCenter[curve][1]) : param[fp < nPars ? fp++ : nPars - 1]; // r_p/r_*
                ar = lockToCenter[curve][2] ? priorCenter[curve][2] : param[fp < nPars ? fp++ : nPars - 1]; // a/r_*
                tc = lockToCenter[curve][3] ? priorCenter[curve][3] : param[fp < nPars ? fp++ : nPars - 1]; //transit center time
                if (!bpLock[curve]) {
                    incl = lockToCenter[curve][4] ? priorCenter[curve][4] * Math.PI / 180.0 : param[fp < nPars ? fp++ : nPars - 1];  //inclination
                    b = Math.cos(incl) * ar;
                    if (b > 1.0 + p0) {  //ensure planet transits or grazes the star
                        incl = Math.acos((1.0 + p0) / ar);
                    }
                } else {
                    incl = Math.acos(bp[curve]/ar);
                }
                u1 = lockToCenter[curve][5] ? priorCenter[curve][5] : param[fp < nPars ? fp++ : nPars - 1];  //quadratic limb darkening parameter 1
                u2 = lockToCenter[curve][6] ? priorCenter[curve][6] : param[fp < nPars ? fp++ : nPars - 1];  //quadratic limb darkening parameter 2

                lcModel[curve] = IJU.transitModel(detrendXs[curve], f0, incl, p0, ar, tc, orbitalPeriod[curve], e, ohm, u1, u2, useLonAscNode[curve], lonAscNode[curve]);
            }

            int dp = 0;
            for (int p = 7; p < maxFittedVars; p++) {
                if (isFitted[curve][p]) {
                    dPars[dp++] = param[fp++];
                } else if (detrendIndex[curve][p - 7] != 0 && detrendYDNotConstant[p - 7] && lockToCenter[curve][p]) {
                    dPars[dp++] = priorCenter[curve][p];
                }
            }


            if (useTransitFit[curve]) {
                if (!lockToCenter[curve][2] && (ar < (1.0 + p0))) {
                    chi2[curve] = Double.POSITIVE_INFINITY;  //boundary check that planet does not orbit within star
                } else if ((!lockToCenter[curve][2] || !lockToCenter[curve][4]) && ((ar * Math.cos(incl) * (1.0 - e * e) / (1.0 + e * Math.sin(ohm * Math.PI / 180.0))) >= 1.0 + p0)) {
                    if (!lockToCenter[curve][4] && autoUpdatePrior[curve][4]) {
                        priorCenter[curve][4] = Math.round(10.0 * Math.acos((0.5 + p0) * (1.0 + e * Math.sin(ohm * Math.PI / 180.0)) / (ar * (1.0 - e * e))) * 180.0 / Math.PI) / 10.0;
                        if (Double.isNaN(priorCenter[curve][4])) priorCenter[curve][4] = 89.9;
                        //priorCenterSpinner[curve][4].setValue(priorCenter[curve][4]);
                    }
                    chi2[curve] = Double.POSITIVE_INFINITY; //boundary check that planet passes in front of star
                } else if ((!lockToCenter[curve][5] || !lockToCenter[curve][6]) && (((u1 + u2) > 1.0) || ((u1 + u2) < 0.0) || (u1 > 1.0) || (u1 < 0.0) || (u2 < -1.0) || (u2 > 1.0))) {
                    chi2[curve] = Double.POSITIVE_INFINITY;
                } else {
                    for (int j = 0; j < numData; j++) {
                        residual = detrendYs[curve][j];// - param[0];
                        for (int i = 0; i < numDetrendVars; i++) {
                            residual -= detrendVars[i][j] * dPars[i];
                        }
                        residual -= (lcModel[curve][j] - detrendYAverage[curve]);
                        chi2[curve] += ((residual * residual) / (detrendYEs[curve][j] * detrendYEs[curve][j]));
                    }
                }
            } else {
                for (int j = 0; j < numData; j++) {
                    residual = detrendYs[curve][j];// - param[0];
                    for (int i = 0; i < numDetrendVars; i++) {
                        residual -= detrendVars[i][j] * dPars[i];
                    }
                    chi2[curve] += ((residual * residual) / (detrendYEs[curve][j] * detrendYEs[curve][j]));
                }
            }
            return chi2[curve] / (double) dof[curve];
        }
    }

    //todo threadsafe
    public class FitDetrendOnly implements MinimizationFunction {
        public double function(double[] param) {
            double sd = 0.0;
            double residual;
            int numData = detrendYs[curve].length;
            int numVars = detrendVars.length;
            for (int j = 0; j < numData; j++) {
                residual = detrendYs[curve][j] - param[0];
                for (int i = 0; i < numVars; i++) {
                    residual -= detrendVars[i][j] * param[i + 1];
                }
                sd += residual * residual;
            }
            return Math.sqrt(sd / (double) numData);
        }
    }

    //todo threadsafe
    public class FitDetrendChi2 implements MinimizationFunction {
        public double function(double[] param) {
            double chi2 = 0.0;
            double residual;
            int numData = detrendYs[curve].length;
            int numVars = detrendVars.length;
            int dof = numData - param.length;
            if (dof < 1) dof = 1;
            for (int j = 0; j < numData; j++) {
                residual = detrendYs[curve][j];// - param[0];
                for (int i = 0; i < numVars; i++) {
                    residual -= detrendVars[i][j] * param[i];
                }
                chi2 += ((residual * residual) / (detrendYEs[curve][j] * detrendYEs[curve][j]));
            }
            return chi2 / (double) dof;
        }
    }
}
