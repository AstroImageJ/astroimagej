package Astronomy;// MultiAperture_.java

import Astronomy.multiaperture.CustomPixelApertureHandler;
import astroj.*;
import ij.IJ;
import ij.ImagePlus;
import ij.Prefs;
import ij.WindowManager;
import ij.astro.gui.GenericSwingDialog;
import ij.astro.gui.RadioEnum;
import ij.astro.gui.ToolTipProvider;
import ij.astro.gui.nstate.NState;
import ij.astro.io.prefs.Property;
import ij.astro.logging.AIJLogger;
import ij.gui.GenericDialog;
import ij.gui.PlotWindow;
import ij.gui.Toolbar;
import ij.measure.ResultsTable;
import ij.plugin.frame.Recorder;
import ij.process.ImageProcessor;
import ij.util.ArrayUtil;
import ij.util.Tools;

import javax.swing.*;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.*;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.List;
import java.util.Timer;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static ij.Prefs.KEY_PREFIX;
import static ij.astro.gui.GenericSwingDialog.ComponentPair.Type.C1;


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
public class MultiAperture_ extends Aperture_ implements MouseListener, MouseMotionListener, KeyListener, WindowListener {
    private static final DecimalFormat FORMAT = new DecimalFormat("###0.#");
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
    protected static String PREFS_APFWHMFACTORSTACK = "multiaperture.apfwhmfactorstack";
    protected static String PREFS_AUTOMODEFLUXCUTOFF = "multiaperture.automodefluxcutoff";
    protected static String PREFS_AUTOMODEFLUXCUTOFFFIXED = "multiaperture.automodefluxcutofffixed";
    protected static String PREFS_APRADIUS = "multiaperture.apradius";
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
    private static ApRadius radiusSetting = ApRadius.AUTO_FIXED;
    protected int ngot = 0;
    //	protected int aperture=0;
    protected int nAperturesMax = 1000;
    protected int nApertures = 2;
    protected static int nAperturesStored = 0, nImportedApStored = 0;
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
    protected boolean useRadialProfile = false;
    protected boolean showHelp = true;
    protected boolean alwaysstartatfirstSlice = false;
    protected boolean haltOnError = true;
    protected boolean foundFWHM = false;
    protected boolean showRatio = true;
    protected boolean showCompTot = true;
    protected boolean debugAp = false;
    protected boolean firstRun = true;
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
    AstroStackWindow asw = null;
    AstroCanvas ac = null;
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
    Timer stackTaskTimer = null;
    boolean doubleClick = false;
    TimerTask doubleClickTask = null;
    Timer doubleClickTaskTimer = null;
    static DecimalFormat uptoEightPlaces = new DecimalFormat("#####0.########", IJU.dfs);
    double max = 0;
    private double gaussRadius = 3.5;
    private boolean t1Placed = false;
    private Seeing_Profile.ApRadii oldRadii;
    private int referenceStar = 1;
    private boolean suggestionRunning;
    private Seeing_Profile sp;
    private List<Seeing_Profile.ApRadii> stackRadii = new ArrayList<>();
    private final HashSet<Component> singleStepListeners = new HashSet<>();
    protected static final Property<Boolean> updateImageDisplay = new Property<>(true, MultiAperture_.class);
    public static final Property<ApLoading> apLoading = new Property<>(ApLoading.ALL_NEW, MultiAperture_.class);
    protected static final Property<ApertureShape> apertureShape = new Property<>(ApertureShape.CIRCULAR, MultiAperture_.class);
    private static String lastRun = "<Not yet run>";
    private boolean processingStackForRadii;
    private final CustomPixelApertureHandler customPixelApertureHandler = new CustomPixelApertureHandler();

    public MultiAperture_() {
        customPixelApertureHandler.setExitCallback(() -> {
            cancel();
            IJ.beep();
            Prefs.set(MultiAperture_.PREFS_CANCELED, "true");
            shutDown();
        });
    }

    //	public static double RETRY_RADIUS = 3.0;

    public static Set<String> getApertureKeys() {
        var o = new HashSet<String>();

        o.add(KEY_PREFIX+AP_PREFS_REMOVEBACKSTARS);
        o.add(KEY_PREFIX+PREFS_APFWHMFACTOR);
        o.add(KEY_PREFIX+PREFS_APFWHMFACTORSTACK);
        o.add(KEY_PREFIX+PREFS_AUTOMODEFLUXCUTOFF);
        o.add(KEY_PREFIX+PREFS_AUTOMODEFLUXCUTOFFFIXED);
        o.add(KEY_PREFIX+PREFS_APRADIUS);
        o.add(KEY_PREFIX+PREFS_XAPERTURES);
        o.add(KEY_PREFIX+PREFS_YAPERTURES);
        o.add(KEY_PREFIX+PREFS_RAAPERTURES);
        o.add(KEY_PREFIX+PREFS_DECAPERTURES);
        o.add(KEY_PREFIX+PREFS_ISREFSTAR);
        o.add(KEY_PREFIX+PREFS_ISALIGNSTAR);
        o.add(KEY_PREFIX+PREFS_CENTROIDSTAR);
        o.add(KEY_PREFIX+PREFS_NAPERTURESMAX);
        o.add(KEY_PREFIX+PREFS_ABSMAGAPERTURES);
        o.add(KEY_PREFIX+apLoading.getPropertyKey());
        o.add(KEY_PREFIX+apertureShape.getPropertyKey());
        o.add(KEY_PREFIX+PREFS_APRADIUS);
        o.add(KEY_PREFIX+Aperture_.AP_PREFS_RADIUS);
        o.add(KEY_PREFIX+AP_PREFS_RBACK1);
        o.add(KEY_PREFIX+AP_PREFS_RBACK2);
        o.add(KEY_PREFIX+AP_PREFS_SHOWFILENAME);
        o.add(KEY_PREFIX+AP_PREFS_SHOWSLICENUMBER);
        o.add(KEY_PREFIX+AP_PREFS_SHOWPEAK);
        o.add(KEY_PREFIX+AP_PREFS_SHOWMEAN);
        o.add(KEY_PREFIX+AP_PREFS_EXACT);
        o.add(KEY_PREFIX+AP_PREFS_SHOWSATWARNING);
        o.add(KEY_PREFIX+AP_PREFS_FORCEABSMAGDISPLAY);
        o.add(KEY_PREFIX+AP_PREFS_SHOWLINWARNING);
        o.add(KEY_PREFIX+AP_PREFS_SATWARNLEVEL);
        o.add(KEY_PREFIX+AP_PREFS_LINWARNLEVEL);
        o.add(KEY_PREFIX+AP_PREFS_USEHOWELL);
        o.add(KEY_PREFIX+AP_PREFS_CALCRADPROFWHM);
        o.add(KEY_PREFIX+AP_PREFS_BACKPLANE);
        o.add(KEY_PREFIX+AP_PREFS_CCDGAIN);
        o.add(KEY_PREFIX+AP_PREFS_CCDNOISE);
        o.add(KEY_PREFIX+AP_PREFS_CCDDARK);
        o.add(KEY_PREFIX+AP_PREFS_DARKKEYWORD);
        o.add(KEY_PREFIX+AP_PREFS_REPOSITION);
        o.add(KEY_PREFIX+AP_PREFS_FITSKEYWORDS);
        o.add(KEY_PREFIX+AP_PREFS_REMOVEBACKSTARS);
        o.add(KEY_PREFIX+AP_PREFS_SHOWREMOVEDPIXELS);
        o.add(KEY_PREFIX+AP_PREFS_SHOWPOSITION);
        o.add(KEY_PREFIX+AP_PREFS_SHOWPOSITION_FITS);
        o.add(KEY_PREFIX+AP_PREFS_SHOWPHOTOMETRY);
        o.add(KEY_PREFIX+AP_PREFS_SHOWNAPERPIXELS);
        o.add(KEY_PREFIX+AP_PREFS_SHOWNBACKPIXELS);
        o.add(KEY_PREFIX+AP_PREFS_SHOWBACK);
        o.add(KEY_PREFIX+AP_PREFS_SHOWWIDTHS);
        o.add(KEY_PREFIX+AP_PREFS_SHOWRADII);
        o.add(KEY_PREFIX+AP_PREFS_SHOWTIMES);
        o.add(KEY_PREFIX+AP_PREFS_SHOWRAW);
        o.add(KEY_PREFIX+AP_PREFS_SHOWMEANWIDTH);
        o.add(KEY_PREFIX+AP_PREFS_SHOWANGLE);
        o.add(KEY_PREFIX+AP_PREFS_SHOWROUNDNESS);
        o.add(KEY_PREFIX+AP_PREFS_SHOWVARIANCE);
        o.add(KEY_PREFIX+AP_PREFS_SHOWERRORS);
        o.add(KEY_PREFIX+AP_PREFS_SHOWSNR);
        o.add(KEY_PREFIX+AP_PREFS_SHOWRATIOERROR);
        o.add(KEY_PREFIX+AP_PREFS_SHOWRATIOSNR);
        o.add(KEY_PREFIX+AP_PREFS_SHOWFITS);
        o.add(KEY_PREFIX+AP_PREFS_SHOWRADEC);
        o.add(KEY_PREFIX+AP_PREFS_STAROVERLAY);
        o.add(KEY_PREFIX+AP_PREFS_SKYOVERLAY);
        o.add(KEY_PREFIX+AP_PREFS_NAMEOVERLAY);
        o.add(KEY_PREFIX+AP_PREFS_VALUEOVERLAY);
        o.add(KEY_PREFIX+AP_PREFS_TEMPOVERLAY);
        o.add(KEY_PREFIX+AP_PREFS_CLEAROVERLAY);
        o.add(KEY_PREFIX+PREFS_MAXPEAKVALUE);
        o.add(KEY_PREFIX+PREFS_MINPEAKVALUE);
        o.add(KEY_PREFIX+PREFS_UPPERBRIGHTNESS);
        o.add(KEY_PREFIX+PREFS_LOWERBRIGHTNESS);
        o.add(KEY_PREFIX+PREFS_BRIGHTNESSDISTANCE);
        o.add(KEY_PREFIX+PREFS_MAXSUGGESTEDSTARS);
        o.add(KEY_PREFIX+PREFS_DEBUGAPERTURESUGGESTION);
        o.add(KEY_PREFIX+PREFS_GAUSSRADIUS);
        o.add(KEY_PREFIX+PREFS_AUTOPEAKS);
        o.add(KEY_PREFIX+PREFS_AUTORADIUS);
        o.add(KEY_PREFIX+PREFS_REFERENCESTAR);
        o.add(KEY_PREFIX+PREFS_AUTOMODE);
        o.add(KEY_PREFIX+PREFS_FINISHED);
        o.add(KEY_PREFIX+PREFS_CANCELED);
        o.add(KEY_PREFIX+PREFS_PREVIOUS);
        o.add(KEY_PREFIX+PREFS_SINGLESTEP);
        o.add(KEY_PREFIX+PREFS_ALLOWSINGLESTEPAPCHANGES);
        o.add(KEY_PREFIX+PREFS_USEVARSIZEAP);
        o.add(KEY_PREFIX+PREFS_USEMA);
        o.add(KEY_PREFIX+PREFS_USEALIGN);
        o.add(KEY_PREFIX+PREFS_USEWCS);
        o.add(KEY_PREFIX+PREFS_SUGGESTCOMPSTARS);
        o.add(KEY_PREFIX+PREFS_HALTONERROR);
        o.add(KEY_PREFIX+PREFS_SHOWHELP);
        o.add(KEY_PREFIX+PREFS_ALWAYSFIRSTSLICE);
        o.add(KEY_PREFIX+PREFS_APFWHMFACTOR);
        o.add(KEY_PREFIX+PREFS_APFWHMFACTORSTACK);
        o.add(KEY_PREFIX+PREFS_AUTOMODEFLUXCUTOFF);
        o.add(KEY_PREFIX+PREFS_AUTOMODEFLUXCUTOFFFIXED);
        o.add(KEY_PREFIX+PREFS_APRADIUS);
        o.add(KEY_PREFIX+PREFS_SHOWRATIO);
        o.add(KEY_PREFIX+PREFS_SHOWCOMPTOT);
        o.add(KEY_PREFIX+PREFS_SHOWRATIO_ERROR);
        o.add(KEY_PREFIX+PREFS_SHOWRATIO_SNR);
        o.add(KEY_PREFIX+PREFS_NAPERTURESMAX);
        o.add(KEY_PREFIX+PREFS_XAPERTURES);
        o.add(KEY_PREFIX+PREFS_YAPERTURES);
        o.add(KEY_PREFIX+PREFS_RAAPERTURES);
        o.add(KEY_PREFIX+PREFS_DECAPERTURES);
        o.add(KEY_PREFIX+PREFS_ABSMAGAPERTURES);
        o.add(KEY_PREFIX+PREFS_ISREFSTAR);
        o.add(KEY_PREFIX+PREFS_ISALIGNSTAR);
        o.add(KEY_PREFIX+PREFS_CENTROIDSTAR);
        o.add(KEY_PREFIX+PREFS_USEMACROIMAGE);
        o.add(KEY_PREFIX+PREFS_MACROIMAGENAME);
        o.add(KEY_PREFIX+PREFS_ENABLEDOUBLECLICKS);
        o.add(KEY_PREFIX+PREFS_ALWAYSFIRST);
        o.add(KEY_PREFIX+PREFS_GETMAGS);
        o.add(KEY_PREFIX+PREFS_XLOCATION);
        o.add(KEY_PREFIX+PREFS_YLOCATION);
        o.add(KEY_PREFIX+PREFS_PREVNUMMONITORS);

        return o;
    }

