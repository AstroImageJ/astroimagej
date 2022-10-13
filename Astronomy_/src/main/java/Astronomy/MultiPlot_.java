// MultiPlot_.java
package Astronomy;

import astroj.*;
import flanagan.analysis.Regression;
import flanagan.analysis.Smooth;
import flanagan.math.Minimization;
import flanagan.math.MinimizationFunction;
import ij.*;
import ij.astro.types.Pair;
import ij.astro.util.PdfPlotOutput;
import ij.astro.util.UIHelper;
import ij.gui.*;
import ij.io.OpenDialog;
import ij.io.SaveDialog;
import ij.measure.ResultsTable;
import ij.plugin.GifWriter;
import ij.plugin.PlugIn;
import ij.process.ImageProcessor;
import ij.text.TextPanel;
import ij.util.Tools;
import util.GenericSwingDialog;
import util.PlotDataBinning;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.GeneralPath;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.Path;
import java.text.DecimalFormat;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.DoubleStream;
import java.util.stream.IntStream;

/**
 * This plugin plots any number of columns from a Results or MeasurementTable.  This plugin
 * is based on the Plot_Columns plugin by F.V. Hessman.
 *
 * @author F.V. Hessman, Georg-August-Universitaet Goettingen
 * @date 2009-FEB-09
 * @version 1.0
 * @author K.A. Collins, University of Louisville, Kentucky USA
 * @date 2010-JUN-09
 * @version 2.1
 * @changes New GUI interface, support for multiple column plotting, error plotting
 * support, interactive shifting and scaling, titles, subtitles, and legends, custom,
 * semi-custom, and auto scaling
 */

/**
 * This plugin plots any two columns from a Results or MeasurementTable.
 * @author F.V. Hessman, Georg-August-Universitaet Goettingen
 * @date 2009-FEB-09
 * @version 1.0
 *
 * To create version outside of the Astronomy plugins, comment in the "// plain ..." lines and comment out the "// astroj ..." lines
 */
@SuppressWarnings("SpellCheckingInspection")
public class MultiPlot_ implements PlugIn, KeyListener {
    static boolean panelsUpdating;
    static String title;
    static double titlePosX;
    static double titlePosY;
    static String subtitle;
    static double subtitlePosX;
    static double subtitlePosY;
    static double legendPosX;
    static double legendPosY;
    static boolean button2Drag;
    static String xlabeldefault;
    static int xcolumndefault;
    static int plotSizeX;
    static int plotSizeY;
    static double pltMinX;
    static double pltMinY;
    static double pltMaxX;
    static double pltMaxY;
    static double plotMinX;
    static double plotMinY;
    static double plotMaxX;
    static double plotMaxY;
    static int xExponent;
    static int yExponent;
    static int mmagrefs;
    static int firstCurve;
    static String xLegend;
    static String yLegend;
    static boolean legendLeft;
    static boolean legendRight;
    static boolean useTitle;
    static boolean useSubtitle;
    static boolean useXCustomName;
    static boolean useYCustomName;
    static boolean useXColumnName;
    static boolean useYColumnName;
    static boolean xTics;
    static boolean yTics;
    static boolean xGrid;
    static boolean yGrid;
    static boolean xNumbers;
    static boolean yNumbers;
    static boolean plotAutoMode;
    static boolean nextPanel;
    static boolean canceled;
    static boolean waitSecs;
    static boolean openDataSetWindow, openRefStarWindow;
    static boolean rememberWindowLocations, keepSeparateLocationsForFitWindows;
    static boolean autoScaleX;
    static boolean useFirstX;
    static boolean autoScaleY;
    static boolean showToolTips;
    static boolean showXAxisNormal;
    static boolean showXAxisAsPhase;
    static boolean showXAxisAsHoursSinceTc;
    static boolean showXAxisAsDaysSinceTc;
    static double T0;
    static double period;
    static double duration;
    static double netT0;
    static double netPeriod;
    static boolean twoxPeriod;
    static boolean oddNotEven;
    static int[] smoothLen;
    static boolean showXScaleInfo;
    static boolean showYScaleInfo, showYNormInfo;
    static boolean showYShiftInfo;
    static boolean showLScaleInfo;
    static boolean showLRelScaleInfo;
    static boolean showLShiftInfo;
    static boolean showLRelShiftInfo;
    static boolean showYAvgInfo;
    static boolean showLAvgInfo;
    static boolean showYmmagInfo;
    static boolean showLmmagInfo, showLdetrendInfo, showLnormInfo;
    static boolean showYSymbolInfo;
    static boolean showLSymbolInfo;
    static boolean showSigmaForAllCurves;
    static boolean showSigmaForDetrendedCurves;
    //        static boolean useTwoLineLegend;
    static boolean useWideDataPanel;
    static boolean subframeWasShowing;
    static boolean newPlotWindow;
    static boolean useDefaultSettings;
    static boolean tableHasText;
    static boolean setSubpanelVisible, refStarPanelWasShowing, addAstroDataFrameWasShowing;
    static boolean usePixelScale;
    static boolean showVMarker1, showVMarker2;
    static boolean showDMarkers, useDMarker1, useDMarker4;
    static boolean divideNotSubtract;
    static boolean leftDragReleased;
    static boolean prioritizeColumns;
    static boolean invertYAxis;
    static int invertYAxisSign;
    static boolean negateMag;
    static boolean selectAnotherTableCanceled;
    static boolean panelShiftDown;
    static boolean panelControlDown;
    static boolean panelAltDown;
    static boolean keepFileNamesOnAppend;
    static public boolean updatePlotRunning;
    static public boolean autoAstroDataUpdateRunning;
    static boolean saveNewXColumn;
    static boolean saveNewYColumn;
    static boolean saveNewYErrColumn;
    static boolean saveNewModelColumn;
    static boolean saveNewResidualColumn;
    static boolean saveNewResidualErrColumn;
    static boolean updatePlotEnabled;
    static boolean refStarChanged;
    static boolean detrendParChanged;
    static boolean disableUpdatePlotBox;
    static boolean astroConverterUpdating;
    static boolean useNelderMeadChi2ForDetrend;
    static boolean openFitPanels;
    static boolean createDetrendModel;
    static boolean modifyCurvesAbove;
    static boolean modifyCurvesBelow;
    static boolean[] detrendYDNotConstant;
    static int maxSubsetColumns;
    static boolean silenceAbsMagTF;

    private static boolean awaitingScheduledPlotUpdate = false;
    private static final Object lock = new Object();

    static double pixelScale;
    static double zoom;
    static double xMin;
    static double xBase;
    static double xMax;
    static double xWidth;
    static double yMin;
    static double yMax;
    static double xOffset, yOffset;
    static int xJD, yJD;
    static double xautoscalemin;
    static double xautoscalemax;
    static double yautoscalemin;
    static double yautoscalemax;
    static double yRange;
    static double dashLength;
    static double numDashes;
    static double mouseX;
    static double mouseY;
    static double totalPanOffsetX;
    static double totalPanOffsetY;
    static double newPanOffsetX;
    static double newPanOffsetY;

    static int cur;
    static int curve;
    static int plotOptions;
    static int mainFrameLocationX;
    static int mainFrameLocationY;
    static int subFrameLocationX, refStarFrameLocationX, addAstroDataFrameLocationX;
    static int subFrameLocationY, refStarFrameLocationY, addAstroDataFrameLocationY;
    static int plotFrameLocationX;
    static int plotFrameLocationY;

    static int excludedHeadSamples;
    static int excludedTailSamples;
    static int holdExcludedHeadSamples;
    static int holdExcludedTailSamples;
    static int excludedHeadSamplesStep;
    static int excludedTailSamplesStep;
    static int refStarHorzWidth;
    static int lastRefStar;
    static int returnCode;
    static boolean cycleEnabledStarsLess1PressedConsecutive;

    static double yMaxStep;
    static double yMinStep;
    static double yMid;
    static double firstXmin;
    static int plotSizeXStep;
    static int plotSizeYStep;
    static int nearestLine;
    static double nearestX;
    static int boldedDatum;
    static boolean useBoldedDatum;
    static boolean useUpdateStack;
    static boolean multiUpdate;
    static int selectedRow;

    static boolean ignoreUpdate;
    static String vMarker1TopText;
    static String vMarker1BotText;
    static String vMarker2TopText;
    static String vMarker2BotText;
    static boolean saveImage;
    static boolean savePlot;
    static boolean saveSeeingProfile;
    static boolean saveConfig;
    static boolean saveTable;
    static boolean saveApertures;
    static boolean saveLog, saveFitPanels, saveFitPanelText, saveDataSubset, showDataSubsetPanel;
    static boolean saveAllPNG;
    static boolean unscale;
    static boolean unshift;
    static boolean autoAstroDataUpdate;
    static String imageSuffix;
    static String seeingProfileSuffix;
    static String aperSuffix;
    static String logSuffix, fitPanelSuffix, fitPanelTextSuffix, dataSubsetSuffix;
    static String plotSuffix;
    static String configSuffix;
    static String dataSuffix;
    static String appendDestinationSuffix;
    static String appendSourceSuffix;
    static String combinedTableName;
    static String requestedTableName;
    static String templateDir;
    static String JDColumn, raColumn, decColumn;
    static int jdCol, raCol, decCol;

    static int n;
    static int[] nn;
    static int[] nnr;

    static String[] xlabel2;
    static int selectedRowStart, selectedRowEnd;
    static java.util.List<String> deletedRowList;
    static java.io.File[] dragAndDropFiles;
    static double[][] x;
    static double[][] xModel1;
    static double[][] xModel2;
    static double[][] y;
    static double[][] yModel1, yModel1Err;
    static double[][] yModel2;
    static double[][] residual, plottedResidual;
    static double[][] yerr;
    static double[][] yop;
    static double[][] yoperr;
    static double[][][] detrend;
    static double[][] detrendVars;
    static double[][] detrendXs;
    static double[][] detrendYs;
    static double[][] detrendYEs;

    static double[] yAverage, yBaselineAverage, yDepthEstimate, detrendYAverage;
    static double[][] lcModel;


    static double[][] xc1;
    static double[][] xc2;
    static double[][] yc1;
    static double[][] yc2;
    static double xMultiplierFactor;
    static double yMultiplierFactor;
    static int excluded;

    static String xlab;
    static String ylab;
    static String priorityColumns;
    static String priorityDetrendColumns;
    static double dx;
    static double dy;
    static double xPlotMin;
    static double xPlotMax;
    static double yPlotMin;
    static double yPlotMax;


    static boolean addAirmass, addAltitude, addAzimuth, addBJD, addBJDCorr, addDecNow, addRaNow;
    static boolean addDec2000, addRA2000, addGJD, addHJD, addHJDCorr, addHourAngle, addZenithDistance;

    static String airmassName, altitudeName, azimuthName, bjdName, bjdCorrName, decNowName, raNowName;
    static String dec2000Name, ra2000Name, gjdName, hjdName, hjdCorrName, hourAngleName, zenithDistanceName;


    static JCheckBox airmassCB, altitudeCB, azimuthCB, bjdCB, bjdCorrCB, decNowCB, raNowCB, autoAstroDataUpdateCB;
    static JCheckBox dec2000CB, ra2000CB, gjdCB, hjdCB, hjdCorrCB, hourAngleCB, zenithDistanceCB;

    static JTextField airmassField, altitudeField, azimuthField, bjdField, bjdCorrField, decNowField, raNowField;
    static JTextField dec2000Field, ra2000Field, gjdField, hjdField, hjdCorrField, hourAngleField, zenithDistanceField;

    static int startDragX, startDragY, endDragX, endDragY;
    static int startDragSubImageX, startDragSubImageY;
    static int startDragScreenX, startDragScreenY;
    static int newPositionX, newPositionY;

    static String temptextfield;
    static Integer tempIntField;

    static Plot plot;
    static ImagePlus iplus;
    static int sliceCol;
    static JFrame mainFrame;
    static JFrame subFrame, refStarFrame, addAstroDataFrame;
    static JFrame getMaxCurvesFrame;
    static Panel plotpanel;
    static JPanel mainpanel;
    static JScrollPane mainscrollpane;
    static JPanel mainsubpanel, refStarMainPanel, refStarBPanel, starsPlusSpacerPanel, starsPanel, spacerPanel, allNonePanel;
    static JScrollPane subscrollpane, refStarScrollPane;


    //----------Transit Fit Panel

    static double[] orbitalPeriod, eccentricity, omega, lonAscNode, teff;
    static double orbitalPeriodStep, eccentricityStep, omegaStep, teffStep, jminuskStep, mStarStep, rStarStep, rhoStarStep;
    static boolean[] forceCircularOrbit, useLonAscNode;
    static JSpinner[] orbitalPeriodSpinner, eccentricitySpinner, omegaSpinner, teffSpinner, jminuskSpinner, mStarSpinner, rStarSpinner, rhoStarSpinner;
    static JSpinner orbitalPeriodStepSpinner, eccentricityStepSpinner, omegaStepSpinner, teffStepSpinner, jminuskStepSpinner, mStarStepSpinner, rStarStepSpinner, rhoStarStepSpinner;
    static JPopupMenu orbitalPeriodStepPopup, eccentricityStepPopup, omegaStepPopup, teffStepPopup, jminuskStepPopup, mStarStepPopup, rStarStepPopup, rhoStarStepPopup;
    static JPanel orbitalPeriodStepPanel, eccentricityStepPanel, omegaStepPanel, teffStepPanel, jminuskStepPanel, mStarStepPanel, rStarStepPanel, rhoStarStepPanel;
    static JPanel t0panel, periodpanel, durationpanel;
    static JButton[] extractPriorsButton;
    static JCheckBox[] autoUpdatePriorsCB;
    static boolean[] autoUpdatePriors;
    static boolean skipPlotUpdate;
    static JMenuBar[] fitMenuBar;
    static JMenu[] fitFileMenu, autoPriorsMenu;
    static JCheckBoxMenuItem[] baselinePriorCB, depthPriorCB, arPriorCB, tcPriorCB, inclPriorCB;
    static JMenuItem[] saveFitPanelPngMenuItem, saveFitPanelJpgMenuItem, saveFitTextMenuItem;

    static boolean[][] autoUpdatePrior;
    static ImageIcon copyAndLockIcon = createImageIcon("astroj/images/customlegend.png", "Lock to the current fitted value.");
    static JButton[][] copyAndLockButton;
    public static double[] sigma, prevSigma, prevBic, tolerance, residualShift, autoResidualShift;
    //        static double residualShiftStep;
    static double[] defaultFitStep;
    static JSpinner[] toleranceSpinner, residualShiftSpinner;
    //        static JSpinner residualShiftStepSpinner;
//        static JPopupMenu residualShiftStepPopup;
    static int[] maxFitSteps;
    static JSpinner[] maxFitStepsSpinner, modelLineWidthSpinner, residualLineWidthSpinner;
    static boolean[] useTransitFit, bpLock, showLTranParams, showLResidual, autoUpdateFit, showModel, showResidual, showResidualError;
    static JCheckBox[] forceCircularOrbitCB, useTransitFitCB, showLTranParamsCB, bpLockCB, showLResidualCB, showResidualErrorCB, autoUpdateFitCB, showModelCB, showResidualCB;

    static boolean[][] lockToCenter, isFitted, detrendVarAllNaNs;
    static JCheckBox[][] lockToCenterCB;
    static double[][] priorCenter;

    static double[] priorCenterStep;
    static JSpinner[][] priorCenterSpinner;
    static JSpinner[] priorCenterStepSpinner;
    static JPopupMenu[] priorCenterStepPopup;
    static JPanel[] priorCenterStepPanel;


    static boolean[][] usePriorWidth;
    static JCheckBox[][] usePriorWidthCB;
    static double[][] priorWidth;
    static double[] priorWidthStep;
    static JSpinner[][] priorWidthSpinner;
    static JSpinner[] priorWidthStepSpinner;
    static JPopupMenu[] priorWidthStepPopup;
    static JPanel[] priorWidthStepPanel;

    static boolean[][] useCustomFitStep;
    static JCheckBox[][] useCustomFitStepCB;
    static double[][] fitStep;
    static double[] fitStepStep;
    static JSpinner[][] fitStepSpinner;
    static JSpinner[] fitStepStepSpinner;
    static JPopupMenu[] fitStepStepPopup;
    static JPanel[] fitStepStepPanel;

    static JTextField[] chi2dofLabel, dofLabel, chi2Label, bicLabel, sigmaLabel;
    static JSpinner[] bpSpinner;
    static JTextField[] t14Label, t14HoursLabel, t23Label, tauLabel, stellarDensityLabel;
    static JTextField[] transitDepthLabel, planetRadiusLabel, stepsTakenLabel;
    static JPanel[] sigmaPanel, transitDepthPanel;
    static Border[] sigmaBorder;

    static double[][] bestFit;
    static JTextField[][] bestFitLabel;

    public static JComboBox<Object>[][] fitDetrendComboBox;
    static JCheckBox[][] useFitDetrendCB;

    static JButton[] fitNowButton;

    static double[][] start, width, step, coeffs;
    static int[][] index;
    static int[] nTries, dof, modelLineWidth, residualLineWidth, startDetrendPars, endDetrendPars;
    static double[] chi2;
    static boolean[] converged;
    static double[] t14, t23, tau, chi2dof, bp, stellarDensity, planetRadius, transitDepth;
    public static double[] bic;
    static String[] spectralType;
    static double[] fitMin;
    static double[] fitMax;
    static double[] fitLeft;
    static double[] fitRight;
    static Minimization minimization;

    static JFrame[] fitFrame;
    static ImageIcon fitFrameIcon;
    static JPanel[] fitPanel;
    static JScrollPane[] fitScrollPane;
    static int[] fitFrameLocationX, fitFrameLocationY;
    static Dimension labelSize, legendLabelSize, spinnerSize, orbitalSpinnerSize, stellarSpinnerSize,bpSize;
    static Dimension bestFitSize, statSize, checkBoxSize, choiceBoxSize, spacerSize, controlSpinnerSize, lineWidthSpinnerSize, colorSelectorSize;
    static double xPlotMinRaw;
    static Color defaultBackground;
    static Color defaultOKForeground;
    static Color darkGreen = new Color(0, 155, 0);
    static Color darkYellow = new Color(255, 190, 0);
    static Border defaultSpinnerBorder;
    static Border grayBorder2 = new CompoundBorder(BorderFactory.createLineBorder(Color.GRAY, 2), new EmptyBorder(2, 2, 2, 2));
    static Border grayBorder = new CompoundBorder(BorderFactory.createLineBorder(Color.GRAY, 1), new EmptyBorder(2, 2, 2, 2));    //top,left,bottom,right
    static Border greenBorder = new CompoundBorder(BorderFactory.createLineBorder(Color.GREEN, 1), new EmptyBorder(1, 1, 1, 1));
    //        static Border greenBorder = BorderFactory.createLineBorder(Color.GREEN, 2);
    static Border lockBorder = new CompoundBorder(BorderFactory.createLineBorder(Color.ORANGE, 2), new EmptyBorder(2, 2, 2, 2));
    static Border convergedBorder = new CompoundBorder(BorderFactory.createLineBorder(new Color(0, 200, 0), 2), new EmptyBorder(2, 2, 2, 2));
    static Border failedBorder = new CompoundBorder(BorderFactory.createLineBorder(Color.RED, 2), new EmptyBorder(2, 2, 2, 2));

//----------------------------------------------------

    static JMenuBar mainmenubar;
    static JMenu filemenu;
    static JMenu preferencesmenu;
    static JMenu xaxismenu;
    static JMenu yaxismenu;
    static JMenu tablemenu;
    static JCheckBoxMenuItem opendatasetCB, openrefstarCB, openFitPanelsCB;
    static JCheckBoxMenuItem usewidedataCB, divideNotSubtractCB, useNelderMeadChi2ForDetrendCB;
    static JCheckBoxMenuItem rememberwindowlocationsCB, keepSeparateLocationsForFitWindowsCB;
    static JCheckBoxMenuItem showtooltipsCB;
    static JCheckBoxMenuItem usedefaultsettingsCB;
    static JCheckBoxMenuItem usepixelscaleCB;
    static JCheckBoxMenuItem useBoldedDatumCB, useUpdateStackCB;
    static JCheckBoxMenuItem xTicsCB, yTicsCB, xGridCB, yGridCB, xNumbersCB, yNumbersCB;
    static JRadioButtonMenuItem showXAxisNormalCB, showXAxisAsPhaseCB, showXAxisAsHoursSinceTcCB, showXAxisAsDaysSinceTcCB;
    static ButtonGroup xPhaseGroup;
    static ButtonGroup[] morelegendRadioGroup, detrendVarRadioGroup;

    static JMenuItem setephemerismenuitem, setrefstarhorzsizemenuitem;
    static JMenuItem changepixelscalemenuitem, setverticalmarkertextmenuitem;
    static JMenuItem savedatamenuitem, saveimagemenuitem, savedataconfigmenuitem, saveplotconfigmenuitem, opendatamenuitem, openplotconfigmenuitem, opendataconfigmenuitem, saveplottemplatemenuitem, openplottemplatemenuitem, opentablemenuitem, transposetablemenuitem, addnewcolsfromplotmenuitem, addnewcolsfromastroCCmenuitem;
    static JMenuItem backupAllAIJPrefsMenuItem, restoreAllAIJPrefsMenuItem, restoreDefaultAIJPrefsMenuItem;
    static JCheckBoxMenuItem usePriorityColumnsCB;
    static JMenuItem changePriorityColumnsmenuitem;
    static JMenuItem changemaxdatalengthmenuitem;

    static JTextField titleField, subtitleField, legendField, xlegendField, ylegendField;
    static JLabel statusLabel, xcolumnlabel, ycolumnslabel, ycolumnlabel, operatorlabel;
    static JLabel xcolumnlabelsub, ycolumnlabelsub, operatorlabelsub;
    static JLabel opcolumnlabel, markerlabel, markercolorlabel;
    static JLabel opcolumnlabelsub, markerlabelsub, markercolorlabelsub;
    static JSlider titlePosYSlider;
    static JSlider titlePosXSlider;
    static JSlider subtitlePosYSlider;
    static JSlider subtitlePosXSlider;
    static JSlider legendPosYSlider;
    static JSlider legendPosXSlider;
    static JComboBox<Object> xdatacolumndefault;

    static JRadioButton autoxButton, firstxButton, customxButton;
    static JRadioButton autoyButton, firstyButton, customyButton;
    static JRadioButton unphasedButton, dayssincetcButton, hourssincetcButton, orbitalphaseButton;

    static JRadioButton useGJDButton, useHJDButton, useBJDButton, useManualRaDecButton, useTableRaDecButton;
    static boolean useGJD = true, useHJD = false, useBJD = false, useTableRaDec = false;
    static ButtonGroup JDRadioGroup, RaDecRadioGroup;

    static SpinnerModel mmagrefsmodel;
    static JSpinner mmagrefsspinner;
    static SpinnerModel maxcurvesmodel, maxdetrendvarsmodel;
    static JSpinner maxcurvesspinner, maxdetrendvarsspinner;

    static JSpinner ymaxstepspinner, yminstepspinner;
    static JSpinner plotwidthstepspinner, plotheightstepspinner;

    static JPopupMenu ymaxsteppopup, yminsteppopup;
    static JPopupMenu plotwidthsteppopup, plotheightsteppopup, trimdataheadsteppopup, trimdatatailsteppopup;
    static JPopupMenu xaxispopup, yaxispopup, legendpopup;
    static JPopupMenu xsteppopup, T0steppopup, periodsteppopup, durationsteppopup;

    static SpinnerModel ymaxstepmodel, yminstepmodel;
    static SpinnerModel plotwidthstepmodel, plotheightstepmodel;
    static SpinnerModel xwidthspinnermodel, xminspinnermodel, xmaxspinnermodel;
    static SpinnerModel yminspinnermodel, ymaxspinnermodel, plotheightspinnermodel, plotwidthspinnermodel;
    static SpinnerModel xmultipliermodel, ymultipliermodel;
    static SpinnerModel trimdataheadmodel, trimdatatailmodel, trimdataheadstepmodel, trimdatatailstepmodel;
    static SpinnerModel vmarker1spinnermodel, vmarker2spinnermodel;
    static SpinnerModel xstepmodel, mfmarker1spinnermodel, dmarker1spinnermodel, dmarker2spinnermodel, dmarker3spinnermodel, dmarker4spinnermodel;
    static SpinnerModel T0spinnermodel, periodspinnermodel, durationspinnermodel;
    static SpinnerModel T0stepspinnermodel, periodstepspinnermodel, durationstepspinnermodel;

    static JSpinner xwidthspinner, xminspinner, xmaxspinner, xmultiplierspinner, ymultiplierspinner;
    static JSpinner yminspinner, ymaxspinner, plotheightspinner, plotwidthspinner;
    static JSpinner trimdataheadspinner, trimdatatailspinner, trimdataheadstepspinner, trimdatatailstepspinner;
    static JSpinner vmarker1spinner, vmarker2spinner;
    static JSpinner xstepspinner, mfmarker1spinner, dmarker1spinner, dmarker2spinner, dmarker3spinner, dmarker4spinner;
    static JSpinner T0stepspinner, periodstepspinner, durationstepspinner;
    static JSpinner T0spinner, periodspinner, durationspinner;

    //        static JCheckBox usextickscheckbox, useytickscheckbox, usexgridcheckbox;
//        static JCheckBox useygridcheckbox, usexnumberscheckbox, useynumberscheckbox;
    static JCheckBox showVMarker1CB, showVMarker2CB, twoxPeriodCB, oddNotEvenCB;
    static JCheckBox showMFMarkersCB, showDMarkersCB, useDMarker1CB, useDMarker4CB;

    static JButton moreybutton, closebutton, grabautoxbutton, grabautoybutton, updateplotbutton, addastrodatabutton, refStarButton, OKbutton;
    static String tableName;
    static String[] columns, unfilteredColumns, oldUnfilteredColumns;
    static String[] columnswd, columnsDetrend;

    static MeasurementTable table;
    static TextPanel tpanel;
    static JLabel dummylabel1, inputAverageOverSizespinnerlabel, mmagrefsspinnerlabel, dummylabel4;

    static ImageWindow plotWindow;
    static ImagePlus plotImage;
    //        static ImageProcessor ip;
    static ImageCanvas plotImageCanvas;
    static OverlayCanvas plotOverlayCanvas;
    static Panel plotbottompanel;
    static Label plotcoordlabel;
    static Vector<Roi> list;
    static GeneralPath path;
    static Roi roi;


    static int maxCurves, maxDetrendVars, maxFittedVars;
    static int mainFrameWidth, mainFrameHeight, subFrameWidth, subFrameHeight;
    static int[] detrendVarDisplayed;
    static int[] nFitTrim;
    static int maxColumnLength;
    static double vMarker1Value = 0.5, vMarker2Value = 0.7;
    static double xStep = 0.001;
    static double T0Step = 0.001, periodStep = 0.0001, durationStep = 0.01;
    static double dMarker1Value = 0.3, dMarker2Value = 0.5, dMarker3Value = 0.7, dMarker4Value = 0.9;
    static double mfMarker1Value = 0.6;
    static boolean showMFMarkers = false;
    static String[] xlabel;
    static String[] ylabel;
    static String[][] detrendlabel;
    static String[][] detrendlabelhold;
    static int[] xcolumn;
    static int[] ycolumn;
    static int[] errcolumn;
    static int[] operrcolumn;
    static int[][] detrendcolumn;
    static int[] xc1column, yc1column, xc2column, yc2column;
    static String[] oplabel;
    static int[] opcolumn;
    //        static String[] errlabel;
//        static String[] operrlabel;
    static boolean[] showErrors;  //user has requested to plot auto-error
    static boolean[] hasErrors;  //indicates error is available for a curve
    //        static boolean[] showErr; //retains showErrors from previous plot update
//        static boolean[] showOpErrors;
    static boolean[] hasOpErrors;  //indicates operator error is available for a curve
    static boolean[] lines;
    static boolean[] smooth;
    static boolean shiftIsDown;
    static int[] marker, residualSymbol;
    static Color[] color, modelColor, residualModelColor, residualColor;
    static int[] markerIndex;
    static int[] residualSymbolIndex;
    static int[] colorIndex;
    static int[] modelColorIndex;
    static int[] residualModelColorIndex;
    static int[] residualColorIndex;
    static int[] operatorIndex;
    static String[] cblabels;
    static boolean[] moreOptions;
    static double[] autoScaleFactor;
    static double[] autoScaleStep;
    static double[] autoShiftFactor;
    static double[] autoShiftStep;
    static double[] customScaleFactor;
    static double[] customScaleStep;
    static double[] customShiftFactor;
    static double[] customShiftStep;
    static double[][] detrendFactor;
    static double[][] detrendFactorStep;
    static double[] baseline;
    static double[] subtotalScaleFactor;
    static double[] subtotalShiftFactor;
    static double[] totalScaleFactor;
    static double[] totalShiftFactor;
    static double[] yWidth;
    static double[] xMinimum;
    static double[] xMaximum;
    static double[] yMinimum;
    static double[] yMn;
    static double[] yMaximum;
    static double[] yMx;
    static double[] yWidthOrig;
    static double[] yMidpoint;
    static double[] nullX = null;
    static double[] nullY = null;

    static boolean[] mmag;
    static boolean[] fromMag;
    static int[] normIndex;
    static int[] detrendFitIndex;
    public static int[][] detrendIndex;

    static boolean[] plotY;
    static boolean[] useColumnName;
    static boolean[] useLegend;
    static boolean[] force;
    static boolean[] ASInclude;

    static int[] inputAverageOverSize;

    static String[] legend;
    static String[] xyc1label;
    static String[] xyc2label;

    static JCheckBox[] usecurvebox;
    static JCheckBox[] errorcolumnbox;
    static JCheckBox[] operrorcolumnbox;
    static JCheckBox[] useDataAvgbox;
    static JCheckBox[] usemmagbox, fromMagBox;
    static JComboBox<ImageIcon>[] normtypecombobox;
    static JComboBox<ImageIcon>[] detrendtypecombobox;
    static JComboBox<String> jdcolumnbox, racolumnbox, deccolumnbox;
    static JCheckBox[] uselinesbox;
    static JCheckBox[] usesmoothbox;
    static JCheckBox[] autoscaleincludebox;
    static JCheckBox[] forcebox, shiftAboveBox, shiftBelowBox;

    static JPanel[] refStarPanel;
    static JLabel[] refStarLabel;
    public static JCheckBox[] refStarCB;
    static boolean[] isRefStar;
    private static boolean[] savedIsRefStar;
    static JTextField[] absMagTF;
    static double[] absMag;
    static boolean hasAbsMag;
    static int numAbsMagRefAps = 0;
    static boolean forceAbsMagDisplay;
    static boolean[] isStarSaturated;
    static boolean[] isStarOverLinear;
    static boolean showSaturationWarning;
    static double saturationWarningLevel;
    static boolean showLinearityWarning;
    static double linearityWarningLevel;
    static int numAps;

    static JComboBox<Object>[] xdatacolumn;
    static JComboBox<Object>[] ydatacolumn;
    static JComboBox<Object>[] operatorselection;
    static JComboBox<Object>[] operatorcolumn;
    static JComboBox<Object>[] detrendbox;
    static JComboBox<String>[] markersymbolselection;
    static JComboBox<Object>[] residualSymbolSelection;

    static JComboBox<String>[] markercolorselection, spectralTypeSelection;
    static JComboBox<Object>[] modelColorSelection, residualModelColorSelection;
    static JComboBox<Object>[] residualColorSelection;

    static SpinnerModel[] inputAverageOverSizespinnermodel;
    static SpinnerModel[] smoothlenspinnermodel;
    static JSpinner[] inputAverageOverSizespinner;
    static JSpinner[] smoothlenspinner;
    static JPanel[] morelegendradiopanelgroup;
    static JPanel[] autoscalepanelgroup;
    static JPanel[] customscalepanelgroup;
    static JPanel[] detrendpanelgroup, normpanelgroup;
    static JPanel[] savecolumnpanelgroup;
    static JRadioButton[] legendnoneButton;
    static JRadioButton[] legendcustomNameButton;
    static JRadioButton[] legendcolumnNameButton;
    static JRadioButton[][] detrendVarButton;
    static JRadioButton[] autoscaleButton;
    static JRadioButton[] customscaleButton;
    static ButtonGroup[] autoscaleRadioGroup;
    static JTextField[] morelegendField;
    static SpinnerModel[] autoshiftmodel;
    static SpinnerModel[] autoshiftstepmodel;
    static SpinnerModel[] autoscalemodel;
    static SpinnerModel[] autoscalestepmodel;
    static SpinnerModel[] customshiftmodel;
    static SpinnerModel[] customshiftstepmodel;
    static SpinnerModel[] customscalemodel;
    static SpinnerModel[] customscalestepmodel;
    static SpinnerModel[] detrendfactormodel;
    static SpinnerModel[] detrendfactorstepmodel;
    static JSpinner[] autoshiftspinner;
    static JSpinner[] autoshiftstepspinner;
    static JSpinner[] autoscalespinner;
    static JSpinner[] autoscalestepspinner;
    static JSpinner[] customshiftspinner;
    static JSpinner[] customshiftstepspinner;
    static JSpinner[] customscalespinner;
    static JSpinner[] customscalestepspinner;
    static JSpinner[] detrendfactorspinner;
    static JSpinner[] detrendfactorstepspinner;
    static JPopupMenu[] autoscalesteppopup;
    static JPopupMenu[] autoshiftsteppopup;
    static JPopupMenu[] customscalesteppopup;
    static JPopupMenu[] customshiftsteppopup;
    static JPopupMenu[] detrendfactorsteppopup;
    static JPanel[] autoscalesteppanel;
    static JPanel[] customscalesteppanel;
    static JPanel[] autoshiftsteppanel;
    static JPanel[] customshiftsteppanel;
    static JPanel[] detrendfactorsteppanel;
    static JLabel[] autoscalesteplabel;
    static JLabel[] autoshiftsteplabel;
    static JLabel[] customscalesteplabel;
    static JLabel[] customshiftsteplabel;
    static JLabel[] detrendfactorsteplabel;
    static JLabel[] curvelabel;
    static JLabel[] othercurvelabel;
    static JPanel[] grabautopanel;
    static JButton[] grabautobutton, savecolumnbutton;
    static ImageIcon forceIcon;
    static ImageIcon configureIcon;
    static ImageIcon helpIcon;
    static ImageIcon plotIcon;
    static ImageIcon editTextIcon, insertColumnIcon, copyVMarkersIcon, copyVMarkersInvertedIcon;
    static JButton legendconfigureButton;
    static JButton vmarker1edittextButton, vmarker2edittextButton;
    static JButton xlegendconfigureButton;
    static JButton ylegendconfigureButton;
    static JButton legendslabelconfigureButton;
    static JPanel legendgroup;
    static JPanel mainpanelgroupa;

    static String[] markers;
    static String[] residualSymbols;
    static String[] colors;
    static ImageIcon[] normiconlist;
    static ImageIcon[] detrendiconlist;

    static String[] operators;

    static String[] opSymbol;

    static String[] spinnerscalelist;

    static String[] integerspinnerscalelist;

    static TimerTask delayedUpdateTask;
    static java.util.Timer delayedUpdateTimer;

    static AstroConverter acc;

    static String fontName = Font.DIALOG;
    static Font p8 = new Font(fontName, Font.PLAIN, 8);
    static Font p9 = new Font(fontName, Font.PLAIN, 9);
    static Font p10 = new Font(fontName, Font.PLAIN, 10);
    static Font p11 = new Font(fontName, Font.PLAIN, 11);
    static Font p12 = new Font(fontName, Font.PLAIN, 12);
    static Font b11 = new Font(fontName, Font.BOLD, 11);
    static Font b12 = new Font(fontName, Font.BOLD, 12);
    static Font b14 = new Font(fontName, Font.BOLD, 14);

    static Color mainBorderColor = new Color(118, 142, 229);
    static Color subBorderColor = Color.LIGHT_GRAY;
    static Color defaultCBColor = Color.LIGHT_GRAY;

    static TitledBorder dataSectionBorder;
    static String fitFormat = "#####0.0########";
    static String oneDotThreeFormat = "#####0.000";
    static DecimalFormat uptoNinePlaces = new DecimalFormat("########0.#########", IJU.dfs);
    static DecimalFormat uptoEightPlaces = new DecimalFormat("#####0.########", IJU.dfs);
    static DecimalFormat uptoSixPlaces = new DecimalFormat("#####0.######", IJU.dfs);
    static DecimalFormat uptoFivePlaces = new DecimalFormat("#####0.#####", IJU.dfs);
    static DecimalFormat uptoThreePlaces = new DecimalFormat("#####0.###", IJU.dfs);
    static DecimalFormat onetoThreePlaces = new DecimalFormat("#####0.0##", IJU.dfs);
    static DecimalFormat uptoTwoPlaces = new DecimalFormat("#####0.##", IJU.dfs);
    static DecimalFormat onePlaces = new DecimalFormat("#####0.0", IJU.dfs);
    static DecimalFormat twoDigits = new DecimalFormat("00", IJU.dfs);
    static DecimalFormat fourDigits = new DecimalFormat("0000", IJU.dfs);
    static DecimalFormat twoDigitsOnePlace = new DecimalFormat("00.0", IJU.dfs);
    static DecimalFormat twoDigitsTwoPlaces = new DecimalFormat("00.00", IJU.dfs);
    static DecimalFormat twoDigitsFivePlaces = new DecimalFormat("00.00000", IJU.dfs);
    static DecimalFormat threeDigitsTwoPlaces = new DecimalFormat("##0.00", IJU.dfs);
    static DecimalFormat fiveDigitsOnePlace = new DecimalFormat("####0.0", IJU.dfs);
    static DecimalFormat ninePlaces = new DecimalFormat("######0.000000000", IJU.dfs);
    static DecimalFormat twoPlaces = new DecimalFormat("######0.00", IJU.dfs);
    static DecimalFormat fourPlaces = new DecimalFormat("0.0000", IJU.dfs);
    static DecimalFormat threePlaces = new DecimalFormat("######0.000", IJU.dfs);
    static DecimalFormat sixPlaces = new DecimalFormat("######0.000000", IJU.dfs);
    static DecimalFormat detrendParameterFormat = new DecimalFormat("######0.000000000000", IJU.dfs);
    private static boolean[] binDisplay = new boolean[maxCurves];//todo preferences
    private static JPanel[] displayBinningPanel = new JPanel[maxCurves];
    private static ArrayList<Pair.GenericPair<Double, JSpinner>> minutes = new ArrayList<>(maxCurves);
    private static boolean showOutBinRms = true;
    private static double[] outBinRms;
    private static boolean saveSeeingProfileStack;
    private static String seeingProfileStackSuffix;

    public void run(String inTableNamePlusOptions) {
        boolean useAutoAstroDataUpdate = false;
        String inTableName = "";
        String[] inOptions = inTableNamePlusOptions.split(",");
        if (inOptions.length > 0) inTableName = inOptions[0];
        if (inOptions.length > 1) useAutoAstroDataUpdate = true;
        Locale.setDefault(IJU.locale);
        //SET DEFAULT SYSTEM LOOK AND FEEL
        UIHelper.setLookAndFeel();
        if (IJ.isWindows()) {
            JLabel testlabel = new JLabel("test");
            p11 = testlabel.getFont();
            String winFont = p11.getFontName();
            p8 = new Font(winFont, Font.PLAIN, 8);
            p9 = new Font(winFont, Font.PLAIN, 9);
            p10 = new Font(winFont, Font.PLAIN, 10);
            p11 = new Font(winFont, Font.PLAIN, 11);
            p12 = new Font(winFont, Font.PLAIN, 12);
            b11 = new Font(winFont, Font.BOLD, 11);
            b12 = new Font(winFont, Font.BOLD, 12);
            b14 = new Font(winFont, Font.BOLD, 14);
        } else if (IJ.isLinux()) {
            p8 = new Font(fontName, Font.PLAIN, 8);
            p9 = new Font(fontName, Font.PLAIN, 8);
            p10 = new Font(fontName, Font.PLAIN, 9);
            p11 = new Font(fontName, Font.PLAIN, 10);
            p12 = new Font(fontName, Font.PLAIN, 11);
            b11 = new Font(fontName, Font.BOLD, 10);
            b12 = new Font(fontName, Font.BOLD, 11);
            b14 = new Font(fontName, Font.BOLD, 13);
        }
        else {
            p8 = new Font(fontName, Font.PLAIN, 8);
            p9 = new Font(fontName, Font.PLAIN, 8);
            p10 = new Font(fontName, Font.PLAIN, 9);
            p11 = new Font(fontName, Font.PLAIN, 10);
            p12 = new Font(fontName, Font.PLAIN, 11);
            b11 = new Font(fontName, Font.BOLD, 10);
            b12 = new Font(fontName, Font.BOLD, 11);
            b14 = new Font(fontName, Font.BOLD, 13);
        }
        dataSectionBorder = BorderFactory.createTitledBorder(BorderFactory.createLineBorder(mainBorderColor, 1), "Data ()", TitledBorder.LEFT, TitledBorder.TOP, b12, Color.DARK_GRAY);
        plotAutoMode = Prefs.get("plot.automode", plotAutoMode);
        panelsUpdating = false;
        if (plotAutoMode) {
            updatePlot(updateAllFits());
            plotAutoMode = false;
            Prefs.set("plot.automode", false);
            return;
        }

        if (mainFrame == null || !mainFrame.isVisible()) {
            if (mainFrame != null) {
                mainFrame.dispose();
                subFrame.dispose();
                refStarFrame.dispose();
                mainFrame = null;
                subFrame = null;
                refStarFrame = null;
            }
            initializeVariables();
            setupArrays();
            useDefaultSettings = Prefs.get("plot2.useDefaultSettings", useDefaultSettings);
            if (useDefaultSettings) {
                useDefaultSettings = false;
                Prefs.set("plot2.useDefaultSettings", useDefaultSettings);
            } else { getPreferences(); }
        }

        // FIND ALL TABLES
        if (inTableName != null && !inTableName.equals("")) requestedTableName = inTableName;
        findTables(false, useAutoAstroDataUpdate);
    }

    static void findTables(boolean forceUpdate) {
        findTables(forceUpdate, false);
    }

    static void findTables(boolean forceUpdate, boolean useAutoAstroDataUpdate) {
        String[] tables = MeasurementTable.getMeasurementTableNames();
        if (tables == null || tables.length == 0) {
            makeDummyTable();
        } else {
            //filter out B&C, Log, Errors windows if they exist
            int j = 0;
            for (String value : tables)
                if (value.equals("B&C") || value.startsWith("Log") || value.endsWith(" Log") || value.equals("Errors")) j++;
            String[] filteredTables = new String[tables.length - j];
            j = 0;
            for (String s : tables)
                if (!s.equals("B&C") && !s.startsWith("Log") && !s.equals("Errors") && !s.endsWith(" Log")) {
                    filteredTables[j] = s;
                    j++;
                }
            if (requestedTableName != null) {
                for (String filteredTable : filteredTables) {
                    if (requestedTableName.equals(filteredTable)) {
                        tableName = requestedTableName;
                        requestedTableName = null;
                        setTable(MeasurementTable.getTable(tableName), forceUpdate, useAutoAstroDataUpdate);
                        break;
                    }
                }
                if (requestedTableName != null) {
                    requestedTableName = null;
                    makeDummyTable();
                }
            } else if (filteredTables.length == 0) {
                makeDummyTable();
            } else if (filteredTables.length == 1) {
                tableName = filteredTables[0];
                setTable(MeasurementTable.getTable(tableName), forceUpdate, useAutoAstroDataUpdate);
            } else                            // IF MORE THAN ONE, ASK WHICH TABLE SHOULD BE USED
            {
                GenericDialog gd = new GenericDialog("Plot Table Columns");
                gd.addMessage("Select table to be plotted.");
                gd.addChoice("Table to be plotted", filteredTables, "Measurements");
                gd.showDialog();
                if (gd.wasCanceled()) {
                    makeDummyTable();
                } else {
                    tableName = gd.getNextChoice();
                    setTable(MeasurementTable.getTable(tableName), forceUpdate, useAutoAstroDataUpdate);
                }
            }
        }
    }

    static public void setTable(MeasurementTable inTable, boolean forceUpdate) {
        setTable(inTable, forceUpdate, false);
    }

    static public void setTable(MeasurementTable inTable, boolean forceUpdate, boolean useAutoAstroDataUpdate) {
        if (table != null) {
            table.removeListeners();
        }
        table = inTable;
        FitOptimization.clearCleanHistory();

        table.addListener(FitOptimization::clearCleanHistory);

        if (table == null) {
            makeDummyTable();
        } else {
            tableName = table.shortTitle();
            tpanel = MeasurementTable.getTextPanel(MeasurementTable.longerName(tableName));
            dataSectionBorder.setTitle("Data (" + MeasurementTable.shorterName(tableName) + ")");
            unfilteredColumns = table.getColumnHeadings().split("\t");
            if (unfilteredColumns.length == 0) {
                IJ.showMessage("No data columns to plot.");
                makeDummyTable();
            } else {
                updateColumnLists();
                Prefs.set("plot2.tableName", tableName);
//                if (table != null && !tableName.equals("No Table Selected") && useAutoAstroDataUpdate && autoAstroDataUpdate && addAstroDataFrameWasShowing && OKbutton != null) {
//                    OKbutton.doClick();
//                }
                if (table != null) {
                    loadConfigOfOpenTable(table.getFilePath());
                    table.show();
                    WindowManager.getFrame(table.shortTitle()).setVisible(true);
                    forceUpdate = true;
                }
                finishSetup(forceUpdate, useAutoAstroDataUpdate);
            }
        }
    }

    static public MeasurementTable getTable() {
        return table;
    }

    static void finishSetup(boolean forceUpdate){
        finishSetup(forceUpdate, false);
    }
    static void finishSetup(boolean forceUpdate, boolean useAutoAstroDataUpdate) {
        //if (oldUnfilteredColumns==null) IJ.log("old unfiltercolumns is null");
        if (mainFrame == null || !mainFrame.isVisible()) {
            showMainJPanel();
        }
//        if (!Arrays.equals(unfilteredColumns, oldUnfilteredColumns))
//            {
//            IJ.log("columns are different");
//            for (int i=0; i<(oldUnfilteredColumns==null?0:oldUnfilteredColumns.length); i++)
//                IJ.log("oldUnfilteredColumn["+i+"]="+oldUnfilteredColumns[i]);
//            for (int i=0; i<(unfilteredColumns==null?0:unfilteredColumns.length); i++)
//                IJ.log("unfilteredColumn["+i+"]="+unfilteredColumns[i]);
//            }
        else if (forceUpdate || oldUnfilteredColumns==null || !Arrays.equals(unfilteredColumns, oldUnfilteredColumns)) {
            updatePanels();
        }
        oldUnfilteredColumns = unfilteredColumns.clone();
        updatePlot(updateAllFits(), useAutoAstroDataUpdate);
    }

//------------------------------------------------------------------------------

    static public void makeDummyTable() {
        tableName = "No Table Selected";
        table = null; //MeasurementTable.getTable(tableName);
        FitOptimization.clearCleanHistory();
        dataSectionBorder.setTitle("Data (" + MeasurementTable.shorterName(tableName) + ")");
        unfilteredColumns = new String[1];
        unfilteredColumns[0] = "";
        columns = unfilteredColumns.clone();
        columnsDetrend = unfilteredColumns.clone();
        columnswd = unfilteredColumns.clone();
        finishSetup(true);
//        Prefs.set("plot2.tableName",tableName);
    }

    static public void updateColumnLists() {
        int j = -1;
        for (String column : unfilteredColumns) {
            if (column.equals("Label") || column.equals("image")) j++;
        }
        columns = new String[unfilteredColumns.length - j];
        j = 1;
        columns[0] = "";
        for (String unfilteredColumn : unfilteredColumns) {
            if (!unfilteredColumn.equals("Label") && !unfilteredColumn.equals("image")) {
                columns[j] = unfilteredColumn;
                j++;
            }
        }

        columnsDetrend = new String[columns.length + 1];
        columnsDetrend[0] = columns[0];
        columnsDetrend[1] = "Meridian_Flip";
        System.arraycopy(columns, 1, columnsDetrend, 2, columnsDetrend.length - 2);

        if (prioritizeColumns) sortColumns();

        columnswd = new String[columns.length + 1];
        columnswd[0] = columns[0];
        columnswd[1] = "default";
        System.arraycopy(columns, 1, columnswd, 2, columnswd.length - 2);
    }

    static public void sortColumns() {
        String[] pcols = priorityColumns.split(",");
        if (pcols.length < 1 || columns.length < 2) return;
        String[] cols = columns.clone();
        int j = 1;
        String pcol;
        for (String value : pcols) {
            pcol = value.toLowerCase().trim();
            if (pcol.equals("rel_flux") || pcol.equals("rel_flux_") || pcol.equals("rel_flux_t") || pcol.equals("rel_flux_c")) {     //treat rel_flux, rel_flux_T, rel_flux_C as a special cases since rel_flux matches rel_flux_err, etc.
                for (int i = 1; i < cols.length; i++) {
                    if (cols[i].toLowerCase().startsWith("rel_flux_t") || cols[i].toLowerCase().startsWith("rel_flux_c")) {
                        columns[j] = cols[i];
                        cols[i] = "";
                        j++;
                    }

                }
            } else {
                for (int i = 1; i < cols.length; i++) {
                    if (cols[i].toLowerCase().startsWith(pcol)) {
                        columns[j] = cols[i];
                        cols[i] = "";
                        j++;
                    }
                }
            }

        }
        for (int i = 1; i < cols.length; i++) //get all non-priority columns
        {
            if (!cols[i].equals("")) {
                columns[j] = cols[i];
                j++;
            }

        }

        pcols = priorityDetrendColumns.split(",");
        if (pcols.length < 1 || columnsDetrend.length < 3) return;
        cols = columnsDetrend.clone();
        j = 2;
        for (String s : pcols) {
            pcol = s.toLowerCase().trim();
            if (pcol.equals("rel_flux") || pcol.equals("rel_flux_") || pcol.equals("rel_flux_t") || pcol.equals("rel_flux_c")) {
                for (int i = 2; i < cols.length; i++) {
                    if (cols[i].toLowerCase().startsWith("rel_flux_t") || cols[i].toLowerCase().startsWith("rel_flux_c")) {
                        columnsDetrend[j] = cols[i];
                        cols[i] = "";
                        j++;
                    }

                }
            } else {
                for (int i = 2; i < cols.length; i++) {
                    if (cols[i].toLowerCase().startsWith(pcol)) {
                        columnsDetrend[j] = cols[i];
                        cols[i] = "";
                        j++;
                    }
                }
            }

        }
        for (int i = 2; i < cols.length; i++) {
            if (!cols[i].equals("")) {
                columnsDetrend[j] = cols[i];
                j++;
            }

        }

    }


    static public void updatePanels() {
        savePreferences();
        int timeout = 31;
        while (updatePlotRunning && timeout > 0) {
            IJ.wait(100);
            timeout--;
        }
        if (timeout != 31) IJ.log("Waited "+(timeout*0.1)+" seconds for plot to finish before closing MP panels.");
        closeFitFrames();
        if (mainFrame.isVisible()) {
            if (subFrame.isVisible()) {
                subFrameLocationX = subFrame.getLocation().x;
                subFrameLocationY = subFrame.getLocation().y;
                subFrameWidth = subFrame.getWidth();
                subFrameHeight = subFrame.getHeight();
                Prefs.set("plot2.subFrameLocationX", subFrameLocationX);
                Prefs.set("plot2.subFrameLocationY", subFrameLocationY);
                subscrollpane.removeAll();
                subFrame.remove(subscrollpane);
            }
            if (refStarFrame != null) {
                refStarPanelWasShowing = refStarFrame.isVisible();
                refStarScrollPane.removeAll();
                rebuildRefStarJPanel();
            } else {
                refStarPanelWasShowing = false;
            }
            if (addAstroDataFrame != null) {
                addAstroDataFrameWasShowing = addAstroDataFrame.isVisible();
                closeAddAstroDataFrame();
            } else {
                addAstroDataFrameWasShowing = false;
            }
            if (acc != null) {
                acc.saveAndClose();
            }
            mainFrameLocationX = mainFrame.getLocation().x;
            mainFrameLocationY = mainFrame.getLocation().y;
            mainFrameWidth = mainFrame.getWidth();
            mainFrameHeight = mainFrame.getHeight();
            Prefs.set("plot2.mainFrameLocationX", mainFrameLocationX);
            Prefs.set("plo2.mainFrameLocationY", mainFrameLocationY);
            mainFrame.remove(mainscrollpane);
        }
        showMainJPanel();
        repaintFrame(mainFrame);
        repaintFrame(subFrame);
        oldUnfilteredColumns = unfilteredColumns.clone();
    }

    static void repaintFrame(Frame frame) {
        frame.pack();
        frame.validate();
        frame.repaint();
    }

    static public void setPlotAutoMode(boolean mode) {
        plotAutoMode = mode;
    }

    static public void clearPlot() {
        checkAndLockTable();
        table = new MeasurementTable(tableName);
        FitOptimization.clearCleanHistory();
        table.setLock(false);
//        table.show();
        if (plot != null) {
            plotImage = WindowManager.getImage("Plot of " + tableName);
            if (plotImage != null) {
                plot = new Plot("Plot of " + tableName, xlab, ylab, plotOptions);
                plot.setSize(plotSizeX, plotSizeY);
                plot.setLimits(plotMinX, plotMaxX, plotMinY, plotMaxY);
                plot.setJustification(Plot.CENTER);
                if (useTitle) {
                    plot.changeFont(new java.awt.Font("Dialog", java.awt.Font.PLAIN, 18));
                    renderTitle();
                }
                if (useSubtitle) {
                    plot.changeFont(new java.awt.Font("Dialog", java.awt.Font.PLAIN, 14));
                    renderSubtitle();
                }
                plot.draw();
                ImageProcessor ip = plot.getProcessor();
                plotImage.setProcessor("Plot of " + tableName, ip);
            }
        } else {
            IJ.showStatus("No plot to clear");
        }
//        IJ.log("deleting rows");
//        for (int i=table.getCounter(); i>=0; i--)
//            {
//            IJ.log("deleting row "+i);
//            table.deleteRow(i);
//            }
//        table.show();
    }

    /**
     * Starts timer which handles simultaneous updatePlot requests.
     */
    static protected void startDelayedUpdateTimer(final boolean[] updateFit, final boolean useAutoAstroDataUpdate) {
        try {
            if (delayedUpdateTimer != null) {
                delayedUpdateTimer.cancel();

            }
            if (delayedUpdateTask != null) {
                delayedUpdateTask.cancel();
            }

            delayedUpdateTask = new TimerTask() {
                public void run() {
                    if (!updatePlotRunning && !awaitingScheduledPlotUpdate) updatePlot(updateFit, useAutoAstroDataUpdate);
                    setWaitingForPlot(false);
                }

            };
            delayedUpdateTimer = new java.util.Timer();
            delayedUpdateTimer.schedule(delayedUpdateTask, 200);
        } catch (Exception e) {
            //IJ.showMessage ("Error starting delayed plot update timer : "+e.getMessage());
        }
    }

    public static void setWaitingForPlot(boolean b) {
        synchronized (lock) {
            awaitingScheduledPlotUpdate = b;
        }
    }

    // For macro users
    public static void updatePlot() {
        updatePlot(updateAllFits(), false);
    }

    // For macro users
    public static void updatePlot(int i) {
        updatePlot(updateOneFit(i), false);
    }


    //------------------------UPDATEPLOT()---------------------------------------
    static public void updatePlot(boolean[] updateFit) {
        updatePlot(updateFit, false);
    }

    static public void updatePlot(boolean[] updateFit, boolean useAutoAstroDataUpdate) {
//                IJ.log("table "+(table == null?"==":"!=")+" null");
//                IJ.log("table "+(table != null && table.isLocked()?"is locked":"is unlocked"));
//                IJ.log("Plot update is "+(updatePlotEnabled?"enabled":"not enabled"));
//                IJ.log("Plot update is "+(updatePlotRunning?"running":"not running"));
        if ((table != null && table.isLocked()) || !updatePlotEnabled || updatePlotRunning) {
//                    IJ.log("starting delayed updatePlot() timer");
            setWaitingForPlot(true);
            startDelayedUpdateTimer(updateFit, false);
            return;
        }


        if (table == null || tableName.equals("No Table Selected")) {
            updatePlotRunning = false;
            if (table != null) table.setLock(false);
            return;
        }

        updatePlotRunning = true;

        if (table != null && !tableName.equals("No Table Selected") && useAutoAstroDataUpdate && autoAstroDataUpdate && addAstroDataFrameWasShowing && OKbutton != null) {
            autoAstroDataUpdateRunning = true;
            OKbutton.doClick();
            autoAstroDataUpdateRunning = false;

        }

        unfilteredColumns = table.getColumnHeadings().split("\t");
        if (!Arrays.equals(unfilteredColumns, oldUnfilteredColumns)) {
            updatePlotRunning = false;
            if (table != null) table.setLock(false);
            updateColumnLists();
            updatePanels();
        }

        checkAndLockTable();
//                table = MeasurementTable.getTable (tableName);
//                tpanel = MeasurementTable.getTextPanel(MeasurementTable.longerName(tableName));



        // This is a hack to fix the plot being cached(?) somewhere.
        // To see demonstrate the broken behavior fixed by this, move the (sub)title and
        // then resize the window, the position will reset until another position (such as the legend)
        // is changed, at which point the (sub)title position returns to the new value.
        if (plot != null) {
            plot.dispose();
        }

        n = table.getCounter();
        if (n < 1) {
//                        clearPlot();
            updatePlotRunning = false;
            if (table != null) table.setLock(false);
            return;
        }
        if (n > maxColumnLength) {
            maxColumnLength = Math.max(n, 2 * maxColumnLength);
            setupDataBuffers();
        }

        vMarker1Value = Prefs.get("plot.vMarker1Value", vMarker1Value);
        vMarker2Value = Prefs.get("plot.vMarker2Value", vMarker2Value);

        //Make sure all panels have been created. If not created within 5 secs, abort.
        if (fitPanel[maxCurves-1] == null){
            for (int i=0; i<51; i++){
                IJ.wait(100);
                if (fitPanel[maxCurves-1] != null){
                    //IJ.log("fitPanel wait = "+(i*100)+"msec");
                    break;
                }
                if (i == 50) {
                    IJ.log("fitPanel wait = "+(i*100)+"msec");
                    return;
                }
            }
        }
        savePreferences();
        updateSaturatedStars();
        for (int curve = 0; curve < maxCurves; curve++) {
            if (inputAverageOverSize[curve] < 1) {
                inputAverageOverSize[curve] = 1;
                inputAverageOverSizespinner[curve].setValue(1);
            }
        }

        holdExcludedHeadSamples = excludedHeadSamples;
        holdExcludedTailSamples = excludedTailSamples;
        if (excludedHeadSamples + excludedTailSamples >= n)  //Handle case for more samples excluded than in dataset
        {
            excludedHeadSamples = excludedHeadSamples < n ? excludedHeadSamples : n - 1;
            excludedTailSamples = n - excludedHeadSamples - 1;
        }
        excluded = excludedHeadSamples + excludedTailSamples;

        netT0 = (twoxPeriod && oddNotEven) ? T0 - period : T0;
        netPeriod = twoxPeriod ? 2 * period : period;

        int magSign = negateMag ? 1 : -1;
        for (int i = 0; i < maxCurves; i++) {
            marker[i] = markerOf(markerIndex[i]);
            residualSymbol[i] = markerOf(residualSymbolIndex[i]);
            color[i] = colorOf(colorIndex[i]);
            modelColor[i] = colorOf(modelColorIndex[i]);
            residualModelColor[i] = colorOf(residualModelColorIndex[i]);
            residualColor[i] = colorOf(residualColorIndex[i]);
            sigma[i] = 0.0;
        }

        // INITIALIZE BASELINE FOR RELATIVE MMAG CALCULATIONS
        for (int i = 0; i < maxCurves; i++) {
            baseline[i] = 0;
        }
        // GET DATA

        if (n > x[0].length) {
            nn = new int[maxCurves];
            xlabel2 = new String[maxCurves];
            x = new double[maxCurves][n];
            y = new double[maxCurves][n];
            yerr = new double[maxCurves][n];
            yop = new double[maxCurves][n];
            yoperr = new double[maxCurves][n];
            detrend = new double[maxCurves][maxDetrendVars][n];
            xc1 = new double[maxCurves][n];
            xc2 = new double[maxCurves][n];
            yc1 = new double[maxCurves][n];
            yc2 = new double[maxCurves][n];
//                    detrendVars = new double [maxCurves][n];
            detrendXs = new double[maxCurves][n];
            detrendYs = new double[maxCurves][n];
            detrendYEs = new double[maxCurves][n];
            updateFit = updateAllFits();
        }

        xMultiplierFactor = Math.pow(10, xExponent);
        yMultiplierFactor = Math.pow(10, yExponent);

        int[] detrendVarsUsed = new int[maxCurves];

        for (int curve = 0; curve < maxCurves; curve++) {
            nn[curve] = (n - excluded) / inputAverageOverSize[curve];
            nnr[curve] = (n - excluded) % inputAverageOverSize[curve];
            if (nnr[curve] > 0) nn[curve]++;
            detrendVarsUsed[curve] = 0;
//                        xModel1[curve] = null;
//                        xModel2[curve] = null;
//                        yModel1[curve] = null;
//                        yModel2[curve] = null;
//                        coeffs[curve] = null;
        }

        for (int curve = 0; curve < maxCurves; curve++) {
            if (xlabel[curve].trim().length() == 0 || (xlabel[curve].equalsIgnoreCase("default") && xlabeldefault.trim().length() == 0)) {
                for (int j = 0; j < nn[curve]; j++)
                    x[curve][j] = j + 1;
                xlabel2[curve] = "Sample Number";
            } else {
                if (xlabel[curve].equalsIgnoreCase("default")) {
                    xcolumn[curve] = table.getColumnIndex(xlabeldefault);
                    if (xcolumn[curve] == ResultsTable.COLUMN_NOT_FOUND) {
                        xcolumn[curve] = 0;
                        plotY[curve] = false;
                        usecurvebox[curve].setSelected(false);
                        savecolumnbutton[curve].setEnabled(plotY[curve]);
//                                    if (xlabeldefault.trim().length() != 0)
//                                        IJ.showMessage ("Cannot access default X-axis table column "+xlabeldefault+" !");
//                                    xlabeldefault = columns[0];
//                                    return;
                    }
                    xlabel2[curve] = xlabeldefault.trim();
                } else {
                    xcolumn[curve] = table.getColumnIndex(xlabel[curve]);
                    if (xcolumn[curve] == ResultsTable.COLUMN_NOT_FOUND) {
                        xcolumn[curve] = 0;
                        plotY[curve] = false;
                        usecurvebox[curve].setSelected(false);
                        savecolumnbutton[curve].setEnabled(plotY[curve]);
//                                        if (xlabel[curve].trim().length() != 0)
//                                            IJ.showMessage ("Cannot access X-axis table column "+xlabel[curve]+" for dataset "+(curve+1)+"!");
//                                        xlabel[curve] = "default";
//                                        xdatacolumn[curve].setSelectedItem(xlabel[curve]);
//                                        return;
                    }
                    xlabel2[curve] = xlabel[curve].trim();
                }

                if (plotY[curve]) {
                    int bucketSize = inputAverageOverSize[curve];

                    for (int j = 0; j < nn[curve]; j++) {
                        double xin;
                        int numNaN = 0;
                        if (nnr[curve] > 0 && j == nn[curve] - 1) {
                            bucketSize = nnr[curve];
                        } else {
                            bucketSize = inputAverageOverSize[curve];
                        }
                        x[curve][j] = 0;
                        for (int k = excludedHeadSamples; k < (bucketSize + excludedHeadSamples); k++) {
                            xin = table.getValueAsDouble(xcolumn[curve], j * inputAverageOverSize[curve] + k);
                            if (Double.isNaN(xin)) {
                                numNaN += 1;
                                if (numNaN == bucketSize) {
                                    bucketSize = 1;
                                    x[curve][j] = Double.NaN;
                                    numNaN = 0;
                                    break;
                                }
                            } else {
                                x[curve][j] += xin;
                            }
                        }
                        bucketSize -= numNaN;
                        x[curve][j] = x[curve][j] * xMultiplierFactor / (double) bucketSize;
                    }
                    if (xlabel2[curve].startsWith("J.D.-2400000")) {
                        for (int j = 0; j < nn[curve]; j++) {
                            x[curve][j] += 2400000;
                        }
                    }
                }
            }
        }
        for (int curve = 0; curve < maxCurves; curve++) {
            if (plotY[curve]) {
                if (ylabel[curve].trim().length() == 0) {
                    for (int j = 0; j < nn[curve]; j++)
                        y[curve][j] = j + 1;
                } else {
                    ycolumn[curve] = table.getColumnIndex(ylabel[curve]);
                    if (ycolumn[curve] == ResultsTable.COLUMN_NOT_FOUND) {
//                                                if (ylabel[curve].trim().length() != 0)
//                                                    IJ.log ("Cannot access Y"+curve+" table column "+ylabel[curve]+" !");
                        ycolumn[curve] = 0;
                        plotY[curve] = false;
                        usecurvebox[curve].setSelected(false);
                        savecolumnbutton[curve].setEnabled(plotY[curve]);
                    } else if (operatorIndex[curve] == 5)  //calculate distance
                    {
                        xc1column[curve] = table.getColumnIndex(getPositionColumn(ylabel[curve], "X"));
                        yc1column[curve] = table.getColumnIndex(getPositionColumn(ylabel[curve], "Y"));
                        xyc1label[curve] = getSourceID(ylabel[curve]);
                        if (xc1column[curve] == ResultsTable.COLUMN_NOT_FOUND) {
                            if (getPositionColumn(ylabel[curve], "X").trim().length() != 0) {
                                IJ.showMessage("Cannot access " + getPositionColumn(ylabel[curve], "X") + " source position data column for distance calculation !");
                            }
                            operatorIndex[curve] = 0;
                            //operatorselection[curve].setSelectedIndex(operatorIndex[curve]);
                            plotY[curve] = false;
                            usecurvebox[curve].setSelected(false);
                            savecolumnbutton[curve].setEnabled(plotY[curve]);
                        }
                        if (yc1column[curve] == ResultsTable.COLUMN_NOT_FOUND) {
                            if (getPositionColumn(ylabel[curve], "Y").trim().length() != 0) {
                                IJ.showMessage("Cannot access " + getPositionColumn(ylabel[curve], "Y") + " source position data column for distance calculation !");
                            }
                            operatorIndex[curve] = 0;
                            //operatorselection[curve].setSelectedIndex(operatorIndex[curve]);
                            plotY[curve] = false;
                            usecurvebox[curve].setSelected(false);
                            savecolumnbutton[curve].setEnabled(plotY[curve]);
                        }
                    }
                    if (detrendFitIndex[curve] != 0) {
                        for (int v = 0; v < maxDetrendVars; v++) {
                            if (detrendIndex[curve][v] > 1 || (!detrendVarAllNaNs[curve][v] && detrendIndex[curve][v] != 1)) {
                                detrendcolumn[curve][v] = table.getColumnIndex(detrendlabel[curve][v]);

                                if (detrendcolumn[curve][v] == ResultsTable.COLUMN_NOT_FOUND) {
                                    detrendIndex[curve][v] = 0;
                                    if (detrendVarDisplayed[curve] == v) detrendbox[curve].setSelectedIndex(0);
                                } else {
                                    detrendVarsUsed[curve]++;
                                }
                            } else if (detrendIndex[curve][v] == 1)  //Meridian Flip Detrend Selected
                            {
                                detrendVarsUsed[curve]++;
                            }
                        }
                    }
//                                        if (showErrors[curve] == true || operatorIndex[curve] == 6)
//                                                {
                    errcolumn[curve] = ResultsTable.COLUMN_NOT_FOUND;
                    if (operatorIndex[curve] == 6)   //custom error
                    {
                        errcolumn[curve] = table.getColumnIndex(oplabel[curve]);
//                                            showErrors[curve] = true;
                    } else if (ylabel[curve].startsWith("rel_flux_T") || ylabel[curve].startsWith("rel_flux_C")) {
                        errcolumn[curve] = table.getColumnIndex("rel_flux_err_" + ylabel[curve].substring(9));
                    } else if (ylabel[curve].startsWith("Source-Sky_")) {
                        errcolumn[curve] = table.getColumnIndex("Source_Error_" + ylabel[curve].substring(11));
                    } else if (ylabel[curve].startsWith("tot_C_cnts")) {
                        errcolumn[curve] = table.getColumnIndex("tot_C_err" + ylabel[curve].substring(10));
                    } else if (ylabel[curve].startsWith("Source_AMag_")) {
                        errcolumn[curve] = table.getColumnIndex("Source_AMag_Err_" + ylabel[curve].substring(12));
                    }
                    //                                            showErrors[curve] = false;
                    hasErrors[curve] = errcolumn[curve] != ResultsTable.COLUMN_NOT_FOUND;
//                                                }


//                                        if (!errlabel[curve].equals(""))
//                                                {
//                                                errcolumn[curve] = table.getColumnIndex (errlabel[curve]);
//                                                if (errcolumn[curve] == ResultsTable.COLUMN_NOT_FOUND)
//                                                        {
////                                                        if (errlabel[curve].trim().length() != 0)
////                                                            IJ.showMessage ("Cannot access Y"+curve+" error column "+errlabel[curve]+" !");
//                                                        showErrors[curve] = false;
////                                                        errorcolumnbox[curve].setSelected(false);
//                                                        }
//                                                else
//                                                        showErrors[curve] = true;
//                                                }
//                                        else
//                                                showErrors[curve] = false;


                    if (operatorIndex[curve] != 0) {
                        opcolumn[curve] = table.getColumnIndex(oplabel[curve]);
                        if (opcolumn[curve] == ResultsTable.COLUMN_NOT_FOUND) {
//                                                        if (oplabel[curve].trim().length() != 0)
//                                                            IJ.showMessage ("Cannot access Y"+curve+" operator data column "+oplabel[curve]+" !");
                            operatorIndex[curve] = 0;
                            //operatorselection[curve].setSelectedIndex(operatorIndex[curve]);
                        } else if (operatorIndex[curve] == 5) //calculate distance
                        {
                            xc2column[curve] = table.getColumnIndex(getPositionColumn(oplabel[curve], "X"));
                            yc2column[curve] = table.getColumnIndex(getPositionColumn(oplabel[curve], "Y"));
                            xyc2label[curve] = getSourceID(oplabel[curve]);
                            if (xc2column[curve] == ResultsTable.COLUMN_NOT_FOUND) {
                                if (getPositionColumn(oplabel[curve], "X").trim().length() != 0) {
                                    IJ.showMessage("Cannot access " + getPositionColumn(oplabel[curve], "X") + " source position data column for distance calculation !");
                                }
                                operatorIndex[curve] = 0;
                                //operatorselection[curve].setSelectedIndex(operatorIndex[curve]);
                            }
                            if (yc2column[curve] == ResultsTable.COLUMN_NOT_FOUND) {
                                if (getPositionColumn(oplabel[curve], "Y").trim().length() != 0) {
                                    IJ.showMessage("Cannot access " + getPositionColumn(oplabel[curve], "Y") + " source position data column for distance calculation !");
                                }
                                operatorIndex[curve] = 0;
                                //operatorselection[curve].setSelectedIndex(operatorIndex[curve]);
                            }
                        }

                    }
//                                        if (!operrlabel[curve].equals(""))
//                                                {
//                                                operrcolumn[curve] = table.getColumnIndex (operrlabel[curve]);
//                                                if (operrcolumn[curve] == ResultsTable.COLUMN_NOT_FOUND)
//                                                        {
////                                                        if (operrlabel[curve].trim().length() != 0)
////                                                            IJ.showMessage ("Cannot access Y"+curve+" operator error column "+operrlabel[curve]+" !");
//                                                        showOpErrors[curve] = false;
////                                                        operrorcolumnbox[curve].setSelected(false);
//                                                        }
//                                                else
//                                                        showOpErrors[curve] = true;
//                                                }
//                                        else
//                                                showOpErrors[curve] = false;

                    if (operatorIndex[curve] > 0 && operatorIndex[curve] < 5)// && showErrors[curve] == true)
                    {
                        operrcolumn[curve] = ResultsTable.COLUMN_NOT_FOUND;
                        if (oplabel[curve].startsWith("rel_flux_T") || oplabel[curve].startsWith("rel_flux_C")) {
                            operrcolumn[curve] = table.getColumnIndex("rel_flux_err_" + oplabel[curve].substring(9));
                        } else if (oplabel[curve].startsWith("Source-Sky_")) {
                            operrcolumn[curve] = table.getColumnIndex("Source_Error_" + oplabel[curve].substring(11));
                        } else if (oplabel[curve].startsWith("tot_C_cnts")) {
                            operrcolumn[curve] = table.getColumnIndex("tot_C_err" + oplabel[curve].substring(10));
                        }
                        //                                                showOpErrors[curve] = false;
                        //                                                showErrors[curve] = false;
                        //                                                hasErrors[curve] = false;
                        //                                                showOpErrors[curve] = true;
                        hasOpErrors[curve] = operrcolumn[curve] != ResultsTable.COLUMN_NOT_FOUND;
                    } else {
//                                            showOpErrors[curve] = false;
                        hasOpErrors[curve] = false;
                    }

                    int bucketSize = inputAverageOverSize[curve];
                    for (int j = 0; j < nn[curve]; j++) {
                        double yin;
                        double errin;
                        double opin = 0;
                        double operrin;
                        int numNaN = 0;
                        y[curve][j] = 0;
                        yerr[curve][j] = 0;
                        yop[curve][j] = 0;
                        for (int v = 0; v < maxDetrendVars; v++) {
                            detrend[curve][v][j] = 0;
                        }
                        xc1[curve][j] = 0;
                        xc2[curve][j] = 0;
                        yc1[curve][j] = 0;
                        yc2[curve][j] = 0;
                        // AVERAGE DATA IF APPLICABLE
                        if (nnr[curve] > 0 && j == nn[curve] - 1) { bucketSize = nnr[curve]; } else {
                            bucketSize = inputAverageOverSize[curve];
                        }
                        for (int k = excludedHeadSamples; k < (bucketSize + excludedHeadSamples); k++) {
                            yin = table.getValueAsDouble(ycolumn[curve], j * inputAverageOverSize[curve] + k);
                            if (Double.isNaN(yin)) {
                                numNaN += 1;
                                if (numNaN == bucketSize) {
                                    bucketSize = 1;
                                    y[curve][j] = Double.NaN;
                                    if (hasErrors[curve]) yerr[curve][j] = Double.NaN;
                                    if (operatorIndex[curve] != 0) yop[curve][j] = Double.NaN;
                                    if (detrendFitIndex[curve] != 0) {
                                        for (int v = 0; v < maxDetrendVars; v++) {
                                            if (detrendIndex[curve][v] != 0 || detrendVarAllNaNs[curve][v]) {
                                                detrend[curve][v][j] = Double.NaN;
                                            }
                                        }
                                    }

                                    if (operatorIndex[curve] == 5) //calculate distance
                                    {
                                        xc1[curve][j] = Double.NaN;
                                        xc2[curve][j] = Double.NaN;
                                        yc1[curve][j] = Double.NaN;
                                        yc2[curve][j] = Double.NaN;
                                    }
                                    if (hasOpErrors[curve]) yoperr[curve][j] = Double.NaN;
                                    numNaN = 0;
                                    break;
                                }
                            } else {
                                if (fromMag[curve]) {
                                    yin = Math.pow(10, -yin / 2.5);
                                }
                                y[curve][j] += yin;

                                if (hasErrors[curve]) {
                                    errin = table.getValueAsDouble(errcolumn[curve], j * inputAverageOverSize[curve] + k);
                                    if (fromMag[curve]) {
                                        errin = yin * (-Math.pow(10, -errin / 2.5) + 1);
                                    }
                                    yerr[curve][j] += errin * errin;
                                }
                                if (operatorIndex[curve] != 0) {
                                    opin = table.getValueAsDouble(opcolumn[curve], j * inputAverageOverSize[curve] + k);
                                    if (fromMag[curve]) {
                                        opin = Math.pow(10, -opin / 2.5);
                                    }
                                    yop[curve][j] += opin;
                                }
                                if (detrendFitIndex[curve] != 0) {
                                    for (int v = 0; v < maxDetrendVars; v++) {
                                        if (detrendIndex[curve][v] > 1 || detrendVarAllNaNs[curve][v]) {
                                            detrend[curve][v][j] += table.getValueAsDouble(detrendcolumn[curve][v], j * inputAverageOverSize[curve] + k);
                                        }
                                    }
                                }
                                if (operatorIndex[curve] == 5) //calculate distance
                                {
                                    xc1[curve][j] += table.getValueAsDouble(xc1column[curve], j * inputAverageOverSize[curve] + k);
                                    xc2[curve][j] += table.getValueAsDouble(xc2column[curve], j * inputAverageOverSize[curve] + k);
                                    yc1[curve][j] += table.getValueAsDouble(yc1column[curve], j * inputAverageOverSize[curve] + k);
                                    yc2[curve][j] += table.getValueAsDouble(yc2column[curve], j * inputAverageOverSize[curve] + k);
                                }
                                if (hasOpErrors[curve]) {
                                    operrin = table.getValueAsDouble(operrcolumn[curve], j * inputAverageOverSize[curve] + k);
                                    if (fromMag[curve]) {
                                        operrin = opin * (-Math.pow(10, -operrin / 2.5) + 1);
                                    }
                                    yoperr[curve][j] += operrin * operrin;
                                }
                            }
                        }
                        bucketSize -= numNaN;
                        y[curve][j] = y[curve][j] / bucketSize;  //*yMultiplierFactor

                        if (hasErrors[curve]) {
                            yerr[curve][j] = Math.sqrt(yerr[curve][j]) / bucketSize; //yMultiplierFactor*
                        } else { yerr[curve][j] = 1.0; }
                        if (detrendFitIndex[curve] != 0) {
                            for (int v = 0; v < maxDetrendVars; v++) {
                                if (detrendIndex[curve][v] != 0 || detrendVarAllNaNs[curve][v]) {
                                    detrend[curve][v][j] /= bucketSize;
                                }
                            }
                        }
                        if (operatorIndex[curve] != 0) {
                            yop[curve][j] = yop[curve][j] / bucketSize; //*yMultiplierFactor
                            if (operatorIndex[curve] == 5) {
                                xc1[curve][j] = xc1[curve][j] / bucketSize;  //*yMultiplierFactor
                                xc2[curve][j] = xc2[curve][j] / bucketSize;  //*yMultiplierFactor
                                yc1[curve][j] = yc1[curve][j] / bucketSize;  //*yMultiplierFactor
                                yc2[curve][j] = yc2[curve][j] / bucketSize;  //*yMultiplierFactor
                            }
                        }
                        if (hasOpErrors[curve]) {
                            yoperr[curve][j] = Math.sqrt(yoperr[curve][j]) / bucketSize;  //yMultiplierFactor*
                        } else { yoperr[curve][j] = 1.0; }

                        //APPLY OPERATOR/OPERROR FUNCTIONS TO YDATA AND YERROR

                        if (operatorIndex[curve] == 0)  //no operator
                        {

                        } else if (operatorIndex[curve] == 1)  //divide by
                        {
                            if (yop[curve][j] == 0) {
                                yerr[curve][j] = 1.0e+100;
                                y[curve][j] = 1.0e+100;
                            } else {
                                if (hasErrors[curve] || hasOpErrors[curve]) {
                                    yerr[curve][j] = Math.sqrt(((yerr[curve][j] * yerr[curve][j]) / (yop[curve][j] * yop[curve][j])) + ((y[curve][j] * y[curve][j] * yoperr[curve][j] * yoperr[curve][j]) / (yop[curve][j] * yop[curve][j] * yop[curve][j] * yop[curve][j])));  //yMultiplierFactor*
                                }
                                y[curve][j] = y[curve][j] / yop[curve][j];  //*yMultiplierFactor
                            }
                        } else if (operatorIndex[curve] == 2)  //multiply by
                        {
                            if (hasErrors[curve] || hasOpErrors[curve]) {
                                yerr[curve][j] = Math.sqrt(yop[curve][j] * yop[curve][j] * yerr[curve][j] * yerr[curve][j] + y[curve][j] * y[curve][j] * yoperr[curve][j] * yoperr[curve][j]); // /yMultiplierFactor;
                            }
                            y[curve][j] = y[curve][j] * yop[curve][j];  // /yMultiplierFactor;
                        } else if (operatorIndex[curve] == 3)  //subtract
                        {
                            if (hasErrors[curve] || hasOpErrors[curve]) {
                                yerr[curve][j] = Math.sqrt(yerr[curve][j] * yerr[curve][j] + yoperr[curve][j] * yoperr[curve][j]);
                            }
                            y[curve][j] = y[curve][j] - yop[curve][j];
                        } else if (operatorIndex[curve] == 4)  //add
                        {
                            if (hasErrors[curve] || hasOpErrors[curve]) {
                                yerr[curve][j] = Math.sqrt(yerr[curve][j] * yerr[curve][j] + yoperr[curve][j] * yoperr[curve][j]);
                            }
                            y[curve][j] = y[curve][j] + yop[curve][j];
                        } else if (operatorIndex[curve] == 5)  //distance from x1,y1 to x2,y2
                        {
                            y[curve][j] = (usePixelScale ? pixelScale : 1.0) * Math.sqrt(((xc1[curve][j] - xc2[curve][j]) * (xc1[curve][j] - xc2[curve][j])) + ((yc1[curve][j] - yc2[curve][j]) * (yc1[curve][j] - yc2[curve][j])));
                        }
                    }

                    if (plotY[curve]) {
                        for (int v = 0; v < maxDetrendVars; v++) {
                            detrendVarAllNaNs[curve][v] = true;
                        }
                        for (int j = 0; j < nn[curve]; j++) {
                            for (int v = 0; v < maxDetrendVars; v++) {
                                if (!Double.isNaN(detrend[curve][v][j])) detrendVarAllNaNs[curve][v] = false;
                            }
                        }
                        for (int v = 0; v < maxDetrendVars; v++) {
                            if (detrendVarAllNaNs[curve][v]) {
                                detrendIndex[curve][v] = 0;
                            }
                        }

                    }

                    if (ylabel[curve].trim().startsWith("J.D.-2400000")) {
                        for (int j = 0; j < nn[curve]; j++) {
                            y[curve][j] += 2400000;
                        }
                    }
                }
            }

            if (plotY[curve] && smooth[curve] && nn[curve] > 2 * smoothLen[curve]) {
                double[] xl = new double[nn[curve]];
                double[] yl = new double[nn[curve]];
                double[] xphase = new double[nn[curve]];
                double[] yphase = new double[nn[curve]];
                double xfold;
                int nskipped = 0;
                double xmax = Double.NEGATIVE_INFINITY;
                double xmin = Double.POSITIVE_INFINITY;
                double halfPeriod = netPeriod / 2.0;
                for (int xx = 0; xx < nn[curve]; xx++) {
                    if (false) //showXAxisNormal
                    {
                        yl[xx] = y[curve][xx];
                        xl[xx] = x[curve][xx] - (int) x[curve][0];
                    } else {
                        xfold = ((x[curve][xx] - netT0) % netPeriod);
                        if (xfold > halfPeriod) { xfold -= netPeriod; } else if (xfold < -halfPeriod) xfold += netPeriod;
                        if (Math.abs(xfold) < duration / 48.0) {
                            nskipped++;
                        } else {
                            yphase[xx - nskipped] = y[curve][xx];
                            xphase[xx - nskipped] = x[curve][xx] - (int) x[curve][0];
                            if (x[curve][xx] > xmax) xmax = x[curve][xx];
                            if (x[curve][xx] < xmin) xmin = x[curve][xx];
                        }
                    }
                }
                if (true) //!showXAxisNormal
                {
                    xl = new double[nn[curve] - nskipped];
                    yl = new double[nn[curve] - nskipped];
                    for (int xx = 0; xx < nn[curve] - nskipped; xx++) {
                        yl[xx] = yphase[xx];
                        xl[xx] = xphase[xx];
                    }
                }
                if (nn[curve] - nskipped > 2 * smoothLen[curve]) {
                    double smoothVal;
                    Smooth csm = new Smooth(xl, yl);
                    //csm.movingAverage(csmwidth);
                    csm.savitzkyGolay(smoothLen[curve]);
                    csm.setSGpolyDegree(2);
                    double yave = 0.0;
                    for (int xx = 0; xx < nn[curve] - nskipped; xx++) {
                        yave += yl[xx];
                    }
                    yave /= (nn[curve] - nskipped);
                    for (int xx = 0; xx < nn[curve]; xx++) {
                        if (x[curve][xx] > xmax) {
                            smoothVal = csm.interpolateSavitzkyGolay(xmax - (int) x[curve][0]);
                        } else if (x[curve][xx] < xmin) {
                            smoothVal = csm.interpolateSavitzkyGolay(xmin - (int) x[curve][0]);
                        } else {
                            smoothVal = csm.interpolateSavitzkyGolay(x[curve][xx] - (int) x[curve][0]);
                        }
                        //y[1][xx] = csm.interpolateMovingAverage(xl[xx]);
                        y[curve][xx] = y[curve][xx] - smoothVal + yave;
                    }
                }
                //y[curve]=smoothed.getMovingAverageValues();
            }
            //IJ.log(""+y[curve]);
        }

        // PERFORM X-AUTOSCALING TO ONE OR MORE CURVES
        dx = 0.0;

        xPlotMin = xMin;
        xPlotMax = xMax;

        xautoscalemin = Double.POSITIVE_INFINITY;
        xautoscalemax = Double.NEGATIVE_INFINITY;

        var hasXDatasetToScaleAgainst = IntStream.range(0, ASInclude.length)
                .mapToObj(i -> ASInclude[i]).filter(b -> b).findAny().isPresent();

        firstCurve = -1;
        for (int curve = 0; curve < maxCurves; curve++) {
            if (plotY[curve]) {
                if ((firstCurve == -1)) {
                    firstCurve = curve; //FIND THE FIRST CURVE TO DISPLAY - IT IS USED FOR THE Y-AXIS LABEL
                    xBase=x[curve][0];
                }
                if (!showXAxisNormal) {
                    if (showXAxisAsPhase) {
                        for (int j = 0; j < nn[curve]; j++) {
                            x[curve][j] = ((x[curve][j] - netT0) % netPeriod) / netPeriod;
                            if (x[curve][j] > 0.5) { x[curve][j] -= 1.0; } else if (x[curve][j] < -0.5) x[curve][j] += 1.0;
                        }
                    } else if (showXAxisAsDaysSinceTc) {
                        double halfPeriod = netPeriod / 2.0;
                        for (int j = 0; j < nn[curve]; j++) {
                            x[curve][j] = ((x[curve][j] - netT0) % netPeriod);
                            if (x[curve][j] > halfPeriod) {
                                x[curve][j] -= netPeriod;
                            } else if (x[curve][j] < -halfPeriod) x[curve][j] += netPeriod;
                        }
                    } else if (showXAxisAsHoursSinceTc) {
                        double halfPeriod = netPeriod / 2.0;
                        for (int j = 0; j < nn[curve]; j++) {
                            x[curve][j] = ((x[curve][j] - netT0) % netPeriod);
                            if (x[curve][j] > halfPeriod) {
                                x[curve][j] -= netPeriod;
                            } else if (x[curve][j] < -halfPeriod) x[curve][j] += netPeriod;
                            x[curve][j] *= 24;
                        }
                    }
                }
                xMinimum[curve] = minOf(x[curve], nn[curve]); //FIND MIN AND MAX X OF EACH SELECTED DATASET
                xMaximum[curve] = maxOf(x[curve], nn[curve]);


                if (ASInclude[curve]) {
                    if (xMinimum[curve] < xautoscalemin) xautoscalemin = xMinimum[curve];
                    if (xMaximum[curve] > xautoscalemax) xautoscalemax = xMaximum[curve];
                }

                if (!hasXDatasetToScaleAgainst) {
                    if (xMinimum[curve] < xautoscalemin) xautoscalemin = xMinimum[curve];
                    if (xMaximum[curve] > xautoscalemax) xautoscalemax = xMaximum[curve];
                }
            }
        }

        if (firstCurve == -1) {  //IF NO CURVES SELECTED FOR DISPLAY, USE THE FIRST CURVE
            firstCurve = 0;
            xBase = x[0][0];
        }

        if (showVMarker1 || showVMarker2) {
            double v1 = vMarker1Value;
            double v2 = vMarker2Value;
            if ((xlabel2[firstCurve].contains("J.D.") || xlabel2[firstCurve].contains("JD")) && showXAxisNormal) {
                v1 += (int) xautoscalemin;
                v2 += (int) xautoscalemin;
            }
            if (showVMarker1) {
                if (v1 < xautoscalemin && (!showVMarker2 || (showVMarker2 && v1 <= v2))) xautoscalemin = v1;
                if (v1 > xautoscalemax && (!showVMarker2 || (showVMarker2 && v1 >= v2))) xautoscalemax = v1;
            }
            if (showVMarker2) {
                if (v2 < xautoscalemin && (!showVMarker1 || (showVMarker1 && v2 < v1))) xautoscalemin = v2;
                if (v2 > xautoscalemax && (!showVMarker1 || (showVMarker1 && v2 > v1))) xautoscalemax = v2;
            }
        }

        if (Double.isInfinite(xautoscalemin)) {
            xautoscalemin = Double.isInfinite(xautoscalemax) ? 0.0 : xautoscalemax - 1.0;
        }
        if (Double.isInfinite(xautoscalemax)) xautoscalemax = xautoscalemin + 1.0;


        if (autoScaleX) {
            xPlotMin = xautoscalemin;
            xPlotMax = (xautoscalemin == xautoscalemax) ? xautoscalemin + 0.01 : xautoscalemax;
        } else if (useFirstX) {
            firstXmin = xautoscalemin;
            xPlotMin = xautoscalemin;
            if (xWidth < 0.0001) xWidth = 0.0001;
            xPlotMax = xautoscalemin + xWidth;
        }
        xPlotMinRaw = xPlotMin;
        dx = (xPlotMax - xPlotMin) / 99.0;
        xJD = 0;
        xOffset = 0.0;
        if ((xlabel2[firstCurve].contains("J.D.") || xlabel2[firstCurve].contains("JD")) && showXAxisNormal) {
            if (xExponent != 0) xmultiplierspinner.setValue(0);
            xJD = (int) xPlotMin;
            xOffset = xJD;
            if (showVMarker1 && vMarker1Value < 0.0) {
                xOffset += 1.0;
                xPlotMinRaw += 1.0;
            }
        }
        for (curve = 0; curve < maxCurves; curve++) {
            residual[curve] = null;
            plottedResidual[curve] = null;
            yModel1Err[curve] = null;
            detrendYAverage[curve] = 0.0;
            if (plotY[curve]) {
                fitMin[curve] = (useDMarker1 ? dMarker1Value : Double.NEGATIVE_INFINITY) + xOffset;
                fitMax[curve] = (useDMarker4 ? dMarker4Value : Double.POSITIVE_INFINITY) + xOffset;
                fitLeft[curve] = dMarker2Value + xOffset;
                fitRight[curve] = dMarker3Value + xOffset;
                switch (detrendFitIndex[curve]) {
                    case 2: // left of D2
                        fitMax[curve] = fitLeft[curve];
                        break;
                    case 3: // right of D3
                        fitMin[curve] = fitRight[curve];
                        break;
                    case 4: // outside D2 and D3
                        break;
                    case 5: // inside D2 and D3
                        fitMin[curve] = fitLeft[curve];
                        fitMax[curve] = fitRight[curve];
                        break;
                    case 6: // left of D3
                        fitMax[curve] = fitRight[curve];
                        break;
                    case 7: // right of D2
                        fitMin[curve] = fitLeft[curve];
                        break;
                    case 8: // use all data
                        break;
                    case 9: // use all data to fit transit with simultaneaous detrend
                        break;
                    default: // use all data
                        detrendFitIndex[curve] = 0;
                        break;
                }
//                        IJ.log("");
//                        IJ.log("fitMin[curve]="+fitMin[curve]);
//                        IJ.log("fitMax[curve]="+fitMax[curve]);
//                        IJ.log("fitLeft[curve]="+fitLeft[curve]);
//                        IJ.log("fitRight[curve]="+fitRight[curve]);
//                        IJ.log("");

                boolean atLeastOne = false;
                boolean detrendYNotConstant = false;
                detrendYDNotConstant = new boolean[maxDetrendVars];
                detrendYAverage[curve] = 0.0;

                for (int v = 0; v < maxDetrendVars; v++) {
                    detrendYDNotConstant[v] = detrendFitIndex[curve] == 1;
                }
                if (detrendFitIndex[curve] != 0) {
                    for (int v = 0; v < maxDetrendVars; v++) {
                        if (detrendIndex[curve][v] != 0) {
                            atLeastOne = true;
                            break;
                        }
                    }
                }
                if (atLeastOne || detrendFitIndex[curve] == 9) {
                    double[] detrendAverage = new double[maxDetrendVars];
                    int[] detrendPower = new int[maxDetrendVars];
                    for (int v = 0; v < maxDetrendVars; v++) {
                        detrendAverage[v] = 0.0;
                        detrendPower[v] = 1;
                        int numNaNs = 0;
                        for (int j = 0; j < nn[curve]; j++) {
                            if (Double.isNaN(detrend[curve][v][j])) { numNaNs++; } else {
                                detrendAverage[v] += detrend[curve][v][j] / (double) nn[curve];
                            }
                        }
                        detrendAverage[v] = ((double) nn[curve] / ((double) nn[curve] - (double) numNaNs)) * detrendAverage[v];
                        for (int j = 0; j < nn[curve]; j++) {
                            detrend[curve][v][j] -= detrendAverage[v];
                        }

                        if (v > 0) {
                            for (int u = 0; u < v; u++) {
                                if (detrendIndex[curve][u] == detrendIndex[curve][v]) detrendPower[v]++;
                            }
                        }
                        if (detrendPower[v] > 1) {
                            detrendAverage[v] = 0.0;
                            numNaNs = 0;
                            for (int j = 0; j < nn[curve]; j++) {
                                if (Double.isNaN(detrend[curve][v][j])) { numNaNs++; } else {
                                    detrendAverage[v] += detrend[curve][v][j];
                                }
                            }
                            detrendAverage[v] /= (nn[curve] - numNaNs);
                            for (int j = 0; j < nn[curve]; j++) {
                                detrend[curve][v][j] -= detrendAverage[v];
                            }
                        }
                    }
                    for (int v = 0; v < maxDetrendVars; v++) {
                        if (detrendPower[v] == 2) {
                            for (int j = 0; j < nn[curve]; j++) {
                                detrend[curve][v][j] *= detrend[curve][v][j];
                            }
                        } else if (detrendPower[v] > 2) {
                            for (int j = 0; j < nn[curve]; j++) {
                                detrend[curve][v][j] = Math.pow(detrend[curve][v][j], detrendPower[v]);
                            }
                        }
                    }
                    double meridianFlip = mfMarker1Value + xOffset;
                    for (int v = 0; v < maxDetrendVars; v++) {
                        if (detrendIndex[curve][v] == 1)    //Meridian Flip Detrend Selected
                        {
                            for (int j = 0; j < nn[curve]; j++) {
                                if (x[curve][j] < meridianFlip)  //meridian flip fitting data = -1.0 to left and 1.0 to right of flip
                                { detrend[curve][v][j] = -1.0; } else { detrend[curve][v][j] = 1.0; }
                            }
                        }
                    }

                    yAverage[curve] = 0.0;
                    yDepthEstimate[curve] = 0.0;
                    yBaselineAverage[curve] = 0.0;
                    nFitTrim[curve] = 0;
                    int avgCount = 0;
                    int detrendCount = 0;
                    int baselineCount = 0;
                    int depthCount = 0;

                    double[] detrendX = new double[nn[curve]];
                    double[] detrendY = new double[nn[curve]];
                    double[] detrendYE = new double[nn[curve]];
                    double[] detrendConstantComparator = new double[Math.max(nn[curve], maxDetrendVars)];
                    Arrays.fill(detrendConstantComparator, Double.NaN);
                    if (detrendFitIndex[curve] != 1) {
                        double[][] detrendYD = new double[maxDetrendVars][nn[curve]];
                        boolean noNaNs = true;
                        if (detrendFitIndex[curve] == 4) {
                            for (int j = 0; j < nn[curve]; j++) {
                                noNaNs = true;
                                if (!Double.isNaN(y[curve][j])) {
                                    for (int v = 0; v < maxDetrendVars; v++) {
                                        if (detrendIndex[curve][v] != 0 && Double.isNaN(detrend[curve][v][j])) {
//                                                    IJ.log("found a detrend var NaN");
                                            noNaNs = false;
                                            break;
                                        }
                                    }
                                } else {
//                                            IJ.log("found a Y NaN");
                                    noNaNs = false;
                                }
                                if (noNaNs) {
                                    avgCount++;
                                    yAverage[curve] += y[curve][j];
//                                            for(int v = 0; v < maxDetrendVars; v++)
//                                                {
//                                                detrendAverage[v] += detrend[curve][v][j];
//                                                }
                                    if ((x[curve][j] > fitMin[curve] && x[curve][j] < fitLeft[curve]) || (x[curve][j] > fitRight[curve] && x[curve][j] < fitMax[curve])) {
                                        detrendX[detrendCount] = x[curve][j];
                                        detrendY[detrendCount] = y[curve][j];
                                        detrendYE[detrendCount] = hasErrors[curve] || hasOpErrors[curve] ? yerr[curve][j] : 1;
                                        detrendYAverage[curve] += y[curve][j];
                                        if (detrendY[0] != detrendY[detrendCount]) detrendYNotConstant = true;
                                        for (int v = 0; v < maxDetrendVars; v++) {
                                            detrendYD[v][detrendCount] = detrend[curve][v][j];

                                            if (Double.isNaN(detrendConstantComparator[v]) && !Double.isNaN(detrendYD[v][detrendCount])) {
                                                detrendConstantComparator[v] = detrendYD[v][detrendCount];
                                            }
                                            if (!Double.isNaN(detrendConstantComparator[v]) && detrendConstantComparator[v] != detrendYD[v][detrendCount]) {
                                                detrendYDNotConstant[v] = true;
                                            }
                                        }
                                        detrendCount++;
                                    }
                                }
                            }
                            if (detrendVarsUsed[curve] > 0 && detrendYNotConstant && detrendCount > detrendVarsUsed[curve] + 2) //need enough data to provide degrees of freedom for successful fit
                            {
                                xModel1[curve] = new double[2];
                                xModel2[curve] = new double[2];
                                xModel1[curve][0] = Math.max(fitMin[curve], xPlotMin);
                                xModel1[curve][1] = fitLeft[curve];
                                xModel2[curve][0] = fitRight[curve];
                                xModel2[curve][1] = Math.min(fitMax[curve], xPlotMax);
                                if (xModel1[curve][0] >= xModel1[curve][1]) xModel1[curve] = null;
                                if (xModel2[curve][0] >= xModel2[curve][1]) xModel2[curve] = null;
//                                        IJ.log("x = "+xModel1[curve][0]+" - "+xModel1[curve][1]);
//                                        IJ.log("x = "+xModel2[curve][0]+" - "+xModel2[curve][1]);
                            } else {
                                for (int v = 0; v < maxDetrendVars; v++) {
                                    if (detrendIndex[curve][v] > 0) {
                                        detrendFactor[curve][v] = 0.0;
                                    }
                                }
                            }
                        } else {
                            for (int j = 0; j < nn[curve]; j++) {
                                noNaNs = true;
                                if (!Double.isNaN(y[curve][j])) {
                                    for (int v = 0; v < maxDetrendVars; v++) {
                                        if (detrendIndex[curve][v] != 0 && Double.isNaN(detrend[curve][v][j])) {
                                            noNaNs = false;
                                            break;
                                        }
                                    }
                                } else {
                                    noNaNs = false;
                                }
                                if (noNaNs) {
                                    avgCount++;
                                    yAverage[curve] += y[curve][j];
//                                            for(int v = 0; v < maxDetrendVars; v++)
//                                                {
//                                                detrendAverage[v] += detrend[curve][v][j];
//                                                }
                                    if (x[curve][j] > fitMin[curve]) {
                                        if (x[curve][j] < fitMax[curve]) {
                                            detrendX[detrendCount] = x[curve][j];
                                            detrendY[detrendCount] = y[curve][j];
                                            detrendYE[detrendCount] = hasErrors[curve] || hasOpErrors[curve] ? yerr[curve][j] : 1;
                                            detrendYAverage[curve] += y[curve][j];
                                            if (detrendY[0] != detrendY[detrendCount]) detrendYNotConstant = true;
                                            for (int v = 0; v < maxDetrendVars; v++) {
                                                detrendYD[v][detrendCount] = detrend[curve][v][j];
                                                if (Double.isNaN(detrendConstantComparator[v]) && !Double.isNaN(detrendYD[v][detrendCount])) {
                                                    detrendConstantComparator[v] = detrendYD[v][detrendCount];
                                                }
                                                if (!Double.isNaN(detrendConstantComparator[v]) && detrendConstantComparator[v] != detrendYD[v][detrendCount]) {
                                                    detrendYDNotConstant[v] = true;
                                                }
                                            }
                                            detrendCount++;
                                        }
                                    } else {
                                        nFitTrim[curve]++;
                                    }
                                    if (detrendFitIndex[curve] == 9) {
                                        if (x[curve][j] > fitLeft[curve] + (fitRight[curve] - fitLeft[curve]) / 4.0 && x[curve][j] < fitRight[curve] - (fitRight[curve] - fitLeft[curve]) / 4.0) {
                                            yDepthEstimate[curve] += y[curve][j];
                                            depthCount++;
                                        } else if (x[curve][j] < fitLeft[curve] || x[curve][j] > fitRight[curve]) {
                                            yBaselineAverage[curve] += y[curve][j];
                                            baselineCount++;
                                        }
                                    }
                                }
                            }
                            if (detrendVarsUsed[curve] > 0 && detrendYNotConstant && detrendCount > detrendVarsUsed[curve] + 2) //need enough data to provide degrees of freedom for successful fit
                            {
                                xModel1[curve] = new double[2];
                                xModel1[curve][0] = Math.max(fitMin[curve], xPlotMin);
                                xModel1[curve][1] = Math.min(fitMax[curve], xPlotMax);
                                xModel2[curve] = null;
//                                        IJ.log("x = "+xModel1[curve][0]+" - "+xModel1[curve][1]);
                            } else {
                                for (int v = 0; v < maxDetrendVars; v++) {
                                    if (detrendIndex[curve][v] > 0) {
                                        detrendFactor[curve][v] = 0.0;
                                    }
                                }
                            }
                        }
//                                IJ.log("detrendCount="+detrendCount);
                        if (detrendCount > 0) {
                            yAverage[curve] /= avgCount; //avgCount is always >= detrendCount, so > 0 here
                            detrendYAverage[curve] /= detrendCount;
                            if (baselineCount > 0) { yBaselineAverage[curve] /= baselineCount; } else {
                                yBaselineAverage[curve] = detrendYAverage[curve] * 1.005;
                            }
                            if (depthCount > 0) { yDepthEstimate[curve] /= depthCount; } else {
                                yDepthEstimate[curve] = detrendYAverage[curve] * 0.995;
                            }

                            for (int j = 0; j < detrendCount; j++) {
                                detrendY[j] -= detrendYAverage[curve];//yAverage[curve];
                            }
                            for (int v = 0; v < maxDetrendVars; v++) {
                                if (detrendIndex[curve][v] != 0 && !detrendYDNotConstant[v] && detrendVarsUsed[curve] > 0) {
                                    detrendVarsUsed[curve]--;
                                }
                            }
                            detrendVars = new double[detrendVarsUsed[curve]][detrendCount];
                            int varCount = 0;
                            for (int v = 0; v < maxDetrendVars; v++) {
                                if (detrendIndex[curve][v] != 0 && detrendYDNotConstant[v]) {
                                    detrendVars[varCount] = Arrays.copyOf(detrendYD[v], detrendCount);
                                    varCount++;
                                }
                            }
//                                    IJ.log("varCount="+varCount);
                            if ((detrendVarsUsed[curve] > 0 || detrendFitIndex[curve] == 9 && useTransitFit[curve]) && detrendYNotConstant && detrendCount > (detrendFitIndex[curve] == 9 && useTransitFit[curve] ? 7 : 0) + detrendVarsUsed[curve] + 2) //need enough data to provide degrees of freedom for successful fit
                            {
                                detrendXs[curve] = Arrays.copyOf(detrendX, detrendCount);
                                detrendYs[curve] = Arrays.copyOf(detrendY, detrendCount);
                                detrendYEs[curve] = Arrays.copyOf(detrendYE, detrendCount);
                                if (updateFit[curve]) {
                                    int fittedDetrendParStart;
                                    if (detrendFitIndex[curve] == 9) {
                                        minimization.removeConstraints();
                                        setFittedParametersBorderColorNewThread(curve, grayBorder2);
                                        if (autoUpdatePriors[curve]) updatePriorCenters(curve);
                                        int nFitted = 0;
                                        for (int p = 0; p < 7; p++) {
                                            if (useTransitFit[curve] && !lockToCenter[curve][p]) {
                                                isFitted[curve][p] = true;
                                                nFitted++;
                                            } else {
                                                isFitted[curve][p] = false;
                                            }
                                        }
                                        fittedDetrendParStart = nFitted;
                                        for (int p = 7; p < maxFittedVars; p++) {
                                            if (detrendIndex[curve][p - 7] != 0 && detrendYDNotConstant[p - 7] && !lockToCenter[curve][p]) {
                                                isFitted[curve][p] = true;
                                                nFitted++;
                                            } else {
                                                isFitted[curve][p] = false;
                                            }
                                        }

                                        start[curve] = new double[nFitted];
                                        width[curve] = new double[nFitted];
                                        step[curve] = new double[nFitted];

                                        index[curve] = new int[nFitted];
                                        int fp = 0;  //fitted parameter
                                        for (int p = 0; p < maxFittedVars; p++) {
                                            if (isFitted[curve][p]) {
                                                index[curve][fp] = p;
                                                fp++;
                                            }
                                        }

                                        dof[curve] = detrendXs[curve].length - start[curve].length;

                                        // 0 = f0 = baseline flux
                                        // 1 = p0 = r_p/r_*
                                        // 2 = ar = a/r_*
                                        // 3 = tc = transit center time
                                        // 4 = i = inclination
                                        // 5 = u1 = quadratic limb darkening parameter 1
                                        // 6 = u2 = quadratic limb darkening parameter 2
                                        // 7+ = detrend parameters
                                        for (fp = 0; fp < nFitted; fp++) {
                                            if (index[curve][fp] == 1) {
                                                start[curve][fp] = Math.sqrt(priorCenter[curve][1]);
                                                width[curve][fp] = Math.sqrt(priorWidth[curve][1]);
                                                step[curve][fp] = Math.sqrt(getFitStep(curve, 1));
                                                minimization.addConstraint(fp, -1, 0.0);
                                            } else if (index[curve][fp] == 4) {
                                                start[curve][fp] = priorCenter[curve][4] * Math.PI / 180.0;  // inclination
                                                width[curve][fp] = priorWidth[curve][4] * Math.PI / 180.0;
                                                step[curve][fp] = getFitStep(curve, 4) * Math.PI / 180.0;
                                                minimization.addConstraint(fp, 1, 90.0 * Math.PI / 180.0);
                                                minimization.addConstraint(fp, -1, 50.0 * Math.PI / 180.0);
                                            } else {
                                                if (index[curve][fp] == 0) minimization.addConstraint(fp, -1, 0.0);
                                                if (index[curve][fp] == 2) minimization.addConstraint(fp, -1, 2.0);
                                                if (index[curve][fp] == 3) minimization.addConstraint(fp, -1, 0.0);
                                                if (index[curve][fp] == 5) {
                                                    minimization.addConstraint(fp, 1, 1.0);
                                                    minimization.addConstraint(fp, -1, -1.0);
                                                }
                                                if (index[curve][fp] == 6) {
                                                    minimization.addConstraint(fp, 1, 1.0);
                                                    minimization.addConstraint(fp, -1, -1.0);
                                                }
                                                start[curve][fp] = priorCenter[curve][index[curve][fp]];
                                                width[curve][fp] = priorWidth[curve][index[curve][fp]];
                                                step[curve][fp] = getFitStep(curve, index[curve][fp]);
                                            }
                                            if (usePriorWidth[curve][index[curve][fp]]) {
                                                minimization.addConstraint(fp, 1, start[curve][fp] + width[curve][fp]);
                                                minimization.addConstraint(fp, -1, start[curve][fp] - width[curve][fp]);
                                            }
                                        }

                                        //                                                    for (int p = 0; p<start[curve].length; p++)
                                        //                                                        {
                                        //                                                        IJ.log(parameterFormat.format(start[curve][p])+"      "+parameterFormat.format(step[curve][p]));
                                        //                                                        }
                                        //                                                    IJ.log("");

                                        //                                                    minimization.setScale(1);
                                        minimization.setNrestartsMax(1);
                                        minimization.nelderMead(new FitLightCurveChi2(), start[curve], step[curve], tolerance[curve], maxFitSteps[curve]);
                                        coeffs[curve] = minimization.getParamValues();
                                        nTries[curve] = minimization.getNiter() - 1;
                                        converged[curve] = minimization.getConvStatus();
                                        fp = 0;
                                        for (int p = 0; p < maxFittedVars; p++) {
                                            if (isFitted[curve][p]) {
                                                bestFit[curve][p] = coeffs[curve][fp];
                                                fp++;
                                            } else if (p < 7 && useTransitFit[curve] && lockToCenter[curve][p]) {
                                                if (p == 1) {
                                                    bestFit[curve][p] = Math.sqrt(priorCenter[curve][p]);
                                                } else if (p == 4) {
                                                    if (bpLock[curve]) {
                                                        bestFit[curve][4] = Math.acos(bp[curve]/bestFit[curve][2]);
                                                    } else {
                                                        bestFit[curve][p] = priorCenter[curve][p] * Math.PI / 180.0;
                                                    }
                                                } else { bestFit[curve][p] = priorCenter[curve][p]; }
                                            } else if (p >= 7 && p < 7 + maxDetrendVars && detrendIndex[curve][p - 7] != 0 && detrendYDNotConstant[p - 7] && lockToCenter[curve][p]) {
                                                bestFit[curve][p] = priorCenter[curve][p];
                                            } else {
                                                bestFit[curve][p] = Double.NaN;
                                            }
                                        }

                                        for (int p = 0; p < maxFittedVars; p++) {
                                            if (p == 1 && !Double.isNaN(bestFit[curve][p])) {
                                                bestFitLabel[curve][p].setText(ninePlaces.format(bestFit[curve][p] * bestFit[curve][p]));
                                            } else if (p == 4 && !Double.isNaN(bestFit[curve][p])) {
                                                bestFitLabel[curve][p].setText(ninePlaces.format(bestFit[curve][p] * 180 / Math.PI));
                                            } else if (!Double.isNaN(bestFit[curve][p])) {
                                                bestFitLabel[curve][p].setText(p < 7 ? ninePlaces.format(bestFit[curve][p]) : detrendParameterFormat.format(bestFit[curve][p]));
                                            } else if (p >= 7 && p < 7 + maxDetrendVars && detrendVarAllNaNs[curve][p - 7]) {
                                                bestFitLabel[curve][p].setText("all NaNs");
                                            } else if (p >= 7 && p < 7 + maxDetrendVars && detrendIndex[curve][p - 7] != 0 && !detrendYDNotConstant[p - 7]) {
                                                bestFitLabel[curve][p].setText("constant var");
                                            } else {
                                                bestFitLabel[curve][p].setText("");
                                            }
                                        }
                                        if (useTransitFit[curve]) {
                                            //Winn 2010 eqautions 14, 15, 16
                                            if (!bpLock[curve]) bp[curve] = bestFit[curve][2] * Math.cos(bestFit[curve][4]) * (1.0 - (forceCircularOrbit[curve] ? 0.0 : eccentricity[curve] * eccentricity[curve])) / (1.0 + (forceCircularOrbit[curve] ? 0.0 : eccentricity[curve]) * Math.sin(forceCircularOrbit[curve] ? 0.0 : omega[curve]));
                                            double bp2 = bp[curve] * bp[curve];
                                            t14[curve] = (orbitalPeriod[curve] / Math.PI * Math.asin(Math.sqrt((1.0 + bestFit[curve][1]) * (1.0 + bestFit[curve][1]) - bp2) / (Math.sin(bestFit[curve][4]) * bestFit[curve][2])) * Math.sqrt(1.0 - (forceCircularOrbit[curve] ? 0.0 : eccentricity[curve] * eccentricity[curve])) / (1.0 + (forceCircularOrbit[curve] ? 0.0 : eccentricity[curve]) * Math.sin(forceCircularOrbit[curve] ? 0.0 : omega[curve])));
                                            t23[curve] = (orbitalPeriod[curve] / Math.PI * Math.asin(Math.sqrt((1.0 - bestFit[curve][1]) * (1.0 - bestFit[curve][1]) - bp2) / (Math.sin(bestFit[curve][4]) * bestFit[curve][2])) * Math.sqrt(1.0 - (forceCircularOrbit[curve] ? 0.0 : eccentricity[curve] * eccentricity[curve])) / (1.0 + (forceCircularOrbit[curve] ? 0.0 : eccentricity[curve]) * Math.sin(forceCircularOrbit[curve] ? 0.0 : omega[curve])));
                                            tau[curve] = (t14[curve] - t23[curve]) / 2.0;
                                            double sin2tTpioP = Math.pow(Math.sin(t14[curve] * Math.PI / orbitalPeriod[curve]), 2);
                                            stellarDensity[curve] = 0.0189 / (orbitalPeriod[curve] * orbitalPeriod[curve]) * Math.pow(((1.0 + bestFit[curve][1]) * (1.0 + bestFit[curve][1]) - bp2 * (1 - sin2tTpioP)) / sin2tTpioP, 1.5);

                                            if (bpLock[curve]) {
                                                priorCenterSpinner[curve][4].setValue(bestFit[curve][4]*180.0/Math.PI);
                                            }
                                            else {
                                                bpSpinner[curve].setValue(bp[curve]);
                                            }
                                            t14Label[curve].setText(Double.isNaN(t14[curve]) ? "NaN" : sixPlaces.format(t14[curve]));
                                            t14HoursLabel[curve].setText(Double.isNaN(t14[curve]) ? "NaN" : IJU.decToSex(24 * t14[curve], 0, 24, false));
                                            t23Label[curve].setText(Double.isNaN(t23[curve]) ? "NaN" : sixPlaces.format(t23[curve]));
                                            tauLabel[curve].setText(Double.isNaN(tau[curve]) ? "NaN" : sixPlaces.format(tau[curve]));
                                            stellarDensityLabel[curve].setText(Double.isNaN(stellarDensity[curve]) ? "NaN" : fourPlaces.format(stellarDensity[curve]));
                                            //if (!MultiAperture_.cancelled) { //todo come up with heuristic to only run the transit model when needed
                                                double midpointFlux = IJU.transitModel(new double[]{bestFit[curve][3]}, bestFit[curve][0], bestFit[curve][4], bestFit[curve][1], bestFit[curve][2], bestFit[curve][3], orbitalPeriod[curve], forceCircularOrbit[curve] ? 0.0 : eccentricity[curve], forceCircularOrbit[curve] ? 0.0 : omega[curve], bestFit[curve][5], bestFit[curve][6], useLonAscNode[curve], lonAscNode[curve], true)[0];
                                                transitDepth[curve] = (1-(midpointFlux/bestFit[curve][0]))*1000;
                                            //}
                                            transitDepthLabel[curve].setText(Double.isNaN(transitDepth[curve]) ? "NaN" : threeDigitsTwoPlaces.format(transitDepth[curve]));
                                        } else {
                                            //bpSpinner[curve].setValue(0.0);
                                            t14Label[curve].setText("");
                                            t14HoursLabel[curve].setText("");
                                            t23Label[curve].setText("");
                                            tauLabel[curve].setText("");
                                            stellarDensityLabel[curve].setText("");
                                            transitDepthLabel[curve].setText("");
                                        }
                                        chi2dof[curve] = minimization.getMinimum();
                                        bic[curve] = chi2dof[curve] * (detrendXs[curve].length - bestFit[curve].length) + bestFit[curve].length * Math.log(detrendXs[curve].length);
                                        chi2dofLabel[curve].setText(sixPlaces.format(chi2dof[curve]));
                                        bicLabel[curve].setText(fourPlaces.format(bic[curve]));
                                        dofLabel[curve].setText("" + dof[curve]);
                                        chi2Label[curve].setText(fourPlaces.format(chi2[curve]));
                                        stepsTakenLabel[curve].setText("" + nTries[curve]);

                                        fp = fittedDetrendParStart;
                                        for (int p = 7; p < maxFittedVars; p++) {
                                            if (isFitted[curve][p]) {
                                                detrendFactor[curve][p - 7] = coeffs[curve][fp++];
                                            } else if (lockToCenter[curve][p]) {
                                                detrendFactor[curve][p - 7] = priorCenter[curve][p];
                                            }
                                            if (detrendVarDisplayed[curve] == p - 7) {
                                                detrendfactorspinner[curve].setValue(detrendFactor[curve][p - 7]);
                                            }
                                        }

                                        setFittedParametersBorderColor(curve, converged[curve] ? convergedBorder : failedBorder);
                                        if (converged[curve]) {
                                            detrendpanelgroup[curve].setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY));
                                        } else {
                                            detrendpanelgroup[curve].setBorder(BorderFactory.createLineBorder(Color.RED));
                                        }
                                    } else if (useNelderMeadChi2ForDetrend) {
                                        minimization.removeConstraints();
                                        start[curve] = new double[detrendVars.length];
                                        step[curve] = new double[detrendVars.length];
                                        for (int i = 0; i < start[curve].length; i++) {
                                            start[curve][i] = 0.0;
                                            step[curve][i] = 1.0;
                                        }
                                        double fTol = 1e-10;
                                        int nMax = 20000;
                                        minimization.nelderMead(new FitDetrendChi2(), start[curve], step[curve], fTol, nMax);
                                        coeffs[curve] = minimization.getParamValues();

                                        varCount = 0;
                                        for (int v = 0; v < maxDetrendVars; v++) {
                                            if (detrendIndex[curve][v] != 0 && detrendYDNotConstant[v]) {
                                                detrendFactor[curve][v] = coeffs[curve][varCount];
                                                if (detrendVarDisplayed[curve] == v) {
                                                    detrendfactorspinner[curve].setValue(coeffs[curve][varCount]);
                                                }
                                                varCount++;
                                            }
                                        }

                                        if (minimization.getConvStatus()) {
                                            detrendpanelgroup[curve].setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY));
                                        } else {
                                            detrendpanelgroup[curve].setBorder(BorderFactory.createLineBorder(Color.RED));
                                        }

                                    } else  //use regression
                                    {
                                        //                                          IJ.log("Setting up Regression, Curve = "+curve+" ,VarsUsed = "+detrendVarsUsed[curve]+" ,detrendCount = "+detrendCount);
                                        Regression regression = new Regression(detrendVars, detrendYs[curve]);
                                        //                                          IJ.log("Starting Regression");
                                        regression.linear();
                                        //                                            IJ.log("Getting coeffs");
                                        coeffs[curve] = regression.getCoeff();

                                        varCount = 1;
                                        for (int v = 0; v < maxDetrendVars; v++) {
                                            if (detrendIndex[curve][v] != 0 && detrendYDNotConstant[v]) {
                                                detrendFactor[curve][v] = coeffs[curve][varCount];
                                                if (detrendVarDisplayed[curve] == v) {
                                                    detrendfactorspinner[curve].setValue(coeffs[curve][varCount]);
                                                }
                                                varCount++;
                                            }
                                        }


                                        detrendpanelgroup[curve].setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY));
                                    }
                                }

                                if (detrendFitIndex[curve] == 9 && useTransitFit[curve]) {
                                    planetRadiusLabel[curve].setText(IJU.planetRadiusFromTeff(teff[curve], bestFit[curve][1]));
                                    createDetrendModel = false;
                                    xModel1[curve] = detrendXs[curve];
                                    int xModel2Len = plotSizeX + 1;
                                    double xModel2Step = ((useDMarker4 && fitMax[curve] < xPlotMax ? fitMax[curve] : xPlotMax) - (useDMarker1 && fitMin[curve] > xPlotMin ? fitMin[curve] : xPlotMin)) / (xModel2Len - 1);
                                    xModel2[curve] = new double[xModel2Len];
                                    xModel2[curve][0] = useDMarker1 && fitMin[curve] > xPlotMin ? fitMin[curve] : xPlotMin;
                                    for (int i = 1; i < xModel2Len; i++) {
                                        xModel2[curve][i] = xModel2[curve][i - 1] + xModel2Step;
                                    }


                                    yModel1[curve] = IJU.transitModel(xModel1[curve], bestFit[curve][0], bestFit[curve][4], bestFit[curve][1], bestFit[curve][2], bestFit[curve][3], orbitalPeriod[curve], forceCircularOrbit[curve] ? 0.0 : eccentricity[curve], forceCircularOrbit[curve] ? 0.0 : omega[curve], bestFit[curve][5], bestFit[curve][6], useLonAscNode[curve], lonAscNode[curve], true);

                                    yModel2[curve] = IJU.transitModel(xModel2[curve], bestFit[curve][0], bestFit[curve][4], bestFit[curve][1], bestFit[curve][2], bestFit[curve][3], orbitalPeriod[curve], forceCircularOrbit[curve] ? 0.0 : eccentricity[curve], forceCircularOrbit[curve] ? 0.0 : omega[curve], bestFit[curve][5], bestFit[curve][6], useLonAscNode[curve], lonAscNode[curve], true);

                                    // f0 = param[curve][0]; // baseline flux
                                    // p0 = param[curve][1]; // r_p/r_*
                                    // ar = param[curve][2]; // a/r_*
                                    // tc = param[curve][3]; //transit center time
                                    // incl = param[curve][4];  //inclination
                                    // u1 = param[curve][5];
                                    // u2 = param[curve][6];
                                } else {
                                    planetRadiusLabel[curve].setText("");
                                    createDetrendModel = true;
                                }
                            } else {
                                xModel1[curve] = null;
                                xModel2[curve] = null;
                                yModel1[curve] = null;
                                yModel2[curve] = null;
                                detrendpanelgroup[curve].setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY));
                            }

                        } else {
                            xModel1[curve] = null;
                            xModel2[curve] = null;
                            yModel1[curve] = null;
                            yModel2[curve] = null;
                            detrendpanelgroup[curve].setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY));
                        }
                    } else {
                        for (int j = 0; j < nn[curve]; j++) {
                            boolean noNaNs = true;
                            if (!Double.isNaN(y[curve][j])) {
                                for (int v = 0; v < maxDetrendVars; v++) {
                                    if (detrendIndex[curve][v] != 0 && Double.isNaN(detrend[curve][v][j])) {
                                        noNaNs = false;
                                        break;
                                    }
                                }
                            } else {
                                noNaNs = false;
                            }
                            if (noNaNs) {
                                yAverage[curve] += y[curve][j];
//                                        for(int v = 0; v < maxDetrendVars; v++)
//                                            {
//                                            detrendAverage[v] += detrend[curve][v][j];
//                                            }
                                avgCount++;
                            }
                        }
                        if (avgCount > 0) {
                            yAverage[curve] /= avgCount;
//                                    for(int v = 0; v < maxDetrendVars; v++)
//                                        {
//                                        detrendAverage[v] /= avgCount;
//                                        }
                        }
                        xModel1[curve] = new double[2];
                        xModel1[curve][0] = Math.max(fitMin[curve], xPlotMin);
                        xModel1[curve][1] = Math.min(fitMax[curve], xPlotMax);
                        xModel2[curve] = null;
                        yModel1[curve] = new double[2];
                        yModel1[curve][0] = yAverage[curve];
                        yModel1[curve][1] = yAverage[curve];
                        yModel2[curve] = null;
                        detrendpanelgroup[curve].setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY));
                    }

                    if (divideNotSubtract) {
                        double trend;
                        if (yAverage[curve] != 0.0) {
                            for (int j = 0; j < nn[curve]; j++) {
                                trend = yAverage[curve];
                                for (int v = 0; v < maxDetrendVars; v++) {
                                    if (detrendIndex[curve][v] != 0 && detrendYDNotConstant[v]) {
                                        trend += detrendFactor[curve][v] * (detrend[curve][v][j]);//-detrendAverage[v]);
                                    }
                                }
                                trend /= yAverage[curve];
                                if (trend != 0.0) {
                                    y[curve][j] /= trend;
                                    if (hasErrors[curve] || hasOpErrors[curve]) {
                                        yerr[curve][j] /= trend;
                                    }
                                }
                            }
                        }
                    } else {
                        double trend;
                        for (int j = 0; j < nn[curve]; j++) {
                            trend = 0.0;
                            for (int v = 0; v < maxDetrendVars; v++) {
                                if (detrendIndex[curve][v] != 0 && detrendYDNotConstant[v]) {
                                    trend += detrendFactor[curve][v] * (detrend[curve][v][j]);//-detrendAverage[v]);
                                }
                            }
                            if (hasErrors[curve] || hasOpErrors[curve]) {
                                yerr[curve][j] /= (y[curve][j] / (y[curve][j] - trend));
                            }
                            y[curve][j] -= trend;
                        }
                    }
                } else {
                    if(detrendpanelgroup[curve] != null) detrendpanelgroup[curve].setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY));
                }

                if (detrendFitIndex[curve] == 9 && useTransitFit[curve] && yModel1[curve] != null) {
                    int cnt = 0;
                    int len = yModel1[curve].length;
                    residual[curve] = new double[len];
                    if (hasErrors[curve] || hasOpErrors[curve]) yModel1Err[curve] = new double[len];
                    for (int j = 0; j < nn[curve]; j++) {
                        if (cnt < len && !Double.isNaN(x[curve][j]) && !Double.isNaN(y[curve][j]) && x[curve][j] > fitMin[curve] && x[curve][j] < fitMax[curve]) {
                            residual[curve][cnt] = y[curve][j] - yModel1[curve][cnt];
                            if (hasErrors[curve] || hasOpErrors[curve]) yModel1Err[curve][cnt] = yerr[curve][j];
                            cnt++;
                        }
                    }
                }

                if (showSigmaForAllCurves && detrendFitIndex[curve] < 2) //calculate standard deviation using all data
                {
                    int cnt = 0;
                    detrendYAverage[curve] = 0;
                    double y2Ave = 0;
                    for (int j = 0; j < nn[curve]; j++) {
                        if (!Double.isNaN(y[curve][j])) {
                            cnt++;
                            y2Ave += y[curve][j] * y[curve][j];
                            detrendYAverage[curve] += y[curve][j];
                        }
                    }
                    if (cnt > 0) {
                        y2Ave /= cnt;
                        detrendYAverage[curve] /= cnt;
                        sigma[curve] = Math.sqrt(y2Ave - detrendYAverage[curve] * detrendYAverage[curve]);
                    } else { detrendYAverage[curve] = 1; }
                } else if ((showSigmaForDetrendedCurves || showSigmaForAllCurves || residual[curve] != null) && detrendFitIndex[curve] > 1) //calculate standard deviation using fit/detrend region
                {
                    int cnt = 0;
                    detrendYAverage[curve] = 0;
                    double y2Ave = 0;
                    if (detrendFitIndex[curve] == 4) {
                        for (int j = 0; j < nn[curve]; j++) {
                            if (!Double.isNaN(y[curve][j]) && ((x[curve][j] > fitMin[curve] && x[curve][j] < fitLeft[curve]) || (x[curve][j] > fitRight[curve] && x[curve][j] < fitMax[curve]))) {
                                y2Ave += y[curve][j] * y[curve][j];
                                detrendYAverage[curve] += y[curve][j];
                                cnt++;
                            }
                        }

                    } else {
                        sigma[curve] = 0.0;
                        for (int j = 0; j < nn[curve]; j++) {
                            if (!Double.isNaN(x[curve][j]) && !Double.isNaN(y[curve][j]) && x[curve][j] > fitMin[curve] && x[curve][j] < fitMax[curve]) {
                                if (detrendFitIndex[curve] == 9 && useTransitFit[curve] && residual[curve] != null) {
                                    sigma[curve] += residual[curve][cnt] * residual[curve][cnt];
                                } else {
                                    y2Ave += y[curve][j] * y[curve][j];
                                }
                                detrendYAverage[curve] += y[curve][j];
                                cnt++;
                            }
                        }
                    }

                    if (cnt > 0) {
                        if (detrendFitIndex[curve] == 9 && useTransitFit[curve] && residual[curve] != null) {
                            sigma[curve] = Math.sqrt(sigma[curve] / cnt);
                            detrendYAverage[curve] = 0.0;
                            cnt = 0;
                            for (int nnn = 0; nnn < xModel1[curve].length; nnn++) {
                                if (((xModel1[curve][nnn] > fitMin[curve] && xModel1[curve][nnn] < fitLeft[curve]) || (xModel1[curve][nnn] > fitRight[curve] && xModel1[curve][nnn] < fitMax[curve]))) {
                                    detrendYAverage[curve] += yModel1[curve][nnn];
                                    cnt++;
                                }
                            }
                            if (cnt > 0) {
                                detrendYAverage[curve] /= cnt;
                            } else {
//                                        detrendYAverage[curve]=1;
                                detrendYAverage[curve] = 0.0;
                                cnt = 0;
                                for (int nnn = 0; nnn < xModel1[curve].length; nnn++) {
                                    detrendYAverage[curve] += yModel1[curve][nnn];
                                    cnt++;
                                }
                                if (cnt > 0) {
                                    detrendYAverage[curve] /= cnt;
                                } else {
                                    detrendYAverage[curve] = 1;
                                }
                            }
                        } else {
                            y2Ave /= cnt;
                            detrendYAverage[curve] /= cnt;
                            sigma[curve] = Math.sqrt(y2Ave - detrendYAverage[curve] * detrendYAverage[curve]);
                        }
                    } else { detrendYAverage[curve] = 1; }
                }

                if (createDetrendModel) {
                    createDetrendModel = false;
                    double average = 0.0;
                    double count = 0;
                    double invVar;
                    if (useNelderMeadChi2ForDetrend) {
                        if (detrendFitIndex[curve] == 4) {
                            for (int j = 0; j < nn[curve]; j++) {
                                if (!Double.isNaN(y[curve][j]) && !Double.isNaN(x[curve][j]) && ((x[curve][j] > fitMin[curve] && x[curve][j] < fitLeft[curve] || (x[curve][j] > fitRight[curve] && x[curve][j] < fitMax[curve])))) {
                                    invVar = 1 / (yerr[curve][j] * yerr[curve][j]);
                                    average += y[curve][j] * invVar;
                                    count += invVar;
                                }
                            }
                        } else {
                            for (int j = 0; j < nn[curve]; j++) {
                                if (!Double.isNaN(y[curve][j]) && !Double.isNaN(x[curve][j]) && x[curve][j] > fitMin[curve] && x[curve][j] < fitMax[curve]) {
                                    invVar = 1 / (yerr[curve][j] * yerr[curve][j]);
                                    average += y[curve][j] * invVar;
                                    count += invVar;
                                }
                            }
                        }

                        if (count == 0) {
                            average = 0.0;
                            for (int j = 0; j < nn[curve]; j++) {
                                if (!Double.isNaN(y[curve][j]) && !Double.isNaN(x[curve][j])) {
                                    invVar = 1 / (yerr[curve][j] * yerr[curve][j]);
                                    average += y[curve][j] * invVar;
                                    count += invVar;
                                }
                            }
                        }
                    } else {
                        if (detrendFitIndex[curve] == 4) {
                            for (int j = 0; j < nn[curve]; j++) {
                                if (!Double.isNaN(y[curve][j]) && !Double.isNaN(x[curve][j]) && ((x[curve][j] > fitMin[curve] && x[curve][j] < fitLeft[curve]) || (x[curve][j] > fitRight[curve] && x[curve][j] < fitMax[curve]))) {
                                    average += y[curve][j];
                                    count += 1.0;
                                }
                            }
                        } else {
                            for (int j = 0; j < nn[curve]; j++) {
                                if (!Double.isNaN(y[curve][j]) && !Double.isNaN(x[curve][j]) && x[curve][j] > fitMin[curve] && x[curve][j] < fitMax[curve]) {
                                    average += y[curve][j];
                                    count += 1.0;
                                }
                            }
                        }

                        if (count == 0) {
                            average = 0.0;
                            for (int j = 0; j < nn[curve]; j++) {
                                if (!Double.isNaN(y[curve][j]) && !Double.isNaN(x[curve][j])) {
                                    average += y[curve][j];
                                    count += 1.0;
                                }
                            }
                        }
                    }

                    if (count != 0) {
                        detrendYAverage[curve] = average / count;
                    }


                    if (detrendFitIndex[curve] == 9) {
                        xModel1[curve] = detrendXs[curve];
                        xModel2[curve] = new double[2];
                        xModel2[curve][0] = Math.max(fitMin[curve], xPlotMin);
                        xModel2[curve][1] = Math.min(fitMax[curve], xPlotMax);
                        yModel1[curve] = new double[xModel1[curve].length];
                        for (int i = 0; i < xModel1[curve].length; i++)
                            yModel1[curve][i] = detrendYAverage[curve];
                        yModel2[curve] = new double[2];
                        yModel2[curve][0] = detrendYAverage[curve];
                        yModel2[curve][1] = detrendYAverage[curve];
                    } else //if (useNelderMeadChi2ForDetrend)
                    {
                        yModel1[curve] = new double[2];
                        yModel1[curve][0] = detrendYAverage[curve];
                        yModel1[curve][1] = detrendYAverage[curve];
                        yModel2[curve] = new double[2];
                        yModel2[curve][0] = detrendYAverage[curve];
                        yModel2[curve][1] = detrendYAverage[curve];
                    }
//                            else  //regression
//                                {
//                                yModel1[curve] = new double[2];
//                                yModel1[curve][0] = detrendYAverage[curve] + coeffs[curve][0];
//                                yModel1[curve][1] = detrendYAverage[curve] + coeffs[curve][0];
//                                yModel2[curve] = new double[2];
//                                yModel2[curve][0] = detrendYAverage[curve] + coeffs[curve][0];
//                                yModel2[curve][1] = detrendYAverage[curve] + coeffs[curve][0];
//                                }
                }


                if (normIndex[curve] != 0) {
                    double normMin = (useDMarker1 ? dMarker1Value : Double.NEGATIVE_INFINITY) + xOffset;
                    double normMax = (useDMarker4 ? dMarker4Value : Double.POSITIVE_INFINITY) + xOffset;
                    double normLeft = dMarker2Value + xOffset;
                    double normRight = dMarker3Value + xOffset;

//                            if ((xlabel2[firstCurve].contains("J.D.") || xlabel2[firstCurve].contains("JD")) && showXAxisNormal)
//                                {
//                                normMin += (int)xPlotMin;
//                                normMax += (int)xPlotMin;
//                                normLeft += (int)xPlotMin;
//                                normRight += (int)xPlotMin;
//                                }
                    double normAverage = 0.0;
                    double normCount = 0;
                    double invVar = 0;
                    switch (normIndex[curve]) {
                        case 1: // left of D2
                            normMax = normLeft;
                            break;
                        case 2: // right of D3
                            normMin = normRight;
                            break;
                        case 3: // outside D2 and D3
                            break;
                        case 4: // inside D2 and D3
                            normMin = normLeft;
                            normMax = normRight;
                            break;
                        case 5: // left of D3
                            normMax = normRight;
                            break;
                        case 6: // right of D2
                            normMin = normLeft;
                            break;
                        case 7: // use all data
                            break;
                        default:
                            normIndex[curve] = 0;
                            normMax = normMin;
                            break;
                    }
                    if (detrendFitIndex[curve] == 9 && useTransitFit[curve] && detrendXs[curve] != null && detrendYEs[curve] != null && yModel1[curve] != null) {
                        int nnn = detrendXs[curve].length;
                        if (normIndex[curve] == 3) {
                            for (int j = 0; j < nnn; j++) {
                                if (!Double.isNaN(yModel1[curve][j]) && !Double.isNaN(detrendXs[curve][j]) && ((detrendXs[curve][j] > normMin && detrendXs[curve][j] < normLeft) || (detrendXs[curve][j] > normRight && detrendXs[curve][j] < normMax))) {
                                    invVar = 1 / (detrendYEs[curve][j] * detrendYEs[curve][j]);
                                    normAverage += yModel1[curve][j] * invVar;
                                    normCount += invVar;
                                }
                            }
                        } else {
                            for (int j = 0; j < nnn; j++) {
                                if (!Double.isNaN(yModel1[curve][j]) && !Double.isNaN(detrendXs[curve][j]) && detrendXs[curve][j] > normMin && detrendXs[curve][j] < normMax) {
                                    invVar = 1 / (detrendYEs[curve][j] * detrendYEs[curve][j]);
                                    normAverage += yModel1[curve][j] * invVar;
                                    normCount += invVar;
                                }
                            }
                        }

                        if (normCount == 0) {
                            normAverage = 0.0;
                            for (int j = 0; j < yModel1[curve].length; j++) {
                                if (!Double.isNaN(yModel1[curve][j]) && !Double.isNaN(detrendXs[curve][j])) {
                                    invVar = 1 / (detrendYEs[curve][j] * detrendYEs[curve][j]);
                                    normAverage += yModel1[curve][j] * invVar;
                                    normCount += invVar;
                                }
                            }
                        }
                    } else if (useNelderMeadChi2ForDetrend) {
                        if (normIndex[curve] == 3) {
                            for (int j = 0; j < nn[curve]; j++) {
                                if (!Double.isNaN(y[curve][j]) && !Double.isNaN(x[curve][j]) && ((x[curve][j] > normMin && x[curve][j] < normLeft) || (x[curve][j] > normRight && x[curve][j] < normMax))) {
                                    invVar = 1 / (yerr[curve][j] * yerr[curve][j]);
                                    normAverage += y[curve][j] * invVar;
                                    normCount += invVar;
                                }
                            }
                        } else {
                            for (int j = 0; j < nn[curve]; j++) {
                                if (!Double.isNaN(y[curve][j]) && !Double.isNaN(x[curve][j]) && x[curve][j] > normMin && x[curve][j] < normMax) {
                                    invVar = 1 / (yerr[curve][j] * yerr[curve][j]);
                                    normAverage += y[curve][j] * invVar;
                                    normCount += invVar;
                                }
                            }
                        }

                        if (normCount == 0) {
                            normAverage = 0.0;
                            for (int j = 0; j < nn[curve]; j++) {
                                if (!Double.isNaN(y[curve][j]) && !Double.isNaN(x[curve][j])) {
                                    invVar = 1 / (yerr[curve][j] * yerr[curve][j]);
                                    normAverage += y[curve][j] * invVar;
                                    normCount += invVar;
                                }
                            }
                        }
                    } else {
                        if (normIndex[curve] == 3) {
                            for (int j = 0; j < nn[curve]; j++) {
                                if (!Double.isNaN(y[curve][j]) && !Double.isNaN(x[curve][j]) && ((x[curve][j] > normMin && x[curve][j] < normLeft) || (x[curve][j] > normRight && x[curve][j] < normMax))) {
                                    normAverage += y[curve][j];
                                    normCount += 1.0;
                                }
                            }
                        } else {
                            for (int j = 0; j < nn[curve]; j++) {
                                if (!Double.isNaN(y[curve][j]) && !Double.isNaN(x[curve][j]) && x[curve][j] > normMin && x[curve][j] < normMax) {
                                    normAverage += y[curve][j];
                                    normCount += 1.0;
                                }
                            }
                        }

                        if (normCount == 0) {
                            normAverage = 0.0;
                            for (int j = 0; j < nn[curve]; j++) {
                                if (!Double.isNaN(y[curve][j]) && !Double.isNaN(x[curve][j])) {
                                    normAverage += y[curve][j];
                                    normCount += 1.0;
                                }
                            }
                        }
                    }

                    if (normAverage == 0.0 || normCount == 0) {
                        normAverage = 1.0;
                    } else {
                        normAverage /= normCount;
                    }

//                    IJ.log("normAverage  = "+normAverage);
//                    IJ.log("fit baseline = "+bestFit[curve][0]);
                    if (detrendFitIndex[curve] == 9 && useTransitFit[curve] && detrendXs[curve] != null && detrendYEs[curve] != null && yModel1[curve] != null) {
                        normAverage = bestFit[curve][0];
                    }
                    for (int j = 0; j < nn[curve]; j++) {
                        if (hasErrors[curve] || hasOpErrors[curve]) {
                            yerr[curve][j] /= normAverage;
                        }
                        y[curve][j] /= normAverage;
                    }
                    detrendYAverage[curve] /= normAverage;
                    sigma[curve] /= normAverage;
                    sigmaLabel[curve].setText(sixPlaces.format(sigma[curve] * 1000));
                    sigmaLabel[curve].setToolTipText("<html>RMS of model residuals (normalized) in parts per thousand (ppt).<br>" + "Green Border: fit converged<br>" + "Red Border: fit did not converge<br>" + "Gray Border: no fit in this session</html>");
                    sigmaPanel[curve].setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(subBorderColor, 1), "RMS (ppt)", TitledBorder.CENTER, TitledBorder.TOP, p11, Color.darkGray));

                    if (yModel1[curve] != null) {
                        for (int nnn = 0; nnn < yModel1[curve].length; nnn++) {
                            yModel1[curve][nnn] /= normAverage;
                        }
                    }
                    if (yModel1Err[curve] != null) {
                        for (int nnn = 0; nnn < yModel1Err[curve].length; nnn++) {
                            yModel1Err[curve][nnn] /= normAverage;
                        }
                    }
                    if (residual[curve] != null) {
                        for (int nnn = 0; nnn < residual[curve].length; nnn++) {
                            residual[curve][nnn] /= normAverage;
                        }
                    }
                    if (yModel2[curve] != null) {
                        for (int nnn = 0; nnn < yModel2[curve].length; nnn++) {
                            yModel2[curve][nnn] /= normAverage;
                        }
                    }
                }

                //PERFORM MAGNITUDE CONVERSION IF APPLICABLE
                if (mmag[curve]) {
                    if (normIndex[curve] == 0 && mmagrefs > 0) {
                        int cnt = 0;
                        int numgood = 0;
                        while (numgood < mmagrefs && cnt < nn[curve])  //Prepare mmag calulation baseline reference level
                        {
                            if (!Double.isNaN(y[curve][cnt]) && !Double.isNaN(x[curve][cnt])) {
                                baseline[curve] += y[curve][cnt];
                                numgood++;
                            }
                            cnt++;
                        }
                        baseline[curve] /= (numgood == 0 ? 1 : numgood);
                        baseline[curve] = (baseline[curve] == 0 ? 1 : baseline[curve]);
                    } else {
                        baseline[curve] = 1.0;
                    }

                    if (residual[curve] != null) {
                        int nnn = 0;
                        int len = residual[curve].length;
                        for (int j = 0; j < nn[curve]; j++) {
                            if (nnn < len && !Double.isNaN(x[curve][j]) && !Double.isNaN(y[curve][j]) && x[curve][j] > fitMin[curve] && x[curve][j] < fitMax[curve]) {
                                residual[curve][nnn] = 2.5 * Math.log10(1.0 + residual[curve][nnn] / y[curve][j]);
                                if (yModel1Err[curve] != null) {
                                    yModel1Err[curve][nnn] = 2.5 * Math.log10(1.0 + yModel1Err[curve][nnn] / y[curve][j]);
                                }
                                nnn++;
                            }
                        }
                    }

                    int cnt = 0;
                    for (int j = 0; j < nn[curve]; j++) {
                        if (hasErrors[curve] || hasOpErrors[curve]) {
                            yerr[curve][j] = 2.5 * Math.log10(1.0 + yerr[curve][j] / y[curve][j]);  // yMultiplierFactor*
                        }
                        y[curve][j] = magSign * 2.5 * Math.log10(y[curve][j] / baseline[curve]);   // yMultiplierFactor*
                    }
                    sigma[curve] = 2.5 * Math.log10(1.0 + sigma[curve] / baseline[curve]);
                    detrendYAverage[curve] = magSign * 2.5 * Math.log10(detrendYAverage[curve] / baseline[curve]);
                    sigmaLabel[curve].setText(fourPlaces.format(sigma[curve] * 1000.0));
                    sigmaLabel[curve].setToolTipText("<html>RMS of model residuals (mmag).<br>" + "Green Border: fit converged<br>" + "Red Border: fit did not converge<br>" + "Gray Border: no fit in this session</html>");
                    sigmaPanel[curve].setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(subBorderColor, 1), "RMS (mmag)", TitledBorder.CENTER, TitledBorder.TOP, p11, Color.darkGray));


                    if (yModel1[curve] != null) {
                        for (int nnn = 0; nnn < yModel1[curve].length; nnn++) {
                            yModel1[curve][nnn] = magSign * 2.5 * Math.log10(yModel1[curve][nnn] / baseline[curve]);   //  yMultiplierFactor*
                        }
                    }

                    if (yModel2[curve] != null) {
                        for (int nnn = 0; nnn < yModel2[curve].length; nnn++) {
                            yModel2[curve][nnn] = magSign * 2.5 * Math.log10(yModel2[curve][nnn] / baseline[curve]);   // yMultiplierFactor*
                        }
                    }

                    // Convert Depth to mmag
                    double magDepth = 2.5 * Math.log10(1.0 + transitDepth[curve]/1000);
                    transitDepthLabel[curve].setText(fourPlaces.format(magDepth * 1000));
                    transitDepthLabel[curve].setToolTipText("<html>Depth defined as transit model flux deficit at mid-transit (Tc) in mmag.<br>" + "Green Border: fit converged<br>" + "Red Border: fit did not converge<br>" + "Gray Border: no fit in this session</html>");
                    transitDepthPanel[curve].setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(subBorderColor, 1), "Depth (mmag)", TitledBorder.CENTER, TitledBorder.TOP, p11, Color.darkGray));
                } else {
                    transitDepthLabel[curve].setText(Double.isNaN(transitDepth[curve]) ? "NaN" : threeDigitsTwoPlaces.format(transitDepth[curve]));
                    transitDepthLabel[curve].setToolTipText("<html>Depth defined as transit model flux deficit at mid-transit (Tc) in parts per thousand (ppt).<br>" + "Green Border: fit converged<br>" + "Red Border: fit did not converge<br>" + "Gray Border: no fit in this session</html>");
                    transitDepthPanel[curve].setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(subBorderColor, 1), "Depth (ppt)", TitledBorder.CENTER, TitledBorder.TOP, p11, Color.darkGray));
                }
                if (normIndex[curve] == 0 && !mmag[curve]) {
                    sigmaLabel[curve].setText(sixPlaces.format(sigma[curve]));
                    sigmaLabel[curve].setToolTipText("<html>RMS of model residuals (raw).<br>" + "WARNING: This is the standard deviation/RMS of the raw data from the model.<br>" + "WARNING: Consider using either normalized or mmag output data.<br>" + "Green Border: fit converged<br>" + "Red Border: fit did not converge<br>" + "Gray Border: no fit in this session</html>");
                    sigmaPanel[curve].setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(subBorderColor, 1), "RMS (raw)", TitledBorder.CENTER, TitledBorder.TOP, p11, Color.darkGray));
                }


            } else {
                detrendpanelgroup[curve].setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY));
            }
            if (refStarChanged || detrendParChanged) setRMSBICBackgroundNewThread(curve);
        }


        // PERFORM Y-AUTOSCALING TO ONE OR MORE CURVES

        dy = 0.0;

        yPlotMin = yMin;
        yPlotMax = yMax;

        yautoscalemin = Double.POSITIVE_INFINITY;
        yautoscalemax = Double.NEGATIVE_INFINITY;

        for (int curve = 0; curve < maxCurves; curve++) {
            double resMin, resMax;
            if (plotY[curve]) {
                yMn[curve] = (showErrors[curve] && (hasErrors[curve] || hasOpErrors[curve])) ? minOf(y[curve], yerr[curve], nn[curve]) : minOf(y[curve], nn[curve]); //FIND MIN AND MAX Y OF EACH SELECTED DATASET
                yMx[curve] = (showErrors[curve] && (hasErrors[curve] || hasOpErrors[curve])) ? maxOf(y[curve], yerr[curve], nn[curve]) : maxOf(y[curve], nn[curve]);
                yWidthOrig[curve] = yMx[curve] - yMn[curve];
                if (showResidual[curve] && residual[curve] != null && useTransitFit[curve] && detrendFitIndex[curve] == 9) {
                    resMin = (showResidualError[curve] && yModel1Err[curve] != null) ? resMinOf(residual[curve], yModel1Err[curve], residual[curve].length, detrendYAverage[curve] + (force[curve] ? yWidthOrig[curve] * autoResidualShift[curve] / autoScaleFactor[curve] : residualShift[curve] / (normIndex[curve] != 0 && !mmag[curve] && !force[curve] ? 1.0 : yMultiplierFactor))) : resMinOf(residual[curve], residual[curve].length, detrendYAverage[curve] + (force[curve] ? yWidthOrig[curve] * autoResidualShift[curve] / autoScaleFactor[curve] : residualShift[curve] / (normIndex[curve] != 0 && !mmag[curve] && !force[curve] ? 1.0 : yMultiplierFactor)));

                    resMax = (showResidualError[curve] && yModel1Err[curve] != null) ? resMaxOf(residual[curve], yModel1Err[curve], residual[curve].length, detrendYAverage[curve] + (force[curve] ? yWidthOrig[curve] * autoResidualShift[curve] / autoScaleFactor[curve] : residualShift[curve] / (normIndex[curve] != 0 && !mmag[curve] && !force[curve] ? 1.0 : yMultiplierFactor))) : resMaxOf(residual[curve], residual[curve].length, detrendYAverage[curve] + (force[curve] ? yWidthOrig[curve] * autoResidualShift[curve] / autoScaleFactor[curve] : residualShift[curve] / (normIndex[curve] != 0 && !mmag[curve] && !force[curve] ? 1.0 : yMultiplierFactor)));
                    if (resMin < yMn[curve]) yMn[curve] = resMin;
                    if (resMax > yMx[curve]) yMx[curve] = resMax;
                }
                if (normIndex[curve] != 0 && !mmag[curve] && !force[curve]) {
                    yMinimum[curve] = 1 + customScaleFactor[curve] * (yMn[curve] - 1.0) + customShiftFactor[curve];
                    yMaximum[curve] = 1 + customScaleFactor[curve] * (yMx[curve] - 1.0) + customShiftFactor[curve];
                } else if (customScaleFactor[curve] >= 0) {
                    yMinimum[curve] = force[curve] ? yMn[curve] : yMn[curve] * yMultiplierFactor * customScaleFactor[curve] + customShiftFactor[curve]; //FIND MIN AND MAX Y OF EACH SELECTED DATASET
                    yMaximum[curve] = force[curve] ? yMx[curve] : yMx[curve] * yMultiplierFactor * customScaleFactor[curve] + customShiftFactor[curve];
                } else {
                    yMinimum[curve] = force[curve] ? yMn[curve] : yMx[curve] * yMultiplierFactor * customScaleFactor[curve] + customShiftFactor[curve]; //FIND MIN AND MAX Y OF EACH SELECTED DATASET
                    yMaximum[curve] = force[curve] ? yMx[curve] : yMn[curve] * yMultiplierFactor * customScaleFactor[curve] + customShiftFactor[curve];
                }
                if (ASInclude[curve] && !force[curve]) {
                    if (yMinimum[curve] < yautoscalemin) yautoscalemin = yMinimum[curve];
                    if (yMaximum[curve] > yautoscalemax) yautoscalemax = yMaximum[curve];
                }
            }
        }

        if (Double.isInfinite(yautoscalemin)) {
            yautoscalemin = Double.isInfinite(yautoscalemax) ? 0.0 : yautoscalemax - 1.0;
        }
        if (Double.isInfinite(yautoscalemax)) yautoscalemax = yautoscalemin + 1.0;

        if (autoScaleY) {
            yPlotMin = yautoscalemin;
            yPlotMax = (yautoscalemin == yautoscalemax) ? yautoscalemin + 0.01 : yautoscalemax;
        }
        dy = (yPlotMax - yPlotMin) / 99.0;

        // IF MJD, SHOW DECIMAL DATE DUE TO float API OF PlotWindow
        yOffset = 0.0;
        if (ylabel[firstCurve].contains("J.D.") || ylabel[firstCurve].contains("JD")) {
            if (yExponent != 0) ymultiplierspinner.setValue(0);
            yJD = (int) yPlotMin;
            yOffset = yJD;
        }
        xlab = "";
        ylab = "";
        boolean xGoodLabel = false;
        boolean yGoodLabel = false;

        // MAKE X-AXIS LABEL

        for (int curve = 0; curve < maxCurves; curve++) {
            if (plotY[curve]) {
                if (showXAxisAsPhase) {
                    xlab = "Orbital Phase (periods) (T0 = " + netT0 + ", P = " + netPeriod + ")";
                    xGoodLabel = true;
                } else if (showXAxisAsDaysSinceTc) {
                    xlab = "Days Since Tc (T0 = " + netT0 + ", P = " + netPeriod + ")";
                    xGoodLabel = true;
                } else if (showXAxisAsHoursSinceTc) {
                    xlab = "Hours Since Tc (T0 = " + netT0 + ", P = " + netPeriod + ")";
                    xGoodLabel = true;
                } else if (xlabel2[curve].contains("J.D.") || xlabel2[curve].contains("JD")) {
                    if (curve == firstCurve) {
//                                    if (xExponent != 0) xmultiplierspinner.setValue(0);
//                                    jd = (int)xPlotMin;
//                                    xOffset = (double)jd;
                        xPlotMin -= xOffset;
                        xPlotMax -= xOffset;
                        if (useXColumnName) {
                            if (xlabel2[curve].trim().startsWith("J.D.-2400000")) {
                                xlab = "Geocentric Julian Date (UTC) - " + xJD;
                            } else if (xlabel2[curve].trim().startsWith("BJD_TDB")) {
                                xlab = "Barycentric Julian Date (TDB) - " + xJD + " (mid-exposure)";
                            } else if (xlabel2[curve].trim().startsWith("BJD_SOBS")) {
                                xlab = "Barycentric Julian Date (TDB) - " + xJD + " (exposure start)";
                            } else if (xlabel2[curve].trim().startsWith("BJD")) {
                                xlab = "Barycentric Julian Date (TDB) - " + xJD;
                            } else if (xlabel2[curve].trim().startsWith("HJD_UTC")) {
                                xlab = "Heliocentric Julian Date (UTC) - " + xJD + " (mid-exposure)";
                            } else if (xlabel2[curve].trim().startsWith("HJD_SOBS")) {
                                xlab = "Heliocentric Julian Date (UTC) - " + xJD + " (exposure start)";
                            } else if (xlabel2[curve].trim().startsWith("HJD")) {
                                xlab = "Heliocentric Julian Date (UTC) - " + xJD;
                            } else if (xlabel2[curve].trim().startsWith("JD_SOBS")) {
                                xlab = "Geocentric Julian Date (UTC) - " + xJD + " (exposure start)";
                            } else if (xlabel2[curve].trim().startsWith("JD_UTC")) {
                                xlab = "Geocentric Julian Date (UTC) - " + xJD + " (mid-exposure)";
                            } else if (xlabel2[curve].trim().contains("JD")) { xlab = "Julian Date - " + xJD; } else {
                                xlab = "Label Error";
                            }
                            xGoodLabel = true;
                        } else if (useXCustomName) {
                            xlab = xLegend;
                            xGoodLabel = true;
                        }
                    }
                    for (int j = 0; j < nn[curve]; j++) {
                        x[curve][j] -= xOffset;
                    }
                    if (xModel1[curve] != null) {
                        for (int nnn = 0; nnn < xModel1[curve].length; nnn++) {
                            xModel1[curve][nnn] -= xOffset;

                        }
                    }
                    if (xModel2[curve] != null) {
                        for (int nnn = 0; nnn < xModel2[curve].length; nnn++) {
                            xModel2[curve][nnn] -= xOffset;
                        }
                    }
                } else if (useXColumnName && curve == firstCurve) {
                    xlab = xlabel2[curve];
                    xGoodLabel = true;
                } else if (useXCustomName && curve == firstCurve) {
                    xlab = xLegend;
                    xGoodLabel = true;
                }
            }
        }

        if (xGoodLabel && (xExponent != 0) && showXScaleInfo) xlab += " x 1E" + xExponent;

        // MAKE Y-AXIS LABEL

        for (int curve = 0; curve < maxCurves; curve++) {
            if (plotY[curve]) {
                if (ylabel[curve].contains("J.D.") || ylabel[curve].contains("JD")) {
                    if (curve == firstCurve) {
//                                    if (yExponent != 0) ymultiplierspinner.setValue(0);
//                                    jd = (int)yPlotMin;
//                                    yOffset = (double)jd;
                        yPlotMin -= yOffset;
                        yPlotMax -= yOffset;
                        if (useYColumnName && operatorIndex[curve] != 5) {
                            if (ylabel[curve].trim().startsWith("J.D.-2400000")) {
                                ylab = "Geocentric Julian Date (UTC) - " + yJD;
                            } else if (ylabel[curve].trim().startsWith("BJD_TDB")) {
                                ylab = "Barycentric Julian Date (TDB) - " + yJD + " (mid-exposure)";
                            } else if (ylabel[curve].trim().startsWith("BJD_SOBS")) {
                                ylab = "Barycentric Julian Date (TDB) - " + yJD + " (exposure start)";
                            } else if (ylabel[curve].trim().startsWith("BJD")) {
                                ylab = "Barycentric Julian Date (TDB) - " + yJD;
                            } else if (ylabel[curve].trim().startsWith("HJD_UTC")) {
                                ylab = "Heliocentric Julian Date (UTC) - " + yJD + " (mid-exposure)";
                            } else if (ylabel[curve].trim().startsWith("HJD_SOBS")) {
                                ylab = "Heliocentric Julian Date (UTC) - " + yJD + " (exposure start)";
                            } else if (ylabel[curve].trim().startsWith("HJD")) {
                                ylab = "Heliocentric Julian Date (UTC) - " + yJD;
                            } else if (ylabel[curve].trim().startsWith("JD_UTC")) {
                                ylab = "Geocentric Julian Date (UTC) - " + yJD + " (mid-exposure)";
                            } else if (ylabel[curve].trim().startsWith("JD_SOBS")) {
                                ylab = "Geocentric Julian Date (UTC) - " + yJD + " (exposure start)";
                            } else if (ylabel[curve].trim().contains("JD")) { ylab = "Julian Date - " + yJD; } else {
                                ylab = "Label Error";
                            }
                            yGoodLabel = true;
                        } else if (useYCustomName) {
                            ylab = yLegend;
                            yGoodLabel = true;
                        } else if (useYColumnName && operatorIndex[curve] == 5) {
                            ylab = xyc1label[curve] + " - " + xyc2label[curve] + " centroid distance";
                            ylab += usePixelScale ? " (arcsecs)" : " (pixels)";
                            yGoodLabel = true;
                        }
                    }
                    for (int j = 0; j < nn[curve]; j++)
                        y[curve][j] -= yOffset;
                } else if (useYColumnName && curve == firstCurve) {
                    if (operatorIndex[curve] != 5) {
                        ylab = ylabel[firstCurve];
                        if (operatorIndex[firstCurve] != 0) {
                            ylab += opSymbol[operatorIndex[firstCurve]] + oplabel[firstCurve];
                        }
                    } else {
                        ylab = xyc1label[firstCurve] + " - " + xyc2label[firstCurve] + " centroid distance";
                        ylab += usePixelScale ? " (arcsecs)" : " (pixels)";
                    }
                    yGoodLabel = true;
                } else if (useYCustomName && curve == firstCurve) {
                    ylab = yLegend;
                    yGoodLabel = true;
                }
            }
        }
        //FINISH Y-LABEL AFTER FINDING SCALING FACTORS BELOW


        //SCALE AND SHIFT SECONDARY CURVES TO FIT ON PLOT

        yRange = yPlotMax - yPlotMin;
        yMid = yPlotMin + yRange / 2.;
        for (int curve = 0; curve < maxCurves; curve++) {
            if (plotY[curve]) {
                if (Double.isNaN(customShiftFactor[curve]) || Double.isInfinite(customShiftFactor[curve])) {
                    customShiftFactor[curve] = 0.0;
                }
                if (Double.isNaN(customScaleFactor[curve]) || Double.isInfinite(customScaleFactor[curve])) {
                    customScaleFactor[curve] = 1.0;
                }
                if (Double.isNaN(autoShiftFactor[curve]) || Double.isInfinite(autoShiftFactor[curve])) {
                    autoShiftFactor[curve] = 0.0;
                }
                if (Double.isNaN(autoScaleFactor[curve]) || Double.isInfinite(autoScaleFactor[curve])) {
                    autoScaleFactor[curve] = 1.0;
                }
                yMidpoint[curve] = (yMaximum[curve] + yMinimum[curve]) / 2.;
                yWidth[curve] = yMaximum[curve] - yMinimum[curve];
                if (yWidth[curve] == 0) yWidth[curve] = 1.0e-10;
                if (force[curve]) {
                    subtotalScaleFactor[curve] = (invertYAxis ? -1 : 1) * autoScaleFactor[curve] * yRange / yWidth[curve];
                    subtotalShiftFactor[curve] = (yMid + yRange * (invertYAxis ? -1 : 1) * autoShiftFactor[curve] - subtotalScaleFactor[curve] * yMidpoint[curve]);
                    totalScaleFactor[curve] = subtotalScaleFactor[curve];//*yMultiplierFactor;
                } else {
                    subtotalScaleFactor[curve] = customScaleFactor[curve];
                    subtotalShiftFactor[curve] = customShiftFactor[curve];
                    totalScaleFactor[curve] = (normIndex[curve] != 0 && !mmag[curve]) ? subtotalScaleFactor[curve] : subtotalScaleFactor[curve] * yMultiplierFactor;
                }
                totalShiftFactor[curve] = subtotalShiftFactor[curve];

                if (!(normIndex[curve] != 0 || mmag[curve] || force[curve])) {
                    sigma[curve] = totalScaleFactor[curve] * sigma[curve];
                }
                if (mmag[curve] && totalScaleFactor[curve] == 1000) sigma[curve] *= 1000;

                if (normIndex[curve] != 0 && !mmag[curve] && !force[curve]) {
                    detrendYAverage[curve] = 1 + totalScaleFactor[curve] * (detrendYAverage[curve] - 1.0) + subtotalShiftFactor[curve];
                } else {
                    detrendYAverage[curve] = totalScaleFactor[curve] * detrendYAverage[curve] + subtotalShiftFactor[curve];
                }
                for (int j = 0; j < nn[curve]; j++) {
                    if (hasErrors[curve] || hasOpErrors[curve]) {
                        yerr[curve][j] = totalScaleFactor[curve] * yerr[curve][j];
                    }
                    if (normIndex[curve] != 0 && !mmag[curve] && !force[curve]) {
                        y[curve][j] = 1 + totalScaleFactor[curve] * (y[curve][j] - 1.0) + subtotalShiftFactor[curve];
                    } else {
                        y[curve][j] = totalScaleFactor[curve] * y[curve][j] + subtotalShiftFactor[curve];
                    }
                }

                if (yModel1[curve] != null) {
                    for (int nnn = 0; nnn < yModel1[curve].length; nnn++) {
                        if (normIndex[curve] != 0 && !mmag[curve] && !force[curve]) {
                            yModel1[curve][nnn] = 1 + totalScaleFactor[curve] * (yModel1[curve][nnn] - 1.0) + subtotalShiftFactor[curve];
                        } else {
                            yModel1[curve][nnn] = totalScaleFactor[curve] * yModel1[curve][nnn] + subtotalShiftFactor[curve];
                        }
                        if (residual[curve] != null) {
                            residual[curve][nnn] = totalScaleFactor[curve] * residual[curve][nnn];
                        }
                    }
                }

                if (yModel1Err[curve] != null) {
                    for (int nnn = 0; nnn < yModel1Err[curve].length; nnn++) {
                        yModel1Err[curve][nnn] = totalScaleFactor[curve] * yModel1Err[curve][nnn];
                    }
                }

                if (yModel2[curve] != null) {
                    for (int nnn = 0; nnn < yModel2[curve].length; nnn++) {
                        if (normIndex[curve] != 0 && !mmag[curve] && !force[curve]) {
                            yModel2[curve][nnn] = 1 + totalScaleFactor[curve] * (yModel2[curve][nnn] - 1.0) + subtotalShiftFactor[curve];
                        } else {
                            yModel2[curve][nnn] = totalScaleFactor[curve] * yModel2[curve][nnn] + subtotalShiftFactor[curve];
                        }
                    }
                }
            }
        }

        if (yGoodLabel) {
            if (showYScaleInfo || showYShiftInfo || force[firstCurve]) {
                ylab += scaleShiftText(force[firstCurve], showYScaleInfo, showYShiftInfo, mmag[firstCurve], showYmmagInfo, totalScaleFactor[firstCurve], totalShiftFactor[firstCurve]);
            }
            if (showYNormInfo && normIndex[firstCurve] != 0 && !mmag[firstCurve] && !force[firstCurve]) {
                ylab += " (normalized)";
            }
            if (mmag[firstCurve] && showYmmagInfo) {
                if (totalScaleFactor[firstCurve] == 1000.0) { ylab += " (mmag)"; } else ylab += " (mag)";
            }

            if ((inputAverageOverSize[firstCurve] != 1) && showYAvgInfo) ylab += " (averaged data size = " + inputAverageOverSize[firstCurve] + ")";
            if (showYSymbolInfo) {
                ylab += " (" + colors[colorIndex[firstCurve]] + " " + markers[markerIndex[firstCurve]] + ")";
            }
        }


        //----------------Set up plot options------------------------------------
        plotOptions = 0;
        if (xTics) plotOptions += ij.gui.Plot.X_TICKS;
        if (yTics) plotOptions += ij.gui.Plot.Y_TICKS;
        if (xGrid) plotOptions += ij.gui.Plot.X_GRID;
        if (yGrid) plotOptions += ij.gui.Plot.Y_GRID;
        if (xNumbers) plotOptions += ij.gui.Plot.X_NUMBERS;
        if (yNumbers) plotOptions += ij.gui.Plot.Y_NUMBERS;


        plot = new Plot("Plot of " + tableName, xlab, ylab, plotOptions);
        plot.setSize(plotSizeX, plotSizeY);
        pltMinX = xPlotMin - 5. * dx;
        pltMaxX = xPlotMax + 5. * dx;
        pltMinY = yPlotMin - 5. * dy;
        pltMaxY = yPlotMax + 5. * dy;

        if (invertYAxis) {
            double yMaxTemp = pltMaxY;
            pltMaxY = pltMinY;
            pltMinY = yMaxTemp;
        }

        if (plotImageCanvas != null) //zoom != 0.0 &&
        {
            Rectangle s = plot.getDrawingFrame();
            plotMinX = totalPanOffsetX + newPanOffsetX + pltMinX + (pltMaxX - pltMinX) * ((mouseX - 1)/ s.width) * zoom;
            plotMaxX = totalPanOffsetX + newPanOffsetX + pltMaxX - (pltMaxX - pltMinX) * ((s.width - mouseX - 15) / (s.width)) * zoom;
            plotMinY = totalPanOffsetY + newPanOffsetY + pltMinY + (pltMaxY - pltMinY) * ((s.height - 15 - mouseY) / (s.height)) * zoom;
            plotMaxY = totalPanOffsetY + newPanOffsetY + pltMaxY - (pltMaxY - pltMinY) * (mouseY / (s.height)) * zoom;
        } else {
            plotMinX = pltMinX;
            plotMaxX = pltMaxX;
            plotMinY = pltMinY;
            plotMaxY = pltMaxY;
            zoom = 0.0;
            totalPanOffsetX = 0.0;
            totalPanOffsetY = 0.0;
            newPanOffsetX = 0.0;
            newPanOffsetY = 0.0;
            leftDragReleased = false;
        }

        plot.setLimits(plotMinX, plotMaxX, plotMinY, plotMaxY);
        double legPosY = legendPosY;
        for (int curve = 0; curve < maxCurves; curve++) {
            if (plotY[curve]) {
                if (showResidual[curve] && residual[curve] != null && useTransitFit[curve] && detrendFitIndex[curve] == 9) {
                    if (showModel[curve]) {
                        plot.setLineWidth((showResidualError[curve] && yModel1Err[curve] != null) ? residualLineWidth[curve] + 1 : residualLineWidth[curve]);
                        plot.setColor(residualModelColor[curve]);
                        double dLen = 7 * (plotMaxX - plotMinX) / plotSizeX;
                        double min = Math.max(fitMin[curve] - xOffset, xPlotMin);
                        double max = Math.min(fitMax[curve] - xOffset, xPlotMax);
                        double nDashes = ((max - min) / dLen);
                        double ypos = detrendYAverage[curve] + (force[curve] ? autoResidualShift[curve] * totalScaleFactor[curve] * (yWidthOrig[curve] / autoScaleFactor[curve]) : residualShift[curve]);//*(normIndex[curve] != 0 && !mmag[curve]?1.0:yMultiplierFactor));
                        for (int dashCount = 0; dashCount < nDashes; dashCount += 2) {
                            plot.drawLine(min + dLen * dashCount, ypos, min + dLen * (dashCount + 1), ypos);
                        }
                    }

                    if (residualSymbol[curve] == ij.gui.Plot.DOT) { plot.setLineWidth(4); } else plot.setLineWidth(1);
                    int len = residual[curve].length;
                    plottedResidual[curve] = Arrays.copyOf(residual[curve], len);
                    for (int nnn = 0; nnn < len; nnn++) {
                        plottedResidual[curve][nnn] += detrendYAverage[curve] + (force[curve] ? autoResidualShift[curve] * totalScaleFactor[curve] * (yWidthOrig[curve] / autoScaleFactor[curve]) : residualShift[curve]);//*(normIndex[curve] != 0 && !mmag[curve]?1.0:yMultiplierFactor));
                    }
                    plot.setColor(residualColor[curve]);
                    plot.addPoints(Arrays.copyOf(xModel1[curve], xModel1[curve].length), plottedResidual[curve], residualSymbol[curve]);
                    if (showResidualError[curve] && yModel1Err[curve] != null)     //code to replace plot.addErrorBars
                    {
                        plot.setLineWidth(1);
                        for (int nnn = 0; nnn < len; nnn++) {
                            plot.drawLine(xModel1[curve][nnn], plottedResidual[curve][nnn] - yModel1Err[curve][nnn], xModel1[curve][nnn], plottedResidual[curve][nnn] + yModel1Err[curve][nnn]);
                            plot.drawLine(xModel1[curve][nnn] - (3.0 * (plotMaxX - plotMinX) / plotSizeX), plottedResidual[curve][nnn] + yModel1Err[curve][nnn], xModel1[curve][nnn] + (3.0 * (plotMaxX - plotMinX) / plotSizeX), plottedResidual[curve][nnn] + yModel1Err[curve][nnn]);
                            plot.drawLine(xModel1[curve][nnn] - (3.0 * (plotMaxX - plotMinX) / plotSizeX), plottedResidual[curve][nnn] - yModel1Err[curve][nnn], xModel1[curve][nnn] + (3.0 * (plotMaxX - plotMinX) / plotSizeX), plottedResidual[curve][nnn] - yModel1Err[curve][nnn]);
                        }
                    }

                }
                if (((showErrors[curve] || operatorIndex[curve] == 6) && (hasErrors[curve] || hasOpErrors[curve]))) {
                    plot.setLineWidth(2);
                } else { plot.setLineWidth(1); }
                plot.setColor(color[curve]);
                if (xModel1[curve] != null && yModel1[curve] != null && xModel1[curve].length == yModel1[curve].length && detrendFitIndex[curve] != 9) {
                    plot.addPoints(Arrays.copyOf(xModel1[curve], xModel1[curve].length), Arrays.copyOf(yModel1[curve], yModel1[curve].length), ij.gui.Plot.LINE);
                }
                if (xModel2[curve] != null && yModel2[curve] != null && xModel2[curve].length == yModel2[curve].length && (detrendFitIndex[curve] != 9 || showModel[curve])) {
                    if (detrendFitIndex[curve] == 9) {
                        plot.setLineWidth(((showErrors[curve] || operatorIndex[curve] == 6) && (hasErrors[curve] || hasOpErrors[curve])) ? modelLineWidth[curve] + 1 : modelLineWidth[curve]);
                        plot.setColor(modelColor[curve]);
                    }
//                            IJ.log("xModel2["+curve+"].length="+xModel2[curve].length);
//                            IJ.log("yModel2["+curve+"].length="+yModel2[curve].length);
                    plot.addPoints(Arrays.copyOf(xModel2[curve], xModel2[curve].length), Arrays.copyOf(yModel2[curve], yModel2[curve].length), ij.gui.Plot.LINE);
                }

                plot.setColor(binDisplay[curve] ? Color.GRAY : color[curve]);

                if (binDisplay[curve] || marker[curve] == ij.gui.Plot.DOT) { plot.setLineWidth(4); } else plot.setLineWidth(1);

                plot.addPoints(Arrays.copyOf(x[curve], nn[curve]), Arrays.copyOf(y[curve], nn[curve]), binDisplay[curve] ? Plot.DOT : marker[curve]);

                plot.setLineWidth(1);

                if (binDisplay[curve]) plot.setColor(Color.gray);
                if ((showErrors[curve] || operatorIndex[curve] == 6) && (hasErrors[curve] || hasOpErrors[curve]))     //code to replace plot.addErrorBars
                {               //since plot.addErrorBars only plots with lines enabled
                    for (int j = 0; j < nn[curve]; j++) {
                        plot.drawLine(x[curve][j], y[curve][j] - yerr[curve][j], x[curve][j], y[curve][j] + yerr[curve][j]);
                        plot.drawLine(x[curve][j] - (3.0 * (plotMaxX - plotMinX) / plotSizeX), y[curve][j] + yerr[curve][j], x[curve][j] + (3.0 * (plotMaxX - plotMinX) / plotSizeX), y[curve][j] + yerr[curve][j]);
                        plot.drawLine(x[curve][j] - (3.0 * (plotMaxX - plotMinX) / plotSizeX), y[curve][j] - yerr[curve][j], x[curve][j] + (3.0 * (plotMaxX - plotMinX) / plotSizeX), y[curve][j] - yerr[curve][j]);
                    }
                }

                if (binDisplay[curve]) {
                    // Convert to JD
                    var binWidth = minutes.get(curve).first() / (24D * 60D);

                    if (binWidth == 0) {
                        binWidth = .001;
                    }

                    // Bin data
                    var binnedData = PlotDataBinning.binDataErr(Arrays.copyOf(x[curve], nn[curve]), Arrays.copyOf(y[curve], nn[curve]), Arrays.copyOf(yerr[curve], nn[curve]), binWidth);

                    if (binnedData != null) {
                        // Update bin width as the minimum was calculated at the same time
                        minutes.get(curve).second().setValue(binnedData.second() * 24D * 60D);

                        var pts = binnedData.first();

                        plot.setColor(color[curve]);
                        if (marker[curve] == ij.gui.Plot.DOT) { plot.setLineWidth(8); } else plot.setLineWidth(2);
                        plot.addPoints(pts.x(), pts.y(), marker[curve]);

                        // Calculate binned RMS
                        if (detrendFitIndex[curve] == 9 && useTransitFit[curve]) {
                            // Undo shift so the model works
                            var xModelBin = Arrays.copyOf(pts.x(), pts.x().length);
                            for (int nnn = 0; nnn < xModelBin.length; nnn++) {
                                xModelBin[nnn] += xOffset;
                            }
                            var modelBin = IJU.transitModel(xModelBin, bestFit[curve][0], bestFit[curve][4], bestFit[curve][1], bestFit[curve][2], bestFit[curve][3], orbitalPeriod[curve], forceCircularOrbit[curve] ? 0.0 : eccentricity[curve], forceCircularOrbit[curve] ? 0.0 : omega[curve], bestFit[curve][5], bestFit[curve][6], useLonAscNode[curve], lonAscNode[curve], true);

                            // I don't know why this is needed, but with this RMS behaves as expected
                            for (int nnn = 0; nnn < modelBin.length; nnn++) {
                                modelBin[nnn] /= bestFit[curve][0];
                                if (normIndex[curve] != 0 && !mmag[curve] && !force[curve]) {
                                    modelBin[nnn] = 1 + totalScaleFactor[curve] * (modelBin[nnn] - 1.0) + subtotalShiftFactor[curve];
                                } else {
                                    modelBin[nnn] = totalScaleFactor[curve] * modelBin[nnn] + subtotalShiftFactor[curve];
                                }
                            }

                            outBinRms[curve] = 1000*CurveFitter.calculateRms(curve, modelBin, pts.err(), pts.err(), pts.x(), pts.x(), pts.y(), pts.err(), bestFit[curve], detrendYAverage[curve]);
                            outBinRms[curve] *= bestFit[curve][0];
                        } else {
                            var xModelBin = Arrays.copyOf(pts.x(), pts.x().length);
                            for (int nnn = 0; nnn < xModelBin.length; nnn++) {
                                xModelBin[nnn] += xOffset;
                            }
                            outBinRms[curve] = 1000*CurveFitter.calculateRms(curve, null, pts.err(), pts.err(), xModelBin, xModelBin, pts.y(), pts.err(), bestFit[curve], detrendYAverage[curve]);
                        }
                    }
                }

                if (lines[curve] && !(marker[curve] == ij.gui.Plot.LINE)) {
                    for (int j = 0; j < nn[curve] - 1; j++) {
                        if (x[curve][j + 1] > x[curve][j]) {
                            plot.drawLine(x[curve][j], y[curve][j], x[curve][j + 1], y[curve][j + 1]);
                        }
                    }
                }

                plot.setLineWidth(1);
                if (legendLeft) {
                    plot.setJustification(ij.process.ImageProcessor.LEFT_JUSTIFY);
                } else if (legendRight) { plot.setJustification(ij.process.ImageProcessor.RIGHT_JUSTIFY); } else {
                    plot.setJustification(ij.process.ImageProcessor.CENTER_JUSTIFY);
                }

                // MAKE FULL LEGEND STRING FOR CURVE

                StringBuilder llab;

                if (detrendFitIndex[curve] == 9 && useTransitFit[curve] && showResidual[curve] && showLResidual[curve] && residualShift[curve] > 0.0) {
                    llab = new StringBuilder(ylabel[curve] + " Residuals");
                    llab.append(" (RMS=").append(sigma[curve] >= 1.0 ? uptoThreePlaces.format(sigma[curve]) : uptoFivePlaces.format(sigma[curve])).append(") (chi^2/dof=").append(uptoTwoPlaces.format(chi2dof[curve])).append(")");
                    drawLegendSymbol(residualSymbol[curve], residualSymbol[curve] == ij.gui.Plot.DOT ? 4 : 1, residualColor[curve], legPosY, llab.toString());
                    plot.addLabel(legendPosX, legPosY, llab.toString());
                    legPosY += 18. / plotSizeY;
                }

                llab = new StringBuilder();

                if (useColumnName[curve]) {
                    if (operatorIndex[curve] == 5) {
                        llab = new StringBuilder(xyc1label[curve] + " - " + xyc2label[curve] + " centroid distance");
                        boolean atLeastOne = false;
                        if (showLdetrendInfo && detrendFitIndex[curve] != 0) {
                            for (int v = 0; v < maxDetrendVars; v++) {
                                if (detrendIndex[curve][v] != 0) {
                                    if (atLeastOne) { llab.append("+").append(detrendlabel[curve][v]); } else {
                                        llab.append(" (").append(detrendlabel[curve][v]);
                                    }
                                    atLeastOne = true;
                                }
                            }
                        }
                        if (atLeastOne) {
                            if (detrendFitIndex[curve] == 9 && useTransitFit[curve]) {
                                llab.append(" detrended with transit fit)");
                            } else {
                                llab.append(" detrended)");
                            }
                        } else {
                            if (detrendFitIndex[curve] == 9 && useTransitFit[curve]) {
                                llab.append(" (transit fit)");
                            }
                        }
                        if (showLnormInfo && normIndex[curve] != 0 && !mmag[curve] && !force[curve]) {
                            llab.append(" (normalized)");
                        } else {
                            llab.append(usePixelScale ? " (arcsecs)" : " (pixels)");
                        }
                        if ((detrendFitIndex[curve] > 1 && showSigmaForDetrendedCurves) || showSigmaForAllCurves) {
                            llab.append(" (RMS=").append(sigma[curve] >= 1.0 ? uptoThreePlaces.format(sigma[curve]) : uptoFivePlaces.format(sigma[curve])).append(")");
                        }
                        if (mmag[curve] && showLmmagInfo) {
                            llab.append(" mmag)");
                        } else {
                            llab.append(" ppt)");
                        }
                    } else {
                        llab = new StringBuilder(ylabel[curve]);
                        if (operatorIndex[curve] != 0) {
                            llab.append(opSymbol[operatorIndex[curve]]).append(oplabel[curve]);
                        }
                        boolean atLeastOne = false;
                        if (showLdetrendInfo && detrendFitIndex[curve] != 0) {
                            for (int v = 0; v < maxDetrendVars; v++) {
                                if (detrendIndex[curve][v] != 0) {
                                    if (atLeastOne) { llab.append("+").append(detrendlabel[curve][v]); } else {
                                        llab.append(" (").append(detrendlabel[curve][v]);
                                    }
                                    atLeastOne = true;
                                }
                            }
                        }
                        if (atLeastOne) {
                            if (detrendFitIndex[curve] == 9 && useTransitFit[curve]) {
                                llab.append(" detrended with transit fit)");
                            } else {
                                llab.append(" detrended)");
                            }
                        } else {
                            if (detrendFitIndex[curve] == 9 && useTransitFit[curve]) {
                                llab.append(" (transit fit)");
                            }
                        }
                        if (showLnormInfo && normIndex[curve] != 0 && !mmag[curve] && !force[curve]) {
                            llab.append(" (normalized)");
                        }
                        if (((detrendFitIndex[curve] > 1 && showSigmaForDetrendedCurves) || showSigmaForAllCurves))  //!force[curve] &&
                        {
                            double factor = 1000;
                            if (mmag[curve] && totalScaleFactor[curve] == 1000) {
                                sigma[curve] *= 1000;
                                factor = 1/1000D; // Fix for display of RMS in legend being times 1000
                            }
                            llab.append(" (RMS=").append(sigma[curve] >= 1.0 ? uptoThreePlaces.format(sigma[curve] * factor)
                                    : threeDigitsTwoPlaces.format(sigma[curve] * factor));
                        }
                        if (mmag[curve] && showLmmagInfo) {
                            llab.append(" mmag)");
                        } else {
                            llab.append(" ppt)");
                        }
                    }

                    // Duplicate conditions of transit model fit legend
                    if (detrendFitIndex[curve] == 9 && useTransitFit[curve] && showModel[curve] && showLTranParams[curve]) {
                        llab.append(" (depth=").append(transitDepthLabel[curve].getText());
                        if (mmag[curve]) {
                            llab.append(" mmag)");
                        } else {
                            llab.append(" ppt)");
                        }

                    }
                    if (detrendFitIndex[curve] == 9 && showModel[curve] && showLTranParams[curve]) {
                        llab.append(" (BIC=").append(Double.isNaN(bic[curve]) ? "NaN" : fiveDigitsOnePlace.format(bic[curve])).append(")");
                    }

                    if (!force[curve])//&&(showLScaleInfo || showLShiftInfo))
                    { llab.append(scaleShiftText(force[curve], showLScaleInfo, showLShiftInfo, mmag[curve], showLmmagInfo, totalScaleFactor[curve], totalShiftFactor[curve])); } else if (force[curve])//&&(showLRelScaleInfo || showLRelShiftInfo))
                    { llab.append(scaleShiftText(force[curve], showLRelScaleInfo, showLRelShiftInfo, mmag[curve], showLmmagInfo, totalScaleFactor[curve], totalShiftFactor[curve])); }
                    if ((inputAverageOverSize[curve] != 1) && showLAvgInfo) {
                        llab.append(" (input average=").append(inputAverageOverSize[curve]).append(")");
                    }
                    if (showOutBinRms && binDisplay[curve]) {
                        llab.append(" (RMS=").append(uptoTwoPlaces.format(outBinRms[curve])).append("/").append(uptoTwoPlaces.format(minutes.get(curve).first())).append(" min)");
                    }
                    if (showLSymbolInfo) llab.append(" (").append(markers[markerIndex[curve]]).append(")");
                }

                if (useLegend[curve]) {
                    llab.append(useColumnName[curve] ? " " : "").append(legend[curve]);
                }

                if (useColumnName[curve] || useLegend[curve]) {
                    drawLegendSymbol(marker[curve], (marker[curve] == ij.gui.Plot.DOT) ? 4 : 1, color[curve], legPosY, llab.toString());
                    plot.addLabel(legendPosX, legPosY, llab.toString());
                    legPosY += 18. / plotSizeY;
                }

                if (detrendFitIndex[curve] == 9 && useTransitFit[curve] && showModel[curve] && showLTranParams[curve]) {
                    llab = new StringBuilder(ylabel[curve] + " Transit Model ([P=" + uptoTwoPlaces.format(orbitalPeriod[curve]) + "], " + (lockToCenter[curve][1] ? "[" : "") + "(Rp/R*)^2=");
                    llab.append(fourPlaces.format(bestFit[curve][1] * bestFit[curve][1])).append(lockToCenter[curve][1] ? "]" : "");
                    llab.append(", ").append(lockToCenter[curve][2] ? "[" : "").append("a/R*=").append(onePlaces.format(bestFit[curve][2])).append(lockToCenter[curve][2] ? "]" : "");
                    llab.append(", ").append(lockToCenter[curve][4] ? "[" : "").append("i=").append(onePlaces.format(bestFit[curve][4] * 180 / Math.PI)).append(lockToCenter[curve][4] ? "]" : "");
                    llab.append(", ").append(lockToCenter[curve][3] ? "[" : "").append("Tc=").append(uptoSixPlaces.format(bestFit[curve][3])).append(lockToCenter[curve][3] ? "]" : "");
                    llab.append(", ").append(lockToCenter[curve][5] ? "[" : "").append("u1=").append(uptoTwoPlaces.format(bestFit[curve][5])).append(lockToCenter[curve][5] ? "]" : "");
                    llab.append(", ").append(lockToCenter[curve][6] ? "[" : "").append("u2=").append(uptoTwoPlaces.format(bestFit[curve][6])).append(lockToCenter[curve][6] ? "]" : "").append(")");
                    drawLegendSymbol(ij.gui.Plot.LINE, modelLineWidth[curve] + ((showErrors[curve] || operatorIndex[curve] == 6) && (hasErrors[curve] || hasOpErrors[curve]) ? 1 : 0), modelColor[curve], legPosY, llab.toString());
                    plot.addLabel(legendPosX, legPosY, llab.toString());
                    legPosY += 18. / plotSizeY;
                }

                if (detrendFitIndex[curve] == 9 && useTransitFit[curve] && showResidual[curve] && showLResidual[curve] && residualShift[curve] <= 0.0) {
                    llab = new StringBuilder(ylabel[curve] + " Residuals");
                    llab.append(" (RMS=").append(sigma[curve] >= 1.0 ? uptoThreePlaces.format(sigma[curve]) : uptoFivePlaces.format(sigma[curve])).append(") (chi^2/dof=").append(uptoTwoPlaces.format(chi2dof[curve])).append(")");
                    drawLegendSymbol(residualSymbol[curve], residualSymbol[curve] == ij.gui.Plot.DOT ? 4 : 1, residualColor[curve], legPosY, llab.toString());
                    plot.addLabel(legendPosX, legPosY, llab.toString());
                    legPosY += 18. / plotSizeY;
                }

            }
        }

        if (useBoldedDatum && boldedDatum != -1) {
            double[] xx = new double[1];
            double[] yy = new double[1];
            for (int curve = 0; curve < maxCurves; curve++) {
                if (plotY[curve]) {
                    plot.setColor(color[curve]);
                    if (marker[curve] == ij.gui.Plot.DOT) { plot.setLineWidth(8); } else plot.setLineWidth(2);
                    if (curve == firstCurve) {
                        xx[0] = x[curve][boldedDatum];
                        yy[0] = y[curve][boldedDatum];
                    } else {
                        xx[0] = x[curve][boldedDatum * inputAverageOverSize[firstCurve] / (inputAverageOverSize[curve])];
                        yy[0] = y[curve][boldedDatum * inputAverageOverSize[firstCurve] / (inputAverageOverSize[curve])];
                    }
                    plot.addPoints(xx, yy, marker[curve]);
                    if (showResidual[curve] && plottedResidual[curve] != null && useTransitFit[curve] && detrendFitIndex[curve] == 9) {
                        plot.setColor(residualColor[curve]);
                        if (residualSymbol[curve] == ij.gui.Plot.DOT) { plot.setLineWidth(8); } else {
                            plot.setLineWidth(2);
                        }
                        if (curve == firstCurve) {
                            int nnn = boldedDatum - nFitTrim[curve];
                            if (nnn >= 0 && nnn < plottedResidual[curve].length) {
                                xx[0] = xModel1[curve][boldedDatum - nFitTrim[curve]];
                                yy[0] = plottedResidual[curve][boldedDatum - nFitTrim[curve]];
                                plot.addPoints(xx, yy, residualSymbol[curve]);
                            }
                        } else {
                            int nnn = boldedDatum * inputAverageOverSize[firstCurve] / inputAverageOverSize[curve] - nFitTrim[curve];
                            if (nnn >= 0 && nnn < plottedResidual[curve].length) {
                                xx[0] = xModel1[curve][nnn];
                                yy[0] = plottedResidual[curve][nnn];
                                plot.addPoints(xx, yy, residualSymbol[curve]);
                            }
                        }
                    }
                }
            }
        }

        plot.setLineWidth(1);
        plot.setJustification(Plot.CENTER);

        if (showDMarkers) {
            plot.setColor(Color.GRAY);
            double[] samples1 = new double[10];
            double[] samples2 = new double[10];
            double[] samples3 = new double[10];
            double[] samples4 = new double[10];
            for (int i = 0; i < 10; i++) {
                samples1[i] = Double.NaN;
                samples2[i] = Double.NaN;
                samples3[i] = Double.NaN;
                samples4[i] = Double.NaN;
            }

            double preDmark1Ref = invertYAxis ? yPlotMin : yPlotMax;
            double preDmark2Ref = preDmark1Ref;
            double postDMarker3Ref = preDmark1Ref;
            double postDMarker4Ref = preDmark1Ref;
            int nBefore2 = 0;
            int nAfter2 = 0;
            int nBefore1 = 0;
            int nAfter1 = 0;
            int nBefore3 = 0;
            int nAfter3 = 0;
            int nBefore4 = 0;
            int nAfter4 = 0;
            for (int i = 0; i < nn[firstCurve]; i++) {
                if (!Double.isNaN(x[firstCurve][i]) && !Double.isNaN(y[firstCurve][i])) {
                    if (x[firstCurve][i] < dMarker2Value) {
                        samples2[4] = samples2[3];
                        samples2[3] = samples2[2];
                        samples2[2] = samples2[1];
                        samples2[1] = samples2[0];
                        samples2[0] = y[firstCurve][i];
                        nBefore2++;
                    } else if (nAfter2 < 5) {
                        samples2[5 + nAfter2] = y[firstCurve][i];
                        nAfter2++;
                    }

                    if (useDMarker1 && x[firstCurve][i] < dMarker1Value) {
                        samples1[4] = samples1[3];
                        samples1[3] = samples1[2];
                        samples1[2] = samples1[1];
                        samples1[1] = samples1[0];
                        samples1[0] = y[firstCurve][i];
                        nBefore1++;
                    } else if (useDMarker1 && nAfter1 < 5) {
                        samples1[5 + nAfter1] = y[firstCurve][i];
                        nAfter1++;
                    }

                    if (x[firstCurve][i] < dMarker3Value) {
                        samples3[4] = samples3[3];
                        samples3[3] = samples3[2];
                        samples3[2] = samples3[1];
                        samples3[1] = samples3[0];
                        samples3[0] = y[firstCurve][i];
                        nBefore3++;
                    } else if (nAfter3 < 5) {
                        samples3[5 + nAfter3] = y[firstCurve][i];
                        nAfter3++;
                    }

                    if (useDMarker4 && x[firstCurve][i] < dMarker4Value) {
                        samples4[4] = samples4[3];
                        samples4[3] = samples4[2];
                        samples4[2] = samples4[1];
                        samples4[1] = samples4[0];
                        samples4[0] = y[firstCurve][i];
                        nBefore4++;
                    } else if (useDMarker4 && nAfter4 < 5) {
                        samples4[5 + nAfter4] = y[firstCurve][i];
                        nAfter4++;
                    }
                }
            }
            if (nBefore2 + nAfter2 > 0) {
                preDmark2Ref = invertYAxis ? minOf(samples2, 10) : maxOf(samples2, 10);
                preDmark2Ref = invertYAxis ? Math.max(preDmark2Ref, plotMaxY) : Math.min(preDmark2Ref, plotMaxY);
            }
            if (nBefore3 + nAfter3 > 0) {
                postDMarker3Ref = invertYAxis ? minOf(samples3, 10) : maxOf(samples3, 10);
                postDMarker3Ref = invertYAxis ? Math.max(postDMarker3Ref, plotMaxY) : Math.min(postDMarker3Ref, plotMaxY);
            }
            dashLength = 5 * (plotMaxY - plotMinY) / plotSizeY;

            numDashes = -10 + (preDmark2Ref - plotMinY) / dashLength;        //plot dMarker2
            for (int dashCount = 0; dashCount < numDashes; dashCount += 2) {
                plot.drawLine(dMarker2Value, preDmark2Ref - dashLength * dashCount, dMarker2Value, preDmark2Ref - dashLength * (dashCount + 1));
            }
            plot.setJustification(Plot.CENTER);
            plot.addLabel((dMarker2Value - plotMinX) / (plotMaxX - plotMinX), 1 - 16.0 / plotSizeY, "Left");
            plot.addLabel((dMarker2Value - plotMinX) / (plotMaxX - plotMinX), 1 + 4.0 / plotSizeY, threePlaces.format(dMarker2Value));

            numDashes = -10 + (postDMarker3Ref - plotMinY) / dashLength;     //plot dMarker3
            for (int dashCount = 0; dashCount < numDashes; dashCount += 2) {
                plot.drawLine(dMarker3Value, postDMarker3Ref - dashLength * dashCount, dMarker3Value, postDMarker3Ref - dashLength * (dashCount + 1));
            }
            plot.addLabel((dMarker3Value - plotMinX) / (plotMaxX - plotMinX), 1 - 16.0 / plotSizeY, "Right");
            plot.addLabel((dMarker3Value - plotMinX) / (plotMaxX - plotMinX), 1 + 4.0 / plotSizeY, threePlaces.format(dMarker3Value));

            if (useDMarker1)   //plot dMarker1
            {
                if (nBefore1 + nAfter1 > 0) {
                    preDmark1Ref = invertYAxis ? minOf(samples1, 10) : maxOf(samples1, 10);
                    preDmark1Ref = invertYAxis ? Math.max(preDmark1Ref, plotMaxY) : Math.min(preDmark1Ref, plotMaxY);
                }
                numDashes = -10 + (preDmark1Ref - plotMinY) / dashLength;        //plot dMarker1
                for (int dashCount = 0; dashCount < numDashes; dashCount += 2) {
                    plot.drawLine(dMarker1Value, preDmark1Ref - dashLength * dashCount, dMarker1Value, preDmark1Ref - dashLength * (dashCount + 1));
                }
                plot.addLabel((dMarker1Value - plotMinX) / (plotMaxX - plotMinX), 1 - 25.0 / plotSizeY, "Left");
                plot.addLabel((dMarker1Value - plotMinX) / (plotMaxX - plotMinX), 1 - 7.0 / plotSizeY, "Trim");
                plot.addLabel((dMarker1Value - plotMinX) / (plotMaxX - plotMinX), 1 + 33.0 / plotSizeY, threePlaces.format(dMarker1Value));
            }
            if (useDMarker4)   //plot dMarker4
            {
                if (nBefore4 + nAfter4 > 0) {
                    postDMarker4Ref = invertYAxis ? minOf(samples4, 10) : maxOf(samples4, 10);
                    postDMarker4Ref = invertYAxis ? Math.max(postDMarker4Ref, plotMaxY) : Math.min(postDMarker4Ref, plotMaxY);
                }
                numDashes = -10 + (postDMarker4Ref - plotMinY) / dashLength;     //plot dMarker4
                for (int dashCount = 0; dashCount < numDashes; dashCount += 2) {
                    plot.drawLine(dMarker4Value, postDMarker4Ref - dashLength * dashCount, dMarker4Value, postDMarker4Ref - dashLength * (dashCount + 1));
                }
                plot.addLabel((dMarker4Value - plotMinX) / (plotMaxX - plotMinX), 1 - 25.0 / plotSizeY, "Right");
                plot.addLabel((dMarker4Value - plotMinX) / (plotMaxX - plotMinX), 1 - 7.0 / plotSizeY, "Trim");
                plot.addLabel((dMarker4Value - plotMinX) / (plotMaxX - plotMinX), 1 + 33.0 / plotSizeY, threePlaces.format(dMarker4Value));
            }
        }
        if (showMFMarkers) drawVMarker(mfMarker1Value, "Meridian", "Flip", new Color(84, 201, 245));

        if (showVMarker1) drawVMarker(vMarker1Value, vMarker1TopText, vMarker1BotText, Color.red);

        if (showVMarker2) drawVMarker(vMarker2Value, vMarker2TopText, vMarker2BotText, Color.red);

        plot.setColor(java.awt.Color.black);
        plot.setJustification(Plot.CENTER);
        if (useTitle) {
            plot.changeFont(new java.awt.Font("Dialog", java.awt.Font.PLAIN, 18));
            renderTitle();
        }
        if (useSubtitle) {
            plot.changeFont(new java.awt.Font("Dialog", java.awt.Font.PLAIN, 14));
            renderSubtitle();
        }

        plotImage = WindowManager.getImage("Plot of " + tableName);

        if (plotImage == null) {
            plotFrameLocationX = (int) Prefs.get("plot2.plotFrameLocationX", plotFrameLocationX);
            plotFrameLocationY = (int) Prefs.get("plot2.plotFrameLocationY", plotFrameLocationY);
            if (!Prefs.isLocationOnScreen(new Point(plotFrameLocationX, plotFrameLocationY))) {
                plotFrameLocationX = 10;
                plotFrameLocationY = 10;
                Prefs.set("plot2.plotFrameLocationX", plotFrameLocationX);
                Prefs.set("plot2.plotFrameLocationY", plotFrameLocationY);
            }
            plotWindow = plot.show();
            plotWindow.setIconImage(plotIcon.getImage());
            plotImage = plotWindow.getImagePlus();//WindowManager.getImage("Plot of "+tableName);

//                    plotWindow = plotImage.getWindow();
//                    plotWindow = new PlotWindow(plot);
//                    plotImage = plotWindow.getImagePlus();
//                    plotFrame = WindowManager.getFrame("Plot of "+tableName);
//                    if ((plotImage != null) && rememberWindowLocations)
//                        {
//                        plotFrameLocationX=(int)Prefs.get("plot2.plotFrameLocationX", plotFrameLocationX);
//                        plotFrameLocationY=(int)Prefs.get("plot2.plotFrameLocationY", plotFrameLocationY);
//                        if (!Prefs.isLocationOnScreen(new Point(plotFrameLocationX,plotFrameLocationY)))
//                            {
//                            plotFrameLocationX = 10;
//                            plotFrameLocationY = 10;
//                            }
//                        plotWindow.setLocation(plotFrameLocationX, plotFrameLocationY);
//                        }

//                    plotImage = WindowManager.getImage("Plot of "+tableName);
//                    plotFrame = WindowManager.getFrame("Plot of "+tableName);
//                    ImageProcessor ip = plot.getProcessor();
//                    plotImage.setProcessor("Plot of "+tableName,ip);

            plotImageCanvas = plotImage.getCanvas();
            plotOverlayCanvas = new OverlayCanvas(plotImage);
            list.clear();

            MouseMotionListener[] mml = plotImageCanvas.getMouseMotionListeners();
            if (mml.length > 0) {
                for (MouseMotionListener mouseMotionListener : mml)
                    plotImageCanvas.removeMouseMotionListener(mouseMotionListener);
            }

            MouseWheelListener[] mwl = plotImageCanvas.getMouseWheelListeners();
            if (mwl.length > 0) {
                for (MouseWheelListener mouseWheelListener : mwl)
                    plotImageCanvas.removeMouseWheelListener(mouseWheelListener);
            }

            MouseWheelListener[] mwl3 = plotWindow.getMouseWheelListeners();
            if (mwl3.length > 0) {
                for (MouseWheelListener mouseWheelListener : mwl3)
                    plotWindow.removeMouseWheelListener(mouseWheelListener);
            }

            MouseListener[] ml = plotImageCanvas.getMouseListeners();
            if (ml.length > 0) {
                for (MouseListener mouseListener : ml) plotImageCanvas.removeMouseListener(mouseListener);
            }

            plotImageCanvas.addMouseWheelListener(plotMouseWheelListener);
            plotImageCanvas.addMouseMotionListener(plotMouseMotionListener);
            plotImageCanvas.addMouseListener(plotMouseListener);
            plotImageCanvas.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
            zoom = 0.0;
        } else {
            plotWindow = plotImage.getWindow();
//                    plotFrame = (Frame)plotWindow;
            ImageProcessor ip = plot.getProcessor();
            plotImage.setProcessor("Plot of " + tableName, ip);
            plotImageCanvas = plotImage.getCanvas();
            plotImageCanvas.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
        }

        plot.changeFont(new java.awt.Font("Dialog", java.awt.Font.PLAIN, 12));
        plot.setJustification(Plot.BOTTOM_RIGHT);
        var wid = plotImage.getImage().getGraphics().getFontMetrics(plot.getCurrentFont()).stringWidth("AIJ " + IJ.getAstroVersion().split("[+]")[0]);
        var pWid = plot.getSize().getWidth();
        var h = plot.getSize().getHeight();
        plot.addLabel((pWid - wid - 10)/pWid, (h + 43)/h, "AIJ " + IJ.getAstroVersion().split("[+]")[0]);

        updatePlotPos();

        plotbottompanel = (Panel) plotWindow.getComponent(1);
        plotbottompanel.getComponentCount();
        plotbottompanel.setSize(600, 30);
        plotcoordlabel = (Label) plotbottompanel.getComponent(plotbottompanel.getComponentCount() - 1);
        plotcoordlabel.setSize(400, 20);
        plotcoordlabel.setBackground(Color.white);

        //Replot to clean up blank areas

//                plotImage = WindowManager.getImage("Plot of "+tableName);
//                ImageProcessor ip = plot.getProcessor();
//                plotImage.setProcessor("Plot of "+tableName,ip);

        excludedHeadSamples = holdExcludedHeadSamples;
        excludedTailSamples = holdExcludedTailSamples;
//                for (int i=0; i<maxCurves; i++)
//                    {
//                    showErr[i] = showErrors[i];
//                    showErrors[i] = errorcolumnbox[i].isSelected();
//                    }
        table.setLock(false);
        plotWindow.getImagePlus().setPlot(plot);
        ((PlotWindow) plotWindow).setPlot(plot);
        updatePlotRunning = false;
    }

    static void updatePlotPos() {
        IJU.setFrameSizeAndLocation(plot.getImagePlus().getWindow(), plotFrameLocationX, plotFrameLocationY,
                plotSizeX, plotSizeY, false);
        plotImage.updateAndRepaintWindow();
    }

    static void drawVMarker(double vMarkerValue, String vMarkerTopText, String vMarkerBotText, Color color) {
        plot.setColor(color);
        double[] samples = new double[10];
        for (int i = 0; i < 10; i++) {
            samples[i] = Double.NaN;
        }
        double preVmarkRef = invertYAxis ? yPlotMin : yPlotMax;
        int nBefore = 0;
        int nAfter = 0;
        for (int i = 0; i < nn[firstCurve]; i++) {
            if (!Double.isNaN(x[firstCurve][i]) && !Double.isNaN(y[firstCurve][i])) {
                if (x[firstCurve][i] < vMarkerValue) {
                    samples[4] = samples[3];
                    samples[3] = samples[2];
                    samples[2] = samples[1];
                    samples[1] = samples[0];
                    samples[0] = y[firstCurve][i];
                    nBefore++;
                } else if (nAfter < 5) {
                    samples[5 + nAfter] = y[firstCurve][i];
                    nAfter++;
                } else {
                    break;
                }
            }
        }
        if (nBefore + nAfter > 0) {
            preVmarkRef = invertYAxis ? minOf(samples, 10) : maxOf(samples, 10);
            preVmarkRef = invertYAxis ? Math.max(preVmarkRef, plotMaxY) : Math.min(preVmarkRef, plotMaxY);
        }
        dashLength = 5 * (plotMaxY - plotMinY) / plotSizeY;
        numDashes = -10 + (preVmarkRef - plotMinY) / dashLength;
        for (int dashCount = 0; dashCount < numDashes; dashCount += 2)    //plot vMarker1
        {
            plot.drawLine(vMarkerValue, preVmarkRef - dashLength * dashCount, vMarkerValue, preVmarkRef - dashLength * (dashCount + 1));
        }
        plot.setJustification(Plot.CENTER);
        plot.addLabel((vMarkerValue - plotMinX) / (plotMaxX - plotMinX), 1 - 25.0 / plotSizeY, vMarkerTopText);
        plot.addLabel((vMarkerValue - plotMinX) / (plotMaxX - plotMinX), 1 - 7.0 / plotSizeY, vMarkerBotText);
        plot.addLabel((vMarkerValue - plotMinX) / (plotMaxX - plotMinX), 1 + 33.0 / plotSizeY, threePlaces.format(vMarkerValue));
    }

    static void drawLegendSymbol(int marker, int width, Color color, double legPosY, String llab) {
        double xShift = 6.0;
        double yShift = 6.0;
        Font font = new Font("Dialog", Font.PLAIN, 12);
        int h = 0;
        int w = 0;
        if (mainpanel != null) {
            Graphics g = mainpanel.getGraphics();
            FontMetrics metrics = g.getFontMetrics(font);
            h = metrics.getHeight();
            w = (int) (metrics.stringWidth(llab) * 1.1) + 18;
        }
        plot.setLineWidth(width);
        if (legendRight) { xShift += w; } else if (!legendLeft) xShift += w / 2f;
        plot.setColor(color);
        if (marker == ij.gui.Plot.DOT) {
            xShift += 2;
            yShift += 2;
        } else if (marker == ij.gui.Plot.LINE) {
            xShift += 7;
            yShift += 2;
        } else {
            xShift += 3;
            yShift += 3;
        }
        Dimension s = plot.getSize();
        double x = plotMinX + (plotMaxX - plotMinX) * (legendPosX - xShift / (s.getWidth()));
        double y = plotMinY + (plotMaxY - plotMinY) * (1 - legPosY + yShift / (s.getHeight()));
        double[] xx = {x};
        double[] yy = {y};
        if (marker != ij.gui.Plot.LINE) {
            plot.addPoints(xx, yy, marker);
        } else {
            plot.drawLine(x, y, x + (plotMaxX - plotMinX) * 6.0 / (s.getWidth()), y);
        }
        plot.setLineWidth(1);
    }

    public static boolean[] updateAllFits() {
        boolean[] updateFits = new boolean[maxCurves];
        for (int curve = 0; curve < maxCurves; curve++) {
            updateFits[curve] = true;
        }
        return updateFits;
    }

    public static boolean[] updateNoFits() {
        return new boolean[maxCurves];
    }

    public static boolean[] updateOneFit(int curve) {
        boolean[] updateFits = new boolean[maxCurves];
        for (int i = 0; i < maxCurves; i++) {
            if (i == curve) {
                updateFits[curve] = true;
                break;
            }
        }
        return updateFits;
    }

    public static class FitDetrendOnly implements MinimizationFunction {
        public double function(double[] param) {
            double sd = 0.0;
            double residual;
            int numData = detrendYs[curve].length;
            int numVars = detrendVars.length;
            for (int j = 0; j < numData; j++) {
                residual = detrendYs[curve][j] - param[0];
                for (int i = 0; i < numVars; i++) {
                    residual -= detrendVars[i][j] * param[i + 1];
                }
                sd += residual * residual;
            }
            return Math.sqrt(sd / (double) numData);
        }
    }

    public static class FitDetrendChi2 implements MinimizationFunction {
        public double function(double[] param) {
            double chi2 = 0.0;
            double residual;
            int numData = detrendYs[curve].length;
            int numVars = detrendVars.length;
            int dof = numData - param.length;
            if (dof < 1) dof = 1;
            for (int j = 0; j < numData; j++) {
                residual = detrendYs[curve][j];// - param[0];
                for (int i = 0; i < numVars; i++) {
                    residual -= detrendVars[i][j] * param[i];
                }
                chi2 += ((residual * residual) / (detrendYEs[curve][j] * detrendYEs[curve][j]));
            }
            return chi2 / (double) dof;
        }
    }

    static void renderSubtitle() {
        Dimension size = plot.getSize();
        double y = subtitlePosY / size.getHeight() > 1 ? 1 : (subtitlePosY - 38) / size.getHeight();
        plot.addLabel(subtitlePosX, y, subtitle);
    }

    static void renderTitle() {
        Dimension size = plot.getSize();
        double y = titlePosY / size.getHeight() > 1 ? 1 : (titlePosY - 35) / size.getHeight();
        plot.addLabel(titlePosX, y, title);
    }

    public static class FitLightCurveChi2 implements MinimizationFunction {
        public double function(double[] param) {
            int numData = detrendYs[curve].length;
            int numDetrendVars = detrendVars.length;
            int nPars = param.length;
            double[] dPars = new double[detrendVars.length];
//            int dof = numData - param.length;// - 7;
            if (dof[curve] < 1) dof[curve] = 1;

            chi2[curve] = 0;
            double residual;
            int fp = 0;

            double f0 = priorCenter[curve][0]; // baseline flux
            double p0 = priorCenter[curve][1]; // r_p/r_*
            double ar = priorCenter[curve][2]; // a/r_*
            double tc = priorCenter[curve][3]; //transit center time
            double incl = priorCenter[curve][4];  //inclination
            double u1 = priorCenter[curve][5];  //quadratic limb darkening parameter 1
            double u2 = priorCenter[curve][6];  //quadratic limb darkening parameter 2
            double e = forceCircularOrbit[curve] ? 0.0 : eccentricity[curve];
            double ohm = forceCircularOrbit[curve] ? 0.0 : omega[curve];
            double b = 0.0;
            if (useTransitFit[curve]) {
                f0 = lockToCenter[curve][0] ? priorCenter[curve][0] : param[fp < nPars ? fp++ : nPars - 1]; // baseline flux
                p0 = lockToCenter[curve][1] ? Math.sqrt(priorCenter[curve][1]) : param[fp < nPars ? fp++ : nPars - 1]; // r_p/r_*
                ar = lockToCenter[curve][2] ? priorCenter[curve][2] : param[fp < nPars ? fp++ : nPars - 1]; // a/r_*
                tc = lockToCenter[curve][3] ? priorCenter[curve][3] : param[fp < nPars ? fp++ : nPars - 1]; //transit center time
                if (!bpLock[curve]) {
                    incl = lockToCenter[curve][4] ? priorCenter[curve][4] * Math.PI / 180.0 : param[fp < nPars ? fp++ : nPars - 1];  //inclination
                    b = Math.cos(incl) * ar;
                    if (b > 1.0 + p0) {  //ensure planet transits or grazes the star
                        incl = Math.acos((1.0 + p0) / ar);
                    }
                } else {
                    incl = Math.acos(bp[curve]/ar);
                }
                u1 = lockToCenter[curve][5] ? priorCenter[curve][5] : param[fp < nPars ? fp++ : nPars - 1];  //quadratic limb darkening parameter 1
                u2 = lockToCenter[curve][6] ? priorCenter[curve][6] : param[fp < nPars ? fp++ : nPars - 1];  //quadratic limb darkening parameter 2

                lcModel[curve] = IJU.transitModel(detrendXs[curve], f0, incl, p0, ar, tc, orbitalPeriod[curve], e, ohm, u1, u2, useLonAscNode[curve], lonAscNode[curve], true);
            }

            int dp = 0;
            for (int p = 7; p < maxFittedVars; p++) {
                if (isFitted[curve][p]) {
                    dPars[dp++] = param[fp++];
                } else if (detrendIndex[curve][p - 7] != 0 && detrendYDNotConstant[p - 7] && lockToCenter[curve][p]) {
                    dPars[dp++] = priorCenter[curve][p];
                }
            }


            if (useTransitFit[curve]) {
                if (!lockToCenter[curve][2] && (ar < (1.0 + p0))) {
                    chi2[curve] = Double.POSITIVE_INFINITY;  //boundary check that planet does not orbit within star
                } else if ((!lockToCenter[curve][2] || !lockToCenter[curve][4]) && ((ar * Math.cos(incl) * (1.0 - e * e) / (1.0 + e * Math.sin(ohm * Math.PI / 180.0))) >= 1.0 + p0)) {
                    if (!lockToCenter[curve][4] && autoUpdatePrior[curve][4]) {
                        priorCenter[curve][4] = Math.round(10.0 * Math.acos((0.5 + p0) * (1.0 + e * Math.sin(ohm * Math.PI / 180.0)) / (ar * (1.0 - e * e))) * 180.0 / Math.PI) / 10.0;
                        if (Double.isNaN(priorCenter[curve][4])) priorCenter[curve][4] = 89.9;
                        priorCenterSpinner[curve][4].setValue(priorCenter[curve][4]);
                    }
                    chi2[curve] = Double.POSITIVE_INFINITY; //boundary check that planet passes in front of star
                } else if ((!lockToCenter[curve][5] || !lockToCenter[curve][6]) && (((u1 + u2) > 1.0) || ((u1 + u2) < 0.0) || (u1 > 1.0) || (u1 < 0.0) || (u2 < -1.0) || (u2 > 1.0))) {
                    chi2[curve] = Double.POSITIVE_INFINITY;
                } else {
                    for (int j = 0; j < numData; j++) {
                        residual = detrendYs[curve][j];// - param[0];
                        for (int i = 0; i < numDetrendVars; i++) {
                            residual -= detrendVars[i][j] * dPars[i];
                        }
                        residual -= (lcModel[curve][j] - detrendYAverage[curve]);
                        chi2[curve] += ((residual * residual) / (detrendYEs[curve][j] * detrendYEs[curve][j]));
                    }
                }
            } else {
                for (int j = 0; j < numData; j++) {
                    residual = detrendYs[curve][j];// - param[0];
                    for (int i = 0; i < numDetrendVars; i++) {
                        residual -= detrendVars[i][j] * dPars[i];
                    }
                    chi2[curve] += ((residual * residual) / (detrendYEs[curve][j] * detrendYEs[curve][j]));
                }
            }
            return chi2[curve] / (double) dof[curve];
        }
    }

    static String getPositionColumn(String label, String XorY) {
        String nums = "";
        for (int i = 0; i < label.length(); i++) {
            if (label.charAt(i) >= '0' && label.charAt(i) <= '9') {
                nums = nums.concat(label.substring(i, i + 1));
            }
        }
        int ap = parseInteger(nums, 1);
        if (ap < 1 || ap > isRefStar.length) ap = 1;
        if (table.getColumnIndex(XorY + "(IJ)_" + (isRefStar[ap - 1] ? "C" : "T") + ap) == ResultsTable.COLUMN_NOT_FOUND) {
            if (table.getColumnIndex(XorY + "(FITS)_" + (isRefStar[ap - 1] ? "C" : "T") + ap) == ResultsTable.COLUMN_NOT_FOUND) {
                return "no_position_data_found";
            } else {
                return XorY + "(FITS)_" + (isRefStar[ap - 1] ? "C" : "T") + ap;
            }
        }
        return XorY + "(IJ)_" + (isRefStar[ap - 1] ? "C" : "T") + ap;
    }

    static String getSourceID(String label) {
        String nums = "";
        for (int i = 0; i < label.length(); i++) {
            if (label.charAt(i) >= '0' && label.charAt(i) <= '9') {
                nums = nums.concat(label.substring(i, i + 1));
            }
        }
        int ap = parseInteger(nums, 1);
        if (ap < 1 || ap > isRefStar.length) ap = 1;
        return (isRefStar[ap - 1] ? "C" : "T") + ap;
    }

    static int parseInteger(String s, int defaultValue) {
        if (s == null) return defaultValue;
        try {
            defaultValue = Integer.parseInt(s);
        } catch (NumberFormatException ignored) {}
        return defaultValue;
    }

    static void addNewAstroData() {
        astroConverterUpdating = false;
        acc = new AstroConverter(true, true, "MP Coordinate Converter");
        acc.setEnableTimeEntry(false);
        acc.getTAIminusUTC();
        acc.setEnableObjectEntry(true);
        acc.setEnableObservatoryEntry(true);
        acc.showPanel(true);

        addAstroDataFrame = new JFrame("Add astronomical data to table");
        addAstroDataFrame.setIconImage(plotIcon.getImage());
        JPanel addAstroDataPanel = new JPanel(new SpringLayout());
        JScrollPane addAstroDataScrollPane = new JScrollPane(addAstroDataPanel);
        addAstroDataFrame.setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        addAstroDataFrame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                addAstroDataFrameWasShowing = false;
                getAstroPanelValues();
                saveAstroPanelPrefs();
                if (acc != null) acc.saveAndClose();
                closeAddAstroDataFrame();
            }
        });

        useGJDButton = new JRadioButton("JD (UTC)");
        useGJDButton.setToolTipText("Use geocentric JD column as time base");
        useHJDButton = new JRadioButton("HJD (UTC)");
        useHJDButton.setToolTipText("Use heliocentric JD column as time base");
        useBJDButton = new JRadioButton("BJD (TDB)");
        useBJDButton.setToolTipText("Use barycentric JD column as time base");

        if (useGJD) {
            useGJDButton.setSelected(true);
            useHJDButton.setSelected(false);
            useBJDButton.setSelected(false);
        } else if (useHJD) {
            useGJDButton.setSelected(false);
            useHJDButton.setSelected(true);
            useBJDButton.setSelected(false);
        } else //useBJD
        {
            useGJDButton.setSelected(false);
            useHJDButton.setSelected(false);
            useBJDButton.setSelected(true);
        }
        useGJD = useGJDButton.isSelected();
        useHJD = useHJDButton.isSelected();
        useBJD = useBJDButton.isSelected();

        JDRadioGroup = new ButtonGroup();
        JDRadioGroup.add(useGJDButton);
        JDRadioGroup.add(useHJDButton);
        JDRadioGroup.add(useBJDButton);

        useGJDButton.addActionListener(ae -> {
            useGJD = true;
            useHJD = false;
            useBJD = false;
            jdcolumnbox.setSelectedIndex(jdcolumnbox.getSelectedIndex());
        });
        useHJDButton.addActionListener(ae -> {
            useGJD = false;
            useHJD = true;
            useBJD = false;
            jdcolumnbox.setSelectedIndex(jdcolumnbox.getSelectedIndex());
        });
        useBJDButton.addActionListener(ae -> {
            useGJD = false;
            useHJD = false;
            useBJD = true;
            jdcolumnbox.setSelectedIndex(jdcolumnbox.getSelectedIndex());
        });

        JPanel jdselectionpanel = new JPanel(new SpringLayout());
        jdselectionpanel.setBorder(BorderFactory.createTitledBorder("Input Time Format"));
        jdselectionpanel.add(useGJDButton);
        jdselectionpanel.add(useHJDButton);
        jdselectionpanel.add(useBJDButton);
        SpringUtil.makeCompactGrid(jdselectionpanel, 1, jdselectionpanel.getComponentCount(), 2, 0, 2, 0);
        addAstroDataPanel.add(jdselectionpanel);

        JPanel jdcolumnpanel = new JPanel(new SpringLayout());
        jdcolumnpanel.setBorder(BorderFactory.createTitledBorder("Date/Time Column From Active Table"));
        String[] tabCols = columns;
        if (columns.length > 1) tabCols = Arrays.copyOfRange(tabCols, 1, tabCols.length - 1);
        MutableComboBoxModel<String> jddefaultmodel = new DefaultComboBoxModel<>(tabCols);
        jdcolumnbox = new JComboBox<>(jddefaultmodel);
        jdcolumnbox.setSelectedItem(JDColumn);
        jdcolumnbox.setPrototypeDisplayValue("123456789012345");
        jdcolumnbox.addActionListener(ae -> {
            if (!astroConverterUpdating) updateMPCC(-1);
        });

        jdcolumnpanel.add(jdcolumnbox);
        jdcolumnpanel.setPreferredSize(new Dimension(125, 25));
        SpringUtil.makeCompactGrid(jdcolumnpanel, 1, 1, 0, 0, 0, 0);
        addAstroDataPanel.add(jdcolumnpanel);


        useManualRaDecButton = new JRadioButton("Manual");
        useManualRaDecButton.setToolTipText("Manually enter RA and Dec in 'MP Coordinate Converter' panel");
        useTableRaDecButton = new JRadioButton("Table");
        useTableRaDecButton.setToolTipText("Use J2000 RA and Dec values from specified table columns");

        if (useTableRaDec) {
            useTableRaDecButton.setSelected(true);
            useManualRaDecButton.setSelected(false);
            acc.setEnableObjectEntry(false);
        } else //use manual RA/Dec entry
        {
            useTableRaDecButton.setSelected(false);
            useManualRaDecButton.setSelected(true);
            acc.setEnableObjectEntry(true);
        }

        RaDecRadioGroup = new ButtonGroup();
        RaDecRadioGroup.add(useManualRaDecButton);
        RaDecRadioGroup.add(useTableRaDecButton);

        useManualRaDecButton.addActionListener(ae -> {
            useTableRaDec = false;
            racolumnbox.setEnabled(false);
            deccolumnbox.setEnabled(false);
            acc.setEnableObjectEntry(true);
            if (!astroConverterUpdating) updateMPCC(-1);
        });
        useTableRaDecButton.addActionListener(ae -> {
            useTableRaDec = true;
            racolumnbox.setEnabled(true);
            deccolumnbox.setEnabled(true);
            acc.setEnableObjectEntry(false);
            if (!astroConverterUpdating) updateMPCC(-1);
        });

        JPanel radecselectionpanel = new JPanel(new SpringLayout());
        radecselectionpanel.setBorder(BorderFactory.createTitledBorder("RA/Dec Source (J2000)"));
        radecselectionpanel.add(useManualRaDecButton);
        radecselectionpanel.add(useTableRaDecButton);
        SpringUtil.makeCompactGrid(radecselectionpanel, 1, radecselectionpanel.getComponentCount(), 2, 0, 2, 0);
        addAstroDataPanel.add(radecselectionpanel);

        JPanel radeccolumnpanel = new JPanel(new SpringLayout());

        JPanel racolumnpanel = new JPanel(new SpringLayout());
        racolumnpanel.setBorder(BorderFactory.createTitledBorder("RA Column (hrs)"));
        MutableComboBoxModel<String> radefaultmodel = new DefaultComboBoxModel<>(columns);
        racolumnbox = new JComboBox<>(radefaultmodel);
        racolumnbox.setSelectedItem(raColumn);
        racolumnbox.setEnabled(useTableRaDec);
        racolumnbox.setToolTipText("Table column containing target J2000 RA coordinate (in hours)");
        racolumnbox.setPrototypeDisplayValue("123456789012345");
        racolumnbox.addActionListener(ae -> {
            if (!astroConverterUpdating) updateMPCC(-1);
        });

        racolumnpanel.add(racolumnbox);
        racolumnpanel.setPreferredSize(new Dimension(125, 25));
        SpringUtil.makeCompactGrid(racolumnpanel, 1, 1, 0, 0, 0, 0);
        radeccolumnpanel.add(racolumnpanel);


        JPanel deccolumnpanel = new JPanel(new SpringLayout());
        deccolumnpanel.setBorder(BorderFactory.createTitledBorder("DEC Column (deg)"));
        MutableComboBoxModel<String> decdefaultmodel = new DefaultComboBoxModel<>(columns);
        deccolumnbox = new JComboBox<>(decdefaultmodel);
        deccolumnbox.setSelectedItem(decColumn);
        deccolumnbox.setEnabled(useTableRaDec);
        deccolumnbox.setToolTipText("Table column containing target J2000 DEC coordinate (in degrees)");
        deccolumnbox.setPrototypeDisplayValue("123456789012345");
        deccolumnbox.addActionListener(ae -> {
            if (!astroConverterUpdating) updateMPCC(-1);
        });

        deccolumnpanel.add(deccolumnbox);
        deccolumnpanel.setPreferredSize(new Dimension(125, 25));
        SpringUtil.makeCompactGrid(deccolumnpanel, 1, 1, 0, 0, 0, 0);
        radeccolumnpanel.add(deccolumnpanel);

        SpringUtil.makeCompactGrid(radeccolumnpanel, 1, radeccolumnpanel.getComponentCount(), 0, 0, 0, 0);
        addAstroDataPanel.add(radeccolumnpanel);


        JLabel dataTitleLabel = new JLabel("Select data to add");
        addAstroDataPanel.add(dataTitleLabel);

        JLabel nameTitleLabel = new JLabel("Enter table column name to add");
        addAstroDataPanel.add(nameTitleLabel);

        airmassCB = new JCheckBox("Airmass", addAirmass);
        airmassCB.setToolTipText("Add Airmass column to active table");
        addAstroDataPanel.add(airmassCB);

        airmassField = new JTextField(airmassName);
        airmassField.setBorder(BorderFactory.createEmptyBorder(0, 4, 0, 4));
        airmassField.setPreferredSize(new Dimension(250, 20));
        airmassField.setHorizontalAlignment(JTextField.LEFT);
        airmassField.setToolTipText("Airmass column name");
        addAstroDataPanel.add(airmassField);

        altitudeCB = new JCheckBox("Altitude", addAltitude);
        altitudeCB.setToolTipText("Add Altitude column to active table");
        addAstroDataPanel.add(altitudeCB);

        altitudeField = new JTextField(altitudeName);
        altitudeField.setBorder(BorderFactory.createEmptyBorder(0, 4, 0, 4));
        altitudeField.setPreferredSize(new Dimension(250, 20));
        altitudeField.setHorizontalAlignment(JTextField.LEFT);
        altitudeField.setToolTipText("Altitude/elevation column name");
        addAstroDataPanel.add(altitudeField);

        azimuthCB = new JCheckBox("Azimuth", addAzimuth);
        azimuthCB.setToolTipText("Add Azumuth column to active table");
        addAstroDataPanel.add(azimuthCB);

        azimuthField = new JTextField(azimuthName);
        azimuthField.setBorder(BorderFactory.createEmptyBorder(0, 4, 0, 4));
        azimuthField.setPreferredSize(new Dimension(250, 20));
        azimuthField.setHorizontalAlignment(JTextField.LEFT);
        azimuthField.setToolTipText("Azimuth column name");
        addAstroDataPanel.add(azimuthField);

        hourAngleCB = new JCheckBox("Hour Angle", addHourAngle);
        hourAngleCB.setToolTipText("Add Hour Angle column to active table");
        addAstroDataPanel.add(hourAngleCB);

        hourAngleField = new JTextField(hourAngleName);
        hourAngleField.setBorder(BorderFactory.createEmptyBorder(0, 4, 0, 4));
        hourAngleField.setPreferredSize(new Dimension(250, 20));
        hourAngleField.setHorizontalAlignment(JTextField.LEFT);
        hourAngleField.setToolTipText("Hour Angle column name");
        addAstroDataPanel.add(hourAngleField);

        zenithDistanceCB = new JCheckBox("Zenith Distance", addZenithDistance);
        zenithDistanceCB.setToolTipText("Add Zenith Distance column to active table");
        addAstroDataPanel.add(zenithDistanceCB);

        zenithDistanceField = new JTextField(zenithDistanceName);
        zenithDistanceField.setBorder(BorderFactory.createEmptyBorder(0, 4, 0, 4));
        zenithDistanceField.setPreferredSize(new Dimension(250, 20));
        zenithDistanceField.setHorizontalAlignment(JTextField.LEFT);
        zenithDistanceField.setToolTipText("Zenith Distance column name");
        addAstroDataPanel.add(zenithDistanceField);

        gjdCB = new JCheckBox("JD_UTC", addGJD);
        gjdCB.setToolTipText("Add Geocentric Julian Date (UTC) column to active table");
        addAstroDataPanel.add(gjdCB);

        gjdField = new JTextField(gjdName);
        gjdField.setBorder(BorderFactory.createEmptyBorder(0, 4, 0, 4));
        gjdField.setPreferredSize(new Dimension(250, 20));
        gjdField.setHorizontalAlignment(JTextField.LEFT);
        gjdField.setToolTipText("Geocentric Julian Date (UTC) column name");
        addAstroDataPanel.add(gjdField);

        hjdCB = new JCheckBox("HJD_UTC", addHJD);
        hjdCB.setToolTipText("Add Heliocentric Julian Date (UTC) column to active table");
        addAstroDataPanel.add(hjdCB);

        hjdField = new JTextField(hjdName);
        hjdField.setBorder(BorderFactory.createEmptyBorder(0, 4, 0, 4));
        hjdField.setPreferredSize(new Dimension(250, 20));
        hjdField.setHorizontalAlignment(JTextField.LEFT);
        hjdField.setToolTipText("Heliocentric Julian Date (UTC) column name");
        addAstroDataPanel.add(hjdField);

        hjdCorrCB = new JCheckBox("HJD Correction", addHJDCorr);
        hjdCorrCB.setToolTipText("Add Geocentric (UTC) to Heliocentric (UTC) Julian Date Correction column to active table");
        addAstroDataPanel.add(hjdCorrCB);

        hjdCorrField = new JTextField(hjdCorrName);
        hjdCorrField.setBorder(BorderFactory.createEmptyBorder(0, 4, 0, 4));
        hjdCorrField.setPreferredSize(new Dimension(250, 20));
        hjdCorrField.setHorizontalAlignment(JTextField.LEFT);
        hjdCorrField.setToolTipText("JD to HJD correction column name");
        addAstroDataPanel.add(hjdCorrField);

        bjdCB = new JCheckBox("BJD_TDB", addBJD);
        bjdCB.setToolTipText("Add Barycentric Julian Date (TDB) column to active table");
        addAstroDataPanel.add(bjdCB);

        bjdField = new JTextField(bjdName);
        bjdField.setBorder(BorderFactory.createEmptyBorder(0, 4, 0, 4));
        bjdField.setPreferredSize(new Dimension(250, 20));
        bjdField.setHorizontalAlignment(JTextField.LEFT);
        bjdField.setToolTipText("Barycentric Julian Date (TDB) column name");
        addAstroDataPanel.add(bjdField);

        bjdCorrCB = new JCheckBox("BJD Correction", addBJDCorr);
        bjdCorrCB.setToolTipText("Add Geocentric (UTC) to Barycentric (TDB) Julian Date Correction column to active table");
        addAstroDataPanel.add(bjdCorrCB);

        bjdCorrField = new JTextField(bjdCorrName);
        bjdCorrField.setBorder(BorderFactory.createEmptyBorder(0, 4, 0, 4));
        bjdCorrField.setPreferredSize(new Dimension(250, 20));
        bjdCorrField.setHorizontalAlignment(JTextField.LEFT);
        bjdCorrField.setToolTipText("JD to BJD correction column name");
        addAstroDataPanel.add(bjdCorrField);

        raNowCB = new JCheckBox("RA Now", addRaNow);
        raNowCB.setToolTipText("Add Right Ascension of target at epoch of observation column to active table");
        addAstroDataPanel.add(raNowCB);

        raNowField = new JTextField(raNowName);
        raNowField.setBorder(BorderFactory.createEmptyBorder(0, 4, 0, 4));
        raNowField.setPreferredSize(new Dimension(250, 20));
        raNowField.setHorizontalAlignment(JTextField.LEFT);
        raNowField.setToolTipText("Right Ascension of target at epoch of observation column name");
        addAstroDataPanel.add(raNowField);

        decNowCB = new JCheckBox("Declination Now", addDecNow);
        decNowCB.setToolTipText("Add Declination of target at epoch of observation column to active table");
        addAstroDataPanel.add(decNowCB);

        decNowField = new JTextField(decNowName);
        decNowField.setBorder(BorderFactory.createEmptyBorder(0, 4, 0, 4));
        decNowField.setPreferredSize(new Dimension(250, 20));
        decNowField.setHorizontalAlignment(JTextField.LEFT);
        decNowField.setToolTipText("Declination of target at epoch of observation column name");
        addAstroDataPanel.add(decNowField);

        ra2000CB = new JCheckBox("RA J2000", addRA2000);
        ra2000CB.setToolTipText("Add Right Ascension of target at epoch J2000 column to active table");
        addAstroDataPanel.add(ra2000CB);

        ra2000Field = new JTextField(ra2000Name);
        ra2000Field.setBorder(BorderFactory.createEmptyBorder(0, 4, 0, 4));
        ra2000Field.setPreferredSize(new Dimension(250, 20));
        ra2000Field.setHorizontalAlignment(JTextField.LEFT);
        ra2000Field.setToolTipText("Right Ascension of target at epoch J2000 column name");
        addAstroDataPanel.add(ra2000Field);

        dec2000CB = new JCheckBox("Declination J2000", addDec2000);
        dec2000CB.setToolTipText("Add Declination of target at epoch J2000 column to active table");
        addAstroDataPanel.add(dec2000CB);

        dec2000Field = new JTextField(dec2000Name);
        dec2000Field.setBorder(BorderFactory.createEmptyBorder(0, 4, 0, 4));
        dec2000Field.setPreferredSize(new Dimension(250, 20));
        dec2000Field.setHorizontalAlignment(JTextField.LEFT);
        dec2000Field.setToolTipText("Declination of target at epoch J2000 column name");
        addAstroDataPanel.add(dec2000Field);

        JLabel lineFeedLabel1 = new JLabel("");
        addAstroDataPanel.add(lineFeedLabel1);
        JLabel lineFeedLabel2 = new JLabel("");
        addAstroDataPanel.add(lineFeedLabel2);

        JLabel instructionLabel1L = new JLabel("Setup target and/or observatory parameters");
        addAstroDataPanel.add(instructionLabel1L);
        JLabel instructionLabel1R = new JLabel("");
        addAstroDataPanel.add(instructionLabel1R);

        JLabel instructionLabel2L = new JLabel("in 'MP Coordinate Converter' window,");
        addAstroDataPanel.add(instructionLabel2L);
        JLabel instructionLabel2R = new JLabel("");
        addAstroDataPanel.add(instructionLabel2R);

        JLabel buttonLabel = new JLabel("then press the 'Update Table' button.");
        addAstroDataPanel.add(buttonLabel);

        JPanel OKCancelPanel = new JPanel(new SpringLayout());
        JLabel buttonIndentLabel = new JLabel("");
        OKCancelPanel.add(buttonIndentLabel);

        autoAstroDataUpdateCB = new JCheckBox("Auto", autoAstroDataUpdate);
        autoAstroDataUpdateCB.setToolTipText("Automatically update table when plot is updated and this panel is open.");
        autoAstroDataUpdateCB.addItemListener(e -> {
            if (e.getStateChange() == ItemEvent.DESELECTED) {
                autoAstroDataUpdate = false;
            } else if (e.getStateChange() == ItemEvent.SELECTED) autoAstroDataUpdate = true;
            Prefs.set("plot2.autoAstroDataUpdate", autoAstroDataUpdate);
        });
        OKCancelPanel.add(autoAstroDataUpdateCB);

        OKbutton = new JButton("Update Table");
        OKbutton.addActionListener(e -> {
            astroConverterUpdating = true;
            JDColumn = (String) jdcolumnbox.getSelectedItem();
            int jdCol = table.getColumnIndex(JDColumn);
            if (jdCol == MeasurementTable.COLUMN_NOT_FOUND) {
                astroConverterUpdating = false;
                IJ.beep();
                IJ.showMessage("Error: could not find table column '" + JDColumn + "'");
                return;
            }

            getAstroPanelValues();

            int airmassCol = MeasurementTable.COLUMN_NOT_FOUND;
            int altitudeCol = MeasurementTable.COLUMN_NOT_FOUND;
            int azimuthCol = MeasurementTable.COLUMN_NOT_FOUND;
            int hourAngleCol = MeasurementTable.COLUMN_NOT_FOUND;
            int zenithDistanceCol = MeasurementTable.COLUMN_NOT_FOUND;
            int gjdCol = MeasurementTable.COLUMN_NOT_FOUND;
            int hjdCol = MeasurementTable.COLUMN_NOT_FOUND;
            int hjdCorrCol = MeasurementTable.COLUMN_NOT_FOUND;
            int bjdCol = MeasurementTable.COLUMN_NOT_FOUND;
            int bjdCorrCol = MeasurementTable.COLUMN_NOT_FOUND;
            int raNowCol = MeasurementTable.COLUMN_NOT_FOUND;
            int decNowCol = MeasurementTable.COLUMN_NOT_FOUND;
            int ra2000Col = MeasurementTable.COLUMN_NOT_FOUND;
            int dec2000Col = MeasurementTable.COLUMN_NOT_FOUND;

            if (addAirmass) airmassCol = table.getColumnIndex(airmassName);
            if (addAltitude) altitudeCol = table.getColumnIndex(altitudeName);
            if (addAzimuth) azimuthCol = table.getColumnIndex(azimuthName);
            if (addHourAngle) hourAngleCol = table.getColumnIndex(hourAngleName);
            if (addZenithDistance) zenithDistanceCol = table.getColumnIndex(zenithDistanceName);
            if (addGJD) gjdCol = table.getColumnIndex(gjdName);
            if (addHJD) hjdCol = table.getColumnIndex(hjdName);
            if (addHJDCorr) hjdCorrCol = table.getColumnIndex(hjdCorrName);
            if (addBJD) bjdCol = table.getColumnIndex(bjdName);
            if (addBJDCorr) bjdCorrCol = table.getColumnIndex(bjdCorrName);
            if (addRaNow) raNowCol = table.getColumnIndex(raNowName);
            if (addDecNow) decNowCol = table.getColumnIndex(decNowName);
            if (addRA2000) ra2000Col = table.getColumnIndex(ra2000Name);
            if (addDec2000) dec2000Col = table.getColumnIndex(dec2000Name);

//                if ((addAirmass && airmassCol != MeasurementTable.COLUMN_NOT_FOUND) ||
//                    (addAltitude && altitudeCol != MeasurementTable.COLUMN_NOT_FOUND) ||
//                    (addAzimuth && azimuthCol != MeasurementTable.COLUMN_NOT_FOUND) ||
//                    (addHourAngle && hourAngleCol != MeasurementTable.COLUMN_NOT_FOUND) ||
//                    (addZenithDistance && zenithDistanceCol != MeasurementTable.COLUMN_NOT_FOUND) ||
//                    (addGJD && gjdCol != MeasurementTable.COLUMN_NOT_FOUND) ||
//                    (addHJD && hjdCol != MeasurementTable.COLUMN_NOT_FOUND) ||
//                    (addHJDCorr && hjdCorrCol != MeasurementTable.COLUMN_NOT_FOUND) ||
//                    (addBJD && bjdCol != MeasurementTable.COLUMN_NOT_FOUND) ||
//                    (addBJDCorr && bjdCorrCol != MeasurementTable.COLUMN_NOT_FOUND) ||
//                    (addRaNow && raNowCol != MeasurementTable.COLUMN_NOT_FOUND) ||
//                    (addDecNow && decNowCol != MeasurementTable.COLUMN_NOT_FOUND) ||
//                    (addRA2000 && ra2000Col != MeasurementTable.COLUMN_NOT_FOUND) ||
//                    (addDec2000 && dec2000Col != MeasurementTable.COLUMN_NOT_FOUND) )
//                    {
//                    GenericDialog gd = new GenericDialog ("Over-write existing data?", addAstroDataFrame.getX()+100, addAstroDataFrame.getY()+100);
//                    gd.addMessage ((airmassCol != MeasurementTable.COLUMN_NOT_FOUND?"Airmass column: "+airmassName+"\n":"")+
//                                   (altitudeCol != MeasurementTable.COLUMN_NOT_FOUND?"Altitude column: "+altitudeName+"\n":"")+
//                                   (azimuthCol != MeasurementTable.COLUMN_NOT_FOUND?"Azimuth column: "+azimuthName+"\n":"")+
//                                   (hourAngleCol != MeasurementTable.COLUMN_NOT_FOUND?"Hour Angle column: "+hourAngleName+"\n":"")+
//                                   (zenithDistanceCol != MeasurementTable.COLUMN_NOT_FOUND?"Zenith Distance column: "+zenithDistanceName+"\n":"")+
//                                   (gjdCol != MeasurementTable.COLUMN_NOT_FOUND?"JD column: "+gjdName+"\n":"")+
//                                   (hjdCol != MeasurementTable.COLUMN_NOT_FOUND?"HJD column: "+hjdName+"\n":"")+
//                                   (hjdCorrCol != MeasurementTable.COLUMN_NOT_FOUND?"HJD Correction column: "+hjdCorrName+"\n":"")+
//                                   (bjdCol != MeasurementTable.COLUMN_NOT_FOUND?"BJD column: "+bjdName+"\n":"")+
//                                   (bjdCorrCol != MeasurementTable.COLUMN_NOT_FOUND?"BJD Correction column: "+bjdCorrName+"\n":"")+
//                                   (raNowCol != MeasurementTable.COLUMN_NOT_FOUND?"RA OBS column: "+raNowName+"\n":"")+
//                                   (decNowCol != MeasurementTable.COLUMN_NOT_FOUND?"DEC OBS column: "+decNowName+"\n":"")+
//                                   (ra2000Col != MeasurementTable.COLUMN_NOT_FOUND?"RA J2000 column: "+ra2000Name+"\n":"")+
//                                   (dec2000Col != MeasurementTable.COLUMN_NOT_FOUND?"Dec J2000 column: "+dec2000Name+"\n":"")+
//                                    "\n"+
//                                    "table column name(s) are already in use.\nPress OK to over-write existing data.");
//                    gd.showDialog();
//                    if (gd.wasCanceled())
//                        {
//                        astroConverterUpdating = false;
//                        return;
//                        }
//                    }
            defaultOKForeground = OKbutton.getForeground();
            Thread t = new Thread(() -> {
                if (OKbutton != null) {
                    OKbutton.setForeground(Color.RED);
                    OKbutton.setText("working...");
                    OKbutton.paint(OKbutton.getGraphics());
                }
            });

            t.start();
            Thread.yield();

            Prefs.set("plot2.JDColumn", JDColumn);

            saveAstroPanelPrefs();
            checkAndLockTable();
            int tableLength = table.getCounter();
            for (int i = 0; i < tableLength; i++) {
                if (updateMPCC(i)) {
                    if (addAirmass) table.setValue(airmassName, i, acc.getAirmass());
                    if (addAltitude) table.setValue(altitudeName, i, acc.getAltitude());
                    if (addAzimuth) table.setValue(azimuthName, i, acc.getAzimuth());
                    if (addHourAngle) table.setValue(hourAngleName, i, acc.getHourAngle());
                    if (addZenithDistance) table.setValue(zenithDistanceName, i, acc.getZenithDistance());
                    if (addGJD) table.setValue(gjdName, i, acc.getJD());
                    if (addHJD) table.setValue(hjdName, i, acc.getHJD());
                    if (addHJDCorr) table.setValue(hjdCorrName, i, acc.getHJDCorrection());
                    if (addBJD) table.setValue(bjdName, i, acc.getBJD());
                    if (addBJDCorr) table.setValue(bjdCorrName, i, acc.getBJDCorrection());
                    if (addRaNow) table.setValue(raNowName, i, acc.getRAEOI());
                    if (addDecNow) table.setValue(decNowName, i, acc.getDecEOI());
                    if (addRA2000) table.setValue(ra2000Name, i, acc.getRAJ2000());
                    if (addDec2000) table.setValue(dec2000Name, i, acc.getDecJ2000());
                } else {
                    if (addAirmass) table.setValue(airmassName, i, Double.NaN);
                    if (addAltitude) table.setValue(altitudeName, i, Double.NaN);
                    if (addAzimuth) table.setValue(azimuthName, i, Double.NaN);
                    if (addHourAngle) table.setValue(hourAngleName, i, Double.NaN);
                    if (addZenithDistance) table.setValue(zenithDistanceName, i, Double.NaN);
                    if (addGJD) table.setValue(gjdName, i, Double.NaN);
                    if (addHJD) table.setValue(hjdName, i, Double.NaN);
                    if (addHJDCorr) table.setValue(hjdCorrName, i, Double.NaN);
                    if (addBJD) table.setValue(bjdName, i, Double.NaN);
                    if (addBJDCorr) table.setValue(bjdCorrName, i, Double.NaN);
                    if (addRaNow) table.setValue(raNowName, i, Double.NaN);
                    if (addDecNow) table.setValue(decNowName, i, Double.NaN);
                    if (addRA2000) table.setValue(ra2000Name, i, Double.NaN);
                    if (addDec2000) table.setValue(dec2000Name, i, Double.NaN);
                }
            }


            table.show();
            table.setLock(false);
            if (!autoAstroDataUpdateRunning) updatePlot(updateAllFits(), false);
            Thread t2 = new Thread(() -> {
                if (OKbutton != null) {
                    OKbutton.setForeground(defaultOKForeground);
                    OKbutton.setText("Update Table");
                    OKbutton.paint(OKbutton.getGraphics());
                }
            });

            t2.start();
            Thread.yield();
            astroConverterUpdating = false;
            Prefs.set("plot2.JDColumn", JDColumn);
        });

        OKCancelPanel.add(OKbutton);

        JButton cancelbutton = new JButton(" Close ");
        cancelbutton.addActionListener(e -> {
            addAstroDataFrameWasShowing = false;
            getAstroPanelValues();
            saveAstroPanelPrefs();
            if (acc != null) acc.saveAndClose();
            closeAddAstroDataFrame();
        });
        OKCancelPanel.add(cancelbutton);

        SpringUtil.makeCompactGrid(OKCancelPanel, 1, OKCancelPanel.getComponentCount(), 6, 6, 6, 6);

        addAstroDataPanel.add(OKCancelPanel);

        SpringUtil.makeCompactGrid(addAstroDataPanel, addAstroDataPanel.getComponentCount() / 2, 2, 6, 6, 6, 6);

        addAstroDataFrame.add(addAstroDataScrollPane);
        addAstroDataFrame.setResizable(true);

        if (rememberWindowLocations) {
            IJU.setFrameSizeAndLocation(addAstroDataFrame, addAstroDataFrameLocationX, addAstroDataFrameLocationY, 0, 0);
        }
        UIHelper.recursiveFontSetter(addAstroDataFrame, p11);
        addAstroDataFrame.pack();
        addAstroDataFrame.setVisible(true);
        addAstroDataFrameWasShowing = true;

        if (!astroConverterUpdating) updateMPCC(-1);

    }

    static boolean updateMPCC(int row)  //set row negative to use first currently selected row
    {
        if (table == null) {
            return false;
        }
        JDColumn = (String) jdcolumnbox.getSelectedItem();
        jdCol = table.getColumnIndex(JDColumn);
        if (jdCol == MeasurementTable.COLUMN_NOT_FOUND) {
            if (row < 1) {
                IJ.beep();
                IJ.showMessage("Error: could not find JD table column '" + JDColumn + "'");
            }
            return false;
        }
        int tableLength = table.getCounter();
        if (tableLength < 1) return false;
        if (row < 0) {
            row = tpanel.getSelectionStart();
            if (row < 0 || row >= tableLength) row = 0;
        }

        if (tableLength > 0) {
            double jd = table.getValueAsDouble(jdCol, row);
            if (JDColumn.contains("-2400000")) jd += 2400000;
            if (!Double.isNaN(jd)) {
                returnCode = 0;
                if (useGJD) {
                    acc.setTime(jd);
                    if (!processCoordinates(row)) return false;
                } else if (useHJD) {
                    returnCode = acc.setHJDTime(jd);
                    if (returnCode == 0 && !processCoordinates(row)) return false;
                } else {
                    returnCode = acc.setBJDTime(jd);
                    if (returnCode == 0 && !processCoordinates(row)) return false;
                }
                if (returnCode == 3) {
                    IJ.beep();
                    IJ.showMessage("ERROR: Ohio State access failed when attempting to retrieve BJD(TDB).");
                    return false;
                }
            }
        }
        return true;
    }


    static boolean processCoordinates(int row) {
        if (useTableRaDec) {
            raColumn = (String) racolumnbox.getSelectedItem();
            decColumn = (String) deccolumnbox.getSelectedItem();
            raCol = table.getColumnIndex(raColumn);
            decCol = table.getColumnIndex(decColumn);
            if (raCol == MeasurementTable.COLUMN_NOT_FOUND) {
                if (row < 1) {
                    IJ.beep();
                    IJ.showMessage("Error: could not find RA table column '" + raColumn + "'");
                }
                return false;
            }
            if (decCol == MeasurementTable.COLUMN_NOT_FOUND) {
                if (row < 1) {
                    IJ.beep();
                    IJ.showMessage("Error: could not find DEC table column '" + decColumn + "'");
                }
                return false;
            }
            double ra = table.getValueAsDouble(raCol, row);
            double dec = table.getValueAsDouble(decCol, row);
            if (!Double.isNaN(ra) && !Double.isNaN(dec)) {
                returnCode = acc.processRADECJ2000(ra, dec);
            } else {
                returnCode = acc.processManualCoordinates();
            }
        } else {
            returnCode = acc.processManualCoordinates();
        }
        if (returnCode == 3) {
            IJ.beep();
            IJ.showMessage("ERROR: Ohio State access failed when attempting to retrieve BJD(TDB).");
            return false;
        }
        return true;
    }


    static void getAstroPanelValues() {
        addAirmass = airmassCB.isSelected();
        addAltitude = altitudeCB.isSelected();
        addAzimuth = azimuthCB.isSelected();
        addHourAngle = hourAngleCB.isSelected();
        addZenithDistance = zenithDistanceCB.isSelected();
        addGJD = gjdCB.isSelected();
        addHJD = hjdCB.isSelected();
        addHJDCorr = hjdCorrCB.isSelected();
        addBJD = bjdCB.isSelected();
        addBJDCorr = bjdCorrCB.isSelected();
        addRaNow = raNowCB.isSelected();
        addDecNow = decNowCB.isSelected();
        addRA2000 = ra2000CB.isSelected();
        addDec2000 = dec2000CB.isSelected();
        airmassName = airmassField.getText().trim();
        airmassName = airmassName.replace(' ', '_');
        altitudeName = altitudeField.getText().trim();
        altitudeName = altitudeName.replace(' ', '_');
        azimuthName = azimuthField.getText().trim();
        azimuthName = azimuthName.replace(' ', '_');
        hourAngleName = hourAngleField.getText().trim();
        hourAngleName = hourAngleName.replace(' ', '_');
        zenithDistanceName = zenithDistanceField.getText().trim();
        zenithDistanceName = zenithDistanceName.replace(' ', '_');
        gjdName = gjdField.getText().trim();
        gjdName = gjdName.replace(' ', '_');
        hjdName = hjdField.getText().trim();
        hjdName = hjdName.replace(' ', '_');
        hjdCorrName = hjdCorrField.getText().trim();
        hjdCorrName = hjdCorrName.replace(' ', '_');
        bjdName = bjdField.getText().trim();
        bjdName = bjdName.replace(' ', '_');
        bjdCorrName = bjdCorrField.getText().trim();
        bjdCorrName = bjdCorrName.replace(' ', '_');
        raNowName = raNowField.getText().trim();
        raNowName = raNowName.replace(' ', '_');
        decNowName = decNowField.getText().trim();
        decNowName = decNowName.replace(' ', '_');
        ra2000Name = ra2000Field.getText().trim();
        ra2000Name = ra2000Name.replace(' ', '_');
        dec2000Name = dec2000Field.getText().trim();
        dec2000Name = dec2000Name.replace(' ', '_');
    }

    public static void updateMPAstroConverter() {
        if (acc != null && addAstroDataFrame != null) {
            jdcolumnbox.setSelectedItem(jdcolumnbox.getSelectedItem());
        }
    }

    static int selectCurve() {
        GenericDialog gd;
        String[] curves = new String[maxCurves];
        int num = 0;
        for (int c = 0; c < maxCurves; c++) {
            if (plotY[c]) {
                curves[num] = "" + (c + 1);
                num++;
            }
        }
        if (num < 1) {
            gd = new GenericDialog("No curves selected", mainFrame.getX() + 100, mainFrame.getY() + 100);
            gd.addMessage("At least one curve must be enabled for plotting.");
            gd.hideCancelButton();
            gd.showDialog();
            return -1;
        }
        curves = Arrays.copyOf(curves, num);
        gd = new GenericDialog("Select plot curve", mainFrame.getX() + 100, mainFrame.getY() + 100);
        gd.addChoice("Add data to table from curve:", curves, curves[0]);
        gd.showDialog();
        if (gd.wasCanceled()) return -1;
        String selection = gd.getNextChoice();
        return parseInteger(selection, 0) - 1;
    }

    static void addNewColumn(int c, boolean main) {
        GenericDialog gd;
        if (tableName.equalsIgnoreCase("No Table Selected")) {
            gd = new GenericDialog("No Table Selected", (main ? mainFrame.getX() : subFrame.getX()) + 100, (main ? mainFrame.getY() : subFrame.getY()) + 100);
            gd.addMessage("A table must be open and selected to add new columns.");
            gd.hideCancelButton();
            gd.showDialog();
            return;
        }
        String xHeading = xlabel2[c];
        if (showXAxisAsPhase) {
            xHeading = "Phase";
        } else if (showXAxisAsDaysSinceTc) {
            xHeading = "DaysSinceTc";
        } else if (showXAxisAsHoursSinceTc) {
            xHeading = "HoursSinceTc";
        }

        String yHeading = "";
        String yErrHeading = "";
        String modelHeading = "";
        String residualHeading = "";
        String residualErrHeading = "";
        if (operatorIndex[c] == 5) {
            yHeading = "Centroid_Dist_" + xyc1label[c] + "-" + xyc2label[c];
        } else {
            yHeading = ylabel[c];
            yErrHeading = (errcolumn[c] < 0 || table.getColumnHeading(errcolumn[c]) == null ? "" : table.getColumnHeading(errcolumn[c]));
            if (operatorIndex[c] != 0 && operatorIndex[c] != 6) {
                yHeading += opSymbol[operatorIndex[c]] + oplabel[c];
                yErrHeading += opSymbol[operatorIndex[c]] + (operrcolumn[c] < 0 || table.getColumnHeading(operrcolumn[c]) == null ? "" : table.getColumnHeading(operrcolumn[c]));
            }
        }
        boolean atLeastOne = false;
        if (detrendFitIndex[c] != 0) {
            for (int v = 0; v < maxDetrendVars; v++) {
                if (detrendIndex[c][v] != 0) {
                    atLeastOne = true;
                    break;
                }
            }
        }
        String suffix = "_" + (atLeastOne ? "d" : "") + (detrendFitIndex[c] == 9 && useTransitFit[c] ? "f" : "") + (normIndex[c] != 0 && !mmag[c] ? "n" : "");

        if (!suffix.equals("_")) {
            yHeading += suffix;
            yErrHeading += suffix;
        }
        modelHeading = yHeading + "_model";
        residualHeading = yHeading + "_residual";
        residualErrHeading = yErrHeading + "_residual";

        if (operatorIndex[c] == 5) {
            yHeading += usePixelScale ? "(arcsecs)" : "(pixels)";
            modelHeading += usePixelScale ? "(arcsecs)" : "(pixels)";
            residualHeading += usePixelScale ? "(arcsecs)" : "(pixels)";
            residualErrHeading += usePixelScale ? "(arcsecs)" : "(pixels)";
        }

        String magsuffix = "";
        if (mmag[c]) {
            magsuffix += (mmag[firstCurve] && totalScaleFactor[firstCurve] == 1000 || totalScaleFactor[c] == 1000 ? "(mmag)" : "(mag)");
        }

        yHeading += magsuffix;
        yErrHeading += magsuffix;
        modelHeading += magsuffix;
        residualHeading += magsuffix;
        residualErrHeading += magsuffix;

        if ((inputAverageOverSize[c] != 1)) {
            String avgSuffix = "(inputAverageOverSize=" + inputAverageOverSize[c] + ")";
            xHeading += avgSuffix;
            yHeading += avgSuffix;
            yErrHeading += avgSuffix;
            modelHeading += avgSuffix;
            residualHeading += avgSuffix;
            residualErrHeading += avgSuffix;
        } else if (showXAxisNormal) {
            xHeading += "_B";
        }

        gd = new GenericDialog("Add new columns to table", (main ? mainFrame.getX() : subFrame.getX()) + 100, (main ? mainFrame.getY() : subFrame.getY()) + 100);
        gd.addCheckbox("Add new column from X-data", saveNewXColumn);
        gd.addStringField("New column name (from X-data): ", xHeading, 40);
        gd.addCheckbox("Add new column from Y-data", saveNewYColumn);
        gd.addStringField("New column name (from Y-data): ", yHeading, 40);
        if (hasErrors[c] || hasOpErrors[c]) {
            gd.addCheckbox("Add new column from Y-error", saveNewYErrColumn);
            gd.addStringField("New column name (from Y-error): ", yErrHeading, 40);
        }
        if (yModel1[c] != null && detrendFitIndex[c] == 9 && useTransitFit[c]) {
            gd.addCheckbox("Add new column from fitted model", saveNewModelColumn);
            gd.addStringField("New column name (from Model): ", modelHeading, 40);
        }
        if (residual[c] != null && detrendFitIndex[c] == 9 && useTransitFit[c]) {
            gd.addCheckbox("Add new column from model residuals", saveNewResidualColumn);
            gd.addStringField("New column name (from Model Residuals): ", residualHeading, 40);
        }
        if (yModel1Err[c] != null && detrendFitIndex[c] == 9 && useTransitFit[c]) {
            gd.addCheckbox("Add new column from model residuals error", saveNewResidualErrColumn);
            gd.addStringField("New column name (from Model Residual Error): ", residualErrHeading, 40);
        }
        gd.addCheckbox("Remove Scale from Y-data and Y-error before saving", unscale);
        gd.addCheckbox("Remove Shift from Y-data before saving", unshift);

        gd.addMessage("***New data column(s) will be added to the open table.***\n***Save table to save new column(s) to disk.***");
        gd.showDialog();
        if (gd.wasCanceled()) return;

        saveNewXColumn = gd.getNextBoolean();
        xHeading = gd.getNextString();
        saveNewYColumn = gd.getNextBoolean();
        yHeading = gd.getNextString();
        if (hasErrors[c] || hasOpErrors[c]) {
            saveNewYErrColumn = gd.getNextBoolean();
            yErrHeading = gd.getNextString();
        }
        if (yModel1[c] != null && detrendFitIndex[c] == 9 && useTransitFit[c]) {
            saveNewModelColumn = gd.getNextBoolean();
            modelHeading = gd.getNextString();
        }
        if (residual[c] != null && detrendFitIndex[c] == 9 && useTransitFit[c]) {
            saveNewResidualColumn = gd.getNextBoolean();
            residualHeading = gd.getNextString();
        }
        if (yModel1Err[c] != null && detrendFitIndex[c] == 9 && useTransitFit[c]) {
            saveNewResidualErrColumn = gd.getNextBoolean();
            residualErrHeading = gd.getNextString();
        }
        unscale = gd.getNextBoolean();
        unshift = gd.getNextBoolean();
        table.setPrecision(6);
        int xColumn = ResultsTable.COLUMN_IN_USE;
        int yColumn = ResultsTable.COLUMN_IN_USE;
        int yErrColumn = ResultsTable.COLUMN_IN_USE;
        int modelColumn = ResultsTable.COLUMN_IN_USE;
        int residualColumn = ResultsTable.COLUMN_IN_USE;
        int residualErrColumn = ResultsTable.COLUMN_IN_USE;
        if (saveNewXColumn) xColumn = table.getFreeColumn(xHeading);
        if (saveNewYColumn) yColumn = table.getFreeColumn(yHeading);
        boolean xUsed = saveNewXColumn && (xColumn == ResultsTable.COLUMN_IN_USE);
        boolean yUsed = saveNewYColumn && (yColumn == ResultsTable.COLUMN_IN_USE);

        boolean yErrUsed = false;
        if (hasErrors[c] || hasOpErrors[c]) {
            if (saveNewYErrColumn) yErrColumn = table.getFreeColumn(yErrHeading);
            yErrUsed = saveNewYErrColumn && (yErrColumn == ResultsTable.COLUMN_IN_USE);
        }

        boolean modelUsed = false;
        if (yModel1[c] != null && detrendFitIndex[c] == 9 && useTransitFit[c]) {
            if (saveNewModelColumn) modelColumn = table.getFreeColumn(modelHeading);
            modelUsed = saveNewModelColumn && (modelColumn == ResultsTable.COLUMN_IN_USE);
        }

        boolean residualUsed = false;
        if (residual[c] != null && detrendFitIndex[c] == 9 && useTransitFit[c]) {
            if (saveNewResidualColumn) residualColumn = table.getFreeColumn(residualHeading);
            residualUsed = saveNewResidualColumn && (residualColumn == ResultsTable.COLUMN_IN_USE);
        }

        boolean residualErrUsed = false;
        if (yModel1Err[c] != null && detrendFitIndex[c] == 9 && useTransitFit[c]) {
            if (saveNewResidualErrColumn) residualErrColumn = table.getFreeColumn(residualErrHeading);
            residualErrUsed = saveNewResidualErrColumn && (residualErrColumn == ResultsTable.COLUMN_IN_USE);
        }

//        if (xUsed || yUsed || yErrUsed || modelUsed || residualUsed || residualErrUsed)
//            {
//            gd = new GenericDialog ("Over-write existing data?", (main?mainFrame.getX():subFrame.getX())+100, (main?mainFrame.getY():subFrame.getY())+100);
//            gd.addMessage ((xUsed?"X-column: "+xHeading+"\n":"")+(yUsed?"Y-column: "+yHeading+"\n":"")+(yErrUsed?"Y-error column: "+yErrHeading+"\n":"")+
//                           (modelUsed?"Model column: "+modelHeading+"\n":"")+(residualUsed?"Residual column: "+residualHeading+"\n":"")+
//                           (residualErrUsed?"Residual Error column: "+residualErrHeading+"\n":"")+
//                           "name(s) are already in use.\nPress OK to over-write existing data.");
//            gd.showDialog();
//            if (gd.wasCanceled()) return;
//            }
        int skipNum = excludedHeadSamples < nn[c] ? excludedHeadSamples : nn[c] - 1;
        if (inputAverageOverSize[c] != 1 || skipNum < 0) skipNum = 0;
        if (saveNewXColumn) {
            xColumn = table.getColumnIndex(xHeading);
            if (inputAverageOverSize[c] == 1 && skipNum > 0) {
                for (int i = 0; i < skipNum; i++) {
                    table.setValue(xColumn, i, Double.NaN);
                }
            }
            if (showXAxisNormal && xlabel2[c].trim().startsWith("J.D.-2400000")) {
                for (int i = 0; i < nn[c]; i++) {
                    table.setValue(xColumn, i + skipNum, x[c][i] + xOffset - 2400000.0);
                }
            } else if (showXAxisNormal && xlabel2[c].contains("J.D.") || xlabel2[c].contains("JD")) {
                for (int i = 0; i < nn[c]; i++) {
                    table.setValue(xColumn, i + skipNum, x[c][i] + xOffset);
                }
            } else {
                for (int i = 0; i < nn[c]; i++) {
                    table.setValue(xColumn, i + skipNum, x[c][i]);
                }
            }
            if (nn[c] < table.getCounter() - skipNum) {
                for (int i = nn[c]; i < table.getCounter() - skipNum; i++) {
                    table.setValue(xColumn, i + skipNum, Double.NaN);
                }
            }
        }
        if (saveNewYColumn) {
            yColumn = table.getColumnIndex(yHeading);

            if (inputAverageOverSize[c] == 1 && skipNum > 0) {
                for (int i = 0; i < skipNum; i++) {
                    table.setValue(yColumn, i, Double.NaN);
                }
            }
            if (ylabel[c].trim().startsWith("J.D.-2400000")) {
                for (int i = 0; i < nn[c]; i++) {
                    table.setValue(yColumn, i + skipNum, y[c][i] + yOffset - 2400000.0);
                }
            } else if (ylabel[c].contains("J.D.") || ylabel[c].contains("JD")) {
                for (int i = 0; i < nn[c]; i++) {
                    table.setValue(yColumn, i + skipNum, y[c][i] + yOffset);
                }
            } else {
                if (normIndex[c] != 0 && !mmag[c] && !force[c]) {
                    for (int i = 0; i < nn[c]; i++) {
                        //table.setValue(yColumn, i+skipNum, y[c][i]);
                        table.setValue(yColumn, i + skipNum, 1 - (unscale ? 1.0 / totalScaleFactor[c] : 1.0) * (1.0 - (y[c][i] - subtotalShiftFactor[c])) + (unshift ? 0 : subtotalShiftFactor[c]));
                    }
                } else {
                    for (int i = 0; i < nn[c]; i++) {
                        table.setValue(yColumn, i + skipNum, (unscale ? 1.0 / totalScaleFactor[c] : 1.0) * ((y[c][i]) - (unshift ? subtotalShiftFactor[c] : 0)));
                    }
                }
            }
            if (nn[c] < table.getCounter() - skipNum) {
                for (int i = nn[c]; i < table.getCounter() - skipNum; i++) {
                    table.setValue(yColumn, i + skipNum, Double.NaN);
                }
            }
        }
        if ((hasErrors[c] || hasOpErrors[c]) && saveNewYErrColumn) {
            yErrColumn = table.getColumnIndex(yErrHeading);
            if (inputAverageOverSize[c] == 1 && skipNum > 0) {
                for (int i = 0; i < skipNum; i++) {
                    table.setValue(yErrColumn, i, Double.NaN);
                }
            }
            for (int i = 0; i < nn[c]; i++) {
                //table.setValue(yErrColumn, i+skipNum, yerr[c][i]);
                table.setValue(yErrColumn, i + skipNum, (unscale ? 1.0 / totalScaleFactor[c] : 1.0) * yerr[c][i]);
            }
            if (nn[c] < table.getCounter() - skipNum) {
                for (int i = nn[c]; i < table.getCounter() - skipNum; i++) {
                    table.setValue(yErrColumn, i + skipNum, Double.NaN);
                }
            }
        }
        skipNum = (inputAverageOverSize[c] == 1 ? skipNum : 0) + nFitTrim[c];
        if (saveNewModelColumn) {
            modelColumn = table.getColumnIndex(modelHeading);
            if (skipNum > 0) {
                for (int i = 0; i < skipNum; i++) {
                    table.setValue(modelColumn, i, Double.NaN);
                }
            }
            for (int i = 0; i < yModel1[c].length; i++) {
                table.setValue(modelColumn, i + skipNum, yModel1[c][i]);
            }

            if (yModel1[c].length + skipNum < table.getCounter()) {
                for (int i = yModel1[c].length + skipNum; i < table.getCounter(); i++) {
                    table.setValue(modelColumn, i, Double.NaN);
                }
            }
        }
        if (saveNewResidualColumn) {
            residualColumn = table.getColumnIndex(residualHeading);
            if (skipNum > 0) {
                for (int i = 0; i < skipNum; i++) {
                    table.setValue(residualColumn, i, Double.NaN);
                }
            }
            for (int i = 0; i < residual[c].length; i++) {
                table.setValue(residualColumn, i + skipNum, residual[c][i]);
            }

            if (residual[c].length + skipNum < table.getCounter()) {
                for (int i = residual[c].length + skipNum; i < table.getCounter(); i++) {
                    table.setValue(residualColumn, i, Double.NaN);
                }
            }
        }
        if (saveNewResidualErrColumn) {
            residualErrColumn = table.getColumnIndex(residualErrHeading);
            if (skipNum > 0) {
                for (int i = 0; i < skipNum; i++) {
                    table.setValue(residualErrColumn, i, Double.NaN);
                }
            }
            for (int i = 0; i < yModel1Err[c].length; i++) {
                table.setValue(residualErrColumn, i + skipNum, yModel1Err[c][i]);
            }

            if (yModel1Err[c].length + skipNum < table.getCounter()) {
                for (int i = yModel1Err[c].length + skipNum; i < table.getCounter(); i++) {
                    table.setValue(residualErrColumn, i, Double.NaN);
                }
            }
        }
        table.show();
    }

    static void addElement(Vector<Roi> list, Shape shape, Color color, int lineWidth) {
        roi = new ShapeRoi(shape);
        roi.setStrokeColor(color);
        roi.setStrokeWidth(lineWidth);
        list.addElement(roi);
    }

    static MouseListener shiftSpinnerMouseListener = new MouseListener() {
        public void mouseClicked(MouseEvent e) {
        }

        public void mousePressed(MouseEvent e) {
        }

        public void mouseReleased(MouseEvent e) {
            int first = 0;
            int last = maxCurves;
            if (e.isShiftDown()) {
                for (int c = 0; c < maxCurves; c++) {
                    if (customshiftspinner[c].getBorder().equals(greenBorder)) {
                        first = c;
                        break;
                    }
                }
                for (int c = 0; c < maxCurves; c++) {
                    if (e.getSource().equals(((JSpinner.DefaultEditor) customshiftspinner[c].getEditor()).getTextField())) {
                        last = c;
                        break;
                    }
                }
                if (last < first) {
                    int holdfirst = first;
                    first = last;
                    last = holdfirst;
                }
                for (int c = 0; c < maxCurves; c++) {
                    if (c >= first && c <= last) {
                        customshiftspinner[c].setBorder(greenBorder);
                    } else {
                        customshiftspinner[c].setBorder(defaultSpinnerBorder);
                    }
                }
            } else {
                for (int c = 0; c < maxCurves; c++) {
                    if (e.getSource().equals(((JSpinner.DefaultEditor) customshiftspinner[c].getEditor()).getTextField())) {
                        customshiftspinner[c].setBorder(greenBorder);
                    } else {
                        if (!e.isControlDown()) customshiftspinner[c].setBorder(defaultSpinnerBorder);
                    }
                }
            }
        }

        public void mouseEntered(MouseEvent e) {
        }

        public void mouseExited(MouseEvent e) {

        }
    };

    static MouseMotionListener panelMouseMotionListener = new MouseMotionListener() {
        public void mouseDragged(MouseEvent e) {
            panelShiftDown = e.isShiftDown();
            panelAltDown = e.isAltDown();
            panelControlDown = e.isControlDown();
        }

        public void mouseMoved(MouseEvent e) {
            panelShiftDown = e.isShiftDown();
            panelAltDown = e.isAltDown();
            panelControlDown = e.isControlDown();
        }
    };


    static MouseListener plotMouseListener = new MouseListener() {
        public void mouseClicked(MouseEvent e) {

            // mouse clicked code is in mouseReleased() to allow drag/click thresholding
        }

        public void mousePressed(MouseEvent e) {
            startDragX = (int) plot.descaleX(e.getX());
            startDragY = (int) plot.descaleY(e.getY());
            startDragScreenX = e.getX();
            startDragScreenY = e.getY();
            startDragSubImageX = plotImageCanvas.getSrcRect().x;
            startDragSubImageY = plotImageCanvas.getSrcRect().y;
            button2Drag = false;
            if (e.isControlDown())  //handle control click to move trend lines
            {
                if ((e.getModifiers() & MouseEvent.BUTTON1_MASK) != 0) {
                    handleControlLeftClickDrag(e, startDragX, startDragY);
                } else if ((e.getModifiers() & MouseEvent.BUTTON3_MASK) != 0) {
                    handleControlRightClickDrag(e, startDragX, startDragY);
                }
            }
        }

        public void mouseReleased(MouseEvent e) {
            if (updatePlotRunning) return;
            int screenX = e.getX();
            int screenY = e.getY();
            int imageX = plotImageCanvas.offScreenX(screenX);
            int imageY = plotImageCanvas.offScreenY(screenY);

            double xval = plotMinX + (double) (startDragX - Plot.LEFT_MARGIN) * (plotMaxX - plotMinX) / (double) (plot.getDrawingFrame().width);
            double yval = plotMaxY - (double) (startDragY - Plot.TOP_MARGIN) * (plotMaxY - plotMinY) / (double) (plot.getDrawingFrame().height);
            double dxval = (double) (imageX - startDragX) * (plotMaxX - plotMinX) / (double) (plot.getDrawingFrame().width);
            double dyval = (double) (imageY - startDragY) * (-1.0) * (plotMaxY - plotMinY) / (double) (plot.getDrawingFrame().height);


            if (((e.getModifiers() & MouseEvent.BUTTON2_MASK) != 0) && button2Drag && !e.isShiftDown() && !e.isControlDown() && !e.isAltDown()) {                                                          // measure distance and report in Results Table
                button2Drag = false;
                IJ.log("x=" + fourPlaces.format(xval) + ", y=" + fourPlaces.format(yval) + ", dx=" + fourPlaces.format(dxval) + ", dy=" + fourPlaces.format(dyval));
            }
            if (Math.sqrt((screenX - startDragScreenX) * (screenX - startDragScreenX) +      //check mouse click/drag threshold
                    (screenY - startDragScreenY) * (screenY - startDragScreenY)) < 4.0) {
                if (e.getButton() == MouseEvent.BUTTON1)       //left mouse click release
                {
                    if (e.isShiftDown() && !e.isAltDown() && !e.isControlDown())  //shift+left-click (remove selected table row)
                    {
                        selectedRowStart = tpanel.getSelectionStart();
                        selectedRowEnd = tpanel.getSelectionEnd();
                        for (int i = selectedRowEnd; i >= selectedRowStart; i--) {
                            deletedRowList.add(tpanel.getLine(i));
                        }
                        tpanel.clearSelection();
                        table = MeasurementTable.getTable(tableName);
//                                if (Data_Processor.running) Data_Processor.setTable(table);
                        table.show();
                        tpanel = MeasurementTable.getTextPanel(MeasurementTable.longerName(tableName));
                        int lineCount = tpanel.getLineCount();
                        if (selectedRowEnd >= lineCount - excludedTailSamples) {
                            selectedRowStart = inputAverageOverSize[firstCurve] * ((lineCount - excludedHeadSamples - excludedTailSamples) / inputAverageOverSize[firstCurve]) - inputAverageOverSize[firstCurve];
                            selectedRowEnd = selectedRowStart + inputAverageOverSize[firstCurve] - 1;
                        }
                        tpanel.setSelection(selectedRowStart, selectedRowEnd);
                        boldedDatum = (selectedRowStart - excludedHeadSamples) / inputAverageOverSize[firstCurve];
                        plotcoordlabel.setText("DATA: x=" + fourPlaces.format(x[firstCurve][boldedDatum]) + ", y=" + fourPlaces.format(y[firstCurve][boldedDatum]));
                        IJ.showStatus("data values: x=" + fourPlaces.format(x[firstCurve][boldedDatum]) + ", y=" + fourPlaces.format(y[firstCurve][boldedDatum]));
                        updatePlot(updateAllFits());
                    } else if (!e.isControlDown() && !e.isShiftDown() && !e.isAltDown()) // left mouse click release (zoom in)
                    {
                        zoomControl(e.getX(), e.getY(), -5);
                    }
                } else if (e.getButton() == MouseEvent.BUTTON2)                     //middle mouse click release
                {
                    IJ.log("x=" + fourPlaces.format(xval) + ", y=" + fourPlaces.format(yval));
                } else if (e.getButton() == MouseEvent.BUTTON3 && e.isShiftDown() && !e.isControlDown() && !e.isAltDown() && !deletedRowList.isEmpty()) //shift + right mouse click release
                {                                                                                     //undo delete selected table row
                    int lastRow = tpanel.getLineCount() - 1;
                    if (selectedRowEnd <= lastRow - inputAverageOverSize[firstCurve]) {  //restore all rows other than the last one OR next to last one when when inputAverageOverSize > 1
                        String[] lines = new String[lastRow - selectedRowStart + 1];
                        for (int i = 0; i < lines.length; i++) {
                            lines[i] = tpanel.getLine(selectedRowStart + i);
                        }
                        tpanel.setSelection(selectedRowStart, lastRow);
                        tpanel.clearSelection();
                        for (int i = 0; i < inputAverageOverSize[firstCurve]; i++) {
                            if (!deletedRowList.isEmpty()) {
                                tpanel.appendWithoutUpdate(deletedRowList.remove(deletedRowList.size() - 1));
                            }
                        }
                        for (String line : lines) {
                            tpanel.appendWithoutUpdate(line);
                        }
                    } else if (selectedRowEnd < lastRow) { //restore next to last row when inputAverageOverSize > 1
                        selectedRowStart -= inputAverageOverSize[firstCurve];
                        selectedRowEnd -= inputAverageOverSize[firstCurve];
                        String[] lines = new String[lastRow - selectedRowEnd];
                        for (int i = 0; i < lines.length; i++) {
                            lines[i] = tpanel.getLine(selectedRowEnd + 1 + i);
                        }
                        tpanel.setSelection(selectedRowEnd + 1, lastRow);
                        tpanel.clearSelection();
                        for (int i = 0; i < inputAverageOverSize[firstCurve]; i++) {
                            if (!deletedRowList.isEmpty()) {
                                tpanel.appendWithoutUpdate(deletedRowList.remove(deletedRowList.size() - 1));
                            }
                        }
                        for (String line : lines) {
                            tpanel.appendWithoutUpdate(line);
                        }
                        selectedRowStart += inputAverageOverSize[firstCurve];
                        selectedRowEnd += inputAverageOverSize[firstCurve];

                    } else { //restore last row in table
                        for (int i = 0; i < inputAverageOverSize[firstCurve]; i++) {
                            if (!deletedRowList.isEmpty()) {
                                tpanel.appendWithoutUpdate(deletedRowList.remove(deletedRowList.size() - 1));
                            }
                        }
                        selectedRowStart += inputAverageOverSize[firstCurve];
                    }
                    table = MeasurementTable.getTable(tableName);
                    table.show();
                    tpanel = MeasurementTable.getTextPanel(MeasurementTable.longerName(tableName));
                    selectedRowEnd = selectedRowStart + inputAverageOverSize[firstCurve] - 1;
                    tpanel.setSelection(selectedRowStart, selectedRowEnd);
                    boldedDatum = (selectedRowStart - excludedHeadSamples) / inputAverageOverSize[firstCurve];
                    plotcoordlabel.setText("DATA: x=" + fourPlaces.format(x[firstCurve][boldedDatum]) + ", y=" + fourPlaces.format(y[firstCurve][boldedDatum]));
                    IJ.showStatus("data values: x=" + fourPlaces.format(x[firstCurve][boldedDatum]) + ", y=" + fourPlaces.format(y[firstCurve][boldedDatum]));
                    updatePlot(updateAllFits());
                } else if (e.getButton() == MouseEvent.BUTTON3)                     //right mouse click release
                {
                    totalPanOffsetX = 0.0;
                    totalPanOffsetY = 0.0;
                    newPanOffsetX = 0.0;
                    newPanOffsetY = 0.0;
                    zoomControl(screenX, screenY, 10000);
                }
            } else                                                                      //complete drag operations
            {
                if (e.getButton() == MouseEvent.BUTTON1 && !e.isShiftDown() && !e.isControlDown() && !e.isAltDown())                             //left mouse drag release
                {
                    totalPanOffsetX += newPanOffsetX;
                    totalPanOffsetY += newPanOffsetY;
                    newPanOffsetX = 0.0;
                    newPanOffsetY = 0.0;
                    leftDragReleased = true;
                }
            }
            list.clear();
            path.reset();
            plotImageCanvas.repaint();
        }

        public void mouseEntered(MouseEvent e) {}

        public void mouseExited(MouseEvent e) {}
    };


    static MouseMotionListener plotMouseMotionListener = new MouseMotionListener() {
        public void mouseDragged(MouseEvent e) {
            int screenX = e.getX();
            int screenY = e.getY();
            int imageX = plotImageCanvas.offScreenX(e.getX());
            int imageY = plotImageCanvas.offScreenY(e.getY());
            if (Math.sqrt((screenX - startDragScreenX) * (screenX - startDragScreenX) +  //check mouse click/drag threshold
                    (screenY - startDragScreenY) * (screenY - startDragScreenY)) >= 4.0) {
                if ((e.getModifiers() & MouseEvent.BUTTON3_MASK) != 0)    // dragging with right mouse button
                {
                    if (!e.isControlDown() && !e.isShiftDown() && !e.isAltDown()) {                                                 // measure distance, show on plot and status bar and save to results window
                        list.clear();
                        path.reset();
                        path.moveTo(startDragX, startDragY);
                        path.lineTo(imageX, imageY);

                        addElement(list, path, Color.red, 2);
                        plotImageCanvas.setDisplayList(list);
                        double x = plotMinX + (double) (imageX - (Plot.LEFT_MARGIN)) * (plotMaxX - plotMinX) / (double) (plot.getDrawingFrame().width);
                        double y = plotMaxY - (double) (imageY - (Plot.TOP_MARGIN)) * (plotMaxY - plotMinY) / (double) (plot.getDrawingFrame().height);
                        double dx = (double) (imageX - startDragX) * (plotMaxX - plotMinX) / (double) (plot.getDrawingFrame().width);
                        double dy = (double) (imageY - startDragY) * (-1.0) * (plotMaxY - plotMinY) / (double) (plot.getDrawingFrame().height);
                        plotbottompanel = (Panel) plotWindow.getComponent(1);
                        plotbottompanel.setSize(600, 30);
                        plotcoordlabel = (Label) plotbottompanel.getComponent(4);
                        plotcoordlabel.setSize(400, 20);
                        plotcoordlabel.setText("x=" + fourPlaces.format(x) + ", y=" + fourPlaces.format(y) + ", dx=" + fourPlaces.format(dx) + ", dy=" + fourPlaces.format(dy));
                        IJ.showStatus("plot coordinates: x=" + fourPlaces.format(x) + ", y=" + fourPlaces.format(y) + ", dx=" + fourPlaces.format(dx) + ", dy=" + fourPlaces.format(dy));
                    } else if (e.isControlDown()) // control right-drag (update vertical marker 3 or 4 position)
                    {
                        handleControlRightClickDrag(e, imageX, imageY);
                    }
                } else if ((e.getModifiers() & MouseEvent.BUTTON2_MASK) != 0)        // dragging with middle mouse button
                {                                                          // measure distance, show on plot and status bar
                    button2Drag = true;                                        // and save to results window when mouse released
                    list.clear();
                    path.reset();
                    path.moveTo(startDragX, startDragY);
                    path.lineTo(imageX, imageY);

                    addElement(list, path, Color.red, 2);
                    plotImageCanvas.setDisplayList(list);
                    double x = plotMinX + (double) (imageX - (Plot.LEFT_MARGIN)) * (plotMaxX - plotMinX) / (double) plot.getDrawingFrame().width;
                    double y = plotMaxY - (double) (imageY - (Plot.TOP_MARGIN)) * (plotMaxY - plotMinY) / (double) (plot.getDrawingFrame().height);
                    double dx = (double) (imageX - startDragX) * (plotMaxX - plotMinX) / (double) (plot.getDrawingFrame().width);
                    double dy = (double) (imageY - startDragY) * (-1.0) * (plotMaxY - plotMinY) / (double) (plot.getDrawingFrame().height);
                    plotcoordlabel.setText("x=" + fourPlaces.format(x) + ", y=" + fourPlaces.format(y) + ", dx=" + fourPlaces.format(dx) + ", dy=" + fourPlaces.format(dy));
                    IJ.showStatus("plot coordinates: x=" + fourPlaces.format(x) + ", y=" + fourPlaces.format(y) + ", dx=" + fourPlaces.format(dx) + ", dy=" + fourPlaces.format(dy));
                } else if ((e.getModifiers() & MouseEvent.BUTTON1_MASK) != 0)            // dragging with left mouse button (pan image)
                {
                    if (!e.isControlDown() && !e.isAltDown()) {
//                                    if (true)//(zoom != 0.0)
//                                        {
                        newPanOffsetX = -(plotMaxX - plotMinX) * (screenX - startDragScreenX) / (plot.getDrawingFrame().width);
                        newPanOffsetY = (plotMaxY - plotMinY) * (screenY - startDragScreenY) / plot.getDrawingFrame().height;
                        updatePlot(updateNoFits());
//                                        }
//                                    else
//                                        {
//                                        totalPanOffsetX=0.0;
//                                        totalPanOffsetY=0.0;
//                                        newPanOffsetX=0.0;
//                                        newPanOffsetY=0.0;
//                                        leftDragReleased=false;
//                                        }
                        if (e.isShiftDown()) {
                            plotcoordlabel.setText("DATA: x=" + fourPlaces.format(x[firstCurve][boldedDatum]) + ", y=" + fourPlaces.format(y[firstCurve][boldedDatum]));
                            IJ.showStatus("data values: x=" + fourPlaces.format(x[firstCurve][boldedDatum]) + ", y=" + fourPlaces.format(y[firstCurve][boldedDatum]));
                        } else {
                            double x = plotMinX + (double) (imageX - (Plot.LEFT_MARGIN)) * (plotMaxX - plotMinX) / (double) (plot.getDrawingFrame().width);
                            double y = plotMaxY - (double) (imageY - (Plot.TOP_MARGIN)) * (plotMaxY - plotMinY) / (double) (plot.getDrawingFrame().height);  //was 58
                            plotcoordlabel.setText("x=" + fourPlaces.format(x) + ", y=" + fourPlaces.format(y));
                            IJ.showStatus("plot coordinates: x=" + fourPlaces.format(x) + ", y=" + fourPlaces.format(y));
                        }
                    } else if (e.isControlDown()) // control left-drag (update left detrend position)
                    {
                        handleControlLeftClickDrag(e, imageX, imageY);
                    }
                }
            }
        }


        public void mouseMoved(MouseEvent e) {

            int imageX = plotImageCanvas.offScreenX(e.getX());
            int imageY = plotImageCanvas.offScreenY(e.getY());
            double xval = plotMinX + (double) (imageX - (Plot.LEFT_MARGIN)) * (plotMaxX - plotMinX) / (double) (plot.getDrawingFrame().width);
            double yval = plotMaxY - (double) (imageY - (Plot.TOP_MARGIN)) * (plotMaxY - plotMinY) / (double) (plot.getDrawingFrame().height);


            if (e.isShiftDown() && !e.isControlDown() && !e.isAltDown())     //select table line nearest x-val of first selected curve
            {
                xval = plot.descaleX(e.getX()); // Fix for scaling
                nearestLine = 0;
                nearestX = Math.abs(xval - x[firstCurve][0]);
                if (nn[firstCurve] > 0) {
                    for (int i = 1; i < nn[firstCurve]; i++) {
                        if (Math.abs(xval - x[firstCurve][i]) < nearestX) {
                            nearestX = Math.abs(xval - x[firstCurve][i]);
                            nearestLine = i;
                        }
                    }
                }
                tpanel.setSelection(excludedHeadSamples + nearestLine * inputAverageOverSize[firstCurve], excludedHeadSamples + (nearestLine + 1) * inputAverageOverSize[firstCurve] - 1);
                boldedDatum = nearestLine;
                plotcoordlabel.setText("DATA: x=" + fourPlaces.format(x[firstCurve][nearestLine]) + ", y=" + fourPlaces.format(y[firstCurve][nearestLine]));
                IJ.showStatus("data values: x=" + fourPlaces.format(x[firstCurve][nearestLine]) + ", y=" + fourPlaces.format(y[firstCurve][nearestLine]));
                if (useBoldedDatum) updatePlot(updateNoFits());
                updateMPAstroConverter();
                if (useUpdateStack) updateStack();
            } else {
                if (plotcoordlabel == null) {
                    return;
                }
                plotcoordlabel.setText("x=" + fourPlaces.format(xval) + ", y=" + fourPlaces.format(yval));
                IJ.showStatus("plot coordinates: x=" + fourPlaces.format(xval) + ", y=" + fourPlaces.format(yval));
                if (boldedDatum != -1) {
                    boldedDatum = -1;
                    if (useBoldedDatum) updatePlot(updateNoFits());
                }
            }
        }
    };

    static public void handleControlLeftClickDrag(MouseEvent e, int imageX, int imageY) {
        double x = plot.descaleX(e.getX());
        double y = plot.descaleY(e.getY());
        double delta = 0.025 * (plotMaxX - plotMinX);
        boolean alreadyMoved = false;
        if (showDMarkers && x > dMarker2Value - delta && x < dMarker2Value + delta) {
            if (e.isAltDown()) dmarker3spinner.setValue(x + (Math.abs(vMarker2Value - vMarker1Value)));
            dmarker2spinner.setValue(x);
            alreadyMoved = true;
        }
        if (showDMarkers && x > dMarker3Value - delta && x < dMarker3Value + delta) {
            if (e.isAltDown()) dmarker2spinner.setValue(x + (Math.abs(vMarker2Value - vMarker1Value)));
            dmarker3spinner.setValue(x);
            alreadyMoved = true;
        }
        if (showDMarkers && x > dMarker1Value - delta && x < dMarker1Value + delta) {
            dmarker1spinner.setValue(x);
            alreadyMoved = true;
        }
        if (showDMarkers && x > dMarker4Value - delta && x < dMarker4Value + delta) {
            dmarker4spinner.setValue(x);
            alreadyMoved = true;
        }
        if (!alreadyMoved) {
            showDMarkersCB.setSelected(true);
            showDMarkers = true;
            if (e.isShiftDown())   //update vertical marker 1 position
            {
                useDMarker1CB.setSelected(true);
                useDMarker1 = true;
                dmarker1spinner.setValue(x);
            } else                   //update vertical marker 2 position
            {
                if (e.isAltDown()) dmarker3spinner.setValue(x + (Math.abs(vMarker2Value - vMarker1Value)));
                dmarker2spinner.setValue(x);
            }
        }
        plotcoordlabel.setText("x=" + fourPlaces.format(x) + ", y=" + fourPlaces.format(y));
        IJ.showStatus("plot coordinates: x=" + fourPlaces.format(x) + ", y=" + fourPlaces.format(y));
    }

    static public void handleControlRightClickDrag(MouseEvent e, int imageX, int imageY) {
        double x = plot.descaleX(e.getX());
        double y = plot.descaleY(e.getY());
        showDMarkersCB.setSelected(true);
        showDMarkers = true;
        if (e.isShiftDown()) //update vertical marker 4 position
        {
            useDMarker4CB.setSelected(true);
            useDMarker4 = true;
            dmarker4spinner.setValue(x);
        } else                 //update vertical marker 3 position
        {
            double olddmarker3value = dMarker3Value;
            if (e.isAltDown()) dmarker2spinner.setValue(x - (Math.abs(vMarker2Value - vMarker1Value)));
            dmarker3spinner.setValue(x);
        }
        plotcoordlabel.setText("x=" + fourPlaces.format(x) + ", y=" + fourPlaces.format(y));
        IJ.showStatus("plot coordinates: x=" + fourPlaces.format(x) + ", y=" + fourPlaces.format(y));
    }

    public static void updateStack() {
        ImageWindow iw;
        ImageStack imstack = null;
        iplus = IJ.getImage();
        sliceCol = table.getColumnIndex("slice");

        int row1Slice = 0;
        if (sliceCol != MeasurementTable.COLUMN_NOT_FOUND) row1Slice = (int) table.getValueAsDouble(sliceCol, 0);
        if (iplus != null) {
            iw = iplus.getWindow();
            imstack = iplus.getStack();
            if (iw != null && imstack != null && imstack.getSize() > 1 && ((row1Slice > 0 && row1Slice <= imstack.getSize()) || imstack.getSize() == table.getCounter())) {
//                WindowManager.toFront(iw);
                updateSlice();
                return;
            }
        }
        if (WindowManager.getWindowCount() > 0) {
            int[] ID = WindowManager.getIDList();
            for (int i : ID) {
                iplus = WindowManager.getImage(i);
                if (iplus != null) {
                    iw = iplus.getWindow();
                    imstack = iplus.getStack();
                } else { iw = null; }
                if (iw != null && imstack != null && imstack.getSize() > 1 && ((row1Slice > 0 && row1Slice <= imstack.getSize()) || imstack.getSize() == table.getCounter())) {
                    WindowManager.setCurrentWindow(iw);
                    WindowManager.toFront(iw);
                    updateSlice();
                    return;
                }
            }
        }
    }

    static void updateSlice() {
        Thread t = new Thread(() -> {
            Thread.yield();
            ImageStack imstack = iplus.getStack();
            selectedRow = tpanel.getSelectionStart();
            if (selectedRow < 0) { selectedRow = 0; } else if (selectedRow >= imstack.getSize()) selectedRow = imstack.getSize() - 1;
            int newSlice = 0;
            if (sliceCol != MeasurementTable.COLUMN_NOT_FOUND) {
                newSlice = (int) table.getValueAsDouble(sliceCol, selectedRow);
            }
            if (newSlice == 0) newSlice = selectedRow + 1;
            if (newSlice > imstack.getSize()) newSlice = imstack.getSize();
            iplus.setSlice(newSlice);
//                iplus.updateAndDraw();
        });
        t.start();
    }


    static MouseWheelListener plotMouseWheelListener = e -> {
        int screenX = e.getX();
        int screenY = e.getY();
        int magChangeSteps = e.getWheelRotation();
        zoomControl(screenX, screenY, magChangeSteps);
    };

    static public void zoomControl(int screenX, int screenY, int magChangeSteps) {
        if (plot.getDrawingFrame().contains(screenX, screenY)) {
            if (zoom == 0.0) {
                mouseX = screenX - plot.getDrawingFrame().x;
                mouseY = screenY - plot.getDrawingFrame().y;
//                        totalPanOffsetX=0.0;
//                        totalPanOffsetY=0.0;
//                        newPanOffsetX=0.0;
//                        newPanOffsetY=0.0;
            }
            zoom -= (1 - zoom) * magChangeSteps / 25.0;
            zoom = Math.min(Math.max(zoom, 0.0), 0.99);
            updatePlot(updateNoFits());
        }
    }

    static public void zoomIn(int x, int y) {
        double magnification = plotImageCanvas.getMagnification();
        if (magnification >= 32) return;
        double newMag = ImageCanvas.getHigherZoomLevel(magnification);

        int w = (int) Math.round(plotSizeX / newMag);
        if (w * newMag < plotSizeX) w++;
        int h = (int) Math.round(plotSizeY / newMag);
        if (h * newMag < plotSizeY) h++;
        x = plotImageCanvas.offScreenX(x);
        y = plotImageCanvas.offScreenY(y);
        Rectangle r = new Rectangle(x - w / 2, y - h / 2, w, h);
        if (r.x < 0) r.x = 0;
        if (r.y < 0) r.y = 0;
        if (r.x + w > plotSizeX) r.x = plotSizeX - w;
        if (r.y + h > plotSizeY) r.y = plotSizeY - h;
        plotImageCanvas.setSourceRect(r);
        plotImageCanvas.setMagnification(newMag);
        plotImageCanvas.repaint();
    }

    static void initializeVariables() {
        shiftIsDown = true;
        ignoreUpdate = false;
        title = "Main Title";
        titlePosX = .5;
        titlePosY = 0;
        subtitle = "Subtitle";
        subtitlePosX = 0.5;
        subtitlePosY = 35;
        legendPosX = 0.5;
        legendPosY = 0.03;
        button2Drag = false;
        priorityColumns = "j.d.,jd,hjd,bjd,ratio,rel_flux,source-sky,tot_c_cnts,alt,air,ccd,saturated,source_radius,sky_radius,fwhm,width,sky/pixel,x(fits),y(fits),peak,x(ij),y(ij)";
        priorityDetrendColumns = "airmass,j.d.,jd,fwhm_mean,fwhm_t1,sky/pixel_t1,width_t1,tot_c_cnts,x(fits)_t1,x(fits)_c1,y(fits)_t1,y(fits)_c1,peak_C2,tot_c_err,ccd-temp,x(ij),y(ij)";
        airmassName = "AIRMASS";
        altitudeName = "ALTITUDE";
        azimuthName = "AZIMUTH";
        bjdName = "BJD_TDB";
        bjdCorrName = "BJD_CORR";
        decNowName = "DEC_EOD";
        raNowName = "RA_EOD";
        dec2000Name = "DEC_J2000";
        ra2000Name = "RA_J2000";
        gjdName = "JD_UTC";
        hjdName = "HJD_UTC";
        hjdCorrName = "HJD_CORR";
        hourAngleName = "HOUR_ANGLE";
        zenithDistanceName = "ZENITH_DIST";
        addAirmass = true;
        addAltitude = true;
        addAzimuth = false;
        addBJD = true;
        addBJDCorr = false;
        addDecNow = false;
        addRaNow = false;
        addDec2000 = false;
        addRA2000 = false;
        addHJD = true;
        addGJD = false;
        addHJDCorr = false;
        addHourAngle = false;
        addZenithDistance = false;
        prioritizeColumns = true;
        updatePlotEnabled = true;
        refStarChanged = false;
        detrendParChanged = false;
        disableUpdatePlotBox = false;
        astroConverterUpdating = false;
        xlabeldefault = "J.D.-2400000";
        plotSizeX = 800;
        plotSizeY = 800;
        xExponent = 0;
        yExponent = 0;
        maxSubsetColumns = 5;
        mmagrefs = 10;
        xLegend = "";
        yLegend = "";
        invertYAxis = false;
        invertYAxisSign = 1;
        negateMag = true;
        saveNewXColumn = true;
        saveNewYColumn = true;
        saveNewYErrColumn = true;
        selectAnotherTableCanceled = false;
        vMarker1TopText = "Predicted";
        vMarker1BotText = "Ingress";
        vMarker2TopText = "Predicted";
        vMarker2BotText = "Egress";
        saveImage = true;
        savePlot = true;
        saveSeeingProfile = true;
        saveSeeingProfileStack = false;
        saveConfig = true;
        saveTable = true;
        saveApertures = true;
        saveLog = true;
        saveFitPanels = true;
        saveFitPanelText = true;
        saveDataSubset = true;
        showDataSubsetPanel = true;
        saveAllPNG = true;
        unscale = true;
        unshift = true;
        autoAstroDataUpdate = false;
        imageSuffix = "_field";
        seeingProfileSuffix = "_seeing-profile";
        seeingProfileStackSuffix = "_seeing-profile";
        aperSuffix = "_measurements";
        logSuffix = "_calibration";
        fitPanelSuffix = "_fitpanel";
        fitPanelTextSuffix = "_fitpanel";
        dataSubsetSuffix = "_datasubset";
        plotSuffix = "_lightcurve";
        configSuffix = "_measurements";
        dataSuffix = "_measurements";
        appendDestinationSuffix = "_A";
        appendSourceSuffix = "_B";
        templateDir = "";
        JDColumn = "JD_UTC";
        raColumn = "RA_OBJ";
        decColumn = "DEC_OBJ";
        jdCol = 0;
        raCol = 0;
        decCol = 0;
        combinedTableName = "Measurements";
        legendLeft = false;
        legendRight = false;
        useTitle = true;
        useSubtitle = true;
        useXCustomName = false;
        useYCustomName = false;
        useXColumnName = true;
        useYColumnName = true;
        xTics = true;
        yTics = true;
        xGrid = false;
        yGrid = true;
        xNumbers = true;
        yNumbers = true;
        plotAutoMode = false;
        nextPanel = false;
        canceled = false;
        waitSecs = false;
        openDataSetWindow = true;
        openRefStarWindow = true;
        rememberWindowLocations = true;
        keepSeparateLocationsForFitWindows = false;
        divideNotSubtract = true;
        autoScaleX = true;
        useFirstX = false;
        autoScaleY = true;
        showToolTips = true;
        showXAxisNormal = true;
        showXAxisAsPhase = false;
        showXAxisAsHoursSinceTc = false;
        showXAxisAsDaysSinceTc = false;
        T0 = 0;
        period = 1;
        duration = 3;
        netT0 = 0;
        netPeriod = 1;
        twoxPeriod = false;
        oddNotEven = false;
        showXScaleInfo = true;
        showYScaleInfo = true;
        showYNormInfo = true;
        showYShiftInfo = true;
        showLScaleInfo = true;
        showLRelScaleInfo = false;
        showLShiftInfo = false;
        showLRelShiftInfo = false;
        showYAvgInfo = true;
        showLAvgInfo = true;
        showYmmagInfo = true;
        showLmmagInfo = true;
        showLdetrendInfo = true;
        showLnormInfo = true;
        showYSymbolInfo = false;
        showLSymbolInfo = false;
        showSigmaForAllCurves = false;
        showSigmaForDetrendedCurves = true;
        useNelderMeadChi2ForDetrend = true;
        openFitPanels = true;
//        useTwoLineLegend = false;
        useWideDataPanel = true;
        subframeWasShowing = true;
        newPlotWindow = false;
        useDefaultSettings = false;
        tableHasText = false;
        setSubpanelVisible = false;
        refStarPanelWasShowing = false;
        addAstroDataFrameWasShowing = false;
        usePixelScale = true;
        deletedRowList = new ArrayList<>();
        useBoldedDatum = true;
        useUpdateStack = false;
        multiUpdate = false;
        selectedRow = -1;
        panelShiftDown = false;
        panelControlDown = false;
        panelAltDown = false;
        keepFileNamesOnAppend = true;
        updatePlotRunning = false;
        autoAstroDataUpdateRunning = false;
        skipPlotUpdate = false;
        boldedDatum = -1;
        pixelScale = 1.0;

        orbitalPeriodStep = 0.1;
        eccentricityStep = 0.01;
        omegaStep = 1.0;
        teffStep = 100.0;
        jminuskStep = 0.01;
        mStarStep = 0.1;
        rStarStep = 0.1;
        rhoStarStep = 0.1;

        plotOptions = 0;
        xMin = 0.0;
        xBase = 0.0;
        xMax = 0.0;
        xWidth = 0.3;
        yMin = 0.0;
        yMax = 0.0;
        xautoscalemin = 0.0;
        xautoscalemax = 0.0;
        yautoscalemin = 0.0;
        yautoscalemax = 0.0;
        yRange = 0.0;
        zoom = 0.0;

        mainFrameLocationX = 40;
        mainFrameLocationY = 40;
        subFrameLocationX = 40;
        subFrameLocationY = 40;
        refStarFrameLocationX = 40;
        refStarFrameLocationY = 40;
        addAstroDataFrameLocationX = -100000;  //force offscreen
        addAstroDataFrameLocationY = -100000;  //force offscreen
        plotFrameLocationX = 40;
        plotFrameLocationY = 40;
        mainFrameWidth = 0;
        mainFrameHeight = 0;
        subFrameWidth = 0;
        subFrameHeight = 0;

        excludedHeadSamples = 0;
        excludedTailSamples = 0;
        holdExcludedHeadSamples = 0;
        holdExcludedTailSamples = 0;
        excludedHeadSamplesStep = 1;
        excludedTailSamplesStep = 1;
        refStarHorzWidth = 30;
        lastRefStar = 1;
        returnCode = 0;
        cycleEnabledStarsLess1PressedConsecutive = false;

        yMaxStep = 1.0;
        yMinStep = 1.0;
        yMid = 0.0;
        firstXmin = 0.0;
        plotSizeXStep = 10;
        plotSizeYStep = 10;
        xStep = 0.01;

        mainmenubar = new JMenuBar();
        filemenu = new JMenu("File");
        preferencesmenu = new JMenu("Preferences");

        tableName = "Results";
        list = new Vector<>();
        path = new GeneralPath();

//        symbols = fourPlaces.getDecimalFormatSymbols();
//        decSep = symbols.getDecimalSeparator();
//        thouSep = symbols.getGroupingSeparator();

        delayedUpdateTask = null;
        delayedUpdateTimer = null;

        markers = new String[]{"box", "circle", "cross", "dot", "line", "triangle", "X"};
        colors = new String[]{"black", "dark gray", "gray", "light gray", "green", "dark green", "light blue", "blue", "magenta", "pink", "red", "orange", "yellow", "brown", "purple", "teal"};
        fitFrameIcon = createImageIcon("astroj/images/detrend_fit_transit.png", "Fit exoplanet transit to all data");
        normiconlist = new ImageIcon[]{createImageIcon("astroj/images/norm_off.png", "Do not use normalization"),                                // 0
                createImageIcon("astroj/images/norm_left.png", "Normalize based on data left of vertical marker 1"),      // 1
                createImageIcon("astroj/images/norm_right.png", "Normalize based on data right of vertical marker 2"),    // 2
                createImageIcon("astroj/images/norm_outside.png", "Normalize based on data outside of vertical markers"), // 3
                createImageIcon("astroj/images/norm_inside.png", "Normalize based on data inside of vertical markers"),   // 4
                createImageIcon("astroj/images/norm_left2.png", "Normalize based on data left of vertical marker 2"),     // 5
                createImageIcon("astroj/images/norm_right2.png", "Normalize based on data right of vertical marker 1"),   // 6
                createImageIcon("astroj/images/norm_all.png", "Normalize based on all data"),                             // 7
        };

        detrendiconlist = new ImageIcon[]{createImageIcon("astroj/images/norm_off.png", "Do not use detrending"),                                 // 0
                createImageIcon("astroj/images/norm_user.png", "User defined detrending constant"),                     // 1
                createImageIcon("astroj/images/detrend_left.png", "Detrend based on data left of vertical marker 1"),      // 2
                createImageIcon("astroj/images/detrend_right.png", "Detrend based on data right of vertical marker 2"),    // 3
                createImageIcon("astroj/images/detrend_outside.png", "Detrend based on data outside of vertical markers"), // 4
                createImageIcon("astroj/images/detrend_inside.png", "Detrend based on data inside of vertical markers"),   // 5
                createImageIcon("astroj/images/detrend_left2.png", "Detrend based on data left of vertical marker 2"),     // 6
                createImageIcon("astroj/images/detrend_right2.png", "Detrend based on data right of vertical marker 1"),   // 7
                createImageIcon("astroj/images/detrend_all.png", "Detrend based on all data"),                             // 8
                createImageIcon("astroj/images/detrend_fit_transit.png", "Fit exoplanet transit to all data"),                 // 9
        };

        operators = new String[]{"none", "divide by", "multiply by", "subtract", "add", "centroid distance", "custom error"};

        opSymbol = new String[]{"", " / ", " * ", " - ", " + ", " -> ", " (with error) "};

        spinnerscalelist = new String[]{"      0.0000000001", "        0.000000001", "          0.00000001", "            0.0000001", "              0.000001", "                0.00001", "                  0.0001", "                    0.001", "                    0.005", "                    0.010", "                    0.100", "                    0.500", "                    1.000", "                  10.000", "                100.000", "              1000.000", "            10000.000", "          100000.000", "        1000000.000", "      10000000.000", "    100000000.000", "1000000000.000",};

        integerspinnerscalelist = new String[]{"                    1", "                    2", "                    3", "                    4", "                    5", "                    6", "                    7", "                    8", "                    9", "                  10", "                  25", "                  50", "                100", "                250", "                500", "              1000"};
    }

    static public void setupArrays() {
        closeFitFrames();
        maxCurves = (int) Prefs.get("plot.maxCurves", 8);
        maxDetrendVars = (int) Prefs.get("plot.maxDetrendVars", 3);
        if (maxCurves < 1) maxCurves = 1;
        if (maxDetrendVars < 1) maxDetrendVars = 1;
        maxFittedVars = 7 + maxDetrendVars;
        nn = new int[maxCurves];
        nnr = new int[maxCurves];
        nFitTrim = new int[maxCurves];

        yAverage = new double[maxCurves];
        yBaselineAverage = new double[maxCurves];
        yDepthEstimate = new double[maxCurves];
        detrendYAverage = new double[maxCurves];
        xlabel2 = new String[maxCurves];
        detrendlabel = new String[maxCurves][maxDetrendVars];
        detrendlabelhold = new String[maxCurves][maxDetrendVars];
        detrendcolumn = new int[maxCurves][maxDetrendVars];
        detrendYDNotConstant = new boolean[maxDetrendVars];
        xModel1 = new double[maxCurves][];
        xModel2 = new double[maxCurves][];
        yModel1 = new double[maxCurves][];
        yModel1Err = new double[maxCurves][];
        yModel2 = new double[maxCurves][];

        lcModel = new double[maxCurves][];
        residual = new double[maxCurves][];
        plottedResidual = new double[maxCurves][];

        binDisplay = new boolean[maxCurves];

        start = new double[maxCurves][];
        width = new double[maxCurves][];
        step = new double[maxCurves][];
        index = new int[maxCurves][];
        startDetrendPars = new int[maxCurves];
        endDetrendPars = new int[maxCurves];
        coeffs = new double[maxCurves][];
        dof = new int[maxCurves];
        sigma = new double[maxCurves];
        prevSigma = new double[maxCurves];
        prevBic = new double[maxCurves];
        nTries = new int[maxCurves];
        smoothLen = new int[maxCurves];
        converged = new boolean[maxCurves];
        t14 = new double[maxCurves];
        t23 = new double[maxCurves];
        tau = new double[maxCurves];
        transitDepth = DoubleStream.generate(() -> 0).limit(maxCurves).toArray();
        stellarDensity = new double[maxCurves];
        bp = new double[maxCurves];
        chi2dof = new double[maxCurves];
        planetRadius = new double[maxCurves];
        spectralType = new String[maxCurves];
        bic = new double[maxCurves];
        chi2 = new double[maxCurves];
        fitMin = new double[maxCurves];
        fitMax = new double[maxCurves];
        fitLeft = new double[maxCurves];
        fitRight = new double[maxCurves];

        orbitalPeriod = new double[maxCurves];
        eccentricity = new double[maxCurves];
        omega = new double[maxCurves];
        lonAscNode = new double[maxCurves];
        teff = new double[maxCurves];
        forceCircularOrbit = new boolean[maxCurves];
        useLonAscNode = new boolean[maxCurves];
        orbitalPeriodSpinner = new JSpinner[maxCurves];
        eccentricitySpinner = new JSpinner[maxCurves];
        omegaSpinner = new JSpinner[maxCurves];
        teffSpinner = new JSpinner[maxCurves];
        jminuskSpinner = new JSpinner[maxCurves];
        mStarSpinner = new JSpinner[maxCurves];
        rStarSpinner = new JSpinner[maxCurves];
        rhoStarSpinner = new JSpinner[maxCurves];
        tolerance = new double[maxCurves];
        toleranceSpinner = new JSpinner[maxCurves];
        residualShiftSpinner = new JSpinner[maxCurves];
        extractPriorsButton = new JButton[maxCurves];
        autoUpdatePriorsCB = new JCheckBox[maxCurves];
        autoUpdatePriors = new boolean[maxCurves];
        maxFitSteps = new int[maxCurves];
        modelLineWidth = new int[maxCurves];
        residualLineWidth = new int[maxCurves];
        maxFitStepsSpinner = new JSpinner[maxCurves];
        modelLineWidthSpinner = new JSpinner[maxCurves];
        residualLineWidthSpinner = new JSpinner[maxCurves];
        useTransitFit = new boolean[maxCurves];
        showLTranParams = new boolean[maxCurves];
        showLResidual = new boolean[maxCurves];

        autoUpdateFit = new boolean[maxCurves];
        showModel = new boolean[maxCurves];
        showResidual = new boolean[maxCurves];
        showResidualError = new boolean[maxCurves];
        forceCircularOrbitCB = new JCheckBox[maxCurves];
        useTransitFitCB = new JCheckBox[maxCurves];
        showLTranParamsCB = new JCheckBox[maxCurves];
        showLResidualCB = new JCheckBox[maxCurves];
        bpLockCB = new JCheckBox[maxCurves];

        fitMenuBar = new JMenuBar[maxCurves];
        fitFileMenu = new JMenu[maxCurves];
        autoPriorsMenu = new JMenu[maxCurves];
        baselinePriorCB = new JCheckBoxMenuItem[maxCurves];
        saveFitPanelPngMenuItem = new JMenuItem[maxCurves];
        saveFitPanelJpgMenuItem = new JMenuItem[maxCurves];
        saveFitTextMenuItem = new JMenuItem[maxCurves];
        depthPriorCB = new JCheckBoxMenuItem[maxCurves];
        arPriorCB = new JCheckBoxMenuItem[maxCurves];
        tcPriorCB = new JCheckBoxMenuItem[maxCurves];
        inclPriorCB = new JCheckBoxMenuItem[maxCurves];
        autoUpdatePrior = new boolean[maxCurves][maxFittedVars];

        autoUpdateFitCB = new JCheckBox[maxCurves];
        showModelCB = new JCheckBox[maxCurves];
        showResidualCB = new JCheckBox[maxCurves];
        showResidualErrorCB = new JCheckBox[maxCurves];

        useCustomFitStep = new boolean[maxCurves][maxFittedVars];
        useCustomFitStepCB = new JCheckBox[maxCurves][maxFittedVars];
        isFitted = new boolean[maxCurves][maxFittedVars];
        detrendVarAllNaNs = new boolean[maxCurves][maxDetrendVars];
        lockToCenter = new boolean[maxCurves][maxFittedVars];
        lockToCenterCB = new JCheckBox[maxCurves][maxFittedVars];
        copyAndLockButton = new JButton[maxCurves][maxFittedVars];
        priorCenter = new double[maxCurves][maxFittedVars];
        priorCenterStep = new double[maxFittedVars];
        priorCenterSpinner = new JSpinner[maxCurves][maxFittedVars];
        priorCenterStepSpinner = new JSpinner[maxFittedVars];
        priorCenterStepPopup = new JPopupMenu[maxFittedVars];
        priorCenterStepPanel = new JPanel[maxFittedVars];

        usePriorWidth = new boolean[maxCurves][maxFittedVars];
        usePriorWidthCB = new JCheckBox[maxCurves][maxFittedVars];
        priorWidth = new double[maxCurves][maxFittedVars];
        priorWidthStep = new double[maxFittedVars];
        priorWidthSpinner = new JSpinner[maxCurves][maxFittedVars];
        priorWidthStepSpinner = new JSpinner[maxFittedVars];
        priorWidthStepPopup = new JPopupMenu[maxFittedVars];
        priorWidthStepPanel = new JPanel[maxFittedVars];

        fitStep = new double[maxCurves][maxFittedVars];
        fitStepStep = new double[maxFittedVars];
        fitStepSpinner = new JSpinner[maxCurves][maxFittedVars];
        fitStepStepSpinner = new JSpinner[maxFittedVars];
        fitStepStepPopup = new JPopupMenu[maxFittedVars];
        fitStepStepPanel = new JPanel[maxFittedVars];
        defaultFitStep = new double[maxFittedVars];


        chi2dofLabel = new JTextField[maxCurves];
        dofLabel = new JTextField[maxCurves];
        chi2Label = new JTextField[maxCurves];
        bicLabel = new JTextField[maxCurves];
        sigmaLabel = new JTextField[maxCurves];
        sigmaPanel = new JPanel[maxCurves];
        sigmaBorder = new Border[maxCurves];
        t14Label = new JTextField[maxCurves];
        t14HoursLabel = new JTextField[maxCurves];
        t23Label = new JTextField[maxCurves];
        tauLabel = new JTextField[maxCurves];
        stellarDensityLabel = new JTextField[maxCurves];
        bpSpinner = new JSpinner[maxCurves];
        planetRadiusLabel = new JTextField[maxCurves];
        transitDepthLabel = new JTextField[maxCurves];
        transitDepthPanel = new JPanel[maxCurves];
        stepsTakenLabel = new JTextField[maxCurves];

        bestFit = new double[maxCurves][maxFittedVars];
        bestFitLabel = new JTextField[maxCurves][maxFittedVars];

        fitDetrendComboBox = new JComboBox[maxCurves][maxDetrendVars];
        useFitDetrendCB = new JCheckBox[maxCurves][maxDetrendVars];

        fitNowButton = new JButton[maxCurves];

        fitFrame = new JFrame[maxCurves];
        fitPanel = new JPanel[maxCurves];
        minimization = new Minimization();
        fitScrollPane = new JScrollPane[maxCurves];
        fitFrameLocationX = new int[maxCurves];
        fitFrameLocationY = new int[maxCurves];
        setupDataBuffers();

        detrendVarDisplayed = new int[maxCurves];
        xlabel = new String[maxCurves];
        ylabel = new String[maxCurves];
        xcolumn = new int[maxCurves];
        ycolumn = new int[maxCurves];
        errcolumn = new int[maxCurves];
        operrcolumn = new int[maxCurves];
        xc1column = new int[maxCurves];
        xc2column = new int[maxCurves];
        yc1column = new int[maxCurves];
        yc2column = new int[maxCurves];
        oplabel = new String[maxCurves];
        opcolumn = new int[maxCurves];
//                errlabel = new String[maxCurves];
//                operrlabel = new String[maxCurves];
        showErrors = new boolean[maxCurves];
        bpLock = new boolean[maxCurves];
        hasErrors = new boolean[maxCurves];
//////                showErr = new boolean[maxCurves];
//                showOpErrors = new boolean[maxCurves];
        hasOpErrors = new boolean[maxCurves];
        lines = new boolean[maxCurves];
        smooth = new boolean[maxCurves];
        marker = new int[maxCurves];
        residualSymbol = new int[maxCurves];
        color = new Color[maxCurves];
        modelColor = new Color[maxCurves];
        residualModelColor = new Color[maxCurves];
        residualColor = new Color[maxCurves];
        residualShift = new double[maxCurves];
        autoResidualShift = new double[maxCurves];
        markerIndex = new int[maxCurves];
        residualSymbolIndex = new int[maxCurves];
        colorIndex = new int[maxCurves];
        modelColorIndex = new int[maxCurves];
        residualModelColorIndex = new int[maxCurves];
        residualColorIndex = new int[maxCurves];
        operatorIndex = new int[maxCurves];
        cblabels = new String[maxCurves];
        moreOptions = new boolean[maxCurves];
        autoScaleFactor = new double[maxCurves];
        autoScaleStep = new double[maxCurves];
        autoShiftFactor = new double[maxCurves];
        autoShiftStep = new double[maxCurves];
        customScaleFactor = new double[maxCurves];
        customScaleStep = new double[maxCurves];
        customShiftFactor = new double[maxCurves];
        customShiftStep = new double[maxCurves];
        detrendFactor = new double[maxCurves][maxDetrendVars];
        detrendFactorStep = new double[maxCurves][maxDetrendVars];
        baseline = new double[maxCurves];
        subtotalScaleFactor = new double[maxCurves];
        subtotalShiftFactor = new double[maxCurves];
        totalScaleFactor = new double[maxCurves];
        totalShiftFactor = new double[maxCurves];
        yWidth = new double[maxCurves];
        xMinimum = new double[maxCurves];
        xMaximum = new double[maxCurves];
        yMinimum = new double[maxCurves];
        yMn = new double[maxCurves];
        yMaximum = new double[maxCurves];
        yMx = new double[maxCurves];
        yWidthOrig = new double[maxCurves];
        yMidpoint = new double[maxCurves];
        mmag = new boolean[maxCurves];
        fromMag = new boolean[maxCurves];
        normIndex = new int[maxCurves];
        detrendFitIndex = new int[maxCurves];
        detrendIndex = new int[maxCurves][maxDetrendVars];
        plotY = new boolean[maxCurves];
        useColumnName = new boolean[maxCurves];
        useLegend = new boolean[maxCurves];
        force = new boolean[maxCurves];
//                detrendFit = new boolean[maxCurves];
        inputAverageOverSize = new int[maxCurves];
        legend = new String[maxCurves];
        xyc1label = new String[maxCurves];
        xyc2label = new String[maxCurves];
        ASInclude = new boolean[maxCurves];
        autoscaleincludebox = new JCheckBox[maxCurves];

        usecurvebox = new JCheckBox[maxCurves];
        errorcolumnbox = new JCheckBox[maxCurves];
//                operrorcolumnbox = new JCheckBox[maxCurves];
        useDataAvgbox = new JCheckBox[maxCurves];
        usemmagbox = new JCheckBox[maxCurves];
        fromMagBox = new JCheckBox[maxCurves];
        normtypecombobox = new JComboBox[maxCurves];
        detrendtypecombobox = new JComboBox[maxCurves];
        uselinesbox = new JCheckBox[maxCurves];
        usesmoothbox = new JCheckBox[maxCurves];
        forcebox = new JCheckBox[maxCurves];
        shiftAboveBox = new JCheckBox[maxCurves];
        shiftBelowBox = new JCheckBox[maxCurves];
        xdatacolumn = new JComboBox[maxCurves];
        ydatacolumn = new JComboBox[maxCurves];
        operatorselection = new JComboBox[maxCurves];
        operatorcolumn = new JComboBox[maxCurves];
        detrendbox = new JComboBox[maxCurves];
        markersymbolselection = new JComboBox[maxCurves];
        residualSymbolSelection = new JComboBox[maxCurves];
        markercolorselection = new JComboBox[maxCurves];
        spectralTypeSelection = new JComboBox[maxCurves];
        modelColorSelection = new JComboBox[maxCurves];
        residualModelColorSelection = new JComboBox[maxCurves];
        residualColorSelection = new JComboBox[maxCurves];
        inputAverageOverSizespinnermodel = new SpinnerModel[maxCurves];
        inputAverageOverSizespinner = new JSpinner[maxCurves];
        smoothlenspinnermodel = new SpinnerModel[maxCurves];
        smoothlenspinner = new JSpinner[maxCurves];
        morelegendradiopanelgroup = new JPanel[maxCurves];
        displayBinningPanel = new JPanel[maxCurves];
        outBinRms = new double[maxCurves];
        minutes = new ArrayList<>(maxCurves);
        autoscalepanelgroup = new JPanel[maxCurves];
        customscalepanelgroup = new JPanel[maxCurves];
        detrendpanelgroup = new JPanel[maxCurves];
        normpanelgroup = new JPanel[maxCurves];
        savecolumnpanelgroup = new JPanel[maxCurves];
        legendnoneButton = new JRadioButton[maxCurves];
        legendcustomNameButton = new JRadioButton[maxCurves];
        legendcolumnNameButton = new JRadioButton[maxCurves];
        detrendVarButton = new JRadioButton[maxCurves][maxDetrendVars];
        autoscaleButton = new JRadioButton[maxCurves];
        customscaleButton = new JRadioButton[maxCurves];
        morelegendRadioGroup = new ButtonGroup[maxCurves];
        detrendVarRadioGroup = new ButtonGroup[maxCurves];
        autoscaleRadioGroup = new ButtonGroup[maxCurves];
        morelegendField = new JTextField[maxCurves];
        autoshiftmodel = new SpinnerModel[maxCurves];
        autoshiftstepmodel = new SpinnerModel[maxCurves];
        autoscalemodel = new SpinnerModel[maxCurves];
        autoscalestepmodel = new SpinnerModel[maxCurves];
        customshiftmodel = new SpinnerModel[maxCurves];
        customshiftstepmodel = new SpinnerModel[maxCurves];
        customscalemodel = new SpinnerModel[maxCurves];
        customscalestepmodel = new SpinnerModel[maxCurves];
        detrendfactormodel = new SpinnerModel[maxCurves];
        detrendfactorstepmodel = new SpinnerModel[maxCurves];
        autoshiftspinner = new JSpinner[maxCurves];
        autoshiftstepspinner = new JSpinner[maxCurves];
        autoscalespinner = new JSpinner[maxCurves];
        autoscalestepspinner = new JSpinner[maxCurves];
        customshiftspinner = new JSpinner[maxCurves];
        customshiftstepspinner = new JSpinner[maxCurves];
        customscalespinner = new JSpinner[maxCurves];
        customscalestepspinner = new JSpinner[maxCurves];
        detrendfactorspinner = new JSpinner[maxCurves];
        detrendfactorstepspinner = new JSpinner[maxCurves];
        autoscalesteppopup = new JPopupMenu[maxCurves];
        autoshiftsteppopup = new JPopupMenu[maxCurves];
        customscalesteppopup = new JPopupMenu[maxCurves];
        customshiftsteppopup = new JPopupMenu[maxCurves];
        detrendfactorsteppopup = new JPopupMenu[maxCurves];
        autoscalesteppanel = new JPanel[maxCurves];
        customscalesteppanel = new JPanel[maxCurves];
        autoshiftsteppanel = new JPanel[maxCurves];
        customshiftsteppanel = new JPanel[maxCurves];
        detrendfactorsteppanel = new JPanel[maxCurves];
        autoscalesteplabel = new JLabel[maxCurves];
        autoshiftsteplabel = new JLabel[maxCurves];
        customscalesteplabel = new JLabel[maxCurves];
        customshiftsteplabel = new JLabel[maxCurves];
        detrendfactorsteplabel = new JLabel[maxCurves];
        curvelabel = new JLabel[maxCurves];
        othercurvelabel = new JLabel[maxCurves];
        grabautopanel = new JPanel[maxCurves];
        grabautobutton = new JButton[maxCurves];
        savecolumnbutton = new JButton[maxCurves];

//                customscaleeditor = new JSpinner.NumberEditor[maxCurves];
//                customscaleformat = new DecimalFormat[maxCurves];
//                customshifteditor = new JSpinner.NumberEditor[maxCurves];
//                customshiftformat = new DecimalFormat[maxCurves];
//                detrendfactoreditor = new JSpinner.NumberEditor[maxCurves];
//                detrendfactorformat = new DecimalFormat[maxCurves];


        //INITIALIZE CERTAIN ARRAYS
        defaultFitStep[0] = 0.1;
        defaultFitStep[1] = 0.1;
        defaultFitStep[2] = 1.0;
        defaultFitStep[3] = 0.01;
        defaultFitStep[4] = 1.0;
        defaultFitStep[5] = 0.1;
        defaultFitStep[6] = 0.1;
        if (defaultFitStep.length > 7) {
            for (int j = 7; j < defaultFitStep.length; j++)  //detrend1, detrend2, ...
            {
                defaultFitStep[j] = 0.1;
            }
        }

//                residualShiftStep = 0.001;
        for (int i = 0; i < maxCurves; i++) {
            xlabel[i] = "default";
            ylabel[i] = "rel_flux_" + (i == 0 ? "T" : "C") + (i + 1);
//                        errlabel[i] = "rel_flux_err_"+(i==0?"T":"C")+(i+1);
            oplabel[i] = "source-sky_" + (i == 0 ? "T" : "C") + (i + 1);
//                        operrlabel[i] = "source_error_"+(i==0?"T":"C")+(i+1);
            colorIndex[i] = (7 + i) % colors.length;  //start with blue
            modelColorIndex[i] = (7 + i) % colors.length;
            residualModelColorIndex[i] = (7 + i) % colors.length;
            residualColorIndex[i] = (7 + i) % colors.length;
            markerIndex[i] = 3;   //default to dot
            residualSymbolIndex[i] = 3;
//                        detrendFit[i] = true;
            force[i] = false;
            bpLock[i] = false;
            showErrors[i] = false;
//                        showOpErrors[i] = false;
            hasErrors[i] = false;
            hasOpErrors[i] = false;
//                        showErr[i] = false;
            useColumnName[i] = true;
            fitFrameLocationX[i] = 40 + i * 25;
            fitFrameLocationY[i] = 40 + i * 25;
            ASInclude[i] = true;
            plotY[i] = i == 0;
            sigma[i] = 0.0;
            prevSigma[i] = Double.POSITIVE_INFINITY;
            prevBic[i] = Double.NEGATIVE_INFINITY;
            autoUpdatePriors[i] = true;
            smooth[i] = false;
            smoothLen[i] = 31;
            orbitalPeriod[i] = 3.0;
            eccentricity[i] = 0.0;
            omega[i] = 0.0;
            teff[i] = 5800.0;
            spectralType[i] = "";
            planetRadius[i] = Double.NaN;
            lonAscNode[i] = 0.0;
            forceCircularOrbit[i] = true;
            useLonAscNode[i] = false;
            tolerance[i] = 1e-10;
            maxFitSteps[i] = 20000;
            modelLineWidth[i] = 1;
            residualLineWidth[i] = 1;
            residualShift[i] = 0.0;
            autoResidualShift[i] = 0.0;
            useTransitFit[i] = true;
            showLTranParams[i] = true;
            showLResidual[i] = true;
            autoUpdateFit[i] = true;
            showModel[i] = true;
            showResidual[i] = true;
            showResidualError[i] = false;
            lockToCenter[i][5] = true;
            autoUpdatePrior[i][0] = true;
            autoUpdatePrior[i][1] = true;
            autoUpdatePrior[i][2] = true;
            autoUpdatePrior[i][3] = true;
            autoUpdatePrior[i][4] = true;
            priorCenter[i][0] = 1.0;  //f0 = baseline flux
            priorCenterStep[0] = 0.001;
            priorWidth[i][0] = 0.005;
            priorWidthStep[0] = 0.001;
            usePriorWidth[i][0] = false;
            useCustomFitStep[i][0] = false;
            fitStep[i][0] = 0.1;
            fitStepStep[0] = 0.1;

            priorCenter[i][1] = 0.010;    // depth = (r_p/r_*)^2
            priorCenterStep[1] = 0.001;
            priorWidth[i][1] = 0.010;
            priorWidthStep[1] = 0.001;
            usePriorWidth[i][1] = false;
            useCustomFitStep[i][1] = false;
            fitStep[i][1] = 0.1;
            fitStepStep[1] = 0.1;

            priorCenter[i][2] = 10.0;   // a/r_*
            priorCenterStep[2] = 1.0;
            priorWidth[i][2] = 7;
            priorWidthStep[2] = 1.0;
            usePriorWidth[i][2] = false;
            useCustomFitStep[i][2] = false;
            fitStep[i][2] = 1.0;
            fitStepStep[2] = 0.1;

            priorCenter[i][3] = 2456500;  // tc = transit center time
            priorCenterStep[3] = 0.001;
            priorWidth[i][3] = 0.015;
            priorWidthStep[3] = 0.001;
            usePriorWidth[i][3] = false;
            useCustomFitStep[i][3] = false;
            fitStep[i][3] = 0.01;
            fitStepStep[3] = 0.01;

            priorCenter[i][4] = 88.0;  // inclination
            priorCenterStep[4] = 1.0;
            priorWidth[i][4] = 15;
            priorWidthStep[4] = 1;
            usePriorWidth[i][4] = false;
            useCustomFitStep[i][4] = false;
            fitStep[i][4] = 1.0;
            fitStepStep[4] = 1.0;

            priorCenter[i][5] = 0.3;  // u1
            priorCenterStep[5] = 0.1;
            priorWidth[i][5] = 1.0;
            priorWidthStep[5] = 0.1;
            usePriorWidth[i][5] = false;
            useCustomFitStep[i][5] = false;
            fitStep[i][5] = 1.0;
            fitStepStep[5] = 0.1;

            priorCenter[i][6] = 0.3;  // u2
            priorCenterStep[6] = 0.1;
            priorWidth[i][6] = 1.0;
            priorWidthStep[6] = 0.1;
            usePriorWidth[i][6] = false;
            useCustomFitStep[i][6] = false;
            fitStep[i][6] = 1.0;
            fitStepStep[6] = 0.1;

            if (priorCenter[i].length > 7) {
                for (int j = 7; j < priorCenter[i].length; j++)  //detrend1, detrend2, ...
                {
                    priorCenter[i][j] = 0.0;
                    priorCenterStep[j] = 0.000001;
                    priorWidth[i][j] = 1.0;
                    priorWidthStep[j] = 0.01;
                    usePriorWidth[i][j] = false;
                    useCustomFitStep[i][j] = false;
                    fitStep[i][j] = 0.1;
                    fitStepStep[j] = 0.1;
                }
            }


            autoScaleFactor[i] = 0.2;
            autoScaleStep[i] = 1;
            autoShiftFactor[i] = -i * 10;
            autoShiftStep[i] = 1;
            customScaleFactor[i] = 1;
            customScaleStep[i] = 0.1;
            customShiftFactor[i] = 0 - i * 0.005;
            customShiftStep[i] = 0.005;
            detrendVarDisplayed[i] = 0;
            inputAverageOverSize[i] = 1;
            baseline[i] = 0;
            legend[i] = "Legend" + (i + 1);
            normIndex[i] = 7;
            detrendFitIndex[i] = 0;
            for (int v = 0; v < maxDetrendVars; v++) {
                detrendlabel[i][v] = "";
                detrendlabelhold[i][v] = "";
                detrendFactor[i][v] = 0;
                detrendFactorStep[i][v] = 0.01;
                detrendIndex[i][v] = 0;
            }
        }
    }

    static void setupDataBuffers() {
        x = new double[maxCurves][maxColumnLength];
        detrendXs = new double[maxCurves][n];
        detrendYs = new double[maxCurves][n];
        detrendYEs = new double[maxCurves][n];
        y = new double[maxCurves][maxColumnLength];

        yerr = new double[maxCurves][maxColumnLength];
        yop = new double[maxCurves][maxColumnLength];
        yoperr = new double[maxCurves][maxColumnLength];
        detrend = new double[maxCurves][maxDetrendVars][maxColumnLength];

        xc1 = new double[maxCurves][maxColumnLength];
        xc2 = new double[maxCurves][maxColumnLength];
        yc1 = new double[maxCurves][maxColumnLength];
        yc2 = new double[maxCurves][maxColumnLength];
    }

    /**
     * Returns minimum of double array.
     */
    static double minOf(double[] arr, int n) {
        double mn = Double.POSITIVE_INFINITY;
        for (int i = 0; i < n; i++) {
            if (!Double.isNaN(arr[i])) {
                mn = Math.min(arr[i], mn);
            }
        }
        return mn;
    }

    /**
     * Returns maximum of double array.
     */
    static double maxOf(double[] arr, int n) {
        double mx = Double.NEGATIVE_INFINITY;
        for (int i = 0; i < n; i++) {
            if (!Double.isNaN(arr[i])) {
                mx = Math.max(arr[i], mx);
            }
        }
        return mx;
    }

    /**
     * Returns minimum of double array including error.
     */
    static double minOf(double[] arr, double[] err, int n) {
        double mn = Double.POSITIVE_INFINITY;
        double value;
        for (int i = 0; i < n; i++) {
            if (!Double.isNaN(arr[i]) && !Double.isNaN(err[i])) {
                value = arr[i] - err[i];
                mn = Math.min(value, mn);
            }
        }
        return mn;
    }

    /**
     * Returns maximum of double array including.
     */
    static double maxOf(double[] arr, double[] err, int n) {
        double mx = Double.NEGATIVE_INFINITY;
        double value;
        for (int i = 0; i < n; i++) {
            if (!Double.isNaN(arr[i]) && !Double.isNaN(err[i])) {
                value = arr[i] + err[i];
                mx = Math.max(value, mx);
            }
        }
        return mx;
    }

    /**
     * Returns minimum of double array.
     */
    static double resMinOf(double[] arr, int n, double shift) {
        double mn = Double.POSITIVE_INFINITY;
        double value;
        for (int i = 0; i < n; i++) {
            if (!Double.isNaN(arr[i])) {
                value = arr[i] + shift;
                mn = Math.min(value, mn);
            }
        }
        return mn;
    }

    /**
     * Returns maximum of double array.
     */
    static double resMaxOf(double[] arr, int n, double shift) {
        double mx = Double.NEGATIVE_INFINITY;
        double value;
        for (int i = 0; i < n; i++) {
            if (!Double.isNaN(arr[i])) {
                value = arr[i] + shift;
                mx = Math.max(value, mx);
            }
        }
        return mx;
    }

    /**
     * Returns minimum of double array including error.
     */
    static double resMinOf(double[] arr, double[] err, int n, double shift) {
        double mn = Double.POSITIVE_INFINITY;
        double value;
        for (int i = 0; i < n; i++) {
            if (!Double.isNaN(arr[i]) && !Double.isNaN(err[i])) {
                value = arr[i] - err[i] + shift;
                mn = Math.min(value, mn);
            }
        }
        return mn;
    }

    /**
     * Returns maximum of double array including.
     */
    static double resMaxOf(double[] arr, double[] err, int n, double shift) {
        double mx = Double.NEGATIVE_INFINITY;
        double value;
        for (int i = 0; i < n; i++) {
            if (!Double.isNaN(arr[i]) && !Double.isNaN(err[i])) {
                value = arr[i] + err[i] + shift;
                mx = Math.max(value, mx);
            }
        }
        return mx;
    }


    static Color colorOf(int ci) {
        Color cmkr;
        switch (ci) {
            case 0:
                cmkr = java.awt.Color.black;
                break;
            case 1:
                cmkr = java.awt.Color.darkGray;
                break;
            case 2:
                cmkr = java.awt.Color.gray;
                break;
            case 3:
                cmkr = java.awt.Color.lightGray;
                break;
            case 4:
                cmkr = new Color(0, 235, 0);
                break;   //green
            case 5:
                cmkr = new Color(0, 155, 0);
                break;   //dark green
            case 6:
                cmkr = new Color(84, 201, 245);
                break;   //light blue
            case 7:
                cmkr = java.awt.Color.blue;
                break;
            case 8:
                cmkr = java.awt.Color.magenta;
                break;
            case 9:
                cmkr = java.awt.Color.pink;
                break;
            case 10:
                cmkr = java.awt.Color.red;
                break;
            case 11:
                cmkr = java.awt.Color.orange;
                break;
            case 12:
                cmkr = new Color(235, 235, 0);
                break;   //yellow
            case 13:
                cmkr = new Color(167, 131, 96);
                break;   //brown
            case 14:
                cmkr = new Color(124, 0, 255);
                break;   //purple
            case 15:
                cmkr = new Color(3, 148, 163);
                break;   //teal
            default:
                cmkr = java.awt.Color.black;
                break;
        }
        return cmkr;
    }

    static int markerOf(int mi) {
        int mkr = 3;
        if (mi == 0) { mkr = ij.gui.Plot.BOX; } else if (mi == 1) { mkr = ij.gui.Plot.CIRCLE; } else if (mi == 2) {
            mkr = ij.gui.Plot.CROSS;
        } else if (mi == 3) { mkr = ij.gui.Plot.DOT; } else if (mi == 4) {
            mkr = ij.gui.Plot.LINE;
        } else if (mi == 5) {
            mkr = ij.gui.Plot.TRIANGLE;
        } else if (mi == 6) { mkr = ij.gui.Plot.X; } else mkr = ij.gui.Plot.DOT;
        return mkr;
    }


    static String scaleShiftText(boolean relative, boolean showScale, boolean showShift, boolean usemmag, boolean showmmaginfo, double scale, double shift) {
        String shiftText = "";
        String scaleText = "";
        DecimalFormat Eformatter = new DecimalFormat("0.###E0", IJU.dfs);
        DecimalFormat dformatter = new DecimalFormat("##0.###", IJU.dfs);
        if (showScale && showmmaginfo && mmag[firstCurve] && usemmag && totalScaleFactor[firstCurve] == 1000 && scale != 1000) {
            scale = 0.001 * scale;
            if (scale == 1.0) {
                scaleText += "";
            } else if (((scale > 0.001) && (scale < 1000.0)) || ((scale < -0.001) && (scale > -1000.0))) {
                scaleText += " x(" + dformatter.format(scale) + ")";
            } else { scaleText += " x(" + Eformatter.format(scale) + ")"; }
        } else if (showScale && (scale != 1.0)) {
            if (((scale > 0.001) && (scale < 1000.0)) || ((scale < -0.001) && (scale > -1000.0))) {
                scaleText += " x(" + dformatter.format(scale) + ")";
            } else if (usemmag && showmmaginfo && (scale == 1000)) { scaleText += ""; } else {
                scaleText += " x(" + Eformatter.format(scale) + ")";
            }
        } else if (relative) {
            if (!showScale && !showShift) {
                scaleText = " (arbitrarily scaled and shifted)";
            } else if (!showScale && showShift) scaleText = " (arbitrarily scaled)";
        }
        if (showShift) {
            if (shift < 0.0) {
                shift *= -1.0;
                shiftText += " - ";
            } else if (shift != 0.0) shiftText += " + ";
            if ((shift >= 0.01) && (shift < 100.0)) { shiftText += dformatter.format(shift); } else if (shift != 0.0)
                shiftText += Eformatter.format(shift);
        } else if (relative && showScale && !showShift) {
            shiftText = " (arbitrarily shifted)";
        }
        return scaleText + shiftText;
    }


    static String convertToText(double increment) {
        if (increment == 0.0000000001) { return "      0.0000000001"; } else if (increment == 0.000000001) {
            return "        0.000000001";
        } else if (increment == 0.00000001) {
            return "          0.00000001";
        } else if (increment == 0.0000001) {
            return "            0.0000001";
        } else if (increment == 0.000001) {
            return "              0.000001";
        } else if (increment == 0.00001) {
            return "                0.00001";
        } else if (increment == 0.0001) {
            return "                  0.0001";
        } else if (increment == 0.001) {
            return "                    0.001";
        } else if (increment == 0.005) {
            return "                    0.005";
        } else if (increment == 0.01) {
            return "                    0.010";
        } else if (increment == 0.1) {
            return "                    0.100";
        } else if (increment == 0.5) {
            return "                    0.500";
        } else if (increment == 1.0) {
            return "                    1.000";
        } else if (increment == 10.0) {
            return "                  10.000";
        } else if (increment == 100.0) {
            return "                100.000";
        } else if (increment == 1000.0) {
            return "              1000.000";
        } else if (increment == 10000.0) {
            return "            10000.000";
        } else if (increment == 100000.0) {
            return "          100000.000";
        } else if (increment == 1000000.0) {
            return "        1000000.000";
        } else if (increment == 10000000.0) {
            return "      10000000.000";
        } else if (increment == 100000000.0) {
            return "    100000000.000";
        } else if (increment == 1000000000.0) {
            return "1000000000.000";
        } else return "                    1.000";
    }

    static String intConvertToText(int increment) {
        if (increment == 1) { return "                    1"; } else if (increment == 2) {
            return "                    2";
        } else if (increment == 3) {
            return "                    3";
        } else if (increment == 4) {
            return "                    4";
        } else if (increment == 5) {
            return "                    5";
        } else if (increment == 6) {
            return "                    6";
        } else if (increment == 7) {
            return "                    7";
        } else if (increment == 8) {
            return "                    8";
        } else if (increment == 9) {
            return "                    9";
        } else if (increment == 10) {
            return "                  10";
        } else if (increment == 25) {
            return "                  25";
        } else if (increment == 50) {
            return "                  50";
        } else if (increment == 100) {
            return "                100";
        } else if (increment == 250) {
            return "                250";
        } else if (increment == 500) {
            return "                500";
        } else if (increment == 1000) {
            return "              1000";
        }
//                else if (increment == 10000)      return "            10000";
//                else if (increment == 100000)     return "          100000";
//                else if (increment == 1000000)    return "        1000000";
//                else if (increment == 10000000)   return "      10000000";
//                else if (increment == 100000000)  return "    100000000";
//                else if (increment == 1000000000) return "1000000000";
        else { return "                    1"; }
    }

    static void checkForUT(JSpinner spinner) {
        JSpinner.NumberEditor ed = (JSpinner.NumberEditor) spinner.getEditor();
        String text = ed.getTextField().getText();
//                IJ.log("Original Text = "+text);
        double dValue = Tools.parseDouble(text);
        if (Double.isNaN(dValue)) {
//                    IJ.log("Not a number");
            double value = 0.5;
            String[] pieces = text.replaceAll("[^0-9\\.]+", " ").trim().split("[^0-9\\.]+");
            if (pieces.length > 0 && !pieces[0].trim().equals("")) {
//                        IJ.log("pieces[0]="+pieces[0]);
                value += Tools.parseDouble(pieces[0], 0) / 24.0;
            }
            if (pieces.length > 1 && !pieces[1].trim().equals("")) {
//                        IJ.log("pieces[1]="+pieces[1]);
                value += Tools.parseDouble(pieces[1], 0) / 1440.0;
            }
            if (pieces.length > 2 && !pieces[2].trim().equals("")) {
//                        IJ.log("pieces[2]="+pieces[2]);
                value += Tools.parseDouble(pieces[2], 0) / 86400.0;
            }
//                    IJ.log("value="+value);
            value %= 1;
            if (spinner.equals(vmarker2spinner)) {
                double vm1Value = (Double) vmarker1spinner.getValue();
                if (!Double.isNaN(vm1Value) && value < vm1Value) value += (int) vm1Value + 1;
            }
//                    IJ.log("value="+value);
            if (!Double.isNaN(value)) {
                spinner.setValue(value);
            }
        }
    }


    static void keepMarkersInOrder(int marker) {
        if (marker == 1) {
            if (dMarker1Value > dMarker2Value) {
                dmarker2spinner.setValue(dMarker1Value);
                dMarker2Value = dMarker1Value;
            }
            if (dMarker1Value > dMarker3Value) {
                dmarker3spinner.setValue(dMarker1Value);
                dMarker3Value = dMarker1Value;
            }
            if (dMarker1Value > dMarker4Value) {
                dmarker4spinner.setValue(dMarker1Value);
                dMarker4Value = dMarker1Value;
            }
        }
        if (marker == 2) {
            if (dMarker2Value < dMarker1Value) {
                dmarker1spinner.setValue(dMarker2Value);
                dMarker1Value = dMarker2Value;
            }
            if (dMarker2Value > dMarker3Value) {
                dmarker3spinner.setValue(dMarker2Value);
                dMarker3Value = dMarker2Value;
            }
            if (dMarker2Value > dMarker4Value) {
                dmarker4spinner.setValue(dMarker2Value);
                dMarker4Value = dMarker2Value;
            }
        }
        if (marker == 3) {
            if (dMarker3Value < dMarker1Value) {
                dmarker1spinner.setValue(dMarker3Value);
                dMarker1Value = dMarker3Value;
            }
            if (dMarker3Value < dMarker2Value) {
                dmarker2spinner.setValue(dMarker3Value);
                dMarker2Value = dMarker3Value;
            }
            if (dMarker3Value > dMarker4Value) {
                dmarker4spinner.setValue(dMarker3Value);
                dMarker4Value = dMarker3Value;
            }
        }
        if (marker == 4) {
            if (dMarker4Value < dMarker1Value) {
                dmarker1spinner.setValue(dMarker4Value);
                dMarker1Value = dMarker4Value;
            }
            if (dMarker4Value < dMarker2Value) {
                dmarker2spinner.setValue(dMarker4Value);
                dMarker2Value = dMarker4Value;
            }
            if (dMarker4Value < dMarker3Value) {
                dmarker3spinner.setValue(dMarker4Value);
                dMarker3Value = dMarker4Value;
            }
        }
    }


    static void showMainJPanel() {
// DISPLAY MAIN PANEL
        panelsUpdating = true;
        if (mainFrame == null) {
            mainFrame = new JFrame("Multi-plot Main");
        }
        plotIcon = createImageIcon("astroj/images/plot.png", "Plot Icon");
        mainFrame.setIconImage(plotIcon.getImage());
        mainFrame.setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        mainFrame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                saveAndClose();
            }
        });

        mainpanel = new JPanel(new SpringLayout());
//                mainpanel.addMouseListener(panelMouseListener);
        mainpanel.addMouseMotionListener(panelMouseMotionListener);


        mainscrollpane = new JScrollPane(mainpanel);

        mainmenubar = new JMenuBar();
        filemenu = new JMenu("  File      ");
        preferencesmenu = new JMenu("Preferences    ");

        mainpanelgroupa = new JPanel(new SpringLayout());

        mainpanelgroupa.setBorder(dataSectionBorder);
        JPanel mainpanelgroupb = new JPanel(new SpringLayout());
        mainpanelgroupb.setBorder(BorderFactory.createTitledBorder("Data Options"));
        JPanel mainpanelgroupc = new JPanel(new SpringLayout());   //TITLE - SUBTITLE - LEGEND GROUP
        JPanel mainpanelgroupd = new JPanel(new SpringLayout());   // X & Y AXIS LEGEND GROUP
        JPanel mainpanelgroupe = new JPanel(new SpringLayout());
//                mainpanelgroupe.setBorder(BorderFactory.createTitledBorder("Scaling Options"));
        JPanel mainpanelgroupf = new JPanel(new SpringLayout());

        // BUILD MENUBAR

        opentablemenuitem = new JMenuItem("Select open table...");
        opentablemenuitem.setToolTipText("<html>" + "select a table that is already open for plotting" + "</html>");
        opentablemenuitem.addActionListener(e -> {
            selectAnotherTableCanceled = false;
            MeasurementTable newTable = selectAnotherTable("");
            if (newTable == null) {
                if (!selectAnotherTableCanceled) {
                    makeDummyTable();
                    IJ.beep();
                    IJ.showMessage("No tables open");
                }
                selectAnotherTableCanceled = false;
                return;
            }
            newTable.show();
            setTable(newTable, true);
            plotWindow.setVisible(true);
        });
        filemenu.add(opentablemenuitem);

        JMenuItem appendtablerowsmenuitem = new JMenuItem("Append open table as new rows...");
        appendtablerowsmenuitem.setToolTipText("<html>" + "select a table that is already open to append to current table" + "</html>");
        appendtablerowsmenuitem.addActionListener(e -> appendDataAsRows(false, null));
        filemenu.add(appendtablerowsmenuitem);

        JMenuItem appendtablecolumnssmenuitem = new JMenuItem("Append open table as new columns...");
        appendtablecolumnssmenuitem.setToolTipText("<html>" + "select a table that is already open to append to current table" + "</html>");
        appendtablecolumnssmenuitem.addActionListener(e -> appendDataAsColumns(false, null));
        filemenu.add(appendtablecolumnssmenuitem);
        filemenu.addSeparator();

        opendatamenuitem = new JMenuItem("Open table from file...");
        opendatamenuitem.setToolTipText("<html>" + "default input format = tab delimited, or use" + "<br>" + "filename.csv = comma delimited" + "<br>" + "filename.prn or filename.spc = space delimted" + "<br>" + "---------------------------------------------" + "<br>" + "first line should be column headings delimited as stated above" + "<br>" + "lines starting with # are considered comments and ignored, except" + "<br>" + "the last comment before the first data line can be headings" + "</html>");
        opendatamenuitem.addActionListener(e -> openData());
        filemenu.add(opendatamenuitem);

        JMenuItem appenddatarowsmenuitem = new JMenuItem("Append table from file as new rows...");
        appenddatarowsmenuitem.setToolTipText("<html>" + "default input format = tab delimited, or use" + "<br>" + "filename.csv = comma delimited" + "<br>" + "filename.prn or filename.spc = space delimted" + "<br>" + "---------------------------------------------" + "<br>" + "first line should be column headings delimited as stated above" + "<br>" + "lines starting with # are considered comments and ignored, except" + "<br>" + "the last comment before the first data line can be headings" + "</html>");
        appenddatarowsmenuitem.addActionListener(e -> appendDataAsRows(true, null));
        filemenu.add(appenddatarowsmenuitem);

        JMenuItem appenddatacolumnsmenuitem = new JMenuItem("Append table from file as new columns...");
        appenddatacolumnsmenuitem.setToolTipText("<html>" + "default input format = tab delimited, or use" + "<br>" + "filename.csv = comma delimited" + "<br>" + "filename.prn or filename.spc = space delimted" + "<br>" + "---------------------------------------------" + "<br>" + "first line should be column headings delimited as stated above" + "<br>" + "lines starting with # are considered comments and ignored, except" + "<br>" + "the last comment before the first data line can be headings" + "</html>");
        appenddatacolumnsmenuitem.addActionListener(e -> appendDataAsColumns(true, null));
        filemenu.add(appenddatacolumnsmenuitem);

        openplotconfigmenuitem = new JMenuItem("Open plot configuration from file...");
        openplotconfigmenuitem.setToolTipText("<html>" + "opens a previously saved plot configuration<br>" + "from a user selected file</html>");
        openplotconfigmenuitem.addActionListener(e -> openConfig(false));
        filemenu.add(openplotconfigmenuitem);

        opendataconfigmenuitem = new JMenuItem("Open table and plot configuration from file...");
        opendataconfigmenuitem.setToolTipText("<html>" + "opens a previously saved measurement table" + "<br>" + "from a user selected file and attempts to" + "<br>" + "open a plot configuration file with the" + "<br>" + "same name but ending in .plotcfg" + "</html>");
        opendataconfigmenuitem.addActionListener(e -> openDataAndConfig(null));
        filemenu.add(opendataconfigmenuitem);

        filemenu.addSeparator();

        savedatamenuitem = new JMenuItem("Save data to file...");
        savedatamenuitem.setToolTipText("<html>" + "saves measurement table data to a user selected file" + "</html>");
        savedatamenuitem.addActionListener(e -> saveData());
        filemenu.add(savedatamenuitem);

        JMenuItem savedatasubsetmenuitem = new JMenuItem("Save data subset to file...");
        savedatasubsetmenuitem.setToolTipText("<html>" + "saves a subset of measurement table data to a user selected file" + "</html>");
        savedatasubsetmenuitem.addActionListener(e -> saveDataSubsetDialog(null));
        filemenu.add(savedatasubsetmenuitem);


        JMenuItem saveimagepngmenuitem = new JMenuItem("Save plot image as PNG...");
        saveimagepngmenuitem.setToolTipText("<html>" + "saves plot image as a .png file" + "</html>");
        saveimagepngmenuitem.addActionListener(e -> savePlotImageAsPng());
        filemenu.add(saveimagepngmenuitem);

        JMenuItem saveimagejpgmenuitem = new JMenuItem("Save plot image as JPG...");
        saveimagejpgmenuitem.setToolTipText("<html>" + "saves plot image as a .jpg file" + "</html>");
        saveimagejpgmenuitem.addActionListener(e -> savePlotImageAsJpg());
        filemenu.add(saveimagejpgmenuitem);

        JMenuItem saveimagepdfmenuitem = new JMenuItem("Save plot image as vector PDF...");
        saveimagepdfmenuitem.setToolTipText("<html>" + "saves plot image as a .pdf file" + "</html>");
        saveimagepdfmenuitem.addActionListener(e -> savePlotImageAsVectorPdf());
        filemenu.add(saveimagepdfmenuitem);

        saveplotconfigmenuitem = new JMenuItem("Save plot configuration...");
        saveplotconfigmenuitem.setToolTipText("<html>" + "saves plot configuration to a user selected file</html>");
        saveplotconfigmenuitem.addActionListener(e -> saveConfig(false));
        filemenu.add(saveplotconfigmenuitem);

        filemenu.addSeparator();

        JMenuItem createNEBReportMenuItem = new JMenuItem("Create NEB search reports and plots...");
        createNEBReportMenuItem.setToolTipText("<html>" + "Create NEB search reports and plots.<br>");
        createNEBReportMenuItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                ij.plugin.Macro_Runner.runMacroFromJar(getClass().getClassLoader(), "Astronomy/NEBSearchMacro.txt", "");
            }
        });
        filemenu.add(createNEBReportMenuItem);

        JMenuItem createAAVSOReportMenuItem = new JMenuItem("Create AAVSO Exoplanet Database formatted data...");
        createAAVSOReportMenuItem.setToolTipText("<html>" + "Create AAVSO formatted data for submission to the AAVSO Exoplanet Database.</html>");
        createAAVSOReportMenuItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                ij.plugin.Macro_Runner.runMacroFromJar(getClass().getClassLoader(), "Astronomy/AAVSO_Exoplanet_Format.txt", "");
            }
        });
        filemenu.add(createAAVSOReportMenuItem);

        JMenuItem createAAVSOVarStarReportMenuItem = new JMenuItem("Create AAVSO Variable Star Report...");
        createAAVSOVarStarReportMenuItem.setToolTipText("<html>" + "Create AAVSO formatted data for submission to the AAVSO Variable Star Database.</html>");
        createAAVSOVarStarReportMenuItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                ij.plugin.Macro_Runner.runMacroFromJar(getClass().getClassLoader(), "Astronomy/AAVSO_Variable_Star_Report_Macro.txt", "");
            }
        });
        filemenu.add(createAAVSOVarStarReportMenuItem);

//                JMenuItem createDmagVsRMSPlotMenuItem = new JMenuItem("Create Delta-magnitude vs. RMS plot...");
//                createDmagVsRMSPlotMenuItem.setToolTipText("<html>"+"Create a Delta-magnitude vs. RMS plot for all apertures.<br>");
//                createDmagVsRMSPlotMenuItem.addActionListener(new ActionListener() {
//                    public void actionPerformed(ActionEvent e) {
//                            ij.plugin.Macro_Runner.runMacroFromJar("DmagVsRMSplotMacro.txt",""); }});
//                filemenu.add(createDmagVsRMSPlotMenuItem);
//
//                JMenuItem createNEBLCPlotMenuItem = new JMenuItem("Create NEB light curve plots...");
//                createNEBLCPlotMenuItem.setToolTipText("<html>"+"Create NEB light curve plots with the predicted depth overplotted....<br>");
//                createNEBLCPlotMenuItem.addActionListener(new ActionListener() {
//                    public void actionPerformed(ActionEvent e) {
//                            ij.plugin.Macro_Runner.runMacroFromJar("NEBLightCurvePlotWithPredDepth.txt",""); }});
//                filemenu.add(createNEBLCPlotMenuItem);

        JMenuItem createMpcFormatMenuItem = new JMenuItem("Create Minor Planet Center (MPC) formatted data...");
        createMpcFormatMenuItem.setToolTipText("<html>" + "Create MPC formatted data for submission to the Minor Planet Center.<br>" + "Open a table into Multi-plot before creating the MPC formatted data.</html>");
        createMpcFormatMenuItem.addActionListener(e -> createMpcFormatDialog());
        filemenu.add(createMpcFormatMenuItem);

        filemenu.addSeparator();

        openplottemplatemenuitem = new JMenuItem("Open plot configuration template...");
        openplottemplatemenuitem.setToolTipText("<html>" + "opens a plot configuration from a user selected template file path</html>");
        openplottemplatemenuitem.addActionListener(e -> openConfig(true));
        filemenu.add(openplottemplatemenuitem);

        saveplottemplatemenuitem = new JMenuItem("Save plot configuration template...");
        saveplottemplatemenuitem.setToolTipText("<html>" + "saves plot configuration to user selected template file path</html>");
        saveplottemplatemenuitem.addActionListener(e -> saveConfig(true));
        filemenu.add(saveplottemplatemenuitem);

        filemenu.addSeparator();

        JMenuItem saveAllMenuItem = new JMenuItem("Save all...");
        saveAllMenuItem.addActionListener(e -> {
            if (saveAllPNG) { saveAll("png", true); } else saveAll("jpg", true);
        });
        filemenu.add(saveAllMenuItem);

        JMenuItem saveAllWithOptionsMenuItem = new JMenuItem("Save all (with options)...");
        saveAllWithOptionsMenuItem.addActionListener(e -> saveAllDialog());
        filemenu.add(saveAllWithOptionsMenuItem);

        filemenu.addSeparator();

        backupAllAIJPrefsMenuItem = new JMenuItem("Save all AIJ preferences to backup file...");
        backupAllAIJPrefsMenuItem.addActionListener(e -> {
            savePreferences();
            IJU.backupAllAIJSettings(false);
        });
        filemenu.add(backupAllAIJPrefsMenuItem);

        restoreAllAIJPrefsMenuItem = new JMenuItem("Restore all AIJ preferences from backup file...");
        restoreAllAIJPrefsMenuItem.addActionListener(e -> IJU.restoreAllAIJSettings());
        filemenu.add(restoreAllAIJPrefsMenuItem);

        restoreDefaultAIJPrefsMenuItem = new JMenuItem("Restore all default AIJ preferences...");
        restoreDefaultAIJPrefsMenuItem.addActionListener(e -> IJU.restoreDefaultAIJSettings(false));
        filemenu.add(restoreDefaultAIJPrefsMenuItem);

        filemenu.addSeparator();

        JMenuItem exitmenuitem = new JMenuItem("Exit");
        exitmenuitem.addActionListener(e -> saveAndClose());
        filemenu.add(exitmenuitem);

        mainmenubar.add(filemenu);

        opendatasetCB = new JCheckBoxMenuItem("Open Y-data columns window at startup", openDataSetWindow);
        opendatasetCB.addItemListener(e -> {
            if (e.getStateChange() == ItemEvent.DESELECTED) {
                openDataSetWindow = false;
            } else if (e.getStateChange() == ItemEvent.SELECTED) openDataSetWindow = true;
            Prefs.set("plot2.openDataSetWindow", openDataSetWindow);
        });
        preferencesmenu.add(opendatasetCB);

        usewidedataCB = new JCheckBoxMenuItem("Use --wide-- Y-data columns window", useWideDataPanel);
        usewidedataCB.addItemListener(e -> {
            if (e.getStateChange() == ItemEvent.DESELECTED) {
                useWideDataPanel = false;
            } else if (e.getStateChange() == ItemEvent.SELECTED) useWideDataPanel = true;
            Prefs.set("plot2.useWideDataPanel", useWideDataPanel);
            savePreferences();
            subframeWasShowing = false;
            closeFitFrames();
            if (subFrame.isVisible()) {
                subFrameLocationX = subFrame.getLocation().x;
                subFrameLocationY = subFrame.getLocation().y;
                subFrameWidth = 0;
                subFrameHeight = 0;
                Prefs.set("plot2.subFrameLocationX", subFrameLocationX);
                Prefs.set("plot2.subFrameLocationY", subFrameLocationY);
                subFrame.dispose();
                subframeWasShowing = true;
            }
            if (subframeWasShowing) {
                showMoreCurvesJPanel();
            }
            updatePlot(updateAllFits());
        });

        preferencesmenu.add(usewidedataCB);

        preferencesmenu.addSeparator();

        openrefstarCB = new JCheckBoxMenuItem("Open reference star selection window at startup", openRefStarWindow);
        openrefstarCB.addItemListener(e -> {
            if (e.getStateChange() == ItemEvent.DESELECTED) {
                openRefStarWindow = false;
            } else if (e.getStateChange() == ItemEvent.SELECTED) openRefStarWindow = true;
            Prefs.set("plot2.openRefStarWindow", openRefStarWindow);
        });
        preferencesmenu.add(openrefstarCB);   //openFitPanelsCB

        setrefstarhorzsizemenuitem = new JMenuItem("Set horizontal width of reference star window...");
        setrefstarhorzsizemenuitem.addActionListener(e -> changeRefStarHorizontalWidth());
        preferencesmenu.add(setrefstarhorzsizemenuitem);

        openFitPanelsCB = new JCheckBoxMenuItem("Open light curve fit panels at startup", openFitPanels);
        openFitPanelsCB.addItemListener(e -> {
            if (e.getStateChange() == ItemEvent.DESELECTED) {
                openFitPanels = false;
            } else if (e.getStateChange() == ItemEvent.SELECTED) openFitPanels = true;
            Prefs.set("plot2.openFitPanels", openFitPanels);
        });
        preferencesmenu.add(openFitPanelsCB);   //openFitPanelsCB

        preferencesmenu.addSeparator();

        rememberwindowlocationsCB = new JCheckBoxMenuItem("Open windows at previous locations", rememberWindowLocations);
        rememberwindowlocationsCB.addItemListener(e -> {
            if (e.getStateChange() == ItemEvent.DESELECTED) {
                rememberWindowLocations = false;
            } else if (e.getStateChange() == ItemEvent.SELECTED) rememberWindowLocations = true;
            Prefs.set("plot2.rememberWindowLocations", rememberWindowLocations);
        });
        preferencesmenu.add(rememberwindowlocationsCB);

        keepSeparateLocationsForFitWindowsCB = new JCheckBoxMenuItem("Keep separate locations for each light curve fitting window", keepSeparateLocationsForFitWindows);
        keepSeparateLocationsForFitWindowsCB.addItemListener(e -> {
            if (e.getStateChange() == ItemEvent.DESELECTED) {
                keepSeparateLocationsForFitWindows = false;
            } else if (e.getStateChange() == ItemEvent.SELECTED) keepSeparateLocationsForFitWindows = true;
            Prefs.set("plot2.keepSeparateLocationsForFitWindows", keepSeparateLocationsForFitWindows);
        });
        preferencesmenu.add(keepSeparateLocationsForFitWindowsCB);

        preferencesmenu.addSeparator();

        divideNotSubtractCB = new JCheckBoxMenuItem("Detrend by division (deselect for faster subtraction)", divideNotSubtract);
        divideNotSubtractCB.addItemListener(e -> {
            if (e.getStateChange() == ItemEvent.DESELECTED) {
                divideNotSubtract = false;
            } else if (e.getStateChange() == ItemEvent.SELECTED) divideNotSubtract = true;
            Prefs.set("plot.divideNotSubtract", divideNotSubtract);
            updatePlot(updateAllFits());
        });
        preferencesmenu.add(divideNotSubtractCB);

        useNelderMeadChi2ForDetrendCB = new JCheckBoxMenuItem("Detrend by NelderMead Chi^2 minimization (deselect for Regression)", useNelderMeadChi2ForDetrend);
        useNelderMeadChi2ForDetrendCB.addItemListener(e -> {
            if (e.getStateChange() == ItemEvent.DESELECTED) {
                useNelderMeadChi2ForDetrend = false;
            } else if (e.getStateChange() == ItemEvent.SELECTED) {
                useNelderMeadChi2ForDetrend = true;
            }
            Prefs.set("plot.useNelderMeadChi2ForDetrend", useNelderMeadChi2ForDetrend);
            updatePlot(updateAllFits());
        });
        preferencesmenu.add(useNelderMeadChi2ForDetrendCB);

        preferencesmenu.addSeparator();

        showtooltipsCB = new JCheckBoxMenuItem("Show tooltips help", showToolTips);
        showtooltipsCB.addItemListener(e -> {
            if (e.getStateChange() == ItemEvent.DESELECTED) {
                showToolTips = false;
            } else if (e.getStateChange() == ItemEvent.SELECTED) {
                showToolTips = true;
            }
            ToolTipManager.sharedInstance().setEnabled(showToolTips);
            Prefs.set("astroIJ.showToolTips", showToolTips);
        });
        preferencesmenu.add(showtooltipsCB);

        preferencesmenu.addSeparator();

        useBoldedDatumCB = new JCheckBoxMenuItem("Bold data point nearest mouse while holding shift", useBoldedDatum);
        useBoldedDatumCB.addItemListener(e -> {
            if (e.getStateChange() == ItemEvent.DESELECTED) {
                useBoldedDatum = false;
            } else if (e.getStateChange() == ItemEvent.SELECTED) useBoldedDatum = true;
            Prefs.set("plot2.useBoldedDatum", useBoldedDatum);
            updatePlot(updateNoFits());
        });
        preferencesmenu.add(useBoldedDatumCB);

        useUpdateStackCB = new JCheckBoxMenuItem("Display slice corresponding to point nearest mouse while holding shift", useUpdateStack);
        useUpdateStackCB.addItemListener(e -> {
            if (e.getStateChange() == ItemEvent.DESELECTED) {
                useUpdateStack = false;
            } else if (e.getStateChange() == ItemEvent.SELECTED) useUpdateStack = true;
            Prefs.set("plot2.useUpdateStack", useUpdateStack);
            updatePlot(updateNoFits());
        });
        preferencesmenu.add(useUpdateStackCB);

        preferencesmenu.addSeparator();

//                setverticalmarkertextmenuitem = new JMenuItem("Change vertical marker text...");
//                setverticalmarkertextmenuitem.addActionListener(new ActionListener() {
//                    public void actionPerformed(ActionEvent e) {
//                            setVMarkerText();
//                            updatePlot();}});
//                preferencesmenu.add(setverticalmarkertextmenuitem);

        usePriorityColumnsCB = new JCheckBoxMenuItem("Show specified priority data names first in pulldown lists", prioritizeColumns);
        usePriorityColumnsCB.addItemListener(e -> {
            if (e.getStateChange() == ItemEvent.DESELECTED) {
                prioritizeColumns = false;
            } else if (e.getStateChange() == ItemEvent.SELECTED) prioritizeColumns = true;
            Prefs.set("plot.prioritizeColumns", prioritizeColumns);
            oldUnfilteredColumns = null;
            updatePlot(updateAllFits());
        });
        preferencesmenu.add(usePriorityColumnsCB);

        changePriorityColumnsmenuitem = new JMenuItem("Change priority data names list...");
        changePriorityColumnsmenuitem.addActionListener(e -> changePriorityColumns());
        preferencesmenu.add(changePriorityColumnsmenuitem);

        preferencesmenu.addSeparator();

        usepixelscaleCB = new JCheckBoxMenuItem("Use pixel scale when calculating distance", usePixelScale);
        usepixelscaleCB.addItemListener(e -> {
            if (e.getStateChange() == ItemEvent.DESELECTED) {
                usePixelScale = false;
            } else if (e.getStateChange() == ItemEvent.SELECTED) usePixelScale = true;
            Prefs.set("plot.usePixelScale", usePixelScale);
            updatePlot(updateNoFits());
        });
        preferencesmenu.add(usepixelscaleCB);

        changepixelscalemenuitem = new JMenuItem("Set pixel scale (" + pixelScale + ")...");
        changepixelscalemenuitem.addActionListener(e -> {
            changePixelScale();
            updatePlot(updateNoFits());
        });
        preferencesmenu.add(changepixelscalemenuitem);

        preferencesmenu.addSeparator();

        changemaxdatalengthmenuitem = new JMenuItem("Set minimum data column buffer length...");
        changemaxdatalengthmenuitem.addActionListener(e -> {
            changeMaxDataLength();
            updatePlot(updateAllFits());
        });
        preferencesmenu.add(changemaxdatalengthmenuitem);

        usedefaultsettingsCB = new JCheckBoxMenuItem("Reset preferences to default settings (restart required)", useDefaultSettings);
        usedefaultsettingsCB.addItemListener(e -> {
            if (e.getStateChange() == ItemEvent.DESELECTED) {
                useDefaultSettings = false;
            } else if (e.getStateChange() == ItemEvent.SELECTED) useDefaultSettings = true;
            Prefs.set("plot2.useDefaultSettings", useDefaultSettings);
        });
        preferencesmenu.add(usedefaultsettingsCB);

        mainmenubar.add(preferencesmenu);


        tablemenu = new JMenu("Table    ");

        addnewcolsfromplotmenuitem = new JMenuItem("Add new data columns to table from plot...");
        addnewcolsfromplotmenuitem.setToolTipText("<html>" + "Add new data columns to the current active table from<br>" + "an enabled 'Multi-plot Y-data' plot row.</html>");
        addnewcolsfromplotmenuitem.addActionListener(e -> {
            int c = selectCurve();
            if (c < 0) return;
            addNewColumn(c, true);
            updatePlot(updateAllFits());
        });
        tablemenu.add(addnewcolsfromplotmenuitem);

        addnewcolsfromastroCCmenuitem = new JMenuItem("Add new astronomical data columns to table...");
        addnewcolsfromastroCCmenuitem.setToolTipText("<html>" + "Add new data columns to the current active table from AstroCC<br>" + "Cordinate Converter (airmass, alternate time formats, etc).</html>");
        addnewcolsfromastroCCmenuitem.addActionListener(e -> {
            if (addAstroDataFrame != null) {
                addAstroDataFrameWasShowing = false;
                closeAddAstroDataFrame();
                if (acc != null) acc.saveAndClose();
            }
            addNewAstroData();
        });
        tablemenu.add(addnewcolsfromastroCCmenuitem);

        tablemenu.addSeparator();

        transposetablemenuitem = new JMenuItem("Transpose currently selected table");
        transposetablemenuitem.setToolTipText("<html>" + "swap rows and columns in current active table" + "</html>");
        transposetablemenuitem.addActionListener(e -> transposeTable());
        tablemenu.add(transposetablemenuitem);

        mainmenubar.add(tablemenu);


        xaxismenu = new JMenu("X-axis    ");

        xTicsCB = new JCheckBoxMenuItem("Show X-axis tick marks", xTics);
        xTicsCB.addItemListener(e -> {
            if (e.getStateChange() == ItemEvent.DESELECTED) {
                xTics = false;
            } else if (e.getStateChange() == ItemEvent.SELECTED) xTics = true;
            Prefs.set("plot.xTics", xTics);
            updatePlot(updateNoFits());
        });
        xaxismenu.add(xTicsCB);

        xGridCB = new JCheckBoxMenuItem("Show X-axis grid lines", xGrid);
        xGridCB.addItemListener(e -> {
            if (e.getStateChange() == ItemEvent.DESELECTED) {
                xGrid = false;
            } else if (e.getStateChange() == ItemEvent.SELECTED) xGrid = true;
            Prefs.set("plot.xGrid", xGrid);
            updatePlot(updateNoFits());
        });
        xaxismenu.add(xGridCB);

        xNumbersCB = new JCheckBoxMenuItem("Show X-axis numbers", xNumbers);
        xNumbersCB.addItemListener(e -> {
            if (e.getStateChange() == ItemEvent.DESELECTED) {
                xNumbers = false;
            } else if (e.getStateChange() == ItemEvent.SELECTED) xNumbers = true;
            Prefs.set("plot.xNumbers", xNumbers);
            updatePlot(updateNoFits());
        });
        xaxismenu.add(xNumbersCB);

//                xaxismenu.addSeparator();
//
//                if (showXAxisNormal)
//                    {
//                    showXAxisAsPhase = false;
//                    showXAxisAsDaysSinceTc = false;
//                    showXAxisAsHoursSinceTc = false;
//                    }
//                else if (showXAxisAsPhase)
//                    {
//                    showXAxisAsDaysSinceTc = false;
//                    showXAxisAsHoursSinceTc = false;
//                    }
//                else if (showXAxisAsDaysSinceTc)
//                    {
//                    showXAxisAsHoursSinceTc = false;
//                    }
//                else if (!showXAxisAsHoursSinceTc)
//                    {
//                    showXAxisNormal = true;
//                    }
//                Prefs.set("plot.showXAxisNormal",showXAxisNormal);
//                Prefs.set("plot.showXAxisAsPhase",showXAxisAsPhase);
//                Prefs.set("plot.showXAxisAsDaysSinceTc",showXAxisAsDaysSinceTc);
//                Prefs.set("plot.showXAxisAsHoursSinceTc",showXAxisAsHoursSinceTc);
//
//                showXAxisNormalCB = new JRadioButtonMenuItem("Show x-axis unphased", showXAxisNormal);
//                showXAxisNormalCB.addActionListener(new ActionListener(){
//                        public void actionPerformed(ActionEvent ae) {
//                        showXAxisNormal = true;
//                        showXAxisAsPhase = false;
//                        showXAxisAsDaysSinceTc = false;
//                        showXAxisAsHoursSinceTc = false;
//                        Prefs.set("plot.showXAxisNormal",showXAxisNormal);
//                        Prefs.set("plot.showXAxisAsPhase",showXAxisAsPhase);
//                        Prefs.set("plot.showXAxisAsDaysSinceTc",showXAxisAsDaysSinceTc);
//                        Prefs.set("plot.showXAxisAsHoursSinceTc",showXAxisAsHoursSinceTc);
//                        updatePlot(updateAllFits());}});
//                xaxismenu.add(showXAxisNormalCB);
//
//                showXAxisAsPhaseCB = new JRadioButtonMenuItem("Show x-axis as orbital phase", showXAxisAsPhase);
//                showXAxisAsPhaseCB.addActionListener(new ActionListener(){
//                        public void actionPerformed(ActionEvent ae) {
//                        showXAxisNormal = false;
//                        showXAxisAsPhase = true;
//                        showXAxisAsDaysSinceTc = false;
//                        showXAxisAsHoursSinceTc = false;
//                        Prefs.set("plot.showXAxisNormal",showXAxisNormal);
//                        Prefs.set("plot.showXAxisAsPhase",showXAxisAsPhase);
//                        Prefs.set("plot.showXAxisAsDaysSinceTc",showXAxisAsDaysSinceTc);
//                        Prefs.set("plot.showXAxisAsHoursSinceTc",showXAxisAsHoursSinceTc);
//                        updatePlot(updateAllFits());}});
//                xaxismenu.add(showXAxisAsPhaseCB);
//
//                showXAxisAsDaysSinceTcCB = new JRadioButtonMenuItem("Show x-axis as days since Tc", showXAxisAsDaysSinceTc);
//                showXAxisAsDaysSinceTcCB.addActionListener(new ActionListener(){
//                        public void actionPerformed(ActionEvent ae) {
//                        showXAxisNormal = false;
//                        showXAxisAsPhase = false;
//                        showXAxisAsDaysSinceTc = true;
//                        showXAxisAsHoursSinceTc = false;
//                        Prefs.set("plot.showXAxisNormal",showXAxisNormal);
//                        Prefs.set("plot.showXAxisAsPhase",showXAxisAsPhase);
//                        Prefs.set("plot.showXAxisAsDaysSinceTc",showXAxisAsDaysSinceTc);
//                        Prefs.set("plot.showXAxisAsHoursSinceTc",showXAxisAsHoursSinceTc);
//                        updatePlot(updateAllFits());}});
//                xaxismenu.add(showXAxisAsDaysSinceTcCB);
//
//                showXAxisAsHoursSinceTcCB = new JRadioButtonMenuItem("Show x-axis as hours since Tc", showXAxisAsHoursSinceTc);
//                showXAxisAsHoursSinceTcCB.addActionListener(new ActionListener(){
//                        public void actionPerformed(ActionEvent ae) {
//                        showXAxisNormal = false;
//                        showXAxisAsPhase = false;
//                        showXAxisAsDaysSinceTc = false;
//                        showXAxisAsHoursSinceTc = true;
//                        Prefs.set("plot.showXAxisNormal",showXAxisNormal);
//                        Prefs.set("plot.showXAxisAsPhase",showXAxisAsPhase);
//                        Prefs.set("plot.showXAxisAsDaysSinceTc",showXAxisAsDaysSinceTc);
//                        Prefs.set("plot.showXAxisAsHoursSinceTc",showXAxisAsHoursSinceTc);
//                        updatePlot(updateAllFits());}});
//                xaxismenu.add(showXAxisAsHoursSinceTcCB);
//
//                xPhaseGroup = new ButtonGroup();
//                xPhaseGroup.add(showXAxisNormalCB);
//                xPhaseGroup.add(showXAxisAsPhaseCB);
//                xPhaseGroup.add(showXAxisAsDaysSinceTcCB);
//                xPhaseGroup.add(showXAxisAsHoursSinceTcCB);
//
//                xaxismenu.addSeparator();
//
//                setephemerismenuitem = new JMenuItem("Set epoch and period for X-axis phase calculation...");
//                setephemerismenuitem.addActionListener(new ActionListener() {
//                    public void actionPerformed(ActionEvent e) {
//                            setEphemeris();
//                            updatePlot(updateAllFits());
//                            }});
//                xaxismenu.add(setephemerismenuitem);

//                xaxismenu.addSeparator();

        mainmenubar.add(xaxismenu);


        yaxismenu = new JMenu("Y-axis    ");

        yTicsCB = new JCheckBoxMenuItem("Show Y-axis tick marks", yTics);
        yTicsCB.addItemListener(e -> {
            if (e.getStateChange() == ItemEvent.DESELECTED) {
                yTics = false;
            } else if (e.getStateChange() == ItemEvent.SELECTED) yTics = true;
            Prefs.set("plot.yTics", yTics);
            updatePlot(updateNoFits());
        });
        yaxismenu.add(yTicsCB);

        yGridCB = new JCheckBoxMenuItem("Show Y-axis grid lines", yGrid);
        yGridCB.addItemListener(e -> {
            if (e.getStateChange() == ItemEvent.DESELECTED) {
                yGrid = false;
            } else if (e.getStateChange() == ItemEvent.SELECTED) yGrid = true;
            Prefs.set("plot.yGrid", yGrid);
            updatePlot(updateNoFits());
        });
        yaxismenu.add(yGridCB);

        yNumbersCB = new JCheckBoxMenuItem("Show Y-axis numbers", yNumbers);
        yNumbersCB.addItemListener(e -> {
            if (e.getStateChange() == ItemEvent.DESELECTED) {
                yNumbers = false;
            } else if (e.getStateChange() == ItemEvent.SELECTED) yNumbers = true;
            Prefs.set("plot.yNumbers", yNumbers);
            updatePlot(updateNoFits());
        });
        yaxismenu.add(yNumbersCB);

        yaxismenu.addSeparator();

        JCheckBoxMenuItem invertyaxisCB = new JCheckBoxMenuItem("Invert Y-axis", invertYAxis);
        invertyaxisCB.addItemListener(e -> {
            if (e.getStateChange() == ItemEvent.DESELECTED) {
                invertYAxis = false;
                invertYAxisSign = 1;
            } else if (e.getStateChange() == ItemEvent.SELECTED) {
                invertYAxis = true;
                invertYAxisSign = -1;
            }
            for (int c = 0; c < maxCurves; c++) {
                customshiftspinner[c].setModel(new SpinnerNumberModel(force[c] ? autoShiftFactor[c] * 100 : customShiftFactor[c], null, null, force[c] ? invertYAxisSign * autoShiftStep[c] : invertYAxisSign * customShiftStep[c]));
                customshiftspinner[c].setEditor(new JSpinner.NumberEditor(customshiftspinner[c], "########0.#########"));
                ((JSpinner.DefaultEditor) customshiftspinner[c].getEditor()).getTextField().addMouseListener(shiftSpinnerMouseListener);
                residualShiftSpinner[c].setModel(new SpinnerNumberModel(force[c] ? autoResidualShift[c] * 100 : residualShift[c], null, null, force[c] ? invertYAxisSign * autoShiftStep[c] : invertYAxisSign * customShiftStep[c]));
                residualShiftSpinner[c].setEditor(new JSpinner.NumberEditor(residualShiftSpinner[c], fitFormat));
            }
            Prefs.set("plot.invertYAxis", invertYAxis);
            updatePlot(updateNoFits());
        });
        yaxismenu.add(invertyaxisCB);

        JCheckBoxMenuItem negateMagCB = new JCheckBoxMenuItem("Negate relative magnitude calculations", negateMag);
        negateMagCB.addItemListener(e -> {
            if (e.getStateChange() == ItemEvent.DESELECTED) {
                negateMag = false;
            } else if (e.getStateChange() == ItemEvent.SELECTED) negateMag = true;
            Prefs.set("plot.negateMag", negateMag);
            updatePlot(updateNoFits());
        });
        yaxismenu.add(negateMagCB);

//                preferencesmenu.addSeparator();
        mainmenubar.add(yaxismenu);

        JMenu helpmenu = new JMenu("Help");
        JMenuItem helpmenuitem = new JMenuItem("General help...");
        helpmenuitem.addActionListener(e -> openHelpPanel());
        helpmenu.add(helpmenuitem);

        JMenuItem helpdatamenuitem = new JMenuItem("Data naming convention...");
        helpdatamenuitem.addActionListener(e -> openDataHelpPanel());
        helpmenu.add(helpdatamenuitem);

        mainmenubar.add(helpmenu);


        // MAIN PANEL GROUP A START
        xcolumnlabel = new JLabel("Default X-data");
        xcolumnlabel.setFont(p11);
        xcolumnlabel.setHorizontalAlignment(JLabel.CENTER);
        mainpanelgroupa.add(xcolumnlabel);

//                ycolumnslabel = new JLabel ("Y-data Panel");
//                ycolumnslabel.setHorizontalAlignment(JLabel.CENTER);
//                mainpanelgroupa.add (ycolumnslabel);

        JLabel dummy50label = new JLabel("Y-datasets");
        dummy50label.setFont(p11);
        dummy50label.setHorizontalAlignment(JLabel.CENTER);
        mainpanelgroupa.add(dummy50label);

        JLabel dummy50Alabel = new JLabel("Detrend Vars");
        dummy50Alabel.setFont(p11);
        dummy50Alabel.setHorizontalAlignment(JLabel.CENTER);
        mainpanelgroupa.add(dummy50Alabel);

        JLabel mmagsampleslabel = new JLabel("Rel. Mag. Reference");
        mmagsampleslabel.setFont(p11);
        mmagsampleslabel.setHorizontalAlignment(JLabel.CENTER);
        mmagsampleslabel.setToolTipText("The number of starting samples averaged to calculate relative magnitude reference level");
        mainpanelgroupa.add(mmagsampleslabel);


        JPanel vmarker1titlepanel = new JPanel();
        vmarker1titlepanel.setLayout(new BoxLayout(vmarker1titlepanel, BoxLayout.LINE_AXIS));

        vmarker1titlepanel.add(Box.createGlue());
        vmarker1titlepanel.add(Box.createHorizontalStrut(5));

        JLabel vmarker1toplabel = new JLabel("V. Marker 1");
        vmarker1toplabel.setFont(p11);
        vmarker1toplabel.setHorizontalAlignment(JLabel.CENTER);
        vmarker1toplabel.setToolTipText("Enter vertical marker 1 x-axis location");
        vmarker1titlepanel.add(vmarker1toplabel);

        vmarker1titlepanel.add(Box.createHorizontalStrut(5));

        editTextIcon = createImageIcon("astroj/images/edittext.png", "Edit vertical marker text");
        vmarker1edittextButton = new JButton(editTextIcon);
        vmarker1edittextButton.setToolTipText("Edit vertical marker text");
        vmarker1edittextButton.setMargin(new Insets(0, 0, 0, 0));

        vmarker1edittextButton.addActionListener(ae -> {
            setVMarkerText();
            updatePlot(updateNoFits());
        });
        vmarker1titlepanel.add(vmarker1edittextButton);
        vmarker1titlepanel.add(Box.createGlue());
        mainpanelgroupa.add(vmarker1titlepanel);


        JLabel copyDetrendLabel = new JLabel("Copy");
        copyDetrendLabel.setFont(p11);
        copyDetrendLabel.setToolTipText("Copy values from 'Fit and Normalize Region Selection' Left and Right Markers to V. Markers 1 and 2");
        copyDetrendLabel.setHorizontalAlignment(JLabel.CENTER);
        mainpanelgroupa.add(copyDetrendLabel);


        JPanel vmarker2titlepanel = new JPanel();
        vmarker2titlepanel.setLayout(new BoxLayout(vmarker2titlepanel, BoxLayout.LINE_AXIS));

        vmarker2titlepanel.add(Box.createGlue());
        vmarker2titlepanel.add(Box.createHorizontalStrut(5));

        JLabel vmarker2toplabel = new JLabel("V. Marker 2");
        vmarker2toplabel.setFont(p11);
        vmarker2toplabel.setHorizontalAlignment(JLabel.CENTER);
        vmarker2toplabel.setToolTipText("Enter vertical marker 2 x-axis location");
        vmarker2titlepanel.add(vmarker2toplabel);

        vmarker2titlepanel.add(Box.createHorizontalStrut(5));

        vmarker2edittextButton = new JButton(editTextIcon);
        vmarker2edittextButton.setToolTipText("Edit vertical marker text");
        vmarker2edittextButton.setMargin(new Insets(0, 0, 0, 0));

        vmarker2edittextButton.addActionListener(ae -> {
            setVMarkerText();
            updatePlot(updateNoFits());
        });
        vmarker2titlepanel.add(vmarker2edittextButton);
        vmarker2titlepanel.add(Box.createGlue());
        mainpanelgroupa.add(vmarker2titlepanel);

        JPanel xdatacolumnpanel = new JPanel(new SpringLayout());
        MutableComboBoxModel<Object> xdatadefaultmodel = new DefaultComboBoxModel<>(columns);
        xdatacolumndefault = new JComboBox<>(xdatadefaultmodel);
        xdatacolumndefault.setFont(p11);
        xdatacolumndefault.setSelectedItem(xlabeldefault);
        xdatacolumndefault.setPrototypeDisplayValue("123456789012345");
        xdatacolumndefault.addActionListener(ae -> {
            xlabeldefault = (String) xdatacolumndefault.getSelectedItem();
            updatePlot(updateAllFits());
        });

        xdatacolumnpanel.add(xdatacolumndefault);
        xdatacolumnpanel.setPreferredSize(new Dimension(125, 25));
        SpringUtil.makeCompactGrid(xdatacolumnpanel, 1, 1, 0, 0, 0, 0);
        mainpanelgroupa.add(xdatacolumnpanel);

        JPanel maxcurvespanel = new JPanel(new SpringLayout());
        maxcurvespanel.setBorder(BorderFactory.createLineBorder(Color.lightGray, 1));
        maxcurvesmodel = new SpinnerNumberModel(maxCurves, 1, null, 1);
        maxcurvesspinner = new JSpinner(maxcurvesmodel);
        maxcurvesspinner.setFont(p11);
        maxcurvesspinner.setPreferredSize(new Dimension(60, 25));
        maxcurvesspinner.addChangeListener(ev -> {
            savePreferences();
            maxCurves = (Integer) maxcurvesspinner.getValue();
            Prefs.set("plot.maxCurves", maxCurves);
            subframeWasShowing = false;
            closeFitFrames();
            if (subFrame.isVisible()) {
                subFrameLocationX = subFrame.getLocation().x;
                subFrameLocationY = subFrame.getLocation().y;
                subFrameWidth = 0;
                subFrameHeight = 0;
                Prefs.set("plot2.subFrameLocationX", subFrameLocationX);
                Prefs.set("plot2.subFrameLocationY", subFrameLocationY);
                subFrame.dispose();
                subframeWasShowing = true;
            }
            setupArrays();
            getPreferences();
            if (subframeWasShowing) {
                subscrollpane.removeAll();
                subFrame.remove(subscrollpane);
                repaintFrame(subFrame);
                showMoreCurvesJPanel();
            }
            updatePlot(updateAllFits());
        });
        maxcurvesspinner.addMouseWheelListener(e -> {
            int newValue = (Integer) maxcurvesspinner.getValue() - e.getWheelRotation();
            if (newValue > 0) { maxcurvesspinner.setValue(newValue); } else maxcurvesspinner.setValue(1);
        });
        maxcurvespanel.add(maxcurvesspinner);

        JLabel maxcurveslabel2 = new JLabel("sets");
        maxcurveslabel2.setFont(p11);
        maxcurveslabel2.setHorizontalAlignment(JLabel.LEFT);
        maxcurvespanel.add(maxcurveslabel2);
        SpringUtil.makeCompactGrid(maxcurvespanel, 1, 2, 0, 0, 0, 0);
        mainpanelgroupa.add(maxcurvespanel);

        JPanel maxdetrendvarspanel = new JPanel(new SpringLayout());
//                maxdetrendvarspanel.setBorder(BorderFactory.createLineBorder(Color.lightGray,1));
        maxdetrendvarsmodel = new SpinnerNumberModel(maxDetrendVars, 1, null, 1);
        maxdetrendvarsspinner = new JSpinner(maxdetrendvarsmodel);
        maxdetrendvarsspinner.setFont(p11);
        maxdetrendvarsspinner.setPreferredSize(new Dimension(60, 25));
        maxdetrendvarsspinner.addChangeListener(ev -> {
            savePreferences();
            closeFitFrames();
            maxDetrendVars = (Integer) maxdetrendvarsspinner.getValue();
            Prefs.set("plot.maxDetrendVars", maxDetrendVars);
            for (int c = 0; c < maxCurves; c++) {
                if (detrendVarDisplayed[c] >= maxDetrendVars) {
                    detrendVarDisplayed[c] = maxDetrendVars - 1;
                    Prefs.set("plot.detrendVarDisplayed" + c, detrendVarDisplayed[c]);
                }
            }
            subframeWasShowing = false;
            if (subFrame.isShowing()) {
                subFrameLocationX = subFrame.getLocation().x;
                subFrameLocationY = subFrame.getLocation().y;
                subFrameWidth = 0;
                subFrameHeight = subFrame.getHeight();
                Prefs.set("plot2.subFrameLocationX", subFrameLocationX);
                Prefs.set("plot2.subFrameLocationY", subFrameLocationY);
                subFrame.dispose();
                subframeWasShowing = true;
            }
            setupArrays();
            getPreferences();
            if (subframeWasShowing) {
                subscrollpane.removeAll();
                subFrame.remove(subscrollpane);
                repaintFrame(subFrame);
                showMoreCurvesJPanel();
            }
            updatePlot(updateAllFits());
        });
        maxdetrendvarsspinner.addMouseWheelListener(e -> {
            int newValue = (Integer) maxdetrendvarsspinner.getValue() - e.getWheelRotation();
            if (newValue > 0) { maxdetrendvarsspinner.setValue(newValue); } else maxdetrendvarsspinner.setValue(1);
        });
        maxdetrendvarspanel.add(maxdetrendvarsspinner);

        SpringUtil.makeCompactGrid(maxdetrendvarspanel, 1, maxdetrendvarspanel.getComponentCount(), 0, 0, 0, 0);
        mainpanelgroupa.add(maxdetrendvarspanel);


        JPanel mmagsubpanel = new JPanel(new SpringLayout());
        mmagsubpanel.setBorder(BorderFactory.createLineBorder(Color.lightGray, 1));
        mmagsubpanel.setToolTipText("<html>Set the number of starting samples averaged to calculate relative magnitude reference level.<br>" + "Set to zero to calculate magnitude from the raw data (i.e. now division by a reference value).<br>" + "A reference level region can also be defined using 'Norm/Mag Ref' on the Y-data panel.</html>");
        mmagrefsmodel = new SpinnerNumberModel(mmagrefs, 0, null, 1);
        mmagrefsspinner = new JSpinner(mmagrefsmodel);
        mmagrefsspinner.setFont(p11);
        mmagrefsspinner.setPreferredSize(new Dimension(60, 25));
        mmagrefsspinner.setToolTipText("<html>Set the number of starting samples averaged to calculate relative magnitude reference level.<br>" + "Set to zero to calculate magnitude from the raw data (i.e. now division by a reference value).<br>" + "A reference level region can also be defined using 'Norm/Mag Ref' on the Y-data panel.</html>");
        mmagrefsspinner.addChangeListener(ev -> {
            mmagrefs = (Integer) mmagrefsspinner.getValue();
            updatePlot(updateNoFits());
        });
        mmagrefsspinner.addMouseWheelListener(e -> {
            int newValue = (Integer) mmagrefsspinner.getValue() - e.getWheelRotation();
            if (newValue >= 0) mmagrefsspinner.setValue(newValue);
        });
        mmagsubpanel.add(mmagrefsspinner);

        mmagrefsspinnerlabel = new JLabel("samples");
        mmagrefsspinnerlabel.setFont(p11);
        mmagrefsspinnerlabel.setHorizontalAlignment(JLabel.LEFT);
        mmagsubpanel.add(mmagrefsspinnerlabel);
        SpringUtil.makeCompactGrid(mmagsubpanel, 1, 2, 0, 0, 0, 0);
        mainpanelgroupa.add(mmagsubpanel);


        //X-AXIS STEPSIZE POPUP

        xsteppopup = new JPopupMenu();
        JPanel xsteppanel = new JPanel();
        xstepmodel = new SpinnerListModel(spinnerscalelist);
        xstepspinner = new JSpinner(xstepmodel);
        xstepspinner.setValue(convertToText(xStep));
        xstepspinner.addChangeListener(ev -> {
            if (xWidth < 0.0001) xWidth = 0.0001;
            double value = IJU.getTextSpinnerDoubleValue(xstepspinner);//Double.parseDouble((String)xstepspinner.getValue());
            if (Double.isNaN(value)) return;
            xStep = value;
            vmarker1spinner.setModel(new SpinnerNumberModel(vMarker1Value, null, null, xStep));
            vmarker2spinner.setModel(new SpinnerNumberModel(vMarker2Value, null, null, xStep));
            xwidthspinner.setModel(new SpinnerNumberModel(xWidth, 0.0001, null, xStep));
            xmaxspinner.setModel(new SpinnerNumberModel(xMax, null, null, xStep));
            xminspinner.setModel(new SpinnerNumberModel(xMin, null, null, xStep));
            mfmarker1spinner.setModel(new SpinnerNumberModel(mfMarker1Value, null, null, xStep));
            dmarker1spinner.setModel(new SpinnerNumberModel(dMarker1Value, null, null, xStep));
            dmarker2spinner.setModel(new SpinnerNumberModel(dMarker2Value, null, null, xStep));
            dmarker3spinner.setModel(new SpinnerNumberModel(dMarker3Value, null, null, xStep));
            dmarker4spinner.setModel(new SpinnerNumberModel(dMarker4Value, null, null, xStep));
            vmarker1spinner.setEditor(new JSpinner.NumberEditor(vmarker1spinner, "########0.######"));
            vmarker2spinner.setEditor(new JSpinner.NumberEditor(vmarker2spinner, "########0.######"));
            xwidthspinner.setEditor(new JSpinner.NumberEditor(xwidthspinner, "########0.######"));
            xmaxspinner.setEditor(new JSpinner.NumberEditor(xmaxspinner, "########0.######"));
            xminspinner.setEditor(new JSpinner.NumberEditor(xminspinner, "########0.######"));
            mfmarker1spinner.setEditor(new JSpinner.NumberEditor(mfmarker1spinner, "########0.######"));
            dmarker1spinner.setEditor(new JSpinner.NumberEditor(dmarker1spinner, "########0.######"));
            dmarker2spinner.setEditor(new JSpinner.NumberEditor(dmarker2spinner, "########0.######"));
            dmarker3spinner.setEditor(new JSpinner.NumberEditor(dmarker3spinner, "########0.######"));
            dmarker4spinner.setEditor(new JSpinner.NumberEditor(dmarker4spinner, "########0.######"));
            Prefs.set("plot.xStep", xStep);
        });

        JLabel xsteplabel = new JLabel("Stepsize:");
        xsteppanel.add(xsteplabel);
        xsteppanel.add(xstepspinner);
        xsteppopup.add(xsteppanel);
        xsteppopup.setLightWeightPopupEnabled(false);
        xsteppopup.addPopupMenuListener(new PopupMenuListener() {
            public void popupMenuCanceled(PopupMenuEvent popupMenuEvent) {
                xsteppopup.setVisible(false);
            }

            public void popupMenuWillBecomeInvisible(PopupMenuEvent popupMenuEvent) {
                xsteppopup.setVisible(true);
            }

            public void popupMenuWillBecomeVisible(PopupMenuEvent popupMenuEvent) {
                xsteppopup.setVisible(false);
            }
        });


        JPanel vmarker1panel = new JPanel(new SpringLayout());
        vmarker1panel.setBorder(BorderFactory.createLineBorder(Color.lightGray, 1));
        showVMarker1CB = new JCheckBox("", showVMarker1);
        showVMarker1CB.setToolTipText("Select to show vertical marker 1");
        showVMarker1CB.addItemListener(e -> {
            if (e.getStateChange() == ItemEvent.DESELECTED) {
                showVMarker1 = false;
            } else if (e.getStateChange() == ItemEvent.SELECTED) showVMarker1 = true;
            updatePlot(updateNoFits());
        });
        showVMarker1CB.setHorizontalAlignment(JLabel.CENTER);
        vmarker1panel.add(showVMarker1CB);

        vmarker1spinnermodel = new SpinnerNumberModel(vMarker1Value, null, null, xStep);

        vmarker1spinner = new JSpinner(vmarker1spinnermodel);
        vmarker1spinner.setFont(p11);
//                JComponent editor = new JSpinner.NumberEditor(vmarker1spinner);
//                vmarker1spinner.setEditor(editor);
        vmarker1spinner.setEditor(new JSpinner.NumberEditor(vmarker1spinner, "########0.######"));
        vmarker1spinner.setPreferredSize(new Dimension(75, 25));
        vmarker1spinner.setComponentPopupMenu(xsteppopup);
        vmarker1spinner.setToolTipText("<html>" + "Enter vertical marker 1 x-axis location" + "<br>" + "or enter UT time in HH:MM or HH:MM:SS format and press 'Enter'" + "<br>" + "---------------------------------------------" + "<br>" + "For a predicted ingress time that occurs in the day prior<br>" + "to the first data point, enter the time as a negative value.<br>" + "If the first data point is at 0.2 and the predicted ingress is at 0.95 the day before, then enter -0.05.<br>" + "---------------------------------------------" + "<br>" + "Right click to set spinner stepsize<br>");

        vmarker1spinner.addChangeListener(ev -> {
            checkForUT(vmarker1spinner);
            vMarker1Value = (Double) vmarker1spinner.getValue(); //IJU.getSpinnerDoubleValue(vmarker1spinner);//
            Prefs.set("plot.vMarker1Value", vMarker1Value);
            if (!skipPlotUpdate) updatePlot(updateNoFits());
        });
        vmarker1spinner.addMouseWheelListener(e -> vmarker1spinner.setValue((Double) vmarker1spinner.getValue() - e.getWheelRotation() * xStep));
        vmarker1panel.add(vmarker1spinner);

        SpringUtil.makeCompactGrid(vmarker1panel, 1, vmarker1panel.getComponentCount(), 2, 2, 0, 0);
        mainpanelgroupa.add(vmarker1panel);


        JPanel detrendmarkercopypanel = new JPanel(new SpringLayout());
        detrendmarkercopypanel.setBorder(BorderFactory.createLineBorder(Color.lightGray, 1));


        copyVMarkersInvertedIcon = createImageIcon("astroj/images/copymarkersinverted.png", "Copy Detrend Markers Icon");
        JButton copyDetrendButton = new JButton(copyVMarkersInvertedIcon);
        copyDetrendButton.setMargin(new Insets(0, 0, 0, 0));
        copyDetrendButton.setToolTipText("Copy values from 'Fit and Normalize Region Selection' Left and Right Markers to V. Markers 1 and 2");
        copyDetrendButton.addActionListener(e -> {

            vmarker1spinner.setValue(dMarker2Value);
            vmarker2spinner.setValue(dMarker3Value);
            showVMarker1CB.setSelected(true);
            showVMarker2CB.setSelected(true);
            updatePlot(updateNoFits());
        });
        detrendmarkercopypanel.add(copyDetrendButton);
        SpringUtil.makeCompactGrid(detrendmarkercopypanel, 1, detrendmarkercopypanel.getComponentCount(), 2, 0, 2, 0);
        mainpanelgroupa.add(detrendmarkercopypanel);


        JPanel vmarker2panel = new JPanel(new SpringLayout());
        vmarker2panel.setBorder(BorderFactory.createLineBorder(Color.lightGray, 1));
        showVMarker2CB = new JCheckBox("", showVMarker2);
        showVMarker2CB.setToolTipText("Select to show vertical marker 2");
        showVMarker2CB.addItemListener(e -> {
            if (e.getStateChange() == ItemEvent.DESELECTED) {
                showVMarker2 = false;
            } else if (e.getStateChange() == ItemEvent.SELECTED) showVMarker2 = true;
            updatePlot(updateNoFits());
        });
        showVMarker2CB.setHorizontalAlignment(JLabel.CENTER);
        vmarker2panel.add(showVMarker2CB);

        vmarker2spinnermodel = new SpinnerNumberModel(vMarker2Value, null, null, xStep);
        vmarker2spinner = new JSpinner(vmarker2spinnermodel);
        vmarker2spinner.setFont(p11);
        vmarker2spinner.setEditor(new JSpinner.NumberEditor(vmarker2spinner, "########0.######"));
        vmarker2spinner.setPreferredSize(new Dimension(75, 25));
        vmarker2spinner.setComponentPopupMenu(xsteppopup);
        vmarker2spinner.setToolTipText("<html>" + "Enter vertical Marker 2 x-axis location" + "<br>" + "or enter UT time in HH:MM or HH:MM:SS format and press 'Enter'" + "<br>" + "---------------------------------------------" + "<br>" + "Right click to set spinner stepsize" + "</html>");
        vmarker2spinner.addChangeListener(ev -> {
            checkForUT(vmarker2spinner);
            vMarker2Value = (Double) vmarker2spinner.getValue();
            Prefs.set("plot.vMarker2Value", vMarker2Value);
            if (!skipPlotUpdate)  updatePlot(updateNoFits());
        });
        vmarker2spinner.addMouseWheelListener(e -> vmarker2spinner.setValue((Double) vmarker2spinner.getValue() - e.getWheelRotation() * xStep));
        vmarker2panel.add(vmarker2spinner);

        SpringUtil.makeCompactGrid(vmarker2panel, 1, vmarker2panel.getComponentCount(), 2, 2, 0, 0);
        mainpanelgroupa.add(vmarker2panel);

        SpringUtil.makeCompactGrid(mainpanelgroupa, 2, mainpanelgroupa.getComponentCount() / 2, 5, 5, 5, 5);
        mainpanel.add(mainpanelgroupa);

        // MAIN PANEL GROUP C START

        // TITLE GROUP
        JPanel titlegroup = new JPanel(new SpringLayout());
//                        titlegroup.setBorder (BorderFactory.createEmptyBorder(10,10,10,10));
        titlegroup.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(mainBorderColor, 1), "Title", TitledBorder.CENTER, TitledBorder.TOP, b12, Color.DARK_GRAY));

        // TITLE RADIO BUTTON GROUP
        JPanel titleradiopanelgroup = new JPanel(new SpringLayout());
        JRadioButton noTitleButton = new JRadioButton("None");
        noTitleButton.setFont(p11);
        noTitleButton.setSelected(!useTitle);
        titleradiopanelgroup.add(noTitleButton);
        JRadioButton useTitleButton = new JRadioButton("Custom");
        useTitleButton.setFont(p11);
        useTitleButton.setSelected(useTitle);
        titleradiopanelgroup.add(useTitleButton);
        ButtonGroup titleRadioGroup = new ButtonGroup();
        titleRadioGroup.add(noTitleButton);
        titleRadioGroup.add(useTitleButton);
        noTitleButton.addActionListener(ae -> {
            useTitle = false;
            updatePlot(updateNoFits());
        });
        useTitleButton.addActionListener(ae -> {
            useTitle = true;
            updatePlot(updateNoFits());
        });
        SpringUtil.makeCompactGrid(titleradiopanelgroup, 1, 2, 0, 0, 0, 0);
        titlegroup.add(titleradiopanelgroup);

        // TITLE TEXT FIELD
        titleField = new JTextField(title);
        titleField.setBorder(BorderFactory.createLineBorder(subBorderColor));
        titleField.setFont(p11);
        titleField.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent ev) {
                title = titleField.getText();
                updatePlot(updateNoFits());
            }

            public void removeUpdate(DocumentEvent ev) {
                title = titleField.getText();
                updatePlot(updateNoFits());
            }

            public void changedUpdate(DocumentEvent ev) {
                title = titleField.getText();
                updatePlot(updateNoFits());
            }
        });
        titleField.setPreferredSize(new Dimension(250, 20));
        titleField.setHorizontalAlignment(JTextField.LEFT);
        titlegroup.add(titleField);

        //MAKE TITLE Y-POSITION SLIDER

        titlePosYSlider = new JSlider(JSlider.HORIZONTAL, 0, 1000, (int) (titlePosY));
        titlePosYSlider.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));
        titlePosYSlider.setFont(p11);
        titlePosYSlider.addChangeListener(ev -> {
            JSlider slider = (JSlider) ev.getSource();
            titlePosY = slider.getValue();
            updatePlot(updateNoFits());
        });
        titlePosYSlider.addMouseWheelListener(e -> {
            int newValue = titlePosYSlider.getValue() + e.getWheelRotation();
            if ((newValue >= 0) && (newValue <= 1000)) titlePosYSlider.setValue(newValue);
        });
        titlePosYSlider.setPreferredSize(new Dimension(250, 50));
        titlePosYSlider.setInverted(false);
        titlePosYSlider.setMajorTickSpacing(500);
        titlePosYSlider.setMinorTickSpacing(50);
        titlePosYSlider.setPaintTicks(true);
        Hashtable<Integer, JLabel> titlePosYLabel = new Hashtable<>();
        JLabel toplabel1 = new JLabel("Top");
        toplabel1.setFont(p11);
        JLabel middlelabel1 = new JLabel("Middle");
        middlelabel1.setFont(p11);
        JLabel bottomlabel1 = new JLabel("Bottom");
        bottomlabel1.setFont(p11);
        titlePosYLabel.put(0, toplabel1);
        titlePosYLabel.put(500, middlelabel1);
        titlePosYLabel.put(1000, bottomlabel1);
        titlePosYSlider.setLabelTable(titlePosYLabel);
        titlePosYSlider.setPaintTrack(true);
        titlePosYSlider.setPaintLabels(true);
        titlegroup.add(titlePosYSlider);

        //MAKE TITLE X-POSITION SLIDER

        titlePosXSlider = new JSlider(0, 500, (int) (titlePosX * 500.0));
        titlePosXSlider.setBorder(BorderFactory.createEmptyBorder(10, 5, 0, 5));
        titlePosXSlider.addChangeListener(ev -> {
            JSlider slider = (JSlider) ev.getSource();
            titlePosX = slider.getValue() / 500.0;
            updatePlot(updateNoFits());
        });
        titlePosXSlider.addMouseWheelListener(e -> {
            int newValue = titlePosXSlider.getValue() + e.getWheelRotation();
            if ((newValue >= 0) && (newValue <= 500)) titlePosXSlider.setValue(newValue);
        });
        titlePosXSlider.setPreferredSize(new Dimension(250, 50));
        titlePosXSlider.setMajorTickSpacing(250);
        titlePosXSlider.setMinorTickSpacing(25);
        titlePosXSlider.setPaintTicks(true);
        Hashtable<Integer, JLabel> titlePosXLabel = new Hashtable<>();
        JLabel leftlabel1 = new JLabel("Left");
        leftlabel1.setFont(p11);
        JLabel centerlabel1 = new JLabel("Center");
        centerlabel1.setFont(p11);
        JLabel rightlabel1 = new JLabel("Right");
        rightlabel1.setFont(p11);
        titlePosXLabel.put(0, leftlabel1);
        titlePosXLabel.put(250, centerlabel1);
        titlePosXLabel.put(500, rightlabel1);
        titlePosXSlider.setLabelTable(titlePosXLabel);
        titlePosXSlider.setFont(b12);
        titlePosXSlider.setPaintTrack(true);
        titlePosXSlider.setPaintLabels(true);
        titlegroup.add(titlePosXSlider);
        SpringUtil.makeCompactGrid(titlegroup, 4, 1, 0, 0, 0, 0);
        mainpanelgroupc.add(titlegroup);

        //SUBTITLE GROUP
        JPanel subtitlegroup = new JPanel(new SpringLayout());
        subtitlegroup.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(mainBorderColor, 1), "Subtitle", TitledBorder.CENTER, TitledBorder.TOP, b12, Color.DARK_GRAY));

        // SUBTITLE RADIO BUTTON GROUP
        JPanel subtitleradiopanelgroup = new JPanel(new SpringLayout());
        JRadioButton noSubtitleButton = new JRadioButton("None");
        noSubtitleButton.setFont(p11);
        noSubtitleButton.setSelected(!useSubtitle);
        subtitleradiopanelgroup.add(noSubtitleButton);
        JRadioButton useSubtitleButton = new JRadioButton("Custom");
        useSubtitleButton.setFont(p11);
        useSubtitleButton.setSelected(useSubtitle);
        subtitleradiopanelgroup.add(useSubtitleButton);
        ButtonGroup subtitleRadioGroup = new ButtonGroup();
        subtitleRadioGroup.add(noSubtitleButton);
        subtitleRadioGroup.add(useSubtitleButton);
        noSubtitleButton.addActionListener(ae -> {
            useSubtitle = false;
            updatePlot(updateNoFits());
        });
        useSubtitleButton.addActionListener(ae -> {
            useSubtitle = true;
            updatePlot(updateNoFits());
        });
        SpringUtil.makeCompactGrid(subtitleradiopanelgroup, 1, 2, 0, 0, 0, 0);
        subtitlegroup.add(subtitleradiopanelgroup);

        // SUBTITLE TEXT FIELD
        subtitleField = new JTextField(subtitle);
        subtitleField.setFont(p11);
        subtitleField.setBorder(BorderFactory.createLineBorder(subBorderColor));
        subtitleField.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent ev) {
                subtitle = subtitleField.getText();
                updatePlot(updateNoFits());
            }

            public void removeUpdate(DocumentEvent ev) {
                subtitle = subtitleField.getText();
                updatePlot(updateNoFits());
            }

            public void changedUpdate(DocumentEvent ev) {
                subtitle = subtitleField.getText();
                updatePlot(updateNoFits());
            }
        });
        subtitleField.setPreferredSize(new Dimension(250, 20));
        subtitleField.setHorizontalAlignment(JTextField.LEFT);

        subtitlegroup.add(subtitleField);

        //SUBTITLE Y-POSITION SLIDER

        subtitlePosYSlider = new JSlider(JSlider.HORIZONTAL, 0, 1000, (int) (subtitlePosY));
        subtitlePosYSlider.setFont(p11);
        subtitlePosYSlider.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));
        subtitlePosYSlider.addChangeListener(ev -> {
            JSlider slider = (JSlider) ev.getSource();
            subtitlePosY = slider.getValue();
            updatePlot(updateNoFits());
        });
        subtitlePosYSlider.addMouseWheelListener(e -> {
            int newValue = subtitlePosYSlider.getValue() + e.getWheelRotation();
            if ((newValue >= 0) && (newValue <= 1000)) subtitlePosYSlider.setValue(newValue);
        });
        subtitlePosYSlider.setPreferredSize(new Dimension(250, 50));
        subtitlePosYSlider.setInverted(false);
        subtitlePosYSlider.setMajorTickSpacing(500);
        subtitlePosYSlider.setMinorTickSpacing(50);
        subtitlePosYSlider.setPaintTicks(true);
        Hashtable<Integer, JLabel> subtitlePosYLabel = new Hashtable<>();
        JLabel toplabel2 = new JLabel("Top");
        toplabel2.setFont(p11);
        JLabel middlelabel2 = new JLabel("Middle");
        middlelabel2.setFont(p11);
        JLabel bottomlabel2 = new JLabel("Bottom");
        bottomlabel2.setFont(p11);
        subtitlePosYLabel.put(0, toplabel2);
        subtitlePosYLabel.put(500, middlelabel2);
        subtitlePosYLabel.put(1000, bottomlabel2);

        subtitlePosYSlider.setLabelTable(subtitlePosYLabel);
        subtitlePosYSlider.setPaintTrack(true);
        subtitlePosYSlider.setPaintLabels(true);
        subtitlegroup.add(subtitlePosYSlider);

        //SUBTITLE X-POSITION SLIDER

        subtitlePosXSlider = new JSlider(0, 500, (int) (subtitlePosX * 500.0));
        subtitlePosXSlider.setFont(p11);
        subtitlePosXSlider.setBorder(BorderFactory.createEmptyBorder(10, 5, 0, 5));
        subtitlePosXSlider.addChangeListener(ev -> {
            JSlider slider = (JSlider) ev.getSource();
            subtitlePosX = slider.getValue() / 500.0;
            updatePlot(updateNoFits());
        });
        subtitlePosXSlider.addMouseWheelListener(e -> {
            int newValue = subtitlePosXSlider.getValue() + e.getWheelRotation();
            if ((newValue >= 0) && (newValue <= 500)) subtitlePosXSlider.setValue(newValue);
        });
        subtitlePosXSlider.setPreferredSize(new Dimension(250, 50));
        subtitlePosXSlider.setMajorTickSpacing(250);
        subtitlePosXSlider.setMinorTickSpacing(25);
        subtitlePosXSlider.setPaintTicks(true);
        Hashtable<Integer, JLabel> subtitlePosXLabel = new Hashtable<>();
        JLabel leftlabel2 = new JLabel("Left");
        leftlabel2.setFont(p11);
        JLabel centerlabel2 = new JLabel("Center");
        centerlabel2.setFont(p11);
        JLabel rightlabel2 = new JLabel("Right");
        rightlabel2.setFont(p11);
        subtitlePosXLabel.put(0, leftlabel2);
        subtitlePosXLabel.put(250, centerlabel2);
        subtitlePosXLabel.put(500, rightlabel2);
        subtitlePosXSlider.setLabelTable(subtitlePosXLabel);
        subtitlePosXSlider.setPaintTrack(true);
        subtitlePosXSlider.setPaintLabels(true);
        subtitlegroup.add(subtitlePosXSlider);
        SpringUtil.makeCompactGrid(subtitlegroup, 4, 1, 0, 0, 0, 0);
        mainpanelgroupc.add(subtitlegroup);


        // LEGEND GROUP
        legendgroup = new JPanel(new SpringLayout());
        legendgroup.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(mainBorderColor, 1), "Legend", TitledBorder.CENTER, TitledBorder.TOP, b12, Color.DARK_GRAY));

        // LEGEND LABEL POPUP

        legendpopup = new JPopupMenu();
        JMenuItem legendshowscalingCB = new JCheckBoxMenuItem("Append scaling factor to legend for absolute curves", showLScaleInfo);
        legendshowscalingCB.addItemListener(e -> {
            if (e.getStateChange() == ItemEvent.DESELECTED) {
                showLScaleInfo = false;
            } else if (e.getStateChange() == ItemEvent.SELECTED) showLScaleInfo = true;
            updatePlot(updateNoFits());
        });
        legendpopup.add(legendshowscalingCB);
        JMenuItem legendshowrelscalingCB = new JCheckBoxMenuItem("Append scaling factor to legend for relative curves", showLRelScaleInfo);
        legendshowrelscalingCB.addItemListener(e -> {
            if (e.getStateChange() == ItemEvent.DESELECTED) {
                showLRelScaleInfo = false;
            } else if (e.getStateChange() == ItemEvent.SELECTED) showLRelScaleInfo = true;
            updatePlot(updateNoFits());
        });
        legendpopup.add(legendshowrelscalingCB);
        JMenuItem legendshowshiftCB = new JCheckBoxMenuItem("Append shift factor to legend for absolute curves", showLShiftInfo);
        legendshowshiftCB.addItemListener(e -> {
            if (e.getStateChange() == ItemEvent.DESELECTED) {
                showLShiftInfo = false;
            } else if (e.getStateChange() == ItemEvent.SELECTED) showLShiftInfo = true;
            updatePlot(updateNoFits());
        });
        legendpopup.add(legendshowshiftCB);
        JMenuItem legendshowrelshiftCB = new JCheckBoxMenuItem("Append shift factor to legend for relative curves", showLRelShiftInfo);
        legendshowrelshiftCB.addItemListener(e -> {
            if (e.getStateChange() == ItemEvent.DESELECTED) {
                showLRelShiftInfo = false;
            } else if (e.getStateChange() == ItemEvent.SELECTED) showLRelShiftInfo = true;
            updatePlot(updateNoFits());
        });
        legendpopup.add(legendshowrelshiftCB);
        JMenuItem legendshowdetrendCB = new JCheckBoxMenuItem("Append detrend usage to legend", showLdetrendInfo);
        legendshowdetrendCB.addItemListener(e -> {
            if (e.getStateChange() == ItemEvent.DESELECTED) {
                showLdetrendInfo = false;
            } else if (e.getStateChange() == ItemEvent.SELECTED) showLdetrendInfo = true;
            updatePlot(updateNoFits());
        });
        legendpopup.add(legendshowdetrendCB);
        JMenuItem legendshownormCB = new JCheckBoxMenuItem("Append normalize usage to legend", showLnormInfo);
        legendshownormCB.addItemListener(e -> {
            if (e.getStateChange() == ItemEvent.DESELECTED) {
                showLnormInfo = false;
            } else if (e.getStateChange() == ItemEvent.SELECTED) showLnormInfo = true;
            updatePlot(updateNoFits());
        });
        legendpopup.add(legendshownormCB);
        JMenuItem legendshowmmagCB = new JCheckBoxMenuItem("Append magnitude usage to legend", showLmmagInfo);
        legendshowmmagCB.addItemListener(e -> {
            if (e.getStateChange() == ItemEvent.DESELECTED) {
                showLmmagInfo = false;
            } else if (e.getStateChange() == ItemEvent.SELECTED) showLmmagInfo = true;
            updatePlot(updateNoFits());
        });
        legendpopup.add(legendshowmmagCB);

        JMenuItem legendshowsigmadetrendCB = new JCheckBoxMenuItem("Append Std. Dev. to legend for detrended absolute curves", showSigmaForDetrendedCurves);
        legendshowsigmadetrendCB.addItemListener(e -> {
            if (e.getStateChange() == ItemEvent.DESELECTED) {
                showSigmaForDetrendedCurves = false;
            } else if (e.getStateChange() == ItemEvent.SELECTED) showSigmaForDetrendedCurves = true;
            updatePlot(updateNoFits());
        });
        legendpopup.add(legendshowsigmadetrendCB);

        JMenuItem legendshowsigmaallCB = new JCheckBoxMenuItem("Append Std. Dev. to legend for all absolute curves", showSigmaForAllCurves);
        legendshowsigmaallCB.addItemListener(e -> {
            if (e.getStateChange() == ItemEvent.DESELECTED) {
                showSigmaForAllCurves = false;
            } else if (e.getStateChange() == ItemEvent.SELECTED) showSigmaForAllCurves = true;
            updatePlot(updateNoFits());
        });
        legendpopup.add(legendshowsigmaallCB);

        JMenuItem legendShowOutbinCB = new JCheckBoxMenuItem("Append output binned Std. Dev. to legend", showOutBinRms);
        legendShowOutbinCB.addItemListener(e -> {
            if (e.getStateChange() == ItemEvent.DESELECTED) {
                showOutBinRms = false;
            } else if (e.getStateChange() == ItemEvent.SELECTED) showOutBinRms = true;
            updatePlot(updateNoFits());
        });
        legendpopup.add(legendShowOutbinCB);

        JMenuItem legendShowInputAvgSizeCB = new JCheckBoxMenuItem("Append averaged data size to legend", showLAvgInfo);
        legendShowInputAvgSizeCB.addItemListener(e -> {
            if (e.getStateChange() == ItemEvent.DESELECTED) {
                showLAvgInfo = false;
            } else if (e.getStateChange() == ItemEvent.SELECTED) showLAvgInfo = true;
            updatePlot(updateNoFits());
        });
        legendpopup.add(legendShowInputAvgSizeCB);
        JMenuItem legendshowsymbolCB = new JCheckBoxMenuItem("Append symbol description to legend", showLSymbolInfo);
        legendshowsymbolCB.addItemListener(e -> {
            if (e.getStateChange() == ItemEvent.DESELECTED) {
                showLSymbolInfo = false;
            } else if (e.getStateChange() == ItemEvent.SELECTED) showLSymbolInfo = true;
            updatePlot(updateNoFits());
        });
        legendpopup.add(legendshowsymbolCB);
//                        JMenuItem twolinelegendCB = new JCheckBoxMenuItem("Use two lines per legend",useTwoLineLegend);
//                        twolinelegendCB.addItemListener(new ItemListener(){
//                            public void itemStateChanged(ItemEvent e) {
//                                if (e.getStateChange() == ItemEvent.DESELECTED)
//                                        useTwoLineLegend = false;
//                                else if (e.getStateChange() == ItemEvent.SELECTED)
//                                        useTwoLineLegend = true;
//                                updatePlot(updateNoFits());}});
//                        legendpopup.add(twolinelegendCB);


        legendpopup.addPopupMenuListener(new PopupMenuListener() {
            public void popupMenuCanceled(PopupMenuEvent popupMenuEvent) { }

            public void popupMenuWillBecomeInvisible(PopupMenuEvent popupMenuEvent) { }

            public void popupMenuWillBecomeVisible(PopupMenuEvent popupMenuEvent) { }
        });


        // LEGEND RADIO BUTTON GROUP
        JPanel legendradiopanelgroup = new JPanel();
        legendradiopanelgroup.setLayout(new BoxLayout(legendradiopanelgroup, BoxLayout.X_AXIS));
        JLabel legendradiolabel = new JLabel("Align:");
        legendradiolabel.setFont(p11);
        legendradiolabel.setToolTipText("Right click to set legend preferences");
        legendradiopanelgroup.add(legendradiolabel);
        legendradiopanelgroup.add(Box.createHorizontalStrut(2));
        JRadioButton leftButton = new JRadioButton("Left");
        leftButton.setFont(p11);
        leftButton.setSelected(legendLeft);
        legendradiopanelgroup.add(leftButton);
        legendradiopanelgroup.add(Box.createHorizontalStrut(2));
        JRadioButton centerButton = new JRadioButton("Center");
        centerButton.setFont(p11);
        centerButton.setSelected(!legendLeft && !legendRight);
        legendradiopanelgroup.add(centerButton);
        legendradiopanelgroup.add(Box.createHorizontalStrut(2));
        JRadioButton rightButton = new JRadioButton("Right");
        rightButton.setFont(p11);
        rightButton.setSelected(legendRight);
        legendradiopanelgroup.add(rightButton);
        legendradiopanelgroup.add(Box.createHorizontalStrut(2));
        legendradiopanelgroup.add(Box.createHorizontalGlue());

        ButtonGroup legendRadioGroup = new ButtonGroup();
        legendRadioGroup.add(leftButton);
        legendRadioGroup.add(centerButton);
        legendRadioGroup.add(rightButton);
        leftButton.addActionListener(ae -> {
            legendLeft = true;
            legendRight = false;
            updatePlot(updateNoFits());
        });
        centerButton.addActionListener(ae -> {
            legendLeft = false;
            legendRight = false;
            updatePlot(updateNoFits());
        });
        rightButton.addActionListener(ae -> {
            legendLeft = false;
            legendRight = true;
            updatePlot(updateNoFits());
        });
        configureIcon = createImageIcon("astroj/images/configure.png", "Configure legend options");

        legendconfigureButton = new JButton(configureIcon);
        legendconfigureButton.setToolTipText("Configure legend options");
        legendconfigureButton.setMargin(new Insets(0, 0, 0, 0));

        legendconfigureButton.addActionListener(ae -> legendpopup.show(legendconfigureButton, mainpanel.getX(), mainpanel.getY() + 25));
        legendradiopanelgroup.add(legendconfigureButton);
        legendradiopanelgroup.add(Box.createHorizontalStrut(2));
//                                SpringUtil.makeCompactGrid (legendradiopanelgroup, 1, legendradiopanelgroup.getComponentCount(), 1,1,1,1);
        leftButton.setComponentPopupMenu(legendpopup);
        leftButton.setToolTipText("Right click to set legend preferences");
        rightButton.setComponentPopupMenu(legendpopup);
        rightButton.setToolTipText("Right click to set legend preferences");
        centerButton.setComponentPopupMenu(legendpopup);
        centerButton.setToolTipText("Right click to set legend preferences");
        legendgroup.add(legendradiopanelgroup);

        // LEGEND SLIDER LABEL
        JLabel legendsliderlabel = new JLabel("Position");
        legendsliderlabel.setFont(p11);
        legendsliderlabel.setBorder(BorderFactory.createEmptyBorder(5, 0, 0, 0));
        legendsliderlabel.setToolTipText("Right click to set legend preferences");
        legendsliderlabel.setHorizontalAlignment(JLabel.CENTER);
        legendsliderlabel.setComponentPopupMenu(legendpopup);

        legendgroup.add(legendsliderlabel);

        //LEGEND Y-POSITION SLIDER

        legendPosYSlider = new JSlider(JSlider.HORIZONTAL, 0, 500, (int) (legendPosY * 500.0));
        legendPosYSlider.setFont(p11);
        legendPosYSlider.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));
        legendPosYSlider.addChangeListener(ev -> {
            JSlider slider = (JSlider) ev.getSource();
            legendPosY = slider.getValue() / 500.0;
            updatePlot(updateNoFits());
        });
        legendPosYSlider.addMouseWheelListener(e -> {
            int newValue = legendPosYSlider.getValue() + e.getWheelRotation();
            if ((newValue >= 0) && (newValue <= 500)) legendPosYSlider.setValue(newValue);
        });
        legendPosYSlider.setPreferredSize(new Dimension(250, 50));
        legendPosYSlider.setInverted(false);
        legendPosYSlider.setMajorTickSpacing(250);
        legendPosYSlider.setMinorTickSpacing(25);
        legendPosYSlider.setPaintTicks(true);
        Hashtable<Integer, JLabel> legendPosYLabel = new Hashtable<>();
        JLabel toplabel3 = new JLabel("Top");
        toplabel3.setFont(p11);
        JLabel middlelabel3 = new JLabel("Middle");
        middlelabel3.setFont(p11);
        JLabel bottomlabel3 = new JLabel("Bottom");
        bottomlabel3.setFont(p11);
        legendPosYLabel.put(0, toplabel3);
        legendPosYLabel.put(250, middlelabel3);
        legendPosYLabel.put(500, bottomlabel3);
        legendPosYSlider.setLabelTable(legendPosYLabel);
        legendPosYSlider.setPaintTrack(true);
        legendPosYSlider.setPaintLabels(true);
        legendgroup.add(legendPosYSlider);

        //LEGEND X-POSITION SLIDER

        legendPosXSlider = new JSlider(0, 500, (int) (legendPosX * 500.0));
        legendPosXSlider.setFont(p11);
        legendPosXSlider.setBorder(BorderFactory.createEmptyBorder(10, 5, 0, 5));
        legendPosXSlider.addChangeListener(ev -> {
            JSlider slider = (JSlider) ev.getSource();
            legendPosX = slider.getValue() / 500.0;
            updatePlot(updateNoFits());
        });
        legendPosXSlider.addMouseWheelListener(e -> {
            int newValue = legendPosXSlider.getValue() + e.getWheelRotation();
            if ((newValue >= 0) && (newValue <= 500)) legendPosXSlider.setValue(newValue);
        });
        legendPosXSlider.setPreferredSize(new Dimension(250, 50));
        legendPosXSlider.setMajorTickSpacing(250);
        legendPosXSlider.setMinorTickSpacing(25);
        legendPosXSlider.setPaintTicks(true);
        Hashtable<Integer, JLabel> legendPosXLabel = new Hashtable<>();
        JLabel leftlabel3 = new JLabel("Left");
        leftlabel3.setFont(p11);
        JLabel centerlabel3 = new JLabel("Center");
        centerlabel3.setFont(p11);
        JLabel rightlabel3 = new JLabel("Right");
        rightlabel3.setFont(p11);
        legendPosXLabel.put(0, leftlabel3);
        legendPosXLabel.put(250, centerlabel3);
        legendPosXLabel.put(500, rightlabel3);
        legendPosXSlider.setLabelTable(legendPosXLabel);
        legendPosXSlider.setPaintTrack(true);
        legendPosXSlider.setPaintLabels(true);
        legendgroup.add(legendPosXSlider);
        SpringUtil.makeCompactGrid(legendgroup, 4, 1, 0, 0, 0, 0);

        mainpanelgroupc.add(legendgroup);

        SpringUtil.makeCompactGrid(mainpanelgroupc, 1, 3, 0, 0, 0, 0);
        mainpanel.add(mainpanelgroupc);

        // X & Y AXIS LEGEND GROUP (GROUP D)

        //X-AXIS LEGEND GROUP
        JPanel xlegendgroup = new JPanel(new SpringLayout());
        xlegendgroup.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(mainBorderColor, 1), "X-Axis Label", TitledBorder.CENTER, TitledBorder.TOP, b12, Color.DARK_GRAY));

        // X-AXIS LABEL POPUP

        xaxispopup = new JPopupMenu();
        JMenuItem showxscalingCB = new JCheckBoxMenuItem("Append scaling factor to X-axis label", showXScaleInfo);

        showxscalingCB.addItemListener(e -> {
            if (e.getStateChange() == ItemEvent.DESELECTED) {
                showXScaleInfo = false;
            } else if (e.getStateChange() == ItemEvent.SELECTED) showXScaleInfo = true;
            updatePlot(updateNoFits());
        });
        xaxispopup.add(showxscalingCB);
        xaxispopup.addPopupMenuListener(new PopupMenuListener() {
            public void popupMenuCanceled(PopupMenuEvent popupMenuEvent) { }

            public void popupMenuWillBecomeInvisible(PopupMenuEvent popupMenuEvent) { }

            public void popupMenuWillBecomeVisible(PopupMenuEvent popupMenuEvent) { }
        });

        // X-AXIS LEGEND RADIO BUTTON GROUP
        JPanel xlegendradiopanelgroup = new JPanel();
        xlegendradiopanelgroup.setLayout(new BoxLayout(xlegendradiopanelgroup, BoxLayout.X_AXIS));
        JRadioButton xnoneButton = new JRadioButton("None");
        xnoneButton.setFont(p11);
        xnoneButton.setSelected(!useXCustomName && !useXColumnName);
        xlegendradiopanelgroup.add(Box.createHorizontalStrut(2));
        xlegendradiopanelgroup.add(xnoneButton);
        xlegendradiopanelgroup.add(Box.createHorizontalStrut(2));
        JRadioButton xcolumnNameButton = new JRadioButton("Column Label");
        xcolumnNameButton.setFont(p11);
        xcolumnNameButton.setSelected(useXColumnName);
        xlegendradiopanelgroup.add(xcolumnNameButton);
        xlegendradiopanelgroup.add(Box.createHorizontalStrut(2));
        JRadioButton xcustomNameButton = new JRadioButton("Custom Label");
        xcustomNameButton.setFont(p11);
        xcustomNameButton.setSelected(!useXColumnName && useXCustomName);
        xlegendradiopanelgroup.add(xcustomNameButton);
        xlegendradiopanelgroup.add(Box.createHorizontalStrut(2));
        xlegendradiopanelgroup.add(Box.createGlue());
        ButtonGroup xlegendRadioGroup = new ButtonGroup();
        xlegendRadioGroup.add(xnoneButton);
        xlegendRadioGroup.add(xcolumnNameButton);
        xlegendRadioGroup.add(xcustomNameButton);
        xnoneButton.addActionListener(ae -> {
            useXColumnName = false;
            useXCustomName = false;
            updatePlot(updateNoFits());
        });
        xcustomNameButton.addActionListener(ae -> {
            useXColumnName = false;
            useXCustomName = true;
            updatePlot(updateNoFits());
        });
        xcolumnNameButton.addActionListener(ae -> {
            useXColumnName = true;
            useXCustomName = false;
            updatePlot(updateNoFits());
        });
        xlegendconfigureButton = new JButton(configureIcon);
        xlegendconfigureButton.setToolTipText("Set X-axis label preferences");
        xlegendconfigureButton.setMargin(new Insets(0, 0, 0, 0));

        xlegendconfigureButton.addActionListener(ae -> xaxispopup.show(xlegendconfigureButton, mainpanel.getX(), mainpanel.getY() + 25));
        xlegendradiopanelgroup.add(xlegendconfigureButton);
        xlegendradiopanelgroup.add(Box.createHorizontalStrut(2));
//                                SpringUtil.makeCompactGrid (xlegendradiopanelgroup, 1, xlegendradiopanelgroup.getComponentCount(), 1,1,1,1);
        xnoneButton.setComponentPopupMenu(xaxispopup);
        xnoneButton.setToolTipText("Right click to set X-axis label preferences");
        xcustomNameButton.setComponentPopupMenu(xaxispopup);
        xcustomNameButton.setToolTipText("Right click to set X-axis label preferences");
        xcolumnNameButton.setComponentPopupMenu(xaxispopup);
        xcolumnNameButton.setToolTipText("Right click to set X-axis label preferences");
        xlegendgroup.add(xlegendradiopanelgroup);

        // X LEGEND TEXT FIELD
        xlegendField = new JTextField(xLegend);
        xlegendField.setFont(p11);
        xlegendField.setBorder(BorderFactory.createLineBorder(subBorderColor));
        xlegendField.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent ev) {
                xLegend = xlegendField.getText();
                updatePlot(updateNoFits());
            }

            public void removeUpdate(DocumentEvent ev) {
                xLegend = xlegendField.getText();
                updatePlot(updateNoFits());
            }

            public void changedUpdate(DocumentEvent ev) {
                xLegend = xlegendField.getText();
                updatePlot(updateNoFits());
            }
        });
        xlegendField.setPreferredSize(new Dimension(250, 20));
        xlegendField.setHorizontalAlignment(JTextField.LEFT);
        xlegendField.setComponentPopupMenu(xaxispopup);
        xlegendField.setToolTipText("Right click to set X-axis label preferences");

        xlegendgroup.add(xlegendField);
        SpringUtil.makeCompactGrid(xlegendgroup, 2, 1, 0, 0, 0, 0);
        mainpanelgroupd.add(xlegendgroup);

        //Y-AXIS LEGEND GROUP
        JPanel ylegendgroup = new JPanel(new SpringLayout());
        ylegendgroup.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(mainBorderColor, 1), "Y-Axis Label", TitledBorder.CENTER, TitledBorder.TOP, b12, Color.DARK_GRAY));

        // Y-AXIS LABEL POPUP

        yaxispopup = new JPopupMenu();
        JMenuItem showyscalingCB = new JCheckBoxMenuItem("Append scaling factor to Y-axis label", showYScaleInfo);
        showyscalingCB.addItemListener(e -> {
            if (e.getStateChange() == ItemEvent.DESELECTED) {
                showYScaleInfo = false;
            } else if (e.getStateChange() == ItemEvent.SELECTED) showYScaleInfo = true;
            updatePlot(updateNoFits());
        });
        yaxispopup.add(showyscalingCB);
        JMenuItem showyshiftCB = new JCheckBoxMenuItem("Append shift factor to Y-axis label", showYShiftInfo);
        showyshiftCB.addItemListener(e -> {
            if (e.getStateChange() == ItemEvent.DESELECTED) {
                showYShiftInfo = false;
            } else if (e.getStateChange() == ItemEvent.SELECTED) showYShiftInfo = true;
            updatePlot(updateNoFits());
        });
        yaxispopup.add(showyshiftCB);

        JMenuItem showynormCB = new JCheckBoxMenuItem("Append normalization usage to Y-axis label", showYNormInfo);
        showynormCB.addItemListener(e -> {
            if (e.getStateChange() == ItemEvent.DESELECTED) {
                showYNormInfo = false;
            } else if (e.getStateChange() == ItemEvent.SELECTED) showYNormInfo = true;
            updatePlot(updateNoFits());
        });
        yaxispopup.add(showynormCB);

        JMenuItem showymmagCB = new JCheckBoxMenuItem("Append magnitude usage to Y-axis label", showYmmagInfo);
        showymmagCB.addItemListener(e -> {
            if (e.getStateChange() == ItemEvent.DESELECTED) {
                showYmmagInfo = false;
            } else if (e.getStateChange() == ItemEvent.SELECTED) showYmmagInfo = true;
            updatePlot(updateNoFits());
        });
        yaxispopup.add(showymmagCB);

        JMenuItem showYInputAvgCB = new JCheckBoxMenuItem("Append averaged data size to Y-axis label", showYAvgInfo);
        showYInputAvgCB.addItemListener(e -> {
            if (e.getStateChange() == ItemEvent.DESELECTED) {
                showYAvgInfo = false;
            } else if (e.getStateChange() == ItemEvent.SELECTED) showYAvgInfo = true;
            updatePlot(updateNoFits());
        });
        yaxispopup.add(showYInputAvgCB);

        JMenuItem showysymbolCB = new JCheckBoxMenuItem("Append symbol description to Y-axis label", showYSymbolInfo);
        showysymbolCB.addItemListener(e -> {
            if (e.getStateChange() == ItemEvent.DESELECTED) {
                showYSymbolInfo = false;
            } else if (e.getStateChange() == ItemEvent.SELECTED) showYSymbolInfo = true;
            updatePlot(updateNoFits());
        });
        yaxispopup.add(showysymbolCB);
        yaxispopup.addPopupMenuListener(new PopupMenuListener() {
            public void popupMenuCanceled(PopupMenuEvent popupMenuEvent) { }

            public void popupMenuWillBecomeInvisible(PopupMenuEvent popupMenuEvent) { }

            public void popupMenuWillBecomeVisible(PopupMenuEvent popupMenuEvent) { }
        });

        // Y-AXIS LEGEND RADIO BUTTON GROUP
        JPanel ylegendradiopanelgroup = new JPanel();
        ylegendradiopanelgroup.setLayout(new BoxLayout(ylegendradiopanelgroup, BoxLayout.X_AXIS));
        JRadioButton ynoneButton = new JRadioButton("None");
        ynoneButton.setFont(p11);
        ynoneButton.setSelected(!useYCustomName && !useYColumnName);
        ylegendradiopanelgroup.add(ynoneButton);
        ylegendradiopanelgroup.add(Box.createHorizontalStrut(2));
        JRadioButton ycolumnNameButton = new JRadioButton("Column Label");
        ycolumnNameButton.setFont(p11);
        ycolumnNameButton.setSelected(useYColumnName);
        ylegendradiopanelgroup.add(ycolumnNameButton);
        ylegendradiopanelgroup.add(Box.createHorizontalStrut(2));
        JRadioButton ycustomNameButton = new JRadioButton("Custom Label");
        ycustomNameButton.setFont(p11);
        ycustomNameButton.setSelected(!useYColumnName && useYCustomName);
        ylegendradiopanelgroup.add(ycustomNameButton);
        ylegendradiopanelgroup.add(Box.createHorizontalStrut(2));
        ylegendradiopanelgroup.add(Box.createGlue());

        ylegendconfigureButton = new JButton(configureIcon);
        ylegendconfigureButton.setToolTipText("Set Y-axis label preferences");
        ylegendconfigureButton.setMargin(new Insets(0, 0, 0, 0));

        ylegendconfigureButton.addActionListener(ae -> yaxispopup.show(ylegendconfigureButton, mainpanel.getX(), mainpanel.getY() + 25));
        ylegendradiopanelgroup.add(ylegendconfigureButton);
        ylegendradiopanelgroup.add(Box.createHorizontalStrut(2));

        ButtonGroup ylegendRadioGroup = new ButtonGroup();
        ylegendRadioGroup.add(ynoneButton);
        ylegendRadioGroup.add(ycustomNameButton);
        ylegendRadioGroup.add(ycolumnNameButton);
        ynoneButton.addActionListener(ae -> {
            useYColumnName = false;
            useYCustomName = false;
            updatePlot(updateNoFits());
        });
        ycustomNameButton.addActionListener(ae -> {
            useYColumnName = false;
            useYCustomName = true;
            updatePlot(updateNoFits());
        });
        ycolumnNameButton.addActionListener(ae -> {
            useYColumnName = true;
            useYCustomName = false;
            updatePlot(updateNoFits());
        });
//                                SpringUtil.makeCompactGrid (ylegendradiopanelgroup, 1, 3, 0,0,0,0);
        ynoneButton.setComponentPopupMenu(yaxispopup);
        ynoneButton.setToolTipText("Right click to set Y-axis label preferences");
        ycustomNameButton.setComponentPopupMenu(yaxispopup);
        ycustomNameButton.setToolTipText("Right click to set Y-axis label preferences");
        ycolumnNameButton.setComponentPopupMenu(yaxispopup);
        ycolumnNameButton.setToolTipText("Right click to set Y-axis label preferences");
        ylegendgroup.add(ylegendradiopanelgroup);

        // Y-LEGEND TEXT FIELD
        ylegendField = new JTextField(yLegend);
        ylegendField.setFont(p11);
        ylegendField.setBorder(BorderFactory.createLineBorder(subBorderColor));
        ylegendField.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent ev) {
                yLegend = ylegendField.getText();
                updatePlot(updateNoFits());
            }

            public void removeUpdate(DocumentEvent ev) {
                yLegend = ylegendField.getText();
                updatePlot(updateNoFits());
            }

            public void changedUpdate(DocumentEvent ev) {
                yLegend = ylegendField.getText();
                updatePlot(updateNoFits());
            }
        });
        ylegendField.setPreferredSize(new Dimension(250, 20));
        ylegendField.setHorizontalAlignment(JTextField.LEFT);
        ylegendField.setComponentPopupMenu(yaxispopup);
        ylegendField.setToolTipText("Right click to set Y-axis label preferences");

        ylegendgroup.add(ylegendField);
        SpringUtil.makeCompactGrid(ylegendgroup, 2, 1, 0, 0, 0, 0);
        mainpanelgroupd.add(ylegendgroup);

        //TRIM DATASET SUBPANEL

        JPanel trimdatapanel = new JPanel(new SpringLayout());
        trimdatapanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(mainBorderColor, 1), "Trim Data Samples", TitledBorder.CENTER, TitledBorder.TOP, b12, Color.DARK_GRAY));

        //TRIM DATA FROM HEAD STEPSIZE POPUP

        trimdataheadsteppopup = new JPopupMenu();
        JPanel trimdataheadsteppanel = new JPanel();
        trimdataheadstepmodel = new SpinnerListModel(integerspinnerscalelist);
        trimdataheadstepspinner = new JSpinner(trimdataheadstepmodel);
        trimdataheadstepspinner.setValue(intConvertToText(excludedHeadSamplesStep));
        trimdataheadstepspinner.addChangeListener(ev -> {
            excludedHeadSamplesStep = Integer.parseInt(((String) trimdataheadstepspinner.getValue()).trim());
            trimdataheadspinner.setModel(new SpinnerNumberModel(excludedHeadSamples, 0, null, excludedHeadSamplesStep));
            Prefs.set("plot.excludedHeadSamplesStep", excludedHeadSamplesStep);
        });

        JLabel trimdataheadsteplabel = new JLabel("Stepsize:");
        trimdataheadsteppanel.add(trimdataheadsteplabel);
        trimdataheadsteppanel.add(trimdataheadstepspinner);
        trimdataheadsteppopup.add(trimdataheadsteppanel);
        trimdataheadsteppopup.setLightWeightPopupEnabled(false);
        trimdataheadsteppopup.addPopupMenuListener(new PopupMenuListener() {
            public void popupMenuCanceled(PopupMenuEvent popupMenuEvent) {
                trimdataheadsteppopup.setVisible(false);
            }

            public void popupMenuWillBecomeInvisible(PopupMenuEvent popupMenuEvent) {
                trimdataheadsteppopup.setVisible(true);
            }

            public void popupMenuWillBecomeVisible(PopupMenuEvent popupMenuEvent) {
                trimdataheadsteppopup.setVisible(false);
            }
        });

        JLabel trimdataheadlabel = new JLabel("Head");
        trimdataheadlabel.setFont(p11);
        trimdataheadlabel.setHorizontalAlignment(JLabel.RIGHT);
        trimdataheadlabel.setPreferredSize(new Dimension(50, 25));
        trimdataheadlabel.setToolTipText("Right click to set spinner stepsize");
        trimdataheadlabel.setComponentPopupMenu(trimdataheadsteppopup);
        trimdatapanel.add(trimdataheadlabel);

        trimdataheadmodel = new SpinnerNumberModel(excludedHeadSamples, 0, null, excludedHeadSamplesStep);
        trimdataheadspinner = new JSpinner(trimdataheadmodel);
        trimdataheadspinner.setFont(p11);
        trimdataheadspinner.setComponentPopupMenu(trimdataheadsteppopup);
        trimdataheadspinner.setToolTipText("Right click to set spinner stepsize");
        trimdataheadspinner.addChangeListener(ev -> {
            excludedHeadSamples = (Integer) trimdataheadspinner.getValue();
            Prefs.set("plot.excludedHeadSamples", excludedHeadSamples);
            updatePlot(updateAllFits());
        });
        trimdataheadspinner.addMouseWheelListener(e -> {
            int newValue = (Integer) trimdataheadspinner.getValue() - e.getWheelRotation();
            if (newValue >= 0) trimdataheadspinner.setValue(newValue);
        });
        trimdatapanel.add(trimdataheadspinner);

        //TRIM DATA FROM TAIL STEPSIZE POPUP

        trimdatatailsteppopup = new JPopupMenu();
        JPanel trimdatatailsteppanel = new JPanel();
        trimdatatailstepmodel = new SpinnerListModel(integerspinnerscalelist);
        trimdatatailstepspinner = new JSpinner(trimdatatailstepmodel);
        trimdatatailstepspinner.setValue(intConvertToText(excludedTailSamplesStep));
        trimdatatailstepspinner.addChangeListener(ev -> {
            excludedTailSamplesStep = Integer.parseInt(((String) trimdatatailstepspinner.getValue()).trim());
            trimdatatailspinner.setModel(new SpinnerNumberModel(excludedTailSamples, 0, null, excludedTailSamplesStep));
            Prefs.set("plot.excludedTailSamplesStep", excludedTailSamplesStep);
        });

        JLabel trimdatatailsteplabel = new JLabel("Stepsize:");
        trimdatatailsteppanel.add(trimdatatailsteplabel);
        trimdatatailsteppanel.add(trimdatatailstepspinner);
        trimdatatailsteppopup.add(trimdatatailsteppanel);
        trimdatatailsteppopup.setLightWeightPopupEnabled(false);
        trimdatatailsteppopup.addPopupMenuListener(new PopupMenuListener() {
            public void popupMenuCanceled(PopupMenuEvent popupMenuEvent) {
                trimdatatailsteppopup.setVisible(false);
            }

            public void popupMenuWillBecomeInvisible(PopupMenuEvent popupMenuEvent) {
                trimdatatailsteppopup.setVisible(true);
            }

            public void popupMenuWillBecomeVisible(PopupMenuEvent popupMenuEvent) {
                trimdatatailsteppopup.setVisible(false);
            }
        });

        JLabel trimdatataillabel = new JLabel("Tail");
        trimdatataillabel.setFont(p11);
        trimdatataillabel.setHorizontalAlignment(JLabel.RIGHT);
        trimdatataillabel.setPreferredSize(new Dimension(50, 25));
        trimdatataillabel.setToolTipText("Right click to set spinner stepsize");
        trimdatataillabel.setComponentPopupMenu(trimdatatailsteppopup);
        trimdatapanel.add(trimdatataillabel);

        trimdatatailmodel = new SpinnerNumberModel(excludedTailSamples, 0, null, excludedTailSamplesStep);
        trimdatatailspinner = new JSpinner(trimdatatailmodel);
        trimdatatailspinner.setFont(p11);
        trimdatatailspinner.setComponentPopupMenu(trimdatatailsteppopup);
        trimdatatailspinner.setToolTipText("Right click to set spinner stepsize");
        trimdatatailspinner.addChangeListener(ev -> {
            excludedTailSamples = (Integer) trimdatatailspinner.getValue();
            Prefs.set("plot.excludedTailSamples", excludedTailSamples);
            updatePlot(updateAllFits());
        });
        trimdatatailspinner.addMouseWheelListener(e -> {
            int newValue = (Integer) trimdatatailspinner.getValue() - e.getWheelRotation();
            if (newValue >= 0) trimdatatailspinner.setValue(newValue);
        });
        trimdatapanel.add(trimdatatailspinner);
        SpringUtil.makeCompactGrid(trimdatapanel, 2, 2, 0, 0, 0, 0);
        mainpanelgroupd.add(trimdatapanel);

        SpringUtil.makeCompactGrid(mainpanelgroupd, 1, 3, 0, 0, 0, 0);
        mainpanel.add(mainpanelgroupd);


        // MAIN PANEL SCALING GROUP START (GROUP E)

        //X-SCALING SUBPANEL

        JPanel xscalingpanel = new JPanel(new SpringLayout());
        xscalingpanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(mainBorderColor, 1), "X-Axis Scaling", TitledBorder.CENTER, TitledBorder.TOP, b12, Color.DARK_GRAY));
        autoxButton = new JRadioButton("Auto X-range");
        autoxButton.setFont(p11);
        autoxButton.setHorizontalAlignment(JLabel.CENTER);
        firstxButton = new JRadioButton("First X-value as min");
        firstxButton.setFont(p11);
        firstxButton.setHorizontalAlignment(JLabel.CENTER);
        customxButton = new JRadioButton("Custom X-range");
        customxButton.setFont(p11);
        customxButton.setHorizontalAlignment(JLabel.CENTER);
        if (autoScaleX) {
            autoxButton.setSelected(true);
            firstxButton.setSelected(false);
            customxButton.setSelected(false);
        } else if (useFirstX) {
            autoxButton.setSelected(false);
            firstxButton.setSelected(true);
            customxButton.setSelected(false);
        } else {
            autoxButton.setSelected(false);
            firstxButton.setSelected(false);
            customxButton.setSelected(true);
        }

        xscalingpanel.add(autoxButton);
        xscalingpanel.add(firstxButton);
        xscalingpanel.add(customxButton);

        ButtonGroup xaxisradiogroup = new ButtonGroup();
        xaxisradiogroup.add(autoxButton);
        xaxisradiogroup.add(firstxButton);
        xaxisradiogroup.add(customxButton);
        autoxButton.addActionListener(ae -> {
            autoScaleX = true;
            useFirstX = false;
            updatePlot(updateAllFits());
        });
        firstxButton.addActionListener(ae -> {
            autoScaleX = false;
            useFirstX = true;
            updatePlot(updateAllFits());
        });
        customxButton.addActionListener(ae -> {
            autoScaleX = false;
            useFirstX = false;
            updatePlot(updateAllFits());
        });

        ImageIcon grabautoIcon = createImageIcon("astroj/images/grabautoscale.png", "Transfer autoscale values to custom scale values");

        JPanel grabautoxpanel = new JPanel(new SpringLayout());
        grabautoxbutton = new JButton(grabautoIcon);
        grabautoxbutton.setMargin(new Insets(0, 0, 0, 0));
        Dimension d = new Dimension(60, 15);
        grabautoxbutton.setMaximumSize(d);
        grabautoxpanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
        grabautoxbutton.setToolTipText("Transfer 'Auto X-range' min and max values to 'Custom X-range'");

        grabautoxbutton.addActionListener(e -> {
            xMin = xautoscalemin;
            xminspinner.setValue(xMin);
            xMax = xautoscalemax;
            xmaxspinner.setValue(xMax);
            xWidth = xautoscalemax - xautoscalemin;
            if (xWidth < 0.0001) xWidth = 0.0001;
            xwidthspinner.setValue(xWidth);
            autoxButton.setSelected(false);
            firstxButton.setSelected(false);
            customxButton.setSelected(true);
            autoScaleX = false;
            useFirstX = false;
            updatePlot(updateNoFits());
        });
        grabautoxpanel.add(grabautoxbutton);

        SpringUtil.makeCompactGrid(grabautoxpanel, 1, 1, 30, 0, 10, 2);
        xscalingpanel.add(grabautoxpanel);

        //XWIDTH STEPSIZE POPUP


        JPanel xwidthpanel = new JPanel(new SpringLayout());
        JLabel xwidthlabel = new JLabel("X-width");
        xwidthlabel.setFont(p11);
        xwidthlabel.setHorizontalAlignment(JLabel.RIGHT);
        xwidthlabel.setPreferredSize(new Dimension(60, 25));
        xwidthlabel.setComponentPopupMenu(xsteppopup);
        xwidthlabel.setToolTipText("Right click to set spinner stepsize");
        xwidthpanel.add(xwidthlabel);
        if (xWidth < 0.0001) xWidth = 0.0001;
        xwidthspinnermodel = new SpinnerNumberModel(xWidth, 0.0001, null, xStep);
        xwidthspinner = new JSpinner(xwidthspinnermodel);
        xwidthspinner.setFont(p11);
        xwidthspinner.setEditor(new JSpinner.NumberEditor(xwidthspinner, "########0.######"));
        xwidthspinner.setPreferredSize(new Dimension(100, 25));
        xwidthspinner.setToolTipText("Right click to set spinner stepsize");
        xwidthspinner.addChangeListener(ev -> {
            xWidth = (Double) xwidthspinner.getValue();
            updatePlot(updateNoFits());
        });
        xwidthspinner.addMouseWheelListener(e -> {
            double newvalue = (Double) xwidthspinner.getValue() - e.getWheelRotation() * xStep;
            if (newvalue >= 0.0001) xwidthspinner.setValue(newvalue);
        });
        xwidthpanel.add(xwidthspinner);
        xwidthspinner.setComponentPopupMenu(xsteppopup);
        SpringUtil.makeCompactGrid(xwidthpanel, 1, 2, 2, 2, 0, 0);
        xscalingpanel.add(xwidthpanel);

        JPanel customxmaxpanel = new JPanel(new SpringLayout());
        JLabel xmaxlabel = new JLabel("X-max");
        xmaxlabel.setFont(p11);
        xmaxlabel.setHorizontalAlignment(JLabel.RIGHT);
        xmaxlabel.setPreferredSize(new Dimension(50, 25));
        xmaxlabel.setToolTipText("Right click to set spinner stepsize");
        xmaxlabel.setComponentPopupMenu(xsteppopup);
        customxmaxpanel.add(xmaxlabel);

        xmaxspinnermodel = new SpinnerNumberModel(xMax, null, null, xStep);
        xmaxspinner = new JSpinner(xmaxspinnermodel);
        xmaxspinner.setFont(p11);
        xmaxspinner.setEditor(new JSpinner.NumberEditor(xmaxspinner, "########0.######"));
        xmaxspinner.setPreferredSize(new Dimension(100, 25));
        xmaxspinner.setComponentPopupMenu(xsteppopup);
        xmaxspinner.setToolTipText("Right click to set spinner stepsize");
        xmaxspinner.addChangeListener(ev -> {
            xMax = (Double) xmaxspinner.getValue();
            updatePlot(updateNoFits());
        });
        xmaxspinner.addMouseWheelListener(e -> xmaxspinner.setValue((Double) xmaxspinner.getValue() - e.getWheelRotation() * xStep));
        customxmaxpanel.add(xmaxspinner);
        SpringUtil.makeCompactGrid(customxmaxpanel, 1, 2, 2, 2, 0, 0);
        xscalingpanel.add(customxmaxpanel);

        JPanel xmultiplierpanel = new JPanel(new SpringLayout());
        xmultiplierpanel.setBorder(BorderFactory.createLineBorder(Color.lightGray));
        JLabel xmultiplierlabel = new JLabel("X x 1E");
        xmultiplierlabel.setFont(p11);
        xmultiplierlabel.setHorizontalAlignment(JLabel.RIGHT);
        xmultiplierpanel.setMaximumSize(new Dimension(250, 30));
        xmultiplierpanel.add(xmultiplierlabel);
        xmultiplierlabel.setToolTipText("Sets the X-axis multiplication factor");
        xmultipliermodel = new SpinnerNumberModel(xExponent, null, null, 1);
        xmultiplierspinner = new JSpinner(xmultipliermodel);
        xmultiplierspinner.setFont(p11);
        xmultiplierspinner.setMaximumSize(new Dimension(35, 25));
        xmultiplierspinner.setToolTipText("Sets the X-axis multiplication factor");
        xmultiplierspinner.addChangeListener(ev -> {
            xExponent = (Integer) xmultiplierspinner.getValue();
            updatePlot(updateNoFits());
        });
        xmultiplierspinner.addMouseWheelListener(e -> xmultiplierspinner.setValue((Integer) xmultiplierspinner.getValue() - e.getWheelRotation()));
        xmultiplierpanel.add(xmultiplierspinner);
        SpringUtil.makeCompactGrid(xmultiplierpanel, 1, 2, 2, 2, 2, 0);
        xscalingpanel.add(xmultiplierpanel);

        ImageIcon grabfirstxIcon = createImageIcon("astroj/images/grabautoscale.png", "Transfer autoscale values to custom scale values");
        JPanel grabfirstxpanel = new JPanel(new SpringLayout());
        JButton grabfirstxbutton = new JButton(grabfirstxIcon);
        grabfirstxbutton.setMargin(new Insets(0, 0, 0, 0));
        grabfirstxbutton.setMaximumSize(new Dimension(60, 15));
        grabfirstxpanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
        grabfirstxbutton.setToolTipText("Transfer 'First-X' min and max values to 'Custom X-range'");

        grabfirstxbutton.addActionListener(e -> {
            xMin = firstXmin;
            xminspinner.setValue(firstXmin);
            xMax = firstXmin + xWidth;
            xmaxspinner.setValue(xMax);
            autoxButton.setSelected(false);
            firstxButton.setSelected(false);
            customxButton.setSelected(true);
            autoScaleX = false;
            useFirstX = false;
            updatePlot(updateNoFits());
        });
        grabfirstxpanel.add(grabfirstxbutton);

        SpringUtil.makeCompactGrid(grabfirstxpanel, 1, 1, 72, 2, 22, 0);
        xscalingpanel.add(grabfirstxpanel);


        JPanel customxminpanel = new JPanel(new SpringLayout());
        JLabel xminlabel = new JLabel("X-min");
        xminlabel.setFont(p11);
        xminlabel.setHorizontalAlignment(JLabel.RIGHT);
        xminlabel.setPreferredSize(new Dimension(50, 25));
        xminlabel.setToolTipText("Right click to set spinner stepsize");
        xminlabel.setComponentPopupMenu(xsteppopup);
        customxminpanel.add(xminlabel);

        xminspinnermodel = new SpinnerNumberModel(xMin, null, null, xStep);
        xminspinner = new JSpinner(xminspinnermodel);
        xminspinner.setFont(p11);
        xminspinner.setEditor(new JSpinner.NumberEditor(xminspinner, "########0.######"));
        xminspinner.setPreferredSize(new Dimension(100, 25));
        xminspinner.setComponentPopupMenu(xsteppopup);
        xminspinner.setToolTipText("Right click to set spinner stepsize");
        xminspinner.addChangeListener(ev -> {
            xMin = (Double) xminspinner.getValue();
            updatePlot(updateNoFits());
        });
        xminspinner.addMouseWheelListener(e -> xminspinner.setValue((Double) xminspinner.getValue() - e.getWheelRotation() * xStep));
        customxminpanel.add(xminspinner);
        SpringUtil.makeCompactGrid(customxminpanel, 1, 2, 2, 2, 0, 0);
        xscalingpanel.add(customxminpanel);

        SpringUtil.makeCompactGrid(xscalingpanel, 3, 3, 0, 0, 0, 0);

        mainpanelgroupe.add(xscalingpanel);

        //Y-SCALING SUBPANEL

        JPanel yscalingpanel = new JPanel(new SpringLayout());
        yscalingpanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(mainBorderColor, 1), "Y-Axis Scaling", TitledBorder.CENTER, TitledBorder.TOP, b12, Color.DARK_GRAY));
        autoyButton = new JRadioButton("Auto Y-range");
        autoyButton.setFont(p11);
        autoyButton.setHorizontalAlignment(JLabel.CENTER);
        customyButton = new JRadioButton("Custom Y-range");
        customyButton.setFont(p11);
        customyButton.setHorizontalAlignment(JLabel.CENTER);
        if (autoScaleY) {
            autoyButton.setSelected(true);
            customyButton.setSelected(false);
        } else {
            autoyButton.setSelected(false);
            customyButton.setSelected(true);
        }

        yscalingpanel.add(autoyButton);
        yscalingpanel.add(customyButton);

        ButtonGroup yaxisradiogroup = new ButtonGroup();
        yaxisradiogroup.add(autoyButton);
        yaxisradiogroup.add(customyButton);
        autoyButton.addActionListener(ae -> {
            autoScaleY = true;
            updatePlot(updateNoFits());
        });
        customyButton.addActionListener(ae -> {
            autoScaleY = false;
            updatePlot(updateNoFits());
        });

        JPanel grabautoypanel = new JPanel(new SpringLayout());
        grabautoybutton = new JButton(grabautoIcon);
        grabautoybutton.setMargin(new Insets(0, 0, 0, 0));
        grabautoybutton.setMaximumSize(d);
        grabautoypanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
        grabautoybutton.setToolTipText("Transfer auto scale Y-range values to custom scale Y-range values");
        grabautoybutton.addActionListener(e -> {
            yMin = yautoscalemin;
            yminspinner.setValue(yMin);
            yMax = yautoscalemax;
            ymaxspinner.setValue(yMax);
            autoyButton.setSelected(false);
            customyButton.setSelected(true);
            autoScaleY = false;
            updatePlot(updateNoFits());
        });
        grabautoypanel.add(grabautoybutton);
        SpringUtil.makeCompactGrid(grabautoypanel, 1, 1, 30, 0, 10, 2);
        yscalingpanel.add(grabautoypanel);

        //YMAX STEPSIZE POPUP

        ymaxsteppopup = new JPopupMenu();
        JPanel ymaxsteppanel = new JPanel();
        ymaxstepmodel = new SpinnerListModel(spinnerscalelist);
        ymaxstepspinner = new JSpinner(ymaxstepmodel);
        ymaxstepspinner.setValue(convertToText(yMaxStep));
        ymaxstepspinner.addChangeListener(ev -> {
            double value = IJU.getTextSpinnerDoubleValue(ymaxstepspinner);
            if (Double.isNaN(value)) return;
            yMaxStep = value;
            ymaxspinner.setModel(new SpinnerNumberModel(yMax, null, null, yMaxStep));
            ymaxspinner.setEditor(new JSpinner.NumberEditor(ymaxspinner, "########0.######"));
            Prefs.set("plot.yMaxStep", yMaxStep);
        });

        JLabel ymaxsteplabel = new JLabel("Stepsize:");
        ymaxsteppanel.add(ymaxsteplabel);
        ymaxsteppanel.add(ymaxstepspinner);
        ymaxsteppopup.add(ymaxsteppanel);
        ymaxsteppopup.setLightWeightPopupEnabled(false);
        ymaxsteppopup.addPopupMenuListener(new PopupMenuListener() {
            public void popupMenuCanceled(PopupMenuEvent popupMenuEvent) {
                ymaxsteppopup.setVisible(false);
            }

            public void popupMenuWillBecomeInvisible(PopupMenuEvent popupMenuEvent) {
                ymaxsteppopup.setVisible(true);
            }

            public void popupMenuWillBecomeVisible(PopupMenuEvent popupMenuEvent) {
                ymaxsteppopup.setVisible(false);
            }
        });

        JPanel customymaxpanel = new JPanel(new SpringLayout());
        JLabel ymaxlabel = new JLabel("Y-max");
        ymaxlabel.setFont(p11);
        ymaxlabel.setHorizontalAlignment(JLabel.RIGHT);
        ymaxlabel.setPreferredSize(new Dimension(50, 25));
        ymaxlabel.setToolTipText("Right click to set spinner stepsize");
        ymaxlabel.setComponentPopupMenu(ymaxsteppopup);
        customymaxpanel.add(ymaxlabel);

        ymaxspinnermodel = new SpinnerNumberModel(yMax, null, null, yMaxStep);
        ymaxspinner = new JSpinner(ymaxspinnermodel);
        ymaxspinner.setFont(p11);
        ymaxspinner.setEditor(new JSpinner.NumberEditor(ymaxspinner, "########0.######"));
        ymaxspinner.setPreferredSize(new Dimension(100, 25));
        ymaxspinner.setComponentPopupMenu(ymaxsteppopup);
        ymaxspinner.setToolTipText("Right click to set spinner stepsize");
        ymaxspinner.addChangeListener(ev -> {
            yMax = (Double) ymaxspinner.getValue();
            updatePlot(updateNoFits());
        });
        ymaxspinner.addMouseWheelListener(e -> ymaxspinner.setValue((Double) ymaxspinner.getValue() - e.getWheelRotation() * yMaxStep));
        customymaxpanel.add(ymaxspinner);
        SpringUtil.makeCompactGrid(customymaxpanel, 1, 2, 2, 2, 0, 0);
        yscalingpanel.add(customymaxpanel);

        JPanel ymultiplierpanel = new JPanel(new SpringLayout());
        ymultiplierpanel.setBorder(BorderFactory.createLineBorder(Color.lightGray));
        JLabel ymultiplierlabel = new JLabel("Y x 1E");
        ymultiplierlabel.setFont(p11);
        ymultiplierlabel.setHorizontalAlignment(JLabel.RIGHT);
        ymultiplierpanel.setMaximumSize(new Dimension(250, 30));
        ymultiplierpanel.add(ymultiplierlabel);
        ymultiplierlabel.setToolTipText("Sets the Y-axis multiplication factor");
        ymultipliermodel = new SpinnerNumberModel(yExponent, null, null, 1);
        ymultiplierspinner = new JSpinner(ymultipliermodel);
        ymultiplierspinner.setFont(p11);
        ymultiplierspinner.setMaximumSize(new Dimension(35, 25));
        ymultiplierspinner.setToolTipText("Sets the Y-axis multiplication factor");
        ymultiplierspinner.addChangeListener(ev -> {
            yExponent = (Integer) ymultiplierspinner.getValue();
            updatePlot(updateNoFits());
        });
        ymultiplierspinner.addMouseWheelListener(e -> ymultiplierspinner.setValue((Integer) ymultiplierspinner.getValue() - e.getWheelRotation()));
        ymultiplierpanel.add(ymultiplierspinner);
        SpringUtil.makeCompactGrid(ymultiplierpanel, 1, 2, 2, 2, 2, 0);
        yscalingpanel.add(ymultiplierpanel);

        //YMIN STEPSIZE POPUP

        yminsteppopup = new JPopupMenu();
        JPanel yminsteppanel = new JPanel();
        yminstepmodel = new SpinnerListModel(spinnerscalelist);
        yminstepspinner = new JSpinner(yminstepmodel);
        yminstepspinner.setValue(convertToText(yMinStep));
        yminstepspinner.addChangeListener(ev -> {
            double value = IJU.getTextSpinnerDoubleValue(yminstepspinner);
            if (Double.isNaN(value)) return;
            yMinStep = value;
            yminspinner.setModel(new SpinnerNumberModel(yMin, null, null, yMinStep));
            yminspinner.setEditor(new JSpinner.NumberEditor(yminspinner, "########0.######"));
            Prefs.set("plot.yMinStep", yMinStep);
        });

        JLabel yminsteplabel = new JLabel("Stepsize:");
        yminsteppanel.add(yminsteplabel);
        yminsteppanel.add(yminstepspinner);
        yminsteppopup.add(yminsteppanel);
        yminsteppopup.setLightWeightPopupEnabled(false);
        yminsteppopup.addPopupMenuListener(new PopupMenuListener() {
            public void popupMenuCanceled(PopupMenuEvent popupMenuEvent) {
                yminsteppopup.setVisible(false);
            }

            public void popupMenuWillBecomeInvisible(PopupMenuEvent popupMenuEvent) {
                yminsteppopup.setVisible(true);
            }

            public void popupMenuWillBecomeVisible(PopupMenuEvent popupMenuEvent) {
                yminsteppopup.setVisible(false);
            }
        });


        JPanel customyminpanel = new JPanel(new SpringLayout());
        JLabel yminlabel = new JLabel("Y-min");
        yminlabel.setFont(p11);
        yminlabel.setHorizontalAlignment(JLabel.RIGHT);
        yminlabel.setComponentPopupMenu(yminsteppopup);
        yminlabel.setPreferredSize(new Dimension(50, 25));
        yminlabel.setToolTipText("Right click to set spinner stepsize");
        customyminpanel.add(yminlabel);

        yminspinnermodel = new SpinnerNumberModel(yMin, null, null, yMinStep);
        yminspinner = new JSpinner(yminspinnermodel);
        yminspinner.setFont(p11);
        yminspinner.setEditor(new JSpinner.NumberEditor(yminspinner, "########0.######"));
        yminspinner.setPreferredSize(new Dimension(100, 25));
        yminspinner.setComponentPopupMenu(yminsteppopup);
        yminspinner.setToolTipText("Right click to set spinner stepsize");
        yminspinner.addChangeListener(ev -> {
            yMin = (Double) yminspinner.getValue();
            updatePlot(updateNoFits());
        });
        yminspinner.addMouseWheelListener(e -> yminspinner.setValue((Double) yminspinner.getValue() - e.getWheelRotation() * yMinStep));
        customyminpanel.add(yminspinner);
        SpringUtil.makeCompactGrid(customyminpanel, 1, 2, 2, 2, 0, 0);
        yscalingpanel.add(customyminpanel);

        SpringUtil.makeCompactGrid(yscalingpanel, 3, 2, 0, 0, 0, 0);
        mainpanelgroupe.add(yscalingpanel);


        //PLOTSIZE SUBPANEL

        JPanel plotsizepanel = new JPanel(new SpringLayout());
        plotsizepanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(mainBorderColor, 1), "Plot Size", TitledBorder.CENTER, TitledBorder.TOP, b12, Color.DARK_GRAY));


        JLabel dummylabel10 = new JLabel("");
        dummylabel10.setFont(p11);
        dummylabel10.setHorizontalAlignment(JLabel.CENTER);
        dummylabel10.setPreferredSize(new Dimension(50, 25));
        plotsizepanel.add(dummylabel10);

        JLabel dummylabel11 = new JLabel("");
        dummylabel11.setFont(p11);
        dummylabel11.setHorizontalAlignment(JLabel.CENTER);
        dummylabel11.setPreferredSize(new Dimension(60, 25));
        plotsizepanel.add(dummylabel11);

        //PLOT HEIGHT STEPSIZE POPUP

        plotheightsteppopup = new JPopupMenu();
        JPanel plotheightsteppanel = new JPanel();
        plotheightstepmodel = new SpinnerListModel(integerspinnerscalelist);
        plotheightstepspinner = new JSpinner(plotheightstepmodel);
        plotheightstepspinner.setValue(intConvertToText(plotSizeYStep));
        plotheightstepspinner.addChangeListener(ev -> {
            plotSizeYStep = Integer.parseInt(((String) plotheightstepspinner.getValue()).trim());
            plotheightspinner.setModel(new SpinnerNumberModel(plotSizeY, 1, null, plotSizeYStep));
            Prefs.set("plot.plotSizeYStep", plotSizeYStep);
        });

        JLabel plotheightsteplabel = new JLabel("Stepsize:");
        plotheightsteppanel.add(plotheightsteplabel);
        plotheightsteppanel.add(plotheightstepspinner);
        plotheightsteppopup.add(plotheightsteppanel);
        plotheightsteppopup.setLightWeightPopupEnabled(false);
        plotheightsteppopup.addPopupMenuListener(new PopupMenuListener() {
            public void popupMenuCanceled(PopupMenuEvent popupMenuEvent) {
                plotheightsteppopup.setVisible(false);
            }

            public void popupMenuWillBecomeInvisible(PopupMenuEvent popupMenuEvent) {
                plotheightsteppopup.setVisible(true);
            }

            public void popupMenuWillBecomeVisible(PopupMenuEvent popupMenuEvent) {
                plotheightsteppopup.setVisible(false);
            }
        });

        JLabel plotheightlabel = new JLabel("Height");
        plotheightlabel.setFont(p11);
        plotheightlabel.setHorizontalAlignment(JLabel.RIGHT);
        plotheightlabel.setPreferredSize(new Dimension(50, 25));
        plotheightlabel.setToolTipText("Right click to set spinner stepsize");
        plotheightlabel.setComponentPopupMenu(plotheightsteppopup);
        plotsizepanel.add(plotheightlabel);

        plotheightspinnermodel = new SpinnerNumberModel(plotSizeY, 1, null, plotSizeYStep);
        plotheightspinner = new JSpinner(plotheightspinnermodel);
        plotheightspinner.setFont(p11);
        plotheightspinner.setComponentPopupMenu(plotheightsteppopup);
        plotheightspinner.setPreferredSize(new Dimension(50, 25));
        plotheightspinner.setToolTipText("Right click to set spinner stepsize");
        plotheightspinner.addChangeListener(ev -> {
            plotSizeY = (Integer) plotheightspinner.getValue();
            updatePlot(updateNoFits());
        });
        plotheightspinner.addMouseWheelListener(e -> {
            int newValue = (Integer) plotheightspinner.getValue() - e.getWheelRotation() * plotSizeYStep;
            if (newValue > 0) plotheightspinner.setValue(newValue);
        });
        plotsizepanel.add(plotheightspinner);

        //PLOT WIDTH STEPSIZE POPUP

        plotwidthsteppopup = new JPopupMenu();
        JPanel plotwidthsteppanel = new JPanel();
        plotwidthstepmodel = new SpinnerListModel(integerspinnerscalelist);
        plotwidthstepspinner = new JSpinner(plotwidthstepmodel);
        plotwidthstepspinner.setValue(intConvertToText(plotSizeXStep));
        plotwidthstepspinner.addChangeListener(ev -> {
            plotSizeXStep = Integer.parseInt(((String) plotwidthstepspinner.getValue()).trim());
            plotwidthspinner.setModel(new SpinnerNumberModel(plotSizeX, 1, null, plotSizeXStep));
            Prefs.set("plot.plotSizeXStep", plotSizeXStep);
        });
        JLabel plotwidthsteplabel = new JLabel("Stepsize:");
        plotwidthsteppanel.add(plotwidthsteplabel);
        plotwidthsteppanel.add(plotwidthstepspinner);
        plotwidthsteppopup.add(plotwidthsteppanel);
        plotwidthsteppopup.setLightWeightPopupEnabled(false);
        plotwidthsteppopup.addPopupMenuListener(new PopupMenuListener() {
            public void popupMenuCanceled(PopupMenuEvent popupMenuEvent) {
                plotwidthsteppopup.setVisible(false);
            }

            public void popupMenuWillBecomeInvisible(PopupMenuEvent popupMenuEvent) {
                plotwidthsteppopup.setVisible(true);
            }

            public void popupMenuWillBecomeVisible(PopupMenuEvent popupMenuEvent) {
                plotwidthsteppopup.setVisible(false);
            }
        });


        JLabel plotwidthlabel = new JLabel("Width");
        plotwidthlabel.setFont(p11);
        plotwidthlabel.setHorizontalAlignment(JLabel.RIGHT);
        plotwidthlabel.setPreferredSize(new Dimension(50, 25));
        plotwidthlabel.setToolTipText("Right click to set spinner stepsize");
        plotwidthlabel.setComponentPopupMenu(plotwidthsteppopup);
        plotsizepanel.add(plotwidthlabel);

        plotwidthspinnermodel = new SpinnerNumberModel(plotSizeX, 1, null, plotSizeXStep);
        plotwidthspinner = new JSpinner(plotwidthspinnermodel);
        plotwidthspinner.setFont(p11);
        plotwidthspinner.setComponentPopupMenu(plotwidthsteppopup);
        plotwidthspinner.setToolTipText("Right click to set spinner stepsize");
        plotwidthspinner.addChangeListener(ev -> {
            plotSizeX = (Integer) plotwidthspinner.getValue();
            updatePlot(updateNoFits());
        });

        plotwidthspinner.addMouseWheelListener(e -> {
            int newValue = (Integer) plotwidthspinner.getValue() - e.getWheelRotation() * plotSizeXStep;
            if (newValue > 0) plotwidthspinner.setValue(newValue);
        });
        plotsizepanel.add(plotwidthspinner);
        SpringUtil.makeCompactGrid(plotsizepanel, 3, 2, 0, 0, 0, 0);
        mainpanelgroupe.add(plotsizepanel);


        //FINALIZE GROUP E PANEL (SCALING PANEL)

        SpringUtil.makeCompactGrid(mainpanelgroupe, 1, 3, 0, 0, 0, 0);
        mainpanel.add(mainpanelgroupe);

        // MAIN PANEL PHASE FOLDING PANEL START

        T0steppopup = new JPopupMenu();
        JPanel T0steppanel = new JPanel();
        T0stepspinnermodel = new SpinnerListModel(spinnerscalelist);
        T0stepspinner = new JSpinner(T0stepspinnermodel);
        T0stepspinner.setValue(convertToText(T0Step));
        T0stepspinner.addChangeListener(ev -> {
            double value = IJU.getTextSpinnerDoubleValue(T0stepspinner);
            if (Double.isNaN(value)) return;
            T0Step = value;
            T0spinner.setModel(new SpinnerNumberModel(T0, 0.0, null, T0Step));
            T0spinner.setEditor(new JSpinner.NumberEditor(T0spinner, "########0.######"));
            Prefs.set("plot.T0Step", T0Step);
        });

        JLabel T0steplabel = new JLabel("Stepsize:");
        T0steppanel.add(T0steplabel);
        T0steppanel.add(T0stepspinner);
        T0steppopup.add(T0steppanel);
        T0steppopup.setLightWeightPopupEnabled(false);
        T0steppopup.addPopupMenuListener(new PopupMenuListener() {
            public void popupMenuCanceled(PopupMenuEvent popupMenuEvent) {
                T0steppopup.setVisible(false);
            }

            public void popupMenuWillBecomeInvisible(PopupMenuEvent popupMenuEvent) {
                T0steppopup.setVisible(true);
            }

            public void popupMenuWillBecomeVisible(PopupMenuEvent popupMenuEvent) {
                T0steppopup.setVisible(false);
            }
        });


        periodsteppopup = new JPopupMenu();
        JPanel periodsteppanel = new JPanel();
        periodstepspinnermodel = new SpinnerListModel(spinnerscalelist);
        periodstepspinner = new JSpinner(periodstepspinnermodel);
        periodstepspinner.setValue(convertToText(periodStep));
        periodstepspinner.addChangeListener(ev -> {
            double value = IJU.getTextSpinnerDoubleValue(periodstepspinner);
            if (Double.isNaN(value)) return;
            periodStep = value;
            periodspinner.setModel(new SpinnerNumberModel(period, 0.0001, null, periodStep));
            periodspinner.setEditor(new JSpinner.NumberEditor(periodspinner, "########0.########"));
            Prefs.set("plot.periodStep", periodStep);
        });

        JLabel periodsteplabel = new JLabel("Stepsize:");
        periodsteppanel.add(periodsteplabel);
        periodsteppanel.add(periodstepspinner);
        periodsteppopup.add(periodsteppanel);
        periodsteppopup.setLightWeightPopupEnabled(false);
        periodsteppopup.addPopupMenuListener(new PopupMenuListener() {
            public void popupMenuCanceled(PopupMenuEvent popupMenuEvent) {
                periodsteppopup.setVisible(false);
            }

            public void popupMenuWillBecomeInvisible(PopupMenuEvent popupMenuEvent) {
                periodsteppopup.setVisible(true);
            }

            public void popupMenuWillBecomeVisible(PopupMenuEvent popupMenuEvent) {
                periodsteppopup.setVisible(false);
            }
        });

        durationsteppopup = new JPopupMenu();
        JPanel durationsteppanel = new JPanel();
        durationstepspinnermodel = new SpinnerListModel(spinnerscalelist);
        durationstepspinner = new JSpinner(durationstepspinnermodel);
        durationstepspinner.setValue(convertToText(durationStep));
        durationstepspinner.addChangeListener(ev -> {
            double value = IJU.getTextSpinnerDoubleValue(durationstepspinner);
            if (Double.isNaN(value)) return;
            durationStep = value;
            durationspinner.setModel(new SpinnerNumberModel(duration, 0.0, null, durationStep));
            durationspinner.setEditor(new JSpinner.NumberEditor(durationspinner, "########0.######"));
            Prefs.set("plot.durationStep", durationStep);
        });

        JLabel durationsteplabel = new JLabel("Stepsize:");
        durationsteppanel.add(durationsteplabel);
        durationsteppanel.add(durationstepspinner);
        durationsteppopup.add(durationsteppanel);
        durationsteppopup.setLightWeightPopupEnabled(false);
        durationsteppopup.addPopupMenuListener(new PopupMenuListener() {
            public void popupMenuCanceled(PopupMenuEvent popupMenuEvent) {
                durationsteppopup.setVisible(false);
            }

            public void popupMenuWillBecomeInvisible(PopupMenuEvent popupMenuEvent) {
                durationsteppopup.setVisible(true);
            }

            public void popupMenuWillBecomeVisible(PopupMenuEvent popupMenuEvent) {
                durationsteppopup.setVisible(false);
            }
        });

        JPanel phasefoldpanel = new JPanel(new SpringLayout());
        TitledBorder phasefoldborder = BorderFactory.createTitledBorder(BorderFactory.createLineBorder(mainBorderColor, 1), "Phase Folding", TitledBorder.CENTER, TitledBorder.TOP, b12, Color.DARK_GRAY);
        phasefoldpanel.setBorder(phasefoldborder);

        unphasedButton = new JRadioButton("Unphased");
        unphasedButton.setFont(p11);
        unphasedButton.setHorizontalAlignment(JLabel.CENTER);
        dayssincetcButton = new JRadioButton("Days Since Tc");
        dayssincetcButton.setFont(p11);
        dayssincetcButton.setHorizontalAlignment(JLabel.CENTER);
        hourssincetcButton = new JRadioButton("Hours Since Tc");
        hourssincetcButton.setFont(p11);
        hourssincetcButton.setHorizontalAlignment(JLabel.CENTER);
        orbitalphaseButton = new JRadioButton("Phase");
        orbitalphaseButton.setFont(p11);
        orbitalphaseButton.setHorizontalAlignment(JLabel.CENTER);

        if (showXAxisNormal) {
            unphasedButton.setSelected(true);
            dayssincetcButton.setSelected(false);
            hourssincetcButton.setSelected(false);
            orbitalphaseButton.setSelected(false);
            Prefs.set("plot.showXAxisNormal", showXAxisNormal);
        } else if (showXAxisAsDaysSinceTc) {
            unphasedButton.setSelected(false);
            dayssincetcButton.setSelected(true);
            hourssincetcButton.setSelected(false);
            orbitalphaseButton.setSelected(false);
        } else if (showXAxisAsHoursSinceTc) {
            unphasedButton.setSelected(false);
            dayssincetcButton.setSelected(false);
            hourssincetcButton.setSelected(true);
            orbitalphaseButton.setSelected(false);
        } else {
            unphasedButton.setSelected(false);
            dayssincetcButton.setSelected(false);
            hourssincetcButton.setSelected(false);
            orbitalphaseButton.setSelected(true);
        }

        phasefoldpanel.add(unphasedButton);
        phasefoldpanel.add(dayssincetcButton);
        phasefoldpanel.add(hourssincetcButton);
        phasefoldpanel.add(orbitalphaseButton);

        ButtonGroup phaseradiogroup = new ButtonGroup();
        phaseradiogroup.add(unphasedButton);
        phaseradiogroup.add(dayssincetcButton);
        phaseradiogroup.add(hourssincetcButton);
        phaseradiogroup.add(orbitalphaseButton);
        unphasedButton.addActionListener(ae -> {
            if (!showXAxisNormal && T0spinner != null && xBase > 0) {
                //IJ.log("xBase="+xBase);
                int epoch = (int)((xBase - T0)/period) + 1;
                skipPlotUpdate = true;
                vmarker2spinner.setValue(T0 + period * (epoch) + duration/48.0 - (int)xBase);
                vmarker1spinner.setValue(T0 + period * (epoch) - duration/48.0 - (int)xBase);
                dmarker3spinner.setValue(T0 + period * (epoch) + duration/48.0 - (int)xBase);
                dmarker2spinner.setValue(T0 + period * (epoch) - duration/48.0 - (int)xBase);
                useDMarker1CB.setSelected(false);
                useDMarker4CB.setSelected(false);
                skipPlotUpdate = false;
            }
            showXAxisNormal = true;
            showXAxisAsDaysSinceTc = false;
            showXAxisAsHoursSinceTc = false;
            showXAxisAsPhase = false;
            T0spinner.setEnabled(false);
            periodspinner.setEnabled(false);
            durationspinner.setEnabled(false);
            twoxPeriodCB.setEnabled(false);
            oddNotEvenCB.setEnabled(false);
            t0panel.setEnabled(false);
            periodpanel.setEnabled(false);
            durationpanel.setEnabled(false);
            Prefs.set("plot.showXAxisNormal", showXAxisNormal);
            Prefs.set("plot.showXAxisAsPhase", showXAxisAsPhase);
            Prefs.set("plot.showXAxisAsHoursSinceTc", showXAxisAsHoursSinceTc);
            Prefs.set("plot.showXAxisAsDaysSinceTc", showXAxisAsDaysSinceTc);
            updatePlot(updateAllFits());
        });
        dayssincetcButton.addActionListener(ae -> {
            showXAxisNormal = false;
            showXAxisAsDaysSinceTc = true;
            showXAxisAsHoursSinceTc = false;
            showXAxisAsPhase = false;
            vMarker1Value = -duration / 48.0;
            skipPlotUpdate = true;
            if (vmarker1spinner != null && !showXAxisNormal) vmarker1spinner.setValue(vMarker1Value);
            vMarker2Value = duration / 48.0;
            if (vmarker1spinner != null && !showXAxisNormal) vmarker2spinner.setValue(vMarker2Value);
            dMarker2Value = -duration / 48.0;
            if (vmarker1spinner != null && !showXAxisNormal) dmarker2spinner.setValue(dMarker2Value);
            dMarker3Value = duration / 48.0;
            if (vmarker1spinner != null && !showXAxisNormal) dmarker3spinner.setValue(dMarker3Value);
            useDMarker1CB.setSelected(false);
            useDMarker4CB.setSelected(false);
            skipPlotUpdate = false;
            t0panel.setEnabled(true);
            periodpanel.setEnabled(true);
            durationpanel.setEnabled(true);
            T0spinner.setEnabled(true);
            periodspinner.setEnabled(true);
            durationspinner.setEnabled(true);
            twoxPeriodCB.setEnabled(true);
            oddNotEvenCB.setEnabled(twoxPeriod);
            Prefs.set("plot.showXAxisNormal", showXAxisNormal);
            Prefs.set("plot.showXAxisAsPhase", showXAxisAsPhase);
            Prefs.set("plot.showXAxisAsHoursSinceTc", showXAxisAsHoursSinceTc);
            Prefs.set("plot.showXAxisAsDaysSinceTc", showXAxisAsDaysSinceTc);
            Prefs.set("plot.vMarker1Value", vMarker1Value);
            Prefs.set("plot.vMarker2Value", vMarker2Value);
            updatePlot(updateAllFits());
        });
        hourssincetcButton.addActionListener(ae -> {
            showXAxisNormal = false;
            showXAxisAsDaysSinceTc = false;
            showXAxisAsHoursSinceTc = true;
            showXAxisAsPhase = false;
            t0panel.setEnabled(true);
            periodpanel.setEnabled(true);
            durationpanel.setEnabled(true);
            T0spinner.setEnabled(true);
            periodspinner.setEnabled(true);
            durationspinner.setEnabled(true);
            twoxPeriodCB.setEnabled(true);
            oddNotEvenCB.setEnabled(twoxPeriod);
            skipPlotUpdate = true;
            vMarker1Value = -duration / 2.0;
            if (vmarker1spinner != null && !showXAxisNormal) vmarker1spinner.setValue(vMarker1Value);
            vMarker2Value = duration / 2.0;
            if (vmarker1spinner != null && !showXAxisNormal) vmarker2spinner.setValue(vMarker2Value);
            dMarker2Value = -duration / 2.0;
            if (vmarker1spinner != null && !showXAxisNormal) dmarker2spinner.setValue(dMarker2Value);
            dMarker3Value = duration / 2.0;
            if (vmarker1spinner != null && !showXAxisNormal) dmarker3spinner.setValue(dMarker3Value);
            useDMarker1CB.setSelected(false);
            useDMarker4CB.setSelected(false);
            skipPlotUpdate = false;
            Prefs.set("plot.showXAxisNormal", showXAxisNormal);
            Prefs.set("plot.showXAxisAsPhase", showXAxisAsPhase);
            Prefs.set("plot.showXAxisAsHoursSinceTc", showXAxisAsHoursSinceTc);
            Prefs.set("plot.showXAxisAsDaysSinceTc", showXAxisAsDaysSinceTc);
            Prefs.set("plot.vMarker1Value", vMarker1Value);
            Prefs.set("plot.vMarker2Value", vMarker2Value);
            updatePlot(updateAllFits());
        });
        orbitalphaseButton.addActionListener(ae -> {
            showXAxisNormal = false;
            showXAxisAsDaysSinceTc = false;
            showXAxisAsHoursSinceTc = false;
            showXAxisAsPhase = true;
            t0panel.setEnabled(true);
            periodpanel.setEnabled(true);
            durationpanel.setEnabled(true);
            T0spinner.setEnabled(true);
            periodspinner.setEnabled(true);
            durationspinner.setEnabled(true);
            twoxPeriodCB.setEnabled(true);
            oddNotEvenCB.setEnabled(twoxPeriod);
            skipPlotUpdate = true;
            vMarker1Value = -duration / 48.0 / period;
            if (vmarker1spinner != null && !showXAxisNormal) vmarker1spinner.setValue(vMarker1Value);
            vMarker2Value = duration / 48.0 / period;
            if (vmarker1spinner != null && !showXAxisNormal) vmarker2spinner.setValue(vMarker2Value);
            dMarker2Value = -duration / 48.0 / period;
            if (vmarker1spinner != null && !showXAxisNormal) dmarker2spinner.setValue(dMarker2Value);
            dMarker3Value = duration / 48.0 / period;
            if (vmarker1spinner != null && !showXAxisNormal) dmarker3spinner.setValue(dMarker3Value);
            useDMarker1CB.setSelected(false);
            useDMarker4CB.setSelected(false);
            skipPlotUpdate = false;
            Prefs.set("plot.showXAxisNormal", showXAxisNormal);
            Prefs.set("plot.showXAxisAsPhase", showXAxisAsPhase);
            Prefs.set("plot.showXAxisAsHoursSinceTc", showXAxisAsHoursSinceTc);
            Prefs.set("plot.showXAxisAsDaysSinceTc", showXAxisAsDaysSinceTc);
            Prefs.set("plot.vMarker1Value", vMarker1Value);
            Prefs.set("plot.vMarker2Value", vMarker2Value);
            updatePlot(updateAllFits());
        });


        t0panel = new JPanel(new SpringLayout());
        TitledBorder t0border = BorderFactory.createTitledBorder(BorderFactory.createLineBorder(subBorderColor, 1), "T0 (Days)", TitledBorder.CENTER, TitledBorder.TOP, p11);
        t0panel.setBorder(t0border);

        T0spinnermodel = new SpinnerNumberModel(T0, 0.0, null, T0Step);

        T0spinner = new JSpinner(T0spinnermodel);
        T0spinner.setFont(p11);
        T0spinner.setEditor(new JSpinner.NumberEditor(T0spinner, "########0.######"));
        T0spinner.setPreferredSize(new Dimension(100, 25));
        T0spinner.setEnabled(true);
        T0spinner.setComponentPopupMenu(T0steppopup);
        T0spinner.setToolTipText("<html>" + "Phase folding reference epoch (days)" + "</html>");
        T0spinner.addChangeListener(ev -> {
            T0 = (Double) T0spinner.getValue();
            if (T0 < 0) T0 = 2450000;
            if (T0 < 8000.0) T0spinner.setValue(T0 + 2457000.0);
            Prefs.set("plot.T0", T0);
            updatePlot(updateAllFits());
        });
        T0spinner.addMouseWheelListener(e -> T0spinner.setValue((Double) T0spinner.getValue() - e.getWheelRotation() * T0Step));
        t0panel.add(T0spinner);
        SpringUtil.makeCompactGrid(t0panel, 1, t0panel.getComponentCount(), 0, 0, 0, 0);
        phasefoldpanel.add(t0panel);

        periodpanel = new JPanel(new SpringLayout());
        TitledBorder periodborder = BorderFactory.createTitledBorder(BorderFactory.createLineBorder(subBorderColor, 1), "Period (Days)", TitledBorder.CENTER, TitledBorder.TOP, p11);
        periodpanel.setBorder(periodborder);

        periodspinnermodel = new SpinnerNumberModel(period, 0.0001, null, periodStep);

        periodspinner = new JSpinner(periodspinnermodel);
        periodspinner.setFont(p11);
        periodspinner.setEditor(new JSpinner.NumberEditor(periodspinner, "########0.########"));
        periodspinner.setPreferredSize(new Dimension(100, 25));
        periodspinner.setEnabled(true);
        periodspinner.setComponentPopupMenu(periodsteppopup);
        periodspinner.setToolTipText("<html>" + "Phase folding reference epoch (days)" + "</html>");
        periodspinner.addChangeListener(ev -> {
            period = (Double) periodspinner.getValue();
            if (period < 0.0001) period = 0.0001;
            Prefs.set("plot.period", period);
            updatePlot(updateAllFits());
        });
        periodspinner.addMouseWheelListener(e -> periodspinner.setValue((Double) periodspinner.getValue() - e.getWheelRotation() * periodStep));
        periodpanel.add(periodspinner);
        SpringUtil.makeCompactGrid(periodpanel, 1, periodpanel.getComponentCount(), 0, 0, 0, 0);
        phasefoldpanel.add(periodpanel);

        durationpanel = new JPanel(new SpringLayout());
        TitledBorder durationborder = BorderFactory.createTitledBorder(BorderFactory.createLineBorder(subBorderColor, 1), "Duration (Hours)", TitledBorder.CENTER, TitledBorder.TOP, p11);
        durationpanel.setBorder(durationborder);

        durationspinnermodel = new SpinnerNumberModel(duration, 0.0, null, durationStep);

        durationspinner = new JSpinner(durationspinnermodel);
        durationspinner.setFont(p11);
        durationspinner.setEditor(new JSpinner.NumberEditor(durationspinner, "########0.######"));
        durationspinner.setPreferredSize(new Dimension(100, 25));
        durationspinner.setEnabled(true);
        durationspinner.setComponentPopupMenu(durationsteppopup);
        durationspinner.setToolTipText("<html>" + "Phase folding reference epoch (days)" + "</html>");
        durationspinner.addChangeListener(ev -> {
            duration = (Double) durationspinner.getValue();
            if (duration < 0) duration = 0.0;
            Prefs.set("plot.duration", duration);
            vMarker1Value = -duration / 48.0;
            if (vmarker1spinner != null && !showXAxisNormal) vmarker1spinner.setValue(vMarker1Value);
            vMarker2Value = duration / 48.0;
            if (vmarker1spinner != null && !showXAxisNormal) vmarker2spinner.setValue(vMarker2Value);
            dMarker2Value = -duration / 48.0;
            if (vmarker1spinner != null && !showXAxisNormal) dmarker2spinner.setValue(dMarker2Value);
            dMarker3Value = duration / 48.0;
            if (vmarker1spinner != null && !showXAxisNormal) dmarker3spinner.setValue(dMarker3Value);
            Prefs.set("plot.vMarker1Value", vMarker1Value);
            Prefs.set("plot.vMarker2Value", vMarker2Value);
            updatePlot(updateAllFits());
        });
        durationspinner.addMouseWheelListener(e -> durationspinner.setValue((Double) durationspinner.getValue() - e.getWheelRotation() * durationStep));
        durationpanel.add(durationspinner);

        SpringUtil.makeCompactGrid(durationpanel, 1, durationpanel.getComponentCount(), 0, 0, 0, 0);
        phasefoldpanel.add(durationpanel);

        twoxPeriodCB = new JCheckBox("2xP", twoxPeriod);
        twoxPeriodCB.setToolTipText("Show at 2 x Period to check odd/even.");
        twoxPeriodCB.addItemListener(e -> {
            if (e.getStateChange() == ItemEvent.DESELECTED) {
                twoxPeriod = false;
            } else if (e.getStateChange() == ItemEvent.SELECTED) twoxPeriod = true;
            oddNotEvenCB.setEnabled(twoxPeriod);
            Prefs.set("plot.twoxPeriod", twoxPeriod);
            updatePlot(updateAllFits());
        });
        phasefoldpanel.add(twoxPeriodCB);

        oddNotEvenCB = new JCheckBox("odd/even", oddNotEven);
        oddNotEvenCB.setToolTipText("Select to show odd transits. Deselect to show even transits.");
        oddNotEvenCB.addItemListener(e -> {
            if (e.getStateChange() == ItemEvent.DESELECTED) {
                oddNotEven = false;
            } else if (e.getStateChange() == ItemEvent.SELECTED) oddNotEven = true;
            Prefs.set("plot.oddNotEven", oddNotEven);
            updatePlot(updateAllFits());
        });
        phasefoldpanel.add(oddNotEvenCB);

        if (showXAxisNormal) {
            T0spinner.setEnabled(false);
            periodspinner.setEnabled(false);
            durationspinner.setEnabled(false);
            twoxPeriodCB.setEnabled(false);
            oddNotEvenCB.setEnabled(false);
            t0panel.setEnabled(false);
            periodpanel.setEnabled(false);
            durationpanel.setEnabled(false);
        }
        if (!twoxPeriod) {
            oddNotEvenCB.setEnabled(false);
        }

        SpringUtil.makeCompactGrid(phasefoldpanel, 1, phasefoldpanel.getComponentCount(), 2, 2, 0, 0);
        mainpanel.add(phasefoldpanel);

        // MAIN PANEL GROUP F START


        JPanel mfmarkerpanel = new JPanel(new SpringLayout());
        TitledBorder mfmarkerborder = BorderFactory.createTitledBorder(BorderFactory.createLineBorder(mainBorderColor, 1), "Meridian Flip", TitledBorder.CENTER, TitledBorder.TOP, b12, Color.DARK_GRAY);
        mfmarkerpanel.setBorder(mfmarkerborder);

        JPanel showmfmarkerpanel = new JPanel(new SpringLayout());
        TitledBorder showmfmarkerborder = BorderFactory.createTitledBorder(BorderFactory.createLineBorder(subBorderColor, 1), "Show", TitledBorder.CENTER, TitledBorder.TOP, p11);
        showmfmarkerpanel.setBorder(showmfmarkerborder);

        JLabel showmfmarkerlabel1 = new JLabel("  ");
        showmfmarkerlabel1.setFont(p11);
        showmfmarkerpanel.add(showmfmarkerlabel1);

        showMFMarkersCB = new JCheckBox("", showMFMarkers);
        showMFMarkersCB.setToolTipText("Show meridian flip marker on plot");
        showMFMarkersCB.addItemListener(e -> {
            if (e.getStateChange() == ItemEvent.DESELECTED) {
                showMFMarkers = false;
            } else if (e.getStateChange() == ItemEvent.SELECTED) showMFMarkers = true;
            updatePlot(updateNoFits());
        });
        showMFMarkersCB.setHorizontalAlignment(JLabel.CENTER);
        showmfmarkerpanel.add(showMFMarkersCB);

        JLabel showmfmarkerlabel2 = new JLabel("  ");
        showmfmarkerlabel2.setFont(p11);
        showmfmarkerpanel.add(showmfmarkerlabel2);

        SpringUtil.makeCompactGrid(showmfmarkerpanel, 1, showmfmarkerpanel.getComponentCount(), 2, 2, 0, 0);
        mfmarkerpanel.add(showmfmarkerpanel);

        JPanel mfmarker1panel = new JPanel(new SpringLayout());
        TitledBorder mfmarker1border = BorderFactory.createTitledBorder(BorderFactory.createLineBorder(subBorderColor, 1), "Flip Time", TitledBorder.CENTER, TitledBorder.TOP, p11);
        mfmarker1panel.setBorder(mfmarker1border);

        mfmarker1spinnermodel = new SpinnerNumberModel(mfMarker1Value, null, null, xStep);

        mfmarker1spinner = new JSpinner(mfmarker1spinnermodel);
        mfmarker1spinner.setFont(p11);
        mfmarker1spinner.setEditor(new JSpinner.NumberEditor(mfmarker1spinner, "########0.######"));
//                JComponent editor = new JSpinner.NumberEditor(vmarker1spinner);
//                vmarker1spinner.setEditor(editor);
        mfmarker1spinner.setPreferredSize(new Dimension(75, 25));
        mfmarker1spinner.setEnabled(true);
        mfmarker1spinner.setComponentPopupMenu(xsteppopup);
        mfmarker1spinner.setToolTipText("<html>" + "Enter meridian flip time in x-axis units" + "<br>" + "or enter UT time in HH:MM or HH:MM:SS format and press 'Enter'" + "<br>" + "---------------------------------------------" + "<br>" + "Right click to set spinner stepsize" + "</html>");
        mfmarker1spinner.addChangeListener(ev -> {
            showMFMarkersCB.setSelected(true);
            checkForUT(mfmarker1spinner);
            mfMarker1Value = (Double) mfmarker1spinner.getValue();
            updatePlot(updateAllFits());
        });
        mfmarker1spinner.addMouseWheelListener(e -> mfmarker1spinner.setValue((Double) mfmarker1spinner.getValue() - e.getWheelRotation() * xStep));
        mfmarker1panel.add(mfmarker1spinner);

        SpringUtil.makeCompactGrid(mfmarker1panel, 1, mfmarker1panel.getComponentCount(), 2, 2, 0, 0);
        mfmarkerpanel.add(mfmarker1panel);

        SpringUtil.makeCompactGrid(mfmarkerpanel, 1, mfmarkerpanel.getComponentCount(), 2, 2, 6, 2);
        mainpanelgroupf.add(mfmarkerpanel);


        JPanel detrendrangepanel = new JPanel(new SpringLayout());
        TitledBorder detrendrangetitle = BorderFactory.createTitledBorder(BorderFactory.createLineBorder(mainBorderColor, 1), "Fit and Normalize Region Selection", TitledBorder.CENTER, TitledBorder.TOP, b12, Color.DARK_GRAY);
        detrendrangepanel.setBorder(detrendrangetitle);

        //DETREND/NORMALIZE STEPSIZE POPUP


        JPanel showdmarkerspanel = new JPanel(new SpringLayout());
        TitledBorder showdmarkerstitle = BorderFactory.createTitledBorder(BorderFactory.createLineBorder(subBorderColor, 1), "Show", TitledBorder.CENTER, TitledBorder.TOP, p11);
        showdmarkerspanel.setBorder(showdmarkerstitle);

        JLabel showdmarkerslabel1 = new JLabel("  ");
        showdmarkerslabel1.setFont(p11);
        showdmarkerspanel.add(showdmarkerslabel1);

        showDMarkersCB = new JCheckBox("", showDMarkers);
        showDMarkersCB.setToolTipText("Show region markers on plot");
        showDMarkersCB.addItemListener(e -> {
            if (e.getStateChange() == ItemEvent.DESELECTED) {
                showDMarkers = false;
            } else if (e.getStateChange() == ItemEvent.SELECTED) showDMarkers = true;
            updatePlot(updateNoFits());
        });
        showDMarkersCB.setHorizontalAlignment(JLabel.CENTER);
        showdmarkerspanel.add(showDMarkersCB);

        JLabel showdmarkerslabel2 = new JLabel("  ");
        showdmarkerslabel2.setFont(p11);
        showdmarkerspanel.add(showdmarkerslabel2);

        SpringUtil.makeCompactGrid(showdmarkerspanel, 1, showdmarkerspanel.getComponentCount(), 2, 2, 0, 0);
        detrendrangepanel.add(showdmarkerspanel);

        JPanel dmarker1panel = new JPanel(new SpringLayout());
        TitledBorder dmarker1title = BorderFactory.createTitledBorder(BorderFactory.createLineBorder(subBorderColor, 1), "Left Trim", TitledBorder.CENTER, TitledBorder.TOP, p11);
        dmarker1title.setTitleJustification(TitledBorder.CENTER);
        dmarker1panel.setBorder(dmarker1title);

        useDMarker1CB = new JCheckBox("", useDMarker1);
        useDMarker1CB.setToolTipText("Enable left trim");
        useDMarker1CB.addItemListener(e -> {
            if (e.getStateChange() == ItemEvent.DESELECTED) {
                useDMarker1 = false;
            } else if (e.getStateChange() == ItemEvent.SELECTED) useDMarker1 = true;
            dmarker1spinner.setEnabled(useDMarker1);
            if (!skipPlotUpdate) updatePlot(updateAllFits());
        });
        useDMarker1CB.setHorizontalAlignment(JLabel.CENTER);
        dmarker1panel.add(useDMarker1CB);

        dmarker1spinnermodel = new SpinnerNumberModel(dMarker1Value, null, null, xStep);

        dmarker1spinner = new JSpinner(dmarker1spinnermodel);
        dmarker1spinner.setFont(p11);
        dmarker1spinner.setEditor(new JSpinner.NumberEditor(dmarker1spinner, "########0.######"));
//                JComponent editor = new JSpinner.NumberEditor(vmarker1spinner);
//                vmarker1spinner.setEditor(editor);
        dmarker1spinner.setPreferredSize(new Dimension(75, 25));
        dmarker1spinner.setEnabled(useDMarker1);
        dmarker1spinner.setComponentPopupMenu(xsteppopup);
        dmarker1spinner.setToolTipText("<html>" + "Enter detrend/normalize left trim location" + "<br>" + "or enter UT time in HH:MM or HH:MM:SS format and press 'Enter'" + "<br>" + "---------------------------------------------" + "<br>" + "SHORTCUT: &lt;Ctrl&gt;&lt;Shift&gt; Left Click or Drag in plot" + "<br>" + "---------------------------------------------" + "<br>" + "Right click to set spinner stepsize" + "</html>");
        dmarker1spinner.addChangeListener(ev -> {
            checkForUT(dmarker1spinner);
            dMarker1Value = (Double) dmarker1spinner.getValue();
            keepMarkersInOrder(1);
            if (!skipPlotUpdate) updatePlot(updateAllFits());
        });
        dmarker1spinner.addMouseWheelListener(e -> {
            if (useDMarker1) {
                dmarker1spinner.setValue((Double) dmarker1spinner.getValue() - e.getWheelRotation() * xStep);
            }
        });
        dmarker1panel.add(dmarker1spinner);

        SpringUtil.makeCompactGrid(dmarker1panel, 1, dmarker1panel.getComponentCount(), 2, 2, 0, 0);
        detrendrangepanel.add(dmarker1panel);

        JPanel dmarker2panel = new JPanel(new SpringLayout());
//                dmarker2panel.setBorder(BorderFactory.createLineBorder(Color.lightGray,1));
        TitledBorder dmarker2title = BorderFactory.createTitledBorder(BorderFactory.createLineBorder(subBorderColor, 1), "Left", TitledBorder.CENTER, TitledBorder.TOP, p11);
//                dmarker2title.setTitlePosition(TitledBorder.BOTTOM);
        dmarker2panel.setBorder(dmarker2title);

        dmarker2spinnermodel = new SpinnerNumberModel(dMarker2Value, null, null, xStep);

        dmarker2spinner = new JSpinner(dmarker2spinnermodel);
        dmarker2spinner.setFont(p11);
        dmarker2spinner.setEditor(new JSpinner.NumberEditor(dmarker2spinner, "########0.######"));
//                JComponent editor = new JSpinner.NumberEditor(vmarker1spinner);
//                vmarker1spinner.setEditor(editor);
        dmarker2spinner.setPreferredSize(new Dimension(75, 25));
        dmarker2spinner.setComponentPopupMenu(xsteppopup);
        dmarker2spinner.setToolTipText("<html>" + "Enter detrend/normalize left marker location" + "<br>" + "or enter UT time in HH:MM or HH:MM:SS format and press 'Enter'" + "<br>" + "---------------------------------------------" + "<br>" + "SHORTCUT: &lt;Ctrl&gt; Left Click or Drag in plot    " + "<br>" + "---------------------------------------------" + "<br>" + "Right click to set spinner stepsize" + "</html>");
        dmarker2spinner.addChangeListener(ev -> {
            checkForUT(dmarker2spinner);
            dMarker2Value = (Double) dmarker2spinner.getValue();

//                        if (shiftIsDown && !ignoreUpdate)
//                            {
//                            ignoreUpdate = true;
//                            dmarker3spinner.setValue((Double)(dMarker3Value + Math.abs(vMarker2Value-vMarker1Value)));
//                            }
//                        else
//                            {
//                            ignoreUpdate = false;
//                            }
            keepMarkersInOrder(2);
            if (!skipPlotUpdate) updatePlot(updateAllFits());
        });
        dmarker2spinner.addMouseWheelListener(e -> {
            dmarker2spinner.setValue((Double) dmarker2spinner.getValue() - e.getWheelRotation() * xStep);
            if (e.isShiftDown() || e.isControlDown() || e.isAltDown()) {
                dmarker3spinner.setValue((Double) dmarker3spinner.getValue() - e.getWheelRotation() * xStep);
            }
        });
        dmarker2panel.add(dmarker2spinner);

        SpringUtil.makeCompactGrid(dmarker2panel, 1, dmarker2panel.getComponentCount(), 2, 2, 0, 0);
        detrendrangepanel.add(dmarker2panel);


        JPanel dmarkercopypanel = new JPanel(new SpringLayout());
        TitledBorder dmarkercopytitle = BorderFactory.createTitledBorder(BorderFactory.createLineBorder(subBorderColor, 1), "Copy", TitledBorder.CENTER, TitledBorder.TOP, p11);
        dmarkercopytitle.setTitleJustification(TitledBorder.CENTER);
        dmarkercopypanel.setBorder(dmarkercopytitle);

        copyVMarkersIcon = createImageIcon("astroj/images/copymarkers.png", "Copy V. Markers Icon");
        JButton copyButton = new JButton(copyVMarkersIcon);
        copyButton.setMargin(new Insets(0, 0, 0, 0));
        copyButton.setToolTipText("Copy values from V. Markers 1 and 2 to Left and Right markers");
        copyButton.addActionListener(e -> {
            dmarker2spinner.setValue(vMarker1Value);
            dmarker3spinner.setValue(vMarker2Value);
            updatePlot(updateNoFits());
        });
        dmarkercopypanel.add(copyButton);
        SpringUtil.makeCompactGrid(dmarkercopypanel, 1, dmarkercopypanel.getComponentCount(), 2, 0, 2, 0);
        detrendrangepanel.add(dmarkercopypanel);

        JPanel dmarker3panel = new JPanel(new SpringLayout());
//                dmarker3panel.setBorder(BorderFactory.createLineBorder(Color.lightGray,1));
        TitledBorder dmarker3title = BorderFactory.createTitledBorder(BorderFactory.createLineBorder(subBorderColor, 1), "Right", TitledBorder.CENTER, TitledBorder.TOP, p11);
        dmarker3title.setTitleJustification(TitledBorder.CENTER);
//                dmarker3title.setTitlePosition(TitledBorder.BOTTOM);
        dmarker3panel.setBorder(dmarker3title);

        dmarker3spinnermodel = new SpinnerNumberModel(dMarker3Value, null, null, xStep);

        dmarker3spinner = new JSpinner(dmarker3spinnermodel);
        dmarker3spinner.setFont(p11);
        dmarker3spinner.setEditor(new JSpinner.NumberEditor(dmarker3spinner, "########0.######"));
//                JComponent editor = new JSpinner.NumberEditor(vmarker1spinner);
//                vmarker1spinner.setEditor(editor);
        dmarker3spinner.setPreferredSize(new Dimension(75, 25));
        dmarker3spinner.setComponentPopupMenu(xsteppopup);
        dmarker3spinner.setToolTipText("<html>" + "Enter detrend/normalize right marker location" + "<br>" + "or enter UT time in HH:MM or HH:MM:SS format and press 'Enter'" + "<br>" + "---------------------------------------------" + "<br>" + "SHORTCUT: &lt;Ctrl&gt; Right Click or Drag in plot   " + "<br>" + "---------------------------------------------" + "<br>" + "Right click to set spinner stepsize" + "</html>");
        dmarker3spinner.addMouseListener(new MouseListener() {
            public void mouseClicked(MouseEvent e) {
            }

            public void mousePressed(MouseEvent e) {
                if (e.isAltDown()) shiftIsDown = true;
                //else shiftIsDown = false;

            }

            public void mouseReleased(MouseEvent e) {
                if (e.isAltDown()) shiftIsDown = true;
                //else shiftIsDown = false;

            }

            public void mouseExited(MouseEvent e) {
            }

            public void mouseEntered(MouseEvent e) {
                //if (e.isAltDown()) shiftIsDown = true;
            }
        });
        dmarker3spinner.addChangeListener(ev -> {
            checkForUT(dmarker3spinner);
            dMarker3Value = (Double) dmarker3spinner.getValue();
//                        IJ.log("D3 Shift="+shiftIsDown);
//                        IJ.log("D3 IgnoreUpdate="+ignoreUpdate);
//                        if (shiftIsDown && !ignoreUpdate)
//                            {
//                            dmarker2spinner.setValue((Double)(dMarker2Value + Math.abs(vMarker2Value-vMarker1Value)));
//                            ignoreUpdate = true;
//                            }
//                        else
//                            {
//                            ignoreUpdate = false;
//                            }
            keepMarkersInOrder(3);
            if (!skipPlotUpdate) updatePlot(updateAllFits());
        });
        dmarker3spinner.addMouseWheelListener(e -> {
            dmarker3spinner.setValue((Double) dmarker3spinner.getValue() - e.getWheelRotation() * xStep);
            if (e.isShiftDown() || e.isControlDown() || e.isAltDown()) {
                dmarker2spinner.setValue((Double) dmarker2spinner.getValue() - e.getWheelRotation() * xStep);
            }
        });
        dmarker3panel.add(dmarker3spinner);

        SpringUtil.makeCompactGrid(dmarker3panel, 1, dmarker3panel.getComponentCount(), 2, 2, 0, 0);
        detrendrangepanel.add(dmarker3panel);

        JPanel dmarker4panel = new JPanel(new SpringLayout());
//                dmarker4panel.setBorder(BorderFactory.createLineBorder(Color.lightGray,1));
        TitledBorder dmarker4title = BorderFactory.createTitledBorder(BorderFactory.createLineBorder(subBorderColor, 1), "Right Trim", TitledBorder.CENTER, TitledBorder.TOP, p11);
        dmarker4title.setTitleJustification(TitledBorder.CENTER);
//                dmarker4title.setTitlePosition(TitledBorder.BOTTOM);
        dmarker4panel.setBorder(dmarker4title);

        dmarker4spinnermodel = new SpinnerNumberModel(dMarker4Value, null, null, xStep);

        dmarker4spinner = new JSpinner(dmarker4spinnermodel);
        dmarker4spinner.setFont(p11);
        dmarker4spinner.setEditor(new JSpinner.NumberEditor(dmarker4spinner, "########0.######"));
        dmarker4spinner.setPreferredSize(new Dimension(75, 25));
        dmarker4spinner.setEnabled(useDMarker4);
        dmarker4spinner.setComponentPopupMenu(xsteppopup);
        dmarker4spinner.setToolTipText("<html>" + "Enter detrend/normalize right trim location" + "<br>" + "or enter UT time in HH:MM or HH:MM:SS format and press 'Enter'" + "<br>" + "---------------------------------------------" + "<br>" + "SHORTCUT: &lt;Ctrl&gt;&lt;Shift&gt; Right Click or Drag in plot" + "<br>" + "---------------------------------------------" + "<br>" + "Right click to set spinner stepsize" + "</html>");
        dmarker4spinner.addChangeListener(ev -> {
            checkForUT(dmarker4spinner);
            dMarker4Value = (Double) dmarker4spinner.getValue();
            keepMarkersInOrder(4);
            if (!skipPlotUpdate)  updatePlot(updateAllFits());
        });
        dmarker4spinner.addMouseWheelListener(e -> {
            if (useDMarker4) {
                dmarker4spinner.setValue((Double) dmarker4spinner.getValue() - e.getWheelRotation() * xStep);
            }
        });
        dmarker4panel.add(dmarker4spinner);

        useDMarker4CB = new JCheckBox("", useDMarker4);
        useDMarker4CB.setToolTipText("Enable right trim");
        useDMarker4CB.addItemListener(e -> {
            if (e.getStateChange() == ItemEvent.DESELECTED) {
                useDMarker4 = false;
            } else if (e.getStateChange() == ItemEvent.SELECTED) useDMarker4 = true;
            dmarker4spinner.setEnabled(useDMarker4);
            if (!skipPlotUpdate) updatePlot(updateAllFits());
        });
        useDMarker4CB.setHorizontalAlignment(JLabel.CENTER);
        dmarker4panel.add(useDMarker4CB);

        SpringUtil.makeCompactGrid(dmarker4panel, 1, dmarker4panel.getComponentCount(), 2, 2, 0, 0);
        detrendrangepanel.add(dmarker4panel);


        SpringUtil.makeCompactGrid(detrendrangepanel, 1, detrendrangepanel.getComponentCount(), 2, 2, 6, 2);
        mainpanelgroupf.add(detrendrangepanel);

//                JPanel closebuttonpanel = new JPanel (new SpringLayout());
//                closebutton = new JButton("        Exit        ");
//                closebutton.setForeground(Color.RED);
//                closebutton.addActionListener(new ActionListener() {
//                    public void actionPerformed(ActionEvent e) {
//                            saveAndClose(); }});
//                closebuttonpanel.add (closebutton);
//                SpringUtil.makeCompactGrid (closebuttonpanel, 1, 1, 6,15,0,0);
//                mainpanelgroupf.add (closebuttonpanel);


        JPanel morepanelspanel = new JPanel(new SpringLayout());
        TitledBorder morepanelstitle = BorderFactory.createTitledBorder(BorderFactory.createLineBorder(mainBorderColor, 1), "Other Panels", TitledBorder.CENTER, TitledBorder.TOP, b12, Color.DARK_GRAY);
        morepanelspanel.setBorder(morepanelstitle);
//                morepanelspanel.setPreferredSize(new Dimension(125, 25));

        JPanel line1morepanelspanel = new JPanel(new SpringLayout());

        updateplotbutton = new JButton("Redraw Plot");
        updateplotbutton.setToolTipText("redraws the plot and brings the panel to the front");
        updateplotbutton.setFont(p11);
        updateplotbutton.setPreferredSize(new Dimension(90, 20));
        updateplotbutton.addActionListener(e -> {
            totalPanOffsetX = 0.0;
            totalPanOffsetY = 0.0;
            newPanOffsetX = 0.0;
            newPanOffsetY = 0.0;
            leftDragReleased = false;
            zoom = 0.0;
            updatePlot(updateAllFits());
            plotWindow.toFront();
        });
        line1morepanelspanel.add(updateplotbutton);

        addastrodatabutton = new JButton(" Add Data ");
        addastrodatabutton.setToolTipText("Add new astronomical data columns to table.");
        addastrodatabutton.setFont(p11);
        addastrodatabutton.setPreferredSize(new Dimension(90, 20));
        addastrodatabutton.addActionListener(e -> {
            if (addAstroDataFrame != null && addAstroDataFrame.isShowing()) {
                addAstroDataFrame.setVisible(true);
            } else {
                setSubpanelVisible = true;
                addNewAstroData();
            }
            addAstroDataFrameWasShowing = true;
            Prefs.set("plot2.addAstroDataFrameWasShowing", addAstroDataFrameWasShowing);
        });
        line1morepanelspanel.add(addastrodatabutton);

        SpringUtil.makeCompactGrid(line1morepanelspanel, 1, line1morepanelspanel.getComponentCount(), 0, 0, 0, 0);
        morepanelspanel.add(line1morepanelspanel);

        JPanel line2morepanelspanel = new JPanel(new SpringLayout());

        moreybutton = new JButton("Y-data ");
        moreybutton.setToolTipText("opens the Y-data panel");
        moreybutton.setFont(p11);
        moreybutton.setPreferredSize(new Dimension(90, 20));
        moreybutton.addActionListener(e -> {
            if (subFrame.isShowing()) { subFrame.setVisible(true); } else {
                setSubpanelVisible = true;
                showMoreCurvesJPanel();
            }
        });
        line2morepanelspanel.add(moreybutton);

        refStarButton = new JButton("Ref. Stars");
        refStarButton.setFont(p11);
        refStarButton.setToolTipText("opens the reference star panel");
        refStarButton.setPreferredSize(new Dimension(90, 20));
        refStarButton.addActionListener(e -> {
            if (refStarFrame != null && refStarFrame.isShowing()) {
                refStarFrame.setVisible(true);
            } else {
                if (table != null) {
                    showRefStarJPanel();
                } else {
                    IJ.showMessage("No table is selected in Multi-Plot");
                }
            }
        });

        line2morepanelspanel.add(refStarButton);

        SpringUtil.makeCompactGrid(line2morepanelspanel, 1, line2morepanelspanel.getComponentCount(), 0, 0, 0, 0);
        morepanelspanel.add(line2morepanelspanel);

        SpringUtil.makeCompactGrid(morepanelspanel, morepanelspanel.getComponentCount(), 1, 2, 0, 2, 2);
        mainpanelgroupf.add(morepanelspanel);

        SpringUtil.makeCompactGrid(mainpanelgroupf, 1, mainpanelgroupf.getComponentCount(), 2, 0, 6, 0);
        mainpanel.add(mainpanelgroupf);

        SpringUtil.makeCompactGrid(mainpanel, 6, 1, 6, 6, 6, 6);

        mainFrame.setJMenuBar(mainmenubar);
        mainFrame.add(mainscrollpane);
        mainFrame.pack();
        mainFrame.setResizable(true);

        if (rememberWindowLocations) {
            IJU.setFrameSizeAndLocation(mainFrame, mainFrameLocationX, mainFrameLocationY, mainFrameWidth, mainFrameHeight);
        }
        if (!plotAutoMode) mainFrame.setVisible(true);
        if (!plotAutoMode && table != null && (openRefStarWindow || refStarPanelWasShowing)) {
//                    checkAndLockTable();
            showRefStarJPanel();
//                    if (table != null) table.setLock(false);
            refStarPanelWasShowing = true;
        }

        if (!plotAutoMode && (addAstroDataFrameWasShowing)) //table != null &&
        {
            addNewAstroData();
            addAstroDataFrameWasShowing = true;
        }
        showMoreCurvesJPanel();
        FileDrop fileDrop = new FileDrop(mainpanel, BorderFactory.createEmptyBorder(), MultiPlot_::openDragAndDropFiles);
    }


    static void showMoreCurvesJPanel() {
        if (subFrame == null) {
            subFrame = new JFrame("Multi-plot Y-data");
        }
        subFrame.setIconImage(plotIcon.getImage());
        var mainsubpanel = new JPanel(new SpringLayout());
        mainsubpanel.addMouseMotionListener(panelMouseMotionListener);

        subscrollpane = new JScrollPane(mainsubpanel);
        if (subscrollpane == null) {
            subscrollpane = new JScrollPane(mainsubpanel);
        }
        subFrame.setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        subFrame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                subFrameLocationX = subFrame.getLocation().x;
                subFrameLocationY = subFrame.getLocation().y;
                subFrameWidth = subFrame.getWidth();
                subFrameHeight = subFrame.getHeight();
                setSubpanelVisible = false;
                Prefs.set("plot2.subFrameLocationX", subFrameLocationX);
                Prefs.set("plot2.subFrameLocationY", subFrameLocationY);
            }
        });

        JPanel mainsubpanelgroupa = new JPanel(new SpringLayout());
        mainsubpanelgroupa.setBorder(BorderFactory.createTitledBorder(""));

        JPanel mainsubpanelgroupb = new JPanel(new SpringLayout());
        mainsubpanelgroupb.setBorder(BorderFactory.createTitledBorder(""));


        // SUBPANEL START

        if (useWideDataPanel) {
//                        constructTopGroupTopLabels(mainsubpanelgroupa);
//                        constructOtherGroupTopLabels(mainsubpanelgroupa);
            constructTopGroupBottomLabels(mainsubpanelgroupa);
            constructOtherGroupBottomLabels(mainsubpanelgroupa);

        } else {
//                        constructTopGroupTopLabels(mainsubpanelgroupa);
            constructTopGroupBottomLabels(mainsubpanelgroupa);
//                        constructOtherGroupTopLabels(mainsubpanelgroupb);
            constructOtherGroupBottomLabels(mainsubpanelgroupb);
        }
        for (cur = 0; cur < maxCurves; cur++) {
            constructTopGroup(mainsubpanelgroupa, cur);
            if (useWideDataPanel) { constructOtherGroup(mainsubpanelgroupa, cur); } else {
                constructOtherGroup(mainsubpanelgroupb, cur);
            }
        }

        if (useWideDataPanel) {
            SpringUtil.makeCompactGrid(mainsubpanelgroupa, maxCurves + 1, mainsubpanelgroupa.getComponentCount() / (maxCurves + 1), 5, 5, 5, 5);
            mainsubpanel.add(mainsubpanelgroupa);
            SpringUtil.makeCompactGrid(mainsubpanel, 1, 1, 5, 5, 5, 5);
        } else {
            SpringUtil.makeCompactGrid(mainsubpanelgroupa, maxCurves + 1, mainsubpanelgroupa.getComponentCount() / (maxCurves + 1), 5, 5, 5, 5);
            mainsubpanel.add(mainsubpanelgroupa);
            SpringUtil.makeCompactGrid(mainsubpanelgroupb, maxCurves + 1, mainsubpanelgroupb.getComponentCount() / (maxCurves + 1), 5, 5, 5, 5);
            mainsubpanel.add(mainsubpanelgroupb);
            SpringUtil.makeCompactGrid(mainsubpanel, mainsubpanel.getComponentCount(), 1, 5, 5, 5, 5);
        }


        if (refStarFrame != null && isRefStar != null && isRefStar.length > 0) {
            for (int r = 0; r < isRefStar.length; r++) {
                updateTable(isRefStar[r], r);
            }
            updatePlotEnabled = false;
            waitForPlotUpdateToFinish();
            updateGUI();
            if (delayedUpdateTimer != null) delayedUpdateTimer.cancel();
            if (delayedUpdateTask != null) delayedUpdateTask.cancel();
            updatePlotEnabled = true;
        }

        subFrame.add(subscrollpane);
        subFrame.pack();
        subFrame.setResizable(true);

        if (rememberWindowLocations) {
            IJU.setFrameSizeAndLocation(subFrame, subFrameLocationX, subFrameLocationY, subFrameWidth, subFrameHeight);
        }

        if (openDataSetWindow || setSubpanelVisible) {
            subFrame.setVisible(true);
        }
        //CREATE FIT PANEL

        createFitPanelCommonItems();
        for (int c = 0; c < maxCurves; c++) {
            createFitPanel(c);
            fitPanel[c].setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(color[c], 2), table == null ? "No Table Selected" : ylabel[c].trim().equals("") ? "No Data Column Selected" : ylabel[c], TitledBorder.CENTER, TitledBorder.TOP, b12, Color.darkGray));
        }
//                if (table != null)
//                    {
//                    table.setHeading(table.getColumnIndex("rel_flux_C2"), "rel_flux_T2");
//                    xdatacolumndefault.removeItem("rel_flux_C2");
//                    xdatacolumndefault.addItem("rel_flux_T2");
//                    }
        panelsUpdating = false;
        FileDrop fileDrop = new FileDrop(mainsubpanel, BorderFactory.createEmptyBorder(), MultiPlot_::openDragAndDropFiles);
    }

    public static boolean isRunning() {
        return mainFrame != null;
    }

    static void constructTopGroupBottomLabels(JPanel mainsubpanelgroup) {
        JLabel dummylabel20 = new JLabel("<HTML><CENTER>Data<BR><CENTER>Set</HTML>");
        dummylabel20.setFont(b11);
        dummylabel20.setForeground(Color.DARK_GRAY);
        dummylabel20.setHorizontalAlignment(JLabel.CENTER);
        dummylabel20.setMaximumSize(new Dimension(35, 25));
        mainsubpanelgroup.add(dummylabel20);

        JLabel savenewlabel = new JLabel("<HTML><CENTER>New<BR><CENTER>Col</HTML>");
        savenewlabel.setToolTipText("Save curve as new table column");
        savenewlabel.setFont(b11);
        savenewlabel.setForeground(Color.DARK_GRAY);
        savenewlabel.setMaximumSize(new Dimension(45, 25));
        savenewlabel.setHorizontalAlignment(JLabel.CENTER);
        mainsubpanelgroup.add(savenewlabel);

        JLabel dummylabel21 = new JLabel("Plot");
        dummylabel21.setFont(b11);
        dummylabel21.setForeground(Color.DARK_GRAY);
        dummylabel21.setToolTipText("Show the dataset on the plot");
        dummylabel21.setHorizontalAlignment(JLabel.CENTER);
        dummylabel21.setMaximumSize(new Dimension(45, 25));
        mainsubpanelgroup.add(dummylabel21);

        JLabel autoscaleincludelabel = new JLabel("<HTML><CENTER>Auto<BR><CENTER>Scale</HTML>");
        autoscaleincludelabel.setFont(b11);
        autoscaleincludelabel.setForeground(Color.DARK_GRAY);
        autoscaleincludelabel.setToolTipText("Include this dataset in X- and Y-axis autoscaling (master autoscaling must also be enabled in MP Main)");
        autoscaleincludelabel.setHorizontalAlignment(JLabel.CENTER);
        autoscaleincludelabel.setMaximumSize(new Dimension(45, 25));
        mainsubpanelgroup.add(autoscaleincludelabel);

        xcolumnlabelsub = new JLabel("X-data");
        xcolumnlabelsub.setFont(b11);
        xcolumnlabelsub.setForeground(Color.DARK_GRAY);
        xcolumnlabelsub.setHorizontalAlignment(JLabel.CENTER);
        mainsubpanelgroup.add(xcolumnlabelsub);
//                ycolumnlabelsub = new JLabel ("Y-data");
//                ycolumnlabelsub.setHorizontalAlignment(JLabel.CENTER);
//                mainsubpanelgroup.add (ycolumnlabelsub);

        JLabel magToFluxlabel = new JLabel("<HTML><CENTER>Input<BR><CENTER>in Mag</HTML>");
        magToFluxlabel.setFont(b11);
        magToFluxlabel.setForeground(Color.DARK_GRAY);
        magToFluxlabel.setToolTipText("<HTML>Enable to convert magnitude-based Y-input data (Y-data, Y-error, Y-operand, and Y-operand-error) to flux for additional calculations .<BR>" + "This option should generally be deselected to plot data generated by Multi-Aperture.</HTML>");
        magToFluxlabel.setHorizontalAlignment(JLabel.CENTER);
        magToFluxlabel.setMaximumSize(new Dimension(45, 25));
        mainsubpanelgroup.add(magToFluxlabel);


        JPanel ycolumnlabelgroup = new JPanel();
        ycolumnlabelgroup.setLayout(new BoxLayout(ycolumnlabelgroup, BoxLayout.X_AXIS));
//                legendslabelgroup.add(Box.createGlue());
        ycolumnlabelgroup.add(Box.createHorizontalGlue());

        ycolumnlabelsub = new JLabel("Y-data");
        ycolumnlabelsub.setFont(b11);
        ycolumnlabelsub.setForeground(Color.DARK_GRAY);
        ycolumnlabelsub.setHorizontalAlignment(JLabel.CENTER);
        ycolumnlabelgroup.add(ycolumnlabelsub);
        ycolumnlabelgroup.add(Box.createHorizontalStrut(5));
//                legendslabelgroup.add(Box.createGlue());

        helpIcon = createImageIcon("astroj/images/help.png", "Multi-plot Help");
        JButton ycolumnlabelconfigureButton = new JButton(helpIcon);
        ycolumnlabelconfigureButton.setFont(b11);
        ycolumnlabelconfigureButton.setForeground(Color.DARK_GRAY);
        ycolumnlabelconfigureButton.setToolTipText("Data naming convention help");
        ycolumnlabelconfigureButton.setMargin(new Insets(0, 0, 0, 0));
        ycolumnlabelconfigureButton.addActionListener(ae -> openDataHelpPanel());
        ycolumnlabelgroup.add(ycolumnlabelconfigureButton);
        ycolumnlabelgroup.add(Box.createHorizontalGlue());
//                legendslabelgroup.add(Box.createGlue());
        mainsubpanelgroup.add(ycolumnlabelgroup);

        JLabel useerrorlabel = new JLabel("<HTML><CENTER>Auto<BR><CENTER>Error</HTML>");
        useerrorlabel.setFont(b11);
        useerrorlabel.setForeground(Color.DARK_GRAY);
        useerrorlabel.setToolTipText("<HTML>Show automatic Y-error bars when dataset has predefined error available.<BR>" + "Compatible datasets are: Source-Sky_XX, rel_flux_XX, tot_C_cnts.</HTML>");
        useerrorlabel.setHorizontalAlignment(JLabel.CENTER);
        useerrorlabel.setMaximumSize(new Dimension(45, 25));
        mainsubpanelgroup.add(useerrorlabel);
//                JLabel errorlabel = new JLabel ("Y-error");
//                errorlabel.setHorizontalAlignment(JLabel.CENTER);
//                errorlabel.setToolTipText("Error associated with Y-data");
//                mainsubpanelgroup.add (errorlabel);
        operatorlabelsub = new JLabel("Function");
        operatorlabelsub.setFont(b11);
        operatorlabelsub.setForeground(Color.DARK_GRAY);
        operatorlabelsub.setHorizontalAlignment(JLabel.CENTER);
        operatorlabelsub.setToolTipText("Performs the selected operation on Y-data Column and Y-operator Column and propagates error if enabled");
        mainsubpanelgroup.add(operatorlabelsub);

        opcolumnlabelsub = new JLabel("Y-operand");
        opcolumnlabelsub.setFont(b11);
        opcolumnlabelsub.setForeground(Color.DARK_GRAY);
        opcolumnlabelsub.setHorizontalAlignment(JLabel.CENTER);
        mainsubpanelgroup.add(opcolumnlabelsub);

//                JLabel useoperrorlabel = new JLabel ("Use");
//                useoperrorlabel.setToolTipText("Use Operator Error as error associated with Y-operator Column");
//                useoperrorlabel.setHorizontalAlignment(JLabel.CENTER);
//                mainsubpanelgroup.add (useoperrorlabel);
//                JLabel operrorlabel = new JLabel ("Y-op Error");
//                operrorlabel.setHorizontalAlignment(JLabel.CENTER);
//                mainsubpanelgroup.add (operrorlabel);

        markerlabelsub = new JLabel("Color");
        markerlabelsub.setFont(b11);
        markerlabelsub.setForeground(Color.DARK_GRAY);
        markerlabelsub.setHorizontalAlignment(JLabel.CENTER);
        mainsubpanelgroup.add(markerlabelsub);

        markercolorlabelsub = new JLabel("Symbol");
        markercolorlabelsub.setFont(b11);
        markercolorlabelsub.setForeground(Color.DARK_GRAY);
        markercolorlabelsub.setHorizontalAlignment(JLabel.CENTER);
        mainsubpanelgroup.add(markercolorlabelsub);

        JLabel lineslabel = new JLabel("Lines");
        lineslabel.setFont(b11);
        lineslabel.setForeground(Color.DARK_GRAY);
        lineslabel.setToolTipText("Connect datapoints with lines");
        lineslabel.setHorizontalAlignment(JLabel.CENTER);
        lineslabel.setMaximumSize(new Dimension(45, 25));
        mainsubpanelgroup.add(lineslabel);

        JLabel inputAverageLabel = new JLabel("<HTML><CENTER>Input<BR><CENTER>Average</HTML>");
        inputAverageLabel.setFont(b11);
        inputAverageLabel.setForeground(Color.DARK_GRAY);
        inputAverageLabel.setMaximumSize(new Dimension(50, 25));
        inputAverageLabel.setPreferredSize(new Dimension(50, 25));
        inputAverageLabel.setHorizontalAlignment(JLabel.CENTER);
        inputAverageLabel.setToolTipText("Average data over n samples before processing further (detrending, plotting, etc)");
        mainsubpanelgroup.add(inputAverageLabel);
        forceIcon = createImageIcon("astroj/images/grab.png", "Transfer 'Page Rel' settings to absolute settings");
        insertColumnIcon = createImageIcon("astroj/images/insertcolumn.png", "Save curve as new table column");

        JLabel smoothlabel = new JLabel("<HTML><CENTER>Smo-<BR><CENTER>oth</HTML>");
        smoothlabel.setFont(b11);
        smoothlabel.setForeground(Color.DARK_GRAY);
        smoothlabel.setToolTipText("Smooth long time-series by removing long-term variations");
        smoothlabel.setHorizontalAlignment(JLabel.CENTER);
        smoothlabel.setMaximumSize(new Dimension(35, 25));
        mainsubpanelgroup.add(smoothlabel);

        JLabel smoothlenlabel = new JLabel("<HTML><CENTER>Len-<BR><CENTER>gth</HTML>");
        smoothlenlabel.setFont(b11);
        smoothlenlabel.setForeground(Color.DARK_GRAY);
        smoothlenlabel.setToolTipText("Set smoothing length (number data points)");
        smoothlenlabel.setHorizontalAlignment(JLabel.CENTER);
        smoothlenlabel.setMaximumSize(new Dimension(35, 25));
        mainsubpanelgroup.add(smoothlenlabel);
    }


    static void constructOtherGroupBottomLabels(JPanel mainsubpanelgroup) {
        JLabel seconddatasetlabel = new JLabel("<HTML><CENTER>Data<BR><CENTER>Set</HTML>");
        seconddatasetlabel.setFont(b11);
        seconddatasetlabel.setForeground(Color.DARK_GRAY);
        seconddatasetlabel.setHorizontalAlignment(JLabel.CENTER);
        seconddatasetlabel.setMaximumSize(new Dimension(35, 25));
        if (!useWideDataPanel) mainsubpanelgroup.add(seconddatasetlabel);

        JPanel detrendlabelgroup = new JPanel(new SpringLayout());
        detrendlabelgroup.setMaximumSize(new Dimension(195, 20));
        detrendlabelgroup.setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY));

        JLabel detrendlabelA = new JLabel("<HTML><CENTER>Fit<BR><CENTER>Mode</HTML>");
        detrendlabelA.setFont(b11);
        detrendlabelA.setForeground(Color.DARK_GRAY);
        detrendlabelA.setToolTipText("<html>Fit and/or detrend Y-data using a user-defined or fitted constant value times the 'Trend Data' values.<br>" + "If transit fit mode is selected, an exoplanet transit model will be fit between the left and right fit markers.<br>" + "If trend dataset(s) are also selected in transit fit mode, detrending will be fit simultaneously with the transit model.<br>" + "--------------------------------------------------------------------------------------------------<br>" + "In transit fit mode, right-click the 'Fit Mode' box to redisplay the 'Transit Fit Settings' panel.<br>" + "Set the Fit and Normalize region markers at the bottom of the Multi-plot Main panel.</html>");
        detrendlabelA.setHorizontalAlignment(JLabel.CENTER);
        detrendlabelA.setPreferredSize(new Dimension(50, 16));
        detrendlabelA.addMouseListener(new MouseListener() {
            public void mouseClicked(MouseEvent e) {
            }

            public void mousePressed(MouseEvent e) {
            }

            public void mouseReleased(MouseEvent e) {
                int firstFittedCurve = 0;
                for (int c = 0; c < maxCurves; c++) {
                    if (detrendtypecombobox[c].getSelectedIndex() == 9) {
                        firstFittedCurve = c;
                        break;
                    }
                }

                for (int c = 0; c < maxCurves; c++) {
                    if (detrendtypecombobox[c].getSelectedIndex() == 9) {
                        if (rememberWindowLocations) {
                            if (keepSeparateLocationsForFitWindows) {
                                if (!fitFrame[c].isVisible()) {
                                    IJU.setFrameSizeAndLocation(fitFrame[c], fitFrameLocationX[c], fitFrameLocationY[c], 0, 0);
                                }
                            } else if (c == firstFittedCurve) {
                                if (!fitFrame[c].isVisible()) {
                                    IJU.setFrameSizeAndLocation(fitFrame[c], fitFrameLocationX[c], fitFrameLocationY[c], 0, 0);
                                }
                            } else {
                                IJU.setFrameSizeAndLocation(fitFrame[c], (fitFrame[firstFittedCurve].isVisible() ? fitFrame[firstFittedCurve].getLocation().x : fitFrameLocationX[firstFittedCurve]) + (c - firstFittedCurve) * 25, (fitFrame[firstFittedCurve].isVisible() ? fitFrame[firstFittedCurve].getLocation().y : fitFrameLocationY[firstFittedCurve]) + (c - firstFittedCurve) * 25, 0, 0);
                            }
                        } else {
                            IJU.setFrameSizeAndLocation(fitFrame[c], 40 + c * 25, 40 + c * 25, 0, 0);
                        }


                        fitFrame[c].setVisible(true);
                    }
                }
            }

            public void mouseEntered(MouseEvent e) {}

            public void mouseExited(MouseEvent e) {}
        });

        detrendlabelgroup.add(detrendlabelA);

        if (maxDetrendVars > 1) {
            JLabel detrendlabelB = new JLabel("<HTML><CENTER>Trend<BR><CENTER>Select</HTML>");
            detrendlabelB.setFont(b11);
            detrendlabelB.setForeground(Color.DARK_GRAY);
            detrendlabelB.setToolTipText("<html>Click buttons to show the corresponding trend dataset coefficient and name.<br>" + "If a trend dataset has been selected and a fit mode is enabled,<br>" + "the button background will display green. The background will be gray otherwise.<br>" + "---------------------------------------------------------------------------<br>" + "Select a trend dataset more than once to increase the power of its polynomial fit.<br>" + "One occurance of a trend dataset -> linear fit, two occurances -> quadratic fit, etc.<br>" + "---------------------------------------------------------------------------<br>" + "If 'Show tooltips help' is enabled under the 'Preferences' menu,<br>" + "a mouse-over of a radio button shows the column set for that variable.<br>" + "If a radio button is gray and a tooltip shows a column name enclosed in square brackets,<br>" + "that column will be recalled if the radio button is clicked again.<br>" + "---------------------------------------------------------------------------<br>" + "Set the Fit and Normalize region markers at the bottom of the Multi-plot Main panel.<br>" + "---------------------------------------------------------------------------<br>" + "SHORTCUTS:<br>" + "Click an already selected button again to alternately enable/disable the corresponding trend variable<br>" + "&lt;SHIFT&gt;Click - selects this trend column to be displayed for all rows<br>" + "&lt;CTRL&gt;Click - copies the selected trend dataset to all rows<br>" + "&lt;ALT&gt;Click - alternately enables/disables the corresponding trend dataset</html>");
            detrendlabelB.setPreferredSize(new Dimension(maxDetrendVars * 20, 25));
            detrendlabelB.setHorizontalAlignment(JLabel.CENTER);
            detrendlabelgroup.add(detrendlabelB);
        }

        JLabel detrendlabelC = new JLabel("<HTML><CENTER>Trend<BR><CENTER>Coefficient</HTML>");
        detrendlabelC.setFont(b11);
        detrendlabelC.setForeground(Color.DARK_GRAY);
        detrendlabelC.setToolTipText("<html>White background indicates user-selected value, gray background indicates fitted constant value.<br>" + "Set the Fit and Normalize region markers at the bottom of the Multi-plot Main panel.</html>");
        detrendlabelC.setPreferredSize(new Dimension(75, 25));
        detrendlabelC.setHorizontalAlignment(JLabel.CENTER);
        detrendlabelgroup.add(detrendlabelC);

        JLabel detrendlabelD = new JLabel("<HTML><CENTER>Trend<BR><CENTER>Dataset</HTML>");
        detrendlabelD.setFont(b11);
        detrendlabelD.setForeground(Color.DARK_GRAY);
        detrendlabelD.setToolTipText("<html>Trend data used to detrend Y-data.<br>" + "---------------------------------------------------------------------------<br>" + "Select a 'Trend Dataset' more than once to increase the power of its polynomial fit.<br>" + "One occurance of a Trend Dataset -> linear fit, two occurances -> quadratic fit, etc.<br>" + "---------------------------------------------------------------------------<br>" + "Set the Fit and Normalize region markers at the bottom of the Multi-plot Main panel.</html>");
        detrendlabelD.setHorizontalAlignment(JLabel.CENTER);
        detrendlabelD.setPreferredSize(new Dimension(132, 25));
        detrendlabelgroup.add(detrendlabelD);

        SpringUtil.makeCompactGrid(detrendlabelgroup, 1, detrendlabelgroup.getComponentCount(), 0, 0, 0, 0);
        mainsubpanelgroup.add(detrendlabelgroup);


//                JLabel customscalelabel4 = new JLabel ("<HTML><CENTER><u>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Detrend Y-data&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;</u><BR>"+
//                                                       "&nbsp;&nbsp;&nbsp;&nbsp;Fit&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;" +
//                                                       "Coefficient&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;"+
//                                                       "&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Trend Data</HTML>");
//                mainsubpanelgroup.add(customscalelabel4);
        JLabel normlabel = new JLabel("<HTML><CENTER>Norm/<BR><CENTER>Mag Ref</HTML>");
        normlabel.setFont(b11);
        normlabel.setForeground(Color.DARK_GRAY);
        normlabel.setToolTipText("<html>Select the region of data used to normalize the dataset.<br>" + "If 'Out Mag' is selected, the mean of the region is used as the reference<br>" + "level when converting the plotted data to relative magnitude.<br>" + "Set the Fit and Normalize region markers at the bottom of the Multi-plot Main panel.</html>");
        normlabel.setHorizontalAlignment(JLabel.CENTER);
        normlabel.setMaximumSize(new Dimension(50, 25));
        mainsubpanelgroup.add(normlabel);

        JLabel mmaglabel = new JLabel("<HTML><CENTER>Out<BR><CENTER>Mag</HTML>");
        mmaglabel.setFont(b11);
        mmaglabel.setForeground(Color.DARK_GRAY);
        mmaglabel.setToolTipText("<html>Plot data in relative magnitude referenced to the average of the first n data samples,<br>" + "where n is set by the 'Rel. Mag. Reference' spinner on the 'Multi-plot Main' window.<br>" + "If a 'Norm/Mag Ref' region is selected, the mean of the region is used as the reference<br>" + "level when converting the plotted data to relative magnitude.</html>");
        mmaglabel.setHorizontalAlignment(JLabel.CENTER);
        mmaglabel.setMaximumSize(new Dimension(30, 25));
        mainsubpanelgroup.add(mmaglabel);

        JPanel customscalelabelgroup = new JPanel();
        customscalelabelgroup.setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY));
        customscalelabelgroup.setLayout(new BoxLayout(customscalelabelgroup, BoxLayout.X_AXIS));
        customscalelabelgroup.setMaximumSize(new Dimension(195, 20));
//                JLabel dummylabel39 = new JLabel ("");
//                customscalelabelgroup.add (dummylabel39);
        customscalelabelgroup.add(Box.createHorizontalStrut(18));
//                customscalelabelgroup.add(Box.createHorizontalGlue());

        JLabel customscalelabel1 = new JLabel("<HTML><CENTER>Page<BR><CENTER>Rel</HTML>");
        customscalelabel1.setFont(b11);
        customscalelabel1.setForeground(Color.DARK_GRAY);
        customscalelabel1.setToolTipText("Select to arbitrarily scale data to fit on plot (\"Scale\" and \"Shift\" by a percentage of the plot Y-range)");
//                customscalelabel1.setHorizontalAlignment(JLabel.LEFT);
        customscalelabelgroup.add(customscalelabel1);
        customscalelabelgroup.add(Box.createHorizontalGlue());

        JLabel customscalelabel2 = new JLabel("Scale");
        customscalelabel2.setFont(b11);
        customscalelabel2.setForeground(Color.DARK_GRAY);
        customscalelabel2.setToolTipText("Scale dataset by specified value or if \"Page Rel\" is selected, by specified percentage of the plot Y-range");
//                customscalelabel2.setHorizontalAlignment(JLabel.CENTER);
        customscalelabelgroup.add(customscalelabel2);
        customscalelabelgroup.add(Box.createHorizontalStrut(25));

        JLabel customscalelabel3 = new JLabel("then Shift");
        customscalelabel3.setFont(b11);
        customscalelabel3.setForeground(Color.DARK_GRAY);
        customscalelabel3.setToolTipText("Add specified constant value to scaled dataset or if \"Page Rel\" is selected, add specified percentage of the plot Y-range");
//                customscalelabel3.setHorizontalAlignment(JLabel.CENTER);
        customscalelabelgroup.add(customscalelabel3);
        customscalelabelgroup.add(Box.createHorizontalStrut(20));
        mainsubpanelgroup.add(customscalelabelgroup);

        //todo make use bin vars
        JPanel displayBinningGroup = new JPanel();
        displayBinningGroup.setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY));
        displayBinningGroup.setLayout(new BoxLayout(displayBinningGroup, BoxLayout.X_AXIS));

        if (useWideDataPanel) { displayBinningGroup.setPreferredSize(new Dimension(225, 20)); } else {
            displayBinningGroup.setPreferredSize(new Dimension(325, 20));
        }

        displayBinningGroup.add(Box.createHorizontalStrut(1));

        JLabel displayBinningLabel = new JLabel("<HTML><CENTER>Out<BR><CENTER>Bin</HTML>");
        displayBinningLabel.setFont(b11);
        displayBinningLabel.setForeground(Color.DARK_GRAY);
        displayBinningLabel.setHorizontalAlignment(JLabel.CENTER);
        displayBinningLabel.setComponentPopupMenu(legendpopup);
        displayBinningLabel.setToolTipText("Whether the display should bin datum together.");
        displayBinningLabel.setMaximumSize(new Dimension(75, 45));
        displayBinningLabel.setPreferredSize(new Dimension(75, 45));
        //displayBinningLabel.setMinimumSize(new Dimension(75, 25));
        displayBinningGroup.add(displayBinningLabel);
        displayBinningGroup.add(Box.createHorizontalStrut(5));

        //displayBinningGroup.add(Box.createGlue());

        JLabel displayBinSizeLabel = new JLabel("<HTML><CENTER>Bin Size<br><CENTER>(minutes)</HTML>");
        displayBinSizeLabel.setFont(b11);
        displayBinSizeLabel.setForeground(Color.DARK_GRAY);
        displayBinSizeLabel.setHorizontalAlignment(JLabel.CENTER);
        displayBinningGroup.add(displayBinSizeLabel);
        displayBinningGroup.add(Box.createHorizontalStrut(5));

        displayBinningGroup.setMaximumSize(new Dimension(100, 45));
        displayBinningGroup.setPreferredSize(new Dimension(100, 40));
        mainsubpanelgroup.add(displayBinningGroup);

        JPanel legendslabelgroup = new JPanel();
        legendslabelgroup.setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY));
        legendslabelgroup.setLayout(new BoxLayout(legendslabelgroup, BoxLayout.X_AXIS));

        if (useWideDataPanel) { legendslabelgroup.setPreferredSize(new Dimension(225, 20)); } else {
            legendslabelgroup.setPreferredSize(new Dimension(325, 20));
        }
//                legendslabelgroup.add(Box.createGlue());
        legendslabelgroup.add(Box.createHorizontalStrut(1));

        JLabel morelegendslabel = new JLabel("<HTML><CENTER>Legend<BR><CENTER>Type</HTML>");
        morelegendslabel.setFont(b11);
        morelegendslabel.setForeground(Color.DARK_GRAY);
        morelegendslabel.setHorizontalAlignment(JLabel.CENTER);
        morelegendslabel.setComponentPopupMenu(legendpopup);
        morelegendslabel.setToolTipText("Right click to set legend preferences");
        morelegendslabel.setMaximumSize(new Dimension(75, 45));
        morelegendslabel.setPreferredSize(new Dimension(75, 45));
        morelegendslabel.setMinimumSize(new Dimension(75, 25));
        legendslabelgroup.add(morelegendslabel);
        legendslabelgroup.add(Box.createHorizontalStrut(5));

        legendslabelgroup.add(Box.createGlue());

        JLabel morelegendsnamelabel = new JLabel("Custom Legend");
        morelegendsnamelabel.setFont(b11);
        morelegendsnamelabel.setForeground(Color.DARK_GRAY);
        morelegendsnamelabel.setHorizontalAlignment(JLabel.CENTER);
        if (useWideDataPanel) { morelegendsnamelabel.setPreferredSize(new Dimension(125, 20)); } else {
            morelegendsnamelabel.setPreferredSize(new Dimension(225, 20));
        }
        morelegendsnamelabel.setComponentPopupMenu(legendpopup);
        morelegendsnamelabel.setToolTipText("Right click to set legend preferences");
        legendslabelgroup.add(morelegendsnamelabel);
        legendslabelgroup.add(Box.createGlue());

        legendslabelconfigureButton = new JButton(configureIcon);
        legendslabelconfigureButton.setToolTipText("Set legend preferences");
        legendslabelconfigureButton.setMargin(new Insets(0, 0, 0, 0));
        legendslabelconfigureButton.setMaximumSize(new Dimension(25, 25));
        legendslabelconfigureButton.addActionListener(ae -> legendpopup.show(legendslabelconfigureButton, mainsubpanel.getX(), mainsubpanel.getY() + 25));
        legendslabelgroup.add(legendslabelconfigureButton);

        mainsubpanelgroup.add(legendslabelgroup);


        if (useWideDataPanel) mainsubpanelgroup.add(seconddatasetlabel);
    }

    static void constructTopGroup(JPanel mainsubpanelgroup, final int c) {
        color[c] = colorOf(colorIndex[c]);
        curvelabel[c] = new JLabel("" + (c + 1));
        curvelabel[c].setBorder(BorderFactory.createLineBorder(color[c], 2));
        curvelabel[c].setHorizontalAlignment(JLabel.CENTER);
        mainsubpanelgroup.add(curvelabel[c]);

//                savecolumnpanelgroup[c] = new JPanel (new SpringLayout());
//                savecolumnpanelgroup[c].setMaximumSize(new Dimension(25,20));
//                savecolumnpanelgroup[c].setBorder(BorderFactory.createEmptyBorder());

        savecolumnbutton[c] = new JButton(insertColumnIcon);
        savecolumnbutton[c].setEnabled(plotY[c]);
        savecolumnbutton[c].setMargin(new Insets(0, 0, 0, 0));
        savecolumnbutton[c].setToolTipText("Save curve " + (c + 1) + " as new table column");
        savecolumnbutton[c].addActionListener(e -> {
            addNewColumn(c, false);
            updatePlot(updateAllFits());
        });

//                savecolumnpanelgroup[c].add(savecolumnbutton[c]);
//                SpringUtil.makeCompactGrid (savecolumnpanelgroup[c], 1, savecolumnpanelgroup[c].getComponentCount(), 0,0,0,0);

        mainsubpanelgroup.add(savecolumnbutton[c]);

        usecurvebox[c] = new JCheckBox("", plotY[c]);
        usecurvebox[c].addItemListener(e -> {
            if (e.getStateChange() == ItemEvent.DESELECTED) {
                plotY[c] = false;
            } else if (e.getStateChange() == ItemEvent.SELECTED) plotY[c] = true;
            savecolumnbutton[c].setEnabled(plotY[c]);
            updatePlot(updateOneFit(c));
        });
        usecurvebox[c].setHorizontalAlignment(JLabel.CENTER);
        mainsubpanelgroup.add(usecurvebox[c]);

        autoscaleincludebox[c] = new JCheckBox("", ASInclude[c]);
        autoscaleincludebox[c].addItemListener(e -> {
            if (e.getStateChange() == ItemEvent.DESELECTED) {
                ASInclude[c] = false;
            } else if (e.getStateChange() == ItemEvent.SELECTED) ASInclude[c] = true;
            updatePlot(updateNoFits());
        });
        autoscaleincludebox[c].setHorizontalAlignment(JLabel.CENTER);
        mainsubpanelgroup.add(autoscaleincludebox[c]);

        xdatacolumn[c] = new JComboBox<>(columnswd);
        xdatacolumn[c].setFont(p11);
        xdatacolumn[c].setSelectedItem(xlabel[c]);
//                if (!IJ.isLinux())
        xdatacolumn[c].setPrototypeDisplayValue("123456789012345678");
//                else
//                    xdatacolumn[c].setPrototypeDisplayValue("1234567890123456");
        xdatacolumn[c].addActionListener(ae -> {
            xlabel[c] = (String) xdatacolumn[c].getSelectedItem();
            updatePlot(updateOneFit(c));
        });
        mainsubpanelgroup.add(xdatacolumn[c]);

        fromMagBox[c] = new JCheckBox("", fromMag[c]);
        fromMagBox[c].addItemListener(e -> {
            if (e.getStateChange() == ItemEvent.DESELECTED) {
                fromMag[c] = false;
            } else if (e.getStateChange() == ItemEvent.SELECTED) fromMag[c] = true;
            updatePlot(updateOneFit(c));
        });
        fromMagBox[c].setHorizontalAlignment(JLabel.CENTER);
        mainsubpanelgroup.add(fromMagBox[c]);

        ydatacolumn[c] = new JComboBox<>(columns);
        ydatacolumn[c].setFont(p11);
        ydatacolumn[c].setSelectedItem(ylabel[c]);
//                if (!IJ.isLinux())
        ydatacolumn[c].setPrototypeDisplayValue("123456789012345678");
//                else
//                    ydatacolumn[c].setPrototypeDisplayValue("1234567890123456");
        ydatacolumn[c].addActionListener(ae -> {
            ylabel[c] = (String) ydatacolumn[c].getSelectedItem();
            if (!disableUpdatePlotBox) {
                usecurvebox[c].setSelected(true);
                plotY[c] = true;
                savecolumnbutton[c].setEnabled(plotY[c]);
                if (fitPanel != null && fitPanel[c] != null) {
                    fitPanel[c].setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(color[c], 2), table == null ? "No Table Selected" : ylabel[c].trim().equals("") ? "No Data Column Selected" : ylabel[c], TitledBorder.CENTER, TitledBorder.TOP, b12, Color.darkGray));
                }
            }
            updatePlot(updateOneFit(c));
        });
        mainsubpanelgroup.add(ydatacolumn[c]);

        errorcolumnbox[c] = new JCheckBox("", showErrors[c]);
        errorcolumnbox[c].setToolTipText("<HTML>Show automatic Y-error bars when data has predefined error available.<BR>" + "Compatible data columns are: Source-Sky_XX, rel_flux_XX, tot_C_cnts, tot_C_cnts-XX.<BR>" + "To display custom user defined error, select the column containing the desired error<BR>" + "under 'Y-operand' and then select 'custom error' under 'Function'.</HTML>");
        errorcolumnbox[c].addItemListener(e -> {
            if (e.getStateChange() == ItemEvent.DESELECTED) {
                showErrors[c] = false;
            } else if (e.getStateChange() == ItemEvent.SELECTED) {
                if (operatorIndex[c] == 6) {
                    operatorIndex[c] = 0;
                    operatorselection[c].setSelectedIndex(operatorIndex[c]);
                }
                showErrors[c] = true;
            }
            updatePlot(updateNoFits());
        });
        errorcolumnbox[c].setHorizontalAlignment(JLabel.CENTER);
        mainsubpanelgroup.add(errorcolumnbox[c]);

        operatorselection[c] = new JComboBox<>(operators);
        operatorselection[c].setFont(p11);
        operatorselection[c].setSelectedIndex(operatorIndex[c]);
        operatorselection[c].setPrototypeDisplayValue("123456789012");
        operatorselection[c].addActionListener(ae -> {
            operatorIndex[c] = operatorselection[c].getSelectedIndex();
            if (operatorIndex[c] == 6) {
                showErrors[c] = false;
                errorcolumnbox[c].setSelected(false);
            }
            updatePlot(updateOneFit(c));
        });
        mainsubpanelgroup.add(operatorselection[c]);

        operatorcolumn[c] = new JComboBox<>(columns);
        operatorcolumn[c].setFont(p11);
        operatorcolumn[c].setSelectedItem(oplabel[c]);
//                if (!IJ.isLinux())
        operatorcolumn[c].setPrototypeDisplayValue("123456789012345678");
//                else
//                    operatorcolumn[c].setPrototypeDisplayValue("1234567890123456");
        operatorcolumn[c].addActionListener(ae -> {
            operatorIndex[c] = operatorselection[c].getSelectedIndex();
            oplabel[c] = (String) operatorcolumn[c].getSelectedItem();
            updatePlot(updateOneFit(c));
        });
        mainsubpanelgroup.add(operatorcolumn[c]);


        markercolorselection[c] = new JComboBox<>(colors);
        markercolorselection[c].setFont(p11);
//                        markercolorselection[c].setPreferredSize(new Dimension(100, 25));
        markercolorselection[c].setSelectedIndex(colorIndex[c]);
        markercolorselection[c].setFont(new Font("Dialog", Font.BOLD, 12));
        markercolorselection[c].setForeground(color[c]);
        markercolorselection[c].setMaximumRowCount(16);
        markercolorselection[c].setPrototypeDisplayValue("12345678");
        markercolorselection[c].addActionListener(ae -> {
            colorIndex[c] = markercolorselection[c].getSelectedIndex();
            color[c] = colorOf(colorIndex[c]);
            curvelabel[c].setBorder(BorderFactory.createLineBorder(color[c], 2));
            othercurvelabel[c].setBorder(BorderFactory.createLineBorder(color[c], 2));
            markercolorselection[c].setForeground(color[c]);
            markersymbolselection[c].setForeground(color[c]);
            if (fitPanel != null && fitPanel[c] != null) {
                fitPanel[c].setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(color[c], 2), table == null ? "No Table Selected" : ylabel[c].trim().equals("") ? "No Data Column Selected" : ylabel[c], TitledBorder.CENTER, TitledBorder.TOP, b12, Color.darkGray));
            }
            updatePlot(updateNoFits());
        });
        mainsubpanelgroup.add(markercolorselection[c]);


        markersymbolselection[c] = new JComboBox<>(markers);
        markersymbolselection[c].setFont(p11);
//                        markersymbolselection[c].setPreferredSize(new Dimension(100, 25));
        markersymbolselection[c].setForeground(color[c]);
        markersymbolselection[c].setFont(new Font("Dialog", Font.BOLD, 12));
        markersymbolselection[c].setSelectedIndex(markerIndex[c]);
        markersymbolselection[c].setPrototypeDisplayValue("12345");
        markersymbolselection[c].addActionListener(ae -> {
            markerIndex[c] = markersymbolselection[c].getSelectedIndex();
            updatePlot(updateNoFits());
        });
        mainsubpanelgroup.add(markersymbolselection[c]);

        uselinesbox[c] = new JCheckBox("", lines[c]);
        uselinesbox[c].addItemListener(e -> {
            if (e.getStateChange() == ItemEvent.DESELECTED) {
                lines[c] = false;
            } else if (e.getStateChange() == ItemEvent.SELECTED) lines[c] = true;
            updatePlot(updateNoFits());
        });
        uselinesbox[c].setHorizontalAlignment(JLabel.CENTER);
        mainsubpanelgroup.add(uselinesbox[c]);

        inputAverageOverSizespinnermodel[c] = new SpinnerNumberModel(inputAverageOverSize[c], 1, null, 1);
        inputAverageOverSizespinner[c] = new JSpinner(inputAverageOverSizespinnermodel[c]);
        inputAverageOverSizespinner[c].setFont(p11);
        inputAverageOverSizespinner[c].setMaximumSize(new Dimension(50, 25));
        inputAverageOverSizespinner[c].setPreferredSize(new Dimension(50, 25));
        inputAverageOverSizespinner[c].addChangeListener(ev -> {
            inputAverageOverSize[c] = (Integer) inputAverageOverSizespinner[c].getValue();
            Prefs.set("plot.inputAverageOverSize" + c, inputAverageOverSize[c]);
            updatePlot(updateOneFit(c));
        });
        inputAverageOverSizespinner[c].addMouseWheelListener(e -> {
            int newValue = (Integer) inputAverageOverSizespinner[c].getValue() - e.getWheelRotation();
            if (newValue > 0) inputAverageOverSizespinner[c].setValue(newValue);
        });
        mainsubpanelgroup.add(inputAverageOverSizespinner[c]);

        usesmoothbox[c] = new JCheckBox("", smooth[c]);
        usesmoothbox[c].addItemListener(e -> {
            if (e.getStateChange() == ItemEvent.DESELECTED) {
                smooth[c] = false;
            } else if (e.getStateChange() == ItemEvent.SELECTED) smooth[c] = true;
            updatePlot(updateOneFit(c));
        });
        usesmoothbox[c].setHorizontalAlignment(JLabel.CENTER);
        mainsubpanelgroup.add(usesmoothbox[c]);

        smoothlenspinnermodel[c] = new SpinnerNumberModel(smoothLen[c], 1, null, 1);
        smoothlenspinner[c] = new JSpinner(smoothlenspinnermodel[c]);
        smoothlenspinner[c].setFont(p11);
        smoothlenspinner[c].setMaximumSize(new Dimension(50, 25));
        smoothlenspinner[c].setPreferredSize(new Dimension(50, 25));
        smoothlenspinner[c].addChangeListener(ev -> {
            smoothLen[c] = (Integer) smoothlenspinner[c].getValue();
            Prefs.set("plot.smoothLen" + c, smoothLen[c]);
            updatePlot(updateOneFit(c));
        });
        smoothlenspinner[c].addMouseWheelListener(e -> {
            int newValue = (Integer) smoothlenspinner[c].getValue() - e.getWheelRotation();
            if (newValue > 0) smoothlenspinner[c].setValue(newValue);
        });
        mainsubpanelgroup.add(smoothlenspinner[c]);
    }

    static void constructOtherGroup(JPanel mainsubpanelgroup, final int c) {
        if (!useWideDataPanel) {
            othercurvelabel[c] = new JLabel("" + (c + 1));
            othercurvelabel[c].setFont(p11);
            othercurvelabel[c].setBorder(BorderFactory.createLineBorder(color[c], 2));
            othercurvelabel[c].setHorizontalAlignment(JLabel.CENTER);
            mainsubpanelgroup.add(othercurvelabel[c]);
        }


        //DETREND PANEL GROUP

        detrendpanelgroup[c] = new JPanel(new SpringLayout());
        detrendpanelgroup[c].setMaximumSize(new Dimension(350, 20));
        detrendpanelgroup[c].setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY));
//                customscalepanelgroup[c].setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(new Color(122,138,153)),BorderFactory.createLineBorder(new Color(184,207,229))));


        //DETREND MODE


        detrendtypecombobox[c] = new JComboBox<>(detrendiconlist);
        detrendtypecombobox[c].setFont(p11);
        detrendtypecombobox[c].setSelectedIndex(detrendFitIndex[c]);
        detrendtypecombobox[c].setMaximumRowCount(10);
        detrendtypecombobox[c].setToolTipText("<html>Select 'user' to manually select detrend coefficient or select region type to auto-fit<br>" + "for the best detrend coefficient(s) for the selected trend data column(s).<br>" + "If transit fit mode is selected, an exoplanet transit model will be fit between the left and right fit markers.<br>" + "If trend dataset(s) are also selected in transit fit mode, detrending will be fit simultaneously with the transit model.<br>" + "-------------------------------------------------------------------------------------------------------<br>" + "In transit fit mode, right click the 'Fit Mode' box to redisplay the 'Transit Fit Settings' panel.<br>" + "Set the Fit and Normalize region markers at the bottom of the Multi-plot Main panel.</html>");
        detrendtypecombobox[c].setPreferredSize(new Dimension(50, 16));
        detrendtypecombobox[c].addActionListener(ae -> {
            detrendFitIndex[c] = detrendtypecombobox[c].getSelectedIndex();
            if (detrendFitIndex[c] == 9) {
                int firstFittedCurve = 0;
                for (int cc = 0; cc < maxCurves; cc++) {
                    if (detrendtypecombobox[cc].getSelectedIndex() == 9) {
                        firstFittedCurve = cc;
                        break;
                    }
                }
                if (rememberWindowLocations) {
                    if (keepSeparateLocationsForFitWindows) {
                        IJU.setFrameSizeAndLocation(fitFrame[c], fitFrameLocationX[c], fitFrameLocationY[c], 0, 0);
                    } else if (c == firstFittedCurve) {
                        IJU.setFrameSizeAndLocation(fitFrame[c], fitFrameLocationX[c], fitFrameLocationY[c], 0, 0);
                    } else {
                        IJU.setFrameSizeAndLocation(fitFrame[c], (fitFrame[firstFittedCurve] != null && fitFrame[firstFittedCurve].isVisible() ? fitFrame[firstFittedCurve].getLocation().x : fitFrameLocationX[firstFittedCurve]) + (c - firstFittedCurve) * 25, (fitFrame[firstFittedCurve] != null && fitFrame[firstFittedCurve].isVisible() ? fitFrame[firstFittedCurve].getLocation().y : fitFrameLocationY[firstFittedCurve]) + (c - firstFittedCurve) * 25, 0, 0);
                    }
                } else {
                    IJU.setFrameSizeAndLocation(fitFrame[c], 40 + c * 25, 40 + c * 25, 0, 0);
                }
                fitFrame[c].setVisible(true);
            } else {
                fitFrame[c].setVisible(false);
            }
            detrendfactorspinner[c].setEnabled(detrendFitIndex[c] == 1);
            Prefs.set("plot.detrendFitIndex" + c, detrendFitIndex[c]);
            for (int v = 0; v < maxDetrendVars; v++) {
                if (detrendFitIndex[c] != 0 && detrendIndex[c][v] != 0) {
                    detrendVarButton[c][v].setBackground(new Color(0, 193, 0));
                } else { detrendVarButton[c][v].setBackground(defaultBackground); }
            }
            updatePlot(updateOneFit(c));
        });
        detrendtypecombobox[c].addMouseListener(new MouseListener() {
            public void mouseClicked(MouseEvent e) {
            }

            public void mousePressed(MouseEvent e) {
//                    if (detrendtypecombobox[c].getSelectedIndex() == 9)
//                            {
//                            fitFrame[c].setVisible(true);
//                            }
            }

            public void mouseReleased(MouseEvent e) {
                if (detrendtypecombobox[c].getSelectedIndex() == 9 && ((e.getModifiers() & MouseEvent.BUTTON3_MASK) != 0 || e.isShiftDown() || e.isControlDown() || e.isAltDown() || e.isMetaDown())) {
                    if (detrendtypecombobox[c].getSelectedIndex() == 9) {
                        fitFrame[c].setVisible(true);
                    }
                }

            }

            public void mouseEntered(MouseEvent e) {}

            public void mouseExited(MouseEvent e) {}
        });
        detrendpanelgroup[c].add(detrendtypecombobox[c]);


        //DETREND VARIABLE SELECTORS

        detrendVarRadioGroup[c] = new ButtonGroup();
        for (int v = 0; v < maxDetrendVars; v++) {
            constructDetrendVarRadioGroup(c, v);
        }
        if (c == 1) defaultBackground = detrendVarButton[c][0].getBackground();


        //DETREND SPINNER

        detrendfactormodel[c] = new SpinnerNumberModel(detrendFactor[c][detrendVarDisplayed[c]], null, null, detrendFactorStep[c][detrendVarDisplayed[c]]);
        detrendfactorspinner[c] = new JSpinner(detrendfactormodel[c]);
        detrendfactorspinner[c].setFont(p11);
        detrendfactorspinner[c].setToolTipText("<HTML>Y-dataset values are detrended by applying the product(s)<BR>" + "of the coefficient(s) and the corresponding 'Trend Dataset(s)'.<BR>" + "Right click to set spinner stepsize.</HTML>");
        detrendfactorspinner[c].setPreferredSize(new Dimension(75, 25));
        detrendfactorspinner[c].setEnabled(detrendFitIndex[c] == 1);
        detrendfactorspinner[c].setEditor(new JSpinner.NumberEditor(detrendfactorspinner[c], "0.#########"));
        detrendfactorspinner[c].addChangeListener(ev -> {
            if (detrendFitIndex[c] == 1) {
                detrendFactor[c][detrendVarDisplayed[c]] = (Double) detrendfactorspinner[c].getValue();
                Prefs.set("plot.detrendFactor[" + c + "][" + detrendVarDisplayed[c] + "]", detrendFactor[c][detrendVarDisplayed[c]]);
                updatePlot(updateOneFit(c));
            }
        });
        detrendfactorspinner[c].addMouseWheelListener(e -> {
            if (detrendFitIndex[c] == 1) {
                detrendfactorspinner[c].setValue((Double) detrendfactorspinner[c].getValue() - e.getWheelRotation() * detrendFactorStep[c][detrendVarDisplayed[c]]);
            }
        });
        detrendpanelgroup[c].add(detrendfactorspinner[c]);

        //DETREND FACTOR SPINNER STEPSIZE POPUP

        detrendfactorsteppopup[c] = new JPopupMenu();
        detrendfactorsteppanel[c] = new JPanel();
        detrendfactorstepmodel[c] = new SpinnerListModel(spinnerscalelist);
        detrendfactorstepspinner[c] = new JSpinner(detrendfactorstepmodel[c]);
        detrendfactorstepspinner[c].setValue(convertToText(detrendFactorStep[c][detrendVarDisplayed[c]]));
        detrendfactorstepspinner[c].addChangeListener(ev -> {
            double value = IJU.getTextSpinnerDoubleValue(detrendfactorstepspinner[c]);
            if (Double.isNaN(value)) return;
            detrendFactorStep[c][detrendVarDisplayed[c]] = value;
            Prefs.set("plot.detrendFactorStep[" + c + "][" + detrendVarDisplayed[c] + "]", detrendFactorStep[c][detrendVarDisplayed[c]]);
            detrendfactorspinner[c].setModel(new SpinnerNumberModel(detrendFactor[c][detrendVarDisplayed[c]], null, null, detrendFactorStep[c][detrendVarDisplayed[c]]));
            detrendfactorspinner[c].setEditor(new JSpinner.NumberEditor(detrendfactorspinner[c], "########0.#########"));
        });
        detrendfactorsteplabel[c] = new JLabel("Stepsize:");
        detrendfactorsteppanel[c].add(detrendfactorsteplabel[c]);
        detrendfactorsteppanel[c].add(detrendfactorstepspinner[c]);
        detrendfactorsteppopup[c].add(detrendfactorsteppanel[c]);
        detrendfactorsteppopup[c].addPopupMenuListener(new PopupMenuListener() {
            public void popupMenuCanceled(PopupMenuEvent popupMenuEvent) { }

            public void popupMenuWillBecomeInvisible(PopupMenuEvent popupMenuEvent) { }

            public void popupMenuWillBecomeVisible(PopupMenuEvent popupMenuEvent) { }
        });
        detrendfactorspinner[c].setComponentPopupMenu(detrendfactorsteppopup[c]);

        //DETREND COLUMN

        detrendbox[c] = new JComboBox<>(columnsDetrend);
        detrendbox[c].setFont(p11);
        detrendbox[c].setMaximumRowCount(16);
        for (int v = 0; v < maxDetrendVars; v++) {
            if (!detrendlabel[c][v].equals("Meridian_Flip") && (detrendlabel[c][v].trim().equals("") || (table != null && table.getColumnIndex(detrendlabel[c][v]) == MeasurementTable.COLUMN_NOT_FOUND))) {
                detrendbox[c].setSelectedIndex(0);
                detrendIndex[c][v] = 0;
                detrendlabel[c][v] = "";
            } else {
                detrendbox[c].setSelectedItem(detrendlabel[c][v]); //if in table heading names, should be in detrendbox column names
                detrendIndex[c][v] = detrendbox[c].getSelectedIndex();
            }
            if (detrendFitIndex[c] != 0 && detrendIndex[c][v] != 0) {
                detrendVarButton[c][v].setBackground(new Color(0, 193, 0));
            } else { detrendVarButton[c][v].setBackground(defaultBackground); }
        }
        if (!detrendlabel[c][detrendVarDisplayed[c]].equals("Meridian_Flip") && (detrendlabel[c][detrendVarDisplayed[c]].trim().equals("") || (table != null && table.getColumnIndex(detrendlabel[c][detrendVarDisplayed[c]]) == MeasurementTable.COLUMN_NOT_FOUND))) {
            detrendbox[c].setSelectedIndex(0);
            detrendIndex[c][detrendVarDisplayed[c]] = 0;
            detrendlabel[c][detrendVarDisplayed[c]] = "";
        } else {
            detrendbox[c].setSelectedItem(detrendlabel[c][detrendVarDisplayed[c]]);
            detrendIndex[c][detrendVarDisplayed[c]] = detrendbox[c].getSelectedIndex();
        }

        detrendbox[c].setPrototypeDisplayValue("123456789012345678");

        detrendbox[c].addActionListener(ae -> {
            int oldIndex = detrendIndex[c][detrendVarDisplayed[c]];
            Color oldColor = detrendVarButton[c][detrendVarDisplayed[c]].getBackground();
            boolean newIndex = false;
            boolean newState = false;
            detrendlabel[c][detrendVarDisplayed[c]] = (String) detrendbox[c].getSelectedItem();
            detrendIndex[c][detrendVarDisplayed[c]] = detrendbox[c].getSelectedIndex();
            newIndex = (oldIndex != detrendIndex[c][detrendVarDisplayed[c]]);
            detrendVarButton[c][detrendVarDisplayed[c]].setToolTipText(detrendlabel[c][detrendVarDisplayed[c]].trim().equals("") ? (detrendlabelhold[c][detrendVarDisplayed[c]].trim().equals("") ? "unused" : "[" + detrendlabelhold[c][detrendVarDisplayed[c]] + "]") : detrendlabel[c][detrendVarDisplayed[c]]);


            if (detrendFitIndex[c] != 0 && detrendIndex[c][detrendVarDisplayed[c]] != 0) {
                detrendVarButton[c][detrendVarDisplayed[c]].setBackground(new Color(0, 193, 0));
            } else if (!detrendVarButton[c][detrendVarDisplayed[c]].getBackground().equals(defaultBackground)) {
                detrendVarButton[c][detrendVarDisplayed[c]].setBackground(defaultBackground);
            }
            newState = !oldColor.equals(detrendVarButton[c][detrendVarDisplayed[c]].getBackground());

            if (fitDetrendComboBox[c][detrendVarDisplayed[c]] != null && fitDetrendComboBox[c][detrendVarDisplayed[c]].isEnabled()) {
                if (detrendIndex[c][detrendVarDisplayed[c]] != 0 || (!detrendVarButton[c][detrendVarDisplayed[c]].getBackground().equals(defaultBackground))) {
                    fitDetrendComboBox[c][detrendVarDisplayed[c]].setSelectedIndex(detrendIndex[c][detrendVarDisplayed[c]]);
                }
            }

            if (useFitDetrendCB[c][detrendVarDisplayed[c]] != null && useFitDetrendCB[c][detrendVarDisplayed[c]].isEnabled()) {
                useFitDetrendCB[c][detrendVarDisplayed[c]].setSelected(!detrendVarButton[c][detrendVarDisplayed[c]].getBackground().equals(defaultBackground));
            }

            if (newIndex || newState) updatePlot(updateOneFit(c));

        });
        detrendpanelgroup[c].add(detrendbox[c]);

        SpringUtil.makeCompactGrid(detrendpanelgroup[c], 1, detrendpanelgroup[c].getComponentCount(), 0, 0, 0, 0);
        mainsubpanelgroup.add(detrendpanelgroup[c]);


        normpanelgroup[c] = new JPanel(new SpringLayout());
        normpanelgroup[c].setMaximumSize(new Dimension(50, 25));
        normpanelgroup[c].setBorder(BorderFactory.createEmptyBorder());

        normtypecombobox[c] = new JComboBox<>(normiconlist);
        normtypecombobox[c].setFont(p11);
        normtypecombobox[c].setToolTipText("<html>Select a 'Norm/Mag Ref' region mode that shows green color over the region(s) of data to include in the calculation.<br>" + "If 'Out Mag' is selected, the mean of the region is used as the reference<br>" + "level when converting the plotted data to relative magnitude.<br>" + "Set the Fit and Normalize region markers at the bottom of the Multi-plot Main panel.</html>");
        normtypecombobox[c].setSelectedIndex(normIndex[c]);
        normtypecombobox[c].setPreferredSize(new Dimension(50, 16));
        normtypecombobox[c].addActionListener(ae -> {
            normIndex[c] = normtypecombobox[c].getSelectedIndex();
            updatePlot(updateNoFits());
        });
        normpanelgroup[c].add(normtypecombobox[c]);
        SpringUtil.makeCompactGrid(normpanelgroup[c], 1, normpanelgroup[c].getComponentCount(), 0, 0, 0, 0);
        mainsubpanelgroup.add(normpanelgroup[c]);

        usemmagbox[c] = new JCheckBox("", mmag[c]);
        usemmagbox[c].setToolTipText("<html>Plot data in relative magnitude referenced to the average of the first n data samples,<br>" + "where n is set by the 'Rel. Mag. Reference' spinner on the 'Multi-plot Main' window.<br>" + "If a 'Norm/Mag Ref' region is selected, the mean of the region is used as the reference<br>" + "level when converting the plotted data to relative magnitude.</html>");
        usemmagbox[c].addItemListener(e -> {
            if (e.getStateChange() == ItemEvent.DESELECTED) {
                mmag[c] = false;
            } else if (e.getStateChange() == ItemEvent.SELECTED) mmag[c] = true;
            updatePlot(updateNoFits());
        });
        usemmagbox[c].setHorizontalAlignment(JLabel.CENTER);
        mainsubpanelgroup.add(usemmagbox[c]);

        Insets buttonMargin = new Insets(0, 0, 0, 0);

        customscalepanelgroup[c] = new JPanel(new SpringLayout());
        customscalepanelgroup[c].setMaximumSize(new Dimension(195, 20));
        customscalepanelgroup[c].setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY));
//                customscalepanelgroup[c].setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(new Color(122,138,153)),BorderFactory.createLineBorder(new Color(184,207,229))));

        // GRAB "FORCE" SCALE AND SHIFT VALUES
//                grabautopanel[c] = new JPanel (new SpringLayout());

        grabautobutton[c] = new JButton(forceIcon);
        grabautobutton[c].setMargin(buttonMargin);
        grabautobutton[c].setToolTipText("Transfer \"Page Rel\" scale and shift values to absolute values");
        grabautobutton[c].addActionListener(e -> {
            customScaleFactor[c] = autoScaleFactor[c] * yRange / yWidth[c];
//                        customscalespinner[c].setValue(customScaleFactor[c]);
            customShiftFactor[c] = yMid + yRange * autoShiftFactor[c] - customScaleFactor[c] * yMidpoint[c];
            if (Double.isNaN(customShiftFactor[c]) || Double.isInfinite(customShiftFactor[c])) {
                customShiftFactor[c] = 0.0;
            }
            if (Double.isNaN(customScaleFactor[c]) || Double.isInfinite(customScaleFactor[c])) {
                customScaleFactor[c] = 1.0;
            }
//                        customshiftspinner[c].setValue(customShiftFactor[c]);
            force[c] = false;
            forcebox[c].setSelected(false);
            customscalespinner[c].setModel(new SpinnerNumberModel(customScaleFactor[c], null, null, customScaleStep[c]));
            customscalespinner[c].setEditor(new JSpinner.NumberEditor(customscalespinner[c], "########0.#########"));
            customscalestepspinner[c].setValue(convertToText(customScaleStep[c]));
            customshiftspinner[c].setModel(new SpinnerNumberModel(customShiftFactor[c], null, null, customShiftStep[c]));
            customshiftspinner[c].setEditor(new JSpinner.NumberEditor(customshiftspinner[c], "########0.#########"));
            ((JSpinner.DefaultEditor) customshiftspinner[c].getEditor()).getTextField().addMouseListener(shiftSpinnerMouseListener);
            customshiftstepspinner[c].setValue(convertToText(customShiftStep[c]));
            Prefs.set("plot.force" + c, force[c]);
            updatePlot(updateNoFits());
        });
        customscalepanelgroup[c].add(grabautobutton[c]);
//                SpringUtil.makeCompactGrid (grabautopanel[c],1,1, 0,0,0,0);
//                mainsubpanelgroup.add (grabautopanel[c]);
//                customscalepanelgroup[c].add (grabautopanel[c]);

        //FORCE CHECKBOX

        forcebox[c] = new JCheckBox("", force[c]);
        forcebox[c].setToolTipText("Select to arbitrarily scale data to fit on plot (\"Scale\" and \"Shift\" by a percentage of the plot Y-range)");
        forcebox[c].addItemListener(e -> {
            if (e.getStateChange() == ItemEvent.DESELECTED) {
                force[c] = false;
            } else if (e.getStateChange() == ItemEvent.SELECTED) force[c] = true;
            if (Double.isNaN(autoScaleFactor[c]) || Double.isInfinite(autoScaleFactor[c])) autoScaleFactor[c] = 1.0;
            if (Double.isNaN(customScaleFactor[c]) || Double.isInfinite(customScaleFactor[c])) {
                customScaleFactor[c] = 1.0;
            }
            if (Double.isNaN(autoShiftFactor[c]) || Double.isInfinite(autoShiftFactor[c])) autoShiftFactor[c] = 1.0;
            if (Double.isNaN(customShiftFactor[c]) || Double.isInfinite(customShiftFactor[c])) {
                customShiftFactor[c] = 1.0;
            }
            customscalespinner[c].setModel(new SpinnerNumberModel(force[c] ? autoScaleFactor[c] * 100.0 : customScaleFactor[c], null, null, force[c] ? autoScaleStep[c] : customScaleStep[c]));
            customscalespinner[c].setEditor(new JSpinner.NumberEditor(customscalespinner[c], "########0.#########"));
            customscalestepspinner[c].setValue(convertToText(force[c] ? autoScaleStep[c] : customScaleStep[c]));
            customshiftspinner[c].setModel(new SpinnerNumberModel(force[c] ? autoShiftFactor[c] * 100 : customShiftFactor[c], null, null, force[c] ? autoShiftStep[c] : customShiftStep[c]));
            customshiftspinner[c].setEditor(new JSpinner.NumberEditor(customshiftspinner[c], "########0.#########"));
            ((JSpinner.DefaultEditor) customshiftspinner[c].getEditor()).getTextField().addMouseListener(shiftSpinnerMouseListener);
            residualShiftSpinner[c].setModel(new SpinnerNumberModel(force[c] ? autoResidualShift[c] * 100 : residualShift[c], null, null, force[c] ? autoShiftStep[c] : customShiftStep[c]));
            residualShiftSpinner[c].setEditor(new JSpinner.NumberEditor(residualShiftSpinner[c], fitFormat));
            customshiftstepspinner[c].setValue(convertToText(force[c] ? autoShiftStep[c] : customShiftStep[c]));
            Prefs.set("plot.force" + c, force[c]);
            updatePlot(updateNoFits());
        });
//                forcebox[c].setHorizontalAlignment(JLabel.CENTER);
        customscalepanelgroup[c].add(forcebox[c]);

        //SCALE SPINNER

        customscalemodel[c] = new SpinnerNumberModel(force[c] ? autoScaleFactor[c] * 100 : customScaleFactor[c], null, null, force[c] ? autoScaleStep[c] : customScaleStep[c]);
        customscalespinner[c] = new JSpinner(customscalemodel[c]);
        customscalespinner[c].setFont(p11);
        customscalespinner[c].setEditor(new JSpinner.NumberEditor(customscalespinner[c], "########0.#########"));
        customscalespinner[c].setToolTipText("<HTML>If 'Page Rel' is deselected, the Y-dataset values are multiplied by this factor<BR>" + "(and by the factor of 10 specified in the 'Y-axis' box of the 'Multi-plot Main' panel).<BR>" + "If 'Page Rel' is selected, the dataset values are arbitrarily scaled to fit on a 'Scale' percentage of the Y-axis range.<BR>" + "Right click to set spinner stepsize.</HTML>");
        customscalespinner[c].setPreferredSize(new Dimension(75, 25));

//                customscaleeditor[c] = (JSpinner.NumberEditor)customscalespinner[c].getEditor();
//                customscaleformat[c] = customscaleeditor[c].getFormat();
//                customscaleformat[c].setMaximumFractionDigits(9);

        customscalespinner[c].addChangeListener(ev -> {
            if (force[c]) { autoScaleFactor[c] = (Double) customscalespinner[c].getValue() / 100.0; } else {
                customScaleFactor[c] = (Double) customscalespinner[c].getValue();
            }
            if (Double.isNaN(autoScaleFactor[c]) || Double.isInfinite(autoScaleFactor[c])) autoScaleFactor[c] = 1.0;
            if (Double.isNaN(customScaleFactor[c]) || Double.isInfinite(customScaleFactor[c])) {
                customScaleFactor[c] = 1.0;
            }
            updatePlot(updateNoFits());
        });
        customscalespinner[c].addMouseWheelListener(e -> {
            if (force[c]) {
                customscalespinner[c].setValue((Double) customscalespinner[c].getValue() - e.getWheelRotation() * autoScaleStep[c]);
            } else {
                customscalespinner[c].setValue((Double) customscalespinner[c].getValue() - e.getWheelRotation() * customScaleStep[c]);
            }
            if (Double.isNaN((Double) customscalespinner[c].getValue()) || Double.isInfinite((Double) customscalespinner[c].getValue())) {
                customscalespinner[c].setValue(1.0);
                customScaleFactor[c] = 1.0;
            }
        });
        customscalepanelgroup[c].add(customscalespinner[c]);

        //SCALE SPINNER STEPSIZE POPUP

        customscalesteppopup[c] = new JPopupMenu();
        customscalesteppanel[c] = new JPanel();
        customscalestepmodel[c] = new SpinnerListModel(spinnerscalelist);
        customscalestepspinner[c] = new JSpinner(customscalestepmodel[c]);
        customscalestepspinner[c].setValue(convertToText(force[c] ? autoScaleStep[c] : customScaleStep[c]));
        customscalestepspinner[c].addChangeListener(ev -> {
            if (force[c]) {
                double value = IJU.getTextSpinnerDoubleValue(customscalestepspinner[c]);
                if (Double.isNaN(value)) return;
                autoScaleStep[c] = value;
                Prefs.set("plot.autoScaleStep" + c, autoScaleStep[c]);
            } else {
                double value = IJU.getTextSpinnerDoubleValue(customscalestepspinner[c]);
                if (Double.isNaN(value)) return;
                customScaleStep[c] = value;
                Prefs.set("plot.customScaleStep" + c, customScaleStep[c]);
            }
            customscalespinner[c].setModel(new SpinnerNumberModel(force[c] ? autoScaleFactor[c] * 100 : customScaleFactor[c], null, null, force[c] ? autoScaleStep[c] : customScaleStep[c]));
            customscalespinner[c].setEditor(new JSpinner.NumberEditor(customscalespinner[c], "########0.#########"));
//                        customscaleeditor[c] = (JSpinner.NumberEditor)customscalespinner[c].getEditor();
//                        customscaleformat[c] = customscaleeditor[c].getFormat();
//                        customscaleformat[c].setMaximumFractionDigits(9);
        });
        customscalesteplabel[c] = new JLabel("Stepsize:");
        customscalesteppanel[c].add(customscalesteplabel[c]);
        customscalesteppanel[c].add(customscalestepspinner[c]);
        customscalesteppopup[c].add(customscalesteppanel[c]);
        customscalesteppopup[c].addPopupMenuListener(new PopupMenuListener() {
            public void popupMenuCanceled(PopupMenuEvent popupMenuEvent) { }

            public void popupMenuWillBecomeInvisible(PopupMenuEvent popupMenuEvent) { }

            public void popupMenuWillBecomeVisible(PopupMenuEvent popupMenuEvent) { }
        });
        customscalespinner[c].setComponentPopupMenu(customscalesteppopup[c]);

        //SHIFT SPINNER

        customshiftmodel[c] = new SpinnerNumberModel(force[c] ? autoShiftFactor[c] * 100 : customShiftFactor[c], null, null, force[c] ? invertYAxisSign * autoShiftStep[c] : invertYAxisSign * customShiftStep[c]);
        customshiftspinner[c] = new JSpinner(customshiftmodel[c]);
        if (c == 0) defaultSpinnerBorder = customshiftspinner[0].getBorder();
        customshiftspinner[c].setFont(p11);
        customshiftspinner[c].setEditor(new JSpinner.NumberEditor(customshiftspinner[c], "########0.#########"));
//                customshifteditor[c] = (JSpinner.NumberEditor)customshiftspinner[c].getEditor();
//                customshiftformat[c] = customshifteditor[c].getFormat();
//                customshiftformat[c].setMaximumFractionDigits(6);


        customshiftspinner[c].setPreferredSize(new Dimension(75, 25));
        customshiftspinner[c].setToolTipText("<HTML>If 'Page Rel' is deselected, the 'Shift' value is added to the scaled Y-dataset values before plotting.<BR>" + "If 'Page Rel' is selected, the dataset values are arbitrarily shifted by 'Shift' percentage of the Y-axis plot range.<BR>" + "Use 'CONTROL' and 'SHIFT' click modifiers to select and shift multiple curves simultaneously.<BR>" + "Right click to set spinner stepsize.</HTML>");
        customshiftspinner[c].addChangeListener(ev -> {
            if (customshiftspinner[c].isEnabled()) {
                if (force[c]) //work around java bug that sometimes causes two click detections rather than one
                {
                    double a = Double.parseDouble(uptoNinePlaces.format(autoShiftFactor[c]));
                    double b = Double.parseDouble(uptoNinePlaces.format((Double) customshiftspinner[c].getValue() / 100.0));
                    if (a == b) return;
                } else {
                    double a = Double.parseDouble(uptoNinePlaces.format(customShiftFactor[c]));
                    double b = Double.parseDouble(uptoNinePlaces.format(((Double) customshiftspinner[c].getValue()).doubleValue()));
                    if (a == b) return;
                }
                if (!customshiftspinner[c].getBorder().equals(greenBorder)) {
                    for (int curve = 0; curve < maxCurves; curve++) {
                        customshiftspinner[curve].setBorder(defaultSpinnerBorder);
                    }
                }
                double oldAutoShiftFactor = autoShiftFactor[c];
                double oldCustomShiftFactor = customShiftFactor[c];
                if (force[c]) { autoShiftFactor[c] = (Double) customshiftspinner[c].getValue() / 100.0; } else {
                    customShiftFactor[c] = (Double) customshiftspinner[c].getValue();
                }

                for (int curve = 0; curve < maxCurves; curve++) {
                    if (curve != c && customshiftspinner[curve].getBorder().equals(greenBorder) && force[curve] == force[c]) {
                        customshiftspinner[curve].setEnabled(false);
                        if (force[c]) {
                            autoShiftFactor[curve] = (((Double) customshiftspinner[curve].getValue() + (autoShiftFactor[c] > oldAutoShiftFactor ? 1.0 : -1.0) * autoShiftStep[c]) / 100.0);
                            if (Double.isNaN(autoShiftFactor[curve]) || Double.isInfinite(autoShiftFactor[curve])) {
                                autoShiftFactor[curve] = 0.0;
                            }
                            customshiftspinner[curve].setValue(((Double) customshiftspinner[curve].getValue() + (autoShiftFactor[c] > oldAutoShiftFactor ? 1.0 : -1.0) * autoShiftStep[c]));
                        } else {
                            customShiftFactor[curve] = (Double) customshiftspinner[curve].getValue() + (customShiftFactor[c] > oldCustomShiftFactor ? 1.0 : -1.0) * customShiftStep[c];
                            if (Double.isNaN(customShiftFactor[curve]) || Double.isInfinite(customShiftFactor[curve])) {
                                customShiftFactor[curve] = 0.0;
                            }
                            customshiftspinner[curve].setValue((Double) customshiftspinner[curve].getValue() + (customShiftFactor[c] > oldCustomShiftFactor ? 1.0 : -1.0) * customShiftStep[c]);
                        }
                        if (Double.isNaN((Double) customshiftspinner[curve].getValue()) || Double.isInfinite((Double) customshiftspinner[curve].getValue())) {
                            customshiftspinner[curve].setValue(0.0);
                        }
                    }
                }
                updatePlot(updateNoFits());
            } else {
                customshiftspinner[c].setEnabled(true);
            }
        });
        customshiftspinner[c].addMouseWheelListener(e -> {
            if (force[c]) {
                customshiftspinner[c].setValue((Double) customshiftspinner[c].getValue() - invertYAxisSign * e.getWheelRotation() * autoShiftStep[c]);
            } else {
                customshiftspinner[c].setValue((Double) customshiftspinner[c].getValue() - invertYAxisSign * e.getWheelRotation() * customShiftStep[c]);
            }
            if (Double.isNaN((Double) customshiftspinner[c].getValue()) || Double.isInfinite((Double) customshiftspinner[c].getValue())) {
                customshiftspinner[c].setValue(0.0);
            }
        });
        ((JSpinner.DefaultEditor) customshiftspinner[c].getEditor()).getTextField().addMouseListener(shiftSpinnerMouseListener);
        customscalepanelgroup[c].add(customshiftspinner[c]);

        //SHIFT SPINNER STEPSIZE POPUP

        customshiftsteppopup[c] = new JPopupMenu();
        customshiftsteppanel[c] = new JPanel(new SpringLayout());
        customshiftstepmodel[c] = new SpinnerListModel(spinnerscalelist);
        customshiftstepspinner[c] = new JSpinner(customshiftstepmodel[c]);
        customshiftstepspinner[c].setValue(convertToText(force[c] ? autoShiftStep[c] : customShiftStep[c]));

        customshiftstepspinner[c].addChangeListener(ev -> {
            int start = c;
            int end = c + 1;
            if (modifyCurvesAbove) {
                start = 0;
            }
            if (modifyCurvesBelow) {
                end = maxCurves;
            }
            for (int curve = start; curve < end; curve++) {
                if (force[c]) {
                    double value = IJU.getTextSpinnerDoubleValue(customshiftstepspinner[c]);
                    if (Double.isNaN(value)) return;
                    autoShiftStep[curve] = value;
                    Prefs.set("plot.autoShiftStep" + curve, autoShiftStep[curve]);
                } else {
                    double value = IJU.getTextSpinnerDoubleValue(customshiftstepspinner[c]);
                    if (Double.isNaN(value)) return;
                    customShiftStep[curve] = value;
                    Prefs.set("plot.customShiftStep" + curve, customShiftStep[curve]);
                }
                customshiftspinner[curve].setModel(new SpinnerNumberModel(force[curve] ? autoShiftFactor[curve] * 100 : customShiftFactor[curve], null, null, force[curve] ? invertYAxisSign * autoShiftStep[curve] : invertYAxisSign * customShiftStep[curve]));
                customshiftspinner[curve].setEditor(new JSpinner.NumberEditor(customshiftspinner[curve], "########0.#########"));
                ((JSpinner.DefaultEditor) customshiftspinner[curve].getEditor()).getTextField().addMouseListener(shiftSpinnerMouseListener);
                customshiftstepspinner[curve].setValue(convertToText(force[curve] ? autoShiftStep[curve] : customShiftStep[curve]));
            }
        });
        JPanel line1 = new JPanel(new SpringLayout());
        customshiftsteplabel[c] = new JLabel("Stepsize: ");
        line1.add(customshiftsteplabel[c]);
        line1.add(customshiftstepspinner[c]);
        SpringUtil.makeCompactGrid(line1, 1, line1.getComponentCount(), 0, 0, 0, 0);
        customshiftsteppanel[c].add(line1);

        JPanel line2 = new JPanel(new SpringLayout());
        JLabel line2Label = new JLabel("Also Modify Curves: ");
        shiftAboveBox[c] = new JCheckBox("Above", modifyCurvesAbove);
        shiftAboveBox[c].addItemListener(e -> {
            if (e.getStateChange() == ItemEvent.DESELECTED) {
                modifyCurvesAbove = false;
            } else if (e.getStateChange() == ItemEvent.SELECTED) {
                modifyCurvesAbove = true;
            }
            for (int curve = 0; curve < maxCurves; curve++) {
                shiftAboveBox[curve].setSelected(modifyCurvesAbove);
            }
        });

        shiftBelowBox[c] = new JCheckBox("Below", modifyCurvesBelow);
        shiftBelowBox[c].addItemListener(e -> {
            if (e.getStateChange() == ItemEvent.DESELECTED) {
                modifyCurvesBelow = false;
            } else if (e.getStateChange() == ItemEvent.SELECTED) {
                modifyCurvesBelow = true;
            }
            for (int curve = 0; curve < maxCurves; curve++) {
                shiftBelowBox[curve].setSelected(modifyCurvesBelow);
            }
        });
        line2.add(line2Label);
        line2.add(shiftAboveBox[c]);
        line2.add(shiftBelowBox[c]);
        SpringUtil.makeCompactGrid(line2, 1, line2.getComponentCount(), 0, 0, 0, 0);
        customshiftsteppanel[c].add(line2);

        SpringUtil.makeCompactGrid(customshiftsteppanel[c], customshiftsteppanel[c].getComponentCount(), 1, 0, 0, 0, 0);
        customshiftsteppopup[c].add(customshiftsteppanel[c]);


        customshiftsteppopup[c].addPopupMenuListener(new PopupMenuListener() {
            public void popupMenuCanceled(PopupMenuEvent popupMenuEvent) { }

            public void popupMenuWillBecomeInvisible(PopupMenuEvent popupMenuEvent) { }

            public void popupMenuWillBecomeVisible(PopupMenuEvent popupMenuEvent) { }
        });
        customshiftspinner[c].setComponentPopupMenu(customshiftsteppopup[c]);
//                SpringUtil.makeCompactGrid (autoscalepanelgroup[c], 1, 3, 0,0,0,0);
        SpringUtil.makeCompactGrid(customscalepanelgroup[c], 1, customscalepanelgroup[c].getComponentCount(), 0, 0, 0, 0);
//                mainsubpanelgroup.add (autoscalepanelgroup[c]);
        mainsubpanelgroup.add(customscalepanelgroup[c]);

//
        displayBinningPanel[c] = new JPanel();
        displayBinningPanel[c].setLayout(new BoxLayout(displayBinningPanel[c], BoxLayout.X_AXIS));
        displayBinningPanel[c].add(Box.createHorizontalStrut(10));
        var binCB = new JCheckBox();
        binCB.setSelected(binDisplay[c]);
        binCB.addActionListener($ -> {
            binDisplay[c] = binCB.isSelected();
            updatePlot(c);
        });
        displayBinningPanel[c].add(binCB);
        displayBinningPanel[c].add(Box.createHorizontalStrut(10));
        var binSpin = new JSpinner(new SpinnerNumberModel(5, 0, Double.MAX_VALUE, 1d));
        if (minutes.size() == c) {
            minutes.add(new Pair.GenericPair<>((Double) binSpin.getValue(), binSpin));
        } else {
            binSpin.setValue(minutes.get(c).first());
            minutes.set(c, new Pair.GenericPair<>((Double) binSpin.getValue(), binSpin));
        }
        binSpin.addChangeListener($ -> {
            minutes.set(c, new Pair.GenericPair<>((Double) binSpin.getValue(), binSpin));
            updatePlot(c);
        });
        GenericSwingDialog.getTextFieldFromSpinner(binSpin).ifPresent(s -> s.setColumns(5));
        binSpin.addMouseWheelListener(e -> {
            if (binSpin.getModel() instanceof SpinnerNumberModel spin) {
                var delta = e.getPreciseWheelRotation() * spin.getStepSize().doubleValue();
                var newValue = -delta + (Double) binSpin.getValue();

                if (newValue < (Double) spin.getMinimum()) {
                    newValue = (Double) spin.getMinimum();
                } else if (newValue > (Double) spin.getMaximum()) {
                    newValue = (Double) spin.getMaximum();
                }

                binSpin.setValue(newValue);
            }
        });
        displayBinningPanel[c].add(binSpin);

        mainsubpanelgroup.add(displayBinningPanel[c]);

        //LEGEND RADIO GROUP AND TEXT FIELD

        morelegendradiopanelgroup[c] = new JPanel();
        morelegendradiopanelgroup[c].setLayout(new BoxLayout(morelegendradiopanelgroup[c], BoxLayout.X_AXIS));

        if (useWideDataPanel) { morelegendradiopanelgroup[c].setPreferredSize(new Dimension(225, 20)); } else {
            morelegendradiopanelgroup[c].setPreferredSize(new Dimension(325, 20));
        }
//                legendslabelgroup.add(Box.createGlue());
        morelegendradiopanelgroup[c].add(Box.createHorizontalStrut(1));
        morelegendradiopanelgroup[c].setBorder(BorderFactory.createLineBorder(Color.lightGray));
        ImageIcon noneLegendIcon = createImageIcon("astroj/images/nonelegend.png", "Disable legend for this data set");
        ImageIcon columnLegendIcon = createImageIcon("astroj/images/columnlegend.png", "Use the column name as the legend for this data set");
        ImageIcon customLegendIcon = createImageIcon("astroj/images/customlegend.png", "Use a custom legend for this data set");
        legendnoneButton[c] = new JRadioButton(noneLegendIcon);
        legendnoneButton[c].setMaximumSize(new Dimension(25, 20));
        if (!useLegend[c] && !useColumnName[c]) {
            legendnoneButton[c].setSelected(true);
            legendnoneButton[c].setBackground(Color.LIGHT_GRAY);
        } else {
            legendnoneButton[c].setSelected(false);
            legendnoneButton[c].setBackground(Color.WHITE);
        }
        morelegendradiopanelgroup[c].add(legendnoneButton[c]);
        morelegendradiopanelgroup[c].add(Box.createHorizontalStrut(1));
        legendcolumnNameButton[c] = new JRadioButton(columnLegendIcon);
        legendcolumnNameButton[c].setMaximumSize(new Dimension(25, 20));
        if (useColumnName[c]) {
            legendcolumnNameButton[c].setSelected(true);
            legendcolumnNameButton[c].setBackground(Color.LIGHT_GRAY);
        } else {
            legendcolumnNameButton[c].setSelected(false);
            legendcolumnNameButton[c].setBackground(Color.WHITE);
        }
        morelegendradiopanelgroup[c].add(legendcolumnNameButton[c]);
        morelegendradiopanelgroup[c].add(Box.createHorizontalStrut(1));
        legendcustomNameButton[c] = new JRadioButton(customLegendIcon);
        legendcustomNameButton[c].setMaximumSize(new Dimension(25, 20));
        if (useLegend[c]) {
            legendcustomNameButton[c].setSelected(true);
            legendcustomNameButton[c].setBackground(Color.LIGHT_GRAY);
        } else {
            legendcustomNameButton[c].setSelected(false);
            legendcustomNameButton[c].setBackground(Color.WHITE);
        }
        morelegendradiopanelgroup[c].add(legendcustomNameButton[c]);
        morelegendradiopanelgroup[c].add(Box.createHorizontalStrut(3));

        morelegendRadioGroup[c] = new ButtonGroup();
        morelegendRadioGroup[c].add(legendnoneButton[c]);
        morelegendRadioGroup[c].add(legendcolumnNameButton[c]);
        morelegendRadioGroup[c].add(legendcustomNameButton[c]);
        legendnoneButton[c].addActionListener(ae -> {
            useColumnName[c] = false;
            useLegend[c] = false;
            legendnoneButton[c].setBackground(Color.LIGHT_GRAY);
            legendcolumnNameButton[c].setBackground(Color.WHITE);
            legendcustomNameButton[c].setBackground(Color.WHITE);
            updatePlot(updateNoFits());
        });
        legendcustomNameButton[c].addActionListener(ae -> {
            useLegend[c] = legendcustomNameButton[c].getBackground() == Color.WHITE;
            legendnoneButton[c].setBackground(useLegend[c] || useColumnName[c] ? Color.WHITE : Color.LIGHT_GRAY);
            legendcustomNameButton[c].setBackground(useLegend[c] ? Color.LIGHT_GRAY : Color.WHITE);
            updatePlot(updateNoFits());
        });
        legendcolumnNameButton[c].addActionListener(ae -> {
            useColumnName[c] = legendcolumnNameButton[c].getBackground() == Color.WHITE;
            legendnoneButton[c].setBackground(useLegend[c] || useColumnName[c] ? Color.WHITE : Color.LIGHT_GRAY);
            legendcolumnNameButton[c].setBackground(useColumnName[c] ? Color.LIGHT_GRAY : Color.WHITE);
            updatePlot(updateNoFits());
        });

        legendnoneButton[c].setComponentPopupMenu(legendpopup);
        legendnoneButton[c].setToolTipText("Disable Legend -- Right click to set legend preferences");
        legendcustomNameButton[c].setComponentPopupMenu(legendpopup);
        legendcustomNameButton[c].setToolTipText("Use or append Custom Legend -- Right click to set legend preferences");
        legendcolumnNameButton[c].setComponentPopupMenu(legendpopup);
        legendcolumnNameButton[c].setToolTipText("Use Column Name as Legend -- Right click to set legend preferences");


        // LEGEND TEXT FIELD
        morelegendField[c] = new JTextField(legend[c]);
        morelegendField[c].setFont(p11);
        morelegendField[c].setBorder(BorderFactory.createEmptyBorder(0, 4, 0, 4));
        morelegendField[c].getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent ev) {
                legend[c] = morelegendField[c].getText();
                updatePlot(updateNoFits());
            }

            public void removeUpdate(DocumentEvent ev) {
                legend[c] = morelegendField[c].getText();
                updatePlot(updateNoFits());
            }

            public void changedUpdate(DocumentEvent ev) {
                legend[c] = morelegendField[c].getText();
                updatePlot(updateNoFits());
            }
        });
        if (useWideDataPanel) { morelegendField[c].setPreferredSize(new Dimension(150, 20)); } else {
            morelegendField[c].setPreferredSize(new Dimension(250, 20));
        }
        morelegendField[c].setHorizontalAlignment(JTextField.LEFT);
        morelegendField[c].setComponentPopupMenu(legendpopup);
        morelegendField[c].setToolTipText("Right click to set legend preferences");
        morelegendradiopanelgroup[c].add(morelegendField[c]);

//                SpringUtil.makeCompactGrid (morelegendradiopanelgroup[c], 1, morelegendradiopanelgroup[c].getComponentCount(), 0,0,0,0);
        mainsubpanelgroup.add(morelegendradiopanelgroup[c]);

        if (useWideDataPanel) {
            othercurvelabel[c] = new JLabel("" + (c + 1));
            othercurvelabel[c].setFont(p11);
            othercurvelabel[c].setBorder(BorderFactory.createLineBorder(color[c], 2));
            othercurvelabel[c].setHorizontalAlignment(JLabel.CENTER);
            mainsubpanelgroup.add(othercurvelabel[c]);
        }
    }

    static void constructDetrendVarRadioGroup(final int c, final int v) {
        detrendVarButton[c][v] = new JRadioButton("");
        detrendVarButton[c][v].setMaximumSize(new Dimension(25, 20));
        detrendVarRadioGroup[c].add(detrendVarButton[c][v]);
        detrendVarButton[c][v].setSelected(detrendVarDisplayed[c] == v);
        detrendVarButton[c][v].setToolTipText(detrendlabel[c][v].trim().equals("") ? (detrendlabelhold[c][v].trim().equals("") ? "unused" : "[" + detrendlabelhold[c][v] + "]") : detrendlabel[c][v]);
        detrendVarButton[c][v].addActionListener(ae -> {
            int mods = ae.getModifiers();
            if ((mods & ActionEvent.CTRL_MASK) != 0 && (mods & ActionEvent.SHIFT_MASK) == 0) //copy this column name to all rows
            {
                for (int cc = 0; cc < maxCurves; cc++) {
                    detrendVarDisplayed[cc] = v;
                    detrendVarButton[cc][v].setSelected(true);
                    detrendlabel[cc][detrendVarDisplayed[cc]] = detrendlabel[c][detrendVarDisplayed[c]];
                    detrendFactorStep[cc][detrendVarDisplayed[cc]] = detrendFactorStep[c][detrendVarDisplayed[c]];
                    detrendfactorstepspinner[cc].setValue(convertToText(detrendFactorStep[cc][detrendVarDisplayed[cc]]));
                    if (!detrendlabel[cc][detrendVarDisplayed[cc]].equals("Meridian_Flip") && (detrendlabel[cc][detrendVarDisplayed[cc]].trim().equals("") || (table != null && table.getColumnIndex(detrendlabel[cc][detrendVarDisplayed[cc]]) == MeasurementTable.COLUMN_NOT_FOUND))) {
                        detrendbox[cc].setSelectedIndex(0);
//                                fitDetrendComboBox[cc][v].setSelectedIndex(0);
//                                useFitDetrendCB[cc][v].setSelected(false);
                        detrendIndex[cc][detrendVarDisplayed[cc]] = 0;
                        detrendlabel[cc][detrendVarDisplayed[cc]] = "";
                    } else {
                        detrendbox[cc].setSelectedItem(detrendlabel[cc][detrendVarDisplayed[cc]]);
//                                fitDetrendComboBox[cc][v].setSelectedItem(detrendlabel[cc][v]);
//                                useFitDetrendCB[cc][v].setSelected(true);
                        detrendIndex[cc][detrendVarDisplayed[cc]] = detrendbox[cc].getSelectedIndex();
                    }
                    detrendfactorspinner[cc].setValue(detrendFactor[cc][detrendVarDisplayed[cc]]);
                }
            } else if ((mods & ActionEvent.CTRL_MASK) == 0 && (mods & ActionEvent.SHIFT_MASK) != 0) //select this variable for display on all rows
            {
                for (int cc = 0; cc < maxCurves; cc++) {
                    detrendVarDisplayed[cc] = v;
                    detrendVarButton[cc][v].setSelected(true);
                    detrendfactorstepspinner[cc].setValue(convertToText(detrendFactorStep[cc][detrendVarDisplayed[cc]]));
                    if (!detrendlabel[cc][detrendVarDisplayed[cc]].equals("Meridian_Flip") && (detrendlabel[cc][detrendVarDisplayed[cc]].trim().equals("") || (table != null && table.getColumnIndex(detrendlabel[cc][detrendVarDisplayed[cc]]) == MeasurementTable.COLUMN_NOT_FOUND))) {
                        detrendbox[cc].setSelectedIndex(0);
                        detrendIndex[cc][detrendVarDisplayed[cc]] = 0;
                        detrendlabel[cc][detrendVarDisplayed[cc]] = "";
                    } else {
                        detrendbox[cc].setSelectedItem(detrendlabel[cc][detrendVarDisplayed[cc]]);
                        detrendIndex[cc][detrendVarDisplayed[cc]] = detrendbox[cc].getSelectedIndex();
                    }
                    detrendfactorspinner[cc].setValue(detrendFactor[cc][detrendVarDisplayed[cc]]);
                }
            } else {
                if (detrendVarDisplayed[c] == v || (mods & ActionEvent.ALT_MASK) != 0) {
                    if (!detrendVarButton[c][v].getBackground().equals(defaultBackground)) {
                        detrendlabelhold[c][v] = detrendlabel[c][v];
                        detrendVarDisplayed[c] = v;
                        detrendfactorstepspinner[c].setValue(convertToText(detrendFactorStep[c][detrendVarDisplayed[c]]));
                        detrendbox[c].setSelectedIndex(0);
                    } else {
                        detrendlabel[c][v] = detrendlabelhold[c][v];
                        detrendVarDisplayed[c] = v;
                        detrendfactorstepspinner[c].setValue(convertToText(detrendFactorStep[c][detrendVarDisplayed[c]]));
                        if (!detrendlabel[c][v].equals("Meridian_Flip") && (detrendlabel[c][v].trim().equals("") || (table != null && table.getColumnIndex(detrendlabel[c][v]) == MeasurementTable.COLUMN_NOT_FOUND))) {
                            detrendbox[c].setSelectedIndex(0);
                            detrendIndex[c][v] = 0;
                            detrendlabel[c][v] = "";
                        } else {

                            detrendbox[c].setSelectedItem(detrendlabel[c][v]);
                            detrendIndex[c][v] = detrendbox[c].getSelectedIndex();
                        }
                    }
                } else {
                    detrendVarDisplayed[c] = v;
                    detrendfactorstepspinner[c].setValue(convertToText(detrendFactorStep[c][detrendVarDisplayed[c]]));
                    if (!detrendlabel[c][v].equals("Meridian_Flip") && (detrendlabel[c][v].trim().equals("") || (table != null && table.getColumnIndex(detrendlabel[c][v]) == MeasurementTable.COLUMN_NOT_FOUND))) {
                        detrendbox[c].setSelectedIndex(0);
                        detrendIndex[c][v] = 0;
                        detrendlabel[c][v] = "";
                    } else {
                        detrendbox[c].setSelectedItem(detrendlabel[c][v]);
//                                fitDetrendComboBox[c][v].setSelectedItem(detrendlabel[c][v]);
//                                useFitDetrendCB[c][v].setSelected(true);
                        detrendIndex[c][v] = detrendbox[c].getSelectedIndex();
                    }
                }
                detrendfactorspinner[c].setValue(detrendFactor[c][detrendVarDisplayed[c]]);
            }
            enableTransitComponents(c);
        });
        if (maxDetrendVars > 1) {
            detrendpanelgroup[c].add(detrendVarButton[c][v]);
            detrendpanelgroup[c].add(Box.createHorizontalStrut(1));
        }

    }

    static void createFitPanelCommonItems() {
        spinnerSize = new Dimension(125, 25);
        orbitalSpinnerSize = new Dimension(70, 25);
        stellarSpinnerSize = new Dimension(60, 25);
        bpSize = new Dimension(55, 25);
        labelSize = new Dimension(135, 25);
        legendLabelSize = new Dimension(120, 25);
        controlSpinnerSize = new Dimension(105, 25);
        lineWidthSpinnerSize = new Dimension(75, 25);
        bestFitSize = new Dimension(110, 25);
        statSize = new Dimension(70, 25);
        colorSelectorSize = new Dimension(100, 43);
        spacerSize = new Dimension(10, 25);
        checkBoxSize = new Dimension(28, 25);
        choiceBoxSize = new Dimension(125, 25);
        labelSize = new Dimension(checkBoxSize.width + choiceBoxSize.width, 25);

        //ORBITAL PERIOD STEPSIZE POPUP

        orbitalPeriodStepPopup = new JPopupMenu();
        orbitalPeriodStepPanel = new JPanel();
        orbitalPeriodStepSpinner = new JSpinner(new SpinnerListModel(spinnerscalelist));
        orbitalPeriodStepSpinner.setValue(convertToText(orbitalPeriodStep));
        orbitalPeriodStepSpinner.addChangeListener(ev -> {
            double value = IJU.getTextSpinnerDoubleValue(orbitalPeriodStepSpinner);
            if (Double.isNaN(value)) return;
            orbitalPeriodStep = value;
            for (int c = 0; c < maxCurves; c++) {
                orbitalPeriodSpinner[c].setModel(new SpinnerNumberModel(orbitalPeriod[c], 0.001, null, orbitalPeriodStep));
                orbitalPeriodSpinner[c].setEditor(new JSpinner.NumberEditor(orbitalPeriodSpinner[c], "####0.##########"));
            }
            Prefs.set("plot.orbitalPeriodStep", orbitalPeriodStep);
        });

        JLabel orbitalPeriodStepLabel = new JLabel("Stepsize:");
        orbitalPeriodStepPanel.add(orbitalPeriodStepLabel);
        orbitalPeriodStepPanel.add(orbitalPeriodStepSpinner);
        orbitalPeriodStepPopup.add(orbitalPeriodStepPanel);
        orbitalPeriodStepPopup.setLightWeightPopupEnabled(false);
        orbitalPeriodStepPopup.addPopupMenuListener(new PopupMenuListener() {
            public void popupMenuCanceled(PopupMenuEvent popupMenuEvent) {
                orbitalPeriodStepPopup.setVisible(false);
            }

            public void popupMenuWillBecomeInvisible(PopupMenuEvent popupMenuEvent) {
                orbitalPeriodStepPopup.setVisible(true);
            }

            public void popupMenuWillBecomeVisible(PopupMenuEvent popupMenuEvent) {
                orbitalPeriodStepPopup.setVisible(false);
            }
        });

//ECCENTRICITY STEPSIZE POPUP

        eccentricityStepPopup = new JPopupMenu();
        eccentricityStepPanel = new JPanel();
        eccentricityStepSpinner = new JSpinner(new SpinnerListModel(spinnerscalelist));
        eccentricityStepSpinner.setValue(convertToText(eccentricityStep));
        eccentricityStepSpinner.addChangeListener(ev -> {
            double value = IJU.getTextSpinnerDoubleValue(eccentricityStepSpinner);
            if (Double.isNaN(value)) return;
            eccentricityStep = value;
            for (int c = 0; c < maxCurves; c++) {
                eccentricitySpinner[c].setModel(new SpinnerNumberModel(Double.valueOf(eccentricity[c]), Double.valueOf(0), Double.valueOf(1), Double.valueOf(eccentricityStep)));
                eccentricitySpinner[c].setEditor(new JSpinner.NumberEditor(eccentricitySpinner[c], "####0.##########"));
            }
            Prefs.set("plot.eccentricityStep", eccentricityStep);
        });

        JLabel eccentricityStepLabel = new JLabel("Stepsize:");
        eccentricityStepPanel.add(eccentricityStepLabel);
        eccentricityStepPanel.add(eccentricityStepSpinner);
        eccentricityStepPopup.add(eccentricityStepPanel);
        eccentricityStepPopup.setLightWeightPopupEnabled(false);
        eccentricityStepPopup.addPopupMenuListener(new PopupMenuListener() {
            public void popupMenuCanceled(PopupMenuEvent popupMenuEvent) {
                eccentricityStepPopup.setVisible(false);
            }

            public void popupMenuWillBecomeInvisible(PopupMenuEvent popupMenuEvent) {
                eccentricityStepPopup.setVisible(true);
            }

            public void popupMenuWillBecomeVisible(PopupMenuEvent popupMenuEvent) {
                eccentricityStepPopup.setVisible(false);
            }
        });

        //OMEGA STEPSIZE POPUP

        omegaStepPopup = new JPopupMenu();
        omegaStepPanel = new JPanel();
        omegaStepSpinner = new JSpinner(new SpinnerListModel(spinnerscalelist));
        omegaStepSpinner.setValue(convertToText(omegaStep));
        omegaStepSpinner.addChangeListener(ev -> {
            double value = IJU.getTextSpinnerDoubleValue(omegaStepSpinner);
            if (Double.isNaN(value)) return;
            omegaStep = value;
            for (int c = 0; c < maxCurves; c++) {
                omegaSpinner[c].setModel(new SpinnerNumberModel(Double.valueOf(omega[c]), Double.valueOf(-360.0), Double.valueOf(360.0), Double.valueOf(omegaStep)));
                omegaSpinner[c].setEditor(new JSpinner.NumberEditor(omegaSpinner[c], "####0.##########"));
            }
            Prefs.set("plot.omegaStep", omegaStep);
        });

        JLabel omegaStepLabel = new JLabel("Stepsize:");
        omegaStepPanel.add(omegaStepLabel);
        omegaStepPanel.add(omegaStepSpinner);
        omegaStepPopup.add(omegaStepPanel);
        omegaStepPopup.setLightWeightPopupEnabled(false);
        omegaStepPopup.addPopupMenuListener(new PopupMenuListener() {
            public void popupMenuCanceled(PopupMenuEvent popupMenuEvent) {
                omegaStepPopup.setVisible(false);
            }

            public void popupMenuWillBecomeInvisible(PopupMenuEvent popupMenuEvent) {
                omegaStepPopup.setVisible(true);
            }

            public void popupMenuWillBecomeVisible(PopupMenuEvent popupMenuEvent) {
                omegaStepPopup.setVisible(false);
            }
        });


        //TEFF STEPSIZE POPUP

        teffStepPopup = new JPopupMenu();
        teffStepPanel = new JPanel();
        teffStepSpinner = new JSpinner(new SpinnerListModel(spinnerscalelist));
        teffStepSpinner.setValue(convertToText(teffStep));
        teffStepSpinner.addChangeListener(ev -> {
            double value = IJU.getTextSpinnerDoubleValue(teffStepSpinner);
            if (Double.isNaN(value)) return;
            teffStep = value;
            for (int c = 0; c < maxCurves; c++) {
                teffSpinner[c].setModel(new SpinnerNumberModel(Double.valueOf(teff[c]), Double.valueOf(IJU.tStar[IJU.tStar.length - 1]), Double.valueOf(IJU.tStar[0]), Double.valueOf(teffStep)));
                teffSpinner[c].setEditor(new JSpinner.NumberEditor(teffSpinner[c], "####0"));
            }
            Prefs.set("plot.teffStep", teffStep);
        });

        JLabel teffStepLabel = new JLabel("Stepsize:");
        teffStepPanel.add(teffStepLabel);
        teffStepPanel.add(teffStepSpinner);
        teffStepPopup.add(teffStepPanel);
        teffStepPopup.setLightWeightPopupEnabled(false);
        teffStepPopup.addPopupMenuListener(new PopupMenuListener() {
            public void popupMenuCanceled(PopupMenuEvent popupMenuEvent) {
                teffStepPopup.setVisible(false);
            }

            public void popupMenuWillBecomeInvisible(PopupMenuEvent popupMenuEvent) {
                teffStepPopup.setVisible(true);
            }

            public void popupMenuWillBecomeVisible(PopupMenuEvent popupMenuEvent) {
                teffStepPopup.setVisible(false);
            }
        });

        //JMINUSK STEPSIZE POPUP

        jminuskStepPopup = new JPopupMenu();
        jminuskStepPanel = new JPanel();
        jminuskStepSpinner = new JSpinner(new SpinnerListModel(spinnerscalelist));
        jminuskStepSpinner.setValue(convertToText(jminuskStep));
        jminuskStepSpinner.addChangeListener(ev -> {
            double value = IJU.getTextSpinnerDoubleValue(jminuskStepSpinner);
            if (Double.isNaN(value)) return;
            jminuskStep = value;
            for (int c = 0; c < maxCurves; c++) {
                jminuskSpinner[c].setModel(new SpinnerNumberModel((Double) jminuskSpinner[c].getValue(), Double.valueOf(IJU.JminusK[0]), Double.valueOf(IJU.JminusK[IJU.JminusK.length - 1]), Double.valueOf(jminuskStep)));
                jminuskSpinner[c].setEditor(new JSpinner.NumberEditor(jminuskSpinner[c], "#0.000"));
            }
            Prefs.set("plot.jminuskStep", jminuskStep);
        });

        JLabel jminuskStepLabel = new JLabel("Stepsize:");
        jminuskStepPanel.add(jminuskStepLabel);
        jminuskStepPanel.add(jminuskStepSpinner);
        jminuskStepPopup.add(jminuskStepPanel);
        jminuskStepPopup.setLightWeightPopupEnabled(false);
        jminuskStepPopup.addPopupMenuListener(new PopupMenuListener() {
            public void popupMenuCanceled(PopupMenuEvent popupMenuEvent) {
                jminuskStepPopup.setVisible(false);
            }

            public void popupMenuWillBecomeInvisible(PopupMenuEvent popupMenuEvent) {
                jminuskStepPopup.setVisible(true);
            }

            public void popupMenuWillBecomeVisible(PopupMenuEvent popupMenuEvent) {
                jminuskStepPopup.setVisible(false);
            }
        });

        //MSTAR STEPSIZE POPUP

        mStarStepPopup = new JPopupMenu();
        mStarStepPanel = new JPanel();
        mStarStepSpinner = new JSpinner(new SpinnerListModel(spinnerscalelist));
        mStarStepSpinner.setValue(convertToText(mStarStep));
        mStarStepSpinner.addChangeListener(ev -> {
            double value = IJU.getTextSpinnerDoubleValue(mStarStepSpinner);
            if (Double.isNaN(value)) return;
            mStarStep = value;
            for (int c = 0; c < maxCurves; c++) {
                mStarSpinner[c].setModel(new SpinnerNumberModel((Double) mStarSpinner[c].getValue(), Double.valueOf(IJU.mStar[IJU.mStar.length - 1]), Double.valueOf(IJU.mStar[0]), Double.valueOf(mStarStep)));
                mStarSpinner[c].setEditor(new JSpinner.NumberEditor(mStarSpinner[c], "#0.000"));
            }
            Prefs.set("plot.mStarStep", mStarStep);
        });

        JLabel mStarStepLabel = new JLabel("Stepsize:");
        mStarStepPanel.add(mStarStepLabel);
        mStarStepPanel.add(mStarStepSpinner);
        mStarStepPopup.add(mStarStepPanel);
        mStarStepPopup.setLightWeightPopupEnabled(false);
        mStarStepPopup.addPopupMenuListener(new PopupMenuListener() {
            public void popupMenuCanceled(PopupMenuEvent popupMenuEvent) {
                mStarStepPopup.setVisible(false);
            }

            public void popupMenuWillBecomeInvisible(PopupMenuEvent popupMenuEvent) {
                mStarStepPopup.setVisible(true);
            }

            public void popupMenuWillBecomeVisible(PopupMenuEvent popupMenuEvent) {
                mStarStepPopup.setVisible(false);
            }
        });

        //RSTAR STEPSIZE POPUP

        rStarStepPopup = new JPopupMenu();
        rStarStepPanel = new JPanel();
        rStarStepSpinner = new JSpinner(new SpinnerListModel(spinnerscalelist));
        rStarStepSpinner.setValue(convertToText(rStarStep));
        rStarStepSpinner.addChangeListener(ev -> {
            double value = IJU.getTextSpinnerDoubleValue(rStarStepSpinner);
            if (Double.isNaN(value)) return;
            rStarStep = value;
            for (int c = 0; c < maxCurves; c++) {
                rStarSpinner[c].setModel(new SpinnerNumberModel((Double) rStarSpinner[c].getValue(), Double.valueOf(IJU.rStar[IJU.rStar.length - 1]), Double.valueOf(IJU.rStar[0]), Double.valueOf(rStarStep)));
                rStarSpinner[c].setEditor(new JSpinner.NumberEditor(rStarSpinner[c], "#0.000"));
            }
            Prefs.set("plot.rStarStep", rStarStep);
        });

        JLabel rStarStepLabel = new JLabel("Stepsize:");
        rStarStepPanel.add(rStarStepLabel);
        rStarStepPanel.add(rStarStepSpinner);
        rStarStepPopup.add(rStarStepPanel);
        rStarStepPopup.setLightWeightPopupEnabled(false);
        rStarStepPopup.addPopupMenuListener(new PopupMenuListener() {
            public void popupMenuCanceled(PopupMenuEvent popupMenuEvent) {
                rStarStepPopup.setVisible(false);
            }

            public void popupMenuWillBecomeInvisible(PopupMenuEvent popupMenuEvent) {
                rStarStepPopup.setVisible(true);
            }

            public void popupMenuWillBecomeVisible(PopupMenuEvent popupMenuEvent) {
                rStarStepPopup.setVisible(false);
            }
        });

        //RHOSTAR STEPSIZE POPUP

        rhoStarStepPopup = new JPopupMenu();
        rhoStarStepPanel = new JPanel();
        rhoStarStepSpinner = new JSpinner(new SpinnerListModel(spinnerscalelist));
        rhoStarStepSpinner.setValue(convertToText(rhoStarStep));
        rhoStarStepSpinner.addChangeListener(ev -> {
            double value = IJU.getTextSpinnerDoubleValue(rhoStarStepSpinner);
            if (Double.isNaN(value)) return;
            rhoStarStep = value;
            for (int c = 0; c < maxCurves; c++) {
                rhoStarSpinner[c].setModel(new SpinnerNumberModel((Double) rhoStarSpinner[c].getValue(), Double.valueOf(IJU.rhoStar[0]), Double.valueOf(IJU.rhoStar[IJU.rhoStar.length - 1]), Double.valueOf(rhoStarStep)));
                rhoStarSpinner[c].setEditor(new JSpinner.NumberEditor(rhoStarSpinner[c], "#0.000"));
            }
            Prefs.set("plot.rhoStarStep", rhoStarStep);
        });

        JLabel rhoStarStepLabel = new JLabel("Stepsize:");
        rhoStarStepPanel.add(rhoStarStepLabel);
        rhoStarStepPanel.add(rhoStarStepSpinner);
        rhoStarStepPopup.add(rhoStarStepPanel);
        rhoStarStepPopup.setLightWeightPopupEnabled(false);
        rhoStarStepPopup.addPopupMenuListener(new PopupMenuListener() {
            public void popupMenuCanceled(PopupMenuEvent popupMenuEvent) {
                rhoStarStepPopup.setVisible(false);
            }

            public void popupMenuWillBecomeInvisible(PopupMenuEvent popupMenuEvent) {
                rhoStarStepPopup.setVisible(true);
            }

            public void popupMenuWillBecomeVisible(PopupMenuEvent popupMenuEvent) {
                rhoStarStepPopup.setVisible(false);
            }
        });

        //FIT AND DETREND STEPSIZE POPUPS

        for (int p = 0; p < priorCenterStepPopup.length; p++) {
            buildFitAndDetrendPopups(p);
        }


    }

    static void buildFitAndDetrendPopups(final int p) {
        priorCenterStepPopup[p] = new JPopupMenu();
        priorCenterStepPanel[p] = new JPanel();
        priorCenterStepSpinner[p] = new JSpinner(new SpinnerListModel(spinnerscalelist));
        priorCenterStepSpinner[p].setValue(convertToText(priorCenterStep[p]));
        priorCenterStepSpinner[p].addChangeListener(ev -> {
            double value = IJU.getTextSpinnerDoubleValue(priorCenterStepSpinner[p]);
            if (Double.isNaN(value)) return;
            priorCenterStep[p] = value;
            for (int c = 0; c < maxCurves; c++) {
                if (p == 0) {
                    priorCenterSpinner[c][p].setModel(new SpinnerNumberModel(priorCenter[c][p], 0.0, null, priorCenterStep[p]));
                } else if (p == 1) {
                    priorCenterSpinner[c][p].setModel(new SpinnerNumberModel(priorCenter[c][p], 0.0, null, priorCenterStep[p]));
                } else if (p == 2) {
                    priorCenterSpinner[c][p].setModel(new SpinnerNumberModel(priorCenter[c][p], 0.0, null, priorCenterStep[p]));
                } else if (p == 3) {
                    priorCenterSpinner[c][p].setModel(new SpinnerNumberModel(priorCenter[c][p], 0.0, null, priorCenterStep[p]));
                } else if (p == 4) {
                    priorCenterSpinner[c][p].setModel(new SpinnerNumberModel(Double.valueOf(priorCenter[c][p]), Double.valueOf(0.0), Double.valueOf(90.0), Double.valueOf(priorCenterStep[p])));
                } else if (p == 5) {
                    priorCenterSpinner[c][p].setModel(new SpinnerNumberModel(Double.valueOf(priorCenter[c][p]), Double.valueOf(-1.0), Double.valueOf(1.0), Double.valueOf(priorCenterStep[p])));
                } else if (p == 6) {
                    priorCenterSpinner[c][p].setModel(new SpinnerNumberModel(Double.valueOf(priorCenter[c][p]), Double.valueOf(-1.0), Double.valueOf(1.0), Double.valueOf(priorCenterStep[p])));
                } else {
                    priorCenterSpinner[c][p].setModel(new SpinnerNumberModel(priorCenter[c][p], null, null, priorCenterStep[p]));
                }
                priorCenterSpinner[c][p].setEditor(new JSpinner.NumberEditor(priorCenterSpinner[c][p], fitFormat));
            }
            Prefs.set("plot.priorCenterStep[" + p + "]", priorCenterStep[p]);
        });

        JLabel priorCenterStepLabel = new JLabel("Stepsize:");
        priorCenterStepPanel[p].add(priorCenterStepLabel);
        priorCenterStepPanel[p].add(priorCenterStepSpinner[p]);
        priorCenterStepPopup[p].add(priorCenterStepPanel[p]);
        priorCenterStepPopup[p].setLightWeightPopupEnabled(false);
        priorCenterStepPopup[p].addPopupMenuListener(new PopupMenuListener() {
            public void popupMenuCanceled(PopupMenuEvent popupMenuEvent) {
                priorCenterStepPopup[p].setVisible(false);
            }

            public void popupMenuWillBecomeInvisible(PopupMenuEvent popupMenuEvent) {
                priorCenterStepPopup[p].setVisible(true);
            }

            public void popupMenuWillBecomeVisible(PopupMenuEvent popupMenuEvent) {
                priorCenterStepPopup[p].setVisible(false);
            }
        });

        priorWidthStepPopup[p] = new JPopupMenu();
        priorWidthStepPanel[p] = new JPanel();
        priorWidthStepSpinner[p] = new JSpinner(new SpinnerListModel(spinnerscalelist));
        priorWidthStepSpinner[p].setValue(convertToText(priorWidthStep[p]));
        priorWidthStepSpinner[p].addChangeListener(ev -> {
            double value = IJU.getTextSpinnerDoubleValue(priorWidthStepSpinner[p]);
            if (Double.isNaN(value)) return;
            priorWidthStep[p] = value;
            for (int c = 0; c < maxCurves; c++) {
                priorWidthSpinner[c][p].setModel(new SpinnerNumberModel(priorWidth[c][p], 0.0, null, priorWidthStep[p]));
                priorWidthSpinner[c][p].setEditor(new JSpinner.NumberEditor(priorWidthSpinner[c][p], fitFormat));
            }
            Prefs.set("plot.priorWidthStep[" + p + "]", priorWidthStep[p]);
        });

        JLabel priorWidthStepLabel = new JLabel("Stepsize:");
        priorWidthStepPanel[p].add(priorWidthStepLabel);
        priorWidthStepPanel[p].add(priorWidthStepSpinner[p]);
        priorWidthStepPopup[p].add(priorWidthStepPanel[p]);
        priorWidthStepPopup[p].setLightWeightPopupEnabled(false);
        priorWidthStepPopup[p].addPopupMenuListener(new PopupMenuListener() {
            public void popupMenuCanceled(PopupMenuEvent popupMenuEvent) {
                priorWidthStepPopup[p].setVisible(false);
            }

            public void popupMenuWillBecomeInvisible(PopupMenuEvent popupMenuEvent) {
                priorWidthStepPopup[p].setVisible(true);
            }

            public void popupMenuWillBecomeVisible(PopupMenuEvent popupMenuEvent) {
                priorWidthStepPopup[p].setVisible(false);
            }
        });

        fitStepStepPopup[p] = new JPopupMenu();
        fitStepStepPanel[p] = new JPanel();
        fitStepStepSpinner[p] = new JSpinner(new SpinnerListModel(spinnerscalelist));
        fitStepStepSpinner[p].setValue(convertToText(fitStepStep[p]));
        fitStepStepSpinner[p].addChangeListener(ev -> {
            double value = IJU.getTextSpinnerDoubleValue(fitStepStepSpinner[p]);
            if (Double.isNaN(value)) return;
            fitStepStep[p] = value;
            for (int c = 0; c < maxCurves; c++) {
                fitStepSpinner[c][p].setModel(new SpinnerNumberModel(fitStep[c][p], 0.0, null, fitStepStep[p]));
                fitStepSpinner[c][p].setEditor(new JSpinner.NumberEditor(fitStepSpinner[c][p], fitFormat));
            }
            Prefs.set("plot.fitStepStep[" + p + "]", fitStepStep[p]);
        });

        JLabel fitStepStepLabel = new JLabel("Stepsize:");
        fitStepStepPanel[p].add(fitStepStepLabel);
        fitStepStepPanel[p].add(fitStepStepSpinner[p]);
        fitStepStepPopup[p].add(fitStepStepPanel[p]);
        fitStepStepPopup[p].setLightWeightPopupEnabled(false);
        fitStepStepPopup[p].addPopupMenuListener(new PopupMenuListener() {
            public void popupMenuCanceled(PopupMenuEvent popupMenuEvent) {
                fitStepStepPopup[p].setVisible(false);
            }

            public void popupMenuWillBecomeInvisible(PopupMenuEvent popupMenuEvent) {
                fitStepStepPopup[p].setVisible(true);
            }

            public void popupMenuWillBecomeVisible(PopupMenuEvent popupMenuEvent) {
                fitStepStepPopup[p].setVisible(false);
            }
        });

    }


    static void createFitPanel(final int c) {
        if (fitFrame[c] != null && fitFrame[c].isShowing()) return;
        int nlines = 1;
        fitFrame[c] = new JFrame("Data Set " + (c + 1) + " Fit Settings");
        fitFrame[c].setIconImage(fitFrameIcon.getImage());
        fitFrame[c].addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                fitFrameLocationX[c] = fitFrame[c].getLocation().x;
                fitFrameLocationY[c] = fitFrame[c].getLocation().y;
                fitFrame[c].setVisible(false);
                Prefs.set("plot2.fitFrameLocationX" + c, fitFrameLocationX[c]);
                Prefs.set("plot2.fitFrameLocationY" + c, fitFrameLocationY[c]);
            }
        });

        fitPanel[c] = new JPanel(new SpringLayout());
//            fitPanel[c].setBorder(BorderFactory.createLineBorder(color[c], 2));
        fitPanel[c].setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(color[c], 2), table == null ? "No Table Selected" : ylabel[c].trim().equals("") ? "No Data Column Selected" : ylabel[c], TitledBorder.CENTER, TitledBorder.TOP, b12, Color.darkGray));

        fitMenuBar[c] = new JMenuBar();
        fitFileMenu[c] = new JMenu("File    ");

        saveFitPanelPngMenuItem[c] = new JMenuItem("Save image of fit panel as PNG file");
        saveFitPanelPngMenuItem[c].addActionListener(e -> {
            String filename = table != null ? MeasurementTable.shorterName(table.shortTitle()) : "No_Table_Data";
            int location = filename.lastIndexOf('.');
            if (location >= 0) filename = filename.substring(0, location);
            filename = filename + "_fitpanel" + twoDigits.format(c + 1) + "_" + ylabel[c] + ".png";
            SaveDialog sf = new SaveDialog("Save Fit Panel Image as PNG file", filename, "");
            if (sf.getDirectory() == null || sf.getFileName() == null) return;
            String outPath = sf.getDirectory() + sf.getFileName();
            location = outPath.lastIndexOf('.');
            if (location >= 0) outPath = outPath.substring(0, location);

            BufferedImage bi = new BufferedImage(fitPanel[c].getSize().width, fitPanel[c].getSize().height, BufferedImage.TYPE_INT_RGB);
            Graphics gg = bi.createGraphics();
            fitPanel[c].paint(gg);
            gg.dispose();
            IJU.saveAsPngOrJpg(bi, new File(outPath + ".png"), "png");
        });
        fitFileMenu[c].add(saveFitPanelPngMenuItem[c]);

        saveFitPanelJpgMenuItem[c] = new JMenuItem("Save image of fit panel as JPG file");
        saveFitPanelJpgMenuItem[c].addActionListener(e -> {
            String filename = table != null ? MeasurementTable.shorterName(table.shortTitle()) : "No_Table_Data";
            int location = filename.lastIndexOf('.');
            if (location >= 0) filename = filename.substring(0, location);
            filename = filename + "_fitpanel" + twoDigits.format(c + 1) + "_" + ylabel[c] + ".jpg";
            SaveDialog sf = new SaveDialog("Save Fit Panel Image as JPG file", filename, "");
            if (sf.getDirectory() == null || sf.getFileName() == null) return;
            String outPath = sf.getDirectory() + sf.getFileName();
            location = outPath.lastIndexOf('.');
            if (location >= 0) outPath = outPath.substring(0, location);

            BufferedImage bi = new BufferedImage(fitPanel[c].getSize().width, fitPanel[c].getSize().height, BufferedImage.TYPE_INT_RGB);
            Graphics gg = bi.createGraphics();
            fitPanel[c].paint(gg);
            gg.dispose();
            IJU.saveAsPngOrJpg(bi, new File(outPath + ".jpg"), "jpg");
        });
        fitFileMenu[c].add(saveFitPanelJpgMenuItem[c]);

        fitFileMenu[c].addSeparator();

        saveFitTextMenuItem[c] = new JMenuItem("Save fit results as text file");
        saveFitTextMenuItem[c].addActionListener(e -> saveFitPanelToTextFileDialog(null, c));
        fitFileMenu[c].add(saveFitTextMenuItem[c]);


        autoPriorsMenu[c] = new JMenu("Auto Priors    ");


        baselinePriorCB[c] = new JCheckBoxMenuItem("Include baseline in auto prior update", autoUpdatePrior[c][0]);
        baselinePriorCB[c].addItemListener(e -> {
            autoUpdatePrior[c][0] = e.getStateChange() == ItemEvent.SELECTED;
            enableTransitComponents(c);
            if (autoUpdatePriors[c] && autoUpdatePrior[c][0]) updatePlot(updateOneFit(c));
            Prefs.set("plot.autoUpdatePrior[" + c + "][0]", autoUpdatePrior[c][0]);
        });
        autoPriorsMenu[c].add(baselinePriorCB[c]);

        depthPriorCB[c] = new JCheckBoxMenuItem("Include transit depth in auto prior update", autoUpdatePrior[c][1]);
        depthPriorCB[c].addItemListener(e -> {
            autoUpdatePrior[c][1] = e.getStateChange() == ItemEvent.SELECTED;
            enableTransitComponents(c);
            if (autoUpdatePriors[c] && autoUpdatePrior[c][1]) updatePlot(updateOneFit(c));
            Prefs.set("plot.autoUpdatePrior[" + c + "][1]", autoUpdatePrior[c][1]);
        });
        autoPriorsMenu[c].add(depthPriorCB[c]);

        arPriorCB[c] = new JCheckBoxMenuItem("Include a/R* in auto prior update", autoUpdatePrior[c][2]);
        arPriorCB[c].addItemListener(e -> {
            autoUpdatePrior[c][2] = e.getStateChange() == ItemEvent.SELECTED;
            enableTransitComponents(c);
            if (autoUpdatePriors[c] && autoUpdatePrior[c][2]) updatePlot(updateOneFit(c));
            Prefs.set("plot.autoUpdatePrior[" + c + "][2]", autoUpdatePrior[c][2]);
        });
        autoPriorsMenu[c].add(arPriorCB[c]);

        tcPriorCB[c] = new JCheckBoxMenuItem("Include transit center time in auto prior update", autoUpdatePrior[c][3]);
        tcPriorCB[c].addItemListener(e -> {
            autoUpdatePrior[c][3] = e.getStateChange() == ItemEvent.SELECTED;
            enableTransitComponents(c);
            if (autoUpdatePriors[c] && autoUpdatePrior[c][3]) updatePlot(updateOneFit(c));
            Prefs.set("plot.autoUpdatePrior[" + c + "][3]", autoUpdatePrior[c][3]);
        });
        autoPriorsMenu[c].add(tcPriorCB[c]);

        inclPriorCB[c] = new JCheckBoxMenuItem("Include transit inclination in auto prior update", autoUpdatePrior[c][4]);
        inclPriorCB[c].addItemListener(e -> {
            autoUpdatePrior[c][4] = e.getStateChange() == ItemEvent.SELECTED;
            enableTransitComponents(c);
            if (autoUpdatePriors[c] && autoUpdatePrior[c][4]) updatePlot(updateOneFit(c));
            Prefs.set("plot.autoUpdatePrior[" + c + "][4]", autoUpdatePrior[c][4]);
        });
        autoPriorsMenu[c].add(inclPriorCB[c]);

        fitMenuBar[c].add(fitFileMenu[c]);
        fitMenuBar[c].add(autoPriorsMenu[c]);

        JPanel specifiedParmetersPanel = new JPanel(new SpringLayout());
        specifiedParmetersPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(mainBorderColor, 1), "User Specified Parameters (not fitted)", TitledBorder.LEFT, TitledBorder.TOP, b12, Color.darkGray));

        JPanel orbitalParmetersPanel = new JPanel(new SpringLayout());
        orbitalParmetersPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(mainBorderColor, 1), "Orbital Parameters", TitledBorder.LEFT, TitledBorder.TOP, b12, Color.darkGray));

//            JLabel periodLabel = new JLabel("Orbital Parameters");
//            periodLabel.setFont(p12);
//            periodLabel.setPreferredSize(labelSize);
//            periodLabel.setMaximumSize(labelSize);
//            specifiedParmetersPanel.add(periodLabel);

//            JLabel periodDummyLabel1 = new JLabel("");
//            specifiedParmetersPanel.add(periodDummyLabel1);

        JPanel orbitalPeriodPanel = new JPanel(new SpringLayout());
        orbitalPeriodPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(subBorderColor, 1), "Period (days)", TitledBorder.CENTER, TitledBorder.TOP, p11, Color.darkGray));

        orbitalPeriodSpinner[c] = new JSpinner(new SpinnerNumberModel(orbitalPeriod[c], 0.001, null, orbitalPeriodStep));
        orbitalPeriodSpinner[c].setFont(p11);
        orbitalPeriodSpinner[c].setEditor(new JSpinner.NumberEditor(orbitalPeriodSpinner[c], fitFormat));
        orbitalPeriodSpinner[c].setPreferredSize(new Dimension(100, 25));
        orbitalPeriodSpinner[c].setMaximumSize(new Dimension(100, 25));
        orbitalPeriodSpinner[c].setComponentPopupMenu(orbitalPeriodStepPopup);
        orbitalPeriodSpinner[c].setToolTipText("<html>Enter orbital period of eclipsing object.<br>" + "The orbital period of a transiting planet is not well constrained by<br>" + "a single transit light curve and must be specified from prior data.<br>" + "---------------------------------------------<br>" + "Right click to set spinner stepsize</html>");
        orbitalPeriodSpinner[c].addChangeListener(ev -> {
            orbitalPeriod[c] = (Double) orbitalPeriodSpinner[c].getValue();
            if (autoUpdatePriors[c]) updatePriorCenters(c);
            if (autoUpdateFit[c]) updatePlot(updateOneFit(c));
        });
        orbitalPeriodSpinner[c].addChangeListener(acc);

        orbitalPeriodSpinner[c].addMouseWheelListener(e -> {
            double newValue = (Double) orbitalPeriodSpinner[c].getValue() - e.getWheelRotation() * orbitalPeriodStep;
            orbitalPeriodSpinner[c].setValue(Math.max(newValue, 0.001));
        });
        orbitalPeriodSpinner[c].setEnabled(true);
        orbitalPeriodPanel.add(orbitalPeriodSpinner[c]);
        SpringUtil.makeCompactGrid(orbitalPeriodPanel, 1, orbitalPeriodPanel.getComponentCount(), 2, 0, 0, 0);
        orbitalParmetersPanel.add(orbitalPeriodPanel);

        JLabel periodDummyLabel2 = new JLabel("");
        periodDummyLabel2.setPreferredSize(new Dimension(10, 25));
        periodDummyLabel2.setMaximumSize(new Dimension(10, 25));
        orbitalParmetersPanel.add(periodDummyLabel2);
//
//            nlines++;

//            JLabel eccentricityLabel = new JLabel("Eccentricity");
//            eccentricityLabel.setFont(b12);
//            eccentricityLabel.setToolTipText("<html>Enter eccentricity of eclipsing object's orbit.<br>"+
//                "The eccentricity of a transiting planet's orbit is not well constrained by<br>"+
//                "a transit light curve and must be specified from prior data.<br>"+
//                "---------------------------------------------<br>"+
//                "Right click to set spinner stepsize</html>");
//            specifiedParmetersPanel.add(eccentricityLabel);

        JPanel forceCircularOrbitPanel = new JPanel(new SpringLayout());
        forceCircularOrbitPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(subBorderColor, 1), "Cir", TitledBorder.CENTER, TitledBorder.TOP, p11, Color.darkGray));
        forceCircularOrbitCB[c] = new JCheckBox("", forceCircularOrbit[c]);
        forceCircularOrbitCB[c].setToolTipText("Enable to force a circular orbit");
        forceCircularOrbitCB[c].addItemListener(e -> {
            if (e.getStateChange() == ItemEvent.DESELECTED) {
                forceCircularOrbit[c] = false;
                eccentricitySpinner[c].setEnabled(true);
                omegaSpinner[c].setEnabled(true);
            } else if (e.getStateChange() == ItemEvent.SELECTED) {
                forceCircularOrbit[c] = true;
                eccentricitySpinner[c].setEnabled(false);
                omegaSpinner[c].setEnabled(false);
            }
            if (autoUpdateFit[c]) updatePlot(updateOneFit(c));
        });
        forceCircularOrbitCB[c].setHorizontalAlignment(JLabel.CENTER);
        forceCircularOrbitPanel.add(forceCircularOrbitCB[c]);
        SpringUtil.makeCompactGrid(forceCircularOrbitPanel, 1, forceCircularOrbitPanel.getComponentCount(), 2, 0, 0, 0);
        orbitalParmetersPanel.add(forceCircularOrbitPanel);

        JPanel eccentricitySpinnerPanel = new JPanel(new SpringLayout());
        eccentricitySpinnerPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(subBorderColor, 1), "Ecc", TitledBorder.CENTER, TitledBorder.TOP, p11, Color.darkGray));
        eccentricitySpinner[c] = new JSpinner(new SpinnerNumberModel(Double.valueOf(eccentricity[c]), Double.valueOf(0.0), Double.valueOf(1.0), Double.valueOf(eccentricityStep)));
        eccentricitySpinner[c].setEnabled(!forceCircularOrbit[c]);
        eccentricitySpinner[c].setFont(p11);
        eccentricitySpinner[c].setEditor(new JSpinner.NumberEditor(eccentricitySpinner[c], fitFormat));
        eccentricitySpinner[c].setPreferredSize(orbitalSpinnerSize);
        eccentricitySpinner[c].setMaximumSize(orbitalSpinnerSize);
        eccentricitySpinner[c].setComponentPopupMenu(eccentricityStepPopup);
        eccentricitySpinner[c].setToolTipText("<html>Enter eccentricity of eclipsing object's orbit.<br>" + "The eccentricity of a transiting planet's orbit is not well constrained by<br>" + "a transit light curve and must be specified from prior data.<br>" + "---------------------------------------------<br>" + "Right click to set spinner stepsize</html>");
        eccentricitySpinner[c].addChangeListener(ev -> {
            eccentricity[c] = (Double) eccentricitySpinner[c].getValue();
            if (autoUpdateFit[c]) updatePlot(updateOneFit(c));
        });
        eccentricitySpinner[c].addMouseWheelListener(e -> {
            double newValue = (Double) eccentricitySpinner[c].getValue() - e.getWheelRotation() * eccentricityStep;
            if (eccentricitySpinner[c].isEnabled()) {
                if (newValue < 0.0) { eccentricitySpinner[c].setValue(0.0); } else {
                    eccentricitySpinner[c].setValue(Math.min(newValue, 1.0));
                }
            }
        });
        eccentricitySpinnerPanel.add(eccentricitySpinner[c]);
        SpringUtil.makeCompactGrid(eccentricitySpinnerPanel, 1, eccentricitySpinnerPanel.getComponentCount(), 2, 0, 0, 0);
        orbitalParmetersPanel.add(eccentricitySpinnerPanel);

        JPanel omegaSpinnerPanel = new JPanel(new SpringLayout());
        omegaSpinnerPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(subBorderColor, 1), "\u03C9 (deg)", TitledBorder.CENTER, TitledBorder.TOP, p11, Color.darkGray));
        omegaSpinner[c] = new JSpinner(new SpinnerNumberModel(Double.valueOf(omega[c]), Double.valueOf(-360), Double.valueOf(360), Double.valueOf(omegaStep)));
        omegaSpinner[c].setEnabled(!forceCircularOrbit[c]);
        omegaSpinner[c].setFont(p11);
        omegaSpinner[c].setEditor(new JSpinner.NumberEditor(omegaSpinner[c], fitFormat));
        omegaSpinner[c].setPreferredSize(orbitalSpinnerSize);
        omegaSpinner[c].setMaximumSize(orbitalSpinnerSize);
        omegaSpinner[c].setComponentPopupMenu(omegaStepPopup);
        omegaSpinner[c].setToolTipText("<html>Enter the argument of periastron of eclipsing object's orbit.<br>" + "The argument of periastron of a transiting planet's orbit is not well constrained by<br>" + "a transit light curve and must be specified from prior data.<br>" + "---------------------------------------------<br>" + "Right click to set spinner stepsize</html>");
        omegaSpinner[c].addChangeListener(ev -> {
            omega[c] = (Double) omegaSpinner[c].getValue();
            if (autoUpdateFit[c]) updatePlot(updateOneFit(c));
        });
        omegaSpinner[c].addMouseWheelListener(e -> {
            double newValue = (Double) omegaSpinner[c].getValue() - e.getWheelRotation() * omegaStep;
            if (omegaSpinner[c].isEnabled()) {
                if (newValue < -360.0) { omegaSpinner[c].setValue(-360.0); } else {
                    omegaSpinner[c].setValue(Math.min(newValue, 360.0));
                }
            }
        });
        omegaSpinnerPanel.add(omegaSpinner[c]);

        SpringUtil.makeCompactGrid(omegaSpinnerPanel, 1, omegaSpinnerPanel.getComponentCount(), 2, 0, 0, 0);
        orbitalParmetersPanel.add(omegaSpinnerPanel);

        SpringUtil.makeCompactGrid(orbitalParmetersPanel, 1, orbitalParmetersPanel.getComponentCount(), 2, 0, 0, 0);
        specifiedParmetersPanel.add(orbitalParmetersPanel);

        JPanel stellarParmetersPanel = new JPanel(new SpringLayout());
        stellarParmetersPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(Color.pink, 1), "Host Star Parameters (enter one)", TitledBorder.LEFT, TitledBorder.TOP, b12, Color.darkGray));
        stellarParmetersPanel.setToolTipText("<html>Enter any host star parameter to calculate the others and the planet radius.<br>" + "This setting is required only if a planetary radius estimation<br>" + "is to be calculated from the light curve fit.</html>");
        JPanel spectralTypeSelectionPanel = new JPanel(new SpringLayout());
        spectralTypeSelectionPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(subBorderColor, 1), "Sp.T.", TitledBorder.CENTER, TitledBorder.TOP, p11, Color.darkGray));
        spectralTypeSelection[c] = new JComboBox<>(IJU.spType);
        spectralTypeSelection[c].setFont(b11);
//                        markercolorselection[c].setPreferredSize(new Dimension(100, 25));
        spectralTypeSelection[c].setSelectedItem(IJU.getSpTFromTeff(teff[c]));
//            spectralTypeSelection[c].setFont(new Font("Dialog", Font.BOLD, 12));
//            spectralTypeSelection[c].setForeground(color[c]);
        spectralTypeSelection[c].setPreferredSize(new Dimension(60, 25));
        spectralTypeSelection[c].setMaximumSize(new Dimension(60, 25));
        spectralTypeSelection[c].setToolTipText("<html>Select the host star spectral type to calculate the other<br>" + "stellar parameters and the planet radius.<br>" + "This setting is required only if a planetary radius estimation<br>" + "is to be calculated from the light curve fit.</html>");
        spectralTypeSelection[c].setMaximumRowCount(IJU.tStar.length);
        spectralTypeSelection[c].setPrototypeDisplayValue("123");
        spectralTypeSelection[c].addActionListener(ae -> {
            if (!spectralTypeSelection[c].isEditable()) {
                double newValue = IJU.getTeffFromSpT((String) spectralTypeSelection[c].getSelectedItem());
                if (newValue < IJU.tStar[IJU.tStar.length - 1]) {
                    teffSpinner[c].setValue(IJU.tStar[IJU.tStar.length - 1]);
                } else { teffSpinner[c].setValue(Math.min(newValue, IJU.tStar[0])); }
            }
        });
        spectralTypeSelectionPanel.add(spectralTypeSelection[c]);

        SpringUtil.makeCompactGrid(spectralTypeSelectionPanel, 1, spectralTypeSelectionPanel.getComponentCount(), 2, 0, 0, 0);
        stellarParmetersPanel.add(spectralTypeSelectionPanel);


        JPanel teffSpinnerPanel = new JPanel(new SpringLayout());
        if (teff[c] < IJU.tStar[IJU.tStar.length - 1]) teff[c] = IJU.tStar[IJU.tStar.length - 1];
        if (teff[c] > IJU.tStar[0]) teff[c] = IJU.tStar[0];
        teffSpinnerPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(subBorderColor, 1), "Teff (K)", TitledBorder.CENTER, TitledBorder.TOP, p11, Color.darkGray));
        teffSpinner[c] = new JSpinner(new SpinnerNumberModel(Double.valueOf(teff[c]), Double.valueOf(IJU.tStar[IJU.tStar.length - 1]), Double.valueOf(IJU.tStar[0]), Double.valueOf(teffStep)));
        teffSpinner[c].setFont(p11);
        teffSpinner[c].setEditor(new JSpinner.NumberEditor(teffSpinner[c], "####0"));
        teffSpinner[c].setPreferredSize(stellarSpinnerSize);
        teffSpinner[c].setMaximumSize(stellarSpinnerSize);
        teffSpinner[c].setComponentPopupMenu(teffStepPopup);
        teffSpinner[c].setToolTipText("<html>Enter the effective temperature of the host star in K and press enter<br>" + "to calculate the other stellar parameters and the planet radius.<br>" + "This value is required only if a planetary radius estimation<br>" + "is to be calculated from the light curve fit.<br>" + "---------------------------------------------<br>" + "Right click to set spinner stepsize</html>");
        teffSpinner[c].addChangeListener(ev -> {
            teff[c] = (Double) teffSpinner[c].getValue();

            spectralTypeSelection[c].setEditable(true);
            spectralTypeSelection[c].setSelectedItem(IJU.getSpTFromTeff(teff[c]));
            spectralTypeSelection[c].setEditable(false);

            jminuskSpinner[c].setEnabled(false);
            jminuskSpinner[c].setValue(IJU.getJminusKFromTeff(teff[c]));
            jminuskSpinner[c].setEnabled(true);

            mStarSpinner[c].setEnabled(false);
            mStarSpinner[c].setValue(IJU.getMStarFromTeff(teff[c]));
            mStarSpinner[c].setEnabled(true);

            rStarSpinner[c].setEnabled(false);
            rStarSpinner[c].setValue(IJU.getRStarFromTeff(teff[c]));
            rStarSpinner[c].setEnabled(true);

            rhoStarSpinner[c].setEnabled(false);
            rhoStarSpinner[c].setValue(IJU.getRhoStarFromTeff(teff[c]));
            rhoStarSpinner[c].setEnabled(true);

            if (useTransitFit[c] && table != null) {
                planetRadiusLabel[c].setText(IJU.planetRadiusFromTeff(teff[c], bestFit[c][1]));
            }
        });
        teffSpinner[c].addMouseWheelListener(e -> {
            double newValue = (Double) teffSpinner[c].getValue() - e.getWheelRotation() * teffStep;
            if (teffSpinner[c].isEnabled()) {
                if (newValue < IJU.tStar[IJU.tStar.length - 1]) {
                    teffSpinner[c].setValue(IJU.tStar[IJU.tStar.length - 1]);
                } else { teffSpinner[c].setValue(Math.min(newValue, IJU.tStar[0])); }
            }
        });
        teffSpinnerPanel.add(teffSpinner[c]);

        SpringUtil.makeCompactGrid(teffSpinnerPanel, 1, teffSpinnerPanel.getComponentCount(), 2, 0, 0, 0);
        stellarParmetersPanel.add(teffSpinnerPanel);


        JPanel jminuskSpinnerPanel = new JPanel(new SpringLayout());
        jminuskSpinnerPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(subBorderColor, 1), "J-K", TitledBorder.CENTER, TitledBorder.TOP, p11, Color.darkGray));
        jminuskSpinner[c] = new JSpinner(new SpinnerNumberModel(Double.valueOf(IJU.getJminusKFromTeff(teff[c])), Double.valueOf(IJU.JminusK[0]), Double.valueOf(IJU.JminusK[IJU.JminusK.length - 1]), Double.valueOf(jminuskStep)));
        jminuskSpinner[c].setFont(p11);
        jminuskSpinner[c].setEditor(new JSpinner.NumberEditor(jminuskSpinner[c], "#0.000"));
        jminuskSpinner[c].setPreferredSize(stellarSpinnerSize);
        jminuskSpinner[c].setMaximumSize(stellarSpinnerSize);
        jminuskSpinner[c].setComponentPopupMenu(jminuskStepPopup);
        jminuskSpinner[c].setToolTipText("<html>Enter J-K for the host star and press enter to calculate<br>" + "the other stellar parameters and the planet radius.<br>" + "This value is required only if a planetary radius estimation<br>" + "is to be calculated from the light curve fit.<br>" + "---------------------------------------------<br>" + "Right click to set spinner stepsize</html>");
        jminuskSpinner[c].addChangeListener(ev -> {
            if (jminuskSpinner[c].isEnabled()) {
                teffSpinner[c].setValue(IJU.getTeffFromJminusK((Double) jminuskSpinner[c].getValue()));
            }
        });
        jminuskSpinner[c].addMouseWheelListener(e -> {
            double newValue = (Double) jminuskSpinner[c].getValue() - e.getWheelRotation() * jminuskStep;
            if (jminuskSpinner[c].isEnabled()) {
                if (newValue < IJU.JminusK[0]) { jminuskSpinner[c].setValue(IJU.JminusK[0]); } else {
                    jminuskSpinner[c].setValue(Math.min(newValue, IJU.JminusK[IJU.JminusK.length - 1]));
                }
            }
        });
        jminuskSpinnerPanel.add(jminuskSpinner[c]);

        SpringUtil.makeCompactGrid(jminuskSpinnerPanel, 1, jminuskSpinnerPanel.getComponentCount(), 2, 0, 0, 0);
        stellarParmetersPanel.add(jminuskSpinnerPanel);


        JPanel rStarSpinnerPanel = new JPanel(new SpringLayout());
        rStarSpinnerPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(subBorderColor, 1), "R* (Rsun)", TitledBorder.CENTER, TitledBorder.TOP, p11, Color.darkGray));
        rStarSpinner[c] = new JSpinner(new SpinnerNumberModel(Double.valueOf(IJU.getRStarFromTeff(teff[c])), Double.valueOf(IJU.rStar[IJU.rStar.length - 1]), Double.valueOf(IJU.rStar[0]), Double.valueOf(rStarStep)));
        rStarSpinner[c].setFont(p11);
        rStarSpinner[c].setEditor(new JSpinner.NumberEditor(rStarSpinner[c], "#0.000"));
        rStarSpinner[c].setPreferredSize(stellarSpinnerSize);
        rStarSpinner[c].setMaximumSize(stellarSpinnerSize);
        rStarSpinner[c].setComponentPopupMenu(rStarStepPopup);
        rStarSpinner[c].setToolTipText("<html>Enter the radius of the host star and press enter to calculate<br>" + "the other stellar parameters and the planet radius.<br>" + "This value is required only if a planetary radius estimation<br>" + "is to be calculated from the light curve fit.<br>" + "---------------------------------------------<br>" + "Right click to set spinner stepsize</html>");
        rStarSpinner[c].addChangeListener(ev -> {
            if (rStarSpinner[c].isEnabled()) {
                teffSpinner[c].setValue(IJU.getTeffFromRStar((Double) rStarSpinner[c].getValue()));
            }
        });
        rStarSpinner[c].addMouseWheelListener(e -> {
            double newValue = (Double) rStarSpinner[c].getValue() - e.getWheelRotation() * rStarStep;
            if (rStarSpinner[c].isEnabled()) {
                if (newValue < IJU.rStar[IJU.rStar.length - 1]) {
                    rStarSpinner[c].setValue(IJU.rStar[IJU.rStar.length - 1]);
                } else { rStarSpinner[c].setValue(Math.min(newValue, IJU.rStar[0])); }
            }
        });
        rStarSpinnerPanel.add(rStarSpinner[c]);

        SpringUtil.makeCompactGrid(rStarSpinnerPanel, 1, rStarSpinnerPanel.getComponentCount(), 2, 0, 0, 0);
        stellarParmetersPanel.add(rStarSpinnerPanel);

        JPanel mStarSpinnerPanel = new JPanel(new SpringLayout());
        mStarSpinnerPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(subBorderColor, 1), "M* (Msun)", TitledBorder.CENTER, TitledBorder.TOP, p11, Color.darkGray));
        mStarSpinner[c] = new JSpinner(new SpinnerNumberModel(Double.valueOf(IJU.getMStarFromTeff(teff[c])), Double.valueOf(IJU.mStar[IJU.mStar.length - 1]), Double.valueOf(IJU.mStar[0]), Double.valueOf(mStarStep)));
        mStarSpinner[c].setFont(p11);
        mStarSpinner[c].setEditor(new JSpinner.NumberEditor(mStarSpinner[c], "#0.000"));
        mStarSpinner[c].setPreferredSize(stellarSpinnerSize);
        mStarSpinner[c].setMaximumSize(stellarSpinnerSize);
        mStarSpinner[c].setComponentPopupMenu(mStarStepPopup);
        mStarSpinner[c].setToolTipText("<html>Enter the mass of the host star and press enter to calculate<br>" + "the other stellar parameters and the planet radius.<br>" + "This value is required only if a planetary radius estimation<br>" + "is to be calculated from the light curve fit.<br>" + "---------------------------------------------<br>" + "Right click to set spinner stepsize</html>");
        mStarSpinner[c].addChangeListener(ev -> {
            if (mStarSpinner[c].isEnabled()) {
                teffSpinner[c].setValue(IJU.getTeffFromMStar((Double) mStarSpinner[c].getValue()));
            }
        });
        mStarSpinner[c].addMouseWheelListener(e -> {
            double newValue = (Double) mStarSpinner[c].getValue() - e.getWheelRotation() * mStarStep;
            if (mStarSpinner[c].isEnabled()) {
                if (newValue < IJU.mStar[IJU.mStar.length - 1]) {
                    mStarSpinner[c].setValue(IJU.mStar[IJU.mStar.length - 1]);
                } else { mStarSpinner[c].setValue(Math.min(newValue, IJU.mStar[0])); }
            }
        });
        mStarSpinnerPanel.add(mStarSpinner[c]);

        SpringUtil.makeCompactGrid(mStarSpinnerPanel, 1, mStarSpinnerPanel.getComponentCount(), 2, 0, 0, 0);
        stellarParmetersPanel.add(mStarSpinnerPanel);

        JPanel rhoStarSpinnerPanel = new JPanel(new SpringLayout());
        rhoStarSpinnerPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(subBorderColor, 1), "\u03C1* (cgs)", TitledBorder.CENTER, TitledBorder.TOP, p11, Color.darkGray));
        rhoStarSpinner[c] = new JSpinner(new SpinnerNumberModel(Double.valueOf(IJU.getRhoStarFromTeff(teff[c])), Double.valueOf(IJU.rhoStar[0]), Double.valueOf(IJU.rhoStar[IJU.rhoStar.length - 1]), Double.valueOf(rhoStarStep)));
        rhoStarSpinner[c].setFont(p11);
        rhoStarSpinner[c].setEditor(new JSpinner.NumberEditor(rhoStarSpinner[c], "#0.000"));
        rhoStarSpinner[c].setPreferredSize(stellarSpinnerSize);
        rhoStarSpinner[c].setMaximumSize(stellarSpinnerSize);
        rhoStarSpinner[c].setComponentPopupMenu(rhoStarStepPopup);
        rhoStarSpinner[c].setToolTipText("<html>Enter density of the host star in g/cm^3 and press enter to calculate<br>" + "the other stellar parameters and the planet radius.<br>" + "This value is required only if a planetary radius estimation<br>" + "is to be calculated from the light curve fit.<br>" + "---------------------------------------------<br>" + "Right click to set spinner stepsize</html>");
        rhoStarSpinner[c].addChangeListener(ev -> {
            if (rhoStarSpinner[c].isEnabled()) {
                teffSpinner[c].setValue(IJU.getTeffFromRhoStar((Double) rhoStarSpinner[c].getValue()));
            }
        });
        rhoStarSpinner[c].addMouseWheelListener(e -> {
            double newValue = (Double) rhoStarSpinner[c].getValue() - e.getWheelRotation() * rhoStarStep;
            if (rhoStarSpinner[c].isEnabled()) {
                if (newValue < IJU.rhoStar[0]) { rhoStarSpinner[c].setValue(IJU.rhoStar[0]); } else {
                    rhoStarSpinner[c].setValue(Math.min(newValue, IJU.rhoStar[IJU.rhoStar.length - 1]));
                }
            }
        });
        rhoStarSpinnerPanel.add(rhoStarSpinner[c]);

        SpringUtil.makeCompactGrid(rhoStarSpinnerPanel, 1, rhoStarSpinnerPanel.getComponentCount(), 2, 0, 0, 0);
        stellarParmetersPanel.add(rhoStarSpinnerPanel);


        SpringUtil.makeCompactGrid(stellarParmetersPanel, 1, stellarParmetersPanel.getComponentCount(), 2, 0, 0, 0);
        specifiedParmetersPanel.add(stellarParmetersPanel);


        JLabel periodDummyLabel3 = new JLabel("");
        specifiedParmetersPanel.add(periodDummyLabel3);


        SpringUtil.makeCompactGrid(specifiedParmetersPanel, nlines, specifiedParmetersPanel.getComponentCount() / nlines, 2, 2, 0, 0);
        fitPanel[c].add(specifiedParmetersPanel);


        JPanel fittedParametersPanel = new JPanel(new SpringLayout());
        fittedParametersPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(mainBorderColor, 1), "Transit Parameters", TitledBorder.LEFT, TitledBorder.TOP, b12, Color.darkGray));

        JPanel fittedParametersPanel1 = new JPanel(new SpringLayout());
        JPanel useTransitFitCBPanel = new JPanel(new SpringLayout());
        useTransitFitCBPanel.setPreferredSize(labelSize);
        useTransitFitCB[c] = new JCheckBox("Enable Transit Fit", useTransitFit[c]);
        useTransitFitCB[c].setPreferredSize(labelSize);
        useTransitFitCB[c].setMaximumSize(labelSize);
        useTransitFitCB[c].setFont(p11);
        useTransitFitCB[c].setToolTipText("Enable to include a transit model fit to the data");
        useTransitFitCB[c].addItemListener(e -> {
            if (e.getStateChange() == ItemEvent.DESELECTED) {
                useTransitFit[c] = false;
            } else if (e.getStateChange() == ItemEvent.SELECTED) {
                useTransitFit[c] = true;
            }
            enableTransitComponents(c);
            if (autoUpdateFit[c]) updatePlot(updateOneFit(c));
        });
        useTransitFitCBPanel.add(useTransitFitCB[c]);
        SpringUtil.makeCompactGrid(useTransitFitCBPanel, 1, useTransitFitCBPanel.getComponentCount(), 0, 0, 0, 0);
        fittedParametersPanel1.add(useTransitFitCBPanel);

//            JLabel fittedParametersPanel1Label0 = new JLabel("");
//            fittedParametersPanel1Label0.setPreferredSize(new Dimension(25, 25));
//            fittedParametersPanel1Label0.setMaximumSize(new Dimension(25, 25));
//            fittedParametersPanel1.add(fittedParametersPanel1Label0);

        JPanel extractPriorsPanel = new JPanel(new SpringLayout());
        extractPriorsPanel.setBorder(BorderFactory.createLineBorder(subBorderColor, 1));

        autoUpdatePriorsCB[c] = new JCheckBox("Auto Update Priors", autoUpdatePriors[c]);
        autoUpdatePriorsCB[c].setPreferredSize(labelSize);
        autoUpdatePriorsCB[c].setMaximumSize(labelSize);
        autoUpdatePriorsCB[c].setEnabled(useTransitFit[c]);
        autoUpdatePriorsCB[c].setFont(p11);
        autoUpdatePriorsCB[c].setToolTipText("Enable to automatically update the extracted prior center values when settings change.");
        autoUpdatePriorsCB[c].addItemListener(e -> {
            if (e.getStateChange() == ItemEvent.DESELECTED) {
                autoUpdatePriors[c] = false;
            } else if (e.getStateChange() == ItemEvent.SELECTED) {
                autoUpdatePriors[c] = true;
            }
            enableTransitComponents(c);
            if (autoUpdatePriors[c] && (autoUpdatePrior[c][0] || autoUpdatePrior[c][1] || autoUpdatePrior[c][2] || autoUpdatePrior[c][3] || autoUpdatePrior[c][4])) {
                updatePlot(updateOneFit(c));
            }
        });
        extractPriorsPanel.add(autoUpdatePriorsCB[c]);

        extractPriorsButton[c] = new JButton("Extract Prior Center Values From Light Curve, Orbit, and Fit Markers");
        extractPriorsButton[c].setToolTipText("<html> Click to estimate baseline flux, transit depth, a/R<sub>*</sub> and T<sub>C</sub><br>" + "from the light curve, orbit parameters, and the mid-point of the left and right fit markers.</html>");
        extractPriorsButton[c].setFont(p11);
        extractPriorsButton[c].setPreferredSize(new Dimension(400, 25));
        extractPriorsButton[c].setMaximumSize(new Dimension(400, 25));
        extractPriorsButton[c].setEnabled(useTransitFit[c]);
        extractPriorsButton[c].addActionListener(e -> {
            if (plotY[c]) {
                updatePriorCenters(c);
                if (autoUpdateFit[c]) updatePlot(updateOneFit(c));
            } else {
                IJ.beep();
                IJ.showMessage("The 'Plot' option for this curve must be enabled to extract prior center values");
            }
        });
        extractPriorsPanel.add(extractPriorsButton[c]);

        SpringUtil.makeCompactGrid(extractPriorsPanel, 1, extractPriorsPanel.getComponentCount(), 2, 2, 2, 2);
        fittedParametersPanel1.add(extractPriorsPanel);

        JLabel fittedParametersPanel1Label1 = new JLabel("");
        fittedParametersPanel1.add(fittedParametersPanel1Label1);

        SpringUtil.makeCompactGrid(fittedParametersPanel1, 1, fittedParametersPanel1.getComponentCount(), 2, 2, 2, 2);
        fittedParametersPanel.add(fittedParametersPanel1);

        nlines = 1;

        JPanel fittedParametersPanel2 = new JPanel(new SpringLayout());

        JLabel fittedColumnNameLabel1 = new JLabel("Parameter");
        fittedColumnNameLabel1.setFont(b11);
        fittedColumnNameLabel1.setHorizontalAlignment(JLabel.LEFT);
        fittedColumnNameLabel1.setPreferredSize(spinnerSize);
        fittedColumnNameLabel1.setMaximumSize(spinnerSize);
        fittedParametersPanel2.add(fittedColumnNameLabel1);

        JLabel fittedColumnNameLabel5a = new JLabel("Best Fit");
        fittedColumnNameLabel5a.setFont(b11);
        fittedColumnNameLabel5a.setHorizontalAlignment(JLabel.CENTER);
        fittedColumnNameLabel5a.setPreferredSize(bestFitSize);
        fittedColumnNameLabel5a.setMaximumSize(bestFitSize);
        fittedParametersPanel2.add(fittedColumnNameLabel5a);

        JLabel fittedColumnNameLabel5 = new JLabel("");
        fittedColumnNameLabel5.setFont(b11);
        fittedColumnNameLabel5.setHorizontalAlignment(JLabel.CENTER);
        fittedColumnNameLabel5.setPreferredSize(spacerSize);
        fittedColumnNameLabel5.setMaximumSize(spacerSize);
        fittedParametersPanel2.add(fittedColumnNameLabel5);

        JLabel fittedColumnNameLabel2a = new JLabel("Lock");
        fittedColumnNameLabel2a.setFont(b11);
        fittedColumnNameLabel2a.setHorizontalAlignment(JLabel.CENTER);
        fittedColumnNameLabel2a.setToolTipText("Lock transit model parameter to prior center value.");
        fittedColumnNameLabel2a.setPreferredSize(checkBoxSize);
        fittedColumnNameLabel2a.setMaximumSize(checkBoxSize);
        fittedParametersPanel2.add(fittedColumnNameLabel2a);

        JLabel fittedColumnNameLabel2b = new JLabel("Prior Center");
        fittedColumnNameLabel2b.setFont(b11);
        fittedColumnNameLabel2b.setHorizontalAlignment(JLabel.CENTER);
        fittedColumnNameLabel2b.setPreferredSize(spinnerSize);
        fittedColumnNameLabel2b.setMaximumSize(spinnerSize);
        fittedColumnNameLabel2b.setToolTipText("Enter the parameter's starting value for the fit (if enabled).");
        fittedParametersPanel2.add(fittedColumnNameLabel2b);

        JLabel fittedColumnNameLabel2c = new JLabel("");
        fittedColumnNameLabel2c.setFont(b11);
        fittedColumnNameLabel2c.setHorizontalAlignment(JLabel.CENTER);
        fittedColumnNameLabel2c.setPreferredSize(spacerSize);
        fittedColumnNameLabel2c.setMaximumSize(spacerSize);
        fittedParametersPanel2.add(fittedColumnNameLabel2c);

        JLabel fittedColumnNameLabel3a = new JLabel("Use");
        fittedColumnNameLabel3a.setFont(b11);
        fittedColumnNameLabel3a.setHorizontalAlignment(JLabel.CENTER);
        fittedColumnNameLabel3a.setToolTipText("The fitting range allowed from the prior center start value (if enabled).");
        fittedColumnNameLabel3a.setPreferredSize(checkBoxSize);
        fittedColumnNameLabel3a.setMaximumSize(checkBoxSize);
        fittedParametersPanel2.add(fittedColumnNameLabel3a);

        JLabel fittedColumnNameLabel3b = new JLabel("Prior Width");
        fittedColumnNameLabel3b.setFont(b11);
        fittedColumnNameLabel3b.setHorizontalAlignment(JLabel.CENTER);
        fittedColumnNameLabel3b.setPreferredSize(spinnerSize);
        fittedParametersPanel2.add(fittedColumnNameLabel3b);

        JLabel fittedColumnNameLabel4 = new JLabel("");
        fittedColumnNameLabel4.setFont(b11);
        fittedColumnNameLabel4.setHorizontalAlignment(JLabel.CENTER);
        fittedColumnNameLabel4.setPreferredSize(spacerSize);
        fittedColumnNameLabel4.setMaximumSize(spacerSize);
        fittedParametersPanel2.add(fittedColumnNameLabel4);

        JLabel fittedColumnNameLabel4a = new JLabel("Cust");
        fittedColumnNameLabel4a.setFont(b11);
        fittedColumnNameLabel4a.setHorizontalAlignment(JLabel.CENTER);
        fittedColumnNameLabel4a.setToolTipText("<html>Enable to enter a custom minimization stepsize.<br>" + "Disable for automatic determination of minimizer stepsize.<br>" + "The minimizer is used to find the best light curve fit.</html>");
        fittedColumnNameLabel4a.setPreferredSize(checkBoxSize);
        fittedColumnNameLabel4a.setMaximumSize(checkBoxSize);
        fittedParametersPanel2.add(fittedColumnNameLabel4a);

        JLabel fittedColumnNameLabel4b = new JLabel("StepSize");
        fittedColumnNameLabel4b.setFont(b11);
        fittedColumnNameLabel4b.setHorizontalAlignment(JLabel.CENTER);
        fittedColumnNameLabel4b.setToolTipText("<html>Enable to enter a custom minimization stepsize.<br>" + "Disable for automatic determination of minimizer stepsize.<br>" + "The minimizer is used to find the best light curve fit.</html>");
        fittedColumnNameLabel4b.setPreferredSize(spinnerSize);
        fittedColumnNameLabel4b.setMaximumSize(spinnerSize);
        fittedParametersPanel2.add(fittedColumnNameLabel4b);

        JLabel fittedColumnNameLabel6 = new JLabel("");
        fittedParametersPanel2.add(fittedColumnNameLabel6);


        String rowName = "<html>Baseline Flux (Raw)</html>";
        String rowNameToolTipText = "<html>Baseline flux before detrending, normalization, scaling, shifting,<br>" + "or magnitude conversion (see rel_flux_T" + c + " table column)</html>";
        createFittedParametersRow(fittedParametersPanel2, c, 0, rowName, rowNameToolTipText, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY);
        nlines++;

        rowName = "<html>(R<sub>p</sub> / R<sub>*</sub>)<sup>2</sup></html>";
        rowNameToolTipText = "<html>Transit depth</html>";
        createFittedParametersRow(fittedParametersPanel2, c, 1, rowName, rowNameToolTipText, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY);
        nlines++;

        rowName = "<html>a / R<sub>*</sub></html>";
        rowNameToolTipText = "<html>Semi-major axis of the planet's orbit in units of stellar radii</html>";
        createFittedParametersRow(fittedParametersPanel2, c, 2, rowName, rowNameToolTipText, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY);
        nlines++;

        rowName = "<html>T<sub>C</sub></html>";
        rowNameToolTipText = "<html>Transit center time</html>";
        createFittedParametersRow(fittedParametersPanel2, c, 3, rowName, rowNameToolTipText, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY);
        nlines++;

        rowName = "<html>Inclination (deg)</html>";
        rowNameToolTipText = "<html>Inclination of planet's orbit relative to observer's line of site</html>";
        createFittedParametersRow(fittedParametersPanel2, c, 4, rowName, rowNameToolTipText, 0, 90);
        nlines++;

        rowName = "<html>Linear LD u1</html>";
        rowNameToolTipText = "<html>Linear limb darkening coefficient (range 0.0 to 1.0).<br>" + "For ground-based observations, it maybe necessary to lock<br>" + "one or both of the coefficients to theoretical values.<br>" + "With no prior info, try u1=0.3 (unlock), and u2=0.3 (lock).</html>";
        createFittedParametersRow(fittedParametersPanel2, c, 5, rowName, rowNameToolTipText, 0, 1);
        nlines++;

        rowName = "<html>Quad LD u2</html>";
        rowNameToolTipText = "<html>Quadratic limb darkening coefficient (range -1.0 to 1.0).<br>" + "For ground-based observations, it maybe necessary to lock<br>" + "one or both of the coefficients to theoretical values.<br>" + "With no prior info, try u1=0.3 (unlock), and u2=0.3 (lock).</html>";
        createFittedParametersRow(fittedParametersPanel2, c, 6, rowName, rowNameToolTipText, -1, 1);
        nlines++;

        SpringUtil.makeCompactGrid(fittedParametersPanel2, nlines, fittedParametersPanel2.getComponentCount() / nlines, 2, 2, 2, 2);
        fittedParametersPanel.add(fittedParametersPanel2);

        JPanel fittedParametersPanel3 = new JPanel(new SpringLayout());

        JLabel calcParmsLabel = new JLabel("Calculated from model");
        calcParmsLabel.setFont(p12);
        calcParmsLabel.setPreferredSize(labelSize);
        calcParmsLabel.setMaximumSize(labelSize);
        fittedParametersPanel3.add(calcParmsLabel);

        transitDepthPanel[c] = new JPanel(new GridBagLayout());
        transitDepthPanel[c].setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(subBorderColor, 1), "Depth (ppt)", TitledBorder.CENTER, TitledBorder.TOP, p11, Color.darkGray));

        transitDepthLabel[c] = new JTextField("");
        transitDepthLabel[c].setToolTipText("<html>Depth defined as transit model flux deficit at mid-transit (Tc) in parts per thousand (ppt).<br>" + "Green Border: fit converged<br>" + "Red Border: fit did not converge<br>" + "Gray Border: no fit in this session</html>");
        transitDepthLabel[c].setFont(p11);
        transitDepthLabel[c].setHorizontalAlignment(JLabel.CENTER);
        transitDepthLabel[c].setBorder(grayBorder);
        transitDepthLabel[c].setPreferredSize(statSize);
        transitDepthLabel[c].setMaximumSize(statSize);
        transitDepthLabel[c].setEnabled(useTransitFit[c]);
        transitDepthLabel[c].setEditable(false);
        transitDepthPanel[c].add(transitDepthLabel[c], new GridBagConstraints());
        fittedParametersPanel3.add(transitDepthPanel[c]);

        JPanel bpPanel = new JPanel(new SpringLayout());
        bpPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(subBorderColor, 1), "b", TitledBorder.CENTER, TitledBorder.TOP, p11, Color.darkGray));

        bpSpinner[c] = new JSpinner(new SpinnerNumberModel(Double.valueOf(bp[c] >= 0.0 && bp[c] <= 2.0 ? bp[c] : 0.0), Double.valueOf(0.0), Double.valueOf(2.0), Double.valueOf(0.01)));
        bpSpinner[c].setEnabled(bpLock[c] && useTransitFit[c]);
        bpSpinner[c].setFont(p11);
        bpSpinner[c].setEditor(new JSpinner.NumberEditor(bpSpinner[c], oneDotThreeFormat));
        bpSpinner[c].setBorder(bpLock[c] ? lockBorder : grayBorder);
        if (!bpLock[c]) {
            bpSpinner[c].setValue(Math.cos((Math.PI/180.0)*priorCenter[c][4])/priorCenter[c][2]);
        }
        bpSpinner[c].setPreferredSize(bpSize);
        bpSpinner[c].setMaximumSize(bpSize);
        //bpSpinner[c].setComponentPopupMenu(priorCenterStepPopup[row]);
        bpSpinner[c].setToolTipText("\"<html>Impact parameter of the transit model.<br>" + "For circular orbits, b = (a/R*)cos(inclination)<br>" + "Green Border: fit converged<br>" + "Red Border: fit did not converge<br>" + "Gray Border: no fit in this session</html>");
        bpSpinner[c].addChangeListener(ev -> {
            if (bpLock[c]) {
                bp[c] = (Double) bpSpinner[c].getValue();
                double inclination = (180.0/Math.PI)*Math.acos(bp[c]/bestFit[c][2]);
                priorCenterSpinner[c][4].setValue(Double.valueOf(inclination));
                priorCenter[c][4] = inclination;
                if (autoUpdateFit[c]) updatePlot(updateOneFit(c));
            }
        });
        bpSpinner[c].addMouseWheelListener(e -> {
            double newValue = (Double) bpSpinner[c].getValue() - e.getWheelRotation() * 0.01;
            if (newValue < 0.0) newValue = 0.0;
            if (newValue > 2.0) newValue = 2.0;
            if (bpSpinner[c].isEnabled()) {
                bpSpinner[c].setValue(newValue);
            }
        });
        bpPanel.add(bpSpinner[c]);

        bpLockCB[c] = new JCheckBox("", bpLock[c]);
        bpLockCB[c].setEnabled(bpLock[c]);
        bpLockCB[c].setFont(p12);
        bpLockCB[c].setToolTipText("Lock impact parameter of transit.");
        bpLockCB[c].addItemListener(e -> {
            bpLock[c] = (e.getStateChange() == ItemEvent.SELECTED);
            bpSpinner[c].setEnabled(bpLock[c] && useTransitFit[c]);
            lockToCenterCB[c][4].setEnabled(!bpLock[c] && useTransitFit[c]);
            lockToCenter[c][4] = bpLock[c] || lockToCenterCB[c][4].isSelected();
            priorCenterSpinner[c][4].setEnabled(!bpLock[c] && (lockToCenterCB[c][4].isSelected() || !autoUpdatePrior[c][4]) && useTransitFit[c]);
            if (bpLock[c]) {
                double inclination = (180.0/Math.PI)*Math.acos(bp[c]/bestFit[c][2]);
                //double inclination = (180/Math.PI)*Math.acos((1.0 + (forceCircularOrbit[c] ? 0.0 : eccentricity[c]) * Math.sin(forceCircularOrbit[c] ? 0.0 : omega[c])) / (bestFit[c][2] * (1.0 - (forceCircularOrbit[c] ? 0.0 : eccentricity[c] * eccentricity[c]))));
                priorCenterSpinner[c][4].setValue(inclination);
                priorCenter[c][4] = inclination;
                if (autoUpdateFit[c]) updatePlot(updateOneFit(c));
            }
        });
        bpPanel.add(bpLockCB[c]);

        SpringUtil.makeCompactGrid(bpPanel, 1, bpPanel.getComponentCount(), 0, 0, 0, 0);
        fittedParametersPanel3.add(bpPanel);


        JPanel t14Panel = new JPanel(new SpringLayout());
        t14Panel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(subBorderColor, 1), "t14 (d)", TitledBorder.CENTER, TitledBorder.TOP, p11, Color.darkGray));

        t14Label[c] = new JTextField("");
        t14Label[c].setToolTipText("<html>The duration of the transit (days).<br>" + "This is the full duration of the transit from time<br>" + "of 1<sup>st</sup> contact to time of 4<sup>th</sup> contact (t<sub>14</sub>).<br>" + "Green Border: fit converged<br>" + "Red Border: fit did not converge<br>" + "Gray Border: no fit in this session</html>");
        t14Label[c].setFont(p11);
        t14Label[c].setHorizontalAlignment(JLabel.CENTER);
        t14Label[c].setBorder(grayBorder);
        t14Label[c].setPreferredSize(statSize);
        t14Label[c].setMaximumSize(statSize);
        t14Label[c].setEnabled(useTransitFit[c]);
        t14Label[c].setEditable(false);
        t14Panel.add(t14Label[c]);
        SpringUtil.makeCompactGrid(t14Panel, 1, t14Panel.getComponentCount(), 0, 0, 0, 0);
        fittedParametersPanel3.add(t14Panel);

        JPanel t14HoursPanel = new JPanel(new SpringLayout());
        t14HoursPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(subBorderColor, 1), "t14 (hms)", TitledBorder.CENTER, TitledBorder.TOP, p11, Color.darkGray));

        t14HoursLabel[c] = new JTextField("");
        t14HoursLabel[c].setToolTipText("<html>The duration of the transit (hours:minutes:seconds).<br>" + "This is the full duration of the transit from time<br>" + "of 1<sup>st</sup> contact to time of 4<sup>th</sup> contact (t<sub>14</sub>).<br>" + "Green Border: fit converged<br>" + "Red Border: fit did not converge<br>" + "Gray Border: no fit in this session</html>");
        t14HoursLabel[c].setFont(p11);
        t14HoursLabel[c].setHorizontalAlignment(JLabel.CENTER);
        t14HoursLabel[c].setBorder(grayBorder);
        t14HoursLabel[c].setPreferredSize(statSize);
        t14HoursLabel[c].setMaximumSize(statSize);
        t14HoursLabel[c].setEnabled(useTransitFit[c]);
        t14HoursLabel[c].setEditable(false);
        t14HoursPanel.add(t14HoursLabel[c]);
        SpringUtil.makeCompactGrid(t14HoursPanel, 1, t14HoursPanel.getComponentCount(), 0, 0, 0, 0);
        fittedParametersPanel3.add(t14HoursPanel);

        JPanel t23Panel = new JPanel(new SpringLayout());
        t23Panel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(subBorderColor, 1), "t23 (d)", TitledBorder.CENTER, TitledBorder.TOP, p11, Color.darkGray));

        t23Label[c] = new JTextField("");
        t23Label[c].setToolTipText("<html>The in-transit duration of the transit (days).<br>" + "This is the duration of the transit from time<br>" + "of 2<sup>nd</sup> contact to time of 3<sup>rd</sup> contact (t<sub>23</sub>).<br>" + "Green Border: fit converged<br>" + "Red Border: fit did not converge<br>" + "Gray Border: no fit in this session</html>");
        t23Label[c].setFont(p11);
        t23Label[c].setHorizontalAlignment(JLabel.CENTER);
        t23Label[c].setBorder(grayBorder);
        t23Label[c].setPreferredSize(statSize);
        t23Label[c].setMaximumSize(statSize);
        t23Label[c].setEnabled(useTransitFit[c]);
        t23Label[c].setEditable(false);
        t23Panel.add(t23Label[c]);
        SpringUtil.makeCompactGrid(t23Panel, 1, t23Panel.getComponentCount(), 0, 0, 0, 0);
        fittedParametersPanel3.add(t23Panel);

        JPanel tauPanel = new JPanel(new SpringLayout());
        tauPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(subBorderColor, 1), "tau (d)", TitledBorder.CENTER, TitledBorder.TOP, p11, Color.darkGray));

        tauLabel[c] = new JTextField("");
        tauLabel[c].setToolTipText("<html>The transit ingress (and egress) duration (days).<br>" + "This is the duration of the transit ingress (and egress) from time<br>" + "of 1<sup>st</sup> contact to time of 2<sup>nd</sup> contact (t<sub>12</sub>) or in the <br>" + "case of egress t<sub>34</sub>. t<sub>12</sub> and t<sub>34</sub> are assumed to be equal.<br>" + "Green Border: fit converged<br>" + "Red Border: fit did not converge<br>" + "Gray Border: no fit in this session</html>");
        tauLabel[c].setFont(p11);
        tauLabel[c].setHorizontalAlignment(JLabel.CENTER);
        tauLabel[c].setBorder(grayBorder);
        tauLabel[c].setPreferredSize(statSize);
        tauLabel[c].setMaximumSize(statSize);
        tauLabel[c].setEnabled(useTransitFit[c]);
        tauLabel[c].setEditable(false);
        tauPanel.add(tauLabel[c]);
        SpringUtil.makeCompactGrid(tauPanel, 1, tauPanel.getComponentCount(), 0, 0, 0, 0);
        fittedParametersPanel3.add(tauPanel);

        JPanel densityPanel = new JPanel(new SpringLayout());
        densityPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(subBorderColor, 1), "\u03C1* (cgs)", TitledBorder.CENTER, TitledBorder.TOP, p11, Color.darkGray));

        stellarDensityLabel[c] = new JTextField("");
        stellarDensityLabel[c].setToolTipText("<html>Host star density as derived from the light curve shape (g/cm^3).<br>" + "Density calculations are based on Seager & Mallen-Ornelas 2003 eqn 9.<br>" + "Green Border: fit converged<br>" + "Red Border: fit did not converge<br>" + "Gray Border: no fit in this session</html>");
        stellarDensityLabel[c].setFont(p11);
        stellarDensityLabel[c].setHorizontalAlignment(JLabel.CENTER);
        stellarDensityLabel[c].setBorder(grayBorder);
        stellarDensityLabel[c].setPreferredSize(statSize);
        stellarDensityLabel[c].setMaximumSize(statSize);
        stellarDensityLabel[c].setEnabled(useTransitFit[c]);
        stellarDensityLabel[c].setEditable(false);
        densityPanel.add(stellarDensityLabel[c]);
        SpringUtil.makeCompactGrid(densityPanel, 1, densityPanel.getComponentCount(), 0, 0, 0, 0);
        fittedParametersPanel3.add(densityPanel);

        JPanel planetRadiusPanel = new JPanel(new SpringLayout());
        planetRadiusPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(Color.pink, 1), "Rp (Rjup)", TitledBorder.CENTER, TitledBorder.TOP, p11, Color.darkGray));

        planetRadiusLabel[c] = new JTextField("");
        planetRadiusLabel[c].setToolTipText("<html>Planet radius in Jupiter Radii estimated from transit depth and host star radius as entered or calculated above. <br>" + "If available, enter the host star radius directly for the best estimate of planet radius.<br>" + "Otherwise, planet radius estimation assumes the host star is on the main sequence.<br>" + "Green Border: fit converged<br>" + "Red Border: fit did not converge<br>" + "Gray Border: no fit in this session</html>");
        planetRadiusLabel[c].setFont(p11);
        planetRadiusLabel[c].setHorizontalAlignment(JLabel.CENTER);
        planetRadiusLabel[c].setBorder(grayBorder);
        planetRadiusLabel[c].setPreferredSize(statSize);
        planetRadiusLabel[c].setMaximumSize(statSize);
        planetRadiusLabel[c].setEnabled(useTransitFit[c]);
        planetRadiusLabel[c].setEditable(false);
        planetRadiusPanel.add(planetRadiusLabel[c]);
        SpringUtil.makeCompactGrid(planetRadiusPanel, 1, planetRadiusPanel.getComponentCount(), 0, 0, 0, 0);
        fittedParametersPanel3.add(planetRadiusPanel);

        JLabel calcParmsDummyLabel1 = new JLabel("");
        fittedParametersPanel3.add(calcParmsDummyLabel1);

        SpringUtil.makeCompactGrid(fittedParametersPanel3, 1, fittedParametersPanel3.getComponentCount(), 0, 0, 0, 0);
        fittedParametersPanel.add(fittedParametersPanel3);

        SpringUtil.makeCompactGrid(fittedParametersPanel, fittedParametersPanel.getComponentCount(), 1, 2, 2, 0, 0);
        fitPanel[c].add(fittedParametersPanel);


        JPanel detrendParametersPanel = new JPanel(new SpringLayout());
        detrendParametersPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(mainBorderColor, 1), "Detrend Parameters", TitledBorder.LEFT, TitledBorder.TOP, b12, Color.darkGray));

        nlines = 1;

//            JPanel detrendVariableNamePanel = new JPanel(new SpringLayout());
//            detrendVariableNamePanel.setPreferredSize(labelSize);
//            detrendVariableNamePanel.setMaximumSize(labelSize);

        JLabel detrendEnableLabel = new JLabel("Use");
        detrendEnableLabel.setFont(b11);
        detrendEnableLabel.setHorizontalAlignment(JLabel.LEFT);
        detrendEnableLabel.setPreferredSize(checkBoxSize);
        detrendEnableLabel.setMaximumSize(checkBoxSize);
        detrendParametersPanel.add(detrendEnableLabel);

        JLabel detrendVariableNameLabel = new JLabel("Parameter");
        detrendVariableNameLabel.setFont(b11);
        detrendVariableNameLabel.setHorizontalAlignment(JLabel.CENTER);
        detrendVariableNameLabel.setPreferredSize(choiceBoxSize);
        detrendVariableNameLabel.setMaximumSize(choiceBoxSize);
        detrendParametersPanel.add(detrendVariableNameLabel);

//            SpringUtil.makeCompactGrid (detrendVariableNamePanel, 1, detrendVariableNamePanel.getComponentCount(), 0,0,0,0);
//            detrendParametersPanel.add(detrendVariableNamePanel);

        JLabel bestFitLabel = new JLabel("Best Fit");
        bestFitLabel.setFont(b11);
        bestFitLabel.setHorizontalAlignment(JLabel.CENTER);
        bestFitLabel.setPreferredSize(bestFitSize);
        bestFitLabel.setMaximumSize(bestFitSize);
        detrendParametersPanel.add(bestFitLabel);

        JLabel detrendDummyLabel1 = new JLabel("");
        detrendDummyLabel1.setFont(b11);
        detrendDummyLabel1.setHorizontalAlignment(JLabel.CENTER);
        detrendDummyLabel1.setPreferredSize(spacerSize);
        detrendDummyLabel1.setMaximumSize(spacerSize);
        detrendParametersPanel.add(detrendDummyLabel1);

        JLabel detrendLockLabel = new JLabel("Lock");
        detrendLockLabel.setFont(b11);
        detrendLockLabel.setPreferredSize(checkBoxSize);
        detrendLockLabel.setMaximumSize(checkBoxSize);
        detrendLockLabel.setHorizontalAlignment(JLabel.CENTER);
        detrendLockLabel.setToolTipText("Lock detrend parameter to prior center value.");
//            fittedColumnNameLabel2a.setPreferredSize(spinnerSize);
        detrendParametersPanel.add(detrendLockLabel);

        JLabel detrendCenterLabel = new JLabel("Prior Center");
        detrendCenterLabel.setFont(b11);
        detrendCenterLabel.setHorizontalAlignment(JLabel.CENTER);
        detrendCenterLabel.setPreferredSize(spinnerSize);
        detrendCenterLabel.setMaximumSize(spinnerSize);
        detrendCenterLabel.setToolTipText("Enter the detrend parameter's starting value for the fit.");
        detrendParametersPanel.add(detrendCenterLabel);

        JLabel detrendDummyLabel2 = new JLabel("");
        detrendDummyLabel2.setFont(b11);
        detrendDummyLabel2.setHorizontalAlignment(JLabel.CENTER);
        detrendDummyLabel2.setPreferredSize(spacerSize);
        detrendDummyLabel2.setMaximumSize(spacerSize);
        detrendParametersPanel.add(detrendDummyLabel2);

        JLabel detrendUseWidthLabel = new JLabel("Use");
        detrendUseWidthLabel.setFont(b11);
        detrendUseWidthLabel.setPreferredSize(checkBoxSize);
        detrendUseWidthLabel.setMaximumSize(checkBoxSize);
        detrendUseWidthLabel.setHorizontalAlignment(JLabel.CENTER);
        detrendUseWidthLabel.setToolTipText("Enable to restrict the fitting range allowed from the prior center start value).");
//            fittedColumnNameLabel3a.setPreferredSize(spinnerSize);
        detrendParametersPanel.add(detrendUseWidthLabel);

        JLabel detrendWidthLabel = new JLabel("Prior Width");
        detrendWidthLabel.setFont(b11);
        detrendWidthLabel.setHorizontalAlignment(JLabel.CENTER);
        detrendWidthLabel.setPreferredSize(spinnerSize);
        detrendParametersPanel.add(detrendWidthLabel);

        JLabel detrendDummyLabel3 = new JLabel("");
        detrendDummyLabel3.setFont(b11);
        detrendDummyLabel3.setHorizontalAlignment(JLabel.CENTER);
        detrendDummyLabel3.setPreferredSize(spacerSize);
        detrendDummyLabel3.setMaximumSize(spacerSize);
        detrendParametersPanel.add(detrendDummyLabel3);

        JLabel detrendUseCustomStepLabel = new JLabel("Cust");
        detrendUseCustomStepLabel.setFont(b11);
        detrendUseCustomStepLabel.setPreferredSize(checkBoxSize);
        detrendUseCustomStepLabel.setMaximumSize(checkBoxSize);
        detrendUseCustomStepLabel.setHorizontalAlignment(JLabel.CENTER);
        detrendUseCustomStepLabel.setToolTipText("<html>Enable to enter a custom minimization stepsize.<br>" + "Disable for automatic determination of minimizer stepsize.<br>" + "The minimizer is used to find the best light curve fit.</html>");
//            fittedColumnNameLabel3a.setPreferredSize(spinnerSize);
        detrendParametersPanel.add(detrendUseCustomStepLabel);

        JLabel detrendStepSizeLabel = new JLabel("StepSize");
        detrendStepSizeLabel.setFont(b11);
        detrendStepSizeLabel.setHorizontalAlignment(JLabel.CENTER);
        detrendStepSizeLabel.setToolTipText("<html>Enable to enter a custom minimization stepsize.<br>" + "Disable for automatic determination of minimizer stepsize.<br>" + "The minimizer is used to find the best light curve fit.</html>");
        detrendStepSizeLabel.setPreferredSize(spinnerSize);
        detrendStepSizeLabel.setMaximumSize(spinnerSize);
        detrendParametersPanel.add(detrendStepSizeLabel);

        JLabel detrendDummyLabel4 = new JLabel("");
        detrendParametersPanel.add(detrendDummyLabel4);

        for (int d = 7; d < maxDetrendVars + 7; d++) {
            rowName = null;
            rowNameToolTipText = "<html>Detrend variable " + (d - 6) + "</html>";
            createFittedParametersRow(detrendParametersPanel, c, d, rowName, rowNameToolTipText, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY);
            nlines++;
        }

        SpringUtil.makeCompactGrid(detrendParametersPanel, nlines, detrendParametersPanel.getComponentCount() / nlines, 2, 2, 2, 2);
        fitPanel[c].add(detrendParametersPanel);


        JPanel fitStatisticsPanel = new JPanel(new SpringLayout());
        fitStatisticsPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(mainBorderColor, 1), "Fit Statistics", TitledBorder.LEFT, TitledBorder.TOP, b12, Color.darkGray));

        JLabel statisticsLabel = new JLabel("Fit Statistics");
        statisticsLabel.setFont(p12);
        statisticsLabel.setPreferredSize(labelSize);
        statisticsLabel.setMaximumSize(labelSize);
        fitStatisticsPanel.add(statisticsLabel);


        sigmaPanel[c] = new JPanel(new SpringLayout());
        sigmaPanel[c].setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(subBorderColor, 1), "RMS", TitledBorder.CENTER, TitledBorder.TOP, p11, Color.darkGray));

        sigmaLabel[c] = new JTextField("");
        sigmaLabel[c].setToolTipText("<html>Standard deviation of model residuals.<br>" + "Green Border: fit converged<br>" + "Red Border: fit did not converge<br>" + "Gray Border: no fit in this session</html>");
        sigmaLabel[c].setFont(p11);
        sigmaLabel[c].setHorizontalAlignment(JLabel.CENTER);
        sigmaLabel[c].setBorder(grayBorder);
        sigmaLabel[c].setPreferredSize(bestFitSize);
        sigmaLabel[c].setMaximumSize(bestFitSize);
        sigmaLabel[c].setEditable(false);
        sigmaPanel[c].add(sigmaLabel[c]);
        SpringUtil.makeCompactGrid(sigmaPanel[c], 1, sigmaPanel[c].getComponentCount(), 0, 0, 0, 0);
        fitStatisticsPanel.add(sigmaPanel[c]);

        JPanel chi2dofPanel = new JPanel(new SpringLayout());
        chi2dofPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(subBorderColor, 1), "chi\u00B2/dof", TitledBorder.CENTER, TitledBorder.TOP, p11, Color.darkGray));

        chi2dofLabel[c] = new JTextField("");
        chi2dofLabel[c].setToolTipText("<html>&chi;<sup>2</sup> per degree of freedom for the fit.<br>" + "Green Border: fit converged<br>" + "Red Border: fit did not converge<br>" + "Gray Border: no fit in this session</html>");
        chi2dofLabel[c].setFont(p11);
        chi2dofLabel[c].setHorizontalAlignment(JLabel.CENTER);
        chi2dofLabel[c].setBorder(grayBorder);
        chi2dofLabel[c].setPreferredSize(bestFitSize);
        chi2dofLabel[c].setMaximumSize(bestFitSize);
        chi2dofLabel[c].setEditable(false);
        chi2dofPanel.add(chi2dofLabel[c]);
        SpringUtil.makeCompactGrid(chi2dofPanel, 1, chi2dofPanel.getComponentCount(), 0, 0, 0, 0);
        fitStatisticsPanel.add(chi2dofPanel);

        JPanel bicPanel = new JPanel(new SpringLayout());
        bicPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(subBorderColor, 1), "BIC", TitledBorder.CENTER, TitledBorder.TOP, p11, Color.darkGray));

        bicLabel[c] = new JTextField("");
        bicLabel[c].setToolTipText("<html>Bayesian Information Criterion (BIC).<br>" + "BIC = chi^2 + (number of fitted parameters)*ln(number of data points)<br>" + "The fit with the lowest BIC value indicates the preferred model.<br>" + "The difference in BIC values of two models indicates the significance<br>" + "of the preference of the model with the lowest BIC value:<br>" + "0-2 -> no preference<br>" + "2-6 -> positive preference<br>" + "6-10 -> strong preference<br>" + ">10 -> very strong preference</html>");
        bicLabel[c].setFont(p11);
        bicLabel[c].setHorizontalAlignment(JLabel.CENTER);
        bicLabel[c].setBorder(grayBorder);
        bicLabel[c].setPreferredSize(bestFitSize);
        bicLabel[c].setMaximumSize(bestFitSize);
        bicLabel[c].setEditable(false);
        bicPanel.add(bicLabel[c]);
        SpringUtil.makeCompactGrid(bicPanel, 1, bicPanel.getComponentCount(), 0, 0, 0, 0);
        fitStatisticsPanel.add(bicPanel);


        JPanel dofPanel = new JPanel(new SpringLayout());
        dofPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(subBorderColor, 1), "dof", TitledBorder.CENTER, TitledBorder.TOP, p11, Color.darkGray));

        dofLabel[c] = new JTextField("");
        dofLabel[c].setToolTipText("<html>Degrees of freedom (dof) for the fit.<br>" + "dof = number of datapoints - number of fitted parameters.</html>");
        dofLabel[c].setFont(p11);
        dofLabel[c].setHorizontalAlignment(JLabel.CENTER);
        dofLabel[c].setBorder(grayBorder);
        dofLabel[c].setPreferredSize(bestFitSize);
        dofLabel[c].setMaximumSize(bestFitSize);
        dofLabel[c].setEditable(false);
        dofPanel.add(dofLabel[c]);
        SpringUtil.makeCompactGrid(dofPanel, 1, dofPanel.getComponentCount(), 0, 0, 0, 0);
        fitStatisticsPanel.add(dofPanel);

        JPanel chi2Panel = new JPanel(new SpringLayout());
        chi2Panel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(subBorderColor, 1), "chi\u00B2", TitledBorder.CENTER, TitledBorder.TOP, p11, Color.darkGray));

        chi2Label[c] = new JTextField("");
        chi2Label[c].setToolTipText("<html>Total &chi;<sup>2</sup> of the fit.</html>");
        chi2Label[c].setFont(p11);
        chi2Label[c].setHorizontalAlignment(JLabel.CENTER);
        chi2Label[c].setBorder(grayBorder);
        chi2Label[c].setPreferredSize(bestFitSize);
        chi2Label[c].setMaximumSize(bestFitSize);
        chi2Label[c].setEditable(false);
        chi2Panel.add(chi2Label[c]);
        SpringUtil.makeCompactGrid(chi2Panel, 1, chi2Panel.getComponentCount(), 0, 0, 0, 0);
        fitStatisticsPanel.add(chi2Panel);

        JLabel statisticsDummyLabel1 = new JLabel("");
        fitStatisticsPanel.add(statisticsDummyLabel1);

        SpringUtil.makeCompactGrid(fitStatisticsPanel, 1, fitStatisticsPanel.getComponentCount(), 0, 0, 0, 0);
        fitPanel[c].add(fitStatisticsPanel);

        // Fit Optimizations
        var opti = new FitOptimization(c, 0);
        fitPanel[c].add(opti.makeFitOptimizationPanel());

        JPanel plotPanel = new JPanel(new SpringLayout());
        plotPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(mainBorderColor, 1), "Plot Settings", TitledBorder.LEFT, TitledBorder.TOP, b12, Color.darkGray));

        JPanel modelPanel = new JPanel(new SpringLayout());

        showModelCB[c] = new JCheckBox("Show Model", showModel[c]);
        showModelCB[c].setPreferredSize(labelSize);
        showModelCB[c].setMaximumSize(labelSize);
        showModelCB[c].setFont(p12);
        showModelCB[c].setToolTipText("Enable to show transit model on plot.");
        showModelCB[c].addItemListener(e -> {
            showModel[c] = (e.getStateChange() == ItemEvent.SELECTED);
            modelColorSelection[c].setEnabled(showModel[c]);
            modelLineWidthSpinner[c].setEnabled(showModel[c]);
            showLTranParamsCB[c].setEnabled(useTransitFit[c] && showModel[c]);
            residualModelColorSelection[c].setEnabled(useTransitFit[c] && showResidual[c] && showModel[c]);
            residualLineWidthSpinner[c].setEnabled(useTransitFit[c] && showResidual[c] && showModel[c]);
            updatePlot(updateNoFits());
        });
        modelPanel.add(showModelCB[c]);

        showLTranParamsCB[c] = new JCheckBox("Show in legend", showLTranParams[c]);
        showLTranParamsCB[c].setEnabled(useTransitFit[c] && showModel[c]);
        showLTranParamsCB[c].setPreferredSize(legendLabelSize);
        showLTranParamsCB[c].setMaximumSize(legendLabelSize);
        showLTranParamsCB[c].setFont(p12);
        showLTranParamsCB[c].setToolTipText("Enable to show best fit model parameters in legend.");
        showLTranParamsCB[c].addItemListener(e -> {
            showLTranParams[c] = (e.getStateChange() == ItemEvent.SELECTED);
            updatePlot(updateNoFits());
        });
        modelPanel.add(showLTranParamsCB[c]);

        JPanel modelColorPanel = new JPanel(new SpringLayout());
        modelColorPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(subBorderColor, 1), "Line Color", TitledBorder.CENTER, TitledBorder.TOP, p11, Color.darkGray));
        modelColorPanel.setPreferredSize(colorSelectorSize);
        modelColorPanel.setMaximumSize(colorSelectorSize);

        modelColorSelection[c] = new JComboBox<>(colors);
        modelColorSelection[c].setEnabled(showModel[c]);
        modelColorSelection[c].setFont(p11);
        modelColorSelection[c].setToolTipText("<html>The color used to plot the best fit tranist model.</html>");
        modelColorSelection[c].setSelectedIndex(modelColorIndex[c]);
        modelColorSelection[c].setFont(new Font("Dialog", Font.BOLD, 12));
        modelColor[c] = colorOf(modelColorIndex[c]);
        modelColorSelection[c].setForeground(modelColor[c]);
        modelColorSelection[c].setMaximumRowCount(16);
        modelColorSelection[c].setPrototypeDisplayValue("12345678");
        modelColorSelection[c].addActionListener(ae -> {
            modelColorIndex[c] = modelColorSelection[c].getSelectedIndex();
            modelColor[c] = colorOf(modelColorIndex[c]);
            modelColorSelection[c].setForeground(modelColor[c]);
            updatePlot(updateNoFits());
        });
        modelColorPanel.add(modelColorSelection[c]);

        SpringUtil.makeCompactGrid(modelColorPanel, 1, modelColorPanel.getComponentCount(), 0, 0, 0, 0);
        modelPanel.add(modelColorPanel);

        JPanel modelLineWidthPanel = new JPanel(new SpringLayout());
        modelLineWidthPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(subBorderColor, 1), "Line Width", TitledBorder.CENTER, TitledBorder.TOP, p11, Color.darkGray));

        modelLineWidthSpinner[c] = new JSpinner(new SpinnerNumberModel(Math.max(modelLineWidth[c], 1), 1, Integer.MAX_VALUE, 1));
        modelLineWidthSpinner[c].setEnabled(showModel[c]);
        modelLineWidthSpinner[c].setFont(p11);
        modelLineWidthSpinner[c].setBorder(grayBorder);
        modelLineWidthSpinner[c].setPreferredSize(lineWidthSpinnerSize);
        modelLineWidthSpinner[c].setMaximumSize(lineWidthSpinnerSize);
        modelLineWidthSpinner[c].setToolTipText("<html>The width of the line used to plot the best fit transit model.</html>");
        modelLineWidthSpinner[c].addChangeListener(ev -> {
            modelLineWidth[c] = (Integer) modelLineWidthSpinner[c].getValue();
            updatePlot(updateNoFits());
        });
        modelLineWidthSpinner[c].addMouseWheelListener(e -> {
            if (modelLineWidthSpinner[c].isEnabled()) {
                int newValue = (Integer) modelLineWidthSpinner[c].getValue() - e.getWheelRotation();
                if (newValue >= 1 && newValue <= Integer.MAX_VALUE) modelLineWidthSpinner[c].setValue(newValue);
            }
        });
        modelLineWidthPanel.add(modelLineWidthSpinner[c]);

        SpringUtil.makeCompactGrid(modelLineWidthPanel, 1, modelLineWidthPanel.getComponentCount(), 0, 0, 0, 0);
        modelPanel.add(modelLineWidthPanel);

        JPanel optiPanelControl = new JPanel(new SpringLayout());
        optiPanelControl.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(subBorderColor, 1), "Log", TitledBorder.CENTER, TitledBorder.TOP, p11, Color.darkGray));


        JLabel modelDummyLabel = new JLabel("   ");
        modelPanel.add(modelDummyLabel);

        var logCheckBox = new JCheckBox("Log Optimization", FitOptimization.showOptLog);
        logCheckBox.addActionListener($ -> FitOptimization.showOptLog = logCheckBox.isSelected());
        logCheckBox.setToolTipText("Display a log of optimization actions.");
        modelPanel.add(logCheckBox);

        SpringUtil.makeCompactGrid(modelPanel, 1, modelPanel.getComponentCount(), 0, 0, 0, 0);
        plotPanel.add(modelPanel);


        JPanel residualPanel = new JPanel(new SpringLayout());

        showResidualCB[c] = new JCheckBox("Show Residuals", showResidual[c]);
        showResidualCB[c].setEnabled(useTransitFit[c]);
        showResidualCB[c].setPreferredSize(labelSize);
        showResidualCB[c].setMaximumSize(labelSize);
        showResidualCB[c].setFont(p12);
        showResidualCB[c].setToolTipText("Enable to show transit model residuals on plot.");
        showResidualCB[c].addItemListener(e -> {
            showResidual[c] = (e.getStateChange() == ItemEvent.SELECTED);
            residualModelColorSelection[c].setEnabled(useTransitFit[c] && showResidual[c] && showModel[c]);
            residualLineWidthSpinner[c].setEnabled(useTransitFit[c] && showResidual[c] && showModel[c]);
            residualColorSelection[c].setEnabled(useTransitFit[c] && showResidual[c]);
            residualSymbolSelection[c].setEnabled(useTransitFit[c] && showResidual[c]);
            residualShiftSpinner[c].setEnabled(useTransitFit[c] && showResidual[c]);
            showLResidualCB[c].setEnabled(useTransitFit[c] && showResidual[c]);
            showResidualErrorCB[c].setEnabled(useTransitFit[c] && showResidual[c]);
            updatePlot(updateNoFits());
        });
        residualPanel.add(showResidualCB[c]);

        Dimension showCBSize = new Dimension(120, 15);
        JPanel residualOptionsPanel = new JPanel(new SpringLayout());

        showLResidualCB[c] = new JCheckBox("Show in legend", showLResidual[c]);
        showLResidualCB[c].setEnabled(showResidual[c] && useTransitFit[c]);
        showLResidualCB[c].setPreferredSize(legendLabelSize);
        showLResidualCB[c].setMaximumSize(legendLabelSize);
        showLResidualCB[c].setFont(p12);
        showLResidualCB[c].setToolTipText("Enable to show the residual description in legend.");
        showLResidualCB[c].addItemListener(e -> {
            showLResidual[c] = (e.getStateChange() == ItemEvent.SELECTED);
            updatePlot(updateNoFits());
        });
        residualOptionsPanel.add(showLResidualCB[c]);

        showResidualErrorCB[c] = new JCheckBox("Show Error", showResidualError[c]);
        showResidualErrorCB[c].setEnabled(showResidual[c] && useTransitFit[c]);
        showResidualErrorCB[c].setPreferredSize(showCBSize);
        showResidualErrorCB[c].setMaximumSize(showCBSize);
        showResidualErrorCB[c].setFont(p12);
        showResidualErrorCB[c].setToolTipText("Enable to show the residual error in the plot.");
        showResidualErrorCB[c].addItemListener(e -> {
            showResidualError[c] = (e.getStateChange() == ItemEvent.SELECTED);
            updatePlot(updateNoFits());
        });
        residualOptionsPanel.add(showResidualErrorCB[c]);
        SpringUtil.makeCompactGrid(residualOptionsPanel, residualOptionsPanel.getComponentCount(), 1, 0, 0, 0, 0);
        residualPanel.add(residualOptionsPanel);

        JPanel residualModelColorPanel = new JPanel(new SpringLayout());
        residualModelColorPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(subBorderColor, 1), "Line Color", TitledBorder.CENTER, TitledBorder.TOP, p11, Color.darkGray));
        residualModelColorPanel.setPreferredSize(colorSelectorSize);
        residualModelColorPanel.setMaximumSize(colorSelectorSize);

        residualModelColorSelection[c] = new JComboBox<>(colors);
        residualModelColorSelection[c].setEnabled(showResidual[c] && useTransitFit[c] && showModel[c]);
        residualModelColorSelection[c].setFont(p11);
        residualModelColorSelection[c].setToolTipText("<html>The color used to plot the dashed-line residual model.</html>");
        residualModelColorSelection[c].setSelectedIndex(residualModelColorIndex[c]);
        residualModelColorSelection[c].setFont(new Font("Dialog", Font.BOLD, 12));
        residualModelColor[c] = colorOf(residualModelColorIndex[c]);
        residualModelColorSelection[c].setForeground(residualModelColor[c]);
        residualModelColorSelection[c].setMaximumRowCount(16);
        residualModelColorSelection[c].setPrototypeDisplayValue("12345678");
        residualModelColorSelection[c].addActionListener(ae -> {
            residualModelColorIndex[c] = residualModelColorSelection[c].getSelectedIndex();
            residualModelColor[c] = colorOf(residualModelColorIndex[c]);
            residualModelColorSelection[c].setForeground(residualModelColor[c]);
            updatePlot(updateNoFits());
        });
        residualModelColorPanel.add(residualModelColorSelection[c]);

        SpringUtil.makeCompactGrid(residualModelColorPanel, 1, residualModelColorPanel.getComponentCount(), 0, 0, 0, 0);
        residualPanel.add(residualModelColorPanel);

        JPanel residualLineWidthPanel = new JPanel(new SpringLayout());
        residualLineWidthPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(subBorderColor, 1), "Line Width", TitledBorder.CENTER, TitledBorder.TOP, p11, Color.darkGray));

        residualLineWidthSpinner[c] = new JSpinner(new SpinnerNumberModel(Math.max(residualLineWidth[c], 1), 1, Integer.MAX_VALUE, 1));
        residualLineWidthSpinner[c].setEnabled(showResidual[c] && useTransitFit[c] && showModel[c]);
        residualLineWidthSpinner[c].setFont(p11);
        residualLineWidthSpinner[c].setBorder(grayBorder);
        residualLineWidthSpinner[c].setPreferredSize(lineWidthSpinnerSize);
        residualLineWidthSpinner[c].setMaximumSize(lineWidthSpinnerSize);
        residualLineWidthSpinner[c].setToolTipText("<html>The width used to plot the dashed-line residual model.</html>");
        residualLineWidthSpinner[c].addChangeListener(ev -> {
            residualLineWidth[c] = (Integer) residualLineWidthSpinner[c].getValue();
            updatePlot(updateNoFits());
        });
        residualLineWidthSpinner[c].addMouseWheelListener(e -> {
            if (residualLineWidthSpinner[c].isEnabled()) {
                int newValue = (Integer) residualLineWidthSpinner[c].getValue() - e.getWheelRotation();
                if (newValue >= 1 && newValue <= Integer.MAX_VALUE) residualLineWidthSpinner[c].setValue(newValue);
            }
        });
        residualLineWidthPanel.add(residualLineWidthSpinner[c]);

        SpringUtil.makeCompactGrid(residualLineWidthPanel, 1, residualLineWidthPanel.getComponentCount(), 0, 0, 0, 0);
        residualPanel.add(residualLineWidthPanel);


        JPanel residualSymbolPanel = new JPanel(new SpringLayout());
        residualSymbolPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(subBorderColor, 1), "Symbol", TitledBorder.CENTER, TitledBorder.TOP, p11, Color.darkGray));
        residualSymbolPanel.setPreferredSize(colorSelectorSize);
        residualSymbolPanel.setMaximumSize(colorSelectorSize);

        residualSymbolSelection[c] = new JComboBox<>(markers);
        residualSymbolSelection[c].setEnabled(useTransitFit[c] && showResidual[c]);
        residualSymbolSelection[c].setFont(p11);
        residualSymbolSelection[c].setToolTipText("<html>The symbol used to plot the transit model residuals.</html>");
        residualColor[c] = colorOf(residualColorIndex[c]);
        residualSymbolSelection[c].setForeground(residualColor[c]);
        residualSymbolSelection[c].setFont(new Font("Dialog", Font.BOLD, 12));
        residualSymbolSelection[c].setSelectedIndex(residualSymbolIndex[c]);
        residualSymbolSelection[c].setPrototypeDisplayValue("12345");
        residualSymbolSelection[c].addActionListener(ae -> {
            residualSymbolIndex[c] = residualSymbolSelection[c].getSelectedIndex();
            updatePlot(updateNoFits());
        });
        residualSymbolPanel.add(residualSymbolSelection[c]);

        SpringUtil.makeCompactGrid(residualSymbolPanel, 1, residualSymbolPanel.getComponentCount(), 0, 0, 0, 0);
        residualPanel.add(residualSymbolPanel);


        JPanel residualColorPanel = new JPanel(new SpringLayout());
        residualColorPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(subBorderColor, 1), "Symbol Color", TitledBorder.CENTER, TitledBorder.TOP, p11, Color.darkGray));
        residualColorPanel.setPreferredSize(colorSelectorSize);
        residualColorPanel.setMaximumSize(colorSelectorSize);

        residualColorSelection[c] = new JComboBox<>(colors);
        residualColorSelection[c].setEnabled(useTransitFit[c] && showResidual[c]);
        residualColorSelection[c].setFont(p11);
        residualColorSelection[c].setToolTipText("<html>The symbol color used to plot the transit model residuals.</html>");
        residualColorSelection[c].setSelectedIndex(residualColorIndex[c]);
        residualColorSelection[c].setFont(new Font("Dialog", Font.BOLD, 12));
        residualColor[c] = colorOf(residualColorIndex[c]);
        residualColorSelection[c].setForeground(residualColor[c]);
        residualSymbolSelection[c].setForeground(residualColor[c]);
        residualColorSelection[c].setMaximumRowCount(16);
        residualColorSelection[c].setPrototypeDisplayValue("12345678");
        residualColorSelection[c].addActionListener(ae -> {
            residualColorIndex[c] = residualColorSelection[c].getSelectedIndex();
            residualColor[c] = colorOf(residualColorIndex[c]);
            residualColorSelection[c].setForeground(residualColor[c]);
            residualSymbolSelection[c].setForeground(residualColor[c]);
            updatePlot(updateNoFits());
        });
        residualColorPanel.add(residualColorSelection[c]);

        SpringUtil.makeCompactGrid(residualColorPanel, 1, residualColorPanel.getComponentCount(), 0, 0, 0, 0);
        residualPanel.add(residualColorPanel);


        JPanel residualShiftSpinnerPanel = new JPanel(new SpringLayout());
        residualShiftSpinnerPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(subBorderColor, 1), "Shift", TitledBorder.CENTER, TitledBorder.TOP, p11, Color.darkGray));
        residualShiftSpinner[c] = new JSpinner(new SpinnerNumberModel(force[c] ? autoResidualShift[c] * 100 : residualShift[c], null, null, force[c] ? autoShiftStep[c] : customShiftStep[c]));
        residualShiftSpinner[c].setEnabled(showResidual[c] && useTransitFit[c]);
        residualShiftSpinner[c].setFont(p11);
        residualShiftSpinner[c].setEditor(new JSpinner.NumberEditor(residualShiftSpinner[c], fitFormat));
        residualShiftSpinner[c].setPreferredSize(controlSpinnerSize);
        residualShiftSpinner[c].setMaximumSize(controlSpinnerSize);
        residualShiftSpinner[c].setComponentPopupMenu(customshiftsteppopup[c]);
        residualShiftSpinner[c].setToolTipText("<html>Enter the amount to shift the residuals on the y-axis<br>" + "relative to the transit model baseline.<br>" + "---------------------------------------------<br>" + "Right click to set spinner stepsize</html>");
        residualShiftSpinner[c].addChangeListener(ev -> {
            if (force[c]) { autoResidualShift[c] = (Double) residualShiftSpinner[c].getValue() / 100.0; } else {
                residualShift[c] = (Double) residualShiftSpinner[c].getValue();
            }
            updatePlot(updateNoFits());
        });
        residualShiftSpinner[c].addMouseWheelListener(e -> {
            if (residualShiftSpinner[c].isEnabled()) {
                if (force[c]) {
                    residualShiftSpinner[c].setValue((Double) residualShiftSpinner[c].getValue() - invertYAxisSign * e.getWheelRotation() * autoShiftStep[c]);
                } else {
                    residualShiftSpinner[c].setValue((Double) residualShiftSpinner[c].getValue() - invertYAxisSign * e.getWheelRotation() * customShiftStep[c]);
                }
            }
        });
        residualShiftSpinnerPanel.add(residualShiftSpinner[c]);
        SpringUtil.makeCompactGrid(residualShiftSpinnerPanel, 1, residualShiftSpinnerPanel.getComponentCount(), 2, 0, 0, 0);
        residualPanel.add(residualShiftSpinnerPanel);


        JLabel residualDummyLabel = new JLabel("");
        residualPanel.add(residualDummyLabel);

        SpringUtil.makeCompactGrid(residualPanel, 1, residualPanel.getComponentCount(), 0, 0, 0, 0);
        plotPanel.add(residualPanel);

        SpringUtil.makeCompactGrid(plotPanel, plotPanel.getComponentCount(), 1, 0, 0, 0, 0);
        fitPanel[c].add(plotPanel);

        JPanel controlPanel = new JPanel(new SpringLayout());
        controlPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(mainBorderColor, 1), "Fit Control", TitledBorder.LEFT, TitledBorder.TOP, b12, Color.darkGray));

        JLabel controlLabel = new JLabel("Fit Control");
        controlLabel.setFont(p12);
        controlLabel.setPreferredSize(labelSize);
        controlLabel.setMaximumSize(labelSize);
        controlPanel.add(controlLabel);

        JPanel fitControlPanel = new JPanel(new SpringLayout());
        fitControlPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(subBorderColor, 1), "Fit Update Options", TitledBorder.CENTER, TitledBorder.TOP, p11, Color.darkGray));

        autoUpdateFitCB[c] = new JCheckBox("Auto Update Fit", autoUpdateFit[c]);
        autoUpdateFitCB[c].setPreferredSize(bestFitSize);
        autoUpdateFitCB[c].setMaximumSize(bestFitSize);
        autoUpdateFitCB[c].setFont(p11);
        autoUpdateFitCB[c].setToolTipText("Enable to automatically update the fit when settings change.");
        autoUpdateFitCB[c].addItemListener(e -> {
            if (e.getStateChange() == ItemEvent.DESELECTED) {
                autoUpdateFit[c] = false;
            } else if (e.getStateChange() == ItemEvent.SELECTED) {
                autoUpdateFit[c] = true;
            }
            if (autoUpdateFit[c]) updatePlot(updateOneFit(c));
        });
        fitControlPanel.add(autoUpdateFitCB[c]);

        JButton fitNowButton = new JButton("Update Fit Now");
        fitNowButton.setToolTipText("<html>Update the fit and redraw the plot using the above settings.</html>");
        fitNowButton.setFont(p11);
        fitNowButton.setPreferredSize(bestFitSize);
        fitNowButton.setMaximumSize(bestFitSize);
        fitNowButton.addActionListener(e -> {
            if (plotY[c]) {
                updatePlot(updateOneFit(c));
            } else {
                IJ.beep();
                IJ.showMessage("The 'Plot' option for this curve must be enabled before fitting the data.");
            }
        });
        fitControlPanel.add(fitNowButton);

        SpringUtil.makeCompactGrid(fitControlPanel, 1, fitControlPanel.getComponentCount(), 2, 2, 4, 2);
        controlPanel.add(fitControlPanel);

        JPanel minimizationTolerancePanel = new JPanel(new SpringLayout());
        minimizationTolerancePanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(subBorderColor, 1), "Fit Tolerance", TitledBorder.CENTER, TitledBorder.TOP, p11, Color.darkGray));

        toleranceSpinner[c] = new JSpinner(new SpinnerNumberModel(tolerance[c], Double.MIN_NORMAL, Double.MAX_VALUE, tolerance[c] * 0.0010));
        toleranceSpinner[c].setFont(p11);
        toleranceSpinner[c].setEditor(new JSpinner.NumberEditor(toleranceSpinner[c], "0.0E0"));
        toleranceSpinner[c].setBorder(grayBorder);
        toleranceSpinner[c].setPreferredSize(controlSpinnerSize);
        toleranceSpinner[c].setMaximumSize(controlSpinnerSize);
//            toleranceSpinner[c].setComponentPopupMenu(priorCenterStepPopup[row]);
        toleranceSpinner[c].setToolTipText("<html>The minimization tolerance for the fit.<br>" + "The default value is 1.0E-10.</html>");
        toleranceSpinner[c].addChangeListener(ev -> {
            double newValue = (Double) toleranceSpinner[c].getValue();
            if (newValue < tolerance[c] - tolerance[c] * 0.0009 && newValue > tolerance[c] - tolerance[c] * 0.0011) {
                tolerance[c] /= 10.0;
//                        toleranceSpinner[c].setValue(tolerance[c]);
                toleranceSpinner[c].setModel(new SpinnerNumberModel(tolerance[c], Double.MIN_NORMAL, Double.MAX_VALUE, tolerance[c] * 0.0010));
                if (autoUpdateFit[c]) updatePlot(updateOneFit(c));
            } else if (newValue < tolerance[c] + tolerance[c] * 0.0011 && newValue > tolerance[c] + tolerance[c] * 0.0009) {
                tolerance[c] *= 10.0;
//                        toleranceSpinner[c].setValue(tolerance[c]);
                toleranceSpinner[c].setModel(new SpinnerNumberModel(tolerance[c], Double.MIN_NORMAL, Double.MAX_VALUE, tolerance[c] * 0.0010));
                if (autoUpdateFit[c]) updatePlot(updateOneFit(c));
            } else if (newValue < 0.99999 * tolerance[c] || newValue > 1.00001 * tolerance[c]) {
                tolerance[c] = newValue;
                toleranceSpinner[c].setModel(new SpinnerNumberModel(tolerance[c], Double.MIN_NORMAL, Double.MAX_VALUE, tolerance[c] * 0.0010));
                if (autoUpdateFit[c]) updatePlot(updateOneFit(c));
            }
        });
        toleranceSpinner[c].addMouseWheelListener(e -> {
            double newValue = (Double) toleranceSpinner[c].getValue() - (e.getWheelRotation() / Math.abs((float) e.getWheelRotation())) * tolerance[c] * 0.0010;
            if (newValue >= Double.MIN_NORMAL && newValue <= Double.MAX_VALUE) toleranceSpinner[c].setValue(newValue);
        });
        minimizationTolerancePanel.add(toleranceSpinner[c]);

        SpringUtil.makeCompactGrid(minimizationTolerancePanel, 1, minimizationTolerancePanel.getComponentCount(), 2, 2, 4, 2);
        controlPanel.add(minimizationTolerancePanel);

        JPanel maxFitStepsPanel = new JPanel(new SpringLayout());
        maxFitStepsPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(subBorderColor, 1), "Max Allowed Steps", TitledBorder.CENTER, TitledBorder.TOP, p11, Color.darkGray));

        maxFitStepsSpinner[c] = new JSpinner(new SpinnerNumberModel(Math.max(maxFitSteps[c], 1000), 1000, Integer.MAX_VALUE, 1000));
        maxFitStepsSpinner[c].setFont(p11);
//            toleranceSpinner[c].setEditor(new JSpinner.NumberEditor(toleranceSpinner[c], "0.0E0"));
        maxFitStepsSpinner[c].setBorder(grayBorder);
        maxFitStepsSpinner[c].setPreferredSize(controlSpinnerSize);
        maxFitStepsSpinner[c].setMaximumSize(controlSpinnerSize);
//            toleranceSpinner[c].setComponentPopupMenu(priorCenterStepPopup[row]);
        maxFitStepsSpinner[c].setToolTipText("<html>The maximum number of steps allowed to reach the minimization tolerance for the fit.<br>" + "The default value is 20,000 steps.</html>");
        maxFitStepsSpinner[c].addChangeListener(ev -> {
            maxFitSteps[c] = (Integer) maxFitStepsSpinner[c].getValue();
            if (autoUpdateFit[c]) updatePlot(updateOneFit(c));
        });
        maxFitStepsSpinner[c].addMouseWheelListener(e -> {
            int newValue = (Integer) maxFitStepsSpinner[c].getValue() - e.getWheelRotation() * 1000;
            if (newValue >= 1000 && newValue <= Integer.MAX_VALUE) maxFitStepsSpinner[c].setValue(newValue);
        });
        maxFitStepsPanel.add(maxFitStepsSpinner[c]);

        SpringUtil.makeCompactGrid(maxFitStepsPanel, 1, maxFitStepsPanel.getComponentCount(), 2, 2, 4, 2);
        controlPanel.add(maxFitStepsPanel);


        JPanel stepsTakenPanel = new JPanel(new SpringLayout());
        stepsTakenPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(subBorderColor, 1), "Steps Taken", TitledBorder.CENTER, TitledBorder.TOP, p11, Color.darkGray));

        stepsTakenLabel[c] = new JTextField("");
        stepsTakenLabel[c].setToolTipText("<html>The number of minimization steps taken to reach the specified fit tolerance.<br>" + "Green Border: fit converged<br>" + "Red Border: fit did not converge<br>" + "Gray Border: no fit in this session</html>");
        stepsTakenLabel[c].setFont(p11);
        stepsTakenLabel[c].setHorizontalAlignment(JLabel.RIGHT);
        stepsTakenLabel[c].setBorder(grayBorder);
        stepsTakenLabel[c].setPreferredSize(bestFitSize);
        stepsTakenLabel[c].setMaximumSize(bestFitSize);
        stepsTakenLabel[c].setEditable(false);
        stepsTakenPanel.add(stepsTakenLabel[c]);
        SpringUtil.makeCompactGrid(stepsTakenPanel, 1, stepsTakenPanel.getComponentCount(), 0, 0, 0, 0);
        controlPanel.add(stepsTakenPanel);

        JLabel controlDummyLabel2 = new JLabel("");
//            controlDummyLabel2.setPreferredSize(labelSize);
        controlPanel.add(controlDummyLabel2);

        SpringUtil.makeCompactGrid(controlPanel, 1, controlPanel.getComponentCount(), 0, 0, 0, 0);
        fitPanel[c].add(controlPanel);

        SpringUtil.makeCompactGrid(fitPanel[c], fitPanel[c].getComponentCount(), 1, 2, 2, 2, 2);
        enableTransitComponents(c);
        fitScrollPane[c] = new JScrollPane(fitPanel[c]);
        fitFrame[c].add(fitScrollPane[c]);
        fitFrame[c].setJMenuBar(fitMenuBar[c]);
        fitFrame[c].setResizable(true);

        int firstFittedCurve = 0;
        for (int cc = 0; cc < maxCurves; cc++) {
            if (detrendtypecombobox[cc].getSelectedIndex() == 9) {
                firstFittedCurve = cc;
                break;
            }
        }
        fitFrame[c].pack();
        if (rememberWindowLocations) {
            if (keepSeparateLocationsForFitWindows) {
                IJU.setFrameSizeAndLocation(fitFrame[c], fitFrameLocationX[c], fitFrameLocationY[c], 0, 0);
            } else if (c == firstFittedCurve) {
                IJU.setFrameSizeAndLocation(fitFrame[c], fitFrameLocationX[c], fitFrameLocationY[c], 0, 0);
            } else {
                IJU.setFrameSizeAndLocation(fitFrame[c], (fitFrame[firstFittedCurve] != null && fitFrame[firstFittedCurve].isVisible() ? fitFrame[firstFittedCurve].getLocation().x : fitFrameLocationX[firstFittedCurve]) + (c - firstFittedCurve) * 25, (fitFrame[firstFittedCurve] != null && fitFrame[firstFittedCurve].isVisible() ? fitFrame[firstFittedCurve].getLocation().y : fitFrameLocationY[firstFittedCurve]) + (c - firstFittedCurve) * 25, 0, 0);
            }
        } else {
            IJU.setFrameSizeAndLocation(fitFrame[c], 40 + c * 25, 40 + c * 25, 0, 0);
        }
        if (openFitPanels && detrendFitIndex[c] == 9) fitFrame[c].setVisible(true);

        FileDrop fileDrop = new FileDrop(fitPanel[c], BorderFactory.createEmptyBorder(), MultiPlot_::openDragAndDropFiles);
    }

    static void updatePriorCenters(int c) {
        if (!lockToCenter[c][0] && autoUpdatePrior[c][0]) {
            priorCenterSpinner[c][0].setValue(yBaselineAverage[c]);   //f0 = baseline flux
            priorWidthSpinner[c][0].setValue(Math.abs(yBaselineAverage[c] / 5.0));
        }

        double rpOrstarEst = Math.abs((yBaselineAverage[c] - yDepthEstimate[c]) / yBaselineAverage[c]);
//                IJ.log("yBaselineAverage["+c+"]="+yBaselineAverage[c]);
//                IJ.log("yCenterAverage["+c+"]="+yDepthEstimate[c]);
        if (!lockToCenter[c][1] && autoUpdatePrior[c][1]) {
            priorCenterSpinner[c][1].setValue(rpOrstarEst);          // depth = (r_p/r_*)^2
            priorWidthSpinner[c][1].setValue(Math.abs(rpOrstarEst / 2.0));
        }

        //Adapted from Winn 2010 equation 14
        if (!lockToCenter[c][2] && autoUpdatePrior[c][2]) {
            priorCenterSpinner[c][2].setValue((1 + Math.sqrt(priorCenter[c][1])) / (Math.sin(Math.PI * (dMarker3Value - dMarker2Value) / orbitalPeriod[c]))); // ar = a/r_*
        }
        if (!lockToCenter[c][3] && autoUpdatePrior[c][3]) {
            if (showXAxisNormal) {
                priorCenterSpinner[c][3].setValue((int) xPlotMinRaw + (dMarker2Value + dMarker3Value) / 2.0);   // tc = transit center time
            } else {
                priorCenterSpinner[c][3].setValue((dMarker2Value + dMarker3Value) / 2.0);   // tc = transit center time
            }
        }
        if (!lockToCenter[c][4] && !bpLock[c] && autoUpdatePrior[c][4]) {
            priorCenterSpinner[c][4].setValue(Math.round(10.0 * Math.acos((0.5 + Math.sqrt(rpOrstarEst)) / (priorCenter[c][2])) * 180.0 / Math.PI) / 10.0); // inclination
        }
    }

    static void createFittedParametersRow(JPanel parentPanel, final int c, final int row, final String rowName, final String rowNameToolTipText, final double min, final double max) {
        final boolean isDetrend = (rowName == null);
        if (isDetrend) {  //detrend parameter
            useFitDetrendCB[c][row - 7] = new JCheckBox("", detrendIndex[c][row - 7] != 0);
            useFitDetrendCB[c][row - 7].setToolTipText("<html>Enable to include detrend variable " + (row - 6) + " in the fit</html>");
            useFitDetrendCB[c][row - 7].setPreferredSize(checkBoxSize);
            useFitDetrendCB[c][row - 7].setMaximumSize(checkBoxSize);
            useFitDetrendCB[c][row - 7].addItemListener(e -> {
                detrendParChanged = true;
                detrendVarDisplayed[c] = row - 7;
                detrendVarButton[c][row - 7].setSelected(true);
                prevBic[c] = bic[c];
                if (useFitDetrendCB[c][row - 7].isSelected()) {
                    useFitDetrendCB[c][row - 7].setEnabled(false);
                    detrendbox[c].setSelectedIndex(fitDetrendComboBox[c][row - 7].getSelectedIndex());
                    useFitDetrendCB[c][row - 7].setEnabled(true);
                } else {
                    detrendlabelhold[c][row - 7] = (String) fitDetrendComboBox[c][row - 7].getSelectedItem();//detrendlabel[c][row-7];
                    useFitDetrendCB[c][row - 7].setEnabled(false);
                    detrendbox[c].setSelectedIndex(0);
                    useFitDetrendCB[c][row - 7].setEnabled(true);
                }
                enableTransitComponents(c);
                if (autoUpdateFit[c] && !multiUpdate) updatePlot(updateOneFit(c));
            });
            parentPanel.add(useFitDetrendCB[c][row - 7]);

            fitDetrendComboBox[c][row - 7] = new JComboBox<>(columnsDetrend);
            fitDetrendComboBox[c][row - 7].setFont(p11);
            fitDetrendComboBox[c][row - 7].setToolTipText(rowNameToolTipText);
            fitDetrendComboBox[c][row - 7].setMaximumRowCount(16);
            fitDetrendComboBox[c][row - 7].setPreferredSize(choiceBoxSize);
            fitDetrendComboBox[c][row - 7].setMaximumSize(choiceBoxSize);
            if (detrendIndex[c][row - 7] == 0 && !detrendlabelhold[c][row - 7].trim().equals("")) {
                fitDetrendComboBox[c][row - 7].setSelectedItem(detrendlabelhold[c][row - 7].trim());
            } else { fitDetrendComboBox[c][row - 7].setSelectedItem(detrendlabel[c][row - 7]); }
            fitDetrendComboBox[c][row - 7].addActionListener(ae -> {
                if (fitDetrendComboBox[c][row - 7].isEnabled()) {
                    detrendParChanged = true;
                    useFitDetrendCB[c][row - 7].setSelected(true);
                    detrendVarButton[c][row - 7].setSelected(true);
                    detrendVarDisplayed[c] = row - 7;
                    fitDetrendComboBox[c][row - 7].setEnabled(false);
                    detrendbox[c].setSelectedIndex(fitDetrendComboBox[c][row - 7].getSelectedIndex());
                    fitDetrendComboBox[c][row - 7].setEnabled(true);
                }
            });
            parentPanel.add(fitDetrendComboBox[c][row - 7]);
        } else {  //light curve parameter
            JLabel rowNameLabel = new JLabel(rowName);
            if (rowNameLabel.isVisible()) {
                rowNameLabel.setFont(p12);
                rowNameLabel.setToolTipText(rowNameToolTipText);
                rowNameLabel.setPreferredSize(labelSize);
                rowNameLabel.setMaximumSize(labelSize);
            }
            parentPanel.add(rowNameLabel);
        }

        bestFitLabel[c][row] = new JTextField("");
        bestFitLabel[c][row].setToolTipText("<html>Green Border: best fit parameter value from converged fit<br>" + "Red Border: last parameter value from fit that did not converge<br>" + "Gray Border: parameter has not been fit in this session");
        bestFitLabel[c][row].setFont(p11);
        bestFitLabel[c][row].setHorizontalAlignment(JLabel.RIGHT);
        bestFitLabel[c][row].setBorder(grayBorder);
        bestFitLabel[c][row].setEnabled(isDetrend ? useFitDetrendCB[c][row - 7].isSelected() : useTransitFit[c]);
        bestFitLabel[c][row].setEditable(false);
        bestFitLabel[c][row].setPreferredSize(bestFitSize);
        bestFitLabel[c][row].setMaximumSize(bestFitSize);
        parentPanel.add(bestFitLabel[c][row]);

//        JLabel dummyLabel3 = new JLabel();
//        dummyLabel3.setFont(p12);
//        dummyLabel3.setPreferredSize(spacerSize);
//        dummyLabel3.setMaximumSize(spacerSize);
//        parentPanel.add(dummyLabel3);
        copyAndLockButton[c][row] = new JButton(copyAndLockIcon);
        copyAndLockButton[c][row].setMaximumSize(new Dimension(18, 25));
        copyAndLockButton[c][row].setPreferredSize(new Dimension(18, 25));
        copyAndLockButton[c][row].setToolTipText("Click to copy fitted value to prior center value and lock.");
        copyAndLockButton[c][row].addActionListener(e -> {
            double value = bestFit[c][row];
            if (row == 1) {
                lockToCenterCB[c][row].setSelected(true);
                priorCenterSpinner[c][row].setValue(value*value);
            } else if (row == 4) {
                if (!bpLock[c]){
                    lockToCenterCB[c][row].setSelected(true);
                    priorCenterSpinner[c][row].setValue(180.0 * value / Math.PI);
                }
            } else {
                lockToCenterCB[c][row].setSelected(true);
                priorCenterSpinner[c][row].setValue(value);
            }
        });
        parentPanel.add(copyAndLockButton[c][row]);
        lockToCenterCB[c][row] = new JCheckBox("", lockToCenter[c][row]);
        lockToCenterCB[c][row].setEnabled(isDetrend ? useFitDetrendCB[c][row - 7].isSelected() : (row == 4 && bpLock[c] ? false : useTransitFit[c]));
        lockToCenterCB[c][row].setHorizontalAlignment(JLabel.CENTER);
        lockToCenterCB[c][row].setPreferredSize(checkBoxSize);
        lockToCenterCB[c][row].setMaximumSize(checkBoxSize);
        lockToCenterCB[c][row].setFont(p11);
        lockToCenterCB[c][row].setToolTipText("Lock the parameter to 'Prior Center' value");
        lockToCenterCB[c][row].addItemListener(e -> {
            if (e.getStateChange() == ItemEvent.DESELECTED) {
                lockToCenter[c][row] = false;
                priorCenterSpinner[c][row].setBorder(grayBorder);
            } else if (e.getStateChange() == ItemEvent.SELECTED) {
                lockToCenter[c][row] = true;
                priorCenterSpinner[c][row].setBorder(lockBorder);
            }
            enableTransitComponents(c);
            if (autoUpdateFit[c]) updatePlot(updateOneFit(c));
        });
        parentPanel.add(lockToCenterCB[c][row]);
        if (Double.isNaN(priorCenter[c][row]) || Double.isInfinite(priorCenter[c][row])) priorCenter[c][row] = 0.0;
        if (priorCenter[c][row] < min) priorCenter[c][row] = min;
        if (priorCenter[c][row] > max) priorCenter[c][row] = max;
//            IJ.log("priorCenter["+c+"]["+row+"]="+priorCenter[c][row]);
//            IJ.log("min="+min);
//            IJ.log("max="+max);
//            IJ.log("priorCenterStep["+row+"]="+priorCenterStep[row]);
//            IJ.log("");
        priorCenterSpinner[c][row] = new JSpinner(new SpinnerNumberModel(Double.valueOf(priorCenter[c][row]), Double.valueOf(min), Double.valueOf(max), Double.valueOf(priorCenterStep[row])));
        priorCenterSpinner[c][row].setEnabled(isDetrend ? useFitDetrendCB[c][row - 7].isSelected() : (row == 4 && bpLock[c] ? false : useTransitFit[c]));
        priorCenterSpinner[c][row].setFont(p11);
        priorCenterSpinner[c][row].setEditor(new JSpinner.NumberEditor(priorCenterSpinner[c][row], fitFormat));
        priorCenterSpinner[c][row].setBorder(lockToCenter[c][row] ? lockBorder : grayBorder);
        priorCenterSpinner[c][row].setPreferredSize(spinnerSize);
        priorCenterSpinner[c][row].setMaximumSize(spinnerSize);
        priorCenterSpinner[c][row].setComponentPopupMenu(priorCenterStepPopup[row]);
        if (row < 5 && !isDetrend) {
            priorCenterSpinner[c][row].setToolTipText("<html>The parameter's starting value for the fit.<br>" + "---------------------------------------------<br>" + "To enter a custom value, either disable 'Auto Update Priors' or enable 'Lock'.<br>" + "---------------------------------------------<br>" + "Right click to set spinner stepsize</html>");
        } else {
            priorCenterSpinner[c][row].setToolTipText("<html>The parameter's starting value for the fit.<br>" + "---------------------------------------------<br>" + "Right click to set spinner stepsize</html>");
        }
        priorCenterSpinner[c][row].addChangeListener(ev -> {

            priorCenter[c][row] = (Double) priorCenterSpinner[c][row].getValue();
            fitStepSpinner[c][row].setValue(getFitStep(c, row));
            if (autoUpdateFit[c]) updatePlot(updateOneFit(c));
        });
        priorCenterSpinner[c][row].addMouseWheelListener(e -> {
            double newValue = (Double) priorCenterSpinner[c][row].getValue() - e.getWheelRotation() * priorCenterStep[row];
            if (priorCenterSpinner[c][row].isEnabled() && newValue >= min && newValue <= max) {
                priorCenterSpinner[c][row].setValue(newValue);
            }
        });
        parentPanel.add(priorCenterSpinner[c][row]);

        JLabel dummyLabel = new JLabel();
        dummyLabel.setFont(p12);
//            rowNameLabel.setToolTipText(rowNameToolTipText);
        dummyLabel.setPreferredSize(spacerSize);
        dummyLabel.setMaximumSize(spacerSize);
        parentPanel.add(dummyLabel);

        usePriorWidthCB[c][row] = new JCheckBox("", usePriorWidth[c][row]);
        usePriorWidthCB[c][row].setEnabled(isDetrend ? useFitDetrendCB[c][row - 7].isSelected() : useTransitFit[c]);
        usePriorWidthCB[c][row].setHorizontalAlignment(JLabel.CENTER);
        usePriorWidthCB[c][row].setPreferredSize(checkBoxSize);
        usePriorWidthCB[c][row].setMaximumSize(checkBoxSize);
        usePriorWidthCB[c][row].setFont(p11);
        if (row < 2 && !isDetrend) {
            usePriorWidthCB[c][row].setToolTipText("<html>If enabled, the 'Prior Width' value constrains the parameter to be in<br>" + "the range of the 'Prior Center' value plus/minus the 'Prior Width' value.<br>" + "---------------------------------------------------<br>" + "To enter a custom 'Prior Width' value, either disable 'Auto Update Priors'<br>" + "or enable 'Lock' to 'Prior Center'. </html>");
        } else {
            usePriorWidthCB[c][row].setToolTipText("<html>If enabled, the 'Prior Width' value constrains the parameter to be in<br>" + "the range of the 'Prior Center' value plus/minus the 'Prior Width' value.</html>");
        }
        usePriorWidthCB[c][row].addItemListener(e -> {
            if (e.getStateChange() == ItemEvent.DESELECTED) {
                usePriorWidth[c][row] = false;
            } else if (e.getStateChange() == ItemEvent.SELECTED) {
                usePriorWidth[c][row] = true;

            }
            enableTransitComponents(c);
            fitStepSpinner[c][row].setValue(getFitStep(c, row));
            if (autoUpdateFit[c]) updatePlot(updateOneFit(c));
        });

        parentPanel.add(usePriorWidthCB[c][row]);

        priorWidthSpinner[c][row] = new JSpinner(new SpinnerNumberModel(priorWidth[c][row] < 0 ? 0.1 : priorWidth[c][row], 0.0, null, priorWidthStep[row]));
        priorWidthSpinner[c][row].setEnabled((isDetrend ? useFitDetrendCB[c][row - 7].isSelected() : useTransitFit[c]) && usePriorWidth[c][row]);
        priorWidthSpinner[c][row].setFont(p11);
        priorWidthSpinner[c][row].setEditor(new JSpinner.NumberEditor(priorWidthSpinner[c][row], fitFormat));
        priorWidthSpinner[c][row].setBorder(grayBorder);
        priorWidthSpinner[c][row].setPreferredSize(spinnerSize);
        priorWidthSpinner[c][row].setMaximumSize(spinnerSize);
        priorWidthSpinner[c][row].setComponentPopupMenu(priorWidthStepPopup[row]);
        if (row < 2 && !isDetrend) {
            priorWidthSpinner[c][row].setToolTipText("<html>If 'Use' is enabled, the 'Prior Width' value constrains the parameter to be in<br>" + "the range of the 'Prior Center' value plus/minus the 'Prior Width' value.<br>" + "---------------------------------------------------<br>" + "To set a custom value, either disable 'Auto Update Priors'<br>" + "or enable 'Lock' to 'Prior Center'.<br>" + "---------------------------------------------------<br>" + "Right click to set spinner stepsize</html>");
        } else {
            priorWidthSpinner[c][row].setToolTipText("<html>If 'Use' is enabled, the 'Prior Width' value constrains the parameter to be in<br>" + "the range of the 'Prior Center' value plus/minus the 'Prior Width' value.<br>" + "---------------------------------------------------<br>" + "Right click to set spinner stepsize</html>");
        }
        priorWidthSpinner[c][row].addChangeListener(ev -> {
            priorWidth[c][row] = (Double) priorWidthSpinner[c][row].getValue();
            fitStepSpinner[c][row].setValue(getFitStep(c, row));
            if (autoUpdateFit[c]) updatePlot(updateOneFit(c));
        });
        priorWidthSpinner[c][row].addMouseWheelListener(e -> {
            double newValue = (Double) priorWidthSpinner[c][row].getValue() - e.getWheelRotation() * priorWidthStep[row];
            if (priorWidthSpinner[c][row].isEnabled() && newValue >= 0.0) priorWidthSpinner[c][row].setValue(newValue);
        });
        parentPanel.add(priorWidthSpinner[c][row]);

        JLabel dummyLabel2 = new JLabel();
        dummyLabel2.setFont(p12);
//            rowNameLabel.setToolTipText(rowNameToolTipText);
        dummyLabel2.setPreferredSize(spacerSize);
        dummyLabel2.setMaximumSize(spacerSize);
        parentPanel.add(dummyLabel2);

        useCustomFitStepCB[c][row] = new JCheckBox("", useCustomFitStep[c][row]);
        useCustomFitStepCB[c][row].setEnabled(isDetrend ? useFitDetrendCB[c][row - 7].isSelected() : useTransitFit[c]);
        useCustomFitStepCB[c][row].setHorizontalAlignment(JLabel.CENTER);
        useCustomFitStepCB[c][row].setPreferredSize(checkBoxSize);
        useCustomFitStepCB[c][row].setMaximumSize(checkBoxSize);
        useCustomFitStepCB[c][row].setFont(p11);
        useCustomFitStepCB[c][row].setToolTipText("<html>Enable to enter a custom minimization stepsize.<br>" + "Disable for automatic determination of minimizer stepsize.<br>" + "The minimizer is used to find the best light curve fit.<html>");
        useCustomFitStepCB[c][row].addItemListener(e -> {
            if (e.getStateChange() == ItemEvent.DESELECTED) {
                useCustomFitStep[c][row] = false;
            } else if (e.getStateChange() == ItemEvent.SELECTED) {
                useCustomFitStep[c][row] = true;
            }
            fitStepSpinner[c][row].setEnabled((isDetrend ? useFitDetrendCB[c][row - 7].isSelected() : useTransitFit[c]) && useCustomFitStep[c][row]);
            fitStepSpinner[c][row].setValue(getFitStep(c, row));
            if (autoUpdateFit[c]) updatePlot(updateOneFit(c));
        });
        parentPanel.add(useCustomFitStepCB[c][row]);

        fitStepSpinner[c][row] = new JSpinner(new SpinnerNumberModel(getFitStep(c, row), 0.0, null, fitStepStep[row]));
        fitStepSpinner[c][row].setEnabled((isDetrend ? useFitDetrendCB[c][row - 7].isSelected() : useTransitFit[c]) && useCustomFitStep[c][row]);
        fitStepSpinner[c][row].setFont(p11);
        fitStepSpinner[c][row].setEditor(new JSpinner.NumberEditor(fitStepSpinner[c][row], fitFormat));
        fitStepSpinner[c][row].setBorder(grayBorder);
        fitStepSpinner[c][row].setPreferredSize(spinnerSize);
        fitStepSpinner[c][row].setMaximumSize(spinnerSize);
        fitStepSpinner[c][row].setComponentPopupMenu(fitStepStepPopup[row]);
        fitStepSpinner[c][row].setToolTipText("<html>If enabled, the user can set a custom NelderMead minimization stepsize.<br>" + "If disabled, AIJ will automatically determination the minimizer stepsize.<br>" + "The minimizer is used to find the best light curve fit.</html>");
        fitStepSpinner[c][row].addChangeListener(ev -> {
            if (useCustomFitStep[c][row]) fitStep[c][row] = (Double) fitStepSpinner[c][row].getValue();
            if (autoUpdateFit[c]) updatePlot(updateOneFit(c));
        });
        fitStepSpinner[c][row].addMouseWheelListener(e -> {
            double newValue = (Double) fitStepSpinner[c][row].getValue() - e.getWheelRotation() * fitStepStep[row];
            if (useCustomFitStep[c][row] && fitStepSpinner[c][row].isEnabled() && newValue > 0) {
                fitStepSpinner[c][row].setValue(newValue);
            }
        });
        synchronized (parentPanel.getTreeLock()) {
            parentPanel.add(fitStepSpinner[c][row]);

            JLabel dummyLabel4 = new JLabel("");
            parentPanel.add(dummyLabel4);
        }
    }

    static void
    setFittedParametersBorderColor(final int c, final Border border) {
        if (bestFitLabel[c][0] != null) {
            for (int p = 0; p < bestFitLabel[c].length; p++) {
                if (bestFitLabel[c][p].getText() == null || lockToCenter[c][p] || bestFitLabel[c][p].getText().equals("")) {
                    bestFitLabel[c][p].setBorder(grayBorder);
                } else if (bestFitLabel[c][p].getText().equals("constant var")) {
                    bestFitLabel[c][p].setBorder(failedBorder);
                } else if (bestFitLabel[c][p].getText().equals("all NaNs")) {
                    bestFitLabel[c][p].setBorder(failedBorder);
                } else { bestFitLabel[c][p].setBorder(border); }
//                    bestFitLabel[c][p].paint(bestFitLabel[c][p].getGraphics());
            }
            t14Label[c].setBorder(useTransitFit[c] ? border : grayBorder);
//                t14Label[c].paint(t14Label[c].getGraphics());
            t14HoursLabel[c].setBorder(useTransitFit[c] ? border : grayBorder);
//                t14HoursLabel[c].paint(t14HoursLabel[c].getGraphics());
            t23Label[c].setBorder(useTransitFit[c] ? border : grayBorder);
//                t23Label[c].paint(t23Label[c].getGraphics());
            tauLabel[c].setBorder(useTransitFit[c] ? border : grayBorder);
            stellarDensityLabel[c].setBorder(useTransitFit[c] ? border : grayBorder);
            bpSpinner[c].setBorder(useTransitFit[c] && bpLock[c] ? border : grayBorder);
            planetRadiusLabel[c].setBorder(useTransitFit[c] ? border : grayBorder);
            transitDepthLabel[c].setBorder(useTransitFit[c] ? border : grayBorder);
//                tauLabel[c].paint(tauLabel[c].getGraphics());
            chi2dofLabel[c].setBorder(border);
//                chi2dofLabel[c].paint(chi2dofLabel[c].getGraphics());
            bicLabel[c].setBorder(border);
//                bicLabel[c].paint(bicLabel[c].getGraphics());
            sigmaLabel[c].setBorder(border);
//                sigmaLabel[c].paint(sigmaLabel[c].getGraphics());
            dofLabel[c].setBorder(border);
//                dofLabel[c].paint(dofLabel[c].getGraphics());
            chi2Label[c].setBorder(border);
//                chi2Label[c].paint(chi2Label[c].getGraphics());
            stepsTakenLabel[c].setBorder(border);
            //sigmaLabel[c].setBackground(defaultBackgroundColor);
        }
    }

    static void setFittedParametersBorderColorNewThread(final int c, final Border border) {
        Thread t = new Thread(() -> setFittedParametersBorderColor(c, border));
        t.start();
        Thread.yield();
    }

    static void setRMSBICBackground(final int c) {
        if (bestFitLabel[c][0] != null) {
            //IJ.log("refStarChanged="+refStarChanged+"   detrendParChanged="+detrendParChanged);
            if (refStarChanged && (sigma[c] < prevSigma[c])) {
                sigmaLabel[c].setBackground(new Color(0, 235, 0));  //light green
            }
            if (!refStarChanged && detrendParChanged) {
                if (bic[c] < prevBic[c] - 10.0) bicLabel[c].setBackground(Color.magenta);
                else if (bic[c] < prevBic[c] - 2.0) bicLabel[c].setBackground(new Color(0, 235, 0));  //light green
                else if (bic[c] < prevBic[c]) bicLabel[c].setBackground(Color.yellow);
            }
            IJ.wait(200);
            refStarChanged = false;
            detrendParChanged = false;
            sigmaLabel[c].setBackground(defaultBackground);
            bicLabel[c].setBackground(defaultBackground);
        }
    }

    static void setRMSBICBackgroundNewThread(final int c) {
        Thread t = new Thread(() -> setRMSBICBackground(c));
        t.start();
        Thread.yield();
    }

    static double getFitStep(int c, int row) {
        return getFitStep(c, row, priorWidth[c], priorCenter[c]);
    }

    static double getFitStep(int c, int row, double[] width, double[] center) {
        return useCustomFitStep[c][row] ? fitStep[c][row] > 0.0 ? fitStep[c][row] : defaultFitStep[row] : (usePriorWidth[c][row] && width[row] > 0.0 ? (width[row] * 0.9 < center[row] ? width[row] * 0.9 : center[row] > 0.0 ? center[row] : defaultFitStep[row]) : (center[row] < defaultFitStep[row] && center[row] > 0.0 ? center[row] : defaultFitStep[row]));
    }

    static void enableTransitComponents(int c) {
        extractPriorsButton[c].setEnabled(useTransitFit[c]);
        autoUpdatePriorsCB[c].setEnabled(useTransitFit[c]);
        t14Label[c].setEnabled(useTransitFit[c]);
        t14HoursLabel[c].setEnabled(useTransitFit[c]);
        t23Label[c].setEnabled(useTransitFit[c]);
        tauLabel[c].setEnabled(useTransitFit[c]);
        stellarDensityLabel[c].setEnabled(useTransitFit[c]);
        bpSpinner[c].setEnabled(useTransitFit[c] && bpLock[c]);
        bpLockCB[c].setEnabled(useTransitFit[c]);
        transitDepthLabel[c].setEnabled(useTransitFit[c]);
        planetRadiusLabel[c].setEnabled(useTransitFit[c]);
        showResidualCB[c].setEnabled(useTransitFit[c]);
        residualModelColorSelection[c].setEnabled(useTransitFit[c] && showResidual[c] && showModel[c]);
        residualLineWidthSpinner[c].setEnabled(useTransitFit[c] && showResidual[c] && showModel[c]);
        residualColorSelection[c].setEnabled(useTransitFit[c] && showResidual[c]);
        residualSymbolSelection[c].setEnabled(useTransitFit[c] && showResidual[c]);
        residualShiftSpinner[c].setEnabled(useTransitFit[c] && showResidual[c]);
        showLTranParamsCB[c].setEnabled(useTransitFit[c] && showModel[c]);
        showLResidualCB[c].setEnabled(useTransitFit[c] && showResidual[c]);
        showResidualErrorCB[c].setEnabled(useTransitFit[c] && showResidual[c]);
        for (int row = 0; row < 7; row++)  //priorCenterSpinner[c].length
        {
            bestFitLabel[c][row].setEnabled(useTransitFit[c]);
            lockToCenterCB[c][row].setEnabled(row == 4 && bpLock[c] ? false : useTransitFit[c]);
            priorCenterSpinner[c][row].setEnabled(row == 4 && bpLock[c] ? false : useTransitFit[c] && (row > 4 || !autoUpdatePriors[c] || !autoUpdatePrior[c][row] || lockToCenter[c][row]));
            usePriorWidthCB[c][row].setEnabled(useTransitFit[c]);
            priorWidthSpinner[c][row].setEnabled(useTransitFit[c] && usePriorWidth[c][row] && (row > 1 || !autoUpdatePriors[c] || !autoUpdatePrior[c][row] || lockToCenter[c][row]));
            useCustomFitStepCB[c][row].setEnabled(useTransitFit[c]);
            fitStepSpinner[c][row].setEnabled(useTransitFit[c] && useCustomFitStep[c][row]);
        }
        if (bestFitLabel[c].length > 7) {
            for (int row = 7; row < bestFitLabel[c].length; row++)  //priorCenterSpinner[c].length
            {
                bestFitLabel[c][row].setEnabled(useFitDetrendCB[c][row - 7].isSelected());
                lockToCenterCB[c][row].setEnabled(useFitDetrendCB[c][row - 7].isSelected());
                priorCenterSpinner[c][row].setEnabled(useFitDetrendCB[c][row - 7].isSelected());
                usePriorWidthCB[c][row].setEnabled(useFitDetrendCB[c][row - 7].isSelected());
                priorWidthSpinner[c][row].setEnabled(useFitDetrendCB[c][row - 7].isSelected() && usePriorWidth[c][row]);
                useCustomFitStepCB[c][row].setEnabled(useFitDetrendCB[c][row - 7].isSelected());
                fitStepSpinner[c][row].setEnabled(useFitDetrendCB[c][row - 7].isSelected() && useCustomFitStep[c][row]);
            }
        }
    }

    static void updateSaturatedStars() {
        if (table == null) return;
        if (isStarSaturated == null || isStarSaturated.length < 1 || isStarSaturated.length != numAps) return;
        int numRows = table.getCounter();
        if (numRows < 1) return;
        showSaturationWarning = Prefs.get(Aperture_.AP_PREFS_SHOWSATWARNING, true);
        saturationWarningLevel = Prefs.get(Aperture_.AP_PREFS_SATWARNLEVEL, 55000);
        showLinearityWarning = Prefs.get(Aperture_.AP_PREFS_SHOWLINWARNING, true);
        linearityWarningLevel = Prefs.get(Aperture_.AP_PREFS_LINWARNLEVEL, 30000);
        int peakcol = ResultsTable.COLUMN_NOT_FOUND;
        double value;
        for (int ap = 0; ap < numAps; ap++) {
            isStarSaturated[ap] = false;
            isStarOverLinear[ap] = false;
            if (showSaturationWarning || showLinearityWarning) {
                peakcol = table.getColumnIndex("Peak_" + (isRefStar[ap] ? "C" : "T") + (ap + 1));
                if (peakcol != ResultsTable.COLUMN_NOT_FOUND) {
                    for (int row = 0; row < numRows; row++) {
                        value = table.getValueAsDouble(peakcol, row);
                        if (showSaturationWarning && value >= saturationWarningLevel) isStarSaturated[ap] = true;
                        if (showLinearityWarning && value >= linearityWarningLevel) isStarOverLinear[ap] = true;
                    }
                    refStarCB[ap].setBackground(showSaturationWarning && isStarSaturated[ap] ? Color.RED : (showLinearityWarning && isStarOverLinear[ap] ? darkYellow : darkGreen));
                } else {
                    refStarCB[ap].setBackground(defaultCBColor);
                }
            } else {
                refStarCB[ap].setBackground(defaultCBColor);
            }
        }
    }

    static void rebuildRefStarJPanel() {
        allNonePanel.validate();
        SpringUtil.makeCompactGrid(allNonePanel, 1, allNonePanel.getComponentCount(), 2, 2, 2, 2);
        starsPanel.validate();
        if (numAps / refStarHorzWidth > 0) {
            SpringUtil.makeCompactGrid(starsPanel, starsPanel.getComponentCount() / refStarHorzWidth, refStarHorzWidth, 4, 10, 4, 10);
        } else { SpringUtil.makeCompactGrid(starsPanel, 1, starsPanel.getComponentCount(), 4, 10, 4, 10); }
        spacerPanel.validate();
        SpringUtil.makeCompactGrid(spacerPanel, spacerPanel.getComponentCount(), 1, 0, 0, 0, 0);
        starsPlusSpacerPanel.validate();
        SpringUtil.makeCompactGrid(starsPlusSpacerPanel, 1, starsPlusSpacerPanel.getComponentCount(), 0, 0, 0, 0);
        refStarBPanel.validate();
        SpringUtil.makeCompactGrid(refStarBPanel, refStarBPanel.getComponentCount(), 1, 6, 6, 6, 6);
        refStarMainPanel.validate();
        SpringUtil.makeCompactGrid(refStarMainPanel, refStarMainPanel.getComponentCount(), 1, 6, 6, 6, 6);
        refStarFrame.pack();
        refStarFrame.setVisible(true);
    }

    static void showRefStarJPanel() {
        if (refStarFrame == null) {
            refStarFrame = new JFrame("Multi-plot Reference Star Settings");
        }
        refStarFrame.setIconImage(plotIcon.getImage());
        refStarMainPanel = new JPanel(new SpringLayout());
//                mainsubpanel.addMouseListener(panelMouseListener);
//                refStarMainPanel.addMouseMotionListener(panelMouseMotionListener);
//                FileDrop fileDrop = new FileDrop(refStarMainPanel, BorderFactory.createEmptyBorder(),new FileDrop.Listener()
//                    {   public void filesDropped( java.io.File[] files )
//                        {
//                        openDragAndDropFiles(files);
//                        }
//                    });

        refStarScrollPane = new JScrollPane(refStarMainPanel);
        refStarFrame.setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        refStarFrame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                refStarPanelWasShowing = false;
                closeRefStarFrame();
            }
        });

        multiUpdate = false;
        showSaturationWarning = Prefs.get(Aperture_.AP_PREFS_SHOWSATWARNING, showSaturationWarning);
        saturationWarningLevel = Prefs.get(Aperture_.AP_PREFS_SATWARNLEVEL, saturationWarningLevel);
        showLinearityWarning = Prefs.get(Aperture_.AP_PREFS_SHOWLINWARNING, showLinearityWarning);
        linearityWarningLevel = Prefs.get(Aperture_.AP_PREFS_LINWARNLEVEL, linearityWarningLevel);

        JPanel refStarAPanel = new JPanel(new SpringLayout());
        refStarAPanel.setBorder(BorderFactory.createTitledBorder(""));
        JLabel refstarlabel1 = new JLabel("No table has been selected in 'Multi-plot Main'.");
        if (table != null) {
            refstarlabel1 = new JLabel("Select reference stars to include in tot_C_cnts and rel_flux calculations");
            refstarlabel1.setToolTipText("<html>Updates T's and C's in the table column headings and the following table values:<br>" + "rel_flux_Xx, rel_flux_err_Xx, rel_flux_SNR_Xx, tot_C_cnts, tot_C_err, </html>");
        }
        refStarAPanel.add(refstarlabel1);
        JPanel forceMagDisplayPanel = new JPanel(new SpringLayout());
        JButton forceMagDisplayButton = new JButton("Show Magnitudes");
        forceMagDisplayButton.setToolTipText("Forces the display of reference star apparent magnitudes");
        forceMagDisplayButton.addActionListener(e -> {
            boolean needsPanelUpdate = false;
            for (int ap = 0; ap < numAps; ap++) {
                if (!refStarPanel[ap].isAncestorOf(absMagTF[ap])) {
                    refStarPanel[ap].add(absMagTF[ap], -1);
                    refStarPanel[ap].validate();
                    SpringUtil.makeCompactGrid(refStarPanel[ap], refStarPanel[ap].getComponentCount(), 1, 0, 0, 0, 0);
                    needsPanelUpdate = true;
                }
            }
            if (needsPanelUpdate) rebuildRefStarJPanel();
        });
        forceMagDisplayPanel.add(forceMagDisplayButton);
        JButton forceNoMagDisplayButton = new JButton("Hide Magnitudes");
        forceNoMagDisplayButton.setToolTipText("Removes the display of reference star apparent magnitudes");
        forceNoMagDisplayButton.addActionListener(e -> {
            boolean needsPanelUpdate = false;
            for (int ap = 0; ap < numAps; ap++) {
                if (refStarPanel[ap].isAncestorOf(absMagTF[ap])) {
                    refStarPanel[ap].remove(absMagTF[ap]);
                    refStarPanel[ap].validate();
                    SpringUtil.makeCompactGrid(refStarPanel[ap], refStarPanel[ap].getComponentCount(), 1, 0, 0, 0, 0);
                    needsPanelUpdate = true;
                }
            }
            if (needsPanelUpdate) rebuildRefStarJPanel();
        });
        forceMagDisplayPanel.add(forceNoMagDisplayButton);

        SpringUtil.makeCompactGrid(forceMagDisplayPanel, 1, forceMagDisplayPanel.getComponentCount(), 6, 6, 6, 6);
        refStarAPanel.add(forceMagDisplayPanel);

        SpringUtil.makeCompactGrid(refStarAPanel, refStarAPanel.getComponentCount(), 1, 6, 6, 6, 6);
        refStarMainPanel.add(refStarAPanel);

        refStarBPanel = new JPanel(new SpringLayout());
        refStarBPanel.setBorder(BorderFactory.createTitledBorder("Reference Star Selection"));

        checkAndLockTable();

        numAps = 0;
        while (true) {
            if (table.getColumnIndex("Source-Sky_C" + (numAps + 1)) != MeasurementTable.COLUMN_NOT_FOUND || table.getColumnIndex("Source-Sky_T" + (numAps + 1)) != MeasurementTable.COLUMN_NOT_FOUND) {
                numAps++;
            } else {
                break;
            }
        }
        if (lastRefStar >= numAps) lastRefStar = numAps - 1;
        if (lastRefStar < 0) lastRefStar = 0;
        if (numAps > 0) {
            refStarPanel = new JPanel[numAps];
            refStarLabel = new JLabel[numAps];
            refStarCB = new JCheckBox[numAps];
            absMagTF = new JTextField[numAps];
            isRefStar = new boolean[numAps];
            absMag = new double[numAps];
            hasAbsMag = false;
            isStarSaturated = new boolean[numAps];
            isStarOverLinear = new boolean[numAps];
            allNonePanel = new JPanel(new SpringLayout());
            JButton noneButton = new JButton("None");
            noneButton.setToolTipText("Removes all stars from the reference star set");
            noneButton.addActionListener(e -> {
                multiUpdate = true;
                for (int r = 1; r < numAps; r++) {
                    refStarCB[r].setSelected(false);
                }
                multiUpdate = false;
                cycleEnabledStarsLess1PressedConsecutive = false;
                updatePlotEnabled = false;
                waitForPlotUpdateToFinish();
                checkAndLockTable();
                updateTotals();
                updateGUI();
                updatePlotEnabled = true;
                if (table != null) table.setLock(false);
                table.show();
                updatePlot(updateAllFits());
            });
            allNonePanel.add(noneButton);
            JButton allButton = new JButton("All");
            allButton.setToolTipText("Adds all stars to the reference star set");
            allButton.addActionListener(e -> {
                if (numAps < 2) return;
                multiUpdate = true;
                for (int r = 1; r < numAps; r++) {
                    refStarCB[r].setSelected(true);
                }
                multiUpdate = false;
                cycleEnabledStarsLess1PressedConsecutive = false;
                updatePlotEnabled = false;
                waitForPlotUpdateToFinish();
                checkAndLockTable();
                updateTotals();
                updateGUI();
                updatePlotEnabled = true;
                if (table != null) table.setLock(false);
                table.show();
                updatePlot(updateAllFits());
            });
            allNonePanel.add(allButton);

            JButton setButton = new JButton("Save");
            setButton.setToolTipText("Sets the current enabled stars as the 'cycle' set, and saves them for use in 'Recall.'");
            setButton.addActionListener(e -> {
                cycleEnabledStarsLess1PressedConsecutive = false;
                saveCompEnsemble();
            });
            allNonePanel.add(setButton);

            JButton recallButton = new JButton("Recall");
            recallButton.setToolTipText("Sets the current enabled stars to match the stars that were 'Saved.'");
            recallButton.addActionListener(e -> loadCompEnsemble());
            allNonePanel.add(recallButton);

            JButton cycleEnabledStarsLess1Button = new JButton("Cycle Enabled Stars Less One");
            cycleEnabledStarsLess1Button.setToolTipText("Removes one star at a time from the current selected set");
            cycleEnabledStarsLess1Button.addActionListener(e -> {

                if (numAps > 2) {
                    int numEnabled = 0;
                    for (int i = 1; i < numAps; i++) {
                        if (isRefStar[i]) {
                            numEnabled++;
                        }
                    }
                    if (numEnabled < (cycleEnabledStarsLess1PressedConsecutive ? 1 : 2)) {
                        return;
                    }

                    multiUpdate = true;
                    boolean foundOne = false;
                    int startingRefStar = lastRefStar;
                    if (cycleEnabledStarsLess1PressedConsecutive) {
                        lastRefStar = (lastRefStar + 1) % (numAps);
                        if (lastRefStar < 1) lastRefStar = 1;
                    }
                    if (!isRefStar[lastRefStar]) {
                        for (int i = lastRefStar; i < numAps; i++) {
                            if (isRefStar[i]) {
                                lastRefStar = i;
                                foundOne = true;
                                break;
                            }
                        }
                        if (!foundOne) {
                            for (int i = 1; i < lastRefStar; i++) {
                                if (isRefStar[i]) {
                                    lastRefStar = i;
                                    foundOne = true;
                                    break;
                                }
                            }
                        }
                    } else {
                        foundOne = true;
                    }

                    if (foundOne) {
                        if (cycleEnabledStarsLess1PressedConsecutive) {
                            refStarCB[startingRefStar].setSelected(true);
                        }
                        refStarCB[lastRefStar].setSelected(false);
                        multiUpdate = false;
                        cycleEnabledStarsLess1PressedConsecutive = true;
                        updatePlotEnabled = false;
                        waitForPlotUpdateToFinish();
                        checkAndLockTable();
                        updateTotals();
                        updateGUI();
                        updatePlotEnabled = true;
                        if (table != null) table.setLock(false);
                        table.show();
                        updatePlot(updateAllFits());
                    }
                }
            });
            allNonePanel.add(cycleEnabledStarsLess1Button);

            JButton cycleIndividualStarButton = new JButton("Cycle Individual Stars");
            cycleIndividualStarButton.setToolTipText("Cycles through all stars considering each as an individual reference star");
            cycleIndividualStarButton.addActionListener(e -> {
                if (numAps < 2) return;
                multiUpdate = true;
                for (int r = 1; r < numAps; r++) {
                    refStarCB[r].setSelected(r == lastRefStar);
                }
                lastRefStar = (lastRefStar + 1) % (numAps);
                if (lastRefStar < 1) lastRefStar = 1;
                multiUpdate = false;
                cycleEnabledStarsLess1PressedConsecutive = false;
                updatePlotEnabled = false;
                waitForPlotUpdateToFinish();
                checkAndLockTable();
                updateTotals();
                updateGUI();
                updatePlotEnabled = true;
                if (table != null) table.setLock(false);
                table.show();
                updatePlot(updateAllFits());
            });
            allNonePanel.add(cycleIndividualStarButton);

            JLabel dummy1Label = new JLabel("");
            allNonePanel.add(dummy1Label);
            SpringUtil.makeCompactGrid(allNonePanel, 1, allNonePanel.getComponentCount(), 2, 2, 2, 2);
            refStarBPanel.add(allNonePanel);

            starsPlusSpacerPanel = new JPanel(new SpringLayout());

            starsPanel = new JPanel(new SpringLayout());
            int absMagCol = -1;
            numAbsMagRefAps = 0;
            for (int r = 0; r < numAps; r++) {
                absMag[r] = 99.999;
                if (table.getColumnIndex("Source-Sky_C" + (r + 1)) != MeasurementTable.COLUMN_NOT_FOUND) {
                    refStarLabel[r] = new JLabel("C" + (r + 1));
                    isRefStar[r] = true;
                    refStarLabel[r].setForeground(Color.RED);
                    absMagCol = table.getColumnIndex("Source_AMag_C" + (r + 1));
                    if (absMagCol != MeasurementTable.COLUMN_NOT_FOUND) {
                        absMag[r] = table.getValueAsDouble(absMagCol, 0);
                        if (absMag[r] < 99.0) {
                            hasAbsMag = true;
                            numAbsMagRefAps++;
                        }
                    }
                } else if (table.getColumnIndex("Source-Sky_T" + (r + 1)) != MeasurementTable.COLUMN_NOT_FOUND) {
                    refStarLabel[r] = new JLabel("T" + (r + 1));
                    isRefStar[r] = false;
                    refStarLabel[r].setForeground(darkGreen);
                } else {
                    IJ.beep();
                    IJ.showMessage("Error getting apertures from table");
                    closeRefStarFrame();
                    if (table != null) table.setLock(false);
                    return;
                }
            }
            for (int r = 0; r < numAps; r++) {
                refStarPanel[r] = new JPanel(new SpringLayout());
                refStarLabel[r].setHorizontalAlignment(JLabel.CENTER);
                refStarLabel[r].setFont(b11);
                refStarLabel[r].setMaximumSize(new Dimension(25, 25));
                refStarPanel[r].add(refStarLabel[r]);
                refStarCB[r] = new JCheckBox("", isRefStar[r]);
                refStarCB[r].setHorizontalAlignment(SwingConstants.CENTER);
                absMagTF[r] = new JTextField(absMag[r] < 99.9 ? onetoThreePlaces.format(absMag[r]) : "", 5);
                if (isRefStar[r]) {
                    absMagTF[r].setToolTipText("<html>Enter known magnitude and press ENTER to recalculate target star magnitudes.<br>" + "Right click to open SIMBAD target(s) near coordinates" + " (requires RA_C" + (r + 1) + " and Dec_C" + (r + 1) + " columns in table.");
                } else {
                    absMagTF[r].setToolTipText("<html>Right click to open SIMBAD target(s) near coordinates" + " (requires RA_T" + (r + 1) + " and Dec_T" + (r + 1) + " columns in table.");
                }
                absMagTF[r].setHorizontalAlignment(JTextField.CENTER);
                absMagTF[r].setEditable(isRefStar[r]);
                setUpAbsMagTFListener(r);
                setupRefStarCBListener(r);
                refStarPanel[r].add(refStarCB[r]);
                if (hasAbsMag || forceAbsMagDisplay) {
                    refStarPanel[r].add(absMagTF[r]);
                }
                SpringUtil.makeCompactGrid(refStarPanel[r], refStarPanel[r].getComponentCount(), 1, 0, 0, 0, 0);
                starsPanel.add(refStarPanel[r]);
            }
            defaultCBColor = refStarCB[0].getBackground();

            if (numAps / refStarHorzWidth > 0) {
                int fill = refStarHorzWidth - numAps % refStarHorzWidth;
                if (fill != 0 && fill != refStarHorzWidth) {
                    JPanel[] dummyPanel = new JPanel[fill];
                    for (int i = 0; i < fill; i++) {
                        dummyPanel[i] = new JPanel();
                        dummyPanel[i].setMaximumSize(refStarPanel[0].getSize());
                        starsPanel.add(dummyPanel[i]);
                    }
                }
                SpringUtil.makeCompactGrid(starsPanel, starsPanel.getComponentCount() / refStarHorzWidth, refStarHorzWidth, 4, 10, 4, 10);
            } else {
                SpringUtil.makeCompactGrid(starsPanel, 1, starsPanel.getComponentCount(), 4, 10, 4, 10);
            }
            starsPlusSpacerPanel.add(starsPanel);

            spacerPanel = new JPanel(new SpringLayout());
            JLabel spacerDummyLabel = new JLabel("");
            spacerPanel.add(spacerDummyLabel);

            SpringUtil.makeCompactGrid(spacerPanel, spacerPanel.getComponentCount(), 1, 0, 0, 0, 0);
            starsPlusSpacerPanel.add(spacerPanel);

            SpringUtil.makeCompactGrid(starsPlusSpacerPanel, 1, starsPlusSpacerPanel.getComponentCount(), 0, 0, 0, 0);
            refStarBPanel.add(starsPlusSpacerPanel);

            JPanel legendPanel = new JPanel(new SpringLayout());

            JLabel unsaturatedCBLabel = new JLabel("Green checkbox border - aperture peak count under " + (showLinearityWarning ? "linearity limit" : "saturation limit"));
            JLabel overLinearityCBLabel = new JLabel("Yellow checkbox border - aperture peak count over linearity limit");
            JLabel overSaturationCBLabel = new JLabel("Red checkbox border - aperture peak count over saturation limit");
            if (showSaturationWarning || showLinearityWarning) legendPanel.add(unsaturatedCBLabel);
            if (showLinearityWarning) legendPanel.add(overLinearityCBLabel);
            if (showSaturationWarning) legendPanel.add(overSaturationCBLabel);
            SpringUtil.makeCompactGrid(legendPanel, legendPanel.getComponentCount(), 1, 6, 6, 6, 6);
            refStarBPanel.add(legendPanel);

            SpringUtil.makeCompactGrid(refStarBPanel, refStarBPanel.getComponentCount(), 1, 6, 6, 6, 6);
            refStarMainPanel.add(refStarBPanel);

            JPanel reminderPanel = new JPanel();
            reminderPanel.setBorder(BorderFactory.createTitledBorder("Save/Show Current Configuration"));
            reminderPanel.setLayout(new BoxLayout(reminderPanel, BoxLayout.LINE_AXIS));

            JButton saveTableButton = new JButton("Save Table");
            saveTableButton.setForeground(Color.RED);
            saveTableButton.setToolTipText("Save the measurement table with current reference star configuration");
            saveTableButton.addActionListener(e -> saveData());
            reminderPanel.add(saveTableButton);

            reminderPanel.add(Box.createHorizontalStrut(10));

            JButton saveApsButton = new JButton("Save Apertures");
//                    sendApsButton.setForeground(Color.RED);
            saveApsButton.setToolTipText("Save new aperture configuration to file");
            saveApsButton.addActionListener(e -> saveApertures());
            reminderPanel.add(saveApsButton);

            reminderPanel.add(Box.createHorizontalStrut(10));

            JButton sendApsButton = new JButton("Send to Multi-aperture");
//                    sendApsButton.setForeground(Color.RED);
            sendApsButton.setToolTipText("Send new aperture configuration to Multi-aperture");
            sendApsButton.addActionListener(e -> storeApertures());
            reminderPanel.add(sendApsButton);

            reminderPanel.add(Box.createHorizontalStrut(10));

            JButton showApsButton = new JButton("Show Apertures");
//                    sendApsButton.setForeground(Color.RED);
            showApsButton.setToolTipText("Show apertures in active image");
            showApsButton.addActionListener(e -> showApertures());
            reminderPanel.add(showApsButton);
//                    JLabel reminderlabel= new JLabel("   ***** Save table to retain reference star configuration *****");
//                    reminderlabel.setForeground(Color.RED);
//                    reminderPanel.add (reminderlabel);

            reminderPanel.add(Box.createGlue());
            updateSaturatedStars();
            refStarMainPanel.add(reminderPanel);
            if (table != null) table.setLock(false);
            table.show();
        } else {
            JLabel refstarlabel2 = new JLabel("No 'Source-Sky' columns found in table.");
            refStarBPanel.add(refstarlabel2);

            JLabel refstarlabel3 = new JLabel("Enable 'Integrated Counts (Source-Sky)' in aperture settings.");
            refStarBPanel.add(refstarlabel3);
            SpringUtil.makeCompactGrid(refStarBPanel, refStarBPanel.getComponentCount(), 1, 6, 6, 6, 6);
            refStarMainPanel.add(refStarBPanel);
            if (table != null) table.setLock(false);
        }


        SpringUtil.makeCompactGrid(refStarMainPanel, refStarMainPanel.getComponentCount(), 1, 6, 6, 6, 6);

        refStarFrame.add(refStarScrollPane);
        refStarFrame.pack();
        refStarFrame.setResizable(true);

        if (rememberWindowLocations) {
            IJU.setFrameSizeAndLocation(refStarFrame, refStarFrameLocationX, refStarFrameLocationY, 0, 0);
        }
        UIHelper.recursiveFontSetter(refStarFrame, p11);
        refStarFrame.pack();
        refStarFrame.setVisible(true);
        refStarPanelWasShowing = true;
        FileDrop fileDrop = new FileDrop(refStarMainPanel, BorderFactory.createEmptyBorder(), MultiPlot_::openDragAndDropFiles);

    }

    public static void saveCompEnsemble() {
        if (isRefStar == null) return;
        savedIsRefStar = Arrays.copyOf(isRefStar, isRefStar.length);
    }

    public static void loadCompEnsemble() {
        for (int r = 0; r < savedIsRefStar.length; r++) {
            refStarCB[r].setSelected(savedIsRefStar[r]);
        }
    }


    /** Returns an ImageIcon, or null if the path was invalid. */
    static protected ImageIcon createImageIcon(String path, String description) {
        MultiPlot_ m = new MultiPlot_();
        java.net.URL imgURL = m.getClass().getClassLoader().getResource(path);
        if (imgURL != null) {
            return new ImageIcon(imgURL, description);
        } else {
            IJ.log("Couldn't find icon file: " + path);
            return null;
        }
    }

    static void setupRefStarCBListener(final int r) {
        refStarCB[r].addItemListener(e -> {
            if ((table == null || table.getCounter() < 1) && refStarCB[r].isSelected() != isRefStar[r]) {
                refStarCB[r].setSelected(isRefStar[r]);
                absMagTF[r].setEditable(isRefStar[r]);
                return;
            }

            boolean foundApertureInImage = false;
            ImagePlus imp = WindowManager.getCurrentImage();
            ImageWindow iw = null;
            if (imp != null) {
                iw = imp.getWindow();
                if (iw instanceof AstroStackWindow) {
                    foundApertureInImage = updateImageOverlay(imp, e.getStateChange() == ItemEvent.SELECTED, r, true);
                }
            }
            if (!foundApertureInImage && WindowManager.getWindowCount() > 1) {
                int[] ID = WindowManager.getIDList();
                for (int i : ID) {
                    imp = WindowManager.getImage(i);
                    if (imp != null) { iw = imp.getWindow(); } else iw = null;
                    if (iw instanceof AstroStackWindow) {
                        foundApertureInImage = updateImageOverlay(imp, e.getStateChange() == ItemEvent.SELECTED, r, true);
                        if (foundApertureInImage) {
                            break;
                        }
                    }
                }
            }
            refStarChanged = true;
            for (int curve = 0; curve < maxCurves; curve++){
                prevSigma[curve] = sigma[curve];
            }
            if (e.getStateChange() == ItemEvent.DESELECTED) {
                isRefStar[r] = false;
                refStarLabel[r].setText("T" + (r + 1));
                refStarLabel[r].setForeground(darkGreen);
            } else if (e.getStateChange() == ItemEvent.SELECTED) {
                isRefStar[r] = true;
                refStarLabel[r].setText("C" + (r + 1));
                refStarLabel[r].setForeground(Color.RED);
            }
            absMagTF[r].setEditable(isRefStar[r]);
            updatePlotEnabled = false;
            waitForPlotUpdateToFinish();
            checkAndLockTable();

            updateTable(isRefStar[r], r);
            if (!multiUpdate) {
                lastRefStar = r;
                cycleEnabledStarsLess1PressedConsecutive = false;
                updateTotals();
                updateGUI();
                table.show();
                updatePlotEnabled = true;
                updatePlot(updateAllFits());
            }
            updatePlotEnabled = true;
            if (table != null) table.setLock(false);
            setWaitingForPlot(false);
        });
    }

    static void setUpAbsMagTFListener(final int a) {
        if (table == null || table.getCounter() < 1) return;

        absMagTF[a].addActionListener(e -> {
            updatePlotEnabled = false;
            waitForPlotUpdateToFinish();
            checkAndLockTable();
            updateAbsMags(a);
            table.show();
            updatePlotEnabled = true;
            if (table != null) table.setLock(false);
            updatePlot(updateAllFits());
        });

        absMagTF[a].addMouseListener(new MouseListener() {
            public void mouseClicked(MouseEvent e) {}

            public void mousePressed(MouseEvent e) {}

            public void mouseReleased(MouseEvent e) {
                if (table == null || table.getCounter() < 1) return;
                if ((e.getModifiers() & MouseEvent.BUTTON3_MASK) != 0) {
                    int col = table.getColumnIndex("RA_" + (isRefStar[a] ? "C" : "T") + (a + 1));
                    if (col != ResultsTable.COLUMN_NOT_FOUND) {
                        double ra = table.getValueAsDouble(col, 0);
                        col = table.getColumnIndex("DEC_" + (isRefStar[a] ? "C" : "T") + (a + 1));
                        if (col != ResultsTable.COLUMN_NOT_FOUND) {
                            double dec = table.getValueAsDouble(col, 0);
                            if (!Double.isNaN(ra) && !Double.isNaN(dec)) {
                                AstroStackWindow asw = IJU.getBestOpenAstroStackWindow();
                                double searchRadius = 10; //default to 10 arcsec
                                if (asw != null) searchRadius = asw.getSIMBADSearchRadius();

                                IJU.showInSIMBAD(ra * 15.0, dec, searchRadius);
                            }
                        } else {
                            IJ.beep();
                            IJ.showMessage("Error Accessing SIMBAD", "<html>Table column " + "DEC_" + (isRefStar[a] ? "C" : "T") + (a + 1) + " not found.<br>" + "Original images must have WCS headers (i.e. be plate solved) and <br>" + "'World coordinates (RA, Dec)' must be enabled in 'Set Aperture' 'More Settings'.</html>");
                        }
                    } else {
                        IJ.beep();
                        IJ.showMessage("Error Accessing SIMBAD", "<html>Table column " + "RA_" + (isRefStar[a] ? "C" : "T") + (a + 1) + " not found.<br>" + "Original images must have WCS headers (i.e. be plate solved) and <br>" + "'World coordinates (RA, Dec)' must be enabled in 'Set Aperture' 'More Settings'.</html>");

                    }


                }
            }

            public void mouseEntered(MouseEvent e) {}

            public void mouseExited(MouseEvent e) {}
        });
    }

    static void updateAbsMags(int aper) {
        int numRows = table.getCounter();
        if (aper >= 0 && isRefStar[aper]) {
            absMag[aper] = Tools.parseDouble(absMagTF[aper].getText(), 99.999);
            if (absMag[aper] >= 99.0) absMag[aper] = 99.999;
            silenceAbsMagTF = true;
            absMagTF[aper].setText(absMag[aper] < 99.0 ? onetoThreePlaces.format(absMag[aper]) : "");
            for (int row = 0; row < numRows; row++) {
                table.setValue("Source_AMag_C" + (aper + 1), row, absMag[aper]);
            }
        }
        if (aper < 0) {
            for (int ap = 0; ap < numAps; ap++) {
                silenceAbsMagTF = true;
                absMagTF[ap].setText(isRefStar[ap] && absMag[ap] < 99.0 ? onetoThreePlaces.format(absMag[ap]) : "");
            }
        }

        hasAbsMag = false;
        double totAbsMag = 0.0;
        numAbsMagRefAps = 0;
        for (int ap = 0; ap < numAps; ap++) {
            if (isRefStar[ap] && absMag[ap] < 99.0) {
                numAbsMagRefAps++;
                hasAbsMag = true;
                totAbsMag += Math.pow(2.512, -absMag[ap]);
            }
        }
        if (hasAbsMag) {
            boolean hasErr = true;
            totAbsMag = -Math.log(totAbsMag) / Math.log(2.512);
            int[] srcCol = new int[numAps];
            int[] srcVarCol = new int[numAps];
            for (int ap = 0; ap < numAps; ap++) {
                srcCol[ap] = table.getColumnIndex("Source-Sky_" + (isRefStar[ap] ? "C" : "T") + (ap + 1));
                if (srcCol[ap] == ResultsTable.COLUMN_NOT_FOUND) {
                    IJ.showMessage("Table Error", "<html>Table column 'Source-Sky_" + (isRefStar[ap] ? "C" : "T") + (ap + 1) + " does not exist.<br>" + "Can not calculate new target apparent magnitudes.</html>");
                    return;
                }
                srcVarCol[ap] = table.getColumnIndex("Source_Error_" + (isRefStar[ap] ? "C" : "T") + (ap + 1));
                if (srcVarCol[ap] == ResultsTable.COLUMN_NOT_FOUND) {
                    hasErr = false;
                }
            }
            double totAbsRefVar;
            double totAbsRefCnts;
            double[] src = new double[numAps];
            double sourceErr, srcAbsErr, mag, absRatio;
            double[] srcVar = new double[numAps];
            for (int row = 0; row < numRows; row++) {
                totAbsRefVar = 0.0;
                totAbsRefCnts = 0.0;
                for (int ap = 0; ap < numAps; ap++) {
                    if (!isRefStar[ap] || (isRefStar[ap] && absMag[ap] < 99.0)) {
                        src[ap] = table.getValueAsDouble(srcCol[ap], row);
                        if (hasErr) {
                            sourceErr = table.getValueAsDouble(srcVarCol[ap], row);
                            srcVar[ap] = sourceErr * sourceErr;
                        }
                        if (isRefStar[ap]) {
                            totAbsRefCnts += src[ap];
                            if (hasErr) totAbsRefVar += srcVar[ap];
                        }
                    }
                }
                for (int ap = 0; ap < numAps; ap++) {
                    if (!isRefStar[ap]) {
                        absRatio = src[ap] / totAbsRefCnts;
                        table.setValue("Source_AMag_T" + (ap + 1), row, totAbsMag - 2.5 * Math.log10(absRatio));
                        if (hasErr) {
                            srcAbsErr = absRatio * Math.sqrt(srcVar[ap] / (src[ap] * src[ap]) + totAbsRefVar / (totAbsRefCnts * totAbsRefCnts));
                            table.setValue("Source_AMag_Err_T" + (ap + 1), row, 2.5 * Math.log10(1.0 + srcAbsErr / absRatio));
                        }
                    } else {
                        table.setValue("Source_AMag_C" + (ap + 1), row, absMag[ap]);
                        if (hasErr) {
                            table.setValue("Source_AMag_Err_C" + (ap + 1), row, 2.5 * Math.log10(1.0 + Math.sqrt(srcVar[ap]) / src[ap]));
                        }
                    }
                }
            }
        }

        ImagePlus imp = IJU.getBestOpenAstroImage();
        if (imp != null) {
            for (int ap = 0; ap < numAps; ap++) {
                updateImageOverlay(imp, isRefStar[ap], ap, ap == numAps - 1);
            }
        }
    }


    static boolean updateImageOverlay(ImagePlus imp, boolean selected, int r, boolean repaint) {
        OverlayCanvas ocanvas = OverlayCanvas.getOverlayCanvas(imp);
        if (ocanvas != null) {
            ApertureRoi aRoi = ocanvas.findApertureRoiByNumber(r);
            if (aRoi != null) {
                if (!selected) {
                    aRoi.setName("T" + (r + 1));
                    aRoi.setApColor(Color.GREEN);
                    int col = table.getColumnIndex("Source_AMag_T" + (r + 1));
                    if (col != ResultsTable.COLUMN_NOT_FOUND && hasAbsMag) {
                        //IJ.log("Setting ROI "+(selected?"C":"T")+(r+1)+" = "+table.getValueAsDouble(col, 0));
                        aRoi.setAMag(table.getValueAsDouble(col, 0));
                    } else {
                        aRoi.setAMag(99.999);
                    }
                } else {
                    aRoi.setName("C" + (r + 1));
                    aRoi.setApColor(Color.RED);
                    aRoi.setAMag(absMag[r]);
                }
                if (repaint) ocanvas.repaint();
                return true;
            }
        }
        return false;
    }

    static void updateTable(boolean comp, int r) {
        if (table == null || table.getCounter() < 1) return;
        int tablen = table.getLastColumn();
        String newSuffix = "_T" + (r + 1);
        String oldSuffix = "_C" + (r + 1);
        if (comp) {
            newSuffix = "_C" + (r + 1);
            oldSuffix = "_T" + (r + 1);
        }
        int suflen = oldSuffix.length();
        String heading;
        int headlen;
        for (int col = 0; col <= tablen; col++) {
            heading = table.getColumnHeading(col);
            headlen = heading.length();
            if (heading.endsWith(oldSuffix)) {
                table.setHeading(col, heading.substring(0, headlen - suflen) + newSuffix);
            }
        }
        if (xlabeldefault.endsWith(oldSuffix)) {
            xlabeldefault = xlabeldefault.substring(0, xlabeldefault.length() - suflen) + newSuffix;
        }
        for (int c = 0; c < maxCurves; c++) {
            if (xlabel[c].endsWith(oldSuffix)) {
                xlabel[c] = xlabel[c].substring(0, xlabel[c].length() - suflen) + newSuffix;
            }
            if (ylabel[c].endsWith(oldSuffix)) {
                ylabel[c] = ylabel[c].substring(0, ylabel[c].length() - suflen) + newSuffix;
            }
            if (oplabel[c].endsWith(oldSuffix)) {
                oplabel[c] = oplabel[c].substring(0, oplabel[c].length() - suflen) + newSuffix;
            }
            for (int v = 0; v < maxDetrendVars; v++) {
                if (detrendlabel[c][v].endsWith(oldSuffix)) {
                    detrendlabel[c][v] = detrendlabel[c][v].substring(0, detrendlabel[c][v].length() - suflen) + newSuffix;
                }
                if (detrendlabelhold[c][v].endsWith(oldSuffix)) {
                    detrendlabelhold[c][v] = detrendlabelhold[c][v].substring(0, detrendlabelhold[c][v].length() - suflen) + newSuffix;
                }
            }
        }
    }


    static void updateTotals() {

        if (table == null) return;
        int numRows = table.getCounter();
        if (numRows < 1) return;
        boolean goodErrData = true;
        double[][] source = new double[numAps][numRows];
        double[][] srcvar = new double[numAps][numRows];
        double[][] total = new double[numAps][numRows];
        double[][] totvar = new double[numAps][numRows];
        int totCcntAP = -1;
        for (int ap = 0; ap < numAps; ap++) {
            if (!isRefStar[ap]) {
                totCcntAP = ap;
                break;
            }
        }
        for (int ap = 0; ap < numAps; ap++) {
            for (int row = 0; row < numRows; row++) {
                source[ap][row] = 0.0;
                total[ap][row] = 0.0;
                srcvar[ap][row] = 0.0;
                totvar[ap][row] = 0.0;
            }
        }
        double value;
        double errval;
        double ratio;
        double factor;
        double oneOverFactor;
        int col = ResultsTable.COLUMN_NOT_FOUND;
        int errcol = ResultsTable.COLUMN_NOT_FOUND;
        int snrcol = ResultsTable.COLUMN_NOT_FOUND;
        int peakcol = ResultsTable.COLUMN_NOT_FOUND;

        for (int ap = 0; ap < numAps; ap++) {
            col = table.getColumnIndex("Source-Sky_" + (isRefStar[ap] ? "C" : "T") + (ap + 1));
            if (col == ResultsTable.COLUMN_NOT_FOUND) {
                IJ.beep();
                IJ.showMessage("Error: could not find data column 'Source-Sky_" + (isRefStar[ap] ? "C" : "T") + (ap + 1) + "'");
                return;
            } else {
                errcol = table.getColumnIndex("Source_Error_" + (isRefStar[ap] ? "C" : "T") + (ap + 1));
                if (errcol == ResultsTable.COLUMN_NOT_FOUND) {
                    goodErrData = false;
                }
                for (int row = 0; row < numRows; row++) {
                    value = table.getValueAsDouble(col, row);
                    source[ap][row] = value;
                    if (goodErrData) {
                        errval = table.getValueAsDouble(errcol, row);
                        srcvar[ap][row] = errval * errval;
                    }
                    if (isRefStar[ap]) {
                        for (int i = 0; i < numAps; i++) {
                            if (i != ap) {
                                total[i][row] += value;
                                totvar[i][row] += srcvar[ap][row];
                            }
                        }
                    }
                }
            }
        }
        if (numAps > 1) {
            for (int ap = 0; ap < numAps; ap++) {
                col = table.getColumnIndex("rel_flux_" + (isRefStar[ap] ? "C" : "T") + (ap + 1));
                if (col == ResultsTable.COLUMN_NOT_FOUND) {
                    col = table.getFreeColumn("rel_flux_" + (isRefStar[ap] ? "C" : "T") + (ap + 1));
                    if (col == ResultsTable.COLUMN_NOT_FOUND) {
                        IJ.beep();
                        IJ.showMessage("Error: could not create data column 'rel_flux_" + (isRefStar[ap] ? "C" : "T") + (ap + 1) + "'");
                        return;
                    }
                }
                if (goodErrData) {
                    errcol = table.getColumnIndex("rel_flux_err_" + (isRefStar[ap] ? "C" : "T") + (ap + 1));
                    if (errcol == ResultsTable.COLUMN_NOT_FOUND) {
                        errcol = table.getFreeColumn("rel_flux_err_" + (isRefStar[ap] ? "C" : "T") + (ap + 1));
                        if (errcol == ResultsTable.COLUMN_NOT_FOUND) {
                            IJ.beep();
                            IJ.showMessage("Error: could not create data column 'rel_flux_err_" + (isRefStar[ap] ? "C" : "T") + (ap + 1) + "'");
                            return;
                        }
                    }
                    snrcol = table.getColumnIndex("rel_flux_SNR_" + (isRefStar[ap] ? "C" : "T") + (ap + 1));
                    if (snrcol == ResultsTable.COLUMN_NOT_FOUND) {
                        snrcol = table.getFreeColumn("rel_flux_SNR_" + (isRefStar[ap] ? "C" : "T") + (ap + 1));
                        if (snrcol == ResultsTable.COLUMN_NOT_FOUND) {
                            IJ.beep();
                            IJ.showMessage("Error: could not create data column 'rel_flux_SNR_" + (isRefStar[ap] ? "C" : "T") + (ap + 1) + "'");
                            return;
                        }
                    }
                }

                for (int row = 0; row < numRows; row++) {
                    ratio = total[ap][row] == 0 ? 0 : source[ap][row] / total[ap][row];
                    table.setValue(col, row, ratio);
                    if (goodErrData) {
                        if (source[ap][row] == 0 || total[ap][row] == 0) {
                            factor = 0;
                            oneOverFactor = 0;
                        } else {
                            factor = Math.sqrt(srcvar[ap][row] / (source[ap][row] * source[ap][row]) + totvar[ap][row] / (total[ap][row] * total[ap][row]));
                            //ratio[ap]*Math.sqrt(srcVar[ap]/(src[ap]*src[ap])+ totVar[ap]/(tot[ap]*tot[ap]))
                            oneOverFactor = 1.0 / factor;
                        }
                        table.setValue(errcol, row, ratio * factor);
                        table.setValue(snrcol, row, oneOverFactor);
                    }
                }
            }

            for (int row = 0; row < numRows; row++) {
                table.setValue("tot_C_cnts", row, totCcntAP < 0 ? 0.0 : total[totCcntAP][row]);
                table.setValue("tot_C_err", row, totCcntAP < 0 ? 0.0 : Math.sqrt(totvar[totCcntAP][row]));
            }
        }

        updateAbsMags(-1);

        StringBuilder isRefString = new StringBuilder();   //update prefs to reflect new ref/target star designations
        for (int ap = 0; ap < numAps; ap++) {
            if (ap == 0) {
                isRefString.append(isRefStar[ap]);
            } else {
                isRefString.append(",").append(isRefStar[ap]);
            }
        }
        Prefs.set(MultiAperture_.PREFS_ISREFSTAR, isRefString.toString());
    }

    static void updateGUI() {
        if (table == null || table.getCounter() < 1) return;
        unfilteredColumns = table.getColumnHeadings().split("\t");
        if (unfilteredColumns.length == 0) {
            IJ.showMessage("No data columns in table.");
            makeDummyTable();
            closeRefStarFrame();
        } else {
            updateColumnLists();
            oldUnfilteredColumns = unfilteredColumns.clone();
            xdatacolumndefault.setModel(new DefaultComboBoxModel<>(columns));
            xdatacolumndefault.setSelectedItem(xlabeldefault);
            xdatacolumndefault.repaint();
            for (int c = 0; c < maxCurves; c++) {
                xdatacolumn[c].setModel(new DefaultComboBoxModel<>(columnswd));
                xdatacolumn[c].setSelectedItem(xlabel[c]);
                disableUpdatePlotBox = true;   //disable automatic enable of plot when a new data column has been selected
                ydatacolumn[c].setModel(new DefaultComboBoxModel<>(columns));
                ydatacolumn[c].setSelectedItem(ylabel[c]);
                disableUpdatePlotBox = false;
                operatorcolumn[c].setModel(new DefaultComboBoxModel<>(columns));
                operatorcolumn[c].setSelectedItem(oplabel[c]);
                detrendbox[c].setModel(new DefaultComboBoxModel<>(columnsDetrend));
                detrendbox[c].setSelectedItem(detrendlabel[c][detrendVarDisplayed[c]]);

                for (int d = 0; d < maxDetrendVars; d++) {
                    if (fitDetrendComboBox[c][d] != null) {
                        fitDetrendComboBox[c][d].setModel(new DefaultComboBoxModel<>(columnsDetrend));
                        if (detrendIndex[c][d] == 0 && !detrendlabelhold[c][d].trim().equals("")) {
                            fitDetrendComboBox[c][d].setEnabled(false);
                            fitDetrendComboBox[c][d].setSelectedItem(detrendlabelhold[c][d].trim());
                            fitDetrendComboBox[c][d].setEnabled(true);
                        } else {
                            fitDetrendComboBox[c][d].setEnabled(false);
                            fitDetrendComboBox[c][d].setSelectedItem(detrendlabel[c][d]);
                            fitDetrendComboBox[c][d].setEnabled(true);
                        }
                    }
                }
            }
        }
    }

    static void saveApertures() {
        storeApertures();
        SaveDialog sf = new SaveDialog("Save apertures", MeasurementTable.shorterName(table.shortTitle()), "");
        if (sf.getDirectory() == null || sf.getFileName() == null) return;
        String apsPath = sf.getDirectory() + sf.getFileName();
        int location = apsPath.lastIndexOf('.');
        if (location >= 0) apsPath = apsPath.substring(0, location);
        IJU.saveApertures(apsPath + ".apertures");
    }

    static void storeApertures() {
        int numRows = table.getCounter();
        if (numRows < 1) return;

        checkAndLockTable();

        double[] X = new double[numAps];
        double[] Y = new double[numAps];
        double[] aMag = new double[numAps];
        String xColMiddle = "(IJ)_";
        String yColMiddle = "(IJ)_";
        boolean isXFITS = false;
        boolean isYFITS = false;
        int col;

        for (int ap = 0; ap < numAps; ap++) {
            X[ap] = 0.0;
            Y[ap] = 0.0;
        }

        col = table.getColumnIndex("X(IJ)_" + (isRefStar[0] ? "C1" : "T1"));
        if (col == ResultsTable.COLUMN_NOT_FOUND) {
            col = table.getColumnIndex("X_" + (isRefStar[0] ? "C1" : "T1"));
            if (col == ResultsTable.COLUMN_NOT_FOUND) {
                col = table.getColumnIndex("X(FITS)_" + (isRefStar[0] ? "C1" : "T1"));
                if (col != ResultsTable.COLUMN_NOT_FOUND) {
                    xColMiddle = "(FITS)_";
                    isXFITS = true;
                }
            } else {
                xColMiddle = "_";
            }
        }
        col = table.getColumnIndex("Y(IJ)_" + (isRefStar[0] ? "C1" : "T1"));
        if (col == ResultsTable.COLUMN_NOT_FOUND) {
            col = table.getColumnIndex("Y_" + (isRefStar[0] ? "C1" : "T1"));
            if (col == ResultsTable.COLUMN_NOT_FOUND) {
                col = table.getColumnIndex("Y(FITS)_" + (isRefStar[0] ? "C1" : "T1"));
                if (col != ResultsTable.COLUMN_NOT_FOUND) {
                    yColMiddle = "(FITS)_";
                    isYFITS = true;
                }
            } else {
                yColMiddle = "_";
            }
        }


        for (int ap = 0; ap < numAps; ap++) {
            col = table.getColumnIndex("X" + xColMiddle + (isRefStar[ap] ? "C" : "T") + (ap + 1));
            if (col != ResultsTable.COLUMN_NOT_FOUND) {
                X[ap] = table.getValueAsDouble(col, 0);
            }
            col = table.getColumnIndex("Y" + yColMiddle + (isRefStar[ap] ? "C" : "T") + (ap + 1));
            if (col != ResultsTable.COLUMN_NOT_FOUND) {
                Y[ap] = table.getValueAsDouble(col, 0);
            }
            aMag[ap] = 99.999;
            if (isRefStar[ap]) {
                col = table.getColumnIndex("Source_AMag_C" + (ap + 1));
                if (col != ResultsTable.COLUMN_NOT_FOUND) {
                    aMag[ap] = table.getValueAsDouble(col, 0);
                }
            }
        }

        StringBuilder xString = new StringBuilder();
        StringBuilder yString = new StringBuilder();
        StringBuilder absMagString = new StringBuilder();
        StringBuilder isRefString = new StringBuilder();

        for (int ap = 0; ap < numAps; ap++) {
            if (ap == 0) {
                xString.append(isXFITS ? "FITS" : "").append((float) X[ap]);
                yString.append(isYFITS ? "FITS" : "").append((float) Y[ap]);
                absMagString.append((float) absMag[ap]);
                isRefString.append(isRefStar[ap]);
            } else {
                xString.append(",").append((float) X[ap]);
                yString.append(",").append((float) Y[ap]);
                absMagString.append(",").append((float) absMag[ap]);
                isRefString.append(",").append(isRefStar[ap]);
            }
        }


        Prefs.set(MultiAperture_.PREFS_PREVIOUS, true);
        Prefs.set(MultiAperture_.PREFS_XAPERTURES, xString.toString());
        Prefs.set(MultiAperture_.PREFS_YAPERTURES, yString.toString());
        Prefs.set(MultiAperture_.PREFS_ABSMAGAPERTURES, absMagString.toString());
        Prefs.set(MultiAperture_.PREFS_ISREFSTAR, isRefString.toString());

        col = table.getColumnIndex(Aperture_.AP_FWHMMULT);
        if (col != ResultsTable.COLUMN_NOT_FOUND) {
            Prefs.set(MultiAperture_.PREFS_USEVARSIZEAP, true);
            Prefs.set(MultiAperture_.PREFS_APFWHMFACTOR, table.getValueAsDouble(col, 0));
            col = table.getColumnIndex(Aperture_.AP_BRSOURCE);
            if (col != ResultsTable.COLUMN_NOT_FOUND) {
                Prefs.set(Aperture_.AP_PREFS_RADIUS, table.getValueAsDouble(col, 0));
            }
        } else {
            col = table.getColumnIndex(Aperture_.AP_RSOURCE);
            if (col != ResultsTable.COLUMN_NOT_FOUND) {
                Prefs.set(MultiAperture_.PREFS_USEVARSIZEAP, false);
                Prefs.set(Aperture_.AP_PREFS_RADIUS, table.getValueAsDouble(col, 0));
            }
        }

        col = table.getColumnIndex(Aperture_.AP_RBACK1);
        if (col == ResultsTable.COLUMN_NOT_FOUND) {
            col = table.getColumnIndex("Sky_Radius(min)");
            if (col != ResultsTable.COLUMN_NOT_FOUND) {
                Prefs.set(Aperture_.AP_PREFS_RBACK1, table.getValueAsDouble(col, 0));
            }
        } else {
            Prefs.set(Aperture_.AP_PREFS_RBACK1, table.getValueAsDouble(col, 0));
        }

        col = table.getColumnIndex(Aperture_.AP_RBACK2);
        if (col == ResultsTable.COLUMN_NOT_FOUND) {
            col = table.getColumnIndex("Sky_Radius(max)");
            if (col != ResultsTable.COLUMN_NOT_FOUND) {
                Prefs.set(Aperture_.AP_PREFS_RBACK2, table.getValueAsDouble(col, 0));
            }
        } else {
            Prefs.set(Aperture_.AP_PREFS_RBACK2, table.getValueAsDouble(col, 0));
        }
        if (table != null) table.setLock(false);
    }


    static void showApertures() {
        storeApertures();
        ImagePlus iplus = WindowManager.getCurrentImage();
        ImageWindow iw = null;
        AstroStackWindow asw = null;
        if (iplus != null) {
            iw = iplus.getWindow();
            if (iw instanceof AstroStackWindow) {
                asw = (AstroStackWindow) iw;
                WindowManager.toFront(iw);
                asw.openApertures("");
            } else if (WindowManager.getWindowCount() > 0) {
                int[] ID = WindowManager.getIDList();
                for (int i : ID) {
                    iplus = WindowManager.getImage(i);
                    if (iplus != null) { iw = iplus.getWindow(); } else iw = null;
                    if (iw instanceof AstroStackWindow) {
                        asw = (AstroStackWindow) iw;
                        WindowManager.setCurrentWindow(iw);
                        WindowManager.toFront(iw);
                        asw.openApertures("");
                        break;
                    }
                }
            }
        }

    }

    public static void waitForPlotUpdateToFinish() {
        int cnt = 0;  //timeout after 1 second
        while (cnt < 10 && (updatePlotRunning || awaitingScheduledPlotUpdate)) {
            IJ.wait(100);
            cnt++;
//                if (cnt == 10)
//                    {
//                    IJ.log("NOTICE: timed out waiting on plot to finish update");
//                    }
        }
        updatePlotRunning = false;
    }

    static void checkAndLockTable() {
        if (table == null) return;
        int cnt = 0;  //timeout after 1 second
        while (table != null && table.isLocked() && cnt < 10) {
            IJ.wait(100);
            cnt++;
//                if (cnt == 10)
//                    {
//                    IJ.log("NOTICE: timed out waiting on table to unlock");
//                    }
        }
        if (table != null) table.setLock(true);
    }

    static void closeRefStarFrame() {
//            updatePlotEnabled = true;
        if (refStarFrame.isShowing()) {
            refStarFrameLocationX = refStarFrame.getLocation().x;
            refStarFrameLocationY = refStarFrame.getLocation().y;
            refStarFrame.setVisible(false);
            Prefs.set("plot2.refStarFrameLocationX", refStarFrameLocationX);
            Prefs.set("plot2.refStarFrameLocationY", refStarFrameLocationY);
        }
//            if (table != null) table.setLock(false);

        refStarFrame.dispose();
        refStarFrame = null;
    }

    static void closeFitFrames() {
        int waitCnt = 3;
        while (panelsUpdating && waitCnt > 0) {
            IJ.wait(1000);
            waitCnt--;
        }
//            IJ.log("Start Close Fit Frames");
        if (fitFrame != null) {
            for (int c = 0; c < fitFrame.length; c++) {
                //                IJ.log("Start close of fitFrame["+c+"]");
                if (fitFrame[c] != null) {
                    //                    IJ.log("fitFrame["+c+"] != null");
                    if (fitFrame[c].isVisible()) {
                        fitFrameLocationX[c] = fitFrame[c].getLocation().x;
                        fitFrameLocationY[c] = fitFrame[c].getLocation().y;
                        fitFrame[c].setVisible(false);
                        Prefs.set("plot2.fitFrameLocationX" + c, fitFrameLocationX[c]);
                        Prefs.set("plot2.fitFrameLocationY" + c, fitFrameLocationY[c]);
                    }
                    //                    IJ.log("Disposing fitFrame["+c+"]");
                    fitFrame[c].dispose();
                    //                    IJ.log("Disposed fitFrame["+c+"]");
                    fitFrame[c] = null;
                    //                    IJ.log("fitFrame["+c+"] is null");
                }
            }
        }
    }

    static void closeAddAstroDataFrame() {
        astroConverterUpdating = false;
        if (addAstroDataFrame == null) return;
        if (addAstroDataFrame.isShowing()) {
            addAstroDataFrameLocationX = addAstroDataFrame.getLocation().x;
            addAstroDataFrameLocationY = addAstroDataFrame.getLocation().y;
            addAstroDataFrame.setVisible(false);
            Prefs.set("plot2.addAstroDataFrameLocationX", addAstroDataFrameLocationX);
            Prefs.set("plot2.addAstroDataFrameLocationY", addAstroDataFrameLocationY);
        }
        addAstroDataFrame.dispose();
        addAstroDataFrame = null;
    }

    static void setVMarkerText() {
        GenericDialog gd = new GenericDialog("Set Vertical Marker Text", mainFrame.getX() + mainFrame.getWidth() / 2 - 165, mainFrame.getY() + mainFrame.getHeight() / 2 - 77);
        gd.addStringField("Vertical Marker 1 Text - Line 1:", vMarker1TopText, 40);
        gd.addStringField("Vertical Marker 1 Text - Line 2:", vMarker1BotText, 40);
        gd.addMessage("");
        gd.addStringField("Vertical Marker 2 Text - Line 1:", vMarker2TopText, 40);
        gd.addStringField("Vertical Marker 2 Text - Line 2:", vMarker2BotText, 40);
        gd.addMessage("");
        gd.showDialog();
        if (gd.wasCanceled()) return;
        vMarker1TopText = gd.getNextString();
        vMarker1BotText = gd.getNextString();
        vMarker2TopText = gd.getNextString();
        vMarker2BotText = gd.getNextString();
        Prefs.set("plot.vMarker1TopText", vMarker1TopText);
        Prefs.set("plot.vMarker1BotText", vMarker1BotText);
        Prefs.set("plot.vMarker2TopText", vMarker2TopText);
        Prefs.set("plot.vMarker2BotText", vMarker2BotText);
    }

    static void changePixelScale() {
        GenericDialog gd = new GenericDialog("Change Pixel Scale", mainFrame.getX() + mainFrame.getWidth() / 2 - 165, mainFrame.getY() + mainFrame.getHeight() / 2 - 77);

        gd.addNumericField("Pixel scale: ", pixelScale, 4, 8, "(seconds of arc per pixel)");
        gd.addMessage("");

        gd.showDialog();
        if (gd.wasCanceled()) return;

        pixelScale = gd.getNextNumber();
        Prefs.set("plot.pixelScale", pixelScale);
        changepixelscalemenuitem.setText("Change pixel scale (" + pixelScale + ")...");
    }

    static void setEphemeris() {
        GenericDialog gd = new GenericDialog("Set Ref. Epoch and Period for x-Axis Phase Display", mainFrame.getX() + mainFrame.getWidth() / 2 - 165, mainFrame.getY() + mainFrame.getHeight() / 2 - 77);

        gd.addNumericField("Reference Epoch: ", T0, 8, 20, "(days)");
        gd.addNumericField("Orbital Period: ", period, 8, 20, "(days)");
        gd.addNumericField("Transit Duration: ", duration, 8, 20, "(hours)");
        gd.addMessage("");

        gd.showDialog();
        if (gd.wasCanceled()) return;

        T0 = gd.getNextNumber();
        if (T0 < 8000.0) T0 += 2457000;
        period = gd.getNextNumber();
        duration = gd.getNextNumber();
        vMarker1Value = -duration / 48.0;
        vmarker1spinner.setValue(vMarker1Value);
        vMarker2Value = duration / 48.0;
        vmarker2spinner.setValue(vMarker2Value);
        dMarker2Value = -duration / 48.0;
        dmarker2spinner.setValue(dMarker2Value);
        dMarker3Value = duration / 48.0;
        dmarker3spinner.setValue(dMarker3Value);
        Prefs.set("plot.T0", T0);
        Prefs.set("plot.period", period);
        Prefs.set("plot.duration", duration);
        Prefs.set("plot.vMarker1Value", vMarker1Value);
        Prefs.set("plot.vMarker2Value", vMarker2Value);
    }


    static void changeMaxDataLength() {
        GenericDialog gd = new GenericDialog("Change minimum data column length", mainFrame.getX() + mainFrame.getWidth() / 2 - 165, mainFrame.getY() + mainFrame.getHeight() / 2 - 77);

        gd.addMessage("The default length is 1000, but can be made smaller or larger\n" + "in order to optimize your system's speed and memory utilization.\n" + "The length will automatically double if the set size is reached, however\n" + "plot updates will be slightly delayed while memory is being reconfigured.");

        gd.addNumericField("Length: ", maxColumnLength, 0, 10, "(measurement table rows)");
        gd.addMessage("");

        gd.showDialog();
        if (gd.wasCanceled()) return;

        maxColumnLength = (int) gd.getNextNumber();
        setupDataBuffers();
        Prefs.set("plot.maxColumnLength", maxColumnLength);
    }


    static void changePriorityColumns() {
        GenericDialog gd = new GenericDialog("Change data name priorities in pulldown lists", mainFrame.getX() + mainFrame.getWidth() / 2 - 165, mainFrame.getY() + mainFrame.getHeight() / 2 - 77);
        gd.addMessage("Enter data names to be placed at the top of data selection pulldown lists,\n" + "in the order desired, with each name separated by a comma (case insensitive).\n" + "All data column names starting with a comma separated value will be included.");
        gd.addStringField("Data Columns:", priorityColumns, 80);
        gd.addStringField("Detrend Columns:", priorityDetrendColumns, 80);
        gd.addMessage("");

        gd.showDialog();
        if (gd.wasCanceled()) return;

        priorityColumns = gd.getNextString();
        priorityDetrendColumns = gd.getNextString();
        Prefs.set("plot.priorityColumns", priorityColumns);
        Prefs.set("plot.priorityDetrendColumns", priorityDetrendColumns);
        if (prioritizeColumns) {
            oldUnfilteredColumns = null;
            updatePlot(updateAllFits());
        }
    }

    static void changeRefStarHorizontalWidth() {
        GenericDialog gd = new GenericDialog("Change reference star window width", mainFrame.getX() + mainFrame.getWidth() / 2 - 165, mainFrame.getY() + mainFrame.getHeight() / 2 - 77);
        gd.addMessage("Enter the number of reference stars to display per\n" + "line in the reference star selection window.");
        gd.addNumericField("Stars per line:", refStarHorzWidth, 0);
        gd.addMessage("");

        gd.showDialog();
        if (gd.wasCanceled()) return;

        refStarHorzWidth = (int) gd.getNextNumber();
        if (refStarHorzWidth < 1) refStarHorzWidth = 1;
        Prefs.set("plot.refStarHorzWidth", refStarHorzWidth);
        closeRefStarFrame();
        if (refStarPanelWasShowing) showRefStarJPanel();
    }

    static void saveAndClose() {
        updatePlotEnabled = true;
        refStarChanged = false;
        detrendParChanged = false;
        panelsUpdating = false;
        if (table != null) table.setLock(false);
        closeFitFrames();
        if (mainFrame != null) {
            mainFrameLocationX = mainFrame.getLocation().x;
            mainFrameLocationY = mainFrame.getLocation().y;
            Prefs.set("plot2.mainFrameLocationX", mainFrameLocationX);
            Prefs.set("plot2.mainFrameLocationY", mainFrameLocationY);
        }
        if (subFrame != null && subFrame.isShowing()) {
            subFrameLocationX = subFrame.getLocation().x;
            subFrameLocationY = subFrame.getLocation().y;
            Prefs.set("plot2.subFrameLocationX", subFrameLocationX);
            Prefs.set("plot2.subFrameLocationY", subFrameLocationY);
        }
        if (refStarFrame != null) {
            refStarPanelWasShowing = refStarFrame.isShowing();
            closeRefStarFrame();
        }
        if (acc != null) {
            acc.saveAndClose();
        }
        if (addAstroDataFrame != null) {
            addAstroDataFrameWasShowing = addAstroDataFrame.isShowing();
            closeAddAstroDataFrame();
        }

        if (plotImage != null) {
            plotWindow = plotImage.getWindow();
            if (plotWindow != null) {
                plotFrameLocationX = plotWindow.getLocation().x;
                plotFrameLocationY = plotWindow.getLocation().y;
                Prefs.set("plot2.plotFrameLocationX", plotFrameLocationX);
                Prefs.set("plot2.plotFrameLocationY", plotFrameLocationY);
            }
        }

//                plotImageCanvas.removeMouseMotionListener(plotMouseMotionListener);
//                plotImageCanvas.removeMouseWheelListener(plotMouseWheelListener);
//                plotImageCanvas.removeMouseListener(plotMouseListener);

        savePreferences();
        if (delayedUpdateTimer != null) delayedUpdateTimer.cancel();
        if (delayedUpdateTask != null) delayedUpdateTask.cancel();
        if (table != null) table.setLock(false);
        if (mainFrame != null) mainFrame.dispose();
        if (subFrame != null) subFrame.dispose();
        mainFrame = null;
        subFrame = null;
    }

    public static synchronized void openDragAndDropFiles(java.io.File[] files) {
        int errorCode;
        dragAndDropFiles = files;
        errorCode = 1;
        if (files.length > 0 && files[0].isFile() && files[0].getName().endsWith(".plotcfg")) {
            errorCode = 2;
            OpenDialog.setDefaultDirectory(files[0].getParent());
            errorCode = 3;
            try {
                InputStream is = new BufferedInputStream(new FileInputStream(files[0].getCanonicalPath()));
                errorCode = 4;
                Prefs.ijPrefs.load(is);
                errorCode = 5;
                is.close();
                errorCode = 6;
                setupArrays();
                errorCode = 7;
                getPreferences();
                errorCode = 8;
                setTable(table, true);
                errorCode = 9;
                if (plotWindow != null && table != null) plotWindow.setVisible(true);
                errorCode = 10;
            } catch (Exception e) {
                IJ.beep();
                IJ.showMessage("DragAndDrop: Error code " + errorCode + " reading plot configuration file");
            }
        } else if (files.length > 0 && files[0].isFile() && files[0].getName().endsWith(".apertures")) {
            OpenDialog.setDefaultDirectory(files[0].getParent());
            try {
                InputStream is = new BufferedInputStream(new FileInputStream(files[0].getCanonicalPath()));
                Prefs.ijPrefs.load(is);
                is.close();
            } catch (Exception e) {
                IJ.beep();
                IJ.showMessage("DragAndDrop: Error reading aperture file");
            }
        } else if (files.length > 0 && files[0].isFile() && files[0].getName().endsWith(".radec")) {
            OpenDialog.setDefaultDirectory(files[0].getParent());
            try {
                IJU.openRaDecApertures(files[0].getCanonicalPath());
            } catch (Exception e) {
                IJ.beep();
                IJ.showMessage("DragAndDrop: Error reading RA/Dec file");
            }
        } else if (files.length > 0 && files[0].isFile() && (files[0].getName().endsWith(Prefs.get("options.ext", ".xls")) || files[0].getName().endsWith(".txt") || files[0].getName().endsWith(".csv") || files[0].getName().endsWith(".prn") || files[0].getName().endsWith(".spc") || files[0].getName().endsWith(".xls") || files[0].getName().endsWith(".dat") || files[0].getName().endsWith(".tbl"))) {

            Thread t = new Thread(() -> {
                IJ.wait(100);
                String fileName = null;
                try {fileName = dragAndDropFiles[0].getCanonicalPath();} catch (Exception e) {
                    IJ.beep();
                    IJ.showMessage("DragAndDrop: Error creating file name to read plot configuration file.");
                }
                if (panelShiftDown && fileName != null) {
                    appendDataAsRows(true, fileName);
                } else if (panelControlDown && fileName != null) {
                    appendDataAsColumns(true, fileName);
                } else if (fileName != null) {
                    MeasurementTable newTable = MeasurementTable.getTableFromFile(fileName);
                    if (newTable == null) {
                        IJ.beep();
                        IJ.showMessage("DragAndDrop: Unable to open MeasurementTable " + fileName);
                    } else {
                        newTable.show();
                        Frame ntf = WindowManager.getFrame(newTable.shortTitle());
                        if (ntf != null) ntf.setVisible(true);
                        int lastDot = fileName.lastIndexOf('.');
                        String cfgPath = lastDot > 0 ? fileName.substring(0, lastDot) + ".plotcfg" : fileName + ".plotcfg";
                        File cfgFile = null;
                        try {cfgFile = new File(cfgPath);} catch (Exception ignored) {}
                        if (cfgFile != null && cfgFile.isFile()) {
                            InputStream is = null;
                            try {is = new BufferedInputStream(new FileInputStream(cfgPath));} catch (Exception e) {
                                IJ.beep();
                                IJ.showMessage("DragAndDrop: Error operning input stream to read plot configuration file.");
                            }
                            try {Prefs.ijPrefs.load(is);} catch (Exception e) {
                                IJ.beep();
                                IJ.showMessage("DragAndDrop: Error loading plot configuration file.");
                            }
                            try {is.close();} catch (Exception e) {
                                IJ.beep();
                                IJ.showMessage("DragAndDrop: Error closing plot configuration file.");
                            }
                        }
                        setupArrays();
                        getPreferences();
                        setTable(newTable, false);
                        if (plotWindow != null) plotWindow.setVisible(true);
                    }
                }
            });
            OpenDialog.setDefaultDirectory(files[0].getParent());
            t.start();
            Thread.yield();

        }
    }

    static void openData() {
        OpenDialog of = new OpenDialog("Select measurement table to open", "");
        String path = of.getDirectory() + of.getFileName();
        if (of.getDirectory() == null || of.getFileName() == null) return;
        try {
            MeasurementTable newTable = MeasurementTable.getTableFromFile(of.getDirectory() + of.getFileName());
            if (newTable == null) {
                IJ.beep();
                IJ.showMessage("Unable to open measurement table " + path);
            } else {
                newTable.show();
                setTable(newTable, true);
                plotWindow.setVisible(true);
            }
        } catch (Exception e) {
            IJ.beep();
            IJ.showMessage("Multi-Plot: Error reading or plotting measurements table");
        }
    }

    static void transposeTable() {
        MeasurementTable newTable = new MeasurementTable(tableName + "_transposed");
        if (table.getLastColumn() < 0 || table.getCounter() < 1) {
            IJ.error("No data in active table to transpose");
            IJ.beep();
            return;
        }
        for (int row = 0; row < table.getCounter(); row++) {
            if (table.getLabel(row).equals("") || table.getLabel(row) == null) {
                if (newTable.getFreeColumn("Col_" + (row + 1)) == MeasurementTable.COLUMN_IN_USE) {
                    IJ.error("Could not make unique column labels from row labels");
                    IJ.beep();
                    return;
                }
            } else if (newTable.getFreeColumn(table.getLabel(row)) == MeasurementTable.COLUMN_IN_USE) {
                if (newTable.getFreeColumn(table.getLabel(row) + "_Col_" + (row + 1)) == MeasurementTable.COLUMN_IN_USE) {
                    IJ.error("Could not make unique column labels from row labels");
                    IJ.beep();
                    return;
                }
            }
        }
        for (int col = 0; col <= table.getLastColumn(); col++) {
            newTable.incrementCounter();
            newTable.setLabel(table.getColumnHeading(col), col);
        }
        for (int row = 0; row < table.getCounter(); row++) {
            for (int col = 0; col <= table.getLastColumn(); col++) {
                newTable.setValue(row, col, table.getValueAsDouble(col, row));
            }
        }
        newTable.show();
        setTable(newTable, true);
        plotWindow.setVisible(true);
    }


    static void appendDataAsRows(boolean fromFile, String inPath) {
        String filePath = inPath;

        if (fromFile && inPath == null) {
            OpenDialog of = new OpenDialog("Select measurement table to append as new rows", "");
            if (of.getDirectory() == null || of.getFileName() == null) return;
            filePath = of.getDirectory() + of.getFileName();
        }

        try {
            MeasurementTable newTable;
            selectAnotherTableCanceled = false;
            if (fromFile) {
                newTable = MeasurementTable.getTableFromFile(filePath);
            } else {
                newTable = selectAnotherTable(tableName);
            }
            if (newTable == null) {
                if (!selectAnotherTableCanceled) {
                    IJ.beep();
                    if (fromFile) { IJ.showMessage("Unable to open measurement table " + filePath); } else {
                        IJ.showMessage("No additional measurement tables open");
                    }
                }
            } else {
                newTable.show();

                combinedTableName = MeasurementTable.shorterName(tableName);
                if (combinedTableName.endsWith(Prefs.get("options.ext", ".xls"))) {
                    int position = combinedTableName.lastIndexOf(Prefs.get("options.ext", ".xls"));
                    if (position >= 0) combinedTableName = combinedTableName.substring(0, position);
                }
                combinedTableName += "_" + MeasurementTable.shorterName(newTable.shortTitle());

                MeasurementTable combinedTable = MeasurementTable.getTable(combinedTableName, tableName);
                String[] oldColumns = table.getColumnHeadings().split("\t");
                String[] newColumns = newTable.getColumnHeadings().split("\t");
                int oldRows = combinedTable.getCounter();
                int newRows = newTable.getCounter();

                int combinedRows = oldRows + newRows;
                int columnIndex;
                if (newColumns.length > 2 && newRows > 0) {
                    for (int i = 0; i < newRows; i++) {
                        combinedTable.incrementCounter();
                        combinedTable.addLabel(newTable.getLabel(i));
                    }

                    for (int i = 2; i < oldColumns.length; i++)  //copy all new columns common with old table
                    {
                        columnIndex = newTable.getColumnIndex(oldColumns[i]);
                        for (int j = oldRows; j < combinedRows; j++) {
                            combinedTable.setValue(oldColumns[i], j, columnIndex == ResultsTable.COLUMN_NOT_FOUND ? Double.NaN : newTable.getValueAsDouble(columnIndex, j - oldRows));
                        }
                    }
                    for (int i = 2; i < newColumns.length; i++)  //copy all new columns not common with old table
                    {
                        columnIndex = table.getColumnIndex(newColumns[i]);
                        if (columnIndex == ResultsTable.COLUMN_NOT_FOUND) {
                            for (int j = 0; j < combinedRows; j++) {
                                combinedTable.setValue(newColumns[i], j, j < oldRows ? Double.NaN : newTable.getValue(newColumns[i], j - oldRows));
                            }
                        }
                    }

                    table.show();
                    combinedTable.show();
                    setTable(combinedTable, true);
                    plotWindow.setVisible(true);
                } else if (newColumns.length < 3) {
                    IJ.beep();
                    IJ.showMessage("No new columns to append");
                } else if (newRows < 1) {
                    IJ.beep();
                    IJ.showMessage("No new rows to append");
                } else {
                    IJ.beep();
                    IJ.showMessage("Table append error");
                }
            }
        } catch (Exception e) {
            IJ.beep();
            IJ.showMessage("Append Rows: Error reading measurement table");
        }
    }

    static void appendDataAsColumns(boolean fromFile, String inPath) {
        String filePath = inPath;
        boolean uniqueHeadings = true;
        int errorCode = 1;
        if (fromFile && inPath == null) {
            OpenDialog of = new OpenDialog("Select measurement table to append as new columns", "");
            if (of.getDirectory() == null || of.getFileName() == null) return;
            filePath = of.getDirectory() + of.getFileName();
        }
        errorCode = 1;
        try {
            MeasurementTable newTable;
            selectAnotherTableCanceled = false;
            if (fromFile) {
                newTable = MeasurementTable.getTableFromFile(filePath);
            } else {
                newTable = selectAnotherTable(tableName);
            }
            errorCode = 2;
            if (newTable == null) {
                if (!selectAnotherTableCanceled) {
                    IJ.beep();
                    if (fromFile) { IJ.showMessage("Unable to open measurement table " + filePath); } else {
                        IJ.showMessage("No additional measurement tables open");
                    }
                }
            } else {
                errorCode = 3;
                newTable.show();

                combinedTableName = MeasurementTable.shorterName(tableName);
                if (combinedTableName.endsWith(Prefs.get("options.ext", ".xls"))) {
                    int position = combinedTableName.lastIndexOf(Prefs.get("options.ext", ".xls"));
                    if (position >= 0) combinedTableName = combinedTableName.substring(0, position);
                }
                combinedTableName += "_" + MeasurementTable.shorterName(newTable.shortTitle());

                if (!appendTableDialog()) return;
                MeasurementTable combinedTable = MeasurementTable.getTable(combinedTableName, tableName);
                String[] oldColumns = table.getColumnHeadings().split("\t");
                String[] newColumns = newTable.getColumnHeadings().split("\t");
                int oldRows = combinedTable.getCounter();
                int newRows = newTable.getCounter();

                errorCode = 4;
                for (int r = 0; r < oldRows; r++) {
                    if (keepFileNamesOnAppend) {
                        combinedTable.setLabel(combinedTable.getLabel(r) + (appendDestinationSuffix.equals("") ? " " : "(" + appendDestinationSuffix + ")") + (r < newRows ? " " + newTable.getLabel(r) + "(" + appendSourceSuffix + ")" : ""), r);
                    } else {
                        combinedTable.setLabel("merged", r);
                    }
                }
                errorCode = 5;
                if (newRows > oldRows) {
                    for (int r = oldRows; r < newRows; r++) {
                        combinedTable.incrementCounter();
                        if (keepFileNamesOnAppend) {
                            combinedTable.addLabel(newTable.getLabel(r) + "(" + appendSourceSuffix + ")");
                        } else { combinedTable.addLabel("merged"); }
                        for (int i = 0; i < oldColumns.length; i++) {
                            combinedTable.setValue(i, r, Double.NaN);
                        }
                    }
                }
                errorCode = 6;
                if (!appendDestinationSuffix.equals("") && (oldColumns.length > 1)) {
                    for (int col = 0; col < oldColumns.length - 2; col++) {
                        combinedTable.setHeading(col, combinedTable.getColumnHeading(col) + appendDestinationSuffix);
                    }

                    if (!xlabeldefault.equals("")) xlabeldefault += appendDestinationSuffix;
                    for (int curve = 0; curve < maxCurves; curve++) {
                        if (!xlabel[curve].equals("") && !xlabel[curve].equals("default")) {
                            xlabel[curve] += appendDestinationSuffix;
                        }
                        if (!ylabel[curve].equals("")) ylabel[curve] += appendDestinationSuffix;
                        if (!oplabel[curve].equals("")) oplabel[curve] += appendDestinationSuffix;
                        for (int v = 0; v < maxDetrendVars; v++) {
                            if (!detrendlabel[curve][v].equals("")) detrendlabel[curve][v] += appendDestinationSuffix;
                            if (!detrendlabelhold[curve][v].equals("")) {
                                detrendlabelhold[curve][v] += appendDestinationSuffix;
                            }
                        }
                    }
                }
                errorCode = 7;

                for (String newColumn : newColumns) {
                    if (combinedTable.getColumnIndex(newColumn + appendSourceSuffix) != ResultsTable.COLUMN_NOT_FOUND) {
                        uniqueHeadings = false;
                        break;
                    }
                }
                errorCode = 8;
                int combinedRows = combinedTable.getCounter();
                errorCode = 9;
                if (uniqueHeadings && newColumns.length > 2 && newRows > 0) {
                    errorCode = 10;
                    for (int i = 2; i < newColumns.length; i++) {
                        for (int j = 0; j < combinedRows; j++) {
                            if (j < newRows) {
                                combinedTable.setValue(newColumns[i] + appendSourceSuffix, j, newTable.getValue(newColumns[i], j));
                            } else { combinedTable.setValue(newColumns[i] + appendSourceSuffix, j, Double.NaN); }
                        }
                    }
                    errorCode = 11;
                    table.show();
                    errorCode = 12;
                    combinedTable.show();
                    errorCode = 13;
                    setTable(combinedTable, true);
                    errorCode = 14;
                    plotWindow.setVisible(true);
                    errorCode = 15;
                } else if (!uniqueHeadings) {
                    errorCode = 16;
                    IJ.beep();
                    IJ.showMessage("Heading names are not unique");
                } else if (newColumns.length < 3) {
                    errorCode = 17;
                    IJ.beep();
                    IJ.showMessage("No new columns to append");
                } else if (newRows < 1) {
                    errorCode = 18;
                    IJ.beep();
                    IJ.showMessage("No new rows to append");
                } else {
                    errorCode = 19;
                    IJ.beep();
                    IJ.showMessage("Table append error");
                }
            }
        } catch (Exception e) {
            IJ.beep();
            IJ.showMessage("Append Columns: Error code " + errorCode + " reading measurement table");
        }
    }

    static boolean appendTableDialog() {
        GenericDialog gd = new GenericDialog("Append settings", mainFrame.getX() + 20, mainFrame.getY() + mainFrame.getHeight() / 2 - 77);

        gd.addStringField("Enter name for combined table:", combinedTableName, 80);

        gd.addMessage("The combined dataset column headings must all be unique.\n" + "Enter suffixes to be appended to existing headings to make them unique.\n" + "Either or both of the suffix fields can left blank if the headings are already unique.\n" + "Short suffixes display better in the selection panels.");
        gd.addStringField("Original table column heading suffix:", "_", 80);
        gd.addStringField("Appended table column heading suffix:", "_", 80);
        gd.addCheckbox("Use concatenated source file names as label (label can become very long)***", keepFileNamesOnAppend);
        gd.addMessage("***if deselected, 'merged' will appear in all label columns ");

        gd.showDialog();
        if (gd.wasCanceled()) return false;

        combinedTableName = gd.getNextString().trim();
        appendDestinationSuffix = gd.getNextString().trim();
        appendSourceSuffix = gd.getNextString().trim();
        keepFileNamesOnAppend = gd.getNextBoolean();

        return true;
    }

    static MeasurementTable selectAnotherTable(String firstTableName) {
        MeasurementTable secondTable;
        String secondTableName = "";
        String[] tables = MeasurementTable.getMeasurementTableNames();
        if (tables == null || tables.length == 0) {
            return null;
        } else {
            //filter out first table and B&C, Log, Errors windows if they exist
            int j = 0;
            for (String value : tables)
                if (value.equals(firstTableName) || value.equals("B&C") || value.startsWith("Log") || value.equals("Errors")) {
                    j++;
                }
            String[] filteredTables = new String[tables.length - j];

            j = 0;
            for (String s : tables)
                if (!s.equals(firstTableName) && !s.equals("B&C") && !s.startsWith("Log") && !s.equals("Errors")) {
                    filteredTables[j] = s;
                    j++;
                }

            if (filteredTables.length == 0) {
                return null;
            } else                            // IF MORE THAN ONE, ASK WHICH TABLE SHOULD BE USED
            {
                GenericDialog gd = new GenericDialog((firstTableName.equals("") ? "Select table" : "Select table to append"), mainFrame.getX() + mainFrame.getWidth() / 2 - 165, mainFrame.getY() + mainFrame.getHeight() / 2 - 77);
                gd.addChoice((firstTableName.equals("") ? "Select table" : "Select table to append"), filteredTables, "Measurements");
                gd.showDialog();
                if (gd.wasCanceled()) {
                    selectAnotherTableCanceled = true;
                    return null;
                }
                selectAnotherTableCanceled = false;
                secondTableName = gd.getNextChoice();
            }
            secondTable = MeasurementTable.getTable(MeasurementTable.longerName(secondTableName));
            return secondTable;
        }
    }

    static void openConfig(boolean template) {
        String startingDir = OpenDialog.getDefaultDirectory();
//            String dir = OpenDialog.getLastDirectory();
        if (template && templateDir != null && !templateDir.equals("")) {
            OpenDialog.setDefaultDirectory(templateDir);
        }
        OpenDialog of = new OpenDialog("Open plot " + (template ? "template" : "configuration"), "");
        if (of.getDirectory() == null || of.getFileName() == null) {
            if (template) OpenDialog.setDefaultDirectory(startingDir);
            return;
        }
        if (template) {
            templateDir = of.getDirectory();
            OpenDialog.setDefaultDirectory(startingDir);
            Prefs.set("plot2.templateDir", templateDir);
        }
        try {
            InputStream is = new BufferedInputStream(new FileInputStream(of.getDirectory() + of.getFileName()));
            Prefs.ijPrefs.load(is);
            is.close();
            setupArrays();
            getPreferences();
            setTable(table, true);
            if (plotWindow != null && table != null) plotWindow.setVisible(true);
        } catch (Exception e) {
            IJ.beep();
            IJ.showMessage("Error reading plot configuration file");
        }
    }

    static public void openDataAndConfig(String filePath) {
        String inPath = filePath;
        if (inPath == null) {
            OpenDialog of = new OpenDialog("Select measurement table to open", "");
            if (of.getDirectory() == null || of.getFileName() == null) return;
            inPath = of.getDirectory() + of.getFileName();
        }
        MeasurementTable newTable = MeasurementTable.getTableFromFile(inPath);
        if (newTable == null) {
            IJ.beep();
            IJ.showMessage("Unable to open measurement table " + inPath);
        } else {
            newTable.show();
            int lastDot = inPath.lastIndexOf('.');
            String cfgPath = lastDot > 0 ? inPath.substring(0, lastDot) + ".plotcfg" : inPath + ".plotcfg";
            File cfgFile = null;
            try {cfgFile = new File(cfgPath);} catch (Exception e) {
                IJ.beep();
                IJ.showMessage("Open Data and Config: No plot configuration file found named: " + cfgPath);
            }
            if (cfgFile != null && cfgFile.isFile()) {
                try {
                    InputStream is = new BufferedInputStream(new FileInputStream(cfgPath));
                    Prefs.ijPrefs.load(is);
                    is.close();
                } catch (Exception e) {
                    IJ.beep();
                    IJ.showMessage("Open Data and Config: Error loading plot config file: " + cfgPath);
                }
            } else {
                IJ.beep();
                IJ.showMessage("Open Data and Config: No plot configuration file found named: " + cfgPath);
            }
            setupArrays();
            getPreferences();
            setTable(newTable, true);
            plotWindow.setVisible(true);
        }
    }

    static public void loadDataOpenConfig(MeasurementTable newTable, String filePath) {
        loadConfigOfOpenTable(filePath);
        Frame ntf = WindowManager.getFrame(newTable.shortTitle());
        if (ntf != null) ntf.setVisible(true);
        setTable(newTable, true);
        if (plotWindow != null) plotWindow.setVisible(true);
    }

    static public void loadConfigOfOpenTable(String path) {
        if (path.equals("")) {
            setupArrays();
            getPreferences();
            return;
        }

        var cfgPath = Path.of("");

        int lastDot = path.lastIndexOf('.');
        cfgPath = Path.of(lastDot > 0 ? path.substring(0, lastDot) + ".plotcfg" : path + ".plotcfg");
        File cfgFile = null;
        try {cfgFile = cfgPath.toFile();} catch (Exception ignored) {}
        if (cfgFile != null && cfgFile.isFile()) {
            try {
                var is = new BufferedInputStream(new FileInputStream(cfgFile));
                Prefs.ijPrefs.load(is);
                is.close();
            } catch (Exception e) {
                e.printStackTrace();
                IJ.beep();
                IJ.showMessage("Load Data and Open Config: Error loading plot config file: " + cfgPath);
            }

        }
        setupArrays();
        getPreferences();
    }


    static void saveData() {
        if (table == null || table.getLastColumn() == -1) {
            IJ.beep();
            IJ.showMessage("No table to save");
            return;
        }
        SaveDialog sf = new SaveDialog("Save measurement table data", MeasurementTable.shorterName(table.shortTitle()), Prefs.get("options.ext", ".xls"));
        if (sf.getDirectory() == null || sf.getFileName() == null) return;
        try {
            table.saveAs(sf.getDirectory() + sf.getFileName());
        } catch (IOException ioe) {
            IJ.beep();
            IJ.showMessage("Error writing measurement table file");
        }
    }


    static void saveDataSubsetDialog(String savePath) {
        class Holder {
            public static boolean saveColumnHeadings = true;
            public static boolean saveHeadersAsComment = true;
            public static boolean saveRowNumbers = true;
            public static boolean saveRowLabels = true;
        }
        maxSubsetColumns = (int) Prefs.get("plot2.maxSubsetColumns", maxSubsetColumns);
        boolean[] subsetColumnEnable = new boolean[maxSubsetColumns];
        String[] subsetColumn = new String[maxSubsetColumns];
        Holder.saveColumnHeadings = Prefs.get("plot2.saveColumnHeadings", Holder.saveColumnHeadings);
        Holder.saveHeadersAsComment = Prefs.get("plot2.saveHeadersAsComment", Holder.saveHeadersAsComment);
        Holder.saveRowNumbers = Prefs.get("plot2.saveRowNumbers", Holder.saveRowNumbers);
        Holder.saveRowLabels = Prefs.get("plot2.saveRowLabels", Holder.saveRowLabels);

        for (int i = 0; i < maxSubsetColumns; i++) {
            subsetColumnEnable[i] = true;
            subsetColumn[i] = "";
        }

        var gd = new GenericSwingDialog("Save data subset", mainFrame.getX() + 25, mainFrame.getY() + 50);

        for (int i = 0; i < maxSubsetColumns; i++) {
            subsetColumnEnable[i] = Prefs.get("plot2.subsetColumnEnable" + i, subsetColumnEnable[i]);
            subsetColumn[i] = Prefs.get("plot2.subsetColumn" + i, subsetColumn[i]);
        }

        gd.addMessage("Select datasets in the order (left to right) desired in the output file.\nNo column will be output for blank selections.");
        gd.addSlider("Number of data selection boxes (next time):", 1, 20, maxSubsetColumns < 21 && maxSubsetColumns > 0 ? maxSubsetColumns : 5, d -> maxSubsetColumns = d.intValue());
        String[] saveColumns = new String[columns.length + 1];
        saveColumns[0] = "";
        String meridian_flip = "Meridian_Flip";
        if (saveColumns.length > 1) saveColumns[1] = meridian_flip;
        if (saveColumns.length > 2) {
            System.arraycopy(columns, 1, saveColumns, 2, columns.length - 1);
        }
        gd.addMessage("             Column                          Enabled");
        gd.setOverridePosition(true);
        gd.resetPositionOverride();
        var size = new Dimension(180, 20);
        for (int i = 0; i < maxSubsetColumns; i++) {
            int finalI = i;
            gd.setNewPosition(GridBagConstraints.WEST);
            var x = gd.addChoice(IJ.pad((i + 1) + ":", 3), saveColumns, subsetColumn[i], b -> subsetColumn[finalI] = b);
            x.c1().setMaximumSize(size);
            x.c1().setPreferredSize(size);
            x.c1().setSize(size);
            x.c2().setMaximumSize(size);
            x.c2().setPreferredSize(size);
            x.c2().setSize(size);
            gd.resetPositionOverride();
            gd.setNewPosition(GridBagConstraints.CENTER);
            gd.addToSameRow();
            gd.addCheckbox("", subsetColumnEnable[i], b -> subsetColumnEnable[finalI] = b);
            gd.resetPositionOverride();
        }
        gd.setOverridePosition(false);

        String[] optionLabels = {"Save column headings", "Comment headings with '#'", "Save row numbers", "Save row labels"};
        boolean[] optionSettings = {Holder.saveColumnHeadings, Holder.saveHeadersAsComment, Holder.saveRowNumbers, Holder.saveRowLabels};
        final var cs = new ArrayList<Consumer<Boolean>>();
        cs.add(b -> Holder.saveColumnHeadings = b);
        cs.add(b -> Holder.saveHeadersAsComment = b);
        cs.add(b -> Holder.saveRowNumbers = b);
        cs.add(b -> Holder.saveRowLabels = b);
        gd.addCheckboxGroup(2, 2, optionLabels, optionSettings, cs);

        gd.showDialog();
        Prefs.set("plot2.maxSubsetColumns", maxSubsetColumns);
        if (gd.wasCanceled()) return;

        boolean meridianFlipSelected = false;
        for (int i = 0; i < subsetColumn.length; i++) {
            if (subsetColumn[i].equals("Meridian_Flip")) meridianFlipSelected = true;
        }

        for (int i = 0; i < maxSubsetColumns; i++) {
            Prefs.set("plot2.subsetColumnEnable" + i, subsetColumnEnable[i]);
            Prefs.set("plot2.subsetColumn" + i, subsetColumn[i]);
        }
        Prefs.set("plot2.saveColumnHeadings", Holder.saveColumnHeadings);
        Prefs.set("plot2.saveHeadersAsComment", Holder.saveHeadersAsComment);
        Prefs.set("plot2.saveRowNumbers", Holder.saveRowNumbers);
        Prefs.set("plot2.saveRowLabels", Holder.saveRowLabels);

        if (meridianFlipSelected) {
            if (!meridianFlipTimeColumnNotice()) return;
        }

        if (savePath == null) {
            savePath = MeasurementTable.shorterName(table.shortTitle());
            int location = savePath.lastIndexOf('.');
            if (location >= 0) savePath = savePath.substring(0, location);
            savePath += "_subset.dat";
            SaveDialog sf = new SaveDialog("Save data subset", savePath, null);
            if (sf.getDirectory() == null || sf.getFileName() == null) {
                IJ.beep();
                IJ.showMessage("Error: No save file path returned. Aborting save subset.");
                return;
            }
            savePath = sf.getDirectory() + sf.getFileName();
        }
        saveDataSubset(savePath);
    }

    static void saveDataSubset(String savePath) {
        PrintWriter pw = null;
        maxSubsetColumns = (int) Prefs.get("plot2.maxSubsetColumns", maxSubsetColumns);
        boolean[] subsetColumnEnable = new boolean[maxSubsetColumns];
        String[] subsetColumn = new String[maxSubsetColumns];
        for (int i = 0; i < maxSubsetColumns; i++) {
            subsetColumnEnable[i] = Prefs.get("plot2.subsetColumnEnable" + i, subsetColumnEnable[i]);
            subsetColumn[i] = Prefs.get("plot2.subsetColumn" + i, "");
        }
        boolean saveColumnHeadings = Prefs.get("plot2.saveColumnHeadings", true);
        boolean saveHeadersAsComment = Prefs.get("plot2.saveHeadersAsComment", true);
        boolean saveRowNumbers = Prefs.get("plot2.saveRowNumbers", true);
        boolean saveRowLabels = Prefs.get("plot2.saveRowLabels", true);
        String meridian_flip = "Meridian_Flip";

        int numColumns = 0;
        for (int i = 0; i < maxSubsetColumns; i++) {
            if (subsetColumnEnable[i] && !subsetColumn[i].trim().equals("")) {
                if (!subsetColumn[i].equals(meridian_flip) && table.getColumnIndex(subsetColumn[i]) == ResultsTable.COLUMN_NOT_FOUND) {
                    IJ.beep();
                    if (IJ.showMessageWithCancel("Save Data Subset Error", "Error: Table column " + subsetColumn[i] + " not found.\nPress OK to correct setting or Cancel to abort save subset.")) {
                        saveDataSubsetDialog(savePath);
                    }
                    return;
                } else {
                    numColumns++;
                }
            }
        }
        if (numColumns == 0 && !saveRowLabels) {
            IJ.beep();
            IJ.showMessage("Error: No table columns selected and not saving row labels. Aborting save subset.");
            return;
        }

        int rows = table.getCounter();
        if (rows == 0) {
            IJ.beep();
            IJ.showMessage("Error: No rows in table to save. Aborting save subset.");
            return;
        }

        char delimiter = savePath.endsWith(".csv") ? ',' : '\t';
        StringBuilder formatString = new StringBuilder("0.");
        for (int i = 0; i < MeasurementTable.DEFAULT_DECIMALS; i++)
            formatString.append("0");
        DecimalFormat output = new DecimalFormat(formatString.toString(), IJU.dfs);
//            DecimalFormatSymbols dfs = output.getDecimalFormatSymbols();
//            dfs.setDecimalSeparator('.');
//            output.setDecimalFormatSymbols(dfs);
        try {
            FileOutputStream fos = new FileOutputStream(savePath);
            BufferedOutputStream bos = new BufferedOutputStream(fos);
            pw = new PrintWriter(bos);
            StringBuilder line = new StringBuilder();
            String outText = "";
            double value = -1.0;
            boolean needDelimiter;
            if (saveColumnHeadings) {
                line = new StringBuilder((saveHeadersAsComment ? "#" : "") + (saveRowNumbers ? delimiter : "") + (saveRowLabels ? "Label" + delimiter : ""));
                needDelimiter = false;
                for (int i = 0; i < maxSubsetColumns; i++) {
                    if (subsetColumnEnable[i] && !subsetColumn[i].trim().equals("")) {
                        line.append(needDelimiter ? delimiter : "").append(subsetColumn[i]);
                        needDelimiter = true;
                    }
                }
                pw.println(line);
            }
            for (int row = 0; row < rows; row++) {
                line = new StringBuilder((saveRowNumbers ? "" + (row + 1) + delimiter : "") + (saveRowLabels ? table.getLabel(row) + delimiter : ""));
                needDelimiter = false;
                int xlen = x[firstCurve].length;
                for (int i = 0; i < maxSubsetColumns; i++) {
                    if (subsetColumnEnable[i] && !subsetColumn[i].trim().equals("")) {
                        if (subsetColumn[i].equals(meridian_flip)) {
                            if (row - excludedHeadSamples >= 0) {
                                if (row - excludedHeadSamples < xlen) {
                                    value = x[firstCurve][row - excludedHeadSamples] < mfMarker1Value ? -1.0 : 1.0;
                                } else { value = 1.0; }
                            } else {
                                value = -1.0;
                            }
                        } else {
                            value = table.getValue(subsetColumn[i], row);
                            if (i > 2 && (subsetColumn[i].contains("J.D.") || subsetColumn[i].contains("JD"))) {
                                if (subsetColumn[i].startsWith("J.D.-2400000")) value += 2400000;
                                value -= xOffset;
                            }
                        }
                        outText = Double.isNaN(value) ? "" : output.format(value);
                        line.append(needDelimiter ? delimiter : "").append(outText);
                        needDelimiter = true;
                    }
                }
                pw.println(line);
            }
        } catch (IOException ioe) {
            IJ.beep();
            IJ.showMessage("Error writing data subset file");
        } finally {
            pw.close();
        }

    }

    static void createMpcFormatDialog() {
        if (table == null) {
            IJ.showMessage("Open a measurements table into Multi-plot and then restart MPC formatting");
            return;
        }

        String definitiveDesignation = Prefs.get("plot2.definitiveDesignation", "");
        if (definitiveDesignation.length() > 5) definitiveDesignation = definitiveDesignation.substring(0, 5);
        String provisionalDesignation = Prefs.get("plot2.provisionalDesignation", "");
        if (provisionalDesignation.length() > 7) provisionalDesignation = provisionalDesignation.substring(0, 7);
        String observatoryDesignation = Prefs.get("plot2.observatoryDesignation", "XXX");
        if (observatoryDesignation.length() > 3) observatoryDesignation = observatoryDesignation.substring(0, 3);
        String discovery = Prefs.get("plot2.discovery", " ");
        if (!(discovery.equals(" ") || discovery.equals("*"))) discovery = " ";
        String note1 = Prefs.get("plot2.note1", " ");
        if (note1.length() > 1) note1 = note1.substring(0, 1);
        String note2 = Prefs.get("plot2.note2", "C");
        if (note2.length() > 1) note2 = note2.substring(0, 1);
        String filter = Prefs.get("plot2.filter", "B");
        if (filter.length() > 1) filter = filter.substring(0, 1);
        if (!(filter.equals(" ") || filter.equals("B") || filter.equals("V") || filter.equals("R") || filter.equals("I") || filter.equals("J") || filter.equals("W") || filter.equals("U") || filter.equals("g") || filter.equals("r") || filter.equals("i") || filter.equals("w") || filter.equals("y") || filter.equals("z") || filter.equals("N") || filter.equals("T"))) {
            filter = "B";
        }
        String contactName = Prefs.get("plot2.contactName", "");
        if (contactName.length() > 76) contactName = contactName.substring(0, 76);
        String observerName = Prefs.get("plot2.observerName", "");
        if (observerName.length() > 76) observerName = observerName.substring(0, 76);
        String measurerName = Prefs.get("plot2.measurerName", "");
        if (measurerName.length() > 76) measurerName = measurerName.substring(0, 76);
        String telescopeDetails = Prefs.get("plot2.telescopeDetails", "");
        if (telescopeDetails.length() > 76) telescopeDetails = telescopeDetails.substring(0, 76);
        String reductionCatalogues = Prefs.get("plot2.reductionCatalogues", "USNO-B1.0");
        if (reductionCatalogues.length() > 76) reductionCatalogues = reductionCatalogues.substring(0, 76);
        String MPCacknowledgement = Prefs.get("plot2.MPCacknowledgement", "Data Submission Received");
        if (MPCacknowledgement.length() > 76) MPCacknowledgement = MPCacknowledgement.substring(0, 76);
        String jdutcColumnName = Prefs.get("plot2.jdutcColumnName", "");
        String magnitudeColumnName = Prefs.get("plot2.magnitudeColumnName", "");
        double magnitude = Prefs.get("plot2.magnitude", 0.0);
        boolean useTwoMagPlaces = Prefs.get("plot2.useTwoMagPlaces", false);
        String raColumnName = Prefs.get("plot2.raColumnName", "");
        String decColumnName = Prefs.get("plot2.decColumnName", "");

        GenericDialog gd = new GenericDialog("Create Minor Planet Center Format", mainFrame.getX() + 10, mainFrame.getY() + 10);

        gd.addMessage("*** Detailed instructions for each field are at: http://www.minorplanetcenter.net/iau/info/OpticalObs.html ***");
        gd.addStringField("Definitive Designation (5 chars or empty)", definitiveDesignation, 5);
        gd.addStringField("Provisional Designation (7 chars or empty)", provisionalDesignation, 7);
        gd.addStringField("Observatory Designation (3 chars)", observatoryDesignation, 3);
        gd.addChoice("Discovery", new String[]{" ", "*"}, discovery);
        gd.addStringField("Note 1 (1 char)", note1, 1);
        gd.addStringField("Note 2 (1 char)", note2, 1);
        gd.addChoice("Filter", new String[]{" ", "B", "V", "R", "I", "J", "W", "U", "g", "r", "i", "w", "y", "z", "N", "T"}, filter);
        gd.addStringField("Contact Name", contactName, 76);
        gd.addStringField("Observer Name", observerName, 76);
        gd.addStringField("Measurer Name", measurerName, 76);
        gd.addStringField("Telescope Details", telescopeDetails, 76);
        gd.addStringField("Reduction Catalogue", reductionCatalogues, 76);
        gd.addStringField("Acknowledgement Message", MPCacknowledgement, 76);
        gd.addChoice("Select JD_UTC Column", columns, jdutcColumnName);
        gd.addChoice("Select Magnitude Column (blank for fixed mag)", columns, magnitudeColumnName);
        gd.addNumericField("OR: Fixed Magnitude", magnitude, 2, 5, "(format = xx.xx OR select blank filter to report no magnitude)");
        gd.addChoice("Select RA Column", columns, raColumnName);
        gd.addChoice("Select Dec Column", columns, decColumnName);
        gd.addCheckbox("Report magnitudes to 2 decimal places (deselect for standard 1 decimal place)", useTwoMagPlaces);

        gd.showDialog();

        if (gd.wasCanceled()) return;

        definitiveDesignation = gd.getNextString();
        definitiveDesignation = forceLength(definitiveDesignation, 5);
        Prefs.set("plot2.definitiveDesignation", definitiveDesignation);

        provisionalDesignation = gd.getNextString();
        provisionalDesignation = forceLength(provisionalDesignation, 7);
        Prefs.set("plot2.provisionalDesignation", provisionalDesignation);

        observatoryDesignation = gd.getNextString();
        observatoryDesignation = forceLength(observatoryDesignation, 3);
        Prefs.set("plot2.observatoryDesignation", observatoryDesignation);

        discovery = gd.getNextChoice();
        if (!(discovery.equals(" ") || discovery.equals("*"))) discovery = " ";
        Prefs.set("plot2.discovery", discovery);

        note1 = gd.getNextString();
        note1 = forceLength(note1, 1);
        Prefs.set("plot2.note1", note1);

        note2 = gd.getNextString();
        note2 = forceLength(note2, 1);
        Prefs.set("plot2.note2", note2);

        filter = gd.getNextChoice();
        if (filter.length() > 1) filter = filter.substring(0, 1);
        if (!(filter.equals(" ") || filter.equals("B") || filter.equals("V") || filter.equals("R") || filter.equals("I") || filter.equals("J") || filter.equals("W") || filter.equals("U") || filter.equals("g") || filter.equals("r") || filter.equals("i") || filter.equals("w") || filter.equals("y") || filter.equals("z") || filter.equals("N") || filter.equals("T"))) {
            filter = "B";
        }
        Prefs.set("plot2.filter", filter);

        contactName = gd.getNextString().trim();
        if (contactName.length() > 76) contactName = contactName.substring(0, 76);
        Prefs.set("plot2.contactName", contactName);

        observerName = gd.getNextString().trim();
        if (observerName.length() > 76) observerName = observerName.substring(0, 76);
        Prefs.set("plot2.observerName", observerName);

        measurerName = gd.getNextString().trim();
        if (measurerName.length() > 76) measurerName = measurerName.substring(0, 76);
        Prefs.set("plot2.measurerName", measurerName);

        telescopeDetails = gd.getNextString().trim();
        if (telescopeDetails.length() > 76) telescopeDetails = telescopeDetails.substring(0, 76);
        Prefs.set("plot2.telescopeDetails", telescopeDetails);

        reductionCatalogues = gd.getNextString().trim();
        if (reductionCatalogues.length() > 76) reductionCatalogues = reductionCatalogues.substring(0, 76);
        Prefs.set("plot2.reductionCatalogues", reductionCatalogues);

        MPCacknowledgement = gd.getNextString().trim();
        if (MPCacknowledgement.length() > 76) MPCacknowledgement = MPCacknowledgement.substring(0, 76);
        Prefs.set("plot2.MPCacknowledgement", MPCacknowledgement);

        jdutcColumnName = gd.getNextChoice();
        Prefs.set("plot2.jdutcColumnName", jdutcColumnName);
        if (jdutcColumnName.trim().equals("")) {
            IJ.showMessage("No JD_UTC column selected");
            return;
        }

        magnitudeColumnName = gd.getNextChoice();
        Prefs.set("plot2.magnitudeColumnName", magnitudeColumnName);
        magnitude = gd.getNextNumber();
        Prefs.set("plot2.magnitude", magnitude);

        raColumnName = gd.getNextChoice();
        Prefs.set("plot2.raColumnName", raColumnName);
        if (raColumnName.trim().equals("")) {
            IJ.showMessage("No RA column selected");
            return;
        }

        decColumnName = gd.getNextChoice();
        Prefs.set("plot2.decColumnName", decColumnName);
        if (decColumnName.trim().equals("")) {
            IJ.showMessage("No Dec column selected");
            return;
        }
        Prefs.set("plot2.decColumnName", decColumnName);

        useTwoMagPlaces = gd.getNextBoolean();
        Prefs.set("plot2.useTwoMagPlaces", useTwoMagPlaces);

        int jdCol = table.getColumnIndex(jdutcColumnName);
        if (jdCol == MeasurementTable.COLUMN_NOT_FOUND) {
            IJ.showMessage("JD_UTC column '" + jdutcColumnName + "' not found. Aborting.");
            return;
        }
        int magCol = table.getColumnIndex(magnitudeColumnName);
        int raCol = table.getColumnIndex(raColumnName);
        if (raCol == MeasurementTable.COLUMN_NOT_FOUND) {
            IJ.showMessage("RA column '" + raColumnName + "' not found. Aborting.");
            return;
        }
        int decCol = table.getColumnIndex(decColumnName);
        if (decCol == MeasurementTable.COLUMN_NOT_FOUND) {
            IJ.showMessage("Dec column '" + decColumnName + "' not found. Aborting.");
            return;
        }

        IJ.log("COD " + observatoryDesignation);
        IJ.log("CON " + contactName);
        IJ.log("OBS " + observerName);
        IJ.log("MEA " + measurerName);
        IJ.log("TEL " + telescopeDetails);
        IJ.log("NET " + reductionCatalogues);
        IJ.log("ACK " + MPCacknowledgement);
        IJ.log("" + table.getCounter());

        double jd, mag, ra, dec;
        String SMag;
        for (int row = 0; row < table.getCounter(); row++) {
            jd = table.getValueAsDouble(jdCol, row);
            if (magCol == MeasurementTable.COLUMN_NOT_FOUND) { mag = magnitude; } else {
                mag = table.getValueAsDouble(magCol, row);
            }
            ra = table.getValueAsDouble(raCol, row);
            dec = table.getValueAsDouble(decCol, row);
            if (filter.equals(" ") || Double.isNaN(mag)) {
                SMag = "     ";
            } else {
                if (useTwoMagPlaces) { SMag = twoDigitsTwoPlaces.format(mag); } else {
                    SMag = twoDigitsOnePlace.format(mag) + " ";
                }
                if (SMag.length() > 5) SMag = SMag.substring(SMag.length() - 5);
            }
            IJ.log(definitiveDesignation + provisionalDesignation + discovery + note1 + note2 + JD_to_MPCDate(jd) + RA_to_MPCRA(ra) + Dec_to_MPCDec(dec) + "         " + SMag + filter + "      " + observatoryDesignation);
        }
    }

    static String forceLength(String s, int len) {
        int slength = s.length();
        if (slength > len) {
            s = s.substring(0, len);
        } else if (slength < len) {
            StringBuilder sBuilder = new StringBuilder(s);
            for (int i = 0; i < len - slength; i++) {
                sBuilder.append(" ");
            }
            s = sBuilder.toString();
        }
        return s;
    }

    static String JD_to_MPCDate(double jd) {
        // Fliegel-Van Flandern algorithm
        double J = Math.floor(jd);
        double F = jd - J;
        if (F >= 0.5) {
            J++;
            F -= 0.5;
        } else { F += 0.5; }

        double p = J + 68569;
        double q = Math.floor(4 * p / 146097);
        double r = p - Math.floor((146097 * q + 3) / 4);
        double s = Math.floor(4000 * (r + 1) / 1461001);
        double t = r - Math.floor(1461 * s / 4) + 31;
        double u = Math.floor(80 * t / 2447);
        double v = Math.floor(u / 11);

        double Y = 100 * (q - 49) + s + v;
        double M = u + 2 - 12 * v;
        double D = t - Math.floor(2447 * u / 80);
        D += F;

        String SY = fourDigits.format(Y);
        String SM = twoDigits.format(M);
        String SD = twoDigitsFivePlaces.format(D);
        return "" + SY + " " + SM + " " + SD + " ";
    }


    static String RA_to_MPCRA(double ra) {
        ra %= 24;
        double H = Math.floor(ra);
        double S = (ra - H) * 3600;
        double M = Math.floor(S / 60);
        S -= M * 60;
        String SH = twoDigits.format(H);
        String SM = twoDigits.format(M);
        String SS = twoDigitsTwoPlaces.format(S);
        return "" + SH + " " + SM + " " + SS + " ";
    }

    static String Dec_to_MPCDec(double dec) {
        if (dec < -90) dec = -90;
        if (dec > 90) dec = 90;
        String Sign;
        if (dec < 0) {
            Sign = "-";
            dec = -dec;
        } else {
            Sign = "+";
        }

        double D = Math.floor(dec);
        double S = (dec - D) * 3600;
        double M = Math.floor(S / 60);
        S -= M * 60;
        String SD = twoDigits.format(D);
        String SM = twoDigits.format(M);
        String SS = twoDigitsOnePlace.format(S);
        return "" + Sign + SD + " " + SM + " " + SS + " ";
    }


    static void saveFitPanelToTextFileDialog(String savePath, int curve) {
        if (savePath == null) {
            savePath = table == null ? "NoTableLoaded" : MeasurementTable.shorterName(table.shortTitle());
            int location = savePath.lastIndexOf('.');
            if (location >= 0) savePath = savePath.substring(0, location);
            savePath += fitPanelTextSuffix + twoDigits.format(curve + 1) + "_" + ylabel[curve] + ".txt";
            SaveDialog sf = new SaveDialog("Save fit panel results to text file", savePath, null);
            if (sf.getDirectory() == null || sf.getFileName() == null) {
                IJ.beep();
                IJ.showMessage("Error: No save file path returned. Aborting save of fit panel results to text file.");
                return;
            }
            savePath = sf.getDirectory() + sf.getFileName();
        }
        saveFitPanelToTextFile(savePath, curve);
    }

    static void saveFitPanelToTextFile(String savePath, int curve) {
        PrintWriter pw = null;
        String mpf = "%-25s %8s %17.9f";
        String fpf = "%-25s %8s %17.9f";
        String mpd = "%-25s %8s %7d";
        String tlf = "%-25s %8s %17s %17s %17s %17s";
        String tpf = "%-25s %8s %17.9f %17.9f %17.9f %17.9f";
        try {
            FileOutputStream fos = new FileOutputStream(savePath);
            BufferedOutputStream bos = new BufferedOutputStream(fos);
            pw = new PrintWriter(bos);
            pw.println("                          Fit Results for Data Set " + (curve + 1) + " (" + ylabel[curve] + ")");
            pw.println("----------------------------------------------------------------------------------------------------------");
            pw.println("");
            pw.println(String.format(tlf, "    Parameter", "Type ", "Best Fit  ", "Prior Center", "Prior Width", "Step Size "));
            pw.println(String.format(tlf, "    --------------------", "-------", "------------", "------------", "-----------", "-----------"));
            pw.println("User Specified Parameters");
            pw.println(String.format(fpf, "    Orbital Period (days)", "Fixed ", orbitalPeriod[curve]));
            pw.println(String.format(fpf, "    Orbital Eccentricity", "Fixed ", forceCircularOrbit[curve] ? 0.0 : eccentricity[curve]));
            pw.println(String.format(fpf, "    Arg. of Peri. (deg)", "Fixed ", forceCircularOrbit[curve] ? 0.0 : omega[curve]));
            pw.println(String.format(fpf, "    Stellar Radius (Rsun)", "Fixed ", IJU.getRStarFromTeff(teff[curve])));
            if (table == null) {
                pw.println("No Data Table Loaded for Fitting");
            } else if (useTransitFit[curve] && converged[curve]) {
                pw.println("Transit Parameters");

                String[] parName = {"    Baseline Flux (raw)", "    (Rp/R*)^2", "    a/R*", "    Tc", "    Inclination (deg)", "    Quad LD u1", "    Quad LD u2"};
                for (int p = 0; p < 7; p++) {
                    double value = bestFit[curve][p];
                    if (p == 1) value *= value;
                    if (p == 4) value *= 180.0 / Math.PI;
                    pw.println(String.format(tpf, parName[p], lockToCenter[curve][p] ? "LOCKED" : "Fitted", value, priorCenter[curve][p], lockToCenter[curve][p] ? 0.0 : usePriorWidth[curve][p] ? priorWidth[curve][p] : Double.POSITIVE_INFINITY, getFitStep(curve, p)));
                }

            } else if (useTransitFit[curve] && !converged[curve]) {
                pw.println("Fit Did Not Converge");
            } else {
                pw.println("Transit Fit Disabled");
            }


            boolean useDetrend = false;
            for (int d = 0; d < maxDetrendVars; d++) {
                if (useFitDetrendCB[curve][d].isSelected()) useDetrend = true;
            }
            if (useDetrend && converged[curve]) {
                pw.println("Detrend Parameters");
                for (int p = 7; p < 7 + maxDetrendVars; p++) {
                    if (useFitDetrendCB[curve][p - 7].isSelected()) {
                        pw.println(String.format(tpf, "    " + fitDetrendComboBox[curve][p - 7].getSelectedItem(), lockToCenter[curve][p] ? "LOCKED" : "Fitted", bestFit[curve][p], priorCenter[curve][p], usePriorWidth[curve][p] ? priorWidth[curve][p] : Double.POSITIVE_INFINITY, getFitStep(curve, p)));
                    }
                }
            }

            if (useTransitFit[curve] && converged[curve]) {
                pw.println("Calculated From Model");
                pw.println(String.format(mpf, "    Impact Angle", "Calc ", bp[curve]));
                pw.println(String.format(mpf, "    t14 (d)", "Calc ", t14[curve]));
                pw.println(String.format("%-25s %8s %13s", "    t14 (h:m:s)", "Calc ", IJU.decToSex(24 * t14[curve], 0, 24, false)));
                pw.println(String.format(mpf, "    t23 (d)", "Calc ", t23[curve]));
                pw.println(String.format(mpf, "    tau (d)", "Calc ", tau[curve]));
                pw.println(String.format(mpf, "    Stellar Density (cgs)", "Calc ", stellarDensity[curve]));
                pw.println(String.format("%-25s %8s %9s", "    (est)Spectral Type", "Calc ", IJU.spectralTypeFromDensity(stellarDensity[curve])));
                pw.println(String.format(mpf, "    Planet Radius (Rjup)", "Calc ", IJU.getPlanetRadiusFromTeff(teff[curve], bestFit[curve][1])));
            }
            pw.println("Fit Statistics");
            String sigmaLabel = "RMS";
            double sigmaValue = sigma[curve];
            if (!mmag[curve] && normIndex[curve] == 0) {
                sigmaLabel += " (raw)";
            } else if (mmag[curve]) {
                sigmaLabel += " (mmag)";
                sigmaValue *= 1000.0;
            } else if (normIndex[curve] != 0) {
                sigmaLabel += " (normalized)";
            }
            pw.println(String.format(mpf, "    " + sigmaLabel, "Stat ", sigmaValue));
            pw.println(String.format(mpf, "    chi^2/dof", "Stat ", chi2dof[curve]));
            pw.println(String.format(mpf, "    BIC", "Stat ", bic[curve]));
            pw.println(String.format(mpd, "    Degrees of freedom", "Stat ", dof[curve]));
            pw.println(String.format(mpf, "    chi^2", "Stat ", chi2[curve]));
            pw.println(String.format(mpd, "    Number of steps taken", "Stat ", nTries[curve]));
            pw.println("Fit Settings");
            pw.println(String.format(mpd, "    Max steps allowed", "Setting", maxFitSteps[curve]));
            pw.println(String.format("%-25s %8s %7.1G", "    Tolerance of fit", "Setting", tolerance[curve]));
        } catch (IOException ioe) {
            IJ.beep();
            IJ.showMessage("Error writing fit results to text file");
        } finally {
            pw.close();
        }

    }

    static boolean meridianFlipTimeColumnNotice() {
        GenericDialog gd = new GenericDialog("Meridian Flip Output Warning", mainFrame.getX() + 25, mainFrame.getY() + 100);
        gd.addMessage("Meridian_Flip has been selected as an output data column.\n" + "Certain plot settings are first required to ensure that proper meridian flip data are written.\n" + "(1) The meridian flip time marker must be set properly in the 'Multi-plot Main' panel.\n" + "(2) The time data column corresponding to the output data columns must be set as the\n" + "'X-data' column for the top-most enabled 'Multi-plot Y-data' plot row.\n" + "(3) 'Input Average Size' on the top-most enabled row must be set the same as for the data columns being written.\n" + "NOTE: All output data are unaveraged unless average input data have been previously saved back into the\n" + "measurements table using the 'New Col' button on the left side of a 'Multi-plot Y-data' row.\n" + "Therefore, before saving a data subset, 'Input Average Size' on the top-most enabled plot row should be set to '1'\n" + "unless averaged data were previously saved to the measurements table and are selected as the output data.");

        gd.addMessage("Press 'OK' if the plot settings are correct,\n" + "or 'Cancel' if the plot settings need to be changed.");
        gd.showDialog();
        return !gd.wasCanceled();
    }

    public static void savePlotImageAsPng() {
        SaveDialog sf = new SaveDialog("Save plot image as PNG...", MeasurementTable.shorterName(table.shortTitle()), ".png");
        if (sf.getDirectory() == null || sf.getFileName() == null) return;
        savePlotImage(sf.getDirectory() + sf.getFileName(), "ij.plugin.PNG_Writer");
    }

    public static void savePlotImageAsJpg() {
        SaveDialog sf = new SaveDialog("Save plot image as JPG...", MeasurementTable.shorterName(table.shortTitle()), ".jpg");
        if (sf.getDirectory() == null || sf.getFileName() == null) return;
        savePlotImage(sf.getDirectory() + sf.getFileName(), "ij.plugin.JpegWriter");
    }

    public static void savePlotImageAsVectorPdf() {
        SaveDialog sf = new SaveDialog("Save plot image as vector PDF...", MeasurementTable.shorterName(table.shortTitle()), ".pdf");
        if (sf.getDirectory() == null || sf.getFileName() == null) return;
        PdfPlotOutput.savePlot(plot, sf.getDirectory() + sf.getFileName());
    }

    public static void savePlotImageAsVectorPdf(String path) {
        PdfPlotOutput.savePlot(plot, path);
    }

    public static void savePlotImageAsPng(String path) {
        savePlotImage(path, "ij.plugin.PNG_Writer");
    }

    public static void savePlotImageAsJpg(String path) {
        savePlotImage(path, "ij.plugin.JpegWriter");
    }

    public static void savePlotImage(String path, String writer) {
        ImagePlus image = WindowManager.getImage("Plot of " + tableName);
        if (image == null) {
            IJ.beep();
            IJ.showMessage("No plot image to save");
            return;
        }
        if (path == null) return;
        IJ.runPlugIn(image, writer, path);
    }

    static void saveConfig(boolean template) {
        savePreferences();
        String startingDir = OpenDialog.getDefaultDirectory();
        String fileName = table.shortTitle().startsWith("Measurements in ") ? table.shortTitle().replace("Measurements in ", "") : table.shortTitle();
        int lastDot = fileName.lastIndexOf('.');
        if (lastDot >= 0) fileName = fileName.substring(0, lastDot);
        SaveDialog sf;
        if (template && templateDir != null && !templateDir.equals("")) {
            OpenDialog.setDefaultDirectory(templateDir);
        }
        sf = new SaveDialog("Save plot " + (template ? "template" : "configuration"), fileName, ".plotcfg");
        if (sf.getDirectory() == null || sf.getFileName() == null || sf.getFileName().equals("")) {
            if (template) OpenDialog.setDefaultDirectory(startingDir);
            return;
        }
        String savepath = sf.getDirectory() + sf.getFileName();
        if (template) {
            templateDir = sf.getDirectory();
            OpenDialog.setDefaultDirectory(startingDir);
            Prefs.set("plot2.templateDir", templateDir);
        }
        lastDot = savepath.lastIndexOf('.');
        if (lastDot >= 0) savepath = savepath.substring(0, lastDot);
        savepath += ".plotcfg";
        Properties prefs = new Properties();
        Enumeration e = Prefs.ijPrefs.keys();
        while (e.hasMoreElements()) {
            String key = (String) e.nextElement();
            if (key.indexOf(".plot.") == 0) prefs.put(key, Prefs.ijPrefs.getProperty(key));
        }

        try {
            FileOutputStream fos = new FileOutputStream(savepath);
            BufferedOutputStream bos = new BufferedOutputStream(fos);
            prefs.store(bos, "AstroImageJ Plot Configuration");
            bos.close();
        } catch (IOException ioe) {
            IJ.beep();
            IJ.showMessage("Error writing plot configuration file");
        }
    }

    static void saveDataConfig() {
        savePreferences();
        SaveDialog sf = new SaveDialog("Save measurement table data and plot configuration", MeasurementTable.shorterName(table.shortTitle()), Prefs.get("options.ext", ".xls"));
        if (sf.getDirectory() == null || sf.getFileName() == null) return;
        String path = sf.getDirectory() + sf.getFileName();
        if (table == null || table.getLastColumn() == -1) {
            IJ.beep();
            IJ.showMessage("No table to save");
            return;
        } else {
            try {
                table.saveAs(path);
            } catch (IOException ioe) {
                IJ.beep();
                IJ.showMessage("Error writing measurement table file");
            }
        }
        int lastDot = path.lastIndexOf('.');
        if (lastDot >= 0) path = path.substring(0, lastDot);
        path += ".plotcfg";
        File outFile = new File(path);
        if (outFile.isDirectory()) return;
        if (outFile.isFile()) outFile.delete();
        Properties prefs = new Properties();
        Enumeration e = Prefs.ijPrefs.keys();
        while (e.hasMoreElements()) {
            String key = (String) e.nextElement();
            if (key.indexOf(".plot.") == 0) prefs.put(key, Prefs.ijPrefs.getProperty(key));
        }
        try {
            FileOutputStream fos = new FileOutputStream(path);
            BufferedOutputStream bos = new BufferedOutputStream(fos);
            prefs.store(bos, "AstroImageJ Plot Configuration");
            bos.close();
        } catch (IOException ioe) {
            IJ.beep();
            IJ.showMessage("Error writing plot configuration file");
        }
    }

    static public void saveDataImageConfig(boolean savePlot, boolean saveConfig, boolean saveData, boolean filenamesProvided, String imageFormat, String plotPath, String configPath, String dataPath) {
        savePreferences();
        SaveDialog sf;
        String outPath = "";
        String outBase = "";
        if (!filenamesProvided) {
            sf = new SaveDialog("Save " + (saveData ? "measurement table data" : "") + (saveData && savePlot ? ", " : "") + (savePlot ? "plot image" : "") + (saveData && savePlot && saveConfig ? "," : "") + ((saveData || savePlot) && saveConfig ? " and " : "") + (saveConfig ? "plot configuration" : ""), MeasurementTable.shorterName(table.shortTitle()), "");
            if (sf.getDirectory() == null || sf.getFileName() == null) return;
            outPath = sf.getDirectory() + sf.getFileName();
            int location = outPath.lastIndexOf('.');
            if (location >= 0) outPath = outPath.substring(0, location);
            outBase = outPath;
            if (outBase.endsWith(plotSuffix)) {
                location = outBase.lastIndexOf(plotSuffix);
                if (location >= 0) outBase = outBase.substring(0, location);
            } else if (outBase.endsWith(configSuffix)) {
                location = outBase.lastIndexOf(configSuffix);
                if (location >= 0) outBase = outBase.substring(0, location);
            } else if (outBase.endsWith(dataSuffix)) {
                location = outBase.lastIndexOf(dataSuffix);
                if (location >= 0) outBase = outBase.substring(0, location);
            }
        }

        if (saveData) {
            if (table == null || table.getLastColumn() == -1) {
                IJ.beep();
                IJ.showMessage("No data table to save");
            } else {
                try {
                    table.saveAs(filenamesProvided ? dataPath : outBase + dataSuffix + Prefs.get("options.ext", ".xls"));
                } catch (IOException ioe) {
                    IJ.beep();
                    IJ.showMessage("Error writing measurement table file");
                }
            }
        }

        if (savePlot) {
            String imagepath = filenamesProvided ? plotPath : outBase + plotSuffix + "." + imageFormat;
            ImagePlus image = WindowManager.getImage("Plot of " + tableName);
            if (image == null) {
                IJ.beep();
                IJ.showMessage("No plot image to save");
            } else if (imageFormat.equalsIgnoreCase("png")) {
                IJ.runPlugIn(image, "ij.plugin.PNG_Writer", imagepath);
            } else if (imageFormat.equalsIgnoreCase("jpg")) {
                IJ.runPlugIn(image, "ij.plugin.JpegWriter", imagepath);
            }
        }
        if (saveConfig) {
            String cfgpath = filenamesProvided ? configPath : outBase + configSuffix + ".plotcfg";
            File outFile = new File(cfgpath);
            if (outFile.isDirectory()) {
                IJ.error("bad configuration save filename");
                return;
            }
            if (outFile.isFile()) outFile.delete();
            Properties prefs = new Properties();
            Enumeration e = Prefs.ijPrefs.keys();
            while (e.hasMoreElements()) {
                String key = (String) e.nextElement();
                if (key.indexOf(".plot.") == 0) prefs.put(key, Prefs.ijPrefs.getProperty(key));
            }
            try {
                FileOutputStream fos = new FileOutputStream(cfgpath);
                BufferedOutputStream bos = new BufferedOutputStream(fos);
                prefs.store(bos, "AstroImageJ Plot Configuration");
                bos.close();
            } catch (IOException ioe) {
                IJ.beep();
                IJ.showMessage("Error writing plot configuration file");
            }
        }
    }

    static void saveAllDialog() {
        saveImage = Prefs.get("Astronomy_Tool.saveImage", saveImage);
        savePlot = Prefs.get("Astronomy_Tool.savePlot", savePlot);
        saveSeeingProfile = Prefs.get("Astronomy_Tool.saveSeeingProfile", saveSeeingProfile);
        saveSeeingProfileStack = Prefs.get("Astronomy_Tool.saveSeeingProfileStack", saveSeeingProfileStack);
        saveConfig = Prefs.get("Astronomy_Tool.saveConfig", saveConfig);
        saveTable = Prefs.get("Astronomy_Tool.saveTable", saveTable);
        saveApertures = Prefs.get("Astronomy_Tool.saveApertures", saveApertures);
        saveFitPanels = Prefs.get("Astronomy_Tool.saveFitPanels", saveFitPanels);
        saveFitPanelText = Prefs.get("Astronomy_Tool.saveFitPanelText", saveFitPanelText);
        saveLog = Prefs.get("Astronomy_Tool.saveLog", saveLog);
        saveDataSubset = Prefs.get("Astronomy_Tool.saveDataSubset", saveDataSubset);
        showDataSubsetPanel = Prefs.get("Astronomy_Tool.showDataSubsetPanel", showDataSubsetPanel);
        imageSuffix = Prefs.get("Astronomy_Tool.imageSuffix", imageSuffix);
        seeingProfileSuffix = Prefs.get("Astronomy_Tool.seeingProfileSuffix", seeingProfileSuffix);
        seeingProfileStackSuffix = Prefs.get("Astronomy_Tool.seeingProfileStackSuffix", seeingProfileStackSuffix);
        plotSuffix = Prefs.get("Astronomy_Tool.plotSuffix", plotSuffix);
        configSuffix = Prefs.get("Astronomy_Tool.configSuffix", configSuffix);
        dataSuffix = Prefs.get("Astronomy_Tool.dataSuffix", dataSuffix);
        aperSuffix = Prefs.get("Astronomy_Tool.aperSuffix", aperSuffix);
        logSuffix = Prefs.get("Astronomy_Tool.logSuffix", logSuffix);
        fitPanelSuffix = Prefs.get("Astronomy_Tool.fitPanelSuffix", fitPanelSuffix);
        fitPanelTextSuffix = Prefs.get("Astronomy_Tool.fitPanelTextSuffix", fitPanelTextSuffix);
        dataSubsetSuffix = Prefs.get("Astronomy_Tool.dataSubsetSuffix", dataSubsetSuffix);
        saveAllPNG = Prefs.get("Astronomy_Tool.saveAllPNG", saveAllPNG);

        GenericDialog gd = new GenericDialog("Save all settings", mainFrame.getX() + mainFrame.getWidth() / 2 - 300, mainFrame.getY() + mainFrame.getHeight() / 2 - 200);
        gd.enableYesNoCancel("Save Files Now", "Save Settings Only");

        gd.addMessage("Select items to save when using save all:");
        gd.addCheckboxGroup(1, 10, new String[]{"Image", "Plot", "Seeing Profile", "Seeing Profile Stack", "Plot Config", "Data Table", "Apertures", "Fit Panels", "Fit Text", "Log"}, new boolean[]{saveImage, savePlot, saveSeeingProfile, saveSeeingProfileStack, saveConfig, saveTable, saveApertures, saveFitPanels, saveFitPanelText, saveLog});
        gd.addCheckboxGroup(1, 2, new String[]{"Data Subset", "Show Data Subset Panel"}, new boolean[]{saveDataSubset, showDataSubsetPanel});
        gd.addStringField("Science Image display suffix:", imageSuffix, 40);
        gd.addStringField("Plot image display suffix:", plotSuffix, 40);
        gd.addStringField("Seeing Profile suffix:", seeingProfileSuffix, 40);
        gd.addStringField("Seeing Profile Stack suffix:", seeingProfileStackSuffix, 40);
        gd.addStringField("Plot config file suffix**:", configSuffix, 40);
        gd.addStringField("Full data table file suffix**:", dataSuffix, 40);
        gd.addStringField("Data table subset file suffix:", dataSubsetSuffix, 40);
        gd.addStringField("Aperture file suffix:", aperSuffix, 40);
        gd.addStringField("Fit panel image suffix:", fitPanelSuffix, 40);
        gd.addStringField("Fit data text file suffix:", fitPanelTextSuffix, 40);
        gd.addStringField("Log file suffix:", logSuffix, 40);

        gd.addCheckbox("Save images in PNG format (uncheck for JPEG format)", saveAllPNG);
        gd.addMessage("**Tip: make plot config and data table suffix the same so that the plot config\n" + "will auto-load when a new data table file is opened by drag and drop.");

        gd.showDialog();
        if (gd.wasCanceled()) return;
        saveImage = gd.getNextBoolean();
        savePlot = gd.getNextBoolean();
        saveSeeingProfile = gd.getNextBoolean();
        saveSeeingProfileStack = gd.getNextBoolean();
        saveConfig = gd.getNextBoolean();
        saveTable = gd.getNextBoolean();
        saveApertures = gd.getNextBoolean();
        saveFitPanels = gd.getNextBoolean();
        saveFitPanelText = gd.getNextBoolean();
        saveLog = gd.getNextBoolean();

        saveDataSubset = gd.getNextBoolean();
        showDataSubsetPanel = gd.getNextBoolean();

        imageSuffix = gd.getNextString();
        plotSuffix = gd.getNextString();
        seeingProfileSuffix = gd.getNextString();
        seeingProfileStackSuffix = gd.getNextString();
        configSuffix = gd.getNextString();
        dataSuffix = gd.getNextString();
        dataSubsetSuffix = gd.getNextString();
        aperSuffix = gd.getNextString();
        fitPanelSuffix = gd.getNextString();
        fitPanelTextSuffix = gd.getNextString();
        logSuffix = gd.getNextString();

        saveAllPNG = gd.getNextBoolean();

        Prefs.set("Astronomy_Tool.saveImage", saveImage);
        Prefs.set("Astronomy_Tool.savePlot", savePlot);
        Prefs.set("Astronomy_Tool.saveSeeingProfile", saveSeeingProfile);
        Prefs.set("Astronomy_Tool.saveSeeingProfileStack", saveSeeingProfileStack);
        Prefs.set("Astronomy_Tool.saveConfig", saveConfig);
        Prefs.set("Astronomy_Tool.saveTable", saveTable);
        Prefs.set("Astronomy_Tool.saveApertures", saveApertures);
        Prefs.set("Astronomy_Tool.saveFitPanels", saveFitPanels);
        Prefs.set("Astronomy_Tool.saveFitPanelText", saveFitPanelText);
        Prefs.set("Astronomy_Tool.saveLog", saveLog);
        Prefs.set("Astronomy_Tool.saveDataSubset", saveDataSubset);
        Prefs.set("Astronomy_Tool.showDataSubsetPanel", showDataSubsetPanel);

        Prefs.set("Astronomy_Tool.imageSuffix", imageSuffix);
        Prefs.set("Astronomy_Tool.plotSuffix", plotSuffix);
        Prefs.set("Astronomy_Tool.seeingProfileSuffix", seeingProfileSuffix);
        Prefs.set("Astronomy_Tool.seeingProfileStackSuffix", seeingProfileStackSuffix);
        Prefs.set("Astronomy_Tool.configSuffix", configSuffix);
        Prefs.set("Astronomy_Tool.dataSuffix", dataSuffix);
        Prefs.set("Astronomy_Tool.dataSubsetSuffix", dataSubsetSuffix);
        Prefs.set("Astronomy_Tool.aperSuffix", aperSuffix);
        Prefs.set("Astronomy_Tool.fitPanelSuffix", fitPanelSuffix);
        Prefs.set("Astronomy_Tool.fitPanelTextSuffix", fitPanelTextSuffix);
        Prefs.set("Astronomy_Tool.logSuffix", logSuffix);

        Prefs.set("Astronomy_Tool.saveAllPNG", saveAllPNG);

        if (gd.wasOKed()) {
            if (saveAllPNG) { saveAll("png", true); } else saveAll("jpg", true);
        }
    }


    static void saveAll(String format, boolean saveAll) {
        savePreferences();
        String outBase = "dataset";
        ImagePlus iplus = WindowManager.getCurrentImage();
        ImageWindow iw;
        ImageCanvas ic;
        AstroCanvas ac;
        BufferedImage imageDisplay = null;
        boolean imageFound = false;
        if (iplus != null) {
            iw = iplus.getWindow();
            if (iw instanceof AstroStackWindow) {
                WindowManager.toFront(iw);
                ic = iplus.getCanvas();
                ac = (AstroCanvas) ic;
                imageDisplay = new BufferedImage(ac.getSize().width, ac.getSize().height, BufferedImage.TYPE_INT_RGB);
                Graphics gg = imageDisplay.createGraphics();
                ac.paint(gg);
                gg.dispose();
                imageFound = true;
            } else if (WindowManager.getWindowCount() > 0) {
                int[] ID = WindowManager.getIDList();
                for (int i : ID) {
                    iplus = WindowManager.getImage(i);
                    if (iplus != null) { iw = iplus.getWindow(); } else iw = null;
                    if (iw instanceof AstroStackWindow) {
                        WindowManager.setCurrentWindow(iw);
                        WindowManager.toFront(iw);
                        ic = iplus.getCanvas();
                        ac = (AstroCanvas) ic;
                        imageDisplay = new BufferedImage(ac.getSize().width, ac.getSize().height, BufferedImage.TYPE_INT_RGB);
                        Graphics gg = imageDisplay.createGraphics();
                        ac.paint(gg);
                        gg.dispose();
                        imageFound = true;
                        break;
                    }
                }
            }
        }

        SaveDialog sf = new SaveDialog(saveAll ? "Save all" : "Save as " + format.toUpperCase(), imageFound ? iplus.getShortTitle() : MeasurementTable.shorterName(table.shortTitle()), "");
        if (sf.getDirectory() == null || sf.getFileName() == null) return;
        String outFileBase = sf.getFileName();
        int location = outFileBase.lastIndexOf('.');
        if (location > (outFileBase.length() - 5) || outFileBase.endsWith(".plotcfg") || outFileBase.endsWith(".apertures")) outFileBase = outFileBase.substring(0, location);
        outBase = sf.getDirectory() + outFileBase;
        String outPath = outBase;
        if (outBase.endsWith(imageSuffix)) {
            location = outBase.lastIndexOf(imageSuffix);
            if (location >= 0) outBase = outBase.substring(0, location);
        } else if (outBase.endsWith(plotSuffix)) {
            location = outBase.lastIndexOf(plotSuffix);
            if (location >= 0) outBase = outBase.substring(0, location);
        } else if (outBase.endsWith(seeingProfileSuffix)) {
            location = outBase.lastIndexOf(seeingProfileSuffix);
            if (location >= 0) outBase = outBase.substring(0, location);
        } else if (outBase.endsWith(configSuffix)) {
            location = outBase.lastIndexOf(configSuffix);
            if (location >= 0) outBase = outBase.substring(0, location);
        } else if (outBase.endsWith(dataSuffix)) {
            location = outBase.lastIndexOf(dataSuffix);
            if (location >= 0) outBase = outBase.substring(0, location);
        } else if (outBase.endsWith(dataSubsetSuffix)) {
            location = outBase.lastIndexOf(dataSubsetSuffix);
            if (location >= 0) outBase = outBase.substring(0, location);
        } else if (outBase.endsWith(aperSuffix)) {
            location = outBase.lastIndexOf(aperSuffix);
            if (location >= 0) outBase = outBase.substring(0, location);
        } else if (outBase.endsWith(logSuffix)) {
            location = outBase.lastIndexOf(logSuffix);
            if (location >= 0) outBase = outBase.substring(0, location);
        }

        if (imageFound && (!saveAll || (saveAll && saveImage))) {
            String imagePath = (saveAll ? outBase + imageSuffix : outPath) + "." + format;
            File saveFile = new File(imagePath);
            IJU.saveAsPngOrJpg(imageDisplay, saveFile, format);
        }

        if (saveAll && (savePlot || saveConfig || saveTable)) {
            saveDataImageConfig(savePlot, saveConfig, saveTable, true, format, outBase + plotSuffix + "." + format, outBase + configSuffix + ".plotcfg", outBase + dataSuffix + Prefs.get("options.ext", ".xls"));
        }

        if (saveSeeingProfile) {
            ImagePlus image = WindowManager.getImage("Seeing Profile");
            if (image != null) {
                String imagepath = outBase + seeingProfileSuffix + "." + format;
                image.setSlice(1);
                if (image.getStack() instanceof PlotVirtualStack && saveSeeingProfileStack) {
                    GifWriter.save(image, outBase + seeingProfileStackSuffix + ".gif");
                }

                if (format.equalsIgnoreCase("png")) {
                    IJ.runPlugIn(image, "ij.plugin.PNG_Writer", imagepath);
                } else if (format.equalsIgnoreCase("jpg")) {
                    IJ.runPlugIn(image, "ij.plugin.JpegWriter", imagepath);
                }
            }
        }

        if (saveAll && saveApertures) {
            IJU.saveApertures(outBase + aperSuffix + ".apertures");
        }
        if (saveAll && saveLog) {
            saveLogToFile(outBase + logSuffix + ".log");
        }
        if (saveAll && (saveFitPanels || saveFitPanelText)) {
            for (int c = 0; c < maxCurves; c++) {
                if (detrendFitIndex[c] == 9) {
                    if (saveFitPanels) {
                        BufferedImage bi = new BufferedImage(fitPanel[c].getSize().width, fitPanel[c].getSize().height, BufferedImage.TYPE_INT_RGB);
                        Graphics gg = bi.createGraphics();
                        fitPanel[c].paint(gg);
                        gg.dispose();
                        IJU.saveAsPngOrJpg(bi, new File(outBase + fitPanelSuffix + twoDigits.format(c + 1) + "_" + ylabel[c] + "." + format), format);
                    }
                    if (saveFitPanelText) {
                        saveFitPanelToTextFile(outBase + fitPanelTextSuffix + twoDigits.format(c + 1) + "_" + ylabel[c] + ".txt", c);
                    }
                }
            }
        }
        if (saveAll && saveDataSubset) {
            if (showDataSubsetPanel) {
                IJ.beep();
                saveDataSubsetDialog(outBase + dataSubsetSuffix + ".dat");
            } else {
                saveDataSubset(outBase + dataSubsetSuffix + ".dat");
            }
        }
    }

    static void saveLogToFile(String path) {
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
            for (String logline : loglines) {
                pw.println(logline);
            }
            pw.close();
        }
    }

//static void saveApertures(String apsPath)
//        {
//        File outFile = new File(apsPath);
//        if (outFile == null || outFile.isDirectory())
//            {
//            IJ.error("bad aperture save filename");
//            return;
//            }
//        if (outFile.isFile()) outFile.delete();
//        Properties prefs = new Properties();
//        Enumeration e = Prefs.ijPrefs.keys();
//        while (e.hasMoreElements()) {
//            String key = (String) e.nextElement();
//            if (key.startsWith(".aperture.radius") || key.startsWith(".aperture.rback1") ||
//                key.startsWith(".aperture.rback2") || key.startsWith(".aperture.removebackstars") ||
//                key.startsWith(".aperture.backplane") || key.startsWith(".multiaperture.usevarsizeap") ||
//                key.startsWith(".multiaperture.apfwhmfactor") || key.startsWith(".multiaperture.xapertures") ||
//                key.startsWith(".multiaperture.yapertures") || key.startsWith(".multiaperture.isrefstar") ||
//                key.startsWith(".multiaperture.naperturesmax"))
//                prefs.put(key, Prefs.ijPrefs.getProperty(key));
//        }
//        try
//            {
//            FileOutputStream fos = new FileOutputStream(apsPath);
//            BufferedOutputStream bos = new BufferedOutputStream(fos);
//            prefs.store(bos, "AstroImageJ Saved Apertures");
//            bos.close();
//            }
//        catch (IOException ioe)
//            {
//            IJ.beep();
//            IJ.showMessage("Error writing apertures to file");
//            }
//        }


//        static void setSaveStateDialog()
//            {
//            GenericDialog gd = new GenericDialog ("File suffix settings", mainFrame.getX()+mainFrame.getWidth()/2-165,
//                                                                          mainFrame.getY()+mainFrame.getHeight()/2-77);
//
//            plotSuffix = Prefs.get("Astronomy_Tool.plotSuffix", plotSuffix);
//            configSuffix = Prefs.get("Astronomy_Tool.configSuffix", configSuffix);
//            dataSuffix = Prefs.get("Astronomy_Tool.dataSuffix", dataSuffix);
//
//            gd.addMessage ("Enter save file suffix settings.");
//            gd.addStringField("Plot image suffix:", plotSuffix, 40);
//            gd.addStringField("Plot config file suffix:", configSuffix, 40);
//            gd.addStringField("Data table file suffix:", dataSuffix, 40);
//            gd.addMessage ("Tip: make plot config and data table suffix the same so that the plot config");
//            gd.addMessage ("will auto-load when a new data table file is opened by drag and drop.");
//
//            gd.showDialog();
//            if (gd.wasCanceled()) return;
//
//            plotSuffix = gd.getNextString();
//            configSuffix = gd.getNextString();
//            dataSuffix = gd.getNextString();
//
//            Prefs.set("Astronomy_Tool.plotSuffix", plotSuffix);
//            Prefs.set("Astronomy_Tool.configSuffix", configSuffix);
//            Prefs.set("Astronomy_Tool.dataSuffix", dataSuffix);
//            }

    static void openHelpPanel() {
        String filename = "help/multiplot_help.htm";
        new HelpPanel(filename, "Multi-plot");
    }

    static void openDataHelpPanel() {
        String filename = "help/multiplot_data_help.htm";
        new HelpPanel(filename, "Data Naming Convention");
    }


    public void keyTyped(KeyEvent e) {

    }


    public void keyPressed(KeyEvent e) {
        int keyCode = e.getKeyCode();
        if (keyCode == KeyEvent.VK_ALT) {
            //shiftIsDown = true;
        }
    }


    public void keyReleased(KeyEvent e) {
        int keyCode = e.getKeyCode();
        if (keyCode == KeyEvent.VK_ALT) {
            //shiftIsDown = false;
        }
    }

    public static void getPreferences() {
        tableName = Prefs.get("plot2.tableName", tableName);
        keepFileNamesOnAppend = Prefs.get("plot2.keepFileNamesOnAppend", keepFileNamesOnAppend);
        templateDir = Prefs.get("plot2.templateDir", templateDir);
        JDColumn = Prefs.get("plot2.JDColumn", JDColumn);
        raColumn = Prefs.get("plot2.raColumn", raColumn);
        decColumn = Prefs.get("plot2.decColumn", decColumn);

        addAirmass = Prefs.get("plot2.addAirmass", addAirmass);
        addAltitude = Prefs.get("plot2.addAltitude", addAltitude);
        addAzimuth = Prefs.get("plot2.addAzimuth", addAzimuth);
        addBJD = Prefs.get("plot2.addBJD", addBJD);
        addBJDCorr = Prefs.get("plot2.addBJDCorr", addBJDCorr);
        addDecNow = Prefs.get("plot2.addDecNow", addDecNow);
        addRaNow = Prefs.get("plot2.addRaNow", addRaNow);
        addDec2000 = Prefs.get("plot2.addDec2000", addDec2000);
        addRA2000 = Prefs.get("plot2.addRA2000", addRA2000);
        addGJD = Prefs.get("plot2.addGJD", addGJD);
        addHJD = Prefs.get("plot2.addHJD", addHJD);
        addHJDCorr = Prefs.get("plot2.addHJDCorr", addHJDCorr);
        addHourAngle = Prefs.get("plot2.addHourAngle", addHourAngle);
        addZenithDistance = Prefs.get("plot2.addZenithDistance", addZenithDistance);

        airmassName = Prefs.get("plot2.airmassName", airmassName);
        altitudeName = Prefs.get("plot2.altitudeName", altitudeName);
        azimuthName = Prefs.get("plot2.azimuthName", azimuthName);
        bjdName = Prefs.get("plot2.bjdName", bjdName);
        bjdCorrName = Prefs.get("plot2.bjdCorrName", bjdCorrName);
        decNowName = Prefs.get("plot2.decNowName", decNowName);
        raNowName = Prefs.get("plot2.raNowName", raNowName);
        dec2000Name = Prefs.get("plot2.dec2000Name", dec2000Name);
        ra2000Name = Prefs.get("plot2.ra2000Name", ra2000Name);
        gjdName = Prefs.get("plot2.gjdName", gjdName);
        hjdName = Prefs.get("plot2.hjdName", hjdName);
        hjdCorrName = Prefs.get("plot2.hjdCorrName", hjdCorrName);
        hourAngleName = Prefs.get("plot2.hourAngleName", hourAngleName);
        zenithDistanceName = Prefs.get("plot2.zenithDistanceName", zenithDistanceName);
        autoAstroDataUpdate = Prefs.get("plot2.autoAstroDataUpdate", autoAstroDataUpdate);

        useGJD = Prefs.get("plot2.useGJD", useGJD);
        useHJD = Prefs.get("plot2.useHJD", useHJD);
        useBJD = Prefs.get("plot2.useBJD", useBJD);
        unscale = Prefs.get("plot2.unscale", unscale);
        unshift = Prefs.get("plot2.unshift", unshift);
        useTableRaDec = Prefs.get("plot2.useTableRaDec", useTableRaDec);
        maxSubsetColumns = (int) Prefs.get("plot2.maxSubsetColumns", maxSubsetColumns);
        modifyCurvesAbove = Prefs.get("plot2.modifyCurvesAbove", modifyCurvesAbove);
        modifyCurvesBelow = Prefs.get("plot2.modifyCurvesBelow", modifyCurvesBelow);

        plotAutoMode = Prefs.get("plot.automode", plotAutoMode);
        xlabeldefault = Prefs.get("plot.xlabeldefault", xlabeldefault);
        title = Prefs.get("plot.title", title);
        useTitle = Prefs.get("plot.useTitle", useTitle);
        subtitle = Prefs.get("plot.subtitle", subtitle);
        useSubtitle = Prefs.get("plot.useSubtitle", useSubtitle);
        titlePosX = Prefs.get("plot.titlePosX", titlePosX);
        titlePosY = Prefs.get("plot.titlePosY", titlePosY);
        subtitlePosX = Prefs.get("plot.subtitlePosX", subtitlePosX);
        subtitlePosY = Prefs.get("plot.subtitlePosY", subtitlePosY);
        legendPosX = Prefs.get("plot.legendPosX", legendPosX);
        legendPosY = Prefs.get("plot.legendPosY", legendPosY);
        xTics = Prefs.get("plot.xTics", xTics);
        yTics = Prefs.get("plot.yTics", yTics);
        xGrid = Prefs.get("plot.xGrid", xGrid);
        yGrid = Prefs.get("plot.yGrid", yGrid);

        xNumbers = Prefs.get("plot.xNumbers", xNumbers);
        yNumbers = Prefs.get("plot.yNumbers", yNumbers);
        autoScaleX = Prefs.get("plot.autoScaleX", autoScaleX);
        useFirstX = Prefs.get("plot.useFirstX", useFirstX);
        autoScaleY = Prefs.get("plot.autoScaleY", autoScaleY);
        xMin = Prefs.get("plot.xMin", xMin);
        xMax = Prefs.get("plot.xMax", xMax);
        xWidth = Prefs.get("plot.xWidth", xWidth);
        yMin = Prefs.get("plot.yMin", yMin);
        yMax = Prefs.get("plot.yMax", yMax);
        plotSizeX = (int) Prefs.get("plot.plotSizeX", plotSizeX);
        plotSizeY = (int) Prefs.get("plot.plotSizeY", plotSizeY);
        mainFrameLocationX = (int) Prefs.get("plot2.mainFrameLocationX", mainFrameLocationX);
        mainFrameLocationY = (int) Prefs.get("plot2.mainFrameLocationY", mainFrameLocationY);
        subFrameLocationX = (int) Prefs.get("plot2.subFrameLocationX", subFrameLocationX);
        subFrameLocationY = (int) Prefs.get("plot2.subFrameLocationY", subFrameLocationY);
        refStarFrameLocationX = (int) Prefs.get("plot2.refStarFrameLocationX", refStarFrameLocationX);
        refStarFrameLocationY = (int) Prefs.get("plot2.refStarFrameLocationY", refStarFrameLocationY);
        addAstroDataFrameLocationX = (int) Prefs.get("plot2.addAstroDataFrameLocationX", addAstroDataFrameLocationX);
        addAstroDataFrameLocationY = (int) Prefs.get("plot2.addAstroDataFrameLocationY", addAstroDataFrameLocationY);
        plotFrameLocationX = (int) Prefs.get("plot2.plotFrameLocationX", plotFrameLocationX);
        plotFrameLocationY = (int) Prefs.get("plot2.plotFrameLocationY", plotFrameLocationY);
        openDataSetWindow = Prefs.get("plot2.openDataSetWindow", openDataSetWindow);
        openRefStarWindow = Prefs.get("plot2.openRefStarWindow", openRefStarWindow);
        openFitPanels = Prefs.get("plot2.openFitPanels", openFitPanels);
        addAstroDataFrameWasShowing = Prefs.get("plot2.addAstroDataFrameWasShowing", addAstroDataFrameWasShowing);
        rememberWindowLocations = Prefs.get("plot2.rememberWindowLocations", rememberWindowLocations);
        keepSeparateLocationsForFitWindows = Prefs.get("plot2.keepSeparateLocationsForFitWindows", keepSeparateLocationsForFitWindows);
        divideNotSubtract = Prefs.get("plot.divideNotSubtract", divideNotSubtract);
        usePixelScale = Prefs.get("plot.usePixelScale", usePixelScale);
        pixelScale = Prefs.get("plot.pixelScale", pixelScale);
        mmagrefs = (int) Prefs.get("plot.mmagrefs", mmagrefs);
        xExponent = (int) Prefs.get("plot.xExponent", xExponent);
        yExponent = (int) Prefs.get("plot.yExponent", yExponent);
        showToolTips = Prefs.get("astroIJ.showToolTips", showToolTips);
        ToolTipManager.sharedInstance().setEnabled(showToolTips);
        showXAxisNormal = Prefs.get("plot.showXAxisNormal", showXAxisNormal);
        showXAxisAsPhase = Prefs.get("plot.showXAxisAsPhase", showXAxisAsPhase);
        showXAxisAsHoursSinceTc = Prefs.get("plot.showXAxisAsHoursSinceTc", showXAxisAsHoursSinceTc);
        showXAxisAsDaysSinceTc = Prefs.get("plot.showXAxisAsDaysSinceTc", showXAxisAsDaysSinceTc);
        T0 = Prefs.get("plot.T0", T0);
        period = Prefs.get("plot.period", period);
        duration = Prefs.get("plot.duration", duration);
        T0Step = Prefs.get("plot.T0Step", T0Step);
        periodStep = Prefs.get("plot.periodStep", periodStep);
        durationStep = Prefs.get("plot.durationStep", durationStep);
        twoxPeriod = Prefs.get("plot.twoxPeriod", twoxPeriod);
        oddNotEven = Prefs.get("plot.oddNotEven", oddNotEven);
        yMaxStep = Prefs.get("plot.yMaxStep", yMaxStep);
        yMinStep = Prefs.get("plot.yMinStep", yMinStep);
        vMarker1Value = Prefs.get("plot.vMarker1Value", vMarker1Value);
        vMarker2Value = Prefs.get("plot.vMarker2Value", vMarker2Value);
        showVMarker1 = Prefs.get("plot.showVMarker1", showVMarker1);
        showVMarker2 = Prefs.get("plot.showVMarker2", showVMarker2);
        vMarker1TopText = Prefs.get("plot.vMarker1TopText", vMarker1TopText);
        vMarker1BotText = Prefs.get("plot.vMarker1BotText", vMarker1BotText);
        vMarker2TopText = Prefs.get("plot.vMarker2TopText", vMarker2TopText);
        vMarker2BotText = Prefs.get("plot.vMarker2BotText", vMarker2BotText);
        maxDetrendVars = (int) Prefs.get("plot.maxDetrendVars", 3);
        if (maxDetrendVars < 1) maxDetrendVars = 1;
        dMarker1Value = Prefs.get("plot.dMarker1Value", dMarker1Value);
        dMarker2Value = Prefs.get("plot.dMarker2Value", dMarker2Value);
        dMarker3Value = Prefs.get("plot.dMarker3Value", dMarker3Value);
        dMarker4Value = Prefs.get("plot.dMarker4Value", dMarker4Value);
        mfMarker1Value = Prefs.get("plot.mfMarker1Value", mfMarker1Value);
        xStep = Prefs.get("plot.xStep", xStep);
        useDMarker1 = Prefs.get("plot.useDMarker1", useDMarker1);
        useDMarker4 = Prefs.get("plot.useDMarker4", useDMarker4);
        showDMarkers = Prefs.get("plot.showDMarkers", showDMarkers);
        showMFMarkers = Prefs.get("plot.showMFMarkers", showMFMarkers);
        invertYAxis = Prefs.get("plot.invertYAxis", invertYAxis);
        invertYAxisSign = invertYAxis ? -1 : 1;
        negateMag = Prefs.get("plot.negateMag", negateMag);
        saveNewXColumn = Prefs.get("plot.saveNewXColumn", saveNewXColumn);
        saveNewYColumn = Prefs.get("plot.saveNewYColumn", saveNewYColumn);
        saveNewYErrColumn = Prefs.get("plot.saveNewYErrColumn", saveNewYErrColumn);
        plotSizeXStep = (int) Prefs.get("plot.plotSizeXStep", plotSizeXStep);
        plotSizeYStep = (int) Prefs.get("plot.plotSizeYStep", plotSizeYStep);
        excludedHeadSamples = (int) Prefs.get("plot.excludedHeadSamples", excludedHeadSamples);
        excludedTailSamples = (int) Prefs.get("plot.excludedTailSamples", excludedTailSamples);
        excludedHeadSamplesStep = (int) Prefs.get("plot.excludedHeadSamplesStep", excludedHeadSamplesStep);
        excludedTailSamplesStep = (int) Prefs.get("plot.excludedTailSamplesStep", excludedTailSamplesStep);
        refStarHorzWidth = (int) Prefs.get("plot.refStarHorzWidth", refStarHorzWidth);
        if (refStarHorzWidth < 1) refStarHorzWidth = 1;
        useXColumnName = Prefs.get("plot.useXColumnName", useXColumnName);
        useYColumnName = Prefs.get("plot.useYColumnName", useYColumnName);
        useXCustomName = Prefs.get("plot.useXCustomName", useXCustomName);
        useYCustomName = Prefs.get("plot.useYCustomName", useYCustomName);
        xLegend = Prefs.get("plot.xLegend", xLegend);
        yLegend = Prefs.get("plot.yLegend", yLegend);
        showXScaleInfo = Prefs.get("plot.showXScaleInfo", showXScaleInfo);
        showYScaleInfo = Prefs.get("plot.showYScaleInfo", showYScaleInfo);
        showYShiftInfo = Prefs.get("plot.showYShiftInfo", showYShiftInfo);
        showLScaleInfo = Prefs.get("plot.showLScaleInfo", showLScaleInfo);
        showLRelScaleInfo = Prefs.get("plot.showLRelScaleInfo", showLRelScaleInfo);
        showLShiftInfo = Prefs.get("plot.showLShiftInfo", showLShiftInfo);
        showLRelShiftInfo = Prefs.get("plot.showLRelShiftInfo", showLRelShiftInfo);
        showYAvgInfo = Prefs.get("plot.showYBinInfo", showYAvgInfo);
        showLAvgInfo = Prefs.get("plot.showLBinInfo", showLAvgInfo);
        showOutBinRms = Prefs.get("plot.showOutBinRms", showOutBinRms);
        showYmmagInfo = Prefs.get("plot.showYmmagInfo", showYmmagInfo);
        showLmmagInfo = Prefs.get("plot.showLmmagInfo", showLmmagInfo);
        useNelderMeadChi2ForDetrend = Prefs.get("plot.useNelderMeadChi2ForDetrend", useNelderMeadChi2ForDetrend);
        showLdetrendInfo = Prefs.get("plot.showLdetrendInfo", showLdetrendInfo);
        showLnormInfo = Prefs.get("plot.showLnormInfo", showLnormInfo);
        showYSymbolInfo = Prefs.get("plot.showYSymbolInfo", showYSymbolInfo);
        showLSymbolInfo = Prefs.get("plot.showLSymbolInfo", showLSymbolInfo);
        showSigmaForAllCurves = Prefs.get("plot.showSigmaForAllCurves", showSigmaForAllCurves);
        showSigmaForDetrendedCurves = Prefs.get("plot.showSigmaForDetrendedCurves", showSigmaForDetrendedCurves);
//                useTwoLineLegend=Prefs.get("plot.useTwoLineLegend", useTwoLineLegend);
        useWideDataPanel = Prefs.get("plot2.useWideDataPanel", useWideDataPanel);
        useBoldedDatum = Prefs.get("plot2.useBoldedDatum", useBoldedDatum);
        useUpdateStack = Prefs.get("plot2.useUpdateStack", useUpdateStack);
        legendLeft = Prefs.get("plot.legendLeft", legendLeft);
        legendRight = Prefs.get("plot.legendRight", legendRight);
        priorityColumns = Prefs.get("plot.priorityColumns", priorityColumns);
        priorityDetrendColumns = Prefs.get("plot.priorityDetrendColumns", priorityDetrendColumns);
        prioritizeColumns = Prefs.get("plot.prioritizeColumns", prioritizeColumns);
        maxColumnLength = (int) Prefs.get("plot.maxColumnLength", 1000);

        saveImage = Prefs.get("Astronomy_Tool.saveImage", saveImage);
        savePlot = Prefs.get("Astronomy_Tool.savePlot", savePlot);
        saveSeeingProfile = Prefs.get("Astronomy_Tool.saveSeeingProfile", saveSeeingProfile);
        saveSeeingProfileStack = Prefs.get("Astronomy_Tool.saveSeeingProfileStack", saveSeeingProfileStack);
        saveConfig = Prefs.get("Astronomy_Tool.saveConfig", saveConfig);
        saveTable = Prefs.get("Astronomy_Tool.saveTable", saveTable);
        saveApertures = Prefs.get("Astronomy_Tool.saveApertures", saveApertures);
        saveFitPanels = Prefs.get("Astronomy_Tool.saveFitPanels", saveFitPanels);
        saveFitPanelText = Prefs.get("Astronomy_Tool.saveFitPanelText", saveFitPanelText);
        saveLog = Prefs.get("Astronomy_Tool.saveLog", saveLog);

        saveDataSubset = Prefs.get("Astronomy_Tool.saveDataSubset", saveDataSubset);
        showDataSubsetPanel = Prefs.get("Astronomy_Tool.showDataSubsetPanel", showDataSubsetPanel);

        imageSuffix = Prefs.get("Astronomy_Tool.imageSuffix", imageSuffix);
        plotSuffix = Prefs.get("Astronomy_Tool.plotSuffix", plotSuffix);
        seeingProfileSuffix = Prefs.get("Astronomy_Tool.seeingProfileSuffix", seeingProfileSuffix);
        seeingProfileStackSuffix = Prefs.get("Astronomy_Tool.seeingProfileStackSuffix", seeingProfileStackSuffix);
        configSuffix = Prefs.get("Astronomy_Tool.configSuffix", configSuffix);
        dataSuffix = Prefs.get("Astronomy_Tool.dataSuffix", dataSuffix);
        dataSubsetSuffix = Prefs.get("Astronomy_Tool.dataSubsetSuffix", dataSubsetSuffix);
        aperSuffix = Prefs.get("Astronomy_Tool.aperSuffix", aperSuffix);
        fitPanelSuffix = Prefs.get("Astronomy_Tool.fitPanelSuffix", fitPanelSuffix);
        fitPanelTextSuffix = Prefs.get("Astronomy_Tool.fitPanelTextSuffix", fitPanelTextSuffix);
        logSuffix = Prefs.get("Astronomy_Tool.logSuffix", logSuffix);

        saveAllPNG = Prefs.get("Astronomy_Tool.saveAllPNG", saveAllPNG);

        showSaturationWarning = Prefs.get(Aperture_.AP_PREFS_SHOWSATWARNING, showSaturationWarning);
        forceAbsMagDisplay = Prefs.get("plot2.forceAbsMagDisplay", forceAbsMagDisplay);
        saturationWarningLevel = Prefs.get(Aperture_.AP_PREFS_SATWARNLEVEL, saturationWarningLevel);
        showLinearityWarning = Prefs.get(Aperture_.AP_PREFS_SHOWLINWARNING, showLinearityWarning);
        linearityWarningLevel = Prefs.get(Aperture_.AP_PREFS_LINWARNLEVEL, linearityWarningLevel);

        orbitalPeriodStep = Prefs.get("plot.orbitalPeriodStep", orbitalPeriodStep);
        eccentricityStep = Prefs.get("plot.eccentricityStep", eccentricityStep);
        omegaStep = Prefs.get("plot.omegaStep", omegaStep);
        teffStep = Prefs.get("plot.teffStep", teffStep);
        jminuskStep = Prefs.get("plot.jminuskStep", jminuskStep);
        mStarStep = Prefs.get("plot.mStarStep", mStarStep);
        rStarStep = Prefs.get("plot.rStarStep", rStarStep);
        rhoStarStep = Prefs.get("plot.rhoStarStep", rhoStarStep);
//                residualShiftStep=Prefs.get("plot.residualShiftStep", residualShiftStep);

        for (int i = 0; i < (maxFittedVars); i++) {
            priorCenterStep[i] = Prefs.get("plot.priorCenterStep[" + i + "]", priorCenterStep[i]);
            priorWidthStep[i] = Prefs.get("plot.priorWidthStep[" + i + "]", priorWidthStep[i]);
            fitStepStep[i] = Prefs.get("plot.fitStepStep[" + i + "]", fitStepStep[i]);
        }

        for (int i = 0; i < maxCurves; i++) {
            fitFrameLocationX[i] = (int) Prefs.get("plot2.fitFrameLocationX" + i, fitFrameLocationX[i]);
            fitFrameLocationY[i] = (int) Prefs.get("plot2.fitFrameLocationY" + i, fitFrameLocationY[i]);
            orbitalPeriod[i] = Prefs.get("plot.orbitalPeriod" + i, orbitalPeriod[i]);
            bp[i] = Prefs.get("plot.bp" + i, bp[i]);
            eccentricity[i] = Prefs.get("plot.eccentricity" + i, eccentricity[i]);
            omega[i] = Prefs.get("plot.omega" + i, omega[i]);
            teff[i] = Prefs.get("plot.teff" + i, teff[i]);
            forceCircularOrbit[i] = Prefs.get("plot.forceCircularOrbit" + i, forceCircularOrbit[i]);
            tolerance[i] = Prefs.get("plot.tolerance" + i, tolerance[i]);
            residualShift[i] = Prefs.get("plot.residualShift" + i, residualShift[i]);
            autoResidualShift[i] = Prefs.get("plot.autoResidualShift" + i, autoResidualShift[i]);
            maxFitSteps[i] = (int) Prefs.get("plot.maxFitSteps" + i, maxFitSteps[i]);
            modelLineWidth[i] = (int) Prefs.get("plot.modelLineWidth" + i, modelLineWidth[i]);
            residualLineWidth[i] = (int) Prefs.get("plot.residualLineWidth" + i, residualLineWidth[i]);
            useTransitFit[i] = Prefs.get("plot.useTransitFit" + i, useTransitFit[i]);
            showLTranParams[i] = Prefs.get("plot.showLTranParams" + i, showLTranParams[i]);
            showLResidual[i] = Prefs.get("plot.showLResidual" + i, showLResidual[i]);
            autoUpdateFit[i] = Prefs.get("plot.autoUpdateFit" + i, autoUpdateFit[i]);
            showModel[i] = Prefs.get("plot.showModel" + i, showModel[i]);
            showResidual[i] = Prefs.get("plot.showResidual" + i, showResidual[i]);
            showResidualError[i] = Prefs.get("plot.showResidualError" + i, showResidualError[i]);
            autoUpdatePriors[i] = Prefs.get("plot.autoUpdatePriors" + i, autoUpdatePriors[i]);
            autoUpdatePrior[i][0] = Prefs.get("plot.autoUpdatePrior[" + i + "][0]", autoUpdatePrior[i][0]);
            autoUpdatePrior[i][1] = Prefs.get("plot.autoUpdatePrior[" + i + "][1]", autoUpdatePrior[i][1]);
            autoUpdatePrior[i][2] = Prefs.get("plot.autoUpdatePrior[" + i + "][2]", autoUpdatePrior[i][2]);
            autoUpdatePrior[i][3] = Prefs.get("plot.autoUpdatePrior[" + i + "][3]", autoUpdatePrior[i][3]);
            autoUpdatePrior[i][4] = Prefs.get("plot.autoUpdatePrior[" + i + "][4]", autoUpdatePrior[i][4]);
            bpLock[i] = Prefs.get("plot.bpLock[" + i + "]", bpLock[i]);

            moreOptions[i] = Prefs.get("plot.moreOptions" + i, moreOptions[i]);
            oplabel[i] = Prefs.get("plot.oplabel" + i, oplabel[i]);
            xlabel[i] = Prefs.get("plot.xlabel" + i, xlabel[i]);
            ylabel[i] = Prefs.get("plot.ylabel" + i, ylabel[i]);
            lines[i] = Prefs.get("plot.lines" + i, lines[i]);
            smooth[i] = Prefs.get("plot.smooth" + i, smooth[i]);
            smoothLen[i] = (int) Prefs.get("plot.smoothLen" + i, smoothLen[i]);
            if (smoothLen[i] < 1) smoothLen[i] = 31;
            markerIndex[i] = (int) Prefs.get("plot.markerIndex" + i, markerIndex[i]);
            residualSymbolIndex[i] = (int) Prefs.get("plot.residualSymbolIndex" + i, residualSymbolIndex[i]);
            colorIndex[i] = (int) Prefs.get("plot.colorIndex" + i, colorIndex[i]);
            modelColorIndex[i] = (int) Prefs.get("plot.modelColorIndex" + i, modelColorIndex[i]);
            residualModelColorIndex[i] = (int) Prefs.get("plot.residualModelColorIndex" + i, residualModelColorIndex[i]);
            residualColorIndex[i] = (int) Prefs.get("plot.residualColorIndex" + i, residualColorIndex[i]);
            normIndex[i] = (int) Prefs.get("plot.normIndex" + i, normIndex[i]);
            detrendVarDisplayed[i] = (int) Prefs.get("plot.detrendVarDisplayed" + i, detrendVarDisplayed[i]);
            if (detrendVarDisplayed[i] >= maxDetrendVars) detrendVarDisplayed[i] = maxDetrendVars - 1;
            if (detrendVarDisplayed[i] < 0) detrendVarDisplayed[i] = 0;
            detrendFitIndex[i] = (int) Prefs.get("plot.detrendFitIndex" + i, detrendFitIndex[i]);
            mmag[i] = Prefs.get("plot.mmag" + i, mmag[i]);
            fromMag[i] = Prefs.get("plot.fromMag" + i, fromMag[i]);
            useColumnName[i] = Prefs.get("plot.useColumnName" + i, useColumnName[i]);
            useLegend[i] = Prefs.get("plot.useLegend" + i, useLegend[i]);
            legend[i] = Prefs.get("plot.legend" + i, legend[i]);
            operatorIndex[i] = (int) Prefs.get("plot.operatorIndex" + i, operatorIndex[i]);
            inputAverageOverSize[i] = (int) Prefs.get("plot.inputAverageOverSize" + i, inputAverageOverSize[i]);
            plotY[i] = Prefs.get("plot.plotY" + i, plotY[i]);
            force[i] = Prefs.get("plot.force" + i, force[i]);
            showErrors[i] = Prefs.get("plot.showErrors" + i, showErrors[i]);
            autoScaleFactor[i] = Prefs.get("plot.autoScaleFactor" + i, autoScaleFactor[i]);
            if (Double.isNaN(autoScaleFactor[i]) || Double.isInfinite(autoScaleFactor[i])) autoScaleFactor[i] = 1.0;
            autoScaleStep[i] = Prefs.get("plot.autoScaleStep" + i, autoScaleStep[i]);
            autoShiftFactor[i] = Prefs.get("plot.autoShiftFactor" + i, autoShiftFactor[i]);
            if (Double.isNaN(autoShiftFactor[i]) || Double.isInfinite(autoShiftFactor[i])) autoShiftFactor[i] = 0.0;
            autoShiftStep[i] = Prefs.get("plot.autoShiftStep" + i, autoShiftStep[i]);
            customScaleFactor[i] = Prefs.get("plot.customScaleFactor" + i, customScaleFactor[i]);
            if (Double.isNaN(customScaleFactor[i]) || Double.isInfinite(customScaleFactor[i])) {
                customScaleFactor[i] = 1.0;
            }
            customScaleStep[i] = Prefs.get("plot.customScaleStep" + i, customScaleStep[i]);
            customShiftFactor[i] = Prefs.get("plot.customShiftFactor" + i, customShiftFactor[i]);
            if (Double.isNaN(customShiftFactor[i]) || Double.isInfinite(customShiftFactor[i])) {
                customShiftFactor[i] = 0.0;
            }
            customShiftStep[i] = Prefs.get("plot.customShiftStep" + i, customShiftStep[i]);
            ASInclude[i] = Prefs.get("plot.ASInclude" + i, ASInclude[i]);
            for (int v = 0; v < maxDetrendVars; v++) {
                detrendlabel[i][v] = Prefs.get("plot.detrendlabel[" + i + "][" + v + "]", detrendlabel[i][v]);
                detrendlabelhold[i][v] = Prefs.get("plot.detrendlabelhold[" + i + "][" + v + "]", detrendlabelhold[i][v]);
                detrendFactor[i][v] = Prefs.get("plot.detrendFactor[" + i + "][" + v + "]", detrendFactor[i][v]);
                detrendFactorStep[i][v] = Prefs.get("plot.detrendFactorStep[" + i + "][" + v + "]", detrendFactorStep[i][v]);
            }
            for (int v = 0; v < (maxFittedVars); v++) {
                lockToCenter[i][v] = Prefs.get("plot.lockToCenter[" + i + "][" + v + "]", lockToCenter[i][v]);
                priorCenter[i][v] = Prefs.get("plot.priorCenter[" + i + "][" + v + "]", priorCenter[i][v]);
                priorWidth[i][v] = Prefs.get("plot.priorWidth[" + i + "][" + v + "]", priorWidth[i][v]);
                usePriorWidth[i][v] = Prefs.get("plot.usePriorWidth[" + i + "][" + v + "]", usePriorWidth[i][v]);
                useCustomFitStep[i][v] = Prefs.get("plot.useCustomFitStep[" + i + "][" + v + "]", useCustomFitStep[i][v]);
                fitStep[i][v] = Prefs.get("plot.fitStep[" + i + "][" + v + "]", fitStep[i][v]);
            }
            if (i < minutes.size()) {
                minutes.set(i, minutes.get(i).setFirst(Prefs.get("plot.displayBinMinutes[" + i +"]", minutes.get(i).first())));
                minutes.get(i).second().setValue(minutes.get(i).first());
            } else {
                minutes.add(new Pair.GenericPair<>(Prefs.get("plot.displayBinMinutes[" + i +"]", 5), null));
            }
            binDisplay[i] = Prefs.get("plot.displayBin[" + i +"]", binDisplay[i]);
        }
    }

    static void saveAstroPanelPrefs() {
        Prefs.set("plot2.addAirmass", addAirmass);
        Prefs.set("plot2.addAltitude", addAltitude);
        Prefs.set("plot2.addAzimuth", addAzimuth);
        Prefs.set("plot2.addBJD", addBJD);
        Prefs.set("plot2.addBJDCorr", addBJDCorr);
        Prefs.set("plot2.addDecNow", addDecNow);
        Prefs.set("plot2.addRaNow", addRaNow);
        Prefs.set("plot2.addDec2000", addDec2000);
        Prefs.set("plot2.addRA2000", addRA2000);
        Prefs.set("plot2.addGJD", addGJD);
        Prefs.set("plot2.addHJD", addHJD);
        Prefs.set("plot2.addHJDCorr", addHJDCorr);
        Prefs.set("plot2.addHourAngle", addHourAngle);
        Prefs.set("plot2.addZenithDistance", addZenithDistance);

        Prefs.set("plot2.airmassName", airmassName);
        Prefs.set("plot2.altitudeName", altitudeName);
        Prefs.set("plot2.azimuthName", azimuthName);
        Prefs.set("plot2.bjdName", bjdName);
        Prefs.set("plot2.bjdCorrName", bjdCorrName);
        Prefs.set("plot2.decNowName", decNowName);
        Prefs.set("plot2.raNowName", raNowName);
        Prefs.set("plot2.dec2000Name", dec2000Name);
        Prefs.set("plot2.ra2000Name", ra2000Name);
        Prefs.set("plot2.gjdName", gjdName);
        Prefs.set("plot2.hjdName", hjdName);
        Prefs.set("plot2.hjdCorrName", hjdCorrName);
        Prefs.set("plot2.hourAngleName", hourAngleName);
        Prefs.set("plot2.zenithDistanceName", zenithDistanceName);
        Prefs.set("plot2.JDColumn", JDColumn);
        Prefs.set("plot2.raColumn", raColumn);
        Prefs.set("plot2.decColumn", decColumn);
        Prefs.set("plot2.useGJD", useGJD);
        Prefs.set("plot2.useHJD", useHJD);
        Prefs.set("plot2.useBJD", useBJD);
        Prefs.set("plot2.unscale", unscale);
        Prefs.set("plot2.unshift", unshift);
        Prefs.set("plot2.useTableRaDec", useTableRaDec);
        Prefs.set("plot2.autoAstroDataUpdate", autoAstroDataUpdate);
    }


    static void savePreferences() {
        FitOptimization.savePrefs();
        Prefs.set("plot2.tableName", tableName);
        Prefs.set("plot2.keepFileNamesOnAppend", keepFileNamesOnAppend);
        Prefs.set("plot2.templateDir", templateDir);
        Prefs.set("plot2.modifyCurvesAbove", modifyCurvesAbove);
        Prefs.set("plot2.modifyCurvesBelow", modifyCurvesBelow);
        Prefs.set("plot2.maxSubsetColumns", maxSubsetColumns);
        Prefs.set("plot2.forceAbsMagDisplay", forceAbsMagDisplay);
        saveAstroPanelPrefs();
        Prefs.set("plot.maxDetrendVars", maxDetrendVars);
        Prefs.set("plot2.useDefaultSettings", useDefaultSettings);
        Prefs.set("plot.xlabeldefault", xlabeldefault);
        Prefs.set("plot.xTics", xTics);
        Prefs.set("plot.yTics", yTics);
        Prefs.set("plot.xGrid", xGrid);
        Prefs.set("plot.yGrid", yGrid);
        Prefs.set("plot.xNumbers", xNumbers);
        Prefs.set("plot.yNumbers", yNumbers);
        Prefs.set("plot.autoScaleX", autoScaleX);
        Prefs.set("plot.autoScaleY", autoScaleY);
        Prefs.set("plot.useFirstX", useFirstX);
        Prefs.set("plot.xWidth", xWidth);
        Prefs.set("plot.xMin", xMin);
        Prefs.set("plot.xMax", xMax);
        Prefs.set("plot.yMin", yMin);
        Prefs.set("plot.yMax", yMax);
        Prefs.set("plot.vMarker1Value", vMarker1Value);
        Prefs.set("plot.vMarker2Value", vMarker2Value);
        Prefs.set("plot.showVMarker1", showVMarker1);
        Prefs.set("plot.showVMarker2", showVMarker2);
        Prefs.set("plot.vMarker1TopText", vMarker1TopText);
        Prefs.set("plot.vMarker1BotText", vMarker1BotText);
        Prefs.set("plot.vMarker2TopText", vMarker2TopText);
        Prefs.set("plot.vMarker2BotText", vMarker2BotText);
        Prefs.set("plot.dMarker1Value", dMarker1Value);
        Prefs.set("plot.dMarker2Value", dMarker2Value);
        Prefs.set("plot.dMarker3Value", dMarker3Value);
        Prefs.set("plot.dMarker4Value", dMarker4Value);
        Prefs.set("plot.mfMarker1Value", mfMarker1Value);
        Prefs.set("plot.xStep", xStep);
        Prefs.set("plot.useDMarker1", useDMarker1);
        Prefs.set("plot.useDMarker4", useDMarker4);
        Prefs.set("plot.showDMarkers", showDMarkers);
        Prefs.set("plot.showMFMarkers", showMFMarkers);
        Prefs.set("plot.invertYAxis", invertYAxis);
        Prefs.set("plot.negateMag", negateMag);
        Prefs.set("plot.saveNewXColumn", saveNewXColumn);
        Prefs.set("plot.saveNewYColumn", saveNewYColumn);
        Prefs.set("plot.saveNewYErrColumn", saveNewYErrColumn);
        Prefs.set("plot.plotSizeX", plotSizeX);
        Prefs.set("plot.plotSizeY", plotSizeY);
        Prefs.set("plot.title", title);
        Prefs.set("plot.useTitle", useTitle);
        Prefs.set("plot.subtitle", subtitle);
        Prefs.set("plot.useSubtitle", useSubtitle);
        Prefs.set("plot.titlePosX", titlePosX);
        Prefs.set("plot.titlePosY", titlePosY);
        Prefs.set("plot.subtitlePosX", subtitlePosX);
        Prefs.set("plot.subtitlePosY", subtitlePosY);
        Prefs.set("plot2.openDataSetWindow", openDataSetWindow);
        Prefs.set("plot2.openRefStarWindow", openRefStarWindow);
        Prefs.set("plot2.openFitPanels", openFitPanels);
        Prefs.set("plot2.addAstroDataFrameWasShowing", addAstroDataFrameWasShowing);
        Prefs.set("plot.legendPosX", legendPosX);
        Prefs.set("plot.legendPosY", legendPosY);
        Prefs.set("plot2.rememberWindowLocations", rememberWindowLocations);
        Prefs.set("plot2.keepSeparateLocationsForFitWindows", keepSeparateLocationsForFitWindows);
        Prefs.set("plot.divideNotSubtract", divideNotSubtract);
        Prefs.set("plot.usePixelScale", usePixelScale);
        Prefs.set("plot.pixelScale", pixelScale);
        Prefs.set("plot.mmagrefs", mmagrefs);
        Prefs.set("plot.xExponent", xExponent);
        Prefs.set("plot.yExponent", yExponent);
        Prefs.set("plot.yMaxStep", yMaxStep);
        Prefs.set("plot.yMinStep", yMinStep);
        Prefs.set("plot.plotSizeXStep", plotSizeXStep);
        Prefs.set("plot.plotSizeYStep", plotSizeYStep);
        Prefs.set("plot.excludedHeadSamples", excludedHeadSamples);
        Prefs.set("plot.excludedTailSamples", excludedTailSamples);
        Prefs.set("plot.excludedHeadSamplesStep", excludedHeadSamplesStep);
        Prefs.set("plot.excludedTailSamplesStep", excludedTailSamplesStep);
        Prefs.set("plot.refStarHorzWidth", refStarHorzWidth);
        Prefs.set("plot.useXColumnName", useXColumnName);
        Prefs.set("plot.useYColumnName", useYColumnName);
        Prefs.set("plot.useXCustomName", useXCustomName);
        Prefs.set("plot.useYCustomName", useYCustomName);
        Prefs.set("plot.xLegend", xLegend);
        Prefs.set("plot.yLegend", yLegend);
        Prefs.set("plot.showXAxisNormal", showXAxisNormal);
        Prefs.set("plot.showXAxisAsPhase", showXAxisAsPhase);
        Prefs.set("plot.showXAxisAsHoursSinceTc", showXAxisAsHoursSinceTc);
        Prefs.set("plot.showXAxisAsDaysSinceTc", showXAxisAsDaysSinceTc);
        Prefs.set("plot.T0", T0);
        Prefs.set("plot.period", period);
        Prefs.set("plot.duration", duration);
        Prefs.set("plot.T0Step", T0Step);
        Prefs.set("plot.periodStep", periodStep);
        Prefs.set("plot.durationStep", durationStep);
        Prefs.set("plot.twoxPeriod", twoxPeriod);
        Prefs.set("plot.oddNotEven", oddNotEven);
        Prefs.set("plot.showXScaleInfo", showXScaleInfo);
        Prefs.set("plot.showYScaleInfo", showYScaleInfo);
        Prefs.set("plot.showYShiftInfo", showYShiftInfo);
        Prefs.set("plot.showLScaleInfo", showLScaleInfo);
        Prefs.set("plot.showLRelScaleInfo", showLRelScaleInfo);
        Prefs.set("plot.showLShiftInfo", showLShiftInfo);
        Prefs.set("plot.showLRelShiftInfo", showLRelShiftInfo);
        Prefs.set("plot.showYBinInfo", showYAvgInfo);
        Prefs.set("plot.showLBinInfo", showLAvgInfo);
        Prefs.set("plot.showOutBinRms", showOutBinRms);
        Prefs.set("plot.showYmmagInfo", showYmmagInfo);
        Prefs.set("plot.showLmmagInfo", showLmmagInfo);
        Prefs.set("plot.useNelderMeadChi2ForDetrend", useNelderMeadChi2ForDetrend);
        Prefs.set("plot.showLdetrendInfo", showLdetrendInfo);
        Prefs.set("plot.showLnormInfo", showLnormInfo);
        Prefs.set("plot.showYSymbolInfo", showYSymbolInfo);
        Prefs.set("plot.showLSymbolInfo", showLSymbolInfo);
        Prefs.set("plot.showSigmaForAllCurves", showSigmaForAllCurves);
        Prefs.set("plot.showSigmaForDetrendedCurves", showSigmaForDetrendedCurves);
        Prefs.set("plot2.useWideDataPanel", useWideDataPanel);
//                Prefs.set("plot.useTwoLineLegend", useTwoLineLegend);
        Prefs.set("plot2.useBoldedDatum", useBoldedDatum);
        Prefs.set("plot2.useUpdateStack", useUpdateStack);
        Prefs.set("plot.legendLeft", legendLeft);
        Prefs.set("plot.legendRight", legendRight);
        Prefs.set("plot.priorityColumns", priorityColumns);
        Prefs.set("plot.priorityDetrendColumns", priorityDetrendColumns);
        Prefs.set("plot.prioritizeColumns", prioritizeColumns);

        Prefs.set("Astronomy_Tool.saveImage", saveImage);
        Prefs.set("Astronomy_Tool.savePlot", savePlot);
        Prefs.set("Astronomy_Tool.saveSeeingProfile", saveSeeingProfile);
        Prefs.set("Astronomy_Tool.saveSeeingProfileStack", saveSeeingProfileStack);
        Prefs.set("Astronomy_Tool.saveConfig", saveConfig);
        Prefs.set("Astronomy_Tool.saveTable", saveTable);
        Prefs.set("Astronomy_Tool.saveApertures", saveApertures);
        Prefs.set("Astronomy_Tool.saveLog", saveLog);
        Prefs.set("Astronomy_Tool.saveFitPanels", saveFitPanels);
        Prefs.set("Astronomy_Tool.saveFitPanelText", saveFitPanelText);
        Prefs.set("Astronomy_Tool.saveDataSubset", saveDataSubset);
        Prefs.set("Astronomy_Tool.showDataSubsetPanel", showDataSubsetPanel);
        Prefs.set("Astronomy_Tool.imageSuffix", imageSuffix);
        Prefs.set("Astronomy_Tool.plotSuffix", plotSuffix);
        Prefs.set("Astronomy_Tool.seeingProfileSuffix", seeingProfileSuffix);
        Prefs.set("Astronomy_Tool.seeingProfileStackSuffix", seeingProfileStackSuffix);
        Prefs.set("Astronomy_Tool.configSuffix", configSuffix);
        Prefs.set("Astronomy_Tool.dataSuffix", dataSuffix);
        Prefs.set("Astronomy_Tool.aperSuffix", aperSuffix);
        Prefs.set("Astronomy_Tool.logSuffix", logSuffix);
        Prefs.set("Astronomy_Tool.fitPanelSuffix", fitPanelSuffix);
        Prefs.set("Astronomy_Tool.fitPanelTextSuffix", fitPanelTextSuffix);
        Prefs.set("Astronomy_Tool.dataSubsetSuffix", dataSubsetSuffix);
        Prefs.set("Astronomy_Tool.saveAllPNG", saveAllPNG);

        Prefs.set("plot.orbitalPeriodStep", orbitalPeriodStep);
        Prefs.set("plot.eccentricityStep", eccentricityStep);
        Prefs.set("plot.omegaStep", omegaStep);
        Prefs.set("plot.teffStep", teffStep);
        Prefs.set("plot.jminuskStep", jminuskStep);
        Prefs.set("plot.mStarStep", mStarStep);
        Prefs.set("plot.rStarStep", rStarStep);
        Prefs.set("plot.rhoStarStep", rhoStarStep);
//                Prefs.set("plot.residualShiftStep", residualShiftStep);

        for (int i = 0; i < (maxFittedVars); i++) {
            Prefs.set("plot.priorCenterStep[" + i + "]", priorCenterStep[i]);
            Prefs.set("plot.priorWidthStep[" + i + "]", priorWidthStep[i]);
            Prefs.set("plot.fitStepStep[" + i + "]", fitStepStep[i]);
        }

        for (int i = 0; i < maxCurves; i++) {
            Prefs.set("plot2.fitFrameLocationX" + i, fitFrameLocationX[i]);
            Prefs.set("plot2.fitFrameLocationY" + i, fitFrameLocationY[i]);
            Prefs.set("plot.orbitalPeriod" + i, orbitalPeriod[i]);
            Prefs.set("plot.bp" + i, bp[i]);
            Prefs.set("plot.eccentricity" + i, eccentricity[i]);
            Prefs.set("plot.omega" + i, omega[i]);
            Prefs.set("plot.teff" + i, teff[i]);
            Prefs.set("plot.forceCircularOrbit" + i, forceCircularOrbit[i]);
            Prefs.set("plot.tolerance" + i, tolerance[i]);
            Prefs.set("plot.residualShift" + i, residualShift[i]);
            Prefs.set("plot.autoResidualShift" + i, autoResidualShift[i]);
            Prefs.set("plot.maxFitSteps" + i, maxFitSteps[i]);
            Prefs.set("plot.modelLineWidth" + i, modelLineWidth[i]);
            Prefs.set("plot.residualLineWidth" + i, residualLineWidth[i]);
            Prefs.set("plot.useTransitFit" + i, useTransitFit[i]);
            Prefs.set("plot.showLTranParams" + i, showLTranParams[i]);
            Prefs.set("plot.showLResidual" + i, showLResidual[i]);
            Prefs.set("plot.autoUpdateFit" + i, autoUpdateFit[i]);
            Prefs.set("plot.showModel" + i, showModel[i]);
            Prefs.set("plot.showResidual" + i, showResidual[i]);
            Prefs.set("plot.showResidualError" + i, showResidualError[i]);
            Prefs.set("plot.autoUpdatePriors" + i, autoUpdatePriors[i]);
            Prefs.set("plot.autoUpdatePrior[" + i + "][0]", autoUpdatePrior[i][0]);
            Prefs.set("plot.autoUpdatePrior[" + i + "][1]", autoUpdatePrior[i][1]);
            Prefs.set("plot.autoUpdatePrior[" + i + "][2]", autoUpdatePrior[i][2]);
            Prefs.set("plot.autoUpdatePrior[" + i + "][3]", autoUpdatePrior[i][3]);
            Prefs.set("plot.autoUpdatePrior[" + i + "][4]", autoUpdatePrior[i][4]);
            Prefs.set("plot.bpLock[" + i + "]", bpLock[i]);

            Prefs.set("plot.xlabel" + i, xlabel[i]);
            Prefs.set("plot.ylabel" + i, ylabel[i]);
            Prefs.set("plot.oplabel" + i, oplabel[i]);
            Prefs.set("plot.lines" + i, lines[i]);
            Prefs.set("plot.smooth" + i, smooth[i]);
            Prefs.set("plot.smoothLen" + i, smoothLen[i]);
            Prefs.set("plot.markerIndex" + i, markerIndex[i]);
            Prefs.set("plot.residualSymbolIndex" + i, residualSymbolIndex[i]);
            Prefs.set("plot.colorIndex" + i, colorIndex[i]);
            Prefs.set("plot.modelColorIndex" + i, modelColorIndex[i]);
            Prefs.set("plot.residualModelColorIndex" + i, residualModelColorIndex[i]);
            Prefs.set("plot.residualColorIndex" + i, residualColorIndex[i]);
            Prefs.set("plot.normIndex" + i, normIndex[i]);
            Prefs.set("plot.detrendVarDisplayed" + i, detrendVarDisplayed[i]);
            Prefs.set("plot.detrendFitIndex" + i, detrendFitIndex[i]);
            Prefs.set("plot.mmag" + i, mmag[i]);
            Prefs.set("plot.fromMag" + i, fromMag[i]);
            Prefs.set("plot.useColumnName" + i, useColumnName[i]);
            Prefs.set("plot.useLegend" + i, useLegend[i]);
            Prefs.set("plot.legend" + i, legend[i]);
            Prefs.set("plot.operatorIndex" + i, operatorIndex[i]);
            Prefs.set("plot.moreOptions" + i, moreOptions[i]);
            Prefs.set("plot.inputAverageOverSize" + i, inputAverageOverSize[i]);
            Prefs.set("plot.plotY" + i, plotY[i]);
            Prefs.set("plot.force" + i, force[i]);
            Prefs.set("plot.showErrors" + i, showErrors[i]);
            Prefs.set("plot.autoScaleFactor" + i, autoScaleFactor[i]);
            Prefs.set("plot.autoScaleStep" + i, autoScaleStep[i]);
            Prefs.set("plot.autoShiftFactor" + i, autoShiftFactor[i]);
            Prefs.set("plot.autoShiftStep" + i, autoShiftStep[i]);
            Prefs.set("plot.customScaleFactor" + i, customScaleFactor[i]);
            Prefs.set("plot.customScaleStep" + i, customScaleStep[i]);
            Prefs.set("plot.customShiftFactor" + i, customShiftFactor[i]);
            Prefs.set("plot.customShiftStep" + i, customShiftStep[i]);
            Prefs.set("plot.ASInclude" + i, ASInclude[i]);
            for (int v = 0; v < maxDetrendVars; v++) {
                Prefs.set("plot.detrendlabel[" + i + "][" + v + "]", detrendlabel[i][v]);
                Prefs.set("plot.detrendlabelhold[" + i + "][" + v + "]", detrendlabelhold[i][v]);
                Prefs.set("plot.detrendFactor[" + i + "][" + v + "]", detrendFactor[i][v]);
                Prefs.set("plot.detrendFactorStep[" + i + "][" + v + "]", detrendFactorStep[i][v]);
            }
            for (int v = 0; v < (maxFittedVars); v++) {
                Prefs.set("plot.lockToCenter[" + i + "][" + v + "]", lockToCenter[i][v]);
                Prefs.set("plot.priorCenter[" + i + "][" + v + "]", priorCenter[i][v]);
                Prefs.set("plot.priorWidth[" + i + "][" + v + "]", priorWidth[i][v]);
                Prefs.set("plot.usePriorWidth[" + i + "][" + v + "]", usePriorWidth[i][v]);
                Prefs.set("plot.useCustomFitStep[" + i + "][" + v + "]", useCustomFitStep[i][v]);
                Prefs.set("plot.fitStep[" + i + "][" + v + "]", fitStep[i][v]);
            }
            Prefs.set("plot.displayBinMinutes[" + i +"]", minutes.get(i).first());
            Prefs.set("plot.displayBin[" + i +"]", binDisplay[i]);
        }
    }
}

