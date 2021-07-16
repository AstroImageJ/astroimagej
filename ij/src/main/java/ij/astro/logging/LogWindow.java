package ij.astro.logging;

import ij.IJ;
import ij.Prefs;
import ij.WindowManager;
import ij.gui.GUI;
import ij.text.TextWindow;

import java.awt.*;
import java.util.Arrays;

/**
 * Custom TextWindow for use with {@link AIJLogger}'s logging capabilities - implements closing and screen pos/size of
 * log window.
 */
public class LogWindow extends TextWindow {
    public LogWindow(String path, String text, int width, int height) {
        super(path, text, width, height);
        var loc = Prefs.getLocation(LOG_LOC_KEY);
        var w = (int)Prefs.get(LOG_WIDTH_KEY, width);
        var h = (int)Prefs.get(LOG_HEIGHT_KEY, height);

        if (loc!=null&&w>0 && h>0) {
            //todo improve offset generation
            var openLogWindows = Arrays.stream(WindowManager.getNonImageTitles())
                    .filter(s -> s.toLowerCase().contains("log")).count();
            if (!Prefs.isLocationOnScreen(loc)) {
                openLogWindows *= -1;
            }
            loc.translate((int) (openLogWindows*50), (int) (openLogWindows*50));

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
        AIJLogger.removePanel(getTextPanel());
        Prefs.saveLocation(LOG_LOC_KEY, getLocation());
        Dimension d = getSize();
        Prefs.set(LOG_WIDTH_KEY, d.width);
        Prefs.set(LOG_HEIGHT_KEY, d.height);
    }
}
