package Astronomy;//Astronomy_Listener.java

import astroj.AstroCanvas;
import astroj.AstroStackWindow;
import ij.IJ;
import ij.ImageListener;
import ij.ImagePlus;
import ij.Prefs;
import ij.plugin.PlugIn;

import javax.swing.*;
import java.awt.*;
import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * @author K.A. Collins, University of Louisville
 * @version 1.0
 * @date 2010-Jul-25
 */
public class Astronomy_Listener implements PlugIn, ImageListener {
    public static final boolean REFRESH = true;
    public static final boolean NEW = false;
    public static final boolean RESIZE = true;
    public static final boolean NORESIZE = false;
    final static Executor EXECUTOR_SERVICE = Executors.newCachedThreadPool();

    public void run(String arg) {
        if (IJ.versionLessThan("1.35l")) return;
        ImagePlus.addImageListener(this);
    }

    public void imageOpened(ImagePlus imp) {
        boolean autoConvert = true;
        Frame openFrame = imp.getWindow();

        if (openFrame instanceof ij.gui.PlotWindow || imp.getTitle().startsWith("About") ||
                imp.getTitle().startsWith("Profile of") || imp.getTitle().startsWith("Seeing Profile")) {
            return;
        }
        autoConvert = Prefs.get("Astronomy_Tool.autoConvert", autoConvert);
        if (autoConvert) {
            if (!(openFrame instanceof astroj.AstroStackWindow)) {
                var o = imp.getWindow();
                AstroCanvas ac = new AstroCanvas(imp);
                imp.setWindow(new AstroStackWindow(imp, ac, NEW, RESIZE));
                if (o != null) {
                    o.close();
                }
            }
        }

    }

    public void imageClosed(ImagePlus imp) {
    }

    public void imageUpdated(ImagePlus imp) {
        Frame openFrame = imp.getWindow();
        if (openFrame instanceof ij.gui.PlotWindow || imp.getTitle().startsWith("About ImageJ") ||
                imp.getTitle().startsWith("Profile of") || imp.getTitle().startsWith("Seeing Profile")) {
            return;
        }

        if (openFrame instanceof AstroStackWindow asw) {
            if (asw.isReady && !asw.minMaxChanged) {
                if (IJ.isMacro()) {
                    if (!SwingUtilities.isEventDispatchThread()) {
                        try {
                            SwingUtilities.invokeAndWait(() -> {
                                if (asw.isReady) {
                                    asw.setAstroProcessor(false);
                                }
                            });
                        } catch (InterruptedException | InvocationTargetException e) {
                            throw new RuntimeException(e);
                        }
                    } else {
                        asw.setAstroProcessor(false);
                    }
                    return;
                }
                EXECUTOR_SERVICE.execute(() -> {
                    if (IJ.isMacro()) imp.waitTillActivated();
                    asw.setAstroProcessor(false);
                });
            } else {
                asw.minMaxChanged = false;
            }
        }

    }

}

