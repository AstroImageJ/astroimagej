import ij.plugin.filter.ExtendedPlugInFilter;
import ij.plugin.filter.PlugInFilterRunner;
import ij.plugin.filter.RankFilters;
import ij.*;
import ij.gui.GenericDialog;
import ij.gui.DialogListener;
import ij.process.*;
import ij.measure.Calibration;
import java.awt.*;
import java.awt.event.*;
import java.util.*;

/** A collection of fast filters (mean, min, max, median, background, ...)
*   working on a rectangular n*m or square n*n kernel.
*   Unidirectional filtering can be done with a kernel size of n*0 or 0*n pixels.
*   Apart form the speed, the kernel type is the main difference to the built-in ImageJ
*   Process>Filters, which are working on a circular kernel, see Process>Filters>Show Circular Masks.
*
* "Filter Types":
* - Mean: Average over n*m pixels, where out-of-image pixels are replaced by nearest border pixel.
*   For large kerner sizes (radii), this gives high weight to the border pixels.
* - Border-limited mean: Average over n*m pixels; in contrast to most ImageJ filters, out-of-image
*   pixels are not replaced by border pixels but rather the area for averaging is reduced at the border.
* - Median: For unidirectional filters (i.e., with one of the radii = 0) the pixel is replaced by
*   the median of the pixels within a distance of 8x or y-)radius.
*   For bidirectional filters (i.e., if both x and y radii are >0), a fast and
*   rough approximation to the median in a rectangular surrounding is used:
*   First a median filter is applied in x direction, then in y direction.
*   In contrast to most ImageJ filters, out-of-image pixels are not replaced by border pixels but
*   rather the area for median determination is reduced at the border.
* - Minimum: Minimum over n*m pixels
* - Maximum: Maximum over n*m pixels
* - Eliminate maxima: Runs "minimum" and "maximum" filters in succession.
*   Eliminates maxima smaller than the kernel.
* - Eliminate minima: Runs "maximum" and "minimum" filters in succession.
*   Eliminates minima smaller than the kernel.
* - Background from minima: eliminates maxima (see above) and smoothens the result (border-limited mean)
* - Background from maxima: eliminates minima (see above) and smoothens the result (border-limited mean)
* - Background from median: runs a median (median approximation for bidirectional filtering, see above),
*   then smoothens the result (border-limited mean)
*
* "x Radius" and "y Radius" determine the kernel size of the filters (in pixels, irrespective of any
*   spatial calibration of the image).
*   For each target (output) pixel, the simple operations (mean, min, max) are performed over a
*   neighborhood given by a rectangle of width = 2*xRadius+1 and height = 2*yRadius+1.
*   x Radius = 0 or y Radius=0 results in no filter operation in that direction.
*
* Check "Link x & y" if a square kernel (x Radius = y Radius) is desired.
*
* "Preprocessing" is an operation applied before all others. It can be "none", "smooth" or "median".
*   For unidirectinal filtering (y radius = 0 or x radius = 0), preprocessing is also unidirectional:
*   the kernel size for unidirectional preprocessing is 5x1 or 1x5 pixels.
*   For bidirectional filters (i.e., if both x and y radii are >0), for preprocessing averaging or
*   the (approximated) median runs over a 3x3 pixel kernel.
*   Preprocessing helps to eliminate outliers for the "minimum", "maximum" and related filters.
*
* "Subtract Filtered" does not output the result of the filter operation above, but rather the
*   original (input) image minus the result of the filter operation, plus an offset.
*   With "mean" filters, "Subtract Filtered" results in a high-pass filter; with "median" it
*   highlights outliers. With the "minimum" and "maximum" filters, "Subtract Filtered" is a
*   kind of edge detection, and with the other filters it provides various types of background
*   subtraction. Especially the "Background from minima" and "Background from maxima" filters
*   are suitable for background subtraction in images with particles: Make sure that xRadius
*   and yRadius are large enough to eliminate any particles (use preview without "Subtract Filtered",
*   then apply the filter with "Subtract Filtered").
*
* "Offset" is added to the data when subtracting a filtered image from the original one.
*   The offset is needed except for 32-bit float images to keep the result in the range of
*   the image type, e.g. 0-255 for 8-bit grayscale and 8-bit/channel RGB.
*   Use a low value (e.g., 10) for subtracting "background from minima", high values (e.g. 245)
*   for subtracting "background from maxima".
*
* Notes:
* - All operations are performed on raw pixel values, not taking any grayscale calibration into account.
* - A fast alternative to filtering 16-bit images with the ImageJ built in "Subtract Background"
*   command with the traditional algorithm (rolling ball, not sliding paraboloid) are the
*   "Background from minima" and "Background from maxima" filters with preprocessing,
*   "Subtract Filtered" and a suitable offset. You have to choose a somewhat  smaller radius for the
*   fast filter than for the ImageJ built-in rolling ball filter.
*
* Code by Michael Schmid, version 2008-11-21
*/
public class Fast_Filters implements ExtendedPlugInFilter, DialogListener {
    private final static String[] TYPES = new String[] {
        "mean", "border-limited mean", "median", "minimum", "maximum", "eliminate maxima", "eliminate minima",
        "background from minima", "background from maxima", "background from median"
    };
    private final static int MEAN=0, BORDER_LIMITED_MEAN=1, MEDIAN=2, MIN=3, MAX=4;   //"elementary" filter types
        //ELIM_MAX=5,ELIM_MIN=6,BACK_MIN=7, BACK_MAX=8, BACK_MEDIAN=9;                  //"composite filters" from > 1 elementary types
    private final static int[][] taskLists = new int[][] { //what to do for the filter types
        {MEAN},
        {BORDER_LIMITED_MEAN},
        {MEDIAN},
        {MIN},
        {MAX},
        {MIN, MAX},
        {MAX, MIN},
        {MIN, MAX, BORDER_LIMITED_MEAN},
        {MAX, MIN, BORDER_LIMITED_MEAN},
        {MEDIAN, BORDER_LIMITED_MEAN}
    };
    private final static String[] PREPROCESSES = new String[] {
        "none", "smooth", "median"              //preprocessing types; 1 and 2 are the same as filter types 1 and 2 above
    };
    // Note that this makes it impossible to run the filter in parallel threads with different filter parameters!
    private static int type = MEAN;             // Filter type
    private static int xRadius = 5;             // The kernel radius in x direction
    private static int yRadius = 5;             // The kernel radius in x direction
    private static boolean linkXY = true;       // Whether to use the same radius in x&y
    private static int preProcess = 0;          // Preprocessing type
    private static boolean subtract = false;    // Whether output should be the original minus filtered
    private static double[] offset = new double[] {  // When subtracting, this will be added to the result.
        128, 32768, 0, 128, 128                 // Array for image types GRAY8, GRAY16, GRAY32, COLOR_256, COLOR_RGB
    };
    // F u r t h e r   c l a s s   v a r i a b l e s
    private int flags = DOES_ALL|CONVERT_TO_FLOAT|SNAPSHOT|SUPPORTS_MASKING|KEEP_PREVIEW|PARALLELIZE_STACKS;
    private int impType;                        // type of ImagePlus (GRAY_8, etc...)
    private int nPasses = 1;                    // The number of passes (color channels * stack slices)
    private int pass;                           // Current pass

