package Astronomy.multiplot.optimization;

import Astronomy.CurveFitter;
import Astronomy.FitOptimization;
import Astronomy.MultiPlot_;

import java.math.BigInteger;
import java.util.Arrays;

public class BicFitting extends Optimizer {
    private final int[] initialDetrendIndex;
    private final int[] index2workingIndexMapping;

    public BicFitting(BigInteger startState, BigInteger endState, FitOptimization fitOptimization) {
        super(startState, endState, fitOptimization);
        initialDetrendIndex = Arrays.copyOf(MultiPlot_.detrendIndex[curve], MultiPlot_.detrendIndex[curve].length);
        index2workingIndexMapping = new int[(int) Arrays.stream(initialDetrendIndex).filter(j -> j != 0).count()];
        int p = 0;
        for (int i = 0; i < initialDetrendIndex.length; i++) {
            if (initialDetrendIndex[i] != 0) index2workingIndexMapping[p++] = i;
        }
    }

    @Override
    public FitOptimization.MinimumState call() throws Exception {
        return plotUpdater();
    }


    private FitOptimization.MinimumState plotUpdater() {
        var minimumState = new FitOptimization.MinimumState(endState, MultiPlot_.bic[curve]);
        BigInteger counter = BigInteger.ZERO;
        for (BigInteger state = startState; state.compareTo(endState) < 0; state = state.add(BigInteger.ONE)) {
            if (state.equals(BigInteger.ZERO)) continue;
            if (Thread.interrupted()) break;
            fitOptimization.detrendCounter.dynamicSet(counter);

            var x = newState(state);
            var r = CurveFitter.getInstance(curve, fitOptimization.getTargetStar()).fitCurveAndGetResults(x);

            if (Double.isNaN(r.rms()) || Double.isNaN(r.bic())) continue;

            //AIJLogger.log(state.toString(2));
            /*AIJLogger.log(state);
            AIJLogger.log(x);
            AIJLogger.log(r);*/
            var newState = new FitOptimization.MinimumState(state, r.bic(), x);
            if (newState.lessThan(minimumState)) minimumState = newState;
            counter = counter.add(BigInteger.ONE);
        }
        /*AIJLogger.log("--------");
        AIJLogger.log(minimumState);*/
        return minimumState;
    }

    private int[] newState(BigInteger state) {
        var out = Arrays.copyOf(initialDetrendIndex, initialDetrendIndex.length); //todo can this not be a copy?
        for (int i = 0; i < state.bitLength(); i++) {
            if (!state.testBit(i)) {
                out[index2workingIndexMapping[i]] = 0;
            }
        }
        return out;
    }
}
