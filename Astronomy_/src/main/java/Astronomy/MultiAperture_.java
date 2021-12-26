package Astronomy;// MultiAperture_.java

import astroj.*;
import ij.IJ;
import ij.ImagePlus;
import ij.Prefs;
import ij.WindowManager;
import ij.astro.logging.AIJLogger;
import ij.gui.GenericDialog;
import ij.gui.PlotWindow;
import ij.gui.Toolbar;
import ij.measure.ResultsTable;
import ij.process.ImageProcessor;
import ij.util.Tools;
import util.GenericSwingDialog;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.lang.reflect.InvocationTargetException;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.List;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.function.Consumer;
import java.util.stream.Collectors;


/**
 * Based on Aperture_.java, but with pre-selection of multiple apertures and processing of stacks.
 *
 * @author F.V. Hessman, Georg-August-Universitaet Goettingen
 * @author Karen Collins (Univ. Louisville/KY)
 * @author F. Hessman (Goettingen)
 * @version 1.7
 * @date 2006-Feb-15
 * @date 2006-Nov-29
 * @changes Slight re-organization so that it's easier to use MultiAperture_ as a parent class, e.g. for Stack_Aligner_.
 * @date 2006-Dec-6
 * @changes Corrected problem with non-instantiated xOld for the case of images and not stacks;
 * added thread to display current ROIs.
 * @date 2006-Dec-11
 * @changes Complicated thread solution replaced with simple overlay solution inherited from Aperture_
 * @date 2007-Mar-14
 * @changes Added <ESC> to stop processing of large stacks which have gone awry.
 * @date 2007-May-01
 * @changes Added output of ratio
 * @date 2010-Mar-18
 * @changes 1) On my machine, the number of apertures and the 4 check box option values do not get stored between runs.
 * I don't know if this is a problem with my computing environment, or if it is a problem in general.
 * To make it work on my end, I had to implement the MultiAperture_ preferences retrieval from the Aperture_
 * preferences management code. I was able to implement the save (set) from MultiAperture_ without problems.
 * I could never work out how to do both from MultiAperture_, so my solution is probably not ideal, but it seems to work now.
 * 2) Implemented the Ratio Error functionality. The code I am currently using is:
 * ratio*Math.sqrt(srcVar/(src*src)+
 * othersVariance/(others*others))        {equation 2}
 * where
 * ratio = src counts/total of all comparison counts  (all adjusted for sky background)
 * srcVar = (source error)^2 from the 1st of N apertures per image (as calculated in Photometer above)
 * src = total source counts less sky background from the 1st of N apertures
 * totVar = (ap#2 error)^2 + (ap#3 error)^2 + ...  + (ap#N error)^2
 * tot = total source counts less sky background from the 2nd through Nth apertures
 * 3) Implemented the Ratio SNR functionality. It simply reports (src counts)/{equation 2}, which is accomplished by
 * not multiplying by "ratio".
 * 4) Measurements table code to support the ratio error and ratioSNR reporting.
 * @date 2011-Mar-29
 * @changes added dialog() to make it easier to sub-class (e.g. Stack_Aligner)
 */
@SuppressWarnings("SpellCheckingInspection")
public class MultiAperture_ extends Aperture_ implements MouseListener, MouseMotionListener, KeyListener {
    public static boolean cancelled = false;
    //	double ratio = 0.0;		// FIRST APERTURE
//	double ratioError = 0.0;
//	double ratioSNR = 0.0;
    public static String RATIO = "rel_flux_T1";
    public static String TOTAL = "tot_C_cnts";
    public static String TOTAL_ERROR = "tot_C_err";
    static protected boolean updatePlot = true;
    protected static String PREFS_AUTOMODE = "multiaperture.automode";  //0 click - for use with macros
    protected static String PREFS_FINISHED = "multiaperture.finished";  //signals finished to macros
    protected static String PREFS_CANCELED = "multiaperture.canceled";
    protected static String PREFS_PREVIOUS = "multiaperture.previous";
    protected static String PREFS_SINGLESTEP = "multiaperture.singlestep";
    protected static String PREFS_ALLOWSINGLESTEPAPCHANGES = "multiaperture.allowsinglestepapchanges";
    protected static String PREFS_USEVARSIZEAP = "multiaperture.usevarsizeap";
    protected static String PREFS_USEMA = "multiaperture.usema";
    protected static String PREFS_USEALIGN = "multiaperture.usealign";
    protected static String PREFS_USEWCS = "multiaperture.usewcs";
    protected static String PREFS_SUGGESTCOMPSTARS = "multiaperture.suggestCompStars";
    protected static String PREFS_HALTONERROR = "multiaperture.haltOnError";
    protected static String PREFS_SHOWHELP = "multiaperture.showhelp";
    protected static String PREFS_ALWAYSFIRSTSLICE = "multiaperture.alwaysstartatfirstSlice";
    protected static String PREFS_APFWHMFACTOR = "multiaperture.apfwhmfactor";
    protected static String PREFS_AUTOMODEFLUXCUTOFF = "multiaperture.automodefluxcutoff";
    //    protected static String PREFS_FOLLOW          = new String ("multiaperture.follow");
//	protected static String PREFS_WIDETABLE       = new String ("multiaperture.widetable");
    protected static String PREFS_SHOWRATIO = "multiaperture.showratio";
    protected static String PREFS_SHOWCOMPTOT = "multiaperture.showcomptot";
    protected static String PREFS_SHOWRATIO_ERROR = "multiaperture.showratioerror";
    protected static String PREFS_SHOWRATIO_SNR = "multiaperture.showratiosnr";
    protected static String PREFS_NAPERTURESMAX = "multiaperture.naperturesmax";
    protected static String PREFS_XAPERTURES = "multiaperture.xapertures";
    protected static String PREFS_YAPERTURES = "multiaperture.yapertures";
    protected static String PREFS_RAAPERTURES = "multiaperture.raapertures";
    protected static String PREFS_DECAPERTURES = "multiaperture.decapertures";
    protected static final String PREFS_MAXPEAKVALUE = "multiaperture.maxpeakvalue";
    protected static final String PREFS_MINPEAKVALUE = "multiaperture.minpeakvalue";
    protected static final String PREFS_UPPERBRIGHTNESS = "multiaperture.upperbrightness";
    protected static final String PREFS_LOWERBRIGHTNESS = "multiaperture.lowerbrightness";
    protected static final String PREFS_BRIGHTNESSDISTANCE = "multiaperture.brightnessdistance";
    protected static final String PREFS_MAXSUGGESTEDSTARS = "multiaperture.maxsuggestedstars";
    protected static final String PREFS_DEBUGAPERTURESUGGESTION = "multiaperture.debugaperturesuggestion";
    protected static final String PREFS_GAUSSRADIUS = "multiaperture.gaussradius";
    protected static final String PREFS_AUTOPEAKS = "multiaperture.autopeaks";
    protected static final String PREFS_AUTORADIUS = "multiaperture.autoradius";
    protected static final String PREFS_REFERENCESTAR = "multiaperture.referencestar";
    protected static final String PREFS_ENABLELOG = "multiaperture.enablelog";

//	double vx = 0.0;
//	double vy = 0.0;
//	double vxOld = 0.0;
//	double vyOld = 0.0;
//	public static String VX = new String ("vx");
//	public static String VY = new String ("vy");
    protected static String PREFS_ABSMAGAPERTURES = "multiaperture.absmagapertures";
    protected static String PREFS_ISREFSTAR = "multiaperture.isrefstar";
    protected static String PREFS_ISALIGNSTAR = "multiaperture.isalignstar";
    protected static String PREFS_CENTROIDSTAR = "multiaperture.centroidstar";
    protected static String PREFS_USEMACROIMAGE = "multiaperture.useMacroImage";
    protected static String PREFS_MACROIMAGENAME = "multiaperture.macroImageName";
    protected static String PREFS_ENABLEDOUBLECLICKS = "multiaperture.enableDoubleClicks";
    protected static String PREFS_ALWAYSFIRST = "multiaperture.alwaysstartatfirstslice";
    protected static String PREFS_UPDATEPLOT = "multiaperture.updatePlot";
    protected static String PREFS_GETMAGS = "multiaperture.getMags";
    protected static String PREFS_XLOCATION = "multiaperture.xLocation";
    protected static String PREFS_YLOCATION = "multiaperture.yLocation";
    protected static String PREFS_PREVNUMMONITORS = "multiaperture.prevNumberOfMonitors";
    protected static Boolean ENABLECENTROID = true;
    protected static Boolean DISABLECENTROID = false;
    protected static Boolean CLEARROIS = true;
    protected static Boolean KEEPROIS = false;
    protected int ngot = 0;
    //	protected int aperture=0;
    protected int nAperturesMax = 1000;
    protected int nApertures = 2;
    protected int nAperturesStored = 0;
    protected int startDragScreenX;
    protected int startDragScreenY;
    protected int currentScreenX;
    protected int currentScreenY;
    protected double startDragX;
    protected double startDragY;
    protected double currentX;
    protected double currentY;
    protected double[] xOld;
    protected double[] yOld;
    protected double[] xPosStored;
    protected double[] yPosStored;
    protected double[] raPosStored;
    protected double[] decPosStored;
    protected boolean[] isRefStarStored;
    protected boolean[] isAlignStarStored;
    protected boolean[] centroidStarStored;
    protected double[] absMagStored;
    protected String xOldApertures, yOldApertures, raOldApertures, decOldApertures, isOldRefStar, isOldAlignStar, oldCentroidStar, absMagOldApertures;
    protected double[] xPos;
    protected double[] yPos;
    protected double[] raPos;
    protected double[] decPos;
    protected double[] ratio;
    protected double[] ratioError;
    protected double[] ratioSNR;
    protected boolean[] isRefStar;    // indication of classification as reference star or target star (target stars not included in total ref star count)
    //	protected List<String> isRefStar2 = new ArrayList<String>();
    protected boolean[] isAlignStar;
    protected boolean[] centroidStar;
    protected double[] absMag, targetAbsMag;
    protected boolean hasAbsMag = false, autoPeakValues = true;
    protected double totAbsMag = 0.0;
    protected int numAbsRefs = 0;
    protected double[] src;           // net integrated counts for each aperture
    protected double[] srcVar;        // error*error for each aperture
    protected double[] tot;            // total ref star counts for each source
    protected double[] totVar;        // variance in tot ref star counts
    protected double[] xWidthFixed;
    protected double[] yWidthFixed;
    protected double[] widthFixed;
    protected double[] angleFixed;
    protected double[] roundFixed;
    protected double peak = 0.0;        // max pixel value in aperture
    protected boolean autoMode = false, enableLog = true;
    protected boolean singleStep = false;
    protected boolean allowSingleStepApChanges = false;
    protected boolean simulatedLeftClick = false;
    protected boolean allStoredAperturesPlaced = false;
    protected boolean enableDoubleClicks = true;
    protected boolean multiApertureRunning = false;
    protected boolean useVarSizeAp = false;
    protected boolean showHelp = true;
    protected boolean alwaysstartatfirstSlice = false;
    protected boolean haltOnError = true;
    protected boolean foundFWHM = false;
    protected boolean showRatio = true;
    protected boolean showCompTot = true;
    protected boolean debugAp = false;

//	protected boolean follow=false;
//	protected boolean wideTable=true;
    protected boolean showRatioError = true;
    protected boolean showRatioSNR = true;
    protected boolean getMags = false;
    protected boolean useMacroImage = false;
    protected String macroImageName = null;
    protected boolean frameAdvance = false;
    protected int screenX;
    protected int screenY;
    protected int modis;
    double autoModeFluxCutOff = 0.010;
    boolean openSimbadForAbsMag = true;
    boolean verbose = true;
    boolean blocked = false;
    boolean previous = false;
    boolean doStack = false;
    boolean mouseDrag = false;
    boolean processingStack = false;
    boolean apertureClicked = false;
    boolean apertureChanged = false;
    boolean firstClick = true;
    boolean enterPressed = false;
    boolean hasWCS = false;
    boolean runningWCSOnlyAlignment = false;
    astroj.AstroStackWindow asw = null;
    astroj.AstroCanvas ac = null;
    WCS wcs = null;
    ApertureRoi selectedApertureRoi = null;
    int xLocation = 10, yLocation = 10;
    int xAbsMagLocation = 10, yAbsMagLocation = 10;
    int firstSlice = 1;
    int initialFirstSlice = 1;
//    DecimalFormatSymbols dfs = uptoEightPlaces.getDecimalFormatSymbols();
    int initialLastSlice = 1;
    int lastSlice = 1;
    int astronomyToolId = 0;
    int currentToolId = 0;
    Toolbar toolbar;
    String infoMessage = "";
    double apFWHMFactor = 1.4;
    double xFWHM = 0.0;
    double yFWHM = 0.0;
    double rRD = 0.0;
    double fwhmRD = 0.0;
    boolean autoAperture = false;
    double fwhmMean = 0;
    double oldradius;
    double oldrBack1;
    double oldrBack2;
    double oldapFWHMFactor;
    double oldAutoModeFluxCutOff;
    boolean oldUseVarSizeAp;
    boolean oldBackIsPlane;
    boolean oldRemoveBackStars;
    boolean oldGetMags;
    ImagePlus openImage;
    MouseEvent dummyClick = null;
    MouseEvent ee;
    JFrame helpFrame;
    JScrollPane helpScrollPane;
    JPanel helpPanel;
    JLabel leftClickLabel, shiftLeftClickLabel, shiftControlLeftClickLabel, altLeftClickLabel, controlLeftClickLabel, rightClickLabel, controlRightClickLabel;
    JLabel escapeLabel, enterLabel, mouseWheelLabel, middleClickLabel, leftClickDragLabel, altLeftClickDragLabel;
    ImageIcon MAIcon;
    int helpFrameLocationX = 10;
    int helpFrameLocationY = 10;
    double maxPeakValue = Double.MAX_VALUE, minPeakValue = 2000, upperBrightness = 150, lowerBrightness = 50, brightness2DistanceWeight = 50;
    int maxSuggestedStars = 12;
    boolean useWCS = false, suggestCompStars = true, tempSuggestCompStars = true;
    boolean useMA = true, useAlign = false;
    TimerTask stackTask = null;
    java.util.Timer stackTaskTimer = null;
    boolean doubleClick = false;
    TimerTask doubleClickTask = null;
    java.util.Timer doubleClickTaskTimer = null;
    DecimalFormat uptoEightPlaces = new DecimalFormat("#####0.########", IJU.dfs);
    double max = 0;
    private double gaussRadius = 3.5;
    private boolean autoRadius = true, t1Placed = false;
    private Seeing_Profile.ApRadii oldRadii;
    private int referenceStar = 1;

//	public static double RETRY_RADIUS = 3.0;

    static void checkAndLockTable() {
        if (table == null) return;
        int cnt = 0;  //timeout after 4 second
        while (table != null && table.isLocked() && cnt < 200) {
            IJ.wait(20);
            cnt++;
        }
        if (table != null) table.setLock(true);
    }

    static public void clearTable() {
        if (table != null && MeasurementTable.getTextPanel(tableName) != null) {
            table = new MeasurementTable(tableName);
            table.show();
        } else {
            IJ.showStatus("No table to clear");
        }
        if (table != null) table.setLock(false);

    }

    /**
     * Standard ImageJ PluginFilter setup routine.
     */
    public int setup(String arg, ImagePlus imp) {
        Locale.setDefault(IJU.locale);
        this.getMeasurementPrefs();
        if (useMacroImage) {
            openImage = WindowManager.getImage(macroImageName);
            imp = openImage;
        }
        if (imp == null) {
            cancel();
            return DONE;
        }

        if (imp.getWindow() instanceof PlotWindow)     //if plotwindow is selected and only one other image window is open
        {                                          //switch to other image if not a plotwindow, otherwise message to user
            int numImages = WindowManager.getImageCount();
            if (numImages == 1) {
                IJ.showMessage("No image windows open for Multi-aperture processing");
                cancel();
                return DONE;
            }
            if (numImages == 2) {
                int[] idList = WindowManager.getIDList();
                if (idList == null) {
                    cancel();
                    return DONE;
                }
                imp = idList[0] == imp.getID() ? WindowManager.getImage(idList[1]) : WindowManager.getImage(idList[0]);
                if ((imp != null ? imp.getWindow() : null) instanceof PlotWindow) {
                    IJ.showMessage("No image windows open for Multi-aperture processing");
                    cancel();
                    return DONE;
                }
            } else {
                IJ.showMessage("Select image window to process, then restart Multi-aperture");
                cancel();
                return DONE;
            }
        }

        // TO MAKE SURE THAT THE NEXT CLICK WILL WORK
        toolbar = Toolbar.getInstance();
        astronomyToolId = toolbar.getToolId("Astronomy_Tool");
        currentToolId = Toolbar.getToolId();
        if (currentToolId != astronomyToolId) {
            if (astronomyToolId != -1) { IJ.setTool(astronomyToolId); } else { IJ.setTool(0); }
        }

        IJ.register(MultiAperture_.class);
        return super.setup(arg, imp);
    }

    /**
     * Standard ImageJ Plug-in method which runs the plug-in, notes whether the image is a stack,
     * and registers the routine for mouse clicks.
     */
    public void run(ImageProcessor ip) {
//        if (table==null)
//            IJ.log("Table is null");
//        else
//            IJ.log("Table rows = "+table.getCounter());

        Frame[] Frames = JFrame.getFrames();
        if (Frames.length > 0) {
            for (int i = 0; i < Frames.length; i++) {
                if (Frames[i].getTitle().equals("Multi-Aperture Help") || Frames[i].getTitle().equals("Stack Aligner Help")) {
                    Frames[i].dispose();
                }
            }
        }

        Prefs.set(MultiAperture_.PREFS_CANCELED, "false");
        cancelled = false;
        this.getMeasurementPrefs();
        suffix = "_T1";
        if (!autoMode) {
            String[] apsX = xOldApertures.split(",");
            double[] xStored = extract(true, apsX);
            nAperturesStored = xStored == null ? 0 : xStored.length;
            String[] apsY = yOldApertures.split(",");
            double[] yStored = extract(false, apsY);
            if (yStored == null || xStored == null || yStored.length == 0 || xStored.length != yStored.length) {
                nAperturesStored = 0;
            }
        }
        if (useMacroImage) {
            openImage = WindowManager.getImage(macroImageName);

            if (!(openImage == null)) {
                imp = openImage;
            }
        }

        if (imp.getWindow() instanceof astroj.AstroStackWindow) {
            asw = (AstroStackWindow) imp.getWindow();
            ac = (AstroCanvas) imp.getCanvas();
            hasWCS = asw.hasWCS();
            if (hasWCS) wcs = asw.getWCS();
            asw.setDisableShiftClick(true);
        }
        
        max = imp.getStatistics().max;

        // GET HOW MANY APERTURES WILL BE MEASURED WITH WHAT RADII
        maxPeakValue = Prefs.get(MultiAperture_.PREFS_MAXPEAKVALUE, max);

        if (!setUpApertures() || nApertures == 0 || !prepare()) {
            imp.unlock();
            shutDown();
            return;
        }

        imp.setSlice(firstSlice);

        // START ESCAPE ABORTION POSSIBILITY
        IJ.resetEscape();

        // REGISTER FOR MOUSE CLICKS

        if (imp.getWindow() instanceof astroj.AstroStackWindow) {
            asw = (AstroStackWindow) imp.getWindow();
            ac = (AstroCanvas) imp.getCanvas();
            hasWCS = asw.hasWCS();
            if (hasWCS) wcs = asw.getWCS();
            asw.setDisableShiftClick(true);
        }

        if (!autoMode) {
            MouseListener[] ml = canvas.getMouseListeners();
            for (int i = 0; i < ml.length; i++) {
                if (ml[i] instanceof MultiAperture_) {
//                        IJ.log("MouseListener already running");
                    canvas.removeMouseListener(ml[i]);
                }
            }
            MouseMotionListener[] mml = canvas.getMouseMotionListeners();
            for (int i = 0; i < mml.length; i++) {
                if (mml[i] instanceof MultiAperture_) {
//                        IJ.log("MouseMotionListener already running");
                    canvas.removeMouseMotionListener(mml[i]);
                }
            }
            KeyListener[] kl = canvas.getKeyListeners();
            for (int i = 0; i < kl.length; i++) {
                if (kl[i] instanceof MultiAperture_) {
//                        IJ.log("KeyListener already running");
                    canvas.removeKeyListener(kl[i]);
                }
            }
            canvas.addMouseListener(this);
            canvas.addMouseMotionListener(this);
            canvas.addKeyListener(this);
        }

        if (starOverlay || skyOverlay || valueOverlay || nameOverlay) {
            //ocanvas.clearRois();
            ocanvas.removeApertureRois();
            ocanvas.removeAstrometryAnnotateRois();
            ocanvas.removeMarkingRois();
        }
        if (previous && (!useWCS || (useWCS && (raPosStored == null || decPosStored == null)))) {
            infoMessage = "Please select first aperture (right click to finalize) ...";
            IJ.showStatus(infoMessage);
        }
        setApertureColor(Color.green);
        setApertureName("T1");

//        imp.getWindow().requestFocus();
//        imp.getCanvas().requestFocusInWindow();

        if (runningWCSOnlyAlignment) {
            startProcessStack();
        } else if (autoMode) { mouseReleased(dummyClick); } else {
            imp.getWindow().requestFocus();
            imp.getCanvas().requestFocusInWindow();
            if (previous && useWCS && hasWCS && raPosStored != null && decPosStored != null) {
                enterPressed = true;
                simulatedLeftClick = true;
                processSingleClick(dummyClick);
            }
        }
    }