    /**
     * This method is called by ImageJ for initialization.
     * @param arg Unused here. For plugins in a .jar file this argument string can
     *            be specified in the plugins.config file of the .jar archive.
     * @param imp The ImagePlus containing the image (or stack) to process.
     * @return    The method returns flags (i.e., a bit mask) specifying the
     *            capabilities (supported formats, etc.) and needs of the filter.
     *            See PlugInFilter.java and ExtendedPlugInFilter in the ImageJ
     *            sources for details.
     */
    public int setup(String arg, ImagePlus imp) {
        if (IJ.versionLessThan("1.38x"))        // generates an error message for older versions
            return DONE;
        return flags;
    }

    // Called by ImageJ after setup.
    public int showDialog(ImagePlus imp, String command, PlugInFilterRunner pfr) {
        impType = imp.getType();
        int digits = (impType == ImagePlus.GRAY32) ? 2 : 0;
        GenericDialog gd = new GenericDialog(command+"...");
        gd.addChoice("Filter Type", TYPES, TYPES[type]);
        gd.addNumericField("x Radius", xRadius, 0);
        gd.addNumericField("y Radius", yRadius, 0);
        gd.addCheckbox("Link x & y", linkXY);
        gd.addChoice("Preprocessing", PREPROCESSES, PREPROCESSES[preProcess]);
        gd.addCheckbox("Subtract Filtered", subtract);
        gd.addNumericField("Offset (subtract only)", offset[impType], digits);
        gd.addPreviewCheckbox(pfr);     		// passing pfr makes the filter ready for preview
        gd.addDialogListener(this);     		// the DialogItemChanged method will be called on user input
        gd.addHelp("http://imagejdocu.tudor.lu/doku.php?id=plugin:filter:fast_filters:start");
        gd.showDialog();                		// display the dialog; preview runs in the background now
       if (gd.wasCanceled()) return DONE;
        IJ.register(this.getClass());   		// protect static class variables (filter parameters) from garbage collection
        return IJ.setupDialog(imp, flags);  	// ask whether to process all slices of stack (if a stack)
    }

