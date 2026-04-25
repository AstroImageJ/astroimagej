/*
 * tls_periodogram.cl  — Phase-binned TLS periodogram for AIJ Periodogram.
 *
 * ALGORITHM
 * ---------
 * The naive O(nDurations × nPhase × nPoints) TLS causes each GPU thread to run
 * for many seconds on large datasets (e.g. 15 000 points × 20 durations ×
 * 1000 phases = 300 M iterations), triggering Windows' 2-second TDR watchdog
 * and deadlocking clFinish() on NVIDIA WDDM drivers.
 *
 * This kernel follows the same two-stage strategy that made BLS viable:
 *
 *   Stage 1 — Phase-fold  O(nPoints)
 *     Fold every data point into one of TLS_BINS uniform phase bins.
 *     Accumulate flux sum and count per bin.
 *
 *   Stage 2 — Per-duration model-weighted window sweep
 *                                   O(nDurations × TLS_BINS × halfWin)
 *     For each trial duration we precompute the Mandel-Agol model value at
 *     each bin offset from transit centre, then for every phase offset we
 *     sum flux and counts over the in-transit window.  Out-of-transit bins
 *     contribute m=1 implicitly via (total - inTransit) terms.
 *
 * PRECISION
 * ---------
 * Full DOUBLE PRECISION end-to-end.  FP32 produced wrong peak rankings for
 * shallow long-period transits because (a) the Stage 1 phase fold in FP32
 * shifts the narrow transit across bin boundaries at long periods, and
 * (b) chi2 = sumF2 - sumFM^2/sumM2 catastrophically cancels in FP32 when
 * the fit is good (sumFM^2/sumM2 ≈ sumF2).  sumF2 is pre-computed on the
 * host in double and passed in as a kernel argument.
 *
 * Global work size: { nPeriods } (padded to multiple of 64).
 * One work-item = one period.
 */

/*
 * Compile-time bin count.  The Java host prepends `#define TLS_BINS <n>`
 * to the source before compilation; the guard below only takes effect if the
 * file is compiled standalone (e.g. for offline testing).
 */
#ifndef TLS_BINS
#define TLS_BINS 200
#endif

/*
 * MAX_HALF_WIN bounds the per-duration model-profile half-window.  It is
 * prepended by the Java host as
 *     #define MAX_HALF_WIN <ceil(0.5 * maxFracDur * TLS_BINS) clamped to
 *                           (TLS_BINS-1)/2>
 * so the `modelProfile` array below is sized to the user's actual search
 * range rather than the full TLS_BINS/2+1 worst case.  For typical TLS
 * settings (maxFracDur ~ 0.1, TLS_BINS = 1000) this shrinks the array
 * from ~4 KB to ~400 B, small enough to stay in registers instead of
 * spilling to NVIDIA's unreliable backing store (see bin-array note
 * below).
 */
#ifndef MAX_HALF_WIN
#define MAX_HALF_WIN ((TLS_BINS - 1) / 2)
#endif

#pragma OPENCL EXTENSION cl_khr_fp64 : enable

/*
 * NOTE: the two *bin* arrays (binFluxSum, binN) live in *global* memory
 * rather than as function-local (__private) arrays.  See the same note in
 * bls_periodogram.cl: at TLS_BINS = 1000 the 12 KB private footprint is
 * large enough that NVIDIA's OpenCL compiler spills it to a backing store
 * whose Stage-1-write / Stage-2-read ordering proved unreliable in
 * practice.
 *
 * MEMORY LAYOUT — BIN MAJOR (coalesced).  We do NOT give each work-item
 * a contiguous TLS_BINS-sized slice; instead the same bin index for all
 * work-items is contiguous:
 *
 *     scratchBinFluxSum[bin * nPeriods + gid]
 *
 * In Stage 2 every lane of a warp reads at the same bin index (`ph`, `b1`,
 * `b2`) in lock step.  With bin-major the 32 lanes hit consecutive
 * addresses, merging into a single coalesced transaction per load.  With
 * the older thread-major layout every warp load was a scatter across 32
 * separate 8 KB pages.
 *
 * The model-profile array, in contrast, is kept in __private memory sized
 * to MAX_HALF_WIN + 1 — small enough to stay in registers, so it doesn't
 * suffer the spill bug and avoids the large global-memory read traffic
 * that otherwise dominates Stage 2.
 */
