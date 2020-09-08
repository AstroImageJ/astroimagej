package Astronomy;// CCD_Calibration.java

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.JCheckBox;

import ij.*;
import ij.io.*;
import ij.process.*;
import ij.plugin.*;

import astroj.*;

/**
 * Facilitates the calibration of CCD (or similar) grayscale images/stacks by noting standard calibration images 
 * in Preferences, automatically converting images to 32 bits, and applying standard calibrations.
 * 
 * @author F.V. Hessman
 * @date 2006-10-31
 * @version 1.1
 *
 * @version 1.2
 * @date 2007-09-17
 * @changes Switched from FITSUtilities to FitsHeader for better support of stacks (FVH)
 *
 * @version 1.3
 * @date 2008-02-26
 * @changes Switched from FitsHeader to FitsJ for even BETTER support of stacks! (FVH)
 */
public class CCD_Calibration implements PlugIn, ActionListener, ItemListener
	{
	public static String SUFFIX = new String ("_cal");

	public static String PREFS_RAW_IMAGE   = new String ("calibration.rawimage");
 	public static String PREFS_BIAS_IMAGE   = new String ("calibration.biasimage");
	public static String PREFS_DARK_IMAGE = new String ("calibration.darkimage");
	public static String PREFS_FLAT_IMAGE  = new String ("calibration.flatimage");

	public static String PREFS_BIAS_CORRECTION = new String ("calibration.biascorrection");
	public static String PREFS_DARK_CORRECTION = new String ("calibration.darkcorrection");
	public static String PREFS_FLAT_CORRECTION = new String ("calibration.flatcorrection");
	public static String PREFS_NORMALIZE_DARK = new String ("calibration.normdark");

	public static String BROWSE_RAW = new String ("Browse for data");
	public static String BROWSE_BIAS = new String ("Browse for bias");
	public static String BROWSE_DARK = new String ("Browse for dark");
	public static String BROWSE_FLAT = new String ("Browse for flatfield");

	protected String rawPath;
	protected String darkPath;
	protected String biasPath;
	protected String flatPath;
	protected String slash;

	protected boolean fileCorrection = true;
	protected boolean imageCorrection = false;

	protected boolean darkCorrection, biasCorrection, flatCorrection;
	protected boolean normalizeDark;
	protected boolean ok = true;
	protected boolean ignoreAction = false;

	ImagePlus rawImage = null;
	ImagePlus biasImage = null;
	ImagePlus darkImage = null;
	ImagePlus flatImage = null;
	ImagePlus resultImage = null;

	ImageStack resultStack = null;
	int stackSize = 0;

	JFrame dialogFrame;
	JCheckBox biasCheck, darkCheck, flatCheck;

	JCheckBox imageCheck = null;
	JCheckBox fileCheck = null;

	JTextField rawText, biasText, darkText, flatText;
	JButton rawButton, biasButton, darkButton, flatButton, okButton, cancelButton;
	JComboBox imageSelector = null;

	String[] header = null;

	public void run (String arg)
		{
		initialize ();
		startDialog();
		}

	/**
	 * Prepares calibration.
	 */
	protected void initialize ()
		{
		String def = "nix"; // IJ.getDirectory("home")+"image.fits";

		rawPath = Prefs.get (PREFS_RAW_IMAGE, def);
		darkPath = Prefs.get (PREFS_DARK_IMAGE, def);
		biasPath = Prefs.get (PREFS_BIAS_IMAGE, def);
		flatPath = Prefs.get (PREFS_FLAT_IMAGE, def);

		darkCorrection = Prefs.get (PREFS_DARK_CORRECTION, false);
		biasCorrection  = Prefs.get (PREFS_BIAS_CORRECTION,  false);
		flatCorrection   = Prefs.get (PREFS_FLAT_CORRECTION,  false);
		normalizeDark = Prefs.get (PREFS_NORMALIZE_DARK, false);

		fileCorrection = true;
		imageCorrection = false;
		}

	/**
	 * Creates dialog which lets the user enter the calibration options and calibration images.
	 */
	protected void startDialog ()
		{
		int rows=0;

		dialogFrame = new JFrame ("Raw CCD Image/Stack Calibration");

		// MAIN PANEL

		JPanel p = new JPanel(new SpringLayout());
		p.setBorder (BorderFactory.createEmptyBorder(15,15,15,15));

		// LABEL

		p.add (Box.createHorizontalGlue());
		p.add (Box.createHorizontalGlue());
		p.add (Box.createHorizontalGlue());
		rows++;

		// DATA IMAGE

		String[] str = IJU.listOfOpenImages(null);	// displayedImages();
		if (str != null)
			{
			imageCorrection = true;
			fileCorrection = false;			

			imageCheck = new JCheckBox ("Calibrate displayed image/stack", imageCorrection);
			imageCheck.addItemListener (this);
			p.add (imageCheck);

			imageSelector = new JComboBox (str);
			imageSelector.setSelectedIndex(0);
			imageSelector.addActionListener (this);
			p.add (imageSelector);

			p.add (Box.createHorizontalStrut(20));

			rows++;
			}

		// DATA FILE

		fileCheck = new JCheckBox ("Calibrate image/stack in file", fileCorrection);
		fileCheck.addItemListener (this);
		p.add (fileCheck);

		rawText = new JTextField (rawPath);
		rawText.addActionListener (this);
		rawText.setPreferredSize (new Dimension (400,20));
		rawText.setHorizontalAlignment (JTextField.RIGHT);
		p.add (rawText);

		rawButton = new JButton (BROWSE_RAW);
		rawButton.addActionListener (this);
		p.add (rawButton);

		rows++;

		// SPACE TO SEPARATE DATA FROM CALIBRATION

		p.add (Box.createHorizontalGlue());
		p.add (Box.createHorizontalGlue());
		p.add (Box.createHorizontalGlue());
		rows++;

		// BIAS

		biasCheck = new JCheckBox ("Subtract bias        ", biasCorrection);
		biasCheck.addItemListener (this);
		p.add (biasCheck);

		biasText = new JTextField (biasPath);
		biasText.addActionListener (this);
		biasText.setPreferredSize (new Dimension (400,20));
		biasText.setHorizontalAlignment (JTextField.RIGHT);
		p.add (biasText);

		biasButton = new JButton (BROWSE_BIAS);
		biasButton.addActionListener (this);
		p.add (biasButton);

		rows++;

		// DARK

		darkCheck = new JCheckBox ("Subtract dark current", darkCorrection);
		darkCheck.addItemListener (this);
		p.add (darkCheck);

		darkText = new JTextField (darkPath);
		darkText.addActionListener (this);
		darkText.setPreferredSize (new Dimension (400,20));
		darkText.setHorizontalAlignment (JTextField.RIGHT);
		p.add (darkText);

		darkButton = new JButton (BROWSE_DARK);
		darkButton.addActionListener (this);
		p.add (darkButton);

		rows++;

		// FLAT

		flatCheck = new JCheckBox ("Divide by flatfield  ", flatCorrection);
		flatCheck.addItemListener (this);
		p.add (flatCheck);

		flatText = new JTextField (flatPath);
		flatText.addActionListener (this);
		flatText.setPreferredSize (new Dimension (400,20));
		flatText.setHorizontalAlignment (JTextField.RIGHT);
		p.add (flatText);

		flatButton = new JButton (BROWSE_FLAT);
		flatButton.addActionListener (this);
		p.add (flatButton);

		rows++;

		// BUTTONS

		p.add (Box.createHorizontalStrut(20));

		JPanel pp = new JPanel();
		okButton = new JButton ("OK");
		okButton.addActionListener (this);
		pp.add (okButton);
		pp.add (Box.createHorizontalGlue());
		cancelButton = new JButton ("Cancel");
		cancelButton.addActionListener (this);
		pp.add (cancelButton);
		p.add(pp);

		p.add (Box.createHorizontalStrut(20));

		rows++;

		// LAY OUT PANEL AFTER THE FACT (rows HIGH, 3 WIDE)

		SpringUtil.makeCompactGrid (p, rows,3, 6,6,6,6);   // FORGOTTEN BY STUPID JAVA DEVELOPERS!
		dialogFrame.add(p);

		// ADJUST SIZES AND DISPLAY

		dialogFrame.pack();
		dialogFrame.setResizable (false);
		dialogFrame.setVisible(true);
		}

	/**
	 * Response to calibration option checkboxes being clicked.
	 */
	public void itemStateChanged (ItemEvent ie)
		{
		String action = ie.paramString();
		Object source = ie.getItemSelectable();
		if (source == biasCheck)
			biasCorrection = ! biasCorrection;
		else if (source == darkCheck)
			darkCorrection = ! darkCorrection;
		else if (source == flatCheck)
			flatCorrection = ! flatCorrection;
		else if (imageCheck != null && source == imageCheck)
			{
			imageCorrection = imageCheck.isSelected();
			fileCheck.setSelected (!imageCorrection);
			}
		else if (source == fileCheck)
			{
			fileCorrection = fileCheck.isSelected();
			if (imageCheck != null)
				imageCheck.setSelected (!fileCorrection);
			}
		}

	/**
	 * Reponse to buttons being pushed.
	 */
	public void actionPerformed (ActionEvent ae)
		{
		String action = ae.getActionCommand();
		if (action.equals("Cancel"))
			{
			ok = false;
			if (dialogFrame != null)
				{
				dialogFrame.setVisible (false);
				dialogFrame.dispose();
				}
			}
		else if (action.equals ("OK"))
			{
			if (! process() && dialogFrame != null)
				{
				dialogFrame.setVisible (false);
				dialogFrame.dispose();
				dialogFrame = null;
				}
			}
		else if (action.equals (BROWSE_RAW))
			{
			if (rawPath.equals(""))
				rawPath = getPossiblePath();
			OpenDialog od = new OpenDialog ("Select data image to be calibrated",
				IJU.extractDirectory(rawPath), IJU.extractFilename(rawPath));
			if (od.getDirectory() != null && od.getFileName() != null)
				{
				rawPath = od.getDirectory()+od.getFileName();
				rawText.setText (rawPath);
				rawText.setHorizontalAlignment (JTextField.RIGHT);
				}
			}
		else if (action.equals (BROWSE_BIAS))
			{
			if (biasPath.equals(""))
				biasPath = getPossiblePath();
			OpenDialog od = new OpenDialog ("Select bias calibration image",
					IJU.extractDirectory(biasPath), IJU.extractFilename(biasPath));
			if (od.getDirectory() != null && od.getFileName() != null)
				{
				biasPath = od.getDirectory()+od.getFileName();
				biasText.setText (biasPath);
				biasText.setHorizontalAlignment (JTextField.RIGHT);
				}
			}
		else if (action.equals (BROWSE_DARK))
			{
			if (darkPath.equals(""))
				darkPath = getPossiblePath();
			OpenDialog od = new OpenDialog ("Select dark calibration image",
				IJU.extractDirectory(darkPath), IJU.extractFilename(darkPath));
			if (od.getDirectory() != null && od.getFileName() != null)
				{
				darkPath = od.getDirectory()+od.getFileName();
				darkText.setText (darkPath);
				darkText.setHorizontalAlignment (JTextField.RIGHT);
				}
			}
		else if (action.equals (BROWSE_FLAT))
			{
			if (flatPath.equals(""))
				flatPath = getPossiblePath();
			OpenDialog od = new OpenDialog ("Select flatfield calibration image",
				IJU.extractDirectory(flatPath), IJU.extractFilename(flatPath));
			if (od.getDirectory() != null && od.getFileName() != null)
				{
				flatPath = od.getDirectory()+od.getFileName();
				flatText.setText (flatPath);
				flatText.setHorizontalAlignment (JTextField.RIGHT);
				}
			}
/*
		else
			{
			IJ.log("Unknown action command:  ["+action+"]");
			}
*/
		}

	/**
	 * Checks to see if there are any blank paths which could be set to a newly set path.
	 */
	protected String getPossiblePath ()
		{
		if (! biasPath.equals(""))
			return new String (biasPath);
		if (! darkPath.equals(""))
			return new String (darkPath);
		if (! flatPath.equals(""))
			return new String (flatPath);
		if (! rawPath.equals(""))
			return new String (rawPath);
		else
			return IJ.getDirectory("home");
		}

	/**
	 * Get the images, apply the calibrations, show the result image, and save the preferences.
	 */
	protected boolean process ()
		{
		// GET IMAGES

		if (!imageCorrection && !fileCorrection)
			{
			IJ.showMessage ("No file or image to correction was given!");
			return false;
			}
		if (!getImages())
			{
			IJ.showMessage ("Could not get all images!");
			return false;
			}
		if (!biasCorrection && !darkCorrection && !flatCorrection)
			{
			IJ.showMessage ("Please select at least one calibration.");
			return false;
			}

		// IMAGES SUCCESSFULLY OBTAINED SO CLOSE DIALOGUE

		dialogFrame.setVisible (false);
		dialogFrame.dispose();
		dialogFrame = null;

		// PERFORM CALIBRATION

		if (!calibrate())
			{
			IJ.showMessage ("Could not perform calibration!");
			return false;
			}
		resultImage.show();

		// CLEAN UP

		savePrefs ();
		if (rawImage != null) rawImage.unlock();
		if (biasImage != null) biasImage.unlock();
		if (darkImage != null) darkImage.unlock();
		if (flatImage != null) flatImage.unlock();
		if (resultImage != null) resultImage.unlock();
		IJ.log("     CCD_Calibration finished.");

		return true;
		}

	/**
	 * Given the list of images to be used and the flags which determine which form
	 * of calibration is desired, this method instantiates the images.
	 */
	protected boolean getImages ()
		{
		int w,h,z;
		String filename;

		// GET INPUT DATA IMAGE FROM FILE

		if (fileCorrection)
			{
			rawImage = new ImagePlus (rawPath);
			filename = IJU.extractFilename (rawPath);
			if (rawImage == null)
				{
				IJ.showMessage ("Unable to read data image "+filename);
				return false;
				}
			}

		// GET CURRENTLY DISPLAYED IMAGE

		else	{
			filename = (String)imageSelector.getSelectedItem();
			rawImage = WindowManager.getImage (filename);
			if (rawImage == null)
				{
				IJ.showMessage ("Unable to get selected displayed image?");
				return false;
				}
			}
			
		w = rawImage.getWidth();
		h = rawImage.getHeight();
		stackSize = rawImage.getStackSize();

		// GET THE BIAS IMAGE

		if (biasCorrection)
			{
			biasImage = new ImagePlus (biasPath);
			if (biasImage == null)
				{
				IJ.showMessage ("Unable to read bias image "+IJU.extractFilename (biasPath));
				return false;
				}
			else	if (w != biasImage.getWidth() || h != biasImage.getHeight())
				{
				IJ.showMessage ("Raw data and Bias images are not the same size!");
				return false;
				}
			}

		// GET THE DARK CURRENT IMAGE

		if (darkCorrection)
			{
			darkImage = new ImagePlus (darkPath);
			if (darkImage == null)
				{
				IJ.showMessage ("Unable to read dark image "+IJU.extractFilename(darkPath));
				return false;
				}
			else	if (w != darkImage.getWidth() || h != darkImage.getHeight())
				{
				IJ.showMessage ("Data and Dark images are not the same size!");
				return false;
				}
			}

		// GET THE FLATFIELD IMAGE

		if (flatCorrection)
			{
			flatImage = new ImagePlus (flatPath);
			if (flatImage == null)
				{
				IJ.showMessage ("Unable to read flatfield image "+IJU.extractFilename(flatPath));
				return false;
				}
			else if (w != flatImage.getWidth() || h != flatImage.getHeight())
				{
				IJ.showMessage ("Data and Flatfield images are not the same size!");
				return false;
				}
			}

		// GET UNIQUE NAME FOR OUTPUT IMAGE/STACK

		String path = null;
		String suffix = SUFFIX;
		if (biasCorrection || darkCorrection || flatCorrection)
			{
			suffix += "-";
			if (biasCorrection)
				suffix += "B";
			if (darkCorrection)
				suffix += "D";
			if (flatCorrection)
				suffix += "F";
			if (biasCorrection && darkCorrection && flatCorrection)
				suffix = SUFFIX;
			path = IJU.extractFilenameWithoutSuffix(filename)+suffix;
			String s = IJU.extractFilenameSuffix(filename);
			if (s != null)
				path += "."+s;
			filename = IJU.uniqueDisplayedImageName (path);
			}

		if (stackSize <= 1)
			{
			FloatProcessor fp = new FloatProcessor (w,h);
			resultImage = new ImagePlus (filename, fp);
			}
		else	{
			ImageStack stack = rawImage.getStack();
			resultStack = new ImageStack (w,h);
			for (int k=1; k <= stackSize; k++)
				{
				rawImage.setSlice(k);
				ImageProcessor inp = rawImage.getProcessor();
				FloatProcessor fp = new FloatProcessor (w,h);
				String label = stack.getShortSliceLabel (k); 
				path = IJU.extractFilenameWithoutSuffix (label)+suffix;
				String s = IJU.extractFilenameSuffix (label);
				if (s != null)
					path += "."+s;
				label = IJU.uniqueDisplayedImageName (path);
				resultStack.addSlice (label,fp);
				}
			resultImage = new ImagePlus (filename, resultStack);
			}
		return true;
		}

	/**
	 * Performs bias and dark subtraction and division by a flatfield image.
	 */
	protected boolean calibrate ()
		{
		int w = rawImage.getWidth();
		int h = rawImage.getHeight();
		int z = rawImage.getStackSize();
		int total = w*h*z;

		double b = 0.0;
		double d = 0.0;
		double f = 1.0;
		double r = 0.0;

		ImageProcessor bp=null;
		ImageProcessor dp=null;
		ImageProcessor fp=null;
		ImageProcessor ip=null;
		ImageProcessor rp=null;

		if (biasCorrection) bp = biasImage.getProcessor();
		if (darkCorrection) dp = darkImage.getProcessor();
		if (flatCorrection) fp = flatImage.getProcessor();

		String comment = "CCD_Calibration "+IJU.extractFilename(rawPath);
		if (biasCorrection) comment += " - "+IJU.extractFilename(biasPath);
		if (darkCorrection) comment += " - "+IJU.extractFilename(darkPath);
		if (flatCorrection) comment += " / "+IJU.extractFilename(flatPath);


		IJ.log(comment);
		if (stackSize > 1)
			IJ.showStatus ("Processing stack...");
		else
			IJ.showStatus ("Processing image...");

		for (int k=1; k <= z; k++)
			{
			if (stackSize > 1)
				{
				rawImage.setSlice(k);
				resultImage.setSlice(k);
				}
			ip = rawImage.getProcessor();
			rp = resultImage.getProcessor();

			for (int j=0; j < h; j++)
				{
				int done = w*j*k;
				IJ.showProgress (done,total);
				for (int i=0; i < w; i++)
					{
					if (biasCorrection) b = bp.getPixelValue(i,j);
					if (darkCorrection) d = dp.getPixelValue(i,j);
					if (flatCorrection) f = fp.getPixelValue(i,j);
					r = (ip.getPixelValue(i,j)-b-d)/f;						rp.putPixelValue(i,j,r);
					}
				}
			header = FitsJ.getHeader (rawImage);
			FitsJ.addComment (comment,header);
			FitsJ.putHeader (resultImage,header);
			}

		// CLEAN UP

		IJ.showStatus ("...finished");
		rp.resetMinAndMax ();
		return true;
		}

	/**
	 * Save paths and options in ImageJ preferences.
	 */
	protected void savePrefs ()
		{
		Prefs.set (PREFS_RAW_IMAGE, rawPath);
		Prefs.set (PREFS_DARK_IMAGE, darkPath);
		Prefs.set (PREFS_BIAS_IMAGE, biasPath);
		Prefs.set (PREFS_FLAT_IMAGE, flatPath);

		Prefs.set (PREFS_DARK_CORRECTION, darkCorrection);
		Prefs.set (PREFS_BIAS_CORRECTION, biasCorrection);
		Prefs.set (PREFS_FLAT_CORRECTION, flatCorrection);
		Prefs.set (PREFS_NORMALIZE_DARK, normalizeDark);
		}
	}
