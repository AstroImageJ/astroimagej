package Astronomy;// Image_Shifter.java

import ij.*;
import ij.gui.*;
import ij.plugin.filter.PlugInFilter;
import ij.process.*;

import Jama.Jama.*;
import astroj.*;

/**
 * Shifts an image by a simple amount in x and y.
 *
 * @version 1.1
 * @name F. Hessman
 * @date 2008-Jun-12
 * @changes Modified to conform to ImageJ's half-pixel position of pixel centers.
 *
 * @version 1.2
 * @name F. Hessman
 * @date 2008-Aug-18
 * @changes Optional new image and support for stacks.
 */
public class Image_Shifter implements PlugInFilter
	{
	ImagePlus refImage, resultImage;
	ImageProcessor refProc, resultProc;

	double xShift,yShift;
	int iShift = 0;
	int jShift = 0;
	boolean fractional = true;

	double fillValue = Double.NaN;
	int colorFillValue = 0;
	boolean findFillValue = true;

	boolean createNew = false;
	int nslices = 0;

	/** Standard ImageJ setup routine. */
	public int setup (String arg, ImagePlus imag)
		{
		IJ.register(Image_Shifter.class);
		return DOES_ALL;
		}

	/**
	 * Shifts and image in X and Y.
	 */
	public void run(ImageProcessor ip)
		{
		// ARE THERE ENOUGH WINDOWS?

		refImage = WindowManager.getCurrentImage();
		if (refImage == null)
			{
			IJ.showMessage ("No image to shift!");
			return;
			}
		nslices = refImage.getStackSize();
		int slice = 1;
		if (nslices > 1)
			slice = refImage.getCurrentSlice();
		refProc  = refImage.getProcessor();

		// ASK USER FOR SHIFTS

		if ( !getShifts()) return;

		// CREATE REBINNED IMAGE

		if (refProc instanceof ColorProcessor)
			rebinColorImage ();
		else
			rebinGrayscaleImage ();
		}

	/**
	 * Lets the user select the file to be rebinned and the reference file.
	 */
	protected boolean getShifts ()
		{
		// SHOW POSSIBLE IMAGES IN DIALOGUE

 		GenericDialog gd = new GenericDialog ("Image Shifter", IJ.getInstance());

		gd.addStringField ("X-shift (> 0 moves image to the right)  : ","0.0",10);
		gd.addStringField ("Y-shift (> 0 moves image down)           : ","0.0",10);
		gd.addStringField ("Optional fill value :"," ",10);
		if (nslices == 1)
			gd.addCheckbox("Create new image",createNew);

		if (refProc instanceof ColorProcessor)
			gd.addMessage ("(RGB syntax, e.g. \"255,00,00\" is red, \"00,00,00\" is black)");
		else
			gd.addMessage ("(default filler is the mean value at the image edges)");

		// DO DIALOGUE

		gd.showDialog ();
		if (gd.wasCanceled())
			return false;

		// GET SHIFTS

		String sx = gd.getNextString().trim();
		sx.replace(",",".");
		String sy = gd.getNextString().trim();
		sy.replace(",",".");
		if (sx.contains(".") || sy.contains("."))
			{
			try	{
				xShift = Double.parseDouble (sx);
				yShift = Double.parseDouble (sy);
				}
			catch (NumberFormatException e)
				{
				IJ.error ("Unable to parse shift ("+sx+","+sy+")");
				return false;
				}
			}
		else	{
			fractional = false;
			try	{
				iShift = Integer.parseInt (sx);
				jShift = Integer.parseInt (sy);
				}
			catch (NumberFormatException e)
				{
				IJ.error ("Unable to parse shift ("+sx+","+sy+")");
				return false;
				}
			}

		// GET FILL VALUE

		String fillString = gd.getNextString().trim();
		int r,g,b;
		if (fillString.length() > 0)
			{
			if (refProc instanceof ColorProcessor)
				{
				findFillValue = false;
				String[] rgb = fillString.split(",");
				if (rgb.length != 3)
					{
					IJ.showMessage ("Cannot interpret color ["+fillString+"]");
					return false;
					}
				try	{
					r = Integer.parseInt(rgb[0]);
					g = Integer.parseInt(rgb[1]);
					b = Integer.parseInt(rgb[2]);
					}
				catch (NumberFormatException e)
					{
					IJ.showMessage ("Could not parse integer values in ["+fillString+"]");
					return false;
					}
				colorFillValue = (r<<16)+(g<<8)+b;
				}
			else	{
				try	{
					fillValue = Double.parseDouble(fillString);
					findFillValue = false;
					}
				catch (NumberFormatException e)
					{
					IJ.showMessage ("Could not parse fill value ["+fillString+"]");
					return false;
					}
				}
			}

		// IF NOT STACK, SHOULD A NEW IMAGE BE CREATED?

		if (nslices == 1)
			createNew = gd.getNextBoolean();

		return true;
		}

	/**
	 * Performs the actual rebinning of a grayscale image.
	 */
	protected void rebinGrayscaleImage()
		{
		int w = refProc.getWidth();
		int h = refProc.getHeight();

		// CREATE THE RESULT IMAGE

		resultImage = refImage.createImagePlus();
		ImageProcessor resultProc = refProc.duplicate();
		if (resultProc == null)
			{
			IJ.error ("Image processor of result image is null!");
			return;
			}

		// COPY FITS HEADER

		String[] hdr = FitsJ.getHeader(refImage);
		if (createNew)
			{
			String prefix = new String("Shifted_");
			resultImage.setProcessor(prefix+refImage.getShortTitle(), resultProc);
			}
		else
			resultImage.setProcessor("temp",resultProc);

		// IF USER DOESN'T WANT A PARTICULAR FILL VALUE, ESTIMATE IT

		if (findFillValue)
			getFillValue(refProc);

		// REBIN

		double z = 0.0;
		for (int j=0; j < h; j++)
  			{
			IJ.showProgress (j,h);

			double y = (double)j-Centroid.PIXELCENTER+0.5-yShift;		// Y-POSITION IN REF
			int jy = (int)(y+0.5-Centroid.PIXELCENTER);			// ...AS INT
			int jj = j-jShift;

			if (jj < 1 || jj >= (h-1))
				{
				for (int i=0; i < w; i++)
					resultProc.putPixelValue (i,j, fillValue);
				}
			else
				{
				for (int i=0; i < w; i++)
					{
					double x = (double)i-Centroid.PIXELCENTER+0.5-xShift;
// if (i == 200 && j == 200) IJ.log("i,j="+i+","+j+", x,y="+x+","+y+" ("+Centroid.PIXELCENTER+")");
					int ix = (int)(x+0.5-Centroid.PIXELCENTER);
					int ii = i-iShift;

					if (ii < 1 || ii >= (w-1))
						z = fillValue;
					else if (fractional)
						z = refProc.getInterpolatedValue (x,y);
					else
						z = refProc.getPixelValue (ii,jj);
					resultProc.putPixelValue (i,j,z);
					}
				}
			}

		// FINISHED

		if (hdr != null)
			{
			FitsJ.addHistory("Shifted image by ("+xShift+","+yShift+") pixels.",hdr);
			FitsJ.putHeader(resultImage,hdr);
			}
		if (createNew)
			resultImage.show();
		else
			refImage.setProcessor(refImage.getTitle(),resultProc);
		}

	/**
	 * Gets mean value around the edges of the image treated by the given ImageProcessor
	 * and saves it for later.
	 */
	protected void getFillValue (ImageProcessor ip)
		{
		int w = ip.getWidth();
		int h = ip.getHeight();
		fillValue = 0.0;
		for (int i=0; i < w; i++)
			{
			fillValue += ip.getPixelValue(i,0);
			fillValue += ip.getPixelValue(i,h-1);
			}
		for (int j=0; j < h; j++)
			{
			fillValue += ip.getPixelValue(0,j);
			fillValue += ip.getPixelValue(w-1,j);
			}
		fillValue /= (double)(2*w+2*h-4);
		}

	/**
	 * Performs the actual rebinning of a color image.
	 */
	protected void rebinColorImage()
		{
		ColorProcessor cp = (ColorProcessor)refImage.getProcessor();

		int w = cp.getWidth();
		int h = cp.getHeight();

		// CREATE THE RESULT IMAGE

		resultImage = refImage.createImagePlus();
		ImageProcessor resultProc = cp.duplicate();
		if (resultProc == null)
			{
			IJ.error ("Image processor of result image is null!");
			return;
			}

		String[] hdr = FitsJ.getHeader(refImage);
		if (createNew)
			{
			String prefix = new String("Shifted_");
			resultImage.setProcessor(prefix+refImage.getShortTitle(), resultProc);
			}
		else
			resultImage.setProcessor("temp",resultProc);

		// REBIN

		int z = 0;
		for (int j=0; j < h; j++)
  			{
			IJ.showProgress (j,h);

			// PIXEL CENTROID ARE AT pixel+Centroid.PIXELCENTER

			double y = (double)j+Centroid.PIXELCENTER-0.5-yShift;		// Y-POSITION IN REF
			int jy = (int)(y+0.5-Centroid.PIXELCENTER);			// ...AS INT
			int jj = j-jShift;

			if (jj < 1 || jj >= (h-1))
				{
				for (int i=0; i < w; i++)
					resultProc.putPixel (i,j, colorFillValue);
				}
			else	{
				for (int i=0; i < w; i++)
					{
					double x = (double)i+Centroid.PIXELCENTER-0.5-xShift;
					int ix = (int)(x+0.5-Centroid.PIXELCENTER);
					int ii = i-iShift;

					if (ii < 1 || ii >= (w-1))
						z = colorFillValue;
					else if (fractional)
						z = cp.getInterpolatedRGBPixel (x,y);
					else
						z = cp.getPixel (ii,jj);
					resultProc.putPixel (i,j,z);
					}
				}
			}

		// FINISHED

		if (hdr != null)
			{
			FitsJ.addHistory("Shifted image by ("+xShift+","+yShift+") pixels.",hdr);
			FitsJ.putHeader(resultImage,hdr);
			}
		if (createNew)
			resultImage.show();
		else
			refImage.setProcessor(refImage.getTitle(),resultProc.duplicate());
		}
	}
 
