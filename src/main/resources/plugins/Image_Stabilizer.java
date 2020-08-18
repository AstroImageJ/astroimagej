/**
    Image stabilizer plugin for ImageJ.

    This plugin stabilizes jittery image stacks using the
      Lucas-Kanade algorithm.

    Authors:  Kang Li (kangli AT cs.cmu.edu)
              Steven Kang (sskang AT andrew.cmu.edu)

    Requires: ImageJ 1.38q or later
              JRE 5.0 or later

    Installation:
      Download  Image_Stabilizer.java  to the  plugins  folder or  subfolder.
      Compile and run  it using Plugins/Compile  and Run. Restart  ImageJ and
      there will be a new "Image  Stabilizer" command in the Plugins menu  or
      its submenu.

    History:
        2008/02/07: First version
        2008/02/10: Optimized for speed using gradient pyramids
        2008/02/12: Performed further speed optimizations and bug fixes
        2008/02/14: Added support for affine transformation
        2008/03/15: Made user interface improvements
        2008/05/02: Added support for macro recordering 
                      (thanks to Christophe Leterrier AT univmed.fr)
        2009/01/11: Added support for logging transformation coefficients 
                    The stabilization can be interrupted by pressing ESC or by
                      closing the image 
        2009/01/20: Fixed a runtime error when Log_Transformation_Coefficients
                      is not selected (thanks to Nico Stuurman)
        2009/06/12: Fixed a bug that affected 32-bit float input images
                      (thanks to Derek Bailey)

    References:
      [1]  S.   Baker  and   I. Matthews,   "Lucas-Kanade  20   Years  On:  A
      Unifying  Framework," International Journal  of Computer Vision,   Vol.
      56, No.  3,  March, 2004, pp. 221-255.

      [2]  B.d.  Lucas  and  T.  Kanade,  "An  Iterative  Image  Registration
      Technique with an Application  to Stereo Vision  (IJCAI),"  Proceedings
      of  the 7th International Joint Conference  on Artificial  Intelligence
      (IJCAI '81), April, 1981, pp. 674-679.

*/

/**
Copyright (C) 2008-2009 Kang Li. All rights reserved.

Permission to use, copy, modify, and distribute this software for any purpose
without fee is hereby granted,  provided that this entire notice  is included
in all copies of any software which is or includes a copy or modification  of
this software  and in  all copies  of the  supporting documentation  for such
software. Any for profit use of this software is expressly forbidden  without
first obtaining the explicit consent of the author.

THIS  SOFTWARE IS  BEING PROVIDED  "AS IS",  WITHOUT ANY  EXPRESS OR  IMPLIED
WARRANTY.  IN PARTICULAR,  THE AUTHOR  DOES NOT  MAKE ANY  REPRESENTATION OR
WARRANTY OF ANY KIND CONCERNING  THE MERCHANTABILITY OF THIS SOFTWARE  OR ITS
FITNESS FOR ANY PARTICULAR PURPOSE.
*/

import java.lang.*;
import java.io.File;
import java.io.IOException;
import java.awt.event.*;
import ij.*;
import ij.gui.*;
import ij.process.*;
import ij.plugin.filter.*;
import ij.plugin.frame.Editor;
import ij.io.*;



public class Image_Stabilizer implements PlugInFilter {

    static final int TRANSLATION = 0;
    static final int AFFINE = 1;

    ImagePlus  imp = null;
    ImageStack stack = null;
    ImageStack stackOut = null;
    String     outputDir = null;
    boolean    stackVirtual = false;
    boolean    outputNewStack = false;
    int        transform = TRANSLATION;
    int        pyramidLevel = 1;
    int        maxIter = 200;
    double     tol = 1e-7;
    double     alpha = 0.9;

    /* transformation coefficient Log */
    boolean    logEnabled = false;
    Editor     logEditor = null;

    public int setup(String arg, ImagePlus imp) {
        IJ.register(Image_Stabilizer.class);
        this.imp = imp;
        return DOES_ALL|STACK_REQUIRED;
    }


