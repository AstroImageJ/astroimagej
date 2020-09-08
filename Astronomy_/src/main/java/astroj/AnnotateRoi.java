// ApertureRoi.java

package astroj;

import ij.*;
import ij.process.*;
import ij.gui.*;
import java.awt.*;
import ij.plugin.*;
import java.awt.geom.*;

/**
 * A ROI consisting of a circle to identify an object and an optional text label above he circle.
 */
public class AnnotateRoi extends Roi
	{
    protected String label = "";
	protected Font font =  new Font ("SansSerif", Font.PLAIN, 16);
	protected double xPos,yPos;

	protected double radius =10;
    protected Color roiColor = Color.cyan;
    protected boolean aij = false;
    protected boolean isCentroid = false;
	protected boolean showCircle = true;
    protected boolean showLabel = true;
    protected boolean isFromAstrometry = false;
    protected boolean isSourceROI = false; //always display source identification ROIs (blue circles with an empty label)
    AstroCanvas ac = null;
    AffineTransform canvTrans = null;
    boolean netFlipX=false, netFlipY=false, netRotate=false;

	public AnnotateRoi (boolean showCir, boolean isCentered, boolean showLab, boolean fromAstrometry, double x, double y, double rad, String labelText, Color col)
		{
		super ((int)x,(int)y,1,1);

        isCentroid = isCentered;
        showCircle = showCir;
        showLabel = showLab;
        isFromAstrometry = fromAstrometry;
		xPos = x;		// Roi CLASS HAS PRIVATE FLOATING POINT COORDINATES
		yPos = y;
		radius = rad;
		label = labelText.trim();
        roiColor = col;
		}
    
    public void setSize(double rad)
        {
		radius = rad;
        }    

	/**
	 * Sets the appearance of the ROI when it is displayed.
	 */
	public void setAppearance (boolean showCir, boolean isCentered, boolean showLab, boolean fromAstrometry, double x, double y, double rad, String labelText, Color col)
		{
        showCircle = showCir;  
        isCentroid = isCentered;
        showLabel = showLab;
        isFromAstrometry = fromAstrometry;
		xPos = x;		// Roi CLASS HAS PRIVATE FLOATING POINT COORDINATES
		yPos = y;
		radius = rad;
		label = labelText.trim();
        roiColor = col;
		}
    public void setIsSourceROI(boolean isSrcROI)
        {
        isSourceROI = isSrcROI;   //always show source identification ROIs (blue color with a blank label)
        }  
    public boolean getIsSourceROI()
        {
        return isSourceROI;   //always show source identification ROIs (blue color with a blank label)
        }     
    public void setShowLabel(boolean l)
        {
        showLabel = l;
        }
    public void setShowCircle(boolean cir)
        {
        showCircle = cir;
        }
    public void setCentroid(boolean isCentered)
        {
        isCentroid = isCentered;
        } 
    public void setIsFromAstrometry(boolean fromAstrometry)
        {
        isFromAstrometry = fromAstrometry;
        }     
    public void setAnnotateColor(Color col)
        {
        roiColor = col;
        }

    public double getXpos()
        {
        return xPos;
        }
    public double getYpos()
        {
        return yPos;
        }
    public double getRadius()
        {
        return radius;
        }
    public String getLabel()
        {
        return label;
        }    
    public boolean getShowLabel()
        {
        return showLabel;
        }
    public boolean getShowCircle()
        {
        return showCircle;
        }
    public boolean getShowCentroid()
        {
        return isCentroid;
        } 
    public boolean getIsFromAstrometry()
        {
        return isFromAstrometry;
        }     
    public Color getAnnotateColor()
        {
        return roiColor;
        }



	/**
	 * Lists the ROI contents.
	 */
	void log()
		{
		IJ.log("ApertureRoi: x,y="+xPos+","+yPos+", radius="+radius);
		}

	/**
	 * Returns an array with the double position.
	 */
	public double[] getCircleCenter()
		{
		double[] d = new double[] {xPos,yPos};
		return d;
		}

	/**
	 * Returns true if the screen position (xs,ys) is within the currently displayed area of the ROI
	 */
	public boolean contains (int xs, int ys)
		{
		int xx,yy,ww,hh;
        xx = (int)(xPos-radius);
        ww = (int)(xPos+radius)-xx;
        yy = (int)(yPos-radius);
        hh = (int)(yPos+radius)-yy;
		if (xs < xx || xs > xx+ww) return false;
		if (ys < yy || ys > yy+hh) return false;
		return true;
		}

	/**
	 * Displays the ROI either as a simple circle or a circle plus a label above.
	 */
	public void draw (Graphics g)
		{
        aij = false;
        if (ic instanceof astroj.AstroCanvas)
            {
            aij = true;
            ac =(AstroCanvas)ic;
            if (!ac.showAnnotations && !isSourceROI) return;
            canvTrans = ((Graphics2D)g).getTransform();
            ((Graphics2D)g).setTransform(ac.invCanvTrans);
            netFlipX = ac.getNetFlipX();
            netFlipY = ac.getNetFlipY();
            netRotate = ac.getNetRotate();
            }
		g.setColor (roiColor);
		FontMetrics metrics = g.getFontMetrics (font);
		int h = metrics.getHeight();
		int w = metrics.stringWidth(label);
		int descent = metrics.getDescent();
		g.setFont (font);

		int sx = screenXD (xPos);
		int sy = screenYD (yPos);
		double x1d = netFlipX ? screenXD (xPos+radius) : screenXD (xPos-radius);
        int x1 = (int)x1d;
		double w1d = netFlipX ? screenXD (xPos-radius)-x1 : screenXD (xPos+radius)-x1;
        int w1 = (int)w1d;
		double y1d = netFlipY ? screenYD (yPos+radius) : screenYD (yPos-radius);
        int y1 = (int)y1d;
		double h1d = netFlipY ? screenYD (yPos-radius)-y1 : screenYD (yPos+radius)-y1;
        int h1 = (int)h1d;

		int xl = sx - w/2;
		int yl = y1 - 3 - descent;

		if (showCircle)
			{
			g.drawOval (x1,y1,w1,h1);
            }
        if (isCentroid)
            {
            int w1do4 = (int)(w1d/4.0);
            int h1do4 = (int)(h1d/4.0);
            g.drawLine(sx-w1do4, sy, sx+w1do4, sy);
            g.drawLine(sx, sy-h1do4, sx, sy+h1do4);                
            }           

		if (showLabel && label != null && !label.equals(""))
            {
//            xl = x1+w1+descent;
			g.drawString (label, xl,yl);
            }

        if (aij) ((Graphics2D)g).setTransform(canvTrans);
		}

	/**
	 * Calculates the CORRECT screen x-position of a given decimal pixel position.
	 */
//	public int fscreenX (double x) { return netFlipX ? (int)(ac.getWidth()-ac.screenXD(x)-1): ac.screenXD(x); } //-((ac.getWidth())%ac.getMagnification())
//
//	/**
//	 * Calculates the CORRECT screen y-position of a given decimal pixel position.
//	 */
//	public int fscreenY (double y) { return netFlipY ? (int)(ac.getHeight()-ac.screenYD(y)-1): ac.screenYD(y); } //-(ac.getHeight()%ac.getMagnification())
    
	}

