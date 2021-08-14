package ij.astro.util;

import de.rototor.pdfbox.graphics2d.PdfBoxGraphics2D;
import ij.IJ;
import ij.WindowManager;
import ij.astro.accessors.IPlotObject;
import ij.astro.accessors.IPlotProperties;
import ij.gui.Plot;
import ij.io.SaveDialog;
import ij.process.ImageProcessor;
import ij.util.Java2;
import ij.util.Tools;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.graphics.form.PDFormXObject;
import org.apache.pdfbox.util.Matrix;

import java.awt.*;
import java.awt.font.GlyphVector;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Rectangle2D;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Vector;

import static ij.gui.Plot.*;
import static ij.process.ImageProcessor.CENTER_JUSTIFY;
import static ij.process.ImageProcessor.RIGHT_JUSTIFY;

/**
 * Save a plot as a vector PDF. Drawing methods are adapted from {@link Plot}.
 */
public class PdfPlotOutput {
    private final Vector<IPlotObject> allPlotObjects;
    private final Plot plot;
    private final double frameWidth;
    private final double frameHeight;
    private final double leftMargin;
    private final double topMargin;
    private int lineWidth = 1;
    private int cx, cy;
    private int justification;

    private PdfPlotOutput(Plot plot) {
        var allPlotObjects = (Vector<?>) plot.allPlotObjects;
        this.allPlotObjects = new Vector<>();
        this.plot = plot;

        allPlotObjects.forEach(plotObject -> {
            this.allPlotObjects.add((IPlotObject) plotObject);
        });

        var margins = plot.getMargins();

        frameHeight = margins.getHeight();
        frameWidth = margins.getWidth();
        topMargin = margins.getY();
        leftMargin = margins.getX();
    }

    public static void savePlot(Plot plot) {
        savePlot(plot, "");
    }

    public static void savePlot(Plot plot, String path) {
        var imp = WindowManager.getCurrentImage();
        if (imp==null) {
            IJ.noImage();
            return;
        }
        if (path.equals("")) {
            SaveDialog sd = new SaveDialog("Save as PDF...", imp.getTitle(), ".pdf");
            String name = sd.getFileName();
            if (name==null)
                return;
            String dir = sd.getDirectory();
            path = dir + name;
        }
        try {
            new PdfPlotOutput(plot).writePdf(path);
        } catch (IOException e) {
            String msg = e.getMessage();
            if (msg==null || msg.equals(""))
                msg = ""+e;
            msg = "An error occurred writing the file.\n \n" + msg;
            if (msg.contains("NullPointerException"))
                msg = "Incorrect file path:";
            msg += "\n \n"+path;
            IJ.error("Vector PDF Writer", msg);
        }
        IJ.showStatus("");
    }

    private void writePdf(String path) throws IOException {
        PDDocument document = new PDDocument();
        PDPage page = new PDPage(new PDRectangle(plot.getSize().width, plot.getSize().height));
        document.addPage(page);

        /*
         * Creates the Graphics and sets a size in pixel. This size is used for the BBox of the XForm.
         * So everything drawn outside (0x0)-(width,height) will be clipped.
         */
        PdfBoxGraphics2D pdfBoxGraphics2D = new PdfBoxGraphics2D(document, plot.getSize().width, plot.getSize().height);

        /*
         * Now do your drawing. By default, all texts are rendered as vector shapes
         */
        initPlotDrawing(pdfBoxGraphics2D);
        for (IPlotObject plotObject : allPlotObjects) {
            //todo mimic order of drawing, see drawContents
            //  gonna ignore for now as this works
            drawPlotObject(pdfBoxGraphics2D, plotObject);
        }

        /*
         * Dispose when finished
         */
        pdfBoxGraphics2D.dispose();

        /*
         * After dispose() of the graphics object we can get the XForm.
         */
        PDFormXObject xform = pdfBoxGraphics2D.getXFormObject();
        xform.setBBox(new PDRectangle(plot.getSize().width, plot.getSize().height));

        /*
         * Build a matrix to place the form
         */
        Matrix matrix = new Matrix();
        /*
         *  Note: As PDF coordinates start in the bottom left corner, we move up from there.
         */
        PDPageContentStream contentStream = new PDPageContentStream(document, page);

        contentStream.transform(matrix);

        //todo shape reduction?

        contentStream.drawForm(xform);

        contentStream.close();
        document.save(new File(path));
        document.close();
    }

    private void initPlotDrawing(Graphics2D g) {
        g.setColor(Color.BLACK);
        drawAxesTicksGridNumbers(g, plot.steps);
        var pp = (IPlotProperties) plot.pp;
        var stroke = new BasicStroke(plot.sc(pp.getFrame().getLineWidth()));
        g.setStroke(stroke);
        g.setColor(pp.getFrame().getColor());
        // Plot border
        g.drawRect(plot.frame.x, plot.frame.y, plot.frame.width - 1, plot.frame.height - 1);
        drawPlotObject(g, pp.getLegend());
    }

