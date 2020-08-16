package Astronomy;// Plot_Table_Columns.java

import ij.*;
import ij.gui.*;
import ij.measure.ResultsTable;
import ij.plugin.PlugIn;
import ij.Prefs;

import java.awt.*;
import ij.process.*;
import java.lang.Math.*;

// astroj ------
	import astroj.*;
// ------ astroj


/**
 * This plugin plots any two columns from a Results or MeasurementTable.
 * @author F.V. Hessman, Georg-August-Universitaet Goettingen
 * @date 2009-FEB-09
 * @version 1.0
 *
 * To create version outside of the Astronomy plugins, comment in the "// plain ..." lines and comment out the "// astroj ..." lines
 */
public class Plot_Table_Columns implements PlugIn
	{
        int maxCurves = 8;
        String title = "title";
        double titlePosX = .5;
        double titlePosY = 0.06;
        String subtitle ="subtitle";
        double subtitlePosX = 0.5;
        double subtitlePosY = 0.1;
        double legendPosX = 0.8;
        double legendPosY = 0.2;

        String xlabel = "J.D.-2400000";
        int xcolumn;
        String[] ylabel = new String[maxCurves];
        int[] ycolumn = new int[maxCurves];
        int[] errcolumn = new int[maxCurves];
        String[] oplabel = new String[maxCurves];
        int[] opcolumn = new int[maxCurves];
        String[] errlabel = new String[maxCurves];
        boolean[] showErrors = new boolean[maxCurves];
        boolean[] lines = new boolean[maxCurves];
        int[] marker = new int[maxCurves];
        Color[] color = new Color[maxCurves];
        int[] markerIndex = new int[maxCurves];
        int[] colorIndex = new int[maxCurves];
        int[] operatorIndex = new int[maxCurves];
        int plotSizeX = 500;
        int plotSizeY = 350;
        boolean[] moreOptions = new boolean [maxCurves];
        double[] yScaleFactor = new double[maxCurves];
        double[] yShiftFactor = new double[maxCurves];
        double[] baseline = new double[maxCurves];

        boolean[] mmag = new boolean[maxCurves];
        boolean[] plotY = new boolean[maxCurves];
        boolean[] useColumnName = new boolean[maxCurves];
        boolean[] useLegend = new boolean[maxCurves];
        String[] legend = new String[maxCurves];
        boolean xTics = true;
        boolean yTics = true;
        boolean xGrid = false;
        boolean yGrid = true;
        boolean xNumbers = true;
        boolean yNumbers = true;
        boolean plotAutoMode = false;
        boolean nextPanel = false;
        boolean canceled = false;
        boolean waitSecs = false;

        boolean autoScaleX = true;
        boolean useFirstX = false;
        boolean autoScaleY = true;
        double xMin = 0.0;
        double xMax = 0.0;
        double xWidth = 0.3;
        double yMin = 0.0;
        double yMax = 0.0;

        String tableName = "Results";
        String[] cblabels = new String[maxCurves];
        String[] columns;
        String[] tables;
        MeasurementTable table;

        String[] markers = new String[]
                {
                "box",
                "circle",
                "cross",
                "dot",
                "line",
                "triangle",
                "X"
                };
        String[] colors = new String[]
                {
                "black",
                "dark gray",
                "gray",
                "light gray",
                "green",
                "cyan",
                "blue",
                "magenta",
                "pink",
                "red",
                "orange",
                "yellow",
                };

        String[] operators = new String[]
                {
                "none",
                "divide by",
                "multiply by",
                "subtract",
                "add",
                };

        String[] opSymbol = new String[]
                {
                "",
                " / ",
                " * ",
                " - ",
                " + ",
                };

        ImagePlus plotImage;
    
	public void run(String arg)
		{
		if (IJ.versionLessThan("1.27t"))
			{
			IJ.showMessage ("Your version of ImageJ is too old!");
			return;
			}
                //INITIALIZE CERTAIN ARRAYS
                for (int i=0; i<maxCurves; i++)
                        {
                        ylabel[i] = "ratio1";
                        errlabel[i] = "ratio1_error";
                        oplabel[i] = "source-sky_#2";
                        color[i] = java.awt.Color.black;
                        yScaleFactor[i] = 1;
                        yShiftFactor[i] = 0;
                        baseline[i] = 0;
                        legend[i] = "";
                        }

                
                getPreferences();

		// FIND ALL TABLES

		
                tableName = Prefs.get("plot.tableName",tableName);

		
                tables = MeasurementTable.getMeasurementTableNames();

                if (tables == null || tables.length == 0)
                        {
                        IJ.showMessage ("No measurement tables to be accessed.");
                        return;
                        }

                //filter out B&C window if it exists
                String[] filteredTables = new String[tables.length-1];
                boolean bcfound = false;
                int j = 0;
                for (int i=0; i < tables.length; i++)
                        {
                        if (tables[i].equals("B&C"))
                                {
                                bcfound = true;
                                i++;
                                }
                        if (j < filteredTables.length)
                            filteredTables[j] = tables[i];
                        j++;
                        }


                if (!plotAutoMode)
                    {
                    if (bcfound)
                        {
                        tableName = filteredTables[0];

                        // IF MORE THAN ONE, ASK WHICH TABLE SHOULD BE USED

                        if (filteredTables.length > 1)
                            {
                            GenericDialog gd = new GenericDialog("Plot Table Columns");
                            gd.addMessage ("Select table to be plotted.");
                            gd.addChoice ("Table to be plotted",filteredTables,"Measurements");
                            gd.showDialog();
                            if (gd.wasCanceled()) return;
                            tableName = gd.getNextChoice();
                            }
                        }
                    else
                        {
                        tableName = tables[0];

                        // IF MORE THAN ONE, ASK WHICH TABLE SHOULD BE USED

                        if (tables.length > 1)
                            {
                            GenericDialog gd = new GenericDialog("Plot Table Columns");
                            gd.addMessage ("Select table to be plotted.");
                            gd.addChoice ("Table to be plotted",tables,"Measurements");
                            gd.showDialog();
                            if (gd.wasCanceled()) return;
                            tableName = gd.getNextChoice();
                            }
                        }
                    }

                Prefs.set("plot.tableName",tableName);

                table = MeasurementTable.getTable (tableName);

		if (table == null)
			{
			IJ.showMessage ("Cannot access table called \""+tableName+"\"");
			return ;
			}
		columns = table.getColumnHeadings().split("\t");
		if (columns == null || columns.length == 0)
			{
			IJ.showMessage ("No data columns to plot.");
			return;
			}

		xcolumn = table.getColumnIndex (xlabel);
                if (xcolumn == ResultsTable.COLUMN_NOT_FOUND)
                        xlabel = columns[0];

                for (int i=0;i<maxCurves;i++)
                        {
                        ycolumn[i] = table.getColumnIndex (ylabel[i]);
                        errcolumn[i] = table.getColumnIndex (errlabel[i]);
                        opcolumn[i] = table.getColumnIndex (oplabel[i]);
                        if (ycolumn[i] == ResultsTable.COLUMN_NOT_FOUND)
                                ylabel[i] = columns[0];
                        if (errcolumn[i] == ResultsTable.COLUMN_NOT_FOUND)
                                errlabel[i] = columns[0];
                        if (opcolumn[i] == ResultsTable.COLUMN_NOT_FOUND)
                                oplabel[i] = columns[0];
                        }
				

                if (!plotAutoMode)
                    {
                    while (!canceled)        //continue loop until cancel is pressed
                        {
                        do
                            {
                            showMainPanel();
                            if (canceled) return;
                            updatePlot();
                            if (waitSecs) waitNsecs(4000);
                            }
                        while (!nextPanel);
                        nextPanel = false;
                        if (moreOptions[0])
                            {
                            do
                                {
                                showOptionPanel();
                                if (canceled) return;
                                updatePlot();
                                if (waitSecs) waitNsecs(4000);
                                }
                            while(!nextPanel);
                            nextPanel = false;
                            }

                        for (int i=1; i<maxCurves; i++)
                            {
                            if (moreOptions[i])
                                {
                                do
                                    {
                                    showMoreCurvesPanel(i);
                                    if (canceled) return;
                                    updatePlot();
                                    if (waitSecs) waitNsecs(4000);
                                    }
                                while (!nextPanel);
                                nextPanel = false;
                                }
                            }
                        }
                    }
                updatePlot();
		return;
		}

        void updatePlot()
                {
                // CONVERT INDICES TO ATTRIBUTES
                table = MeasurementTable.getTable (tableName);
                for (int i=0;i<maxCurves;i++)
                        {
                        marker[i] = markerOf(markerIndex[i]);
                        color[i] = colorOf(colorIndex[i]);
                        }

                // INITIALIZE BASELINE FOR RELATIVE MMAG CALCULATIONS
                for (int i=0; i<maxCurves;i++)
                        baseline[i]=0;

		// GET DATA

		int n = table.getCounter();
		double[] x = new double[n];
		double[][] y = new double[maxCurves][n];
		double[][] yerr = new double[maxCurves][n];
                double[][] yop = new double [maxCurves][n];

		if (xlabel.trim().length() == 0)
			{
			for (int i=0; i < n; i++)
				x[i] = (double)(i+1);
			}
		else	{
			xcolumn = table.getColumnIndex (xlabel); // STRANGELY, THE IMAGEJ ROUTINES ARE OFF BY 1 INDEX
			if (xcolumn == ResultsTable.COLUMN_NOT_FOUND)
				{
				IJ.showMessage ("Cannot access table column "+xlabel+" !");
				return;
				}
			for (int i=0; i < n; i++)
				x[i] = table.getValueAsDouble (xcolumn,i);
			}

                for (int curve=0;curve<maxCurves;curve++)
                        {
                        if (ylabel[curve].trim().length() == 0)
                                {
                                for (int i=0; i < n; i++)
                                        y[curve][i] = (double)(i+1);
                                }
                        else	{
                                ycolumn[curve] = table.getColumnIndex (ylabel[curve]);
                                if (ycolumn[curve] == ResultsTable.COLUMN_NOT_FOUND)
                                        {
                                        IJ.showMessage ("Cannot access table column "+ylabel[curve]+" !");
                                        return;
                                        }
                                if (showErrors[curve])
                                        {
                                        errcolumn[curve] = table.getColumnIndex (errlabel[curve]);	// SEE COMMENT ABOVE
                                        if (errcolumn[curve] == ResultsTable.COLUMN_NOT_FOUND)
                                                {
                                                IJ.showMessage ("Cannot access error column "+errlabel[curve]+" !");
                                                return;
                                                }
                                        }
                                 if (operatorIndex[curve] != 0)
                                        {
                                        opcolumn[curve] = table.getColumnIndex (oplabel[curve]);	// SEE COMMENT ABOVE
                                        if (opcolumn[curve] == ResultsTable.COLUMN_NOT_FOUND)
                                                {
                                                IJ.showMessage ("Cannot access operator data column "+oplabel[curve]+" !");
                                                return;
                                                }
                                        }
                                for (int i=0; i < n; i++)
                                        {
                                        y[curve][i] = table.getValueAsDouble (ycolumn[curve],i);
                                        if (showErrors[curve])
                                                yerr[curve][i] = table.getValueAsDouble (errcolumn[curve],i);
                                        if (operatorIndex[curve] != 0)
                                                {
                                                yop[curve][i] = table.getValueAsDouble (opcolumn[curve],i);
                                                if (operatorIndex[curve] == 1)  //divide by
                                                    {
                                                    if (yop[curve][i]==0) y[curve][i] = 1.0e+100;
                                                    else y[curve][i] = y[curve][i]/yop[curve][i];
                                                    if (showErrors[curve])
                                                        {
                                                        if (yop[curve][i]==0) yerr[curve][i] = 1.0e+100;
                                                        else yerr[curve][i] = yerr[curve][i]/yop[curve][i];
                                                        }
                                                    }
                                                else if (operatorIndex[curve] == 2)  //multiply by
                                                    y[curve][i] = y[curve][i]*yop[curve][i];
                                                    if (showErrors[curve])
                                                            yerr[curve][i] = yerr[curve][i]*yop[curve][i];
                                                else if (operatorIndex[curve] == 3)  //subtract
                                                    y[curve][i] = y[curve][i]-yop[curve][i];
                                                else if (operatorIndex[curve] == 4)  //add
                                                    y[curve][i] = y[curve][i]+yop[curve][i];
                                                }
                                        }

                                if (n < 10)    //Prepare mmag calulation baseline magnitude
                                    {
                                    for (int i=0; i < n; i++)
                                        {
                                        baseline[curve] += y[curve][i];
                                        }
                                    baseline[curve] /= n;
                                    }
                                else
                                    {
                                    for (int i=0; i < 10; i++)
                                        {
                                        baseline[curve] += y[curve][i];
                                        }
                                    baseline[curve] /= 10;
                                    }
                                if (mmag[curve])
                                    {
                                    for (int i=0; i < n; i++)
                                            {
                                            if (showErrors[curve])
                                                {
                                                yerr[curve][i] = 2500.0*Math.log10(1.0+yerr[curve][i]/y[curve][i]);
                                                }
                                            y[curve][i] = 2500.0*Math.log10(y[curve][i]/baseline[curve]);
                                            }
                                    }
                                }
                        }

		double dx = 0.0;
		double dy = 0.0;
                double xPlotMin = 0.0;
                double xPlotMax = 0.0;
                double yPlotMin = 0.0;
                double yPlotMax = 0.0;
                xPlotMin = xMin;
                xPlotMax = xMax;
                yPlotMin = yMin;
                yPlotMax = yMax;
		if (autoScaleX)
			{
			xPlotMin = minOf(x);
			xPlotMax = maxOf(x);
			dx = (xPlotMax-xPlotMin)/99.0;
			}
                else if (useFirstX)
			{
			xPlotMin = x[0];
			xPlotMax = xPlotMin+xWidth;
			dx = (xWidth)/99.0;
			}

		if (autoScaleY)
			{
			yPlotMin = minOf(y[0]);
			yPlotMax = maxOf(y[0]);
			dy = (yPlotMax-yPlotMin)/99.0;
			}


		// IF MJD, SHOW DECIMAL DATE DUE TO float API OF PlotWindow

		double xOffset = 0.0;
		double yOffset = 0.0;
		int jd=0;
		String xlab = xlabel;
                String ylab = ylabel[0];
		if (mmag[0])
                    ylab = ylabel[0]+" (mmag)";

		if (xlabel.trim().equals("J.D.-2400000"))
			{
			jd = (int)xPlotMin;
			xOffset = (double)jd;
			xPlotMin -= xOffset;
			xPlotMax -= xOffset;
			for (int i=0; i < n; i++)
                                {
				x[i] -= xOffset;
                                }
			xlab = "J.D.-"+(2400000+jd);
			}
		if (ylabel[0].trim().equals("J.D.-2400000"))
			{
			jd = (int)yPlotMin;
			yOffset = (double)jd;
			yPlotMin -= yOffset;
			yPlotMax -= yOffset;
			for (int i=0; i < n; i++)
				y[0][i] -= yOffset;
			ylab = "J.D.-"+(2400000+jd);
			}

                //SCALE AND SHIFT SECONDARY CURVES TO FIT ON PLOT
                double[] yWidth = new double[maxCurves];
                double[] yMinimum = new double[maxCurves];
                double[] yMaximum = new double[maxCurves];
                double[] yMidpoint = new double[maxCurves];
                yWidth[0] = yPlotMax - yPlotMin;
                double yMid = yPlotMin +(yPlotMax-yPlotMin)/2.;
                for (int curve=1; curve<maxCurves; curve++)
                    {
                    if (plotY[curve])
                        {
                        yMinimum[curve] = minOf(y[curve]);
                        yMaximum[curve] = maxOf(y[curve]);
                        yMidpoint[curve] = (yMaximum[curve] + yMinimum[curve])/2.;
                        yWidth[curve] = yMaximum[curve] - yMinimum[curve];
                        if (yWidth[curve] == 0) yWidth[curve] = 1.0e-10;
                        for (int i=0; i<n; i++)
                            {
                            y[curve][i] = yScaleFactor[curve]*((y[curve][i]-yMidpoint[curve])*yWidth[0]/yWidth[curve]) + yMid+ yWidth[0]*yShiftFactor[curve];
                            }
                        }
                    }


		// PLOT DATA

                //Set up plot options
                String olabel = "";
                int plotOptions = 0;
//                plotOptions += ij.gui.Plot.X_FORCE2GRID;
//                plotOptions += ij.gui.Plot.Y_FORCE2GRID;
                if (xTics) plotOptions += ij.gui.Plot.X_TICKS;
                if (yTics) plotOptions += ij.gui.Plot.Y_TICKS;
                if (xGrid) plotOptions += ij.gui.Plot.X_GRID;
                if (yGrid) plotOptions += ij.gui.Plot.Y_GRID;
                if (xNumbers) plotOptions += ij.gui.Plot.X_NUMBERS;
                if (yNumbers) plotOptions += ij.gui.Plot.Y_NUMBERS;


                double[] nullX = null;
                double[] nullY = null;
		Plot plot = new Plot ("Plot of "+tableName,xlab,ylab,nullX,nullY,plotOptions);
                plot.setSize(plotSizeX, plotSizeY);
                plot.setLimits (xPlotMin-5.*dx, xPlotMax+5.*dx, yPlotMin-5.*dy, yPlotMax+5.*dy);
                double legPosY = legendPosY;
                for (int curve=0; curve<maxCurves; curve++)
                    {
                    if ( (curve==0) || plotY[curve])
                        {
                        plot.setColor (color[curve]);
                        if (marker[curve] == ij.gui.Plot.DOT)
                                plot.setLineWidth(4);
                        else
                                plot.setLineWidth(1);

                        plot.addPoints (x,y[curve],marker[curve]);
        //                plot.addErrorBars(yerr[0]);

                        plot.setLineWidth(1);

                        if (showErrors[curve])     //code to replace plot.addErrorBars
                            {               //since plot.addErrorBars only plots with lines enable
                            for (int i=0; i < n; i++)
                                {
                                plot.drawLine(x[i], y[curve][i]-yerr[curve][i], x[i], y[curve][i]+yerr[curve][i]);
                                }
                            }

                        if (lines[curve] && !(marker[curve] == ij.gui.Plot.LINE))
                            {
                             for (int i=0; i < n-1; i++)
                                {
                                plot.drawLine(x[i],y[curve][i],x[i+1],y[curve][i+1]);
                                }
                            }

                        plot.setLineWidth(1);
                        plot.setJustification(ij.process.ImageProcessor.LEFT_JUSTIFY);
                        if (useColumnName[curve])
                            {
                            if (operatorIndex[curve] == 0) olabel = "";
                            else olabel = oplabel[curve];
                            plot.setColor (color[curve]);
                            plot.addLabel(legendPosX, legPosY, ""+ylabel[curve]+opSymbol[operatorIndex[curve]]+olabel+" ("+markers[markerIndex[curve]]+")");
                            legPosY += 20./plotSizeY;
                            }
                        else if (useLegend[curve])
                            {
                            plot.setColor (color[curve]);
                            plot.addLabel(legendPosX, legPosY, ""+legend[curve]+" ("+markers[markerIndex[curve]]+")");
                            legPosY += 20./plotSizeY;
                            }
                        }
                    }

                plot.setLineWidth(1);
                plot.setJustification(ij.process.ImageProcessor.CENTER_JUSTIFY);
                plot.setColor (java.awt.Color.black);
                plot.changeFont(new java.awt.Font("bold16",java.awt.Font.BOLD,16));
                plot.addLabel(titlePosX, titlePosY,title);
                plot.changeFont(new java.awt.Font("plain14",java.awt.Font.PLAIN,14));
                plot.addLabel(subtitlePosX, subtitlePosY, subtitle);
                
//                if (lines[0]) plot.setColor (color[0]);
//                else plot.setColor (java.awt.Color.white);

                plotImage = WindowManager.getImage("Plot of "+tableName);
                
                if (plotImage == null)
                    {
                    plot.show();
                    }
                else
                    {
                    ImageProcessor ip = plot.getProcessor();
                    plotImage.setProcessor("Plot of "+tableName,ip);
                    }


                //Replot to clean up blank areas

                plotImage = WindowManager.getImage("Plot of "+tableName);
                ImageProcessor ip = plot.getProcessor();
                plotImage.setProcessor("Plot of "+tableName,ip);

                }



	/**
	 * Returns minimum of double array.
	 */
	double minOf(double[] arr)
		{
		int n=arr.length;
		double mn = arr[0];
		for (int i=1; i < n; i++)
			mn = (arr[i] < mn)? arr[i] : mn;
		return mn;
		}

	/**
	 * Returns maximum of double array.
	 */
	double maxOf(double[] arr)
		{
		int n=arr.length;
		double mx = arr[0];
		for (int i=1; i < n; i++)
			mx = (arr[i] > mx)? arr[i] : mx;
		return mx;
		}
        Color colorOf(int ci)
                {
                Color cmkr = java.awt.Color.black;
                if (ci == 0) cmkr = java.awt.Color.black;
                else if (ci == 1) cmkr = java.awt.Color.darkGray;
                else if (ci == 2) cmkr = java.awt.Color.gray;
                else if (ci == 3) cmkr = java.awt.Color.lightGray;
                else if (ci == 4) cmkr = java.awt.Color.green;
                else if (ci == 5) cmkr = java.awt.Color.cyan;
                else if (ci == 6) cmkr = java.awt.Color.blue;
                else if (ci == 7) cmkr = java.awt.Color.magenta;
                else if (ci == 8) cmkr = java.awt.Color.pink;
                else if (ci == 9) cmkr = java.awt.Color.red;
                else if (ci == 10) cmkr = java.awt.Color.orange;
                else if (ci == 11) cmkr = java.awt.Color.yellow;
                else cmkr = java.awt.Color.black;
                return cmkr;
                }

        int markerOf(int mi)
                {
                int mkr = 3;
                if      (mi == 0) mkr = ij.gui.Plot.BOX;
                else if (mi == 1) mkr = ij.gui.Plot.CIRCLE;
                else if (mi == 2) mkr = ij.gui.Plot.CROSS;
                else if (mi == 3) mkr = ij.gui.Plot.DOT;
                else if (mi == 4) mkr = ij.gui.Plot.LINE;
                else if (mi == 5) mkr = ij.gui.Plot.TRIANGLE;
                else if (mi == 6) mkr = ij.gui.Plot.X;
                else              mkr = ij.gui.Plot.DOT;
                return mkr;
                }

        void waitNsecs(int s)
                {
                ij.IJ.wait(4000);
                waitSecs = false;
                }


        void showMainPanel()
                {
                GenericDialog gd = new GenericDialog("Plot Table Columns in "+tableName);

                gd.addChoice ("X-data from column", columns, xlabel);
                gd.addChoice ("Y-data from column", columns, ylabel[0]);
                gd.addChoice ("operator", operators, operators[operatorIndex[0]]);
                gd.addChoice ("Y-operator column", columns, oplabel[0]);
                gd.addChoice ("Marker", markers, markers[markerIndex[0]]);
                gd.addChoice ("Color", colors, colors[colorIndex[0]]);
                gd.addCheckbox ("Connect markers with lines",lines[0]);
                gd.addCheckbox ("Plot_Y-data_in_relative_mmag",mmag[0]);
                gd.addCheckbox ("Show_Y-errors",showErrors[0]);
                gd.addChoice ("Y-error from column", columns, errlabel[0]);
                gd.addNumericField ("X_plot_size",plotSizeX,0,8,"pixels");
                gd.addNumericField ("Y_plot_size",plotSizeY,0,8,"pixels");
                gd.addStringField("Title", title, 40);
                gd.addSlider("Title_X-position",0.0,200.0,titlePosX*200.0);
                gd.addSlider("Title_Y-position",0.0,200.0,titlePosY*200.0);
                gd.addStringField("Subtitle", subtitle, 40);
                gd.addSlider("Subtitle_X-position",0.0,200.0,subtitlePosX*200.0);
                gd.addSlider("Subtitle_Y-position",0.0,200.0,subtitlePosY*200.0);
                gd.addCheckbox("Display legend as column name", useColumnName[0]);
                gd.addCheckbox("Display custom legend", useLegend[0]);
                gd.addStringField("Custom legend", legend[0], 40);
                gd.addSlider("Legend_X-position",0.0,200.0,legendPosX*200.0);
                gd.addSlider("Legend_Y-position",0.0,200.0,legendPosY*200.0);
                cblabels[0]="Format options";
                for (int i=1; i<maxCurves; i++)
                        cblabels[i]="Y"+(i+1)+" options";
                gd.setInsets(10,0,0);
                gd.addCheckboxGroup (maxCurves/4, maxCurves/2, cblabels, moreOptions);
                gd.setInsets(0,0,0);
                gd.addCheckbox("Close dialog for 4 seconds to allow plot repositioning",waitSecs);
                gd.setInsets(0,0,0);
                gd.addMessage("Update & advance to next enabled options panel (YES) or update only (NO)?");
                gd.setInsets(0,0,0);
                gd.enableYesNoCancel();
                gd.showDialog();
                if (gd.wasCanceled()) 
                    {
                        canceled = true;
                        return;
                    }
                else if (gd.wasOKed())
                    nextPanel = true;
                else
                    nextPanel = false;

                xlabel = gd.getNextChoice();
                ylabel[0] = gd.getNextChoice();
                operatorIndex[0] = gd.getNextChoiceIndex();
                oplabel[0] = gd.getNextChoice();
                markerIndex[0] = gd.getNextChoiceIndex();

                colorIndex[0] = gd.getNextChoiceIndex();
                lines[0] = gd.getNextBoolean();
                mmag[0] = gd.getNextBoolean();
                showErrors[0] = gd.getNextBoolean();
                errlabel[0] = gd.getNextChoice();
                plotSizeX = (int)gd.getNextNumber();
                plotSizeY = (int)gd.getNextNumber();
                title = gd.getNextString();
                titlePosX = gd.getNextNumber()/200.0;
                titlePosY = gd.getNextNumber()/200.0;
                subtitle = gd.getNextString();
                subtitlePosX = gd.getNextNumber()/200.0;
                subtitlePosY = gd.getNextNumber()/200.0;
                useColumnName[0] = gd.getNextBoolean();
                useLegend[0] = gd.getNextBoolean();
                legend[0] = gd.getNextString();
                legendPosX = gd.getNextNumber()/200.0;
                legendPosY = gd.getNextNumber()/200.0;
                for (int k=0; k<maxCurves; k++)
                        moreOptions[k] = gd.getNextBoolean();
                waitSecs = gd.getNextBoolean();
                savePreferences();
                }

        void showOptionPanel()
                {
                GenericDialog gd2 = new GenericDialog("More Plot Table Columns Options");
                gd2.addMessage(" ");
                gd2.addCheckbox ("X_tick_marks",xTics);
                gd2.addCheckbox ("Y_tick_marks",yTics);
                gd2.addMessage(" ");
                gd2.addCheckbox ("X_grid_lines",xGrid);
                gd2.addCheckbox ("Y_grid_lines",yGrid);
                gd2.addMessage(" ");
                gd2.addCheckbox ("X_numbers",xNumbers);
                gd2.addCheckbox ("Y_numbers",yNumbers);
                gd2.addMessage(" ");
                gd2.addMessage ("X-axis Options:");
                gd2.addCheckbox ("X_automatic_scaling",autoScaleX);
                gd2.addNumericField ("Minimum_X-value",xMin,2,8,"");
                gd2.addNumericField ("Maximum_X-value",xMax,2,8,"");
                gd2.setInsets(23,20,0);
                gd2.addCheckbox ("Use_first_X-value as Minimum X-value",useFirstX);
                gd2.addNumericField ("X-axis_width",xWidth,2,8,"for 'Use first X-value' mode only");
                gd2.addMessage(" ");
                gd2.addMessage("Y-axis Options:");
                gd2.addCheckbox ("Y_automatic_scaling",autoScaleY);
                gd2.addNumericField ("Minimum_Y-value",yMin,2,8,"");
                gd2.addNumericField ("Maximum_Y-value",yMax,2,8,"");
                gd2.addMessage(" ");
                gd2.addCheckboxGroup (maxCurves/4, maxCurves/2, cblabels, moreOptions);
                gd2.setInsets(0,0,0);
                gd2.addCheckbox("Close dialog for 4 seconds to allow plot repositioning",waitSecs);
                gd2.setInsets(0,0,0);
                gd2.addMessage("Update & advance to next enabled options panel (YES) or update only (NO)?");
                gd2.setInsets(0,0,0);
                gd2.enableYesNoCancel();
                gd2.showDialog();
                if (gd2.wasCanceled())
                    {
                        canceled = true;
                        return;
                    }
                else if (gd2.wasOKed())
                    nextPanel = true;
                else
                    nextPanel = false;

                xTics = gd2.getNextBoolean();
                yTics = gd2.getNextBoolean();
                xGrid = gd2.getNextBoolean();
                yGrid = gd2.getNextBoolean();
                xNumbers = gd2.getNextBoolean();
                yNumbers = gd2.getNextBoolean();

                autoScaleX = gd2.getNextBoolean();
                xMin = gd2.getNextNumber();
                xMax = gd2.getNextNumber();
                useFirstX = gd2.getNextBoolean();
                xWidth = gd2.getNextNumber();
                autoScaleY = gd2.getNextBoolean();
                yMin = gd2.getNextNumber();
                yMax = gd2.getNextNumber();
                for (int k=0; k<maxCurves; k++)
                        moreOptions[k] = gd2.getNextBoolean();
                waitSecs = gd2.getNextBoolean();
                savePreferences();
                }

        void showMoreCurvesPanel(int i)
                {
                GenericDialog gd3 = new GenericDialog("Y"+(i+1)+" Plotting Options");
                gd3.addMessage(" ");
                gd3.addMessage(" ");
                gd3.addCheckbox ("Plot_Y"+(i+1)+"-data",plotY[i]);
                gd3.addMessage(" ");
                gd3.addChoice ("Y"+(i+1)+"-data from column", columns, ylabel[i]);
                gd3.addChoice ("operator", operators, operators[operatorIndex[i]]);
                gd3.addChoice ("Y"+(i+1)+"-operator column", columns, oplabel[i]);
                gd3.addMessage(" ");
                gd3.addChoice ("Marker", markers, markers[markerIndex[i]]);
                gd3.addChoice ("Color", colors, colors[colorIndex[i]]);
                gd3.addCheckbox ("Connect markers with lines",lines[i]);
                gd3.addCheckbox ("Use_relative_mmag_for_Y"+(i+1)+"-data",mmag[i]);
                gd3.addMessage(" ");
                gd3.addSlider("Scale_by (%_of Y-range)",0.0,200.0,yScaleFactor[i]*100.0);
                gd3.addSlider("Shift (%_of Y-range)",-100.0,100.0,yShiftFactor[i]*100.0);
                gd3.addCheckbox ("Show_Y"+(i+1)+"-errors",showErrors[i]);
                gd3.addChoice ("Y"+(i+1)+"-error from column", columns, errlabel[i]);
                gd3.addMessage(" ");
                gd3.addCheckbox ("Display legend as column name", useColumnName[i]);
                gd3.addCheckbox ("Display custom legend", useLegend[i]);
                gd3.addStringField("Custom legend", legend[i], 20);
                gd3.addMessage(" ");
                gd3.addCheckboxGroup (maxCurves/4, maxCurves/2, cblabels, moreOptions);
                gd3.setInsets(0,0,0);
                gd3.addCheckbox("Close dialog for 4 seconds to allow plot repositioning",waitSecs);
                gd3.setInsets(0,0,0);
                gd3.addMessage("Update & advance to next enabled options panel (YES) or update only (NO)?");
                gd3.setInsets(0,0,0);
                gd3.enableYesNoCancel();
                gd3.showDialog();
                if (gd3.wasCanceled())
                    {
                        canceled = true;
                        return;
                    }
                else if (gd3.wasOKed())
                    nextPanel = true;
                else
                    nextPanel = false;

                plotY[i] = gd3.getNextBoolean();
                ylabel[i] = gd3.getNextChoice();
                operatorIndex[i] = gd3.getNextChoiceIndex();
                oplabel[i] = gd3.getNextChoice();
                markerIndex[i] = gd3.getNextChoiceIndex();
                colorIndex[i] = gd3.getNextChoiceIndex();
                lines[i] = gd3.getNextBoolean();
                mmag[i] = gd3.getNextBoolean();
                yScaleFactor[i] = gd3.getNextNumber()/100.0;
                yShiftFactor[i] = gd3.getNextNumber()/100.0;
                showErrors[i] = gd3.getNextBoolean();
                errlabel[i] = gd3.getNextChoice();
                useColumnName[i] = gd3.getNextBoolean();
                useLegend[i] = gd3.getNextBoolean();
                legend[i] = gd3.getNextString();
                for (int k=0; k<maxCurves; k++)
                        moreOptions[k] = gd3.getNextBoolean();
                waitSecs = gd3.getNextBoolean();
                savePreferences();
                }

        void getPreferences()
                {
		plotAutoMode=Prefs.get("plot.automode",plotAutoMode);
                xlabel=Prefs.get("plot.xlabel",xlabel);
                title=Prefs.get("plot.title", title);
                subtitle=Prefs.get("plot.subtitle", subtitle);
                titlePosX=Prefs.get("plot.titlePosX",titlePosX);
                titlePosY=Prefs.get("plot.titlePosY",titlePosY);
                subtitlePosX=Prefs.get("plot.subtitlePosX",subtitlePosX);
                subtitlePosY=Prefs.get("plot.subtitlePosY",subtitlePosY);
                legendPosX=Prefs.get("plot.legendPosX",legendPosX);
                legendPosY=Prefs.get("plot.legendPosY",legendPosY);
                xTics=Prefs.get("plot.xTics", xTics);
                yTics=Prefs.get("plot.yTics", yTics);
                xGrid=Prefs.get("plot.xGrid", xGrid);
                yGrid=Prefs.get("plot.yGrid", yGrid);
                xNumbers=Prefs.get("plot.xNumbers", xNumbers);
                yNumbers=Prefs.get("plot.yNumbers", yNumbers);
                autoScaleX=Prefs.get("plot.autoScaleX",autoScaleX);
                useFirstX=Prefs.get("plot.useFirstX",useFirstX);
                autoScaleY=Prefs.get("plot.autoScaleY",autoScaleY);
                xMin=Prefs.get("plot.xMin", xMin);
                xMax=Prefs.get("plot.xMax", xMax);
                xWidth=Prefs.get("plot.xWidth", xWidth);
                yMin=Prefs.get("plot.yMin", yMin);
                yMax=Prefs.get("plot.yMax", yMax);
                plotSizeX=(int)Prefs.get("plot.plotSizeX", plotSizeX);
                plotSizeY=(int)Prefs.get("plot.plotSizeY", plotSizeY);

                for (int i=0; i<maxCurves; i++)
                        {
                        moreOptions[i]=Prefs.get("plot.moreOptions"+i,moreOptions[i]);
                        errlabel[i]=Prefs.get("plot.errlabel"+i,errlabel[i]);
                        showErrors[i]=Prefs.get("plot.showErrors"+i,showErrors[i]);
                        ylabel[i]=Prefs.get("plot.ylabel"+i,ylabel[i]);
                        lines[i]=Prefs.get("plot.lines"+i, lines[i]);
                        markerIndex[i]=(int)Prefs.get("plot.markerIndex"+i,markerIndex[i]);
                        colorIndex[i]=(int)Prefs.get("plot.colorIndex"+i, colorIndex[i]);
                        mmag[i]=Prefs.get("plot.mmag"+i, mmag[i]);
                        useColumnName[i]=Prefs.get("plot.useColumnName"+i, useColumnName[i]);
                        useLegend[i]=Prefs.get("plot.useLegend"+i, useLegend[i]);
                        legend[i] = Prefs.get("plot.legend"+i, legend[i]);
                        oplabel[i]=Prefs.get("plot.oplabel"+i, oplabel[i]);
                        operatorIndex[i]=(int)Prefs.get("plot.operatorIndex"+i, operatorIndex[i]);
                        }
                 for (int i=1; i<maxCurves; i++)
                        {
                        plotY[i] = Prefs.get("plot.plotY"+i,plotY[i]);
                        yScaleFactor[i]=Prefs.get("plot.yScaleFactor"+i,yScaleFactor[i]);
                        yShiftFactor[i]=Prefs.get("plot.yShiftFactor"+i,yShiftFactor[i]);
                        

                        }
                }
        
        
        
        void savePreferences()
                {
                Prefs.set("plot.xlabel",xlabel);
                Prefs.set("plot.xTics", xTics);
                Prefs.set("plot.yTics", yTics);
                Prefs.set("plot.xGrid", xGrid);
                Prefs.set("plot.yGrid", yGrid);
                Prefs.set("plot.xNumbers", xNumbers);
                Prefs.set("plot.yNumbers", yNumbers);
                Prefs.set("plot.autoScaleX", autoScaleX);
                Prefs.set("plot.autoScaleY", autoScaleY);
                Prefs.set("plot.useFirstX", useFirstX);
                Prefs.set("plot.xWidth", xWidth);
                Prefs.set("plot.xMin", xMin);
                Prefs.set("plot.xMax", xMax);
                Prefs.set("plot.yMin", yMin);
                Prefs.set("plot.yMax", yMax);
                Prefs.set("plot.plotSizeX",plotSizeX);
                Prefs.set("plot.plotSizeY",plotSizeY);
                Prefs.set("plot.title",title);
                Prefs.set("plot.subtitle",subtitle);
                Prefs.set("plot.titlePosX",titlePosX);
                Prefs.set("plot.titlePosY",titlePosY);
                Prefs.set("plot.subtitlePosX",subtitlePosX);
                Prefs.set("plot.subtitlePosY",subtitlePosY);

                Prefs.set("plot.legendPosX",legendPosX);
                Prefs.set("plot.legendPosY",legendPosY);


                for (int i=0; i<maxCurves; i++)
                        {
                        Prefs.set("plot.ylabel"+i,ylabel[i]);
                        Prefs.set("plot.errlabel"+i,errlabel[i]);
                        Prefs.set("plot.showErrors"+i,showErrors[i]);
                        Prefs.set("plot.lines"+i, lines[i]);
                        Prefs.set("plot.markerIndex"+i,markerIndex[i]);
                        Prefs.set("plot.colorIndex"+i,colorIndex[i]);
                        Prefs.set("plot.mmag"+i, mmag[i]);
                        Prefs.set("plot.useColumnName"+i,useColumnName[i]);
                        Prefs.set("plot.useLegend"+i,useLegend[i]);
                        Prefs.set("plot.legend"+i,legend[i]);
                        Prefs.set("plot.oplabel"+i,oplabel[i]);
                        Prefs.set("plot.operatorIndex"+i,operatorIndex[i]);
                        Prefs.set("plot.moreOptions"+i,moreOptions[i]);
                        }
                for (int i=1; i<maxCurves; i++)
                        {
                        Prefs.set("plot.plotY"+i,plotY[i]);
                        Prefs.set("plot.yScaleFactor"+i,yScaleFactor[i]);
                        Prefs.set("plot.yShiftFactor"+i,yShiftFactor[i]);
                        }

                }
        
	}

