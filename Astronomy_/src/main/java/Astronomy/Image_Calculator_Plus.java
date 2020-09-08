package Astronomy;// Image_Calculator_Plus.java

import ij.*;
import ij.gui.*;
import ij.plugin.*;
import ij.process.*;
import ij.text.*;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.*;

/**
 * Works like the standard ImageJ Image Calculator, but the images can be of different sizes,
 * and the secondary image can be multiplied by a factor and/or shifted in pixel position.
 */
public class Image_Calculator_Plus implements PlugIn
	{
	ImagePlus img1,img2,img3;
	ImageProcessor imp1,imp2,imp3;
	String[] functions = new String[] {"+","-","*","/"};
	String func = "+";
	String name;
	double scale = 1.0;
	double xShift,yShift;

	/**
	 * Gets a list of images, sets up the dialog, instantiates the images, lets the operations
	 * be performed, and then displays the results.
	 */
	public void run(String args)
		{
		// GET LIST OF IMAGES

		int[] idList = WindowManager.getIDList();
		int n = idList.length;
		if (idList == null || n < 1)
			{
			IJ.error("No images are open!");
			return;
			}
		String[] imageList = new String[n];
		for (int i=0; i < n; i++)
			{
			ImagePlus im = WindowManager.getImage(idList[i]);
			imageList[i] = im.getShortTitle();
			}

		// RUN DIALOGUE

		GenericDialog gd = new GenericDialog ("Image Calculator Plus");

		gd.addChoice("Image #1",imageList,imageList[0]);
		gd.addChoice("",functions,functions[0]);
		if (n == 1)
			gd.addChoice("Image #2",imageList,imageList[0]);
		else
			gd.addChoice("Image #2",imageList,imageList[1]);
		gd.addStringField("=","ImageCalculatorPlusImage",20);
		gd.addStringField("Scaling for image #2","1.0",20);
		gd.addStringField("X-shift of image #2 [pixels]","0.0",20);
		gd.addStringField("Y-shift of image #2 [pixels]","0.0",20);
		gd.addMessage ("You can use simple arithmetic expressions for the scaling and shifts");
		gd.addMessage ("(e.g. dx=x1-x2, scale=sum1/sum2) but avoid the use of unnecessary +/- signs!");
		gd.showDialog();
		if (gd.wasCanceled()) return;

		int choice1 = gd.getNextChoiceIndex();
		func = gd.getNextChoice();
		int choice2 = gd.getNextChoiceIndex();

		String title = gd.getNextString();

		String s = gd.getNextString();
		scale = getScale(s);
		s = gd.getNextString();
		xShift = getScale(s);
		s = gd.getNextString();
		yShift = getScale(s);
		if (Double.isNaN(scale) || Double.isNaN(xShift) || Double.isNaN(yShift))
			{
			IJ.error("Could not parse strings with arithmetic.");
			return;
			}
 

		img1 = WindowManager.getImage(idList[choice1]);
		img2 = WindowManager.getImage(idList[choice2]);
		if (img1 == null || img2 == null)
			{
			IJ.error("Could not instantiate the input images!");
			return;
			}
		if (img1.getImageStackSize() != 1 || img2.getImageStackSize() != 1)
			{
			IJ.error("Sorry - this only works for non-stacks!");
			return;
			}
		img3 = img1.createImagePlus ();
		if (img3 == null)
			{
			IJ.error("Could not instantiate the output image!");
			return;
			}
		img3.setProcessor (title, new FloatProcessor(img1.getWidth(),img1.getHeight()));

		imp1 = img1.getProcessor();
		imp2 = img2.getProcessor();
		imp3 = img3.getProcessor();

		arithmetic();
		img3.show();
		}

	/**
	 * Performs the actual image arithmetic, including the scaling of the secondary image and the pixel shifts.
	 */
	protected void arithmetic ()
		{
		int w1 = img1.getWidth();
		int h1 = img1.getHeight();
		int w2 = img2.getWidth();
		int h2 = img2.getHeight();

		double x1,x2,y1,y2,val1,val2;
		double val3=0.0;
		int oops=0;
		int i2,j2;
		for (int j1=0; j1 < h1; j1++)
			{
			y1 = (double)j1;
			y2 = y1-yShift;
			for (int i1=0; i1 < w1; i1++)
				{
				x1 = (double)i1;
				x2 = x1-xShift;
				i2 = (int)(x2+0.5);
				j2 = (int)(y2+0.5);
				val1 = imp1.getPixelValue(i1,j1);
				val2 = imp2.getPixelValue(i2,j2);	// Interpolated(x2,y2);
				if (! Double.isNaN(val2))
					{
					if (func.equals("+"))
						val3 = val1+scale*val2;
					else if (func.equals("-"))
						val3 = val1-scale*val2;
					else if (func.equals("*"))
						val3 = val1*scale*val2;
					else if (func.equals("/"))
						val3 = val1/(scale*val2);
					if (oops < 100 && (Math.abs(val3) < 1.e-12 || Math.abs(val3) > 1.e12))
						{
						oops++;
						IJ.log("i1,j1="+i1+","+j1+" too big/small! vals="+val1+","+val2+","+val3);
						}
					imp3.putPixelValue(i1,j1,val3);
					}
				}
			}
		}

	/**
	 * Parses a simple arithmetic expression using a single +,-,*, or / operator on two numbers.
	*/
	protected double getScale (String s)
		{
		double d1=0.0;
		double d2=0.0;
		try	{
			String[] parts = s.split("\\*");
			if (parts.length > 1)
				{
				d1 = Double.parseDouble(parts[0]);
				d2 = Double.parseDouble(parts[1]);
				return d1*d2;
				}
			parts = s.split("\\/");
			if (parts.length > 1)
				{
				d1 = Double.parseDouble(parts[0]);
				d2 = Double.parseDouble(parts[1]);
				return d1/d2;
				}
			parts = s.split("\\+");
			if (parts.length > 1)
				{
				d1 = Double.parseDouble(parts[0]);
				d2 = Double.parseDouble(parts[1]);
				return d1+d2;
				}
			parts = s.split("\\-");
			if (parts.length > 1)
				{
				if (parts.length == 3)  // E.G.   "-1.2 - 3.4"
					{
					d1 = -Double.parseDouble(parts[1]);
					d2 = Double.parseDouble(parts[2]);
					}
				else	{
					d1 = Double.parseDouble(parts[0]);
					d2 = Double.parseDouble(parts[1]);
					}
				return d1-d2;
				}
			else
				return Double.parseDouble(s);
			}
		catch (NumberFormatException e)
			{
			return Double.NaN;
			}
		}
	}