    private void resetRadii() {
        if (oldRadii == null) return;
        radius = oldRadii.r();
        rBack1 = oldRadii.r2();
        rBack2 = oldRadii.r3();
        Prefs.set("aperture.radius", radius);
        Prefs.set("aperture.rback1", rBack1);
        Prefs.set("aperture.rback2", rBack2);
    }

    protected void cancel() {
        if (table != null) table.setLock(false);
        if (table != null) table.show();
        if (table != null) table.setLock(false);
        resetRadii();
        Prefs.set(MultiAperture_.PREFS_AUTOMODE, "false");
        Prefs.set(MultiAperture_.PREFS_FINISHED, "true");
        Prefs.set(MultiAperture_.PREFS_USEMACROIMAGE, "false");
        Prefs.set(MultiAperture_.PREFS_CANCELED, "true");
    }

    /**
     * Get all preferences.
     */
    protected void getMeasurementPrefs() {
        super.getMeasurementPrefs();
        oldBackIsPlane = backIsPlane;
        oldRemoveBackStars = removeBackStars;

        autoMode = Prefs.get(MultiAperture_.PREFS_AUTOMODE, autoMode);
        useMacroImage = Prefs.get(MultiAperture_.PREFS_USEMACROIMAGE, useMacroImage);
        macroImageName = Prefs.get(MultiAperture_.PREFS_MACROIMAGENAME, macroImageName);
        previous = Prefs.get(MultiAperture_.PREFS_PREVIOUS, previous);
        singleStep = Prefs.get(MultiAperture_.PREFS_SINGLESTEP, singleStep);
        openSimbadForAbsMag = Prefs.get("plot2.openSimbadForAbsMag", openSimbadForAbsMag);
        allowSingleStepApChanges = Prefs.get(MultiAperture_.PREFS_ALLOWSINGLESTEPAPCHANGES, allowSingleStepApChanges);
//        wideTable      = Prefs.get (MultiAperture_.PREFS_WIDETABLE, wideTable);
//		follow         = false;	// Prefs.get (MultiAperture_.PREFS_FOLLOW,    follow);
        oldradius = radius;
        oldrBack1 = rBack1;
        oldrBack2 = rBack2;
        showRatio = Prefs.get(MultiAperture_.PREFS_SHOWRATIO, showRatio);
        showCompTot = Prefs.get(MultiAperture_.PREFS_SHOWCOMPTOT, showCompTot);
        showRatioError = Prefs.get(MultiAperture_.PREFS_SHOWRATIO_ERROR, showRatioError);
        showRatioSNR = Prefs.get(MultiAperture_.PREFS_SHOWRATIO_SNR, showRatioSNR);
        useVarSizeAp = Prefs.get(MultiAperture_.PREFS_USEVARSIZEAP, useVarSizeAp);
        calcRadProFWHM = Prefs.get(Aperture_.AP_PREFS_CALCRADPROFWHM, calcRadProFWHM);
        reposition = Prefs.get("aperture.reposition", reposition);
        haltOnError = Prefs.get(MultiAperture_.PREFS_HALTONERROR, haltOnError);
        useWCS = Prefs.get(MultiAperture_.PREFS_USEWCS, useWCS);
        maxPeakValue = Prefs.get(MultiAperture_.PREFS_MAXPEAKVALUE, maxPeakValue);
        minPeakValue = Prefs.get(MultiAperture_.PREFS_MINPEAKVALUE, minPeakValue);
        upperBrightness = Prefs.get(MultiAperture_.PREFS_UPPERBRIGHTNESS, upperBrightness);
        lowerBrightness = Prefs.get(MultiAperture_.PREFS_LOWERBRIGHTNESS, lowerBrightness);
        brightness2DistanceWeight = Prefs.get(MultiAperture_.PREFS_BRIGHTNESSDISTANCE, brightness2DistanceWeight);
        maxSuggestedStars = (int) Prefs.get(MultiAperture_.PREFS_MAXSUGGESTEDSTARS, maxSuggestedStars);
        suggestCompStars = Prefs.get(PREFS_SUGGESTCOMPSTARS, suggestCompStars);
        debugAp = Prefs.get(PREFS_DEBUGAPERTURESUGGESTION, debugAp);
        gaussRadius = Prefs.get(PREFS_GAUSSRADIUS, gaussRadius);
        autoPeakValues = Prefs.get(PREFS_AUTOPEAKS, autoPeakValues);
        autoRadius = Prefs.get(PREFS_AUTORADIUS, autoRadius);
        enableLog = Prefs.get(PREFS_ENABLELOG, enableLog);
        referenceStar = (int) Prefs.get(PREFS_REFERENCESTAR, referenceStar);
        oldUseVarSizeAp = useVarSizeAp;
        apFWHMFactor = Prefs.get(MultiAperture_.PREFS_APFWHMFACTOR, apFWHMFactor);
        oldapFWHMFactor = apFWHMFactor;
        autoModeFluxCutOff = Prefs.get(MultiAperture_.PREFS_AUTOMODEFLUXCUTOFF, autoModeFluxCutOff);
        oldAutoModeFluxCutOff = autoModeFluxCutOff;
        nAperturesMax = (int) Prefs.get(MultiAperture_.PREFS_NAPERTURESMAX, nAperturesMax);
        enableDoubleClicks = Prefs.get(MultiAperture_.PREFS_ENABLEDOUBLECLICKS, enableDoubleClicks);
        showHelp = Prefs.get(MultiAperture_.PREFS_SHOWHELP, showHelp);
        alwaysstartatfirstSlice = Prefs.get(MultiAperture_.PREFS_ALWAYSFIRSTSLICE, alwaysstartatfirstSlice);
        xOldApertures = Prefs.get(MultiAperture_.PREFS_XAPERTURES, "");
        yOldApertures = Prefs.get(MultiAperture_.PREFS_YAPERTURES, "");
        raOldApertures = Prefs.get(MultiAperture_.PREFS_RAAPERTURES, "");
        decOldApertures = Prefs.get(MultiAperture_.PREFS_DECAPERTURES, "");
        absMagOldApertures = Prefs.get(MultiAperture_.PREFS_ABSMAGAPERTURES, "");
        isOldRefStar = Prefs.get(MultiAperture_.PREFS_ISREFSTAR, "");
        isOldAlignStar = Prefs.get(MultiAperture_.PREFS_ISALIGNSTAR, "");
        oldCentroidStar = Prefs.get(MultiAperture_.PREFS_CENTROIDSTAR, "");
        updatePlot = Prefs.get(MultiAperture_.PREFS_UPDATEPLOT, updatePlot);
        getMags = Prefs.get(MultiAperture_.PREFS_GETMAGS, getMags);
        oldGetMags = getMags;
        xLocation = (int) Prefs.get(MultiAperture_.PREFS_XLOCATION, xLocation);
        yLocation = (int) Prefs.get(MultiAperture_.PREFS_YLOCATION, yLocation);
        xAbsMagLocation = (int) Prefs.get("plot2.absMagFrameLocationX", xAbsMagLocation);
        yAbsMagLocation = (int) Prefs.get("plot2.absMagFrameLocationY", yAbsMagLocation);
        helpFrameLocationX = (int) Prefs.get("plot2.helpFrameLocationX", helpFrameLocationX);
        helpFrameLocationY = (int) Prefs.get("plot2.helpFrameLocationY", helpFrameLocationY);
    }

    /**
     * Initializes variables etc.
     */
    protected boolean prepare() {
//		if (!checkResultsTable ()) return false;  //removed to allow dynamic number of apertures using right click to terminate aperture selections
        // LOAD PREVIOUS APERTURES IF DESIRED
        isRefStar = new boolean[nAperturesMax];
        isAlignStar = new boolean[nAperturesMax];
        centroidStar = new boolean[nAperturesMax];
        absMag = new double[nAperturesMax];
        if (previous) {
//            List<String> aps = new ArrayList<String>(Arrays.asList(xOldApertures.split(",")));
            String[] aps = xOldApertures.split(",");
            xPosStored = extract(true, aps);
            nAperturesStored = xPosStored == null ? 0 : xPosStored.length;
            if (nAperturesStored == 0) {
                IJ.error("There are no stored apertures");
                return false;
            }

            aps = yOldApertures.split(",");
            yPosStored = extract(false, aps);
            if (yPosStored == null || yPosStored.length == 0) {
                IJ.error("There are no stored aperture y-positions");
                return false;
            }
            if (yPosStored.length != nAperturesStored) {
                IJ.error("The number of stored x and y aperture positions are not equal: " + nAperturesStored + "!=" + yPosStored.length);
                return false;
            }

            aps = raOldApertures.split(",");
            raPosStored = extractDoubles(aps);
            if (raPosStored == null || raPosStored.length != nAperturesStored) {
                raOldApertures = "";
                raPosStored = null;
            }
            aps = decOldApertures.split(",");
            decPosStored = extractDoubles(aps);
            if (decPosStored == null || decPosStored.length != nAperturesStored) {
                decOldApertures = "";
                decPosStored = null;
            }
            aps = absMagOldApertures.split(",");
            absMagStored = extractAbsMagDoubles(aps);
            if (absMagStored == null || absMagStored.length != nAperturesStored) {
                absMagOldApertures = "";
                absMagStored = new double[nAperturesStored];
                for (int ap = 0; ap < nAperturesStored; ap++) {
                    absMagStored[ap] = 99.999;
                }
            }

            aps = isOldRefStar.split(",");
            isRefStarStored = extractBoolean(aps);
            if (isRefStarStored == null || isRefStarStored.length != nAperturesStored) {
                isRefStarStored = new boolean[nAperturesStored];
                for (int ap = 0; ap < nAperturesStored; ap++) {
                    isRefStarStored[ap] = ap != 0;
                }
            }

            aps = isOldAlignStar.split(",");
            isAlignStarStored = extractBoolean(aps);
            if (isAlignStarStored == null || isAlignStarStored.length != nAperturesStored) {
                isAlignStarStored = new boolean[nAperturesStored];
                for (int ap = 0; ap < nAperturesStored; ap++) {
                    isAlignStarStored[ap] = true;
                }
            }

            aps = oldCentroidStar.split(",");
            centroidStarStored = extractBoolean(aps);
            if (centroidStarStored == null || centroidStarStored.length != nAperturesStored) {
                centroidStarStored = new boolean[nAperturesStored];
                for (int ap = 0; ap < nAperturesStored; ap++) {
                    centroidStarStored[ap] = reposition;
                }
            }

            int size = Math.min(nAperturesStored, nAperturesMax);
            for (int ap = 0; ap < size; ap++) {
                isRefStar[ap] = isRefStarStored[ap];
                isAlignStar[ap] = isAlignStarStored[ap];
                centroidStar[ap] = centroidStarStored[ap];
            }
        }
        if (autoMode) {
            String[] aps = xOldApertures.split(",");
            xPos = extract(true, aps);
            if (xPos == null || xPos.length == 0) {
                IJ.error("There are no stored x-positions for apertures.");
                return false;
            }
            nApertures = xPos.length;


            aps = yOldApertures.split(",");
            yPos = extract(false, aps);
            if (yPos == null || yPos.length == 0 || yPos.length != nApertures) {
                if (yPos == null || yPos.length == 0) {
                    IJ.error("The are no stored y-positions for apertures.");
                } else {
                    IJ.error("The number of stored aperture y-positions is not consistent with the number of stored x-positions: " + nApertures + "!=" + yPos.length);
                }
                return false;
            }

            aps = raOldApertures.split(",");
            raPos = extractDoubles(aps);
            if (((useMA || useAlign) && useWCS) && (raPos == null || raPos.length != nApertures)) {
                if (raPos == null) {
                    IJ.error("Locate apertures by RA/Dec requested, but no stored RA-positions found.");
                } else {
                    IJ.error("The number of stored aperture RA-positions is not consistent with the number of stored x-positions: " + nApertures + "!=" + raPos.length);
                }
                return false;
            }
            if (raPos == null || raPos.length != nApertures) {
                raPos = new double[nApertures];
                for (int ap = 0; ap < nApertures; ap++) {
                    raPos[ap] = -1000001;
                }
            }

            aps = decOldApertures.split(",");
            decPos = extractDoubles(aps);
            if (((useMA || useAlign) && useWCS) && (decPos == null || decPos.length != nApertures)) {
                if (decPos == null) {
                    IJ.error("Locate apertures by RA/Dec requested, but no stored Dec-positions found.");
                } else {
                    IJ.error("The number of stored aperture Dec-positions is not consistent with the number of stored x-positions: " + nApertures + "!=" + decPos.length);
                }
                return false;
            }
            if (decPos == null || decPos.length != nApertures) {
                decPos = new double[nApertures];
                for (int ap = 0; ap < nApertures; ap++) {
                    decPos[ap] = -1000001;
                }
            }

            aps = absMagOldApertures.split(",");
            absMag = extractAbsMagDoubles(aps);
            if (absMag == null || absMag.length != nApertures) {
                absMag = new double[nApertures];
                for (int ap = 0; ap < nApertures; ap++) {
                    absMag[ap] = 99.999;
                }
            }

            aps = isOldRefStar.split(",");
            isRefStar = extractBoolean(aps);
            if (isRefStar == null || isRefStar.length != nApertures) {
                isRefStar = new boolean[nApertures];
                for (int ap = 0; ap < nApertures; ap++) {
                    isRefStar[ap] = ap != 0;
                }
            }

            aps = isOldAlignStar.split(",");
            isAlignStar = extractBoolean(aps);
            if (isAlignStar == null || isAlignStar.length != nApertures) {
                isAlignStar = new boolean[nApertures];
                for (int ap = 0; ap < nApertures; ap++) {
                    isAlignStar[ap] = true;
                }
            }


            aps = oldCentroidStar.split(",");
            centroidStar = extractBoolean(aps);
            if (centroidStar == null || centroidStar.length != nApertures) {
                centroidStar = new boolean[nApertures];
                for (int ap = 0; ap < nApertures; ap++) {
                    centroidStar[ap] = reposition;
                }
            }

            aperturesInitialized = true;

        } else {
            xPos = new double[nApertures];
            yPos = new double[nApertures];
            absMag = new double[nApertures];
            for (int ap = 0; ap < nApertures; ap++) {
                absMag[ap] = 99.999;
            }
            raPos = new double[nApertures];
            decPos = new double[nApertures];
        }

        if (xPos == null || yPos == null || absMag == null || raPos == null || decPos == null || isRefStar == null || isAlignStar == null || centroidStar == null) {
            IJ.error("Null aperture arrays???");
            IJ.beep();
            return false;
        }
        imp.setSlice(firstSlice);
        ip = imp.getProcessor();
        imp.killRoi();
        return true;
    }

    /**
     * Extracts a double array from a string array.
     */
    protected double[] extract(boolean XnotY, String[] s) {
        boolean isFITS = false;
        double[] arr = new double[s.length];
        if (s.length > 0 && s[0].startsWith("FITS")) {
            isFITS = true;
            s[0] = s[0].substring(4);
        }
        try {
            for (int i = 0; i < arr.length; i++)
                arr[i] = Double.parseDouble(s[i]);
        } catch (NumberFormatException e) {
            arr = null;
        }
        if (arr != null && isFITS) {
            if (XnotY) {
                for (int ap = 0; ap < arr.length; ap++) {
                    arr[ap] -= Centroid.PIXELCENTER;
                }
            } else {
                for (int ap = 0; ap < arr.length; ap++) {
                    arr[ap] = (double) imp.getHeight() - arr[ap] + Centroid.PIXELCENTER;
                }
            }
        }

        return arr;
    }

    /**
     * Extracts a double array from a string array.
     */
    protected double[] extractDoubles(String[] s) {
        double[] arr = new double[s.length];
        try {
            for (int i = 0; i < arr.length; i++)
                arr[i] = Double.parseDouble(s[i]);
        } catch (NumberFormatException e) {
            arr = null;
        }
        return arr;
    }

    /**
     * Extracts a double array from a string array and returns 99.999 as NaN.
     */
    protected double[] extractAbsMagDoubles(String[] s) {
        if (s == null || s.length < 1) return null;
        double[] arr = new double[s.length];

        for (int i = 0; i < arr.length; i++)
            arr[i] = Tools.parseDouble(s[i], 99.999);

        return arr;
    }

    /**
     * Extracts a boolean array from a string array.
     */
    protected boolean[] extractBoolean(String[] s) {
        boolean[] arr = new boolean[s.length];

        for (int i = 0; i < arr.length; i++) {
            arr[i] = s[i].equalsIgnoreCase("true");
        }

        return arr;
    }

    //
    // MouseListener METHODS
    //

    /**
     * Stops reception of mouse and keyboard clicks.
     */
    protected void noMoreInput() {
        if (!autoMode && helpFrame != null) {
            leftClickLabel.setText("");
            shiftLeftClickLabel.setText("");
            shiftControlLeftClickLabel.setText("");
            altLeftClickLabel.setText("");
            rightClickLabel.setText("");
            enterLabel.setText("");
            leftClickDragLabel.setText("Pan image up/down/left/right");
            altLeftClickDragLabel.setText("Measure arclength");
        }
        canvas.removeMouseListener(this);
        canvas.removeKeyListener(this);
        if (imp.getWindow() instanceof astroj.AstroStackWindow) {
            asw = (AstroStackWindow) imp.getWindow();
            asw.setDisableShiftClick(false);
        }
    }

