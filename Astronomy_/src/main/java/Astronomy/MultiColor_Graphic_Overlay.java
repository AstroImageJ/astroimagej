package Astronomy;

import ij.*;
import ij.process.*;
import ij.gui.*;
import ij.plugin.*;
import java.awt.*;
import java.awt.geom.*;
import java.util.*;

/** This plugin demonstrates how to display color objects in a non-destructive graphic overlay. */
public class MultiColor_Graphic_Overlay implements PlugIn {

	public void run(String arg) {
		if (IJ.versionLessThan("1.39f")) return;
		ImagePlus img = IJ.getImage();
		ImageCanvas ic = img.getCanvas();
		Vector list = new Vector();

		// display two red lines
		GeneralPath path = new GeneralPath();
		path.moveTo(10.5f, 10.5f);
		path.lineTo(100.5f, 100.5f);
		path.moveTo(10.5f, 70.5f);
		path.lineTo(80.5f, 110.5f);
		addElement(list, path, Color.red, 2);

		// display a green circle
		addElement(list, new Ellipse2D.Float(25.5f, 120.5f, 50f, 50f), Color.green, 4);
		ic.setDisplayList(list);

		// wait 2 seconds then display a blue box
		IJ.wait(2000);
		addElement(list, new Rectangle2D.Float(120.5f, 20.5f, 50f, 50f), Color.blue, 8);
		ic.repaint();

		// wait 2 seconds then remove green circle
		IJ.wait(2000);
		list.remove(1);
		ic.repaint();
	}

	void addElement(Vector list, Shape shape, Color color, int lineWidth) {
		Roi roi = new ShapeRoi(shape);
		roi.setInstanceColor(color);
		roi.setLineWidth(lineWidth);
		list.addElement(roi);
	}

}