    // Called after modifications to the dialog. Returns true if valid input.
    public boolean dialogItemChanged(GenericDialog gd, AWTEvent e) {
        Vector numFields = gd.getNumericFields();
        TextField xNumField = (TextField)numFields.get(0);
        TextField yNumField = (TextField)numFields.get(1);
        Checkbox linkCheckbox = (Checkbox)gd.getCheckboxes().get(0);
        linkXY = gd.getNextBoolean();
        if (linkXY && !xNumField.getText().equals(yNumField.getText())) {
            if (e.getSource() == xNumField  || e.getSource() == linkCheckbox)
                yNumField.setText(xNumField.getText());
            else if (e.getSource() == yNumField)
                xNumField.setText(yNumField.getText());
        }
        type = gd.getNextChoiceIndex();
        xRadius = (int)gd.getNextNumber();
        yRadius = (int)gd.getNextNumber();
        preProcess = gd.getNextChoiceIndex();
        subtract = gd.getNextBoolean();
        offset[impType] = gd.getNextNumber();
        return (!gd.invalidNumber() && xRadius>=0 && yRadius>=0 && xRadius<1000000 && yRadius<1000000);
    }

    // Process a FloatProcessor (with the CONVERT_TO_FLOAT flag,ImageJ does the conversion).
    // Called by ImageJ for each stack slice (when processing a full stack); for RGB image also called once for each color.
    public void run(ImageProcessor ip) {
        int width = ip.getWidth();
        int height = ip.getHeight();
        Rectangle roiRect = ip.getRoi();
        int[] taskList = taskLists[type];
        int nTasks = taskList.length;
        int extraX = xRadius*(nTasks-1);        //out-of-roi margin that has to be processed for next steps
        int extraY = yRadius*nTasks;
        if (preProcess > 0) {
            extraX += xRadius;
            if (xRadius>0 && yRadius>0) {
                filterFloat(ip, preProcess, 1, true, extraX, extraY+1);
                filterFloat(ip, preProcess, 1, false, extraX, extraY);
                extraY++;
            } else if (xRadius>0) {
                filterFloat(ip, preProcess, 2, true, extraX, extraY);
            } else if (yRadius>0) {
                filterFloat(ip, preProcess, 2, false, extraX, extraY);
            }
        }
        for (int iTask=0; iTask<nTasks; iTask++) {
            if (xRadius>0) {
                filterFloat(ip, taskList[iTask], xRadius, true, xRadius*(nTasks-iTask-1), yRadius*(nTasks-iTask));
                
            }
            if (yRadius>0) {
                filterFloat(ip, taskList[iTask], yRadius, false, xRadius*(nTasks-iTask-1), yRadius*(nTasks-iTask-1));
            }
            if (Thread.currentThread().isInterrupted()) return; // interruption for new parameters during preview?
        }
        if (subtract) {
            float[] pixels = (float[])ip.getPixels();
            float[] snapPixels = (float[])ip.getSnapshotPixels();
            float fOffset = (float)offset[impType];
            for (int y=roiRect.y; y<roiRect.y+roiRect.height; y++)
                for (int x=roiRect.x, p=x+y*width; x<roiRect.x+roiRect.width; x++,p++)
                    pixels[p] = snapPixels[p] - pixels[p] + fOffset;
            if (Thread.currentThread().isInterrupted()) return;
        }
        if (roiRect.height!=height || roiRect.width!=width)
            resetOutOfRoi(ip, extraX, extraY); // reset out-of-Rectangle pixels above and below roi
        showProgress(1.0);
        return;
	}

