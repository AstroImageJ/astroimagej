package Astronomy;// Read_MeasurementTable.java

import astroj.MeasurementTable;
import ij.IJ;
import ij.io.OpenDialog;
import ij.plugin.PlugIn;

public class Read_MeasurementTable implements PlugIn
	{
	public void run(String arg)
		{
        String path = arg;
        if (path == null || path.equals(""))
            {
            OpenDialog od = new OpenDialog("Select measurement table to be opened",null);
            String dir = od.getDirectory();
            String filename = od.getFileName();
            if (dir == null || filename == null)
                return;
            path = dir + filename;
            }
        
		// IJ.showMessage ("open "+dir+filename);
		MeasurementTable table = MeasurementTable.getTableFromFile (path);
		if (table == null)
			IJ.showMessage ("Unable to open measurement table "+path);
		else
            {
			table.show();
            if (MultiPlot_.isRunning()) {
                MultiPlot_.loadDataOpenConfig(table, path);
                }
            }
		}
	}