    public static Set<String> getCircularApertureKeys() {
        var o = new HashSet<String>();

        o.add(KEY_PREFIX+PREFS_APRADIUS);
        o.add(KEY_PREFIX+PREFS_XAPERTURES);
        o.add(KEY_PREFIX+PREFS_YAPERTURES);
        o.add(KEY_PREFIX+PREFS_RAAPERTURES);
        o.add(KEY_PREFIX+PREFS_DECAPERTURES);
        o.add(KEY_PREFIX+PREFS_ISREFSTAR);
        o.add(KEY_PREFIX+PREFS_ISALIGNSTAR);
        o.add(KEY_PREFIX+PREFS_CENTROIDSTAR);
        o.add(KEY_PREFIX+PREFS_ABSMAGAPERTURES);
        o.add(KEY_PREFIX+Aperture_.AP_PREFS_RADIUS);
        o.add(KEY_PREFIX+AP_PREFS_RBACK1);
        o.add(KEY_PREFIX+AP_PREFS_RBACK2);
        o.add(KEY_PREFIX+PREFS_XAPERTURES);
        o.add(KEY_PREFIX+PREFS_YAPERTURES);
        o.add(KEY_PREFIX+PREFS_RAAPERTURES);
        o.add(KEY_PREFIX+PREFS_DECAPERTURES);
        o.add(KEY_PREFIX+PREFS_ABSMAGAPERTURES);
        o.add(KEY_PREFIX+PREFS_ISREFSTAR);
        o.add(KEY_PREFIX+PREFS_ISALIGNSTAR);
        o.add(KEY_PREFIX+PREFS_GETMAGS);
        o.add(KEY_PREFIX+PREFS_XLOCATION);
        o.add(KEY_PREFIX+PREFS_YLOCATION);

        return o;
    }

    static void checkAndLockTable() {
        if (table != null) table.setLock(true);
    }

    static public void clearTable() {
        if (table != null && MeasurementTable.getMeasurementsWindow(tableName) != null) {
            table.clearTable();
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
            var timeout = 3000;
            do {
                openImage = WindowManager.getImage(macroImageName);
                timeout -= 100;
                if (openImage == null) {
                    IJ.wait(100);
                }
            } while (openImage == null && timeout >= 0);

            imp = openImage;
        }
        if (imp == null) {
            cancel();
            IJ.error("No image windows open for Multi-aperture processing");
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
            if (astronomyToolId != -1) {
                IJ.setTool(astronomyToolId);
            } else {
                IJ.setTool(0);
            }
        }

        if (imp != null && !(imp.getWindow() instanceof AstroStackWindow)) {
            var o = imp.getWindow();
            AstroCanvas ac = new AstroCanvas(imp);
            imp.setWindow(new AstroStackWindow(imp, ac, false, true));
            if (o != null) {
                o.close();
            }
        }

        IJ.register(MultiAperture_.class);
        return super.setup(arg, imp);
    }

    /**
     * Standard ImageJ Plug-in method which runs the plug-in, notes whether the image is a stack,
     * and registers the routine for mouse clicks.
     */
    public void run(ImageProcessor ip) {
        if (Recorder.record) {
            Recorder.setCommand(getClass().getName());
        }

        customPixelApertureHandler.hideControls();

        Prefs.set(MultiAperture_.PREFS_FINISHED, "false");

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

        if (imp != null && !(imp.getWindow() instanceof AstroStackWindow)) {
            var o = imp.getWindow();
            AstroCanvas ac = new AstroCanvas(imp);
            imp.setWindow(new AstroStackWindow(imp, ac, false, true));
            if (o != null) {
                o.close();
            }
        }

        // Sets up canvas again as running through DP gives it the wrong one
        canvas = imp.getCanvas();
        ocanvas = null;
        if (starOverlay || skyOverlay || valueOverlay || nameOverlay) {
            ocanvas = OverlayCanvas.getOverlayCanvas(imp);
            canvas = ocanvas;
        }

        Prefs.set(MultiAperture_.PREFS_CANCELED, "false");
        cancelled = false;
        previous = false;
        firstRun = true;
        if (this instanceof Stack_Aligner) {
            autoMode = false;
        }
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
            // Load imported aps
            apsX = Prefs.get("multiaperture.import.xapertures", "").split(",");
            xStored = extract(true, apsX);
            nImportedApStored = xStored == null ? 0 : xStored.length;
            apsY = Prefs.get("multiaperture.import.yapertures", "").split(",");
            yStored = extract(false, apsY);
            if (yStored == null || xStored == null || yStored.length == 0 || xStored.length != yStored.length) {
                nImportedApStored = 0;
            }
        }
        if (useMacroImage) {
            openImage = WindowManager.getImage(macroImageName);

            if (!(openImage == null)) {
                imp = openImage;
            }
        }

        if (imp.getWindow() instanceof AstroStackWindow) {
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

        imp.unlock();
        imp.setSlice(firstSlice);

        // START ESCAPE ABORTION POSSIBILITY
        IJ.resetEscape();

        // REGISTER FOR MOUSE CLICKS

        if (imp.getWindow() instanceof AstroStackWindow) {
            asw.showSlice(firstSlice);
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
        if ((apLoading.get().isPrevious() || previous) && (!useWCS || (useWCS && (raPosStored == null || decPosStored == null)))) {
            infoMessage = "Please select first aperture (right click to finalize) ...";
            IJ.showStatus(infoMessage);
        }
        setApertureColor(Color.green);
        setApertureName("T1");

//        imp.getWindow().requestFocus();
//        imp.getCanvas().requestFocusInWindow();

        if (radiusSetting == ApRadius.AUTO_VAR_FWHM || radiusSetting == ApRadius.AUTO_VAR_RAD_PROF) {
            oldRadii = new Seeing_Profile.ApRadii(radius, rBack1, rBack2);
        }

        if (apertureShape.get() == ApertureShape.CUSTOM_PIXEL) {
            if (apLoading.get().isPrevious()) {
                customPixelApertureHandler.loadAperturesFromPrefs(apLoading.get() == ApLoading.FIRST_PREVIOUS, apLoading.get() == ApLoading.IMPORTED);
                ngot = customPixelApertureHandler.apCount();
            }

            customPixelApertureHandler.setImp(imp);
            customPixelApertureHandler.setPlayCallback(this::runCustomAperture);
            customPixelApertureHandler.showControls();
            if (ac != null) {
                ac.setCustomPixelMode(true);
            }
        }

        if (runningWCSOnlyAlignment) {
            startProcessStack();
        } else if (autoMode) {
            mouseReleased(dummyClick);
        } else {
            imp.getWindow().requestFocus();
            imp.getCanvas().requestFocusInWindow();
            if (apLoading.get().isPrevious() && useWCS && hasWCS && raPosStored != null && decPosStored != null) {
                enterPressed = true;
                simulatedLeftClick = true;
                processSingleClick(dummyClick);
            }
        }

        asw.addWindowListener(this);
    }

    private void resetRadii() {
        Prefs.set("aperture.lastradius", radius);
        Prefs.set("aperture.lastrback1", rBack1);
        Prefs.set("aperture.lastrback2", rBack2);
        if (oldRadii == null) return;
        radius = oldRadii.r();
        rBack1 = oldRadii.r2();
        rBack2 = oldRadii.r3();
        Prefs.set("aperture.radius", radius);
        Prefs.set("aperture.rback1", rBack1);
        Prefs.set("aperture.rback2", rBack2);
    }

    protected void cancel() {
        storeLastRun();
        if (table != null) table.setLock(false);
        if (table != null) table.show();
        if (table != null) table.setLock(false);
        resetRadii();
        Prefs.set(MultiAperture_.PREFS_AUTOMODE, "false");
        Prefs.set(MultiAperture_.PREFS_FINISHED, "true");
        Prefs.set(MultiAperture_.PREFS_USEMACROIMAGE, "false");
        Prefs.set(MultiAperture_.PREFS_CANCELED, "true");
        Prefs.set("multiaperture.lastrun", lastRun);
        customPixelApertureHandler.closeControl();
        if (ac != null) {
            ac.setCustomPixelMode(false);
        }
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
        //previous = Prefs.get(MultiAperture_.PREFS_PREVIOUS, previous);
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
        enableLog = Prefs.get(PREFS_ENABLELOG, enableLog);
        referenceStar = (int) Prefs.get(PREFS_REFERENCESTAR, referenceStar);
        oldUseVarSizeAp = useVarSizeAp;
        ApRadius.AUTO_VAR_FWHM.cutoff = Prefs.get(MultiAperture_.PREFS_APFWHMFACTOR, ApRadius.AUTO_VAR_FWHM.cutoff);
        ApRadius.AUTO_FIXED_STACK_RAD.cutoff = Prefs.get(MultiAperture_.PREFS_APFWHMFACTORSTACK, ApRadius.AUTO_FIXED_STACK_RAD.cutoff);
        oldapFWHMFactor = ApRadius.AUTO_VAR_FWHM.cutoff;
        ApRadius.AUTO_VAR_RAD_PROF.cutoff = Prefs.get(MultiAperture_.PREFS_AUTOMODEFLUXCUTOFF, ApRadius.AUTO_VAR_RAD_PROF.cutoff);
        oldAutoModeFluxCutOff = ApRadius.AUTO_VAR_RAD_PROF.cutoff;
        ApRadius.AUTO_FIXED.cutoff = Prefs.get(PREFS_AUTOMODEFLUXCUTOFFFIXED, ApRadius.AUTO_FIXED.cutoff);
        try {
            radiusSetting = Enum.valueOf(ApRadius.class, Prefs.get(PREFS_APRADIUS, ApRadius.AUTO_FIXED.name()));
        } catch (Exception e) {
            //e.printStackTrace();
            radiusSetting = ApRadius.AUTO_FIXED;
        }
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

        switch (apertureShape.get()) {
            case CIRCULAR -> {
                if (apLoading.get().isPrevious()) {
                    // Load imported aps
                    if (apLoading.get() == ApLoading.IMPORTED) {
                        xOldApertures = Prefs.get("multiaperture.import.xapertures", "");
                        yOldApertures = Prefs.get("multiaperture.import.yapertures", "");
                        raOldApertures = Prefs.get("multiaperture.import.raapertures", "");
                        decOldApertures = Prefs.get("multiaperture.import.decapertures", "");
                        absMagOldApertures = Prefs.get("multiaperture.import.absmagapertures", "");
                        isOldRefStar = Prefs.get("multiaperture.import.isrefstar", "");
                        isOldAlignStar = Prefs.get("multiaperture.import.isalignstar", "");
                        oldCentroidStar = Prefs.get("multiaperture.import.centroidstar", "");
                    }

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
                    if (apLoading.get() == ApLoading.FIRST_PREVIOUS) {
                        nAperturesStored = 1;
                        centroidStarStored = Arrays.copyOfRange(centroidStarStored, 0, 1);
                        isAlignStarStored = Arrays.copyOfRange(isAlignStarStored, 0, 1);
                        isRefStarStored = Arrays.copyOfRange(isRefStarStored, 0, 1);
                        absMagStored = Arrays.copyOfRange(absMagStored, 0, 1);
                        if (decPosStored != null) {
                            decPosStored = Arrays.copyOfRange(decPosStored, 0, 1);
                        }
                        if (raPosStored != null) {
                            raPosStored = Arrays.copyOfRange(raPosStored, 0, 1);
                        }
                        yPosStored = Arrays.copyOfRange(yPosStored, 0, 1);
                        xPosStored = Arrays.copyOfRange(xPosStored, 0, 1);
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
            }
            case CUSTOM_PIXEL -> {

            }
        }

        imp.setSlice(firstSlice);
        ip = imp.getProcessor();
        imp.killRoi();
        return true;
    }

    public static void addApertureAsOld(double ra, double dec, double x, double y, boolean centroid) {
        var xS = Double.toString(x);
        var yS = Double.toString(y);

        addOption(MultiAperture_.PREFS_XAPERTURES, xS);
        addOption(MultiAperture_.PREFS_YAPERTURES, yS);
        addOption(MultiAperture_.PREFS_RAAPERTURES, uptoEightPlaces.format(ra));
        addOption(MultiAperture_.PREFS_DECAPERTURES, uptoEightPlaces.format(dec));
        addOption(MultiAperture_.PREFS_ABSMAGAPERTURES, Double.toString(0));
        addOption(MultiAperture_.PREFS_ISREFSTAR, "false");
        addOption(MultiAperture_.PREFS_ISALIGNSTAR, "false");
        addOption(MultiAperture_.PREFS_CENTROIDSTAR, Boolean.toString(centroid));
        addOption("multiaperture.import.xapertures", xS);
        addOption("multiaperture.import.yapertures", yS);
        addOption("multiaperture.import.raapertures", uptoEightPlaces.format(ra));
        addOption("multiaperture.import.decapertures", uptoEightPlaces.format(dec));
        addOption("multiaperture.import.isrefstar", Double.toString(0));
        addOption("multiaperture.import.centroidstar", Boolean.toString(centroid));
        addOption("multiaperture.import.isalignstar", "false");
        addOption("multiaperture.import.absmagapertures", Double.toString(0));
    }

    private static void addOption(String opt, String addition) {
        var old = Prefs.get(opt, "");
        /*if (!"".equals(old)) {
            addition = ","+addition;
        }*/
        Prefs.set(opt, /*old + */addition);
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
        if (imp.getWindow() instanceof AstroStackWindow) {
            asw = (AstroStackWindow) imp.getWindow();
            asw.setDisableShiftClick(false);
        }
    }

    /**
     * Finishes whole process.
     */
    public void shutDown() {
        // Reset drawing state
        if (imp.getCanvas() instanceof AstroCanvas a) {
            a.setPerformDraw(true);
        }
        storeLastRun();
        SwingUtilities.invokeLater(() -> {
            imp.setSlice(imp.getCurrentSlice());
            customPixelApertureHandler.closeControl();
        });
        noMoreInput();
        closeHelpPanel();
        if (ac != null) {
            ac.setCustomPixelMode(false);
        }
        Prefs.set("multiaperture.lastrun", lastRun);
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
            if (MultiPlot_.isRunning()) {
                //IJ.log("made it 1");
                if (MultiPlot_.getTable() != null && MultiPlot_.getTable().equals(table)) {
                    //IJ.log("made it 2");
                    MultiPlot_.updatePlot(MultiPlot_.updateAllFits(), slice == initialLastSlice);
                    //IJ.log("made it 2b");
                } else {
                    //IJ.log("made it 3");
                    MultiPlot_.setTable(table, false, slice == initialLastSlice);
                    //IJ.log("made it 3b");
                }
            } else {
                IJ.runPlugIn("Astronomy.MultiPlot_", tableName + ",true");  //",true" enables useAutoAstroDataUpdate
                //if (MultiPlot_.mainFrame != null && MultiPlot_.getTable() != null) {
                //IJ.log("made it 4");
                //    MultiPlot_.setTable(table, false, slice == initialLastSlice);
                //}
            }
        }
        cancelled = true;
        processingStack = false;
        processingImage = false;
        Prefs.set(MultiAperture_.PREFS_NAPERTURESMAX, nAperturesMax);
        //Prefs.set(MultiAperture_.PREFS_PREVIOUS, previous);
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
        Prefs.set(MultiAperture_.PREFS_APFWHMFACTOR, ApRadius.AUTO_VAR_FWHM.cutoff);
        Prefs.set(MultiAperture_.PREFS_APFWHMFACTORSTACK, ApRadius.AUTO_FIXED_STACK_RAD.cutoff);
        Prefs.set(MultiAperture_.PREFS_AUTOMODEFLUXCUTOFF, ApRadius.AUTO_VAR_RAD_PROF.cutoff);
        Prefs.set(MultiAperture_.PREFS_AUTOMODEFLUXCUTOFFFIXED, ApRadius.AUTO_FIXED.cutoff);
        Prefs.set(MultiAperture_.PREFS_APRADIUS, radiusSetting.name());

//        Prefs.set (MultiAperture_.PREFS_WIDETABLE, wideTable);
        Prefs.set(MultiAperture_.PREFS_SHOWRATIO, showRatio);
        Prefs.set(MultiAperture_.PREFS_SHOWCOMPTOT, showCompTot);
        Prefs.set(MultiAperture_.PREFS_SHOWRATIO_ERROR, showRatioError);
        Prefs.set(MultiAperture_.PREFS_SHOWRATIO_SNR, showRatioSNR);
        Prefs.set(MultiAperture_.PREFS_ENABLEDOUBLECLICKS, enableDoubleClicks);
//        Prefs.set (MultiAperture_.PREFS_FOLLOW, follow);
        Prefs.set(MultiAperture_.PREFS_AUTOMODE, "false");
        if (!(this instanceof Stack_Aligner)) {
            Prefs.set(MultiAperture_.PREFS_FINISHED, "true");
        }
        Prefs.set(MultiAperture_.PREFS_USEMACROIMAGE, "false");
        Prefs.set(MultiAperture_.PREFS_XLOCATION, xLocation);
        Prefs.set(MultiAperture_.PREFS_YLOCATION, yLocation);
        Prefs.set("plot2.absMagFrameLocationX", xAbsMagLocation);
        Prefs.set("plot2.absMagFrameLocationY", yAbsMagLocation);
        Prefs.set("plot2.helpFrameLocationX", helpFrameLocationX);
        Prefs.set("plot2.helpFrameLocationY", helpFrameLocationY);
        Prefs.set("aperture.lastradius", radius);
        Prefs.set("aperture.lastrback1", rBack1);
        Prefs.set("aperture.lastrback2", rBack2);
        if (!(Prefs.get(MultiAperture_.PREFS_AUTOMODE, autoMode) || Data_Processor.active)) resetRadii();

        if (MultiPlot_.isRunning()) {
            MultiPlot_.saveCompEnsemble();
        }
        asw.removeWindowListener(this);
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
                if (!adjustAperture(true, centroidStar[ap])) {
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
        if (!(apertureShape.get() == ApertureShape.CUSTOM_PIXEL && mouseDrag && !aperturesInitialized)) {
            asw.setMovingAperture(false);
        }

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
                    doubleClickTaskTimer = new Timer();
                    if ((modis & InputEvent.BUTTON1_MASK) != 0) {
                        doubleClickTaskTimer.schedule(doubleClickTask, 300);
                    } else {
                        doubleClickTaskTimer.schedule(doubleClickTask, 600);
                    }
                } catch (Exception eee) {
                    IJ.showMessage("Error starting double click timer task : " + eee.getMessage());
                }
            } else {
                doubleClick = true;
            }
        }
    }

