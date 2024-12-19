package Astronomy.multiplot.modelling;

import astroj.IJU;
import flanagan.math.MinimizationFunction;
import ij.measure.UserFunction;

import static Astronomy.MultiPlot_.*;

public class FitLightCurveChi2 implements MinimizationFunction, UserFunction {
    final int curve;
    private final double min;
    private final double max;
    final boolean fittingAgainstMedian;
    double[] detrendY;
    double dof;
    double chi2;
    double bp;
    double[] detrendX, detrendYE, priorCenter, lcModel;
    boolean[] isFitted;
    double detrendYAverage;
    int[] detrendIndex;
    int maxFittedVars;
    double[][] detrendVars;
    final int[] index;

    public FitLightCurveChi2(int curve, double[] detrendY, double dof, double bp, double[] detrendX, double[] detrendYE,
                             boolean[] isFitted, double detrendYAverage, double[] priorCenter, int[] detrendIndex,
                             int maxFittedVars, double[][] detrendVars, int[] index) {
        this(curve, Double.NaN, Double.NaN, false, detrendY, dof, bp, detrendX, detrendYE, isFitted,
                detrendYAverage, priorCenter, detrendIndex, maxFittedVars, detrendVars, index);
    }

    public FitLightCurveChi2(int curve, double min, double max, boolean fittingAgainstMedian, double[] detrendY,
                             double dof, double bp, double[] detrendX, double[] detrendYE, boolean[] isFitted,
                             double detrendYAverage, double[] priorCenter, int[] detrendIndex, int maxFittedVars,
                             double[][] detrendVars, int[] index) {
        this.curve = curve;
        this.min = min;
        this.max = max;
        this.fittingAgainstMedian = fittingAgainstMedian;
        this.detrendY = detrendY;
        this.dof = dof;
        this.bp = bp;
        this.detrendX = detrendX;
        this.detrendYE = detrendYE;
        this.isFitted = isFitted;
        this.detrendYAverage = detrendYAverage;
        this.priorCenter = priorCenter;
        this.detrendIndex = detrendIndex;
        this.maxFittedVars = maxFittedVars;
        this.detrendVars = detrendVars;
        this.index = index;
    }

