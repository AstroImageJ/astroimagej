package ij.astro.util;

import java.io.File;
import java.io.IOException;

import de.rototor.pdfbox.graphics2d.PdfBoxGraphics2D;
import ij.IJ;
import ij.WindowManager;
import ij.gui.Plot;
import ij.gui.PlotVirtualStack;
import ij.io.SaveDialog;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.graphics.form.PDFormXObject;
import org.apache.pdfbox.util.Matrix;

/**
 * Save a plot as a vector PDF. Drawing methods are adapted from {@link Plot}.
 */
public class PdfPlotOutput extends VectorPlotDrawing {
    private PdfPlotOutput(Plot plot) {
        super(plot);
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

    public static void savePlotStack(PlotVirtualStack plotStack, String path) {
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
            var document = new PDDocument();
            for (int i = 0; i < plotStack.getSize(); i++) {
                new PdfPlotOutput(plotStack.getPlot(i + 1)).writePdf(document, path, (i+1) == plotStack.getSize());
            }
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
        writePdf(new PDDocument(), path, true);
    }

    private void writePdf(PDDocument document, String path, boolean done) throws IOException {
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
        drawVectorForm(pdfBoxGraphics2D);

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

        if (done) {
            document.save(new File(path));
            document.close();
        }
    }
}