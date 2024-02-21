package Astronomy;//Astronomy_Tool.java

import astroj.AstroCanvas;
import astroj.AstroStackWindow;
import ij.IJ;
import ij.ImagePlus;
import ij.WindowManager;
import ij.plugin.PlugIn;

import java.awt.*;

/**
 * @author K.A. Collins, University of Louisville
 * @version 1.0
 * @date 2010-Jul-25
 */
public class Astronomy_Tool implements PlugIn //, ImageListener
//public class Astronomy_Tool implements PlugInFilter, MouseListener,
//        MouseMotionListener, MouseWheelListener, ImageListener
{
    public static final boolean REFRESH = true;
    public static final boolean NEW = false;
    public static final boolean RESIZE = true;
    public static final boolean NORESIZE = false;
    boolean resizeNoResize = true;

    // PlugInFilter METHODS

//	public int setup (String arg, ImagePlus im)
//		{
//		imp = im;
//		IJ.register(Astronomy_Tool.class);
//		return NO_IMAGE_REQUIRED+DOES_ALL;
//		}

    public void run(String s) {
        Frame openFrame;
        ImagePlus imp;
        if (s == null || s.isEmpty()) {
            openFrame = WindowManager.getCurrentWindow();
            imp = WindowManager.getCurrentImage();
        } else {
            openFrame = WindowManager.getFrame(s);
            imp = WindowManager.getImage(s);
        }

        if (imp != null) {
            if (openFrame instanceof ij.gui.PlotWindow || imp.getTitle().startsWith("About") ||
                    imp.getTitle().startsWith("Profile of") || imp.getTitle().startsWith("Seeing Profile")) {
                return;
            }

            if (!(openFrame instanceof AstroStackWindow)) {
                resizeNoResize = RESIZE;
            } else {
                resizeNoResize = NORESIZE;
            }
            AstroCanvas ac = new AstroCanvas(imp);
            AstroStackWindow astroWindow = new AstroStackWindow(imp, ac, NEW, resizeNoResize);
        } else {
            IJ.error("No images are open!");
        }
    }
}

//
//public void imageOpened(ImagePlus imp) {
//    // called when an image is opened
////    IJ.log(imp.getTitle() + " opened");
//    autoConvert = Prefs.get("Astronomy_Tool.autoConvert", autoConvert);
//    if (autoConvert)
//        {
//        openFrame = imp.getWindow();
//        Class<?> mainComponentClass = openFrame.getClass();
//        if (mainComponentClass.getName() != "astroj.AstroStackWindow")
//            {
////            ImagePlus.removeImageListener(this);
////            IJ.runPlugIn("Astronomy_Tool","");
//            Class<?> imageWindowClass = openFrame.getClass();
//            if (imageWindowClass.getName() != "astroj.AstroStackWindow")
//                    resizeNoResize = RESIZE;
//            else
//                    resizeNoResize = NORESIZE;
//            ic = OverlayCanvas.getOverlayCanvas(imp);
//            astroWindow = new AstroStackWindow(imp, ic, NEW, resizeNoResize);
//            openFrame.addWindowListener(new WindowAdapter(){
//                    @Override
//                    public void windowClosing(WindowEvent e){
//                    openFrame.dispose(); }});
//            }
//        }
//    }
//
//public void imageClosed(ImagePlus imp) {
//    // Called when an image is closed
//
////    IJ.log(imp.getTitle() + " closed");
//    }
//
//public void imageUpdated(ImagePlus imp) {
//    // Called when an image's pixel data is updated
////    IJ.log("imageUpdated");
////    boolean autoLevel = Prefs.get("Astronomy_Tool.startupAutoLevel", false);
////    String title = imp.getTitle();
////    Frame frame = WindowManager.getFrame(title);
////    Class<?> imageClass = frame.getClass();
////    if (imageClass.getName().contains("AstroStackWindow"))
////        {
////        astroj.AstroStackWindow csw = (astroj.AstroStackWindow) frame;
////        if (autoLevel) csw.setAutoLevels(title);
////        }
////    IJ.log(imp.getTitle() + " updated");
//    }
//}
//
