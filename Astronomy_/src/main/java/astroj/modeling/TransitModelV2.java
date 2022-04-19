package astroj.modeling;

import astroj.IJU;
import ij.astro.logging.AIJLogger;
import ij.astro.types.Pair;

import java.security.InvalidParameterException;
import java.util.Arrays;
import java.util.Vector;
import java.util.function.Function;
import java.util.stream.DoubleStream;
import java.util.stream.IntStream;

import static java.lang.Math.*;

//todo javadoc, rmeove massive comments copied from exofast
public class TransitModelV2 {

    public static double[] modelTransit(double[] bjd, double f0, double inclination, double p, double ar, double tc, double period,
                                        double e, double omega, double u1, double u2, boolean useLonAscNode, double lonAscNode) {
        var m = new ModelOptionals();
        m.setTc(tc);
        m.lonAscNode = useLonAscNode ? lonAscNode : null;
        //todo handle lon, check params - tp, p, etc
        //  something wrong with tp?
        //  check period, does it need to be in bjd?
        var a = transitModelV2(bjd, inclination, ar, tc - period * IJU.getTcPhase(e, omega), period, e, omega, p, u1, u2, f0, m);
        return a;
    }

    /**
     * <a href="https://github.com/jdeast/EXOFASTv2/blob/master/exofast_tran.pro">Based on exofast_tran from EXOFASTv2.</a>
     */
    private static double[] transitModelV2(double[] time, double inc, double ar, double tp, double period, double e, double omega, double p, double u1, double u2, double f0, ModelOptionals optionals) {
        /* NAME:
           EXOFAST_TRAN

           PURPOSE:
              Computes a transit model given a complete set of physical
              parameters

          CALLING SEQUENCE:
              model = EXOFAST_TRAN(time, inc, ar, tp, period, e, omega, p, u1, u2, f0, $
                               rstar=rstar, thermal=thermal, reflect=reflect, $
                               dilute=dilute, tc=tc, q=q,x1=x1,y1=y1,z1=z1,
                               au=au,c=c)

          INPUTS:
             TIME   - The BJD_TDB time of the model to compute. Scalar or
                      array.
             INC    - The inclination of the planetary orbit, in radians
             AR     - a/Rstar, the semi-major axis of the planetary orbit in
                      units of stellar radii
             TP     - The time of periastron, in BJD_TDB
             PERIOD - The planetary orbital period, in days
             E      - The planetary eccentricity
             OMEGA  - The argument of periastron, in radians
             P      - Rp/Rstar, the planetary radius in units of stellar
                      radii
             U1     - The linear limb darkening parameter
             U2     - The quadratic limb darkening parameter
             F0     - The transit baseline level

         OPTIONAL INPUTS:
             RSTAR   - The stellar radius **in AU**. If supplied, it will
                       apply the light travel time correction to the target's
                       barycenter
             THERMAL - The thermal emission contribution from the planet, in ppm.
             REFLECT - The reflected light from the planet, in ppm.
             DILUTE  - The fraction of to basline flux that is due to
                       contaminating sources. DILUTE = F2/(F1+F2), where F1
                       is the flux from the host star, and F2 is the flux
                       from all other sources in the aperture.
             TC      - The time of conjunction, used for calculating the
                       phase of the reflected light component. If not
                       supplied, it is computed from e, omega, period.
             Q       - M1/M2. The mass ratio of the primary to companion. If
                       supplied, the stellar reflex motion is computed and
                       applied.
             X1      - The X motion of the star due to all other bodies in the
                       system, in units of Rstar with the origin at the
                       barycenter. Output from exofast_getb2.pro.
             Y1      - Same as X1, but in the Y direction
             Z1      - Same as X1, but in the Z direction
             AU      - The value of the AU, in solar radii, default is
                       215.094177
             C       - The speed of light, in AU/day (default is computed in
                       TARGET2BJD).
          OUTPUTS:
            MODEL - The transit model as a function of time

          REVISION HISTORY:
            2015 (?) - Written by Jason Eastman (CfA)
            2018/10  - Documented (JDE)
            2019/01/28 - Replaced incorrect arg_present check with n_elements
                       check. Didn't convert to target frame before,
                       as called by exofast_chi2v2.pro
        */

        if (optionals == null) {
            optionals = new ModelOptionals();
        }

        var timeOptions = new TimeOptionals(optionals.q, null, null, optionals.c);

        // If we have the stellar radius, we can convert time to the target's barycentric frame
        var transitBjd = optionals.rStar != null ? bjd2target(time, inc, ar*optionals.rStar(), tp, period, e, omega, timeOptions) : time;

        // Impact parameter
        //todo check - z2/depth, x2/x, y2/y are passed to this
        var te = impactParameter(transitBjd, inc, ar, tp, period, new ImpactOptionals(new double[]{e}, new double[]{omega}, optionals.lonAscNode == null ? new double[]{} : new double[]{optionals.lonAscNode}, optionals.q == null ? null: new double[]{optionals.q()}, 1));
        var z = te.first();
        var depth = te.second();

        // Primary transit
        var modelFlux = new double[time.length];
        Arrays.fill(modelFlux, 1);
        var t = where(depth, d -> d < 0);
        var primary = t.first();
        var secondary = t.second();

        /*if (primary.length > 0) {
            AIJLogger.log(7);
            var x = occultQuadCel(takeIndices(z, primary), u1, u2, p);
            //x.1 = mu1
            for (int i = 0; i < primary.length; i++) {
                modelFlux[primary[i]] = x.first()[i];
            }
        }*/
        if (primary.length > 0) {//todo this is testing code as for some reason this gives a transit
            AIJLogger.log(7);
            var x = occultQuadCel(z, u1, u2, p);
            //x.1 = mu1
            modelFlux = x.first();
        }

        var planetVisible = new double[time.length];
        // calculate the fraction of the planet that is visible for each time
        if (optionals.thermal() != 0 || optionals.reflect() != 0) {
            AIJLogger.log(6);
            Arrays.fill(planetVisible, 1);
            var x = occultQuadCel(Arrays.stream(takeIndices(z, secondary)).map(d -> d/p).toArray(), 0, 0, 1/p);
            //x.1 = mu1
            var c = 0;
            for (int i : secondary) {
                planetVisible[i] = x.first()[c++];
            }
        }

        // thermal emission from planet (isotropic)
        if (optionals.thermal() != 0) {
            AIJLogger.log(5);
            for (int i = 0; i < modelFlux.length; i++) {
                modelFlux[i] += 1e-6 * optionals.thermal()*planetVisible[i];
            }
        }

        // phase-dependent reflection off planet
        if (optionals.reflect() != 0) {
            AIJLogger.log(4);
            var tc0 = optionals.tc();
            if (optionals.tc == null) {
                var phase = IJU.getTcPhase(e, omega);
                tc0 = tp - phase*period;
            }

            // This makes flux=0 during primary transit (Thanks Sam Quinn!)
            for (int i = 0; i < modelFlux.length; i++) {
                modelFlux[i] -= (1e-6*optionals.reflect()/2D)*(cos(2*PI*(transitBjd[i]-tc0)/period)-1)*planetVisible[i];
            }
        }

        // normalization and dilution due to neighboring star
        for (int i = 0; i < modelFlux.length; i++) {
            if (optionals.dilute() != 0) {
                AIJLogger.log(3);
                modelFlux[i] = f0*(modelFlux[i]*(1-optionals.dilute())+optionals.dilute());
            } else {
                modelFlux[i] *= f0;
            }
        }

        // add beaming and ellipsoidal variations
        if (optionals.ellipsoidal() != 0) {
            AIJLogger.log(2);
            var tc0 = optionals.tc();
            if (optionals.tc == null) {
                var phase = IJU.getTcPhase(e, omega);
                tc0 = tp - phase*period;
            }

            for (int i = 0; i < modelFlux.length; i++) {
                modelFlux[i] *= (1 - optionals.ellipsoidal()/1e6*cos(2*PI*(transitBjd[i]-tc0)/(period/2)));
            }
        }

        if (optionals.beam() != 0) {
            AIJLogger.log(1);
            var tc0 = optionals.tc();
            if (optionals.tc == null) {
                var phase = IJU.getTcPhase(e, omega);
                tc0 = tp - phase*period;
            }

            for (int i = 0; i < modelFlux.length; i++) {
                modelFlux[i] += optionals.beam()/1e6 * sin(2*PI*(transitBjd[i]-tc0)/period);
            }
        }

        return modelFlux;
    }