    public void run(ImageProcessor ip) {
        stack = imp.getStack();
        int stackSize = stack.getSize();

        if (stack.isVirtual()) {
            boolean ok = IJ.showMessageWithCancel(
                "Image Stabilizer",
                "You are using a virtual stack.\n" +
                "You will be asked to choose an output directory to save the stablized images.\n" +
                "If you did not intend to use a virtual stack, or if you want to view the stablized images in ImageJ directly,\n" +
                "please reload the image sequence with the 'Use Virtual Stack' option unchecked.");
            if (!ok) return;
            DirectoryChooser dc = new DirectoryChooser("Output Directory");
            outputDir = dc.getDirectory();
            if (outputDir == null || outputDir.length() == 0)
                return;
            File file = new File(outputDir);
            VirtualStack virtualStack = (VirtualStack)stack;
            String stackDir = virtualStack.getDirectory();
            if (null != stackDir) {
                File stackFile = new File(stackDir);
                try {
                    file = file.getCanonicalFile();
                    stackFile = stackFile.getCanonicalFile();
                }
                catch (IOException e) {
                    IJ.error("Could not get canonical file path.");
                    return;
                }
                if (file.equals(stackFile)) {
                    IJ.error("Output directory must be difference from the stack directory.");
                    return;
                }
            }
            stackVirtual = true;
            outputNewStack = false;
        }

        if (!showDialog(ip))
            return;

        int current = imp.getCurrentSlice();
        ImageProcessor ipRef = stack.getProcessor(current);

        if (outputNewStack)
            stackOut = new ImageStack(ip.getWidth(), ip.getHeight());

        showProgress(0.0);
        if (!IJ.escapePressed()) {
            process(ipRef, current - 1, 1, -1, 1);
            if (!IJ.escapePressed())
                process(ipRef, current, stackSize, 1, current);
        }

        if (!outputNewStack) // in-place processing
            imp.updateAndDraw();
        else if (stackOut.getSize() > 0) {
            // Create new image using the new stack.
            ImagePlus impOut = new ImagePlus(
                "Stablized " + imp.getShortTitle(), stackOut);
            impOut.setStack(null, stackOut);

            // Display the new stacks.
            impOut.show();
        }
    }


    int getTransform(String name) {
        int xform = TRANSLATION;
        if (name.compareTo("Affine") == 0)
            xform = AFFINE;
        return xform;
    }


    String getTransformName(int xform) {
        String name = "Translation";
        if (xform == AFFINE)
            name = "Affine";
        return name;
    }


    boolean showDialog(ImageProcessor ip) {
        final String[] transformNames = { "Translation", "Affine" };
        final String[] pyramidLevelNames = { "0", "1", "2", "3", "4" };
        GenericDialog gd = new GenericDialog("Image Stabilizer");
        gd.addChoice("Transformation:", transformNames, getTransformName(transform));
        gd.addChoice("Maximum_Pyramid_Levels:", pyramidLevelNames, Integer.toString(pyramidLevel));
        gd.addNumericField("Template_Update_Coefficient (0..1):", alpha, 2, 11, null);
        gd.addNumericField("Maximum_Iterations:", maxIter, 0, 11, null);
        gd.addNumericField("Error_Tolerance:", tol, 7, 11, null);
        gd.addCheckbox("Log_Transformation_Coefficients", false);
        if (!stackVirtual)
            gd.addCheckbox("Output_to_a_New_Stack", false);

        gd.showDialog();

        if (gd.wasCanceled())
            return false;
        transform = getTransform(gd.getNextChoice());
        pyramidLevel = Integer.parseInt(gd.getNextChoice());
        alpha = gd.getNextNumber();
        maxIter = (int)gd.getNextNumber();
        tol = gd.getNextNumber();

        if (logEnabled = gd.getNextBoolean()){
            logEditor = new Editor();
            logEditor.display(
                imp.getShortTitle() + ".log",
                "Image Stabilizer Log File for " 
                    + "\"" + imp.getShortTitle() + "\"\n"
                    + transform + "\n"
            );
        }

        if (!stackVirtual)
            outputNewStack = gd.getNextBoolean();
        return true;
    }

    void showProgress(double percent) {
        if (!stackVirtual)
            IJ.showProgress(percent);
    }


