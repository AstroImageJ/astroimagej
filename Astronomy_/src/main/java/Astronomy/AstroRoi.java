package Astronomy;

import astroj.AstroCanvas;
import ij.ImagePlus;
import ij.gui.Roi;

import java.awt.*;
import java.awt.geom.AffineTransform;

public class AstroRoi extends Roi {
    public AstroRoi(int x, int y, int width, int height) {
        super(x, y, width, height);
    }

    public AstroRoi(double x, double y, double width, double height) {
        super(x, y, width, height);
    }

    public AstroRoi(int x, int y, int width, int height, int cornerDiameter) {
        super(x, y, width, height, cornerDiameter);
    }

    public AstroRoi(double x, double y, double width, double height, int cornerDiameter) {
        super(x, y, width, height, cornerDiameter);
    }

    public AstroRoi(Rectangle r) {
        super(r);
    }

    public AstroRoi(int sx, int sy, ImagePlus imp) {
        super(sx, sy, imp);
    }

    public AstroRoi(int sx, int sy, ImagePlus imp, int cornerDiameter) {
        super(sx, sy, imp, cornerDiameter);
    }

    @Override
    public void draw(Graphics g) {

        boolean aij = false;
        AffineTransform canvTrans = null;
        if (ic instanceof AstroCanvas)
        {
            aij = true;
            AstroCanvas ac = (AstroCanvas) ic;
            canvTrans = ((Graphics2D)g).getTransform();
            ((Graphics2D)g).setTransform(ac.invCanvTrans);
            boolean netFlipX = ac.getNetFlipX();
            boolean netFlipY = ac.getNetFlipY();
            boolean netRotate = ac.getNetRotate();
        }

        super.draw(g);

        if (aij) ((Graphics2D)g).setTransform(canvTrans);
    }
}
