package util;

import flanagan.analysis.Stat;
import flanagan.math.ArrayMaths;
import ij.astro.types.Pair;

import java.util.Arrays;
import java.util.stream.IntStream;

public class PlotDataBinning {

    //todo return binWidth
    public static Pair.GenericPair<Pair.GenericPair<double[], double[]>, Double> binData(double[] x, double[] y, double binWidth) {
        if (x.length != y.length) throw new IllegalArgumentException("Arrays must be of the same length");

        var t = new ArrayMaths(x);
        var xMin = t.minimum();
        var xMax = t.maximum();

        var minBinWidth = Stat.median(IntStream.range(1, x.length).mapToDouble(i -> x[i] - x[i-1]).toArray());
        if (minBinWidth > binWidth) binWidth = minBinWidth;

        var span = xMax - xMin;
        var nBins = (int) Math.ceil(span/binWidth);

        var binBounds = new double[nBins + 1];
        double finalBinWidth = binWidth;
        Arrays.setAll(binBounds, i -> (i * finalBinWidth) + xMin);

        var bins = new DataBin[nBins];
        Arrays.setAll(bins, i -> new DataBin(x.length, i));

        for (int i = 0; i < x.length; i++) {
            var p = 0;
            var accepted = false;
            while (!accepted) {
                accepted = bins[p].accept(binBounds, x[i], y[i]);
                p++;
                if (p == bins.length) break;
            }

            if (!accepted) throw new RuntimeException("data did not fit into a bin");
        }

        var binCompleted = Arrays.stream(bins).parallel().map(DataBin::binnedDatum).toList();
        var outX = new double[binCompleted.size()];
        var outY = new double[binCompleted.size()];

        for (int i = 0; i < binCompleted.size(); i++) {
            outX[i] = binCompleted.get(i).first();
            outY[i] = binCompleted.get(i).second();
        }

        return new Pair.GenericPair<>(new Pair.GenericPair<>(outX, outY), binWidth);
    }

    private record DataBin(double[] x, double[] y, Holder lastIndex, int binIndex) {
        public DataBin(int maximumSize, int binIndex) {
            this(new double[maximumSize], new double[maximumSize], new Holder(), binIndex);
        }

        public void addData(double xDatum, double yDatum) {
            x[lastIndex.val] = xDatum;
            y[lastIndex.val] = yDatum;
            lastIndex.val++;
        }

        public Pair.DoublePair binnedDatum() {
            return new Pair.DoublePair(Stat.mean(Arrays.copyOf(x, lastIndex.val)),
                    Stat.mean(Arrays.copyOf(y, lastIndex.val)));
        }

        public boolean accept(double[] binBounds, double x, double y) {
            if (x>=binBounds[binIndex] && x<binBounds[binIndex+1]) {
                addData(x, y);
                return true;
            }

            return false;
        }

        static class Holder {
            public int val = 0;
        }
    }
}