    void process(ImageProcessor ipRef,
                 int            firstSlice,
                 int            lastSlice,
                 int            interval,
                 int            tick)
    {
        int width = ipRef.getWidth();
        int height = ipRef.getHeight();
        int stackSize = stack.getSize();

        ImageProcessor ipFloatRef = null;

        if (ipRef instanceof FloatProcessor)
            ipFloatRef = ipRef.duplicate();
        else
            ipFloatRef = ipRef.convertToFloat();

        // workspaces
        ImageProcessor[] ipPyramid = { null, null, null, null, null };
        ImageProcessor[] ipRefPyramid = { null, null, null, null, null };

        ipPyramid[0] = new FloatProcessor(width, height);
        ipRefPyramid[0] = new FloatProcessor(width, height);

        if (pyramidLevel >= 1 && width >= 100 && height >= 100) {
            int width2 = width / 2;
            int height2 = height / 2;
            ipPyramid[1] = new FloatProcessor(width2, height2);
            ipRefPyramid[1] = new FloatProcessor(width2, height2);
            if (pyramidLevel >= 2 && width >= 200 && height >= 200) {
                int width4 = width / 4;
                int height4 = height / 4;
                ipPyramid[2] = new FloatProcessor(width4, height4);
                ipRefPyramid[2] = new FloatProcessor(width4, height4);
                if (pyramidLevel >= 3 && width >= 400 && height >= 400) {
                    int width8 = width / 8;
                    int height8 = height / 8;
                    ipPyramid[3] = new FloatProcessor(width8, height8);
                    ipRefPyramid[3] = new FloatProcessor(width8, height8);
                    if (pyramidLevel >= 4 && width >= 800 && height >= 800) {
                        int width16 = width / 16;
                        int height16 = height / 16;
                        ipPyramid[4] = new FloatProcessor(width16, height16);
                        ipRefPyramid[4] = new FloatProcessor(width16, height16);
                    }
                }
            }
        }

        for (int slice = firstSlice; interval * slice <= interval * lastSlice; slice += interval) {
            if (IJ.escapePressed() || imp.getWindow().isClosed())
                break;

            String label = stack.getSliceLabel(slice);
            if (slice == firstSlice && interval > 0) {
                IJ.showStatus("Skipping " + slice + "/" + stackSize + " ...");
                
                if (transform == TRANSLATION){
                   if (logEditor != null) {
                       logEditor.append(
                           Integer.toString(slice) + "," + 
                           Integer.toString(interval) + "," + 
                           "0,0\n"
                       );
                   }
                }
                else {
                   if (logEditor != null) {
                       logEditor.append(
                           Integer.toString(slice) + "," + 
                           Integer.toString(interval) + "," + 
                           "0,0,0,0,0,0\n"
                       );
                   }
                }
                
                if (stackOut != null)
                    stackOut.addSlice(label, ipRef);
                else {
                    if (!stackVirtual)
                        stack.setPixels(ipRef.getPixels(), slice);
                    else
                        saveImage(ipRef, slice);
                }
                showProgress(tick / (double)stackSize);
                ++tick;
            }
            else {
                IJ.showStatus("Stabilizing " + slice + "/" + stackSize + 
                    " ... (Press 'ESC' to Cancel)");

                ImageProcessor ip = stack.getProcessor(slice);
                ImageProcessor ipFloat = ip.convertToFloat();

                double[][] wp = null;

                if (transform == TRANSLATION){
                    wp = estimateTranslation(
                        ipFloat, ipFloatRef, ipPyramid, ipRefPyramid, maxIter, tol);
                    
                    if (logEnabled) {
                        logEditor.append(
                            Integer.toString(slice) + "," + Integer.toString(interval) + "," +
                            Double.toString(wp[0][0]) + "," +
                            Double.toString(wp[1][0]) +
                            "\n");
                    }
                }
                else {
                    wp = estimateAffine(
                        ipFloat, ipFloatRef, ipPyramid, ipRefPyramid, maxIter, tol);
                    
                    if (logEnabled) {
                        logEditor.append(
                            Integer.toString(slice) + "," + Integer.toString(interval) + "," +
                            Double.toString(wp[0][0]) + "," +
                            Double.toString(wp[0][1]) + "," +
                            Double.toString(wp[0][2]) + "," +
                            Double.toString(wp[1][0]) + "," +
                            Double.toString(wp[1][1]) + "," +
                            Double.toString(wp[1][2]) + "," +
                            "\n");
                    }
                }
                
                FloatProcessor ipFloatOut = new FloatProcessor(width, height);
                
                if (transform == TRANSLATION)
                    warpTranslation(ipFloatOut, ipFloat, wp);
                else
                    warpAffine(ipFloatOut, ipFloat, wp);

                if (ip instanceof ColorProcessor) {
                    ColorProcessor ipColorOut = new ColorProcessor(width, height);

                    if (transform == TRANSLATION)
                        warpColorTranslation(ipColorOut, (ColorProcessor)ip, wp);
                    else
                        warpColorAffine(ipColorOut, (ColorProcessor)ip, wp);

                    if (stackOut == null) {
                        if (!stackVirtual)
                            stack.setPixels(ipColorOut.getPixels(), slice);
                        else
                            saveImage(ipColorOut, slice);
                    }
                    else if (interval < 0)
                        stackOut.addSlice(label, ipColorOut, 0);
                    else
                        stackOut.addSlice(label, ipColorOut);
                }
                else {
                    if (ip instanceof ByteProcessor) {
                        ImageProcessor ipByteOut = ipFloatOut.convertToByte(false);
                        if (stackOut == null) {
                            if (!stackVirtual)
                                stack.setPixels(ipByteOut.getPixels(), slice);
                            else
                                saveImage(ipByteOut, slice);
                        }
                        else if (interval < 0)
                            stackOut.addSlice(label, ipByteOut, 0);
                        else
                            stackOut.addSlice(label, ipByteOut);
                    }
                    else if (ip instanceof ShortProcessor) {
                        ImageProcessor ipShortOut = ipFloatOut.convertToShort(false);
                        if (stackOut == null) {
                            if (!stackVirtual)
                                stack.setPixels(ipShortOut.getPixels(), slice);
                            else
                                saveImage(ipShortOut, slice);
                        }
                        else if (interval < 0)
                            stackOut.addSlice(label, ipShortOut, 0);
                        else
                            stackOut.addSlice(label, ipShortOut);
                    }
                    else {
                        if (stackOut == null) {
                            if (!stackVirtual)
                                stack.setPixels(ipFloatOut.getPixels(), slice);
                            else
                                saveImage(ipFloatOut, slice);
                        }
                        else if (interval < 0)
                            stackOut.addSlice(label, ipFloatOut, 0);
                        else
                            stackOut.addSlice(label, ipFloatOut);
                    }
                }

                combine(ipFloatRef, ipFloatOut);

                showProgress(tick / (double)stackSize);
                ++tick;
            }
        }
    }


