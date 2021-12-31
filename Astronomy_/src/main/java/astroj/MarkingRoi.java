package astroj;

import ij.gui.Roi;

import java.awt.*;

public class MarkingRoi extends Roi {
    AstroCanvas ac = null;
    protected double xPos,yPos;
    protected boolean isCircle;
    boolean netFlipX=false, netFlipY=false, netRotate=false;
    
    public MarkingRoi(double x, double y, boolean isCircle) {
        super(x, y, 4, 4);
        xPos = x;
        yPos = y;
        this.isCircle = isCircle;
    }

    public void draw (Graphics g) {
        boolean aij = false;
        if (ic instanceof astroj.AstroCanvas)
        {
            aij =  true;
            ac =(AstroCanvas)ic;
            ((Graphics2D)g).setTransform(ac.invCanvTrans);
            netFlipX = ac.getNetFlipX();
            netFlipY = ac.getNetFlipY();
            netRotate = ac.getNetRotate();
        }
        g.setColor(getFillColor());

        int sx = screenXD (xPos);
        int sy = screenYD (yPos);
        double x1d = netFlipX ? screenXD (xPos+4) : screenXD (xPos-4);
        int x1 = (int)Math.round(x1d);
        double w1d = netFlipX ? screenXD (xPos-4)-x1 : screenXD (xPos+4)-x1;
        int w1 = (int)Math.round(w1d);
        double y1d = netFlipY ? screenYD (yPos+4) : screenYD (yPos-4);
        int y1 = (int)Math.round(y1d);
        double h1d = netFlipY ? screenYD (yPos-4)-y1 : screenYD (yPos+4)-y1;
        int h1 = (int)Math.round(h1d);

        if (isCircle) {
            g.drawOval(x1, y1, w1, h1);
        } else {
            int w1do4 = (int)Math.round(w1d/4.0);
            int h1do4 = (int)Math.round(h1d/4.0);
            g.drawLine(sx-w1do4, sy, sx+w1do4, sy);
            g.drawLine(sx, sy-h1do4, sx, sy+h1do4);
        }
        if (aij) ((Graphics2D)g).setTransform(ac.canvTrans);
    }
}