    /**
     * Finishes whole process.
     */
    public void shutDown() {
        noMoreInput();
        closeHelpPanel();
        super.shutDown();
        if (asw != null && asw.autoDisplayAnnotationsFromHeader) asw.displayAnnotationsFromHeader(true, true, false);

        if (stackTask != null) stackTask = null;
        if (stackTaskTimer != null) {
            stackTaskTimer.cancel();
            stackTaskTimer = null;
        }
        if (doubleClickTask != null) doubleClickTask = null;
        if (doubleClickTaskTimer != null) doubleClickTaskTimer = null;
        if ((table != null) && !Data_Processor.active && !isInstanceOfStackAlign) {
            table.show();  // && Data_Processor.runMultiPlot)
        }
        if (table != null) table.setLock(false);
        if (processingStack && table != null && !isInstanceOfStackAlign && !updatePlot && !Data_Processor.active) {
            if (MultiPlot_.mainFrame != null) {
                if (MultiPlot_.getTable() != null && MultiPlot_.getTable().equals(table)) {
                    MultiPlot_.updatePlot(MultiPlot_.updateAllFits(), slice == initialLastSlice);
                } else {
                    MultiPlot_.setTable(table, true, slice == initialLastSlice);
                }
            } else {
                IJ.runPlugIn("Astronomy.MultiPlot_", tableName);
                if (MultiPlot_.mainFrame != null && MultiPlot_.getTable() != null) {
                    MultiPlot_.setTable(table, false, slice == initialLastSlice);
                }
            }
        }
        cancelled = true;
        processingStack = false;
        processingImage = false;
        Prefs.set(MultiAperture_.PREFS_NAPERTURESMAX, nAperturesMax);
        Prefs.set(MultiAperture_.PREFS_PREVIOUS, previous);
        Prefs.set("plot2.openSimbadForAbsMag", openSimbadForAbsMag);
        Prefs.set(MultiAperture_.PREFS_SINGLESTEP, singleStep);
        Prefs.set(MultiAperture_.PREFS_ALLOWSINGLESTEPAPCHANGES, allowSingleStepApChanges);
        Prefs.set(MultiAperture_.PREFS_USEVARSIZEAP, useVarSizeAp);
        Prefs.set(MultiAperture_.PREFS_HALTONERROR, haltOnError);
        Prefs.set(MultiAperture_.PREFS_USEMA, useMA);
        Prefs.set(MultiAperture_.PREFS_USEALIGN, useAlign);
        Prefs.set(MultiAperture_.PREFS_USEWCS, useWCS);
        Prefs.set(PREFS_SUGGESTCOMPSTARS, suggestCompStars);
        Prefs.set(MultiAperture_.PREFS_SHOWHELP, showHelp);
        Prefs.set(MultiAperture_.PREFS_ALWAYSFIRSTSLICE, alwaysstartatfirstSlice);
        Prefs.set(MultiAperture_.PREFS_APFWHMFACTOR, apFWHMFactor);
        Prefs.set(MultiAperture_.PREFS_AUTOMODEFLUXCUTOFF, autoModeFluxCutOff);

//        Prefs.set (MultiAperture_.PREFS_WIDETABLE, wideTable);
        Prefs.set(MultiAperture_.PREFS_SHOWRATIO, showRatio);
        Prefs.set(MultiAperture_.PREFS_SHOWCOMPTOT, showCompTot);
        Prefs.set(MultiAperture_.PREFS_SHOWRATIO_ERROR, showRatioError);
        Prefs.set(MultiAperture_.PREFS_SHOWRATIO_SNR, showRatioSNR);
        Prefs.set(MultiAperture_.PREFS_ENABLEDOUBLECLICKS, enableDoubleClicks);
//        Prefs.set (MultiAperture_.PREFS_FOLLOW, follow);
        Prefs.set(MultiAperture_.PREFS_AUTOMODE, "false");
        Prefs.set(MultiAperture_.PREFS_FINISHED, "true");
        Prefs.set(MultiAperture_.PREFS_USEMACROIMAGE, "false");
        Prefs.set(MultiAperture_.PREFS_XLOCATION, xLocation);
        Prefs.set(MultiAperture_.PREFS_YLOCATION, yLocation);
        Prefs.set("plot2.absMagFrameLocationX", xAbsMagLocation);
        Prefs.set("plot2.absMagFrameLocationY", yAbsMagLocation);
        Prefs.set("plot2.helpFrameLocationX", helpFrameLocationX);
        Prefs.set("plot2.helpFrameLocationY", helpFrameLocationY);
        resetRadii();
    }

    /**
     * Main MouseListener method used: process all mouse clicks.
     */
    public void mouseReleased(MouseEvent e) {
        ee = e;
        mouseDrag = false;
        if (!enterPressed && !autoMode) {
            screenX = e.getX();
            screenY = e.getY();
            modis = e.getModifiers();
            mouseDrag = (Math.abs(screenX - startDragScreenX) + Math.abs(screenY - startDragScreenY) >= 2.0);
        }
        if (mouseDrag && !aperturesInitialized && selectedApertureRoi != null) {
            int ap = selectedApertureRoi.getApNumber();
            if (ap >= 0 && ap < ngot) {
                if (e.isAltDown()) {
                    centroidStar[ap] = !centroidStar[ap];
                    selectedApertureRoi.setIsCentroid(centroidStar[ap]);
                }
                boolean holdReposition = Prefs.get("aperture.reposition", reposition);
                Prefs.set("aperture.reposition", centroidStar[ap]);
                xCenter = xPos[ap];
                yCenter = yPos[ap];
                if (!adjustAperture(true)) {
                    if (haltOnError || this instanceof Stack_Aligner) {
                        selectedApertureRoi = null;
                        asw.setMovingAperture(false);
                        Prefs.set("aperture.reposition", holdReposition);
                        centerROI();
                        setVariableAperture(false);
                        IJ.beep();
                        IJ.showMessage("No signal for centroid in aperture " + (ap + 1) + " of image " +
                                IJU.getSliceFilename(imp, slice) +
                                ((this instanceof Stack_Aligner) ? ". Stack Aligner aborted." : ". Multi-Aperture aborted."));
                        shutDown();
                        return;
                    }
                }
                Prefs.set("aperture.reposition", holdReposition);
                xPos[ap] = xCenter;
                yPos[ap] = yCenter;
                selectedApertureRoi.setLocation(xPos[ap], yPos[ap]);
                if (hasWCS) {
                    double[] radec = wcs.pixels2wcs(new double[]{xPos[ap], yPos[ap]});
                    raPos[ap] = radec[0];
                    decPos[ap] = radec[1];
                }
                absMag[ap] = getAbsMag(ap, raPos[ap], decPos[ap]);
                selectedApertureRoi.setIntCnts(source);
                updateApMags();
                ac.repaint();
                selectedApertureRoi = null;
                asw.setMovingAperture(false);
                return;
            }
        }
        selectedApertureRoi = null;
        asw.setMovingAperture(false);

        if (!enterPressed && !autoMode) {
            mouseDrag = (Math.abs(screenX - startDragScreenX) + Math.abs(screenY - startDragScreenY) >= 4.0);
        }
        if (autoMode || !enableDoubleClicks) {
            processSingleClick(ee);
        } else {
            if (e.getClickCount() == 1) {
                doubleClick = false;
                try {
                    doubleClickTask = new TimerTask() {
                        public void run() {
                            if (!doubleClick) processSingleClick(ee);
                            doubleClickTask = null;
                            doubleClickTaskTimer = null;
                        }
                    };
                    doubleClickTaskTimer = new java.util.Timer();
                    if ((modis & InputEvent.BUTTON1_MASK) != 0) {
                        doubleClickTaskTimer.schedule(doubleClickTask, 300);
                    } else { doubleClickTaskTimer.schedule(doubleClickTask, 600); }
                } catch (Exception eee) {
                    IJ.showMessage("Error starting double click timer task : " + eee.getMessage());
                }
            } else {
                doubleClick = true;
            }
        }
    }

    void processSingleClick(MouseEvent e) {

//        if (!enterPressed && !autoMode)
//            {
//            screenX = e.getX();
//            screenY = e.getY();
//            modis = e.getModifiers();
//            }

        //Right mouse click or <Enter> finalizes aperture selection
        if (enterPressed || (!mouseDrag && (modis & InputEvent.BUTTON3_MASK) != 0 && !e.isShiftDown() && !e.isControlDown() && !e.isAltDown())) {
            enterPressed = false;
            if (!aperturesInitialized) {
                if ((!previous && ngot > 0) || allStoredAperturesPlaced) {
                    nApertures = ngot;
                    simulatedLeftClick = true;
                    aperturesInitialized = true;
                }
            }
        }

        //do nothing unless automode or left mouse is clicked with no modifier keys pressed except "shift" or "alt"
        if (autoMode || simulatedLeftClick || (!mouseDrag &&
                (modis & InputEvent.BUTTON1_MASK) != 0 && (!e.isControlDown() || e.isShiftDown()) && !e.isMetaDown())) {
            if (!autoMode && previous && !firstClick && !allStoredAperturesPlaced)  //ignore clicks while placing stored apertures
            {
                return;
            }

            simulatedLeftClick = false;

            if (!autoMode && firstSlice > stackSize) {
                IJ.beep();
                shutDown();
                return;
            }

            slice = imp.getCurrentSlice();
            if (imp.getWindow() instanceof AstroStackWindow) {
                asw = (AstroStackWindow) imp.getWindow();
                ac = (AstroCanvas) imp.getCanvas();
                hasWCS = asw.hasWCS();
                if (hasWCS) wcs = asw.getWCS();
                asw.setDisableShiftClick(true);
            }
            apertureClicked = false;
            xCenter = e != null ? canvas.offScreenXD(e.getX()) : 0;
            yCenter = e != null ? canvas.offScreenYD(e.getY()) : 0;
            if (!autoMode && !(previous && firstClick) && ngot < nApertures) {
                apertureClicked = ocanvas.findApertureRoi((int) xCenter, (int) yCenter, 0) != null;
            }

            if (firstClick && autoRadius && !t1Placed) {
                oldRadii = new Seeing_Profile.ApRadii(radius, rBack1, rBack2);
                var rs = Seeing_Profile.getRadii(imp, previous ? xPosStored[0] : xCenter, previous ? yPosStored[0] : yCenter);
                radius = rs.r();
                rBack1 = rs.r2();
                rBack2 = rs.r3();
                Prefs.set("aperture.radius", radius);
                Prefs.set("aperture.rback1", rBack1);
                Prefs.set("aperture.rback2", rBack2);
            }

            // ADD APERTURE TO LIST OR SHIFT OLD APERTURE POSITIONS

            if (autoMode) {
                ngot = nApertures;
                if (!placeApertures(0, ngot - 1, ENABLECENTROID, CLEARROIS)) {
                    shutDown();
                    return;
                }
                updateApMags();
            } else if (previous && firstClick) {
                ngot = nAperturesStored;
                if (!placeApertures(0, ngot - 1, ENABLECENTROID, CLEARROIS)) return;
                updateApMags();
                firstClick = false;
                allStoredAperturesPlaced = true;
                if (singleStep && !allowSingleStepApChanges && firstSlice > initialFirstSlice) {
                    nApertures = ngot;
                }
            } else if (!apertureClicked && ngot < nApertures) {
                if (!e.isControlDown()) {
                    addAperture((!e.isShiftDown() && ngot > 0) || (e.isShiftDown() && ngot == 0), e.isAltDown());
                } else { addApertureAsT1(e.isAltDown()); }
            } else if (apertureClicked) {
                if (!e.isShiftDown() && !e.isControlDown() && !e.isAltDown()) {
                    if (!removeAperture()) {
                        apertureClicked = false;
                        return;
                    }
                } else if (e.isShiftDown() && !e.isControlDown()) {
                    toggleApertureType(e.isAltDown());
                } else if (e.isShiftDown() && e.isControlDown()) {
                    renameApertureToT1(e.isAltDown());
                } else if (e.isAltDown()) { toggleCentroidType(); }
                apertureClicked = false;
            }

            if (singleStep && ngot >= nApertures) {
                //PROCESS ONE SLICE AT A TIME WHILE IN SINGLE STEP MODE
                xOld = xPos.clone();
                yOld = yPos.clone();
                if (!checkResultsTable()) {
                    IJ.showMessage("Multi-Aperture failed to create Measurements table");
                    IJ.beep();
                    shutDown();
                }
                processStack();
                saveNewApertures();
                previous = true;
                Prefs.set(MultiAperture_.PREFS_PREVIOUS, previous);
                firstSlice += 1;
                lastSlice = firstSlice;
                if (firstSlice > stackSize) {
                    IJ.beep();
                    shutDown();
                    return;
                }
                imp.setSlice(firstSlice);
                imp.updateImage();
                if (starOverlay || skyOverlay || valueOverlay || nameOverlay) {
                    ocanvas.clearRois();
                }
                ip = imp.getProcessor();
                firstClick = true;
                allStoredAperturesPlaced = false;
                nApertures = nAperturesMax;
                if (allowSingleStepApChanges) aperturesInitialized = false;
                IJ.showStatus("Identify star 1 to place all apertures (Esc to exit).");
                if (helpFrame != null) {
                    leftClickLabel.setText("Identify star 1 to place all apertures");
                    shiftLeftClickLabel.setText("");
                    shiftControlLeftClickLabel.setText("");
                    altLeftClickLabel.setText("");
                    rightClickLabel.setText(this instanceof Stack_Aligner ? "" : "");//"Cancel Stack Aligner" : "Cancel Multi-Aperture");
                    enterLabel.setText(this instanceof Stack_Aligner ? "" : "");//"Cancel Stack Aligner" : "Cancel Multi-Aperture");
                    leftClickDragLabel.setText("Pan image up/down/left/right");
                    altLeftClickDragLabel.setText("Measure arclength");
                }
            }

            // GOT ALL APERTURES?
            else if (ngot < nApertures) {
                infoMessage = "Click to select aperture #" + (ngot + 1) + " (<ESC> to abort).";
                IJ.showStatus(infoMessage);
                if (helpFrame != null) {
                    if (ngot > 0) {
                        leftClickLabel.setText("Add reference star aperture C" + (ngot + 1) + ", or delete aperture");
                        shiftLeftClickLabel.setText("Add target star aperture T" + (ngot + 1) + ", or change T/C designation");
                        shiftControlLeftClickLabel.setText("Add target star aperture T1, or rename aperture to T1");
                        altLeftClickLabel.setText("Toggle centroid setting of existing aperture or new aperture");
                        rightClickLabel.setText("Finalize aperture selection" + (singleStep ? (this instanceof Stack_Aligner ? ", align image, and move to next image" : ", perform photometry, and move to next image") : " and start processing"));
                        enterLabel.setText("Finalize aperture selection" + (singleStep ? (this instanceof Stack_Aligner ? ", align image, and move to next image" : ", perform photometry, and move to next image") : " and start processing"));
                        leftClickDragLabel.setText("Move aperture, or pan image up/down/left/right");
                        altLeftClickDragLabel.setText("Move aperture & toggle centroid, or measure arclength");
                    } else {
                        leftClickLabel.setText("Add target star aperture T" + (ngot + 1));
                        shiftLeftClickLabel.setText("Add reference star aperture C" + (ngot + 1));
                        shiftControlLeftClickLabel.setText("");
                        altLeftClickLabel.setText("Invert sense of centroid setting for new aperture");
                        rightClickLabel.setText("");
                        enterLabel.setText("");
                        leftClickDragLabel.setText("Pan image up/down/left/right");
                        altLeftClickDragLabel.setText("Measure arclength");
                    }
                }
            } else {    // PROCESS ALL SLICES WHEN NOT IN SINGLE STEP MODE
                noMoreInput();
                xOld = xPos.clone();
                yOld = yPos.clone();
                saveNewApertures();
                if (!checkResultsTable()) {
                    IJ.showMessage("Multi-Aperture failed to create Measurements table");
                    IJ.beep();
                    shutDown();
                }
                if (!singleStep) {
                    if (stackSize > 1 && doStack) {
                        IJ.showStatus("Processing stack...");
                        processingStack = true;
                        startProcessStack();
                    } else {
                        IJ.showStatus("Processing image...");
                        processImage();
                    }
                }
                if (!processingStack && !autoMode && (firstSlice < lastSlice)) IJ.beep();
                if (!processingStack) shutDown();
            }

            if (suggestCompStars && !tempSuggestCompStars && debugAp) {
                ocanvas.removeMarkingRois();
                debugAp = false;
            }

            var refCount = 0;
            for (boolean b : isRefStar) {
                if (b) refCount++;
            }

            if (suggestCompStars && tempSuggestCompStars && ngot >= referenceStar && refCount < maxSuggestedStars) {
                xCenter = xPos[referenceStar - 1];
                yCenter = yPos[referenceStar - 1];

                // Make sure photometry is current
                measurePhotometry();

                final var t1Source = photom.sourceBrightness();

                final var liveStats = asw.getLiveStatistics();
                var minP = autoPeakValues ? liveStats.mean + (1 * liveStats.stdDev) : minPeakValue;
                var maxP = autoPeakValues ? liveStats.max * 0.9 : maxPeakValue;

                var maxima = StarFinder.findLocalMaxima(imp, minP, Double.MAX_VALUE, (int) Math.ceil(2 * radius), gaussRadius);

                if (maxima.coordinateMaximas().size() > 25000) {
                    var g = new GenericSwingDialog("MA Automatic Comp. Star Selection");
                    g.addMessage("Maxima count has exceeded " + NumberFormat.getInstance().format(25000) + "; this can take a while to process, " +
                            "do you wish to continue?\nMaxima count: " + NumberFormat.getInstance().format(maxima.coordinateMaximas().size()) +
                            "\nChanging the peak value bounds will effect this number.\n" + "Probable causes are that " +
                            "the Minimum peak threshold is set too low and/or the Maximum peak threshold is set " +
                            "too high. Ensure that the Minimum value set is above the highest sky background region " +
                            "and that the Maximum is below the saturation level.\n");
                    g.enableYesNoCancel("Continue Auto", "Continue Manual");
                    g.centerDialog(true);
                    g.getOkay().setToolTipText("Continue to automatically extract comp stars");
                    g.getNo().setToolTipText("Cancel the automatic comp star extraction, " +
                            "but continue with manual aperture placement");
                    g.getCancel().setToolTipText("Cancel both automatic and manual aperture placement");
                    g.showDialog();
                    IJ.beep();
                    if (g.wasCanceled()) {
                        cancelled = true;
                        shutDown();
                    } else if (!g.wasOKed()) {
                        tempSuggestCompStars = false;
                        return;
                    }
                }

                if (maxima.coordinateMaximas().size() == 0) {
                    var g = new GenericSwingDialog("MA Automatic Comp. Star Selection");
                    g.addMessage("Number of maxima found is 0!\nProbable causes are that " +
                            "the Minimum peak threshold is set too high and/or the Maximum peak threshold is set " +
                            "too low. Ensure that the Minimum value set is above the highest sky background region " +
                            "and that the Maximum is below the saturation level.\n");
                    g.enableYesNoCancel("Continue Auto", "Continue Manual");
                    g.centerDialog(true);
                    g.getOkay().setToolTipText("Continue to automatically extract comp stars");
                    g.getNo().setToolTipText("Cancel the automatic comp star extraction, " +
                            "but continue with manual aperture placement");
                    g.getCancel().setToolTipText("Cancel both automatic and manual aperture placement");
                    g.showDialog();
                    IJ.beep();
                    if (g.wasCanceled()) {
                        cancelled = true;
                        shutDown();
                    } else if (!g.wasOKed()) {
                        tempSuggestCompStars = false;
                        return;
                    }
                }

                if (cancelled) return;

                if (enableLog) AIJLogger.log("Number of maxima: " + NumberFormat.getInstance().format(maxima.coordinateMaximas().size()));
                if (enableLog) AIJLogger.log("Filtering...");
                var m = removeCloseStars(maxima.coordinateMaximas(), t1Source, maxP);
                if (cancelled) return;
                if (enableLog) AIJLogger.log("Number of maxima that met distance and brightness thresholds: " + NumberFormat.getInstance().format(m.size()));
                if (enableLog) AIJLogger.log("Weighing peaks...");
                var set = weightAndLimitPeaks(m, t1Source);
                if (cancelled) return;

                if (enableLog) AIJLogger.log("Placing suggested comp. stars...");
                for (WeightedCoordinateMaxima coordinateMaxima : set.subList(0, Math.min(maxSuggestedStars - refCount, set.size() - 1))) {
                    if (cancelled) return;
                    if (enableLog) AIJLogger.log(ngot + 1);
                    if (enableLog) AIJLogger.log(coordinateMaxima);//todo apertures placing in wrong coordinates for tica image, fine for others, due to wcs
                    xCenter = coordinateMaxima.cm.x();
                    yCenter = coordinateMaxima.cm.y();

                    addAperture(true, false);
                }
                if (enableLog) AIJLogger.log("Finished placing comp. stars!");

                tempSuggestCompStars = false; // Disable suggestion for all other stars
            }
        }
    }