    void saveImage(ImageProcessor ip, int slice) {
        VirtualStack virtualStack = (VirtualStack)stack;
        String fileName = null;
        try {
            fileName = virtualStack.getFileName(slice);
        }
        catch (NullPointerException e) {
            // do nothing...
        }
        if (null == fileName || fileName.length() == 0) {
            String title = imp.getTitle();
            String baseName = getBaseName(title);
            Object[] args = { Integer.valueOf(slice) };
            fileName = baseName + String.format("%05d", args) + ".tif";
        }
        else {
            boolean dup = false;
            if (slice > 1 && virtualStack.getFileName(slice - 1) == fileName)
                dup = true; // duplicated name
            String baseName = getBaseName(fileName);
            if (!dup)
                fileName = baseName + ".tif";
            else {
                Object[] args = { Integer.valueOf(slice) };
                fileName = baseName + String.format("%05d", args) + ".tif";
            }
        }
        FileSaver fs = new FileSaver(new ImagePlus(fileName, ip));
        fs.saveAsTiff(outputDir + File.separator + fileName);
    }


    String getBaseName(String fileName) {
        String baseName = fileName;
        int index = fileName.lastIndexOf('.');
        if (index >= 0)
            baseName = fileName.substring(0, index);
        return baseName;
    }


    double[][] estimateAffine(ImageProcessor   ip,
                              ImageProcessor   ipRef,
                              ImageProcessor[] ipPyramid,
                              ImageProcessor[] ipRefPyramid,
                              int              maxIter,
                              double           tol)
    {
        double[][] wp = { {0.0, 0.0, 0.0 }, 
                          {0.0, 0.0, 0.0} };

        // We operate on the gradient magnitude of the image
        //   rather than on the original pixel intensity.
        gradient(ipPyramid[0], ip);
        gradient(ipRefPyramid[0], ipRef);

        if (ipPyramid[4] != null && ipRefPyramid[4] != null) {
            resize(ipPyramid[4], ipPyramid[0]);
            resize(ipRefPyramid[4], ipRefPyramid[0]);
            wp = estimateAffine(
                wp, ipPyramid[4], ipRefPyramid[4], maxIter, tol);
            wp[0][2] *= 16;
            wp[1][2] *= 16;
        }

        if (ipPyramid[3] != null && ipRefPyramid[3] != null) {
            resize(ipPyramid[3], ipPyramid[0]);
            resize(ipRefPyramid[3], ipRefPyramid[0]);
            wp = estimateAffine(
                wp, ipPyramid[3], ipRefPyramid[3], maxIter, tol);
            wp[0][2] *=  8;
            wp[1][2] *=  8;
        }

        if (ipPyramid[2] != null && ipRefPyramid[2] != null) {
            resize(ipPyramid[2], ipPyramid[0]);
            resize(ipRefPyramid[2], ipRefPyramid[0]);
            wp = estimateAffine(
                wp, ipPyramid[2], ipRefPyramid[2], maxIter, tol);
            wp[0][2] *=  4;
            wp[1][2] *=  4;
        }

        if (ipPyramid[1] != null && ipRefPyramid[1] != null) {
            resize(ipPyramid[1], ipPyramid[0]);
            resize(ipRefPyramid[1], ipRefPyramid[0]);
            wp = estimateAffine(
                wp, ipPyramid[1], ipRefPyramid[1], maxIter, tol);
            wp[0][2] *=  2;
            wp[1][2] *=  2;
        }

        wp = estimateAffine(wp, ipPyramid[0], ipRefPyramid[0], maxIter, tol);

        return wp;
    }