    void drawAxesTicksGridNumbers(Graphics2D g, double[] steps) {
        var pp = (IPlotProperties) plot.pp;
        String[] xCats = plot.labelsInBraces('x');   // create categories for the axes (if any)
        String[] yCats = plot.labelsInBraces('y');
        String multiplySymbol = plot.getMultiplySymbol(); // for scientific notation
        Font scFont = plot.scFont(pp.getFrame().getFont());
        Font scFontMedium = scFont.deriveFont(scFont.getSize2D() * 10f / 12f); //for axis numbers if full size does not fit
        Font scFontSmall = scFont.deriveFont(scFont.getSize2D() * 9f / 12f);   //for subscripts
        g.setFont(scFont.deriveFont(12f));
        FontMetrics fm = g.getFontMetrics();
        int fontAscent = fm.getAscent();
        setJustification(LEFT);
        // ---	A l o n g	X	A x i s
        double yOfXAxisNumbers = topMargin + frameHeight + fm.getHeight() * 5 / 4f + plot.sc(2);
        if (plot.hasFlag(X_NUMBERS | (plot.logXAxis ? (X_TICKS | X_MINOR_TICKS) : X_LOG_TICKS) + X_GRID)) {
            Font baseFont = scFont;
            boolean majorTicks = plot.logXAxis ? plot.hasFlag(X_LOG_TICKS) : plot.hasFlag(X_TICKS);
            boolean minorTicks = plot.hasFlag(X_MINOR_TICKS);
            minorTicks = minorTicks && (xCats == null);
            double step = steps[0];
            int i1 = (int) Math.ceil(Math.min(plot.xMin, plot.xMax) / step - 1.e-10);
            int i2 = (int) Math.floor(Math.max(plot.xMin, plot.xMax) / step + 1.e-10);
            int suggestedDigits = (int) Tools.getNumberFromList(pp.getFrame().getOptions(), "xdecimals="); //is not given, NaN cast to 0
            int digits = getDigits(plot.xMin, plot.xMax, step, 7, suggestedDigits);
            double y1 = topMargin;
            double y2 = topMargin + frameHeight;
            if (plot.xMin == plot.xMax) {
                if (plot.hasFlag(X_NUMBERS)) {
                    String s = IJ.d2s(plot.xMin, getDigits(plot.xMin, 0.001 * plot.xMin, 5, suggestedDigits));
                    int y = plot.yBasePxl;
                    drawString(g, s, plot.xBasePxl - getStringWidth(g, s) / 2, (int) yOfXAxisNumbers);
                }
            } else {
                if (plot.hasFlag(X_NUMBERS)) {
                    int w1 = getStringWidth(g, IJ.d2s(plot.currentMinMax[0], plot.logXAxis ? -1 : digits));
                    int w2 = getStringWidth(g, IJ.d2s(plot.currentMinMax[1], plot.logXAxis ? -1 : digits));
                    int wMax = Math.max(w1, w2);
                    if (wMax > Math.abs(step * plot.xScale) - plot.sc(8)) {
                        baseFont = scFontMedium;   //small font if there is not enough space for the numbers
                        g.setFont(baseFont);
                    }
                }

                for (int i = 0; i <= (i2 - i1); i++) {
                    double v = (i + i1) * step;
                    double x = Math.round((v - plot.xMin) * plot.xScale) + leftMargin;

                    if (xCats != null) {
                        int index = (int) v;
                        double remainder = Math.abs(v - Math.round(v));
                        if (index >= 0 && index < xCats.length && remainder < 1e-9) {
                            String s = xCats[index];
                            String[] parts = s.split("\n");
                            int w = 0;
                            for (int jj = 0; jj < parts.length; jj++)
                                w = Math.max(w, getStringWidth(g, parts[jj]));


                            drawString(g, s, (int) (x - w / 2f), (int) yOfXAxisNumbers);
                        }
                        continue;
                    }

                    if (plot.hasFlag(X_GRID)) {
                        g.setColor(plot.gridColor);
                        g.draw(new Line2D.Double(x, y1, x, y2));
                        g.setColor(Color.black);
                    }
                    if (majorTicks) {
                        g.draw(new Line2D.Double(x, y1, x, y1 + plot.sc(plot.tickLength)));
                        g.draw(new Line2D.Double(x, y2, x, y2 - plot.sc(plot.tickLength)));
                    }
                    if (plot.hasFlag(X_NUMBERS)) {
                        if (plot.logXAxis || digits < 0) {
                            drawExpString(g, plot.logXAxis ? Math.pow(10, v) : v, plot.logXAxis ? -1 : -digits,
                                    (int) x, (int) yOfXAxisNumbers - fontAscent / 2, CENTER, fontAscent, baseFont, scFontSmall, multiplySymbol);
                        } else {
                            String s = IJ.d2s(v, digits);
                            drawString(g, s, (int) x - getStringWidth(g, s) / 2, (int) yOfXAxisNumbers);
                        }
                    }
                }
                boolean haveMinorLogNumbers = i2 - i1 < 2;        //nunbers on log minor ticks only if < 2 decades
                if (minorTicks && (!plot.logXAxis || step > 1.1)) {  //'standard' log minor ticks only for full decades
                    double mstep = plot.niceNumber(step * 0.19);       //non-log: 4 or 5 minor ticks per major tick
                    double minorPerMajor = step / mstep;
                    if (Math.abs(minorPerMajor - Math.round(minorPerMajor)) > 1e-10) //major steps are not an integer multiple of minor steps? (e.g. user step 90 deg)
                        mstep = step / 4;
                    if (plot.logXAxis && mstep < 1) mstep = 1;
                    i1 = (int) Math.ceil(Math.min(plot.xMin, plot.xMax) / mstep - 1.e-10);
                    i2 = (int) Math.floor(Math.max(plot.xMin, plot.xMax) / mstep + 1.e-10);
                    for (int i = i1; i <= i2; i++) {
                        double v = i * mstep;
                        double x = Math.round((v - plot.xMin) * plot.xScale) + leftMargin;
                        g.draw(new Line2D.Double(x, y1, x, y1 + plot.sc(plot.minorTickLength)));
                        g.draw(new Line2D.Double(x, y2, x, y2 - plot.sc(plot.minorTickLength)));
                    }
                } else if (plot.logXAxis && majorTicks && Math.abs(plot.xScale) > plot.sc(MIN_X_GRIDSPACING)) {        //minor ticks for log
                    int minorNumberLimit = haveMinorLogNumbers ? (int) (0.12 * Math.abs(plot.xScale) / (fm.charWidth('0') + plot.sc(2))) : 0;   //more numbers on minor ticks when zoomed in
                    i1 = (int) Math.floor(Math.min(plot.xMin, plot.xMax) - 1.e-10);
                    i2 = (int) Math.ceil(Math.max(plot.xMin, plot.xMax) + 1.e-10);
                    for (int i = i1; i <= i2; i++) {
                        for (int m = 2; m < 10; m++) {
                            double v = i + Math.log10(m);
                            if (v > Math.min(plot.xMin, plot.xMax) && v < Math.max(plot.xMin, plot.xMax)) {
                                double x = Math.round((v - plot.xMin) * plot.xScale) + leftMargin;
                                g.draw(new Line2D.Double(x, y1, x, y1 + plot.sc(plot.minorTickLength)));
                                g.draw(new Line2D.Double(x, y2, x, y2 - plot.sc(plot.minorTickLength)));
                                if (m <= minorNumberLimit)
                                    drawExpString(g, Math.pow(10, v), 0, (int) x, (int) yOfXAxisNumbers - fontAscent / 2, CENTER,
                                            fontAscent, baseFont, scFontSmall, multiplySymbol);
                            }
                        }
                    }
                }
            }
        }
        // ---	A l o n g	Y	A x i s
        g.setFont(scFont.deriveFont(12f));
        int maxNumWidth = 0;
        double xNumberRight = leftMargin - plot.sc(2) - getStringWidth(g, "0") / 2f;
        Rectangle rect = getStringBounds(g, "0169");
        int yNumberOffset = -rect.y - rect.height / 2;
        if (plot.hasFlag(Y_NUMBERS | (plot.logYAxis ? (Y_TICKS | Y_MINOR_TICKS) : Y_LOG_TICKS) + Y_GRID)) {
            setJustification(RIGHT);
            Font baseFont = scFont;
            boolean majorTicks = plot.logYAxis ? plot.hasFlag(Y_LOG_TICKS) : plot.hasFlag(Y_TICKS);
            boolean minorTicks = plot.logYAxis ? plot.hasFlag(Y_LOG_TICKS) : plot.hasFlag(Y_MINOR_TICKS);
            minorTicks = minorTicks && (yCats == null);
            double step = steps[1];
            int i1 = (int) Math.ceil(Math.min(plot.yMin, plot.yMax) / step - 1.e-10);
            int i2 = (int) Math.floor(Math.max(plot.yMin, plot.yMax) / step + 1.e-10);
            int suggestedDigits = (int) Tools.getNumberFromList(pp.getFrame().getOptions(), "ydecimals="); //is not given, NaN cast to 0
            int digits = getDigits(plot.yMin, plot.yMax, step, 5, suggestedDigits);
            double x1 = leftMargin;
            double x2 = leftMargin + frameWidth;
            if (plot.yMin == plot.yMax) {
                if (plot.hasFlag(Y_NUMBERS)) {
                    String s = IJ.d2s(plot.yMin, getDigits(plot.yMin, 0.001 * plot.yMin, 5, suggestedDigits));
                    maxNumWidth = getStringWidth(g, s);
                    int y = plot.yBasePxl;
                    drawString(g, s, (int) xNumberRight, y + fontAscent / 2 + plot.sc(1));
                }
            } else {
                int digitsForWidth = plot.logYAxis ? -1 : digits;
                if (digitsForWidth < 0) {
                    digitsForWidth--; //"1.0*10^5" etc. needs more space than 1.0*5, simulate by adding one decimal
                    xNumberRight += plot.sc(1) + getStringWidth(g, "0") / 4f;
                }
                String str1 = IJ.d2s(plot.currentMinMax[2], digitsForWidth);
                String str2 = IJ.d2s(plot.currentMinMax[3], digitsForWidth);
                if (digitsForWidth < 0) {
                    str1 = str1.replaceFirst("E", multiplySymbol);
                    str2 = str2.replaceFirst("E", multiplySymbol);
                }
                int w1 = getStringWidth(g, str1);
                int w2 = getStringWidth(g, str2);
                int wMax = Math.max(w1, w2);
                if (plot.hasFlag(Y_NUMBERS)) {
                    if (wMax > xNumberRight - plot.sc(4) - (pp.getyLabel().getLabel().length() > 0 ? fm.getHeight() : 0)) {
                        baseFont = scFontMedium;   //small font if there is not enough space for the numbers
                        g.setFont(baseFont);
                    }
                }
                for (int i = i1; i <= i2; i++) {
                    double v = step == 0 ? plot.yMin : i * step;
                    double y = topMargin + frameHeight - (int) Math.round((v - plot.yMin) * plot.yScale);

                    if (yCats != null) {
                        int index = (int) v;
                        double remainder = Math.abs(v - Math.round(v));
                        if (index >= 0 && index < yCats.length && remainder < 1e-9) {
                            String s = yCats[index];
                            int multiLineOffset = 0; // multi-line cat labels
                            for (int jj = 0; jj < s.length(); jj++)
                                if (s.charAt(jj) == '\n')
                                    multiLineOffset -= rect.height / 2;

                            drawString(g, s, (int) xNumberRight, (int) y + yNumberOffset + multiLineOffset);
                        }
                        continue;
                    }

                    if (plot.hasFlag(Y_GRID)) {
                        g.setColor(plot.gridColor);
                        g.draw(new Line2D.Double(x1, y, x2, y));
                        g.setColor(Color.black);
                    }
                    if (majorTicks) {
                        g.draw(new Line2D.Double(x1, y, x1 + plot.sc(plot.tickLength), y));
                        g.draw(new Line2D.Double(x2, y, x2 - plot.sc(plot.tickLength), y));
                    }
                    if (plot.hasFlag(Y_NUMBERS)) {
                        int w = 0;
                        if (plot.logYAxis || digits < 0) {
                            w = drawExpString(g, plot.logYAxis ? Math.pow(10, v) : v, plot.logYAxis ? -1 : -digits,
                                    (int) xNumberRight, (int) y, RIGHT, fontAscent, baseFont, scFontSmall, multiplySymbol);
                        } else {
                            String s = IJ.d2s(v, digits);
                            w = getStringWidth(g, s);
                            drawString(g, s, (int) xNumberRight, (int) y + yNumberOffset);
                        }
                        if (w > maxNumWidth) maxNumWidth = w;
                    }
                }
                boolean haveMinorLogNumbers = i2 - i1 < 2;        //numbers on log minor ticks only if < 2 decades
                if (minorTicks && (!plot.logYAxis || step > 1.1)) {  //'standard' log minor ticks only for full decades
                    double mstep = plot.niceNumber(step * 0.19);       //non-log: 4 or 5 minor ticks per major tick
                    double minorPerMajor = step / mstep;
                    if (Math.abs(minorPerMajor - Math.round(minorPerMajor)) > 1e-10) //major steps are not an integer multiple of minor steps? (e.g. user step 90 deg)
                        mstep = step / 4;
                    if (plot.logYAxis && step < 1) mstep = 1;
                    i1 = (int) Math.ceil(Math.min(plot.yMin, plot.yMax) / mstep - 1.e-10);
                    i2 = (int) Math.floor(Math.max(plot.yMin, plot.yMax) / mstep + 1.e-10);
                    for (int i = i1; i <= i2; i++) {
                        double v = i * mstep;
                        double y = topMargin + frameHeight - (int) Math.round((v - plot.yMin) * plot.yScale);
                        g.draw(new Line2D.Double(x1, y, x1 + plot.sc(plot.minorTickLength), y));
                        g.draw(new Line2D.Double(x2, y, x2 - plot.sc(plot.minorTickLength), y));
                    }
                }
                if (plot.logYAxis && majorTicks && Math.abs(plot.yScale) > plot.sc(MIN_X_GRIDSPACING)) {         //minor ticks for log within the decade
                    int minorNumberLimit = haveMinorLogNumbers ? (int) (0.4 * Math.abs(plot.yScale) / fm.getHeight()) : 0;    //more numbers on minor ticks when zoomed in
                    i1 = (int) Math.floor(Math.min(plot.yMin, plot.yMax) - 1.e-10);
                    i2 = (int) Math.ceil(Math.max(plot.yMin, plot.yMax) + 1.e-10);
                    for (int i = i1; i <= i2; i++) {
                        for (int m = 2; m < 10; m++) {
                            double v = i + Math.log10(m);
                            if (v > Math.min(plot.yMin, plot.yMax) && v < Math.max(plot.yMin, plot.yMax)) {
                                double y = topMargin + frameHeight - (int) Math.round((v - plot.yMin) * plot.yScale);
                                g.draw(new Line2D.Double(x1, y, x1 + plot.sc(plot.minorTickLength), y));
                                g.draw(new Line2D.Double(x2, y, x2 - plot.sc(plot.minorTickLength), y));
                                if (m <= minorNumberLimit) {
                                    int w = drawExpString(g, Math.pow(10, v), 0, (int) xNumberRight, (int) y, RIGHT,
                                            fontAscent, baseFont, scFontSmall, multiplySymbol);
                                    if (w > maxNumWidth) maxNumWidth = w;
                                }
                            }
                        }
                    }
                }
            }
        }
        // --- Write min&max of range if simple style without any axis format flags
        g.setFont(scFont);
        setJustification(LEFT);
        String xLabelToDraw = pp.getxLabel().getLabel();
        String yLabelToDraw = pp.getyLabel().getLabel();
        if (plot.simpleYAxis()) { // y-axis min&max
            int digits = getDigits(plot.yMin, plot.yMax, 0.001 * (plot.yMax - plot.yMin), 6, 0);
            String s = IJ.d2s(plot.yMax, digits);
            int sw = getStringWidth(g, s);
            if ((sw + plot.sc(4)) > leftMargin)
                drawString(g, s, plot.sc(4), (int) topMargin - plot.sc(4));
            else
                drawString(g, s, (int) leftMargin - getStringWidth(g, s) - plot.sc(4), (int) topMargin + 10);
            s = IJ.d2s(plot.yMin, digits);
            sw = getStringWidth(g, s);
            if ((sw + 4) > leftMargin)
                drawString(g, s, plot.sc(4), (int) topMargin + plot.frame.height);
            else
                drawString(g, s, (int) leftMargin - getStringWidth(g, s) - plot.sc(4), (int) topMargin + plot.frame.height);
            if (plot.logYAxis) yLabelToDraw += " (LOG)";
        }
        double y = yOfXAxisNumbers;
        if (plot.simpleXAxis()) { // x-axis min&max
            int digits = getDigits(plot.xMin, plot.xMax, 0.001 * (plot.xMax - plot.xMin), 7, 0);
            drawString(g, IJ.d2s(plot.xMin, digits), (int) leftMargin, (int) y);
            String s = IJ.d2s(plot.xMax, digits);
            drawString(g, s, (int) leftMargin + plot.frame.width - getStringWidth(g, s) + 6, (int) y);
            y -= fm.getHeight();
            if (plot.logXAxis) xLabelToDraw += " (LOG)";
        } else
            y += plot.sc(1);
        // --- Write x and y axis text labels
        if (xCats == null) {
            g.setFont(pp.getxLabel().getFont() == null ? scFont.deriveFont(12f) : plot.scFont(pp.getxLabel().getFont()).deriveFont(12f));
            int xpos = (int) (leftMargin + (plot.frame.width - getStringWidth(g, xLabelToDraw)) / 2);
            int ypos = (int) (y + getStringBounds(g, xLabelToDraw).height);
            g.drawString(xLabelToDraw, xpos, ypos); //todo add sub/superscript drawing like ylabel
        }
        if (yCats == null) {
            g.setFont(pp.getxLabel().getFont() == null ? scFont.deriveFont(12f) : plot.scFont(pp.getxLabel().getFont()).deriveFont(12f));
            int xRightOfYLabel = (int) (xNumberRight - maxNumWidth - plot.sc(2));
            int xpos = xRightOfYLabel - getStringWidth(g, yLabelToDraw) - plot.sc(2);
            int ypos = (int) (topMargin + (plot.frame.height - g.getFontMetrics().getHeight()) / 2);
            var o = g.getTransform();

            // todo not a perfect match, but close enough - a bit too high
            g.rotate(-Math.PI / 2f);
            stringToPixels(g, yLabelToDraw, -ypos - getStringWidth(g, yLabelToDraw) / 2, 0);
            g.setTransform(o);
        }
    }