    private TreeSet<StarFinder.CoordinateMaxima> removeCloseStars(TreeSet<StarFinder.CoordinateMaxima> initialSet, double t1Source, double maxP) {
        final var radius2 = 4 * radius * radius;
        final var high = t1Source * (upperBrightness/100d);
        final var low = t1Source * (lowerBrightness/100d);
        //initialSet.removeIf(cm -> cm.squaredDistanceTo(xPos[0], yPos[0]) <= (radius2));

        if (debugAp) {
            initialSet.forEach(cm -> {
                var ap = new MarkingRoi(cm.x(), cm.y(), false);
                ap.setImage(imp);
                ap.setFillColor(Color.RED);
                ocanvas.add(ap);
            });
        }

        final var radiusHalf = 0.25 * radius * radius;
        final var reversedSet = (TreeSet<StarFinder.CoordinateMaxima>) initialSet.descendingSet();
        final var toRemove = new HashSet<StarFinder.CoordinateMaxima>();
        for (StarFinder.CoordinateMaxima brighter : reversedSet) {
            for (StarFinder.CoordinateMaxima fainter : reversedSet.tailSet(brighter, false)) {
                if (brighter != fainter) {
                    if (brighter.value() < maxP && fainter.value() < maxP) {
                        if (toRemove.contains(brighter)) continue;
                    }
                    if (toRemove.contains(fainter)) continue;
                    var d = brighter.squaredDistanceTo(fainter);
                    if (d <= radius2) {
                        if (d > radiusHalf) {
                            if (fainter.value() > 0.1 * brighter.value()) toRemove.add(brighter);
                        }
                        toRemove.add(fainter);
                    }
                }
            }
        }

        initialSet.removeAll(toRemove);
        initialSet.removeIf(cm -> cm.value() >= maxP);

        TreeSet<StarFinder.CoordinateMaxima> n;
        getMeasurementPrefs();

        n = initialSet.parallelStream().map(m -> {
            // Centroid for all stars
            var center = adjustAperture(imp, m.x(), m.y(), radius, rBack1, rBack2, true).center();

            for (int i = 0; i < xPos.length; i++) {
                if (squaredDistanceTo(xPos[i], yPos[i], center.x(), center.y()) <= (radius2)) return null;
            }

            var photom = measurePhotometry(imp, center.x(), center.y(), radius, rBack1, rBack2);

            var s = photom.sourceBrightness();
            return new StarFinder.CoordinateMaxima(s, center.x(), center.y());
        }).filter(Objects::nonNull).collect(Collectors.toCollection(TreeSet::new));

        // Remove elements where the apertures would be identical
        var m = new ConcurrentLinkedDeque<StarFinder.CoordinateMaxima>();
        n.parallelStream().forEach(c -> {
            if (m.contains(c)) return;
            n.parallelStream().forEach(c2 -> {
                if (c == c2) return;
                if (c2.identicalRoi(c)) m.add(c2);
            });
        });
        n.removeAll(m);

        initialSet = n;

        if (debugAp) {
            initialSet.forEach(cm -> {
                var ap = new MarkingRoi(cm.x(), cm.y(), true);
                ap.setImage(imp);
                ap.setFillColor(Color.BLUE);
                ocanvas.add(ap);
            });
        }

        // Remove all stars that are outside the thresholds
        initialSet.removeIf(c -> !(c.value() >= low && c.value() <= high));

        return (TreeSet<StarFinder.CoordinateMaxima>) initialSet.descendingSet();
    }

    private List<WeightedCoordinateMaxima> weightAndLimitPeaks(TreeSet<StarFinder.CoordinateMaxima> initialSet, final double t1Source) {
        final Comparator<WeightedCoordinateMaxima> x = Comparator.comparingDouble(d -> d.weight);
        final var out = initialSet.parallelStream().map(o -> calculateDistanceBrightnessFactor(t1Source, o)).sorted(x.reversed());
        return out.limit(maxSuggestedStars).collect(Collectors.toList());
    }

    private WeightedCoordinateMaxima calculateDistanceBrightnessFactor(double t1Source, StarFinder.CoordinateMaxima coordinateMaxima) {
        final double imageWidth2 = imp.getWidth() * imp.getWidth();
        final double imageHeight2 = imp.getHeight() * imp.getHeight();
        final double imageDiagonalLength = Math.sqrt(imageHeight2 + imageWidth2);

        final var b = coordinateMaxima.value();
        final double normBrightness;
        if (b <= t1Source) {
            normBrightness = 1.0 - (t1Source - b) / (t1Source * (1.0 - (lowerBrightness / 100.0)));
        } else {
            normBrightness = 1.0 - (b - t1Source) / (t1Source * ((upperBrightness / 100.0) - 1.0));
        }
        final double normDistance = 1 - (distanceTo(xPos[referenceStar - 1], yPos[referenceStar - 1], coordinateMaxima.x(), coordinateMaxima.y()) / imageDiagonalLength);
        return new WeightedCoordinateMaxima(coordinateMaxima, (brightness2DistanceWeight/100d) * normBrightness + (1 - brightness2DistanceWeight/100d) * normDistance);
    }

    record WeightedCoordinateMaxima(StarFinder.CoordinateMaxima cm, double weight) {}

    private double distanceTo(double x1, double y1, double x2, double y2) {
        return Math.sqrt(squaredDistanceTo(x1, y1, x2, y2));
    }

    private double squaredDistanceTo(double x1, double y1, double x2, double y2) {
        final var h = x2 - x1;
        final var v = y2 - y1;
        return h*h + v*v;
    }

    boolean placeApertures(int start, int end, boolean enableCentroid, boolean clearRois) {
        double dx = 0;
        double dy = 0;
        double dxx = 0;
        double dyy = 0;

        if (clearRois && (starOverlay || skyOverlay || valueOverlay || nameOverlay)) {
            //ocanvas.clearRois();
            ocanvas.removeApertureRois();
            ocanvas.removeMarkingRois();
            ocanvas.removeAstrometryAnnotateRois();
        }

        if (!autoMode && previous && firstClick && nAperturesStored > 0) {
            dx = xCenter - xPosStored[0];
            dy = yCenter - yPosStored[0];
            int size = Math.min(nAperturesStored, nAperturesMax);
            for (int ap = 0; ap < size; ap++) {
                xPos[ap] = xPosStored[ap] + dx;
                yPos[ap] = yPosStored[ap] + dy;
                if (raPosStored == null || decPosStored == null) {
                    raPos[ap] = -1000001;
                    decPos[ap] = -1000001;
                } else {
                    raPos[ap] = raPosStored[ap];
                    decPos[ap] = decPosStored[ap];
                }
                if (absMagStored == null) {
                    absMag[ap] = 99.999;
                } else {
                    absMag[ap] = absMagStored[ap];
                }
                isRefStar[ap] = isRefStarStored[ap];
                isAlignStar[ap] = isAlignStarStored[ap];
                centroidStar[ap] = centroidStarStored[ap];
            }
            start = 0;
            end = size - 1;
        }

        for (int ap = start; ap <= end; ap++) {
            setAbsMag(99.999);
            if (!isRefStar[ap]) {
                setApertureColor(Color.green);
                setApertureName("T" + (ap + 1));
            } else {
                setApertureColor(Color.red);
                setApertureName("C" + (ap + 1));
                if (absMag[ap] < 99.0) setAbsMag(absMag[ap]);
            }

            if (!t1Placed) t1Placed = "T1".equals(apertureName);

            if ((useMA || useAlign) && useWCS) {
                if (autoMode && !hasWCS) {
                    return false;
                }
                if (hasWCS && (raPos[ap] < -1000000 || decPos[ap] < -1000000)) {
                    xCenter = xPos[ap];
                    yCenter = yPos[ap];
                } else if (hasWCS && raPos[ap] > -1000000 && decPos[ap] > -1000000) {
                    double[] xy = wcs.wcs2pixels(new double[]{raPos[ap], decPos[ap]});
                    xPos[ap] = xy[0];
                    yPos[ap] = xy[1];
                    xCenter = xy[0];
                    yCenter = xy[1];
                } else if (raPos[ap] < -1000000 && decPos[ap] < -1000000) {
                    IJ.beep();
                    IJ.showMessage("Error", "WCS mode requested but no valid WCS coordinates stored. ABORTING.");
                    Prefs.set(MultiAperture_.PREFS_CANCELED, "true");
                    cancelled = true;
                    shutDown();
                    return false;
                } else {
                    IJ.beep();
                    IJ.showMessage("Error", "WCS mode requested but no valid WCS FITS Headers. ABORTING.");
                    Prefs.set(MultiAperture_.PREFS_CANCELED, "true");
                    cancelled = true;
                    shutDown();
                    return false;
                }
            } else {
                xCenter = xPos[ap];
                yCenter = yPos[ap];
            }

            //fine tune other aperture positions based on first ap position in auto mode or previous mode when not using WCS
            if ((autoMode || previous && firstClick) && centroidStar[0] && !((useMA || useAlign) && useWCS)) {
                if (ap == 0) {
                    boolean holdReposition = Prefs.get("aperture.reposition", reposition);
                    Prefs.set("aperture.reposition", centroidStar[0]);
                    if (!adjustAperture(false)) {
                        if (!autoMode || (autoMode && haltOnError)) {
                            Prefs.set("aperture.reposition", holdReposition);
                            centerROI();
                            imp.unlock();
                            IJ.beep();
                            IJ.showMessage("No signal for centroid in aperture " + apertureName + " of image " +
                                    IJU.getSliceFilename(imp, slice) +
                                    ((this instanceof Stack_Aligner) ? ". Stack Aligner aborted." : ". Multi-Aperture aborted."));
                            shutDown();
                            return false;
                        } else {
                            IJ.beep();
                            IJ.log("***ERROR: No signal for centroid in aperture " + apertureName + " of image " + IJU.getSliceFilename(imp, slice) + ".");
                            IJ.log("********: Measurements are referenced to the non-centroided aperture location");
                        }
                    }
                    Prefs.set("aperture.reposition", holdReposition);
                    dxx = xCenter - xPos[0];
                    dyy = yCenter - yPos[0];
                    xPos[0] = xCenter;
                    yPos[0] = yCenter;
                } else {
                    xPos[ap] += dxx;
                    yPos[ap] += dyy;
                    xCenter += dxx;
                    yCenter += dyy;
                }
            }


            boolean holdReposition = Prefs.get("aperture.reposition", reposition);
            boolean holdHaltOnError = Prefs.get(MultiAperture_.PREFS_HALTONERROR, haltOnError);
            Prefs.set("aperture.reposition", centroidStar[ap] && (enableCentroid || ((useMA || useAlign) && useWCS)));
            setShowAsCentered(centroidStar[ap]);
            Prefs.set(MultiAperture_.PREFS_HALTONERROR, false);
            if (!measureAperture()) {
                if (autoMode && holdHaltOnError) {
                    Prefs.set(MultiAperture_.PREFS_HALTONERROR, holdHaltOnError);
                    haltOnError = holdHaltOnError;
                    Prefs.set("aperture.reposition", holdReposition);
                    centerROI();
                    imp.unlock();
                    IJ.beep();
                    IJ.showMessage("No signal for centroid in aperture " + apertureName + " of image " +
                            IJU.getSliceFilename(imp, slice) +
                            ((this instanceof Stack_Aligner) ? ". Stack Aligner aborted." : ". Multi-Aperture aborted."));
                    shutDown();
                    return false;
                } else {
                    IJ.beep();
                    IJ.log("***ERROR: No signal for centroid in aperture " + apertureName + " of image " + IJU.getSliceFilename(imp, slice) + ".");
                    IJ.log("********: Measurements are referenced to the non-centroided aperture location");
                }
            }
            haltOnError = holdHaltOnError;
            Prefs.set(MultiAperture_.PREFS_HALTONERROR, holdHaltOnError);
            Prefs.set("aperture.reposition", holdReposition);

            xPos[ap] = xCenter;
            yPos[ap] = yCenter;
            if (hasWCS && (raPos[ap] < -1000000 || decPos[ap] < -1000000)) {
                double[] radec = wcs.pixels2wcs(new double[]{xPos[ap], yPos[ap]});
                raPos[ap] = radec[0];
                decPos[ap] = radec[1];
            }
        }
        return true;
    }

    public boolean checkAperturesInitialized() {
        return aperturesInitialized;
    }

    /**
     * Adds the aperture parameters to the list of apertures.
     */
    protected void addAperture(boolean isRef, boolean altDown) {
        xPos[ngot] = xCenter;
        yPos[ngot] = yCenter;
        if (hasWCS) {
            double[] radec = wcs.pixels2wcs(new double[]{xCenter, yCenter});
            raPos[ngot] = radec[0];
            decPos[ngot] = radec[1];
        } else {
            raPos[ngot] = -1000001;
            decPos[ngot] = -1000001;
        }
        isRefStar[ngot] = isRef;
        isAlignStar[ngot] = true;
        centroidStar[ngot] = altDown ? !Prefs.get("aperture.reposition", reposition) : Prefs.get("aperture.reposition", reposition);
        if (!placeApertures(ngot, ngot, ENABLECENTROID, KEEPROIS)) return;

        if (hasWCS && centroidStar[ngot]) {
            double[] radec = wcs.pixels2wcs(new double[]{xCenter, yCenter});
            raPos[ngot] = radec[0];
            decPos[ngot] = radec[1];
        }
        absMag[ngot] = getAbsMag(ngot, raPos[ngot], decPos[ngot]);
        ngot++;
        updateApMags();
    }

    protected double getAbsMag(int ap, double ra, double dec) {
        if (!getMags || !isRefStar[ap] || (previous && !allStoredAperturesPlaced)) {
            return absMag[ap];
        } else {
            openSimbadForAbsMag = Prefs.get("plot2.openSimbadForAbsMag", openSimbadForAbsMag);
            xAbsMagLocation = (int) Prefs.get("plot2.absMagFrameLocationX", xAbsMagLocation);
            yAbsMagLocation = (int) Prefs.get("plot2.absMagFrameLocationY", yAbsMagLocation);
            openSimbadForAbsMag = Prefs.get("plot2.openSimbadForAbsMag", openSimbadForAbsMag);
            if (!Prefs.isLocationOnScreen(new Point(xAbsMagLocation, yAbsMagLocation))) {
                xAbsMagLocation = 10;
                yAbsMagLocation = 10;
            }

            GenericDialog gd = new GenericDialog("Magnitude Entry", xAbsMagLocation, yAbsMagLocation);
            gd.addStringField("Enter " + (isRefStar[ap] ? "C" : "T") + (ap + 1) + " Magnitude", "" + (absMag[ap] > 99.0 ? "" : uptoEightPlaces.format(absMag[ap])), 20);
            if (hasWCS && ra > -1000000 && dec > -1000000) {
                gd.addCheckbox("Open ref star in SIMBAD", openSimbadForAbsMag);
                if (openSimbadForAbsMag) IJU.showInSIMBAD(ra, dec, Prefs.get("Astronomy_Tool.simbadSearchRadius", 10));
            }
            gd.showDialog();

            xAbsMagLocation = gd.getX();
            yAbsMagLocation = gd.getY();
            Prefs.set("plot2.absMagFrameLocationX", xAbsMagLocation);
            Prefs.set("plot2.absMagFrameLocationY", yAbsMagLocation);
            if (hasWCS && ra > -1000000 && dec > -1000000) openSimbadForAbsMag = gd.getNextBoolean();
            Prefs.set("plot2.openSimbadForAbsMag", openSimbadForAbsMag);
            if (gd.wasCanceled()) {
                return absMag[ap];
            }
            return Tools.parseDouble(gd.getNextString(), 99.999);
        }
    }

    protected void addApertureAsT1(boolean altDown) {
        for (int ap = ngot - 1; ap >= 0; ap--) {
            xPos[ap + 1] = xPos[ap];
            yPos[ap + 1] = yPos[ap];
            absMag[ap + 1] = absMag[ap];
            raPos[ap + 1] = raPos[ap];
            decPos[ap + 1] = decPos[ap];
            isRefStar[ap + 1] = isRefStar[ap];
            isAlignStar[ap + 1] = isAlignStar[ap];
            centroidStar[ap + 1] = centroidStar[ap];
        }
        xPos[0] = xCenter;
        yPos[0] = yCenter;
        if (hasWCS) {
            double[] radec = wcs.pixels2wcs(new double[]{xCenter, yCenter});
            raPos[0] = radec[0];
            decPos[0] = radec[1];
        } else {
            raPos[0] = -1000001;
            decPos[0] = -1000001;
        }
        absMag[0] = 99.999;
        isRefStar[0] = false;
        isAlignStar[0] = true;
        centroidStar[0] = altDown ? !Prefs.get("aperture.reposition", reposition) : Prefs.get("aperture.reposition", reposition);
        if (!placeApertures(0, 0, ENABLECENTROID, CLEARROIS)) return;
        if (hasWCS && centroidStar[0]) {
            double[] radec = wcs.pixels2wcs(new double[]{xCenter, yCenter});
            raPos[0] = radec[0];
            decPos[0] = radec[1];
        }
        if (ngot > 0) placeApertures(1, ngot, DISABLECENTROID, KEEPROIS);

        ngot++;

        updateApMags();
    }

    protected void updateApMags() {
        updateApMags(0, ngot - 1);
    }

    protected void updateApMags(int ap) {
        updateApMags(ap, ap);
    }

    protected void updateApMags(int firstAp, int lastAp) {
        for (int ap = firstAp; ap <= lastAp; ap++)  //store new aperture magnitudes
        {
            if (isRefStar[ap] && absMag[ap] < 99.0) {
                ApertureRoi aRoi = ocanvas.findApertureRoiByNumber(ap);
                if (aRoi != null) {
                    aRoi.setAMag(absMag[ap]);
                }
            }
        }

        double totRefMag = 0.0;             //recalculate total reference star magnitude
        double totRefCnts = 0.0;
        int numRefMags = 0;
        for (int ap = 0; ap < ngot; ap++) {
            if (isRefStar[ap] && absMag[ap] < 99.0) {
                ApertureRoi aRoi = ocanvas.findApertureRoiByNumber(ap);
                if (aRoi != null && !Double.isNaN(aRoi.getIntCnts())) {
                    numRefMags++;
                    totRefMag += Math.pow(2.512, -absMag[ap]);
                    totRefCnts += aRoi.getIntCnts();
                }
            }
        }
        if (numRefMags > 0)                    //recalculate target star magnitude(s)
        {
            totRefMag = -Math.log(totRefMag) / Math.log(2.512);
        }
        for (int ap = 0; ap < ngot; ap++) {
            if (!isRefStar[ap]) {
                ApertureRoi aRoi = ocanvas.findApertureRoiByNumber(ap);
                if (aRoi != null) {
                    if (numRefMags > 0 && !Double.isNaN(aRoi.getIntCnts())) {
                        aRoi.setAMag(totRefMag - 2.5 * Math.log10(aRoi.getIntCnts() / totRefCnts));
                    } else {
                        aRoi.setAMag(99.999);
                    }
                }
            }
        }
        ac.repaint();
    }

    protected boolean removeAperture() {
        int ap;
        for (ap = 0; ap < ngot; ap++) {
            if ((xCenter - xPos[ap]) * (xCenter - xPos[ap]) + (yCenter - yPos[ap]) * (yCenter - yPos[ap]) <= radius * radius) {
                if (ap == 0 && slice != initialFirstSlice) {
                    IJ.beep();
                    if (!IJ.showMessageWithCancel("Confirm Delete", "Press OK to delete aperture or Cancel to keep.")) {
                        apertureClicked = false;
                        return false;
                    }
                }
                ocanvas.removeRoi((int) xCenter, (int) yCenter);
                ngot--;
                for (int i = ap; i < ngot; i++) {
                    xPos[i] = xPos[i + 1];
                    yPos[i] = yPos[i + 1];
                    absMag[i] = absMag[i + 1];
                    raPos[i] = raPos[i + 1];
                    decPos[i] = decPos[i + 1];
                    isAlignStar[i] = isAlignStar[i + 1];
                    centroidStar[i] = centroidStar[i + 1];
                    isRefStar[i] = isRefStar[i + 1];
                }
                break;
            }
        }
        placeApertures(0, ngot - 1, DISABLECENTROID, CLEARROIS);

        updateApMags();
        return true;
    }