    double[][] estimateAffine(double[][]     wp,
                              ImageProcessor ip,
                              ImageProcessor ipRef,
                              int            maxIter,
                              double         tol)
    {
        int width = ip.getWidth();
        int height = ip.getHeight();

        float[] jx = new float[width * height];
        float[] jy = new float[width * height];

        for (int y = 0; y < height; ++y) {
            for (int x = 0; x < width; ++x) {
                jx[y * width + x] = (float)x;
                jy[y * width + x] = (float)y;
            }
        }

        float[][] sd = new float[6][];

        sd[4] = dx(ipRef);
        sd[5] = dy(ipRef);
        sd[0] = dot(sd[4], jx);
        sd[1] = dot(sd[5], jx);
        sd[2] = dot(sd[4], jy);
        sd[3] = dot(sd[5], jy);

        ImageProcessor ipOut = ip.duplicate();

        double[] dp = { 0.0, 0.0, 0.0, 0.0, 0.0, 0.0 };

        double[][] bestWp = new double[2][3];
        bestWp[0][0] = wp[0][0];
        bestWp[0][1] = wp[0][1];
        bestWp[0][2] = wp[0][2];
        bestWp[1][0] = wp[1][0];
        bestWp[1][1] = wp[1][1];
        bestWp[1][2] = wp[1][2];

        double[][] d = { {1.0, 0.0, 0.0},
                         {0.0, 1.0, 0.0},
                         {0.0, 0.0, 1.0} };
        double[][] w = { {1.0, 0.0, 0.0},
                         {0.0, 1.0, 0.0},
                         {0.0, 0.0, 1.0} };

        double[][] h = new double[6][6];

        for (int y = 0; y < 6; ++y) {
            for (int x = 0; x < 6; ++x) {
                h[y][x] = dotSum(sd[x], sd[y]);
            }
        }
        h = invert(h);

        double oldRmse = Double.MAX_VALUE;
        double minRmse = Double.MAX_VALUE;

        for (int iter = 0; iter < maxIter; ++iter) {

            warpAffine(ipOut, ip, wp);

            subtract(ipOut, ipRef);

            double rmse = rootMeanSquare(ipOut);

            if (iter > 0) {
                if (rmse < minRmse) {
                    bestWp[0][0] = wp[0][0];
                    bestWp[0][1] = wp[0][1];
                    bestWp[0][2] = wp[0][2];
                    bestWp[1][0] = wp[1][0];
                    bestWp[1][1] = wp[1][1];
                    bestWp[1][2] = wp[1][2];
                    minRmse      = rmse;
                }
                if (Math.abs((oldRmse - rmse) /
                        (oldRmse + Double.MIN_VALUE)) < tol)
                    break;
            }
            oldRmse = rmse;

            float[] error = (float[])ipOut.getPixels();

            dp[0] = dotSum(sd[0], error);
            dp[1] = dotSum(sd[1], error);
            dp[2] = dotSum(sd[2], error);
            dp[3] = dotSum(sd[3], error);
            dp[4] = dotSum(sd[4], error);
            dp[5] = dotSum(sd[5], error);

            dp = prod(h, dp);

            d[0][0] = dp[0] + 1.0;
            d[0][1] = dp[2];
            d[0][2] = dp[4];
            d[1][0] = dp[1];
            d[1][1] = dp[3] + 1.0;
            d[1][2] = dp[5];
            d[2][0] = 0.0;
            d[2][1] = 0.0;
            d[2][2] = 1.0;

            w[0][0] = wp[0][0] + 1.0;
            w[0][1] = wp[0][1];
            w[0][2] = wp[0][2];
            w[1][0] = wp[1][0];
            w[1][1] = wp[1][1] + 1.0;
            w[1][2] = wp[1][2];
            w[2][0] = 0.0;
            w[2][1] = 0.0;
            w[2][2] = 1.0;

            w = prod(w, invert(d));

            wp[0][0] = w[0][0] - 1.0;
            wp[0][1] = w[0][1];
            wp[0][2] = w[0][2];
            wp[1][0] = w[1][0];
            wp[1][1] = w[1][1] - 1.0;
            wp[1][2] = w[1][2];
        }

        return bestWp;
    }


    double[][] estimateTranslation(ImageProcessor   ip,
                                   ImageProcessor   ipRef,
                                   ImageProcessor[] ipPyramid,
                                   ImageProcessor[] ipRefPyramid,
                                   int              maxIter,
                                   double           tol)
    {
        double[][] wp = { {0.0}, {0.0} };

        // We operate on the gradient magnitude of the image
        //   rather than on the original pixel intensity.
        gradient(ipPyramid[0], ip);
        gradient(ipRefPyramid[0], ipRef);
        
        if (ipPyramid[4] != null && ipRefPyramid[4] != null) {
            resize(ipPyramid[4], ipPyramid[0]);
            resize(ipRefPyramid[4], ipRefPyramid[0]);
            wp = estimateTranslation(
                wp, ipPyramid[4], ipRefPyramid[4], maxIter, tol);
            wp[0][0] *= 16;
            wp[1][0] *= 16;
        }

        if (ipPyramid[3] != null && ipRefPyramid[3] != null) {
            resize(ipPyramid[3], ipPyramid[0]);
            resize(ipRefPyramid[3], ipRefPyramid[0]);
            wp = estimateTranslation(
                wp, ipPyramid[3], ipRefPyramid[3], maxIter, tol);
            wp[0][0] *=  8;
            wp[1][0] *=  8;
        }

        if (ipPyramid[2] != null && ipRefPyramid[2] != null) {
            resize(ipPyramid[2], ipPyramid[0]);
            resize(ipRefPyramid[2], ipRefPyramid[0]);
            wp = estimateTranslation(
                wp, ipPyramid[2], ipRefPyramid[2], maxIter, tol);
            wp[0][0] *=  4;
            wp[1][0] *=  4;
        }

        if (ipPyramid[1] != null && ipRefPyramid[1] != null) {
            resize(ipPyramid[1], ipPyramid[0]);
            resize(ipRefPyramid[1], ipRefPyramid[0]);
            wp = estimateTranslation(
                wp, ipPyramid[1], ipRefPyramid[1], maxIter, tol);
            wp[0][0] *=  2;
            wp[1][0] *=  2;
        }

        wp = estimateTranslation(wp, ipPyramid[0], ipRefPyramid[0], maxIter, tol);

        return wp;
    }