    /**
     * <a href="https://github.com/jdeast/EXOFASTv2/blob/master/exofast_occultquad_cel.pro">Based on exofast_occultquad_cel from EXOFASTv2</a>
     *
     * @return
     */
    private static Pair.GenericPair<double[], double[]> occultQuadCel(double[] z0, double u1, double u2, double p0) {
        /*
        NAME:
        EXOFAST_OCCULTQUAD_CEL

        PURPOSE:
        This routine computes the lightcurve for occultation of a
        quadratically limb-darkened source without microlensing.  Please
        cite Mandel & Agol (2002) and Eastman et al., (2013) if you make use
        of this routine in your research.  Please report errors or bugs to
        agol@astro.washington.edu and jason.eastman@cfa.harvard.edu

        Limb darkening has the form:
        I(r)=[1-u1*(1-sqrt(1-(r/rs)^2))-u2*(1-sqrt(1-(r/rs)^2))^2]/(1-u1/3-u2/6)/pi

        CALLING SEQUENCE:
        exofast_occultquad_cel, z0, u1, u2, p0, muo1, mu0, d=d

        INPUTS:

        z0 - impact parameter in units of rs
        u1 - linear    limb-darkening coefficient (gamma_1 in paper)
        u2 - quadratic limb-darkening coefficient (gamma_2 in paper)
        p0 - occulting star size in units of rs

        OUTPUTS:

        muo1 - fraction of flux at each z0 for a limb-darkened source

        OPTIONAL OUTPUTS:

        mu0  - fraction of flux at each z0 for a uniform source
        d    - The coefficients required to analytically calculate the
               limb darkening parameters (see Eastman et al, 2013). For
               backward compatibility, u1 and u2 are required, but not
               necessary if d is used.

        EXAMPLES:

        Calculate the same geometric transit with two different sets of
        limb darkening coefficients
        p = 0.1
        b = 0.5
        x = (dindgen(300)/299 - 0.5)*2
        z = sqrt(x^2 + b^2)
        u1 = 0.25
        u1 = 0.75
        exofast_occultquad, z, u1, u2, p, muo1, muo, d=d

        MODIFICATION HISTORY

        2002 -- Eric Agol 2002

        2009/04/06 -- Eric Agol (University of Washington) and
                    Jason Eastman (Ohio State University)
        fixed bugs for p > 1 (e.g., secondary eclipses)
        used double precision constants throughout
        replaced rj,rc,rf with ellpic_bulirsch
          faster, more robust integration for specialized case of
          elliptic integral of the third kind
        vectorized
        more efficient case handling
        combined ellk and ellec into ellke for speed
        200x speed improvement over previous IDL code in typical case
        allow negative values of p (anti-transit) to avoid Lucy-Sweeney like bias
        2018/12/12 -- Now uses more stable integrations, 25% faster
        */
        var nz = z0.length;
        var lambdaD = new double[nz];
        var etad = new double[nz];
        var lambdaE = new double[nz];

        var p = abs(p0);
        var z = z0;

        var x1 = new double[nz];
        var x2 = new double[nz];
        var x3 = new double[nz];
        for (int i = 0; i < x1.length; i++) {
            x1[i] = (p - z[i])*(p - z[i]);
            x2[i] = (p + z[i])*(p + z[i]);
            x3[i] = p*p + z[i]*z[i];
        }

        first: { //todo this is bad, mimicing goto
            // Case 1 - the star is unocculted:
            // only consider points with z < 1+p
            // exit if there is no planet (p <= 0)
            var notUsedYet = where(z, d -> (d < 1+p) && (p > 0)).first();
            if (notUsedYet.length == 0) break first;

            // Case 11 - the source is completely occulted
            if (p >= 1) {
                var t = where(takeIndices(z, notUsedYet), d -> d <= p-1);
                var occulted = t.first();
                var notUsed2 = t.second();

                if (occulted.length > 0) {
                    var ndxuse = takeIndices(notUsedYet, occulted);
                    for (int i : ndxuse) {
                        etad[i] = 0.5;
                        lambdaE[i] = 1;
                    }
                }

                if (notUsed2.length == 0) break first;

                notUsedYet = takeIndices(notUsedYet, notUsed2);
            }

            // Case 2, 7, 8 - ingress/egress (uniform disk only)
            var zNotUsed = takeIndices(z, notUsedYet);
            var t = where(zNotUsed, d -> (d >= abs(1-p)) && (d < 1+p));
            var ingressUni = t.first();
            if (ingressUni.length > 0) {
                var ndxuse = takeIndices(notUsedYet, ingressUni);
                var zNdx = takeIndices(z, ndxuse);
                var sqArea = sqAreaTriangle(zNdx, p);
                for (int i = 0; i < ndxuse.length; i++) {
                    var kiteArea2 = sqrt(sqArea[i]);
                    var kap1 = atan2(kiteArea2, (1-p)*(p+1)+z[ndxuse[i]]*z[ndxuse[i]]);
                    var kap0 = atan2(kiteArea2, (p-1)*(p+1)+z[ndxuse[i]]*z[ndxuse[i]]);
                    lambdaE[ndxuse[i]] = (p*p*kap0+kap1 - 0.5*kiteArea2)/PI;
                    etad[ndxuse[i]] = 1./(2.*PI)*(kap1+p*p*(p*p+2.*z[ndxuse[i]]*z[ndxuse[i]])*kap0-0.25*(1.+5.*p*p+z[ndxuse[i]]*z[ndxuse[i]])*kiteArea2);
                }
            }

            // Case 5, 6, 7 - the edge of planet lies at origin of star
            t = where(zNotUsed, d -> d == p);
            var ocltor = t.first();
            var notUsed3 = t.second();
            if (ocltor.length > 0) {
                var ndxuse = takeIndices(notUsedYet, ocltor);
                if (p < 0.5) {
                    // Case 5
                    var q = 2*p;
                    var t1 = ellke(q);
                    var Ek = t1.first();
                    var Kk = t1.second();

                    for (int i : ndxuse) {
                        lambdaD[i] = 1./3.+2./9./PI*(4.*(2.*p*p-1.)*Ek+(1.-4.*p*p)*Kk);
                        // eta_2
                        etad[i] = 3.*p*p*p*p/2.;
                        lambdaE[i] = p*p; // uniform disk
                    }
                } else if (p > 0.5) {
                    // Case 7
                    var q = 0.5 / p;
                    var t1 = ellke(q);
                    var Ek = t1.first();
                    var Kk = t1.second();
                    // lambda_3
                    for (int i : ndxuse) {
                        lambdaD[i] = 1./3.+16.*p/9./PI*(2.*p*p-1.)*Ek-(32.*p*p*p*p-20.*p*p+3.)/9./PI/p*Kk;
                    }
                } else {
                    // Case 6
                    for (int i : ndxuse) {
                        lambdaD[i] = 1./3.-4./PI/9.;
                        etad[i] = 3./32d;
                    }
                }
            }
            if (notUsed3.length == 0) break first;
            notUsedYet = takeIndices(notUsedYet, notUsed3);
            zNotUsed = takeIndices(z, notUsedYet);

            // Case 3, 4, 9, 10 - planet completely inside star
            t = where(zNotUsed, d -> d <= 1-p && p < 1);
            var inside = t.first();
            var notUsed5 = t.second();
            if (inside.length > 0) {
                var ndxuse = takeIndices(notUsedYet, inside);

                for (int i : ndxuse) {
                    // eta_2
                    etad[i] = p*p/2.*(p*p+2.*z[i]*z[i]);

                    // uniform disk
                    lambdaE[i] = p*p;
                }

                // Case 4 - edge of planet hits edge of star
                t = where(takeIndices(z, ndxuse), d -> d == 1-p);
                var edge = t.first();
                var notUsed6 = t.second();
                if (edge.length > 0) {
                    // lambda_5
                    for (int i : takeIndices(ndxuse, edge)) {
                        lambdaD[i] = 2./3./PI*acos(1.-2.*p)-
                                4./9./PI*sqrt(p*(1.-p))*
                                        (3.+2.*p-8.*p*p)-2./3.*bool2int(p > 0.5);
                    }
                    if (notUsed6.length == 0) break first;
                    ndxuse = takeIndices(ndxuse, notUsed6);
                }

                // Case 10 - origin of planet hits origin of star
                t = where(takeIndices(z, ndxuse), d -> d == 0);
                var origin = t.first();
                var notUsed7 = t.second();
                if (origin.length > 0) {
                    // lambda_6
                    for (int i : takeIndices(ndxuse, origin)) {
                        lambdaD[i] = -2./3D*pow((1.-p*p), 1.5);
                    }
                    if (notUsed7.length == 0) break first;
                    ndxuse = takeIndices(ndxuse, notUsed7);
                }

                var onembpr2 = new double[ndxuse.length];
                var onembmr2 = new double[ndxuse.length];
                var fourbr = new double[ndxuse.length];
                var fourbrinv = new double[ndxuse.length];
                var k2 = new double[ndxuse.length];
                var kc = new double[ndxuse.length];
                var onembmr2inv = new double[ndxuse.length];
                var k2inv = new double[ndxuse.length];
                var bmrdpr = new double[ndxuse.length];
                var kc2 = new double[ndxuse.length];
                var mu = new double[ndxuse.length];
                var p_bulirsch = new double[ndxuse.length];
                for (int i = 0; i < ndxuse.length; i++) {
                    onembpr2[i] = (1-z[ndxuse[i]]-p)*(1+z[ndxuse[i]]+p);
                    onembmr2[i] = (p-z[ndxuse[i]]+1)*(1-p+z[ndxuse[i]]);
                    fourbr[i] = 4*z[ndxuse[i]]*p;
                    fourbrinv[i] = 1/fourbr[i];
                    k2[i] = onembpr2[i]*fourbrinv[i]+1;
                    onembmr2inv[i] = 1/onembmr2[i];
                    k2inv[i] = 1/k2[i];
                    kc2[i] = onembpr2[i]*onembmr2inv[i];
                    kc[i] = sqrt(kc2[i]);
                    bmrdpr[i] = (z[ndxuse[i]]-p)/(z[ndxuse[i]]+p);
                    mu[i] = 3*bmrdpr[i]*onembmr2inv[i];
                    p_bulirsch[i]  = bmrdpr[i]*bmrdpr[i]*onembpr2[i]*onembmr2inv[i];
                }

                // Build params for celBulirsch
                var tmp = new double[ndxuse.length];
                var tmp2 = new double[ndxuse.length];
                var tmp3 = new double[ndxuse.length];
                for (int i = 0; i < tmp.length; i++) {
                    tmp[i] = mu[i] + 1;
                    tmp2[i] = p_bulirsch[i] + mu[i];
                    tmp3[i] = 1;
                }
                var t1 = celBulirschVec(k2inv, kc, p_bulirsch, tmp, tmp3, copy(tmp3), tmp2, kc2, new double[ndxuse.length]);
                var Piofk = t1.a1;
                var Eofk = t1.a2;
                //var Em1mKdm = t1.a3;
                for (int i = 0; i < ndxuse.length; i++) {//todo something wrong here?
                    lambdaD[ndxuse[i]] = 2*sqrt(onembmr2[i])*(onembpr2[i]*Piofk[i] -(4-7*p*p-z[ndxuse[i]]*z[ndxuse[i]])*Eofk[i])/(9*PI);
                }
            }

            // Case 2, Case 8 - ingress/egress (with limb darkening)
            var ingress = notUsed5;
            if (ingress.length > 0) {
                var ndxuse = takeIndices(notUsedYet, ingress);
                var onembpr2 = new double[ndxuse.length];
                var onembmr2 = new double[ndxuse.length];
                var fourbr = new double[ndxuse.length];
                var fourbrinv = new double[ndxuse.length];
                var k2 = new double[ndxuse.length];
                var kc = new double[ndxuse.length];
                var kc2 = new double[ndxuse.length];
                for (int i = 0; i < ndxuse.length; i++) {
                    onembpr2[i] = (1-z[ndxuse[i]]-p)*(1+z[ndxuse[i]]+p);
                    onembmr2[i] = (p-z[ndxuse[i]]+1)*(1-p+z[ndxuse[i]]);
                    fourbr[i] = 4*z[ndxuse[i]]*p;
                    fourbrinv[i] = 1/fourbr[i];
                    k2[i] = onembpr2[i]*fourbrinv[i]+1;
                    kc2[i] = -onembpr2[i]*fourbrinv[i];
                    kc[i] = sqrt(kc2[i]);
                }

                // Build params for celBulirsch
                var tmp = new double[ndxuse.length];
                var tmp2 = new double[ndxuse.length];
                for (int i = 0; i < tmp.length; i++) {
                    tmp[i] = (z[ndxuse[i]] - p)*(z[ndxuse[i]] - p)*kc2[i];
                    tmp2[i] = 3 * kc2[i]*(z[ndxuse[i]]-p)*(z[ndxuse[i]]+p);
                }
                var t1 = celBulirschVec(k2, kc, tmp, 0D, 1D, 1D, tmp2, kc2, 0D);
                var Piofk = t1.a1;
                var Eofk = t1.a2;
                var Em1mKdm = t1.a3;
                for (int i = 0; i < ndxuse.length; i++) {
                    lambdaD[ndxuse[i]] = onembmr2[i]*(Piofk[i]+ (-3+6*p*p+2*z[ndxuse[i]]*p)*Em1mKdm[i]-fourbr[i]*Eofk[i])/(9*PI*sqrt(z[ndxuse[i]]*p));
                }
            }
        }

        // Final
        var omega = 1 - u1/3D - u2/6D;

        // avoid Lutz-Kelker bias (negative values of p0 allowed)
        double[] muo1;
        double[] mu0;
        if (p0 > 0) {
            // limb darkened flux
            muo1 = IntStream.range(0, lambdaE.length)
                    .mapToDouble(i -> 1. - ((1. - u1 - 2. * u2) * lambdaE[i] + (u1 + 2. * u2) * (lambdaD[i] + 2. / 3. * bool2int(p > z[i])) + u2 * etad[i]) / omega)
                    .toArray();
            mu0 = Arrays.stream(lambdaE).map(v -> 1 - v).toArray();

            //todo d transpose

        } else {
            // limb darkened flux
            muo1 = IntStream.range(0, lambdaE.length)
                    .mapToDouble(i -> 1. + ((1. - u1 - 2. * u2) * lambdaE[i] + (u1 + 2. * u2) * (lambdaD[i] + 2. / 3. * bool2int(p > z[i])) + u2 * etad[i]) / omega)
                    .toArray();
            mu0 = Arrays.stream(lambdaE).map(v -> 1 + v).toArray();

            //todo d transpose
        }
        return new Pair.GenericPair<>(muo1, mu0);
    }

