package Astronomy.multiplot.optimization;

import Astronomy.FitOptimization;
import Astronomy.PlotUpdater;
import ij.astro.logging.Translation;

import java.math.BigInteger;

@Translation(value = "Comparison Star Minimizer", trackThread = true)
public class CompStarFitting extends Optimizer {

    public CompStarFitting(final BigInteger startState, final BigInteger endState, FitOptimization fitOptimization) {
        super(startState, endState, fitOptimization);
    }

    @Override
    //todo option to save calculated states and their RMS in a sorted list
    public FitOptimization.MinimumState call() throws Exception {
        return plotUpdater();
    }

    private FitOptimization.MinimumState plotUpdater() {
        var minimumState = new FitOptimization.MinimumState();
        BigInteger counter = BigInteger.ZERO;
        for (BigInteger state = startState; state.compareTo(endState) <= 0; state = state.add(BigInteger.ONE)) {
            if (state.equals(BigInteger.ZERO)) continue;
            if (Thread.interrupted()) break;
            fitOptimization.compCounter.dynamicSet(counter);

            var x = fitOptimization.setArrayToState(state);
            var r = PlotUpdater.getInstance(curve, fitOptimization.getTargetStar()).fitCurveAndGetResults(x);

            if (Double.isNaN(r.rms()) || Double.isNaN(r.bic())) continue;

            //AIJLogger.log(state.toString(2));
            //AIJLogger.log(r);
            var newState = new FitOptimization.MinimumState(state, r.rms());
            if (newState.lessThan(minimumState)) minimumState = newState;
            counter = counter.add(BigInteger.ONE);
        }
        return minimumState;
    }
}
