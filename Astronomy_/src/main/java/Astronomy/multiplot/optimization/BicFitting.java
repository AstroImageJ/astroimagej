package Astronomy.multiplot.optimization;

import Astronomy.CurveFitter;
import Astronomy.FitOptimization;
import Astronomy.MultiPlot_;
import ij.IJ;

import java.math.BigInteger;
import java.util.Arrays;

public class BicFitting extends Optimizer {
    private final int[] initialDetrendIndex;

    public BicFitting(BigInteger startState, BigInteger endState, FitOptimization fitOptimization) {
        super(startState, endState, fitOptimization);
        initialDetrendIndex = Arrays.copyOf(MultiPlot_.detrendIndex[curve], MultiPlot_.detrendIndex[curve].length);
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
        var minimumState = new FitOptimization.MinimumState(endState, b.bic(), state0);
        final var refBic = b.bic();
        final var epsilon = FitOptimization.EPSILON;
        BigInteger counter = BigInteger.ZERO;
        for (BigInteger state = startState; state.compareTo(endState) < 0; state = state.add(BigInteger.ONE)) {
            if (state.equals(BigInteger.ZERO)) continue;
            if (Thread.interrupted()) break;
            fitOptimization.detrendCounter.dynamicSet(counter);
            counter = counter.add(BigInteger.ONE);

            var x = newState(state);
            var paramCount = Arrays.stream(x).filter(i -> i != 0).count();

            // Ensure param count is <= max params
            if (paramCount > (int) fitOptimization.detrendParamCount.getValue()) continue;

            var r = CurveFitter.getInstance(curve, fitOptimization.getTargetStar()).fitCurveAndGetResults(x);

            if (Double.isNaN(r.rms()) || Double.isNaN(r.bic())) continue;

            //AIJLogger.log(state.toString(2));
            /*AIJLogger.log(state);
            AIJLogger.log(x);
            AIJLogger.log(r);*/
            var newState = new FitOptimization.MinimumState(state, r.bic(), x);
            //AIJLogger.log(newState.lessThan(refBic, epsilon * paramCount));
            if (newState.lessThan(refBic, epsilon * paramCount) && newState.lessThan(minimumState, 0)) {
                minimumState = newState;
            }
        }
        /*AIJLogger.log("--------");
        AIJLogger.log(minimumState);*/
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