    /** Filter a float image in one direction (x or y).
     * @param ip        The Image with the original data where also the result will be stored
     * @param type      Filter type: MEAN, MIN, ...
     * @param radius    Radius of kernel, e.g. r=1 for 3-point
     * @param xDirection True for filtering in x direction, false for y direction
     * @param extraX    The roi bounds should be grown by this in x direction
     * @param extraY    The roi bounds should be grown by this in y direction
     */
    private void filterFloat(ImageProcessor ip, int type, int radius, boolean xDirection, int extraX, int extraY) {
        float sign = (type==MIN) ? -1 : 1;
        int width = ip.getWidth();
        int height = ip.getHeight();
        Rectangle roiRect = (Rectangle)ip.getRoi().clone();
        roiRect.grow(extraX, extraY);
        Rectangle rect = roiRect.intersection(new Rectangle(width, height));

        float[] pixels = (float[])ip.getPixels();
        int length = xDirection ? width : height;   //number of points per line (line can be a row or column)
        float[] cache = new float[length];          //input for filter, hopefully in CPU cache
        float[] vHi = (type == MEDIAN) ? new float[(2*xRadius+1)*(2*yRadius+1)] : null; //needed for median
        float[] vLo = (type == MEDIAN) ? new float[(2*xRadius+1)*(2*yRadius+1)] : null; //needed for median                       
        int pointInc = xDirection ? 1 : width;      //increment of the pixels array index to the next point in a line
        int lineInc = xDirection ? width : 1;       //increment of the pixels array index to the next line
        int lineFrom = (xDirection ? rect.y : rect.x);  //the first line to process
        if (lineFrom < 0) lineFrom = 0;
        int lineTo = (xDirection ? rect.y+rect.height : rect.x+rect.width); //the last line+1 to process
        if (lineTo > (xDirection ? height:width)) lineTo = (xDirection ? height:width);
        int writeFrom = xDirection? rect.x : rect.y;  //first point of a line that needs to be written
        int writeTo = xDirection ? rect.x+rect.width : rect.y+rect.height;
        int readFrom = (writeFrom-radius < 0) ? 0 : writeFrom-radius;
        int readTo = (writeTo+radius > length) ? length : writeTo+radius;
        int pixel0 = lineFrom*lineInc + writeFrom*pointInc; //the first pixel to write in a line
        for (int line=lineFrom; line<lineTo; line++, pixel0+=lineInc) {
            if (line%30==0) {
                if (Thread.currentThread().isInterrupted()) return; // interruption for new parameters during preview?
                showProgress((double)(line-lineFrom)/(lineTo-lineFrom));
            }
            int p = line*lineInc + readFrom*pointInc;
            for (int i=readFrom; i<readTo; i++ ,p+=pointInc)
                cache[i] = pixels[p]*sign;
            switch (type) {
                case MEAN:
                    lineMean (radius, cache, pixels, writeFrom, writeTo, pixel0, pointInc);
                    break;
                case BORDER_LIMITED_MEAN:
                    lineBorderLimitedMean (radius, cache, pixels, writeFrom, writeTo, pixel0, pointInc);
                    break;
                case MEDIAN:
                    lineMedian (radius, cache, pixels, writeFrom, writeTo, pixel0, pointInc, vLo, vHi);
                    break;
                case MIN: case MAX:
                    lineMax(radius, sign, cache, pixels, writeFrom, writeTo, pixel0, pointInc);
            }
        }
        pass++;
    }

    // Mean filter of a line.
    // When trying to access out-of-border pixels it replaces them with the nearest border pixel.
    // (this is the usual behavior of ImageJ filters)
    // radius: Kernel width is 2*radius+1
    // cache: Holds input data for one line, i.e., for one image row or column
    // pixels: Image data are written to this point
    // writeFrom: Index of first point of the line that should be written.
    // writeTo: Last point + 1 of the line that should be written. Data will be read from 'cache'
    // also outside the 'writeFrom', 'writeTo' range.
    // pixel0: Index of the first value to be written in 'pixels'. Corresponds to index 'writeFrom' in 'cache'.
    // pointInc: increment of index in 'pixels' from one point to the next (1 for image rows, width for columns).
    private static void lineMean (int radius, float[] cache, float[] pixels, int writeFrom, int writeTo,
            int pixel0, int pointInc) {
        double sum = 0;
        double factor = 1./(1 + 2*radius);
        int length = cache.length;
        float first = cache[0];
        float last = cache[length-1];
        int sumFrom = writeFrom-radius;
        int sumTo = writeFrom+radius;
        if (sumFrom < 0) {
            sum = -sumFrom*first;
            sumFrom = 0;
        }
        if (sumTo > length) {
            sum += (sumTo-length)*last;
            sumTo = length;
        }
        for (int i=sumFrom; i<sumTo; i++)
            sum += cache[i];
        for (int i = writeFrom, iMinus =i-radius, iPlus=i+radius, p=pixel0;
                i<writeTo; i++,iMinus++,iPlus++,p+=pointInc) {
            sum += (iPlus<length) ? cache[iPlus] : last;
            pixels[p] = (float)(sum*factor);
            sum -= (iMinus>=0) ? cache[iMinus] : first;
        }
    }

