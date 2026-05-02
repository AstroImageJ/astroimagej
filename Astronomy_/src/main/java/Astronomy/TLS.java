package Astronomy;

import static Astronomy.Periodogram_.lowerBoundD;
import static Astronomy.Periodogram_.upperBoundD;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BooleanSupplier;
import java.util.function.IntConsumer;

import ij.IJ;

public class TLS {
    /**
     * Perform a parallel TLS BLIND-SEARCH using the simple limb-darkened
     * trapezoid template.  Instead of a single (u1, u2, ingress) template
     * the outer grid loop sweeps all combinations of {@code u1Grid} ×
     * {@code u2Grid} × {@code ingressGrid} inside every per-period task,
     * and the running-best SNR is kept for each period.  Any axis can be
     * collapsed to a single value (length-1 array) if the user has a known
     * stellar LD or just doesn't want to sweep that axis.
     *
     * @param time             Time array
     * @param flux             Flux array
     * @param minPeriod        Minimum period (days)
     * @param maxPeriod        Maximum period (days)
     * @param nPeriods         Number of period steps
     * @param minDuration      Minimum fractional duration (0–1 of period)
     * @param maxDuration      Maximum fractional duration
     * @param nDurations       Number of duration steps
     * @param nPhase           Number of phase grid steps = phase bin count
     * @param u1Grid           Quadratic LD u1 values (length ≥ 1)
     * @param u2Grid           Quadratic LD u2 values (length ≥ 1)
     * @param ingressGrid      Ingress-fraction values t12/t14 in (0, 0.5]
     * @param progressCallback Callback to report progress (period index, 0..nPeriods-1)
     * @return Result object with period grid, SDE, and per-period winning (u1, u2, ingress)
     */
    public static Result search(double[] time, double[] flux,
                                double minPeriod, double maxPeriod, int nPeriods,
                                double minDuration, double maxDuration, int nDurations,
                                int nPhase,
                                double[] u1Grid, double[] u2Grid, double[] ingressGrid,
                                IntConsumer progressCallback) {
        return search(time, flux, minPeriod, maxPeriod, nPeriods, minDuration, maxDuration,
                nDurations, nPhase, u1Grid, u2Grid, ingressGrid,
                progressCallback, () -> false);
    }

