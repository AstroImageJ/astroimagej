package Astronomy;

import ij.IJ;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.IntConsumer;

import static Astronomy.Periodogram_.huberLoss;

public class TLS {
    /**
     * Perform a parallel TLS search using the Mandel & Agol model.
     *
     * @param time             Time array
     * @param flux             Flux array
     * @param minPeriod        Minimum period (days)
     * @param maxPeriod        Maximum period (days)
     * @param nPeriods         Number of period steps
     * @param minDuration      Minimum duration (days)
     * @param maxDuration      Maximum duration (days)
     * @param nDurations       Number of duration steps
     * @param u1               Linear limb darkening coefficient (default 0.3)
     * @param u2               Quadratic limb darkening coefficient (default 0.3)
     * @param progressCallback Callback to report progress (period index)
     * @return Result object with period grid and SDE
     */
    public static Result search(double[] time, double[] flux, double minPeriod, double maxPeriod, int nPeriods, double minDuration, double maxDuration, int nDurations, double u1, double u2, IntConsumer progressCallback) {
        double[] periods = new double[nPeriods];
        double[] sde = new double[nPeriods];
        double[] bestDurations = new double[nPeriods];
        double[] bestDepths = new double[nPeriods];
        // Limb darkening and geometric parameters (box-shaped model)
        // u1 and u2 now come from parameters
        double aRs = 15.0; // Scaled semi-major axis
        double inc = 90.0; // Central transit (impact parameter = 0)
        double rprsGuess = 0.1; // Initial guess for planet/star radius ratio
        int nThreads = Math.max(1, Runtime.getRuntime().availableProcessors() - 2);
        ExecutorService executor = Executors.newFixedThreadPool(nThreads);
        AtomicInteger progress = new AtomicInteger(0);
        try {
            List<Future<Void>> futures = new ArrayList<>();
            // --- Set up uniform frequency grid (like BLS) ---
            double minFreq = 1.0 / maxPeriod;
            double maxFreq = 1.0 / minPeriod;
            for (int pi = 0; pi < nPeriods; pi++) {
                final int periodIdx = pi;
                futures.add(executor.submit(() -> {
                    double freq = minFreq + periodIdx * (maxFreq - minFreq) / Math.max(1, nPeriods - 1);
                    double period = 1.0 / freq;
                    periods[periodIdx] = period;
                    double bestSNR = 0;
                    double bestDuration = 0;
                    double bestDepth = 0;
                    for (int di = 0; di < nDurations; di++) {
                        double fracDuration = minDuration + di * (maxDuration - minDuration) / Math.max(1, nDurations - 1);
                        double duration = fracDuration * period;
                        int nPhase = 200;
                        for (int ph = 0; ph < nPhase; ph++) {
                            double t0 = time[0] + ph * period / nPhase;
                            // --- Analytic best-fit depth (like BLS) ---
                            double[] modelUnitDepth = MandelAgolTransitModel.compute(time, period, t0, duration, 1.0, 1.0, aRs, inc, u1, u2);
                            double bestDepthThis = fitTransitDepth(flux, modelUnitDepth);
                            double[] bestModel = new double[flux.length];
                            for (int i = 0; i < flux.length; i++) bestModel[i] = modelUnitDepth[i] * bestDepthThis;
                            // Compute SNR (signal to noise ratio) for this trial
                            double signal = 0, noise = 0;
                            int nIn = 0;
                            for (int i = 0; i < flux.length; i++) {
                                double phase = ((time[i] - t0 + 0.5 * period) % period) - 0.5 * period;
                                if (Math.abs(phase) < 0.5 * duration) {
                                    signal += flux[i] - 1.0;
                                    nIn++;
                                }
                            }
                            // Compute chi2 for noise estimate
                            double chi2 = 0;
                            for (int i = 0; i < flux.length; i++) {
                                double res = flux[i] - bestModel[i];
                                chi2 += res * res;
                            }
                            noise = Math.sqrt(chi2 / flux.length);
                            double snr = (nIn > 0 && noise > 0) ? Math.abs(signal) / (noise * Math.sqrt(nIn)) : 0;
                            if (snr > bestSNR) {
                                bestSNR = snr;
                                bestDuration = duration;
                                bestDepth = bestDepthThis;
                            }
                        }
                    }
                    sde[periodIdx] = bestSNR;
                    bestDurations[periodIdx] = bestDuration;
                    bestDepths[periodIdx] = bestDepth;
                    int done = progress.incrementAndGet();
                    IJ.showProgress(done, nPeriods);
                    if (progressCallback != null) progressCallback.accept(done - 1);
                    return null;
                }));
            }
            for (Future<Void> f : futures) f.get();
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            executor.shutdown();
        }
        IJ.showProgress(1.0);
        // Find the index of the highest peak (needed for SDE normalization)
        int bestPeriodIdx = 0;
        for (int i = 1; i < nPeriods; i++) {
            if (sde[i] > sde[bestPeriodIdx]) bestPeriodIdx = i;
        }
        // Compute SDE (Signal Detection Efficiency) using BLS-style normalization
        // Exclude a window around the peak (±1% of period range)
        int sdeWindow = Math.max(1, nPeriods / 100);
        List<Double> sdeForStats = new ArrayList<>();
        for (int i = 0; i < nPeriods; i++) {
            if (Math.abs(i - bestPeriodIdx) > sdeWindow) {
                sdeForStats.add(sde[i]);
            }
        }
        // Compute median and stddev
        double[] sdeStatsArr = sdeForStats.stream().mapToDouble(Double::doubleValue).toArray();
        double median = 0, std = 0;
        if (sdeStatsArr.length > 0) {
            java.util.Arrays.sort(sdeStatsArr);
            median = sdeStatsArr[sdeStatsArr.length / 2];
            double sumsq = 0;
            for (double v : sdeStatsArr) sumsq += (v - median) * (v - median);
            std = Math.sqrt(sumsq / sdeStatsArr.length);
        }
        for (int i = 0; i < sde.length; i++) {
            sde[i] = (std > 0) ? (sde[i] - median) / std : 0;
        }
        // After main grid search, do BLS-style T0 refinement for best period
        double bestPeriod = periods[bestPeriodIdx];
        double bestDuration = minDuration + (nDurations > 1 ? (nDurations - 1) / 2.0 * (maxDuration - minDuration) / (nDurations - 1) : 0.0); // crude guess, could be improved

        // --- Min average phase bin (compute first, for phase reference like BLS) ---
        int nBins = 200;
        double[] phases = new double[flux.length];
        for (int i = 0; i < flux.length; i++) {
            double phase = (time[i] - time[0]) / bestPeriod;
            phase = phase - Math.floor(phase);
            phases[i] = phase;
        }
        List<List<Double>> binFluxes = new ArrayList<>();
        for (int i = 0; i < nBins; i++) binFluxes.add(new ArrayList<>());
        for (int i = 0; i < flux.length; i++) {
            int bin = (int) Math.floor(phases[i] * nBins);
            if (bin < 0) bin = 0;
            if (bin >= nBins) bin = nBins - 1;
            binFluxes.get(bin).add(flux[i]);
        }
        double minAvg = Double.POSITIVE_INFINITY;
        int minBin = -1;
        int minPointsPerBin = 5;
        for (int i = 0; i < nBins; i++) {
            List<Double> fbin = binFluxes.get(i);
            if (fbin.size() < minPointsPerBin) continue;
            double sum = 0.0;
            for (double v : fbin) sum += v;
            double avg = sum / fbin.size();
            if (avg < minAvg) {
                minAvg = avg;
                minBin = i;
            }
        }
        double minPhase = (minBin + 0.5) / nBins;
        double t0MinAvg = time[0] + minPhase * bestPeriod;
        System.out.printf("[TLS DIAG] Min avg T0 = %.6f (will be center for sliding fit)\n", t0MinAvg);

        // --- Sliding trapezoid fit (Huber loss minimization, BLS-style, centered at t0MinAvg) ---
        double bestT0Sliding = t0MinAvg;
        double minLoss = Double.POSITIVE_INFINITY;
        double minTauFrac = 0.01;
        double maxTauFrac = 0.99;
        double tauFracStep = 0.01;
        int nTau = (int) Math.round((maxTauFrac - minTauFrac) / tauFracStep) + 1;
        double phaseStep = (bestDuration / bestPeriod) / 200.0;
        int nPhaseSteps = (int) Math.ceil(1.0 / phaseStep);
        double delta = 0.002; // Huber loss parameter
        int halfPhaseSteps = nPhaseSteps / 2;
        System.out.printf("[TLS DIAG] Sliding fit scan: %d steps, phase range = %.6f to %.6f around t0MinAvg\n",
                nPhaseSteps, t0MinAvg - halfPhaseSteps * bestPeriod / nPhaseSteps, t0MinAvg + halfPhaseSteps * bestPeriod / nPhaseSteps);

        // Pre-calculate phases using time[0] as reference (like BLS)
        double[] phasesForSliding = new double[flux.length];
        double phaseRef = time[0];
        for (int i = 0; i < flux.length; i++) {
            phasesForSliding[i] = (time[i] - phaseRef) / bestPeriod;
            phasesForSliding[i] = phasesForSliding[i] - Math.floor(phasesForSliding[i]);
        }
        // Use bestDepth from the periodogram grid search (approximate as the depth from the SDE grid search)
        double bestDepthForSliding = 0.0;
        {
            // Recompute bestDepth using the best period and best duration
            double bestChi2 = Double.POSITIVE_INFINITY;
            for (double depthTest : new double[]{0.001, 0.002, 0.005, 0.01, 0.02, 0.05, 0.1}) {
                double[] model = MandelAgolTransitModel.compute(time, bestPeriod, t0MinAvg, bestDuration, depthTest, Math.sqrt(depthTest), aRs, inc, u1, u2);
                double chi2 = 0;
                for (int i = 0; i < flux.length; i++) {
                    double res = flux[i] - model[i];
                    chi2 += res * res;
                }
                if (chi2 < bestChi2) {
                    bestChi2 = chi2;
                    bestDepthForSliding = depthTest;
                }
            }
        }
        for (int tauIdx = 0; tauIdx < nTau; tauIdx++) {
            double tauFrac = minTauFrac + tauIdx * tauFracStep;
            double tauPhase = tauFrac * (bestDuration / bestPeriod);
            double flatPhase = (bestDuration / bestPeriod) - 2 * tauPhase;
            if (flatPhase < 0) continue; // skip unphysical
            for (int s = 0; s < nPhaseSteps; s++) {
                double phaseOffset = s * phaseStep * bestPeriod; // Match BLS calculation
                double[] model = new double[flux.length];
                double[] residuals = new double[flux.length];
                int nUsed = 0;
                for (int j = 0; j < flux.length; j++) {
                    double phase = phasesForSliding[j] - (phaseOffset / bestPeriod); // Match BLS calculation
                    phase = phase - Math.floor(phase);
                    // Only use points within ±1.5*duration (in phase units) of the model center (like BLS)
                    double dphase = Math.abs(phase);
                    if (dphase > 0.5) dphase = 1.0 - dphase; // wrap around
                    double window = 1.5 * (bestDuration / bestPeriod);
                    if (dphase <= window) {
                        if (phase < tauPhase) {
                            model[j] = -bestDepthForSliding * (phase / tauPhase);
                        } else if (phase < tauPhase + flatPhase) {
                            model[j] = -bestDepthForSliding;
                        } else if (phase < 2 * tauPhase + flatPhase) {
                            model[j] = -bestDepthForSliding * (1 - (phase - tauPhase - flatPhase) / tauPhase);
                        } else {
                            model[j] = 0.0;
                        }
                        residuals[nUsed] = flux[j] - model[j];
                        nUsed++;
                    }
                }
                if (nUsed > 0) {
                    double[] usedResiduals = new double[nUsed];
                    System.arraycopy(residuals, 0, usedResiduals, 0, nUsed);
                    double loss = huberLoss(usedResiduals, delta);
                    if (loss < minLoss) {
                        minLoss = loss;
                        bestT0Sliding = time[0] + phaseOffset;
                    }
                }
            }
        }
        System.out.printf("[TLS DIAG] Sliding fit (BLS-style): bestT0Sliding = %.6f, minLoss = %.6g\n", bestT0Sliding, minLoss);

        return new Result(periods, sde, bestDurations, bestDepths, bestT0Sliding, t0MinAvg);
    }

