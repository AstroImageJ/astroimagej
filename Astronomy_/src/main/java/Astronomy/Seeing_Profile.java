package Astronomy;// Seeing_Profile.java

import astroj.*;
import ij.IJ;
import ij.ImagePlus;
import ij.Prefs;
import ij.gui.GenericDialog;
import ij.gui.ImageCanvas;
import ij.gui.Plot;
import ij.gui.PlotWindow;
import ij.measure.Calibration;
import ij.plugin.filter.PlugInFilter;
import ij.process.ImageProcessor;

import java.awt.*;
import java.text.DecimalFormat;
import java.util.Locale;

/**
 * Plots radial profile of star-like object.
 */
public class Seeing_Profile implements PlugInFilter
	{
        private final boolean isBatch;
        double autoModeFluxCutOff = 0.010;
	ImagePlus imp;
	boolean canceled=false;
	double X0;
	double Y0;
	double mR = 10;
    double mR1 = 15;
    double mR2 = 30;
	double peak = Double.MIN_VALUE;
    double peak_raw = Double.MIN_VALUE;
    double low_raw = 0.0;
    double meanPeak = Double.MIN_VALUE;
    double meanPeakRaw = Double.MIN_VALUE;
	double background;
	double fwhm;
    double r1, r2, r3;

	int nBins=100;
	double[] radii = null;
	double[] means = null;
    double[] means_raw = null;
    int[] count =null;

    Calibration cal;
	Centroid center;
	PlotWindow pw;
    public Plot plot;
    double[] nullX = null;
    double[] nullY = null;
    ImageCanvas canvas;
    OverlayCanvas ocanvas;
    
    boolean fromClick = false;

	boolean estimate = true;
	boolean recenter = true;
	boolean subtract = false;
    boolean roundRadii = true;

	static final public double SEEING_RADIUS1 = 1.7;	// IN UNITS OF fwhm
	static final public double SEEING_RADIUS2 = 3.4;
	static final public double SEEING_RADIUS3 = 6.8; // EQUAL NUMBERS OF PIXELS

	DecimalFormat  df;

	public int setup(String arg, ImagePlus imp)
		{
        fromClick = arg.equals("alt-click") || imp.getRoi() == null;
		this.imp = imp;
        canvas = imp.getCanvas();
        ocanvas = OverlayCanvas.getOverlayCanvas (imp);
        if (fromClick) return DOES_ALL+NO_UNDO+NO_CHANGES;
        else return DOES_ALL+NO_UNDO+ROI_REQUIRED+NO_CHANGES;
		}

	public void run(ImageProcessor ip)
		{
        Locale.setDefault(IJU.locale);
        df = new DecimalFormat("#####0.00", IJU.dfs);
        cal = imp.getCalibration();
        autoModeFluxCutOff = Prefs.get (MultiAperture_.PREFS_AUTOMODEFLUXCUTOFF, autoModeFluxCutOff);
        if (fromClick)
            {
            X0 = canvas.offScreenXD(canvas.xClicked);
            Y0 = canvas.offScreenYD(canvas.yClicked);  
            mR = Prefs.get ("aperture.radius", mR);
            mR1 = Prefs.get ("aperture.rback1", mR*1.2);
            mR2 = Prefs.get ("aperture.rback2", mR*2.0);
            recenter = Prefs.get("aperture.reposition", true);
            }
        else
            {
            Rectangle r = imp.getRoi().getBounds();
            if (r == null) return;

            int x = r.x;
            int y = r.y;
            int w = r.width;
            int h = r.height;
            X0 = (double)x+0.5*(double)(w-1);
            Y0 = (double)y+0.5*(double)(h-1);
            mR = 0.5*(double)(w+h);
            mR1 = mR*1.2;
            mR2 = mR*2.0;
            
            centerROI();
            doDialog();
            if (canceled) return;
            centerROI();            
            }
	

		// GET CENTROID WITHIN Aperture/ROI


        center = new Centroid();
        center.measure(imp, X0, Y0, mR, mR1, mR2, recenter, Prefs.get("aperture.backplane", false), Prefs.get("aperture.removebackstars", true));
        X0 = center.x();
        Y0 = center.y();
        background = center.background();
        peak = center.peak();
        peak_raw = peak;
        low_raw = 0.0;
        if (fromClick)
            {
            ApertureRoi roi = new ApertureRoi (X0,Y0,mR,mR1,mR2,Double.NaN,recenter);
			roi.setAppearance (true,recenter,Prefs.get("aperture.skyoverlay", false),false,false,Color.RED,"",Double.NaN);
			roi.setImage (imp);
			ocanvas.add (roi);
			canvas.repaint();
            }
        else
            {
            centerROI();
            }
        
		getRadialDistribution(ip);
        
        createPlot(ip);
       

		// ASK WHETHER TO SAVE APERTURE RADII DERIVED FROM PSF

//		if (estimate) saveRadii();
//		if (subtract) subtractProfile();
//        imp.changes = false;
		}

        public Seeing_Profile() {
            this(false);
        }

        public Seeing_Profile(boolean isBatch) {
            this.isBatch = isBatch;
        }

        public record ApRadii(double r, double r2, double r3, boolean centroidSuccessful) {
            public ApRadii(double r, double r2, double r3) {
                this(r, r2, r3, true);
            }

            public boolean isValid() {
                return r != 0 && r2 != 0 && r3 != 0;
            }

            public ApRadii setSuccess(boolean success) {
                return new ApRadii(r, r2, r3, success);
            }
        }

        public static ApRadii getRadii(ImagePlus imp, double x, double y) {
            return getRadii(imp, x, y, Prefs.get(MultiAperture_.PREFS_AUTOMODEFLUXCUTOFF, 0.010));
        }

        public static ApRadii getRadii(ImagePlus imp, double x, double y, double cutoff) {
            return getRadii(imp, x, y, cutoff, true);
        }

        public static ApRadii getRadii(ImagePlus imp, double x, double y, double cutoff, boolean show) {
            var sp = new Seeing_Profile();
            return sp.getRadii(imp, x, y, cutoff, show, Prefs.get("aperture.reposition", true));
        }

        public ApRadii getRadii(ImagePlus imp, double x, double y, double cutoff, boolean show, boolean centroid) {
            var sp = this;
            sp.cal = imp.getCalibration();
            sp.autoModeFluxCutOff = cutoff;
            sp.X0 = x;
            sp.Y0 = y;
            sp.mR = Prefs.get ("aperture.radius", sp.mR);
            sp.mR1 = Prefs.get ("aperture.rback1", sp.mR*2);
            sp.mR2 = Prefs.get ("aperture.rback2", sp.mR*4);
            sp.recenter = centroid;
            sp.center = new Centroid();
            var b = sp.center.measure(imp, sp.X0, sp.Y0, sp.mR, sp.mR1, sp.mR2, sp.recenter, Prefs.get("aperture.backplane", false), Prefs.get("aperture.removebackstars", true));
            sp.X0 = sp.center.x();
            sp.Y0 = sp.center.y();
            sp.background = sp.center.background();
            sp.peak = sp.center.peak();
            sp.peak_raw = sp.peak;
            sp.low_raw = 0.0;
            sp.meanPeak = Double.MIN_VALUE;

            sp.getRadialDistribution(imp.getProcessor());
            if (show) {
                sp.df = new DecimalFormat("#####0.00", IJU.dfs);
                sp.imp = imp;
                sp.createPlot(imp.getProcessor());
            }

            return new ApRadii(sp.r1, sp.r2, sp.r3, b);
        }


	/**
	 * Calculates the radial distribution of intensities around the center (X0,Y0).
	 */
	protected void getRadialDistribution(ImageProcessor ip)
		{
        int iterations = 0;
        boolean foundR1 = false;
        while (!foundR1 && iterations < 10)
            {
            nBins = (int)mR;
            radii = new double[nBins];
            means = new double[nBins];
            means_raw = new double[nBins];
            count = new int[nBins];

            double R,z;

            int xmin = (int)(X0-mR);
            int xmax = (int)(X0+mR);
            int ymin = (int)(Y0-mR);
            int ymax = (int)(Y0+mR);
            peak = ip.getPixelValue((int)X0,(int)Y0);
            
            // ACCUMULATE ABOUT CENTROID POSITION

            for (int j=ymin; j < ymax; j++)
                {
                double dy = (double)j+Centroid.PIXELCENTER-Y0;
                for (int i=xmin; i < xmax; i++)
                    {
                    double dx = (double)i+Centroid.PIXELCENTER-X0;
                    R = Math.sqrt(dx*dx+dy*dy);
                    int bin = (int)R; //Math.round((float)R);  //
                    if (bin >= nBins)  continue; //bin = nBins-1;
                    z = ip.getPixelValue(i,j);
                    radii[bin] += R;
                    means[bin] += z;
                    count[bin]++;
                    if (z > peak) peak=z;
                    if (z < low_raw) low_raw=z;
                    }
                }

            for (int bin=0; bin<nBins; bin++)
                {
                if (count[bin]>0 && (means[bin]/count[bin]) > meanPeak) meanPeak = means[bin]/(double) count[bin];
                }
            meanPeakRaw = meanPeak;
            meanPeak -= background;

            // NORMALIZE

    //		radii[0] = 0.0;
    //		means[0] = 0.0;
            peak_raw = peak;
            peak -= background;
            for (int bin=0; bin < nBins; bin++)
                {
                if (count[bin] > 0)
                    {
                    means_raw[bin] = means[bin] / (double) count[bin];
                    means[bin]  =  ((means[bin] / (double) count[bin]) - background)/meanPeak;
                    radii[bin] /= (double) count[bin];
                    }
                else
                    {
    //                IJ.log("No samples at radius "+bin);
                    means_raw[bin] = Double.NaN;
                    means[bin] = Double.NaN;
                    radii[bin] = Double.NaN;
                    }
                }

            // CALIBRATE X-AXIS USING LEFT-OVER BIN


            if ("pixel".equals(cal.getUnit()))
                {
                for (int k=0; k < nBins; k++)
                    radii[k]  *= cal.pixelWidth;
                }

            // FIND FWHM

            fwhm = 0.0;
            boolean foundFWHM = false;

            for (int bin=1; bin < nBins; bin++)
                {
                if (!foundFWHM && means[bin-1] > 0.5 && means[bin] <= 0.5)
                    {
                    if (bin+1 < nBins && means[bin+1] > means[bin] && bin+2 < nBins && means[bin+2] > means[bin]) continue;
                    double m = (means[bin]-means[bin-1])/(radii[bin]-radii[bin-1]);
                    fwhm = 2.0*(radii[bin-1] + (0.5 - means[bin-1])/m);
                    foundFWHM = true;
                    }
                else if (foundFWHM && bin < nBins-5)
                    {
                    if (means[bin] < autoModeFluxCutOff) 
                        {
                        r1 = radii[bin];
                        r2 = r1*2;
                        r3 = r2*2;
                        foundR1 = true;
                        break;
                        }
                    }
                }
            if (!foundR1)
                {
                mR+=10;
                mR1+=10;
                mR2+=10;
                }
            iterations++;
            }

        if (roundRadii) {
            r1 = Math.ceil(r1);
            r2 = Math.ceil(r2);
            r3 = Math.ceil(r3);
        }
        
        
        if (!foundR1)
            {
    		r1 = (fwhm*SEEING_RADIUS1);
            r2 = (fwhm*SEEING_RADIUS2);
            r3 = (fwhm*SEEING_RADIUS3);
            }

        if (roundRadii) {
            r1 = (int)(r1);
            r2 = (int)(r2);
            r3 = (int)(r3);
        }

        Prefs.set("seeingprofile.radius", r1);
        Prefs.set("seeingprofile.rback1", r2);
        Prefs.set("seeingprofile.rback2", r3);
        }
    

		// CREATE PLOT WITH LABELS
    protected void createPlot(ImageProcessor ip)
        {
        int plotOptions = 0;
        int plotWidth = 600;
        int plotHeight = 600;
        double xMin = 0;
        double xMax = Math.max(mR,r3*1.1);
        double xRange = xMax - xMin;
        double yMax = peak_raw*1.05; //1.3;
        double yMin = -yMax*0.3;//-0.3;
        double yRange = yMax - yMin;
     
        plotOptions += ij.gui.Plot.X_TICKS;
        plotOptions += ij.gui.Plot.Y_TICKS;
        plotOptions += ij.gui.Plot.X_GRID;
        plotOptions += ij.gui.Plot.Y_GRID;
        plotOptions += ij.gui.Plot.X_NUMBERS;
        plotOptions += ij.gui.Plot.Y_NUMBERS;
        if (plot == null) plot = new Plot ("Seeing Profile","Radius ["+cal.getUnits()+"]","ADU",nullX,nullY,plotOptions);
        plot.setSize(plotWidth, plotHeight);
        plot.setLimits (xMin, xMax, yMin, yMax);
        double xPixels = plotWidth - (Plot.LEFT_MARGIN+Plot.RIGHT_MARGIN+1);
        double yPixels = plotHeight - (Plot.TOP_MARGIN+Plot.BOTTOM_MARGIN);
         
        plotAllPoints(ip);

        plot.setColor(Color.MAGENTA);
        plot.setLineWidth(3);
      
        plot.addPoints(radii, means_raw, ij.gui.Plot.LINE);
        plot.setLineWidth(1);

		plot.setColor (Color.RED);
		double x1[] = new double[] {0.0, r1,   r1};
		double y1[] = new double[] {meanPeakRaw, meanPeakRaw, yMin*0.725};
		plot.addPoints (x1, y1, PlotWindow.LINE);
		double x2[] = new double[] {r2,   r2,   r3,  r3};
		double y2[] = new double[] {yMin*0.725, meanPeakRaw, meanPeakRaw, yMin*0.725};
		plot.addPoints (x2, y2, PlotWindow.LINE);  
        
        plot.setJustification(ImageProcessor.RIGHT_JUSTIFY);
		plot.addLabel ((r1-xMin)/xRange, 1.0-((meanPeakRaw-yMin)/yRange),"SOURCE");
        plot.setJustification(ImageProcessor.CENTER_JUSTIFY);
		plot.addLabel ((r2+r3-2.0*xMin)/2.0/xRange,1.0-((meanPeakRaw-yMin)/yRange),"BACKGROUND");   
        
        plot.setColor(new Color(50,205,50));  //dark green
        plot.setLineWidth(1);
        double dashLength = 5*(yRange)/yPixels;
        double numDashes = (meanPeakRaw-(yMin*0.725))/dashLength;        //plot dMarker2
        for (int dashCount = 0; dashCount < numDashes; dashCount +=2)    
            {
            plot.drawLine((fwhm)/2.0, meanPeakRaw-dashLength*dashCount, (fwhm)/2.0, meanPeakRaw-dashLength*(dashCount+1));
            }        
      
        plot.setJustification(ImageProcessor.CENTER_JUSTIFY);
		plot.setLineWidth(1);
        plot.addLabel(fwhm/2.0/xRange, 1.0 - (-yMin*0.27/yRange - 17.0/yPixels), "HWHM");
        plot.addLabel(fwhm/2.0/xRange, 1.0 - (-yMin*0.27/yRange - 31.0/yPixels), df.format(fwhm/2.0));
        
        plot.setColor(Color.RED);
        plot.addLabel(r1/xRange, 1.0 - (-yMin*0.27/yRange - 17.0/yPixels), "Radius");
        plot.addLabel(r1/xRange, 1.0 - (-yMin*0.27/yRange - 31.0/yPixels), df.format(r1));
        
        plot.addLabel(r2/xRange, 1.0 - (-yMin*0.27/yRange - 17.0/yPixels), "Back>");
        plot.addLabel(r2/xRange, 1.0 - (-yMin*0.27/yRange - 31.0/yPixels), df.format(r2));
        
        plot.addLabel(r3/xRange, 1.0 - (-yMin*0.27/yRange - 17.0/yPixels), "<Back");
        plot.addLabel(r3/xRange, 1.0 - (-yMin*0.27/yRange - 31.0/yPixels), df.format(r3));

        plot.setColor(Color.BLACK);
		plot.addLabel(0.5, -32.0/yPixels, "Image: "+IJU.getSliceFilename(imp));
		plot.addLabel(0.5, -16.0/yPixels, "FITS Center: ("+df.format(IJU.ijX2fitsX(X0))+",  "+df.format(IJU.ijY2fitsY(imp.getHeight(), Y0))+")");
        double pixScale = 0;

        if (imp.getWindow() instanceof AstroStackWindow)
            {
            AstroStackWindow asw = (AstroStackWindow)imp.getWindow();
            if (asw.hasWCS() && asw.getWCS().hasScale)
                {
                double pixScaleX = asw.getWCS().getXScaleArcSec();
                double pixScaleY = asw.getWCS().getYScaleArcSec();    
                pixScale = (pixScaleX + pixScaleY) / 2.0;
                }
            }
		plot.addLabel(0.5,0.0,"FWHM: "+df.format(fwhm)+" ["+cal.getUnits()+"]"+(pixScale > 0 ? " : "+df.format(fwhm*pixScale)+" [arcsec]" : ""));
        plot.setJustification(ImageProcessor.LEFT_JUSTIFY);
        if (!isBatch) {
            plot.show().getImagePlus().setPlot(plot);
        } else {
            plot.appendToStack();
        }
		}

        public void setRoundRadii(boolean roundRadii) {
            this.roundRadii = roundRadii;
        }

	/**
	 * Plots all the points in the ROI versus radius as points (not binned).
	 */
	protected void plotAllPoints (ImageProcessor ip)
		{
		int xmin = (int)(X0-mR);
		int xmax = (int)(X0+mR);
		int ymin = (int)(Y0-mR);
		int ymax = (int)(Y0+mR);
		int n = (xmax-xmin+1)*(ymax-ymin+1);

		double radii[] = new double[n];
		double fluxes[] = new double[n];
		double val = 0.0;

		int num=0;
		for (int j=ymin; j <= ymax; j++)
			{
			double dy = (double)j+Centroid.PIXELCENTER-Y0;
			for (int i=xmin; i <= xmax; i++)
				{
				double dx = (double)i+Centroid.PIXELCENTER-X0;
				double R = Math.sqrt(dx*dx+dy*dy);
				radii[num] = R;
				val = (ip.getPixelValue(i,j)); //-background)/meanPeak;
				fluxes[num] = val;
				num++;
				}
			}
		plot.setColor (Color.BLUE);
		plot.addPoints (radii, fluxes, PlotWindow.BOX);
//		plot.setVisible(true);
		}
    
	/**
	 * Dialogue which lets the user adjust the radius.
	 */
	protected void doDialog()
		{
		canceled=false;
		GenericDialog gd = new GenericDialog("Seeing Profile for "+imp.getShortTitle(), IJ.getInstance());
		gd.addMessage ("X center (pixels) : "+df.format(X0));
		gd.addMessage ("Y center (pixels) : "+df.format(Y0));
		gd.addNumericField("Radius (pixels):", mR,2);
		gd.addCheckbox ("Recenter", recenter);
		gd.addCheckbox ("Estimate aperture radii", estimate);
		gd.addCheckbox ("Subtract mean profile", subtract);

		gd.showDialog();
		if (gd.wasCanceled())
			{
			canceled = true;
			return;
			}
		mR=gd.getNextNumber();
		if (gd.invalidNumber())
			{
			IJ.error("Invalid input Number");
			canceled=true;
			return;
			}
		recenter = gd.getNextBoolean();
		estimate = gd.getNextBoolean();
		subtract = gd.getNextBoolean();
		}
    

	protected void centerROI ()
		{
		IJ.makeOval((int)(X0-mR), (int)(Y0-mR), (int)(2*mR+1.0), (int)(2*mR+1.0));
		}

	protected void saveRadii ()
		{
//		double r1 = (int)(fwhm*SEEING_RADIUS1);
//		double r2 = (int)(fwhm*SEEING_RADIUS2);
//		double r3 = (int)(fwhm*SEEING_RADIUS3);

//		pw.setColor (Color.RED);
//		double x[] = new double[] {0.0, r1,   r1,  r2,   r2,   r3,  r3};
//		double y[] = new double[] {1.0, 1.0, 0.0, 0.0, 1.0, 1.0, 0.0};
//		pw.addPoints (x, y, PlotWindow.LINE);
//		pw.draw();
//
//		pw.addLabel ((0.5*r1/Math.max(mR,fwhm*SEEING_RADIUS3*1.1))-0.05,0.09,"SOURCE");
//		pw.addLabel (1.0*r2/Math.max(mR,fwhm*SEEING_RADIUS3*1.1),0.09,"BACKGROUND");
		GenericDialog gd = new GenericDialog ("Save Aperture Radii?");
		gd.addMessage ("The following aperture radii were derived from the seeing profile :");
		gd.addNumericField ("Source radius : ", r1,2);
		gd.addNumericField ("Minimum background radius [pixels] : ", r2,2);
		gd.addNumericField ("Maximum background radius [pixels] : ",r3,2);
		gd.addMessage ("Select CANCEL to keep previous aperture radii.");
		gd.showDialog();
		if (gd.wasCanceled()) return;

		r1 = gd.getNextNumber();
		if (! gd.invalidNumber())
			Prefs.set (Aperture_.AP_PREFS_RADIUS, r1);
		r2  = gd.getNextNumber();
		if (! gd.invalidNumber())
			Prefs.set (Aperture_.AP_PREFS_RBACK1, r2);
		r3 = gd.getNextNumber();
		if (! gd.invalidNumber())
			Prefs.set (Aperture_.AP_PREFS_RBACK2, r3);
        Prefs.set("setaperture.aperturechanged",true);
		}
    

	/**
	 * Calculates a crude background value using the edges of the ROI.
	 */
	protected double crudeBackground (ImageProcessor ip)
		{
		Rectangle r = ip.getRoi().getBounds();
		int x = r.x;
		int y = r.y;
		int w = r.width;
		int h = r.height;
		double b = 0.0;
		int n=0;
		for (int i=x; i < x+w; i++)
			{
			b += ip.getPixelValue(i,y);
			n++;
			}
		for (int i=x; i < x+w; i++)
			{
			b += ip.getPixelValue(i,y+h);
			n++;
			}
		for (int j=y; j < y+h; j++)
			{
			b += ip.getPixelValue(x,j);
			n++;
			}
		for (int j=y; j < y+h; j++)
			{
			b += ip.getPixelValue(x+w,j);
			n++;
			}
		if (n == 0)
			return 0.0;
		else
			return b/(double)n;
		}    


	double maximumOf(double[] arr)
		{
		int n=arr.length;
		double mx = arr[0];
		for (int i=1; i < n; i++)
			mx = (arr[i] > mx)? arr[i] : mx;
		return mx;
		}

	/**
	 * Subtract the mean profile from the entire image.
	 */

	void subtractProfile()
		{
		int h = imp.getHeight();
		int w = imp.getWidth();
		double x,y,r,z,c;
		ImageProcessor ip = imp.getProcessor();

		for (int j=0; j < h; j++)
			{
			y = (double)j-Y0;
			for (int i=0; i < w; i++)
				{
				x = (double)i-X0;
				r = Math.sqrt(x*x+y*y);
				if (r >= radii[nBins-1])
					z = means[nBins-1];
				else	{
					int k=nBins-1;
					while (r < radii[k] && k > 0) k--;
					z = means[k];
					}
				c = ip.getPixelValue(i,j)-peak*z;
				ip.putPixelValue(i,j,c);
				}
			}
		}
	}
