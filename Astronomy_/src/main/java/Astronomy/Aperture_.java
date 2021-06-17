package Astronomy;// Aperture_.java

import astroj.*;
import ij.*;
import ij.gui.*;
import ij.measure.*;
import ij.plugin.filter.*;
import ij.process.*;

import java.awt.*;
import java.util.*;

import static astroj.FitsJ.getExposureTime;
import ij.text.TextPanel;

/**
 * Simple circular aperture tool using a circular background annulus.
 * Results are entered in a MeasurementTable as well as in a dialogue
 * pop-up window.
 * 
 * @author F.V. Hessman, Georg-August-Universitaet Goettingen
 * @version 1.0
 * @date 2006-Feb-15
 * @changes Based on original ApertureTool but made it one-shot to permit use in a macro tool a la HOU
 *
 * @version 1.1
 * @date 2006-Apr-25
 * @changes Aperture defined by 3 radii (1x star, 2xsky) (FVH)
 *
 * @version 1.2
 * @date 2006-Dec-12
 * @changes  Added support for OverlayCanvas so that aperture radii and results displayed;
 * @changes  Added static method "a2rect" to make ROI's centered on best integer pixel.
 *
 * @version 1.3
 * @date 2007-Apr-11
 * @changes  Added "forgiving" reporting of errors, display of values from FITS header.
 *
 * @version 1.4
 * @date 2007-May-01
 * @changes  Added "slice" column to insure that the number of columns in a saved MeasurementTable file remains constant.
 *
 * @version 1.5
 * @date 2007-Aug-21
 * @changes Added temporary overlay possibility (to prevent future manipulations from occuring only with a ROI).
 *
 * @version 1.6
 * @date 2007-Sep-17
 * @changes Switched from FITSUtilities to FitsJ (support for separate FITS headers in stack images).
 *
 * @version 1.7
 * @date 2008-02-07
 * @changes Added support for WCS.
 *
 * @version 1.8
 * @date 2008-06-02
 * @changes Added support for ApertureRoi
 *
 * @version 1.9
 * @date 2009-01-10
 * @changes Added retry option.
 *
 * @version 1.10
 * @date 2009-07-05
 * @changes Added variance option.
 *
 * @version 1.11
 * @dave 2010-03-18
 * @author Karen Collins (Univ. Louisville/KY)
 * @changes
 * 1) Changes to the measurements table management code to support the new options mentioned in the
 *	Set_Aperture section.
 * 2) Changes to the preferences management code to support the new options mentioned in the Set_Aperture section.
 *
 * @version 1.12
 * @date 2010-Nov-24
 * @author Karen Collins (Univ. Louisville/KY)
 * @changes Added support for removal of stars from background region (>3 sigma from mean)
 *
 * @version 1.13
 * @date 2011-03-27
 * @author Karen Collins (Univ. Louisville/KY)
 * @changes Merged fix from F.V. Hessman - now uses the centers of pixels
 * as a measure of position when the user has turned off the automatic centering.
 */
