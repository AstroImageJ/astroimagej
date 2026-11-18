package astroj.astrometry;

import ij.plugin.filter.RankFilters;
import uk.ac.manchester.tornado.api.annotations.Parallel;

final class StarKernels {
    static final double MAX_MEDIAN_RADIUS = 100.0; // See IJ's RankFilters median limit

    private StarKernels() {
    }

    /// Half-width of every row of the kernel; port of [RankFilters#makeLineRadii(double)] so the
    /// mask is identical to IJ's. The kernel radius in x and y is (rows.length - 1) / 2.
    @SuppressWarnings("JavadocReference")
    static int[] medianRows(double radius) {
        if (!(radius > 0) || radius > MAX_MEDIAN_RADIUS)
            throw new IllegalArgumentException("radius must be in (0, " + MAX_MEDIAN_RADIUS + "]");
        if (radius >= 1.5 && radius < 1.75) {
            radius = 1.75;
        } else if (radius >= 2.5 && radius < 2.85) {
            radius = 2.85;
        }
        int r2 = (int) (radius * radius) + 1;
        int kR = (int) Math.sqrt(r2 + 1e-10);
        int[] rows = new int[2 * kR + 1];
        rows[kR] = kR;
        for (int y = 1; y <= kR; y++) {
            int d = (int) Math.sqrt(r2 - y * y + 1e-10);
            rows[kR - y] = d;
            rows[kR + y] = d;
        }
        return rows;
    }

    static void medianFilter(float[] src, float[] dst, int[] dxRow, int width, int height, int kR) {
        for (@Parallel int y = 0; y < height; y++) {
            for (@Parallel int x = 0; x < width; x++) {
                dst[y * width + x] = medianAt(src, dxRow, width, height, kR, x, y);
            }
        }
    }

    /// Median of the circular window around (x, y); the per-pixel body of [#medianFilter].
    /// Port of ImageJ's [RankFilters#getNaNAwareMedian(float\[\], int, int\[\], float\[\], float\[\], int, float)]
    @SuppressWarnings("JavadocReference")
    static float medianAt(float[] src, int[] dxRow, int width, int height, int kR, int x, int y) {
        float center = src[y * width + x];
        if (Float.isNaN(center)) return center;

        int kHeight = 2 * kR + 1;
        int nValid = 0;
        int nAbove = 0; // valid values > center
        int nBelow = 0; // valid values < center
        float mn = 0f;
        float mx = 0f;
        for (int ky = 0; ky < kHeight; ky++) {
            int yy = y + ky - kR;
            // clamp to the image
            if (yy < 0) yy = 0;
            if (yy > height - 1) yy = height - 1;
            int rowBase = yy * width;
            int r = dxRow[ky];
            for (int xx = x - r; xx <= x + r; xx++) {
                int xc = xx;
                if (xc < 0) xc = 0;
                if (xc > width - 1) xc = width - 1;
                float v = src[rowBase + xc];
                if (v == v) { // not NaN
                    if (nValid == 0) {
                        mn = v;
                        mx = v;
                    } else {
                        if (v < mn) mn = v;
                        if (v > mx) mx = v;
                    }
                    nValid++;
                    if (v > center) {
                        nAbove++;
                    } else if (v < center) {
                        nBelow++;
                    }
                }
            }
        }

        int half = nValid / 2;
        int k = -1;
        if (nAbove > half) {
            k = nValid - half - 1;
        } else if (nBelow > half) {
            k = half;
        }

        float result = center;
        if (k >= 0) {
            float lo = mn;
            float hi = mx;
            int below = 0;
            result = lo;
            boolean done = (lo == hi);
            while (!done) {
                float pivot = lo * 0.5f + hi * 0.5f;
                if (Float.isNaN(pivot)) pivot = lo;
                if (pivot < lo) pivot = lo;
                if (pivot > hi) pivot = hi;

                int cLess = 0;
                int cEq = 0;
                float lessMax = lo;
                float greatMin = hi;
                boolean hasL = false;
                boolean hasG = false;
                for (int ky = 0; ky < kHeight; ky++) {
                    int yy = y + ky - kR;
                    if (yy < 0) yy = 0;
                    if (yy > height - 1) yy = height - 1;
                    int rowBase = yy * width;
                    int r = dxRow[ky];
                    for (int xx = x - r; xx <= x + r; xx++) {
                        int xc = xx;
                        if (xc < 0) xc = 0;
                        if (xc > width - 1) xc = width - 1;
                        float v = src[rowBase + xc];
                        if (v == v && v >= lo && v <= hi) {
                            if (v < pivot) {
                                cLess++;
                                if (!hasL || v > lessMax) {
                                    lessMax = v;
                                    hasL = true;
                                }
                            } else if (v > pivot) {
                                if (!hasG || v < greatMin) {
                                    greatMin = v;
                                    hasG = true;
                                }
                            } else {
                                cEq++;
                            }
                        }
                    }
                }
                int lowRank = below + cLess;
                int highRank = lowRank + cEq;
                if (k < lowRank) {
                    hi = lessMax;
                } else if (k >= highRank) {
                    below = highRank;
                    lo = greatMin;
                } else {
                    result = pivot;
                    done = true;
                }
                if (!done && lo == hi) {
                    result = lo;
                    done = true;
                }
            }
        }
        return result;
    }

    /// Sets `types[p] = GpuMaxima.MAXIMUM` for every pixel p inside the ROI that is not an image-edge pixel and has
    /// no strictly greater 8-neighbor, and 0 for every other pixel.
    static void markMaxima(float[] img, byte[] types, int width, int height, int rx, int ry, int rw, int rh) {
        for (@Parallel int y = 0; y < height; y++) {
            for (@Parallel int x = 0; x < width; x++) {
                byte t = 0;
                if (isLocalMax(img, width, height, x, y, rx, ry, rw, rh)) {
                    t = GpuMaxima.MAXIMUM;
                }
                types[x + y * width] = t;
            }
        }
    }

    /// Check if it is an inner pixel, within the ROI, and that it is a local maximum of its 8 neighbors
    static boolean isLocalMax(float[] img, int width, int height, int x, int y, int rx, int ry, int rw, int rh) {
        var ok =
                x >= rx && x < rx + rw && y >= ry && y < ry + rh && // Within ROI
                        x != 0 && x != width - 1 && y != 0 && y != height - 1; // Not an image edge
        if (ok) {
            int p = x + y * width;
            float v = img[p];
            if (Float.isNaN(v)) return false;
            if (img[p - width - 1] > v) ok = false;
            if (img[p - width] > v) ok = false;
            if (img[p - width + 1] > v) ok = false;
            if (img[p - 1] > v) ok = false;
            if (img[p + 1] > v) ok = false;
            if (img[p + width - 1] > v) ok = false;
            if (img[p + width] > v) ok = false;
            if (img[p + width + 1] > v) ok = false;
        }
        return ok;
    }
}
