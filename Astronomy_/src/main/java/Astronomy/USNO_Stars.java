package Astronomy;// USNO_Stars.java

import astroj.FitsJ;
import astroj.MeasurementTable;
import astroj.OverlayCanvas;
import astroj.WCS;
import ij.IJ;
import ij.ImagePlus;
import ij.gui.GenericDialog;
import ij.gui.ImageCanvas;
import ij.gui.OvalRoi;
import ij.plugin.filter.PlugInFilter;
import ij.process.ImageProcessor;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;

public class USNO_Stars implements PlugInFilter
	{
	public ImagePlus img;
	protected ImageCanvas canvas;
	protected OverlayCanvas ocanvas;
	protected WCS wcs = null;

	protected FitsJ.Header header = null;
	protected String ra = "12:34:56.7";
	protected String dec = "+76:54:32.1";
	protected double radius = 10.0;		// ARCMINUTES
	protected double bright = 8.0;		// BRIGHT LIMIT IN MAGN
	protected double faint  = 16.0;		// FAINT  LIMIT IN MAGN

	protected String[] catalog = null;
	protected Boolean fillTable = true;
 
	static String IDENT = "Identifier";
	static String RA = "R.A.[hr]";
	static String DEC = "Decl.[deg]";
	static String RMAG = "R[mag]";
	static String XPIXEL = "X";
	static String YPIXEL = "Y";

	protected String url1 = "http://archive.eso.org/skycat/servers/usnoa_res?catalogue=usnoa&epoch=2000.0&chart=0&format=2";
	protected String url2 = "&ra=";
	protected String url3 = "&dec=";
	protected String url4 = "&magbright=";
	protected String url5 = "&magfaint=";
	protected String url6 = "&radmax=";

	public int setup(String arg, ImagePlus img)
		{
		this.img = img;
		return DOES_ALL;
		}

	public void run(ImageProcessor ip)
		{
		wcs = new WCS(img);
		if (!getRaDecRadius() || !dialog())
			{
			img.unlock();
			return;
			}
		if (getCatalog())
			parseCatalog();
		}

	protected boolean getRaDecRadius ()
		{
		header = FitsJ.getHeader(img);
		if (header == null || header.cards().length == 0)
			return false;
		else	{
			int ny = this.img.getHeight();	// PIXELS
			double scale = 0.3/60.;		// ARCMIN/PIXEL
			int card = FitsJ.findCardWithKey("CDELT2",header);
			if (card > 0)
				{
				scale = FitsJ.getCardDoubleValue(header.cards()[card])*60.;
				radius = 1.5*ny*scale;
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
							return false;
							}
						}
					}
				}
			}
		return true;
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
		GenericDialog gd = new GenericDialog("USNO Query");
		gd.addStringField(RA,ra,12);
		gd.addStringField(DEC,dec,12);
		// gd.addNumericField("Radius [arcmin]",radius,2);
		gd.addNumericField("Bright limit [mag]",bright,2);
		gd.addNumericField("Faint  limit [mag]",faint,2);
		gd.addCheckbox("Save catalogue in table",fillTable);
		gd.showDialog();
		if (gd.wasCanceled()) return false;
		ra = gd.getNextString().trim();
		dec = gd.getNextString().trim();
		// radius = (int)gd.getNextNumber();
		bright = gd.getNextNumber();
		faint  = gd.getNextNumber();
		fillTable = gd.getNextBoolean();
		return true;
		}

	protected boolean getCatalog()
		{
		String query = url1+url2+ra.replace(":","+")+url3+dec.replace(":","+")+url4+bright+url5+faint+url6+radius;
		IJ.log("ESO Query: "+query);
		try	{
			URL eso = new URL(query);
			BufferedReader out = new BufferedReader(new InputStreamReader(eso.openStream()));
			String answer = "";
			String chunk = "";
			Boolean reached = false;
			while ((chunk = out.readLine()) != null)
				{
				if (chunk.contains("</pre>")) reached = false;
				if (reached && !chunk.equals("") && !chunk.contains("<b>"))
					{
					if (chunk.contains("</b>"))
						answer += chunk.substring(chunk.lastIndexOf(">")+1).trim()+"\n";
					else
						answer += chunk.trim()+"\n";
					// IJ.log("["+chunk+"]");
					}
				if (chunk.contains("<pre>")) reached = true;
				}
			out.close();
			catalog = answer.split("\n");
			return true;
			}
		catch (Exception e)
			{
			IJ.beep();
			IJ.error("Can't read ESO skycat response!\n:"+e.getMessage());
			return false;
			}
		}

	protected void parseCatalog()
		{
		MeasurementTable table = null;
		int nx = img.getWidth();
		int ny = img.getHeight();

		for (int k=0; k < catalog.length; k++)
			{
			String entry = catalog[k].trim().replace("   "," ").replace("  "," ");
			String[] parts = entry.split(" ");
			// IJ.log(""+k+": parts="+parts.length);
			if (parts.length < 5) break;
			// IJ.log(parts[0]+","+parts[1]+","+parts[2]+","+parts[3]+","+parts[4]);
			try	{
				String name = parts[1];
				double ra  = Double.parseDouble(parts[2]);	// IN DEG
				double dec = Double.parseDouble(parts[3]);	// IN DEG
				double mag = Double.parseDouble(parts[4]);
				double[] rd = new double[2];
				rd[0] = ra; rd[1] = dec;
				double[] xy = wcs.wcs2pixels(rd);

				if (fillTable)
					{
					if (table == null)
						table = new MeasurementTable("USNO Catalogue");
					table.incrementCounter();
					table.addLabel (IDENT,name);
					table.addValue (RA,ra);
					table.addValue (DEC,dec);
					table.addValue (RMAG,mag);
					table.addValue (XPIXEL, xy[0]);
					table.addValue (YPIXEL, xy[1]);
					}

				// CREATE ROI TO MARK POSITION
				if (xy[0] > -1.0 && xy[0] < nx &&
				    xy[1] > -1.0 && xy[1] < ny)
					{
					if (canvas == null)
						{
                				canvas = img.getCanvas();
                				ocanvas = OverlayCanvas.getOverlayCanvas (img);
						}
					OvalRoi roi = new OvalRoi ((int)xy[0]-10,(int)xy[1]-10,20,20);
					roi.setImage (img);
					ocanvas.add (roi);
					ocanvas.repaint();
					}
				}
			catch (Exception e)
				{
				IJ.beep();
				IJ.log(e.getMessage());
				IJ.log("Cannot process USNO catalog entry: "+catalog[k]);
				}
			}
		if (fillTable && table != null) table.show();
		}
	}
