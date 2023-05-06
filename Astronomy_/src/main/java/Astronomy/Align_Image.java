package Astronomy;// Align_Image.java

import Jama.LUDecomposition;
import Jama.Matrix;
import astroj.*;
import ij.IJ;
import ij.ImagePlus;
import ij.Prefs;
import ij.WindowManager;
import ij.gui.GenericDialog;
import ij.gui.ImageCanvas;
import ij.gui.PlotWindow;
import ij.gui.Roi;
import ij.measure.Calibration;
import ij.plugin.filter.PlugInFilter;
import ij.process.ImageProcessor;

import java.util.Properties;

/**
 * Aligns two images using one or more fiduciary positions in the OverlayCanvas's of the images, either using
 * an affine transformation (x- and y-shift, rotation, scale) of three or more pairs of point or a simple shift using
 * one or more pairs.  The user has to have created MeasurementTables with the points beforehand, e.g. using Aperture_.
 * Based on ancient version which read MeasurementTables.
 *
 * Uses the JAMA matrix package: see http://math.nist.gov/javanumerics/jama/.
 *
 * @author F.V. Hessman, Georg-August-Universitaet Goettingen
 * @version 1.0
 * @date 2008-JUN-11
 *
 * @version 1.1
 * @date 2009-FEB-09
 * @changes Added replace option and cleaned up stack label problem.
 */
