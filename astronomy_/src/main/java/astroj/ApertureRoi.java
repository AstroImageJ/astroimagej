// ApertureRoi.java

package astroj;

import ij.*;
import ij.process.*;
import ij.gui.*;
import java.awt.*;
import ij.plugin.*;
import ij.util.Tools;
import java.awt.geom.*;
import java.text.DecimalFormat;

/**
 * A ROI consisting of three concentric circles (inner for object, outer annulus for background).
 * The ROI keeps track both of it's double position and a boolean flag which determines whether
 * the backround aperture should be displayed or not.
 */
public class ApertureRoi extends Roi
	{
    protected double intCnts = Double.NaN;
	protected String intCntsText = "";
    protected String intCntsWithMagText = "";
    protected String aMagText = "";
    protected boolean intCntsBlank = true;
    protected String nameText = "";
	protected Font font = null;
	protected double xPos,yPos;
    protected double aMag = 99.99;
	protected double r1,r2,r3;
    protected boolean isCentroid = false;
    protected Color apColor;
    GFormat g21 = new GFormat("2.1");
    GFormat g23 = new GFormat("2.3");
    protected DecimalFormat upto3Places = new DecimalFormat("0.0##", IJU.dfs);
	protected boolean showAperture = true;
	protected boolean showSky = false;
    protected boolean showName = false;
	protected boolean showValues = false;
    AstroCanvas ac = null;
    AffineTransform canvTrans = null;
    boolean netFlipX=false, netFlipY=false, netRotate=false, showMag=true, showIntCntWithMag = true;

	public ApertureRoi (double x, double y, double rad1, double rad2, double rad3, double integratedCnts, boolean isCentered)
		{
		super ((int)x,(int)y,1,1);

		xPos = x;		// Roi CLASS HAS PRIVATE FLOATING POINT COORDINATES
		yPos = y;
		r1 = rad1;
		r2 = rad2;
		r3 = rad3;
		intCnts = integratedCnts;
        if (Double.isNaN(intCnts))
            {
            intCntsText = "";
            intCntsWithMagText = "";
            intCntsBlank = true;
            }
        else
            {
            intCntsText = g23.format(intCnts);
            intCntsWithMagText = g21.format(intCnts);
            intCntsBlank = false;
            }        
        isCentroid = isCentered;
		font = new Font ("SansSerif", Font.PLAIN, 16);
		}
    
    public void setSize(double rad1, double rad2, double rad3)
        {
		r1 = rad1;
		r2 = rad2;
		r3 = rad3;
        }
    
    public void setRadius(double rad1)
        {
		r1 = rad1;
        }
    public void setBack1(double rad2)
        {
		r2 = rad2;
        }
    public void setBack2(double rad3)
        {
		r3 = rad3;
        }
    
    public void move(double dx, double dy)
        {
		xPos -= dx;
		yPos -= dy;
        }
    
    public void setLocation(double x, double y)
        {
		xPos = x;
		yPos = y;
        } 
    public void setXpos(double x)
        {
		xPos = x;
        }
    public void setYpos(double y)
        {
		yPos = y;
        }

	/**
	 * Sets the appearance of the ROI when it is displayed.
	 */
	public void setAppearance (boolean aper, boolean isCentered, boolean sky, boolean name, boolean label, Color col, String ntext, Double integratedCnts)
		{
		showAperture = aper;
        isCentroid = isCentered;
		showSky = sky;
        showName = name;
		showValues = label;
        apColor = col;
        intCnts = integratedCnts;
        if (Double.isNaN(intCnts))
            {
            intCntsText = "";
            intCntsWithMagText = "";
            intCntsBlank = true;
            }
        else
            {
            intCntsText = g23.format(intCnts);
            intCntsWithMagText = g21.format(intCnts);
            intCntsBlank = false;
            }
        nameText = ntext;
		}
    public void setAMag(double mag)
        {
        aMag = mag;
        aMagText = upto3Places.format(aMag);
        }    
    public void setIntCnts(double cnts)
        {
        intCnts = cnts;
        if (Double.isNaN(intCnts))
            {
            intCntsText = "";
            intCntsWithMagText = "";
            intCntsBlank = true;
            }
        else
            {
            intCntsText = g23.format(intCnts);
            intCntsWithMagText = g21.format(intCnts);
            intCntsBlank = false;
            }        
        }    
    public void setShowName(boolean name)
        {
        showName = name;
        }
    public void setShowValues(boolean label)
        {
        showValues = label;
        }
    public void setShowSky(boolean sky)
        {
        showSky = sky;
        }
    public void setApColor(Color col)
        {
        apColor = col;
        }
    public void setIsCentroid(boolean isCentered)
        {
        isCentroid = isCentered;
        }  
    public void setName(String ntext)
        {
        nameText = ntext;
        } 
 
    public double getIntCnts()
        {
        return intCnts;
        }    
    public String getName()
        {
        return nameText;
        }
    public int getApNumber()
        {
        return IJU.parseInteger(nameText.substring(1, nameText.length()), 0) - 1;
        }    
    public boolean getIsRefStar()
        {
        return nameText.startsWith("C");
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
        return r1;
        }
    public double getBack1()
        {
        return r2;
        }
    public double getBack2()
        {
        return r3;
        }
    public boolean getShowName()
        {
        return showAperture;
        }
    public boolean getShowValues()
        {
        return showValues;
        }
    public boolean getShowSky()
        {
        return showSky;
        }
    public boolean getIsCentroid()
        {
        return isCentroid;
        }
    public Color getApColor()
        {
        return apColor;
        }
    public double getAMag()
        {
        return aMag;
        }    



	/**
	 * Lists the ROI contents.
	 */
	void log()
		{
		IJ.log("ApertureRoi: x,y="+xPos+","+yPos+", src-back="+intCnts+", r1="+r1+", r2="+r2+", r3="+r3);
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
	 * Returns the measurement value associated with the ROI.
	 */
	public double getMeasurement()
		{
		return intCnts;
		}

	/**
	 * Returns true if the screen position (xs,ys) is within the currently displayed area of the ROI
	 * (i.e. inner circle if showSky==false).
	 */
	public boolean contains (int xs, int ys)
		{
		int xx,yy,ww,hh;
		if (showSky)
			{
			xx = (int)(xPos-r3);
			ww = (int)(xPos+r3)-xx;
			yy = (int)(yPos-r3);
			hh = (int)(yPos+r3)-yy;
			}
		else	{
			xx = (int)(xPos-r1);
			ww = (int)(xPos+r1)-xx;
			yy = (int)(yPos-r1);
			hh = (int)(yPos+r1)-yy;
			}
		if (xs < xx || xs > xx+ww) return false;
		if (ys < yy || ys > yy+hh) return false;
		return true;
		}

	/**
	 * Displays the aperture either as a simple circle (showSky false) or as three circles (showSky true).
	 */
	public void draw (Graphics g)
		{
        boolean aij = false;
        if (ic instanceof astroj.AstroCanvas)
            {
            aij =  true;
            ac =(AstroCanvas)ic;
            canvTrans = ((Graphics2D)g).getTransform();
            ((Graphics2D)g).setTransform(ac.invCanvTrans);
            netFlipX = ac.getNetFlipX();
            netFlipY = ac.getNetFlipY();
            netRotate = ac.getNetRotate();
            showMag = ac.getShowAbsMag() && aMag<99.0;
            showIntCntWithMag = ac.getShowIntCntWithAbsMag();
            }
        String value = showMag? aMagText+(showIntCntWithMag && !intCntsBlank?", "+intCntsWithMagText:""):intCntsText;
		g.setColor (apColor);
		FontMetrics metrics = g.getFontMetrics (font);
		int h = metrics.getHeight();
		int w = metrics.stringWidth(value)+3;
		int descent = metrics.getDescent();
		g.setFont (font);

		int sx = screenXD (xPos);
		int sy = screenYD (yPos);
		double x1d = netFlipX ? screenXD (xPos+r1) : screenXD (xPos-r1);
        int x1 = (int)Math.round(x1d);
		double w1d = netFlipX ? screenXD (xPos-r1)-x1 : screenXD (xPos+r1)-x1;
        int w1 = (int)Math.round(w1d);
		double y1d = netFlipY ? screenYD (yPos+r1) : screenYD (yPos-r1);
        int y1 = (int)Math.round(y1d);
		double h1d = netFlipY ? screenYD (yPos-r1)-y1 : screenYD (yPos+r1)-y1;
        int h1 = (int)Math.round(h1d);
        
		int xl = sx + h;
		int yl = sy + (int)Math.round(h/3.0);

		if (showAperture)
			{
			g.drawOval(x1, y1, w1, h1);
            if (isCentroid)
                {
                int w1do4 = (int)Math.round(w1d/4.0);
                int h1do4 = (int)Math.round(h1d/4.0);
                g.drawLine(sx-w1do4, sy, sx+w1do4, sy);
                g.drawLine(sx, sy-h1do4, sx, sy+h1do4);
                
//                g.drawLine(x1, sy, (int)(x1d+w1do6), sy);
//                g.drawLine(x1+w1, sy, (int)(x1d+w1d-w1do6), sy);
//                g.drawLine(sx, y1, sx, (int)(y1d+h1d06));
//                g.drawLine(sx, y1+h1, sx, (int)(y1d+h1d-h1d06));
                
//                g.drawLine(x1, y1, (int)(x1d+w1d/4.0), (int)(y1d+h1d/4.0));
//                g.drawLine(x1+w1, y1, (int)(x1d+w1d-w1d/4.0), (int)(y1d+h1d/4.0));
//                g.drawLine(x1, y1+h1, (int)(x1d+w1d/4.0), (int)(y1d+h1d-h1d/4.0));
//                g.drawLine(x1+w1, y1+h1, (int)(x1d+w1d-w1d/4.0), (int)(y1d+h1d-h1d/4.0));                
                }
			xl = x1+w1+descent;
			}

		if (showSky)
			{
			int x2 = netFlipX ? screenXD (xPos+r2) : screenXD (xPos-r2);
			int x3 = netFlipX ? screenXD (xPos+r3) : screenXD (xPos-r3);
			int w2 = netFlipX ? screenXD (xPos-r2)-x2 : screenXD (xPos+r2)-x2;
			int w3 = netFlipX ? screenXD (xPos-r3)-x3 : screenXD (xPos+r3)-x3;
			int y2 = netFlipY ? screenYD (yPos+r2) : screenYD (yPos-r2);
			int y3 = netFlipY ? screenYD (yPos+r3) : screenYD (yPos-r3);
			int h2 = netFlipY ? screenYD (yPos-r2)-y2 : screenYD (yPos+r2)-y2;
			int h3 = netFlipY ? screenYD (yPos-r3)-y3 : screenYD (yPos+r3)-y3;
			g.drawOval (x2,y2,w2,h2);
			g.drawOval (x3,y3,w3,h3);
//            if (aij) ac.transEnabled = true;
//            int xl3 = netFlipX ? screenXD (xPos+r3) : screenXD (xPos-r3);
//            int wl3 = netFlipX ? screenXD (xPos-r3)-xl3 : screenXD (xPos+r3)-xl3;
//            if (aij) ac.transEnabled = false;
			xl = x3+w3+descent;
			}
//        xl += (int)w1do6;
//        if (aij) ((Graphics2D)g).setTransform(ac.invCanvTrans);

		if (showName && showValues && nameText != null && !nameText.equals("") && value != null && !value.equals(""))
			g.drawString (nameText+"="+value, xl,yl);
        else if(showName && nameText != null && !nameText.equals(""))
			g.drawString (nameText, xl,yl);
        else if(showValues && value != null && !value.equals(""))
			g.drawString (value, xl,yl);
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

