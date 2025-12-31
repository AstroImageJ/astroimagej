package astroj;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.awt.event.MouseEvent;
import java.awt.font.FontRenderContext;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferStrategy;
import java.awt.image.MemoryImageSource;
import java.text.DecimalFormat;
import java.util.Locale;

import javax.swing.SwingUtilities;

import ij.IJ;
import ij.ImagePlus;
import ij.Prefs;
import ij.gui.Arrow;
import ij.gui.ImageWindow;
import ij.gui.Roi;
import ij.gui.Toolbar;
import ij.plugin.tool.PlugInTool;

public class AstroCanvas extends OverlayCanvas {

    public static final int ROT_0 = 0;
    public static final int ROT_90 = -90;
    public static final int ROT_180 = -180;
    public static final int ROT_270 = -270;
    public boolean transEnabled = true;
    public boolean showRedCrossHairCursor = true;
    public boolean showAnnotations = true;
    public AffineTransform canvTrans = new AffineTransform(); //default identity transform
    public AffineTransform invCanvTrans = new AffineTransform(); //default identity transform
    int nXShift = -13, nYShift = 10, eXShift = 2, eYShift = -5;
    double NdirAngle = 0.0, EdirAngle = 90.0;
    double XPixelScale = 0.0, YPixelScale = 0.0;
    double sinEl = 0, cosEl = 0, sinNl = 0, cosNl = 0;
    int sinEli = 0, cosEli = 0, sinNli = 0, cosNli = 0;
    double dtr = Math.PI / 180.0;
    double angNr, angEr;
    double sinNa, sinEa;
    double cosNa, cosEa;
    int[] xarrowheadX = new int[3];
    int[] yarrowheadX = new int[3];
    int[] xarrowheadY = new int[3];
    int[] yarrowheadY = new int[3];
    int[] xarrowheadE = new int[3];
    int[] yarrowheadE = new int[3];
    int[] xarrowheadN = new int[3];
    int[] yarrowheadN = new int[3];
    double aspectRatio;
    int h1, w1, h2, w2;
    int x1, y1, x2, y2, xc, yc;
    Color mouseApertureColor = new Color(128, 128, 255);
    Cursor redCrossHairCursor, clearCursor;
    Font p12 = new Font(Font.SANS_SERIF, Font.PLAIN, 12);
    Font p16 = new Font(Font.MONOSPACED, Font.PLAIN, 20);
    DecimalFormat twoplaces = new DecimalFormat("0.00", IJU.dfs);    int zoomIndicatorSize = 100, len = zoomIndicatorSize / 2;
    DecimalFormat onePlace = new DecimalFormat("0.0", IJU.dfs);
    DecimalFormat noPlaces = new DecimalFormat("0", IJU.dfs);
    private boolean flipX;
    private boolean flipY;
    private int rotation;
    private boolean showZoom;
    private boolean showDir;
    private boolean showXY;
    private boolean showXScale, showYScale;
    private boolean showSkyOverlay = true;
    private boolean astronomyMode;
    private WCS wcs = null;

    //the internal states yielding proper orientation of a canvas given flipX, flipY, and rotation settings
    private boolean netRotate; // netRotate = rotate -90 degrees - other rotations are accomplished with netFlipX/Y
    private boolean netFlipX;  // netFlipX is the net required X-axis orientation
    private boolean netFlipY;  // netFlipY is the net required Y-axis orientation
    private boolean oldNetRotate;
    private boolean mouseInImage;
    private boolean showPhotometerCursor;
    private boolean showAbsMag = true;
    private boolean showIntCntWithAbsMag = true;
    private int screenX;
    private int screenY;
    private double imageX;
    private double imageY;
    private double radius, rBack1, rBack2;

    private boolean isTransformed;
    private Color zoomIndicatorColor = new Color(128, 128, 255);
    private final Color axisIndicatorColor = new Color(0, 175, 0);
    private final Color dirIndicatorColor = new Color(238, 0, 28);
    private final Color dirIndicatorColorWCS = new Color(255, 190, 0);//(255,129,35);
    private boolean performDraw = true;
    private boolean customPixelMode;

    public AstroCanvas(ImagePlus imp) {
        super(imp);
        Locale.setDefault(IJU.locale);
//            if (IJ.isWindows())
//                    {
////                        try {UIManager.setLookAndFeel("javax.swing.plaf.metal.MetalLookAndFeel");}
//                    try {UIManager.setLookAndFeel("com.sun.java.swing.plaf.windows.WindowsLookAndFeel");}
////                        try {UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());}
//                    catch (Exception e) { }
//                    }
//            else if (IJ.isLinux())
//                    {
//                    try {UIManager.setLookAndFeel("javax.swing.plaf.metal.MetalLookAndFeel");}
//                    catch (Exception e) { }
//                    }
////                    try {UIManager.setLookAndFeel("com.sun.java.swing.plaf.windows.WindowsLookAndFeel");}
////                    try {UIManager.setLookAndFeel("com.sun.java.swing.plaf.nimbus.NimbusLookAndFeel");}
////                    try {UIManager.setLookAndFeel("com.sun.java.swing.plaf.motif.MotifLookAndFeel");}
//            else
//                    {
//
//                    try {UIManager.setLookAndFeel("javax.swing.plaf.metal.MetalLookAndFeel");}
////                        try {UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());}
//                    catch (Exception e) { }
//                    }
        buildRedCrossHairCursor();
        buildClearCursor();

        AstrometrySetup.EXCLUDE_BORDERS.addListener(this, (_, _) -> updateDisplay());
        AstrometrySetup.BORDER_EXCLUSION_TOP.addListener(this, (_, _) -> updateDisplay());
        AstrometrySetup.BORDER_EXCLUSION_BOTTOM.addListener(this, (_, _) -> updateDisplay());
        AstrometrySetup.BORDER_EXCLUSION_LEFT.addListener(this, (_, _) -> updateDisplay());
        AstrometrySetup.BORDER_EXCLUSION_RIGHT.addListener(this, (_, _) -> updateDisplay());
        AstrometrySetup.DISPLAY_EXCLUDED_BORDERS.addListener(this, (_, _) -> updateDisplay());
    }

