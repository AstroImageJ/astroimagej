package ij.plugin.filter;

import ij.ImagePlus;
import ij.plugin.Duplicator;
import ij.process.ImageProcessor;

/**
* @deprecated
* replaced by ij.plugin.Duplicator class
*/
public class Duplicater implements PlugInFilter {
	ImagePlus imp;

	public int setup(String arg, ImagePlus imp) {
		this.imp = imp;
		return DOES_ALL+NO_CHANGES;
	}

	public void run(ImageProcessor ip) {
	}

	public ImagePlus duplicateStack(ImagePlus imp, String newTitle) {
		ImagePlus imp2 = (new Duplicator()).run(imp);
		imp2.setTitle(newTitle);
		return imp2;
	}
	
	public ImagePlus duplicateSubstack(ImagePlus imp, String newTitle, int first, int last) {
		ImagePlus imp2 = (new Duplicator()).run(imp, first, last);
		imp2.setTitle(newTitle);
		return imp2;
	}

}
