package Astronomy;

import Astronomy.multiplot.optimization.CompStarFitting;
import Astronomy.multiplot.optimization.Optimizer;
import astroj.SpringUtil;
import ij.IJ;
import ij.astro.logging.AIJLogger;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.math.BigInteger;
import java.util.Comparator;
import java.util.Objects;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiFunction;

//todo organize properly
public class FitOptimization implements AutoCloseable {
    private static final int MAX_THREADS = 32;
    private final int curve;
    private int targetStar;
    /**
     * The change in the comparator to determine improvement
     */
    private static int EPSILON;
    /**
     * The index of this array is the selected option,
     * the value of the array is the option index in the relevant {@link MultiPlot_} array.
     * For reference stars, it is "initially selected reference star" -> "reference star index."
     */
    private int[] selectable2PrimaryIndex;
    private final ExecutorService pool;
    private boolean[] selectable;
    CompletionService<MinimumState> completionService;
    public DynamicCounter compCounter;

    //todo atomic strings for iter count fields
    //todo get min state out, volatile/synchronized int, or atomic? need to track comparison value as well. Could also make a map and minimize similar to autoswitch
    //https://www.baeldung.com/java-thread-safety

    // Init. after numAps is set
    public FitOptimization(int curve, int epsilon) {
        this.curve = curve;
        EPSILON = epsilon;
        this.pool = new ThreadPoolExecutor(0, MAX_THREADS,
                10L, TimeUnit.SECONDS,
                new SynchronousQueue<>());
        completionService = new ExecutorCompletionService<>(pool);
    }

    public void setSelectable(boolean[] selectable) {
        this.selectable = selectable;
        this.selectable2PrimaryIndex = new int[selectable.length];
    }