    // Mean filter of a line; at the image borders it does not give extra weight to the border pixels.
    private static void lineBorderLimitedMean (int radius, float[] cache, float[] pixels, int writeFrom, int writeTo,
            int pixel0, int pointInc) {
        double sum = 0;
        int length = cache.length;
        int sumFrom = (writeFrom-radius>0) ? writeFrom-radius : 0;
        int sumTo = (writeFrom+radius<length) ? writeFrom+radius : length;
        int kSize = sumTo - sumFrom;
        for (int i=sumFrom; i<sumTo; i++)
            sum += cache[i];
        for (int i = writeFrom, iMinus =i-radius, iPlus=i+radius, p=pixel0;
                i<writeTo; i++,iMinus++,iPlus++,p+=pointInc) {
            if (iPlus<length) { sum += cache[iPlus]; kSize++; }
            pixels[p] = ((float)sum)/kSize;
            if (iMinus>=0) { sum -= cache[iMinus]; kSize--; }
        }
    }

    // Median filter of a line; at the image borders it does not give extra weight to the border pixels.
    private static void lineMedian (int radius, float[] cache, float[] pixels, int writeFrom, int writeTo,
            int pixel0, int pointInc, float[] vHi, float[] vLo) {
        int length = cache.length;
        float median = cache[writeFrom];                //a first guess
        for (int i=writeFrom, iMinus=i-radius, iPlus=i+radius, p=pixel0;
                i<writeTo; i++, iMinus++,iPlus++,p+=pointInc) {
            int nHi = 0, nLo = 0;
            int iStart = (iMinus>=0) ? iMinus : 0;
            int iStop = (iPlus<length) ? iPlus : length-1;
            int nPoints = iStop-iStart+1;
            for (int iRead=iStart; iRead <= iStop; iRead++) {
                float v = cache[iRead];
                if (v > median) vHi[nHi++] = v;
                else if (v < median) vLo[nLo++] = v;
            }
            if (nPoints%2 == 0) {           //avoid an even number of points: in case of doubt, leave it closer to original value
                float v = cache[i];
                if (v > median) vHi[nHi++] = v;
                else if (v < median) vLo[nLo++] = v;
            }
            int half = nPoints/2;
            if (nHi>half)
                median = RankFilters.findNthLowestNumber(vHi, nHi, nHi-half-1);
            else if (nLo>half)
                median = RankFilters.findNthLowestNumber(vLo, nLo, half);
            pixels[p] = median;
        }
    }

