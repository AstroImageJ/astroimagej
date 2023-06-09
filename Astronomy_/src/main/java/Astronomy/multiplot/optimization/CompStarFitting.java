package Astronomy.multiplot.optimization;

import Astronomy.CurveFitter;
import Astronomy.FitOptimization;
import ij.astro.logging.Translation;

import java.math.BigInteger;

@Translation(value = "Comparison Star Minimizer", trackThread = true)
public class CompStarFitting extends Optimizer {
    private final Mode mode;

    public CompStarFitting(BigInteger startState, BigInteger endState, FitOptimization fitOptimization, Mode mode) {
        super(startState, endState, fitOptimization);
        this.mode = mode;
    }

    public CompStarFitting(BigInteger startState, FitOptimization fitOptimization, Mode mode) {
        super(startState, null, fitOptimization);
        this.mode = mode;
    }

    @Override
    //todo option to save calculated states and their RMS in a sorted list
    public FitOptimization.MinimumState call() throws Exception {
        return switch (mode) {
            case EXHAUSTIVE -> bruteForceSolver();
            case QUICK -> quickOptiSolver();
            case SIMPLE -> simpleOptiSolver();
        };
    }

    private FitOptimization.MinimumState bruteForceSolver() {
        var minimumState = new FitOptimization.MinimumState();
        BigInteger counter = BigInteger.ZERO;
        fitOptimization.compCounter.setSpinner(false);
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

    private FitOptimization.MinimumState quickOptiSolver() {
        var minimumState = new FitOptimization.MinimumState();
        BigInteger counter = BigInteger.ZERO;

        // The startState is a compressed representation of all 1s
        BigInteger state = startState;

        fitOptimization.compCounter.setSpinner(true);
        fitOptimization.compCounter.setBasis(fitOptimization.compCounter.getBasis().multiply(BigInteger.valueOf(2L)));

        var iterRemaining = getOnBits(state);
        var maxCompStars = iterRemaining;

        fitOptimization.compCounter.setBasis(fitOptimization.compCounter.getBasis().multiply(BigInteger.valueOf(iterRemaining * 2L)));

        var convergence = 0;
        while (convergence < 2) {
            if (Thread.interrupted()) break;

            var itersOfUnchangedState = 0;
            var improvedState = state;
            var rmsChanged = false;
            var convergenceTries = 0;
            while (iterRemaining > 0 && itersOfUnchangedState <= 1 && (convergenceTries < maxCompStars*maxCompStars || convergence < 1)) {
                if (Thread.interrupted()) break;
                rmsChanged = false;

                // Evaluate state for the current set of stars
                var stateArray = fitOptimization.setArrayToState(state);
                var results = CurveFitter.getInstance(curve, fitOptimization.getTargetStar()).fitCurveAndGetResults(stateArray);

                if (Double.isNaN(results.rms()) || results.rms() <= 0 || Double.isNaN(results.bic())) continue;

                minimumState = new FitOptimization.MinimumState(state, results.rms());

                fitOptimization.compCounter.dynamicSet(counter);

                for (int i = 0; i < (convergence > 0 ? startState.bitLength() : state.bitLength()) + 1; i++) {
                    if (convergence > 0 ? startState.testBit(i) : state.testBit(i)) {
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
                        counter = counter.add(BigInteger.ONE);
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
                convergenceTries++;
            }

            if (!rmsChanged && convergence > 0) {
                break;
            }

            convergence++;
        }

        // Reevaluate results for final state to ensure RMS is up to date
        var stateArray = fitOptimization.setArrayToState(state);
        var results = CurveFitter.getInstance(curve, fitOptimization.getTargetStar()).fitCurveAndGetResults(stateArray);

        if (Double.isNaN(results.rms()) || results.rms() <= 0 || Double.isNaN(results.bic())) {
            return minimumState;
        }

        return new FitOptimization.MinimumState(state, results.rms());
    }

    private FitOptimization.MinimumState simpleOptiSolver() {
        var minimumState = new FitOptimization.MinimumState();
        BigInteger counter = BigInteger.ZERO;

        // The startState is a compressed representation of all 1s
        BigInteger state = startState;

        fitOptimization.compCounter.setSpinner(false);

        var compStarCount = getOnBits(state);

        fitOptimization.compCounter.setBasis(BigInteger.valueOf(compStarCount*compStarCount));

        var oldState = state;
        for (int $ = 0; $ < compStarCount; $++) {
            if (Thread.interrupted()) break;

            // Evaluate state for the current set of stars
            var stateArray = fitOptimization.setArrayToState(state);
            var results = CurveFitter.getInstance(curve, fitOptimization.getTargetStar()).fitCurveAndGetResults(stateArray);

            if (Double.isNaN(results.rms()) || results.rms() <= 0 || Double.isNaN(results.bic())) continue;

            minimumState = new FitOptimization.MinimumState(state, results.rms());

            fitOptimization.compCounter.dynamicSet(counter);

            for (int i = 0; i < compStarCount; i++) {
                state = state.flipBit(i);
                stateArray = fitOptimization.setArrayToState(state);
                results = CurveFitter.getInstance(curve, fitOptimization.getTargetStar()).fitCurveAndGetResults(stateArray);

                if (Double.isNaN(results.rms()) || results.rms() <= 0 || Double.isNaN(results.bic())) continue;

                var newState = new FitOptimization.MinimumState(state, results.rms());
                if (!newState.lessThan(minimumState)) {
                    state = state.flipBit(i);
                } else {
                    minimumState = newState;
                }

                counter = counter.add(BigInteger.ONE);
            }

            if (state.equals(oldState)) {
                break;
            }

            oldState = state;
        }

        // Reevaluate results for final state to ensure RMS is up to date
        var stateArray = fitOptimization.setArrayToState(state);
        var results = CurveFitter.getInstance(curve, fitOptimization.getTargetStar()).fitCurveAndGetResults(stateArray);

        if (Double.isNaN(results.rms()) || results.rms() <= 0 || Double.isNaN(results.bic())) {
            return minimumState;
        }

        return new FitOptimization.MinimumState(state, results.rms());
    }

    private int getOnBits(BigInteger i) {
        // bitCount ignores the sign bit, but we make use of it for representing state
        return i.bitCount() /*+ (i.testBit(0) ? 1 : 0)*/;
    }

    public enum Mode {
        EXHAUSTIVE,
        QUICK,
        SIMPLE
    }
}
