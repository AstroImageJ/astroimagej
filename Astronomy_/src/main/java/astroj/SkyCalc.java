// SkyCalc.java

package astroj;

import java.util.Calendar;
import java.text.NumberFormat;
import java.lang.Math.*;
import ij.*;

// import java.text.DecimalFormat;

/*
 * Celestial Coordinate Algorithms
 *
 * John Kielkopf and Karen Collins
 * kielkopf@louisville.edu, kacoll04@louisville.edu
 *
 * Classes Const, getAltitude, getLST, and getAirmass are based on code
 * from JSkyCalc written by John Thorstensen, Dartmouth College. 
 *
 * Date: January 28, 2011
 * Version: 1.0
 *
 * History:
 *
 *   January 28, 2011
 *     Version 1.0
 *     First working version
 *
 * References:
 *
 * Explanatory Supplement to the Astronomical Almanac
 *  P. Kenneth Seidelmann, Ed.
 *  University Science Books, Mill Valley, CA, 1992
 *
 * Astronomical Formulae for Calculators, 2nd Edition
 *  Jean Meeus
 *  Willmann-Bell, Richmond, VA, 1982
 *
 * Astronomical Algorithms
 *  Jean Meeus
 *  Willmann-Bell, Richmond, VA, 1991
 *
 */

/*
 * Usage:
 *
 * These algorithms convert astronomical catalog entries into apparent
 * celestial coordinates at a specific observatory site. They should have an
 * inherent accuracy of the order of a second of arc.
 *
 * A correction for atmospheric refraction is included that
 * is reliable when objects are more than 15 degrees above the horizon.
 * At lower altitudes the atmosphere cannot be modeled with second of arc
 * precision without additional real-time corrections.
 *
 * The precession formulae apply to catalogs in the FK5 system.
 * They will not give exact results for catalogs in the FK4 system.
 * See Meeus, Astronomical Algorithms, p. 129, for an explanation.
 *
 */