    void processSingleClick(MouseEvent e) {
        if (suggestionRunning) return;

//        if (!enterPressed && !autoMode)
//            {
//            screenX = e.getX();
//            screenY = e.getY();
//            modis = e.getModifiers();
//            }

        if (apertureShape.get() == ApertureShape.CUSTOM_PIXEL) {
            customPixelApertureHandler.setPlayCallback(this::runCustomAperture);
            customPixelApertureHandler.setImp(imp);
            customPixelApertureHandler.currentAperture().setImage(imp);

            if (e != dummyClick && e != null && (!mouseDrag || e.isShiftDown())) {
                var x = canvas.offScreenX(e.getX());
                var y = canvas.offScreenY(e.getY());

                // Drag Paint
                if (mouseDrag && e.isShiftDown()) {
                    var x0 = Math.min((int) startDragX, x);
                    var x1 = Math.max((int) startDragX, x);
                    var y0 = Math.min((int) startDragY, y);
                    var y1 = Math.max((int) startDragY, y);
                    for (int i = x0; i <= x1; i++) {
                        for (int j = y0; j <= y1; j++) {
                            if (SwingUtilities.isLeftMouseButton(e)) {
                                customPixelApertureHandler.addPixel(i, j, e.isAltDown(), false);
                            } else {
                                customPixelApertureHandler.removePixel(i, j, false);
                            }
                        }
                    }
                    ocanvas.removeRoi("selectionRoi");
                    customPixelApertureHandler.currentAperture().update();
                } else { // Point Paint
                    if (SwingUtilities.isLeftMouseButton(e)) {
                        customPixelApertureHandler.addPixel(x, y, e.isAltDown());
                    } else {
                        customPixelApertureHandler.removePixel(x, y);
                    }
                }

                ngot = customPixelApertureHandler.apCount();

                ocanvas.add(customPixelApertureHandler.currentAperture());
                canvas.repaint();
            }

            //Right mouse click or <Enter> finalizes aperture selection
            if (enterPressed) {
                enterPressed = false;
                runCustomAperture();
            }

            return;
        }

        //Right mouse click or <Enter> finalizes aperture selection
        if (enterPressed || (!(e == dummyClick) && (!mouseDrag && (modis & InputEvent.BUTTON3_MASK) != 0 && !e.isShiftDown() && !e.isControlDown() && !e.isAltDown()))) {
            enterPressed = false;
            if (!aperturesInitialized) {
                if ((!(apLoading.get().isPrevious() || previous) && ngot > 0) || allStoredAperturesPlaced) {
                    nApertures = ngot;
                    simulatedLeftClick = true;
                    aperturesInitialized = true;
                }
            }
        }

        //do nothing unless automode or left mouse is clicked with no modifier keys pressed except "shift" or "alt"
        if (autoMode || simulatedLeftClick || (!mouseDrag &&
                                                       (modis & InputEvent.BUTTON1_MASK) != 0 && (!e.isControlDown() || e.isShiftDown()) && !e.isMetaDown())) {
            if (!autoMode && (apLoading.get().isPrevious() || previous) && !firstClick && !allStoredAperturesPlaced)  //ignore clicks while placing stored apertures
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
            if (!autoMode && !((apLoading.get().isPrevious() || previous) && firstClick) && ngot < nApertures) {
                apertureClicked = ocanvas.findApertureRoi(xCenter, yCenter, 0) != null;
            }

            // Autoradius feature
            if (!autoMode && firstClick && radiusSetting.autoRadius() && !t1Placed && !(this instanceof Stack_Aligner)) {
                oldRadii = new Seeing_Profile.ApRadii(radius, rBack1, rBack2);

                var x = xCenter;
                var y = yCenter;
                // Acount for old star positions
                if ((apLoading.get().isPrevious() || previous) && nAperturesStored > 0) {
                    x = xPosStored[0];
                    y = yPosStored[0];
                    var ra = -1000001d;
                    var dec = -1000001d;
                    if (raPosStored != null && decPosStored != null) {
                        ra = raPosStored[0];
                        dec = decPosStored[0];
                    }
                    if ((useMA || useAlign) && useWCS) {
                        if (!hasWCS || (!(ra < -1000000) && !(dec < -1000000))) {
                            if (hasWCS && ra > -1000000 && dec > -1000000) {
                                double[] xy = wcs.wcs2pixels(new double[]{ra, dec});
                                x = xy[0];
                                y = xy[1];
                            }
                        }
                    }
                }

                var rs = Seeing_Profile.getRadii(imp, x, y, ApRadius.AUTO_FIXED.cutoff);
                if (rs.isValid()) {
                    radius = rs.r();
                    rBack1 = rs.r2();
                    rBack2 = rs.r3();
                    Prefs.set("aperture.radius", radius);
                    Prefs.set("aperture.rback1", rBack1);
                    Prefs.set("aperture.rback2", rBack2);
                }
                // Don't focus on the seeing profile, allowing smoother access to continue MA
                asw.requestFocus();
                canvas.requestFocus();
            }

            // Auto stack radius feature
            if (!autoMode && firstClick && radiusSetting == ApRadius.AUTO_FIXED_STACK_RAD && !t1Placed && !(this instanceof Stack_Aligner)) {
                // Protect against clicking on stack while finding radii
                if (processingStackForRadii) {
                    return;
                }

                oldRadii = new Seeing_Profile.ApRadii(radius, rBack1, rBack2);
                var d = showWarning("Finding radii...");
                var warning = new AnnotateRoi(false, false, true, false, imp.getWidth() / 2f, imp.getHeight() / 2f, 2, "Finding radii...", Color.GREEN);
                warning.setImage(imp);

                // Show aperture on click

                var x = xCenter;
                var y = yCenter;
                var raPos = -1000001d;
                var decPos = -1000001d;

                // Account for old star positions
                if (apLoading.get().isPrevious() && nAperturesStored > 0) {
                    x = xPosStored[0];
                    y = yPosStored[0];

                    x += xCenter - x;
                    y += yCenter - y;

                    if (raPosStored != null && decPosStored != null) {
                        raPos = raPosStored[0];
                        decPos = decPosStored[0];
                    }
                    if ((useMA || useAlign) && useWCS) {
                        if (!hasWCS || (!(raPos < -1000000) && !(decPos < -1000000))) {
                            if (hasWCS && raPos > -1000000 && decPos > -1000000) {
                                double[] xy = wcs.wcs2pixels(new double[]{raPos, decPos});
                                x = xy[0];
                                y = xy[1];
                            }
                        }
                    }
                } else {
                    if (hasWCS) {
                        double[] radec = wcs.pixels2wcs(new double[]{xCenter, yCenter});
                        raPos = radec[0];
                        decPos = radec[1];
                    }
                }

                var oc = OverlayCanvas.getOverlayCanvas(imp);
                oc.clearRois();

                if (useWCS && asw.goodWCS && raPos > -1000000 && decPos > -1000000) {
                    double[] xy = asw.getWCS().wcs2pixels(new double[]{raPos, decPos});
                    x = xy[0];
                    y = xy[1];
                }

                var sp = new Seeing_Profile(true);

                var testRs = sp.getRadii(imp, x, y, ApRadius.AUTO_FIXED_STACK_RAD.cutoff, true, true);

                if (!testRs.centroidSuccessful()) {
                    testRs = new Seeing_Profile.ApRadii(radius, rBack1, rBack2, false);
                }

                x = sp.X0;
                y = sp.Y0;

                var ap = new ApertureRoi(x, y, testRs.r(), testRs.r2(), testRs.r3(), Double.NaN, testRs.centroidSuccessful());

                ap.setApColor(e != null && ((!e.isShiftDown() && ngot > 0) || (e.isShiftDown() && ngot == 0)) ? Color.RED : Color.GREEN);
                ap.setShowValues(false);
                ap.setImage(imp);
                oc.add(ap);
                oc.repaintOverlay();
                imp.updateAndDraw();

                // Make sure the aperture has time to draw
                waitForEventQueue();

                // Get radii
                ocanvas.add(warning);
                var rs = evaluateStackForRadii(e != null && ((!e.isShiftDown() && ngot > 0) || (e.isShiftDown() && ngot == 0)));
                ocanvas.removeRoi(warning);
                d.dispose();
                if (!rs.success()) {
                    showWarning("Failed to retrieve radii for %1$d/%2$d images.\nDo you wish to continue?".formatted(rs.count, lastSlice - firstSlice), true);
                }
            }

            // ADD APERTURE TO LIST OR SHIFT OLD APERTURE POSITIONS

            if (autoMode) {
                ngot = nApertures;
                if (!placeApertures(0, ngot - 1, ENABLECENTROID, CLEARROIS)) {
                    shutDown();
                    return;
                }
                updateApMags();
            } else if ((apLoading.get().isPrevious() || previous) && firstClick) {
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
                } else {
                    addApertureAsT1(e.isAltDown());
                }
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
                } else if (e.isAltDown()) {
                    toggleCentroidType();
                }
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
                //Prefs.set(MultiAperture_.PREFS_PREVIOUS, previous);
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
                    rightClickLabel.setText("");//"Cancel Stack Aligner" : "Cancel Multi-Aperture");
                    enterLabel.setText("");//"Cancel Stack Aligner" : "Cancel Multi-Aperture");
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

            if (!autoMode && suggestCompStars && tempSuggestCompStars && ngot >= referenceStar && refCount < maxSuggestedStars && !(this instanceof Stack_Aligner)) {
                suggestionRunning = true;
                var d = showWarning("Searching for comparison stars...");
                var warning = new AnnotateRoi(false, false, true, false, imp.getWidth() / 2f, imp.getHeight() / 2f, 2, "Searching for comparison stars...", Color.GREEN);
                warning.setImage(imp);
                ocanvas.add(warning);

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
                    IJ.beep();
                    g.showDialog();
                    if (g.wasCanceled()) {
                        cancelled = true;
                        ocanvas.removeRoi(warning);
                        d.dispose();
                        shutDown();
                    } else if (!g.wasOKed()) {
                        tempSuggestCompStars = false;
                        ocanvas.removeRoi(warning);
                        d.dispose();
                        return;
                    }
                }

                if (maxima.coordinateMaximas().isEmpty()) {
                    var g = new GenericSwingDialog("MA Automatic Comp. Star Selection");
                    g.addMessage("Number of maxima found is 0!\nProbable causes are that " +
                            "the Minimum peak threshold is set too high and/or the Maximum peak threshold is set " +
                            "too low. Ensure that the Minimum value set is above the highest sky background region " +
                            "and that the Maximum is below the saturation level.\n");
                    g.enableYesNoCancel("Continue Manual", "");
                    g.centerDialog(true);
                    g.disableNo();
                    g.getOkay().setToolTipText("Continue to manually place comparison star apertures.");
                    g.getCancel().setToolTipText("Cancel both automatic and manual aperture placement.");
                    IJ.beep();
                    g.showDialog();
                    if (g.wasCanceled()) {
                        ocanvas.removeRoi(warning);
                        d.dispose();
                        cancelled = true;
                        shutDown();
                    } else if (!g.wasOKed()) {
                        tempSuggestCompStars = false;
                        ocanvas.removeRoi(warning);
                        d.dispose();
                        return;
                    }
                }

                if (cancelled) return;

                if (enableLog)
                    AIJLogger.log("Number of maxima: " + NumberFormat.getInstance().format(maxima.coordinateMaximas().size()));
                if (enableLog) AIJLogger.log("Filtering...");
                var m = removeCloseStars(maxima.coordinateMaximas(), t1Source, maxP);
                if (cancelled) return;
                if (enableLog)
                    AIJLogger.log("Number of maxima that met distance and brightness thresholds: " + NumberFormat.getInstance().format(m.size()));
                if (enableLog) AIJLogger.log("Weighing peaks...");
                var set = weightAndLimitPeaks(m, t1Source);
                if (cancelled) return;

                if (set.isEmpty()) {
                    var g = new GenericSwingDialog("MA Automatic Comparison Star Selection");
                    g.addMessage("No comparison stars found that meet the brightness thresholds set.\n" +
                            "Check the brightness threshold settings in the Multi-Aperture set-up panel.\n");
                    g.enableYesNoCancel("Continue Manual", "");
                    g.centerDialog(true);
                    g.disableNo();
                    g.getOkay().setToolTipText("Continue to manually place comp. stars");
                    g.getCancel().setToolTipText("Cancel Multi-Aperture");
                    IJ.beep();
                    g.showDialog();
                    if (g.wasCanceled()) {
                        ocanvas.removeRoi(warning);
                        d.dispose();
                        cancelled = true;
                        shutDown();
                    } else if (!g.wasOKed()) {
                        tempSuggestCompStars = false;
                        ocanvas.removeRoi(warning);
                        d.dispose();
                        return;
                    }
                }

                if (!set.isEmpty()) {
                    if (enableLog) AIJLogger.log("Placing suggested comp. stars...");
                    for (WeightedCoordinateMaxima coordinateMaxima : set.subList(0, Math.min(maxSuggestedStars - refCount, set.size()))) {
                        if (cancelled) return;
                        if (enableLog) AIJLogger.log(ngot + 1);
                        if (enableLog)
                            AIJLogger.log(coordinateMaxima);//todo apertures placing in wrong coordinates for tica image, fine for others, due to wcs
                        xCenter = coordinateMaxima.cm.x();
                        yCenter = coordinateMaxima.cm.y();

                        addAperture(true, false);
                    }
                    if (enableLog) AIJLogger.log("Finished placing comp. stars!");
                }

                ocanvas.removeRoi(warning);
                d.dispose();

                tempSuggestCompStars = false; // Disable suggestion for all other stars
            }

