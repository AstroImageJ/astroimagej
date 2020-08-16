package Astronomy;// Focus_Telescope.java

import Jama.Jama.Matrix;
import ij.*;
import ij.gui.*;
import ij.measure.ResultsTable;
import ij.plugin.*;
import ij.process.*;
import ij.text.*;
import ij.WindowManager;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.*;
import javax.swing.*;
import javax.swing.border.*;

import Jama.Jama.*;
import astroj.*;

/**
 * @author F.V. Hessman, Georg-August-Universitaet Goettingen
 * @version 1.0
 * @date 2007-MAY-08
 *
 * Swing-based interface because that created by GenericDialog is simply too large.
 *
 * Uses the JAMA matrix package: see http://math.nist.gov/javanumerics/jama/ and the
 * LinearLeastSquares object from Astronomy_.jar to do the fittting and also get decent error estimates.
 *
 * @version 1.1
 * @date 2007-SEP-19
 * @changes Added preferences, title to IJ.log() output.
 *
 * @version 1.2
 * @date 2009-FEB-27
 * @changes Added full choice of width and focus table columns.
 *
 * @version 1.3
 * @date 2009-MAY-29
 * @changes Added check for "FOCUS" as possible table column and made Measurement table default if present.
 *
 * @version 1.4
 * @date 2010-MAR-22
 * @change Fixed bug where table column entries weren't updated.
 * 
 * @version 1.5
 * @date 2010-DEC_01
 * @change Dynamic update of table availability.
 */
