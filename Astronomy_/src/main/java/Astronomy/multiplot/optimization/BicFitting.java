package Astronomy.multiplot.optimization;

import Astronomy.CurveFitter;
import Astronomy.FitOptimization;
import Astronomy.MultiPlot_;
import ij.IJ;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.HashMap;

public class BicFitting extends Optimizer {
    private final int[] initialDetrendIndex;
    private final String[] detrendlabel;
    private final HashMap<Integer, FitOptimization.MinimumState> stateTracker = new HashMap<>();

    public BicFitting(BigInteger startState, BigInteger endState, FitOptimization fitOptimization) {
        super(startState, endState, fitOptimization);
        initialDetrendIndex = Arrays.copyOf(MultiPlot_.detrendIndex[curve], MultiPlot_.detrendIndex[curve].length);
        detrendlabel = new String[initialDetrendIndex.length];
        for (int i = 0; i < MultiPlot_.detrendIndex[curve].length; i++) {
            initialDetrendIndex[i] = MultiPlot_.fitDetrendComboBox[curve][i].getSelectedIndex();
            detrendlabel[i] = (String) MultiPlot_.fitDetrendComboBox[curve][i].getSelectedItem();
        }
    }

    @Override
    public FitOptimization.MinimumState call() throws Exception {
        // This is a hack to prevent MP's delayed update plot timer from interfering with the optimizer
        IJ.wait(1000);

        return plotUpdater();
    }

    private FitOptimization.MinimumState plotUpdater() {
        var state0 = newState(BigInteger.ZERO);
        Arrays.fill(state0, 0);
        var b = CurveFitter.getInstance(curve, fitOptimization.getTargetStar()).fitCurveAndGetResults(state0);

        CurveFitter.detrendIndex = initialDetrendIndex;
        CurveFitter.detrendlabel = detrendlabel;
        CurveFitter.getInstance(curve, fitOptimization.getTargetStar()).setupData();

        var minimumState = new FitOptimization.MinimumState(endState, b.bic(), state0);
        final var refBic = b.bic();
        final var epsilon = FitOptimization.EPSILON;
        final var maxParams = (int) fitOptimization.detrendParamCount.getValue();
        BigInteger counter = BigInteger.ZERO;
        for (BigInteger state = startState; state.compareTo(endState) < 0; state = state.add(BigInteger.ONE)) {
            if (state.equals(BigInteger.ZERO)) continue;
            if (Thread.interrupted()) break;
            fitOptimization.detrendCounter.dynamicSet(counter);
            counter = counter.add(BigInteger.ONE);

            var x = newState(state);
            var paramCount = Arrays.stream(x).filter(i -> i != 0).count();

            // Ensure param count is <= max params
            if (paramCount > maxParams) continue;

            var r = CurveFitter.getInstance(curve, fitOptimization.getTargetStar()).fitCurveAndGetResults(x);

            if (Double.isNaN(r.rms()) || Double.isNaN(r.bic())) continue;

            var newState = new FitOptimization.MinimumState(state, r.bic(), x);

            if (newState.lessThan(refBic, epsilon * paramCount)) { // Only store valid states
                stateTracker.computeIfPresent((int) paramCount, ($i, minState) ->
                        newState.lessThan(minState, 0) ? newState : minState);
                stateTracker.putIfAbsent((int) paramCount, newState);
            }
        }

        var m = 0;
        for (int n = 1; n <= maxParams; n++) {
            if (!stateTracker.containsKey(n)) continue;
            var state = stateTracker.get(n);
            if (state.lessThan(minimumState, (n-m) * epsilon)) {
                minimumState = state;
                m = n;
            }
        }

        return minimumState;
    }

    private int[] newState(BigInteger state) {
        var out = Arrays.copyOf(initialDetrendIndex, initialDetrendIndex.length);
        for (int i = 0; i < out.length; i++) {
            if (!state.testBit(i)) {
                out[i] = 0;
            }
        }
        return out;
    }
}
