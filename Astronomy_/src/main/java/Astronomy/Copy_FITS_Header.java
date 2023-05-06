package Astronomy;// Copy_FITS_Header.java

import astroj.FitsJ;
import astroj.IJU;
import ij.IJ;
import ij.ImagePlus;
import ij.WindowManager;
import ij.gui.GenericDialog;
import ij.plugin.PlugIn;

/**
 * Copies the FITS header from one image to another.  Optionally, a FITS HISTORY entry can be made.
 * 
 * @author F.V. Hessman
 * @date 2008-08-05
 * @version 1.0
 */
public class Copy_FITS_Header implements PlugIn 
	{
	static String OPTIONAL_HISTORY_ENTRY = new String("Your text for an optional history entry here!");

	public void run (String copyTo_ImageName)
		{
		FitsJ.Header header = null;
        
		// GET LIST OF CURRENT IMAGES

		String[] images = IJU.listOfOpenImages(null);	// displayedImages();
		if (images.length < 2)
			{
			IJ.showMessage("Copy FITS Header","Not enough images are open.");
			return;
			}

		// RUN DIALOG
        String image1 = images[0];
        String image2 = images[1];
        if (copyTo_ImageName != null && !copyTo_ImageName.equals(""))
            {
            image2 = copyTo_ImageName;
            if (image1.equals(image2)) image1 = images[1];
            }

		GenericDialog gd = new GenericDialog("Copy FITS Header");
		gd.addChoice ("from",images,image1);
		gd.addChoice ("to",images,image2);
        gd.addMessage ("Optional entry:");
		gd.addStringField("HISTORY",OPTIONAL_HISTORY_ENTRY,40);
		gd.showDialog();
		if (gd.wasCanceled()) return;
		image1 = gd.getNextChoice();
		image2 = gd.getNextChoice();
		String history = gd.getNextString();

		// CONNECT TO IMAGES, GET FITS HEADER

		ImagePlus img1 = WindowManager.getImage (image1);
		ImagePlus img2 = WindowManager.getImage (image2);
		if (img1 == null || img2 == null)
			{
			IJ.showMessage ("Unable to access selected images!");
			return;
			}

		header = FitsJ.getHeader(img1);
		if (header == null || header.cards().length == 0)
			{
			IJ.showMessage ("Unable to access FITS header from image "+image1);
			return;
			}

		// ADD OPTIONAL HISTORY

		if (history != null && history.length() > 0 && !history.equals(OPTIONAL_HISTORY_ENTRY))
			{
			// IJ.log("Adding optional history entry: "+history);
			header = FitsJ.addHistory(history,header);
			}

		// COPY HEADER

		FitsJ.putHeader(img2,header);
		}
	}
