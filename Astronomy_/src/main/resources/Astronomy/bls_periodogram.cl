/*
 * bls_periodogram.cl  — Phase-binned BLS periodogram for AIJ Periodogram.
 *
 * ALGORITHM
 * ---------
 * The naive O(nDurations × nPhase × nPoints) direct BLS causes each GPU thread
 * to run for many seconds on large datasets (e.g. 15 000 points × 200 durations
 * × 200 phases = 600 M iterations), triggering Windows' 2-second TDR watchdog
 * and permanently deadlocking clFinish() on NVIDIA WDDM drivers.
 *
 * This kernel uses a two-stage approach that keeps per-thread work bounded:
 *
 *   Stage 1 — Phase-fold  O(nPoints)
 *     For this period, fold every data point into one of BLS_BINS phase bins.
 *     Accumulate flux sum and count per bin.  Cost: nPoints fmods.
 *
 *   Stage 2 — Sliding-window BLS  O(nDurations × BLS_BINS)
 *     For each trial duration (in bins), slide a box of that width around the
 *     full phase circle in unit-bin steps, computing the BLS power statistic
 *     using incremental in/out sums — no inner data-point loop at all.
 *     Cost: nDurations × BLS_BINS iterations, each O(1).
 *
 * Total per period:  ~(nPoints + nDurations × BLS_BINS) ≈ 55 000 for the
 * user's 15 648-point, 200-duration dataset.
 *
 * PRECISION
 * ---------
 * DOUBLE PRECISION is used throughout — for t, f, periods, phase fold, bin sums,
 * and all Stage 2 accumulators.  FP32 was tried first but produced wrong peak
 * rankings for shallow (<0.005) long-period (>100 d) transits because:
 *   (a) depth = meanOut - meanIn is a catastrophic cancellation of two numbers
 *       that differ in the last 3-4 FP32 digits;
 *   (b) the sliding-window incremental sum drifts over BLS_BINS updates;
 *   (c) the Stage 1 phase fold ph = t[j] * (1/period) in FP32 can shift a
 *       narrow transit across bin boundaries at long periods where the transit
 *       is only 2-3 bins wide.
 *
 * Host inputs (t, f) are passed raw — no tMin subtraction, no flux mean
 * subtraction.  The CPU path in Periodogram_.java uses raw values too, and
 * matching the arithmetic exactly is what guarantees CPU/GPU agreement on
 * which (period, duration, phase) the power surface peaks at.  Raw BJD
 * (~2.5e6) still leaves ~11 decimal digits of precision in the fractional
 * phase after floor(), comfortably above the ~3 digits needed for 1000 bins.
 *
 * The kernel runs in a few ms at most on any modern GPU with cl_khr_fp64.
 *
 * Global work size: { nPeriods } (padded to multiple of 64).
 * One work-item = one period.
 */

/*
 * Compile-time bin count.  The Java host prepends `#define BLS_BINS <n>`
 * to the source before compilation; the guard below only takes effect if
 * the file is compiled standalone (for testing outside AIJ).
 */
#ifndef BLS_BINS
#define BLS_BINS 200
#endif

#pragma OPENCL EXTENSION cl_khr_fp64 : enable

/*
 * NOTE: the two bin arrays live in *global* memory rather than as
 * function-local (__private) arrays.  With BLS_BINS = 1000 the private
 * footprint was 12 KB per work-item, large enough that the NVIDIA OpenCL
 * compiler spills them to an implementation-defined backing store whose
 * Stage-1-write / Stage-2-read ordering proved unreliable in practice.
 *
 * MEMORY LAYOUT — BIN MAJOR (coalesced).
 * We do NOT give each work-item a contiguous BLS_BINS-sized slice.  Instead
 * we interleave so the same bin index for all work-items is contiguous:
 *
 *     scratchBinFluxSum[bin * nPeriods + gid]
 *
 * Rationale: in Stage 2 every lane of a warp reads/writes at the same bin
 * index `p` (and at `addBin`) in lock step.  With the bin-major layout
 * those 32 lanes hit consecutive addresses, merging into a single coalesced
 * 256-byte transaction per load; with the older thread-major layout every
 * warp load was a scatter across 32 separate 8 KB pages.  The Stage-1
 * zeroing loop and the post-fold prefix-sum loop also become coalesced.
 * The only step that remains non-coalesced is the Stage-1 histogram
 * scatter itself, because each lane's `bin` depends on its period — but
 * that's ~nPoints writes per thread (≤ ~100 K), whereas Stage 2 does
 * ~nDurations × BLS_BINS × 4 = millions of loads per thread, so the win
 * dominates.
 */
