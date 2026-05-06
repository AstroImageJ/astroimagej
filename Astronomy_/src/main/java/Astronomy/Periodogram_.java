package Astronomy;

import java.awt.BorderLayout;
import java.awt.Button;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Panel;
import java.awt.Point;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.IntConsumer;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.Popup;
import javax.swing.PopupFactory;
import javax.swing.SwingUtilities;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;

import astroj.MeasurementTable;
import ij.IJ;
import ij.ImagePlus;
import ij.WindowManager;
import ij.astro.io.prefs.Property;
import ij.gui.GenericDialog;
import ij.gui.Plot;
import ij.plugin.PlugIn;
import ij.text.TextWindow;

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
    private static final Property<Double> PEAK_WIDTH_CUTOFF = new Property<>(0.25, Periodogram_.class);
    private static final Property<Double> PEAK_COUNT = new Property<>(1.0, Periodogram_.class);
    private static final Property<Double> PLANET_COUNT = new Property<>(1.0, Periodogram_.class);
    private static final Property<Double> TRANSIT_MASK_FACTOR = new Property<>(3.0, Periodogram_.class);
    // TLS blind-search grid — the periodogram search sweeps every combination
    // of (u1, u2, ingressFrac) and keeps the running-best SNR per period.
    // Defaults cover most FGK host stars (u1,u2 ≈ 0.2–0.6 / 0.0–0.4) and
    // transit geometries from central flat-bottomed (ingressFrac ≈ 0.05)
    // through grazing V-shaped (ingressFrac ≈ 0.5).  A 3×3×5 = 45-cell grid
    // is cheap on GPU and tolerable on CPU.
    private static final Property<Double>  LIMB_U1_MIN    = new Property<>(0.20, Periodogram_.class);
    private static final Property<Double>  LIMB_U1_MAX    = new Property<>(0.60, Periodogram_.class);
    private static final Property<Double>  LIMB_U1_STEPS  = new Property<>(3.0,  Periodogram_.class);
    private static final Property<Double>  LIMB_U2_MIN    = new Property<>(0.00, Periodogram_.class);
    private static final Property<Double>  LIMB_U2_MAX    = new Property<>(0.40, Periodogram_.class);
    private static final Property<Double>  LIMB_U2_STEPS  = new Property<>(3.0,  Periodogram_.class);
    private static final Property<Double>  INGRESS_MIN    = new Property<>(0.05, Periodogram_.class);
    private static final Property<Double>  INGRESS_MAX    = new Property<>(0.45, Periodogram_.class);
    private static final Property<Double>  INGRESS_STEPS  = new Property<>(5.0,  Periodogram_.class);
    private static final Property<Boolean> LOCK_T0         = new Property<>(false, Periodogram_.class);
    private static final Property<Double>  LOCKED_T0_VALUE = new Property<>(0.0,   Periodogram_.class);
    private static final Property<String> PLOT_X_SCALE = new Property<>("Linear", Periodogram_.class);
    private static final Property<String> COMPUTE_BACKEND = new Property<>("CPU", Periodogram_.class);
    // Shared between BLS and TLS — both use this for phase-grid resolution during search.
    private static final Property<Double> PHASE_BINS = new Property<>(1000.0, Periodogram_.class);

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

        if ("Synthetic".equals(DATA_SOURCE.get()) && tableNames != null && tableNames.length > 0) {
            defaultDataSource = tableNames[0];
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
                    // Top-level safety net: anything uncaught in this background thread
                    // would otherwise kill the thread silently — leaving the user with a
                    // "Planet N: Done!" message and no plots/results.  Catch Throwable so
                    // both RuntimeExceptions and (unlikely) Errors are surfaced.
                    try {
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
                    gdCol.addNumericField("Phase bins (resolution = P / bins)", PHASE_BINS.get(), 0);
                    gdCol.addNumericField("Peak width cutoff (0-1)", PEAK_WIDTH_CUTOFF.get(), 2, 6, null);
                    gdCol.addNumericField("Number of peaks", PEAK_COUNT.get(), 0, 6, null);
                    gdCol.addNumericField("Number of planets to search", PLANET_COUNT.get(), 0);
                    gdCol.addNumericField("In-transit masking factor", TRANSIT_MASK_FACTOR.get(), 2);
                    gdCol.addChoice("T0 for masking", new String[]{"Model center (sliding fit)", "Min average (phase bin)"}, T0_MASK.get());
                    gdCol.addCheckbox("Lock T0 (skip auto-fit, use value below)", LOCK_T0.get());
                    gdCol.addNumericField("Locked T0 value (days)", LOCKED_T0_VALUE.get(), 6, 14, "days");
                    gdCol.addRadioButtonGroup("X Axis Scale:", new String[]{"Linear", "Log"}, 1, 2, PLOT_X_SCALE.get());
                    { Panel sp = new Panel(); sp.setPreferredSize(new Dimension(1, 7)); gdCol.addPanel(sp); }
                    String[] blsBackends = PeriodogramGpuContext.getAvailableBackends();
                    String blsDefaultBackend = PeriodogramGpuContext.isGpu(COMPUTE_BACKEND.get())
                            && Arrays.asList(blsBackends).contains(COMPUTE_BACKEND.get())
                            ? COMPUTE_BACKEND.get() : PeriodogramGpuContext.CPU_BACKEND;
                    gdCol.addChoice("Compute backend", blsBackends, blsDefaultBackend);

                    // --- Per-field hover tooltips (yellow bubble help) ---
                    applyTooltips(gdCol.getChoices(), new String[] {
                            "Data-table column holding observation times (usually BJD_TDB).",
                            "Data-table column holding the relative/normalised flux (typically ~1.0 out of transit).",
                            "How to determine transit centre T0 when masking a detected planet:" +
                                    "  \u2022 \"Model center\" runs a sliding-trapezoid Huber fit (more accurate for noisy data)." +
                                    "  \u2022 \"Min average\" picks the phase bin with the lowest mean flux (faster).",
                            "Where to run the periodogram computation. " +
                                    "GPU entries require an OpenCL device with double-precision (cl_khr_fp64) support; " +
                                    "devices without FP64 are filtered out at startup."
                    });
                    applyTooltips(gdCol.getNumericFields(), new String[] {
                            "Shortest orbital period to search, in days. " +
                                    "Enter 0 for automatic (data_span / 20).",
                            "Longest orbital period to search, in days. " +
                                    "Enter 0 for automatic (0.8 \u00d7 data_span).",
                            "Number of trial periods in the frequency grid. " +
                                    "Higher = finer period resolution but slower (cost is O(steps)).",
                            "Minimum transit duration expressed as a fraction of the orbital period " +
                                    "(e.g. 0.005 = 0.5% of period).",
                            "Maximum transit duration expressed as a fraction of the orbital period " +
                                    "(e.g. 0.1 = 10% of period).",
                            "Number of trial durations sampled between min and max. " +
                                    "More = finer transit-shape scan, linear cost.",
                            "Phase-fold resolution. Time resolution per cell = period / bins. " +
                                    "Example: 1000 bins at P=1 day \u2248 1.44 min per cell; at P=100 days \u2248 2.4 hr per cell. " +
                                    "Valid range 50\u20132000.",
                            "Minimum fractional drop from the main peak (0\u20131) required to report a secondary peak. " +
                                    "0.25 = secondary peaks must be within 25% of the main peak height.",
                            "Number of secondary peaks to annotate on the periodogram plot.",
                            "How many planet candidates to detect iteratively. " +
                                    "After each detection the found transit is masked and BLS re-runs on the residual.",
                            "When masking a detected planet, multiply the fitted transit duration by this factor " +
                                    "to widen the mask window (e.g. 3.0 masks \u00b11.5 durations around T0).",
                            "User-supplied T0 (BJD days) to use when the \"Lock T0\" checkbox above is ticked."
                    });
                    applyTooltips(gdCol.getCheckboxes(), new String[] {
                            "Skip the automatic sliding-trapezoid T0 fit and use the value entered below."
                    });

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
                    int nBinsCfg = (int) gdCol.getNextNumber();
                    double userCutoff = gdCol.getNextNumber();
                    int userNumPeaks = (int) gdCol.getNextNumber();
                    int nPlanets = (int) gdCol.getNextNumber();
                    double maskFactor = gdCol.getNextNumber();
                    String t0MaskingChoice = gdCol.getNextChoice();
                    boolean lockT0 = gdCol.getNextBoolean();
                    double lockedT0Value = gdCol.getNextNumber();
                    String plotXScale = gdCol.getNextRadioButton();
                    String blsComputeBackend = gdCol.getNextChoice();
                    // Validate bin count: 50 minimum (meaningful resolution), 2000 maximum
                    // (cap on GPU private memory per thread: 2000 * 8 bytes = 16 KB).
                    if (nBinsCfg < 50)   nBinsCfg = 50;
                    if (nBinsCfg > 2000) nBinsCfg = 2000;
                    final int nBinsUser = nBinsCfg;
                    TIME_COL.set(timeCol);
                    FLUX_COL.set(fluxCol);
                    MIN_PERIOD.set(minPeriodUser);
                    MAX_PERIOD.set(maxPeriodUser);
                    STEPS.set((double) nPeriods);
                    MIN_FRACTIONAL_DURATION.set(minFracDur);
                    MAX_FRACTIONAL_DURATION.set(maxFracDur);
                    DURATION_STEPS.set((double) nDurations);
                    PHASE_BINS.set((double) nBinsUser);
                    PEAK_WIDTH_CUTOFF.set(userCutoff);
                    PEAK_COUNT.set((double) userNumPeaks);
                    PLANET_COUNT.set((double) nPlanets);
                    TRANSIT_MASK_FACTOR.set(maskFactor);
                    T0_MASK.set(t0MaskingChoice);
                    LOCK_T0.set(lockT0);
                    LOCKED_T0_VALUE.set(lockedT0Value);
                    PLOT_X_SCALE.set(plotXScale);
                    COMPUTE_BACKEND.set(blsComputeBackend);
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
                    // BJD reference for converting (phase, duration) into an absolute BJD.
                    //
                    // Historically this was looked up from a hardcoded "Measurements" table,
                    // which silently failed when the user loaded a saved table via
                    // Read_MeasurementTable (the loaded table is named after the file, not
                    // "Measurements").  That left bjdOffset = NaN, which propagated into
                    // minAverageT0 and later tripped a `minIdx = -1` / `t[minIdx]` path
                    // (ArrayIndexOutOfBoundsException) that killed the background thread
                    // silently — "Planet N: Done!" appeared but no plots ever materialised.
                    //
                    // Fallback chain:
                    //   1) the user's selected table (always present here)
                    //   2) a table literally named "Measurements" (legacy behaviour)
                    //   3) sortedTime[0] as a last resort so bjdOffset is never NaN
                    double bjdOffset = Double.NaN;
                    try {
                        int bjdCol = table.getColumnIndex("BJD_TDB");
                        if (bjdCol != MeasurementTable.COLUMN_NOT_FOUND && table.getCounter() > 0) {
                            bjdOffset = table.getValueAsDouble(bjdCol, 0);
                        }
                    } catch (Exception ignored) { /* fall through */ }
                    if (Double.isNaN(bjdOffset)) {
                        try {
                            MeasurementTable measTable = MeasurementTable.getTable("Measurements");
                            if (measTable != null) {
                                int bjdCol = measTable.getColumnIndex("BJD_TDB");
                                if (bjdCol != MeasurementTable.COLUMN_NOT_FOUND && measTable.getCounter() > 0) {
                                    bjdOffset = measTable.getValueAsDouble(bjdCol, 0);
                                }
                            }
                        } catch (Exception ignored) { /* fall through */ }
                    }
                    if (Double.isNaN(bjdOffset) && sortedTime.length > 0) {
                        bjdOffset = sortedTime[0];
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
                    // Pre-compile GPU kernel before the planet loop.
                    // NVIDIA's OpenCL JIT can take 30–90 s on the first run; doing it here
                    // lets us show a clear message instead of silently stalling at "Running on GPU…".
                    if (PeriodogramGpuContext.isGpu(blsComputeBackend)) {
                        progressWin.getTextPanel().append(
                                "Compiling GPU kernel (first run may take 30–90 s — watch the Log window)...\n");
                        try {
                            BLSGpu.warmUp(blsComputeBackend, nBinsUser);
                            progressWin.getTextPanel().append("GPU kernel ready.\n");
                        } catch (Exception warmEx) {
                            IJ.log("[GPU BLS] Kernel compile failed: " + warmEx.getMessage()
                                    + " — falling back to CPU.");
                            blsComputeBackend = PeriodogramGpuContext.CPU_BACKEND;
                            progressWin.getTextPanel().append(
                                    "GPU kernel compile failed — falling back to CPU (see Log).\n");
                        }
                    }

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
                        // --- BLS Grid Search: GPU or CPU ---
                        double bestPower = 0;
                        double bestPeriod = 0;
                        double bestDuration = 0;
                        double bestDepth = 0;
                        double bestPhase = 0;

                        boolean blsUseGpu = PeriodogramGpuContext.isGpu(blsComputeBackend);
                        BLSGpu.BLSResult blsGpuResult = null;
                        if (blsUseGpu) {
                            progressWin.getTextPanel().setLine(progressLine,
                                    String.format("Planet %d: Running on GPU (%s)...", planet + 1, blsComputeBackend));
                            try {
                                // Pass the same AtomicBoolean the Stop button flips so the
                                // GPU kernel can be aborted between dispatch slices instead
                                // of running to completion (CPU mode already honours it).
                                blsGpuResult = BLSGpu.search(t, f, periods, nDurations,
                                        minFracDur, maxFracDur, nBinsUser, blsComputeBackend,
                                        blsCancelled::get);
                            } catch (Exception gpuEx) {
                                IJ.log("[GPU BLS] Error: " + gpuEx.getMessage() + " \u2014 falling back to CPU.");
                                blsUseGpu = false;
                            }
                        }
                        if (blsUseGpu && blsGpuResult != null) {
                            System.arraycopy(blsGpuResult.power,         0, power,         0, nPeriods);
                            System.arraycopy(blsGpuResult.bestDurations, 0, bestDurations, 0, nPeriods);
                            System.arraycopy(blsGpuResult.bestDepths,    0, bestDepths,    0, nPeriods);
                            System.arraycopy(blsGpuResult.bestPhases,    0, bestPhases,    0, nPeriods);
                            for (int i = 0; i < nPeriods; i++) {
                                if (power[i] > bestPower) {
                                    bestPower    = power[i];
                                    bestPeriod   = periods[i];
                                    bestDuration = bestDurations[i];
                                    bestDepth    = bestDepths[i];
                                    bestPhase    = bestPhases[i];
                                }
                            }
                            IJ.showProgress(1.0);
                        } else {
                            // CPU path
                            int nThreads = Math.max(1, Runtime.getRuntime().availableProcessors() - 2);
                            ExecutorService executor = Executors.newFixedThreadPool(nThreads);
                            List<Future<double[]>> futures = new ArrayList<>();
                            for (int i = 0; i < nPeriods; i++) {
                                final int periodIdx = i;
                                futures.add(executor.submit(() -> {
                                    double period = periods[periodIdx];
                                    final int nBins = nBinsUser;
                                    final double invPeriod = 1.0 / period;

                                    // Stage 1: phase-fold all points into nBins uniform bins.
                                    // Each point is visited exactly once — O(nPoints).
                                    double[] binFluxSum = new double[nBins];
                                    int[]    binN       = new int[nBins];
                                    for (int j = 0; j < t.length; j++) {
                                        double ph = t[j] * invPeriod;
                                        ph -= Math.floor(ph);          // fractional phase in [0, 1)
                                        int bin = (int)(ph * nBins);
                                        if (bin >= nBins) bin = nBins - 1;
                                        binFluxSum[bin] += f[j];
                                        binN[bin]++;
                                    }

                                    // Stage 2: sliding-window BLS on the binned data — O(nDurations × nBins).
                                    // The data-point loop is gone; in/out sums update with 4 array ops per step.
                                    double maxThisPower = 0, bestThisDuration = 0, bestThisDepth = 0, bestThisPhase = 0;
                                    int nDurM1 = Math.max(1, nDurations - 1);
                                    double fracRange = maxFracDur - minFracDur;

                                    for (int d = 0; d < nDurations; d++) {
                                        double fracDur  = minFracDur + (double) d * fracRange / nDurM1;
                                        int    durBins  = Math.max(1, (int)(fracDur * nBins + 0.5));
                                        if (durBins >= nBins) durBins = nBins - 1;
                                        double duration = fracDur * period;

                                        // Build initial window: bins [0, durBins) are "in-transit"
                                        double sumIn  = 0, sumOut = 0;
                                        double nIn    = 0, nOut  = 0;
                                        for (int k = 0; k < durBins; k++) {
                                            sumIn += binFluxSum[k];
                                            nIn   += binN[k];
                                        }
                                        for (int k = durBins; k < nBins; k++) {
                                            sumOut += binFluxSum[k];
                                            nOut   += binN[k];
                                        }

                                        // Slide the window one bin at a time around the full circle
                                        for (int p = 0; p < nBins; p++) {
                                            if (nIn >= 2 && nOut >= 2) {
                                                double meanIn  = sumIn  / nIn;
                                                double meanOut = sumOut / nOut;
                                                double depth   = meanOut - meanIn;
                                                double thisPower = depth * depth * nIn * nOut / (nIn + nOut);
                                                if (thisPower > maxThisPower) {
                                                    maxThisPower     = thisPower;
                                                    bestThisDuration = duration;
                                                    bestThisDepth    = depth;
                                                    // Phase = centre of the transit box
                                                    bestThisPhase = ((double) p + durBins * 0.5) / nBins * period;
                                                }
                                            }
                                            // Slide: bin p leaves, bin (p + durBins) % nBins enters
                                            int addBin = p + durBins;
                                            if (addBin >= nBins) addBin -= nBins;
                                            sumIn  -= binFluxSum[p];       nIn  -= binN[p];
                                            sumIn  += binFluxSum[addBin];  nIn  += binN[addBin];
                                            sumOut += binFluxSum[p];       nOut += binN[p];
                                            sumOut -= binFluxSum[addBin];  nOut -= binN[addBin];
                                        }
                                    }
                                    return new double[]{maxThisPower, bestThisDuration, bestThisDepth, bestThisPhase};
                                }));
                            }
                            int progressStep = Math.max(1, nPeriods / 100);
                            int barLength = 50;
                            long startTime = System.currentTimeMillis();
                            System.out.print("BLS Progress: [");
                            for (int i = 0; i < nPeriods; i++) {
                                if (blsCancelled.get()) {
                                    executor.shutdownNow();
                                    try {
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
                                        bestPower    = result[0];
                                        bestPeriod   = periods[i];
                                        bestDuration = result[1];
                                        bestDepth    = result[2];
                                        bestPhase    = result[3];
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
                                        IJ.showProgress((i + 1) / (double) nPeriods);
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
                            if (!executor.isShutdown()) {
                                executor.shutdown();
                                try {
                                    executor.awaitTermination(5, TimeUnit.SECONDS);
                                } catch (InterruptedException e) {
                                    executor.shutdownNow();
                                }
                            }
                            IJ.showProgress(1.0);
                            System.out.println();
                        }
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
                        if (lockT0 && bestPeriod > 0) {
                            // User-locked T0: use the exact value entered; masking uses modular arithmetic so no propagation needed
                            modelCenterTimeForMasking = lockedT0Value;
                            minAverageT0 = lockedT0Value;
                            System.out.printf("[DIAG] T0 locked: using exact user value=%.6f\n", lockedT0Value);
                        } else if (bestPeriod > 0 && bestDuration > 0) {
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
                            // Print the phase for the time closest to minAverageT0 (using robust formula).
                            // Guard against minAverageT0 being NaN (or any future case where every
                            // dt comparison is false) — without this guard `t[minIdx]` below would
                            // hit `t[-1]` and throw ArrayIndexOutOfBoundsException, silently killing
                            // the BLS background thread and suppressing all plots/results.
                            double minDt = Double.POSITIVE_INFINITY;
                            int minIdx = -1;
                            if (!Double.isNaN(minAverageT0)) {
                                for (int i = 0; i < t.length; i++) {
                                    double dt = Math.abs(t[i] - minAverageT0);
                                    if (dt < minDt) {
                                        minDt = dt;
                                        minIdx = i;
                                    }
                                }
                            }
                            if (minIdx < 0) {
                                System.out.printf("[DIAG] Closest time to minAverageT0: skipped (minAverageT0 = %.6f — no finite distances)%n", minAverageT0);
                            } else {
                                double phaseAtMinAverageT0 = (t[minIdx] - minAverageT0) / bestPeriod;
                                phaseAtMinAverageT0 = phaseAtMinAverageT0 - Math.floor(phaseAtMinAverageT0);
                                System.out.printf("[DIAG] Closest time to minAverageT0: t = %.6f, phase = %.6f\n", t[minIdx], phaseAtMinAverageT0);
                            }
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
                            final int nTau = (int) Math.round((maxTauFrac - minTauFrac) / tauFracStep) + 1;
                            // Phase step size: (duration/200) in phase units
                            final double phaseStep = (bestDuration / bestPeriod) / 200.0;
                            final int nPhaseSteps = (int) Math.ceil(1.0 / phaseStep);
                            double[] bestModel = new double[t.length];
                            final double[] lossGrid        = new double[nTau * nPhaseSteps];
                            final double[] tauFracGrid     = new double[nTau * nPhaseSteps];
                            final double[] phaseOffsetGrid = new double[nTau * nPhaseSteps];
                            double delta = 0.002; // Huber loss parameter (slightly less robust)

                            // --- Sort phases once so each grid cell can binary-search the in-window points ---
                            // Before: O(nPoints) per cell (scan all, most rejected).
                            // After:  O(log n + window_points) per cell (only the relevant arc).
                            final int nData = t.length;
                            Integer[] sortIdxT0 = new Integer[nData];
                            for (int i = 0; i < nData; i++) sortIdxT0[i] = i;
                            final double[] phasesCapture = phases;
                            Arrays.sort(sortIdxT0, Comparator.comparingDouble(i -> phasesCapture[i]));
                            final double[] sortedPhasesT0 = new double[nData];
                            final double[] sortedFluxT0   = new double[nData];
                            for (int i = 0; i < nData; i++) {
                                sortedPhasesT0[i] = phases[sortIdxT0[i]];
                                sortedFluxT0[i]   = f[sortIdxT0[i]];
                            }

                            final double durOverPeriodT0 = bestDuration / bestPeriod;
                            final double windowT0        = 1.5 * durOverPeriodT0;
                            final double deltaT0         = delta;
                            final double phaseStepT0     = phaseStep;
                            final int    nPhaseStepsT0   = nPhaseSteps;
                            final double bestDepthT0     = bestDepth;
                            final double bestPeriodT0    = bestPeriod;

                            // --- Parallelise the outer tauFrac loop across CPU cores ---
                            int nT0Threads = Math.max(1, Runtime.getRuntime().availableProcessors() - 2);
                            ExecutorService t0Exec = Executors.newFixedThreadPool(nT0Threads);
                            List<Future<double[]>> t0Futures = new ArrayList<>();
                            long t0Start = System.currentTimeMillis();

                            for (int tauIdx = 0; tauIdx < nTau; tauIdx++) {
                                final int tauIdxF = tauIdx;
                                final double tauFrac = minTauFrac + tauIdxF * tauFracStep;
                                final double tauPhase = tauFrac * durOverPeriodT0;
                                final double flatPhase = durOverPeriodT0 - 2 * tauPhase;
                                if (flatPhase < 0) continue; // skip unphysical (same as original)
                                final double tauFracF = tauFrac;
                                final double tauPhaseF = tauPhase;
                                final double flatPhaseF = flatPhase;

                                t0Futures.add(t0Exec.submit(() -> {
                                    double localBestLoss        = Double.POSITIVE_INFINITY;
                                    double localBestPhaseOffset = 0;

                                    for (int s = 0; s < nPhaseStepsT0; s++) {
                                        if (blsCancelled.get()) break;
                                        double phaseOffset = s * phaseStepT0 * bestPeriodT0;
                                        double C = phaseOffset / bestPeriodT0;
                                        C -= Math.floor(C); // wrap to [0, 1)

                                        double sumLoss = 0.0;
                                        int    nUsed   = 0;

                                        if (windowT0 >= 0.5) {
                                            // Window spans the full circle — iterate every point.
                                            for (int k = 0; k < nData; k++) {
                                                double ps = sortedPhasesT0[k] - C;
                                                ps -= Math.floor(ps);
                                                double mdl;
                                                if      (ps < tauPhaseF)                  mdl = -bestDepthT0 * (ps / tauPhaseF);
                                                else if (ps < tauPhaseF + flatPhaseF)     mdl = -bestDepthT0;
                                                else if (ps < 2*tauPhaseF + flatPhaseF)   mdl = -bestDepthT0 * (1 - (ps - tauPhaseF - flatPhaseF) / tauPhaseF);
                                                else                                       mdl = 0.0;
                                                double rr = sortedFluxT0[k] - mdl;
                                                double absR = Math.abs(rr);
                                                sumLoss += (absR <= deltaT0) ? 0.5*rr*rr : deltaT0*(absR - 0.5*deltaT0);
                                                nUsed++;
                                            }
                                        } else {
                                            // Binary-search the in-window arc [C-window, C+window] (circular)
                                            double lo = C - windowT0;
                                            double hi = C + windowT0;
                                            int r0a, r0b, r1a, r1b;
                                            if (lo < 0) {
                                                // Two arcs: [lo+1, 1) and [0, hi]
                                                r0a = lowerBoundD(sortedPhasesT0, lo + 1);  r0b = nData;
                                                r1a = 0;                                    r1b = upperBoundD(sortedPhasesT0, hi);
                                            } else if (hi >= 1.0) {
                                                // Two arcs: [lo, 1) and [0, hi-1]
                                                r0a = lowerBoundD(sortedPhasesT0, lo);      r0b = nData;
                                                r1a = 0;                                    r1b = upperBoundD(sortedPhasesT0, hi - 1);
                                            } else {
                                                // Single arc [lo, hi]
                                                r0a = lowerBoundD(sortedPhasesT0, lo);      r0b = upperBoundD(sortedPhasesT0, hi);
                                                r1a = 0;                                    r1b = 0;
                                            }
                                            // Arc 1
                                            for (int k = r0a; k < r0b; k++) {
                                                double ps = sortedPhasesT0[k] - C;
                                                ps -= Math.floor(ps);
                                                double mdl;
                                                if      (ps < tauPhaseF)                  mdl = -bestDepthT0 * (ps / tauPhaseF);
                                                else if (ps < tauPhaseF + flatPhaseF)     mdl = -bestDepthT0;
                                                else if (ps < 2*tauPhaseF + flatPhaseF)   mdl = -bestDepthT0 * (1 - (ps - tauPhaseF - flatPhaseF) / tauPhaseF);
                                                else                                       mdl = 0.0;
                                                double rr = sortedFluxT0[k] - mdl;
                                                double absR = Math.abs(rr);
                                                sumLoss += (absR <= deltaT0) ? 0.5*rr*rr : deltaT0*(absR - 0.5*deltaT0);
                                                nUsed++;
                                            }
                                            // Arc 2 (empty when not wrapping)
                                            for (int k = r1a; k < r1b; k++) {
                                                double ps = sortedPhasesT0[k] - C;
                                                ps -= Math.floor(ps);
                                                double mdl;
                                                if      (ps < tauPhaseF)                  mdl = -bestDepthT0 * (ps / tauPhaseF);
                                                else if (ps < tauPhaseF + flatPhaseF)     mdl = -bestDepthT0;
                                                else if (ps < 2*tauPhaseF + flatPhaseF)   mdl = -bestDepthT0 * (1 - (ps - tauPhaseF - flatPhaseF) / tauPhaseF);
                                                else                                       mdl = 0.0;
                                                double rr = sortedFluxT0[k] - mdl;
                                                double absR = Math.abs(rr);
                                                sumLoss += (absR <= deltaT0) ? 0.5*rr*rr : deltaT0*(absR - 0.5*deltaT0);
                                                nUsed++;
                                            }
                                        }

                                        double cellLoss = (nUsed > 0) ? sumLoss / nUsed : Double.POSITIVE_INFINITY;
                                        int gi = tauIdxF * nPhaseStepsT0 + s;
                                        lossGrid[gi]        = cellLoss;
                                        tauFracGrid[gi]     = tauFracF;
                                        phaseOffsetGrid[gi] = phaseOffset;

                                        if (cellLoss < localBestLoss) {
                                            localBestLoss        = cellLoss;
                                            localBestPhaseOffset = phaseOffset;
                                        }
                                    }
                                    return new double[] { localBestLoss, localBestPhaseOffset, tauFracF };
                                }));
                            }

                            double bestLoss = Double.POSITIVE_INFINITY;
                            double bestPhaseOffset = 0;
                            double bestTauFrac = 0;
                            try {
                                for (Future<double[]> fut : t0Futures) {
                                    double[] r = fut.get();
                                    if (r[0] < bestLoss) {
                                        bestLoss        = r[0];
                                        bestPhaseOffset = r[1];
                                        bestTauFrac     = r[2];
                                    }
                                }
                            } catch (InterruptedException ie) {
                                Thread.currentThread().interrupt();
                                IJ.log("[T0] Parallel T0 search interrupted.");
                            } catch (java.util.concurrent.ExecutionException ee) {
                                IJ.log("[T0] Parallel T0 search error: " + ee.getMessage());
                            } finally {
                                t0Exec.shutdown();
                            }
                            System.out.printf("[T0] Parallel T0 refinement: %d threads, %d ms%n",
                                    nT0Threads, System.currentTimeMillis() - t0Start);

                            // Fill bestModel once using the final winning parameters.
                            if (bestLoss < Double.POSITIVE_INFINITY) {
                                double durOP = bestDuration / bestPeriod;
                                double tauPh = bestTauFrac * durOP;
                                double flatPh = durOP - 2 * tauPh;
                                for (int j = 0; j < t.length; j++) {
                                    double phaseForPlot = phases[j] - (bestPhaseOffset / bestPeriod);
                                    phaseForPlot -= Math.floor(phaseForPlot);
                                    if      (phaseForPlot < tauPh)                bestModel[j] = -bestDepth * (phaseForPlot / tauPh);
                                    else if (phaseForPlot < tauPh + flatPh)       bestModel[j] = -bestDepth;
                                    else if (phaseForPlot < 2 * tauPh + flatPh)   bestModel[j] = -bestDepth * (1 - (phaseForPlot - tauPh - flatPh) / tauPh);
                                    else                                          bestModel[j] = 0.0;
                                }
                            }

                            // Plot code below checks `gridIndex < gridIdx`; we filled the full array
                            // using direct (tauIdx * nPhaseSteps + s) indexing, so set gridIdx to the
                            // full size.  Slots for skipped tauIdxs (flatPhase < 0) keep default 0
                            // and are filtered out by the `tauFracGrid[gi] == bestTauFrac` check.
                            int gridIdx = nTau * nPhaseSteps;
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
                        } // end else if (bestPeriod > 0 && bestDuration > 0)
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
                        // When T0 is locked, use the exact user value with no phase refinement
                        double t0_ref_masking_model_center = Double.NaN;
                        if (lockT0 && !Double.isNaN(modelCenterTimeForMasking)) {
                            t0_ref_masking_model_center = lockedT0Value;
                        } else if (!Double.isNaN(modelCenterTimeForMasking) && !Double.isNaN(bestPeriod)) {
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
                        double t0_ref_masking_min_avg = Double.NaN;
                        if (lockT0 && !Double.isNaN(minAverageT0)) {
                            t0_ref_masking_min_avg = lockedT0Value;
                        } else if (!Double.isNaN(minAverageT0) && !Double.isNaN(bestPeriod)) {
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
                        // Compute odd/even transit depths for this iteration
                        double blsT0ForOE = t0MaskingChoice.equals("Min average (phase bin)")
                                ? t0_ref_masking_min_avg : t0_ref_masking_model_center;
                        double[] blsOE = (!Double.isNaN(blsT0ForOE) && bestPeriod > 0 && bestDuration > 0)
                                ? computeOddEvenDepths(t, f, bestPeriod, blsT0ForOE, bestDuration)
                                : new double[]{Double.NaN, Double.NaN, Double.NaN, 0, 0};
                        double blsOddDepth = blsOE[0], blsEvenDepth = blsOE[1], blsOddEvenDiffPct = blsOE[2];
                        int blsNOddIn = (int) blsOE[3], blsNEvenIn = (int) blsOE[4];
                        // Save all peaks for this iteration
                        for (int i = 0; i < nLabelPeaks; i++) {
                            int peakIdx = sortedTopPeaks.get(i);
                            allPeaks.add(new PeakResult(planet + 1, i + 1, periods[peakIdx], power[peakIdx], bestDurations[peakIdx], bestDepths[peakIdx], bestPhases[peakIdx], t0_ref_masking_model_center, t0_ref_masking_min_avg, blsOddDepth, blsEvenDepth, blsOddEvenDiffPct, blsNOddIn, blsNEvenIn));
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
                            if (!lockT0) {
                                double minPhase = getMinPhase(nBins, binFluxes, minPointsPerBin);
                                t0_ref_masking = t0_ref_masking + minPhase * bestPeriod;
                            }
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
                        Plot plot = new Plot("BLS Periodogram (Planet " + (planet + 1) + ")", "Period (days)", "Power");
                        plot.setLimits(minPeriod, maxPeriod, 0, bestPower * 1.1);
                        if (plotXScale.equals("Log")) plot.setLogScaleX();
                        plot.addPoints(periods, power, Plot.DOT);
                        plot.addPoints(periods, power, Plot.LINE);

                        // Highlight the most dominant peak with vertical shaded band
                        if (bestIdx >= 0 && bestIdx < periods.length) {
                            double dominantPeriod = periods[bestIdx];

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

                            double yMin = 0;
                            double yMax = bestPower * 1.1;

                            // Draw a single vertical line at the dominant peak center
                            plot.setColor(Color.RED);
                            plot.addPoints(new double[]{dominantPeriod, dominantPeriod}, new double[]{yMin, yMax}, Plot.LINE);

                            // Reset color to black for other elements
                            plot.setColor(Color.BLACK);

                            // Orange vertical lines for harmonics/aliases — capped at
                            // 5 multiples of the dominant period.  Only those that fall
                            // inside [minPeriod, maxPeriod] are actually drawn, and the
                            // legend below lists exactly the ones that became visible.
                            double[] harmonicFactors = {0.5, 2.0, 3.0, 4.0, 5.0};
                            List<String> visibleHarmonicLabels = new ArrayList<>();
                            plot.setColor(new Color(255, 165, 0));
                            for (double factor : harmonicFactors) {
                                double p = dominantPeriod * factor;
                                if (p >= minPeriod && p <= maxPeriod) {
                                    plot.addPoints(new double[]{p, p}, new double[]{yMin, yMax}, Plot.LINE);
                                    visibleHarmonicLabels.add(factor == (int) factor
                                            ? "\u00D7" + (int) factor
                                            : "\u00D7" + factor);
                                }
                            }

                            // Gray vertical lines for the secondary peaks (Peak 2..N)
                            // so the user can see where on the periodogram they sit.
                            if (nLabelPeaks > 1) {
                                plot.setColor(Color.GRAY);
                                for (int si = 1; si < nLabelPeaks; si++) {
                                    double sp = periods[sortedTopPeaks.get(si)];
                                    plot.addPoints(new double[]{sp, sp}, new double[]{yMin, yMax}, Plot.LINE);
                                }
                            }

                            // Legend — right-anchored (Plot.RIGHT) so the text is
                            // guaranteed to stay inside the plot frame regardless of
                            // how wide the user resizes the window.  Label colour is
                            // set per line so it matches the line it describes.
                            plot.setJustification(Plot.RIGHT);
                            plot.setColor(Color.RED);
                            plot.addLabel(0.99, 0.06, "\u2014 dominant period");
                            if (!visibleHarmonicLabels.isEmpty()) {
                                plot.setColor(new Color(255, 165, 0));
                                plot.addLabel(0.99, 0.11,
                                        "\u2014 harmonics/aliases (" + String.join(", ", visibleHarmonicLabels) + ")");
                            }
                            if (nLabelPeaks > 1) {
                                plot.setColor(Color.GRAY);
                                plot.addLabel(0.99, 0.16, "\u2014 secondary peaks");
                            }
                            plot.setJustification(Plot.LEFT);
                            plot.setColor(Color.BLACK);
                        }

                        // Label top peaks — Peak 1 (dominant) in red, Peak 2..N in gray,
                        // matching the vertical line colours above.
                        for (int i = 0; i < nLabelPeaks; i++) {
                            int peakIdx = sortedTopPeaks.get(i);
                            double peakPeriod = periods[peakIdx];
                            plot.setColor(i == 0 ? Color.RED : Color.GRAY);
                            if (i == 0 && peakIdx == bestIdx) {
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
                        plot.setColor(Color.BLACK);
                        plot.show();
                        plot.update();
                    }
                    // After all planets, update the progress window to show completion
                    progressWin.getTextPanel().append("\nAll planet searches complete.\n");
                    // Show results in a table
                    StringBuilder resultMsg = new StringBuilder();
                    String t0ColLabel = t0MaskingChoice.equals("Min average (phase bin)") ? "T0 (masking, Min avg)" : "T0 (masking, Model center)";
                    String header = String.format("%-4s %-4s %-12s %-10s %-14s %-10s %-12s %-24s %-10s %-10s %-10s %-8s %-8s\n",
                            "Iter", "Pk", "Period", "Power", "Duration (hr)", "Depth", "Phase", t0ColLabel,
                            "Odd Depth", "Even Depth", "O-E Diff%", "#Odd pts", "#Even pts");
                    String sep = String.format("%-4s %-4s %-12s %-10s %-14s %-10s %-12s %-24s %-10s %-10s %-10s %-8s %-8s\n",
                            "----", "---", "------------", "----------", "--------------", "----------", "------------", "------------------------",
                            "----------", "----------", "----------", "--------", "--------");
                    resultMsg.append(header).append(sep);
                    int lastIter = -1;
                    for (PeakResult pr : allPeaks) {
                        if (lastIter != -1 && pr.iteration != lastIter) resultMsg.append("\n");
                        String t0Val = t0MaskingChoice.equals("Min average (phase bin)")
                                ? (Double.isNaN(pr.t0_masking_min_avg) ? "---" : String.format("%.6f", pr.t0_masking_min_avg))
                                : (Double.isNaN(pr.t0_masking_model_center) ? "---" : String.format("%.6f", pr.t0_masking_model_center));
                        double durationHours = pr.duration * 24.0;
                        String oddDepthStr  = Double.isNaN(pr.oddDepth)       ? "---" : String.format("%.4f", pr.oddDepth);
                        String evenDepthStr = Double.isNaN(pr.evenDepth)      ? "---" : String.format("%.4f", pr.evenDepth);
                        String diffPctStr   = Double.isNaN(pr.oddEvenDiffPct) ? "---" : String.format("%.1f", pr.oddEvenDiffPct);
                        resultMsg.append(String.format("%4d %3d %12.6f %10.4f %14.4f %10.4f %12.6f %24s %10s %10s %10s %8d %8d\n",
                                pr.iteration, pr.peakNumber, pr.period, pr.power, durationHours, pr.depth, pr.phase, t0Val,
                                oddDepthStr, evenDepthStr, diffPctStr, pr.nOddIn, pr.nEvenIn));
                        lastIter = pr.iteration;
                    }
                    // Show results in a JTable with Save as CSV button
                    SwingUtilities.invokeLater(() ->
                            createResultsTable(new String[]{"Iter", "Pk", "Period", "Power", "Duration (hr)",
                                            "Depth", "Phase", "T0 (masking, Model center)",
                                            "Odd Depth", "Even Depth", "O-E Diff%", "#Odd pts", "#Even pts"
                                    },
                                    resultMsg, "BLS Multi-Planet Results"));
                    } catch (Throwable th) {
                        IJ.log("[BLS] Unhandled error in background thread: " + th);
                        java.io.StringWriter sw = new java.io.StringWriter();
                        th.printStackTrace(new PrintWriter(sw));
                        IJ.log(sw.toString());
                        final String msg = th.getClass().getSimpleName()
                                + (th.getMessage() != null ? (": " + th.getMessage()) : "");
                        SwingUtilities.invokeLater(() -> IJ.showMessage(
                                "Periodogram",
                                "BLS failed with an unexpected error:\n" + msg
                                        + "\n\nSee the Log window for the full stack trace."));
                    }
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
                gdCol.addNumericField("Min period", MIN_PERIOD.get(), 6, 12, null);
                gdCol.addNumericField("Max period", MAX_PERIOD.get(), 6, 12, null);
                gdCol.addNumericField("Steps", STEPS.get(), 0, 6, null);
                gdCol.addNumericField("Peaks to label", PEAK_COUNT.get(), 0, 6, null);
                gdCol.showDialog();
                if (gdCol.wasCanceled()) return;
                String timeCol = gdCol.getNextChoice();
                String fluxCol = gdCol.getNextChoice();
                double minPeriodUser = gdCol.getNextNumber();
                double maxPeriodUser = gdCol.getNextNumber();
                int nPeriods = (int) gdCol.getNextNumber();
                int userNumPeaks = (int) gdCol.getNextNumber();
                TIME_COL.set(timeCol);
                FLUX_COL.set(fluxCol);
                MIN_PERIOD.set(minPeriodUser);
                MAX_PERIOD.set(maxPeriodUser);
                STEPS.set((double) nPeriods);
                PEAK_COUNT.set((double) userNumPeaks);

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
                Integer[] sortIdx = new Integer[n];
                for (int i = 0; i < n; i++) sortIdx[i] = i;
                final double[] timeToSort = time;
                Arrays.sort(sortIdx, Comparator.comparingDouble(i2 -> timeToSort[i2]));
                for (int i = 0; i < n; i++) {
                    sortedTime[i] = time[sortIdx[i]];
                    sortedFlux[i] = flux[sortIdx[i]];
                }
                double[] t = sortedTime;
                double[] f = sortedFlux;

                // Period / frequency grid
                double tMin = t[0], tMax = t[t.length - 1];
                double minPeriod = minPeriodUser > 0 ? minPeriodUser : (tMax - tMin) / 20.0;
                double maxPeriod = maxPeriodUser > 0 ? maxPeriodUser : (tMax - tMin) * 0.8;
                if (minPeriod <= 0 || maxPeriod <= 0 || minPeriod >= maxPeriod || nPeriods < 2) {
                    IJ.showMessage("Periodogram", "Invalid period range or step count.");
                    return;
                }
                double minFreq = 1.0 / maxPeriod;
                double maxFreq = 1.0 / minPeriod;
                double[] freq = new double[nPeriods];
                double[] power = new double[nPeriods];
                for (int i = 0; i < nPeriods; i++) {
                    freq[i] = minFreq + i * (maxFreq - minFreq) / (nPeriods - 1);
                }

                // Mean, variance, and mean-subtracted flux (pre-computed once, outside parallel tasks)
                double mean = 0.0, var = 0.0;
                for (double v : f) mean += v;
                mean /= f.length;
                for (double v : f) var += (v - mean) * (v - mean);
                var /= f.length;
                final double[] fCentered = new double[f.length];
                for (int j = 0; j < f.length; j++) fCentered[j] = f[j] - mean;

                // Tier-1: parallelize over frequencies using all available CPU cores.
                // Tier-2: for each frequency, precompute cos(w·t[j]) and sin(w·t[j]) once,
                //         then derive the double-angle tau sums and the shifted-basis power
                //         terms via multiplications only — halving trig-call count vs the
                //         naive four-call-per-point formulation.
                final double[] tFinal   = t;
                final double   varFinal = var;
                final AtomicBoolean lsCancelled = new AtomicBoolean(false);
                final AtomicInteger lsDone      = new AtomicInteger(0);
                int nThreads = Runtime.getRuntime().availableProcessors();
                ExecutorService lsPool = Executors.newFixedThreadPool(nThreads);
                List<Future<?>> lsFutures = new ArrayList<>(nPeriods);
                IJ.showStatus("Lomb-Scargle: " + nPeriods + " frequencies on " + nThreads + " threads…");

                for (int i = 0; i < nPeriods; i++) {
                    final int fi = i;
                    lsFutures.add(lsPool.submit(() -> {
                        if (lsCancelled.get() || IJ.escapePressed()) {
                            lsCancelled.set(true);
                            return;
                        }
                        final double w  = 2.0 * Math.PI * freq[fi];
                        final int    np = tFinal.length;

                        // One cos+sin pair per time point covers both tau and power.
                        double[] C = new double[np];
                        double[] S = new double[np];
                        for (int j = 0; j < np; j++) {
                            double wt = w * tFinal[j];
                            C[j] = Math.cos(wt);
                            S[j] = Math.sin(wt);
                        }

                        // tau via double-angle identities — no additional trig calls
                        double s2wt = 0.0, c2wt = 0.0;
                        for (int j = 0; j < np; j++) {
                            s2wt += 2.0 * S[j] * C[j];          // sin(2wt) = 2 sin(wt)cos(wt)
                            c2wt += C[j] * C[j] - S[j] * S[j];  // cos(2wt) = cos²(wt) − sin²(wt)
                        }
                        double tau  = Math.atan2(s2wt, c2wt) / (2.0 * w);
                        double cTau = Math.cos(w * tau);
                        double sTau = Math.sin(w * tau);

                        // Power in the tau-shifted orthogonal basis
                        double cosTerm = 0.0, sinTerm = 0.0, cos2Term = 0.0, sin2Term = 0.0;
                        for (int j = 0; j < np; j++) {
                            double cj = C[j] * cTau + S[j] * sTau;  // cos(w(t−tau))
                            double sj = S[j] * cTau - C[j] * sTau;  // sin(w(t−tau))
                            cosTerm  += fCentered[j] * cj;
                            sinTerm  += fCentered[j] * sj;
                            cos2Term += cj * cj;
                            sin2Term += sj * sj;
                        }
                        power[fi] = (cosTerm * cosTerm / cos2Term
                                   + sinTerm * sinTerm / sin2Term) / (2.0 * varFinal);

                        int d = lsDone.incrementAndGet();
                        if (d % 500 == 0) IJ.showProgress(d, nPeriods);
                    }));
                }
                lsPool.shutdown();
                try {
                    lsPool.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                IJ.showProgress(1.0);
                IJ.showStatus("");
                if (lsCancelled.get()) {
                    IJ.showMessage("Lomb-Scargle", "Computation cancelled.");
                    return;
                }

                double[] periods = new double[nPeriods];
                for (int i = 0; i < nPeriods; i++) periods[i] = 1.0 / freq[i];

                // Find peaks (fixed alias-separation threshold)
                int bestIdx = 0;
                for (int i = 1; i < power.length; i++) {
                    if (power[i] > power[bestIdx]) bestIdx = i;
                }
                final double peakCutoff = 0.25;
                double peakPower = power[bestIdx];
                double cutoff = peakPower * peakCutoff;
                int left = bestIdx, right = bestIdx;
                while (left > 0 && power[left] > cutoff) left--;
                while (right < power.length - 1 && power[right] > cutoff) right++;
                int peakWidth = right - left;
                int minSeparation = Math.max(1, peakWidth);
                List<Integer> peakIndices = findPeaks(power, minSeparation);
                peakIndices.sort((a, b) -> Double.compare(power[b], power[a]));
                int nTopPeaks = Math.min(userNumPeaks, peakIndices.size());
                List<Integer> topPeaks = new ArrayList<>(peakIndices.subList(0, nTopPeaks));
                if (bestIdx != -1 && (topPeaks.isEmpty() || topPeaks.get(topPeaks.size() - 1) != bestIdx)) {
                    topPeaks.add(bestIdx);
                }
                List<Integer> uniqueTopPeaks = new ArrayList<>();
                for (Integer idx2 : topPeaks) {
                    if (!uniqueTopPeaks.contains(idx2)) uniqueTopPeaks.add(idx2);
                }
                int nLabelPeaks = Math.min(userNumPeaks, uniqueTopPeaks.size());
                List<Integer> sortedTopPeaks = new ArrayList<>(
                        uniqueTopPeaks.subList(uniqueTopPeaks.size() - nLabelPeaks, uniqueTopPeaks.size()));
                sortedTopPeaks.sort((a, b) -> Double.compare(power[b], power[a]));

                // Collect results
                List<LSResult> allResults = new ArrayList<>();
                for (int i = 0; i < nLabelPeaks; i++) {
                    int peakIdx = sortedTopPeaks.get(i);
                    allResults.add(new LSResult(i + 1, periods[peakIdx], power[peakIdx]));
                }

                // Plot
                Plot plot = new Plot("Lomb-Scargle Periodogram", "Period", "Power");
                plot.add("line", periods, power);

                if (bestIdx >= 0 && bestIdx < periods.length) {
                    double dominantPeriod = periods[bestIdx];
                    double dominantPower  = power[bestIdx];
                    double yMin = 0;
                    double yMax = dominantPower * 1.1;

                    plot.setColor(Color.RED);
                    plot.addPoints(new double[]{dominantPeriod, dominantPeriod}, new double[]{yMin, yMax}, Plot.LINE);
                    plot.setColor(Color.BLACK);

                    double[] harmonicFactors = {0.5, 2.0, 3.0, 4.0, 5.0};
                    List<String> visibleHarmonicLabels = new ArrayList<>();
                    plot.setColor(new Color(255, 165, 0));
                    for (double factor : harmonicFactors) {
                        double p = dominantPeriod * factor;
                        if (p >= minPeriod && p <= maxPeriod) {
                            plot.addPoints(new double[]{p, p}, new double[]{yMin, yMax}, Plot.LINE);
                            visibleHarmonicLabels.add(factor == (int) factor
                                    ? "\u00D7" + (int) factor : "\u00D7" + factor);
                        }
                    }
                    if (nLabelPeaks > 1) {
                        plot.setColor(Color.GRAY);
                        for (int si = 1; si < nLabelPeaks; si++) {
                            double sp = periods[sortedTopPeaks.get(si)];
                            plot.addPoints(new double[]{sp, sp}, new double[]{yMin, yMax}, Plot.LINE);
                        }
                    }

                    plot.setJustification(Plot.RIGHT);
                    plot.setColor(Color.RED);
                    plot.addLabel(0.99, 0.06, "\u2014 dominant period");
                    if (!visibleHarmonicLabels.isEmpty()) {
                        plot.setColor(new Color(255, 165, 0));
                        plot.addLabel(0.99, 0.11,
                                "\u2014 harmonics/aliases (" + String.join(", ", visibleHarmonicLabels) + ")");
                    }
                    if (nLabelPeaks > 1) {
                        plot.setColor(Color.GRAY);
                        plot.addLabel(0.99, 0.16, "\u2014 secondary peaks");
                    }
                    plot.setJustification(Plot.LEFT);
                    plot.setColor(Color.BLACK);
                }

                plot.addPoints(periods, power, Plot.LINE);
                for (int i = 0; i < nLabelPeaks; i++) {
                    int peakIdx = sortedTopPeaks.get(i);
                    double peakPeriod = periods[peakIdx];
                    plot.setColor(i == 0 ? Color.RED : Color.GRAY);
                    if (i == 0 && peakIdx == bestIdx) {
                        plot.addLabel(0.01, 0.06 + i * 0.05,
                                String.format("Peak %d: P=%.4f Power=%.4f", i + 1, peakPeriod, power[peakIdx]));
                    } else {
                        plot.addLabel(0.01, 0.06 + i * 0.05,
                                String.format("Peak %d: P=%.4f", i + 1, peakPeriod));
                    }
                }
                plot.setColor(Color.BLACK);
                plot.show();
                plot.update();

                // Results table
                StringBuilder resultMsg = new StringBuilder();
                String header = String.format("%-4s %-12s %-10s\n", "Pk", "Period", "Power");
                String sep    = String.format("%-4s %-12s %-10s\n", "---", "------------", "----------");
                resultMsg.append(header).append(sep);
                for (LSResult res : allResults) {
                    resultMsg.append(String.format("%3d %12.6f %10.4f\n",
                            res.peakNumber(), res.period(), res.power()));
                }
                SwingUtilities.invokeLater(() -> {
                    createResultsTable(new String[]{"Pk", "Period", "Power"},
                            resultMsg, "Lomb-Scargle Results");
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
                    // Top-level safety net — see the matching comment in the BLS block.
                    try {
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
                    gdCol.addNumericField("Phase bins (resolution = P / bins)", PHASE_BINS.get(), 0);
                    gdCol.addNumericField("Peak width cutoff (0-1)", PEAK_WIDTH_CUTOFF.get(), 2, 6, null);
                    gdCol.addNumericField("Number of peaks", PEAK_COUNT.get(), 0, 6, null);
                    gdCol.addNumericField("Number of planets to search", PLANET_COUNT.get(), 0);
                    gdCol.addNumericField("In-transit masking factor", TRANSIT_MASK_FACTOR.get(), 2);
                    gdCol.addChoice("T0 for masking", new String[]{"Model center (sliding fit)", "Min average (phase bin)"}, T0_MASK.get());
                    // --- Blind-search grid: u1 × u2 × ingressFrac ---
                    // The periodogram search tries every combination in this
                    // 3-D grid and keeps the running-best SNR per period, so
                    // users doing a TESS-style blind search don't have to
                    // guess stellar limb darkening or transit geometry.
                    // a/Rs and inclination were removed: they only seeded
                    // the post-search depth guess and had zero effect on
                    // the periodogram search itself.
                    gdCol.addMessage("Blind-search grid (u1 \u00d7 u2 \u00d7 ingress fraction):");
                    gdCol.addNumericField("u1 min", LIMB_U1_MIN.get(), 3);
                    gdCol.addNumericField("u1 max", LIMB_U1_MAX.get(), 3);
                    gdCol.addNumericField("u1 steps", LIMB_U1_STEPS.get(), 0);
                    gdCol.addNumericField("u2 min", LIMB_U2_MIN.get(), 3);
                    gdCol.addNumericField("u2 max", LIMB_U2_MAX.get(), 3);
                    gdCol.addNumericField("u2 steps", LIMB_U2_STEPS.get(), 0);
                    gdCol.addNumericField("Ingress frac min (t12/t14)", INGRESS_MIN.get(), 3);
                    gdCol.addNumericField("Ingress frac max (t12/t14)", INGRESS_MAX.get(), 3);
                    gdCol.addNumericField("Ingress frac steps", INGRESS_STEPS.get(), 0);
                    gdCol.addCheckbox("Lock T0 (skip auto-fit, use value below)", LOCK_T0.get());
                    gdCol.addNumericField("Locked T0 value (days)", LOCKED_T0_VALUE.get(), 6, 14, "days");
                    gdCol.addRadioButtonGroup("X Axis Scale:", new String[]{"Linear", "Log"}, 1, 2, PLOT_X_SCALE.get());
                    { Panel sp = new Panel(); sp.setPreferredSize(new Dimension(1, 7)); gdCol.addPanel(sp); }
                    String[] tlsBackends = PeriodogramGpuContext.getAvailableBackends();
                    String tlsDefaultBackend = PeriodogramGpuContext.isGpu(COMPUTE_BACKEND.get())
                            && Arrays.asList(tlsBackends).contains(COMPUTE_BACKEND.get())
                            ? COMPUTE_BACKEND.get() : PeriodogramGpuContext.CPU_BACKEND;
                    gdCol.addChoice("Compute backend", tlsBackends, tlsDefaultBackend);

                    // --- Per-field hover tooltips (yellow bubble help) ---
                    applyTooltips(gdCol.getChoices(), new String[] {
                            "Data-table column holding observation times (usually BJD_TDB).",
                            "Data-table column holding the relative/normalised flux (typically ~1.0 out of transit).",
                            "How to determine transit centre T0 when masking a detected planet:" +
                                    "  \u2022 \"Model center\" runs a sliding-trapezoid Huber fit (more accurate for noisy data)." +
                                    "  \u2022 \"Min average\" picks the phase bin with the lowest mean flux (faster).",
                            "Where to run the periodogram computation. " +
                                    "GPU entries require an OpenCL device with double-precision (cl_khr_fp64) support; " +
                                    "devices without FP64 are filtered out at startup."
                    });
                    applyTooltips(gdCol.getNumericFields(), new String[] {
                            "Shortest orbital period to search, in days. " +
                                    "Enter 0 for automatic (data_span / 20).",
                            "Longest orbital period to search, in days. " +
                                    "Enter 0 for automatic (0.8 \u00d7 data_span).",
                            "Number of trial periods in the frequency grid. " +
                                    "Higher = finer period resolution but slower (cost is O(steps)).",
                            "Minimum transit duration expressed as a fraction of the orbital period " +
                                    "(e.g. 0.005 = 0.5% of period). " +
                                    "Setting this very small lets TLS latch onto narrow noise features \u2014 raise it " +
                                    "if spurious short-period peaks appear.",
                            "Maximum transit duration expressed as a fraction of the orbital period " +
                                    "(e.g. 0.1 = 10% of period). Also sizes the GPU kernel's model-profile array.",
                            "Number of trial durations sampled between min and max. " +
                                    "More = finer transit-shape scan, linear cost.",
                            "Phase-fold resolution. Time resolution per cell = period / bins. " +
                                    "Example: 1000 bins at P=1 day \u2248 1.44 min per cell; at P=100 days \u2248 2.4 hr per cell. " +
                                    "Valid range 50\u20132000.",
                            "Minimum fractional drop from the main peak (0\u20131) required to report a secondary peak. " +
                                    "0.25 = secondary peaks must be within 25% of the main peak height.",
                            "Number of secondary peaks to annotate on the periodogram plot.",
                            "How many planet candidates to detect iteratively. " +
                                    "After each detection the found transit is masked and TLS re-runs on the residual.",
                            "When masking a detected planet, multiply the fitted transit duration by this factor " +
                                    "to widen the mask window (e.g. 3.0 masks \u00b11.5 durations around T0).",
                            "Lower bound of the linear limb-darkening coefficient u1 (Mandel \u0026 Agol). " +
                                    "Typical range 0.2\u20130.6 for FGK hosts.",
                            "Upper bound of u1.  Set u1 min = u1 max and steps = 1 if you want to pin a known value.",
                            "Number of u1 values tried (\u22651).  Total grid cost = u1 steps \u00d7 u2 steps \u00d7 ingress steps.",
                            "Lower bound of the quadratic limb-darkening coefficient u2. " +
                                    "Typical range 0.0\u20130.4.",
                            "Upper bound of u2.",
                            "Number of u2 values tried (\u22651).",
                            "Lower bound of ingress fraction t12/t14  (ingress duration \u00f7 total transit duration). " +
                                    "Values near 0 give flat-bottomed central transits; values near 0.5 give V-shaped " +
                                    "grazing transits.  0.05\u20130.45 covers the full realistic range.",
                            "Upper bound of ingress fraction.",
                            "Number of ingress-fraction values tried (\u22651).  Default 5 covers central\u2192grazing well.",
                            "User-supplied T0 (BJD days) to use when the \"Lock T0\" checkbox above is ticked."
                    });
                    applyTooltips(gdCol.getCheckboxes(), new String[] {
                            "Skip the automatic sliding-trapezoid T0 fit and use the value entered below."
                    });

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
                    int nBinsCfg = (int) gdCol.getNextNumber();
                    double userCutoff = gdCol.getNextNumber();
                    int userNumPeaks = (int) gdCol.getNextNumber();
                    int nPlanets = (int) gdCol.getNextNumber();
                    double maskFactor = gdCol.getNextNumber();
                    String t0MaskingChoice = gdCol.getNextChoice();
                    double u1Min    = gdCol.getNextNumber();
                    double u1Max    = gdCol.getNextNumber();
                    int    u1Steps  = (int) gdCol.getNextNumber();
                    double u2Min    = gdCol.getNextNumber();
                    double u2Max    = gdCol.getNextNumber();
                    int    u2Steps  = (int) gdCol.getNextNumber();
                    double ingMin   = gdCol.getNextNumber();
                    double ingMax   = gdCol.getNextNumber();
                    int    ingSteps = (int) gdCol.getNextNumber();
                    boolean lockT0 = gdCol.getNextBoolean();
                    double lockedT0Value = gdCol.getNextNumber();
                    String plotXScale = gdCol.getNextRadioButton();
                    String tlsComputeBackend = gdCol.getNextChoice();
                    // Validate bin count: 50 minimum, 2000 maximum (same as BLS).
                    if (nBinsCfg < 50)   nBinsCfg = 50;
                    if (nBinsCfg > 2000) nBinsCfg = 2000;
                    final int nBinsUser = nBinsCfg;
                    TIME_COL.set(timeCol);
                    FLUX_COL.set(fluxCol);
                    MIN_PERIOD.set(minPeriod);
                    MAX_PERIOD.set(maxPeriod);
                    STEPS.set((double) nPeriods);
                    MIN_FRACTIONAL_DURATION.set(minFracDur);
                    MAX_FRACTIONAL_DURATION.set(maxFracDur);
                    DURATION_STEPS.set((double) nDurations);
                    PHASE_BINS.set((double) nBinsUser);
                    PEAK_WIDTH_CUTOFF.set(userCutoff);
                    PEAK_COUNT.set((double) userNumPeaks);
                    PLANET_COUNT.set((double) nPlanets);
                    TRANSIT_MASK_FACTOR.set(maskFactor);
                    T0_MASK.set(t0MaskingChoice);
                    LOCK_T0.set(lockT0);
                    LOCKED_T0_VALUE.set(lockedT0Value);
                    // Clamp grid steps \u2265 1 BEFORE persisting so a stray 0/negative
                    // entry doesn't stick around between runs.
                    if (u1Steps  < 1) u1Steps  = 1;
                    if (u2Steps  < 1) u2Steps  = 1;
                    if (ingSteps < 1) ingSteps = 1;
                    // Clamp ingress fraction to a sane physical range (0, 0.5].
                    if (ingMin <= 0.0) ingMin = 0.01;
                    if (ingMax >  0.5) ingMax = 0.5;
                    if (ingMax < ingMin) ingMax = ingMin;
                    LIMB_U1_MIN.set(u1Min);
                    LIMB_U1_MAX.set(u1Max);
                    LIMB_U1_STEPS.set((double) u1Steps);
                    LIMB_U2_MIN.set(u2Min);
                    LIMB_U2_MAX.set(u2Max);
                    LIMB_U2_STEPS.set((double) u2Steps);
                    INGRESS_MIN.set(ingMin);
                    INGRESS_MAX.set(ingMax);
                    INGRESS_STEPS.set((double) ingSteps);
                    PLOT_X_SCALE.set(plotXScale);
                    COMPUTE_BACKEND.set(tlsComputeBackend);

                    // Build the actual grid arrays the search methods accept.
                    // linspace() below returns a length-`steps` array inclusive
                    // of min and max; with steps == 1 it returns [min] so users
                    // can "pin" an axis to a single value.
                    final double[] u1Grid      = buildLinearGrid(u1Min, u1Max, u1Steps);
                    final double[] u2Grid      = buildLinearGrid(u2Min, u2Max, u2Steps);
                    final double[] ingressGrid = buildLinearGrid(ingMin, ingMax, ingSteps);

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
                    // BJD reference — same fallback chain as the BLS block above.  Currently
                    // unused in the TLS code path, but kept populated and non-NaN so that any
                    // future TLS code that reuses it can't resurface the BLS-era silent-crash
                    // bug caused by the hardcoded "Measurements" table lookup.
                    double bjdOffset = Double.NaN;
                    try {
                        int bjdCol = table.getColumnIndex("BJD_TDB");
                        if (bjdCol != MeasurementTable.COLUMN_NOT_FOUND && table.getCounter() > 0) {
                            bjdOffset = table.getValueAsDouble(bjdCol, 0);
                        }
                    } catch (Exception ignored) { /* fall through */ }
                    if (Double.isNaN(bjdOffset)) {
                        try {
                            MeasurementTable measTable = MeasurementTable.getTable("Measurements");
                            if (measTable != null) {
                                int bjdCol = measTable.getColumnIndex("BJD_TDB");
                                if (bjdCol != MeasurementTable.COLUMN_NOT_FOUND && measTable.getCounter() > 0) {
                                    bjdOffset = measTable.getValueAsDouble(bjdCol, 0);
                                }
                            }
                        } catch (Exception ignored) { /* fall through */ }
                    }
                    if (Double.isNaN(bjdOffset) && sortedTime.length > 0) {
                        bjdOffset = sortedTime[0];
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
                    // Pre-compile GPU kernel before the planet loop (same rationale as BLS).
                    if (PeriodogramGpuContext.isGpu(tlsComputeBackend)) {
                        tlsProgressWin.getTextPanel().append(
                                "Compiling GPU kernel (first run may take 30–90 s — watch the Log window)...\n");
                        try {
                            TLSGpu.warmUp(tlsComputeBackend, nBinsUser, maxFracDur);
                            tlsProgressWin.getTextPanel().append("GPU kernel ready.\n");
                        } catch (Exception warmEx) {
                            IJ.log("[GPU TLS] Kernel compile failed: " + warmEx.getMessage()
                                    + " — falling back to CPU.");
                            tlsComputeBackend = PeriodogramGpuContext.CPU_BACKEND;
                            tlsProgressWin.getTextPanel().append(
                                    "GPU kernel compile failed — falling back to CPU (see Log).\n");
                        }
                    }

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
                        System.out.print("TLS Progress: [");

                        // --- TLS Grid Search: GPU or CPU ---
                        IntConsumer tlsProgressCallback = (pi) -> {
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
                            IJ.showProgress((pi + 1) / (double) nPeriods);
                            int totalLines = tlsProgressWin.getTextPanel().getLineCount();
                            tlsProgressWin.getTextPanel().setLine(progressLine, String.format("Planet %d: %s", planetFinal + 1, bar.toString()));
                        };

                        TLS.Result tlsResult;
                        if (PeriodogramGpuContext.isGpu(tlsComputeBackend)) {
                            tlsProgressWin.getTextPanel().setLine(progressLine,
                                    String.format("Planet %d: Running on GPU (%s)...", planetFinal + 1, tlsComputeBackend));
                            TLS.Result gpuTlsResult = null;
                            try {
                                // Pass the same AtomicBoolean the Stop button flips so the
                                // GPU kernel can be aborted between dispatch slices instead
                                // of running to completion (CPU mode already honours it).
                                gpuTlsResult = TLSGpu.search(t, f, minPeriod, maxPeriod, nPeriods,
                                        minFracDur, maxFracDur, nDurations, nBinsUser,
                                        u1Grid, u2Grid, ingressGrid,
                                        tlsProgressCallback, tlsComputeBackend,
                                        tlsCancelled::get);
                            } catch (Exception gpuEx) {
                                IJ.log("[GPU TLS] Error: " + gpuEx.getMessage() + " \u2014 falling back to CPU.");
                            }
                            if (gpuTlsResult != null) {
                                tlsResult = gpuTlsResult;
                            } else {
                                // Pass the Stop flag so the CPU fallback honours it too.
                                tlsResult = TLS.search(t, f, minPeriod, maxPeriod, nPeriods,
                                        minFracDur, maxFracDur, nDurations, nBinsUser,
                                        u1Grid, u2Grid, ingressGrid,
                                        tlsProgressCallback, tlsCancelled::get);
                            }
                        } else {
                            // Pass the Stop flag so the grid-search worker pool, the
                            // outer futures drain, and the T0-refinement pool all
                            // abort promptly on a user Stop (matches BLS CPU / TLS GPU).
                            tlsResult = TLS.search(t, f, minPeriod, maxPeriod, nPeriods,
                                    minFracDur, maxFracDur, nDurations, nBinsUser,
                                    u1Grid, u2Grid, ingressGrid,
                                    tlsProgressCallback, tlsCancelled::get);
                        }

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

                        // If T0 is locked, override the T0 values returned by TLS
                        if (lockT0 && bestPeriod > 0) {
                            double tMidTLS = (t[0] + t[t.length - 1]) / 2.0;
                            long nAdjTLS = Math.round((tMidTLS - lockedT0Value) / bestPeriod);
                            double adjustedT0TLS = lockedT0Value + nAdjTLS * bestPeriod;
                            tlsResult.t0_sliding = adjustedT0TLS;
                            tlsResult.t0_minavg = adjustedT0TLS;
                            System.out.printf("[TLS DIAG] T0 locked: user=%.6f, adjusted to nearest transit=%.6f\n",
                                    lockedT0Value, adjustedT0TLS);
                        }

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
                        // The safety-net add above can push topPeaks to userNumPeaks+1
                        // when findPeaks() excluded bestIdx (happens whenever another
                        // local maximum sits within minSeparation = 1% of nPeriods of
                        // the global max — common on TLS SDE curves).  Cap here so
                        // the downstream results table, console print-out, and plot
                        // labels all agree on exactly N = userNumPeaks entries.
                        // bestIdx is guaranteed to survive the trim because it has
                        // the highest SDE and therefore leads the sorted list.
                        if (topPeaks.size() > userNumPeaks) {
                            topPeaks = new ArrayList<>(topPeaks.subList(0, userNumPeaks));
                        }

                        // Compute odd/even transit depths for this TLS iteration
                        double tlsT0ForOE = t0MaskingChoice.equals("Min average (phase bin)")
                                ? tlsResult.t0_minavg : tlsResult.t0_sliding;
                        double[] tlsOE = (!Double.isNaN(tlsT0ForOE) && bestPeriod > 0 && bestDuration > 0)
                                ? computeOddEvenDepths(t, f, bestPeriod, tlsT0ForOE, bestDuration)
                                : new double[]{Double.NaN, Double.NaN, Double.NaN, 0, 0};
                        double tlsOddDepth = tlsOE[0], tlsEvenDepth = tlsOE[1], tlsOddEvenDiffPct = tlsOE[2];
                        int tlsNOddIn = (int) tlsOE[3], tlsNEvenIn = (int) tlsOE[4];
                        // Save all peaks for this iteration
                        for (int i = 0; i < topPeaks.size(); i++) {
                            int peakIdx = topPeaks.get(i);
                            // Per-peak grid winner.  If a peak landed in a
                            // NaN-padded slot (cancelled mid-cell) we fall
                            // back to the user-entered grid midpoint so the
                            // results table shows a usable value instead of
                            // NaN in the new columns.
                            double peakU1  = tlsResult.bestU1         != null ? tlsResult.bestU1[peakIdx]          : Double.NaN;
                            double peakU2  = tlsResult.bestU2         != null ? tlsResult.bestU2[peakIdx]          : Double.NaN;
                            double peakIng = tlsResult.bestIngressFrac!= null ? tlsResult.bestIngressFrac[peakIdx] : Double.NaN;
                            if (Double.isNaN(peakU1))  peakU1  = 0.5 * (u1Min + u1Max);
                            if (Double.isNaN(peakU2))  peakU2  = 0.5 * (u2Min + u2Max);
                            if (Double.isNaN(peakIng)) peakIng = 0.5 * (ingMin + ingMax);
                            allResults.add(new TLSResult(planetFinal + 1, i + 1,
                                    tlsResult.periods[peakIdx], tlsResult.sde[peakIdx],
                                    bestDuration, bestDepth,
                                    tlsResult.t0_sliding, tlsResult.t0_minavg,
                                    tlsOddDepth, tlsEvenDepth, tlsOddEvenDiffPct,
                                    tlsNOddIn, tlsNEvenIn,
                                    peakU1, peakU2, peakIng));
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
                            double pu1  = tlsResult.bestU1         != null ? tlsResult.bestU1[peakIdx]          : Double.NaN;
                            double pu2  = tlsResult.bestU2         != null ? tlsResult.bestU2[peakIdx]          : Double.NaN;
                            double ping = tlsResult.bestIngressFrac!= null ? tlsResult.bestIngressFrac[peakIdx] : Double.NaN;
                            System.out.printf("Peak %d: P=%.6f, SDE=%.4f, grid winner u1=%.3f u2=%.3f ingress=%.3f%n",
                                    i + 1, tlsResult.periods[peakIdx], tlsResult.sde[peakIdx],
                                    pu1, pu2, ping);
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
                        Plot plot = new Plot("TLS Periodogram (Planet " + (planetFinal + 1) + ")", "Period (days)", "SDE");
                        plot.setLimits(minPeriod, maxPeriod, 0, bestSDE * 1.1);
                        if (plotXScale.equals("Log")) plot.setLogScaleX();
                        plot.add("line", tlsResult.periods, tlsResult.sde);

                        // Highlight the most dominant peak with vertical shaded band
                        if (bestIdx >= 0 && bestIdx < tlsResult.periods.length) {
                            double dominantPeriod = tlsResult.periods[bestIdx];

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

                            double yMin = 0;
                            double yMax = bestSDE * 1.1;

                            // Draw a single vertical line at the dominant peak center
                            plot.setColor(Color.RED);
                            plot.addPoints(new double[]{dominantPeriod, dominantPeriod}, new double[]{yMin, yMax}, Plot.LINE);

                            // Reset color to black for other elements
                            plot.setColor(Color.BLACK);

                            // Orange vertical lines for harmonics/aliases — up to 5 multiples.
                            double[] harmonicFactors = {0.5, 2.0, 3.0, 4.0, 5.0};
                            List<String> visibleHarmonicLabels = new ArrayList<>();
                            plot.setColor(new Color(255, 165, 0));
                            for (double factor : harmonicFactors) {
                                double p = dominantPeriod * factor;
                                if (p >= minPeriod && p <= maxPeriod) {
                                    plot.addPoints(new double[]{p, p}, new double[]{yMin, yMax}, Plot.LINE);
                                    visibleHarmonicLabels.add(factor == (int) factor
                                            ? "\u00D7" + (int) factor
                                            : "\u00D7" + factor);
                                }
                            }

                            // Gray vertical lines for secondary peaks (Peak 2..N).
                            int tlsNLabelPeaks = Math.min(topPeaks.size(), userNumPeaks);
                            if (tlsNLabelPeaks > 1) {
                                plot.setColor(Color.GRAY);
                                for (int si = 1; si < tlsNLabelPeaks; si++) {
                                    double sp = tlsResult.periods[topPeaks.get(si)];
                                    plot.addPoints(new double[]{sp, sp}, new double[]{yMin, yMax}, Plot.LINE);
                                }
                            }

                            // Legend — right-anchored so text stays inside the plot frame.
                            plot.setJustification(Plot.RIGHT);
                            plot.setColor(Color.RED);
                            plot.addLabel(0.99, 0.06, "\u2014 dominant period");
                            if (!visibleHarmonicLabels.isEmpty()) {
                                plot.setColor(new Color(255, 165, 0));
                                plot.addLabel(0.99, 0.11,
                                        "\u2014 harmonics/aliases (" + String.join(", ", visibleHarmonicLabels) + ")");
                            }
                            if (tlsNLabelPeaks > 1) {
                                plot.setColor(Color.GRAY);
                                plot.addLabel(0.99, 0.16, "\u2014 secondary peaks");
                            }
                            plot.setJustification(Plot.LEFT);
                            plot.setColor(Color.BLACK);
                        }

                        plot.addLabel(0.01, 0.95, String.format("Best: P=%.4f, SDE=%.2f", bestPeriod, bestSDE));

                        // Label top peaks on plot — Peak 1 (dominant) in red, Peak 2..N
                        // in gray, matching the vertical line colours above.
                        for (int i = 0; i < Math.min(topPeaks.size(), userNumPeaks); i++) {
                            int peakIdx = topPeaks.get(i);
                            double peakPeriod = tlsResult.periods[peakIdx];
                            plot.setColor(i == 0 ? Color.RED : Color.GRAY);
                            if (i == 0 && peakIdx == bestIdx) {
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
                        plot.setColor(Color.BLACK);
                        plot.show();
                        plot.update();
                    }
                    // After all planets, update the progress window to show completion
                    tlsProgressWin.getTextPanel().append("\nAll planet searches complete.\n");
                    // Show results in a table
                    StringBuilder resultMsg = new StringBuilder();
                    String t0ColLabel = t0MaskingChoice.equals("Min average (phase bin)") ? "T0 (masking, Min avg)" : "T0 (masking, Model center)";
                    String header = String.format("%-4s %-4s %-12s %-10s %-14s %-10s %-24s %-10s %-10s %-10s %-8s %-8s %-6s %-6s %-8s\n",
                            "Iter", "Pk", "Period", "SDE", "Duration (hr)", "Depth", t0ColLabel,
                            "Odd Depth", "Even Depth", "O-E Diff%", "#Odd pts", "#Even pts",
                            "u1*", "u2*", "ingress*");
                    String sep = String.format("%-4s %-4s %-12s %-10s %-14s %-10s %-24s %-10s %-10s %-10s %-8s %-8s %-6s %-6s %-8s\n",
                            "----", "---", "------------", "----------", "--------------", "----------", "------------------------",
                            "----------", "----------", "----------", "--------", "--------",
                            "------", "------", "--------");
                    resultMsg.append(header).append(sep);
                    int lastIter = -1;
                    for (TLSResult tr : allResults) {
                        if (lastIter != -1 && tr.iteration != lastIter) resultMsg.append("\n");
                        String t0Val = t0MaskingChoice.equals("Min average (phase bin)")
                                ? (Double.isNaN(tr.t0_minavg) ? "---" : String.format("%.6f", tr.t0_minavg))
                                : (Double.isNaN(tr.t0_sliding) ? "---" : String.format("%.6f", tr.t0_sliding));
                        double durationHours = tr.duration * 24.0;
                        String oddDepthStr  = Double.isNaN(tr.oddDepth)       ? "---" : String.format("%.4f", tr.oddDepth);
                        String evenDepthStr = Double.isNaN(tr.evenDepth)      ? "---" : String.format("%.4f", tr.evenDepth);
                        String diffPctStr   = Double.isNaN(tr.oddEvenDiffPct) ? "---" : String.format("%.1f", tr.oddEvenDiffPct);
                        resultMsg.append(String.format("%4d %3d %12.6f %10.4f %14.4f %10.4f %24s %10s %10s %10s %8d %8d %6.3f %6.3f %8.3f\n",
                                tr.iteration, tr.peakNumber, tr.period, tr.sde, durationHours, tr.depth, t0Val,
                                oddDepthStr, evenDepthStr, diffPctStr, tr.nOddIn, tr.nEvenIn,
                                tr.bestU1, tr.bestU2, tr.bestIngressFrac));
                        lastIter = tr.iteration;
                    }
                    // Show results in a JTable with Save as CSV button
                    SwingUtilities.invokeLater(() ->
                            createResultsTable(new String[]{"Iter", "Pk", "Period", "SDE", "Duration (hr)", "Depth",
                                    "T0 (masking, Model center)",
                                    "Odd Depth", "Even Depth", "O-E Diff%", "#Odd pts", "#Even pts"
                            }, resultMsg, "TLS Multi-Planet Results"));
                    } catch (Throwable th) {
                        IJ.log("[TLS] Unhandled error in background thread: " + th);
                        java.io.StringWriter sw = new java.io.StringWriter();
                        th.printStackTrace(new PrintWriter(sw));
                        IJ.log(sw.toString());
                        final String msg = th.getClass().getSimpleName()
                                + (th.getMessage() != null ? (": " + th.getMessage()) : "");
                        SwingUtilities.invokeLater(() -> IJ.showMessage(
                                "Periodogram",
                                "TLS failed with an unexpected error:\n" + msg
                                        + "\n\nSee the Log window for the full stack trace."));
                    }
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

    /**
     * Compute odd/even transit depths by separating transits into two interleaved sets.
     * Transit index 0 (at T0) is "odd", index ±1 is "even", etc.
     *
     * @return double[] { oddDepth, evenDepth, oddEvenDiffPct, nOddIn, nEvenIn }
     *         where depths are (meanOut - meanIn), diffPct = |odd-even|/max(|odd|,|even|)*100
     */
    static double[] computeOddEvenDepths(double[] time, double[] flux, double period, double t0, double duration) {
        double sumOddIn = 0, sumEvenIn = 0;
        int nOddIn = 0, nEvenIn = 0;
        double sumOut = 0;
        int nOut = 0;
        double halfDuration = 0.5 * duration;
        for (int i = 0; i < time.length; i++) {
            int transitIdx = (int) Math.round((time[i] - t0) / period);
            double tCenter = t0 + transitIdx * period;
            if (Math.abs(time[i] - tCenter) < halfDuration) {
                if (transitIdx % 2 == 0) {
                    sumOddIn += flux[i];
                    nOddIn++;
                } else {
                    sumEvenIn += flux[i];
                    nEvenIn++;
                }
            } else {
                sumOut += flux[i];
                nOut++;
            }
        }
        double meanOut = nOut > 0 ? sumOut / nOut : 1.0;
        double oddDepth  = nOddIn  > 0 ? meanOut - sumOddIn  / nOddIn  : Double.NaN;
        double evenDepth = nEvenIn > 0 ? meanOut - sumEvenIn / nEvenIn : Double.NaN;
        double oddEvenDiffPct = Double.NaN;
        if (!Double.isNaN(oddDepth) && !Double.isNaN(evenDepth)) {
            double maxD = Math.max(Math.abs(oddDepth), Math.abs(evenDepth));
            if (maxD > 0) oddEvenDiffPct = 100.0 * Math.abs(oddDepth - evenDepth) / maxD;
        }
        return new double[]{oddDepth, evenDepth, oddEvenDiffPct, nOddIn, nEvenIn};
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
    /** First index i where a[i] &gt;= value (or a.length if none). Assumes a is sorted ascending. */
    static int lowerBoundD(double[] a, double value) {
        int lo = 0, hi = a.length;
        while (lo < hi) {
            int mid = (lo + hi) >>> 1;
            if (a[mid] < value) lo = mid + 1;
            else                hi = mid;
        }
        return lo;
    }

    /** First index i where a[i] &gt; value (or a.length if none). Assumes a is sorted ascending. */
    static int upperBoundD(double[] a, double value) {
        int lo = 0, hi = a.length;
        while (lo < hi) {
            int mid = (lo + hi) >>> 1;
            if (a[mid] <= value) lo = mid + 1;
            else                 hi = mid;
        }
        return lo;
    }

    /**
     * Builds an inclusive-both-ends linear grid from {@code min} to {@code max}
     * with {@code steps} values.  Used by the TLS blind-search dialog to
     * convert the three (min, max, steps) UI triples into the
     * {@code double[]} arrays that {@link TLS#search} / {@link TLSGpu#search}
     * expect.
     *
     * <p>Corner cases:
     * <ul>
     *   <li>{@code steps \u2264 1}     \u2192 {@code [min]}        (collapses the axis to one value)</li>
     *   <li>{@code min == max}  \u2192 {@code [min]} * steps   (still fine, just redundant)</li>
     * </ul>
     */
    static double[] buildLinearGrid(double min, double max, int steps) {
        if (steps <= 1) return new double[]{min};
        double[] g = new double[steps];
        double span = max - min;
        for (int i = 0; i < steps; i++) {
            g[i] = min + i * span / (steps - 1);
        }
        return g;
    }

    // -------------------------------------------------------------------------
    // AWT tooltip helpers for the BLS / TLS settings dialogs.
    //
    // ImageJ's GenericDialog uses pure AWT components (TextField, Choice,
    // Checkbox) which have no setToolTipText.  These helpers attach a small
    // yellow pop-up "bubble" to any AWT Component, so users can hover over a
    // field to see per-field help, mirroring the Swing tooltip UX.
    // -------------------------------------------------------------------------

    /** Attaches a delayed hover-tooltip (yellow bubble) to an AWT Component. */
    private static void setAwtTooltip(Component c, String text) {
        if (c == null || text == null || text.isEmpty()) return;
        c.addMouseListener(new MouseAdapter() {
            Popup popup;
            javax.swing.Timer timer;

            @Override public void mouseEntered(MouseEvent e) {
                final Point screen = e.getLocationOnScreen();
                timer = new javax.swing.Timer(600, ev -> {
                    JLabel lbl = new JLabel(text);
                    lbl.setOpaque(true);
                    lbl.setBackground(new Color(255, 255, 225));
                    lbl.setForeground(Color.BLACK);
                    lbl.setBorder(BorderFactory.createCompoundBorder(
                            BorderFactory.createLineBorder(Color.BLACK),
                            BorderFactory.createEmptyBorder(2, 5, 2, 5)));
                    popup = PopupFactory.getSharedInstance()
                            .getPopup(c, lbl, screen.x + 14, screen.y + 18);
                    popup.show();
                });
                timer.setRepeats(false);
                timer.start();
            }

            @Override public void mouseExited(MouseEvent e) {
                if (timer != null) { timer.stop(); timer = null; }
                if (popup != null) { popup.hide(); popup = null; }
            }
        });
    }

    /**
     * Applies per-index tooltips to every component of a GenericDialog field
     * vector (e.g. the vector returned by {@link GenericDialog#getNumericFields()}).
     * Safely no-ops when the vector or a tooltip entry is null / empty, and stops
     * at the shorter of the two lengths so it tolerates future dialog edits.
     */
    private static void applyTooltips(java.util.Vector<?> components, String[] tooltips) {
        if (components == null || tooltips == null) return;
        int n = Math.min(components.size(), tooltips.length);
        for (int i = 0; i < n; i++) {
            Object c = components.get(i);
            if (c instanceof Component) setAwtTooltip((Component) c, tooltips[i]);
        }
    }

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
                      double t0_masking_min_avg,
                      double oddDepth, double evenDepth, double oddEvenDiffPct, int nOddIn, int nEvenIn) {
    }

    private record LSResult(int peakNumber, double period, double power) {
    }

    private record TLSResult(int iteration, int peakNumber, double period, double sde, double duration,
                     double depth, double t0_sliding, double t0_minavg,
                     double oddDepth, double evenDepth, double oddEvenDiffPct, int nOddIn, int nEvenIn,
                     /* Grid-winning template parameters at this peak's period.
                      * Useful for (a) diagnosing which limb-darkening cell
                      * captured the signal and (b) seeding a follow-up
                      * fixed-template refit outside AIJ. */
                     double bestU1, double bestU2, double bestIngressFrac) {
    }
}