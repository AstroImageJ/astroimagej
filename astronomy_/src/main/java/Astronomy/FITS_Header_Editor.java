package Astronomy;// FITS_Header_Editor.java

import ij.*;
import ij.process.*;
import ij.plugin.filter.*;

import astroj.FitsHeaderEditor;

public class FITS_Header_Editor implements PlugInFilter
	{
	ImagePlus imp;

    
	public int setup(String arg, ImagePlus imp)
		{
		this.imp = imp;
		return DOES_ALL;
		}

	public void run(ImageProcessor ip)
		{
		// PLACE FITS HEADER IN A STRING ARRAY
        FitsHeaderEditor fhe = new FitsHeaderEditor(imp);
        }
 	}
