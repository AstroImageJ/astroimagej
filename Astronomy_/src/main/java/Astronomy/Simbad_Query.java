package Astronomy;// Simbad_Query.java

import astroj.*;
import ij.IJ;
import ij.ImagePlus;
import ij.gui.GenericDialog;
import ij.gui.ImageCanvas;
import ij.plugin.filter.PlugInFilter;
import ij.process.ImageProcessor;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;

public class Simbad_Query implements PlugInFilter
	{
	public ImagePlus img;
	ImageCanvas canvas;
	OverlayCanvas ocanvas;

	protected FitsJ.Header header = null;
	protected String ra = "12:34:56.7";
	protected String dec = "+76:54:32.1";
	protected double radius = 10.0;		// ARCMINUTES
	protected String[] response = null;
	protected WCS wcs = null;

	static String IDENT = "Identifier";
	static String RA = "R.A. [hr]";
	static String DEC = "Decl. [deg]";
	static String TYPE = "Star?";
	static String XPIXEL = "X";
	static String YPIXEL = "Y";

	protected String url = "http://simbad.u-strasbg.fr/simbad/sim-coo?"
		+ "CooFrame=ICRS&"
		+ "output.format=ASCII_TAB&"
		+ "Radius.unit=arcmin&";
	//	+ "Radius=10&Coord=20:54:05+37:01:17"

	public int setup(String arg, ImagePlus img)
		{
		this.img = img;
		return DOES_ALL;
		}

	public void run(ImageProcessor ip)
		{
		wcs = new WCS(img);
		if (img != null && ip != null)
			header = FitsJ.getHeader(img);
		if (header == null || header.cards().length == 0)
			{
			if (! dialog()) return;
			}
		else	{
			int ny = this.img.getHeight();	// PIXELS
			double scale = 0.3/60.;		// ARCMIN/PIXEL
			int card = FitsJ.findCardWithKey("CDELT2",header);
			if (card > 0)
				{
				scale = FitsJ.getCardDoubleValue(header.cards()[card])*60.;
				radius = ny*scale;
				}

			int racard = FitsJ.findCardWithKey("RA",header);
			int deccard = FitsJ.findCardWithKey("DEC",header);
			if (racard > 0 && deccard > 0)
				{
				ra = FitsJ.getCardStringValue(header.cards()[racard]).trim();
				dec = FitsJ.getCardStringValue(header.cards()[deccard]).trim();
				}
			else	{
				racard = FitsJ.findCardWithKey("CRVAL1",header);
				deccard = FitsJ.findCardWithKey("CRVAL2",header);
				int typcard = FitsJ.findCardWithKey("CTYPE1",header);
				if (racard > 0 && deccard > 0 && typcard > 0)
					{
					ra = FitsJ.getCardStringValue(header.cards()[racard]).trim();
					dec = FitsJ.getCardStringValue(header.cards()[deccard]).trim();
					String typ = FitsJ.getCardStringValue(header.cards()[typcard]).trim();
					if (typ.startsWith("RA"))
						{
						try	{
							double rad = Double.parseDouble(ra)/15.;
							ra = dms(rad);
							double decd = Double.parseDouble(dec);
							dec = dms(decd);
							}
						catch (NumberFormatException e)
							{
							IJ.error(e.getMessage());
							return;
							}
						}
					}
				}
			}
		if (! dialog()) return;
		askSimbad();
		if (response != null)
			findEntries();
		}

	public String dms (double degs)
		{
		String sgn = "+";
		if (degs < 0.) sgn = "-";
		double adegs = Math.abs(degs);
		int d = (int)adegs;
		int m = (int)((adegs-d)*60.);
		double s = (adegs-d-m/60.)*3600.;
		String mm = ""+m;
		if (m < 10) mm = "0"+m;
		String ss = ""+s;
		if (s < 10.) ss = "0"+s;
		return sgn+d+":"+mm+":"+ss;
		}

	protected boolean dialog()
		{
		GenericDialog gd = new GenericDialog("Simbad Query");
		gd.addStringField(RA,ra,12);
		gd.addStringField(DEC,dec,12);
		gd.addNumericField("Radius [arcmin]",radius,2);
		gd.showDialog();
		if (gd.wasCanceled()) return false;
		ra = gd.getNextString().trim();
		dec = gd.getNextString().trim();
		radius = (int)gd.getNextNumber();
		return true;
		}

	protected void askSimbad()
		{
		String query = url+"Radius="+radius+"&Coord="+ra;
		if (dec.startsWith("-") || dec.startsWith("+"))
			query += dec;
		else
			query += "+"+dec;
		IJ.log("SIMBAD Query: "+query);
		try	{
			URL simbad = new URL(query);
			BufferedReader out = new BufferedReader(new InputStreamReader(simbad.openStream()));
			String answer = "";
			String chunk = "";
			while ((chunk = out.readLine()) != null)
				{
				IJ.log(chunk);
				answer += chunk+"\n";
				}
			out.close();
			response = answer.split("\n");
			}
		catch (Exception e)
			{
			IJ.error("Can't read SIMBAD response!\n:"+e.getMessage());
			}
		}

	protected void findEntries()
		{
                canvas = img.getCanvas();
                ocanvas = OverlayCanvas.getOverlayCanvas (img);
		MeasurementTable table = new MeasurementTable("SIMBAD Query");
		int nx = img.getWidth();
		int ny = img.getHeight();

		int l = response.length;
		int k = 0;
		while (k < l && ! response[k].trim().startsWith("#"))
			{
			k++;
			}
		if (k >= l)
			{
			IJ.beep();
			IJ.log("No info left after #? l="+l+", k="+k);
			return;
			}
		String[] parts = response[k].split("\t");
		// DEPRECATED!
		// table.setHeading(1,IDENT);
		// table.setHeading(2,RA);
		// table.setHeading(3,DEC);
		// table.setHeading(4,TYPE);
		parts = null;

		k += 2;		// SKIP COMMENT AND "------" LINE

		while (k < l && ! response[k].trim().startsWith("="))
			{
			parts = response[k].split("\t");
			if (parts.length >= 5)
				{
				int i = parts[4].indexOf("-");
				if (i < 0) i=parts[4].indexOf("+");
				String r = parts[4].substring(0,i);
				String d = parts[4].substring(i);
				double ra = DmsFormat.unformat(r);
				double dec = DmsFormat.unformat(d);

				table.incrementCounter();
				table.addLabel (IDENT,parts[2]);
				table.addValue (RA,ra);
				table.addValue (DEC,dec);

				if (parts[3].contains("*"))
					table.addValue (TYPE,1);
				else
					table.addValue (TYPE,0);

				double[] rd = new double[2];
				rd[0] = ra*15.; rd[1] = dec;
				double[] xy = wcs.wcs2pixels(rd);
				table.addValue (XPIXEL, xy[0]);
				table.addValue (YPIXEL, xy[1]);

				// CREATE ROI TO MARK POSITION
				if (xy[0] > -1.0 && xy[0] < nx &&
				    xy[1] > -1.0 && xy[1] < ny)
					{
					StringRoi sroi = new StringRoi (5+(int)xy[0],5+(int)xy[1],parts[2]);
					sroi.setImage (img);
					ocanvas.add (sroi);
					ocanvas.repaint();
					}
				}
			else	{
				IJ.beep();
				IJ.log("Cannot process SIMBAD: "+response[k]);
				}
			k++;
			}
		table.show();
		}
	}