    private void updateDisplay() {
        if (SwingUtilities.isEventDispatchThread()) {
            repaint();
        } else {
            SwingUtilities.invokeLater(this::repaint);
        }
    }

    void buildRedCrossHairCursor() {
        int curWidth = 33;
        int curHeight = 33;
        int x;
        int y;
        int[] pix = new int[curWidth * curHeight];
        for (y = 0; y < curHeight; y++)
            for (x = 0; x < curWidth; x++)
                pix[y + x] = 0; // all points transparent

        int curCol = Color.RED.getRGB();

        for (x = 0; x < curWidth; x++)
            pix[(curWidth) * (curHeight / 2 + 1) + x] = curCol;
        for (y = 0; y < curHeight; y++)
            pix[(curWidth / 2 + 1) + (curWidth) * y] = curCol;

        Image img = createImage(new MemoryImageSource(curWidth, curHeight, pix, 0, curWidth));
        redCrossHairCursor = Toolkit.getDefaultToolkit().createCustomCursor(img, new Point(16, 16), "red cross-hair");
    }

    void buildClearCursor() {
        int curWidth = 32;
        int curHeight = 32;
        int x;
        int y;
        int[] pix = new int[curWidth * curHeight];
        for (y = 0; y < curHeight; y++)
            for (x = 0; x < curWidth; x++)
                pix[y + x] = 0; // all points transparent

        Image img = createImage(new MemoryImageSource(curWidth, curHeight, pix, 0, curWidth));
        clearCursor = Toolkit.getDefaultToolkit().createCustomCursor(img, new Point(curWidth / 2, curHeight / 2), "clear cursor");
    }

    public boolean getShowAbsMag() {
        return showAbsMag;
    }

    public void setShowAbsMag(boolean showMag) {
        showAbsMag = showMag;
    }

    public boolean getShowIntCntWithAbsMag() {
        return showIntCntWithAbsMag;
    }

    public void setShowIntCntWithAbsMag(boolean showIntCnt) {
        showIntCntWithAbsMag = showIntCnt;
    }

    public void setOrientation(boolean flipx, boolean flipy, int rot) {
        flipX = flipx;
        flipY = flipy;
        rotation = rot;
        setCanvasTransform();
    }

    public void setShowZoom(boolean showZ) {
        showZoom = showZ;
    }

    public void setShowDir(boolean showD) {
        showDir = showD;
    }

    public void setShowXY(boolean showOrientation) {
        showXY = showOrientation;
    }

    public void setShowAnnotations(boolean showAnnotationsOnCanvas) {
        showAnnotations = showAnnotationsOnCanvas;
    }

    public void setShowPixelScale(boolean showX, boolean showY, double XPixScale, double YPixScale) {
        XPixelScale = XPixScale;
        YPixelScale = YPixScale;
        showXScale = showX;
        showYScale = showY;

        if (wcs != null && wcs.hasScale) {
            XPixelScale = wcs.getXScaleArcSec();
            YPixelScale = wcs.getYScaleArcSec();
        }
    }

    public boolean getFlipX() {
        return flipX;
    }

    public void setFlipX(boolean fx) {
        flipX = fx;
        setCanvasTransform();
    }

    public boolean getFlipY() {
        return flipY;
    }

    public void setFlipY(boolean fy) {
        flipY = fy;
        setCanvasTransform();
    }

    public int getRotation() {
        return rotation;
    }

    public void setRotation(int rot) {
        rotation = rot;
        setCanvasTransform();
    }

    public void setTransEnabled(boolean enable) {
        transEnabled = enable;
    }

    public void setWCS(WCS wcsIn) {
        wcs = wcsIn;
    }

    void setCanvasTransform() {
        isTransformed = flipX || flipY || rotation != ROT_0;
        if ((rotation == ROT_0 && !flipX && !flipY) || (rotation == ROT_180 && flipX && flipY)) {
            netRotate = false;
            netFlipX = false;
            netFlipY = false;
        } else if ((rotation == ROT_0 && flipX && !flipY) || (rotation == ROT_180 && !flipX && flipY)) {
            netRotate = false;
            netFlipX = true;
            netFlipY = false;
        } else if ((rotation == ROT_0 && !flipX && flipY) || (rotation == ROT_180 && flipX && !flipY)) {
            netRotate = false;
            netFlipX = false;
            netFlipY = true;
        } else if ((rotation == ROT_0 && flipX && flipY) || (rotation == ROT_180 && !flipX && !flipY)) {
            netRotate = false;
            netFlipX = true;
            netFlipY = true;
        } else if ((rotation == ROT_90 && !flipX && !flipY) || (rotation == ROT_270 && flipX && flipY)) {
            netRotate = true;
            netFlipX = false;
            netFlipY = false;
        } else if ((rotation == ROT_90 && flipX && !flipY) || (rotation == ROT_270 && !flipX && flipY)) {
            netRotate = true;
            netFlipX = true;
            netFlipY = false;
        } else if ((rotation == ROT_90 && !flipX && flipY) || (rotation == ROT_270 && flipX && !flipY)) {
            netRotate = true;
            netFlipX = false;
            netFlipY = true;
        } else if ((rotation == ROT_90 && flipX && flipY) || (rotation == ROT_270 && !flipX && !flipY)) {
            netRotate = true;
            netFlipX = true;
            netFlipY = true;
        }

        if (oldNetRotate != netRotate) {
//            int tempWidth = srcRect.width;
//            srcRect.x = (int)(srcRect.x + srcRect.width/2.0 - srcRect.height/2.0);
//            srcRect.y = (int)(srcRect.y + srcRect.height/2.0 - srcRect.width/2.0);
//            srcRect.width = srcRect.height;
//            srcRect.height = tempWidth;
//            if (netRotate)
//                magnification = magnification*getWidth()/getHeight();
//            else
//                magnification = magnification*getHeight()/getWidth();
            oldNetRotate = netRotate;
        }
    }

