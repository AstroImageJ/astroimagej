package Astronomy;

import Astronomy.multiplot.optimization.BicFitting;
import Astronomy.multiplot.optimization.CompStarFitting;
import Astronomy.multiplot.optimization.Optimizer;
import astroj.MeasurementTable;
import astroj.SpringUtil;
import ij.IJ;
import ij.astro.logging.AIJLogger;
import ij.measure.ResultsTable;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.Arrays;
import java.util.Hashtable;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.*;
import java.util.function.BiFunction;
import java.util.regex.Pattern;

import static Astronomy.MultiPlot_.*;

public class FitOptimization implements AutoCloseable {
    private static final int MAX_THREADS = getThreadCount();
    private static final BigInteger MIN_CHUNK_SIZE = BigInteger.valueOf(1000L);
    private final static Pattern apGetter = Pattern.compile("rel_flux_[ct]([0-9]+)");
    /**
     * The change in the comparator to determine improvement
     */
    public static double EPSILON;
    private final int curve;
    public DynamicCounter compCounter;
    public DynamicCounter detrendCounter;
    public JSpinner detrendParamCount;
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
    private JToggleButton detOptimizeButton;
    private JToggleButton optimizeButton;
    private RollingAvg rollingAvg = new RollingAvg();
    private JSpinner detrendEpsilon;
    private int nSigmaOutlier = 5;
    private ResultsTable backupTable;

    // Init. after numAps is set
    public FitOptimization(int curve, int epsilon) {
        this.curve = curve;
        EPSILON = epsilon;
        setupThreadedSpace();
    }