    // Add static helper methods for T0 refinement
    private static double fitTransitDepth(double[] flux, double[] model) {
        double sumResiduals = 0.0;
        double sumModelSquared = 0.0;
        int nValid = 0;
        for (int i = 0; i < flux.length; i++) {
            if (!Double.isNaN(model[i])) {
                sumResiduals += flux[i] * model[i];
                sumModelSquared += model[i] * model[i];
                nValid++;
            }
        }
        if (nValid > 0 && sumModelSquared > 0) {
            return sumResiduals / sumModelSquared;
        }
        return 0.0;
    }

    private static double computeSDE(double[] flux, double[] model, double depth) {
        double[] residuals = new double[flux.length];
        double sumSquaredResiduals = 0.0;
        int nValid = 0;
        for (int i = 0; i < flux.length; i++) {
            if (!Double.isNaN(model[i])) {
                residuals[i] = flux[i] - depth * model[i];
                sumSquaredResiduals += residuals[i] * residuals[i];
                nValid++;
            }
        }
        if (nValid > 0) {
            double noise = Math.sqrt(sumSquaredResiduals / nValid);
            return Math.abs(depth) / noise;
        }
        return Double.NEGATIVE_INFINITY;
    }

    public static class Result {
        public double[] periods;
        public double[] sde;
        public double[] bestDurations;
        public double[] bestDepths;
        public double t0_sliding;
        public double t0_minavg;

        public Result(double[] periods, double[] sde, double[] bestDurations, double[] bestDepths, double t0_sliding, double t0_minavg) {
            this.periods = periods;
            this.sde = sde;
            this.bestDurations = bestDurations;
            this.bestDepths = bestDepths;
            this.t0_sliding = t0_sliding;
            this.t0_minavg = t0_minavg;
        }
    }
} 