    public boolean[] getCanvasTransform() {
        return new boolean[]{netFlipX, netFlipY, netRotate};
    }

    public boolean getNetFlipX() {
        return netFlipX;
    }

    public boolean getNetFlipY() {
        return netFlipY;
    }

    public boolean getNetRotate() {
        return netRotate;
    }

    public boolean isTransformed() {
        return isTransformed;
    }

    /**
     * Converts an offscreen x-coordinate to a screen x-coordinate.
     */
    @Override
    public int screenX(int ox) {
//        return        (int)((ox-srcRect.x)*magnification);
        return transEnabled && netFlipX ? (int) (getWidth() - (ox - srcRect.x) * magnification) : (int) ((ox + -srcRect.x) * magnification);
    }

    /**
     * Converts an offscreen y-coordinate to a screen y-coordinate.
     */
    @Override
    public int screenY(int oy) {
//        return    (int)((oy-srcRect.y)*magnification);
        return transEnabled && netFlipY ? (int) (getHeight() - (oy - srcRect.y) * magnification) : (int) ((oy - srcRect.y) * magnification);
    }

    /**
     * Converts a floating-point offscreen x-coordinate to a screen x-coordinate.
     */
    @Override
    public int screenXD(double ox) {
//        return        (int)((ox-srcRect.x)*magnification);
        return transEnabled && netFlipX ? (int) (getWidth() - (ox - srcRect.x) * magnification) : (int) ((ox - srcRect.x) * magnification);
    }

    /**
     * Converts a floating-point offscreen x-coordinate to a screen x-coordinate.
     */
    @Override
    public int screenYD(double oy) {
//        return        (int)((oy-srcRect.y)*magnification);
        return transEnabled && netFlipY ? (int) (getHeight() - (oy - srcRect.y) * magnification) : (int) ((oy - srcRect.y) * magnification);
    }

    public void setMousePosition(int sx, int sy) {
        screenX = sx;
        screenY = sy;
        imageX = offScreenXD(screenX);
        imageY = offScreenYD(screenY);
    }

    public void setAperture(double rad, double rB1, double rB2, boolean showSky, boolean showPhotCursor) {
        radius = rad;
        rBack1 = rB1;
        rBack2 = rB2;
        showSkyOverlay = showSky;
        showPhotometerCursor = showPhotCursor;
    }

    public void setMouseInImage(boolean inImage) {
        mouseInImage = inImage;
    }

    /**
     * Converts screen x/y-coordinates to an offscreen x-coordinate (required for canvas rotation support).
     */
    @Override
    public int offScreenX(int sx) {
        int sy = screenY;
        double aspectDelta = (imp.getWidth() - imp.getHeight()) / 2.0;
        if (!transEnabled || (!netFlipX && !netRotate)) return srcRect.x + (int) (sx / magnification);
        else if (netFlipX && !netRotate) return srcRect.x + (int) ((getWidth() - sx) / magnification);
        else if (!netFlipX && netRotate) return srcRect.x + (int) (aspectDelta + ((getHeight() - sy) / magnification));
        else return srcRect.x + (int) (aspectDelta + (sy / magnification));    //(netFlipX && netRotate)
    }

    /**
     * Converts screen x/y-coordinates to an offscreen y-coordinate (required for canvas rotation support).
     */
    @Override
    public int offScreenY(int sy) {
        int sx = screenX;
        double aspectDelta = (imp.getWidth() - imp.getHeight()) / 2.0;
        if (!transEnabled || (!netFlipY && !netRotate)) return srcRect.y + (int) (sy / magnification);
        else if (netFlipY && !netRotate) return srcRect.y + (int) ((getHeight() - sy) / magnification);
        else if (!netFlipY && netRotate) return srcRect.y - (int) (aspectDelta + (sx / magnification));
        else return srcRect.y - (int) (aspectDelta + ((getWidth() - sx) / magnification));    //(netFlipY && netRotate)
    }

    @Override
    public int offScreenY2(int sy) {
        return Math.round(offScreenY(sy));
    }

    @Override
    public int offScreenX2(int sx) {
        return Math.round(offScreenX(sx));
    }

    /**
     * Converts screen x/y-coordinates to a floating-point offscreen x-coordinate (required for canvas rotation support).
     */
    @Override
    public double offScreenXD(int sx) {
        int sy = screenY;
        double aspectDelta = (imp.getWidth() - imp.getHeight()) / 2.0;
        if (!transEnabled || (!netFlipX && !netRotate)) return srcRect.x + (sx / magnification);
        else if (netFlipX && !netRotate) return srcRect.x + (getWidth() - sx) / magnification;
        else if (!netFlipX && netRotate) return srcRect.x + aspectDelta + ((getHeight() - sy) / magnification);
        else return srcRect.x + aspectDelta + (sy / magnification);    //(netFlipX && netRotate)
    }

    /**
     * Converts screen x/y-coordinates to a floating-point offscreen y-coordinate (required for canvas rotation support).
     */
    @Override
    public double offScreenYD(int sy) {
        int sx = screenX;
        double aspectDelta = (imp.getWidth() - imp.getHeight()) / 2.0;
        if (!transEnabled || (!netFlipY && !netRotate)) return srcRect.y + (sy / magnification);
        else if (netFlipY && !netRotate) return srcRect.y + (getHeight() - sy) / magnification;
        else if (!netFlipY && netRotate) return srcRect.y - aspectDelta + (sx / magnification);
        else return srcRect.y - aspectDelta + ((getWidth() - sx) / magnification);    //(netFlipY && netRotate)
    }