    protected void renameApertureToT1(boolean altDown) {
        for (int ap = 0; ap < ngot; ap++) {
            if ((xCenter - xPos[ap]) * (xCenter - xPos[ap]) + (yCenter - yPos[ap]) * (yCenter - yPos[ap]) <= radius * radius) {
                double xpos0 = xPos[ap];
                double ypos0 = yPos[ap];
                double amag0 = absMag[ap];
                double rapos0 = raPos[ap];
                double decpos0 = decPos[ap];
                boolean isalignstar0 = isAlignStar[ap];
                boolean centroidStar0 = centroidStar[ap];
                for (int i = ap; i > 0; i--) {
                    xPos[i] = xPos[i - 1];
                    yPos[i] = yPos[i - 1];
                    absMag[i] = absMag[i - 1];
                    raPos[i] = raPos[i - 1];
                    decPos[i] = decPos[i - 1];
                    isAlignStar[i] = isAlignStar[i - 1];
                    centroidStar[i] = centroidStar[i - 1];
                    isRefStar[i] = isRefStar[i - 1];
                }
                xPos[0] = xpos0;
                yPos[0] = ypos0;
                absMag[0] = amag0;
                raPos[0] = rapos0;
                decPos[0] = decpos0;
                isAlignStar[0] = isalignstar0;
                centroidStar[0] = centroidStar0;
                isRefStar[0] = false;
                break;
            }
        }
        if (altDown) {
            centroidStar[0] = !centroidStar[0];
            if (centroidStar[0]) placeApertures(0, 0, ENABLECENTROID, CLEARROIS);
        }
        placeApertures(0, ngot - 1, DISABLECENTROID, CLEARROIS);
        updateApMags();
    }

    protected void toggleApertureType(boolean altDown) {
        int ap;
        for (ap = 0; ap < ngot; ap++) {
            if ((xCenter - xPos[ap]) * (xCenter - xPos[ap]) + (yCenter - yPos[ap]) * (yCenter - yPos[ap]) <= radius * radius) {
                isRefStar[ap] = !isRefStar[ap];
                absMag[ap] = getAbsMag(ap, raPos[ap], decPos[ap]);
                if (altDown) centroidStar[ap] = !centroidStar[ap];
                break;
            }
        }
        if (altDown && centroidStar[ap]) placeApertures(ap, ap, ENABLECENTROID, CLEARROIS);
        placeApertures(0, ngot - 1, DISABLECENTROID, CLEARROIS);
        updateApMags();
    }

    protected void toggleCentroidType() {
        int ap;
        for (ap = 0; ap < ngot; ap++) {
            if ((xCenter - xPos[ap]) * (xCenter - xPos[ap]) + (yCenter - yPos[ap]) * (yCenter - yPos[ap]) <= radius * radius) {
                centroidStar[ap] = !centroidStar[ap];
                break;
            }
        }
        if (ap > 0) placeApertures(0, ap - 1, DISABLECENTROID, CLEARROIS);
        placeApertures(ap, ap, ENABLECENTROID, ap > 0 ? KEEPROIS : CLEARROIS);
        if (ap < ngot - 1) placeApertures(ap + 1, ngot - 1, DISABLECENTROID, KEEPROIS);
        updateApMags();
    }

    protected void moveAperture(double x, double y) {
        if (asw == null || ac == null || selectedApertureRoi == null) return;
        int ap = selectedApertureRoi.getApNumber();
        if (ap >= 0 && ap < ngot) {
            xPos[ap] = x;
            yPos[ap] = y;
            selectedApertureRoi.setIsCentroid(centroidStar[ap]);
            selectedApertureRoi.setLocation(x, y);
            ac.repaint();
        }
    }

    /**
     * Saves new aperture locations to preferences.
     */
    protected void saveNewApertures() {
        StringBuilder xpos = new StringBuilder();
        StringBuilder ypos = new StringBuilder();
        StringBuilder ra = new StringBuilder();
        StringBuilder dec = new StringBuilder();
        StringBuilder amag = new StringBuilder();
        StringBuilder isref = new StringBuilder();
        StringBuilder isalign = new StringBuilder();
        StringBuilder centroid = new StringBuilder();
        uptoEightPlaces.setDecimalFormatSymbols(IJU.dfs);
        for (int i = 0; i < nApertures; i++) {
            if (i == 0) {
                xpos.append((float) xPos[i]);
                ypos.append((float) yPos[i]);
                amag.append((float) absMag[i]);
                if (hasWCS) ra.append(uptoEightPlaces.format(raPos[i]));
                if (hasWCS) dec.append(uptoEightPlaces.format(decPos[i]));
                isref.append(isRefStar[i]);
                isalign.append(isAlignStar[i]);
                centroid.append(centroidStar[i]);
            } else {
                xpos.append(",").append((float) xPos[i]);
                ypos.append(",").append((float) yPos[i]);
                amag.append(",").append((float) absMag[i]);
                if (hasWCS) ra.append(",").append(uptoEightPlaces.format(raPos[i]));
                if (hasWCS) dec.append(",").append(uptoEightPlaces.format(decPos[i]));
                isref.append(",").append(isRefStar[i]);
                isalign.append(",").append(isAlignStar[i]);
                centroid.append(",").append(centroidStar[i]);
            }
        }
        if (aperturesInitialized) {
            IJ.showStatus("saving new aperture locations");
            xPosStored = new double[nApertures];
            yPosStored = new double[nApertures];
            absMagStored = new double[nApertures];
            isRefStarStored = new boolean[nApertures];
            isAlignStarStored = new boolean[nApertures];
            centroidStarStored = new boolean[nApertures];
            if (useWCS) {
                raPosStored = new double[nApertures];
                decPosStored = new double[nApertures];
            } else {
                raPosStored = null;
                decPosStored = null;
            }

            for (int i = 0; i < nApertures; i++) {
                xPosStored[i] = xPos[i];
                yPosStored[i] = yPos[i];
                absMagStored[i] = absMag[i];
                if (useWCS) {
                    raPosStored[i] = raPos[i];
                    decPosStored[i] = decPos[i];
                }
                isRefStarStored[i] = isRefStar[i];
                isAlignStarStored[i] = isAlignStar[i];
                centroidStarStored[i] = centroidStar[i];
            }
            nAperturesStored = nApertures;

            Prefs.set(MultiAperture_.PREFS_XAPERTURES, xpos.toString());
            Prefs.set(MultiAperture_.PREFS_YAPERTURES, ypos.toString());
            Prefs.set(MultiAperture_.PREFS_RAAPERTURES, ra.toString());
            Prefs.set(MultiAperture_.PREFS_DECAPERTURES, dec.toString());
            Prefs.set(MultiAperture_.PREFS_ABSMAGAPERTURES, amag.toString());
            Prefs.set(MultiAperture_.PREFS_ISREFSTAR, isref.toString());
            Prefs.set(MultiAperture_.PREFS_ISALIGNSTAR, isalign.toString());
            Prefs.set(MultiAperture_.PREFS_CENTROIDSTAR, centroid.toString());
        }
    }

    public void mouseMoved(MouseEvent e) {
        currentScreenX = e.getX();
        currentScreenY = e.getY();
        if (asw == null || ac == null) return;
        ac.setMousePosition(currentScreenX, currentScreenY);
        currentX = ac.offScreenXD(currentScreenX);
        currentY = ac.offScreenYD(currentScreenY);
    }

    public void mouseDragged(MouseEvent e) {
        currentScreenX = e.getX();
        currentScreenY = e.getY();
        if (asw == null || ac == null) return;
        ac.setMousePosition(currentScreenX, currentScreenY);
        currentX = ac.offScreenXD(currentScreenX);
        currentY = ac.offScreenYD(currentScreenY);
        if (aperturesInitialized || selectedApertureRoi == null) return;
        boolean dragging = Math.abs(currentScreenX - startDragScreenX) + Math.abs(currentScreenY - startDragScreenY) >= 2.0;
        if (dragging && (e.getModifiers() & MouseEvent.BUTTON1_MASK) != 0 && !e.isShiftDown() && !e.isControlDown()) {
            moveAperture(currentX, currentY);
        }
    }

    public void mousePressed(MouseEvent e) {
        if (!autoMode && asw != null && ac != null) {
            startDragScreenX = e.getX();
            startDragScreenY = e.getY();
            ac.setMousePosition(startDragScreenX, startDragScreenY);
            startDragX = ac.offScreenXD(startDragScreenX);
            startDragY = ac.offScreenYD(startDragScreenY);
            selectedApertureRoi = ocanvas.findApertureRoi(startDragX, startDragY, 0);
            asw.setMovingAperture(selectedApertureRoi != null && !aperturesInitialized);
        }
    }

    public void mouseClicked(MouseEvent e) {}

    public void mouseExited(MouseEvent e) {
        IJ.showStatus(infoMessage);
    }

    public void mouseEntered(MouseEvent e) {}

    /**
     * Handle the key typed event from the image canvas.
     */
    public void keyTyped(KeyEvent e) {

    }


    //
    // MultiAperture_ METHODS
    //

    /**
     * Handle the key-pressed event from the image canvas.
     */
    public void keyPressed(KeyEvent e) {
        int keyCode = e.getKeyCode();
        if (IJ.escapePressed()) {
            IJ.beep();
            Prefs.set(MultiAperture_.PREFS_CANCELED, "true");
            shutDown();
        } else if (keyCode == KeyEvent.VK_ENTER) {
            enterPressed = true;
            processSingleClick(dummyClick);
        }
    }

    /**
     * Handle the key-released event from the image canvas.
     */
    public void keyReleased(KeyEvent e) {

    }

    /**
     * Define the apertures and decide on the sub-stack if appropriate.
     */
    protected boolean setUpApertures() {
        // CHECK SLICES
        firstSlice = imp.getCurrentSlice();
        lastSlice = stackSize;
        if (singleStep) { lastSlice = firstSlice; }
        if (!autoMode) {
            GenericSwingDialog gd = dialog();

            gd.showDialog();
            xLocation = gd.getX();
            yLocation = gd.getY();
            Prefs.set(MultiAperture_.PREFS_XLOCATION, xLocation);
            Prefs.set(MultiAperture_.PREFS_YLOCATION, yLocation);
            if (gd.wasCanceled()) {
                cancelled = true;
                return false;
            }

            // GET UPDATED STANDARD PARAMETERS FROM REQUIRED DIALOG FIELDS:
            //	nApertures,firstSlice,lastSlice,previous,singleStep,oneTable,wideTable

            // NOTE: ONLY THE GENERIC MultiAperture_ FIELDS BELONG HERE !!!!!!!!!!!!!

//                nAperturesMax = (int)gd.getNextNumber();
            nApertures = nAperturesMax;
            if (gd.invalidNumber() || nApertures <= 0) {
                IJ.beep();
                IJ.error("Invalid number of apertures: " + nApertures);
                return false;
            }
//                Prefs.set (MultiAperture_.PREFS_NAPERTURESMAX, nAperturesMax);
            if (stackSize > 1) {
                //firstSlice = (int) gd.getNextNumber();
                if (gd.invalidNumber() || firstSlice < 1) { firstSlice = 1; }
                //lastSlice = (int) gd.getNextNumber();
                if (gd.invalidNumber() || lastSlice > stackSize) { lastSlice = stackSize; }
                if (firstSlice != lastSlice) {
                    if (firstSlice > lastSlice) {
                        int i = firstSlice;
                        firstSlice = lastSlice;
                        lastSlice = i;
                    }
                    doStack = true;
                }
            } else {
                firstSlice = 1;
                lastSlice = 1;
            }
            initialFirstSlice = firstSlice;
            initialLastSlice = lastSlice;
            //radius = gd.getNextNumber();
            if (gd.invalidNumber() || radius <= 0) {
                IJ.beep();
                IJ.error("Invalid aperture radius: " + radius);
                return false;
            }
            if (oldradius != radius) { changeAperture(); }
            //rBack1 = gd.getNextNumber();
            if (gd.invalidNumber() || rBack1 < radius) {
                IJ.beep();
                IJ.error("Invalid background inner radius: " + rBack1);
                return false;
            }
            if (oldrBack1 != rBack1) { changeAperture(); }
            //rBack2 = gd.getNextNumber();
            if (gd.invalidNumber() || rBack2 < rBack1) {
                IJ.beep();
                IJ.error("Invalid background outer radius: " + rBack2);
                return false;
            }
            if (oldrBack2 != rBack2) { changeAperture(); }
            if (singleStep) { lastSlice = firstSlice; }
//                oneTable = !gd.getNextBoolean();
//                wideTable = gd.getNextBoolean();

            Prefs.set(MultiAperture_.PREFS_PREVIOUS, previous);
            Prefs.set(MultiAperture_.PREFS_USEWCS, useWCS);
            Prefs.set(MultiAperture_.PREFS_SINGLESTEP, singleStep);
            Prefs.set(MultiAperture_.PREFS_ALLOWSINGLESTEPAPCHANGES, allowSingleStepApChanges);

//                Prefs.set (MultiAperture_.PREFS_WIDETABLE, wideTable);

            // GET NON-STANDARD PARAMETERS AND CLEAN UP
            return finishFancyDialog(gd);

        } else {  //handle automode
            firstSlice = 1;
            lastSlice = 1;
            doStack = false;
            if (stackSize > 1) {
                lastSlice = stackSize;
                doStack = true;
            }
            initialFirstSlice = firstSlice;
            initialLastSlice = lastSlice;
        }
        return true;
    }

    protected void changeAperture() {
        apertureChanged = true;
        Prefs.set("setaperture.aperturechanged", apertureChanged);
        Prefs.set(Aperture_.AP_PREFS_RADIUS, radius);
        Prefs.set(Aperture_.AP_PREFS_RBACK1, rBack1);
        Prefs.set(Aperture_.AP_PREFS_RBACK2, rBack2);
        Prefs.set(MultiAperture_.PREFS_GETMAGS, getMags);
        Prefs.set(MultiAperture_.PREFS_APFWHMFACTOR, apFWHMFactor);
        Prefs.set(MultiAperture_.PREFS_AUTOMODEFLUXCUTOFF, autoModeFluxCutOff);
        Prefs.set(MultiAperture_.PREFS_USEVARSIZEAP, useVarSizeAp);
    }

    protected void startProcessStack()     //start new task to run stack processing so that table will update for each image processed
    {                                   //the mouse handler apparently blocks window updates to improve performance
        try {
            stackTask = new TimerTask() {
                public void run() {
                    processStack();
                    stackTask = null;
                    stackTaskTimer = null;
                }
            };
            stackTaskTimer = new java.util.Timer();
            stackTaskTimer.schedule(stackTask, 0);
        } catch (Exception e) {
            IJ.showMessage("Error starting process stack task : " + e.getMessage());
        }
    }

    /**
     * Perform photometry on each image of selected sub-stack.
     */
    synchronized protected void processStack() {
        verbose = false;
//		vx = 0.0;
//		vy = 0.0;
        canvas = imp.getCanvas();
        ocanvas = null;
        long timeStart = System.currentTimeMillis();
//        IJ.log("firstSlice="+firstSlice+"   lastSlice="+lastSlice);
        for (int i = firstSlice; i <= lastSlice; i++) {
            slice = i;
            imp.setSliceWithoutUpdate(i); //fixes scroll sync issue
            waitForEventQueue(); // Fixes scrollbar not updating on mac
            if (starOverlay || skyOverlay || valueOverlay || nameOverlay) {
                ocanvas = OverlayCanvas.getOverlayCanvas(imp);
                canvas = ocanvas;
                ocanvas.clearRois();
            }
            if (imp.getWindow() instanceof astroj.AstroStackWindow) {
                asw = (AstroStackWindow) imp.getWindow();
                ac = (AstroCanvas) imp.getCanvas();

                // This fixes the counter subtitle of the stack window not updating as the images progress
                asw.update(asw.getGraphics());

                // This fixes histogram not updating
                asw.updatePanelValues(false);
                asw.updateWCS();
                asw.updateCalibration();

                asw.setAstroProcessor(false);

                // Fixes apertures not properly being drawn/cleared when autoNupEleft is disabled
                //KC: but I don't understand why
                asw.repaintAstroCanvas();

                //waitForEventQueue();

                hasWCS = asw.hasWCS();
                if (hasWCS) wcs = asw.getWCS();
                asw.setDisableShiftClick(true);
            } else {
                ac = null;
                asw = null;
                hasWCS = false;
                wcs = null;
            }
            ip = imp.getProcessor();

            processImage();
            if (cancelled || IJ.escapePressed()) {
                IJ.beep();
                Prefs.set(MultiAperture_.PREFS_CANCELED, "true");
                shutDown();
                return;
            }
        }
        if (processingStack) {
            IJ.beep();
            shutDown();
            //AIJLogger.log("Multiaperture photometry took " + (System.currentTimeMillis() - timeStart) / 1000D + " seconds");
            IJ.showStatus("Multiaperture photometry took " + (System.currentTimeMillis() - timeStart) / 1000D + " seconds");
        }
    }