#define BIN_FLUX(b)  scratchBinFluxSum[((size_t)(b)) * (size_t)nPeriods + (size_t)gid]
#define BIN_N(b)     scratchBinN      [((size_t)(b)) * (size_t)nPeriods + (size_t)gid]
__kernel void tls_compute(
    __global const double* t,         /* normalised time      [nPoints]          */
    __global const double* f,         /* flux                 [nPoints]          */
    __global const double* periods,   /* period grid          [nPeriods]         */
    __global       double* outSnr,    /* best SNR             [nPeriods]         */
    __global       double* outDepth,  /* corresponding depth  [nPeriods]         */
    __global       double* outDuration,/* corresponding duration (days) [nPeriods] */
    /* Scratch buffers in BIN-MAJOR layout (see note above).              */
    __global       double* scratchBinFluxSum, /* [bin * nPeriods + gid]   */
    __global       int*    scratchBinN,       /* [bin * nPeriods + gid]   */
    const int    nPoints,
    const int    nPeriods,
    const int    nDurations,
    const double minFracDur,
    const double maxFracDur,
    const double u1,
    const double u2,
    const double sumF2                /* sum_j f[j]^2, host-side double          */
) {
    int gid = get_global_id(0);
    if (gid >= nPeriods) return;

    const double period    = periods[gid];
    const double invPeriod = 1.0 / period;

    /* --- Stage 1: phase-fold all points into TLS_BINS bins (bin-major
     *              global scratch — see layout note above).          --- */
    for (int b = 0; b < TLS_BINS; b++) {
        BIN_FLUX(b) = 0.0;
        BIN_N(b)    = 0;
    }

    for (int j = 0; j < nPoints; j++) {
        double ph = t[j] * invPeriod;
        ph -= floor(ph);
        int bin = (int)(ph * (double)TLS_BINS);
        if ((unsigned int)bin >= (unsigned int)TLS_BINS) bin = TLS_BINS - 1;
        BIN_FLUX(bin) += f[j];
        BIN_N(bin)    += 1;
    }

    double totalFluxSum = 0.0;
    int    totalN       = 0;
    for (int b = 0; b < TLS_BINS; b++) {
        totalFluxSum += BIN_FLUX(b);
        totalN       += BIN_N(b);
    }

    /* --- Stage 2: per-duration model-weighted window sweep. -------------- */
    const double fracRange = maxFracDur - minFracDur;
    const int    nDurM1    = max(1, nDurations - 1);
    const int    maxHalfWin = MAX_HALF_WIN;   /* host-sized; avoids b1==b2 collision */

    /* Reusable model profile for the current duration.  Lives in private
     * memory, sized to the user's actual maxFracDur (see top of file) so
     * it fits in registers rather than spilling.  Reads in the hot phase
     * loop then avoid any global-memory traffic. */
    double modelProfile[MAX_HALF_WIN + 1];

    double bestSnr      = 0.0;
    double bestDepth    = 0.0;
    double bestDuration = 0.0;

    for (int d = 0; d < nDurations; d++) {
        const double fracDur  = minFracDur + (double)d * fracRange / (double)nDurM1;
        const double duration = fracDur * period;
        const double t14      = duration;
        const double t12      = t14 * 0.25;

        int halfWin = (int)(0.5 * fracDur * (double)TLS_BINS + 0.5);
        if (halfWin < 0)          halfWin = 0;
        if (halfWin > maxHalfWin) halfWin = maxHalfWin;

        /* Precompute model profile m(k) at bin offsets k = 0..halfWin. */
        for (int k = 0; k <= halfWin; k++) {
            double absPhaseTime = ((double)k / (double)TLS_BINS) * period;
            double m;
            if (absPhaseTime > 0.5 * t14) {
                m = 1.0;
            } else if (absPhaseTime < 0.5 * t14 - t12) {
                m = u1 / 3.0 + u2 / 6.0;     /* 1 - (1 - u1/3 - u2/6) */
            } else {
                double x    = (absPhaseTime - (0.5 * t14 - t12)) / t12;
                double limb = 1.0 - u1 * (1.0 - x)
                                  - u2 * (1.0 - x) * (1.0 - x);
                m = 1.0 - limb;
            }
            modelProfile[k] = m;
        }

        /* Sweep every phase trial. */
        for (int ph = 0; ph < TLS_BINS; ph++) {
            double f0               = BIN_FLUX(ph);
            int    n0               = BIN_N(ph);
            double inTransitFluxSum = f0;
            int    nIn              = n0;
            double m0               = modelProfile[0];
            double sumFM_in         = m0 * f0;
            double sumM2_in         = (m0 * m0) * (double)n0;

            for (int k = 1; k <= halfWin; k++) {
                int b1 = ph + k; if (b1 >= TLS_BINS) b1 -= TLS_BINS;
                int b2 = ph - k; if (b2 < 0)         b2 += TLS_BINS;
                double f12 = BIN_FLUX(b1) + BIN_FLUX(b2);
                int    n12 = BIN_N(b1)    + BIN_N(b2);
                double m   = modelProfile[k];
                inTransitFluxSum += f12;
                nIn              += n12;
                sumFM_in         += m * f12;
                sumM2_in         += (m * m) * (double)n12;
            }

            /* signal = sum_{in-transit} (f - 1) */
            double signal = inTransitFluxSum - (double)nIn;

            /* out-of-transit bins contribute m=1 to both sumFM and sumM2 */
            double sumFM = (totalFluxSum - inTransitFluxSum) + sumFM_in;
            double sumM2 = (double)(totalN - nIn) + sumM2_in;

            double depthFit = (sumM2 > 0.0) ? sumFM / sumM2 : 0.0;
            double chi2     = (sumM2 > 0.0) ? sumF2 - (sumFM * sumFM) / sumM2 : sumF2;
            if (chi2 < 0.0) chi2 = 0.0;        /* FP round-off guard */
            double noise = (nPoints > 0) ? sqrt(chi2 / (double)nPoints) : 1.0;
            double snr   = (nIn > 0 && noise > 0.0)
                           ? fabs(signal) / (noise * sqrt((double)nIn))
                           : 0.0;

            if (snr > bestSnr) {
                bestSnr      = snr;
                bestDepth    = depthFit;
                bestDuration = duration;
            }
        }
    }

    outSnr[gid]      = bestSnr;
    outDepth[gid]    = bestDepth;
    outDuration[gid] = bestDuration;
}
