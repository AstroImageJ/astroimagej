package Astronomy;// Clear_Overlay.java

import java.awt.*;

import ij.*;
import ij.process.*;
import ij.gui.*;
import ij.plugin.filter.*;

import astroj.OverlayCanvas;

/**
 * Removes all the ROIs from the OverlayCanvas of the top image and puts the
 * toolbar back to a default rectangle.
 */
public class Clear_Overlay implements PlugInFilter
	{
	ImagePlus img;
	ImageCanvas canvas;
	boolean debug = false;

	public int setup (String arg, ImagePlus img)
		{
		IJ.register(Clear_Overlay.class);
		return DOES_ALL+NO_CHANGES;
		}
	
	public void run (ImageProcessor ip)	// String arg)
		{
		debug = Prefs.get("astroj.debug",debug);

		ImagePlus img = IJ.getImage();
		if (img == null) return;
		canvas = img.getCanvas();
		if (canvas instanceof OverlayCanvas)
			{
			Rectangle rct = ip.getRoi();
			int rx = rct.x+rct.width/2;
			int ry = rct.y+rct.height/2;

			OverlayCanvas ocanvas = (OverlayCanvas)canvas;
			int ix = rx;	// ocanvas.offScreenX(rx);
			int iy = ry;	// ocanvas.offScreenY(ry);
			int num = ocanvas.numberOfRois();

			if (debug)
				{
				IJ.log("number of rois = "+num);
				ocanvas.listRois();
				if (ocanvas.contains(ix,iy))
					IJ.log("contains "+ix+","+iy+" ("+rx+","+ry+")");
				else
					IJ.log("does not contain "+ix+","+iy+" ("+rx+","+ry+")");
				}

			// IF NO ROI AT THAT POSITION TO REMOVE, REMOVE ALL

			if (! ocanvas.removeRoi (ix,iy))
				{
				if (num >= 5)
					{
					// IJ.showMessage("Deletion of a large number of apertures should be via dialog window!");
					// GenericDialog gd = new GenericDialog("Clear Overlay");
					// gd.addMessage("Deleting ALL "+num+" aperture measurements in "+img.getShortTitle());
					// gd.showDialog();
					// if (!gd.wasCanceled())
					ocanvas.clearRois ();
					}
				else
					ocanvas.clearRois ();
				}
			}
		// Toolbar toolbar = Toolbar.getInstance();
		// toolbar.setTool (Toolbar.RECTANGLE);
		img.killRoi();
        if (img.getOverlay() != null) img.getOverlay().clear();
		canvas.repaint();
		}
	}
