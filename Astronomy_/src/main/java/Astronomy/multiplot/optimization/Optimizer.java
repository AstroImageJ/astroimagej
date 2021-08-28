package Astronomy.multiplot.optimization;

import Astronomy.FitOptimization;

import java.math.BigInteger;
import java.util.concurrent.Callable;

//todo detrend version
public abstract class Optimizer implements Callable<FitOptimization.MinimumState> {
    protected final BigInteger startState;
    protected final BigInteger endState;
    protected final FitOptimization fitOptimization;
    protected final int curve;

    protected Optimizer(BigInteger startState, BigInteger endState, FitOptimization fitOptimization) {
        this.startState = startState;
        this.endState = endState;
        this.fitOptimization = fitOptimization;
        this.curve = fitOptimization.getCurve();
    }
}