    /**
     * Perform photometry on each aperture of current image.
     */
    protected void processImage() {
        double dx = 0.0;        // CHANGE
        double dy = 0.0;

        double srcMean = 0.0;        // MEAN SOURCE BRIGHTNESSES AND BACKGROUNDS
        double bck = 0.0;
        int nFWHM = 0;
        int nRD = 0;
        double radiusRD = 0.0;
        boolean centroidFailed = false;

        checkAndLockTable();
        processingImage = true;
        ratio = new double[nApertures];
        ratioError = new double[nApertures];
        ratioSNR = new double[nApertures];
//        isRefStar = new boolean [nApertures];
        src = new double[nApertures];
        if (targetAbsMag == null || targetAbsMag.length != nApertures) {
            targetAbsMag = new double[nApertures];
            for (int ap = 0; ap < nApertures; ap++) {
                targetAbsMag[ap] = 99.999;
            }
        }
        srcVar = new double[nApertures];
        tot = new double[nApertures];
        totVar = new double[nApertures];
        xWidthFixed = new double[nApertures];
        yWidthFixed = new double[nApertures];
        widthFixed = new double[nApertures];
        angleFixed = new double[nApertures];
        roundFixed = new double[nApertures];

        if (!isInstanceOfStackAlign) {
            for (int r = 0; r < nApertures; r++)  //check for target star <--> ref star changes in table (i.e. changes from multi-plot)
            {
                if (table.getColumnIndex("Source-Sky_C" + (r + 1)) != MeasurementTable.COLUMN_NOT_FOUND &&
                        table.getColumnIndex("Source-Sky_T" + (r + 1)) == MeasurementTable.COLUMN_NOT_FOUND) {
                    isRefStar[r] = true;
                } else if (table.getColumnIndex("Source-Sky_T" + (r + 1)) != MeasurementTable.COLUMN_NOT_FOUND &&
                        table.getColumnIndex("Source-Sky_C" + (r + 1)) == MeasurementTable.COLUMN_NOT_FOUND) {
                    isRefStar[r] = false;
                } else {  //leave the same as initially entered
//                    isRefStar[r] = r == 0 ? false : true;
//                    IJ.beep();
//                    IJ.showMessage("Error checking aperture types in table");
                }
            }
        }

        int totCcntAP = -1;
        for (int ap = 0; ap < nApertures; ap++) {
            if (!isRefStar[ap]) {
                totCcntAP = ap;
                break;
            }
        }
        for (int ap = 0; ap < nApertures; ap++) {
            ratio[ap] = 0.0;
            ratioError[ap] = 0.0;
            ratioSNR[ap] = 0.0;
            src[ap] = 0.0;
            srcVar[ap] = 0.0;
            tot[ap] = 0.0;
            totVar[ap] = 0.0;
        }
        xFWHM = 0.0;
        yFWHM = 0.0;

        if (useVarSizeAp) {
            setVariableAperture(false);
            for (int ap = 0; ap < nApertures; ap++) {
                // GET POSITION ESTIMATE

                if (!isRefStar[ap]) {
                    setApertureColor(Color.green);
                    setApertureName("T" + (ap + 1));
                    setAbsMag(targetAbsMag[ap]);
                } else {
                    setApertureColor(Color.red);
                    setApertureName("C" + (ap + 1));
                    setAbsMag(absMag[ap]);
                }
                if ((useMA || useAlign) && useWCS) {
                    if (hasWCS && raPos[ap] > -1000000 && decPos[ap] > -1000000) {
                        double[] xy = wcs.wcs2pixels(new double[]{raPos[ap], decPos[ap]});
                        xPos[ap] = xy[0];
                        yPos[ap] = xy[1];
                        xCenter = xy[0];
                        yCenter = xy[1];
                    }
//                    else if (!hasWCS && autoMode)
//                        {
//                        if (table != null) table.setLock(false);
//                        return;
//                        }
                    else if (raPos[ap] <= -1000000 && decPos[ap] <= -1000000) {
                        IJ.beep();
                        IJ.showMessage("Error", "WCS mode requested but no valid WCS coordinates stored. ABORTING.");
                        Prefs.set(MultiAperture_.PREFS_CANCELED, "true");
                        cancelled = true;
                        shutDown();
                        if (table != null) table.setLock(false);
                        return;
                    } else if (haltOnError) {
                        IJ.beep();
                        IJ.showMessage("Error", "WCS mode requested but no valid WCS FITS Headers. ABORTING.");
                        Prefs.set(MultiAperture_.PREFS_CANCELED, "true");
                        cancelled = true;
                        shutDown();
                        if (table != null) table.setLock(false);
                        return;
                    } else {
                        //IJ.log("WARNING: WCS mode requested but no valid WCS FITS Headers found in image "+ IJU.getSliceFilename(imp, slice)+". Using last aperture positions for slice "+slice+".");
                        xCenter = xPos[ap];
                        yCenter = yPos[ap];
                    }
                } else {
                    xCenter = xPos[ap];
                    yCenter = yPos[ap];
                }

                // MEASURE NEW POSITION
                boolean holdReposition = Prefs.get("aperture.reposition", reposition);
                Prefs.set("aperture.reposition", centroidStar[ap]);
                centroidFailed = false;
                if (!adjustAperture(false)) {
                    if (haltOnError || this instanceof Stack_Aligner) {
                        Prefs.set("aperture.reposition", holdReposition);
                        centerROI();
                        setVariableAperture(false);
                        IJ.beep();
                        IJ.showMessage("No signal for centroid in aperture " + apertureName + " of image " +
                                IJU.getSliceFilename(imp, slice) +
                                ((this instanceof Stack_Aligner) ? ". Stack Aligner aborted." : ". Multi-Aperture aborted."));
                        shutDown();
                        if (table != null) table.setLock(false);
                        return;
                    } else {
                        centroidFailed = true;
                    }
                }
                Prefs.set("aperture.reposition", holdReposition);
                xOld[ap] = xPos[ap];
                yOld[ap] = yPos[ap];

                xPos[ap] = xCenter;        // STORE POSITION IN CASE IT DRIFTS WITHIN A STACK
                yPos[ap] = yCenter;

                if (ap == 0 && !centroidFailed) {
                    double xDel = xPos[0] - xOld[0];
                    double yDel = yPos[0] - yOld[0];
                    for (int app = 1; app < nApertures; app++) {
                        xOld[app] = xPos[app];
                        yOld[app] = yPos[app];

                        xPos[app] += xDel;
                        yPos[app] += yDel;
                    }
                }
                if (centroidStar[ap] && !centroidFailed) {
                    nFWHM++;
                    xFWHM += xWidth;
                    yFWHM += yWidth;
                    if (apFWHMFactor == 0.0) {
                        if (radialDistribution(xCenter, yCenter, radius, back)) {
                            nRD++;
                            radiusRD += rRD;
                        }
                    }

                }
                xWidthFixed[ap] = xWidth;
                yWidthFixed[ap] = yWidth;
                widthFixed[ap] = apFWHMFactor != 0.0 ? 0.5 * (xWidth + yWidth) : fwhmRD;
                angleFixed[ap] = angle;
                roundFixed[ap] = round;
            }
            if (nFWHM == 0) {
                for (int ap = 0; ap < nApertures; ap++) {
                    xFWHM += xWidthFixed[ap] != 0 ? xWidthFixed[ap] : radius;
                    yFWHM += yWidthFixed[ap] != 0 ? yWidthFixed[ap] : radius;
                }
                nFWHM = nApertures;
            }
            if (nRD == 0) {
                radiusRD = radius;
                nRD = 1;
            }
            if (apFWHMFactor != 0.0) {
                setVariableAperture(true, Math.max(xFWHM / nFWHM, yFWHM / nFWHM) * apFWHMFactor, apFWHMFactor, autoModeFluxCutOff);
            } else { setVariableAperture(true, radiusRD / nRD, 0.0, autoModeFluxCutOff); }
        }

        // This is needed when running from DP for some reason, other rois are not removed
        OverlayCanvas.getOverlayCanvas(imp).removeApertureRois();

        nFWHM = 0;
        fwhmMean = 0.0;
        for (int ap = 0; ap < nApertures; ap++) {
            if (!isRefStar[ap]) {
                setApertureColor(Color.green);
                setApertureName("T" + (ap + 1));
                setAbsMag(targetAbsMag[ap]);
            } else {
                setApertureColor(Color.red);
                setApertureName("C" + (ap + 1));
                setAbsMag(absMag[ap]);
            }

            if ((useMA || useAlign) && useWCS) {
                if (hasWCS && raPos[ap] > -1000000 && decPos[ap] > -1000000) {
                    double[] xy = wcs.wcs2pixels(new double[]{raPos[ap], decPos[ap]});
                    xPos[ap] = xy[0];
                    yPos[ap] = xy[1];
                    xCenter = xy[0];
                    yCenter = xy[1];
                }
//                else if (!hasWCS && autoMode)
//                    {
//                    if (table != null) table.setLock(false);
//                    return;
//                    }
                else if (raPos[ap] <= -1000000 && decPos[ap] <= -1000000) {
                    IJ.beep();
                    IJ.showMessage("Error", "WCS mode requested but no valid WCS coordinates stored. ABORTING.");
                    Prefs.set(MultiAperture_.PREFS_CANCELED, "true");
                    cancelled = true;
                    shutDown();
                    if (table != null) table.setLock(false);
                    return;
                } else if (haltOnError) {
                    IJ.beep();
                    IJ.showMessage("Error", "WCS mode requested but no valid WCS FITS Headers. ABORTING.");
                    Prefs.set(MultiAperture_.PREFS_CANCELED, "true");
                    cancelled = true;
                    shutDown();
                    if (table != null) table.setLock(false);
                    return;
                } else {
                    //IJ.log("WARNING: WCS mode requested but no valid WCS FITS Headers found in image "+ IJU.getSliceFilename(imp, slice)+". Using last aperture positions for slice "+slice+".");
                    xCenter = xPos[ap];
                    yCenter = yPos[ap];
                }
            } else {
                xCenter = xPos[ap];
                yCenter = yPos[ap];
            }

            // MEASURE NEW POSITION AND RECENTER IF CENTROID ENABLED
            boolean holdReposition = Prefs.get("aperture.reposition", reposition);
            Prefs.set("aperture.reposition", centroidStar[ap]);
            setShowAsCentered(centroidStar[ap]);

            valueOverlay = false; // Don't show values as they will differ when measured and will be drawn over
        }

        valueOverlay = Prefs.get (AP_PREFS_VALUEOVERLAY, valueOverlay);
        for (int ap = 0; ap < nApertures; ap++) {
            if (!isRefStar[ap]) {
                setApertureColor(Color.green);
                setApertureName("T" + (ap + 1));
                setAbsMag(targetAbsMag[ap]);
            } else {
                setApertureColor(Color.red);
                setApertureName("C" + (ap + 1));
                setAbsMag(absMag[ap]);
            }

            xCenter = xPos[ap];
            yCenter = yPos[ap];

            boolean holdReposition = Prefs.get("aperture.reposition", reposition);
            Prefs.set("aperture.reposition", centroidStar[ap]);
            setShowAsCentered(centroidStar[ap]);

            if (!measureAperture()) {
                if (haltOnError || this instanceof Stack_Aligner) {
                    Prefs.set("aperture.reposition", holdReposition);
                    centerROI();
                    setVariableAperture(false);
                    IJ.beep();
                    IJ.showMessage("No signal for centroid in aperture " + apertureName + " of image " +
                            IJU.getSliceFilename(imp, slice) +
                            ((this instanceof Stack_Aligner) ? ". Stack Aligner aborted." : ". Multi-Aperture aborted."));
                    shutDown();
                    if (table != null) table.setLock(false);
                    return;
                } else {
                    IJ.log("***ERROR: No signal for centroid in aperture " + apertureName + " of image " + IJU.getSliceFilename(imp, slice) + ".");
                    IJ.log("********: Measurements are referenced to the non-centroided aperture location");
                }
            }
            Prefs.set("aperture.reposition", holdReposition);

            if (useVarSizeAp) {
                xWidth = xWidthFixed[ap];
                yWidth = yWidthFixed[ap];
                width = widthFixed[ap];
                angle = angleFixed[ap];
                round = roundFixed[ap];
            }
            if (showMeanWidth && calcRadProFWHM && !Double.isNaN(fwhm)) {
                fwhmMean += fwhm;
                nFWHM++;
            }
            processingImage = false;

            // STORE RESULTS

            if (ap == 0) {
                suffix = isRefStar[ap] ? "_C1" : "_T1";
                storeResults();
            } else { storeAdditionalResults(ap); }

            // FOLLOW MOTION FROM FRAME TO FRAME

            dx += xCenter - xPos[ap];
            dy += yCenter - yPos[ap];

            xOld[ap] = xPos[ap];
            yOld[ap] = yPos[ap];

            xPos[ap] = xCenter;        // STORE POSITION IN CASE IT DRIFTS WITHIN A STACK
            yPos[ap] = yCenter;

            srcMean += source;
            bck += back;

            ratio[ap] = source;
            src[ap] = source;
            srcVar[ap] = serror * serror;
            if (isRefStar[ap]) {
                for (int i = 0; i < nApertures; i++) {
                    if (i != ap) {
                        tot[i] += source;
                        totVar[i] += srcVar[ap];
                    }
                }
            }
            if (ap == 0) {
                double xDel = xPos[0] - xOld[0];
                double yDel = yPos[0] - yOld[0];
                for (int app = 1; app < nApertures; app++) {
                    xOld[app] = xPos[app];
                    yOld[app] = yPos[app];

                    xPos[app] += xDel;
                    yPos[app] += yDel;
                }
            }

            // FOR DAUGHTER CLASSES....

            noteOtherApertureProperty(ap);
        }

        // Increase chance of aperture actually rendering on mac
        if (IJ.isMacOSX()) {
            ocanvas.update(ocanvas.getGraphics());
            canvas.update(canvas.getGraphics());
            //ocanvas.repaint();//renders extra apertures?!
        } else {
            // This is broken on mac.
            // Causes apertures to render in wrong location, if they render at all
            ocanvas.drawOverlayCanvas(ocanvas.getGraphics());
        }

        canvas.repaintOverlay();
        canvas.repaint();

        if (!isInstanceOfStackAlign && showMeanWidth && calcRadProFWHM) {
            if (nFWHM > 0) {
                fwhmMean /= nFWHM;
            } else fwhmMean = 0.0;
            table.addValue("FWHM_Mean", fwhmMean, 6);
        }

        setVariableAperture(false);

        // COMPUTE APPARENT MAGNITUDE IF APPLICABLE

        hasAbsMag = false;
        numAbsRefs = 0;
        totAbsMag = 0.0;
        for (int ap = 0; ap < nApertures; ap++) {
            if (isRefStar[ap] && absMag[ap] < 99.0) {
                hasAbsMag = true;
                numAbsRefs++;
                totAbsMag += Math.pow(2.512, -absMag[ap]);
            }
        }
        if (numAbsRefs > 0) totAbsMag = -Math.log(totAbsMag) / Math.log(2.512);


        if (!isInstanceOfStackAlign && hasAbsMag && showPhotometry) {
            double totAbsVar = 0.0;
            double totAbs = 0.0;
            for (int ap = 0; ap < nApertures; ap++) {
                if (isRefStar[ap] && absMag[ap] < 99.0) {
                    totAbs += src[ap];
                    totAbsVar += srcVar[ap];
                }
            }

            for (int ap = 0; ap < nApertures; ap++) {
                if (!isRefStar[ap]) {
                    double absRatio = src[ap] / totAbs;
                    targetAbsMag[ap] = totAbsMag - 2.5 * Math.log10(absRatio);
                    table.addValue(AP_SOURCE_AMAG + "_T" + (ap + 1), targetAbsMag[ap], 6);
                    if (showErrors) {
                        double srcAbsErr = absRatio * Math.sqrt(srcVar[ap] / (src[ap] * src[ap]) + totAbsVar / (totAbs * totAbs));
                        table.addValue(AP_SOURCE_AMAG_ERR + "_T" + (ap + 1), 2.5 * Math.log10(1.0 + srcAbsErr / absRatio), 6);
                    }
                    ApertureRoi aRoi = ocanvas.findApertureRoiByNumber(ap);
                    if (aRoi != null) aRoi.setAMag(targetAbsMag[ap]);
                } else {
                    table.addValue(AP_SOURCE_AMAG + "_C" + (ap + 1), absMag[ap], 6);
                    if (showErrors) {
                        table.addValue(AP_SOURCE_AMAG_ERR + "_C" + (ap + 1), 2.5 * Math.log10(1.0 + Math.sqrt(srcVar[ap]) / src[ap]), 6);
                    }
                }
            }
        }

        // COMPUTE APERTURE RATIO AND ERRORS AND UPDATE TABLE

        if (!isInstanceOfStackAlign && nApertures > 1) {
            if (showRatio) {
                for (int ap = 0; ap < nApertures; ap++) {
                    if (tot[ap] == 0) { ratio[ap] = 0; } else { ratio[ap] /= tot[ap]; }
                    table.addValue("rel_flux_" + (isRefStar[ap] ? "C" : "T") + (ap + 1), ratio[ap], 6);
                    if (showRatioError) {
                        if (src[ap] == 0 || tot[ap] == 0) {
                            table.addValue("rel_flux_err_" + (isRefStar[ap] ? "C" : "T") + (ap + 1), 0.0, 6);
                        } else {
                            table.addValue("rel_flux_err_" + (isRefStar[ap] ? "C" : "T") + (ap + 1), ratio[ap] * Math.sqrt(srcVar[ap] / (src[ap] * src[ap]) + totVar[ap] / (tot[ap] * tot[ap])), 6);
                        }
                    }
                    if (showRatioSNR) {
                        if (src[ap] == 0 || tot[ap] == 0) {
                            table.addValue("rel_flux_SNR_" + (isRefStar[ap] ? "C" : "T") + (ap + 1), 0.0, 6);
                        } else {
                            table.addValue("rel_flux_SNR_" + (isRefStar[ap] ? "C" : "T") + (ap + 1), 1 / Math.sqrt(srcVar[ap] / (src[ap] * src[ap]) + totVar[ap] / (tot[ap] * tot[ap])), 6);
                        }
                    }
                }
            }
            if (showCompTot) {
                table.addValue(TOTAL, totCcntAP < 0 ? 0.0 : tot[totCcntAP], 6);
            }
            if (showCompTot && showErrors) {
                table.addValue(TOTAL_ERROR, totCcntAP < 0 ? 0.0 : Math.sqrt(totVar[totCcntAP]), 6);
            }
        }

        // CALCULATE MEAN SHIFT, BRIGHTNESS, AND BACKGROUND

//		xCenter = dx/nApertures;
//		yCenter = dy/nApertures;
//		source = srcMean/nApertures;
//		back = bck/nApertures;

        // CALCULATE AND NOTE THE PIXEL VELOCITIES IN PIXELS/SLICE

//		if (follow)
//			{
//			vxOld = vx;
//			vyOld = vy;
//			vx = dx/nApertures;
//			vy = dy/nApertures;
//			table.addValue (VX, vx, 6);
//			table.addValue (VY, vy, 6);
//			}


        // UPDATE TABLE
        if (table != null && !isInstanceOfStackAlign && ((updatePlot && !Data_Processor.active) || (Data_Processor.active))) {
            table.show();
            tablePanel = MeasurementTable.getTextPanel(tableName);
            if (tablePanel != null) {
                int lastLine = tablePanel.getLineCount() - 1;
                tablePanel.setSelection(lastLine, lastLine);
                tablePanel.resetSelection();
            }

            table.setLock(false);

            if ((updatePlot && !Data_Processor.active) || (Data_Processor.active && Data_Processor.runMultiPlot)) {
                if (MultiPlot_.mainFrame != null) {
                    if (MultiPlot_.getTable() != null && MultiPlot_.getTable().equals(table)) {
//                        IJ.log("update plot");
//                        while (MultiPlot_.updatePlotRunning)
//                                {
//                                IJ.log("waiting");
//                                IJ.wait(100);
//                                }
                        MultiPlot_.updatePlot(MultiPlot_.updateAllFits(), slice == initialLastSlice);
//                        IJ.log("update plot complete");
                    } else {
//                        IJ.log("setTable");
                        MultiPlot_.setTable(table, true);
//                        IJ.log("setTable complete");
                    }
                } else {
                    IJ.runPlugIn("Astronomy.MultiPlot_", tableName);
                    if (MultiPlot_.mainFrame != null && MultiPlot_.getTable() != null) {
//                        IJ.log("setTable first time");
                        MultiPlot_.setTable(table, true);
//                        IJ.log("setTable first time complete");
                    }
                }
            }
        } else {
            if (table != null) table.setLock(false);
        }
    }

    /**
     * This method blocks the current thread to wait for the EventQueue to finish
     */
    // This is a hack
    private void waitForEventQueue() {
        try {
            // We don't actually care about what happens, we just want the blocking
            EventQueue.invokeAndWait(() -> {});
        } catch (InterruptedException | InvocationTargetException e) {
            e.printStackTrace();
        }
    }

    boolean radialDistribution(double X0, double Y0, double rFixed, double background) {
        int iterations = 0;
        boolean foundR1 = false;
        int nBins;
        double R;
        double mR = rFixed;
        float z, peak;
        double meanPeak;
        double[] radii;
        double[] means;
        int[] count;
        fwhmRD = 0.0;
        rRD = rFixed;

        while (!foundR1 && iterations < 10) {
            nBins = (int) mR;
            foundFWHM = false;
            radii = new double[nBins];
            means = new double[nBins];
            count = new int[nBins];
            meanPeak = Double.MIN_VALUE;
            peak = Float.MIN_VALUE;
            int xmin = (int) (X0 - mR);
            int xmax = (int) (X0 + mR);
            int ymin = (int) (Y0 - mR);
            int ymax = (int) (Y0 + mR);

            // ACCUMULATE ABOUT CENTROID POSITION

            for (int j = ymin; j < ymax; j++) {
                double dy = (double) j + Centroid.PIXELCENTER - Y0;
                for (int i = xmin; i < xmax; i++) {
                    double dx = (double) i + Centroid.PIXELCENTER - X0;
                    R = Math.sqrt(dx * dx + dy * dy);
                    int bin = (int) R; //Math.round((float)R);  //
                    if (bin >= nBins) continue; //bin = nBins-1;
                    z = ip.getPixelValue(i, j);
                    radii[bin] += R;
                    means[bin] += z;
                    count[bin]++;
                    if (z > peak) peak = z;
                }
            }

            for (int bin = 0; bin < nBins; bin++) {
                if (count[bin] > 0 && (means[bin] / count[bin]) > meanPeak) meanPeak = means[bin] / count[bin];
            }
            meanPeak -= background;

            // NORMALIZE

            peak -= background;
            for (int bin = 0; bin < nBins; bin++) {
                if (count[bin] > 0) {
                    means[bin] = ((means[bin] / count[bin]) - background) / meanPeak;
                    radii[bin] /= count[bin];
                } else {
                    //                IJ.log("No samples at radius "+bin);
                    means[bin] = Double.NaN;
                    radii[bin] = Double.NaN;
                }
            }


            // FIND FWHM

            for (int bin = 1; bin < nBins; bin++) {
                if (!foundFWHM && means[bin - 1] > 0.5 && means[bin] <= 0.5) {
                    if (bin + 1 < nBins && means[bin + 1] > means[bin] && bin + 2 < nBins && means[bin + 2] > means[bin]) {
                        continue;
                    }
                    double m = (means[bin] - means[bin - 1]) / (radii[bin] - radii[bin - 1]);
                    fwhmRD = 2.0 * (radii[bin - 1] + (0.5 - means[bin - 1]) / m);
                    foundFWHM = true;
                } else if (foundFWHM && bin < nBins - 5) {
                    if (means[bin] < autoModeFluxCutOff) {
                        rRD = radii[bin];
                        foundR1 = true;
                        break;
                    }
                }
            }
            if (!foundR1) {
                mR += 10;
            }
            iterations++;
        }
        return foundR1;
    }

