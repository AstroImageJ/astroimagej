package Astronomy;// Create_Mean_PSF.java

import ij.*;
import ij.gui.*;
import ij.measure.ResultsTable;
import ij.plugin.filter.PlugInFilter;
import ij.process.*;

import astroj.*;

import Jama.Jama.*;


/**
 * Uses the JAMA matrix package: see http://math.nist.gov/javanumerics/jama/.
 *
 * @author F.V. Hessman, Georg-August-Universitaet Goettingen
 * @version 1.0
 * @date 2010-FEB-23
 */
public class Create_Mean_PSF implements PlugInFilter
	{
	String[] tableList;
	ImagePlus workImage;
	MeasurementTable table;
	double[] xm, ym;
	int wPsf,hPsf;

	/** Standard ImageJ setup routine. */
	public int setup (String arg, ImagePlus imag)
		{
		workImage = imag;
		IJ.register(Create_Mean_PSF.class);
		return DOES_ALL;
		}

	/**
	 */
	public void run(ImageProcessor ip)
		{
		// FIND MEASUREMENT TABLES 

		tableList = MeasurementTable.getMeasurementTableNames();
		int numTables = tableList.length;

		// IF NOT ENOUGH, EXIT

		if (numTables < 1)
			{
			IJ.error ("No results tables available!");
			return;
			}

		// SELECT THE TABLE

		if (numTables == 1 || tableList[0].equals(tableList[1]))
			{
			table = new MeasurementTable(tableList[0]);
			if (table == null)
				{
				IJ.error ("Unable to access the table "+tableList[0]);
				return;
				}
			}
		else if (!selectTable())
			return;

		// GET THE TABLE COLUMNS WITH THE POSITIONS 

		if (!getTableColumns()) return;

		// CREATE MEAN IMAGE

		getMeanImage();
		}

	/**
	 * Lets the user select the file to be rebinned and the reference file.
	 */
	protected boolean selectTable()
		{
		// SHOW POSSIBLE TABLES IN DIALOGUE

 		GenericDialog gd = new GenericDialog ("Create Mean PSF", IJ.getInstance());
		gd.addChoice ("Select the table you want to use:", tableList, tableList[0]);

		// DO DIALOGUE

		gd.showDialog ();
		if (gd.wasCanceled())
			return false;

		// GET TABLE

		int t = gd.getNextChoiceIndex();
		table = MeasurementTable.getTable (tableList[t]);
		if (table == null)
			{
			IJ.error ("Unable to access the table "+tableList[t]);
			return false;
			}
		return true;
		}

	/**
	 * Tries to find the correct columns for the X,Y positions.
	 */
	protected boolean getTableColumns ()
		{
		// SHOW POSSIBLE TABLES IN DIALOGUE

 		GenericDialog gd = new GenericDialog ("Create Mean PSF", IJ.getInstance());
		gd.addMessage ("Select the columns you want to use.");

		// CHECK TABLE COLUMNS

		String[] cols = table.getColumnHeadings().split("\t");
		if (cols.length < 3)
			{
			IJ.error ("Too few columns!");
			return false;
			}
// for (int i=0; i < cols.length; i++)
//	IJ.log("cols["+i+"]="+cols[i]);

		int xCol = table.getColumnIndex("x");
		if (xCol == ResultsTable.COLUMN_NOT_FOUND)
			{
			xCol = table.getColumnIndex("XM");
			if (xCol == ResultsTable.COLUMN_NOT_FOUND)
				xCol = 0;
			}
		int yCol = table.getColumnIndex("y");
		if (yCol == ResultsTable.COLUMN_NOT_FOUND)
			{
			yCol = table.getColumnIndex("YM");
			if (yCol == ResultsTable.COLUMN_NOT_FOUND)
				yCol = 1;
			}

		gd.addChoice ("Select column of X-values : ",cols,cols[xCol+1]);	// NOTE SHIFT BY +1 !!!!!
		gd.addChoice ("Select column of Y-values : ",cols,cols[yCol+1]);
		gd.addNumericField ("Width of PSF : ",11,0);
		gd.addNumericField ("Height of PSF : ",11,0);

		// DO DIALOGUE

		gd.showDialog ();
		if (gd.wasCanceled())
			return false;
		xCol = gd.getNextChoiceIndex();	
		yCol = gd.getNextChoiceIndex();
		wPsf = (int)gd.getNextNumber();
		hPsf = (int)gd.getNextNumber();

		// GET X AND Y DATA

		xm = table.getDoubleColumn(xCol-1);		// NOTE SHIFT BY -1 !!!!!
		ym = table.getDoubleColumn(yCol-1);
		if (xm == null || ym == null || xm.length != ym.length)
			{
			IJ.error("Could not read the columns "+xCol+" and/or "+yCol);
			return false;
			}
		return true;
		}

	/**
	 * Performs the actual averaging of all sub-images.
	 */
	protected void getMeanImage()
		{
		ImageProcessor work = workImage.getProcessor();
		int wWork = workImage.getWidth();
		int hWork = workImage.getHeight();

		// CREATE THE RESULT IMAGE

		ImagePlus psfImage = IJ.createImage("PSF","32-bit",wPsf,hPsf,1);
		ImageProcessor psf = psfImage.getProcessor();

		// COPY FITS HEADER

		FitsJ.copyHeader (workImage,psfImage);
		String[] hdr = FitsJ.getHeader(psfImage);
		FitsJ.makeConsistent (psfImage,hdr);
		FitsJ.putHeader(psfImage,hdr);

		// REBIN

		int n = xm.length;
		double halfw = (double)wPsf*0.5;
		double halfh = (double)hPsf*0.5;
		double maxval = 0.0;
		for (int j=0; j < hPsf; j++)
  			{
			double y = (double)j;
			for (int i=0; i < wPsf; i++)
				{
				double x = (double)i;
				double sum = 0.0;
				for (int k=0; k < n; k++)
					{
					double xw = xm[k]+x-halfw;
					double yw = ym[k]+y-halfh;
					double val = work.getInterpolatedValue(xw,yw);
					sum += val;
					}
				psf.putPixelValue(i,j,sum);
				if (sum > maxval) maxval = sum;
				}
			}

		// SUBTRACT BOUNDARY VALUE AND NORMALIZE

		double sky = getFillValue(psf);
		// psf.add(-sky);
		psf.multiply(1.0/maxval);
IJ.log("sky value = "+sky+", max="+maxval);

		// FINISHED
		psfImage.setDisplayRange(0.0,1.0);
		psfImage.show();
		}

	/**
	 * Returns the mean value around the edges of the image treated by the given ImageProcessor.
	 */
	public double getFillValue (ImageProcessor ip)
		{
		int w = ip.getWidth();
		int h = ip.getHeight();
		double fillValue = 0.0;
		for (int i=0; i < w; i++)
			{
			fillValue += ip.getPixelValue(i,0);
			fillValue += ip.getPixelValue(i,h-1);
			}
		for (int j=1; j < h-1; j++)
			{
			fillValue += ip.getPixelValue(0,j);
			fillValue += ip.getPixelValue(w-1,j);
			}
		fillValue /= (double)(2*w+2*(h-2));
		return fillValue;
		}

	protected boolean withinPixelBounds (double x, double y, int w, int h)
		{
		if (x < 0 || x >= w) return false;
		if (y < 0 || y >= h) return false;
		return true;
		}
	}
 