    void stringToPixels(Graphics2D g, String labelStr, int x, int y) {
        Font bigFont = g.getFont();
        Rectangle rect = getStringBounds(g, labelStr);
        int ww = rect.width * 2;
        int hh = rect.height * 3;//enough space, will be cropped later
        int y0 = rect.height * 2;//base line
        g.setColor(Color.BLACK);

        FontMetrics fm = g.getFontMetrics();
        int ascent = fm.getAscent();
        int offSub = ascent / 6;
        int offSuper = -ascent / 2;
        Font smallFont = bigFont.deriveFont((float) (bigFont.getSize() * 0.7));

        Rectangle bigBounds = getStringBounds(g, labelStr);
        boolean doParse = (labelStr.contains("^^") || labelStr.contains("!!"));
        doParse = doParse && (!labelStr.contains("^^^") && !labelStr.contains("!!!"));
        if (!doParse) {
            g.drawString(labelStr, x, y0 + y);
            Rectangle cropRect = new Rectangle(bigBounds);
            cropRect.y += y0;
            return;
        }

        if (labelStr.endsWith("^^") || labelStr.endsWith("!!")) {
            labelStr = labelStr.substring(0, labelStr.length() - 2);
        }
        if (labelStr.startsWith("^^") || labelStr.startsWith("!!")) {
            labelStr = " " + labelStr;
        }

        g.setFont(smallFont);
        Rectangle smallBounds = getStringBounds(g, labelStr);
        g.setFont(bigFont);
        int upperBound = y0 + smallBounds.y + offSuper;
        int lowerBound = y0 + smallBounds.y + smallBounds.height + offSub;

        int h = fm.getHeight();
        int len = labelStr.length();
        int[] tags = new int[len];
        int nTags = 0;

        for (int jj = 0; jj < len - 2; jj++) {//get positions where font size changes
            if (labelStr.startsWith("^^", jj)) {
                tags[nTags++] = jj;
            }
            if (labelStr.startsWith("!!", jj)) {
                tags[nTags++] = -jj;
            }
        }
        tags[nTags++] = len;
        tags = Arrays.copyOf(tags, nTags);

        int leftIndex = 0;
        int xRight = 0;
        int y2 = y0;

        boolean subscript = labelStr.startsWith("!!");
        for (int pp = 0; pp < tags.length; pp++) {//draw all text fragments
            int rightIndex = tags[pp];
            rightIndex = Math.abs(rightIndex);
            String part = labelStr.substring(leftIndex, rightIndex);
            boolean small = pp % 2 == 1;//toggle odd/even
            if (small) {
                g.setFont(smallFont);
                if (subscript) {
                    y2 = y0 + offSub;
                } else {//superscript:
                    y2 = y0 + offSuper;
                }
            } else {
                g.setFont(bigFont);
                y2 = y0;
            }
            xRight++;
            int partWidth = getStringWidth(g, part);
            g.drawString(part, xRight + x, y2 + y);
            leftIndex = rightIndex + 2;
            subscript = tags[pp] < 0;//negative positions = subscript
            xRight += partWidth;
        }
    }


