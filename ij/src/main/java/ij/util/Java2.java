package ij.util;

import ij.IJ;
import ij.astro.AstroImageJ;

import javax.swing.*;
import javax.swing.text.DefaultEditorKit;
import java.awt.*;
import java.awt.event.KeyEvent;

/**
This class contains static methods that use the Java 2 API. They are isolated 
here to prevent errors when ImageJ is running on Java 1.1 JVMs.
*/
public class Java2 {

	public static void setAntialiased(Graphics g, boolean antialiased) {
			Graphics2D g2d = (Graphics2D)g;
			if (antialiased)
				g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			else
				g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
	}

	public static void setAntialiasedText(Graphics g, boolean antialiasedText) {
		Graphics2D g2d = (Graphics2D)g;
		if (antialiasedText) {
			//if (IJ.isMacOSX())
			//	g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
			g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB);
		} else
			g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_OFF);
	}

	public static int getStringWidth(String s, FontMetrics fontMetrics, Graphics g) {
			java.awt.geom.Rectangle2D r = fontMetrics.getStringBounds(s, g);
			return (int)r.getWidth();
	}

	public static void setBilinearInterpolation(Graphics g, boolean bilinearInterpolation) {
			Graphics2D g2d = (Graphics2D)g;
			if (bilinearInterpolation)
				g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
			else
				g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
	}
	
	/** Sets the Swing look and feel to the system look and feel (Windows only). */
	@AstroImageJ(reason = "update keymapping on mac for copy/paste", modified = true)
	public static void setSystemLookAndFeel() {
		if (!IJ.isWindows())
			return;
		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch(Throwable t) {}
	}

	/** Sets the Swing look and feel. */
	@AstroImageJ(reason = "update keymapping on mac for copy/paste", modified = true)
	public static void setLookAndFeel(LookAndFeel newLookAndFeel) {
		if (IJ.isMacOSX()) {
			InputMap im = (InputMap) UIManager.get("TextField.focusInputMap");
			im.put(KeyStroke.getKeyStroke(KeyEvent.VK_C, KeyEvent.META_DOWN_MASK), DefaultEditorKit.copyAction);
			im.put(KeyStroke.getKeyStroke(KeyEvent.VK_V, KeyEvent.META_DOWN_MASK), DefaultEditorKit.pasteAction);
			im.put(KeyStroke.getKeyStroke(KeyEvent.VK_X, KeyEvent.META_DOWN_MASK), DefaultEditorKit.cutAction);
		}
		if (!IJ.isWindows() || newLookAndFeel==null)
			return;
		try {
			UIManager.setLookAndFeel(newLookAndFeel);
		} catch(Throwable t) {}
	}

	/** Returns the current Swing look and feel or null. */
	public static LookAndFeel getLookAndFeel() {
		if (!IJ.isWindows())
			return null;
		return UIManager.getLookAndFeel();
	}


}

