package Astronomy;

import Astronomy.multiplot.optimization.BicFitting;
import Astronomy.multiplot.optimization.CompStarFitting;
import Astronomy.multiplot.optimization.Optimizer;
import astroj.MeasurementTable;
import astroj.SpringUtil;
import ij.IJ;
import ij.Prefs;
import ij.astro.gui.GenericSwingDialog;
import ij.astro.io.prefs.Property;
import ij.astro.logging.AIJLogger;
import ij.astro.util.UIHelper;
import ij.measure.ResultsTable;
import ij.util.ArrayUtil;
import ij.util.FontUtil;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.BiFunction;
import java.util.regex.Pattern;

import static Astronomy.MultiPlot_.*;

public class FitOptimization implements AutoCloseable {
    protected static final String PREFS_ENABLELOG = "fitoptimization.enablelog";
    protected static final String PREFS_NSIGMA = "fitoptimization.nsigma";
    protected static final String PREFS_MAX_DETREND = "fitoptimization.maxdetrend";
    protected static final String PREFS_BIC_THRESHOLD = "fitoptimization.bict";
    private static final Property<Integer> maxThreads = new Property<>(getThreadCount(), FitOptimization.class);
    private static final Property<Long> minChunkSize = new Property<>(512L, FitOptimization.class);
    private static final Property<Boolean> autoMaxThreads = new Property<>(true, FitOptimization.class);
    private final static Pattern apGetter = Pattern.compile("rel_flux_[ct]([0-9]+)");
    private static final HashSet<FitOptimization> INSTANCES = new HashSet<>();
    /**
     * The change in the comparator to determine improvement
     */
    public static double EPSILON;
    private static LinkedList<CleanTracker> undoBuffer = new LinkedList<>();
    static boolean showOptLog = false;
    private static int maxDetrend = 1;
    private static double bict = 2;
    private static double nSigmaOutlier = 5;
    private final int curve;
    public DynamicCounter compCounter;
    public DynamicCounter detrendCounter;
    public JSpinner detrendParamCount;
    public JTextField cleanNumTF = new JTextField("0");
    CompletionService<MinimumState> completionService;
    private ScheduledExecutorService ipsExecutorService;
    private BigInteger iterRemainingOld = BigInteger.ZERO;
    private int targetStar;
    /**
     * The index of this array is the selected option,
     * the value of the array is the option index in the relevant {@link MultiPlot_} array.
     * For reference stars, it is "initially selected reference star" -> "reference star index."
     */
    private int[] selectable2PrimaryIndex;
    private ExecutorService pool;
    private boolean[] selectable;
    private JPanel detOptiCards;
    private JPanel compOptiCards;
    private RollingAvg rollingAvg = new RollingAvg();
    private JSpinner detrendEpsilon;
    private final JTextField difNumTF = new JTextField("0");
    private static final Property<CompStarFitting.Mode> compStarMode = new Property<>(CompStarFitting.Mode.QUICK, FitOptimization.class);
    private static final Property<CleanMode> CLEAN_MODE = new Property<>(CleanMode.RMS, FitOptimization.class);

    // Init. after numAps is set
    private FitOptimization(int curve, int epsilon) {
        this.curve = curve;
        EPSILON = epsilon;
        setupThreadedSpace();
        INSTANCES.add(this);
        nSigmaOutlier = Prefs.get(PREFS_NSIGMA, nSigmaOutlier);
        showOptLog = Prefs.get(PREFS_ENABLELOG, showOptLog);
        bict = Prefs.get(PREFS_BIC_THRESHOLD, bict);
        maxDetrend = (int) Prefs.get(PREFS_MAX_DETREND, maxDetrend);
    }

    public static FitOptimization getOrCreateInstance(int curve, int epsilon) {
        var m = INSTANCES.stream().filter(f -> f.curve == curve).findFirst();
        return m.orElseGet(() -> new FitOptimization(curve, epsilon));
    }

    public static void clearCleanHistory() {
        INSTANCES.removeIf(Objects::isNull);
        INSTANCES.forEach(FitOptimization::clearHistory);
    }

    private static void setFinalRefStarState(String minimizationTarget, boolean[] state) {
        setNewStars(state);

        if (showOptLog) AIJLogger.log("Found minimum " + minimizationTarget + " state, reference stars set.");
        IJ.beep();
    }

    /**
     * Ignores hyper-threading.
     *
     * @return an estimate of the number of available threads that can be used for minimization
     */
    private static int getThreadCount() {
        final int maxRealThreads = Runtime.getRuntime().availableProcessors();
        return Math.max(1 + (maxRealThreads / 3), maxRealThreads - 4);
    }

    public static void savePrefs() {
        Prefs.set(PREFS_NSIGMA, nSigmaOutlier);
        Prefs.set(PREFS_ENABLELOG, showOptLog);
        Prefs.set(PREFS_MAX_DETREND, maxDetrend);
        Prefs.set(PREFS_BIC_THRESHOLD, bict);
    }

    public void clearHistory() {
        undoBuffer.clear();
        INSTANCES.forEach(f -> {
            f.cleanNumTF.setText("0");
            f.difNumTF.setText("0");
        });
    }

    public void setSelectable(boolean[] selectable) {
        this.selectable = selectable;
        this.selectable2PrimaryIndex = new int[selectable.length];
    }

    public void setSelectable(int selectableSize) {
        this.selectable = new boolean[selectableSize];
        Arrays.fill(this.selectable, true);
        this.selectable2PrimaryIndex = new int[selectableSize];
    }