    private static TripleDoubleArray celBulirschVec(double[] k2, double[] kc, double[] p, double a1, double a2, double a3, double[] b1, double[] b2, double b3) {
        var a1A = new double[k2.length];
        var a2A = new double[k2.length];
        var a3A = new double[k2.length];
        var b3A = new double[k2.length];
        Arrays.fill(a1A, a1);
        Arrays.fill(a2A, a2);
        Arrays.fill(a3A, a3);
        Arrays.fill(b3A, b3);
        return celBulirschVec(k2, kc, p, a1A, a2A, a3A, b1, b2, b3A);
    }

    /**
     * <a href="https://github.com/jdeast/EXOFASTv2/blob/master/cel_bulirsch_vec.pro">https://github.com/jdeast/EXOFASTv2/blob/master/cel_bulirsch_vec.pro</a>
     */
    private static TripleDoubleArray celBulirschVec(double[] k2, double[] kc, double[] p, double[] a1, double[] a2, double[] a3, double[] b1, double[] b2, double[] b3) {
        // This assumes the first value of a and b uses p,m the rest have p=1
        var ca = Arrays.stream(k2).map(d -> sqrt(d * 2.2e-16)).toArray();

        // Avoid undefined k2=1 case
        var t = where(k2, d -> d == 1);
        var indx = t.first();
        t = where(kc, d -> d == 0);
        indx = concatWithArray(indx, t.first());
        for (int i : indx) {
            kc[i] = 2.22e-16;
        }

        // Init. values
        var ee = copy(kc);
        var m = new double[kc.length];
        Arrays.fill(m, 1);

        t = where(p, d -> d >= 0);
        var pos = t.first();
        var neg = t.second();

        var pinv = new double[k2.length];
        for (int i : pos) {
            p[i] = sqrt(p[i]);
            pinv[i] = 1/p[i];
            b1[i] *= pinv[i];
        }
        for (int i : neg) {
            var q = k2[i];
            var g = 1-p[i];
            var f = g - k2[i];
            q *= (b1[i]-a1[i]*p[i]);
            var ginv = 1/g;
            p[i] = sqrt(f*ginv);
            a1[i] = (a1[i]-b1[i])*ginv;
            pinv[i] = 1/p[i];
            b1[i] = -q*ginv*ginv*pinv[i]+a1[i]*p[i];
        }

        // Compute recursion
        var f1 = copy(a1);
        // First compute the first integral with p
        var g = new double[a1.length];
        for (int i = 0; i < a1.length; i++) {
            a1[i] += b1[i] * pinv[i];
            g[i] = ee[i] * pinv[i];
            b1[i] += f1[i]*g[i];
            b1[i] += b1[i];
            p[i] += g[i];
        }

        // Next, compute the remainder with p = 1:
        var p1 = new double[ee.length];
        Arrays.fill(p1, 1);
        var g1 = copy(ee);
        var f2 = copy(a2);
        var f3 = copy(a3);
        for (int i = 0; i < f3.length; i++) {
            a2[i] += b2[i];
            b2[i] += f2[i]*g1[i];
            b2[i] *= 2;
            a3[i] += b3[i];
            b3[i] += f3[i]*g1[i];
            b3[i] *= 2;
            p1[i] += g1[i];
            g[i] = m[i];
            m[i] += kc[i];
        }

        var iter = 0;
        var itmax = 50;

        class Holder {
            double[] kc;
            double[] g;
            double[] ca;

            public Holder(double[] kc, double[] g, double[] ca) {
                this.kc = kc;
                this.g = g;
                this.ca = ca;
            }
        }

        var h = new Holder(kc, g, ca);

        while (iter < itmax && IntStream.range(0, kc.length)
                .map(i -> bool2int(abs(h.g[i] - h.kc[i]) > h.g[i]*h.ca[i])).anyMatch(i -> i == 1)) {
            kc = Arrays.stream(ee).map(Math::sqrt).map(d -> d + d).toArray();
            f1 = copy(a1); f2=copy(a2); f3=copy(a3);
            for (int i = 0; i < ee.length; i++) {
                ee[i] = kc[i] * m[i];
                pinv[i] = 1/p[i];
                var pinv1 = 1/p1[i];
                a1[i] += b1[i]*pinv[i];
                a2[i] += b2[i]*pinv1;
                a3[i] += b3[i]*pinv1;
                g[i] = ee[i]*pinv[i];
                g1[i]= ee[i]*pinv1;
                b1[i] += f1[i]*g[i];
                b2[i] += f2[i]*g1[i];
                b3[i] += f3[i]*g1[i];
                b1[i] += b1[i];
                b2[i] += b2[i];
                b3[i] += b3[i];
                p[i]  += g[i];
                p1[i] += g1[i];
                g[i] = m[i];
                m[i] += kc[i];
            }

            h.ca = ca;
            h.g = g;
            h.kc = kc;

            iter++;
        }

        for (int i = 0; i < f1.length; i++) {
            double v = m[i] * (m[i] + p1[i]);
            f1[i] = 0.5*PI*(a1[i]*m[i]+b1[i])/(m[i] * (m[i] + p[i]));
            f2[i] = 0.5*PI*(a2[i]*m[i]+b2[i])/v;
            f3[i] = 0.5*PI*(a3[i]*m[i]+b3[i])/v;
        }

        return new TripleDoubleArray(f1, f2, f3);
    }