            suggestionRunning = false;
        }
    }

    private void runCustomAperture() {
        if (customPixelApertureHandler.validateApertures()) {
            noMoreInput();
            customPixelApertureHandler.hideControls();
            nApertures = customPixelApertureHandler.apCount();
            for (int ap = 0; ap < nApertures; ap++) {
                isRefStar[ap] = customPixelApertureHandler.getAperture(ap).isComparisonStar();
                centroidStar[ap] = false;
            }
            aperturesInitialized = true;
            checkResultsTable();
            if (stackSize > 1 && doStack) {
                IJ.showStatus("Processing stack...");
                processingStack = true;
                startProcessStack();
            } else {
                IJ.showStatus("Processing image...");
                processImage();
            }
        }
    }

    private Output evaluateStackForRadii(boolean isComp) {
        processingStackForRadii = true;
        List<Seeing_Profile.ApRadii> radii = new ArrayList<>(lastSlice - firstSlice);
        var sp = new Seeing_Profile(true);
        //sp.setRoundRadii(false);
        imp.unlock();

        var x = xCenter;
        var y = yCenter;
        var raPos = -1000001d;
        var decPos = -1000001d;

        // Acount for old star positions
        if ((apLoading.get().isPrevious() || previous) && nAperturesStored > 0) {
            x = xPosStored[0];
            y = yPosStored[0];

            x += xCenter - x;
            y += yCenter - y;

            if (raPosStored != null && decPosStored != null) {
                raPos = raPosStored[0];
                decPos = decPosStored[0];
            }
            if ((useMA || useAlign) && useWCS) {
                if (!hasWCS || (!(raPos < -1000000) && !(decPos < -1000000))) {
                    if (hasWCS && raPos > -1000000 && decPos > -1000000) {
                        double[] xy = wcs.wcs2pixels(new double[]{raPos, decPos});
                        x = xy[0];
                        y = xy[1];
                    }
                }
            }
        } else {
            if (hasWCS) {
                double[] radec = wcs.pixels2wcs(new double[]{xCenter, yCenter});
                raPos = radec[0];
                decPos = radec[1];
            }
        }

        var hasErrored = 0;
        var oc = OverlayCanvas.getOverlayCanvas(asw.getImagePlus());
        if (!updateImageDisplay.get()) {
            if (imp.getCanvas() instanceof AstroCanvas a) {
                a.setPerformDraw(false);
            }
        }
        for (int i = firstSlice; i <= lastSlice; i++) {
            if (updateImageDisplay.get()) {
                asw.showSlice(i);
            } else {
                imp.setSliceWithoutUpdate(i);
            }

            if (useWCS) {
                asw.updateWCS();
            }
            oc.clearRois();

            if (useWCS && asw.goodWCS && raPos > -1000000 && decPos > -1000000) {
                double[] xy = asw.getWCS().wcs2pixels(new double[]{raPos, decPos});
                x = xy[0];
                y = xy[1];
            }

            var rs = sp.getRadii(imp, x, y, ApRadius.AUTO_FIXED_STACK_RAD.cutoff, true, true);
            if (cancelled) {
                imp.unlock();
                return new Output(rs.r(), rs.r2(), rs.r3(), 0);
            }
            if (!rs.centroidSuccessful()) {
                AIJLogger.log("Failed to centroid on slice: " + i + ".");
                hasErrored++;
                continue;
            }
            x = sp.X0;
            y = sp.Y0;

            var ap = new ApertureRoi(x, y, rs.r(), rs.r2(), rs.r3(), Double.NaN, true);
            ap.setApColor(isComp ? Color.RED : Color.GREEN);
            ap.setShowValues(false);
            ap.setImage(asw.getImagePlus());
            oc.add(ap);
            oc.repaint();

            radii.add(rs);
            IJ.showProgress(i / (float)(lastSlice - firstSlice));
        }
        if (imp.getCanvas() instanceof AstroCanvas a) {
            a.setPerformDraw(true);
        }
        imp.setSlice(firstSlice);
        imp.unlock();

        radii = radii.stream().filter(Seeing_Profile.ApRadii::isValid).toList();
        var sr = radii.stream().mapToDouble(Seeing_Profile.ApRadii::r).toArray();
        var br = radii.stream().mapToDouble(Seeing_Profile.ApRadii::r2).toArray();
        var br2 = radii.stream().mapToDouble(Seeing_Profile.ApRadii::r3).toArray();

        if (enableLog) {
            AIJLogger.multiLog("medians: ", upperMadMedian(sr), upperMadMedian(br), upperMadMedian(br2));
        }
        var rs = new Seeing_Profile.ApRadii(upperMadMedian(sr), upperMadMedian(br), upperMadMedian(br2));

        if (rs.isValid()) {
            radius = rs.r();
            rBack1 = rs.r2();
            rBack2 = rs.r3();
            Prefs.set("aperture.radius", radius);
            Prefs.set("aperture.rback1", rBack1);
            Prefs.set("aperture.rback2", rBack2);
        }

        // Clear and redraw aperture at initial location
        oc.clearRois();
        measureAperture();

        if (enableLog) {
            AIJLogger.multiLog("radii: ", rs);
        }

        sp.plot.setXYLabels("Slice", "Radius [px]");
        sp.plot.setLimits(0, lastSlice - firstSlice, 0, Math.max(ArrayUtil.max(br2), rBack2) + 5);
        sp.plot.setColor(Color.RED);
        sp.plot.setLineWidth(4);
        sp.plot.add("dot", sr);
        sp.plot.setLineWidth(2);
        sp.plot.drawLine(firstSlice, radius, lastSlice, radius);
        sp.plot.setColor(Color.GREEN);
        sp.plot.setLineWidth(4);
        sp.plot.add("dot", br);
        sp.plot.setLineWidth(2);
        sp.plot.drawLine(firstSlice, rBack1, lastSlice, rBack1);
        sp.plot.setColor(Color.BLUE);
        sp.plot.setLineWidth(4);
        sp.plot.add("dot", br2);
        sp.plot.setLineWidth(2);
        sp.plot.drawLine(firstSlice, rBack2, lastSlice, rBack2);

        sp.plot.addLegend("Aperture Radius\nInner Sky\nOuter Sky");


        sp.plot.addLabel(0, 0, "Final aperture: " + IJ.d2s(radius, 3) + " - " + IJ.d2s(rBack1, 3) + " - " + IJ.d2s(rBack2, 3) + " pixels");

        sp.plot.draw();
        sp.plot.getStack().prependPlot(sp.plot);

        sp.plot.show();
        var pw = sp.plot.getImagePlus().getWindow();
        var p = Prefs.getLocation("multiaperture.multisp.loc");
        if (p != null) {
            pw.setLocation(p);
        }
        pw.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                Prefs.saveLocation("multiaperture.multisp.loc", pw.getLocation());
            }
        });
        pw.setVisible(true);

        IJ.showProgress(1);

        processingStackForRadii = false;

        return new Output(rs.r(), rs.r2(), rs.r3(), hasErrored);
    }

    private double upperMadMedian(double[] a) {
        if (a.length == 1) {
            return a[0];
        } else if (a.length == 0) {
            return 0;
        }

        double med = ArrayUtil.median(a);
        var distanceAboveMed = Arrays.stream(a).filter(d -> d> med).map(d -> d - med).toArray();

        if (distanceAboveMed.length == 1) {
            return med + distanceAboveMed[0];
        } else if (distanceAboveMed.length == 0) {
            return med;
        }

        return med + ArrayUtil.median(distanceAboveMed);
    }

    private TreeSet<StarFinder.CoordinateMaxima> removeCloseStars(TreeSet<StarFinder.CoordinateMaxima> initialSet, double t1Source, double maxP) {
        final var radius2 = 4 * radius * radius;
        final var high = t1Source * (upperBrightness / 100d);
        final var low = t1Source * (lowerBrightness / 100d);
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
        return new WeightedCoordinateMaxima(coordinateMaxima, (brightness2DistanceWeight / 100d) * normBrightness + (1 - brightness2DistanceWeight / 100d) * normDistance);
    }

    private double distanceTo(double x1, double y1, double x2, double y2) {
        return Math.sqrt(squaredDistanceTo(x1, y1, x2, y2));
    }

    private double squaredDistanceTo(double x1, double y1, double x2, double y2) {
        final var h = x2 - x1;
        final var v = y2 - y1;
        return h * h + v * v;
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

        // Make sure WCS is updated
        asw.updateWCS();
        hasWCS = asw.hasWCS();
        if (hasWCS) wcs = asw.getWCS();

        var radii = new Seeing_Profile.ApRadii(radius, rBack1, rBack2);

        if (!autoMode && (apLoading.get().isPrevious() || previous) && firstClick && nAperturesStored > 0) {
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

            if (firstClick) {
                switch (radiusSetting) {
                    case AUTO_VAR_FWHM -> {
                        var x = adjustAperture(imp, xCenter, yCenter, radius, rBack1, rBack2, reposition);
                        var center = x.center();
                        var xFWHM = center.width();
                        var yFWHM = center.height();

                        radius = Math.max(xFWHM, yFWHM) * ApRadius.AUTO_VAR_FWHM.cutoff;
                        Prefs.set("aperture.radius", radius);
                        Prefs.set("aperture.rback1", rBack1);
                        Prefs.set("aperture.rback2", rBack2);
                    }
                    case AUTO_VAR_RAD_PROF -> {
                        var x = adjustAperture(imp, xCenter, yCenter, radius, rBack1, rBack2, reposition);
                        var center = x.center();
                        var rs = new Seeing_Profile(true).getRadii(imp, xPos[0], yPos[0], ApRadius.AUTO_VAR_RAD_PROF.cutoff, true, true);
                        if (rs.isValid()) {
                            radius = rs.r();
                            rBack1 = rs.r2();
                            rBack2 = rs.r3();
                            Prefs.set("aperture.radius", radius);
                            Prefs.set("aperture.rback1", rBack1);
                            Prefs.set("aperture.rback2", rBack2);
                        }
                    }
                }
            }

            //fine tune other aperture positions based on first ap position in auto mode or previous mode when not using WCS
            if ((autoMode || (apLoading.get().isPrevious() || previous) && firstClick) && centroidStar[0] && !((useMA || useAlign) && useWCS)) {
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

        Prefs.set("aperture.radius", radii.r());
        Prefs.set("aperture.rback1", radii.r2());
        Prefs.set("aperture.rback2", radii.r3());

        return true;
    }

    private JDialog showWarning(String message) {
        return showWarning(message, false);
    }

    private JDialog showWarning(String message, boolean hasProceed) {
        var gd = new GenericSwingDialog("MultiAperture Stack/Slice processor", imp.getWindow());
        gd.addMessage(message);
        gd.setModalityType(Dialog.ModalityType.MODELESS);

        if (hasProceed) {
            gd.enableYesNoCancel("Cancel", "Proceed");
            gd.getNo().addActionListener($ -> {
                gd.dispose();
            });
        } else {
            gd.disableNo();
        }

        gd.setOKLabel("Cancel");

        gd.getOkay().addActionListener($ -> {
            cancelled = true;
            cancel();
            shutDown();
        });

        gd.addKeyListener(new KeyListener() {
            @Override
            public void keyTyped(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
                    cancelled = true;
                    cancel();
                    shutDown();
                }
            }

            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
                    cancelled = true;
                    cancel();
                    shutDown();
                }
            }

            @Override
            public void keyReleased(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
                    cancelled = true;
                    cancel();
                    shutDown();
                }
            }
        });

        var l = imp.getWindow().getLocationOnScreen();
        gd.pack();
        l.translate(imp.getWindow().getSize().width / 2 - gd.getWidth()/2, imp.getWindow().getSize().height / 2 - gd.getHeight()/2);
        gd.setLocation(l);

        gd.setHideCancelButton(true);
        gd.showDialog();
        gd.setResizable(false);
        return gd;
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
        centroidStar[ngot] = altDown != Prefs.get("aperture.reposition", reposition);
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
        if (!getMags || !isRefStar[ap] || ((apLoading.get().isPrevious() || previous) && !allStoredAperturesPlaced)) {
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
            } else {
                gd.addMessage("Plate solution required for SIMBAD lookup");
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
        centroidStar[0] = altDown != Prefs.get("aperture.reposition", reposition);
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

        if (apertureShape.get() == ApertureShape.CUSTOM_PIXEL &&
                Math.abs(currentScreenX - startDragScreenX) + Math.abs(currentScreenY - startDragScreenY) >= 2.0 &&
                e.isShiftDown() && !aperturesInitialized) {
            ocanvas.removeRoi("selectionRoi");

            var x0 = ac.getNetFlipX() ? Math.max(startDragX, currentX) : Math.min(startDragX, currentX);
            var y0 = ac.getNetFlipY() ? Math.max(currentY, startDragY) : Math.min(currentY, startDragY);
            var xw = Math.abs(currentX - startDragX);
            var xh = Math.abs(currentY - startDragY);

            /*x0 = Math.round(x0);
            y0 = Math.round(y0);
            xw = Math.round(xw);
            xh = Math.round(xh);*/

            var roi = new AstroRoi(x0, y0, xw, xh);
            roi.setStrokeColor(Color.RED);
            roi.setName("selectionRoi");
            roi.setImage(imp);
            ocanvas.add(roi);
            ocanvas.repaint();
            asw.setMovingAperture(true);
        }

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
            asw.setMovingAperture((selectedApertureRoi != null && !aperturesInitialized) ||
                    (apertureShape.get() == ApertureShape.CUSTOM_PIXEL && e.isShiftDown() && !aperturesInitialized));
        }
    }

    public void mouseClicked(MouseEvent e) {
    }

    public void mouseExited(MouseEvent e) {
        IJ.showStatus(infoMessage);
    }

    public void mouseEntered(MouseEvent e) {
    }

    /**
     * Handle the key typed event from the image canvas.
     */
    public void keyTyped(KeyEvent e) {

    }

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


    //
    // MultiAperture_ METHODS
    //

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
        if (singleStep) {
            lastSlice = firstSlice;
        }
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
                if (gd.invalidNumber() || firstSlice < 1) {
                    firstSlice = 1;
                }
                //lastSlice = (int) gd.getNextNumber();
                if (gd.invalidNumber() || lastSlice > stackSize) {
                    lastSlice = stackSize;
                }
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
            if (oldradius != radius) {
                changeAperture();
            }
            //rBack1 = gd.getNextNumber();
            if (gd.invalidNumber() || rBack1 < radius) {
                IJ.beep();
                IJ.error("Invalid background inner radius: " + rBack1);
                return false;
            }
            if (oldrBack1 != rBack1) {
                changeAperture();
            }
            //rBack2 = gd.getNextNumber();
            if (gd.invalidNumber() || rBack2 < rBack1) {
                IJ.beep();
                IJ.error("Invalid background outer radius: " + rBack2);
                return false;
            }
            if (oldrBack2 != rBack2) {
                changeAperture();
            }
            if (singleStep) {
                lastSlice = firstSlice;
            }
