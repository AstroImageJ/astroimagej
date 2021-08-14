package util;

import de.rototor.pdfbox.graphics2d.PdfBoxGraphics2D;
import ij.IJ;
import ij.ImagePlus;
import ij.WindowManager;
import ij.io.SaveDialog;
import ij.plugin.PlugIn;
import ij.process.ImageConverter;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.graphics.form.PDFormXObject;
import org.apache.pdfbox.util.Matrix;

import java.awt.image.BufferedImage;
import java.awt.image.RescaleOp;
import java.io.File;
import java.io.IOException;

/**
 * Write an ImagePlus to a PDF file
 */
public class PdfRasterWriter implements PlugIn {
    ImagePlus imp;

    @Override
    public void run(String path) {
        imp = WindowManager.getCurrentImage();
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
            writeImage(imp, path);
        } catch (Exception e) {
            String msg = e.getMessage();
            if (msg==null || msg.equals(""))
                msg = ""+e;
            msg = "An error occured writing the file.\n \n" + msg;
            if (msg.contains("NullPointerException"))
                msg = "Incorrect file path:";
            msg += "\n \n"+path;
            IJ.error("PDF Writer", msg);
        }
        IJ.showStatus("");
    }

    public void writeImage(ImagePlus imp, String path) {
        if (imp.getType()==ImagePlus.COLOR_256) {
            imp = imp.duplicate();
            new ImageConverter(imp).convertToRGB();
        }

        writeImage(imp.getBufferedImage(), path);
    }

    public void writeImage(BufferedImage image, String path) {
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage(new PDRectangle(image.getWidth(), image.getHeight()));
            document.addPage(page);

            /*
             * Creates the Graphics and sets a size in pixel. This size is used for the BBox of the XForm.
             * So everything drawn outside (0x0)-(width,height) will be clipped.
             */
            PdfBoxGraphics2D pdfBoxGraphics2D = new PdfBoxGraphics2D(document, image.getWidth(), image.getHeight());
            pdfBoxGraphics2D.drawImage(image, new RescaleOp(1, 0, null), 0, 0);

            /*
             * Dispose when finished
             */
            pdfBoxGraphics2D.dispose();

            /*
             * After dispose() of the graphics object we can get the XForm.
             */
            PDFormXObject xform = pdfBoxGraphics2D.getXFormObject();
            xform.setBBox(new PDRectangle(image.getWidth(), image.getHeight()));

            /*
             * Build a matrix to place the form
             */
            Matrix matrix = new Matrix();
            /*
             *  Note: As PDF coordinates start in the bottom left corner, we move up from there.
             */
            PDPageContentStream contentStream = new PDPageContentStream(document, page);

            contentStream.transform(matrix);

            contentStream.drawForm(xform);

            contentStream.close();
            document.save(new File(path));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