    /**
     * Stores results for additional apertures.
     */
    void storeAdditionalResults(int ap) {
        if (isInstanceOfStackAlign || ap <= 0) return;

        String header = (isRefStar[ap] ? "_C" : "_T") + (ap + 1);
        if (showPosition) {
            table.addValue(AP_XCENTER + header, xCenter, 6);
            table.addValue(AP_YCENTER + header, yCenter, 6);
        }
        if (showPositionFITS) {
            table.addValue(AP_XCENTER_FITS + header, xCenter + Centroid.PIXELCENTER, 6);
            table.addValue(AP_YCENTER_FITS + header, (double) imp.getHeight() - yCenter + Centroid.PIXELCENTER, 6);
        }
        if (showRADEC && wcs != null && wcs.hasRaDec() && raDec != null) {
            table.addValue(AP_RA + header, raDec[0] / 15.0, 6);
            table.addValue(AP_DEC + header, raDec[1], 6);
        }
        if (showPhotometry) table.addValue(AP_SOURCE + header, photom.sourceBrightness(), 6);
        if (showNAperPixels) table.addValue(AP_NAPERPIX + suffix, photom.numberOfSourceAperturePixels(), 6);
        if (showErrors) table.addValue(AP_SOURCE_ERROR + header, photom.sourceError(), 6);
        if (showSNR) table.addValue(AP_SOURCE_SNR + header, photom.sourceBrightness() / photom.sourceError(), 6);
        if (showBack) table.addValue(AP_BACK + header, photom.backgroundBrightness());
        if (showNBackPixels) table.addValue(AP_NBACKPIX + suffix, photom.numberOfBackgroundAperturePixels(), 6);
        if (showPeak) table.addValue(AP_PEAK + header, photom.peakBrightness(), 6);
        if (showMean) table.addValue(AP_MEAN + header, photom.meanBrightness(), 6);
        if (showSaturationWarning && photom.peakBrightness() > saturationWarningLevel &&
                photom.peakBrightness() > table.getValue(AP_WARNING, table.getCounter() - 1)) {
            table.setValue(AP_WARNING, table.getCounter() - 1, photom.peakBrightness());
        }
        if (showWidths) {
            table.addValue(AP_XWIDTH + header, xWidth, 6);
            table.addValue(AP_YWIDTH + header, yWidth, 6);
        }
        if (showMeanWidth && calcRadProFWHM) { table.addValue(AP_FWHM + header, fwhm, 6); }
        if (showMeanWidth) { table.addValue(AP_MEANWIDTH + header, width, 6); }
        if (showAngle) { table.addValue(AP_ANGLE + header, angle, 6); }
        if (showRoundness) { table.addValue(AP_ROUNDNESS + header, round, 6); }
        if (showVariance) { table.addValue(AP_VARIANCE + header, variance, 6); }

//		table.show();
    }

    /**
     * Notes anything else which might be interesting about an aperture measurement.
     */
    protected void noteOtherApertureProperty(int ap) {
        if (isInstanceOfStackAlign) return;
    }

    /**
     * Set up extended table format.
     */
    @SuppressWarnings("UnusedAssignment")
    protected boolean checkResultsTable() {

        if (isInstanceOfStackAlign) return true;
        MeasurementTable plotTable = MultiPlot_.getTable();
        if (MultiPlot_.mainFrame != null && plotTable != null && MeasurementTable.shorterName(plotTable.shortTitle()).equals("Measurements")) {
            table = plotTable;
        }

        tablePanel = MeasurementTable.getTextPanel(tableName);
        if (table != null && tablePanel != null && (table.getCounter() > 0 || !updatePlot)) {
            return true;  //!autoMode && ()
        }

        table = MeasurementTable.getTable(tableName);

        if (table == null) {
            IJ.error("Unable to open measurement table.");
            return false;
        }

        checkAndLockTable();

        int i = 0;

        tablePanel = MeasurementTable.getTextPanel(tableName);

        hasAbsMag = false;
        for (int ap = 0; ap < nApertures; ap++) {
            if (isRefStar[ap] && absMag[ap] < 99.0) {
                hasAbsMag = true;
                break;
            }
        }

        if (showSliceNumber && table.getColumnIndex(AP_SLICE) == ResultsTable.COLUMN_NOT_FOUND) {
            i = table.getFreeColumn(AP_SLICE);
        }
        if (showSaturationWarning && table.getColumnIndex(AP_WARNING) == ResultsTable.COLUMN_NOT_FOUND) {
            i = table.getFreeColumn(AP_WARNING);
        }
        if (showTimes && table.getColumnIndex(AP_MJD) == ResultsTable.COLUMN_NOT_FOUND) {
            i = table.getFreeColumn(AP_MJD);
        }
        if (showTimes && table.getColumnIndex(AP_JDUTC) == ResultsTable.COLUMN_NOT_FOUND) {
            i = table.getFreeColumn(AP_JDUTC);
        }
        if (showTimes && FitsJ.isTESS(FitsJ.getHeader(imp)) && table.getColumnIndex(AP_BJDTDB) == ResultsTable.COLUMN_NOT_FOUND) {
            i = table.getFreeColumn(AP_BJDTDB);
        }
        if (showFits && fitsKeywords != null) {
            String[] sarr = fitsKeywords.split(",");
            for (int l = 0; l < sarr.length; l++) {
                if (!sarr[l].equals("") &&
                        table.getColumnIndex(sarr[l]) == ResultsTable.COLUMN_NOT_FOUND) {
                    i = table.getFreeColumn(sarr[l]);
                }
            }
        }
        if (showMeanWidth && calcRadProFWHM) {
            if (table.getColumnIndex("FWHM_Mean") == ResultsTable.COLUMN_NOT_FOUND) {
                i = table.getFreeColumn("FWHM_Mean");
            }
        }
        if (showRadii) {
            if (table.getColumnIndex(AP_RSOURCE) == ResultsTable.COLUMN_NOT_FOUND) {
                i = table.getFreeColumn(AP_RSOURCE);
            }
            if (useVarSizeAp) {
                if (apFWHMFactor > 0) {
                    if (table.getColumnIndex(AP_FWHMMULT) == ResultsTable.COLUMN_NOT_FOUND) {
                        i = table.getFreeColumn(AP_FWHMMULT);
                    }
                } else {
                    if (table.getColumnIndex(AP_RADIALCUTOFF) == ResultsTable.COLUMN_NOT_FOUND) {
                        i = table.getFreeColumn(AP_RADIALCUTOFF);
                    }
                }
                if (table.getColumnIndex(AP_BRSOURCE) == ResultsTable.COLUMN_NOT_FOUND) {
                    i = table.getFreeColumn(AP_BRSOURCE);
                }
            }
            if (table.getColumnIndex(AP_RBACK1) == ResultsTable.COLUMN_NOT_FOUND) {
                i = table.getFreeColumn(AP_RBACK1);
            }
            if (table.getColumnIndex(AP_RBACK2) == ResultsTable.COLUMN_NOT_FOUND) {
                i = table.getFreeColumn(AP_RBACK2);
            }
        }


        if (nApertures > 1) {
            if (showRatio) {
                for (int ap = 0; ap < nApertures; ap++) {
                    if (table.getColumnIndex("rel_flux_" + (isRefStar[ap] ? "C" : "T") + (ap + 1)) == MeasurementTable.COLUMN_NOT_FOUND) {
                        i = table.getFreeColumn("rel_flux_" + (isRefStar[ap] ? "C" : "T") + (ap + 1));
                    }
                }
                if (showRatioError) {
                    for (int ap = 0; ap < nApertures; ap++) {
                        if (table.getColumnIndex("rel_flux_err_" + (isRefStar[ap] ? "C" : "T") + (ap + 1)) == MeasurementTable.COLUMN_NOT_FOUND) {
                            i = table.getFreeColumn("rel_flux_err_" + (isRefStar[ap] ? "C" : "T") + (ap + 1));
                        }
                    }
                }
                if (showRatioSNR) {
                    for (int ap = 0; ap < nApertures; ap++) {
                        if (table.getColumnIndex("rel_flux_SNR_" + (isRefStar[ap] ? "C" : "T") + (ap + 1)) == MeasurementTable.COLUMN_NOT_FOUND) {
                            i = table.getFreeColumn("rel_flux_SNR_" + (isRefStar[ap] ? "C" : "T") + (ap + 1));
                        }
                    }
                }
            }
            if (showCompTot) {
                if (table.getColumnIndex(TOTAL) == MeasurementTable.COLUMN_NOT_FOUND) {
                    i = table.getFreeColumn(TOTAL);
                }
                if (showErrors && table.getColumnIndex(TOTAL_ERROR) == MeasurementTable.COLUMN_NOT_FOUND) {
                    i = table.getFreeColumn(TOTAL_ERROR);
                }
            }
        }

        for (int ap = 0; ap < nApertures; ap++) {
            String header = (isRefStar[ap] ? "_C" : "_T") + (ap + 1);
            if (showPosition) {
                if (table.getColumnIndex(AP_XCENTER + header) == MeasurementTable.COLUMN_NOT_FOUND) {
                    i = table.getFreeColumn(AP_XCENTER + header);
                }
                if (table.getColumnIndex(AP_YCENTER + header) == MeasurementTable.COLUMN_NOT_FOUND) {
                    i = table.getFreeColumn(AP_YCENTER + header);
                }
            }
            if (showPositionFITS) {
                if (table.getColumnIndex(AP_XCENTER_FITS + header) == MeasurementTable.COLUMN_NOT_FOUND) {
                    i = table.getFreeColumn(AP_XCENTER_FITS + header);
                }
                if (table.getColumnIndex(AP_YCENTER_FITS + header) == MeasurementTable.COLUMN_NOT_FOUND) {
                    i = table.getFreeColumn(AP_YCENTER_FITS + header);
                }
            }
            if (showRADEC && wcs != null && wcs.hasRaDec() && raDec != null) {
                if (table.getColumnIndex(AP_RA + header) == ResultsTable.COLUMN_NOT_FOUND) {
                    i = table.getFreeColumn(AP_RA + header);
                }
                if (table.getColumnIndex(AP_DEC + header) == ResultsTable.COLUMN_NOT_FOUND) {
                    i = table.getFreeColumn(AP_DEC + header);
                }
            }
            if (showPhotometry && table.getColumnIndex(AP_SOURCE + header) == MeasurementTable.COLUMN_NOT_FOUND) {
                i = table.getFreeColumn(AP_SOURCE + header);
            }
            if (showNAperPixels && table.getColumnIndex(AP_NAPERPIX + suffix) == ResultsTable.COLUMN_NOT_FOUND) {
                i = table.getFreeColumn(AP_NAPERPIX + suffix);
            }
            if (showErrors && table.getColumnIndex(AP_SOURCE_ERROR + header) == MeasurementTable.COLUMN_NOT_FOUND) {
                i = table.getFreeColumn(AP_SOURCE_ERROR + header);
            }
            if (hasAbsMag && table.getColumnIndex(AP_SOURCE_AMAG + header) == MeasurementTable.COLUMN_NOT_FOUND) {
                i = table.getFreeColumn(AP_SOURCE_AMAG + header);
            }
            if (hasAbsMag && showErrors && table.getColumnIndex(AP_SOURCE_AMAG_ERR + header) == MeasurementTable.COLUMN_NOT_FOUND) {
                i = table.getFreeColumn(AP_SOURCE_AMAG_ERR + header);
            }
            if (showSNR && table.getColumnIndex(AP_SOURCE_SNR + header) == MeasurementTable.COLUMN_NOT_FOUND) {
                i = table.getFreeColumn(AP_SOURCE_SNR + header);
            }
            if (showPeak && table.getColumnIndex(AP_PEAK + header) == MeasurementTable.COLUMN_NOT_FOUND) {
                i = table.getFreeColumn(AP_PEAK + header);
            }
            if (showMean && table.getColumnIndex(AP_MEAN + header) == MeasurementTable.COLUMN_NOT_FOUND) {
                i = table.getFreeColumn(AP_MEAN + header);
            }
            if (showBack && table.getColumnIndex(AP_BACK + header) == MeasurementTable.COLUMN_NOT_FOUND) {
                i = table.getFreeColumn(AP_BACK + header);
            }
            if (showNBackPixels && table.getColumnIndex(AP_NBACKPIX + suffix) == ResultsTable.COLUMN_NOT_FOUND) {
                i = table.getFreeColumn(AP_NBACKPIX + suffix);
            }
            if (showMeanWidth && calcRadProFWHM && table.getColumnIndex(AP_FWHM) == MeasurementTable.COLUMN_NOT_FOUND) {
                i = table.getFreeColumn(AP_FWHM + header);
            }
            if (showMeanWidth && table.getColumnIndex(AP_MEANWIDTH) == MeasurementTable.COLUMN_NOT_FOUND) {
                i = table.getFreeColumn(AP_MEANWIDTH + header);
            }
            if (showWidths) {
                if (table.getColumnIndex(AP_XWIDTH + header) == MeasurementTable.COLUMN_NOT_FOUND) {
                    i = table.getFreeColumn(AP_XWIDTH + header);
                }
                if (table.getColumnIndex(AP_YWIDTH + header) == MeasurementTable.COLUMN_NOT_FOUND) {
                    i = table.getFreeColumn(AP_YWIDTH + header);
                }
            }
            if (showAngle && table.getColumnIndex(AP_ANGLE + header) == ResultsTable.COLUMN_NOT_FOUND) {
                i = table.getFreeColumn(AP_ANGLE + header);
            }
            if (showRoundness && table.getColumnIndex(AP_ROUNDNESS + header) == ResultsTable.COLUMN_NOT_FOUND) {
                i = table.getFreeColumn(AP_ROUNDNESS + header);
            }
            if (showVariance && table.getColumnIndex(AP_VARIANCE + header) == ResultsTable.COLUMN_NOT_FOUND) {
                i = table.getFreeColumn(AP_VARIANCE + header);
            }
            if (showRaw && isCalibrated) {
                if (table.getColumnIndex(AP_RAWSOURCE + header) == ResultsTable.COLUMN_NOT_FOUND) {
                    i = table.getFreeColumn(AP_RAWSOURCE + header);
                }
                if (table.getColumnIndex(AP_RAWBACK + header) == ResultsTable.COLUMN_NOT_FOUND) {
                    i = table.getFreeColumn(AP_RAWBACK + header);
                }
            }
        }

        table.setLock(false);
        table.show();
        return true;
    }


    /**
     * Add aperture number to overlay display.
     */
//	protected void addStringRoi (double x, double y, String text)
//		{
//		super.addStringRoi (x,y,"  #"+(aperture+1)+":  "+text.trim());
//		}

    /**
     * Standard preferences dialog for MultiAperture_
     */
    protected GenericSwingDialog dialog() {
        if (!Prefs.isLocationOnScreen(new Point(xLocation, yLocation))) {
            xLocation = 10;
            yLocation = 10;
        }

        GenericSwingDialog gd = new GenericSwingDialog("Multi-Aperture Measurements", xLocation, yLocation);
        gd.setSaveAndUseStepSize(true);

        var sliders = new JPanel[5];
        if (stackSize > 1) {
            firstSlice = (alwaysstartatfirstSlice && !(this instanceof Stack_Aligner)) ? 1 : firstSlice;
            sliders[0] = gd.addSlider("           First slice ", 1, stackSize, (firstSlice == stackSize || (alwaysstartatfirstSlice && !(this instanceof Stack_Aligner))) ? 1 : firstSlice, d -> firstSlice = d.intValue());
            sliders[1] = gd.addSlider("           Last slice ", 1, stackSize, lastSlice, d -> lastSlice = d.intValue());
        }
        gd.addCheckbox("Auto Radius", autoRadius, b -> autoRadius = b);
        gd.addToSameRow();
        gd.setOverridePosition(true);
        sliders[2] = gd.addFloatSlider("Radius of object aperture", 0.01, radius > 100 ? radius : 100, false, radius, 3, 1.0, d -> radius = d);
        gd.setOverridePosition(false);
        sliders[3] = gd.addFloatSlider("Inner radius of background annulus", 0.01, rBack1 > 100 ? rBack1 : 100, false, rBack1, 3, 1.0, d -> rBack1 = d);
        sliders[4] = gd.addFloatSlider("Outer radius of background annulus", 0.01, rBack2 > 100 ? rBack2 : 100, false, rBack2, 3, 1.0, d -> rBack2 = d);
        gd.addCheckbox("Use previous " + nAperturesStored + " apertures (1-click to set first aperture location)", previous && nAperturesStored > 0, b -> previous = b);
        gd.addCheckbox("Use RA/Dec to locate aperture positions", useWCS, b -> useWCS = b);
        gd.addCheckbox("Use single step mode (1-click to set first aperture location in each image)", singleStep, b -> singleStep = b);
        gd.addCheckbox("Allow aperture changes between slices in single step mode (right click to advance image)", allowSingleStepApChanges, b -> allowSingleStepApChanges = b);
        gd.addMessage("");

        // Make all sliders the same size
        var sliderWidth = Math.max(GenericSwingDialog.getSliderWidth(sliders[2]), Integer.toString(stackSize).length());
        for (JPanel slider : sliders) {
            GenericSwingDialog.setSliderSpinnerColumns(slider, sliderWidth);
        }

        // HERE ARE THE THINGS WHICH AREN'T ABSOLUTELY NECESSARY
        addFancyDialog(gd);

        gd.addMessage("CLICK 'PLACE APERTURES' AND SELECT APERTURE LOCATIONS WITH LEFT CLICKS.\nTHEN RIGHT CLICK or <ENTER> TO BEGIN PROCESSING.\n(to abort aperture selection or processing, press <ESC>)");
        if (!(this instanceof Stack_Aligner)) gd.enableYesNoCancel("Place Apertures", "Aperture Settings");
        return gd;
    }

