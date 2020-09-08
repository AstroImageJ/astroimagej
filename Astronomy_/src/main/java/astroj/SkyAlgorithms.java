// SkyAlgorithms.java

package astroj;

import java.util.*;
import static java.lang.Math.*;


/* ****************************************************************************/
/*                                                                            */
/* Celestial Coordinate Algorithms                                            */
/*                                                                            */
/* John Kielkopf                                                              */
/* kielkopf@louisville.edu                                                    */
/*                                                                            */
/* Converted to Java by Karen Collins December 22nd, 2011                     */
/* karen.collins@insightbb.com                                                */
/*                                                                            */
/* Distributed under the terms of the General Public License (see LICENSE)    */
/*                                                                            */
/* Date: October 15, 2011                                                     */
/* Version: 1.1                                                               */
/*                                                                            */
/* History:                                                                   */
/*                                                                            */
/*   September 7, 2006                                                        */
/*     Version 1.02                                                           */
/*     First working version                                                  */
/*                                                                            */
/*   June 16, 2007                                                            */
/*     Version 1.04                                                           */
/*     Cleaned remnants of developmental testing                              */
/*     Moved refraction routines to pointing                                  */
/*                                                                            */
/*   September 1, 2008                                                        */
/*     Version 1.05                                                           */
/*     Corrected for quandrant errors in equatorial-horizontal conversions    */
/*                                                                            */
/*   September 12, 2008                                                       */
/*     Version 1.06                                                           */
/*     Changed to algorithms.h for a header instead of protocol.h             */
/*                                                                            */
/*   December 16, 2008                                                        */
/*     Version 1.07                                                           */
/*     Corrected Map functions so that upper limit is >=24 rather than >      */
/*                                                                            */
/*   October 15, 2011                                                         */
/*     Version 1.1                                                            */
/*     JDNow, LSTNow and UTNow modified for millisecond resolution            */
/*                                                                            */
/******************************************************************************/
/*                                                                            */
/* References:                                                                */
/*                                                                            */
/* Explanatory Supplement to the Astronomical Almanac                         */
/*  P. Kenneth Seidelmann, Ed.                                                */
/*  University Science Books, Mill Valley, CA, 1992                           */
/*                                                                            */
/* Astronomical Formulae for Calculators, 2nd Edition                         */
/*  Jean Meeus                                                                */
/*  Willmann-Bell, Richmond, VA, 1982                                         */
/*                                                                            */
/* Astronomical Algorithms                                                    */
/*  Jean Meeus                                                                */
/*  Willmann-Bell, Richmond, VA, 1991                                         */
/*                                                                            */
/*                                                                            */
/* ****************************************************************************/
/*                                                                            */
/* Usage:                                                                     */
/*                                                                            */
/* These algorithms convert astronomical catalog entries into apparent        */
/* celestial coordinates at a specific observatory site. They should have an  */
/* inherent accuracy of the order of a second of arc.                         */
/*                                                                            */
/* A correction for atmospheric refraction is included that                   */
/* is reliable when objects are more than 15 degrees above the horizon.       */
/* At lower altitudes the atmosphere cannot be modeled with second of arc     */
/* precision without additional real-time corrections.                        */
/*                                                                            */
/* The algorithms should be used in conjuntion with routines that convert     */
/* apparent celestial coordinates into real telescope coordinates,            */
/* allowing for mounting mechanical errors, misalignment, and flexure.        */
/* Without such real world corrections, pointing errors typically will        */
/* be minutes, rather than seconds, of arc.                                   */
/*                                                                            */
/* The precession formulae apply to catalogs in the FK5 system.               */
/* They will not give exact results for catalogs in the FK4 system.           */
/* See Meeus, Astronomical Algorithms, p. 129, for an explanation.            */
/*                                                                            */
/* ****************************************************************************/
 



