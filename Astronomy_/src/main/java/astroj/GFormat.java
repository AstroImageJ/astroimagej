// GFormat.java

package astroj;

import java.text.DecimalFormat;

/**
 * Provides C/FORTRAN-like g-formatting and can easily be changed along the way via a setPlaces(String places) invocation.
 *
 * @author F.V. Hessman, Georg-August-Universitaet Goettingen
 * @date 2006-02-15T15:21
 * @version 1.0
 */
public class GFormat
	{
	private DecimalFormat e;
	private DecimalFormat f;
	private double limit;

	public GFormat (String places)	// USUAL C/FORTRAN FORMAT:   7.2 => 7 INTEGER, 2 DECIMAL PLACES
		{
		setPlaces(places);
		}

	public void setPlaces (String places)
		{
		int i;
		int iplaces=6;
		int dplaces=3;
		try	{
			i=places.indexOf(".");
			i=places.indexOf(".");
			iplaces = Integer.parseInt(places.substring(0,i++));
			dplaces = Integer.parseInt(places.substring(i));
			}
		catch (NumberFormatException e) {}

		String dpattern = "";
		for (i=0; i < dplaces; i++)
			dpattern += "0";
		String epattern = "0."+dpattern+"E0";
		e = new DecimalFormat(epattern);

		String fpattern = "";
		for (i=0; i < iplaces-1; i++)
			fpattern += "#";
		fpattern += "0."+dpattern;
		f = new DecimalFormat(fpattern);

		if (dplaces < 2) dplaces=3;
		limit = Math.pow(10.0,-(dplaces-2));
		}

	public String format (double d)
		{
		String eString = e.format(d);
		String fString = f.format(d);
		double absd = Math.abs(d);
		if (absd < limit || eString.length() < fString.length())
			return eString.replace(",",".");
		else
			return fString.replace(",",".");
		}

	public String format (float d)
		{
		String eString = e.format(d);
		String fString = f.format(d);
		double absd = Math.abs(d);
		if (absd < limit || eString.length() < fString.length())
			return eString.replace(",",".");
		else
			return fString.replace(",",".");
		}

	public static void main (String[] args)
		{
		double[] tests = {0.0, -333.333, 4444.4444, -55555.55555, 666666.666666, -7777777.7777777, 88888888.88888888,
					-0.1, 0.02, -0.003, 0.0004, -0.00005, 0.000006, -0.0000007, 0.00000008, -0.000000009};
		GFormat g = new GFormat("7.5");
		for (int i=0; i < tests.length; i++)
			System.out.println("g-Formatted form of "+tests[i]+" is "+g.format(tests[i]));
		}
	}