    static int[] concatWithArray(int[] array1, int[] array2) {
        int[] result = Arrays.copyOf(array1, array1.length + array2.length);
        System.arraycopy(array2, 0, result, array1.length, array2.length);
        return result;
    }

    private static double[] copy(double[] a) {//todo needed elsehwere?
        return Arrays.copyOf(a, a.length);
    }

    private record TripleDoubleArray(double[] a1, double[] a2, double[] a3) {}

    private static int bool2int(boolean b) {
        return b ? 1 : 0;
    }

    /**
     * <a href="https://github.com/jdeast/EXOFASTv2/blob/master/ellke.pro">https://github.com/jdeast/EXOFASTv2/blob/master/ellke.pro</a>
     */
    private static Pair.DoublePair ellke(double k) {
        /*
        NAME:
        ELLKE

        PURPOSE:
        Computes Hasting's polynomial approximation for the complete
        elliptic integral of the first (ek) and second (kk) kind. Combines
        the calculation of both so as not to duplicate the expensive
        calculation of alog10(1-k^2).

        CALLING SEQUENCE:
        ellke, k, ek, kk

        INPUTS:

        k - The elliptic modulus.

        OUTPUTS:

        ek - The elliptic integral of the first kind
        kk - The elliptic integral of the second kind

        MODIFICATION HISTORY

        2009/04/06 -- Written by Jason Eastman (Ohio State University)
        */
        var m1=1.-k*k;
        var logm1 = log(m1);

        var a1=0.44325141463;
        var a2=0.06260601220;
        var a3=0.04757383546;
        var a4=0.01736506451;
        var b1=0.24998368310;
        var b2=0.09200180037;
        var b3=0.04069697526;
        var b4=0.00526449639;
        var ee1=1.+m1*(a1+m1*(a2+m1*(a3+m1*a4)));
        var ee2=m1*(b1+m1*(b2+m1*(b3+m1*b4)))*(-logm1);
        var ek = ee1+ee2;

        var a0=1.38629436112;
        a1=0.09666344259;
        a2=0.03590092383;
        a3=0.03742563713;
        a4=0.01451196212;
        var b0=0.5;
        b1=0.12498593597;
        b2=0.06880248576;
        b3=0.03328355346;
        b4=0.00441787012;
        var ek1=a0+m1*(a1+m1*(a2+m1*(a3+m1*a4)));
        var ek2=(b0+m1*(b1+m1*(b2+m1*(b3+m1*b4))))*logm1;
        var kk = ek1-ek2;

        return new Pair.DoublePair(ek, kk);
    }