#define BIN_FLUX(b)  scratchBinFluxSum[((size_t)(b)) * (size_t)nPeriods + (size_t)gid]
#define BIN_N(b)     scratchBinN      [((size_t)(b)) * (size_t)nPeriods + (size_t)gid]
__kernel void bls_compute(
    __global const double* t,            /* raw time (BJD or any frame) [nPoints]  */
    __global const double* f,            /* raw flux (~1.0 nominal)     [nPoints]  */
    __global const double* periods,      /* period grid                 [nPeriods] */
    __global       double* outPower,     /* best BLS power            [nPeriods] */
    __global       double* outDuration,  /* best duration       (days)[nPeriods] */
    __global       double* outDepth,     /* best depth                [nPeriods] */
    __global       double* outPhase,     /* best phase          (days)[nPeriods] */
    /* Scratch buffers in BIN-MAJOR layout (see note above).              */
    __global       double* scratchBinFluxSum, /* [bin * nPeriods + gid]   */
    __global       int*    scratchBinN,       /* [bin * nPeriods + gid]   */
    const int  nPoints,
    const int  nPeriods,
    const int  nDurations,
    const double minFracDur,
    const double maxFracDur
) {
    int gid = get_global_id(0);
    if (gid >= nPeriods) return;

    const double period    = periods[gid];
    const double invPeriod = 1.0 / period;

    /* ------------------------------------------------------------------ */
    /* Stage 1: phase-fold all data points into BLS_BINS uniform bins.    */
    /* BIN_FLUX(b) = sum of f[j] for points whose phase lands in bin b    */
    /* BIN_N(b)    = number of such points                                */
    /*                                                                    */
    /* Storage is bin-major (see layout note above) so the zeroing loop   */
    /* and every Stage-2 bin access coalesces across warp lanes.          */
    /* ------------------------------------------------------------------ */
    for (int b = 0; b < BLS_BINS; b++) {
        BIN_FLUX(b) = 0.0;
        BIN_N(b)    = 0;
    }

    for (int j = 0; j < nPoints; j++) {
        /* Phase in [0, 1): ph = frac(t[j] / period) */
        double ph = t[j] * invPeriod;
        ph -= floor(ph);
        int bin = (int)(ph * (double)BLS_BINS);
        if ((unsigned int)bin >= (unsigned int)BLS_BINS) bin = BLS_BINS - 1;
        BIN_FLUX(bin) += f[j];
        BIN_N(bin)    += 1;
    }

    /* ------------------------------------------------------------------ */
    /* Stage 2: sliding-window BLS over the binned representation.        */
    /* ------------------------------------------------------------------ */
    const double fracRange = maxFracDur - minFracDur;
    const int    nDurM1    = max(1, nDurations - 1);

    double maxPower  = 0.0;
    double bestDur   = 0.0;
    double bestDepth = 0.0;
    double bestPhase = 0.0;

    for (int d = 0; d < nDurations; d++) {
        const double fracDur = minFracDur + (double)d * fracRange / (double)nDurM1;
        int durBins = (int)(fracDur * (double)BLS_BINS + 0.5);
        if (durBins < 1)         durBins = 1;
        if (durBins >= BLS_BINS) durBins = BLS_BINS - 1;
        const double duration = fracDur * period;

        /* Build initial window: bins [0, durBins) = "in-transit" */
        double sumIn  = 0.0;
        double sumOut = 0.0;
        double nIn    = 0.0;
        double nOut   = 0.0;
        for (int k = 0; k < durBins; k++) {
            sumIn += BIN_FLUX(k);
            nIn   += (double)BIN_N(k);
        }
        for (int k = durBins; k < BLS_BINS; k++) {
            sumOut += BIN_FLUX(k);
            nOut   += (double)BIN_N(k);
        }

        /* Slide the window through all BLS_BINS phase offsets */
        for (int p = 0; p < BLS_BINS; p++) {
            if (nIn >= 2.0 && nOut >= 2.0) {
                const double meanIn  = sumIn  / nIn;
                const double meanOut = sumOut / nOut;
                const double depth   = meanOut - meanIn;
                const double power   = depth * depth * nIn * nOut / (nIn + nOut);
                if (power > maxPower) {
                    maxPower  = power;
                    bestDur   = duration;
                    bestDepth = depth;
                    /* Phase offset of transit centre within the period */
                    bestPhase = ((double)p + (double)durBins * 0.5)
                                / (double)BLS_BINS * period;
                }
            }

            /* Slide: bin p leaves the window, bin (p + durBins) enters */
            int addBin = p + durBins;
            if (addBin >= BLS_BINS) addBin -= BLS_BINS;

            double fp = BIN_FLUX(p);       double fa = BIN_FLUX(addBin);
            double np = (double)BIN_N(p);  double na = (double)BIN_N(addBin);
            sumIn  -= fp;   nIn  -= np;
            sumIn  += fa;   nIn  += na;
            sumOut += fp;   nOut += np;
            sumOut -= fa;   nOut -= na;
        }
    }

    outPower[gid]    = maxPower;
    outDuration[gid] = bestDur;
    outDepth[gid]    = bestDepth;
    outPhase[gid]    = bestPhase;
}