    @Override
    /** Sets the cursor based on the current tool and cursor location. */
    public void setCursor(int sx, int sy, int ox, int oy) {
        xMouse = ox;
        yMouse = oy;
        mouseExited = false;
        Roi roi = imp.getRoi();
        ImageWindow win = imp.getWindow();
        if (win == null)
            return;
        if (IJ.spaceBarDown()) {
            setCursor(handCursor);
            return;
        }
        int id = Toolbar.getToolId();
        switch (Toolbar.getToolId()) {
            case Toolbar.MAGNIFIER:
                setCursor(moveCursor);
                break;
            case Toolbar.HAND:
                setCursor(handCursor);
                break;
            default:  //selection tool
                PlugInTool tool = Toolbar.getPlugInTool();
                boolean arrowTool = roi != null && (roi instanceof Arrow) && tool != null && "Arrow Tool".equals(tool.getToolName());
                if ((id == Toolbar.SPARE1 || id >= Toolbar.SPARE2) && !arrowTool) {
                    if (Prefs.usePointerCursor)
                        setCursor(defaultCursor);
                    else {
                        if (showRedCrossHairCursor || !showPhotometerCursor || !mouseInImage || !astronomyMode)
                            setCursor(redCrossHairCursor);
                        else
                            setCursor(clearCursor);
                    }
                } else if (roi != null && roi.getState() != Roi.CONSTRUCTING && roi.isHandle(sx, sy) >= 0)
                    setCursor(handCursor);
                else if (Prefs.usePointerCursor || (roi != null && roi.getState() != Roi.CONSTRUCTING && roi.contains(ox, oy)))
                    setCursor(defaultCursor);
                else
                    setCursor(crosshairCursor);
        }
    }

    public void setAstronomyMode(boolean enabled) {
        astronomyMode = enabled;
    }

    @Override
    public void paint(Graphics g) {
        if (!performDraw) {
            return;
        }

        if (g != null) {
            if (imageWidth == 0 && imageHeight == 0) {
                return;
            }

            paint(g, true); //!IJ.isMacOSX())
        }
    }

    @Override
    public boolean isDoubleBuffered() {
        return true;
    }

    @Override
    public void addNotify() {
        super.addNotify();
        // Ensure the buffer is created and the component is drawn on window creation
        createBufferStrategy(2);
        repaint();
    }

    // Use double buffer to reduce flicker when drawing complex ROIs.
    // Author: Erik Meijering
    public void paint(Graphics g, boolean doubleBuffered) {
        try {
            invCanvTrans = ((Graphics2D) g).getTransform();
            canvTrans = ((Graphics2D) g).getTransform();

            Roi roi = imp.getRoi();
            if (roi != null) {
                roi.updatePaste();
            }

            if (doubleBuffered) {
                BufferStrategy bufferStrategy = getBufferStrategy();

                if (bufferStrategy == null) {
                    createBufferStrategy(2);
                    bufferStrategy = getBufferStrategy();
                }

                do {
                    // The following loop ensures that the contents of the drawing buffer
                    // are consistent in case the underlying surface was recreated
                    do {
                        // Get a new graphics context every time through the loop
                        // to make sure the strategy is validated
                        Graphics graphics = bufferStrategy.getDrawGraphics();

                        // Render to graphics
                        paintCanvas(graphics);

                        graphics.dispose();

                        // Repeat the rendering if the drawing buffer contents were restored
                    } while (bufferStrategy.contentsRestored());

                    // Display the buffer
                    bufferStrategy.show();

                    // Repeat the rendering if the drawing buffer was lost
                } while (bufferStrategy.contentsLost());
            } else {
                paintCanvas(g);
            }
        } catch (OutOfMemoryError e) {
            IJ.outOfMemory("Paint");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void paintCanvas(Graphics drawingGraphics) {
        int clipWidth = srcRect.width + 1; //+1 to allow for partial pixel at edge
        int clipHeight = srcRect.height + 1; //+1 to allow for partial pixel at edge
        int offScrnWidth = (int) (clipWidth * magnification);
        int offScrnHeight = (int) (clipHeight * magnification);
        Roi roi = imp.getRoi();

        if (imageUpdated) {
            imageUpdated = false;
            imp.updateImage();
        }

        setInterpolation(drawingGraphics, Prefs.interpolateScaledImages);
        Image img = imp.getImage();
        flipAndRotateCanvas(drawingGraphics);
        if (!netRotate) {
            if (img != null) {
                drawingGraphics.drawImage(img,
                        srcRect.x < 0 ? (int) (-srcRect.x * magnification) : 0,
                        srcRect.y < 0 ? (int) (-srcRect.y * magnification) : 0,
                        srcRect.x + srcRect.width < imp.getWidth() ? offScrnWidth : (int) ((imp.getWidth() - srcRect.x) * magnification),
                        srcRect.y + srcRect.height < imp.getHeight() ? offScrnHeight : (int) ((imp.getHeight() - srcRect.y) * magnification),
                        srcRect.x < 0 ? 0 : srcRect.x,
                        srcRect.y < 0 ? 0 : srcRect.y,
                        srcRect.x + srcRect.width < imp.getWidth() ? srcRect.x + clipWidth : imp.getWidth(),
                        srcRect.y + srcRect.height < imp.getHeight() ? srcRect.y + clipHeight : imp.getHeight(),
                        null);
            }
        } else {
            if (img != null)  //needs major updating when netRotate is implemented
                drawingGraphics.drawImage(img, 0, 0, (int) (srcRect.height * magnification), (int) (srcRect.width * magnification),
                        (int) (srcRect.x + srcRect.width / 2.0 - srcRect.height / 2.0),
                        (int) (srcRect.y + srcRect.height / 2.0 - srcRect.width / 2.0),
                        (int) (srcRect.x + srcRect.width / 2.0 + srcRect.height / 2.0),
                        (int) (srcRect.y + srcRect.height / 2.0 + srcRect.width / 2.0), null);
        }

        transEnabled = false;
        int xx1 = screenX(0) > 0 ? screenX(0) : 0;    //top left screen x-location
        int yy1 = screenY(0) > 0 ? screenY(0) : 0;    //top left screen y-location
        int xx2 = screenX(imp.getWidth()) < offScrnWidth ? screenX(imp.getWidth()) : offScrnWidth;    //bottom right image x-pixel plus 1
        int yy2 = screenY(imp.getHeight()) < offScrnHeight ? screenY(imp.getHeight()) : offScrnHeight;    //bottom right image y-pixel plus 1

        drawingGraphics.setColor(Color.WHITE);
        if (xx1 > 0)
            drawingGraphics.fillRect(0, 0, xx1, offScrnHeight);
        if (yy1 > 0)
            drawingGraphics.fillRect(xx1, 0, offScrnWidth - xx1, yy1);
        if (xx2 < offScrnWidth)
            drawingGraphics.fillRect(xx2, yy1, offScrnWidth - xx2, offScrnHeight - yy1);
        if (yy2 < offScrnHeight)
            drawingGraphics.fillRect(xx1, yy2, xx2 - xx1, offScrnHeight - yy2);

        transEnabled = true;

        if (showPhotometerCursor && mouseInImage && astronomyMode && !customPixelMode) updatePhotometerOverlay(drawingGraphics);
        OverlayCanvas oc = getOverlayCanvas(imp);
        if (oc.numberOfRois() > 0) drawOverlayCanvas(drawingGraphics);

        if (AstrometrySetup.DISPLAY_EXCLUDED_BORDERS.get()) {
            drawExcludedRegions((Graphics2D) drawingGraphics);
        }
        
        transEnabled = false;

        //if (overlay!=null) ((ImageCanvas)this).drawOverlay(overlay, offScreenGraphics);

        if (showAllOverlay != null) this.drawOverlay(showAllOverlay, drawingGraphics);
        if (roi != null) drawRoi(roi, drawingGraphics);
        transEnabled = true;
        drawZoomIndicator(drawingGraphics);
        if (IJ.debugMode) showFrameRate(drawingGraphics);
    }
    
    private void drawExcludedRegions(Graphics2D g2) {
        var left = AstrometrySetup.BORDER_EXCLUSION_LEFT.get();
        var right = AstrometrySetup.BORDER_EXCLUSION_RIGHT.get();
        var top = AstrometrySetup.BORDER_EXCLUSION_TOP.get();
        var bottom = AstrometrySetup.BORDER_EXCLUSION_BOTTOM.get();

        g2.setTransform(invCanvTrans);

        g2.setColor(Color.RED);
        var comp = g2.getComposite();
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.25f));

        // Draw left region
        if (left > 0) {
            g2.fillRect(screenX(0), screenY(0), screenX(left) - screenX(0), screenY(imp.getHeight()) - screenY(0));
        }

        // Draw right region
        if (right > 0) {
            g2.fillRect(screenX(imp.getWidth() - right), screenY(0), screenX(imp.getWidth()) - screenX(imp.getWidth() - right), screenY(imp.getHeight()) - screenY(0));
        }

        // Draw top region
        if (top > 0) {
            g2.fillRect(screenX(left), screenY(0), screenX(imp.getWidth() - right) - screenX(left), screenY(top) - screenY(0));
        }

        // Draw bottom region
        if (bottom > 0) {
            g2.fillRect(screenX(left), screenY(imp.getHeight() - bottom), screenX(imp.getWidth() - right) - screenX(left), screenY(imp.getHeight()) - screenY(imp.getHeight() - bottom));
        }

        g2.setComposite(comp);
        g2.setTransform(canvTrans);
    }

