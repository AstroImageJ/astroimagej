package Astronomy;// Normalize_Stack.java

import ij.*;
import ij.plugin.filter.*;
import ij.process.*;
import ij.gui.*;
import ij.measure.*;

import java.awt.*;

/**
 * Normalizes the images in a stack based on the median, average, or mode.
 */
public class Normalize_Stack implements PlugInFilter
	{
	ImagePlus imp;
	boolean norm = false;
	double val = 1.0;

	static int MEDIAN=0;
	static int MEAN=1;
	static int MODE=2;
	static String[] types = {"Median", "Mean"};
	static int things = Measurements.MEDIAN+Measurements.MEAN;

	public int setup(String arg, ImagePlus imp)
		{
		IJ.register (Normalize_Stack.class);
		if (IJ.versionLessThan("1.32c"))
			return DONE;
		this.imp = imp;
		return DOES_ALL+SUPPORTS_MASKING;
		}

	public void run(ImageProcessor ip)
		{
		GenericDialog dia = new GenericDialog("Normalize_Stack", IJ.getInstance());
		dia.addMessage ("(based on plugin written by J. West/Univ. Manitoba)");
		dia.addMessage (" ");

		dia.addChoice("Normalize each image by:", types, "Median");
		dia.addCheckbox ("Normalize to the value below or deselect to normalize to brightest image", norm);
		dia.addNumericField (" ",val,2);

		dia.showDialog();
		if (dia.wasCanceled()) return;

		int image_op = dia.getNextChoiceIndex();
		norm = dia.getNextBoolean();
		val = dia.getNextNumber();

		normalize(image_op);
		imp.getProcessor().resetMinAndMax();
		imp.updateAndRepaintWindow();
		}

	void normalize(int image_op)
		{
		Analyzer analyzer = new Analyzer();
		ImageStack stack = imp.getStack();
		int size = stack.getSize();
		double measurement = 1.0;
		double max = 0;
		double[] measurements = new double[size];

		ImageProcessor ip = imp.getProcessor();

		for (int slice=1; slice <= size; slice++)
			{
			IJ.showStatus(" "+slice+"/"+size);
			imp.setSlice(slice);

			ip = stack.getProcessor(slice);
			ImageStatistics stats = ImageStatistics.getStatistics(ip,things,null);
			// ImageStatistics stats = imp.getStatistics(Analyzer.getMeasurements());
			// IJ.log(""+slice+": mean="+stats.mean+", median="+stats.median+", mode="+stats.mode);

			if (image_op == MEAN)
				measurement = stats.mean;
			else if (image_op == MEDIAN)
				measurement = stats.median;
			else if (image_op == MODE)
				measurement = stats.mode;
			if (measurement > max)
				max = measurement;
			measurements[slice-1] = measurement;
			}

		for (int slice=1; slice <= size; slice++)
			{
			IJ.showStatus(" "+slice+"/"+size);
			imp.setSlice(slice);
			ip = stack.getProcessor(slice);
			if (norm)
				ip.multiply(val/measurements[slice-1]);
			else
				ip.multiply(max/measurements[slice-1]);
			}	
		imp.unlock();
		}
	}