    //todo check with IDl if this is correct implementation of 2-param IDL atan
    // https://www.l3harrisgeospatial.com/docs/atan.html
    private static double atan2(double x, double y) {
        return Math.atan(y/x);
    }

    /**
     * <a href="https://github.com/jdeast/EXOFASTv2/blob/master/sqarea_triangle.pro">Based on sqarea_triangle from EXOFASTv2</a>
     */
    private static double[] sqAreaTriangle(double[] z0, double p0) {
        /*Computes sixteen times the square of the area of a triangle
        with sides 1, z0 and p0 using Kahan method (Goldberg 1991).*/

        var sqArea = new double[z0.length];
        // There are six cases to consider
        for (int i = 0; i < z0.length; i++) {
            if ((p0 <= z0[i]) && (z0[i] <= 1)) {
                sqArea[i] = (p0+(z0[i]+1))*(1-(p0-z0[i]))*(1+(p0-z0[i]))*(p0+(z0[i]-1));
            } else if ((z0[i] <= p0) && (p0 <= 1)) {
                sqArea[i] = (z0[i]+(p0+1))*(1-(z0[i]-p0))*(1+(z0[i]-p0))*(z0[i]+(p0-1));
            } else if ((p0 <= 1) && (1 <= z0[i])) {
                sqArea[i] = (p0+(1+z0[i]))*(z0[i]-(p0-1))*(z0[i]+(p0-1))*(p0+(1-z0[i]));
            } else if ((z0[i] <= 1) && (1 <= p0)) {
                sqArea[i] = (z0[i]+(1+p0))*(p0-(z0[i]-1))*(p0+(z0[i]-1))*(z0[i]+(1-p0));
            } else if ((1 <= p0) && (p0 <= z0[i])) {
                sqArea[i] = (1+(p0+z0[i]))*(z0[i]-(1-p0))*(z0[i]+(1-p0))*(1+(p0-z0[i]));
            } else if (1 <= z0[i] && z0[i] <= p0) {
                sqArea[i] = (1+(z0[i]+p0))*(p0-(1-z0[i]))*(p0+(1-z0[i]))*(1+(z0[i]-p0));
            }
        }

        return sqArea;
    }

    private static double[] takeIndices(double[] ar, int[] indices) {
        var out = new Vector<Double>();
        for (int index : indices) {
            out.add(ar[index]);
        }

        return out.stream().mapToDouble(Double::doubleValue).toArray();
    }

    private static int[] takeIndices(int[] ar, int[] indices) {
        var out = new Vector<Integer>();
        for (int index : indices) {
            out.add(ar[index]);
        }

        return out.stream().mapToInt(Integer::intValue).toArray();
    }

    private static Pair.GenericPair<int[], int[]> where(double[] a, Function<Double, Boolean> test) {
        var primary = new Vector<Integer>();
        var secondary = new Vector<Integer>();

        for (int i = 0; i < a.length; i++) {
            if (test.apply(a[i])) {
                primary.add(i);
            } else {
                secondary.add(i);
            }
        }

        primary.trimToSize();
        secondary.trimToSize();

        return new Pair.GenericPair<>(primary.stream().mapToInt(Integer::intValue).toArray(),
                secondary.stream().mapToInt(Integer::intValue).toArray());
    }

    /**
     * <a href="https://github.com/jdeast/EXOFASTv2/blob/master/exofast_getb2.pro">Based on exofast_getb2 from EXOFASTv2</a>
     */
    private static Pair.GenericPair<double[], double[]> impactParameter(double[] bjd, double inc, double a, double tPeriastron,
                                          double period, ImpactOptionals optionals) {
        return impactParameter(bjd, new double[]{inc}, new double[]{a}, new double[]{tPeriastron}, new double[]{period}, optionals);
    }