    public double function(double[] params) {
        int numData = detrendY.length;
        int numDetrendVars = detrendVars.length;
        int nPars = params.length;
        double[] dPars = new double[detrendVars.length];

        if (dof < 1) {
            dof = 1;
        }

        var chi2 = 0D;
        double residual;
        int fp = 0;

        double f0 = priorCenter[0]; // baseline flux
        double p0 = priorCenter[1]; // r_p/r_*
        double ar = priorCenter[2]; // a/r_*
        double tc = priorCenter[3]; //transit center time
        double incl = priorCenter[4];  //inclination
        double u1 = priorCenter[5];  //quadratic limb darkening parameter 1
        double u2 = priorCenter[6];  //quadratic limb darkening parameter 2
        double e = forceCircularOrbit[curve] ? 0.0 : eccentricity[curve];
        double ohm = forceCircularOrbit[curve] ? 0.0 : omega[curve];
        double b = 0.0;
        double[] lcModel = null;
        if (useTransitFit[curve]) {
            f0 = lockToCenter[curve][0] ?
                    priorCenter[0] :
                    params[fp < nPars ? fp++ : nPars - 1]; // baseline flux
            p0 = lockToCenter[curve][1] ?
                    Math.sqrt(priorCenter[1]) :
                    params[fp < nPars ? fp++ : nPars - 1]; // r_p/r_*
            ar = lockToCenter[curve][2] ?
                    priorCenter[2] :
                    params[fp < nPars ? fp++ : nPars - 1]; // a/r_*
            tc = lockToCenter[curve][3] ?
                    priorCenter[3] :
                    params[fp < nPars ? fp++ : nPars - 1]; //transit center time
            if (!bpLock[curve]) {
                incl = lockToCenter[curve][4] ?
                        priorCenter[4] * Math.PI / 180.0 :
                        params[fp < nPars ? fp++ : nPars - 1];  //inclination
                b = Math.cos(incl) * ar;
                if (b > 1.0 + p0) {  //ensure planet transits or grazes the star
                    return Double.POSITIVE_INFINITY;
                }
            } else {
                incl = Math.acos(bp / ar);
                b = bp;
            }
            u1 = lockToCenter[curve][5] ?
                    priorCenter[5] :
                    params[fp < nPars ? fp++ : nPars - 1];  //quadratic limb darkening parameter 1
            u2 = lockToCenter[curve][6] ?
                    priorCenter[6] :
                    params[fp < nPars ? fp++ : nPars - 1];  //quadratic limb darkening parameter 2

            lcModel = IJU.transitModel(detrendX, f0, incl, p0, ar, tc, orbitalPeriod[curve], e, ohm, u1, u2, useLonAscNode[curve], lonAscNode[curve], true);
            var midpointFlux = IJU.transitModel(new double[]{
                    tc}, f0, incl, p0, ar, tc, orbitalPeriod[curve], e, ohm, u1, u2, useLonAscNode[curve], lonAscNode[curve], true)[0];
            var depth = (1 - (midpointFlux / f0)) * 1000;
            if (depth < 0) {
                return Double.NaN;
            }
        }

        int dp = 0;
        for (int p = 7; p < maxFittedVars; p++) {
            if (isFitted[p]) {
                dPars[dp++] = params[fp++];
            } else if (detrendIndex[p - 7] != 0 && detrendYDNotConstant[p - 7] && lockToCenter[curve][p]) {
                dPars[dp++] = priorCenter[p];
            }
        }


        if (useTransitFit[curve]) {
            var halfdur = (orbitalPeriod[curve] / Math.PI * Math.asin(Math.sqrt((1.0 + p0) * (1.0 + p0) - b * b) / (Math.sin(incl) * ar)) * Math.sqrt(1.0 - (
                    forceCircularOrbit[curve] ?
                            0.0 :
                            eccentricity[curve] * eccentricity[curve])) / (1.0 + (
                    forceCircularOrbit[curve] ?
                            0.0 :
                            eccentricity[curve]) * Math.sin(forceCircularOrbit[curve] ?
                    0.0 :
                    omega[curve]))) / 2.0;
            if (!lockToCenter[curve][3] && ((tc - halfdur > max - 0.007 || tc + halfdur < min + 0.007))) {
                return Double.NaN;
            }
            if (!lockToCenter[curve][2] && (ar < (1.0 + p0))) {
                chi2 = Double.POSITIVE_INFINITY;  //boundary check that planet does not orbit within star
            } else if ((!lockToCenter[curve][2] || !lockToCenter[curve][4]) && ((ar * Math.cos(incl) * (1.0 - e * e) / (1.0 + e * Math.sin(ohm * Math.PI / 180.0))) >= 1.0 + p0)) {
                if (!lockToCenter[curve][4] && autoUpdatePrior[curve][4]) {
                    priorCenter[4] = Math.round(10.0 * Math.acos((0.5 + p0) * (1.0 + e * Math.sin(ohm * Math.PI / 180.0)) / (ar * (1.0 - e * e))) * 180.0 / Math.PI) / 10.0;
                    if (Double.isNaN(priorCenter[4])) {
                        priorCenter[4] = 89.9;
                    }
                    //priorCenterSpinner[curve][4].setValue(priorCenter[4]);
                }
                chi2 = Double.POSITIVE_INFINITY; //boundary check that planet passes in front of star
            } else if ((!lockToCenter[curve][5] || !lockToCenter[curve][6]) && (((u1 + u2) > 1.0) || ((u1 + u2) < 0.0) || (u1 > 1.0) || (u1 < 0.0) || (u2 < -1.0) || (u2 > 1.0))) {
                chi2 = Double.POSITIVE_INFINITY;
            } else {
                // apply the Gaussian prior
                // if it's an angular parameter, make sure we handle the boundary
                for (int p = 0; p < params.length - 1; p++) {
                    if (usePriorWidth[curve][index[p]]) {
                        if (index[p] == 4) {
                            double chi = Math.atan2(Math.sin(params[p] - Math.toRadians(priorCenter[index[p]])), Math.cos(params[p] - Math.toRadians(priorCenter[index[p]]))) / Math.toRadians(priorWidth[curve][index[p]]);
                            chi2 += chi * chi;
                        } else if (index[p] == 1) {
                            double chi = (params[p] * params[p] - priorCenter[index[p]]) / priorWidth[curve][index[p]];
                            chi2 += chi * chi;
                        } else {
                            double chi = (params[p] - priorCenter[index[p]]) / priorWidth[curve][index[p]];
                            chi2 += chi * chi;
                        }
                    }
                }

                //apply data-model chi2 contribution
                for (int j = 0; j < numData; j++) {
                    residual = detrendY[j];// - param[0];
                    for (int i = 0; i < numDetrendVars; i++) {
                        residual -= detrendVars[i][j] * dPars[i];
                    }
                    residual -= (lcModel[j] - detrendYAverage);
                    chi2 += ((residual * residual) / (detrendYE[j] * detrendYE[j]));
                }
            }
        } else {
            for (int j = 0; j < numData; j++) {
                residual = detrendY[j];// - param[0];
                for (int i = 0; i < numDetrendVars; i++) {
                    residual -= detrendVars[i][j] * dPars[i];
                }
                if (numDetrendVars == 0 && params.length == 1) {
                    residual -= params[0];
                }
                chi2 += ((residual * residual) / (detrendYE[j] * detrendYE[j]));
            }
        }
        return chi2 / (double) dof;
    }

