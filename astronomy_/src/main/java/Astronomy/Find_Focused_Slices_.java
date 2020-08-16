package Astronomy;

import ij.*;
import ij.process.*;
import ij.gui.*;
import java.awt.*;
import ij.plugin.filter.*;
import ij.measure.*;

/** Select focused slices from a Z stack. Based on the autofocus algorithm "Normalized Variance" (Groen et al., 1985; Yeo et
 * al., 1993). However, the images are first treated by a sobel edge filter. This provided a better result for fluorescent bead images.
 * Code modified from the "Select Frames With Best Edges" plugin from Jennifer West (http://www.umanitoba.ca/faculties/science/astronomy/jwest/plugins.html)
 * First version 2009-9-27
 * Second version 2010-11-27
 * Third version 2011-2-15
 * Forth version 2011-3-2
 * By TSENG Qingzong; qztseng at gmail.com
 */
 
public class Find_Focused_Slices_ implements PlugInFilter, Measurements {

    ImagePlus imp;
    boolean abort = false;
    double percent = 80.0, vThr = 0.0;
    boolean consecutive = false, verbose = true, edge = false;

    public int setup(String arg, ImagePlus imp) {
        this.imp = imp;
        return DOES_ALL;
    }

    public void run(ImageProcessor ip) {
        
        if(imp.isHyperStack()){
        	IJ.error("HyperStack is not supported.\nPlease split channels or time frames\nthen do the find focus seperately");
            return;
        }
        
        ImageStack stack = imp.getStack();
        int width = imp.getWidth();
        int height = imp.getHeight();
        String name = imp.getTitle();

        ImageStack stack2 = new ImageStack(width, height, imp.getProcessor().getColorModel());
        int fS = 0;

        int size = stack.getSize();
        if (size == 1){
        	IJ.error("Stack required.");
            return;
        }

        double vMax = 0;
        double[] varA = new double[size];
        consecutive = Prefs.get ("bestfocus.consecutive",consecutive);
        verbose = Prefs.get ("bestfocus.verbose",verbose);
        edge = Prefs.get ("bestfocus.edge",edge);
        percent = Prefs.get ("bestfocus.percent",percent);
        vThr = Prefs.get ("bestfocus.vThr",vThr);

        if (!getParam()) {
            return;
        }
        
        Prefs.set ("bestfocus.consecutive",consecutive);
        Prefs.set ("bestfocus.verbose",verbose);
        Prefs.set ("bestfocus.edge",edge);
        Prefs.set ("bestfocus.percent",percent);
        Prefs.set ("bestfocus.vThr",vThr);        

        if (verbose) IJ.log("\n" + "Processing: " + name);
        int cnt = 0;

        for (int slice = 1; slice <= size; slice++) {
            cnt++;
            IJ.showProgress(cnt, size);
            IJ.showStatus(" " + slice + "/" + size);
            varA[slice - 1] = calVar(imp.getStack().getProcessor(slice));
            if (verbose) {
                IJ.log("Slice: " + slice + "\t\t Variance: " + varA[slice - 1]);
            }
            if (varA[slice - 1] > vMax) {
                vMax = varA[slice - 1];
                fS = slice;
            }
        
        }
        if (vMax < vThr) {
            IJ.error("All slices are below the variance threshold value");
            return;
        }
        if (verbose) {
            IJ.log("Slices selected: ");
        }
		
		int nn = 0; 
		//go through the slices after the best focus slice 
		boolean con = true;             
        for (int slice = fS; slice <= size; slice++) {
            if (varA[slice - 1] / vMax >= percent / 100 && varA[slice - 1] > vThr && con == true) {
                ImageProcessor ipp = imp.getStack().getProcessor(slice);
                ImageProcessor ipp2 = ipp.duplicate();
                String label = stack.getShortSliceLabel(slice);
                if (label == null) label = "OriginalSlice_"+slice;
                stack2.addSlice(label, ipp2, nn);
                nn++;
                if (verbose) {
                    IJ.log("" + slice);
                }
            }else{
            	if(consecutive)	con = false;	
            }
        }
        //go through the slices before the best focus slice
        con = true;
        for (int slice = fS-1; slice >0; slice--) {
            if (varA[slice - 1] / vMax >= percent / 100 && varA[slice - 1] > vThr && con == true) {
                ImageProcessor ipp = imp.getStack().getProcessor(slice);
                ImageProcessor ipp2 = ipp.duplicate();
                String label = stack.getShortSliceLabel(slice);
                if (label == null) label = "OriginalSlice_"+slice;
                stack2.addSlice(label, ipp2, 0);
                if (verbose) {
                    IJ.log("" + slice);
                }
            }else{
            	if(consecutive)	con = false;	
            }
        }


        ImagePlus focusstack = imp.createImagePlus();
        focusstack.setStack("Focused slices of " + name + "_" + percent + "%", stack2);
        focusstack.setCalibration(imp.getCalibration());
        if (focusstack.getStackSize() == 1) {
            String label = imp.getStack().getShortSliceLabel(fS);
            if (label == null) label = "OriginalSlice_"+fS;
            focusstack.setProperty("Label", "1/1 ("+label+")");
        }
        focusstack.show();

    }

    double calVar(ImageProcessor ipp) {

        double variance = 0;
        int W = ipp.getWidth();
        int H = ipp.getHeight();

        Rectangle rect = ipp.getRoi();
        if (rect == null) {
            rect.x = 0;
            rect.y = 0;
            rect.height = H;
            rect.width = W;
        }
        ImageProcessor edged = ipp.duplicate();
//        ImageProcessor edged = ip.duplicate();
        if (edge) edged.findEdges();
        double mean = ImageStatistics.getStatistics(edged, MEAN, null).mean;
        double a = 0;
        double value;
        double diff;
        for (int y = rect.y; y < (rect.y + rect.height); y++) {
            for (int x = rect.x; x < (rect.x + rect.width); x++) {
                value = edged.getPixelValue(x, y);
                diff = value - mean;
                a += diff*diff;
            }
        }
        variance = (1 / (W * H * mean)) * a;
        return variance;

    }

    private boolean getParam() {
        GenericDialog gd = new GenericDialog("Find focused slices", IJ.getInstance());

        gd.addNumericField("Select images with at least", percent, 1, 4, "% of maximum variance.");
        gd.addNumericField("Variance threshold: ", vThr, 3, 9, "(set to zero to disable)");
        gd.addCheckbox("Edge filter first?", edge);
        gd.addCheckbox("Select_only consecutive slices?", consecutive);
        gd.addCheckbox("Verbose mode?", verbose);
        gd.showDialog();
        if (gd.wasCanceled()) {
            return false;
        }

        percent = gd.getNextNumber();
        vThr = gd.getNextNumber();
        edge = gd.getNextBoolean();
        consecutive = gd.getNextBoolean();
        verbose = gd.getNextBoolean();

        return true;

    }
}