    //todo buttons -> list of options, buttton for start/stop [[optimization: List], [iterations: count]]
    public Component makeFitOptimizationPanel() {//todo see if multirun issue is fixed by making this static and it creates a new instance of this
        JPanel fitOptimizationPanel = new JPanel(new SpringLayout());
        fitOptimizationPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(MultiPlot_.mainBorderColor, 1), "Fit Optimization", TitledBorder.LEFT, TitledBorder.TOP, MultiPlot_.b12, Color.darkGray));

        JPanel compStarPanel = new JPanel(new SpringLayout());
        compStarPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(MultiPlot_.subBorderColor, 1), "Comparison Star Selection", TitledBorder.CENTER, TitledBorder.TOP, MultiPlot_.p11, Color.darkGray));

        var compOptimizationSelection = new JComboBox<ToolTipWrapper>();
        compOptimizationSelection.setEditable(false);
        compOptimizationSelection.setRenderer(new ToolTipRenderer());
        var compBruteForce = new ToolTipWrapper("Exhaustive Optimize", "Exhaustive search of comparison star combinations for minimize RMS of the fit. Only comparison stars selected at the start of this run are searched.");
        var compTest = new ToolTipWrapper("Debug", "Debug a single run.");
        compOptimizationSelection.addItem(compBruteForce);
        compOptimizationSelection.addItem(compTest);
        var optimizeButton = new JToggleButton("Start");
        optimizeButton.setToolTipText("Begin the optimization. Click again to stop it.");
        optimizeButton.addActionListener(e -> {
            optimizeButton.setSelected(optimizeButton.isSelected());
            if (optimizeButton.isSelected()) {
                optimizeButton.setText("Cancel");
                if (Objects.equals(compOptimizationSelection.getSelectedItem(), compTest)) {
                    testCompMin();
                } else if (Objects.equals(compOptimizationSelection.getSelectedItem(), compBruteForce)) {
                    minimizeCompStars();
                }
            } else {
                optimizeButton.setText("Start");
                pool.shutdownNow();
            }
        });
        compStarPanel.add(compOptimizationSelection);
        compStarPanel.add(optimizeButton);

        var compOptiIterLabel = new JLabel("Iter. Remaining:");
        compOptiIterLabel.setToolTipText("Number of iterations remaining in comp. star optimization.");
        compOptiIterLabel.setHorizontalAlignment(SwingConstants.CENTER);
        compStarPanel.add(compOptiIterLabel);

        var compOptiIterCount = new JTextField("12345");
        compOptiIterCount.setEditable(false);//todo make field
        compOptiIterCount.setMaximumSize(new Dimension(50, 10));
        compOptiIterCount.setHorizontalAlignment(SwingConstants.RIGHT);
        compOptiIterCount.setToolTipText("Number of iterations remaining in comp. star optimization.");
        compCounter = new DynamicCounter(compOptiIterCount);
        compStarPanel.add(compOptiIterCount);

        SpringUtil.makeCompactGrid(compStarPanel, 2, compStarPanel.getComponentCount()/2, 0, 0, 0, 0);

        JPanel detrendOptPanel = new JPanel(new SpringLayout());
        detrendOptPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(MultiPlot_.subBorderColor, 1), "Detrend Parameter Selection", TitledBorder.CENTER, TitledBorder.TOP, MultiPlot_.p11, Color.darkGray));

        var detrendOptimizationSelection = new JComboBox<ToolTipWrapper>();
        detrendOptimizationSelection.setEditable(false);
        detrendOptimizationSelection.setRenderer(new ToolTipRenderer());
        var detrendBruteForce = new ToolTipWrapper("Exhaustive Optimize", "Exhaustive search of detrendarison star combinations for minimize RMS of the fit. Only detrendarison stars selected at the start of this run are searched.");
        var detrendTest = new ToolTipWrapper("Debug", "Debug a single run.");
        detrendOptimizationSelection.addItem(detrendBruteForce);
        detrendOptimizationSelection.addItem(detrendTest);
        var detOptimizeButton = new JToggleButton("Start");
        detOptimizeButton.setToolTipText("Begin the optimization. Click again to stop it.");
        detOptimizeButton.addActionListener(e -> {
            detOptimizeButton.setSelected(detOptimizeButton.isSelected());
            if (detOptimizeButton.isSelected()) {
                detOptimizeButton.setText("Cancel");
                if (Objects.equals(detrendOptimizationSelection.getSelectedItem(), detrendTest)) {
                    AIJLogger.log("Not yet implemented.");
                } else if (Objects.equals(detrendOptimizationSelection.getSelectedItem(), detrendBruteForce)) {
                    AIJLogger.log("Not yet implemented.");
                }
            } else {
                detOptimizeButton.setText("Start");
                //todo rebuild pool/service after cancel as no new tasks can be submitted
                //pool.shutdownNow();
            }
        });
        detrendOptPanel.add(detrendOptimizationSelection);
        detrendOptPanel.add(detOptimizeButton);

        var paramOptiIterLabel = new JLabel("Iter. Remaining:"); //todo make field
        paramOptiIterLabel.setToolTipText("Number of iterations remaining in detrend parameter optimization.");
        paramOptiIterLabel.setHorizontalAlignment(SwingConstants.CENTER);
        detrendOptPanel.add(paramOptiIterLabel);

        var paramOptiIterCount = new JTextField("12345");//todo make field
        paramOptiIterCount.setEditable(false);
        paramOptiIterCount.setMaximumSize(new Dimension(50, 10));
        paramOptiIterCount.setToolTipText("Number of iterations remaining in detrend parameter optimization.");
        paramOptiIterCount.setHorizontalAlignment(SwingConstants.RIGHT);
        detrendOptPanel.add(paramOptiIterCount);

        SpringUtil.makeCompactGrid(detrendOptPanel, 2, detrendOptPanel.getComponentCount()/2, 0, 0, 0, 0);

        fitOptimizationPanel.add(compStarPanel);
        fitOptimizationPanel.add(detrendOptPanel);
        SpringUtil.makeCompactGrid(fitOptimizationPanel, 1, fitOptimizationPanel.getComponentCount(), 2, 2, 2, 2);

        return fitOptimizationPanel;
    }

    private void testCompMin() {
        selectable = null;
        selectable2PrimaryIndex = null;
        PlotUpdater.invalidateInstance();
        if (MultiPlot_.refStarFrame == null) MultiPlot_.showRefStarJPanel();
        if (MultiPlot_.isRefStar != null) {
            setSelectable(MultiPlot_.isRefStar);
        } else {
            AIJLogger.log("Open ref. star panel");
            return;
        }

        targetStar = Integer.parseInt(MultiPlot_.ylabel[curve].split("rel_flux_T")[1]) - 1;

        BigInteger initState = createBinaryRepresentation(selectable);
        var x = PlotUpdater.getInstance(curve, targetStar).fitCurveAndGetResults(setArrayToState(initState));
        AIJLogger.log(x);
        MultiPlot_.updatePlot(curve);
    }

    private void minimizeCompStars() {
        selectable = null;
        selectable2PrimaryIndex = null;
        PlotUpdater.invalidateInstance();
        if (MultiPlot_.refStarFrame == null) MultiPlot_.showRefStarJPanel();
        if (MultiPlot_.isRefStar != null) {
            setSelectable(MultiPlot_.isRefStar);
        } else {
            AIJLogger.log("Open ref. star panel.");
            return;
        }

        //todo do for C stars
        //todo error if this fails
        targetStar = Integer.parseInt(MultiPlot_.ylabel[curve].split("rel_flux_T")[1]) - 1;

        BigInteger initState = createBinaryRepresentation(selectable); //numAps has number of apertures

        var finalState = divideTasksAndRun(new MinimumState(initState, Double.MAX_VALUE), BigInteger.ONE,
                (start, end) -> new CompStarFitting(start, end, this));

        setFinalState("RMS", finalState, MultiPlot_.refStarCB);
    }

    //todo isRefStar is only not null when the window is/has been open

    //todo require that the y-data column corresponding to the data set fitting panel contains
    // "rel_flux_Txx" (where the x's can be any valid aperture number) and a corresponding error
    // column must exist named "rel_flux_err_Txx", where xx should be the same. This means we will
    // not attempt to optimize a comp star, just target stars, and we can avoid all of the
    // mathematical operators, etc, for non- rel_flux type data. We also need to only support
    // "Unphased" and "Days Since Tc" phase-folding modes on MP-main. If any of these conditions are not met,
    // then when the optimization is started, throw an error message to the user specifying what conditions
    // are required for minimization.

    /**
     * @param initState
     * @param startingPoint
     * @param optimizerBiFunction
     * @return the final state array
     */
    private boolean[] divideTasksAndRun(final MinimumState initState, final BigInteger startingPoint, //todo does detrend need to start at 0? if not, can remove this param
                                   BiFunction<BigInteger, BigInteger, Optimizer> optimizerBiFunction) {
        var minimumState = initState;
        var state = minimumState.state;
        var count = 0;
        var CHUNK_SIZE = state.divide(BigInteger.valueOf(MAX_THREADS)).add(BigInteger.ONE);
        for (BigInteger start = startingPoint; start.compareTo(state) < 0;) {
            var end = state.add(BigInteger.ONE).min(start.add(CHUNK_SIZE));
            evaluateStatesInRange(optimizerBiFunction.apply(start, end));
            start = end;
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

        return setArrayToState(minimumState.state);
    }

    private static void setFinalState(String minimizationTarget, boolean[] state, JCheckBox[] selectables) {
        for (int r = 0; r < state.length; r++) {
            selectables[r].setSelected(state[r]);
        }
        AIJLogger.log("Found minimum " + minimizationTarget + " state, reference stars set.");
        IJ.beep();
    }

    //todo make workingState be a BigInteger or BitSet to handle more comp stars
    // BigInteger makes splitting the tasks and the generation of all possible combinations easier
    // BitSet is potentially easier to extract values from and generation of the state array

    //todo fix outofmemory error in plot, likely references to tbe image processor not being removed
    // see if this happens with normal MP operation, fix
    // see if this is fixed when pulling out fitting code (probably)

    private void evaluateStatesInRange(Optimizer optimizer) {
        completionService.submit(optimizer);
    }

    //todo this is still broken
    public String workingState2SelectableStateString(final BigInteger state) {
        var r = BigInteger.ZERO;
        for (int i = 0; i < selectable2PrimaryIndex.length; i++) {
            if (state.testBit(i)) r = r.setBit(selectable2PrimaryIndex[i]);
        }
        var x = new StringBuffer(r.toString(2)).reverse();
        return x.toString();
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
     * @return an integer representing the current state of enabled options.
     */
    private BigInteger createBinaryRepresentation(boolean[] options) {
        return createBinaryRepresentation(options, true);
    }

    /**
     * Modifies {@link FitOptimization#selectable2PrimaryIndex}.
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
    }

    public int getCurve() {
        return curve;
    }

    public int getTargetStar() {
        return targetStar;
    }

    /**
     * State tracker object for selected parameter optimization.
     * Contains the current working state and the comparator value.
     */
    //todo rename to StateTracker or similar?
    public record MinimumState(BigInteger state, double comparator) {
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

        /**
         * @param comparator2 the state to compare to.
         * @return if current state is less than the {@code comparator2} based on the comparators
         * and {@link FitOptimization#EPSILON}.
         */
        public boolean lessThan(double comparator2) {
            return comparator < comparator2 - EPSILON;
        }
    }

    static class MinimumStateComparator implements Comparator<MinimumState> {
        @Override
        public int compare(MinimumState o1, MinimumState o2) {
            if (o1.comparator == o2.comparator) return 0;
            if (o1.lessThan(o2)) return -1;
            return 1;
        }
    }

    public interface ToolTipProvider {
        String getToolTip();
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
            if (value instanceof ToolTipProvider) {
                ToolTipProvider ttp = (ToolTipProvider) value;
                tip = ttp.getToolTip();
            }
            list.setToolTipText(tip);
            return component;
        }
    }

    public class DynamicCounter extends AtomicReference<BigInteger> {
        JTextField textField;

        public DynamicCounter(JTextField field) {
            super();
            textField = field;
        }
        public void dynamicSet(BigInteger integer) {
            if (getAcquire() != null && getAcquire().compareTo(integer) == 0) {
                return;
            }
            lazySet(integer);
            textField.setText(integer.toString());
        }
    }
}