    @Override
    public double userFunction(double[] params, double $) {
        int fp = 0;
        int nPars = params.length;

        double f0 = priorCenter[0]; // baseline flux
        double p0 = priorCenter[1]; // r_p/r_*
        double ar = priorCenter[2]; // a/r_*
        double tc = priorCenter[3]; //transit center time
        double incl = priorCenter[4];  //inclination
        double u1 = priorCenter[5];  //quadratic limb darkening parameter 1
        double u2 = priorCenter[6];  //quadratic limb darkening parameter 2
        double e = forceCircularOrbit[curve] ? 0.0 : eccentricity[curve];
        double ohm = forceCircularOrbit[curve] ? 0.0 : omega[curve];
        double b = 0.0;

        if (useTransitFit[curve]) {
            f0 = lockToCenter[curve][0] ?
                    priorCenter[0] :
                    params[fp < nPars ? fp++ : nPars - 1]; // baseline flux
            p0 = lockToCenter[curve][1] ?
                    Math.sqrt(priorCenter[1]) :
                    params[fp < nPars ? fp++ : nPars - 1]; // r_p/r_*
            ar = lockToCenter[curve][2] ?
                    priorCenter[2] :
                    params[fp < nPars ? fp++ : nPars - 1]; // a/r_*
            tc = lockToCenter[curve][3] ?
                    priorCenter[3] :
                    params[fp < nPars ? fp++ : nPars - 1]; //transit center time
            if (!bpLock[curve]) {
                incl = lockToCenter[curve][4] ?
                        priorCenter[4] * Math.PI / 180.0 :
                        params[fp < nPars ? fp++ : nPars - 1];  //inclination
                b = Math.cos(incl) * ar;
                if (b > 1.0 + p0) {  //ensure planet transits or grazes the star
                    return Double.NaN;
                }
            } else {
                incl = Math.acos(bp / ar);
            }
            u1 = lockToCenter[curve][5] ?
                    priorCenter[5] :
                    params[fp < nPars ? fp++ : nPars - 1];  //quadratic limb darkening parameter 1
            u2 = lockToCenter[curve][6] ?
                    priorCenter[6] :
                    params[fp < nPars ? fp++ : nPars - 1];  //quadratic limb darkening parameter 2
        }

        // Fit against yMedian when no transit and no params
        if (nPars == 1 && !useTransitFit[curve] && fittingAgainstMedian) {
            if (params[0] < 0) {
                return Double.NaN;
            }
        }

        if (useTransitFit[curve]) {
            if (p0 < 0) {
                return Double.NaN;
            }

            if (!bpLock[curve] && (incl > Math.toRadians(90) || incl < Math.toRadians(50))) {
                return Double.NaN;
            }

            if (f0 < 0) {
                return Double.NaN;
            }

            if (ar < 1.0) {
                return Double.NaN;
            }

            if ((u1 > 1 || u1 < -1) || (u2 > 1 || u2 < -1)) {
                return Double.NaN;
            }


            if (!lockToCenter[curve][2] && (ar < (1.0 + p0))) {
                return Double.NaN;  //boundary check that planet does not orbit within star
            } else if ((!lockToCenter[curve][2] || !lockToCenter[curve][4]) && ((ar * Math.cos(incl) * (1.0 - e * e) / (1.0 + e * Math.sin(ohm * Math.PI / 180.0))) >= 1.0 + p0)) {
                /*if (!lockToCenter[curve][4] && autoUpdatePrior[curve][4]) {
                    priorCenter[curve][4] = Math.round(10.0 * Math.acos((0.5 + p0) * (1.0 + e * Math.sin(ohm * Math.PI / 180.0)) / (ar * (1.0 - e * e))) * 180.0 / Math.PI) / 10.0;
                    if (Double.isNaN(priorCenter[curve][4])) priorCenter[curve][4] = 89.9;
                    priorCenterSpinner[curve][4].setValue(priorCenter[curve][4]);
                }*/
                return Double.NaN; //boundary check that planet passes in front of star
            } else if ((!lockToCenter[curve][5] || !lockToCenter[curve][6]) && (((u1 + u2) > 1.0) || ((u1 + u2) < 0.0) || (u1 > 1.0) || (u1 < 0.0) || (u2 < -1.0) || (u2 > 1.0))) {
                return Double.NaN;
            }
        }

        return function(params);
    }
}
