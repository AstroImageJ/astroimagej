package util;

import java.util.Arrays;
import java.util.Comparator;
import java.util.stream.IntStream;

public class LinearInterpolator {
    private final double[] xs;
    private final double[] ys;
    private final Integer[] idx;

    public LinearInterpolator(double[] xs, double[] ys) {
        if (xs.length != ys.length) {
            throw new IllegalArgumentException("x and y must be same size");
        }

        // Sort arrays
        var idx = IntStream.range(0, xs.length).boxed().toArray(Integer[]::new);
        Arrays.parallelSort(idx, Comparator.comparingDouble(i -> xs[i]));

        this.xs = xs;
        this.ys = ys;
        this.idx = idx;
    }

    public double interpolate(double x) {
        if (x < xs[idx[0]]) {
            throw new IllegalArgumentException("%g is too small".formatted(x));
        } else if (x > xs[idx[xs.length-1]]) {
            throw new IllegalArgumentException("%g is too large".formatted(x));
        }

        if (x == xs[idx[0]]) {
            return ys[idx[0]];
        }

        for (int p = 1; p < xs.length; p++) {
            if (x == xs[idx[p]]) {
                return ys[idx[p]];
            }

            if (x < xs[idx[p]]) {
                return ys[idx[p-1]] + (x - xs[idx[p-1]]) * (ys[idx[p]] - ys[idx[p-1]]) / (xs[idx[p]] - xs[idx[p-1]]);
            }
        }

        return Double.NaN;
    }
}
