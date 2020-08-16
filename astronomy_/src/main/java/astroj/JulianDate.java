// JulianDate.java

package astroj;

import java.util.Calendar;
import java.text.NumberFormat;
// import java.text.DecimalFormat;

/**
 * Static methods back and forth from fractional Julian day numbers.
 *
 * Derived from http://www.rgagnon.com/javadetails/java-0506.html
 * Note that JD begins at noon of each day.
 * Positive years signifies A.D., negative years B.C.
 * Remember that the year after 1 B.C. was 1 A.D.
 *
 * References :
 *  Numerical Recipes in C, 2nd ed., Cambridge University Press 1992
 */
public class JulianDate
	{
	// Gregorian Calendar adopted Oct. 15, 1582 (2299161)

	/**
	 * Gets JD from a dateTime string.
	 */
	public static double JD (String dt)
		{
		if (dt == null) return Double.NaN;
		int[] YMDhmsm = new int[7];
		try	{
			int i1=dt.indexOf("-");
			if (i1 < 0) return Double.NaN;
			YMDhmsm[0] = Integer.parseInt(dt.substring(0,i1));
			i1++;
			int i2=dt.indexOf("-",i1);
			if (i2 < 0) return Double.NaN;
			YMDhmsm[1] = Integer.parseInt(dt.substring(i1,i2));
			i1=i2+1;
			i2=dt.indexOf("T",i1);
			if (i2 < 0) return JD(YMDhmsm);
			YMDhmsm[2] = Integer.parseInt(dt.substring(i1,i2));
			i1=i2+1;
			i2=dt.indexOf(":",i1);
			if (i2 < 0) return Double.NaN;
			YMDhmsm[3] = Integer.parseInt(dt.substring(i1,i2));
			i1=i2+1;
			i2=dt.indexOf(":",i1);
			if (i2 < 0) return Double.NaN;
			YMDhmsm[4] = Integer.parseInt(dt.substring(i1,i2));
			double secs = Double.parseDouble(dt.substring(i2+1));
			YMDhmsm[5] = (int)(secs+0.5);
			YMDhmsm[6] = (int)(1000*(secs-(double)YMDhmsm[5]));
			return JD(YMDhmsm);
			}
		catch (NumberFormatException e)
			{
			return Double.NaN;
			}
		}

	/**
	 * Gets JD from a Calendar object.
	 */
	public static double JD (Calendar cal)
		{
		int[] YMDhmsm = new int[]{
			cal.get(Calendar.YEAR),
			cal.get(Calendar.MONTH)+1,
			cal.get(Calendar.DAY_OF_MONTH),
			cal.get(Calendar.HOUR_OF_DAY),
			cal.get(Calendar.MINUTE),
			cal.get(Calendar.SECOND),
			cal.get(Calendar.MILLISECOND)
			};
		return JD(YMDhmsm);
		}

	/**
	 * Gets JD from integer year,month,day,hour,minute,second,millisecond array.
	 */
	public static double JD (int[] YMDhmsm)
		{
		if (YMDhmsm == null) return Double.NaN;

		int ijd = julday(YMDhmsm);	// STANDARD NR INTEGER JD AT NOON OF GIVEN DAY

		// CONSTRUCT THE REST

		int hour=0;
		int minute=0;
		int second=0;
		int millisecond=0;
		if (YMDhmsm.length > 3)
			hour=YMDhmsm[3];
		if (YMDhmsm.length > 4)
			minute=YMDhmsm[4];
		if (YMDhmsm.length > 5)
			second=YMDhmsm[5];
		if (YMDhmsm.length > 6)
			millisecond=YMDhmsm[6];

		double julian = (double)ijd+(hour+(double)minute/60.0+(second+0.001*(double)millisecond)/3600.0)/24.0;
		julian -= 0.5;

		return julian;
		}

	/**
	 * Converts a Julian day to a Calendar object.
	 */
	public static Calendar calendar (double injulian)
		{
		int[] YMDhmsm = dateArray(injulian);
		Calendar cal = Calendar.getInstance();
		cal.set (Calendar.YEAR,         YMDhmsm[0]);
		cal.set (Calendar.MONTH,        YMDhmsm[1]-1);
		cal.set (Calendar.DAY_OF_MONTH, YMDhmsm[0]);
		cal.set (Calendar.HOUR,         YMDhmsm[0]);
		cal.set (Calendar.MINUTE,       YMDhmsm[0]);
		cal.set (Calendar.SECOND,       YMDhmsm[0]);
		cal.set (Calendar.MILLISECOND,  YMDhmsm[0]);
		return cal;
		}

	/**
	 * Converts a Julian day to integer year,month,day,hour,minute,second,millisecond
	 */
	public static int[] dateArray (double jd)
		{
		int ijd = (int)(jd+0.5);
		double diff = jd-(double)ijd;	// RESIDUAL DAY

		int[] darr = caldat(ijd);	// RESULT CONTAINS YEAR,MONTH,DAY AT NOON OF GIVEN DAY

		// NOW DO THE FRACTIONAL PART

		diff += 0.5;
		int hour = (int)(diff*24.0);
		diff = 24.0*diff-(double)hour;		// HOURS
		int minute = (int)(diff*60.0);
		diff = 60.0*diff-(double)minute;	// MINUTES
		int second = (int)(diff*60.0+0.5);
		diff = 60.0*diff-(double)second;	// SECONDS
		int millisecond = (int)(diff*1000.0);

		darr[3] = hour;
		darr[4] = minute;
		darr[5] = second;
		darr[6] = millisecond;
		return darr;
		}

	/**
	 * Converts a JD to a dateTime string.
	 */
	public static String dateTime (double julian)
		{
		int[] YMDhmsm = dateArray(julian);
		return dateTime (YMDhmsm);
		}

	/**
	 * Converts a dateTime array to a dateTime string.
	 */
	public static String dateTime (int[] YMDhmsm)
		{
		double secs = YMDhmsm[5]+0.001*(double)YMDhmsm[6];

		NumberFormat fi = NumberFormat.getIntegerInstance ();
		fi.setMinimumIntegerDigits (2);
		NumberFormat fd = NumberFormat.getInstance();
		fd.setMinimumIntegerDigits (2);

		// DecimalFormat f = new DecimalFormat("##");

		String sjd = new String(""
			+YMDhmsm[0]+"-"+fi.format(YMDhmsm[1])+"-"+fi.format(YMDhmsm[2])
			+"T"
			+fi.format(YMDhmsm[3])+":"+fi.format(YMDhmsm[4])+":"+fd.format(secs));
		return sjd;
		}

	/**
	 * Test of JulianDay methods.
	 */
	public static void main(String args[])
		{
		// TEST ARRAY METHOD

		System.out.println("Test #1 : Julian date for May 23, 1968, 11:59:59 : "
					+ JD( new int[] {1968,5,23,11,59,59,0} )
					+ " = slightly under 2440000");
		int results[] = dateArray(JD(new int[] {1968,5,23,11,59,59,0} ));
		double secs = results[5]+0.001*(double)results[6];
		System.out.println ("\t... back to calendar : "+dateTime(results));

		System.out.println("Test #2 : Julian date for May 23, 1968, 12:00:01 : "
					+ JD( new int[] {1968,5,23,12,0,1,0} )
					+ " = slightly over 2440000");
		results = dateArray(JD(new int[] {1968,5,23,12,00,01,0} ));
		secs = results[5]+0.001*(double)results[6];
		System.out.println ("\t... back to calendar : "+dateTime(results));

		System.out.println("Test #3 : Julian date for May 23, 1968, 23:59:59.99 : "
					+ JD( new int[] {1968,5,23,23,59,59,99} )
					+ " = slightly under 2440000.5");
		results = dateArray(JD(new int[] {1968,5,23,23,59,59,990} ));
		secs = results[5]+0.001*(double)results[6];
		System.out.println ("\t... back to calendar : "+dateTime(results));

		// TEST Calendar METHODS

		Calendar today = Calendar.getInstance();
		double todayJulian = JD(new int[]{
			today.get(Calendar.YEAR),
			today.get(Calendar.MONTH)+1, 
			today.get(Calendar.DAY_OF_MONTH),
			today.get(Calendar.HOUR),
			today.get(Calendar.MINUTE),
			today.get(Calendar.SECOND),
			today.get(Calendar.MILLISECOND)
			});
		System.out.println("Test #4 : Julian date for today : " + todayJulian);
		System.out.println ("\t... back to dateTime : "+dateTime(todayJulian));

		// TEST DATETIME METHODS

		double date1 = JD("2005-01-01T02:34:56.789");
		double date2 = date1+Math.PI;
		System.out.println("Test #5 : Between"+dateTime(date1)+" and "+dateTime(date2)+" : "
				+ (date2 - date1) + " days");
		}

	/**
	 * Returns the whole number JD at noon of the day represented by year,month,day.
	 * Numerical Recipes in C, 2nd ed., Cambridge University Press 1992
	 */
	public static int julday (int[] YMDhmsm)
		{
		int year=YMDhmsm[0];
		int month=YMDhmsm[1]; // jan=1, feb=2,... IS ONE MORE THAN THE java.util.Calendar STANDARD!!
		int day=YMDhmsm[2];

		int ja,jul,jy,jm;
		int IGREG = 15+31*(10+12*1582);

		jy = year;
		if (jy < 0) ++jy;
		if (month > 2)
			{
			jm = month+1;
			}
		else	{
			jy--;
			jm = month+13;
			}
		jul = (int)(java.lang.Math.floor(365.25*jy)+java.lang.Math.floor(30.6001*jm)+day+1720995);
		if ((day+31*(month+12*year)) >= IGREG)
			{
			ja = (int)(0.01*jy);
			jul += 2-ja+(int)(0.25*ja);
			}
		return jul;
		}

	/**
	 * Returns the year,month,day where the whole number JD took place at noon.
	 * Numerical Recipes in C, 2nd ed., Cambridge University Press 1992
	 */
	public static int[] caldat (int julian)
		{
		int ja,jalpha,jb,jc,jd,je;
		int IGREG = 2299161;

		int[] YMDhmsm = new int[7];	// ONLY NON-NR PART!

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
		jd = (int)(365*jc+(0.25*jc));
		je = (int)((jb-jd)/30.6001);
		int day = jb-jd-(int)(30.6001*je);
		int mm = je-1;
		if (mm > 12) mm -= 12;
		int iyyy = jc-4715;
		if (mm > 2) iyyy--;
		if (iyyy <= 0) iyyy--;
		if (julian < 0) iyyy -= 100*(1-julian/36525);

		YMDhmsm[0] = iyyy;
		YMDhmsm[1] = mm;
		YMDhmsm[2] = day;
		YMDhmsm[3] = 0;		// HOURS
		YMDhmsm[4] = 0;		// MINUTES
		YMDhmsm[5] = 0;		// SECONDS
		YMDhmsm[6] = 0;		// MILLISECONDS
		return YMDhmsm;
		}

	}