    @Override
    protected void showFrameRate(Graphics g) {
        ((Graphics2D) g).setTransform(invCanvTrans);
        super.showFrameRate(g);
        ((Graphics2D) g).setTransform(canvTrans);
    }

    public Image graphicsToImage(Graphics g) {   //used by AstroStackWindow to save image display
        int swidth = srcRect.width + 1;
        int sheight = srcRect.height + 1;
        final int srcRectWidthMag = (int) (swidth * magnification);
        final int srcRectHeightMag = (int) (sheight * magnification);
        Image image = createImage(srcRectWidthMag, srcRectHeightMag);

        Roi roi = imp.getRoi();

        if (imageUpdated) {
            imageUpdated = false;
            imp.updateImage();
        }
        Graphics imageGraphics = image.getGraphics();
        setInterpolation(g, Prefs.interpolateScaledImages);
        Image img = imp.getImage();
        if (isTransformed)
            flipAndRotateCanvas(imageGraphics);
        if (!netRotate) {
            if (img != null)
                imageGraphics.drawImage(img, 0, 0, (int) (swidth * magnification), (int) (sheight * magnification),
                        srcRect.x, srcRect.y, srcRect.x + swidth, srcRect.y + sheight, null);
        } else {
            if (img != null)
                imageGraphics.drawImage(img, 0, 0, (int) (sheight * magnification), (int) (swidth * magnification),
                        (int) (srcRect.x + swidth / 2.0 - sheight / 2.0),
                        (int) (srcRect.y + sheight / 2.0 - swidth / 2.0),
                        (int) (srcRect.x + swidth / 2.0 + sheight / 2.0),
                        (int) (srcRect.y + sheight / 2.0 + swidth / 2.0), null);
        }
        OverlayCanvas oc = getOverlayCanvas(imp);
        if (oc.numberOfRois() > 0) drawOverlayCanvas(imageGraphics);
        transEnabled = false;
        //if (overlay!=null) ((ImageCanvas)this).drawOverlay(overlay, imageGraphics);
        if (showAllOverlay != null) this.drawOverlay(showAllOverlay, imageGraphics);
        if (roi != null) drawRoi(roi, imageGraphics);
        transEnabled = true;
        drawZoomIndicator(imageGraphics);
        g.drawImage(image, 0, 0, null);
        return image;


    }

    @Override
    public void mouseMoved(MouseEvent e) {
        super.mouseMoved(e);
        int sx = e.getX();
        int sy = e.getY();
        int ox = (int) offScreenXD(sx);
        int oy = (int) offScreenYD(sy);
        setCursor(sx, sy, ox, oy);
    }

