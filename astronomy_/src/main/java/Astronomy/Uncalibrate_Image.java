package Astronomy;// Uncalibrate_Image.java

import ij.*;
import ij.gui.*;
import ij.measure.*;
import ij.plugin.filter.*;
import ij.process.*;

import java.awt.*;

/**
 * Turns off image intensity calibration (e.g. for FITS files with BZERO keyword).
 */
public class Uncalibrate_Image implements PlugInFilter
	{
	ImagePlus img;

	public int setup(String arg, ImagePlus img)
		{
		this.img = img;
		return DOES_ALL;
		}

	public void run(ImageProcessor ip)
		{
		Calibration cal = img.getCalibration();
		if (cal == null) return;
		cal.setFunction(Calibration.NONE,null,null);
		}
	}
