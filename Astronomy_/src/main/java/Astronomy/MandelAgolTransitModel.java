package Astronomy;

/**
 * Mandel & Agol (2002) analytic transit model for quadratic limb darkening.
 * <p>
 * Reference: <a href="https://arxiv.org/abs/astro-ph/0210099">Arxiv</a>
 */
public class MandelAgolTransitModel {
    /**
     * Compute the normalized flux for a given phase array.
     *
     * @param phase    Array of phases (0 = mid-transit)
     * @param period   Orbital period (days)
     * @param t0       Transit center (same units as phase)
     * @param duration Transit duration (days)
     * @param depth    Transit depth (fractional, e.g. 0.01 for 1%)
     * @param rprs     Planet/star radius ratio (sqrt(depth) if no limb darkening)
     * @param aRs      Scaled semi-major axis (a/Rs)
     * @param inc      Inclination (degrees)
     * @param u1       Linear limb darkening coefficient
     * @param u2       Quadratic limb darkening coefficient
     * @return Array of normalized fluxes
     */
    public static double[] compute(double[] time, double period, double t0, double duration, double depth, double rprs, double aRs, double inc, double u1, double u2) {
        double[] flux = new double[time.length];
        double incRad = Math.toRadians(inc);
        double b = aRs * Math.cos(incRad); // impact parameter
        double totalDuration = duration;
        double t14 = totalDuration;
        double t12 = t14 / 4.0; // ingress/egress duration (approx)
        for (int i = 0; i < time.length; i++) {
            double phase = ((time[i] - t0 + 0.5 * period) % period) - 0.5 * period;
            double z = Math.abs(phase) * aRs / (0.5 * t14);
            // For simplicity, use a fast approximation: trapezoidal with quadratic limb darkening
            if (Math.abs(phase) > 0.5 * t14) {
                flux[i] = 1.0;
            } else if (Math.abs(phase) < 0.5 * t14 - t12) {
                // Full transit
                flux[i] = 1.0 - depth * (1.0 - u1 / 3.0 - u2 / 6.0);
            } else {
                // Ingress/egress (linear ramp)
                double x = (Math.abs(phase) - (0.5 * t14 - t12)) / t12;
                double limb = 1.0 - u1 * (1.0 - x) - u2 * (1.0 - x) * (1.0 - x);
                flux[i] = 1.0 - depth * limb;
            }
        }
        return flux;
    }
} 