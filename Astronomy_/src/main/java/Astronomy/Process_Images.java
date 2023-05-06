package Astronomy;// Process_Images.java

import astroj.FitsJ;
import astroj.IJU;
import ij.*;
import ij.gui.GenericDialog;
import ij.plugin.PlugIn;
import ij.process.ImageConverter;
import ij.process.ImageProcessor;
import ij.process.StackConverter;

/**
 * Plugin that processes/calibrates CCD images.
 *
 * @date 2008-DEC-17
 * @name F.V. Hessman (Univ. Goettingen)
 * @version 1.2
 */
public class Process_Images implements PlugIn
	{
	ImagePlus rawImage;

	boolean flatCorrection,biasCorrection,darkCorrection,expCorrection,overCorrection,cosmicsCorrection;
	boolean newimage = true;
	boolean pipeline = false;

	String overscan = "";

	String flat = null;
	String bias = null;
	String dark = null;
	String raw  = null;
	String result=null;

	ImagePlus flatImage = null;
	ImagePlus biasImage = null;
	ImagePlus darkImage = null;
	ImagePlus resultImage = null;

	ImageProcessor rawp = null;
	ImageProcessor biasp = null;
	ImageProcessor darkp = null;
	ImageProcessor flatp = null;
	ImageProcessor resultp = null;

	int h,w;
	int slices,rawDepth;

	int row1,row2,col1,col2;

	public static String PREFS_CCD_BIAS = "BIAS";
	public static String PREFS_CCD_DARK = "DARK";
	public static String PREFS_CCD_FLAT = "FLATFIELD";

	FitsJ.Header hdr = null;

	float darktime = 1.0f;
	float[] rawtimes;

	/**
	 * Perform all the necessary steps (ignores the ImageProcessor argument).
	 */
	public void run(String arg)
		{
		getPreferences();
		if (! doDialog()) return;
		if (! getImages()) return;
		if (! process()) return;
		if (newimage)
			resultImage.show();
		else
			rawImage.show();
		savePreferences();

		rawImage.unlock();
		if (biasImage != null) biasImage.unlock();
		if (darkImage != null) darkImage.unlock();
		if (flatImage != null) flatImage.unlock();
		if (resultImage != null) resultImage.unlock();
		}

	/**
	 * Get preferences: names of files and options.
	 */
	protected void getPreferences()
		{
		biasCorrection = Prefs.get("ccd.biascorr",false);
		bias = Prefs.get("ccd.bias",PREFS_CCD_BIAS);

		darkCorrection = Prefs.get("ccd.darkcorr",false);
		dark = Prefs.get ("ccd.dark",PREFS_CCD_DARK);

		flatCorrection = Prefs.get("ccd.flatcorr",false);
		flat = Prefs.get("ccd.flat",PREFS_CCD_FLAT);

		expCorrection = Prefs.get("ccd.expcorr",false);

		overCorrection = Prefs.get("ccd.overcorr",false);
		cosmicsCorrection = Prefs.get("ccd.cosmicscorr",false);
		overscan = Prefs.get ("ccd.overscan",overscan);
		}

	/**
	 * Dialogue that lets the user input options and image names.
	 */
	protected boolean doDialog()
		{
		String r,b,d,f;

		String[] images = IJU.listOfOpenImages("");
		if (raw == null || raw.trim().length() == 0)
			{
			ImagePlus im = WindowManager.getCurrentImage();
			if (im != null) raw = im.getTitle();
			}
		if (raw == null)
			{
			IJ.error("No images to process!");
			return false;
			}
		if (result == null)
			result = "Processed "+raw;

		GenericDialog gd = new GenericDialog("Process Images");

		gd.addCheckbox ("Create new image/stack",newimage);			// 1
		gd.addStringField ("Name of new image/stack",result,20);		// 2

		if (contains(images,raw))
			gd.addChoice ("Image or stack to be processed",images,raw);	// 3
		else
			gd.addChoice ("Image or stack to be processed",images,"");

		gd.addCheckbox ("Overscan bias in pixel area",overCorrection);		// 4
		gd.addStringField ("within row1,row2,col1,col2",overscan,20);		// 5

		gd.addCheckbox ("Subtract bias image",biasCorrection);			// 6
		if (biasCorrection && contains(images,bias))
			gd.addChoice ("bias image",images,bias);			// 7
		else
			gd.addChoice ("bias image",images,"");

		gd.addCheckbox ("Remove-dark-current",darkCorrection);			// 8
		if (darkCorrection && contains(images,dark))
			gd.addChoice ("dark image",images,dark);			// 9
		else
			gd.addChoice ("dark image",images,"");
		gd.addCheckbox ("Correct for exposure times",expCorrection);		// 10
 
		gd.addCheckbox ("Divide by flatfield",flatCorrection);			// 11
		if (flatCorrection && contains(images,flat))
			gd.addChoice ("flat image",images,flat);			// 12
		else
			gd.addChoice ("flat image",images,"");

		gd.addCheckbox ("Remove-cosmic ray hits",cosmicsCorrection);		// 13

		gd.showDialog();
		if (gd.wasCanceled()) return false;

		newimage = gd.getNextBoolean();						// 1
		result = gd.getNextString();						// 2

		r = gd.getNextChoice();							// 3
		if (r != null && r.trim().length() != 0) raw=r;

		overCorrection = gd.getNextBoolean();					// 4
		overscan = gd.getNextString();						// 5
		if (overCorrection)
			{
			int i1 = overscan.indexOf(",");
			int i2 = overscan.indexOf(",",i1+1);
			int i3 = overscan.indexOf(",",i2+1);
			if (i1 < 0 || i2 < 0 || i3 < 0)
				{
				IJ.showMessage("Cannot interpret overscan region : \""+overscan+"\"");
				return false;
				}
			try	{
				row1 = Integer.parseInt(overscan.substring(0,i1));
				row2 = Integer.parseInt(overscan.substring(i1+1,i2));
				col1 = Integer.parseInt(overscan.substring(i2+1,i3));
				col2 = Integer.parseInt(overscan.substring(i3+1));
				}
			catch (NumberFormatException e)
				{
				IJ.showMessage("Cannot interpret overscan region : "+overscan);
				return false;
				}
			}

		biasCorrection = gd.getNextBoolean();					// 6
		b = gd.getNextChoice();							// 7
		if (b != null && b.trim().length() != 0) bias=b;

		darkCorrection = gd.getNextBoolean();					// 8
		d = gd.getNextChoice();							// 9
		if (d != null && d.trim().length() != 0) dark=d;

		expCorrection = gd.getNextBoolean();					// 10

		flatCorrection = gd.getNextBoolean();					// 11
		f = gd.getNextChoice();							// 12
		if (f != null && f.trim().length() != 0) flat=f;

		cosmicsCorrection = gd.getNextBoolean();				// 13
		return true;
		}

	/**
	 * Given the options and input names, retrieves the images (must be active!).
	 */
	protected boolean getImages()
		{
		// GET RAW IMAGE

		if (raw.trim().length() == 0)
			{
			rawImage = IJ.getImage();
			if (rawImage == null)
				{
				IJ.showMessage("No image available to be processed!");
				return false;
				}
			raw = rawImage.getTitle();
			}
		else	{
			rawImage = WindowManager.getImage(raw);
			if (rawImage == null)
				{
				IJ.showMessage("No raw image called \""+raw+"\" available!");
				return false;
				}
			}
		slices = rawImage.getImageStackSize();
		rawtimes = new float[slices];
		w = rawImage.getWidth();
		h = rawImage.getHeight();
		if (overCorrection && (row1 < 0 || row2 < 0 || col1 < 0 || col2 < 0 || row1 >= w || row2 >= w || col1 >= h || col2 >= h))
			{
			IJ.showMessage("Undefined region for overscan correction : "+row1+" <= x <= "+row2+", "+col1+" <= y <= "+col2);
			return false;
			}

		// NEED 32 BIT GRAYSCALE IMAGES

		if (rawImage.getBitDepth() != 32)
			{
			if (slices == 1)
				{
				ImageConverter ic = new ImageConverter(rawImage);
				ic.convertToGray32();
				}
			else	{
				StackConverter sc = new StackConverter(rawImage);
				sc.convertToGray32();
				}
			}

		// GET CORRECTION IMAGES

		if (biasCorrection)
			{
			biasImage = WindowManager.getImage(bias);
			if (biasImage == null)
				{
				IJ.showMessage("ERROR: No bias image called \""+bias+"\" available!");
				return false;
				}
			else if (biasImage.getImageStackSize() != 1)
				{
				IJ.showMessage("ERROR: Bias image is a stack!");
				return false;
				}
			else if (biasImage.getWidth() != w || biasImage.getHeight() != h)
				{
				IJ.showMessage("ERROR: Bias image is the wrong size!");
				return false;
				}
			if (biasImage.getBitDepth() != 32)
				{
				ImageConverter ic = new ImageConverter(biasImage);
				ic.convertToGray32();
				}
			}

		if (darkCorrection)
			{
			darkImage = WindowManager.getImage(dark);
			if (darkImage == null)
				{
				IJ.showMessage("ERROR: No dark-current image called \""+dark+"\" available!");
				return false;
				}
			else if (darkImage.getImageStackSize() != 1)
				{
				IJ.showMessage("ERROR: Dark-current image is a stack!");
				return false;
				}
			else if (darkImage.getWidth() != w || darkImage.getHeight() != h)
				{
				IJ.showMessage("ERROR: Dark-current image is the wrong size!");
				return false;
				}
			if (darkImage.getBitDepth() != 32)
				{
				ImageConverter ic = new ImageConverter(darkImage);
				ic.convertToGray32();
				}
			if (expCorrection)
				{
				hdr = FitsJ.getHeader(darkImage);
				if (hdr == null)
					{
					IJ.showMessage("ERROR: Cannot extract FITS header for dark-current image!");
					return false;
					}
				darktime = (float)FitsJ.getExposureTime(hdr);
				if (Float.isNaN(darktime))
					{
					IJ.showMessage("ERROR: Cannot extract exposure time for dark-current image!");
					return false;
					}
				}
			}

		if (flatCorrection)
			{
			flatImage = WindowManager.getImage(flat);
			if (flatImage == null)
				{
				IJ.showMessage("ERROR: No flatfield image called \""+flat+"\" available!");
				return false;
				}
			else if (flatImage.getImageStackSize() != 1)
				{
				IJ.showMessage("ERROR: Flatfield image is a stack!");
				return false;
				}
			else if (flatImage.getWidth() != w || flatImage.getHeight() != h)
				{
				IJ.showMessage("ERROR: Flatfield image is the wrong size!");
				return false;
				}
			if (flatImage.getBitDepth() != 32)
				{
				ImageConverter ic = new ImageConverter(flatImage);
				ic.convertToGray32();
				}
			}

		if (expCorrection)
			{
			if (slices == 1)
				{
				hdr = FitsJ.getHeader(rawImage);
				if (hdr == null)
					{
					IJ.showMessage("ERROR: Cannot extract FITS header for raw image!");
					return false;
					}
				rawtimes[0] = (float)FitsJ.getExposureTime(hdr);
				if (Float.isNaN(rawtimes[0]))
					{
					IJ.showMessage("ERROR: Cannot extract exposure time for raw image!");
					return false;
					}
				}
			else	{
				for (int i=1; i <= slices; i++)
					{
					rawImage.setSlice(i);
					hdr = FitsJ.getHeader(rawImage);
					if (hdr == null)
						{
						IJ.showMessage("ERROR: Cannot read FITS header for raw iamge #"+i);
						return false;
						}
					rawtimes[i-1] = (float)FitsJ.getExposureTime(hdr);
					if (Float.isNaN(rawtimes[i-1]))
						{
						IJ.showMessage("Cannot extract exposure time for raw image #"+i);
						return false;
						}
					}
				}
			}
		else	{
			rawtimes = new float[slices];
			for (int i=0; i < slices; i++)
				rawtimes[i] = 1.0f;
			}

		// CREATE RESULT IMAGE

		if (newimage)
			{
			if (result == null || result.trim().length() == 0)
				result = newName(IJU.extractFilenameWithoutFitsSuffix(raw));		
			resultImage = IJ.createImage (result,"32-bit",w,h,slices);
			}
		else	{
			result = raw;
			}
		return true;
		}

	/**
	 * Performs the actual image arithmetic.
	 */
	protected boolean process()
		{
		float[] rawData=null;
		float[] biasData=null;
		float[] darkData=null;
		float[] flatData=null;
		float[] resultData=null;
		int wh = w*h;

		ImageStack rawStack = null;
		ImageStack resultStack = null;

		String rawLabel = raw;
		String resultLabel = result;

		if (slices > 1)
			{
			rawStack = rawImage.getStack();
			if (newimage)
				resultStack = resultImage.getStack();
			}

		// GET THE CALIBRATION DATA

		if (biasCorrection)
			{
			biasp = biasImage.getProcessor();
			biasp = biasp.convertToFloat();
			biasData = (float[])biasp.getPixels();
			}
		if (darkCorrection)
			{
			darkp = darkImage.getProcessor();
			darkp = darkp.convertToFloat();
			darkData = (float[])darkp.getPixels();
			}
		if (flatCorrection)
			{
			flatp = flatImage.getProcessor();
			flatp = flatp.convertToFloat();
			flatData = (float[])flatp.getPixels();
			}

		// PROCESS EACH SLICE

		for (int i=1; i <= slices; i++)
			{
			rawImage.setSlice(i);
			if (newimage)
				resultImage.setSlice(i);

			rawp = rawImage.getProcessor();
			rawData = (float[])rawp.getPixels();

			if (newimage)
				{
				resultp = resultImage.getProcessor();
				resultData = (float[])resultp.getPixels();
				}
			else	{
				resultp = rawp;
				resultData = rawData;
				}

			// DETERMINE OVERSCAN CORRECTION

			float over=0.0f;
			int cr=0;
			if (overCorrection)
				{
				int num=0;
				for (int c=col1; c <= col2; c++)
					{
					for (int r=row1; r <= row2; r++)
						{
						cr = c*w+r;
						over += rawData[cr];
						num++;
						}
					}
				over /= (float)num;
				}

			// PROCESS SLICE

			hdr = FitsJ.getHeader(rawImage);
			float factor = rawtimes[i-1]/darktime;
			float val=0.0f;
			for (int n=0; n < wh; n++)
				{
				val = rawData[n]-over;
				if (biasCorrection)
					val -= biasData[n];
				if (darkCorrection)
					val -= darkData[n]*factor;
				if (flatCorrection)
					val /= flatData[n];
				resultData[n] = val;
				}

			// SAVE RESULTS

			if (slices > 1 && newimage)
				{
				rawLabel = rawStack.getShortSliceLabel(i);
				resultLabel = newName(IJU.extractFilenameWithoutFitsSuffix(rawLabel));
				resultStack.setSliceLabel(resultLabel,i);
				}

			// REMOVE COSMICS

			if (cosmicsCorrection)
				IJ.run("Remove Outliers...","radius=2 threshold=50 which=Bright stack");

			// NOTE PROCESSING IN FITS HEADER

			resultp.resetMinAndMax();
			if (hdr != null)
				{
				String sl = "";
				if (slices > 1) sl = "["+i+"]";

				String history = "Process_Images "+sl+" : "+resultLabel+" = "+rawLabel;
				if (overCorrection)
					history += " - "+over;
				if (biasCorrection)
					history += " - "+bias;
				if (darkCorrection)
					history += " - "+dark;
				if (expCorrection)
					history += " * ("+rawtimes[i-1]+"/"+darktime+")";
				if (flatCorrection)
					history += " / "+flat;
				if (cosmicsCorrection)
					history += " minus cosmic ray hits";

				hdr = FitsJ.addHistory(history,hdr);

				// STORE HEADER, IF IT EXISTS

				if (newimage)
					{
					if (slices == 1)
						FitsJ.putHeader(resultImage,hdr);
					else
						FitsJ.putHeader(resultStack,hdr,i);
					}
				else	{
					FitsJ.putHeader(rawImage,hdr);
					}
				}
			}
		return true;	
		}

	/**
	 * Saves user selected values as ImageJ preference for later use.
	 */
	protected void savePreferences()
		{
		Prefs.set("ccd.biascorr",biasCorrection);
		if (biasCorrection)
			Prefs.set("ccd.bias",bias);
		Prefs.set("ccd.darkcorr",darkCorrection);
		if (darkCorrection)
			Prefs.set ("ccd.dark",dark);
		Prefs.set("ccd.flatcorr",flatCorrection);
		if (flatCorrection)
			Prefs.set("ccd.flat",flat);
		Prefs.set("ccd.expcorr",expCorrection);
		Prefs.set("ccd.cosmicscorr",cosmicsCorrection);
		}

	/**
	 * Constructs a logical name for a processed image.
	 */
	public static String newName(String oldName)
		{
		return new String(oldName+"_PROC");
		}

	/**
	 * Checks if a string is present in an array of strings.
	 */
	public static boolean contains(String[] arr, String str)
		{
		int n=arr.length;
		if (arr == null || n == 0)
			return false;
		for (int i=0; i < n; i++)
			{
			if (arr[i].equals(str)) return true;
			}
		return false;
		}
	}