    // Algorithm for finding maxima within a range of the input array 'cache':
    // - When going to the next pixel, if the new pixel is > than the old maximum take it.
    // - Get the maximum over the full range only if the pixel that is not in the range any more ('out')
    //   could be the one that has caused the current value of the maximum.
    // - It is faster to start at a border than to end there: In the beginning, we need not care about
    //   pixels that get out of the range. Thus the algorithm starts from both borders; whenever a full
    //   determination of the maximum over the full range becomes necessary, the algorithm tries to avoid
    //   this by continuing at the other end.
    private static void lineMax (int radius, float sign, float[] cache, float[] pixels, int writeFrom, int writeTo,
            int pixel0, int pointInc) {
        int length = cache.length;
        int pUp = pixel0;
        int pDn = pixel0 + (writeTo-writeFrom-1)*pointInc;
        float maxUp = -Float.MAX_VALUE;
        float maxDn = -Float.MAX_VALUE;
        int iInUp = writeFrom + radius;         //new in the range that we have to find the max of
        int iOutUp = writeFrom - radius - 1;    //not in the range any more
        int iInDn = writeTo - radius -1;
        int iOutDn = writeTo + radius;

        while (pUp<=pDn) {
            boolean first = true;
            for (; pUp<=pDn; pUp+=pointInc, iInUp++, iOutUp++) {
                float oldmax = maxUp;
                if (iInUp<length && maxUp<cache[iInUp]) maxUp = cache[iInUp];
                if (first || (iOutUp>=0 && cache[iOutUp]==oldmax && (iInUp>=length || cache[iOutUp]>cache[iInUp]))) {
                //if (pixel0==0)IJ.log("UP: first="+first+" p="+pUp+" oldmax="+oldmax);
                if (!first) break;
                    int maxFrom = (iOutUp >= -1) ? iOutUp+1 : 0;
                    int maxTo = (iInUp < length) ? iInUp : length-1;
                    //if (pixel0==0)IJ.log("UP range: "+maxFrom+" - "+maxTo);
                    maxUp = cache[maxFrom];
                    for (int i=maxFrom+1; i<=maxTo; i++)
                        if (maxUp < cache[i]) maxUp = cache[i];
                }
               //else if (pixel0==0)IJ.log("up quick: first="+first+" p="+pUp+" oldmax="+oldmax);

                first = false;
                pixels[pUp] = maxUp*sign;
            }
            first = true;
            for (; pUp<=pDn; pDn-=pointInc, iInDn--, iOutDn--) {
                float oldmax = maxDn;
                if (iInDn>=0 && maxDn<cache[iInDn]) maxDn = cache[iInDn];
                if (first || (iOutDn<length && cache[iOutDn]==oldmax && (iInDn<0 || cache[iOutDn]>cache[iInDn]))) {
                    if (!first) break;
                    int maxFrom = (iOutDn <=length) ? iOutDn-1 : length-1;
                    int maxTo = (iInDn > 0) ? iInDn : 0;
                    maxDn = cache[maxFrom];
                    for (int i=maxFrom-1; i>=maxTo; i--)
                        if (maxDn < cache[i]) maxDn = cache[i];
                }
                first = false;
                pixels[pDn] = maxDn*sign;
            }
        }
    }

    /** In a zone of width 'extraX' (in x direction) and 'extraY' in y direction around the roi rectangle,
     *  reset pixels to the snapshot */
    private static void resetOutOfRoi (ImageProcessor ip, int extraX, int extraY) {
        int width = ip.getWidth();
        int height = ip.getHeight();
        Rectangle roiRect = ip.getRoi();
        Rectangle temp = (Rectangle)roiRect.clone();
        temp.grow(extraX, extraY);
        Rectangle outer = temp.intersection(new Rectangle(width, height));

        float[] pixels = (float[])ip.getPixels();
        float[] snapPixels = (float[])ip.getSnapshotPixels();
        for (int y=outer.y, p=y*width+outer.x; y<roiRect.y; y++, p+=width)
            System.arraycopy(snapPixels, p, pixels, p, outer.width);
        int leftWidth = roiRect.x - outer.x;
        int rightWidth = outer.x+outer.width - (roiRect.x+roiRect.width);
        for (int y=roiRect.y; y<roiRect.y+roiRect.height; y++) {
            if (leftWidth > 0) {
                int p = outer.x + y*width;
                System.arraycopy(snapPixels, p, pixels, p, leftWidth);
            }
            if (rightWidth > 0) {
                int p = roiRect.x+roiRect.width + y*width;
                System.arraycopy(snapPixels, p, pixels, p, rightWidth);
            }
        }
        for (int y=roiRect.y+roiRect.height, p=y*width+outer.x; y<outer.y+outer.height; y++, p+=width)
            System.arraycopy(snapPixels, p, pixels, p, outer.width);

    }

    /** This method is called by ImageJ to set the number of calls to run(ip)
     *  corresponding to 100% of the progress bar */
    public void setNPasses (int nPasses) {
        if (xRadius>0 && yRadius>0) nPasses *= 2;
        this.nPasses = nPasses * taskLists[type].length;
        pass = 0;
    }

    /** Update the progress bar */
    private void showProgress(double percent) {
        if (nPasses == 0) return;
        percent = (double)pass/nPasses + percent/nPasses;
        IJ.showProgress(percent);
    }

}
