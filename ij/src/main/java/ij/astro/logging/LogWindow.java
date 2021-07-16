package ij.astro.logging;

import ij.IJ;
import ij.Prefs;
import ij.gui.GUI;
import ij.text.TextWindow;

import java.awt.*;

/**
 * Custom TextWindow for use with {@link AIJ}'s logging capabilities - implements closing and screen pos/size of
 * log window.
 */
public class LogWindow extends TextWindow {
    public LogWindow(String path, String text, int width, int height) {
        super(path, text, width, height);
        var loc = Prefs.getLocation(LOG_LOC_KEY);
        var w = (int)Prefs.get(LOG_WIDTH_KEY, width);
        var h = (int)Prefs.get(LOG_HEIGHT_KEY, height);

        if (loc!=null&&w>0 && h>0) {
            setSize(w, h);
            setLocation(loc);
        } else {
            setSize(width, height);
            if (!IJ.debugMode) GUI.centerOnImageJScreen(this);
        }
    }

    @Override
    public void close() {
        super.close();
        AIJ.removePanel(getTextPanel());
        Prefs.saveLocation(LOG_LOC_KEY, getLocation());
        Dimension d = getSize();
        Prefs.set(LOG_WIDTH_KEY, d.width);
        Prefs.set(LOG_HEIGHT_KEY, d.height);
    }
}