    /**
     * draw something like 1.2 10^-9; returns the width of the string drawn.
     * 'Digits' should be >=0 for drawing the mantissa (=1.38 in this example), negative to draw only 10^exponent
     * Currently only supports center justification and right justification (y of center line)
     * Fonts baseFont, smallFont should be scaled already
     * Returns the width of the String
     */
    int drawExpString(Graphics2D g, double value, int digits, int x, int y, int justification,
                      int fontAscent, Font baseFont, Font smallFont, String multiplySymbol) {
        String base = "10";
        String exponent = null;
        String s = IJ.d2s(value, digits <= 0 ? -1 : -digits);
        if (Tools.parseDouble(s) == 0) s = "0"; //don't write 0 as 0*10^0
        int ePos = s.indexOf('E');
        if (ePos < 0)
            base = s;    //can't have exponential format, e.g. NaN
        else {
            if (digits >= 0) {
                base = s.substring(0, ePos);
                if (digits == 0)
                    base = Integer.toString((int) Math.round(Tools.parseDouble(base)));
                base += multiplySymbol + "10";
            }
            exponent = s.substring(ePos + 1);
        }
        //IJ.log(s+" -> "+base+"^"+exponent+"  maxAsc="+fontAscent+" font="+baseFont);
        setJustification(RIGHT);
        int width = getStringWidth(g, base);
        if (exponent != null) {
            g.setFont(smallFont);
            int wExponent = getStringWidth(g, exponent);
            width += wExponent;
            if (justification == CENTER) x += width / 2;
            drawString(g, exponent, x, y + fontAscent * 3 / 10);
            x -= wExponent;
            g.setFont(baseFont);
        }
        drawString(g, base, x, y + fontAscent * 7 / 10);
        return width;
    }