    /**
     * Cancellable variant of the {@code search} method above.
     *
     * <p>{@code cancelCheck} is polled at the start of every per-period task,
     * once per grid cell, and once per duration inside each cell — so a user
     * Stop is noticed within a handful of milliseconds even in the middle of
     * a dense blind sweep.  Unfinished per-period slots are filled with
     * {@link Double#NaN} so the downstream SDE reduction and T0 refinement
     * skip them (matching GPU Stop / BLS CPU Stop semantics).  The executor
     * is also {@code shutdownNow()}'d so queued tasks never start.
     */
    public static Result search(double[] time, double[] flux,
                                double minPeriod, double maxPeriod, int nPeriods,
                                double minDuration, double maxDuration, int nDurations,
                                int nPhase,
                                double[] u1Grid, double[] u2Grid, double[] ingressGrid,
                                IntConsumer progressCallback, BooleanSupplier cancelCheck) {
        // Phase-binned TLS: the phase grid is also the binning grid (nBins == nPhase),
        // giving O(nPoints + nDurations * nPhase * halfWin) per period — a ~100× speedup
        // over the naive O(nDurations * nPhase * nPoints).
        final int nBins = Math.max(2, nPhase);

        // Sanity — every axis needs at least one value so the outer grid loops run.
        if (u1Grid      == null || u1Grid.length      == 0) u1Grid      = new double[]{0.3};
        if (u2Grid      == null || u2Grid.length      == 0) u2Grid      = new double[]{0.3};
        if (ingressGrid == null || ingressGrid.length == 0) ingressGrid = new double[]{0.25};
        final double[] u1GridF      = u1Grid;
        final double[] u2GridF      = u2Grid;
        final double[] ingressGridF = ingressGrid;
        IJ.log("[TLS CPU] Blind grid " + u1Grid.length + "x" + u2Grid.length
                + "x" + ingressGrid.length + " = "
                + (u1Grid.length * u2Grid.length * ingressGrid.length) + " cells");

        double[] periods        = new double[nPeriods];
        double[] sde            = new double[nPeriods];
        double[] bestDurations  = new double[nPeriods];
        double[] bestDepths     = new double[nPeriods];
        double[] bestU1Arr      = new double[nPeriods];
        double[] bestU2Arr      = new double[nPeriods];
        double[] bestIngressArr = new double[nPeriods];

        // sumF2 = sum_j f[j]^2 is independent of (period, duration, phase) — compute once.
        double sumF2Global = 0.0;
        for (double v : flux) sumF2Global += v * v;
        final double sumF2 = sumF2Global;
        final double tRef = time[0];
        final int nData = time.length;

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
                    double invPeriod = 1.0 / period;
                    periods[periodIdx] = period;

                    // Early-out if the user pressed Stop before this task started
                    // running.  Queued tasks picked up after a shutdownNow() call
                    // on the executor are already skipped by the framework, but
                    // checking here covers the gap between cancel-set and
                    // shutdownNow() and also handles tasks that started just
                    // before shutdownNow fired.
                    if (cancelCheck.getAsBoolean()) {
                        sde[periodIdx]           = Double.NaN;
                        bestDurations[periodIdx] = Double.NaN;
                        bestDepths[periodIdx]    = Double.NaN;
                        return null;
                    }

                    // Stage 1 — phase-fold all points into nBins uniform bins (O(nPoints)).
                    double[] binFluxSum = new double[nBins];
                    int[]    binN       = new int[nBins];
                    for (int j = 0; j < nData; j++) {
                        double ph = (time[j] - tRef) * invPeriod;
                        ph -= Math.floor(ph);
                        int bin = (int) (ph * nBins);
                        if (bin < 0) bin = 0;
                        else if (bin >= nBins) bin = nBins - 1;
                        binFluxSum[bin] += flux[j];
                        binN[bin]++;
                    }
                    double totalFluxSum = 0.0;
                    int    totalN       = 0;
                    for (int b = 0; b < nBins; b++) {
                        totalFluxSum += binFluxSum[b];
                        totalN       += binN[b];
                    }

                    // Reusable model-profile buffer sized for the worst-case halfWin.
                    double[] modelProfile = new double[nBins / 2 + 1];

                    double bestSNR         = 0.0;
                    double bestDuration    = 0.0;
                    double bestDepth       = 0.0;
                    double bestU1Cell      = u1GridF[0];
                    double bestU2Cell      = u2GridF[0];
                    double bestIngressCell = ingressGridF[0];

                    // Stage 2 — outer grid sweep over (u1, u2, ingressFrac).
                    // Stage-1 bin arrays are shared across all cells so we
                    // only pay the O(nPoints) fold once per period; the inner
                    // (durations × phases) cost still dominates and is
                    // multiplied by totalCells.
                    for (double u1v : u1GridF) {
                        for (double u2v : u2GridF) {
                            for (double ingressFrac : ingressGridF) {
                                // Per-cell cancel poll — still cheap relative
                                // to a full cell's (durations × phases) scan.
                                if (cancelCheck.getAsBoolean()) {
                                    sde[periodIdx]            = Double.NaN;
                                    bestDurations[periodIdx]  = Double.NaN;
                                    bestDepths[periodIdx]     = Double.NaN;
                                    bestU1Arr[periodIdx]      = Double.NaN;
                                    bestU2Arr[periodIdx]      = Double.NaN;
                                    bestIngressArr[periodIdx] = Double.NaN;
                                    return null;
                                }

                                for (int di = 0; di < nDurations; di++) {
                                    // Poll once per duration so a single cell's
                                    // long tail still aborts within a few ms.
                                    if (cancelCheck.getAsBoolean()) {
                                        sde[periodIdx]            = Double.NaN;
                                        bestDurations[periodIdx]  = Double.NaN;
                                        bestDepths[periodIdx]     = Double.NaN;
                                        bestU1Arr[periodIdx]      = Double.NaN;
                                        bestU2Arr[periodIdx]      = Double.NaN;
                                        bestIngressArr[periodIdx] = Double.NaN;
                                        return null;
                                    }
                                    double fracDuration = minDuration + di * (maxDuration - minDuration) / Math.max(1, nDurations - 1);
                                    double duration = fracDuration * period;
                                    double t14 = duration;
                                    double t12 = t14 * ingressFrac;

                                    int halfWin = (int) Math.round(0.5 * fracDuration * nBins);
                                    if (halfWin < 0) halfWin = 0;
                                    int maxHalfWin = (nBins - 1) / 2;
                                    if (halfWin > maxHalfWin) halfWin = maxHalfWin;

                                    // Precompute m(k) for bin offsets 0..halfWin.
                                    // m(k) = MandelAgol flux deficit at phase distance (k/nBins)*period
                                    // under the linear-ingress/quadratic-LD-plateau approximation.
                                    for (int k = 0; k <= halfWin; k++) {
                                        double absPhaseTime = ((double) k / nBins) * period;
                                        double m;
                                        if (absPhaseTime > 0.5 * t14) {
                                            m = 1.0;
                                        } else if (absPhaseTime < 0.5 * t14 - t12) {
                                            m = 1.0 - (1.0 - u1v / 3.0 - u2v / 6.0);
                                        } else {
                                            double x = (absPhaseTime - (0.5 * t14 - t12)) / t12;
                                            double limb = 1.0 - u1v * (1.0 - x) - u2v * (1.0 - x) * (1.0 - x);
                                            m = 1.0 - limb;
                                        }
                                        modelProfile[k] = m;
                                    }

                                    for (int ph = 0; ph < nBins; ph++) {
                                        double inTransitFluxSum = binFluxSum[ph];
                                        int    nIn              = binN[ph];
                                        double m0               = modelProfile[0];
                                        double sumFM_in         = m0 * binFluxSum[ph];
                                        double sumM2_in         = (m0 * m0) * binN[ph];

                                        for (int k = 1; k <= halfWin; k++) {
                                            int b1 = ph + k; if (b1 >= nBins) b1 -= nBins;
                                            int b2 = ph - k; if (b2 < 0)      b2 += nBins;
                                            double f12 = binFluxSum[b1] + binFluxSum[b2];
                                            int    n12 = binN[b1]       + binN[b2];
                                            double m   = modelProfile[k];
                                            inTransitFluxSum += f12;
                                            nIn              += n12;
                                            sumFM_in         += m * f12;
                                            sumM2_in         += (m * m) * n12;
                                        }

                                        double signal = inTransitFluxSum - nIn;
                                        double sumFM  = (totalFluxSum - inTransitFluxSum) + sumFM_in;
                                        double sumM2  = (totalN       - nIn)              + sumM2_in;

                                        double depthFit = (sumM2 > 0.0) ? sumFM / sumM2 : 0.0;
                                        double chi2     = (sumM2 > 0.0) ? sumF2 - (sumFM * sumFM) / sumM2 : sumF2;
                                        if (chi2 < 0) chi2 = 0;
                                        double noise = (nData > 0) ? Math.sqrt(chi2 / nData) : 1.0;
                                        double snr   = (nIn > 0 && noise > 0.0)
                                                       ? Math.abs(signal) / (noise * Math.sqrt(nIn))
                                                       : 0.0;

                                        if (snr > bestSNR) {
                                            bestSNR         = snr;
                                            bestDuration    = duration;
                                            bestDepth       = depthFit;
                                            bestU1Cell      = u1v;
                                            bestU2Cell      = u2v;
                                            bestIngressCell = ingressFrac;
                                        }
                                    }
                                }
                            }
                        }
                    }

                    sde[periodIdx]            = bestSNR;
                    bestDurations[periodIdx]  = bestDuration;
                    bestDepths[periodIdx]     = bestDepth;
                    bestU1Arr[periodIdx]      = bestU1Cell;
                    bestU2Arr[periodIdx]      = bestU2Cell;
                    bestIngressArr[periodIdx] = bestIngressCell;
                    int done = progress.incrementAndGet();
                    IJ.showProgress(done, nPeriods);
                    if (progressCallback != null) progressCallback.accept(done - 1);
                    return null;
                }));
            }
            // Drain the futures one-by-one, but check the cancel flag between
            // each so the Stop button reacts promptly.  If cancelled we
            // shutdownNow() the executor (discards queued tasks, interrupts
            // running ones) and mark any still-pending periods as NaN so the
            // downstream SDE / T0 code can recognise and skip them — mirrors
            // the BLS CPU Stop behaviour and the TLS GPU Stop behaviour.
            boolean searchCancelled = false;
            for (int pi = 0; pi < nPeriods; pi++) {
                if (cancelCheck.getAsBoolean()) {
                    searchCancelled = true;
                    executor.shutdownNow();
                    try { executor.awaitTermination(1, TimeUnit.SECONDS); }
                    catch (InterruptedException ignored) { Thread.currentThread().interrupt(); }
                    break;
                }
                try {
                    futures.get(pi).get();
                } catch (java.util.concurrent.CancellationException
                         | InterruptedException ie) {
                    sde[pi]           = Double.NaN;
                    bestDurations[pi] = Double.NaN;
                    bestDepths[pi]    = Double.NaN;
                } catch (java.util.concurrent.ExecutionException ee) {
                    IJ.log("[TLS] Period " + pi + " failed: " + ee.getCause());
                    sde[pi]           = Double.NaN;
                    bestDurations[pi] = Double.NaN;
                    bestDepths[pi]    = Double.NaN;
                }
            }
            if (searchCancelled) {
                // Backfill NaN for any slots the workers never got to write.
                for (int j = 0; j < nPeriods; j++) {
                    if (sde[j] == 0.0 && bestDurations[j] == 0.0 && bestDepths[j] == 0.0) {
                        sde[j]            = Double.NaN;
                        bestDurations[j]  = Double.NaN;
                        bestDepths[j]     = Double.NaN;
                        bestU1Arr[j]      = Double.NaN;
                        bestU2Arr[j]      = Double.NaN;
                        bestIngressArr[j] = Double.NaN;
                    }
                }
                IJ.log("[TLS CPU] Cancelled by user.");
            }
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
            double v = sde[i];
            // Skip the NaN tail left behind by a user Stop so median/std below
            // describe only real work.
            if (Double.isNaN(v)) continue;
            if (Math.abs(i - bestPeriodIdx) > sdeWindow) {
                sdeForStats.add(v);
            }
        }
        // Compute median and stddev
        double[] sdeStatsArr = sdeForStats.stream().mapToDouble(Double::doubleValue).toArray();
        double median = 0, std = 0;
        if (sdeStatsArr.length > 0) {
            Arrays.sort(sdeStatsArr);
            median = sdeStatsArr[sdeStatsArr.length / 2];
            double sumsq = 0;
            for (double v : sdeStatsArr) sumsq += (v - median) * (v - median);
            std = Math.sqrt(sumsq / sdeStatsArr.length);
        }
        for (int i = 0; i < sde.length; i++) {
            sde[i] = (std > 0) ? (sde[i] - median) / std : 0;
        }
        // If the user cancelled during the grid search there is no point
        // running T0 refinement on mostly-NaN data — the outer caller checks
        // tlsCancelled right after this returns and breaks the planet loop
        // before touching bestT0Sliding / t0MinAvg, so a stub result is fine
        // and saves a few hundred ms of extra compute after the Stop.
        if (cancelCheck.getAsBoolean()) {
            return new Result(periods, sde, bestDurations, bestDepths,
                    bestU1Arr, bestU2Arr, bestIngressArr,
                    Double.NaN, Double.NaN);
        }

        // After main grid search, do BLS-style T0 refinement for best period
        double bestPeriod = periods[bestPeriodIdx];
        double bestDuration = minDuration + (nDurations > 1 ? (nDurations - 1) / 2.0 * (maxDuration - minDuration) / (nDurations - 1) : 0.0); // crude guess, could be improved

        // --- Min average phase bin (compute first, for phase reference like BLS) ---
        int nT0Bins = 200;
        double[] phases = new double[flux.length];
        for (int i = 0; i < flux.length; i++) {
            double phase = (time[i] - time[0]) / bestPeriod;
            phase = phase - Math.floor(phase);
            phases[i] = phase;
        }
        List<List<Double>> binFluxes = new ArrayList<>();
        for (int i = 0; i < nT0Bins; i++) binFluxes.add(new ArrayList<>());
        for (int i = 0; i < flux.length; i++) {
            int bin = (int) Math.floor(phases[i] * nT0Bins);
            if (bin < 0) bin = 0;
            if (bin >= nT0Bins) bin = nT0Bins - 1;
            binFluxes.get(bin).add(flux[i]);
        }
        double minAvg = Double.POSITIVE_INFINITY;
        int minBin = -1;
        int minPointsPerBin = 5;
        for (int i = 0; i < nT0Bins; i++) {
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
        double minPhase = (minBin + 0.5) / nT0Bins;
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
        // Use bestDepth from the periodogram grid search (approximate as the depth from the SDE grid search).
        // a/Rs and inc are no longer user inputs — they only seeded this depth guess and had zero
        // effect on the SDE periodogram itself, so hard-coded defaults are adequate.  The winning
        // (u1, u2) at bestPeriod is used so the depth seed matches the template that actually won.
        final double aRsDefaultCpu = 10.0;
        final double incDefaultCpu = 89.0;
        double winningU1 = bestU1Arr[bestPeriodIdx];
        double winningU2 = bestU2Arr[bestPeriodIdx];
        if (Double.isNaN(winningU1)) winningU1 = u1GridF[0];
        if (Double.isNaN(winningU2)) winningU2 = u2GridF[0];
        double bestDepthForSliding = 0.0;
        {
            double bestChi2 = Double.POSITIVE_INFINITY;
            for (double depthTest : new double[]{0.001, 0.002, 0.005, 0.01, 0.02, 0.05, 0.1}) {
                double[] model = MandelAgolTransitModel.compute(time, bestPeriod, t0MinAvg, bestDuration,
                        depthTest, Math.sqrt(depthTest),
                        aRsDefaultCpu, incDefaultCpu, winningU1, winningU2);
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
        // --- Sort phases once so each grid cell can binary-search the in-window points ---
        // Before: O(nPoints) per cell (scan all, most rejected), + array allocs per cell.
        // After:  O(log n + window_points) per cell, no per-cell allocations.
        final int nDataT0 = flux.length;
        Integer[] sortIdxT0 = new Integer[nDataT0];
        for (int i = 0; i < nDataT0; i++) sortIdxT0[i] = i;
        final double[] phasesCaptureT0 = phasesForSliding;
        Arrays.sort(sortIdxT0, Comparator.comparingDouble(i -> phasesCaptureT0[i]));
        final double[] sortedPhasesT0 = new double[nDataT0];
        final double[] sortedFluxT0   = new double[nDataT0];
        for (int i = 0; i < nDataT0; i++) {
            sortedPhasesT0[i] = phasesForSliding[sortIdxT0[i]];
            sortedFluxT0[i]   = flux[sortIdxT0[i]];
        }

        final double durOverPeriodT0 = bestDuration / bestPeriod;
        final double windowT0        = 1.5 * durOverPeriodT0;
        final double deltaT0         = delta;
        final double phaseStepT0     = phaseStep;
        final int    nPhaseStepsT0   = nPhaseSteps;
        final double bestDepthT0     = bestDepthForSliding;
        final double bestPeriodT0    = bestPeriod;

        // --- Parallelise the outer tauFrac loop across CPU cores ---
        int nT0Threads = Math.max(1, Runtime.getRuntime().availableProcessors() - 2);
        ExecutorService t0Exec = Executors.newFixedThreadPool(nT0Threads);
        List<Future<double[]>> t0Futures = new ArrayList<>();
        long t0Start = System.currentTimeMillis();

        for (int tauIdx = 0; tauIdx < nTau; tauIdx++) {
            final double tauFrac = minTauFrac + tauIdx * tauFracStep;
            final double tauPhase = tauFrac * durOverPeriodT0;
            final double flatPhase = durOverPeriodT0 - 2 * tauPhase;
            if (flatPhase < 0) continue; // skip unphysical
            final double tauPhaseF  = tauPhase;
            final double flatPhaseF = flatPhase;

            t0Futures.add(t0Exec.submit(() -> {
                double localBestLoss        = Double.POSITIVE_INFINITY;
                double localBestPhaseOffset = 0;

                for (int s = 0; s < nPhaseStepsT0; s++) {
                    // Poll so a Stop during T0 refinement aborts promptly too.
                    if (cancelCheck.getAsBoolean()) break;
                    double phaseOffset = s * phaseStepT0 * bestPeriodT0;
                    double C = phaseOffset / bestPeriodT0;
                    C -= Math.floor(C);

                    double sumLoss = 0.0;
                    int    nUsed   = 0;

                    if (windowT0 >= 0.5) {
                        // Window spans the full circle — iterate every point.
                        for (int k = 0; k < nDataT0; k++) {
                            double ps = sortedPhasesT0[k] - C;
                            ps -= Math.floor(ps);
                            double mdl;
                            if      (ps < tauPhaseF)                mdl = -bestDepthT0 * (ps / tauPhaseF);
                            else if (ps < tauPhaseF + flatPhaseF)   mdl = -bestDepthT0;
                            else if (ps < 2*tauPhaseF + flatPhaseF) mdl = -bestDepthT0 * (1 - (ps - tauPhaseF - flatPhaseF) / tauPhaseF);
                            else                                     mdl = 0.0;
                            double rr = sortedFluxT0[k] - mdl;
                            double absR = Math.abs(rr);
                            sumLoss += (absR <= deltaT0) ? 0.5*rr*rr : deltaT0*(absR - 0.5*deltaT0);
                            nUsed++;
                        }
                    } else {
                        // Binary-search the in-window arc [C-window, C+window] (circular)
                        double lo = C - windowT0;
                        double hi = C + windowT0;
                        int r0a, r0b, r1a, r1b;
                        if (lo < 0) {
                            r0a = lowerBoundD(sortedPhasesT0, lo + 1);  r0b = nDataT0;
                            r1a = 0;                                    r1b = upperBoundD(sortedPhasesT0, hi);
                        } else if (hi >= 1.0) {
                            r0a = lowerBoundD(sortedPhasesT0, lo);      r0b = nDataT0;
                            r1a = 0;                                    r1b = upperBoundD(sortedPhasesT0, hi - 1);
                        } else {
                            r0a = lowerBoundD(sortedPhasesT0, lo);      r0b = upperBoundD(sortedPhasesT0, hi);
                            r1a = 0;                                    r1b = 0;
                        }
                        for (int k = r0a; k < r0b; k++) {
                            double ps = sortedPhasesT0[k] - C;
                            ps -= Math.floor(ps);
                            double mdl;
                            if      (ps < tauPhaseF)                mdl = -bestDepthT0 * (ps / tauPhaseF);
                            else if (ps < tauPhaseF + flatPhaseF)   mdl = -bestDepthT0;
                            else if (ps < 2*tauPhaseF + flatPhaseF) mdl = -bestDepthT0 * (1 - (ps - tauPhaseF - flatPhaseF) / tauPhaseF);
                            else                                     mdl = 0.0;
                            double rr = sortedFluxT0[k] - mdl;
                            double absR = Math.abs(rr);
                            sumLoss += (absR <= deltaT0) ? 0.5*rr*rr : deltaT0*(absR - 0.5*deltaT0);
                            nUsed++;
                        }
                        for (int k = r1a; k < r1b; k++) {
                            double ps = sortedPhasesT0[k] - C;
                            ps -= Math.floor(ps);
                            double mdl;
                            if      (ps < tauPhaseF)                mdl = -bestDepthT0 * (ps / tauPhaseF);
                            else if (ps < tauPhaseF + flatPhaseF)   mdl = -bestDepthT0;
                            else if (ps < 2*tauPhaseF + flatPhaseF) mdl = -bestDepthT0 * (1 - (ps - tauPhaseF - flatPhaseF) / tauPhaseF);
                            else                                     mdl = 0.0;
                            double rr = sortedFluxT0[k] - mdl;
                            double absR = Math.abs(rr);
                            sumLoss += (absR <= deltaT0) ? 0.5*rr*rr : deltaT0*(absR - 0.5*deltaT0);
                            nUsed++;
                        }
                    }

                    double cellLoss = (nUsed > 0) ? sumLoss / nUsed : Double.POSITIVE_INFINITY;
                    if (cellLoss < localBestLoss) {
                        localBestLoss        = cellLoss;
                        localBestPhaseOffset = phaseOffset;
                    }
                }
                return new double[] { localBestLoss, localBestPhaseOffset };
            }));
        }

        try {
            for (Future<double[]> fut : t0Futures) {
                if (cancelCheck.getAsBoolean()) {
                    t0Exec.shutdownNow();
                    try { t0Exec.awaitTermination(1, TimeUnit.SECONDS); }
                    catch (InterruptedException ignored) { Thread.currentThread().interrupt(); }
                    break;
                }
                try {
                    double[] r = fut.get();
                    if (r[0] < minLoss) {
                        minLoss       = r[0];
                        bestT0Sliding = time[0] + r[1];
                    }
                } catch (java.util.concurrent.CancellationException ce) {
                    // task was cancelled by shutdownNow() — ignore
                }
            }
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            IJ.log("[TLS T0] Parallel T0 search interrupted.");
        } catch (java.util.concurrent.ExecutionException ee) {
            IJ.log("[TLS T0] Parallel T0 search error: " + ee.getMessage());
        } finally {
            t0Exec.shutdown();
        }
        System.out.printf("[TLS T0] Parallel T0 refinement: %d threads, %d ms%n",
                nT0Threads, System.currentTimeMillis() - t0Start);
        System.out.printf("[TLS DIAG] Sliding fit (BLS-style): bestT0Sliding = %.6f, minLoss = %.6g\n", bestT0Sliding, minLoss);

        return new Result(periods, sde, bestDurations, bestDepths,
                bestU1Arr, bestU2Arr, bestIngressArr,
                bestT0Sliding, t0MinAvg);
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
        /** Per-period winning limb-darkening u1 from the blind grid. */
        public double[] bestU1;
        /** Per-period winning limb-darkening u2 from the blind grid. */
        public double[] bestU2;
        /** Per-period winning ingress fraction (t12/t14) from the blind grid. */
        public double[] bestIngressFrac;
        public double t0_sliding;
        public double t0_minavg;

        public Result(double[] periods, double[] sde,
                      double[] bestDurations, double[] bestDepths,
                      double[] bestU1, double[] bestU2, double[] bestIngressFrac,
                      double t0_sliding, double t0_minavg) {
            this.periods        = periods;
            this.sde            = sde;
            this.bestDurations  = bestDurations;
            this.bestDepths     = bestDepths;
            this.bestU1         = bestU1;
            this.bestU2         = bestU2;
            this.bestIngressFrac= bestIngressFrac;
            this.t0_sliding     = t0_sliding;
            this.t0_minavg      = t0_minavg;
        }
    }
} 