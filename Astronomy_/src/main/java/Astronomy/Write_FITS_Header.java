package Astronomy;// Write_FITS_Header.java

import ij.*;
import ij.gui.*;
import ij.plugin.*;

import astroj.*;

/**
 * Writes a FITS card value to an image given a keyword, value, comment, and type.
 * Also writes FITS comments and histories.
 */
public class Write_FITS_Header implements PlugIn
	{
	public void run(String arg)
		{
		ImagePlus img = WindowManager.getCurrentImage();
		if (img == null)
			{
			IJ.error("Cannot access any images!");
			return;
			}
		String title = img.getTitle();

		ImageStack stack = null;
		int slice = 1;
		String[] hdr = null;

		GenericDialog gd = new GenericDialog ("Write FITS Header Entry");
		gd.addChoice      ("Image", IJU.listOfOpenImages(title), title);
		gd.addChoice      ("Type", new String[] {"string","integer","real number", "boolean", "COMMENT", "HISTORY"},"real number");
		gd.addStringField ("FITS keyword","EXPTIME",8);
		gd.addStringField ("Value","1.0",30);
		gd.addStringField ("Comment or COMMENT/HISTORY","Value modified by Write FITS Header plugin",40);
		gd.showDialog();
		if (gd.wasCanceled()) return;
		title          = gd.getNextChoice();
		String typ     = gd.getNextChoice();
		String key     = gd.getNextString();
		String val     = gd.getNextString();
		String comment = gd.getNextString();

		if (key.equals("COMMENT") || key.equals("comment") || key.equals("HISTORY") || key.equals("history"))
			{
			IJ.showMessage("Please use the comment field and type=COMMENT/HISTORY");
			return;
			}

		img = WindowManager.getImage(title);
		if (img == null)
			{
			IJ.error("Cannot access image "+title);
			return;
			}
		int slices = img.getImageStackSize();
		if (slices == 1)
			{
			title = img.getTitle();
			}
		else	{
			stack = img.getStack();
			slice = img.getCurrentSlice();
			title = IJU.getSliceFilename(img, slice);
			}

		hdr = FitsJ.getHeader(img);
		if (hdr == null || hdr.length == 0)
			{
			IJ.error("No FITS header for the image "+title+"!");
			return;
			}

		if (typ.equals("COMMENT"))
			hdr = FitsJ.addComment(comment,hdr);
		else if (typ.equals("HISTORY"))
			hdr = FitsJ.addHistory(comment,hdr);
		else if (typ.equals("string"))
			hdr = FitsJ.setCard(key,val,comment,hdr);
		else if (typ.equals("integer"))
			{
			int ival=0;
			try	{
				ival = Integer.parseInt(val);
				}
			catch (NumberFormatException e)
				{
				IJ.error("Cannot parse integer "+val);
				return;
				}
			hdr = FitsJ.setCard(key,ival,comment,hdr);
			}
		else if (typ.equals("real number"))
			{
			double dval = Double.NaN;
			try	{
				dval = Double.parseDouble(val);
				}
			catch (NumberFormatException e)
				{
				IJ.error("Cannot parse real number "+val);
				return;
				}
			hdr = FitsJ.setCard(key,dval,comment,hdr);
			}
		else if (typ.equals("boolean"))
			{
			if (val.startsWith("T") || val.startsWith("t"))
				hdr = FitsJ.setCard(key,true,comment,hdr);
			else if (val.startsWith("F") || val.startsWith("f"))
				hdr = FitsJ.setCard(key,false,comment,hdr);
			else	{
				IJ.error("Cannot parse boolean "+val);
				return;
				}
			}
		else	{
			IJ.error("Unknown FITS card type : "+typ);
			return;
			}
		FitsJ.putHeader(img,hdr);
		// for (int i=0; i < hdr.length; i++) IJ.log(hdr[i]);
		}
	}
