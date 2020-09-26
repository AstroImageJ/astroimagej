import ij.*;
import ij.process.*;
import ij.gui.*;
import java.awt.*;
import java.util.*;
import ij.plugin.filter.*;

public class Calculate_Quality implements PlugInFilter {

	ImagePlus imp;

	
	public int setup(String arg, ImagePlus imp) {
		this.imp = imp;
		return DOES_ALL+SUPPORTS_MASKING;
	}

	public void run(ImageProcessor ip) {
		ImageStack stack = imp.getStack();

		int size = stack.getSize();
		
		for (int slice=1; slice<=size; slice++) {
			IJ.showStatus(" "+slice+"/"+size);
			imp.setSlice(slice);
			ip = imp.getProcessor();
			IJ.write("" + slice + "\t" + calculate(ip));
			
			
		}
			
				
	}
	
	double calculate(ImageProcessor ip) {
			
			double quality = 0;
			int width = ip.getWidth();
			int height = ip.getHeight();
			
			double min = ip.getMin();
			double max = ip.getMax();
			
			ImageProcessor ip2 = ip.resize(width/2, height/2);
			ImageProcessor ip4 = ip.resize(width/4, height/4);
			
							
			for (int x = 0; x< width-1; x++) {
				for (int y = 0; y<height-1; y++) {
					
					double pixel = ip.getPixelValue(x, y);
					double adjacent_hor_pixel = ip.getPixelValue(x+1, y);
					double adjacent_ver_pixel = ip.getPixelValue(x, y+1);
					
					quality = quality + ((pixel - adjacent_hor_pixel) * (pixel - adjacent_hor_pixel));
					quality = quality + ((pixel - adjacent_ver_pixel) * (pixel - adjacent_ver_pixel));
					
					
					
					pixel = ip2.getPixelValue(x, y);
					adjacent_hor_pixel = ip2.getPixelValue(x+1, y);
					adjacent_ver_pixel = ip2.getPixelValue(x, y+1);
					
					quality = quality + ((pixel - adjacent_hor_pixel) * (pixel - adjacent_hor_pixel));
					quality = quality + ((pixel - adjacent_ver_pixel) * (pixel - adjacent_ver_pixel));
					
					
					
					pixel = ip4.getPixelValue(x, y);
					adjacent_hor_pixel = ip4.getPixelValue(x+1, y);
					adjacent_ver_pixel = ip4.getPixelValue(x, y+1);
					
					quality = quality + ((pixel - adjacent_hor_pixel) * (pixel - adjacent_hor_pixel));
					quality = quality + ((pixel - adjacent_ver_pixel) * (pixel - adjacent_ver_pixel));

					
					
				}
			}
			
			return quality;
	
	}
	

}