public class Align_Image implements PlugInFilter
	{
	String[] images;
	int[] imageList;
	int numImages = 0;
	int current = 0;
	int minimumPoints;

	ImagePlus currentImage, workImage, refImage, resultImage;

	double[] cx, cy;
	float[] xr,yr, xw,yw;
	double[] mr;
	int npts=0;
	double fillValue = Double.NaN;
	boolean findFillValue = true;

	// PREFERENCE VARIABLES

	boolean replace = false;
	boolean normalize = false;
	boolean plotResiduals;
	String choice = null;

	// MINIMUM NUMBER OF CURSOR POINTS NEEDED TO PERFORM ACTION:

	public static int SHIFT_ONLY = 1;
	public static int SHIFT_ROTATE_SCALE = 3;

	public static String SHIFT_ONLY_STRING = "Shift only";
	public static String SHIFT_ROTATE_SCALE_STRING = "Shift+Rotate+Scale";

	/** Standard ImageJ setup routine. */
	public int setup (String arg, ImagePlus imag)
		{
		IJ.register(Align_Image.class);
		return DOES_ALL;
		}

	/**
	 * Aligns two images selected by the user.
	 */
	public void run (ImageProcessor ip)
		{
		// GET ALIGNMENT PREFERENCES

		choice        = Prefs.get("align.choice",SHIFT_ONLY_STRING);
		normalize     = Prefs.get("align.normalize",normalize);
		replace       = Prefs.get("align.replace",replace);
		plotResiduals = Prefs.get("align.plot",plotResiduals);

		// ARE THERE ENOUGH WINDOWS?

		currentImage = WindowManager.getCurrentImage();
		imageList = WindowManager.getIDList();
		if (imageList == null)
			{
			IJ.error("No images are open!");
			return;
			}
		numImages = imageList.length;
		if (numImages < 2)
			{
			IJ.error ("At least two images with its own displayed measurements "
					+ "(see \"Set Aperture\"), should be open.");
			return;
			}
		images = new String[numImages];
		for (int i=0; i < numImages; i++)
			{
			ImagePlus im = WindowManager.getImage (imageList[i]);
			if (im == currentImage)
				current = i;
    		images[i] = IJU.getSliceFilename(im);
			}

		// SELECT THE FILES

		if (!selectFiles()) return;

		// CALCULATE TRANSFORMATIONS

		if (!calculateTransformation()) return;

		// CREATE REBINNED IMAGE

		rebinImage();
		var hdr = FitsJ.getHeader(resultImage);
		if (hdr != null)
			{
			hdr = FitsJ.addHistory("Rebinned to match "+refImage.getShortTitle(),hdr);
			FitsJ.putHeader(resultImage,hdr);
			}

		// NORMALIZE TO SAME OBJECT SIGNALS

		if (normalize)
			normalizeImage();
		if (hdr != null)
			{
			hdr = FitsJ.addHistory("Normalized to stars in "+refImage.getShortTitle(),hdr);
			FitsJ.putHeader(resultImage,hdr);
			}

		// REPLACE IMAGE IF DESIRED

		if (replace)
			replaceImage();
		else
			resultImage.show();

		// SET PREFERENCES

		Prefs.set("align.choice",choice);
		Prefs.set("align.normalize",normalize);
		Prefs.set("align.replace",replace);
		Prefs.set("align.plot",plotResiduals);
		}

	/**
	 * Lets the user select the file to be rebinned and the reference file.
	 */
	protected boolean selectFiles()
		{
		// SHOW POSSIBLE IMAGES IN DIALOGUE

 		GenericDialog gd = new GenericDialog ("Align Image", IJ.getInstance());
		gd.addChoice ("Image to be aligned/rebinned:", images, images[current]);

		int i=0;
		if (i == current) i++;
		gd.addChoice ("Reference image:", images, images[i]);

		String[] choices = new String[]
			{
			SHIFT_ONLY_STRING,
			SHIFT_ROTATE_SCALE_STRING
			};
		gd.addChoice ("Transformation:", choices, choices[0]);

		gd.addStringField ("Optional fill value :","");
		gd.addMessage ("(default filler is the mean value at the image edges)");
		gd.addCheckbox ("Plot residuals (if more than 2 points)", plotResiduals);
		gd.addCheckbox ("Normalize", normalize);
		gd.addCheckbox ("Create new image", !replace);

		// DO DIALOGUE

		gd.showDialog ();
		if (gd.wasCanceled())
			return false;

		// GET IMAGES

		int t = gd.getNextChoiceIndex();
		workImage = WindowManager.getImage(imageList[t]);
		if (workImage == null)
			{
			IJ.error ("Unable to access work image "+images[t]);
			return false;
			}

		t = gd.getNextChoiceIndex();
		refImage = WindowManager.getImage(imageList[t]);
		if (refImage == null)
			{
			IJ.error ("Unable to access reference image "+images[t]);
			return false;
			}

		choice = new String (gd.getNextChoice());
		if (choice.equals(SHIFT_ONLY_STRING))
			minimumPoints = SHIFT_ONLY;
		else
			minimumPoints = SHIFT_ROTATE_SCALE;

		// IMAGES HAVE TO BE DIFFERENT

		if (workImage == refImage)
			{
			IJ.error ("Image to be rebinned = reference image!");
			return false;
			}

		// GET ASSOCIATED POSITION MEASUREMENTS IN WORK IMAGE

		ImageCanvas canvas = workImage.getCanvas();
		if (!(canvas instanceof OverlayCanvas))
			{
			IJ.error ("Work image does not have any Aperture measurements!");
			return false;
			}
		OverlayCanvas ocanvas = (OverlayCanvas)canvas;
		Roi[] rois = ocanvas.getRois();
		int n = rois.length;
		xw = new float[n];
		yw = new float[n];
		int ntot = 0;
		for (i=0; i < n; i++)
			{
			Roi roi = rois[i];
			if (roi instanceof ApertureRoi)
				{
				ApertureRoi aroi = (ApertureRoi)roi;
				double[] xy = aroi.getCenter();		// ImageJ PIXEL POSITIONS!
				xw[ntot] = (float)xy[0];
				yw[ntot] = (float)xy[1];
				ntot++;
				// IJ.log("x,y="+xy[0]+","+xy[1]);
				}
			else
				IJ.log("roi #"+i+" is not an ApertureRoi!");
			}

		// GET ASSOCIATED POSITION MEASUREMENTS IN REFERENCE IMAGE

		canvas = refImage.getCanvas();
		if (!(canvas instanceof OverlayCanvas))
			{
			IJ.error ("Reference image does not have any Aperture measurements!");
			return false;
			}
		ocanvas = (OverlayCanvas)canvas;
		rois = ocanvas.getRois();
		int m = rois.length;
		if (m != n)
			{
			IJ.error ("Images have different number of measurements - Align_Image cannot yet find the matches!");
			return false;
			}
		xr = new float[m];
		yr = new float[m];
		mr = new double[m];
		npts = 0;
		for (i=0; i < n; i++)
			{
			Roi roi = rois[i];
			if (roi instanceof ApertureRoi)
				{
				ApertureRoi aroi = (ApertureRoi)roi;
				double[] xy = aroi.getCenter();
				xr[npts] = (float)xy[0];
				yr[npts] = (float)xy[1];
				mr[npts] = aroi.getMeasurement();
				npts++;
				}
			}

		if (ntot != npts)
			{
			IJ.error ("Reference and work image have different number of measurements!");
			return false;
			}

		// GET FILL VALUE

		String fillString = gd.getNextString();
		if (fillString.length() > 0)
			{
			try	{
				fillValue = Double.parseDouble(fillString);
				findFillValue = false;
				}
			catch (NumberFormatException e)
				{
				IJ.showMessage ("Whoops: fillString=["+fillString+"], length="+fillString.length());
				fillValue = Double.NaN;
				findFillValue = true;
				}
			}

		// SHOULD WE NORMALIZE THE IMAGES OR PLOT RESIDUALS?

		plotResiduals = gd.getNextBoolean();
		normalize     = gd.getNextBoolean();
		replace       = !gd.getNextBoolean();
		return true;
		}

	/**
	 * Performs the actual rebinning for the two selected images using standard ImageJ
	 * functionality plus the X- and Y-transformations.
	 */
	protected void rebinImage()
		{
		ImageProcessor work = workImage.getProcessor();
		ImageProcessor ref = refImage.getProcessor();

		int wRef = refImage.getWidth();
		int hRef = refImage.getHeight();
		int wWork = workImage.getWidth();
		int hWork = workImage.getHeight();

		// CREATE THE RESULT IMAGE

		resultImage = refImage.createImagePlus();
		ImageProcessor result = ref.duplicate();
		if (result == null)
			{
			IJ.error ("Image processor of result image is null!");
			return;
			}
		Calibration calib = resultImage.getCalibration();
		if (calib != null)
			calib.disableDensityCalibration();

		// COPY INFO FROM WORK IMAGE (e.g. FITS HEADER)

		String info = (String)workImage.getProperty("Info");
		if (info != null) resultImage.setProperty("Info",info);

		String prefix;
		if (minimumPoints == SHIFT_ONLY)
			prefix = new String("Shifted_");
		else
			prefix = new String("Aligned_");				
		resultImage.setProcessor(prefix+workImage.getShortTitle(), result);

		// IF USER DOESN'T WANT A PARTICULAR FILL VALUE, ESTIMATE IT

		if (findFillValue)
			getFillValue(work);

		// REBIN

		double z = 0.0;
		for (int jRef=0; jRef < hRef; jRef++)
  			{
			double yr = (double)jRef+Centroid.PIXELCENTER;

			for (int iRef=0; iRef < wRef; iRef++)
				{
				double xr = (double)iRef+Centroid.PIXELCENTER;

				double xw = cx[0]+cx[1]*xr+cx[2]*yr;
				double yw = cy[0]+cy[1]*xr+cy[2]*yr;
				if (withinPixelBounds(xw,yw, wWork,hWork))
					z = work.getInterpolatedValue(xw,yw);
				else
					z = fillValue;
				result.putPixelValue(iRef, jRef,z);
				}
			}

		// COPY IMAGE INFO (E.G. FITS HEADER)

		Properties workProps = workImage.getProperties();
		if (workProps != null)
			{
			String workInfo = workProps.getProperty ("Info");
			// if (workInfo != null)
			//	resultImage.setProperty ("Info", workInfo);
			}
		else	{
			IJ.error ("Original image doesn't have Properties?");
			}
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
	 * Simple bounds checker.
	 */
	protected boolean withinPixelBounds (double x, double y, int w, int h)
		{
		if (x < 0 || x >= w) return false;
		if (y < 0 || y >= h) return false;
		return true;
		}

	/**
	 * Calculates transformation coefficients for the ImageJ pixel position (x,y)
	 *	x' = cx[0] + cx[1]*x + cx[2]*y
	 *	y' = cy[0] + cy[1]*x + cy[2]F*y
	 * using a standard least-squares solution.
	 */
	protected boolean calculateTransformation ()
		{
		if (npts < minimumPoints)
			{
			IJ.error ("At least "+minimumPoints+" reference points are needed!");
			}

		// GET ALL SUMS

		double sum = 0;
		double sumx = 0;
		double sumy = 0;
		double sumxx = 0;
		double sumxy = 0;
		double sumyy = 0;
		double sumX = 0;
		double sumY = 0;
		double sumxX = 0;
		double sumyX = 0;
		double sumxY = 0;
		double sumyY = 0;
		for (int i=0; i < npts; i++)
			{
			sum += 1.0;
			sumx += xr[i];
			sumy += yr[i];
			sumxx += xr[i]*xr[i];
			sumxy += xr[i]*yr[i];
			sumyy += yr[i]*yr[i];
			sumX += xw[i];
			sumY += yw[i];
			sumxX += xr[i]*xw[i];
			sumyX += yr[i]*xw[i];
			sumxY += xr[i]*yw[i];
			sumyY += yr[i]*yw[i];
			}

		// GENERAL AFFINE TRANSFORMATION

		if (minimumPoints == SHIFT_ROTATE_SCALE)
			{
			// SOLVE FOR X-TRANSFORMATION x,yRef->xWork

			double[][] mvals =
					{
					{sum, sumx, sumy},
					{sumx,sumxx,sumxy},
					{sumy,sumxy,sumyy}
					};
			Matrix m = new Matrix(mvals);
			double[][] bvals =	
					{
					{sumX},
					{sumxX},
					{sumyX}
					};
			Matrix b = new Matrix(bvals);
			LUDecomposition lu = new LUDecomposition(m);			Matrix xMatrix = lu.solve(b);
			cx = xMatrix.getColumnPackedCopy();

			// SOLVE FOR Y-TRANSFORMATION x,yRef->yWork

			double[][] cvals =
					{
					{sumY},
					{sumxY},
					{sumyY}
					};
			b = new Matrix(cvals);
			Matrix yMatrix = lu.solve(b);
			cy = yMatrix.getColumnPackedCopy();
			}

		// SIMPLE SHIFT

		else	{
			cx = new double[] {(sumX-sumx)/sum,1.0,0.0};
			cy = new double[] {(sumY-sumy)/sum,0.0,1.0};
			}

		// SHOW RESIDUALS

		if (npts > 2 && plotResiduals)
			{
			float mx=0;
			for (int i=0; i < npts; i++)
				{
				xw[i] -= cx[0]+cx[1]*xr[i]+cx[2]*yr[i];
				yw[i] -= cy[0]+cy[1]*xr[i]+cy[2]*yr[i];
				float absol = Math.abs(xw[i]);
				if (mx < absol) mx=absol;
				absol = Math.abs(yw[i]);
				if (mx < absol) mx=absol;
				}
			mx += 0.5;
			double rang = mx;
			float xx[] = new float[] {-3*mx, 3*mx, 0, 0, 0};
			float yy[] = new float[] {0, 0, 0,  -3*mx, 3*mx};
			PlotWindow pw = new PlotWindow ("Residuals", "X-X(fit) [pixels]", "Y-Y(fit) [pixels]", xx,yy);
			pw.addPoints (xw, yw, PlotWindow.BOX);
			pw.setLimits (-2.*rang,2.*rang,-rang,rang);
			pw.draw();
			}

		return true;
		}

	/**
	 * Normalize using total source-background.
	 */
	protected void normalizeImage()
		{
		double sum1 = 0.0;
		double sum2 = 0.0;

		// RE-MEASURE THE RE-SAMPLED IMAGES (BRIGHTNESS WILL CHANGE AFTER RE-SAMPLING)

		Photometer phot = new Photometer();
		ImageProcessor proc = resultImage.getProcessor();
		double rad = Prefs.get(Aperture_.AP_PREFS_RADIUS,8.0);
		double rb1 = Prefs.get(Aperture_.AP_PREFS_RBACK1,10.0);
		double rb2 = Prefs.get(Aperture_.AP_PREFS_RBACK2,12.0);
        boolean exactPref = Prefs.get(Aperture_.AP_PREFS_EXACT,true);

		// CALCULATE FACTOR

		for (int i=0; i < npts; i++)
			{
			phot.measure (resultImage, exactPref, xr[i],yr[i], rad,rb1,rb2);
			double res = phot.sourceBrightness();
			double ref = mr[i];
			sum1 += ref*res;
			sum2 += res*res;
			}
		double ratio = sum1/sum2;
		// IJ.log("ratio="+ratio);

		// APPLY

		proc.resetRoi();	// OTHERWISE MULIPLICATION WILL BE JUST WITHIN THE ROI
		proc.multiply(ratio);
		}

	/**
	 * Replaces input image with rebinned image.
	 */
	protected void replaceImage()
		{
		ImageProcessor res  = resultImage.getProcessor();
		workImage.setProcessor(resultImage.getShortTitle(),res);
		FitsJ.copyHeader(resultImage,workImage);
		}
	}
 