//                oneTable = !gd.getNextBoolean();
//                wideTable = gd.getNextBoolean();

            //Prefs.set(MultiAperture_.PREFS_PREVIOUS, previous);
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
        Prefs.set(MultiAperture_.PREFS_APFWHMFACTOR, ApRadius.AUTO_VAR_FWHM.cutoff);
        Prefs.set(MultiAperture_.PREFS_AUTOMODEFLUXCUTOFF, ApRadius.AUTO_VAR_RAD_PROF.cutoff);
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
            stackTaskTimer = new Timer();
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
        stackRadii = new ArrayList<>();
        ocanvas = null;

        long timeStart = System.currentTimeMillis();
//        IJ.log("firstSlice="+firstSlice+"   lastSlice="+lastSlice);
        JDialog win = null;
        if (!updateImageDisplay.get()) {
            if (imp.getCanvas() instanceof AstroCanvas a) {
                a.setPerformDraw(false);
            }

            win = showWarning("Processing stack without image update, please wait.");
        }
        for (int i = firstSlice; i <= lastSlice; i++) {
            slice = i;
            imp.setSliceWithoutUpdate(i); //fixes scroll sync issue
            // Fixes scrollbar not updating on mac
            if (updateImageDisplay.get()) {
                waitForEventQueue();
            }
            if (starOverlay || skyOverlay || valueOverlay || nameOverlay) {
                ocanvas = OverlayCanvas.getOverlayCanvas(imp);
                canvas = ocanvas;
                ocanvas.clearRois();
            }
            if (imp.getWindow() instanceof AstroStackWindow) {
                asw = (AstroStackWindow) imp.getWindow();
                ac = (AstroCanvas) imp.getCanvas();

                if (updateImageDisplay.get()) {
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

                    waitForEventQueue();
                } else {
                    if (useWCS) {
                        asw.updateWCS();
                    }
                }

                hasWCS = asw.hasWCS();
                if (hasWCS) wcs = asw.getWCS();
                asw.setDisableShiftClick(true);
            } else {
                ac = null;
                asw = null;
                hasWCS = false;
                wcs = null;
            }
            ip = imp.getStack().getProcessor(slice);

            try {
                processImage();
            } catch (Exception e) {
                e.printStackTrace();
            }
            if (cancelled || IJ.escapePressed()) {
                IJ.beep();
                Prefs.set(MultiAperture_.PREFS_CANCELED, "true");
                shutDown();
                return;
            }
        }

        // Reset drawing state
        if (imp.getCanvas() instanceof AstroCanvas a) {
            a.setPerformDraw(true);
        }

        if (sp != null) {
            var sr = stackRadii.stream().mapToDouble(Seeing_Profile.ApRadii::r).toArray();
            var br = stackRadii.stream().mapToDouble(Seeing_Profile.ApRadii::r2).toArray();
            var br2 = stackRadii.stream().mapToDouble(Seeing_Profile.ApRadii::r3).toArray();

            var mr = ArrayUtil.median(sr);
            var mbr = ArrayUtil.median(br);
            var mbr2 = ArrayUtil.median(br2);

            sp.plot.setXYLabels("Slice", "Radius [px]");
            sp.plot.setLimits(0, lastSlice - firstSlice, 0, Math.max(ArrayUtil.max(br2), rBack2) + 5);
            sp.plot.setColor(Color.RED);
            sp.plot.setLineWidth(4);
            sp.plot.add("dot", sr);
            sp.plot.setLineWidth(2);
            sp.plot.drawLine(firstSlice, mr, lastSlice, mr);
            sp.plot.setColor(Color.GREEN);
            sp.plot.setLineWidth(4);
            sp.plot.add("dot", br);
            sp.plot.setLineWidth(2);
            sp.plot.drawLine(firstSlice, mbr, lastSlice, mbr);
            sp.plot.setColor(Color.BLUE);
            sp.plot.setLineWidth(4);
            sp.plot.add("dot", br2);
            sp.plot.setLineWidth(2);
            sp.plot.drawLine(firstSlice, mbr2, lastSlice, mbr2);

            sp.plot.addLegend("Aperture Radius\nInner Sky\nOuter Sky");

            sp.plot.addLabel(0, 0, "Median aperture: " + mr + " - " + mbr + " - " + mbr2 + " pixels");

            sp.plot.draw();
            sp.plot.getStack().prependPlot(sp.plot);

            sp.plot.show();
            var pw = sp.plot.getImagePlus().getWindow();
            var p = Prefs.getLocation("multiaperture.multisp.loc");
            if (p != null) {
                pw.setLocation(p);
            }
            pw.addWindowListener(new WindowAdapter() {
                @Override
                public void windowClosing(WindowEvent e) {
                    Prefs.saveLocation("multiaperture.multisp.loc", pw.getLocation());
                }
            });
            pw.setVisible(true);
        }

        if (processingStack) {
            if (win != null) {
                win.setVisible(false);
            }
            IJ.beep();
            shutDown();
            //AIJLogger.log("Multiaperture photometry took " + (System.currentTimeMillis() - timeStart) / 1000D + " seconds");
            IJ.showStatus("Multiaperture photometry took " + (System.currentTimeMillis() - timeStart) / 1000D + " seconds");
        }
    }

    private void storeLastRun() {
        lastRun = switch (apertureShape.get()) {
            case CIRCULAR -> switch (radiusSetting) {
                case FIXED -> {
                    yield "FA: %s-%s-%s".formatted(FORMAT.format(radius), FORMAT.format(rBack1),
                            FORMAT.format(rBack2));
                }
                case AUTO_FIXED -> {
                    yield "FAF%s: %s-%s-%s".formatted(ApRadius.AUTO_FIXED.cutoff, FORMAT.format(radius),
                            FORMAT.format(rBack1), FORMAT.format(rBack2));
                }
                case AUTO_FIXED_STACK_RAD -> {
                    yield "FAM%s: %s-%s-%s".formatted(ApRadius.AUTO_FIXED_STACK_RAD.cutoff, FORMAT.format(radius),
                            FORMAT.format(rBack1), FORMAT.format(rBack2));
                }
                case AUTO_VAR_RAD_PROF -> {
                    var sr = stackRadii.stream().mapToDouble(Seeing_Profile.ApRadii::r).toArray();
                    var br = stackRadii.stream().mapToDouble(Seeing_Profile.ApRadii::r2).toArray();
                    var br2 = stackRadii.stream().mapToDouble(Seeing_Profile.ApRadii::r3).toArray();

                    yield "VAR%s: %s-%s-%s".formatted(ApRadius.AUTO_VAR_RAD_PROF.cutoff, FORMAT.format(safeMedian(sr)),
                            FORMAT.format(safeMedian(br)), FORMAT.format(safeMedian(br2)));
                }
                case AUTO_VAR_FWHM -> {
                    var sr = stackRadii.stream().mapToDouble(Seeing_Profile.ApRadii::r).toArray();
                    var br = stackRadii.stream().mapToDouble(Seeing_Profile.ApRadii::r2).toArray();
                    var br2 = stackRadii.stream().mapToDouble(Seeing_Profile.ApRadii::r3).toArray();

                    yield "VAF%s: %s-%s-%s".formatted(ApRadius.AUTO_VAR_FWHM.cutoff, FORMAT.format(safeMedian(sr)),
                            FORMAT.format(safeMedian(br)), FORMAT.format(safeMedian(br2)));
                }
            };
            case CUSTOM_PIXEL -> "CA";
        };
    }

    private static double safeMedian(double[] a) {
        if (a.length < 1) {
            return Double.NaN;
        }

        return ArrayUtil.median(a);
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

        // Variable size aperture and reposition
        if (useVarSizeAp && apertureShape.get() == ApertureShape.CIRCULAR) {
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
                    if (useRadialProfile) {
                        if (radialDistribution(xCenter, yCenter, radius, back)) {
                            nRD++;
                            radiusRD += rRD;
                        }
                    }

                }
                xWidthFixed[ap] = xWidth;
                yWidthFixed[ap] = yWidth;
                widthFixed[ap] = ApRadius.AUTO_VAR_FWHM.cutoff != 0.0 ? 0.5 * (xWidth + yWidth) : fwhmRD;
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
            if (!useRadialProfile) {
                setVariableAperture(true, Math.max(xFWHM / nFWHM, yFWHM / nFWHM) * ApRadius.AUTO_VAR_FWHM.cutoff, ApRadius.AUTO_VAR_FWHM.cutoff, ApRadius.AUTO_VAR_RAD_PROF.cutoff);
                stackRadii.add(new Seeing_Profile.ApRadii(vradius, vrBack1, vrBack2));
            } else {
                if (sp == null) {
                    sp = new Seeing_Profile(true);
                    //sp.setRoundRadii(false);
                }
                double vRadSky = 0, vRadBack1 = 0, vRadBack2 = 0;
                nRD = 0;
                //for (int ap = 0; ap < nApertures; ap++) {
                var rs = sp.getRadii(imp, xPos[0], yPos[0], ApRadius.AUTO_VAR_RAD_PROF.cutoff, true, false);
                if (rs.isValid()) {
                    vRadSky += rs.r();
                    vRadBack1 += rs.r2();
                    vRadBack2 += rs.r3();
                    nRD++;
                    stackRadii.add(rs);
                }
                //}

                if (nRD == 0) {
                    var x = new Seeing_Profile(true);
                    rs= x.getRadii(imp, xPos[0], yPos[0], ApRadius.AUTO_VAR_RAD_PROF.cutoff, true, false);
                    if (rs.isValid()) {
                        vRadSky += rs.r();
                        vRadBack1 += rs.r2();
                        vRadBack2 += rs.r3();
                        nRD++;
                        stackRadii.add(rs);
                    } else {
                        vRadSky = radius;
                        vRadBack1 = rBack1;
                        vRadBack2 = rBack2;
                        nRD = 1;
                    }
                }

                useVariableAp = true;
                vradius = vRadSky / nRD;
                fwhmMult = 0;
                radialCutoff = ApRadius.AUTO_VAR_RAD_PROF.cutoff;
                vrBack1 = vRadBack1 / nRD;
                vrBack2 = vRadBack2 / nRD;
            }
        }

        // This is needed when running from DP for some reason, other rois are not removed
        OverlayCanvas.getOverlayCanvas(imp).removeApertureRois();

        nFWHM = 0;
        fwhmMean = 0.0;
        // Save new aperture position
        if (apertureShape.get() == ApertureShape.CIRCULAR) {
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
                    } else if (raPos[ap] <= -1000000 && decPos[ap] <= -1000000) {
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
        }

        // Photometry
        valueOverlay = Prefs.get(AP_PREFS_VALUEOVERLAY, valueOverlay);
        var hdr = FitsJ.getHeader(imp);
        for (int ap = 0; ap < nApertures; ap++) {
            boolean holdReposition = Prefs.get("aperture.reposition", reposition);

            if (apertureShape.get() == ApertureShape.CUSTOM_PIXEL) {
                if (!isRefStar[ap]) {
                    setApertureColor(Color.green);
                    setApertureName("T" + (ap + 1));
                    setAbsMag(targetAbsMag[ap]);
                } else {
                    setApertureColor(Color.red);
                    setApertureName("C" + (ap + 1));
                    setAbsMag(absMag[ap]);
                }

                if (!measureAperture(hdr, customPixelApertureHandler.getAperture(ap))) {
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
            } else {
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

                Prefs.set("aperture.reposition", centroidStar[ap]);
                setShowAsCentered(centroidStar[ap]);

                if (!measureAperture(hdr)) {
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

            suffix = (isRefStar[ap] ? "_C" : "_T") + (ap + 1);
            if (ap == 0) {
                storeResults();
            } else {
                storeAdditionalResults(ap);
            }

            if (apertureShape.get() == ApertureShape.CIRCULAR) {
                // FOLLOW MOTION FROM FRAME TO FRAME

                dx += xCenter - xPos[ap];
                dy += yCenter - yPos[ap];

                xOld[ap] = xPos[ap];
                yOld[ap] = yPos[ap];

                xPos[ap] = xCenter;        // STORE POSITION IN CASE IT DRIFTS WITHIN A STACK
                yPos[ap] = yCenter;

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
            }

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

            // FOR DAUGHTER CLASSES....

            noteOtherApertureProperty(ap);
        }

        // Increase chance of aperture actually rendering on mac
        if (updateImageDisplay.get()) {
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
        }

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
                    if (tot[ap] == 0) {
                        ratio[ap] = 0;
                    } else {
                        ratio[ap] /= tot[ap];
                    }
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
        measurementsWindow = MeasurementTable.getMeasurementsWindow(tableName);
        if (measurementsWindow != null) {
            measurementsWindow.scrollToBottom();
        }

        if (table != null && !isInstanceOfStackAlign && (updatePlot || Data_Processor.active)) {
            table.show();

            table.setLock(false);

            if ((updatePlot && !Data_Processor.active) || (Data_Processor.active && Data_Processor.runMultiPlot)) {
                if (MultiPlot_.isRunning()) {
                    if (MultiPlot_.getTable() != null && MultiPlot_.getTable().equals(table)) {
//                        IJ.log("update plot");
//                        while (MultiPlot_.updatePlotRunning)
//                                {
//                                IJ.log("waiting");
//                                IJ.wait(100);
//                                }
                        MultiPlot_.addTableData.accept(slice-firstSlice);
                        MultiPlot_.updatePlot(MultiPlot_.updateAllFits(), slice == initialLastSlice);
//                        IJ.log("update plot complete");
                    } else {
//                        IJ.log("setTable");
                        MultiPlot_.setTable(table, true);
                        MultiPlot_.addTableData.accept(0);
//                        IJ.log("setTable complete");
                    }
                } else {
                    IJ.runPlugIn("Astronomy.MultiPlot_", tableName);
                    if (MultiPlot_.isRunning() && MultiPlot_.getTable() != null) {
//                        IJ.log("setTable first time");
                        MultiPlot_.setTable(table, false);
//                        IJ.log("setTable first time complete");
                    }
                    MultiPlot_.addTableData.accept(0);
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
            if (!SwingUtilities.isEventDispatchThread()) {
                SwingUtilities.invokeAndWait(() -> {});
            }
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
                    if (means[bin] < ApRadius.AUTO_VAR_RAD_PROF.cutoff) {
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
        if (showMeanWidth && calcRadProFWHM) {
            table.addValue(AP_FWHM + header, fwhm, 6);
        }
        if (showMeanWidth) {
            table.addValue(AP_MEANWIDTH + header, width, 6);
        }
        if (showAngle) {
            table.addValue(AP_ANGLE + header, angle, 6);
        }
        if (showRoundness) {
            table.addValue(AP_ROUNDNESS + header, round, 6);
        }
        if (showVariance) {
            table.addValue(AP_VARIANCE + header, variance, 6);
        }

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
        if (MultiPlot_.isRunning() && plotTable != null && MeasurementTable.shorterName(plotTable.shortTitle()).equals("Measurements")) {
            table = plotTable;
        }

        measurementsWindow = MeasurementTable.getMeasurementsWindow(tableName);
        if (table != null && measurementsWindow != null && (table.getCounter() > 0 || (!updatePlot && !firstRun))) {
            return true;  //!autoMode && ()
        }
        firstRun = false;
        table = MeasurementTable.getTable(tableName);

        if (table == null) {
            IJ.error("Unable to open measurement table.");
            return false;
        }

        checkAndLockTable();

        int i = 0;

        measurementsWindow = MeasurementTable.getMeasurementsWindow(tableName);
//        IJ.log("setting up headings");
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
                if (!useRadialProfile) {
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
     * Standard preferences dialog for MultiAperture_
     */
    protected GenericSwingDialog dialog() {
        if (!Prefs.isLocationOnScreen(new Point(xLocation, yLocation))) {
            xLocation = 10;
            yLocation = 10;
        }

        GenericSwingDialog gd = new GenericSwingDialog("Multi-Aperture Measurements", xLocation, yLocation);
        gd.setSaveAndUseStepSize(true);

        var sliders = new JPanel[7];
        gd.addSwappableSection("Aperture Shape:", apertureShape, (g, shape) -> {
            switch (shape) {
                case CIRCULAR -> {
                    if (stackSize > 1) {
                        firstSlice = (firstSlice == stackSize || (alwaysstartatfirstSlice && !(this instanceof Stack_Aligner))) ? 1 : firstSlice;
                        sliders[0] = g.addSlider("First slice", 1, stackSize, firstSlice, d -> firstSlice = d.intValue());
                        sliders[1] = g.addSlider("Last slice", 1, stackSize, lastSlice, d -> lastSlice = d.intValue());
                    }
                    sliders[2] = g.addFloatSlider("Fixed/Base radius of photometric aperture", 0.01, radius > 100 ? radius : 100, false, radius, 3, 1.0, d -> radius = d);
                    sliders[3] = g.addFloatSlider("Fixed/Base radius of inner background annulus", 0.01, rBack1 > 100 ? rBack1 : 100, false, rBack1, 3, 1.0, d -> rBack1 = d);
                    sliders[4] = g.addFloatSlider("Fixed/Base radius of outer background annulus", 0.01, rBack2 > 100 ? rBack2 : 100, false, rBack2, 3, 1.0, d -> rBack2 = d);
                    g.addLineSeparator();
                    var apRadiiButtons = g.addRadioOptions(ApRadius.class, r -> MultiAperture_.radiusSetting = r, false);
                    g.addGenericComponent(apRadiiButtons.get(ApRadius.FIXED));
                    g.setOverridePosition(true);
                    g.addGenericComponent(apRadiiButtons.get(ApRadius.AUTO_FIXED));
                    g.addToSameRow();
                    g.setLeftInset(-100);
                    g.setNewPosition(GridBagConstraints.WEST);
                    g.addBoundedNumericField("Normalized flux cutoff threshold:", new GenericSwingDialog.Bounds(0, false, 1, false), ApRadius.AUTO_FIXED.cutoff, .01, 6, "(0 < cutoff < 1 ; default = 0.010)", d -> ApRadius.AUTO_FIXED.cutoff = d);
                    g.resetPositionOverride();
                    g.setLeftInset(20);
                    g.addGenericComponent(apRadiiButtons.get(ApRadius.AUTO_FIXED_STACK_RAD));
                    g.addToSameRow();
                    g.setLeftInset(-100);
                    g.setNewPosition(GridBagConstraints.WEST);
                    g.addBoundedNumericField("Normalized flux cutoff threshold:", new GenericSwingDialog.Bounds(0, false, 1, false), ApRadius.AUTO_FIXED_STACK_RAD.cutoff, .01, 6, "(0 < cutoff < 1 ; default = 0.010)", d -> ApRadius.AUTO_FIXED_STACK_RAD.cutoff = d);
                    g.resetPositionOverride();
                    g.addGenericComponent(apRadiiButtons.get(ApRadius.AUTO_VAR_RAD_PROF));
                    g.addToSameRow();
                    g.setLeftInset(-100);
                    g.setNewPosition(GridBagConstraints.WEST);
                    g.addBoundedNumericField("Normalized flux cutoff threshold:", new GenericSwingDialog.Bounds(0, false, 1, false), ApRadius.AUTO_VAR_RAD_PROF.cutoff, .01, 6, "(0 < cutoff < 1 ; default = 0.010)", d -> ApRadius.AUTO_VAR_RAD_PROF.cutoff = d);
                    g.setOverridePosition(false);
                    g.resetPositionOverride();
                    g.addGenericComponent(apRadiiButtons.get(ApRadius.AUTO_VAR_FWHM));
                    g.addToSameRow();
                    g.setOverridePosition(true);
                    g.setNewPosition(GridBagConstraints.EAST);
                    g.setRightInset(-230);
                    g.addFloatSlider("FWHM factor:", 0.1, 5.0, true, ApRadius.AUTO_VAR_FWHM.cutoff, 3, 0.1, d -> ApRadius.AUTO_VAR_FWHM.cutoff = d);
                    g.resetPositionOverride();
                    g.setOverridePosition(false);
                    apRadiiButtons.get(radiusSetting).setSelected(true);
                }
                case CUSTOM_PIXEL -> {
                    if (stackSize > 1) {
                        firstSlice = (firstSlice == stackSize || (alwaysstartatfirstSlice && !(this instanceof Stack_Aligner))) ? 1 : firstSlice;
                        g.setNewPosition(GridBagConstraints.CENTER);
                        sliders[5] = g.addSlider("First slice", 1, stackSize, firstSlice, d -> firstSlice = d.intValue());
                        g.setNewPosition(GridBagConstraints.CENTER);
                        sliders[6] = g.addSlider("Last slice", 1, stackSize, lastSlice, d -> lastSlice = d.intValue());
                    }
                }
            }
        }).setToolTipText("Select aperture type");

        // Sync slice sliders
        var fs1 = GenericSwingDialog.getSpinnerFromSlider(sliders[0]).get();
        var fs2 = GenericSwingDialog.getSpinnerFromSlider(sliders[5]).get();
        var ls1 = GenericSwingDialog.getSpinnerFromSlider(sliders[1]).get();
        var ls2 = GenericSwingDialog.getSpinnerFromSlider(sliders[6]).get();
        ChangeListener fcl = e -> {
            var source = (JSpinner) e.getSource();
            var value = source.getValue();
            if (source == fs1) {
                fs2.setValue(value);
            } else {
                fs1.setValue(value);
            }
        };
        ChangeListener lcl = e -> {
            var source = (JSpinner) e.getSource();
            var value = source.getValue();
            if (source == ls1) {
                ls2.setValue(value);
            } else {
                ls1.setValue(value);
            }
        };
        fs1.addChangeListener(fcl);
        fs2.addChangeListener(fcl);
        ls1.addChangeListener(lcl);
        ls2.addChangeListener(lcl);

        gd.addDoubleSpaceLineSeparator();

        var apLoadingButtons = gd.addRadioOptions(ApLoading.class, apLoading::set, true);
        for (ApLoading value : ApLoading.values()) {
            apLoadingButtons.get(value).setEnabled(value.isEnabled());
            if (value.isSelected()) {
                apLoadingButtons.get(value).setSelected(true);
            }
            if (!apLoadingButtons.get(value).isEnabled() && apLoadingButtons.get(value).isSelected()) {
                apLoadingButtons.get(value).setSelected(true);
                apLoading.set(ApLoading.ALL_NEW);
            }
        }

        // Swap button text for next shape
        apertureShape.addListener((key, val) -> {
            for (ApLoading value : ApLoading.values()) {
                apLoadingButtons.get(value).setEnabled(value.isEnabled());
                if (value.isSelected()) {
                    apLoadingButtons.get(value).setSelected(true);
                }
                if (!apLoadingButtons.get(value).isEnabled() && apLoadingButtons.get(value).isSelected()) {
                    apLoadingButtons.get(value).setSelected(true);
                    apLoading.set(ApLoading.ALL_NEW);
                }
                apLoadingButtons.get(value).setText(value.optionText());
            }
        });

        gd.addLineSeparator();

        gd.addNewSwappableSectionPanel(ApertureShape.class, (d, shape) -> {
            switch (shape) {
                case CIRCULAR -> {
                    gd.addCheckbox("Use RA/Dec to locate aperture positions", useWCS, b -> useWCS = b)
                            .setToolTipText("<html>If enabled, apertures will first be placed according to their RA and DEC location.<br>"+
                                    "If centroid is also enabled for an aperture, the centroid operation will start from the RA and Dec position.<br>"+
                                    "If 'Halt on WCS error' below is disabled, mixed mode RA-Dec and X-Y placement is possible.<br>" +
                                    "Mixed-mode is useful if plate solving is slow. In this mode, only the first image and any subsequent image<br>"+
                                    "with a large shift on the detector, such as a meridian flip, need to be plate solved.</html>");
                    gd.addCheckbox("Use single step mode (1-click to set first aperture location in each image)", singleStep, b -> {
                                singleStep = b;
                                singleStepListeners.forEach(c -> {
                                    if ("with".equals(c.getName())) {
                                        c.setEnabled(b);
                                    } else {
                                        c.setEnabled(!b);
                                    }
                                });
                                if (b) {
                                    updateImageDisplay.set(true);
                                }
                            })
                            .setToolTipText("<html>Single step mode allows apertures to be placed in the first image, and then after each image is processed,<br>"+
                                    "Multi-Aperture will pause to allow the user to click near the T1 star location in the next image. This mode of operation is useful with<br>"+
                                    "image sequences that are not plate solved and that have image shifts too large for centroid to track.</html>");
                    var c1 = gd.addCheckbox("Allow aperture changes between slices in single step mode (right click to advance image)", allowSingleStepApChanges, b -> {
                        allowSingleStepApChanges = b;
                    });
                    c1.setName("with");
                    c1.setToolTipText("<html>This mode works the same as Single Step mode, except that Multi-Aperture does not automatically advance<br>"+
                            "to the next image when the T1 location is selected. Instead, the user has the choice to tweak the aperture locations<br>" +
                            "using drag and drop. After the new locations are optionally set, press Enter or right-click to advance to the next image.<br>" +
                            "This mode can be helpful for image sequences with moving objects, or apertures that jump to incorrect stars.</html>");
                    singleStepListeners.add(c1);
                    c1.setEnabled(singleStep);
                    gd.addDoubleSpaceLineSeparator();
                }
                case CUSTOM_PIXEL -> {
                }
            }
        });

        // Make all sliders the same size
        var sliderWidth = Math.max(GenericSwingDialog.getSliderWidth(sliders[2]), Integer.toString(stackSize).length());
        for (JPanel slider : sliders) {
            GenericSwingDialog.setSliderSpinnerColumns(slider, sliderWidth);
        }

        // HERE ARE THE THINGS WHICH AREN'T ABSOLUTELY NECESSARY
        addFancyDialog(gd);

        gd.addNewSwappableSectionPanel(ApertureShape.class, (d, shape) -> {
            switch (shape) {
                case CIRCULAR -> {
                    d.addMessage("CLICK 'PLACE APERTURES' AND SELECT APERTURE LOCATIONS WITH LEFT CLICKS.\nTHEN RIGHT CLICK or <ENTER> TO BEGIN PROCESSING.\n(to abort aperture selection or processing, press <ESC>)");
                }
                case CUSTOM_PIXEL -> {
                    d.addMessage("FOLLOW INSTRUCTIONS ON NEXT PANEL TO PROCEED");
                }
            }
        });

        if (!(this instanceof Stack_Aligner)) gd.enableYesNoCancel("Place Apertures", "Aperture Settings");
        return gd;
    }


    /**
     * Add aperture number to overlay display.
     */
//	protected void addStringRoi (double x, double y, String text)
//		{
//		super.addStringRoi (x,y,"  #"+(aperture+1)+":  "+text.trim());
//		}

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

        gd.addNewSwappableSectionPanel(ApertureShape.class, (g, shape) -> {
            switch (shape) {
                case CIRCULAR -> {
                    var suggestionComponents = new LinkedHashSet<Component>();

                    var s = gd.addCheckboxGroup(1, 3, new String[]{"Auto comparison stars", "Enable log", "Show peaks"}, new boolean[]{suggestCompStars, enableLog, debugAp}, listb);
                    var boxes = s.subComponents();
                    ((JComponent) boxes.get(0)).setToolTipText("If enabled, uses the following settings to generate a set of comparison stars based on the star in the specified Base Aperture below.");
                    ((JComponent) boxes.get(1)).setToolTipText("Enable log output for comparison star selection.");
                    ((JComponent) boxes.get(2)).setToolTipText("Draw dummy apertures to indicate image peaks that comp. star selection is considering.");

                    final var gauss = gd.addBoundedNumericField("Smoothing Filter Radius", new GenericSwingDialog.Bounds(0, Double.MAX_VALUE), gaussRadius, 1, 10, "pixels", d -> gaussRadius = d);
                    gauss.asSwingComponent(C1).setToolTipText("Radius of gaussian smoothing to use when finding initial peaks.\n Set to 1 to disable.");
                    suggestionComponents.add(gauss.c2());
                    suggestionComponents.add(gauss.c1());

                    var autoPeaks = gd.addCheckbox("Auto Thresholds", autoPeakValues, b -> autoPeakValues = b);
                    g.addToSameRow();
                    var maxPeak = gd.addBoundedNumericField("Max. Peak Value", new GenericSwingDialog.Bounds(0, Double.MAX_VALUE), maxPeakValue, 1, columns, null, d -> maxPeakValue = d);
                    g.addToSameRow();
                    var minPeak = gd.addBoundedNumericField("Min. Peak Value", new GenericSwingDialog.Bounds(0, Double.MAX_VALUE), minPeakValue, 1, columns, null, d -> minPeakValue = d);

                    autoPeaks.setToolTipText("When enabled, set peak thresholds based on image statistics.\nMax = 0.9 * Max Pixel Value, Min = Mean Pixel Value + 1σ.");
                    maxPeak.asSwingComponent(C1).setToolTipText("Maximum peak value to consider");
                    minPeak.asSwingComponent(C1).setToolTipText("Minimum peak value to consider");

                    final var liveStats = asw.getLiveStatistics();
                    var minP = liveStats.mean + (1 * liveStats.stdDev);
                    var maxP = liveStats.max * 0.9;
                    DecimalFormat fourPlaces = new DecimalFormat("###,##0.00", IJU.dfs);
                    DecimalFormat scientificFourPlaces = new DecimalFormat("0.####E00", IJU.dfs);
                    if (autoPeakValues) {
                        GenericSwingDialog.getTextFieldFromSpinner((JSpinner) minPeak.c1()).ifPresent(tf -> tf.setText(minP < 1000000.0 ? fourPlaces.format(minP) : scientificFourPlaces.format(minP)));
                        GenericSwingDialog.getTextFieldFromSpinner((JSpinner) maxPeak.c1()).ifPresent(tf -> tf.setText(maxP < 1000000.0 ? fourPlaces.format(maxP) : scientificFourPlaces.format(maxP)));
                    } else {
                        GenericSwingDialog.getTextFieldFromSpinner((JSpinner) minPeak.c1()).ifPresent(tf -> tf.setText(minPeakValue < 1000000.0 ? fourPlaces.format(minPeakValue) : scientificFourPlaces.format(minPeakValue)));
                        GenericSwingDialog.getTextFieldFromSpinner((JSpinner) maxPeak.c1()).ifPresent(tf -> tf.setText(maxPeakValue < 1000000.0 ? fourPlaces.format(maxPeakValue) : scientificFourPlaces.format(maxPeakValue)));
                    }

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

                    autoPeaks.addActionListener($ -> {
                        var minP1 = liveStats.mean + (1 * liveStats.stdDev);
                        var maxP1 = liveStats.max * 0.9;
                        minPeak.c1().setEnabled(autoPeakValues && suggestCompStars);
                        maxPeak.c1().setEnabled(autoPeakValues && suggestCompStars);
                        if (!autoPeakValues) {
                            GenericSwingDialog.getTextFieldFromSpinner((JSpinner) minPeak.c1()).ifPresent(tf -> tf.setText(minP < 1000000.0 ? fourPlaces.format(minP1) : scientificFourPlaces.format(minP1)));
                            GenericSwingDialog.getTextFieldFromSpinner((JSpinner) maxPeak.c1()).ifPresent(tf -> tf.setText(maxP < 1000000.0 ? fourPlaces.format(maxP1) : scientificFourPlaces.format(maxP1)));
                        } else {
                            GenericSwingDialog.getTextFieldFromSpinner((JSpinner) minPeak.c1()).ifPresent(tf -> tf.setText(minPeakValue < 1000000.0 ? fourPlaces.format(minPeakValue) : scientificFourPlaces.format(minPeakValue)));
                            GenericSwingDialog.getTextFieldFromSpinner((JSpinner) maxPeak.c1()).ifPresent(tf -> tf.setText(maxPeakValue < 1000000.0 ? fourPlaces.format(maxPeakValue) : scientificFourPlaces.format(maxPeakValue)));
                        }
                    });

                    minPeak.c1().setEnabled((!autoPeakValues && suggestCompStars));
                    maxPeak.c1().setEnabled((!autoPeakValues && suggestCompStars));

                    suggestionComponents.add(autoPeaks);
                    suggestionComponents.add(minPeak.c2());
                    suggestionComponents.add(maxPeak.c2());

                    ((JCheckBox) s.subComponents().getFirst()).addActionListener($ -> {
                        toggleComponents(suggestionComponents, !suggestCompStars);
                        var minP1 = liveStats.mean + (1 * liveStats.stdDev);
                        var maxP1 = liveStats.max * 0.9;
                        minPeak.c1().setEnabled(!autoPeakValues && !suggestCompStars);
                        maxPeak.c1().setEnabled(!autoPeakValues && !suggestCompStars);
                        if (autoPeakValues) {
                            GenericSwingDialog.getTextFieldFromSpinner((JSpinner) minPeak.c1()).ifPresent(tf -> tf.setText(minP < 1000000.0 ? fourPlaces.format(minP1) : scientificFourPlaces.format(minP1)));
                            GenericSwingDialog.getTextFieldFromSpinner((JSpinner) maxPeak.c1()).ifPresent(tf -> tf.setText(maxP < 1000000.0 ? fourPlaces.format(maxP1) : scientificFourPlaces.format(maxP1)));
                        } else {
                            GenericSwingDialog.getTextFieldFromSpinner((JSpinner) minPeak.c1()).ifPresent(tf -> tf.setText(minPeakValue < 1000000.0 ? fourPlaces.format(minPeakValue) : scientificFourPlaces.format(minPeakValue)));
                            GenericSwingDialog.getTextFieldFromSpinner((JSpinner) maxPeak.c1()).ifPresent(tf -> tf.setText(maxPeakValue < 1000000.0 ? fourPlaces.format(maxPeakValue) : scientificFourPlaces.format(maxPeakValue)));
                        }
                    });

                    var starSelection = gd.addBoundedNumericField("Base Aperture", new GenericSwingDialog.Bounds(1, Double.MAX_VALUE), referenceStar, 1, 7, null, true, d -> referenceStar = d.intValue());
                    if (upperBrightness < 101.0) upperBrightness = 150.0;
                    if (lowerBrightness > 99.0 || lowerBrightness < 0.0) lowerBrightness = 50.0;
                    g.addToSameRow();
                    var maxDBrightness = gd.addBoundedNumericField("Max. Comp. Brightness %", new GenericSwingDialog.Bounds(101, Double.MAX_VALUE), upperBrightness, 1, columns, null, d -> upperBrightness = d);
                    g.addToSameRow();
                    var minDBrightness = gd.addBoundedNumericField("Min. Comp. Brightness %", new GenericSwingDialog.Bounds(0, 99), lowerBrightness, 1, columns, null, d -> lowerBrightness = d);

                    suggestionComponents.add(starSelection.c2());
                    suggestionComponents.add(maxDBrightness.c2());
                    suggestionComponents.add(minDBrightness.c2());

                    starSelection.asSwingComponent(C1).setToolTipText("The aperture to base comparison star selection on.");
                    maxDBrightness.asSwingComponent(C1).setToolTipText("Upper brightness limit of comp stars relative to the base aperture brightness");
                    minDBrightness.asSwingComponent(C1).setToolTipText("Lower brightness limit of comp stars relative to the base aperture brightness");

                    var distanceRatioBox = Box.createHorizontalBox();
                    var ratioSlider = new JSlider(0, 100, (int) brightness2DistanceWeight);

                    ratioSlider.addChangeListener($ -> brightness2DistanceWeight = ratioSlider.getValue());

                    distanceRatioBox.add(new JLabel("Weight of Distance "));
                    distanceRatioBox.add(ratioSlider);
                    distanceRatioBox.add(new JLabel(" vs Brightness"));
                    //distanceRatioBox.setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY));

                    g.setWidth(2);
                    gd.addGenericComponent(distanceRatioBox);
                    g.addToSameRow();
                    var maxStars = gd.addBoundedNumericField("Max. Comp. Stars", new GenericSwingDialog.Bounds(0, Double.MAX_VALUE), maxSuggestedStars, 1, columns, null, true, d -> maxSuggestedStars = d.intValue());

                    suggestionComponents.add(ratioSlider);
                    suggestionComponents.add(maxStars.c2());

                    ratioSlider.setToolTipText("""
                <html>Weight of brightness vs distance, used to sort stars.<br>
                Based on normalized Source-Sky brightness and distance relative to the specified base aperture.<br>
                A value of 100 makes the weighting based entirely on the normalized brightness.<br>
                A value of 0 makes the weighting entirely based on proximity to the specified base aperture.<br>
                If more stars were found than the maximum requested, the stars with the highest weights are used.</html>""");

                    maxStars.asSwingComponent(C1).setToolTipText("Maximum number of comparison stars to select. Includes already selected comp. stars in its count");

                    suggestionComponents.addAll(s.subComponents().subList(1, s.subComponents().size()));

                    toggleComponents(suggestionComponents, suggestCompStars);

                    gd.addDoubleSpaceLineSeparator();
                }
                case CUSTOM_PIXEL -> {
                }
            }
        });

        // GET NON-REQUIRED DIALOGUE FIELDS:
        gd.addNewSwappableSectionPanel(ApertureShape.class, (d, shape) -> {
            switch (shape) {
                case CIRCULAR -> {
                    var penultimateBoxes = d.addCheckboxGroup(2, 2, new String[]{"Centroid apertures (initial setting)", "Halt processing on WCS or centroid error",
                                    "Remove stars from background", "Assume background is a plane"},
                            new boolean[]{reposition, haltOnError, removeBackStars, backIsPlane}, list1);

                    penultimateBoxes.subComponents().get(0).setToolTipText("<html>Enable to set the initial state of the 'Centroid' icon when aperture placement starts.<br>" +
                            "NOTE: The centroid setting of 'Previous apertures' will not be affected by this setting.<br>" +
                            "The centroid setting of 'Previous apertures' can be changed by an alt-left-click inside the aperture.</html>");
                    penultimateBoxes.subComponents().get(1).setToolTipText("Enable to halt Multi-aperture if centroid fails, or if 'Use RA-Dec' is selected, but an image is not plate solved.");
                    penultimateBoxes.subComponents().get(2).setToolTipText("<html>Enable to use an interative 2-sigma outlier removal technique to ignore pixels in the background region<br>"+
                            "that are statistically brighter or darker than other pixels. This mode is generally recommended and<br>"+
                            "effectively causes Multi-aperture to ignore pixels containing stars within the background region.</html>");
                    penultimateBoxes.subComponents().get(3).setToolTipText("Enable to fit a plane to the background pixels to attempt to account for large background gradients in the images.");
                }
                case CUSTOM_PIXEL -> {
                    var penultimateBoxes = d.addCheckboxGroup(1, 2, new String[]{"Remove stars from background", "Assume background is a plane"},
                            new boolean[]{removeBackStars, backIsPlane}, list1.subList(2, 4));
                    penultimateBoxes.subComponents().get(0).setToolTipText("<html>Enable to use an interative 2-sigma outlier removal technique to ignore pixels in the background region<br>"+
                            "that are statistically brighter or darker than other pixels. This mode is generally recommended and<br>"+
                            "effectively causes Multi-aperture to ignore pixels containing stars within the background region.</html>");
                    penultimateBoxes.subComponents().get(1).setToolTipText("Enable to fit a plane to the background pixels to attempt to account for large background gradients in the images.");
                }
            }
        });

        gd.addDoubleSpaceLineSeparator();

        gd.addNewSwappableSectionPanel(ApertureShape.class, (d, shape) -> {
            switch (shape) {
                case CIRCULAR -> {
                    d.addCheckbox("Prompt to enter ref star apparent magnitude (required if target star apparent mag is desired)", getMags, b -> getMags = b)
                            .setToolTipText("Apparent magntiudes are not needed for standard differential photometry.");
                    final var list2 = new ArrayList<Consumer<Boolean>>();
                    list2.add(b -> updatePlot = b);
                    list2.add(b -> showHelp = b);
                    list2.add(updateImageDisplay::set);
                    var bottomChecks = d.addCheckboxGroup(2, 2, new String[]{"Update plot while running", "Show help panel during aperture selection", "Update image display while running"},
                            new boolean[]{updatePlot, showHelp, updateImageDisplay.get()}, list2);
                    bottomChecks.subComponents().get(0).setToolTipText("<html>Multi-aperture will run faster with this option disabled,<br>" +
                            "but the plot displays will only update once when the Multi-Aperture run has finished.</html>");
                    bottomChecks.subComponents().get(1).setToolTipText("This extra panel is useful to new users that need additional keyboard/mouse help when placing apertures.");
                    singleStepListeners.add(bottomChecks.subComponents().get(2));
                    bottomChecks.subComponents().get(2).setEnabled(!singleStep);
                }
                case CUSTOM_PIXEL -> {
                    final var list2 = new ArrayList<Consumer<Boolean>>();
                    list2.add(b -> updatePlot = b);
                    list2.add(updateImageDisplay::set);
                    var bottomChecks = d.addCheckboxGroup(1, 2, new String[]{"Update plot while running", "Update image display while running"},
                            new boolean[]{updatePlot, updateImageDisplay.get()}, list2);
                    bottomChecks.subComponents().get(0).setToolTipText("<html>Multi-aperture will run faster with this option disabled,<br>" +
                            "but the plot displays will only update once when the Multi-Aperture run has finished.</html>");
                    singleStepListeners.add(bottomChecks.subComponents().get(1));
                    bottomChecks.subComponents().get(1).setEnabled(!singleStep);
                }
            }
        });
    }

    private void toggleComponents(Component[] components, int offset, boolean toggle) {
        for (int i = offset; i < components.length; i++) {
            if (components[i] == null) continue;
            components[i].setEnabled(toggle);
            if (components[i] instanceof Container) {
                for (Component child : ((Container) components[i]).getComponents()) {
                    setEnabled(child, toggle);
                }
            }
            if (components[i] instanceof Panel panel) {
                toggleComponents(panel.getComponents(), 0, toggle);
            } else if (components[i] instanceof JSpinner spinner) {
                toggleComponents(spinner.getComponents(), 0, toggle);
            } else if (components[i] instanceof JSlider slider) {
                toggleComponents(slider.getComponents(), 0, toggle);
            } else if (components[i] instanceof JSpinner.DefaultEditor editor) {
                AIJLogger.log(1);
                editor.getSpinner().setEnabled(toggle);
                editor.getTextField().setEnabled(toggle);
            }
            components[i].setEnabled(toggle);
        }
    }

    void setEnabled(Component component, boolean enabled) {
        component.setEnabled(enabled);
        if (component instanceof Container) {
            for (Component child : ((Container) component).getComponents()) {
                setEnabled(child, enabled);
            }
        }
    }

    private void toggleComponents(Collection<Component> components, boolean toggle) {
        for (Component component : components) {
            component.setEnabled(toggle);
        }
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
        if (ApRadius.AUTO_VAR_FWHM.cutoff < 0) {
            IJ.beep();
            IJ.error("Invalid aperture FWHM factor entered");
            return false;
        }
        if (ApRadius.AUTO_VAR_RAD_PROF.cutoff <= 0 || ApRadius.AUTO_VAR_RAD_PROF.cutoff >= 1) {
            IJ.beep();
            IJ.error("Invalid flux cutoff entered");
            return false;
        }

        useRadialProfile = radiusSetting == ApRadius.AUTO_VAR_RAD_PROF;
        useVarSizeAp = radiusSetting == ApRadius.AUTO_VAR_FWHM || radiusSetting == ApRadius.AUTO_VAR_RAD_PROF;

        if (oldUseVarSizeAp != useVarSizeAp || oldapFWHMFactor != ApRadius.AUTO_VAR_FWHM.cutoff || oldAutoModeFluxCutOff != ApRadius.AUTO_VAR_RAD_PROF.cutoff ||
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
        Prefs.set(MultiAperture_.PREFS_APFWHMFACTOR, ApRadius.AUTO_VAR_FWHM.cutoff);
        Prefs.set(MultiAperture_.PREFS_APFWHMFACTORSTACK, ApRadius.AUTO_FIXED_STACK_RAD.cutoff);
        Prefs.set(MultiAperture_.PREFS_AUTOMODEFLUXCUTOFF, ApRadius.AUTO_VAR_RAD_PROF.cutoff);
        Prefs.set(MultiAperture_.PREFS_AUTOMODEFLUXCUTOFFFIXED, ApRadius.AUTO_FIXED.cutoff);
        Prefs.set(MultiAperture_.PREFS_APRADIUS, radiusSetting.name());
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
        Prefs.set(PREFS_REFERENCESTAR, referenceStar);
        Prefs.set(PREFS_ENABLELOG, enableLog);
        Prefs.set(MultiAperture_.PREFS_USEVARSIZEAP, useVarSizeAp);
        Prefs.savePreferences();

        if (!(this instanceof Stack_Aligner) && !gd.wasOKed()) {
            cancel();
            gd.dispose();
            Executors.newSingleThreadExecutor().submit(() -> {IJ.runPlugIn("Astronomy.Set_Aperture", "from_MA");});
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
        helpFrame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        helpFrame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                closeHelpPanel();
            }
        });
        JLabel leftClickName = new JLabel("left-click:");
        leftClickName.setHorizontalAlignment(JLabel.RIGHT);
        helpPanel.add(leftClickName);
        if ((apLoading.get().isPrevious() || previous)) {
            leftClickLabel = new JLabel("Add previous stored apertures by clicking on the star corresponding to T1/C1");
        } else {
            leftClickLabel = new JLabel("Add target star aperture T1");
        }
        helpPanel.add(leftClickLabel);

        JLabel shiftLeftClickName = new JLabel("<Shift>left-click:");
        shiftLeftClickName.setHorizontalAlignment(JLabel.RIGHT);
        helpPanel.add(shiftLeftClickName);
        if ((apLoading.get().isPrevious() || previous)) {
            shiftLeftClickLabel = new JLabel("");
        } else {
            shiftLeftClickLabel = new JLabel("Add reference star aperture C1");
        }
        helpPanel.add(shiftLeftClickLabel);

        JLabel shiftControlLeftClickName = new JLabel("<Shift><Ctrl>left-click:");
        shiftControlLeftClickName.setHorizontalAlignment(JLabel.RIGHT);
        helpPanel.add(shiftControlLeftClickName);
        if ((apLoading.get().isPrevious() || previous)) {
            shiftControlLeftClickLabel = new JLabel("");
        } else {
            shiftControlLeftClickLabel = new JLabel("");
        }
        helpPanel.add(shiftControlLeftClickLabel);

        JLabel altLeftClickName = new JLabel("<Alt>left-click:");
        altLeftClickName.setHorizontalAlignment(JLabel.RIGHT);
        helpPanel.add(altLeftClickName);
        if ((apLoading.get().isPrevious() || previous)) {
            altLeftClickLabel = new JLabel("");
        } else {
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
        URL imgURL = MultiPlot_.class.getClassLoader().getResource(path);
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

    @Override
    public void windowOpened(WindowEvent e) {

    }

    @Override
    public void windowClosing(WindowEvent e) {
        cancel();
        Prefs.set(MultiAperture_.PREFS_CANCELED, "true");
        shutDown();
    }

    @Override
    public void windowClosed(WindowEvent e) {
        cancel();
        Prefs.set(MultiAperture_.PREFS_CANCELED, "true");
        shutDown();
    }

    @Override
    public void windowIconified(WindowEvent e) {

    }

    @Override
    public void windowDeiconified(WindowEvent e) {

    }

    @Override
    public void windowActivated(WindowEvent e) {

    }

    @Override
    public void windowDeactivated(WindowEvent e) {

    }

    public enum ApLoading implements RadioEnum {
        ALL_NEW(() -> "Place all new apertures", null, () -> true),
        FIRST_PREVIOUS(() -> "Place first previously used aperture", null, () -> storedCount() > 0),
        ALL_PREVIOUS(() -> "Place %s previously used apertures".formatted(storedCount()), null, () -> storedCount() > 0),
        IMPORTED(() -> "Place %s imported apertures".formatted(importedCount()), null, () -> importedCount() > 0),
        ;
        private final String tooltip;
        private final Supplier<String> buttonText;
        private final Supplier<Boolean> isEnabled;

        ApLoading(Supplier<String> buttonText, String tooltip, Supplier<Boolean> isEnabled) {
            this.buttonText = buttonText;
            this.tooltip = tooltip;
            this.isEnabled = isEnabled;
        }

        @Override
        public String optionText() {
            if (buttonText == null) {
                return name();
            }
            return buttonText.get();
        }

        @Override
        public String tooltip() {
            return tooltip;
        }

        public boolean isEnabled() {
            return isEnabled.get();
        }

        public boolean isSelected() {
            return apLoading.get() == this;
        }

        public boolean isPrevious() {
            return this != ALL_NEW;
        }

        private static int storedCount() {
            return switch (apertureShape.get()) {
                case CIRCULAR -> nAperturesStored;
                case CUSTOM_PIXEL -> CustomPixelApertureHandler.savedApertureCount();
            };
        }

        private static int importedCount() {
            return switch (apertureShape.get()) {
                case CIRCULAR -> nImportedApStored;
                case CUSTOM_PIXEL -> CustomPixelApertureHandler.savedImportedApertureCount();
            };
        }
    }

    public enum ApertureShape implements NState<ApertureShape>, ToolTipProvider {
        CIRCULAR("""
                <html>
                Standard circular aperture with sky background annulus
                </html>
                """, "Circular"),
        CUSTOM_PIXEL("""
                <html>
                A single arbitrarily shaped aperture is supported for analysis of space-based data which have precise
                pointing.<br>
                This mode is generally not useful for ground-based data because the aperture is defined as fixed pixel
                values in x/y space, therefore small pointing errors are not accounted for from image-to-image.<br>
                The user defines individual pixels that should be included in the source and background counts.
                </html>
                """, "Freeform Pixel-aligned Aperture"),
        ;

        private final String tooltip;
        private final String optionText;

        ApertureShape(String tooltip, String optionText) {
            this.tooltip = tooltip;
            this.optionText = optionText;
        }

        @Override
        public boolean isOn() {
            return false;
        }

        @Override
        public ApertureShape[] values0() {
            return ApertureShape.values();
        }

        @Override
        public String getToolTip() {
            return tooltip;
        }

        @Override
        public String toString() {
            return optionText;
        }
    }

    enum ApRadius implements RadioEnum {
        /**
         * Fixed based on user setting.
         */
        FIXED("Fixed Apertures as selected above", 0, "<html>Use standard fixed aperture photometry based on the user selected aperture radius and background annulus radii above.</html>"),
        /**
         * Auto radius based on radial profile.
         */
        AUTO_FIXED("Auto Fixed Apertures from first image T1 radial profile", 0.01, "<html>Use automatically sized fixed aperture photometry.<br>" +
                "The aperture and background radii are extracted using a radial profile<br>" +
                "measurement from the T1 aperture in the first image only.<br>" +
                "The user selected fixed aperture radii above are used to perform the initial radial profile measurement,<br>" +
                "so they should be set to reasonable aperture radii guesses for your images (e.g. 10-20-30).<br>" +
                "To make the auto aperture radii generally larger, use a smaller cutoff threshold value and vice versa.</html>"),
        /**
         * Auto variable aperture based on the entire stack.
         */
        AUTO_FIXED_STACK_RAD("Auto Fixed Apertures from multi-image T1 radial profiles", 0.01, "<html>Use automatically sized fixed aperture photometry based on all images.<br>" +
                "The aperture and background radii are extracted using a radial profile measurements from the T1 aperture in ALL images.<br>" +
                "This mode will require two passes through the stack, so will take longer than a standard fixed aperture run.<br>" +
                "Plate solved images are recommended. Un-plate solved images are OK as long as the apertures can track the image shifts.<br>" +
                "The user selected fixed aperture radii above are used to perform the radial profile measurements,<br>" +
                "so they should be set to reasonable aperture radii guesses for your images (e.g. 10-20-30).<br>" +
                "To make the auto aperture radii generally larger, use a smaller cutoff threshold value and vice versa.</html>"),
        /**
         * Auto variable radius based on radial profile.
         */
        AUTO_VAR_RAD_PROF("Auto Variable Apertures from each image T1 radial profile", 0.01, "<html>Use automatically sized variable aperture photometry.<br>" +
                "The aperture and background radii are extracted using a radial profile measurement from the T1 aperture in each image.<br>" +
                "All apertures within a single image will be the same size, but aperture sizes may vary from image to image if the PSF size changes.<br>" +
                "The user selected fixed aperture radii above are used to perform the initial radial profile measurement in each image,<br>" +
                "so they should be set to reasonable aperture radii guesses for your images (e.g. 10-20-30).<br>" +
                "To make the auto aperture radii generally larger, use a smaller cutoff threshold value and vice versa.</html>"),
        /**
         * Auto variable radius based on FWHM.
         */
        AUTO_VAR_FWHM("Auto Variable Apertures from each image T1 FWHM", 1.4, "<html>Use variable aperture photometry based on FWHM estimated from centroid function.<br>" +
                "The aperture radius in each image is the FWHM estimated by the AIJ centroid function<br>" +
                "from the T1 aperture in each image, multiplied by the FWHM Factor setting.<br>" +
                "All apertures within a single image will be the same size, but aperture sizes may vary from image to image if the FWHM changes.<br>" +
                "The user selected fixed aperture radius above is used to perform the centroid measurement and should be set<br>" +
                "to at least the FWHM (in pixels) that you expect in the worst image in the stack.<br>"+
                "The user selected background radii are used directly, so should be set larger than the largest expected aperture radius.<br>" +
                "To make the auto aperture radii generally larger, set the FWHM factor multiplier to a larger value and vice versa.</html>"),
        ;
        private static final ButtonGroup group = new ButtonGroup();
        private final String buttonText, tooltip;
        public double cutoff;
        private JRadioButton button = null;

        ApRadius(String buttonText, double cutoff, String tooltip) {
            this.buttonText = buttonText;
            this.cutoff = cutoff;
            this.tooltip = tooltip;
        }

        @Override
        public String optionText() {
            return buttonText;
        }

        @Override
        public String tooltip() {
            return tooltip;
        }

        public boolean autoRadius() {
            return this == AUTO_FIXED;
        }
    }

    record WeightedCoordinateMaxima(StarFinder.CoordinateMaxima cm, double weight) {
    }

    record Output(double r, double r1, double r2, int count) {
        public boolean success() {
            return Output.this.count == 0;
        }
    }

}

