package Astronomy.multiplot.optimization;

import Astronomy.CurveFitter;
import Astronomy.FitOptimization;
import ij.astro.logging.AIJLogger;
import ij.astro.logging.Translation;

import java.math.BigInteger;

@Translation(value = "Comparison Star Minimizer", trackThread = true)
public class CompStarFitting extends Optimizer {
    private final boolean doBruteForce;

    public CompStarFitting(BigInteger startState, BigInteger endState, FitOptimization fitOptimization) {
        super(startState, endState, fitOptimization);
        doBruteForce = true;
    }

    public CompStarFitting(BigInteger startState, FitOptimization fitOptimization) {
        super(startState, null, fitOptimization);
        doBruteForce = false;
    }

    @Override
    //todo option to save calculated states and their RMS in a sorted list
    public FitOptimization.MinimumState call() throws Exception {
        return doBruteForce ? bruteForceSolver() : quickOptiSolver();
    }

    private FitOptimization.MinimumState bruteForceSolver() {
        var minimumState = new FitOptimization.MinimumState();
        BigInteger counter = BigInteger.ZERO;
        for (BigInteger state = startState; state.compareTo(endState) <= 0; state = state.add(BigInteger.ONE)) {
            if (state.equals(BigInteger.ZERO)) continue;
            if (Thread.interrupted()) break;
            fitOptimization.compCounter.dynamicSet(counter);

            var x = fitOptimization.setArrayToState(state);
            var r = CurveFitter.getInstance(curve, fitOptimization.getTargetStar()).fitCurveAndGetResults(x);

            if (Double.isNaN(r.rms()) || r.rms() <= 0 || Double.isNaN(r.bic())) continue;

            //AIJLogger.log(state.toString(2));
            //AIJLogger.log(r);
            var newState = new FitOptimization.MinimumState(state, r.rms());
            if (newState.lessThan(minimumState)) minimumState = newState;
            counter = counter.add(BigInteger.ONE);
        }
        return minimumState;
    }

    //todo don't go through BigInt for state as it isn't needed here
    private FitOptimization.MinimumState quickOptiSolver() {
        var minimumState = new FitOptimization.MinimumState();
        BigInteger counter = BigInteger.ZERO;

        BigInteger state = startState;

        AIJLogger.log(state.toString(2));
        AIJLogger.log(getOnBits(state));

        // The startState is a compressed representation of all 1s
        var iterRemaining = getOnBits(state);
        var itersOfUnchangedState = 0;
        var improvedState = state;
        var rmsChanged = false;
        while (iterRemaining > 0 && itersOfUnchangedState <= 10) {
            if (Thread.interrupted()) break;
            rmsChanged = false;

            // Evaluate state for the current set of stars
            var stateArray = fitOptimization.setArrayToState(state);
            var results = CurveFitter.getInstance(curve, fitOptimization.getTargetStar()).fitCurveAndGetResults(stateArray);

            if (Double.isNaN(results.rms()) || results.rms() <= 0 || Double.isNaN(results.bic())) continue;

            minimumState = new FitOptimization.MinimumState(state, results.rms());

            for (int i = 0; i < state.bitLength() + 1; i++) {
                if (state.testBit(i)) {
                    state = state.flipBit(i);
                    stateArray = fitOptimization.setArrayToState(state);
                    results = CurveFitter.getInstance(curve, fitOptimization.getTargetStar()).fitCurveAndGetResults(stateArray);

                    if (Double.isNaN(results.rms()) || results.rms() <= 0 || Double.isNaN(results.bic())) continue;

                    var newState = new FitOptimization.MinimumState(state, results.rms());
                    if (newState.lessThan(minimumState)) {
                        improvedState = improvedState.flipBit(i);
                        rmsChanged = true;
                    }

                    state = state.flipBit(i);
                }
            }

            state = improvedState;

            var newRemaining = getOnBits(state);
            if (iterRemaining == newRemaining && !rmsChanged) {
                itersOfUnchangedState++;
                continue;
            }

            iterRemaining = newRemaining;
            itersOfUnchangedState = 0;
        }

        // Reevaluate results for final state to ensure RMS is up to date
        var stateArray = fitOptimization.setArrayToState(state);
        var results = CurveFitter.getInstance(curve, fitOptimization.getTargetStar()).fitCurveAndGetResults(stateArray);

        if (Double.isNaN(results.rms()) || results.rms() <= 0 || Double.isNaN(results.bic())) {
            return minimumState;
        }

        return new FitOptimization.MinimumState(state, results.rms());
    }

    //todo test
    private int getOnBits(BigInteger i) {
        // bitCount ignores the sign bit, but we make use of it for representing state
        return i.bitCount() + (i.testBit(0) ? 1 : 0);
    }
}
