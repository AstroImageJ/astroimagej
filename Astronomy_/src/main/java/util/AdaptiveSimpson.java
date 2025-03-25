package util;

import java.util.function.DoubleUnaryOperator;

/**
 * @see <a href="https://en.wikipedia.org/wiki/Adaptive_Simpson%27s_method">Wikipedia Entry for Adaptive Simpson</a>
 */
public class AdaptiveSimpson {
    /**
     * Maximum allowed recursion depth
     */
    private static final int MAX_DEPTH = 2000;

    /**
     * Compute the Simpson estimate of the integral of f over [a,b].
     */
    private static double simpson(DoubleUnaryOperator f, double a, double b) {
        double c = (a + b) / 2.0;
        return ((b - a) / 6.0) * (f.applyAsDouble(a) + 4 * f.applyAsDouble(c) + f.applyAsDouble(b));
    }

    /**
     * Recursively apply adaptive Simpson's rule to integrate f over [a,b].
     *
     * @param f      the integrand
     * @param a      lower limit
     * @param b      upper limit
     * @param eps    desired accuracy (tolerance)
     * @param whole  Simpson's estimate over [a,b]
     * @param depth  current recursion depth
     * @return       the estimated integral value over [a,b]
     */
    private static double adaptiveSimpson(DoubleUnaryOperator f,
                                          double a, double b, double eps,
                                          double whole, int depth) {
        var c = (a + b) / 2.0;
        var left = simpson(f, a, c);
        var right = simpson(f, c, b);
        var delta = left + right - whole;

        if (depth >= MAX_DEPTH || Math.abs(delta) < 15 * eps) {
            return left + right + delta / 15.0;
        }

        // Recurse on both subintervals, halving eps in each branch.
        return adaptiveSimpson(f, a, c, eps / 2.0, left, depth + 1)
                + adaptiveSimpson(f, c, b, eps / 2.0, right, depth + 1);
    }

    /**
     * Apply Adaptive Simpson's rule to integrate f over [a,b].
     *
     * @param f   the integrand
     * @param a   lower limit
     * @param b   upper limit
     * @param eps desired accuracy (tolerance)
     * @return    the estimated integral value over [a,b]
     */
    public static double integrate(DoubleUnaryOperator f, double a, double b, double eps) {
        var whole = simpson(f, a, b);
        return adaptiveSimpson(f, a, b, eps, whole, 0);
    }

    /**
     * Integrates the area integral for a quadratic Bézier curve defined by (x0, y0),
     * control point (ctrlX, ctrlY), and endpoint (x1, y1) using adaptive Simpson's rule.
     *
     * @param x0    starting x-coordinate
     * @param y0    starting y-coordinate
     * @param ctrlX control point x-coordinate
     * @param ctrlY control point y-coordinate
     * @param x1    ending x-coordinate
     * @param y1    ending y-coordinate
     * @param tol   tolerance for adaptive Simpson (e.g. 1e-6)
     * @return      the signed area contributed by this Bézier segment
     */
    public static double integrateQuadraticBezierAdaptive(double x0, double y0,
                                                          double ctrlX, double ctrlY,
                                                          double x1, double y1,
                                                          double tol) {
        DoubleUnaryOperator f = t -> {
            var u = 1 - t;

            // Position on the quadratic Bézier curve
            var x = u * u * x0 + 2 * u * t * ctrlX + t * t * x1;
            var y = u * u * y0 + 2 * u * t * ctrlY + t * t * y1;

            // Derivatives dx/dt and dy/dt
            var dxdt = -2 * u * x0 + 2 * (1 - 2 * t) * ctrlX + 2 * t * x1;
            var dydt = -2 * u * y0 + 2 * (1 - 2 * t) * ctrlY + 2 * t * y1;

            // Intergrand via
            // parametric Green's Theorem https://en.wikipedia.org/wiki/Green%27s_theorem#Area_calculation
            return x * dydt - y * dxdt;
        };

        return 0.5 * integrate(f, 0.0, 1.0, tol);
    }

    /**
     * Integrates the area integral for a cubic Bézier curve defined by (x0, y0),
     * control points (ctrl1X, ctrl1Y) and (ctrl2X, ctrl2Y), and endpoint (x1, y1)
     * using adaptive Simpson's rule.
     *
     * @param x0      starting x-coordinate
     * @param y0      starting y-coordinate
     * @param ctrl1X  first control point x-coordinate
     * @param ctrl1Y  first control point y-coordinate
     * @param ctrl2X  second control point x-coordinate
     * @param ctrl2Y  second control point y-coordinate
     * @param x1      ending x-coordinate
     * @param y1      ending y-coordinate
     * @param tol     tolerance for adaptive Simpson (e.g. 1e-6)
     * @return        the signed area contributed by this Bézier segment
     */
    public static double integrateCubicBezierAdaptive(double x0, double y0,
                                                      double ctrl1X, double ctrl1Y,
                                                      double ctrl2X, double ctrl2Y,
                                                      double x1, double y1,
                                                      double tol) {
        DoubleUnaryOperator f = t -> {
            var u = 1 - t;

            // Position on the cubic Bézier curve
            var x = u * u * u * x0
                    + 3 * u * u * t * ctrl1X
                    + 3 * u * t * t * ctrl2X
                    + t * t * t * x1;
            var y = u * u * u * y0
                    + 3 * u * u * t * ctrl1Y
                    + 3 * u * t * t * ctrl2Y
                    + t * t * t * y1;

            // Derivatives
            var dxdt = -3 * u * u * x0
                    + 3 * (u * u - 2 * t * u) * ctrl1X
                    + 3 * (2 * t * u - t * t) * ctrl2X
                    + 3 * t * t * x1;
            var dydt = -3 * u * u * y0
                    + 3 * (u * u - 2 * t * u) * ctrl1Y
                    + 3 * (2 * t * u - t * t) * ctrl2Y
                    + 3 * t * t * y1;

            // Intergrand via
            // parametric Green's Theorem https://en.wikipedia.org/wiki/Green%27s_theorem#Area_calculation
            return x * dydt - y * dxdt;
        };

        return 0.5 * integrate(f, 0.0, 1.0, tol);
    }
}