    /**
     * Sets the justification used by drawString(), where <code>justification</code>
     * is CENTER_JUSTIFY, RIGHT_JUSTIFY or LEFT_JUSTIFY. The default is LEFT_JUSTIFY.
     */
    public void setJustification(int justification) {
        if (justification < ImageProcessor.LEFT_JUSTIFY || justification > RIGHT_JUSTIFY)
            justification = ImageProcessor.LEFT_JUSTIFY;
        this.justification = justification;
    }

    //todo other objects
    // see https://github.com/rototor/pdfbox-graphics2d
    private void drawPlotObject(Graphics2D g, IPlotObject plotObject) {
        if (plotObject == null) return;
        if (plotObject.hasFlag(IPlotObject.HIDDEN)) return;
        g.setColor(plotObject.getColor());
        var stroke = new BasicStroke(plot.sc(plotObject.getLineWidth()));
        g.setStroke(stroke);
        setLineWidth(plot.sc(plotObject.getLineWidth()));
        final var type = plotObject.getType();
        switch (type) {
            case IPlotObject.XY_DATA:
                g.setClip(plot.frame);
                int nPoints = Math.min(plotObject.getxValues().length, plotObject.getyValues().length);

                if (plotObject.getShape() == BAR || plotObject.getShape() == SEPARATED_BAR)
                    //drawBarChart(plotObject);       // (separated) bars
                    //todo implement

                    if (plotObject.getShape() == FILLED) {   // filling below line
                        g.setColor(plotObject.getColor2() != null ? plotObject.getColor2() : plotObject.getColor());
                        drawFloatPolyLineFilled(g, plotObject.getxValues(), plotObject.getyValues(), nPoints);
                    }
                g.setColor(plotObject.getColor());
                g.setStroke(new BasicStroke(plot.sc(plotObject.getLineWidth())));

                if (plotObject.getyEValues() != null)    // error bars in front of bars and fill area below the line, but behind lines and marker symbols
                    drawVerticalErrorBars(g, plotObject.getxValues(), plotObject.getyValues(), plotObject.getyEValues());
                if (plotObject.getxEValues() != null)
                    drawHorizontalErrorBars(g, plotObject.getxValues(), plotObject.getyValues(), plotObject.getxEValues());
                if (plotObject.hasFilledMarker()) { // fill markers with secondary color
                    int markSize = plotObject.getMarkerSize();
                    g.setColor(plotObject.getColor2());
                    g.setStroke(new BasicStroke(1));
                    for (int i = 0; i < nPoints; i++)
                        if ((!plot.logXAxis || plotObject.getxValues()[i] > 0) && (!plot.logYAxis || plotObject.getyValues()[i] > 0)
                                && !Double.isNaN(plotObject.getxValues()[i]) && !Double.isNaN(plotObject.getyValues()[i]))
                            fillShape(g, plotObject.getShape(), (int) plot.scaleXtoPxl(plotObject.getxValues()[i]), (int) plot.scaleYtoPxl(plotObject.getyValues()[i]), markSize);
                    g.setColor(plotObject.getColor());
                    g.setStroke(new BasicStroke(plot.sc(plotObject.getLineWidth())));
                }
                if (plotObject.hasCurve()) {        // draw the lines between the points
                    if (plotObject.getShape() == CONNECTED_CIRCLES)
                        g.setColor(plotObject.getColor2() == null ? Color.black : plotObject.getColor2());
                    drawFloatPolyline(g, plotObject.getxValues(), plotObject.getyValues(), nPoints);
                    g.setColor(plotObject.getColor());
                }
                if (plotObject.hasMarker()) {       // draw the marker symbols
                    int markSize = plotObject.getMarkerSize();
                    g.setColor(plotObject.getColor());
                    Font saveFont = g.getFont();
                    for (int i = 0; i < Math.min(plotObject.getxValues().length, plotObject.getyValues().length); i++) {
                        if ((!plot.logXAxis || plotObject.getxValues()[i] > 0) && (!plot.logYAxis || plotObject.getyValues()[i] > 0)
                                && !Double.isNaN(plotObject.getxValues()[i]) && !Double.isNaN(plotObject.getyValues()[i]))
                            drawShape(g, plotObject, plot.scaleXtoPxl(plotObject.getxValues()[i]), plot.scaleYtoPxl(plotObject.getyValues()[i]), markSize, i);
                    }
                    if (plotObject.getShape() == CUSTOM)
                        g.setFont(saveFont);
                }
                g.setClip(null);
                break;
            case IPlotObject.LINE:
                g.setClip(plot.frame);
                g.draw(new Line2D.Double(plot.scaleXtoPxl(plotObject.getX()),
                        plot.scaleYtoPxl(plotObject.getY()),
                        plot.scaleXtoPxl(plotObject.getxEnd()),
                        plot.scaleYtoPxl(plotObject.getyEnd())));
                g.setClip(null);
                break;
            case IPlotObject.NORMALIZED_LINE:
                g.setClip(plot.frame);
                double ix1 = leftMargin + (int) (plotObject.getX() * frameWidth);
                double iy1 = topMargin + (int) (plotObject.getY() * frameHeight);
                double ix2 = leftMargin + (int) (plotObject.getxEnd() * frameWidth);
                double iy2 = topMargin + (int) (plotObject.getyEnd() * frameHeight);
                g.draw(new Line2D.Double(ix1, iy1, ix2, iy2));
                g.setClip(null);
                break;
            case IPlotObject.DOTTED_LINE:
                g.setClip(plot.frame);
                ix1 = plot.scaleXtoPxl(plotObject.getX());
                iy1 = plot.scaleYtoPxl(plotObject.getY());
                ix2 = plot.scaleXtoPxl(plotObject.getxEnd());
                iy2 = plot.scaleYtoPxl(plotObject.getyEnd());
                double length = Plot.calculateDistance(ix1, ix2, iy1, iy2) + 0.1;
                int n = (int) (length / plotObject.getStep());
                //todo make this match orig. output - close enough
                g.setStroke(new BasicStroke(3, BasicStroke.CAP_BUTT, BasicStroke.JOIN_ROUND,
                        1f, new float[]{2f, 0f, 2f}, 2f));
                g.draw(new Line2D.Double(ix1, iy1, ix2, iy2));
                g.setStroke(stroke);
                g.setClip(null);
                break;
            case IPlotObject.LABEL:
            case IPlotObject.NORMALIZED_LABEL:
                setJustification(plotObject.getJustification());
                if (plotObject.getFont() != null) g.setFont(plot.scFont(plotObject.getFont()));
                int xt = (int) (type == IPlotObject.LABEL ? plot.scaleXtoPxl(plotObject.getX()) : leftMargin + (int) (plotObject.getX() * frameWidth));
                int yt = (int) (type == IPlotObject.LABEL ? plot.scaleYtoPxl(plotObject.getY()) : topMargin + (int) (plotObject.getY() * frameHeight));
                //todo bundle font to make it selectable/searchable, this is vectorized by default
                // see https://github.com/rototor/pdfbox-graphics2d#rendering-text-using-fonts-vs-vectors
                drawString(g, plotObject.getLabel(), xt, yt);
                break;
            default:
                break;
        }
    }

