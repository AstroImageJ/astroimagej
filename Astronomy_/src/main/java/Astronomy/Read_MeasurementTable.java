package Astronomy;// Read_MeasurementTable.java

import java.nio.file.Path;
import java.util.Set;

import astroj.MeasurementTable;
import ij.IJ;
import ij.astro.util.FileAssociationHandler;
import ij.io.OpenDialog;
import ij.plugin.PlugIn;

public class Read_MeasurementTable implements PlugIn {
    public void run(String arg) {
        String path = arg;
        if (path == null || path.isEmpty()) {
            OpenDialog od = new OpenDialog("Select measurement table to be opened", null);
            String dir = od.getDirectory();
            String filename = od.getFileName();
            if (dir == null || filename == null) {
                return;
            }
            path = dir + filename;
        }

        if (!FileAssociationHandler.handleFile(path, Set.of())) {
            if (MeasurementTable.EXCEL_XLS.matches(Path.of(path))) {
                IJ.showMessage("Excel files are not supported for measurement tables. " +
                        "Please convert the file to CSV/TSV format and try again.");
                return;
            }

            IJ.showMessage("Unable to open measurement table " + path);
        }
    }
}
