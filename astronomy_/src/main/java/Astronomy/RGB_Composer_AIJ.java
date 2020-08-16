package Astronomy;// RGB_Composer_AIJ.java

import ij.*;
import ij.gui.*;
import ij.plugin.*;
import ij.process.*;
import ij.WindowManager;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.text.*;
import java.util.*;
import javax.swing.*;
import javax.swing.border.*;

/**
 * @author F.V. Hessman, Georg-August-Universitaet Goettingen
 * @version 1.0
 * @date 2008-JUL-22
 *
 * @author FVH
 * @date 2008-JUL-31
 * @version 1.1
 * @changes Added pixel shifting
 *
 * @author FVH
 * @date 2011-Apr-01
 * @version 1.2
 * @changes Removed astroj linkage to make it general-purpose.
 * 
 *  @author KAC
 * @date 2014-Dec-14
 * @version 1.3
 * @changes Modified for AIJ compatibility.
 */
public class RGB_Composer_AIJ implements ActionListener, PlugIn
	{
	DecimalFormat ef,ff;

	// UI
	JFrame frame = null;
	JTextField rMinText = null;
	JTextField rMaxText = null;
	JTextField gMinText = null;
	JTextField gMaxText = null;
	JTextField bMinText = null;
	JTextField bMaxText = null;

	JComboBox rImages = null;
	JComboBox gImages = null;
	JComboBox bImages = null;

	JButton rUp,rDown,rLeft,rRight,rBroader,rNarrower,rWeaker,rStronger,rUpdate,rAuto,rLift1,rLift2,rDrop1,rDrop2;
	JButton gUp,gDown,gLeft,gRight,gBroader,gNarrower,gWeaker,gStronger,gUpdate,gAuto,gLift1,gLift2,gDrop1,gDrop2;
	JButton bUp,bDown,bLeft,bRight,bBroader,bNarrower,bWeaker,bStronger,bUpdate,bAuto,bLift1,bLift2,bDrop1,bDrop2;

	static int VERTICAL = BoxLayout.PAGE_AXIS;
	static int HORIZONTAL = BoxLayout.LINE_AXIS;

    static int NONE = 0;
	static int RED = 1;
	static int GREEN = 2;
	static int BLUE = 3;
    static int ALL = 4;

	ImagePlus red=null;
	ImagePlus green=null;
	ImagePlus blue=null;
	ImagePlus rgb=null;
	int w,h,bits;

	String[] list = null;
	int nlist=0;
	String rImage = null;
	String gImage = null;
	String bImage = null;

	static String title = "RGB-Composed Image";

	int rXShift=0, rYShift=0;
	int gXShift=0, gYShift=0;
	int bXShift=0, bYShift=0;
	int dShift=1;
	
	/**
	 * Standard ImageJ PlugIn method which creates and runs the Swing interface.
	 */
	public void run (String arg)
		{
		initialize();
		if (list != null && red != null)
			createAndRunGUI();
		}

	void initialize()
		{
		// GET LIST OF DISPLAYED NON-COLOR IMAGES

		list = getImages();
		if (list == null) return;

 		// GET INPUT IMAGES

		if (red == null)
			{
			rImage = nextImage();
			red   = WindowManager.getImage(rImage);
			}
		if (green == null)
			{
			gImage = nextImage();
			green = WindowManager.getImage(gImage);
			}
		if (blue == null)
			{
			bImage = nextImage();
			blue  = WindowManager.getImage(bImage);
			}

		// INITIALIZE COMBO BOXES

		if (rImages == null)
			{
			rImages = new JComboBox(list);
			gImages = new JComboBox(list);
			bImages = new JComboBox(list);
			}
		else
			replaceItems();

		if (red == null)
			{
			IJ.error("Cannot load image "+rImage);
			return;
			}
		}

	/**
	 * Make sure the images fit together and are grayscale.
	 */
	boolean checkImages()
		{
		if (red == null)
			{
			IJ.error("Cannot access red image "+rImage);
			initialize();
			return false;
			}
		if (green == null)
			{
			IJ.error("Cannot access green image "+gImage);
			initialize();
			return false;
			}
		if (blue == null)
			{
			IJ.error("Cannot access blue image "+bImage);
			initialize();
			return false;
			}

		// MAKE SURE THE IMAGES ARE STILL THERE

		ImageProcessor ip = red.getProcessor();
		if (ip == null)
			{
			IJ.error("Cannot access red image "+rImage);
			return false;
			}
		ip = green.getProcessor();
		if (ip == null)
			{
			IJ.error("Cannot access green image "+gImage);
			return false;
			}
		ip = blue.getProcessor();
		if (ip == null)
			{
			IJ.error("Cannot access blue image "+bImage);
			return false;
			}

		// CHECK TO MAKE SURE THEY MATCH

		w = red.getWidth();
		h = red.getHeight();
		bits = red.getBitDepth();
		if (bits == 24)
			{
			IJ.error("The red image is already color!");
			return false;
			}
		if (green.getWidth() != w || green.getHeight() != h || green.getBitDepth() != bits)
			{
			IJ.error("The green image doesn't match red image!");
			return false;
			}
		if (blue.getWidth() != w || blue.getHeight() != h || blue.getBitDepth() != bits)
			{
			IJ.error("The blue image doesn't match red image!");
			return false;
			}

		// GET RESULTING IMAGE

		rgb  = WindowManager.getImage(title);
		if (rgb == null)
			{
			IJ.newImage (title,"RGB",w,h,1);
			rgb  = WindowManager.getImage(title);
			if (rgb == null)
				{
				IJ.error("Cannot open rgb image!");
				return false;
				}
			}
		else if (rgb.getWidth() != w || rgb.getHeight() != h)
			{
			IJ.error("Present RGB image has the wrong size!");
			return false;
			}

		return true;
		}

	/**
	 * Returns an array of non-color image names.
	 */
	String[] getImages()
		{
		int nduds=0;
		String[] str = openImages(null);
		if (str.length == 0)
			{
			IJ.error("No images to be composed!");
			return null;
			}
		for (int i=0; i < str.length; i++)
			{
			ImagePlus im = WindowManager.getImage(str[i]);
			if (im == null || im.getBitDepth() == 24)
				{
				str[i]=null;
				nduds++;
				}
			}
		if (nduds == str.length) return null;
		nlist=0;
		return str;
		}

	/**
	 * Returns the next non-color image in the image list.
	 */
	String nextImage()
		{
		String s;
		do	{
			if (nlist >= list.length) nlist=0;
			s = list[nlist];
			nlist++;
			}
		while(s == null);
		return s;
		}

	/**
	 * Returns the index of a given image name in the image list.
	 */
	int imageIndex(String name)
		{
		for (int i=0; i < list.length; i++)
			{
			if (list[i] != null && name.equals(list[i]))
				return i;
			}
		return -1;
		}

	/**
	 * Creates the dynamic GUI using Swing objects.
	 */
	protected void createAndRunGUI ()
		{
		JPanel p,panel;

		ef = new DecimalFormat("#0.000E00");
		ff = new DecimalFormat("#0.000");

		frame = new JFrame ("RGB Composer");
			JPanel rgbPanel = simpleJPanel (HORIZONTAL);

				panel = simpleJPanel (VERTICAL);
					panel.setBackground(Color.red);

					rImages.setSelectedIndex(imageIndex(rImage));
					rImages.addActionListener(this);
					panel.add(rImages);

					p = simpleJPanel (HORIZONTAL,0,5,0,5);
						p.setBackground(Color.red);
						p.add(whiteLabel("Lower:"));
						rMinText = new JTextField(getMin(red),5);
						p.add(rMinText);
						p.add(whiteLabel("=000 in R"));
					panel.add(p);
					p = simpleJPanel (HORIZONTAL,0,5,0,5);
						p.setBackground(Color.red);
						p.add(whiteLabel("Upper:"));
						rMaxText = new JTextField(getMax(red),5);
						p.add(rMaxText);
						p.add(whiteLabel("=255 in R"));
					panel.add(p);
					p = simpleJPanel (HORIZONTAL,0,5,0,5);
						p.setBackground(Color.red);
						rAuto = new JButton(" Auto ");
						rAuto.addActionListener(this);
						p.add(rAuto);
						rUpdate = new JButton("Update");
						rUpdate.addActionListener(this);
						p.add(rUpdate);
					panel.add(p);

					p = simpleJPanel (HORIZONTAL,5,5,0,5);
						p.setBackground(Color.red);
						p.add(whiteLabel("Brightness:"));
						rWeaker = new JButton("<<");
						rWeaker.addActionListener(this);
						p.add(rWeaker);
						rStronger = new JButton(">>");
						rStronger.addActionListener(this);
						p.add(rStronger);
					panel.add(p);
					p = simpleJPanel (HORIZONTAL,0,5,0,5);
						p.setBackground(Color.red);
						p.add(whiteLabel("Contrast   :"));
						rBroader = new JButton("<>");
						rBroader.addActionListener(this);
						p.add(rBroader);
						rNarrower = new JButton("><");
						rNarrower.addActionListener(this);
						p.add(rNarrower);
					panel.add(p);
					p = simpleJPanel (HORIZONTAL,0,5,0,5);
						p.setBackground(Color.red);
						p.add(whiteLabel("Upper     :"));
						rLift2 = new JButton("|>");
						rLift2.addActionListener(this);
						p.add(rLift2);
						rDrop2= new JButton("|<");
						rDrop2.addActionListener(this);
						p.add(rDrop2);
					panel.add(p);
					p = simpleJPanel (HORIZONTAL,0,5,0,5);
						p.setBackground(Color.red);
						p.add(whiteLabel("Lower     :"));
						rDrop1= new JButton("<|");
						rDrop1.addActionListener(this);
						p.add(rDrop1);
						rLift1= new JButton(">|");
						rLift1.addActionListener(this);
						p.add(rLift1);
					panel.add(p);

					p = simpleJPanel (HORIZONTAL,5,5,0,5);
						p.setBackground(Color.red);
						rUp = new JButton("up");
						rUp.addActionListener(this);
						p.add(rUp);
					panel.add(p);
					p = simpleJPanel (HORIZONTAL,0,5,0,5);
						p.setBackground(Color.red);
						rLeft = new JButton("left");
						rLeft.addActionListener(this);
						p.add(rLeft);
						rRight = new JButton("right");
						rRight.addActionListener(this);
						p.add(rRight);
					panel.add(p);
					p = simpleJPanel (HORIZONTAL,0,5,0,5);
						p.setBackground(Color.red);
						rDown = new JButton("down");
						rDown.addActionListener(this);
						p.add(rDown);
					panel.add(p);

				rgbPanel.add(panel);
				panel = simpleJPanel (VERTICAL);
					panel.setBackground(Color.green);

					gImages.setSelectedIndex(imageIndex(gImage));
					gImages.addActionListener(this);
					panel.add(gImages);

					p = simpleJPanel (HORIZONTAL,0,5,0,5);
						p.setBackground(Color.green);
						p.add(new JLabel("Lower:"));
						gMinText = new JTextField(getMin(green),5);
						p.add(gMinText);
						p.add(new JLabel("=000 in G"));
					panel.add(p);
					p = simpleJPanel (HORIZONTAL,0,5,0,5);
						p.setBackground(Color.green);
						p.add(new JLabel("Upper:"));
						gMaxText = new JTextField(getMax(green),5);
						p.add(gMaxText);
						p.add(new JLabel("=255 in G"));
					panel.add(p);
					p = simpleJPanel (HORIZONTAL,0,5,0,5);
						p.setBackground(Color.green);
						gAuto = new JButton(" Auto ");
						gAuto.addActionListener(this);
						p.add(gAuto);
						gUpdate = new JButton("Update");
						gUpdate.addActionListener(this);
						p.add(gUpdate);
					panel.add(p);

					p = simpleJPanel (HORIZONTAL,5,5,0,5);
						p.setBackground(Color.green);
						p.add(new JLabel("Brightness:"));
						gWeaker = new JButton("<<");
						gWeaker.addActionListener(this);
						p.add(gWeaker);
						gStronger = new JButton(">>");
						gStronger.addActionListener(this);
						p.add(gStronger);
					panel.add(p);
					p = simpleJPanel (HORIZONTAL,0,5,0,5);
						p.setBackground(Color.green);
						p.add(new JLabel("Contrast   :"));
						gBroader = new JButton("<>");
						gBroader.addActionListener(this);
						p.add(gBroader);
						gNarrower = new JButton("><");
						gNarrower.addActionListener(this);
						p.add(gNarrower);
					panel.add(p);
					p = simpleJPanel (HORIZONTAL,0,5,0,5);
						p.setBackground(Color.green);
						p.add(new JLabel("Upper     :"));
						gLift2 = new JButton("|>");
						gLift2.addActionListener(this);
						p.add(gLift2);
						gDrop2 = new JButton("|<");
						gDrop2.addActionListener(this);
						p.add(gDrop2);
					panel.add(p);
					p = simpleJPanel (HORIZONTAL,0,5,0,5);
						p.setBackground(Color.green);
						p.add(new JLabel("Lower     :"));
						gDrop1 = new JButton("<|");
						gDrop1.addActionListener(this);
						p.add(gDrop1);
						gLift1 = new JButton(">|");
						gLift1.addActionListener(this);
						p.add(gLift1);
					panel.add(p);

					p = simpleJPanel (HORIZONTAL,5,5,0,5);
						p.setBackground(Color.green);
						gUp = new JButton("up");
						gUp.addActionListener(this);
						p.add(gUp);
					panel.add(p);
					p = simpleJPanel (HORIZONTAL,0,5,0,5);
						p.setBackground(Color.green);
						gLeft = new JButton("left");
						gLeft.addActionListener(this);
						p.add(gLeft);
						gRight = new JButton("right");
						gRight.addActionListener(this);
						p.add(gRight);
					panel.add(p);
					p = simpleJPanel (HORIZONTAL,0,5,0,5);
						p.setBackground(Color.green);
						gDown = new JButton("down");
						gDown.addActionListener(this);
						p.add(gDown);
					panel.add(p);

				rgbPanel.add(panel);
				panel = simpleJPanel (VERTICAL);
					panel.setBackground(Color.blue);

					bImages.setSelectedIndex(imageIndex(bImage));
					bImages.addActionListener(this);
					panel.add(bImages);

					p = simpleJPanel (HORIZONTAL,0,5,0,5);
						p.setBackground(Color.blue);
						p.add(whiteLabel("Lower:"));
						bMinText = new JTextField(getMin(blue),5);
						p.add(bMinText);
						p.add(whiteLabel("=000 in B"));
					panel.add(p);
					p = simpleJPanel (HORIZONTAL,0,5,0,5);
						p.setBackground(Color.blue);
						p.add(whiteLabel("Upper:"));
						bMaxText = new JTextField(getMax(blue),5);
						p.add(bMaxText);
						p.add(whiteLabel("=255 in B"));
					panel.add(p);
					p = simpleJPanel (HORIZONTAL,0,5,0,5);
						p.setBackground(Color.blue);
						bAuto = new JButton(" Auto ");
						bAuto.addActionListener(this);
						p.add(bAuto);
						bUpdate = new JButton("Update");
						bUpdate.addActionListener(this);
						p.add(bUpdate);
					panel.add(p);

					p = simpleJPanel (HORIZONTAL,5,5,0,5);
						p.setBackground(Color.blue);
						p.add(whiteLabel("Brightness:"));
						bWeaker = new JButton("<<");
						bWeaker.addActionListener(this);
						p.add(bWeaker);
						bStronger = new JButton(">>");
						bStronger.addActionListener(this);
						p.add(bStronger);
					panel.add(p);
					p = simpleJPanel (HORIZONTAL,0,5,0,5);
						p.setBackground(Color.blue);
						p.add(whiteLabel("Contrast   :"));
						bBroader = new JButton("<>");
						bBroader.addActionListener(this);
						p.add(bBroader);
						bNarrower = new JButton("><");
						bNarrower.addActionListener(this);
						p.add(bNarrower);
					panel.add(p);
					p = simpleJPanel (HORIZONTAL,0,5,0,5);
						p.setBackground(Color.blue);
						p.add(whiteLabel("Upper     :"));
						bLift2 = new JButton("|>");
						bLift2.addActionListener(this);
						p.add(bLift2);
						bDrop2 = new JButton("|<");
						bDrop2.addActionListener(this);
						p.add(bDrop2);
					panel.add(p);
					p = simpleJPanel (HORIZONTAL,0,5,0,5);
						p.setBackground(Color.blue);
						p.add(whiteLabel("Lower     :"));
						bDrop1 = new JButton("<|");
						bDrop1.addActionListener(this);
						p.add(bDrop1);
						bLift1 = new JButton(">|");
						bLift1.addActionListener(this);
						p.add(bLift1);
					panel.add(p);

					p = simpleJPanel (HORIZONTAL,5,5,0,5);
						p.setBackground(Color.blue);
						bUp = new JButton("up");
						bUp.addActionListener(this);
						p.add(bUp);
					panel.add(p);
					p = simpleJPanel (HORIZONTAL,0,5,0,5);
						p.setBackground(Color.blue);
						bLeft = new JButton("left");
						bLeft.addActionListener(this);
						p.add(bLeft);
						bRight = new JButton("right");
						bRight.addActionListener(this);
						p.add(bRight);
					panel.add(p);
					p = simpleJPanel (HORIZONTAL,0,5,0,5);
						p.setBackground(Color.blue);
						bDown = new JButton("down");
						bDown.addActionListener(this);
						p.add(bDown);
					panel.add(p);

				rgbPanel.add(panel);

		frame.add(rgbPanel);

		frame.pack();
		frame.setResizable (false);
		frame.setVisible (true);

		if (!checkImages()) return;
		compose(RED);
		compose(GREEN);
		compose(BLUE);
        refresh(rgb, ALL);
		}

	// ACTION LISTENER METHOD

	/**
	 * ActionListener method which catches pull-downs and buttons.
	 */
	public void actionPerformed (ActionEvent e)
		{
        int color = 0;
		String cmd = e.getActionCommand();
		Object obj = e.getSource();
		if (cmd.equals("Update"))
			{
			if (!checkImages()) return;
			if (obj == rUpdate)
                {
                color=RED;
				compose(RED);
                }
			else if (obj == gUpdate)
                {
                color=GREEN;
				compose(GREEN);
                }
			else if (obj == bUpdate)
                {
                color=BLUE;
				compose(BLUE);
                }
			refresh(rgb, color);
			}
		else if (cmd.equals(" Auto "))
			{
			if (!checkImages()) return;
			if (obj == rAuto)
				{
                color=RED;
				auto(RED);
				compose(RED);
				}
			else if (obj == gAuto)
				{
                color=GREEN;
				auto(GREEN);
				compose(GREEN);
				}
			else if (obj == bAuto)
				{
                color=BLUE;
				auto(BLUE);
				compose(BLUE);
				}
			refresh(rgb, color);
			}
		else if (cmd.equals("right"))
			{
			if (!checkImages()) return;
			if (obj == rRight)
				{
				rXShift -= 1;
				compose(RED);
				}
			else if (obj == gRight)
				{
				gXShift -= 1;
				compose(GREEN);
				}
			else if (obj == bRight)
				{
				bXShift -= 1;
				compose(BLUE);
				}
			refresh(rgb, NONE);
			return;
			}
		else if (cmd.equals("left"))
			{
			if (!checkImages()) return;
			if (obj == rLeft)
				{
				rXShift += 1;
				compose(RED);
				}
			else if (obj == gLeft)
				{
				gXShift += 1;
				compose(GREEN);
				}
			else if (obj == bLeft)
				{
				bXShift += 1;
				compose(BLUE);
				}
			refresh(rgb, NONE);
			return;
			}
		else if (cmd.equals("up"))
			{
			if (!checkImages()) return;
			if (obj == rUp)
				{
				rYShift += 1;
				compose(RED);
				}
			else if (obj == gUp)
				{
				gYShift += 1;
				compose(GREEN);
				}
			else if (obj == bUp)
				{
				bYShift += 1;
				compose(BLUE);
				}
			refresh(rgb, NONE);
			return;
			}
		else if (cmd.equals("down"))
			{
			if (!checkImages()) return;
			if (obj == rDown)
				{
				rYShift -= 1;
				compose(RED);
				}
			else if (obj == gDown)
				{
				gYShift -= 1;
				compose(GREEN);
				}
			else if (obj == bDown)
				{
				bYShift -= 1;
				compose(BLUE);
				}
			refresh(rgb, NONE);
			return;
			}

		else if (cmd.equals("<<"))
			{
			if      (obj == rWeaker)
				contrastBrightness (rMinText,rMaxText,RED,-1.0,-1.0);
			else if (obj == gWeaker)
				contrastBrightness (gMinText,gMaxText,GREEN,-1.0,-1.0);
			else if (obj == bWeaker)
				contrastBrightness (bMinText,bMaxText,BLUE,-1.0,-1.0);
			}
		else if (cmd.equals(">>"))
			{
			if      (obj == rStronger)
				contrastBrightness (rMinText,rMaxText,RED,+1.0,+1.0);
			else if (obj == gStronger)
				contrastBrightness (gMinText,gMaxText,GREEN,+1.0,+1.0);
			else if (obj == bStronger)
				contrastBrightness (bMinText,bMaxText,BLUE,+1.0,+1.0);
			}
		else if (cmd.equals("><"))
			{
			if      (obj == rNarrower)
				contrastBrightness (rMinText,rMaxText,RED,+1.0,-1.0);
			else if (obj == gNarrower)
				contrastBrightness (gMinText,gMaxText,GREEN,+1.0,-1.0);
			else if (obj == bNarrower)
				contrastBrightness (bMinText,bMaxText,BLUE,+1.0,-1.0);
			}
		else if (cmd.equals("<>"))
			{
			if      (obj == rBroader)
				contrastBrightness (rMinText,rMaxText,RED,-1.0,+1.0);
			else if (obj == gBroader)
				contrastBrightness (gMinText,gMaxText,GREEN,-1.0,+1.0);
			else if (obj == bBroader)
				contrastBrightness (bMinText,bMaxText,BLUE,-1.0,+1.0);
			}

		else if (cmd.equals(">|"))
			{
			if      (obj == rLift1)
				contrastBrightness (rMinText,rMaxText,RED,+1.0,0.0);
			else if (obj == gLift1)
				contrastBrightness (gMinText,gMaxText,GREEN,+1.0,0.0);
			else if (obj == bLift1)
				contrastBrightness (bMinText,bMaxText,BLUE,+1.0,0.0);
			}

		else if (cmd.equals("<|"))
			{
			if      (obj == rDrop1)
				contrastBrightness (rMinText,rMaxText,RED,-1.0,0.0);
			else if (obj == gDrop1)
				contrastBrightness (gMinText,gMaxText,GREEN,-1.0,0.0);
			else if (obj == bDrop1)
				contrastBrightness (bMinText,bMaxText,BLUE,-1.0,0.0);
			}

		else if (cmd.equals("|>"))
			{
			if      (obj == rLift2)
				contrastBrightness (rMinText,rMaxText,RED,0.0,+1.0);
			else if (obj == gLift2)
				contrastBrightness (gMinText,gMaxText,GREEN,0.0,+1.0);
			else if (obj == bLift2)
				contrastBrightness (bMinText,bMaxText,BLUE,0.0,+1.0);
			}

		else if (cmd.equals("|<"))
			{
			if      (obj == rDrop2)
				contrastBrightness (rMinText,rMaxText,RED,0.0,-1.0);
			else if (obj == gDrop2)
				contrastBrightness (gMinText,gMaxText,GREEN,0.0,-1.0);
			else if (obj == bDrop2)
				contrastBrightness (bMinText,bMaxText,BLUE,0.0,-1.0);
			}

		else if (cmd.equals("QUIT"))
			{
			frame.setVisible (false);
			frame = null;
			}
		else	{
			// IMAGES CHANGED

			String s = (String)rImages.getSelectedItem();
			if (!rImage.equals(s))
				{
				rImage = s;
				red   = WindowManager.getImage(rImage);
				if (red == null)
					{
					IJ.error("Cannot access red image "+rImage);
					return;
					}
				rMinText = new JTextField(getMin(red),10);
				rMaxText = new JTextField(getMax(red),10);
				}

			s = (String)gImages.getSelectedItem();
			if (!gImage.equals(s))
				{
				gImage = s;
				green = WindowManager.getImage(gImage);
				if (green == null)
					{
					IJ.error("Cannot access green image "+gImage);
					return;
					}
				gMinText = new JTextField(getMin(green),10);
				gMaxText = new JTextField(getMax(green),10);
				}

			s = (String)bImages.getSelectedItem();
			if (!bImage.equals(s))
				{
				bImage = s;
				blue  = WindowManager.getImage(bImage);
				if (blue == null)
					{
					IJ.error("Cannot access blue image "+bImage);
					return;
					}
				bMinText = new JTextField(getMin(blue),10);
				bMaxText = new JTextField(getMax(blue),10);
				}
			frame.pack();
			frame.setVisible(true);
			}
		}

	void auto (int color)
		{
		double mn=0.0;
		double mx=255.0;
		double med=128.0;
		ImageProcessor ip = null;
		JTextField txt1 = null;
		JTextField txt2 = null;

		if (color == RED)
			{
			ip = red.getProcessor();
			txt1 = rMinText;
			txt2 = rMaxText;
			}
		else if (color == GREEN)
			{
			ip = green.getProcessor();
			txt1 = gMinText;
			txt2 = gMaxText;
			}
		else	{
			ip = blue.getProcessor();
			txt1 = bMinText;
			txt2 = bMaxText;
			}
		// ip.findMinAndMax();
		mn = ip.getMin();
		mx = ip.getMax();
		if (bits == 32)
			{
			med = 0.0;
			for (int j=0; j < h; j++)
				for (int i=0; i < w; i++)
					med += ip.getPixelValue(i,j);
			med /= (double)(h*w);
			mn = 0.5*(mn+med);
			mx = 0.5*(mx+med);
			}
		else	{
			med = (double)ip.getAutoThreshold();
			mn = 0.5*(mn+med);
			mx = 0.5*(mx+med);
			}
		txt1.setText(gformat(mn));
		txt2.setText(gformat(mx));
		}

	/**
	 * Re-composes a color image using a new R,G, or B image as input, scaled to the requested limits.
	 */
	void compose(int color)
		{
		int n=w*h,spix,tpix;
		double[] d;
		double m=1.0,b=0.0;	// byte = m*doubleval+b;
		int bit;
		ImageProcessor ip = null;

		// GET THE COLOR DATA

		ColorProcessor cp = (ColorProcessor)rgb.getProcessor();
		byte[] R = new byte[n];
		byte[] G = new byte[n];
		byte[] B = new byte[n];
		cp.getRGB(R,G,B);

		// FOR ONE OF THE INPUT IMAGES, TRANSFER TO THE CORRESPONDING COLOR PLANE

		if (color == RED)
			{
			d = getMinMax(rMinText,rMaxText);
			if (d == null)
				{
				IJ.error("Cannot read R fields!");
				return;
				}
			if (d[0] != d[1]) m = 255.0/(d[1]-d[0]);
			b =  -m*d[0];
			ip = red.getProcessor();
            red.setDisplayRange(d[0], d[1]);
			for (int j=0; j < h; j++)	// FOR ALL SOURCE ROWS
				{
				int jj=j+rYShift;
				IJ.showProgress(j,h);
				for (int i=0; i < w; i++)	// FOR ALL SOURCE COLUMNS
					{
					int ii=i+rXShift;
					if (ii >= 0 && ii < w && jj >= 0 && jj < h)	// FOR ALL VISIBLE TARGET PIXELS
						{
						spix = w*jj+ii;		// SOURCE POSITION
						tpix = w*j+i;		// TARGET POSITION
						bit = (int)(m*ip.getPixelValue(ii,jj)+b);
						if (bit < 0) bit=0;
						if (bit > 255) bit=255;
						R[tpix] = (byte)bit;
						}
					}
				}
			}
		else if (color == GREEN)
			{
			d = getMinMax(gMinText,gMaxText);
			if (d == null)
				{
				IJ.error("Cannot read G fields!");
				return;
				}
			if (d[0] != d[1]) m = 255.0/(d[1]-d[0]);
			b =  -m*d[0];
			ip = green.getProcessor();
            green.setDisplayRange(d[0], d[1]);
			for (int j=0; j < h; j++)	// FOR ALL SOURCE ROWS
				{
				int jj=j+gYShift;
				IJ.showProgress(j,h);
				for (int i=0; i < w; i++)	// FOR ALL SOURCE COLUMNS
					{
					int ii=i+gXShift;
					if (ii >= 0 && ii < w && jj >= 0 && jj < h)	// FOR ALL VISIBLE TARGET PIXELS
						{
						spix = w*jj+ii;		// SOURCE POSITION
						tpix = w*j+i;		// TARGET POSITION
						bit = (int)(m*ip.getPixelValue(ii,jj)+b);
						if (bit < 0) bit=0;
						if (bit > 255) bit=255;
						G[tpix] = (byte)bit;
						}
					}
				}
			}
		else if (color == BLUE)
			{
			d = getMinMax(bMinText,bMaxText);
			if (d == null)
				{
				IJ.error("Cannot read B fields!");
				return;
				}
			if (d[0] != d[1]) m = 255.0/(d[1]-d[0]);
			b =  -m*d[0];
			ip = blue.getProcessor();
            blue.setDisplayRange(d[0], d[1]);
			for (int j=0; j < h; j++)	// FOR ALL SOURCE ROWS
				{
				int jj=j+bYShift;
				IJ.showProgress(j,h);
				for (int i=0; i < w; i++)	// FOR ALL SOURCE COLUMNS
					{
					int ii=i+bXShift;
					if (ii >= 0 && ii < w && jj >= 0 && jj < h)	// FOR ALL VISIBLE TARGET PIXELS
						{
						spix = w*jj+ii;		// SOURCE POSITION
						tpix = w*j+i;		// TARGET POSITION
						bit = (int)(m*ip.getPixelValue(ii,jj)+b);
						if (bit < 0) bit=0;
						if (bit > 255) bit=255;
						B[tpix] = (byte)bit;
						}
					}
				}
			}

		// RETURN TO THE ORIGINAL COLOR IMAGE

		// ip.setMin(d[0]);
		// ip.setMax(d[1]);
		cp.setRGB(R,G,B);
        refresh(rgb, color);
		}

	/**
	 * Returns current minimum displayed value of an image as a string.
	 */
	String getMin(ImagePlus im)
		{
		ImageProcessor ip = im.getProcessor();
		return gformat(ip.getMin());
		}

	/**
	 * Returns current maximum displayed value of an image as a string.
	 */
	String getMax(ImagePlus im)
		{
		ImageProcessor ip = im.getProcessor();
		return gformat(ip.getMax());
		}

	/**
	 * Sets current minimum displayed value of an image.
	 */
	void setMinMax(ImagePlus im, String smn, String smx)
		{
		ImageProcessor ip = im.getProcessor();
		double mn = 0.0;
		double mx = 1.0;
		try	{
			mn = Double.parseDouble(smn);
			mx = Double.parseDouble(smx);
			}
		catch (NumberFormatException e) {};
		ip.setMinAndMax(mn,mx);
		}

	/*
	 * Returns the numbers in two text fields as a double array.
	 */
	double[] getMinMax(JTextField txt1, JTextField txt2)
		{
		double[] d = new double[2];
		try	{
			d[0] = Double.parseDouble(txt1.getText());
			d[1] = Double.parseDouble(txt2.getText());
			}
		catch (NumberFormatException e)
			{
			return null;
			}
		return d;
		}

	/**
	 * Replaces all the items in the three combo boxes.
	 */
	void replaceItems()
		{
		replaceItems(rImages);
		replaceItems(bImages);
		replaceItems(bImages);
		frame.setVisible (true);
		}

	/**
	 * Changes contrast/brightness.
	 */
	void contrastBrightness (JTextField mn, JTextField mx, int color, double fmn, double fmx)
		{
		double d;
		double[] mnmx;
		if (!checkImages()) return;
		mnmx = getMinMax(mn,mx);
		d = mnmx[1]-mnmx[0];
		mnmx[0] += 0.1*d*fmn;
		mnmx[1] += 0.1*d*fmx;
		if (mnmx[1] < mnmx[0])
			{
			IJ.beep(); IJ.beep(); IJ.beep();
			return;
			}
		mn.setText(gformat(mnmx[0]));
		mx.setText(gformat(mnmx[1]));
		frame.setVisible(true);
		compose(color);
		refresh(rgb, color);
		return;
		}
    
    void refresh(ImagePlus imp, int color)
        {
        imp.getProcessor().snapshot();
        imp.updateAndDraw();
//        if (color==RED)
//            red.updateAndDraw();
//        else if (color==GREEN)
//            green.updateAndDraw();
//        else if (color==BLUE)
//            blue.updateAndDraw();
//        else if (color==ALL)
//            {
//            red.updateAndDraw();
//            green.updateAndDraw();
//            blue.updateAndDraw();
//            }        
 
        }

	/**
	 * Stupid swing doesn't have this function to replace the items!
	 */
	void replaceItems (JComboBox box)
		{
		if (box == null) return;
		int item=0;
		String s = (String)box.getSelectedItem();
		box.removeAllItems();
		for (int i=0; i < list.length; i++)
			{
			if (list[i] != null)
				{
				box.addItem(list[i]);
				if (s.equals(list[i]))
					item=i;
				}
			}
		box.setSelectedIndex(item);
		box.validate();
		}

	/**
	 * Returns a pre-formatted JPanel with a standard border.
	 */
	JPanel simpleJPanel (int layout)
		{
		return simpleJPanel (layout,5,5,5,5);
		}

	/**
	 * Returns a pre-formatted JPanel with a given border.
	 */
	JPanel simpleJPanel (int layout, int btop, int bleft, int bbottom, int bright)
		{
		JPanel pane = new JPanel();
		pane.setLayout (new BoxLayout(pane, layout));
		pane.setBorder (BorderFactory.createEmptyBorder(btop,bleft,bbottom,bright));
		return pane;
		}

	/**
	 * Returns a JLabel with white text.
	 */
	JLabel whiteLabel (String text)
		{
		JLabel label = new JLabel(text);
		label.setForeground(Color.white);
		return label;
		}

	/**
	 * Returns a list of currently displayed images, in reversed order (presumably latest most interesting)
	 * (Taken from astroj).
	 */
	public static String[] openImages (String def)
		{
		int off=0;
		int[] imageList = WindowManager.getIDList();
		if (imageList == null)
			return null;
		int n = imageList.length;
		if (def != null) off=1;
		String[] images = new String[n+off];
		for (int i=n-1; i >= 0; i--)
			{
			ImagePlus im = WindowManager.getImage (imageList[i]);
			images[i+off] = im.getTitle();
			}
		if (def != null) images[0] = new String(def);
		return images;
		}

	/**
	 * Chooses which formatter to use.
	 */
	protected String gformat(double d)
		{
		double ad = Math.abs(d);
		if (ad < 0.01 || ad > 1000.0)
			return ef.format(d);
		else
			return ff.format(d);
		}

	/**
	 * Test program for non-ImageJ use (need to comment out the ImageJ stuff.
	 */
	static public void main (String[] args)
		{
		RGB_Composer c = new RGB_Composer();
		c.run("test");
		}
	}