public class Aperture_ implements PlugInFilter
	{
	ImagePlus imp;
	ImageProcessor ip;

	ImageCanvas canvas;
	OverlayCanvas ocanvas;
    TextPanel tablePanel;

	double xCenter=0, yCenter=0, radius=25, rBack1=40, rBack2=60, back, source, mean, serror;
	double xWidth, yWidth, width, fwhm, saturationWarningLevel=55000, linearityWarningLevel=30000, vradius, vrBack1, vrBack2, fradius=25, fwhmMult = 1.0, radialCutoff = 0.010;
	double angle, round, variance;
	double mjd = Double.NaN;
    double bjd = Double.NaN;
    double apMag = 99.999;

	double[] fitsVals = null;
	String fitsKeywords = "JD_SOBS,JD_MOBS,HJD_MOBS,BJD_MOBS,ALT_OBJ,AIRMASS";

	int count=0;
//	boolean oneTable = true;
    boolean isInstanceOfStackAlign=this instanceof Stack_Aligner;
	boolean backIsPlane = false;
	boolean reposition  = true;
    boolean exact  = true;
    boolean showAsCentered = true;
//	boolean forgiving   = false;
//	boolean retry       = false;
    boolean removeBackStars = true;
    boolean showRemovedPixels = false;
    boolean useVariableAp = false;
    boolean processingImage = false; //used by Multi-Aperture
    boolean calcRadProFWHM = true;
    boolean showFileName = true;
    boolean showSliceNumber = true;
	boolean showPosition = true;
    boolean showPositionFITS = true;
	boolean showPhotometry = true;
    boolean showBack = true;
    boolean showNAperPixels = true;
    boolean showNBackPixels = true;
	boolean showPeak = true;
    boolean showMean = true;
	boolean showSaturationWarning = true;
    boolean showLinearityWarning = true;
	boolean showWidths = true;
	boolean showRadii = true;
	boolean showTimes = true;
	boolean showRaw = false;
	boolean showMeanWidth = true;
	boolean showAngle = true;
	boolean showRoundness = true;
	boolean showVariance = true;
	boolean showErrors = true;
    boolean showSNR = true;
	boolean showFits = true;
	boolean showRADEC = true;

	boolean starOverlay = true;
	boolean skyOverlay = true;
	boolean valueOverlay = true;
    boolean nameOverlay = true;
	boolean tempOverlay = true;
	boolean clearOverlay = false;
    
    boolean aperturesInitialized=false;

    int astronomyToolId = 0;
    int currentToolId = 0;
    Toolbar toolbar;

	String filename;
	int stackSize = 1, slice = 1;
	long sourceCount = 0, backCount = 0;
	Rectangle rct;
	boolean isCalibrated;

	Centroid center = new Centroid();
	GFormat g;
	static MeasurementTable table;
    static String tableName = "Measurements";
	Photometer photom;
	WCS wcs = null;
    Color apertureColor = Color.red;
    String apertureName = "";

	double ccdGain = 1.0;	// e-/count
	double ccdNoise = 0.0;	// e-
	double ccdDark = 0.0;	// e-/pix/sec as of db2.1.5 on 10/12/2014!!!
	String darkKeyword = new String("");
    public String suffix ="";   //multi-aperture sets this value to "_T1" or "_C1" to add aperture suffix

	boolean temporary = false;
	boolean isFITS = false;
	boolean debug = false;

	double[] raDec = null;

	public static String AP_IMAGE = new String("Label");
	public static String AP_SLICE = new String("slice");

	public static String AP_XCENTER = new String("X(IJ)");
	public static String AP_YCENTER = new String("Y(IJ)");
	public static String AP_XCENTER_FITS = new String("X(FITS)");
	public static String AP_YCENTER_FITS = new String("Y(FITS)");    
	public static String AP_SOURCE = new String("Source-Sky");
    public static String AP_NAPERPIX = new String("N_Src_Pixels");
    public static String AP_SOURCE_AMAG = new String("Source_AMag");
    public static String AP_SOURCE_AMAG_ERR = new String("Source_AMag_Err");
	public static String AP_BACK = new String("Sky/Pixel");
    public static String AP_NBACKPIX = new String("N_Sky_Pixels");
	public static String AP_RSOURCE  = new String("Source_Radius");
    public static String AP_BRSOURCE  = new String("Source_Rad(base)");
    public static String AP_FWHMMULT  = new String("FWHM_Mult");
    public static String AP_RADIALCUTOFF  = new String("Radial_Cutoff");
	public static String AP_RBACK1  = new String("Sky_Rad(min)");
	public static String AP_RBACK2  = new String("Sky_Rad(max)");
	public static String AP_RAWSOURCE = new String("RawSource-RawSky");
	public static String AP_RAWBACK = new String("RawSky/Pixel");
	public static String AP_MJD = new String("J.D.-2400000");
    public static String AP_JDUTC = new String("JD_UTC");
    public static String AP_BJDTDB = new String("BJD_TDB");
	public static String AP_XWIDTH = new String("X-Width");
	public static String AP_YWIDTH = new String("Y-Width");
	public static String AP_MEANWIDTH = new String("Width");
    public static String AP_FWHM = new String("FWHM");
	public static String AP_ANGLE = new String("Angle");
	public static String AP_ROUNDNESS = new String ("Roundness");
	public static String AP_VARIANCE = new String("Variance");
	public static String AP_SOURCE_ERROR = new String("Source_Error");
    public static String AP_SOURCE_SNR = new String("Source_SNR");
	public static String AP_RA = new String("RA");
	public static String AP_DEC = new String("DEC");
	public static String AP_PEAK = new String ("Peak");
    public static String AP_MEAN = new String ("Mean");    
	public static String AP_WARNING = new String ("Saturated");

    //aperture2 components do not get saved in aperture configuration files
	public static String AP_PREFS_RADIUS = new String ("aperture.radius");
	public static String AP_PREFS_RBACK1 = new String ("aperture.rback1");
	public static String AP_PREFS_RBACK2 = new String ("aperture.rback2");
    public static String AP_PREFS_SHOWFILENAME = new String ("aperture.showfilename");
    public static String AP_PREFS_SHOWSLICENUMBER = new String ("aperture.showslicenumber");
	public static String AP_PREFS_SHOWPEAK = new String ("aperture.showpeak");
    public static String AP_PREFS_SHOWMEAN = new String ("aperture.showmean");
    public static String AP_PREFS_EXACT = new String ("aperture.exact");
	public static String AP_PREFS_SHOWSATWARNING = new String ("aperture.showsaturationwarning");
    public static String AP_PREFS_FORCEABSMAGDISPLAY = new String ("aperture.forceabsmagdisplay");
    public static String AP_PREFS_SHOWLINWARNING = new String ("aperture.showlinearitywarning");
	public static String AP_PREFS_SATWARNLEVEL = new String ("aperture.saturationwarninglevel"); 
    public static String AP_PREFS_LINWARNLEVEL = new String ("aperture.linearitywarninglevel"); 
    public static String AP_PREFS_USEHOWELL = new String ("aperture.useHowellCentroidMethod");
    public static String AP_PREFS_CALCRADPROFWHM   = new String ("aperture.calcradprofwhm");
	public static String AP_PREFS_BACKPLANE = new String ("aperture.backplane");
	public static String AP_PREFS_CCDGAIN = new String ("aperture.ccdgain");
	public static String AP_PREFS_CCDNOISE = new String ("aperture.ccdnoise");
	public static String AP_PREFS_CCDDARK = new String ("aperture.ccddark");
	public static String AP_PREFS_DARKKEYWORD = new String ("aperture.darkkeyword");

	public static String AP_PREFS_REPOSITION = new String ("aperture.reposition");
//	public static String AP_PREFS_FORGIVING = new String ("aperture.forgiving");
	public static String AP_PREFS_FITSKEYWORDS = new String ("aperture.fitskeywords");
//	public static String AP_PREFS_RETRY = new String ("aperture.retry");
    public static String AP_PREFS_REMOVEBACKSTARS = new String ("aperture.removebackstars");
    public static String AP_PREFS_SHOWREMOVEDPIXELS = new String ("aperture.showremovedpixels");

	public static String AP_PREFS_SHOWPOSITION = new String ("aperture.showposition");
    public static String AP_PREFS_SHOWPOSITION_FITS = new String ("aperture.showpositionfits");
	public static String AP_PREFS_SHOWPHOTOMETRY = new String ("aperture.showphotometry");
    public static String AP_PREFS_SHOWNAPERPIXELS = new String ("aperture.shownaperpixels");
    public static String AP_PREFS_SHOWNBACKPIXELS = new String ("aperture.shownbackpixels");
    public static String AP_PREFS_SHOWBACK = new String ("aperture.showback");
	public static String AP_PREFS_SHOWWIDTHS = new String ("aperture.showwidths");
	public static String AP_PREFS_SHOWRADII = new String ("aperture.showradii");
	public static String AP_PREFS_SHOWTIMES = new String ("aperture.showtimes");
	public static String AP_PREFS_SHOWRAW = new String ("aperture.showraw");
	public static String AP_PREFS_SHOWMEANWIDTH = new String ("aperture.showmeanwidth");
	public static String AP_PREFS_SHOWANGLE = new String ("aperture.showangle");
	public static String AP_PREFS_SHOWROUNDNESS = new String ("aperture.showroundness");
	public static String AP_PREFS_SHOWVARIANCE = new String ("aperture.showvariance");
	public static String AP_PREFS_SHOWERRORS = new String ("aperture.showerrors");
    public static String AP_PREFS_SHOWSNR = new String ("aperture.showsnr");
	public static String AP_PREFS_SHOWRATIOERROR = new String ("aperture.showratioerror");
	public static String AP_PREFS_SHOWRATIOSNR = new String ("aperture.showratiosnr");
	public static String AP_PREFS_SHOWFITS = new String ("aperture.showfits");
	public static String AP_PREFS_SHOWRADEC = new String ("aperture.showradec");

	public static String AP_PREFS_STAROVERLAY = new String ("aperture.staroverlay");
	public static String AP_PREFS_SKYOVERLAY = new String ("aperture.skyoverlay");
    public static String AP_PREFS_NAMEOVERLAY = new String ("aperture.nameoverlay");
	public static String AP_PREFS_VALUEOVERLAY = new String ("aperture.valueoverlay");
	public static String AP_PREFS_TEMPOVERLAY = new String ("aperture.tempoverlay");
	public static String AP_PREFS_CLEAROVERLAY = new String ("aperture.clearoverlay");


	/*
	 * Standard ImageJ PluginFilter setup routine which also determines the default aperture radius.
	 */
	public int setup (String arg, ImagePlus imp)
		{
        Locale.setDefault(IJU.locale);
		// System.err.println("arg="+arg);
		this.imp = imp;
		if (imp == null)
            {
            return DONE;
            }
		// GET VARIOUS MEASUREMENT PARAMETERS FROM PREFERENCES

		getMeasurementPrefs ();

		// GET OVERLAY CANVAS

		canvas = imp.getCanvas();
		ocanvas = null;
		if (starOverlay || skyOverlay || valueOverlay || nameOverlay)
			{
			ocanvas = OverlayCanvas.getOverlayCanvas (imp);
			canvas = ocanvas;
			}

		stackSize=imp.getStackSize();
		slice = imp.getCurrentSlice();

		if (stackSize <= 1)
			filename = imp.getTitle();	// getShortTitle()?
		else	{
			filename = IJU.getSliceFilename(imp, slice); 
			if (filename == null)
				filename = imp.getTitle();
			}

		// GET VARIOUS MEASUREMENT PARAMETERS FROM PREFERENCES

		getMeasurementPrefs ();

		// HANDY FORMATING

		g = new GFormat("2.1");

		// OUTPUT MEASUREMENT TABLE

//		table = null;

		// CALIBRATION OBJECT FOR TRANSFORMING INTENSITIES IF NECESSARY

		Calibration cal = imp.getCalibration();
		if (cal != null && cal.calibrated())
			isCalibrated = true;
		else
			isCalibrated = false;

		// PREVENT UNTIMELY GARBAGE-COLLECTION

		IJ.register(Aperture_.class);

		// SETUP COMPLETED

		return DOES_ALL+NO_UNDO+NO_CHANGES;
		}

	/**
	 * Measures centroid and aperture brightness of object, stores results in a MeasurementTable, and exits.
	 */
	public void run (ImageProcessor ipt)
		{

    	ip = ipt;							// NOTE IMAGE PROCESSOR FOR LATER USE
		// if (IJ.escapePressed()) { shutDown(); return; }
        xCenter = canvas.offScreenXD(canvas.xClicked);
        yCenter = canvas.offScreenYD(canvas.yClicked);
//        addApertureRoi();
//        canvas.repaint();
//		getCrudeCenter();
        
		if (measureAperture())
			storeResults();
		// shutDown();
		}

	/**
	 * Gets crude estimate of object position from ROI created by mouse click.
	 */
//	protected void getCrudeCenter()
//		{
//
//        xCenter = canvas.offScreenXD(canvas.xClicked);
//        yCenter = canvas.offScreenXD(canvas.yClicked);
////		rct = imp.getProcessor().getRoi();
////		xCenter=(double)rct.x+0.5*(double)rct.width;//+Centroid.PIXELCENTER;
////		yCenter=(double)rct.y+0.5*(double)rct.height;//+Centroid.PIXELCENTER;
//		if (debug) IJ.log("Aperture_.getCrudeCenter rct="+rct.x+","+rct.y+","+rct.width+","+rct.height+" => "+xCenter+","+yCenter);
//		}
//
//	/**
//	 * Adjusts ROI to current Aperture_ size.
//	 */
//	protected void adjustRoi ()
//		{
//		Rectangle rect = Aperture_.a2rect (xCenter,yCenter,radius);
//		ip.setRoi (rect.x, rect.y, rect.width, rect.height);
//		IJ.makeOval (rect.x, rect.y, rect.width, rect.height);
//		if (debug) IJ.log("Aperture_.adjustRoi: a2rect("+xCenter+","+yCenter+","+radius+
//				")=("+rect.x+","+rect.y+","+rect.width+","+rect.height+")");
//		}

     /**
	 * Allows multi-aperture to utilize variable size apertures
	 */
    protected void setVariableAperture(boolean useVar, double vrad, double mult, double cutoff)
        {
        useVariableAp = useVar;
        vradius = vrad;
        fwhmMult = mult;
        radialCutoff = cutoff;
        vrBack1 = rBack1;// + vradius - radius;
        vrBack2 = rBack2;// + vradius - radius;
        }

    protected void setVariableAperture(boolean useVar)
        {
        useVariableAp = useVar;
        }

    protected void setApertureColor(Color col)
        {
        apertureColor = col;
        }

    protected void setApertureName(String apName)
        {
        apertureName = apName;
        }
    protected void setShowAsCentered(boolean showCentered)
        {
        showAsCentered = showCentered;
        }
    protected void setAbsMag(double mag)
        {
        apMag = mag;
        }    

	/**
	 * Finishes one-shot measurement by making the default tool a rectangular ROI.
	 */
	void shutDown()
		{
        // TO MAKE SURE THAT THE NEXT CLICK WILL WORK
        toolbar = Toolbar.getInstance();
        astronomyToolId = toolbar.getToolId("Astronomy_Tool");
        if (astronomyToolId != -1)
         	toolbar.setTool(astronomyToolId);
        else
            toolbar.setTool(0);
//		showApertureStatus ();
		if (imp.isLocked()) imp.unlock();
		}

	/**
	 * Performs exact measurement of object position and integrated brightness.
	 */
	protected boolean measureAperture ()
		{
        boolean returnVal = true;
        if (!adjustAperture(false)) 
            {
            if (this instanceof MultiAperture_ && !(this instanceof Stack_Aligner) && !(Prefs.get(MultiAperture_.PREFS_HALTONERROR, true)))
                {
                returnVal = false;
                }
            else
                {
                return false;
                }
            }
		
        measurePhotometry();

        String[] hdr = FitsJ.getHeader (imp);
		// GET MJD
        mjd = 0.0;
		if (hdr != null && isFITS && showTimes)
			{
			mjd = FitsJ.getMeanMJD (hdr);
			if (Double.isNaN(mjd))
				mjd = FitsJ.getMJD (hdr);		// FITSUtilities.getMJD (img);
			}

		// GET FITS KEYWORD VALUES

		if (hdr != null && isFITS && showFits)
			{
			if (fitsKeywords != null && !fitsKeywords.equals(""))
				{
				String[] sarr = fitsKeywords.split(",");
				fitsVals = new double[sarr.length];
				for (int l=0; l < sarr.length; l++)
					{
					try	{
						fitsVals[l] = FitsJ.findDoubleValue (sarr[l],hdr);
						}
					catch (NumberFormatException e) {}
					}
				}
			}
        
        bjd = 0.0;
        if (hdr != null && isFITS && showTimes && FitsJ.isTESS(hdr))
			{
			bjd = FitsJ.getMeanTESSBJD(hdr);
			}

		// GET RA AND DEC (IN DEGREES) USING WCS

		if (isFITS && showRADEC && wcs != null && wcs.hasRaDec())
			{
			raDec = wcs.pixels2wcs(new double[]{ xCenter,yCenter });
			}

		// SHOW RESULTS IN OVERLAY

		drawAperture ();
		// SHOW RESULTS IN ImageJ TOOLBAR

		showApertureStatus ();

		if (tempOverlay)
			imp.killRoi();
       
		return returnVal;
		}
    
protected void measurePhotometry()
    {
    String[] hdr = FitsJ.getHeader (imp);
    double darkPerPix = ccdDark; 
    if (hdr != null)
        {
        isFITS = true;
        wcs = new WCS(hdr);
        double exptime = FitsJ.getExposureTime(hdr);
        if (Double.isNaN(exptime)) exptime = 1.0;
        darkPerPix *= exptime;
        if (!darkKeyword.trim().equals(""))
            {
            try	{
                darkPerPix = FitsJ.findDoubleValue(darkKeyword,hdr);
                }
            catch (NumberFormatException e)
                {
                darkPerPix = ccdDark*exptime;
                }
            }
        }

    // DO APERTURE PHOTOMETRY
    photom = new Photometer (imp.getCalibration());
    photom.setCCD (ccdGain, ccdNoise, darkPerPix);
    photom.setRemoveBackStars(removeBackStars);
    photom.setMarkRemovedPixels(showRemovedPixels);
    photom.setUsePlane(backIsPlane);

    photom.measure (imp,exact,xCenter,yCenter, radius, rBack1, rBack2);

    back = photom.backgroundBrightness();
    source = photom.sourceBrightness();
    serror = photom.sourceError();
    mean = photom.meanBrightness();
    if (calcRadProFWHM) fwhm = photom.getFWHM();
    else fwhm = Double.NaN;
    }


		protected boolean adjustAperture(boolean updatePhotometry) {return adjustAperture(updatePhotometry, reposition);}

		protected boolean adjustAperture(boolean updatePhotometry, boolean centroid)
        {
        ip = imp.getProcessor();
		if (stackSize > 1)
			{
			ImageStack stack = imp.getImageStack();
			filename = IJU.getSliceFilename(imp);
			}

		// GET MEASURMENT PARAMETERS AGAIN IN CASE THEY HAVE CHANGED

		getMeasurementPrefs ();
        if (!(this instanceof MultiAperture_))
            {
            showAsCentered = reposition;
            } else {
        	reposition = centroid;
		}

        if (useVariableAp)
            {
            fradius = radius;
            radius = vradius;
            rBack1 = vrBack1;
            rBack2 = vrBack2;
            }

		// GET CENTROID OBJECT FOR MEASURING


        boolean returnVal = true;
		if (!center.measure(imp, xCenter,yCenter,radius,rBack1,rBack2,reposition,backIsPlane,removeBackStars))
            {
            if (!(this instanceof MultiAperture_)) //single aperture
                {
                IJ.beep();
                IJ.showMessage("Centroid Error", "<html>Centroid not found near initial guess.<br>Try clicking closer to the center of the star.</html>");
                return false;
                }
            showAsCentered = false;
            returnVal = false; 
            }
		xCenter = center.x();
		yCenter = center.y();
		xWidth = center.width();
		yWidth = center.height();
        width = 0.5 * (xWidth + yWidth);
		angle = center.orientation();
		if (angle < 0.0) angle += 360.0;
		round = center.roundness();
		variance = center.variance();
        back = center.background();
        if (updatePhotometry) measurePhotometry();
        
		count++;
        return returnVal;
        }


	/**
	 * Shows results in the image overlay channel.
	 */
	protected void drawAperture ()
		{
		if (ocanvas != null && clearOverlay)
			ocanvas.clearRois();
		if (starOverlay || skyOverlay || valueOverlay || nameOverlay)
			{
			addApertureRoi ();
			canvas.repaint();
			}
		}

	/**
	 * Adds an OvalRoi to the overlay.
	 */
//	protected void addOvalRoi (double x, double y, double r)
//		{
//		Rectangle rect = Aperture_.a2rect (x,y,r);
//		OvalRoi roi = new OvalRoi (rect.x,rect.y,rect.width,rect.height);
//		if (debug) IJ.log("Aperture_.addOvalRoi: a2rect("+x+","+y+","+r+
//				")=("+rect.x+","+rect.y+","+rect.width+","+rect.height+")");
//		roi.setImage (imp);
//		if (starOverlay || skyOverlay || valueOverlay || nameOverlay)
//			ocanvas.add (roi);
//		}

	/**
	 * Adds an ApertureRoi to the overlay
	 */
	protected void addApertureRoi ()
		{
		if (starOverlay || skyOverlay || valueOverlay || nameOverlay)
			{
			ApertureRoi roi = new ApertureRoi (xCenter,yCenter,radius,rBack1,rBack2,source,showAsCentered);
			roi.setAppearance (starOverlay,showAsCentered,skyOverlay,nameOverlay,valueOverlay,apertureColor,apertureName,source);
			roi.setAMag(apMag);
            roi.setImage (imp);
			ocanvas.add (roi);
			}
		}

	/**
	 * Adds text to the overlay.
	 */
	protected void addStringRoi (double x, double y, String text)
		{
		int xc = (int)(x-Centroid.PIXELCENTER);
		int yc = (int)(y-Centroid.PIXELCENTER);
		StringRoi roi = new StringRoi (xc,yc,text);
		roi.setImage (imp);
		if (starOverlay || skyOverlay || valueOverlay || nameOverlay)
			ocanvas.add (roi);
		}

	/**
	 * Shows status of aperture measurement in the ImageJ toolbar.
	 */
	void showApertureStatus ()
		{
		if (wcs != null && showRADEC && wcs.hasRaDec() && raDec != null)
			IJ.showStatus (""+slice+": R.A.="+g.format(raDec[0]/15.0)+", Dec.="+g.format(raDec[1])+", src="
						+g.format(photom.sourceBrightness()));
		else
			IJ.showStatus (""+slice+": x="+g.format(xCenter)+", y="+g.format(yCenter)+", src="
						+g.format(photom.sourceBrightness()));
		}

	/**
	 * Sets a ROI with the correct radius at the aperture position.
	 */
	protected void centerROI()
		{
		Rectangle rect = Aperture_.a2rect (xCenter,yCenter,radius);
		ip.setRoi (rect.x, rect.y, rect.width, rect.height);
		IJ.makeOval (rect.x, rect.y, rect.width, rect.height);
		if (debug) IJ.log("Aperture_.centerROI: a2rect("+xCenter+","+yCenter+","+radius+
				")=("+rect.x+","+rect.y+","+rect.width+","+rect.height+")");
		}

	/**
	 * Displays the centroiding & photometry results in the table.
	 */
	protected void storeResults ()
		{

		if (isInstanceOfStackAlign || !checkResultsTable()) return;

		// CREATE ROW FOR NEXT ENTRY

		table.incrementCounter();
         
		if (showFileName)
            table.addLabel (AP_IMAGE,filename);

		// NOTE SLICE
        if (showSliceNumber)
            {
            if (stackSize == 1)
                table.addValue (AP_SLICE, 0, 0);
            else
                table.addValue (AP_SLICE, slice, 0);
            }

		// NOTE VALUES IN NEW TABLE ROW

		if (showPosition)
			{
			table.addValue (AP_XCENTER+suffix, xCenter, 6);
			table.addValue (AP_YCENTER+suffix, yCenter, 6);
			}
		if (showPositionFITS)
			{
			table.addValue (AP_XCENTER_FITS+suffix, xCenter + Centroid.PIXELCENTER, 6);
			table.addValue (AP_YCENTER_FITS+suffix, (double)imp.getHeight() - yCenter + Centroid.PIXELCENTER, 6);
			}        
		if (showPhotometry)
			table.addValue (AP_SOURCE+suffix, source, 6);
        if (showNAperPixels) 
            table.addValue (AP_NAPERPIX+suffix, photom.numberOfSourceAperturePixels(), 6);
        if (showPeak)
            table.addValue (AP_PEAK+suffix, photom.peakBrightness(), 6);
        if (showMean)
            table.addValue (AP_MEAN+suffix, photom.meanBrightness(), 6);            
        if (showSaturationWarning)
            {
            if (photom.peakBrightness() > saturationWarningLevel)
                table.addValue (AP_WARNING, photom.peakBrightness(), 6);
            else
                table.addValue (AP_WARNING, 0.0, 6);
            }
        if (showErrors)
            table.addValue (AP_SOURCE_ERROR+suffix, serror, 6);
        if (showSNR)
            table.addValue (AP_SOURCE_SNR+suffix, source/serror, 6);
        if (showBack)
            table.addValue (AP_BACK+suffix, back, 6);
        if (showNBackPixels)
            table.addValue (AP_NBACKPIX+suffix, photom.numberOfBackgroundAperturePixels(), 6);
        if (showMeanWidth && calcRadProFWHM)
            table.addValue(AP_FWHM+suffix, fwhm ,6);
        if (showRadii)
            {
            table.addValue (AP_RSOURCE, radius, 6);
            if (useVariableAp)
                {
                if (fwhmMult > 0)
                    {
                    table.addValue (AP_FWHMMULT, fwhmMult, 6);
                    }
                else
                    {
                    table.addValue (AP_RADIALCUTOFF, radialCutoff, 6);
                    }
                table.addValue (AP_BRSOURCE, fradius, 6);
                }
            table.addValue (AP_RBACK1, rBack1, 6);
            table.addValue (AP_RBACK2, rBack2, 6);
            }
		if (showTimes && !Double.isNaN(mjd))
            {
			table.addValue (AP_MJD, mjd, 6);
            }
		if (showRADEC && wcs != null && wcs.hasRaDec() && raDec != null)
			{
			table.addValue (AP_RA+suffix, raDec[0]/15.0, 6);
			table.addValue (AP_DEC+suffix, raDec[1], 6);
			}
		if (showFits && fitsVals != null)
			{
			String[] sarr = fitsKeywords.split(",");
			for (int l=0; l < fitsVals.length; l++)
				{
				if (fitsVals[l] != Double.NaN)
					table.addValue (sarr[l], fitsVals[l], 6);
				}
			}
        if (showTimes && !Double.isNaN(mjd))
            {
            int col = table.getColumnIndex(AP_JDUTC);
            if (col >= 0)
                {
                double value = table.getValueAsDouble(col, table.getCounter()-1);
                if (Double.isNaN(value) || value == 0.0)
                    {
                    table.addValue (AP_JDUTC, mjd+2400000.0, 6);
                    }
                }
            }
        if (showTimes && !Double.isNaN(bjd))
            {
            int col = table.getColumnIndex(AP_BJDTDB);
            if (col >= 0)
                {
                double value = table.getValueAsDouble(col, table.getCounter()-1);
                if (Double.isNaN(value) || value == 0.0)
                    {
                    table.addValue (AP_BJDTDB, bjd, 6);
                    }
                }
            }
		if (showWidths)
			{
			table.addValue (AP_XWIDTH+suffix, xWidth, 6);
			table.addValue (AP_YWIDTH+suffix, yWidth, 6);
			}
		if (showMeanWidth)
			table.addValue (AP_MEANWIDTH+suffix, width, 6);
		if (showAngle)
			table.addValue (AP_ANGLE+suffix, angle, 6);
		if (showRoundness)
			table.addValue (AP_ROUNDNESS+suffix, round, 6);
		if (showVariance)
			table.addValue (AP_VARIANCE+suffix, variance, 6);
		if (showRaw && isCalibrated)
			{
            table.addValue (AP_RAWSOURCE+suffix, photom.rawSourceBrightness(), 6);
            table.addValue (AP_RAWBACK+suffix, photom.rawBackgroundBrightness(), 6);
			}

		// SHOW NEW ROW

		if (!(this instanceof MultiAperture_)) table.show("Measurements");
		}

	/**
	 * Identifies the results table, creating one if necessary.  If the results table
	 * was previously in a different format, it erases the present contents. Bug or feature?
	 */
	protected boolean checkResultsTable()
		{

        tablePanel = MeasurementTable.getTextPanel(tableName);
        
		if (isInstanceOfStackAlign || this instanceof MultiAperture_ || (table != null && tablePanel != null && table.getCounter() > 0)) return true;
        
//		if (oneTable)
	    table = MeasurementTable.getTable (tableName);
//		else
//            table = MeasurementTable.getTable (filename);

		if (table == null)
			{
			IJ.error ("Unable to open measurement table.");
			return false;
			}

		// CHECK TO SEE IF Aperture_ ENTRIES ALREADY THERE

		int i=0;
        if (showSliceNumber && table.getColumnIndex(AP_SLICE) == ResultsTable.COLUMN_NOT_FOUND)
            i=table.getFreeColumn (AP_SLICE);
        if (showSaturationWarning && table.getColumnIndex(AP_WARNING) == ResultsTable.COLUMN_NOT_FOUND)
            i=table.getFreeColumn (AP_WARNING);
		if (showTimes && table.getColumnIndex(AP_MJD) == ResultsTable.COLUMN_NOT_FOUND)
			i=table.getFreeColumn (AP_MJD);
        if (showTimes && table.getColumnIndex(AP_JDUTC) == ResultsTable.COLUMN_NOT_FOUND)
			i=table.getFreeColumn (AP_JDUTC);
        
		if (showFits && fitsKeywords != null)
			{
			String[] sarr = fitsKeywords.split(",");
			for (int l=0; l < sarr.length; l++)
				{
				if (!sarr[l].equals("") &&
						table.getColumnIndex(sarr[l]) == ResultsTable.COLUMN_NOT_FOUND)
					i=table.getFreeColumn (sarr[l]);
				}
			}
        
        if (showTimes && FitsJ.isTESS(FitsJ.getHeader(imp)) && table.getColumnIndex(AP_BJDTDB) == ResultsTable.COLUMN_NOT_FOUND)
            i=table.getFreeColumn (AP_BJDTDB);
        if (showRadii)
            {
			if (table.getColumnIndex(AP_RSOURCE) == ResultsTable.COLUMN_NOT_FOUND)
				i=table.getFreeColumn (AP_RSOURCE);
			if (table.getColumnIndex(AP_RBACK1) == ResultsTable.COLUMN_NOT_FOUND)
				i=table.getFreeColumn (AP_RBACK1);
			if (table.getColumnIndex(AP_RBACK2) == ResultsTable.COLUMN_NOT_FOUND)
				i=table.getFreeColumn (AP_RBACK2);
            }
        if (showPosition && table.getColumnIndex(AP_XCENTER+suffix) == ResultsTable.COLUMN_NOT_FOUND)
            i=table.getFreeColumn (AP_XCENTER+suffix);
        if (showPosition && table.getColumnIndex(AP_YCENTER+suffix) == ResultsTable.COLUMN_NOT_FOUND)
            i=table.getFreeColumn (AP_YCENTER+suffix);
        if (showPositionFITS && table.getColumnIndex(AP_XCENTER_FITS+suffix) == ResultsTable.COLUMN_NOT_FOUND)
            i=table.getFreeColumn (AP_XCENTER_FITS+suffix);
        if (showPositionFITS && table.getColumnIndex(AP_YCENTER_FITS+suffix) == ResultsTable.COLUMN_NOT_FOUND)
            i=table.getFreeColumn (AP_YCENTER_FITS+suffix);
       
		if (showRADEC && wcs != null && wcs.hasRaDec() && raDec != null)
			{
			if (table.getColumnIndex(AP_RA+suffix) == ResultsTable.COLUMN_NOT_FOUND)
				i=table.getFreeColumn (AP_RA+suffix);
			if (table.getColumnIndex(AP_DEC+suffix) == ResultsTable.COLUMN_NOT_FOUND)
				i=table.getFreeColumn (AP_DEC+suffix);
			}
        if (showPhotometry && table.getColumnIndex(AP_SOURCE+suffix) == ResultsTable.COLUMN_NOT_FOUND)
            i=table.getFreeColumn (AP_SOURCE+suffix);
        if (showNAperPixels && table.getColumnIndex(AP_NAPERPIX+suffix) == ResultsTable.COLUMN_NOT_FOUND)
            i=table.getFreeColumn (AP_NAPERPIX+suffix);        
        if (showErrors && table.getColumnIndex(AP_SOURCE_ERROR+suffix) == ResultsTable.COLUMN_NOT_FOUND)
            i=table.getFreeColumn (AP_SOURCE_ERROR+suffix);
        if (showSNR && table.getColumnIndex(AP_SOURCE_SNR+suffix) == ResultsTable.COLUMN_NOT_FOUND)
            i=table.getFreeColumn (AP_SOURCE_SNR+suffix);
        if (showPeak && table.getColumnIndex(AP_PEAK+suffix) == ResultsTable.COLUMN_NOT_FOUND)
            i=table.getFreeColumn (AP_PEAK+suffix);
        if (showMean && table.getColumnIndex(AP_MEAN+suffix) == ResultsTable.COLUMN_NOT_FOUND)
            i=table.getFreeColumn (AP_MEAN+suffix);        
        if (showBack && table.getColumnIndex(AP_BACK+suffix) == ResultsTable.COLUMN_NOT_FOUND)
            i=table.getFreeColumn (AP_BACK+suffix);
        if (showNBackPixels && table.getColumnIndex(AP_NBACKPIX+suffix) == ResultsTable.COLUMN_NOT_FOUND)
            i=table.getFreeColumn (AP_NBACKPIX+suffix);
        if (showMeanWidth && calcRadProFWHM && table.getColumnIndex (AP_FWHM) == MeasurementTable.COLUMN_NOT_FOUND)
                i=table.getFreeColumn (AP_FWHM+suffix);
		if (showMeanWidth && table.getColumnIndex(AP_MEANWIDTH+suffix) == ResultsTable.COLUMN_NOT_FOUND)
			i=table.getFreeColumn (AP_MEANWIDTH+suffix);
		if (showWidths)
			{
			if (table.getColumnIndex(AP_XWIDTH+suffix) == ResultsTable.COLUMN_NOT_FOUND)
				i=table.getFreeColumn (AP_XWIDTH+suffix);
			if (table.getColumnIndex(AP_YWIDTH+suffix) == ResultsTable.COLUMN_NOT_FOUND)
				i=table.getFreeColumn (AP_YWIDTH+suffix);
			}
		if (showAngle && table.getColumnIndex(AP_ANGLE+suffix) == ResultsTable.COLUMN_NOT_FOUND)
			i=table.getFreeColumn (AP_ANGLE+suffix);
		if (showRoundness && table.getColumnIndex(AP_ROUNDNESS+suffix) == ResultsTable.COLUMN_NOT_FOUND)
			i=table.getFreeColumn (AP_ROUNDNESS+suffix);
		if (showVariance && table.getColumnIndex(AP_VARIANCE+suffix) == ResultsTable.COLUMN_NOT_FOUND)
			i=table.getFreeColumn (AP_VARIANCE+suffix);
		if (showRaw)
			{
			if (table.getColumnIndex(AP_RSOURCE) == ResultsTable.COLUMN_NOT_FOUND)
				i=table.getFreeColumn (AP_RSOURCE);
			if (table.getColumnIndex(AP_RBACK1) == ResultsTable.COLUMN_NOT_FOUND)
				i=table.getFreeColumn (AP_RBACK1);
			if (table.getColumnIndex(AP_RBACK2) == ResultsTable.COLUMN_NOT_FOUND)
				i=table.getFreeColumn (AP_RBACK2);
			if (isCalibrated)
				{
				if (table.getColumnIndex(AP_RAWSOURCE+suffix) == ResultsTable.COLUMN_NOT_FOUND)
					i=table.getFreeColumn (AP_RAWSOURCE+suffix);
				if (table.getColumnIndex(AP_RAWBACK+suffix) == ResultsTable.COLUMN_NOT_FOUND)
					i=table.getFreeColumn (AP_RAWBACK+suffix);
				}
			}
         

//		table.show("Measurements");
		return true;
		}

	/**
	 * Gets all the aperture measurement parameters needed from the preferences.
	 */
	protected void getMeasurementPrefs ()
		{
        debug = Prefs.get ("astroj.debug",false);

		radius = Prefs.get (AP_PREFS_RADIUS,radius);
        fradius = radius;
		rBack1 = Prefs.get (AP_PREFS_RBACK1,rBack1);
		rBack2 = Prefs.get (AP_PREFS_RBACK2,rBack2);

//		oneTable = Prefs.get (AP_PREFS_ONETABLE, false);

		// THESE ARE IN MultiAperture_ !!!
		// wideTable = Prefs.get (AP_PREFS_WIDETABLE, true);
		// follow = Prefs.get (AP_PREFS_FOLLOW, false);
		// showRatio = Prefs.get (AP_PREFS_SHOWRATIO, true);
		// nAperturesDefault = (int) Prefs.get (AP_PREFS_NAPERTURESDEFAULT, nAperturesDefault);
		// showRatioError = Prefs.get (AP_PREFS_SHOWRATIOERROR, showRatioError);
		// showRatioSNR = Prefs.get (AP_PREFS_SHOWRATIOSNR, showRatioSNR);

		backIsPlane = Prefs.get (AP_PREFS_BACKPLANE, backIsPlane);
		reposition = Prefs.get (AP_PREFS_REPOSITION, reposition);
//		forgiving = Prefs.get (AP_PREFS_FORGIVING, forgiving);
//		retry = Prefs.get (AP_PREFS_RETRY, retry);
        removeBackStars = Prefs.get(AP_PREFS_REMOVEBACKSTARS, removeBackStars);
        showRemovedPixels = Prefs.get(AP_PREFS_SHOWREMOVEDPIXELS, showRemovedPixels);
		
		showFits = Prefs.get (AP_PREFS_SHOWFITS, showFits);
		fitsKeywords = Prefs.get (AP_PREFS_FITSKEYWORDS, fitsKeywords);

		ccdGain = Prefs.get (AP_PREFS_CCDGAIN, ccdGain);
		ccdNoise = Prefs.get (AP_PREFS_CCDNOISE, ccdNoise);
		ccdDark = Prefs.get (AP_PREFS_CCDDARK, ccdDark);
		darkKeyword = Prefs.get (AP_PREFS_DARKKEYWORD, darkKeyword);

		showPosition = Prefs.get (AP_PREFS_SHOWPOSITION, showPosition);
        showPositionFITS = Prefs.get (AP_PREFS_SHOWPOSITION_FITS, showPositionFITS);
		showPhotometry = Prefs.get (AP_PREFS_SHOWPHOTOMETRY, showPhotometry);
        showNAperPixels = Prefs.get (AP_PREFS_SHOWNAPERPIXELS, showNAperPixels);
        showNBackPixels = Prefs.get (AP_PREFS_SHOWNBACKPIXELS, showNBackPixels);
        showBack = Prefs.get (AP_PREFS_SHOWBACK, showBack);
        exact = Prefs.get (AP_PREFS_EXACT, exact);
        calcRadProFWHM = Prefs.get (AP_PREFS_CALCRADPROFWHM, calcRadProFWHM);
        showFileName = Prefs.get (AP_PREFS_SHOWFILENAME, showFileName);
        showSliceNumber = Prefs.get (AP_PREFS_SHOWSLICENUMBER, showSliceNumber);
		showPeak = Prefs.get (AP_PREFS_SHOWPEAK, showPeak);
        showMean = Prefs.get (AP_PREFS_SHOWMEAN, showMean);
		showSaturationWarning = Prefs.get (AP_PREFS_SHOWSATWARNING, showSaturationWarning);
		saturationWarningLevel = Prefs.get (AP_PREFS_SATWARNLEVEL, saturationWarningLevel);
        showLinearityWarning = Prefs.get (AP_PREFS_SHOWLINWARNING, showLinearityWarning);
		linearityWarningLevel = Prefs.get (AP_PREFS_LINWARNLEVEL, linearityWarningLevel);
		showWidths = Prefs.get (AP_PREFS_SHOWWIDTHS, showWidths);
		showRadii = Prefs.get (AP_PREFS_SHOWRADII, showRadii);
		showTimes = Prefs.get (AP_PREFS_SHOWTIMES, showTimes);
		showRaw = Prefs.get (AP_PREFS_SHOWRAW, showRaw);
		showMeanWidth = Prefs.get (AP_PREFS_SHOWMEANWIDTH, showMeanWidth);
		showAngle = Prefs.get (AP_PREFS_SHOWANGLE, showAngle);
		showRoundness = Prefs.get (AP_PREFS_SHOWROUNDNESS, showRoundness);
		showVariance = Prefs.get (AP_PREFS_SHOWVARIANCE, showVariance);
		showErrors = Prefs.get (AP_PREFS_SHOWERRORS, showErrors);
        showSNR = Prefs.get (AP_PREFS_SHOWSNR, showSNR);
		showRADEC = Prefs.get (AP_PREFS_SHOWRADEC, showRADEC);

		starOverlay = Prefs.get (AP_PREFS_STAROVERLAY, starOverlay);
		skyOverlay = Prefs.get (AP_PREFS_SKYOVERLAY, skyOverlay);
        nameOverlay = Prefs.get (AP_PREFS_NAMEOVERLAY, nameOverlay);
		valueOverlay = Prefs.get (AP_PREFS_VALUEOVERLAY, valueOverlay);
		tempOverlay = Prefs.get (AP_PREFS_TEMPOVERLAY, tempOverlay);
		clearOverlay = Prefs.get (AP_PREFS_CLEAROVERLAY, clearOverlay);
		}

	/**
	 * Help routine to convert real pixel position + aperture radius into
	 * centered pixel rectangle.
	 */
	public static Rectangle a2rect (double x, double y, double r)
		{
		Rectangle rect = new Rectangle();

		rect.width = (int)(2.0*r);
		rect.height = rect.width+1-(rect.width%2); // SHOULD BE ODD
		rect.width = rect.height;
		rect.x = (int)(x-Centroid.PIXELCENTER) - rect.width/2;
		rect.y = (int)(y-Centroid.PIXELCENTER) - rect.height/2;
		return rect;
		}
	}

/*
			addOvalRoi (xCenter,yCenter,radius);
			if (skyOverlay)
				{
				addOvalRoi (xCenter,yCenter,rBack1);
				addOvalRoi (xCenter,yCenter,rBack2);
				}
			int off = (int)radius;
			if (skyOverlay) off = (int)rBack2;
			addStringRoi (xCenter+off,yCenter,"   "+g.format(source));
*/