    double[][] estimateTranslation(double[][]     wp,
                                   ImageProcessor ip,
                                   ImageProcessor ipRef,
                                   int            maxIter,
                                   double         tol)
    {
        float[] dxRef = dx(ipRef);
        float[] dyRef = dy(ipRef);

        ImageProcessor ipOut = ip.duplicate();

        double[] dp = { 0.0, 0.0 };

        double[][] bestWp = new double[2][1];
        bestWp[0][0] = wp[0][0];
        bestWp[1][0] = wp[1][0];

        double[][] d = { {1.0, 0.0, 0.0},
                         {0.0, 1.0, 0.0},
                         {0.0, 0.0, 1.0} };
        double[][] w = { {1.0, 0.0, 0.0},
                         {0.0, 1.0, 0.0},
                         {0.0, 0.0, 1.0} };

        double[][] h = new double[2][2];

        h[0][0] = dotSum(dxRef, dxRef);
        h[1][0] = dotSum(dxRef, dyRef);
        h[0][1] = dotSum(dyRef, dxRef);
        h[1][1] = dotSum(dyRef, dyRef);
        h = invert(h);

        double oldRmse = Double.MAX_VALUE;
        double minRmse = Double.MAX_VALUE;

        for (int iter = 0; iter < maxIter; ++iter) {

            warpTranslation(ipOut, ip, wp);

            subtract(ipOut, ipRef);

            double rmse = rootMeanSquare(ipOut);

            if (iter > 0) {
                if (rmse < minRmse) {
                    bestWp[0][0] = wp[0][0];
                    bestWp[1][0] = wp[1][0];
                    minRmse      = rmse;
                }
                if (Math.abs((oldRmse - rmse) /
                        (oldRmse + Double.MIN_VALUE)) < tol)
                    break;
            }
            oldRmse = rmse;

            float[] error = (float[])ipOut.getPixels();

            dp[0] = dotSum(dxRef, error);
            dp[1] = dotSum(dyRef, error);

            dp = prod(h, dp);

            d[0][0] = 1.0; d[0][1] = 0.0; d[0][2] = dp[0];
            d[1][0] = 0.0; d[1][1] = 1.0; d[1][2] = dp[1];
            d[2][0] = 0.0; d[2][1] = 0.0; d[2][2] = 1.0;

            w[0][0] = 1.0; w[0][1] = 0.0; w[0][2] = wp[0][0];
            w[1][0] = 0.0; w[1][1] = 1.0; w[1][2] = wp[1][0];
            w[2][0] = 0.0; w[2][1] = 0.0; w[2][2] = 1.0;

            w = prod(w, invert(d));

            wp[0][0] = w[0][2];
            wp[1][0] = w[1][2];
        }

        return bestWp;
    }
    
    
    void gradient(ImageProcessor ipOut, ImageProcessor ip) {
        int width = ip.getWidth();
        int height = ip.getHeight();
        float[] pixels = (float[])ip.getPixels();
        float[] outPixels = (float[])ipOut.getPixels();

        for (int y = 1; y + 1 < height; ++y) {
            int offset = 1 + y * width;

            //
            // nw---n---ne
            //  |   |   |
            //  w---o---e
            //  |   |   |
            // sw---s---se
            //

            double p1 = 0f;
            double p2 = pixels[offset - width - 1]; // nw
            double p3 = pixels[offset - width];     // n
            double p4 = 0f;                         // ne
            double p5 = pixels[offset - 1];         // w
            double p6 = pixels[offset];             // o
            double p7 = 0f;                         // e
            double p8 = pixels[offset + width - 1]; // sw
            double p9 = pixels[offset + width];     // s

            for (int x = 1; x + 1 < width; ++x) {
                p1 = p2; p2 = p3; p3 = pixels[offset - width + 1];
                p4 = p5; p5 = p6; p6 = pixels[offset + 1];
                p7 = p8; p8 = p9; p9 = pixels[offset + width + 1];
                double a = p1 + 2 * p2 + p3 - p7 - 2 * p8 - p9;
                double b = p1 + 2 * p4 + p7 - p3 - 2 * p6 - p9;
                outPixels[offset++] = (float)Math.sqrt(a * a + b * b);
            }
        }
    }