public class SkyCalc
	{

    class Const {
        /* some constants */
        public static final double J2000 =  2451545. ; // the julian epoch 2000
        public static final double DEG_IN_RADIAN = 57.2957795130823;
        public static final double HRS_IN_RADIAN = 3.81971863420549;
        public static final double ARCSEC_IN_RADIAN = 206264.806247096;
        public static final double PI_OVER_2 = 1.5707963267949;
        public static final double PI = 3.14159265358979;
        public static final double TWOPI = 6.28318530717959;
        public static final double EARTHRAD_IN_AU = 23454.7910556298; // earth radii in 1 AU
        public static final double EARTHRAD_IN_KM = 6378.1366; // equatorial
        public static final double KMS_AUDAY = 1731.45683633; // 1731 km/sec = 1 AU/d
        public static final double SPEED_OF_LIGHT = 299792.458; // exact, km/s.
        public static final double SS_MASS = 1.00134198; // solar system mass, M_sun
        public static final double ASTRO_UNIT = 1.4959787066e11; // 1 AU in meters
        public static final double LIGHTSEC_IN_AU = 499.0047863852; // 1 AU in SI meters
        public static final double OMEGA_EARTH = 7.292116e-5; // inertial ang vel of earth
        public static final double SID_RATE = 1.0027379093;  // sidereal/solar ratio
        }

    public static double getAltitude(double jd, double ra, double dec, double lat, double lon) {
        return getAltitude(dec, getLST(jd, lon) - ra, lat);
        }

	public static double getAltitude(double dec, double hrangle, double lat) {
        // returns altitiude (degr)
        double alt;
        double cosdec, sindec, cosha, coslat, sinlat;

        dec = dec / Const.DEG_IN_RADIAN;
        hrangle = hrangle / Const.HRS_IN_RADIAN;
        lat = lat / Const.DEG_IN_RADIAN;
        cosdec = Math.cos(dec);
        sindec = Math.sin(dec);
        cosha = Math.cos(hrangle);
        coslat = Math.cos(lat);
        sinlat = Math.sin(lat);
        alt = Const.DEG_IN_RADIAN * Math.asin(cosdec*cosha*coslat + sindec*sinlat);
        return alt;
        }

    public static double getLST(double jdin, double longitin) {
        double tt, ut, jdmid, jdint, jdfrac, sid_g, sid;
        long jdintt, sid_int;

        jdintt = (long) jdin;
        jdfrac = jdin - jdintt;
        if (jdfrac < 0.5) {
         jdmid = jdintt - 0.5;
         ut = jdfrac + 0.5;
        }
        else {
         jdmid = jdintt + 0.5;
         ut = jdfrac - 0.5;
        }
        tt = (jdmid - Const.J2000) / 36525;
        sid_g = (24110.54841+8640184.812866*tt+0.093104*tt*tt-6.2e-6*tt*tt*tt)/86400.;
        sid_int = (long) sid_g;
        sid_g = sid_g - (double) sid_int;
        sid_g = sid_g + 1.0027379093 * ut + longitin/(15.*24.);
        sid_int = (long) sid_g;
        sid_g = (sid_g - (double) sid_int) * 24.;
        if(sid_g < 0.) sid_g = sid_g + 24.;
        return sid_g;
        }

   public static double getAirmass(double jd, double ra, double dec,  double lat, double lon) {
        /* returns the airmass for a given Julian Date, declination (degrees)
         * right ascension (hours), latitude (degrees), and longitude (
         */
        return getAirmass(getAltitude(dec, getLST(jd, lon) - ra, lat));
        }


   public static double getAirmass(double alt) {
        /* returns the true airmass for a given altitude (degrees).  Ported from C. */
        /* The expression used is based on a tabulation of the mean KPNO
           atmosphere given by C. M. Snell & A. M. Heiser, 1968,
        PASP, 80, 336.  They tabulated the airmass at 5 degr
           intervals from z = 60 to 85 degrees; I fit the data with
           a fourth order poly for (secz - airmass) as a function of
           (secz - 1) using the IRAF curfit routine, then adjusted the
           zeroth order term to force (secz - airmass) to zero at
           z = 0.  The poly fit is very close to the tabulated points
        (largest difference is 3.2e-4) and appears smooth.
           This 85-degree point is at secz = 11.47, so for secz > 12
           I just return secz - 1.5 ... about the largest offset
           properly determined. */
        double secz, seczmin1;
        int i, ord = 3;
        double [] coef = {2.879465e-3, 3.033104e-3, 1.351167e-3, -4.716679e-5};
        double result = 0.;

        if(alt <= 0.) return (-1.);   /* out of range. */
        secz = 1. / Math.sin(alt / Const.DEG_IN_RADIAN);
        seczmin1 = secz - 1.;
        if(secz > 12.) return (secz - 1.5);   // approx at extreme airmass
        for(i = ord; i >= 0; i--) result = (result + coef[i]) * seczmin1;
        result = secz - result;
        return result;
        }

    /* Calculates HelioCentric Julian Day correction based on Julian Day
     * Adapted from excel macro written by Dan Bruton on 1/10/2004
     * see http://www.physics.sfasu.edu/astro/javascript/hjd.html
     */
//    public static double getHJDCorrection(double JD, double RA, double DEC) {
//        double Rads = 3.141592654 / 180.;
//        double Pi = 3.141592654;
//
////          Earth Parameters
//        double D = JD - 2451545.;  //Epoch for Planet Positions
//        double ie = (0.00005 - 0.000000356985 * D) * Rads;
//        double oe = (-11.26064 - 0.00013863 * D) * Rads;
//        double pe = (102.94719 + 0.00000911309 * D) * Rads;
//        double ae = 1.00000011 - 1.36893E-12 * D;
//        double ee = 0.01671022 - 0.00000000104148 * D;
//        double le = (Rads * (100.46435 + 0.985609101 * D));
//
////          Get Earth's position using Kepler's equation
//        double Me1 = (le - pe);
//        double B = Me1 / (2. * Pi);
//        Me1 = 2. * Pi * (B - Math.floor(Math.abs(B)));
//        if (B < 0) Me1 = 2. * Pi * (B + Math.floor(Math.abs(B)));
//        if (Me1 < 0) Me1 = 2. * Pi + Me1;
//        double e = Me1;
//        double delta = 0.05;
//        while (Math.abs(delta) >= Math.pow(10,-12))
//            {
//            delta = e - ee * Math.sin(e) - Me1;
//            e -= delta/(1. - ee * Math.cos(e));
//            }
//        double ve = 2. * Math.atan(Math.pow(((1 + ee) / (1 - ee)) , 0.5) * Math.tan(0.5 * e));
//        if (ve < 0) ve += 2. * Pi;
//        double re = ae * (1. - ee * ee) / (1. + ee * Math.cos(ve));
//        double xe = re * Math.cos(ve + pe);
//        double ye = re * Math.sin(ve + pe);
//        double ze = 0.;
//
////          Compute RA and DEC of the Sun
//        double ecl = 23.439292 * Rads;
//        double xeq = xe;
//        double yeq = ye * Math.cos(ecl) - ze * Math.sin(ecl);
//        double zeq = ye * Math.sin(ecl) + ze * Math.cos(ecl);
//        double ra = 12. + Math.atan(yeq / xeq) * 12. / Pi;
//        if (xe < 0) ra += 12.;
//        if (ye < 0 && xe > 0) ra += 24.;
//        double dec = 180. * Math.atan(zeq / (Math.sqrt(xeq * xeq + yeq * yeq))) / Pi;
//
////      Earth XYZ
//        ra += 12.;
//        if (ra > 24.) ra -= 24.;
//        double cel = Math.cos(dec * Pi / 180.);
//        double earthx = Math.cos(ra * Pi / 12.) * cel;
//        double earthy = Math.sin(ra * Pi / 12.) * cel;
//        double earthz = Math.sin(dec * Pi / 180.);
//
////      Object XYZ Coodinates
//        cel = Math.cos((DEC) * Pi / 180.);
//        double objectx = Math.cos(RA * Pi / 180.) * cel;
//        double objecty = Math.sin(RA * Pi / 180.) * cel;
//        double objectz = Math.sin(DEC * Pi / 180.);
//
////      Correction
//        double ausec = 8.3168775; // 1/(Speed of light) (minutes per AU)
//        double correction = ausec * (earthx * objectx + earthy * objecty + earthz * objectz);
//        return correction/(24.0*60.0);
//        }
//	}

    /* Calculates HelioCentric Julian Day correction based on Julian Day
     * Adapted from javascript written by Dan Bruton of Stephen F. Austin State University
     * see http://www.physics.sfasu.edu/astro/javascript/hjd.html
     */

    public static double getHJDCorrection(double JD, double ORA, double ODEC) {

        ORA *= 15.0;

//        IJ.log("JD="+JD);
//        IJ.log("Object RA="+ORA);
//        IJ.log("Object Dec="+ODEC);

        double pi = 3.141592654;
        double[] sunRaDec = getPosition(JD, 3);
        double ra = sunRaDec[0];
        double dec = sunRaDec[1];

    // Earth RA/DEC
        dec*=-1;
        ra+=12;
        if (ra>24) ra -= 24;

//        IJ.log("Earth RA="+ra);
//        IJ.log("Earth Dec="+dec);

    // Earth XYZ
        double cel = Math.cos(dec * pi/180);
        double earthx = Math.cos(ra * pi/12) * cel;
        double earthy = Math.sin(ra * pi/12) * cel;
        double earthz = Math.sin(dec * pi/180);

//        IJ.log("Earth X="+earthx);
//        IJ.log("Earth Y="+earthy);
//        IJ.log("Earth Z="+earthz);

    // Object XYZ
        cel = Math.cos(ODEC * pi/180);
        double objectx = Math.cos(ORA * pi/180) * cel;
        double objecty = Math.sin(ORA * pi/180) * cel;
        double objectz = Math.sin(ODEC * pi/180);

//        IJ.log("Object X="+objectx);
//        IJ.log("Object Y="+objecty);
//        IJ.log("Object Z="+objectz);

    // Light Time (Minutes per AU)
        double aumin=8.3168775;
        double correction = aumin * (earthx * objectx + earthy * objecty + earthz * objectz);
        return correction/(24.*60.);
        }

    public static double[] getPosition(double JD, int Planet) {
      //Compute Positions of All Planets
        int n=965;
        int j=1;
        double pi = 3.141592654;
        double Rads = pi / 180;

        double[] el = new double[55];
        int p = Planet;
//        1 = "Mercury"
//        2 = "Venus"
//        3 = "Sun"
//        4 = "Mars"
//        5 = "Jupiter"
//        6 = "Saturn"
//        7 = "Uranus"
//        8 = "Neptune"
//        9 = "Pluto"
       double D = JD - 2451545.;  //Epoch for Planet Positions

      // Mercury
        el[1] = (7.00487 - 0.000000178797 * D) * Rads;
        el[2] = (48.33167 - 0.0000033942 * D) * Rads;
        el[3] = (77.45645 + 0.00000436208 * D) * Rads;
        el[4] = 0.38709893 + 1.80698E-11 * D;
        el[5] = 0.20563069 + 0.000000000691855 * D;
        el[6] = (Rads * (252.25084 + 4.092338796 * D));
      // Venus
        el[7] = (3.39471 - 0.0000000217507 * D) * Rads;
        el[8] = (76.68069 - 0.0000075815 * D) * Rads;
        el[9] = (131.53298 - 0.000000827439 * D) * Rads;
        el[10] = 0.72333199 + 2.51882E-11 * D;
        el[11] = 0.00677323 - 0.00000000135195 * D;
        el[12] = (Rads * (181.97973 + 1.602130474 * D));
      // Earth
        el[13] = (0.00005 - 0.000000356985 * D) * Rads;
        el[14] = (-11.26064 - 0.00013863 * D) * Rads;
        el[15] = (102.94719 + 0.00000911309 * D) * Rads;
        el[16] = 1.00000011 - 1.36893E-12 * D;
        el[17] = 0.01671022 - 0.00000000104148 * D;
        el[18] = (Rads * (100.46435 + 0.985609101 * D));
      // Mars
        el[19] = (1.85061 - 0.000000193703 * D) * Rads;
        el[20] = (49.57854 - 0.0000077587 * D) * Rads;
        el[21] = (336.04084 + 0.00001187 * D) * Rads;
        el[22] = 1.52366231 - 0.000000001977 * D;
        el[23] = 0.09341233 - 0.00000000325859 * D;
        el[24] = (Rads * (355.45332 + 0.524033035 * D));
      // Jupiter
        el[25] = (1.3053 - 0.0000000315613 * D) * Rads;
        el[26] = (100.55615 + 0.00000925675 * D) * Rads;
        el[27] = (14.75385 + 0.00000638779 * D) * Rads;
        el[28] = 5.20336301 + 0.0000000166289 * D;
        el[29] = 0.04839266 - 0.00000000352635 * D;
        el[30] = (Rads * (34.40438 + 0.083086762 * D));
      // Saturn
        el[31] = (2.48446 + 0.0000000464674 * D) * Rads;
        el[32] = (113.71504 - 0.0000121 * D) * Rads;
        el[33] = (92.43194 - 0.0000148216 * D) * Rads;
        el[34] = 9.53707032 - 0.0000000825544 * D;
        el[35] = 0.0541506 - 0.0000000100649 * D;
        el[36] = (Rads * (49.94432 + 0.033470629 * D));
      // Uranus
        el[37] = (0.76986 - 0.0000000158947 * D) * Rads;
        el[38] = (74.22988 + 0.0000127873 * D) * Rads;
        el[39] = (170.96424 + 0.0000099822 * D) * Rads;
        el[40] = 19.19126393 + 0.0000000416222 * D;
        el[41] = 0.04716771 - 0.00000000524298 * D;
        el[42] = (Rads * (313.23218 + 0.011731294 * D));
      // Neptune
        el[43] = (1.76917 - 0.0000000276827 * D) * Rads;
        el[44] = (131.72169 - 0.0000011503 * D) * Rads;
        el[45] = (44.97135 - 0.00000642201 * D) * Rads;
        el[46] = 30.06896348 - 0.0000000342768 * D;
        el[47] = 0.00858587 + 0.000000000688296 * D;
        el[48] = (Rads * (304.88003 + 0.0059810572 * D));
      // Pluto
        el[49] = (17.14175 + 0.0000000841889 * D) * Rads;
        el[50] = (110.30347 - 0.0000002839 * D) * Rads;
        el[51] = (224.06676 - 0.00000100578 * D) * Rads;
        el[52] = 39.48168677 - 0.0000000210574 * D;
        el[53] = 0.24880766 + 0.00000000177002 * D;
        el[54] = (Rads * (238.92881 + 0.003931834 * D));
        int q = 6 * (p - 1);
        double ip = el[q + 1];
        double op = el[q + 2];
        double pp = el[q + 3];
        double ap = el[q + 4];
        double ep = el[q + 5];
        double lp = el[q + 6];
        double ie = el[13];
        double oe = el[14];
        double pe = el[15];
        double ae = el[16];
        double ee = el[17];
        double le = el[18];

      //Get Earth's position using Kepler's equation
        double Me1 = (le - pe);
        double B = Me1 / (2 * pi);
        Me1 = 2 * pi * (B - Math.floor(Math.abs(B)));
        if (B<0) Me1 = 2 * pi * (B + Math.floor(Math.abs(B)));
        if (Me1 < 0) Me1 = 2 * pi + Me1;
        double e = Me1;
        double delta = 0.05;
        while (Math.abs(delta) >= Math.pow(10,-12))
            {
            delta = e - ee * Math.sin(e) - Me1;
            e -= delta / (1 - ee * Math.cos(e));
            }
        double ve = 2 * Math.atan(Math.pow(((1 + ee) / (1 - ee)) , 0.5) * Math.tan(0.5 * e));
        if (ve < 0) ve += 2 * pi;
        double re = ae * (1 - ee * ee) / (1 + ee * Math.cos(ve));
        double xe = re * Math.cos(ve + pe);
        double ye = re * Math.sin(ve + pe);
        double ze = 0;

      //Get planet's position using Kepler's equation
        double mp = (lp - pp);
        B = mp / (2 * pi);
        mp = 2 * pi * (B - Math.floor(Math.abs(B)));
        if (B<0) mp = 2 * pi * (B + Math.floor(Math.abs(B)));
        if (mp < 0) mp += 2 * pi;
        e = mp;
        delta = 0.05;
        while (Math.abs(delta) >= Math.pow(10,-12))
            {
            delta = e - ep * Math.sin(e) - mp;
            e -= delta / (1 - ep * Math.cos(e));
            }
        double vp = 2 * Math.atan(Math.pow(((1 + ep) / (1 - ep)) , 0.5) * Math.tan(0.5 * e));
        if (vp < 0) vp += 2 * pi;
        double rp = ap * (1 - ep * ep) / (1 + ep * Math.cos(vp));
        double xh = rp * (Math.cos(op) * Math.cos(vp + pp - op) - Math.sin(op) * Math.sin(vp + pp - op) * Math.cos(ip));
        double yh = rp * (Math.sin(op) * Math.cos(vp + pp - op) + Math.cos(op) * Math.sin(vp + pp - op) * Math.cos(ip));
        double zh = rp * (Math.sin(vp + pp - op) * Math.sin(ip));
        double xg = xh - xe;
        double yg = yh - ye;
        double zg = zh;

      //compute RA and DEC
        double ecl = 23.439292 * Rads;  //Updated 1-29-2009 429 instead of 439
        double xeq = xg;
        double yeq = yg * Math.cos(ecl) - zg * Math.sin(ecl);
        double zeq = yg * Math.sin(ecl) + zg * Math.cos(ecl);
        double ra = Math.atan(yeq/ xeq)*12/pi;
        if (xeq < 0) ra += 12;
        if (yeq < 0 && xeq > 0) ra += 24;
        double dec = 180*Math.atan(zeq / Math.pow((xeq * xeq + yeq * yeq),0.5))/pi;

      // Sun Coodinates
        xeq = xe;
        yeq = ye * Math.cos(ecl) - ze * Math.sin(ecl);
        zeq = ye * Math.sin(ecl) + ze * Math.cos(ecl);
        double rae = 12 + Math.atan(yeq/ xeq)*12/pi;
        if (xe < 0) rae += 12;
        if (ye < 0 && xe > 0) rae += 24;
        double dece = -180*Math.atan(zeq / Math.pow((xeq * xeq + yeq * yeq),0.5))/pi;
        if (p == 3)
            {
            ra=rae;
            dec=dece;
            }
        double raa = 0;
        if (ra < 12) raa = 12 - ra;
        else raa = 36 - ra;
        double[] retval = new double[2];
        retval[0] = ra;
        retval[1] = dec;
        return retval;

        }


    }