    /**
     * <a href="https://github.com/jdeast/EXOFASTv2/blob/master/exofast_getb2.pro">Based on exofast_getb2 from EXOFASTv2</a>
     */
    private static Pair.GenericPair<double[], double[]> impactParameter(double[] bjd, double[] inc, double[] a, double[] tPeriastron,
                                                    double[] p, ImpactOptionals optionals
                                          /*double x0, double y0, double z0,
                                          double x1, double y1, double z1,
                                          double x2, double y2, double z2,*/) {
        /*
        NAME:
        EXOFAST_GETB2

        PURPOSE:
        This function returns the impact parameter as a function of BJD in
        the target barycentric frame, given orbital elements of many
        planets, assuming keplerian orbits.  Optionally, the 3-space
        barycentric coordinates of the star (x1,y1,z1) and planets
        (x2,y2,z2) or the stellar coordinates of the planets (x0,y0,z0)
        can be returned.

        CALLING SEQUENCE:
        result = getb(jds, i=i,a=a,tperiastron=tperiastron,period=p,$
                     e=e,omega=omega, x=x, y=y, z=z)

        INPUTS:
        bjd         - Barycentric Julians dates for desired impact
                     parameters (ideally in the target's
                     barycentric frame see BJD2TARGET)
        i           - an NPLANETS array of planetary inclinations (radians)
        a           - an NPLANETS array of semi-major axis, a = a1+a2 (in
                     units of R_*). That is, the semi-major axis should be calculated
                     from Kepler's equation: a/rstar = G(M+m)P^2/(4pi^2rstar).
                     Using G = 2942.71377 R_sun^3/(m_sun*day^2), masses
                     in solar masses, period in days, and rstar in solar
                     radii will give the appropriate input.
        tperiastron - an NPLANETS array of periastron passage times (same
                     units as BJD)
        Period      - an NPLANETS array of Period of orbit (days)

        OPTIONAL INPUTS:
        e           - an NPLANETS array of eccentricities (0 if not specified)
        omega       - an NPLANETS array of arguments of periastron of the star's orbit, in radians
                     -- omega_* is typically quoted from RV
                     -- assumed to be pi/2 if e not specified
                     -- omega_* = omega_planet + PI
        lonascnode  - an NPLANETS array of Longitudes of the ascending node
                     (radians). Assumed to be PI if not specified.
        Q           - an NPLANETS array of mass ratios of the primary to secondary
                     (M1/M2). If not specified, the mass ratio is assumed
                     to be infinite. The returned coordinates are the
                     motion of the companion with respect to the primary.

        OUTPUTS:
        result     - the impact parameter as a function of BJD, in units
                     of R_*.

        OPTIONAL OUTPUTS:
        x,y,z - Arrays of the cartesian coordinates of the planet at
                each BJD, in the units of R_*.
                  +X is right
                  +Y is up
                  +Z is out of the page (primary transit)

        MODIFICATION HISTORY
        2009/05/01 -- Jason Eastman (Ohio State University)
        2017/04/12 -- Add optional mass ratio keyword
        2018/07/06 -- Now properly handles 2D time array (for long cadence)
        2018/11/13 -- Now properly handles a scalar time
        2019/02/05 -- Fix sign conventions
        */

        var sz = 1; // We only handle 1 set of times, todo simplify
        var nTimes = 0;
        var nInterp = 0;
        if (sz == 1) {
            nInterp = 1;
            nTimes = bjd.length;
        } /*else if (sz == 2) {

        } else if (sz == 0) {

        }*/

        var nPlanets = inc.length;
        if (optionals == null) optionals = new ImpactOptionals(nPlanets);

        var x1 = new double[nTimes]/*[nInterp]*/;
        var y1 = new double[nTimes]/*[nInterp]*/;
        var z1 = new double[nTimes]/*[nInterp]*/;

        var x2 = new double[nPlanets][nTimes]/*[nInterp]*/;
        var y2 = new double[nPlanets][nTimes]/*[nInterp]*/;
        var z2 = new double[nPlanets][nTimes]/*[nInterp]*/;

        var x0 = new double[nPlanets][nTimes]/*[nInterp]*/;
        var y0 = new double[nPlanets][nTimes]/*[nInterp]*/;
        var z0 = new double[nPlanets][nTimes]/*[nInterp]*/;

        var a2 = new double[nPlanets];
        var a1 = new double[nPlanets];

        for (int i = 0; i < a1.length; i++) {
            if (Double.isFinite(optionals.q()[i])) {
                a2[i] = a[i]*optionals.q()[i]/(1+optionals.q()[i]);
                a1[i] = a2[i]/(optionals.q()[i]);
            } else {
                a2[i] = a[i];
            }
        }

        for (int i = 0; i < nPlanets; i++) {
            // calculate the mean anomaly corresponding to each observed time
            int finalI = i;
            var meanAnom = Arrays.stream(bjd).map(t -> (2*PI*(1 + (t - tPeriastron[finalI])/p[finalI])) % (2*PI));

            // if eccentricity is given, integrate the orbit
            ImpactOptionals finalOptionals = optionals;
            var trueAnon = optionals.e()[i] != 0 ?
                    meanAnom.map(m -> IJU.solveKeplerEq(m, finalOptionals.e()[finalI]))
                            .map(m -> 2*atan(sqrt((1 + finalOptionals.e()[finalI])/(1 - finalOptionals.e()[finalI]))*tan(0.5*m))).toArray()
                    : meanAnom.toArray();

            // Calculate the corresponding (x,y) coordinates of planet in barycentric coordinates
            // Note sign flip to accound for using omega_* instead of omega_P
            var r2 = Arrays.stream(trueAnon).map(m -> -a2[finalI]*(1-finalOptionals.e()[finalI]*finalOptionals.e()[finalI])/(1+finalOptionals.e()[finalI]*cos(m))).toArray();

            // Planet path as seen from observer
            x2[i] = new double[r2.length];
            for (int j = 0; j < r2.length; j++) {
                x2[i][j] = r2[j]*cos(trueAnon[j]+optionals.omega()[i]);
                var tmp = x2[i][j];
                y2[i][j] = tmp * cos(inc[i]);
                z2[i][j] = tmp * sin(inc[i]);

                // Rotate by the longitude of ascending node
                // For transits, it is not constrained, so assume omega=0
                if (optionals.lonAscNode().length == nPlanets) {
                    var xOld = x2[i][j];
                    var yOld = y2[i][j];
                    x2[i][j] = xOld*cos(optionals.lonAscNode()[i]) - yOld*sin(optionals.lonAscNode()[i]);
                    y2[i][j] = xOld*sin(optionals.lonAscNode()[i]) + yOld*cos(optionals.lonAscNode()[i]);
                }
            }

            var r1 = Arrays.stream(trueAnon).map(m -> -a1[finalI]*(1-finalOptionals.e()[finalI]*finalOptionals.e()[finalI])/(1+finalOptionals.e()[finalI]*cos(m))).toArray();

            // Stellar path as seen by observer
            var x1Tmp = IntStream.range(0, r1.length).mapToDouble(j -> r1[j] * cos(trueAnon[j] + finalOptionals.omega()[finalI])).toArray();
            var tmp = IntStream.range(0, r1.length).mapToDouble(j -> r1[j] * sin(trueAnon[j] + finalOptionals.omega()[finalI])).toArray();
            var y1Tmp = Arrays.stream(tmp).map(d -> d * cos(inc[finalI])).toArray();
            IntStream.range(0, r1.length).forEach(j -> z1[j] += tmp[j] * inc[finalI]);

            // Rotate the longitude by the ascending node
            // For transits, it is not constrained, so we assume Omega=pi
            for (int j = 0; j < r2.length; j++) {
                // Rotate by the longitude of ascending node
                // For transits, it is not constrained, so assume omega=0
                if (optionals.lonAscNode().length == nPlanets) {
                    x1[j] += x1Tmp[j]*cos(optionals.lonAscNode()[i]) - y1Tmp[j]*sin(optionals.lonAscNode()[i]);
                    y1[j] += x1Tmp[j]*sin(optionals.lonAscNode()[i]) + y1Tmp[j]*cos(optionals.lonAscNode()[i]);
                } else {
                    x1[j] += x1Tmp[j];
                    y1[j] += y1Tmp[j];
                }
            }
        }

        // now convert to stellar frame (which is relevant for transits)
        for (int i = 0; i < nPlanets; i++) {
            for (int j = 0; j < x2[i].length; j++) {//todo x,y,z1s are 0s
                x0[i][j] = x2[i][j] - x1[j];
                y0[i][j] = y2[i][j] - y1[j];
                z0[i][j] = z2[i][j] - z1[j];
            }
        }

        var b = new double[nPlanets][nTimes];
        for (int planet = 0; planet < nPlanets; planet++) {
            for (int time = 0; time < nTimes; time++) {
                b[planet][time] = sqrt(x0[planet][time]*x0[planet][time] + y0[planet][time]*y0[planet][time]);
            }
        }

        return new Pair.GenericPair<>(b[0], z2[0]);
    }

