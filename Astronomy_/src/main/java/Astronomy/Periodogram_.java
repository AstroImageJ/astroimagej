package Astronomy;

import astroj.MeasurementTable;
import ij.IJ;
import ij.ImagePlus;
import ij.WindowManager;
import ij.astro.io.prefs.Property;
import ij.gui.GenericDialog;
import ij.gui.Plot;
import ij.plugin.PlugIn;
import ij.text.TextWindow;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import java.awt.*;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class Periodogram_ implements PlugIn {
    private static final Property<String> DATA_SOURCE = new Property<>("Synthetic", Periodogram_.class);
    private static final Property<String> ALGORITHM = new Property<>("BLS", Periodogram_.class);
    private static final Property<String> T0_MASK = new Property<>("Model center (sliding fit)", Periodogram_.class);
    private static final Property<String> TIME_COL = new Property<>("", Periodogram_.class);
    private static final Property<String> FLUX_COL = new Property<>("", Periodogram_.class);
    private static final Property<Double> MIN_PERIOD = new Property<>(0.0, Periodogram_.class);
    private static final Property<Double> MAX_PERIOD = new Property<>(0.0, Periodogram_.class);
    private static final Property<Double> STEPS = new Property<>(5000.0, Periodogram_.class);
    private static final Property<Double> MIN_FRACTIONAL_DURATION = new Property<>(0.01, Periodogram_.class);
    private static final Property<Double> MAX_FRACTIONAL_DURATION = new Property<>(0.1, Periodogram_.class);
    private static final Property<Double> DURATION_STEPS = new Property<>(200.0, Periodogram_.class);
    private static final Property<Double> PEAK_WIDTH_CUTOFF = new Property<>(0.2, Periodogram_.class);
    private static final Property<Double> PEAK_COUNT = new Property<>(1.0, Periodogram_.class);
    private static final Property<Double> PLANET_COUNT = new Property<>(1.0, Periodogram_.class);
    private static final Property<Double> TRANSIT_MASK_FACTOR = new Property<>(3.0, Periodogram_.class);
    private static final Property<Double> MASK_FACTOR_PERIOD = new Property<>(0.1, Periodogram_.class);
    private static final Property<Double> LIMB_U1 = new Property<>(0.3, Periodogram_.class);
    private static final Property<Double> LIMB_U2 = new Property<>(0.3, Periodogram_.class);

    @Override
    public void run(String arg) {
        // Gather measurement tables
        String[] tableNames = MeasurementTable.getMeasurementTableNames();
        List<String> dataSourceList = new ArrayList<>();
        if (tableNames != null) {
            Collections.addAll(dataSourceList, tableNames);
        }
        dataSourceList.add("Synthetic");
        dataSourceList.add("Current Image");
        String[] dataSources = dataSourceList.toArray(new String[0]);
        String defaultDataSource = (tableNames != null && tableNames.length > 0) ? tableNames[0] : "Synthetic";

        // Use previous source
        if (dataSourceList.contains(DATA_SOURCE.get())) {
            defaultDataSource = DATA_SOURCE.get();
        }

        GenericDialog gd = new GenericDialog("Periodogram");
        gd.addChoice("Data Source", dataSources, defaultDataSource);
        gd.addChoice("Algorithm", new String[]{"BLS", "TLS", "Lomb-Scargle"}, ALGORITHM.get());
        gd.showDialog();
        if (gd.wasCanceled()) return;

        String dataSource = gd.getNextChoice();
        String algorithm = gd.getNextChoice();
        ALGORITHM.set(algorithm);
        boolean isMeasurementTable = false;
        if (tableNames != null) {
            for (String t : tableNames) {
                if (dataSource.equals(t)) {
                    isMeasurementTable = true;
                    break;
                }
            }
        }

        if (algorithm.equals("BLS")) {
            if (isMeasurementTable) {
                new Thread(() -> {
                    MeasurementTable table = MeasurementTable.getTable(dataSource);
                    if (table == null) {
                        IJ.showMessage("Periodogram", "Could not open table.");
                        return;
                    }
                    String[] columns = table.getColumnHeadings().split("\t");
                    if (columns.length < 2) {
                        IJ.showMessage("Periodogram", "Not enough columns in table.");
                        return;
                    }
                    GenericDialog gdCol = new GenericDialog("BLS Settings");
                    gdCol.addChoice("Time column", columns, TIME_COL.get().isEmpty() ? columns[0] : TIME_COL.get());
                    gdCol.addChoice("Flux column", columns, FLUX_COL.get().isEmpty() ? columns[1] : FLUX_COL.get());
                    gdCol.addNumericField("Min period", MIN_PERIOD.get(), 6, 12, null); // 0 means auto
                    gdCol.addNumericField("Max period", MAX_PERIOD.get(), 6, 12, null); // 0 means auto
                    gdCol.addNumericField("Steps", STEPS.get(), 0, 6, null);
                    gdCol.addNumericField("Min fractional duration (0-1)", MIN_FRACTIONAL_DURATION.get(), 3);
                    gdCol.addNumericField("Max fractional duration (0-1)", MAX_FRACTIONAL_DURATION.get(), 3);
                    gdCol.addNumericField("Duration steps", DURATION_STEPS.get(), 0);
                    gdCol.addNumericField("Peak width cutoff (0-1)", PEAK_WIDTH_CUTOFF.get(), 2, 6, null);
                    gdCol.addNumericField("Number of peaks", PEAK_COUNT.get(), 0, 6, null);
                    gdCol.addNumericField("Number of planets to search", PLANET_COUNT.get(), 0);
                    gdCol.addNumericField("In-transit masking factor", TRANSIT_MASK_FACTOR.get(), 2);
                    gdCol.addChoice("T0 for masking", new String[]{"Model center (sliding fit)", "Min average (phase bin)"}, T0_MASK.get());
                    gdCol.showDialog();
                    if (gdCol.wasCanceled()) return;
                    String timeCol = gdCol.getNextChoice();
                    String fluxCol = gdCol.getNextChoice();
                    double minPeriodUser = gdCol.getNextNumber();
                    double maxPeriodUser = gdCol.getNextNumber();
                    int nPeriods = (int) gdCol.getNextNumber();
                    double minFracDur = gdCol.getNextNumber();
                    double maxFracDur = gdCol.getNextNumber();
                    int nDurations = (int) gdCol.getNextNumber();
                    double userCutoff = gdCol.getNextNumber();
                    int userNumPeaks = (int) gdCol.getNextNumber();
                    int nPlanets = (int) gdCol.getNextNumber();
                    double maskFactor = gdCol.getNextNumber();
                    String t0MaskingChoice = gdCol.getNextChoice();
                    TIME_COL.set(timeCol);
                    FLUX_COL.set(fluxCol);
                    MIN_PERIOD.set(minPeriodUser);
                    MAX_PERIOD.set(maxPeriodUser);
                    STEPS.set((double) nPeriods);
                    MIN_FRACTIONAL_DURATION.set(minFracDur);
                    MAX_FRACTIONAL_DURATION.set(maxFracDur);
                    DURATION_STEPS.set((double) nDurations);
                    PEAK_WIDTH_CUTOFF.set((double) userNumPeaks);
                    PLANET_COUNT.set((double) nPlanets);
                    TRANSIT_MASK_FACTOR.set(maskFactor);
                    T0_MASK.set(t0MaskingChoice);
                    if (minFracDur <= 0 || maxFracDur <= 0 || minFracDur >= maxFracDur || nDurations < 1) {
                        IJ.showMessage("Periodogram", "Invalid duration scan parameters.");
                        return;
                    }
                    double[] time = table.getDoubleColumn(table.getColumnIndex(timeCol));
                    double[] flux = table.getDoubleColumn(table.getColumnIndex(fluxCol));
                    if (time == null || flux == null || time.length != flux.length || time.length < 10) {
                        IJ.showMessage("Periodogram", "Invalid or insufficient data.");
                        return;
                    }
                    // Sort time and flux by time (in case not sorted)
                    int n = time.length;
                    double[] sortedTime = new double[n];
                    double[] sortedFlux = new double[n];
                    Integer[] idx = new Integer[n];
                    for (int i = 0; i < n; i++) idx[i] = i;
                    final double[] timeToSort = time;
                    Arrays.sort(idx, Comparator.comparingDouble(i2 -> timeToSort[i2]));
                    for (int i = 0; i < n; i++) {
                        sortedTime[i] = time[idx[i]];
                        sortedFlux[i] = flux[idx[i]];
                    }
                    // Prepare for iterative search
                    boolean[] masked = new boolean[n]; // true = masked/ignored
                    List<PeakResult> allPeaks = new ArrayList<>();
                    double bjdOffset = Double.NaN;
                    try {
                        MeasurementTable measTable = MeasurementTable.getTable("Measurements");
                        int bjdCol = measTable.getColumnIndex("BJD_TDB");
                        if (bjdCol != MeasurementTable.COLUMN_NOT_FOUND && measTable.getCounter() > 0) {
                            bjdOffset = measTable.getValueAsDouble(bjdCol, 0);
                        }
                    } catch (Exception e) {
                        // Ignore, leave bjdOffset as NaN
                    }
                    // Create a single progress window for all planet searches
                    TextWindow progressWin = new TextWindow("Periodogram Progress", "", 600, 600);
                    progressWin.getTextPanel().append(String.format("BLS Multi-Planet Progress (%d planets)\n", nPlanets));
                    // Add overall progress bar
                    progressWin.getTextPanel().append(String.format("Overall Progress: [%s] %d%% (0/%d planets finished)\n", "                    ", 0, nPlanets));
                    int overallProgressLine = progressWin.getTextPanel().getLineCount() - 1;
                    // Add ability to cancel BLS run
                    final AtomicBoolean blsCancelled = new AtomicBoolean(false);
                    Button stopButton = new Button("Stop");
                    stopButton.addActionListener(e -> {
                        blsCancelled.set(true);
                        progressWin.getTextPanel().append("\nBLS run cancelled by user.\n");
                    });
                    Panel buttonPanel = new Panel();
                    buttonPanel.add(stopButton);
                    progressWin.add(buttonPanel, BorderLayout.SOUTH);
                    progressWin.pack();
                    for (int planet = 0; planet < nPlanets; planet++) {
                        if (blsCancelled.get()) break;
                        // Append a line for this planet
                        progressWin.getTextPanel().append(String.format("Planet %d: Starting...\n", planet + 1));
                        int progressLine = progressWin.getTextPanel().getLineCount() - 1;
                        // Debug: print number of unmasked points before each BLS run
                        int nUnmasked = 0;
                        for (int i = 0; i < n; i++) if (!masked[i]) nUnmasked++;
                        System.out.println("Iteration " + (planet + 1) + ": unmasked points = " + nUnmasked);
                        if (nUnmasked < 10) break;
                        // Build unmasked arrays for this BLS run
                        List<Integer> unmaskedIdx = new ArrayList<>();
                        for (int i = 0; i < n; i++) if (!masked[i]) unmaskedIdx.add(i);
                        double[] t = new double[unmaskedIdx.size()];
                        double[] f = new double[unmaskedIdx.size()];
                        for (int i = 0; i < t.length; i++) {
                            t[i] = sortedTime[unmaskedIdx.get(i)];
                            f[i] = sortedFlux[unmaskedIdx.get(i)];
                        }
                        // If all points are masked, stop
                        if (t.length < 10) break;
                        // BLS grid setup (reuse previous logic)
                        double tMin = t[0], tMax = t[t.length-1];
                        double minPeriod = minPeriodUser > 0 ? minPeriodUser : (tMax - tMin) / 20.0;
                        double maxPeriod = maxPeriodUser > 0 ? maxPeriodUser : (tMax - tMin) * 0.8;
                        if (minPeriod <= 0 || maxPeriod <= 0 || minPeriod >= maxPeriod || nPeriods < 2) break;
                        double minFreq = 1.0 / maxPeriod;
                        double maxFreq = 1.0 / minPeriod;
                        double[] periods = new double[nPeriods];
                        double[] power = new double[nPeriods];
                        double[] bestDurations = new double[nPeriods];
                        double[] bestDepths = new double[nPeriods];
                        double[] bestPhases = new double[nPeriods];
                        for (int i = 0; i < nPeriods; i++) {
                            double freq = minFreq + i * (maxFreq - minFreq) / (nPeriods - 1);
                            periods[i] = 1.0 / freq;
                        }
                        // Prepare thread pool
                        int nThreads = Math.max(1, Runtime.getRuntime().availableProcessors() - 2);
                        ExecutorService executor = Executors.newFixedThreadPool(nThreads);
                        List<Future<double[]>> futures = new ArrayList<>();
                        for (int i = 0; i < nPeriods; i++) {
                            final int periodIdx = i;
                            futures.add(executor.submit(() -> {
                                double period = periods[periodIdx];
                                double maxThisPower = 0, bestThisDuration = 0, bestThisDepth = 0, bestThisPhase = 0;
                                for (int d = 0; d < nDurations; d++) {
                                    double duration = minFracDur * period + d * (maxFracDur - minFracDur) * period / Math.max(1, nDurations - 1);
                                    int nPhase = 200;
                                    for (int p = 0; p < nPhase; p++) {
                                        double phase = p * period / nPhase;
                                        double sumIn = 0, sumOut = 0;
                                        int nIn = 0, nOut = 0;
                                        for (int j = 0; j < t.length; j++) {
                                            double tmod = ((t[j] - phase) % period + period) % period;
                                            if (tmod < duration) {
                                                sumIn += f[j];
                                                nIn++;
                                            } else {
                                                sumOut += f[j];
                                                nOut++;
                                            }
                                        }
                                        if (nIn < 2 || nOut < 2) continue;
                                        double meanIn = sumIn / nIn;
                                        double meanOut = sumOut / nOut;
                                        double depth = meanOut - meanIn;
                                        double thisPower = depth * depth * nIn * nOut / (nIn + nOut);
                                        if (thisPower > maxThisPower) {
                                            maxThisPower = thisPower;
                                            bestThisDuration = duration;
                                            bestThisDepth = depth;
                                            bestThisPhase = phase;
                                        }
                                    }
                                }
                                return new double[]{maxThisPower, bestThisDuration, bestThisDepth, bestThisPhase};
                            }));
                        }

                        // Collect results
                        double bestPower = 0;
                        double bestPeriod = 0;
                        double bestDuration = 0;
                        double bestDepth = 0;
                        double bestPhase = 0;
                        int progressStars = 0;
                        int progressStep = Math.max(1, nPeriods / 100); // 50 steps for 2% increments
                        int barLength = 50; // total length of the bar
                        long startTime = System.currentTimeMillis(); // Start time for ETA
                        //TextWindow progressWin = new TextWindow("Periodogram Progress", "Progress\n", 600, 150); // moved outside loop
                        System.out.print("BLS Progress: [");
                        for (int i = 0; i < nPeriods; i++) {
                            if (blsCancelled.get()) {
                                // Shutdown executor immediately when cancelled
                                executor.shutdownNow();
                                try {
                                    // Wait a short time for tasks to cancel
                                    executor.awaitTermination(1, TimeUnit.SECONDS);
                                } catch (InterruptedException e) {
                                    // Ignore interruption
                                }
                                break;
                            }
                            try {
                                double[] result = futures.get(i).get();
                                power[i] = result[0];
                                bestDurations[i] = result[1];
                                bestDepths[i] = result[2];
                                bestPhases[i] = result[3];
                                if (result[0] > bestPower) {
                                    bestPower = result[0];
                                    bestPeriod = periods[i];
                                    bestDuration = result[1];
                                    bestDepth = result[2];
                                    bestPhase = result[3];
                                }
                                if ((i + 1) % progressStep == 0 || i == nPeriods - 1) {
                                    int percent = (int) Math.round(100.0 * (i + 1) / nPeriods);
                                    int nStars = (int) Math.round(barLength * (i + 1) / (double) nPeriods);
                                    StringBuilder bar = new StringBuilder();
                                    bar.append("BLS Progress: [");
                                    for (int s = 0; s < nStars; s++) bar.append("*");
                                    for (int s = nStars; s < barLength; s++) bar.append(" ");
                                    long elapsed = System.currentTimeMillis() - startTime;
                                    double fractionDone = (i + 1) / (double) nPeriods;
                                    long estTotal = (long) (elapsed / (fractionDone > 0 ? fractionDone : 1e-6));
                                    long estRemaining = estTotal - elapsed;
                                    String timeStr = String.format(" | ETA: %ds", estRemaining / 1000);
                                    bar.append("] ").append(percent).append("%").append(timeStr);
                                    System.out.print("\r" + bar.toString());
                                    // Update ImageJ progress bar
                                    IJ.showProgress((i + 1) / (double) nPeriods);
                                    // Update only the last line for this planet
                                    int totalLines = progressWin.getTextPanel().getLineCount();
                                    progressWin.getTextPanel().setLine(progressLine, String.format("Planet %d: %s", planet + 1, bar.toString()));
                                }
                            } catch (Exception e) {
                                power[i] = Double.NaN;
                                bestDurations[i] = Double.NaN;
                                bestDepths[i] = Double.NaN;
                                bestPhases[i] = Double.NaN;
                            }
                        }

                        // Properly shutdown executor if not already done
                        if (!executor.isShutdown()) {
                            executor.shutdown();
                            try {
                                executor.awaitTermination(5, TimeUnit.SECONDS);
                            } catch (InterruptedException e) {
                                executor.shutdownNow();
                            }
                        }

                        IJ.showProgress(1.0); // Clear progress bar at end
                        System.out.println();
                        // Mark this planet as done
                        progressWin.getTextPanel().setLine(progressLine, String.format("Planet %d: Done!", planet + 1));
                        // Update overall progress bar
                        int planetsDone = planet + 1;
                        int percent = (int) Math.round(100.0 * planetsDone / nPlanets);
                        int nStars = (int) Math.round(50.0 * planetsDone / nPlanets);
                        StringBuilder bar = new StringBuilder();
                        for (int s = 0; s < nStars; s++) bar.append("*");
                        for (int s = nStars; s < 50; s++) bar.append(" ");
                        progressWin.getTextPanel().setLine(overallProgressLine, String.format("Overall Progress: [%s] %d%% (%d/%d planets finished)", bar.toString(), percent, planetsDone, nPlanets));
                        double modelCenterTimeForMasking = Double.NaN;
                        double minAverageT0 = Double.NaN;
                        // --- Phase-folded sliding trapezoid Huber loss T0 refinement ---
                        if (bestPeriod > 0 && bestDuration > 0) {
                            // Calculate minAverageT0 as in the min average method
                            {
                                int nBins = 200;
                                int minPointsPerBin = 5;
                                double[] phasesTmp = new double[n];
                                for (int i = 0; i < n; i++) {
                                    double phase = (sortedTime[i] - (bjdOffset + bestPhase + 0.5 * bestDuration)) / bestPeriod;
                                    phase = phase - Math.floor(phase);
                                    phasesTmp[i] = phase;
                                }
                                List<List<Double>> binFluxes = new ArrayList<>();
                                for (int i = 0; i < nBins; i++) binFluxes.add(new ArrayList<>());
                                for (int i = 0; i < n; i++) {
                                    int bin = (int) Math.floor(phasesTmp[i] * nBins);
                                    if (bin < 0) bin = 0;
                                    if (bin >= nBins) bin = nBins - 1;
                                    binFluxes.get(bin).add(sortedFlux[i]);
                                }
                                double minPhase = getMinPhase(nBins, binFluxes, minPointsPerBin);
                                minAverageT0 = (bjdOffset + bestPhase + 0.5 * bestDuration) + minPhase * bestPeriod;
                            }
                            System.out.printf("[DIAG] minAverageT0 = %.6f\n", minAverageT0);
                            // Print the phase for the time closest to minAverageT0 (using robust formula)
                            double minDt = Double.POSITIVE_INFINITY;
                            int minIdx = -1;
                            for (int i = 0; i < t.length; i++) {
                                double dt = Math.abs(t[i] - minAverageT0);
                                if (dt < minDt) {
                                    minDt = dt;
                                    minIdx = i;
                                }
                            }
                            double phaseAtMinAverageT0 = (t[minIdx] - minAverageT0) / bestPeriod;
                            phaseAtMinAverageT0 = phaseAtMinAverageT0 - Math.floor(phaseAtMinAverageT0);
                            System.out.printf("[DIAG] Closest time to minAverageT0: t = %.6f, phase = %.6f\n", t[minIdx], phaseAtMinAverageT0);
                            // Phase-fold the data using t[0] as reference (fully independent of minAverageT0)
                            double[] phases = new double[t.length];
                            double phaseRef = t[0];
                            for (int i = 0; i < t.length; i++) {
                                phases[i] = (t[i] - phaseRef) / bestPeriod;
                                phases[i] = phases[i] - Math.floor(phases[i]);
                            }
                            // Scan over ingress/egress fraction (tauFrac)
                            double minTauFrac = 0.01; // allow sharper transits
                            double maxTauFrac = 0.99;  // allow broader transits (changed from 0.9)
                            double tauFracStep = 0.01; // finer tauFrac grid
                            int nTau = (int) Math.round((maxTauFrac - minTauFrac) / tauFracStep) + 1;
                            double bestLoss = Double.POSITIVE_INFINITY;
                            double bestPhaseOffset = 0;
                            double bestTauFrac = 0;
                            // Phase step size: (duration/200) in phase units
                            double phaseStep = (bestDuration / bestPeriod) / 200.0;
                            int nPhaseSteps = (int) Math.ceil(1.0 / phaseStep);
                            double[] bestModel = new double[t.length];
                            double[] lossGrid = new double[nTau * nPhaseSteps];
                            double[] tauFracGrid = new double[nTau * nPhaseSteps];
                            double[] phaseOffsetGrid = new double[nTau * nPhaseSteps];
                            int gridIdx = 0;
                            double delta = 0.002; // Huber loss parameter (slightly less robust)
                            for (int tauIdx = 0; tauIdx < nTau; tauIdx++) {
                                if (blsCancelled.get()) break;
                                double tauFrac = minTauFrac + tauIdx * tauFracStep;
                                double tauPhase = tauFrac * (bestDuration / bestPeriod);
                                double flatPhase = (bestDuration / bestPeriod) - 2 * tauPhase;
                                if (flatPhase < 0) continue; // skip unphysical
                                for (int s = 0; s < nPhaseSteps; s++) {
                                    if (blsCancelled.get()) break;
                                    double phaseOffset = s * phaseStep * bestPeriod;
                                    double[] model = new double[t.length];
                                    double[] residuals = new double[t.length];
                                    int nUsed = 0;
                                    for (int j = 0; j < t.length; j++) {
                                        double phase = phases[j] - (phaseOffset / bestPeriod);
                                        phase = phase - Math.floor(phase);
                                        // Only use points within ±1.5*duration (in phase units) of the model center
                                        double dphase = Math.abs(phase);
                                        if (dphase > 0.5) dphase = 1.0 - dphase; // wrap around
                                        double window = 1.5 * (bestDuration / bestPeriod);
                                        if (dphase <= window) {
                                            if (phase < tauPhase) {
                                                model[j] = -bestDepth * (phase / tauPhase);
                                            } else if (phase < tauPhase + flatPhase) {
                                                model[j] = -bestDepth;
                                            } else if (phase < 2 * tauPhase + flatPhase) {
                                                model[j] = -bestDepth * (1 - (phase - tauPhase - flatPhase) / tauPhase);
                                            } else {
                                                model[j] = 0.0;
                                            }
                                            residuals[nUsed] = f[j] - model[j];
                                            nUsed++;
                                        }
                                    }
                                    if (nUsed > 0) {
                                        double[] usedResiduals = new double[nUsed];
                                        System.arraycopy(residuals, 0, usedResiduals, 0, nUsed);
                                        double loss = huberLoss(usedResiduals, delta);
                                        lossGrid[gridIdx] = loss;
                                        tauFracGrid[gridIdx] = tauFrac;
                                        phaseOffsetGrid[gridIdx] = phaseOffset;
                                        if (loss < bestLoss) {
                                            bestLoss = loss;
                                            bestPhaseOffset = phaseOffset;
                                            bestTauFrac = tauFrac;
                                            // Save the best model for plotting (for all points, not just window)
                                            for (int j = 0; j < t.length; j++) {
                                                double phaseForPlot = phases[j] - (bestPhaseOffset / bestPeriod);
                                                phaseForPlot = phaseForPlot - Math.floor(phaseForPlot);
                                                if (phaseForPlot < bestTauFrac * (bestDuration / bestPeriod)) {
                                                    bestModel[j] = -bestDepth * (phaseForPlot / (bestTauFrac * (bestDuration / bestPeriod)));
                                                } else if (phaseForPlot < bestTauFrac * (bestDuration / bestPeriod) + (bestDuration / bestPeriod) - 2 * bestTauFrac * (bestDuration / bestPeriod)) {
                                                    bestModel[j] = -bestDepth;
                                                } else if (phaseForPlot < 2 * bestTauFrac * (bestDuration / bestPeriod) + (bestDuration / bestPeriod) - 2 * bestTauFrac * (bestDuration / bestPeriod)) {
                                                    bestModel[j] = -bestDepth * (1 - (phaseForPlot - bestTauFrac * (bestDuration / bestPeriod) - ((bestDuration / bestPeriod) - 2 * bestTauFrac * (bestDuration / bestPeriod))) / (bestTauFrac * (bestDuration / bestPeriod)));
                                                } else {
                                                    bestModel[j] = 0.0;
                                                }
                                            }
                                        }
                                    } else {
                                        lossGrid[gridIdx] = Double.POSITIVE_INFINITY;
                                        tauFracGrid[gridIdx] = tauFrac;
                                        phaseOffsetGrid[gridIdx] = phaseOffset;
                                    }
                                    gridIdx++;
                                }
                                if (blsCancelled.get()) break;
                            }
                            // Convert phase offset back to T0 (using t[0] as reference)
                            double bestT0 = phaseRef + bestPhaseOffset;
                            System.out.printf("Phase-folded sliding trapezoid Huber loss T0 refinement: best phase offset = %.6f, best tauFrac = %.3f, best T0 = %.6f, Huber loss = %.6g\n", bestPhaseOffset, bestTauFrac, bestT0, bestLoss);
                            // Store the sliding box T0 for the best peak (first peak in this iteration)
                            // Remove this block since t0_sliding_box is not a field anymore
                            // if (allPeaks.size() > 0) {
                            //     PeakResult lastPeak = allPeaks.get(allPeaks.size() - 1);
                            //     if (lastPeak.iteration == planet + 1 && lastPeak.peakNumber == 1) {
                            //         lastPeak.t0_sliding_box = bestT0;
                            //     }
                            // }
                            // Plot Huber loss vs. phase offset for best tauFrac
                            double[] phaseOffsetsPlot = new double[nPhaseSteps];
                            double[] lossValsPlot = new double[nPhaseSteps];
                            int plotIdx = 0;
                            for (int s = 0; s < nPhaseSteps; s++) {
                                double minLoss = Double.POSITIVE_INFINITY;
                                for (int tauIdx = 0; tauIdx < nTau; tauIdx++) {
                                    int gridIndex = tauIdx * nPhaseSteps + s;
                                    if (gridIndex < gridIdx && tauFracGrid[gridIndex] == bestTauFrac && lossGrid[gridIndex] < minLoss) {
                                        minLoss = lossGrid[gridIndex];
                                    }
                                }
                                phaseOffsetsPlot[plotIdx] = s * bestPeriod / nPhaseSteps;
                                lossValsPlot[plotIdx] = minLoss;
                                plotIdx++;
                            }
                            Plot lossPlot = new Plot(String.format("Phase-Folded Sliding Trapezoid Huber Loss vs. Phase Offset (Planet %d)", planet + 1), "Phase Offset", "Huber Loss");
                            lossPlot.addPoints(phaseOffsetsPlot, lossValsPlot, Plot.LINE);
                            lossPlot.addLabel(0.01, 0.95, String.format("Best T0 = %.6f, tauFrac = %.3f", bestT0, bestTauFrac));
                            lossPlot.show();
                            // Optionally plot phase-folded data with best-fit trapezoid model
                            Plot phasePlot = new Plot(String.format("Phase-Folded Data with Trapezoid Model (Planet %d)", planet + 1), "Phase", "Flux");
                            phasePlot.addPoints(phases, f, Plot.LINE);
                            phasePlot.setLimits(0, 1, -bestDepth * 1.2, bestDepth * 0.2);
                            // Sort phases and bestModel for plotting the model as a line
                            double[] sortedPhases = phases.clone();
                            double[] sortedModel = bestModel.clone();
                            int nPoints = sortedPhases.length;
                            Integer[] order = new Integer[nPoints];
                            for (int i = 0; i < nPoints; i++) order[i] = i;
                            Arrays.sort(order, Comparator.comparingDouble(i -> sortedPhases[i]));
                            double[] sortedPhasesFinal = new double[nPoints];
                            double[] sortedModelFinal = new double[nPoints];
                            for (int i = 0; i < nPoints; i++) {
                                sortedPhasesFinal[i] = sortedPhases[order[i]];
                                sortedModelFinal[i] = sortedModel[order[i]];
                            }
                            phasePlot.addPoints(sortedPhasesFinal, sortedModelFinal, Plot.LINE);
                            phasePlot.addLabel(0.01, 0.95, String.format("Best T0 = %.6f, tauFrac = %.3f", bestT0, bestTauFrac));
                            // Add vertical lines for phase=0 and model center (phase=bestPhaseOffset/bestPeriod)
                            double[] vlineX = {0, bestPhaseOffset / bestPeriod};
                            double[] vlineY = {-bestDepth * 1.2, bestDepth * 0.2};
                            // Draw vertical line at phase=0
                            phasePlot.setColor(Color.RED);
                            phasePlot.addPoints(new double[]{0, 0}, new double[]{-bestDepth * 1.2, bestDepth * 0.2}, Plot.LINE);
                            // Draw vertical line at phase=bestPhaseOffset/bestPeriod
                            phasePlot.setColor(Color.BLUE);
                            phasePlot.addPoints(new double[]{bestPhaseOffset / bestPeriod, bestPhaseOffset / bestPeriod}, new double[]{-bestDepth * 1.2, bestDepth * 0.2}, Plot.LINE);
                            phasePlot.setColor(Color.BLACK);
                            phasePlot.show();
                            // Print corresponding times for minAverageT0 and model center
                            double modelCenterTime = bestT0;
                            modelCenterTimeForMasking = modelCenterTime;
                            System.out.printf("[DIAG] minAverageT0 (phase=0): %.6f\n", minAverageT0);
                            System.out.printf("[DIAG] Model center (phase=%.6f): %.6f\n", bestPhaseOffset / bestPeriod, modelCenterTime);
                        }
                        // Find the index of the highest peak (bestIdx)
                        int bestIdx = 0;
                        for (int i = 1; i < power.length; i++) {
                            if (power[i] > power[bestIdx]) bestIdx = i;
                        }
                        double peakPower = power[bestIdx];
                        double cutoff = peakPower * userCutoff;
                        int left = bestIdx, right = bestIdx;
                        while (left > 0 && power[left] > cutoff) left--;
                        while (right < power.length - 1 && power[right] > cutoff) right++;
                        int peakWidth = right - left;
                        int minSeparation = Math.max(1, peakWidth);
                        List<Integer> peakIndices = findPeaks(power, minSeparation);
                        peakIndices.sort((a, b) -> Double.compare(power[b], power[a]));
                        int nTopPeaks = Math.min(userNumPeaks, peakIndices.size());
                        List<Integer> topPeaks = peakIndices.subList(0, nTopPeaks);
                        if (bestIdx != -1 && (topPeaks.isEmpty() || topPeaks.get(topPeaks.size() - 1) != bestIdx)) {
                            topPeaks.add(bestIdx);
                        }
                        List<Integer> uniqueTopPeaks = new ArrayList<>();
                        for (int i = 0; i < topPeaks.size(); i++) {
                            int idx2 = topPeaks.get(i);
                            if (!uniqueTopPeaks.contains(idx2)) uniqueTopPeaks.add(idx2);
                        }
                        topPeaks = uniqueTopPeaks;
                        int nLabelPeaks = Math.min(userNumPeaks, topPeaks.size());
                        List<Integer> sortedTopPeaks = new ArrayList<>(topPeaks.subList(topPeaks.size() - nLabelPeaks, topPeaks.size()));
                        sortedTopPeaks.sort((a, b) -> Double.compare(power[b], power[a]));
                        // After calculating t0_ref_masking for both methods, store them for each peak
                        // Model center (sliding fit)
                        double t0_ref_masking_model_center = Double.NaN;
                        if (!Double.isNaN(modelCenterTimeForMasking) && !Double.isNaN(bestPeriod)) {
                            // Use minPhase from the masking block
                            int nBins = 200;
                            int minPointsPerBin = 5;
                            double[] phases_model_center = new double[n];
                            for (int i = 0; i < n; i++) {
                                double phase = (sortedTime[i] - modelCenterTimeForMasking) / bestPeriod;
                                phase = phase - Math.floor(phase);
                                phases_model_center[i] = phase;
                            }
                            List<List<Double>> binFluxes = new ArrayList<>();
                            for (int i = 0; i < nBins; i++) binFluxes.add(new ArrayList<>());
                            for (int i = 0; i < n; i++) {
                                int bin = (int) Math.floor(phases_model_center[i] * nBins);
                                if (bin < 0) bin = 0;
                                if (bin >= nBins) bin = nBins - 1;
                                binFluxes.get(bin).add(sortedFlux[i]);
                            }
                            double minPhase = getMinPhase(nBins, binFluxes, minPointsPerBin);
                            t0_ref_masking_model_center = modelCenterTimeForMasking + minPhase * bestPeriod;
                        }
                        // Min average (phase bin)
                        double t0_ref_masking_min_avg = Double.NaN;
                        if (!Double.isNaN(minAverageT0) && !Double.isNaN(bestPeriod)) {
                            int nBins = 200;
                            int minPointsPerBin = 5;
                            double[] phases_min_avg = new double[n];
                            for (int i = 0; i < n; i++) {
                                double phase = (sortedTime[i] - minAverageT0) / bestPeriod;
                                phase = phase - Math.floor(phase);
                                phases_min_avg[i] = phase;
                            }
                            List<List<Double>> binFluxes = new ArrayList<>();
                            for (int i = 0; i < nBins; i++) binFluxes.add(new ArrayList<>());
                            for (int i = 0; i < n; i++) {
                                int bin = (int) Math.floor(phases_min_avg[i] * nBins);
                                if (bin < 0) bin = 0;
                                if (bin >= nBins) bin = nBins - 1;
                                binFluxes.get(bin).add(sortedFlux[i]);
                            }
                            double minPhase = getMinPhase(nBins, binFluxes, minPointsPerBin);
                            t0_ref_masking_min_avg = minAverageT0 + minPhase * bestPeriod;
                        }
                        // Save all peaks for this iteration
                        for (int i = 0; i < nLabelPeaks; i++) {
                            int peakIdx = sortedTopPeaks.get(i);
                            allPeaks.add(new PeakResult(planet + 1, i + 1, periods[peakIdx], power[peakIdx], bestDurations[peakIdx], bestDepths[peakIdx], bestPhases[peakIdx], t0_ref_masking_model_center, t0_ref_masking_min_avg));
                        }
                        // Improved soft mask: robust phase-centering for all in-transit points
                        if (!Double.isNaN(bestPeriod) && !Double.isNaN(bestPhase) && !Double.isNaN(bestDuration) && (!Double.isNaN(modelCenterTimeForMasking) || !Double.isNaN(minAverageT0))) {
                            // Use user-selected T0 for masking
                            double t0_ref_masking = modelCenterTimeForMasking;
                            if (t0MaskingChoice.equals("Min average (phase bin)")) {
                                t0_ref_masking = minAverageT0;
                            }
                            int nBins = 200;
                            int minPointsPerBin = 5;
                            double[] phases = new double[n];
                            for (int i = 0; i < n; i++) {
                                double phase = (sortedTime[i] - t0_ref_masking) / bestPeriod;
                                phase = phase - Math.floor(phase);
                                phases[i] = phase;
                            }
                            // Bin the data
                            List<List<Double>> binFluxes = new ArrayList<>();
                            for (int i = 0; i < nBins; i++) binFluxes.add(new ArrayList<>());
                            for (int i = 0; i < n; i++) {
                                int bin = (int) Math.floor(phases[i] * nBins);
                                if (bin < 0) bin = 0;
                                if (bin >= nBins) bin = nBins - 1;
                                binFluxes.get(bin).add(sortedFlux[i]);
                            }
                            double minPhase = getMinPhase(nBins, binFluxes, minPointsPerBin);
                            t0_ref_masking = t0_ref_masking + minPhase * bestPeriod;
                            // Mask in-transit points using this T0
                            double halfMask = 0.5 * maskFactor * bestDuration;
                            for (int i = 0; i < n; i++) {
                                if (masked[i]) continue;
                                double phase = ((sortedTime[i] - t0_ref_masking + 0.5 * bestPeriod) % bestPeriod) - 0.5 * bestPeriod;
                                if (Math.abs(phase) < halfMask) {
                                    masked[i] = true;
                                }
                            }
                            int nMasked = 0;
                            for (int i = 0; i < n; i++) if (masked[i]) nMasked++;
                            System.out.printf("After masking: %d points masked out of %d (%.2f%%)\n", nMasked, n, 100.0 * nMasked / n);
                            int nShouldBeMasked = 0;
                            for (int i = 0; i < n; i++) {
                                double phase2 = ((sortedTime[i] - t0_ref_masking + 0.5 * bestPeriod) % bestPeriod) - 0.5 * bestPeriod;
                                if (Math.abs(phase2) < halfMask) {
                                    nShouldBeMasked++;
                                }
                            }
                            System.out.printf("Points that should be masked by window: %d out of %d (%.2f%%)\n", nShouldBeMasked, n, 100.0 * nShouldBeMasked / n);
                            System.out.printf("Iteration %d: Detected period = %.6f, T0 (masking, %s) = %.6f, duration = %.6f, depth = %.6f, halfMask = %.6f\n",
                                    planet + 1, bestPeriod, t0MaskingChoice, t0_ref_masking, bestDuration, bestDepth, halfMask);
                        }
                        // Show periodogram plot for this iteration
                        Plot plot = new Plot("BLS Periodogram (Planet " + (planet + 1) + ")", "Period", "Power");
                        plot.setLimits(minPeriod, maxPeriod, 0, bestPower * 1.1);
                        plot.addPoints(periods, power, Plot.DOT);
                        plot.addPoints(periods, power, Plot.LINE);

                        // Highlight the most dominant peak with vertical shaded band
                        if (bestIdx >= 0 && bestIdx < periods.length) {
                            double dominantPeriod = periods[bestIdx];
                            double dominantPower = power[bestIdx];

                            // Calculate peak width for shading
                            double highlightPeakPower = power[bestIdx];
                            double highlightCutoff = highlightPeakPower * userCutoff;
                            int highlightLeft = bestIdx, highlightRight = bestIdx;
                            while (highlightLeft > 0 && power[highlightLeft] > highlightCutoff) {
                                highlightLeft--;
                            }
                            while (highlightRight < power.length - 1 && power[highlightRight] > highlightCutoff) {
                                highlightRight++;
                            }

                            // Create shaded vertical band around dominant peak
                            double bandLeft = periods[highlightLeft];
                            double bandRight = periods[highlightRight];
                            double yMin = 0;
                            double yMax = bestPower * 1.1;

                            // Draw a single vertical line at the dominant peak center instead of filled rectangle
                            plot.setColor(Color.RED); // Red
                            plot.addPoints(new double[]{dominantPeriod, dominantPeriod}, new double[]{yMin, yMax}, Plot.LINE);

                            // Reset color to black for other elements
                            plot.setColor(Color.BLACK);

                            // Add orange vertical lines for harmonics/aliases of the dominant peak only
                            plot.setColor(new Color(255, 165, 0)); // Orange
                            // First harmonic (2x period)
                            double harmonic1 = dominantPeriod * 2.0;
                            if (harmonic1 >= minPeriod && harmonic1 <= maxPeriod) {
                                plot.addPoints(new double[]{harmonic1, harmonic1}, new double[]{yMin, yMax}, Plot.LINE);
                            }
                            // Second harmonic (3x period)
                            double harmonic2 = dominantPeriod * 3.0;
                            if (harmonic2 >= minPeriod && harmonic2 <= maxPeriod) {
                                plot.addPoints(new double[]{harmonic2, harmonic2}, new double[]{yMin, yMax}, Plot.LINE);
                            }
                            // Sub-harmonic (0.5x period)
                            double subHarmonic = dominantPeriod * 0.5;
                            if (subHarmonic >= minPeriod && subHarmonic <= maxPeriod) {
                                plot.addPoints(new double[]{subHarmonic, subHarmonic}, new double[]{yMin, yMax}, Plot.LINE);
                            }

                            // Reset color to black
                            plot.setColor(Color.BLACK);
                        }

                        // Label top peaks with T0 for the dominant peak
                        for (int i = 0; i < nLabelPeaks; i++) {
                            int peakIdx = sortedTopPeaks.get(i);
                            double peakPeriod = periods[peakIdx];
                            if (i == 0 && peakIdx == bestIdx) {
                                // For the dominant peak (Peak 1), add T0 information
                                String t0Value = "";
                                if (t0MaskingChoice.equals("Min average (phase bin)")) {
                                    t0Value = Double.isNaN(minAverageT0) ? "N/A" : String.format("T0=%.3f", minAverageT0);
                                } else {
                                    t0Value = Double.isNaN(modelCenterTimeForMasking) ? "N/A" : String.format("T0=%.3f", modelCenterTimeForMasking);
                                }
                                plot.addLabel(0.01, 0.06 + i * 0.05, String.format("Peak %d: P=%.4f %s", i + 1, peakPeriod, t0Value));
                            } else {
                                plot.addLabel(0.01, 0.06 + i * 0.05, String.format("Peak %d: P=%.4f", i + 1, peakPeriod));
                            }
                        }
                        plot.show();
                        plot.update();
                    }
                    // After all planets, update the progress window to show completion
                    progressWin.getTextPanel().append("\nAll planet searches complete.\n");
                    // Show results in a table
                    StringBuilder resultMsg = new StringBuilder();
                    String t0ColLabel = t0MaskingChoice.equals("Min average (phase bin)") ? "T0 (masking, Min avg)" : "T0 (masking, Model center)";
                    String header = String.format("%-4s %-4s %-12s %-10s %-14s %-10s %-12s %-24s\n",
                            "Iter", "Pk", "Period", "Power", "Duration (hr)", "Depth", "Phase", t0ColLabel);
                    String sep = String.format("%-4s %-4s %-12s %-10s %-14s %-10s %-12s %-24s\n",
                            "----", "---", "------------", "----------", "--------------", "----------", "------------", "------------------------");
                    resultMsg.append(header).append(sep);
                    int lastIter = -1;
                    for (PeakResult pr : allPeaks) {
                        if (lastIter != -1 && pr.iteration != lastIter) resultMsg.append("\n");
                        String t0Val = t0MaskingChoice.equals("Min average (phase bin)")
                                ? (Double.isNaN(pr.t0_masking_min_avg) ? "" : String.format("%.6f", pr.t0_masking_min_avg))
                                : (Double.isNaN(pr.t0_masking_model_center) ? "" : String.format("%.6f", pr.t0_masking_model_center));
                        double durationHours = pr.duration * 24.0;
                        resultMsg.append(String.format("%4d %3d %12.6f %10.4f %14.4f %10.4f %12.6f %24s\n",
                                pr.iteration, pr.peakNumber, pr.period, pr.power, durationHours, pr.depth, pr.phase, t0Val));
                        lastIter = pr.iteration;
                    }
                    // Show results in a JTable with Save as CSV button
                    SwingUtilities.invokeLater(() -> 
                            createResultsTable(new String[]{"Iter", "Pk", "Period", "Power", "Duration (hr)", 
                                            "Depth", "Phase", "T0 (masking, Model center)"
                                    },
                                    resultMsg, "BLS Multi-Planet Results"));
                }).start();
            } else {
                IJ.showMessage("Periodogram", "BLS is only implemented for Measurements Table.");
                return;
            }
        }

        // Lomb-Scargle implementation (replaces FFT)
        if (algorithm.equals("Lomb-Scargle")) {
            double[] time = null;
            double[] flux = null;
            if (dataSource.equals("Current Image")) {
                ImagePlus imp = WindowManager.getCurrentImage();
                if (imp == null) {
                    IJ.showMessage("Periodogram", "No image open.");
                    return;
                }
                int width = imp.getWidth();
                int height = imp.getHeight();
                float[] pixels = null;
                if (imp.getStackSize() > 1) {
                    pixels = (float[]) imp.getStack().getProcessor(1).convertToFloat().getPixels();
                } else {
                    pixels = (float[]) imp.getProcessor().convertToFloat().getPixels();
                }
                time = new double[height];
                flux = new double[height];
                for (int y = 0; y < height; y++) {
                    double sum = 0;
                    for (int x = 0; x < width; x++) {
                        sum += pixels[y * width + x];
                    }
                    time[y] = y;
                    flux[y] = sum / width;
                }
            } else if (dataSource.equals("Synthetic")) {
                int n = 128;
                time = new double[n];
                flux = new double[n];
                for (int i = 0; i < n; i++) {
                    time[i] = i;
                    flux[i] = Math.sin(2 * Math.PI * i / 16.0) + 0.5 * Math.sin(2 * Math.PI * i / 32.0);
                }
            } else if (isMeasurementTable) {
                MeasurementTable table = MeasurementTable.getTable(dataSource);
                if (table == null) {
                    IJ.showMessage("Periodogram", "Could not open table.");
                    return;
                }
                String[] columns = table.getColumnHeadings().split("\t");
                if (columns.length < 2) {
                    IJ.showMessage("Periodogram", "Not enough columns in table.");
                    return;
                }
                GenericDialog gdCol = new GenericDialog("Lomb-Scargle Settings");
                gdCol.addChoice("Time column", columns, TIME_COL.get().isEmpty() ? columns[0] : TIME_COL.get());
                gdCol.addChoice("Flux column", columns, FLUX_COL.get().isEmpty() ? columns[1] : FLUX_COL.get());
                gdCol.addNumericField("Min period", MIN_PERIOD.get(), 6, 12, null); // 0 means auto
                gdCol.addNumericField("Max period", MAX_PERIOD.get(), 6, 12, null); // 0 means auto
                gdCol.addNumericField("Steps", STEPS.get(), 0, 6, null);
                gdCol.addNumericField("Peak width cutoff (0-1)", PEAK_WIDTH_CUTOFF.get(), 2, 6, null);
                gdCol.addNumericField("Number of peaks", PEAK_COUNT.get(), 0, 6, null);
                gdCol.addNumericField("Number of planets to search", PLANET_COUNT.get(), 0);
                gdCol.addNumericField("Masking factor (in period phase)", MASK_FACTOR_PERIOD.get(), 3);
                gdCol.showDialog();
                if (gdCol.wasCanceled()) return;
                String timeCol = gdCol.getNextChoice();
                String fluxCol = gdCol.getNextChoice();
                double minPeriodUser = gdCol.getNextNumber();
                double maxPeriodUser = gdCol.getNextNumber();
                int nPeriods = (int) gdCol.getNextNumber();
                double userCutoff = gdCol.getNextNumber();
                int userNumPeaks = (int) gdCol.getNextNumber();
                int nPlanets = (int) gdCol.getNextNumber();
                double maskFactor = gdCol.getNextNumber();
                TIME_COL.set(timeCol);
                FLUX_COL.set(fluxCol);
                MIN_PERIOD.set(minPeriodUser);
                MAX_PERIOD.set(maxPeriodUser);
                STEPS.set((double) nPeriods);
                PEAK_WIDTH_CUTOFF.set((double) userNumPeaks);
                PLANET_COUNT.set((double) nPlanets);
                MASK_FACTOR_PERIOD.set(maskFactor);

                time = table.getDoubleColumn(table.getColumnIndex(timeCol));
                flux = table.getDoubleColumn(table.getColumnIndex(fluxCol));
                if (time == null || flux == null || time.length != flux.length || time.length < 2) {
                    IJ.showMessage("Periodogram", "Invalid or insufficient data.");
                    return;
                }
                // Sort by time
                int n = time.length;
                double[] sortedTime = new double[n];
                double[] sortedFlux = new double[n];
                Integer[] idx = new Integer[n];
                for (int i = 0; i < n; i++) idx[i] = i;
                final double[] timeToSort = time;
                Arrays.sort(idx, Comparator.comparingDouble(i2 -> timeToSort[i2]));
                for (int i = 0; i < n; i++) {
                    sortedTime[i] = time[idx[i]];
                    sortedFlux[i] = flux[idx[i]];
                }
                // Prepare for iterative search
                boolean[] masked = new boolean[n];
                List<LSResult> allResults = new ArrayList<>();
                TextWindow progressWin = new TextWindow("Lomb-Scargle Progress", "", 600, 600);
                progressWin.getTextPanel().append(String.format("Lomb-Scargle Multi-Planet Progress (%d planets)\n", nPlanets));
                progressWin.getTextPanel().append(String.format("Overall Progress: [%s] %d%% (0/%d planets finished)\n", "                    ", 0, nPlanets));
                int overallProgressLine = progressWin.getTextPanel().getLineCount() - 1;
                // Add ability to cancel Lomb-Scargle run
                final AtomicBoolean lsCancelled = new AtomicBoolean(false);
                Button lsStopButton = new Button("Stop");
                lsStopButton.addActionListener(e -> {
                    lsCancelled.set(true);
                    progressWin.getTextPanel().append("\nLomb-Scargle run cancelled by user.\n");
                });
                Panel lsButtonPanel = new Panel();
                lsButtonPanel.add(lsStopButton);
                progressWin.add(lsButtonPanel, BorderLayout.SOUTH);
                progressWin.pack();
                for (int planet = 0; planet < nPlanets; planet++) {
                    if (lsCancelled.get()) break;
                    progressWin.getTextPanel().append(String.format("Planet %d: Starting...\n", planet + 1));
                    int progressLine = progressWin.getTextPanel().getLineCount() - 1;
                    // Build unmasked arrays for this run
                    List<Integer> unmaskedIdx = new ArrayList<>();
                    for (int i = 0; i < n; i++) if (!masked[i]) unmaskedIdx.add(i);
                    double[] t = new double[unmaskedIdx.size()];
                    double[] f = new double[unmaskedIdx.size()];
                    for (int i = 0; i < t.length; i++) {
                        t[i] = sortedTime[unmaskedIdx.get(i)];
                        f[i] = sortedFlux[unmaskedIdx.get(i)];
                    }
                    if (t.length < 10) break;
                    // Set period/frequency grid
                    double tMin = t[0], tMax = t[t.length-1];
                    double minPeriod = minPeriodUser > 0 ? minPeriodUser : (tMax - tMin) / 20.0;
                    double maxPeriod = maxPeriodUser > 0 ? maxPeriodUser : (tMax - tMin) * 0.8;
                    if (minPeriod <= 0 || maxPeriod <= 0 || minPeriod >= maxPeriod || nPeriods < 2) break;
                    double minFreq = 1.0 / maxPeriod;
                    double maxFreq = 1.0 / minPeriod;
                    double[] freq = new double[nPeriods];
                    double[] power = new double[nPeriods];
                    for (int i = 0; i < nPeriods; i++) {
                        freq[i] = minFreq + i * (maxFreq - minFreq) / (nPeriods - 1);
                    }
                    double mean = 0, var = 0;
                    for (double v : f) mean += v;
                    mean /= f.length;
                    for (double v : f) var += (v - mean) * (v - mean);
                    var /= f.length;
                    for (int i = 0; i < nPeriods; i++) {
                        if (lsCancelled.get()) break;
                        double w = 2 * Math.PI * freq[i];
                        double tau = 0.0;
                        double s2wt = 0.0, c2wt = 0.0;
                        for (double tt : t) {
                            s2wt += Math.sin(2 * w * tt);
                            c2wt += Math.cos(2 * w * tt);
                        }
                        tau = Math.atan2(s2wt, c2wt) / (2 * w);
                        double cosTerm = 0.0, sinTerm = 0.0, cos2Term = 0.0, sin2Term = 0.0;
                        for (int j = 0; j < t.length; j++) {
                            double theta = w * (t[j] - tau);
                            double c = Math.cos(theta);
                            double s = Math.sin(theta);
                            cosTerm += (f[j] - mean) * c;
                            sinTerm += (f[j] - mean) * s;
                            cos2Term += c * c;
                            sin2Term += s * s;
                        }
                        power[i] = (cosTerm * cosTerm / cos2Term + sinTerm * sinTerm / sin2Term) / (2 * var);
                    }
                    double[] periods = new double[nPeriods];
                    for (int i = 0; i < nPeriods; i++) periods[i] = 1.0 / freq[i];
                    // Find peaks
                    int bestIdx = 0;
                    for (int i = 1; i < power.length; i++) {
                        if (power[i] > power[bestIdx]) bestIdx = i;
                    }
                    double peakPower = power[bestIdx];
                    double cutoff = peakPower * userCutoff;
                    int left = bestIdx, right = bestIdx;
                    while (left > 0 && power[left] > cutoff) left--;
                    while (right < power.length - 1 && power[right] > cutoff) right++;
                    int peakWidth = right - left;
                    int minSeparation = Math.max(1, peakWidth);
                    List<Integer> peakIndices = findPeaks(power, minSeparation);
                    peakIndices.sort((a, b) -> Double.compare(power[b], power[a]));
                    int nTopPeaks = Math.min(userNumPeaks, peakIndices.size());
                    List<Integer> topPeaks = peakIndices.subList(0, nTopPeaks);
                    if (bestIdx != -1 && (topPeaks.isEmpty() || topPeaks.get(topPeaks.size() - 1) != bestIdx)) {
                        topPeaks.add(bestIdx);
                    }
                    List<Integer> uniqueTopPeaks = new ArrayList<>();
                    for (int i = 0; i < topPeaks.size(); i++) {
                        int idx2 = topPeaks.get(i);
                        if (!uniqueTopPeaks.contains(idx2)) uniqueTopPeaks.add(idx2);
                    }
                    topPeaks = uniqueTopPeaks;
                    int nLabelPeaks = Math.min(userNumPeaks, topPeaks.size());
                    List<Integer> sortedTopPeaks = new ArrayList<>(topPeaks.subList(topPeaks.size() - nLabelPeaks, topPeaks.size()));
                    sortedTopPeaks.sort((a, b) -> Double.compare(power[b], power[a]));
                    // Save all peaks for this iteration
                    for (int i = 0; i < nLabelPeaks; i++) {
                        int peakIdx = sortedTopPeaks.get(i);
                        double maskCenter = periods[peakIdx];
                        allResults.add(new LSResult(planet + 1, i + 1, periods[peakIdx], power[peakIdx], maskCenter));
                    }
                    // Mask in-phase points for next iteration (around best period)
                    if (!Double.isNaN(periods[bestIdx])) {
                        double bestPeriod = periods[bestIdx];
                        double bestPower = power[bestIdx];
                        double halfMask = 0.5 * maskFactor * bestPeriod;
                        for (int i = 0; i < n; i++) {
                            if (masked[i]) continue;
                            double phase = ((sortedTime[i] - t[0] + 0.5 * bestPeriod) % bestPeriod) - 0.5 * bestPeriod;
                            if (Math.abs(phase) < halfMask) {
                                masked[i] = true;
                            }
                        }
                    }
                    // Plot periodogram for this iteration
                    Plot plot = new Plot("Lomb-Scargle Periodogram (Planet " + (planet + 1) + ")", "Period", "Power");
                    plot.add("line", periods, power);

                    // Highlight the most dominant peak with vertical shaded band
                    if (bestIdx >= 0 && bestIdx < periods.length) {
                        double dominantPeriod = periods[bestIdx];
                        double dominantPower = power[bestIdx];

                        // Calculate peak width for shading
                        double highlightPeakPower = power[bestIdx];
                        double highlightCutoff = highlightPeakPower * userCutoff;
                        int highlightLeft = bestIdx, highlightRight = bestIdx;
                        while (highlightLeft > 0 && power[highlightLeft] > highlightCutoff) {
                            highlightLeft--;
                        }
                        while (highlightRight < power.length - 1 && power[highlightRight] > highlightCutoff) {
                            highlightRight++;
                        }

                        // Create shaded vertical band around dominant peak
                        double bandLeft = periods[highlightLeft];
                        double bandRight = periods[highlightRight];
                        double yMin = 0;
                        double yMax = dominantPower * 1.1;

                        // Draw a single vertical line at the dominant peak center instead of filled rectangle
                        plot.setColor(Color.RED); // Red
                        plot.addPoints(new double[]{dominantPeriod, dominantPeriod}, new double[]{yMin, yMax}, Plot.LINE);

                        // Reset color to black for other elements
                        plot.setColor(Color.BLACK);

                        // Add orange vertical lines for harmonics/aliases of the dominant peak only
                        plot.setColor(new Color(255, 165, 0)); // Orange
                        // First harmonic (2x period)
                        double harmonic1 = dominantPeriod * 2.0;
                        if (harmonic1 >= minPeriod && harmonic1 <= maxPeriod) {
                            plot.addPoints(new double[]{harmonic1, harmonic1}, new double[]{yMin, yMax}, Plot.LINE);
                        }
                        // Second harmonic (3x period)
                        double harmonic2 = dominantPeriod * 3.0;
                        if (harmonic2 >= minPeriod && harmonic2 <= maxPeriod) {
                            plot.addPoints(new double[]{harmonic2, harmonic2}, new double[]{yMin, yMax}, Plot.LINE);
                        }
                        // Sub-harmonic (0.5x period)
                        double subHarmonic = dominantPeriod * 0.5;
                        if (subHarmonic >= minPeriod && subHarmonic <= maxPeriod) {
                            plot.addPoints(new double[]{subHarmonic, subHarmonic}, new double[]{yMin, yMax}, Plot.LINE);
                        }

                        // Reset color to black
                        plot.setColor(Color.BLACK);
                    }

                    plot.addPoints(periods, power, Plot.LINE);
                    for (int i = 0; i < nLabelPeaks; i++) {
                        int peakIdx = sortedTopPeaks.get(i);
                        double peakPeriod = periods[peakIdx];
                        if (i == 0 && peakIdx == bestIdx) {
                            // For the dominant peak (Peak 1), add power information
                            plot.addLabel(0.01, 0.06 + i * 0.05, String.format("Peak %d: P=%.4f Power=%.4f", i + 1, peakPeriod, power[peakIdx]));
                        } else {
                            plot.addLabel(0.01, 0.06 + i * 0.05, String.format("Peak %d: P=%.4f", i + 1, peakPeriod));
                        }
                    }
                    plot.show();
                    plot.update();

                    // Check if cancelled after calculation
                    if (lsCancelled.get()) {
                        System.out.println("\nLomb-Scargle calculation cancelled by user.");
                        break;
                    }

                    // Update progress
                    int planetsDone = planet + 1;
                    int percent = (int) Math.round(100.0 * planetsDone / nPlanets);
                    int nStars = (int) Math.round(50.0 * planetsDone / nPlanets);
                    StringBuilder bar = new StringBuilder();
                    for (int s = 0; s < nStars; s++) bar.append("*");
                    for (int s = nStars; s < 50; s++) bar.append(" ");
                    progressWin.getTextPanel().setLine(overallProgressLine, String.format("Overall Progress: [%s] %d%% (%d/%d planets finished)", bar.toString(), percent, planetsDone, nPlanets));
                    progressWin.getTextPanel().setLine(progressLine, String.format("Planet %d: Done!", planet + 1));
                }
                progressWin.getTextPanel().append("\nAll planet searches complete.\n");
                // Show results in a table
                StringBuilder resultMsg = new StringBuilder();
                String header = String.format("%-4s %-4s %-12s %-10s %-14s\n", "Iter", "Pk", "Period", "Power", "Mask Center");
                String sep = String.format("%-4s %-4s %-12s %-10s %-14s\n", "----", "---", "------------", "----------", "--------------");
                resultMsg.append(header).append(sep);
                int lastIter = -1;
                for (LSResult res : allResults) {
                    if (lastIter != -1 && res.iteration != lastIter) resultMsg.append("\n");
                    resultMsg.append(String.format("%4d %3d %12.6f %10.4f %14.6f\n", res.iteration, res.peakNumber, res.period, res.power, res.maskCenter));
                    lastIter = res.iteration;
                }
                SwingUtilities.invokeLater(() -> {
                    createResultsTable(new String[]{"Iter", "Pk", "Period", "Power", "Mask Center"},
                            resultMsg, "Lomb-Scargle Multi-Planet Results");
                });
                return;
            }
            // For synthetic or image data, use default grid (single run, no masking)
            double[] timeArr = time;
            double[] fluxArr = flux;
            int n2 = timeArr.length;
            double tMin2 = timeArr[0], tMax2 = timeArr[n2-1];
            int nPeriods2 = 5000;
            double minPeriod2 = (tMax2 - tMin2) / 20.0;
            double maxPeriod2 = (tMax2 - tMin2) * 0.8;
            double minFreq2 = 1.0 / maxPeriod2;
            double maxFreq2 = 1.0 / minPeriod2;
            double[] freq2 = new double[nPeriods2];
            double[] power2 = new double[nPeriods2];
            for (int i = 0; i < nPeriods2; i++) {
                freq2[i] = minFreq2 + i * (maxFreq2 - minFreq2) / (nPeriods2 - 1);
            }
            double mean2 = 0, var2 = 0;
            for (double v : fluxArr) mean2 += v;
            mean2 /= fluxArr.length;
            for (double v : fluxArr) var2 += (v - mean2) * (v - mean2);
            var2 /= fluxArr.length;
            for (int i = 0; i < nPeriods2; i++) {
                double w = 2 * Math.PI * freq2[i];
                double tau = 0.0;
                double s2wt = 0.0, c2wt = 0.0;
                for (double t : timeArr) {
                    s2wt += Math.sin(2 * w * t);
                    c2wt += Math.cos(2 * w * t);
                }
                tau = Math.atan2(s2wt, c2wt) / (2 * w);
                double cosTerm = 0.0, sinTerm = 0.0, cos2Term = 0.0, sin2Term = 0.0;
                for (int j = 0; j < timeArr.length; j++) {
                    double theta = w * (timeArr[j] - tau);
                    double c = Math.cos(theta);
                    double s = Math.sin(theta);
                    cosTerm += (fluxArr[j] - mean2) * c;
                    sinTerm += (fluxArr[j] - mean2) * s;
                    cos2Term += c * c;
                    sin2Term += s * s;
                }
                power2[i] = (cosTerm * cosTerm / cos2Term + sinTerm * sinTerm / sin2Term) / (2 * var2);
            }
            double[] periods2 = new double[nPeriods2];
            for (int i = 0; i < nPeriods2; i++) periods2[i] = 1.0 / freq2[i];
            Plot plot2 = new Plot("Lomb-Scargle Periodogram", "Period", "Power");
            plot2.add("line", periods2, power2);
            plot2.show();
            return;
        }

        // TLS implementation (similar to BLS, only for measurement tables)
        if (algorithm.equals("TLS")) {
            if (isMeasurementTable) {
                new Thread(() -> {
                    MeasurementTable table = MeasurementTable.getTable(dataSource);
                    if (table == null) {
                        IJ.showMessage("Periodogram", "Could not open table.");
                        return;
                    }
                    String[] columns = table.getColumnHeadings().split("\t");
                    if (columns.length < 2) {
                        IJ.showMessage("Periodogram", "Not enough columns in table.");
                        return;
                    }
                    GenericDialog gdCol = new GenericDialog("TLS Settings");
                    gdCol.addChoice("Time column", columns, TIME_COL.get().isEmpty() ? columns[0] : TIME_COL.get());
                    gdCol.addChoice("Flux column", columns, FLUX_COL.get().isEmpty() ? columns[1] : FLUX_COL.get());
                    gdCol.addNumericField("Min period", MIN_PERIOD.get(), 6, 12, null); // 0 means auto
                    gdCol.addNumericField("Max period", MAX_PERIOD.get(), 6, 12, null); // 0 means auto
                    gdCol.addNumericField("Steps", STEPS.get(), 0, 6, null);
                    gdCol.addNumericField("Min fractional duration (0-1)", MIN_FRACTIONAL_DURATION.get(), 3);
                    gdCol.addNumericField("Max fractional duration (0-1)", MAX_FRACTIONAL_DURATION.get(), 3);
                    gdCol.addNumericField("Duration steps", DURATION_STEPS.get(), 0);
                    gdCol.addNumericField("Peak width cutoff (0-1)", PEAK_WIDTH_CUTOFF.get(), 2, 6, null);
                    gdCol.addNumericField("Number of peaks", PEAK_COUNT.get(), 0, 6, null);
                    gdCol.addNumericField("Number of planets to search", PLANET_COUNT.get(), 0);
                    gdCol.addNumericField("In-transit masking factor", TRANSIT_MASK_FACTOR.get(), 2);
                    gdCol.addChoice("T0 for masking", new String[]{"Model center (sliding fit)", "Min average (phase bin)"}, T0_MASK.get());
                    gdCol.addNumericField("Limb darkening u1", LIMB_U1.get(), 3);
                    gdCol.addNumericField("Limb darkening u2", LIMB_U2.get(), 3);
                    gdCol.showDialog();
                    if (gdCol.wasCanceled()) return;
                    String timeCol = gdCol.getNextChoice();
                    String fluxCol = gdCol.getNextChoice();
                    double minPeriod = gdCol.getNextNumber();
                    double maxPeriod = gdCol.getNextNumber();
                    int nPeriods = (int) gdCol.getNextNumber();
                    double minFracDur = gdCol.getNextNumber();
                    double maxFracDur = gdCol.getNextNumber();
                    int nDurations = (int) gdCol.getNextNumber();
                    double userCutoff = gdCol.getNextNumber();
                    int userNumPeaks = (int) gdCol.getNextNumber();
                    int nPlanets = (int) gdCol.getNextNumber();
                    double maskFactor = gdCol.getNextNumber();
                    String t0MaskingChoice = gdCol.getNextChoice();
                    double u1 = gdCol.getNextNumber();
                    double u2 = gdCol.getNextNumber();
                    TIME_COL.set(timeCol);
                    FLUX_COL.set(fluxCol);
                    MIN_PERIOD.set(minPeriod);
                    MAX_PERIOD.set(maxPeriod);
                    STEPS.set((double) nPeriods);
                    MIN_FRACTIONAL_DURATION.set(minFracDur);
                    MAX_FRACTIONAL_DURATION.set(maxFracDur);
                    DURATION_STEPS.set((double) nDurations);
                    PEAK_WIDTH_CUTOFF.set((double) userNumPeaks);
                    PLANET_COUNT.set((double) nPlanets);
                    TRANSIT_MASK_FACTOR.set(maskFactor);
                    T0_MASK.set(t0MaskingChoice);
                    LIMB_U1.set(u1);
                    LIMB_U2.set(u2);

                    if (minFracDur <= 0 || maxFracDur <= 0 || minFracDur >= maxFracDur || nDurations < 1) {
                        IJ.showMessage("Periodogram", "Invalid duration scan parameters.");
                        return;
                    }
                    double[] time = table.getDoubleColumn(table.getColumnIndex(timeCol));
                    double[] flux = table.getDoubleColumn(table.getColumnIndex(fluxCol));
                    if (time == null || flux == null || time.length != flux.length || time.length < 10) {
                        IJ.showMessage("Periodogram", "Invalid or insufficient data.");
                        return;
                    }
                    // Sort time and flux by time (in case not sorted)
                    int n = time.length;
                    double[] sortedTime = new double[n];
                    double[] sortedFlux = new double[n];
                    Integer[] idx = new Integer[n];
                    for (int i = 0; i < n; i++) idx[i] = i;
                    final double[] timeToSort = time;
                    Arrays.sort(idx, Comparator.comparingDouble(i2 -> timeToSort[i2]));
                    for (int i = 0; i < n; i++) {
                        sortedTime[i] = time[idx[i]];
                        sortedFlux[i] = flux[idx[i]];
                    }
                    // Prepare for iterative search
                    boolean[] masked = new boolean[n]; // true = masked/ignored
                    List<TLSResult> allResults = new ArrayList<>();
                    double bjdOffset = Double.NaN;
                    try {
                        MeasurementTable measTable = MeasurementTable.getTable("Measurements");
                        int bjdCol = measTable.getColumnIndex("BJD_TDB");
                        if (bjdCol != MeasurementTable.COLUMN_NOT_FOUND && measTable.getCounter() > 0) {
                            bjdOffset = measTable.getValueAsDouble(bjdCol, 0);
                        }
                    } catch (Exception e) {
                        // Ignore, leave bjdOffset as NaN
                    }
                    // If minPeriod or maxPeriod is 0.0, use auto logic (same as BLS)
                    double tMin = sortedTime[0], tMax = sortedTime[n-1];
                    if (minPeriod <= 0.0) minPeriod = (tMax - tMin) / 20.0;
                    if (maxPeriod <= 0.0) maxPeriod = (tMax - tMin) * 0.8;

                    // Print TLS parameters to console
                    System.out.println("=== TLS Parameters ===");
                    System.out.printf("Time range: %.6f to %.6f (%.2f days)\n", tMin, tMax, tMax - tMin);
                    System.out.printf("Period range: %.6f to %.6f days\n", minPeriod, maxPeriod);
                    System.out.printf("Period steps: %d\n", nPeriods);
                    System.out.printf("Duration range (fractional, 0-1): %.4f to %.4f (as a fraction of period)\n", minFracDur, maxFracDur);
                    double medianPeriod = (minPeriod + maxPeriod) / 2.0;
                    System.out.printf("Example: for median period %.4f days, duration range = %.4f to %.4f days\n",
                            medianPeriod, minFracDur * medianPeriod, maxFracDur * medianPeriod);
                    System.out.printf("Duration steps: %d\n", nDurations);
                    System.out.printf("Data points: %d\n", time.length);
                    System.out.println("===================");

                    // Create a single progress window for all planet searches
                    TextWindow tlsProgressWin = new TextWindow("Periodogram Progress", "", 600, 600);
                    tlsProgressWin.getTextPanel().append(String.format("TLS Multi-Planet Progress (%d planets)\n", nPlanets));
                    // Add overall progress bar
                    tlsProgressWin.getTextPanel().append(String.format("Overall Progress: [%s] %d%% (0/%d planets finished)\n", "                    ", 0, nPlanets));
                    int tlsOverallProgressLine = tlsProgressWin.getTextPanel().getLineCount() - 1;
                    // Add ability to cancel TLS run
                    final AtomicBoolean tlsCancelled = new AtomicBoolean(false);
                    Button tlsStopButton = new Button("Stop");
                    tlsStopButton.addActionListener(e -> {
                        tlsCancelled.set(true);
                        tlsProgressWin.getTextPanel().append("\nTLS run cancelled by user.\n");
                    });
                    Panel tlsButtonPanel = new Panel();
                    tlsButtonPanel.add(tlsStopButton);
                    tlsProgressWin.add(tlsButtonPanel, BorderLayout.SOUTH);
                    tlsProgressWin.pack();
                    for (int planet = 0; planet < nPlanets; planet++) {
                        if (tlsCancelled.get()) break;
                        // Append a line for this planet
                        tlsProgressWin.getTextPanel().append(String.format("Planet %d: Starting...\n", planet + 1));
                        int progressLine = tlsProgressWin.getTextPanel().getLineCount() - 1;
                        final int planetFinal = planet;
                        // Debug: print number of unmasked points before each TLS run
                        int nUnmasked = 0;
                        for (int i = 0; i < n; i++) if (!masked[i]) nUnmasked++;
                        System.out.println("TLS Iteration " + (planet + 1) + ": unmasked points = " + nUnmasked);
                        if (nUnmasked < 10) break;
                        // Build unmasked arrays for this TLS run
                        List<Integer> unmaskedIdx = new ArrayList<>();
                        for (int i = 0; i < n; i++) if (!masked[i]) unmaskedIdx.add(i);
                        double[] t = new double[unmaskedIdx.size()];
                        double[] f = new double[unmaskedIdx.size()];
                        for (int i = 0; i < t.length; i++) {
                            t[i] = sortedTime[unmaskedIdx.get(i)];
                            f[i] = sortedFlux[unmaskedIdx.get(i)];
                        }
                        // If all points are masked, stop
                        if (t.length < 10) break;

                        // TLS progress bar setup
                        int barLength = 50;
                        long tlsStartTime = System.currentTimeMillis(); // Start time for ETA
                        //TextWindow tlsProgressWin = new TextWindow("Periodogram Progress", "Progress\n", 600, 200); // moved outside loop
                        System.out.print("TLS Progress: [");
                        TLS.Result tlsResult = TLS.search(t, f, minPeriod, maxPeriod, nPeriods, minFracDur, maxFracDur, nDurations, u1, u2, (pi) -> {
                            if (tlsCancelled.get()) return;
                            int nStars = (int) Math.round(barLength * (pi + 1) / (double) nPeriods);
                            StringBuilder bar = new StringBuilder();
                            bar.append("TLS Progress: [");
                            for (int s = 0; s < nStars; s++) bar.append("*");
                            for (int s = nStars; s < barLength; s++) bar.append(" ");
                            int percent = (int) Math.round(100.0 * (pi + 1) / nPeriods);
                            long elapsed = System.currentTimeMillis() - tlsStartTime;
                            double fractionDone = (pi + 1) / (double) nPeriods;
                            long estTotal = (long) (elapsed / (fractionDone > 0 ? fractionDone : 1e-6));
                            long estRemaining = estTotal - elapsed;
                            String timeStr = String.format(" | ETA: %ds", estRemaining / 1000);
                            bar.append("] ").append(percent).append("%").append(timeStr);
                            System.out.print("\r" + bar.toString());
                            // Update ImageJ progress bar
                            IJ.showProgress((pi + 1) / (double) nPeriods);
                            // Update only the last line for this planet
                            int totalLines = tlsProgressWin.getTextPanel().getLineCount();
                            tlsProgressWin.getTextPanel().setLine(progressLine, String.format("Planet %d: %s", planetFinal + 1, bar.toString()));
                        });

                        // Check if cancelled after TLS search
                        if (tlsCancelled.get()) {
                            System.out.println("\nTLS calculation cancelled by user.");
                            break;
                        }

                        IJ.showProgress(1.0); // Clear progress bar at end
                        System.out.println();
                        // Mark this planet as done
                        tlsProgressWin.getTextPanel().setLine(progressLine, String.format("Planet %d: Done!", planetFinal + 1));
                        // Update overall progress bar
                        int tlsPlanetsDone = planetFinal + 1;
                        int tlsPercent = (int) Math.round(100.0 * tlsPlanetsDone / nPlanets);
                        int tlsNStars = (int) Math.round(50.0 * tlsPlanetsDone / nPlanets);
                        StringBuilder tlsBar = new StringBuilder();
                        for (int s = 0; s < tlsNStars; s++) tlsBar.append("*");
                        for (int s = tlsNStars; s < 50; s++) tlsBar.append(" ");
                        tlsProgressWin.getTextPanel().setLine(tlsOverallProgressLine, String.format("Overall Progress: [%s] %d%% (%d/%d planets finished)", tlsBar.toString(), tlsPercent, tlsPlanetsDone, nPlanets));

                        // Find best period and SDE
                        double bestSDE = Double.NEGATIVE_INFINITY;
                        double bestPeriod = 0.0;
                        int bestIdx = 0;
                        for (int i = 0; i < tlsResult.sde.length; i++) {
                            if (tlsResult.sde[i] > bestSDE) {
                                bestSDE = tlsResult.sde[i];
                                bestPeriod = tlsResult.periods[i];
                                bestIdx = i;
                            }
                        }
                        // Use best duration and depth from grid search
                        double bestDuration = tlsResult.bestDurations[bestIdx];
                        double bestDepth = tlsResult.bestDepths[bestIdx];

                        // Find peaks with minimum separation (same logic as BLS)
                        int minSeparation = Math.max(1, (int) (nPeriods * 0.01)); // 1% of period range
                        List<Integer> peakIndices = findPeaks(tlsResult.sde, minSeparation);
                        peakIndices.sort((a, b) -> Double.compare(tlsResult.sde[b], tlsResult.sde[a]));

                        // Apply cutoff and limit number of peaks
                        double cutoff = bestSDE * userCutoff;
                        List<Integer> topPeaks = new ArrayList<>();
                        for (int peakIdx : peakIndices) {
                            if (tlsResult.sde[peakIdx] > cutoff && topPeaks.size() < userNumPeaks) {
                                topPeaks.add(peakIdx);
                            }
                        }

                        // Ensure best peak is included
                        if (!topPeaks.contains(bestIdx)) {
                            topPeaks.add(bestIdx);
                        }
                        topPeaks.sort((a, b) -> Double.compare(tlsResult.sde[b], tlsResult.sde[a]));

                        // Save all peaks for this iteration
                        for (int i = 0; i < topPeaks.size(); i++) {
                            int peakIdx = topPeaks.get(i);
                            allResults.add(new TLSResult(planetFinal + 1, i + 1, tlsResult.periods[peakIdx], tlsResult.sde[peakIdx], bestDuration, bestDepth, tlsResult.t0_sliding, tlsResult.t0_minavg));
                        }

                        // Print results to console for this iteration
                        System.out.println("=== TLS Results (Planet " + (planetFinal + 1) + ") ===");
                        System.out.printf("Best period: %.6f days\n", bestPeriod);
                        System.out.printf("Best SDE: %.4f\n", bestSDE);
                        System.out.printf("Best T0 (sliding fit): %.6f\n", tlsResult.t0_sliding);
                        System.out.printf("Best T0 (min avg phase bin): %.6f\n", tlsResult.t0_minavg);
                        System.out.printf("Best depth: %.6f\n", bestDepth);
                        System.out.printf("Best transit length: %.6f days\n", bestDuration);
                        System.out.printf("Cutoff SDE: %.4f\n", cutoff);
                        System.out.printf("Found %d peaks above cutoff\n", topPeaks.size());
                        for (int i = 0; i < Math.min(topPeaks.size(), userNumPeaks); i++) {
                            int peakIdx = topPeaks.get(i);
                            System.out.printf("Peak %d: P=%.6f, SDE=%.4f\n", i + 1, tlsResult.periods[peakIdx], tlsResult.sde[peakIdx]);
                        }
                        System.out.println("==================");

                        // Mask in-transit points using user-selected T0 for next iteration
                        if (!Double.isNaN(bestPeriod) && !Double.isNaN(tlsResult.t0_sliding) && !Double.isNaN(bestDuration)) {
                            // Use user-selected T0 for masking
                            double t0_ref_masking = tlsResult.t0_sliding;
                            if (t0MaskingChoice.equals("Min average (phase bin)")) {
                                t0_ref_masking = tlsResult.t0_minavg;
                            }
                            double halfMask = 0.5 * maskFactor * bestDuration;
                            for (int i = 0; i < n; i++) {
                                if (masked[i]) continue;
                                double phase = ((sortedTime[i] - t0_ref_masking + 0.5 * bestPeriod) % bestPeriod) - 0.5 * bestPeriod;
                                if (Math.abs(phase) < halfMask) {
                                    masked[i] = true;
                                }
                            }
                            int nMasked = 0;
                            for (int i = 0; i < n; i++) if (masked[i]) nMasked++;
                            System.out.printf("After masking: %d points masked out of %d (%.2f%%)\n", nMasked, n, 100.0 * nMasked / n);
                            System.out.printf("Iteration %d: Detected period = %.6f, T0 (masking, %s) = %.6f, duration = %.6f, depth = %.6f, halfMask = %.6f\n",
                                    planetFinal + 1, bestPeriod, t0MaskingChoice, t0_ref_masking, bestDuration, bestDepth, halfMask);
                        }

                        // Show periodogram plot for this iteration
                        Plot plot = new Plot("TLS Periodogram (Planet " + (planetFinal + 1) + ")", "Period", "SDE");
                        plot.add("line", tlsResult.periods, tlsResult.sde);

                        // Highlight the most dominant peak with vertical shaded band
                        if (bestIdx >= 0 && bestIdx < tlsResult.periods.length) {
                            double dominantPeriod = tlsResult.periods[bestIdx];
                            double dominantSDE = tlsResult.sde[bestIdx];

                            // Calculate peak width for shading
                            double highlightPeakSDE = tlsResult.sde[bestIdx];
                            double highlightCutoff = highlightPeakSDE * userCutoff;
                            int highlightLeft = bestIdx, highlightRight = bestIdx;
                            while (highlightLeft > 0 && tlsResult.sde[highlightLeft] > highlightCutoff) {
                                highlightLeft--;
                            }
                            while (highlightRight < tlsResult.sde.length - 1 && tlsResult.sde[highlightRight] > highlightCutoff) {
                                highlightRight++;
                            }

                            // Create shaded vertical band around dominant peak
                            double bandLeft = tlsResult.periods[highlightLeft];
                            double bandRight = tlsResult.periods[highlightRight];
                            double yMin = 0;
                            double yMax = bestSDE * 1.1;

                            // Draw a single vertical line at the dominant peak center instead of filled rectangle
                            plot.setColor(Color.RED); // Red
                            plot.addPoints(new double[]{dominantPeriod, dominantPeriod}, new double[]{yMin, yMax}, Plot.LINE);

                            // Reset color to black for other elements
                            plot.setColor(Color.BLACK);

                            // Add orange vertical lines for harmonics/aliases of the dominant peak only
                            plot.setColor(new Color(255, 165, 0)); // Orange
                            // First harmonic (2x period)
                            double harmonic1 = dominantPeriod * 2.0;
                            if (harmonic1 >= minPeriod && harmonic1 <= maxPeriod) {
                                plot.addPoints(new double[]{harmonic1, harmonic1}, new double[]{yMin, yMax}, Plot.LINE);
                            }
                            // Second harmonic (3x period)
                            double harmonic2 = dominantPeriod * 3.0;
                            if (harmonic2 >= minPeriod && harmonic2 <= maxPeriod) {
                                plot.addPoints(new double[]{harmonic2, harmonic2}, new double[]{yMin, yMax}, Plot.LINE);
                            }
                            // Sub-harmonic (0.5x period)
                            double subHarmonic = dominantPeriod * 0.5;
                            if (subHarmonic >= minPeriod && subHarmonic <= maxPeriod) {
                                plot.addPoints(new double[]{subHarmonic, subHarmonic}, new double[]{yMin, yMax}, Plot.LINE);
                            }

                            // Reset color to black
                            plot.setColor(Color.BLACK);
                        }

                        plot.addLabel(0.01, 0.95, String.format("Best: P=%.4f, SDE=%.2f", bestPeriod, bestSDE));

                        // Label top peaks on plot with T0 for the dominant peak
                        for (int i = 0; i < Math.min(topPeaks.size(), userNumPeaks); i++) {
                            int peakIdx = topPeaks.get(i);
                            double peakPeriod = tlsResult.periods[peakIdx];
                            if (i == 0 && peakIdx == bestIdx) {
                                // For the dominant peak (Peak 1), add T0 information
                                String t0Value = "";
                                if (t0MaskingChoice.equals("Min average (phase bin)")) {
                                    t0Value = Double.isNaN(tlsResult.t0_minavg) ? "N/A" : String.format("T0=%.3f", tlsResult.t0_minavg);
                                } else {
                                    t0Value = Double.isNaN(tlsResult.t0_sliding) ? "N/A" : String.format("T0=%.3f", tlsResult.t0_sliding);
                                }
                                plot.addLabel(0.01, 0.06 + i * 0.05, String.format("Peak %d: P=%.4f %s", i + 1, peakPeriod, t0Value));
                            } else {
                                plot.addLabel(0.01, 0.06 + i * 0.05, String.format("Peak %d: P=%.4f", i + 1, peakPeriod));
                            }
                        }
                        plot.show();
                        plot.update();
                    }
                    // After all planets, update the progress window to show completion
                    tlsProgressWin.getTextPanel().append("\nAll planet searches complete.\n");
                    // Show results in a table
                    StringBuilder resultMsg = new StringBuilder();
                    String t0ColLabel = t0MaskingChoice.equals("Min average (phase bin)") ? "T0 (masking, Min avg)" : "T0 (masking, Model center)";
                    String header = String.format("%-4s %-4s %-12s %-10s %-14s %-10s %-24s\n",
                            "Iter", "Pk", "Period", "SDE", "Duration (hr)", "Depth", t0ColLabel);
                    String sep = String.format("%-4s %-4s %-12s %-10s %-14s %-10s %-24s\n",
                            "----", "---", "------------", "----------", "--------------", "----------", "------------------------");
                    resultMsg.append(header).append(sep);
                    int lastIter = -1;
                    for (TLSResult tr : allResults) {
                        if (lastIter != -1 && tr.iteration != lastIter) resultMsg.append("\n");
                        String t0Val = t0MaskingChoice.equals("Min average (phase bin)")
                                ? (Double.isNaN(tr.t0_minavg) ? "" : String.format("%.6f", tr.t0_minavg))
                                : (Double.isNaN(tr.t0_sliding) ? "" : String.format("%.6f", tr.t0_sliding));
                        double durationHours = tr.duration * 24.0;
                        resultMsg.append(String.format("%4d %3d %12.6f %10.4f %14.4f %10.4f %24s\n",
                                tr.iteration, tr.peakNumber, tr.period, tr.sde, durationHours, tr.depth, t0Val));
                        lastIter = tr.iteration;
                    }
                    // Show results in a JTable with Save as CSV button
                    SwingUtilities.invokeLater(() -> 
                            createResultsTable(new String[]{"Iter", "Pk", "Period", "SDE", "Duration (hr)", "Depth", 
                                    "T0 (masking, Model center)"
                            }, resultMsg, "TLS Multi-Planet Results"));
                }).start();
            } else {
                IJ.showMessage("Periodogram", "TLS is only implemented for Measurements Table.");
            }
        }
    }

    private static void createResultsTable(String[] columnNames, StringBuilder resultMsg, String title) {
        List<String[]> rowList = new ArrayList<>();
        String[] lines = resultMsg.toString().split("\\n");
        for (String line : lines) {
            if (line.trim().isEmpty() || line.startsWith("Iter") || line.startsWith("----")) continue;
            String[] row = line.trim().split("\\s+");
            if (row.length >= columnNames.length) rowList.add(row);
        }
        String[][] data = rowList.toArray(new String[0][]);
        JTable blsTable = new JTable(data, columnNames);
        blsTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        blsTable.setCellSelectionEnabled(true);
        JScrollPane scrollPane = new JScrollPane(blsTable);
        JButton saveButton = createSaveButton(blsTable, columnNames, data);
        JPanel panel = new JPanel(new BorderLayout());
        panel.add(scrollPane, BorderLayout.CENTER);
        panel.add(saveButton, BorderLayout.SOUTH);
        JFrame frame = new JFrame(title);
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.setContentPane(panel);
        // Auto-resize columns to fit content first
        TableColumnModel blsColumnModel = blsTable.getColumnModel();
        for (int col = 0; col < blsTable.getColumnCount(); col++) {
            int width = 75; // min width
            for (int row = 0; row < blsTable.getRowCount(); row++) {
                Component comp = blsTable.getCellRenderer(row, col).getTableCellRendererComponent(blsTable, blsTable.getValueAt(row, col), false, false, row, col);
                width = Math.max(comp.getPreferredSize().width + 10, width);
            }
            blsColumnModel.getColumn(col).setPreferredWidth(width);
        }
        // Dynamically expand the last column (T0...) so full text is visible
        if (blsTable.getColumnCount() > 0) {
            int lastCol = blsTable.getColumnCount() - 1;
            TableColumn col = blsColumnModel.getColumn(lastCol);
            JTableHeader headerComp = blsTable.getTableHeader();
            Component headerRenderer = col.getHeaderRenderer() != null ?
                    col.getHeaderRenderer().getTableCellRendererComponent(blsTable, col.getHeaderValue(), false, false, 0, lastCol)
                    : headerComp.getDefaultRenderer().getTableCellRendererComponent(blsTable, col.getHeaderValue(), false, false, 0, lastCol);
            int headerWidth = headerRenderer.getPreferredSize().width + 10;
            int maxCellWidth = headerWidth;
            for (int row = 0; row < blsTable.getRowCount(); row++) {
                Component comp = blsTable.getCellRenderer(row, lastCol).getTableCellRendererComponent(blsTable, blsTable.getValueAt(row, lastCol), false, false, row, lastCol);
                maxCellWidth = Math.max(maxCellWidth, comp.getPreferredSize().width + 10);
            }
            col.setPreferredWidth(maxCellWidth);
        }

        // Force the table to recalculate its preferred size with the new column widths
        blsTable.setPreferredScrollableViewportSize(blsTable.getPreferredSize());

        // Use pack() to size the frame based on the updated preferred sizes
        frame.pack();

        // Ensure the frame is wide enough for all columns
        Dimension tablePref = blsTable.getPreferredSize();
        Dimension scrollPanePref = scrollPane.getPreferredSize();
        int requiredWidth = Math.max(tablePref.width, scrollPanePref.width) + 20; // Add small padding
        int requiredHeight = scrollPanePref.height + 80; // Add space for save button

        frame.setSize(requiredWidth, requiredHeight);
        // Set minimum size to ensure usability
        frame.setMinimumSize(new Dimension(600, 300));
        try {
            frame.setIconImage(IJ.getInstance().getIconImage());
        } catch (Exception ex) {
            // Ignore if not running in ImageJ
        }
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    private static JButton createSaveButton(JTable blsTable, String[] columnNames, String[][] data) {
        JButton saveButton = new JButton("Save as CSV");
        saveButton.addActionListener(e -> {
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setFileFilter(new FileNameExtensionFilter("CSV Files", "csv"));
            if (fileChooser.showSaveDialog(blsTable) == JFileChooser.APPROVE_OPTION) {
                File file = fileChooser.getSelectedFile();
                String fname = file.getName().toLowerCase();
                if (!fname.endsWith(".csv")) {
                    file = new File(file.getParentFile(), file.getName() + ".csv");
                }
                try (PrintWriter pw = new PrintWriter(new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8))) {
                    for (int i = 0; i < columnNames.length; i++) {
                        pw.print(columnNames[i]);
                        if (i < columnNames.length - 1) pw.print(",");
                    }
                    pw.println();
                    for (String[] row : data) {
                        for (int i = 0; i < columnNames.length; i++) {
                            pw.print(row.length > i ? row[i] : "");
                            if (i < columnNames.length - 1) pw.print(",");
                        }
                        pw.println();
                    }
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(blsTable, "Error saving file: " + ex.getMessage());
                }
            }
        });
        return saveButton;
    }

    private static double getMinPhase(int nBins, List<List<Double>> binFluxes, int minPointsPerBin) {
        double minAvg = Double.POSITIVE_INFINITY;
        int minBin = -1;
        for (int i = 0; i < nBins; i++) {
            List<Double> fluxes = binFluxes.get(i);
            if (fluxes.size() < minPointsPerBin) continue;
            double sum = 0.0;
            for (double v : fluxes) sum += v;
            double avg = sum / fluxes.size();
            if (avg < minAvg) {
                minAvg = avg;
                minBin = i;
            }
        }

        return (minBin + 0.5) / nBins;
    }

    // Helper method to find peaks with minimum separation
    private List<Integer> findPeaks(double[] data, int minSeparation) {
        List<Integer> peaks = new ArrayList<>();
        for (int i = 1; i < data.length - 1; i++) {
            if (data[i] > data[i-1] && data[i] > data[i+1]) {
                // Check if this peak is far enough from existing peaks
                boolean tooClose = false;
                for (int existingPeak : peaks) {
                    if (Math.abs(i - existingPeak) < minSeparation) {
                        tooClose = true;
                        break;
                    }
                }
                if (!tooClose) {
                    peaks.add(i);
                }
            }
        }
        return peaks;
    }

    // Huber loss helper
    public static double huberLoss(double[] residuals, double delta) {
        double sum = 0.0;
        for (int i = 0; i < residuals.length; i++) {
            double r = residuals[i];
            if (Math.abs(r) <= delta) {
                sum += 0.5 * r * r;
            } else {
                sum += delta * (Math.abs(r) - 0.5 * delta);
            }
        }
        return sum / residuals.length;
    }

    private record PeakResult(int iteration, int peakNumber, double period, double power, double duration,
                      double depth, double phase, double t0_masking_model_center,
                      double t0_masking_min_avg) {
    }

    private record LSResult(int iteration, int peakNumber, double period, double power, double maskCenter) {
    }

    private record TLSResult(int iteration, int peakNumber, double period, double sde, double duration,
                     double depth, double t0_sliding, double t0_minavg) {
    }
}