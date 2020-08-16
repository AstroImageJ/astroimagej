package Astronomy;// UpdateAstroWindows.java

import ij.plugin.*;
import ij.*;

public class UpdateAstroWindows implements PlugIn
	{
	public void run(String arg)
		{
        try
            {
            if (MultiPlot_.mainFrame != null && MultiPlot_.useUpdateStack)
                {
                MultiPlot_.updateStack();
                }
            }
        catch(Exception ex) {}
        
        try
            {
            if (MultiPlot_.addAstroDataFrame != null)
                {
                MultiPlot_.updateMPAstroConverter();
                }
            }
        catch (Exception ex) {}
		}
	}
