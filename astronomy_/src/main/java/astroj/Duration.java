// Duration.java
// $Id: Duration.java,v 1.0 2004/04/14 16:00:00 fvh Exp $

package astroj;

import java.util.*;

/**
 * Quick and dirty duration parser for ISO 8601 format 
 * http://www.w3.org/TR/2001/REC-xmlschema-2-20010502/#duration 
 */
public class Duration
	{
	private int plusMinus;
	private double years;
	private double months;
	private double days;
	private double hours;
	private double minutes;
	private double seconds;

	/**
	 * Instatiates a Duration from an ISO 8601 format duration string
	 */ 
	public Duration (String isodur) throws InvalidDateException
		{
		boolean isTime = false;
		String value = null;
		String delim = null;

		plusMinus=1;
		years   = -1.0;
		months  = -1.0;
		days    = -1.0;
		hours   = -1.0;
		minutes = -1.0;
		seconds = -1.0;

		// DURATION FORMAT IS: (-)PnYnMnDTnHnMnS
		StringTokenizer st = new StringTokenizer(isodur, "-PYMDTHS", true);

		try	{

			// OPTIONAL SIGN

			value = st.nextToken();

			if (value.equals("-"))
				{
				plusMinus= -1;
				value=st.nextToken();
				}

			// DURATION MUST START WITH A "P"

			if (!value.equals("P"))
				throw new InvalidDateException(isodur+
					" : "+value+
					" : no P deliminator for duration");

			// GET NEXT FIELD

			while (st.hasMoreTokens())
				{

				// VALUE

	    			value = new String(st.nextToken());
				if (value.equals("T"))
					{
					if (!st.hasMoreTokens())
						throw new InvalidDateException(isodur+
							" : "+value+
							": no values after duration T delimitor");
					value = st.nextToken();
					isTime = true;
					}

				// DELIMINATOR

				if (!st.hasMoreTokens())
					throw new InvalidDateException(isodur+
						" : "+value+
						"No deliminator for duration");
				delim = new String(st.nextToken());

				// YEAR

				if (delim.equals("Y"))
					{
					years = Double.parseDouble(value);
					}

				// MONTH

				else if (delim.equals("M") && isTime == false)
					{
					months = Double.parseDouble(value);
					if (months != (double)((int)months))
						throw new InvalidDateException(
							"Cannot process decimal months!");
					}

				// DAYS

				else if (delim.equals("D"))
					{
					days = Double.parseDouble(value);
					}

				// HOURS

				else if (delim.equals("H"))
					{
					hours = Double.parseDouble(value);
					isTime = true;
					}

				// MINUTES

				else if (delim.equals("M") && isTime == true)
					{
					minutes = Double.parseDouble(value);
					}

				// SECONDS

				else if (delim.equals("S"))
					{
					seconds = Double.parseDouble(value);
					}
				else	{
					throw new InvalidDateException(isodur+
						": what duration delimiter is "+delim+"?");
					}
				}
			}
		catch (NumberFormatException ex)
			{
			throw new InvalidDateException("["+ex.getMessage()+
					   "] is not valid");
			}
		}

	/**
	* Add Duration to a Calendar
	* @return modified Calendar
	*/
	public Calendar addTo(Calendar cal) throws InvalidDateException
		{
		int iyears,imonths,idays,ihours,imins,isecs,millis;

		if (years > 0)
			{
			iyears = (int)years;
			imonths = 0;			// AVOID USING MONTHS
			idays  = (int)(365.254*(years
					-iyears));
			ihours = (int)(24.0*365.254*(years
					-iyears
					-idays/365.254));
			imins  = (int)(60.0*24.0*365.254*(years
					-iyears
					-idays/365.254
					-ihours/24.0/365.254));
			isecs  = (int)(60.0*60.0*24.0*365.254*(years
					-iyears
					-idays/365.254
					-ihours/24.0/365.254
					-imins/60.0/24.0/365.254));
			millis  = (int)(1000.0*60.0*60.0*24.0*365.254*(years
					-iyears
					-idays/365.254
					-ihours/24.0/365.254
					-imins/60.0/24.0/365.254
					-isecs/60.0/60.0/24.0/365.254));
			cal.add(Calendar.YEAR,        plusMinus*iyears);
			cal.add(Calendar.DAY_OF_YEAR, plusMinus*idays);
			cal.add(Calendar.HOUR,        plusMinus*ihours);
			cal.add(Calendar.MINUTE,      plusMinus*imins);
			cal.add(Calendar.SECOND,      plusMinus*isecs);
			cal.add(Calendar.MILLISECOND, plusMinus*millis);
			}
		
		if (months > 0)
			{
	 		imonths = (int)months;
			if (months != (double)imonths)
				throw new InvalidDateException(
					"Cannot add decimal months!");
			cal.add(Calendar.MONTH, plusMinus*imonths);
			}

		if (days > 0)
			{
			idays  = (int)days;
			ihours = (int)(24.0*(days
					-idays));
			imins  = (int)(60.0*24.0*(days
					-idays
					-ihours/24.0));
			isecs  = (int)(60.0*60.0*24.0*(days
					-idays
					-ihours/24.0
					-imins/24.0/60.0));
			millis  = (int)(1000.0*60.0*60.0*24.0*(days
					-idays
					-ihours/24.0
					-imins/60.0/24.0
					-isecs/60.0/60.0/24.0));
			cal.add(Calendar.DAY_OF_YEAR, plusMinus*idays);
			cal.add(Calendar.HOUR,        plusMinus*ihours);
			cal.add(Calendar.MINUTE,      plusMinus*imins);
			cal.add(Calendar.SECOND,      plusMinus*isecs);
			cal.add(Calendar.MILLISECOND, plusMinus*millis);
			}

		if (hours > 0)
			{
			ihours  = (int)hours;
			imins  = (int)(60.0*(hours
					-ihours));
			isecs  = (int)(60.0*60.0*(hours
					-ihours
					-imins/60.0));
			millis  = (int)(1000.0*60.0*60.0*(hours
					-ihours
					-imins/60.0
					-isecs/60.0/60.0));
			cal.add(Calendar.HOUR,        plusMinus*ihours);
			cal.add(Calendar.MINUTE,      plusMinus*imins);
			cal.add(Calendar.SECOND,      plusMinus*isecs);
			cal.add(Calendar.MILLISECOND, plusMinus*millis);
			}

		if (minutes > 0)
			{
			imins  = (int)minutes;
			isecs  = (int)(60.0*(minutes
					-imins));
			millis  = (int)(1000.0*60.0*(minutes
					-imins
					-isecs/60.0));
			cal.add(Calendar.MINUTE,      plusMinus*imins);
			cal.add(Calendar.SECOND,      plusMinus*isecs);
			cal.add(Calendar.MILLISECOND, plusMinus*millis);
			}

		if (seconds > 0)
			{
			isecs  = (int)seconds;
			millis  = (int)(1000.0*(seconds
					-isecs));
			cal.add(Calendar.SECOND,      plusMinus*isecs);
			cal.add(Calendar.MILLISECOND, plusMinus*millis);
			}
		return cal;
		}

	/**
	* Generate a string representation of an ISO 8601 duration
	* @return a string representing the duration in the ISO 8601 format
	*/
	public String toString()
		{
		StringBuffer buffer = new StringBuffer();

		// OPTIONAL SIGN

		if (plusMinus == -1)
			buffer.append("-");

		// REQUIRED "P"

		buffer.append("P");

		if (years > 0)
			{
			if (years == (double)((int)years))
				buffer.append((int)years);
			else
				buffer.append(years);
			buffer.append("Y");
			}
		if (months > 0)
			{
			if (months == (double)((int)months))
				buffer.append((int)months);
			else
				buffer.append(months);
			buffer.append("M");
			}
		if (days > 0)
			{
			if (days == (double)((int)days))
				buffer.append((int)days);
			else
				buffer.append(days);
			buffer.append("D");
			}

		// DATE-TIME SEPARATOR (IF NEEDED)

		if ((years > 0 || months > 0 || days > 0) &&
		    (hours > 0 || minutes > 0 || seconds > 0))
			buffer.append("T");


		if (hours > 0)
			{
			if (hours == (double)((int)hours))
				buffer.append((int)hours);
			else
				buffer.append(hours);
			buffer.append("H");
			}
		if (minutes > 0)
			{
			if (minutes == (double)((int)minutes))
				buffer.append((int)minutes);
			else
				buffer.append(minutes);
			buffer.append("M");
			}
		if (seconds > 0)
			{
			buffer.append(seconds);
			buffer.append("S");
			}

		return buffer.toString();
		}

	public static void main(String args[])
		{
		String isodur;
		Duration dur;

		isodur = new String("-P1997Y07M16DT19H20M30.45S");
		System.out.println("Testing negative duration:"+isodur);
		try	{
			dur = new Duration(isodur);
			System.out.println(isodur+"="+dur.toString());
			}
		catch (InvalidDateException e)
			{
			System.out.println(e.getMessage());
			}

		isodur = new String("P1Y2.34M5D");
		System.out.println("\nTesting decimal month error:"+isodur);
		try	{
			dur = new Duration(isodur);
			System.out.println(isodur+"="+dur.toString());
			}
		catch (InvalidDateException e)
			{
			System.out.println(e.getMessage());
			}

		isodur = new String("P4H5M6.789S");
		System.out.println("\nTesting date versus time:"+isodur);
		try	{
			dur = new Duration(isodur);
			System.out.println(isodur+"="+dur.toString());
			}
		catch (InvalidDateException e)
			{
			System.out.println(e.getMessage());
			}

		isodur = new String("P1M2DT4H5M0.789S");
		System.out.println("\nTesting addition to calendar date:"+isodur);
		try	{
			Calendar cal = new GregorianCalendar(TimeZone.getTimeZone("UTC"));
			dur = new Duration(isodur);
			System.out.println(
				"["+cal.getTime().toString()+
				"] + "+dur.toString());
			dur.addTo(cal);
			System.out.println(
				" = ["+cal.getTime().toString()+"]"
				);
			}
		catch (InvalidDateException e)
			{
			System.out.println(e.getMessage());
			}

		isodur = new String("-P1M2DT4H5M0.789S");
		System.out.println("\nTesting subtraction from calendar date:"+isodur);
		try	{
			Calendar cal = new GregorianCalendar(TimeZone.getTimeZone("UTC"));
			dur = new Duration(isodur);
			System.out.println(
				"["+cal.getTime().toString()+
				"] + "+dur.toString());
			dur.addTo(cal);
			System.out.println(
				" = ["+cal.getTime().toString()+"]"
				);
			}
		catch (InvalidDateException e)
			{
			System.out.println(e.getMessage());
			}

		}

	}