    private void drawArrow(Graphics2D g, double x1, double y1, double x2, double y2, double size) {
        double dx = x2 - x1;
        double dy = y2 - y1;
        double ra = Math.sqrt(dx * dx + dy * dy);
        dx /= ra;
        dy /= ra;
        int x3 = (int) Math.round(x2 - dx * size);	//arrow base
        int y3 = (int) Math.round(y2 - dy * size);
        double r = 0.3 * size;
        int x4 = (int) Math.round(x3 + dy * r);
        int y4 = (int) Math.round(y3 - dx * r);
        int x5 = (int) Math.round(x3 - dy * r);
        int y5 = (int) Math.round(y3 + dx * r);
        g.draw(new Line2D.Double(x1, y1, x2, y2));
        g.draw(new Line2D.Double(x4, y4, x2, y2));
        g.draw(new Line2D.Double(x2, y2, x5, y5));
    }

    private void drawVerticalErrorBars(Graphics2D g, float[] x, float[] y, float[] e) {
        int nPoints = Math.min(Math.min(x.length, y.length), e.length);
        for (int i = 0; i < nPoints; i++) {
            if (Float.isNaN(x[i]) || Float.isNaN(y[i]) || (plot.logXAxis && !(x[i] > 0))) continue;
            double x0 = plot.scaleXtoPxl(x[i]);
            double yPlus = plot.scaleYtoPxl(y[i] + e[i]);
            double yMinus = plot.scaleYtoPxl(y[i] - e[i]);
            g.draw(new Line2D.Double(x0, yMinus, x0, yPlus));
        }
    }

    private void drawHorizontalErrorBars(Graphics2D g, float[] x, float[] y, float[] e) {
        int nPoints = Math.min(Math.min(x.length, y.length), e.length);
        for (int i = 0; i < nPoints; i++) {
            if (Float.isNaN(x[i]) || Float.isNaN(y[i]) || (plot.logXAxis && !(y[i] > 0))) continue;
            double y0 = plot.scaleYtoPxl(y[i]);
            double xPlus = plot.scaleYtoPxl(y[i] + e[i]);
            double xMinus = plot.scaleYtoPxl(y[i] - e[i]);
            g.draw(new Line2D.Double(xMinus, y0, xPlus, y0));
        }
    }

    void drawString(Graphics2D g, String s, int x, int y) {
        if (s == null || s.equals("")) return;
        cx = x;
        cy = y;
        if (!s.contains("\n")) {
            drawString2(g, s);
        } else {
            String[] s2 = Tools.split(s, "\n");
            for (String value : s2) {
                drawString2(g, value);
            }
        }
    }

    //todo See https://docs.oracle.com/javase/tutorial/2d/geometry/primitives.html

    /**
     * Draws a single-line string at the current drawing location cx, cy and
     * adds the line height (FontMetrics.getHeight) to the current y coordinate 'cy'
     */
    private void drawString2(Graphics2D g, String s) {
        int w = getStringWidth(g, s);
        int cxx = cx;
        if (justification == CENTER_JUSTIFY)
            cxx -= w / 2;
        else if (justification == RIGHT_JUSTIFY)
            cxx -= w;
        int h = g.getFontMetrics().getHeight();
        if (w <= 0 || h <= 0) return;
        int descent = g.getFontMetrics().getDescent();

        if (cxx >= 0 && cy - h >= 0) {
            Java2.setAntialiasedText(g, true);
            g.drawString(s, cxx, (h - descent) + cy - h);
            cy += h;
        }
    }

    /**
     * Returns the width in pixels of the specified string, including any background
     * space (whitespace) between the x drawing coordinate and the string, not necessarily
     * including all whitespace at the right.
     */
    public int getStringWidth(Graphics2D g, String s) {
        // Note that fontMetrics.getStringBounds often underestimates the width (worst for italic fonts on Macs)
        // On the other hand, GlyphVector.getPixelBounds (returned by this.getStringBounds)
        // does not include the full character width of e.g. the '1' character, which would make
        // lists of right-justified numbers such as the y axis of plots look ugly.
        // Thus, the maximum of both methods is returned.
        Rectangle2D rect = getStringBounds(g, s);
        return (int) Math.max(g.getFontMetrics().getStringBounds(s, g).getWidth(), rect.getX() + rect.getWidth());
    }

    /**
     * Returns a rectangle enclosing the pixels affected by drawString
     * assuming it is drawn at (x=0, y=0). As drawString draws above the drawing location,
     * the y coordinate of the rectangle is negative.
     */
    public Rectangle getStringBounds(Graphics2D g, String s) {
        GlyphVector gv = g.getFont().createGlyphVector(g.getFontRenderContext(), s);
        Rectangle2D rect = gv.getPixelBounds(null, 0.f, -g.getFontMetrics().getDescent());
        return new Rectangle((int) rect.getX(), (int) rect.getY(), (int) rect.getWidth(), (int) rect.getHeight());
    }

    /**
     * Draw a polygon line; NaN values interrupt it.
     */
    //todo make curves be curves
    void drawFloatPolyline(Graphics2D g, float[] x, float[] y, int n) {
        if (x == null || x.length == 0) return;
        double x1, y1;
        boolean isNaN1;
        double x2 = plot.scaleXtoPxl(x[0]);
        double y2 = plot.scaleYtoPxl(y[0]);
        boolean isNaN2 = Float.isNaN(x[0]) || Float.isNaN(y[0]) || (plot.logXAxis && x[0] <= 0) || (plot.logYAxis && y[0] <= 0);
        for (int i = 1; i < n; i++) {
            x1 = x2;
            y1 = y2;
            isNaN1 = isNaN2;
            x2 = plot.scaleXtoPxl(x[i]);
            y2 = plot.scaleYtoPxl(y[i]);
            isNaN2 = Float.isNaN(x[i]) || Float.isNaN(y[i]) || (plot.logXAxis && x[i] <= 0) || (plot.logYAxis && y[i] <= 0);
            if (!isNaN1 && !isNaN2)
                g.draw(new Line2D.Double(x1, y1, x2, y2));
        }
    }

