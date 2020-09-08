package Astronomy;// Read_FITS_Header.java

import ij.*;
import ij.gui.*;
import ij.plugin.*;

import astroj.*;

/**
 * Extracts a FITS card value as a string from an image given the keyword.
 */
public class Read_FITS_Header implements PlugIn
	{
	public void run(String arg)
		{
		ImagePlus img = WindowManager.getCurrentImage();
		if (img == null) return;
		String title = "";
		if (img != null) title = img.getTitle();

		GenericDialog gd = new GenericDialog ("Read FITS Header");
		gd.addStringField("Image:",title,20);
		gd.addStringField("FITS keyword:","EXPTIME");
		gd.showDialog();
		if (gd.wasCanceled()) return;
		title = gd.getNextString();
		String key = gd.getNextString();

		String result = FitsJ.getCardValueFromImage(key,title);
		IJ.showMessage(key+" = "+result);
		}
	}