    void updatePhotometerOverlay(Graphics g) {
        ((Graphics2D) g).setTransform(invCanvTrans);
        transEnabled = false;
        g.setColor(mouseApertureColor);
                /*var m = getMousePosition();
                screenX = m.x;
                screenY = m.y;*/
        imageX = offScreenXD(screenX);
        imageY = offScreenYD(screenY);
//                IJ.log(" imageX = "+imageX+"    imageY = "+imageY);

        int sx = screenXD(imageX);
        int sy = screenYD(imageY);
//                IJ.log("screenX = "+sx+"   screenY = "+sy);
//                IJ.log("");
        int x1 = screenXD(imageX - radius);
        int w1 = screenXD(imageX + radius) - x1;
        int y1 = screenYD(imageY - radius);
        int h1 = screenYD(imageY + radius) - y1;
        int x2 = screenXD(imageX - rBack1);
        int x3 = screenXD(imageX - rBack2);
        int w2 = screenXD(imageX + rBack1) - x2;
        int w3 = screenXD(imageX + rBack2) - x3;
        int y2 = screenYD(imageY - rBack1);
        int y3 = screenYD(imageY - rBack2);
        int h2 = screenYD(imageY + rBack1) - y2;
        int h3 = screenYD(imageY + rBack2) - y3;

        g.drawOval(x1, y1, w1, h1);


        if (showSkyOverlay) {

            g.drawOval(x2, y2, w2, h2);
            g.drawOval(x3, y3, w3, h3);
        }
                /*g.setColor(Color.CYAN);
                g.drawRect((int) screenX, (int) screenY, 1, 1);*/

//                oldX3=x3;
//                oldY3=y3;
//                oldW3=w3;
//                oldH3=h3;
        transEnabled = true;
        ((Graphics2D) g).setTransform(canvTrans);
    }

    private void setInterpolation(Graphics g, boolean interpolate) {
        if (magnification == 1) {
        }
        else if (magnification < 1.0 || interpolate) {
            Object value = RenderingHints.VALUE_RENDER_QUALITY;
            ((Graphics2D) g).setRenderingHint(RenderingHints.KEY_RENDERING, value);
        } else if (magnification > 1.0) {
            Object value = RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR;
            ((Graphics2D) g).setRenderingHint(RenderingHints.KEY_INTERPOLATION, value);
        }
    }

    public void flipAndRotateCanvas(Graphics g) {
        invCanvTrans = ((Graphics2D) g).getTransform();
        Image img = imp.getImage();
        double aspectDelta = (getWidth() - getHeight()) / 2.0;
        double transX = (getWidth() / 2.0);
        double transY = (getHeight() / 2.0);
        if (!netRotate)
//            ((Graphics2D)g).translate(srcRect.width*magnification/2.0, srcRect.height*magnification/2.0);
            ((Graphics2D) g).translate(transX, transY);
        else
//            ((Graphics2D)g).translate((srcRect.height+aspectDelta)*magnification/2.0, srcRect.width*magnification/2.0);
            ((Graphics2D) g).translate(transY + aspectDelta, transX);

        if (netRotate) ((Graphics2D) g).rotate(Math.toRadians(ROT_90));
        if (netFlipX) ((Graphics2D) g).scale(-1.0, 1.0);
        if (netFlipY) ((Graphics2D) g).scale(1.0, -1.0);


        if (!netRotate)
//            ((Graphics2D)g).translate(-srcRect.width*magnification/2.0, -srcRect.height*magnification/2.0);
            ((Graphics2D) g).translate(-transX, -transY);
        else {
//            ((Graphics2D)g).translate((-srcRect.height+(netFlipX ? -aspectDelta : aspectDelta))*magnification/2.0, -srcRect.width*magnification/2.0);
            ((Graphics2D) g).translate(-transY + (netFlipX ? -aspectDelta : aspectDelta), -transX);
        }
        canvTrans = ((Graphics2D) g).getTransform();
        transEnabled = true;
    }