    /**
     * Fills space between polyline and y=0 with the current color (the secondary color of the plotObject)
     */
    //todo make curves be curves
    void drawFloatPolyLineFilled(Graphics2D g, float[] xF, float[] yF, int len) {
        if (xF == null || len <= 1)
            return;
        g.setStroke(new BasicStroke(1));
        int y0 = plot.scaleYWithOverflow(0);
        double x1, y1;
        double x2 = plot.scaleXtoPxl(xF[0]);
        double y2 = plot.scaleYtoPxl(yF[0]);
        boolean isNaN1;
        boolean isNaN2 = Float.isNaN(xF[0]) || Float.isNaN(yF[0]) || (plot.logXAxis && xF[0] <= 0) || (plot.logYAxis && yF[0] <= 0);
        for (int i = 1; i < len; i++) {
            isNaN1 = isNaN2;
            isNaN2 = Float.isNaN(xF[i]) || Float.isNaN(yF[i]) || (plot.logXAxis && xF[i] <= 0) || (plot.logYAxis && yF[i] <= 0);
            x1 = x2;
            y1 = y2;
            x2 = plot.scaleXtoPxl(xF[i]);
            y2 = plot.scaleYtoPxl(yF[i]);
            double left = (int) x1;
            double right = (int) x2;
            if (isNaN1 || isNaN2) continue;
            if (left < plot.frame.x && right < plot.frame.x) continue; // ignore if all outside the plot area
            if (left >= plot.frame.x + plot.frame.width && right >= plot.frame.x + plot.frame.width) continue;
            if (left < plot.frame.x) left = plot.frame.x;
            if (left >= plot.frame.x + plot.frame.width) left = plot.frame.x + plot.frame.width - 1;
            if (right < plot.frame.x) right = plot.frame.x;
            if (right >= plot.frame.x + plot.frame.width) right = plot.frame.x + plot.frame.width - 1;
            if (left != right) {
                for (double xi = Math.min(left, right); xi <= Math.max(left, right); xi++) {
                    double yi = Math.round(y1 + (y2 - y1) * (xi - x1) / (x2 - x1));
                    g.draw(new Line2D.Double(xi, y0, xi, yi));
                }
            } else {
                g.draw(new Line2D.Double(left, y0, left, y2));
            }
        }
    }

    /**
     * Fill the area of the symbols for data points (except for shape=DOT)
     * Note that ip.fill, ip.fillOval etc. can't be used here: they do not care about the clip rectangle
     */
    // Adapted from Plot
    void fillShape(Graphics2D g, int shape, int x0, int y0, int size) {
        if (shape == DIAMOND) size = (int) (size * 1.21);
        int r = plot.sc(size / 2f) - 1;
        switch (shape) {
            case BOX:
                g.fill(new Rectangle2D.Double(x0 - r, y0 - r, x0 + r, y0 + r));
                /*for (int dy=-r; dy<=r; dy++)
                    for (int dx=-r; dx<=r; dx++)
                        drawDot(g, x0+dx, y0+dy);*///todo not loop, not dots
                break;
            case TRIANGLE:
                int ybase = y0 - r - plot.sc(1);
                int yend = y0 + r;
                double halfWidth = plot.sc(size / 2f) + plot.sc(1) - 1;
                double hwStep = halfWidth / (yend - ybase + 1);
                for (int y = yend; y >= ybase; y--, halfWidth -= hwStep) {
                    int dx = (int) (Math.round(halfWidth));
                    for (int x = x0 - dx; x <= x0 + dx; x++)
                        drawDot(g, x, y);//todo not loop, not dots
                }
                break;
            case DIAMOND:
                ybase = y0 - r - plot.sc(1);
                yend = y0 + r;
                halfWidth = plot.sc(size / 2f) + plot.sc(1) - 1;
                hwStep = halfWidth / (yend - ybase + 1);
                for (int y = yend; y >= ybase; y--) {
                    int dx = (int) (Math.round(halfWidth - (hwStep + 1) * Math.abs(y - y0)));
                    for (int x = x0 - dx; x <= x0 + dx; x++)
                        drawDot(g, x, y);//todo not loop, not dots
                }
                break;
            case CIRCLE:
            case CONNECTED_CIRCLES:
                drawCircle(g, x0, y0, r, true);
                break;
        }
    }

    /**
     * Draw the symbol for the data point number 'pointIndex' (pointIndex < 0 when drawing the legend)
     */
    void drawShape(Graphics2D g, IPlotObject plotObject, double x, double y, int size, int pointIndex) {
        int shape = plotObject.getShape();
        if (shape == DIAMOND) size = (int) (size * 1.21);
        double xbase = x - plot.sc(size) / 2f;
        double ybase = y - plot.sc(size) / 2f;
        double xend = x + plot.sc(size) / 2f;
        double yend = y + plot.sc(size) / 2f;
        switch (shape) {
            case X:
                g.draw(new Line2D.Double(xbase, ybase, xend, yend));
                g.draw(new Line2D.Double(xend, ybase, xbase, yend));
                break;
            case BOX:
                g.draw(new Rectangle2D.Double(xbase, ybase, plot.sc(size), plot.sc(size)));
                break;
            case TRIANGLE:
                g.draw(new Line2D.Double(x, ybase - plot.sc(1), xend + plot.sc(1), yend));
                g.draw(new Line2D.Double(x, ybase - plot.sc(1), xbase - plot.sc(1), yend));
                g.draw(new Line2D.Double(xend + plot.sc(1), yend, xbase - plot.sc(1), yend));
                break;
            case CROSS:
                g.draw(new Line2D.Double(xbase, y, xend, y));
                g.draw(new Line2D.Double(x, ybase, x, yend));
                break;
            case DIAMOND:
                g.draw(new Line2D.Double(xbase, y, x, ybase));
                g.draw(new Line2D.Double(x, ybase, xend, y));
                g.draw(new Line2D.Double(xend, y, x, yend));
                g.draw(new Line2D.Double(x, yend, xbase, y));
                break;
            case DOT:
                drawDot(g, x, y);
                break;
            /*case CUSTOM://todo implemnt
                if (plotObject.macroCode==null || frame==null)
                    break;
                if (x<frame.x || y<frame.y || x>=frame.x+frame.width || y>=frame.y+frame.height)
                    break;
                ImagePlus imp = new ImagePlus("", ip);
                WindowManager.setTempCurrentImage(imp);
                StringBuilder sb = new StringBuilder(140+plotObject.macroCode.length());
                sb.append("x="); sb.append(x);
                sb.append(";y="); sb.append(y);
                sb.append(";setColor('");
                sb.append(Tools.c2hex(plotObject.color));
                sb.append("');s="); sb.append(plot.sc(1));
                boolean drawingLegend = pointIndex < 0;
                double xVal = 0;
                double yVal = 0;
                if (!drawingLegend) {
                    xVal = plotObject.xValues[pointIndex];
                    yVal = plotObject.yValues[pointIndex];
                }
                sb.append(";i="); sb.append(drawingLegend ? 0 : pointIndex);
                sb.append(";xval=" + xVal);
                sb.append(";yval=" + yVal);
                sb.append(";");
                sb.append(plotObject.macroCode);
                if (!drawingLegend ||!sb.toString().contains("d2s") ) {// a graphical symbol won't contain "d2s" ..
                    String rtn = IJ.runMacro(sb.toString());//.. so it can go to the legend
                    if ("[aborted]".equals(rtn))
                        plotObject.macroCode = null;
                }
                WindowManager.setTempCurrentImage(null);
                break;*/
            case CIRCLE, CONNECTED_CIRCLES:
                int r = plot.sc(size) < 5.01 ? 3 : plot.sc(0.5f * size - 0.5f);//make circles circle again
                g.draw(new Ellipse2D.Double(x - r, y - r, 2 * r, 2 * r));
                break;
            default: // CIRCLE, CONNECTED_CIRCLES: 5x5 oval approximated by 5x5 square without corners
                if (plot.sc(size) < 5.01) {
                    g.draw(new Line2D.Double(x - 1, y - 2, x + 1, y - 2));
                    g.draw(new Line2D.Double(x - 1, y + 2, x + 1, y + 2));
                    g.draw(new Line2D.Double(x + 2, y + 1, x + 2, y - 1));
                    g.draw(new Line2D.Double(x - 2, y + 1, x - 2, y - 1));
                } else {
                    r = plot.sc(0.5f * size - 0.5f);
                    g.draw(new Ellipse2D.Double(x - r, y - r, 2 * r, 2 * r));
                }
                break;
        }
    }