    /**
     * <a href="https://github.com/jdeast/EXOFASTv2/blob/master/bjd2target.pro">https://github.com/jdeast/EXOFASTv2/blob/master/bjd2target.pro</a>
     */
    private static double[] bjd2target(double[] bjdTdb, double inc, double a, double tp, double period, double e, double omega, TimeOptionals optionals) {
        /*
        NAME:
        BJD2TARGET
        PURPOSE:
        Iteratively calls TARGET2BJD to convert a BJD in Barycentric
        Dynamical Time (BJD_TDB) to a BJD in the target barycenter
        time (BJD_TARGET) within TOL days.

        DESCRIPTION:
        The opposite of TARGET2BJD see description there.

        INPUTS:
        BJD_TDB     - A scalar or array of BJDs in TDB. Must be double
                     precision.
        INCLINATION - The inclination of the orbit
        A           - The semi-major axis of the orbit (AU)
        TP          - The time of periastron of the orbit (BJD_TARGET)
        PERIOD      - The period of the orbit (days)
        E           - Eccentricity of the orbit
        OMEGA       - Argument of Periastron of the orbit (radians)

        OPTIONAL INPUTS:
        Q          - The mass ratio of the targets (M1/M2). If not
                    specified, an infinite mass ratio is assumed (M1 is
                    stationary at the barycenter) (8 ms effect for Hot
                    Jupiters).
        TOL        - The tolerance, in days. The iterative procedure will
                    stop after the worst-case agreement is better than this.
                    Default = 1d-8 (1 ms).

        OPTIONAL KEYWORDS:
        PRIMARY    - If set, the information comes from the position of
                    the primary (as in RV), and therefore the correction
                    will be the light travel time from the center of the
                    primary to the Barycenter -- analagous to the
                    difference between HJD and BJD in our solar system
                    (only ~8 ms for Hot Jupiters, but increasing with a).
                    Otherwise, the correction will be the light
                    travel time from the smaller body to the barycenter
                    (as in transits) -- analagous to the difference
                    between JD and BJD in the Solar System.

                    NOTE: if Q is not specified and PRIMARY is, no
                    correction is applied.

        OUTPUTS:
        BJD_TARGET - The time as it would flow in the Barycenter of the target.

        LIMITATIONS:
        We ignore the distance to the object (plane parallel waves), which
        should have a similar effect as the distance plays in the BJD
        correction (< 1 ms). We also ignore the systemic velocity, which
        will compress/expand the period by a factor gamma/c.

        REVISION HISTORY:
        2011/06: Written by Jason Eastman (OSU)
        */

        var bjdTarget = Arrays.copyOf(bjdTdb, bjdTdb.length);

        var nIter = 0;

        while (nIter <= 100) {
            var targetNew = target2bjd(bjdTarget, inc, a, tp, period, e, omega, optionals);

            var diff = IntStream.range(0, bjdTarget.length).mapToDouble(i -> bjdTdb[i] - targetNew[i]).toArray();
            IntStream.range(0, bjdTarget.length).forEach(i -> bjdTarget[i] += diff[i]);

            if (Arrays.stream(diff).map(Math::abs).max().orElse(Double.POSITIVE_INFINITY) < optionals.tol()) return bjdTarget;

            nIter++;
        }

        throw new InvalidParameterException("Not converging; this is a rare bug usually associated with poorly " +
                "constrained parameters. Try again or consider imposing some priors for poorly constrained parameters. " +
                "Especially if you have parallel tempering enabled, you should have loose, uniform priors on Period and Tc.");
    }

    /**
     * <a href="https://github.com/jdeast/EXOFASTv2/blob/master/target2bjd.pro">Adapted from target2bjd from EXOFASTv2</a>
     */
    public static double[] target2bjd(double[] bjdTarget, double inc, double a, double tp, double period, double e, double omega, TimeOptionals optionals) {
        /*
        NAME:
        TARGET2BJD
        PURPOSE:
        Converts a BJD in the target barycenter time to BJD in Barycentric
        Dynamical Time (BJD_TDB).

        DESCRIPTION:
        Corrects for the Roemer delay (light travel time) in
        the target system (~30 seconds for typical Hot Jupiters). Most
        importantly, this will naturally make primary, secondary, RV
        observations, and phase variations self-consistent.

        Additionally, for a typical Hot Jupiter (3 hour transit in a 3 day
        orbit), the ingress is delayed by ~0.15 seconds relative to the mid
        transit time. For circular-orbits, the delay between mid transit
        time and ingress is:

        dt = a/c*(1-cos((sqrt((R_*+R_P)^2 - b^2)/a))

        Falling for larger semi-major axis.

        INPUTS:
        BJD_TARGET  - A scalar or array of BJDs in target time. Must be
                     double precision.
        INCLINATION - The inclination of the orbit
        A           - The semi-major axis of the orbit (AU)
        TP          - The time of periastron of the orbit (BJD_TARGET)
        PERIOD      - The period of the orbit (days)
        E           - Eccentricity of the orbit
        OMEGA       - Argument of Periastron of the stellar orbit (radians)

        OPTIONAL INPUTS:
        Q          - The mass ratio of the targets (M1/M2). If not
                    specified, an infinite mass ratio is assumed (M1
                    stationary) (8 ms effect for Hot Jupiters).
        C          - The speed of light, in AU/day. If not given,
                    initialized to 173.144483

        OPTIONAL KEYWORDS:
        PRIMARY    - If set, the information comes from the position of
                    the primary (as in RV), and therefore the correction
                    will be the light travel time from the center of the
                    primary to the Barycenter -- analagous to the
                    difference between HJD and BJD in our solar system
                    (only ~8 ms for Hot Jupiters, but increasing with a).
                    Otherwise, the correction will be the light
                    travel time from the smaller body to the barycenter
                    (as in transits) -- analagous to the difference
                    between JD and BJD in the Solar System.

                    NOTE: if Q is not specified and PRIMARY is, no
                    correction is applied.

        OUTPUTS:
        BJD_TDB    - The time as it would flow in the Barycenter of the
                    solar system (BJD_TDB).

        LIMITATIONS:
        We ignore the distance to the object (plane parallel waves), which
        should have a similar effect as the distance plays in the BJD
        correction (< 1 ms). We also ignore the systemic velocity, which
        will compress/expand the period by a factor gamma/c.

        REVISION HISTORY:
        2011/06: Written by Jason Eastman (OSU)
        */

        // No correction necessary, already in the SSB frame
        if (!Double.isFinite(optionals.q()) && optionals.primary()) return bjdTarget;

        var meanAnom = Arrays.stream(bjdTarget).map(targetTime -> (2*PI*(1 + targetTime - tp)/period) % (2*PI)).toArray();

        //todo check kepler is updated
        var eccAnom = Arrays.stream(meanAnom).map(m -> IJU.solveKeplerEq(m, e));

        var trueAnom = eccAnom.map(m -> 2* atan(sqrt((1 + e)/(1 - e))* tan(0.5*m))).toArray();

        // Displacement from barycenter
        var factor = 0D;
        if (Double.isFinite(optionals.q())) {
            if (optionals.primary()) {
                factor = 1D/(1D+optionals.q()); // a*factor = a1
            } else {
                factor = optionals.q() / (1D+optionals.q()); // a*factor = a2
            }
        } else {
            factor = 1; //infinite mass ratio, a1=0, a2=a
        }

        // Distance from barycenter to target
        double finalFactor = factor;
        var r = Arrays.stream(trueAnom).map(ta -> a*(1-e*e)/(1+e*cos(ta))* finalFactor).toArray();

        // Rotate orbit by omega
        var om = optionals.primary() ? omega + PI : omega;

        var out = new double[r.length];
        for (int i = 0; i < r.length; i++) {
            var z = r[i]*sin(trueAnom[i]+om)*sin(inc);
            out[i] = bjdTarget[i] - z/optionals.c();
        }

        return out;
    }

    public static final class ImpactOptionals {
        private double[] e;
        private double[] omega;
        private double[] lonAscNode;
        private double[] q;
        private final int size;

        public ImpactOptionals(int size) {
            this(null, null, null, null, size);
        }