    @Override
    public void drawZoomIndicator(Graphics g) {
        ((Graphics2D) g).setTransform(invCanvTrans);
        g.setFont(p12);
        g.setColor(zoomIndicatorColor);
        ((Graphics2D) g).setStroke(Roi.onePixelWide);
        w2 = (int) (w1 * ((double) srcRect.width / imageWidth));
        h2 = (int) (h1 * ((double) srcRect.height / imageHeight));
        if (w2 < 1) w2 = 1;
        if (h2 < 1) h2 = 1;
        x2 = (int) (w1 * ((double) srcRect.x / imageWidth));
        y2 = (int) (h1 * ((double) srcRect.y / imageHeight));

        if (showZoom) {
            g.drawRect(x1, y1, w1, h1);
            if (srcRect.width < imageWidth || srcRect.height < imageHeight) {
                int x3 = netFlipX ? x1 + w1 - x2 - w2 : x1 + x2;
                int y3 = netFlipY ? y1 + h1 - y2 - h2 : y1 + y2;
                if (w2 * h2 <= 200 || w2 < 10 || h2 < 10)
                    g.fillRect(x3, y3, w2, h2);
                else
                    g.drawRect(x3, y3, w2, h2);
            }
        }

        if (showDir) {
            g.setColor(wcs.hasPA ? dirIndicatorColorWCS : dirIndicatorColor);
            g.drawLine(xc, yc, xc - sinEli, yc - cosEli);
            g.fillPolygon(xarrowheadE, yarrowheadE, 3);
            g.drawString("E", xc - sinEli + eXShift, yc - cosEli + eYShift);
            g.drawLine(xc, yc, xc - sinNli, yc - cosNli);
            g.fillPolygon(xarrowheadN, yarrowheadN, 3);
            g.drawString("N", xc - sinNli + nXShift, yc - cosNli + nYShift);
        }

        if (showXY) {
            int len2 = len;
            if (showDir) len2 -= 10;
            if (len2 < 15) len2 = 15;
            g.setColor(axisIndicatorColor);
            if (!netFlipX) {
                g.drawLine(xc, yc, xc + len2, yc);
                g.drawString("X", xc + len2 - 8, yc - 5);
                xarrowheadX[0] = xc + len2;
                xarrowheadX[1] = xc + len2 - 7;
                xarrowheadX[2] = xc + len2 - 7;
                yarrowheadX[0] = yc;
                yarrowheadX[1] = yc + 4;
                yarrowheadX[2] = yc - 4;
            } else {
                g.drawLine(xc, yc, xc - len2, yc);
                g.drawString("X", xc - len2 + 2, yc - 5);
                xarrowheadX[0] = xc - len2;
                xarrowheadX[1] = xc - len2 + 7;
                xarrowheadX[2] = xc - len2 + 7;
                yarrowheadX[0] = yc;
                yarrowheadX[1] = yc + 4;
                yarrowheadX[2] = yc - 4;
            }
            if (!netFlipY) {
                g.drawLine(xc, yc, xc, yc - len2);
                g.drawString("Y", xc - 12, yc - len2 + 11);
                xarrowheadY[0] = xc;
                xarrowheadY[1] = xc - 4;
                xarrowheadY[2] = xc + 4;
                yarrowheadY[0] = yc - len2;
                yarrowheadY[1] = yc - len2 + 7;
                yarrowheadY[2] = yc - len2 + 7;
            } else {
                g.drawLine(xc, yc, xc, yc + len2);
                g.drawString("Y", xc - 12, yc + len2 + 1);
                xarrowheadY[0] = xc;
                xarrowheadY[1] = xc - 4;
                xarrowheadY[2] = xc + 4;
                yarrowheadY[0] = yc + len2;
                yarrowheadY[1] = yc + len2 - 7;
                yarrowheadY[2] = yc + len2 - 7;
            }
            g.fillPolygon(xarrowheadX, yarrowheadX, 3);
            g.fillPolygon(xarrowheadY, yarrowheadY, 3);

        }

        if (showXScale) {
            g.setColor(wcs.hasScale ? dirIndicatorColorWCS : dirIndicatorColor);
            g.setFont(p16);
            int length = 160;
            int startX = (getWidth() - length) / 2;
            int endX = startX + length;
            double arcLen = length * XPixelScale / getMagnification();
            String units = "\"";
            if (XPixelScale != 0.0) {
                if (arcLen >= 60) {
                    arcLen /= 60.0;
                    units = "'";
                    if (arcLen >= 60) {
                        arcLen /= 60.0;
                        units = " deg";
                    }
                }
            } else {
                arcLen = length / getMagnification();
                units = " pixels";
            }
            String label = (XPixelScale == 0.0 ? noPlaces.format(arcLen) : twoplaces.format(arcLen)) + units;
            FontMetrics fm = g.getFontMetrics(p16);
            java.awt.geom.Rectangle2D rect = fm.getStringBounds(label, g);
            int labelWidth = (int) (rect.getWidth());
            int Y = (int) (rect.getHeight()) + 4;
            g.drawString(label, (startX + endX - labelWidth + 10) / 2, Y - 4);
            g.drawLine(startX, Y, endX, Y);
            g.drawLine(startX, Y - 5, startX, Y + 5);
            g.drawLine(endX, Y - 5, endX, Y + 5);
        }

        if (showYScale) {
            ((Graphics2D) g).translate(getWidth() / 2.0, getHeight() / 2.0);
            ((Graphics2D) g).rotate(Math.toRadians(ROT_90));
            ((Graphics2D) g).translate(-getWidth() / 2.0, -getHeight() / 2.0);
            g.setColor(wcs.hasScale ? dirIndicatorColorWCS : dirIndicatorColor);
            g.setFont(p16);
            int length = 160;
            int startX = (getWidth() - length) / 2;
            int endX = startX + length;
            double arcLen = length * YPixelScale / getMagnification();
            String units = "\"";
            if (YPixelScale != 0.0) {
                if (arcLen >= 60) {
                    arcLen /= 60.0;
                    units = "'";
                    if (arcLen >= 60) {
                        arcLen /= 60.0;
                        units = " deg";
                    }
                }
            } else {
                arcLen = length / getMagnification();
                units = " pixels";
            }

            String label = (YPixelScale == 0.0 ? noPlaces.format(arcLen) : twoplaces.format(arcLen)) + units;
//            if (IJ.isMacOSX())  //work around an apparent OS X java bug that reverses characters in a string when canvas is rotated (at least in JRE 1.6.0_37 (32-bit)
//                {
//                label = new StringBuffer(label).reverse().toString();
//                }

            FontMetrics fm = g.getFontMetrics(p16);
            java.awt.geom.Rectangle2D rect = fm.getStringBounds(label, g);
            int labelWidth = (int) (rect.getWidth());
            int Y = (int) (rect.getHeight()) + 4 + (getHeight() - getWidth()) / 2;
            g.drawLine(startX, Y, endX, Y);
            g.drawLine(startX, Y - 5, startX, Y + 5);
            g.drawLine(endX, Y - 5, endX, Y + 5);
            if (IJ.isMacOSX())  //new workaround for Mac OS X vertical character reversals in some OS X versions.
            //The web post source describes the fix as an "odd solution for this problem (that matches the oddness of the bug)"
            //see http://stackoverflow.com/questions/14569475/java-rotated-text-has-reversed-characters-sequence
            {
                FontRenderContext frc = new FontRenderContext(((Graphics2D) g).getTransform(), true, true);
                ((Graphics2D) g).drawGlyphVector(p16.createGlyphVector(frc, label), (startX + endX - labelWidth + 10) / 2f, Y - 4);
            } else {
                g.drawString(label, (startX + endX - labelWidth + 10) / 2, Y - 4);//IJ.isMacOSX()?(startX+endX+labelWidth-10)/2:(startX+endX-labelWidth+10)/2, Y - 4);
            }
            ((Graphics2D) g).setTransform(canvTrans);
        }

    }