    void drawDot(Graphics2D g, double xcenter, double ycenter) {
        double r = lineWidth / 2.0;
        double xmin = xcenter - r + 0.5, ymin = ycenter - r + 0.5;
        double xmax = xmin + lineWidth, ymax = ymin + lineWidth;
        // draw edge dot
        double r2 = r * r;
        r -= 0.5;
        if (((int) r2) == 0) r2 = 1;
        g.fill(new Ellipse2D.Double(xmin, ymin, r2, r2));
    }

    void drawCircle(Graphics2D g, double xcenter, double ycenter, double radius, boolean fill) {
        double r = radius;
        int xmin = (int) (xcenter - r + 0.5), ymin = (int) (ycenter - r + 0.5);
        int xmax = xmin + lineWidth, ymax = ymin + lineWidth;
        // draw edge dot
        double r2 = r * r;
        r -= 0.5;
        double xoffset = xmin + r, yoffset = ymin + r;
        double xx, yy;
        if (((int) r2) == 0) r2 = 1;
        if (fill) {
            g.fillOval(xmin, ymin, (int) (r2), (int) (r2));
        } else {
            g.drawOval(xmin, ymin, (int) (r2), (int) (r2));
        }
    }

    /**
     * Sets the line width used by lineTo() and drawDot().
     */
    private void setLineWidth(int width) {
        lineWidth = width;
        if (lineWidth < 1) lineWidth = 1;
    }

    /*private static void drawPlotObject(IPlotObject plotObject) {//todo the rest of the shapes
        if (plotObject.hasFlag(PlotObject.HIDDEN)) return;
        g.setColor(plotObject.color);
        ip.setLineWidth(sc(plotObject.lineWidth));
        int type = plotObject.type;
        switch (type) {
            case PlotObject.SHAPES:
                int iBoxWidth = 20;
                g.setClip(plot.frame);
                String shType = plotObject.shapeType.toLowerCase();
                if (shType.contains("rectangles")) {
                    int nShapes = plotObject.shapeData.size();

                    for (int i = 0; i < nShapes; i++) {
                        float[] corners = (float[])(plotObject.shapeData.get(i));
                        int x1 = plot.scaleXtoPxl(corners[0]);
                        int y1 = plot.scaleYtoPxl(corners[1]);
                        int x2 = plot.scaleXtoPxl(corners[2]);
                        int y2 = plot.scaleYtoPxl(corners[3]);

                        ip.setLineWidth(sc(plotObject.lineWidth));
                        int left = Math.min(x1, x2);
                        int right = Math.max(x1, x2);
                        int top = Math.min(y1, y2);
                        int bottom = Math.max(y1, y2);

                        Rectangle r1 = new Rectangle(left, top, right-left, bottom - top);
                        Rectangle cBox = frame.intersection(r1);
                        if (plotObject.color2 != null) {
                            g.setColor(plotObject.color2);
                            ip.fillRect(cBox.x, cBox.y, cBox.width, cBox.height);
                        }
                        g.setColor(plotObject.color);
                        ip.drawRect(cBox.x, cBox.y, cBox.width, cBox.height);
                    }
                    g.setClip(null);
                    break;
                }
                if (shType.equals("redraw_grid")) {
                    ip.setLineWidth(sc(1));
                    redrawGrid();
                    g.setClip(null);
                    break;
                }
                if (shType.contains("boxes")) {

                    String[] parts = Tools.split(shType);
                    for (int jj = 0; jj < parts.length; jj++) {
                        String[] pairs = parts[jj].split("=");
                        if ((pairs.length == 2) && pairs[0].equals("width")) {
                            iBoxWidth = Integer.parseInt(pairs[1]);
                        }
                    }
                    boolean horizontal = shType.contains("boxesx");
                    int nShapes = plotObject.shapeData.size();
                    int halfWidth = Math.round(sc(iBoxWidth / 2));
                    for (int i = 0; i < nShapes; i++) {

                        float[] coords = (float[])(plotObject.shapeData.get(i));

                        if (!horizontal) {

                            int x = plot.scaleXtoPxl(coords[0]);
                            int y1 = plot.scaleYtoPxl(coords[1]);
                            int y2 = plot.scaleYtoPxl(coords[2]);
                            int y3 = plot.scaleYtoPxl(coords[3]);
                            int y4 = plot.scaleYtoPxl(coords[4]);
                            int y5 = plot.scaleYtoPxl(coords[5]);
                            ip.setLineWidth(sc(plotObject.lineWidth));

                            Rectangle r1 = new Rectangle(x - halfWidth, y4, halfWidth * 2, y2 - y4);
                            Rectangle cBox = frame.intersection(r1);
                            if (y1 != y2 || y4 != y5)//otherwise omit whiskers
                            {
                                g.drawLine(x, y1, x, y5);//whiskers
                            }
                            if (plotObject.color2 != null) {
                                g.setColor(plotObject.color2);
                                ip.fillRect(cBox.x, cBox.y, cBox.width, cBox.height);
                            }
                            g.setColor(plotObject.color);
                            ip.drawRect(cBox.x, cBox.y, cBox.width, cBox.height);
                            g.setClip(plot.frame);
                            g.drawLine(x - halfWidth, y3, x + halfWidth - 1, y3);
                        }

                        if (horizontal) {

                            int y = plot.scaleYtoPxl(coords[0]);
                            int x1 = plot.scaleXtoPxl(coords[1]);
                            int x2 = plot.scaleXtoPxl(coords[2]);
                            int x3 = plot.scaleXtoPxl(coords[3]);
                            int x4 = plot.scaleXtoPxl(coords[4]);
                            int x5 = plot.scaleXtoPxl(coords[5]);
                            ip.setLineWidth(sc(plotObject.lineWidth));
                            if(x1 !=x2 || x4 != x5)//otherwise omit whiskers
                                g.drawLine(x1, y, x5, y);//whiskers
                            Rectangle r1 = new Rectangle(x2, y - halfWidth, x4 - x2, halfWidth * 2);
                            Rectangle cBox = frame.intersection(r1);
                            if (plotObject.color2 != null) {
                                g.setColor(plotObject.color2);
                                ip.fillRect(cBox.x, cBox.y, cBox.width, cBox.height);
                            }
                            g.setColor(plotObject.color);
                            ip.drawRect(cBox.x, cBox.y, cBox.width, cBox.height);
                            g.setClip(plot.frame);
                            g.drawLine(x3, y - halfWidth, x3, y + halfWidth - 1);
                        }
                    }
                    g.setClip(null);
                    break;
                }
            case PlotObject.LEGEND:
                drawLegend(plotObject, ip);
                break;
        }
    }*/
}