    private static void setFinalState(String minimizationTarget, boolean[] state, JCheckBox[] selectables) {
        for (int r = 0; r < state.length; r++) {
            selectables[r].setSelected(state[r]);
        }
        AIJLogger.log("Found minimum " + minimizationTarget + " state, reference stars set.");
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
        var undoButton = new JButton("⟲");
        undoButton.addActionListener($ -> undoOutlierClean());
        undoButton.setFont(undoButton.getFont().deriveFont(15f));
        outlierRemoval.add(undoButton);
        var cleanButton = new JButton("Clean");
        cleanButton.addActionListener($ -> cleanOutliers());
        outlierRemoval.add(cleanButton);
        var cleanLabel = new JLabel("Nσ:");
        cleanLabel.setHorizontalAlignment(SwingConstants.CENTER);
        outlierRemoval.add(cleanLabel);
        var cleanSpin = new JSpinner(new SpinnerNumberModel(nSigmaOutlier, 1, null, 1));
        cleanSpin.addChangeListener($ -> nSigmaOutlier = (int) cleanSpin.getValue());
        outlierRemoval.add(cleanSpin);
        SpringUtil.makeCompactGrid(outlierRemoval, 2, outlierRemoval.getComponentCount() / 2, 0, 0, 2, 2);
        fitOptimizationPanel.add(outlierRemoval);

        JPanel compStarPanel = new JPanel(new SpringLayout());
        compStarPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(MultiPlot_.subBorderColor, 1), "Comparison Star Selection", TitledBorder.CENTER, TitledBorder.TOP, MultiPlot_.p11, Color.darkGray));

        var compOptimizationSelection = new JComboBox<ToolTipWrapper>();
        compOptimizationSelection.setEditable(false);
        compOptimizationSelection.setRenderer(new ToolTipRenderer());
        var compBruteForce = new ToolTipWrapper("Exhaustive Optimize", "Exhaustive search of comparison star combinations for minimize RMS of the fit. Only comparison stars selected at the start of this run are searched.");
        var compTest = new ToolTipWrapper("Debug", "Debug a single run.");
        compOptimizationSelection.addItem(compBruteForce);
        compOptimizationSelection.addItem(compTest);
        optimizeButton = new JToggleButton("Start");
        optimizeButton.setToolTipText("Begin the optimization. Click again to stop it.");
        optimizeButton.addActionListener(e -> {
            optimizeButton.setSelected(optimizeButton.isSelected());
            if (optimizeButton.isSelected()) {
                optimizeButton.setText("Cancel");
                if (Objects.equals(compOptimizationSelection.getSelectedItem(), compTest)) {
                    testCompMin();
                    MultiPlot_.updatePlot(curve);
                } else if (Objects.equals(compOptimizationSelection.getSelectedItem(), compBruteForce)) {
                    Executors.newSingleThreadExecutor().submit(this::minimizeCompStars);
                }
            } else {
                optimizeButton.setText("Start");
                pool.shutdownNow();
                ipsExecutorService.shutdown();
                IJ.showProgress(1);
                IJ.showStatus("");
            }
        });
        compStarPanel.add(compOptimizationSelection);
        compStarPanel.add(optimizeButton);

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

        var pLabel = new JLabel("   Max Detrend Pars.:   ");
        pLabel.setHorizontalAlignment(SwingConstants.CENTER);
        pLabel.setToolTipText("The maximum number of detrend parameters to be enabled.");
        detrendOptPanel.add(pLabel);

        detrendParamCount = new JSpinner(new SpinnerNumberModel(2, 0, 100, 1));
        detrendParamCount.setToolTipText("The maximum number of detrend parameters to be enabled.");
        detrendOptPanel.add(detrendParamCount);

        var detrendOptimizationSelection = new JComboBox<ToolTipWrapper>();
        detrendOptimizationSelection.setEditable(false);
        detrendOptimizationSelection.setRenderer(new ToolTipRenderer());
        var detrendBruteForce = new ToolTipWrapper("Exhaustive Optimize", "Exhaustive search of parameter combinations for minimum BIC of the fit. All set parameters are searched.");
        var detrendTest = new ToolTipWrapper("Debug", "Debug a single run.");
        detrendOptimizationSelection.addItem(detrendBruteForce);
        detrendOptimizationSelection.addItem(detrendTest);
        detOptimizeButton = new JToggleButton("Start");
        detOptimizeButton.setToolTipText("Begin the optimization. Click again to stop it.");
        detOptimizeButton.addActionListener(e -> {
            detOptimizeButton.setSelected(detOptimizeButton.isSelected());
            if (detOptimizeButton.isSelected()) {
                detOptimizeButton.setText("Cancel");
                if (Objects.equals(detrendOptimizationSelection.getSelectedItem(), detrendTest)) {
                    testParamMin();
                    MultiPlot_.updatePlot(curve);
                } else if (Objects.equals(detrendOptimizationSelection.getSelectedItem(), detrendBruteForce)) {
                    Executors.newSingleThreadExecutor().submit(this::minimizeParams);
                }
            } else {
                detOptimizeButton.setText("Start");
                pool.shutdownNow();
                ipsExecutorService.shutdown();
                IJ.showProgress(1);
                IJ.showStatus("");
            }
        });
        detrendOptPanel.add(detrendOptimizationSelection);
        detrendOptPanel.add(detOptimizeButton);

        var eLabel = new JLabel("Min. BIC Thres.:");
        eLabel.setHorizontalAlignment(SwingConstants.CENTER);
        eLabel.setToolTipText("The required change in BIC between selected states to be considered a better value.");
        detrendOptPanel.add(eLabel);

        detrendEpsilon = new JSpinner(new SpinnerNumberModel(2, 0D, 100, 1));
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

        fitOptimizationPanel.add(compStarPanel);
        fitOptimizationPanel.add(detrendOptPanel);
        SpringUtil.makeCompactGrid(fitOptimizationPanel, 1, fitOptimizationPanel.getComponentCount(), 2, 2, 2, 2);

        return fitOptimizationPanel;
    }

    private void cleanOutliers() {
        int errcolumn = ResultsTable.COLUMN_NOT_FOUND;
        if (ylabel[curve].startsWith("rel_flux_T") || ylabel[curve].startsWith("rel_flux_C")) {
            errcolumn = table.getColumnIndex("rel_flux_err_" + ylabel[curve].substring(9));
        } else if (ylabel[curve].startsWith("Source-Sky_")) {
            errcolumn = table.getColumnIndex("Source_Error_" + ylabel[curve].substring(11));
        } else if (ylabel[curve].startsWith("tot_C_cnts")) {
            errcolumn = table.getColumnIndex("tot_C_err" + ylabel[curve].substring(10));
        } else if (ylabel[curve].startsWith("Source_AMag_")) {
            errcolumn = table.getColumnIndex("Source_AMag_Err_" + ylabel[curve].substring(12));
        }
        if (errcolumn == ResultsTable.COLUMN_NOT_FOUND) return;

        var oldTable = (ResultsTable) table.clone();

        var hasActionToUndo = false;
        for (int i = 0; i < table.getColumn(ylabel[curve]).length; i++) {
            if (residual[curve][i] < nSigmaOutlier * table.getValueAsDouble(errcolumn, i)) {
                hasActionToUndo = true;
                table.deleteRow(i);
            }
        }

        if (hasActionToUndo) {
            backupTable = oldTable;
            MultiPlot_.updatePlot();
        }
    }

    private void undoOutlierClean() {
        if (backupTable != null) {
            table = (MeasurementTable) backupTable;
            backupTable = null;
            MultiPlot_.updatePlot();
        }
    }

    private void testCompMin() {
        selectable = null;
        selectable2PrimaryIndex = null;
        CurveFitter.invalidateInstance();
        if (MultiPlot_.refStarFrame == null) MultiPlot_.showRefStarJPanel();
        if (MultiPlot_.isRefStar != null) {
            setSelectable(MultiPlot_.isRefStar);
        } else {
            AIJLogger.log("Open ref. star panel");
            return;
        }

        setTargetStar();

        BigInteger initState = createBinaryRepresentation(selectable);
        var x = CurveFitter.getInstance(curve, targetStar).fitCurveAndGetResults(setArrayToState(initState));
        AIJLogger.log(x);
        finishOptimization(optimizeButton);
    }

    private void minimizeCompStars() {
        selectable = null;
        selectable2PrimaryIndex = null;
        CurveFitter.invalidateInstance();
        if (MultiPlot_.refStarFrame == null) MultiPlot_.showRefStarJPanel();
        if (MultiPlot_.isRefStar != null) {
            setSelectable(MultiPlot_.isRefStar);
        } else {
            AIJLogger.log("Open ref. star panel.");
            return;
        }

        setTargetStar();

        BigInteger initState = createBinaryRepresentation(selectable); //numAps has number of apertures

        compCounter.setBasis(initState.subtract(BigInteger.ONE)); // Subtract 1 as 0-state is skipped
        scheduleIpsCounter(0);

        var finalState = divideTasksAndRun(new MinimumState(initState, Double.MAX_VALUE),
                (start, end) -> new CompStarFitting(start, end, this));

        setFinalState("RMS", finalState.stateArray, MultiPlot_.refStarCB);
        finishOptimization(optimizeButton);
    }

    private void testParamMin() {
        selectable = null;
        selectable2PrimaryIndex = null;

        setSelectable((int) Arrays.stream(MultiPlot_.detrendIndex[curve]).filter(i -> i != 0).count());

        if (MultiPlot_.refStarFrame == null) MultiPlot_.showRefStarJPanel();
        CurveFitter.invalidateInstance();

        setTargetStar();

        var x = CurveFitter.getInstance(curve, targetStar).fitCurveAndGetResults(MultiPlot_.detrendIndex[curve]);
        AIJLogger.log(x);
        finishOptimization(optimizeButton);
        EPSILON = 0;
    }

    private void minimizeParams() {
        selectable = null;
        selectable2PrimaryIndex = null;

        setSelectable(MultiPlot_.detrendIndex[curve].length);
        if (selectable.length < 2) {
            IJ.error("More than one detrend parameter is needed for optimization");
            return;
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

        finishOptimization(detOptimizeButton);
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

    private void finishOptimization(JToggleButton button) {
        MultiPlot_.updatePlot(curve);
        button.setSelected(false);
        button.setText("Start");

        // Fixes weird y-data selection changes
        MultiPlot_.subFrame.repaint();
        MultiPlot_.mainsubpanel.repaint();
        if (ipsExecutorService != null) ipsExecutorService.shutdown();
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
        // Update table data - here we use full data, while on first open of a table MP will use truncated data
        MultiPlot_.updateTotals();
        MultiPlot_.updateGUI();

        setupThreadedSpace();
        iterRemainingOld = BigInteger.ZERO;
        var minimumState = initState;
        var state = minimumState.state;
        var count = 0;

        if (multithreaded) {
            var CHUNK_SIZE = state.divide(BigInteger.valueOf(MAX_THREADS)).max(MIN_CHUNK_SIZE).add(BigInteger.ONE);
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

        Future<MinimumState> msf;
        var hasErrored = false;
        while (count > 0 && !hasErrored) {
            try {
                msf = completionService.take();
                var determinedState = msf.get();
                AIJLogger.log("New chunk minimum found:");
                AIJLogger.log(determinedState.comparator);
                AIJLogger.log(setArrayToState(determinedState.state));
                if (determinedState.lessThan(minimumState)) minimumState = determinedState;
                count--;
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
                hasErrored = true;
            }
        }

        if (hasErrored) IJ.error("Error occurred during minimization, preceding with lowest RMS found.");

        AIJLogger.log("Found global minimum:");
        AIJLogger.log(minimumState.comparator);
        AIJLogger.log(setArrayToState(minimumState.state));
        if (minimumState.outState != null) AIJLogger.log(minimumState.outState);

        return new OutPair(setArrayToState(minimumState.state), minimumState.outState);
    }

    private void setFinalState(int[] state) {
        for (int i = 0; i < state.length; i++) {
            if (state[i] != 0) {
                fitDetrendComboBox[curve][i].setSelectedIndex(state[i]);
            }
            MultiPlot_.useFitDetrendCB[curve][i].setSelected(state[i] != 0);
        }
        AIJLogger.log("Found minimum BIC" + " state, the state has been set.");
        IJ.beep();
    }

    private void evaluateStatesInRange(Optimizer optimizer) {
        completionService.submit(optimizer);
    }

    private void setupThreadedSpace() {
        pool = new ThreadPoolExecutor(0, MAX_THREADS,
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
    }

    public int getCurve() {
        return curve;
    }

    public int getTargetStar() {
        return targetStar;
    }

    private void scheduleIpsCounter(int minimizing) {
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

    public class ToolTipWrapper implements ToolTipProvider {
        final Object value;
        final String toolTip;

        public ToolTipWrapper(Object value, String toolTip) {
            this.value = value;
            this.toolTip = toolTip;
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

    public class ToolTipRenderer extends DefaultListCellRenderer {

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

    public class DynamicCounter {
        JTextField textField;
        BigInteger basis, sum;
        Hashtable<Long, BigInteger> counters = new Hashtable<>(getThreadCount());

        public DynamicCounter(JTextField field) {
            super();
            textField = field;
        }

        public synchronized void dynamicSet(BigInteger integer) {
            if (integer == null) return;
            setCounter(integer);
            textField.setText(getTotalCount().toString());
        }

        public synchronized BigInteger getSum() {
            return sum;
        }

        public BigInteger getBasis() {
            return basis;
        }

        public synchronized void setBasis(BigInteger integer) {
            basis = integer;
            sum = BigInteger.ZERO;
            counters.clear();
            textField.setText(integer.toString());
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
