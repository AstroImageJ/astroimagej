package util;

import com.lowagie.text.Document;
import com.lowagie.text.Image;
import com.lowagie.text.pdf.PdfWriter;
import ij.IJ;
import ij.ImagePlus;
import ij.WindowManager;
import ij.io.SaveDialog;
import ij.plugin.PlugIn;
import ij.process.ImageConverter;

import java.io.FileOutputStream;

/**
 * Write an ImagePlus to a PDF file
 */
public class Pdf_Writer implements PlugIn {
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

    public void writeImage(ImagePlus imp, String path) throws Exception {
        if (imp.getType()==ImagePlus.COLOR_256) {
            imp = imp.duplicate();
            new ImageConverter(imp).convertToRGB();
        }

        try(FileOutputStream baos = new FileOutputStream(path)) {
            var document = new Document();
            var pdf = PdfWriter.getInstance(document, baos);
            //var win = imp.getWindow();
            var scaledImp = imp.getImage().getScaledInstance(imp.getWidth()*10, -1, java.awt.Image.SCALE_DEFAULT);
            var img = Image.getInstance(scaledImp, null, false);

            document.open();

            // Make image fit
            float scaleWidth = ((document.getPageSize().getWidth() - document.leftMargin() - document.rightMargin())
                    / img.getWidth()) * 100;
            float scaleHeight = ((document.getPageSize().getHeight() - document.topMargin() - document.bottomMargin())
                    / img.getHeight()) * 100;
            img.scalePercent(Math.min(scaleHeight, scaleWidth));

            document.add(img);

            document.close();
            pdf.close();
        }
    }
}