    void updateZoomBoxParameters() {
        double northDirAngle = NdirAngle;
        if (wcs != null && wcs.hasPA) northDirAngle = wcs.getNorthPA();
        if (netFlipX)
            northDirAngle = 360 - northDirAngle;
        if (netFlipY)
            northDirAngle = 180 - northDirAngle;
        while (northDirAngle < 0.0) {
            northDirAngle += 360;
        }
        northDirAngle %= 360;
        double eastDirAngle = NdirAngle + EdirAngle;
        if (wcs != null && wcs.hasPA) eastDirAngle = wcs.getEastPA();
        if (netFlipX)
            eastDirAngle = 360 - eastDirAngle;
        if (netFlipY)
            eastDirAngle = 180 - eastDirAngle;
        while (eastDirAngle < 0.0) {
            eastDirAngle += 360;
        }
        eastDirAngle %= 360;
        if (eastDirAngle > 340 || eastDirAngle <= 25) {
            eXShift = -12;
            eYShift = 10;
        } else if (eastDirAngle > 25 && eastDirAngle <= 60) {
            eXShift = -8;
            eYShift = 11;
        } else if (eastDirAngle > 60 && eastDirAngle <= 105) {
            eXShift = 1;
            eYShift = -5;
        } else if (eastDirAngle > 105 && eastDirAngle <= 130) {
            eXShift = 0;
            eYShift = -7;
        } else if (eastDirAngle > 130 && eastDirAngle <= 210) {
            eXShift = -12;
            eYShift = 1;
        } else if (eastDirAngle > 210 && eastDirAngle <= 240) {
            eXShift = -14;
            eYShift = 6;
        } else if (eastDirAngle > 240 && eastDirAngle <= 310) {
            eXShift = -8;
            eYShift = -5;
        } else {
            eXShift = -15;
            eYShift = 6;
        }
        if (northDirAngle > 340 || northDirAngle <= 25) {
            nXShift = -12;
            nYShift = 10;
        } else if (northDirAngle > 25 && northDirAngle <= 60) {
            nXShift = -8;
            nYShift = 11;
        } else if (northDirAngle > 60 && northDirAngle <= 105) {
            nXShift = 1;
            nYShift = -5;
        } else if (northDirAngle > 105 && northDirAngle <= 130) {
            nXShift = 0;
            nYShift = -7;
        } else if (northDirAngle > 130 && northDirAngle <= 210) {
            nXShift = -12;
            nYShift = 1;
        } else if (northDirAngle > 210 && northDirAngle <= 240) {
            nXShift = -14;
            nYShift = 6;
        } else if (northDirAngle > 240 && northDirAngle <= 310) {
            nXShift = -8;
            nYShift = -5;
        } else {
            nXShift = -15;
            nYShift = 6;
        }
        angEr = eastDirAngle * dtr;
        sinEa = Math.sin(angEr);
        cosEa = Math.cos(angEr);
        angNr = northDirAngle * dtr;
        sinNa = Math.sin(angNr);
        cosNa = Math.cos(angNr);
        imageHeight = imp.getHeight();
        imageWidth = imp.getWidth();
        aspectRatio = (double) imageHeight / (double) imageWidth;
        h1 = zoomIndicatorSize;
        w1 = (int) (h1 / aspectRatio);
        if (w1 < 50) w1 = 50;
        if (zoomIndicatorColor == null)
            zoomIndicatorColor = new Color(128, 128, 255);
        x1 = 10;
        y1 = 10;
        xc = (2 * x1 + w1) / 2;
        yc = (2 * y1 + h1) / 2;
        len = w1 > h1 ? (h1 / 2) - 5 : (w1 / 2) - 5;
        sinNl = Math.sin(northDirAngle * dtr) * (double) len;
        cosNl = Math.cos(northDirAngle * dtr) * (double) len;
        sinNli = (int) Math.round(sinNl);
        cosNli = (int) Math.round(cosNl);
        sinEl = Math.sin(eastDirAngle * dtr) * (double) len;
        cosEl = Math.cos(eastDirAngle * dtr) * (double) len;
        sinEli = (int) Math.round(sinEl);
        cosEli = (int) Math.round(cosEl);
        xarrowheadE[0] = xc - sinEli;
        yarrowheadE[0] = yc - cosEli;
        xarrowheadE[1] = xc + (int) Math.round(-sinEa * len + sinEa * 7. - cosEa * 4.);
        yarrowheadE[1] = yc + (int) Math.round(-cosEa * len + cosEa * 7. + sinEa * 4.);
        xarrowheadE[2] = xc + (int) Math.round(-sinEa * len + sinEa * 7. + cosEa * 4.);
        yarrowheadE[2] = yc + (int) Math.round(-cosEa * len + cosEa * 7. - sinEa * 4.);
        xarrowheadN[0] = xc - sinNli;
        yarrowheadN[0] = yc - cosNli;
        xarrowheadN[1] = xc + (int) Math.round(-sinNa * len + sinNa * 7. - cosNa * 4.);
        yarrowheadN[1] = yc + (int) Math.round(-cosNa * len + cosNa * 7. + sinNa * 4.);
        xarrowheadN[2] = xc + (int) Math.round(-sinNa * len + sinNa * 7. + cosNa * 4.);
        yarrowheadN[2] = yc + (int) Math.round(-cosNa * len + cosNa * 7. - sinNa * 4.);
    }

    //   @Override
    public void repaint(int x, int y, int width, int height) {
        x = netFlipX ? getWidth() - x - width : x;
        y = netFlipY ? getHeight() - y - height : y;

        super.repaint(x, y, width, height);
//        updatePhotometerOverlay(imp.getCanvas().getGraphics());

    }


    public boolean doesPerformDraw() {
        return performDraw;
    }

    public void setPerformDraw(boolean performDraw) {
        this.performDraw = performDraw;
    }

    public void setCustomPixelMode(boolean customPixelMode) {
        this.customPixelMode = customPixelMode;
    }
} // AstroCanvas class