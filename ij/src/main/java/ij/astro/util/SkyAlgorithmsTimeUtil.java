// SkyAlgorithms.java

package ij.astro.util;

import nom.tam.fits.FitsDate;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;


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




public class SkyAlgorithmsTimeUtil {

    public static void main(String[] args) {
        double[] jds = {2451368.56319, // July 9, 1999, 1:30:59:62
                        2451004.48001, // July 9, 1998, 23:31:12:86
                        2459430.94598, // August 4, 2021, 10:42:12:67
                        2451179.50000143, // 1999-01-01T00:00:00.123456789
                        2455197.5 // 2010-01-01T00:00:00
                        };

        for (double jd : jds) {
            // This should match what is done in FITS_Reader
            var dt = SkyAlgorithmsTimeUtil.UTDateFromJD(jd);
            var hmsms = SkyAlgorithmsTimeUtil.ut2Array(dt[3]);
            var dateTime = LocalDateTime.of((int) dt[0], (int) dt[1], (int) dt[2], hmsms[0], hmsms[1],
                    hmsms[2]).toInstant(ZoneOffset.ofTotalSeconds(0));
            dateTime = dateTime.plusMillis(hmsms[3]);
            System.out.println(jd + " = " + FitsDate.getFitsDateString(Date.from(dateTime)));
        }

    }

    /**
     * Compute the Julian Day for the given date.
     * Julian Date is the number of days since noon of Jan 1 4713 B.C.
     *
     * @param ny year
     * @param nm month
     * @param nd day
     * @param ut UT time
     * @return Julian Date
     */
    public static double CalcJD(int ny, int nm, int nd, double ut) {
        double A, B, C, D, jd, day;

        day = nd + ut / 24.0;
        if ((nm == 1) || (nm == 2)) {
            ny = ny - 1;
            nm = nm + 12;
        }

        if ((ny + nm / 12.0 + day / 365.25) >= (1582.0 + 10.0 / 12.0 + 15.0 / 365.25)) {
            A = ((int) (ny / 100.0));
            B = 2.0 - A + (int) (A / 4.0);
        } else {
            B = 0.0;
        }

        if (ny < 0.0) {
            C = (int) ((365.25 * (double) ny) - 0.75);
        } else {
            C = (int) (365.25 * (double) ny);
        }

        D = (int) (30.6001 * (double) (nm + 1));
        jd = B + C + D + day + 1720994.5;
        return (jd);
    }

    /**
     * Calculate the current universal date and time with millisecond resolution.
     *
     * @return {year, month, day, UT_time}
     */
    public static double[] UTDateNow() {
        int hours, minutes, seconds, milliseconds;
        double[] utdate = {0, 0, 0, 0};
        double ut;

        Calendar now = Calendar.getInstance(TimeZone.getTimeZone("UTC"));

        utdate[0] = now.get(Calendar.YEAR);
        utdate[1] = now.get(Calendar.MONTH) + 1;
        utdate[2] = now.get(Calendar.DAY_OF_MONTH);
        hours = now.get(Calendar.HOUR_OF_DAY);
        minutes = now.get(Calendar.MINUTE);
        seconds = now.get(Calendar.SECOND);
        milliseconds = now.get(Calendar.MILLISECOND);

        /* Calculate floating point ut in hours */

        ut = ((double) milliseconds) / 3600000. +
                ((double) seconds) / 3600. +
                ((double) minutes) / 60. +
                ((double) hours);
        utdate[3] = ut;
        return (utdate);
    }


    /**
     * Convert a Julian day into universal date and time.
     * Numerical Recipes in C, 2nd ed., Cambridge University Press 1992.
     *
     * @param jd Julian date of interest
     * @return {year, month, day, UT_time}
     */
    public static double[] UTDateFromJD(double jd) {
        int julian = (int) (jd + 0.5);
        double ut = (jd - (double) julian + 0.5) * 24.0;
        int ja, jalpha, jb, jc, jdd, je;
        int IGREG = 2299161;
        double[] utdate = {0, 0, 0, 0};

        if (julian >= IGREG) {
            jalpha = (int) (((julian - 1867216) - 0.25) / 36524.25);
            ja = julian + 1 + jalpha - (int) (0.25 * jalpha);
        } else if (julian < 0) {
            ja = julian + 36525 * (1 - julian / 36525);
        } else {
            ja = julian;
        }
        jb = ja + 1524;
        jc = (int) (6680.0 + ((double) (jb - 2439870) - 122.1) / 365.25);
        jdd = (int) (365 * jc + (0.25 * jc));
        je = (int) ((jb - jdd) / 30.6001);
        int day = jb - jdd - (int) (30.6001 * je);
        int mm = je - 1;
        if (mm > 12) mm -= 12;
        int iyyy = jc - 4715;
        if (mm > 2) iyyy--;
        if (iyyy <= 0) iyyy--;
        if (julian < 0) iyyy -= 100 * (1 - julian / 36525);

        utdate[0] = iyyy;
        utdate[1] = mm;
        utdate[2] = day;
        utdate[3] = ut;

        return utdate;
    }


    /**
     * Converts UT time to {hours, minutes, seconds, milliseconds}
     */
    public static int[] ut2Array(double utTime) {
        var hour = (int) utTime;
        var rem = utTime - hour;
        var min = (int) (rem * 60);
        rem = rem - min/60D;
        var secs = (int) (rem * 3600);
        rem = rem - secs/3600D;
        var ms = (int) (rem * 3600000);

        return new int[]{hour, min, secs, ms};
    }

}