        public ImpactOptionals(double[] e, double[] omega, double[] lonAscNode, double[] q, int size) {
            this.e = e;
            this.omega = omega;
            this.lonAscNode = lonAscNode;
            this.q = q;
            this.size = size;
        }

        public double[] e() {
            return e == null ? new double[size] : e;
        }

        public double[] omega() {
            return omega == null ? DoubleStream.generate(() -> PI/2d).limit(size).toArray() : omega;
        }

        public double[] lonAscNode() {
            return lonAscNode == null ? new double[]{} : lonAscNode;
        }

        public double[] q() {
            return q == null ? DoubleStream.generate(() -> Double.POSITIVE_INFINITY).limit(size).toArray() : q;
        }

        public void setE(double[] e) {
            this.e = e;
        }

        public void setOmega(double[] omega) {
            this.omega = omega;
        }

        public void setLonAscNode(double[] lonAscNode) {
            this.lonAscNode = lonAscNode;
        }

        public void setQ(double[] q) {
            this.q = q;
        }

        @Override
        public String toString() {
            return "ImpactOptionals[" +
                    "e=" + Arrays.toString(e) + ", " +
                    "omega=" + Arrays.toString(omega) + ", " +
                    "lonAscNode=" + Arrays.toString(lonAscNode) + ", " +
                    "q=" + Arrays.toString(q) + ']';
        }
    }

    public static final class TimeOptionals {
        private Double q;
        private Double tol;
        private Boolean primary;
        private Double c;

        public TimeOptionals() {
            this(null, null, null, null);
        }

        public TimeOptionals(Double q, Double tol, Boolean primary, Double c) {
            this.q = q;
            this.tol = tol;
            this.primary = primary;
            this.c = c;
        }

        public double q() {
            return q == null ? Double.POSITIVE_INFINITY : q;
        }

        public double tol() {
            return tol == null ? 1e-8 : tol;
        }

        public boolean primary() {
            return primary != null && primary;
        }

        public double c() {
            return c == null ? 173.144483 : c;
        }

        public void setQ(Double q) {
            this.q = q;
        }

        public void setTol(Double tol) {
            this.tol = tol;
        }

        public void setPrimary(Boolean primary) {
            this.primary = primary;
        }

        public void setC(Double c) {
            this.c = c;
        }

        @Override
        public String toString() {
            return "TimeOptionals[" +
                    "q=" + q + ", " +
                    "tol=" + tol + ", " +
                    "primary=" + primary + ", " +
                    "c=" + c + ']';
        }
    }

    public static final class ModelOptionals {
        private Double rStar;
        private Double thermal;
        private Double reflect;
        private Double dilute;
        private Double tc;
        private Double q;
        private Double x1;
        private Double y1;
        private Double z1;
        private Double au;
        private Double c;

        //todo check if these are used, check x,y,z as they don't seem to be used
        private Double beam;

        private Double ellipsoidal;
        private Double phaseShift;
        private Double lonAscNode;

        /**
         * @param rStar       The stellar radius **in AU**. If supplied, it will apply the light travel time correction to the target's barycenter
         * @param thermal     The thermal emission contribution from the planet, in ppm.
         * @param reflect     The reflected light from the planet, in ppm.
         * @param dilute      The fraction of to basline flux that is due to contaminating sources. DILUTE = F2/(F1+F2), where F1 is the flux from the host star, and F2 is the flux from all other sources in the aperture.
         * @param tc          The time of conjunction, used for calculating the phase of the reflected light component. If not supplied, it is computed from e, omega, period.
         * @param q           M1/M2. The mass ratio of the primary to companion. If supplied, the stellar reflex motion is computed and applied.
         * @param x1          The X motion of the star due to all other bodies in the system, in units of Rstar with the origin at the barycenter. Output from exofast_getb2.pro.
         * @param y1          Same as X1, but in the Y direction
         * @param z1          Same as X1, but in the Z direction
         * @param au          The value of the AU, in solar radii, default is 215.094177
         * @param c           The speed of light, in AU/day (default is computed in TARGET2BJD). //todo link method
         * @param beam
         * @param ellipsoidal
         * @param phaseShift
         * @param lonAscNode
         */
        public ModelOptionals(Double rStar, Double thermal, Double reflect, Double dilute, Double tc, Double q, Double x1, Double y1, Double z1, Double au, Double c, Double beam, Double ellipsoidal, Double phaseShift, Double lonAscNode) {
            this.rStar = rStar;
            this.thermal = thermal;
            this.reflect = reflect;
            this.dilute = dilute;
            this.tc = tc;
            this.q = q;
            this.x1 = x1;
            this.y1 = y1;
            this.z1 = z1;
            this.au = au;
            this.c = c;
            this.beam = beam;
            this.ellipsoidal = ellipsoidal;
            this.phaseShift = phaseShift;
            this.lonAscNode = lonAscNode;
        }

        public ModelOptionals() {
            this(null, null, null, null, null, null, null, null, null, null, null, null, null, null, null);
        }

        public double rStar() {
            return rStar == null ? 0 : rStar;
        }

        public double thermal() {
            return thermal == null ? 0 : thermal;
        }

        public double reflect() {
            return reflect == null ? 0 : reflect;
        }

        public double dilute() {
            return dilute == null ? 0 : dilute;
        }

        public double tc() {
            return tc == null ? 0 : tc;
        }

        public double beam() {
            return beam == null ? 0 : beam;
        }

        public void setBeam(Double beam) {
            this.beam = beam;
        }

        public double ellipsoidal() {
            return ellipsoidal == null ? 0 : ellipsoidal;
        }

        public void setEllipsoidal(Double ellipsoidal) {
            this.ellipsoidal = ellipsoidal;
        }

        public double phaseShift() {
            return phaseShift == null ? 0 : phaseShift;
        }

        public void setPhaseShift(Double phaseShift) {
            this.phaseShift = phaseShift;
        }

        public double q() {
            return q == null ? 0 : q;
        }

        public double x1() {
            return x1 == null ? 0 : x1;
        }

        public double y1() {
            return y1 == null ? 0 : y1;
        }

        public double z1() {
            return z1 == null ? 0 : z1;
        }

        public double au() {
            return au == null ? 215.094177 : au;
        }

        public double c() {
            return c == null ? 0 : c;
        }

        public void setrStar(Double rStar) {
            this.rStar = rStar;
        }

        public void setThermal(Double thermal) {
            this.thermal = thermal;
        }

        public void setReflect(Double reflect) {
            this.reflect = reflect;
        }

        public void setDilute(Double dilute) {
            this.dilute = dilute;
        }

        public void setTc(Double tc) {
            this.tc = tc;
        }

        public void setQ(Double q) {
            this.q = q;
        }

        public void setX1(Double x1) {
            this.x1 = x1;
        }

        public void setY1(Double y1) {
            this.y1 = y1;
        }

        public void setZ1(Double z1) {
            this.z1 = z1;
        }

        public void setAu(Double au) {
            this.au = au;
        }

        public void setC(Double c) {
            this.c = c;
        }

        @Override
        public String toString() {
            return "ModelOptionals[" +
                    "rStar=" + rStar + ", " +
                    "thermal=" + thermal + ", " +
                    "reflect=" + reflect + ", " +
                    "dilute=" + dilute + ", " +
                    "tc=" + tc + ", " +
                    "q=" + q + ", " +
                    "x1=" + x1 + ", " +
                    "y1=" + y1 + ", " +
                    "z1=" + z1 + ", " +
                    "au=" + au + ", " +
                    "c=" + c + ']';
        }
    }
}
