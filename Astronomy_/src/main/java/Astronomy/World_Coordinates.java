package Astronomy;// World_Coordinates.java

import ij.*;
import ij.gui.*;
import ij.measure.*;
import ij.plugin.filter.*;
import ij.process.*;

import java.awt.*;
import java.awt.event.*;
import java.util.*;

import astroj.*;

/**
 * @author F.V. Hessman, Georg-August-Universitaet Goettingen
 * @version 1.1
 * @date 2008-Feb-07
 */
public class World_Coordinates implements PlugInFilter
	{
	ImagePlus img;
	ImageCanvas canvas;
	OverlayCanvas ocanvas;
	String filename;
	int slice=0;
	int stackSize=0;
	Rectangle rct;
	DmsFormat form;

	double[] xy = new double[2];
	WCS wcs;

	double[] crpix = null;
	double[][] cd = null;
	int[] npix = null;

	MeasurementTable table = null;

	// PlugInFilter METHODS

	public int setup (String arg, ImagePlus im)
		{
		img = im;
		stackSize = img.getStackSize();
		canvas = img.getCanvas();
		ocanvas = OverlayCanvas.getOverlayCanvas (img);
		form = new DmsFormat (1);
		IJ.register(World_Coordinates.class);
		return DOES_ALL;
		}

	void getPrefs()
		{
		double w = (double)img.getWidth();
		double h = (double)img.getHeight();
		String npix1 = Prefs.get(WCS.PREFS_NPIX1,""+w);
		String npix2 = Prefs.get(WCS.PREFS_NPIX2,""+h);
		String crpix1 = Prefs.get(WCS.PREFS_CRPIX1,""+w/2.0);
		String crpix2 = Prefs.get(WCS.PREFS_CRPIX2,""+h/2.0);
		String cd1_1 = Prefs.get(WCS.PREFS_CD1_1,""+1.0);
		String cd1_2 = Prefs.get(WCS.PREFS_CD1_2,""+0.0);
		String cd2_1 = Prefs.get(WCS.PREFS_CD2_1,""+0.0);
		String cd2_2 = Prefs.get(WCS.PREFS_CD2_2,""+1.0);

		npix = new int[2];
		crpix = new double[2];
		cd = new double[2][2];
		try	{
			crpix[0] = Double.parseDouble(crpix1);
			crpix[1] = Double.parseDouble(crpix2);
			cd[0][0] = Double.parseDouble(cd1_1);
			cd[0][1] = Double.parseDouble(cd1_2);
			cd[1][0] = Double.parseDouble(cd2_1);
			cd[1][1] = Double.parseDouble(cd2_2);
			if (w != npix[0] || h != npix[1])
				{
				double f1 = npix[0]/w;
				double f2 = npix[1]/h;
				crpix[0] *= f1;
				crpix[1] *= f2;
				}
			}
		catch (NumberFormatException e)
			{
			IJ.error("Could not parse WCS preferences!");
			return;
			}
		}

	public void run (ImageProcessor ip)
		{
		getPrefs();
		wcs = new WCS(img);
		if (! wcs.hasWCS())
			{
			IJ.beep();
			if (IJ.showMessageWithCancel ("World Coordinates", "Cannot get world coordinates from FITS header!\nPress OK to repair the Image Info, CANCEL to stop."))
				wcs.repair(img,crpix,cd);
			else
				{
				img.unlock();
				return;
				}
			}

                slice = img.getCurrentSlice();
	
		// GET WCS POSITION
	
		rct = ip.getRoi();
		xy[0] = (double)(rct.x+rct.width/2);
		xy[1] = (double)(rct.y+rct.height/2);
		double[] radec = wcs.pixels2wcs (xy);	// IN DEGREES
		if (radec == null)
			{
			IJ.showMessage ("Cannot get world coordinates from FITS header!");
			IJ.beep();
			// IJ.log("Cannot get world coordinates from FITS header: pixels2wcs; "+wcs.logInfo);
			img.unlock();
			return;
			}

		// CREATE ROIS TO MARK POSITION

		PolygonRoi proi = createRoi(rct);
		proi.setImage (img);
		ocanvas.add (proi);
		String lab = " "+form.dms(radec[0]/15.0)+",";
		if (radec[1] > 0.0)
			lab += "+"+form.dms(radec[1]);
		else
			lab += form.dms(radec[1]);
		StringRoi sroi = new StringRoi ((int)xy[0]+2,(int)xy[1]+2," "+lab);
		sroi.setImage (img);
		ocanvas.add (sroi);
		ocanvas.repaint();

		// GET FILENAME

     	filename = IJU.getSliceFilename(img, slice);

		// GET RESULTS TABLE

		if (table == null)
			{
			int i=0;
			table = MeasurementTable.getTable(null);
			if (table == null)
				{
				IJ.error ("Unable to open measurment table.");
				img.unlock();
				return;
				}
			if (table.getColumnIndex(Aperture_.AP_SLICE) == ResultsTable.COLUMN_NOT_FOUND)
				i=table.getFreeColumn (Aperture_.AP_SLICE);
			if (table.getColumnIndex(Aperture_.AP_XCENTER) == ResultsTable.COLUMN_NOT_FOUND)
				i=table.getFreeColumn (Aperture_.AP_XCENTER);
			if (table.getColumnIndex(Aperture_.AP_YCENTER) == ResultsTable.COLUMN_NOT_FOUND)
				i=table.getFreeColumn (Aperture_.AP_YCENTER);
			if (table.getColumnIndex(Aperture_.AP_RA) == ResultsTable.COLUMN_NOT_FOUND)
				i=table.getFreeColumn (Aperture_.AP_RA);
			if (table.getColumnIndex(Aperture_.AP_DEC) == ResultsTable.COLUMN_NOT_FOUND)
				i=table.getFreeColumn (Aperture_.AP_DEC);
			}

		// PRINT RESULTS

		table.incrementCounter();
		table.addLabel (Aperture_.AP_IMAGE, filename);
		if (stackSize == 1)
			table.addValue (Aperture_.AP_SLICE, 0,0);
		else
			table.addValue (Aperture_.AP_SLICE, slice,0);
		table.addValue (Aperture_.AP_XCENTER, xy[0], 0);
		table.addValue (Aperture_.AP_YCENTER, xy[1], 0);
		table.addValue (Aperture_.AP_RA, radec[0]/15.0, 6);
		table.addValue (Aperture_.AP_DEC, radec[1], 6);
		table.show();
		}

	protected PolygonRoi createRoi (Rectangle r)
		{
		int xx = r.x+r.width/2;
		int yy = r.y+r.height/2;
		int[] xp = new int[12];
		int[] yp = new int[12];
		xp[0] = xx;	yp[0] = yy;
		xp[1] = xx-3;	yp[1] = yy;
		xp[2] = xx-3;	yp[2] = yy+1;
		xp[3] = xx;	yp[3] = yy+1;
		xp[4] = xx;	yp[4] = yy+4;
		xp[5] = xx+1;	yp[5] = yy+4;
		xp[6] = xx+1;	yp[6] = yy+1;
		xp[7] = xx+4;	yp[7] = yy+1;
		xp[8] = xx+4;	yp[8] = yy;
		xp[9] = xx+1;	yp[9] = yy;
		xp[10] = xx+1;	yp[10] = yy-3;
		xp[11] = xx;	yp[11] = yy-3;
		return new PolygonRoi (xp,yp,12,Roi.POLYGON);
		}
	}
