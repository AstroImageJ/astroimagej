package Astronomy;// Stack_Aligner.java

import astroj.AstroStackWindow;
import astroj.Centroid;
import astroj.FitsJ;
import astroj.IJU;
import ij.IJ;
import ij.ImagePlus;
import ij.Prefs;
import ij.process.ColorProcessor;
import ij.process.ImageProcessor;
import ij.util.Tools;
import util.GenericSwingDialog;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Locale;

/**
 * Based on MultiAperture_.java
 * 
 * @author F.V. Hessman, Georg-August-Universitaet Goettingen
 * @version 1.0
 * @date 2006-Oct-10
 *
 * @version 1.1
 * @date 2006-Nov-29
 * @changes Made this version an extension of MultiAperture_ to make alignment more robust.
 *
 * @version 1.2
 * @date 2009-FEB-09
 * @changes Added whole pixel shift.
 */
public class Stack_Aligner extends MultiAperture_
	{
	boolean normalize = false;
	boolean whole = true;
    boolean firstImage = true;
    boolean startingRepositionSetting = false;
    boolean startingUseVarApSetting = false;
    boolean startingUseAMag = false;
    boolean isVirtual = false;
    boolean useWCSOnly = true;
    boolean holdSingleStep = false;
    String label = "Aligned_";
    String imageFilename = "";
    String imageDirname = "";
	double[] xRef = null;
	double[] yRef = null;
	double firstImageFlux = 0.0;
    double[] radecRef;
    String slash = IJ.isWindows() ? "\\": "/";
    DecimalFormat uptoTwoPlaces = new DecimalFormat("0.##", IJU.dfs);

	/**
	 * Standard ImageJ PluginFilter setup routine which also determines the default aperture radius.
	 */
	public int setup (String arg, ImagePlus imp)
		{
        Locale.setDefault(IJU.locale);
		if (imp == null) 		// ONLY WORKS IF THERE'S AN IMAGE
            {
            IJ.showMessage("Stack Aligner requires an image stack. No stack found.");
            return DONE;
            }
		IJ.register(Stack_Aligner.class);
        stackSize = imp.getStackSize();
		if (stackSize < 2)
            {
            IJ.showMessage("Stack Aligner requires a stack size > 1.");
            return DONE;
            }        
        startingRepositionSetting = Prefs.get (AP_PREFS_REPOSITION, true);
        startingUseVarApSetting = Prefs.get(MultiAperture_.PREFS_USEVARSIZEAP, false);
        startingUseAMag = Prefs.get (MultiAperture_.PREFS_GETMAGS, startingUseAMag);
        calcRadProFWHM = Prefs.get (Aperture_.AP_PREFS_CALCRADPROFWHM, calcRadProFWHM);
        Prefs.set (MultiAperture_.PREFS_USEVARSIZEAP, false);
        Prefs.set (AP_PREFS_REPOSITION, true);
        Prefs.set (MultiAperture_.PREFS_GETMAGS, false);
        isVirtual = imp.getStack().isVirtual();
        if (isVirtual)
            {
            imageDirname = imp.getOriginalFileInfo().directory+"aligned"+slash;
            File dir = new File(imageDirname);
            if (!dir.exists())
                {
                dir.mkdir();
                }
            else if (dir.isFile())
                {
                IJ.beep();
                IJ.showMessage("Aligning with a virtual stack requires the sub-directory name 'aligned' to be available.\n"+
                               "A file named 'aligned' in the stack directory is blocking the creation of the sub-directory.");
                return DONE;
                }
            }
		return super.setup(arg,imp);
		}
    

	/**
	 * Initializes the reference position arrays.
	 */
	protected boolean prepare ()
		{
		doStack = true;
		return super.prepare();
		}

    
    
	/**
	 * Adds the aperture parameters to the list of apertures.
	 */
//	protected void addAperture (boolean isref)
//		{
//		super.addAperture (isref);
//		}

	/**
	 * Dialog for this MultiAperture_ sub-class
	 */
	protected GenericSwingDialog dialog()
		{
		// CREATE DIALOGUE WINDOW
        var sliders = new ArrayList<JPanel>();
        if (!Prefs.isLocationOnScreen(new Point(xLocation, yLocation))) {
            xLocation = 10;
            yLocation = 10;
        }
		GenericSwingDialog gd = new GenericSwingDialog("Stack Aligner", xLocation, yLocation);

        if (nAperturesStored == 0) previous = false;

		// REQUIRED FIELDS
            //GenericSwingDialog.setSliderSpinnerColumns(3);
		if (stackSize > 1)
			{
            firstSlice = (firstSlice == stackSize || alwaysstartatfirstSlice) ? 1 : firstSlice;
            sliders.add(gd.addSlider("           First slice ", 1, stackSize, firstSlice, d -> firstSlice = d.intValue()));
            sliders.add(gd.addSlider("           Last slice ", 1, stackSize, lastSlice, d -> lastSlice = d.intValue()));
	        }
        gd.addMessage("");
        sliders.add(gd.addSlider("Radius of object aperture", 1, radius>100?radius:100, false, radius, d -> radius = d.intValue()));
        sliders.add(gd.addSlider("Inner radius of background annulus", 1, rBack1>100?rBack1:100, false, rBack1, d -> rBack1 = d.intValue()));
        sliders.add(gd.addSlider("Outer radius of background annulus", 1, rBack2>100?rBack2:100, false, rBack2, d -> rBack2 = d.intValue()));

        useWCSOnly = Prefs.get ("stackAligner.useWCSOnly", useWCSOnly);
        var buttons = new ArrayList<JCheckBox>();
        if (hasWCS) {
            gd.addCheckbox("Use only WCS headers for alignment (no apertures required)", useWCSOnly, b -> {
                useWCSOnly = b;
                buttons.forEach(j -> j.setEnabled(!useWCSOnly));
            });
        }

        var b1 = gd.addCheckbox ("Use previous "+nAperturesStored+" apertures (1-click to set first aperture location)", previous, b -> previous = b);
        buttons.add(b1);
        b1.setEnabled(nAperturesStored > 0);
        buttons.add(gd.addCheckbox ("Use RA/Dec to locate initial aperture positions", useWCS, b -> useWCS = b));
		buttons.add(gd.addCheckbox ("Use single step mode (1-click to set first aperture location in each image)",singleStep, b -> singleStep = b));
        buttons.add(gd.addCheckbox ("Allow aperture changes between slices in single step mode (right click to advance image)",allowSingleStepApChanges, b -> allowSingleStepApChanges = b));
		gd.addMessage ("");

        sliders.forEach(s -> GenericSwingDialog.getTextFieldFromSlider(s).ifPresent(tf -> tf.setColumns(5)));

		// NON-REQUIRED FIELDS (mirrored in finishFancyDialog())

        normalize = Prefs.get ("stackAligner.normalize", normalize);
        whole = Prefs.get ("stackAligner.whole", whole);
		gd.addCheckbox ("Remove background and scale to common level", normalize, b -> normalize = b);
		gd.addCheckbox ("Align only to whole pixels (no interpolation)",whole, b -> whole = b);
        gd.addMessage ("");
        buttons.add(gd.addCheckbox ("Show help panel during aperture selection.", showHelp, b -> showHelp = b));
        gd.addCheckbox("Update display while running", updateImageDisplay.get(), updateImageDisplay::set);
        gd.addMessage ("");
        if (hasWCS) {
            buttons.forEach(j -> j.setEnabled(!useWCSOnly));
        }
        if (imp.getStack().isVirtual())
            {
            gd.addMessage ("NOTE: ***THIS IS A VIRTUAL STACK***\nAligned images will be placed in the sub-directory 'aligned'.\n"+ 
                           "The new aligned stack must be opened after alignment processing is finished.\n");

            }
		gd.addMessage ("Click \"OK\" and select image alignment stars with left clicks.\nThen right click or press <Enter> to begin alignment process.\n"+
                       (hasWCS?"If 'use only WCS headers for alignment' mode is selected, processing will start when \"OK\" is clicked.\n":"")+
		               "To abort alignment star selection or processing, press <ESC>.");
		return gd;
		}

	/**
	 * Parses the non-required fields of the dialog and cleans up thereafter.
	 */
	protected boolean finishFancyDialog (GenericSwingDialog gd)
		{
        holdSingleStep = singleStep;
        if (hasWCS) 
            {
            runningWCSOnlyAlignment = useWCSOnly;
            }
        
        Prefs.set ("stackAligner.useWCSOnly", useWCSOnly);
        Prefs.set ("stackAligner.normalize", normalize);
        Prefs.set ("stackAligner.whole", whole);   
        Prefs.set (MultiAperture_.PREFS_SHOWHELP, showHelp);
		xPos = new double[nApertures];
		yPos = new double[nApertures];
		ngot = 0;
		xRef = new double[nApertures];
		yRef = new double[nApertures];
        
        showHelpPanel();
        if (runningWCSOnlyAlignment)
            {
            lastSlice = initialLastSlice;
            singleStep = false;
            }
		return true;
		}

	/**
	 * Perform photometry on each image of selected sub-stack.
	 */
	protected void processStack ()
		{
		// GET MEAN APERTURE BRIGHTNESS
        if (firstImage)
            {
            if (runningWCSOnlyAlignment)
                { 
                if (!hasWCS)
                    {
                    IJ.beep();
                    IJ.showMessage("'Use WCS only' option selected, but no WCS headers found in image "+IJU.getSliceFilename(imp, slice)+". Aborting alignment.");
                    shutDown();
                    return;
                    }
                else
                    {                
                    radecRef = wcs.pixels2wcs(new double[]{imp.getWidth()/2.0,imp.getHeight()/2.0});
//                    IJ.log("raRef="+IJU.decToSexRA(radecRef[0])+"     decRef="+IJU.decToSexDec(radecRef[1]));
//                    IJ.log("new first image");
                    }
                }
            else
                {
                for (int i=0; i < nApertures; i++)
                    {
                    xRef[i] = xPos[i];
                    yRef[i] = yPos[i];
                    }
                }
                String titl = imp.getShortTitle();
                if (titl == null)
                    titl = imp.getTitle();
                label += titl;
            }


		// PROCESS STACK
        if (!singleStep) processingStack = true;
		super.processStack ();

		// RENAME RESULTING ALIGNED STACK

		SwingUtilities.invokeLater(() -> {
            imp.updateAndDraw();
            imp.setTitle(label);
        });
        Prefs.set (AP_PREFS_REPOSITION, startingRepositionSetting);
        Prefs.set (MultiAperture_.PREFS_USEVARSIZEAP, startingUseVarApSetting);
        if (isVirtual && slice >= stackSize) IJ.showMessage("Virtual stack alignment complete.\nAligned images are saved in subdirectory 'aligned'."); 
        Prefs.set (MultiAperture_.PREFS_SINGLESTEP, holdSingleStep);
        Prefs.set (MultiAperture_.PREFS_GETMAGS, startingUseAMag);
        }

	/**
	 * Performs processing of single images.
	 */
	protected void processImage ()
		{

        double dx = 0.0;
        double dy = 0.0; 
        double dxNoCen = 0.0;
        double dyNoCen = 0.0;
        double[] xy;
        int numCentroids = 0;
        int numNoCentroids = 0;
        if (runningWCSOnlyAlignment)
            {
            if (!hasWCS)
                {
                IJ.beep();
                IJ.showMessage("'Use WCS only' option selected, but no WCS headers found in image "+IJU.getSliceFilename(imp, slice)+". Aborting alignment.");
                shutDown();
                return;
                }
            else
                {
                xy = wcs.wcs2pixels(radecRef);
//                IJ.log("x["+imp.getSlice()+"][radecRef]="+xy[0]+"     y["+imp.getSlice()+"][radecRef]="+xy[1]);
//                double[] radecCen = wcs.pixels2wcs(new double[]{imp.getWidth()/2.0,imp.getHeight()/2.0});
//                IJ.log("raCen["+imp.getSlice()+"]="+IJU.decToSexRA(radecCen[0])+"     decCen["+imp.getSlice()+"]="+IJU.decToSexDec(radecCen[1]));
//                IJ.log("raRef["+imp.getSlice()+"]="+IJU.decToSexRA(radecRef[0])+"     decRef["+imp.getSlice()+"]="+IJU.decToSexDec(radecRef[1]));
                dx = xy[0] - imp.getWidth()/2.0;
                dy = xy[1] - imp.getHeight()/2.0;
                }
            }
        else
            {
            // NORMAL APERTURE MEASUREMENTS (INCLUDING MEAN back and source)

            super.processImage ();

            if (firstImage)
                firstImageFlux = (src[0]+tot[0])/nApertures;

            // PERFORM BACKGROUND SUBTRACTION AND NORMALIZATION

            if (normalize)
                {
                ip.resetRoi();		// SO CAN PERFORM ON WHOLE IMAGE
                ip.add (-back);	// REMOVE MEAN BACKGROUND
                if (!firstImage)		// NORMALIZE TO STANDARD FLUX
                    ip.multiply (firstImageFlux/((src[0]+tot[0])/nApertures));
                }

            // SHIFT IMAGE

            for (int i=0; i < nApertures; i++)
                {
                if (centroidStar[i])
                    {
                    dx += xPos[i]-xRef[i];
                    dy += yPos[i]-yRef[i];
                    numCentroids++;
                    }
                else
                    {
                    dxNoCen += xPos[i]-xRef[i];
                    dyNoCen += yPos[i]-yRef[i];
                    numNoCentroids++;
                    }
                }
            if (numCentroids == 0)
                {
                dx = dxNoCen/numNoCentroids;
                dy = dyNoCen/numNoCentroids;
                //IJ.beep();
                //IJ.showMessage("No centroid apertures selected. Aborting alignment. Re-run and include apertures with centroid enabled.");
                //shutDown();
                //return;
                }
            else
                {
                dx /= numCentroids;
                dy /= numCentroids;                
                }
            }
        
        imageFilename = IJU.getSliceFilename(imp, slice);
        if (isVirtual)
            {
            
//            ImagePlus imp2 = (ImagePlus)imp.clone();

            ImagePlus imp2 = new ImagePlus(imp.getStack().getSliceLabel(slice), imp.getStack().getProcessor(slice)); 
            imp2.setCalibration(imp.getCalibration());  
            imp2.setFileInfo(imp.getFileInfo());
            imp2.setProcessor (null, imp.getType()==ImagePlus.COLOR_RGB ? shiftedRGBImage(dx,dy) : shiftedImage(dx,dy));
            var scienceHeader = FitsJ.getHeader(imp);
            if (scienceHeader != null)
                {
                scienceHeader = updateHeaders(scienceHeader, dx, dy);
                FitsJ.putHeader(imp2, scienceHeader);
                }
            IJU.saveFile(imp2, imageDirname+slash+"aligned_"+imageFilename);
            }
        else
            {
            imp.setProcessor (null, imp.getType()==ImagePlus.COLOR_RGB ? shiftedRGBImage(dx,dy) : shiftedImage(dx,dy));
            //imp.getStack().setSliceLabel("aligned_" + imageFilename, slice);
            var scienceHeader = FitsJ.getHeader(imp);
            if (scienceHeader != null)
                {
                scienceHeader = updateHeaders(scienceHeader, dx, dy);
                FitsJ.putHeader(imp, scienceHeader);  
                if (imp.getWindow() instanceof astroj.AstroStackWindow)
                    {
                    asw = (AstroStackWindow)imp.getWindow();
                    asw.setAstroProcessor(true);
                    }                
                }
            ocanvas.moveApertureRois(dx,dy);
            ocanvas.repaint();
            }
        firstImage = false;
		}
    
    protected FitsJ.Header updateHeaders(FitsJ.Header header, double dx, double dy)
        { 
        if (whole)
            {
            header = FitsJ.setCard("X_SHIFT", -(int)(Math.floor(dx+Centroid.PIXELCENTER)), "AIJ stack align X-shift value", header);
            header = FitsJ.setCard("Y_SHIFT", -(int)(Math.floor(dy+Centroid.PIXELCENTER)), "AIJ stack align Y-shift value", header);
            if (hasWCS) header = updateCRPIX(header, -(int)(Math.floor(dx+Centroid.PIXELCENTER)), -(int)(Math.floor(dy+Centroid.PIXELCENTER)));
            }
        else
            {
            header = FitsJ.setCard("X_SHIFT", -dx, "AIJ stack align X-shift value", header);
            header = FitsJ.setCard("Y_SHIFT", -dy, "AIJ stack align Y-shift value", header);   
            if (hasWCS) header = updateCRPIX(header, -dx, -dy);
            } 
        return header;
        }
    
    protected FitsJ.Header updateCRPIX(FitsJ.Header header, double dx, double dy)
        {
        double crpix1, crpix2;
        int index;
        index = FitsJ.findCardWithKey("CRPIX1", header);
        if (index >= 0)
            {
            crpix1 = FitsJ.getCardDoubleValue(header.cards()[index]);
            header = FitsJ.setCard("CRPIX1", crpix1+dx, "Adjusted by AIJ Stack_Aligner", header);
            }
        index = FitsJ.findCardWithKey("CRPIX2", header);
        if (index >= 0)
            {
            crpix2 = FitsJ.getCardDoubleValue(header.cards()[index]);
            header = FitsJ.setCard("CRPIX2", crpix2-dy, "Adjusted by AIJ Stack_Aligner", header);
            } 
        
        //UPDATE ANNOTATIONS
        String key;
        String value;
        String label;
        String[] pieces;
        double x, y;
        for (int i=0; i<header.cards().length; i++)
            {
            key = FitsJ.getCardKey(header.cards()[i]);
            if (key != null && key.equals("ANNOTATE")) 
                {
                pieces = FitsJ.getCardStringValue(header.cards()[i]).split(",");
                if (pieces.length > 1)
                    {
                    label = FitsJ.getCardComment(header.cards()[i]);
                    x = Tools.parseDouble(pieces[0], 0);
                    y = Tools.parseDouble(pieces[1], 0);
                    value = "'"+uptoTwoPlaces.format(x+dx)+","+uptoTwoPlaces.format(y-dy);
                    if (pieces.length > 2)
                        {
                        for (int j=2; j<pieces.length; j++)
                            {
                            value += ","+pieces[j];
                            }
                        }
                    value += "'";
                    header.cards()[i] = FitsJ.createCard("ANNOTATE", value, label);
                    }
                }
            }
        return header;
        }

        @Override
        public void shutDown() {
            super.shutDown();
            Prefs.set("stackaligner.finished", true);
        }

        /**
	 * Shifts image linearly by an amount (dx,dy).
	 */
	protected ImageProcessor shiftedImage (double dx, double dy)
		{
        double d = 0.0;
		int h = ip.getHeight();
		int w = ip.getWidth();
        ImageProcessor ips = ip.duplicate();

		for (int j=0; j < h; j++)
			{
			double y = (double)j+dy;
			int iy = (int)(y+Centroid.PIXELCENTER);
			if (whole)
				{
				for (int i=0; i < w; i++)
					{
					double x = (double)i+dx;
					int ix = (int)(x+Centroid.PIXELCENTER);
                    if (ix >= 0 && ix < w && iy >=0 && iy < h)
                        d = (double)ip.getPixelValue(ix, iy);
                    else
                        d = 0.0;
					ips.putPixelValue (i,j,d);
					}
				}
			else	{
				for (int i=0; i < w; i++)
					{
					double x = (double)i+dx;
                    if (x >= 0 && x < w && y >=0 && y < h)
					    d = ip.getInterpolatedPixel(x, y);
                    else
                        d = 0.0;
					ips.putPixelValue (i,j,d);
					}
				}
			}
		return ips;
		}
    
	/**
	 * Shifts image linearly by an amount (dx,dy).
	 */
	protected ColorProcessor shiftedRGBImage (double dx, double dy)
		{
		int h = ip.getHeight();
		int w = ip.getWidth();
        ColorProcessor cp;
        Frame openFrame = imp.getWindow();
        if (openFrame instanceof astroj.AstroStackWindow)
            {
            astroj.AstroStackWindow asw = (astroj.AstroStackWindow)openFrame;
            cp = asw.getcp();
            if (cp == null)
                {
                ip.reset();;
                cp = (ColorProcessor)ip;
                }
            }
        else
            {
            ip.reset();;
            cp = (ColorProcessor)ip;
            }
    
        ColorProcessor cps = (ColorProcessor)(cp.duplicate());

		for (int j=0; j < h; j++)
			{
			double y = (double)j+dy;
			int iy = (int)(y+Centroid.PIXELCENTER);
			if (whole)
				{
				for (int i=0; i < w; i++)
					{
					double x = (double)i+dx;
					int ix = (int)(x+Centroid.PIXELCENTER);
					cps.putPixel(i,j,cp.getPixel(ix, iy));
					}
				}
			else	{
				for (int i=0; i < w; i++)
					{
					double x = (double)i+dx;
					int d = cp.getInterpolatedRGBPixel(x, y);
					cps.putPixel(i,j,cp.getInterpolatedRGBPixel(x, y));
					}
				}
			}
		return cps;
		}    

	}
