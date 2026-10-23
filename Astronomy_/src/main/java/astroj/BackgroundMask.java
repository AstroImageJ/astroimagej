package astroj;

import java.util.Set;

/**
 * Background annulus pixels rejected by star removal on a reference image. Reusing the same rejection for the
 * rest of a stack keeps the background estimate continuous, as per-image clipping can settle on different pixel
 * subsets from image to image when the annulus contains bright stars.
 *
 * @param x        aperture center x on the reference image
 * @param y        aperture center y on the reference image
 * @param rBack1   inner background radius the mask was built for
 * @param rBack2   outer background radius the mask was built for
 * @param rejected rejected pixel coordinates on the reference image, see {@link #key(int, int)}
 */
public record BackgroundMask(double x, double y, double rBack1, double rBack2, Set<Long> rejected) {
    public boolean matches(double rb1, double rb2) {
        return rb1 == rBack1 && rb2 == rBack2;
    }

    /**
     * The mask follows the aperture, shifted by the nearest whole pixel offset from the reference center.
     */
    public boolean isRejected(int i, int j, double xc, double yc) {
        return rejected.contains(key(i - (int) Math.round(xc - x), j - (int) Math.round(yc - y)));
    }

    public static long key(int i, int j) {
        return ((long) i << 32) | (j & 0xffffffffL);
    }
}
