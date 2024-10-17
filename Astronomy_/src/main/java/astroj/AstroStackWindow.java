package astroj;

import Astronomy.MultiAperture_;
import Astronomy.MultiPlot_;
import Astronomy.multiaperture.FreeformPixelApertureHandler;
import Astronomy.multiaperture.io.AperturesFile;
import Astronomy.postprocess.PhotometricDebayer;
import bislider.com.visutools.nav.bislider.*;
import ij.*;
import ij.astro.io.prefs.Property;
import ij.astro.logging.AIJLogger;
import ij.astro.util.FitsCompressionUtil;
import ij.astro.util.FitsExtensionUtil;
import ij.astro.util.UIHelper;
import ij.gui.*;
import ij.io.OpenDialog;
import ij.io.SaveDialog;
import ij.measure.Calibration;
import ij.plugin.FITS_Reader;
import ij.plugin.FITS_Writer;
import ij.plugin.FolderOpener;
import ij.plugin.Macro_Runner;
import ij.process.ColorProcessor;
import ij.process.ImageProcessor;
import ij.process.ImageStatistics;
import ij.process.StackProcessor;
import ij.util.Tools;
import util.PdfRasterWriter;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.PixelGrabber;
import java.io.*;
import java.net.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.DecimalFormat;
import java.util.List;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.Consumer;
import java.util.stream.IntStream;


/**
 * @author Karen
 */
public class AstroStackWindow extends StackWindow implements LayoutManager, ActionListener,
        MouseListener, MouseMotionListener, MouseWheelListener,
        KeyListener, ItemListener {

    AstroCanvas ac;
    ColorProcessor cp = null;
    //            Component[] stackSliders;
//            static final int MIN_WIDTH = 128;
//            static final int MIN_HEIGHT = 32;
    static final int MIN_FRAME_WIDTH = 700;
    static final int MIN_FRAME_HEIGHT = 450;
    static final int MAX_FRAME_HEIGHT_PADDING = 300;
    static final boolean DRAGGING = true;
    static final boolean NOT_DRAGGING = false;
    static final boolean REFRESH = true;
    static final boolean NEW = false;
    static final boolean RESIZE = true;
    static final boolean NORESIZE = false;
    static final boolean MOUSECLICK = true;
    static final boolean WHEEL = false;
    static final int BISLIDER_SEGMENTS = 256;
    static final boolean IMAGE_UPDATE = true;
    static final boolean NO_IMAGE_UPDATE = false;

    final static int FAILED = 0;
    final static int SUCCESS = 1;
    final static int SKIPPED = 2;
    final static int CANCELED = 3;

    int slice = -1;
    int oldSlice = -1;
    int stackSize = 0;
    double oldX;
    double oldY;
    double magnification = 0.5;
    Rectangle rct;

    DecimalFormat uptoTwoPlaces = new DecimalFormat("0.##", IJU.dfs);
    DecimalFormat fourPlaces = new DecimalFormat("###,###,##0.0000", IJU.dfs);
    DecimalFormat uptoFourPlaces = new DecimalFormat("0.####", IJU.dfs);
    DecimalFormat twoPlaces = new DecimalFormat("0.00", IJU.dfs);
    DecimalFormat threePlaces = new DecimalFormat("0.000", IJU.dfs);
    DecimalFormat threeDigits = new DecimalFormat("000", IJU.dfs);
    DecimalFormat noPlaces = new DecimalFormat("###,###,##0", IJU.dfs);
    DecimalFormat sixPlaces = new DecimalFormat("0.000000", IJU.dfs);
    DecimalFormat uptoSixPlaces = new DecimalFormat("0.######", IJU.dfs);
    DecimalFormat scientificSixPlaces = new DecimalFormat("0.######E00", IJU.dfs);

    public ImageStatistics stats;
//            Dimension screenDim;
//            Rectangle screenDim;

    double startDragX, startDragY, startDragCenX, startDragCenY, endDragX, endDragY;
    int origCanvasHeight, origCanvasWidth;
    int startDragSubImageX, startDragSubImageY;
    int startDragScreenX, startDragScreenY;
    int lastScreenX, lastScreenY;
    int screenX, screenY;
    double lastImageX, lastImageY;
    int frameLocationX, frameLocationY;
    int newPositionX, newPositionY;
    int icHeight, icWidth, ipWidth, ipHeight;
    int astronomyToolId, apertureToolId, zoomToolId, panToolId, currentToolId;
    int ocanvasHeight, ocanvasWidth;
    int imageHeight, imageWidth, winWidth, winHeight;
    int otherPanelsHeight, frameHeightPadding;
    int sliderScale;
    int count = 0;
    int newicWidth;
    int currentSlice;
    int oldICHeight = 0;
    int oldICWidth = 0;
    int clipX = 0;
    int clipY = 0;
    int clipWidth = 0;
    int clipHeight = 0;
    String defaultMeasurementColor = "magenta";
    String defaultAnnotationColor = "orange";
    String[] colors = IJU.colors;

    int oldX3 = 0, oldY3 = 0, oldH3 = 0, oldW3 = 0;

    double radius = 25, rBack1 = 40, rBack2 = 60;

    private String oldSubtitle = "";

    int astrometryStatus = 2;
    Astrometry astrometry;
    int hgap = 0;
    int vgap = 0;
    int[] histogram;
    double histMax = 0.0;
    double[] logHistogram;
    boolean maxBoundsReset = false;
    boolean exact = true;
    int resetMaxBoundsCount;
    double dstWidth;
    double dstHeight;
    double prevBarHeight = 0.0;
    Rectangle srcRect;
    int imageEdgeX1, imageEdgeX2, imageEdgeY1, imageEdgeY2;
    long setMaxBoundsTime;
    String slash = System.getProperty("file.separator");

    double sliderMultiplier, sliderShift;
    double prevMag, prevImageX = 0, prevImageY = 0;
    double startMinDisplayValue, startMaxDisplayValue;
    double imageMedian;
    public double blackValue;
    double whiteValue;
    double minValue;
    public double maxValue;
    double meanValue;
    double stdDevValue;
    double[] sliceMin, sliceMax;
    double scaleMin, scaleMax, fixedMinValue, fixedMaxValue;
    double brightness, contrast, brightstepsize, contrastStepSize;
    double autoScaleFactorLow = 0.5, autoScaleFactorHigh = 2.0, autoScaleFactorLowRGB = 2.0, autoScaleFactorHighRGB = 6.0;
    double simbadSearchRadius = 0.25;
    double annotateCircleRadius = 20;

    double savedMag = 1.0;
    double savedBlackValue = 0.0;
    double savedWhiteValue = 255.0;
    public double pixelScaleX = 0.0, pixelScaleY = 0.0;
    int wcsSlice = 0;
    int savedICHeight = 600;
    int savedICWidth = 600;
    int savedPanX = 0;
    int savedPanY = 0;
    int savedPanHeight = 600;
    int savedPanWidth = 600;
    int savedIpHeight = 0;
    int savedIpWidth = 0;
    int winHeightBeforeMaximize;
    int winWidthBeforeMaximize;
    int canHeightBeforeMaximize;
    int canWidthBeforeMaximize;
    double magbefore = 1;
    String extraInfo = "";

    /**
     * Causes {@link Astronomy.Astronomy_Listener#imageUpdated(ImagePlus)} to update the image when its value
     * is {@code false} via calling {@link AstroStackWindow#setAstroProcessor(boolean)}.
     */
    public boolean minMaxChanged = false;
    boolean newClick;
    boolean button23Drag;
    boolean startButtonCentroid = true;
    boolean endButtonCentroid = true;
    boolean alreadyCustomStackWindow = false;
    public boolean goodWCS = false;
    boolean firstClick = true;
    boolean useSexagesimal = true;
    boolean startupPrevSize = true;
    boolean showPhotometer = true;
    boolean prevShiftDownState = false;
    public boolean autoContrast = true;
    public boolean isReady = false;
    boolean startupPrevPan = false;
    boolean startupPrevZoom = false;
    boolean fixedContrast = false;
    boolean fixedContrastPerSlice = false;
    boolean tempAutoLevel = false;
    boolean tempPrevLevels = false;
    boolean tempPrevLevelsPerSlice = false;
    boolean rememberWindowLocation = true;
    boolean writeMiddleClickValuesTable = true;
    boolean writeMiddleDragValuesTable = true;
    boolean writeMiddleClickValuesLog = false;
    boolean writeMiddleDragValuesLog = false;
    boolean astronomyMode = true;
    boolean movingAperture = false;
    boolean autoConvert = true;
    boolean firstTime = true;
    boolean mouseDown = false;
    boolean stackRotated = false;
    boolean refresh2 = false;
    boolean fillNotFit = false;
    boolean useInvertingLut = false;
    boolean reposition = true;
    boolean redrawing = false;
    boolean middleClickCenter = true;
    boolean removeBackStars = true;
    boolean showRemovedPixels = false;
    boolean apertureChanged = false;
    boolean showSkyOverlay = false;
    boolean showZoom = true;
    boolean showDir = true;
    boolean showXY = true;
    boolean showScaleX = true, showScaleY = true;
    boolean useFixedMinMaxValues = false;
    boolean dataRotated = false;
    boolean shiftAndControlWasDown = false;
    boolean shiftClickDisabled = false;
    boolean unzoomWhenMinimize = false;
    boolean invertX = false;
    boolean invertY = false;
    boolean autoNupEleft = true;
    boolean nameOverlay = true;
    boolean valueOverlay = true;
    boolean showMeanNotPeak = false;
    boolean saveAllPNG = true;
    boolean saveImage = true;
    boolean savePlot = true;
    boolean saveConfig = true;
    boolean saveTable = true;
    boolean saveApertures = true;
    boolean saveLog = true;
    boolean updatesEnabled = true;
    boolean autoScaleIconClicked = false;
    boolean useSIPAllProjections = true;
    boolean astrometryCanceled = false;
    boolean showSetup = true;
    boolean backPlane = false;
    boolean showAnnotateCircle = true;
    boolean showAnnotateCrosshair = true;
    boolean rightClickAnnotate = true;
    boolean useSimbadSearch = true;
    boolean showInSimbad = true;
    boolean autoUpdateAnnotationsInHeader = true;
    public boolean autoDisplayAnnotationsFromHeader = true;
    boolean showAbsMag = true;
    boolean showIntCntWithAbsMag = true;
    boolean autoSaveWCStoPrefs = true;
    boolean negateMeasureDelMag = false;
    boolean showMeasureSex = true;
    boolean showMeasureCircle = true;
    boolean showMeasureLength = true;
    boolean showMeasurePA = true;
    boolean showMeasureDelMag = true;
    boolean showMeasureFluxRatio = true;
    boolean showMeasureMultiLines = true;
    boolean showMeasureCrosshair = true;
    boolean writeMeasureLengthLog = true;
    boolean writeMeasureLengthTableDeg = false;
    boolean writeMeasureLengthTableMin = true;
    boolean writeMeasureLengthTableSec = false;
    boolean writeMeasurePA = true;
    boolean writeMeasureDelMag = true;
    boolean writeMeasureFluxRatio = true;
    boolean writePhotometricDataTable = true;

    Boolean showFits = true;
    String fitsKeywords = "";
    Boolean showPosition = true;
    Boolean showPositionFITS = true;
    Boolean showPhotometry = true;
    Boolean showNAperPixels = true;
    Boolean showNBackPixels = true;
    Boolean showBack = true;
    Boolean showFileName = true;
    Boolean showSliceNumber = true;
    Boolean showPeak = true;
    Boolean showMean = true;
    Boolean showWidths = true;
    Boolean calcRadProFWHM = true;
    Boolean showRadii = true;
    Boolean showTimes = true;
    Boolean showMeanWidth = true;
    Boolean showAngle = true;
    Boolean showRoundness = true;
    Boolean showVariance = true;
    Boolean showErrors = true;
    Boolean showSNR = true;
    Boolean showRADEC = true;
    Boolean autoGrabBandCFromHistogram = true;

    int rotation = AstroCanvas.ROT_0;
    boolean netFlipX, netFlipY, netRotate;
    boolean flipDataX, flipDataY, rotateDataCW, rotateDataCCW;
    boolean doubleClick = false;
    int mouseButton = 1;
    double[] rightClickPixel = new double[2];
    TimerTask doubleClickTask = null;
    java.util.Timer doubleClickTaskTimer = null;

    String IJVersion = IJ.getVersion();
    String impTitle;
    Calibration cal;

    String imageSuffix = "_field";
    String plotSuffix = "_lightcurve";
    String configSuffix = "_measurements";
    String dataSuffix = "_measurements";
    String aperSuffix = "_measurements";
    String logSuffix = "_calibration";

    TimerTask rotateTask = null, photometerTask = null;
    java.util.Timer rotateTaskTimer = null, photometerTaskTimer = null;

    JScrollBar channelSelector, sliceSelector, frameSelector;
    Thread thread = null, astrometryThread = null;
    Centroid cen = new Centroid();
    Centroid endCen = new Centroid();
    volatile boolean done;
    boolean hyperStack;
    //            int nChannels=1, nSlices=1, nFrames=1;
    int c = 1, z = 1, t = 1;
    int scrollBarTotal;

    MouseWheelListener[] mwl;
    MouseWheelListener[] icmwl;
    MouseMotionListener[] mml;
    MouseListener[] ml;
    Toolbar toolbar;

    double[] radec, startRadec;

    double[] xy = new double[2];
    public WCS wcs;
    Photometer photom;
    Photometer photom1;
    Photometer photom2;
    //            Overlay apertureOverlay = new Overlay();
    Roi radiusRoi = null;
    Roi rBack1Roi = null;
    Roi rBack2Roi = null;

    double[] crpix = null;
    double[][] cd = null;
    int[] npix = null;

    Font p12;
    Font p13;
    Font b12;

    Color mouseApertureColor = new Color(128, 128, 255);
    Color colorWCS = new Color(255, 190, 0);

    AstrometrySetup astrometrySetup = new AstrometrySetup();
    MeasurementRoi measRoi = new MeasurementRoi(-1000.0, -1000.0);
    String tableName = "Measure_Tool";
    MeasurementTable table;

    MenuBar mainMenuBar = new MenuBar();
    Panel mainPanel;
    JPanel infoPanel;
    JPanel topPanelA;
    JPanel zoomPanel;
    JPanel topPanelB, topPanelBC;
    JPanel bottomPanelB;
    JPanel canvasPanel;
    JTextField lengthLabel, peakLabel, infoTextField;

    Menu fileMenu, preferencesMenu, scaleMenu, saveFitsMenuItem, saveFitsStack3DMenuItem, saveFitsStackMenuItem, viewMenu, annotateMenu, measureMenu, editMenu, processMenu, colorMenu, analyzeMenu, wcsMenu;

    MenuItem exitMenuItem, flipDataXMenuItem, flipDataYMenuItem, rotateDataCWMenuItem, rotateDataCCWMenuItem, simbadSearchRadiusMenuItem;
    MenuItem openMenuItem, openInNewWindowMenuItem, openSeqMenuItem, openSeqInNewWindowMenuItem;
    MenuItem saveDisplayAsJpgMenuItem, saveDisplayAsPngMenuItem, saveDisplayAsPdfMenuItem, saveStatePNGMenuItem, saveStateJPGMenuItem, setSaveStateMenuItem;
    MenuItem openAperturesMenuItem, saveAperturesMenuItem, saveMenuItem, saveStackSequenceMenuItem, clearOverlayMenuItem;
    MenuItem openRaDecAperturesMenuItem, saveRaDecAperturesMenuItem;
    MenuItem saveTiffMenuItem, saveJpegMenuItem, savePdfMenuItem, savePngMenuItem, saveBmpMenuItem, saveGifMenuItem, saveAviMenuItem;
    MenuItem dirAngleMenuItem, saveWCStoPrefsMenuItem, astrometryMenuItem, astrometrySetupMenuItem;
    MenuItem annotateMenuItem, editAnnotationMenuItem, deleteAnnotationMenuItem;
    MenuItem annotateFromHeaderMenuItem, annotateAppendFromHeaderMenuItem, replaceAnnotationsInHeaderMenuItem,
            appendToAnnotationsInHeaderMenuItem, deleteAnnotationsFromHeaderMenuItem, clearAllAnnotateRoisMenuItem;

    MenuItem backupAllAIJPrefsMenuItem, restoreAllAIJPrefsMenuItem, restoreDefaultAIJPrefsMenuItem;
    MenuItem combineStackImagesMenuItem, concatStacksMenuItem, copyFitsHeaderProcessMenuItem;

    MenuItem stackSorterMenuItem, alignStackMenuItem, imageStabilizerMenuItem, imageStabilizerApplyMenuItem;
    MenuItem debayerMenuItem, photoDebayerMenuItem, splitChannelsMenuItem, imagesToStackMenuItem, stackToImagesMenuItem, RGBComposerMenuItem;
    MenuItem normalizeStackMenuItem, shiftImageMenuItem, editFitsHeaderMenuItem, copyFitsHeaderMenuItem, staticProfilerMenuItem, stackToRGBMenuItem, makeCompositeMenuItem;
    MenuItem apertureSettingsMenuItem, multiApertureMenuItem, multiPlotMenuItem, openMeasurementsTableMenuItem, threeDSurfacePlotMenuItem;
    MenuItem bestEdgesMenuItem, imageCalcMenuItem, seeingProfileMenuItem, dynamicProfilerMenuItem;
    MenuItem contourLinesMenuItem, contourPlottersMenuItem, azimuthalAverageMenuItem;
    MenuItem measurementSettingsMenuItem, measurementMenuItem, smoothMenuItem, sharpenMenuItem, removeOutliersMenuItem;
    MenuItem dataReducerMenuItem, selectBestFramesMenuItem, setPixelScaleMenuItem, setZoomIndicatorSizeMenuItem, setAutoScaleParametersMenuItem,
            grabAutoScaleParametersMenuItem, resetAutoScaleParametersMenuItem;
    MenuItem defaultAnnotationColorMenuItem, defaultMeasurementColorMenuItem;
    CheckboxMenuItem showMeasureSexCB, showMeasureCircleCB, showMeasureCrosshairCB, showMeasureLengthCB, showMeasurePACB, showMeasureDelMagCB, showMeasureFluxRatioCB,
            showMeasureMultiLinesCB, negateMeasureDelMagCB, writeMeasureLengthLogCB, writeMeasureLengthTableDegCB, writeMeasureLengthTableMinCB, writeMeasureLengthTableSecCB,
            writeMeasurePACB, writeMeasureDelMagCB, writeMeasureFluxRatioCB, writePhotometricDataTableCB;
    CheckboxMenuItem autoContrastRB, fixedContrastRB, fixedContrastPerSliceRB, useFullRangeRB, negativeDisplayRB;
    CheckboxMenuItem autoNupEleftRB, invertNoneRB, invertXRB, invertYRB, invertXYRB;
    CheckboxMenuItem rotate0RB, rotate90RB, rotate180RB, rotate270RB, useSIPAllProjectionsCB;
    CheckboxMenuItem showZoomCB, showDirCB, showXYCB, showScaleXCB, showScaleYCB, useFixedMinMaxValuesCB;
    CheckboxMenuItem showAbsMagCB, showIntCntWithAbsMagCB, autoSaveWCStoPrefsCB, autoGrabBandCFromHistogramCB;
    CheckboxMenuItem rightClickAnnotateCB, useSimbadSearchCB, showInSimbadCB, autoUpdateAnnotationsInHeaderCB, autoDisplayAnnotationsFromHeaderCB;
    ButtonGroup contrastGroup, invertGroup, rotationGroup;
    CheckboxMenuItem autoConvertCB, usePreviousSizeCB, usePreviousPanCB, usePreviousZoomCB, showMeanNotPeakCB,
            rememberWindowLocationCB, useSexagesimalCB, middleClickCenterCB, writeMiddleClickValuesTableCB, writeMiddleDragValuesTableCB, writeMiddleClickValuesLogCB,
            writeMiddleDragValuesLogCB, showPhotometerCB, removeBackStarsCB, showRemovedPixelsCB, showRedCrossHairCursorCB;

    JButton buttonAdd32768, buttonSub32768, buttonFit, buttonHeader, buttonLUT, buttonZoomInFast, buttonZoomIn, buttonZoomOut;
    JButton buttonFlipX, buttonFlipY, buttonRotCCW, buttonRotCW, buttonAutoLevels, buttonClearMeasurements;
    JButton buttonBroom, buttonShowAll, buttonMultiAperture, buttonAlign, buttonSetAperture, buttonDeleteSlice;
    JToggleButton buttonShowSky, buttonSourceID, buttonSourceCounts, buttonCentroid, buttonNegative, buttonAstrometry, buttonShowAnnotations;
    JSlider minSlider, maxSlider;
    JTextField minValueTextField, maxValueTextField, blackTextfield, whiteTextfield, meanTextField;
    JTextField valueTextField, RATextField, DecTextField, peakTextField;
    JTextField fitsXTextField, fitsYTextField, lengthTextField;
    JTextField ijXTextField, ijYTextField;
    BiSlider minMaxBiSlider;

    GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
    GraphicsDevice defaultScreen = ge.getDefaultScreenDevice();
    Rectangle defaultScreenBounds = defaultScreen.getDefaultConfiguration().getBounds();

    private boolean hasNotified;
    private final Set<Consumer<Void>> stackListeners = new HashSet<>();
    private final static Property<Boolean> plotStackPixelValues = new Property<>(false, AstroStackWindow.class);
    private Plot stackPixelPlot = null;
    private PlotWindow stackPixelPlotWin = null;
    private static final Property<Point> stackPlotWindowLocation = new Property<>(new Point(), AstroStackWindow.class);

    public AstroStackWindow(ImagePlus imp, AstroCanvas ac, boolean refresh, boolean resize) {

        super(imp, ac);
        isReady = false;
        if (IJ.isMacro() && !isVisible()) //'super' may have called show()
            imp.setDeactivated(); //prepare for waitTillActivated (imp may have been activated before)

        // Fixes the menu bar being overridden on macs
        // See ImageWindow#setImageJMenuBar(ImageWindow)
        imp.setIJMenuBar(false);

        Locale.setDefault(IJU.locale);
//                SET DEFAULT SYSTEM LOOK AND FEEL
//                UIManager.LookAndFeelInfo[] laf = UIManager.getInstalledLookAndFeels();
//                for (int i = 0 ; i < laf.length; i++)
//                IJ.log(""+laf[i]);
//                System.setProperty("com.apple.laf.useScreenMenuBar", "false");

        UIHelper.setLookAndFeel();

//                if (rememberWindowLocation)
//                    {
//                    topLeftX
//
//                GraphicsConfiguration graphicsConfiguration = null;
//                for (GraphicsDevice gd : GraphicsEnvironment.getLocalGraphicsEnvironment().getScreenDevices())
//                    {
//                    if (gd.getDefaultConfiguration().getBounds().contains(new Point(this.getX(),this.getY())))
//                        {
//                        graphicsConfiguration = gd.getDefaultConfiguration();
//                        break;
//                        }
//                    }


        this.imp = imp;
        this.ac = ac;
        cal = imp.getCalibration();

        super.hasMenus = true;

//                super.ic.setBackground(Color.WHITE);

        getStatistics();
        minValue = stats.min;
        maxValue = stats.max;
        blackValue = minValue;
        whiteValue = maxValue;

        ImageProcessor ip = imp.getProcessor();

        if (imp.getType() == ImagePlus.COLOR_RGB) {
            ip.reset();
            ip.snapshot();
            cp = (ColorProcessor) (ip.duplicate());
        }

        stackSize = imp.getStackSize();
        sliceMin = new double[stackSize];
        sliceMax = new double[stackSize];
        for (int i = 0; i < stackSize; i++) {
            sliceMin[i] = blackValue;
            sliceMax[i] = whiteValue;
        }

        getPrefs();

        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        GraphicsDevice[] gds = ge.getScreenDevices();
        GraphicsDevice gd = null;
        Rectangle screenBounds = new Rectangle();
        boolean foundScreen = false;
        for (int j = 0; j < gds.length; j++) {
            gd = gds[j];
            screenBounds.setRect(gd.getDefaultConfiguration().getBounds());
            if (screenBounds.contains(frameLocationX, frameLocationY)) {
                foundScreen = true;
                break;
            }
        }
        if (!foundScreen) {
            gd = ge.getDefaultScreenDevice();
            screenBounds.setRect(gd.getDefaultConfiguration().getBounds());
        }

        Insets insets = Toolkit.getDefaultToolkit().getScreenInsets(gd.getDefaultConfiguration());
        screenBounds.x += insets.left;
        screenBounds.y += insets.top;
        screenBounds.width -= insets.left + insets.right;
        screenBounds.height -= insets.top + insets.bottom;

        if (imp.getType() == ImagePlus.COLOR_256 || imp.getType() == ImagePlus.COLOR_RGB || imp.getType() == ImagePlus.GRAY8) {
            useFixedMinMaxValues = false;
            minValue = cal.getCValue(0);
            maxValue = cal.getCValue(255);
            if (blackValue < minValue) blackValue = minValue;
            if (whiteValue > maxValue) whiteValue = maxValue;
        } else {
            maxValue = useFixedMinMaxValues ? fixedMaxValue : stats.max;
            minValue = useFixedMinMaxValues ? fixedMinValue : stats.min;
            if (imp.getType() == ImagePlus.GRAY16 && maxValue - minValue < 256)
                maxValue = minValue + 255;
        }
        impTitle = imp.getTitle();


        wcs = new WCS(imp);
        goodWCS = wcs.hasWCS();
        wcs.setUseSIPAlways(useSIPAllProjections);
        extraInfo = " (" + wcs.coordsys + ")";
        ac.setWCS(wcs);
        if (autoNupEleft) setBestOrientation();
        ac.setOrientation(invertX, invertY, rotation);
        ac.setShowPixelScale(showScaleX, showScaleY, pixelScaleX, pixelScaleY);
        netFlipX = ac.getNetFlipX();
        netFlipY = ac.getNetFlipY();
        netRotate = ac.getNetRotate();
        ac.setShowZoom(showZoom);
        ac.setShowDir(showDir);
        ac.setShowXY(showXY);


//                adjustImageRotation(NO_IMAGE_UPDATE);


        photom = new Photometer(cal);
        photom.setSourceApertureRadius(radius);
        photom.setBackgroundApertureRadii(rBack1, rBack2);
        photom.setRemoveBackStars(removeBackStars);
        photom.setMarkRemovedPixels(false);
        photom1 = new Photometer(cal);
        photom2 = new Photometer(cal);

        ac.setAperture(radius, rBack1, rBack2, showSkyOverlay, showPhotometer);
        ac.setAstronomyMode(true);

//                imp.setOverlay(apertureOverlay);


        if (IJ.isWindows()) {
            p12 = new Font("Dialog", Font.PLAIN, 12);
            p13 = new Font("Dialog", Font.PLAIN, 13);
            b12 = new Font("Dialog", Font.BOLD, 12);
        } else {
            p12 = new Font("Dialog", Font.PLAIN, 11);
            p13 = new Font("Dialog", Font.PLAIN, 12);
            b12 = new Font("Dialog", Font.BOLD, 11);
        }

        winWidth = this.getWidth();
        winHeight = this.getHeight();

        if (!startupPrevSize && resize) {
            ac.setDrawingSize((int) (ac.getWidth() * 0.9), (int) (ac.getHeight() * 0.9));
            ac.setMagnification(ac.getMagnification() * 0.9);
        } else if (startupPrevSize) {
//                    ac.setDrawingSize((int)((double)savedICHeight*(double)imp.getWidth()/(double)imp.getHeight()),savedICHeight);
//                    ac.setMagnification((double)savedICHeight/(double)imp.getHeight());
            ac.setDrawingSize(savedICWidth, savedICHeight);
            ac.setMagnification(Math.min((double) (savedICWidth) / (double) imp.getWidth(), (double) (savedICHeight) / (double) imp.getHeight()));
        }

        magnification = ac.getMagnification();
        icWidth = ac.getWidth();
        icHeight = ac.getHeight();
        ipWidth = ip.getWidth();
        ipHeight = ip.getHeight();

        if (icWidth < MIN_FRAME_WIDTH - extraWidth()) {
            newicWidth = MIN_FRAME_WIDTH - extraWidth();
            double mag = Math.max((double) newicWidth / (double) ipWidth, (double) icHeight / (double) ipHeight);
            ac.setDrawingSize((int) (ipWidth * mag), (int) (ipHeight * mag));
            ac.setMagnification(mag);
        }


        if (ac.getHeight() > screenBounds.height - MAX_FRAME_HEIGHT_PADDING) {
            ac.setMagnification((double) (screenBounds.height - MAX_FRAME_HEIGHT_PADDING) / (double) ip.getHeight());
            ac.setDrawingSize((int) Math.max(((screenBounds.height - MAX_FRAME_HEIGHT_PADDING) * (double) ip.getWidth() /
                    (double) ip.getHeight()), MIN_FRAME_WIDTH), screenBounds.height - MAX_FRAME_HEIGHT_PADDING);
        }

        magnification = ac.getMagnification();
        icWidth = ac.getWidth();
        icHeight = ac.getHeight();
        ipWidth = ip.getWidth();
        ipHeight = ip.getHeight();

//                IJ.log("Starting build of astro window");
        buildAstroWindow();
//                IJ.log("Finished build of astro window");

        if (ac.getHeight() > screenBounds.height - frameHeightPadding) {
            ac.setMagnification((double) (screenBounds.height - frameHeightPadding) / (double) ip.getHeight());
            ac.setDrawingSize((int) Math.max(((screenBounds.height - frameHeightPadding) * (double) ip.getWidth() /
                    (double) ip.getHeight()), MIN_FRAME_WIDTH), screenBounds.height - frameHeightPadding);
        }

//                IJ.log("height="+screenBounds.height+"    padding="+frameHeightPadding);
//                IJ.log("Starting check of 'startup using previous pan position'");
        if (startupPrevPan && ipWidth == savedIpWidth && ipHeight == savedIpHeight) {
            double w = (double) icWidth / magnification;
            if (w * magnification < icWidth) w++;
            double h = (double) icHeight / magnification;
            if (h * magnification < icHeight) h++;
            Rectangle rect = new Rectangle(savedPanX, savedPanY, savedPanWidth, savedPanHeight);
            ac.setSourceRect(rect);
            ac.setMagnification((double) ac.getHeight() / (double) rect.height);
            ac.setDrawingSize((int) ((double) ac.getHeight() * (double) rect.width / (double) rect.height), ac.getHeight());
            if (rect.x < 0 || rect.y < 0 || rect.x + w > ipWidth || rect.y + h > ipHeight) {
                ac.paint(ac.getGraphics());//clearAndPaint();
            }
        }
//                IJ.log("Finished check of 'startup using previous pan position'");

        setupListeners();
        setImageEdges();
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                saveAndClose(false);
            }
        });

//                IJ.log("Before 'set layout'");
        setLayout(this);
//                IJ.log("Before 'wait 200 ms'");
        IJ.wait(200); //200 an attempt to work around window non-display
//                IJ.log("After 'wait 200 ms'");
//                doLayout();
        isReady = true;
        startDragScreenX = (int) (ac.getX() + ac.getWidth() / 2.0);
        startDragScreenY = (int) (ac.getY() + ac.getHeight() / 2.0);
//                IJ.error("pause");
//                IJ.log("Start setAutoLevels");

        slice = imp.getCurrentSlice();
        oldSlice = slice;
        if (autoDisplayAnnotationsFromHeader) {
            displayAnnotationsFromHeader(true, true, true);
        }
        if (autoContrast) {
            setAutoLevels(null);
        } else if (fixedContrast || fixedContrastPerSlice) {
            if (imp.getType() == ImagePlus.COLOR_256 || imp.getType() == ImagePlus.COLOR_RGB || imp.getType() == ImagePlus.GRAY8) {
                blackValue = savedBlackValue < minValue ? minValue : savedBlackValue;
                whiteValue = savedWhiteValue > maxValue ? maxValue : savedWhiteValue;
            } else {
                blackValue = savedBlackValue;
                whiteValue = savedWhiteValue;
            }
            for (int i = 0; i < stackSize; i++) {
                sliceMin[i] = blackValue;
                sliceMax[i] = whiteValue;
            }
            updatePanelValues();
        } else {
            blackValue = minValue;
            whiteValue = maxValue;
            for (int i = 0; i < stackSize; i++) {
                sliceMin[i] = blackValue;
                sliceMax[i] = whiteValue;
            }
            updatePanelValues();
        }
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosed(WindowEvent e) {
                WindowManager.removeWindow(AstroStackWindow.this);
            }
        });

//                IJ.log("Start useInvertingLut");
        if (useInvertingLut != ip.isInvertedLut() && !ip.isColorLut())
            ip.invertLut();
//                IJ.log("Start updateZoomBoxParameters");
        ac.setShowAbsMag(showAbsMag);
        ac.setShowIntCntWithAbsMag(showIntCntWithAbsMag);
        ac.updateZoomBoxParameters();
        saveWCStoPrefsMenuItem.setEnabled(wcs != null && (wcs.hasPA || wcs.hasScale));
        imp.getWindow().requestFocus();
//                IJ.log("Start requestFocusInWindow");
        imp.getCanvas().requestFocusInWindow();
//                IJ.log("Start setVisible");
        setVisible(true);
//                IJ.log("Finished displaying astro window");
        if (IJ.isMacro())
            imp.waitTillActivated();

        isReady = true;

    }

    @Override
    public void focusGained(FocusEvent e) {
        super.focusGained(e);
        setMenuBar(mainMenuBar);
    }

    @Override
    public MenuBar getMenuBar() {
        return mainMenuBar;
    }

    @Override
    public boolean close() {
        isReady = false;
        toolbar.removeMouseListener(toolbarMouseListener);
        ac.removeMouseWheelListener(this);
        ac.removeMouseListener(this);
        ac.removeMouseMotionListener(this);
        ac.removeKeyListener(this);
        WindowManager.removeWindow(this);

        return super.close();
    }

    void saveAndClose(boolean cleanWindow) {
        savePrefs();
        toolbar.removeMouseListener(toolbarMouseListener);
        ac.removeMouseWheelListener(this);
        ac.removeMouseListener(this);
        ac.removeMouseMotionListener(this);
        ac.removeKeyListener(this);
        if (astrometryThread != null) {
            if (astrometry != null && astrometry.astrometrySetup != null &&
                    astrometry.astrometrySetup.astrometrySetupFrame != null)
                astrometry.astrometrySetup.astrometrySetupFrame.dispose();
            if (astrometry != null) astrometry.setAstrometryCanceled();
            astrometryThread.stop();
            astrometryThread = null;
        }
        if (imp != null) imp.changes = false;
        if (imp != null) imp.unlock();
        wcs = null;
        photom = null;
        if (cleanWindow) {
            if (imp != null) imp.close();
            WindowManager.removeWindow(this);
        }
    }

//        void updatePhotometerOverlay()
//                {
////                apertureChanged = Prefs.get("setaperture.aperturechanged", apertureChanged);
////
////                if ((apertureOverlay.size() > 0) && !apertureChanged)
////                    {
////                    apertureOverlay.set   StrokeColor(mouseApertureColor);
////                    radiusRoi.setLocation((int)(lastImageX-photom.radius+0.5), (int)(lastImageY-photom.radius+0.5));
////                    if (showSkyOverlay)
////                        {
////                        rBack1Roi.setLocation((int) (lastImageX - photom.rBack1 + 0.5), (int) (lastImageY - photom.rBack1 + 0.5));
////                        rBack2Roi.setLocation((int)(lastImageX-photom.rBack2+0.5), (int)(lastImageY-photom.rBack2+0.5));
////                        }
////                    Graphics g = ac.getGraphics();
////                    ac.paint(g);
////                    }
////                else
////                    {
////                    showSkyOverlay = Prefs.get ("aperture.skyoverlay", showSkyOverlay);
////                    Prefs.set("setaperture.aperturechanged", false);
////                    apertureOverlay.clear();
////                    radius = Prefs.get("aperture.radius", radius);
////                    rBack1 = Prefs.get("aperture.rback1",rBack1);
////                    rBack2 = Prefs.get("aperture.rback2",rBack2);
////                    removeBackStars = Prefs.get("aperture.removebackstars", removeBackStars);
////                    photom.setSourceApertureRadius(radius);
////                    photom.setBackgroundApertureRadii(rBack1,rBack2);
////                    photom.setRemoveBackStars(removeBackStars);
////                    apertureOverlay.setStrokeColor(mouseApertureColor);
////                    radiusRoi = new ij.gui.OvalRoi((int)(lastImageX-photom.radius+0.5), (int)(lastImageY-photom.radius+0.5), (int)(photom.radius*2.0), (int)(photom.radius*2.0));
////                    apertureOverlay.add(radiusRoi);
////                    if (showSkyOverlay)
////                        {
////                        rBack1Roi = new ij.gui.OvalRoi((int)(lastImageX-photom.rBack1+0.5), (int)(lastImageY-photom.rBack1+0.5), (int)(photom.rBack1*2.0), (int)(photom.rBack1*2.0));
////                        rBack2Roi = new ij.gui.OvalRoi((int)(lastImageX-photom.rBack2+0.5), (int)(lastImageY-photom.rBack2+0.5), (int)(photom.rBack2*2.0), (int)(photom.rBack2*2.0));
////                        apertureOverlay.add(rBack1Roi);
////                        apertureOverlay.add(rBack2Roi);
////                        }
////                    imp.setOverlay(apertureOverlay);
////                    }
//                Thread tt = new Thread()
//                    {
//                    public void run()
//                        {
//                Graphics g = ac.getGraphics();
//                ((Graphics2D)g).setTransform(ac.invCanvTrans);
//                ac.transEnabled = false;
//                g.setColor(mouseApertureColor);
//                double imageX = ac.offScreenXD(screenX);
//                double imageY = ac.offScreenYD(screenY);
//
//                int sx = ac.screenXD (imageX);
//                int sy = ac.screenYD (imageY);
//
//                int x1 = ac.screenXD (imageX-radius);
//                int w1 = ac.screenXD (imageX+radius)-x1;
//                int y1 = ac.screenYD (imageY-radius);
//                int h1 = ac.screenYD (imageY+radius)-y1;
//                int x2 = ac.screenXD (imageX-rBack1);
//                int x3 = ac.screenXD (imageX-rBack2);
//                int w2 = ac.screenXD (imageX+rBack1)-x2;
//                int w3 = ac.screenXD (imageX+rBack2)-x3;
//                int y2 = ac.screenYD (imageY-rBack1);
//                int y3 = ac.screenYD (imageY-rBack2);
//                int h2 = ac.screenYD (imageY+rBack1)-y2;
//                int h3 = ac.screenYD (imageY+rBack2)-y3;
//
//                g.drawOval (x1,y1,w1,h1);
//
//
//                if (showSkyOverlay)
//                    {
//
//                    g.drawOval (x2,y2,w2,h2);
//                    g.drawOval (x3,y3,w3,h3);
//                    }
//                clipX = (x3<=oldX3)?x3:oldX3;
//                clipY = (y3<=oldY3)?y3:oldY3;
//                clipWidth = ((x3+w3>=oldX3+oldW3)?x3+w3:oldX3+oldW3) - clipX + 1;
//                clipHeight = ((y3+h3>=oldY3+oldH3)?y3+h3:oldY3+oldH3) - clipY + 1;
//                clipX-=2; clipY-=2;
//                clipWidth+=4; clipHeight+=4;
//                oldX3=x3;
//                oldY3=y3;
//                oldW3=w3;
//                oldH3=h3;
//
//                ac.transEnabled = true;
//                ((Graphics2D)g).setTransform(ac.canvTrans);
//
//                        ac.repaint(ac.getNetFlipX()?ac.getWidth()-clipWidth-clipX:clipX, ac.getNetFlipY()?ac.getHeight()-clipHeight-clipY:clipY, clipWidth, clipHeight);
//                        }
//                    };
//                tt.start();
//                Thread.yield();
//                }


    void buildAstroWindow() {
        mainMenuBar = new MenuBar();
//                JPopupMenu.setDefaultLightWeightPopupEnabled(false);

//------FILE menu---------------------------------------------------------------------

        fileMenu = new Menu("   File");

        openMenuItem = new MenuItem("Open image in this window...");
        openMenuItem.addActionListener(this);
        if (stackSize != 1) openMenuItem.setEnabled(false);
        fileMenu.add(openMenuItem);

        openInNewWindowMenuItem = new MenuItem("Open image in new window...");
        openInNewWindowMenuItem.addActionListener(this);
        fileMenu.add(openInNewWindowMenuItem);

//                openSeqMenuItem = new MenuItem("Open sequence...");
//                openSeqMenuItem.addActionListener(this);
//                fileMenu.add(openSeqMenuItem);

        openSeqInNewWindowMenuItem = new MenuItem("Open image sequence in new window...");
        openSeqInNewWindowMenuItem.addActionListener(this);
        fileMenu.add(openSeqInNewWindowMenuItem);

        fileMenu.addSeparator();

        openMeasurementsTableMenuItem = new MenuItem("Open data file...");
        openMeasurementsTableMenuItem.addActionListener(this);
        fileMenu.add(openMeasurementsTableMenuItem);

        fileMenu.addSeparator();

        saveDisplayAsPngMenuItem = new MenuItem("Save image display as PNG...");
        saveDisplayAsPngMenuItem.addActionListener(this);
        fileMenu.add(saveDisplayAsPngMenuItem);

        saveDisplayAsJpgMenuItem = new MenuItem("Save image display as JPEG...");
        saveDisplayAsJpgMenuItem.addActionListener(this);
        fileMenu.add(saveDisplayAsJpgMenuItem);

        saveDisplayAsPdfMenuItem = new MenuItem("Save image display as PDF...");
        saveDisplayAsPdfMenuItem.addActionListener(this);
        fileMenu.add(saveDisplayAsPdfMenuItem);

        fileMenu.addSeparator();

        MenuItem createNEBReportMenuItem = new MenuItem("Create NEB search reports and plots...");
        createNEBReportMenuItem.addActionListener(e -> {
            Executors.newSingleThreadExecutor().submit(() -> Macro_Runner.runMacroFromJar(getClass().getClassLoader(), "Astronomy/NEBSearchMacro.txt", ""));
        });
        fileMenu.add(createNEBReportMenuItem);

//                MenuItem createDmagVsRMSPlotMenuItem = new MenuItem("Create Delta-magnitude vs. RMS plot...");
//                createDmagVsRMSPlotMenuItem.addActionListener(new ActionListener() {
//                    public void actionPerformed(ActionEvent e) {
//                            Macro_Runner.runMacroFromJar("DmagVsRMSplotMacro.txt",""); }});
//                fileMenu.add(createDmagVsRMSPlotMenuItem);
//
//                MenuItem createNEBLCPlotMenuItem = new MenuItem("Create NEB light curve plots...");
//                createNEBLCPlotMenuItem.addActionListener(new ActionListener() {
//                    public void actionPerformed(ActionEvent e) {
//                            Macro_Runner.runMacroFromJar("NEBLightCurvePlotWithPredDepth.txt",""); }});
//                fileMenu.add(createNEBLCPlotMenuItem);

        fileMenu.addSeparator();

        openAperturesMenuItem = new MenuItem("Open apertures...");
        openAperturesMenuItem.addActionListener(this);
        fileMenu.add(openAperturesMenuItem);

        saveAperturesMenuItem = new MenuItem("Save apertures...");
        saveAperturesMenuItem.addActionListener(this);
        fileMenu.add(saveAperturesMenuItem);

        openRaDecAperturesMenuItem = new MenuItem("Import apertures from RA/Dec list...");
        openRaDecAperturesMenuItem.addActionListener(this);
        fileMenu.add(openRaDecAperturesMenuItem);

        saveRaDecAperturesMenuItem = new MenuItem("Export apertures to RA/Dec list...");
        saveRaDecAperturesMenuItem.addActionListener(this);
        fileMenu.add(saveRaDecAperturesMenuItem);

//                fileMenu.addSeparator();
//
//                saveStatePNGMenuItem = new MenuItem("Save all...");
//                saveStatePNGMenuItem.addActionListener(this);
//                fileMenu.add(saveStatePNGMenuItem);
//
//                setSaveStateMenuItem = new MenuItem("Save all (with options)...");
//                setSaveStateMenuItem.addActionListener(this);
//                fileMenu.add(setSaveStateMenuItem);

        fileMenu.addSeparator();

        saveMenuItem = new MenuItem("Save image");
        saveMenuItem.addActionListener(this);
        fileMenu.add(saveMenuItem);

        // FITS saving
        var fitsMenu = new Menu("Save as FITS...");
        fileMenu.add(fitsMenu);

        // Slice saving
        saveFitsMenuItem = new Menu("Save image/slice as FITS...");
        saveFitsMenuItem.addActionListener(this);

        var sliceSaveNC = new MenuItem("No compression");
        sliceSaveNC.addActionListener($ ->
                FITS_Writer.savingThread.submit(() -> {
                    var l = imp.lockSilently();
                    FITS_Writer.saveImage(imp, null, imp.getCurrentSlice());
                    if (l) imp.unlock();
                }));
        saveFitsMenuItem.add(sliceSaveNC);

        var sliceSaveFz = new MenuItem("FPACK");
        sliceSaveFz.addActionListener($ ->
                FITS_Writer.savingThread.submit(() -> {
                    var l = imp.lockSilently();
                    FITS_Writer.saveImage(imp, null, imp.getCurrentSlice(), ".fits.fz");
                    if (l) imp.unlock();
                }));
        saveFitsMenuItem.add(sliceSaveFz);

        var sliceSaveGz = new MenuItem("GZip");
        sliceSaveGz.addActionListener($ ->
                FITS_Writer.savingThread.submit(() -> {
                    var l = imp.lockSilently();
                    FITS_Writer.saveImage(imp, null, imp.getCurrentSlice(), ".fits.gz");
                    if (l) imp.unlock();
                }));
        saveFitsMenuItem.add(sliceSaveGz);

        var sliceSaveFzGz = new MenuItem("FPACK and GZip");
        sliceSaveFzGz.addActionListener($ ->
                FITS_Writer.savingThread.submit(() -> {
                    var l = imp.lockSilently();
                    FITS_Writer.saveImage(imp, null, imp.getCurrentSlice(), ".fits.fz.gz");//todo this is doubled
                    if (l) imp.unlock();
                }));
        saveFitsMenuItem.add(sliceSaveFzGz);
        fitsMenu.add(saveFitsMenuItem);

        // Stack 3D Saving
        saveFitsStack3DMenuItem = new Menu("Save image/stack as 3D FITS...");
        saveFitsStack3DMenuItem.addActionListener(this);

        var stackSave3DNC = new MenuItem("No compression");
        stackSave3DNC.addActionListener($ ->
                FITS_Writer.savingThread.submit(() -> {
                    var l = imp.lockSilently();
                    FITS_Writer.saveImage(imp, null);
                    if (l) imp.unlock();
                }));
        saveFitsStack3DMenuItem.add(stackSave3DNC);

        var stackSave3DFz = new MenuItem("FPACK");
        stackSave3DFz.addActionListener($ ->
                FITS_Writer.savingThread.submit(() -> {
                    var l = imp.lockSilently();
                    FITS_Writer.saveImage(imp, null,".fits.fz");
                    if (l) imp.unlock();
                }));
        saveFitsStack3DMenuItem.add(stackSave3DFz);

        var stackSave3DGz = new MenuItem("GZip");
        stackSave3DGz.addActionListener($ ->
                FITS_Writer.savingThread.submit(() -> {
                    var l = imp.lockSilently();
                    FITS_Writer.saveImage(imp, null, ".fits.gz");
                    if (l) imp.unlock();
                }));
        saveFitsStack3DMenuItem.add(stackSave3DGz);

        var stackSave3DFzGz = new MenuItem("FPACK and GZip");
        stackSave3DFzGz.addActionListener($ ->
                FITS_Writer.savingThread.submit(() -> {
                    var l = imp.lockSilently();
                    FITS_Writer.saveImage(imp, null, ".fits.fz.gz");
                    if (l) imp.unlock();
                }));
        saveFitsStack3DMenuItem.add(stackSave3DFzGz);
        fitsMenu.add(saveFitsStack3DMenuItem);

        saveFitsStackMenuItem = new Menu("Save image/stack as folder...");
        saveFitsStackMenuItem.addActionListener(this);

        // Stack Folder Saving
        var stackSaveNC = new MenuItem("No compression");
        stackSaveNC.addActionListener($ ->
                FITS_Writer.savingThread.submit(() -> {
                    var l = imp.lockSilently();
                    FITS_Writer.saveFolder(imp, null, ".fits");
                    if (l) imp.unlock();
                }));
        saveFitsStackMenuItem.add(stackSaveNC);

        var stackSaveFz = new MenuItem("FPACK");
        stackSaveFz.addActionListener($ ->
                FITS_Writer.savingThread.submit(() -> {
                    var l = imp.lockSilently();
                    FITS_Writer.saveFolder(imp, null,".fits.fz");
                    if (l) imp.unlock();
                }));
        saveFitsStackMenuItem.add(stackSaveFz);

        var stackSaveGz = new MenuItem("GZip");
        stackSaveGz.addActionListener($ ->
                FITS_Writer.savingThread.submit(() -> {
                    var l = imp.lockSilently();
                    FITS_Writer.saveFolder(imp, null, ".fits.gz");
                    if (l) imp.unlock();
                }));
        saveFitsStackMenuItem.add(stackSaveGz);

        var stackSaveFzGz = new MenuItem("FPACK and GZip");
        stackSaveFzGz.addActionListener($ ->
                FITS_Writer.savingThread.submit(() -> {
                    var l = imp.lockSilently();
                    FITS_Writer.saveFolder(imp, null, ".fits.fz.gz");
                    if (l) imp.unlock();
                }));
        saveFitsStackMenuItem.add(stackSaveFzGz);
        fitsMenu.add(saveFitsStackMenuItem);

        var fpackSettings = new MenuItem("Fits Compression (FPACK) Settings");
        fpackSettings.addActionListener($ -> FitsCompressionUtil.dialog());
        fitsMenu.add(fpackSettings);

        saveTiffMenuItem = new MenuItem("Save image/stack as TIFF...");
        saveTiffMenuItem.addActionListener(this);
        fileMenu.add(saveTiffMenuItem);

        saveJpegMenuItem = new MenuItem("Save image/slice as JPEG...");
        saveJpegMenuItem.addActionListener(this);
        fileMenu.add(saveJpegMenuItem);

        savePdfMenuItem = new MenuItem("Save image/slice as PDF...");
        savePdfMenuItem.addActionListener(this);
        fileMenu.add(savePdfMenuItem);

        saveGifMenuItem = new MenuItem("Save image/stack as GIF...");
        saveGifMenuItem.addActionListener(this);
        fileMenu.add(saveGifMenuItem);

        savePngMenuItem = new MenuItem("Save image/slice as PNG...");
        savePngMenuItem.addActionListener(this);
        fileMenu.add(savePngMenuItem);

        saveBmpMenuItem = new MenuItem("Save image/slice as BMP...");
        saveBmpMenuItem.addActionListener(this);
        fileMenu.add(saveBmpMenuItem);

        saveAviMenuItem = new MenuItem("Save image/stack as AVI...");
        saveAviMenuItem.addActionListener(this);
        fileMenu.add(saveAviMenuItem);

        saveStackSequenceMenuItem = new MenuItem("Save stack as sequence...");
        saveStackSequenceMenuItem.addActionListener(this);
        fileMenu.add(saveStackSequenceMenuItem);

        fileMenu.addSeparator();

        backupAllAIJPrefsMenuItem = new MenuItem("Save all AIJ preferences to backup file...");
        backupAllAIJPrefsMenuItem.addActionListener(this);
        fileMenu.add(backupAllAIJPrefsMenuItem);

        restoreAllAIJPrefsMenuItem = new MenuItem("Restore all AIJ preferences from backup file...");
        restoreAllAIJPrefsMenuItem.addActionListener(this);
        fileMenu.add(restoreAllAIJPrefsMenuItem);

        restoreDefaultAIJPrefsMenuItem = new MenuItem("Restore all default AIJ preferences...");
        restoreDefaultAIJPrefsMenuItem.addActionListener(this);
        fileMenu.add(restoreDefaultAIJPrefsMenuItem);

        fileMenu.addSeparator();

        exitMenuItem = new MenuItem("Close Window");
        exitMenuItem.addActionListener(this);
        fileMenu.add(exitMenuItem);

        mainMenuBar.add(fileMenu);

//------Preferences menu---------------------------------------------------------------------

        preferencesMenu = new Menu("Preferences");

        autoConvertCB = new CheckboxMenuItem("Use astro-window when images are opened", autoConvert);
        autoConvertCB.addItemListener(this);
        preferencesMenu.add(autoConvertCB);

        preferencesMenu.addSeparator();

        usePreviousSizeCB = new CheckboxMenuItem("Use previous window size", startupPrevSize);
        usePreviousSizeCB.addItemListener(this);
        preferencesMenu.add(usePreviousSizeCB);

        usePreviousPanCB = new CheckboxMenuItem("Use previous pan position", startupPrevPan);
        usePreviousPanCB.addItemListener(this);
        preferencesMenu.add(usePreviousPanCB);

        usePreviousZoomCB = new CheckboxMenuItem("Use previous zoom setting", startupPrevZoom);
        usePreviousZoomCB.addItemListener(this);
//                preferencesMenu.add(usePreviousZoomCB);

        rememberWindowLocationCB = new CheckboxMenuItem("Use previous window location", rememberWindowLocation);
        rememberWindowLocationCB.addItemListener(this);
        preferencesMenu.add(rememberWindowLocationCB);

        preferencesMenu.addSeparator();

        showMeanNotPeakCB = new CheckboxMenuItem("Display mean counts in aperture (deselect to show peak)", showMeanNotPeak);
        showMeanNotPeakCB.addItemListener(this);
        preferencesMenu.add(showMeanNotPeakCB);

        useSexagesimalCB = new CheckboxMenuItem("Display in sexagesimal format", useSexagesimal);
        useSexagesimalCB.addItemListener(this);
        preferencesMenu.add(useSexagesimalCB);

        middleClickCenterCB = new CheckboxMenuItem("Middle click centers image at click location", middleClickCenter);
        middleClickCenterCB.addItemListener(this);
        preferencesMenu.add(middleClickCenterCB);

        preferencesMenu.addSeparator();

        showPhotometerCB = new CheckboxMenuItem("Show photometer aperture at mouse cursor", showPhotometer);
        showPhotometerCB.addItemListener(this);
        preferencesMenu.add(showPhotometerCB);

        showRedCrossHairCursorCB = new CheckboxMenuItem("Show cross-hair with photometer aperture at mouse cursor", ac.showRedCrossHairCursor);
        showRedCrossHairCursorCB.addItemListener(this);
        preferencesMenu.add(showRedCrossHairCursorCB);

        var plotPixelVals = new CheckboxMenuItem("Plot pixel values from all stack slices at mouse cursor (or hold <Shift><Contol>)", plotStackPixelValues.get());
        plotPixelVals.addItemListener(e -> plotStackPixelValues.set(plotPixelVals.getState()));
        preferencesMenu.add(plotPixelVals);

        removeBackStarsCB = new CheckboxMenuItem("Ignore pixels > 2 sigma from mean in photometer background region", removeBackStars);
        removeBackStarsCB.addItemListener(this);
        preferencesMenu.add(removeBackStarsCB);

        showRemovedPixelsCB = new CheckboxMenuItem("Mark pixels > 2 sigma from mean in photometer background region", showRemovedPixels);
        showRemovedPixelsCB.addItemListener(this);
        preferencesMenu.add(showRemovedPixelsCB);

        preferencesMenu.addSeparator();

        fpackSettings = new MenuItem("Fits Compression (FPACK) Settings");
        fpackSettings.addActionListener($ -> FitsCompressionUtil.dialog());
        preferencesMenu.add(fpackSettings);

        preferencesMenu.addSeparator();

        var qualCheckTess = new CheckboxMenuItem("When opening TESS images, also open images with data quality flag set",
                FITS_Reader.skipTessQualCheck);
        qualCheckTess.addItemListener(e -> {
            FITS_Reader.skipTessQualCheck = e.getStateChange() == ItemEvent.SELECTED;
            Prefs.set("aij.skipTessQualCheck", FITS_Reader.skipTessQualCheck);
        });
        preferencesMenu.add(qualCheckTess);

        preferencesMenu.addSeparator();
        var logInNewWindows = new CheckboxMenuItem("When logging, separate logs based on the task logging them",
                Prefs.getBoolean(AIJLogger.USE_NEW_LOG_WINDOW_KEY, true));
        logInNewWindows.addItemListener(e -> Prefs.set(AIJLogger.USE_NEW_LOG_WINDOW_KEY.substring(1), e.getStateChange() == ItemEvent.SELECTED));
        var logAutoClose = new CheckboxMenuItem("Separate log windows auto-close for some tasks",
                Prefs.getBoolean(AIJLogger.CERTAIN_LOGS_AUTO_CLOSE, true));
        logAutoClose.addItemListener(e -> Prefs.set(AIJLogger.CERTAIN_LOGS_AUTO_CLOSE.substring(1), e.getStateChange() == ItemEvent.SELECTED));
        preferencesMenu.add(logInNewWindows);
        preferencesMenu.add(logAutoClose);

        mainMenuBar.add(preferencesMenu);

//------SCALE menu---------------------------------------------------------------------

        scaleMenu = new Menu("Contrast");

        scaleMenu.add("--When an image is opened or modified use:--");

        autoContrastRB = new CheckboxMenuItem("auto brightness & contrast", autoContrast);
        autoContrastRB.addItemListener(this);
        scaleMenu.add(autoContrastRB);

        fixedContrastRB = new CheckboxMenuItem("fixed brightness & contrast", !autoContrast && fixedContrast);
        fixedContrastRB.addItemListener(this);
        scaleMenu.add(fixedContrastRB);

        fixedContrastPerSliceRB = new CheckboxMenuItem("fixed brightness & contrast (per image slice)", !autoContrast && !fixedContrast && fixedContrastPerSlice);
        fixedContrastPerSliceRB.addItemListener(this);
        scaleMenu.add(fixedContrastPerSliceRB);

        useFullRangeRB = new CheckboxMenuItem("full dynamic range", !autoContrast && !fixedContrast && !fixedContrastPerSlice);
        useFullRangeRB.addItemListener(this);
        scaleMenu.add(useFullRangeRB);

        scaleMenu.addSeparator();

        useFixedMinMaxValuesCB = new CheckboxMenuItem("Use fixed min and max histogram values", useFixedMinMaxValues);
        useFixedMinMaxValuesCB.addItemListener(this);
        scaleMenu.add(useFixedMinMaxValuesCB);

        scaleMenu.addSeparator();

        autoGrabBandCFromHistogramCB = new CheckboxMenuItem("Update auto-contrast thresholds when histogram range is changed", autoGrabBandCFromHistogram);
        autoGrabBandCFromHistogramCB.addItemListener(this);
        scaleMenu.add(autoGrabBandCFromHistogramCB);

        scaleMenu.addSeparator();

        setAutoScaleParametersMenuItem = new MenuItem("Set auto brightness & contrast parameters...");
        setAutoScaleParametersMenuItem.addActionListener(this);
        scaleMenu.add(setAutoScaleParametersMenuItem);

        grabAutoScaleParametersMenuItem = new MenuItem("Grab auto brightness & contrast from histogram");
        grabAutoScaleParametersMenuItem.addActionListener(this);
        scaleMenu.add(grabAutoScaleParametersMenuItem);

        resetAutoScaleParametersMenuItem = new MenuItem("Reset auto brightness & contrast to defaults");
        resetAutoScaleParametersMenuItem.addActionListener(this);
        scaleMenu.add(resetAutoScaleParametersMenuItem);

        mainMenuBar.add(scaleMenu);

//------VIEW menu---------------------------------------------------------------------

        viewMenu = new Menu("View");

        clearOverlayMenuItem = new MenuItem("Clear Overlay");
        clearOverlayMenuItem.addActionListener(this);
        viewMenu.add(clearOverlayMenuItem);

        viewMenu.addSeparator();

        autoNupEleftRB = new CheckboxMenuItem("Auto WCS North Up, East Left", autoNupEleft);
        autoNupEleftRB.addItemListener(this);
        viewMenu.add(autoNupEleftRB);

        viewMenu.addSeparator();

        invertNoneRB = new CheckboxMenuItem("Invert None", !invertX && !invertY);
        invertNoneRB.addItemListener(this);
        viewMenu.add(invertNoneRB);

        invertXRB = new CheckboxMenuItem("Invert X", invertX && !invertY);
        invertXRB.addItemListener(this);
        viewMenu.add(invertXRB);

        invertYRB = new CheckboxMenuItem("Invert Y", !invertX && invertY);
        invertYRB.addItemListener(this);
        viewMenu.add(invertYRB);

        invertXYRB = new CheckboxMenuItem("Invert X and Y", invertX && invertY);
        invertXYRB.addItemListener(this);
        viewMenu.add(invertXYRB);

//                invertGroup = new ButtonGroup();
//                invertGroup.add(invertNoneRB);
//                invertGroup.add(invertXRB);
//                invertGroup.add(invertYRB);
//                invertGroup.add(invertXYRB);

        viewMenu.addSeparator();

        rotate0RB = new CheckboxMenuItem("0 degrees", rotation == AstroCanvas.ROT_0);
        rotate0RB.addItemListener(this);
        viewMenu.add(rotate0RB);

        rotate90RB = new CheckboxMenuItem("90 degrees", rotation == AstroCanvas.ROT_90);
        rotate90RB.setEnabled(false);
        rotate90RB.addItemListener(this);
        viewMenu.add(rotate90RB);

        rotate180RB = new CheckboxMenuItem("180 degrees", rotation == AstroCanvas.ROT_180);
        rotate180RB.addItemListener(this);
        rotate180RB.setEnabled(true);
        viewMenu.add(rotate180RB);

        rotate270RB = new CheckboxMenuItem("270 degrees", rotation == AstroCanvas.ROT_270);
        rotate270RB.addItemListener(this);
        rotate270RB.setEnabled(false);
        viewMenu.add(rotate270RB);

        viewMenu.addSeparator();

        showZoomCB = new CheckboxMenuItem("Show zoom indicator in overlay", showZoom);
        showZoomCB.addItemListener(this);
        viewMenu.add(showZoomCB);

        setZoomIndicatorSizeMenuItem = new MenuItem("Set zoom indicator size...");
        setZoomIndicatorSizeMenuItem.addActionListener(this);
        viewMenu.add(setZoomIndicatorSizeMenuItem);

        viewMenu.addSeparator();

        showDirCB = new CheckboxMenuItem("Show north & east in overlay", showDir);
        showDirCB.addItemListener(this);
        viewMenu.add(showDirCB);

        showXYCB = new CheckboxMenuItem("Show x-dir & y-dir in overlay", showXY);
        showXYCB.addItemListener(this);
        viewMenu.add(showXYCB);

        showScaleXCB = new CheckboxMenuItem("Show x-dir arclength scale in overlay", showScaleX);
        showScaleXCB.addItemListener(this);
        viewMenu.add(showScaleXCB);

        showScaleYCB = new CheckboxMenuItem("Show y-dir arclength scale in overlay", showScaleY);
        showScaleYCB.addItemListener(this);
        viewMenu.add(showScaleYCB);

        viewMenu.addSeparator();

        showAbsMagCB = new CheckboxMenuItem("Show apparent magnitude in aperture quantites (if entered)", showAbsMag);
        showAbsMagCB.addItemListener(this);
        viewMenu.add(showAbsMagCB);

        showIntCntWithAbsMagCB = new CheckboxMenuItem("Also show integrated counts when magnitude is displayed", showIntCntWithAbsMag);
        showIntCntWithAbsMagCB.addItemListener(this);
        viewMenu.add(showIntCntWithAbsMagCB);

//                rotationGroup = new ButtonGroup();
//                rotationGroup.add(rotate0RB);
//                rotationGroup.add(rotate90RB);
//                rotationGroup.add(rotate180RB);
//                rotationGroup.add(rotate270RB);

        mainMenuBar.add(viewMenu);

//------Annotate menu---------------------------------------------------------------------

        annotateMenu = new Menu("Annotate");

        rightClickAnnotateCB = new CheckboxMenuItem("Right click in image opens annotation dialog", rightClickAnnotate);
        rightClickAnnotateCB.addItemListener(this);
        annotateMenu.add(rightClickAnnotateCB);

        showInSimbadCB = new CheckboxMenuItem("Right click shows SIMBAD objects(s) in web browser (if has WCS)", useSimbadSearch);
        showInSimbadCB.addItemListener(this);
        annotateMenu.add(showInSimbadCB);


        useSimbadSearchCB = new CheckboxMenuItem("Search SIMBAD for object names when opening annotate dialog (if has WCS)", useSimbadSearch);
        useSimbadSearchCB.addItemListener(this);
        annotateMenu.add(useSimbadSearchCB);


        simbadSearchRadiusMenuItem = new MenuItem("Set SIMBAD search radius (currently " + simbadSearchRadius + " arcsec)...");
        simbadSearchRadiusMenuItem.addActionListener(this);
        annotateMenu.add(simbadSearchRadiusMenuItem);

        defaultAnnotationColorMenuItem = new MenuItem("Set default annotation color (currently '" + defaultAnnotationColor + "')...");
        defaultAnnotationColorMenuItem.addActionListener(this);
        annotateMenu.add(defaultAnnotationColorMenuItem);

        annotateMenu.addSeparator();

        annotateMenuItem = new MenuItem("Annotate last clicked location (or right-click location)...");
        annotateMenuItem.addActionListener(this);
        annotateMenu.add(annotateMenuItem);

        editAnnotationMenuItem = new MenuItem("Edit annotation at last clicked location (or right-click annotation)...");
        editAnnotationMenuItem.addActionListener(this);
        annotateMenu.add(editAnnotationMenuItem);

        deleteAnnotationMenuItem = new MenuItem("Remove annotation at last clicked location (or right-click annotation)...");
        deleteAnnotationMenuItem.addActionListener(this);
        annotateMenu.add(deleteAnnotationMenuItem);

        clearAllAnnotateRoisMenuItem = new MenuItem("Remove all annotations from display");
        clearAllAnnotateRoisMenuItem.addActionListener(this);
        annotateMenu.add(clearAllAnnotateRoisMenuItem);

        annotateMenu.addSeparator();

        annotateFromHeaderMenuItem = new MenuItem("Display (replace) annotations from FITS header");
        annotateFromHeaderMenuItem.addActionListener(this);
        annotateMenu.add(annotateFromHeaderMenuItem);

        annotateAppendFromHeaderMenuItem = new MenuItem("Display (append) annotations from FITS header");
        annotateAppendFromHeaderMenuItem.addActionListener(this);
        annotateMenu.add(annotateAppendFromHeaderMenuItem);

        replaceAnnotationsInHeaderMenuItem = new MenuItem("Replace annotations in FITS header with displayed annotations");
        replaceAnnotationsInHeaderMenuItem.addActionListener(this);
        annotateMenu.add(replaceAnnotationsInHeaderMenuItem);

        appendToAnnotationsInHeaderMenuItem = new MenuItem("Append to annotations in FITS header the displayed annotations");
        appendToAnnotationsInHeaderMenuItem.addActionListener(this);
        annotateMenu.add(appendToAnnotationsInHeaderMenuItem);

        deleteAnnotationsFromHeaderMenuItem = new MenuItem("Remove all annotations from FITS header");
        deleteAnnotationsFromHeaderMenuItem.addActionListener(this);
        annotateMenu.add(deleteAnnotationsFromHeaderMenuItem);

        annotateMenu.addSeparator();


        autoUpdateAnnotationsInHeaderCB = new CheckboxMenuItem("Auto update annotations in FITS header", autoUpdateAnnotationsInHeader);
        autoUpdateAnnotationsInHeaderCB.addItemListener(this);
        annotateMenu.add(autoUpdateAnnotationsInHeaderCB);

        autoDisplayAnnotationsFromHeaderCB = new CheckboxMenuItem("Display annotations from FITS header when image is displayed", autoDisplayAnnotationsFromHeader);
        autoDisplayAnnotationsFromHeaderCB.addItemListener(this);
        annotateMenu.add(autoDisplayAnnotationsFromHeaderCB);


        mainMenuBar.add(annotateMenu);

//-----MEASURE menu-------------------------------------------------------------------

        measureMenu = new Menu("Measure");

        measureMenu.add("--Middle-drag, right-drag, or alt-left-drag to measure--");

        writeMiddleClickValuesTableCB = new CheckboxMenuItem("Middle click writes measurement data to table", writeMiddleClickValuesTable);
        writeMiddleClickValuesTableCB.addItemListener(this);
        measureMenu.add(writeMiddleClickValuesTableCB);

        writeMiddleDragValuesTableCB = new CheckboxMenuItem("Middle or Right drag writes measurement data to table", writeMiddleDragValuesTable);
        writeMiddleDragValuesTableCB.addItemListener(this);
        measureMenu.add(writeMiddleDragValuesTableCB);

        writeMiddleClickValuesLogCB = new CheckboxMenuItem("Middle click writes measurement data to log window", writeMiddleClickValuesLog);
        writeMiddleClickValuesLogCB.addItemListener(this);
        measureMenu.add(writeMiddleClickValuesLogCB);

        writeMiddleDragValuesLogCB = new CheckboxMenuItem("Middle or Right drag writes measurement data to log window", writeMiddleDragValuesLog);
        writeMiddleDragValuesLogCB.addItemListener(this);
        measureMenu.add(writeMiddleDragValuesLogCB);

        measureMenu.addSeparator();

        defaultMeasurementColorMenuItem = new MenuItem("Set default measurement color (currently '" + defaultMeasurementColor + "')...");
        defaultMeasurementColorMenuItem.addActionListener(this);
        measureMenu.add(defaultMeasurementColorMenuItem);

        showMeasureSexCB = new CheckboxMenuItem("Display and write to log in sexagesimal", showMeasureSex);
        showMeasureSexCB.addItemListener(this);
        measureMenu.add(showMeasureSexCB);

        showMeasureCircleCB = new CheckboxMenuItem("Display aperture circles at ends of measurement", showMeasureCircle);
        showMeasureCircleCB.addItemListener(this);
        measureMenu.add(showMeasureCircleCB);

        showMeasureCrosshairCB = new CheckboxMenuItem("Display crosshairs at ends of measurement if centroided", showMeasureCrosshair);
        showMeasureCrosshairCB.addItemListener(this);
        measureMenu.add(showMeasureCrosshairCB);

        showMeasureMultiLinesCB = new CheckboxMenuItem("Display measurements on multiple lines if too long", showMeasureMultiLines);
        showMeasureMultiLinesCB.addItemListener(this);
        measureMenu.add(showMeasureMultiLinesCB);

        measureMenu.addSeparator();

        negateMeasureDelMagCB = new CheckboxMenuItem("Negate delta magnitude of measurement", negateMeasureDelMag);
        negateMeasureDelMagCB.addItemListener(this);
        measureMenu.add(negateMeasureDelMagCB);

        measureMenu.addSeparator();

        showMeasureLengthCB = new CheckboxMenuItem("Display arclength", showMeasureLength);
        showMeasureLengthCB.addItemListener(this);
        measureMenu.add(showMeasureLengthCB);

        showMeasurePACB = new CheckboxMenuItem("Display position angle", showMeasurePA);
        showMeasurePACB.addItemListener(this);
        measureMenu.add(showMeasurePACB);

        showMeasureDelMagCB = new CheckboxMenuItem("Display delta magnitude", showMeasureDelMag);
        showMeasureDelMagCB.addItemListener(this);
        measureMenu.add(showMeasureDelMagCB);

        showMeasureFluxRatioCB = new CheckboxMenuItem("Display flux ratio", showMeasureFluxRatio);
        showMeasureFluxRatioCB.addItemListener(this);
        measureMenu.add(showMeasureFluxRatioCB);

        measureMenu.addSeparator();

        writeMeasureLengthLogCB = new CheckboxMenuItem("Write arclength to log window", writeMeasureLengthLog);
        writeMeasureLengthLogCB.addItemListener(this);
        measureMenu.add(writeMeasureLengthLogCB);

        writeMeasureLengthTableDegCB = new CheckboxMenuItem("Write arclength in degrees to table", writeMeasureLengthTableDeg);
        writeMeasureLengthTableDegCB.addItemListener(this);
        measureMenu.add(writeMeasureLengthTableDegCB);

        writeMeasureLengthTableMinCB = new CheckboxMenuItem("Write arclength in minutes to table", writeMeasureLengthTableMin);
        writeMeasureLengthTableMinCB.addItemListener(this);
        measureMenu.add(writeMeasureLengthTableMinCB);

        writeMeasureLengthTableSecCB = new CheckboxMenuItem("Write arclength in seconds to table", writeMeasureLengthTableSec);
        writeMeasureLengthTableSecCB.addItemListener(this);
        measureMenu.add(writeMeasureLengthTableSecCB);

        writeMeasurePACB = new CheckboxMenuItem("Write position angle to table and log", writeMeasurePA);
        writeMeasurePACB.addItemListener(this);
        measureMenu.add(writeMeasurePACB);

        writeMeasureDelMagCB = new CheckboxMenuItem("Write delta magnitude to table and log", writeMeasureDelMag);
        writeMeasureDelMagCB.addItemListener(this);
        measureMenu.add(writeMeasureDelMagCB);

        writeMeasureFluxRatioCB = new CheckboxMenuItem("Write flux ratio to table and log", writeMeasureFluxRatio);
        writeMeasureFluxRatioCB.addItemListener(this);
        measureMenu.add(writeMeasureFluxRatioCB);

        writePhotometricDataTableCB = new CheckboxMenuItem("Write photometry to table (select in 'More Aperture Settings')", writePhotometricDataTable);
        writePhotometricDataTableCB.addItemListener(this);
        measureMenu.add(writePhotometricDataTableCB);

        mainMenuBar.add(measureMenu);

//------EDIT menu---------------------------------------------------------------------

        editMenu = new Menu("Edit");

        apertureSettingsMenuItem = new MenuItem("Aperture settings...");
        apertureSettingsMenuItem.addActionListener(this);
        editMenu.add(apertureSettingsMenuItem);

        measurementSettingsMenuItem = new MenuItem("Measurement settings...");
        measurementSettingsMenuItem.addActionListener(this);
        editMenu.add(measurementSettingsMenuItem);

        editFitsHeaderMenuItem = new MenuItem("Edit FITS header...");
        editFitsHeaderMenuItem.addActionListener(this);
        editMenu.add(editFitsHeaderMenuItem);

        copyFitsHeaderMenuItem = new MenuItem("Copy FITS header to this image...");
        copyFitsHeaderMenuItem.addActionListener(this);
        editMenu.add(copyFitsHeaderMenuItem);

        stackSorterMenuItem = new MenuItem("Stack...");
        stackSorterMenuItem.addActionListener(this);
        editMenu.add(stackSorterMenuItem);

        mainMenuBar.add(editMenu);


//------Process menu---------------------------------------------------------------------

        processMenu = new Menu("Process");
        processMenu.add("**Warning - these selections may modify your image data**");

        processMenu.addSeparator();

        dataReducerMenuItem = new MenuItem("Data reduction facility...");
        dataReducerMenuItem.addActionListener(this);
        processMenu.add(dataReducerMenuItem);

        processMenu.addSeparator();

        combineStackImagesMenuItem = new MenuItem("Combine stack slices into single image...");
        combineStackImagesMenuItem.addActionListener(this);
        processMenu.add(combineStackImagesMenuItem);

        concatStacksMenuItem = new MenuItem("Combine stacks into single stack...");
        concatStacksMenuItem.addActionListener(this);
        processMenu.add(concatStacksMenuItem);

        imageCalcMenuItem = new MenuItem("Image/stack calculator...");
        imageCalcMenuItem.addActionListener(this);
        processMenu.add(imageCalcMenuItem);

        copyFitsHeaderProcessMenuItem = new MenuItem("Copy FITS header to this image...");
        copyFitsHeaderProcessMenuItem.addActionListener(this);
        processMenu.add(copyFitsHeaderProcessMenuItem);

        removeOutliersMenuItem = new MenuItem("Remove outliers from image/stack...");
        removeOutliersMenuItem.addActionListener(this);
        processMenu.add(removeOutliersMenuItem);

        smoothMenuItem = new MenuItem("Smooth image/stack...");
        smoothMenuItem.addActionListener(this);
        processMenu.add(smoothMenuItem);

        sharpenMenuItem = new MenuItem("Sharpen image/stack...");
        sharpenMenuItem.addActionListener(this);
        processMenu.add(sharpenMenuItem);

        normalizeStackMenuItem = new MenuItem("Normalize image/stack...");
        normalizeStackMenuItem.addActionListener(this);
        processMenu.add(normalizeStackMenuItem);

        processMenu.addSeparator();

        alignStackMenuItem = new MenuItem("Align stack using WCS or apertures...");
        alignStackMenuItem.addActionListener(this);
        processMenu.add(alignStackMenuItem);

        imageStabilizerMenuItem = new MenuItem("Align stack using image stabilizer...");
        imageStabilizerMenuItem.addActionListener(this);
        processMenu.add(imageStabilizerMenuItem);

        imageStabilizerApplyMenuItem = new MenuItem("Apply image stabilizer coefficients...");
        imageStabilizerApplyMenuItem.addActionListener(this);
        processMenu.add(imageStabilizerApplyMenuItem);

        shiftImageMenuItem = new MenuItem("Shift image manually...");
        shiftImageMenuItem.addActionListener(this);
        processMenu.add(shiftImageMenuItem);

        selectBestFramesMenuItem = new MenuItem("Select stack images with best focus...");
        selectBestFramesMenuItem.addActionListener(this);
        processMenu.add(selectBestFramesMenuItem);

        processMenu.addSeparator();

        flipDataXMenuItem = new MenuItem("Flip data in x-axis");
        flipDataXMenuItem.addActionListener(this);
        processMenu.add(flipDataXMenuItem);

        flipDataYMenuItem = new MenuItem("Flip data in y-axis");
        flipDataYMenuItem.addActionListener(this);
        processMenu.add(flipDataYMenuItem);

        rotateDataCWMenuItem = new MenuItem("Rotate data 90 degrees clockwise");
        rotateDataCWMenuItem.addActionListener(this);
        processMenu.add(rotateDataCWMenuItem);

        rotateDataCCWMenuItem = new MenuItem("Rotate data 90 degrees counter-clockwise");
        rotateDataCCWMenuItem.addActionListener(this);
        processMenu.add(rotateDataCCWMenuItem);

        mainMenuBar.add(processMenu);

//------Color processing menu---------------------------------------------------------------------

        colorMenu = new Menu("Color"); //splitChannelsMenuItem, imagesToStackMenuItem stackToImagesMenuItem

        RGBComposerMenuItem = new MenuItem("RGB Composer");
        RGBComposerMenuItem.addActionListener(this);
        colorMenu.add(RGBComposerMenuItem);

        splitChannelsMenuItem = new MenuItem("Split RBG image into three 8-bit images");
        splitChannelsMenuItem.addActionListener(this);
        colorMenu.add(splitChannelsMenuItem);

        imagesToStackMenuItem = new MenuItem("Images to Stack");
        imagesToStackMenuItem.addActionListener(this);
        colorMenu.add(imagesToStackMenuItem);

        stackToImagesMenuItem = new MenuItem("Stack to Images");
        stackToImagesMenuItem.addActionListener(this);
        colorMenu.add(stackToImagesMenuItem);

        photoDebayerMenuItem = new MenuItem("Debayer to single color/luminosity 1/4 size stack(s)");
        photoDebayerMenuItem.addActionListener(this);
        colorMenu.add(photoDebayerMenuItem);

        debayerMenuItem = new MenuItem("Debayer with demosaicing and smoothing options");
        debayerMenuItem.addActionListener(this);
        colorMenu.add(debayerMenuItem);

        makeCompositeMenuItem = new MenuItem("Make Composite color image");
        makeCompositeMenuItem.addActionListener(this);
        colorMenu.add(makeCompositeMenuItem);

        stackToRGBMenuItem = new MenuItem("Convert RGB stack to color image");
        stackToRGBMenuItem.addActionListener(this);
        colorMenu.add(stackToRGBMenuItem);


        mainMenuBar.add(colorMenu);

//------ANALYZE menu---------------------------------------------------------------------

        analyzeMenu = new Menu("Analyze");

        multiApertureMenuItem = new MenuItem("Multi-aperture...");
        multiApertureMenuItem.addActionListener(this);
        analyzeMenu.add(multiApertureMenuItem);

        multiPlotMenuItem = new MenuItem("Multi-plot...");
        multiPlotMenuItem.addActionListener(this);
        analyzeMenu.add(multiPlotMenuItem);

        measurementMenuItem = new MenuItem("Measure *");
        measurementMenuItem.addActionListener(this);
        analyzeMenu.add(measurementMenuItem);

        analyzeMenu.addSeparator();

        seeingProfileMenuItem = new MenuItem("Plot seeing profile... * (or alt-click star)");
        seeingProfileMenuItem.addActionListener(this);
        analyzeMenu.add(seeingProfileMenuItem);

        staticProfilerMenuItem = new MenuItem("Plot static line/box profile... *");
        staticProfilerMenuItem.addActionListener(this);
        analyzeMenu.add(staticProfilerMenuItem);

        dynamicProfilerMenuItem = new MenuItem("Plot dynamic line/box profile... *");
        dynamicProfilerMenuItem.addActionListener(this);
        analyzeMenu.add(dynamicProfilerMenuItem);

        contourLinesMenuItem = new MenuItem("Plot automated contour lines on image...");
        contourLinesMenuItem.addActionListener(this);
        analyzeMenu.add(contourLinesMenuItem);

        contourPlottersMenuItem = new MenuItem("Plot custom contour lines on image...");
        contourPlottersMenuItem.addActionListener(this);
        //analyzeMenu.add(contourPlottersMenuItem);

        azimuthalAverageMenuItem = new MenuItem("Plot azimuthal average... *");
        azimuthalAverageMenuItem.addActionListener(this);
        azimuthalAverageMenuItem.setEnabled(true);
        //analyzeMenu.add(azimuthalAverageMenuItem);

        threeDSurfacePlotMenuItem = new MenuItem("Interactive 3-D surface plot");
        threeDSurfacePlotMenuItem.addActionListener(this);
        analyzeMenu.add(threeDSurfacePlotMenuItem);

        analyzeMenu.addSeparator();

        analyzeMenu.add("*Requires line, box, or circle selection");
        analyzeMenu.add("  on image before execution.");

        mainMenuBar.add(analyzeMenu);


//------WCS menu---------------------------------------------------------------------

        wcsMenu = new Menu("WCS");

        astrometryMenuItem = new MenuItem("Plate solve using Astrometry.net");
        astrometryMenuItem.addActionListener(this);
        wcsMenu.add(astrometryMenuItem);

        astrometrySetupMenuItem = new MenuItem("Plate solve using Astrometry.net (with options)...");
        astrometrySetupMenuItem.addActionListener(this);
        wcsMenu.add(astrometrySetupMenuItem);

        autoSaveWCStoPrefsCB = new CheckboxMenuItem("Automatically save new WCS pixel scale and image rotation to preferences", autoSaveWCStoPrefs);
        autoSaveWCStoPrefsCB.addItemListener(this);
        if (autoSaveWCStoPrefs) {
            updatePrefsFromWCS(false);
        }
        wcsMenu.add(autoSaveWCStoPrefsCB);

        saveWCStoPrefsMenuItem = new MenuItem("Save current WCS pixel scale and image rotation to preferences");
        saveWCStoPrefsMenuItem.addActionListener(this);
        wcsMenu.add(saveWCStoPrefsMenuItem);

        setPixelScaleMenuItem = new MenuItem("Set pixel scale for images without WCS...");
        setPixelScaleMenuItem.addActionListener(this);
        wcsMenu.add(setPixelScaleMenuItem);

        dirAngleMenuItem = new MenuItem("Set north and east arrow orientations for images without WCS...");
        dirAngleMenuItem.addActionListener(this);
        wcsMenu.add(dirAngleMenuItem);

        useSIPAllProjectionsCB = new CheckboxMenuItem("Enable SIP support for all WCS projections (deselect for TAN only)", useSIPAllProjections);
        useSIPAllProjectionsCB.addItemListener(this);
        wcsMenu.add(useSIPAllProjectionsCB);

        mainMenuBar.add(wcsMenu);


//------end menus---------------------------------------------------------------------

        mainPanel = new Panel(new SpringLayout());
        topPanelA = new JPanel();
        topPanelA.setLayout(new BoxLayout(topPanelA, BoxLayout.LINE_AXIS));
        zoomPanel = new JPanel();
        zoomPanel.setLayout(new BoxLayout(zoomPanel, BoxLayout.LINE_AXIS));
        topPanelB = new JPanel();
        topPanelB.setLayout(new BoxLayout(topPanelB, BoxLayout.LINE_AXIS));
        topPanelBC = new JPanel(new SpringLayout());
        bottomPanelB = new JPanel();
        bottomPanelB.setLayout(new BoxLayout(bottomPanelB, BoxLayout.LINE_AXIS));

        topPanelB.add(Box.createHorizontalGlue());

        Dimension valueDim = new Dimension(90, 20);
        Dimension valueDimMin = new Dimension(90, 20);
        Dimension intCntDim = new Dimension(120, 20);
        Dimension intCntDimMin = new Dimension(120, 20);
        Dimension labelDim = new Dimension(65, 20);
        Dimension labelDimMin = new Dimension(65, 20);

        JLabel ijXLabel = new JLabel("ImageJ X:");
        ijXLabel.setFont(p12);
        ijXLabel.setHorizontalAlignment(JLabel.RIGHT);
        ijXLabel.setPreferredSize(labelDim);
        ijXLabel.setMaximumSize(labelDim);
        ijXLabel.setMinimumSize(labelDimMin);
        ijXLabel.setLabelFor(ijXTextField);
        topPanelBC.add(ijXLabel);

        ijXTextField = new JTextField("");
        ijXTextField.setFont(p12);
        ijXTextField.setHorizontalAlignment(JLabel.RIGHT);
        ijXTextField.setPreferredSize(valueDim);
        ijXTextField.setMaximumSize(valueDim);
        ijXTextField.setMinimumSize(valueDimMin);
        ijXTextField.setEditable(false);
        topPanelBC.add(ijXTextField);

        JLabel ijYLabel = new JLabel("ImageJ Y:");
        ijYLabel.setFont(p12);
        ijYLabel.setHorizontalAlignment(JLabel.RIGHT);
        ijYLabel.setPreferredSize(labelDim);
        ijYLabel.setMaximumSize(labelDim);
        ijYLabel.setMinimumSize(labelDimMin);
        ijYLabel.setLabelFor(ijYTextField);
        topPanelBC.add(ijYLabel);

        ijYTextField = new JTextField("");
        ijYTextField.setFont(p12);
        ijYTextField.setHorizontalAlignment(JTextField.RIGHT);
        ijYTextField.setPreferredSize(valueDim);
        ijYTextField.setMaximumSize(valueDim);
        ijYTextField.setMinimumSize(valueDimMin);
        ijYTextField.setEditable(false);
        topPanelBC.add(ijYTextField);

        JLabel valueLabel = new JLabel("Value:");
        valueLabel.setFont(p12);
        valueLabel.setHorizontalAlignment(JLabel.RIGHT);
        valueLabel.setPreferredSize(labelDim);
        valueLabel.setMaximumSize(labelDim);
        valueLabel.setMinimumSize(labelDimMin);
        valueLabel.setLabelFor(valueTextField);
        topPanelBC.add(valueLabel);

        valueTextField = new JTextField("");
        valueTextField.setFont(b12);
        valueTextField.setHorizontalAlignment(JTextField.RIGHT);
        valueTextField.setPreferredSize(intCntDim);
        valueTextField.setMaximumSize(intCntDim);
        valueTextField.setMinimumSize(intCntDimMin);
        valueTextField.setEditable(false);
        topPanelBC.add(valueTextField);

        JLabel RALabel = new JLabel("RA:");
        RALabel.setFont(p12);
        RALabel.setHorizontalAlignment(JLabel.RIGHT);
        RALabel.setPreferredSize(labelDim);
        RALabel.setMaximumSize(labelDim);
        RALabel.setMinimumSize(labelDimMin);
        RALabel.setToolTipText("<html>Shows Right Ascension (RA) at mouse pointer for images with WCS headers.<br>"+
                "Type or paste RA and Dec values and press &lt;Enter&gt; to draw an ROI at the corresponding location in the image.<br>"+
                "Hold &lt;Shift&gt; when pressing &lt;Enter&gt; to also create a centroided T1 aperture for Multi-Aperture.<br>"+
                "Hold &lt;Control&gt; when pressing &lt;Enter&gt; to also create an uncentroided T1 aperture for Multi-Aperture.<br></html>");
        RALabel.setLabelFor(RATextField);
        topPanelBC.add(RALabel);

        RATextField = new JTextField("");
        RATextField.setFont(p12);
        RATextField.setHorizontalAlignment(JLabel.RIGHT);
        RATextField.setPreferredSize(valueDim);
        RATextField.setMaximumSize(valueDim);
        RATextField.setMinimumSize(valueDimMin);
        RATextField.setToolTipText("<html>Shows Right Ascension (RA) at mouse pointer for images with WCS headers.<br>"+
                "Type or paste RA and Dec values and press &lt;Enter&gt; to draw an ROI at the corresponding location in the image.<br>"+
                "Hold &lt;Shift&gt; when pressing &lt;Enter&gt; to also create a centroided T1 aperture for Multi-Aperture.<br>"+
                "Hold &lt;Control&gt; when pressing &lt;Enter&gt; to also create an uncentroided T1 aperture for Multi-Aperture.<br></html>");
        RATextField.setEditable(goodWCS);
        RATextField.addActionListener(this);
        RATextField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER && ((e.getModifiers() & MouseEvent.SHIFT_MASK) != 0 || (e.getModifiers() & MouseEvent.CTRL_MASK) != 0)) {
                    RATextField.postActionEvent();
                }
            }
        });
        topPanelBC.add(RATextField);

        JLabel DecLabel = new JLabel("DEC:");
        DecLabel.setFont(p12);
        DecLabel.setHorizontalAlignment(JLabel.RIGHT);
        DecLabel.setPreferredSize(labelDim);
        DecLabel.setMaximumSize(labelDim);
        DecLabel.setMinimumSize(labelDimMin);
        DecLabel.setToolTipText("<html>Shows Declination (Dec) at mouse pointer for images with WCS headers.<br>"+
                "Type or paste RA and Dec values and press &lt;Enter&gt; to draw an ROI at the corresponding location in the image.<br>"+
                "Hold &lt;Shift&gt; when pressing &lt;Enter&gt; to also create a centroided T1 aperture for Multi-Aperture.<br>"+
                "Hold &lt;Control&gt; when pressing &lt;Enter&gt; to also create an uncentroided T1 aperture for Multi-Aperture.<br></html>");
        DecLabel.setLabelFor(DecTextField);
        topPanelBC.add(DecLabel);

        DecTextField = new JTextField("");
        DecTextField.setFont(p12);
        DecTextField.setHorizontalAlignment(JLabel.RIGHT);
        DecTextField.setPreferredSize(valueDim);
        DecTextField.setMaximumSize(valueDim);
        DecTextField.setMinimumSize(valueDimMin);
        DecTextField.setToolTipText("<html>Shows Declination (Dec) at mouse pointer for images with WCS headers.<br>"+
                "Type or paste RA and Dec values and press &lt;Enter&gt; to draw an ROI at the corresponding location in the image.<br>"+
                "Hold &lt;Shift&gt; when pressing &lt;Enter&gt; to also create a centroided T1 aperture for Multi-Aperture.<br>"+
                "Hold &lt;Control&gt; when pressing &lt;Enter&gt; to also create an uncentroided T1 aperture for Multi-Aperture.<br></html>");
        DecTextField.setEditable(goodWCS);
        DecTextField.addActionListener(this);
        topPanelBC.add(DecTextField);

//                JLabel arcLengthLabel = new JLabel("Arclen:");
        peakLabel = new JTextField("Peak:");
        peakLabel.setFont(p12);
        peakLabel.setBorder(BorderFactory.createEmptyBorder());
        peakLabel.setBackground(topPanelA.getBackground());
        peakLabel.setPreferredSize(labelDim);
        peakLabel.setMaximumSize(labelDim);
        peakLabel.setMinimumSize(labelDimMin);
        peakLabel.setHorizontalAlignment(JLabel.RIGHT);
        topPanelBC.add(peakLabel);

        peakTextField = new JTextField("");
        peakTextField.setFont(p12);
        peakTextField.setHorizontalAlignment(JLabel.RIGHT);
        peakTextField.setPreferredSize(intCntDim);
        peakTextField.setMaximumSize(intCntDim);
        peakTextField.setMinimumSize(intCntDimMin);
        peakTextField.setEditable(false);
        topPanelBC.add(peakTextField);

        JLabel fitsXLabel = new JLabel("FITS X:");
        fitsXLabel.setFont(p12);
        fitsXLabel.setHorizontalAlignment(JLabel.RIGHT);
        fitsXLabel.setPreferredSize(labelDim);
        fitsXLabel.setMaximumSize(labelDim);
        fitsXLabel.setMinimumSize(labelDimMin);
        fitsXLabel.setLabelFor(fitsXTextField);
        topPanelBC.add(fitsXLabel);

        fitsXTextField = new JTextField("");
        fitsXTextField.setFont(p12);
        fitsXTextField.setHorizontalAlignment(JLabel.RIGHT);
        fitsXTextField.setPreferredSize(valueDim);
        fitsXTextField.setMaximumSize(valueDim);
        fitsXTextField.setMinimumSize(valueDimMin);
        fitsXTextField.setEditable(false);
        topPanelBC.add(fitsXTextField);

        JLabel fitsYLabel = new JLabel("FITS Y:");
        fitsYLabel.setFont(p12);
        fitsYLabel.setHorizontalAlignment(JLabel.RIGHT);
        fitsYLabel.setPreferredSize(labelDim);
        fitsYLabel.setMaximumSize(labelDim);
        fitsYLabel.setMinimumSize(labelDimMin);
        fitsYLabel.setLabelFor(fitsYTextField);
        topPanelBC.add(fitsYLabel);

        fitsYTextField = new JTextField("");
        fitsYTextField.setFont(p12);
        fitsYTextField.setHorizontalAlignment(JLabel.RIGHT);
        fitsYTextField.setPreferredSize(valueDim);
        fitsYTextField.setMaximumSize(valueDim);
        fitsYTextField.setMinimumSize(valueDimMin);
        fitsYTextField.setEditable(false);
        topPanelBC.add(fitsYTextField);

//                JLabel lengthLabel = new JLabel("Length:");
        lengthLabel = new JTextField("Int Cnts:");
        lengthLabel.setFont(p12);
        lengthLabel.setBorder(BorderFactory.createEmptyBorder());
        lengthLabel.setBackground(topPanelA.getBackground());
        lengthLabel.setPreferredSize(labelDim);
        lengthLabel.setMaximumSize(labelDim);
        lengthLabel.setMinimumSize(labelDimMin);
        lengthLabel.setHorizontalAlignment(JLabel.RIGHT);
        topPanelBC.add(lengthLabel);

        lengthTextField = new JTextField("");
        lengthTextField.setFont(p12);
        lengthTextField.setHorizontalAlignment(JLabel.RIGHT);
        lengthTextField.setPreferredSize(intCntDim);
        lengthTextField.setMaximumSize(intCntDim);
        lengthTextField.setMinimumSize(intCntDimMin);
        lengthTextField.setEditable(false);
        topPanelBC.add(lengthTextField);

        SpringUtil.makeCompactGrid(topPanelBC, 3, topPanelBC.getComponentCount() / 3, 3, 3, 3, 3);
        topPanelB.add(topPanelBC);

        topPanelB.add(Box.createGlue());

        mainPanel.add(topPanelB);

        int iconWidth = 26;
        int iconHeight = 26;
        Dimension iconDimension = new Dimension(iconWidth, iconHeight);

        ImageIcon zoomInFastIcon = createImageIcon("images/viewmag++.png", "In Fast");
        ImageIcon zoomInIcon = createImageIcon("images/viewmag+.png", "In");
        ImageIcon zoomOutIcon = createImageIcon("images/viewmag-.png", "Out");
        ImageIcon zoomFitIcon = createImageIcon("images/viewmagfit.png", "Fit");
        ImageIcon multiApertureIcon = createImageIcon("images/multiaperture.png", "Multi-Aperture");
        ImageIcon alignIcon = createImageIcon("images/align.png", "Stack Align");
        ImageIcon headerIcon = createImageIcon("images/header.png", "Header");
        ImageIcon negativeIcon = createImageIcon("images/negative.png", "Negative");
        ImageIcon negativeIconSelected = createImageIcon("images/negativeselected.png", "Negative (selected)");
        ImageIcon autoscaleIcon = createImageIcon("images/autoscale.png", "Autoscale");
        ImageIcon broomIcon = createImageIcon("images/broom.png", "Clear Aperture Overlay");
        ImageIcon showAllIcon = createImageIcon("images/showallaps.png", "Show All Apertures in Overlay");
        ImageIcon showAnnotationIcon = createImageIcon("images/showannotations.png", "Toggle display of annotations in overlay");
        ImageIcon showAnnotationIconSelected = createImageIcon("images/showannotationsselected.png", "Toggle display of annotations in overlay");
        ImageIcon showSkyIcon = createImageIcon("images/showsky.png", "Toggle Sky Annuli");
        ImageIcon showSkyIconSelected = createImageIcon("images/showskyselected.png", "Toggle Sky Annuli (selected)");
        ImageIcon sourceIDIcon = createImageIcon("images/sourceid.png", "Toogle Source ID");
        ImageIcon sourceIDIconSelected = createImageIcon("images/sourceidselected.png", "Toogle Source ID (selected)");
        ImageIcon sourceCountsIcon = createImageIcon("images/sourcecounts.png", "Toggle Source Counts");
        ImageIcon sourceCountsIconSelected = createImageIcon("images/sourcecountsselected.png", "Toggle Source Counts (selected)");
        ImageIcon centroidIcon = createImageIcon("images/centroid.png", "Centroid Apertures");
        ImageIcon centroidIconSelected = createImageIcon("images/centroidselected.png", "Centroid Apertures (selected)");
        ImageIcon setApertureIcon = createImageIcon("images/setaperture.png", "Change Aperture Settings");
        ImageIcon clearMeasurementsIcon = createImageIcon("images/cleartable.png", "Clear Measurements Table");
        ImageIcon deleteSliceIcon = createImageIcon("images/deleteslice.png", "Delete Current Slice");
        ImageIcon astrometryIcon = createImageIcon("images/astrometry.png", "Plate Solve");
        ImageIcon astrometryIconSelected = createImageIcon("images/astrometryselected.png", "Plate Solve (selected)");

        topPanelA.add(Box.createHorizontalGlue());
        topPanelA.add(Box.createHorizontalStrut(20));
        Insets buttonMargin = new Insets(2, 2, 2, 2); //top,left,bottom,right
//                if (IJ.isWindows() || IJ.isMacintosh())
//                       buttonMargin = new Insets(2,4,2,4); //top,left,bottom,right

        buttonSub32768 = new JButton("-32768");
//                buttonSub32768.setMargin(buttonMargin);
        buttonSub32768.addActionListener(this);
//                topPanelAC.add(buttonSub32768);
        buttonAdd32768 = new JButton("+32768");
//                buttonAdd32768.setMargin(buttonMargin);
        buttonAdd32768.addActionListener(this);
//                topPanelAC.add(buttonAdd32768);

        buttonDeleteSlice = new JButton(deleteSliceIcon);
        buttonDeleteSlice.setToolTipText("delete currently displayed slice from stack");
        buttonDeleteSlice.setPreferredSize(iconDimension);
        buttonDeleteSlice.setMargin(buttonMargin);
        buttonDeleteSlice.addActionListener(this);
        topPanelA.add(buttonDeleteSlice);

        topPanelA.add(Box.createHorizontalStrut(10));

        buttonNegative = new JToggleButton(negativeIcon, useInvertingLut);
        buttonNegative.setToolTipText("display as image negative");
        buttonNegative.setSelectedIcon(negativeIconSelected);
        buttonNegative.setPreferredSize(iconDimension);
        buttonNegative.setMargin(buttonMargin);
        buttonNegative.addActionListener(this);
        topPanelA.add(buttonNegative);

        buttonShowAnnotations = new JToggleButton(showAnnotationIcon, ac.showAnnotations);
        buttonShowAnnotations.setToolTipText("<html>left-click: toggle display of annotations<br>" +
                "middle-click or control-click: display (append) annotations from FITS header<br>" +
                "right-click or shift-click: display (replace) annotations from FITS header</html>");
        buttonShowAnnotations.setSelectedIcon(showAnnotationIconSelected);
        buttonShowAnnotations.setPreferredSize(iconDimension);
        buttonShowAnnotations.setMargin(buttonMargin);
//                buttonShowAnnotations.addActionListener(this);
        buttonShowAnnotations.addMouseListener(new MouseListener() {
            public void mousePressed(MouseEvent e) {
                if (!e.isShiftDown() && !e.isControlDown() && ((e.getModifiers() & MouseEvent.BUTTON1_MASK) != 0)) {
                    ac.showAnnotations = !ac.showAnnotations;
//                            buttonShowAnnotations.setSelected(ac.showAnnotations);
                    Prefs.set("Astronomy_Tool.showAnnotations", ac.showAnnotations);
                    ac.repaint();
                } else if (e.isShiftDown() || (e.getModifiers() & MouseEvent.BUTTON3_MASK) != 0) {
                    buttonShowAnnotations.setSelected(true);
                    ac.showAnnotations = true;
                    Prefs.set("Astronomy_Tool.showAnnotations", ac.showAnnotations);
                    displayAnnotationsFromHeader(true, true, true);
                } else if (e.isControlDown() || (e.getModifiers() & MouseEvent.BUTTON2_MASK) != 0) {
                    buttonShowAnnotations.setSelected(true);
                    ac.showAnnotations = true;
                    Prefs.set("Astronomy_Tool.showAnnotations", ac.showAnnotations);
                    displayAnnotationsFromHeader(false, true, true);
                }
            }

            public void mouseReleased(MouseEvent e) {
            }

            public void mouseEntered(MouseEvent e) {
            }

            public void mouseExited(MouseEvent e) {
            }

            public void mouseClicked(MouseEvent e) {
            }
        });

        topPanelA.add(buttonShowAnnotations);

        buttonShowSky = new JToggleButton(showSkyIcon, showSkyOverlay);
        buttonShowSky.setToolTipText("toggle display of aperture sky background regions");
        buttonShowSky.setSelectedIcon(showSkyIconSelected);
        buttonShowSky.setPreferredSize(iconDimension);
        buttonShowSky.setMargin(buttonMargin);
        buttonShowSky.addActionListener(this);
        topPanelA.add(buttonShowSky);

        buttonSourceID = new JToggleButton(sourceIDIcon, nameOverlay);
        buttonSourceID.setToolTipText("toggle display of aperture source identifications");
        buttonSourceID.setSelectedIcon(sourceIDIconSelected);
        buttonSourceID.setPreferredSize(iconDimension);
        buttonSourceID.setMargin(buttonMargin);
        buttonSourceID.addActionListener(this);
        topPanelA.add(buttonSourceID);

        buttonSourceCounts = new JToggleButton(sourceCountsIcon, valueOverlay);
        buttonSourceCounts.setToolTipText("toggle display of aperture source integrated counts");
        buttonSourceCounts.setSelectedIcon(sourceCountsIconSelected);
        buttonSourceCounts.setPreferredSize(iconDimension);
        buttonSourceCounts.setMargin(buttonMargin);
        buttonSourceCounts.addActionListener(this);
        topPanelA.add(buttonSourceCounts);

        buttonCentroid = new JToggleButton(centroidIcon, reposition);
        buttonCentroid.setToolTipText("centroid apertures");
        buttonCentroid.setSelectedIcon(centroidIconSelected);
        buttonCentroid.setPreferredSize(iconDimension);
        buttonCentroid.setMargin(buttonMargin);
        buttonCentroid.addActionListener(this);
        topPanelA.add(buttonCentroid);

        buttonSetAperture = new JButton(setApertureIcon);
        buttonSetAperture.setToolTipText("change aperture settings");
        buttonSetAperture.setPreferredSize(iconDimension);
        buttonSetAperture.setMargin(buttonMargin);
        buttonSetAperture.addActionListener(this);
        topPanelA.add(buttonSetAperture);

        buttonShowAll = new JButton(showAllIcon);
        buttonShowAll.setToolTipText("draw all stored apertures in overlay");
        buttonShowAll.setPreferredSize(iconDimension);
        buttonShowAll.setMargin(buttonMargin);
        buttonShowAll.addActionListener(this);
        topPanelA.add(buttonShowAll);

        buttonBroom = new JButton(broomIcon);
        buttonBroom.setToolTipText("clear apertures and annotations from overlay");
        buttonBroom.setPreferredSize(iconDimension);
        buttonBroom.setMargin(buttonMargin);
        buttonBroom.addActionListener(this);
        topPanelA.add(buttonBroom);

        topPanelA.add(Box.createHorizontalStrut(10));

        buttonFlipX = new JButton("FlipX");
//                buttonFlipX.setMargin(buttonMargin);
        buttonFlipX.addActionListener(this);
//                topPanelA.add(buttonFlipX);
        buttonFlipY = new JButton("FlipY");
//                buttonFlipY.setMargin(buttonMargin);
        buttonFlipY.addActionListener(this);
//                topPanelA.add(buttonFlipY);
        buttonRotCCW = new JButton("RotCCW");
//                buttonRotCCW.setMargin(buttonMargin);
        buttonRotCCW.addActionListener(this);
//                topPanelA.add(buttonRotCCW);
        buttonRotCW = new JButton("RotCW");
//                buttonRotCW.setMargin(buttonMargin);
        buttonRotCW.addActionListener(this);
//                topPanelA.add(buttonRotCW);

        buttonClearMeasurements = new JButton(clearMeasurementsIcon);
        buttonClearMeasurements.setToolTipText("clear measurements table data");
        buttonClearMeasurements.setPreferredSize(iconDimension);
        buttonClearMeasurements.setMargin(buttonMargin);
        buttonClearMeasurements.addActionListener(this);
        topPanelA.add(buttonClearMeasurements);

        buttonMultiAperture = new JButton(multiApertureIcon);
        buttonMultiAperture.setToolTipText("perform multi-aperture photometry");
        buttonMultiAperture.setPreferredSize(iconDimension);
        buttonMultiAperture.setMargin(buttonMargin);
        buttonMultiAperture.addActionListener(this);
        topPanelA.add(buttonMultiAperture);

        buttonAlign = new JButton(alignIcon);
        buttonAlign.setToolTipText("align stack using apertures");
        buttonAlign.setPreferredSize(iconDimension);
        buttonAlign.setMargin(buttonMargin);
        buttonAlign.addActionListener(this);
        topPanelA.add(buttonAlign);

        buttonAstrometry = new JToggleButton(astrometryIcon, false);
        buttonAstrometry.setToolTipText("<html>plate solve using astrometry.net<br>" +
                "left-click to start with options panel<br>" +
                "shift-click or right-click to skip options panel</html>");
        buttonAstrometry.setPreferredSize(iconDimension);
        buttonAstrometry.setSelectedIcon(astrometryIconSelected);
        buttonAstrometry.setMargin(buttonMargin);
        buttonAstrometry.addActionListener(this);
        buttonAstrometry.addMouseListener(new MouseListener() {
            public void mousePressed(MouseEvent e) {
                if ((e.getModifiers() & MouseEvent.BUTTON3_MASK) != 0 && !buttonAstrometry.isSelected()) {
                    handleAstrometry(false);
                }
            }

            public void mouseReleased(MouseEvent e) {
            }

            public void mouseEntered(MouseEvent e) {
            }

            public void mouseExited(MouseEvent e) {
            }

            public void mouseClicked(MouseEvent e) {
            }
        });
        topPanelA.add(buttonAstrometry);

        astrometry = new Astrometry();

        buttonHeader = new JButton(headerIcon);
        buttonHeader.setToolTipText("display fits header");
        buttonHeader.setPreferredSize(iconDimension);
        buttonHeader.setMargin(buttonMargin);
        buttonHeader.addActionListener(this);
        topPanelA.add(buttonHeader);
        topPanelA.add(Box.createHorizontalStrut(10));
        topPanelA.setBorder(BorderFactory.createEmptyBorder(0, 0, 5, 0));

//                buttonLUT = new JButton("LUT");
//                buttonLUT.setMargin(buttonMargin);
//                buttonLUT.addActionListener(this);
//                topPanelAC.add(buttonLUT);


//                JLabel zoomLabel = new JLabel("Zoom  ");
//                zoomPanel.add(zoomLabel);


        buttonZoomInFast = new JButton(zoomInFastIcon);
        buttonZoomInFast.setToolTipText("zoom in fast");
        buttonZoomInFast.setMargin(buttonMargin);
        buttonZoomInFast.addActionListener(this);
        buttonZoomInFast.setPreferredSize(iconDimension);
        zoomPanel.add(buttonZoomInFast);

        buttonZoomIn = new JButton(zoomInIcon);
        buttonZoomIn.setMargin(buttonMargin);
        buttonZoomIn.addActionListener(this);
        buttonZoomIn.setToolTipText("zoom in");
        buttonZoomIn.setPreferredSize(iconDimension);
        zoomPanel.add(buttonZoomIn);

        buttonZoomOut = new JButton(zoomOutIcon);
        buttonZoomOut.setMargin(buttonMargin);
        buttonZoomOut.addActionListener(this);
        buttonZoomOut.setToolTipText("zoom out");
        buttonZoomOut.setPreferredSize(iconDimension);
        zoomPanel.add(buttonZoomOut);

        buttonFit = new JButton(zoomFitIcon);
        buttonFit.setMargin(buttonMargin);
        buttonFit.setToolTipText("zoom to fit image to window");
        buttonFit.setPreferredSize(iconDimension);
        buttonFit.addActionListener(this);
        zoomPanel.add(buttonFit);
//                zoomPanel.setBorder(BorderFactory.createTitledBorder(""));
        topPanelA.add(zoomPanel);

        topPanelA.add(Box.createHorizontalStrut(10));

        buttonAutoLevels = new JButton(autoscaleIcon);
        buttonAutoLevels.setToolTipText("auto brightness and contrast");
        buttonAutoLevels.setMargin(buttonMargin);
        buttonAutoLevels.setPreferredSize(iconDimension);
        buttonAutoLevels.addActionListener(this);
        topPanelA.add(buttonAutoLevels);
        topPanelA.add(Box.createHorizontalGlue());
        mainPanel.add(topPanelA);


//                icPanel.add(ac);
//                SpringUtil.makeCompactGrid (icPanel, 1, 1, 0,0,0,0);

        mainPanel.add(ac);

//                stackSliders = super.getComponents();

//                IJ.log("stackSliders.length="+stackSliders.length);
        if (cSelector != null) mainPanel.add(cSelector);
        if (zSelector != null) mainPanel.add(zSelector);
        if (tSelector != null) mainPanel.add(tSelector);
//                JScrollBar stackSlider = new JScrollBar(JScrollBar.HORIZONTAL, 0, 1, 0, imp.getNSlices());
//                stackSlider.addAdjustmentListener(new AdjustmentListener() {
//                                                      //@Override
//                                                      public void adjustmentValueChanged(AdjustmentEvent e) {
//                                                          int slice = stackSlider.getValue()+1;
//                                                          imp.setSlice(slice);
//                                                      }
//                                                  });
//                mainPanel.add(stackSlider);
//
//                JSlider stackSlider2 = new JSlider(JSlider.HORIZONTAL, 1, imp.getNSlices(), 1);
//                stackSlider2.addChangeListener(ev -> {
//                    int slice = stackSlider2.getValue();
//                    imp.setSlice(slice);
//                });
//                mainPanel.add(stackSlider2);

//                if (super.getNScrollbars() > 0)
//                        for (int i = 0; i < super.getNScrollbars(); i++)
//                                {
//                                stackSliders[i].setPreferredSize(new Dimension(100,18));
//                                mainPanel.add(stackSliders[i]);
//                                }


        minValueTextField = new JTextField(fourPlaces.format(minValue));
        minValueTextField.setFont(p12);
        minValueTextField.setPreferredSize(new Dimension(70, 17));
        minValueTextField.setHorizontalAlignment(JTextField.LEFT);
        writeNumericPanelField(minValue, minValueTextField);
        bottomPanelB.add(minValueTextField);
        JTextField minlabelTextField = new JTextField(":min");
        minlabelTextField.setFont(p12);
        minlabelTextField.setPreferredSize(new Dimension(30, 17));
        minlabelTextField.setHorizontalAlignment(JTextField.LEFT);
        minlabelTextField.setBorder(BorderFactory.createEmptyBorder());
        minlabelTextField.setEditable(false);
        bottomPanelB.add(minlabelTextField);

        bottomPanelB.add(Box.createHorizontalStrut(10));

        blackTextfield = new JTextField(fourPlaces.format(blackValue));
        blackTextfield.setFont(b12);
        blackTextfield.setPreferredSize(new Dimension(70, 17));
        blackTextfield.setHorizontalAlignment(JTextField.RIGHT);
        blackTextfield.setBorder(BorderFactory.createLineBorder(Color.RED));
        blackTextfield.setEditable(true);
        blackTextfield.addActionListener(this);
        writeNumericPanelField(blackValue, blackTextfield);
//                minTextField.getDocument().addDocumentListener(new thisDocumentListener());
        bottomPanelB.add(blackTextfield);

        JTextField lowlabelTextField = new JTextField(":black");
        lowlabelTextField.setFont(p12);
        lowlabelTextField.setPreferredSize(new Dimension(30, 17));
        lowlabelTextField.setHorizontalAlignment(JTextField.LEFT);
        lowlabelTextField.setBorder(BorderFactory.createEmptyBorder());
        lowlabelTextField.setEditable(false);
        bottomPanelB.add(lowlabelTextField);

        bottomPanelB.add(Box.createHorizontalGlue());

        JTextField meanlabelTextField = new JTextField("mean:");
        meanlabelTextField.setFont(p12);
        meanlabelTextField.setPreferredSize(new Dimension(70, 17));
        meanlabelTextField.setHorizontalAlignment(JTextField.RIGHT);
        meanlabelTextField.setBorder(BorderFactory.createEmptyBorder());
        meanlabelTextField.setEditable(false);
        bottomPanelB.add(meanlabelTextField);

        meanTextField = new JTextField(fourPlaces.format(stats.mean));
        meanTextField.setFont(p12);
        meanTextField.setPreferredSize(new Dimension(70, 17));
        meanTextField.setHorizontalAlignment(JTextField.LEFT);
        meanTextField.setBorder(BorderFactory.createEmptyBorder());
        meanTextField.setEditable(false);
        writeNumericPanelField(stats.mean, meanTextField);
        bottomPanelB.add(meanTextField);

        bottomPanelB.add(Box.createHorizontalGlue());

        JTextField highlabelTextField = new JTextField("white:");
        highlabelTextField.setFont(p12);
        highlabelTextField.setPreferredSize(new Dimension(30, 17));
        highlabelTextField.setHorizontalAlignment(JTextField.RIGHT);
        highlabelTextField.setBorder(BorderFactory.createEmptyBorder());
        highlabelTextField.setEditable(false);
        bottomPanelB.add(highlabelTextField);

        whiteTextfield = new JTextField(fourPlaces.format(whiteValue));
        whiteTextfield.setFont(b12);
        whiteTextfield.setPreferredSize(new Dimension(70, 17));
        whiteTextfield.setHorizontalAlignment(JTextField.RIGHT);
        whiteTextfield.setBorder(BorderFactory.createLineBorder(Color.RED));
        whiteTextfield.setEditable(true);
        whiteTextfield.addActionListener(this);
        writeNumericPanelField(whiteValue, whiteTextfield);
//                maxTextField.getDocument().addDocumentListener(new thisDocumentListener());
        bottomPanelB.add(whiteTextfield);

        bottomPanelB.add(Box.createHorizontalStrut(10));

        JTextField maxlabelTextField = new JTextField("max:");
        maxlabelTextField.setFont(p12);
        maxlabelTextField.setPreferredSize(new Dimension(30, 17));
        maxlabelTextField.setHorizontalAlignment(JTextField.RIGHT);
        maxlabelTextField.setBorder(BorderFactory.createEmptyBorder());
        maxlabelTextField.setEditable(false);
        bottomPanelB.add(maxlabelTextField);

        maxValueTextField = new JTextField(fourPlaces.format(maxValue));
        maxValueTextField.setFont(p12);
        maxValueTextField.setPreferredSize(new Dimension(70, 17));
        maxValueTextField.setHorizontalAlignment(JTextField.RIGHT);
        writeNumericPanelField(maxValue, maxValueTextField);
        bottomPanelB.add(maxValueTextField);
        updateMinMaxValueTextFields();
        bottomPanelB.setBorder(BorderFactory.createEmptyBorder(0, 0, 5, 0));

//                SpringUtil.makeCompactGrid (bottomPanelB, 1, bottomPanelB.getComponentCount(), 5,0,5,0);
//                ImageProcessor ip = imp.getProcessor();

        getBiSliderStatistics();
        histogram = stats.histogram;
        logHistogram = new double[histogram.length];

        for (int i = 0; i < histogram.length; i++) {
            if (histogram[i] <= 1)
                logHistogram[i] = 0;
            else
                logHistogram[i] = Math.log(histogram[i]);
            if (logHistogram[i] > histMax)
                histMax = logHistogram[i];
        }

        minMaxBiSlider = new BiSlider(BiSlider.RGB);
        minMaxBiSlider.setUniformSegment(false);
        minMaxBiSlider.setDecimalFormater(fourPlaces);
        minMaxBiSlider.setFont(p13);
//                minMaxBiSlider.setHorizontal(false);
//                minMaxBiSlider.setUI((BiSliderPresentation) metal);
        minMaxBiSlider.setVisible(true);
        minMaxBiSlider.setValues(minValue, maxValue);
        minMaxBiSlider.setMinimumColoredValue(blackValue);
        minMaxBiSlider.setMaximumColoredValue(whiteValue);
        minMaxBiSlider.setMinimumValue(minValue);
        minMaxBiSlider.setMaximumValue(maxValue);
        minMaxBiSlider.setSegmentSize((maxValue - minValue) / (double) BISLIDER_SEGMENTS);
        minMaxBiSlider.setMinimumColor(Color.BLACK);

        minMaxBiSlider.setMiddleColor(Color.BLACK);
        minMaxBiSlider.setMaximumColor(Color.BLACK);
        minMaxBiSlider.setColoredValues(blackValue, whiteValue);
        minMaxBiSlider.setUnit("  ");
        minMaxBiSlider.setSliderBackground(Color.LIGHT_GRAY);
        minMaxBiSlider.setForeground(Color.BLACK);

        minMaxBiSlider.setDefaultColor(Color.WHITE);
        minMaxBiSlider.setPreferredSize(new Dimension(535, 75));
        minMaxBiSlider.setPrecise(false);
        minMaxBiSlider.setOpaque(true);
        minMaxBiSlider.setArcSize(0);
        minMaxBiSlider.addContentPainterListener(new ContentPainterListener() {
            public void paint(ContentPainterEvent ContentPainterEvent_Arg) {
                Graphics2D Graphics2 = (Graphics2D) ContentPainterEvent_Arg.getGraphics();
                Rectangle Rect1 = ContentPainterEvent_Arg.getRectangle();
                Graphics2.setColor((new Color(230, 230, 230)));
                Graphics2.fillRect(Rect1.x, Rect1.y, Rect1.width, Rect1.height);
                Rectangle Rect2 = ContentPainterEvent_Arg.getBoundingRectangle();
//                    double BarHeight = Math.abs(Math.cos(Math.PI*(Rect2.x+Rect2.width/2) / minMaxBiSlider.getWidth()));
//                    double BarHeight = (double)(Rect2.x+Rect2.width/2) / minMaxBiSlider.getWidth();
//                    double BarHeight = Math.random();

//                    float X = ((float)Rect2.x-minMaxBiSlider.getWidth()/2)/minMaxBiSlider.getWidth()*6;
                double X = ((double) (Rect2.x - 10 - minMaxBiSlider.getX())) / ((double) minMaxBiSlider.getWidth() - 22.0);
//                    double BarHeight = 1-Math.exp((-1*X*X)/2);
                int index = (int) (histogram.length * X);
                if (index < 0) index = 0;
                if (index >= logHistogram.length) index = logHistogram.length - 1;
//                    IJ.log("index = "+index+"   Max index = "+(logHistogram.length-1));
                double BarHeight = 1.0 - (logHistogram[index]) / histMax;
//                    X = ((float)(Rect2.x-Rect2.width - 10 - minMaxBiSlider.getX()))/(double)minMaxBiSlider.getWidth();
//                    double BarHeight2 = 1-Math.exp((-1*X*X)/2);
//                    double BarHeight2 = 1.0-(logHistogram[(int)(histogram.length*X)])/histMax;

                if (ContentPainterEvent_Arg.getColor() != null) {
                    Graphics2.setColor(Color.WHITE);
                    Graphics2.fillRect(Rect2.x, Rect2.y, Rect2.width, (int) ((BarHeight * Rect2.height)));
                    Graphics2.setColor(new Color(120, 165, 255));//(ContentPainterEvent_Arg.getColor());
                    Graphics2.fillRect(Rect2.x, Rect2.y + (int) ((BarHeight * Rect2.height)), Rect2.width + 1, 1 + (int) (((1 - BarHeight) * Rect2.height)));
                    //Graphics2.drawRect(Rect2.x, Rect2.y+(int)((BarHeight*Rect2.height)), Rect2.width+1, 1+(int)(((1-BarHeight)*Rect2.height)));
                } else {
                    Graphics2.setColor(Color.LIGHT_GRAY);//(new Color(255, 255, 218, 64));
                    Graphics2.fillRect(Rect2.x, Rect2.y + (int) ((BarHeight * Rect2.height)), Rect2.width + 1, 1 + (int) (((1 - BarHeight) * Rect2.height)));
                }
//                    Graphics2.setColor(Color.LIGHT_GRAY);
//                    //Graphics2.drawRect(Rect2.x, Rect2.y+(int)((BarHeight*Rect2.height)), Rect2.width-1, (int)(((1-BarHeight)*Rect2.height)));
//                    Graphics2.drawLine(Rect2.x, Rect2.y+(int)((BarHeight*Rect2.height)), Rect2.x+Rect2.width-1, Rect2.y+(int)((BarHeight*Rect2.height)));
//                    Graphics2.drawLine(Rect2.x, Rect2.y+(int)((BarHeight*Rect2.height)), Rect2.x, Rect2.y+(int)((prevBarHeight*Rect2.height)));
////                    Graphics2.drawLine(Rect2.x, Rect2.y+(int)((Math.max(BarHeight, BarHeight2)*Rect2.height)), Rect2.x, Rect2.y+Rect2.height);
//                    Rect3 = Rect2;
//                    prevBarHeight = BarHeight;
            }
        });

//                final JPopupMenu JPopupMenu6 = minMaxBiSlider.createPopupMenu();
//                minMaxBiSlider.addMouseListener(new MouseAdapter(){
//                  public void mousePressed(MouseEvent MouseEvent_Arg){
//                    if (MouseEvent_Arg.getButton()==MouseEvent.BUTTON3){
//                      JPopupMenu6.show(minMaxBiSlider, MouseEvent_Arg.getX(), MouseEvent_Arg.getY());
//                    }
//                  }
//                });

        final String initialText = "\n\n\n Use this BiSlider to see the events generated\n";
        final JTextArea JTextArea5 = new JTextArea(initialText);
        minMaxBiSlider.addBiSliderListener(new BiSliderAdapter() {
            /** something changed that modified the color gradient between min and max */
            public void newColors(BiSliderEvent BiSliderEvent_Arg) {
//                      IJ.log("newColors()");
            }

            /**  min or max colored values changed  */
            public void newValues(BiSliderEvent BiSliderEvent_Arg) {
                if (updatesEnabled) {
                    blackValue = minMaxBiSlider.getMinimumColoredValue();
                    whiteValue = minMaxBiSlider.getMaximumColoredValue();
                    sliceMin[imp.getCurrentSlice() - 1] = blackValue;
                    sliceMax[imp.getCurrentSlice() - 1] = whiteValue;
                    //
                    //                            if (min < minValue)
                    //                                    {
                    //                                    min = minValue;
                    //                                    minMaxBiSlider.setMinimumColoredValue(min);
                    //                                    }
                    //                            if (min > maxValue)
                    //                                     {
                    //                                    min = maxValue;
                    //                                    minMaxBiSlider.setMinimumColoredValue(min);
                    //                                    }
                    //                            if (max > maxValue)
                    //                                    {
                    //                                    max = maxValue;
                    //                                    minMaxBiSlider.setMaximumColoredValue(max);
                    //                                    }
                    //                            if (max < min)
                    //                                    {
                    //                                    max = min;
                    //                                    minMaxBiSlider.setMaximumColoredValue(max);
                    //                                    }

                    imp.setDisplayRange(cal.getRawValue(blackValue), cal.getRawValue(whiteValue));
                    minMaxChanged = true;
                    writeNumericPanelField(blackValue, blackTextfield);
                    writeNumericPanelField(whiteValue, whiteTextfield);
                    savedBlackValue = blackValue;
                    savedWhiteValue = whiteValue;
                    if (autoContrast && autoGrabBandCFromHistogram) grabAutoScaleParameters();
                    //                            Prefs.set("Astronomy_Tool.savedMin", savedMin);
                    //                            Prefs.set("Astronomy_Tool.savedMax", savedMax);
                    imp.updateAndDraw();
                }
            }

            /**  min selected value changed  */
            public void newMinValue(BiSliderEvent BiSliderEvent_Arg) {
//                      IJ.log("newMinValue()");
//                          getBiSliderStatistics;
//                          updatePanelValues();
            }

            /**  max selected value changed  */
            public void newMaxValue(BiSliderEvent BiSliderEvent_Arg) {
//                      IJ.log("newMaxValue()");
//                            getBiSliderStatistics;
//                            maxValue = minMaxBiSlider.getMaximumValue();
//                            minMaxBiSlider.setSegmentSize((maxValue - minValue)/256.0);
//                            histogram = stats.histogram;
//                            IJ.log("after histogram="+minMaxBiSlider.getMaximumValue());
//                            for (int i=0; i<histogram.length; i++)
//                                {
//                                if (histogram[i] <= 1)
//                                    logHistogram[i] = 0;
//                                else
//                                    logHistogram[i] = Math.log(histogram[i]);
//                                if (logHistogram[i] > histMax)
//                                    histMax = logHistogram[i];
//                                }
//                            updatePanelValues();
            }

            /**  selected segments changed  */
            public void newSegments(BiSliderEvent BiSliderEvent_Arg) {
//                          IJ.log("newSegments()");
//                          getBiSliderStatistics);
//                          updatePanelValues();
            }
        });
//                Calibration cal = imp.getCalibration();
//                imp.setDisplayRange(cal.getRawValue(min), cal.getRawValue(max));
//                minMaxChanged = true;
        mainPanel.add(minMaxBiSlider);
        mainPanel.add(bottomPanelB);

        FileDrop fileDrop = new FileDrop(mainPanel, BorderFactory.createEmptyBorder(), new FileDrop.Listener() {
            public void filesDropped(java.io.File[] files) {
                openDragAndDropFiles(files);
            }
        });

        SpringUtil.makeCompactGrid(mainPanel, mainPanel.getComponentCount(), 1, 0, 0, 0, 0);
        setMinimumSize(new Dimension(MIN_FRAME_WIDTH, MIN_FRAME_HEIGHT));
        setMenuBar(mainMenuBar);

        setTitle(impTitle);
        setName(impTitle);
        add(mainPanel);
        ImageIcon frameIcon = createImageIcon("images/astroIJ.png", "File Open");
        this.setIconImage(frameIcon.getImage());

        setResizable(true);


        pack();

        if (rememberWindowLocation) {
            Dimension mainScreenSize = Toolkit.getDefaultToolkit().getScreenSize();
            if (!Prefs.isLocationOnScreen(new Point(frameLocationX, frameLocationY))) {
                frameLocationX = mainScreenSize.width / 2 - getWidth() / 2;
                frameLocationY = mainScreenSize.height / 2 - getHeight() / 2;
            }
            this.setLocation(frameLocationX, frameLocationY);
        }

        otherPanelsHeight = topPanelA.getHeight() + topPanelB.getHeight() +
                bottomPanelB.getHeight() + minMaxBiSlider.getHeight();
        frameHeightPadding = this.getHeight() - ac.getHeight();// - minMaxBiSlider.getHeight() - bottomPanelB.getHeight();
        drawInfo(getGraphics());
        repaint();
    }

//            class thisDocumentListener implements DocumentListener
//                {
//                public void insertUpdate (DocumentEvent ev)
//                    {
//                    IJ.log("insert");
//                    }
//                public void removeUpdate (DocumentEvent ev)
//                    {
//                    IJ.log("remove");
//                    }
//                public void changedUpdate (DocumentEvent ev)
//                    {
//                    IJ.log("changed");
//                    }
//                }


    /**
     * Returns an ImageIcon, or null if the path was invalid.
     */
    protected ImageIcon createImageIcon(String path, String description) {
        java.net.URL imgURL = getClass().getClassLoader().getResource(path);
        if (imgURL != null) {
            return new ImageIcon(imgURL, description);
        } else {
            IJ.log("Couldn't find icon file: " + path);
            return null;
        }
    }

    /**
     * This is used to force the subtitle to update when animation/MA is running and the cursor is mvoed over the
     * stackwindow. The subtitle is normally prevented from updating due to
     * {@link AstroStackWindow#updateXYValue(double, double, boolean)} calling a method that locks the EventQueue.
     * Draws a background to prevent text from "bolding" or overlapping from multiple calls.
     */
    private void drawSubtitle() {
        var g = getGraphics();
        var sub = createSubtitle();
        if (oldSubtitle.equals("") || !oldSubtitle.equals(sub)) {
            var c = g.getColor();
            g.setColor(Color.WHITE);
            g.fillRect(super.getInsets().left + 5, 0, getWidth(), super.getInsets().top + g.getFontMetrics().getHeight() + 3);
            g.setColor(c);
            oldSubtitle = sub;
            drawInfo(g);
        }
    }

    public void registerStackListener(Consumer<Void> listener) {
        stackListeners.add(listener);
    }

    public void removeStackListener(Consumer<Void> listener) {
        stackListeners.remove(listener);
    }

    void updateMinMaxValueTextFields() {
        if (!useFixedMinMaxValues) {
            minValueTextField.setEditable(false);
            minValueTextField.setBorder(BorderFactory.createLineBorder(Color.GRAY));
            minValueTextField.removeActionListener(this);
            maxValueTextField.setEditable(false);
            maxValueTextField.setBorder(BorderFactory.createLineBorder(Color.GRAY));
            maxValueTextField.removeActionListener(this);
        } else {
            minValueTextField.setBorder(BorderFactory.createLineBorder(Color.RED));
            minValueTextField.setEditable(true);
            minValueTextField.addActionListener(this);
            maxValueTextField.setBorder(BorderFactory.createLineBorder(Color.RED));
            maxValueTextField.setEditable(true);
            maxValueTextField.addActionListener(this);
        }
    }

    public void itemStateChanged(ItemEvent e) {
        Object source = e.getItemSelectable();
        if (e.getStateChange() == ItemEvent.SELECTED) {
            if (source == autoConvertCB) {
                autoConvert = true;
                Prefs.set("Astronomy_Tool.autoConvert", autoConvert);
            } else if (source == autoContrastRB) {
                autoContrast = true;
                fixedContrast = false;
                fixedContrastPerSlice = false;
                fixedContrastRB.setState(false);
                fixedContrastPerSliceRB.setState(false);
                useFullRangeRB.setState(false);
                Prefs.set("Astronomy_Tool.startupAutoLevel", autoContrast);
                Prefs.set("Astronomy_Tool.startupPrevLevels", fixedContrast);
                Prefs.set("Astronomy_Tool.startupPrevLevelsPerSlice", fixedContrastPerSlice);
                setAutoLevels(null);
            } else if (source == fixedContrastRB) {
                autoContrast = false;
                fixedContrast = true;
                fixedContrastPerSlice = false;
                autoContrastRB.setState(false);
                fixedContrastPerSliceRB.setState(false);
                useFullRangeRB.setState(false);
                Prefs.set("Astronomy_Tool.startupAutoLevel", autoContrast);
                Prefs.set("Astronomy_Tool.startupPrevLevels", fixedContrast);
                Prefs.set("Astronomy_Tool.startupPrevLevelsPerSlice", fixedContrastPerSlice);
            } else if (source == fixedContrastPerSliceRB) {
                autoContrast = false;
                fixedContrast = false;
                fixedContrastPerSlice = true;
                autoContrastRB.setState(false);
                fixedContrastRB.setState(false);
                useFullRangeRB.setState(false);
                Prefs.set("Astronomy_Tool.startupAutoLevel", autoContrast);
                Prefs.set("Astronomy_Tool.startupPrevLevels", fixedContrast);
                Prefs.set("Astronomy_Tool.startupPrevLevelsPerSlice", fixedContrastPerSlice);
                blackValue = sliceMin[imp.getCurrentSlice() - 1];
                whiteValue = sliceMax[imp.getCurrentSlice() - 1];
                updatePanelValues();
            } else if (source == useFullRangeRB) {
                autoContrast = false;
                fixedContrast = false;
                fixedContrastPerSlice = false;
                autoContrastRB.setState(false);
                fixedContrastRB.setState(false);
                fixedContrastPerSliceRB.setState(false);
                Prefs.set("Astronomy_Tool.startupAutoLevel", autoContrast);
                Prefs.set("Astronomy_Tool.startupPrevLevels", fixedContrast);
                Prefs.set("Astronomy_Tool.startupPrevLevelsPerSlice", fixedContrastPerSlice);
                blackValue = minValue;
                whiteValue = maxValue;
                updatePanelValues();
            } else if (source == useFixedMinMaxValuesCB) {
                if (imp.getType() == ImagePlus.COLOR_256 || imp.getType() == ImagePlus.COLOR_RGB || imp.getType() == ImagePlus.GRAY8) {
                    useFixedMinMaxValues = false;
                    useFixedMinMaxValuesCB.setState(false);
                } else {
                    useFixedMinMaxValues = true;
                    Prefs.set("Astronomy_Tool.useFixedMinMaxValues", useFixedMinMaxValues);

                    if (imp.getType() == ImagePlus.GRAY16 && fixedMaxValue - fixedMinValue < 256)
                        fixedMaxValue = fixedMinValue + 255;
                    maxValue = fixedMaxValue;
                    minValue = fixedMinValue;
                    updateHistogramValues(false);
                    updateMinMaxValueTextFields();
                }
            } else if (source == autoGrabBandCFromHistogramCB) {
                autoGrabBandCFromHistogram = true;
                Prefs.set("Astronomy_Tool.autoGrabBandCFromHistogram", autoGrabBandCFromHistogram);
            } else if (source == usePreviousSizeCB) {
                startupPrevSize = true;
                Prefs.set("Astronomy_Tool.startupPrevSize", startupPrevSize);
            } else if (source == usePreviousPanCB) {
                startupPrevPan = true;
                Prefs.set("Astronomy_Tool.startupPrevPan", startupPrevPan);
            } else if (source == usePreviousZoomCB) {
                startupPrevZoom = true;
                Prefs.set("Astronomy_Tool.startupPrevZoom", startupPrevZoom);
            } else if (source == rememberWindowLocationCB) {
                rememberWindowLocation = true;
                Prefs.set("Astronomy_Tool.rememberWindowLocation", rememberWindowLocation);
            } else if (source == showMeanNotPeakCB) {
                showMeanNotPeak = true;
                peakLabel.setText("Mean:");
                writeNumericPanelField(photom.meanBrightness(), peakTextField);
                Prefs.set("Astronomy_Tool.showMeanNotPeak", showMeanNotPeak);
            } else if (source == useSexagesimalCB) {
                useSexagesimal = true;
                Prefs.set("Astronomy_Tool.useSexagesimal", useSexagesimal);
            } else if (source == middleClickCenterCB) {
                middleClickCenter = true;
                Prefs.set("Astronomy_Tool.middleClickCenter", middleClickCenter);
            } else if (source == writeMiddleClickValuesLogCB) {
                writeMiddleClickValuesLog = true;
                Prefs.set("Astronomy_Tool.writeMiddleClickValuesLog", writeMiddleClickValuesLog);
            } else if (source == writeMiddleDragValuesLogCB) {
                writeMiddleDragValuesLog = true;
                Prefs.set("Astronomy_Tool.writeMiddleDragValuesLog", writeMiddleDragValuesLog);
            } else if (source == writeMiddleClickValuesTableCB) {
                writeMiddleClickValuesTable = true;
                Prefs.set("Astronomy_Tool.writeMiddleClickValuesTable", writeMiddleClickValuesTable);
            } else if (source == writeMiddleDragValuesTableCB) {
                writeMiddleDragValuesTable = true;
                Prefs.set("Astronomy_Tool.writeMiddleDragValuesTable", writeMiddleDragValuesTable);
            } else if (source == showPhotometerCB) {
                showPhotometer = true;
//                updatePhotometerOverlay();
                ac.setAperture(radius, rBack1, rBack2, showSkyOverlay, showPhotometer);
                ac.paint(ac.getGraphics());
                Prefs.set("Astronomy_Tool.showPhotometer", showPhotometer);
            } else if (source == showRedCrossHairCursorCB) {
                ac.showRedCrossHairCursor = true;
                ac.paint(ac.getGraphics());
                Prefs.set("Astronomy_Tool.showRedCrossHairCursor", ac.showRedCrossHairCursor);
            } else if (source == removeBackStarsCB) {
                removeBackStars = true;
                Prefs.set("aperture.removebackstars", removeBackStars);
                photom.setRemoveBackStars(removeBackStars);
            } else if (source == showRemovedPixelsCB) {
                showRemovedPixels = true;
                Prefs.set("aperture.showremovedpixels", showRemovedPixels);
            } else if (source == rightClickAnnotateCB) {
                rightClickAnnotate = true;
                Prefs.set("Astronomy_Tool.rightClickAnnotate", rightClickAnnotate);
            } else if (source == useSimbadSearchCB) {
                useSimbadSearch = true;
                Prefs.set("Astronomy_Tool.useSimbadSearch", useSimbadSearch);
            } else if (source == showInSimbadCB) {
                showInSimbad = true;
                Prefs.set("Astronomy_Tool.showInSimbad", showInSimbad);
            } else if (source == autoUpdateAnnotationsInHeaderCB) {
                autoUpdateAnnotationsInHeader = true;
                Prefs.set("aperture.autoUpdateAnnotationsInHeader", autoUpdateAnnotationsInHeader);
            } else if (source == autoDisplayAnnotationsFromHeaderCB) {
                autoDisplayAnnotationsFromHeader = true;
                Prefs.set("aperture.autoDisplayAnnotationsFromHeader", autoDisplayAnnotationsFromHeader);

                displayAnnotationsFromHeader(true, true, true);
            } else if (source == showMeasureSexCB) {
                showMeasureSex = true;
                Prefs.set("Astronomy_Tool.showMeasureSex", showMeasureSex);
            } else if (source == showMeasureCircleCB) {
                showMeasureCircle = true;
                Prefs.set("Astronomy_Tool.showMeasureCircle", showMeasureCircle);
            } else if (source == showMeasureCrosshairCB) {
                showMeasureCrosshair = true;
                Prefs.set("Astronomy_Tool.showMeasureCrosshair", showMeasureCrosshair);
            } else if (source == showMeasureMultiLinesCB) {
                showMeasureMultiLines = true;
                Prefs.set("Astronomy_Tool.showMeasureMultiLines", showMeasureMultiLines);
            } else if (source == showMeasureLengthCB) {
                showMeasureLength = true;
                Prefs.set("Astronomy_Tool.showMeasureLength", showMeasureLength);
            } else if (source == showMeasurePACB) {
                showMeasurePA = true;
                Prefs.set("Astronomy_Tool.showMeasurePA", showMeasurePA);
            } else if (source == showMeasureDelMagCB) {
                showMeasureDelMag = true;
                Prefs.set("Astronomy_Tool.showMeasureDelMag", showMeasureDelMag);
            } else if (source == negateMeasureDelMagCB) {
                negateMeasureDelMag = true;
                Prefs.set("Astronomy_Tool.negateMeasureDelMag", negateMeasureDelMag);
            } else if (source == showMeasureFluxRatioCB) {
                showMeasureFluxRatio = true;
                Prefs.set("Astronomy_Tool.showMeasureFluxRatio", showMeasureFluxRatio);
            } else if (source == writeMeasureLengthLogCB) {
                writeMeasureLengthLog = true;
                Prefs.set("Astronomy_Tool.writeMeasureLengthLog", writeMeasureLengthLog);
            } else if (source == writeMeasureLengthTableDegCB) {
                writeMeasureLengthTableDeg = true;
                Prefs.set("Astronomy_Tool.writeMeasureLengthTableDeg", writeMeasureLengthTableDeg);
            } else if (source == writeMeasureLengthTableMinCB) {
                writeMeasureLengthTableMin = true;
                Prefs.set("Astronomy_Tool.writeMeasureLengthTableMin", writeMeasureLengthTableMin);
            } else if (source == writeMeasureLengthTableSecCB) {
                writeMeasureLengthTableSec = true;
                Prefs.set("Astronomy_Tool.writeMeasureLengthTableSec", writeMeasureLengthTableSec);
            } else if (source == writeMeasurePACB) {
                writeMeasurePA = true;
                Prefs.set("Astronomy_Tool.writeMeasurePA", writeMeasurePA);
            } else if (source == writeMeasureDelMagCB) {
                writeMeasureDelMag = true;
                Prefs.set("Astronomy_Tool.writeMeasureDelMag", writeMeasureDelMag);
            } else if (source == writeMeasureFluxRatioCB) {
                writeMeasureFluxRatio = true;
                Prefs.set("Astronomy_Tool.writeMeasureFluxRatio", writeMeasureFluxRatio);
            } else if (source == writePhotometricDataTableCB) {
                writePhotometricDataTable = true;
                Prefs.set("Astronomy_Tool.writePhotometricDataTable", writePhotometricDataTable);
            } else if (source == showZoomCB) {
                showZoom = true;
                ac.setShowZoom(showZoom);
                ac.paint(ac.getGraphics());
                Prefs.set("Astronomy_Tool.showZoom", showZoom);
            } else if (source == showDirCB) {
                showDir = true;
                ac.setShowDir(showDir);
                ac.paint(ac.getGraphics());
                Prefs.set("Astronomy_Tool.showDir", showDir);
            } else if (source == showXYCB) {
                showXY = true;
                ac.setShowXY(showXY);
                ac.paint(ac.getGraphics());
                Prefs.set("Astronomy_Tool.showXY", showXY);
            } else if (source == showScaleXCB) {
                showScaleX = true;
                ac.setShowPixelScale(showScaleX, showScaleY, pixelScaleX, pixelScaleY);
                ac.paint(ac.getGraphics());
                Prefs.set("Astronomy_Tool.showScaleX", showScaleX);
            } else if (source == showScaleYCB) {
                showScaleY = true;
                ac.setShowPixelScale(showScaleX, showScaleY, pixelScaleX, pixelScaleY);
                ;
                ac.paint(ac.getGraphics());
                Prefs.set("Astronomy_Tool.showScaleY", showScaleY);
            } else if (source == showAbsMagCB) {
                showAbsMag = true;
                ac.setShowAbsMag(showAbsMag);
                ac.paint(ac.getGraphics());
                Prefs.set("Astronomy_Tool.showAbsMag", showAbsMag);
            } else if (source == showIntCntWithAbsMagCB) {
                showIntCntWithAbsMag = true;
                ac.setShowIntCntWithAbsMag(showIntCntWithAbsMag);
                ac.paint(ac.getGraphics());
                Prefs.set("Astronomy_Tool.showIntCntWithAbsMag", showIntCntWithAbsMag);
            } else if (source == autoNupEleftRB) {
                autoNupEleft = true;
                Prefs.set("Astronomy_Tool.autoNupEleft", autoNupEleft);
                setBestOrientation();
            } else if (source == invertNoneRB) {
                invertX = false;
                invertY = false;
                invertXRB.setState(false);
                invertYRB.setState(false);
                invertXYRB.setState(false);
                setOrientation();
            } else if (source == invertXRB) {
                invertX = true;
                invertY = false;
                invertNoneRB.setState(false);
                invertYRB.setState(false);
                invertXYRB.setState(false);
                setOrientation();
            } else if (source == invertYRB) {
                invertX = false;
                invertY = true;
                invertXRB.setState(false);
                invertNoneRB.setState(false);
                invertXYRB.setState(false);
                setOrientation();
            } else if (source == invertXYRB) {
                invertX = true;
                invertY = true;
                invertXRB.setState(false);
                invertYRB.setState(false);
                invertNoneRB.setState(false);
                setOrientation();
            } else if (source == rotate0RB) {
                rotation = AstroCanvas.ROT_0;
                rotate90RB.setState(false);
                rotate180RB.setState(false);
                rotate270RB.setState(false);
                setOrientation();
            } else if (source == rotate90RB) {
                rotation = AstroCanvas.ROT_90;
                rotate0RB.setState(false);
                rotate180RB.setState(false);
                rotate270RB.setState(false);
                setOrientation();
            } else if (source == rotate180RB) {
                rotation = AstroCanvas.ROT_180;
                rotate0RB.setState(false);
                rotate90RB.setState(false);
                rotate270RB.setState(false);
                setOrientation();
            } else if (source == rotate270RB) {
                rotation = AstroCanvas.ROT_270;
                rotate0RB.setState(false);
                rotate90RB.setState(false);
                rotate180RB.setState(false);
                setOrientation();
            } else if (source == useSIPAllProjectionsCB) {
                useSIPAllProjections = true;
                Prefs.set("Astronomy_Tool.useSIPAllProjections", useSIPAllProjections);
                if (wcs != null) {
                    wcs.setUseSIPAlways(useSIPAllProjections);
                    extraInfo = " (" + wcs.coordsys + ")";
                    repaint();
                }
            } else if (source == autoSaveWCStoPrefsCB) {
                autoSaveWCStoPrefs = true;
                Prefs.set("Astronomy_Tool.autoSaveWCStoPrefs", autoSaveWCStoPrefs);
                updatePrefsFromWCS(true);
            }
        } else if (e.getStateChange() == ItemEvent.DESELECTED) {
            if (source == autoConvertCB) {
                autoConvert = false;
                Prefs.set("Astronomy_Tool.autoConvert", autoConvert);
            } else if (source == useFixedMinMaxValuesCB) {
                useFixedMinMaxValues = false;
                getBiSliderStatistics();
                if (imp.getType() == ImagePlus.COLOR_RGB || imp.getType() == ImagePlus.GRAY8) {
                    minValue = cal.getCValue(0);
                    maxValue = cal.getCValue(255);
                } else if (imp.getType() == ImagePlus.GRAY16 && stats.max - stats.min < 256) {
                    minValue = stats.min;
                    maxValue = stats.min + 255;
                } else {
                    minValue = stats.min;
                    maxValue = stats.max;
                    Prefs.set("Astronomy_Tool.useFixedMinMaxValues", useFixedMinMaxValues);
                }
                updateMinMaxValueTextFields();
                updateHistogramValues(false);
            } else if (source == autoGrabBandCFromHistogramCB) {
                autoGrabBandCFromHistogram = false;
                Prefs.set("Astronomy_Tool.autoGrabBandCFromHistogram", autoGrabBandCFromHistogram);
            } else if (source == usePreviousSizeCB) {
                startupPrevSize = false;
                Prefs.set("Astronomy_Tool.startupPrevSize", startupPrevSize);
            } else if (source == usePreviousPanCB) {
                startupPrevPan = false;
                Prefs.set("Astronomy_Tool.startupPrevPan", startupPrevPan);
            } else if (source == usePreviousZoomCB) {
                startupPrevZoom = false;
                Prefs.set("Astronomy_Tool.startupPrevZoom", startupPrevZoom);
            } else if (source == rememberWindowLocationCB) {
                rememberWindowLocation = false;
                Prefs.set("Astronomy_Tool.rememberWindowLocation", rememberWindowLocation);
            } else if (source == showMeanNotPeakCB) {
                showMeanNotPeak = false;
                peakLabel.setText("Peak:");
                writeNumericPanelField(photom.peakBrightness(), peakTextField);
                Prefs.set("Astronomy_Tool.showMeanNotPeak", showMeanNotPeak);
            } else if (source == useSexagesimalCB) {
                useSexagesimal = false;
                Prefs.set("Astronomy_Tool.useSexagesimal", useSexagesimal);
            } else if (source == middleClickCenterCB) {
                middleClickCenter = false;
                Prefs.set("Astronomy_Tool.middleClickCenter", middleClickCenter);
            } else if (source == writeMiddleClickValuesLogCB) {
                writeMiddleClickValuesLog = false;
                Prefs.set("Astronomy_Tool.writeMiddleClickValuesLog", writeMiddleClickValuesLog);
            } else if (source == writeMiddleDragValuesLogCB) {
                writeMiddleDragValuesLog = false;
                Prefs.set("Astronomy_Tool.writeMiddleDragValuesLog", writeMiddleDragValuesLog);
            } else if (source == writeMiddleClickValuesTableCB) {
                writeMiddleClickValuesTable = false;
                Prefs.set("Astronomy_Tool.writeMiddleClickValuesTable", writeMiddleClickValuesTable);
            } else if (source == writeMiddleDragValuesTableCB) {
                writeMiddleDragValuesTable = false;
                Prefs.set("Astronomy_Tool.writeMiddleDragValuesTable", writeMiddleDragValuesTable);
            } else if (source == showPhotometerCB) {
                showPhotometer = false;
                ac.setAperture(radius, rBack1, rBack2, showSkyOverlay, showPhotometer);
//                apertureOverlay.clear();
                ac.repaint();
                Prefs.set("Astronomy_Tool.showPhotometer", showPhotometer);
            } else if (source == showRedCrossHairCursorCB) {
                ac.showRedCrossHairCursor = false;
                ac.paint(ac.getGraphics());
                Prefs.set("Astronomy_Tool.showRedCrossHairCursor", ac.showRedCrossHairCursor);
            } else if (source == removeBackStarsCB) {
                removeBackStars = false;
                Prefs.set("aperture.removebackstars", removeBackStars);
                photom.setRemoveBackStars(removeBackStars);
            } else if (source == showRemovedPixelsCB) {
                showRemovedPixels = false;
                OverlayCanvas oc = OverlayCanvas.getOverlayCanvas(imp);
                oc.removePixelRois();
                ac.paint(ac.getGraphics());
                Prefs.set("aperture.showremovedpixels", showRemovedPixels);
            } else if (source == rightClickAnnotateCB) {
                rightClickAnnotate = false;
                Prefs.set("Astronomy_Tool.rightClickAnnotate", rightClickAnnotate);
            } else if (source == useSimbadSearchCB) {
                useSimbadSearch = false;
                Prefs.set("Astronomy_Tool.useSimbadSearch", useSimbadSearch);
            } else if (source == showInSimbadCB) {
                showInSimbad = false;
                Prefs.set("Astronomy_Tool.showInSimbad", showInSimbad);
            } else if (source == autoUpdateAnnotationsInHeaderCB) {
                autoUpdateAnnotationsInHeader = false;
                Prefs.set("aperture.autoUpdateAnnotationsInHeader", autoUpdateAnnotationsInHeader);
            } else if (source == autoDisplayAnnotationsFromHeaderCB) {
                autoDisplayAnnotationsFromHeader = false;
                Prefs.set("aperture.autoDisplayAnnotationsFromHeader", autoDisplayAnnotationsFromHeader);
            } else if (source == showMeasureSexCB) {
                showMeasureSex = false;
                Prefs.set("Astronomy_Tool.showMeasureSex", showMeasureSex);
            } else if (source == showMeasureCircleCB) {
                showMeasureCircle = false;
                Prefs.set("Astronomy_Tool.showMeasureCircle", showMeasureCircle);
            } else if (source == showMeasureCrosshairCB) {
                showMeasureCrosshair = false;
                Prefs.set("Astronomy_Tool.showMeasureCrosshair", showMeasureCrosshair);
            } else if (source == showMeasureMultiLinesCB) {
                showMeasureMultiLines = false;
                Prefs.set("Astronomy_Tool.showMeasureMultiLines", showMeasureMultiLines);
            } else if (source == showMeasureLengthCB) {
                showMeasureLength = false;
                Prefs.set("Astronomy_Tool.showMeasureLength", showMeasureLength);
            } else if (source == showMeasurePACB) {
                showMeasurePA = false;
                Prefs.set("Astronomy_Tool.showMeasurePA", showMeasurePA);
            } else if (source == showMeasureDelMagCB) {
                showMeasureDelMag = false;
                Prefs.set("Astronomy_Tool.showMeasureDelMag", showMeasureDelMag);
            } else if (source == negateMeasureDelMagCB) {
                negateMeasureDelMag = false;
                Prefs.set("Astronomy_Tool.negateMeasureDelMag", negateMeasureDelMag);
            } else if (source == showMeasureFluxRatioCB) {
                showMeasureFluxRatio = false;
                Prefs.set("Astronomy_Tool.showMeasureFluxRatio", showMeasureFluxRatio);
            } else if (source == writeMeasureLengthLogCB) {
                writeMeasureLengthLog = false;
                Prefs.set("Astronomy_Tool.writeMeasureLengthLog", writeMeasureLengthLog);
            } else if (source == writeMeasureLengthTableDegCB) {
                writeMeasureLengthTableDeg = false;
                Prefs.set("Astronomy_Tool.writeMeasureLengthTableDeg", writeMeasureLengthTableDeg);
            } else if (source == writeMeasureLengthTableMinCB) {
                writeMeasureLengthTableMin = false;
                Prefs.set("Astronomy_Tool.writeMeasureLengthTableMin", writeMeasureLengthTableMin);
            } else if (source == writeMeasureLengthTableSecCB) {
                writeMeasureLengthTableSec = false;
                Prefs.set("Astronomy_Tool.writeMeasureLengthTableSec", writeMeasureLengthTableSec);
            } else if (source == writeMeasurePACB) {
                writeMeasurePA = false;
                Prefs.set("Astronomy_Tool.writeMeasurePA", writeMeasurePA);
            } else if (source == writeMeasureDelMagCB) {
                writeMeasureDelMag = false;
                Prefs.set("Astronomy_Tool.writeMeasureDelMag", writeMeasureDelMag);
            } else if (source == writeMeasureFluxRatioCB) {
                writeMeasureFluxRatio = false;
                Prefs.set("Astronomy_Tool.writeMeasureFluxRatio", writeMeasureFluxRatio);
            } else if (source == writePhotometricDataTableCB) {
                writePhotometricDataTable = false;
                Prefs.set("Astronomy_Tool.writePhotometricDataTable", writePhotometricDataTable);
            } else if (source == showZoomCB) {
                showZoom = false;
                ac.setShowZoom(showZoom);
                ac.paint(ac.getGraphics());
                Prefs.set("Astronomy_Tool.showZoom", showZoom);
            } else if (source == showDirCB) {
                showDir = false;
                ac.setShowDir(showDir);
                ac.paint(ac.getGraphics());
                Prefs.set("Astronomy_Tool.showDir", showDir);
            } else if (source == showXYCB) {
                showXY = false;
                ac.setShowXY(showXY);
                ac.paint(ac.getGraphics());
                Prefs.set("Astronomy_Tool.showXY", showXY);
            } else if (source == showScaleXCB) {
                showScaleX = false;
                ac.setShowPixelScale(showScaleX, showScaleY, pixelScaleX, pixelScaleY);
                ac.paint(ac.getGraphics());
                Prefs.set("Astronomy_Tool.showScaleX", showScaleX);
            } else if (source == showScaleYCB) {
                showScaleY = false;
                ac.setShowPixelScale(showScaleX, showScaleY, pixelScaleX, pixelScaleY);
                ac.paint(ac.getGraphics());
                Prefs.set("Astronomy_Tool.showScaleY", showScaleY);
            } else if (source == showAbsMagCB) {
                showAbsMag = false;
                ac.setShowAbsMag(showAbsMag);
                ;
                ac.paint(ac.getGraphics());
                Prefs.set("Astronomy_Tool.showAbsMag", showAbsMag);
            } else if (source == showIntCntWithAbsMagCB) {
                showIntCntWithAbsMag = false;
                ac.setShowIntCntWithAbsMag(showIntCntWithAbsMag);
                ac.paint(ac.getGraphics());
                Prefs.set("Astronomy_Tool.showIntCntWithAbsMag", showIntCntWithAbsMag);
            } else if (source == autoNupEleftRB) {
                autoNupEleft = false;
                Prefs.set("Astronomy_Tool.autoNupEleft", autoNupEleft);
            } else if (source == invertNoneRB) {
                invertNoneRB.setState(true);
            } else if (source == invertXRB) {
                invertXRB.setState(true);
            } else if (source == invertYRB) {
                invertYRB.setState(true);
            } else if (source == invertXYRB) {
                invertXYRB.setState(true);
            } else if (source == rotate0RB) {
                rotate0RB.setState(true);
            } else if (source == rotate90RB) {
                rotate90RB.setState(true);
            } else if (source == rotate180RB) {
                rotate180RB.setState(true);
            } else if (source == rotate270RB) {
                rotate270RB.setState(true);
            } else if (source == autoContrastRB) {
                autoContrastRB.setState(true);
                setAutoLevels(null);
            } else if (source == fixedContrastRB) {
                fixedContrastRB.setState(true);
            } else if (source == useFullRangeRB) {
                useFullRangeRB.setState(true);
                blackValue = minValue;
                whiteValue = maxValue;
                updatePanelValues();
            } else if (source == useSIPAllProjectionsCB) {
                useSIPAllProjections = false;
                Prefs.set("Astronomy_Tool.useSIPAllProjections", useSIPAllProjections);
                if (wcs != null) {
                    wcs.setUseSIPAlways(useSIPAllProjections);
                    extraInfo = " (" + wcs.coordsys + ")";
                    imp.updateAndDraw();
//                    ac.paint(getGraphics());
                }
            } else if (source == autoSaveWCStoPrefsCB) {
                autoSaveWCStoPrefs = false;
                Prefs.set("Astronomy_Tool.autoSaveWCStoPrefs", autoSaveWCStoPrefs);
            }
        }
    }

    void setOrientation(boolean saveConfig) {
        if (isNonScienceImage()) {
            determineOrientation();
        }
        ac.setOrientation(invertX, invertY, rotation);
        ac.updateZoomBoxParameters();
        netFlipX = ac.getNetFlipX();
        netFlipY = ac.getNetFlipY();
        netRotate = ac.getNetRotate();
        ac.paint(ac.getGraphics());
        if (saveConfig) {
            Prefs.set("Astronomy_Tool.invertX", invertX);
            Prefs.set("Astronomy_Tool.invertY", invertY);
            Prefs.set("Astronomy_Tool.rotation", rotation);
        }
    }

    /**
     * Fixes a draw bug in MA where aperture do not get cleared/rendered in the proper location.
     * Unneeded when {@link AstroStackWindow#autoNupEleft} is {@code true} as
     * {@link AstroStackWindow#setOrientation(boolean)} will be called.
     * Update KC 20210823: apertures flash for large images when autoNupELeft is excluded. Removed the corresponding if/return.
     */
    public void repaintAstroCanvas() {
        //if (autoNupEleft) return;
        setOrientation();
    }

    void setOrientation() {
        setOrientation(isNonScienceImage());
    }

    void setBestOrientation() {
        //boolean usewcs = Prefs.get ("multiaperture.usewcs", false);
        //IJ.log("Running Best Orientation");
        determineOrientation();
        setOrientation(!isNonScienceImage());
    }

    private void determineOrientation() {
        if (!autoNupEleft) return;

        // Determine orientation based on WCS
        if (wcs != null && wcs.hasWCS()) {
            double npa = (360 + wcs.getNorthPA()) % 360;
            double epa = (360 + wcs.getEastPA()) % 360;
            invertY = (npa > 90 && npa < 270) ? true : false;
            invertX = (epa < 0 || epa > 180) ? true : false;
            if (invertXYRB != null && invertY && invertX) {
                if (!invertXYRB.getState()) invertXYRB.setState(true);
                if (invertXRB.getState()) invertXRB.setState(false);
                if (invertYRB.getState()) invertYRB.setState(false);
                if (invertNoneRB.getState()) invertNoneRB.setState(false);
            } else if (invertXYRB != null && !invertY && !invertX) {
                if (invertXYRB.getState()) invertXYRB.setState(false);
                if (invertXRB.getState()) invertXRB.setState(false);
                if (invertYRB.getState()) invertYRB.setState(false);
                if (!invertNoneRB.getState()) invertNoneRB.setState(true);
            } else if (invertXYRB != null && invertY) {
                if (invertXYRB.getState()) invertXYRB.setState(false);
                if (invertXRB.getState()) invertXRB.setState(false);
                if (!invertYRB.getState()) invertYRB.setState(true);
                if (invertNoneRB.getState()) invertNoneRB.setState(false);
            } else if (invertXYRB != null && invertX) {
                if (invertXYRB.getState()) invertXYRB.setState(false);
                if (!invertXRB.getState()) invertXRB.setState(true);
                if (invertYRB.getState()) invertYRB.setState(false);
                if (invertNoneRB.getState()) invertNoneRB.setState(false);
            }
            rotation = AstroCanvas.ROT_0;
            if (rotate0RB != null) {
                if (!rotate0RB.getState()) rotate0RB.setState(true);
                if (rotate90RB.getState()) rotate90RB.setState(false);
                if (rotate180RB.getState()) rotate180RB.setState(false);
                if (rotate270RB.getState()) rotate270RB.setState(false);
            }
        }

        // Ignore orientation for pngs and jpgs
        if (isNonScienceImage()) {
            invertX = false;
            invertY = false;
            rotation = AstroCanvas.ROT_0;
            showZoom = false;
            showDir = false;
            showXY = false;
            showScaleX = false;
            showScaleY = false;
            if (invertXYRB != null) {
                if (invertXYRB.getState()) invertXYRB.setState(false);
                if (invertXRB.getState()) invertXRB.setState(false);
                if (invertYRB.getState()) invertYRB.setState(false);
                if (!invertNoneRB.getState()) invertNoneRB.setState(true);
                if (!rotate0RB.getState()) rotate0RB.setState(true);
                if (rotate90RB.getState()) rotate90RB.setState(false);
                if (rotate180RB.getState()) rotate180RB.setState(false);
                if (rotate270RB.getState()) rotate270RB.setState(false);
                if (showZoomCB.getState()) showZoomCB.setState(false);
                if (showDirCB.getState()) showDirCB.setState(false);
                if (showXYCB.getState()) showXYCB.setState(false);
                if (showScaleXCB.getState()) showScaleXCB.setState(false);
                if (showScaleYCB.getState()) showScaleYCB.setState(false);
            }
        }
    }

    private boolean isNonScienceImage() {
        String fileName = IJU.getSliceFilename(imp);
        return autoNupEleft && (fileName.endsWith(".png") || fileName.endsWith(".jpg"));
    }


    public void actionPerformed(ActionEvent e) {
        Object b = e.getSource();
        currentSlice = imp.getCurrentSlice();

//------FILE menu--------------------------------------------------------------------------------------

        if (b == openMenuItem) {
            ImagePlus imp2 = IJ.openImage();
            if (imp2 != null) {
                cal = imp2.getCalibration();
                StackProcessor sp = new StackProcessor(imp.getStack(), imp2.getProcessor());
                ImageStack s2 = imp2.getImageStack();
                imp.setStack(s2);
                imp.setFileInfo(imp2.getFileInfo());
                copyImageProperties(imp2);
                imp.setProcessor(imp2.getTitle(), imp2.getProcessor());
                imp.setCalibration(cal);
                setAstroProcessor(false);
            }
        } else if (b == openInNewWindowMenuItem) {
            IJ.run("Open...");
        } else if (b == openSeqMenuItem) {
//				    String path = IJ.getDirectory ("current");
//                    DirectoryChooser.setDefaultDirectory(path);
//                    DirectoryChooser od = new DirectoryChooser ("Open Image Sequence...");
//        			if (od.getDirectory() != null)
//                        {
//                        path = od.getDirectory();
            ImagePlus imp2 = FolderOpener.open(null);
            StackProcessor sp = new StackProcessor(imp.getStack(), imp2.getProcessor());
            ImageStack s2 = imp2.getImageStack();
            imp.setStack(s2);
            imp.setFileInfo(imp2.getFileInfo());
            copyImageProperties(imp2);
            imp.setProcessor(imp2.getTitle(), imp2.getProcessor());
            setAstroProcessor(false);
//                        }
        } else if (b == openSeqInNewWindowMenuItem) {
            IJ.run("Image Sequence...");
        } else if (b == openMeasurementsTableMenuItem) {
            IJ.runPlugIn("Astronomy.Read_MeasurementTable", "");
        } else if (b == openAperturesMenuItem) {
            OpenDialog of = new OpenDialog("Open saved apertures", "");
            if (of.getDirectory() == null || of.getFileName() == null)
                return;
            openApertures(of.getDirectory() + of.getFileName());
        } else if (b == saveAperturesMenuItem) {
            SaveDialog sf = new SaveDialog("Save aperture configuration", imp.getShortTitle(), "");
            if (sf.getDirectory() == null || sf.getFileName() == null)
                return;
            String apsPath = sf.getDirectory() + sf.getFileName();
            int location = apsPath.lastIndexOf('.');
            if (location >= 0) apsPath = apsPath.substring(0, location);
            IJU.saveApertures(apsPath + ".apertures", this);
        } else if (b == openRaDecAperturesMenuItem) {
            IJU.openRaDecApertures();
        } else if (b == saveRaDecAperturesMenuItem) {
            IJU.updateApertures(imp);
            IJU.saveRaDecApertures();
        } else if (b == saveDisplayAsJpgMenuItem) {
            saveImageDisplay("jpg", false);
        } else if (b == saveDisplayAsPdfMenuItem) {
            saveImageDisplay("pdf", false);
        } else if (b == saveDisplayAsPngMenuItem) {
            saveImageDisplay("png", false);
        } else if (b == saveStatePNGMenuItem) {
            if (saveAllPNG)
                saveImageDisplay("png", true);
            else
                saveImageDisplay("jpg", true);
        } else if (b == setSaveStateMenuItem) {
            if (imp.getType() == ImagePlus.COLOR_RGB) imp.getProcessor().reset();
            setSaveStateDialog();
        } else if (b == saveMenuItem) {
            if (imp.getType() == ImagePlus.COLOR_RGB) imp.getProcessor().reset();
            String p = null;
            if (imp.getOriginalFileInfo() != null) {
                String imageDirname = imp.getOriginalFileInfo().directory;
                String imageFilename = IJU.getSliceFilename(imp);
                p = imageDirname + imageFilename;
            }
            IJU.saveFile(imp, p);
        } else if (b == saveFitsMenuItem) {
            FITS_Writer.savingThread.submit(() -> {
                var l = imp.lockSilently();
                FITS_Writer.saveImage(imp, null, imp.getCurrentSlice());
                if (l) imp.unlock();
            });
        } else if (b == saveFitsStack3DMenuItem) {
            FITS_Writer.savingThread.submit(() -> {
                var l = imp.lockSilently();
                FITS_Writer.saveImage(imp, null);
                if (l) imp.unlock();
            });
        } else if (b == saveTiffMenuItem) {
            if (imp.getType() == ImagePlus.COLOR_RGB) imp.getProcessor().reset();
            IJ.run("Tiff...");
            if (imp.getType() == ImagePlus.COLOR_RGB) setAstroProcessor(false);
        } else if (b == saveJpegMenuItem) {
            if (imp.getType() == ImagePlus.COLOR_RGB) imp.getProcessor().reset();
            IJ.run("Jpeg...");
            if (imp.getType() == ImagePlus.COLOR_RGB) setAstroProcessor(false);
        } else if (b == savePdfMenuItem) {
            if (imp.getType() == ImagePlus.COLOR_RGB) imp.getProcessor().reset();
            SaveDialog sf = new SaveDialog("Save plot image as PDF...", imp.getTitle(), ".pdf");
            if (sf.getDirectory() == null || sf.getFileName() == null) return;
            var path = sf.getDirectory() + sf.getFileName();
            ImagePlus image = WindowManager.getCurrentImage();
            if (image == null) {
                IJ.beep();
                IJ.showMessage("No image to save");
                return;
            }
            IJ.runPlugIn(image, PdfRasterWriter.class.getCanonicalName(), path);
            if (imp.getType() == ImagePlus.COLOR_RGB) setAstroProcessor(false);
        } else if (b == saveGifMenuItem) {
            if (imp.getType() == ImagePlus.COLOR_RGB) imp.getProcessor().reset();
            IJ.run("Gif...");
            if (imp.getType() == ImagePlus.COLOR_RGB) setAstroProcessor(false);
        } else if (b == savePngMenuItem) {
            if (imp.getType() == ImagePlus.COLOR_RGB) imp.getProcessor().reset();
            IJ.run("PNG...");
            if (imp.getType() == ImagePlus.COLOR_RGB) setAstroProcessor(false);
        } else if (b == saveBmpMenuItem) {
            if (imp.getType() == ImagePlus.COLOR_RGB) imp.getProcessor().reset();
            IJ.run("BMP...");
            if (imp.getType() == ImagePlus.COLOR_RGB) setAstroProcessor(false);
        } else if (b == saveAviMenuItem) {
            if (imp.getType() == ImagePlus.COLOR_RGB) imp.getProcessor().reset();
            IJ.run("AVI... ");
            if (imp.getType() == ImagePlus.COLOR_RGB) setAstroProcessor(false);
        } else if (b == saveStackSequenceMenuItem) {
            if (imp.getType() == ImagePlus.COLOR_RGB) imp.getProcessor().reset();
            IJ.run("Image Sequence... ");
            if (imp.getType() == ImagePlus.COLOR_RGB) setAstroProcessor(false);
        }//backupAllAIJPrefsMenuItem
        else if (b == backupAllAIJPrefsMenuItem) {
            savePrefs();
            IJU.backupAllAIJSettings(true);
        } else if (b == restoreAllAIJPrefsMenuItem) {
            savePrefs();
            IJU.restoreAllAIJSettings();
        } else if (b == restoreDefaultAIJPrefsMenuItem) {
            savePrefs();
            IJU.restoreDefaultAIJSettings(true);
        } else if (b == exitMenuItem) {
            saveAndClose(true);
        }

//-----PREFERENCES menu -------------------------------------------------------------


        else if (b == setZoomIndicatorSizeMenuItem) {
            setZoomIndicatorSizeDialog();
        } else if (b == simbadSearchRadiusMenuItem) {
            setSimbadSearchRadiusDialog();
        } else if (b == defaultAnnotationColorMenuItem) {
            setDefaultAnnotationColor();
        } else if (b == defaultMeasurementColorMenuItem) {
            setDefaultMeasurementColor();
        } else if (b == setAutoScaleParametersMenuItem) {
            setAutoScaleParametersDialog();
        } else if (b == grabAutoScaleParametersMenuItem) {
            grabAutoScaleParameters();
        } else if (b == resetAutoScaleParametersMenuItem) {
            resetAutoScaleParameters();
        }

        // Other are checkboxMenuItems


//-----VIEW menu --------------------------------------------------------------------

        else if (b == clearOverlayMenuItem) {
            IJ.runPlugIn("Astronomy.Clear_Overlay", "");
        }

        // Others are checkboxMenuItems


//-----ANNOTATE menu --------------------------------------------------------------------

        else if (b == annotateMenuItem) {
            displayAnnotation(new double[]{startDragX, startDragY});
        } else if (b == editAnnotationMenuItem) {
            if (!ac.showAnnotations) {
                IJ.showMessage("Enable display of annotations before editing an annotation");
                return;
            }
            AnnotateRoi roi = OverlayCanvas.getOverlayCanvas(imp).findAnnotateRoi(startDragX, startDragY);
            if (roi == null)
                IJ.showMessage("No annotation found at last clicked image position");
            else
                editAnnotateRoi(roi);
        } else if (b == deleteAnnotationMenuItem) {
            if (!ac.showAnnotations) {
                IJ.showMessage("Enable display of annotations before removing an annotation");
                return;
            }
            AnnotateRoi roi = OverlayCanvas.getOverlayCanvas(imp).findAnnotateRoi(startDragX, startDragY);
            if (roi == null)
                IJ.showMessage("No annotation found at last clicked image position");
            else
                removeAnnotateRoi(startDragX, startDragY);
        } else if (b == clearAllAnnotateRoisMenuItem) {
            if (!ac.showAnnotations) {
                IJ.showMessage("Enable display of annotations before removing annotations");
                return;
            }
            OverlayCanvas.getOverlayCanvas(imp).removeAnnotateRois();
            imp.updateAndDraw();
        } else if (b == annotateFromHeaderMenuItem) {
            OverlayCanvas.getOverlayCanvas(imp).removeAnnotateRois();
            displayAnnotationsFromHeader(true, true, true);
        } else if (b == annotateAppendFromHeaderMenuItem) {
            displayAnnotationsFromHeader(false, true, true);
        } else if (b == replaceAnnotationsInHeaderMenuItem) {
            FitsJ.putHeader(imp, FitsJ.removeAnnotateCards(FitsJ.getHeader(imp)));
            addDisplayedAnnotationsToHeader();
        } else if (b == appendToAnnotationsInHeaderMenuItem) {
            addDisplayedAnnotationsToHeader();
        } else if (b == deleteAnnotationsFromHeaderMenuItem) {
            FitsJ.putHeader(imp, FitsJ.removeAnnotateCards(FitsJ.getHeader(imp)));
        }


//clearAllAnnotateRoisMenuItem, annotateFromHeaderMenuItem, annotateAppendFromHeaderMenuItem, replaceAnnotationsInHeaderMenuItem,
//                appendToAnnotationsInHeaderMenuItem, deleteAnnotationsFromHeaderMenuItem
        // Others are checkboxMenuItems


//-----EDIT menu --------------------------------------------------------------------

        else if (b == apertureSettingsMenuItem) {
            IJ.runPlugIn("Astronomy.Set_Aperture", "");
        } else if (b == measurementSettingsMenuItem) {
            IJ.run("Set Measurements...", "");
        } else if (b == editFitsHeaderMenuItem) {
            FitsHeaderEditor fhe = new FitsHeaderEditor(imp);
        } else if (b == copyFitsHeaderMenuItem) {
            IJ.runPlugIn("Astronomy.Copy_FITS_Header", imp.getTitle());
        } else if (b == stackSorterMenuItem) {
            IJ.runPlugIn("Stack_Sorter", "");
        }

//-----PROCESS menu ------------------------------------------------------------------


        else if (b == dataReducerMenuItem) {
            IJ.runPlugIn("Astronomy.Data_Processor", "");
        } else if (b == combineStackImagesMenuItem) {
            if (imp.getType() == ImagePlus.COLOR_RGB) imp.getProcessor().reset();
            IJ.run("Z Project...", "");
        } else if (b == concatStacksMenuItem) {
            if (imp.getType() == ImagePlus.COLOR_RGB) imp.getProcessor().reset();
            IJ.run("Concatenate...", "");
        } else if (b == imageCalcMenuItem) {
            if (imp.getType() == ImagePlus.COLOR_RGB) imp.getProcessor().reset();
            IJ.run("Image Calculator...", "");
        } else if (b == copyFitsHeaderProcessMenuItem) {
            IJ.runPlugIn("Astronomy.Copy_FITS_Header", imp.getTitle());
        } else if (b == removeOutliersMenuItem) {
            if (imp.getType() == ImagePlus.COLOR_RGB) imp.getProcessor().reset();
            IJ.run("Remove Outliers...", "");
        } else if (b == smoothMenuItem) {
            if (imp.getType() == ImagePlus.COLOR_RGB) imp.getProcessor().reset();
            IJ.run("Smooth", "");
        } else if (b == sharpenMenuItem) {
            if (imp.getType() == ImagePlus.COLOR_RGB) imp.getProcessor().reset();
            IJ.run("Sharpen", "");
        } else if (b == normalizeStackMenuItem) {
            if (imp.getType() == ImagePlus.COLOR_RGB) imp.getProcessor().reset();
            IJ.runPlugIn("Astronomy.Normalize_Stack", "");
        } else if (b == alignStackMenuItem) {
            if (imp.getType() == ImagePlus.COLOR_RGB) imp.getProcessor().reset();
            IJ.runPlugIn("Astronomy.Stack_Aligner", "");
            if (imp.getType() == ImagePlus.COLOR_RGB) setAutoLevels(null);
        } else if (b == imageStabilizerMenuItem) {
            if (imp.getType() == ImagePlus.COLOR_RGB) imp.getProcessor().reset();
            IJ.runPlugIn("Astronomy.Image_Stabilizer_AIJ", "");
            if (imp.getType() == ImagePlus.COLOR_RGB) setAutoLevels(null);
        } else if (b == imageStabilizerApplyMenuItem) {
            if (imp.getType() == ImagePlus.COLOR_RGB) imp.getProcessor().reset();
            IJ.runPlugIn("Image_Stabilizer_Log_Applier", "");
        } else if (b == shiftImageMenuItem) {
            if (imp.getType() == ImagePlus.COLOR_RGB) imp.getProcessor().reset();
            IJ.runPlugIn("Astronomy.Image_Shifter", "");
            if (imp.getType() == ImagePlus.COLOR_RGB) setAutoLevels(null);
        } else if (b == selectBestFramesMenuItem) {
            if (imp.getType() == ImagePlus.COLOR_RGB) imp.getProcessor().reset();
            Thread t = new Thread() {
                public void run() {
                    if (imp.getType() == ImagePlus.COLOR_RGB)
                        IJ.runPlugIn("Astronomy.Find_Focused_Slices_RGB", "");
                    else
                        IJ.runPlugIn("Astronomy.Find_Focused_Slices_", "");
                }
            };
            t.start();
        } else if (b == flipDataXMenuItem) {
            if (imp.getType() == ImagePlus.COLOR_RGB) imp.getProcessor().reset();
            flipDataX = true;
            startDataFlipRotate();
        } else if (b == flipDataYMenuItem) {
            if (imp.getType() == ImagePlus.COLOR_RGB) imp.getProcessor().reset();
            flipDataY = true;
            startDataFlipRotate();
        } else if (b == rotateDataCWMenuItem) {
            if (imp.getType() == ImagePlus.COLOR_RGB) imp.getProcessor().reset();
            rotateDataCW = true;
            startDataFlipRotate();
        } else if (b == rotateDataCCWMenuItem) {
            if (imp.getType() == ImagePlus.COLOR_RGB) imp.getProcessor().reset();
            rotateDataCCW = true;
            startDataFlipRotate();
        }


//-----Color menu ------------------------------------------------------------------

        else if (b == RGBComposerMenuItem) {
            if (imp.getType() == ImagePlus.COLOR_RGB) imp.getProcessor().reset();
            IJ.runPlugIn("Astronomy.RGB_Composer_AIJ", "");
        } else if (b == splitChannelsMenuItem) {
            if (imp.getType() == ImagePlus.COLOR_RGB) imp.getProcessor().reset();
            IJ.doCommand("Split Channels");
        } else if (b == imagesToStackMenuItem) {
            if (imp.getType() == ImagePlus.COLOR_RGB) imp.getProcessor().reset();
            IJ.doCommand("Images to Stack");
        } else if (b == stackToImagesMenuItem) {
            if (imp.getType() == ImagePlus.COLOR_RGB) imp.getProcessor().reset();
            IJ.doCommand("Stack to Images");
        } else if (b == debayerMenuItem) {
            if (imp.getType() == ImagePlus.COLOR_RGB) imp.getProcessor().reset();
            IJ.runPlugIn("Astronomy.Debayer_Image_FITS", "");
        } else if (b == photoDebayerMenuItem) {
            if (imp.getType() == ImagePlus.COLOR_RGB) imp.getProcessor().reset();
            Executors.newSingleThreadExecutor().submit(() -> IJ.runPlugIn(PhotometricDebayer.class.getName(), ""));
        } else if (b == makeCompositeMenuItem) {
            if (imp.getType() == ImagePlus.COLOR_RGB) imp.getProcessor().reset();
            IJ.doCommand("Make Composite");
            saveAndClose(false);
        } else if (b == stackToRGBMenuItem) {
            if (imp.getType() == ImagePlus.COLOR_RGB) imp.getProcessor().reset();
            IJ.doCommand("Stack to RGB");
        }

//-----ANALYZE menu ------------------------------------------------------------------


        else if (b == multiApertureMenuItem) {
            Executors.newSingleThreadExecutor().submit(() -> IJ.runPlugIn("Astronomy.MultiAperture_", ""));
        } else if (b == multiPlotMenuItem) {
            IJ.runPlugIn("Astronomy.MultiPlot_", "");
        } else if (b == measurementMenuItem) {
            IJ.run("Measure", "");
        } else if (b == seeingProfileMenuItem) {
            IJ.runPlugIn("Astronomy.Seeing_Profile", "");
        } else if (b == staticProfilerMenuItem) {
            IJ.run("Plot Profile", "");
        } else if (b == dynamicProfilerMenuItem) {
            IJ.runPlugIn("Dynamic_Profiler", "");
        } else if (b == contourLinesMenuItem) {
            IJ.runPlugIn("Astronomy.ContourLines_", "imp");
        } else if (b == contourPlottersMenuItem) {
            IJ.runPlugIn("Astronomy.ContourPlotter_", "imp");
        } else if (b == azimuthalAverageMenuItem) {
            IJ.runPlugIn("Azimuthal_Average", "imp");
        } else if (b == threeDSurfacePlotMenuItem) {
            IJ.runPlugIn("Interactive_3D_Surface_Plot", "");
        }


//-----WCS menu ------------------------------------------------------------------
        else if (b == astrometryMenuItem) {
            handleAstrometry(false);
        } else if (b == astrometrySetupMenuItem) {
            handleAstrometry(true);
        } else if (b == saveWCStoPrefsMenuItem) {
            updatePrefsFromWCS(true);
        } else if (b == setPixelScaleMenuItem) {
            setPixelScaleDialog();
        } else if (b == dirAngleMenuItem) {
            setDirAngleDialog();
        }


//-----TEXT fields ------------------------------------------------------------------

        else if (b == blackTextfield || b == whiteTextfield || b == minValueTextField || b == maxValueTextField) {
            if (blackTextfield.isEditable()) {
                blackValue = Double.parseDouble(blackTextfield.getText().replaceAll(",", ""));
                sliceMin[imp.getCurrentSlice() - 1] = blackValue;
                savedBlackValue = blackValue;
                Prefs.set("Astronomy_Tool.savedMin", savedBlackValue);
            }
            if (whiteTextfield.isEditable()) {
                whiteValue = Double.parseDouble(whiteTextfield.getText().replaceAll(",", ""));
                sliceMax[imp.getCurrentSlice() - 1] = whiteValue;
                savedWhiteValue = whiteValue;
                Prefs.set("Astronomy_Tool.savedMax", savedWhiteValue);
            }
            if (minValueTextField.isEditable()) {
                minValue = Double.parseDouble(minValueTextField.getText().replaceAll(",", ""));
                if (imp.getType() == ImagePlus.GRAY16 && maxValue - minValue < 256)
                    minValue = maxValue - 255;
                if (minValue > maxValue) minValue = maxValue;
                if (blackValue < minValue) blackValue = minValue;
                fixedMinValue = minValue;
                Prefs.set("Astronomy_Tool.fixedMinValue", fixedMinValue);
            }
            if (maxValueTextField.isEditable()) {
                maxValue = Double.parseDouble(maxValueTextField.getText().replaceAll(",", ""));
                if (imp.getType() == ImagePlus.GRAY16 && maxValue - minValue < 256)
                    maxValue = minValue + 255;
                if (maxValue < minValue) maxValue = minValue;
                if (whiteValue > maxValue) whiteValue = maxValue;
                fixedMaxValue = maxValue;
                Prefs.set("Astronomy_Tool.fixedMaxValue", fixedMaxValue);
            }
            updateHistogramValues(b == blackTextfield || b == whiteTextfield);
            if (autoContrast && autoGrabBandCFromHistogram) grabAutoScaleParameters();
        } else if (b == RATextField || b == DecTextField) {
            double[] coords = processCoordinatePair(RATextField, 3, 24, false, DecTextField, 2, 90, true, b == RATextField ? true : false, true);
            if (!Double.isNaN(coords[0]) && !Double.isNaN(coords[1])) {
                coords[0] *= 15.0;
                double pixel[] = wcs.wcs2pixels(coords);
                String label = "";
//                        label += "x = "+pixel[0]+"   y = "+pixel[1]+"   ";
                if (useSexagesimal) {
                    label += hms(coords[0] / 15.0, 3) + " ";
                    if (coords[1] > 0.0)
                        label += "+" + hms(coords[1], 2);
                    else
                        label += hms(coords[1], 2);
                } else {
                    label += sixPlaces.format(coords[0] / 15.0) + " ";
                    if (coords[1] > 0.0)
                        label += "+" + sixPlaces.format(coords[1]);
                    else
                        label += sixPlaces.format(coords[1]);
                }
//                        IJ.log(label);

                if ((e.getModifiers() & MouseEvent.SHIFT_MASK) != 0 || (e.getModifiers() & MouseEvent.CTRL_MASK) != 0) {
                    photom.setSourceApertureRadius(radius);
                    photom.setBackgroundApertureRadii(rBack1, rBack2);
                    photom.setRemoveBackStars(removeBackStars);

                    Photometer phot = new Photometer(imp.getCalibration());
                    phot.setRemoveBackStars(removeBackStars);
                    phot.setMarkRemovedPixels(false);
                    phot.measure(imp, exact, pixel[0], pixel[1], radius, rBack1, rBack2);
                    var centroid = (e.getModifiers() & MouseEvent.SHIFT_MASK) != 0;
                    var isRef = false;
                    ApertureRoi roi = new ApertureRoi(pixel[0], pixel[1], radius, rBack1, rBack2, phot.source, centroid);
                    roi.setAppearance(true, centroid, showSkyOverlay, nameOverlay, valueOverlay, !isRef ? new Color(196, 222, 155) : Color.PINK, (!isRef ? "T" : "C") + 1, phot.source);
                    roi.setAMag(99.999);
                    roi.setImage(imp);
                    roi.setPhantom(true);
                    ac.removePhantomApertureRois();
                    ac.add(roi);
                    ac.paint(ac.getGraphics());
                    MultiAperture_.addApertureAsOld(coords[0], coords[1], pixel[0], pixel[1], (e.getModifiers() & MouseEvent.SHIFT_MASK) != 0);
                    MultiAperture_.apLoading.set(MultiAperture_.ApLoading.IMPORTED);
                } else {
                    addAnnotateRoi(imp, true, false, true, false, pixel[0], pixel[1], radius, label, colorWCS, false);
                }

                imp.draw();
            }
        }


//-----BUTTONS---------------------------------------------------------------------
        else if (b == buttonBroom) {
            IJ.runPlugIn("Astronomy.Clear_Overlay", "");
            OverlayCanvas ocanvas = OverlayCanvas.getOverlayCanvas(imp);
            ocanvas.clearRois();
            imp.updateAndDraw();
        } else if (b == buttonShowAll) {
            openApertures("");
        } else if (b == buttonShowSky) {
            Prefs.set("setaperture.aperturechanged", true);
            showSkyOverlay = !Prefs.get("aperture.skyoverlay", showSkyOverlay);
            Prefs.set("aperture.skyoverlay", showSkyOverlay);
            buttonShowSky.setSelected(showSkyOverlay);
            ac.setAperture(radius, rBack1, rBack2, showSkyOverlay, showPhotometer);
            if (OverlayCanvas.hasOverlayCanvas(imp)) {
                OverlayCanvas oc = OverlayCanvas.getOverlayCanvas(imp);
                Roi[] rois = oc.getRois();
                for (int i = 0; i < rois.length; i++) {
                    if (rois[i] instanceof astroj.ApertureRoi) {
                        astroj.ApertureRoi aroi = (astroj.ApertureRoi) rois[i];
                        aroi.setShowSky(showSkyOverlay);
                    }
                }
            }
            ac.repaint();
        } else if (b == buttonSourceID) {
            nameOverlay = !Prefs.get("aperture.nameoverlay", nameOverlay);
            Prefs.set("aperture.nameoverlay", nameOverlay);
            buttonSourceID.setSelected(nameOverlay);
            if (OverlayCanvas.hasOverlayCanvas(imp)) {
                OverlayCanvas oc = OverlayCanvas.getOverlayCanvas(imp);
                Roi[] rois = oc.getRois();
                for (int i = 0; i < rois.length; i++) {
                    if (rois[i] instanceof astroj.ApertureRoi) {
                        astroj.ApertureRoi aroi = (astroj.ApertureRoi) rois[i];
                        aroi.setShowName(nameOverlay);
                    }
                }
            }
            ac.repaint();
        } else if (b == buttonSourceCounts) {
            valueOverlay = !Prefs.get("aperture.valueoverlay", valueOverlay);
            Prefs.set("aperture.valueoverlay", valueOverlay);
            buttonSourceCounts.setSelected(valueOverlay);
            if (OverlayCanvas.hasOverlayCanvas(imp)) {
                OverlayCanvas oc = OverlayCanvas.getOverlayCanvas(imp);
                Roi[] rois = oc.getRois();
                for (int i = 0; i < rois.length; i++) {
                    if (rois[i] instanceof astroj.ApertureRoi) {
                        astroj.ApertureRoi aroi = (astroj.ApertureRoi) rois[i];
                        aroi.setShowValues(valueOverlay);
                    }
                }
            }
            ac.repaint();
        } else if (b == buttonCentroid) {
            reposition = !Prefs.get("aperture.reposition", reposition);
            Prefs.set("aperture.reposition", reposition);
            buttonCentroid.setSelected(reposition);
        } else if (b == buttonSetAperture) {
            IJ.runPlugIn("Astronomy.Set_Aperture", "");
        } else if (b == buttonDeleteSlice) {
            if (!imp.lock())
                return;
            ImageStack stack = imp.getStack();
            int numSlices = stack.getSize();
            if (numSlices > 2) {
                int n = imp.getCurrentSlice();
                stack.deleteSlice(n);
                imp.setStack(null, stack);
                numSlices--;
                if (n > numSlices)
                    imp.setSlice(numSlices);
                else
                    imp.setSlice(n);

            } else {
                IJ.beep();
            }
            imp.unlock();
        } else if (b == buttonMultiAperture) {
            reenterAstronomyTool();
            Executors.newSingleThreadExecutor().submit(() -> IJ.runPlugIn("Astronomy.MultiAperture_", ""));
        } else if (b == buttonAlign) {
            reenterAstronomyTool();
            IJ.runPlugIn("Astronomy.Stack_Aligner", "");
            if (imp.getType() == ImagePlus.COLOR_RGB) setAutoLevels(null);
        } else if (b == buttonAstrometry) {
            if ((e.getModifiers() & MouseEvent.SHIFT_MASK) != 0 || (e.getModifiers() & MouseEvent.CTRL_MASK) != 0 || (e.getModifiers() & MouseEvent.ALT_MASK) != 0)
                handleAstrometry(false);
            else
                handleAstrometry(true);
        } else if (b == buttonHeader) {
            FitsHeaderEditor fhe = new FitsHeaderEditor(imp);
        } else if (b == buttonNegative) {
            useInvertingLut = !useInvertingLut;
            buttonNegative.setSelected(useInvertingLut);
            ImageProcessor ip = imp.getProcessor();
            if (useInvertingLut != ip.isInvertedLut() && !ip.isColorLut())
                ip.invertLut();
            imp.updateAndDraw();
        } else if (b == buttonClearMeasurements) {
            MultiPlot_.clearPlot();

            if (table != null) {
                table.clearTable();
            } else {
                table = MeasurementTable.getTable(tableName);
                if (table != null) {
                    table.clearTable();
                }
            }

            MultiAperture_.clearTable();

//                                Class MP = Class.forName("MultiPlot_");
//                                Method setTable = MP.getMethod("setTable", MeasurementTable.class, boolean.class);
//                                setTable.invoke(null, table, false);
        } else if (b == buttonFlipX) {
            for (int i = 1; i <= stackSize; i++) {
                imp.setSlice(i);
                imp.getProcessor().flipHorizontal();
                imp.updateAndDraw();
            }
            if (stackSize > 1) {
                imp.setSlice(currentSlice);
                imp.updateAndDraw();
            }
        } else if (b == buttonFlipY) {
            for (int i = 1; i <= stackSize; i++) {
                imp.setSlice(i);
                imp.getProcessor().flipVertical();
                imp.updateAndDraw();
            }
            if (stackSize > 1) {
                imp.setSlice(currentSlice);
                imp.updateAndDraw();
            }
        } else if (b == buttonRotCCW) {
            Calibration cal = imp.getCalibration();
            currentSlice = imp.getCurrentSlice();
            ImageProcessor ip = imp.getProcessor();
            icWidth = ac.getWidth();
            icHeight = ac.getHeight();
            magnification = ac.getMagnification();
            if (ipWidth != ipHeight) {
                StackProcessor sp = new StackProcessor(imp.getStack(), ip);
                ImageStack s2 = null;
                s2 = sp.rotateLeft();
                imp.setStack(null, s2);
                if (IJVersion.compareTo("1.42q") > 0 && IJVersion.compareTo("1.44f") < 0)
                    imp = WindowManager.getImage(impTitle);
                double pixelWidth = cal.pixelWidth;
                cal.pixelWidth = cal.pixelHeight;
                cal.pixelHeight = pixelWidth;
                imp.setCalibration(cal);
                if (imp.getStackSize() > 1)
                    stackRotated = true;
                layoutContainer(this);
                ac.repaint();
//                            refreshAstroWindow();
            } else {
                for (int i = 1; i <= stackSize; i++) {
                    imp.setSlice(i);
                    ip = imp.getProcessor().rotateLeft();
                    imp.setProcessor(null, ip);
                }
                double pixelWidth = cal.pixelWidth;
                cal.pixelWidth = cal.pixelHeight;
                cal.pixelHeight = pixelWidth;
                imp.setCalibration(cal);
                imp.setSlice(currentSlice);
            }
        } else if (b == buttonRotCW) {
            Calibration cal = imp.getCalibration();
            ImageProcessor ip = imp.getProcessor();
            currentSlice = imp.getCurrentSlice();
            if (ipWidth != ipHeight) {
                StackProcessor sp = new StackProcessor(imp.getStack(), ip);
                ImageStack s2 = null;
                s2 = sp.rotateRight();
                imp.setStack(null, s2);
                if (IJVersion.compareTo("1.42q") > 0 && IJVersion.compareTo("1.44f") < 0)
                    imp = WindowManager.getImage(impTitle);
                double pixelWidth = cal.pixelWidth;
                cal.pixelWidth = cal.pixelHeight;
                cal.pixelHeight = pixelWidth;
                imp.setCalibration(cal);
                if (imp.getStackSize() > 1)
                    stackRotated = true;
                ac.repaint();
//                            refreshAstroWindow();
            } else {

//                            ImagePlus impr = NewImage.createFloatImage("temp", ipHeight, ipWidth, stackSize, 0);
//                            ImageProcessor ipr = impr.getProcessor();
//                            for(int i = 1; i <= stackSize; i++)
//                                    {
//                                    imp.setSlice(i);
//                                    impr.setSlice(i);
//                                    ipr = imp.getProcessor().rotateRight();
//                                    impr.setProcessor(null, ipr);
//                                    }
//                            ipr = impr.getProcessor();
//                            imp.flush();
//                            imp.setProcessor(null, ipr);
//                            ip = imp.getProcessor();
//                            imp.setWindow(iw);
                for (int i = 1; i <= stackSize; i++) {
                    imp.setSlice(i);
                    ip = imp.getProcessor().rotateLeft();
                    imp.setProcessor(null, ip);
                }
                double pixelWidth = cal.pixelWidth;
                cal.pixelWidth = cal.pixelHeight;
                cal.pixelHeight = pixelWidth;
                imp.setCalibration(cal);
                imp.setSlice(currentSlice);
//                            Graphics g = ac.getGraphics();
//                            g.clearRect(0, 0, ac.getWidth(), ac.getHeight());
//                            ac.update(g);
                ac.repaint();
            }
        }
//                else if (b==buttonLUT)
//                    {
//                    IJ.run("Image>Lookup Tables");
//                    }
        else if (b == buttonZoomOut) {
            zoomOut(startDragScreenX, startDragScreenY, true, true);
        } else if (b == buttonZoomIn) {
            zoomIn(startDragScreenX, startDragScreenY, true, true, 0.0);
        } else if (b == buttonZoomInFast) {
            zoomIn(startDragScreenX, startDragScreenY, true, true, 8.0);
        } else if (b == buttonFit) {
            if ((e.getModifiers() & e.ALT_MASK) != 0)
                fillNotFit = true;
            fitImageToCanvas();
        } else if (b == buttonAutoLevels) {
            autoScaleIconClicked = true;
            setAutoLevels(null);
        }

        if (imp != null) {
            imp.getCanvas().requestFocusInWindow();
        }
    }

    void handleAstrometry(boolean showSetupPanel) {
        showSetup = showSetupPanel;
        int maxCount = 10;
        int count = 0;
        boolean start = astrometryMenuItem.getLabel().equals("Plate solve using Astrometry.net");

        if (astrometryThread != null && astrometryThread.isAlive() && astrometry.isSetupActive()) {
            buttonAstrometry.setSelected(true);
            return;
        }

        while (astrometryThread != null && astrometryThread.isAlive() && count <= maxCount) {
            if (astrometry != null) {
                astrometry.setAstrometryCanceled();
                count++;
                IJ.wait(1000);
            } else {
                astrometryThread.stop();
                astrometryThread = null;
                break;
            }
        }
        if (count >= maxCount) {
            astrometryMenuItem.setLabel("Stop plate solve process");
            astrometrySetupMenuItem.setLabel("Stop plate solve process");
            buttonAstrometry.setToolTipText("Stop plate solve process");
            buttonAstrometry.setSelected(true);
            IJ.showMessage("Error: unable to halt existing plate solve thread.");
        } else if (start) {
            astrometryThread = new Thread() {
                public void run() {
                    int status = FAILED;
                    astrometryMenuItem.setLabel("Stop plate solve process");
                    astrometrySetupMenuItem.setLabel("Stop plate solve process");
                    buttonAstrometry.setToolTipText("Stop plate solve process");
                    buttonAstrometry.setSelected(true);
                    astrometry = new Astrometry();
                    status = astrometry.solve(imp, showSetup, null, useSexagesimal, false, false, null, null);
                    //astrometry.solve(impIn, runSetup)
                    astrometryMenuItem.setLabel("Plate solve using Astrometry.net");
                    astrometrySetupMenuItem.setLabel("Plate solve using Astrometry.net (with options)...");
                    buttonAstrometry.setToolTipText("<html>plate solve using astrometry.net<br>shift-click or right-click to skip setup options</html>");
                    buttonAstrometry.setSelected(false);
                    if (status == SUCCESS) {
                        IJ.showStatus("Plate solve finished");
                        if (Prefs.get("astrometry.showLog", true)) IJ.log("*****PLATE SOLVE FINISHED*****");
                    } else if (status == CANCELED) {
                        IJ.showStatus("Plate solve canceled");
                        if (Prefs.get("astrometry.showLog", true)) IJ.log("*****PLATE SOLVE CANCELED BY USER*****");
                    } else if (status == SKIPPED) {
                        IJ.showStatus("Plate solve skipped");
                    } else if (status == FAILED) {
                        IJ.showStatus("Plate solve failed");
                        if (Prefs.get("astrometry.showLog", true)) IJ.log("*****PLATE SOLVE FAILED*****");
                    } else {
                        IJ.showStatus("Plate solve invalid return code");
                    }
                }
            };
            astrometryThread.start();
        } else {
            IJ.showStatus("Plate solve canceled");
            astrometryMenuItem.setLabel("Plate solve using Astrometry.net");
            astrometrySetupMenuItem.setLabel("Plate solve using Astrometry.net (with options)...");
            buttonAstrometry.setToolTipText("<html>plate solve using astrometry.net<br>shift-click or right-click to skip setup options</html>");
            buttonAstrometry.setSelected(false);
        }
    }

    void copyImageProperties(ImagePlus impp) {
        //CLEAR PROPERTIES FROM OPENIMAGE
        Enumeration enProps;
        String key;
        Properties props = imp.getProperties();
        if (props != null) {
            enProps = props.propertyNames();
            key = "";
            while (enProps.hasMoreElements()) {
                key = (String) enProps.nextElement();
                imp.setProperty(key, null);
            }
        }
        // COPY NEW PROPERTIES TO OPEN WINDOW IMAGEPLUS
        props = impp.getProperties();
        if (props != null) {
            enProps = props.propertyNames();
            key = "";
            while (enProps.hasMoreElements()) {
                key = (String) enProps.nextElement();
                imp.setProperty(key, props.getProperty(key));
            }
        }
    }

    void setTempFullDynamicRange() {
        tempAutoLevel = autoContrast;
        tempPrevLevels = fixedContrast;
        tempPrevLevelsPerSlice = fixedContrastPerSlice;
        autoContrast = false;
        fixedContrast = false;
        fixedContrastPerSlice = false;
        autoContrastRB.setState(false);
        fixedContrastRB.setState(false);
        fixedContrastPerSliceRB.setState(false);
        useFullRangeRB.setState(true);
        Prefs.set("Astronomy_Tool.startupAutoLevel", autoContrast);
        Prefs.set("Astronomy_Tool.startupPrevLevels", fixedContrast);
        Prefs.set("Astronomy_Tool.startupPrevLevelsPerSlice", fixedContrastPerSlice);
        setAutoLevels(null);
    }

    void clearTempFullDynamicRange() {
        autoContrast = tempAutoLevel;
        fixedContrast = tempPrevLevels;
        fixedContrastPerSlice = tempPrevLevelsPerSlice;
        autoContrastRB.setState(autoContrast);
        fixedContrastRB.setState(fixedContrast);
        fixedContrastPerSliceRB.setState(fixedContrastPerSlice);
        useFullRangeRB.setState(!autoContrast && !fixedContrast && !fixedContrastPerSlice);
        Prefs.set("Astronomy_Tool.startupAutoLevel", autoContrast);
        Prefs.set("Astronomy_Tool.startupPrevLevels", fixedContrast);
        Prefs.set("Astronomy_Tool.startupPrevLevelsPerSlice", fixedContrastPerSlice);
        setAutoLevels(null);
    }

    void setSaveStateDialog() {
        GenericDialog gd = new GenericDialog("Save all settings", getX() + getWidth() / 2 - 165, getY() + getHeight() / 2 - 77);
        gd.enableYesNoCancel("Save Files Now", "Save Settings Only");
        saveImage = Prefs.get("Astronomy_Tool.saveImage", saveImage);
        savePlot = Prefs.get("Astronomy_Tool.savePlot", savePlot);
        saveConfig = Prefs.get("Astronomy_Tool.saveConfig", saveConfig);
        saveTable = Prefs.get("Astronomy_Tool.saveTable", saveTable);
        saveApertures = Prefs.get("Astronomy_Tool.saveApertures", saveApertures);
        saveLog = Prefs.get("Astronomy_Tool.saveLog", saveLog);
        imageSuffix = Prefs.get("Astronomy_Tool.imageSuffix", imageSuffix);
        plotSuffix = Prefs.get("Astronomy_Tool.plotSuffix", plotSuffix);
        configSuffix = Prefs.get("Astronomy_Tool.configSuffix", configSuffix);
        dataSuffix = Prefs.get("Astronomy_Tool.dataSuffix", dataSuffix);
        aperSuffix = Prefs.get("Astronomy_Tool.aperSuffix", aperSuffix);
        logSuffix = Prefs.get("Astronomy_Tool.logSuffix", logSuffix);
        saveAllPNG = Prefs.get("Astronomy_Tool.saveAllPNG", saveAllPNG);

        gd.addMessage("Select items to save when using save all:");
        String[] labels = {"Image", "Plot", "Plot Config", "Data Table", "Apertures", "Log"};
        boolean[] defaults = {saveImage, savePlot, saveConfig, saveTable, saveApertures, saveLog};
        gd.addCheckboxGroup(1, 6, labels, defaults);
        gd.addStringField("Image suffix:", imageSuffix, 40);
        gd.addStringField("Plot image suffix:", plotSuffix, 40);
        gd.addStringField("Plot config file suffix:", configSuffix, 40);
        gd.addStringField("Data table file suffix:", dataSuffix, 40);
        gd.addStringField("Aperture file suffix:", aperSuffix, 40);
        gd.addStringField("Log file suffix:", logSuffix, 40);
        gd.addCheckbox("Save images in PNG format (uncheck for JPEG format)", saveAllPNG);
        gd.addMessage("Tip: make plot config and data table suffix the same so that the plot config\n" +
                "will auto-load when a new data table file is opened by drag and drop.");

        gd.showDialog();
        if (gd.wasCanceled()) return;
        saveImage = gd.getNextBoolean();
        savePlot = gd.getNextBoolean();
        saveConfig = gd.getNextBoolean();
        saveTable = gd.getNextBoolean();
        saveApertures = gd.getNextBoolean();
        saveLog = gd.getNextBoolean();

        imageSuffix = gd.getNextString();
        plotSuffix = gd.getNextString();
        configSuffix = gd.getNextString();
        dataSuffix = gd.getNextString();
        aperSuffix = gd.getNextString();
        logSuffix = gd.getNextString();

        saveAllPNG = gd.getNextBoolean();

        Prefs.set("Astronomy_Tool.saveImage", saveImage);
        Prefs.set("Astronomy_Tool.savePlot", savePlot);
        Prefs.set("Astronomy_Tool.saveConfig", saveConfig);
        Prefs.set("Astronomy_Tool.saveTable", saveTable);
        Prefs.set("Astronomy_Tool.saveApertures", saveApertures);
        Prefs.set("Astronomy_Tool.saveLog", saveLog);
        Prefs.set("Astronomy_Tool.saveAllPNG", saveAllPNG);

        Prefs.set("Astronomy_Tool.imageSuffix", imageSuffix);
        Prefs.set("Astronomy_Tool.plotSuffix", plotSuffix);
        Prefs.set("Astronomy_Tool.configSuffix", configSuffix);
        Prefs.set("Astronomy_Tool.dataSuffix", dataSuffix);
        Prefs.set("Astronomy_Tool.aperSuffix", aperSuffix);
        Prefs.set("Astronomy_Tool.logSuffix", logSuffix);
        if (gd.wasOKed()) {
            if (saveAllPNG)
                saveImageDisplay("png", true);
            else
                saveImageDisplay("jpg", true);
        }
    }

    void setPixelScaleDialog() {
        GenericDialog gd = new GenericDialog("Set pixel scale for images without WCS", getX() + getWidth() / 2 - 165, getY() + getHeight() / 2 - 77);

        gd.addMessage("Enter 0 to report length in pixels.");
        gd.addNumericField("X-pixel scale: ", pixelScaleX, 4, 8, "(seconds of arc per pixel in x-direction)");
        gd.addNumericField("Y-pixel scale: ", pixelScaleY, 4, 8, "(seconds of arc per pixel in y-direction)");
        gd.addMessage("");

        gd.showDialog();
        if (gd.wasCanceled()) return;
        pixelScaleX = gd.getNextNumber();
        pixelScaleY = gd.getNextNumber();
        ac.setShowPixelScale(showScaleX, showScaleY, pixelScaleX, pixelScaleY);
        ac.updateZoomBoxParameters();
        ac.paint(ac.getGraphics());
        Prefs.set("Astronomy_Tool.pixelScaleX", pixelScaleX);
        Prefs.set("Astronomy_Tool.pixelScaleY", pixelScaleY);
    }

    void setZoomIndicatorSizeDialog() {
        GenericDialog gd = new GenericDialog("Set Zoom Indicator Size", getX() + getWidth() / 2 - 170, getY() + getHeight() / 2 - 90);

        gd.addNumericField("Zoom indicator height: ", ac.zoomIndicatorSize, 0, 6, "(pixels)");
        gd.addMessage("(width is scaled according to image aspect ratio)");

        gd.showDialog();
        if (gd.wasCanceled()) return;
        ac.zoomIndicatorSize = (int) gd.getNextNumber();
        ac.updateZoomBoxParameters();
        ac.paint(ac.getGraphics());
        Prefs.set("Astronomy_Tool.zoomIndicatorSize", ac.zoomIndicatorSize);
    }//setSimbadSearchRadiusDialog

    void setSimbadSearchRadiusDialog() {
        GenericDialog gd = new GenericDialog("Set SIMBAD Search Radius", getX() + getWidth() / 2 - 170, getY() + getHeight() / 2 - 90);

        gd.addNumericField("Search Radius: ", simbadSearchRadius, 3, 9, "(arcsec)");

        gd.showDialog();
        if (gd.wasCanceled()) return;
        simbadSearchRadius = gd.getNextNumber();
        Prefs.set("Astronomy_Tool.simbadSearchRadius", simbadSearchRadius);
        simbadSearchRadiusMenuItem.setLabel("Set SIMBAD search radius (currently " + simbadSearchRadius + " arcmin)...");
    }

    void setDirAngleDialog() {
        GenericDialog gd = new GenericDialog("Set Direction Indicator Angles", getX() + getWidth() / 2 - 250, getY() + getHeight() / 2 - 125);
//        gd.addMessage ("Direction angles are used when WCS is not available for an image.");
        gd.addNumericField("North direction indicator angle: ", ac.NdirAngle, 0, 6, "(degrees CCW from +y-axis)***");
        gd.addNumericField("East direction indicator angle: ", ac.EdirAngle, 0, 6, "(degrees CCW from north direction)***");
        gd.addMessage("***Angles and directions are relative to image orientation 'Invert None'");
        gd.addMessage("      and will follow the image if its orientation is changed.");

        gd.showDialog();

        if (gd.wasCanceled()) return;
        ac.NdirAngle = gd.getNextNumber();
        ac.EdirAngle = gd.getNextNumber();
        ac.updateZoomBoxParameters();
        ac.paint(ac.getGraphics());
        Prefs.set("Astronomy_Tool.NdirAngle", ac.NdirAngle);
        Prefs.set("Astronomy_Tool.EdirAngle", ac.EdirAngle);
    }

    void setAutoScaleParametersDialog() {
        GenericDialog gd = new GenericDialog("Set Autoscale Parameters", getX() + getWidth() / 2 - 337, getY() + getHeight() / 2 - 175);
        gd.addMessage("Auto brightness & contrast displays a range of pixel values based on the image's mean and standard deviation.");
        gd.addMessage("");
        gd.addMessage("Monochrome Images:");
        gd.addNumericField("Low pixel value: mean image value less ", autoScaleFactorLow, 4, 8, "times standard deviation (default = 0.5)");
        gd.addNumericField("High pixel value: mean image value plus ", autoScaleFactorHigh, 4, 8, "times standard deviation (default = 2.0)");
        gd.addMessage("");
        gd.addMessage("RGB Images:");
        gd.addNumericField("Low pixel value: mean image value less ", autoScaleFactorLowRGB, 4, 8, "times standard deviation (default = 2.0)");
        gd.addNumericField("High pixel value: mean image value plus ", autoScaleFactorHighRGB, 4, 8, "times standard deviation (default = 6.0)");

        gd.showDialog();

        if (gd.wasCanceled()) return;
        autoScaleFactorLow = gd.getNextNumber();
        autoScaleFactorHigh = gd.getNextNumber();
        autoScaleFactorLowRGB = gd.getNextNumber();
        autoScaleFactorHighRGB = gd.getNextNumber();

        if (autoContrast) setAutoLevels(null);

        Prefs.set("Astronomy_Tool.autoScaleFactorLow", autoScaleFactorLow);
        Prefs.set("Astronomy_Tool.autoScaleFactorHigh", autoScaleFactorHigh);
        Prefs.set("Astronomy_Tool.autoScaleFactorLowRGB", autoScaleFactorLowRGB);
        Prefs.set("Astronomy_Tool.autoScaleFactorHighRGB", autoScaleFactorHighRGB);
    }

    void grabAutoScaleParameters() {
        getStatistics();
        if (imp.getType() != ImagePlus.COLOR_RGB) {
            if (stats.stdDev == 0) {
                autoScaleFactorLow = (stats.mean - blackValue);
                autoScaleFactorHigh = (whiteValue - stats.mean);
                return;
            }
            autoScaleFactorLow = (stats.mean - blackValue) / stats.stdDev;
            autoScaleFactorHigh = (whiteValue - stats.mean) / stats.stdDev;
            Prefs.set("Astronomy_Tool.autoScaleFactorLow", autoScaleFactorLow);
            Prefs.set("Astronomy_Tool.autoScaleFactorHigh", autoScaleFactorHigh);
        } else {
            autoScaleFactorLowRGB = (stats.mean - blackValue) / stats.stdDev;
            autoScaleFactorHighRGB = (whiteValue - stats.mean) / stats.stdDev;
            Prefs.set("Astronomy_Tool.autoScaleFactorLowRGB", autoScaleFactorLowRGB);
            Prefs.set("Astronomy_Tool.autoScaleFactorHighRGB", autoScaleFactorHighRGB);
        }
        //setAstroProcessor(false);
    }

    void resetAutoScaleParameters() {
        if (imp.getType() != ImagePlus.COLOR_RGB) {
            autoScaleFactorLow = 0.5;
            autoScaleFactorHigh = 2.0;
            Prefs.set("Astronomy_Tool.autoScaleFactorLow", autoScaleFactorLow);
            Prefs.set("Astronomy_Tool.autoScaleFactorHigh", autoScaleFactorHigh);
        } else {
            autoScaleFactorLowRGB = 2.0;
            autoScaleFactorHighRGB = 6.0;
            Prefs.set("Astronomy_Tool.autoScaleFactorLowRGB", autoScaleFactorLowRGB);
            Prefs.set("Astronomy_Tool.autoScaleFactorHighRGB", autoScaleFactorHighRGB);
        }
        setAstroProcessor(false);
    }

    void updateHistogramValues(boolean blackOrWhiteChanged) {
        if (blackOrWhiteChanged) {
            updatePanelValues();
        } else if (autoContrast) {
            setAutoLevels(null);//todo mark
        } else if (fixedContrast || fixedContrastPerSlice) {
            updatePanelValues();
        } else {//full dyn. range
            blackValue = minValue;
            whiteValue = maxValue;
            updatePanelValues();
        }
    }

    void startDataFlipRotate() {
        try {
            rotateTask = new TimerTask() {
                public void run() {
                    if (flipDataX) invertData("x", true);
                    else if (flipDataY) invertData("y", true);
                    else if (rotateDataCCW) rotateData("CCW", true);
                    else if (rotateDataCW) rotateData("CW", true);
                    flipDataX = false;
                    flipDataY = false;
                    rotateDataCCW = false;
                    rotateDataCW = false;

//                    adjustImageRotation(IMAGE_UPDATE);
                    rotateTask = null;
                    rotateTaskTimer = null;
                }
            };
            rotateTaskTimer = new java.util.Timer();
            rotateTaskTimer.schedule(rotateTask, 0);
        } catch (Exception e) {
            IJ.showMessage("Error starting rotation task : " + e.getMessage());
        }
    }


//       void adjustImageRotation(boolean updateImage)
//            {
//            if (fX != prevInvertX)
//                {
//                invert("x", updateImage);
//                prevInvertX = invertX;
//
//                }
//            if (invertY != prevInvertY)
//                {
//                invert("y", updateImage);
//                prevInvertY = invertY;
//
//                }
//            if (rotation != prevRotation)
//                {
//                int del = rotation - prevRotation;
//                if (del == 1) rotate("left", updateImage);
//                else if (del ==-1) rotate("right", updateImage);
//                else if (del == 2 || del == - 2) {invert("x", updateImage); invert("y", updateImage);}
//                else if (del == 3) rotate("right", updateImage);
//                else if (del ==-3) rotate("left", updateImage);
//                prevRotation = rotation;
//
//                }
//            }

    void invertData(String dir, boolean updateImage) {
        stackSize = imp.getStackSize();
        currentSlice = imp.getCurrentSlice();
        ImageProcessor ip = imp.getProcessor();
        if (!dir.equals("x") && !dir.equals("y"))
            return;
        for (int i = 1; i <= stackSize; i++) {
            minMaxChanged = true;
            imp.setSlice(i);
            IJ.showStatus("Invert-" + dir + ": " + i + "/" + stackSize);
            IJ.showProgress((double) i / (double) stackSize);
            if (dir.equals("x")) {
                minMaxChanged = true;
                imp.getProcessor().flipHorizontal();
            } else {
                minMaxChanged = true;
                imp.getProcessor().flipVertical();
            }
            if (updateImage) {
                minMaxChanged = true;
                imp.updateAndDraw();
            }
        }
        if (stackSize > 1) {
            minMaxChanged = true;
            imp.setSlice(currentSlice);
            if (updateImage) {
                minMaxChanged = true;
                imp.updateAndDraw();
            }
        }
    }

    void rotateData(String dir, boolean updateImage) {
        if (!dir.equals("CCW") && !dir.equals("CW"))
            return;
        Calibration cal = imp.getCalibration();
        ImageProcessor ip = imp.getProcessor();
        icWidth = ac.getWidth();
        icHeight = ac.getHeight();
        ipWidth = imp.getWidth();
        ipHeight = imp.getHeight();
        stackSize = imp.getStackSize();
        currentSlice = imp.getCurrentSlice();
        if (ipWidth != ipHeight) {
            StackProcessor sp = new StackProcessor(imp.getStack(), ip);
            ImageStack s2 = null;
            if (dir.equals("CW")) {
                s2 = sp.rotateLeft();
            } else {
                s2 = sp.rotateRight();
            }
            imp.setStack(null, s2);
            if (IJVersion.compareTo("1.42q") > 0 && IJVersion.compareTo("1.44f") < 0)
                imp = WindowManager.getImage(impTitle);
            double pixelWidth = cal.pixelWidth;
            cal.pixelWidth = cal.pixelHeight;
            cal.pixelHeight = pixelWidth;
            minMaxChanged = true;
            imp.setCalibration(cal);
            if (imp.getStackSize() > 1)
                stackRotated = true;
            if (updateImage) {
                dataRotated = true;
//                    layoutContainer(this);
                ac.paint(ac.getGraphics());
            }
        } else {
            for (int i = 1; i <= stackSize; i++) {
                imp.setSlice(i);
                IJ.showStatus("Rotate: " + i + "/" + stackSize);
                IJ.showProgress((double) i / (double) stackSize);
                if (dir.equals("CW")) {
                    ip = imp.getProcessor().rotateLeft();
                } else {
                    ip = imp.getProcessor().rotateRight();
                }
                imp.setProcessor(null, ip);
                if (updateImage) {
                    imp.updateAndDraw();
                }
            }
            double pixelWidth = cal.pixelWidth;
            cal.pixelWidth = cal.pixelHeight;
            cal.pixelHeight = pixelWidth;
            imp.setCalibration(cal);
            imp.setSlice(currentSlice);
        }
        if (stackSize > 1) {
            imp.setSlice(currentSlice);
            if (updateImage) {
                imp.updateAndDraw();
            }
        }
    }

    void fitImageToCanvas() {
        if (updatesEnabled) {
            int canvasWidth = newCanvasWidth();
            int canvasHeight = newCanvasHeight();


            double xmag = (double) canvasWidth / (double) imp.getWidth();
            double ymag = (double) canvasHeight / (double) imp.getHeight();
            //            if (ac.getNetRotate())
            //                {
            //                xmag = (double)newCanvasWidth()/(double)imp.getHeight();
            //                ymag = (double)newCanvasHeight()/(double)imp.getWidth();
            //                }

            ac.setDrawingSize(canvasWidth, canvasHeight);
            if (fillNotFit) {
                fillNotFit = false;
                ac.setMagnification(Math.max(xmag, ymag));
            } else {
                ac.setMagnification(Math.min(xmag, ymag));
            }

            Rectangle r = new Rectangle((int) ((imp.getWidth() / 2.0) - canvasWidth / ac.getMagnification() / 2.0),
                    (int) ((imp.getHeight() / 2.0 - canvasHeight / ac.getMagnification() / 2.0)),
                    (int) ((double) newCanvasWidth() / ac.getMagnification()),
                    (int) ((double) newCanvasHeight() / ac.getMagnification()));
            ac.setSourceRect(r);

            magnification = ac.getMagnification();
            Graphics g = ac.getGraphics();
            if (!fillNotFit) {
                g.setColor(Color.WHITE);
                g.fillRect(0, 0, ac.getWidth(), ac.getHeight());
            }
            ac.paint(g);

            srcRect = ac.getSrcRect();
            savedPanX = srcRect.x;
            savedPanY = srcRect.y;
            savedPanHeight = srcRect.height;
            savedPanWidth = srcRect.width;
            savedMag = ac.getMagnification();
            savedICWidth = ac.getWidth();
            savedICHeight = ac.getHeight();
            Prefs.set("Astronomy_Tool.savedMag", savedMag);
            Prefs.set("Astronomy_Tool.savedICWidth", savedICWidth);
            Prefs.set("Astronomy_Tool.savedICHeight", savedICHeight);
            Prefs.set("Astronomy_Tool.savedPanX", savedPanX);
            Prefs.set("Astronomy_Tool.savedPanY", savedPanY);
            Prefs.set("Astronomy_Tool.savedPanHeight", savedPanHeight);
            Prefs.set("Astronomy_Tool.savedPanWidth", imp.getWidth());
            setImageEdges();
        }
    }

    @Override
    protected Dimension getExtraSize() {
        if (otherPanelsHeight > 0) {
            return new Dimension(extraWidth(), extraHeight());
        } else {
            Insets insets = getInsets();
            int extraWidth = insets.left + insets.right + 10;
            int extraHeight = insets.top + insets.bottom + 10;
            if (extraHeight == 20) extraHeight = 42;
            int members = getComponentCount();
            //if (IJ.debugMode) IJ.log("getExtraSize: "+members+" "+insets);
            for (int i = 1; i < members; i++) {
                Component m = getComponent(i);
                Dimension d = m.getPreferredSize();
                extraHeight += d.height + 5;
                if (IJ.debugMode) IJ.log(i + "  " + d.height + " " + extraHeight);
            }
            return new Dimension(extraWidth, extraHeight);
        }
    }


    int newCanvasWidth() {
        return getWidth() - extraWidth();
    }

    int extraWidth() {
        return getInsets().left + getInsets().right + 10;
    }

    int newCanvasHeight() {
        return getHeight() - extraHeight();
    }

    int extraHeight() {
        return getInsets().top + getInsets().bottom + otherPanelsHeight
                + super.getNScrollbars() * 17 + 10;
    }

//       void clearAndPaint(){
//            ac.resetDoubleBuffer();
//
////            Graphics g = ac.getGraphics();
////            g.setColor(Color.WHITE);
////            g.fillRect(0, 0, ac.getWidth(), ac.getHeight());
////            ac.paint(g);
////            ac.repaint();
////            IJ.log("clearandPaint");
//            Graphics g = ac.getGraphics();
//            if (!IJ.isMacOSX())
//                {
//                int x1 = ac.screenX(0) > 0 ? ac.screenX(0) : 0;    //top left screen x-location
//                int y1 = ac.screenY(0) > 0 ? ac.screenY(0) : 0;    //top left screen y-location
//                int x2 = ac.screenX(imp.getWidth()) < ac.getWidth() ? ac.screenX(imp.getWidth()) : ac.getWidth();    //bottom right screen x-location
//                int y2 = ac.screenY(imp.getHeight()) < ac.getHeight() ? ac.screenY(imp.getHeight()) : ac.getHeight();    //bottom right screen y-location
//
//                g.setColor(Color.WHITE);
//                if (x1 >= 0)
//                    g.fillRect(0, 0, x1, ac.getHeight());
//                if (y1 >= 0)
//                    g.fillRect(x1, 0 , ac.getWidth()-x1, y1);
//                if (x2 <= ac.getWidth())
//                    g.fillRect(x2, y1 , ac.getWidth()-x2, ac.getHeight()-y1);
//                if (y2 <= ac.getHeight())
//                    g.fillRect(x1, y2 , x2-x1, ac.getHeight()-y2);
//               }
//
//            ac.paint(g);
//            }


//       public void refreshAstroWindow()
//                {
//                double newMag, oldMag;
//                imageWindow = imp.getWindow();
//                ac = OverlayCanvas.getOverlayCanvas(imp);
//                oldMag = ac.getMagnification();
//                ImageProcessor ip = imp.getProcessor();
//
//                ipWidth = ip.getWidth();
//                ipHeight = ip.getHeight();
//                icWidth = ac.getWidth();
//                icHeight = ac.getHeight();
//                imageHeight = imageWindow.getHeight();
//                imageWidth = imageWindow.getWidth();
//
//                int height = imageHeight-58-otherPanelsHeight-stackSliders.length*18;
//                int width = imageWidth - 26;
//                if (width < MIN_FRAME_WIDTH) width = MIN_FRAME_WIDTH;
//
//                if (stackRotated && (IJVersion.compareTo("1.44f") >= 0)) {
//                    newMag = Math.max((double)width/(double)ipHeight, (double)height/(double)ipWidth);
//                    stackRotated = false;
//                } else {
//                    newMag = Math.max((double)width/(double)ipWidth, (double)height/(double)ipHeight);
//                }
//                ac.setDrawingSize((int)(ipWidth*newMag), (int)(ipHeight*newMag));
//                ac.setMagnification(newMag);
////                ac.repaint();
////                toolbar.removeMouseListener(toolbarMouseListener);
//                astroWindow = new AstroStackWindow(imp, (OverlayCanvas)ac, REFRESH, NORESIZE);
//                }


    void saveImageDisplay(String format, boolean saveAll) {
        String outBase = "dataset";
//    Graphics g = ac.getGraphics();
//    Image img = ac.graphicsToImage(g);
//    BufferedImage imageDisplay = (BufferedImage)(img);
////    BufferedImage imageDisplay = toBufferedImage(img);

        SaveDialog sf = new SaveDialog(saveAll ? "Save all" : "Save as " + format.toUpperCase(), imp.getShortTitle(), "");
        if (sf.getDirectory() == null || sf.getFileName() == null)
            return;
        String outPath = sf.getDirectory() + sf.getFileName();
        int location = outPath.lastIndexOf('.');
        if (location >= 0) outPath = outPath.substring(0, location);
        outBase = outPath;
        if (outBase.endsWith(imageSuffix)) {
            location = outBase.lastIndexOf(imageSuffix);
            if (location >= 0) outBase = outBase.substring(0, location);
        } else if (outBase.endsWith(plotSuffix)) {
            location = outBase.lastIndexOf(plotSuffix);
            if (location >= 0) outBase = outBase.substring(0, location);
        } else if (outBase.endsWith(configSuffix)) {
            location = outBase.lastIndexOf(configSuffix);
            if (location >= 0) outBase = outBase.substring(0, location);
        } else if (outBase.endsWith(dataSuffix)) {
            location = outBase.lastIndexOf(dataSuffix);
            if (location >= 0) outBase = outBase.substring(0, location);
        } else if (outBase.endsWith(aperSuffix)) {
            location = outBase.lastIndexOf(aperSuffix);
            if (location >= 0) outBase = outBase.substring(0, location);
        } else if (outBase.endsWith(logSuffix)) {
            location = outBase.lastIndexOf(logSuffix);
            if (location >= 0) outBase = outBase.substring(0, location);
        }

        if (!saveAll || (saveAll && saveImage)) {
            String imagePath = (saveAll ? outBase + imageSuffix : outPath) + "." + format;
            File saveFile = new File(imagePath);
            BufferedImage imageDisplay = new BufferedImage(ac.getSize().width, ac.getSize().height, BufferedImage.TYPE_INT_RGB);
            Graphics gg = imageDisplay.createGraphics();
            ac.paint(gg);
            gg.dispose();
            if ("pdf".equals(format)) {
                new PdfRasterWriter().writeImage(imageDisplay, saveFile.getAbsolutePath());
                return;
            }
            IJU.saveAsPngOrJpg(imageDisplay, saveFile, format);
        }

        if (saveAll && (savePlot || saveConfig || saveTable)) {
            MultiPlot_.saveDataImageConfig(savePlot, false, saveConfig, saveTable, true, format, outBase + plotSuffix + "." + format, outBase + configSuffix + ".plotcfg",
                    outBase + dataSuffix + Prefs.get("options.ext", ".xls"));
        }
        if (saveAll && saveApertures) {
            IJU.saveApertures(outBase + aperSuffix + ".apertures");
        }
        if (saveAll && saveLog) {
            saveLogToFile(outBase + logSuffix + ".log");
        }
    }

    public void saveLogToFile(String path) {
        String log = IJ.getLog();
        if (log != null) {
            String[] loglines = log.split("\\r?\\n|\\r");
            PrintWriter pw = null;
            try {
                FileOutputStream fos = new FileOutputStream(path);
                BufferedOutputStream bos = new BufferedOutputStream(fos);
                pw = new PrintWriter(bos);
            } catch (IOException e) {
                IJ.beep();
                IJ.showMessage("Error writing log file!");
            }
            for (int i = 0; i < loglines.length; i++) {
                pw.println(loglines[i]);
            }
            pw.close();
        }
    }

    public void openApertures(String apsPath) {
        if (apsPath != null && !apsPath.trim().isEmpty()) {
            try {
                var s = Files.readString(Path.of(apsPath));
                var d = AperturesFile.read(s);
                if (d != null) {
                    FreeformPixelApertureHandler.APS.set(d.apertureRois());
                    FreeformPixelApertureHandler.IMPORTED_APS.set(d.apertureRois());
                    Prefs.ijPrefs.putAll(d.prefs());
                    ac.removeApertureRois();
                    for (int i = 0; i < FreeformPixelApertureHandler.APS.get().size(); i++) {
                        var ap = FreeformPixelApertureHandler.APS.get().get(i);
                        ap.setName((ap.isComparisonStar() ? "C" : "T") + (i+1));
                        ap.setImage(imp);
                        ac.add(ap);
                    }
                    return;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            try (var io = new FileInputStream(apsPath)) {
                openApertures(io);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void openApertures(InputStream stream) {
        try {
            var isCustomAp = false;
            if (stream != null) {
                Prefs.set("multiaperture.xapertures", "");
                Prefs.set("multiaperture.yapertures", "");
                Prefs.set("multiaperture.raapertures", "");
                Prefs.set("multiaperture.decapertures", "");
                Prefs.set("multiaperture.isrefstar", "");
                Prefs.set("multiaperture.isalignstar", "");
                Prefs.set("multiaperture.centroidstar", "");
                Prefs.set("multiaperture.absmagapertures", "");
                Prefs.set("multiaperture.import.xapertures", "");
                Prefs.set("multiaperture.import.yapertures", "");
                Prefs.set("multiaperture.import.raapertures", "");
                Prefs.set("multiaperture.import.decapertures", "");
                Prefs.set("multiaperture.import.isrefstar", "");
                Prefs.set("multiaperture.import.isalignstar", "");
                Prefs.set("multiaperture.import.centroidstar", "");
                FreeformPixelApertureHandler.IMPORTED_APS.set(new ArrayList<>());
                InputStream is = new BufferedInputStream(stream);
                var p = new Properties();
                p.load(is);
                isCustomAp = p.containsKey(Prefs.KEY_PREFIX+ FreeformPixelApertureHandler.APS.getPropertyKey());
                if (isCustomAp) {
                    p.put(Prefs.KEY_PREFIX+ FreeformPixelApertureHandler.IMPORTED_APS.getPropertyKey(),
                            p.get(Prefs.KEY_PREFIX+ FreeformPixelApertureHandler.APS.getPropertyKey()));
                }
                Prefs.ijPrefs.putAll(p);
                Property.resetLoadStatus();
                is.close();
            }
            ac.removeApertureRois();

            if (isCustomAp) {
                for (int i = 0; i < FreeformPixelApertureHandler.APS.get().size(); i++) {
                    var ap = FreeformPixelApertureHandler.APS.get().get(i);
                    ap.setName((ap.isComparisonStar() ? "C" : "T") + (i+1));
                    ap.setImage(imp);
                    ac.add(ap);
                }
                return;
            }

            radius = Prefs.get("aperture.radius", radius);
            rBack1 = Prefs.get("aperture.rback1", rBack1);
            rBack2 = Prefs.get("aperture.rback2", rBack2);
            removeBackStars = Prefs.get("aperture.removebackstars", removeBackStars);
            String xapertures = Prefs.get("multiaperture.xapertures", "");
            String yapertures = Prefs.get("multiaperture.yapertures", "");
            String raapertures = Prefs.get("multiaperture.raapertures", "");
            String decapertures = Prefs.get("multiaperture.decapertures", "");
            String isRefStarString = Prefs.get("multiaperture.isrefstar", "");
            String isAlignStarString = Prefs.get("multiaperture.isalignstar", "");
            String centroidStarString = Prefs.get("multiaperture.centroidstar", "");
            String absMagApertueresString = Prefs.get("multiaperture.absmagapertures", "");
            Prefs.set("multiaperture.import.xapertures", xapertures);
            Prefs.set("multiaperture.import.yapertures", yapertures);
            Prefs.set("multiaperture.import.raapertures", raapertures);
            Prefs.set("multiaperture.import.decapertures", decapertures);
            Prefs.set("multiaperture.import.isrefstar", isRefStarString);
            Prefs.set("multiaperture.import.centroidstar", centroidStarString);
            Prefs.set("multiaperture.import.isalignstar", isAlignStarString);
            Prefs.set("multiaperture.import.absmagapertures", absMagApertueresString);
            String[] xaps = xapertures.split(",");
            String[] yaps = yapertures.split(",");
            String[] raaps = raapertures.split(",");
            String[] decaps = decapertures.split(",");
            String[] isRefStar = isRefStarString.split(",");
            String[] isAlignStar = isAlignStarString.split(",");
            String[] centroidStar = centroidStarString.split(",");
            String[] absMagApertures = absMagApertueresString.split(",");
            boolean[] isRef;
            boolean[] isAlign;
            boolean[] isCentroid;
            if ((xaps.length == 1 && (xaps[0].isEmpty() || xaps[0].equals("FITS"))) && (yaps.length == 1 && (yaps[0].isEmpty() || yaps[0].equals("FITS")))) {
                IJ.beep();
                IJ.showMessage("No apertures stored for display.");
                return;
            }
            if (xaps.length != yaps.length) {
                IJ.beep();
                IJ.showMessage("Error: The number of stored X and Y aperture coordinates is different. Aborting.");
                return;
            }
            double[] xap = extract(true, xaps);
            double[] yap = extract(false, yaps);

            double[] absMagStored = extractAbsMagDoubles(absMagApertures);
            if (absMagStored == null || absMagStored.length != xaps.length) {
                absMagStored = new double[xaps.length];
                for (int ap = 0; ap < xaps.length; ap++) {
                    absMagStored[ap] = 99.999;
                }
            }

            boolean usewcs = Prefs.get("multiaperture.usewcs", false);
            if (usewcs && wcs != null && wcs.hasWCS() && raaps.length == xaps.length && decaps.length == xaps.length) {
                double[] raap = extractDoubles(raaps);
                double[] decap = extractDoubles(decaps);
                for (int i = 0; i < xaps.length; i++) {
                    double[] xy = wcs.wcs2pixels(new double[]{raap[i], decap[i]});
                    xap[i] = xy[0];
                    yap[i] = xy[1];
                }
            }


            if (xaps.length != isRefStar.length) {
                isRef = new boolean[xaps.length];
                for (int ap = 0; ap < xaps.length; ap++) {
                    if (ap == 0)
                        isRef[ap] = false;
                    else
                        isRef[ap] = true;
                }
            } else {
                isRef = extractBoolean(isRefStar);
            }


            if (xaps.length != isAlignStar.length) {
                isAlign = new boolean[xaps.length];
                for (int ap = 0; ap < xaps.length; ap++) {
                    isAlign[ap] = true;
                }
            } else {
                isAlign = extractBoolean(isAlignStar);
            }


            if (xaps.length != centroidStar.length) {
                isCentroid = new boolean[xaps.length];
                for (int ap = 0; ap < xaps.length; ap++) {
                    isCentroid[ap] = true;
                }
            } else {
                isCentroid = extractBoolean(centroidStar);
            }

            Prefs.set("multiaperture.previous", true);
            MultiAperture_.apLoading.set(MultiAperture_.ApLoading.IMPORTED);
            GFormat g = new GFormat("2.1");
            photom.setSourceApertureRadius(radius);
            photom.setBackgroundApertureRadii(rBack1, rBack2);
            photom.setRemoveBackStars(removeBackStars);

            Photometer phot = new Photometer(imp.getCalibration());
            phot.setRemoveBackStars(removeBackStars);
            phot.setMarkRemovedPixels(false);
            for (int i = 0; i < xaps.length; i++) {
                phot.measure(imp, exact, xap[i], yap[i], radius, rBack1, rBack2);
                ApertureRoi roi = new ApertureRoi(xap[i], yap[i], radius, rBack1, rBack2, phot.source, isCentroid[i]);
                roi.setAppearance(true, isCentroid[i], showSkyOverlay, nameOverlay, valueOverlay, !isRef[i] ? new Color(196, 222, 155) : Color.PINK, (!isRef[i] ? "T" : "C") + (i + 1), phot.source);
                roi.setAMag(absMagStored[i]);
                roi.setImage(imp);
                roi.setPhantom(true);
                ac.add(roi);
                ac.paint(ac.getGraphics());
            }
        } catch (Exception e) {
            IJ.beep();
            IJ.showMessage("Error reading apertures file");
        }
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
        if (isFITS) {
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
            if (s[i].equalsIgnoreCase("true"))
                arr[i] = true;
            else
                arr[i] = false;
        }

        return arr;
    }

    void openDragAndDropFiles(java.io.File[] files) {
        if (files.length > 0 && files[0].isFile()) {
            if (files[0].getName().endsWith(".apertures")) {
                try {
                    openApertures(files[0].getCanonicalPath());
                } catch (Exception e) {
                    IJ.beep();
                    IJ.showMessage("Error reading aperture file with drag and drop operation");
                }
            } else if (files[0].getName().endsWith(".radec")) {
                try {
                    IJU.openRaDecApertures(files[0].getCanonicalPath());
                } catch (Exception e) {
                    IJ.beep();
                    IJ.showMessage("Error reading RA/Dec file with drag and drop operation");
                }
            } else if (FitsExtensionUtil.isFitsFile(files[0].getName()) ||
                    files[0].getName().toLowerCase().endsWith(".tif") ||
                    files[0].getName().toLowerCase().endsWith(".tiff") ||
                    files[0].getName().toLowerCase().endsWith(".jpeg") ||
                    files[0].getName().toLowerCase().endsWith(".jpg") ||
                    files[0].getName().toLowerCase().endsWith(".png") ||
                    files[0].getName().toLowerCase().endsWith(".bmp") ||
                    files[0].getName().toLowerCase().endsWith(".bmp") ||
                    files[0].getName().toLowerCase().endsWith(".pgm")) {
                ImagePlus imp2 = null;
                try {
                    imp2 = IJ.openImage(files[0].getCanonicalPath());
                } catch (Exception e) {
                    IJ.beep();
                    IJ.showMessage("Error reading image file");
                    return;
                }
                if (imp2 != null && ((imp.getStackSize() == 1 && imp2.getStackSize() == 1) ||
                        (imp.getStackSize() != 1 && imp2.getStackSize() != 1))) {
                    StackProcessor sp = new StackProcessor(imp.getStack(), imp2.getProcessor());
                    ImageStack s2 = imp2.getImageStack();
                    imp.setStack(s2);
                    imp.setFileInfo(imp2.getFileInfo());
                    copyImageProperties(imp2);
                    imp.setProcessor(imp2.getTitle(), imp2.getProcessor());
                    setAstroProcessor(false);
                }
            }
        }
    }


    @Override
    // Add extraInfo to subtitle
    public synchronized String createSubtitle() {
        return super.createSubtitle() + extraInfo;
    }

    // This method returns a buffered image with the contents of an image
    public static BufferedImage toBufferedImage(Image image) {
        if (image instanceof BufferedImage) {
            return (BufferedImage) image;
        }

        // This code ensures that all the pixels in the image are loaded
        image = new ImageIcon(image).getImage();

        // Determine if the image has transparent pixels; for this method's
        // implementation, see Determining If an Image Has Transparent Pixels
        boolean hasAlpha = hasAlpha(image);

        // Create a buffered image with a format that's compatible with the screen
        BufferedImage bimage = null;
        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        try {
            // Determine the type of transparency of the new buffered image
            int transparency = Transparency.OPAQUE;
            if (hasAlpha) {
                transparency = Transparency.BITMASK;
            }

            // Create the buffered image
            GraphicsDevice gs = ge.getDefaultScreenDevice();
            GraphicsConfiguration gc = gs.getDefaultConfiguration();
            bimage = gc.createCompatibleImage(
                    image.getWidth(null), image.getHeight(null), transparency);
        } catch (HeadlessException e) {
            // The system does not have a screen
        }

        if (bimage == null) {
            // Create a buffered image using the default color model
            int type = BufferedImage.TYPE_INT_RGB;
            if (hasAlpha) {
                type = BufferedImage.TYPE_INT_ARGB;
            }
            bimage = new BufferedImage(image.getWidth(null), image.getHeight(null), type);
        }

        // Copy image to buffered image
        Graphics g = bimage.createGraphics();

        // Paint the image onto the buffered image
        g.drawImage(image, 0, 0, null);
        g.dispose();

        return bimage;
    }

    // This method returns true if the specified image has transparent pixels
    public static boolean hasAlpha(Image image) {
        // If buffered image, the color model is readily available
        if (image instanceof BufferedImage) {
            BufferedImage bimage = (BufferedImage) image;
            return bimage.getColorModel().hasAlpha();
        }

        // Use a pixel grabber to retrieve the image's color model;
        // grabbing a single pixel is usually sufficient
        PixelGrabber pg = new PixelGrabber(image, 0, 0, 1, 1, false);
        try {
            pg.grabPixels();
        } catch (InterruptedException e) {
        }

        // Get the image's color model
        ColorModel cm = pg.getColorModel();
        return cm.hasAlpha();
    }


    void setupListeners() {

//                imageWindow.removeComponentListener(this);
//                imageWindow.addComponentListener(this);
//                WindowListener[] wl = originalWindow.getWindowListeners();
//                if (wl.length>0)
//                    for (int i=0; i<wl.length; i++)
//                            imageWindow.addWindowListener(wl[i]);

        mwl = this.getMouseWheelListeners();
        if (mwl.length > 0)
            for (int i = 0; i < mwl.length; i++)
                this.removeMouseWheelListener(mwl[i]);

        icmwl = ac.getMouseWheelListeners();
        if (icmwl.length > 0)
            for (int i = 0; i < icmwl.length; i++)
                ac.removeMouseWheelListener(icmwl[i]);


//                mml = ac.getMouseMotionListeners();
//                if (mml.length>0)
//                        for (int i=0; i<mml.length; i++)
//                                ac.removeMouseMotionListener(mml[i]);

        ml = ac.getMouseListeners();
        if (ml.length > 0)
            for (int i = 0; i < ml.length; i++)
                ac.removeMouseListener(ml[i]);

        ac.removeMouseMotionListener(this);
        ac.addMouseMotionListener(this);
        ac.removeMouseListener(this);
        ac.addMouseListener(this);
        ac.removeMouseWheelListener(this);
        ac.addMouseWheelListener(this);
        ac.addKeyListener(this);


//        FocusListener[] fl = super.getFocusListeners();
//        IJ.log("# of focus listeners = "+fl.length);
//        if (fl.length>0)
//                for (int i=0; i<fl.length; i++)
//                    super.removeFocusListener(fl[i]);
//
//
//        addFocusListener(new FocusListener(){
//            public void focusGained(FocusEvent e) {
//                IJ.log("focusGained: "+imp.getTitle());
//                WindowManager.setWindow(asw);
//            }
//            public void focusLost(FocusEvent e) {}});
//

//        this.addFocusListener(this);

        toolbar = Toolbar.getInstance();
        astronomyToolId = toolbar.getToolId("Astronomy_Tool");
        if (astronomyToolId != -1)
            toolbar.setTool(astronomyToolId);
        else
            astronomyToolId = -9999;
        apertureToolId = toolbar.getToolId("Aperture");
        if (apertureToolId == -1)
            apertureToolId = -9999;
        zoomToolId = Toolbar.MAGNIFIER;
        panToolId = Toolbar.HAND;
        toolbar.removeMouseListener(toolbarMouseListener);
        toolbar.addMouseListener(toolbarMouseListener);
    }


    void exitAstronomyTool() {

//        ac.removeMouseMotionListener(this);
//                ac.removeMouseWheelListener(this);

        ac.removeMouseListener(this);


//                if (mwl.length>0)
//                        for (int i=0; i<mwl.length; i++)
//                                imageWindow.addMouseWheelListener(mwl[i]);

//        if (mml.length>0)
//                for (int i=0; i<mml.length; i++)
//                        ac.removeMouseMotionListener(mml[i]);
//        if (mml.length>0)
//                for (int i=0; i<mml.length; i++)
//                        ac.addMouseMotionListener(mml[i]);

        if (ml.length > 0)
            for (int i = 0; i < ml.length; i++)
                ac.removeMouseListener(ml[i]);
        if (ml.length > 0)
            for (int i = 0; i < ml.length; i++)
                ac.addMouseListener(ml[i]);
        frameLocationX = this.getLocation().x;
        frameLocationY = this.getLocation().y;
//        apertureOverlay.clear();
        ac.setAstronomyMode(false);
        ac.paint(ac.getGraphics());
        savePrefs();
        astronomyMode = false;
        imp.unlock();
    }

    ;

    void reenterAstronomyTool() {

//        ac.addMouseMotionListener(this);
        ac.removeMouseListener(this);
        ac.addMouseListener(this);

//        if (mml.length>0)
//                for (int i=0; i<mml.length; i++)
//                        ac.removeMouseMotionListener(mml[i]);
        if (ml.length > 0)
            for (int i = 0; i < ml.length; i++)
                ac.removeMouseListener(ml[i]);
        astronomyMode = true;
        radius = Prefs.get("aperture.radius", radius);
        rBack1 = Prefs.get("aperture.rback1", rBack1);
        rBack2 = Prefs.get("aperture.rback2", rBack2);
        exact = Prefs.get("aperture.exact", exact);
        reposition = Prefs.get("aperture.reposition", reposition);
        backPlane = Prefs.get("aperture.backplane", backPlane);
        buttonCentroid.setSelected(reposition);
        removeBackStars = Prefs.get("aperture.removebackstars", removeBackStars);
        removeBackStarsCB.setState(removeBackStars);
        photom.setSourceApertureRadius(radius);
        photom.setBackgroundApertureRadii(rBack1, rBack2);
        photom.setRemoveBackStars(removeBackStars);
        photom.setUsePlane(backPlane);
        ac.setAperture(radius, rBack1, rBack2, showSkyOverlay, showPhotometer);
//        apertureOverlay.clear();
        ac.setMouseInImage(false);
        ac.setAstronomyMode(true);
        ac.paint(ac.getGraphics());
    }

//	public void focusGained(FocusEvent e) {
//		WindowManager.setWindow(this);
//
//        if(IJ.isMacOSX())
//            {
//            IJ.wait(1);
//            setMenuBar(mainMenuBar);
//            }
//        IJ.log("focus gained");
//
//	}
//
//	public void focusLost(FocusEvent e) {}


    MouseListener toolbarMouseListener = new MouseListener() {
        public void mousePressed(MouseEvent e) {
            IJ.wait(250);
            currentToolId = toolbar.getToolId();
            if (currentToolId == astronomyToolId ||
                    currentToolId == zoomToolId ||
                    currentToolId == apertureToolId ||
                    currentToolId == panToolId) {
                reenterAstronomyTool();
            } else {
                exitAstronomyTool();
            }
        }

        public void mouseReleased(MouseEvent e) {
        }

        public void mouseEntered(MouseEvent e) {
        }

        public void mouseExited(MouseEvent e) {
        }

        public void mouseClicked(MouseEvent e) {
        }
    };


    public void setAutoLevels(String windowName) {
        if (imp.getType() == ImagePlus.COLOR_RGB) {
            imp.getProcessor().reset();
        }
        setAstroProcessor(false);
//            getStatistics();
//
//            if (imp.getType()==ImagePlus.COLOR_RGB)
//                {
//                min = Math.max(stats.mean - autoScaleFactorLowRGB*stats.stdDev, stats.min);
//                max = Math.min(stats.mean + autoScaleFactorHighRGB*stats.stdDev, stats.max);
//                }
//            else
//                {
//                min = Math.max(stats.mean - autoScaleFactorLow*stats.stdDev, stats.min);
//                max = Math.min(stats.mean + autoScaleFactorHigh*stats.stdDev, stats.max);
//                }
//
//            updatePanelValues();
//            setImageEdges();
//            savedMin = min;
//            savedMax = max;
//            Prefs.set("Astronomy_Tool.savedMin", savedMin);
//            Prefs.set("Astronomy_Tool.savedMax", savedMax);
//
//            radius = Prefs.get("aperture.radius", radius);
//            rBack1 = Prefs.get("aperture.rback1", rBack1);
//            rBack2 = Prefs.get("aperture.rback2", rBack2);
//            photom = new Photometer (imp.getCalibration());
//            photom.setSourceApertureRadius (radius);
//            photom.setBackgroundApertureRadii (rBack1,rBack2);
//            photom.setRemoveBackStars(removeBackStars);
//            ac.setAperture(radius,rBack1,rBack2,showSkyOverlay,showPhotometer);
//            double value = imp.getProcessor().getPixelValue((int)lastImageX, (int)lastImageY);
//            valueTextField.setText(fourPlaces.format(value));
//            photom.setMarkRemovedPixels(false);
//            photom.measure (imp,lastImageX,lastImageY);
//            if (showMeanNotPeak)
//                {
//                peakLabel.setText("Mean:");
//                peakTextField.setText(fourPlaces.format(photom.meanBrightness()));
//                }
//            else
//                {
//                peakLabel.setText("Peak:");
//                peakTextField.setText(fourPlaces.format(photom.peakBrightness()));
//                }
//            lengthLabel.setText("Int Cnts:");
//            lengthTextField.setText(fourPlaces.format(photom.sourceBrightness()));
    }

    public ImageStatistics getLiveStatistics() {
        Roi roi = imp.getRoi();
        imp.killRoi();
        if (imp.getType() == ImagePlus.COLOR_RGB) {
            ImageProcessor ip = imp.getProcessor();
            ip.reset();
        }
        ImageStatistics liveStats = imp.getStatistics(ImageStatistics.MEAN + ImageStatistics.MIN_MAX + ImageStatistics.STD_DEV, BISLIDER_SEGMENTS);
        imp.setRoi(roi);
        return liveStats;
    }

    public void getStatistics() {
        Roi roi = imp.getRoi();
        imp.killRoi();
        if (imp.getType() == ImagePlus.COLOR_RGB) {
            ImageProcessor ip = imp.getProcessor();
            ip.reset();
        }
        stats = imp.getStatistics(ImageStatistics.MEAN + ImageStatistics.MIN_MAX + ImageStatistics.STD_DEV, BISLIDER_SEGMENTS);
        imp.setRoi(roi);
    }

    protected synchronized void getBiSliderStatistics() {
        if (closed) {
            return;
        }
        Roi roi = imp.getRoi();
        imp.killRoi();
        if (imp.getType() == ImagePlus.COLOR_RGB) {
            ImageProcessor ip = imp.getProcessor();
            ip.reset();
        }
        stats = imp.getStatistics((ImageStatistics.MEAN + ImageStatistics.MIN_MAX +
                        ImageStatistics.STD_DEV), BISLIDER_SEGMENTS,
                minValue, maxValue);
        imp.setRoi(roi);
    }

    public ColorProcessor getcp() {
        return cp;
    }

    /**
     * Updates the image pixel scale calibration sliders and display if auto update is enabled.
     * (Histogram sliders at bottom of stack window).
     */
    public synchronized void updateCalibration() {
        getStatistics();

        if (imp.getType() == ImagePlus.COLOR_256 || imp.getType() == ImagePlus.COLOR_RGB || imp.getType() == ImagePlus.GRAY8) {
            useFixedMinMaxValues = false;
            useFixedMinMaxValuesCB.setState(false);
            minValue = cal.getCValue(0);
            maxValue = cal.getCValue(255);
            if (blackValue < minValue) blackValue = minValue;
            if (whiteValue > maxValue) whiteValue = maxValue;
        } else {
            maxValue = useFixedMinMaxValues ? fixedMaxValue : (fixedContrast ? Math.max(whiteValue, stats.max) : stats.max);
            minValue = useFixedMinMaxValues ? fixedMinValue : (fixedContrast ? Math.min(blackValue, stats.min) : stats.min);
            if (imp.getType() == ImagePlus.GRAY16 && maxValue - minValue < 256)
                maxValue = minValue + 255;
        }

        if (autoContrast || autoScaleIconClicked) {
            if (imp.getType() == ImagePlus.COLOR_RGB) {
                blackValue = Math.max(stats.mean - autoScaleFactorLowRGB * stats.stdDev, minValue);
                whiteValue = Math.min(stats.mean + autoScaleFactorHighRGB * stats.stdDev, maxValue);
            } else {
                var sd = stats.stdDev == 0 ? 1 : stats.stdDev;
                blackValue = Math.max(stats.mean - autoScaleFactorLow * sd, minValue);
                whiteValue = Math.min(stats.mean + autoScaleFactorHigh * sd, maxValue);
            }
        } else if (!fixedContrast && !fixedContrastPerSlice) {
            blackValue = minValue;
            whiteValue = maxValue;
        }
    }

    public synchronized void updateWCS() {
        wcs = new WCS(imp);
        goodWCS = wcs.hasWCS();
        RATextField.setEditable(goodWCS);
        DecTextField.setEditable(goodWCS);
        wcs.setUseSIPAlways(useSIPAllProjections);
        extraInfo = " (" + wcs.coordsys + ")";
        ac.setWCS(wcs);
        if (autoSaveWCStoPrefs) updatePrefsFromWCS(false);
        ac.setShowPixelScale(showScaleX, showScaleY, pixelScaleX, pixelScaleY);
        saveWCStoPrefsMenuItem.setEnabled(wcs != null && (wcs.hasPA || wcs.hasScale));
        if (autoNupEleft) {
            setBestOrientation();
        } else {
            ac.paint(ac.getGraphics());
        }
    }

    public void setAstroProcessor(boolean requestUpdateAnnotationsFromHeader) {
        setAstroProcessor(requestUpdateAnnotationsFromHeader, true);
    }

    public void setAstroProcessor(boolean requestUpdateAnnotationsFromHeader, boolean updateImage) {
        ImageProcessor ip = imp.getProcessor();
        slice = imp.getCurrentSlice();
        cal = imp.getCalibration();
        if (imp.getType() == ImagePlus.COLOR_RGB) {
            ip.reset();
            ip.snapshot();
            cp = (ColorProcessor) (ip.duplicate());
        }
        impTitle = imp.getTitle();
        stackSize = imp.getStackSize();

        updateWCS();
        updateCalibration();

        if (autoDisplayAnnotationsFromHeader && (requestUpdateAnnotationsFromHeader || oldSlice != slice)) {
            displayAnnotationsFromHeader(true, false, false);
        }

        if (oldSlice != slice) {
            stackListeners.forEach(l -> l.accept(null));
        }

        oldSlice = slice;

        double[] oldSliceMin = sliceMin;
        double[] oldSliceMax = sliceMax;
        sliceMin = new double[stackSize];
        sliceMax = new double[stackSize];
        for (int i = 0; i < stackSize; i++) {
            sliceMin[i] = i < oldSliceMin.length ? oldSliceMin[i] : blackValue;
            sliceMax[i] = i < oldSliceMax.length ? oldSliceMax[i] : whiteValue;
        }

        //infoTextField.setText(""+super.createSubtitle());

        if (useInvertingLut != ip.isInvertedLut() && !ip.isColorLut())
            ip.invertLut();
        SwingUtilities.invokeLater(() -> {
            layoutContainer(this);
        });
        ac.updateZoomBoxParameters();
        SwingUtilities.invokeLater(() -> updatePanelValues(updateImage));
        setImageEdges();
        radius = Prefs.get("aperture.radius", radius);
        rBack1 = Prefs.get("aperture.rback1", rBack1);
        rBack2 = Prefs.get("aperture.rback2", rBack2);
        photom = new Photometer(cal);
        //photom1 = new Photometer (cal);
        //photom2 = new Photometer (cal);
        photom.setRemoveBackStars(removeBackStars);
        ac.setAperture(radius, rBack1, rBack2, showSkyOverlay, showPhotometer);
//            setValueTextField();
        photom.setMarkRemovedPixels(false);
        photom.measure(imp, exact, lastImageX, lastImageY, radius, rBack1, rBack2);
        if (showMeanNotPeak) {
            peakLabel.setText("Mean:");
            writeNumericPanelField(photom.meanBrightness(), peakTextField);
        } else {
            peakLabel.setText("Peak:");
            writeNumericPanelField(photom.peakBrightness(), peakTextField);
        }
        lengthLabel.setText("Int Cnts:");
        writeNumericPanelField(photom.sourceBrightness(), lengthTextField);
    }


    public void setUpdatesEnabled(boolean enabled) {
        updatesEnabled = enabled;
    }

    public boolean getUpdatesEnabled() {
        return updatesEnabled;
    }

    void updatePanelValues() {
        updatePanelValues(true);
    }

    public void updatePanelValues(boolean updateImage) {
        if (updatesEnabled && !closed && imp != null) {
            //ImageProcessor ip = imp.getProcessor();
            getBiSliderStatistics();

            if (fixedContrastPerSlice) {
                if (!autoScaleIconClicked) {
                    blackValue = sliceMin[imp.getCurrentSlice() - 1];
                    whiteValue = sliceMax[imp.getCurrentSlice() - 1];
                } else {
                    sliceMin[imp.getCurrentSlice() - 1] = blackValue;
                    sliceMax[imp.getCurrentSlice() - 1] = whiteValue;
                }
            }
            autoScaleIconClicked = false;

            if (fixedContrast || fixedContrastPerSlice) {
                if (whiteValue < blackValue) whiteValue = blackValue;
                if (blackValue < minValue) minValue = blackValue;
                if (whiteValue > maxValue) maxValue = whiteValue;
            } else {//autoContrast=true
                if (blackValue < minValue) blackValue = minValue;
                if (blackValue > maxValue) blackValue = maxValue;
                if (whiteValue > maxValue) whiteValue = maxValue;
                if (whiteValue < blackValue) whiteValue = blackValue;
            }

            histogram = stats.histogram;

            for (int i = 0; i < histogram.length; i++) {
                if (histogram[i] <= 1)
                    logHistogram[i] = 0;
                else
                    logHistogram[i] = Math.log(histogram[i]);
                if (logHistogram[i] > histMax)
                    histMax = logHistogram[i];
            }

            minMaxBiSlider.setParameters(BiSlider.RGB, false,
                    ((maxValue - minValue) / (double) BISLIDER_SEGMENTS),
                    Color.BLACK, Color.BLACK, minValue, maxValue, blackValue, whiteValue);
            writeNumericPanelField(minValue, minValueTextField);
            writeNumericPanelField(maxValue, maxValueTextField);

            writeNumericPanelField(blackValue, blackTextfield);
            writeNumericPanelField(whiteValue, whiteTextfield);

            writeNumericPanelField(stats.mean, meanTextField);
            setValueTextField();
            if (goodWCS) {
                radec = wcs.pixels2wcs(xy);
                if (useSexagesimal) {
                    RATextField.setText(IJU.decToSexRA(radec[0]));
                    DecTextField.setText(IJU.decToSexDec(radec[1]));
                } else {
                    RATextField.setText(sixPlaces.format(radec[0] / 15));
                    DecTextField.setText(sixPlaces.format(radec[1]));
                }
            } else {
                RATextField.setText("");
                DecTextField.setText("");
            }
            imp.setDisplayRange(cal.getRawValue(blackValue), cal.getRawValue(whiteValue));
            //minMaxChanged = true;
            if (updateImage) {
                imp.updateAndDraw(!hasNotified);
                hasNotified = true; // Fixes flash
            }
            if (imp.getWindow() != null) {
                //imp.getWindow().repaint();
            }
        }
    }


    void setValueTextField() {
        if (imp.getType() == ImagePlus.COLOR_RGB && cp != null) {
            float value = cp.getPixelValue((int) lastImageX, (int) lastImageY);
            int[] RGB = imp.getPixel((int) lastImageX, (int) lastImageY);
            if (RGB.length >= 3) {
                valueTextField.setText(IJ.pad((int) value, 3) + " (" + IJ.pad(RGB[0], 3) + "," + IJ.pad(RGB[1], 3) + "," + IJ.pad(RGB[2], 3) + ")");
            } else {
                valueTextField.setText("" + value);
            }
        } else {
            writeNumericPanelField(imp.getProcessor().getPixelValue((int) lastImageX, (int) lastImageY), valueTextField);
        }
    }


    void writeNumericPanelField(float value, JTextField textField) {
        if (Float.isNaN(value)) {
            textField.setText("NaN");
        } else {
            if (value >= 1e9 || value <= -1e9 || (value > -1e-5 && value < 0) || (value > 0 && value < 1e-5)) {
                textField.setText(scientificSixPlaces.format(value));
            } else {
                textField.setText(fourPlaces.format(value));
            }
        }

    }

    void writeNumericPanelField(double value, JTextField textField) {
        if (Double.isNaN(value)) {
            textField.setText("NaN");
        } else {
            if (value >= 1e9 || value <= -1e9 || (value > -1e-5 && value < 0) || (value > 0 && value < 1e-5)) {
                textField.setText(scientificSixPlaces.format(value));
            } else {
                textField.setText(fourPlaces.format(value));
            }
        }

    }


    @Override
    public Rectangle getMaximumBounds() {
        double width = imp.getWidth();
        double height = imp.getHeight();
        double iAspectRatio = width / height;
        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        Rectangle maxWindow = ge.getMaximumWindowBounds();
//		maxWindowBounds = maxWindow;
//		if (iAspectRatio/((double)maxWindow.width/maxWindow.height)>0.75) {
//			maxWindow.y += 22;  // uncover ImageJ menu bar
//			maxWindow.height -= 22;
//		}
//		Dimension extraSize = getExtraSize();
//		double maxWidth = maxWindow.width-extraSize.width;
//		double maxHeight = maxWindow.height-extraSize.height;
//		double mAspectRatio = maxWidth/maxHeight;
//		int wWidth, wHeight;
//		double mag;
//		if (iAspectRatio>=mAspectRatio) {
//			mag = maxWidth/width;
//			wWidth = maxWindow.width;
//			wHeight = (int)(height*mag+extraSize.height);
//		} else {
//			mag = maxHeight/height;
//			wHeight = maxWindow.height;
//			wWidth = (int)(width*mag+extraSize.width);
//		}
//		int xloc = (int)(maxWidth-wWidth)/2;
//		if (xloc<0) xloc = 0;
        return null;//todo revert to maxWindow?
    }


    @Override
    public void maximize() {
        if (!IJ.isMacOSX() && updatesEnabled) {
            fillNotFit = false;
            fitImageToCanvas();
            validate();
            //repaint();
            //imp.updateAndDraw();
            setAstroProcessor(false);
        }
    }

    @Override
    public void minimize() {
//        IJ.log("minimize");
//		srcRect = new Rectangle(0, 0, imageWidth, imageHeight);
//		ac.setDrawingSize(canWidthBeforeMaximize, canWidthBeforeMaximize);
//		ac.setMagnification(magbefore);
//        setSize(winWidthBeforeMaximize,winWidthBeforeMaximize);
//        ac.setMaxBounds();
        pack();
//		ac.setMaxBounds();
//        setSize(winWidthBeforeMaximize, winHeightBeforeMaximize);
//        fillNotFit =true;
//        fitImageToCanvas();
//		if (unzoomWhenMinimizing)
//			ic.unzoom();
//		unzoomWhenMinimizing = true;
        setAstroProcessor(false);
    }

//        @Override
//        protected Rectangle getMaxWindow(int xloc, int yloc) {
//            GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
//            Rectangle bounds = ge.getMaximumWindowBounds();
//
//            if (xloc>bounds.x+bounds.width || yloc>bounds.y+bounds.height || xloc<0 || yloc<0) {
//                Rectangle bounds2 = getSecondaryMonitorBounds(ge, xloc, yloc);
//                if (bounds2!=null) return bounds2;
//            }
//            Dimension ijSize = ij!=null?ij.getSize():new Dimension(0,0);
//            if (bounds.height>600) {
//                bounds.y += ijSize.height;
//                bounds.height -= ijSize.height;
//            }
//            return bounds;
//        }

    /** LAYOUT MANAGER METHODS **/

    /**
     * Not used by this class.
     */
    public void addLayoutComponent(String name, Component comp) {
    }

    /**
     * Not used by this class.
     */
    public void removeLayoutComponent(Component comp) {
    }

    /**
     * Returns the preferred dimensions for this layout.
     */
    public Dimension preferredLayoutSize(Container target) {
        Dimension dim = new Dimension((int) newCanvasWidth(), (int) (this.getHeight() - 56));

//		int nmembers = target.getComponentCount();
//		for (int i=0; i<nmembers; i++) {
//		    Component m = target.getComponent(i);
//			Dimension d = m.getPreferredSize();
//			dim.width = Math.max(dim.width, d.width);
//			if (i>0) dim.height += vgap;
//			dim.height += d.height;
//		}
//		Insets insets = target.getInsets();
//		dim.width += insets.left + insets.right + hgap*2;
//		dim.height += insets.top + insets.bottom + vgap*2;
        if (!redrawing) {
            ac.setDrawingSize((int) newCanvasWidth(), (int) newCanvasHeight());
        }

        return dim;
    }

    /**
     * Returns the minimum dimensions for this layout.
     */
    public Dimension minimumLayoutSize(Container target) {
        return preferredLayoutSize(target);
//        return new Dimension(500, 500);
    }

    /**
     * Centers the elements in the specified column, if there is any slack.
     */
    private void moveComponents(Container target, int x, int y, int width, int height, int nmembers) {
//       IJ.log("moveComponents executed");
//    	int x2 = 0;
//	    y += height / 2;
//		for (int i=0; i<nmembers; i++) {
//		    Component m = target.getComponent(i);
//		    Dimension d = m.getSize();
//		    if (i==0 || d.height>60)
//		    	x2 = x + (width - d.width)/2;
//			m.setLocation(x2, y);
//			y += vgap + d.height;
//		}
    }

    /**
     * Lays out the container and calls ImageCanvas.resizeCanvas()
     * to adjust the image canvas size as needed.
     */
    @Override
    public void layoutContainer(Container target) {
        if (!redrawing) {
            redrawing = true;
//		Insets insets = target.getInsets();
//		int nmembers = target.getComponentCount();
//		Dimension d;
//		int extraHeight = 0;
//		for (int i=1; i<nmembers; i++) {
//			Component m = target.getComponent(i);
//			d = m.getPreferredSize();
//			extraHeight += d.height;
//		}
//
//		d = target.getSize();
//		int preferredImageWidth = d.width - (insets.left + insets.right + hgap*2);
//		int preferredImageHeight = d.height - (insets.top + insets.bottom + vgap*2 + extraHeight);
//
//		resizeCanvas ((int)(imageWindow.getWidth() - 26), (int)(imageWindow.getHeight()-56-otherPanelsHeight-stackSliders.length*18));//(preferredImageWidth, preferredImageHeight);

//		int maxwidth = d.width - (insets.left + insets.right + hgap*2);
//		int maxheight = d.height - (insets.top + insets.bottom + vgap*2);
//		Dimension psize = preferredLayoutSize(target);
//		int x =  hgap + (d.width - psize.width)/2; //insets.left + hgap + (d.width - psize.width)/2;
//		int y = 0;
//		int colw = 0;

//		for (int i=0; i<nmembers; i++) {
//			Component m = target.getComponent(0);
//			Dimension d = m.getPreferredSize();
//			if ((m instanceof ScrollbarWithLabel) || (m instanceof Scrollbar)) {
//				int scrollbarWidth = target.getComponent(0).getPreferredSize().width;
//				Dimension minSize = m.getMinimumSize();
//				if (scrollbarWidth<minSize.width) scrollbarWidth = minSize.width;
//				m.setSize(scrollbarWidth, d.height);
//			} else
//				m.setSize(d.width, d.height);
//			if (y > 0) y += vgap;
//			y += d.height;
//			colw = Math.max(colw, d.width);
//		}
//		moveComponents(target, x, insets.top + vgap, colw, maxheight - y, nmembers);

//        else
//            {
            Dimension psize = preferredLayoutSize(target);
            Component m = target.getComponent(0);
            Dimension d = m.getPreferredSize();
            m.setSize(d.width, d.height);

//            }
            int newCanvasWidth = newCanvasWidth();
            int newCanvasHeight = newCanvasHeight();
            ac.setDrawingSize(newCanvasWidth, newCanvasHeight);
            Rectangle r = new Rectangle(ac.getSrcRect());

            if (imp == null) {
                return;
            }
//            double xmag = (double)newCanvasWidth/(double)(ac.getSrcRect().width);
//            double ymag = (double)newCanvasHeight/(double)(ac.getSrcRect().height);
//            if (IJ.altKeyDown() || (r.x <= 12 && r.y <= 12 && r.x+r.width > imp.getWidth()- 12 && r.x+r.height>imp.getHeight() - 12))
            if (IJ.altKeyDown() || (imp.getWidth() * ac.getMagnification() < newCanvasWidth || imp.getHeight() * ac.getMagnification() < newCanvasHeight)) {
                double xmag = (double) newCanvasWidth / (double) (imp.getWidth());
                double ymag = (double) newCanvasHeight / (double) (imp.getHeight());
                ac.setMagnification(Math.min(xmag, ymag));
                int xStart = (int) (imp.getWidth() / 2.0 - newCanvasWidth / (ac.getMagnification() * 2.0));
                int yStart = (int) (imp.getHeight() / 2.0 - newCanvasHeight / (ac.getMagnification() * 2.0));
                r.x = xStart;//Math.abs(xStart)<3?0:xStart;
                r.y = yStart;//Math.abs(yStart)<3?0:yStart;
            }

            magnification = ac.getMagnification();

            r.width = (int) ((double) (newCanvasWidth()) / magnification);
            r.height = (int) ((double) (newCanvasHeight()) / magnification);
            ac.setSourceRect(r);

//            imageWindow = imp.getWindow();
//            ac = OverlayCanvas.getOverlayCanvas(imp);
            dstWidth = ac.getWidth();
            dstHeight = ac.getHeight();
            magnification = ac.getMagnification();
            srcRect = ac.getSrcRect();
//            maxBounds = imageWindow.getMaximumBounds();
//            imageWindow.setMaximizedBounds(new Rectangle (1000, 500));
            imageWidth = imp.getWidth();
            imageHeight = imp.getHeight();
            oldICWidth = this.getWidth();
            oldICHeight = this.getHeight();

            winWidth = this.getWidth();
            winHeight = this.getHeight();

            srcRect = ac.getSrcRect();
            savedPanX = srcRect.x;
            savedPanY = srcRect.y;
            savedPanHeight = srcRect.height;
            savedPanWidth = srcRect.width;
            savedMag = magnification;
            savedICWidth = newCanvasWidth();
            savedICHeight = newCanvasHeight();
            savedIpWidth = imp.getWidth();
            savedIpHeight = imp.getHeight();
            Prefs.set("Astronomy_Tool.savedPanX", savedPanX);
            Prefs.set("Astronomy_Tool.savedPanY", savedPanY);
            Prefs.set("Astronomy_Tool.savedPanHeight", savedPanHeight);
            Prefs.set("Astronomy_Tool.savedPanWidth", savedPanWidth);
            Prefs.set("Astronomy_Tool.savedMag", savedMag);
            Prefs.set("Astronomy_Tool.savedICWidth", savedICWidth);
            Prefs.set("Astronomy_Tool.savedICHeight", savedICHeight);
            Prefs.set("Astronomy_Tool.savedIpWidth", savedIpWidth);
            Prefs.set("Astronomy_Tool.savedIpHeight", savedIpHeight);
            setImageEdges();
            redrawing = false;
        }
    }

//	void setMaxBounds() {
//        IJ.log("setMaxBounds");
//        imageWindow.setMaximizedBounds(new Rectangle (500, 1000));
////		if (maxBoundsReset) {
////			maxBoundsReset = false;
////			imageWindow = imp.getWindow();
////			if (imageWindow!=null && !IJ.isLinux() && maxBounds!=null) {
////				imageWindow.setMaximizedBounds(maxBounds);
////				setMaxBoundsTime = System.currentTimeMillis();
////			}
////		}
//	}
//
//	void resetMaxBounds() {
//        IJ.log("resetMaxBounds");
//		imageWindow = imp.getWindow();
//		if (imageWindow!=null && (System.currentTimeMillis()-setMaxBoundsTime)>500L) {
//			imageWindow.setMaximizedBounds(maxBounds);
//			maxBoundsReset = true;
//		}
//	}
//
//
//	/** Enlarge the canvas if the user enlarges the window. */
//	void resizeCanvas(int width, int height) {
//        IJ.log("resize canvas");
//        ac.setDrawingSize(newCanvasWidth(), newCanvasHeight());
//        double xmag = (double)newCanvasWidth()/(double)(ac.getSrcRect().width);
//        double ymag = (double)newCanvasHeight()/(double)(ac.getSrcRect().height);
//        ac.setMagnification(Math.max(xmag,ymag));
//
//
//		imageWindow = imp.getWindow();
//        ac = OverlayCanvas.getOverlayCanvas(imp);
//        dstWidth = ac.getWidth();
//        dstHeight = ac.getHeight();
//        magnification = ac.getMagnification();
//        srcRect = ac.getSrcRect();
//        maxBounds = imageWindow.getMaximumBounds();
//
//        imageWidth = imp.getWidth();
//        imageHeight = imp.getHeight();
//
////		IJ.log("resizeCanvas: "+srcRect+" "+imageWidth+"  "+imageHeight+" "+width+"  "+height+" "+dstWidth+"  "+dstHeight+" "+win.maxBounds);
//		if (!maxBoundsReset&& (width>dstWidth||height>dstHeight)&&imageWindow!=null&&maxBounds!=null&&width!=maxBounds.width-10) {
//			if (resetMaxBoundsCount!=0)
//				resetMaxBounds(); // Works around problem that prevented window from being larger than maximized size
//			resetMaxBoundsCount++;
//        }
//
////		if (IJ.altKeyDown())
////			{fitToWindow(); return;}
////		if (srcRect.width<imageWidth || srcRect.height<imageHeight) {
////			if (width>imageWidth*magnification)
////				width = (int)(imageWidth*magnification);
////			if (height>imageHeight*magnification)
////				height = (int)(imageHeight*magnification);
////			ac.setDrawingSize(width, height);
////			srcRect.width = (int)(dstWidth/magnification);
////			srcRect.height = (int)(dstHeight/magnification);
////			if ((srcRect.x+srcRect.width)>imageWidth)
////				srcRect.x = imageWidth-srcRect.width;
////			if ((srcRect.y+srcRect.height)>imageHeight)
////				srcRect.y = imageHeight-srcRect.height;
////			ac.repaint();
//		}

//        winWidth = imageWindow.getWidth();
//        winHeight = imageWindow.getHeight();
//
//        IJ.log(""+oldWidth +" "+ winWidth +" "+ oldHeight +" "+ winHeight);
//
//        ac.setSize((int)(winWidth - 26), (int)(winHeight-otherPanelsHeight-stackSliders.length*18));
//        ac.setDrawingSize((int)(winWidth - 26), (int)(winHeight-otherPanelsHeight-stackSliders.length*18));
//        ac.setBounds(10, 50, (int)(winWidth - 26), (int)(winHeight-otherPanelsHeight-stackSliders.length*18));
//        ac.setMagnification(ac.getMagnification()*(double)winHeight/(double)oldHeight);
//        SpringUtil.makeCompactGrid (mainPanel, 5 + stackSliders.length, 1, 0,0,0,0);
//        ac.validate();
//        topPanelAL.validate();
//        topPanelAC.validate();
//        topPanelAR.validate();
//        topPanelBL.validate();
//        topPanelBC.validate();
//        topPanelBR.validate();
//        topPanelA.validate();
//        topPanelB.validate();
//        bottomPanelA.validate();
//        bottomPanelB.validate();
//        mainPanel.validate();

    //       mainPanel.repaint();

//        ac = OverlayCanvas.getOverlayCanvas(imp);
//        icHeight = ac.getHeight();
//        icWidth = ac.getWidth();
//		//IJ.log("resizeCanvas2: "+srcRect+" "+dstWidth+"  "+dstHeight+" "+width+"  "+height);
//	}


    /**
     * Handle the key typed event from the text field.
     */
    public void keyTyped(KeyEvent e) {

    }

    /**
     * Handle the key-pressed event from the text field.
     */
    public void keyPressed(KeyEvent e) {
        int keyCode = e.getKeyCode();

        if (keyCode == KeyEvent.VK_TAB) {
            shiftAndControlWasDown = true;
            IJ.setTool(0);
            exitAstronomyTool();
        }
        if (e.isShiftDown() && keyCode == KeyEvent.VK_UP)
            zoomIn(startDragScreenX, startDragScreenY, true, true, 8.0);
        else if (e.isShiftDown() && keyCode == KeyEvent.VK_DOWN) {
            if ((e.getModifiers() & e.ALT_MASK) != 0)
                fillNotFit = true;
            fitImageToCanvas();
        } else if (keyCode == '+' || keyCode == '=' || keyCode == KeyEvent.VK_UP)
            zoomIn(startDragScreenX, startDragScreenY, true, true, 0.0);

        else if (keyCode == '-' || keyCode == '_' || keyCode == KeyEvent.VK_DOWN)
            zoomOut(startDragScreenX, startDragScreenY, true, true);

    }

    /**
     * Handle the key-released event from the text field.
     */
    public void keyReleased(KeyEvent e) {
        int keyCode = e.getKeyCode();
        if (keyCode == KeyEvent.VK_TAB) {
            shiftAndControlWasDown = false;
            IJ.setTool(astronomyToolId);
            reenterAstronomyTool();
        }
    }


    public void mouseClicked(MouseEvent e) {

        // mouse clicked code is in mouseReleased() to allow drag/click thresholding
        IJ.setInputEvent(e);
    }

    public void mousePressed(MouseEvent e) {

        startDragScreenX = e.getX();
        startDragScreenY = e.getY();
        ac.setMousePosition(startDragScreenX, startDragScreenY);
        startDragX = ac.offScreenXD(startDragScreenX);
        startDragY = ac.offScreenYD(startDragScreenY);
        startDragCenX = startDragX;
        startDragCenY = startDragY;
        startButtonCentroid = (buttonCentroid.isSelected() && !e.isShiftDown()) || (!buttonCentroid.isSelected() && e.isShiftDown());
        if (cen.measure(imp, startDragX, startDragY, radius, rBack1, rBack2, startButtonCentroid, backPlane, removeBackStars) && startButtonCentroid) {
            startDragCenX = cen.xCenter;
            startDragCenY = cen.yCenter;
        }
        lastScreenX = startDragScreenX;  //update in mouseDragged during drag
        lastScreenY = startDragScreenY;  //update in mouseDragged during drag
        startDragSubImageX = ac.getSrcRect().x;
        startDragSubImageY = ac.getSrcRect().y;
        button23Drag = false;
        IJ.setInputEvent(e);
        newClick = true;

//                if((e.getModifiers() & MouseEvent.BUTTON2_MASK) != 0)
//                    {
//                    if (e.getClickCount() == 1)
//                        {
//                        imp.killRoi();
//                        }
//                    else
//                        {
//                        IJ.runPlugIn("Clear_Overlay", "");
//                        }
//                    }

        if (goodWCS) {
            xy[0] = startDragCenX;
            xy[1] = startDragCenY;
            startRadec = wcs.pixels2wcs(xy);
        }
    }


    public void mouseReleased(MouseEvent e) {

        boolean apMoving = movingAperture;
        int screenX = e.getX();
        int screenY = e.getY();
        ac.xClicked = e.getX();
        ac.yClicked = e.getY();
        movingAperture = false;
        ac.setMousePosition(screenX, screenY);
        double imageX = ac.offScreenXD(screenX);
        double imageY = ac.offScreenYD(screenY);
        ImageProcessor ip = imp.getProcessor();
        IJ.setInputEvent(e);
        if (((e.getModifiers() & MouseEvent.BUTTON2_MASK) != 0 || (e.getModifiers() & MouseEvent.BUTTON3_MASK) != 0) && button23Drag) //  right-drag or middle drag measures distance and delta mag and reports in Tables
        {
            button23Drag = false;
            endButtonCentroid = (buttonCentroid.isSelected() && !e.isShiftDown()) || (!buttonCentroid.isSelected() && e.isShiftDown());
            if (endCen.measure(imp, imageX, imageY, radius, rBack1, rBack2, endButtonCentroid, backPlane, removeBackStars) && endButtonCentroid) {
                imageX = endCen.xCenter;
                imageY = endCen.yCenter;
            }
            if (goodWCS) {
                xy[0] = imageX;
                xy[1] = imageY;
                radec = wcs.pixels2wcs(xy);
            }
            if (!apMoving) {
                OverlayCanvas overlayCanvas = OverlayCanvas.getOverlayCanvas(imp);
                measRoi.setShow(false);
                overlayCanvas.removeLiveMeasurementRoi();
                photom1.setRemoveBackStars(removeBackStars);
                photom1.setUsePlane(backPlane);
                photom1.setMarkRemovedPixels(false);
                photom1.measure(imp, exact, startDragCenX, startDragCenY, radius, rBack1, rBack2);
                photom2.setRemoveBackStars(removeBackStars);
                photom2.setUsePlane(backPlane);
                photom2.setMarkRemovedPixels(false);
                photom2.measure(imp, exact, imageX, imageY, radius, rBack1, rBack2);
                MeasurementRoi mroi = new MeasurementRoi(false, true, showMeasureCircle, showMeasureCrosshair && startButtonCentroid, showMeasureCrosshair && endButtonCentroid,
                        showMeasureLength, showMeasurePA, showMeasureDelMag, showMeasureFluxRatio, startDragCenX, startDragCenY, imageX, imageY, radius,
                        getLengthLabel(imageX, imageY), getPALabel(imageX, imageY), getDelMagLabel(), getFluxRatioLabel(), IJU.colorOf(defaultMeasurementColor), showMeasureMultiLines);
                mroi.setImage(imp);
                overlayCanvas.add(mroi);
            }

            double value = ip.getPixelValue((int) imageX, (int) imageY);

            updateXYValue(imageX, imageY, DRAGGING);
            if (!apMoving) {
                updateResultsTable(imageX, imageY, value, DRAGGING);
            }
        }

        if (Math.abs(screenX - startDragScreenX) + Math.abs(screenY - startDragScreenY) < 4.0)    //check mouse click/drag threshold
        {
            if (e.getClickCount() > 1 && e.getButton() == MouseEvent.BUTTON3 && !e.isControlDown() && !e.isAltDown() && currentToolId != zoomToolId) {
                doubleClick = true;
                if (IJ.altKeyDown() || e.getClickCount() == 2)
                    fillNotFit = true;
                fitImageToCanvas();
            } else if (e.getClickCount() > 1 && e.getButton() == MouseEvent.BUTTON1 && !e.isControlDown() && !e.isAltDown() && currentToolId != zoomToolId) {
                zoomIn(startDragScreenX, startDragScreenY, true, false, 10.0);
            } else {
                if (e.getButton() == MouseEvent.BUTTON1)                          //left mouse click
                {
                    if ((e.isControlDown() && !e.isShiftDown()) || currentToolId == zoomToolId)
                        zoomControl(e.getX(), e.getY(), -1, MOUSECLICK);
                    else if ((e.isShiftDown() || currentToolId == apertureToolId) && !shiftClickDisabled) {
//                                        imp.setRoi(new OvalRoi((int)(imageX-radius+0.5), (int)(imageY-radius+0.5), (int)(radius*2.0), (int)(radius*2.0)));
                        IJ.runPlugIn(imp, "Astronomy.Aperture_", "");
                        validate();
                    } else if (e.isAltDown() && !shiftClickDisabled) {
                        IJ.runPlugIn(imp, "Astronomy.Seeing_Profile", "alt-click");
                    } else if (!shiftClickDisabled) {
                        checkEditRoi(imageX, imageY, 1);
                    }
                } else if (e.getButton() == MouseEvent.BUTTON2)                     //middle mouse click
                {
                    if (goodWCS) {
                        xy[0] = startDragCenX;
                        xy[1] = startDragCenY;
                        radec = wcs.pixels2wcs(xy);
                    }
                    updateXYValue(imageX, imageY, NOT_DRAGGING);
                    if (!apMoving) {
                        photom1.setRemoveBackStars(removeBackStars);
                        photom1.setUsePlane(backPlane);
                        photom1.setMarkRemovedPixels(false);
                        photom1.measure(imp, exact, startDragCenX, startDragCenY, radius, rBack1, rBack2);
                        if (writePhotometricDataTable) {
                            ApertureRoi roi = new ApertureRoi(startDragCenX, startDragCenY, radius, rBack1, rBack2, photom1.source, startButtonCentroid);
                            roi.setAppearance(true, startButtonCentroid, showSkyOverlay, nameOverlay, valueOverlay, Color.RED, "", photom1.source);
                            roi.setImage(imp);
                            OverlayCanvas.getOverlayCanvas(imp).add(roi);
                            OverlayCanvas.getOverlayCanvas(imp).repaint();
                        }
                        updateResultsTable(startDragCenX, startDragCenY, ip.getPixelValue((int) startDragCenX, (int) startDragCenY), NOT_DRAGGING);
                    }
                    if (middleClickCenter) {
                        zoomControl(screenX, screenY, 0, MOUSECLICK);
                        try {
                            Robot robot = new Robot();     //move mouse pointer to new position on screen
                            robot.mouseMove(ac.getLocationOnScreen().x + ac.getWidth() / 2 - defaultScreenBounds.x,
                                    ac.getLocationOnScreen().y + ac.getHeight() / 2 - defaultScreenBounds.y);
                        } catch (AWTException ee) {
                        }
                    }
                } else if (e.getButton() == MouseEvent.BUTTON3)    //right mouse click
                {
                    if ((e.isControlDown() && !e.isShiftDown()) || currentToolId == zoomToolId)
                        zoomControl(screenX, screenY, 1, MOUSECLICK);
                    else if (!e.isControlDown() && !e.isShiftDown() && !shiftClickDisabled) {
                        checkEditRoi(imageX, imageY, 3);
                    }
                }
            }
        }
        ac.repaint();
        startDragScreenX = screenX;
        startDragScreenY = screenY;
    }

    public void mouseEntered(MouseEvent e) {
        reposition = Prefs.get("aperture.reposition", reposition);
        buttonCentroid.setSelected(reposition);
        showSkyOverlay = Prefs.get("aperture.skyoverlay", showSkyOverlay);
        buttonShowSky.setSelected(showSkyOverlay);
        nameOverlay = Prefs.get("aperture.nameoverlay", nameOverlay);
        buttonSourceID.setSelected(nameOverlay);
        valueOverlay = Prefs.get("aperture.valueoverlay", valueOverlay);
        buttonSourceCounts.setSelected(valueOverlay);
        radius = Prefs.get("aperture.radius", radius);
        rBack1 = Prefs.get("aperture.rback1", rBack1);
        rBack2 = Prefs.get("aperture.rback2", rBack2);
        ac.setAperture(radius, rBack1, rBack2, showSkyOverlay, showPhotometer);
        photom.setSourceApertureRadius(radius);
        photom.setBackgroundApertureRadii(rBack1, rBack2);
        removeBackStars = Prefs.get("aperture.removebackstars", removeBackStars);
        photom.setRemoveBackStars(removeBackStars);
        removeBackStarsCB.setState(removeBackStars);
        backPlane = Prefs.get("aperture.backplane", backPlane);
        photom.setUsePlane(backPlane);
        exact = Prefs.get("aperture.exact", exact);
        showRemovedPixels = Prefs.get("aperture.showremovedpixels", showRemovedPixels);
        showRemovedPixelsCB.setState(showRemovedPixels);
//            if (OverlayCanvas.hasOverlayCanvas(imp))     //UPDATES ALL APERTURES WITH NEW SETTINGS, AS CODED HERE, IT CAUSES MULTI-APERTURE
//                {                                        //APERTURES TO INAPPROPRIATELY RESIZE WHEN MOUSE CURSOR REENTERS IMAGE
//                                                         //MAYBE FIX THE ISSUE BY ADDING A MULTI-APERTURE SOURCE ID TO EACH APERTUREROI?
//                OverlayCanvas oc = OverlayCanvas.getOverlayCanvas(imp);
//                Roi[] rois = oc.getRois();
//                for (int i=0; i<rois.length; i++)
//                    {
//                    if (rois[i] instanceof astroj.ApertureRoi)
//                        {
//                        astroj.ApertureRoi aroi = (astroj.ApertureRoi)rois[i];
//                        aroi.setSize(radius, rBack1, rBack2);
//                        aroi.setShowSky(showSkyOverlay);
//                        aroi.setShowName(nameOverlay);
//                        aroi.setShowValue(valueOverlay);
//                        }
//                    }
//                }
        ac.paint(ac.getGraphics());
    }

    public void updateIntCnts() {
        radius = Prefs.get("aperture.radius", radius);
        rBack1 = Prefs.get("aperture.rback1", rBack1);
        rBack2 = Prefs.get("aperture.rback2", rBack2);
        photom = new Photometer(cal);
        photom.setRemoveBackStars(removeBackStars);
        ac.setAperture(radius, rBack1, rBack2, showSkyOverlay, showPhotometer);
        photom.setMarkRemovedPixels(false);
        photom.measure(imp, exact, lastImageX, lastImageY, radius, rBack1, rBack2);
        if (showMeanNotPeak) {
            peakLabel.setText("Mean:");
            writeNumericPanelField(photom.meanBrightness(), peakTextField);
        } else {
            peakLabel.setText("Peak:");
            writeNumericPanelField(photom.peakBrightness(), peakTextField);
        }
        lengthLabel.setText("Int Cnts:");
        writeNumericPanelField(photom.sourceBrightness(), lengthTextField);
    }

    @Override
    public void showSlice(int index) {
        super.showSlice(index);

        // Fixes image not updating when slice changes with animate feature
        //todo fix slow draw
        ac.paint(ac.getGraphics(), true);

        drawSubtitle();

        // Fixes subtitle (x/ nslices string at top of window) not updating
        synchronized (this) {
            notify();
        }
    }

    @Override
    // Part of fix for GH-20
    public synchronized void adjustmentValueChanged(AdjustmentEvent e) {
        super.adjustmentValueChanged(e);
        if (e.getSource() == zSelector) {
            z = zSelector.getValue();
            int slice = hyperStack ? imp.getSlice() : imp.getCurrentSlice();
            if (z == slice && e.getAdjustmentType() == AdjustmentEvent.TRACK) return;
            drawSubtitle();
            notifyAll();
        }
    }

    public void mouseExited(MouseEvent e) {
//            apertureOverlay.clear();
        ac.setMouseInImage(false);
        ac.paint(ac.getGraphics());
    }

    private Future<?> previousTask;
    private static final ExecutorService PHOTOMETER_THREAD = Executors.newSingleThreadExecutor();//todo not static?

    public void mouseMoved(MouseEvent e) {
        if (previousTask != null) {
            previousTask.cancel(true);
        }

        previousTask = PHOTOMETER_THREAD.submit(() -> {
            ac.setMousePosition(e.getX(), e.getY());
            screenX = e.getX();
            screenY = e.getY();
            double imageX = ac.offScreenXD(e.getX());
            double imageY = ac.offScreenYD(e.getY());
            lastImageX = imageX;
            lastImageY = imageY;
            xy[0] = imageX;
            xy[1] = imageY;
            photom.measure(imp, exact, imageX, imageY, radius, rBack1, rBack2);
            IJ.setInputEvent(e);
            if (showPhotometer) {// != e.isShiftDown())
                ac.setMouseInImage(true);
            }

            if (goodWCS) {
                radec = wcs.pixels2wcs(xy);
            }

            SwingUtilities.invokeLater(() -> {
                if (showPhotometer) {
                    ac.paint(ac.getGraphics());
                }

                updateXYValue(imageX, imageY, NOT_DRAGGING, e.isShiftDown(), e.isControlDown());
            });

            prevImageX = lastImageX;
            prevImageY = lastImageY;
        });
    }

    public void mouseDragged(MouseEvent e) {

        int screenX = e.getX();
        int screenY = e.getY();
        ac.setMousePosition(screenX, screenY);
        double imageX = ac.offScreenXD(screenX);
        double imageY = ac.offScreenYD(screenY);
        ac.xClicked = e.getX();
        ac.yClicked = e.getY();
        IJ.setInputEvent(e);
        lastImageX = imageX;
        lastImageY = imageY;
        magnification = ac.getMagnification();
        photom.measure(imp, exact, imageX, imageY, radius, rBack1, rBack2);
        IJ.setInputEvent(e);
        if (Math.abs(screenX - startDragScreenX) + Math.abs(screenY - startDragScreenY) >= 4.0)  //check mouse click/drag threshold
        {
//                        apertureOverlay.clear();
            ac.repaint();
            if (false) //((e.getModifiers() & MouseEvent.BUTTON3_MASK) != 0)    // change screen min and max display values
            {                                                 //
                if (e.isControlDown())                            // if control press do nothing
                {
                } else                                                //no modifier - change screen min and max display values
                {
                    adjustMinAndMax(lastScreenX - screenX, lastScreenY - screenY);

                    lastScreenX = screenX;
                    lastScreenY = screenY;
                    if (goodWCS) {
                        xy[0] = imageX;
                        xy[1] = imageY;
                        radec = wcs.pixels2wcs(xy);
                    }
                    updateXYValue(imageX, imageY, NOT_DRAGGING);
                }
            } else if ((e.getModifiers() & MouseEvent.BUTTON2_MASK) != 0 || (e.getModifiers() & MouseEvent.BUTTON3_MASK) != 0)        // dragging with middle mouse button
            {                                                          // measure distance
                if (goodWCS) {
                    xy[0] = imageX;
                    xy[1] = imageY;
                    radec = wcs.pixels2wcs(xy);
                }
                if (!movingAperture) {
                    if (!button23Drag) {
                        measRoi.setAppearance(true, true, showMeasureCircle, showMeasureCrosshair && startButtonCentroid, false, showMeasureLength, showMeasurePA, false, false,
                                startDragCenX, startDragCenY, imageX, imageY, radius,
                                getLengthLabel(imageX, imageY), getPALabel(imageX, imageY), "", "", IJU.colorOf(defaultMeasurementColor), showMeasureMultiLines);
                        measRoi.setImage(imp);
                        OverlayCanvas.getOverlayCanvas(imp).add(measRoi);
                    } else {
                        measRoi.setAppearance(true, true, showMeasureCircle, showMeasureCrosshair && startButtonCentroid, false, showMeasureLength, showMeasurePA, false, false,
                                startDragCenX, startDragCenY, imageX, imageY, radius,
                                getLengthLabel(imageX, imageY), getPALabel(imageX, imageY), "", "", IJU.colorOf(defaultMeasurementColor), showMeasureMultiLines);
                    }
                }
                button23Drag = true;                                        // and save to results window when mouse released

                //if (!movingAperture) imp.setRoi(new Line(startDragCenX, startDragCenY, imageX, imageY));

                updateXYValue(imageX, imageY, DRAGGING);
            }

//                        else if ((e.getModifiers() & MouseEvent.BUTTON1_MASK) != 0 && e.isShiftDown() && !e.isControlDown())
//                            {
//                            imp.setRoi(new OvalRoi(imageX>startDragX?startDragX:imageX, imageY>startDragY?startDragY:imageY,
//                                    imageX>startDragX?imageX-startDragX:startDragX-imageX,
//                                    imageY>startDragY?imageY-startDragY:startDragY-imageY));
//                            }
            else if ((e.getModifiers() & MouseEvent.BUTTON1_MASK) != 0 && e.isControlDown() && !e.isShiftDown()) {
                imp.setRoi(new Rectangle(imageX > startDragX ? (int) startDragX : (int) imageX, imageY > startDragY ? (int) startDragY : (int) imageY,
                        imageX > startDragX ? (int) imageX - (int) startDragX : (int) startDragX - (int) imageX,
                        imageY > startDragY ? (int) imageY - (int) startDragY : (int) startDragY - (int) imageY));
            } else if ((e.getModifiers() & MouseEvent.BUTTON1_MASK) != 0 && !(e.isControlDown() || e.isShiftDown() || e.isAltDown())) {                                                 // dragging with left mouse button (pan image)
                Rectangle imageRect = ac.getSrcRect();
                String lab;

                if (astronomyMode && !movingAperture) {
//                                        int w = (int)Math.round(icWidth/magnification);
//                                        if (w*magnification<icWidth) w++;
//                                        int h = (int)Math.round(icHeight/magnification);
//                                        if (h*magnification<icHeight) h++;
                    int height = imp.getHeight();
                    int width = imp.getWidth();
                    int ox = netFlipX ? startDragSubImageX + imageRect.width - (int) (e.getX() / magnification) :
                            startDragSubImageX + (int) (e.getX() / magnification);
                    int oy = netFlipY ? startDragSubImageY + imageRect.height - (int) (e.getY() / magnification) :
                            startDragSubImageY + (int) (e.getY() / magnification);
                    imageRect.x = startDragSubImageX + ((int) startDragX - ox);
                    imageRect.y = startDragSubImageY + ((int) startDragY - oy);
                    savedPanX = imageRect.x;
                    savedPanY = imageRect.y;
                    savedPanHeight = imageRect.height;
                    savedPanWidth = imageRect.width;

//                                        if (imageRect.x<0) imageRect.x = 0;
//                                        if (imageRect.y<0) imageRect.y = 0;
//                                        if (imageRect.x+w>icWidth) imageRect.x = ipWidth-w;
//                                        if (imageRect.y+h>icHeight) imageRect.y = ipHeight-h;

                    ac.setSourceRect(imageRect);
                    ImageProcessor ip = imp.getProcessor();
                    savedIpWidth = ip.getWidth();
                    savedIpHeight = ip.getHeight();
                    Prefs.set("Astronomy_Tool.savedIpWidth", savedIpWidth);
                    Prefs.set("Astronomy_Tool.savedIpHeight", savedIpHeight);
                    Prefs.set("Astronomy_Tool.savedPanX", savedPanX);
                    Prefs.set("Astronomy_Tool.savedPanY", savedPanY);
                    Prefs.set("Astronomy_Tool.savedPanHeight", savedPanHeight);
                    Prefs.set("Astronomy_Tool.savedPanWidth", savedPanWidth);
                    setImageEdges();
//                                        if (imageRect.x<0 || imageRect.y<0 || imageRect.x+imageRect.width>width || imageRect.y+imageRect.height>height)
//                                            {
//                                            clearAndPaint();
//                                            }
//                                        else
//                                            ac.repaint();
                    ac.paint(ac.getGraphics());
                }
                xy[0] = imageX;
                xy[1] = imageY;
                if (goodWCS) {
                    radec = wcs.pixels2wcs(xy);
                    lab = ", RA: " + fourPlaces.format(radec[0] / 15) + ", DEC: " + fourPlaces.format(radec[1]);
                } else {
                    lab = "";
                }
                updateXYValue(imageX, imageY, NOT_DRAGGING);
            }
        }

    }

    void checkEditRoi(double imageX, double imageY, int button) {
        mouseButton = button;
        doubleClick = false;
        rightClickPixel[0] = imageX;
        rightClickPixel[1] = imageY;
        try {
            doubleClickTask = new TimerTask() {
                public void run() {
                    if (!doubleClick) {
                        MeasurementRoi mroi = OverlayCanvas.getOverlayCanvas(imp).findMeasurementRoi(rightClickPixel[0], rightClickPixel[1]);
                        if (!(mroi == null)) {
                            editMeasurementRoi(mroi);
                        } else {
                            AnnotateRoi aroi = OverlayCanvas.getOverlayCanvas(imp).findAnnotateRoi(rightClickPixel[0], rightClickPixel[1]);
                            if (aroi == null) {
                                if (mouseButton == 3)
                                    displayAnnotation(rightClickPixel);
                                else {
                                    ApertureRoi aproi = OverlayCanvas.getOverlayCanvas(imp).findApertureRoi(rightClickPixel[0], rightClickPixel[1], 5);
                                    editApertureRoi(aproi);
                                }
                            } else {
                                if (!ac.showAnnotations) {
                                    IJ.showMessage("Enable display of annotations before editing annotation");
                                    return;
                                }
                                editAnnotateRoi(aroi);
                            }
                        }
                    }
                    doubleClickTask = null;
                    doubleClickTaskTimer = null;
                }
            };
            doubleClickTaskTimer = new java.util.Timer();
            doubleClickTaskTimer.schedule(doubleClickTask, 600);
        } catch (Exception eee) {
            IJ.showMessage("Error starting double right click timer task : " + eee.getMessage());
        }
    }

    void updateXYValue(double imageX, double imageY, boolean dragging) {
        updateXYValue(imageX, imageY, dragging, false, false);
    }

    void updateXYValue(double imageX, double imageY, boolean dragging, boolean shiftKeyDown, boolean controlKeyDown) {
        drawSubtitle();
        setValueTextField();
        ijXTextField.setText(fourPlaces.format(imageX));
        ijYTextField.setText(fourPlaces.format(imageY));
        fitsXTextField.setText(fourPlaces.format(imageX + Centroid.PIXELCENTER));
        fitsYTextField.setText(fourPlaces.format((double) imp.getHeight() - imageY + Centroid.PIXELCENTER));
        if (goodWCS) {
            if (useSexagesimal) {
                RATextField.setText(IJU.decToSexRA(radec[0]));
                DecTextField.setText(IJU.decToSexDec(radec[1]));
            } else {
                RATextField.setText(sixPlaces.format(radec[0] / 15));
                DecTextField.setText(sixPlaces.format(radec[1]));
            }
        } else {
            RATextField.setText("");
            DecTextField.setText("");
        }
        if (dragging) {
            peakLabel.setText("PA:");
            if (goodWCS) {
                lengthLabel.setText("Arclen:");
                writeNumericPanelField(posAngle(), peakTextField);
                if (useSexagesimal) {
                    lengthTextField.setText(IJU.decToSexDeg(arcLength(), 2, true));
                } else {
                    writeNumericPanelField(arcLength(), lengthTextField);
                }
            } else {
                if (ac.XPixelScale <= 0.0 || ac.YPixelScale <= 0.0) {
                    writeNumericPanelField((360.0 + Math.atan2((startDragCenX - imageX), (startDragCenY - imageY)) * 180.0 / Math.PI) % 360.0, peakTextField);
                    lengthLabel.setText("Length:");
                    writeNumericPanelField(Math.sqrt((imageX - startDragCenX) * (imageX - startDragCenX)
                            + (imageY - startDragCenY) * (imageY - startDragCenY)), lengthTextField);
                } else {
                    writeNumericPanelField((360.0 + Math.atan2(ac.XPixelScale * (startDragCenX - imageX), ac.YPixelScale * (startDragCenY - imageY)) * 180.0 / Math.PI) % 360.0, peakTextField);
                    lengthLabel.setText("Arclen:");
                    if (useSexagesimal)
                        lengthTextField.setText(IJU.decToSexDeg(Math.sqrt(ac.XPixelScale * ac.XPixelScale * (imageX - startDragCenX) * (imageX - startDragCenX) +
                                ac.YPixelScale * ac.YPixelScale * (imageY - startDragCenY) * (imageY - startDragCenY)) / 3600.0, 2, true));
                    else
                        writeNumericPanelField(Math.sqrt(ac.XPixelScale * ac.XPixelScale * (imageX - startDragCenX) * (imageX - startDragCenX) +
                                ac.YPixelScale * ac.YPixelScale * (imageY - startDragCenY) * (imageY - startDragCenY)) / 3600.0, lengthTextField);
                }
            }
        } else {
            if (showMeanNotPeak) {
                peakLabel.setText("Mean:");
                writeNumericPanelField(photom.meanBrightness(), peakTextField);
            } else {
                peakLabel.setText("Peak:");
                writeNumericPanelField(photom.peakBrightness(), peakTextField);
            }
            lengthLabel.setText("Int Cnts:");
            writeNumericPanelField(photom.sourceBrightness(), lengthTextField);

            var stack = imp.getStack();


            if ((shiftKeyDown && controlKeyDown) || plotStackPixelValues.get()) {
                if (stackPixelPlot == null) {
                    stackPixelPlot = new Plot("Pixel Values", "Slice", "Value");
                }
                stackPixelPlot.setXYLabels("Slice", "Value at (x= %,.2f, y= %,.2f)".formatted(imageX + Centroid.PIXELCENTER, (double) imp.getHeight() - imageY + Centroid.PIXELCENTER));
                stackPixelPlot.allPlotObjects.clear();
                stackPixelPlot.add("dot", IntStream.range(1, stack.size() + 1)
                        .mapToDouble(n -> stack.getProcessor(n).getf((int) imageX, (int) imageY)).toArray());
                stackPixelPlot.setLimitsToFit(true);
                if (stackPixelPlotWin == null || !stackPixelPlotWin.isShowing()) {
                    stackPixelPlotWin = stackPixelPlot.show();
                    stackPlotWindowLocation.locationSavingWindow(stackPixelPlotWin);
                }
                stackPixelPlotWin.getImagePlus().setPlot(stackPixelPlot);
                stackPixelPlot.update();
            }
        }
    }

    String getLengthLabel(double imageX, double imageY) {
        String s = "";
        if (goodWCS) {
            s = "ArcLen: ";
            if (showMeasureSex) {
                s += IJU.decToSexDeg(arcLength(), 2, true);
            } else {
                s += fourPlaces.format(arcLength()) + "\u00B0";
            }
        } else {
            if (ac.XPixelScale <= 0.0 || ac.YPixelScale <= 0.0) {

                s = "Length: " + twoPlaces.format(Math.sqrt((imageX - startDragCenX) * (imageX - startDragCenX)
                        + (imageY - startDragCenY) * (imageY - startDragCenY))) + " pixels";
            } else {
                s = "ArcLen: ";
                if (showMeasureSex)
                    s += (IJU.decToSexDeg(Math.sqrt(ac.XPixelScale * ac.XPixelScale * (imageX - startDragCenX) * (imageX - startDragCenX) +
                            ac.YPixelScale * ac.YPixelScale * (imageY - startDragCenY) * (imageY - startDragCenY)) / 3600.0, 2, true));
                else
                    s += fourPlaces.format(Math.sqrt(ac.XPixelScale * ac.XPixelScale * (imageX - startDragCenX) * (imageX - startDragCenX) +
                            ac.YPixelScale * ac.YPixelScale * (imageY - startDragCenY) * (imageY - startDragCenY)) / 3600.0) + "\u00B0";
            }
        }
        return s;
    }

    String getPALabel(double imageX, double imageY) {
        String s = "";
        if (goodWCS) {
            s = "PA: " + twoPlaces.format(posAngle()) + "\u00B0";
        } else {
            if (ac.XPixelScale <= 0.0 || ac.YPixelScale <= 0.0) {
                s = "Y-axis Angle: " + twoPlaces.format((360.0 + Math.atan2((startDragCenX - imageX), (startDragCenY - imageY)) * 180.0 / Math.PI) % 360.0) + "\u00B0";
            } else {
                s = "Y-axis Angle: " + twoPlaces.format((360.0 + Math.atan2(ac.XPixelScale * (startDragCenX - imageX), ac.YPixelScale * (startDragCenY - imageY)) * 180.0 / Math.PI) % 360.0) + "\u00B0";
            }
        }
        return s;
    }

    String getDelMagLabel() {
        double delMag = getDelMag();
        if (delMag <= -1000.0) return "\u0394mag: < -1000.00";
        if (delMag >= 1000.0) return "\u0394mag: > 1000.00";
        return "\u0394mag: " + twoPlaces.format(delMag);
    }

    Double getDelMag() {
        double flux1 = photom1.source;
        if (flux1 < 0) flux1 = 0.0;
        double flux2 = photom2.source;
        if (flux2 < 0) flux2 = 0.0;
        if (flux2 == 0.0 && flux1 == 0.0) return 0.0;
        if (flux2 == 0.0) return negateMeasureDelMag ? -1000.00 : 1000.00;
        if (flux1 == 0.0) return negateMeasureDelMag ? 1000.00 : -1000.00;
        double delMag = (negateMeasureDelMag ? 1 : -1) * 2.5 * Math.log10(flux2 / flux1);
        if (delMag < -1000.0) return -1000.00;
        if (delMag > 1000.0) return 1000.00;
        return delMag;
    }

    String getFluxRatioLabel() {
        double fluxRatio = getFluxRatio();
        if (Double.isInfinite(fluxRatio)) return "F2/F1: Infinity";
        return "F2/F1: " + sixPlaces.format(fluxRatio);
    }

    Double getFluxRatio() {
        double flux1 = photom1.source;
        if (flux1 < 0) flux1 = 0.0;
        double flux2 = photom2.source;
        if (flux2 < 0) flux2 = 0.0;
        if (flux2 == 0.0 && flux1 == 0.0) return 1.0;
        if (flux2 == 0.0) return 0.0;
        if (flux1 == 0.0) return Double.POSITIVE_INFINITY;
        return flux2 / flux1;
    }

    double arcLength() {
        return wcs.getWCSDistance(startRadec, radec);

        //DS9 reports dimensions of square matching the following simplistic formulation
//            return Math.sqrt(wcs.getXScale()*wcs.getXScale()*(startDragX-xy[0])*(startDragX-xy[0]) + wcs.getYScale()*wcs.getYScale()*(startDragY-xy[1])*(startDragY-xy[1]));


//            return Math.acos(Math.cos((90.-startRadec[1])*d2r)*Math.cos((90. - radec[1])*d2r) +
//                    Math.sin((90.-startRadec[1])*d2r)*Math.sin((90.-radec[1])*d2r)*
//                    Math.cos((startRadec[0]-radec[0])*d2r))/d2r;

    }

    double posAngle() {
        return wcs.getWCSPA(startRadec, radec);
    }


    int parseInteger(String s) {
        if (s == null) return 0;
        int value = 0;
        try {
            value = Integer.parseInt(s);
        } catch (NumberFormatException e) {
        }
        return value;
    }


    void updateResultsTable(double imageX, double imageY, double value, boolean dragging) {
        getMeasPrefs();
        if ((writeMiddleClickValuesTable && !dragging) || (writeMiddleDragValuesTable && dragging)) {
            if (table == null) {
                table = MeasurementTable.getTable(tableName);
                if (table == null) {
                    table = new MeasurementTable(tableName);;
                }
            }
            table.show();
            table.incrementCounter();
        }
        String lab = IJU.getSliceFilename(imp) + ",  Slice: " + imp.getSlice();
        if ((writeMiddleClickValuesTable && !dragging) || (writeMiddleDragValuesTable && dragging)) {
            if (showFileName) table.addLabel("Label", IJU.getSliceFilename(imp));
            if (showSliceNumber) table.addValue("Slice", imp.getSlice(), 0);
        }
        if (!dragging) {
            if (goodWCS) {
                if (showMeasureSex) {
                    if (writeMiddleClickValuesLog) lab += ",  RA: " + IJU.decToSexRA(radec[0]);
                    if (writeMiddleClickValuesLog) lab += ",  DEC: " + IJU.decToSexDec(radec[1]);
                } else {
                    if (writeMiddleClickValuesLog) lab += ",  RA (hrs): " + sixPlaces.format(radec[0] / 15.0);
                    if (writeMiddleClickValuesLog) lab += ",  DEC (deg): " + sixPlaces.format(radec[1]);
                }
                if (writeMiddleClickValuesTable) table.addValue("RA1 (hrs)", radec[0] / 15.0, 6);
                if (writeMiddleClickValuesTable) table.addValue("DEC1 (deg)", radec[1], 6);
            }
            if (writeMiddleClickValuesLog) lab += ",  X(IJ): " + twoPlaces.format(imageX);
            if (writeMiddleClickValuesLog) lab += ",  Y(IJ): " + twoPlaces.format(imageY);
            if (writeMiddleClickValuesLog)
                lab += ",  X(FITS): " + twoPlaces.format(imageX - Centroid.PIXELCENTER + 1.0);
            if (writeMiddleClickValuesLog)
                lab += ",  Y(FITS): " + twoPlaces.format((double) imp.getHeight() - imageY + Centroid.PIXELCENTER);
            if (writeMiddleClickValuesLog) lab += ",  PixValue: " + fourPlaces.format(value);
            if (writeMiddleClickValuesTable) table.addValue("X1(IJ)", imageX, 6);
            if (writeMiddleClickValuesTable) table.addValue("Y1(IJ)", imageY, 6);
            if (writeMiddleClickValuesTable) table.addValue("X1(FITS)", imageX - Centroid.PIXELCENTER + 1.0, 6);
            if (writeMiddleClickValuesTable)
                table.addValue("Y1(FITS)", (double) imp.getHeight() - imageY + Centroid.PIXELCENTER, 6);
            if (writeMiddleClickValuesTable) table.addValue("PixValue", value, 6);
            if (writeMiddleClickValuesTable && writePhotometricDataTable) writePhotometryToTable(photom1, cen, "1");
        }
        if (dragging) {
            if (goodWCS) {
                if (showMeasureSex) {
                    if (writeMiddleDragValuesLog && writeMeasureLengthLog)
                        lab += ",  ArcLength: " + (IJU.decToSexDeg(arcLength(), 2, false));
                } else {
                    if (writeMiddleDragValuesLog && writeMeasureLengthLog)
                        lab += ",  ArcLength: " + fourPlaces.format(arcLength()) + "\u00B0";
                }
                if (writeMiddleDragValuesTable && writeMeasureLengthTableDeg)
                    table.addValue("ArcLen (deg)", arcLength(), 6);
                if (writeMiddleDragValuesTable && writeMeasureLengthTableMin)
                    table.addValue("ArcLen (min)", arcLength() * 60.0, 6);
                if (writeMiddleDragValuesTable && writeMeasureLengthTableSec)
                    table.addValue("ArcLen (sec)", arcLength() * 3600.0, 6);
                if (writeMiddleDragValuesLog && writeMeasurePA)
                    lab += ",  PosAngle: " + fourPlaces.format(posAngle()) + "\u00B0";
                if (writeMiddleDragValuesTable && writeMeasurePA) table.addValue("PosAng (deg)", posAngle(), 6);
            } else {
                if (ac.XPixelScale > 0.0 && ac.YPixelScale > 0.0) {
                    double arclen = Math.sqrt(ac.XPixelScale * ac.XPixelScale * (imageX - startDragCenX) * (imageX - startDragCenX) +
                            ac.YPixelScale * ac.YPixelScale * (imageY - startDragCenY) * (imageY - startDragCenY)) / 3600.0;
                    if (showMeasureSex) {
                        if (writeMiddleDragValuesLog && writeMeasureLengthLog)
                            lab += ",  ArcLength: " + (IJU.decToSexDeg(arclen, 2, false));
                    } else {
                        if (writeMiddleDragValuesLog && writeMeasureLengthLog)
                            lab += ",  ArcLength: " + (fourPlaces.format(arclen)) + "\u00B0";
                    }
                    if (writeMiddleDragValuesTable && writeMeasureLengthTableDeg)
                        table.addValue("ArcLen (deg)", arclen, 6);
                    if (writeMiddleDragValuesTable && writeMeasureLengthTableMin)
                        table.addValue("ArcLen (min)", arclen * 60.0, 6);
                    if (writeMiddleDragValuesTable && writeMeasureLengthTableSec)
                        table.addValue("ArcLen (sec)", arclen * 3600.0, 6);
                    double angle = (360.0 + Math.atan2(ac.XPixelScale * (startDragCenX - imageX), ac.YPixelScale * (startDragCenY - imageY)) * 180.0 / Math.PI) % 360.0;
                    if (writeMiddleDragValuesLog && writeMeasurePA)
                        lab += ",  Y Angle: " + fourPlaces.format(angle) + "\u00B0";
                    if (writeMiddleDragValuesTable && writeMeasurePA) table.addValue("Y Angle (deg)", angle, 6);
                } else {
                    double len = Math.sqrt((imageX - startDragCenX) * (imageX - startDragCenX) + (imageY - startDragCenY) * (imageY - startDragCenY));
                    if (writeMiddleDragValuesLog && writeMeasureLengthLog)
                        lab += ",  Length: " + fourPlaces.format(len) + " (Pixels)";
                    if (writeMiddleDragValuesTable && (writeMeasureLengthTableDeg || writeMeasureLengthTableMin || writeMeasureLengthTableSec))
                        table.addValue("Length (pix)", len, 6);
                    double angle = (360.0 + Math.atan2((startDragCenX - imageX), (startDragCenY - imageY)) * 180.0 / Math.PI) % 360.0;
                    if (writeMiddleDragValuesLog && writeMeasurePA)
                        lab += ",  Y Angle: " + fourPlaces.format(angle) + "\u00B0";
                    if (writeMiddleDragValuesTable && writeMeasurePA) table.addValue("Y Angle (deg)", angle, 6);
                }
            }
            if (writeMiddleDragValuesLog && writeMeasureDelMag) lab += ",  " + getDelMagLabel();
            if (writeMiddleDragValuesTable && writeMeasureDelMag) table.addValue("Delta Mag", getDelMag(), 6);
            if (writeMiddleDragValuesLog && writeMeasureFluxRatio) lab += ",  " + getFluxRatioLabel();
            if (writeMiddleDragValuesTable && writeMeasureFluxRatio) table.addValue("Ratio (F2/F1)", getFluxRatio(), 6);
            if (goodWCS) {
                double angle = (360.0 + Math.atan2(ac.XPixelScale * (startDragCenX - imageX), ac.YPixelScale * (startDragCenY - imageY)) * 180.0 / Math.PI) % 360.0;
                if (writeMiddleDragValuesLog && writeMeasurePA)
                    lab += ",  Y Angle: " + fourPlaces.format(angle) + "\u00B0";
                if (writeMiddleDragValuesTable && writeMeasurePA) table.addValue("Y Angle (deg)", angle, 6);
            }
            if (writeMiddleDragValuesLog)
                lab += ",  ApRadius: " + twoPlaces.format(radius) + ",  ApSkyInner: " + twoPlaces.format(rBack1) + ",  ApSkyOuter: " + twoPlaces.format(rBack2);
            if (writeMiddleDragValuesTable && writePhotometricDataTable && showRadii) {
                table.addValue("ApRadius", radius, 6);
                table.addValue("ApSkyInner", rBack1, 6);
                table.addValue("ApSkyOuter", rBack2, 6);
            }
            if (goodWCS) {
                if (writeMiddleDragValuesLog) {
                    if (showMeasureSex) {
                        lab += ",  RA1: " + IJU.decToSexRA(startRadec[0]);
                        lab += ",  DEC1: " + IJU.decToSexDec(startRadec[1]);
                        lab += ",  RA2: " + IJU.decToSexRA(radec[0]);
                        lab += ",  DEC2: " + IJU.decToSexDec(radec[1]);
                    } else {
                        lab += ",  RA1 (hrs): " + sixPlaces.format(startRadec[0] / 15.0);
                        lab += ",  DEC1 (deg): " + sixPlaces.format(startRadec[1]);
                        lab += ",  RA2 (hrs): " + sixPlaces.format(radec[0] / 15.0);
                        lab += ",  DEC2 (deg): " + sixPlaces.format(radec[1]);
                    }
                }
                if (writeMiddleDragValuesTable && writePhotometricDataTable && showRADEC) {
                    table.addValue("RA1 (hrs)", startRadec[0] / 15.0, 6);
                    table.addValue("DEC1 (deg)", startRadec[1], 6);
                    table.addValue("RA2 (hrs)", radec[0] / 15.0, 6);
                    table.addValue("DEC2 (deg)", radec[1], 6);
                }
            }
            if (writeMiddleDragValuesLog) {
                lab += ",  X1(IJ): " + twoPlaces.format(startDragCenX);
                lab += ",  Y1(IJ): " + twoPlaces.format(startDragCenY);
                lab += ",  X2(IJ): " + twoPlaces.format(imageX);
                lab += ",  Y2(IJ): " + twoPlaces.format(imageY);
            }
            if (writeMiddleDragValuesTable && writePhotometricDataTable && showPosition) {
                table.addValue("X1(IJ)", startDragCenX, 6);
                table.addValue("Y1(IJ)", startDragCenY, 6);
                table.addValue("X2(IJ)", imageX, 6);
                table.addValue("Y2(IJ)", imageY, 6);
            }
            if (writeMiddleDragValuesLog) {
                lab += ",  X1(FITS): " + twoPlaces.format(startDragCenX - Centroid.PIXELCENTER + 1.0);
                lab += ",  Y1(FITS): " + twoPlaces.format((double) imp.getHeight() - startDragCenY + Centroid.PIXELCENTER);
                lab += ",  X2(FITS): " + twoPlaces.format(imageX - Centroid.PIXELCENTER + 1.0);
                lab += ",  Y2(FITS): " + twoPlaces.format((double) imp.getHeight() - imageY + Centroid.PIXELCENTER);
            }
            if (writeMiddleDragValuesTable && writePhotometricDataTable && showPositionFITS) {
                table.addValue("X1(FITS)", startDragCenX - Centroid.PIXELCENTER + 1.0, 6);
                table.addValue("Y1(FITS)", (double) imp.getHeight() - startDragCenY + Centroid.PIXELCENTER, 6);
                table.addValue("X2(FITS)", imageX - Centroid.PIXELCENTER + 1.0, 6);
                table.addValue("Y2(FITS)", (double) imp.getHeight() - imageY + Centroid.PIXELCENTER, 6);
            }
            if (writeMiddleDragValuesTable && writePhotometricDataTable) writePhotometryToTable(photom1, cen, "1");
            if (writeMiddleDragValuesTable && writePhotometricDataTable) writePhotometryToTable(photom2, endCen, "2");
        }
        if ((writeMiddleClickValuesLog && !dragging) || (writeMiddleDragValuesLog && dragging)) IJ.log(lab);
        if ((writeMiddleClickValuesTable && !dragging) || (writeMiddleDragValuesTable && dragging))
            table.show(tableName);
    }

    void getMeasPrefs() {
        showFits = Prefs.get("aperture.showfits", showFits);
        fitsKeywords = Prefs.get("aperture.fitskeywords", fitsKeywords);
        showPosition = Prefs.get("aperture.showposition", showPosition);
        showPositionFITS = Prefs.get("aperture.showpositionfits", showPositionFITS);
        showPhotometry = Prefs.get("aperture.showphotometry", showPhotometry);
        showNAperPixels = Prefs.get("aperture.shownaperpixels", showNAperPixels);
        showNBackPixels = Prefs.get("aperture.shownbackpixels", showNBackPixels);
        showBack = Prefs.get("aperture.showback", showBack);
        showFileName = Prefs.get("aperture.showfilename", showFileName);
        showSliceNumber = Prefs.get("aperture.showslicenumber", showSliceNumber);
        showPeak = Prefs.get("aperture.showpeak", showPeak);
        showMean = Prefs.get("aperture.showmean", showMean);
        showWidths = Prefs.get("aperture.showwidths", showWidths);
        showRadii = Prefs.get("aperture.showradii", showRadii);
        showTimes = Prefs.get("aperture.showtimes", showTimes);
        calcRadProFWHM = Prefs.get("aperture.calcradprofwhm", calcRadProFWHM);
        showMeanWidth = Prefs.get("aperture.showmeanwidth", showMeanWidth);
        showAngle = Prefs.get("aperture.showangle", showAngle);
        showRoundness = Prefs.get("aperture.showroundness", showRoundness);
        showVariance = Prefs.get("aperture.showvariance", showVariance);
        showErrors = Prefs.get("aperture.showerrors", showErrors);
        showSNR = Prefs.get("aperture.showsnr", showSNR);
        showRADEC = Prefs.get("aperture.showradec", showRADEC);

    }

    void writePhotometryToTable(Photometer phot, Centroid centroid, String suffix) {

        if (showPhotometry)
            table.addValue("Source_" + suffix, phot.source, 6);
        if (showNAperPixels)
            table.addValue("NAperPixels_" + suffix, phot.numberOfSourceAperturePixels(), 6);
        if (showPeak)
            table.addValue("Peak_" + suffix, phot.peakBrightness(), 6);
        if (showMean)
            table.addValue("Mean_" + suffix, phot.meanBrightness(), 6);
        if (showErrors)
            table.addValue("Source_Err_" + suffix, phot.serror, 6);
        if (showSNR)
            table.addValue("Source_SNR_" + suffix, phot.source / phot.serror, 6);
        if (showBack)
            table.addValue("Sky/Pix_" + suffix, phot.back, 6);
        if (showNBackPixels)
            table.addValue("NBackPixels_" + suffix, phot.numberOfBackgroundAperturePixels(), 6);
        if (showTimes) {
            var hdr = FitsJ.getHeader(imp);
            if (hdr != null) {
                double mjd = FitsJ.getMeanMJD(hdr);
                if (Double.isNaN(mjd)) mjd = FitsJ.getMJD(hdr);
                if (!Double.isNaN(mjd)) table.addValue("JD_UTC", mjd + 2400000.0, 6);
            }
        }
        if (calcRadProFWHM)
            table.addValue("FWHM_" + suffix, phot.getFWHM(), 6);
        if (showMeanWidth)
            table.addValue("Width_" + suffix, (centroid.xWidth + centroid.yWidth) / 2.0, 6);
        if (showWidths) {
            table.addValue("X-Width_" + suffix, centroid.xWidth, 6);
            table.addValue("Y-Width_" + suffix, centroid.yWidth, 6);
        }
        if (showAngle)
            table.addValue("Angle_" + suffix, centroid.angle, 6);
        if (showRoundness)
            table.addValue("Roundness_" + suffix, centroid.roundness(), 6);
        if (showVariance)
            table.addValue("Variance_" + suffix, centroid.variance, 6);
    }

    public boolean hasWCS() {
        return goodWCS;
    }

    public WCS getWCS() {
        return wcs;
    }

    public void updatePrefsFromWCS(boolean repaintoverlay) {
        if (wcs != null) {
            if (wcs.hasPA) {
                ac.NdirAngle = wcs.getNorthPA();
                ac.EdirAngle = wcs.getEastPA() - ac.NdirAngle;
                Prefs.set("Astronomy_Tool.NdirAngle", ac.NdirAngle);
                Prefs.set("Astronomy_Tool.EdirAngle", ac.EdirAngle);
            }
            if (wcs.hasScale) {
                pixelScaleX = wcs.getXScaleArcSec();
                pixelScaleY = wcs.getYScaleArcSec();
                Prefs.set("Astronomy_Tool.pixelScaleX", pixelScaleX);
                Prefs.set("Astronomy_Tool.pixelScaleY", pixelScaleY);
            }
            if (repaintoverlay) {
                ac.setShowPixelScale(showScaleX, showScaleY, pixelScaleX, pixelScaleY);
                ac.updateZoomBoxParameters();
                ac.paint(ac.getGraphics());
            }
        }
    }

    public void mouseWheelMoved(MouseWheelEvent e) {

        if (updatesEnabled) {
            int magChangeSteps = e.getWheelRotation();
            if (magChangeSteps == 0) return;
            updatesEnabled = false;
            int screenX = e.getX();
            int screenY = e.getY();
            ac.setMousePosition(screenX, screenY);
            double imageX = ac.offScreenXD(screenX);
            double imageY = ac.offScreenYD(screenY);
            //ImageProcessor ip = imp.getProcessor();
            //double value = ip.getPixelValue((int)imageX, (int)imageY);
            IJ.setInputEvent(e);
            if (e.isControlDown() && ((e.getModifiers() & MouseEvent.BUTTON3_MASK) == 0)) {
                adjustMinAndMax(magChangeSteps, 0);
            } else if (e.isShiftDown() && ((e.getModifiers() & MouseEvent.BUTTON3_MASK) == 0)) {
                adjustMinAndMax(0, -magChangeSteps);
            } else {
                zoomControl(screenX, screenY, magChangeSteps, WHEEL);
            }
        }
        updatesEnabled = true;
    }


    void adjustMinAndMax(int xSteps, int ySteps) {
        double low, high;
//                min = ip.getMin();
        blackValue = imp.getDisplayRangeMin();
//                max = ip.getMax();
        whiteValue = imp.getDisplayRangeMax();
        brightness = (whiteValue + blackValue) / 2.0;
        contrast = (whiteValue - blackValue) / 2.0;

        if (imp.getType() == ImagePlus.COLOR_RGB || imp.getType() == ImagePlus.COLOR_256 ||
                imp.getType() == ImagePlus.GRAY8) brightstepsize = 1.0;
        else if ((contrast < 2.0) && (imp.getType() == ImagePlus.GRAY32) &&
                (maxValue - minValue >= 10.0)) brightstepsize = 0.01;
        else if ((contrast < 2.0) && (imp.getType() == ImagePlus.GRAY32) &&
                (maxValue - minValue < 10.0)) brightstepsize = 0.001;
        else if ((contrast < 10.0) && (imp.getType() == ImagePlus.GRAY32)) brightstepsize = 0.2;
        else if (contrast < 500.0) brightstepsize = 2.0;
        else if (contrast < 10000.0) brightstepsize = 20.0;
        else brightstepsize = 200.0;


        contrast -= brightstepsize * (double) (xSteps);
        if (brightstepsize > 0.2 && contrast < 10.0 && imp.getType() == ImagePlus.GRAY32) {
            contrast = 9.8;
            brightstepsize = 0.2;
            ySteps = -1;
        } else if (brightstepsize > 0.01 && contrast < 2.0 && imp.getType() == ImagePlus.GRAY32) {
            contrast = 1.99;
            brightstepsize = 0.001;
            ySteps = -1;
        }
        brightness += brightstepsize * (double) (ySteps);

        if (contrast < brightstepsize)
            contrast = brightstepsize;
        else if ((contrast > (maxValue - minValue) / 2.0))
            contrast = (maxValue - minValue) / 2.0;

        low = (brightness - contrast < minValue) ? minValue : brightness - contrast;
        high = (brightness + contrast < minValue + brightstepsize) ? minValue + brightstepsize : brightness + contrast;
        if (high > maxValue) high = maxValue;
//                ip.setMinAndMax(low, high);
        Calibration cal = imp.getCalibration();
        imp.setDisplayRange(cal.getRawValue(blackValue), cal.getRawValue(whiteValue));
        minMaxChanged = true;
//                minSlider.setValue((int)((low + sliderShift)*(double)sliderMultiplier));
//                maxSlider.setValue((int)((high + sliderShift)*(double)sliderMultiplier));
        minMaxBiSlider.setColoredValues(low, high);
//                imp.updateAndDraw();
    }


    public void zoomControl(int screenX, int screenY, int magChangeSteps, boolean mouseClick) {
        ac.setMousePosition(screenX, screenY);
        double imageX = ac.offScreenXD(screenX);
        double imageY = ac.offScreenYD(screenY);
        double zoom = ac.getMagnification();
        double nextzoom = ac.getLowerZoomLevel(zoom);

        ipWidth = imp.getWidth();
        ipHeight = imp.getHeight();
        icWidth = ac.getWidth();
        icHeight = ac.getHeight();
        if (magChangeSteps > 0) //&& (zoom > initialHighMag))     //zoom out
        {
            zoomOut((int) (screenX), (int) (screenY), mouseClick, false);
        } else if (magChangeSteps < 0)    //zoom in
        {
            zoomIn((int) (screenX), (int) (screenY), mouseClick, false, 0.0);
        } else if (mouseClick) // and magChangeSteps==0
        {
            adjustSourceRect(zoom, screenX, screenY, true);
        }
//                try
//                        {
//                        Robot robot = new Robot();     //move mouse pointer to new position on screen
//                        robot.mouseMove(ac.getLocationOnScreen().x +ac.screenXD(Math.round(imageX+0.499)),
//                                        ac.getLocationOnScreen().y + ac.screenYD(Math.round(imageY+0.499)));
//                        }
//                catch (AWTException ee)
//                        {
//                        ee.printStackTrace();
//                        }
    }

    public void zoomIn(int screenX, int screenY, boolean mouseClick, boolean center, double factor) {
        magnification = ac.getMagnification();
        if (magnification >= 128) return;
        double newMag = magnification;
        if (mouseClick) {
            if (factor > 0.0)
                newMag = magnification * factor;
            else
                newMag = ac.getHigherZoomLevel(magnification);
        } else
            newMag = magnification * 1.1;
        if (newMag >= 128) newMag = 128;
        adjustSourceRect(newMag, screenX, screenY, center);
    }

    public void zoomOut(int screenX, int screenY, boolean mouseClick, boolean center) {
        magnification = ac.getMagnification();
        if (magnification <= 0.03125) return;
        double newMag = magnification;
        if (mouseClick)
            newMag = ac.getLowerZoomLevel(magnification);
        else
            newMag = magnification / 1.1;
        if (newMag <= 0.03125) newMag = 0.03125;
        adjustSourceRect(newMag, screenX, screenY, center);
    }


    void adjustSourceRect(double newMag, int screenX, int screenY, boolean center) {
        ac.setMousePosition(screenX, screenY);
        icWidth = ac.getWidth();
        icHeight = ac.getHeight();
        double w = (double) icWidth / newMag;
        if (w * newMag < icWidth) w++;
        double h = (double) icHeight / newMag;
        if (h * newMag < icHeight) h++;

        double xSign = netFlipX ? 1.0 : -1.0;
        double ySign = netFlipY ? 1.0 : -1.0;


        double offx = ac.offScreenXD(screenX);
        double offy = ac.offScreenYD(screenY);

        Rectangle r = ac.getSrcRect();

        if (center && newClick) {
            r.x = (int) (offx - w / 2.0);
            r.y = (int) (offy - h / 2.0);
            r.width = (int) w;
            r.height = (int) h;
            newClick = false;
        } else if (center && !newClick) {
            r.x = (int) (r.x + r.width / 2.0 - w / 2.0);
            r.y = (int) (r.y + r.height / 2.0 - h / 2.0);
            r.width = (int) w;
            r.height = (int) h;
        } else {
            if (offx < 0) {
                offx = 0;
                screenX = ac.screenXD(offx);
            }
            if (offx > imp.getWidth()) {
                offx = imp.getWidth() - 1;
                screenX = ac.screenXD(offx + 2);
            }
            if (offy < 0) {
                offy = 0;
                screenY = ac.screenYD(offy);
            }
            if (offy > imp.getHeight()) {
                offy = imp.getHeight() - 1;
                screenY = ac.screenYD(offy + 2);
            }
            double offsetX = netFlipX ? 0.499 : 0.499;
            double offsetY = netFlipY ? 0.499 : 0.499;
            r = new Rectangle((int) (offsetX + offx - w / 2.0 * (1.0 + xSign * ((double) icWidth / 2.0 - (double) screenX) / ((double) icWidth / 2.0))),
                    (int) (offsetY + offy - h / 2.0 * (1.0 + ySign * ((double) icHeight / 2.0 - (double) screenY) / ((double) icHeight / 2.0))),
                    (int) w, (int) h);
        }

//        if (ac.getNetRotate())
//                  r = new Rectangle((int)(offsetY + offy-h/2.0*(1.0 + xSign*((double)icHeight/2.0 - (double)screenY)/((double)icHeight/2.0))),
//                                    (int)(offsetX + offx-w/2.0*(1.0 + ySign*((double)icWidth/2.0 - (double)screenX)/((double)icWidth/2.0))),
//                                    (int) h, (int) w);


        ac.setMagnification(newMag);
        ac.setSourceRect(r);


        savedPanX = r.x;
        savedPanY = r.y;
        savedPanHeight = r.height;
        savedPanWidth = r.width;
        savedMag = ac.getMagnification();
        savedIpWidth = imp.getWidth();
        savedIpHeight = imp.getHeight();
        Prefs.set("Astronomy_Tool.savedPanX", savedPanX);
        Prefs.set("Astronomy_Tool.savedPanY", savedPanY);
        Prefs.set("Astronomy_Tool.savedPanHeight", savedPanHeight);
        Prefs.set("Astronomy_Tool.savedPanWidth", savedPanWidth);
        Prefs.set("Astronomy_Tool.savedIpWidth", savedIpWidth);
        Prefs.set("Astronomy_Tool.savedIpHeight", savedIpHeight);
        setImageEdges();
//        if (r.x<=0 || r.y<=0 || r.x+r.width>=ipWidth || r.y+h>=ipHeight)
//            {
//            clearAndPaint();
//            }
//        else
//            ac.repaint();
        ac.paint(ac.getGraphics());
        prevMag = ac.getMagnification();
    }


    void setImageEdges() {
        imageEdgeX1 = ac.screenX(0) > 0 ? (int) photom.rBack2 : ac.getSrcRect().x;
        imageEdgeY1 = ac.screenY(0) > 0 ? (int) photom.rBack2 : ac.getSrcRect().y;
        imageEdgeX2 = ac.screenX(imp.getWidth()) < ac.getWidth() ? imp.getWidth() - (int) photom.rBack2 : ac.getSrcRect().x + ac.getSrcRect().width;
        imageEdgeY2 = ac.screenY(imp.getHeight()) < ac.getHeight() ? imp.getHeight() - (int) photom.rBack2 : ac.getSrcRect().y + ac.getSrcRect().height;
    }

//    public void fitToAstroWindow() {
//        ipWidth = imp.getWidth();
//        ipHeight = imp.getHeight();
//        icWidth = ac.getWidth();
//        icHeight = ac.getHeight();
//        Rectangle srcRect = new Rectangle(ac.getSrcRect());
//		ImageWindow win = imp.getWindow();
//		if (win==null) return;
//		Rectangle bounds = win.getBounds();
//		Insets insets = win.getInsets();
//		int sliderHeight = (win instanceof StackWindow)?20:0;
//		double xmag = (double)(bounds.width-10)/srcRect.width;
//		double ymag = (double)(bounds.height-(10+insets.top+insets.bottom+otherPanelsHeight+sliderHeight))/srcRect.height;
//		ac.setMagnification(Math.min(xmag, ymag));
//		int width=(int)(ipWidth*magnification);
//		int height=(int)(ipHeight*magnification);
//		if (width==bounds.width-10&&height==bounds.height-(10+insets.top+insets.bottom+otherPanelsHeight+sliderHeight)) return;
//		srcRect=new Rectangle(0,0,ipWidth, ipHeight);
//        ac.setSourceRect(srcRect);
//		ac.setDrawingSize(width, height);
//        savedPanX = 0;
//        savedPanY = 0;
//        savedPanHeight = ipHeight;
//        savedPanWidth = ipWidth;
//        savedMag = ac.getMagnification();
//        savedICWidth = width;
//        savedICHeight = height;
//        Prefs.set("Astronomy_Tool.savedPanX", savedPanX);
//        Prefs.set("Astronomy_Tool.savedPanY", savedPanY);
//        Prefs.set("Astronomy_Tool.savedPanHeight", savedPanHeight);
//        Prefs.set("Astronomy_Tool.savedPanWidth", savedPanWidth);
//        Prefs.set("Astronomy_Tool.savedMag", savedMag);
//        Prefs.set("Astronomy_Tool.savedICWidth", savedICWidth);
//        Prefs.set("Astronomy_Tool.savedICHeight", savedICHeight);
//        buildAstroWindow();
////        layoutContainer(imageWindow);
////        ac.repaint();
////		openFrame.doLayout();
////        openFrame.pack();
//	}

    public String hms(double d, int fractionPlaces) {
        DecimalFormat nf = new DecimalFormat();
        DecimalFormat nf22 = new DecimalFormat();
        DecimalFormat nf23 = new DecimalFormat();
        nf.setDecimalFormatSymbols(IJU.dfs);
        nf22.setDecimalFormatSymbols(IJU.dfs);
        nf23.setDecimalFormatSymbols(IJU.dfs);
        nf.setMinimumIntegerDigits(2);
        nf23.setMinimumIntegerDigits(2);
        nf23.setMinimumFractionDigits(3);
        nf23.setMaximumFractionDigits(3);
        nf22.setMinimumIntegerDigits(2);
        nf22.setMinimumFractionDigits(2);
        nf22.setMaximumFractionDigits(2);
        double dd = Math.abs(d);
        int h = (int) dd;
        int m = (int) (60.0 * (dd - (double) h));
        double s = 3600.0 * (dd - (double) h - (double) m / 60.0);

        String str = "";
        if (d < 0.0) str = "-";
        str += "" + nf.format(h) + ":" + nf.format(m) + ":";
        if (fractionPlaces == 2)
            str += nf22.format(s);
        else
            str += nf23.format(s);
        return str;
    }

    double[] processCoordinatePair(JTextField textFieldA, int decimalPlacesA, int baseA, boolean showSignA,
                                   JTextField textFieldB, int decimalPlacesB, int baseB, boolean showSignB, boolean AIsSource, boolean update) {
        double X = Double.NaN;
        double Y = Double.NaN;
        int YstartPosition = 1;

        boolean XNegative = false;
        boolean YNegative = false;
        String text = AIsSource ? textFieldA.getText() : textFieldB.getText();
        String[] pieces = text.replaceAll("[\\-][^0-9\\.]{0,}", " \\-").replaceAll("[+][^0-9\\.]{0,}", " +").replaceAll("[^0-9\\.\\-+]{1,}", " ").trim().split("[^0-9\\.\\-+]{1,}");
//        for (int i=0; i<pieces.length; i++)
//            {
//            IJ.log(""+pieces[i]);
//            }

        if (pieces.length > 0) {
            X = Tools.parseDouble(pieces[0]);
            if (!Double.isNaN(X) && pieces[0].contains("-")) {
                X = -X;
                XNegative = true;
            }
            if (pieces.length > 1 && !pieces[1].contains("-") && !pieces[1].contains("+")) {
                X += Math.abs(Tools.parseDouble(pieces[1])) / 60.0;
                YstartPosition = 2;
                if (pieces.length > 2 && !pieces[2].contains("-") && !pieces[2].contains("+")) {
                    X += Math.abs(Tools.parseDouble(pieces[2])) / 3600.0;
                    YstartPosition = 3;
                }
            }
        }
        if (pieces.length > YstartPosition) {
            Y = Tools.parseDouble(pieces[YstartPosition]);
            if (!Double.isNaN(Y) && pieces[YstartPosition].contains("-")) {
                Y = -Y;
                YNegative = true;
            }
            if (pieces.length > YstartPosition + 1) Y += Math.abs(Tools.parseDouble(pieces[YstartPosition + 1])) / 60.0;
            if (pieces.length > YstartPosition + 2)
                Y += Math.abs(Tools.parseDouble(pieces[YstartPosition + 2])) / 3600.0;
        } else if (pieces.length > 0 && AIsSource) {
            text = textFieldB.getText();
            pieces = text.replaceAll("[\\-][^0-9\\.]{0,}", " \\-").replaceAll("[+][^0-9\\.]{0,}", " +").replaceAll("[^0-9\\.\\-+]{1,}", " ").trim().split("[^0-9\\.\\-+]{1,}");
            if (pieces.length > 0) {
                Y = Tools.parseDouble(pieces[0]);
                if (!Double.isNaN(Y) && pieces[0].contains("-")) {
                    Y = -Y;
                    YNegative = true;
                }
                if (pieces.length > 1 && !pieces[1].contains("-") && !pieces[1].contains("+")) {
                    Y += Math.abs(Tools.parseDouble(pieces[1])) / 60.0;
                    if (pieces.length > 2 && !pieces[2].contains("-") && !pieces[2].contains("+")) {
                        Y += Math.abs(Tools.parseDouble(pieces[2])) / 3600.0;
                    }
                }
            }
        } else if (pieces.length > 0 && !AIsSource) {
            Y = X;
            YNegative = XNegative;
            X = Double.NaN;
            XNegative = false;
            text = textFieldA.getText();
            pieces = text.replaceAll("[\\-][^0-9\\.]{0,}", " \\-").replaceAll("[+][^0-9\\.]{0,}", " +").replaceAll("[^0-9\\.\\-+]{1,}", " ").trim().split("[^0-9\\.\\-+]{1,}");
            if (pieces.length > 0) {
                X = Tools.parseDouble(pieces[0]);
                if (!Double.isNaN(X) && pieces[0].contains("-")) {
                    X = -X;
                    XNegative = true;
                }
                if (pieces.length > 1 && !pieces[1].contains("-") && !pieces[1].contains("+")) {
                    X += Math.abs(Tools.parseDouble(pieces[1])) / 60.0;
                    if (pieces.length > 2 && !pieces[2].contains("-") && !pieces[2].contains("+")) {
                        X += Math.abs(Tools.parseDouble(pieces[2])) / 3600.0;
                    }
                }
            }
        }

        X = mapToBase(X, baseA, XNegative);
        if (!Double.isNaN(X) && update)
            textFieldA.setText(useSexagesimal ? decToSex(X, decimalPlacesA, baseA, showSignA) : sixPlaces.format(X));
        Y = mapToBase(Y, baseB, YNegative);
        if (!Double.isNaN(Y) && update)
            textFieldB.setText(useSexagesimal ? decToSex(Y, decimalPlacesB, baseB, showSignB) : sixPlaces.format(Y));
        return new double[]{X, Y};
    }

    public double sexToDec(String text, int base) {
        double X = Double.NaN;
        boolean XNegative = false;
        String[] pieces = text.replace("-", " -").replaceAll("[^0-9\\.\\-]{1,}", " ").trim().split("[^0-9\\.\\-]{1,}");
        if (pieces.length > 0) {
            X = Tools.parseDouble(pieces[0]);
            if (!Double.isNaN(X) && pieces[0].contains("-")) {
                X = -X;
                XNegative = true;
            }
            if (pieces.length > 1) X += Math.abs(Tools.parseDouble(pieces[1])) / 60.0;
            if (pieces.length > 2) X += Math.abs(Tools.parseDouble(pieces[2])) / 3600.0;
        }

        X = mapToBase(X, base, XNegative);
        return X;
    }

    public String decToSex(double d, int fractionPlaces, int base, Boolean showPlus) {
        DecimalFormat nf = new DecimalFormat();
        DecimalFormat nf2x = new DecimalFormat();
        nf.setMinimumIntegerDigits(2);
        nf.setDecimalFormatSymbols(IJU.dfs);
        nf2x.setMinimumIntegerDigits(2);
        nf2x.setMinimumFractionDigits(0);
        nf2x.setMaximumFractionDigits(fractionPlaces);
        nf2x.setDecimalFormatSymbols(IJU.dfs);

        boolean ampm = false;
        boolean pm = false;
        if (base == 1224) {
//            base = 12;
            ampm = true;
            if (d >= 12.0) {
                d -= 12.0;
                pm = true;
            }
        }

        double dd = Math.abs(d);
//        dd += 0.0000001;

        int h = (int) dd;
        int m = (int) (60.0 * (dd - (double) h));
        double s = 3600.0 * (dd - (double) h - (double) m / 60.0);

        if (Tools.parseDouble(nf2x.format(s)) >= 60.0) {
            s = 0.0;
            m += 1;
        }
        if (m > 59) {
            m -= 60;
            h += 1;
        }
        if (d > 0 && h >= base) {
            if (base == 180 || (base == 12 && !ampm)) {
                d = -d;
                if (s != 0) {
                    s = 60 - s;
                    m = 59 - m;
                    h--;
                } else if (m != 0) {
                    m = 59 - m;
                    h--;
                }
            } else if (base == 12 && ampm) {
                h -= base;
                pm = !pm;
            } else if (base == 90) {
                h = 90;
                m = 0;
                s = 0;
            } else
                h -= base;
        } else if (base == 90 && d < -90) {
            h = 90;
            m = 0;
            s = 0;
        }

        if (ampm && h == 0) h = 12;
        String str = "";
        if (d < 0.0) str = "-";
        else if (showPlus) str = "+";
        str += "" + nf.format(h) + ":" + nf.format(m) + ":" + nf2x.format(s);
        if (ampm) str += pm ? " PM" : " AM";
        return str;
    }


    double mapToBase(double num, int base, boolean negative) {
        double x = num;
        if (base == 90)
            x = x >= 90 ? (negative ? -90 : 90) : (negative ? -x : x);  //-89.999722 : 89.999722
        else if (base == 180 || base == 12) {
            x %= 2 * base;
            x = x > base ? -2 * base + x : x;
            x = negative ? -x : x;
        } else {
            x %= base;
            x = negative ? base - x : x;
        }
        return x;
    }

    public double getSIMBADSearchRadius() {
        return simbadSearchRadius;
    }

    void editApertureRoi(ApertureRoi roi) {
        if (roi == null) return;
        GenericDialog gd = new GenericDialog("Edit Aperture", getX() + getWidth() / 2 - 165, getY() + getHeight() / 2 - 77);
        gd.enableYesNoCancel("Save", "Delete");
        gd.addCheckbox("Display Centroid Crosshair", roi != null && roi.getIsCentroid());
        gd.addNumericField("Aperture Radius:", roi.getRadius(), 6, 20, "(pixels)");
        gd.addNumericField("Background Inner Radius:", roi.getBack1(), 6, 20, "(pixels)");
        gd.addNumericField("Background Outer Radius:", roi.getBack2(), 6, 20, "(pixels)");
        gd.addNumericField("Aperture center X location:", roi.getXpos(), 6, 20, "(IJ Pixel coordinate)");
        gd.addNumericField("Aperture center Y location:", roi.getYpos(), 6, 20, "(IJ Pixel coordinate)");
        gd.addStringField("Aperture Name Text:", roi.getName(), 40);
        gd.addNumericField("Integrated Counts:", roi.getIntCnts(), 6, 20, "(Total ADU)");
        gd.addNumericField("Apparent Magnitude", roi.getAMag(), 6, 20, "(values>99.00 show as blank)");
        gd.addChoice("Aperture Color", colors, IJU.colorNameOf(roi.getApColor()));
        gd.showDialog();

        if (gd.wasCanceled()) return;
        if (gd.wasOKed()) {
            roi.setIsCentroid(gd.getNextBoolean());
            roi.setRadius(gd.getNextNumber());
            roi.setBack1(gd.getNextNumber());
            roi.setBack2(gd.getNextNumber());
            roi.setXpos(gd.getNextNumber());
            roi.setYpos(gd.getNextNumber());
            roi.setName(gd.getNextString().trim());
            roi.setIntCnts(gd.getNextNumber());
            roi.setAMag(gd.getNextNumber());
            roi.setApColor(IJU.colorOf(gd.getNextChoice()));
        } else {
            OverlayCanvas.getOverlayCanvas(imp).removeApertureRoi(roi);
        }
        IJU.updateApertures(imp);
        ac.repaint();
    }

    void editMeasurementRoi(MeasurementRoi roi) {
        GenericDialog gd = new GenericDialog("Edit Measurement", getX() + getWidth() / 2 - 165, getY() + getHeight() / 2 - 77);
        gd.enableYesNoCancel("Save", "Delete");
        gd.addCheckboxGroup(2, 4, new String[]{"Display ArcLen", "Display PA", "Display \u0394Mag", "Display F2/F1",
                        "Start Crosshair", "End Crosshair", "Display Apertures"},
                new boolean[]{roi.getShowLength(), roi.getShowPA(), roi.getShowDelMag(), roi.getShowFluxRatio(),
                        roi.getShowCentroid1(), roi.getShowCentroid2(), roi.getShowCircle()});
        gd.addNumericField("Circle Radius:", roi.getRadius(), 3, 9, "(pixels)");
        gd.addStringField("ArcLen Text:", roi.getLengthLabel(), 40);
        gd.addStringField("PA Text:", roi.getPALabel(), 40);
        gd.addStringField("\u0394Mag Text:", roi.getDelMagLabel(), 40);
        gd.addStringField("F2/F1 Text:", roi.getFluxRatioLabel(), 40);
        gd.addCheckbox("Display text on multiple lines (if needed)", roi.getShowMultiLines());
        gd.addChoice("Color:", colors, IJU.colorNameOf(roi.getRioColor()));
        gd.showDialog();

        if (gd.wasCanceled()) return;
        if (gd.wasOKed()) {
            roi.setShowLength(gd.getNextBoolean());
            roi.setShowPA(gd.getNextBoolean());
            roi.setShowDelMag(gd.getNextBoolean());
            roi.setShowFluxRatio(gd.getNextBoolean());
            roi.setShowCentroid1(gd.getNextBoolean());
            roi.setShowCentroid2(gd.getNextBoolean());
            roi.setShowCircle(gd.getNextBoolean());
            roi.setRadius(gd.getNextNumber());
            roi.setLengthLabel(gd.getNextString().trim());
            roi.setPALabel(gd.getNextString().trim());
            roi.setDelMagLabel(gd.getNextString().trim());
            roi.setFluxRatioLabel(gd.getNextString().trim());
            roi.setShowMultiLines(gd.getNextBoolean());
            roi.setRoiColor(IJU.colorOf(gd.getNextChoice()));
        } else {
            OverlayCanvas.getOverlayCanvas(imp).removeMeasurementRoi(roi);
        }
        ac.repaint();
    }

    void setDefaultMeasurementColor() {
        GenericDialog gd = new GenericDialog("Default Measurement Color", getX() + getWidth() / 2 - 165, getY() + getHeight() / 2 - 77);
        gd.addChoice("Default Measurement Color", colors, defaultMeasurementColor);
        gd.showDialog();
        if (gd.wasCanceled()) return;
        defaultMeasurementColor = gd.getNextChoice();
        Prefs.set("Astronomy_Tool.defaultMeasurementColor", defaultMeasurementColor);
        defaultMeasurementColorMenuItem.setLabel("Set default measurement color (currently '" + defaultMeasurementColor + "')...");
    }

    void setDefaultAnnotationColor() {
        GenericDialog gd = new GenericDialog("Default Annotation Color", getX() + getWidth() / 2 - 165, getY() + getHeight() / 2 - 77);
        gd.addChoice("Default Annotation Color", colors, defaultAnnotationColor);
        gd.showDialog();
        if (gd.wasCanceled()) return;
        defaultAnnotationColor = gd.getNextChoice();
        Prefs.set("Astronomy_Tool.defaultAnnotationColor", defaultAnnotationColor);
        defaultAnnotationColorMenuItem.setLabel("Set default annotation color (currently '" + defaultAnnotationColor + "')...");
    }

    void displayAnnotation(double[] pixel) {
        String coordsText = "";
        List<String> objectList = new ArrayList<String>();
        List<String> arcsecList = new ArrayList<String>();
        String label = "";
        backPlane = Prefs.get("aperture.backplane", backPlane);
        if (buttonCentroid.isSelected() && cen.measure(imp, pixel[0], pixel[1], radius, rBack1, rBack2, buttonCentroid.isSelected(), backPlane, removeBackStars)) {
            pixel[0] = cen.xCenter;
            pixel[1] = cen.yCenter;
        }
        double[] coords = {Double.NaN, Double.NaN};
        if (goodWCS) coords = wcs.pixels2wcs(pixel);
        if (showInSimbad && goodWCS && !Double.isNaN(coords[0]) && !Double.isNaN(coords[1])) {
            simbadSearchRadius = Prefs.get("Astronomy_Tool.simbadSearchRadius", simbadSearchRadius);
            IJU.showInSIMBAD(coords[0], coords[1], simbadSearchRadius);
        }
        if (rightClickAnnotate) {
            if (useSimbadSearch && goodWCS && !Double.isNaN(coords[0]) && !Double.isNaN(coords[1])) {
                coordsText = hms(coords[0] / 15.0, 3) + ((coords[1] > 0.0) ? "+" : "") + hms(coords[1], 2);
                boolean useHarvard = Prefs.get("coords.useHarvard", false);
                extraInfo = " (" + wcs.coordsys + ")  Accessing SIMBAD...";
                repaint();
                try {
                    simbadSearchRadius = Prefs.get("Astronomy_Tool.simbadSearchRadius", simbadSearchRadius);
                    String objectCoords = URLEncoder.encode(coordsText, "UTF-8");
                    URL simbad;
                    if (useHarvard)
                        simbad = new URL("http://simbad.cfa.harvard.edu/simbad/sim-coo?Coord=" + objectCoords + "&Radius=" + simbadSearchRadius + "&Radius.unit=arcsec&output.format=ASCII");
                    else
                        simbad = new URL("http://simbad.u-strasbg.fr/simbad/sim-coo?Coord=" + objectCoords + "&Radius=" + simbadSearchRadius + "&Radius.unit=arcsec&output.format=ASCII");

                    URLConnection simbadCon;
                    if (Prefs.get("coords.useProxy", false)) {
                        SocketAddress socketaddr = new InetSocketAddress(Prefs.get("coords.proxyAddress", "proxyserver.mydomain.com"), (int) Prefs.get("coords.proxyPort", 8080));
                        java.net.Proxy proxy = new java.net.Proxy(java.net.Proxy.Type.HTTP, socketaddr);
                        simbadCon = simbad.openConnection(proxy);
                    } else simbadCon = simbad.openConnection();
                    simbadCon.setConnectTimeout(10000);
                    simbadCon.setReadTimeout(10000);
                    BufferedReader in = new BufferedReader(new InputStreamReader(simbadCon.getInputStream()));
                    String inputLine;

                    while ((inputLine = in.readLine()) != null) {
                        //                        IJ.log(inputLine);
                        if (inputLine.startsWith("!! No astronomical object found : ")) {
                            break;
                        }
                        if (inputLine.startsWith("!!")) {
                            IJ.showMessage("SIMBAD query error", inputLine.trim());
                            break;
                        }
                        if (inputLine.contains("Number of objects :")) {
                            int num = parseInteger(inputLine.substring(inputLine.indexOf(":") + 1).trim());
                            //                            IJ.log(inputLine.substring(inputLine.indexOf(":")+1).trim());
                            //                            IJ.log(""+num);
                            if (num > 0) {
                                inputLine = in.readLine();
                                inputLine = in.readLine();
                                inputLine = in.readLine();

                                for (int i = 0; i < num; i++) {
                                    inputLine = in.readLine();
                                    if (inputLine == null) break;
                                    String[] columns = inputLine.split("\\|");
                                    if (columns.length > 2) {
                                        objectList.add(columns[2].trim());
                                        arcsecList.add(columns[1].trim());
                                    }
                                }
                            }
                            break;
                        }
                        if (inputLine.startsWith("Object")) {
                            objectList.add(inputLine.substring(7, inputLine.indexOf("---")).trim());
                            arcsecList.add("");
                            //                            while ((inputLine = in.readLine()) != null)
                            //                                {
                            //                                IJ.log(inputLine);
                            //                                }
                            break;
                        }

                    }

                    in.close();
                } catch (IOException ioe) {
                    IJ.showMessage("SIMBAD query error", "<html>" + "Could not open link to Simbad " + (useHarvard ? "at Harvard." : "in France.") + "<br>" +
                            "Check internet connection or proxy settings or" + "<br>" +
                            "try " + (useHarvard ? "France" : "Harvard") + " server (see Coordinate Converter Network menu)." + "</html>");

                }
                extraInfo = " (" + wcs.coordsys + ")";
                repaint();
            }

            GenericDialog gd = new GenericDialog("Select Annotation Text", getX() + getWidth() / 2 - 165, getY() + getHeight() / 2 - 77);

            gd.addMessage("Select object description or enter custom text.");
            if (!coordsText.equals("")) {
                objectList.add(coordsText);
                arcsecList.add("");
            }
            objectList.add("(" + uptoFourPlaces.format(pixel[0]) + ", " + uptoFourPlaces.format(pixel[1]) + ")");
            arcsecList.add("");
            String[] names = new String[objectList.size()];
            for (int i = 0; i < objectList.size(); i++) {
                names[i] = objectList.get(i) + (arcsecList.get(i).equals("") ? "" : "  (" + arcsecList.get(i) + " arcsec)");
            }

            gd.addChoice("Selection:", names, names[0]);
            gd.addStringField("Custom Text:", "", 40);
            useSimbadSearch = Prefs.get("Astronomy_Tool.useSimbadSearch", useSimbadSearch);
            showAnnotateCircle = Prefs.get("Astronomy_Tool.showAnnotateCircle", showAnnotateCircle);
            showInSimbad = Prefs.get("Astronomy_Tool.showInSimbad", showInSimbad);
            showAnnotateCrosshair = Prefs.get("Astronomy_Tool.showAnnotateCrosshair", showAnnotateCrosshair);
            simbadSearchRadius = Prefs.get("Astronomy_Tool.simbadSearchRadius", simbadSearchRadius);
            annotateCircleRadius = Prefs.get("Astronomy_Tool.annotateCircleRadius", annotateCircleRadius);
            gd.addCheckboxGroup(2, 2, new String[]{"Search SIMBAD (next time)", "Show Circle", "Show in SIMBAD (next time)", "Show Crosshair"},
                    new boolean[]{useSimbadSearch, showAnnotateCircle, showInSimbad, showAnnotateCrosshair});//addCheckbox("Show Circle", showAnnotateCircle);
            //            gd.addCheckbox(, useSimbadSearch);

            gd.addNumericField("Search Radius:", simbadSearchRadius, 3, 9, "(arcsec)");
            gd.addNumericField("Circle Radius:", annotateCircleRadius, 3, 9, "(pixels)");
            gd.addMessage("Change SIMBAD network parameters in Coordinate Converter");
            gd.showDialog();

            if (gd.wasCanceled()) return;


            useSimbadSearch = gd.getNextBoolean();
            useSimbadSearchCB.setState(useSimbadSearch);
            Prefs.set("Astronomy_Tool.useSimbadSearch", useSimbadSearch);
            showAnnotateCircle = gd.getNextBoolean();
            Prefs.set("Astronomy_Tool.showAnnotateCircle", showAnnotateCircle);
            showInSimbad = gd.getNextBoolean();
            showInSimbadCB.setState(showInSimbad);
            Prefs.set("Astronomy_Tool.showInSimbad", showInSimbad);
            showAnnotateCrosshair = gd.getNextBoolean();
            Prefs.set("Astronomy_Tool.showAnnotateCrosshair", showAnnotateCrosshair);
            simbadSearchRadius = gd.getNextNumber();
            Prefs.set("Astronomy_Tool.simbadSearchRadius", simbadSearchRadius);
            simbadSearchRadiusMenuItem.setLabel("Set SIMBAD search radius (currently " + simbadSearchRadius + " arcsec)...");
            annotateCircleRadius = gd.getNextNumber();
            Prefs.set("Astronomy_Tool.annotateCircleRadius", annotateCircleRadius);
            label = gd.getNextString();
            if (label.trim().equals("")) {
                label = objectList.get(gd.getNextChoiceIndex());
            }

            addAnnotateRoi(imp, showAnnotateCircle, showAnnotateCrosshair, true, false, pixel[0], pixel[1], annotateCircleRadius, label, IJU.colorOf(defaultAnnotationColor), false);
            ac.showAnnotations = true;
            buttonShowAnnotations.setSelected(true);
            ac.repaint();
        }
    }


    void editAnnotateRoi(AnnotateRoi roi) {
        String coordsText = "";
        String col = "orange";
        double[] pixel = new double[2];
        pixel[0] = roi.getXpos();
        pixel[1] = roi.getYpos();
        List<String> objectList = new ArrayList<String>();
        List<String> arcsecList = new ArrayList<String>();
        String label = roi.getLabel();
        boolean showCircle = roi.getShowCircle();
        boolean showCrosshair = roi.getShowCentroid();
        double radius = roi.getRadius();
        simbadSearchRadius = Prefs.get("Astronomy_Tool.simbadSearchRadius", simbadSearchRadius);
        useSimbadSearch = Prefs.get("Astronomy_Tool.useSimbadSearch", useSimbadSearch);
        showInSimbad = Prefs.get("Astronomy_Tool.showInSimbad", showInSimbad);
        double[] coords = {Double.NaN, Double.NaN};
        if (goodWCS) coords = wcs.pixels2wcs(pixel);

        if (showInSimbad && goodWCS && !Double.isNaN(coords[0]) && !Double.isNaN(coords[1])) {
            IJU.showInSIMBAD(coords[0], coords[1], simbadSearchRadius);
        }

        if (goodWCS && !Double.isNaN(coords[0]) && !Double.isNaN(coords[1])) {
            coordsText = hms(coords[0] / 15.0, 3) + ((coords[1] > 0.0) ? "+" : "") + hms(coords[1], 2);
            if (useSimbadSearch) {
                boolean useHarvard = Prefs.get("coords.useHarvard", false);
                extraInfo = " (" + wcs.coordsys + ")  Accessing SIMBAD...";
                repaint();
                try {
                    String objectCoords = URLEncoder.encode(coordsText, "UTF-8");
                    URL simbad;
                    if (useHarvard)
                        simbad = new URL("http://simbad.cfa.harvard.edu/simbad/sim-coo?Coord=" + objectCoords + "&Radius=" + simbadSearchRadius + "&Radius.unit=arcsec&output.format=ASCII");
                    else
                        simbad = new URL("http://simbad.u-strasbg.fr/simbad/sim-coo?Coord=" + objectCoords + "&Radius=" + simbadSearchRadius + "&Radius.unit=arcsec&output.format=ASCII");

                    URLConnection simbadCon;
                    if (Prefs.get("coords.useProxy", false)) {
                        SocketAddress socketaddr = new InetSocketAddress(Prefs.get("coords.proxyAddress", "proxyserver.mydomain.com"), (int) Prefs.get("coords.proxyPort", 8080));
                        java.net.Proxy proxy = new java.net.Proxy(java.net.Proxy.Type.HTTP, socketaddr);
                        simbadCon = simbad.openConnection(proxy);
                    } else simbadCon = simbad.openConnection();
                    simbadCon.setConnectTimeout(10000);
                    simbadCon.setReadTimeout(10000);
                    BufferedReader in = new BufferedReader(new InputStreamReader(simbadCon.getInputStream()));
                    String inputLine;

                    while ((inputLine = in.readLine()) != null) {
                        //                        IJ.log(inputLine);
                        if (inputLine.startsWith("!! No astronomical object found : ")) {
                            break;
                        }
                        if (inputLine.startsWith("!!")) {
                            IJ.showMessage("SIMBAD query error", inputLine.trim());
                            break;
                        }
                        if (inputLine.contains("Number of objects :")) {
                            int num = parseInteger(inputLine.substring(inputLine.indexOf(":") + 1).trim());
                            //                            IJ.log(inputLine.substring(inputLine.indexOf(":")+1).trim());
                            //                            IJ.log(""+num);
                            if (num > 0) {
                                inputLine = in.readLine();
                                inputLine = in.readLine();
                                inputLine = in.readLine();

                                for (int i = 0; i < num; i++) {
                                    inputLine = in.readLine();
                                    if (inputLine == null) break;
                                    String[] columns = inputLine.split("\\|");
                                    if (columns.length > 2) {
                                        objectList.add(columns[2].trim());
                                        arcsecList.add(columns[1].trim());
                                    }
                                }
                            }
                            break;
                        }
                        if (inputLine.startsWith("Object")) {
                            objectList.add(inputLine.substring(7, inputLine.indexOf("---")).trim());
                            arcsecList.add("");
                            //                            while ((inputLine = in.readLine()) != null)
                            //                                {
                            //                                IJ.log(inputLine);
                            //                                }
                            break;
                        }

                    }

                    in.close();
                } catch (IOException ioe) {
                    IJ.showMessage("SIMBAD query error", "<html>" + "Could not open link to Simbad " + (useHarvard ? "at Harvard." : "in France.") + "<br>" +
                            "Check internet connection or proxy settings or" + "<br>" +
                            "try " + (useHarvard ? "France" : "Harvard") + " server (see Coordinate Converter Network menu)." + "</html>");

                }
                extraInfo = " (" + wcs.coordsys + ")";
                repaint();
            }
        }

        GenericDialog gd = new GenericDialog("Edit Annotation", getX() + getWidth() / 2 - 165, getY() + getHeight() / 2 - 77);
        gd.enableYesNoCancel("Save", "Delete");
        gd.addMessage("**Clear custom text to use the following object description selection**");
        if (!coordsText.equals("")) {
            objectList.add(coordsText);
            arcsecList.add("");
        }
        objectList.add("(" + uptoFourPlaces.format(pixel[0]) + ", " + uptoFourPlaces.format(pixel[1]) + ")");
        arcsecList.add("");
        String[] names = new String[objectList.size()];
        for (int i = 0; i < objectList.size(); i++) {
            names[i] = objectList.get(i) + (arcsecList.get(i).equals("") ? "" : "  (" + arcsecList.get(i) + " arcsec)");
        }

        gd.addChoice("Selection:", names, names[0]);
        gd.addStringField("Custom Text:", label, 40);
        gd.addCheckboxGroup(2, 2, new String[]{"Search SIMBAD (next time)", "Show Circle", "Show in SIMBAD (next time)", "Show Crosshair"},
                new boolean[]{useSimbadSearch, showCircle, showInSimbad, showCrosshair});//addCheckbox("Show Circle", showAnnotateCircle);
        gd.addNumericField("Search Radius:", simbadSearchRadius, 3, 9, "(arcsec)");
        gd.addNumericField("Circle Radius:", radius, 3, 9, "(pixels)");
        gd.addChoice("Annotation Color:", colors, IJU.colorNameOf(roi.getAnnotateColor()));
        gd.addMessage("Change SIMBAD network parameters in Coordinate Converter");
        gd.showDialog();

        if (gd.wasCanceled()) return;
        if (gd.wasOKed()) {
            useSimbadSearch = gd.getNextBoolean();
            useSimbadSearchCB.setState(useSimbadSearch);
            Prefs.set("Astronomy_Tool.useSimbadSearch", useSimbadSearch);
            showCircle = gd.getNextBoolean();
            showInSimbad = gd.getNextBoolean();
            showInSimbadCB.setState(showInSimbad);
            Prefs.set("Astronomy_Tool.showInSimbad", showInSimbad);
            showCrosshair = gd.getNextBoolean();
            simbadSearchRadius = gd.getNextNumber();
            Prefs.set("Astronomy_Tool.simbadSearchRadius", simbadSearchRadius);
            simbadSearchRadiusMenuItem.setLabel("Set SIMBAD search radius (currently " + simbadSearchRadius + " arcsec)...");
            radius = gd.getNextNumber();
            label = gd.getNextString();
            int choiceIndex = gd.getNextChoiceIndex();
            if (label.trim().equals("")) {
                label = objectList.get(choiceIndex);
            }
            col = gd.getNextChoice();
            updateAnnotateRoi(roi, showCircle, showCrosshair, true, pixel[0], pixel[1], radius, label, IJU.colorOf(col));
        } else {
            removeAnnotateRoi(pixel[0], pixel[1]);
        }
        ac.repaint();
    }

    protected AnnotateRoi findAnnotateRoi(double x, double y) {
        return OverlayCanvas.getOverlayCanvas(imp).findAnnotateRoi(x, y);
    }

    protected void addAnnotateRoi(ImagePlus implus, boolean showCir, boolean isCentered, boolean showLab, boolean isFromAstrometry, double x, double y, double rad, String labelText, Color col, boolean fromHeader) {
        AnnotateRoi roi = new AnnotateRoi(showCir, isCentered, showLab, isFromAstrometry, x, y, rad, labelText, col);
//		roi.setAppearance (pixelColor);
        roi.setImage(implus);
        OverlayCanvas overlayCanvas = OverlayCanvas.getOverlayCanvas(implus);
        overlayCanvas.add(roi);
        if (autoUpdateAnnotationsInHeader && !fromHeader) {
            String value = "'" + uptoTwoPlaces.format(IJU.ijX2fitsX(x)) + "," + uptoTwoPlaces.format(IJU.ijY2fitsY(imp.getHeight(), y)) + "," + uptoTwoPlaces.format(rad) + "," +
                    (showCir ? "1" : "0") + "," + (isCentered ? "1" : "0") + "," + (showLab ? "1" : "0") + "," + (isFromAstrometry ? "1" : "0") + "," + IJU.colorNameOf(col) + "'";
            FitsJ.putHeader(imp, FitsJ.addAnnotateCard(value, labelText, FitsJ.getHeader(imp)));
        }
    }

    protected void updateAnnotateRoi(AnnotateRoi roi, boolean showCircle, boolean showCrosshair, boolean showLab, double x, double y, double rad, String labelText, Color col) {
        roi.setAppearance(showCircle, showCrosshair, showLab, false, x, y, rad, labelText, col);
        if (autoUpdateAnnotationsInHeader) {
            String value = "'" + uptoTwoPlaces.format(IJU.ijX2fitsX(x)) + "," + uptoTwoPlaces.format(IJU.ijY2fitsY(imp.getHeight(), y)) + "," + uptoTwoPlaces.format(rad) + "," +
                    (showCircle ? "1" : "0") + "," + (showCrosshair ? "1" : "0") + "," + (showLab ? "1" : "0") + ",0," + IJU.colorNameOf(col) + "'";  //the last '0' indicates NOT from astrometry
            FitsJ.putHeader(imp, FitsJ.setAnnotateCard(IJU.ijX2fitsX(x), IJU.ijY2fitsY(imp.getHeight(), y), value, labelText, FitsJ.getHeader(imp)));
        }
    }

    protected boolean removeAnnotateRoi(double x, double y) {
        if (OverlayCanvas.getOverlayCanvas(imp).removeAnnotateRoi(x, y)) {
            if (autoUpdateAnnotationsInHeader)
                FitsJ.putHeader(imp, FitsJ.removeAnnotateCard(IJU.ijX2fitsX(x), IJU.ijY2fitsY(imp.getHeight(), y), FitsJ.getHeader(imp)));
            ac.repaint();
            return true;
        }
        return false;
    }

    protected void addDisplayedAnnotationsToHeader() {
        var header = FitsJ.getHeader(imp);
        Roi[] rois = OverlayCanvas.getOverlayCanvas(imp).getRois();
        int len = 0;
        if (rois != null) len = rois.length;
        if (len > 0) {
            for (int i = 0; i < len; i++) {
                if (rois[i] instanceof AnnotateRoi) {
                    AnnotateRoi roi = (AnnotateRoi) rois[i];
                    String value = "'" + uptoTwoPlaces.format(IJU.ijX2fitsX(roi.getXpos())) + "," +
                            uptoTwoPlaces.format(IJU.ijY2fitsY(imp.getHeight(), roi.getYpos())) + "," +
                            uptoTwoPlaces.format(roi.getRadius()) + "," + (roi.getShowCircle() ? "1" : "0") + "," +
                            (roi.getShowCentroid() ? "1" : "0") + "," + (roi.getShowLabel() ? "1" : "0") + "," +
                            (roi.getIsFromAstrometry() ? "1" : "0") + "," + IJU.colorNameOf(roi.getAnnotateColor()) + "'";
                    header = FitsJ.addAnnotateCard(value, roi.getLabel(), header);
                }
            }
        }
        FitsJ.putHeader(imp, header);
    }

    public void displayAnnotationsFromHeader(boolean clearFirst, boolean redraw, boolean forceShow) {
        String key;
        String colorName = defaultAnnotationColor;
        var hdr = FitsJ.getHeader(imp);
        if (clearFirst) OverlayCanvas.getOverlayCanvas(imp).removeAnnotateRois();
        if (hdr != null && hdr.cards().length > 0) {
            for (int i = 0; i < hdr.cards().length; i++) {
                key = FitsJ.getCardKey(hdr.cards()[i]);
                if (hdr.cards()[i] != null && key != null && key.equals("ANNOTATE")) {
                    String[] pieces = FitsJ.getCardStringValue(hdr.cards()[i]).trim().split(",");
                    if (pieces.length > 1) {
                        double x = Tools.parseDouble(pieces[0]);
                        double y = Tools.parseDouble(pieces[1]);
                        if (!Double.isNaN(x) && !Double.isNaN(y)) {
                            double rad = 10;
                            if (pieces.length > 2) {
                                double r = Tools.parseDouble(pieces[2]);
                                if (!Double.isNaN(r)) rad = r;
                            }
                            boolean showCir = true;
                            if (pieces.length > 3 && pieces[3].equals("0")) showCir = false;
                            boolean showCen = false;
                            if (pieces.length > 4 && pieces[4].equals("1")) showCen = true;
                            boolean showLab = true;
                            if (pieces.length > 5 && pieces[5].equals("0")) showLab = false;
                            boolean isFromAstrometry = false;
                            if (pieces.length > 6 && pieces[6].equals("1")) isFromAstrometry = true;
                            if (pieces.length > 7) colorName = pieces[7].trim();
                            String labText = FitsJ.getCardComment(hdr.cards()[i]);
                            if (labText == null) labText = new String("");
                            labText.trim();
                            addAnnotateRoi(imp, showCir, showCen, showLab, isFromAstrometry, IJU.fitsX2ijX(x), IJU.fitsY2ijY(imp.getHeight(), y), rad, labText, IJU.colorOf(colorName), true);
                        }
                    }
                }
            }
        }
        if (forceShow) {
            ac.showAnnotations = true;
            buttonShowAnnotations.setSelected(true);
        }
        if (redraw) ac.repaint();
    }


    public void setDisableShiftClick(boolean state) {
        shiftClickDisabled = state;
    }

    public void setMovingAperture(boolean state) {
        movingAperture = state;
    }

    void getPrefs() {
        savedIpHeight = (int) Prefs.get("Astronomy_Tool.savedIpHeight", savedIpHeight);
        savedIpWidth = (int) Prefs.get("Astronomy_Tool.savedIpWidth", savedIpWidth);
        showMeanNotPeak = Prefs.get("Astronomy_Tool.showMeanNotPeak", showMeanNotPeak);
        useSexagesimal = Prefs.get("Astronomy_Tool.useSexagesimal", useSexagesimal);
        autoContrast = Prefs.get("Astronomy_Tool.startupAutoLevel", autoContrast);
        ac.showRedCrossHairCursor = Prefs.get("Astronomy_Tool.showRedCrossHairCursor", ac.showRedCrossHairCursor);
        startupPrevSize = Prefs.get("Astronomy_Tool.startupPrevSize", startupPrevSize);
        startupPrevPan = Prefs.get("Astronomy_Tool.startupPrevPan", startupPrevPan);
        startupPrevZoom = Prefs.get("Astronomy_Tool.startupPrevZoom", startupPrevZoom);
        fixedContrast = Prefs.get("Astronomy_Tool.startupPrevLevels", fixedContrast);
        fixedContrastPerSlice = Prefs.get("Astronomy_Tool.startupPrevLevelsPerSlice", fixedContrastPerSlice);
        writeMiddleClickValuesTable = Prefs.get("Astronomy_Tool.writeMiddleClickValuesTable", writeMiddleClickValuesTable);
        writeMiddleDragValuesTable = Prefs.get("Astronomy_Tool.writeMiddleDragValuesTable", writeMiddleDragValuesTable);
        writeMiddleClickValuesLog = Prefs.get("Astronomy_Tool.writeMiddleClickValuesLog", writeMiddleClickValuesLog);
        writeMiddleDragValuesLog = Prefs.get("Astronomy_Tool.writeMiddleDragValuesLog", writeMiddleDragValuesLog);
        showAnnotateCircle = Prefs.get("Astronomy_Tool.showAnnotateCircle", showAnnotateCircle);
        rightClickAnnotate = Prefs.get("Astronomy_Tool.rightClickAnnotate", rightClickAnnotate);
        useSimbadSearch = Prefs.get("Astronomy_Tool.useSimbadSearch", useSimbadSearch);
        showInSimbad = Prefs.get("Astronomy_Tool.showInSimbad", showInSimbad);
        autoUpdateAnnotationsInHeader = Prefs.get("Astronomy_Tool.autoUpdateAnnotationsInHeader", autoUpdateAnnotationsInHeader);
        autoDisplayAnnotationsFromHeader = Prefs.get("Astronomy_Tool.autoDisplayAnnotationsFromHeader", autoDisplayAnnotationsFromHeader);
        autoConvert = Prefs.get("Astronomy_Tool.autoConvert", autoConvert);
        useSIPAllProjections = Prefs.get("Astronomy_Tool.useSIPAllProjections", useSIPAllProjections);
        autoSaveWCStoPrefs = Prefs.get("Astronomy_Tool.autoSaveWCStoPrefs", autoSaveWCStoPrefs);
        autoScaleFactorLow = Prefs.get("Astronomy_Tool.autoScaleFactorLow", autoScaleFactorLow);
        autoScaleFactorHigh = Prefs.get("Astronomy_Tool.autoScaleFactorHigh", autoScaleFactorHigh);
        autoScaleFactorLowRGB = Prefs.get("Astronomy_Tool.autoScaleFactorLowRGB", autoScaleFactorLowRGB);
        autoScaleFactorHighRGB = Prefs.get("Astronomy_Tool.autoScaleFactorHighRGB", autoScaleFactorHighRGB);
        simbadSearchRadius = Prefs.get("Astronomy_Tool.simbadSearchRadius", simbadSearchRadius);
        annotateCircleRadius = Prefs.get("Astronomy_Tool.annotateCircleRadius", annotateCircleRadius);
        reposition = Prefs.get("aperture.reposition", reposition);
        saveImage = Prefs.get("Astronomy_Tool.saveImage", saveImage);
        savePlot = Prefs.get("Astronomy_Tool.savePlot", savePlot);
        saveConfig = Prefs.get("Astronomy_Tool.saveConfig", saveConfig);
        saveTable = Prefs.get("Astronomy_Tool.saveTable", saveTable);
        saveApertures = Prefs.get("Astronomy_Tool.saveApertures", saveApertures);
        saveLog = Prefs.get("Astronomy_Tool.saveLog", saveLog);
        imageSuffix = Prefs.get("Astronomy_Tool.imageSuffix", imageSuffix);
        plotSuffix = Prefs.get("Astronomy_Tool.plotSuffix", plotSuffix);
        configSuffix = Prefs.get("Astronomy_Tool.configSuffix", configSuffix);
        dataSuffix = Prefs.get("Astronomy_Tool.dataSuffix", dataSuffix);
        aperSuffix = Prefs.get("Astronomy_Tool.aperSuffix", aperSuffix);
        logSuffix = Prefs.get("Astronomy_Tool.logSuffix", logSuffix);
        saveAllPNG = Prefs.get("Astronomy_Tool.saveAllPNG", saveAllPNG);
        defaultMeasurementColor = Prefs.get("Astronomy_Tool.defaultMeasurementColor", defaultMeasurementColor);
        defaultAnnotationColor = Prefs.get("Astronomy_Tool.defaultAnnotationColor", defaultAnnotationColor);

        savedMag = Prefs.get("Astronomy_Tool.savedMag", savedMag);
        savedICWidth = (int) Prefs.get("Astronomy_Tool.savedICWidth", savedICWidth);
        savedICHeight = (int) Prefs.get("Astronomy_Tool.savedICHeight", savedICHeight);
        savedPanX = (int) Prefs.get("Astronomy_Tool.savedPanX", savedPanX);
        savedPanY = (int) Prefs.get("Astronomy_Tool.savedPanY", savedPanY);
        savedPanHeight = (int) Prefs.get("Astronomy_Tool.savedPanHeight", savedPanHeight);
        savedPanWidth = (int) Prefs.get("Astronomy_Tool.savedPanWidth", savedPanWidth);
        savedBlackValue = Prefs.get("Astronomy_Tool.savedMin", savedBlackValue);
        savedWhiteValue = Prefs.get("Astronomy_Tool.savedMax", savedWhiteValue);
        frameLocationX = (int) Prefs.get("Astronomy_Tool.frameLocationX", frameLocationX);
        frameLocationY = (int) Prefs.get("Astronomy_Tool.frameLocationY", frameLocationY);
        rememberWindowLocation = Prefs.get("Astronomy_Tool.rememberWindowLocation", rememberWindowLocation);
        radius = Prefs.get("aperture.radius", radius);
        rBack1 = Prefs.get("aperture.rback1", rBack1);
        rBack2 = Prefs.get("aperture.rback2", rBack2);
        exact = Prefs.get("aperture.exact", exact);
        nameOverlay = Prefs.get("aperture.nameoverlay", nameOverlay);
        valueOverlay = Prefs.get("aperture.valueoverlay", valueOverlay);
        ac.zoomIndicatorSize = (int) Prefs.get("Astronomy_Tool.zoomIndicatorSize", ac.zoomIndicatorSize);
        pixelScaleX = Prefs.get("Astronomy_Tool.pixelScaleX", pixelScaleX);
        pixelScaleY = Prefs.get("Astronomy_Tool.pixelScaleY", pixelScaleY);
        ac.NdirAngle = Prefs.get("Astronomy_Tool.NdirAngle", ac.NdirAngle);
        ac.EdirAngle = Prefs.get("Astronomy_Tool.EdirAngle", ac.EdirAngle);
        middleClickCenter = Prefs.get("Astronomy_Tool.middleClickCenter", middleClickCenter);
        showPhotometer = Prefs.get("Astronomy_Tool.showPhotometer", showPhotometer);
        removeBackStars = Prefs.get("aperture.removebackstars", removeBackStars);
        backPlane = Prefs.get("aperture.backplane", backPlane);
        showRemovedPixels = Prefs.get("aperture.showremovedpixels", showRemovedPixels);
        useInvertingLut = Prefs.get("Astronomy_Tool.useInvertingLut", useInvertingLut);
        ac.showAnnotations = Prefs.get("Astronomy_Tool.showAnnotations", ac.showAnnotations);
        autoNupEleft = Prefs.get("Astronomy_Tool.autoNupEleft", autoNupEleft);
        invertX = Prefs.get("Astronomy_Tool.invertX", invertX);
        invertY = Prefs.get("Astronomy_Tool.invertY", invertY);
        rotation = (int) Prefs.get("Astronomy_Tool.rotation", rotation);
        showZoom = Prefs.get("Astronomy_Tool.showZoom", showZoom);
        showDir = Prefs.get("Astronomy_Tool.showDir", showDir);
        showXY = Prefs.get("Astronomy_Tool.showXY", showXY);
        showScaleX = Prefs.get("Astronomy_Tool.showScaleX", showScaleX);
        showScaleY = Prefs.get("Astronomy_Tool.showScaleY", showScaleY);
        showAbsMag = Prefs.get("Astronomy_Tool.showAbsMag", showAbsMag);
        showIntCntWithAbsMag = Prefs.get("Astronomy_Tool.showIntCntWithAbsMag", showIntCntWithAbsMag);
        showSkyOverlay = Prefs.get("aperture.skyoverlay", showSkyOverlay);
        useFixedMinMaxValues = Prefs.get("Astronomy_Tool.useFixedMinMaxValues", useFixedMinMaxValues);
        autoGrabBandCFromHistogram = Prefs.get("Astronomy_Tool.autoGrabBandCFromHistogram", autoGrabBandCFromHistogram);
        fixedMinValue = Prefs.get("Astronomy_Tool.fixedMinValue", minValue);
        fixedMaxValue = Prefs.get("Astronomy_Tool.fixedMaxValue", maxValue);
        negateMeasureDelMag = Prefs.get("Astronomy_Tool.negateMeasureDelMag", negateMeasureDelMag);
        showMeasureSex = Prefs.get("Astronomy_Tool.showMeasureSex", showMeasureSex);
        showMeasureCircle = Prefs.get("Astronomy_Tool.showMeasureCircle", showMeasureCircle);
        showMeasureLength = Prefs.get("Astronomy_Tool.showMeasureLength", showMeasureLength);
        showMeasurePA = Prefs.get("Astronomy_Tool.showMeasurePA", showMeasurePA);
        showMeasureDelMag = Prefs.get("Astronomy_Tool.showMeasureDelMag", showMeasureDelMag);
        showMeasureFluxRatio = Prefs.get("Astronomy_Tool.showMeasureFluxRatio", showMeasureFluxRatio);
        writeMeasureLengthLog = Prefs.get("Astronomy_Tool.writeMeasureLengthLog", writeMeasureLengthLog);
        writeMeasureLengthTableDeg = Prefs.get("Astronomy_Tool.writeMeasureLengthTableDeg", writeMeasureLengthTableDeg);
        writeMeasureLengthTableMin = Prefs.get("Astronomy_Tool.writeMeasureLengthTableMin", writeMeasureLengthTableMin);
        writeMeasureLengthTableSec = Prefs.get("Astronomy_Tool.writeMeasureLengthTableSec", writeMeasureLengthTableSec);
        writeMeasurePA = Prefs.get("Astronomy_Tool.writeMeasurePA", writeMeasurePA);
        writeMeasureDelMag = Prefs.get("Astronomy_Tool.writeMeasureDelMag", writeMeasureDelMag);
        writeMeasureFluxRatio = Prefs.get("Astronomy_Tool.writeMeasureFluxRatio", writeMeasureFluxRatio);
        writePhotometricDataTable = Prefs.get("Astronomy_Tool.writePhotometricDataTable", writePhotometricDataTable);
        showMeasureMultiLines = Prefs.get("Astronomy_Tool.showMeasureMultiLines", showMeasureMultiLines);
        showMeasureCrosshair = Prefs.get("Astronomy_Tool.showMeasureCrosshair", showMeasureCrosshair);
    }

    void savePrefs() {
        if (imp != null) savedIpWidth = imp.getWidth();
        if (imp != null) savedIpHeight = imp.getHeight();

        Prefs.set("Astronomy_Tool.savedIpHeight", savedIpHeight);
        Prefs.set("Astronomy_Tool.savedIpWidth", savedIpWidth);
        Prefs.set("Astronomy_Tool.useSexagesimal", useSexagesimal);
        Prefs.set("Astronomy_Tool.showMeanNotPeak", showMeanNotPeak);
        Prefs.set("Astronomy_Tool.startupAutoLevel", autoContrast);
        Prefs.set("Astronomy_Tool.startupPrevPan", startupPrevPan);
        Prefs.set("Astronomy_Tool.startupPrevZoom", startupPrevZoom);
        Prefs.set("Astronomy_Tool.startupPrevLevels", fixedContrast);
        Prefs.set("Astronomy_Tool.startupPrevLevelsPerSlice", fixedContrastPerSlice);
        Prefs.set("Astronomy_Tool.middleClickCenter", middleClickCenter);
        Prefs.set("Astronomy_Tool.showRedCrossHairCursor", ac.showRedCrossHairCursor);
        Prefs.set("Astronomy_Tool.writeMiddleClickValuesTable", writeMiddleClickValuesTable);
        Prefs.set("Astronomy_Tool.writeMiddleDragValuesTable", writeMiddleDragValuesTable);
        Prefs.set("Astronomy_Tool.writeMiddleClickValuesLog", writeMiddleClickValuesLog);
        Prefs.set("Astronomy_Tool.writeMiddleDragValuesLog", writeMiddleDragValuesLog);
        Prefs.set("Astronomy_Tool.autoConvert", autoConvert);
        Prefs.set("Astronomy_Tool.useSIPAllProjections", useSIPAllProjections);
        Prefs.set("Astronomy_Tool.autoSaveWCStoPrefs", autoSaveWCStoPrefs);
        Prefs.set("aperture.nameoverlay", nameOverlay);
        Prefs.set("aperture.valueoverlay", valueOverlay);
        Prefs.set("Astronomy_Tool.autoScaleFactorLow", autoScaleFactorLow);
        Prefs.set("Astronomy_Tool.autoScaleFactorHigh", autoScaleFactorHigh);
        Prefs.set("Astronomy_Tool.autoScaleFactorLowRGB", autoScaleFactorLowRGB);
        Prefs.set("Astronomy_Tool.autoScaleFactorHighRGB", autoScaleFactorHighRGB);
        Prefs.set("Astronomy_Tool.simbadSearchRadius", simbadSearchRadius);
        Prefs.set("Astronomy_Tool.annotateCircleRadius", annotateCircleRadius);
        Prefs.set("Astronomy_Tool.startupPrevSize", startupPrevSize);
        Prefs.set("Astronomy_Tool.savedMag", savedMag);
        Prefs.set("Astronomy_Tool.pixelScaleX", pixelScaleX);
        Prefs.set("Astronomy_Tool.pixelScaleY", pixelScaleY);
        Prefs.set("Astronomy_Tool.savedICWidth", savedICWidth);
        Prefs.set("Astronomy_Tool.savedICHeight", savedICHeight);
        Prefs.set("Astronomy_Tool.savedPanX", savedPanX);
        Prefs.set("Astronomy_Tool.savedPanY", savedPanY);
        Prefs.set("Astronomy_Tool.savedPanHeight", savedPanHeight);
        Prefs.set("Astronomy_Tool.savedPanWidth", savedPanWidth);
        Prefs.set("Astronomy_Tool.savedMin", savedBlackValue);
        Prefs.set("Astronomy_Tool.savedMax", savedWhiteValue);
        Prefs.set("Astronomy_Tool.zoomIndicatorSize", ac.zoomIndicatorSize);
        Prefs.set("Astronomy_Tool.NdirAngle", ac.NdirAngle);
        Prefs.set("Astronomy_Tool.EdirAngle", ac.EdirAngle);
        Prefs.set("Astronomy_Tool.saveImage", saveImage);
        Prefs.set("Astronomy_Tool.savePlot", savePlot);
        Prefs.set("Astronomy_Tool.saveConfig", saveConfig);
        Prefs.set("Astronomy_Tool.saveTable", saveTable);
        Prefs.set("Astronomy_Tool.saveApertures", saveApertures);
        Prefs.set("Astronomy_Tool.saveLog", saveLog);
        Prefs.set("Astronomy_Tool.imageSuffix", imageSuffix);
        Prefs.set("Astronomy_Tool.plotSuffix", plotSuffix);
        Prefs.set("Astronomy_Tool.configSuffix", configSuffix);
        Prefs.set("Astronomy_Tool.dataSuffix", dataSuffix);
        Prefs.set("Astronomy_Tool.aperSuffix", aperSuffix);
        Prefs.set("Astronomy_Tool.logSuffix", logSuffix);
        Prefs.set("Astronomy_Tool.defaultMeasurementColor", defaultMeasurementColor);
        Prefs.set("Astronomy_Tool.defaultAnnotationColor", defaultAnnotationColor);
        Prefs.set("Astronomy_Tool.saveAllPNG", saveAllPNG);
        Prefs.set("Astronomy_Tool.rightClickAnnotate", rightClickAnnotate);
        Prefs.set("Astronomy_Tool.showAnnotateCircle", showAnnotateCircle);
        Prefs.set("Astronomy_Tool.useSimbadSearch", useSimbadSearch);
        Prefs.set("Astronomy_Tool.showInSimbad", showInSimbad);
        Prefs.set("Astronomy_Tool.autoUpdateAnnotationsInHeader", autoUpdateAnnotationsInHeader);
        Prefs.set("Astronomy_Tool.autoDisplayAnnotationsFromHeader", autoDisplayAnnotationsFromHeader);
        Prefs.set("aperture.reposition", reposition);
        if (this != null) frameLocationX = this.getLocation().x;
        if (this != null) frameLocationY = this.getLocation().y;
        Prefs.set("Astronomy_Tool.frameLocationX", frameLocationX);
        Prefs.set("Astronomy_Tool.frameLocationY", frameLocationY);
        Prefs.set("Astronomy_Tool.rememberWindowLocation", rememberWindowLocation);
        Prefs.set("aperture.removebackstars", removeBackStars);
        Prefs.set("aperture.showremovedpixels", showRemovedPixels);
        Prefs.set("Astronomy_Tool.useInvertingLut", useInvertingLut);
        Prefs.set("Astronomy_Tool.showAnnotations", ac.showAnnotations);
        Prefs.set("Astronomy_Tool.autoNupEleft", autoNupEleft);
        if (imp != null) {
            String fileName = IJU.getSliceFilename(imp);
            if (!autoNupEleft || (autoNupEleft && !(fileName.endsWith(".png") || fileName.endsWith(".jpg")))) {
                Prefs.set("Astronomy_Tool.invertX", invertX);
                Prefs.set("Astronomy_Tool.invertY", invertY);
                Prefs.set("Astronomy_Tool.rotation", rotation);
                Prefs.set("Astronomy_Tool.showZoom", showZoom);
                Prefs.set("Astronomy_Tool.showDir", showDir);
                Prefs.set("Astronomy_Tool.showXY", showXY);
                Prefs.set("Astronomy_Tool.showScaleX", showScaleX);
                Prefs.set("Astronomy_Tool.showScaleY", showScaleY);
            }
        }
        Prefs.set("Astronomy_Tool.showAbsMag", showAbsMag);
        Prefs.set("Astronomy_Tool.showIntCntWithAbsMag", showIntCntWithAbsMag);
        Prefs.set("aperture.skyoverlay", showSkyOverlay);
        if (imp != null && imp.getType() != ImagePlus.COLOR_RGB)
            Prefs.set("Astronomy_Tool.useFixedMinMaxValues", useFixedMinMaxValues);
        Prefs.set("Astronomy_Tool.autoGrabBandCFromHistogram", autoGrabBandCFromHistogram);
        Prefs.set("Astronomy_Tool.fixedMinValue", fixedMinValue);
        Prefs.set("Astronomy_Tool.fixedMaxValue", fixedMaxValue);
        Prefs.set("Astronomy_Tool.negateMeasureDelMag", negateMeasureDelMag);
        Prefs.set("Astronomy_Tool.showMeasureSex", showMeasureSex);
        Prefs.set("Astronomy_Tool.showMeasureCircle", showMeasureCircle);
        Prefs.set("Astronomy_Tool.showMeasureLength", showMeasureLength);
        Prefs.set("Astronomy_Tool.showMeasurePA", showMeasurePA);
        Prefs.set("Astronomy_Tool.showMeasureDelMag", showMeasureDelMag);
        Prefs.set("Astronomy_Tool.showMeasureFluxRatio", showMeasureFluxRatio);
        Prefs.set("Astronomy_Tool.writeMeasureLengthLog", writeMeasureLengthLog);
        Prefs.set("Astronomy_Tool.writeMeasureLengthTableDeg", writeMeasureLengthTableDeg);
        Prefs.set("Astronomy_Tool.writeMeasureLengthTableMin", writeMeasureLengthTableMin);
        Prefs.set("Astronomy_Tool.writeMeasureLengthTableSec", writeMeasureLengthTableSec);
        Prefs.set("Astronomy_Tool.writeMeasurePA", writeMeasurePA);
        Prefs.set("Astronomy_Tool.writeMeasureDelMag", writeMeasureDelMag);
        Prefs.set("Astronomy_Tool.writeMeasureFluxRatio", writeMeasureFluxRatio);
        Prefs.set("Astronomy_Tool.writePhotometricDataTable", writePhotometricDataTable);
        Prefs.set("Astronomy_Tool.showMeasureMultiLines", showMeasureMultiLines);
        Prefs.set("Astronomy_Tool.showMeasureCrosshair", showMeasureCrosshair);
    }

}   // AstroStackWindow class