    /**
     * Adds options to MultiAperture_ dialog() which aren't absolutely necessary.
     * Sub-classes of MultiAperture_ may choose to replace or extend this functionality if they use the original dialog().
     */
    protected void addFancyDialog(GenericSwingDialog gd) {
        final var list1 = new ArrayList<Consumer<Boolean>>();
        list1.add(b -> reposition = b);
        list1.add(b -> haltOnError = b);
        list1.add(b -> removeBackStars = b);
        list1.add(b -> backIsPlane = b);

        // Suggestion of comp. stars
        final var columns = Math.max(10, Math.max(Double.toString(max).length(), Double.toString(maxPeakValue).length()));

        final var listb = new ArrayList<Consumer<Boolean>>();
        listb.add(b -> suggestCompStars = b);
        listb.add(b -> enableLog = b);
        listb.add(b -> debugAp = b);
        gd.addCheckboxGroup(1, 3, new String[]{"Auto comparison stars", "Enable log", "Show peaks"}, new boolean[]{suggestCompStars, enableLog, debugAp}, listb);

        final var gauss = gd.addBoundedNumericField("Smoothing Filter Radius", new GenericSwingDialog.Bounds(0, Double.MAX_VALUE), gaussRadius, 1, 10, "pixels", d -> gaussRadius = d);
        final var autoPeaks = gd.addCheckbox("Auto Thresholds", autoPeakValues, b -> autoPeakValues = b);
        gd.addToSameRow();
        gd.setOverridePosition(true);
        final var maxPeak = gd.addBoundedNumericField("Max. Peak Value", new GenericSwingDialog.Bounds(0, Double.MAX_VALUE), maxPeakValue, 1, columns, null, d -> maxPeakValue = d);
        gd.addToSameRow();
        final var minPeak = gd.addBoundedNumericField("Min. Peak Value", new GenericSwingDialog.Bounds(0, Double.MAX_VALUE), minPeakValue, 1, columns, null, d -> minPeakValue = d);
        gd.resetPositionOverride();
        gd.setNewPosition(GridBagConstraints.WEST);
        gd.setLeftInset(20);
        final var starSelection = gd.addBoundedNumericField("Base Aperture", new GenericSwingDialog.Bounds(1, Double.MAX_VALUE), referenceStar, 1, 7, null, true, d -> referenceStar = d.intValue());
        gd.addToSameRow();
        gd.resetPositionOverride();
        if (upperBrightness < 101.0) upperBrightness = 150.0;
        if (lowerBrightness >  99.0 || lowerBrightness < 0.0 ) lowerBrightness = 50.0;
        final var maxDBrightness = gd.addBoundedNumericField("Max. Comp. Brightness %", new GenericSwingDialog.Bounds(101, Double.MAX_VALUE), upperBrightness, 1, columns, null, d -> upperBrightness = d);
        gd.addToSameRow();
        final var minDBrightness = gd.addBoundedNumericField("Min. Comp. Brightness %", new GenericSwingDialog.Bounds(0, 99), lowerBrightness, 1, columns, null, d -> lowerBrightness = d);
        gd.setOverridePosition(false);

        final var brightnessVsDistance = gd.addBoundedNumericField("Weight of brightness vs. distance:", new GenericSwingDialog.Bounds(0, 100), brightness2DistanceWeight, 1, columns, null, d -> brightness2DistanceWeight = d);
        gd.addToSameRow();
        final var maxStars = gd.addBoundedNumericField("Max. Comp. Stars", new GenericSwingDialog.Bounds(0, Double.MAX_VALUE), maxSuggestedStars, 1, columns, null, true, d -> maxSuggestedStars = d.intValue());
        autoPeaks.setToolTipText("<hmtl>When enabled, set peak thresholds based on image statistics.<br>Max = 0.9 * Max Pixel Value, Min = Mean Pixel Value + 1σ.</html>");

        final var liveStats = asw.getLiveStatistics();
        var minP = autoPeakValues ? liveStats.mean + (1 * liveStats.stdDev) : minPeakValue;
        var maxP = autoPeakValues ? liveStats.max * 0.9 : maxPeakValue;
        if (enableLog) AIJLogger.log("Image mean = "+liveStats.mean);
        if (enableLog) AIJLogger.log("Image stdDev = "+liveStats.stdDev);
        if (enableLog) AIJLogger.log("Image max = "+liveStats.max);
        minPeak.c1().setEnabled(!autoPeakValues);
        maxPeak.c1().setEnabled(!autoPeakValues);
        DecimalFormat fourPlaces = new DecimalFormat("###,##0.00", IJU.dfs);
        DecimalFormat scientificFourPlaces = new DecimalFormat("0.####E00", IJU.dfs);
        if (autoPeakValues) {
            GenericSwingDialog.getTextFieldFromSpinner((JSpinner) minPeak.c1()).ifPresent(tf -> tf.setText(minP < 1000000.0 ? fourPlaces.format(minP) : scientificFourPlaces.format(minP)));
            GenericSwingDialog.getTextFieldFromSpinner((JSpinner) maxPeak.c1()).ifPresent(tf -> tf.setText(maxP < 1000000.0 ? fourPlaces.format(maxP) : scientificFourPlaces.format(maxP)));
        } else {
            GenericSwingDialog.getTextFieldFromSpinner((JSpinner) minPeak.c1()).ifPresent(tf -> tf.setText(minPeakValue < 1000000.0 ? fourPlaces.format(minPeakValue) : scientificFourPlaces.format(minPeakValue)));
            GenericSwingDialog.getTextFieldFromSpinner((JSpinner) maxPeak.c1()).ifPresent(tf -> tf.setText(maxPeakValue < 1000000.0 ? fourPlaces.format(maxPeakValue) : scientificFourPlaces.format(maxPeakValue)));
        }
        autoPeaks.addActionListener($ -> {
            minPeak.c1().setEnabled(autoPeakValues);
            maxPeak.c1().setEnabled(autoPeakValues);
            if (!autoPeakValues) {
                GenericSwingDialog.getTextFieldFromSpinner((JSpinner) minPeak.c1()).ifPresent(tf -> tf.setText(minP < 1000000.0 ? fourPlaces.format(minP) : scientificFourPlaces.format(minP)));
                GenericSwingDialog.getTextFieldFromSpinner((JSpinner) maxPeak.c1()).ifPresent(tf -> tf.setText(maxP < 1000000.0 ? fourPlaces.format(maxP) : scientificFourPlaces.format(maxP)));
            } else {
                GenericSwingDialog.getTextFieldFromSpinner((JSpinner) minPeak.c1()).ifPresent(tf -> tf.setText(minPeakValue < 1000000.0 ? fourPlaces.format(minPeakValue) : scientificFourPlaces.format(minPeakValue)));
                GenericSwingDialog.getTextFieldFromSpinner((JSpinner) maxPeak.c1()).ifPresent(tf -> tf.setText(maxPeakValue < 1000000.0 ? fourPlaces.format(maxPeakValue) : scientificFourPlaces.format(maxPeakValue)));
            }
        });


        starSelection.c2().setToolTipText("The aperture to base comparison star selection on.");
        gauss.c2().setToolTipText("<html>Radius of gaussian smoothing to use when finding initial peaks.<br> Set to 1 to disable.</html>");
        maxPeak.c2().setToolTipText("Maximum peak value to consider");
        minPeak.c2().setToolTipText("Minimum peak value to consider");
        maxStars.c2().setToolTipText("Maximum number of comparison stars to select. Includes already selected comp. stars in its count");
        maxDBrightness.c2().setToolTipText("Upper brightness limit of comp stars relative to the base aperture brightness");
        minDBrightness.c2().setToolTipText("Lower brightness limit of comp stars relative to the base aperture brightness");
        brightnessVsDistance.c2().setToolTipText("<html>Weight of brightness vs distance, used to sort stars.<br>" +
                "Based on normalized Source-Sky brightness and distance relative to the specified base aperture.<br>" +
                "A value of 100 makes the weighting based entirely on the normalized brightness.<br>" +
                "A value of 0 makes the weighting entirely based on proximity to the specified base aperture.<br>" +
                "If more stars were found than the maximum requested, the stars with the highest weights are used.</html>");

        JSpinner maxPeakSpin = (JSpinner) maxPeak.c1();
        JSpinner minPeakSpin = (JSpinner) minPeak.c1();
        maxPeakSpin.getModel().addChangeListener($ -> {
            if (maxPeakSpin.getValue() instanceof Double d) {
                if (d.compareTo(minPeakValue) <= 0) maxPeakSpin.setValue(GenericSwingDialog.nextUp(minPeakValue));
            }
        });
        minPeakSpin.getModel().addChangeListener($ -> {
            if (minPeakSpin.getValue() instanceof Double d) {
                if (d.compareTo(maxPeakValue) >= 0) minPeakSpin.setValue(GenericSwingDialog.nextDown(maxPeakValue));
            }
        });

        gd.addMessage("");

        // GET NON-REQUIRED DIALOGUE FIELDS:
        gd.addCheckboxGroup(2, 2, new String[]{"Centroid apertures (initial setting)", "Halt processing on WCS or centroid error",
                        "Remove stars from background", "Assume background is a plane"},
//                                                "Compute relative flux", "Compute relative flux error",
//                                                "Compute relative flux signal-to-noise", "Compute total comparison star counts"},
                new boolean[]{reposition, haltOnError, removeBackStars, backIsPlane}, list1);//,showRatio, showRatioError,showRatioSNR,showCompTot});

        gd.addMessage("");
        gd.addCheckbox("Vary aperture radius based on FWHM", useVarSizeAp, b -> useVarSizeAp = b);
        gd.addFloatSlider("            FWHM factor (set to 0.00 for radial profile mode):", 0.0, 5.0, false, apFWHMFactor, 3, 0.1, d -> apFWHMFactor = d);
        gd.addBoundedNumericField("Radial profile mode normalized flux cutoff:", new GenericSwingDialog.Bounds(0, false, 1, false), autoModeFluxCutOff, .01, 6, "(0 < cuffoff < 1 ; default = 0.010)", d -> autoModeFluxCutOff = d);
        gd.addMessage("");
        gd.addCheckbox("Prompt to enter ref star apparent magnitude (required if target star apparent mag is desired)", getMags, b -> getMags = b);

        final var list2 = new ArrayList<Consumer<Boolean>>();
        list2.add(b -> updatePlot = b);
        list2.add(b -> showHelp = b);
        gd.addCheckboxGroup(1, 2, new String[]{"Update table and plot while running", "Show help panel during aperture selection"},
                new boolean[]{updatePlot, showHelp}, list2);
    }

    /**
     * Last part of non-required dialog created by addFancyDialog().
     * Sub-classes not using the original dialog() will need a dummy version of this method!
     */
    protected boolean finishFancyDialog(GenericSwingDialog gd) {
        // GET NON-REQUIRED DIALOGUE FIELDS:
        //	showRatio,showRatioError,showRatioSNR,useVarSizeAp,apFWHMFactor
        if (gd.invalidNumber()) {
            IJ.beep();
            IJ.error("Invalid number entered");
            return false;
        }
        if (apFWHMFactor < 0) {
            IJ.beep();
            IJ.error("Invalid aperture FWHM factor entered");
            return false;
        }
        if (autoModeFluxCutOff <= 0 || autoModeFluxCutOff >= 1) {
            IJ.beep();
            IJ.error("Invalid flux cutoff entered");
            return false;
        }
        if (oldUseVarSizeAp != useVarSizeAp || oldapFWHMFactor != apFWHMFactor || oldAutoModeFluxCutOff != autoModeFluxCutOff ||
                oldRemoveBackStars != removeBackStars || oldBackIsPlane != backIsPlane || oldGetMags != getMags) {
            changeAperture();
        }
        // follow = gd.getNextBoolean();
        Prefs.set(Aperture_.AP_PREFS_REPOSITION, reposition);
        Prefs.set(Aperture_.AP_PREFS_REMOVEBACKSTARS, removeBackStars);
        Prefs.set(Aperture_.AP_PREFS_BACKPLANE, backIsPlane);
        Prefs.set(MultiAperture_.PREFS_SHOWRATIO, showRatio);
        Prefs.set(MultiAperture_.PREFS_SHOWCOMPTOT, showCompTot);
        Prefs.set(MultiAperture_.PREFS_SHOWRATIO_ERROR, showRatioError);
        Prefs.set(MultiAperture_.PREFS_SHOWRATIO_SNR, showRatioSNR);
        Prefs.set(MultiAperture_.PREFS_USEVARSIZEAP, useVarSizeAp);
        Prefs.set(MultiAperture_.PREFS_HALTONERROR, haltOnError);
        Prefs.set(MultiAperture_.PREFS_SHOWHELP, showHelp);
        Prefs.set(MultiAperture_.PREFS_ALWAYSFIRSTSLICE, alwaysstartatfirstSlice);
        Prefs.set(MultiAperture_.PREFS_APFWHMFACTOR, apFWHMFactor);
        Prefs.set(MultiAperture_.PREFS_AUTOMODEFLUXCUTOFF, autoModeFluxCutOff);
        Prefs.set(MultiAperture_.PREFS_ENABLEDOUBLECLICKS, enableDoubleClicks);
        Prefs.set(MultiAperture_.PREFS_UPDATEPLOT, updatePlot);
        Prefs.set(MultiAperture_.PREFS_GETMAGS, getMags);
        Prefs.set(MultiAperture_.PREFS_MAXPEAKVALUE, maxPeakValue);
        Prefs.set(MultiAperture_.PREFS_MINPEAKVALUE, minPeakValue);
        Prefs.set(MultiAperture_.PREFS_UPPERBRIGHTNESS, upperBrightness);
        Prefs.set(MultiAperture_.PREFS_LOWERBRIGHTNESS, lowerBrightness);
        Prefs.set(MultiAperture_.PREFS_BRIGHTNESSDISTANCE, brightness2DistanceWeight);
        Prefs.set(MultiAperture_.PREFS_MAXSUGGESTEDSTARS, maxSuggestedStars);
        Prefs.set(PREFS_SUGGESTCOMPSTARS, suggestCompStars);
        Prefs.set(PREFS_DEBUGAPERTURESUGGESTION, debugAp);
        Prefs.set(PREFS_GAUSSRADIUS, gaussRadius);
        Prefs.set(PREFS_AUTOPEAKS, autoPeakValues);
        Prefs.set(PREFS_AUTORADIUS, autoRadius);
        Prefs.set(PREFS_REFERENCESTAR, referenceStar);
        Prefs.set(PREFS_ENABLELOG, enableLog);
        Prefs.savePreferences();

        if (!(this instanceof Stack_Aligner) && !gd.wasOKed()) {
            cancel();
            Thread t2 = new Thread(() -> IJ.runPlugIn("Astronomy.Set_Aperture", "from_MA"));
            t2.start();
            return false;
        }

        showHelpPanel();

        return true;
    }

    protected void showHelpPanel() {
        MAIcon = this instanceof Stack_Aligner ? createImageIcon("images/align.png", "Stack Aligner Icon") : createImageIcon("images/multiaperture.png", "Multi-Aperture Icon");
        helpFrame = new JFrame(this instanceof Stack_Aligner ? "Stack Aligner Help" : "Multi-Aperture Help");
        helpFrame.setIconImage(MAIcon.getImage());
        helpPanel = new JPanel(new SpringLayout());
        helpScrollPane = new JScrollPane(helpPanel);
        helpFrame.setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        helpFrame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                closeHelpPanel();
            }
        });
        JLabel leftClickName = new JLabel("left-click:");
        leftClickName.setHorizontalAlignment(JLabel.RIGHT);
        helpPanel.add(leftClickName);
        if (previous) {
            leftClickLabel = new JLabel("Add previous stored apertures by clicking on the star corresponding to T1/C1");
        } else { leftClickLabel = new JLabel("Add target star aperture T1"); }
        helpPanel.add(leftClickLabel);

        JLabel shiftLeftClickName = new JLabel("<Shift>left-click:");
        shiftLeftClickName.setHorizontalAlignment(JLabel.RIGHT);
        helpPanel.add(shiftLeftClickName);
        if (previous) { shiftLeftClickLabel = new JLabel(""); } else {
            shiftLeftClickLabel = new JLabel("Add reference star aperture C1");
        }
        helpPanel.add(shiftLeftClickLabel);

        JLabel shiftControlLeftClickName = new JLabel("<Shift><Ctrl>left-click:");
        shiftControlLeftClickName.setHorizontalAlignment(JLabel.RIGHT);
        helpPanel.add(shiftControlLeftClickName);
        if (previous) { shiftControlLeftClickLabel = new JLabel(""); } else {
            shiftControlLeftClickLabel = new JLabel("");
        }
        helpPanel.add(shiftControlLeftClickLabel);

        JLabel altLeftClickName = new JLabel("<Alt>left-click:");
        altLeftClickName.setHorizontalAlignment(JLabel.RIGHT);
        helpPanel.add(altLeftClickName);
        if (previous) { altLeftClickLabel = new JLabel(""); } else {
            altLeftClickLabel = new JLabel("Invert sense of centroid setting for new aperture");
        }
        helpPanel.add(altLeftClickLabel);

        JLabel rightClickName = new JLabel("right-click:");
        rightClickName.setHorizontalAlignment(JLabel.RIGHT);
        helpPanel.add(rightClickName);
        rightClickLabel = new JLabel("");
        helpPanel.add(rightClickLabel);

        JLabel enterName = new JLabel("<Enter>:");
        enterName.setHorizontalAlignment(JLabel.RIGHT);
        helpPanel.add(enterName);
        enterLabel = new JLabel("");
        helpPanel.add(enterLabel);

        JLabel controlLeftClickName = new JLabel("<Ctrl>left-click:");
        controlLeftClickName.setHorizontalAlignment(JLabel.RIGHT);
        helpPanel.add(controlLeftClickName);
        controlLeftClickLabel = new JLabel("Zoom In");
        helpPanel.add(controlLeftClickLabel);

        JLabel controlRightClickName = new JLabel("<Ctrl>right-click:");
        controlRightClickName.setHorizontalAlignment(JLabel.RIGHT);
        helpPanel.add(controlRightClickName);
        controlRightClickLabel = new JLabel("Zoom Out");
        helpPanel.add(controlRightClickLabel);

        JLabel mouseWheelName = new JLabel("roll mouse wheel:");
        mouseWheelName.setHorizontalAlignment(JLabel.RIGHT);
        helpPanel.add(mouseWheelName);
        mouseWheelLabel = new JLabel("Zoom In/Out");
        helpPanel.add(mouseWheelLabel);

        JLabel leftClickDragName = new JLabel("left-click-drag:");
        leftClickDragName.setHorizontalAlignment(JLabel.RIGHT);
        helpPanel.add(leftClickDragName);
        leftClickDragLabel = new JLabel("Pan image up/down/left/right");
        helpPanel.add(leftClickDragLabel);

        JLabel altLeftClickDragName = new JLabel("<Alt>left-click-drag:");
        altLeftClickDragName.setHorizontalAlignment(JLabel.RIGHT);
        helpPanel.add(altLeftClickDragName);
        altLeftClickDragLabel = new JLabel("Measure arclength");
        helpPanel.add(altLeftClickDragLabel);

        JLabel middleClickName = new JLabel("middle-click:");
        middleClickName.setHorizontalAlignment(JLabel.RIGHT);
        helpPanel.add(middleClickName);
        middleClickLabel = new JLabel("Center clicked point in image display (if enabled in Preferences menu)");
        helpPanel.add(middleClickLabel);

        JLabel escapeName = new JLabel("<escape>:");
        escapeName.setHorizontalAlignment(JLabel.RIGHT);
        helpPanel.add(escapeName);
        escapeLabel = new JLabel(this instanceof Stack_Aligner ? "Cancel Stack Aligner" : "Cancel Multi-Aperture");
        helpPanel.add(escapeLabel);

        SpringUtil.makeCompactGrid(helpPanel, helpPanel.getComponentCount() / 2, 2, 6, 6, 6, 6);

        helpFrame.add(helpScrollPane);
        helpFrame.pack();
        helpFrame.setResizable(true);
        Dimension mainScreenSize = Toolkit.getDefaultToolkit().getScreenSize();
        if (!Prefs.isLocationOnScreen(new Point(helpFrameLocationX, helpFrameLocationY))) {
            helpFrameLocationX = mainScreenSize.width / 2 - helpFrame.getWidth() / 2;
            helpFrameLocationY = mainScreenSize.height / 2 - helpFrame.getHeight() / 2;
        }
        helpFrame.setLocation(helpFrameLocationX, helpFrameLocationY);
        helpFrame.setVisible(showHelp && !runningWCSOnlyAlignment);
    }

    /**
     * Returns an ImageIcon, or null if the path was invalid.
     */
    protected ImageIcon createImageIcon(String path, String description) {
        java.net.URL imgURL = MultiPlot_.class.getClassLoader().getResource(path);
        if (imgURL != null) {
            return new ImageIcon(imgURL, description);
        } else {
            IJ.log("Couldn't find icon file: " + path);
            return null;
        }
    }

    protected void closeHelpPanel() {
        if (helpFrame != null && helpFrame.isShowing()) {
            helpFrameLocationX = helpFrame.getLocation().x;
            helpFrameLocationY = helpFrame.getLocation().y;
            helpFrame.setVisible(false);
            Prefs.set("plot2.helpFrameLocationX", helpFrameLocationX);
            Prefs.set("plot2.helpFrameLocationY", helpFrameLocationY);
        }
        if (helpFrame != null) {
            helpFrame.dispose();
            helpFrame = null;
        }
    }

}

