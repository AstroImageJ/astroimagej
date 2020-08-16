// MeasurementRoi.java

package astroj;

import ij.*;
import ij.process.*;
import ij.gui.*;
import java.awt.*;
import ij.plugin.*;
import java.awt.geom.*;

/**
 * The ROI consists of a line with text label above to indicate the length and PA
 */
public class MeasurementRoi extends Roi
	{
    protected String lengthText = "";
    protected String paText = "";
    protected String delMagText = "";
    protected String fluxRatioText = "";
	protected Font font =  new Font ("SansSerif", Font.PLAIN, 16);
	protected double x1=-1000,y1=-1000,x2=-2000,y2=-2000;
    double dtr = Math.PI/180.0;
	protected double radius =10;
    protected Color roiColor = Color.MAGENTA;
    protected boolean aij = false;
    protected boolean isCentroid1 = false;
    protected boolean isCentroid2 = false;
	protected boolean showCircle = true;
    protected boolean showLength = true;
    protected boolean showPA = true;
    protected boolean showDelMag = true;
    protected boolean showFluxRatio = true;
    protected boolean showMultiLines = false;
    protected AstroCanvas ac = null;
    protected AffineTransform canvTrans = null;
    protected boolean netFlipX=false;
    protected boolean netFlipY=false;
    protected boolean netRotate=false;
    protected boolean show = true;
    protected int[] xarrowhead = new int[3];
    protected int[] yarrowhead = new int[3];
    protected double magnif = 1.0;
    protected String allText = "";
    protected boolean isliveMeasRoi = true;
    
    public MeasurementRoi (double xpos1, double ypos1)
        {
        super ((int)xpos1,(int)ypos1,1,1);
        }
    
	public MeasurementRoi (boolean isMeasRoi, boolean showROI, boolean showCir, boolean isCentered1, boolean isCentered2,
                           boolean showLengthLabel, boolean showPALabel, boolean showDelMagLabel, boolean showFluxRatioLabel,
                           double xpos1, double ypos1, double xpos2, double ypos2, double rad, String lenLabel, 
                           String paLabel, String delMagLabel, String fluxRatioLabel, Color col, Boolean showMultipleLines)
		{
		super ((int)xpos1,(int)ypos1,1,1);

        isliveMeasRoi = isMeasRoi;
        show = showROI;
        isCentroid1 = isCentered1;
        isCentroid2 = isCentered2;
        showCircle = showCir;
        showLength = showLengthLabel;
        showPA = showPALabel;
        showDelMag = showDelMagLabel;
        showFluxRatio = showFluxRatioLabel;
		x1 = xpos1;		
		y1 = ypos1;
        x2 = xpos2;		
		y2 = ypos2;        
		radius = rad;
		lengthText = lenLabel;
        paText = paLabel;
        delMagText = delMagLabel;
        fluxRatioText = fluxRatioLabel;
        roiColor = col;
        showMultiLines = showMultipleLines;
		}

	/**
	 * Change the appearance of the ROI when it is displayed.
	 */
	public void setAppearance (boolean isMeasRoi, boolean showROI, boolean showCir, boolean isCentered1, boolean isCentered2,
                               boolean showLengthLabel, boolean showPALabel, boolean showDelMagLabel, boolean showFluxRatioLabel,
                               double xpos1, double ypos1, double xpos2, double ypos2, double rad, String lenLabel, 
                               String paLabel, String delMagLabel, String fluxRatioLabel, Color col, boolean showMultipleLines)
		{
        isliveMeasRoi = isMeasRoi;
        show = showROI;
        showCircle = showCir;  
        isCentroid1 = isCentered1;
        isCentroid2 = isCentered2;
        showLength = showLengthLabel;
        showPA = showPALabel;
        showDelMag = showDelMagLabel;
        showFluxRatio = showFluxRatioLabel;
		x1 = xpos1;		
		y1 = ypos1;
        x2 = xpos2;		
		y2 = ypos2;
		radius = rad;
		lengthText = lenLabel;
        paText = paLabel;
        delMagText = delMagLabel;
        fluxRatioText = fluxRatioLabel;
        roiColor = col;
        showMultiLines = showMultipleLines;
		}
        
    public void setRadius(double rad)
        {
		radius = rad;
        } 
    public void setIsLiveMeasRoi(boolean isMeasRoi)
        {
        isliveMeasRoi = isMeasRoi;
        }
    public void setShow(boolean showROI)
        {
        show = showROI;
        }
    public void setShowLength(boolean showLengthLabel)
        {
        showLength = showLengthLabel;
        }
    public void setShowPA(boolean showPALabel)
        {
        showPA = showPALabel;
        }
    public void setShowDelMag(boolean showDelMagLabel)
        {
        showDelMag = showDelMagLabel;
        }
    public void setShowFluxRatio(boolean showFluxRatioLabel)
        {
        showFluxRatio = showFluxRatioLabel;
        }
    public void setLengthLabel(String lengthLabel)
        {
        lengthText = lengthLabel;
        }
    public void setPALabel(String paLabel)
        {
        paText = paLabel;
        }
    public void setDelMagLabel(String delMagLabel)
        {
        delMagText = delMagLabel;
        }
    public void setFluxRatioLabel(String fluxRatioLabel)
        {
        fluxRatioText = fluxRatioLabel;
        }
    public void setShowCircle(boolean cir)
        {
        showCircle = cir;
        }
    public void setShowCentroid1(boolean isCentered1)
        {
        isCentroid1 = isCentered1;
        } 
    public void setShowCentroid2(boolean isCentered2)
        {
        isCentroid2 = isCentered2;
        }
    public void setRoiColor(Color col)
        {
        roiColor = col;
        }
    public void setShowMultiLines(boolean showMultipleLines)
        {
        showMultiLines = showMultipleLines;
        }
    
    public boolean getIsLiveMeasRoi()
        {
        return isliveMeasRoi;
        }    
    public double getX1()
        {
        return x1;
        }
    public double getY1()
        {
        return y1;
        }
    public double getX2()
        {
        return x2;
        }
    public double getY2()
        {
        return y2;
        }
    public double getRadius()
        {
        return radius;
        }
    public Boolean getShowLength()
        {
        return showLength;
        }
    public Boolean getShowPA()
        {
        return showPA;
        }
    public Boolean getShowDelMag()
        {
        return showDelMag;
        }  
    public Boolean getShowFluxRatio()
        {
        return showFluxRatio;
        }
    public String getLengthLabel()
        {
        return lengthText;
        }
    public String getPALabel()
        {
        return paText;
        }
    public String getDelMagLabel()
        {
        return delMagText;
        }
    public String getFluxRatioLabel()
        {
        return fluxRatioText;
        }
    public boolean getShowCircle()
        {
        return showCircle;
        }
    public boolean getShowCentroid1()
        {
        return isCentroid1;
        } 
    public boolean getShowCentroid2()
        {
        return isCentroid2;
        }
    public Color getRioColor()
        {
        return roiColor;
        }
    public boolean getShowMultiLines()
        {
        return showMultiLines;
        } 


	/**
	 * Lists the ROI contents.
	 */
	void log()
		{
		IJ.log("MeasurementRoi: x1,y1="+x1+","+y1+";  x2,y2="+x2+","+y2+"; radius="+radius);
		}

	/**
	 * Returns an array with the double position.
	 */
	public double[] getStartCenter()
		{
		double[] d = new double[] {x1,y1};
		return d;
		}
    public double[] getEndCenter()
		{
		double[] d = new double[] {x2,y2};
		return d;
		}

	/**
	 * Returns true if the image position (x,y) is within the currently defined radius of the ROI
	 */
	public boolean contains (double x0, double y0)
		{
        if ((x1<x2&&(x0<x1-radius||x0>x2+radius)) || (x1>x2&&(x0>x1+radius||x0<x2-radius)) ||
            (y1<y2&&(y0<y1-radius||y0>y2+radius)) || (y1>y2&&(y0>y1+radius||y0<y2-radius))) return false;
        //distance of point x0,y0 to line passing through x1,y1,x2,y2
        double d = Math.abs((y2-y1)*x0 - (x2-x1)*y0 + x2*y1 - y2*x1) / Math.sqrt((y2-y1)*(y2-y1) + (x2-x1)*(x2-x1));
        if (d <= radius)
		  return true;
        else
          return false;
		}
    
	/**
	 * Displays the ROI as a line with an arrow at the final end with option circles at either end.
	 */
	public void draw (Graphics g)
		{
        if (!show) return;
        aij = (ic instanceof astroj.AstroCanvas) ? true : false;
        if (!aij) return;
        
        ac =(AstroCanvas)ic;
        canvTrans = ((Graphics2D)g).getTransform();
        ((Graphics2D)g).setTransform(ac.invCanvTrans);   
        netFlipX = ac.getNetFlipX();
        netFlipY = ac.getNetFlipY();
        netRotate = ac.getNetRotate();
        magnif = ac.getMagnification();
		g.setColor (roiColor);


		int sx1 = ac.screenXD (x1);
		int sy1 = ac.screenYD (y1);
		double x1d = (netFlipX) ? ac.screenXD (x1+radius) : ac.screenXD (x1-radius);
        int x1i = (int)x1d;
		double w1d = (netFlipX) ? ac.screenXD (x1-radius)-x1i : ac.screenXD (x1+radius)-x1i;
        int w1 = (int)w1d;
		double y1d = (netFlipY) ? ac.screenYD (y1+radius) : ac.screenYD (y1-radius);
        int y1i = (int)y1d;
		double h1d = (netFlipY) ? ac.screenYD (y1-radius)-y1i : ac.screenYD (y1+radius)-y1i;
        int h1 = (int)h1d;


        
        int sx2 = screenXD (x2);
		int sy2 = screenYD (y2);
		double x2d = (netFlipX) ? ac.screenXD (x2+radius) : ac.screenXD (x2-radius);
        int x2i = (int)x2d;
		double w2d = (netFlipX) ? ac.screenXD (x2-radius)-x2i : ac.screenXD (x2+radius)-x2i;
        int w2 = (int)w2d;
		double y2d = (netFlipY) ? ac.screenYD (y2+radius) : ac.screenYD (y2-radius);
        int y2i = (int)y2d;
		double h2d = (netFlipY) ? ac.screenYD (y2-radius)-y2i : ac.screenYD (y2+radius)-y2i;
        int h2 = (int)h2d;

        if (isCentroid1)
            {
            int w1do4 = (int)(w1d/4.0);
            int h1do4 = (int)(h1d/4.0);
            g.drawLine(sx1-w1do4, sy1, sx1+w1do4, sy1);
            g.drawLine(sx1, sy1-h1do4, sx1, sy1+h1do4); 
            }
        if (isCentroid2)
            {                   
            int w2do4 = (int)(w2d/4.0);
            int h2do4 = (int)(h2d/4.0);
            g.drawLine(sx2-w2do4, sy2, sx2+w2do4, sy2);
            g.drawLine(sx2, sy2-h2do4, sx2, sy2+h2do4);
            }  
        g.drawLine(sx1, sy1, sx2, sy2);

        double ang = Math.atan2(sy1-sy2,sx2-sx1) - Math.PI/2.0;
        double sina = Math.sin(ang);
        double cosa = Math.cos(ang);
        double alen = 12.0 * magnif;
        double awid = 6.0 * magnif;
        double lineLength = Math.sqrt((sy1-sy2)*(sy1-sy2)+(sx2-sx1)*(sx2-sx1));
        if (alen > (lineLength/6.0))
            {
            alen = (int)(lineLength/6.0);
            if (alen<3) alen = 3;
            awid = alen/2;
            if (awid<2) awid = 2;
            }
        int xc = sx2; //((sx1+sx2)/2);
        int yc = sy2; //((sy1+sy2)/2);
        xarrowhead[0] = xc;
        yarrowhead[0] = yc;
        xarrowhead[1] = xc+(int)Math.round(sina*alen-cosa*awid);
        yarrowhead[1] = yc+(int)Math.round(cosa*alen+sina*awid);
        xarrowhead[2] = xc+(int)Math.round(sina*alen+cosa*awid);
        yarrowhead[2] = yc+(int)Math.round(cosa*alen-sina*awid);
        g.drawPolygon(xarrowhead, yarrowhead, 3);
            
        if (showCircle)
			{
			g.drawOval (x1i,y1i,w1,h1);
            }
        if (showCircle)
			{
            g.drawOval (x2i,y2i,w2,h2);
            }

        FontMetrics metrics = g.getFontMetrics (font);
		int h = metrics.getHeight();
		int wLength = metrics.stringWidth(lengthText);
        int wPA = metrics.stringWidth(paText);
        int wDM = metrics.stringWidth(delMagText);
        int wFR = metrics.stringWidth(fluxRatioText);
        
        boolean sLen = showLength && lengthText != null && !lengthText.equals("");
        boolean sPA = showPA && paText != null && !paText.equals("");
        boolean sDM = showDelMag && delMagText != null && !delMagText.equals("");
        boolean sFR = showFluxRatio && fluxRatioText != null && !fluxRatioText.equals("");
                
        int descent = metrics.getDescent();
		g.setFont (font);
        int yShift = 0;
        double angle = Math.atan2(sy2-sy1, sx2-sx1);
        if (angle > Math.PI/2.0) angle -= Math.PI;
        if (angle < -Math.PI/2.0) angle += Math.PI;

        ((Graphics2D)g).translate(((sx1+sx2)/2), ((sy1+sy2)/2));
        ((Graphics2D)g).rotate(angle);

        int r = w1/2;
        allText = (sLen?lengthText+(sPA||sDM?", ":""):"")+(sPA?paText+(sDM?", ":""):"")+(sDM?delMagText+(sFR?", ":""):"")+(sFR?fluxRatioText:"");
        int wAllText = metrics.stringWidth(allText);
        if (showMultiLines && !(wAllText + (alen>r?alen+alen:r+r)<lineLength))
            {
            yShift = h-(sLen?h:0)-(sPA?h:0)-(sDM?h:0)-(sFR?h:0)-descent-1;
            if (sFR && showCircle && r>awid && (wFR+w1>=lineLength-1)) yShift -= r;
            else if (sFR && (wFR + 2*alen >= lineLength - 1)) yShift -= (int)awid;
            else if (sDM && showCircle && r>awid && (wDM+w1>=lineLength-1)) yShift -= r - (sFR?(r<h?r:h):0);
            else if (sDM && (wDM + 2*alen >= lineLength - 1)) yShift -= (int)awid - (sFR?((int)awid<h?(int)awid:h):0);
            else if (sPA && showCircle && r>awid && (wPA+w1>=lineLength-1)) yShift -= r - (sFR&&sDM?(r<h+h?r:h+h):0) - (!(sFR&&sDM)&&(sFR||sDM)?(r<h?r:h):0);
            else if (sPA && (wPA + 2*alen >= lineLength - 1)) yShift -=(int)awid - (sFR&&sDM?((int)awid<h+h?(int)awid:h+h):0) - (!(sFR&&sDM)&&(sFR||sDM)?((int)awid<h?(int)awid:h):0);
            else if (sLen && showCircle && w1/2>awid && (wLength+w1>=lineLength-1)) yShift -= r - (sFR&&sDM&&sPA?(r<h+h+h?r:h+h+h):0) - 
                                                                                             (!(sFR&&sDM&&sPA)&&((sFR&&sDM)||(sDM&&sPA)||(sPA&&sFR))?(r<h+h?r:h+h):0) -
                                                                                             (((sFR && !sDM && !sPA) || (!sFR && sDM && !sPA) || (!sFR && !sDM && sPA))?(r<h?r:h):0);
            else if (sLen && (wLength + 2*alen >= lineLength - 1)) yShift -=(int)awid - (sFR&&sDM&&sPA?((int)awid<h+h+h?(int)awid:h+h+h):0) - 
                                                                                             (!(sFR&&sDM&&sPA)&&((sFR&&sDM)||(sDM&&sPA)||(sPA&&sFR))?((int)awid<h+h?(int)awid:h+h):0) -
                                                                                             (((sFR && !sDM && !sPA) || (!sFR && sDM && !sPA) || (!sFR && !sDM && sPA))?((int)awid<h?(int)awid:h):0);           
            if (sLen)
                {
                g.drawString(lengthText, - wLength/2, yShift);
                yShift += h;
                }
            if (sPA)
                {
                g.drawString(paText, - wPA/2, yShift);
                yShift += h;
                }
            if (sDM)
                {
                g.drawString(delMagText, - wDM/2, yShift);
                yShift += h;
                }
            if (sFR)
                {
                g.drawString(fluxRatioText, - wFR/2, yShift);
                }
            }
        else if (sLen || sPA || sDM || sFR)
            {
            yShift = -descent-1;
            
            if (showCircle && r>awid && (wAllText+w1>=lineLength-1)) yShift -= r;
            else if (wAllText + 2*alen >= lineLength - 1) yShift -=(int)awid;
            g.drawString(allText, -wAllText/2, yShift);
            }

        if (aij) ((Graphics2D)g).setTransform(canvTrans);
		}
	}

