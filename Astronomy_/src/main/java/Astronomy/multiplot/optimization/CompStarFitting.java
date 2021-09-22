package Astronomy.multiplot.optimization;

import Astronomy.FitOptimization;
import Astronomy.MultiPlot_;
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

    private FitOptimization.MinimumState checkboxTestCase() {
        var minimumState = new FitOptimization.MinimumState();
        for (BigInteger state = startState; state.compareTo(endState) < 0; state = state.add(BigInteger.ONE)) {//todo countdown from state? allows for current state = iterations left
            if (state.equals(BigInteger.ZERO)) continue;
            fitOptimization.compCounter.dynamicSet(state);

            var x = fitOptimization.setArrayToState(state);
            for (int r = 0; r < x.length; r++) {
                MultiPlot_.refStarCB[r].setSelected(x[r]);
            }
            MultiPlot_.updatePlot(curve);
            MultiPlot_.waitForPlotUpdateToFinish();
            var newState = new FitOptimization.MinimumState(state, MultiPlot_.sigma[curve]);
            if (newState.lessThan(minimumState)) minimumState = newState;
            System.gc();//todo remove if possible
        }
        System.out.println(fitOptimization.workingState2SelectableStateString(minimumState.state()));
        return minimumState;
    }

    private FitOptimization.MinimumState plotUpdater() {
        var minimumState = new FitOptimization.MinimumState();
        long counter = 0;
        for (BigInteger state = startState; state.compareTo(endState) <= 0; state = state.add(BigInteger.ONE)) {//todo countdown from state? allows for current state = iterations left
            counter++;
            if (state.equals(BigInteger.ZERO)) continue;
            if (Thread.interrupted()) break;
            fitOptimization.compCounter.dynamicSet(state);

            var x = fitOptimization.setArrayToState(state);
            var r = PlotUpdater.getInstance(curve, fitOptimization.getTargetStar()).fitCurveAndGetResults(x);

            if (Double.isNaN(r.rms()) || Double.isNaN(r.bic())) continue;

            //AIJLogger.log(state.toString(2));
            //AIJLogger.log(r);
            var newState = new FitOptimization.MinimumState(state, r.rms());
            if (newState.lessThan(minimumState)) minimumState = newState;
        }
        return minimumState;
    }
}
