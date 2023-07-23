package Astronomy.multiplot;

import ij.gui.*;

import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.geom.Line2D;

public class PlotDraggableShape {
    private double startX;
    private double startY;
    private double endX;
    private double endY;

    public void drawRectangle(ImageCanvas canvas) {
        var x0 = Math.min(startX, endX);
        var y0 = Math.min(startY, endY);
        var xw = Math.abs(endX - startX);
        var xh = Math.abs(endY - startY);

        var roi = new Roi(x0, y0, xw, xh);
        roi.setName("selectionRoi");
        canvas.setOverlay(new Overlay(roi));
    }

    public void drawLine(ImageCanvas canvas) {
        var roi = new ShapeRoi(new Line2D.Double(startX, startY, endX, endY));
        roi.setName("selectionRoi");
        roi.setStrokeColor(Color.RED);
        roi.setStrokeWidth(2);
        canvas.setOverlay(new Overlay(roi));
    }

    public void setStart(ImageCanvas canvas, MouseEvent e) {
        startX = canvas.offScreenXD(e.getX());
        startY = canvas.offScreenYD(e.getY());
    }

    public void setEnd(ImageCanvas canvas, MouseEvent e) {
        endX = canvas.offScreenXD(e.getX());
        endY = canvas.offScreenYD(e.getY());
    }

    public void removeSelection(ImageCanvas canvas) {
        if (canvas.getOverlay() != null) {
            canvas.getOverlay().remove("selectionRoi");
        }
    }


    /**
     * @see Plot#descaleX(int)
     */
    public double getStartX(Plot plot) {
        if (plot.xMin == plot.xMax) return plot.xMin;
        double xv = (startX-plot.xBasePxl)/plot.xScale + plot.xMin;
        if (plot.logXAxis) xv = Math.pow(10, xv);
        return xv;
    }

    /**
     * @see Plot#descaleY(int)
     */
    public double getStartY(Plot plot) {
        if (plot.yMin == plot.yMax) return plot.yMin;
        double yv = (plot.yBasePxl-startY)/plot.yScale + plot.yMin;
        if (plot.logYAxis) yv = Math.pow(10, yv);
        return yv;
    }

    /**
     * @see Plot#descaleX(int)
     */
    public double getEndX(Plot plot) {
        if (plot.xMin == plot.xMax) return plot.xMin;
        double xv = (endX-plot.xBasePxl)/plot.xScale + plot.xMin;
        if (plot.logXAxis) xv = Math.pow(10, xv);
        return xv;
    }

    /**
     * @see Plot#descaleY(int)
     */
    public double getEndY(Plot plot) {
        if (plot.yMin == plot.yMax) return plot.yMin;
        double yv = (plot.yBasePxl-endY)/plot.yScale + plot.yMin;
        if (plot.logYAxis) yv = Math.pow(10, yv);
        return yv;
    }

    public double getDX(Plot plot) {
        return getEndX(plot) - getStartX(plot);
    }

    public double getDY(Plot plot) {
        return getEndY(plot) - getStartY(plot);
    }

    public void updatePlotBounds(Plot plot) {
        plot.setLimits(Math.min(getStartX(plot), getEndX(plot)), Math.max(getStartX(plot), getEndX(plot)),
                Math.min(getStartY(plot), getEndY(plot)), Math.max(getStartY(plot), getEndY(plot)));
    }
}
