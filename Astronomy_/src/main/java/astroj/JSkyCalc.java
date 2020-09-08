package astroj;
/* JSkyCalc.java -- copyright 2007, John Thorstensen, Dartmouth College. */
/** TERMS OF USE -- Anyone is free to use this software for any purpose, and to
modify it for their own purposes, provided that credit is given to the author
in a prominent place.  For the present program that means that the green
title and author banner appearing on the main window must not be removed,
and may not be altered without premission of the author. */

import java.io.*;
import java.util.*;
import java.util.Scanner;
import java.util.regex.*;
import java.util.List.*;
import java.awt.*;
import java.awt.print.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.awt.font.*;
// import java.awt.PointerInfo.*;
// import java.awt.MouseInfo.*;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.table.*;
import javax.swing.text.html.*;

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

class sexagesimal implements Cloneable

{
   int sign;
   int hour, minute;
   double second;
   double value;

   // Overloaded constructor takes either a string or a double argument.
   // little public classes wrap the ones that do all the work.

   public sexagesimal(String s) {
       parseSexString(s);
   }
   public sexagesimal(double v) {   
       tosex(v);
       value = v;
   }
   
   public sexagesimal clone() {
      try {
          sexagesimal copy = (sexagesimal) super.clone();
          copy.sign = sign;
          copy.hour= hour;
          copy.minute = minute;
          copy.second= second;
          copy.value = value;
          return copy;
      } catch (CloneNotSupportedException e) {
          throw new Error("This should never happen!");
      }
   }
 
   void tosex(double h) {  // decimal hour converter ... 
       double m;
       // System.out.printf("h = %f\n",h);
       if(h >= 0.) sign = 1;
       else sign = -1;
       h = h * sign;    
       hour = (int) h;
       m = 60. * (h - (double) hour);
       minute = (int) m;
       second = 60. * (m - (double) minute);
       // System.out.printf("sgn h m s %d  %d %d %f\n",sign,hour,minute,second);
   }

   void parseSexString(String s) { 
       // converts a string, either colon or space separated, into a
       // sexagesimal. "minus-zero" problem is handled.  
       double doubleMinute;
       String [] fields;
       if(s.contains(":")) {
          fields = s.split(":");      // colon-delimited
       }
       else {
          fields = s.split("\\s+");  // whitespace
       } 
       if(fields.length == 1) {  // a string, but just a single number 
           value = Double.parseDouble(fields[0]);
           tosex(value);
       }
       else {   // it's a triplet (or possibly doublet)
          // for (String ss : fields) {
            // System.out.printf("%s ... ",ss);  // colon-delimited
          // }
          if(fields[0].contains("-")) sign = -1;
          else sign = 1;
          fields[0] = fields[0].replace("+","");  // parseInt chokes on explicit "+"
          hour = Integer.parseInt(fields[0]);
          if(hour < 0) hour = hour * -1;   // positive definite
          try {
             minute = Integer.parseInt(fields[1]);
          }
          catch ( NumberFormatException e ) { // decimal minute input 
             doubleMinute = Double.parseDouble(fields[1]);  
             minute = (int) doubleMinute;
             second = 60. * (double) (doubleMinute - (double) minute);
          }
          if(fields.length > 2) {  // seconds are there ... 
             second = Double.parseDouble(fields[2]);
          }
       }
       // System.out.printf("%d  %d %d %f\n",sign,hour,minute,second);   
       value = (double) hour + (double) minute / 60. + (double) second / 3600.;
       value = value * sign;   
       // System.out.printf("value: %f\n",value);
   }

   public sexagesimal roundsex(int ndigits) {
       // returns a sexagesimal rounded off to ndigits 
       // so that seconds and minutes are never 60.  Higher overflows
       // (e.g. 24 00 00 for a time of day) will be handled elsewhere.

       String teststr = "";
       String testformat = "";
       int hourback, minuteback;
       double secondback;
       int secondswidth;
       int tenthMinutes;
       double decimalMinute,decimalMinuteback;

       if(ndigits >= 0) {  // including seconds ... 
         if(ndigits == 0) testformat = 
              String.format(Locale.ENGLISH, "%%d %%d %%02.0f");
         if(ndigits > 0) {
            secondswidth = 3 + ndigits;
            testformat = String.format(Locale.ENGLISH, "%%d %%d %%0%1d.%1df",
               secondswidth,ndigits);
            // System.out.println(testformat);
         }
           
         // System.out.printf("In roundsex, testformat = %s\n",testformat);
         teststr = String.format(Locale.ENGLISH, testformat,hour,minute,second);
         Scanner readback = new Scanner( teststr ).useDelimiter("\\s");
         readback.useLocale(Locale.ENGLISH);
         // read back the result ...
         // System.out.printf("In roundsex, teststr = %s\n",teststr);
         hourback = readback.nextInt();
         if(hourback < 0.) hourback *= -1.;
         minuteback = readback.nextInt();
         secondback = readback.nextDouble();
         // System.out.printf("read back: %d %d %f\n",hourback,minuteback,secondback);
         if(secondback > 59.999999999) {  // klugy, but should be very safe
            secondback = 0.;
            minuteback++; 
            if(minuteback == 60) {
               minuteback = 0;
               hourback++; // overflows to 24, etc need to be handled 
                          // at the next level
            }
            teststr = String.format(Locale.ENGLISH, testformat,hourback,minuteback,secondback);
         }
       } 
       else {  // -1 -> tenths of a minute, -2 -> whole minutes 
         decimalMinute = minute + second / 60.;
         if(ndigits == -1) 
           teststr = String.format(Locale.ENGLISH, "%d %4.1f",hour,decimalMinute);
         if(ndigits <= -2) 
           teststr = String.format(Locale.ENGLISH, "%d %02.0f",hour,decimalMinute);
         Scanner readback = new Scanner (teststr);
         readback.useLocale(Locale.ENGLISH);
         hourback = readback.nextInt();
         decimalMinuteback = readback.nextDouble();
         if(decimalMinuteback > 59.99) {  // limited precision - this will be safe
            decimalMinuteback = 0.00001;
            hourback++;
         }
         minuteback = (int) decimalMinuteback;
         if(ndigits == -1) {
            tenthMinutes = (int) 
              ((10. * (decimalMinuteback - minuteback) + 0.0001));
            teststr = String.format(Locale.ENGLISH, "%d %02d.%1d",hourback,minuteback,tenthMinutes);
         }            
         else 
            teststr = String.format(Locale.ENGLISH, "%d %02d",hourback,minuteback);
       }
       sexagesimal roundedsex =  new sexagesimal(teststr);
       roundedsex.sign = sign;
       return roundedsex;
   }
}

/* The Java calendar API is pretty nasty, and the DST rules in 
   particular are buried very deeply.  The application I have in 
   mind requires me to have explicit control over these rules in the
   past and the future.  Given that I've already
   written my own date handlers in other languages, I'm thinking
   it may be be easier and more robust for me to just go ahead and 
   reproduce that functionality from scratch.  */

class GenericCalDat implements Cloneable
/** GenericCalDat - handles conversions back and forth to JD, and 
    formatting and rounding of dates.  Rounding of times is handled in the
    sexagesimal class, and then continued rounding of dates is handled 
    here.  GenericCalDat is blind to whether the date represented is 
    UT or local - that's handled by InstantInTime. */
{
   int year, month, day;
   sexagesimal timeofday;
   String [] months = {"","January","February","March","April",
           "May","June","July","August","September","October","November",
           "December"};
   String monthtest = "janfebmaraprmayjunjulaugsepoctnovdec";
   String [] dayname = {"Mon","Tue","Wed","Thu","Fri","Sat","Sun"};

   // Polymorphism! Overload constructor class to
   // accept either a string or a double.

   public GenericCalDat( String s ) {
      // "yyyy mm dd hh mm ss" ... or JD as character string.
      CalFromString(s);
   }
   // overload the constructor method to take a JD number.
   public GenericCalDat(double jdin) {
      CalFromJD(jdin);  
   }
  
   public GenericCalDat clone() {
      try {
          GenericCalDat copy = (GenericCalDat) super.clone();
          copy.year= year;
          copy.month = month;
          copy.day = day;
          copy.timeofday = (sexagesimal) timeofday.clone();
          return copy;
      } catch (CloneNotSupportedException e) {
          throw new Error("This should never happen!");
      }
   }
 
   void CalFromJD (double jd) {
       /* sets the calendar date using the current values of jd -- can
          be either a local or UT date */

       /* Adapted from J. Meeus,  Astronomical Formulae for Calculators,
           published by Willman-Bell Inc.
           Avoids a copyrighted routine from Numerical Recipes.
           Tested and works properly from the beginning of the 
            calendar era (1583) to beyond 3000 AD. */

       double tmp;
       long alpha; 
       long Z, A, B, C, D, E;
       double F, x;
       int dow;
       double jdin;
       int rounded_ok = 0;
 
       jdin = jd;
     

       while(rounded_ok == 0) {
          tmp = jdin + 0.5; 
          Z = (long) tmp;
          x = Z/7. + 0.01;
          dow = (int) (7.* Math.floor(x));
       
          F = tmp - Z;
          if (Z < 2299161) A = Z;
          else {
             alpha = (long) ((Z - 1867216.25) / 36524.25);
             A = Z + 1 + alpha - (long) (alpha / 4);
          }
     
          B = A + 1524;
          C = (long)((B - 122.1) / 365.25);
          D = (long)(365.25 * C);
          E = (long)((B - D) / 30.6001);
   
          day = (int)(B - D - (long)(30.6001 * E));
          if(E < 13.5) month = (int)(E - 1);
          else month = (int)(E - 13);
          if(month > 2.5) year = (int)(C - 4716);
          else year = (int) (C - 4715);
             timeofday = new sexagesimal(24. * F);

          if(timeofday.hour == 24) {  
             jdin = jdin + 1.0e-7; // near to the resolution of the double
          }
          else rounded_ok = 1;
      }
   }
   
   void CalFromString(String s) {
      // "yyyy mm dd hh mm ss" ... or just a string "JD".
      // "yyyy mm dd" or "yyyy mm dd hh mm" work ok too.
      // "2006 Jul 14" works, as does "2006 July 14 18:00"
      String [] fields = s.split("\\s+");   // whitespace

      if(fields.length == 1) { // presumably only a JD ... 
         double jdin = Double.parseDouble(fields[0]);
         CalFromJD(jdin);
         return;
      }

      year = Integer.parseInt(fields[0]);
      try {
         month = Integer.parseInt(fields[1]);
      }
      catch (NumberFormatException e) { 
         // System.out.println("Catching exception .. ");
         String ss = fields[1].toLowerCase();
         ss = ss.substring(0,3);
         int ind = monthtest.indexOf(ss);
         if(ind > -1) {
            month =  ((ind / 3) + 1) ;
            // System.out.printf("ss %s ind %d month %d\n",ss,ind,month);
         }
         else {
            // System.out.println("Undecipherable month, set to 1000.\n");
            month = 1000;
         }
      }
      day = Integer.parseInt(fields[2]);
      if(fields.length == 4) { // colon-sep time included
         timeofday = new sexagesimal(fields[3]);
      }
      if(fields.length == 5) {
         timeofday = new sexagesimal(String.format(Locale.ENGLISH, "%s:%s",fields[3],fields[4]));
      }
      if(fields.length > 5) {
         timeofday = 
          new sexagesimal(String.format(Locale.ENGLISH, "%s:%s:%s",fields[3],fields[4],fields[5]));
      }
   }

   double Cal2JD() {
       
       int y, m;
       long A, B;
       double jdout;

       // System.out.printf("%d %d %d  %d %d %f\n",
          // year,month,day,timeofday.hour,timeofday.minute,timeofday.second); 

       if(month <= 2) {
           y = year - 1;
           m = month + 12;
       }
       else {
           y = year;
           m = month;
       }
     
       A = (long) ((double) (y+0.00000001) / 100.);
       B = 2 - A + (long) ((double) (A + 0.000000001) / 4.);

       jdout =  (long)(365.25 * (double) y) + (long) (30.6001 * (m + 1)) + day + 
           1720994.5;
       //System.out.println(jd);
       jdout += (double) timeofday.hour / 24. + (double) timeofday.minute / 1440. + 
              timeofday.second / 86400.;
       //System.out.println(jd);
       
       if(year > 1583) return (jdout + (double) B);
       else return jdout;
   }

   int DayOfWeek() {
   /** Adapted straight from skycalc, returns 0=Mon throuh 6=Sun **/

       double jd;
       long i;
       double x;
       int d;

       jd = Cal2JD() + 0.5;
       i = (long) jd;
       x = i/7. + 0.01;
       d = (int) (7.*(x - (long) x));
       return d;
   }

   void quickprint() {
       System.out.printf("%d %02d %02d  %02d %02d %f",
          year,month,day,timeofday.hour,timeofday.minute,timeofday.second);       
       System.out.printf(" -> %s\n",dayname[DayOfWeek()]);
   }

   String RoundedCalString(int style, int digits) {
   /** Returns a descriptive string; rather than writing a flexible
       format I'll code a number of options.  Much sturm und drang here
       because of the need to round the day along with everything else. 
       These styles follow cooclasses.py; I'll write more as needed. :

       style 0 -> 2006 8 12  10 11 12
       style 1 -> 2005 Aug 12  10 11
       style 2 -> Fri Aug 12  10:11 
       style 3 -> 2005 Aug 12
       style 4 -> Fri Aug 12 
       style 5 -> 2005 6 12
       style 6 -> 2006 Aug 12 Tue
       style 7 -> Fri 2006 Aug 12  10:11 

       style 10 -> (time only) 10 11 12.0
       style 11 -> (time only) 10:11:12.0
       style 12 -> (time only) 10:11  
       
       These are included here to force correct rounding when printing
          only the time.

        */
       String result;
       String outputFormat;
       int timeDigits;
       double jdtemp;
       int printYear, printMonth, printDay;
       int printHour, printMinute; 
       double printSecond;
       int printDOW;
       
     
       if(style == 0)  digits = 0;
       if(style == 1 | style == 2 | style == 12)  digits = -2;
  
       sexagesimal Rounded = timeofday.roundsex(digits);
       // round the date upward ...
       if(Rounded.hour == 24) {
          // System.out.println("Oops, gotta round day upward.\n");
          jdtemp = Cal2JD() + 0.4; // this will always round upward
                                   // and never screw the day of week
          GenericCalDat tempcal = new GenericCalDat(jdtemp);
          printYear = tempcal.year;
          printMonth = tempcal.month;
          printDay = tempcal.day;
          printHour = 0;
          printMinute = 0;
          printSecond = 0.;
          printDOW = tempcal.DayOfWeek();
       }
       else {
          printYear = year;
          printMonth = month; 
          printDay = day;
          printHour = Rounded.hour;
          printMinute = Rounded.minute;
          printSecond = Rounded.second;
          printDOW = DayOfWeek();
       } 
       String monthAbr = months[printMonth].substring(0,3);
       switch(style) {
         case 0 :
           // System.out.println("*** 0 ***");
           result = String.format(Locale.ENGLISH, "%4d %02d %02d  %02d %02d %02.0f",
             printYear, printMonth, printDay, printHour, printMinute, 
             printSecond);
         break;
         case 1:
           // System.out.println("*** 1 ***");
           result = String.format(Locale.ENGLISH, "%4d %s %02d  %02d %02d",
             printYear, monthAbr, printDay, printHour, printMinute);
           break; 
         case 2 :
           // System.out.println("*** 2 ***");
           result = String.format(Locale.ENGLISH, "%s %s %02d  %02d:%02d",
             dayname[printDOW],monthAbr,printDay,printHour,printMinute);
           break;
         case 3 : 
           // System.out.println("*** 3 ***");
           result = String.format(Locale.ENGLISH, "%4d %s %02d",printYear,monthAbr,
              printDay);
           break;
         case 4 :
           // System.out.println("*** 4 ***");
           result = String.format(Locale.ENGLISH, "%s %s %02d",dayname[printDOW],
             monthAbr,printDay);
           break;
         case 5 :
           // System.out.println("*** 5 ***");
           result = String.format(Locale.ENGLISH, "%4d %02d %02d",printYear,printMonth,
                 printDay);
           break; 
         case 6 : 
           // System.out.println("*** 6 ***");
           result = String.format(Locale.ENGLISH, "%4d %s %02d  %s",printYear,monthAbr,
              printDay,dayname[printDOW]);
           break;
         case 7 :
           // System.out.println("*** 2 ***");
           result = String.format(Locale.ENGLISH, "%s  %4d %s %02d  %02d:%02d",
             dayname[printDOW],printYear,monthAbr,printDay,printHour,printMinute);
           break;
 
         case 11 :
           // System.out.println("*** 11 ***");
           result = String.format(Locale.ENGLISH, "%02d %02d %04.1f",printHour,printMinute,
                 printSecond);
           break;
         case 12 : 
           result = String.format(Locale.ENGLISH, "%02d:%02d",printHour,printMinute);
           break;
         default :
           // System.out.println("*** Default ***");
           result = String.format(Locale.ENGLISH, "%02d %02d %02.0f",
             printYear, monthAbr, printDay, printHour, printMinute,
             printSecond);
       }
       return result;
   }
}

class InstantInTime implements Cloneable 
/** The instance variable jd is the true jd (always), and there are 
    two GenericCalDat instances, UTDate and localDate.  Instance 
    variables stdz and useDST are the std zone offset (hours west) and an 
    integer encoding which daylight savings convention is used.  
    FindDSTBounds figures out whether DST is actually in effect.
**/

{
   double jd;   /* the real JD, always. */

   GenericCalDat UTDate;
   GenericCalDat localDate;
   double localjd;   /* used transiently ... */
   double stdz;   // hours west
   int useDST;
   boolean dstInEffect = false;
   double [] DSTBounds = {0.,0};

   static double TheEpoch = 2440587.50000;  // Jan 1, 1970 0h UT 

   public InstantInTime (double stdzin, int use_dst) {
        // default constructor sets to system time ... 
        stdz = stdzin;  useDST = use_dst;
        SetInstant( stdzin, use_dst );
   }

   public InstantInTime( String s, double stdzin, 
           int use_dst, boolean is_ut ) {
        /* allowed formats are "yyyy mo dd hh mm ss" and "mo" can 
           be a three-letter abbreviation. */
        stdz = stdzin;  useDST = use_dst;
        SetInstant(s, stdzin, use_dst, is_ut);
   }

   public InstantInTime( double jdin, double stdzin, 
           int use_dst, boolean is_ut ) {
        /* allowed formats are "yyyy mo dd hh mm ss" and "mo" can 
           be a three-letter abbreviation. */
        stdz = stdzin;  useDST = use_dst;
        SetInstant(jdin, stdzin, use_dst, is_ut);
   }

   void SetInstant( double stdzin, int use_dst) {
        // when no time specified, sets to system time.
        long milliseconds;
        double jdnow;
        
        stdz = stdzin;  useDST = use_dst;
        milliseconds = System.currentTimeMillis();
        jdnow = TheEpoch + milliseconds / 86400000.;
        SetInstant(jdnow, stdzin, use_dst, true);
   }

   public InstantInTime clone() {
       try {
         InstantInTime copy = (InstantInTime) super.clone();
            copy.jd = jd;
            copy.UTDate = (GenericCalDat) UTDate.clone();
            copy.localDate = (GenericCalDat) localDate.clone();
            copy.localjd = localjd;
            copy.stdz = stdz;
            copy.useDST = useDST;
            copy.dstInEffect = dstInEffect;
            copy.DSTBounds = DSTBounds;
            return copy;
       } catch (CloneNotSupportedException e) {
          throw new Error("This should never happen!");
       }
   }

   void SetInstant( String s, double stdzin, int use_dst, boolean is_ut) {

      useDST = use_dst;
       
      if(is_ut) {
          UTDate = new GenericCalDat(s);
          jd = UTDate.Cal2JD();
          // System.out.printf("Setting to UT, s = %s,jd = %f\n",s,jd);
          // The DST calculation is not sensitive to year if dstInEffect is 
          // computed using the same year as used in findDSTBounds.
          if(use_dst == 0) dstInEffect = false;
          else {  // determine if DST is in effect ... 
             DSTBounds = findDSTBounds(UTDate.year, stdzin, use_dst);
             if(use_dst > 0)  {  // northern hemisphere logic
                if(jd > DSTBounds[0] & jd < DSTBounds[1]) dstInEffect = true;
                else dstInEffect=false;
             }
             if(use_dst < 0) {  // southern hemisphere logic
                if(jd < DSTBounds[0] | jd > DSTBounds[1]) dstInEffect = true;
                else dstInEffect = false;
             }
             // System.out.printf("use_dst %d jd %f Bounds[0,1] = %f %f\n",
               //   use_dst,jd,DSTBounds[0],DSTBounds[1]);
            // if (dstInEffect) System.out.printf("DST is in effect.\n");
            // else System.out.printf("DST is NOT in effect.\n");
          }
          
          if(dstInEffect) {
              localjd = jd - (stdzin - 1.) / 24.;
            //  System.out.printf("setting localjd using DST\n"); 
          }
          else {
              localjd = jd - (stdzin / 24.);
            //  System.out.printf("Setting localjd using std time\n");
          }
          localDate = new GenericCalDat(localjd);

      }
      else {  // input string is local date and time.
          localDate = new GenericCalDat(s);  // by definition .. 
          localjd = localDate.Cal2JD();
          // System.out.printf("using localjd = %f\n",localjd);
          if(use_dst == 0) dstInEffect = false;
          else { // dst is used if applicable ... use local-time limits of 
                 // applicability {DSTBounds [2] and [3] instead of [0] and [1])
             DSTBounds = findDSTBounds(localDate.year, stdzin, use_dst);
             if(use_dst > 0)  {  // northern hemisphere logic
                if(localjd > DSTBounds[2] & localjd < DSTBounds[3]) dstInEffect = true;
                else dstInEffect = false;
             }
             if(use_dst < 0) {  // southern hemisphere logic
                if(localjd < DSTBounds[2] | localjd > DSTBounds[3]) dstInEffect = true;
                else dstInEffect = false;
             }
          }

          if(dstInEffect) jd = localjd + (stdzin - 1.) / 24.;
          else jd = localjd + stdzin / 24.;
          // System.out.printf("Setting jd to %f\n",jd);
          UTDate = new GenericCalDat(jd);
       }
   }

   void AdvanceTime(String s, double stdzin, int use_dst_in) {
      String [] fields ;
      String lastfield;
      double inputdelta, delta;
      int nf;

      stdz = stdzin;
      useDST = use_dst_in;
      
      fields = s.split("\\s+");  // whitespace
   
      nf = fields.length;
      inputdelta = Double.parseDouble(fields[0]);
      if(nf > 1) 
          lastfield = fields[nf - 1].toLowerCase();
      else lastfield = "h";   // defaults to hours

      // use first character to tell us what the unit is 
//      System.out.printf("last field %s  lastfield.substring(0,1) :%s:\n",
 //          lastfield,lastfield.substring(0,1));
      if(lastfield.startsWith("h")) delta = inputdelta / 24.;
      else if(lastfield.startsWith("m")) delta = inputdelta / 1440.;
      else if(lastfield.startsWith("s")) delta = inputdelta / 86400.;
      else if(lastfield.startsWith("d")) delta = inputdelta;
      else if(lastfield.startsWith("t")) delta = inputdelta / 1.0027379093;  
           // 1 sidereal day
      else if(lastfield.startsWith("l")) delta = 29.5307 * inputdelta;  
           // 1 lunation
      else if(lastfield.startsWith("y")) delta = 365. * inputdelta;  
      else delta = inputdelta / 24.;

      SetInstant(jd + delta, stdz, useDST, true);  
         // jd is always UT, so use_dst is true

   }

   void AdvanceTime(String s, double stdzin, int use_dst_in, boolean forward) {
      String [] fields ;
      String lastfield;
      double inputdelta, delta;
      int nf;
      
      stdz = stdzin;
      useDST = use_dst_in;

      fields = s.split("\\s+");  // whitespace
   
      nf = fields.length;
      inputdelta = Double.parseDouble(fields[0]);
      if(nf > 1) 
          lastfield = fields[nf - 1].toLowerCase();
      else lastfield = "h";   // defaults to hours

      // use first character to tell us what the unit is 
//      System.out.printf("last field %s  lastfield.substring(0,1) :%s:\n",
 //          lastfield,lastfield.substring(0,1));
      if(lastfield.startsWith("h")) delta = inputdelta / 24.;
      else if(lastfield.startsWith("m")) delta = inputdelta / 1440.;
      else if(lastfield.startsWith("s")) delta = inputdelta / 86400.;
      else if(lastfield.startsWith("d")) delta = inputdelta;
      else if(lastfield.startsWith("t")) delta = inputdelta / 1.0027379093;  
           // 1 sidereal day
      else if(lastfield.startsWith("l")) delta = 29.5307 * inputdelta;  
           // 1 lunation
      else if(lastfield.startsWith("y")) delta = 365. * inputdelta;  
      else delta = inputdelta / 24.;
/*      System.out.println("AdvanceTime, delta = " + String.format(Locale.ENGLISH, "%f",delta) + 
               "forward = " + forward); */
      
      if(forward) SetInstant(jd + delta, stdz, useDST, true);  
         // jd is always UT, so use_dst is true
      else {    
         SetInstant(jd - delta, stdz, useDST, true);  
        // System.out.printf("AdvanceTime: JD, delta %f %f  stdz %f useDST %d\n",
         //          jd,delta,stdz,useDST);
      }
   }

   void SetInstant( double jdin, double stdzin, int use_dst, boolean is_ut) {
      // Silly to repeat all that code just to change datatype ... 
      // but it'll work correctly at least.
   
      useDST = use_dst;

      if(is_ut) {
          jd = jdin;  // by definition, it's the JD
          UTDate = new GenericCalDat(jd);
          // The DST calculation is not sensitive to year if dstInEffect is 
          // computed using the same year as used in findDSTBounds.
          if(use_dst == 0) dstInEffect = false;
          else {  // determine if DST is in effect ... 
             DSTBounds = findDSTBounds(UTDate.year, stdzin, use_dst);
             if(use_dst > 0)  {  // northern hemisphere logic
                if(jd > DSTBounds[0] & jd < DSTBounds[1]) dstInEffect = true;
                else dstInEffect=false;
             }
             if(use_dst < 0) {  // southern hemisphere logic
                if(jd < DSTBounds[0] | jd > DSTBounds[1]) dstInEffect = true;
                else dstInEffect = false;
             }
             // System.out.printf("use_dst %d jd %f Bounds[0,1] = %f %f\n",
               //   use_dst,jd,DSTBounds[0],DSTBounds[1]);
             // if (dstInEffect) System.out.printf("DST is in effect.\n");
             // else System.out.printf("DST is NOT in effect.\n");
          }
          
          if(dstInEffect) localjd = jd - (stdzin - 1.) / 24.;
          else localjd = jd - (stdzin / 24.);
          localDate = new GenericCalDat(localjd);
      }
      else {  // input string is local date and time.
          localDate = new GenericCalDat(jdin);  // by definition .. 
          localjd = localDate.Cal2JD();
          if(use_dst == 0) dstInEffect = false;
          else { // dst is used if applicable ... use local-time limits of 
                 // applicability (DSTBounds [2] and [3] instead of [0] and [1])
             DSTBounds = findDSTBounds(localDate.year, stdzin, use_dst);
             if(use_dst > 0)  {  // northern hemisphere logic
                if(localjd > DSTBounds[2] & localjd < DSTBounds[3]) dstInEffect = true;
                else dstInEffect = false;
             }
             if(use_dst < 0) {  // southern hemisphere logic
                if(localjd < DSTBounds[2] | localjd > DSTBounds[3]) dstInEffect = true;
                else dstInEffect = false;
             }
          }

          if(dstInEffect) jd = localjd + (stdzin - 1.) / 24.;
          else jd = localjd + stdzin / 24.;
          UTDate = new GenericCalDat(jd);
       }
   }


   double [] findDSTBounds(int year, double stdz, int use_dst) {
      /** returns jdb and jde, first and last dates for dst in year.
          [0] and [1] are true JD, [2] and [3] are reckoned wrt local time.
          This proved to be useful later.
         The parameter use_dst allows for a number
            of conventions, namely:
                0 = don't use it at all (standard time all the time)
                1 = use USA convention (1st Sun in April to
                     last Sun in Oct 1986 - 2006; last Sun in April before;
                     2nd sunday in March to first in Nov from 2007 on.)
                2 = use Spanish convention (for Canary Islands)
                -1 = use Chilean convention (CTIO).
                -2 = Australian convention (for AAT).
            Negative numbers denote sites in the southern hemisphere,
            where jdb and jde are beginning and end of STANDARD time for
            the year. 
            It's assumed that the time changes at 2AM local time; so
            when clock is set ahead, time jumps suddenly from 2 to 3,
            and when time is set back, the hour from 1 to 2 AM local 
            time is repeated.  This could be changed in code if need be. 
         Straight translation of skycalc c routine.
      **/
       int nSundays = 0;
       int nSaturdays = 0;  // for Chile, keep descriptive name
       int trialday = 1;
       int trialdow = 0;
       String trialstring;
       double [] JDBoundary = {0.,0.,0.,0.};
       double jdtmp;

       GenericCalDat trial = new GenericCalDat("2000 1 1 1 1 1");
       if (use_dst == 0) {  // if DST is not used this shouldn't matter.
          return JDBoundary;  // this is a common case, get outta here fast.
       }
          
       else if (use_dst == 1) {
          // USA conventions from mid-60s (?) onward.
          // No attempt to account for energy-crisis measures (Nixon admin.)
          if(year < 1986) {  // last sunday in April 
             trialday = 30;
             while(trialdow != 6) {
                trialstring = String.format(Locale.ENGLISH, "%d 4 %d 2 0 0",year,trialday);
                trial.CalFromString(trialstring);
                trialdow = trial.DayOfWeek();
                trialday--;
             }
          }
          else if(year <= 2006) {  // first Sunday in April 
             trialday = 1;
             trialdow = 0;
             while(trialdow != 6) {
                trialstring = String.format(Locale.ENGLISH, "%d 4 %d 2 0 0",year,trialday);
                trial.CalFromString(trialstring);
                trialdow = trial.DayOfWeek();
                trialday++;
             }
          }
          else {  // 2007 and after, it's 2nd Sunday in March .... 
             nSundays = 0;
             trialday = 1;
             trialdow = 0;
             while(nSundays < 2) {
                trialstring = String.format(Locale.ENGLISH, "%d 3 %d 2 0 0",year,trialday);
                trial.CalFromString(trialstring);
                trialdow = trial.DayOfWeek();
                if(trialdow == 6) nSundays++;
                trialday++;
             }
          }
          jdtmp = trial.Cal2JD();
          JDBoundary[0] = jdtmp + stdz / 24.;  // true JD of change.
          JDBoundary[2] = jdtmp;  // local-time version
         

          trialdow = 0;  // for next round

          if(year < 2007) {  // last Sunday in October 
             trialday = 31;
             while(trialdow != 6) {
                trialstring = String.format(Locale.ENGLISH, "%d 10 %d 2 0 0",year,trialday);
                trial.CalFromString(trialstring);
                trialdow = trial.DayOfWeek();
                trialday--;
             }
          }
          else {   // first Sunday in November, didn't change in 1986. 
             trialday = 1;
             trialdow = 0;
             while(trialdow != 6) {
                trialstring = String.format(Locale.ENGLISH, "%d 11 %d 2 0 0",year,trialday);
                trial.CalFromString(trialstring);
                trialdow = trial.DayOfWeek();
                trialday++;
             }
          }
          jdtmp = trial.Cal2JD();
          JDBoundary[1] = jdtmp + (stdz - 1.) / 24.; // true JD
          JDBoundary[3] = jdtmp;  // local-time version
       } 

       else if(use_dst == 2) { // EU convention
           trialday = 31; // start last Sunday in March at 1 AM
           while(trialdow != 6) {
              trialstring = String.format(Locale.ENGLISH, "%d 3 %d 1 0 0",year,trialday);
              trial.CalFromString(trialstring);
              trialdow = trial.DayOfWeek();
              trialday--;
           }
           jdtmp = trial.Cal2JD();
           JDBoundary[0] = jdtmp + stdz / 24.;
           JDBoundary[2] = jdtmp;

           trialday = 30; // end last Sunday in October at 1 AM
           trialdow = 0;
           while(trialdow != 6) {
              trialstring = String.format(Locale.ENGLISH, "%d 10 %d 1 0 0",year,trialday);
              trial.CalFromString(trialstring);
              trialdow = trial.DayOfWeek();
              trialday--;
           }
           jdtmp = trial.Cal2JD();
           JDBoundary[1] = jdtmp + (stdz - 1.) / 24.;
           JDBoundary[3] = jdtmp;
       }
       else if(use_dst == -1) { // Chile - negative -> southern
           // In the south, [0] and [2] -> March-ish -> END of DST
           //               [1] and [3] -> Octoberish -> BEGIN DST

           trialday = 1;  // starts at 24h on 2nd Sat. of Oct.
           nSaturdays = 0;
           while(nSaturdays != 2) {
              trialstring = String.format(Locale.ENGLISH, "%d 10 %d 23 0 0",year,trialday);
                  // use 11 pm for day calculation to avoid any ambiguity
              trial.CalFromString(trialstring);
              trialdow = trial.DayOfWeek();
              if(trialdow == 5) nSaturdays++;
              trialday++;
           }
           jdtmp = trial.Cal2JD();
           JDBoundary[1] = jdtmp + (stdz + 1.) / 24.;  
           JDBoundary[3] = jdtmp;
              // add the hour back here (DST start)

           nSaturdays = 0;
           trialday = 1;
           while(nSaturdays != 2) { // end on the 2nd Sat in March
              trialstring = String.format(Locale.ENGLISH, "%d 3 %d 23 0 0",year,trialday);
                  // use 11 pm for day calculation to avoid any ambiguity
              trial.CalFromString(trialstring);
              trialdow = trial.DayOfWeek();
              if(trialdow == 5) nSaturdays++;
              trialday++;
           }
           jdtmp = trial.Cal2JD();
           JDBoundary[0] = jdtmp + stdz / 24.;  
           JDBoundary[2] = jdtmp;
              // no need to add the hour back, DST is ending now.
       }

       else if(use_dst == -2) {  // Australia (NSW)
           trialday = 31;
           trialdow = 0;
           while(trialdow != 6) { // DST begins at 2 AM, Last sun in Oct
              trialstring = String.format(Locale.ENGLISH, "%d 10 %d 2 0 0",year,trialday);
              trial.CalFromString(trialstring);
              trialdow = trial.DayOfWeek();
              trialday--;
           }
           jdtmp = trial.Cal2JD();
           JDBoundary[1] = jdtmp + stdz / 24.;  
           JDBoundary[3] = jdtmp;

           trialday = 31;
           trialdow = 0;
           while(trialdow != 6) { // DST ends at 3 AM, Last sun in March 
              trialstring = String.format(Locale.ENGLISH, "%d 3 %d 3 0 0",year,trialday);
              trial.CalFromString(trialstring);
              trialdow = trial.DayOfWeek();
              trialday--;
           }
           jdtmp = trial.Cal2JD();
           JDBoundary[0] = jdtmp + (stdz + 1.) / 24.;  
           JDBoundary[2] = jdtmp;
       }
       return JDBoundary;
   }
   double JulianEpoch() {
       return 2000. + (jd - Const.J2000) / 365.25;   // as simple as that ...
   }
}

/* Now come the classes that handle celestial coordinates ...  */

class RA implements Cloneable {
/** Right Ascension.  */

   double value;    // decimal hours
   sexagesimal sex;

   double adjra(double inval) {
      // System.out.printf("input inval %f ... ",inval);
      inval = inval % 24.;
      while(inval < 0.) inval += 24.;
      while(inval > 24.) inval -= 24.;
      // System.out.printf("returning inval = %f\n",inval);
      return inval;
   }

   // overloaded constructors are simply wrappers 
   public RA(double inval) {
      value = adjra(inval);
      sex = new sexagesimal(value);
   }
   public RA(String s) {
      sex = new sexagesimal(s);
      value = adjra(sex.value);
      sex.tosex(value);
   }

   public void setRA(double inval) {
      value = adjra(inval);
//      System.out.printf("Setting start %d %d %5.2f  %f  -> ",
//        sex.hour,sex.minute,sex.second,value);
      sex.tosex(value);
//      System.out.printf("%d %d %5.2f ... \n",sex.hour,sex.minute,sex.second);
   }

   public void setRA(String s) {
      sex.parseSexString(s);
      value = adjra(sex.value);
      sex.tosex(value);
   }
  
   public RA clone() {
      try {
          RA copy = (RA) super.clone();
          copy.value = value;
          copy.sex = (sexagesimal) sex.clone();
          return copy;
      } catch (CloneNotSupportedException e) {
          throw new Error("This should never happen!");
      }
   }

   public double radians() {
      return(value / Const.HRS_IN_RADIAN);
   }
   public double degrees() {
      return(value * 15.);
   }

   public String RoundedRAString(int ndigits, String divider) {

   /** Returns a rounded sexagesimal RA, with the cut imposed at 24 h */

      sexagesimal rounded;
      int secfieldwidth;
      double decimalMinute;
      String raformat = "";
      String outstr = "";

      rounded = sex.roundsex(ndigits);
      if(rounded.hour == 24) {  
         rounded.hour = 0;
         rounded.minute = 0;
         rounded.second = 0.;
      }
      if(ndigits >= 0) {
         if(ndigits == 0) raformat = 
           String.format(Locale.ENGLISH, "%%02d%s%%02d%s%%02.0f",divider,divider);
         else { 
           secfieldwidth=ndigits + 3;
           raformat = 
             String.format(Locale.ENGLISH, "%%02d%s%%02d%s%%0%1d.%1df",divider,divider,
                      secfieldwidth,ndigits);
         }
         outstr = String.format(Locale.ENGLISH, raformat,rounded.hour,rounded.minute,rounded.second);
      }
      else if (ndigits == -1) {
         decimalMinute = rounded.minute + rounded.second / 60.;
         outstr = String.format(Locale.ENGLISH, "%02d%s%04.1f",rounded.hour,divider,decimalMinute);
      }
      else if (ndigits == -2) {
         outstr = String.format(Locale.ENGLISH, "%02d%s%02d",rounded.hour,divider,rounded.minute);
      }
      return outstr;
   }
}  
 
class HA implements Cloneable {
/** Hour angle, nearly the same as RA, but force -12 to +12.  */

   double value;    // decimal hours
   sexagesimal sex;

   double adjha(double inval) {
      inval = inval % 24.;
      while(inval < -12.) inval += 24.;
      while(inval > 12.) inval -= 24.;
      return inval;
   }

   // overloaded constructors are simply wrappers 
   public HA(double inval) {
      setHA(inval);
   }
   public HA(String s) {
      setHA(s);
   }

   public void setHA(double inval) {
      value = adjha(inval);
      sex = new sexagesimal(value);
   }
   public void setHA(String s) {
      sex = new sexagesimal(s);
      value = adjha(sex.value);
      sex.tosex(value);
   }
 
   public HA clone() {
      try {
          HA copy = (HA) super.clone();
          copy.value = value;
          copy.sex = (sexagesimal) sex.clone();
          return copy;
      } catch (CloneNotSupportedException e) {
          throw new Error("This should never happen!");
      }
   }

   public double radians() {
      return(value / Const.HRS_IN_RADIAN);
   }
   public double degrees() {
      return(value * 15.);
   }

   public String RoundedHAString(int ndigits, String divider) {
   /** sexagesimal dec string, with the cut at +- 12 hr */
      
      sexagesimal rounded;
      int secfieldwidth;
      double decimalMinute;
      String haformat = "";
      String outstr = "";
      String signout = "+";
      
      rounded = sex.roundsex(ndigits);
      if(rounded.hour == 12 & rounded.sign == -1) {  
         rounded.hour = 12; 
         rounded.minute = 0;
         rounded.second = 0.;
         rounded.sign = 1;
      }
      // System.out.printf("rounded.sign = %d\n",rounded.sign);
      if(rounded.sign > 0) signout = "+";
         else signout = "-";
      if(ndigits >= 0) {
         if(ndigits == 0) haformat = 
           String.format(Locale.ENGLISH, "%s%%d%s%%02d%s%%02.0f",signout,divider,divider);
         else { 
           secfieldwidth=ndigits + 3;
           haformat = 
             String.format(Locale.ENGLISH, "%s%%d%s%%02d%s%%0%1d.%1df",signout,divider,divider,
                      secfieldwidth,ndigits);
         }
         outstr = String.format(Locale.ENGLISH, haformat,rounded.hour,rounded.minute,rounded.second);
      }
      else if (ndigits == -1) {
         decimalMinute = rounded.minute + rounded.second / 60.;
         outstr = String.format(Locale.ENGLISH, "%s%d%s%04.1f",signout,rounded.hour,divider,decimalMinute);
      }
      else if (ndigits == -2) {
         outstr = String.format(Locale.ENGLISH, "%s%d%s%02d",signout,rounded.hour,divider,rounded.minute);
      }
   
      return outstr;
   }
}  
  
class dec implements Cloneable {
/** declination.  */

   double value;    // decimal degrees 
   sexagesimal sex;

   // overloaded constructors are simply wrappers 
   public dec(double inval) {
      setDec(inval);
   }
   public dec(String s) {
      setDec(s);
   }
   // no good way to adjust out-of-range decs (it
   // doesn't wrap) so this is simpler than HA and RA
   public void setDec(double inval) {
      value = inval;
      sex = new sexagesimal(value);
   }
   public void setDec(String s) {
      sex = new sexagesimal(s);
      value = sex.value;
   }

   public dec clone() {
      try {
          dec copy = (dec) super.clone();
          copy.value = value;
          copy.sex = (sexagesimal) sex.clone();
          return copy;
      } catch (CloneNotSupportedException e) {
          throw new Error("This should never happen!");
      }
   }
 
   public double radians() {
      return(value / Const.DEG_IN_RADIAN);
   }
   public double degrees() {
      return(value);   // duuhh - but may be good to have
   }

   public String RoundedDecString(int ndigits, String divider) {
      // no need to wrap, so again simpler than HA and RA
      sexagesimal rounded;
      int secfieldwidth;
      double decimalMinute;
      String decformat = "";
      String outstr = "";
      String signout = "+";
      
      rounded = sex.roundsex(ndigits);

      if(rounded.sign > 0) signout = "+";
         else signout = "-";
      if(ndigits >= 0) {
         if(ndigits == 0) decformat = 
           String.format(Locale.ENGLISH, "%s%%02d%s%%02d%s%%02.0f",signout,divider,divider);
         else { 
           secfieldwidth=ndigits + 3;
           decformat = 
             String.format(Locale.ENGLISH, "%s%%02d%s%%02d%s%%0%1d.%1df",signout,divider,divider,
                      secfieldwidth,ndigits);
         }
         outstr = String.format(Locale.ENGLISH, decformat,rounded.hour,rounded.minute,rounded.second);
      }
      else if (ndigits == -1) {
         decimalMinute = rounded.minute + rounded.second / 60.;
         outstr = String.format(Locale.ENGLISH, "%s%02d%s%04.1f",signout,rounded.hour,divider,decimalMinute);
      }
      else if (ndigits == -2) {
         outstr = String.format(Locale.ENGLISH, "%s%02d%s%02d",signout,rounded.hour,divider,rounded.minute);
      }
      return outstr;
   }
}  
   
class Celest implements Cloneable {

   // A celestial coordinate, which knows its equinox.

   RA Alpha;
   dec Delta;
   double Equinox;
   double distance;  // not always used but sometimes useful
   double galat, galong;  //  galactic coords, in degrees.

   // To do:  Add an elaborate input parsing mechanism and overload
   // constructors like crazy. 
   public Celest(RA a, dec d, double e) {
      Alpha = a;
      Delta = d;
      Equinox = e;
      distance = 0.;
   }

   public Celest(double r, double d, double e) {  // ra, dec, and equinox 
                                         // decimal hours, degr.
      Alpha = new RA(r); 
      Delta = new dec(d); 
      Equinox = e;
      distance = 0.;
   }

   public Celest(double r, double d, double e, double dist) {  
                   // ra, dec, and equinox decimal hours, degr; dist in 
                   // undefined units.
      Alpha = new RA(r); 
      Delta = new dec(d); 
      Equinox = e;
      distance = dist;
   }

   public Celest(String [] s) {  // ra, dec, and equinox as separate strings
      Alpha = new RA(s[0]);
      Delta = new dec(s[1]);
      Equinox = Double.parseDouble(s[2]);
      distance = 0.;
   }
 
   public Celest(String ras, String decs, String eqs) {  
             // ra, dec, and equinox as separate strings not in an array
      Alpha = new RA(ras);
      Delta = new dec(decs);
      Equinox = Double.parseDouble(eqs);
      distance = 0.;
   }
 
   public Celest(String s) {  // colon-separated ra, dec, equinox (3 string)
      String [] fields;
     
      fields = s.split("\\s+");  // whitespace
      Alpha = new RA(fields[0]);
      Delta = new dec(fields[1]);
      Equinox = Double.parseDouble(fields[2]);
      distance = 0.;
   }

   public boolean equals( Object arg ) {
       // override (I hope) the Object equals method to use in at least one test later.
       if(( arg != null) && (arg instanceof Celest)) {
          Celest c = (Celest) arg;
          if(c.Alpha.value == Alpha.value && c.Delta.value == Delta.value &&
              c.Equinox == Equinox) return true;
          else return false;
       }
       else return false;
   }

   void UpdateFromStrings(String rastr, String decstr, String eqstr) {
      // updates a previously instantiated celest from three strings. 
      Alpha.setRA(rastr); 
      Delta.setDec(decstr);
      Equinox = Double.parseDouble(eqstr);
   }
 
   public Celest clone() {
      try {
          Celest copy = (Celest) super.clone();
          copy.Alpha = (RA) Alpha.clone();
          copy.Delta = (dec) Delta.clone();
          copy.Equinox = Equinox;
          copy.distance = distance;
          copy.galat = galat;
          copy.galong = galong;
          return copy;
      } catch (CloneNotSupportedException e) {
          throw new Error("This should never happen!");
      }
   }
 
   String checkstring() {
      String outstr = "";
      outstr = outstr + Alpha.RoundedRAString(2,":") + "  ";
      outstr = outstr + Delta.RoundedDecString(1,":") + "  ";
      outstr = outstr + String.format(Locale.ENGLISH, "%6.1f",Equinox);
      return outstr;
   }
 
   String shortstring() {
      String outstr = "";
      outstr = outstr + Alpha.RoundedRAString(1,":") + "  ";
      outstr = outstr + Delta.RoundedDecString(0,":") + "  ";
      //outstr = outstr + String.format(Locale.ENGLISH, "%6.1f",Equinox);
      return outstr;
   }

   static double [] XYZcel(double x, double y, double z) {
   /** Given x, y, and z, returns {ra, dec, distance}.  */
     
      double mod;
      double xy;
      double raout, decout;
      double [] radecReturn = {0.,0.,0.};

      mod = Math.sqrt(x*x + y*y + z*z);
      if(mod > 0.) {
         x = x / mod; y = y / mod; z = z / mod;
      }
      else {
         System.out.println("Zero modulus in XYZcel.");
         radecReturn[0] = 0.; radecReturn[1] = 0.;
         radecReturn[2] = 0.;
         return radecReturn;
      }

      // System.out.printf("XYZcel: %f %f %f ->\n",x,y,z);
      xy = Math.sqrt(x * x + y * y);
      if(xy < 1.0e-11) {  // on the pole
         raout = 0.;      // ra is degenerate
         decout = Const.PI_OVER_2;
         if(z < 0.) decout *= -1.;
      }
      else {
         raout = Math.atan2(y,x) * Const.HRS_IN_RADIAN;
         decout = Math.asin(z) * Const.DEG_IN_RADIAN;
      }
      // System.out.printf("%f %f\n",raout,decout);
      radecReturn[0] = raout;
      radecReturn[1] = decout;
      radecReturn[2] = mod;  // it will sometimees be useful to have the dist.
         
      return radecReturn;
   }

   double [] cel_unitXYZ() {
      /** Given instance variables, returns UNIT VECTOR {x,y,z}. */
      double [] retvals = {0.,0.,0.};
      double cosdec;

      cosdec = Math.cos(Delta.radians());
      retvals[0] = Math.cos(Alpha.radians()) * cosdec;
      retvals[1] = Math.sin(Alpha.radians()) * cosdec;
      retvals[2] = Math.sin(Delta.radians());
   
      return retvals;
   }
      
   double [] precess(double NewEquinox) {
      /** generates a unit vector XYZ in equinox NewEquinox from the current 
          Alpha, Delta, and Equinox.  I believe these are IUA 1976 precession
          constants, which are not fully up-to-date but which are close
          enough for most puropses. */
      double ti, tf, zeta, z, theta;
      double cosz, coszeta, costheta, sinz, sinzeta, sintheta;
      double [][] p = {{0.,0.,0.,},{0.,0.,0.},{0.,0.,0.}};
      double [] orig = {0.,0.,0.};
      double [] fin = {0.,0.,0.};

      int i, j;


      //System.out.printf("equinoxes %f %f\n",Equinox,NewEquinox);
      ti = (Equinox - 2000.) / 100.;
      tf = (NewEquinox - 2000. - 100. * ti) / 100.;
 
      zeta = (2306.2181 + 1.39656 * ti + 0.000139 * ti * ti) * tf +
         (0.30188 - 0.000344 * ti) * tf * tf + 0.017998 * tf * tf * tf;
      z = zeta + (0.79280 + 0.000410 * ti) * tf * tf + 0.000205 * tf * tf * tf;
      theta = (2004.3109 - 0.8533 * ti - 0.000217 * ti * ti) * tf
         - (0.42665 + 0.000217 * ti) * tf * tf - 0.041833 * tf * tf * tf;

      cosz = Math.cos(z / Const.ARCSEC_IN_RADIAN);
      coszeta = Math.cos(zeta / Const.ARCSEC_IN_RADIAN);
      costheta = Math.cos(theta / Const.ARCSEC_IN_RADIAN);
      sinz = Math.sin(z / Const.ARCSEC_IN_RADIAN);
      sinzeta = Math.sin(zeta / Const.ARCSEC_IN_RADIAN);
      sintheta = Math.sin(theta / Const.ARCSEC_IN_RADIAN);

      p[0][0] = coszeta * cosz * costheta - sinzeta * sinz;
      p[0][1] = -1. * sinzeta * cosz * costheta - coszeta * sinz;
      p[0][2] = -1. * cosz * sintheta;

      p[1][0] = coszeta * sinz * costheta + sinzeta * cosz;
      p[1][1] = -1. * sinzeta * sinz * costheta + coszeta * cosz;
      p[1][2] = -1. * sinz * sintheta;

      p[2][0] = coszeta * sintheta;
      p[2][1] = -1. * sinzeta * sintheta;
      p[2][2] = costheta;

      orig[0] = Math.cos(Delta.radians()) * Math.cos(Alpha.radians());
      orig[1] = Math.cos(Delta.radians()) * Math.sin(Alpha.radians());
      orig[2] = Math.sin(Delta.radians()); 

      for(i = 0; i < 3; i++) {  // matrix multiplication 
         fin[i] = 0.;
         //System.out.printf("orig[%d] = %f\n",i,orig[i]);
         for(j = 0; j < 3; j++) {
             //System.out.printf("%d%d: %f  ",i,j,p[i][j]);
             fin[i] += p[i][j] * orig[j];
         }
         //System.out.printf("\nfin[%d] = %f\n\n",i,fin[i]);
      }
      return XYZcel(fin[0],fin[1],fin[2]);

   }

   public void selfprecess(double newEquinox) {
      /** precesses a Celest in place. */
      double [] radecOut;
      radecOut = precess(newEquinox);
      Alpha.setRA(radecOut[0]);
      Delta.setDec(radecOut[1]);
      Equinox = newEquinox;

   }

   public Celest precessed(double newEquinox) {
      /** returns a new Celest precessed from this one. */
      double [] radecOut;
      radecOut = precess(newEquinox);
      // System.out.printf("radecOut %f %f\n",radecOut[0],radecOut[1]);
      Celest outputCel = new Celest(radecOut[0],radecOut[1],newEquinox);
      return outputCel;
   }

   void galactic() {
      /** computes instance variables galong and galat.  Algorithm is 
          rigorous. */
      
              Celest cel1950 = precessed(1950);
      double [] xyz;
      double [] xyzgal = {0.,0.,0.};
      double [] temp;
      
      double  p11= -0.066988739415,
	p12= -0.872755765853,
	p13= -0.483538914631,
	p21=  0.492728466047,
	p22= -0.450346958025,
	p23=  0.744584633299,
	p31= -0.867600811168,
	p32= -0.188374601707,
	p33=  0.460199784759; 
     
	xyz = cel1950.cel_unitXYZ();

        xyzgal[0] = xyz[0] * p11 + xyz[1] * p12 + xyz[2] * p13;
        xyzgal[1] = xyz[0] * p21 + xyz[1] * p22 + xyz[2] * p23;
        xyzgal[2] = xyz[0] * p31 + xyz[1] * p32 + xyz[2] * p33;
        temp = XYZcel(xyzgal[0],xyzgal[1],xyzgal[2]);
        galong = temp[0] * 15.;
        while (galong < 0.) galong += 360.;  
        galat = temp[1];
   }
}   
  
class latitude extends dec implements Cloneable {

/** latitude, basically the same structure as a dec.  */

   // overloaded constructors are simply wrappers 
   public latitude(double inval) {
     super(inval);
   }
   public latitude(String s) {
     super(s);
   }
   public latitude clone() {
      //try {
         latitude copy = (latitude) super.clone();
         return copy;
      //} catch (CloneNotSupportedException e) {
       //  throw new Error ("This should never happen!");
      //}
   }
}

class longitude implements Cloneable {
 
/** longitude, +- 12 hours.  Includes some special hooks for
    interpreting inmput strings .*/

   double value;   // internally, hours west.
   sexagesimal sex;

   double adjlongit(double inval) {
      inval = inval % 24.;
      while(inval < -12.) inval += 24.;
      while(inval > 12.) inval -= 24.;
      return inval;
   }

   longitude(double x) {
     setFromDouble(x);
   }
   longitude(String s) {
     setFromString(s);
   }

   public longitude clone() {
      try {
         longitude copy = (longitude) super.clone();
         copy.value = value;
         copy.sex = (sexagesimal) sex.clone();
         return copy;
      } catch (CloneNotSupportedException e) {
         throw new Error ("This should never happen!");
      }
   }

   void setFromDouble(double x) {
     value = adjlongit(x);  // force value to +- 12 h on input.
     sex = new sexagesimal(value);
   }
   
   void setFromString(String s) {
      String [] fields;
      String unitfield;
      String directionfield;
      boolean indegrees = false, positiveEast = false;
            
      int nf, i;

// THIS NEEDS WORK to avoid colliding with rules used in the 
// interpretation of sexagesimals.  Need to test for letters in last
// couple of pieces of the string, then feed the sexagesimal converter
// with only the remaining pieces.

      // check for a label at the end ...  
      fields = s.split("\\s+");  // whitespace
      nf = fields.length;
      unitfield = fields[nf - 2].toLowerCase();
     // System.out.printf("last field %s\n",lastfield);
      i = unitfield.indexOf("h");
      if(i > -1) {
         indegrees = false;
        // System.out.println("h found ... ");
      }
      i = unitfield.indexOf("d");
      if(i > -1) {
         indegrees = true;
        // System.out.println("d found ... ");
      }
      directionfield = fields[nf - 1].toLowerCase();
      i = directionfield.indexOf("e");
      if(i > -1) {
         positiveEast = true;
        // System.out.println("e found ... ");
      }
      i = directionfield.indexOf("w");
      if(i > -1) {
         positiveEast = false;
        // System.out.println("w found ... ");
      }
      sex = new sexagesimal(s);
      value = sex.value;
      if(indegrees) value = value / 15.;
      if(positiveEast) value *= -1.;
     // System.out.printf("Value = %f\n",value);
      value = adjlongit(value);
      sex.tosex(value);
   }

   double radiansWest() {
      return (value / Const.HRS_IN_RADIAN);
   }
   
   double hoursWest() {
      return value;
   }
   double degreesEast() {
      return value * -15.;
   }

   public String RoundedLongitString(int ndigits, String divider, 
         boolean inDegrees) {
      
      sexagesimal outvalsex, rounded;
      double outval;  
      int secfieldwidth;
      double decimalMinute;
      String raformat = "";
      String outstr = "";

      if(inDegrees) outval = value * 15.;
      else outval = value;

      outvalsex = new sexagesimal(outval);
      rounded = outvalsex.roundsex(ndigits);

      // System.out.printf("rounded value is %f\n",rounded.value);

      if(rounded.hour == 24) {  
         rounded.hour = 0;
         rounded.minute = 0;
         rounded.second = 0.;
      }
      if(ndigits >= 0) {
         if(ndigits == 0) raformat = 
           String.format(Locale.ENGLISH, "%%02d%s%%02d%s%%02.0f",divider,divider);
         else { 
           secfieldwidth=ndigits + 3;
           raformat = 
             String.format(Locale.ENGLISH, "%%02d%s%%02d%s%%0%1d.%1df",divider,divider,
                      secfieldwidth,ndigits);
         }
         outstr = String.format(Locale.ENGLISH, raformat,rounded.hour,rounded.minute,rounded.second);
      }
      else if (ndigits == -1) {
         decimalMinute = rounded.minute + rounded.second / 60.;
         outstr = String.format(Locale.ENGLISH, "%02d%s%04.1f",rounded.hour,divider,decimalMinute);
      }
      else if (ndigits == -2) {
         outstr = String.format(Locale.ENGLISH, "%02d%s%02d",rounded.hour,divider,rounded.minute);
      }

      if(inDegrees) outstr = outstr + " D";
      else outstr = outstr + " H";
      if(rounded.sign == 1) outstr = outstr + " W";
      else outstr = outstr + " E";

      return outstr;
   }
}



class Site implements Cloneable {

   // Complicated class holding much data about sites, which at this point
   // are all `canned'.  

   String name;          // name of site
   latitude lat;         // geographic latitude
   longitude longit;     // longitude
   double stdz;          // time zone offset from UT, decimal hrs
   int use_dst;          // code for DST use, 0 = none, 1 = US, etc.
   String timezone_name; // name of timezone e.g. "Mountain"
   String zone_abbrev;   // one-letter abbreviation of timezone
   double elevsea;       // elevation above sea level, meters
   double elevhoriz;     // elevation above the typical local horizon, m.

//   Site(HashMap<String, Site> sitelist, String sitename) {
//      Reload(sitelist, sitename);
//   }

//   void Reload(Site [] sitelist, string sitename) {
//        Site tempsite = sitelist.get(sitename);
//        name = tempsite.name;
//        longit = new longitude(tempsite.longit.value);
//        lat = new latitude(tempsite.lat.value);
//        stdz = tempsize.stdz;
//        use_dst = tempsite.use_dst;
//        timezone_name = tempsite.timezone_name;
//        zone_abbrev = tempsite.zone_abbrev;
//        elevsea = tempsite.elevsea;
//        elevhoriz = tempsite.elev_horiz;
//   }

   void Reload(Site s) {
        name = s.name;
        longit = (longitude) s.longit.clone();
        lat = (latitude) s.lat.clone();
        stdz = s.stdz;
        use_dst = s.use_dst;
        timezone_name = s.timezone_name;
        zone_abbrev = s.zone_abbrev;
        elevsea = s.elevsea;
        elevhoriz = s.elevhoriz;
   }

   Site(String [] sitepars) {
        // for(int j = 0; j < 9; j++) System.out.printf("%d %s\n",j,sitepars[j]);
        name = sitepars[0];
        // System.out.printf("%s %s",sitepars[0],sitepars[1]);
        // System.out.printf("-> %f\n",Double.parseDouble(sitepars[1].trim()));
        longit = new longitude(Double.parseDouble(sitepars[1].trim()));
        lat = new latitude(Double.parseDouble(sitepars[2].trim()));
        stdz = Double.parseDouble(sitepars[3].trim());
        use_dst = Integer.parseInt(sitepars[4].trim());
        timezone_name = sitepars[5];
        zone_abbrev = sitepars[6];
        elevsea = Double.parseDouble(sitepars[7].trim());
        elevhoriz = Double.parseDouble(sitepars[8].trim());
  }

  void dumpsite() {  // for diagnostics
    System.out.printf("%s\n",name);
    System.out.printf("longitude %s\n",longit.RoundedLongitString(1,":",true));
    System.out.printf("latitude  %s\n",lat.RoundedDecString(0,":"));
    System.out.printf("Zone offset from UT %6.3f hours\n",stdz);
  }

  public boolean equals(Object arg) {
    if((arg != null) && (arg instanceof Site)) {
       Site ss = (Site) arg;
       if(!(ss.name.equals(name))) return false;
       if(ss.lat.value != lat.value) return false;
       if(ss.longit.value != longit.value) return false;
       if(ss.stdz != stdz) return false;
       // close enough ... 
    }
    return true;
  }

  public Site clone() {
     try {
        Site copy = (Site) super.clone();
         copy.name = name;
         copy.lat = (latitude) lat.clone();
         copy.longit = (longitude) longit.clone();
         copy.stdz = stdz;
         copy.use_dst = use_dst;
         copy.timezone_name = timezone_name;
         copy.zone_abbrev = zone_abbrev;
         copy.elevsea = elevsea;
         copy.elevhoriz = elevhoriz;
        return copy;
     } catch (CloneNotSupportedException e) {
        throw new Error("This should never happen!");
     }
  }
}

class Spherical {
  /** container for several static spherical trig methods. */
  
  static double subtend(Celest a, Celest b) {
   /** angle in radians between two positions. */
      double [] aCart = {0.,0.,0.};
      double [] bCart = {0.,0.,0.};
      Celest bprime;
      double dotproduct, theta;
      double dr,dd;

      if(Math.abs(a.Equinox - b.Equinox) > 0.001) 
         bprime = b.precessed(a.Equinox);
      else bprime = (Celest) b.clone();

      aCart = a.cel_unitXYZ();
      bCart = bprime.cel_unitXYZ();
      
      dotproduct = aCart[0] * bCart[0] + aCart[1] * bCart[1] + 
                   aCart[2] * bCart[2];

      theta = Math.acos(dotproduct);
     
      // if the angle is tiny, use a flat sky approximation away from
      // the poles to get a more accurate answer.
      if(theta < 1.0e-5) {  
          if(Math.abs(a.Delta.radians()) < (Const.PI_OVER_2 - 0.001) &&
             Math.abs(bprime.Delta.radians()) < (Const.PI_OVER_2 - 0.001)) {
		dr = (bprime.Alpha.radians() - a.Alpha.radians()) * 
                       Math.cos((a.Delta.radians() + bprime.Delta.radians())/2.);
                dd = bprime.Delta.radians() - a.Delta.radians();
                theta = Math.sqrt(dr*dr + dd*dd);
                
          }
      }
      return theta;
  }

  static double [] CuspPA(Celest s, Celest m) {
  /** Given the positions of the sun and the moon, returns the position angle
      of the line connecting the moon's two cusps.  Ported from python. */
      double codecsun = (90. - s.Delta.value) / Const.DEG_IN_RADIAN;
      double codecmoon = (90. - m.Delta.value) / Const.DEG_IN_RADIAN;
      double dra = s.Alpha.value - m.Alpha.value;
      while(dra < -12.) dra += 24.; 
      while(dra >= 12.) dra -= 24.; 
      dra = dra / Const.HRS_IN_RADIAN;

     // Spherical law of cosines gives moon-sun separation
     double moonsun = Math.acos(Math.cos(codecsun) * Math.cos(codecmoon) +
       Math.sin(codecsun) * Math.sin(codecmoon) * Math.cos(dra));
     // spherical law of sines + law of cosines needed to get
     // sine and cosine of pa separately; this gets quadrant etc.!
     double pasin = Math.sin(dra) * Math.sin(codecsun) / Math.sin(moonsun);
     double pacos = (Math.cos(codecsun) - 
         Math.cos(codecmoon) * Math.cos(moonsun)) / 
        (Math.sin(codecmoon) * Math.sin(moonsun));
     double pa = Math.atan2(pasin,pacos);

//   print "pa of arc from moon to sun is %5.2f deg." % (pa * _skysub.DEG_IN_RADIAN)

     double cusppa = pa - Const.PI/2.;   // line of cusps ... 
//     System.out.printf("cusppa = %f\n",cusppa);
     double [] retvals = {cusppa, moonsun};
     return retvals ;
  }
 
  static double [] min_max_alt (double lat, double dec) {
      /** returns the minimum and maximum altitudes (degrees) of an object at
          declination dec as viewed from lat.  */
      /* translated straight from skycalc. */

      double x, latrad, decrad;
      double [] retvals = {0.,0.};

      latrad = lat / Const.DEG_IN_RADIAN;
      decrad = dec / Const.DEG_IN_RADIAN;
      x = Math.cos(decrad) * Math.cos(latrad) + Math.sin(decrad) * Math.sin(latrad);
      if(Math.abs(x) <= 1.) retvals[1] = Math.asin(x) * Const.DEG_IN_RADIAN;
      else System.out.printf("min_max_alt ... asin(>1)\n");

      x = Math.sin(decrad) * Math.sin(latrad) - Math.cos(decrad) * Math.cos(latrad);
      if(Math.abs(x) <= 1.) retvals[0] = Math.asin(x) * Const.DEG_IN_RADIAN;
      else System.out.printf("min_max_alt ... asin(>1)\n");
      
      return retvals;
  }
  
  static double ha_alt(double dec, double lat, double alt)  {
      /** Finds the hour angle at which an object at declination dec is at altitude
          alt, as viewed from latitude lat;  returns 1000 if always higher, 
          -1000 if always lower. */

      double x, coalt, codec, colat;
      double [] minmax;

      minmax = min_max_alt(lat,dec);
      if(alt < minmax[0]) return(1000.);   // always higher than asked
      if(alt > minmax[1]) return(-1000.);  // always lower than asked
    
      // System.out.printf("dec %f lat %f alt %f ... \n",dec,lat,alt);     
      codec = Const.PI_OVER_2 - dec / Const.DEG_IN_RADIAN;
      colat = Const.PI_OVER_2 - lat / Const.DEG_IN_RADIAN;
      coalt = Const.PI_OVER_2 - alt / Const.DEG_IN_RADIAN;
      x = (Math.cos(coalt) - Math.cos(codec) * Math.cos(colat)) / 
           (Math.sin(codec) * Math.sin(colat));
      if(Math.abs(x) <= 1.) return(Math.acos(x) * Const.HRS_IN_RADIAN);
      else {
          System.out.printf("Bad inverse trig in ha_alt ... acos(%f)\n",x);
          return(1000.);
      }
   }

   static double true_airmass(double alt) {
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

   static Celest Gal2Cel(double galacticlongit, double galacticlatit) {
      /** computes instance variables galong and galat.  Algorithm is 
          rigorous. */
      
      double [] xyz = {0., 0., 0.};
      double [] xyzgal = {0.,0.,0.};
      double [] temp;
      
      double  p11= -0.066988739415,
	p12= -0.872755765853,
	p13= -0.483538914631,
	p21=  0.492728466047,
	p22= -0.450346958025,
	p23=  0.744584633299,
	p31= -0.867600811168,
	p32= -0.188374601707,
	p33=  0.460199784759; 

      double galongitrad = galacticlongit / Const.DEG_IN_RADIAN;
      double galatitrad = galacticlatit / Const.DEG_IN_RADIAN;

      xyzgal[0] = Math.cos(galongitrad) * Math.cos(galatitrad);
      xyzgal[1] = Math.sin(galongitrad) * Math.cos(galatitrad);
      xyzgal[2] = Math.sin(galatitrad);
      // System.out.printf("Galactic xyz %f %f %f\n",xyzgal[0],xyzgal[1],xyzgal[2]);

      // for rotation matrices, inverse is the transpose, so ... 
      xyz[0] = xyzgal[0] * p11 + xyzgal[1] * p21 + xyzgal[2] * p31;
      xyz[1] = xyzgal[0] * p12 + xyzgal[1] * p22 + xyzgal[2] * p32;
      xyz[2] = xyzgal[0] * p13 + xyzgal[1] * p23 + xyzgal[2] * p33;
      // System.out.printf("Equatorial xyz %f %f %f\n",xyz[0],xyz[1],xyz[2]);
        
      double [] retvals = Celest.XYZcel(xyz[0],xyz[1],xyz[2]);
      Celest cel = new Celest(retvals[0],retvals[1],1950.);  // galactic are defined for 1950
  
      return cel;   // and precess elsehwere to whatever.

   }
}

class Topo {  // topocentric correction stuff 

  static final double FLATTEN = 0.003352813;  // flattening, 1/298.257
  static final double EQUAT_RAD = 6378137.;   // equatorial radius, meters

  static double [] Geocent(double longitin, double latitin, double height) {
      // XYZ coordinates given geographic.  Declared static because it will often
      // be used with lst in place of longitude.  
      // input is decimal hours, decimal degrees, and meters. 
      // See 1992 Astr Almanac, p. K11.
      
      double denom, C_geo, S_geo;
      double geolat, coslat, sinlat, geolong, sinlong, coslong;
      double [] retval = {0.,0.,0.};
     

      //System.out.printf("lat long %f %f\n",latitin,longitin);
      geolat = latitin / Const.DEG_IN_RADIAN;
      geolong = longitin / Const.HRS_IN_RADIAN;
      //System.out.printf("radians %f %f \n",geolat,geolong);
      coslat = Math.cos(geolat);  sinlat = Math.sin(geolat);
      coslong = Math.cos(geolong); sinlong = Math.sin(geolong);

      denom = (1. - FLATTEN) * sinlat;
      denom = coslat * coslat + denom * denom;
      C_geo = 1./ Math.sqrt(denom);
      S_geo = (1. - FLATTEN) * (1. - FLATTEN) * C_geo;
      C_geo = C_geo + height / EQUAT_RAD;
      S_geo = S_geo + height / EQUAT_RAD;
      retval[0] = C_geo * coslat * coslong;
      retval[1] = C_geo * coslat * sinlong;
      retval[2] = S_geo * sinlat;

      return retval;
  }

  static Celest topocorr(Celest geopos, InstantInTime when, Site where, double sidereal) {
      // The geopos to which this is being applied needs to have its
      // distance set.
      
      double x, y, z, x_geo, y_geo, z_geo, topodist;
      double [] retvals;

      x = Math.cos(geopos.Alpha.radians()) * 
          Math.cos(geopos.Delta.radians()) * geopos.distance;
      y = Math.sin(geopos.Alpha.radians()) * 
          Math.cos(geopos.Delta.radians()) * geopos.distance;
      z = Math.sin(geopos.Delta.radians()) * geopos.distance;

      retvals = Geocent(sidereal,where.lat.value,
        where.elevsea);
      x_geo = retvals[0] / Const.EARTHRAD_IN_AU;
      y_geo = retvals[1] / Const.EARTHRAD_IN_AU;
      z_geo = retvals[2] / Const.EARTHRAD_IN_AU;

      x = x - x_geo;
      y = y - y_geo;
      z = z - z_geo;

      topodist = Math.sqrt(x*x + y*y + z*z);

      x /= topodist;
      y /= topodist;
      z /= topodist;

      return new Celest(Math.atan2(y,x) * Const.HRS_IN_RADIAN,
                        Math.asin(z) * Const.DEG_IN_RADIAN,
                        when.JulianEpoch(), topodist);
   }
}

class Ecliptic {
  /* Holds some static methods for rotating from ecliptic to equatorial and back ... */

  static double [] eclrot(double jd, double x, double y, double z) {
  /** rotates x,y,z coordinates to equatorial x,y,z; all are 
      in equinox of date. Returns [0] = x, [1] = y, [2] = z */
     double incl;
     double T;
     double [] retval = {0.,0.,0.};
    
     T = (jd - Const.J2000) / 36525;  
     incl = (23.439291 + T * (-0.0130042 - 0.00000016 * T))/Const.DEG_IN_RADIAN;
             /* 1992 Astron Almanac, p. B18, dropping the 
               cubic term, which is 2 milli-arcsec! */
     // System.out.printf("T incl %f %f\n",T,incl);
     retval[1] = Math.cos(incl) * y - Math.sin(incl) * z;
     retval[2] = Math.sin(incl) * y + Math.cos(incl) * z;
     retval[0] = x;
     return(retval);
  }

  static double [] Cel2Ecl(Observation o) {
  /** rotates celestial coords to equatorial at 
      equinox of jd. Returns [0] = x, [1] = y, [2] = z */
     double incl;
     double T;
     double julep;
     double [] retval = {0.,0.};   // ecliptic longitude and latitude
     double [] equat = {0.,0.,0.}; // equatorial unit vector
     double [] eclipt = {0.,0.,0.}; // ecliptic unit vector
    
     T = (o.w.when.jd - Const.J2000) / 36525;  
     incl = (23.439291 + T * (-0.0130042 - 0.00000016 * T))/Const.DEG_IN_RADIAN;
             /* 1992 Astron Almanac, p. B18, dropping the 
               cubic term, which is 2 milli-arcsec! */
     // System.out.printf("T incl %f %f\n",T,incl);
     
     equat = o.current.cel_unitXYZ();

     eclipt[1] = Math.cos(incl) * equat[1] + Math.sin(incl) * equat[2];
     eclipt[2] = -1. * Math.sin(incl) * equat[1] + Math.cos(incl) * equat[2];
     eclipt[0] = equat[0];

     retval[0] = Math.atan2(eclipt[1],eclipt[0]) * Const.DEG_IN_RADIAN;
     while(retval[0] < 0.) retval[0] += 360.;
     while(retval[0] >= 360.) retval[0] -= 360.;
     retval[1] = Math.asin(eclipt[2]) * Const.DEG_IN_RADIAN;

     return(retval);
  }
}

class deltaT {
  /* holds only a static method that returns a rough ephemeris time 
     correction */

  static double etcorr(double jd) {

	/* Given a julian date in 1900-2100, returns the correction
           delta t which is:
		TDT - UT (after 1983 and before 1998)
		ET - UT (before 1983)
		an extrapolated guess  (after 2001). 

	For dates in the past (<= 2001 and after 1900) the value is linearly
        interpolated on 5-year intervals; for dates after the present,
        an extrapolation is used, because the true value of delta t
	cannot be predicted precisely.  Note that TDT is essentially the
	modern version of ephemeris time with a slightly cleaner 
	definition.  

	Where the algorithm shifts there will be a small (< 0.1 sec)
        discontinuity.  Also, the 5-year linear interpolation scheme can 
        lead to errors as large as 0.5 seconds in some cases, though
 	usually rather smaller.   One seldom has actual UT to work with anyway,
	since the commonly-used UTC is tied to TAI within an integer number
	of seconds.  */

	double jd1900 = 2415019.5;
	double [] dates = {1900.,1905.,1910.,1915.,1920.,1925.,1930.,
          1935.,1940.,1945.,1950.,1955.,1960.,1965.,1970.,1975.,1980.,
          1985.,1990.,1995.,2000.,2004.};
          // 2004 is the last one tabulated in the 2006 almanac
	double [] delts = { -2.72, 3.86, 10.46, 17.20, 21.16, 23.62,
	24.02,  23.93, 24.33, 26.77,  29.15, 31.07, 33.15,  35.73, 40.18,
	45.48,  50.54, 54.34, 56.86,  60.78, 63.83, 64.57};
	double year, delt = 0.;
	int i;

	year = 1900. + (jd - jd1900) / 365.25;

	if(year < 2004. && year >= 1900.) {
		i = (int) ((year - 1900) / 5);
		delt = delts[i] + 
		 ((delts[i+1] - delts[i])/(dates[i+1] - dates[i])) * (year - dates[i]);
	}

	else if (year >= 2004. && year < 2100.)
		delt = 31.69 + (2.164e-3) * (jd - 2436935.4);  /* rough extrapolation */
                /* the 31.69 is adjusted to give 64.09 sec at the start of 2001. */
	else if (year < 1900) {
		// printf("etcorr ... no ephemeris time data for < 1900.\n");
       		delt = 0.;
	}

	else if (year >= 2100.) {
		// printf("etcorr .. very long extrapolation in delta T - inaccurate.\n");
		delt = 180.; /* who knows? */
	} 

	return(delt);
  }
}

class SkyIllum {
/** container for the ztwilight and krisciunas/schaefer routines */
   
   static double ztwilight(double alt) {

/* evaluates a polynomial expansion for the approximate brightening
   in magnitudes of the zenith in twilight compared to its 
   value at full night, as function of altitude of the sun (in degrees).
   To get this expression I looked in Meinel, A.,
   & Meinel, M., "Sunsets, Twilight, & Evening Skies", Cambridge U.
   Press, 1983; there's a graph on p. 38 showing the decline of 
   zenith twilight.  I read points off this graph and fit them with a
   polynomial; I don't even know what band there data are for! */
/* Comparison with Ashburn, E. V. 1952, JGR, v.57, p.85 shows that this
   is a good fit to his B-band measurements.  */

        double y, val;

        if(alt > 0.) return 99.;  // guard 
        if(alt < -18.) return 0.;
       	
        y = (-1.* alt - 9.0) / 9.0;  /* my polynomial's argument...*/
	val = ((2.0635175 * y + 1.246602) * y - 9.4084495)*y + 6.132725;
	return(val);
   }

   static double lunskybright(double alpha,double rho,double kzen,
		double altmoon, double alt, double moondist)  {

/* Evaluates predicted LUNAR part of sky brightness, in 
   V magnitudes per square arcsecond, following K. Krisciunas
   and B. E. Schaeffer (1991) PASP 103, 1033.

   alpha = separation of sun and moon as seen from earth,
   converted internally to its supplement,
   rho = separation of moon and object,
   kzen = zenith extinction coefficient, 
   altmoon = altitude of moon above horizon,
   alt = altitude of object above horizon 
   moondist = distance to moon, in earth radii

   all are in decimal degrees. */

    double istar,Xzm,Xo,Z,Zmoon,Bmoon,fofrho,rho_rad,test;

    rho_rad = rho/Const.DEG_IN_RADIAN;
    alpha = (180. - alpha); 
    Zmoon = (90. - altmoon)/Const.DEG_IN_RADIAN;
    Z = (90. - alt)/Const.DEG_IN_RADIAN;
    moondist = Const.EARTHRAD_IN_AU * moondist/(60.27);  
        /* distance arrives in AU, want it normalized to mean distance, 
            60.27 earth radii. */

    istar = -0.4*(3.84 + 0.026*Math.abs(alpha) + 4.0e-9*Math.pow(alpha,4.)); /*eqn 20*/
    istar =  Math.pow(10.,istar)/(moondist * moondist);
    if(Math.abs(alpha) < 7.)   /* crude accounting for opposition effect */
	istar = istar * (1.35 - 0.05 * Math.abs(istar));
	/* 35 per cent brighter at full, effect tapering linearly to 
	   zero at 7 degrees away from full. mentioned peripherally in 
	   Krisciunas and Scheafer, p. 1035. */
    fofrho = 229087. * (1.06 + Math.cos(rho_rad)*Math.cos(rho_rad));
    if(Math.abs(rho) > 10.)
       fofrho=fofrho + Math.pow(10.,(6.15 - rho/40.));            /* eqn 21 */
    else if (Math.abs(rho) > 0.25)
       fofrho= fofrho+ 6.2e7 / (rho*rho);   /* eqn 19 */
    else fofrho = fofrho+9.9e8;  /*for 1/4 degree -- radius of moon! */
    Xzm = Math.sqrt(1.0 - 0.96*Math.sin(Zmoon)*Math.sin(Zmoon));
    if(Xzm != 0.) Xzm = 1./Xzm;  
	  else Xzm = 10000.;     
    Xo = Math.sqrt(1.0 - 0.96*Math.sin(Z)*Math.sin(Z));
    if(Xo != 0.) Xo = 1./Xo;
	  else Xo = 10000.; 
    Bmoon = fofrho * istar * Math.pow(10.,(-0.4*kzen*Xzm)) 
	  * (1. - Math.pow(10.,(-0.4*kzen*Xo)));   /* nanoLamberts */
    if(Bmoon > 0.001) 
      return(22.50 - 1.08574 * Math.log(Bmoon/34.08)); /* V mag per sq arcs-eqn 1 */
    else return(99.);                                     
  }
}

class Sun implements Cloneable {
   Celest geopos;
   Celest topopos;
   double [] xyz = {0.,0.,0.};     /* for use in barycentric correction */
   double [] xyzvel = {0.,0.,0.};  /* ditto. */

   Sun(WhenWhere w) {
       update(w.when,w.where,w.sidereal);
   }

   Sun(InstantInTime inst) {  // no site, so no topo
       double retvals [];
       retvals = computesun(inst.jd);
       xyz[0] = retvals[3]; xyz[1] = retvals[4]; xyz[2] = retvals[5];       
       geopos = new Celest(retvals[0],retvals[1],inst.JulianEpoch());
       geopos.distance = retvals[2];
       topopos = new Celest(0.,0.,inst.JulianEpoch());  // not set
       topopos.distance = 0.;
   }

   Sun(double jdin) {        // no site, so no topo possible
       double retvals [];
       double eq;

       retvals = computesun(jdin);
       xyz[0] = retvals[3]; xyz[1] = retvals[4]; xyz[2] = retvals[5];       
       eq = 2000. + (jdin - Const.J2000) / 365.25;
       geopos = new Celest(retvals[0],retvals[1],eq);
       geopos.distance = retvals[2];
       topopos = new Celest(0.,0.,eq); // not set, no geogr. info
       topopos.distance = 0.;
   }
 
   public Sun clone() {
      try {
          Sun copy = (Sun) super.clone();
          copy.geopos = (Celest) geopos.clone();
          copy.topopos = (Celest) topopos.clone();
          copy.xyz = xyz;
          copy.xyzvel = xyzvel;
          return copy;
      } catch (CloneNotSupportedException e) {
          throw new Error("This should never happen!");
      }
   }
 
   void update(InstantInTime when, Site where, double sidereal) {
       // need to avoid handing in a whenwhere or get circularity ...
        double [] retvals;
       
       // System.out.printf("updating sun jd %f ... ",w.when.jd);
       retvals = computesun(when.jd);
       xyz = new double [3];
       xyz[0] = retvals[3]; xyz[1] = retvals[4]; xyz[2] = retvals[5];       

       //System.out.printf("Sun constructor - jd %f xyz = %f %f %f\n",
       //     w.when.jd,xyz[0],xyz[1],xyz[2]);
       
       // ignoring topocentric part of helio time correction.

       geopos = new Celest(retvals[0],retvals[1],when.JulianEpoch());
       geopos.distance = retvals[2];
       topopos = Topo.topocorr(geopos,when,where,sidereal);  
       // System.out.printf("topo radec %s %s\n",topopos.Alpha.RoundedRAString(2,":"),
         //        topopos.Delta.RoundedDecString(1,":"));
   }     


/* Implements Jean Meeus' solar ephemer is, from Astronomical
   Formulae for Calculators, pp. 79 ff.  Position is wrt *mean* equinox of 
   date. */   
   
   static double [] computesun(double jd) {
       double xecl, yecl, zecl;
       double [] equatorial = {0.,0.,0.};
       double e, L, T, Tsq, Tcb;
       double M, Cent, nu, sunlong;
       double Lrad, Mrad, nurad, R;
       double A, B, C, D, E, H;
       double [] retvals = {0.,0.,0.,0.,0.,0.};
            // will be geora, geodec, geodist, x, y, z (geo)
       
       // correct jd to ephemeris time once we have that done ...

       jd = jd + deltaT.etcorr(jd) / 86400.;   
       T = (jd - 2415020.) / 36525.;  // Julian centuries since 1900
       Tsq = T*T;   Tcb = T*Tsq;

       L = 279.69668 + 36000.76892*T + 0.0003025*Tsq;
       M = 358.47583 + 35999.04975*T - 0.000150*Tsq - 0.0000033*Tcb;
       e = 0.01675104 - 0.0000418*T - 0.000000126*Tsq;

       A = (153.23 + 22518.7541 * T) / Const.DEG_IN_RADIAN;  /* A, B due to Venus */
       B = (216.57 + 45037.5082 * T) / Const.DEG_IN_RADIAN;
       C = (312.69 + 32964.3577 * T) / Const.DEG_IN_RADIAN;  /* C due to Jupiter */
                /* D -- rough correction from earth-moon 
                        barycenter to center of earth. */
       D = (350.74 + 445267.1142*T - 0.00144*Tsq) / Const.DEG_IN_RADIAN;
       E = (231.19 + 20.20*T) / Const.DEG_IN_RADIAN;    
                       /* "inequality of long period .. */
       H = (353.40 + 65928.7155*T) / Const.DEG_IN_RADIAN;  /* Jupiter. */

       L = L + 0.00134 * Math.cos(A) 
             + 0.00154 * Math.cos(B)
	     + 0.00200 * Math.cos(C)
	     + 0.00179 * Math.sin(D)
	     + 0.00178 * Math.sin(E);

       Lrad = L/Const.DEG_IN_RADIAN;
       Mrad = M/Const.DEG_IN_RADIAN;
	
       Cent = (1.919460 - 0.004789*T -0.000014*Tsq)*Math.sin(Mrad)
	     + (0.020094 - 0.000100*T) * Math.sin(2.0*Mrad)
	     + 0.000293 * Math.sin(3.0*Mrad);
       sunlong = L + Cent;


       nu = M + Cent;
       nurad = nu / Const.DEG_IN_RADIAN;
	
       R = (1.0000002 * (1 - e*e)) / (1. + e * Math.cos(nurad));
       R = R + 0.00000543 * Math.sin(A)
	      + 0.00001575 * Math.sin(B)
	      + 0.00001627 * Math.sin(C)
	      + 0.00003076 * Math.cos(D)
	      + 0.00000927 * Math.sin(H);
/*      printf("solar longitude: %10.5f  Radius vector %10.7f\n",sunlong,R);
	printf("eccentricity %10.7f  eqn of center %10.5f\n",e,Cent);   */
	
       sunlong = sunlong/Const.DEG_IN_RADIAN;

       retvals[2] = R; // distance
       xecl = Math.cos(sunlong);  /* geocentric */
       yecl = Math.sin(sunlong);
       zecl = 0.;
       equatorial = Ecliptic.eclrot(jd, xecl, yecl, zecl);

       retvals[0] = Math.atan2(equatorial[1],equatorial[0]) * Const.HRS_IN_RADIAN;
       while(retvals[0] < 0.) retvals[0] = retvals[0] + 24.;
       retvals[1] = Math.asin(equatorial[2]) * Const.DEG_IN_RADIAN;

       retvals[3] = equatorial[0] * R;  // xyz
       retvals[4] = equatorial[1] * R;
       retvals[5] = equatorial[2] * R;
//       System.out.printf("computesun XYZ %f %f %f  %f\n",
//          retvals[3],retvals[4],retvals[5],jd);
  
       return(retvals);
    }

    void sunvel(double jd) {
       /* numerically differentiates sun xyz to get velocity. */
       double dt = 0.05; // days ... gives about 8 digits ... 
       int i;
       
       double [] pos1 = computesun(jd - dt/2.);
       double [] pos2 = computesun(jd + dt/2.);
       for(i = 0; i < 3; i++) {
           xyzvel[i] = (pos2[i+3] - pos1[i+3]) / dt;  // AU/d, eq. of date.
       }
    }
}

class Moon implements Cloneable {
   Celest geopos;
   Celest topopos;

   Moon(double jd) {
      double [] retvals;
      double eq;
      retvals = computemoon(jd);
      eq = 2000. + (jd - Const.J2000) / 365.25;
      geopos = new Celest(retvals[0],retvals[1],eq,retvals[2]);
      topopos = new Celest(0.,0.,eq);  // not set, no geogr info
   }

   Moon(WhenWhere w) {
//      double [] retvals;
//      retvals = computemoon(w.when.jd);
//      geopos = new Celest(retvals[0],retvals[1],w.when.JulianEpoch(),retvals[2]);
//      topopos = Topo.topocorr(geopos, w);
        update(w.when,w.where,w.sidereal);
   }

   void update(InstantInTime when, Site where, double sidereal) {
      double [] retvals;

      retvals = computemoon(when.jd);
      geopos = new Celest(retvals[0],retvals[1],when.JulianEpoch(),retvals[2]);
      topopos = Topo.topocorr(geopos, when, where, sidereal);
   }   
    
   public Moon clone() {
      try {
          Moon copy = (Moon) super.clone();
          copy.geopos = (Celest) geopos.clone();
          copy.topopos = (Celest) topopos.clone();
          return copy;
      } catch (CloneNotSupportedException e) {
          throw new Error("This should never happen!");
      }
   }
 
   static double [] computemoon(double jd) {
 /* Rather accurate lunar 
   ephemeris, from Jean Meeus' *Astronomical Formulae For Calculators*,
   pub. Willman-Bell.  Includes all the terms given there. */
      double Lpr,M,Mpr,D,F,Om,T,Tsq,Tcb;
      double e,lambda,B,beta,om1,om2,dist;
      double sinx, x, y, z, l, m, n, pie;
      double [] retvals = {0.,0.,0.,0.,0.,0.};  //ra,dec,distance,x,y,z.
      double [] equatorial = {0.,0.,0.};
   
      jd = jd + deltaT.etcorr(jd)/86400.;   
          /* approximate correction to ephemeris time */
      T = (jd - 2415020.) / 36525.;   /* this based around 1900 ... */
      Tsq = T * T;
      Tcb = Tsq * T;
   
      Lpr = 270.434164 + 481267.8831 * T - 0.001133 * Tsq 
      		+ 0.0000019 * Tcb;
      M = 358.475833 + 35999.0498*T - 0.000150*Tsq
      		- 0.0000033*Tcb;
      Mpr = 296.104608 + 477198.8491*T + 0.009192*Tsq 
      		+ 0.0000144*Tcb;
      D = 350.737486 + 445267.1142*T - 0.001436 * Tsq
      		+ 0.0000019*Tcb;
      F = 11.250889 + 483202.0251*T -0.003211 * Tsq 
      		- 0.0000003*Tcb;
      Om = 259.183275 - 1934.1420*T + 0.002078*Tsq 
      		+ 0.0000022*Tcb;

      Lpr = Lpr % 360.;  M = M % 360.;  Mpr = Mpr % 360.;
      D = D % 360.;  F = F % 360.;  Om = Om % 360.;
     
      sinx =  Math.sin((51.2 + 20.2 * T)/Const.DEG_IN_RADIAN);
      Lpr = Lpr + 0.000233 * sinx;
      M = M - 0.001778 * sinx;
      Mpr = Mpr + 0.000817 * sinx;
      D = D + 0.002011 * sinx;
      
      sinx = 0.003964 * Math.sin((346.560+132.870*T -0.0091731*Tsq)/Const.DEG_IN_RADIAN);
   
      Lpr = Lpr + sinx;
      Mpr = Mpr + sinx;
      D = D + sinx;
      F = F + sinx;
   
   
      sinx = Math.sin(Om/Const.DEG_IN_RADIAN);
      Lpr = Lpr + 0.001964 * sinx;
      Mpr = Mpr + 0.002541 * sinx;
      D = D + 0.001964 * sinx;
      F = F - 0.024691 * sinx;
      F = F - 0.004328 * Math.sin((Om + 275.05 -2.30*T)/Const.DEG_IN_RADIAN);
   
      e = 1 - 0.002495 * T - 0.00000752 * Tsq;
   
      M = M / Const.DEG_IN_RADIAN;   /* these will all be arguments ... */
      Mpr = Mpr / Const.DEG_IN_RADIAN;
      D = D / Const.DEG_IN_RADIAN;
      F = F / Const.DEG_IN_RADIAN;
   
      lambda = Lpr + 6.288750 * Math.sin(Mpr)
      	+ 1.274018 * Math.sin(2*D - Mpr)
      	+ 0.658309 * Math.sin(2*D)
      	+ 0.213616 * Math.sin(2*Mpr)
      	- e * 0.185596 * Math.sin(M) 
      	- 0.114336 * Math.sin(2*F)
      	+ 0.058793 * Math.sin(2*D - 2*Mpr)
      	+ e * 0.057212 * Math.sin(2*D - M - Mpr)
      	+ 0.053320 * Math.sin(2*D + Mpr)
      	+ e * 0.045874 * Math.sin(2*D - M)
      	+ e * 0.041024 * Math.sin(Mpr - M)
      	- 0.034718 * Math.sin(D)
      	- e * 0.030465 * Math.sin(M+Mpr)
      	+ 0.015326 * Math.sin(2*D - 2*F)
      	- 0.012528 * Math.sin(2*F + Mpr)
      	- 0.010980 * Math.sin(2*F - Mpr)
      	+ 0.010674 * Math.sin(4*D - Mpr)
      	+ 0.010034 * Math.sin(3*Mpr)
      	+ 0.008548 * Math.sin(4*D - 2*Mpr)
      	- e * 0.007910 * Math.sin(M - Mpr + 2*D)
      	- e * 0.006783 * Math.sin(2*D + M)
      	+ 0.005162 * Math.sin(Mpr - D);
   
      	/* And furthermore.....*/
   
      lambda = lambda + e * 0.005000 * Math.sin(M + D)
      	+ e * 0.004049 * Math.sin(Mpr - M + 2*D)
      	+ 0.003996 * Math.sin(2*Mpr + 2*D)
      	+ 0.003862 * Math.sin(4*D)
      	+ 0.003665 * Math.sin(2*D - 3*Mpr)
      	+ e * 0.002695 * Math.sin(2*Mpr - M)
      	+ 0.002602 * Math.sin(Mpr - 2*F - 2*D)
      	+ e * 0.002396 * Math.sin(2*D - M - 2*Mpr)
      	- 0.002349 * Math.sin(Mpr + D)
      	+ e * e * 0.002249 * Math.sin(2*D - 2*M)
      	- e * 0.002125 * Math.sin(2*Mpr + M)
      	- e * e * 0.002079 * Math.sin(2*M)
      	+ e * e * 0.002059 * Math.sin(2*D - Mpr - 2*M)
      	- 0.001773 * Math.sin(Mpr + 2*D - 2*F)
      	- 0.001595 * Math.sin(2*F + 2*D)
      	+ e * 0.001220 * Math.sin(4*D - M - Mpr)
      	- 0.001110 * Math.sin(2*Mpr + 2*F)
      	+ 0.000892 * Math.sin(Mpr - 3*D)
      	- e * 0.000811 * Math.sin(M + Mpr + 2*D)
      	+ e * 0.000761 * Math.sin(4*D - M - 2*Mpr)
      	+ e * e * 0.000717 * Math.sin(Mpr - 2*M)
      	+ e * e * 0.000704 * Math.sin(Mpr - 2 * M - 2*D)
      	+ e * 0.000693 * Math.sin(M - 2*Mpr + 2*D)
      	+ e * 0.000598 * Math.sin(2*D - M - 2*F)
      	+ 0.000550 * Math.sin(Mpr + 4*D)
      	+ 0.000538 * Math.sin(4*Mpr)
      	+ e * 0.000521 * Math.sin(4*D - M)
      	+ 0.000486 * Math.sin(2*Mpr - D);
      
   /*              *eclongit = lambda;  */
   
      B = 5.128189 * Math.sin(F)
      	+ 0.280606 * Math.sin(Mpr + F)
      	+ 0.277693 * Math.sin(Mpr - F)
      	+ 0.173238 * Math.sin(2*D - F)
      	+ 0.055413 * Math.sin(2*D + F - Mpr)
      	+ 0.046272 * Math.sin(2*D - F - Mpr)
      	+ 0.032573 * Math.sin(2*D + F)
      	+ 0.017198 * Math.sin(2*Mpr + F)
      	+ 0.009267 * Math.sin(2*D + Mpr - F)
      	+ 0.008823 * Math.sin(2*Mpr - F)
      	+ e * 0.008247 * Math.sin(2*D - M - F) 
      	+ 0.004323 * Math.sin(2*D - F - 2*Mpr)
      	+ 0.004200 * Math.sin(2*D + F + Mpr)
      	+ e * 0.003372 * Math.sin(F - M - 2*D)
      	+ 0.002472 * Math.sin(2*D + F - M - Mpr)
      	+ e * 0.002222 * Math.sin(2*D + F - M)
      	+ e * 0.002072 * Math.sin(2*D - F - M - Mpr)
      	+ e * 0.001877 * Math.sin(F - M + Mpr)
      	+ 0.001828 * Math.sin(4*D - F - Mpr)
      	- e * 0.001803 * Math.sin(F + M)
      	- 0.001750 * Math.sin(3*F)
      	+ e * 0.001570 * Math.sin(Mpr - M - F)
      	- 0.001487 * Math.sin(F + D)
      	- e * 0.001481 * Math.sin(F + M + Mpr)
      	+ e * 0.001417 * Math.sin(F - M - Mpr)
      	+ e * 0.001350 * Math.sin(F - M)
      	+ 0.001330 * Math.sin(F - D)
      	+ 0.001106 * Math.sin(F + 3*Mpr)
      	+ 0.001020 * Math.sin(4*D - F)
      	+ 0.000833 * Math.sin(F + 4*D - Mpr);
        /* not only that, but */
      B = B + 0.000781 * Math.sin(Mpr - 3*F)
      	+ 0.000670 * Math.sin(F + 4*D - 2*Mpr)
      	+ 0.000606 * Math.sin(2*D - 3*F)
      	+ 0.000597 * Math.sin(2*D + 2*Mpr - F)
      	+ e * 0.000492 * Math.sin(2*D + Mpr - M - F)
      	+ 0.000450 * Math.sin(2*Mpr - F - 2*D)
      	+ 0.000439 * Math.sin(3*Mpr - F)
      	+ 0.000423 * Math.sin(F + 2*D + 2*Mpr)
      	+ 0.000422 * Math.sin(2*D - F - 3*Mpr)
      	- e * 0.000367 * Math.sin(M + F + 2*D - Mpr)
      	- e * 0.000353 * Math.sin(M + F + 2*D)
      	+ 0.000331 * Math.sin(F + 4*D)
      	+ e * 0.000317 * Math.sin(2*D + F - M + Mpr)
      	+ e * e * 0.000306 * Math.sin(2*D - 2*M - F)
      	- 0.000283 * Math.sin(Mpr + 3*F);
      
      
      om1 = 0.0004664 * Math.cos(Om/Const.DEG_IN_RADIAN);        
      om2 = 0.0000754 * Math.cos((Om + 275.05 - 2.30*T)/Const.DEG_IN_RADIAN);
      
      beta = B * (1. - om1 - om2);
   /*      *eclatit = beta; */
      
      pie = 0.950724 
      	+ 0.051818 * Math.cos(Mpr)
      	+ 0.009531 * Math.cos(2*D - Mpr)
      	+ 0.007843 * Math.cos(2*D)
      	+ 0.002824 * Math.cos(2*Mpr)
      	+ 0.000857 * Math.cos(2*D + Mpr)
      	+ e * 0.000533 * Math.cos(2*D - M)
      	+ e * 0.000401 * Math.cos(2*D - M - Mpr)
      	+ e * 0.000320 * Math.cos(Mpr - M)
      	- 0.000271 * Math.cos(D)
      	- e * 0.000264 * Math.cos(M + Mpr)
      	- 0.000198 * Math.cos(2*F - Mpr)
      	+ 0.000173 * Math.cos(3*Mpr)
      	+ 0.000167 * Math.cos(4*D - Mpr)
      	- e * 0.000111 * Math.cos(M)
      	+ 0.000103 * Math.cos(4*D - 2*Mpr)
      	- 0.000084 * Math.cos(2*Mpr - 2*D)
      	- e * 0.000083 * Math.cos(2*D + M)
      	+ 0.000079 * Math.cos(2*D + 2*Mpr)
      	+ 0.000072 * Math.cos(4*D)
      	+ e * 0.000064 * Math.cos(2*D - M + Mpr)
      	- e * 0.000063 * Math.cos(2*D + M - Mpr)
      	+ e * 0.000041 * Math.cos(M + D)
      	+ e * 0.000035 * Math.cos(2*Mpr - M)
      	- 0.000033 * Math.cos(3*Mpr - 2*D)
      	- 0.000030 * Math.cos(Mpr + D)
      	- 0.000029 * Math.cos(2*F - 2*D)
      	- e * 0.000029 * Math.cos(2*Mpr + M)
      	+ e * e * 0.000026 * Math.cos(2*D - 2*M)
      	- 0.000023 * Math.cos(2*F - 2*D + Mpr)
      	+ e * 0.000019 * Math.cos(4*D - M - Mpr);
   
      // System.out.printf("beta lambda %f %f",beta,lambda);
      beta = beta/Const.DEG_IN_RADIAN;
      lambda = lambda/Const.DEG_IN_RADIAN;
      dist = 1./Math.sin((pie)/Const.DEG_IN_RADIAN);
//      System.out.printf("dist %f\n",dist);

      retvals[2] = dist / Const.EARTHRAD_IN_AU;

      l = Math.cos(lambda) * Math.cos(beta);    
      m = Math.sin(lambda) * Math.cos(beta);
      n = Math.sin(beta);

      equatorial = Ecliptic.eclrot(jd,l,m,n);
      retvals[3] = equatorial[0] * dist;
      retvals[4] = equatorial[1] * dist;
      retvals[5] = equatorial[2] * dist;
   
      retvals[0] = Math.atan2(equatorial[1],equatorial[0]) 
                 * Const.HRS_IN_RADIAN;
      retvals[1] = Math.asin(equatorial[2]) * Const.DEG_IN_RADIAN;        

      return retvals;

   }  

   public static double flmoon(int n, int nph) {
    /* Gives JD (+- 2 min) of phase nph during a given lunation n.
       Implements formulae in Meeus' Astronomical Formulae for Calculators,
       2nd Edn, publ. by Willman-Bell. */
    /* nph = 0 for new, 1 first qtr, 2 full, 3 last quarter. */

       double jd, cor;
	double M, Mpr, F;
	double T;
	double lun;

	lun = (double) n + (double) nph / 4.;
	T = lun / 1236.85;
	jd = 2415020.75933 + 29.53058868 * lun  
		+ 0.0001178 * T * T 
		- 0.000000155 * T * T * T
		+ 0.00033 * Math.sin((166.56 + 132.87 * T - 0.009173 * T * T)/Const.DEG_IN_RADIAN);
	M = 359.2242 + 29.10535608 * lun - 0.0000333 * T * T - 0.00000347 * T * T * T;
	M = M / Const.DEG_IN_RADIAN;
	Mpr = 306.0253 + 385.81691806 * lun + 0.0107306 * T * T + 0.00001236 * T * T * T;
	Mpr = Mpr / Const.DEG_IN_RADIAN;
	F = 21.2964 + 390.67050646 * lun - 0.0016528 * T * T - 0.00000239 * T * T * T;
	F = F / Const.DEG_IN_RADIAN;
	if((nph == 0) || (nph == 2)) {/* new or full */
		cor =   (0.1734 - 0.000393*T) * Math.sin(M)
			+ 0.0021 * Math.sin(2*M)
			- 0.4068 * Math.sin(Mpr)
			+ 0.0161 * Math.sin(2*Mpr)
			- 0.0004 * Math.sin(3*Mpr)
			+ 0.0104 * Math.sin(2*F)
			- 0.0051 * Math.sin(M + Mpr)
			- 0.0074 * Math.sin(M - Mpr)
			+ 0.0004 * Math.sin(2*F+M)
			- 0.0004 * Math.sin(2*F-M)
			- 0.0006 * Math.sin(2*F+Mpr)
			+ 0.0010 * Math.sin(2*F-Mpr)
			+ 0.0005 * Math.sin(M+2*Mpr);
		jd = jd + cor;
	}
	else {
		cor = (0.1721 - 0.0004*T) * Math.sin(M)
			+ 0.0021 * Math.sin(2 * M)
			- 0.6280 * Math.sin(Mpr)
			+ 0.0089 * Math.sin(2 * Mpr)
			- 0.0004 * Math.sin(3 * Mpr)
			+ 0.0079 * Math.sin(2*F)
			- 0.0119 * Math.sin(M + Mpr)
			- 0.0047 * Math.sin(M - Mpr)
			+ 0.0003 * Math.sin(2 * F + M)
			- 0.0004 * Math.sin(2 * F - M)
			- 0.0006 * Math.sin(2 * F + Mpr)
			+ 0.0021 * Math.sin(2 * F - Mpr)
			+ 0.0003 * Math.sin(M + 2 * Mpr)
			+ 0.0004 * Math.sin(M - 2 * Mpr)
			- 0.0003 * Math.sin(2*M + Mpr);
		if(nph == 1) cor = cor + 0.0028 - 
				0.0004 * Math.cos(M) + 0.0003 * Math.cos(Mpr);
		if(nph == 3) cor = cor - 0.0028 +
				0.0004 * Math.cos(M) - 0.0003 * Math.cos(Mpr);
		jd = jd + cor;

	}
	return jd;
   }
   
   public static int lunation(double jd) {
       int n; // approximate lunation ... 
       int nlast;
       double newjd, lastnewjd;
       int kount = 0;
       double x;

       nlast = (int) ((jd - 2415020.5) / 29.5307) - 1;
       
       lastnewjd = flmoon(nlast,0);
       nlast++;
       newjd = flmoon(nlast,0);
       // increment lunations until you're sure the last and next new
       // moons bracket your input value.
       while((newjd < jd) && (kount < 40)) {
           lastnewjd = newjd;
           nlast++; kount++; 
           newjd = flmoon(nlast,0);
       }
       if(kount > 35) System.out.printf("didn't find lunation!\n");
       return (nlast - 1);
   }

   public static String MoonPhaseDescr(double jd) {
       
       int n; 
       int nlast, noctiles;
       double newjd, lastnewjd;
       double fqjd, fljd, lqjd;
       int kount = 0;
       double x;

       nlast = lunation(jd);
       lastnewjd = flmoon(nlast,0);
       x = jd - lastnewjd;
       noctiles = (int) (x / 3.69134);  // 1/8 month, truncated.
       if(noctiles == 0) return
          String.format(Locale.ENGLISH, "%3.1f days since new moon.",x);
       else if (noctiles <= 2) {  // nearest first quarter ...
          fqjd = flmoon(nlast,1);
          x = jd - fqjd;
          if(x < 0.) 
              return String.format(Locale.ENGLISH, "%3.1f days before first quarter.",(-1.*x));
          else 
              return String.format(Locale.ENGLISH, "%3.1f days after first quarter.",x);
       }
       else if (noctiles <= 4) {  // nearest full ...
          fljd = flmoon(nlast,2);
          x = jd - fljd;
          if(x < 0.) 
               return String.format(Locale.ENGLISH, "%3.1f days before full moon.",(-1.*x)); 
          else 
              return String.format(Locale.ENGLISH, "%3.1f days after full moon.",x);
       }
       else if (noctiles <= 6) {  // nearest last quarter ...
          lqjd = flmoon(nlast,3);
          x = jd - lqjd;
          if(x < 0.) 
               return String.format(Locale.ENGLISH, "%3.1f days before last quarter.",(-1.*x)); 
          else 
              return String.format(Locale.ENGLISH, "%3.1f days after last quarter.",x);
       }
       else {
          newjd = flmoon(nlast + 1,0);
          x = jd - newjd;
          return String.format(Locale.ENGLISH, "%3.1f days before new moon.",(-1.*x));
       }
   }
}

class WhenWhere implements Cloneable {
   InstantInTime when;
   Site where;
   double sidereal;
   RA siderealobj;   // for output 
   double [] barycenter = {0.,0.,0.,0.,0.,0.};  
     // Barycentric coords in epoch of date; 0-2 are XYZ, 3-5 are velocity.

   Sun sun;
   HA hasun;
   double altsun, azsun;
   double twilight;
   
   Moon moon;    // mostly for use in rise-set calculations.
   HA hamoon;
   double altmoon, azmoon;

   double sunmoon;
   double moonillum; 
   double cusppa;

   void dump() {
      System.out.printf("jd %f  Local %d %d %d  %d %d  UT %d %d %d  %d %d\n",
         when.jd,when.localDate.year,when.localDate.month,when.localDate.day,
         when.localDate.timeofday.hour,when.localDate.timeofday.minute,
         when.UTDate.year,when.UTDate.month,when.UTDate.day,when.UTDate.timeofday.hour,
         when.UTDate.timeofday.minute);
      System.out.printf("lst %f\n",sidereal);
   }

   WhenWhere(InstantInTime t, Site loc) {
      when = t;
      where = loc;
      sidereal = lstcalc(when.jd,where.longit.value);
      siderealobj = new RA(sidereal);
      MakeLocalSun();
      MakeLocalMoon();   // these always need instantiation to avoid trouble later.

   }

   WhenWhere(double jdin, Site loc) {
      when = new InstantInTime(jdin,loc.stdz,loc.use_dst,true);
      where = loc;
      sidereal = lstcalc(when.jd,where.longit.value);
      siderealobj = new RA(sidereal);
      MakeLocalSun();
      MakeLocalMoon();   // these always need instantiation to avoid trouble later.
   }

   void ChangeWhen(double jdin) {
      when.SetInstant(jdin, where.stdz, where.use_dst, true);
      sidereal = lstcalc(when.jd,where.longit.value);
      siderealobj.setRA(sidereal);
   }

   void ChangeWhen(String s, boolean is_ut) {
      when.SetInstant(s, where.stdz, where.use_dst,is_ut);
      sidereal = lstcalc(when.jd,where.longit.value);
      siderealobj.setRA(sidereal);
   }

   void SetToNow() {
      when.SetInstant(where.stdz,where.use_dst);
      sidereal = lstcalc(when.jd,where.longit.value);
      siderealobj.setRA(sidereal);
   }

   void ChangeSite(HashMap<String,Site> hash, String s) {
      // changing site involves synching sidereal and local time 
      // so test to see if these are needed. 
      Site ss = hash.get(s);
      if(where.equals(ss) == false) {  // there's an equals method for this now ... 
         // System.out.printf("Changing site ... .\n");
         where = ss;
         // System.out.printf("Site changed, stdz = %f\n",where.stdz);
         sidereal = lstcalc(when.jd,where.longit.value);
         when.SetInstant(when.jd, where.stdz, where.use_dst, true);
         siderealobj.setRA(sidereal);
      }
      // otherwise do nothing.
      // else System.out.printf("Not changing site ... \n");
   }

   void AdvanceWhen(String s) {
      when.AdvanceTime(s, where.stdz, where.use_dst);
      sidereal = lstcalc(when.jd,where.longit.value);
      siderealobj.setRA(sidereal);
   }

   void AdvanceWhen(String s, boolean forward) {
      when.AdvanceTime(s, where.stdz, where.use_dst, forward);
      sidereal = lstcalc(when.jd,where.longit.value);
      siderealobj.setRA(sidereal);
   }

   Celest zenith2000() {
      Celest c = new Celest(sidereal,where.lat.value,when.JulianEpoch());
      c.selfprecess(2000.);
      return c;
   }

   public WhenWhere clone() {  // override clone method to make it public
      try {
         WhenWhere copy = (WhenWhere) super.clone();   // this needs to be try/catch to make it work
         copy.when = (InstantInTime) when.clone();
         copy.where = (Site) where.clone();   
         copy.sidereal = sidereal;
         copy.siderealobj = (RA) siderealobj.clone();   
         copy.barycenter = (double []) barycenter;
         copy.sun = (Sun) sun.clone();   
         copy.hasun = (HA) hasun.clone();
         copy.altsun = altsun;
         copy.azsun = azsun;
         copy.twilight = twilight;
         copy.moon = (Moon) moon.clone();
         copy.hamoon = (HA) hamoon.clone();
         copy.altmoon = altmoon;
         copy.azmoon = azmoon;
         copy.sunmoon = sunmoon;
         copy.moonillum = moonillum;
         copy.cusppa = cusppa;
         return copy;
      } catch (CloneNotSupportedException e) {
        throw new Error("This should never happen!\n");
      }
   }

   static double lstcalc(double jdin, double longitin) {
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
      sid_g = sid_g + 1.0027379093 * ut - longitin / 24.;
      sid_int = (long) sid_g;
      sid_g = (sid_g - (double) sid_int) * 24.; 
      if(sid_g < 0.) sid_g = sid_g + 24.;
      return sid_g;
   }

   // Pass in a previously-computed planets and sun for this ...   
   // Planets and sun are assumed up-to-date.

   void baryxyzvel(Planets p, Sun s) {

       int i;
       double [] geopos; 

       // need sunvel now so get it ... 

 //      System.out.printf("into baryxyzvel, jd = %f\n",when.jd);
       s.sunvel(when.jd);  // compute sun velocity  ... 

//       System.out.printf("Helio Vxyz sun: %f %f %f  %f\n",
 //         s.xyzvel[0] * Const.KMS_AUDAY,
  //        s.xyzvel[1] * Const.KMS_AUDAY,
   //       s.xyzvel[2] * Const.KMS_AUDAY, when.jd);

       p.ComputeBaryCor(); // compute offset of barycenter from heliocenter

//       System.out.printf("sun xyz  %f %f %f\n",s.xyz[0],s.xyz[1],s.xyz[2]);
//       System.out.printf(" baryc   %f %f %f\n",p.barycor[0],p.barycor[1],p.barycor[2]);
       for(i = 0; i < 3; i++) {
          barycenter[i] = (-1. * s.xyz[i] - p.barycor[i]) * 
                     Const.LIGHTSEC_IN_AU;
          barycenter[i+3] = (-1. * s.xyzvel[i] - p.barycor[i+3]) * 
                      Const.KMS_AUDAY;
//          System.out.printf("pre-topo: %d   %f   %f \n",i,
//              barycenter[i] / Const.LIGHTSEC_IN_AU,
//                                                barycenter[i+3]);
       }    

       // add in the topocentric velocity ... note use of sidereal for longit

       geopos = Topo.Geocent(sidereal, where.lat.value, where.elevsea);

//       System.out.printf("Geopos: %f %f %f\n",geopos[0],geopos[1],geopos[2]);
//       System.out.printf("topo corrn vx vy %f %f\n",
//          Const.OMEGA_EARTH * geopos[1] * Const.EARTHRAD_IN_KM,
//          Const.OMEGA_EARTH * geopos[0] * Const.EARTHRAD_IN_KM);

       // rotation vel is vector omega crossed into posn vector

       barycenter[3] -= Const.OMEGA_EARTH * geopos[1] * Const.EARTHRAD_IN_KM ;
       barycenter[4] += Const.OMEGA_EARTH * geopos[0] * Const.EARTHRAD_IN_KM ;

   }
   
   void MakeLocalSun() {
       //System.out.printf("Making a new sun, jd = %f\n",when.jd);
       sun = new Sun(this);
       //System.out.printf("Made sun, sidereal = %f\n",sidereal);
       hasun = new HA(sidereal - sun.topopos.Alpha.value);
       double [] altazpar = Observation.altit(sun.topopos.Delta.value, hasun.value, 
              where.lat.value);
       altsun = altazpar[0];
       azsun = altazpar[1];
       twilight = SkyIllum.ztwilight(altsun);
       //System.out.printf("Made a new sun: %s alt %f\n",sun.topopos.checkstring(),altazpar[0]);
   }

   void UpdateLocalSun() {
       sun.update(when,where,sidereal);
       hasun = new HA(sidereal - sun.topopos.Alpha.value);
       double [] altazpar = Observation.altit(sun.topopos.Delta.value, hasun.value, 
              where.lat.value);
       altsun = altazpar[0];
       azsun = altazpar[1];
       twilight = SkyIllum.ztwilight(altsun);
//       System.out.printf("Updated sun: %s %f\n",sun.topopos.checkstring(),altazpar[0]);
   }

   void MakeLocalMoon() {
      moon = new Moon(this);
      hamoon = new HA(sidereal - moon.topopos.Alpha.value);
      double [] altazpar = Observation.altit(moon.topopos.Delta.value, hamoon.value, 
              where.lat.value);
      altmoon = altazpar[0];
      azmoon = altazpar[1]; 
//      sunmoon = Spherical.subtend(sun.topopos,moon.topopos);
//      moonillum = 0.5 * (1. - Math.cos(sunmoon));
//      sunmoon *= Const.DEG_IN_RADIAN;
//      System.out.printf("Made a new moon: %s HA %s alt %f\n",moon.topopos.checkstring(),
//          hamoon.RoundedHAString(0,":"),altazpar[0]);
   }

   void UpdateLocalMoon() {
      moon.update(when,where,sidereal);
      hamoon = new HA(sidereal - moon.topopos.Alpha.value);
      double [] altazpar = Observation.altit(moon.topopos.Delta.value, hamoon.value, 
              where.lat.value);
      altmoon = altazpar[0];
      azmoon = altazpar[1];
      // call this after updating the sun ... 
//      sunmoon = Spherical.subtend(sun.topopos,moon.topopos);
//      moonillum = 0.5 * (1. - Math.cos(sunmoon));
//      sunmoon *= Const.DEG_IN_RADIAN;

//      System.out.printf("Updated the moon: %s HA %s alt %f\n",moon.topopos.checkstring(),
//          hamoon.RoundedHAString(0,":"),altazpar[0]);
   }

   void ComputeSunMoon() {
      double retvals[];

      UpdateLocalSun();
      UpdateLocalMoon();
      retvals = Spherical.CuspPA(sun.topopos,moon.topopos);
      sunmoon = retvals[1]; 
      moonillum = 0.5 * (1. - Math.cos(sunmoon));
      sunmoon *= Const.DEG_IN_RADIAN;
      cusppa = retvals[0];  // radians ... 
   }
}

class Observation implements Cloneable {

   WhenWhere w;
   Celest c;
   Celest current;
   HA ha;
   double altitude, azimuth, parallactic;
   double airmass;
   double barytcor, baryvcor;   // time and velocity corrections to barycenter 
   double baryjd;

   // I'd been hauling around a separate sun and moon, but these are now 
   // expunged ... they're in the WhenWhere.  But moonlight, moonobj, and sunobj
   // depend on the object coordinates so they are here:
   
   double moonlight;          
   double moonobj, sunobj;    // angular sepn of moon from obj and sun from obj

   Observation(WhenWhere wIn, Celest celIn) { 
      w = (WhenWhere) wIn.clone();
      c = (Celest) celIn.clone();
      ha = new HA(0.); 
      ComputeSky();
   }

   void ComputeSky() {   // bare-bones updater
      // assumes WhenWhere w has been updated. 
      current = c.precessed(w.when.JulianEpoch());
      ha.setHA(w.sidereal - current.Alpha.value);
      double [] altazpar = altit(current.Delta.value, ha.value, 
              w.where.lat.value);
      altitude = altazpar[0];
      azimuth = altazpar[1];
      parallactic = altazpar[2];
      airmass = Spherical.true_airmass(altitude);
   }

   // Split off the sun, moon, barycenter etc. to save time -- they're 
   // not always needed in every instance.

   public Observation clone() {  // override clone method to make it public
      try {
         Observation copy = (Observation) super.clone();   // this needs to be try/catch to make it work
         copy.w = (WhenWhere) w.clone();
         copy.c = (Celest) c.clone();
         copy.current = (Celest) current.clone();
         copy.ha = (HA) ha.clone();
         copy.altitude = altitude;
         copy.azimuth = azimuth;
         copy.parallactic = parallactic;
         copy.barytcor = barytcor;
         copy.baryvcor = baryvcor;
         copy.baryjd = baryjd;
         copy.moonlight = moonlight;
         copy.moonobj = moonobj;
         copy.sunobj = sunobj;
         return copy;
      } catch (CloneNotSupportedException e) {
        throw new Error("This should never happen!\n");
      }
   }

   void ComputeSunMoon() {

      
      current = c.precessed(w.when.JulianEpoch());
      //System.out.printf("ComputeSunMoon %s (%f)\n",
      //       current.Alpha.RoundedRAString(2," "),current.Equinox);
      w.ComputeSunMoon();   // the non-object related parts are all done here

      sunobj = Const.DEG_IN_RADIAN * Spherical.subtend(w.sun.topopos,current);
      moonobj = Const.DEG_IN_RADIAN * Spherical.subtend(w.moon.topopos,current);

      moonlight = SkyIllum.lunskybright(w.sunmoon,moonobj,0.172,w.altmoon,
                altitude,w.moon.topopos.distance);

   }

   static double [] altit(double dec, double hrangle, double lat) {
    // returns altitiude (degr), azimuth (degrees), parallactic angle (degr)
      double x,y,z;
      double sinp, cosp;   // sine and cosine of parallactic angle
      double cosdec, sindec, cosha, sinha, coslat, sinlat;
      double retvals [] = {0.,0.,0.};
      double az, parang;
      
      dec = dec / Const.DEG_IN_RADIAN;
      hrangle = hrangle / Const.HRS_IN_RADIAN;
      lat = lat / Const.DEG_IN_RADIAN;
      cosdec = Math.cos(dec); sindec = Math.sin(dec);
      cosha = Math.cos(hrangle); sinha = Math.sin(hrangle);
      coslat = Math.cos(lat); sinlat = Math.sin(lat);
      x = Const.DEG_IN_RADIAN * Math.asin(cosdec*cosha*coslat + sindec*sinlat);
            // x is the altitude.
      y =  sindec*coslat - cosdec*cosha*sinlat; /* due N comp. */
      z =  -1. * cosdec*sinha; /* due east comp. */
      az = Math.atan2(z,y);   
    
      if(cosdec != 0.) { // protect from divide by zero
         sinp = -1. * Math.sin(az) * coslat / cosdec;
              /* spherical law of sines ... cosdec = sine of codec,
                    coslat = sine of colatitude */
         cosp = -1. * Math.cos(az) * cosha - Math.sin(az) * sinha * sinlat;
              /* spherical law of cosines ... also transformed to local
                    available variables. */
         parang = Math.atan2(sinp, cosp) * Const.DEG_IN_RADIAN;
              /* library function gets the quadrant. */
      }
      else { // you're on the pole ...
         if(lat >= 0.) parang = 180.;
         else parang = 0.;
      }
     
      az *= Const.DEG_IN_RADIAN;
      while(az < 0.) az += 360.;
      while(az >= 360.) az -= 360.;

      retvals[0] = x;
      retvals[1] = az;
      retvals[2] = parang;

      return retvals;
   }
//   
//   void computesun() {
//      sun = new Sun(w);
//   }
//
//   void computemoon() {
//      moon = new Moon(w);
//   }
//
   void computebary(Planets p) {

      w.baryxyzvel(p, w.sun);  /* find the position and velocity of the 
                                 observing site wrt the solar system barycent */
      double [] unitvec = current.cel_unitXYZ();
//      System.out.printf("Current: %s\n",current.checkstring());
      barytcor = 0.; baryvcor = 0.;
//      System.out.printf("Bary xyz %f %f %f \n",w.barycenter[0],
//             w.barycenter[1],w.barycenter[2]);
      for(int i = 0; i < 3; i++) {
         barytcor += unitvec[i] * w.barycenter[i];
         baryvcor += unitvec[i] * w.barycenter[i+3];
      }
//      System.out.printf("Bary %f sec %f km/s ...\n",
//          barytcor, baryvcor);
      baryjd = w.when.jd + barytcor / 86400.;
   }
}

class Planets  {
/** This includes everything for the planets. */
/* The planetary ephemeris used is based on Meeus' Astronomical 
   Forumulae for Calculators.  This gives fairly good (< 1 arcmin)
   positions for the inner planets but somewhat worse for the outer
   planets.  These should NOT be used in applications where precise
   positions are needed.  This is intended as a very lightweight 
   application for purposes such as  (a) telling you which planet is 
   which in the sky; (b) plotting the planet recognizably among the 
   constellations; (c) warning you if your target is fairly close to 
   a bright planet; (d) generating a reasonably accurate value for the
   offset of the solar system barycenter from the center of the sun. */

   double jd_el;

   /* Arrays of planetary elements */
   static String [] names = {"Mercury","Venus","Earth","Mars","Jupiter","Saturn",
        "Uranus","Neptune","Pluto"}; // what the heck, let's keep Pluto
   double [] incl = {0.,0.,0.,0.,0.,0.,0.,0.,0.};  // inclination
   double [] Omega = {0.,0.,0.,0.,0.,0.,0.,0.,0.}; // longit of asc node
   double [] omega = {0.,0.,0.,0.,0.,0.,0.,0.,0.}; // longit of perihelion
   double [] a = {0.,0.,0.,0.,0.,0.,0.,0.,0.};     // semimajor axis
   double [] daily = {0.,0.,0.,0.,0.,0.,0.,0.,0.}; // mean daily motion, degr.
   double [] ecc = {0.,0.,0.,0.,0.,0.,0.,0.,0.};   // eccentricity
   double [] L_0 = {0.,0.,0.,0.,0.,0.,0.,0.,0.};   // starting longitude (?)  
   static double [] mass = {1.660137e-7, 2.447840e-6, 3.040433e-6,
          3.227149e-7,9.547907e-4,2.858776e-4,4.355401e-5,5.177591e-5,
          7.69e-9};  // IAU 1976 masses of planets in terms of solar mass
                     // used to weight barycenter calculation only.
// Need to include instance variables of x,y,z and xdot,ydot,zdot
   double [][] xyz;
   double [][] xyzvel;
   double [] barycor;  // 0,1,2 = xyz in AU; 3,4,5 = xyzvel in km/s. 
   double [] mags = {0.,0.,0.,0.,0.,0.,0.,0.,0.};   // apparent mags
   static final double [] V0 = {-0.42,-4.40,-3.86,-1.52,-9.40,-9.22,-7.19,
                                      -6.87,-1.0};

/*  From Astronomical Almanac, 2003, p. E88.  V mag of planet when
    full face on at unit distance from both sun and earth.  Saturn
    has been goosed up a bit b/c Almanac quantity didn't have rings
    in it ...   */

   Observation [] PlanetObs; 

   Planets(WhenWhere wIn) {
      comp_el(wIn.when.jd);
      PlanetObs = pposns(wIn);
   }

   void Update(WhenWhere wIn) {
      comp_el(wIn.when.jd);
      PlanetObs = pposns(wIn);
   }

   void comp_el(double jd_in) {
      /* Compute and load mean elements for the planets. */ 
      double T, Tsq, Tcb, d;
      double ups, P, Q, S, V, W, G, H, zeta, psi;  // Meeus p. 110 ff.  
      double sinQ, sinZeta, cosQ, cosZeta, sinV, cosV, sin2Zeta, cos2Zeta;
      double sin2Q, cos2Q, sinH, sin2H, cosH, cos2H;

      jd_el = jd_in; d = jd_el - 2415020.;    // 1900
      T = d / 36525.;
      Tsq = T * T;
      Tcb = Tsq * T;
 
      // Mercury, Venus, and Mars from Explanatory Suppl. p. 113

      // Mercury = 0
      incl[0] = 7.002880 + 1.8608e-3 * T - 1.83e-5 * Tsq;
      Omega[0] = 47.14594 + 1.185208 * T + 1.74e-4 * Tsq;
      omega[0] = 75.899697 + 1.55549 * T + 2.95e-4 * Tsq;
      a[0] = 0.3870986;
      daily[0] = 4.0923388;
      ecc[0] = 0.20561421 + 0.00002046 * T;
      L_0[0] = 178.179078 + 4.0923770233 * d  +
	 0.0000226 * Math.pow((3.6525 * T),2.);

      // Venus = 1
      incl[1] = 3.39363 + 1.00583e-03 * T - 9.722e-7 * Tsq;
      Omega[1] = 75.7796472 + 0.89985 * T + 4.1e-4 * Tsq;
      omega[1] = 130.16383 + 1.4080 * T + 9.764e-4 * Tsq;
      a[1] = 0.723325;
      daily[1] = 1.60213049;
      ecc[1] = 0.00682069 - 0.00004774 * T;
      L_0[1] = 342.767053 + 1.6021687039 * 36525 * T +
	 0.000023212 * Math.pow((3.6525 * T),2.);

      // Earth = 2  ... elements from old Nautical Almanac 
      ecc[2] = 0.01675104 - 0.00004180*T + 0.000000126*Tsq;
      incl[2] = 0.0;
      Omega[2] = 0.0;
      omega[2] = 101.22083 + 0.0000470684*d + 0.000453*Tsq + 0.000003*Tcb;
      a[2] = 1.0000007;;
      daily[2] = 0.985599;
      L_0[2] = 358.47583 + 0.9856002670*d - 0.000150*Tsq - 0.000003*Tcb +
	    omega[2];

      // Mars = 3 
      incl[3] = 1.85033 - 6.75e-04 * T - 1.833e-5 * Tsq;
      Omega[3] = 48.786442 + .770992 * T + 1.39e-6 * Tsq;
      omega[3] = 334.218203 + 1.840758 * T + 1.299e-4 * Tsq;
      a[3] = 1.5236915;
      daily[3] = 0.5240329502 + 1.285e-9 * T;
      ecc[3] = 0.09331290 - 0.000092064 * T - 0.000000077 * Tsq;
      L_0[3] = 293.747628 + 0.5240711638 * d  +
	 0.000023287 * Math.pow((3.6525 * T),2.);

      // Outer planets from Jean Meeus, Astronomical Formulae for 
      // Calculators, 3rd Edn, Willman-Bell; p. 100
      // Mutual interactions get pretty big; I'm including some of the 
      // larger perturbation terms from Meeus' book. 

      // Jupiter = 4
     
     incl[4] = 1.308736 - 0.0056961 * T + 0.0000039 * Tsq;
     Omega[4] = 99.443414 + 1.0105300 * T + 0.0003522 * Tsq 
  		- 0.00000851 * Tcb;
     omega[4] = 12.720972 + 1.6099617 * T + 1.05627e-3 * Tsq
  	- 3.43e-6 * Tcb;
     a[4] = 5.202561;
     daily[4] = 0.08312941782;
     ecc[4] = .04833475  + 1.64180e-4 * T - 4.676e-7*Tsq -
  	1.7e-9 * Tcb;
     L_0[4] = 238.049257 + 3036.301986 * T + 0.0003347 * Tsq -
  	1.65e-6 * Tcb;
  
     ups = 0.2*T + 0.1;
     P = (237.47555 + 3034.9061 * T) / Const.DEG_IN_RADIAN;
     Q = (265.91650 + 1222.1139 * T) / Const.DEG_IN_RADIAN;
     S = (243.51721 + 428.4677 * T) / Const.DEG_IN_RADIAN;
     V = 5*Q - 2*P;
     W = 2*P - 6*Q + 3*S;
     zeta = Q - P;
     psi = S - Q;
     sinQ = Math.sin(Q);  // compute some of the more popular ones ... 
     cosQ = Math.cos(Q);
     sin2Q = Math.sin(2.*Q);
     cos2Q = Math.cos(2.*Q);
     sinV = Math.sin(V);
     cosV = Math.cos(V);
     sinZeta = Math.sin(zeta);
     cosZeta = Math.cos(zeta);
     sin2Zeta = Math.sin(2*zeta);
     cos2Zeta = Math.cos(2*zeta);

     L_0[4] = L_0[4] 
	+ (0.331364 - 0.010281*ups - 0.004692*ups*ups)*sinV
	+ (0.003228 - 0.064436*ups + 0.002075*ups*ups)*cosV
	- (0.003083 + 0.000275*ups - 0.000489*ups*ups)*Math.sin(2*V)
	+ 0.002472 * Math.sin(W) + 0.013619 * sinZeta + 0.018472 * sin2Zeta 
	+ 0.006717 * Math.sin(3*zeta) 
	+ (0.007275  - 0.001253*ups) * sinZeta * sinQ
	+ 0.006417 * sin2Zeta * sinQ  
	- (0.033839 + 0.001253 * ups) * cosZeta * sinQ 
	- (0.035681 + 0.001208 * ups) * sinZeta * sinQ;
	/* only part of the terms, the ones first on the list and
	   selected larger-amplitude terms from farther down. */

     ecc[4] = ecc[4] + 1e-7 * (
	  (3606 + 130 * ups - 43 * ups*ups) * sinV 
	+ (1289 - 580 * ups) * cosV - 6764 * sinZeta * sinQ 
	- 1110 * sin2Zeta * sinQ 
	+ (1284 + 116 * ups) * cosZeta * sinQ 
	+ (1460 + 130 * ups) * sinZeta * cosQ 
	+ 6074 * cosZeta * cosQ);

     omega[4] = omega[4] 
	+ (0.007192 - 0.003147 * ups) * sinV
	+ ( 0.000197*ups*ups - 0.00675*ups - 0.020428) * cosV
	+ 0.034036 * cosZeta * sinQ + 0.037761 * sinZeta * cosQ;

     a[4] = a[4] + 1.0e-6 * (
	205 * cosZeta - 263 * cosV + 693 * cos2Zeta + 312 * Math.sin(3*zeta)
	+ 147 * Math.cos(4*zeta) + 299 * sinZeta * sinQ 
	+ 181 * cos2Zeta * sinQ + 181 * cos2Zeta * sinQ
	+ 204 * sin2Zeta * cosQ + 111 * Math.sin(3*zeta) * cosQ 
	- 337 * cosZeta * cosQ - 111 * cos2Zeta * cosQ
	);
   
     // Saturn = 5 
      incl[5] = 2.492519 - 0.00034550*T - 7.28e-7*Tsq;
      Omega[5] = 112.790414 + 0.8731951*T - 0.00015218*Tsq - 5.31e-6*Tcb ;
      omega[5] = 91.098214 + 1.9584158*T + 8.2636e-4*Tsq;
      a[5] = 9.554747;
      daily[5] = 0.0334978749897;
      ecc[5] = 0.05589232 - 3.4550e-4 * T - 7.28e-7*Tsq;
      L_0[5] = 266.564377 + 1223.509884*T + 0.0003245*Tsq - 5.8e-6*Tcb 
	+ (0.018150*ups - 0.814181 + 0.016714 * ups*ups) * sinV 
	+ (0.160906*ups - 0.010497 - 0.004100 * ups*ups) * cosV
	+ 0.007581 * Math.sin(2*V) - 0.007986 * Math.sin(W) 
	- 0.148811 * sinZeta - 0.040786*sin2Zeta 
	- 0.015208 * Math.sin(3*zeta) - 0.006339 * Math.sin(4*zeta) 
	- 0.006244 * sinQ
	+ (0.008931 + 0.002728 * ups) * sinZeta * sinQ 
	- 0.016500 * sin2Zeta * sinQ
	- 0.005775 * Math.sin(3*zeta) * sinQ 
	+ (0.081344 + 0.003206 * ups) * cosZeta * sinQ 
	+ 0.015019 * cos2Zeta * sinQ 
	+ (0.085581 + 0.002494 * ups) * sinZeta * cosQ
	+ (0.025328 - 0.003117 * ups) * cosZeta * cosQ
	+ 0.014394 * cos2Zeta * cosQ;   /* truncated here -- no
		      terms larger than 0.01 degrees, but errors may
		      accumulate beyond this.... */
      ecc[5] = ecc[5] + 1.0e-7 * (
	  (2458. * ups - 7927.) * sinV + (13381. + 1226. * ups) * cosV
	+ 12415. * sinQ + 26599. * cosZeta * sinQ 
	- 4687. * cos2Zeta * sinQ - 12696. * sinZeta * cosQ 
	- 4200. * sin2Zeta * cosQ +(2211. - 286*ups) * sinZeta*sin2Q
	- 2208. * sin2Zeta * sin2Q 
	- 2780. * cosZeta * sin2Q + 2022. * cos2Zeta*sin2Q 
	- 2842. * sinZeta * cos2Q - 1594. * cosZeta * cos2Q
	+ 2162. * cos2Zeta*cos2Q );  /* terms with amplitudes
	    > 2000e-7;  some secular variation ignored. */
      omega[5] = omega[5] 
	+ (0.077108 + 0.007186 * ups - 0.001533 * ups*ups) * sinV
	+ (0.045803 - 0.014766 * ups - 0.000536 * ups*ups) * cosV
	- 0.075825 * sinZeta * sinQ - 0.024839 * sin2Zeta*sinQ
	- 0.072582 * cosQ - 0.150383 * cosZeta * cosQ +
	0.026897 * cos2Zeta * cosQ;  /* all terms with amplitudes 
	    greater than 0.02 degrees -- lots of others! */
      a[5] = a[5] + 1.0e-6 * (
	2933. * cosV + 33629. * cosZeta - 3081. * cos2Zeta 
	- 1423. * Math.cos(3*zeta) + 1098. * sinQ - 2812. * sinZeta * sinQ 
	+ 2138. * cosZeta * sinQ  + 2206. * sinZeta * cosQ 
	- 1590. * sin2Zeta*cosQ + 2885. * cosZeta * cosQ 
	+ 2172. * cos2Zeta * cosQ);  /* terms with amplitudes greater
	   than 1000 x 1e-6 */

      // Uranus = 6
      incl[6] = 0.772464 + 0.0006253*T + 0.0000395*Tsq;
      Omega[6] = 73.477111 + 0.4986678*T + 0.0013117*Tsq;
      omega[6] = 171.548692 + 1.4844328*T + 2.37e-4*Tsq - 6.1e-7*Tcb;
      a[6] = 19.21814;
      daily[6] = 1.1769022484e-2;
      ecc[6] = 0.0463444 - 2.658e-5 * T;
      L_0[6] = 244.197470 + 429.863546*T + 0.000316*Tsq - 6e-7*Tcb;
      /* stick in a little bit of perturbation -- this one really gets
         yanked around.... after Meeus p. 116*/
      G = (83.76922 + 218.4901 * T)/Const.DEG_IN_RADIAN;
      H = 2*G - S;

      sinH = Math.sin(H); sin2H = Math.sin(2.*H);
      cosH = Math.cos(H); cos2H = Math.cos(2.*H);

      L_0[6] = L_0[6] + (0.864319 - 0.001583 * ups) * sinH
   	+ (0.082222 - 0.006833 * ups) * cosH
   	+ 0.036017 * sin2H;
      omega[6] = omega[6] + 0.120303 * sinH 
   	+ (0.019472 - 0.000947 * ups) * cosH
   	+ 0.006197 * sin2H;
      ecc[6] = ecc[6] + 1.0e-7 * (
   	20981. * cosH - 3349. * sinH + 1311. * cos2H);
      a[6] = a[6] - 0.003825 * cosH;
   
      /* other corrections to "true longitude" are ignored. */
   
      // Neptune = 7
      incl[7] = 1.779242 - 9.5436e-3 * T - 9.1e-6*Tsq;
      Omega[7] = 130.681389 + 1.0989350 * T + 2.4987e-4*Tsq - 4.718e-6*Tcb;
      omega[7] = 46.727364 + 1.4245744*T + 3.9082e-3*Tsq - 6.05e-7*Tcb;
      a[7] = 30.10957;
      daily[7] = 6.020148227e-3;
      ecc[7] = 0.00899704 + 6.33e-6 * T;
      L_0[7] = 84.457994 + 219.885914*T + 0.0003205*Tsq - 6e-7*Tcb;
      L_0[7] = L_0[7] 
	- (0.589833 - 0.001089 * ups) * sinH 
	- (0.056094 - 0.004658 * ups) * cosH
	- 0.024286 * sin2H;
      omega[7] = omega[7] + 0.024039 * sinH 
	- 0.025303 * cosH;
      ecc[7] = ecc[7] + 1.0e-7 * (
	4389. * sinH + 1129. * sin2H
	+ 4262. * cosH + 1089. * cos2H);
      a[7] = a[7] + 8.189e-3 * cosH; 
 
      // Pluto = 8; very approx elements, osculating for Sep 15 1992.
      d = jd_el - 2448880.5;  /* 1992 Sep 15 */       
      T = d / 36525.;
      incl[8] = 17.1426;
      Omega[8] = 110.180;
      omega[8] = 223.782;
      a[8] = 39.7465;
      daily[8] = 0.00393329;
      ecc[8] = 0.253834;
      L_0[8] = 228.1027 + 0.00393329 * d;
 
  }

  double [] planetxyz(int p, double jd) {
  /** produces ecliptic X,Y,Z coords for planet number 'p' at date jd. */
  
     double M, omnotil, nu, r;
     double e, LL, Om, om, nuu, ii;
     double [] retvals = {0.,0.,0.};

  // 1992 Astronomical Almanac p. E4 has these formulae.

     ii = incl[p] / Const.DEG_IN_RADIAN;
     e = ecc[p];

     LL = (daily[p] * (jd - jd_el) + L_0[p]) / Const.DEG_IN_RADIAN;
     Om = Omega[p] / Const.DEG_IN_RADIAN;
     om = omega[p] / Const.DEG_IN_RADIAN;
	
     M = LL - om;
     omnotil = om - Om;
     // approximate formula for Kepler equation solution ... 
     nu = M + (2.*e - 0.25 * Math.pow(e,3.)) * Math.sin(M) +
	     1.25 * e * e * Math.sin(2 * M) +
	     1.08333333 * Math.pow(e,3.) * Math.sin(3 * M);
     r = a[p] * (1. - e*e) / (1 + e * Math.cos(nu));

     retvals[0] = r * 
         (Math.cos(nu + omnotil) * Math.cos(Om) - Math.sin(nu +  omnotil) * 
               Math.cos(ii) * Math.sin(Om));
     retvals[1] = r * 
         (Math.cos(nu +  omnotil) * Math.sin(Om) + Math.sin(nu +  omnotil) * 
		Math.cos(ii) * Math.cos(Om));
     retvals[2] = r * Math.sin(nu +  omnotil) * Math.sin(ii);

     return retvals;

  }

  double [] planetvel(int p, double jd)  {

  /* numerically evaluates planet velocity by brute-force
  numerical differentiation. Very unsophisticated algorithm. */

	double dt; /* timestep */
	double x1,y1,z1,x2,y2,z2,r1,d1,r2,d2,ep1;
        double [] pos1 = {0.,0.,0.}; 
        double [] pos2 = {0.,0.,0.}; 
        double [] retval = {0.,0.,0.};
        int i;

	dt = 0.1 / daily[p]; /* time for mean motion of 0.1 degree */
	pos1 = planetxyz(p, (jd - dt));
	pos2 = planetxyz(p, (jd + dt));
        for (i = 0; i < 3; i ++) retval[i] = 0.5 * (pos2[i] - pos1[i]) / dt;
        return retval;
	/* answer should be in ecliptic coordinates, in AU per day.*/
   }

   double modulus (double [] a) {
      int i;
      double result = 0.;

      for(i = 0; i < a.length; i++)  result += a[i] * a[i];
      return Math.sqrt(result);

   }

   double dotprod (double [] a, double [] b) {
      int i;
      double result = 0.;
      for(i = 0; i < a.length; i++) result += a[i] * b[i];
      return result;
   }

   void computemags() {
        /* assumes xyz[][] has been updated.  All the calculations are relative,
           so it doesn't matter what the system is. */
         int i, j;
         double sun2planet, earth2planet, phasefac;
         double [] dxyz = {0., 0., 0.};

         for (i = 0; i < 9; i++) {
             if(i != 2) {  // skip earth
                 sun2planet = modulus(xyz[i]); 
                 for(j = 0; j < 3; j++)  {
                    dxyz[j] = xyz[i][j] - xyz[2][j];
                 }
                 earth2planet = modulus(dxyz);
                 phasefac = 0.5 * (dotprod(xyz[i] , dxyz) / (sun2planet * earth2planet) + 1.);
                 // this should be the illuminated fraction.

                 mags[i] = V0[i] + 
                   2.5 * Math.log10(phasefac) + 5. * Math.log10(sun2planet * earth2planet);
              }
              else {
                 mags[i] = -99.;   // earth
              }
         }

        // for(i = 0; i < 9; i++) System.out.printf("%s  %f\n",names[i],mags[i]);
   }
   

   Celest earthview(double [] earthxyz, double [] planxyz, double jd) {
        double [] dxyz = {0.,0.,0.};
        double [] retvals = {0.,0.,0.};
        double eq;
        int i;
        
        eq = 2000. + (jd - Const.J2000) / 365.25;
        for(i = 0; i < 3; i++) {
           dxyz[i] = planxyz[i] - earthxyz[i];
        }
        retvals = Celest.XYZcel(dxyz[0],dxyz[1],dxyz[2]);
        Celest ViewFromEarth = new Celest(retvals[0],retvals[1],eq,retvals[2]);
        return ViewFromEarth;
   }
   
   public Observation [] pposns(WhenWhere w) {
        /* returns Observations for all the planets. */
        
        int i;
        double [] earthxyz = {0.,0.,0.};
        double [] eclipt = {0.,0.,0.};
        double [] equat  = {0.,0.,0.};
        double [] ecliptvel   = {0.,0.,0.};
        double [] equatvel   = {0.,0.,0.};
        Celest planetcel;

        Observation [] planetpos = new Observation [9];

        xyz = new double [9][3];
        xyzvel = new double [9][3];

        // compute_el(w.when.jd);   refresh the planetary elements

        eclipt = planetxyz(2,w.when.jd);   // earth  ... do separately
        earthxyz = Ecliptic.eclrot(w.when.jd,eclipt[0],eclipt[1],eclipt[2]);
             xyz[2][0] = earthxyz[0];  xyz[2][1] = earthxyz[1]; 
                                     xyz[2][2] = earthxyz[2];
        ecliptvel = planetvel(2,w.when.jd);
        equatvel = Ecliptic.eclrot(w.when.jd,ecliptvel[0],ecliptvel[1],
                                             ecliptvel[2]);
        xyzvel[2][0] = equatvel[0]; xyzvel[2][1] = equatvel[1]; 
                                     xyzvel[2][2] = equatvel[2];

        for(i = 0; i < 9; i++) {
          if(i != 2)  {  // skip earth
             eclipt = planetxyz(i,w.when.jd);
             equat = Ecliptic.eclrot(w.when.jd,eclipt[0],eclipt[1],eclipt[2]);
             // save xyz position of planet for barycentric correction.
             xyz[i][0] = equat[0];  xyz[i][1] = equat[1]; 
                                     xyz[i][2] = equat[2];
             // and the velocities, too
             ecliptvel = planetvel(i,w.when.jd);
             equatvel = Ecliptic.eclrot(w.when.jd,ecliptvel[0],ecliptvel[1],
                                                  ecliptvel[2]);
             xyzvel[i][0] = equatvel[0]; xyzvel[i][1] = equatvel[1]; 
                                          xyzvel[i][2] = equatvel[2];
             planetcel = earthview(earthxyz,equat,w.when.jd);
             planetpos[i] = new Observation(w,planetcel);
           //  System.out.printf("i %d %s\n",i,planetpos[i].ha.RoundedHAString(0,":"));
          }
          else {         // earth
             planetpos[i] = new Observation(w,new Celest(0.,0.,2000.,0.));
          }
        }
        return planetpos;
   }
   
   void printxyz() {  // for diagn.
      int i, j;
      for(i = 0; i < 9; i++) {
         System.out.printf("%d ",i);   
         for(j = 0; j < 3; j++) 
            System.out.printf("%f ",xyz[i][j]);
         System.out.printf("  ");
         for(j = 0; j < 3; j++) 
            System.out.printf("%f ",xyzvel[i][j]);
         System.out.printf("\n");
      }
   }

   void ComputeBaryCor () {
     // Using PREVIOUSLY COMPUTED xyz and xyzvel, computes the offset to the
     // barycenter.
       int i, j;
       
       barycor = new double [6];
       barycor[0] = 0.; barycor[1] = 0.; barycor[2] = 0.;
       barycor[3] = 0.; barycor[4] = 0.; barycor[5] = 0.;
        
       for(i = 0; i < 9; i++) {
          for(j = 0; j < 3; j++) {
             barycor[j] = barycor[j] + xyz[i][j] * mass[i];
             barycor[j+3] = barycor[j+3] + xyzvel[i][j] * mass[i];
          }
       }
       for(j = 0; j < 3; j++) {
          barycor[j] = barycor[j] / Const.SS_MASS;
          barycor[j+3] = barycor[j+3] / Const.SS_MASS;
       }
   }
}


class Constel {
  /** Just a static routine to deliver a constellation. */
  /* This is adapted from an implementation of an algorithm from
     \bibitem[Roman(1987)]{1987PASP...99..695R} Roman, N.~G.\ 1987, 
     \pasp, 99, 695.  I adpated the skycalc routine, which was in turn
     written by Francois Ochsenbein of the CDS, Strasbourg.  There are
     big arrays that define the corners of the constellations (in 1875
     coords).  
  */
  static String getconstel(Celest c) {
      double ra1875, dec1875; 
      int i;
      double [] ra1 = 
      {0,  0.0000,  120.0000,  315.0000,  270.0000,  0.0000,  137.5000,  0.0000,  
      160.0000,  262.5000,  302.5000,  0.0000,  172.5000,  248.0000,  302.5000,  119.5000,  
      137.5000,  195.0000,  46.5000,  306.2500,  170.0000,  0.0000,  210.0000,  353.7500,  
      180.0000,  202.5000,  347.5000,  91.5000,  300.0000,  308.0500,  105.0000,  119.5000,  
      296.5000,  300.0000,  343.0000,  0.0000,  291.2500,  25.5000,  36.5000,  46.5000,  
      334.7500,  75.0000,  210.5000,  216.2500,  47.5000,  332.0000,  309.0000,  0.0000,  
      91.5000,  181.2500,  228.7500,  329.5000,  50.0000,  343.0000,  236.2500,  30.6250,  
      255.0000,  0.0000,  20.5000,  97.5000,  350.0000,  202.5000,  0.0000,  353.7500,  
      272.6250,  273.5000,  286.2500,  25.0000,  126.2500,  2.5000,  180.0000,  102.0000,  
      328.6250,  328.1250,  287.5000,  137.5000,  152.5000,  231.5000,  236.2500,  138.750,  
      0.0000,  37.7500,  290.3750,  67.5000,  326.0000,  328.1250,  98.0000,  110.5000,  
      0.0000,  330.0000,  342.2500,  343.0000,  38.5000,  161.7500,  180.0000,  116.2500,  
      138.7500,  10.7500,  227.7500,  352.5000,  185.0000,  356.2500,  209.3750,  36.2500,  
      40.7500,  67.5000,  272.6250,  165.0000,  295.0000,  71.2500,  148.2500,  198.7500,  
      0.0000,  21.1250,  88.2500,  118.2500,  313.7500,  288.8750,  28.7500,  242.5000,  
      226.2500,  227.7500,  275.5000,  161.2500,  283.0000,  25.0000,  10.7500,  157.5000,  
      318.7500,  85.5000,  1.0000,  238.7500,  88.2500,  297.5000,  283.0000,  2.1250,  
      303.7500,  117.1250,  308.5000,  288.7500,  49.2500,  283.0000,  85.5000,  93.2500,  
      285.0000,  74.5000,  238.7500,  297.5000,  69.2500,  80.0000,  192.5000,  258.7500,  
      178.0000,  112.5000,  251.2500,  0.0000,  84.0000,  105.0000,  316.7500,  94.6250,  
      273.7500,  313.1250,  315.7500,  172.7500,  93.6250,  104.0000,  117.1250,  357.5000,  
      25.0000,  302.1250,  202.5000,  341.2500,  118.8750,  138.7500,  273.7500,  279.9333,  
      312.5000,  105.0000,  273.7500,  241.2500,  273.7500,  322.0000,  0.0000,  278.7500,  
      304.5000,  312.5000,  320.0000,  330.0000,  325.0000,  105.2500,  53.7500,  69.2500,  
      108.0000,  220.0000,  267.5000,  39.7500,  49.2500,  226.2500,  70.0000,  87.5000,  
      267.5000,  273.7500,  278.7500,  341.2500,  161.2500,  172.7500,  0.0000,  357.5000,  
      213.7500,  238.7500,  300.0000,  320.0000,  257.5000,  87.5000,  73.7500,  76.2500,  
      121.2500,  143.7500,  177.5000,  263.7500,  283.0000,  72.5000,  308.0000,  257.5000,  
      273.7500,  125.5000,  244.0000,  128.7500,  161.2500,  244.0000,  235.0000,  188.750,  
      192.5000,  136.2500,  25.0000,  39.7500,  162.5000,  177.5000,  213.7500,  244.0000,  
      0.0000,  320.0000,  328.0000,  357.5000,  146.2500,  70.5000,  72.5000,  300.0000,  
      153.7500,  188.7500,  223.7500,  235.0000,  68.7500,  251.2500,  264.0000,  158.7500,  
      91.7500,  183.7500,  162.5000,  52.5000,  125.5000,  64.0000,  267.5000,  320.0000,  
      345.0000,  45.0000,  140.5000,  0.0000,  25.0000,  58.0000,  350.0000,  212.5000,  
      235.0000,  240.0000,  72.5000,  75.0000,  120.0000,  51.2500,  246.3125,  267.5000,  
      287.5000,  305.0000,  45.0000,  67.5000,  230.0000,  0.0000,  40.0000,  61.2500,  
      64.0000,  320.0000,  90.0000,  120.0000,  36.2500,  57.5000,  0.0000,  90.0000,  
      122.5000,  52.5000,  57.5000,  0.0000,  32.5000,  67.5000,  225.7500,  126.7500,  
      92.5000,  177.5000,  212.5000,  225.7500,  60.0000,  132.5000,  165.0000,  262.5000,  
      270.0000,  330.0000,  48.0000,  75.0000,  97.5000,  0.0000,  20.0000,  350.0000,  
      65.0000,  230.0000,  305.0000,  82.5000,  227.5000,  246.3125,  223.7500,  248.7500,  
      90.0000,  102.5000,  168.7500,  177.5000,  192.5000,  202.5000,  251.2500,  32.5000,  
      48.0000,  221.2500,  252.5000,  262.5000,  330.0000,  68.7500,  205.0000,  221.2500,  
      0.0000,  52.5000,  98.7500,  135.5000,  168.7500,  270.0000,  320.0000,  350.0000,  
      11.2500,  0.0000,  115.0000,  205.0000,  52.5000,  0.0000};  
      
      double [] ra2 = 
      {360,  360.0000,  217.5000,  345.0000,  315.0000,  120.0000,  160.0000,   75.0000,  
      217.5000,  270.0000,  315.0000,   52.6250,  203.7500,  262.5000,  310.0000,  137.500,  
      170.0000,  248.0000,   51.2500,  310.0000,  180.0000,    5.0000,  235.0000,  360.000,  
      202.5000,  216.2500,  353.7500,  105.0000,  306.2500,  309.0000,  119.5000,  126.250,  
      300.0000,  308.0500,  347.5000,   36.5000,  296.5000,   28.6250,   46.5000,   47.500,  
      343.0000,   91.5000,  216.2500,  291.2500,   50.0000,  334.7500,  329.5000,   25.500,  
       97.5000,  202.5000,  236.2500,  332.0000,   75.0000,  350.0000,  255.0000,   37.750,  
      273.5000,   20.5000,   25.0000,  102.0000,  360.0000,  210.5000,   16.7500,  360.000,  
      273.5000,  286.2500,  287.5000,   30.6250,  137.5000,   13.0000,  181.2500,  110.500,  
      329.5000,  328.6250,  291.0000,  152.5000,  161.7500,  236.2500,  245.0000,  143.750,  
       37.7500,   38.5000,  291.0000,   70.3750,  328.1250,  330.0000,  110.5000,  116.250,  
       30.0000,  342.2500,  343.0000,  352.5000,   40.7500,  165.0000,  185.0000,  138.750,  
      148.2500,   21.1250,  231.5000,  356.2500,  198.7500,  360.0000,  210.5000,   40.750,  
       67.5000,   71.2500,  290.3750,  180.0000,  313.7500,   88.2500,  157.5000,  209.375,  
        1.0000,   25.0000,   98.0000,  120.0000,  326.0000,  295.0000,   36.2500,  245.000,  
      227.7500,  242.5000,  283.0000,  165.0000,  288.8750,   28.7500,   12.7500,  161.250,  
      321.2500,   88.2500,    2.1250,  240.5000,   93.2500,  303.7500,  288.7500,   12.750,  
      308.5000,  118.2500,  318.7500,  297.5000,   50.5000,  285.0000,   86.5000,   94.625,  
      297.5000,   80.0000,  241.2500,  303.7500,   74.5000,   84.0000,  202.5000,  273.750,  
      192.5000,  117.1250,  258.7500,    2.1250,   86.5000,  112.5000,  320.0000,  104.000,  
      283.0000,  315.7500,  316.7500,  178.0000,   94.6250,  105.0000,  118.8750,  360.000,  
       49.2500,  304.5000,  226.2500,  357.5000,  138.7500,  161.2500,  279.9333,  283.000,  
      313.1250,  105.2500,  276.3750,  251.2500,  276.3750,  325.0000,   30.0000,  283.000,  
      312.5000,  320.0000,  322.0000,  341.2500,  330.0000,  108.0000,   69.2500,   70.000,  
      121.2500,  226.2500,  273.7500,   49.2500,   53.7500,  244.0000,   76.2500,   93.625,  
      269.5000,  278.7500,  283.0000,  357.5000,  172.7500,  177.5000,    5.0000,  360.000,  
      220.0000,  244.0000,  308.0000,  328.0000,  269.5000,  121.2500,   76.2500,   87.500,  
      125.5000,  161.2500,  192.5000,  265.0000,  300.0000,   73.7500,  320.0000,  273.750,  
      283.0000,  128.7500,  245.6250,  136.2500,  162.5000,  245.6250,  238.7500,  192.500,  
      213.7500,  146.2500,   39.7500,   56.2500,  177.5000,  188.7500,  223.7500,  251.250,  
       25.0000,  328.0000,  357.5000,  360.0000,  153.7500,   72.5000,   91.7500,  320.000,  
      158.7500,  223.7500,  235.0000,  240.0000,   70.5000,  264.0000,  267.5000,  162.500,  
      110.5000,  188.7500,  183.7500,   56.2500,  140.5000,   68.7500,  287.5000,  345.000,  
      350.0000,   52.5000,  165.0000,   25.0000,   45.0000,   64.0000,  360.0000,  223.750,  
      240.0000,  246.3125,   75.0000,   98.7500,  125.5000,   58.0000,  267.5000,  287.500,  
      305.0000,  320.0000,   51.2500,   72.5000,  235.0000,   35.0000,   45.0000,   64.000,  
       67.5000,  330.0000,  120.0000,  122.5000,   40.0000,   61.2500,   27.5000,   92.500,  
      126.7500,   57.5000,   60.0000,   23.7500,   36.2500,   75.0000,  230.0000,  132.500,  
       97.5000,  192.5000,  225.7500,  230.0000,   65.0000,  165.0000,  168.7500,  270.000,  
      305.0000,  350.0000,   52.5000,   82.5000,  102.5000,   20.0000,   32.5000,  360.000,  
       68.7500,  246.3125,  320.0000,   90.0000,  230.0000,  248.7500,  227.5000,  251.250,  
      102.5000,  135.5000,  177.5000,  192.5000,  218.0000,  205.0000,  252.5000,   48.000,  
       68.7500,  223.7500,  262.5000,  270.0000,  350.0000,   98.7500,  221.2500,  255.000,  
       20.0000,   68.7500,  135.5000,  168.7500,  205.0000,  320.0000,  350.0000,  360.000,  
       20.0000,   52.5000,  205.0000,  270.0000,  115.0000,  360.0000};  
      
      double [] decs = 
       {90,   88.0000,   86.5000,   86.1667,   86.0000,   85.0000,   82.0000,   80.0000,  
       80.0000,   80.0000,   80.0000,   77.0000,   77.0000,   75.0000,   75.0000,   73.500,  
       73.5000,   70.0000,   68.0000,   67.0000,   66.5000,   66.0000,   66.0000,   66.000,  
       64.0000,   63.0000,   63.0000,   62.0000,   61.5000,   60.9167,   60.0000,   60.000,  
       59.5000,   59.5000,   59.0833,   58.5000,   58.0000,   57.5000,   57.0000,   57.000,  
       56.2500,   56.0000,   55.5000,   55.5000,   55.0000,   55.0000,   54.8333,   54.000,  
       54.0000,   53.0000,   53.0000,   52.7500,   52.5000,   52.5000,   51.5000,   50.500,  
       50.5000,   50.0000,   50.0000,   50.0000,   50.0000,   48.5000,   48.0000,   48.000,  
       47.5000,   47.5000,   47.5000,   47.0000,   47.0000,   46.0000,   45.0000,   44.500,  
       44.0000,   43.7500,   43.5000,   42.0000,   40.0000,   40.0000,   40.0000,   39.750,  
       36.7500,   36.7500,   36.5000,   36.0000,   36.0000,   36.0000,   35.5000,   35.500,  
       35.0000,   35.0000,   34.5000,   34.5000,   34.0000,   34.0000,   34.0000,   33.500,  
       33.5000,   33.0000,   33.0000,   32.0833,   32.0000,   31.3333,   30.7500,   30.666,  
       30.6667,   30.0000,   30.0000,   29.0000,   29.0000,   28.5000,   28.5000,   28.500,  
       28.0000,   28.0000,   28.0000,   28.0000,   28.0000,   27.5000,   27.2500,   27.000,  
       26.0000,   26.0000,   26.0000,   25.5000,   25.5000,   25.0000,   23.7500,   23.500,  
       23.5000,   22.8333,   22.0000,   22.0000,   21.5000,   21.2500,   21.0833,   21.000,  
       20.5000,   20.0000,   19.5000,   19.1667,   19.0000,   18.5000,   18.0000,   17.500,  
       16.1667,   16.0000,   16.0000,   15.7500,   15.5000,   15.5000,   15.0000,   14.333,  
       14.0000,   13.5000,   12.8333,   12.5000,   12.5000,   12.5000,   12.5000,   12.000,  
       12.0000,   11.8333,   11.8333,   11.0000,   10.0000,   10.0000,   10.0000,   10.000,  
        9.9167,    8.5000,    8.0000,    7.5000,    7.0000,    7.0000,    6.2500,    6.250,  
        6.0000,    5.5000,    4.5000,    4.0000,    3.0000,    2.7500,    2.0000,    2.000,  
        2.0000,    2.0000,    2.0000,    2.0000,    1.7500,    1.5000,    0.0000,    0.000,  
        0.0000,    0.0000,    0.0000,   -1.7500,   -1.7500,   -3.2500,   -4.0000,   -4.000,  
       -4.0000,   -4.0000,   -4.0000,   -4.0000,   -6.0000,   -6.0000,   -7.0000,   -7.000,  
       -8.0000,   -8.0000,   -9.0000,   -9.0000,  -10.0000,  -11.0000,  -11.0000,  -11.000,  
      -11.0000,  -11.0000,  -11.0000,  -11.6667,  -12.0333,  -14.5000,  -15.0000,  -16.000,  
      -16.0000,  -17.0000,  -18.2500,  -19.0000,  -19.0000,  -19.2500,  -20.0000,  -22.000,  
      -22.0000,  -24.0000,  -24.3833,  -24.3833,  -24.5000,  -24.5000,  -24.5000,  -24.583,  
      -25.5000,  -25.5000,  -25.5000,  -25.5000,  -26.5000,  -27.2500,  -27.2500,  -28.000,  
      -29.1667,  -29.5000,  -29.5000,  -29.5000,  -30.0000,  -30.0000,  -30.0000,  -31.166,  
      -33.0000,  -33.0000,  -35.0000,  -36.0000,  -36.7500,  -37.0000,  -37.0000,  -37.000,  
      -37.0000,  -39.5833,  -39.7500,  -40.0000,  -40.0000,  -40.0000,  -40.0000,  -42.000,  
      -42.0000,  -42.0000,  -43.0000,  -43.0000,  -43.0000,  -44.0000,  -45.5000,  -45.500,  
      -45.5000,  -45.5000,  -46.0000,  -46.5000,  -48.0000,  -48.1667,  -49.0000,  -49.000,  
      -49.0000,  -50.0000,  -50.7500,  -50.7500,  -51.0000,  -51.0000,  -51.5000,  -52.500,  
      -53.0000,  -53.1667,  -53.1667,  -53.5000,  -54.0000,  -54.0000,  -54.0000,  -54.500,  
      -55.0000,  -55.0000,  -55.0000,  -55.0000,  -56.5000,  -56.5000,  -56.5000,  -57.000,  
      -57.0000,  -57.0000,  -57.5000,  -57.5000,  -58.0000,  -58.5000,  -58.5000,  -58.500,  
      -59.0000,  -60.0000,  -60.0000,  -61.0000,  -61.0000,  -61.0000,  -63.5833,  -63.583,  
      -64.0000,  -64.0000,  -64.0000,  -64.0000,  -64.0000,  -65.0000,  -65.0000,  -67.500,  
      -67.5000,  -67.5000,  -67.5000,  -67.5000,  -67.5000,  -70.0000,  -70.0000,  -70.000,  
      -75.0000,  -75.0000,  -75.0000,  -75.0000,  -75.0000,  -75.0000,  -75.0000,  -75.000,  
      -76.0000,  -82.5000,  -82.5000,  -82.5000,  -85.0000,  -90.0000};
      
      String [] abbrevs = 
      { " " , "UMi" , "UMi" , "UMi" , "UMi" , "Cep" , "Cam" , "Cep" , 
      "Cam" , "UMi" , "Dra" , "Cep" , "Cam" , "UMi" , "Cep" , "Cam" , 
      "Dra" , "UMi" , "Cas" , "Dra" , "Dra" , "Cep" , "UMi" , "Cep" , 
      "Dra" , "Dra" , "Cep" , "Cam" , "Dra" , "Cep" , "Cam" , "UMa" , 
      "Dra" , "Cep" , "Cep" , "Cas" , "Dra" , "Cas" , "Cas" , "Cam" , 
      "Cep" , "Cam" , "UMa" , "Dra" , "Cam" , "Cep" , "Cep" , "Cas" , 
      "Lyn" , "UMa" , "Dra" , "Cep" , "Cam" , "Cas" , "Dra" , "Per" , 
      "Dra" , "Cas" , "Per" , "Lyn" , "Cas" , "UMa" , "Cas" , "Cas" , 
      "Her" , "Dra" , "Cyg" , "Per" , "UMa" , "Cas" , "UMa" , "Lyn" , 
      "Cyg" , "Cyg" , "Cyg" , "UMa" , "UMa" , "Boo" , "Her" , "Lyn" , 
      "And" , "Per" , "Lyr" , "Per" , "Cyg" , "Lac" , "Aur" , "Lyn" , 
      "And" , "Lac" , "Lac" , "And" , "Per" , "UMa" , "CVn" , "Lyn" , 
      "LMi" , "And" , "Boo" , "And" , "CVn" , "And" , "CVn" , "Tri" , 
      "Per" , "Aur" , "Lyr" , "UMa" , "Cyg" , "Aur" , "LMi" , "CVn" , 
      "And" , "Tri" , "Aur" , "Gem" , "Cyg" , "Cyg" , "Tri" , "CrB" , 
      "Boo" , "CrB" , "Lyr" , "LMi" , "Lyr" , "Tri" , "Psc" , "LMi" , 
      "Vul" , "Tau" , "And" , "Ser" , "Gem" , "Vul" , "Vul" , "And" , 
      "Vul" , "Gem" , "Vul" , "Vul" , "Ari" , "Sge" , "Ori" , "Gem" , 
      "Sge" , "Tau" , "Her" , "Sge" , "Tau" , "Tau" , "Com" , "Her" , 
      "Com" , "Gem" , "Her" , "Peg" , "Tau" , "Gem" , "Peg" , "Gem" , 
      "Her" , "Del" , "Peg" , "Leo" , "Ori" , "Gem" , "Cnc" , "Peg" , 
      "Ari" , "Del" , "Boo" , "Peg" , "Cnc" , "Leo" , "Oph" , "Aql" , 
      "Del" , "CMi" , "Ser" , "Her" , "Oph" , "Peg" , "Psc" , "Ser" , 
      "Del" , "Equ" , "Peg" , "Peg" , "Peg" , "CMi" , "Tau" , "Ori" , 
      "CMi" , "Vir" , "Oph" , "Cet" , "Tau" , "Ser" , "Ori" , "Ori" , 
      "Ser" , "Ser" , "Aql" , "Psc" , "Leo" , "Vir" , "Psc" , "Psc" , 
      "Vir" , "Oph" , "Aql" , "Aqr" , "Oph" , "Mon" , "Eri" , "Ori" , 
      "Hya" , "Sex" , "Vir" , "Oph" , "Aql" , "Eri" , "Aqr" , "Ser" , 
      "Sct" , "Hya" , "Oph" , "Hya" , "Crt" , "Sco" , "Lib" , "Crv" , 
      "Vir" , "Hya" , "Cet" , "Eri" , "Crt" , "Crv" , "Lib" , "Oph" , 
      "Cet" , "Cap" , "Aqr" , "Cet" , "Hya" , "Eri" , "Lep" , "Cap" , 
      "Hya" , "Hya" , "Lib" , "Sco" , "Eri" , "Oph" , "Sgr" , "Hya" , 
      "CMa" , "Hya" , "Hya" , "For" , "Pyx" , "Eri" , "Sgr" , "PsA" , 
      "Scl" , "For" , "Ant" , "Scl" , "For" , "Eri" , "Scl" , "Cen" , 
      "Lup" , "Sco" , "Cae" , "Col" , "Pup" , "Eri" , "Sco" , "CrA" , 
      "Sgr" , "Mic" , "Eri" , "Cae" , "Lup" , "Phe" , "Eri" , "Hor" , 
      "Cae" , "Gru" , "Pup" , "Vel" , "Eri" , "Hor" , "Phe" , "Car" , 
      "Vel" , "Hor" , "Dor" , "Phe" , "Eri" , "Pic" , "Lup" , "Vel" , 
      "Car" , "Cen" , "Lup" , "Nor" , "Dor" , "Vel" , "Cen" , "Ara" , 
      "Tel" , "Gru" , "Hor" , "Pic" , "Car" , "Phe" , "Eri" , "Phe" , 
      "Dor" , "Nor" , "Ind" , "Pic" , "Cir" , "Ara" , "Cir" , "Ara" , 
      "Pic" , "Car" , "Cen" , "Cru" , "Cen" , "Cir" , "Ara" , "Hor" , 
      "Ret" , "Cir" , "Ara" , "Pav" , "Tuc" , "Dor" , "Cir" , "TrA" , 
      "Tuc" , "Hyi" , "Vol" , "Car" , "Mus" , "Pav" , "Ind" , "Tuc" , 
      "Tuc" , "Hyi" , "Cha" , "Aps" , "Men" , "Oct" }; 
      
       Celest c1875 = c.precessed(1875.);
       ra1875 = c1875.Alpha.degrees();
       dec1875 = c1875.Delta.degrees();
       i = 0;
       while(i < ra1.length) {
           if((ra1875 >= ra1[i]) && (ra1875 < ra2[i]) && (dec1875 >= decs[i])) break;
           i++;
       } 
       if (i < ra1.length) return (abbrevs[i]);
       else return "???";
   }
}

class NightlyAlmanac {
   /** For finding timing of various phenomena, esp sun and moon rise and set. */
       
   WhenWhere midnight;  
   WhenWhere sunrise;
   WhenWhere sunset;
   WhenWhere moonrise;
   WhenWhere moonset;
   WhenWhere eveningTwilight;
   WhenWhere morningTwilight;
   WhenWhere nightcenter;
   
   static double jd_sun_alt(double alt, WhenWhere wIn) {
   /**  finds the jd at which the sun is at altitude alt, given initial guess handed
        in with a whenwhere.  */
      double jdguess, lastjd;
      double deriv, err, del = 0.002;
      double alt2, alt3;
      int i = 0;

      WhenWhere w = (WhenWhere) wIn.clone();

   /* Set up calculation, then walk in with Newton-Raphson scheme (guess and 
      check using numerical derivatives). */

      jdguess = w.when.jd;
//      System.out.printf("Before makelocalsun, w.when.jd %f\n",w.when.jd);
      w.MakeLocalSun();
      alt2 = w.altsun;
//      System.out.printf("after alt2: w.when.jd %f alt2 %f\n",
 //                  w.when.jd,alt2);
      jdguess = jdguess + del;
      w.ChangeWhen(jdguess);
      w.UpdateLocalSun();
      alt3 = w.altsun;
      err = alt3 - alt;
      deriv = (alt3 - alt2) / del;
//      System.out.printf("alt2 alt3 %f %f  err %f deriv %f\n",
 //             alt2,alt3,err,deriv);
      while((Math.abs(err) > 0.02) && (i < 10)) {
          lastjd = jdguess;  
          alt2 = alt3;       // save last guess
          jdguess = jdguess - err/deriv;
          w.ChangeWhen(jdguess);
          w.UpdateLocalSun();
          alt3 = w.altsun;
//          System.out.printf("alt3 %f jdguess %f\n",alt3,jdguess);
          err = alt3 - alt;
          i++;
          deriv = (alt3 - alt2) / (jdguess - lastjd);
  
          if(i == 9) System.out.printf("jd_sun_alt not converging.\n");

      }
      if (i >= 9) jdguess = -1000.;
//      System.out.printf("Leaving sun w/ wIn %f w %f\n",wIn.when.jd,w.when.jd);
      return jdguess;
   }  

   static double jd_moon_alt(double alt, WhenWhere wIn) {
   /**  finds the jd at which the moon is at altitude alt, given initial guess handed
        in with a whenwhere.  */
      double jdguess, lastjd;
      double deriv, err, del = 0.002;
      double alt2, alt3;
      int i = 0;

   /* Set up calculation, then walk in with Newton-Raphson scheme (guess and 
      check using numerical derivatives). */

      WhenWhere w =  (WhenWhere) wIn.clone();

      // System.out.printf("Into jd_moon_alt, target = %f\n",alt);
      jdguess = w.when.jd;
      w.MakeLocalMoon();
      alt2 = w.altmoon;
      // System.out.printf("after alt2: w.when.jd %f alt2 %f\n",
      //              w.when.jd,alt2);
      jdguess = jdguess + del;
      w.ChangeWhen(jdguess);
      w.UpdateLocalMoon();
      alt3 = w.altmoon;
      err = alt3 - alt;
      deriv = (alt3 - alt2) / del;
      // System.out.printf("alt2 alt3 %f %f  err %f deriv %f\n",
      //        alt2,alt3,err,deriv);
      while((Math.abs(err) > 0.02) && (i < 10)) {
          lastjd = jdguess;  
          alt2 = alt3;       // save last guess
          jdguess = jdguess - err/deriv;
          w.ChangeWhen(jdguess);
          w.UpdateLocalMoon();
          alt3 = w.altmoon;
          // System.out.printf("alt3 %f jdguess %f ",alt3,jdguess,deriv);
          err = alt3 - alt;
          i++;
          deriv = (alt3 - alt2) / (jdguess - lastjd);
          // System.out.printf(" err %f deriv %f\n",err,deriv);
  
          if(i == 9) System.out.printf("jd_moon_alt not converging.\n");

      }
      if (i >= 9) jdguess = -1000.;
      // System.out.printf("Exiting with jdguess = %f\n\n",jdguess);
      return jdguess;
   }  

   NightlyAlmanac(WhenWhere wIn) {
   /** Computes rise, set, and twilight for the night nearest in time to wIn.when */

      WhenWhere w = (WhenWhere) wIn.clone();

      midnight = new WhenWhere(w.when.clone(),w.where.clone());   // instantiate these ...
      // midnight.MakeLocalSun();
      // midnight.MakeLocalMoon();

      // AAACK!  Have to clone EVERY WHEN, or they are all the SAME WHEN forever.

      sunrise = new WhenWhere(w.when.clone(), w.where.clone());
      sunset = new WhenWhere(w.when.clone(), w.where.clone());
      moonrise = new WhenWhere(w.when.clone(), w.where.clone());
      moonset = new WhenWhere(w.when.clone(), w.where.clone());
      eveningTwilight = new WhenWhere(w.when.clone(), w.where.clone());
      morningTwilight = new WhenWhere(w.when.clone(), w.where.clone());
      nightcenter = new WhenWhere(w.when.clone(), w.where.clone());

      Update(w);
   }

   void Update(WhenWhere wIn) {
     /** Computes rise, set, and twilight for the night nearest in time to wIn.when */
     double horiz;    // depression of the horizon in degrees
     double rise_set_alt;
     double dtrise, dtset;  
     double jdtemp, jdnoon;
     double twilight_alt = -18.;  // the standard choice for solar altitude at twilight

     // be sure the site infor is up to date

     WhenWhere w = (WhenWhere) wIn.clone();
     w.when = (InstantInTime)  wIn.when.clone();  
     w.where = (Site) wIn.where.clone();
     midnight.where = (Site) w.where.clone();
     midnight.when = (InstantInTime) w.when.clone();
     sunrise.where = (Site) w.where.clone();
     sunset.where = (Site) w.where.clone();
     moonrise.where = (Site) w.where.clone();
     moonset.where = (Site) w.where.clone();
     eveningTwilight.where = (Site) w.where.clone();
     morningTwilight.where = (Site) w.where.clone();
     nightcenter.where = (Site) w.where.clone();

     // approx means you can't use a negative elevation for the obs to 
     // compensate for higher terrain.

     horiz = Const.DEG_IN_RADIAN * 
              Math.sqrt(2. * w.where.elevhoriz / (1000. * Const.EARTHRAD_IN_KM));
     rise_set_alt = -(0.83 + horiz);   
        // upper limb of sun and moon rise and set when center of disk is about 50 arcmin
        // below horizon, mostly because of refraction.  Sun and moon are almost the same
        // angular size, and variation in refraction is much larger than ang. size variations.

 
     // Establish and set the nearest midnight -- previous if before local noon, 
     // next midnight if after local noon.      

      midnight.when = (InstantInTime) w.when.clone();
//      System.out.printf("Entering almanac with local  %s\n",midnight.when.localDate.RoundedCalString(0,0));
      if(midnight.when.localDate.timeofday.hour >= 12) {
         midnight.when.localDate.timeofday.hour = 23;
         midnight.when.localDate.timeofday.minute = 59;
         midnight.when.localDate.timeofday.second = 59.9;
      }
      else {
         midnight.when.localDate.timeofday.hour = 0;
         midnight.when.localDate.timeofday.minute = 0;
         midnight.when.localDate.timeofday.second = 0.0;
      }
      jdtemp = midnight.when.localDate.Cal2JD();
     // System.out.printf("jdtemp (local) = %f\n",jdtemp);
      midnight.when.SetInstant(jdtemp, w.where.stdz, w.where.use_dst, false);
     // System.out.printf("translates to midnight.jd %f\n",midnight.when.jd);
      double jdmid = midnight.when.jd;   // the real JD
      midnight.ChangeWhen(jdmid);        // to synch sidereal etc.
//      System.out.printf("Midnight set to %s\n", midnight.when.UTDate.RoundedCalString(0,1));
//      System.out.printf("lst at midnight = %f\n",midnight.sidereal);

      midnight.UpdateLocalSun();
      midnight.UpdateLocalMoon();

      // See if the sun rises or sets ... 
      double hasunrise = Spherical.ha_alt(midnight.sun.topopos.Delta.value,
              midnight.where.lat.value, rise_set_alt);
//      System.out.printf("hourangle sunrise: %f\n",hasunrise);

      if(hasunrise < 11.8 && hasunrise > 0.2) {
         // if sun grazes horizon, small changes in dec may affect whether it actually
         // rises or sets.  Since dec is computed at midnight, put a little pad on to 
         // avoid non-convergent calculations.
        // sunrise = new WhenWhere(jdmid + hasunrise/24.,w.where);

         dtrise = midnight.sun.topopos.Alpha.value - hasunrise - 
              midnight.sidereal;
         
         while (dtrise >= 12.) dtrise -= 24.;
         while (dtrise < -12.) dtrise += 24.;

         dtset = midnight.sun.topopos.Alpha.value + hasunrise - 
              midnight.sidereal;
         while (dtset >= 12) dtset -= 24.;
         while (dtset < -12.) dtset += 24.;

//         System.out.printf("going to jd_sun_alt with est sunrise = %f\n",jdmid + dtrise/24.);
         sunrise.ChangeWhen(jdmid + dtrise / 24.);
         sunrise.UpdateLocalSun();
//         System.out.printf("sunrise.when.jd %f, sunrise.altsun %f\n",sunrise.when.jd,
//             sunrise.altsun);
         jdtemp = jd_sun_alt(rise_set_alt,sunrise);
//         System.out.printf("out, sunrise = %f\n",jdtemp);
         sunrise.ChangeWhen(jdtemp);
         
//         System.out.printf("going to jd_sun_alt with est sunset = %f\n",jdmid + dtset/24.);
         sunset.ChangeWhen(jdmid + dtset/24.);
         sunset.UpdateLocalSun();
         jdtemp = jd_sun_alt(rise_set_alt,sunset);
//         System.out.printf("out, sunset = %f\n",jdtemp);
         sunset.ChangeWhen(jdtemp);
//         System.out.printf("In NightlyAlmanac.Update, sunset set to:\n");
//         sunset.dump();
         nightcenter.ChangeWhen((sunset.when.jd + sunrise.when.jd) / 2.);
      }
      else if (hasunrise < 0.2) {  // may not rise ... set sunrise to noontime to flag.
         if(midnight.when.localDate.timeofday.hour == 23) 
            jdnoon = jdmid - 0.5;
         else jdnoon = jdmid + 0.5;
         sunrise.ChangeWhen(jdnoon);
         sunset.ChangeWhen(jdnoon);
         nightcenter.ChangeWhen(jdnoon);
      }
      else if (hasunrise >= 11.8) { // may not set ... set sunset to midnight to flag.
         sunrise.ChangeWhen(jdmid);
         sunset.ChangeWhen(jdmid);
         nightcenter.ChangeWhen(jdmid);
      }

      // Now let's do the same thing for twilight ... 
      double hatwilight = Spherical.ha_alt(midnight.sun.topopos.Delta.value,
              midnight.where.lat.value, twilight_alt);
      // System.out.printf("hourangle sunrise: %f\n",hasunrise);
      if(hatwilight < 11.8 && hatwilight > 0.2) {

         dtrise = midnight.sun.topopos.Alpha.value - hatwilight - 
              midnight.sidereal;
         
         while (dtrise >= 12.) dtrise -= 24.;
         while (dtrise < -12.) dtrise += 24.;

         dtset = midnight.sun.topopos.Alpha.value + hatwilight - 
              midnight.sidereal;
         while (dtset >= 12) dtset -= 24.;
         while (dtset < -12.) dtset += 24.;

         eveningTwilight.ChangeWhen(jdmid + dtset / 24.);
         eveningTwilight.UpdateLocalSun();
         jdtemp = jd_sun_alt(twilight_alt,eveningTwilight);
         eveningTwilight.ChangeWhen(jdtemp);

         morningTwilight.ChangeWhen(jdmid + dtrise / 24.);
         morningTwilight.UpdateLocalSun();
         jdtemp = jd_sun_alt(twilight_alt,morningTwilight);
         morningTwilight.ChangeWhen(jdtemp);

      }
      else if (hatwilight < 0.2) {  // twilight may not begin ... set to noon to flag.
         if(midnight.when.localDate.timeofday.hour == 23)  // this case will be rare.
            jdnoon = jdmid - 0.5;
         else jdnoon = jdmid + 0.5;
         morningTwilight.ChangeWhen(jdnoon);
         eveningTwilight.ChangeWhen(jdnoon);

      }
      else if (hatwilight >= 11.8) { // twilight may not end (midsummer at high lat).
         morningTwilight.ChangeWhen(jdmid);
         eveningTwilight.ChangeWhen(jdmid);
           // flag -- set twilight to exactly midn.
      }

      // now we tackle the moon ... which is a bit harder.

      double hamoonrise = Spherical.ha_alt(midnight.moon.topopos.Delta.value,
              midnight.where.lat.value, rise_set_alt);

      if(hamoonrise < 11. && hamoonrise > 1.) {
         // The moon moves faster than the sun, so set more conservative limits for
         // proceeding with the calculation.

         dtrise = midnight.moon.topopos.Alpha.value - hamoonrise - 
              midnight.sidereal;
         
         while (dtrise >= 12.) dtrise -= 24.;
         while (dtrise < -12.) dtrise += 24.;

         dtset = midnight.moon.topopos.Alpha.value + hamoonrise - 
              midnight.sidereal;
         while (dtset >= 12) dtset -= 24.;
         while (dtset < -12.) dtset += 24.;
/*
         System.out.printf("ra moon midn    %s\n",midnight.moon.topopos.Alpha.RoundedRAString(0,":"));
         System.out.printf("lst at midnight %f\n",midnight.sidereal);
         System.out.printf("rise-set HA %f\n",hamoonrise);
         System.out.printf("dtrise %f dtset %f\n",dtrise,dtset);
*/

         moonrise.ChangeWhen(jdmid + dtrise / 24.);
         moonrise.UpdateLocalMoon();
         jdtemp = jd_moon_alt(rise_set_alt,moonrise);
         moonrise.ChangeWhen(jdtemp);

         moonset.ChangeWhen(jdmid + dtset/24.);
         moonset.UpdateLocalMoon();
         jdtemp = jd_moon_alt(rise_set_alt,moonset);
         moonset.ChangeWhen(jdtemp);
      }
   }
}          

class Seasonal {
   double [] jdnew;
   double [] jdfull;   // new and full moon dates bracketing the present
   double ha_at_center;       // hour angle at night center
   public Object [][] tabledata;
   static Observation lastcomputedobs;
   boolean wasupdated = false;

   Seasonal(Observation obs) {  // just sets up arrays
      int i;
      jdnew = new double [8];
      jdfull = new double [8];
      tabledata = new Object [16][11];
      lastcomputedobs = (Observation) obs.clone();
      this.Update(obs);  // for constructor, go ahead and do it ... 
   }
 
   void Update(Observation obs) {
      int lun, i;
      double [] xy; 
      double min_alt, max_alt;
      String [] hoursup = {" "," "," "};
   
      // check to see if anything has changed significantly, otherwise skip it.
      if(Math.abs(lastcomputedobs.w.when.jd - obs.w.when.jd) > 5. || 
        !obs.c.equals(lastcomputedobs.c) || !obs.w.where.equals(lastcomputedobs.w.where))  {

         Observation oseason = (Observation) obs.clone();
         NightlyAlmanac ng = new NightlyAlmanac(oseason.w);

         xy = Spherical.min_max_alt(oseason.w.where.lat.value, oseason.current.Delta.value);
         min_alt = xy[0];
         max_alt = xy[1];

         // run computations for new and full moon in a roughly +- 3-month interval
         
         int lunstart = Moon.lunation(oseason.w.when.jd) - 3;
         for(i = 0; i < 8; i++) {

            lun = lunstart + i;

            jdnew[i] = Moon.flmoon(lun,0);
            oseason.w.ChangeWhen(jdnew[i]);

            ng.Update(oseason.w);          
            tabledata[2 * i][0] =  " New ";   
            // tabulated date is EVENING DATE of the night closest to the instant of phase ... 
            tabledata[2 * i][1] = (Object) ng.sunset.when.localDate.RoundedCalString(3,0);
            // System.out.printf("New : %s  ",oseason.w.when.localDate.RoundedCalString(1,0));
            
            // System.out.printf(" sunrise: %s\n",ng.sunrise.when.localDate.RoundedCalString(1,0));

            oseason.w.ChangeWhen(ng.eveningTwilight.when.jd);
            oseason.ComputeSky();
            tabledata[2 * i][2] = oseason.ha.RoundedHAString(-2,":");
            tabledata[2 * i][3] = airmassstring(oseason.altitude,oseason.airmass);

            oseason.w.ChangeWhen(ng.nightcenter.when.jd);
            oseason.ComputeSky();
            tabledata[2 * i][4] = oseason.ha.RoundedHAString(-2,":");
            tabledata[2 * i][5] = airmassstring(oseason.altitude,oseason.airmass);
            ha_at_center = oseason.ha.value;  // store for later

            oseason.w.ChangeWhen(ng.morningTwilight.when.jd);
            oseason.ComputeSky();
            tabledata[2 * i][6] = oseason.ha.RoundedHAString(-2,":");
            tabledata[2 * i][7] = airmassstring(oseason.altitude,oseason.airmass);

            hoursup = NightHoursAboveAirmass(oseason, ng, min_alt, max_alt); 
            tabledata[2 * i][8] = hoursup[0];
            tabledata[2 * i][9] = hoursup[1];
            tabledata[2 * i][10] = hoursup[2];

            jdfull[i] = Moon.flmoon(lun,2);
            oseason.w.ChangeWhen(jdfull[i]);

            ng.Update(oseason.w);          
            tabledata[2 * i + 1][0] = " Full";
            tabledata[2 * i + 1][1] = ng.sunset.when.localDate.RoundedCalString(3,0);
            // System.out.printf("Full: %s  ",oseason.w.when.localDate.RoundedCalString(1,0));

            ng.Update(oseason.w);          
            // System.out.printf(" sunrise: %s\n",ng.sunrise.when.localDate.RoundedCalString(1,0));

            oseason.w.ChangeWhen(ng.eveningTwilight.when.jd);
            oseason.ComputeSky();
            tabledata[2 * i + 1][2] = oseason.ha.RoundedHAString(-2,":");
            tabledata[2 * i + 1][3] = airmassstring(oseason.altitude,oseason.airmass);

            oseason.w.ChangeWhen(ng.nightcenter.when.jd);
            oseason.ComputeSky();
            tabledata[2 * i + 1][4] = oseason.ha.RoundedHAString(-2,":");
            tabledata[2 * i + 1][5] = airmassstring(oseason.altitude,oseason.airmass);
            ha_at_center = oseason.ha.value;  // store for later

            oseason.w.ChangeWhen(ng.morningTwilight.when.jd);
            oseason.ComputeSky();
            tabledata[2 * i + 1][6] = oseason.ha.RoundedHAString(-2,":");
            tabledata[2 * i + 1][7] = airmassstring(oseason.altitude,oseason.airmass);

            hoursup = NightHoursAboveAirmass(oseason, ng, min_alt, max_alt); 
            tabledata[2 * i + 1][8] = hoursup[0];
            tabledata[2 * i + 1][9] = hoursup[1];
            tabledata[2 * i + 1][10] = hoursup[2];

         }
         wasupdated = true;
         lastcomputedobs = obs.clone();  // copy computed obs for use in skip condition
         // dump();  
      }
      else {
         wasupdated = false ;
         // System.out.printf("no seasonal update.\n");  // diagn. used to check skip condition.
      }
   }

   String airmassstring(double altitude, double airmass) {
      // this happens so many times that it's worth writing a method.
      if(altitude < 0.) return "(down)";
      else if (airmass > 10.) return "> 10.";
      else return String.format(Locale.ENGLISH, "%5.2f",airmass);
   }      

   double hrs_up(double jdup, double jddown, double jdeve, double jdmorn) {
     /* an object comes up past a given point at jdup, and goes down at jddown,
        with twilight jdeve and jdmorn.  Computes how long object is up *and* it's
        dark.  Transcribed without significant modification from _skysub.c  ... */
     
      double jdup2, jddown0;   

      if(jdup < jdeve) {
        if(jddown >= jdmorn)   /* up all night */
           return ((jdmorn - jdeve) * 24.);
        else if(jddown >= jdeve)  {
           /* careful here ... circumpolar objects can come back *up*
              a second time before morning.  jdup and jddown are
              the ones immediately preceding and following the upper
              culmination nearest the center of the night, so "jdup"
              can be on the previous night rather than the one we want. */
          jdup2 = jdup + 1.0/Const.SID_RATE;
          if(jdup2 > jdmorn)  /* the usual case ... doesn't rise again */
              return ((jddown - jdeve) * 24.);
          else return(((jddown - jdeve) + (jdmorn - jdup2)) * 24.);
        }
        else return(0.);
      }
      else if(jddown > jdmorn) {
        if(jdup >= jdmorn) return(0.);
        else {
           /* again, a circumpolar object can be up at evening twilight
               and come 'round again in the morning ... */
           jddown0 = jddown - 1.0/Const.SID_RATE;
           if(jddown0 < jdeve) return((jdmorn - jdup) * 24.);
           else return(((jddown0 - jdeve) + (jdmorn - jdup)) * 24.);
        }
      }
      else return((jddown - jdup)*24.);  /* up & down the same night ...
         might happen a second time in pathological cases, but this will
         be extremely rare except at very high latitudes.  */
   }

   void dump() {
      int i, j;
      for(i = 0; i < 16; i++) {
         for(j = 0; j < 11; j++) {
            System.out.printf("%8s ",tabledata[i][j]);
         }
         System.out.printf("\n");
      }
   }

   String [] NightHoursAboveAirmass(Observation obs, NightlyAlmanac ng, double min_alt, double max_alt) {
      double [] critical_alt = {19.2786, 29.8796, 41.7592};
          // altitudes above horizon at which true airmass = 1.5, 2, and 3 respectively.
      String [] retvals = {" "," "," "};   // night hours above critical altitude
      double jdtrans;   // jd of transit closest to midnight 
      double [] dt = {0.,0.,0.};  
      double [] jdup = {0.,0.,0.};
      double [] jddown = {0.,0.,0.};
      int i; 

      WhenWhere diagn = obs.w.clone();

      // jd of transit nearest midnight 

      jdtrans = ng.nightcenter.when.jd - ha_at_center / (24. * Const.SID_RATE);
 
//      diagn.ChangeWhen(jdtrans);
//      System.out.printf("diagnostic: transit at local time %s\n",
//             diagn.when.localDate.RoundedCalString(0,0));

      for(i = 0; i < 3; i++) {
          if((min_alt < critical_alt[i]) && (max_alt > critical_alt[i])) { // passes this altitude
             dt[i] = Spherical.ha_alt(obs.current.Delta.value,obs.w.where.lat.value,critical_alt[i]) / 
                          (Const.SID_RATE * 24.);
             jdup[i] = jdtrans - dt[i];
             jddown[i] = jdtrans + dt[i];
          }
          else {
             jdup[i] = 0.; jddown[i] = 0.;
             if(min_alt < critical_alt[i]) dt[i] = 0.;
             else dt[i] = 12.;
          }
    
          // let's just hope twilight occurs for now ...
 
          if(jdup[i] != 0.) 
             // passes the relevant airmass
             retvals[i] = String.format(Locale.ENGLISH, "%4.1f",hrs_up(jdup[i],jddown[i],ng.eveningTwilight.when.jd,
                               ng.morningTwilight.when.jd));
             // always remains above the relevant altitude
          else if (min_alt > critical_alt[i]) 
             retvals[i] = String.format(Locale.ENGLISH, "%4.1f",
                      24. * (ng.morningTwilight.when.jd - ng.eveningTwilight.when.jd));
             // never rises above the relevant altitude
          else retvals[i] = String.format(Locale.ENGLISH, "0.0");   

      }
   
      return retvals;

   }
}

class filewriter {
 // a little class to make it easy to open and close an outfile.
   File outfile;
   PrintWriter pw = null;
   FileWriter fw = null;

   filewriter(String fname) {
      outfile = new File(fname);
      fw = null;
      try {
         fw = new FileWriter(outfile,true);
      } catch(IOException e) { 
         System.out.printf("File writer didn't open for %s.\n",fname);
      }
      pw = new PrintWriter(fw);
      
   }
   
   void closer() {
      try {
         fw.close();      
      } catch(IOException e) { 
         System.out.printf("File writer didn't close.\n");
      }
   }
}


class FileGrabber extends JFrame {
// similar utility class for opening an infile, pops a file chooser.
    File infile;
    BufferedReader br = null;
    FileReader fr = null;
    
    FileGrabber() {
       JFileChooser chooser = new JFileChooser();
       int result = chooser.showOpenDialog(this);
       if(result != JFileChooser.CANCEL_OPTION) {
          try {
              infile = chooser.getSelectedFile();
              br = null;
              fr = new FileReader(infile);
          } catch (Exception e) {
              System.out.println("File opening error of some kind.");
          }
          if(fr != null)  br = new BufferedReader(fr);
       }
    }

    void closer() {
       try {
           fr.close();
       } catch(IOException e) {
           System.out.println("File reader didn't close."); 
       }
    }
}
 
class AstrObj {
   // simple class for storing info from coord files ... 
   String name;
   Celest c;

   // String comment;   // may be added later ... 
   // double pmx, pmy;   

   AstrObj(String instuff) {
      String [] fields;
      instuff = instuff.replace(":"," ");
      fields = instuff.split("\\s+");  // whitespace separated.
      name = fields[0];
      try {
         String raf = fields[1] + " " + fields[2] + " " + fields[3];
         String decf = fields[4] + " " + fields[5] + " " + fields[6];
       //  System.out.printf("raf %s decf %s\n",raf,decf);
         c = new Celest(raf,decf,fields[7]);
      } catch (Exception e) { 
         System.out.printf("Unconvertable input in AstrObj ... %s\n",instuff);
      }
   }   
   AstrObj(String sIn, Celest cIn) {
      name = sIn;
      c = cIn;
   }
}

class BrightStar {
   // A class for storing info from bright star list.  These are to be read in from
   // brightest.dat, which is a file of the 518 stars with m < 4.
   String name;
   Celest c;
   double m;   // apparent magnitude.
   Color col;  // color used for plotting the star, based on spectral type.
   
   BrightStar(String instuff) {
       // constructor reads from a file with lines like:
       // 3.158278  44.85722  3.80 " 27Kap Per"
      // try {
          String [] fields;
          String [] fields2;
       try {
          fields = instuff.split("\"");   // split out the quoted name 
          name = fields[1];
          fields[0] = fields[0].trim();
          fields2 = fields[0].split("\\s+");  // and the rest
          c = new Celest(Double.parseDouble(fields2[0]),Double.parseDouble(fields2[1]),2000.);
          m = Double.parseDouble(fields2[2]);
          col = new Color(Integer.parseInt(fields2[3]),Integer.parseInt(fields2[4]),
                         Integer.parseInt(fields2[5]));
       } catch (Exception e) 
          { System.out.printf("Unreadable line in bright star file input: %s\n",instuff); }
   }

   // time to rationalize the precession in the display map ... which means we need:
}       

public class JSkyCalc {
   public static void main(String [] args) {
   //   Locale locale = Locale.getDefault();  ... fixes the problems with German locale,
   //   Locale.setDefault(Locale.ENGLISH);    ... but, breaks the webstart!!

      try {
	    // Set cross-platform Java L&F (also called "Metal")
        UIManager.setLookAndFeel(
            UIManager.getCrossPlatformLookAndFeelClassName());
      } 
      catch (UnsupportedLookAndFeelException e) {
         System.out.printf("UnsupportedLookAndFeelException ... \n");
      }
      catch (ClassNotFoundException e) {
         System.out.printf("Class not found while trying to set look-and-feel ...\n");
      }
      catch (InstantiationException e) {
         System.out.printf("Instantiation exception while trying to set look-and-feel ...\n");
      }
      catch (IllegalAccessException e) {
         System.out.printf("Illegal access exception while trying to set look-and-feel ...\n");
      }
  
      JSkyCalcWindow MainWin = new JSkyCalcWindow() ;
   }
}

//class JSkyCalcWindow extends JPanel implements MouseListener  {
class JSkyCalcWindow extends JComponent {
/** This ENORMOUS class includes the entire user interface -- subwindows are
    implemented as subclasses. */
/*
   final UIManager.LookAndFeelInfo [] landfs  = UIManager.getInstalledLookAndFeels();
   final String className;
   className = landfs[2].getClassName();
   try  { 
     UIManager.setLookAndFeel(className);
   } catch catch (Exception e) { System.out.println(e); }
*/

/* I also tried to add a MouseListener to this, but it was ignored because the 
   SkyDisplay subclass pre-empts it.  Mouse events are always dispatched to the 
   "deepest" component in the hierarchy.  */

   WhenWhere w;
   InstantInTime i;
   Site s;
   Observation o; // oooh!
   Planets p; 

   PlanetWindow PlWin;
   boolean planetframevisible = false;

   SiteWindow siteframe;
   boolean siteframevisible = true;
 
   HourlyWindow HrWin;
   boolean hourlyframevisible = false;

   NightlyWindow NgWin;
   NightlyAlmanac Nightly;
   boolean nightlyframevisible = false;

   Seasonal season;
   SeasonalWindow SeasonWin;
   boolean seasonframevisible = false;  

   SkyDisplay SkyDisp;
   boolean skydisplayvisible = true;
   final JFrame SkyWin;

   AltCoordWin AltWin;
   boolean altcoowinvisible = false;

   HelpWindow HelpWin;
   boolean helpwindowvisible = false;

   AirmassDisplay AirDisp;
   boolean airmasswindowvisible = false;
   final JFrame AirWin;
  
   int sleepinterval = 2000;
   boolean autoupdaterunning = false;
   AutoUpdate au;
   boolean autosteprunning = false;
   AutoStep as;
   Thread thr;

   static AstrObj obj;
   AstrObjSelector ObjSelWin;
   boolean objselwinvisible = false;
   static String st = null;
   static HashMap<String, AstrObj> byname = new HashMap<String, AstrObj>();
   static HashMap<Double, AstrObj> byra = new HashMap<Double, AstrObj>(); 
   static Double rakey;
   static String [] RASelectors;  // real names, sorted by ra
   static String [] NameSelectors;  // real names, sorted by dec.
   static HashMap<String, AstrObj> presenterKey = new HashMap<String, AstrObj>();
   static HashMap<String, Site> siteDict = new HashMap<String, Site>();

   AstrObjSelector AirmSelWin;
   static Object [] airmassPlotSelections = {null};

   // littleHints hints;

   JTextField datefield;
   JTextField timefield;
   JTextField JDfield; 

   JTextField objnamefield;
   JTextField RAfield;
   JTextField decfield;
   JTextField equinoxfield;

   JTextField timestepfield;
   JTextField sleepfield;

   JTextField obsnamefield;
   JTextField longitudefield;
   JTextField latitudefield;
   JTextField stdzfield;
   JTextField use_dstfield;
   JTextField zonenamefield;
   JTextField elevseafield;
   JTextField elevhorizfield;
 
   JTextField siderealfield;
   JTextField HAfield;
   JTextField airmassfield;
   JTextField altazfield;
   JTextField parallacticfield;
   JTextField sunradecfield;
   JTextField sunaltazfield;
   JTextField ztwilightfield;
   JTextField moonphasefield;
   JTextField moonradecfield;
   JTextField moonaltazfield;
   JTextField illumfracfield;
   JTextField lunskyfield;
   JTextField moonobjangfield;
   JTextField baryjdfield;
   JTextField baryvcorfield;
   JTextField constelfield;
   JTextField planetproximfield;

   ButtonGroup UTbuttons;
   JRadioButton UTradiobutton;  // save references for swapper (later)
   JRadioButton Localradiobutton;

   ButtonGroup SiteButtons;

   Color outputcolor = new Color(230,230,230);  // light grey
   Color inputcolor = Color.WHITE;  // for input fields  
   Color lightblue = new Color(150,220,255);       
   Color lightpurple = new Color(221,136,255);
   Color sitecolor = new Color(255,215,215); // a little pink 
   Color panelcolor = new Color(185,190,190); // a little darker
   Color brightyellow = new Color(255,255,0);  // brighteryellow
   Color runningcolor = new Color(154,241,162);  // pale green, for autostep highlighting 
   Color objboxcolor = new Color(0,228,152);  // for object box on display 

   JSkyCalcWindow() {
      
      final JFrame frame = new JFrame("JSkyCalc");

      /* Put up site panel and grab the first site ... */

      siteframe = new SiteWindow();
      siteframevisible = true;
      siteframe.setVisible(siteframevisible);

//      String [] initialsite = {"Kitt Peak [MDM Obs.]",  "7.44111",  "31.9533",  
//           "7.",  "0",  "Mountain",  "M",  "1925",  "700."};

      s = siteframe.firstSite();
      i = new InstantInTime(s.stdz,s.use_dst);
      w = new WhenWhere(i,s);
      o = new Observation(w,w.zenith2000());
      o.ComputeSky();
      o.w.MakeLocalSun();  
      o.w.MakeLocalMoon();  
      o.ComputeSunMoon();
      p = new Planets(w);
      o.computebary(p);

      int iy = 0;

      /* make up the separate subpanels */

/* ****************** DO NOT REMOVE THIS BANNER ********************************* 
   ******************************************************************************

   I am distributing this COPYRIGHTED code freely with the condition that this
         credit banner will not be removed.  */
      JPanel bannerpanel = new JPanel();
      JLabel bannerlabel = new JLabel("JSkyCalc v1.1.1: John Thorstensen, Dartmouth College");
      Color dartmouthgreen = new Color(0,105,62);  // Official Dartmouth Green
      bannerpanel.setBackground(dartmouthgreen);
      bannerlabel.setBackground(dartmouthgreen);
      bannerlabel.setForeground(Color.WHITE);
      bannerpanel.add(bannerlabel);

/* ******************************************************************************
   ****************************************************************************** */

      /* Panel for text I/O fields and their labels ... */

      JPanel textpanel = new JPanel();
      textpanel.setLayout(new GridBagLayout());
      GridBagConstraints constraints = new GridBagConstraints();

      objnamefield = new JTextField(13);
      objnamefield.setToolTipText("If object list is loaded, you can select by name.");
      JLabel objnamelabel = new JLabel("Object: ",SwingConstants.RIGHT);

      RAfield = new JTextField(13);
      RAfield.setToolTipText("White fields accept input; Enter key synchs output.");
      JLabel RAlabel = new JLabel("RA: ",SwingConstants.RIGHT);

      decfield = new JTextField(13);
      JLabel declabel = new JLabel("dec: ",SwingConstants.RIGHT);

      equinoxfield = new JTextField(13);
      JLabel equinoxlabel = new JLabel("equinox: ",SwingConstants.RIGHT);

      datefield = new JTextField(13);
      JLabel datelabel = new JLabel("Date: ",SwingConstants.RIGHT);

      timefield = new JTextField(13);
      JLabel timelabel = new JLabel("Time: ",SwingConstants.RIGHT);

      JDfield = new JTextField(13);
      JDfield.setToolTipText("Hitting Enter in JD field forces time to JD.");
      JLabel JDlabel = new JLabel("JD: ",SwingConstants.RIGHT);

      timestepfield  = new JTextField(13);
      timestepfield.setText("1 h");
      timestepfield.setToolTipText("Units can be h, m, s, d, t (sid. day), l (lunation)");
      JLabel timesteplabel = new JLabel("timestep: ",SwingConstants.RIGHT);

      sleepfield  = new JTextField(13);
      sleepfield.setText("2");
      sleepfield.setToolTipText("Used in Auto Update and Auto Step");
      JLabel sleeplabel = new JLabel("sleep for (s): ",SwingConstants.RIGHT);

      obsnamefield = new JTextField(13);
      obsnamefield.setBackground(sitecolor);
      obsnamefield.setToolTipText("You must select 'Allow User Input' for sites not on menu");
      JLabel obsnamelabel = new JLabel("Site name: ",SwingConstants.RIGHT);

      longitudefield = new JTextField(13);
      longitudefield.setBackground(sitecolor);
      obsnamefield.setToolTipText("If these fields are pink, they're not accepting input.");
      JLabel longitudelabel = new JLabel("Longitude: ",SwingConstants.RIGHT);

      latitudefield = new JTextField(13);
      latitudefield.setBackground(sitecolor);
      JLabel latitudelabel = new JLabel("Latitude: ",SwingConstants.RIGHT);

      stdzfield = new JTextField(13);
      stdzfield.setBackground(sitecolor);
      JLabel stdzlabel = new JLabel("Time zone: ",SwingConstants.RIGHT);

      use_dstfield = new JTextField(13);
      use_dstfield.setBackground(sitecolor);
      JLabel use_dstlabel = new JLabel("DST code: ",SwingConstants.RIGHT);

      zonenamefield = new JTextField(13);
      zonenamefield.setBackground(sitecolor);
      JLabel zonenamelabel = new JLabel("Zone name: ",SwingConstants.RIGHT);

      elevseafield = new JTextField(13);
      elevseafield.setBackground(sitecolor);
      JLabel elevsealabel = new JLabel("Elevation: ",SwingConstants.RIGHT);

      elevhorizfield = new JTextField(13);
      elevhorizfield.setBackground(sitecolor);
      JLabel elevhorizlabel = new JLabel("Terrain elev: ",SwingConstants.RIGHT);

      siderealfield = new JTextField(16);
      siderealfield.setBackground(outputcolor);
      JLabel sidereallabel = new JLabel(" Sidereal ",SwingConstants.RIGHT);

      HAfield = new JTextField(16);
      HAfield.setBackground(outputcolor);
      JLabel HAlabel = new JLabel(" HA ",SwingConstants.RIGHT);

      airmassfield = new JTextField(16);
      airmassfield.setBackground(outputcolor);
      JLabel airmasslabel = new JLabel(" Airmass ",SwingConstants.RIGHT);

      altazfield = new JTextField(16);
      altazfield.setBackground(outputcolor);
      JLabel altazlabel = new JLabel(" AltAz ",SwingConstants.RIGHT);

      parallacticfield = new JTextField(16);
      parallacticfield.setBackground(outputcolor);
      JLabel parallacticlabel = new JLabel(" parallactic ",SwingConstants.RIGHT);
      
      sunradecfield = new JTextField(16);
      sunradecfield.setBackground(outputcolor);
      JLabel sunradeclabel = new JLabel(" SunRAdec ",SwingConstants.RIGHT);
      
      sunaltazfield = new JTextField(16);
      sunaltazfield.setBackground(outputcolor);
      JLabel sunaltazlabel = new JLabel(" SunAltAz ",SwingConstants.RIGHT);

      ztwilightfield = new JTextField(16);
      ztwilightfield.setBackground(outputcolor);
      JLabel ztwilightlabel = new JLabel(" ZTwilight",SwingConstants.RIGHT);
      
      moonphasefield = new JTextField(16);
      moonphasefield.setBackground(outputcolor);
      JLabel moonphaselabel = new JLabel(" MoonPhase ",SwingConstants.RIGHT);
      
      moonradecfield = new JTextField(16);
      moonradecfield.setBackground(outputcolor);
      JLabel moonradeclabel = new JLabel(" MoonRAdec ",SwingConstants.RIGHT);
      
      moonaltazfield = new JTextField(16);
      moonaltazfield.setBackground(outputcolor);
      JLabel moonaltazlabel = new JLabel(" MoonAltAz ",SwingConstants.RIGHT);
 
      illumfracfield = new JTextField(16);
      illumfracfield.setBackground(outputcolor);
      JLabel illumfraclabel = new JLabel(" MoonIllumFrac ",SwingConstants.RIGHT);

      lunskyfield = new JTextField(16);
      lunskyfield.setBackground(outputcolor);
      JLabel lunskylabel = new JLabel(" LunSkyBrght ",SwingConstants.RIGHT);

      moonobjangfield = new JTextField(16);
      moonobjangfield.setBackground(outputcolor);
      JLabel moonobjanglabel = new JLabel(" Moon-Obj ang. ",SwingConstants.RIGHT);

      baryjdfield = new JTextField(16);
      baryjdfield.setBackground(outputcolor);
      JLabel baryjdlabel = new JLabel(" Bary. JD ",SwingConstants.RIGHT);

      baryvcorfield = new JTextField(16);
      baryvcorfield.setBackground(outputcolor);
      JLabel baryvcorlabel = new JLabel(" Bary. Vcorrn. ",SwingConstants.RIGHT);

      constelfield = new JTextField(16);
      constelfield.setBackground(outputcolor);
      JLabel constellabel = new JLabel(" Constellation ",SwingConstants.RIGHT);

      planetproximfield = new JTextField(16);
      planetproximfield.setBackground(outputcolor);
      JLabel planetproximlabel = new JLabel("Planet Warning? ",SwingConstants.RIGHT);

      iy = 0; constraints.gridx = 0;  constraints.gridy = iy;
      textpanel.add(objnamelabel, constraints);
      constraints.gridx = 1;  constraints.gridy = iy;
      textpanel.add(objnamefield, constraints);
      objnamefield.setText("null");

      iy++; constraints.gridx = 0;  constraints.gridy = iy;
      textpanel.add(RAlabel, constraints);
      constraints.gridx = 1;  constraints.gridy = iy;
      textpanel.add(RAfield, constraints);

      iy++; constraints.gridx = 0;  constraints.gridy = iy;
      textpanel.add(declabel, constraints);
      constraints.gridx = 1;  constraints.gridy = iy;
      textpanel.add(decfield, constraints);

      iy++; constraints.gridx = 0;  constraints.gridy = iy;
      textpanel.add(equinoxlabel, constraints);
      constraints.gridx = 1;  constraints.gridy = iy;
      textpanel.add(equinoxfield, constraints);

      // Need to fill in values or synchOutput chokes later ...
      RAfield.setText(o.c.Alpha.RoundedRAString(2,":"));
      decfield.setText(o.c.Delta.RoundedDecString(1,":"));
      equinoxfield.setText(String.format(Locale.ENGLISH, "%7.2f",o.c.Equinox));

      iy++; constraints.gridx = 0;  constraints.gridy = iy;
      textpanel.add(datelabel, constraints);
      constraints.gridx = 1;  constraints.gridy = iy;
      textpanel.add(datefield, constraints);

      iy++; constraints.gridx = 0;  constraints.gridy = iy;
      textpanel.add(timelabel, constraints);
      constraints.gridx = 1;  constraints.gridy = iy;
      textpanel.add(timefield, constraints);

      JPanel buttonpan = new JPanel();   
      UTbuttons = new ButtonGroup();
      buttonpan.setBackground(panelcolor);

      buttonpan.add(Localradiobutton = new JRadioButton("Local",true));
      Localradiobutton.setActionCommand("Local");
      Localradiobutton.addActionListener(new ActionListener() {    // so it toggles time ... 
         public void actionPerformed(ActionEvent e) {
            setToJD();
         }
      });
      UTbuttons.add(Localradiobutton);

      buttonpan.add(UTradiobutton = new JRadioButton("UT",false));
      UTradiobutton.setActionCommand("UT");
      UTradiobutton.addActionListener(new ActionListener() {    // so it toggles time ... 
         public void actionPerformed(ActionEvent e) {
            setToJD();
         }
      });
      UTbuttons.add(UTradiobutton);
   
      JLabel buttonlabel = new JLabel("Time is:");
      iy++; constraints.gridx = 0;  constraints.gridy = iy;
      textpanel.add(buttonlabel,constraints);
      constraints.gridx = 1;  constraints.gridy = iy;
      textpanel.add(buttonpan,constraints);

      iy++; constraints.gridx = 0;  constraints.gridy = iy;
      textpanel.add(timesteplabel, constraints);
      constraints.gridx = 1;  constraints.gridy = iy;
      textpanel.add(timestepfield, constraints);

      iy++; constraints.gridx = 0;  constraints.gridy = iy;
      textpanel.add(sleeplabel, constraints);
      constraints.gridx = 1;  constraints.gridy = iy;
      textpanel.add(sleepfield, constraints);

      iy++; constraints.gridx = 0;  constraints.gridy = iy;
      textpanel.add(JDlabel, constraints);
      constraints.gridx = 1;  constraints.gridy = iy;
      textpanel.add(JDfield, constraints);
 
      iy++; constraints.gridx = 0;  constraints.gridy = iy;
      textpanel.add(obsnamelabel, constraints);
      constraints.gridx = 1;  constraints.gridy = iy;
      textpanel.add(obsnamefield, constraints);
 
      iy++; constraints.gridx = 0;  constraints.gridy = iy;
      textpanel.add(longitudelabel, constraints);
      constraints.gridx = 1;  constraints.gridy = iy;
      textpanel.add(longitudefield, constraints);
 
      iy++; constraints.gridx = 0;  constraints.gridy = iy;
      textpanel.add(latitudelabel, constraints);
      constraints.gridx = 1;  constraints.gridy = iy;
      textpanel.add(latitudefield, constraints);
 
      iy++; constraints.gridx = 0;  constraints.gridy = iy;
      textpanel.add(stdzlabel, constraints);
      constraints.gridx = 1;  constraints.gridy = iy;
      textpanel.add(stdzfield, constraints);
 
      iy++; constraints.gridx = 0;  constraints.gridy = iy;
      textpanel.add(use_dstlabel, constraints);
      constraints.gridx = 1;  constraints.gridy = iy;
      textpanel.add(use_dstfield, constraints);
 
      iy++; constraints.gridx = 0;  constraints.gridy = iy;
      textpanel.add(zonenamelabel, constraints);
      constraints.gridx = 1;  constraints.gridy = iy;
      textpanel.add(zonenamefield, constraints);
 
      iy++; constraints.gridx = 0;  constraints.gridy = iy;
      textpanel.add(elevsealabel, constraints);
      constraints.gridx = 1;  constraints.gridy = iy;
      textpanel.add(elevseafield, constraints);
  
      iy++; constraints.gridx = 0;  constraints.gridy = iy;
      textpanel.add(elevhorizlabel, constraints);
      constraints.gridx = 1;  constraints.gridy = iy;
      textpanel.add(elevhorizfield, constraints);
  
 
 // Right-hand column of output ... 

      iy = 0; constraints.gridx = 2;  constraints.gridy = iy;
      textpanel.add(sidereallabel, constraints);
      constraints.gridx = 3;  constraints.gridy = iy;
      textpanel.add(siderealfield, constraints);

      iy++; constraints.gridx = 2;  constraints.gridy = iy;
      textpanel.add(HAlabel, constraints);
      constraints.gridx = 3;  constraints.gridy = iy;
      textpanel.add(HAfield, constraints);

      iy++; constraints.gridx = 2;  constraints.gridy = iy;
      textpanel.add(airmasslabel, constraints);
      constraints.gridx = 3;  constraints.gridy = iy;
      textpanel.add(airmassfield, constraints);

      iy++; constraints.gridx = 2;  constraints.gridy = iy;
      textpanel.add(altazlabel, constraints);
      constraints.gridx = 3;  constraints.gridy = iy;
      textpanel.add(altazfield, constraints);

      iy++; constraints.gridx = 2;  constraints.gridy = iy;
      textpanel.add(parallacticlabel, constraints);
      constraints.gridx = 3;  constraints.gridy = iy;
      textpanel.add(parallacticfield, constraints);

      iy++; constraints.gridx = 2;  constraints.gridy = iy;
      textpanel.add(sunradeclabel, constraints);
      constraints.gridx = 3;  constraints.gridy = iy;
      textpanel.add(sunradecfield, constraints);

      iy++; constraints.gridx = 2;  constraints.gridy = iy;
      textpanel.add(sunaltazlabel, constraints);
      constraints.gridx = 3;  constraints.gridy = iy;
      textpanel.add(sunaltazfield, constraints);

      iy++; constraints.gridx = 2;  constraints.gridy = iy;
      textpanel.add(ztwilightlabel, constraints);
      constraints.gridx = 3;  constraints.gridy = iy;
      textpanel.add(ztwilightfield, constraints);

      iy++; constraints.gridx = 2;  constraints.gridy = iy;
      textpanel.add(moonphaselabel, constraints);
      constraints.gridx = 3;  constraints.gridy = iy;
      textpanel.add(moonphasefield, constraints);

      iy++; constraints.gridx = 2;  constraints.gridy = iy;
      textpanel.add(moonradeclabel, constraints);
      constraints.gridx = 3;  constraints.gridy = iy;
      textpanel.add(moonradecfield, constraints);

      iy++; constraints.gridx = 2;  constraints.gridy = iy;
      textpanel.add(moonaltazlabel, constraints);
      constraints.gridx = 3;  constraints.gridy = iy;
      textpanel.add(moonaltazfield, constraints);

      iy++; constraints.gridx = 2;  constraints.gridy = iy;
      textpanel.add(illumfraclabel, constraints);
      constraints.gridx = 3;  constraints.gridy = iy;
      textpanel.add(illumfracfield, constraints);

      iy++; constraints.gridx = 2;  constraints.gridy = iy;
      textpanel.add(lunskylabel, constraints);
      constraints.gridx = 3;  constraints.gridy = iy;
      textpanel.add(lunskyfield, constraints);

      iy++; constraints.gridx = 2;  constraints.gridy = iy;
      textpanel.add(moonobjanglabel, constraints);
      constraints.gridx = 3;  constraints.gridy = iy;
      textpanel.add(moonobjangfield, constraints);

      iy++; constraints.gridx = 2;  constraints.gridy = iy;
      textpanel.add(baryjdlabel, constraints);
      constraints.gridx = 3;  constraints.gridy = iy;
      textpanel.add(baryjdfield, constraints);

      iy++; constraints.gridx = 2;  constraints.gridy = iy;
      textpanel.add(baryvcorlabel, constraints);
      constraints.gridx = 3;  constraints.gridy = iy;
      textpanel.add(baryvcorfield, constraints);

      iy++; constraints.gridx = 2;  constraints.gridy = iy;
      textpanel.add(constellabel, constraints);
      constraints.gridx = 3;  constraints.gridy = iy;
      textpanel.add(constelfield, constraints);

      iy++; constraints.gridx = 2;  constraints.gridy = iy;
      textpanel.add(planetproximlabel, constraints);
      constraints.gridx = 3;  constraints.gridy = iy;
      textpanel.add(planetproximfield, constraints);

      textpanel.setBackground(panelcolor);

//      /* A panel for the site buttons, to be housed in a separate frame ... */
//
//      siteframe = new SiteWindow();
//      siteframevisible = true;
//      siteframe.setVisible(siteframevisible);

      /* A panel for planets, to be hidden */

      PlWin = new PlanetWindow();
      PlWin.setTitle("Low-Precision Planetary Positions");
      PlWin.setVisible(planetframevisible);

      /* A panel for the nightly almanac */

      Nightly = new NightlyAlmanac(o.w);
      NgWin = new NightlyWindow();
      NgWin.setTitle("Nightly Almanac");
      NgWin.setVisible(nightlyframevisible);

      /* A panel for hourly airmass */

      HrWin = new HourlyWindow();
      HrWin.setTitle("Hourly Circumstances");
      HrWin.Update();
      HrWin.setVisible(hourlyframevisible);  
   
      /* A panel for object lists */

      RASelectors = new String[1];
      RASelectors[0] = " "; // this needs to be initialized
      ObjSelWin = new AstrObjSelector(true);  // argument is "is_single"

      /* Sky display window .... */

      // int skywinxpix = 850; 
      int skywinxpix = 800; 
      int skywinypix = 700;
      SkyDisp = new SkyDisplay(skywinxpix, skywinypix);
      SkyWin = new JFrame("Sky Display");
 //     final JWindow SkyWin = new JWindow(); -- can't move it with the mouse.
      SkyWin.setSize(skywinxpix+15, skywinypix+35);  
      // add a bit to window size to account for JFrame borders. 
//      SkyWin.setSize(skywinxpix+15, skywinypix+75);  
      // and more to account for the top border in weblaunch ... 
      SkyWin.setLocation(50,300);
      SkyWin.add(SkyDisp);
      SkyWin.setVisible(true);

      /* A panel for alternate coords (e.g. current RA and dec, galactic etc.) */

      AltWin = new AltCoordWin();
      altcoowinvisible = false;

      /* A seasonal observability window ... */
      season = new Seasonal(o);
      SeasonWin = new SeasonalWindow();
      seasonframevisible = false;

      AirDisp = new AirmassDisplay(800,500);
      AirWin = new JFrame("Airmass Display");
      AirWin.setSize(800,530);
//      AirWin.setSize(800,560);
      AirWin.setLocation(400,200);
      AirWin.add(AirDisp);
      AirWin.setVisible(airmasswindowvisible);

      AirmSelWin = new AstrObjSelector(false);
      AirmSelWin.setVisible(airmasswindowvisible);

      HelpWin = new HelpWindow();
      helpwindowvisible = false;

      // control buttons go in a separate panel at the bottom ...

      JPanel controlbuttonpan = new JPanel();
      controlbuttonpan.setLayout(new FlowLayout());  // it's the default anyway
      Dimension controlbuttonpansize = new Dimension(540,170);  // ugh
      controlbuttonpan.setPreferredSize(controlbuttonpansize);

      JButton synchbutton = new JButton("Refresh output");
      synchbutton.addActionListener(new ActionListener() {
         public void actionPerformed(ActionEvent e) {
            setToDate();           
         }
      });
      controlbuttonpan.add(synchbutton);

      JButton nowbutton = new JButton("Set to Now");
      nowbutton.addActionListener(new ActionListener() {
         public void actionPerformed(ActionEvent e) {
            SetToNow();           
         }
      });
      controlbuttonpan.add(nowbutton);

      JButton forwardbutton = new JButton("Step Forward");
      forwardbutton.addActionListener(new ActionListener() {
         public void actionPerformed(ActionEvent e) {
            advanceTime();           
         }
      });
      controlbuttonpan.add(forwardbutton);
 
      JButton backbutton = new JButton("Step Back");
      backbutton.addActionListener(new ActionListener() {
         public void actionPerformed(ActionEvent e) {
            advanceTime(false);           
         }
      });
      controlbuttonpan.add(backbutton);

      final JButton updatebutton = new JButton("Auto Update");
      updatebutton.addActionListener(new ActionListener() {
         public void actionPerformed(ActionEvent e) {
            if (!autoupdaterunning) {
               sleepfield.setBackground(runningcolor);  // draw attention
               thr = new Thread(au);
               autoupdaterunning = true;
               thr.start();           
               updatebutton.setText("Stop Update");
               updatebutton.setBackground(Color.ORANGE);
            }
            else {
               autoupdaterunning = false;
               thr.interrupt();
               updatebutton.setText("Resume Update");
               sleepfield.setBackground(Color.WHITE);
               updatebutton.setBackground(Color.WHITE);  // not quite right 
            }
         }
      });
      controlbuttonpan.add(updatebutton);

      final JButton stepbutton = new JButton("Auto Step");
      stepbutton.addActionListener(new ActionListener() {
         public void actionPerformed(ActionEvent e) {
            if (!autosteprunning) {
               sleepfield.setBackground(runningcolor);  // draw attention
               timestepfield.setBackground(runningcolor);  // draw attention
               thr = new Thread(as);
               autosteprunning = true;
               thr.start();           
               stepbutton.setText("Stop Stepping");
               stepbutton.setBackground(Color.ORANGE);
            }
            else {
               autosteprunning = false;
               thr.interrupt();
               stepbutton.setText("Resume Stepping");
               sleepfield.setBackground(Color.WHITE);
               timestepfield.setBackground(Color.WHITE);
               stepbutton.setBackground(Color.WHITE);  // not quite right 
           }
         }
      });
      controlbuttonpan.add(stepbutton);

//  -- I can't figure out how to alter the UT/local radiobuttons from within
//     the program.  
//      JButton UTLocalbutton = new JButton("UT <-> Local");
//      UTLocalbutton.addActionListener(new ActionListener() {
//         public void actionPerformed(ActionEvent e) {
//            SwapUTLocal();           
//         }
//      });
//      controlbuttonpan.add(UTLocalbutton);

      JButton sitebutton = new JButton("Site\n Menu");
      sitebutton.addActionListener(new ActionListener() {
         public void actionPerformed(ActionEvent e) {
            if(siteframevisible)  {  // there's got to be a better way ..
               siteframe.setVisible(false);
               siteframevisible = false;
            }
            else {  // site frame is invisible
               siteframe.setVisible(true);
               siteframevisible = true;
            }
         }
      });
      controlbuttonpan.add(sitebutton);

      JButton planetbutton = new JButton("Planet Table");
      planetbutton.addActionListener(new ActionListener() {
         public void actionPerformed(ActionEvent e) {
            if(planetframevisible)  {  // there's got to be a better way ..
               PlWin.setVisible(false);
               planetframevisible = false;
            }
            else {  // planet frame is invisible
               PlWin.setVisible(true);
               PlWin.DisplayUpdate();   // refresh it ... 
               planetframevisible = true;
            }
         }
      });
      controlbuttonpan.add(planetbutton);

      JButton hourlybutton = new JButton("Hourly Circumstances");
      hourlybutton.addActionListener(new ActionListener() {
         public void actionPerformed(ActionEvent e) {
            if(hourlyframevisible)  {  // there's got to be a better way ..
               HrWin.setVisible(false);
               hourlyframevisible = false;
            }
            else {  // hourly frame is invisible
               HrWin.Update();
               HrWin.setVisible(true);
               hourlyframevisible = true;
            }
      //      hourlyframe.repaint();
         }
      });
      controlbuttonpan.add(hourlybutton);

      JButton nightlybutton = new JButton("Nightly Almanac");
      nightlybutton.addActionListener(new ActionListener() {
         public void actionPerformed(ActionEvent e) {
            if(nightlyframevisible)  {  // there's got to be a better way ..
               NgWin.setVisible(false);
               nightlyframevisible = false;
            }
            else {  // nightly frame is invisible
               Nightly.Update(o.w);
               NgWin.UpdateDisplay();
               NgWin.setVisible(true);
               nightlyframevisible = true;
            }
         }
      });
      controlbuttonpan.add(nightlybutton);

      JButton seasonalshow = new JButton("Seasonal Observability");
      seasonalshow.addActionListener(new ActionListener() {
         public void actionPerformed(ActionEvent e) {
             if(seasonframevisible) {
                SeasonWin.DisplayUpdate();
                SeasonWin.setVisible(false);
                seasonframevisible = false;
             }
             else {
                seasonframevisible = true;
                SeasonWin.setVisible(true);
             }
         }
      });
      controlbuttonpan.add(seasonalshow);

      JButton objselshow = new JButton("Object Lists ...");
      objselshow.addActionListener(new ActionListener() {
         public void actionPerformed(ActionEvent e) {
             objselwinvisible = true;
             ObjSelWin.setVisible(true);
         }
      });
      controlbuttonpan.add(objselshow);

      JButton skydisplayshow = new JButton("Sky Display");
      skydisplayshow.addActionListener(new ActionListener() {
         public void actionPerformed(ActionEvent e) {
             if(skydisplayvisible) {
                skydisplayvisible = false;
                SkyWin.setVisible(false);
             } 
             else {
                skydisplayvisible = true;
                SkyDisp.repaint();
                SkyWin.setVisible(true);
             }
         }
      });
      controlbuttonpan.add(skydisplayshow);

      JButton altwinshow = new JButton("Alt. Coordinates");
      altwinshow.addActionListener(new ActionListener() {
         public void actionPerformed(ActionEvent e) {
             if(altcoowinvisible) {
                altcoowinvisible = false;
                AltWin.setVisible(false);
             } 
             else {
                altcoowinvisible = true;
                AltWin.Refresh();
                AltWin.setVisible(true);
             }
         }
      });
      controlbuttonpan.add(altwinshow);

      JButton airmassshow = new JButton("Airmass Graphs");
      airmassshow.addActionListener(new ActionListener() {
         public void actionPerformed(ActionEvent e) {
            if(!airmasswindowvisible) {
               airmasswindowvisible = true;
               AirWin.setVisible(true);
               AirmSelWin.setVisible(true);
               synchOutput();
            }
            else {
               airmasswindowvisible = false;
               AirWin.setVisible(false);
               AirmSelWin.setVisible(false);
            } 
         }
      });

      controlbuttonpan.add(airmassshow);

      JButton stopper = new JButton("Quit");
      stopper.addActionListener(new ActionListener() {
         public void actionPerformed(ActionEvent e) {
            /* If you hate the confirm-exit, start killing lines here.... */
            int result = JOptionPane.showConfirmDialog(frame,
                "Really quit JSkyCalc?");
            switch(result) {
              case JOptionPane.YES_OPTION: 
                  System.exit(1);      // protected exit call ...
                  break;
              case JOptionPane.NO_OPTION:
              case JOptionPane.CANCEL_OPTION:
              case JOptionPane.CLOSED_OPTION:
                  break;
            }
            /* ... and stop killing them here, then uncomment the line below. */
            // System.exit(1);    // ... naked exit call.
         }
      });
      stopper.setBackground(sitecolor);  
      controlbuttonpan.add(stopper);

      JButton helpwinshow = new JButton("Help");
      helpwinshow.setBackground(runningcolor);
      helpwinshow.addActionListener(new ActionListener() {
         public void actionPerformed(ActionEvent e) {
             if(helpwindowvisible) {
                helpwindowvisible = false;
                HelpWin.setVisible(false);
             } 
             else {
                helpwindowvisible = true;
                HelpWin.setVisible(true);
             }
         }
      });

      controlbuttonpan.add(helpwinshow);
//
//      JButton airmassshow = new JButton("Airmass Graphs");
//      airmassshow.addActionListener(new ActionListener() {
//         public void actionPerformed(ActionEvent e) {
//            if(!airmasswindowvisible) {
//               airmasswindowvisible = true;
//               AirWin.setVisible(true);
//               AirmSelWin.setVisible(true);
//            }
//            else {
//               airmasswindowvisible = false;
//               AirWin.setVisible(false);
//               AirmSelWin.setVisible(false);
//            } 
//         }
//      });
//
//      controlbuttonpan.add(airmassshow);

      controlbuttonpan.setBackground(panelcolor);

      /* action listeners for the text fields ... */
      
      objnamefield.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
            try {
               String sel = objnamefield.getText();
               RAfield.setText(
                    presenterKey.get(sel).c.Alpha.RoundedRAString(3," "));
               decfield.setText(
                    presenterKey.get(sel).c.Delta.RoundedDecString(2," "));
               equinoxfield.setText(String.format(Locale.ENGLISH, "%7.2f",
                    presenterKey.get(sel).c.Equinox));
               synchOutput();
            } catch (Exception exc) {
               objnamefield.setText("Not Found.");
            } 
         }
      });

      RAfield.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
            setToDate();
        }
      });

      decfield.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
            setToDate();
        }
      });

      equinoxfield.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
            setToDate();
        }
      });
 
      datefield.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
            setToDate();
        }
      });

      timefield.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
            setToDate();
        }
      });

      JDfield.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
            setToJD();
        }
      });
 
      timestepfield.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
            advanceTime();
        }
      });

      // Actions on observatory parameters lead to a setToJD()
      // to parallel behavior of the site menu.

      obsnamefield.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
            setToJD();
        }
      });
 
      longitudefield.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
            setToJD();
        }
      });
 
      latitudefield.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
            setToJD();
        }
      });
 
      stdzfield.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
            setToJD();
        }
      });
 
      use_dstfield.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
            setToJD();
        }
      });
 
      zonenamefield.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
            setToJD();
        }
      });
 
      elevseafield.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
            setToJD();
        }
      });
 
      elevhorizfield.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
            setToJD();
        }
      });

      Container outer = frame.getContentPane();
      outer.setLayout(new FlowLayout());
      outer.add(bannerpanel);
      outer.add(textpanel);
      outer.add(controlbuttonpan);
      outer.setBackground(panelcolor);

      frame.setDefaultCloseOperation( JFrame.EXIT_ON_CLOSE );
      frame.setSize(540,590);
//      frame.setSize(560,620);
      frame.setLocation(30,30);
      frame.setContentPane(outer);
      frame.setVisible(true);

      synchSite();
      synchOutput();
      au = new AutoUpdate("x");   // instantiate for later start if desired
      as = new AutoStep("s");   
     
      // hints = new littleHints();

   }

   void synchOutput() {
      boolean is_ut;
      Color mooncolor;  // used for several fields
      double parallactic, altparallactic;

      o.c.UpdateFromStrings(RAfield.getText(),decfield.getText(),
          equinoxfield.getText());

      // special hook for equinox of date ... 
      if(o.c.Equinox < 0.)  {
         o.c.Equinox = o.w.when.JulianEpoch();
      }

      // and repeat them back ...      

      RAfield.setText(o.c.Alpha.RoundedRAString(2," "));
      decfield.setText(o.c.Delta.RoundedDecString(1," "));
      equinoxfield.setText(String.format(Locale.ENGLISH, "%7.2f",o.c.Equinox));

      o.ComputeSky();
      o.ComputeSunMoon();

      p.Update(o.w);
      if(planetframevisible) {
         PlWin.DisplayUpdate();
      }
      o.computebary(p);
      checkPlanets();
     
//      if(hourlyframevisible) {   // an expensive operation 
//         HrWin.Update();
//      }

      if(nightlyframevisible) {
         if(!hourlyframevisible) {
            Nightly.Update(o.w);
         }
         NgWin.UpdateDisplay();
      }

      if(seasonframevisible) {
         SeasonWin.DisplayUpdate();
      }

      HAfield.setText(o.ha.RoundedHAString(0," "));
      HAfield.setBackground(HAWarningColor(o.ha.value));

      if(o.altitude < 0.) airmassfield.setText("(down.)");
      else if (o.airmass > 10.) airmassfield.setText("> 10.");
      else airmassfield.setText(String.format(Locale.ENGLISH, "%6.3f",o.airmass));
      airmassfield.setBackground(AirMassWarningColor(o.altitude,o.airmass));

      altazfield.setText(String.format(Locale.ENGLISH, "%5.1f  az = %6.1f",o.altitude,o.azimuth));

      parallactic = o.parallactic;
      while(parallactic < -180.) parallactic += 360.;
      while(parallactic >= 180.) parallactic -= 360.;
      altparallactic = parallactic + 180.;
      while(altparallactic < -180.) altparallactic += 360.;
      while(altparallactic >= 180.) altparallactic -= 360.;

      parallacticfield.setText(String.format(Locale.ENGLISH, "%5.1f  [%5.1f] degr.",parallactic,
        altparallactic));
   
      if(o.w.altsun > 0.) 
         ztwilightfield.setText("(Daytime.)"); 
      else if(o.w.altsun < -18.) 
         ztwilightfield.setText("No twilight.");
      else {
         ztwilightfield.setText(String.format(Locale.ENGLISH, "%5.1f mag (blue)",o.w.twilight));
      }
      ztwilightfield.setBackground(TwilightWarningColor(o.w.altsun,o.w.twilight));

      mooncolor = MoonWarningColor(o.w.altmoon,o.altitude,o.w.altsun,o.moonobj,o.moonlight);
      if (o.w.altmoon < -2.) lunskyfield.setText("Moon is down.");
      else if (o.altitude < 0.) lunskyfield.setText("Target is down.");
      else if(o.w.altsun < -12.) {
         lunskyfield.setText(String.format(Locale.ENGLISH, "%5.1f V mag/sq arcsec",o.moonlight));
      }
      else if (o.w.altsun < 0.) lunskyfield.setText("(Bright twilight.)");
      else lunskyfield.setText("(Daytime.)");
      lunskyfield.setBackground(mooncolor);

      baryjdfield.setText(String.format(Locale.ENGLISH, "%13.5f  [%6.1f s]",
            o.baryjd,o.barytcor));
      baryvcorfield.setText(String.format(Locale.ENGLISH, "%6.2f km/s",o.baryvcor));
      constelfield.setText(Constel.getconstel(o.c));

      String UTstring = UTbuttons.getSelection().getActionCommand();
      if (UTstring.equals("UT")) is_ut = true;
      else is_ut = false;      // easy binary choice
      
      if(is_ut) { 
         datefield.setText(o.w.when.UTDate.RoundedCalString(6,0));
         timefield.setText(o.w.when.UTDate.RoundedCalString(11,1));
      }
      else { 
         datefield.setText(o.w.when.localDate.RoundedCalString(6,0));
         timefield.setText(o.w.when.localDate.RoundedCalString(11,1));
      }
      JDfield.setText(String.format(Locale.ENGLISH, "%15.6f",o.w.when.jd));
      // w.siderealobj.setRA(w.sidereal);  // update
      siderealfield.setText(o.w.siderealobj.RoundedRAString(0," "));
      sunradecfield.setText(o.w.sun.topopos.shortstring());
      moonradecfield.setText(o.w.moon.topopos.shortstring());
      sunaltazfield.setText(String.format(Locale.ENGLISH, "%5.1f   az = %6.1f",o.w.altsun,o.w.azsun));
      moonaltazfield.setText(String.format(Locale.ENGLISH, "%5.1f   az = %6.1f",o.w.altmoon,o.w.azmoon));
      moonphasefield.setText(String.format(Locale.ENGLISH, "%s",o.w.moon.MoonPhaseDescr(o.w.when.jd))); 
      illumfracfield.setText(String.format(Locale.ENGLISH, "%5.3f",o.w.moonillum));
      moonobjangfield.setText(String.format(Locale.ENGLISH, "%5.1f deg",o.moonobj));
      moonobjangfield.setBackground(mooncolor);

      if(altcoowinvisible) {
         AltWin.Refresh();
      }

      if(skydisplayvisible) {
         SkyDisp.repaint();
      }     

      if(hourlyframevisible) {   // an expensive operation 
         HrWin.Update();
      }

      if(airmasswindowvisible) {
         if(!hourlyframevisible) {
            Nightly.Update(o.w);
         }
         AirDisp.Update();
      }

      season.Update(o);

   }

   void synchSite() {

      String SiteString = SiteButtons.getSelection().getActionCommand();
      if( SiteString.equals("x") ) {   // code for load from fields ... 
          o.w.where.name = obsnamefield.getText();
          o.w.where.lat.setDec(latitudefield.getText());
          o.w.where.longit.setFromString(longitudefield.getText());
          o.w.where.stdz = Double.parseDouble(stdzfield.getText());
          o.w.where.use_dst = Integer.parseInt(use_dstfield.getText());
          o.w.where.timezone_name = zonenamefield.getText();
          String [] fields = elevseafield.getText().trim().split("\\s+");  // chuck unit
          o.w.where.elevsea = Double.parseDouble(fields[0]);
          fields = elevhorizfield.getText().trim().split("\\s+");  // chuck unit
          // System.out.printf("About to try to parse %s ... \n",fields[0]);
          double elevhoriz_in = Double.parseDouble(fields[0]);
          if (elevhoriz_in >= 0.)  o.w.where.elevhoriz = Double.parseDouble(fields[0]);
          else {
              System.out.printf("Negative elev_horiz causes a square root error later.  Set to zero.\n");
              o.w.where.elevhoriz = 0.;
          }
      }
      else {
          o.w.ChangeSite(siteDict, SiteString);
      }
      // and spit them all back out ... 
      obsnamefield.setText(o.w.where.name);
      longitudefield.setText(o.w.where.longit.RoundedLongitString(1," ",false));
      latitudefield.setText(o.w.where.lat.RoundedDecString(0," "));
      stdzfield.setText(String.format(Locale.ENGLISH, "%5.2f",o.w.where.stdz));
      use_dstfield.setText(String.format(Locale.ENGLISH, "%d",o.w.where.use_dst));
      zonenamefield.setText(o.w.where.timezone_name);
      elevseafield.setText(String.format(Locale.ENGLISH, "%4.0f m",o.w.where.elevsea));
      elevhorizfield.setText(String.format(Locale.ENGLISH, "%4.0f m",o.w.where.elevhoriz));
   }

   Color MoonWarningColor(double altmoon,  
           double altitude, double altsun, double moonobj, double moonlight) {
      if(altmoon < 0. | altitude < 0.) return outputcolor;
      if(altsun > -12.) return outputcolor;  // twilight dominates
      if(moonobj > 25.) {   // not proximity 
         if(moonlight > 21.5) return outputcolor;
         else if (moonlight > 19.5) {
            // System.out.printf("lightblue moon \n");
            return lightblue;
         }
         else {
              // System.out.printf("lightpurple moon\n");   
              return lightpurple;
         }
      }
      if(moonobj < 10.) return Color.RED;  // always flag < 10 deg
      if(moonlight > 21.5) return outputcolor;
      if(moonlight > 19.5) {
         // System.out.printf("brightyellow moon \n");
         return brightyellow;
      }
      if(moonlight > 18.) return Color.ORANGE;
      return Color.RED;
   }

   Color AirMassWarningColor(double altitude,double airmass) {
      if(altitude < 0.) return Color.RED;
      if(airmass < 2.) return outputcolor;
      if(airmass > 4.) return Color.RED;
      if(airmass > 3.) return Color.ORANGE;
      return brightyellow;
   }

   Color TwilightWarningColor(double altsun, double twilight) {
      if(altsun < -18.) return outputcolor;
      if(altsun > 0.) return lightblue;
      if(twilight < 3.5) return brightyellow;
      if(twilight < 8.) return Color.ORANGE;
      return Color.RED;
   }   

   Color HAWarningColor(Double haval) {
      if(Math.abs(haval) > 6.) return Color.ORANGE;
      else return outputcolor;
   }

   void SetToNow() {
 
      // Get site and UT options
      synchSite();
   
      o.w.SetToNow();
      o.w.ComputeSunMoon();
      synchOutput();
   }

   class AutoUpdate implements Runnable {
      String mystr;
      AutoUpdate(String s) {
         this.mystr = s;   // constructor does basically nothing ...
      }
      public void run() {
         while(autoupdaterunning) {
            SetToNow();     
            sleepinterval = 1000 * Integer.parseInt(sleepfield.getText());
            try {  
              Thread.sleep(sleepinterval);
            } catch( InterruptedException e ) {
               System.out.println("Auto-update interrupted.");
            }
         }
      }
   }

   class AutoStep implements Runnable {
      String mystr;
      AutoStep(String s) {
         this.mystr = s;   // constructor does basically nothing ...
      }
      public void run() {
         while(autosteprunning) {
            advanceTime();     
            sleepinterval = 1000 * Integer.parseInt(sleepfield.getText());
            try {  
              Thread.sleep(sleepinterval);
            } catch( InterruptedException e ) {
               System.out.println("Auto-step interrupted.");
            }
         }
      }
   }

//   void SwapUTLocal() {
//      boolean is_ut;
// 
//      // Get site and UT options
//      String SiteString = SiteButtons.getSelection().getActionCommand();
//      w.ChangeSite(SiteString);
// 
//      System.out.printf("swapping ... ");
//   
//      String UTstring = UTbuttons.getSelection().getActionCommand();
//      if (UTstring.equals("UT")) {
//          System.out.printf("UT was selected ... \n");
//          //UTbuttons.setSelected("UT",false);
//          //UTbuttons.setSelected("Local",true);
//          Localradiobutton.setSelected(true);
//          UTradiobutton.setSelected(false);
//          Localradiobutton.repaint();
//    //     UTradiobutton.setSelected(false);
//    //     UTbuttons.setSelectedJRadioButton(Localradiobutton,true);
//          is_ut = false;
//      }
//      else { 
//          System.out.printf("Local was selected ... \n");
//     //     UTbuttons.setSelected("UT",true);
//          UTradiobutton.setSelected(true);
//          Localradiobutton.setSelected(false);
//          Localradiobutton.repaint();
//     //     Localradiobutton.setSelected(false);
//     //     UTradiobutton.setSelected(true);
//     //     UTbuttons.setSelectedJRadioButton(UTradiobutton,true);
//          is_ut = true;
//      }
//      System.out.println("ut " + UTradiobutton.isSelected());
//      setToDate();
//   }
 
   void advanceTime() {
    
      synchSite();
   
      String advanceString = timestepfield.getText();   
      o.w.AdvanceWhen(advanceString);
      o.w.ComputeSunMoon();
      
      synchOutput();

   }
 
   void advanceTime(boolean forward) {
    
      synchSite();
   
      String advanceString = timestepfield.getText();   
      o.w.AdvanceWhen(advanceString, forward);
      o.w.ComputeSunMoon();
      
      synchOutput();
   }

   void setToDate() {

      String dateTimeString;
      boolean is_ut;
 
      // Get site and UT options
      //String SiteString = SiteButtons.getSelection().getActionCommand();
      //o.w.ChangeSite(SiteString);
      synchSite();
   
      String UTstring = UTbuttons.getSelection().getActionCommand();
      if (UTstring.equals("UT")) is_ut = true;
      else is_ut = false;      // easy binary choice

      // grab date and time from input fields
      String dateString = datefield.getText();
      String [] datepieces = dateString.split("\\s+");
      // take first three fields in date -- to ignore day of week 
      dateTimeString = String.format(Locale.ENGLISH, "%s %s %s %s",datepieces[0],datepieces[1],
             datepieces[2],timefield.getText());

      // System.out.printf("%s %s %s\n",dateTimeString,UTstring,is_ut);
      // set the actual time ...
      o.w.ChangeWhen(dateTimeString, is_ut); 
      // and update the display with the new values.
      o.w.ComputeSunMoon();
      synchOutput();
  }
  
  void setToJD() {
      
      double jd;
      boolean is_ut;

      // Get site and UT options, and set them ... 
      //String SiteString = SiteButtons.getSelection().getActionCommand();
      //o.w.ChangeSite(SiteString);
      synchSite();
 
      String UTstring = UTbuttons.getSelection().getActionCommand();
      if (UTstring.equals("UT")) is_ut = true;
      else is_ut = false;      // easy binary choice
      
      // grab the jd from the display
      jd = Double.parseDouble(JDfield.getText());

      // System.out.printf("jd = %f\n",jd);
      // change the time using current options
      o.w.ChangeWhen(jd); 
      o.w.ComputeSunMoon();
      synchOutput();
  }

  class PlanetWindow extends JFrame {
      // a subclass
      String [] headings; 
      Object[][] PlanetDispData;
      JTable ptable;
      double [] PlanObjAng;
   
      PlanetWindow() {
          int i = 0;
          int j = 0;

          headings = new String [] {"Name","RA","Dec","HA","airmass","proximity"};
          JPanel container = new JPanel();
          p.Update(o.w); 
          PlanObjAng = new double [9];

          PlanetDispData = new Object[8][6];
          for(i = 0; i < 9; i++) {
            if(i != 2) {  // skip earth
                PlanetDispData[j][0] = p.names[i];
                PlanetDispData[j][1] = p.PlanetObs[i].c.Alpha.RoundedRAString(-1,":");
                PlanetDispData[j][2] = p.PlanetObs[i].c.Delta.RoundedDecString(-2,":");
                PlanetDispData[j][3] = p.PlanetObs[i].ha.RoundedHAString(-2,":");
                if(p.PlanetObs[i].altitude < 0.) PlanetDispData[j][4] = "(Down.)";
                else if(p.PlanetObs[i].airmass > 10.) PlanetDispData[j][4] = "> 10.";
                else PlanetDispData[j][4] = String.format(Locale.ENGLISH, "%5.2f",
                      p.PlanetObs[i].airmass);
                PlanObjAng[i] = Const.DEG_IN_RADIAN * Spherical.subtend(o.c,p.PlanetObs[i].c);
                PlanetDispData[j][5] = String.format(Locale.ENGLISH, "%6.2f",PlanObjAng[i]);
                j = j + 1;
             }
         }        
     
          ptable = new JTable(PlanetDispData, headings); 
          JScrollPane scroller= new JScrollPane(ptable);
          ptable.setPreferredScrollableViewportSize(new Dimension(470,130));
          container.add(scroller);
          
          JButton hider = new JButton("Hide Planet Table"); 
          hider.addActionListener(new ActionListener() {
          public void actionPerformed(ActionEvent e) {
               setVisible(false);
               planetframevisible = false;
            }
          }); 
          container.add(hider);
          
          JButton printer = new JButton("Print Planet Table"); 
          printer.addActionListener(new ActionListener() {
          public void actionPerformed(ActionEvent e) {
               Print();  // JTable has this automated utility to do this ... 
            }
          }); 
          container.add(printer);


          this.setSize(490,220);
//          this.setSize(520,260);
          this.setLocation(400,200);
          this.setContentPane(container);
      }      

      void DisplayUpdate() {  // assumes computations are already done 
         int i = 0;
         int j = 0;
         p.Update(o.w);
         for(i = 0; i < 9; i++) {
            if(i != 2) {  // skip earth
                ptable.setValueAt(p.names[i],j,0);
                ptable.setValueAt(p.PlanetObs[i].c.Alpha.RoundedRAString(-1,":"),j,1);
                ptable.setValueAt(p.PlanetObs[i].c.Delta.RoundedDecString(-2,":"),j,2);
                ptable.setValueAt(p.PlanetObs[i].ha.RoundedHAString(-2,":"),j,3);
                if(p.PlanetObs[i].altitude < 0.) ptable.setValueAt("(Down.)",j,4);
                else if(p.PlanetObs[i].airmass > 10.) ptable.setValueAt("> 10.",j,4);
                else ptable.setValueAt(String.format(Locale.ENGLISH, "%5.2f",p.PlanetObs[i].airmass),j,4);;
                PlanObjAng[i] = Const.DEG_IN_RADIAN * Spherical.subtend(o.c,p.PlanetObs[i].c);
                ptable.setValueAt(String.format(Locale.ENGLISH, "%6.2f",PlanObjAng[i]),j,5);
                j = j + 1;
             }
         }        
     }
     
     void Print() {
        try {
           ptable.print();
        } catch (PrinterException e) {
           System.out.printf("Printer exception caught.\n");
        }
     }
  }

  class SeasonalWindow extends JFrame {
      // a subclass
      String [] headings; 
      // Object[][] SeasonalDispData;
      JTable seasontable;
   
      SeasonalWindow() {
          int i = 0;
          int j = 0;

          headings = new String [] {"Moon","Evening Date","HA.eve","airm.eve","HA.ctr","airm.ctr",
                   "HA.morn","airm.morn","hrs<3","hrs<2","hrs<1.5"};
          JPanel container = new JPanel();
          season.Update(o); 

//          SeasonalDispData = new Object[16][11];
//          for(i = 0; i < 9; i++) {
//            if(i != 2) {  // skip earth
//                PlanetDispData[j][0] = p.names[i];
//                PlanetDispData[j][1] = p.PlanetObs[i].c.Alpha.RoundedRAString(-1,":");
//                PlanetDispData[j][2] = p.PlanetObs[i].c.Delta.RoundedDecString(-2,":");
//                PlanetDispData[j][3] = p.PlanetObs[i].ha.RoundedHAString(-2,":");
//                if(p.PlanetObs[i].altitude < 0.) PlanetDispData[j][4] = "(Down.)";
//                else if(p.PlanetObs[i].airmass > 10.) PlanetDispData[j][4] = "> 10.";
//                else PlanetDispData[j][4] = String.format(Locale.ENGLISH, "%5.2f",
//                      p.PlanetObs[i].airmass);
//                PlanObjAng[i] = Const.DEG_IN_RADIAN * Spherical.subtend(o.c,p.PlanetObs[i].c);
//                PlanetDispData[j][5] = String.format(Locale.ENGLISH, "%6.2f",PlanObjAng[i]);
//                j = j + 1;
//             }
//         }        
//     
          seasontable = new JTable(season.tabledata, headings); 

   // complicated rigamarole to set column widths 

          seasontable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
          TableColumn col = seasontable.getColumnModel().getColumn(0);
          col.setPreferredWidth(40);  // pixels
          col = seasontable.getColumnModel().getColumn(1);
          col.setPreferredWidth(105);  // pixels
          int colno;
          for(colno = 2; colno < 11; colno++) {
             col = seasontable.getColumnModel().getColumn(colno);
             col.setPreferredWidth(60);
          } 

          seasontable.setPreferredScrollableViewportSize(new Dimension(690,270));
          JScrollPane scroller = new JScrollPane(seasontable);
          container.add(scroller);
          
          JButton hider = new JButton("Hide Observability Table"); 
          hider.addActionListener(new ActionListener() {
          public void actionPerformed(ActionEvent e) {
               setVisible(false);
               seasonframevisible = false;
            }
          }); 
          container.add(hider);
          
          JButton printer = new JButton("Print Observability Table"); 
          printer.addActionListener(new ActionListener() {
          public void actionPerformed(ActionEvent e) {
               Print();  // JTable has this automated utility to do this ... 
            }
          }); 
          container.add(printer);


          this.setSize(710,370);
//          this.setSize(730,420);
          this.setLocation(400,200);
          this.setContentPane(container);
          setVisible(seasonframevisible);
      }      

      void DisplayUpdate() {  // assumes computations are already done 
         int i = 0;
         int j = 0;
         season.Update(o); // this doesn't update unless it's warranted.
         if(season.wasupdated) {  // don't bother refreshing table unless the update happened.
            for(i = 0; i < 16; i++) {
               for(j = 0; j < 11; j++) {
                   seasontable.setValueAt(season.tabledata[i][j],i,j);
               }
            }        
        }
     }
     
     void Print() {
        try {
           seasontable.print();
        } catch (PrinterException e) {
           System.out.printf("Printer exception caught.\n");
        }
     }
  }


  class HourlyWindow extends JFrame {
  /** I'd hoped to use the JTable class for this, but the java developers
  in their infinite wisdom do not provide any handy way of changing the color
  of an individual cell.  So I will go with an array of entry boxes, which 
  should be much easier to handle (though getting it to print will involve
  more labor). */
    
     JTextField [][] hrfield;

     HourlyWindow() {
        int i = 0;
        int j = 0;

        JPanel container = new JPanel();

        JPanel tablepanel = new JPanel();
        tablepanel.setLayout(new GridBagLayout());
        GridBagConstraints tableconstraints = new GridBagConstraints();

        JLabel [] headers = new JLabel[7];
        headers[0] = new JLabel("Local");
        headers[1] = new JLabel("UT");
        headers[2] = new JLabel("LST");
        headers[3] = new JLabel("HA");
        headers[4] = new JLabel("Airmass");
        headers[5] = new JLabel("moonalt");
        headers[6] = new JLabel("sunalt");
        tableconstraints.gridy = 0;
        for(j = 0; j < 7; j ++) {
            tableconstraints.gridx = j;
            tablepanel.add(headers[j],tableconstraints);
            headers[j].setForeground(new Color(100,100,200));
        }

        hrfield = new JTextField[18][7];
        for(i = 0; i < 18; i++) {
           for(j = 0; j < 7; j++) {
              // System.out.printf("i %d j %d\n",i,j);
              if(j == 0) hrfield[i][j] = new JTextField(11);
              else hrfield[i][j] = new JTextField(5);
              tableconstraints.gridx = j;  tableconstraints.gridy = i+1;
              tablepanel.add(hrfield[i][j], tableconstraints);
              hrfield[i][j].setBackground(outputcolor);
           }
        }

        JButton hider = new JButton("Hide Hourly Table"); 
        hider.addActionListener(new ActionListener() {
           public void actionPerformed(ActionEvent e) {
              setVisible(false);
              hourlyframevisible = false;
           }
        }); 

        JButton printer = new JButton("Dump to 'jskycalc.out'"); 
        printer.addActionListener(new ActionListener() {
           public void actionPerformed(ActionEvent e) {
              DumpToFile();
           }
        }); 

        container.add(tablepanel);
        container.add(hider);
        container.add(printer);
        this.setSize(495,440);
//        this.setSize(510,480);
        this.setLocation(400,150);
        this.setContentPane(container);
     }

     void Update() {
        int i, j, starthr, endhr, jdint; 
        double jd, jdtemp;

        Color nodatacolor = new Color(100,100,100);

        Observation otmp = (Observation) o.clone();
        
        // compute nightly almanac ; start with first whole hour before
        // sunset, end with first whole hour after sunrise. 
        
        // Had a horrible time debugging this because all the WhenWhere 
        // instances in Nightly were pointing at the same When!!
        // They do all point to the same Where, which is actually OK.
        
        Nightly.Update(otmp.w);
         
        jdtemp = Nightly.sunset.when.jd;
        jdint = (int) jdtemp;
        starthr = (int) (24. * (jdtemp - jdint)); 
        
        jdtemp = Nightly.sunrise.when.jd + 1./24.;  // round up
        endhr = (int) (24. * (jdtemp - jdint));
        
/*        System.out.printf("\nTabulation start and end:");
        otmp.w.ChangeWhen((double) jdint + (double) starthr / 24.);
        otmp.w.when.localDate.quickprint();
        System.out.printf("\n end:");
        otmp.w.ChangeWhen((double) jdint + (double) endhr / 24.);
        otmp.w.when.localDate.quickprint();
        System.out.printf("\n"); */

        i = starthr; j = 0;   // start with first row of table
        while (i <= endhr) { 
           otmp.w.ChangeWhen((double) jdint + (double) i / 24.);
           otmp.ComputeSky();
           otmp.ComputeSunMoon();
           hrfield[j][0].setText(otmp.w.when.localDate.RoundedCalString(2,0));
           hrfield[j][0].setBackground(outputcolor);
           hrfield[j][1].setText(otmp.w.when.UTDate.RoundedCalString(12,0));
           hrfield[j][1].setBackground(outputcolor);
           RA tempsid = new RA(otmp.w.sidereal);
           hrfield[j][2].setText(tempsid.RoundedRAString(-2,":"));
           hrfield[j][2].setBackground(outputcolor);
           hrfield[j][3].setText(otmp.ha.RoundedHAString(-2,":"));
           hrfield[j][3].setBackground(HAWarningColor(otmp.ha.value));
           // System.out.printf("HAstr %s\n",otmp.ha.RoundedHAString(-2,":"));
           if(otmp.altitude < 0.) hrfield[j][4].setText("(Down.)");
           else if(otmp.airmass >= 10.) hrfield[j][4].setText("> 10.");
           else hrfield[j][4].setText(String.format(Locale.ENGLISH, "%5.2f",otmp.airmass));
           hrfield[j][4].setBackground(AirMassWarningColor(otmp.altitude, otmp.airmass));
           if(otmp.w.altmoon < -3.) hrfield[j][5].setText("---");
           else hrfield[j][5].setText(String.format(Locale.ENGLISH, "%5.1f",otmp.w.altmoon));
           hrfield[j][5].setBackground(MoonWarningColor(otmp.w.altmoon,otmp.altitude,
              otmp.w.altsun,otmp.moonobj,otmp.moonlight));
           if(otmp.w.altsun < -18.) hrfield[j][6].setText("---");
           else hrfield[j][6].setText(String.format(Locale.ENGLISH, "%5.1f",
                       otmp.w.altsun));
           hrfield[j][6].setBackground(TwilightWarningColor(otmp.w.altsun,otmp.w.twilight));
           i++;
           j++;
        }
        while (j < 18) {
           for(i =  0; i < 7; i++) {
              hrfield[j][i].setText(" ");
              hrfield[j][i].setBackground(nodatacolor);
           }
           j++;
        }
     }

     void DumpToFile() {
         int i = 0;
         int j = 0;
         filewriter f = new filewriter("jskycalc.out");
         //f.pw.printf("Boo!  (not implemented yet.)\n");
         f.pw.printf("\n\n** Hourly circumstances for NoNameYet ** \n\n");
         f.pw.printf("Coordinates: %s\n",o.c.checkstring());
         f.pw.printf("       Site: %s     (Year %d)\n\n",o.w.where.name,
              o.w.when.localDate.year);
         f.pw.printf("  --- Local ---  ");
         f.pw.printf("      UT     LST      HA  airmass  moonAlt sunAlt \n\n");
         for(i = 0; i < 18; i++) {
            for(j = 0; j < 7; j++) {
              if (j == 0) f.pw.printf("%14s  ",hrfield[i][j].getText());
              else f.pw.printf("%7s ",hrfield[i][j].getText());
            }
            f.pw.printf("\n");
         }
         f.closer();
     }
  }

   class NightlyWindow extends JFrame {
      /** Displays phenomena (sunset, moonrise, etc) */

      JTextField [] phenfield ;  
      JLabel [] phenlabel;
      String [] labeltext = {"Sunset","Twilight Ends","LST Eve. Twi.","Night Center","Twilight Begins",
        "LST Morn. Twi.","Sunrise","Moonrise","Moonset"};

      NightlyWindow() {
         int i;

         JPanel container = new JPanel();

         JPanel tablepanel = new JPanel();
         tablepanel.setLayout(new GridBagLayout());
         GridBagConstraints tableconstraints = new GridBagConstraints();

	 phenfield = new JTextField [9];
         phenlabel = new JLabel [9];
         for (i = 0; i < 9; i++) {
            tableconstraints.gridy = i;  
            tableconstraints.gridx = 0;
            phenlabel[i] = new JLabel(labeltext[i]);
            tablepanel.add(phenlabel[i],tableconstraints);
            tableconstraints.gridx = 1;
            phenfield[i] = new JTextField(11);
            tablepanel.add(phenfield[i],tableconstraints);
         }

         JButton hider = new JButton("Hide Nightly Almanac"); 
         hider.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                setVisible(false);
                nightlyframevisible = false;
             }
         }); 
         container.add(tablepanel);
         container.add(hider);
         this.setSize(250,240);
//         this.setSize(280,270);
         this.setLocation(400,100);
         this.setContentPane(container);
      }

      void UpdateDisplay() {  // assumes Nightly has been updated separately.
         
         phenfield[0].setText(Nightly.sunset.when.localDate.RoundedCalString(2,0));
         phenfield[1].setText(Nightly.eveningTwilight.when.localDate.RoundedCalString(2,0));
         phenfield[2].setText(Nightly.eveningTwilight.siderealobj.RoundedRAString(-2," "));
         phenfield[3].setText(Nightly.nightcenter.when.localDate.RoundedCalString(2,0));
         phenfield[4].setText(Nightly.morningTwilight.when.localDate.RoundedCalString(2,0));
         phenfield[5].setText(Nightly.morningTwilight.siderealobj.RoundedRAString(-2," "));
         phenfield[6].setText(Nightly.sunrise.when.localDate.RoundedCalString(2,0));

         if(Nightly.moonrise.when.jd < Nightly.moonset.when.jd) {
            phenfield[7].setText(Nightly.moonrise.when.localDate.RoundedCalString(2,0));
            phenlabel[7].setText("Moonrise");
            phenfield[8].setText(Nightly.moonset.when.localDate.RoundedCalString(2,0));
            phenlabel[8].setText("Moonset");
         }
         else {
            phenfield[7].setText(Nightly.moonset.when.localDate.RoundedCalString(2,0));
            phenlabel[7].setText("Moonset");
            phenfield[8].setText(Nightly.moonrise.when.localDate.RoundedCalString(2,0));
            phenlabel[8].setText("Moonrise");
         }
      }
   }

   class AstrObjSelector extends JFrame {

       JList selectorList; 

       AstrObjSelector(boolean is_single) {
            JPanel outer = new JPanel();
            selectorList = new JList(RASelectors);

            if (is_single) {
               selectorList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
               this.setTitle("Object Selector");
            }
            else {
               selectorList.setSelectionMode(
                  ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
               this.setTitle("Airmass Graph Sel.");
               outer.setBackground(new Color(100,100,100));  // color this differently 
            }

            selectorList.setPrototypeCellValue("xxxxxxxxxxxxxxxxxxxx");
            outer.add(new JScrollPane(selectorList));
            super.add(outer);

            JButton objselbutton = new JButton("Load Object List");
            objselbutton.addActionListener(new ActionListener() {
               public void actionPerformed(ActionEvent e) {
                   LoadAstrObjs();
                   SkyDisp.repaint();   // cause them to appear on display.
               }
            });
            outer.add(objselbutton);
 
            JButton byra = new JButton("Sort by RA");
            byra.addActionListener(new ActionListener() {
               public void actionPerformed(ActionEvent e) {
                  selectorList.setListData(RASelectors);
               }
            });
            outer.add(byra); 

            JButton byname = new JButton("Alphabetical Order");
            byname.addActionListener(new ActionListener() {
               public void actionPerformed(ActionEvent e) {
                  selectorList.setListData(NameSelectors);
               //   for(int i = 0; i < NameSelectors.length; i++) 
                //    System.out.printf("%s\n",NameSelectors[i]);
               }
            });
            outer.add(byname); 
 
            JButton clearbutton = new JButton("Clear list");
            clearbutton.addActionListener(new ActionListener() {
               public void actionPerformed(ActionEvent e) {
                  ClearAstrObjs();
                  SkyDisp.repaint();
               }
            });
            outer.add(clearbutton); 

            if(!is_single) {

               JButton plotairmasses = new JButton("Plot airmasses");
               plotairmasses.addActionListener(new ActionListener() {
                  public void actionPerformed(ActionEvent e) {
                     airmassPlotSelections =  selectorList.getSelectedValues();
                     System.out.printf("%d objects selected\n",airmassPlotSelections.length);
                     synchOutput();
                  }
               });
               outer.add(plotairmasses);

               JButton deselector = new JButton("Deselect all");
               deselector.addActionListener(new ActionListener() {
                  public void actionPerformed(ActionEvent e) {
                     selectorList.clearSelection();
                  }
               });
               outer.add(deselector);
            }

            JButton hider = new JButton("Hide Window");
            hider.addActionListener(new ActionListener() {
               public void actionPerformed(ActionEvent e) {
                  setVisible(false);
                  objselwinvisible = false;
               }
            }); 
            outer.add(hider);

            this.add(outer);
//            this.setSize(180,340);
            // leave more room for the airmass selector ... 
            if(is_single) this.setSize(210,340);
            else this.setSize(210,390);
            if(is_single) this.setLocation(575,100);
            else this.setLocation(700,50);

            if(is_single) {
               selectorList.addListSelectionListener(new ListSelectionListener() {
                   public void valueChanged(ListSelectionEvent l) {
                      /* Changing the list fires a selection event that can 
                         generate bad data.  Catch the resulting exceptions. */
                      try {   
                         String sel = (String) selectorList.getSelectedValues()[0];
      /*                    System.out.printf("%d %s %s %s\n",i,
                           Selectors[i],
                          presenterKey.get(Selectors[i]).name,
                          presenterKey.get(Selectors[i]).c.checkstring()  
                          ); */
                         RAfield.setText(
                           presenterKey.get(sel).c.Alpha.RoundedRAString(3," "));
                         decfield.setText(
                           presenterKey.get(sel).c.Delta.RoundedDecString(2," "));
                         equinoxfield.setText(String.format(Locale.ENGLISH, "%7.2f",
                           presenterKey.get(sel).c.Equinox));
                         synchOutput();
                      } catch (ArrayIndexOutOfBoundsException e) {}
                   }
               });
            }
       }

       void LoadList() {
           selectorList.setListData(RASelectors);
       }
   }

   void LoadAstrObjs() {
       
       FileGrabber ff = new FileGrabber();
      
       if(ff == null) {
          System.out.printf("No objects loaded.\n");
          return;
       }
      
       try { 
          while((st = ff.br.readLine()) != null) {
              obj = new AstrObj(st);
              if (obj.name != null & obj.c != null) {
                  byname.put(obj.name.toLowerCase(),obj);
                  rakey = (Double) obj.c.Alpha.value;
                  // ensure unique RA keys by inserting small tie-breaker offset
                  while(byra.keySet().contains(rakey)) rakey = rakey + 0.00001;
                  byra.put(rakey,obj);
              }
          } 
       } catch (IOException e) { System.out.println(e); }
 
       ff.closer();
 
       java.util.List<String> namekeys = new ArrayList<String>(byname.keySet());
       java.util.List<Double> rakeys = new ArrayList<Double>(byra.keySet());
 
       Collections.sort(namekeys);
       Collections.sort(rakeys);
 
       RASelectors = new String[rakeys.size()];
 
       Iterator raiterator = rakeys.iterator();
       int i = 0;
       while(raiterator.hasNext()) {
          Object key =  raiterator.next();
          AstrObj tempobj = byra.get(key);
          presenterKey.put(tempobj.name,tempobj);
          RASelectors[i] = tempobj.name;
          i++;
       }
  
       NameSelectors = new String[namekeys.size()];

       Iterator nameiterator = namekeys.iterator();
       i = 0;
       while(nameiterator.hasNext()) {
          Object key =  nameiterator.next();
          AstrObj tempobj = byname.get(key);
          // presenterkey is alreary loaded ... but the NameSelector array will
          // be in alphabetical order.
          NameSelectors[i] = tempobj.name;
          i++;
       }
       // AstrObjSelector objsel = new AstrObjSelector();
       ObjSelWin.LoadList();
       AirmSelWin.LoadList();
   }

   void ClearAstrObjs() {
       
       NameSelectors = new String[1];
       NameSelectors[0] = "null";
       RASelectors = new String[1];
       RASelectors[0] =   "null";
 
       java.util.List<String> namekeys = new ArrayList<String>(byname.keySet());
       java.util.List<Double> rakeys = new ArrayList<Double>(byra.keySet());

       Iterator raiterator = rakeys.iterator();
       while(raiterator.hasNext()) {
          Object key =  raiterator.next();
          AstrObj tempobj = byra.get(key);
          byra.remove(key);
          presenterKey.remove(tempobj.name);
       }
  
       Iterator nameiterator = namekeys.iterator();
       while(nameiterator.hasNext()) {
          Object key =  nameiterator.next();
          byname.remove(key);
       }

       Celest nullc = new Celest(0.,0.,2000.);
       AstrObj nullobj = new AstrObj("null",nullc);
       byra.put(0., nullobj);
       byname.put("null", nullobj);
       presenterKey.put("null", nullobj);

       // AstrObjSelector objsel = new AstrObjSelector();
       ObjSelWin.LoadList();   // which is now empty.
       AirmSelWin.LoadList();   
   }

   void SelObjByPos(Celest incel) {  
    /* get input from the graphical display, or wherever, and get the nearest
       object on the list within a tolerance.  Precession is ignored. */
       double tolerance = 0.1;  // radians
       double decband = 6.;     // degrees
       double decin;
       Celest objcel;
       double sep, minsep = 1000000000000.;
       int i, minindex = 0;

       if(presenterKey.size() > 0) {
          decin = incel.Delta.value;
          for(i = 0; i < RASelectors.length; i++) {
              objcel = presenterKey.get(RASelectors[i]).c;
              if(Math.abs(decin - objcel.Delta.value) < decband) {  // guard expensive subtend
                  sep = Spherical.subtend(incel,objcel);
                  if (sep < minsep) {
                      minsep = sep;
                      minindex = i;
                  }
             }
          }
       
          if(minsep < tolerance) {
              objcel = presenterKey.get(RASelectors[minindex]).c;
              objnamefield.setText(RASelectors[minindex]);
              RAfield.setText(objcel.Alpha.RoundedRAString(2," "));
              decfield.setText(objcel.Delta.RoundedDecString(1," "));
              equinoxfield.setText(String.format(Locale.ENGLISH, "%7.2f",objcel.Equinox));
          }
      }
   }

//   class littleHints extends JWindow {
//
//      final JTextArea area;
//
//      littleHints() {
//         area = new JTextArea();
//         area.append("Hints: ");   
//         area.append("- White fields are for input data\n");   
//         area.append("- Output refreshes with 'Enter' in input\n");   
//         area.append("- Control buttons are in bottom panel \n");   
//         area.append("- Some pop extra windows (e.g. hourly)\n");   
//         area.append("- To enter your own site params, you must first\n");   
//         area.append("  select 'Allow User Input' on site menu. \n");   
//         area.append("- Sky Display has several useful keyboard  \n");   
//         area.append("  and mouse controls - menu on right button.\n");
//         area.setBackground(brightyellow);
//         this.setSize(275,150);
//         this.add(area);
//      }
//     
//      void showme(int x, int y) {
//         this.setLocation(x, y);
//         this.setVisible(true);
//      }
//      void hideme(int x, int y) {
//         this.setVisible(false);
//      }
//   }

   class SiteWindow extends JFrame {
      // shoves this large block of code into its own subclass.

      String [] sitekeys;
      int nsites;

      SiteWindow() {
         int iy = 0;
         int i = 0;

         ReadSites();    // see below.
   
         JPanel sitepan = new JPanel();
         sitepan.setLayout(new GridBagLayout());
         GridBagConstraints constr = new GridBagConstraints();
         SiteButtons = new ButtonGroup();
         JRadioButton radioButton;  // need a generic one here
         
         constr.anchor = GridBagConstraints.LINE_START;

         constr.gridx = 0; constr.gridy = iy;
         sitepan.add(radioButton = new JRadioButton("Enable User Input",false),constr);
         radioButton.setActionCommand("x");  // we'll wait before refreshing.  
         radioButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                ColorUserInput(true);  // change site to input color
            }
         });
 
         SiteButtons.add(radioButton);

         for(i = 0; i < nsites; i++) {
            String name = sitekeys[i];
   
            iy++; constr.gridx = 0; constr.gridy = iy;
            sitepan.add(radioButton = new JRadioButton(name,true),constr);
            radioButton.setActionCommand(name);
            radioButton.addActionListener(new ActionListener() {  // ugly to do it to all...
            public void actionPerformed(ActionEvent e) {
                   setToJD();
                   ColorUserInput(false);
               }
            });
            SiteButtons.add(radioButton);
         }
   
         iy++; constr.gridx = 0; constr.gridy = iy;
         JButton sitehider = new JButton("Hide Site Chooser");
         sitehider.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                setVisible(false);
                siteframevisible = false;
            }
         }); 
         sitepan.add(sitehider,constr);
   
         Container sitecontainer = this.getContentPane();
         sitecontainer.add(sitepan);
         this.setSize(180,580);
//         this.setSize(210,640);
         // this.pack();
         this.setLocation(585,20);
         this.setContentPane(sitepan);
      }

      Site firstSite() {
         // just pops out first site on list for initilization ... 
         return siteDict.get(sitekeys[0]);
      }

      void ReadSites() {

         BufferedReader br = null;
         boolean inquote = false;
         String [] fields = new String[14];  // too many but that's ok
         int i,j;

         sitekeys = new String[40];         // hard-coded, sorry
         nsites = 0;

         try {
             ClassLoader cl = this.getClass().getClassLoader();
             InputStream is = cl.getResourceAsStream("skycalcsites.dat");
             br = new BufferedReader(new InputStreamReader(is));
         } catch (Exception e) {
             System.out.printf("Problem opening skycalcsites.dat for input.\n");
         }
       
         // read the site info character-by-character to preserve quoted values.
         // there's undoubtedly a better way, but this works well enough.

         try {
            while((st = br.readLine()) != null) {
               // System.out.printf("read: %s\n",st);
               if(st.length() > 0) {
                  if(st.charAt(0) != '#') {
                     j = 0;   // field counter
                     fields[j] = "";
                     for(i = 0; i < st.length(); i++) {
                        char [] thischar = {st.charAt(i)};
                        if(st.charAt(i) == '"') {
                            if(inquote) inquote = false;
                            else inquote = true;
                        }
                        else {
                           if(inquote) fields[j] = fields[j] + new String(thischar);
                           else {
                              if (st.charAt(i) == ',') { 
                                 j = j + 1;
                                 fields[j] = "";
                              }
                              else fields[j] = fields[j] + new String(thischar);
                           }
                        }
                     }
                     siteDict.put(fields[0],new Site(fields));
                     sitekeys[nsites] = fields[0];  // so they'll come out in order ... 
                     nsites++;
      //               for(j = 0; j < fields.length; j++) System.out.printf("%s ",fields[j]);
       //              System.out.printf("\n");
                  }
               }
            }
         } catch (IOException e) {System.out.printf("IO exception\n");}
      }

      void ColorUserInput(boolean allowed) {
        /* sets the background color in all the site param boxes according to whether
           user input is allowed or not. */
         if(allowed) {
            obsnamefield.setBackground(inputcolor);
            longitudefield.setBackground(inputcolor);
            latitudefield.setBackground(inputcolor);
            stdzfield.setBackground(inputcolor);
            use_dstfield.setBackground(inputcolor);
            zonenamefield.setBackground(inputcolor);
            elevseafield.setBackground(inputcolor);
            elevhorizfield.setBackground(inputcolor);
         }
         else {
            obsnamefield.setBackground(sitecolor);
            longitudefield.setBackground(sitecolor);
            latitudefield.setBackground(sitecolor);
            stdzfield.setBackground(sitecolor);
            use_dstfield.setBackground(sitecolor);
            zonenamefield.setBackground(sitecolor);
            elevseafield.setBackground(sitecolor);
            elevhorizfield.setBackground(sitecolor);
         }
      }
   } 
  
   class AltCoordWin extends JFrame {
      // shoves this large block of code into its own subclass.

      JTextField currentrafield;
      JTextField currentdecfield;
      JTextField currenteqfield;
      JTextField galactlongitfield;
      JTextField galactlatitfield;
      JTextField ecliptlatitfield;
      JTextField ecliptlongitfield;

      AltCoordWin() {

        JPanel coopan = new JPanel();
        coopan.setLayout(new GridBagLayout());
        GridBagConstraints constr = new GridBagConstraints();
        ButtonGroup PMButtons = new ButtonGroup();
        JRadioButton radioButton; // generic 

        int iy = 0;
        int fw = 13;

        currentrafield = new JTextField(fw);
        JLabel currentralabel = new JLabel("Current RA: ",SwingConstants.RIGHT);
        constr.gridx = 0; constr.gridy = iy; 
        coopan.add(currentralabel,constr);
        constr.gridx = 1; constr.gridy = iy; 
        coopan.add(currentrafield,constr);
        currentrafield.setBackground(outputcolor);

        currentdecfield = new JTextField(fw);;
        JLabel currentdeclabel = new JLabel("Current dec: ",SwingConstants.RIGHT);
        iy++; constr.gridx = 0; constr.gridy = iy; 
        coopan.add(currentdeclabel,constr);
        constr.gridx = 1; constr.gridy = iy; 
        coopan.add(currentdecfield,constr);
        currentdecfield.setBackground(outputcolor);

        currenteqfield = new JTextField(fw);;
        JLabel currenteqlabel = new JLabel("Current Equinox: ",SwingConstants.RIGHT);
        iy++; constr.gridx = 0; constr.gridy = iy; 
        coopan.add(currenteqlabel,constr);
        constr.gridx = 1; constr.gridy = iy; 
        coopan.add(currenteqfield,constr);
        currenteqfield.setBackground(outputcolor);

        galactlongitfield = new JTextField(fw);;
        JLabel galactlongitlabel = new JLabel("Galactic longit: ",SwingConstants.RIGHT);
        iy++; constr.gridx = 0; constr.gridy = iy; 
        coopan.add(galactlongitlabel,constr);
        constr.gridx = 1; constr.gridy = iy; 
        coopan.add(galactlongitfield,constr);
        galactlongitfield.setBackground(inputcolor);

        galactlatitfield = new JTextField(fw);;
        JLabel galactlatitlabel = new JLabel("Galactic latit: ",SwingConstants.RIGHT);
        iy++; constr.gridx = 0; constr.gridy = iy; 
        coopan.add(galactlatitlabel,constr);
        constr.gridx = 1; constr.gridy = iy; 
        coopan.add(galactlatitfield,constr);
        galactlatitfield.setBackground(inputcolor);

        galactlongitfield.addActionListener(new ActionListener() {
          public void actionPerformed(ActionEvent e) {
              double galactlongit = Double.parseDouble(galactlongitfield.getText());
              double galactlatit = Double.parseDouble(galactlatitfield.getText());
              Celest ctmp = Spherical.Gal2Cel(galactlongit, galactlatit);
              // get equinox from main window 
              double eq = Double.parseDouble(equinoxfield.getText());
              ctmp.selfprecess(eq);
              RAfield.setText(ctmp.Alpha.RoundedRAString(2," "));
              decfield.setText(ctmp.Delta.RoundedDecString(1," "));
              setToDate();
          }
        });
   
        galactlatitfield.addActionListener(new ActionListener() {
          public void actionPerformed(ActionEvent e) {
              double galactlongit = Double.parseDouble(galactlongitfield.getText());
              double galactlatit = Double.parseDouble(galactlatitfield.getText());
              // System.out.printf("read %f %f from long/lat fields\n",galactlongit,galactlatit);
              Celest ctmp = Spherical.Gal2Cel(galactlongit, galactlatit);
              // get equinox from main window 
              double eq = Double.parseDouble(equinoxfield.getText());
              ctmp.selfprecess(eq);
              RAfield.setText(ctmp.Alpha.RoundedRAString(2," "));
              decfield.setText(ctmp.Delta.RoundedDecString(1," "));
              setToDate();
          }
        });

        ecliptlongitfield = new JTextField(fw);;
        JLabel ecliptlongitlabel = new JLabel("Ecliptic longit.: ",SwingConstants.RIGHT);
        iy++; constr.gridx = 0; constr.gridy = iy; 
        coopan.add(ecliptlongitlabel,constr);
        constr.gridx = 1; constr.gridy = iy; 
        coopan.add(ecliptlongitfield,constr);
        ecliptlongitfield.setBackground(outputcolor);

        ecliptlatitfield = new JTextField(fw);;
        JLabel ecliptlatitlabel = new JLabel("Ecliptic latit: ",SwingConstants.RIGHT);
        iy++; constr.gridx = 0; constr.gridy = iy; 
        coopan.add(ecliptlatitlabel,constr);
        constr.gridx = 1; constr.gridy = iy; 
        coopan.add(ecliptlatitfield,constr);
        ecliptlatitfield.setBackground(outputcolor);

        this.setSize(330,200);
//        this.setSize(350,250);
        this.setLocation(400,150);
        this.setContentPane(coopan);
      } 
      
      void Refresh() {
        // to be called at the end of a synchOutput so site etc are all done.

        currentrafield.setText(o.current.Alpha.RoundedRAString(2," "));
        currentdecfield.setText(o.current.Delta.RoundedDecString(1," "));
        currenteqfield.setText(String.format(Locale.ENGLISH, "%7.2f",o.current.Equinox));
        o.c.galactic();
        galactlongitfield.setText(String.format(Locale.ENGLISH, "%6.2f",o.c.galong));
        galactlatitfield.setText(String.format(Locale.ENGLISH, "%6.2f",o.c.galat));
        double [] eclonglat = Ecliptic.Cel2Ecl(o);
        ecliptlongitfield.setText(String.format(Locale.ENGLISH, "%6.2f",eclonglat[0]));
        ecliptlatitfield.setText(String.format(Locale.ENGLISH, "%6.2f",eclonglat[1]));
      } 
   }

   void checkPlanets() {
      int i;
      double sepn;

      String warningtext = ""; 
      int warninglevel = 0;  // save worst warning ... 0 none, 1 orange, 2 red.

      for(i = 0; i < 9; i++) {
         if (i != 2) {  // skip earth ....
            sepn = Spherical.subtend(o.c, p.PlanetObs[i].c) * Const.DEG_IN_RADIAN;
            if(sepn < 3.) {
               if(i > 0 & i < 6) {  // Venus through Saturn
                  warningtext = warningtext + String.format(Locale.ENGLISH, "%s - %4.2f deg ",p.names[i],sepn);
                  if(sepn < 1. ) {
                     warninglevel = 2;
                  }   
                  else if (warninglevel < 2) warninglevel = 1;
               }
               else {   // the rest of the planets
                  if(sepn < 1.) {
                     warningtext = warningtext + String.format(Locale.ENGLISH, "%s - %4.2f deg ",p.names[i],sepn);
                     if (warninglevel < 1) warninglevel = 1;
                  }
               }
            }
         }
      }
      if (warningtext.equals("")) {
         planetproximfield.setText(" --- ");
         planetproximfield.setBackground(outputcolor);
      }
      else {
         planetproximfield.setText(warningtext);
         if (warninglevel == 1) planetproximfield.setBackground(Color.ORANGE);
         else planetproximfield.setBackground(Color.RED);
      }
   }

   class HelpWindow extends JFrame {
      HelpWindow() {

         File infile = null;
         FileReader fr = null;
 
         JPanel container = new JPanel();
         //container.setLayout(new GridBagLayout());
         //GridBagConstraints constraints = new GridBagConstraints();
         // container.setLayout(new BorderLayout());

         JEditorPane pane = null;
	 ClassLoader cl = this.getClass().getClassLoader();
         InputStream is = cl.getResourceAsStream("helptext.html");
        // try {
       //      infile = new File("helptext.html");
       //      fr = new FileReader(infile);
      //   } catch (Exception e) {System.out.printf("Input reader didn't open right\n"); }
       
         try {      
             pane = new JEditorPane();
             pane.setEditorKit(new HTMLEditorKit());
            // pane.read(fr,"HTMLDocument");
	    pane.read(is,"HTMLDocument");
         }
         catch (Exception e) { 
             System.out.printf("Manual text didn't open.\n");
         }
         pane.setEditable(false);
         JScrollPane scroller = new JScrollPane(pane);
         scroller.setPreferredSize(new Dimension(680,640));
         // container.add(scroller,BorderLayout.NORTH);
         container.add(scroller);

         JButton hideme = new JButton("Hide Help Text");
         hideme.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                setVisible(false);
                helpwindowvisible = false;
            }
         }); 
         // container.add(hideme,BorderLayout.SOUTH);
         container.add(hideme);
         add(container);

         setSize(700,740);
//         setSize(720,780);
         setLocation(400,10);
      }
   }

   class SkyDisplay extends JComponent  
                implements   MouseMotionListener, 
                KeyListener,  MouseListener 
   {
      
       int xpixint, ypixint;       // number of pixels in x and y directions, int
       double xpix, ypix, aspect;  // number of pixels in x and y directions, double
       double halfwidthy;          // sets the scale factor.
       double halfwidthx;
       double pixperunit;
       double xmid, ymid;          // x, y coords of the center of the screen.
       double halfwidthxfull, halfwidthyfull, pixperunitfull;
       Graphics2D g2;
       Font smallfont;
       FontMetrics smallfontmetrics;
       Font mediumfont;
       FontMetrics mediumfontmetrics;
       Font largefont;
       FontMetrics largefontmetrics;
       BrightStar [] bs;
       double currentlat;       // keep track of this to avoid recomputing the HAgrid
       GeneralPath [] HAgrid;   // polylines that draw out hour angle paths and equator
       Color gridcolor;
       MouseEvent mousevent;
       KeyEvent keyevent;
       double zoomedby;
       tinyHelp tinyhelp;

       SkyDisplay(int xpixIn, int ypixIn) {
           xpixint = xpixIn;
           ypixint = ypixIn;
           xpix = (double) xpixint;
           ypix = (double) ypixint;
           aspect = xpix / ypix;
           xmid = 0.;  ymid = 0.; 
           halfwidthy = 0.88; 
           halfwidthx = halfwidthy * aspect;
           pixperunit = ypix / (2. * halfwidthy);
        
           halfwidthxfull = halfwidthx;  halfwidthyfull = halfwidthy; pixperunitfull = pixperunit;
           zoomedby = 1.;
/*
           System.out.printf("xpix %f ypix %f aspect %f\n",xpix,ypix,aspect);
           System.out.printf("halfwidthx %f halfwidthy %f\n",halfwidthx, halfwidthy);
           System.out.printf("pixperunit %f\n",pixperunit);
*/

           Graphics g;

           smallfont = new Font("Dialog", Font.PLAIN, 11);
           mediumfont = new Font("Dialog",Font.PLAIN, 15);
           largefont = new Font("Dialog",Font.PLAIN, 18);
           gridcolor = new Color(153,0,0);  // dark red

           LoadBright();
   
           currentlat = o.w.where.lat.value;
           makeHAgrid(currentlat);   // computes it, doesn't plot it.  

           setFocusable(true);
           addKeyListener(this);
           addMouseMotionListener(this);
           addMouseListener(this);
           tinyhelp = new tinyHelp(); 
       }

       public void mousePressed(MouseEvent e) {
            Point parentloc = SkyWin.getLocation();
            if(e.getButton() == MouseEvent.BUTTON3) {
                tinyhelp.show((int) parentloc.getX() + e.getX(),
                              (int) parentloc.getY() + e.getY());
            }
       }

       public void mouseReleased(MouseEvent e) {
            if(e.getButton() == MouseEvent.BUTTON3) {
                tinyhelp.hide(e.getX(),e.getY());
            }
       }

       public void mouseClicked(MouseEvent e) {
            mousevent = e;
            if(mousevent.getButton() == MouseEvent.BUTTON1) {   // select from list
//              System.out.printf("%d %d\n",mousevent.getX(),mousevent.getY());
               Celest markedC = pixtocelest(mousevent.getX(),mousevent.getY());
               SelObjByPos(markedC);
               synchOutput();
            }
            else if (mousevent.getButton() == MouseEvent.BUTTON2) {  // middle sets to coords
//               System.out.printf("%d %d\n",mousevent.getX(),mousevent.getY());
               Celest markedC = pixtocelest(mousevent.getX(),mousevent.getY()); 
               RAfield.setText(markedC.Alpha.RoundedRAString(0," "));
               decfield.setText(markedC.Delta.RoundedDecString(0," "));
               synchOutput();
            }
       }
  
       public void mouseExited(MouseEvent e) {
       }

       public void mouseEntered(MouseEvent e) {
           this.requestFocusInWindow();
       }

       public void mouseDragged(MouseEvent e) { }  // not used for anything.
      
       public void mouseMoved(MouseEvent e) {    // keeps track of cursor location.
           mousevent = e;
           this.requestFocusInWindow();
//            System.out.printf("Mouse moved %d %d\n",mousevent.getX(),mousevent.getY());
//            if(this.isFocusOwner()) System.out.printf("We have focus.\n");
//            else System.out.printf("We Don't have focus.\n");
      // Focus appears flaky.  When window border is pink, kb input is accepted.
       }

       public void keyReleased(KeyEvent k) { 
          // we don't care about these but need empty methods for the interface.
       }
       public void keyPressed(KeyEvent k) { 
       }

// This has proven a little awkward under fvwm2 because focus doesn't follow mouse very
// faithfully.  But once you have focus it all works perfectly!
       public void keyTyped(KeyEvent k) { 
           keyevent = k;
//           System.out.printf("Key typed at coords %d %d\n",mousevent.getX(),
//              mousevent.getY());    // yup, when we have focus, it works.
           if(k.getKeyChar() == 'f') advanceTime();
           else if(k.getKeyChar() == 'b') advanceTime(false);
           else if(k.getKeyChar() == 'c') {
           //   System.out.printf("%d %d\n",mousevent.getX(),mousevent.getY());
              Celest markedC = pixtocelest(mousevent.getX(),mousevent.getY());
              RAfield.setText(markedC.Alpha.RoundedRAString(0," "));
              decfield.setText(markedC.Delta.RoundedDecString(0," "));
              synchOutput();
           }
           else if(k.getKeyChar() == 's') {
           //   System.out.printf("%d %d\n",mousevent.getX(),mousevent.getY());
              Celest markedC = pixtocelest(mousevent.getX(),mousevent.getY());
              SelObjByPos(markedC);
              synchOutput();
           }
              
           else if(k.getKeyChar() == 'z') {
              zoom(mousevent.getX(),mousevent.getY(),2.);
           }
           else if(k.getKeyChar() == 'o') {
              zoom(mousevent.getX(),mousevent.getY(),0.5);
           }
           else if(k.getKeyChar() == 'p') {
              pan(mousevent.getX(),mousevent.getY());
           }
           else if(k.getKeyChar() == 'r') {
              restorefull();
           }
           else if(k.getKeyChar() == 'h') {  // HR star ... 
              Celest markedC = pixtocelest(mousevent.getX(),mousevent.getY());
              SelBrightByPos(markedC);
              synchOutput();
           }
           else if(k.getKeyChar() == 'q' ||  k.getKeyChar() == 'x') {
              skydisplayvisible = false;
              SkyWin.setVisible(false);
           }
       }

      void SelBrightByPos(Celest incel) {  
       /* get input from the graphical display, or wherever, and get the nearest
          bright star on the list within a tolerance.  Precession is ignored. */
          double tolerance = 0.1;  // radians
          double decband = 6.;     // degrees
          double decin;
          Celest objcel;
          double sep, minsep = 1000000000000.;
          int i, minindex = 0;
          decin = incel.Delta.value;
   
          for(i = 0; i < bs.length; i++) {
              if(Math.abs(decin - bs[i].c.Delta.value) < decband) {  // guard expensive subtend
                  sep = Spherical.subtend(incel,bs[i].c);
                  if (sep < minsep) {
                      minsep = sep;
                      minindex = i;
                  }
             }
          } 
          
          if(minsep < tolerance) {
              objnamefield.setText(bs[minindex].name);
              RAfield.setText(bs[minindex].c.Alpha.RoundedRAString(2," "));
              decfield.setText(bs[minindex].c.Delta.RoundedDecString(1," "));
              equinoxfield.setText(String.format(Locale.ENGLISH, "%7.2f",bs[minindex].c.Equinox));
          }
      }
   
       void makeHAgrid(double latit) {

           int i;
           double ha, hamiddle, haend;
           double decstart, decmiddle, decend;
           double [] xystart;
           double [] xystartpix = {0.,0.};
           double [] xymiddle;
           double [] xymiddlepix = {0.,0.};
           double [] xyend;
           double [] xyendpix = {0.,0.};
           
           double coslat = Math.cos(latit / Const.DEG_IN_RADIAN);
           double sinlat = Math.sin(latit / Const.DEG_IN_RADIAN);
           
           HAgrid = new GeneralPath[8];

           // draw lines of constant HA from -6h to +6h
           for(i = 0; i < 7; i++) {
               HAgrid[i] = new GeneralPath();
               ha = (double) (2 * i - 6);
               decstart = 90.;
               decmiddle = 85.;
               decend = 80.;
               xystart = SkyProject(ha,decstart,coslat,sinlat);
               xystartpix = xytopix(xystart[0],xystart[1]);

               HAgrid[i].moveTo((float) xystartpix[0], (float) xystartpix[1]);
               while(decend > -91.) {
                   xymiddle = SkyProject(ha,decmiddle,coslat,sinlat);
                   xymiddlepix = xytopix(xymiddle[0],xymiddle[1]);
                   xyend = SkyProject(ha,decend,coslat,sinlat);
                   xyendpix = xytopix(xyend[0],xyend[1]);
                   HAgrid[i].quadTo((float) xymiddlepix[0], (float) xymiddlepix[1],
                                     (float) xyendpix[0], (float) xyendpix[1]);
                   decmiddle = decend - 10.;
                   decend = decend - 10.;
               }
           }
           // draw equator -- tuck it in as the last path in HAgrid.
           HAgrid[i] = new GeneralPath();
           ha = -6.;
           hamiddle = -5.5;
           haend = -5.;
           xystart = SkyProject(ha,0.,coslat,sinlat);
           xystartpix = xytopix(xystart[0],xystart[1]);
           HAgrid[i].moveTo((float) xystartpix[0], (float) xystartpix[1]);
           while(haend < 6.1) {
              xymiddle = SkyProject(hamiddle,0.,coslat,sinlat);
              xymiddlepix = xytopix(xymiddle[0],xymiddle[1]);
              xyend = SkyProject(haend,0.,coslat,sinlat);
              xyendpix = xytopix(xyend[0],xyend[1]);
              HAgrid[i].quadTo((float) xymiddlepix[0], (float) xymiddlepix[1],
                                 (float) xyendpix[0], (float) xyendpix[1]);
              hamiddle = hamiddle + 1.0;  
              haend = haend + 1.0;
           }
           System.out.printf("\n");
       }

       public void paint(Graphics g) {   // this method does the graphics.
           double [] xy1 = {0.,0.};
           double [] xy2 = {0.,0.};
           int i;

           g2 = (Graphics2D) g;
           smallfontmetrics = g2.getFontMetrics(smallfont);
           mediumfontmetrics = g2.getFontMetrics(mediumfont);
           largefontmetrics = g2.getFontMetrics(largefont);
           g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                 RenderingHints.VALUE_ANTIALIAS_ON);
           g2.setBackground(skycolor());
           g2.clearRect(0,0,xpixint,ypixint);

           // there's some attention given here to stacking these in the best order, since the 
           // last thing painted scribbles over previous things.

           if(o.w.where.lat.value != currentlat) {
              currentlat = o.w.where.lat.value;
              makeHAgrid(currentlat);
           }
           
           g2.setPaint(gridcolor);
           g2.setStroke(new BasicStroke(0.7f));
           for (i = 0; i < HAgrid.length; i++) {
               g2.draw(HAgrid[i]);
           }
 
           drawcircle(0.,0.,1.,"solid",0.7f);   // horizon
           drawcircle(0.,0.,0.57735,"dotted",0.7f); // 2 airmasses
           drawcircle(0.,0.,0.70711,"dotted",0.7f); // 3 airmasses
           drawcircle(0.,0.,0.7746,"dotted",0.7f); // 4 airmasses
           g2.setStroke(new BasicStroke(1.0f));

           labelDirections();

           PlotBright();  

           markcoords();
           plotsun();
           plotmoon();
           plotplanets();

           drawclock(xmid + 0.85 * halfwidthx, ymid + 0.85 * halfwidthy, 0.10 * halfwidthx);

           puttext(xmid + 0.7 * halfwidthx, ymid - 0.90 * halfwidthy, o.w.where.name,
                  mediumfont, mediumfontmetrics);
           puttext(xmid + 0.7 * halfwidthx, ymid - 0.95 * halfwidthy, 
                  o.w.when.UTDate.RoundedCalString(7,0) + " UT", mediumfont,
                  mediumfontmetrics);

           plotobjects();
   
       }

       Color skycolor() {
           if(o.w.altsun < -18) return Color.BLACK;   
           if(o.w.altsun > -0.8) return new Color(61,122,140);  // daytime sky color
           if(o.w.twilight > 5.)  {
                double fac = (o.w.twilight - 5.) / 10.;
                return new Color(51 + (int) (fac * 10.2), 69 + (int) (fac * 54.), 
                                112 + (int) (fac * 28.));
           }
           else {
                double fac = (o.w.twilight + 4.) / 9.;
                return new Color((int) (51. * fac), (int) (68.85 * fac), (int) (112.2 * fac));
           }
       }

       void drawclock(double x, double y, double radius) {

           int i;
           double angle, tickx1, tickx2, ticky1, ticky2;
           double cosang, sinang;
           double minutes;
           double seconds;
           double timeval;
           boolean isAM;
           double pixrad;
     
           double [] xy1 = {0.,0.};
           double [] xy2 = {0.,0.};    

           g2.setPaint(Color.CYAN);
           xy1 = xytopix(x - radius,y + radius);
           pixrad = radius * pixperunit;
           g2.draw(new Ellipse2D.Double(xy1[0],xy1[1], 2.* pixrad, 2. * pixrad));

           for(i = 0; i < 12; i++) {
              angle = 0.523599 * (double) i;  // constants is 30 degrees expressed in rad
              cosang = Math.cos(angle);
              sinang = Math.sin(angle);
              tickx1 = x + 0.93 * radius * cosang;
              tickx2 = x + radius * cosang;
              ticky1 = y + 0.93 * radius * sinang;
              ticky2 = y + radius * sinang;
              drawline(tickx1,ticky1,tickx2,ticky2);
           }
           timeval = o.w.when.localDate.timeofday.value;
           isAM = true;
           if(timeval >= 12.) isAM = false;
           while(timeval >= 12.) timeval -= 12.;
           // System.out.printf("Timeval = %f\n",timeval);
            
           // draw hands 
           angle = 0.523599 * timeval;
           cosang = Math.cos(angle);
           sinang = Math.sin(angle);
           g2.setStroke(new BasicStroke(2.5f));
           drawline(x,y,x + 0.65*radius*sinang, y + 0.65*radius*cosang);
           g2.setStroke(new BasicStroke(1.5f));
     
           angle = 6.2831853  * (timeval - (int) timeval);
           cosang = Math.cos(angle);
           sinang = Math.sin(angle);
           drawline(x,y,x + 0.83*radius*sinang, y + 0.83*radius*cosang);

    /* Uncomment to plot a second hand   */
           g2.setPaint(Color.RED);
           minutes = 60. * (timeval - (int) timeval);
           angle = 6.2831853  * (minutes - (int) minutes);
           cosang = Math.cos(angle);
           sinang = Math.sin(angle);
           g2.setStroke(new BasicStroke(1.0f));
           drawline(x + 0.7 * radius*sinang, y + 0.7 * radius * cosang, 
                    x + 0.88* radius*sinang, y + 0.88* radius*cosang);
           g2.setStroke(new BasicStroke(1.5f));
           g2.setPaint(Color.CYAN);
    /* */
           String ampm = "AM";
           if(!isAM) ampm = "PM";
           String dststr = "S";
           if (o.w.when.dstInEffect) dststr = "D";

           String outstr = String.format(Locale.ENGLISH, "%s %s%sT",ampm,o.w.where.zone_abbrev,
                  dststr);
           puttext(x, y - 1.4 * radius, outstr, mediumfont, 
                    mediumfontmetrics);
           if(o.w.altsun > -0.9) outstr = "Daytime";
           else if (o.w.altsun < -18.) outstr = "Nighttime";
           else outstr = "Twilight";
          // outstr = String.format(Locale.ENGLISH, "%s",o.w.where.name);
           puttext(x, y - 1.7 * radius, outstr, mediumfont, 
                    mediumfontmetrics);
//           String dststr = "Standard";
 //          if (o.w.when.dstInEffect) dststr = "Daylight";
  //         outstr = String.format(Locale.ENGLISH, "%s %s Time",o.w.where.timezone_name,dststr);
   //        puttext(x - 0.9 * radius, y - 1.5 * radius, outstr, mediumfont);
       }

       double [] xytopix(double x, double y) {
           double [] retvals = {0.,0.,};
           retvals[0] = 0.5 * xpix * (1. + (x - xmid) / halfwidthx);
           retvals[1] = 0.5 * ypix * (1. - (y - ymid) / halfwidthy);
           return retvals;
       }

       double [] pixtoxy(int xpixel, int ypixel) {
           double x, y;  // map coords, zero at zenith, r = 1 at horizon, radius = tan z/2.
           double [] retvals = {0.,0.};
  
           retvals[0] = xmid + halfwidthx * ((double) (2 * xpixel) / (double) xpix - 1.);
           retvals[1] = ymid + halfwidthy * (1. - (double) (2 * ypixel) / (double) ypix);
           
           return retvals;
       }
   
       Celest pixtocelest(int xpixel, int ypixel) {
           double x, y;  // map coords, zero at zenith, r = 1 at horizon, radius = tan z/2.
           double alt, az;
           double [] retvals = {0.,0.};
           double xt, yt, zt;  // topocentric xyz, zt toward zenith, yt south, xt west
  
           x = xmid + halfwidthx * ((double) (2 * xpixel) / (double) xpix - 1.);
           y = ymid + halfwidthy * (1. - (double) (2 * ypixel) / (double) ypix);
           double mod = Math.sqrt(x*x + y*y);
          // retvals[0] = (Const.PI / 2. - 2. * Math.atan(mod)) * Const.DEG_IN_RADIAN;  // alt 

           if(o.w.where.lat.value < 0.) {
               x = x * -1.;  y = y * -1.;    // plot is inverted in s hemishphere ... 
           }
           alt = (Const.PI / 2. - 2. * Math.atan(mod)); // alt 
           az = Math.atan2(-1.*x, y); //  * Const.DEG_IN_RADIAN;  
          // retvals[1] = az;
          // System.out.printf("%d %d -> %f %f -> %f %f\n",xpixel,ypixel,x,y,retvals[0],retvals[1]);
           zt = Math.sin(alt);
           xt = -1. * Math.cos(alt) * Math.sin(az);
           yt = -1. * Math.cos(alt) * Math.cos(az);
           // rotate around x axis, zenith to the pole ... 
           double coslat = Math.cos(o.w.where.lat.radians());
           double sinlat = Math.sin(o.w.where.lat.radians());
           double yc = yt * sinlat + zt * coslat;
           double zc = -1. * yt * coslat + zt * sinlat;
           double [] hadecdist = Celest.XYZcel(xt,yc,zc);  // x remains the same
           // hadecdist[0] is the hour angle ... 
           RA Alph = new RA(hadecdist[0] - 6. + o.w.sidereal);
           dec Delt = new dec(hadecdist[1]);
           Celest cel = new Celest(Alph,Delt,2000.); // coord eq is sloppy ... 
//           System.out.printf("%s %s\n",cel.Alpha.RoundedRAString(0,":"),
 //                 cel.Delta.RoundedDecString(-1,":"));
           return(cel);
       }

       void drawline(double x1, double y1, double x2, double y2) {  // user coords
           double [] xy1;
           double [] xy2;
           xy1 = xytopix(x1,y1);
           xy2 = xytopix(x2,y2);
           g2.draw(new Line2D.Double(xy1[0],xy1[1],xy2[0],xy2[1]));
       }

       void drawcircle(double x1, double y1, double radius, String style, float thickness) {
           // centered on x1, y1 user coords, radius is also in user coords, 
           // string can be "solid" or "dashed"
           double [] xy = {0.,0.};
           xy = xytopix(x1 - radius, y1 + radius);   // upper left corner
           double diam = 2. * pixperunit * radius;

           if(style.equals("dashed")) {
               float [] dash1 = {10.0f};
               BasicStroke dashed = new BasicStroke(thickness,BasicStroke.CAP_BUTT,
                           BasicStroke.JOIN_MITER,10.0f,dash1,0.0f);
               g2.setStroke(dashed);
           }
           if(style.equals("dotted")) {
               float [] dot1 = {3.0f};
               BasicStroke dotted = new BasicStroke(thickness,BasicStroke.CAP_BUTT,
                           BasicStroke.JOIN_MITER,3.0f,dot1,0.0f);
               g2.setStroke(dotted);
           }
 
           g2.draw(new Ellipse2D.Double(xy[0],xy[1],diam,diam));
           g2.setStroke(new BasicStroke(thickness)); 
       }

       void drawbox(double x1, double y1, double edge, String style, float thickness) {
           // centered on x1, y1 user coords, radius is also in user coords, 
           // string can be "solid" or "dashed"
           double [] xy = {0.,0.};
           xy = xytopix(x1 - edge/2., y1 + edge/2.);   // upper left corner
           double edgepix =  pixperunit * edge;

           if(style.equals("dashed")) {
               float [] dash1 = {10.0f};
               BasicStroke dashed = new BasicStroke(thickness,BasicStroke.CAP_BUTT,
                           BasicStroke.JOIN_MITER,10.0f,dash1,0.0f);
               g2.setStroke(dashed);
           }
           if(style.equals("dotted")) {
               float [] dot1 = {3.0f};
               BasicStroke dotted = new BasicStroke(thickness,BasicStroke.CAP_BUTT,
                           BasicStroke.JOIN_MITER,3.0f,dot1,0.0f);
               g2.setStroke(dotted);
           }
 
           g2.draw(new Rectangle2D.Double(xy[0],xy[1],edgepix,edgepix));
           g2.setStroke(new BasicStroke(thickness)); 
       }

       void markcoords() {
           double lst;
           double xy[];

           lst = o.w.sidereal;
           double coslat = Math.cos(o.w.where.lat.radians());
           double sinlat = Math.sin(o.w.where.lat.radians());
           xy = SkyProject(lst - o.current.Alpha.value, 
                           o.current.Delta.value, coslat, sinlat);
           g2.setPaint(objboxcolor);
           drawbox(xy[0],xy[1],0.05,"solid",1.3f);
       }

       void plotsun() {
           double lst;
           double xy[];

           lst = o.w.sidereal;
           double coslat = Math.cos(o.w.where.lat.radians());
           double sinlat = Math.sin(o.w.where.lat.radians());
           xy = SkyProject(lst - o.w.sun.topopos.Alpha.value, 
                           o.w.sun.topopos.Delta.value, coslat, sinlat);
           putDot(xy[0],xy[1],0.009,brightyellow);
           g2.setPaint(brightyellow);
           drawcircle(xy[0],xy[1],0.02,"solid",1.5f);
      }
    
      void puttext(double x, double y, String text, Font font, FontMetrics metrics) {
           double [] xy;
           // write text centered left-to-right on user coords x and y.
           xy = xytopix(x,y);
           
           g2.setFont(font);
           int adv = metrics.stringWidth(text);
           // System.out.printf("adv = %d\n",adv);
           g2.drawString(text,(int) xy[0] - adv / 2,(int) xy[1]);
       }       

       void labelDirections() {
          if (o.w.where.lat.value > 0.) {
             puttext(-0.97, 0.,  "E", largefont, largefontmetrics);      
             puttext( 0.97, 0.,  "W", largefont, largefontmetrics);
             puttext( 0.0,  0.80, "N", largefont, largefontmetrics);
             puttext( 0.0, -0.83, "S", largefont, largefontmetrics);
          }
          else {
             puttext(-0.97, 0.,  "W", largefont, largefontmetrics);      
             puttext( 0.97, 0.,  "E", largefont, largefontmetrics);
             puttext( 0.0,  0.80, "S", largefont, largefontmetrics);
             puttext( 0.0, -0.83, "N", largefont, largefontmetrics);
          }
       }

       void putDot(double x, double y, double size, Color color) {
          double [] xy;
          // dotsize, unfortunately, is in user units.
          xy = xytopix(x - size/2.,y + size/2.);
          g2.setPaint(color);
          g2.fill(new Ellipse2D.Double(xy[0],xy[1],pixperunit * size, pixperunit * size));
       }

       void LoadBright() {
 
          int i = 0;
          bs = new BrightStar [904];
          File infile = null;
          FileReader fr = null;
          BufferedReader br = null;
          String st;
          
          try {
	      ClassLoader cl = this.getClass().getClassLoader();
       	      InputStream is = cl.getResourceAsStream("brightest.dat");
             // infile = new File("brightest.dat");
            //  fr = new FileReader(infile);
              br = new BufferedReader(new InputStreamReader(is));
          } catch (Exception e)
            { System.out.printf("Problem opening brightest.dat for input.\n"); } 
         
          try { 
             while((st = br.readLine()) != null) {
                 bs[i] = new BrightStar(st);
                 i++;
             }
          } catch (IOException e) { System.out.println(e); }
          
          System.out.printf("%d bright stars read.\n",bs.length);
          
       } 

       void PrecessBrightStars(double equinox) {
          // precesses the stars in the BrightStar list bs.  This is more efficient
          // with a special method because the matrix only gets computed once.
 
          // code is largely copied from the precess method of Celest.
          double ti, tf, zeta, z, theta;
          double cosz, coszeta, costheta, sinz, sinzeta, sintheta, cosdelt;
          double [][] p = {{0.,0.,0.,},{0.,0.,0.},{0.,0.,0.}};
          double [] orig = {0.,0.,0.};
          double [] fin = {0.,0.,0.};
          double [] radecdist = {0.,0.,0.};
    
          int i, j, ist;

          // compute the precession matrix ONCE.
          ti = (bs[0].c.Equinox - 2000.) / 100.;
          tf = (equinox - 2000. - 100. * ti) / 100.;
     
          zeta = (2306.2181 + 1.39656 * ti + 0.000139 * ti * ti) * tf +
             (0.30188 - 0.000344 * ti) * tf * tf + 0.017998 * tf * tf * tf;
          z = zeta + (0.79280 + 0.000410 * ti) * tf * tf + 0.000205 * tf * tf * tf;
          theta = (2004.3109 - 0.8533 * ti - 0.000217 * ti * ti) * tf
             - (0.42665 + 0.000217 * ti) * tf * tf - 0.041833 * tf * tf * tf;
    
          cosz = Math.cos(z / Const.ARCSEC_IN_RADIAN);
          coszeta = Math.cos(zeta / Const.ARCSEC_IN_RADIAN);
          costheta = Math.cos(theta / Const.ARCSEC_IN_RADIAN);
          sinz = Math.sin(z / Const.ARCSEC_IN_RADIAN);
          sinzeta = Math.sin(zeta / Const.ARCSEC_IN_RADIAN);
          sintheta = Math.sin(theta / Const.ARCSEC_IN_RADIAN);
    
          p[0][0] = coszeta * cosz * costheta - sinzeta * sinz;
          p[0][1] = -1. * sinzeta * cosz * costheta - coszeta * sinz;
          p[0][2] = -1. * cosz * sintheta;
    
          p[1][0] = coszeta * sinz * costheta + sinzeta * cosz;
          p[1][1] = -1. * sinzeta * sinz * costheta + coszeta * cosz;
          p[1][2] = -1. * sinz * sintheta;
    
          p[2][0] = coszeta * sintheta;
          p[2][1] = -1. * sinzeta * sintheta;
          p[2][2] = costheta;
    
          for(ist = 0; ist < bs.length; ist++) {   
             cosdelt = Math.cos(bs[ist].c.Delta.radians()); 
             orig[0] = cosdelt * Math.cos(bs[ist].c.Alpha.radians());
             orig[1] = cosdelt * Math.sin(bs[ist].c.Alpha.radians());
             orig[2] = Math.sin(bs[ist].c.Delta.radians()); 
             for(i = 0; i < 3; i++) {  // matrix multiplication 
                fin[i] = 0.;
                //System.out.printf("orig[%d] = %f\n",i,orig[i]);
                for(j = 0; j < 3; j++) {
                    //System.out.printf("%d%d: %f  ",i,j,p[i][j]);
                    fin[i] += p[i][j] * orig[j];
                }
                //System.out.printf("\nfin[%d] = %f\n\n",i,fin[i]);
             }
             radecdist = Celest.XYZcel(fin[0],fin[1],fin[2]);
             bs[ist].c.Alpha.setRA(radecdist[0]);
             bs[ist].c.Delta.setDec(radecdist[1]);
             bs[ist].c.Equinox = equinox;
          }
       }

       double [] SkyProject(double hain, double decin, double coslat, double sinlat) {

          double [] retvals  = {0.,0.};

          // This will be called many times so code it for speed!

          double ha = hain / Const.HRS_IN_RADIAN;
          double dec = decin / Const.DEG_IN_RADIAN;
          double x,y,z;
         
          double cosdec = Math.cos(dec);

          x = cosdec * Math.sin(ha);
          y = cosdec * Math.cos(ha);
          z = Math.sin(dec);

          double ypr = sinlat * y - coslat * z;   // rotating backward, by CO-latitude, so sin 
          double zpr = coslat * y + sinlat * z;   // and cos switch around.

          double zdist = Math.acos(zpr);
          double r = Math.tan(zdist / 2.);
          double inground = Math.sqrt(x * x + ypr * ypr); 
          retvals[0] =  r * (x / inground);
          retvals[1] = -1. * r * (ypr / inground);
          if(sinlat < 0.)  {  // invert for south
              retvals[0] = -1. * retvals[0];
              retvals[1] = -1. * retvals[1];
          }

          return retvals;
       }

       void PlotBright() {
          // stripped down for speed -- avoids the OO stuff . 
          // Precesses only if mismatch is > 1 year.
          double ha, dec, lst;
          double equinoxnow;
          double xy[];
          double magconst1 = 0.002, magslope = 0.002;  // 
	  double magzpt = 4.7;
          int i;
          
          lst = o.w.sidereal;

          // precess the list if the epoch mismatch is > 1 yr
          equinoxnow = o.w.when.JulianEpoch();   
          if(Math.abs(equinoxnow - bs[0].c.Equinox) > 1.) {
              // System.out.printf("%s -> ",bs[0].c.checkstring());
              PrecessBrightStars(equinoxnow);
              // System.out.printf("%s \n",bs[0].c.checkstring());
          }

          double coslat = Math.cos(o.w.where.lat.radians());
          double sinlat = Math.sin(o.w.where.lat.radians());
          for(i = 0; i < bs.length; i++) {
             xy = SkyProject(lst - bs[i].c.Alpha.value, bs[i].c.Delta.value, coslat,sinlat);
             //System.out.printf("PB %f %f\n",xy[0],xy[1]);
             putDot(xy[0],xy[1],magconst1 + magslope * (magzpt - bs[i].m),bs[i].col);
          } 
       }

       void plotmoon() {

          int nseg = 21;
          
          double [][] limbxy = new double [2][nseg];    // in system where cusp axis = y
          double [][] termxy = new double [2][nseg];

          double [][] limbxypr = new double [2][nseg];  // in system where North = y         
          double [][] termxypr = new double [2][nseg];
          
          double theta;
          double cos_sunmoon;
          int i, j, k;

          // set up points describing terminator and limb -- then rotate into
          // position.
          cos_sunmoon = Math.cos(o.w.sunmoon / Const.DEG_IN_RADIAN);
          // System.out.printf("sunmoon %f cos_sunmoon %f\n",o.w.sunmoon,cos_sunmoon);
          double dtheta = Const.PI / (double)(nseg - 1);
          for(j = 0; j < nseg; j++) {
             theta = dtheta * (double) j;
             limbxy[0][j] = Math.cos(theta);
             limbxy[1][j] = Math.sin(theta);
             termxy[0][j] = limbxy[0][j];   // need a second copy later
             termxy[1][j] = limbxy[1][j] * cos_sunmoon;
          }

          // rotate to appropriate position angle

          double pa = o.w.cusppa + (Const.PI)/2.;   // cusppa already in rad.
          double[][] turnmoon = {{Math.cos(pa),Math.sin(pa)},
                                   {-1. * Math.sin(pa),Math.cos(pa)}};
          for(j = 0; j < nseg; j++) {
             for(i = 0; i < 2; i++) {
                limbxypr[i][j] = 0.;
                termxypr[i][j] = 0.;
                for (k = 0; k < 2; k++) {
                   limbxypr[i][j] += turnmoon[i][k] * limbxy[k][j];
                   termxypr[i][j] += turnmoon[i][k] * termxy[k][j];
                }
             }
          }

          double zover2 = (90. - o.w.altmoon) / (Const.DEG_IN_RADIAN * 2.);
          double coszover2 = Math.cos(zover2);
          double moonsize = 3. * coszover2;

          double rafac = 15. * Math.cos(o.w.moon.topopos.Delta.radians());

          double [] ralimb = new double[nseg];
          double [] declimb = new double[nseg];
          double [] raterm = new double[nseg];
          double [] decterm = new double[nseg];
         
          double racent =  o.w.moon.topopos.Alpha.value;  // saving verbosity
          double deccent = o.w.moon.topopos.Delta.value;  

         // double moonsize = 3.;  
          
          for(i = 0; i < nseg; i++) {
              ralimb[i] = o.w.sidereal -   // Hereafter actually HA, not RA 
                  (racent + limbxypr[0][i] * moonsize/ rafac); 
              declimb[i] = deccent + limbxypr[1][i] * moonsize;
              raterm[i] = o.w.sidereal - 
                  (racent + termxypr[0][i] * moonsize / rafac);
              decterm[i] = deccent + termxypr[1][i] * moonsize;
          }

          double coslat = Math.cos(o.w.where.lat.radians());
          double sinlat = Math.sin(o.w.where.lat.radians());

          GeneralPath limbpath = new GeneralPath();
          GeneralPath termpath = new GeneralPath();
          
          double [] xy = SkyProject(ralimb[0],declimb[0],coslat,sinlat);
          double [] xypix = xytopix(xy[0],xy[1]);
          limbpath.moveTo((float) xypix[0], (float) xypix[1]);

          xy = SkyProject(raterm[0],decterm[0],coslat,sinlat);
          xypix = xytopix(xy[0],xy[1]);
          termpath.moveTo((float) xypix[0], (float) xypix[1]);

//          for(i = 1; i < 11; i++) {
//              
//             xy = SkyProject(ralimb[i],declimb[i],coslat,sinlat);
//             xypix = xytopix(xy[0],xy[1]);
//             limbpath.lineTo((float) xypix[0], (float) xypix[1]);
//   
//             xy = SkyProject(raterm[i],decterm[i],coslat,sinlat);
//             xypix = xytopix(xy[0],xy[1]);
//             termpath.lineTo((float) xypix[0], (float) xypix[1]);
//
//          }
         
        // try rendering with quadratic curves

          double [] xy2 = {0.,0.};
          double [] xypix2 = {0.,0.};

         for(i = 1; i < nseg-1; i = i + 2) {
             
            xy = SkyProject(ralimb[i],declimb[i],coslat,sinlat);
            xypix = xytopix(xy[0],xy[1]);
            xy2 = SkyProject(ralimb[i+1],declimb[i+1],coslat,sinlat);
            xypix2 = xytopix(xy2[0],xy2[1]);
            limbpath.quadTo((float) xypix[0], (float) xypix[1],
                             (float) xypix2[0], (float) xypix2[1]);
  
            xy = SkyProject(raterm[i],decterm[i],coslat,sinlat);
            xypix = xytopix(xy[0],xy[1]);
            xy2 = SkyProject(raterm[i+1],decterm[i+1],coslat,sinlat);
            xypix2 = xytopix(xy2[0],xy2[1]);
            termpath.quadTo((float) xypix[0], (float) xypix[1], 
                             (float) xypix2[0], (float) xypix2[1]);

         }

          g2.setPaint(brightyellow);

          g2.draw(limbpath);
          g2.draw(termpath);

       }
  
       void plotplanets() {  
          int i;
          double xy[] = {0.,0.};
          double xypix[] = {0.,0.};
          double ha, dec, lst;
          double magconst1 = 0.003, magslope = 0.002;  // 
 
          p.computemags();
          
          lst = o.w.sidereal;
          double coslat = Math.cos(o.w.where.lat.radians());
          double sinlat = Math.sin(o.w.where.lat.radians());
  
          for (i = 0; i < 9; i++)  {
             if(i != 2) {
                xy = SkyProject(lst - p.PlanetObs[i].c.Alpha.value,
                          p.PlanetObs[i].c.Delta.value, coslat, sinlat);
                if(p.mags[i] < 4.7) 
                  putDot(xy[0],xy[1],magconst1 + magslope * (4.5 - p.mags[i]),Color.YELLOW);
                else 
                  putDot(xy[0],xy[1],magconst1,Color.YELLOW);
                puttext(xy[0],xy[1] + 0.01,p.names[i],smallfont,smallfontmetrics);
             }
          }
       } 
    

       void plotobjects() {  // plots out the objects on the user list.
           int i;
           double xy[] = {0.,0.};
           double xypix[] = {0.,0.};
           double ha, dec, lst;
           double magconst1 = 0.003, magslope = 0.002;  // 
           AstrObj obj;
           Celest cel;
           double currenteq = o.w.when.JulianEpoch();
           
           double coslat = Math.cos(o.w.where.lat.radians());
           double sinlat = Math.sin(o.w.where.lat.radians());
           lst = o.w.sidereal;
           g2.setPaint(Color.GREEN);
   
           for(i = 0; i < presenterKey.size(); i++) {
               // System.out.printf("i %d RASelectors[i] = %s, name = \n",i,RASelectors[i]);
               obj = presenterKey.get(RASelectors[i]);
               if( ! obj.name.equals("null")) {
                   cel = obj.c.precessed(currenteq);
                   xy = SkyProject(lst - cel.Alpha.value,
                            cel.Delta.value, coslat, sinlat);
                   puttext(xy[0],xy[1],obj.name,smallfont,smallfontmetrics);
               }
           }
       }

       void zoom(int xpixin, int ypixin, double zoomfac) {
            // user hands in pixel coordinates around which to zoom.
 
            double [] xycent = pixtoxy(xpixin, ypixin);
            xmid = xycent[0];
            ymid = xycent[1];
            halfwidthy = halfwidthy / zoomfac;
            halfwidthx = halfwidthx / zoomfac;
            pixperunit = pixperunit * zoomfac;
            zoomedby = zoomedby * zoomfac;
            currentlat = o.w.where.lat.value;
            makeHAgrid(currentlat); 
            repaint();
       }

       void pan(int xpixin, int ypixin) {
            // user hands in pixel coordinates around which to zoom.
 
            double [] xycent = pixtoxy(xpixin, ypixin);
            xmid = xycent[0];
            ymid = xycent[1];
            currentlat = o.w.where.lat.value;
            makeHAgrid(currentlat);
            repaint();
       }

       void restorefull() {
            xmid = 0.;  ymid = 0.;
            halfwidthx = halfwidthxfull; halfwidthy = halfwidthyfull;
            pixperunit = pixperunitfull;
            zoomedby = 1.;
            currentlat = o.w.where.lat.value;
            makeHAgrid(currentlat);
            repaint();
       }

       class tinyHelp extends JWindow {

            final JTextArea area;

            tinyHelp() {
               area = new JTextArea();
               area.append("Middle button or 'c'  - sets to coords\n");   
               area.append("Left button or 's' - sets to nearest listed obj\n");   
               area.append("'f'  - step time Forward \n");   
               area.append("'b'  - step time Backward \n");   
               area.append("'z'  - Zoom in at mouse position\n");   
               area.append("'o'  - zoom Out at mouse position\n");   
               area.append("'p'  - Pan, recenter at mouse position \n");   
               area.append("'r'  - Restore original scaling \n");   
               area.append("'h'  - Set to nearest HR star.\n");
               area.append("'q' or 'x'  - Hide display window.\n");
               area.append("Keys don't respond unless window has focus.\n");
               area.setBackground(brightyellow);
               this.setSize(275,150);
//               this.setSize(300,220);
               this.add(area);
            }
           
            void show(int x, int y) {
               this.setLocation(x, y);
               this.setVisible(true);
            }
            void hide(int x, int y) {  // giving it arguments avoids an override
                                       // problem.
               this.setVisible(false);
            }
       }
    }

    class AirmassDisplay extends JComponent 
         //       implements MouseMotionListener, 
          implements      MouseListener
    {
       int xpix, ypix;             // window size
       double xwidth, yheight;      // same, but double
       double xlobord, xhibord, ylobord, yhibord;   // user coords of extreme edge
       double xlo, xhi, ylo, yhi;      // user coordinates of frame
       double endfade, startfade;      // beginning and end of "twilight" for fade
       double xvplo, xvphi, yvplo, yvphi;  // borders of frame as fraction of viewport
       Graphics2D g2;
       double jdstart, jdend;
       WhenWhere w;
       Font smallfont;
       FontMetrics smallfontmetrics;
       Font mediumfont;
       FontMetrics mediumfontmetrics;
       Font largefont;
       FontMetrics largefontmetrics;
       MouseEvent mousevent;
       Color [] objcolors = {Color.RED,Color.GREEN,Color.CYAN,Color.MAGENTA};
 
       AirmassDisplay(int xpixin, int ypixin) {  // constructed after Nightly has been updated ... 
          xpix = xpixin;
          ypix = ypixin;
          yheight = (double) ypixin;
          xwidth = (double) xpixin;
          ylo = 3.2;  yhi = 0.9;
          xvplo = 0.08; xvphi = 0.88;
          yvplo = 0.15; yvphi = 0.90;
 
          smallfont = new Font("Dialog", Font.PLAIN, 11);
          mediumfont = new Font("Dialog",Font.PLAIN, 15);

          setFocusable(true);
        //  addMouseMotionListener(this);
          addMouseListener(this);
         
          Update();  
       }

       void Update() {
          double timespan;

          w = (WhenWhere) Nightly.sunset.clone();
          jdstart = w.when.jd;
          xlo = w.when.UTDate.timeofday.value;
         
          jdend = Nightly.sunrise.when.jd;
          timespan = (jdend - jdstart) * 24.;
          // System.out.printf("timespan = %f hrs\n",timespan);
          xhi = xlo + timespan;  
          endfade = xlo + (Nightly.eveningTwilight.when.jd - jdstart) * 24.;
          //System.out.printf("xlo %f dt %f Endfade = %f\n",xlo,
           //      (Nightly.eveningTwilight.when.jd - jdstart) * 24.,endfade);
          startfade = xlo + (Nightly.morningTwilight.when.jd - jdstart) * 24.;

          double span = xhi - xlo;
          double fracx = xvphi - xvplo;
          xlobord = xlo - xvplo * span / fracx;
          xhibord = xhi + (1 - xvphi) * span/fracx;

          span = yhi - ylo;
          double fracy = yvphi - yvplo;
          ylobord = ylo - yvplo * span / fracy;
          yhibord = yhi + (1 - yvphi) * span / fracy;

          // System.out.printf("start %f end %f\n",jdstart, jdend);
          repaint();
       }

       public void mouseClicked(MouseEvent e) {
            // New feature -- set time to the time clicked in the AirmassDisplay window
            mousevent = e;
            double mousetimeofday = xlobord + ((float) mousevent.getX() / (float) xpix) * (xhibord - xlobord);
            double since_xlo = mousetimeofday - xlo;
            double jdmouse = jdstart + since_xlo / 24.;
            JDfield.setText(String.format(Locale.ENGLISH, "%15.6f",jdmouse));
            setToJD();
            // System.out.printf("x %d y %d mousetimeofday %f since_xlo %f jdmouse %f\n",
              //    mousevent.getX(),mousevent.getY(),mousetimeofday,since_xlo,jdmouse);
        
       }
       // need to define all the rest of the MouseEvent interface of course ... 
       public void mousePressed(MouseEvent e) {
       }
       public void mouseReleased(MouseEvent e) {
       }
       public void mouseEntered(MouseEvent e) {
       }
       public void mouseExited(MouseEvent e) {
       }


       public void paint(Graphics g) {

          int i, j;
          double xtick;
          double [] xy1 = {0.,0.};
          double [] xy2 = {0.,0.};
          double [] xy3 = {0.,0.};
          double [] xy4 = {0.,0.};
          double jd, ut, local;
   
          Observation omoon = (Observation) o.clone();

          g2 = (Graphics2D) g;

          Stroke defStroke = g2.getStroke();   // store the default stroke.
          Color gridcolor = new Color(153,0,0);  // dark red

          float [] dot1 = {1.0f,4.0f};
          BasicStroke dotted = new BasicStroke(0.3f,BasicStroke.CAP_BUTT,
                           BasicStroke.JOIN_MITER,1.0f,dot1,0.0f);

          smallfontmetrics = g2.getFontMetrics(smallfont);
          mediumfontmetrics = g2.getFontMetrics(mediumfont);
    
          g2.setBackground(Color.BLACK);
          g2.clearRect(0,0,xpix,ypix);
    
          //Color dayColor = new Color(70,130,180);
          // Color dayColor = new Color(60,80,180);
          Color dayColor = new Color(60,105,190);
          Color deepTwilight = new Color(10,20,30);   // faint bluish-grey
          // Color deepTwilight = new Color(40,40,40);   // Brighter grey for testing.
    
          g2.setPaint(dayColor);                     // left bdy = day color
          xy1 = xytopix(xlo,yhi);
          // g2.fill(new Rectangle2D.Double(0.,0.,xy1[0],yheight));

          xy2 = xytopix(endfade,yhi);
          // System.out.printf("xy1 %f %f  xy2 %f %f\n",xy1[0],xy1[1],xy2[0],xy2[1]);
    
          GradientPaint daytonight = new GradientPaint((int)xy1[0],0,dayColor,
                                                       (int)xy2[0],0,deepTwilight);
          g2.setPaint(daytonight);
          xy2 = xytopix(endfade,ylo);
          //g2.fill(new Rectangle2D.Double(xy1[0],0.,
           //                              xy2[0],yheight));
          g2.fill(new Rectangle2D.Double(xy1[0],xy1[1],
                                        xy2[0],xy2[1]));

          xy1 = xytopix(startfade,yhi);   // beginning of twilight

          g2.setPaint(Color.BLACK);       // have to overpaint the black ...
          g2.fill(new Rectangle2D.Double(xy2[0],0.,xy1[0],yheight));
    
          xy2 = xytopix(xhi,yhi);
          GradientPaint nighttoday = new GradientPaint((int)xy1[0],0,deepTwilight,(int)xy2[0],0,dayColor);
          g2.setPaint(nighttoday);
          xy2 = xytopix(xhi,ylo);
          g2.fill(new Rectangle2D.Double(xy1[0],xy1[1],xy2[0],xy2[1]));
    
          g2.setPaint(Color.BLACK);   // for some reason have to overpaint this ... 
          g2.fill(new Rectangle2D.Double(xy2[0],0.,xwidth,yheight));

          // overpaint the bottom, too ... 
          xy1 = xytopix(xlo,ylo);
          g2.fill(new Rectangle2D.Double(0,(int)xy1[1],xpix,ypix));
    
          g2.setPaint(Color.WHITE);
    
          g2.setStroke(new BasicStroke(1.0f));
          // draw the box
          drawline(xlo,yhi,xhi,yhi);
          drawline(xlo,yhi,xlo,ylo);
          drawline(xlo,ylo,xhi,ylo);
          drawline(xhi,ylo,xhi,yhi);

          double span = xhi - xlo;
          
          xy1 = xytopix(xlo - 0.07 * span, (ylo + yhi) / 2.);
          g2.rotate(-1. * Const.PI/2.,xy1[0],xy1[1]);
          puttext(xlo - 0.07 * span ,(ylo + yhi)/2.,"Airmass",mediumfont,mediumfontmetrics);
          // g2.transform(saveXform);
          g2.rotate(Const.PI/2,xy1[0],xy1[1]);

          span = yhi - ylo;
          double ytoplabel = yhi + 0.02 * span; 
          double ybottomlabel = ylo - 0.05 * span;
          double ybanner = yhi + 0.07 * span;

          span = xhi - xlo;
          double ticklen = (yhi - ylo)/80.;
          double minordiv = 0.25; // hard code divisions, sorry ...

          puttext(xlo - span * 0.04,ytoplabel,"UT:",mediumfont,mediumfontmetrics);
          puttext(xlo - span * 0.05,ybottomlabel,"Local:",mediumfont,mediumfontmetrics);

          w.ChangeWhen(jdstart);

          String banner = String.format("%s - Evening date %s",
               w.where.name,w.when.localDate.RoundedCalString(6,0));
          puttext((xlo + xhi) / 2., ybanner, banner, mediumfont, mediumfontmetrics);
          
    
          xtick = (double)((int) xlo + 1);
          double xminortick;
    
          for(j = 1; j < 4; j++) {  // fill in start .. 
             xminortick = xtick - j * minordiv;  
             if(xminortick > xlo) {
                drawline(xminortick, yhi, xminortick, yhi - ticklen/2.);
                drawline(xminortick, ylo, xminortick, ylo + ticklen/2.);
             }
          }

          while( xtick < xhi ) { 
             w.ChangeWhen(jdstart + (xtick - xlo) / 24.);
             ut = w.when.UTDate.timeofday.value;
             local = w.when.localDate.timeofday.value;
             puttext(xtick,ytoplabel,String.format("%02.0f",ut),
                     mediumfont,mediumfontmetrics);
             puttext(xtick,ybottomlabel,String.format("%02.0f",local),
                     mediumfont,mediumfontmetrics);
    
             drawline(xtick, yhi, xtick, yhi - ticklen);
             drawline(xtick, ylo, xtick, ylo + ticklen);
             g2.setStroke(dotted);
             g2.setPaint(gridcolor);
             drawline(xtick,yhi,xtick,ylo);
             g2.setStroke(defStroke);
             g2.setPaint(Color.WHITE);
             for(j = 1; j < 4; j ++) {
                 xminortick = xtick + j * minordiv;
                 if(xminortick < xhi) {
                     drawline(xminortick, yhi, xminortick, yhi - ticklen/2.);
                     drawline(xminortick, ylo, xminortick, ylo + ticklen/2.);
                 }
             }

             omoon.w.ChangeWhen(jdstart + (xtick - xlo) / 24.);
             omoon.ComputeSky();
             omoon.ComputeSunMoon();
             if(omoon.w.altmoon > 0.) plotmoon();

             xtick += 1.;
          }

          // Draw a vertical line at current time ... 

          // System.out.printf("o.w.when.jd = %f, jdstart = %f, ",o.w.when.jd,jdstart);          
	  double hrs_since = xlo + (o.w.when.jd - jdstart) * 24.;
          // System.out.printf("hrs_since = %f\n",hrs_since);
           
          if(hrs_since > xlo && hrs_since < xhi) {
                Color nowColor = new Color(128,128,128); // grey
                // g2.setStroke(dotted);
                g2.setPaint(nowColor);
          	drawline(hrs_since,yhi,hrs_since,ylo);
                // g2.setStroke(defStroke);
                g2.setPaint(Color.WHITE);
	  }
  

          double yticklen = (xhi - xlo) / 120.;
          double yminortick;
          double ytick = 1.0;
          double labelx = xlo - (xhi - xlo) / 30.;
          double ylabeloffset = (yhi - ylo) / 60.;
          while (ytick < ylo) {
             drawline(xlo,ytick,xlo+yticklen,ytick);
             drawline(xhi,ytick,xhi-yticklen,ytick);
             puttext(labelx,ytick-ylabeloffset,String.format("%3.1f",ytick),
                    mediumfont,mediumfontmetrics);
             g2.setStroke(dotted);
             g2.setPaint(gridcolor);
             drawline(xlo,ytick,xhi,ytick);
             g2.setStroke(defStroke);
             g2.setPaint(Color.WHITE);
             for(j = 1; j < 5; j++) {
                yminortick = ytick + j * 0.1;
                if(yminortick < ylo) {
                   drawline(xlo,yminortick,xlo+yticklen/2.,yminortick);
                   drawline(xhi,yminortick,xhi-yticklen/2.,yminortick);
                }
             }
             ytick = ytick + 0.5;
          }

          // draw a separate vertical axis for moon altitude, off to the side.
          // scale is not the same as for the airmass axes.

          g2.setPaint(Color.YELLOW);
          span = xhi - xlo;
          double xmoonaxis = xhi + span * 0.04;
          drawline(xmoonaxis,ylo,xmoonaxis,yhi);
          double tickstep = (ylo - yhi) / 9.;  // 10 degrees
          for(i = 0; i < 10; i++) {
             ytick = yhi + i * tickstep;
             drawline(xmoonaxis-yticklen,ytick,xmoonaxis,ytick);
             if(i % 3 == 0) 
                 puttext(xmoonaxis + span/40.,ytick + 0.05,String.format("%d",(9 - i) *10),
                    mediumfont,mediumfontmetrics);
          }

          double xmoonlabel = xmoonaxis + span * 0.05;
          xy1 = xytopix(xmoonlabel,(ylo + yhi) / 2.);
          g2.rotate(Const.PI/2, xy1[0],xy1[1]);
          puttext(xmoonlabel,(ylo + yhi) / 2.,"Moon Altitude [deg]", mediumfont,
              mediumfontmetrics);
          g2.rotate(-1. * Const.PI/2, xy1[0],xy1[1]);
          
          g2.setPaint(Color.WHITE);

          plotAirmass(o.w,o.c, " ", Color.WHITE);  // white = main window object.
   
          if (airmassPlotSelections.length > 0 && airmassPlotSelections[0] != null) {
             String sel;
             String airmname;
             int cindex = 0;
             for(i = 0; i < airmassPlotSelections.length; i++) {
                sel = (String) airmassPlotSelections[i];
                plotAirmass(o.w, presenterKey.get(sel).c, sel, objcolors[cindex]);
                cindex++;
                if(cindex == objcolors.length) cindex = 0;  // cycle colors
             }
          } 
       }

       void drawline(double x1, double y1, double x2, double y2) {  // user coords
               double [] xy1; 
               double [] xy2;
               xy1 = xytopix(x1,y1);
               xy2 = xytopix(x2,y2);
          //     System.out.printf("x2 y2 %f %f xy2 %f %f\n",x2,y2,xy2[0],xy2[1]);
               g2.draw(new Line2D.Double(xy1[0],xy1[1],xy2[0],xy2[1]));
       }
    
       double [] xytopix(double x, double y) {
            double [] retvals = {0.,0.};
            retvals[0] = xpix * (x - xlobord) / (xhibord - xlobord);
            retvals[1] = ypix * (1. - (y - ylobord) / (yhibord - ylobord));
            return(retvals);
       }    
 
//       double [] pixtoxy(double pixx, double pixy) {
//            double [] retvals = {0.,0.};
//            retvals[0] = xpix * (x - xlobord) / (xhibord - xlobord);
//            retvals[1] = ypix * (1. - (y - ylobord) / (yhibord - ylobord));
//            return(retvals);
//       }    

       void puttext(double x, double y, String text, Font font, FontMetrics metrics) {
           double [] xy;
           // write text centered left-to-right on user coords x and y.
           xy = xytopix(x,y);
           
           g2.setFont(font);
           int adv = metrics.stringWidth(text);
           g2.drawString(text,(int) xy[0] - adv / 2,(int) xy[1]);
       }              

       void plotAirmass(WhenWhere wIn, Celest cIn, String objectname, Color objcolor) {

          g2.setPaint(objcolor);

          Observation oairm = new Observation((WhenWhere) wIn.clone(),
                                   (Celest) cIn.clone());
          // System.out.printf("%s\n",oairm.c.Alpha.RoundedRAString(2,":"));
          double jd = jdstart;
          double dt = 0.005;
          double [] xy1 = {0.,0.};

          float [] dot1 = {2.0f};
          BasicStroke dotted = new BasicStroke(1.0f,BasicStroke.CAP_BUTT,
                           BasicStroke.JOIN_MITER,3.0f,dot1,0.0f);

          GeneralPath airpath = new GeneralPath();
          GeneralPath airpath2 = new GeneralPath();
//          GeneralPath airpath3 = new GeneralPath();
//          GeneralPath airpath4 = new GeneralPath();
          
          Stroke defStroke = g2.getStroke();

          boolean firstptfound = false;
          boolean uptwice = false;  
          double xpl = 0.;

          while(!firstptfound && jd < jdend) {
             oairm.w.ChangeWhen(jd);
             oairm.ComputeSky();
             // System.out.printf("%s %f\n",oairm.w.when.UTDate.RoundedCalString(0,0),oairm.airmass);
             if(oairm.airmass < ylo && oairm.airmass > 0.99 
                   && Math.abs(oairm.ha.value) < 6.) {
                // System.out.printf("firstptfound, airmass %f\n",oairm.airmass);
                firstptfound = true;
                xpl = xlo + (jd - jdstart) * 24.;
                xy1 = xytopix(xpl,oairm.airmass);
                airpath.moveTo((float) xy1[0], (float) xy1[1]);
             }
             jd += dt;
          }
          // System.out.printf("Out, airmass %f, jd %f\n",oairm.airmass,jd);
          while(oairm.airmass < ylo && oairm.airmass > 0.99 && jd < jdend
                   && Math.abs(oairm.ha.value) < 6.) {
             // System.out.printf("plt - %s %f\n",oairm.w.when.UTDate.RoundedCalString(0,0),oairm.airmass);
             oairm.w.ChangeWhen(jd);
             oairm.ComputeSky();
             xpl = xlo + (jd - jdstart) * 24.;
             xy1 = xytopix(xpl,oairm.airmass);
             if (oairm.airmass > 0.99 && oairm.airmass < ylo 
                   && Math.abs(oairm.ha.value) < 6.) {   // don't follow it past max airm 
               //if(Math.abs(oairm.ha.value) > 6.) g2.setStroke(dotted);
               //else g2.setStroke(defStroke);
               airpath.lineTo((float) xy1[0], (float) xy1[1]);
             }
             jd += dt;
          }
          
         
          if(firstptfound) {  // label tracks near their ends ....
             if(jd > jdend) puttext(xpl - 0.5, oairm.airmass, objectname, 
                        smallfont, smallfontmetrics);
             else if (oairm.airmass > ylo) puttext(xpl + 0.2, ylo-0.05, objectname, 
                        smallfont, smallfontmetrics);
             else puttext(xpl + 0.2, oairm.airmass, objectname, smallfont, 
                        smallfontmetrics);
          }   
          
          // objects can come up a second time ... 

          firstptfound = false;
          while(!firstptfound && jd < jdend) {
             oairm.w.ChangeWhen(jd);
             oairm.ComputeSky();
             if(oairm.airmass < ylo && oairm.airmass > 0.99 
                   && Math.abs(oairm.ha.value) < 6.) {
                firstptfound = true;
                uptwice = true;                
                xpl = xlo + (jd - jdstart) * 24.;
                xy1 = xytopix(xpl,oairm.airmass);
                airpath2.moveTo((float) xy1[0], (float) xy1[1]);
             }
             jd += dt;
          }
          while(oairm.airmass < ylo && oairm.airmass > 0.99 && jd < jdend
                   && Math.abs(oairm.ha.value) < 6.) {
             oairm.w.ChangeWhen(jd);
             oairm.ComputeSky();
             xpl = xlo + (jd - jdstart) * 24.;
             xy1 = xytopix(xpl,oairm.airmass);
             if (oairm.airmass > 0.99 && oairm.airmass < ylo) {   // don't follow it past 3.5
               //if(Math.abs(oairm.ha.value) > 6.) g2.setStroke(dotted);
               //else g2.setStroke(defStroke);
               airpath2.lineTo((float) xy1[0], (float) xy1[1]);
             }
             jd += dt;
          }
          // now we're sure to have it all.
          g2.draw(airpath);
          if(uptwice) g2.draw(airpath2);  // second part, if there is one.
          
          g2.setPaint(Color.WHITE);
       } 

       void plotmoon() {

          Observation omoon = new Observation(w,w.moon.topopos);
          omoon.ComputeSky();
          omoon.ComputeSunMoon();

          int nseg = 21;
          
          double [][] limbxy = new double [2][nseg];    // in system where cusp axis = y
          double [][] termxy = new double [2][nseg];

          double [][] limbxypr = new double [2][nseg];  // in system where North = y         
          double [][] termxypr = new double [2][nseg];
          
          double theta;
          double cos_sunmoon;
          int i, j, k;

          // set up points describing terminator and limb -- then rotate into
          // position.
          cos_sunmoon = Math.cos(omoon.w.sunmoon / Const.DEG_IN_RADIAN);
          // System.out.printf("sunmoon %f cos_sunmoon %f\n",o.w.sunmoon,cos_sunmoon);
          double dtheta = Const.PI / (double)(nseg - 1);
          for(j = 0; j < nseg; j++) {
             theta = dtheta * (double) j;
             limbxy[0][j] = Math.cos(theta);
             limbxy[1][j] = Math.sin(theta);
             termxy[0][j] = limbxy[0][j];   // need a second copy later
             termxy[1][j] = limbxy[1][j] * cos_sunmoon;
          }

          // rotate to appropriate position angle

//          double pa = w.cusppa + (Const.PI)/2. + 
 //                    omoon.parallactic / Const.DEG_IN_RADIAN;   
//          double pa = w.cusppa - 
 //                   omoon.parallactic / Const.DEG_IN_RADIAN;   
          double pa = omoon.w.cusppa - (Const.PI)/2.; 
                                // cusppa already in rad.
          if(w.where.lat.value < 0.) pa += Const.PI;  // flip in S.
          double[][] turnmoon = {{Math.cos(pa),Math.sin(pa)},
                                   {-1. * Math.sin(pa),Math.cos(pa)}};
          for(j = 0; j < nseg; j++) {
             for(i = 0; i < 2; i++) {
                limbxypr[i][j] = 0.;
                termxypr[i][j] = 0.;
                for (k = 0; k < 2; k++) {
                   limbxypr[i][j] += turnmoon[i][k] * limbxy[k][j];
                   termxypr[i][j] += turnmoon[i][k] * termxy[k][j];
                }
             }
          }

          double ycent = ylo + (omoon.w.altmoon / 90.) * (yhi - ylo);  // scale ...
          double xcent = xlo + (omoon.w.when.jd - jdstart) * 24.;
          double [] xypix = xytopix(xcent, ycent);
//          System.out.printf("jd %f xcent ycent %f %f altmoon %f \n",
//                          o.w.when.jd,xcent,ycent,omoon.w.altmoon);
//          System.out.printf(" -> pix %f %f\n",xypix[0],xypix[1]);
          double moonsizeinpix = 8.;  // radius
        // double moonsize = 3.;  

          GeneralPath limbpath = new GeneralPath();
          GeneralPath termpath = new GeneralPath();
          
          limbpath.moveTo((float) (xypix[0] + limbxypr[0][0] * moonsizeinpix),
                          (float) (xypix[1] + limbxypr[1][0] * moonsizeinpix));

          termpath.moveTo((float) (xypix[0] + termxypr[0][0] * moonsizeinpix),
                          (float) (xypix[1] + termxypr[1][0] * moonsizeinpix));

          double x1, y1, x2, y2;

          for(i = 1; i < nseg-1; i = i + 2) {
            x1 =  xypix[0] + termxypr[0][i] * moonsizeinpix;
            y1 =  xypix[1] + termxypr[1][i] * moonsizeinpix;
            x2 =  xypix[0] + termxypr[0][i+1] * moonsizeinpix;
            y2 =  xypix[1] + termxypr[1][i+1] * moonsizeinpix;
            termpath.quadTo((float) x1, (float) y1, (float) x2, (float) y2);
             
            x1 =  xypix[0] + limbxypr[0][i] * moonsizeinpix;
            y1 =  xypix[1] + limbxypr[1][i] * moonsizeinpix;
            x2 =  xypix[0] + limbxypr[0][i+1] * moonsizeinpix;
            y2 =  xypix[1] + limbxypr[1][i+1] * moonsizeinpix;
            limbpath.quadTo((float) x1, (float) y1, (float) x2, (float) y2);

         }

          g2.setPaint(brightyellow);
          g2.draw(limbpath);
          g2.draw(termpath);
          g2.setPaint(Color.WHITE);
       }
    }
}
