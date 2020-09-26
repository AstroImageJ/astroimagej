import ij.*;
import ij.process.*;
import ij.gui.*;
import java.awt.*;
import java.util.*;
import ij.plugin.filter.*;
import ij.measure.*;

public class Select_Frames_With_Best_Edges implements PlugInFilter, Measurements {

	ImagePlus imp;
	
	
	boolean abort = false;
	
	
	public int setup(String arg, ImagePlus imp) {
		this.imp = imp;
		return DOES_ALL;
	}

	public void run(ImageProcessor ip) {
		ImageStack stack = imp.getStack();
		int width = imp.getWidth();
		int height = imp.getHeight();
		ImageStack stack2 = new ImageStack(width, height, imp.getProcessor().getColorModel());
		
		int size = stack.getSize();

		double quality = 0;	
		double best = 1;	
		double[] qf = new double[size];
		
		double percent = getInput();
		if(abort) return;
		
		for (int slice=1; slice<=size; slice++) {
			quality = calculate(ip);
			IJ.showStatus(" "+slice+"/"+size);
			imp.setSlice(slice);
			ip = imp.getProcessor();
			
			qf[slice-1] = quality;
			if (quality > best) {best = quality;}

		}
			
		
		for (int slice=1; slice<=size; slice++) {			
			if(qf[slice-1]/best >= percent/100) {
				imp.setSlice(slice);
				ip = imp.getProcessor();
				ip = ip.crop();
				stack2.addSlice(stack.getSliceLabel(slice), ip);
				IJ.write("" + slice + "\t" + qf[slice-1]);
			}
		}
			
		
		ImagePlus impSubstack = imp.createImagePlus();
		impSubstack.setStack("Best " + percent +"% of images", stack2);
		impSubstack.setCalibration(imp.getCalibration());
		impSubstack.show();
				

				
	}
	
	double calculate(ImageProcessor ip) {
			
			double quality = 0;
			ImageProcessor edges = ip.duplicate();
			edges.findEdges();
			quality = ImageStatistics.getStatistics(edges, MEAN, null).mean;
			return quality;
	
	}
	
	
	double getInput(){
        GenericDialog gd = new GenericDialog("Image Selector", IJ.getInstance());
        
        gd.addNumericField("Create stack of images that are at least", 95, 1, 4, "%");
		gd.addMessage("as good as the best image.");
        gd.showDialog();
        if (gd.wasCanceled()){
            abort = true;
			return 0;
			
        }
       return gd.getNextNumber();

    }
	

}