public class Focus_Telescope implements ActionListener, PlugIn
	{
	double[] x = null;
	double[] y = null;
	double[] coefs = null;
	int num;
	double slope = Double.NaN;
	String tableName = null;
	String pattern = null;
	MeasurementTable table = null;
	LinearLeastSquares lls = null;

	// HIGH-LEVEL UI
	JFrame frame = null;

	// EITHER TEXT OR TABLE
	String way = null;
	JComboBox methodSelection = null;
	static String USE_TEXTAREA = new String ("Input values by hand");
	static String USE_TABLE_WIDTHS = new String ("Get image width values from a measurement table");

	static String USE_ORDERED_SEQUENCE = new String ("Ordered sequence");
	static String USE_UNORDERED_SEQUENCE = new String ("Unordered sequence");
	static String USE_TABLE_COLUMN = new String ("Use table column");
	static String USE_FILENAME = new String ("Extract from file name");

	static String CANCEL = new String ("Cancel");
	static String OK = new String ("OK");
	static String MODE_SWITCH = new String ("MODE");
	static String NEW_TABLE = new String ("TABLE");

	// FOCUS,WIDTH VALUES FROM TEXTAREA
	JPanel textPanel = null;
	JTextArea text = null;

	// WIDTH VALUES FROM MEASUREMENTS TABLE
	JComboBox tableSelection = null;
	JPanel tablePanel = null;

	// FOCUS VALUES
	JTabbedPane sources = null;

	// FOCUS VALUES VIA min,interval,max
	JPanel focusPanel = null;
	JTextField minText = null;
	JTextField intText = null;
	JTextField maxText = null;

	// FOCUS VALUES EXTRACTED FROM LIST
	JPanel listPanel = null;
	JTextField listText = null;

	// FOCUS VALUES EXTRACTED FROM FILENAME PATTERN
	JPanel namePanel = null;
	JTextField preText = null;
	JTextField postText = null;

	// FOCUS VALUES EXTRACTED FROM A TABLE COLUMN
	JPanel columnPanel = null;
	JComboBox columnSelection = null;

	// SEEING VALUES
	JPanel seeingPanel = null;
	JComboBox seeingSelection = null;

	// ASYMPTOTIC SLOPE OF FOCUS "CURVE"
	JCheckBox checkSlope = null;
	JTextField slopeText = null;

	static int VERTICAL = BoxLayout.PAGE_AXIS;
	static int HORIZONTAL = BoxLayout.LINE_AXIS;

	String[] tables = null;
	boolean hasTables = false;

	// PREFERENCES

	static String PREF_SLOPE        = "telfocus.slope";
	static String PREF_METHOD       = "telfocus.method";
	static int    PREF_METHOD_TEXTAREA  = 0;
	static int    PREF_METHOD_REGULAR   = 1;
	static int    PREF_METHOD_IRREGULAR = 2;
	static int    PREF_METHOD_TABLE     = 3;
	static int    PREF_METHOD_FILENAME  = 4;
	int prefMethod = Focus_Telescope.PREF_METHOD_TABLE;

	/**
	 * Standard ImageJ PlugIn method which creates and runs the Swing interface.
	 */
	public void run (String arg)
		{
		if (cannotFindClasses()) return;
		getPrefs();
		createAndRunGUI();
		}

	/**
	 * Gets preferences from ImageJ preference file.
	 */
	protected void getPrefs ()
		{
		Prefs.get (Focus_Telescope.PREF_SLOPE, slope);
		Prefs.get (Focus_Telescope.PREF_METHOD, prefMethod);
		}

	/**
	 * Saves preferences to ImageJ preference file.
	 */
	protected void savePrefs ()
		{
		Prefs.set (Focus_Telescope.PREF_SLOPE, slope);
		Prefs.set (Focus_Telescope.PREF_METHOD, prefMethod);
		}

	/**
	 * Makes sure that the Jama classes are available.
	 */
	protected boolean cannotFindClasses()
		{
		try	{
			Class c = Class.forName ("Jama.Matrix");
			c = Class.forName ("Jama.SingularValueDecomposition");
			}
		catch (ClassNotFoundException e)
			{
			IJ.showMessage ("Could not find Jama Matrix classes : check out http://math.nist.gov/javanumerics/jama/");
			return true;
			}
		return false;
		}

	/**
	 * Creates the dynamic GUI using Swing objects.
	 */
	protected void createAndRunGUI ()
		{
		JPanel p;
		JLabel l;
		hasTables = checkForTables();

		// PUT TOGETHER THE INTERFACES (THIS WOULD BE HELL OF A LOT SIMPLER WITH AN IDE....)

		frame = new JFrame ("Find Telescope Focus");
		frame.setDefaultCloseOperation (JFrame.EXIT_ON_CLOSE);
		JPanel panel = simpleJPanel (VERTICAL);

		// TOP LEVEL IS PULL-DOWN FOR CHOICE OF TEXT OR TABLE AND POSSIBLE PULL-DOWN FOR CHOICE OF TABLE

		JPanel topPanel = simpleJPanel (VERTICAL);
		TitledBorder border = BorderFactory.createTitledBorder ("Select source of image width values");
		border.setTitleJustification(TitledBorder.CENTER);
		topPanel.setBorder (border);

			// MODE SWITCH

			if (hasTables)
				methodSelection = new JComboBox (new String[] {USE_TABLE_WIDTHS, USE_TEXTAREA});
			else
				methodSelection = new JComboBox (new String[] {USE_TEXTAREA, USE_TABLE_WIDTHS});
			methodSelection.setActionCommand (MODE_SWITCH);
			methodSelection.addActionListener (this);
			topPanel.add (methodSelection);

			// TABLE SELECTION

			tablePanel = simpleJPanel (VERTICAL);
			fillTablePanel();
			topPanel.add (tablePanel);

		panel.add (topPanel);
		panel.add (new JLabel(" "));

		// MIDDLE LEVEL IS REGION WHICH DETERMINES WHERE THE FOCUS VALUES COME FROM

		JPanel centerPanel = simpleJPanel (VERTICAL);
		border = BorderFactory.createTitledBorder ("Select source of telescope focus values and star widths");
		border.setTitleJustification(TitledBorder.CENTER);
		centerPanel.setBorder (border);

			// TEXT

			textPanel = simpleJPanel (VERTICAL);
			String vals = "50.90, 4.4\n51.00, 3.1\n51.20, 2.3\n51.40, 1.9\n51.50, 1.7\n51.60, 2.6\n51.80, 3.0\n52.00, 4.2\n\n";
			l = new JLabel ("Edit the following  {focus},{width}  pairs.");
			l.setAlignmentX (Component.CENTER_ALIGNMENT);
			textPanel.add (l);
			textPanel.add (new JLabel(" "));
			text = new JTextArea (vals);
			textPanel.add (text);
			textPanel.setVisible(!hasTables);
			centerPanel.add (textPanel);

		centerPanel.add (new JLabel(" "));

			// TABLE SOURCE OF FOCUS VALUES

			sources = new JTabbedPane();

				// REGULAR SERIES

				focusPanel = simpleJPanel (VERTICAL);

				p = simpleJPanel(HORIZONTAL);
				p.add (Box.createHorizontalGlue());
				p.add (new JLabel("Mininum value :  "));
				minText = new JTextField ("51.0",10);
				p.add (minText);
				p.add (Box.createHorizontalGlue());
				focusPanel.add (p);

				p = simpleJPanel (HORIZONTAL);
				p.add (Box.createHorizontalGlue());
				p.add (new JLabel("Regular increment :  "));
				intText = new JTextField ("0.1",10);
				p.add (intText);
				p.add (Box.createHorizontalGlue());
				focusPanel.add (p);

				p = simpleJPanel (HORIZONTAL);
				p.add (Box.createHorizontalGlue());
				p.add (new JLabel("Maximum value :  "));
				maxText = new JTextField ("52.0",10);
				p.add (maxText);
				p.add (Box.createHorizontalGlue());
				focusPanel.add (p);

				sources.addTab (USE_ORDERED_SEQUENCE, null, focusPanel, null);

				// IRREGULAR SERIES

				listPanel = simpleJPanel (VERTICAL);
				l = new JLabel("Edit this list of values.");
				l.setHorizontalTextPosition (JLabel.CENTER);
				listPanel.add (l);
				listPanel.add (new JLabel (" "));
				listText = new JTextField ("51.0, 51.1, 51.2, 51.3, 51.4, 51.5, 51.6, 51.7, 51.8, 51.9, 52.0");
				listPanel.add (listText);
				sources.addTab (USE_UNORDERED_SEQUENCE, null, listPanel, null);

				// FROM TABLE COLUMN

				columnPanel = simpleJPanel (HORIZONTAL);
				columnPanel.add (Box.createHorizontalGlue());
				columnPanel.add (new JLabel("Name of table column containing focus:  "));
				fillColumnPanel();
				columnPanel.add (Box.createHorizontalGlue());
				sources.addTab (USE_TABLE_COLUMN, null, columnPanel, null);

				// EXTRACT FOCUS FROM FILE NAMES

				namePanel = simpleJPanel(VERTICAL);
				namePanel.add (new JLabel("... using the pattern (*=focus value) : "));
				namePanel.add (new JLabel(" "));

					p = simpleJPanel (HORIZONTAL);
					p.add (Box.createHorizontalGlue());
					preText = new JTextField ("focus_",20);
					preText.setHorizontalAlignment (JTextField.RIGHT);
					p.add (preText);
					p.add (new JLabel(" * "));
					postText = new JTextField (".fits",10);
					postText.setHorizontalAlignment (JTextField.LEFT);
					p.add (postText);
					p.add (Box.createHorizontalGlue());

				namePanel.add (p);
				namePanel.add (Box.createVerticalGlue());
				sources.addTab (USE_FILENAME, null, namePanel, null);

				if (prefMethod == Focus_Telescope.PREF_METHOD_REGULAR)
					sources.setSelectedComponent (focusPanel);
				else if (prefMethod == Focus_Telescope.PREF_METHOD_IRREGULAR)
					sources.setSelectedComponent (listPanel);
				else if (prefMethod == Focus_Telescope.PREF_METHOD_FILENAME)
					sources.setSelectedComponent (namePanel);
				else if (prefMethod == Focus_Telescope.PREF_METHOD_TABLE)
					sources.setSelectedComponent (columnPanel);
				sources.setVisible (hasTables);

			centerPanel.add (sources);

			// TABLE SOURCE OF SEEING VALUES

			seeingPanel = simpleJPanel (HORIZONTAL);
			seeingPanel.add (Box.createHorizontalGlue());
			seeingPanel.add (new JLabel("Name of table column containing star widths :  "));
			fillSeeingPanel();
			seeingPanel.add (Box.createHorizontalGlue());
			seeingPanel.setVisible (hasTables);

			centerPanel.add (seeingPanel);

		panel.add (centerPanel);

		// FIELD FOR ASYMPTOTIC SLOPE

		p = simpleJPanel (HORIZONTAL);
		checkSlope = new JCheckBox("Use this pre-measured asymptotic focus slope");
		p.add (checkSlope);
		slopeText = new JTextField ("     ",10);
		p.add (slopeText);
		panel.add (p);

		// BOTTOM LEVEL IS FOR BUTTONS

		p = simpleJPanel(HORIZONTAL);
		JButton cancelButton = new JButton (CANCEL);
		cancelButton.addActionListener (this);
		p.add (cancelButton);
		p.add (Box.createHorizontalGlue());
		JButton okButton = new JButton (OK);
		okButton.addActionListener (this);
		p.add (okButton);

		panel.add (p);

		// START INTERFACE

		if (hasTables)
			way = new String (USE_TABLE_WIDTHS);
		else
			way = new String (USE_TEXTAREA);
		p = (JPanel)frame.getContentPane();
		p.setBorder (BorderFactory.createEmptyBorder(10,10,10,10));
		p.add (panel);

		frame.pack();
		frame.setResizable (false);
		frame.setVisible (true);
		}

	/**
	 * Retrieves all of the focus data from the input dialogue and/or measurements tables.
	 */
	protected boolean gotFocusData ()
		{
		boolean answer = true;
		boolean useTables = false;
		boolean gotFocusValues = false;
		String str;

		// USE FIXED ASYMPTOTIC SLOPE?

		if (checkSlope.isSelected())
			{
			str = slopeText.getText().trim();
			if (str.equals(""))
				{
				IJ.showMessage ("No fixed slope given.");
				return false;
				}
			slope = Double.NaN;
			try	{
				slope = Double.parseDouble (str);
				}
			catch (NumberFormatException e)
				{
				IJ.showMessage ("Could not parse fixed slope = ["+str+"]");
				return false;
				}
			}

		// GET GENERAL METHOD TO BE USED

		if (way.equals(USE_TEXTAREA))
			{
			if (!useSimpleDataPairs (text.getText())) return false;
			}

		else	{

			// GET WIDTH MEASUREMENTS FROM THE SELECTED TABLE COLUMN

			if (table == null || !useMeasurementTable () || num <= 0)
				{
				IJ.showMessage ("Could not use measurement table!");
				return false;
				}

			// GET FOCUS SETTINGS USING ONE OF THE AVAILABLE METHODS

			Component comp = sources.getSelectedComponent();
			if (comp == focusPanel)
				return useRegularSequence (minText.getText(),intText.getText(),maxText.getText());
			else if (comp == namePanel)
				return useFilename (preText.getText(),postText.getText());
			else if (comp == listPanel)
				return useIrregularSequence (listText.getText());
			else if (comp == columnPanel)
				return getOtherColumn ((String)columnSelection.getSelectedItem());
			}
		return true;
		}

	/**
	 * Plots results of focus curve fit.
	 */
	protected void plotFit ()
		{
		PlotWindow pw;
		double center;
		double val;
		boolean fixedSlope = ! Double.isNaN(slope);

		GFormat g = new GFormat("7.3");

		double xMin = IJU.minOf(x);
		double xMax = IJU.maxOf(x);
		double yMin = IJU.minOf(y);
		double yMax = IJU.maxOf(y);
		double  dx = (xMax-xMin)/99.0;

		// NO FIT MADE, SO JUST PLOT DATA

		if (coefs == null)
			{
			pw = new PlotWindow (
					"Focus Curve Data",
					"Focus Setting",
					"Image Width [pixels]",
					x, y);
			pw.setLimits (xMin-5.*dx, xMax+5.*dx, 0.0, yMax+0.05*(yMax-yMin));
			pw.draw();
			pw.addLabel (0.2, 0.08, "No focus fit performed");
			return;
			}

		// GET RESULTS

		double[] cerr = lls.coefficientErrors();
		Matrix cov = lls.covarianceMatrix();
		if (cerr == null || cov == null)
			{
			IJ.showMessage ("Unable to get coefficient errors or covariance matrix?");
			return;
			}

		double sigSlope = 0.0;
		if (coefs.length == 3)
			{
			slope = Math.sqrt(coefs[2]);
			sigSlope = 0.25*cerr[2]/coefs[2];
			}

		center = -0.5*coefs[1]/(slope*slope);
		double sigCenter = 0.5*cerr[1]/(slope*slope);
		if (coefs.length == 3)
			sigCenter = center*Math.sqrt(
						cerr[1]*cerr[1]/(coefs[1]*coefs[1])+
						cerr[2]*cerr[2]/(coefs[2]*coefs[2])-
						2.0*cov.get(1,2)/(coefs[1]*coefs[2])
						);
		val = Math.sqrt(coefs[0]-slope*slope*center*center);

		// PLOT FIT

		double[] xfit = new double[100];
		double[] yfit = new double[100];
		double X=0.0;
		double yy;
		for (int i=0; i < 100; i++)
			{
			X = xMin+dx*(double)i;
			yy = val*val+slope*slope*(X-center)*(X-center);
			xfit[i] = X;
			yfit[i] = Math.sqrt(yy);
			}
		pw = new PlotWindow (
				"Focus Curve",
				"Focus Setting",
				"Image Width [pixels]",
				xfit, yfit);
		pw.setLimits (xMin-5.*dx, xMax+5.*dx, 0.0, yMax+0.05*(yMax-yMin));
		pw.draw();

		// LABEL FIT

		String slStr = g.format(slope);
		if (!fixedSlope)
			slStr += "("+g.format(sigSlope)+")";
		pw.addLabel (0.05, 0.08, 
			g.format(center)+"("+g.format(sigCenter)+"), "+slStr+", "+g.format(val));

		// PLOT ASYMPTOTES

		double[] xa = new double[3];
		double[] ya = new double[3];
		xa[0] = xMin;
		xa[1] = center;
		xa[2] = xMax;
		ya[0] = slope*(center-xMin);
		ya[1] = 0.0;
		ya[2] = slope*(xMax-center);
		pw.setColor (Color.RED);
		pw.addPoints (xa,ya, PlotWindow.LINE);
		pw.draw();

		// PLOT DATA

		pw.setColor (Color.BLUE);
		pw.addPoints (x, y, PlotWindow.CIRCLE);
		pw.draw();

		// SAVE PREFERENCES

		savePrefs();
		}

	// ACTION LISTENER METHOD

	/**
	 * ActionListener method which catches OK and CANCEL buttons.
	 */
	public void actionPerformed (ActionEvent e)
		{
		String cmd = e.getActionCommand();
		if (cmd.equals(OK))
			{
			if (!gotFocusData()) return;
			frame.setVisible (false);
			frame = null;

			// CHECK DATA FOR 0,0 : PROBABLY DUE TO BAD private ResultsTable.getColumn PARSING
			checkData(x,y);

			lls = new LinearLeastSquares ();
			if (Double.isNaN(slope))
				lls.setFunctionType (LinearLeastSquares.HYPERBOLA_FUNCTION, null, null);
			else
				lls.setFunctionType (LinearLeastSquares.FOCUS_FUNCTION, null, new double[] {slope});
			for (int k=0; k < x.length; k++) IJ.log("\t"+x[k]+","+y[k]);
			coefs = lls.fit (x,y,null,lls);

			IJ.log("\nFocus Curve Fit\n===============\n");
			IJ.log("Data:\n");
			for (int k=0; k < x.length; k++) IJ.log("\t"+x[k]+","+y[k]);
			IJ.log(lls.results(x,y,null,lls,null));
			plotFit ();
			return;
			}
		else if (cmd.equals(CANCEL))
			{
			frame.setVisible (false);
			frame = null;
			return;
			}
		else if (cmd.equals(NEW_TABLE))
			{
			String s = (String)tableSelection.getSelectedItem();
			table = MeasurementTable.getTable (s);
			if (table == null)
				{
				IJ.showMessage ("Cannot access MeasurementTable called \""+s+"\"");
				return;
				}

			String[] columns = table.getColumnHeadings().split("\t");
			columnSelection.removeAllItems();
			seeingSelection.removeAllItems();
			for (int i=0; i < columns.length; i++)
				{
				columnSelection.addItem (columns[i]);
				seeingSelection.addItem (columns[i]);
				}
			frame.setResizable (true);
			frame.pack();
			frame.setResizable (false);
			frame.setVisible (true);
			}
		else if (cmd.equals(MODE_SWITCH))
			{
			JComboBox box = (JComboBox)e.getSource();
			String s = (String)box.getSelectedItem();
			hasTables = checkForTables();
			fillTablePanel();
			fillColumnPanel();
			fillSeeingPanel();
			if (box == methodSelection && s.equals(USE_TABLE_WIDTHS))
				{
				textPanel.setVisible (false);
				tablePanel.setVisible (true);
				sources.setVisible (true);
				seeingPanel.setVisible (true);
				frame.setResizable (true);
				frame.pack();
				frame.setResizable (false);
				frame.setVisible (true);
				way = new String (USE_TABLE_WIDTHS);
				}
			else if (box == methodSelection && s.equals(USE_TEXTAREA))
				{
				textPanel.setVisible (true);
				tablePanel.setVisible (false);
				sources.setVisible (false);
				seeingPanel.setVisible (false);
				frame.setResizable (true);
				frame.pack();
				frame.setResizable (false);
				frame.setVisible (true);
				way = new String (USE_TEXTAREA);
				}
			}
		}

	//
	// EXTRACTION METHODS
	//

	/*
	 * Creates a sequence of x[] and y[] values drawn from a textfield.
	 */
	protected boolean useSimpleDataPairs (String text)
		{
		int nix=0;
		String s = text.replace('\n',',');
		String[] vals = s.split(",");
		num = vals.length/2;
		x = new double[num];
		y = new double[num];
		for (int i=0; i < num; i++)
			{
			try	{
				x[i] = Double.parseDouble(vals[2*i]);
				y[i] = Double.parseDouble(vals[2*i+1]);
				}
			catch (NumberFormatException e)
				{
				nix++;
				x[i] = Double.NaN;
				y[i] = Double.NaN;
				}
			}
		if (nix > 0) IJ.showMessage ("Warning: could only parse "+(num-nix)+" data pairs!");
		if ((num-nix) < 4) return false;

		if (nix > 0)
			{
			double[] xx = new double[num-nix];
			double[] yy = new double[num-nix];
			nix=0;
			for (int i=0; i < num; i++)
				{
				if (!Double.isNaN(x[i]) && !Double.isNaN(y[i]))
					{
					xx[nix] = x[i];
					yy[nix] = y[i];
					nix++;
					}
				}
			x = xx;
			y = yy;
			}
		prefMethod = Focus_Telescope.PREF_METHOD_TEXTAREA;
		return true;
		}

	/*
	 * Creates a sequence of x[] focus values for a set of focus y[] values based upon any two of:
	 * a starting value, a focus difference between values, and a final value.
	 */
	protected boolean useRegularSequence (String sf1, String sdf, String sf2)
		{
		if (x == null || x.length != num) return false;

		double f1=Double.NaN, df=Double.NaN, f2=Double.NaN;

		try	{
			f1 = Double.parseDouble(sf1);
			df = Double.parseDouble(sdf);
			f2 = Double.parseDouble(sf2);
			}
		catch (NumberFormatException e)
			{
			}
		if (!Double.isNaN(f1) && !Double.isNaN(df))
			f2 = f1+df*(num-1);
		else if (!Double.isNaN(f1) && !Double.isNaN(f2))
			df = (f2-f1)/(num-1);
		else if (!Double.isNaN(df) && !Double.isNaN(f2))
			f1 = f2-df*(num-1);
		else
			{
			IJ.showMessage("Cannot parse useable numbers from ["+sf1+"]["+sdf+"]["+sf2+"]");
			return false;
			}			
		for (int i=0; i < num; i++)
			x[i] = f1+df*i;
		prefMethod = Focus_Telescope.PREF_METHOD_REGULAR;
		return true;
		}

	/*
	 * Creates a sequence of x[] focus values for a set of focus y[] values based upon an irregular list of values.
	 */
	protected boolean useIrregularSequence (String str)
		{
		if (x == null || x.length <= 0) return false;

		String[] vals = str.split(",");
		if (vals.length != num)
			{
			IJ.showMessage ("The number of focus entries in \""+str+"\" does not match the number of table entries ("+ num+")!");
			return false;
			}
		try	{
			for (int i=0; i < num; i++)
				x[i] = Double.parseDouble (vals[i]);
			}
		catch (NumberFormatException e)
			{
			IJ.showMessage ("Cannot parse list \""+str+"\"");
			return false;
			}
		prefMethod = Focus_Telescope.PREF_METHOD_IRREGULAR;
		return true;
		}

	/*
	 * Creates a sequence of x[] focus values derived from the names of the original files.
	 */
	protected boolean useFilename (String prefx, String postfx)
		{
		if (table == null || x == null || x.length != num) return false;

		// PARSE FILE NAMES

		for (int i=0; i < num; i++)
			{
			String label = table.getLabel (i);
			if (label == null) break;

			String focus = getSubString (label, prefx, postfx);
			if (focus == null)
				return false;
			try	{
				x[i] = Double.parseDouble(focus);
				}
			catch (NumberFormatException e)
				{
				IJ.showMessage ("Table row #"+i+", image \""+label+"\" : cannot parse "+focus);
				return false;
				}
			}
		prefMethod = Focus_Telescope.PREF_METHOD_FILENAME;
		return true;
		}

	/**
	 * Extracts the sub-string contained between two other sub-strings.
	 */
	protected String getSubString (String label, String prefix, String suffix)
		{
		String focus = null;
		if (prefix == null || prefix.length() == 0) // ONLY A SUFFIX TO GET RID OF
			{
			int k=label.indexOf(suffix);
			if (k >= 0)
				focus = label.substring(0,k);
			else
				IJ.showMessage("Unable to find suffix \""+suffix+"\" in \""+label+"\"");
			}
		else if (suffix == null || suffix.length() == 0) // ONLY A PREFIX TO GET RID OF
			{
			int k=label.indexOf(prefix);
			if (k >= 0)
				{
				k += prefix.length();
				focus = label.substring(k);
				}
			else	
				IJ.showMessage("Unable to find prefix \""+prefix+"\" in \""+label+"\"");
			}
		else	{
			int k1=label.indexOf(prefix);
			int k2=label.indexOf(suffix);
			if (k1 < 0 || k2 < 0)
				System.err.println("index<0"); // IJ.showMessage("Unable to find prefix \""+prefix+"\" and suffix \"" +suffix+"\" in \""+label+"\"");
			else
				focus = label.substring(k1+prefix.length(),k2);
			}
		return focus;
		}

	/**
	 * Finds the MeasurementTable with the name "filename" and extracts the width values into y[].
	 */
	protected boolean useMeasurementTable ()
		{
		if (table != null)
			{
			String widthSelection = (String)seeingSelection.getSelectedItem();
			int widthColumn = table.getColumnIndex (widthSelection);
			if (widthColumn == ResultsTable.COLUMN_NOT_FOUND)
				{
				IJ.showMessage ("Table does not have a column labelled \""+widthSelection+"\"");
				return false;
				}
			y = table.getDoubleColumn (widthColumn);
			if (y == null)
				{
				IJ.showMessage ("Could not read column \""+widthSelection+"\"");
				return false;
				}
			num = y.length;
			if (num <= 0)
				{
				IJ.showMessage ("No data in column \""+widthSelection+"\"");
				return false;
				}
			x = new double[num];
			}
		else
			return false;
		return true;
		}

	/**
	 * Uses the pre-found MeasurementTable "table" and extracts the column of focus setting values into x[].
	 */
	protected boolean getOtherColumn (String label)
		{
		if (table == null || y == null)
			{
			IJ.showMessage ("Strange error: Could not access table or width array");
			return false;
			}
		int widthColumn = table.getColumnIndex (label);
		if (widthColumn == ResultsTable.COLUMN_NOT_FOUND)
			{
			IJ.showMessage ("Cannot find column with label \""+label+"\"");
			return false;
			}
		x = table.getDoubleColumn (widthColumn);
		if (x == null)
			{
			IJ.showMessage ("Cannot read column with label \""+label+"\"");
			return false;
			}
		if (x.length != y.length)
			{
			IJ.showMessage ("X- and Y-arrays have different lengths! ("+x.length+" = "+y.length+"?)");
			return false;
			}
// for (int i=0; i < x.length; i++) IJ.log("i,x,y="+i+","+x[i]+","+y[i]);	// ??????
		prefMethod = PREF_METHOD_TABLE;
 		return true;
		} 

	/**
	 * Returns a pre-formatted JPanel.
	 */
	JPanel simpleJPanel (int layout)
		{
		JPanel pane = new JPanel();
		pane.setLayout (new BoxLayout(pane, layout));
		pane.setBorder (BorderFactory.createEmptyBorder(5,5,5,5));
		return pane;
		}

	/**
	 * Test program for non-ImageJ use (need to comment out the ImageJ stuff.
	 */
	static public void main (String[] args)
		{
		Focus_Telescope ft = new Focus_Telescope();
		ft.run("test");
		}

	/**
	 * Returns index of a given string in a JComboBox. If not found, returns defVal if < 0  or count-1.
	 */
	static int getIndexOf (String str, JComboBox box, int defVal)
		{
		int n = box.getItemCount();
		for (int i=0; i < n; i++)
			{
			String s = (String)box.getItemAt(i);
			if (str.equals(s))
				return i;
			}
		if (defVal < 0)
			return defVal;
		else
			return n-1;
		}
	/**
	 * Check's data for funny data pairs due to bad ResultsTable parsing.
	 */
	static void checkData (double[] x, double[] y)
		{
		int cnt=x.length;
		if (cnt != y.length) return;
		for (int i=0; i < cnt; i++)
			{
			if (x[i] == 0.0 && y[i] == 0.0)
				{
				x[i] = Double.NaN;
				y[i] = Double.NaN;
				}
// IJ.log(""+i+": "+x[i]+","+y[i]);
			}
		}
 
	/**
	 * Check to see if there are any tables available.
	 */
	protected boolean checkForTables()
		{
		boolean has = false;
		tables = MeasurementTable.getMeasurementTableNames();
		if (tables != null && tables.length > 0)
			{
			int n=0;
			while (n < tables.length && has == false)
				{
				table = MeasurementTable.getTable (tables[n]);
				if (table != null) has = true;
				}
			}
		return has;
		}

	/**
	 * Fills table panel with current table info.
	 */
	protected void fillTablePanel()
		{
		tablePanel.removeAll();
		if (hasTables)
			{
			tableSelection = new JComboBox (tables);
			tableSelection.setActionCommand (NEW_TABLE);
			tableSelection.addActionListener (this);
			JPanel p = simpleJPanel (VERTICAL);
			p.add (new JLabel("Select a measurement table."));
			p.add (tableSelection);
			tablePanel.add (p);
			}
		tablePanel.setVisible (hasTables);
		}

	/**
	 * Fills column selection panel with current table.
	 */
	protected void fillColumnPanel()
		{
		columnPanel.removeAll();
		if (hasTables)
			{
			String[] columns = table.getColumnHeadings().split("\t");
			columnSelection = new JComboBox (columns);
			int idx = getIndexOf("TEL-FOCU",columnSelection,-1);
			if (idx == -1)
				idx = getIndexOf("FOCUS",columnSelection,0);
			columnSelection.setSelectedIndex (idx);	// getIndexOf("TEL-FOCU",columnSelection));
			columnSelection.addActionListener (this);
			columnPanel.add (columnSelection);	// columnText);
			}
		}

	/**
	 * Fills seeing selection panel with current table.
	 */
	protected void fillSeeingPanel()
		{
		seeingPanel.removeAll();
		if (hasTables)
			{
			String[] columns = table.getColumnHeadings().split("\t");
			seeingSelection = new JComboBox (columns);
			seeingSelection.setSelectedIndex (getIndexOf("Width",seeingSelection,0));
			seeingSelection.addActionListener (this);
			seeingPanel.add (seeingSelection);
			}
		}

	}