    public Component makeFitOptimizationPanel() {
        JPanel fitOptimizationPanel = new JPanel(new SpringLayout());
        fitOptimizationPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(MultiPlot_.mainBorderColor, 1), "Fit Optimization", TitledBorder.LEFT, TitledBorder.TOP, MultiPlot_.b12, Color.darkGray));

        var outlierRemoval = new JPanel(new SpringLayout());
        outlierRemoval.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(MultiPlot_.subBorderColor, 1), "Outlier Removal", TitledBorder.CENTER, TitledBorder.TOP, MultiPlot_.p11, Color.darkGray));
        var undoButton = new JButton(UIHelper.createImageIcon("astroj/images/icons/undo.png", 14, 14));
        var undoFont = undoButton.getFont();
        undoButton.addActionListener($ -> undoOutlierClean());
        undoButton.setToolTipText("<html>Undo clean<br>(up to 10 levels)</html>");
        outlierRemoval.add(undoButton);

        var options = Arrays.stream(CleanMode.values()).filter(CleanMode::isMenuDisplayable).toArray(CleanMode[]::new);
        var cleanModeSelection = new JComboBox<>(options);
        cleanModeSelection.setToolTipText("""
                <html>
                Controls the method by which data is removed.
                </html>
                """);
        cleanModeSelection.setSelectedItem(CLEAN_MODE.get());
        cleanModeSelection.setRenderer(new ToolTipRenderer());
        cleanModeSelection.addActionListener(e -> {
            CLEAN_MODE.set((CleanMode) cleanModeSelection.getSelectedItem());
        });
        outlierRemoval.add(cleanModeSelection);
        var cleanButton = new JButton("Clean");
        cleanButton.addActionListener($ ->
                cleanOutliers((CleanMode) cleanModeSelection.getSelectedItem(), FitOptimization.this, FitOptimization.this.curve));
        outlierRemoval.add(cleanButton);
        outlierRemoval.add(Box.createHorizontalGlue());
        var b = Box.createHorizontalBox();
        b = Box.createHorizontalBox();
        b.add(b.add(Box.createHorizontalGlue()));
        var cleanLabel = new JLabel("N:");
        cleanLabel.setHorizontalAlignment(SwingConstants.CENTER);
        cleanLabel.setToolTipText("The number of sigma away from the model to clean.");
        b.add(cleanLabel);

        var cleanSpin = new JSpinner(new SpinnerNumberModel(nSigmaOutlier, 1d, 100, 1d));
        addMouseListener(cleanSpin);
        cleanSpin.addChangeListener($ -> nSigmaOutlier = ((Number) cleanSpin.getValue()).doubleValue());
        cleanSpin.setToolTipText("The number of sigma away from the model to clean.");
        b.add(Box.createHorizontalStrut(5));
        b.add(cleanSpin);
        b.add(Box.createHorizontalGlue());
        outlierRemoval.add(b);
        b = Box.createHorizontalBox();
        cleanNumTF.setEditable(false);
        cleanNumTF.setHorizontalAlignment(SwingConstants.RIGHT);
        cleanNumTF.setColumns(3);
        cleanNumTF.setToolTipText("Last operation number of data points removed (-) or restored (+).");
        b.add(cleanNumTF);
        b.add(Box.createHorizontalGlue());
        difNumTF.setEditable(false);
        difNumTF.setHorizontalAlignment(SwingConstants.RIGHT);
        difNumTF.setColumns(3);
        difNumTF.setToolTipText("Total number of data points removed.");
        b.add(difNumTF);
        outlierRemoval.add(b);
        SpringUtil.makeCompactGrid(outlierRemoval, 2, outlierRemoval.getComponentCount() / 2, 0, 0, 0, 0);
        fitOptimizationPanel.add(outlierRemoval);

        JPanel compStarPanel = new JPanel(new SpringLayout());
        compStarPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(MultiPlot_.subBorderColor, 1), "Comparison Star Selection", TitledBorder.CENTER, TitledBorder.TOP, MultiPlot_.p11, Color.darkGray));

        var compOptimizationSelection = new JComboBox<ToolTipWrapper>();
        compOptimizationSelection.setEditable(false);
        compOptimizationSelection.setRenderer(new ToolTipRenderer());
        var compBruteForce = new ToolTipWrapper("Exhaustive", "Exhaustive search of comparison star combinations for minimize RMS of the fit. Only comparison stars selected at the start of this run are searched.");
        var compQuickOpti = new ToolTipWrapper("Old Quick", "Search for ensemble by testing the toggling of individual stars. Completes when the RMS converges. Only comparison stars selected at the start of this run are searched.");
        var simpleQuickOpti = new ToolTipWrapper("Quick", "Search for ensemble by testing the toggling of individual stars. Completes when the RMS converges. Only comparison stars selected at the start of this run are searched.");
        var compTest = new ToolTipWrapper("Debug", "Debug a single run.");
        compOptimizationSelection.addItem(compBruteForce);
        //compOptimizationSelection.addItem(compQuickOpti);
        compOptimizationSelection.addItem(simpleQuickOpti);
        if (IJ.isAijDev()) compOptimizationSelection.addItem(compTest);

        compOptiCards = new JPanel(new CardLayout());
        var compOptiButtonStart = new JButton("Start");
        var compOptiButtonCancel = new JToggleButton("Cancel", true);

        compOptiCards.add(compOptiButtonStart);
        compOptiCards.add(compOptiButtonCancel);

        switch (compStarMode.get()) {
            case EXHAUSTIVE -> compOptimizationSelection.setSelectedItem(compBruteForce);
            case MODERATE -> compOptimizationSelection.setSelectedItem(compQuickOpti);
            case QUICK -> compOptimizationSelection.setSelectedItem(simpleQuickOpti);
        }

        compOptiButtonStart.addActionListener($ -> {
            CardLayout cl = (CardLayout) compOptiCards.getLayout();
            cl.next(compOptiCards);
            if (Objects.equals(compOptimizationSelection.getSelectedItem(), compTest)) {
                testCompMin();
                MultiPlot_.updatePlot(curve);
            } else if (Objects.equals(compOptimizationSelection.getSelectedItem(), compBruteForce)) {
                compStarMode.set(CompStarFitting.Mode.EXHAUSTIVE);
                Executors.newSingleThreadExecutor().submit(this::minimizeCompStarsByBruteForce);
            } else if (Objects.equals(compOptimizationSelection.getSelectedItem(), compQuickOpti)) {
                compStarMode.set(CompStarFitting.Mode.MODERATE);
                Executors.newSingleThreadExecutor().submit(this::minimizeCompStarsByQuickOpti);
            } else if (Objects.equals(compOptimizationSelection.getSelectedItem(), simpleQuickOpti)) {
                compStarMode.set(CompStarFitting.Mode.QUICK);
                Executors.newSingleThreadExecutor().submit(this::minimizeCompStarsBySimpleOpti);
            }
        });

        compOptiButtonCancel.addActionListener($ -> {
            CardLayout cl = (CardLayout) compOptiCards.getLayout();
            cl.next(compOptiCards);
            pool.shutdownNow();
            ipsExecutorService.shutdown();
            IJ.showProgress(1);
            IJ.showStatus("");
        });

        compOptiCards.setToolTipText("Begin the optimization. Click again to stop it.");

        compStarPanel.add(compOptimizationSelection);
        compStarPanel.add(compOptiCards);

        var compOptiIterLabel = new JLabel("Iter. Remaining:");
        compOptiIterLabel.setToolTipText("Number of iterations remaining in comp. star optimization.");
        compOptiIterLabel.setHorizontalAlignment(SwingConstants.CENTER);
        compStarPanel.add(compOptiIterLabel);

        var compOptiIterCount = new JTextField("N/A");
        compOptiIterCount.setEditable(false);
        compOptiIterCount.setMaximumSize(new Dimension(50, 10));
        compOptiIterCount.setHorizontalAlignment(SwingConstants.RIGHT);
        compOptiIterCount.setToolTipText("Number of iterations remaining in comp. star optimization.");
        compCounter = new DynamicCounter(compOptiIterCount);
        compStarPanel.add(compOptiIterCount);

        SpringUtil.makeCompactGrid(compStarPanel, 2, compStarPanel.getComponentCount() / 2, 0, 0, 0, 0);

        JPanel detrendOptPanel = new JPanel(new SpringLayout());
        detrendOptPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(MultiPlot_.subBorderColor, 1), "Detrend Parameter Selection", TitledBorder.CENTER, TitledBorder.TOP, MultiPlot_.p11, Color.darkGray));

        var pLabel = new JLabel("Max Pars:");
        pLabel.setHorizontalAlignment(SwingConstants.CENTER);
        pLabel.setToolTipText("The maximum number of detrend parameters to be enabled.");
        detrendOptPanel.add(pLabel);

        detrendParamCount = new JSpinner(new SpinnerNumberModel(maxDetrend, 0, 99, 1));//todo we only need at most 3 digits
        detrendParamCount.addChangeListener($ -> maxDetrend = ((Number) detrendParamCount.getValue()).intValue());
        addMouseListener(detrendParamCount);
        detrendParamCount.setToolTipText("The maximum number of detrend parameters to be enabled.");
        detrendOptPanel.add(detrendParamCount);

        var detrendOptimizationSelection = new JComboBox<ToolTipWrapper>();
        detrendOptimizationSelection.setEditable(false);
        detrendOptimizationSelection.setRenderer(new ToolTipRenderer());
        var detrendBruteForce = new ToolTipWrapper("Exhaustive", "Exhaustive search of parameter combinations for minimum BIC of the fit. All set parameters are searched.");
        var detrendTest = new ToolTipWrapper("Debug", "Debug a single run.");
        detrendOptimizationSelection.addItem(detrendBruteForce);
        if (IJ.isAijDev()) detrendOptimizationSelection.addItem(detrendTest);

        detOptiCards = new JPanel(new CardLayout());

        var detOptiButtonStart = new JButton("Start");
        var detOptiButtonCancel = new JToggleButton("Cancel", true);
        detOptiCards.add(detOptiButtonStart);
        detOptiCards.add(detOptiButtonCancel);

        detOptiButtonStart.addActionListener($ -> {
            CardLayout cl = (CardLayout) detOptiCards.getLayout();
            cl.next(detOptiCards);
            if (Objects.equals(detrendOptimizationSelection.getSelectedItem(), detrendTest)) {
                testParamMin();
                MultiPlot_.updatePlot(curve);
            } else if (Objects.equals(detrendOptimizationSelection.getSelectedItem(), detrendBruteForce)) {
                Executors.newSingleThreadExecutor().submit(this::minimizeParams);
            }
        });

        detOptiButtonCancel.addActionListener($ -> {
            CardLayout cl = (CardLayout) detOptiCards.getLayout();
            cl.next(detOptiCards);
            pool.shutdownNow();
            ipsExecutorService.shutdown();
            IJ.showProgress(1);
            IJ.showStatus("");
        });

        detOptiCards.setToolTipText("Begin the optimization. Click again to stop it.");
        detrendOptPanel.add(detrendOptimizationSelection);
        detrendOptPanel.add(detOptiCards);

        var eLabel = new JLabel("BIC Thres:");
        eLabel.setHorizontalAlignment(SwingConstants.CENTER);
        eLabel.setToolTipText("The required change in BIC between selected states to be considered a better value.");
        detrendOptPanel.add(eLabel);

        detrendEpsilon = new JSpinner(new SpinnerNumberModel(bict, 0D, 99, 1));
        addMouseListener(detrendEpsilon);
        detrendEpsilon.addChangeListener($ -> bict = ((Number) detrendEpsilon.getValue()).doubleValue());
        detrendEpsilon.setToolTipText("The required change in BIC between selected states to be considered a better value.");
        detrendOptPanel.add(detrendEpsilon);

        var paramOptiIterLabel = new JLabel("Iter. Remaining:");
        paramOptiIterLabel.setToolTipText("Number of iterations remaining in detrend parameter optimization.");
        paramOptiIterLabel.setHorizontalAlignment(SwingConstants.CENTER);
        detrendOptPanel.add(paramOptiIterLabel);

        var paramOptiIterCount = new JTextField("N/A");
        paramOptiIterCount.setEditable(false);
        paramOptiIterCount.setMaximumSize(new Dimension(50, 10));
        paramOptiIterCount.setToolTipText("Number of iterations remaining in detrend parameter optimization.");
        paramOptiIterCount.setHorizontalAlignment(SwingConstants.RIGHT);
        detrendCounter = new DynamicCounter(paramOptiIterCount);
        detrendOptPanel.add(paramOptiIterCount);

        SpringUtil.makeCompactGrid(detrendOptPanel, 2, detrendOptPanel.getComponentCount() / 2, 0, 0, 0, 0);

        JPanel logOptPanel = new JPanel(new SpringLayout());
        var t = new JLabel("Log");
        logOptPanel.add(t);
        var logCheckBox = new JCheckBox("", showOptLog);
        logCheckBox.addActionListener($ -> showOptLog = logCheckBox.isSelected());
        logCheckBox.setToolTipText("Display a log of optimization actions.");
        logOptPanel.add(logCheckBox);

        SpringUtil.makeCompactGrid(logOptPanel, 2, 1, 2, 2, 2, 2);

        fitOptimizationPanel.add(compStarPanel);
        fitOptimizationPanel.add(detrendOptPanel);
        //fitOptimizationPanel.add(logOptPanel);
        SpringUtil.makeCompactGrid(fitOptimizationPanel, 1, fitOptimizationPanel.getComponentCount(), 2, 2, 2, 2);

        UIHelper.recursiveFontSetter(fitOptimizationPanel, p12);
        undoButton.setFont(undoFont);
        return fitOptimizationPanel;
    }

    public static void showThreadingPanel(Frame owner) {
        var gd = new GenericSwingDialog("Optimization Threading Preferences", owner);
        gd.addCheckbox("Auto max threads", autoMaxThreads.get(), b -> autoMaxThreads.set(b));
        var x = gd.addBoundedNumericField("Max Threads:", new GenericSwingDialog.Bounds(1, 256),
                maxThreads.get(), 1, 7, "", true, d -> maxThreads.set(d.intValue()));
        gd.addBoundedNumericField("Minimum Chunk Size:", new GenericSwingDialog.Bounds(1, Integer.MAX_VALUE),
                minChunkSize.get(), 1, 7, "", true, d -> minChunkSize.set(d.longValue()));
        gd.addMessage("Default: " + 512);
        autoMaxThreads.addListener((k, b) -> {
            if (b) {
                ((JSpinner) x.c1()).setValue((double)getThreadCount());
                x.c1().setEnabled(false);
            } else {
                x.c1().setEnabled(true);
            }
        });
        if (autoMaxThreads.get()) {
            x.c1().setEnabled(false);
        } else {
            x.c1().setEnabled(true);
        }
        gd.centerDialog(true);
        gd.showDialog();
        autoMaxThreads.clearListeners();
    }

    public static void cleanOutliers(CleanMode cleanMode, FitOptimization fo, int curve) {
        if (fo != null && !plotY[curve]) {
            IJ.error("The 'Plot' check box for this data set must be enabled in Multi-Plot Y-data panel for optimization.");
            return;
        }

        int holdBinSize = 1;
        boolean holdUseDMarker1 = false;
        boolean holdUseDMarker4 = false;
        if (inputAverageOverSize[curve] > 1 || useDMarker1) {
            holdBinSize = inputAverageOverSize[curve];
            holdUseDMarker1 = useDMarker1;
            holdUseDMarker4 = useDMarker4;
            inputAverageOverSize[curve] = 1;
            useDMarker1 = false;
            useDMarker4 = false;
            updatePlot(updateOneFit(curve));
            for (int i = 0; i < 30; i++) {
                IJ.wait(100);
                if (!updatePlotRunning) {
                    break;
                }
                if (i == 29) {
                    IJ.error("Unsuccessfully attempted to disable left trim and/or set Binsize = 1 for outlier cleaning. Aborting.");
                    return;
                }
            }
        }
        var oldTable = (MeasurementTable) table.clone();

        var hasActionToUndo = false;
        var toRemove = new TreeSet<Integer>();


        var sigma = switch (cleanMode) {
            case RMS -> MultiPlot_.sigma[curve];
            case POINT_MEDIAN -> ArrayUtil.median(Arrays.copyOf(yerr[curve], nn[curve]));
            default -> 0;
        };

        // Residuals and RMS are only calculated w/ transit fit
        var res = new double[nn[curve]];
        if (!useTransitFit[curve]) {
            sigma = 0;
            var med = ArrayUtil.median(Arrays.copyOf(y[curve], nn[curve]));
            for (int i = 0; i < nn[curve]; i++) {
                res[i] = y[curve][i] - med;
                sigma += res[i] * res[i];
            }
            sigma /= nn[curve];
            sigma = Math.sqrt(sigma);
        }
        for (int i = 0; i < nn[curve]; i++) {
            if (cleanMode == CleanMode.PRECISION) break;
            if (cleanMode == CleanMode.POINT) sigma = yerr[curve][i];
            var comparator = switch (cleanMode) {
                case POINT_MEDIAN -> yerr[curve][i];
                default -> {
                    // Residuals are only calculated w/ transit fit
                    if (!useTransitFit[curve]) {
                        yield res[i];
                    } else {
                        yield residual[curve][i];
                    }
                }
            };
            if (Math.abs(comparator) > Math.abs(nSigmaOutlier * sigma)) {
                hasActionToUndo = true;
                toRemove.add(excludedHeadSamples + i);
                //if (showOptLog) AIJLogger.log("Datapoint removed because residual > n * yerr: "+Math.abs(residual[curve][i])+" > "+Math.abs(nSigmaOutlier * yerr[curve][i]));
            }

            if (cleanMode != CleanMode.POINT_MEDIAN) {
                if (i+1 >= (!useTransitFit[curve] ? res : residual[curve]).length) {
                    break;
                }
            }
        }

        if (cleanMode == CleanMode.PRECISION) {
            selectedRowStart = measurementsWindow.getSelectionStart();
            selectedRowEnd = measurementsWindow.getSelectionEnd();
            for (int i = selectedRowEnd; i >= selectedRowStart; i--) {
                toRemove.add(i);
                hasActionToUndo = true;
            }
        }

        if (hasActionToUndo) {
            for (Integer i : toRemove.descendingSet()) {
                //if (showOptLog) AIJLogger.log("row["+i+"] removed");
                table.deleteRow(i);
            }

            INSTANCES.forEach(f -> f.cleanNumTF.setText("-" + toRemove.size()));
            undoBuffer.addFirst(new CleanTracker(cleanMode, oldTable, toRemove));
            if (undoBuffer.size() > 10) undoBuffer.remove(9);
            // If the table is empty MP proceeds with no errors and doesn't update the plot
            if (table.size() == 0) IJ.error("Cleaning", "Removed all points in the table, " +
                    "please undo and increase the number of sigma being used");
            MultiPlot_.updatePlot(MultiPlot_.updateAllFits());
        } else {
            IJ.beep();
            INSTANCES.forEach(f -> f.cleanNumTF.setText("0"));
        }

        if (showOptLog) AIJLogger.log(toRemove.size() + " new outliers removed");

        if (holdBinSize > 1 || holdUseDMarker1) {
            inputAverageOverSize[curve] = holdBinSize;
            useDMarker1 = holdUseDMarker1;
            useDMarker4 = holdUseDMarker4;
            MultiPlot_.updatePlot(MultiPlot_.updateOneFit(curve));
            for (int i = 0; i < 300; i++) {
                IJ.wait(10);
                if (!updatePlotRunning) break;
                if (i == 299) {
                    IJ.error("Unsuccessfully attempted to replot after re-enabling left trim and/or set Binsize = 1 after outlier cleaning.");
                }
            }
        }
        table.show();
        measurementsWindow = MeasurementTable.getMeasurementsWindow(MeasurementTable.longerName(tableName));

        INSTANCES.forEach(f -> f.difNumTF.setText("" + (!undoBuffer.isEmpty() ? table.size() - undoBuffer.getLast().table.size() : "0")));

        savePrefs();
    }

    public static void undoOutlierClean() {
        if (!undoBuffer.isEmpty()) {
            var rs = table.size();
            var t = undoBuffer.pop();
            table = t.table;
            INSTANCES.forEach(f -> f.cleanNumTF.setText("+" + (table.size() - rs)));
            MultiPlot_.updatePlot(MultiPlot_.updateAllFits());
            table.show();
            measurementsWindow = MeasurementTable.getMeasurementsWindow(MeasurementTable.longerName(tableName));
            if (t.mode == CleanMode.PRECISION && measurementsWindow != null) {
                selectedRowEnd = t.removedRows.last();
                selectedRowStart = t.removedRows.first();
                measurementsWindow.setSelection(t.removedRows.first(), t.removedRows.last());
            }
        } else {
            IJ.beep();
        }
        INSTANCES.forEach(f -> f.difNumTF.setText("" + (!undoBuffer.isEmpty() ? table.size() - undoBuffer.getLast().table.size() : "0")));
    }

    private void testCompMin() {
        selectable = null;
        selectable2PrimaryIndex = null;
        CurveFitter.invalidateInstance();
        if (MultiPlot_.refStarFrame == null) MultiPlot_.showRefStarJPanel();
        if (MultiPlot_.isRefStar != null) {
            setSelectable(MultiPlot_.isRefStar);
        } else {
            IJ.error("Apertures must be present in the table");
            finishOptimization(compOptiCards);
            return;
        }

        for (int ap = 0; ap < numAps; ap++) {
            if (table.getColumnIndex("Source_Error_" + (isRefStar[ap] ? "C" : "T") + (ap + 1))
                    == ResultsTable.COLUMN_NOT_FOUND) {
                IJ.error("Missing Source_Error columns for 1 or more stores.\nEnable in Aperture settings and rerun photometry.");
                CardLayout cl = (CardLayout) compOptiCards.getLayout();
                cl.next(compOptiCards);
                return;
            }
        }

        setTargetStar();

        BigInteger initState = createBinaryRepresentation(selectable);
        var x = CurveFitter.getInstance(curve, targetStar).fitCurveAndGetResults(setArrayToState(initState));
        if (showOptLog) AIJLogger.log(x);
        finishOptimization(compOptiCards);
    }

    private void minimizeCompStarsByBruteForce() {
        selectable = null;
        selectable2PrimaryIndex = null;
        CurveFitter.invalidateInstance();
        if (MultiPlot_.refStarFrame == null) MultiPlot_.showRefStarJPanel();
        if (MultiPlot_.isRefStar != null) {
            setSelectable(MultiPlot_.isRefStar);
        } else {
            IJ.error("Apertures must be present in the table");
            finishOptimization(compOptiCards);
            return;
        }

        if (!plotY[curve]) {
            IJ.error("The 'Plot' check box for this data set must be enabled in Multi-Plot Y-data panel for optimization.");
            CardLayout cl = (CardLayout) compOptiCards.getLayout();
            cl.next(compOptiCards);
            return;
        }

        for (int ap = 0; ap < numAps; ap++) {
            if (table.getColumnIndex("Source_Error_" + (isRefStar[ap] ? "C" : "T") + (ap + 1))
                    == ResultsTable.COLUMN_NOT_FOUND) {
                IJ.error("Missing Source_Error columns for 1 or more stores.\nEnable in Aperture settings and rerun photometry.");
                CardLayout cl = (CardLayout) compOptiCards.getLayout();
                cl.next(compOptiCards);
                return;
            }
        }

        setTargetStar();

        BigInteger initState = createBinaryRepresentation(selectable); //numAps has number of apertures

        compCounter.setBasis(initState.subtract(BigInteger.ONE)); // Subtract 1 as 0-state is skipped
        scheduleIpsCounter(0);

        var finalState = divideTasksAndRun(new MinimumState(initState, Double.MAX_VALUE),
                (start, end) -> new CompStarFitting(start, end, this, CompStarFitting.Mode.EXHAUSTIVE));

        setFinalRefStarState("RMS", finalState.stateArray);
        compCounter.setBasis(BigInteger.ZERO);
        finishOptimization(compOptiCards);
    }

    private void minimizeCompStarsByQuickOpti() {
        selectable = null;
        selectable2PrimaryIndex = null;
        CurveFitter.invalidateInstance();
        if (MultiPlot_.refStarFrame == null) MultiPlot_.showRefStarJPanel();
        if (MultiPlot_.isRefStar != null) {
            setSelectable(MultiPlot_.isRefStar);
        } else {
            IJ.error("Apertures must be present in the table");
            finishOptimization(compOptiCards);
            return;
        }

        if (!plotY[curve]) {
            IJ.error("The 'Plot' check box for this data set must be enabled in Multi-Plot Y-data panel for optimization.");
            CardLayout cl = (CardLayout) compOptiCards.getLayout();
            cl.next(compOptiCards);
            return;
        }

        for (int ap = 0; ap < numAps; ap++) {
            if (table.getColumnIndex("Source_Error_" + (isRefStar[ap] ? "C" : "T") + (ap + 1))
                    == ResultsTable.COLUMN_NOT_FOUND) {
                IJ.error("Missing Source_Error columns for 1 or more stores.\nEnable in Aperture settings and rerun photometry.");
                CardLayout cl = (CardLayout) compOptiCards.getLayout();
                cl.next(compOptiCards);
                return;
            }
        }

        setTargetStar();

        BigInteger initState = createBinaryRepresentation(selectable); //numAps has number of apertures

        compCounter.setBasis(initState.subtract(BigInteger.ONE)); // Subtract 1 as 0-state is skipped
        scheduleIpsCounter(0);

        var finalState = divideTasksAndRun(new MinimumState(initState, Double.MAX_VALUE),
                ($, end) -> new CompStarFitting(end, this, CompStarFitting.Mode.MODERATE), false);

        setFinalRefStarState("RMS", finalState.stateArray);
        compCounter.setBasis(BigInteger.ZERO);
        finishOptimization(compOptiCards);
    }

    private void minimizeCompStarsBySimpleOpti() {
        selectable = null;
        selectable2PrimaryIndex = null;
        CurveFitter.invalidateInstance();
        if (MultiPlot_.refStarFrame == null) MultiPlot_.showRefStarJPanel();
        if (MultiPlot_.isRefStar != null) {
            setSelectable(MultiPlot_.isRefStar);
        } else {
            IJ.error("Apertures must be present in the table");
            finishOptimization(compOptiCards);
            return;
        }

        if (!plotY[curve]) {
            IJ.error("The 'Plot' check box for this data set must be enabled in Multi-Plot Y-data panel for optimization.");
            CardLayout cl = (CardLayout) compOptiCards.getLayout();
            cl.next(compOptiCards);
            return;
        }

        for (int ap = 0; ap < numAps; ap++) {
            if (table.getColumnIndex("Source_Error_" + (isRefStar[ap] ? "C" : "T") + (ap + 1))
                    == ResultsTable.COLUMN_NOT_FOUND) {
                IJ.error("Missing Source_Error columns for 1 or more stores.\nEnable in Aperture settings and rerun photometry.");
                CardLayout cl = (CardLayout) compOptiCards.getLayout();
                cl.next(compOptiCards);
                return;
            }
        }

        setTargetStar();

        BigInteger initState = createBinaryRepresentation(selectable); //numAps has number of apertures

        compCounter.setBasis(initState.subtract(BigInteger.ONE)); // Subtract 1 as 0-state is skipped
        scheduleIpsCounter(0);

        var finalState = divideTasksAndRun(new MinimumState(initState, Double.MAX_VALUE),
                ($, end) -> new CompStarFitting(end, this, CompStarFitting.Mode.QUICK), false);

        setFinalRefStarState("RMS", finalState.stateArray);
        compCounter.setBasis(BigInteger.ZERO);
        finishOptimization(compOptiCards);
    }

    private void testParamMin() {
        selectable = null;
        selectable2PrimaryIndex = null;

        for (int ap = 0; ap < numAps; ap++) {
            if (table.getColumnIndex("Source_Error_" + (isRefStar[ap] ? "C" : "T") + (ap + 1))
                    == ResultsTable.COLUMN_NOT_FOUND) {
                IJ.error("Missing Source_Error columns for 1 or more stores.\nEnable in Aperture settings and rerun photometry.");
                CardLayout cl = (CardLayout) detOptiCards.getLayout();
                cl.next(detOptiCards);
                return;
            }
        }

        setSelectable((int) Arrays.stream(MultiPlot_.detrendIndex[curve]).filter(i -> i != 0).count());

        if (MultiPlot_.refStarFrame == null) MultiPlot_.showRefStarJPanel();
        CurveFitter.invalidateInstance();

        setTargetStar();

        var x = CurveFitter.getInstance(curve, targetStar).fitCurveAndGetResults(MultiPlot_.detrendIndex[curve]);
        if (showOptLog) AIJLogger.log(x);
        finishOptimization(detOptiCards);
        EPSILON = 0;
    }

    private void minimizeParams() {
        selectable = null;
        selectable2PrimaryIndex = null;

        setSelectable(MultiPlot_.detrendIndex[curve].length);
        if (selectable.length < 2) {
            IJ.error("More than one detrend parameter is needed for optimization");
            CardLayout cl = (CardLayout) detOptiCards.getLayout();
            cl.next(detOptiCards);
            return;
        }

        if (!plotY[curve]) {
            IJ.error("The 'Plot' check box for this data set must be enabled in Multi-Plot Y-data panel for optimization.");
            CardLayout cl = (CardLayout) detOptiCards.getLayout();
            cl.next(detOptiCards);
            return;
        }

        for (int ap = 0; ap < numAps; ap++) {
            if (table.getColumnIndex("Source_Error_" + (isRefStar[ap] ? "C" : "T") + (ap + 1))
                    == ResultsTable.COLUMN_NOT_FOUND) {
                IJ.error("Missing Source_Error columns for 1 or more stores.\nEnable in Aperture settings and rerun photometry.");
                CardLayout cl = (CardLayout) detOptiCards.getLayout();
                cl.next(detOptiCards);
                return;
            }
        }

        if (MultiPlot_.refStarFrame == null) MultiPlot_.showRefStarJPanel();
        CurveFitter.invalidateInstance();
        EPSILON = (double) detrendEpsilon.getValue();

        setTargetStar();

        BigInteger initState = createBinaryRepresentation(selectable); //numAps has number of apertures

        detrendCounter.setBasis(initState.subtract(BigInteger.ONE)); // Subtract 1 as 0-state is skipped
        scheduleIpsCounter(1);

        var finalState = divideTasksAndRun(new MinimumState(initState, Double.MAX_VALUE),
                (start, end) -> new BicFitting(start, end, this), false);

        if (finalState.outState instanceof int[] x) setFinalState(x);

        finishOptimization(detOptiCards);
        detrendCounter.setBasis(BigInteger.ZERO);
        EPSILON = 0;
    }

    private void setTargetStar() {
        var match = apGetter.matcher(MultiPlot_.ylabel[curve].toLowerCase(Locale.ENGLISH));
        try {
            if (match.matches()) {
                targetStar = Integer.parseInt(match.groupCount() == 1 ? match.group(1) : match.group()) - 1;
            }
        } catch (NumberFormatException ignored) {
            IJ.error("Optimization must be run on a curve representing an aperture.");
        }
    }

    private void finishOptimization(JPanel button) {
        table.setLock(false);
        MultiPlot_.updatePlot();
        CardLayout cl = (CardLayout) button.getLayout();
        cl.first(button);

        // Fixes weird y-data selection changes
        MultiPlot_.subFrame.repaint();
        mainpanel.repaint();
        if (ipsExecutorService != null) ipsExecutorService.shutdownNow();
        ipsExecutorService = null;
        IJ.showStatus("");
        IJ.showProgress(1);
    }

    /**
     * @return the final state array
     */
    private OutPair divideTasksAndRun(final MinimumState initState,
                                      BiFunction<BigInteger, BigInteger, Optimizer> optimizerBiFunction) {
        return divideTasksAndRun(initState, optimizerBiFunction, true);
    }

    /**
     * @return the final state array
     */
    private OutPair divideTasksAndRun(final MinimumState initState,
                                      BiFunction<BigInteger, BigInteger, Optimizer> optimizerBiFunction, boolean multithreaded) {
        table.setLock(true);
        // Update table data - here we use full data, while on first open of a table MP will use truncated data
        MultiPlot_.updateTotals();
        //MultiPlot_.updateGUI();
        MultiPlot_.waitForPlotUpdateToFinish();

        if (showOptLog) AIJLogger.log(String.format("Using at most %d threads", maxThreads.get()));

        setupThreadedSpace();
        iterRemainingOld = BigInteger.ZERO;
        var minimumState = initState;
        var state = minimumState.state;
        var count = 0;

        if (multithreaded) {
            var CHUNK_SIZE = state.divide(BigInteger.valueOf(maxThreads.get()))
                    .max(BigInteger.valueOf(minChunkSize.get())).add(BigInteger.ONE);
            for (BigInteger start = BigInteger.ONE; start.compareTo(state) < 0; ) {
                var end = state.add(BigInteger.ONE).min(start.add(CHUNK_SIZE)).min(state);
                evaluateStatesInRange(optimizerBiFunction.apply(start, end));
                start = end;
                count++;
            }
        } else {
            evaluateStatesInRange(optimizerBiFunction.apply(BigInteger.ONE, state));
            count++;
        }

        if (showOptLog) AIJLogger.log(String.format("Using %d threads", count));

        Future<MinimumState> msf;
        var hasErrored = false;
        while (count > 0 && !hasErrored) {
            try {
                msf = completionService.take();
                var determinedState = msf.get();
                if (showOptLog) AIJLogger.log("New chunk minimum found:");
                if (showOptLog) AIJLogger.log(determinedState.comparator);
                if (showOptLog) AIJLogger.log(setArrayToState(determinedState.state));
                if (determinedState.lessThan(minimumState)) minimumState = determinedState;
                count--;
            } catch (InterruptedException | ExecutionException e) {
                if (e.getCause() instanceof CurveFitter.MPOperatorError) {
                    IJ.error("No operator is allowed");
                } else {
                    e.printStackTrace();
                }

                hasErrored = true;
            }
        }

        if (hasErrored || ipsExecutorService.isShutdown()) {
            if (showOptLog) AIJLogger.log("Optimization canceled.");
            minimumState = initState;
        } else {
            if (showOptLog) {
                AIJLogger.log("Found global minimum:");
                AIJLogger.log(minimumState.comparator);
                AIJLogger.log(setArrayToState(minimumState.state));

                if (minimumState.outState != null) {
                    AIJLogger.log(minimumState.outState);
                }
            }
        }

        IJ.showStatus("");
        IJ.showProgress(1);

        return new OutPair(setArrayToState(minimumState.state), minimumState.outState);
    }

    private void setFinalState(int[] state) {
        for (int i = 0; i < state.length; i++) {
            if (state[i] != 0) {
                fitDetrendComboBox[curve][i].setSelectedIndex(state[i]);
            }
            MultiPlot_.useFitDetrendCB[curve][i].setSelected(state[i] != 0);
        }
        if (showOptLog) AIJLogger.log("Found minimum BIC" + " state, the state has been set.");
        IJ.beep();
    }

    private void evaluateStatesInRange(Optimizer optimizer) {
        completionService.submit(optimizer);
    }

    private void setupThreadedSpace() {
        if (completionService != null) {
            completionService = null;
            if (pool != null) {
                pool.shutdownNow();
                pool = null;
            }
        }

        pool = new ThreadPoolExecutor(0, maxThreads.get(),
                10L, TimeUnit.SECONDS, new SynchronousQueue<>());
        completionService = new ExecutorCompletionService<>(pool);
    }

    public boolean[] setArrayToState(final BigInteger state) {
        var stateArray = new boolean[selectable.length];
        for (int i = 0; i < selectable.length; i++) {
            stateArray[this.selectable2PrimaryIndex[i]] = state.testBit(i);
        }
        return stateArray;
    }

    /**
     * Modifies {@link FitOptimization#selectable2PrimaryIndex}.
     *
     * @return an integer representing the current state of enabled options.
     */
    private BigInteger createBinaryRepresentation(boolean[] options) {
        return createBinaryRepresentation(options, true);
    }

    /**
     * Modifies {@link FitOptimization#selectable2PrimaryIndex}.
     *
     * @return an integer representing the current state of enabled options.
     */
    private BigInteger createBinaryRepresentation(boolean[] options, boolean updateIndex) {
        int enabledOptions = 0;
        for (int i = 0; i < options.length; i++) {
            if (options[i]) {
                if (updateIndex) selectable2PrimaryIndex[enabledOptions] = i;
                enabledOptions++;
            }
        }
        var x = BigInteger.ONE;
        return x.shiftLeft(enabledOptions).subtract(BigInteger.ONE);
    }

    @Override
    public void close() throws Exception {
        pool.shutdown();
        ipsExecutorService.shutdown();
        INSTANCES.remove(this);
        savePrefs();
    }

    public int getCurve() {
        return curve;
    }

    public int getTargetStar() {
        return targetStar;
    }

    private void scheduleIpsCounter(int minimizing) {
        if (ipsExecutorService != null) {
            ipsExecutorService.shutdownNow();
        }
        ipsExecutorService = Executors.newSingleThreadScheduledExecutor();
        rollingAvg = new RollingAvg();
        ipsExecutorService.scheduleAtFixedRate(() -> updateIpsCounter(minimizing), 1L, 1L, TimeUnit.SECONDS);
    }

    private synchronized void updateIpsCounter(int minimizing) {
        var counter = switch (minimizing) {
            case 0 -> compCounter;
            case 1 -> detrendCounter;
            default -> throw new IllegalStateException("Unexpected value: " + minimizing);
        };

        var iterRemaining = counter.getSum();
        var ips = iterRemainingOld.subtract(iterRemaining);
        iterRemainingOld = iterRemaining;

        var avgIps = rollingAvg.getAverage(ips);
        if (avgIps.compareTo(BigDecimal.ZERO) == 0) return;
        var totalSecs = counter.getTotalCount().divide(rollingAvg.getAverage(ips).toBigInteger());
        var hours = totalSecs.divide(BigInteger.valueOf(3600));
        var minutes = (totalSecs.mod(BigInteger.valueOf(3600))).divide(BigInteger.valueOf(60));
        var seconds = totalSecs.mod(BigInteger.valueOf(60));

        IJ.showStatus("!Minimization IPS: " + ips +
                "; Estimated time remaining: " + String.format("%02d:%02d:%02d", hours, minutes, seconds));
        IJ.showProgress(1 - new BigDecimal(iterRemaining).divide(new BigDecimal(counter.basis), 3, RoundingMode.HALF_UP).doubleValue());
    }

    private void addMouseListener(JSpinner spinner) {
        for (Component component : spinner.getComponents()) {
            if (component instanceof JSpinner.DefaultEditor editor) {
                editor.addMouseWheelListener(e -> {
                    if (spinner.getModel() instanceof SpinnerNumberModel spin) {
                        var delta = e.getPreciseWheelRotation() * spin.getStepSize().doubleValue();
                        var newValue = -delta + ((Number) spinner.getValue()).doubleValue();

                        if (newValue < ((Number) spin.getMinimum()).doubleValue()) {
                            newValue = ((Number) spin.getMinimum()).doubleValue();
                        } else if (spin.getMaximum() != null) {
                            if (newValue > ((Number) spin.getMaximum()).doubleValue()) {
                                newValue = ((Number) spin.getMaximum()).doubleValue();
                            }
                        }

                        spinner.setValue(newValue);
                    }
                });
            }
        }
    }

    enum CleanMode implements ToolTipProvider {
        /**
         * Compares residual to model RMS
         */
        RMS("Model vs RMS",
                """
                        <html>
                        Remove all data points that are outliers from the transit model by more than
                        N times the RMS of the transit model residuals.
                        </html>
                        """),
        /**
         * Compares residual to yerr
         */
        POINT("Model vs Phot. Err.",
                """
                        <html>
                        Remove all data points that are outliers from the transit model by more than
                        N times the per-point photometric error.
                        </html>
                        """),
        /**
         * Compares yerr to median of yerr
         */
        POINT_MEDIAN("Large Phot. Err.",
                """
                        <html>
                        Remove all data points that have photometric error greater than N times the
                        median photometric error (e.g. cleans clouded data points).
                        </html>
                        """),
        /**
         * User selected points
         */
        PRECISION("Precision", false, null);

        private final boolean menuDisplayable;
        private final String displayName;
        private final String tooltip;

        CleanMode(String displayName, boolean menuDisplayable, String tooltip) {
            this.displayName = displayName;
            this.menuDisplayable = menuDisplayable;
            this.tooltip = tooltip;
        }

        CleanMode(String displayName, String tooltip) {
            this.tooltip = tooltip;
            menuDisplayable = true;
            this.displayName = displayName;
        }

        public boolean isMenuDisplayable() {
            return menuDisplayable;
        }

        @Override
        public String toString() {
            return displayName;
        }

        @Override
        public String getToolTip() {
            return tooltip;
        }
    }

    record CleanTracker(CleanMode mode, MeasurementTable table, TreeSet<Integer> removedRows) {}

    public interface ToolTipProvider {
        String getToolTip();
    }

    private record OutPair(boolean[] stateArray, Object outState) {
    }

    /**
     * State tracker object for selected parameter optimization.
     * Contains the current working state and the comparator value.
     */
    public record MinimumState(BigInteger state, double comparator, Object outState) {
        public MinimumState(BigInteger state, double comparator) {
            this(state, comparator, null);
        }

        /**
         * Makes a state with most negative integer and of the largest comparator.
         */
        public MinimumState() {
            this(BigInteger.valueOf(Long.MIN_VALUE), Double.MAX_VALUE);
        }

        /**
         * @param minState the state to compare to.
         * @return if current state is less than the {@code minState} based on the comparators
         * and {@link FitOptimization#EPSILON}.
         */
        public boolean lessThan(MinimumState minState) {
            return comparator < minState.comparator - EPSILON;
        }

        public boolean lessThan(MinimumState minState, double epsilon) {
            return comparator < minState.comparator - epsilon;
        }

        /**
         * @param comparator2 the state to compare to.
         * @return if current state is less than the {@code comparator2} based on the comparators
         * and {@link FitOptimization#EPSILON}.
         */
        public boolean lessThan(double comparator2) {
            return comparator < comparator2 - EPSILON;
        }

        public boolean lessThan(double comparator2, double epsilon) {
            return comparator < comparator2 - epsilon;
        }

        @Override
        public String toString() {
            return "MinimumState{" +
                    "state=" + state +
                    ", comparator=" + comparator +
                    ", outState=" + AIJLogger.object2String(outState) +
                    '}';
        }
    }

    static class RollingAvg {
        BigDecimal currentAverage = BigDecimal.ZERO;
        BigDecimal count = BigDecimal.ZERO;

        public BigDecimal getAverage(BigInteger newDatum) {
            var protoAverage = new BigDecimal(newDatum);
            if (newDatum.compareTo(BigInteger.ZERO) <= 0) return protoAverage;
            protoAverage = protoAverage.add(currentAverage.multiply(count));
            count = count.add(BigDecimal.ONE);
            currentAverage = protoAverage.divide(count, 2, RoundingMode.HALF_UP);
            return currentAverage;
        }
    }

    public static class ToolTipWrapper implements ToolTipProvider {
        final Object value;
        final String toolTip;

        public ToolTipWrapper(Object value, String toolTip) {
            this.value = value;
            this.toolTip = toolTip;
        }

        public Object getValue() {//todo generic?
            return value;
        }

        @Override
        public String getToolTip() {
            return toolTip;
        }

        @Override
        public String toString() {
            return value.toString();
        }
    }

    public static class ToolTipRenderer extends DefaultListCellRenderer {

        @Override
        public Component getListCellRendererComponent(JList list, Object value,
                                                      int index, boolean isSelected, boolean cellHasFocus) {
            JComponent component = (JComponent) super.getListCellRendererComponent(list, value, index, isSelected,
                    cellHasFocus);
            String tip = null;
            if (value instanceof ToolTipProvider ttp) {
                tip = ttp.getToolTip();
            }
            list.setToolTipText(tip);
            return component;
        }
    }

    public static class DynamicCounter {
        JTextField textField;
        BigInteger basis, sum;
        Hashtable<Long, BigInteger> counters = new Hashtable<>(getThreadCount());
        boolean isSpinner= false;
        private static final String[] spinner = new String[]{"⠋","⠙","⠚","⠓"/*,"⠖","⠛"*/};
        private static Font oldFont;
        boolean awaitingUpdates = false;

        public DynamicCounter(JTextField field) {
            super();
            textField = field;
        }

        public void dynamicSet(BigInteger integer) {
            if (integer == null) return;
            setCounter(integer);
            if (awaitingUpdates) {
                return;
            }
            awaitingUpdates = true;
            SwingUtilities.invokeLater(() -> {
                textField.setText(isSpinner ? spinner[(int) (integer.longValue() % spinner.length)] :
                        getTotalCount().toString());
                awaitingUpdates = false;
            });
        }

        public synchronized BigInteger getSum() {
            return sum;
        }

        public BigInteger getBasis() {
            return basis;
        }

        public void setBasis(BigInteger integer) {
            basis = integer;
            sum = BigInteger.ZERO;
            counters.clear();
            SwingUtilities.invokeLater(() -> textField.setText(integer.toString()));
        }

        public void setSpinner(boolean spinner) {
            isSpinner = spinner;

            if (oldFont == null) {
                oldFont = textField.getFont();
            }

            if (isSpinner) {
                textField.setFont(FontUtil.getFont("Serif", Font.PLAIN, 12));
            } else {
                textField.setFont(oldFont);
            }
        }

        private synchronized BigInteger getTotalCount() {
            BigInteger total = basis;
            for (BigInteger value : counters.values()) {
                total = total.subtract(value);
            }

            sum = total;
            return total;
        }

        private void setCounter(BigInteger integer) {
            counters.put(Thread.currentThread().getId(), integer);
        }
    }
}