    void resize(ImageProcessor ipOut, ImageProcessor ip) {
        int widthOut = ipOut.getWidth();
        int heightOut = ipOut.getHeight();
        double xScale = ip.getWidth() / (double)widthOut;
        double yScale = ip.getHeight() / (double)heightOut;
        float[] pixelsOut = (float[])ipOut.getPixels();
        for (int i = 0, y = 0; y < heightOut; ++y) {
            double ys = y * yScale;
            for (int x = 0; x < widthOut; ++x) {
                pixelsOut[i++] = (float)ip.getInterpolatedPixel(x * xScale, ys);
            }
        }
    }
    
    
    double[] prod(double[][] m, double[] v) {
        int n = v.length;
        double[] out = new double[n];
        for (int j = 0; j < n; ++j) {
            out[j] = 0.0;
            for (int i = 0; i < n; ++i)
                out[j] = out[j] + m[j][i] * v[i];
        }
        return out;
    }


    double[][] prod(double[][] a, double[][] b) {
        double[][] out = new double[a.length][b[0].length];
        for (int i = 0; i < a.length; ++i) {
            for (int j = 0; j < b[i].length; ++j) {
                out[i][j] = 0.0;
                for (int k = 0; k < a[i].length; ++k)
                    out[i][j] = out[i][j] + a[i][k] * b[k][j];
            }
        }
        return out;
    }


    float[] dx(ImageProcessor ip) {
        int width = ip.getWidth();
        int height = ip.getHeight();

        float[] pixels = (float[])ip.getPixels();
        float[] outPixels = new float[width * height];

        for (int y = 0; y < height; ++y) {
            // Take forward/backward difference on edges.
            outPixels[y * width] = (float)(pixels[y * width + 1] - pixels[y * width]);
            outPixels[y * width + width - 1] = (float)(pixels[y * width + width - 1]
                                                     - pixels[y * width + width - 2]);

            // Take central difference in interior.
            for (int x = 1; x + 1 < width; ++x) {
                outPixels[y * width + x] = (float)((pixels[y * width + x + 1] -
                                                    pixels[y * width + x - 1]) * 0.5);
            } // x
        } // y

        return outPixels;
    }


    float[] dy(ImageProcessor ip) {
        int width = ip.getWidth();
        int height = ip.getHeight();

        float[] pixels = (float[])ip.getPixels();
        float[] outPixels = new float[width * height];

        for (int x = 0; x < (int)width; ++x) {
            // Take forward/backward difference on edges.
            outPixels[x] = (float)(pixels[width + x] - pixels[x]);
            outPixels[(height - 1) * width + x] = (float)(pixels[width * (height - 1) + x]
                                                        - pixels[width * (height - 2) + x]);

            // Take central difference in interior.
            for (int y = 1; y + 1 < (int)height; ++y) {
                outPixels[y * width + x] = (float)((pixels[width * (y + 1) + x] -
                                                    pixels[width * (y - 1) + x]) * 0.5);
            } // y
        } // x

        return outPixels;
    }


    float[] dot(float[] p1, float[] p2) {
        int n = p1.length < p2.length ? p1.length : p2.length;
        float[] output = new float[n];
        for (int i = 0; i < n; ++i)
            output[i] = p1[i] * p2[i];
        return output;
    }


    double dotSum(float[] p1, float[] p2) {
        double sum = 0.0;
        int n = p1.length < p2.length ? p1.length : p2.length;
        for (int i = 0; i < n; ++i)
            sum += p1[i] * p2[i];
        return sum;
    }

    /**
        Gaussian elimination (required by invert).

        This Java program is part of the book, "An Introduction to Computational
        Physics, 2nd Edition,"  written by Tao  Pang and published  by Cambridge
        University Press on January 19, 2006.
    */
    void gaussian(double a[][], int index[]) {

        int n = index.length;
        double[] c = new double[n];

        // Initialize the index
        for (int i = 0; i < n; ++i) index[i] = i;

        // Find the rescaling factors, one from each row
        for (int i = 0; i < n; ++i) {
            double c1 = 0;
            for (int j = 0; j < n; ++j) {
                double c0 = Math.abs(a[i][j]);
                if (c0 > c1) c1 = c0;
            }
            c[i] = c1;
        }

        // Search the pivoting element from each column
        int k = 0;
        for (int j = 0; j < n-1; ++j) {
            double pi1 = 0;
            for (int i = j; i < n; ++i) {
                double pi0 = Math.abs(a[index[i]][j]);
                pi0 /= c[index[i]];
                if (pi0 > pi1) {
                    pi1 = pi0;
                    k = i;
                }
            }

            // Interchange rows according to the pivoting order
            int itmp = index[j];
            index[j] = index[k];
            index[k] = itmp;
            for (int i= j + 1; i < n; ++i) {
                double pj = a[index[i]][j] / a[index[j]][j];
                // Record pivoting ratios below the diagonal
                a[index[i]][j] = pj;
                // Modify other elements accordingly
                for (int l = j + 1; l < n; ++l)
                    a[index[i]][l] -= pj * a[index[j]][l];
            }
        }
    }


