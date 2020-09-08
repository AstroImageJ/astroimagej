package Astronomy;// Set_World_Coordinates.java

import ij.*;
import ij.gui.*;
import ij.plugin.*;

import astroj.WCS;

/**
 * Setup plug-in for WCS use.
 *
 * @author F.V. Hessman, Georg-August-Universitaet Goettingen
 * @version 1.0
 * @date 2008-Feb-07
 */
public class Set_World_Coordinates implements PlugIn
	{

	/**
	 * Standard ImageJ PluginFilter setup routine which also determines the default aperture radius.
	 */
	public void run (String arg)
		{
		// String longs = Prefs.get (WCS.PREFS_LONGITUDES, "RA---TAN");
		// String lats  = Prefs.get (WCS.PREFS_LATITUDES,  "DEC--TAN");
		// String crdelt1 = Prefs.get (WCS.PREFS_CRDELT1, "1.0");
		// String crdelt2 = Prefs.get (WCS.PREFS_CRDELT2, "1.0");
		String npix1 = Prefs.get (WCS.PREFS_NPIX1, "1024");
		String npix2 = Prefs.get (WCS.PREFS_NPIX2, "1024");
		String crpix1 = Prefs.get (WCS.PREFS_CRPIX1, "566.6399");
		String crpix2 = Prefs.get (WCS.PREFS_CRPIX2, "493.2029");
		String cd1_1 = Prefs.get (WCS.PREFS_CD1_1, "-6.382004e-7");
		String cd1_2 = Prefs.get (WCS.PREFS_CD1_2, "+8.858990e-5");
		String cd2_1 = Prefs.get (WCS.PREFS_CD2_1, "-8.858990e-5");
		String cd2_2 = Prefs.get (WCS.PREFS_CD2_2, "-6.382004e-7");

		GenericDialog gd = new GenericDialog ("WCS Options");

		// gd.addMessage ("Default coordinate scales");
		// gd.addStringField ("CRDELT1",crdelt1,16);
		// gd.addStringField ("CRDELT2",crdelt2,16);

		gd.addMessage ("Default image size");
		gd.addStringField ("NPIX1",npix1,16);
		gd.addStringField ("NPIX2",npix2,16);

		gd.addMessage ("Default coordinate origin");
		gd.addStringField ("CRPIX1",crpix1,16);
		gd.addStringField ("CRPIX2",crpix2,16);

		gd.addMessage ("Default linear transformation");
		gd.addStringField ("CD1_1",cd1_1,16);
		gd.addStringField ("CD1_2",cd1_2,16);
		gd.addStringField ("CD2_1",cd2_1,16);
		gd.addStringField ("CD2_2",cd2_2,16);

		gd.showDialog();
		if (gd.wasCanceled()) return;

		npix1 = gd.getNextString();
		npix2 = gd.getNextString();
		crpix1 = gd.getNextString();
		crpix2 = gd.getNextString();
		cd1_1 = gd.getNextString();
		cd1_2 = gd.getNextString();
		cd2_1 = gd.getNextString();
		cd2_2 = gd.getNextString();

		Prefs.set (WCS.PREFS_NPIX1, npix1);
		Prefs.set (WCS.PREFS_NPIX2, npix2);
		Prefs.set (WCS.PREFS_CRPIX1, crpix1);
		Prefs.set (WCS.PREFS_CRPIX2, crpix2);
		Prefs.set (WCS.PREFS_CD1_1, cd1_1);
		Prefs.set (WCS.PREFS_CD1_2, cd1_2);
		Prefs.set (WCS.PREFS_CD2_1, cd2_1);
		Prefs.set (WCS.PREFS_CD2_2, cd2_2);
		}
	}
