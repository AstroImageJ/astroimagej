package Astronomy;// Set_Telescope.java

import ij.*;
import ij.gui.*;
import ij.plugin.*;
import ij.process.*;

import java.awt.*;
import java.awt.event.*;

/**
 * Setup plug-in for storing telescope info.
 *
 * @author F.V. Hessman, Georg-August-Universitaet Goettingen
 * @version 1.0
 * @date 2008-Feb-07
 */
public class Set_Telescope implements PlugIn
	{
	public static String PREFS_TELESCOPE_ENTRIES = new String("telescope.entries");

	protected TextField[][] db;

	// FORMAT IS: NAME$APERTURE$FOCALLENGTH$LONGITUDE$LATITUDE$HEIGHT

	// STUFF TAKEN FROM THE Dialog_Grid_Demo PLUGIN EXAMPLE

	int ntels = 0;
	int gridWidth = 6;
	int gridHeight = 2;	// MINIMUM, INCLUDING TOP LABELS AND EMPTY INPUT ROW

	/**
	 * Standard ImageJ PluginFilter setup routine which retrieves, displays, and re-stores telescope information.
	 */
	public void run (String arg)
		{
		if (IJ.versionLessThan("1.311")) return;

		String[] tel;

		String t = Prefs.get (PREFS_TELESCOPE_ENTRIES,"");
IJ.log("entries="+t);
		String[] entries = t.split("$");
		int l = entries.length+1;				// EXTRA ONE IS A BLANK FOR A NEW ENTRY
		db = new TextField[l][6];
		for (int i=0; i < l-1; i++)
			{
			t = Prefs.get("telescope."+entries[i],"?");
			if (!t.equals("?"))
				{
				tel = t.split("$");
				if (tel.length == 6)
					{
					for (int k=0; k < 6; k++)
						db[ntels][k] = new TextField(tel[k]);
					gridHeight++;
					ntels++;
					}
				}
			}
		for (int k=0; k < 6; k++)
			db[ntels][k] = new TextField(" ");

		GenericDialog gd = new GenericDialog ("Telescope Database");
		gd.addPanel(makePanel(gd));

		gd.showDialog();
		if (gd.wasCanceled()) return;

		t = getEntries();
IJ.log("final entries: "+t);
		entries = t.split("$");
		Prefs.set (PREFS_TELESCOPE_ENTRIES,t);

		l = entries.length;
		String[] tels = getTelescopes(l);
		for (int i=0; i < l; i++)
			{
IJ.log("telescope."+entries[i]+"="+tels[i]);
			Prefs.set ("telescope."+entries[i],tels[i]);
			}
		}

	boolean isOK(TextField[] tf)
		{
		String name;
		double aperture,focallength,longitude,latitude,height;
		name = tf[0].getText();
		if (name.length() <= 0) return false;
		try	{
			aperture    = Double.parseDouble(tf[1].getText());
			focallength = Double.parseDouble(tf[2].getText());
			longitude   = Double.parseDouble(tf[3].getText());
			latitude    = Double.parseDouble(tf[4].getText());
			height      = Double.parseDouble(tf[5].getText());
			}
		catch (NumberFormatException e)
			{
			return false;
			}
		return true;
		}

	String getEntries()
		{
		int num=0;
		String e = "";
		for (int i=0; i <= ntels; i++)
			{
			if (isOK(db[i]))
				{
				String name = formatName(db[i][0].getText());
				if (num == 0)
					e += name;
				else
					e += "$"+name;
				num++;
				}
			}
		return e;
		}

	String formatName (String name)
		{
		String s = new String(name);
		s.replaceAll(" ","_");
		s.replaceAll("/","_");
		return s;
		}

	String[] getTelescopes(int num)
		{
		int n=0;
		String tel;
		String[] tels = new String[num];
		for (int i=0; i <= ntels; i++)
			{
			if (isOK(db[i]))
				{
				tel = db[i][0].getText();
				for (int k=1; k < 6; k++)
					tel += "$"+db[i][k].getText();
				tels[n] = tel;
				n++;
				}
			}
		return tels;
		}

	Panel makePanel (GenericDialog gd)
		{
		Panel panel = new Panel();
		panel.setLayout(new GridLayout(gridHeight,gridWidth));

		panel.add(new Label("NAME"));
		panel.add(new Label("APERTURE [m]"));
		panel.add(new Label("FOCAL LENGTH [m]"));
		panel.add(new Label("EAST LONGITUDE [deg]"));
		panel.add(new Label("LATITUDE [deg]"));
		panel.add(new Label("HEIGHT [m]"));

		for (int j=0; j <= ntels; j++)
			{
			for (int i=0; i < 6; i++)
				panel.add(db[j][i]);
			}
		return panel;
		}

	}