public class SkyAlgorithms
	{
    /**
     * The mathematical constant PI.
     */
    public static final double PI = 3.14159265358979;

    /**
     *  Compute the Julian Day for the given date.
     *  Julian Date is the number of days since noon of Jan 1 4713 B.C.
     * @param ny year
     * @param nm month
     * @param nd day
     * @param ut UT time
     * @return Julian Date
     */
    public static double CalcJD(int ny, int nm, int nd, double ut)
    {
    double A, B, C, D, jd, day;

    day = nd + ut / 24.0;
    if ((nm == 1) || (nm == 2))
        {
        ny = ny - 1;
        nm = nm + 12;
        }

    if (((double)(ny + nm / 12.0 + day / 365.25)) >= (1582.0 + 10.0 / 12.0 + 15.0 / 365.25))
        {
        A = ((int) (ny / 100.0));
        B = 2.0 - A + (int) (A / 4.0);
        }
    else
        {
        B = 0.0;
        }

    if (ny < 0.0)
        {
        C = (int) ((365.25 * (double) ny) - 0.75);
        }
    else
        {
        C = (int) (365.25 * (double) ny);
        }

    D = (int) (30.6001 * (double) (nm + 1));
    jd = B + C + D + day + 1720994.5;
    return (jd);
    }


    /**
     *  Compute the Julian Day for the given date.
     *  Julian Date is the number of days since noon of Jan 1 4713 B.C.
     * @param utDate UT time as {yr, mo, day, UTtime (hours)}
     * @return Julian Date
     */
    public static double CalcJD(double[] utDate)
    {
    double A, B, C, D, jd, day;
    double ny = utDate[0];
    double nm = utDate[1];
    double nd = utDate[2];
    double ut = utDate[3];

    day = nd + ut / 24.0;
    if ((nm == 1) || (nm == 2))
        {
        ny -= 1;
        nm += 12;
        }

    if (((double) ny + nm / 12.0 + day / 365.25) >= (1582.0 + 10.0 / 12.0 + 15.0 / 365.25))
        {
        A = ((int) (ny / 100.0));
        B = 2.0 - A + (int) (A / 4.0);
        }
    else
        {
        B = 0.0;
        }

    if (ny < 0.0)
        {
        C = (int) ((365.25 * (double) ny) - 0.75);
        }
    else
        {
        C = (int) (365.25 * (double) ny);
        }

    D = (int) (30.6001 * (double) (nm + 1));
    jd = B + C + D + day + 1720994.5;
    return (jd);
    }

    /**
     *  Compute the Julian Day for the given date. Adapted from SOFA library.
     *  Julian Date is the number of days since noon of Jan 1 4713 B.C.
     * @param utDate UT time as {yr, mo, day, UTtime (hours)}
     * @return Julian Date
     */
    public static double CalcJD2(double[] utDate)
    {
    int iy = (int)utDate[0];
    int im = (int)utDate[1];
    int id = (int)utDate[2];
    double ut = utDate[3];
    double jd =0;
   int j, ly, my;

   long iypmy;

/* Earliest year allowed (4800BC) */
   int IYMIN = -4799;

/* Month lengths in days */
   int mtab[] = {31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31};


/* Preset status. */
   j = 0;

/* Validate year and month. */
   if (iy < IYMIN) iy = IYMIN;
   if (im < 1) im = 1;
   if (im > 12) im = 12;

/* If February in a leap year, 1, otherwise 0. */
   ly = ((im == 2) && !(iy%4==0) && (iy%100==0 || !(iy%400==0)))?1:0;

/* Validate day, taking into account leap years. */
   if (id < 1) id = 1;
   if (id > (mtab[im-1] + ly)) id = mtab[im-1] + ly;

/* Return result. */
   my = (im - 14) / 12;
   iypmy = (long) (iy + my);
   jd = 2400000.5;
   jd +=(double)((1461L * (iypmy + 4800L)) / 4L
                 + (367L * (long) (im - 2 - 12 * my)) / 12L
                 - (3L * ((iypmy + 4900L) / 100L)) / 4L
                 + (long) id - 2432076L);

/* Return status. */
   return jd+ut/24.0;
    }





/**
 * Calculate the current Julian date with millisecond resolution.
 * @return Julian Date
 */
public static double JDNow()
    {
      int year, month, day;
      int hours, minutes, seconds, milliseconds;

      double ut, jd;
      
	  Calendar now = Calendar.getInstance(TimeZone.getTimeZone("UTC"));

      year = now.get(Calendar.YEAR);
      month = now.get(Calendar.MONTH) + 1;
      day = now.get(Calendar.DAY_OF_MONTH);
      hours = now.get(Calendar.HOUR_OF_DAY);
      minutes = now.get(Calendar.MINUTE);
      seconds = now.get(Calendar.SECOND);
      milliseconds = now.get(Calendar.MILLISECOND);

      /* Calculate floating point ut in hours */

      ut = ( (double) milliseconds )/3600000. +
        ( (double) seconds )/3600. +
        ( (double) minutes )/60. +
        ( (double) hours );

      jd = CalcJD(year, month, day, ut);

      return (jd) ;
    }

/**
 * Compute Local Mean Sidereal Time (lmst) for the specified UT date, time, and longitude.
 *
 * @param year
 * @param month
 * @param day
 * @param ut UT time
 * @param glong geographic longitude of observation (east positive)
 * @param leapSecs Leap Seconds (TAI - UTC)
 * @return Local Mean Sidereal Time (lmst)
 */
public static double CalcLST(int year, int month, int day, double ut, double glong, double leapSecs)
    {
    double TU, TU2, TU3, T0;
    double gmst,lmst, jdEOD;

    jdEOD = CalcJD(year, month, day, 0.0);

    TU = (jdEOD - 2451545.0) / 36525.0;  //TU =number of Julian centuries since 2000 January 1.5
    TU2 = TU * TU;
    TU3 = TU2 * TU;
    T0 = 6.697374558 + 2400.0513369072 * TU + 2.58622E-5 * TU2 - 1.7222078704899681391543959355894E-9* TU3;
    T0 = Map24(T0);

    gmst = Map24(T0 + ut * 1.00273790935 + NLongitude(jdEOD, leapSecs) * cos(TrueObliquity(jdEOD, leapSecs)*PI/180.0) / 15);

    lmst = 24.0 * frac((gmst + glong / 15.0) / 24.0);
    return (lmst);
    }

/**
 * Compute Local Mean Sidereal Time (lmst) for the specified JD and longitude.
 *
 * @param jdin JD
 * @param longitin geographic longitude of observation (east positive)
 * @return Greenwich Mean Sidereal Time (sid_g)
 */
public static double CalcLST(double jdin, double longitin) {
      double tt, ut, jdmid, jdint, jdfrac, sid_g, sid;
      long jdintt, sid_int;
      longitin /= -15.0;

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
      tt = (jdmid - 2451545.) / 36525;
      sid_g = (24110.54841+8640184.812866*tt+0.093104*tt*tt-6.2e-6*tt*tt*tt)/86400.;
      sid_int = (long) sid_g;
      sid_g = sid_g - (double) sid_int;
      sid_g = sid_g + 1.0027379093 * ut - longitin / 24.;
      sid_int = (long) sid_g;
      sid_g = (sid_g - (double) sid_int) * 24.;
      if(sid_g < 0.) sid_g = sid_g + 24.;
      return sid_g;
   }

/**
 * Calculate the current local sidereal time with millisecond resolution.
 * @param glong geographic longitude (east positive)
 * @param leapSecs Leap Seconds (TAI - UTC)
 * @return LST
 */
public static double LSTNow(double glong, double leapSecs)
    {
      int year, month, day;
      int hours, minutes, seconds, milliseconds;

      double ut, lst;

	  Calendar now = Calendar.getInstance(TimeZone.getTimeZone("UTC"));

      year = now.get(Calendar.YEAR);
      month = now.get(Calendar.MONTH) + 1;
      day = now.get(Calendar.DAY_OF_MONTH);
      hours = now.get(Calendar.HOUR_OF_DAY);
      minutes = now.get(Calendar.MINUTE);
      seconds = now.get(Calendar.SECOND);
      milliseconds = now.get(Calendar.MILLISECOND);

      /* Calculate floating point ut in hours */

      ut = ( (double) milliseconds )/3600000. +
        ( (double) seconds )/3600. +
        ( (double) minutes )/60. +
        ( (double) hours );

      lst = CalcLST(year, month, day, ut, glong, leapSecs);

      return (lst) ;
    }


/**
 * Calculate the current universal time in hours with millisecond resolution.
 * @return UT time
 */
public static double UTNow()
    {
    int hours, minutes, seconds, milliseconds;

    double ut;

    Calendar now = Calendar.getInstance(TimeZone.getTimeZone("UTC"));

    hours = now.get(Calendar.HOUR_OF_DAY);
    minutes = now.get(Calendar.MINUTE);
    seconds = now.get(Calendar.SECOND);
    milliseconds = now.get(Calendar.MILLISECOND);

    /* Calculate floating point ut in hours */

    ut = ( (double) milliseconds )/3600000. +
         ( (double) seconds )/3600. +
         ( (double) minutes )/60. +
         ( (double) hours );

    return (ut) ;
    }

/**
 * Calculate the current universal date and time with millisecond resolution.
 * @return {year, month, day, UT_time}
 */
public static double[] UTDateNow()
    {
    int hours, minutes, seconds, milliseconds;
    double[] utdate = {0, 0, 0, 0};
    double ut;

    Calendar now = Calendar.getInstance(TimeZone.getTimeZone("UTC"));

    utdate[0] = now.get(Calendar.YEAR);
    utdate[1] = now.get(Calendar.MONTH)+1;
    utdate[2] = now.get(Calendar.DAY_OF_MONTH);
    hours = now.get(Calendar.HOUR_OF_DAY);
    minutes = now.get(Calendar.MINUTE);
    seconds = now.get(Calendar.SECOND);
    milliseconds = now.get(Calendar.MILLISECOND);

    /* Calculate floating point ut in hours */

    ut = ( (double) milliseconds )/3600000. +
         ( (double) seconds )/3600. +
         ( (double) minutes )/60. +
         ( (double) hours );
    utdate[3] = ut;
    return (utdate) ;
    }


/**
 * Convert a Julian day into universal date and time.
 * Numerical Recipes in C, 2nd ed., Cambridge University Press 1992.
 * @param jd Julian date of interest
 * @return {year, month, day, UT_time}
 */
	public static double[] UTDateFromJD (double jd)
		{
        int julian = (int)(jd+0.5);
        double ut = (jd-(double)julian+0.5)*24.0;
		int ja,jalpha,jb,jc,jdd,je;
		int IGREG = 2299161;
		double[] utdate = {0, 0, 0, 0};

		if (julian >= IGREG)
			{
			jalpha = (int)(((julian-1867216)-0.25)/36524.25);
			ja = julian+1+jalpha-(int)(0.25*jalpha);
			}
		else if (julian < 0)
			{
			ja = julian+36525*(1-julian/36525);
			}
		else	{
			ja = julian;
			}
		jb = ja + 1524;
		jc = (int)(6680.0+((double)(jb-2439870)-122.1)/365.25);
		jdd = (int)(365*jc+(0.25*jc));
		je = (int)((jb-jdd)/30.6001);
		int day = jb-jdd-(int)(30.6001*je);
		int mm = je-1;
		if (mm > 12) mm -= 12;
		int iyyy = jc-4715;
		if (mm > 2) iyyy--;
		if (iyyy <= 0) iyyy--;
		if (julian < 0) iyyy -= 100*(1-julian/36525);

		utdate[0] = iyyy;
		utdate[1] = mm;
		utdate[2] = day;
		utdate[3] = ut;

		return utdate;
		}




/**
 * Map a time in hours to the range  0  to 24.
 * @param hour
 * @return modified hour
 */
public static double Map24(double hour)
    {
    int n;
    if (hour < 0.0)
        {
        n = (int) (hour / 24.0) - 1;
        return (hour - n * 24.0);
        }
    else if (hour >= 24.0)
        {
        n = (int) (hour / 24.0);
        return (hour - n * 24.0);
        }
    else
        {
        return (hour);
        }
    }


/**
 * Map an hourangle in hours to  -12 <= ha < +12.
 * @param hour
 * @return modified hour
 */
public static double Map12(double hour)
    {
    double hour24;
    hour24 = Map24(hour);
    if (hour24 >= 12.0)
        {
        return (hour24 - 24.0);
        }
    else
        {
        return (hour24);
        }
    }


/**
 * Map an angle in degrees to  0 <= angle < 360.
 * @param angle
 * @return modified angle in degrees
 */
public static double Map360(double angle)
    {
    int n;
    if (angle < 0.0)
        {
        n = (int) (angle / 360.0) - 1;
        return (angle - n * 360.0);
        }
    else if (angle >= 360.0)
        {
        n = (int) (angle / 360.0);
        return (angle - n * 360.0);
        }
    else
        {
        return (angle);
        }
    }



/**
 * Map an angle in degrees to -180 <= angle < 180.
 * @param angle
 * @return modified angle in degrees
 */
public static double Map180(double angle)
    {
    double angle360;
    angle360 = Map360(angle);
    if (angle360 >= 180.0)
        {
         return (angle360 - 360.0);
        }
    else
        {
         return (angle360);
        }


    }


/**
 * Calculate the fractional part of double number.
 * @param x number to find fractional part of
 * @return fraction of x
 */
public static double frac(double x)
    {
    x -= (int) x;
    return ((x < 0) ? x + 1.0 : x);
    }


/**
 * Precession from Epoch to JD or back.
 * @param epoch  reference epoch (i.e. 2000.0)
 * @param jd  Julian Date of interest
 * @param ra  Right Ascension at reference epoch
 * @param dec  Declination at reference epoch
 * @param dirflag  +1 = precess from epoch to jd, -1 = precess from jd to epoch
 * @return Modified ra and dec as {ra,dec}
 */
public static double[] Precession(double epoch, double jd, double ra, double dec, int dirflag)
    {
    double[] radec = {ra,dec};
    double tmpra = ra;
    double tmpdec = dec;
    if (dirflag > 0)
        {
        radec = PrecessEpochToJD(epoch, jd, tmpra, tmpdec);
        }
    else if (dirflag < 0)
        {
        radec = PrecessJDToEpoch(epoch, jd, tmpra, tmpdec);
        }

    /* Return ra and dec */

    return radec;
    }


/**
 * Precess from reference epoch to Julian Date of interest.
 * @param epoch reference epoch (i.e. 2000.0)
 * @param jd Julian Date of interest
 * @param ra Right Ascension in hours
 * @param dec Declination in degrees
 * @return Modified ra and dec as {ra,dec}
 */
public static double[] PrecessEpochToJD(double epoch, double jd, double ra, double dec)
    {
      double ra_in, dec_in, ra_out, dec_out;
      double a,b,c;
      double zeta,z,theta;
      double T, t;
      double jdfixed;
      double[] radec = {0, 0};

      /* Fetch the input values for ra and dec and save in radians */

      ra_in = ra;
      dec_in = dec;

      /* Convert to radians for use here */

      ra_in = ra_in*PI/12.;
      dec_in = dec_in*PI/180.;

      /* Find zeta, z, and theta at this moment */

      /* JD for the fixed epoch */

      jdfixed = (epoch - 2000.0)*365.25+2451545.0;


      /* Julian centuries for the fixed epoch from a base epoch 2000.0 */

      T = (jdfixed - 2451545.0) / 36525.0;

      /* Julian centuries for the jd from the fixed epoch */

      t = (jd - jdfixed) / 36525.0;

      /* Evaluate the constants in arc seconds */

      zeta=(2306.2181 + 1.39656*T - 0.000139*T*T)*t +
        (0.30188 - 0.000344*T)*t*t +
        (0.017998)*t*t*t;

      z=(2306.2181 + 1.39656*T - 0.000139*T*T)*t +
        (1.09468 + 0.000066*T)*t*t +
        (0.018203)*t*t*t;

      theta=(2004.3109 - 0.85330*T - 0.000217*T*T)*t +
        (-0.42665 - 0.000217*T)*t*t +
        (-0.041833)*t*t*t;

      /* Convert to radians */

      zeta = zeta * PI / (180.*3600.);
      z = z *  PI / (180.*3600.);
      theta = theta * PI / (180.*3600.);

      /* Calculate the precession */

      a = sin(ra_in + zeta)*cos(dec_in);
      b = cos(ra_in + zeta)*cos(theta)*cos(dec_in) -
            sin(theta)*sin(dec_in);
      c = cos(ra_in + zeta)*sin(theta)*cos(dec_in) +
            cos(theta)*sin(dec_in);
      if (c > 0.9)
        {
        dec_out = acos(sqrt(a*a+b*b));
        }
      else if (c < -0.9)
        {
        dec_out = -acos(sqrt(a*a+b*b));
        }
      else
        {
        dec_out = asin(c);
        }
      ra_out = atan2(a,b) + z;

      /* Convert back to hours and degrees */

      ra_out = ra_out*12./PI;

      dec_out = dec_out*180./PI;

      /* Check for range and adjust to -90 -> +90 and 0 -> 24 and if needed */

      if (dec_out > 90. )
        {
        dec_out = 180. - dec_out;
        ra_out = ra_out + 12.;
}
       if (dec_out < -90. )
        {
        dec_out = -180. - dec_out;
        ra_out = ra_out + 12.;
        }

      ra_out = Map24(ra_out);

      /* Return ra and dec */

      radec[0] = ra_out;
      radec[1] = dec_out;
      return radec;
    }


/**
 * Precess from Julian Date of interest to reference epoch.
 * @param epoch reference epoch (i.e. 2000.0)
 * @param jd Julian Date of interest
 * @param ra Right Ascension in hours
 * @param dec Declination in degrees
 * @return Modified ra and dec as {ra,dec}
 */
public static double[] PrecessJDToEpoch(double epoch, double jd, double ra, double dec)
    {
      double ra_in, dec_in, ra_out, dec_out;
      double a,b,c;
      double zeta,z,theta;
      double T, t;
      double jdfixed;
      double radec[] = {0, 0};

      /* Fetch the input values for ra and dec */

      ra_in = ra;
      dec_in = dec;

      /* Convert to radians for use here */

      ra_in = ra_in*PI/12.;
      dec_in = dec_in*PI/180.;

      /* JD for the fixed epoch */

      jdfixed = (epoch - 2000.0)*365.25+2451545.0;

      /* Julian centuries for the fixed epoch from a base epoch 2000.0 */

      T = (jd - 2451545.0) / 36525.0;

      /* Julian centuries for the jd from the fixed epoch */

      t = (jdfixed - jd) / 36525.0;

      /* Evaluate the constants in arc seconds */

      zeta=(2306.2181 + 1.39656*T - 0.000139*T*T)*t +
        (0.30188 - 0.000344*T)*t*t +
        (0.017998)*t*t*t;

      z=(2306.2181 + 1.39656*T - 0.000139*T*T)*t +
        (1.09468 + 0.000066*T)*t*t +
        (0.018203)*t*t*t;

      theta=(2004.3109 - 0.85330*T - 0.000217*T*T)*t +
        (-0.42665 - 0.000217*T)*t*t +
        (-0.041833)*t*t*t;

      /* Convert to radians */

      zeta = zeta * PI / (180.*3600.);
      z = z *  PI / (180.*3600.);
      theta = theta * PI / (180.*3600.);

      /* Calculate the precession */

      a = sin(ra_in + zeta)*cos(dec_in);
      b = cos(ra_in + zeta)*cos(theta)*cos(dec_in) -
            sin(theta)*sin(dec_in);
      c = cos(ra_in + zeta)*sin(theta)*cos(dec_in) +
            cos(theta)*sin(dec_in);
      if (c > 0.9)
      {
        dec_out = acos(sqrt(a*a+b*b));
      }
      else if (c < -0.9)
      {
        dec_out = -acos(sqrt(a*a+b*b));
      }
      else
      {
        dec_out = asin(c);
      }
      ra_out = atan2(a,b) + z;

      /* Convert back to hours and degrees */

      ra_out = ra_out*12./PI;
      dec_out = dec_out*180./PI;

      /* Check for range and adjust to -90 -> +90 and 0 -> 24 and if needed */

      if (dec_out > 90. )
      {
        dec_out = 180. - dec_out;
        ra_out = ra_out + 12.;
      }
      if (dec_out < -90. )
      {
        dec_out = -180. - dec_out;
        ra_out = ra_out + 12.;
      }

      ra_out = Map24(ra_out);

      /* Return ra and dec */

      radec[0] = ra_out;
      radec[1] = dec_out;
      return radec;

    }


/**
 * Calculate the current (now) coordinates of an object from J2000 ra and dec.
 *
 * This method assumes that the J2000 coordinates already include
 * proper motion to the current time.  That is, these J2000 coordinates are
 * for the current position of the object, but given in an
 * epoch 2000.0 coordinate system.
 *
 * If proper motion is known and the coordinates do not yet include it,
 * then the following method will allow for proper motion to now from epoch:
 *
 * ProperMotion(epoch, jd, ra, dec, pm_ra, pm_dec)
 *
 *
 * Procedure for finding now coordinates from J2000 coordinates:
 *
 *   1. Allow for precession by converting from catalog epoch to the JD
 *   2. Add nutation for the JD
 *
 * @param ra Right Ascension in hours
 * @param dec Declination in degrees
 * @param leapSecs Leap Seconds (TAI - UTC)
 * @return  Modified ra and dec as {ra,dec}
 */
public static double[] raDecNowFromJ2000(double ra, double dec, double leapSecs)
    {

    double[] radec = {ra, dec};
    double jd = JDNow();

    /* Precess ra and dec from J2000 to JD */

    radec = Precession(2000.0, jd, radec[0], radec[1], 1);

    /* Include nutation for ra and dec */

    radec = Nutation(jd, radec[0], radec[1], leapSecs, 1);

    return radec;
    }


/**
 * Calculate the J2000 coordinates of an object from the current (now) ra and dec.
 *
 * The following assumes that the J2000 coordinates already include
 * proper motion to the EOD.  That is, these J2000 coordinates are
 * for the current position of the object, but given in an
 * epoch 2000.0 coordinate system.
 *
 * If proper motion is known and the coordinates do not yet include it,
 * then this routine will allow for proper motion to epoch from JD:
 *
 * ProperMotion(epoch, jd, ra, dec, -pm_ra, -pm_dec)
 *
 *
 * Procedure for finding J2000 coordinates from EOD coordinates:
 *
 *   1. Remove nutation for EOD to J2000
 *   2. Remove precession for EOC to J2000
 *
 * @param ra Right Ascension in hours
 * @param dec Declination in degrees
 * @param leapSecs Leap Seconds (TAI - UTC)
 * @return  Modified ra and dec as {ra,dec}
 */
public static double[] J2000RaDecFromNow(double ra, double dec, double leapSecs)
    {

    double[] radec = {ra, dec};
    double jd = JDNow();

    /* Remove nutation for JD */

    radec = Nutation(jd, radec[0], radec[1], leapSecs, -1);

    /* Remove precession to EOD from J2000 */

    radec = Precession(2000.0, jd, radec[0], radec[1], -1);

    /* Return J2000 coordinates */

    return radec;
    }


/**
 * Convert J2000 ra and dec to ra and dec on a Julian Day
 * or convert ra and dec on a Julian Day to J2000 ra and dec.

 * Optional procedures for finding new coordinates from J2000 coordinates:
 *   1. Compute change in ra and dec due to proper motion
 *   2. Allow for precession by converting from catalog epoch to the JD
 *   3. Add nutation for the JD
 *   4. Add stellar aberration for the JD
 *
 * The forward transformation is exact but the inverse transformation
 * requires iterating stellar aberration and is done only approximately
 * in the aberration routine.
 *
 * @param epoch Reference Epoch (i.e. 2000.0)
 * @param jd Julian Date of interest
 * @param ra Right Ascension in hours
 * @param dec Declination in degrees
 * @param pm_ra proper motion in Right Ascension in mas/yr
 * @param pm_dec proper motion in Declination in mas/yr
 * @param leapSecs Leap Seconds (TAI - UTC)
 * @param direction true = forward (Epoch to JD), false = reverse (JD to Epoch)
 * @param usePM true = include proper motion corrections
 * @param usePrec true = include precession corrections
 * @param useNut true = include nutation corrections
 * @param useAber true = include stellar aberration corrections
 * @return  Modified ra and dec as {ra,dec}
 */
public static double[] Convert(double epoch, double jd, double ra, double dec, double pm_ra, double pm_dec, double leapSecs,
                                boolean direction, boolean usePM, boolean usePrec, boolean useNut, boolean useAber)
    {

      double[] radec = {ra, dec};

      if(direction)
      {

        /* Include proper motion for ra and dec */

        if (usePM) radec = ProperMotion(epoch, jd, radec[0], radec[1], pm_ra, pm_dec);

        /* Precess ra and dec from Epoch to the JD */

        if (usePrec) radec = Precession(epoch, jd, radec[0], radec[1], 1);

        /* Include nutation for ra and dec */

        if (useNut) radec = Nutation(jd, radec[0], radec[1], leapSecs, 1);

        /* Include aberration for ra and dec */

        if (useAber) radec = Aberration(jd, radec[0], radec[1], leapSecs, 1);

        /* Return the new JD based coordinates */

      }
      else
      {

        /* Remove aberration for ra and dec  */

        if (useAber) radec = Aberration(jd, radec[0], radec[1], leapSecs, -1);

        /* Remove nutation for ra and dec */

        if (useNut) radec = Nutation(jd, radec[0], radec[1], leapSecs, -1);

        /* Remove precession to JD to Epoch */

        if (usePrec) radec = Precession(epoch, jd, radec[0], radec[1], -1);

        /* Remove proper motion from ra and dec */

        if (usePM) radec = ProperMotion(epoch, jd, radec[0], radec[1], -pm_ra, -pm_dec);

        /* Return the J2000 coordinates */
        
      }
      return radec;
    }

/**
 * Evaluate proper motion from reference epoch to JD for coordinates in the catalog epoch.
 *
 * @param epoch Reference epoch (i.e. 2000.0)
 * @param jd Julian Date of interest
 * @param ra Right Ascension in hours
 * @param dec Declinations in degrees
 * @param pm_ra Proper Motion in RA in units of milli-arc-seconds per year
 * @param pm_dec Proper Motion in DEC in units of milli-arc-seconds per year
 * @return  Modified ra and dec as {ra,dec}
 */
public static double[] ProperMotion(double epoch, double jd, double ra, double dec, double pm_ra, double pm_dec)
    {
      double ra_in, dec_in, ra_out, dec_out;
      double ut, jdfixed, T;
      int year, month, day;
      double[] radec = {ra, dec};

      /* JD for the fixed epoch */

      year = (int)epoch;
      month = 1;
      day = 1;
      ut = 12.0;
      jdfixed = CalcJD ( year, month, day, ut ) + frac(epoch)*365.25;


      /* Elapsed JD years */

      T = (jd - jdfixed)/365.25;

      /* Fetch the input values for ra and dec*/

      ra_in = ra;
      dec_in = dec;

      /* Calculate the new coordinates with proper motion */

      ra_out = ra_in + T*pm_ra/(15.0*1000.0*3600.0*cos(dec_in*PI/180.0)); 
      dec_out = dec_in + T*pm_dec/(1000.0*3600.0);

      /* Check for range and adjust to -90 -> +90 and 0 -> 24 and if needed */

      if (dec_out > 90. )
      {
        dec_out = 180. - dec_out;
        ra_out = ra_out + 12.;
      }
      if (dec_out < -90. )
      {
        dec_out = -180. - dec_out;
        ra_out = ra_out + 12.;
      }

      ra_out = Map24(ra_out);

      radec[0] = ra_out;
      radec[1] = dec_out;
      return radec;
    }

/**
 * Add or remove nutation for this Julian Date.
 *
 * @param jd Julian Date of interest
 * @param ra Right Ascension in hours
 * @param dec Declination in degrees
 * @param leapSecs Leap Seconds (TAI - UTC)
 * @param dirflag 1 = add nutation, -1 = remove nutation
 * @return Modified ra and dec as {ra,dec}
 */
public static double[] Nutation(double jd, double ra, double dec, double leapSecs, int dirflag)
    {
      double ra_in, dec_in;
      double elong, elat, dlong;
      double dpsi, deps, eps0;
      double dra, ddec;
      double dir;
      double[] radec = {ra, dec};
      double[] elonglat = {0, 0};

      /* Routine will add nutation by default */

      dir = 1.0;
      if (dirflag < 0)
      {
        dir = -1.0;
      }

      ra_in = ra;
      dec_in = dec;

      /* Near the celestial pole convert to ecliptic coordinates */

      if(abs(dec_in) > (double) 85.)
      {
        elonglat = CelestialToEcliptical(jd, ra_in, dec_in, leapSecs);

        elong = elonglat[0];
        elat = elonglat[1];

        dlong = dir*NLongitude(jd, leapSecs);
        elong += dlong;

        radec = EclipticalToCelestial(jd, elong, elat, leapSecs);
      }
      else
      {
        dpsi = dir*NLongitude(jd, leapSecs);
        eps0 = MeanObliquity(jd, leapSecs);
        deps = dir*NObliquity(jd, leapSecs);
        dra = (cos(eps0*PI/180.) +
          sin(eps0*PI/180.)*sin(ra_in*PI/12.)*tan(dec_in*PI/180.))*dpsi -
          cos(ra_in*PI/12.)*tan(dec_in*PI/180.)*deps;
        dra /= 15.;
        ddec = sin(eps0*PI/180.)*cos(ra_in*PI/12.)*dpsi +
          sin(ra_in*PI/12.)*deps;
        radec[0] = ra_in + dra;
        radec[1] = dec_in + ddec;
      }

      return radec;

    }


/**
 * Add or remove stellar aberration for this Julian Date.
 *
 * The routine is exact for adding stellar aberration to celestial
 * coordinates.  It is approximate for removing aberration from
 * apparent coordinates since the amount of aberration depends on
 * true celestial coordinates, not the apparent coordinates.  For
 * greater accuracy the routine should iterate a few times.
 *
 * @param jd Julian Date
 * @param ra Right Ascension in hours
 * @param dec Declination in degrees
 * @param leapSecs Leap Seconds (TAI - UTC)
 * @param dirflag 1 = add aberration, -1 = remove aberration
 * @return Modified ra and dec as {ra,dec}
 */
public static double[] Aberration(double jd, double ra, double dec, double leapSecs, int dirflag)
    {
      double ra_in, dec_in;
      double elong, elat;
      double dlong, dlat;
      double ec, lp, glsun, ka, eps0;
      double c1, c2;
      double dra, ddec;
      double dir;
      double[] radec = {0, 0};
      double[] elonglat = {0, 0};

      /* Routine will add stellar aberration by default */

      dir = 1.0;
      if (dirflag < 0)
      {
        dir = -1.0;
      }

      ka = 20.49552;
      glsun = LongitudeSun(jd,leapSecs);
      ec = Eccentricity(jd, leapSecs);
      lp = LongitudePerihelion(jd, leapSecs);
      eps0 = MeanObliquity(jd, leapSecs);
      ra_in = ra;
      dec_in = dec;

      /* Near the celestial pole convert to ecliptic coordinates */

      if(abs(dec_in) > (double) 85.)
      {
        elonglat = CelestialToEcliptical(jd, ra_in, dec_in, leapSecs);
        elong = elonglat[0];
        elat = elonglat[1];

        c1 = -cos((glsun - elong)*PI/180.) + ec*cos((lp - elong)*PI/180.);
        c2 = cos(elat*PI/180.);

        /* Indeterminate dlong near the ecliptic pole */

        if(c2 < (double) 0.0001) c2 = 0.0001;
        dlong = dir*ka*(c1/c2)/3600.;

        c1 = - sin(elat*PI/180.);
        c2 = sin((glsun - elong)*PI/180.) - ec*sin((lp - elong)*PI/180.);
        dlat = dir*ka*c1*c2/3600.;
        elong += dlong;
        elat += dlat;

        /* Check for range and adjust to -90 -> +90 and 0 -> 360 if needed */

        elat = Map180(elat);
        if (elat > 90. )
        {
          elat = 180. - elat;
          elong += 180.;
        }
        if (elat < -90. )
        {
          elat = -180. - elat;
          elong += 180.;
        }
        elong = Map360(elong);

        radec = EclipticalToCelestial(jd, elong, elat, leapSecs);
      }
      else
      {
        dra = (-cos(ra_in*PI/12.)*cos(glsun*PI/180.)*cos(eps0*PI/180.)
          - sin(ra_in*PI/12.)*sin(glsun*PI/180.)
          + ec*cos(ra_in*PI/12.)*cos(lp*PI/180.)*cos(eps0*PI/180)
          + ec*sin(ra_in*PI/12.)*sin(lp*PI/180.))/cos(dec_in*PI/180.);

        dra = dir*ka*dra;

        dra /= (15.*3600.);

        ddec = -cos(glsun*PI/180)*cos(eps0*PI/180.)
          *(tan(eps0*PI/180)*cos(dec_in*PI/180.)
          - sin(ra_in*PI/12.)*sin(dec_in*PI/180.))
          - cos(ra_in*PI/12.)*sin(dec_in*PI/180.)*sin(glsun*PI/180)
          + ec*cos(lp*PI/180.)*cos(eps0*PI/180.)
          *(tan(eps0*PI/180.)*cos(dec_in*PI/180.)
          - sin(ra_in*PI/12.)*sin(dec_in*PI/180.))
          + ec*cos(ra_in*PI/12.)*sin(dec_in*PI/180.)*sin(lp*PI/180.);

        ddec = dir*ka*ddec;
        ddec /= 3600.;

        radec[0] = ra_in + dra;
        radec[1] = dec_in +ddec;
      }

      return radec;

    }


/**
 * Correct altitude for atmospheric refraction.
 * Valid above 15 degrees.  Below 15 degrees uses 15 degree value.
 * @param alt altitude
 * @param dirflag positive for real to apparent, negative for apparent to real
 * @return corrected altitude
 */
public static double Refraction(double alt, int dirflag)
{
  double dalt=0.0, arg;
  double SiteTemperature = 10.0;  // degrees C
  double SitePressure = 760.0;    // Torr (~mmHg)

  /* Calculate the change in apparent altitude due to refraction */
  /* Using 15 degree value for altitudes below 15 degrees */

  /* Real to apparent */
  /* Object appears to be higher due to refraction */

  if (dirflag >= 0)
  {
    if (alt >= 15.0)
    {
      arg = (90.0 - alt)*PI/180.0;
      dalt = tan(arg);
      dalt = 58.276 * dalt - 0.0824 * dalt * dalt * dalt;
      dalt /= 3600.;
      dalt = dalt * (SitePressure/(760.))*(283./(273.+SiteTemperature));
    }
   else if (alt >= 0.0)
    {
      arg = (90.0 - 15.0)*PI/180.0;
      dalt = tan(arg);
      dalt = 58.276 * dalt - 0.0824 * dalt * dalt * dalt;
      dalt /= 3600.;
      dalt = dalt * (SitePressure/(760.))*(283./(273.+SiteTemperature));
    }
    alt += dalt;
  }

  /* Apparent to real */
  /* Object appears to be higher due to refraction */

  else if (dirflag < 0)
  {
    if (alt >= 15.0)
    {
      arg = (90.0 - alt)*PI/180.0;
      dalt = tan(arg);
      dalt = 58.294 * dalt - 0.0668 * dalt * dalt * dalt;
      dalt /= 3600.;
      dalt = dalt * (SitePressure/(760.))*(283./(273.+SiteTemperature));
    }
   else if (alt >= 0.0)
    {
      arg = (90.0 - 15.0)*PI/180.0;
      dalt = tan(arg);
      dalt = 58.294 * dalt - 0.0668 * dalt * dalt * dalt;
      dalt /= 3600.;
      dalt = dalt * (SitePressure/(760.))*(283./(273.+SiteTemperature));
    }

    alt -= dalt;
  }
  return alt;

}



/**
 * Convert local Hour Angle and Declination to local Altitude and Azimuth.
 * @param ha Hour Angle
 * @param dec Declination
 * @param lat geographic latitude
 * @return Altitude and Azimuth as (alt, az} where due north is zero and azimuth increases from north to east
 */
public static double[] EquatorialToHorizontal(double ha, double dec, double lat)
    {
      double phi, altitude, azimuth ;
      double[] altaz = {0,0};

      ha *= PI/12.;
      phi = lat*PI/180.;
      dec = dec*PI/180.;
      altitude = asin(sin(phi)*sin(dec)+cos(phi)*cos(dec)*cos(ha));
      altitude *= 180.0/PI;
      azimuth = atan2(-cos(dec)*sin(ha),
        sin(dec)*cos(phi)-sin(phi)*cos(dec)*cos(ha));
      azimuth *= 180.0/PI;

      azimuth = Map360(azimuth);

      altaz[0] = altitude;
      altaz[1] = azimuth;
      return altaz;
    }


/**
 * Convert local Altitude, Azimuth, and Latitude to local Hour Angle and Declination.
 *
 * @param alt Altitude
 * @param az Azimuth where due north is zero and azimuth increases from north to east
 * @param lat Latitude (north positive)
 * @return Hour Angle and Declination as {ha, dec}
 */
public static double[] HorizontalToEquatorial(double alt, double az, double lat)
    {
      double phi, hourangle, declination ;
      double[] hadec = {0,0};

      alt *= PI/180.;
      az = Map360(az);
      az *= PI/180.;
      phi = lat*PI/180.;

      hourangle = atan2(-sin(az)*cos(alt),cos(phi)*sin(alt)-sin(phi)*cos(alt)*cos(az));
      declination = asin(sin(phi)*sin(alt) + cos(phi)*cos(alt)*cos(az));
      hourangle = Map12(hourangle*12./PI);
      declination = Map180(declination*180./PI);

      /* Test for hemisphere */

      if(declination > 90.)
      {
        hadec[0] = Map12(hourangle + 12.);
        hadec[1] = 180. - declination;
      }
      else if(declination < -90.)
      {
        hadec[0] = Map12(hourangle + 12.);
        hadec[1] = declination + 180;
      }
      else
      {
        hadec[0] = hourangle;
        hadec[1] = declination;
      }
      return hadec;
    }


/**
 * Convert RA, Dec, Latitude, Longitude, and UT time to Altitude and Azimuth,.
 * @param ra Right Ascension
 * @param dec Declination
 * @param lat geographic latitude (north positive)
 * @param lon geographic longitude (east positive)
 * @param utdate UT date and time as {yr, mo, day, ut (in hours)}
 * @param leapSecs Leap Seconds (TAI - UTC)
 * @param includeRefraction set true to apply atmospheric refraction correction to altitude
 * @return Altitude and Azimuth as (alt, az} where due north is zero and azimuth increases from north to east
 */
public static double[] CelestialToHorizontal(double ra, double dec, double lat, double lon, double[] utdate, double leapSecs, boolean useRefr)
    {
      double phi, altitude, azimuth ;
      double[] altaz = {0,0};
      double ha = Map12(CalcLST((int)utdate[0], (int)utdate[1], (int)utdate[2], utdate[3], lon, leapSecs) - ra);
      ha *= PI/12.;
      phi = lat*PI/180.;
      dec = dec*PI/180.;
      altitude = asin(sin(phi)*sin(dec)+cos(phi)*cos(dec)*cos(ha));
      altitude *= 180.0/PI;
      azimuth = atan2(-cos(dec)*sin(ha), sin(dec)*cos(phi)-sin(phi)*cos(dec)*cos(ha));
      azimuth *= 180.0/PI;

      azimuth = Map360(azimuth);

      altaz[0] = altitude;
      altaz[1] = azimuth;
      if (useRefr) altaz[0] = Map180(SkyAlgorithms.Refraction(altaz[0], 1));
      return altaz;
    }


/**
 * Convert local Altitude, Azimuth, Latitude, Longitude and time to RA and Dec.
 *
 * @param alt Altitude
 * @param az Azimuth where due north is zero and azimuth increases from north to east
 * @param lat Latitude (north positive)
 * @param lon Longitude (east positive)
 * @param utdate UT date and time as {yr, mo, day, ut (in hours)}
 * @param leapSecs Leap Seconds (TAI - UTC)
 * @param includeRefraction set true to remove atmospheric refraction correction from altitude before converting
 * @return Right Ascension and Declination as {ra, dec}
 */
public static double[] HorizontalToCelestial(double alt, double az, double lat, double lon, double[] utdate, double leapSecs, boolean useRefr)
    {
      double phi, hourangle, declination, ra ;
      double[] hadec = {0.0,0.0};
      double[] radec = {0.0,0.0};

      if (useRefr) alt = SkyAlgorithms.Refraction(alt, -1);
      alt = Map180(alt);
      alt *= PI/180.;
      az = Map360(az);
      az *= PI/180.;
      phi = lat*PI/180.;

      hourangle = atan2(-sin(az)*cos(alt),cos(phi)*sin(alt)-sin(phi)*cos(alt)*cos(az));
      declination = asin(sin(phi)*sin(alt) + cos(phi)*cos(alt)*cos(az));
      hourangle = Map12(hourangle*12./PI);
      declination = Map180(declination*180./PI);

      /* Test for hemisphere */

      if(declination > 90.)
      {
        hadec[0] = Map12(hourangle + 12.);
        hadec[1] = 180. - declination;
      }
      else if(declination < -90.)
      {
        hadec[0] = Map12(hourangle + 12.);
        hadec[1] = declination + 180;
      }
      else
      {
        hadec[0] = hourangle;
        hadec[1] = declination;
      }

      radec[0] = Map24(CalcLST((int)utdate[0], (int)utdate[1], (int)utdate[2], utdate[3], lon, leapSecs) - hadec[0]);
      radec[1] = hadec[1];

      return radec;
    }

/**
 * Convert celestial to ecliptical coordinates for the Julian Date.
 *
 * @param jd Julian Date of interest
 * @param ra Right Ascension in hours
 * @param dec Declination in degrees
 * @param leapSecs Leap Seconds (TAI - UTC)
 * @return Ecliptical latitude and longitude as {elat, elong}
 */
public static double[] CelestialToEcliptical(double jd, double ra, double dec, double leapSecs)
    {
      double elong, elat, eps;
      double[] elonglat = {0, 0};

      ra *= PI/12.;
      dec *= PI/180.;
      eps = MeanObliquity(jd, leapSecs);
      eps = eps*PI/180;
      elong = atan2(sin(ra)*cos(eps) + tan(dec)*sin(eps),cos(ra));
      elong = Map360(elong*180./PI);
      elat = asin(sin(dec)*cos(eps)-cos(dec)*sin(eps)*sin(ra));
      elat = Map180(elat*180./PI);

      /* Test for hemisphere */

      if(elat > 90.)
      {
        elonglat[0] = Map360(elong + 180.);
        elonglat[1] = 180. - elat;
      }
      else if(elat < -90.)
      {
        elonglat[0] = Map360(elong + 180.);
        elonglat[1] = -180 - elat;
      }
      else
      {
        elonglat[0] = elong;
        elonglat[1] = elat;
      }
     return elonglat;
    }


/**
 * Convert ecliptical to celestial coordinates for the Julian Date.
 * @param jd Julian Date of interest
 * @param lambda Ecliptical longitude in degrees
 * @param beta Ecliptical latitude in degrees
 * @param leapSecs Leap Seconds (TAI - UTC)
 * @return RA and DEC as {ra, dec}
 */
public static double[] EclipticalToCelestial(double jd, double lambda, double beta, double leapSecs)
    {
      double ra_out, dec_out, eps;
      double[] radec = {0, 0};
      lambda *= PI/180.;
      beta *= PI/180.;
      eps = MeanObliquity(jd, leapSecs)*PI/180;
      ra_out = atan2(sin(lambda)*cos(eps) - tan(beta)*sin(eps), cos(lambda));
      ra_out = Map24(ra_out*12./PI);
      dec_out = asin(sin(beta)*cos(eps) + cos(beta)*sin(eps)*sin(lambda));
      dec_out = Map180(dec_out*180./PI);

      /* Test for hemisphere */

      if(dec_out > 90.)
      {
        radec[0] = Map24(ra_out + 12.);
        radec[1] = 180. - dec_out;
      }
      else if(dec_out < -90.)
      {
        radec[0] = Map24(ra_out + 12.);
        radec[1] = -180 - dec_out;
      }
      else
      {
        radec[0] = ra_out;
        radec[1] = dec_out;
      }
    return radec;
    }


/**
 * True obliquity of the ecliptic for the JD in degrees.
 * @param jd Julian Date
 * @param leapSecs Leap Seconds (TAI - UTC)
 * @return True Obliquity of the ecliptic in degrees
 */
public static double TrueObliquity(double jd, double leapSecs)
    {
      double eps, eps0, deps;

      eps0 = MeanObliquity(jd, leapSecs);
      deps = NObliquity(jd, leapSecs);
      eps = eps0 + deps;

      return (eps);
    }


    /* Routines for dynamical astronomy
     *
     *
     * Time matters ...
     *
     * TAI is the International Atomic Time standard.
     * TT is the terrestrial (dynamical) time for ephemeris calculations.
     * TT runs (very nearly) at the SI second on the Earth's rotating geoid.
     *
     * The difference TT - TAI is 32.184 seconds and does not change.
     *
     * UTC is Coordinated Universal Time, by which we set network clocks.
     * UTC runs at the TAI rate.
     * UT1 runs at the  the slowly decreasing mean solar day rate.
     * The UT1 counter must read 12h at each mean solar noon,
     *   but since the rotation of the Earth is slowing,
     *   by the UTC counter this event is at an increasingly later time.
     *
     * The difference UTC - UT1 < 0.9 seconds is maintained by
     *   not incrementing the UTC counter for 1 second when necessary.
     *   This uncounted second is called a Leap Second.
     *
     * Whenever the UTC clock counter is not incremented for one tick,
     *   UTC falls behind TT an additional second. TAI and TT are
     *   always incremented.
     *
     * TT = TAI + 32.184 is the basis for the ephemeris here.
     * The difference between TAI and UTC is the number of leap seconds
     *   accumulated since the clocks were initially synchronized.
     * We define LEAPSECONDS = TAI - UTC in the header file.
     * It was incremented to 32 on January 1.0, 1999.
     * It will be 33 after January 1.0, 2006.
     * Here dt = TT - UTC = LEAPSECONDS + 32.184
     *
     */


/**
 * Mean obliquity of the ecliptic for the Julian Date in degrees.
 * @param jd Julian Date
 * @param leapSecs Leap Seconds (TAI - UTC)
 * @return Mean Obliquity of the ecliptic in degrees
 */
public static double MeanObliquity(double jd, double leapSecs)
    {
      double eps0;
      double dt, t;

      dt = leapSecs;
      dt += 32.184;

      /* Change units to centuries */

      dt /= (36525 * 24. * 60. * 60.);

      /* Julian centuries for the JD from a base epoch 2000.0 */

      t = (jd - 2451545.0) / 36525.0;

      /* Correct for dt = tdt - ut1 (not significant) */

      t += dt ;

      /* Mean obliquity in degrees */

      eps0 = 23.0+26./60+21.448/3600.;
      eps0 += (- 46.8150*t - 0.00059*t*t + 0.001813*t*t*t)/3600.;

      return (eps0);
    }


/**
 * Nutation of the obliquity of the ecliptic for the EOD in degrees.
 * @param jd Julian Date
 * @param leapSecs Leap Seconds (TAI - UTC)
 * @return Nutation of the obliquity of the ecliptic in degrees
 */
public static double NObliquity(double jd, double leapSecs)
    {
      double deps, lsun, lmoon, omega;
      double dt, t;

      dt = leapSecs;
      dt += 32.184;

      /* Change units to centuries */

      dt /= (36525 * 24. * 60. * 60.);

      /* Julian centuries for the JD from a base epoch 2000.0 */

      t = (jd - 2451545.0) / 36525.0;

      /* Correct for dt = tt - ut1  */

      t += dt ;

      /* Longitude of the ascending node of the Moon's mean orbit in degrees */

      omega = 125.04452 - 1934.136261*t + 0.0020708*t*t + t*t*t/450000.;

      /* Mean longitudes of the Sun and the Moon in degrees */

      lsun = LongitudeSun(jd, leapSecs);
      lmoon = Map360(218.31654591 + 481267.88134236*t - 0.00163*t*t + t*t*t/538841. - t*t*t*t/65194000.);

      /* Convert to radians */

      omega = omega*PI/180.;
      lsun = lsun*PI/180.;
      lmoon = lmoon*PI/180.;

      /* Nutation of the obliquity in seconds of arc for the JD */

      deps = 9.20*cos(omega) + 0.57*cos(2.*lsun) +
        0.1*cos(2.*lmoon) - 0.09*cos(2.*omega);

      /* Convert to degrees */

      deps /= 3600.;

      return (deps);
    }


/**
 * Nutation of the longitude of the ecliptic for the EOD in degrees.
 * @param jd Julian Date
 * @param leapSecs Leap Seconds (TAI - UTC)
 * @return Nutation of the longitude of the ecliptic in degrees
 */
public static double NLongitude(double jd, double leapSecs)
    {
      double dpsi, lsun, lmoon, omega;
      double dt, t;

      dt = leapSecs;
      dt += 32.184;

      /* Change units to centuries */

      dt /= (36525 * 24. * 60. * 60.);

      /* Julian centuries for the EOD from a base epoch 2000.0 */

      t = (jd - 2451545.0) / 36525.0;

      /* Correct for dt = tt - ut1  */

      t += dt ;

      /* Longitude of the ascending node of the Moon's mean orbit */

      omega = Map360(125.04452 - 1934.136261*t + 0.0020708*t*t + t*t*t/450000.);

      /* Mean longitude of the Moon */

      lmoon = Map360(218.31654591 + 481267.88134236*t
        - 0.00163*t*t + t*t*t/538841. - t*t*t*t/65194000.);

      /* Mean longitude of the Sun */

      lsun = LongitudeSun(jd, leapSecs);

      /* Convert to radians */

      omega = omega*PI/180.;
      lsun = lsun*PI/180.;
      lmoon = lmoon*PI/180.;

      /* Nutation in longitude in seconds of arc for the EOD */

      dpsi = -17.20*sin(omega) - 1.32*sin(2.*lsun) -
        0.23*sin(2.*lmoon) + 0.21*sin(2.*omega);

      /* Convert to degrees */

      dpsi /= 3600.;

      return (dpsi);
    }


/**
 * True geometric solar longitude for the JD in degrees.
 * @param jd Julian Date
 * @param leapSecs Leap Seconds (TAI - UTC)
 * @return True geometric solar longitude in degrees
 */
public static double LongitudeSun(double jd, double leapSecs)
    {
      double lsun, glsun, msun, csun;
      double dt, t;

      dt = leapSecs;
      dt += 32.184;

      /* Change units to centuries */

      dt /= (36525 * 24. * 60. * 60.);

      /* Julian centuries for the EOD from a base epoch 2000.0 */

      t = (jd - 2451545.0) / 36525.0;

      /* Correct for dt = tt - ut1  */

      t += dt ;

      lsun = Map360(280.46645 + 36000.76983*t + 0.0003032*t*t);

      /* Mean anomaly */

      msun = Map360(357.52910 + 35999.05030*t - 0.0001559*t*t -
        0.00000048*t*t*t);

      msun = msun*PI/180.;

      /* Sun's center */

      csun = (1.9146000 - 0.004817*t - 0.000014*t*t)*sin(msun)
        + (0.019993 - 0.000101*t)*sin(2.*msun)
        + 0.000290*sin(3.*msun);

      /* True geometric longitude */

      glsun = Map360(lsun  + csun);

      return (glsun);
    }


/**
 * Eccentricity of the Earth's orbit at the Julian Date.
 * @param jd Julian Date
 * @param leapSecs Leap Seconds (TAI - UTC)
 * @return Eccentricity of the Earth's orbit
 */
public static double Eccentricity(double jd, double leapSecs)
    {
      double ec;
      double dt, t;


      dt = leapSecs;
      dt += 32.184;

      /* Change units to centuries */

      dt /= (36525 * 24. * 60. * 60.);

      /* Julian centuries for the EOD from a base epoch 2000.0 */

      t = (jd - 2451545.0) / 36525.0;

      /* Correct for dt = tt - ut1  */

      t += dt ;

      ec = 0.016708617 - 0.000042037*t - 0.0000001236*t*t;

      return (ec);
    }

/**
 * Longitude of perihelion of the Earth's orbit in degrees for the Julian Date.
 * @param jd Julian Date
 * @param leapSecs Leap Seconds (TAI - UTC)
 * @return Longitude of perihelion of the Earth's orbit in degrees
 */
public static double LongitudePerihelion(double jd, double leapSecs)
    {
      double lp;
      double dt, t;

      dt = leapSecs;
      dt += 32.184;

      /* Change units to centuries */

      dt /= (36525 * 24. * 60. * 60.);

      /* Julian centuries for the EOD from a base epoch 2000.0 */

      t = (jd - 2451545.0) / 36525.0;

      /* Correct for dt = tt - ut1  */

      t += dt ;

      /* Longitude of perihelion */

      lp = 102.93735 + 0.71953*t + 0.00046*t*t;
      lp = Map360(lp);

      return (lp);
    }

/**
 * Convert J2000 RA/Dec to B1950 RA/Dec (RA in hours, Dec in degrees)
 * @param ra J2000 Right ascension in hours
 * @param dec J2000 Declination in degrees
 * @param rapm Proper motion in right ascension (mas/yr)
 * @param decpm Proper motion in declination (mas/yr)
 * @param usePM Include proper motion in conversion
 * @return B1950 RA in hours Dec in degrees as {ra, dec}
 */
public static double[] J2000toB1950 (double ra, double dec, double rapm, double decpm, boolean usePM)
    {
    //  This method is heavily based on the WCSTools routine fk524pv

    /*  This routine converts stars from the IAU 1976 FK5 Fricke
        system, to the old Bessel-Newcomb FK4 system, using Yallop's
        implementation (see ref 2) of a matrix method due to Standish
        (see ref 3).  The numerical values of ref 2 are used canonically.

     *  Conversion from other than Julian epoch 2000.0 to other than Besselian
        epoch 1950.0 will require use of the appropriate precession, proper
        motion, and e-terms routines before and/or after fk524 is called.

     *  In the FK4 catalogue the proper motions of stars within 10 degrees
        of the poles do not embody the differential e-term effect and should,
        strictly speaking, be handled in a different manner from stars outside
        these regions.  however, given the general lack of homogeneity of the
        star data available for routine astrometry, the difficulties of handling
        positions that may have been determined from astrometric fields spanning
        the polar and non-polar regions, the likelihood that the differential
        e-terms effect was not taken into account when allowing for proper motion
        in past astrometry, and the undesirability of a discontinuity in the
        algorithm, the decision has been made in this routine to include the
        effect of differential e-terms on the proper motions for all stars,
        whether polar or not, at epoch 2000, and measuring on the sky rather
        than in terms of dra, the errors resulting from this simplification are
        less than 1 milliarcsecond in position and 1 milliarcsecond per century
        in proper motion.

        References:

          1  "Mean and apparent place computations in the new IAU System.
              I. The transformation of astrometric catalog systems to the
          equinox J2000.0." Smith, C.A.; Kaplan, G.H.; Hughes, J.A.;
          Seidelmann, P.K.; Yallop, B.D.; Hohenkerk, C.Y.
          Astronomical Journal vol. 97, Jan. 1989, p. 265-273.

          2  "Mean and apparent place computations in the new IAU System.
          II. Transformation of mean star places from FK4 B1950.0 to
          FK5 J2000.0 using matrices in 6-space."  Yallop, B.D.;
          Hohenkerk, C.Y.; Smith, C.A.; Kaplan, G.H.; Hughes, J.A.;
          Seidelmann, P.K.; Astronomical Journal vol. 97, Jan. 1989,
          p. 274-279.

          3  Seidelmann, P.K. (ed), 1992.  "Explanatory Supplement to
             the Astronomical Almanac", ISBN 0-935702-68-7.

          4  "Conversion of positions and proper motions from B1950.0 to the
          IAU system at J2000.0", Standish, E.M.  Astronomy and
          Astrophysics, vol. 115, no. 1, Nov. 1982, p. 20-22.

       P.T.Wallace   Starlink   19 December 1993
       Doug Mink     Smithsonian Astrophysical Observatory 1 November 2000 */


/*  Constant vector and matrix (by columns)
    These values were obtained by inverting C.Hohenkerk's forward matrix
    (private communication), which agrees with the one given in reference
    2 but which has one additional decimal place.  */
    
    double[] a  = {-1.62557e-6, -0.31919e-6, -0.13843e-6 };
    double[] ad = {1.245e-3,  -1.580e-3,  -0.659e-3};
    double d2pi = 6.283185307179586476925287;	/* two PI */
    double tiny = 1.e-30; /* small number to avoid arithmetic problems */

    /* FK524  convert J2000 FK5 star data to B1950 FK4
       based on Starlink sla_fk524 by P.T.Wallace 27 October 1987 */

    double[][] emi = {
        {	 0.9999256795,		/* emi[0][0] */
         0.0111814828,		/* emi[0][1] */
         0.0048590039,		/* emi[0][2] */
        -0.00000242389840,	/* emi[0][3] */
        -0.00000002710544,	/* emi[0][4] */
        -0.00000001177742 },	/* emi[0][5] */

        {	-0.0111814828,		/* emi[1][0] */
         0.9999374849,		/* emi[1][1] */
        -0.0000271771,		/* emi[1][2] */
         0.00000002710544,	/* emi[1][3] */
        -0.00000242392702,	/* emi[1][4] */
         0.00000000006585 },	/* emi[1][5] */

        {	-0.0048590040,		/* emi[2][0] */
        -0.0000271557,		/* emi[2][1] */
         0.9999881946,		/* emi[2][2] */
         0.00000001177742,	/* emi[2][3] */
         0.00000000006585,	/* emi[2][4] */
        -0.00000242404995 },	/* emi[2][5] */

        {	-0.000551,		/* emi[3][0] */
         0.238509,		/* emi[3][1] */
        -0.435614,		/* emi[3][2] */
         0.99990432,		/* emi[3][3] */
         0.01118145,		/* emi[3][4] */
         0.00485852 },		/* emi[3][5] */

        {	-0.238560,		/* emi[4][0] */
        -0.002667,		/* emi[4][1] */
         0.012254,		/* emi[4][2] */
        -0.01118145,		/* emi[4][3] */
         0.99991613,		/* emi[4][4] */
        -0.00002717 },		/* emi[4][5] */

        {	 0.435730,		/* emi[5][0] */
        -0.008541,		/* emi[5][1] */
         0.002117,		/* emi[5][2] */
        -0.00485852,		/* emi[5][3] */
        -0.00002716,		/* emi[5][4] */
         0.99996684 }		/* emi[5][5] */
        };

        double r2000,d2000;		/* J2000.0 ra,dec (hours,radians) */
        double r1950,d1950;		/* B1950.0 ra,dec (hours,radians) */

        /* Miscellaneous */
        double ur=0.0,ud=0.0;
        double sr, cr, sd, cd, x, y, z, w, wd;
        double[] v1 = new double[6];
        double[] v2 = new double[6];
        double xd,yd,zd;
        double rxyz, rxysq, rxy;
        double dra,ddec;
        int	i,j;
        int	diag = 0;

        /* Constants */
        double zero = (double) 0.0;
        double vf = 21.095;	/* Km per sec to AU per tropical century */
                /* = 86400 * 36524.2198782 / 149597870 */

        /* Convert J2000 RA from hours to radians and Dec from degrees to radians */
        r2000 = ra*PI/12.0;
        d2000 = dec*PI/180.0;

        /* Convert J2000 RA and Dec proper motion from mas/year to arcsec/tc */
        if (usePM)
            {
            ur = rapm  / (10*cos(d2000));  // *100/(1000*cos(d2000)
            ud = decpm / 10.0;  // *100/1000
            }

        /* Spherical to Cartesian */
        sr = sin (r2000);
        cr = cos (r2000);
        sd = sin (d2000);
        cd = cos (d2000);

        x = cr * cd;
        y = sr * cd;
        z = sd;

        v1[0] = x;
        v1[1] = y;
        v1[2] = z;

        if (ur != zero || ud != zero) {
        v1[3] = -(ur*y) - (cr*sd*ud);
        v1[4] =  (ur*x) - (sr*sd*ud);
        v1[5] =          (cd*ud);
        }
        else {
        v1[3] = zero;
        v1[4] = zero;
        v1[5] = zero;
        }

        /* Convert position + velocity vector to bn system */
        for (i = 0; i < 6; i++) {
        w = zero;
        for (j = 0; j < 6; j++) {
            w = w + emi[i][j] * v1[j];
            }
        v2[i] = w;
        }

        /* Vector components */
        x = v2[0];
        y = v2[1];
        z = v2[2];

        /* Magnitude of position vector */
        rxyz = sqrt (x*x + y*y + z*z);

        /* Apply e-terms to position */
        w = (x * a[0]) + (y * a[1]) + (z * a[2]);
        x = x + (a[0] * rxyz) - (w * x);
        y = y + (a[1] * rxyz) - (w * y);
        z = z + (a[2] * rxyz) - (w * z);

        /* Recompute magnitude of position vector */
        rxyz = sqrt (x*x + y*y + z*z);

        /* Apply e-terms to position and velocity */
        x = v2[0];
        y = v2[1];
        z = v2[2];
        w = (x * a[0]) + (y * a[1]) + (z * a[2]);
        wd = (x * ad[0]) + (y * ad[1]) + (z * ad[2]);
        x = x + (a[0] * rxyz) - (w * x);
        y = y + (a[1] * rxyz) - (w * y);
        z = z + (a[2] * rxyz) - (w * z);
        xd = v2[3] + (ad[0] * rxyz) - (wd * x);
        yd = v2[4] + (ad[1] * rxyz) - (wd * y);
        zd = v2[5] + (ad[2] * rxyz) - (wd * z);

        /*  Convert to spherical  */
        rxysq = (x * x) + (y * y);
        rxy = sqrt (rxysq);

        /* Convert back to spherical coordinates */
        if (x == zero && y == zero)
        r1950 = zero;
        else {
        r1950 = atan2 (y,x);
        if (r1950 < zero)
            r1950 = r1950 + d2pi;
        }
        d1950 = atan2 (z,rxy);

        /* Return results */

        ra = r1950*12/PI;
        dec = d1950*180/PI;

        return new double[] {ra, dec};
    }


/**
 * Convert B1950 RA/Dec to J2000 RA/Dec (RA in hours, Dec in degrees)
 * @param ra B1950 Right ascension in hours
 * @param dec B1950 Declination in degrees
 * @param rapm Proper motion in right ascension (mas/yr)
 * @param decpm Proper motion in declination (mas/yr)
 * @param usePM Include proper motion in conversion
 * @return J2000 RA in hours Dec in degrees as {ra, dec}
 */
public static double[] B1950toJ2000(double ra, double dec, double rapm, double decpm, boolean usePM)
    {
    //  This method is heavily based on the WCSTools routine fk425pv

    /*  This routine converts stars from the old Bessel-Newcomb FK4 system
        to the IAU 1976 FK5 Fricke system, using Yallop's implementation
        (see ref 2) of a matrix method due to Standish (see ref 3).  The
        numerical values of ref 2 are used canonically.

     *  Conversion from other than Besselian epoch 1950.0 to other than Julian
        epoch 2000.0 will require use of the appropriate precession, proper
        motion, and e-terms routines before and/or after fk425 is called.

     *  In the FK4 catalogue the proper motions of stars within 10 degrees
        of the poles do not embody the differential e-term effect and should,
        strictly speaking, be handled in a different manner from stars outside
        these regions.  however, given the general lack of homogeneity of the
        star data available for routine astrometry, the difficulties of handling
        positions that may have been determined from astrometric fields spanning
        the polar and non-polar regions, the likelihood that the differential
        e-terms effect was not taken into account when allowing for proper motion
        in past astrometry, and the undesirability of a discontinuity in the
        algorithm, the decision has been made in this routine to include the
        effect of differential e-terms on the proper motions for all stars,
        whether polar or not, at epoch 2000, and measuring on the sky rather
        than in terms of dra, the errors resulting from this simplification are
        less than 1 milliarcsecond in position and 1 milliarcsecond per century
        in proper motion.

        References:

          1  "Mean and apparent place computations in the new IAU System.
              I. The transformation of astrometric catalog systems to the
          equinox J2000.0." Smith, C.A.; Kaplan, G.H.; Hughes, J.A.;
          Seidelmann, P.K.; Yallop, B.D.; Hohenkerk, C.Y.
          Astronomical Journal vol. 97, Jan. 1989, p. 265-273.

          2  "Mean and apparent place computations in the new IAU System.
          II. Transformation of mean star places from FK4 B1950.0 to
          FK5 J2000.0 using matrices in 6-space."  Yallop, B.D.;
          Hohenkerk, C.Y.; Smith, C.A.; Kaplan, G.H.; Hughes, J.A.;
          Seidelmann, P.K.; Astronomical Journal vol. 97, Jan. 1989,
          p. 274-279.

          3  "Conversion of positions and proper motions from B1950.0 to the
          IAU system at J2000.0", Standish, E.M.  Astronomy and
          Astrophysics, vol. 115, no. 1, Nov. 1982, p. 20-22.

       P.T.Wallace   Starlink   20 December 1993
       Doug Mink     Smithsonian Astrophysical Observatory  7 June 1995 */

        double r1950,d1950;		/* B1950.0 ra,dec (rad) */
        double r2000,d2000;		/* J2000.0 ra,dec (rad) */

        /* Miscellaneous */
        double ur=0.0,ud=0.0,sr,cr,sd,cd,w,wd;
        double x,y,z,xd,yd,zd, dra,ddec;
        double rxyz, rxysq, rxy, rxyzsq, spxy, spxyz;
        int	i,j;
        int	diag = 0;

        double[] r0 = new double[3];
        double[] rd0 = new double[3];	/* star position and velocity vectors */
        double[] v1 = new double[6];
        double[] v2 = new double[6];		/* combined position and velocity vectors */

        /* Constants */
        double zero = (double) 0.0;
        double vf = 21.095;	/* Km per sec to AU per tropical century */
                /* = 86400 * 36524.2198782 / 149597870 */

        /* Convert B1950.0 FK4 star data to J2000.0 FK5 */
        double[][] em = {
            {	 0.9999256782,		/* em[0][0] */
            -0.0111820611,		/* em[0][1] */
            -0.0048579477,		/* em[0][2] */
             0.00000242395018,	/* em[0][3] */
            -0.00000002710663,	/* em[0][4] */
            -0.00000001177656 },	/* em[0][5] */

            {	 0.0111820610,		/* em[1][0] */
             0.9999374784,		/* em[1][1] */
            -0.0000271765,		/* em[1][2] */
             0.00000002710663,	/* em[1][3] */
             0.00000242397878,	/* em[1][4] */
            -0.00000000006587 },	/* em[1][5] */

            {	 0.0048579479,		/* em[2][0] */
            -0.0000271474,		/* em[2][1] */
             0.9999881997,		/* em[2][2] */
             0.00000001177656,	/* em[2][3] */
            -0.00000000006582,	/* em[2][4] */
             0.00000242410173 },	/* em[2][5] */

            {	-0.000551,		/* em[3][0] */
            -0.238565,		/* em[3][1] */
             0.435739,		/* em[3][2] */
             0.99994704,		/* em[3][3] */
            -0.01118251,		/* em[3][4] */
            -0.00485767 },		/* em[3][5] */

            {	 0.238514,		/* em[4][0] */
            -0.002667,		/* em[4][1] */
            -0.008541,		/* em[4][2] */
             0.01118251,		/* em[4][3] */
             0.99995883,		/* em[4][4] */
            -0.00002718 },		/* em[4][5] */

            {	-0.435623,		/* em[5][0] */
             0.012254,		/* em[5][1] */
             0.002117,		/* em[5][2] */
             0.00485767,		/* em[5][3] */
            -0.00002714,		/* em[5][4] */
             1.00000956 }		/* em[5][5] */
            };

        /*  Constant vector and matrix (by columns)
            These values were obtained by inverting C.Hohenkerk's forward matrix
            (private communication), which agrees with the one given in reference
            2 but which has one additional decimal place.  */
        double[] a = {-1.62557e-6, -0.31919e-6, -0.13843e-6};
        double[] ad = {1.245e-3,  -1.580e-3,  -0.659e-3};
        double d2pi = 6.283185307179586476925287;	/* two PI */
        double tiny = 1.e-30; /* small number to avoid arithmetic problems */
        double rv = 0;
        double parallax = 0;

        /* Convert B1950 RA in hours and Dec in degrees to radians */
        r1950 = ra*PI/12;
        d1950 = dec*PI/180;

        /* Convert B1950 RA and Dec proper motion from mas/year to arcsec/tc */
        if (usePM)
            {
            ur = rapm/(10*cos(d1950));   // *100/(1000*cos(d1950))
            ud = decpm/10;  // *100/1000
            }

        /* Convert direction to Cartesian */
        sr = sin (r1950);
        cr = cos (r1950);
        sd = sin (d1950);
        cd = cos (d1950);
        r0[0] = cr * cd;
        r0[1] = sr * cd;
        r0[2] = sd;

        /* Convert motion to Cartesian */
        w = vf * rv * parallax;
        if (ur != zero || ud != zero || (rv != zero && parallax != zero)) {
        rd0[0] = (-sr * cd * ur) - (cr * sd * ud) + (w * r0[0]);
        rd0[1] =  (cr * cd * ur) - (sr * sd * ud) + (w * r0[1]);
        rd0[2] = 	                (cd * ud) + (w * r0[2]);
        }
        else {
        rd0[0] = zero;
        rd0[1] = zero;
        rd0[2] = zero;
        }

        /* Remove e-terms from position and express as position+velocity 6-vector */
        w = (r0[0] * a[0]) + (r0[1] * a[1]) + (r0[2] * a[2]);
        for (i = 0; i < 3; i++)
        v1[i] = r0[i] - a[i] + (w * r0[i]);

        /* Remove e-terms from proper motion and express as 6-vector */
        wd = (r0[0] * ad[0]) + (r0[1] * ad[1]) + (r0[2] * ad[2]);
        for (i = 0; i < 3; i++)
        v1[i+3] = rd0[i] - ad[i] + (wd * r0[i]);

        /* Alternately: Put proper motion in 6-vector without adding e-terms
        for (i = 0; i < 3; i++)
        v1[i+3] = rd0[i]; */

        /* Convert position + velocity vector to FK5 system */
        for (i = 0; i < 6; i++) {
        w = zero;
        for (j = 0; j < 6; j++) {
            w += em[i][j] * v1[j];
            }
        v2[i] = w;
        }

        /* Vector components */
        x = v2[0];
        y = v2[1];
        z = v2[2];
        xd = v2[3];
        yd = v2[4];
        zd = v2[5];

        /* Magnitude of position vector */
        rxysq = x*x + y*y;
        rxy = sqrt (rxysq);
        rxyzsq = rxysq + z*z;
        rxyz = sqrt (rxyzsq);

        spxy = (x * xd) + (y * yd);
        spxyz = spxy + (z * zd);

        /* Convert back to spherical coordinates */
        if (x == zero && y == zero)
        r2000 = zero;
        else {
        r2000 = atan2 (y,x);
        if (r2000 < zero)
            r2000 = r2000 + d2pi;
        }
        d2000 = atan2 (z,rxy);

        /* Return results */
        
        ra = r2000*12.0/PI;
        dec = d2000*180.0/PI;

        return new double[] {ra, dec};
    }


/**
 * Convert J2000 equatorial coordinates to IAU 1958 galactic coordinates
 * @param ra J2000 Right ascension in hours
 * @param dec J2000 Declination in degrees
 * @param pm_ra Proper motion in right ascension (mas/yr)
 * @param pm_dec Proper motion in declination (mas/yr)
 * @param usePM Include proper motion in conversion
 * @return galactic longitude and latitude in degrees as {glong, glat}
 */
public static double[] J2000toGal (double ra, double dec, double pm_ra, double pm_dec, boolean usePM)
    {
    //  This method is heavily based on the WCSTools routine fk52gal
    /* Rotation matrices by P.T.Wallace, Starlink eqgal and galeq, March 1986 */

    /*  l2,b2 system of galactic coordinates
        p = 192.25       ra of galactic north pole (mean b1950.0)
        q =  62.6        inclination of galactic to mean b1950.0 equator
        r =  33          longitude of ascending node
        p,q,r are degrees */
    /*  Equatorial to galactic rotation matrix
        The eulerian angles are p, q, 90-r
        +cp.cq.sr-sp.cr     +sp.cq.sr+cp.cr     -sq.sr
        -cp.cq.cr-sp.sr     -sp.cq.cr+cp.sr     +sq.cr
        +cp.sq              +sp.sq              +cq		*/
        double[][] jgal =
        {{-0.054875539726,-0.873437108010,-0.483834985808},
        {0.494109453312,-0.444829589425, 0.746982251810},
        {-0.867666135858,-0.198076386122, 0.455983795705}};
    

        double[] pos = new double[3];
        double[] pos1 = new double[3];
        double r,dl,db,rl,rb,rra,rdec,hra,ddec,x,y,z,rxy2,rxy,z2;
        int i;

        if (usePM)
            {
            double[] radec = ProperMotion(2000.0, 2433282.422917, ra, dec, pm_ra, pm_dec);//  from epoch J2000 to B1950 (UT 1949 12/31/1949 22:09)
            ra = radec[0];
            dec = radec[1];
            }

        /*  Spherical to cartesian */
        hra = ra;
        ddec = dec;
        rra = hra*PI/12.0;
        rdec = ddec*PI/180.0;
        r = 1.0;

        pos[0] = r * cos (rra) * cos (rdec);
        pos[1] = r * sin (rra) * cos (rdec);
        pos[2] = r * sin (rdec);

        /*  Rotate to galactic */
        for (i = 0; i < 3; i++) {
        pos1[i] = pos[0]*jgal[i][0] + pos[1]*jgal[i][1] + pos[2]*jgal[i][2];
        }

        /*  Cartesian to spherical */

        x = pos1[0];
        y = pos1[1];
        z = pos1[2];

        rl = atan2 (y, x);

        /* Keep RA within 0 to 2pi range */
        if (rl < 0.0)
        rl += (2.0 * PI);
        if (rl > 2.0 * PI)
        rl -= (2.0 * PI);

        rxy2 = x*x + y*y;
        rxy = sqrt (rxy2);
        rb = atan2 (z, rxy);

        z2 = z * z;
        r = sqrt (rxy2 + z2);

        dl = rl*180.0/PI;
        db = rb*180.0/PI;

        return new double[] {dl, db};
    }


/**
 * Convert IAU 1958 galactic coordinates to J2000 equatorial coordinates
 * @param glon galactic longitude in degrees
 * @param glat galactic latitude in degrees
 * @param pm_ra Proper motion in right ascension (mas/yr)
 * @param pm_dec Proper motion in declination (mas/yr)
 * @param usePM Include proper motion in conversion
 * @return J2000 ra in hours and dec in degrees as {ra, dec}
 */
public static double[] GaltoJ2000 (double glon, double glat, double pm_ra, double pm_dec, boolean usePM)
    {
        //  This method is heavily based on the WCSTools routine gal2fk5
        /*  l2,b2 system of galactic coordinates
            p = 192.25       ra of galactic north pole (mean b1950.0)
            q =  62.6        inclination of galactic to mean b1950.0 equator
            r =  33          longitude of ascending node
            p,q,r are degrees */
        /*  Equatorial to galactic rotation matrix
            The eulerian angles are p, q, 90-r
            +cp.cq.sr-sp.cr     +sp.cq.sr+cp.cr     -sq.sr
            -cp.cq.cr-sp.sr     -sp.cq.cr+cp.sr     +sq.cr
            +cp.sq              +sp.sq              +cq		*/
        double[][] jgal =
        {{-0.054875539726,-0.873437108010,-0.483834985808},
        {0.494109453312,-0.444829589425, 0.746982251810},
        {-0.867666135858,-0.198076386122, 0.455983795705}};

        double[] pos = new double[3];
        double[] pos1 = new double[3];
        double r,dl,db,rl,rb,rra,rdec,hra,ddec;
        double x,y,z,rxy,rxy2,z2;
        int i;

        /*  Spherical to Cartesian */
        dl = glon;
        db = glat;
        rl = dl*PI/180.0;
        rb = db*PI/180.0;
        r = 1.0;
        pos[0] = r * cos (rl) * cos (rb);
        pos[1] = r * sin (rl) * cos (rb);
        pos[2] = r * sin (rb);

        /*  Rotate to equatorial coordinates */
        for (i = 0; i < 3; i++) {
            pos1[i] = pos[0]*jgal[0][i] + pos[1]*jgal[1][i] + pos[2]*jgal[2][i];
            }

        /*  Cartesian to Spherical */

        x = pos1[0];
        y = pos1[1];
        z = pos1[2];

        rra = atan2 (y, x);

        /* Keep RA within 0 to 2pi range */
        if (rra < 0.0)
        rra += (2.0 * PI);
        if (rra > 2.0 * PI)
        rra -= (2.0 * PI);

        rxy2 = x*x + y*y;
        rxy = sqrt (rxy2);
        rdec = atan2 (z, rxy);

        z2 = z * z;
        r = sqrt (rxy2 + z2);



        hra = rra*12.0/PI;
        ddec = rdec*180.0/PI;

        if (usePM)
            {
            double[] radec = ProperMotion(2000.0, 2433282.422917, hra, ddec, -pm_ra, -pm_dec);//  from B1950 (UT 1949 12/31/1949 22:09) to J2000
            hra = radec[0];
            ddec = radec[1];
            }

        return new double[] {hra, ddec};
    }



}