    /**
        Matrix inversion with the Gaussian elimination scheme.

        This Java program is part of the book, "An Introduction to Computational
        Physics, 2nd Edition,"  written by Tao  Pang and published  by Cambridge
        University Press on January 19, 2006.
    */
    double[][] invert(double a[][]) {
        int n = a.length;
        double[][] x = new double[n][n];
        double[][] b = new double[n][n];
        int index[] = new int[n];
        for (int i = 0; i < n; ++i) b[i][i] = 1;

            // Transform the matrix into an upper triangle
            gaussian(a, index);

            // Update the matrix b[i][j] with the ratios stored
            for (int i = 0; i < n - 1; ++i)
                for (int j = i + 1; j < n; ++j)
                    for (int k = 0; k < n; ++k)
                        b[index[j]][k] -= a[index[j]][i] * b[index[i]][k];

                    // Perform backward substitutions
                    for (int i = 0; i < n; ++i) {
                        x[n - 1][i] = b[index[n - 1]][i] / a[index[n - 1]][n - 1];
                        for (int j = n - 2; j >= 0; --j) {
                            x[j][i] = b[index[j]][i];
                            for (int k = j + 1; k < n; ++k) {
                                x[j][i] -= a[index[j]][k] * x[k][i];
                }
                x[j][i] /= a[index[j]][j];
            }
        }
        return x;
    }


    double rootMeanSquare(ImageProcessor ip) {
        double mean = 0.0;
        float[] pixels = (float[])ip.getPixels();
        for (int i = 0; i < pixels.length; ++i)
            mean += pixels[i] * pixels[i];
        mean /= pixels.length;
        return Math.sqrt(mean);
    }


    void combine(ImageProcessor ipOut, ImageProcessor ip) {
        float[] pixels = (float[])ip.getPixels();
        float[] outPixels = (float[])ipOut.getPixels();
        double beta = 1.0 - alpha;
        for (int i = 0; i < pixels.length; ++i) {
            if (pixels[i] != 0)
                outPixels[i] = (float)(alpha * outPixels[i] + beta * pixels[i]);
        }
    }


    void subtract(ImageProcessor ipOut, ImageProcessor ip) {
        float[] pixels = (float[])ip.getPixels();
        float[] outPixels = (float[])ipOut.getPixels();
        for (int i = 0; i < pixels.length; ++i)
            outPixels[i] = outPixels[i] - pixels[i];
    }


    void warpAffine(ImageProcessor ipOut,
                    ImageProcessor ip,
                    double[][]     wp)
    {
        float[] outPixels = (float[])ipOut.getPixels();
        int width = ipOut.getWidth();
        int height = ipOut.getHeight();
        for (int p = 0, y = 0; y < height; ++y) {
            for (int x = 0; x < width; ++x) {
                double xx = (1.0 + wp[0][0]) * x + wp[0][1] * y + wp[0][2];
                double yy = wp[1][0] * x + (1.0 + wp[1][1]) * y + wp[1][2];
                outPixels[p] = (float)ip.getInterpolatedPixel(xx, yy);
                ++p;
            } // x
        } // y
    }


    void warpColorAffine(ImageProcessor ipOut,
                         ColorProcessor ip,
                         double[][]     wp)
    {
        int[] outPixels = (int[])ipOut.getPixels();
        int width = ipOut.getWidth();
        int height = ipOut.getHeight();
        for (int p = 0, y = 0; y < height; ++y) {
            for (int x = 0; x < width; ++x) {
                double xx = (1.0 + wp[0][0]) * x + wp[0][1] * y + wp[0][2];
                double yy = wp[1][0] * x + (1.0 + wp[1][1]) * y + wp[1][2];
                outPixels[p] = (int)ip.getInterpolatedRGBPixel(xx, yy);
                ++p;
            } // x
        } // y
    }


    void warpTranslation(ImageProcessor ipOut,
                         ImageProcessor ip,
                         double[][]     wp)
    {
        float[] outPixels = (float[])ipOut.getPixels();
        int width = ipOut.getWidth();
        int height = ipOut.getHeight();
        for (int p = 0, y = 0; y < height; ++y) {
            for (int x = 0; x < width; ++x) {
                double xx = x + wp[0][0];
                double yy = y + wp[1][0];
                outPixels[p] = (float)ip.getInterpolatedPixel(xx, yy);
                ++p;
            } // x
        } // y
    }


    void warpColorTranslation(ImageProcessor ipOut,
                              ColorProcessor ip,
                              double[][]     wp)
    {
        int[] outPixels = (int[])ipOut.getPixels();
        int width = ipOut.getWidth();
        int height = ipOut.getHeight();
        for (int p = 0, y = 0; y < height; ++y) {
            for (int x = 0; x < width; ++x) {
                outPixels[p] = (int)ip.getInterpolatedRGBPixel(x + wp[0][0], y + wp[1][0]);
                ++p;
            } // x
        } // y
    }
}