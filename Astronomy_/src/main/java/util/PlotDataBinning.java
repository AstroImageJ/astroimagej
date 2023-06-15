package util;

import flanagan.math.ArrayMaths;
import ij.astro.types.Pair;

import java.util.Arrays;
import java.util.Comparator;
import java.util.stream.IntStream;

public class PlotDataBinning {

    public static Pair.GenericPair<DoubleArrayTriple, Double> binData(double[] x, double[] y, double binWidth) {
        return binDataErr(x, y, null, binWidth);
    }

    public static Pair.GenericPair<DoubleArrayTriple, Double> binDataErr(double[] x, double[] y, double[] err, double binWidth) {
        try {
            if (x.length != y.length) {
                System.err.println("Binning arrays do match");
                return null;
            }

            if (x.length == 0) {
                return null;
            }

            double[] finalX = x;
            var idx = IntStream.range(0, x.length).filter(i -> Double.isFinite(finalX[i])).toArray();
            x = takeIndices(x, idx);
            y = takeIndices(y, idx);


            var withErr = err != null;
            if (!withErr) {
                err = new double[x.length];
            } else {
                err = takeIndices(err, idx);
            }


            var t = new ArrayMaths(x);
            var xMin = t.minimum();
            var xMax = t.maximum();

            double[] finalX1 = x;
            var l = IntStream.range(1, x.length).parallel().mapToDouble(i -> finalX1[i] - finalX1[i-1]).toArray();
            if (l.length == 0) {
                return null;
            }
            var minBinWidth = new ArrayMaths(l).minimum();
            if (minBinWidth > binWidth) binWidth = minBinWidth;

            var span = xMax - xMin;
            var nBins = (int) Math.ceil(span/binWidth);

            if (nBins == 0) nBins = x.length;

            var binBounds = new double[nBins + 1];
            double finalBinWidth = binWidth;
            Arrays.setAll(binBounds, i -> (i * finalBinWidth) + xMin);

            var bins = new DataBin[nBins];
            Arrays.setAll(bins, DataBin::new);

            var lastUsedBin = 0;
            for (int i = 0; i < x.length; i++) {
                var accepted = false;

                for (int p = lastUsedBin; p < bins.length; p++) {
                    accepted = bins[p].accept(binBounds, x[i], y[i], err[i]);
                    if (accepted) {
                        lastUsedBin = p;
                        break;
                    }
                }

                for (int p = lastUsedBin; p >= 0; p--) {
                    accepted = bins[p].accept(binBounds, x[i], y[i], err[i]);
                    if (accepted) {
                        lastUsedBin = p;
                        break;
                    }
                }

                if (!accepted) throw new RuntimeException("data did not fit into a bin");
            }

            // Order the elements by time and filter out empty bins
            var binCompleted = Arrays.stream(bins).parallel().filter(DataBin::hasData)
                    .map(withErr ? DataBin::binnedDatumErr : DataBin::binnedDatum)
                    .sorted(Comparator.comparingDouble(c -> c.x)).toList();
            var outX = new double[binCompleted.size()];
            var outY = new double[binCompleted.size()];
            var outErr = new double[binCompleted.size()];

            for (int i = 0; i < binCompleted.size(); i++) {
                outX[i] = binCompleted.get(i).x();
                outY[i] = binCompleted.get(i).y();
                outErr[i] = binCompleted.get(i).err();
            }

            return new Pair.GenericPair<>(new DoubleArrayTriple(outX, outY, outErr), binWidth);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static double[] takeIndices(double[] in, int[] idx) {
        var o = new double[idx.length];
        var  p = 0;
        for (int i : idx) {
            o[p++] = in[i];
        }

        return o;
    }

    private static class DataBin {
        private final int binIndex;
        private double totX;
        private double totY;
        private double totInvErr2;
        private double totXErr;
        private double totYErr;
        private int count;

        public DataBin(int binIndex) {
            this.binIndex = binIndex;
        }

        public void addData(double xDatum, double yDatum, double errDatum) {
            totX += xDatum;
            totY += yDatum;
            if (errDatum != 0) {
                totInvErr2 += 1/(errDatum*errDatum);
                totXErr += xDatum / (errDatum*errDatum);
                totYErr += yDatum / (errDatum*errDatum);
            }
            count++;
        }

        public boolean hasData() {
            return count > 0;
        }

        public boolean accept(double[] binBounds, double x, double y) {
            return accept(binBounds, x, y, 0);
        }

        public boolean accept(double[] binBounds, double x, double y, double err) {
            // As the bins are populated in order, the upper bound can be <=
            if (x>=binBounds[binIndex] && x<=binBounds[binIndex+1]) {
                addData(x, y, err);
                return true;
            }

            return false;
        }

        public DoubleTriple binnedDatum() {
            return new DoubleTriple(totX/count, totY/count, 0);
        }

        public DoubleTriple binnedDatumErr() {
            return new DoubleTriple(totXErr / totInvErr2, totYErr / totInvErr2, 1/Math.sqrt(totInvErr2));
        }

    }

    private record DoubleTriple(double x, double y, double err) {}

    public record DoubleArrayTriple(double[] x, double[] y, double[] err) {}
}
