// PixelRoi.java

package astroj;

import ij.*;
import ij.process.*;
import ij.gui.*;
import java.awt.*;
import ij.plugin.*;
import java.awt.geom.*;

/**
 * A ROI consisting of three concentric circles (inner for object, outer annulus for background).
 * The ROI keeps track both of it's double position and a boolean flag which determines whether
 * the backround aperture should be displayed or not.
 */
public class PixelRoi extends Roi
	{

	protected double xPos,yPos;
    protected Color pixColor = Color.BLACK;
    protected boolean showPixel=true;

    AstroCanvas ac = null;
    AffineTransform canvTrans = null;
    boolean netFlipX=false, netFlipY=false, netRotate=false;

	public PixelRoi (double x, double y)
		{
		super ((int)x,(int)y,1,1);

		xPos = x;		// Roi CLASS HAS PRIVATE FLOATING POINT COORDINATES
		yPos = y;
		}

	/**
	 * Sets the appearance of the ROI when it is displayed.
	 */
	public void setAppearance (Color col)
		{
        pixColor = col;
		}

    public Color getApColor()
        {
        return pixColor;
        }



	/**
	 * Lists the ROI contents.
	 */
	void log()
		{
		IJ.log("PixelRoi: x,y="+xPos+","+yPos);
		}

	/**
	 * Returns an array with the double position.
	 */
	public double[] getCenter()
		{
		double[] d = new double[] {xPos,yPos};
		return d;
		}

	/**
	 * Displays the pixel in the desired color.
	 */
	public void draw (Graphics g)
		{
        boolean aij = false;
        if (ic instanceof astroj.AstroCanvas)
            {
            aij = true;
            ac =(AstroCanvas)ic;
            canvTrans = ((Graphics2D)g).getTransform();
            ((Graphics2D)g).setTransform(ac.invCanvTrans);
            netFlipX = ac.getNetFlipX();
            netFlipY = ac.getNetFlipY();
            netRotate = ac.getNetRotate();
            }
        ImageProcessor ip = imp.getProcessor();
        if (ip.getPixelValue((int)xPos, (int)yPos) > ((ip.getMax() + ip.getMin())/2.0))
            g.setColor (Color.BLACK);
        else
            g.setColor (Color.WHITE);

		int sx = screenXD (xPos+Centroid.PIXELCENTER);
		int sy = screenYD (yPos+Centroid.PIXELCENTER);

		if (showPixel)
			{
			g.drawLine(sx, sy, sx, sy);
			}
        if (aij) ((Graphics2D)g).setTransform(canvTrans);
		}

	/**
	 * Calculates the CORRECT screen x-position of a given decimal pixel position.
	 */
//	public int fscreenX (double x) { return netFlipX ? (int)(ac.getWidth()-ac.screenXD(x+Centroid.PIXELCENTER)-1): ac.screenXD(x+Centroid.PIXELCENTER); }
//
//	/**
//	 * Calculates the CORRECT screen y-position of a given decimal pixel position.
//	 */
//	public int fscreenY (double y) { return netFlipY ? (int)(ac.getHeight()-ac.screenYD(y+Centroid.PIXELCENTER)-1): ac.screenYD(y+Centroid.PIXELCENTER); }

	}

