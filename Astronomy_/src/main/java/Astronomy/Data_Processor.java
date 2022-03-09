// Data_Prosser.java
package Astronomy;

import astroj.*;
import ij.*;
import ij.gui.GenericDialog;
import ij.gui.ImageCanvas;
import ij.gui.ImageWindow;
import ij.io.DirectoryChooser;
import ij.io.FileInfo;
import ij.io.OpenDialog;
import ij.plugin.Macro_Runner;
import ij.plugin.PlugIn;
import ij.process.ImageProcessor;
import util.UIHelper;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.stream.FileImageOutputStream;
import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.MaskFormatter;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.*;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Timer;
import java.util.*;


/**
 * This plugin processes CCD image data, including bias and dark subtraction,
 * flat-field correction, and CCD-non-linearity correction. The plugin can process
 * existing datasets, or it can reduce data as it is being added to a directory 
 * (e.g. as it is written from a CCD). This plugin is modeled after the directory_watcher
 * plugin written originally by F.V. Hessman, Inst. f. Astrophysik,
 * Georg-August-Unversitaet Goettingen.
 * @author K.A. Collins, University of Louisville
 * @date 2012-04-01
 * @version 2.0
 * 
 */
public class Data_Processor implements PlugIn, ActionListener, ChangeListener, //,KeyListener
        ItemListener, MouseListener, MouseMotionListener, MouseWheelListener, PropertyChangeListener 
	{
	int pollingInterval = 0;		// IN SECONDS
	TimerTask	task = null;
	Timer		timer = null;
	boolean blocked = false;

	boolean onlyNew = false; // VERSION 1.1
    boolean firstRun = true;
    boolean restart = true;
    boolean loadNewCals = true;
    boolean useBeep = false;
    boolean requestStop = false;
    boolean sortNumerically = true;
    boolean updateExclude = true;
    boolean reloadFilenames = false;
    boolean usingNewWindow = false;
    boolean removing = false;
    boolean MAcanceled = false;
    boolean showToolTips = true;
    boolean autoWildcard = true;
    boolean displayCC = true;
    boolean removeBrightOutliers = true;
    boolean removeDarkOutliers = true;
    boolean removePedestal = true;

	int ignoredImages=0, totalNumFilesInDir=0;

	String mainDir = IJ.getDirectory ("home");
    String filepath = IJ.getDirectory ("home");
	String filenamePattern = "filenamepattern.fits";
    String oldFilenamePattern = "";
	String slash = "/";
    String biasPath = "", darkPath = "", flatPath = "";
    String biasMasterPath = "", darkMasterPath = "", flatMasterPath = "";
    String biasFilePath = "", darkFilePath = "", flatFilePath = "";
    String mbiasPath = "", mdarkPath = "", mflatPath = "", sciencePath = "";
    String savePath = "", saveDirPath = "", saveFileName = "", saveFormat="";
    String numPrefix = "";
//    String raText = "", decText = "", latText = "", lonText = "";
    String s, sOriginal;
    String oldObservatoryName = "", oldTargetName = "";

	File scienceDir, biasDirectory, darkDirectory, flatDirectory, biasMasterFile, darkMasterFile, flatMasterFile;
    File saveFile, biasMasterDirectory, darkMasterDirectory, flatMasterDirectory;
    ImagePlus scienceImp, rawScienceImp, mbiasImp, mdarkImp, mflatImp;
    String[] darkHeader = null, flatHeader = null, scienceHeader = null;
    double darkExpTime = 0, flatExpTime = 0, scienceExpTime = 0;
    int validTextFilteredFiles = 0,validNumFilteredFiles = 0, validBiasFiles = 0, validDarkFiles = 0, validFlatFiles = 0;
    int validMasterBiasFiles = 0, validMasterDarkFiles = 0, validMasterFlatFiles = 0;

	HashMap<String, String> images;
    ImagePlus openImage, testImp;
    int scienceHeight=0, scienceWidth=0, length = 0;
	int foundImages=0;
    int dialogFrameLocationX = 20;
    int dialogFrameLocationY = 20;
    long minFileNumber = 0, maxFileNumber = 1000000000;
    int outlierRadius = 2, outlierThreshold = 50;
    int fileSizeChangeWaitTime = 500;

    Class<?> imageWindowClass;
	boolean running = false;
    boolean autoRunAndClose = false;
    static public boolean active = false;  //used by MultiAperture to determine if runMultiPlot is valid
	protected boolean ignoreAction = false;
    
    long flen, newflen;


	JFrame dialogFrame;
    JScrollPane mainScrollPane, fitsHeaderScrollPane;
    Frame openFrame;
    JMenuBar menuBar;
    JMenu fileMenu, prefsMenu, viewMenu;
    JMenuItem exitMenuItem;
    JCheckBoxMenuItem useBeepCB, useShowLogCB, showLogDateTimeCB, showScienceCB, showRawCalsCB, showMasterImagesCB;
    JCheckBoxMenuItem autoRunAndCloseCB;
    JCheckBoxMenuItem onlyNewCB, usepreMacro1AutoLevelCB, showToolTipsCB, autoWildcardCB;
    JCheckBoxMenuItem rawCalCommonDirCB, masterCalCommonDirCB, postMacro1AutoLevelCB, postMacro2AutoLevelCB;
    JMenuItem setFileSizeChangeWaitTimeMenuItem;

	JTextField dirText, filenamePatternText;
	JLabel remainingNumLabel, processedNumLabel, pollingIntervalLabel, minFileNumberLabel, maxFileNumberLabel;
    JLabel validTextFilteredFilesLabel, validNumFilteredFilesLabel;
    JLabel validBiasFilesLabel, validDarkFilesLabel, validFlatFilesLabel, numPrefixLabel;
    JLabel validMasterBiasFilesLabel, validMasterDarkFilesLabel, validMasterFlatFilesLabel;
    JLabel validMacro1FilesLabel, validMacro2FilesLabel;
    JLabel NLCLabel, coeffBLabel, coeffCLabel, coeffDLabel, NLCFinalLabel, subDirLabel, suffixLabel, saveFormatLabel;
    JButton fitsHeaderToolButton, astrometrySetupButton;
    JButton setApertureButton, changeAperturesButton, clearTableButton, displayCCButton;
	JButton dirButton, fileButton, startButton, pauseButton, clearButton;
    JButton biasBaseButton, darkBaseButton, flatBaseButton, biasMasterButton, darkMasterButton, flatMasterButton;
    JButton biasBaseDirButton, darkBaseDirButton, flatBaseDirButton, biasMasterDirButton, darkMasterDirButton, flatMasterDirButton;

	JCheckBox sortNumericallyBox, gradientRemovalCB, cosmicRemovalCB, removeBrightOutliersCB, removeDarkOutliersCB;
    SpinnerModel pollingIntervalModel, minFileNumberModel, maxFileNumberModel, outlierRadiusModel, outlierThresholdModel;
    JSpinner pollingIntervalSpinner, minFileNumberSpinner, maxFileNumberSpinner, outlierRadiusSpinner, outlierThresholdSpinner;
    JLabel outlierRadiusLabel, outlierThresholdLabel;
    JCheckBox runPreMacroBox, runPostMacroBox;
    JCheckBox runMultiApertureBox, runMultiPlotBox, saveImageBox, savePlotBox;
    JButton savePlotPathButton, saveImagePathButton;
    String savePlotPath = "", saveImagePath = "";
    boolean savePlot = false, saveImage = false;
	JTextField preMacroText, postMacroText, saveImageTextField, savePlotTextField;
	JButton preMacroButton, postMacroButton;

    JComboBox  objectCoordinateSourceCombo, observatoryLocationSourceCombo;
    String[] objectCoordinateSources = {"Coordinate Converter manual entry",
                                        "FITS header target name",
                                        "FITS header target name (less trailing alpha char)",
                                        "FITS header target RA/DEC (J2000)",
                                        "FITS header target RA/DEC (epoch of observation)"
                                       };
    String[] observatoryLocationSources = {"Coordinate Converter manual entry",
                                           "FITS header observatory name",
                                           "FITS header latitude and longitude"
                                          };
    int selectedObjectCoordinateSource = 0;
    int selectedObservatoryLocationSource = 0;


    boolean useScienceProcessing = true;
    boolean changeApertures = true, fitsHeadersDisplayed = false;
    boolean rawCalCommonDir = false;
    boolean masterCalCommonDir = false;
    boolean useBias, useDark=true, useFlat=true, useNLC=false, calcHeaders=true;
    boolean createBias, createDark, createFlat, saveProcessedData=true;
    boolean biasMedian=true, darkMedian=true, flatMedian=true, saveFloatingPoint, scaleExpTime;
    boolean showRawCals=false, showMasters = false, showScience = true, showLog = true, showLogDateTime = true;
    boolean useGradientRemoval = true, useCosmicRemoval = false;
    boolean runMultiAperture = false;
    static public boolean runMultiPlot = false;
    boolean enableFileNumberFiltering = true;
    boolean enableNLCBoxes = true;
    
    String biasMasterDir="", darkMasterDir="", flatMasterDir="";
    String biasMasterDirText="", darkMasterDirText="", flatMasterDirText="";
    String biasRawDir="", darkRawDir="", flatRawDir="", saveDir="pipelineout";
    String biasRawDirText="", darkRawDirText="", flatRawDirText="";

    String biasBase="bias_", darkBase="dark_", flatBase="flat_", saveSuffix="_out";
    String coeffCText="";
    double coeffA=0.0, coeffB=1.0, coeffC=0.0, coeffD=0.0;;
//    double dec=0, ra=0, lat=0, lon=0;
    String biasMaster="mbias.fits", darkMaster="mdark.fits", flatMaster="mflat.fits";

    SimpleDateFormat f = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    AstroConverter acc;

    JCheckBox useBiasBox, useDarkBox, deBiasMasterDarkBox, useFlatBox, useNLCBox, scaleExpTimeBox, enableFileNumberFilteringBox; //updateExcludeBox;
    JCheckBox createBiasBox, createDarkBox, createFlatBox, saveProcessedDataBox;
    JCheckBox calcAirmassBox, plateSolveBox, useScienceProcessingBox, compressBox;
    JRadioButton biasAverageRadio, darkAverageRadio, flatAverageRadio, saveIntegerRadio;
    JRadioButton biasMedianRadio, darkMedianRadio, flatMedianRadio, saveFloatingPointRadio;
    ButtonGroup biasRadioGroup, darkRadioGroup, flatRadioGroup, saveRadioGroup;
    JTextField biasRawDirField, darkRawDirField, flatRawDirField, saveDirField, numPrefixField;
    JTextField biasMasterDirField, darkMasterDirField, flatMasterDirField;
    JTextField biasBaseField, darkBaseField, flatBaseField, saveSuffixField, saveFormatField;
    JSpinner coeffASpinner, coeffBSpinner, coeffCSpinner, coeffDSpinner;
    SpinnerModel coeffAModel, coeffBModel, coeffCModel, coeffDModel;
    JTextField biasMasterField, darkMasterField, flatMasterField;

    boolean deBiasMasterDark = true;
	boolean runPreMacro = false;		
    boolean runPostMacro = false;
    boolean preMacro1AutoLevel = false;
    boolean postMacro1AutoLevel = false;
    boolean postMacro2AutoLevel = false;
    boolean compress = false;
    boolean plateSolve = false;
    AstrometrySetup astrometrySetup;
    boolean astrometryCanceledByUser = false;


	Macro_Runner runner = new Macro_Runner();
	String preMacroPath = new String(mainDir);
    String postMacroPath = new String(mainDir);
    String lastImageName = null;

    DecimalFormat twoPlaces = new DecimalFormat("0.00");
    DecimalFormat fourPlaces = new DecimalFormat("0.0000");
    DecimalFormat sixPlaces = new DecimalFormat("0.000000");
    DecimalFormat uptoFourPlaces = new DecimalFormat("0.####");
    DecimalFormat upto20Places = new DecimalFormat("0.####################");

    Color mainBorderColor = new Color(118,142,229);
    Color red = new Color(255,160,160);
    Color yellow = new Color(255,255,200);
    Color green = new Color(225,255,225);
    Color gray = new Color(240,240,240);

    JFrame fitsHeaderFrame;
    JTextField objectNameReadKeywordTF, objectRAJ2000ReadKeywordTF, objectDecJ2000ReadKeywordTF;
    JTextField observatoryNameReadKeywordTF, observatoryLatReadKeywordTF, observatoryLonReadKeywordTF;
    String objectNameReadKeyword = "TARGET";
    String objectRAJ2000ReadKeyword = "RA_OBJ";
    String objectDecJ2000ReadKeyword = "DEC_OBJ";
    String observatoryNameReadKeyword = "TELESCOP";
    String observatoryLatReadKeyword = "SITELAT";
    String observatoryLonReadKeyword = "SITELONG";
    JCheckBox saveObjectRAJ2000CB, saveObjectDecJ2000CB, saveObjectRAEODCB, saveObjectDecEODCB, saveObjectAltitudeCB, saveObjectAzimuthCB;
    JCheckBox saveObservatoryLatCB, saveObservatoryLonCB, saveObjectHourAngleCB, saveObjectZenithDistanceCB;
    JCheckBox saveObjectAirmassCB, saveJD_SOBSCB, saveJD_MOBSCB, saveHJD_MOBSCB, saveBJD_MOBSCB, latNegateCB, lonNegateCB, raInDegreesCB;
    boolean saveObservatoryLat = true;
    boolean saveObservatoryLon = true;
    boolean saveObjectRAJ2000 = true;
    boolean saveObjectDecJ2000 = true;
    boolean saveObjectRAEOD = true;
    boolean saveObjectDecEOD = true;
    boolean saveObjectAltitude = true;
    boolean saveObjectAzimuth = true;
    boolean saveObjectHourAngle = true;
    boolean saveObjectZenithDistance = true;
    boolean saveObjectAirmass = true;
    boolean saveJD_SOBS = true;
    boolean saveJD_MOBS = true;
    boolean saveHJD_MOBS = true;
    boolean saveBJD_MOBS = true;
    boolean latNegate = false;
    boolean lonNegate = false;
    boolean raInDegrees = false;
    JTextField objectRAJ2000SaveKeywordTF, objectDecJ2000SaveKeywordTF, objectRAEODSaveKeywordTF, objectDecEODSaveKeywordTF;
    JTextField objectAltitudeSaveKeywordTF, objectAzimuthSaveKeywordTF, observatoryLatSaveKeywordTF, observatoryLonSaveKeywordTF;
    JTextField objectAirmassSaveKeywordTF, JD_SOBSSaveKeywordTF, JD_MOBSSaveKeywordTF, HJD_MOBSSaveKeywordTF, BJD_MOBSSaveKeywordTF;
    JTextField objectHourAngleSaveKeywordTF, objectZenithDistanceSaveKeywordTF;
    String observatoryLatSaveKeyword = "SITELAT";
    String observatoryLonSaveKeyword = "SITELONG";
    String objectRAJ2000SaveKeyword = "RAOBJ2K";
    String objectDecJ2000SaveKeyword = "DECOBJ2K";
    String objectRAEODSaveKeyword = "RA_OBJ";
    String objectDecEODSaveKeyword = "DEC_OBJ";
    String objectAltitudeSaveKeyword = "ALT_OBJ";
    String objectAzimuthSaveKeyword = "AZ_OBJ";
    String objectHourAngleSaveKeyword = "HA_OBJ";
    String objectZenithDistanceSaveKeyword = "ZD_OBJ";
    String objectAirmassSaveKeyword = "AIRMASS";
    String JD_SOBSSaveKeyword = "JD_SOBS";
    String JD_MOBSSaveKeyword = "JD_UTC";
    String HJD_MOBSSaveKeyword = "HJD_UTC";
    String BJD_MOBSSaveKeyword = "BJD_TDB";
    
    Thread astrometrySetupThread;
    Astrometry astrometry = new Astrometry();
    ImageIcon astrometrySetupIcon;
    ImageIcon astrometryActiveIcon; 

    String fontName = "Dialog";
    Font p8 = new Font(fontName,Font.PLAIN,8);
    Font p11 = new Font(fontName,Font.PLAIN,11);
    Font p12 = new Font(fontName,Font.PLAIN,12);
    Font b12 = new Font(fontName,Font.BOLD,12);
    Font b11 = new Font(fontName,Font.BOLD,11);
    Font b14 = new Font(fontName,Font.BOLD,14);
    
    Canvas imc, openImageCanvas;
    ImageWindow win;
    Properties props;
    Enumeration enProps;
    String key;
    FileInfo imf;
    
    Calendar cal = Calendar.getInstance();
    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS");
    
    final static int FAILED = 0;
    final static int SUCCESS = 1;
    final static int SKIPPED = 2;
    final static int CANCELED = 3;  
    final static boolean CONVERTTOFLOAT = true;

    public void mouseClicked(MouseEvent e) {
    int x = e.getX();
    int y = e.getY();
    //int offscreenX = canvas.offScreenX(x);
    //int offscreenY = canvas.offScreenY(y);
    //IJ.write("mousePressed: "+offscreenX+","+offscreenY);
    }

    public void mousePressed(MouseEvent e) {}

    public void mouseReleased(MouseEvent e) {}

    public void mouseEntered(MouseEvent e) {}

    public void mouseExited(MouseEvent e) {}


    public void mouseDragged(MouseEvent e) {
//        IJ.log("mouse dragged: "+e.getX()+","+e.getY());
        }
    public void mouseMoved(MouseEvent e) {
//        IJ.log("mouse moved: "+e.getX()+","+e.getY());
        }




	public void run(String arg)
		{
        getPrefs();
		if (IJ.isWindows()) slash="\\";

        UIHelper.setLookAndFeel();
        images = new HashMap<String, String>(20);
//		startDialog();
//		}
//
//	/**
//	 * Constructs Swing interface.
//	 */
//	protected void startDialog ()
//		{

        acc = new AstroConverter(false, true, "DP Coordinate Converter");
        acc.setEnableTimeEntry(false);
        acc.getTAIminusUTC();

        int subpanelHeight = 22;
        int topLabelHeight = 20;
        int textboxHeight = 22;
        int directoryWidth = 500;
        int fileWidth = 200;
        int coeffWidth = 100;
        int validFilesWidth = 60;
        int checkBoxWidth = 80;
        int radioWidth = 125;
        Dimension directoryBoxSize = new Dimension(directoryWidth, textboxHeight);
        Dimension fileBoxSize = new Dimension(fileWidth, subpanelHeight);
        Dimension macroBoxSize = new Dimension(fileWidth, 20);
        Dimension validFilesSize = new Dimension(validFilesWidth, textboxHeight);
        Dimension checkBoxSize = new Dimension(checkBoxWidth, textboxHeight);
        Dimension checkboxPlusRadioSize = new Dimension(checkBoxWidth+radioWidth+2, textboxHeight);
        Dimension radioSize = new Dimension(radioWidth, textboxHeight);
        Dimension coeffBoxSize = new Dimension(coeffWidth, textboxHeight);
        Dimension iconButtonSize = new Dimension(52, 30);
        
        int folderIconHeight = 22;
        int folderIconRightMargin = 6;
        int folderIconWidth = folderIconHeight+folderIconRightMargin;
        Dimension folderIconSize = new Dimension(folderIconWidth, folderIconHeight);
        Insets folderIconMargin = new Insets(0,0,0,folderIconRightMargin); //top,left,bottom,right
        
        int boxWidthLeft = 75;
        int boxWidthRight = 125;
        int boxHeight = 24;

        if (!IJ.isWindows())
            {
            p8  = new Font(fontName,Font.PLAIN,8);
            p11 = new Font(fontName, Font.PLAIN,10);
            p12 = new Font(fontName, Font.PLAIN,11);
            b12 = new Font(fontName, Font.BOLD,11);
            b11 = new Font(fontName, Font.BOLD,10);
            b14 = new Font(fontName, Font.BOLD,13);
            }

        Dimension fitsSize = new Dimension(100, 20);
        Dimension fitsDummySize = new Dimension(80, 10);
        Dimension fitsLabelSize = new Dimension(200, 20);
        Dimension fitsCBSize = new Dimension(80, 20);
        Insets fitsMargin = new Insets(2,2,2,2); //top,left,bottom,right
        Insets textboxMargin = new Insets(2,2,2,2); //top,left,bottom,right

        int iconWidth = 40;
        int iconHeight = 22;
        Dimension iconSize = new Dimension(iconWidth, iconHeight);
        Insets buttonMargin = new Insets(0,0,0,0); //top,left,bottom,right



        ImageIcon fileOpenIcon = createImageIcon("images/fileopenblue.png", "File Open");
        ImageIcon folderOpenIcon = createImageIcon("images/folderopenblue.png", "Folder Open");
        ImageIcon windowIcon = createImageIcon("images/dp.png", "Window Icon");
        ImageIcon fitsHeaderToolIcon = createImageIcon("images/fitsheadertool.png", "FITS Header Tool Icon");
        astrometrySetupIcon = createImageIcon("images/astrometry.png", "Astrometry Setup Icon");
        astrometryActiveIcon = createImageIcon("images/astrometryselected.png", "Astrometry Active Icon");

        fitsHeaderFrame = new JFrame ("General FITS Header Settings");
        fitsHeaderFrame.setIconImage(fitsHeaderToolIcon.getImage());
        fitsHeaderFrame.addWindowListener(new WindowAdapter(){
            @Override
            public void windowClosing(WindowEvent e){
            fitsHeadersDisplayed = !fitsHeadersDisplayed;}});
        JPanel fitsHeaderPanel = new JPanel(new SpringLayout());

        JPanel fitsReadPanel = new JPanel(new SpringLayout());
        fitsReadPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(mainBorderColor, 1), "FITS Header Input Settings", TitledBorder.LEFT, TitledBorder.TOP, b12, Color.DARK_GRAY));

        JLabel objectNameReadKeywordLabel = new JLabel ("Target Name Keyword:");
        objectNameReadKeywordLabel.setPreferredSize(fitsLabelSize);
        objectNameReadKeywordLabel.setFont(p12);
        objectNameReadKeywordLabel.setHorizontalAlignment (JTextField.RIGHT);
		fitsReadPanel.add (objectNameReadKeywordLabel);

		objectNameReadKeywordTF = new JTextField (objectNameReadKeyword);
        objectNameReadKeywordTF.setMargin(fitsMargin);
        objectNameReadKeywordTF.setFont(p12);
		objectNameReadKeywordTF.setPreferredSize(fitsSize);
		objectNameReadKeywordTF.setHorizontalAlignment(JTextField.LEFT);
        objectNameReadKeywordTF.setToolTipText("<html>"+"Enter FITS header keyword that identifies the SIMBAD resolvable target name"+"<br>"+
                                               "(used when 'Target Coordinate Source' is set to 'FITS header target name')"+"</html>");
        objectNameReadKeywordTF.getDocument().addDocumentListener(new thisDocumentListener());
        fitsReadPanel.add (objectNameReadKeywordTF);

        JLabel fitsDummyLabel1 = new JLabel ("");
        fitsDummyLabel1.setPreferredSize(fitsDummySize);
		fitsReadPanel.add (fitsDummyLabel1);

//        JLabel fitsDummyLabel1a = new JLabel ("");
//        fitsDummyLabel1a.setPreferredSize(fitsDummySize);
//		fitsReadPanel.add (fitsDummyLabel1a);
//
//        JLabel fitsDummyLabel2 = new JLabel ("");
//        fitsDummyLabel2.setPreferredSize(fitsDummySize);
//		fitsReadPanel.add (fitsDummyLabel2);
//
//        JLabel fitsDummyLabel2a = new JLabel ("");
//        fitsDummyLabel2a.setPreferredSize(fitsDummySize);
//		fitsReadPanel.add (fitsDummyLabel2a);

        JLabel objectRAJ2000ReadKeywordLabel = new JLabel ("Target RA Keyword:");
        objectRAJ2000ReadKeywordLabel.setPreferredSize(fitsLabelSize);
        objectRAJ2000ReadKeywordLabel.setFont(p12);
        objectRAJ2000ReadKeywordLabel.setHorizontalAlignment (JTextField.RIGHT);
		fitsReadPanel.add (objectRAJ2000ReadKeywordLabel);

		objectRAJ2000ReadKeywordTF = new JTextField (objectRAJ2000ReadKeyword);
        objectRAJ2000ReadKeywordTF.setMargin(fitsMargin);
        objectRAJ2000ReadKeywordTF.setFont(p12);
		objectRAJ2000ReadKeywordTF.setPreferredSize(fitsSize);
		objectRAJ2000ReadKeywordTF.setHorizontalAlignment(JTextField.LEFT);
        objectRAJ2000ReadKeywordTF.setToolTipText("<html>Enter FITS header keyword that identifies the target right ascension<br>"+
                                               "(used when 'Target Coordinate Source' is set to<br>"+
                                               "'FITS header target RA/DEC (J2000) OR (epoch of observation)')"+"</html>");
        objectRAJ2000ReadKeywordTF.getDocument().addDocumentListener(new thisDocumentListener());
        fitsReadPanel.add (objectRAJ2000ReadKeywordTF);

        raInDegreesCB = new JCheckBox("Degrees", raInDegrees);
        raInDegreesCB.setPreferredSize(fitsCBSize);
        raInDegreesCB.setToolTipText("<html>Select if Target RA Keyword value is in degrees.<br>"+
                                   "(De-select if Target RA Keyword value is in hours.</html>");
		raInDegreesCB.addItemListener (this);
        fitsReadPanel.add(raInDegreesCB);

        SpringUtil.makeCompactGrid (fitsReadPanel, fitsReadPanel.getComponentCount()/3,3, 2,2,2,2);
        fitsHeaderPanel.add(fitsReadPanel);

        JLabel objectDecJ2000ReadKeywordLabel = new JLabel ("Target DEC Keyword:");
        objectDecJ2000ReadKeywordLabel.setPreferredSize(fitsLabelSize);
        objectDecJ2000ReadKeywordLabel.setFont(p12);
        objectDecJ2000ReadKeywordLabel.setHorizontalAlignment (JTextField.RIGHT);
		fitsReadPanel.add (objectDecJ2000ReadKeywordLabel);

		objectDecJ2000ReadKeywordTF = new JTextField (objectDecJ2000ReadKeyword);
        objectDecJ2000ReadKeywordTF.setMargin(fitsMargin);
        objectDecJ2000ReadKeywordTF.setFont(p12);
		objectDecJ2000ReadKeywordTF.setPreferredSize(fitsSize);
		objectDecJ2000ReadKeywordTF.setHorizontalAlignment(JTextField.LEFT);
        objectDecJ2000ReadKeywordTF.setToolTipText("<html>Enter FITS header keyword that identifies the target declination<br>"+
                                               "(used when 'Target Coordinate Source' is set to<br>"+
                                               "'FITS header target RA/DEC (J2000) OR (epoch of observation)')</html>");
        objectDecJ2000ReadKeywordTF.getDocument().addDocumentListener(new thisDocumentListener());
        fitsReadPanel.add (objectDecJ2000ReadKeywordTF);

        JLabel fitsDummyLabel2c = new JLabel ("");
        fitsDummyLabel2c.setPreferredSize(fitsDummySize);
		fitsReadPanel.add (fitsDummyLabel2c);

        JLabel fitsDummyLabel3 = new JLabel ("");
        fitsDummyLabel3.setPreferredSize(fitsDummySize);
		fitsReadPanel.add (fitsDummyLabel3);

        JLabel fitsDummyLabel4 = new JLabel ("");
        fitsDummyLabel4.setPreferredSize(fitsDummySize);
		fitsReadPanel.add (fitsDummyLabel4);

        JLabel fitsDummyLabel4b = new JLabel ("");
        fitsDummyLabel4b.setPreferredSize(fitsDummySize);
		fitsReadPanel.add (fitsDummyLabel4b);

        JLabel observatoryNameReadKeywordLabel = new JLabel ("Observatory Name Keyword:");
        observatoryNameReadKeywordLabel.setPreferredSize(fitsLabelSize);
        observatoryNameReadKeywordLabel.setFont(p12);
        observatoryNameReadKeywordLabel.setHorizontalAlignment (JTextField.RIGHT);
		fitsReadPanel.add (observatoryNameReadKeywordLabel);

		observatoryNameReadKeywordTF = new JTextField (observatoryNameReadKeyword);
        observatoryNameReadKeywordTF.setMargin(fitsMargin);
        observatoryNameReadKeywordTF.setFont(p12);
		observatoryNameReadKeywordTF.setPreferredSize(fitsSize);
		observatoryNameReadKeywordTF.setHorizontalAlignment(JTextField.LEFT);
        observatoryNameReadKeywordTF.setToolTipText("<html>"+"Enter FITS header keyword that identifies the observatory name"+"<br>"+
                                               "(used when 'Observatory Location Source' is set to 'FITS header observatory name')<br>"+
                                               "(the keyword's associated string value must match part of a Coordinate Converter observatory option)"+"</html>");
        observatoryNameReadKeywordTF.getDocument().addDocumentListener(new thisDocumentListener());
        fitsReadPanel.add (observatoryNameReadKeywordTF);

        JLabel fitsDummyLabel4e = new JLabel ("");
        fitsDummyLabel4e.setPreferredSize(fitsDummySize);
		fitsReadPanel.add (fitsDummyLabel4e);

        JLabel observatoryLatReadKeywordLabel = new JLabel ("Observatory Latitude Keyword:");
        observatoryLatReadKeywordLabel.setPreferredSize(fitsLabelSize);
        observatoryLatReadKeywordLabel.setFont(p12);
        observatoryLatReadKeywordLabel.setHorizontalAlignment (JTextField.RIGHT);
		fitsReadPanel.add (observatoryLatReadKeywordLabel);

		observatoryLatReadKeywordTF = new JTextField (observatoryLatReadKeyword);
        observatoryLatReadKeywordTF.setMargin(fitsMargin);
        observatoryLatReadKeywordTF.setFont(p12);
		observatoryLatReadKeywordTF.setPreferredSize(fitsSize);
		observatoryLatReadKeywordTF.setHorizontalAlignment(JTextField.LEFT);
        observatoryLatReadKeywordTF.setToolTipText("<html>"+"Enter FITS header keyword that identifies the observatory geographic latitude"+"<br>"+
                                               "(used when 'Observatory Location Source' is set to 'FITS header latitude and longitude')"+"</html>");
        observatoryLatReadKeywordTF.getDocument().addDocumentListener(new thisDocumentListener());
        fitsReadPanel.add (observatoryLatReadKeywordTF);

        latNegateCB = new JCheckBox("negate", latNegate);
        latNegateCB.setPreferredSize(fitsCBSize);
        latNegateCB.setToolTipText("<html>Select to multiply the header latitude value by -1.0<br>"+
                                   "(Data Processor requires north positive latitude values)</html>");
		latNegateCB.addItemListener (this);
        fitsReadPanel.add(latNegateCB);

        JLabel observatoryLonReadKeywordLabel = new JLabel ("Observatory Longitude Keyword:");
        observatoryLonReadKeywordLabel.setPreferredSize(fitsLabelSize);
        observatoryLonReadKeywordLabel.setFont(p12);
        observatoryLonReadKeywordLabel.setHorizontalAlignment (JTextField.RIGHT);
		fitsReadPanel.add (observatoryLonReadKeywordLabel);

		observatoryLonReadKeywordTF = new JTextField (observatoryLonReadKeyword);
        observatoryLonReadKeywordTF.setMargin(fitsMargin);
        observatoryLonReadKeywordTF.setFont(p12);
		observatoryLonReadKeywordTF.setPreferredSize(fitsSize);
		observatoryLonReadKeywordTF.setHorizontalAlignment(JTextField.LEFT);
        observatoryLonReadKeywordTF.setToolTipText("<html>"+"Enter FITS header keyword that identifies the observatory geographic longitude"+"<br>"+
                                               "(used when 'Observatory Location Source' is set to 'FITS header latitude and longitude')"+"</html>");
        observatoryLonReadKeywordTF.getDocument().addDocumentListener(new thisDocumentListener());
        fitsReadPanel.add (observatoryLonReadKeywordTF);

        lonNegateCB = new JCheckBox("negate", lonNegate);
        lonNegateCB.setPreferredSize(fitsCBSize);
        lonNegateCB.setToolTipText("<html>Select to multiply the header longitude value by -1.0<br>"+
                                   "(Data Processor requires east positive longitude values)</html>");
		lonNegateCB.addItemListener (this);
        fitsReadPanel.add(lonNegateCB);

        SpringUtil.makeCompactGrid (fitsReadPanel, fitsReadPanel.getComponentCount()/3,3, 2,2,2,2);
        fitsHeaderPanel.add(fitsReadPanel);


        JPanel fitsWritePanel = new JPanel(new SpringLayout());
        fitsWritePanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(mainBorderColor, 1), "FITS Header Output Settings", TitledBorder.LEFT, TitledBorder.TOP, b12, Color.DARK_GRAY));

        JLabel objectRAJ2000SaveKeywordLabel = new JLabel ("Target J2000 RA Keyword:");
        objectRAJ2000SaveKeywordLabel.setPreferredSize(fitsLabelSize);
        objectRAJ2000SaveKeywordLabel.setFont(p12);
        objectRAJ2000SaveKeywordLabel.setHorizontalAlignment (JTextField.RIGHT);
		fitsWritePanel.add (objectRAJ2000SaveKeywordLabel);

		objectRAJ2000SaveKeywordTF = new JTextField (objectRAJ2000SaveKeyword);
        objectRAJ2000SaveKeywordTF.setMargin(fitsMargin);
        objectRAJ2000SaveKeywordTF.setFont(p12);
		objectRAJ2000SaveKeywordTF.setPreferredSize(fitsSize);
		objectRAJ2000SaveKeywordTF.setHorizontalAlignment(JTextField.LEFT);
        objectRAJ2000SaveKeywordTF.setToolTipText("<html>"+"Enter keyword used when saving target J2000 RA to FITS header"+"</html>");
        objectRAJ2000SaveKeywordTF.getDocument().addDocumentListener(new thisDocumentListener());
        fitsWritePanel.add (objectRAJ2000SaveKeywordTF);

        saveObjectRAJ2000CB = new JCheckBox("enable", saveObjectRAJ2000);
        saveObjectRAJ2000CB.setPreferredSize(fitsCBSize);
        saveObjectRAJ2000CB.setToolTipText("Select to save target J2000 RA to FITS header");
		saveObjectRAJ2000CB.addItemListener (this);
        fitsWritePanel.add(saveObjectRAJ2000CB);

        JLabel objectDecJ2000SaveKeywordLabel = new JLabel ("Target J2000 DEC Keyword:");
        objectDecJ2000SaveKeywordLabel.setPreferredSize(fitsLabelSize);
        objectDecJ2000SaveKeywordLabel.setFont(p12);
        objectDecJ2000SaveKeywordLabel.setHorizontalAlignment (JTextField.RIGHT);
		fitsWritePanel.add (objectDecJ2000SaveKeywordLabel);

		objectDecJ2000SaveKeywordTF = new JTextField (objectDecJ2000SaveKeyword);
        objectDecJ2000SaveKeywordTF.setMargin(fitsMargin);
        objectDecJ2000SaveKeywordTF.setFont(p12);
		objectDecJ2000SaveKeywordTF.setPreferredSize(fitsSize);
		objectDecJ2000SaveKeywordTF.setHorizontalAlignment(JTextField.LEFT);
        objectDecJ2000SaveKeywordTF.setToolTipText("<html>"+"Enter keyword used when saving target J2000 DEC to FITS header"+"</html>");
        objectDecJ2000SaveKeywordTF.getDocument().addDocumentListener(new thisDocumentListener());
        fitsWritePanel.add (objectDecJ2000SaveKeywordTF);

        saveObjectDecJ2000CB = new JCheckBox("enable", saveObjectDecJ2000);
        saveObjectDecJ2000CB.setPreferredSize(fitsCBSize);
        saveObjectRAJ2000CB.setPreferredSize(fitsCBSize);
        saveObjectDecJ2000CB.setToolTipText("Select to save target J2000 DEC to FITS header");
		saveObjectDecJ2000CB.addItemListener (this);
        fitsWritePanel.add(saveObjectDecJ2000CB);

        JLabel fitsDummyLabel5a = new JLabel ("");
        fitsDummyLabel5a.setPreferredSize(fitsDummySize);
		fitsWritePanel.add (fitsDummyLabel5a);

        JLabel fitsDummyLabel6a = new JLabel ("");
        fitsDummyLabel6a.setPreferredSize(fitsDummySize);
		fitsWritePanel.add (fitsDummyLabel6a);

        JLabel fitsDummyLabel7a = new JLabel ("");
        fitsDummyLabel7a.setPreferredSize(fitsDummySize);
		fitsWritePanel.add (fitsDummyLabel7a);

        JLabel objectRAEODSaveKeywordLabel = new JLabel ("Target RA Keyword:");
        objectRAEODSaveKeywordLabel.setPreferredSize(fitsLabelSize);
        objectRAEODSaveKeywordLabel.setFont(p12);
        objectRAEODSaveKeywordLabel.setHorizontalAlignment (JTextField.RIGHT);
		fitsWritePanel.add (objectRAEODSaveKeywordLabel);

		objectRAEODSaveKeywordTF = new JTextField (objectRAEODSaveKeyword);
        objectRAEODSaveKeywordTF.setMargin(fitsMargin);
        objectRAEODSaveKeywordTF.setFont(p12);
		objectRAEODSaveKeywordTF.setPreferredSize(fitsSize);
		objectRAEODSaveKeywordTF.setHorizontalAlignment(JTextField.LEFT);
        objectRAEODSaveKeywordTF.setToolTipText("<html>"+"Enter keyword used when saving target Epoch-of-Date RA to FITS header"+"</html>");
        objectRAEODSaveKeywordTF.getDocument().addDocumentListener(new thisDocumentListener());
        fitsWritePanel.add (objectRAEODSaveKeywordTF);

        saveObjectRAJ2000CB = new JCheckBox("enable", saveObjectRAJ2000);
        saveObjectRAJ2000CB.setPreferredSize(fitsCBSize);
        saveObjectRAJ2000CB.setToolTipText("Select to save target Epoch-of-Date RA to FITS header");
		saveObjectRAJ2000CB.addItemListener (this);
        fitsWritePanel.add(saveObjectRAJ2000CB);

        JLabel objectDecEODSaveKeywordLabel = new JLabel ("Target DEC Keyword:");
        objectDecEODSaveKeywordLabel.setPreferredSize(fitsLabelSize);
        objectDecEODSaveKeywordLabel.setFont(p12);
        objectDecEODSaveKeywordLabel.setHorizontalAlignment (JTextField.RIGHT);
		fitsWritePanel.add (objectDecEODSaveKeywordLabel);

		objectDecEODSaveKeywordTF = new JTextField (objectDecEODSaveKeyword);
        objectDecEODSaveKeywordTF.setMargin(fitsMargin);
        objectDecEODSaveKeywordTF.setFont(p12);
		objectDecEODSaveKeywordTF.setPreferredSize(fitsSize);
		objectDecEODSaveKeywordTF.setHorizontalAlignment(JTextField.LEFT);
        objectDecEODSaveKeywordTF.setToolTipText("<html>"+"Enter keyword used when saving target Epoch-of-Date DEC to FITS header"+"</html>");
        objectDecEODSaveKeywordTF.getDocument().addDocumentListener(new thisDocumentListener());
        fitsWritePanel.add (objectDecEODSaveKeywordTF);

        saveObjectDecEODCB = new JCheckBox("enable", saveObjectDecEOD);
        saveObjectDecEODCB.setPreferredSize(fitsCBSize);
        saveObjectDecEODCB.setToolTipText("Select to save target Epoch-of-Date DEC to FITS header");
		saveObjectDecEODCB.addItemListener (this);
        fitsWritePanel.add(saveObjectDecEODCB);

        JLabel objectAltitudeSaveKeywordLabel = new JLabel ("Target Altitude Keyword:");
        objectAltitudeSaveKeywordLabel.setPreferredSize(fitsLabelSize);
        objectAltitudeSaveKeywordLabel.setFont(p12);
        objectAltitudeSaveKeywordLabel.setHorizontalAlignment (JTextField.RIGHT);
		fitsWritePanel.add (objectAltitudeSaveKeywordLabel);

		objectAltitudeSaveKeywordTF = new JTextField (objectAltitudeSaveKeyword);
        objectAltitudeSaveKeywordTF.setMargin(fitsMargin);
        objectAltitudeSaveKeywordTF.setFont(p12);
		objectAltitudeSaveKeywordTF.setPreferredSize(fitsSize);
		objectAltitudeSaveKeywordTF.setHorizontalAlignment(JTextField.LEFT);
        objectAltitudeSaveKeywordTF.setToolTipText("<html>"+"Enter keyword used when saving target Altitude to FITS header"+"</html>");
        objectAltitudeSaveKeywordTF.getDocument().addDocumentListener(new thisDocumentListener());
        fitsWritePanel.add (objectAltitudeSaveKeywordTF);

        saveObjectAltitudeCB = new JCheckBox("enable", saveObjectAltitude);
        saveObjectAltitudeCB.setPreferredSize(fitsCBSize);
        saveObjectAltitudeCB.setToolTipText("Select to save target Altitude to FITS header");
		saveObjectAltitudeCB.addItemListener (this);
        fitsWritePanel.add(saveObjectAltitudeCB);

        JLabel objectAzimuthSaveKeywordLabel = new JLabel ("Target Azimuth Keyword:");
        objectAzimuthSaveKeywordLabel.setPreferredSize(fitsLabelSize);
        objectAzimuthSaveKeywordLabel.setFont(p12);
        objectAzimuthSaveKeywordLabel.setHorizontalAlignment (JTextField.RIGHT);
		fitsWritePanel.add (objectAzimuthSaveKeywordLabel);

		objectAzimuthSaveKeywordTF = new JTextField (objectAzimuthSaveKeyword);
        objectAzimuthSaveKeywordTF.setMargin(fitsMargin);
        objectAzimuthSaveKeywordTF.setFont(p12);
		objectAzimuthSaveKeywordTF.setPreferredSize(fitsSize);
		objectAzimuthSaveKeywordTF.setHorizontalAlignment(JTextField.LEFT);
        objectAzimuthSaveKeywordTF.setToolTipText("<html>"+"Enter keyword used when saving target Azimuth to FITS header"+"</html>");
        objectAzimuthSaveKeywordTF.getDocument().addDocumentListener(new thisDocumentListener());
        fitsWritePanel.add (objectAzimuthSaveKeywordTF);

        saveObjectAzimuthCB = new JCheckBox("enable", saveObjectAzimuth);
        saveObjectAzimuthCB.setPreferredSize(fitsCBSize);
        saveObjectAzimuthCB.setToolTipText("Select to save target Azimuth to FITS header");
		saveObjectAzimuthCB.addItemListener (this);
        fitsWritePanel.add(saveObjectAzimuthCB);
        

        JLabel objectHourAngleSaveKeywordLabel = new JLabel ("Target Hour Angle Keyword:");
        objectHourAngleSaveKeywordLabel.setPreferredSize(fitsLabelSize);
        objectHourAngleSaveKeywordLabel.setFont(p12);
        objectHourAngleSaveKeywordLabel.setHorizontalAlignment (JTextField.RIGHT);
		fitsWritePanel.add (objectHourAngleSaveKeywordLabel);

		objectHourAngleSaveKeywordTF = new JTextField (objectHourAngleSaveKeyword);
        objectHourAngleSaveKeywordTF.setMargin(fitsMargin);
        objectHourAngleSaveKeywordTF.setFont(p12);
		objectHourAngleSaveKeywordTF.setPreferredSize(fitsSize);
		objectHourAngleSaveKeywordTF.setHorizontalAlignment(JTextField.LEFT);
        objectHourAngleSaveKeywordTF.setToolTipText("<html>"+"Enter keyword used when saving target Hour Angle to FITS header"+"</html>");
        objectHourAngleSaveKeywordTF.getDocument().addDocumentListener(new thisDocumentListener());
        fitsWritePanel.add (objectHourAngleSaveKeywordTF);

        saveObjectHourAngleCB = new JCheckBox("enable", saveObjectHourAngle);
        saveObjectHourAngleCB.setPreferredSize(fitsCBSize);
        saveObjectHourAngleCB.setToolTipText("Select to save target Hour Angle to FITS header");
		saveObjectHourAngleCB.addItemListener (this);
        fitsWritePanel.add(saveObjectHourAngleCB);        
        

        JLabel objectZenithDistanceSaveKeywordLabel = new JLabel ("Target Zenith Distance Keyword:");
        objectZenithDistanceSaveKeywordLabel.setPreferredSize(fitsLabelSize);
        objectZenithDistanceSaveKeywordLabel.setFont(p12);
        objectZenithDistanceSaveKeywordLabel.setHorizontalAlignment (JTextField.RIGHT);
		fitsWritePanel.add (objectZenithDistanceSaveKeywordLabel);

		objectZenithDistanceSaveKeywordTF = new JTextField (objectZenithDistanceSaveKeyword);
        objectZenithDistanceSaveKeywordTF.setMargin(fitsMargin);
        objectZenithDistanceSaveKeywordTF.setFont(p12);
		objectZenithDistanceSaveKeywordTF.setPreferredSize(fitsSize);
		objectZenithDistanceSaveKeywordTF.setHorizontalAlignment(JTextField.LEFT);
        objectZenithDistanceSaveKeywordTF.setToolTipText("<html>"+"Enter keyword used when saving target Zenith Distance to FITS header"+"</html>");
        objectZenithDistanceSaveKeywordTF.getDocument().addDocumentListener(new thisDocumentListener());
        fitsWritePanel.add (objectZenithDistanceSaveKeywordTF);

        saveObjectZenithDistanceCB = new JCheckBox("enable", saveObjectZenithDistance);
        saveObjectZenithDistanceCB.setPreferredSize(fitsCBSize);
        saveObjectZenithDistanceCB.setToolTipText("Select to save target Zenith Distance to FITS header");
		saveObjectZenithDistanceCB.addItemListener (this);
        fitsWritePanel.add(saveObjectZenithDistanceCB);         

        JLabel fitsDummyLabel5 = new JLabel ("");
        fitsDummyLabel5.setPreferredSize(fitsDummySize);
		fitsWritePanel.add (fitsDummyLabel5);

        JLabel fitsDummyLabel6 = new JLabel ("");
        fitsDummyLabel6.setPreferredSize(fitsDummySize);
		fitsWritePanel.add (fitsDummyLabel6);

        JLabel fitsDummyLabel7 = new JLabel ("");
        fitsDummyLabel7.setPreferredSize(fitsDummySize);
		fitsWritePanel.add (fitsDummyLabel7);

        JLabel objectAirmassSaveKeywordLabel = new JLabel ("Target Airmass Keyword:");
        objectAirmassSaveKeywordLabel.setPreferredSize(fitsLabelSize);
        objectAirmassSaveKeywordLabel.setFont(p12);
        objectAirmassSaveKeywordLabel.setHorizontalAlignment (JTextField.RIGHT);
		fitsWritePanel.add (objectAirmassSaveKeywordLabel);

		objectAirmassSaveKeywordTF = new JTextField (objectAirmassSaveKeyword);
        objectAirmassSaveKeywordTF.setMargin(fitsMargin);
        objectAirmassSaveKeywordTF.setFont(p12);
		objectAirmassSaveKeywordTF.setPreferredSize(fitsSize);
		objectAirmassSaveKeywordTF.setHorizontalAlignment(JTextField.LEFT);
        objectAirmassSaveKeywordTF.setToolTipText("<html>"+"Enter keyword used when saving target Airmass to FITS header"+"</html>");
        objectAirmassSaveKeywordTF.getDocument().addDocumentListener(new thisDocumentListener());
        fitsWritePanel.add (objectAirmassSaveKeywordTF);

        saveObjectAirmassCB = new JCheckBox("enable", saveObjectAirmass);
        saveObjectAirmassCB.setPreferredSize(fitsCBSize);
        saveObjectAirmassCB.setToolTipText("Select to save target Airmass to FITS header");
		saveObjectAirmassCB.addItemListener (this);
        fitsWritePanel.add(saveObjectAirmassCB);

        JLabel fitsDummyLabel8 = new JLabel ("");
        fitsDummyLabel8.setPreferredSize(fitsDummySize);
		fitsWritePanel.add (fitsDummyLabel8);

        JLabel fitsDummyLabel9 = new JLabel ("");
        fitsDummyLabel9.setPreferredSize(fitsDummySize);
		fitsWritePanel.add (fitsDummyLabel9);

        JLabel fitsDummyLabel10 = new JLabel ("");
        fitsDummyLabel10.setPreferredSize(fitsDummySize);
		fitsWritePanel.add (fitsDummyLabel10);

        JLabel JD_SOBSSaveKeywordLabel = new JLabel ("JD (UTC) start-Obs Keyword:");
        JD_SOBSSaveKeywordLabel.setPreferredSize(fitsLabelSize);
        JD_SOBSSaveKeywordLabel.setFont(p12);
        JD_SOBSSaveKeywordLabel.setHorizontalAlignment (JTextField.RIGHT);
		fitsWritePanel.add (JD_SOBSSaveKeywordLabel);

		JD_SOBSSaveKeywordTF = new JTextField (JD_SOBSSaveKeyword);
        JD_SOBSSaveKeywordTF.setMargin(fitsMargin);
        JD_SOBSSaveKeywordTF.setFont(p12);
		JD_SOBSSaveKeywordTF.setPreferredSize(fitsSize);
		JD_SOBSSaveKeywordTF.setHorizontalAlignment(JTextField.LEFT);
        JD_SOBSSaveKeywordTF.setToolTipText("<html>"+"Enter keyword used when saving JD (UTC) at start of observation to FITS header"+"</html>");
        JD_SOBSSaveKeywordTF.getDocument().addDocumentListener(new thisDocumentListener());
        fitsWritePanel.add (JD_SOBSSaveKeywordTF);

        saveJD_SOBSCB = new JCheckBox("enable", saveJD_SOBS);
        saveJD_SOBSCB.setPreferredSize(fitsCBSize);
        saveJD_SOBSCB.setToolTipText("Select to save JD (UTC) at start of observation to FITS header");
		saveJD_SOBSCB.addItemListener (this);
        fitsWritePanel.add(saveJD_SOBSCB);

        JLabel JD_MOBSSaveKeywordLabel = new JLabel ("JD (UTC) mid-Obs Keyword:");
        JD_MOBSSaveKeywordLabel.setPreferredSize(fitsLabelSize);
        JD_MOBSSaveKeywordLabel.setFont(p12);
        JD_MOBSSaveKeywordLabel.setHorizontalAlignment (JTextField.RIGHT);
		fitsWritePanel.add (JD_MOBSSaveKeywordLabel);

		JD_MOBSSaveKeywordTF = new JTextField (JD_MOBSSaveKeyword);
        JD_MOBSSaveKeywordTF.setMargin(fitsMargin);
        JD_MOBSSaveKeywordTF.setFont(p12);
		JD_MOBSSaveKeywordTF.setPreferredSize(fitsSize);
		JD_MOBSSaveKeywordTF.setHorizontalAlignment(JTextField.LEFT);
        JD_MOBSSaveKeywordTF.setToolTipText("<html>"+"Enter keyword used when saving JD (UTC) at middle of observation to FITS header"+"</html>");
        JD_MOBSSaveKeywordTF.getDocument().addDocumentListener(new thisDocumentListener());
        fitsWritePanel.add (JD_MOBSSaveKeywordTF);

        saveJD_MOBSCB = new JCheckBox("enable", saveJD_MOBS);
        saveJD_MOBSCB.setPreferredSize(fitsCBSize);
        saveJD_MOBSCB.setToolTipText("Select to save JD (UTC) at middle of observation to FITS header");
		saveJD_MOBSCB.addItemListener (this);
        fitsWritePanel.add(saveJD_MOBSCB);

        JLabel HJD_MOBSSaveKeywordLabel = new JLabel ("HJD (UTC) mid-Obs Keyword:");
        HJD_MOBSSaveKeywordLabel.setPreferredSize(fitsLabelSize);
        HJD_MOBSSaveKeywordLabel.setFont(p12);
        HJD_MOBSSaveKeywordLabel.setHorizontalAlignment (JTextField.RIGHT);
		fitsWritePanel.add (HJD_MOBSSaveKeywordLabel);

		HJD_MOBSSaveKeywordTF = new JTextField (HJD_MOBSSaveKeyword);
        HJD_MOBSSaveKeywordTF.setMargin(fitsMargin);
        HJD_MOBSSaveKeywordTF.setFont(p12);
		HJD_MOBSSaveKeywordTF.setPreferredSize(fitsSize);
		HJD_MOBSSaveKeywordTF.setHorizontalAlignment(JTextField.LEFT);
        HJD_MOBSSaveKeywordTF.setToolTipText("<html>"+"Enter keyword used when saving Heliocentric JD (UTC) at middle of observation to FITS header"+"</html>");
        HJD_MOBSSaveKeywordTF.getDocument().addDocumentListener(new thisDocumentListener());
        fitsWritePanel.add (HJD_MOBSSaveKeywordTF);

        saveHJD_MOBSCB = new JCheckBox("enable", saveHJD_MOBS);
        saveHJD_MOBSCB.setPreferredSize(fitsCBSize);
        saveHJD_MOBSCB.setToolTipText("Select to save Heliocentric JD (UTC) at middle of observation to FITS header");
		saveHJD_MOBSCB.addItemListener (this);
        fitsWritePanel.add(saveHJD_MOBSCB);

        JLabel BJD_MOBSSaveKeywordLabel = new JLabel ("BJD (TDB) mid-Obs Keyword:");
        BJD_MOBSSaveKeywordLabel.setPreferredSize(fitsLabelSize);
        BJD_MOBSSaveKeywordLabel.setFont(p12);
        BJD_MOBSSaveKeywordLabel.setHorizontalAlignment (JTextField.RIGHT);
		fitsWritePanel.add (BJD_MOBSSaveKeywordLabel);

		BJD_MOBSSaveKeywordTF = new JTextField (BJD_MOBSSaveKeyword);
        BJD_MOBSSaveKeywordTF.setMargin(fitsMargin);
        BJD_MOBSSaveKeywordTF.setFont(p12);
		BJD_MOBSSaveKeywordTF.setPreferredSize(fitsSize);
		BJD_MOBSSaveKeywordTF.setHorizontalAlignment(JTextField.LEFT);
        BJD_MOBSSaveKeywordTF.setToolTipText("<html>"+"Enter keyword used when saving Barycentric JD (TDB) at middle of observation to FITS header"+"</html>");
        BJD_MOBSSaveKeywordTF.getDocument().addDocumentListener(new thisDocumentListener());
        fitsWritePanel.add (BJD_MOBSSaveKeywordTF);

        saveBJD_MOBSCB = new JCheckBox("enable", saveBJD_MOBS);
        saveBJD_MOBSCB.setPreferredSize(fitsCBSize);
        saveBJD_MOBSCB.setToolTipText("Select to save Barycentric JD (TDB) at middle of observation to FITS header");
		saveBJD_MOBSCB.addItemListener (this);
        fitsWritePanel.add(saveBJD_MOBSCB);


        JLabel fitsDummyLabel11 = new JLabel ("");
        fitsDummyLabel11.setPreferredSize(fitsDummySize);
		fitsWritePanel.add (fitsDummyLabel11);

        JLabel fitsDummyLabel12 = new JLabel ("");
        fitsDummyLabel12.setPreferredSize(fitsDummySize);
		fitsWritePanel.add (fitsDummyLabel12);

        JLabel fitsDummyLabel13 = new JLabel ("");
        fitsDummyLabel13.setPreferredSize(fitsDummySize);
		fitsWritePanel.add (fitsDummyLabel13);

        JLabel observatoryLatSaveKeywordLabel = new JLabel ("Observatory Latitude Keyword:");
        observatoryLatSaveKeywordLabel.setPreferredSize(fitsLabelSize);
        observatoryLatSaveKeywordLabel.setFont(p12);
        observatoryLatSaveKeywordLabel.setHorizontalAlignment (JTextField.RIGHT);
		fitsWritePanel.add (observatoryLatSaveKeywordLabel);

		observatoryLatSaveKeywordTF = new JTextField (observatoryLatSaveKeyword);
        observatoryLatSaveKeywordTF.setMargin(fitsMargin);
        observatoryLatSaveKeywordTF.setFont(p12);
		observatoryLatSaveKeywordTF.setPreferredSize(fitsSize);
		observatoryLatSaveKeywordTF.setHorizontalAlignment(JTextField.LEFT);
        observatoryLatSaveKeywordTF.setToolTipText("<html>"+"Enter keyword used when saving observatory latitude to FITS header (north positive)"+"</html>");
        observatoryLatSaveKeywordTF.getDocument().addDocumentListener(new thisDocumentListener());
        fitsWritePanel.add (observatoryLatSaveKeywordTF);

        saveObservatoryLatCB = new JCheckBox("enable", saveObservatoryLat);
        saveObservatoryLatCB.setPreferredSize(fitsCBSize);
        saveObservatoryLatCB.setToolTipText("Select to save observatory latitude to FITS header (north positive)");
		saveObservatoryLatCB.addItemListener (this);
        fitsWritePanel.add(saveObservatoryLatCB);

        JLabel observatoryLonSaveKeywordLabel = new JLabel ("Observatory Longitude Keyword:");
        observatoryLonSaveKeywordLabel.setPreferredSize(fitsLabelSize);
        observatoryLonSaveKeywordLabel.setFont(p12);
        observatoryLonSaveKeywordLabel.setHorizontalAlignment (JTextField.RIGHT);
		fitsWritePanel.add (observatoryLonSaveKeywordLabel);

		observatoryLonSaveKeywordTF = new JTextField (observatoryLonSaveKeyword);
        observatoryLonSaveKeywordTF.setMargin(fitsMargin);
        observatoryLonSaveKeywordTF.setFont(p12);
		observatoryLonSaveKeywordTF.setPreferredSize(fitsSize);
		observatoryLonSaveKeywordTF.setHorizontalAlignment(JTextField.LEFT);
        observatoryLonSaveKeywordTF.setToolTipText("<html>"+"Enter keyword used when saving observatory longitude to FITS header (east positive)"+"</html>");
        observatoryLonSaveKeywordTF.getDocument().addDocumentListener(new thisDocumentListener());
        fitsWritePanel.add (observatoryLonSaveKeywordTF);

        saveObservatoryLonCB = new JCheckBox("enable", saveObservatoryLon);
        saveObservatoryLonCB.setPreferredSize(fitsCBSize);
        saveObservatoryLonCB.setToolTipText("Select to save observatory longitude to FITS header (east positive)");
		saveObservatoryLonCB.addItemListener (this);
        fitsWritePanel.add(saveObservatoryLonCB);



       
        SpringUtil.makeCompactGrid (fitsWritePanel, fitsWritePanel.getComponentCount()/3,3, 2,2,2,2);
        fitsHeaderPanel.add(fitsWritePanel);
        
        SpringUtil.makeCompactGrid (fitsHeaderPanel, fitsHeaderPanel.getComponentCount(),1, 2,2,2,2);
        fitsHeaderScrollPane = new JScrollPane(fitsHeaderPanel);
        fitsHeaderFrame.add(fitsHeaderScrollPane);
		fitsHeaderFrame.pack();
		fitsHeaderFrame.setResizable (true);

        
//********************************************************************************************************



		dialogFrame = new JFrame ("CCD Data Processor");
        dialogFrame.setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        dialogFrame.addWindowListener(new WindowAdapter(){
            @Override
            public void windowClosing(WindowEvent e){
            saveAndClose();}});

		JPanel mainPanel = new JPanel (new SpringLayout());
		mainPanel.setBorder (BorderFactory.createEmptyBorder(0,10,10,10));

        FileDrop fileDrop = new FileDrop(mainPanel, BorderFactory.createEmptyBorder(0,10,10,10),new FileDrop.Listener()
            {   public void filesDropped( java.io.File[] files )
                {
                openDragAndDropFiles(files);
                }
            });        
        
        mainScrollPane = new JScrollPane(mainPanel);

        dialogFrame.setIconImage(windowIcon.getImage());
        f.setTimeZone(java.util.TimeZone.getTimeZone("UTC"));
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));

        // MAKE MENUBAR and MENUS
        menuBar = new JMenuBar();

        fileMenu = new JMenu("   File");
        exitMenuItem = new JMenuItem("Exit Data Processor");
        exitMenuItem.addActionListener(this);
        fileMenu.add(exitMenuItem);


        menuBar.add(fileMenu);

        prefsMenu = new JMenu("Preferences");

        showToolTipsCB = new JCheckBoxMenuItem("Show tool tips", showToolTips);
        showToolTipsCB.addItemListener(this);
        prefsMenu.add(showToolTipsCB);

        prefsMenu.addSeparator();

        autoWildcardCB = new JCheckBoxMenuItem("Insert '*' wildcard between last '_' and '.' in filename patterns", autoWildcard);
        autoWildcardCB.addItemListener(this);
        prefsMenu.add(autoWildcardCB);

        rawCalCommonDirCB = new JCheckBoxMenuItem("Use science directory for default raw calibration file directory", rawCalCommonDir);
        rawCalCommonDirCB.addItemListener(this);
        prefsMenu.add(rawCalCommonDirCB);

        masterCalCommonDirCB = new JCheckBoxMenuItem("Use science directory for default master calibration file directory", masterCalCommonDir);
        masterCalCommonDirCB.addItemListener(this);
        prefsMenu.add(masterCalCommonDirCB);

        onlyNewCB = new JCheckBoxMenuItem("Process only new science files written after pressing 'Start' button", onlyNew);
        onlyNewCB.addItemListener(this);
        prefsMenu.add(onlyNewCB);

        prefsMenu.addSeparator();

        useBeepCB = new JCheckBoxMenuItem("Beep after processing each science image", useBeep);
        useBeepCB.addItemListener(this);
        prefsMenu.add(useBeepCB);
        
        prefsMenu.addSeparator();
        
        setFileSizeChangeWaitTimeMenuItem = new JMenuItem("Set time to wait for new file writes to complete...");
                setFileSizeChangeWaitTimeMenuItem.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                            setfileSizeChangeWaitTime();
                            }});
                prefsMenu.add(setFileSizeChangeWaitTimeMenuItem);
                
        prefsMenu.addSeparator();
        
        autoRunAndCloseCB = new JCheckBoxMenuItem("Automatically run and close DP. Keep disabled. For macro processing only.", autoRunAndClose);
        autoRunAndCloseCB.addItemListener(this);
        prefsMenu.add(autoRunAndCloseCB);
        
        menuBar.add(prefsMenu);

        

        viewMenu = new JMenu("View");

        showScienceCB = new JCheckBoxMenuItem("Show science images while processing",showScience);
		showScienceCB.addItemListener (this);
        viewMenu.add(showScienceCB);
        
        showRawCalsCB = new JCheckBoxMenuItem("Show raw calibration images while building master calibration files (requires more memory)", showRawCals);
        showRawCalsCB.addItemListener(this);
        viewMenu.add(showRawCalsCB);  
        
        showMasterImagesCB = new JCheckBoxMenuItem("Show master calibration images (requires more memory)", showMasters);
        showMasterImagesCB.addItemListener(this);
        viewMenu.add(showMasterImagesCB);
        
        useShowLogCB = new JCheckBoxMenuItem("Show log of data processing history", showLog);
        useShowLogCB.addItemListener(this);
        viewMenu.add(useShowLogCB);
        
        showLogDateTimeCB = new JCheckBoxMenuItem("Show date/time stamp for each log entry", showLogDateTime);
        showLogDateTimeCB.addItemListener(this);
        viewMenu.add(showLogDateTimeCB);

        viewMenu.addSeparator();

        usepreMacro1AutoLevelCB = new JCheckBoxMenuItem("Auto scale brightness and contrast after processing science image", preMacro1AutoLevel);
        usepreMacro1AutoLevelCB.addItemListener(this);
        viewMenu.add(usepreMacro1AutoLevelCB);
		postMacro1AutoLevelCB = new JCheckBoxMenuItem("Auto scale brightness and contrast after running Macro 1",postMacro1AutoLevel);
		postMacro1AutoLevelCB.addItemListener (this);
        viewMenu.add(postMacro1AutoLevelCB);
		postMacro2AutoLevelCB = new JCheckBoxMenuItem("Auto scale brightness and contrast after running Macro 2",postMacro2AutoLevel);
		postMacro2AutoLevelCB.addItemListener (this);
        viewMenu.add(postMacro2AutoLevelCB);

        menuBar.add(viewMenu);

        JPanel labelPanel = new JPanel(new SpringLayout());
        labelPanel.setBorder(BorderFactory.createEmptyBorder(0,6,0,8));

        JLabel labelrowdummyLabel = new JLabel ("Control");
        labelrowdummyLabel.setFont(b12);
        labelrowdummyLabel.setPreferredSize(checkBoxSize);
        labelrowdummyLabel.setHorizontalAlignment (JLabel.LEFT);
		labelPanel.add (labelrowdummyLabel);

        JLabel labelrow2dummyLabel = new JLabel ("Options");
        labelrow2dummyLabel.setFont(b12);
        labelrow2dummyLabel.setPreferredSize(radioSize);
        labelrow2dummyLabel.setHorizontalAlignment (JLabel.CENTER);
		labelPanel.add (labelrow2dummyLabel);

        JTextField dirLabel = new JTextField ("Directory");
        dirLabel.setFont(b12);
        dirLabel.setEditable(false);
        dirLabel.setBackground(gray);
        dirLabel.setBorder(BorderFactory.createEmptyBorder());
        dirLabel.setPreferredSize(new Dimension(directoryWidth, topLabelHeight));
        dirLabel.setHorizontalAlignment (JLabel.CENTER);
		labelPanel.add (dirLabel);

        JLabel labelDummyLabel1 = new JLabel ("");
        labelDummyLabel1.setFont(p12);
        labelDummyLabel1.setPreferredSize(new Dimension(folderIconWidth, topLabelHeight));
        labelDummyLabel1.setHorizontalAlignment (JLabel.CENTER);
		labelPanel.add (labelDummyLabel1);

        JTextField filenameLabel = new JTextField ("Filename/Pattern");
        filenameLabel.setFont(b12);
        filenameLabel.setEditable(false);
        filenameLabel.setBackground(gray);
        filenameLabel.setBorder(BorderFactory.createEmptyBorder());
        filenameLabel.setPreferredSize(new Dimension(fileWidth, topLabelHeight));
        filenameLabel.setHorizontalAlignment (JLabel.CENTER);
		labelPanel.add (filenameLabel);

        JLabel labelDummyLabel2 = new JLabel ("");
        labelDummyLabel2.setFont(p12);
        labelDummyLabel2.setPreferredSize(new Dimension(folderIconWidth, topLabelHeight));
        labelDummyLabel2.setHorizontalAlignment (JLabel.CENTER);
		labelPanel.add (labelDummyLabel2);

        JLabel foundLabel = new JLabel ("Totals");
        foundLabel.setFont(b12);
        foundLabel.setPreferredSize(new Dimension(validFilesWidth, topLabelHeight));
        foundLabel.setHorizontalAlignment (JLabel.RIGHT);
		labelPanel.add (foundLabel);

        
        SpringUtil.makeCompactGrid (labelPanel, 1, labelPanel.getComponentCount(), 10,0,6,0);
        mainPanel.add (labelPanel);



        JPanel sciencePanel = new JPanel(new SpringLayout());
        sciencePanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(mainBorderColor, 1), "Science Image Processing", TitledBorder.LEFT, TitledBorder.TOP, b12, Color.DARK_GRAY));

        JPanel scienceImagePanel = new JPanel(new SpringLayout());
        scienceImagePanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(Color.lightGray, 1), "Filename Pattern Matching", TitledBorder.LEFT, TitledBorder.TOP, p12, Color.DARK_GRAY));

        useScienceProcessingBox = new JCheckBox("Enable", useScienceProcessing);
        useScienceProcessingBox.setFont(p12);
        useScienceProcessingBox.setPreferredSize (checkBoxSize);
        useScienceProcessingBox.setToolTipText("<html>Enable science file processing.</html>");
		useScienceProcessingBox.addItemListener (this);
        scienceImagePanel.add(useScienceProcessingBox);

		sortNumericallyBox = new JCheckBox("Sort Num", sortNumerically);
        sortNumericallyBox.setFont(p12);
        sortNumericallyBox.setPreferredSize (radioSize);
        sortNumericallyBox.setToolTipText("<html>Sort science files numerically before processing. The file processing order is determined<br>"+
                                          "by sorting the numbers obtained by combining all numeric characters of a file name.</html>");
		sortNumericallyBox.addItemListener (this);
        scienceImagePanel.add(sortNumericallyBox);


//        JLabel secondrowdummyLabel = new JLabel ("");
//        secondrowdummyLabel.setFont(p12);
//        secondrowdummyLabel.setPreferredSize(checkBoxSize);
//        secondrowdummyLabel.setHorizontalAlignment (JLabel.CENTER);
//		scienceImagePanel.add (secondrowdummyLabel);
//
//        JPanel scienceOptionPanel = new JPanel(new SpringLayout());
//        scienceOptionPanel.setPreferredSize(radioSize);
//
//        SpringUtil.makeCompactGrid (scienceOptionPanel, 1, scienceOptionPanel.getComponentCount(), 0,0,0,0);
//        scienceImagePanel.add(scienceOptionPanel);

		dirText = new JTextField (mainDir);
        scienceDir = new File (mainDir);
        dirText.setFont(p12);
//        dirText.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(Color.lightGray, 1), "primary science directory", TitledBorder.CENTER, TitledBorder.BELOW_TOP, p8, Color.DARK_GRAY));
        dirText.setMargin(textboxMargin);
        dirText.getDocument().addDocumentListener(new thisDocumentListener());
        dirText.setToolTipText("<html>Enter primary folder name containing science images for processing<br>"+
                                "or click folder opener or file opener to right</html>");
		dirText.setPreferredSize (directoryBoxSize);
		dirText.setHorizontalAlignment (JTextField.LEFT);
        FileDrop fileDropdirText = new FileDrop(dirText, BorderFactory.createEmptyBorder(0,10,10,10),new FileDrop.Listener()
            {   public void filesDropped( java.io.File[] files )
                {
                openDragAndDropFileNames(files, dirText, filenamePatternText, autoWildcard);
                }
            });        
        scienceImagePanel.add (dirText);
             
		dirButton = new JButton (folderOpenIcon);
        dirButton.setOpaque(false);
        dirButton.setFocusPainted(false);
        dirButton.setBorderPainted(false);
        dirButton.setContentAreaFilled(false);
        dirButton.setBorder(BorderFactory.createEmptyBorder(0,0,0,folderIconRightMargin));
        dirButton.setToolTipText("<html>Click to define primary folder containing science images for processing<br>"+
                                 "or click file opener to right and select a file in the desired folder</html>");
		dirButton.addActionListener (this);
        dirButton.setPreferredSize(folderIconSize);
		scienceImagePanel.add (dirButton);

		filenamePatternText = new JTextField (filenamePattern);
        filenamePatternText.setMargin(textboxMargin);
        filenamePatternText.setFont(p12);
		filenamePatternText.setPreferredSize (fileBoxSize);
        filenamePatternText.setToolTipText("<html>Enter a science image filename pattern using ? and * as wildcard characters<br>"+
                                            "or click the file opener to the right to select a representative file. If a file is not currently available,<br>"+
                                            "use the folder opener to the left to select the folder and type the filename pattern here.</html>");
		filenamePatternText.setHorizontalAlignment (JTextField.LEFT);
        filenamePatternText.getDocument().addDocumentListener(new thisDocumentListener());
        FileDrop fileDropfilenamePatternText = new FileDrop(filenamePatternText, BorderFactory.createEmptyBorder(0,10,10,10),new FileDrop.Listener()
            {   public void filesDropped( java.io.File[] files )
                {
                openDragAndDropFileNames(files, dirText, filenamePatternText, autoWildcard);
                }
            });         
		scienceImagePanel.add (filenamePatternText);

		fileButton = new JButton (fileOpenIcon);
        fileButton.setPreferredSize(folderIconSize);
        fileButton.setOpaque(false);
        fileButton.setFocusPainted(false);
        fileButton.setBorderPainted(false);
        fileButton.setContentAreaFilled(false);
        fileButton.setBorder(BorderFactory.createEmptyBorder(0,0,0,folderIconRightMargin));
        fileButton.setToolTipText("<html>Click to select a representative file for processing. If the preferences option: \"Insert wildcard between<br>"+
                                    "last '_' and '.'\" is selected, a pattern will be derived from the selected file. Otherwise, hand modify the selected<br>"+
                                    "filename using ? and * wildcard characters. If a file is not currently available,<br>"+
                                    "use the folder opener to the left to select the folder and type the filename pattern directly.</html>");
		fileButton.addActionListener (this);
		scienceImagePanel.add (fileButton);

        validTextFilteredFilesLabel = new JLabel ("");
        validTextFilteredFilesLabel.setToolTipText("The number of science files in the primary directory matching the filename pattern");
        validTextFilteredFilesLabel.setHorizontalAlignment(JLabel.RIGHT);
        validTextFilteredFilesLabel.setFont(b12);
		validTextFilteredFilesLabel.setPreferredSize (validFilesSize);
        scienceImagePanel.add (validTextFilteredFilesLabel);

        SpringUtil.makeCompactGrid (scienceImagePanel, 1, scienceImagePanel.getComponentCount(), 4,0,2,2);
		sciencePanel.add (scienceImagePanel);



        JPanel scienceNumberPanel = new JPanel(new SpringLayout());
        scienceNumberPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY, 1), "Filename Number Filtering", TitledBorder.LEFT, TitledBorder.TOP, p12, Color.DARK_GRAY));

		enableFileNumberFilteringBox = new JCheckBox("Enable",enableFileNumberFiltering);
        enableFileNumberFilteringBox.setFont(p12);
        enableFileNumberFilteringBox.setPreferredSize(checkBoxSize);
		enableFileNumberFilteringBox.addItemListener (this);
        enableFileNumberFilteringBox.setToolTipText("<html>If selected, filename number filtering is enabled. The number obtained by combining all<br>"+
                                                    "numeric characters in the filename, excluding the characters in the ignore field, must fall between<br>"+
                                                    "the values in min and max for the file to be included in processing.</html>");
		scienceNumberPanel.add (enableFileNumberFilteringBox);

        JPanel numberFilterOptionPanel = new JPanel(new SpringLayout());
        numberFilterOptionPanel.setPreferredSize(radioSize);

        SpringUtil.makeCompactGrid (numberFilterOptionPanel, 1, numberFilterOptionPanel.getComponentCount(), 0,0,0,0);
        scienceNumberPanel.add(numberFilterOptionPanel);

        JPanel minmaxValuePanel = new JPanel(new SpringLayout());
        minmaxValuePanel.setPreferredSize(directoryBoxSize);

        minFileNumberLabel = new JLabel("Min:");
        minFileNumberLabel.setFont(p12);
        minFileNumberLabel.setHorizontalAlignment (JLabel.RIGHT);
        minmaxValuePanel.add(minFileNumberLabel);

        minFileNumberModel = new SpinnerNumberModel(minFileNumber, 0L, null, 1L);
        minFileNumberSpinner = new JSpinner(minFileNumberModel);
        JSpinner.NumberEditor minFileNumberEditor = new JSpinner.NumberEditor( minFileNumberSpinner, "#" );
        minFileNumberSpinner.setEditor( minFileNumberEditor );
//        minFileNumberSpinner.setPreferredSize (new Dimension(60,subpanelHeight));
        minFileNumberSpinner.setFont(p12);
        minFileNumberSpinner.setToolTipText("The minimum filename number that will be processed, if number filtering is enabled");
        minFileNumberSpinner.addChangeListener (this);
        minFileNumberSpinner.addMouseWheelListener(this);
        minmaxValuePanel.add(minFileNumberSpinner);

        maxFileNumberLabel = new JLabel("    Max:");
        maxFileNumberLabel.setFont(p12);
        maxFileNumberLabel.setHorizontalAlignment (JLabel.RIGHT);
        minmaxValuePanel.add(maxFileNumberLabel);

        maxFileNumberModel = new SpinnerNumberModel(maxFileNumber, 0L, null, 1L);
        maxFileNumberSpinner = new JSpinner(maxFileNumberModel);
        JSpinner.NumberEditor maxFileNumberEditor = new JSpinner.NumberEditor( maxFileNumberSpinner, "#" );
        maxFileNumberSpinner.setEditor( maxFileNumberEditor );
//        maxFileNumberSpinner.setPreferredSize (new Dimension(100,subpanelHeight));
        maxFileNumberSpinner.setFont(p12);
        maxFileNumberSpinner.setToolTipText("The maximum filename number that will be processed, if number filtering is enabled");
        maxFileNumberSpinner.addChangeListener (this);
        maxFileNumberSpinner.addMouseWheelListener(this);
        minmaxValuePanel.add(maxFileNumberSpinner);

        SpringUtil.makeCompactGrid (minmaxValuePanel, 1, minmaxValuePanel.getComponentCount(), 0,0,2,0);
        scienceNumberPanel.add(minmaxValuePanel);

//        JPanel numPrefixPanel = new JPanel(new SpringLayout());
//        numPrefixPanel.setPreferredSize(new Dimension(fileWidth+iconWidth+6, subpanelHeight));

//        numPrefixLabel = new JLabel("Ignore:");
//        numPrefixLabel.setFont(p12);
//        numPrefixLabel.setHorizontalAlignment (JLabel.RIGHT);
//        scienceNumberPanel.add(numPrefixLabel);

        JLabel numberfilterrowdummyLabel1 = new JLabel ("");
        numberfilterrowdummyLabel1.setFont(p12);
        numberfilterrowdummyLabel1.setPreferredSize(folderIconSize);
        numberfilterrowdummyLabel1.setHorizontalAlignment (JLabel.CENTER);
		scienceNumberPanel.add (numberfilterrowdummyLabel1);

        numPrefixField = new JTextField (numPrefix);
        numPrefixField.setFont(p12);
		numPrefixField.setPreferredSize (fileBoxSize);
		numPrefixField.setHorizontalAlignment (JTextField.LEFT);
        numPrefixField.setToolTipText("<html>"+"Defines the ignored filename character sequences for filtering filenames numerically."+"<br>"+
                                             "Only numbers in the filename after the last occurance of this string"+"<br>"+
                                             "are used for filtering. Leave blank to use all numbers in the filename."+"<br>"+
                                             "Multiple ignored character sequences can be entered by separating them with a '*'."+"</html>");
        numPrefixField.getDocument().addDocumentListener(new thisDocumentListener());
		scienceNumberPanel.add (numPrefixField);

        JLabel numberfilterrowdummyLabel2 = new JLabel ("");
        numberfilterrowdummyLabel2.setFont(p12);
        numberfilterrowdummyLabel2.setPreferredSize(folderIconSize);
        numberfilterrowdummyLabel2.setHorizontalAlignment (JLabel.CENTER);
		scienceNumberPanel.add (numberfilterrowdummyLabel2);

        validNumFilteredFilesLabel = new JLabel ("");
        validNumFilteredFilesLabel.setToolTipText("<html>The number of science files in the primary directory matching<br>"+
                                                   "the filename pattern and filename number filter</html>");
        validNumFilteredFilesLabel.setFont(b12);
        validNumFilteredFilesLabel.setHorizontalAlignment(JLabel.RIGHT);
		validNumFilteredFilesLabel.setPreferredSize (validFilesSize);
        scienceNumberPanel.add (validNumFilteredFilesLabel);

        SpringUtil.makeCompactGrid (scienceNumberPanel, 1, scienceNumberPanel.getComponentCount(), 4,0,2,2);
		sciencePanel.add (scienceNumberPanel);

        SpringUtil.makeCompactGrid (sciencePanel, sciencePanel.getComponentCount(),1, 0,0,2,2);
        mainPanel.add(sciencePanel);


        JPanel biasPanel = new JPanel(new SpringLayout());
        biasPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(mainBorderColor, 1), "Bias Subtraction", TitledBorder.LEFT, TitledBorder.TOP, b12, Color.DARK_GRAY));

        JPanel rawBiasPanel = new JPanel(new SpringLayout());

        createBiasBox = new JCheckBox("Build",createBias);
        createBiasBox.setFont(p12);
        createBiasBox.setPreferredSize (checkBoxSize);
        createBiasBox.setToolTipText("Build a new master bias file from the raw bias files specified to the right");
		createBiasBox.addItemListener (this);
        rawBiasPanel.add(createBiasBox);

        JPanel rawBiasRadioPanel = new JPanel(new SpringLayout());
        rawBiasRadioPanel.setPreferredSize(radioSize);

        biasRadioGroup = new ButtonGroup ();

        biasAverageRadio = new JRadioButton("ave");
        biasAverageRadio.setFont(p12);
        if (!biasMedian) biasAverageRadio.setSelected(true);
        else biasAverageRadio.setSelected(false);
        biasAverageRadio.setToolTipText("average raw files to build master bias");
        rawBiasRadioPanel.add (biasAverageRadio);
        biasMedianRadio = new JRadioButton("med");
        biasMedianRadio.setFont(p12);
        if (biasMedian) biasMedianRadio.setSelected(true);
        else biasMedianRadio.setSelected(false);
        biasMedianRadio.setToolTipText("median raw files to build master bias");
        rawBiasRadioPanel.add (biasMedianRadio);
        biasRadioGroup.add(biasAverageRadio);
        biasRadioGroup.add(biasMedianRadio);
        biasAverageRadio.addActionListener(this);
        biasMedianRadio.addActionListener(this);

        SpringUtil.makeCompactGrid (rawBiasRadioPanel, 1, rawBiasRadioPanel.getComponentCount(), 0,0,0,0);
        rawBiasPanel.add(rawBiasRadioPanel);


		biasRawDirField = new JTextField (biasRawDirText);
        biasRawDirField.setFont(p12);
        biasRawDirField.setMargin(textboxMargin);
//        biasRawDirField.setEnabled(!rawCalCommonDir);
//        biasRawDirField.setBackground(!rawCalCommonDir?Color.white:gray);
        biasRawDirField.setPreferredSize(directoryBoxSize);
        biasRawDirField.setHorizontalAlignment (JTextField.LEFT);
        biasRawDirField.setToolTipText("<html>"+"Enter the directory name where raw bias images are located<br>"+
                                             "or browse to the directory using the button to the right.<br>"+
                                             "Enter the full directory path or:"+"<br>"+
                                             "-leave blank to use the science image directory."+"<br>"+
                                             "-use ."+slash+" to define a sub-directory relative to the science directory."+"<br>"+
                                             "-use .."+slash+" to define a parent directory relative to the science directory."+"</html>");
        biasRawDirField.getDocument().addDocumentListener(new thisDocumentListener());
        FileDrop fileDropbiasRawDirField = new FileDrop(biasRawDirField, BorderFactory.createEmptyBorder(0,10,10,10),new FileDrop.Listener()
            {   public void filesDropped( java.io.File[] files )
                {
                openDragAndDropFileNames(files, biasRawDirField, biasBaseField, autoWildcard);
                }
            });         
		rawBiasPanel.add (biasRawDirField);




		biasBaseDirButton = new JButton (folderOpenIcon);
        biasBaseDirButton.setOpaque(false);
        biasBaseDirButton.setFocusPainted(false);
        biasBaseDirButton.setBorderPainted(false);
        biasBaseDirButton.setContentAreaFilled(false);
        biasBaseDirButton.setBorder(BorderFactory.createEmptyBorder(0,0,0,folderIconRightMargin));
        biasBaseDirButton.setToolTipText("<html>Click to define the folder containing raw bias images<br>"+
                                 "or click file opener to right and select a file in the desired folder</html>");
//        biasBaseDirButton.setEnabled(!rawCalCommonDir);
		biasBaseDirButton.addActionListener (this);
        biasBaseDirButton.setPreferredSize(folderIconSize);
		rawBiasPanel.add (biasBaseDirButton);


		biasBaseField = new JTextField (biasBase);
		biasBaseField.setPreferredSize (fileBoxSize);
        biasBaseField.setFont(p12);
        biasBaseField.setHorizontalAlignment (JTextField.LEFT);
        biasBaseField.setToolTipText("<html>Enter a raw bias filename pattern using ? and * as wildcard characters<br>"+
                                     "or use the file browser icon to the right to select a representative file</html>");
        biasBaseField.getDocument().addDocumentListener(new thisDocumentListener());
        FileDrop fileDropbiasBaseField = new FileDrop(biasBaseField, BorderFactory.createEmptyBorder(0,10,10,10),new FileDrop.Listener()
            {   public void filesDropped( java.io.File[] files )
                {
                openDragAndDropFileNames(files, biasRawDirField, biasBaseField, autoWildcard);
                }
            });        
		rawBiasPanel.add (biasBaseField);

		biasBaseButton = new JButton (fileOpenIcon);
        biasBaseButton.setPreferredSize(folderIconSize);
        biasBaseButton.setOpaque(false);
        biasBaseButton.setFocusPainted(false);
        biasBaseButton.setBorderPainted(false);
        biasBaseButton.setContentAreaFilled(false);
        biasBaseButton.setBorder(BorderFactory.createEmptyBorder(0,0,0,folderIconRightMargin));
        biasBaseButton.setToolTipText("<html>Click to select a representative raw bias file. If the preferences option: \"Insert wildcard between<br>"+
                                    "last '_' and '.'\" is selected, a pattern will be derived from the selected file. Otherwise, hand modify the selected<br>"+
                                    "filename using ? and * wildcard characters. If a file is not currently available,<br>"+
                                    "use the folder opener to the left to select the folder and type the filename pattern directly.</html>");
		biasBaseButton.addActionListener (this);
		rawBiasPanel.add (biasBaseButton);

        validBiasFilesLabel = new JLabel ("");
        validBiasFilesLabel.setToolTipText("The number of files in the bias directory matching the bias filename pattern");
        validBiasFilesLabel.setHorizontalAlignment(JLabel.RIGHT);
        validBiasFilesLabel.setFont(b12);
		validBiasFilesLabel.setPreferredSize (validFilesSize);
        rawBiasPanel.add (validBiasFilesLabel);

        SpringUtil.makeCompactGrid (rawBiasPanel, 1, rawBiasPanel.getComponentCount(), 0,0,2,0);
        biasPanel.add (rawBiasPanel);

        JPanel masterBiasPanel = new JPanel(new SpringLayout());

		useBiasBox = new JCheckBox("Enable",useBias);
        useBiasBox.setFont(p12);
        useBiasBox.setPreferredSize (checkBoxSize);
        useBiasBox.setToolTipText("<html>Enable bias subtraction using the master bias file specified to the right<br>"+
                                  "(applies to master dark builds, master flat builds and science data reduction).</html>");
		useBiasBox.addItemListener (this);
		masterBiasPanel.add (useBiasBox);

        JPanel masterBiasOptionPanel = new JPanel(new SpringLayout());
        masterBiasOptionPanel.setPreferredSize(radioSize);

        SpringUtil.makeCompactGrid (masterBiasOptionPanel, 1, masterBiasOptionPanel.getComponentCount(), 0,0,0,0);
        masterBiasPanel.add(masterBiasOptionPanel);


		biasMasterDirField = new JTextField (biasMasterDirText);
        biasMasterDirField.setFont(p12);
//        biasMasterDirField.setEnabled(!masterCalCommonDir);
//        biasMasterDirField.setBackground(!masterCalCommonDir?Color.white:gray);
        biasMasterDirField.setPreferredSize(directoryBoxSize);
        biasMasterDirField.setHorizontalAlignment (JTextField.LEFT);
        biasMasterDirField.setToolTipText("<html>"+"Enter the directory name for the master bias image<br>"+
                                             "or browse to the directory using the button to the right.<br>"+
                                             "Enter the full directory path or:"+"<br>"+
                                             "-leave blank to use the science image directory."+"<br>"+
                                             "-use ."+slash+" to define a sub-directory relative to the science directory."+"<br>"+
                                             "-use .."+slash+" to define a parent directory relative to the science directory."+"</html>");
        biasMasterDirField.getDocument().addDocumentListener(new thisDocumentListener());
        FileDrop fileDropbiasMasterDirField = new FileDrop(biasMasterDirField, BorderFactory.createEmptyBorder(0,10,10,10),new FileDrop.Listener()
            {   public void filesDropped( java.io.File[] files )
                {
                openDragAndDropFileNames(files, biasMasterDirField, biasMasterField, false);
                }
            });        
		masterBiasPanel.add (biasMasterDirField);

		biasMasterDirButton = new JButton (folderOpenIcon);
        biasMasterDirButton.setOpaque(false);
        biasMasterDirButton.setFocusPainted(false);
        biasMasterDirButton.setBorderPainted(false);
        biasMasterDirButton.setContentAreaFilled(false);
        biasMasterDirButton.setBorder(BorderFactory.createEmptyBorder(0,0,0,folderIconRightMargin));
        biasMasterDirButton.setToolTipText("<html>Click to define the directory containing the master bias file<br>"+
                                 "or click file opener to right and select a file in the desired folder</html>");
//        biasMasterDirButton.setEnabled(!masterCalCommonDir);
		biasMasterDirButton.addActionListener (this);
        biasMasterDirButton.setPreferredSize(folderIconSize);
		masterBiasPanel.add (biasMasterDirButton);

		biasMasterField = new JTextField (biasMaster);
		biasMasterField.setPreferredSize (fileBoxSize);
        biasMasterField.setFont(p12);
        biasMasterField.setToolTipText("Enter the master bias filename or use the file browser icon to the right to select a file");
		biasMasterField.setHorizontalAlignment (JTextField.LEFT);
        biasMasterField.getDocument().addDocumentListener(new thisDocumentListener());
        FileDrop fileDropbiasMasterField = new FileDrop(biasMasterField, BorderFactory.createEmptyBorder(0,10,10,10),new FileDrop.Listener()
            {   public void filesDropped( java.io.File[] files )
                {
                openDragAndDropFileNames(files, biasMasterDirField, biasMasterField, false);
                }
            });        
		masterBiasPanel.add (biasMasterField);

		biasMasterButton = new JButton (fileOpenIcon);
        biasMasterButton.setPreferredSize(folderIconSize);
        biasMasterButton.setOpaque(false);
        biasMasterButton.setFocusPainted(false);
        biasMasterButton.setBorderPainted(false);
        biasMasterButton.setContentAreaFilled(false);
        biasMasterButton.setBorder(BorderFactory.createEmptyBorder(0,0,0,folderIconRightMargin));
        biasMasterButton.setToolTipText("<html>Click to select a master bias file.</html>");
		biasMasterButton.addActionListener (this);
		masterBiasPanel.add (biasMasterButton);

        validMasterBiasFilesLabel = new JLabel ("");
        validMasterBiasFilesLabel.setToolTipText("The number of master bias files in the specified directory matching the filename (0 or 1)");
        validMasterBiasFilesLabel.setHorizontalAlignment(JLabel.RIGHT);
        validMasterBiasFilesLabel.setFont(b12);
		validMasterBiasFilesLabel.setPreferredSize (validFilesSize);
        masterBiasPanel.add (validMasterBiasFilesLabel);

        SpringUtil.makeCompactGrid (masterBiasPanel, 1, masterBiasPanel.getComponentCount(), 0,0,2,0);
        biasPanel.add (masterBiasPanel);
       
        SpringUtil.makeCompactGrid (biasPanel, biasPanel.getComponentCount(),1, 9,0,7,2);
        mainPanel.add (biasPanel);





        JPanel darkPanel = new JPanel(new SpringLayout());
        darkPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(mainBorderColor, 1), "Dark Subtraction", TitledBorder.LEFT, TitledBorder.TOP, b12, Color.DARK_GRAY));

        JPanel rawDarkPanel = new JPanel(new SpringLayout());

        createDarkBox = new JCheckBox("Build",createDark);
        createDarkBox.setFont(p12);
        createDarkBox.setPreferredSize (checkBoxSize);
        createDarkBox.setToolTipText("Build a new master dark file from the raw dark files specified to the right");
		createDarkBox.addItemListener (this);
        rawDarkPanel.add(createDarkBox);

        JPanel darkOptionPanel = new JPanel(new SpringLayout());
        darkOptionPanel.setPreferredSize(radioSize);

        darkRadioGroup = new ButtonGroup ();

        darkAverageRadio = new JRadioButton("ave");
        darkAverageRadio.setFont(p12);
        if (!darkMedian) darkAverageRadio.setSelected(true);
        else darkAverageRadio.setSelected(false);
        darkAverageRadio.setToolTipText("average raw files to build master dark");
        darkOptionPanel.add (darkAverageRadio);
        darkMedianRadio = new JRadioButton("med");
        darkMedianRadio.setFont(p12);
        if (darkMedian) darkMedianRadio.setSelected(true);
        else darkMedianRadio.setSelected(false);
        darkMedianRadio.setToolTipText("median raw files to build master dark");
        darkOptionPanel.add (darkMedianRadio);
        darkRadioGroup.add(darkAverageRadio);
        darkRadioGroup.add(darkMedianRadio);
        darkAverageRadio.addActionListener(this);
        darkMedianRadio.addActionListener(this);

        SpringUtil.makeCompactGrid (darkOptionPanel, 1, darkOptionPanel.getComponentCount(), 0,0,0,0);
        rawDarkPanel.add(darkOptionPanel);

		darkRawDirField = new JTextField (darkRawDirText);
        darkRawDirField.setFont(p12);
        darkRawDirField.setMargin(textboxMargin);
//        darkRawDirField.setEnabled(!rawCalCommonDir);
//        darkRawDirField.setBackground(!rawCalCommonDir?Color.white:gray);
        darkRawDirField.setPreferredSize(directoryBoxSize);
        darkRawDirField.setHorizontalAlignment (JTextField.LEFT);
        darkRawDirField.setToolTipText("<html>"+"Enter the directory name where raw dark images are located<br>"+
                                             "or browse to the directory using the button to the right.<br>"+
                                             "Enter the full directory path or:"+"<br>"+
                                             "-leave blank to use the science image directory."+"<br>"+
                                             "-use ."+slash+" to define a sub-directory relative to the science directory."+"<br>"+
                                             "-use .."+slash+" to define a parent directory relative to the science directory."+"</html>");
        darkRawDirField.getDocument().addDocumentListener(new thisDocumentListener());
        FileDrop fileDropdarkRawDirField = new FileDrop(darkRawDirField, BorderFactory.createEmptyBorder(0,10,10,10),new FileDrop.Listener()
            {   public void filesDropped( java.io.File[] files )
                {
                openDragAndDropFileNames(files, darkRawDirField, darkBaseField, autoWildcard);
                }
            });         
		rawDarkPanel.add (darkRawDirField);




		darkBaseDirButton = new JButton (folderOpenIcon);
        darkBaseDirButton.setOpaque(false);
        darkBaseDirButton.setFocusPainted(false);
        darkBaseDirButton.setBorderPainted(false);
        darkBaseDirButton.setContentAreaFilled(false);
        darkBaseDirButton.setBorder(BorderFactory.createEmptyBorder(0,0,0,folderIconRightMargin));
        darkBaseDirButton.setToolTipText("<html>Click to define the folder containing raw dark images<br>"+
                                 "or click file opener to right and select a file in the desired folder</html>");
//        darkBaseDirButton.setEnabled(!rawCalCommonDir);
		darkBaseDirButton.addActionListener (this);
        darkBaseDirButton.setPreferredSize(folderIconSize);
		rawDarkPanel.add (darkBaseDirButton);


		darkBaseField = new JTextField (darkBase);
		darkBaseField.setPreferredSize (fileBoxSize);
        darkBaseField.setFont(p12);
        darkBaseField.setHorizontalAlignment (JTextField.LEFT);
        darkBaseField.setToolTipText("<html>Enter a raw dark filename pattern using ? and * as wildcard characters<br>"+
                                     "or use the file browser icon to the right to select a representative file</html>");
        darkBaseField.getDocument().addDocumentListener(new thisDocumentListener());
        FileDrop fileDropdarkBaseField = new FileDrop(darkBaseField, BorderFactory.createEmptyBorder(0,10,10,10),new FileDrop.Listener()
            {   public void filesDropped( java.io.File[] files )
                {
                openDragAndDropFileNames(files, darkRawDirField, darkBaseField, autoWildcard);
                }
            });        
		rawDarkPanel.add (darkBaseField);

		darkBaseButton = new JButton (fileOpenIcon);
        darkBaseButton.setPreferredSize(folderIconSize);
        darkBaseButton.setOpaque(false);
        darkBaseButton.setFocusPainted(false);
        darkBaseButton.setBorderPainted(false);
        darkBaseButton.setContentAreaFilled(false);
        darkBaseButton.setBorder(BorderFactory.createEmptyBorder(0,0,0,folderIconRightMargin));
        darkBaseButton.setToolTipText("<html>Click to select a representative raw dark file. If the preferences option: \"Insert wildcard between<br>"+
                                    "last '_' and '.'\" is selected, a pattern will be derived from the selected file. Otherwise, hand modify the selected<br>"+
                                    "filename using ? and * wildcard characters. If a file is not currently available,<br>"+
                                    "use the folder opener to the left to select the folder and type the filename pattern directly.</html>");
		darkBaseButton.addActionListener (this);
		rawDarkPanel.add (darkBaseButton);

        validDarkFilesLabel = new JLabel ("");
        validDarkFilesLabel.setToolTipText("The number of files in the dark directory matching the dark filename pattern");
        validDarkFilesLabel.setHorizontalAlignment(JLabel.RIGHT);
        validDarkFilesLabel.setFont(b12);
		validDarkFilesLabel.setPreferredSize (validFilesSize);
        rawDarkPanel.add (validDarkFilesLabel);

        SpringUtil.makeCompactGrid (rawDarkPanel, 1, rawDarkPanel.getComponentCount(), 0,0,2,0);
        darkPanel.add (rawDarkPanel);

        JPanel masterDarkPanel = new JPanel(new SpringLayout());

		useDarkBox = new JCheckBox("Enable",useDark);
        useDarkBox.setFont(p12);
        useDarkBox.setPreferredSize (checkBoxSize);
        useDarkBox.setToolTipText("<html>Enable dark subtraction using the master dark file specified to the right.<br>"+
                                  "(applies to master flat builds and science data reduction)</html>");
		useDarkBox.addItemListener (this);
		masterDarkPanel.add (useDarkBox);

        JPanel masterDarkOptionPanel = new JPanel(new SpringLayout());
        masterDarkOptionPanel.setPreferredSize(radioSize);

//        JLabel headerLabel = new JLabel ("Dark ExpTime Scaling:");
//        headerLabel.setFont(p12);
//        headerLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
//        headerLabel.setHorizontalAlignment (JTextField.RIGHT);
//		masterDarkOptionPanel.add (headerLabel);


		scaleExpTimeBox = new JCheckBox("scale",scaleExpTime);
        scaleExpTimeBox.setEnabled(useBias);
        scaleExpTimeBox.setFont(p12);
        scaleExpTimeBox.setToolTipText("<html>"+"Scale the master dark pixel values by the"+"<br>"+
                            "ratio of the science image to master dark exposure times."+"<br>"+
                            "Enable both master Bias Subtraction and Dark Subtraction to use scale."+"</html>");
		scaleExpTimeBox.addItemListener (this);
		masterDarkOptionPanel.add (scaleExpTimeBox);

		deBiasMasterDarkBox = new JCheckBox("deBias",deBiasMasterDark);
        deBiasMasterDarkBox.setEnabled(useBias);
        deBiasMasterDarkBox.setFont(p11);
        deBiasMasterDarkBox.setToolTipText("<html>"+
                            "When selected, master darks will be bias subtracted during the build process if 'Bias Subtraction' is enabled.<br>"+
                            "In this mode, master darks created outside AIJ should be bias subtracted."+"<br>"+"<br>"+
                            "When deselected, master darks will not be bias subtracted during the build process, even if 'Bias Subtraction' is enabled.<br>"+
                            "When 'Bias Subtraction' is enabled in this mode, the master dark will be debiased prior to processing the science images.<br>"+
                            "In this mode, master darks created outside AIJ should NOT be bias subtracted."+"<br>"+"<br>"+
                            "Enable Bias Subtraction plus Build master dark and/or Enable Dark Subtraction to use the deBias feature.</html>");
		deBiasMasterDarkBox.addItemListener (this);
		masterDarkOptionPanel.add (deBiasMasterDarkBox);        

        SpringUtil.makeCompactGrid (masterDarkOptionPanel, 1, masterDarkOptionPanel.getComponentCount(), 0,0,0,0);
        masterDarkPanel.add(masterDarkOptionPanel);

		darkMasterDirField = new JTextField (darkMasterDirText);
        darkMasterDirField.setFont(p12);
//        darkMasterDirField.setEnabled(!masterCalCommonDir);
//        darkMasterDirField.setBackground(!masterCalCommonDir?Color.white:gray);
        darkMasterDirField.setPreferredSize(directoryBoxSize);
        darkMasterDirField.setHorizontalAlignment (JTextField.LEFT);
        darkMasterDirField.setToolTipText("<html>"+"Enter the directory name for the master dark image<br>"+
                                             "or browse to the directory using the button to the right.<br>"+
                                             "Enter the full directory path or:"+"<br>"+
                                             "-leave blank to use the science image directory."+"<br>"+
                                             "-use ."+slash+" to define a sub-directory relative to the science directory."+"<br>"+
                                             "-use .."+slash+" to define a parent directory relative to the science directory."+"</html>");
        darkMasterDirField.getDocument().addDocumentListener(new thisDocumentListener());
        FileDrop fileDropdarkMasterDirField = new FileDrop(darkMasterDirField, BorderFactory.createEmptyBorder(0,10,10,10),new FileDrop.Listener()
            {   public void filesDropped( java.io.File[] files )
                {
                openDragAndDropFileNames(files, darkMasterDirField, darkMasterField, false);
                }
            });        
		masterDarkPanel.add (darkMasterDirField);

		darkMasterDirButton = new JButton (folderOpenIcon);
        darkMasterDirButton.setOpaque(false);
        darkMasterDirButton.setFocusPainted(false);
        darkMasterDirButton.setBorderPainted(false);
        darkMasterDirButton.setContentAreaFilled(false);
        darkMasterDirButton.setBorder(BorderFactory.createEmptyBorder(0,0,0,folderIconRightMargin));
        darkMasterDirButton.setToolTipText("<html>Click to define the directory containing the master dark file<br>"+
                                 "or click file opener to right and select a file in the desired folder</html>");
//        darkMasterDirButton.setEnabled(!masterCalCommonDir);
		darkMasterDirButton.addActionListener (this);
        darkMasterDirButton.setPreferredSize(folderIconSize);
		masterDarkPanel.add (darkMasterDirButton);

		darkMasterField = new JTextField (darkMaster);
		darkMasterField.setPreferredSize (fileBoxSize);
        darkMasterField.setFont(p12);
        darkMasterField.setToolTipText("Enter the master dark filename or use the file browser icon to the right to select a file");
		darkMasterField.setHorizontalAlignment (JTextField.LEFT);
        darkMasterField.getDocument().addDocumentListener(new thisDocumentListener());
        FileDrop fileDropdarkMasterField = new FileDrop(darkMasterField, BorderFactory.createEmptyBorder(0,10,10,10),new FileDrop.Listener()
            {   public void filesDropped( java.io.File[] files )
                {
                openDragAndDropFileNames(files, darkMasterDirField, darkMasterField, false);
                }
            });         
		masterDarkPanel.add (darkMasterField);

		darkMasterButton = new JButton (fileOpenIcon);
        darkMasterButton.setPreferredSize(folderIconSize);
        darkMasterButton.setOpaque(false);
        darkMasterButton.setFocusPainted(false);
        darkMasterButton.setBorderPainted(false);
        darkMasterButton.setContentAreaFilled(false);
        darkMasterButton.setBorder(BorderFactory.createEmptyBorder(0,0,0,folderIconRightMargin));
        darkMasterButton.setToolTipText("<html>Click to select a master dark file.</html>");
		darkMasterButton.addActionListener (this);
		masterDarkPanel.add (darkMasterButton);

        validMasterDarkFilesLabel = new JLabel ("");
        validMasterDarkFilesLabel.setToolTipText("The number of master dark files in the specified directory matching the filename (0 or 1)");
        validMasterDarkFilesLabel.setHorizontalAlignment(JLabel.RIGHT);
        validMasterDarkFilesLabel.setFont(b12);
		validMasterDarkFilesLabel.setPreferredSize (validFilesSize);
        masterDarkPanel.add (validMasterDarkFilesLabel);

        SpringUtil.makeCompactGrid (masterDarkPanel, 1, masterDarkPanel.getComponentCount(), 0,0,2,0);
        darkPanel.add (masterDarkPanel);

        SpringUtil.makeCompactGrid (darkPanel, darkPanel.getComponentCount(),1, 9,0,7,2);
        mainPanel.add (darkPanel);



        JPanel flatPanel = new JPanel(new SpringLayout());
        flatPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(mainBorderColor, 1), "Flat Division", TitledBorder.LEFT, TitledBorder.TOP, b12, Color.DARK_GRAY));

        JPanel rawFlatPanel = new JPanel(new SpringLayout());

        createFlatBox = new JCheckBox("Build",createFlat);
        createFlatBox.setFont(p12);
        createFlatBox.setPreferredSize (checkBoxSize);
        createFlatBox.setToolTipText("Build a new master flat file from the raw flat files specified to the right");
		createFlatBox.addItemListener (this);
        rawFlatPanel.add(createFlatBox);

        JPanel flatRadioPanelPanel = new JPanel(new SpringLayout());
        flatRadioPanelPanel.setPreferredSize(radioSize);

        flatRadioGroup = new ButtonGroup ();

        flatAverageRadio = new JRadioButton("ave");
        flatAverageRadio.setFont(p12);
        if (!flatMedian) flatAverageRadio.setSelected(true);
        else flatAverageRadio.setSelected(false);
        flatAverageRadio.setToolTipText("average raw files to build master flat");
        flatRadioPanelPanel.add (flatAverageRadio);
        flatMedianRadio = new JRadioButton("med");
        flatMedianRadio.setFont(p12);
        if (flatMedian) flatMedianRadio.setSelected(true);
        else flatMedianRadio.setSelected(false);
        flatMedianRadio.setToolTipText("median raw files to build master flat");
        flatRadioPanelPanel.add (flatMedianRadio);
        flatRadioGroup.add(flatAverageRadio);
        flatRadioGroup.add(flatMedianRadio);
        flatAverageRadio.addActionListener(this);
        flatMedianRadio.addActionListener(this);

        SpringUtil.makeCompactGrid (flatRadioPanelPanel, 1, flatRadioPanelPanel.getComponentCount(), 0,0,0,0);
        rawFlatPanel.add(flatRadioPanelPanel);

		flatRawDirField = new JTextField (flatRawDirText);
        flatRawDirField.setFont(p12);
        flatRawDirField.setMargin(textboxMargin);
//        flatRawDirField.setEnabled(!rawCalCommonDir);
//        flatRawDirField.setBackground(!rawCalCommonDir?Color.white:gray);
        flatRawDirField.setPreferredSize(directoryBoxSize);
        flatRawDirField.setHorizontalAlignment (JTextField.LEFT);
        flatRawDirField.setToolTipText("<html>"+"Enter the directory name where raw flat images are located<br>"+
                                             "or browse to the directory using the button to the right.<br>"+
                                             "Enter the full directory path or:"+"<br>"+
                                             "-leave blank to use the science image directory."+"<br>"+
                                             "-use ."+slash+" to define a sub-directory relative to the science directory."+"<br>"+
                                             "-use .."+slash+" to define a parent directory relative to the science directory."+"</html>");
        flatRawDirField.getDocument().addDocumentListener(new thisDocumentListener());
        FileDrop fileDropflatRawDirField = new FileDrop(flatRawDirField, BorderFactory.createEmptyBorder(0,10,10,10),new FileDrop.Listener()
            {   public void filesDropped( java.io.File[] files )
                {
                openDragAndDropFileNames(files, flatRawDirField, flatBaseField, autoWildcard);
                }
            });         
		rawFlatPanel.add (flatRawDirField);



		flatBaseDirButton = new JButton (folderOpenIcon);
        flatBaseDirButton.setOpaque(false);
        flatBaseDirButton.setFocusPainted(false);
        flatBaseDirButton.setBorderPainted(false);
        flatBaseDirButton.setContentAreaFilled(false);
        flatBaseDirButton.setBorder(BorderFactory.createEmptyBorder(0,0,0,folderIconRightMargin));
        flatBaseDirButton.setToolTipText("<html>Click to define the folder containing raw flat images<br>"+
                                 "or click file opener to right and select a file in the desired folder</html>");
//        flatBaseDirButton.setEnabled(!rawCalCommonDir);
		flatBaseDirButton.addActionListener (this);
        flatBaseDirButton.setPreferredSize(folderIconSize);
		rawFlatPanel.add (flatBaseDirButton);


		flatBaseField = new JTextField (flatBase);
		flatBaseField.setPreferredSize (fileBoxSize);
        flatBaseField.setFont(p12);
        flatBaseField.setHorizontalAlignment (JTextField.LEFT);
        flatBaseField.setToolTipText("<html>Enter a raw flat filename pattern using ? and * as wildcard characters<br>"+
                                     "or use the file browser icon to the right to select a representative file</html>");
        flatBaseField.getDocument().addDocumentListener(new thisDocumentListener());
        FileDrop fileDropflatBaseField = new FileDrop(flatBaseField, BorderFactory.createEmptyBorder(0,10,10,10),new FileDrop.Listener()
            {   public void filesDropped( java.io.File[] files )
                {
                openDragAndDropFileNames(files, flatRawDirField, flatBaseField, autoWildcard);
                }
            });         
		rawFlatPanel.add (flatBaseField);

		flatBaseButton = new JButton (fileOpenIcon);
        flatBaseButton.setPreferredSize(folderIconSize);
        flatBaseButton.setOpaque(false);
        flatBaseButton.setFocusPainted(false);
        flatBaseButton.setBorderPainted(false);
        flatBaseButton.setContentAreaFilled(false);
        flatBaseButton.setBorder(BorderFactory.createEmptyBorder(0,0,0,folderIconRightMargin));
        flatBaseButton.setToolTipText("<html>Click to select a representative raw flat file. If the preferences option: \"Insert wildcard between<br>"+
                                    "last '_' and '.'\" is selected, a pattern will be derived from the selected file. Otherwise, hand modify the selected<br>"+
                                    "filename using ? and * wildcard characters. If a file is not currently available,<br>"+
                                    "use the folder opener to the left to select the folder and type the filename pattern directly.</html>");
		flatBaseButton.addActionListener (this);
		rawFlatPanel.add (flatBaseButton);

        validFlatFilesLabel = new JLabel ("");
        validFlatFilesLabel.setToolTipText("The number of files in the flat directory matching the flat filename pattern");
        validFlatFilesLabel.setHorizontalAlignment(JLabel.RIGHT);
        validFlatFilesLabel.setFont(b12);
		validFlatFilesLabel.setPreferredSize (validFilesSize);
        rawFlatPanel.add (validFlatFilesLabel);

        SpringUtil.makeCompactGrid (rawFlatPanel, 1, rawFlatPanel.getComponentCount(), 0,0,2,0);
        flatPanel.add (rawFlatPanel);

        JPanel masterFlatPanel = new JPanel(new SpringLayout());

		useFlatBox = new JCheckBox("Enable",useFlat);
        useFlatBox.setFont(p12);
        useFlatBox.setPreferredSize (checkBoxSize);
        useFlatBox.setToolTipText("<html>Enable flat field division using the master flat file specified to the right.</html>");
		useFlatBox.addItemListener (this);
		masterFlatPanel.add (useFlatBox);

        JPanel masterFlatOptionPanel = new JPanel(new SpringLayout());
        masterFlatOptionPanel.setPreferredSize(radioSize);

        gradientRemovalCB = new JCheckBox("Remove Gradient", useGradientRemoval);
        gradientRemovalCB.setFont(p11);
        gradientRemovalCB.setToolTipText("<html>Remove gradient from calibrated raw flats when Building a master flat.<br>"+
                                         "Flat Division 'Build' must be selected to use this option.</html>");
        gradientRemovalCB.addItemListener(this);
        masterFlatOptionPanel.add(gradientRemovalCB);        

        SpringUtil.makeCompactGrid (masterFlatOptionPanel, 1, masterFlatOptionPanel.getComponentCount(), 0,0,0,0);
        masterFlatPanel.add(masterFlatOptionPanel);

		flatMasterDirField = new JTextField (flatMasterDirText);
        flatMasterDirField.setFont(p12);
//        flatMasterDirField.setEnabled(!masterCalCommonDir);
//        flatMasterDirField.setBackground(!masterCalCommonDir?Color.white:gray);
        flatMasterDirField.setPreferredSize(directoryBoxSize);
        flatMasterDirField.setHorizontalAlignment (JTextField.LEFT);
        flatMasterDirField.setToolTipText("<html>"+"Enter the directory name for the master flat image<br>"+
                                             "or browse to the directory using the button to the right.<br>"+
                                             "Enter the full directory path or:"+"<br>"+
                                             "-leave blank to use the science image directory."+"<br>"+
                                             "-use ."+slash+" to define a sub-directory relative to the science directory."+"<br>"+
                                             "-use .."+slash+" to define a parent directory relative to the science directory."+"</html>");
        flatMasterDirField.getDocument().addDocumentListener(new thisDocumentListener());
        FileDrop fileDropflatMasterDirField = new FileDrop(flatMasterDirField, BorderFactory.createEmptyBorder(0,10,10,10),new FileDrop.Listener()
            {   public void filesDropped( java.io.File[] files )
                {
                openDragAndDropFileNames(files, flatMasterDirField, flatMasterField, false);
                }
            });         
		masterFlatPanel.add (flatMasterDirField);

		flatMasterDirButton = new JButton (folderOpenIcon);
        flatMasterDirButton.setOpaque(false);
        flatMasterDirButton.setFocusPainted(false);
        flatMasterDirButton.setBorderPainted(false);
        flatMasterDirButton.setContentAreaFilled(false);
        flatMasterDirButton.setBorder(BorderFactory.createEmptyBorder(0,0,0,folderIconRightMargin));
        flatMasterDirButton.setToolTipText("<html>Click to define the directory containing the master flat file<br>"+
                                 "or click file opener to right and select a file in the desired folder</html>");
//        flatMasterDirButton.setEnabled(!masterCalCommonDir);
		flatMasterDirButton.addActionListener (this);
        flatMasterDirButton.setPreferredSize(folderIconSize);
		masterFlatPanel.add (flatMasterDirButton);

		flatMasterField = new JTextField (flatMaster);
		flatMasterField.setPreferredSize (fileBoxSize);
        flatMasterField.setFont(p12);
        flatMasterField.setToolTipText("Enter the master flat filename or use the file browser icon to the right to select a file");
		flatMasterField.setHorizontalAlignment (JTextField.LEFT);
        flatMasterField.getDocument().addDocumentListener(new thisDocumentListener());
        FileDrop fileDropflatMasterField = new FileDrop(flatMasterField, BorderFactory.createEmptyBorder(0,10,10,10),new FileDrop.Listener()
            {   public void filesDropped( java.io.File[] files )
                {
                openDragAndDropFileNames(files, flatMasterDirField, flatMasterField, false);
                }
            });        
		masterFlatPanel.add (flatMasterField);

		flatMasterButton = new JButton (fileOpenIcon);
        flatMasterButton.setPreferredSize(folderIconSize);
        flatMasterButton.setOpaque(false);
        flatMasterButton.setFocusPainted(false);
        flatMasterButton.setBorderPainted(false);
        flatMasterButton.setContentAreaFilled(false);
        flatMasterButton.setBorder(BorderFactory.createEmptyBorder(0,0,0,folderIconRightMargin));
        flatMasterButton.setToolTipText("<html>Click to select a master flat file.</html>");
		flatMasterButton.addActionListener (this);
		masterFlatPanel.add (flatMasterButton);

        validMasterFlatFilesLabel = new JLabel ("");
        validMasterFlatFilesLabel.setToolTipText("The number of master flat files in the specified directory matching the filename (0 or 1)");
        validMasterFlatFilesLabel.setHorizontalAlignment(JLabel.RIGHT);
        validMasterFlatFilesLabel.setFont(b12);
		validMasterFlatFilesLabel.setPreferredSize (validFilesSize);
        masterFlatPanel.add (validMasterFlatFilesLabel);

        SpringUtil.makeCompactGrid (masterFlatPanel, 1, masterFlatPanel.getComponentCount(), 0,0,2,0);
        flatPanel.add (masterFlatPanel);

        SpringUtil.makeCompactGrid (flatPanel, flatPanel.getComponentCount(),1, 9,0,7,2);
        mainPanel.add (flatPanel);


        JPanel linearityPanel = new JPanel(new SpringLayout());
        linearityPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(mainBorderColor, 1), "Image Correction", TitledBorder.LEFT, TitledBorder.TOP, b12, Color.DARK_GRAY));

        JPanel linearityLine1Panel = new JPanel(new SpringLayout());

		useNLCBox = new JCheckBox("Enable Linearity Correction",useNLC);
        useNLCBox.setFont(p12);
        useNLCBox.setPreferredSize (checkboxPlusRadioSize);
        useNLCBox.setToolTipText("<html>"+"Enable correction of CCD non-linearity on a per pixel basis"+"<br>"+
                                    "using the formula and four values to the right."+"<br>"+
                                    "To use NLC, Bias Subtraction must be Enabled if Dark Subtraction and/or Flat Division is Enabled."+"</html>");
		useNLCBox.addItemListener (this);
		linearityLine1Panel.add (useNLCBox);

//        JPanel NLCOptionPanel = new JPanel(new SpringLayout());
//        NLCOptionPanel.setPreferredSize(radioSize);
//
//        SpringUtil.makeCompactGrid (NLCOptionPanel, 1, NLCOptionPanel.getComponentCount(), 0,0,0,0);
//        linearityLine1Panel.add(NLCOptionPanel);

        NLCLabel = new JLabel ("New pixel value = ");
        NLCLabel.setFont(b12);
        NLCLabel.setHorizontalAlignment (JTextField.LEFT);
		linearityLine1Panel.add (NLCLabel);


        coeffAModel = new SpinnerNumberModel(coeffA, null, null, 0.0);
        coeffASpinner = new JSpinner(coeffAModel);
        JSpinner.NumberEditor coeffANumberEditor = new JSpinner.NumberEditor( coeffASpinner, "0.0#######E0" );
        coeffASpinner.setEditor(coeffANumberEditor);
        coeffASpinner.setPreferredSize (coeffBoxSize);
        coeffASpinner.setFont(p12);
        coeffASpinner.addChangeListener (this);
//        coeffASpinner.addMouseWheelListener(this);
        linearityLine1Panel.add(coeffASpinner);

        coeffBLabel = new JLabel(" + ");
        coeffBLabel.setHorizontalAlignment (JLabel.LEFT);
        coeffBLabel.setFont(b12);
        linearityLine1Panel.add(coeffBLabel);

        coeffBModel = new SpinnerNumberModel(coeffB, null, null, 0.0);
        coeffBSpinner = new JSpinner(coeffBModel);
        coeffBSpinner.setFont(p12);
        JSpinner.NumberEditor coeffBNumberEditor = new JSpinner.NumberEditor( coeffBSpinner, "0.0#######E0" );
        coeffBSpinner.setEditor(coeffBNumberEditor);
        coeffBSpinner.setPreferredSize (coeffBoxSize);
        coeffBSpinner.addChangeListener (this);
//        coeffBSpinner.addMouseWheelListener(this);
        linearityLine1Panel.add(coeffBSpinner);

        coeffCLabel = new JLabel(" \u00d7 (PixVal) + ");
        coeffCLabel.setFont(b12);
        coeffCLabel.setHorizontalAlignment (JLabel.RIGHT);
        linearityLine1Panel.add(coeffCLabel);

        coeffCModel = new SpinnerNumberModel(coeffC, null, null, 0.0);
        coeffCSpinner = new JSpinner(coeffCModel);
        coeffCSpinner.setFont(p12);
        JSpinner.NumberEditor coeffCNumberEditor = new JSpinner.NumberEditor( coeffCSpinner, "0.0#######E0" );
        coeffCSpinner.setEditor(coeffCNumberEditor);
        coeffCSpinner.setPreferredSize (coeffBoxSize);
        coeffCSpinner.addChangeListener (this);
//        coeffCSpinner.addMouseWheelListener(this);
        linearityLine1Panel.add(coeffCSpinner);
        
        coeffDLabel = new JLabel(" \u00d7 (PixVal)\u00b2 + ");
        coeffDLabel.setFont(b12);
        coeffDLabel.setHorizontalAlignment (JLabel.RIGHT);
        linearityLine1Panel.add(coeffDLabel);

        coeffDModel = new SpinnerNumberModel(coeffD, null, null, 0.0);
        coeffDSpinner = new JSpinner(coeffDModel);
        coeffDSpinner.setFont(p12);
        JSpinner.NumberEditor coeffDNumberEditor = new JSpinner.NumberEditor( coeffDSpinner, "0.0#######E0" );
        coeffDSpinner.setEditor(coeffDNumberEditor);
        coeffDSpinner.setPreferredSize (coeffBoxSize);
        coeffDSpinner.addChangeListener (this);
//        coeffCSpinner.addMouseWheelListener(this);
        linearityLine1Panel.add(coeffDSpinner);        

        NLCFinalLabel = new JLabel(" \u00d7 (PixVal)\u00b3 ");
        NLCFinalLabel.setHorizontalAlignment (JLabel.CENTER);
        NLCFinalLabel.setFont(b12);
        linearityLine1Panel.add(NLCFinalLabel);

        JPanel NLCNumPanel = new JPanel(new SpringLayout());
        NLCNumPanel.setPreferredSize(validFilesSize);
        SpringUtil.makeCompactGrid (NLCNumPanel, 1, NLCNumPanel.getComponentCount(), 0,0,0,0);
        linearityLine1Panel.add(NLCNumPanel);

        SpringUtil.makeCompactGrid (linearityLine1Panel, 1, linearityLine1Panel.getComponentCount(), 0,0,2,0);
        linearityPanel.add (linearityLine1Panel);
        
        JPanel outlierRemovalPanel = new JPanel(new SpringLayout());
        
        JPanel outlierControlPanel = new JPanel(new SpringLayout());
        outlierControlPanel.setPreferredSize(new Dimension(280, textboxHeight));
        cosmicRemovalCB = new JCheckBox("Remove Outliers    ", useCosmicRemoval);
        cosmicRemovalCB.setToolTipText("<html>Remove outliers (cosmic rays/hot pixels) from calibrated science images.<br>"+
                                             "NOTE: Outlier removal may affect photometric accuracy. Use this feature with<br>"+
                                             "extreme caution when extracting photometry from processed images.<br>"+
                                             "NOTE: Outlier removal is applied to science images only.</html>");
        cosmicRemovalCB.setFont(p12);
//        cosmicRemovalCB.setPreferredSize(new Dimension(70, textboxHeight));        
        cosmicRemovalCB.addItemListener(this);
        outlierControlPanel.add(cosmicRemovalCB);  
        
        removeBrightOutliersCB = new JCheckBox("Bright", removeBrightOutliers);
        removeBrightOutliersCB.setToolTipText("<html>Enable to remove outliers above the median level.</html>");
        removeBrightOutliersCB.setFont(p12);
//        outlierBrightOrDarkThresholdCB.setPreferredSize (checkBoxSize);        
        removeBrightOutliersCB.addItemListener(this);
        outlierControlPanel.add(removeBrightOutliersCB); 
        
        removeDarkOutliersCB = new JCheckBox("Dark", removeDarkOutliers);
        removeDarkOutliersCB.setToolTipText("<html>Enable to remove outliers below the median level.</html>");
        removeDarkOutliersCB.setFont(p12);
//        outlierBrightOrDarkThresholdCB.setPreferredSize (checkBoxSize);        
        removeDarkOutliersCB.addItemListener(this);
        outlierControlPanel.add(removeDarkOutliersCB);        
        
        SpringUtil.makeCompactGrid (outlierControlPanel, 1, outlierControlPanel.getComponentCount(), 0,0,2,0);
        outlierRemovalPanel.add (outlierControlPanel);        
        
        outlierRadiusLabel = new JLabel("Radius:");
        outlierRadiusLabel.setToolTipText("<html>The radius in pixels used to determine the median when removing outliers.<br>"+
                                                "NOTE: Outlier removal may affect photometric accuracy. Use this feature with<br>"+
                                                "extreme caution when extracting photometry from processed images.<br>"+
                                                "NOTE: Outlier removal is applied to science images only.</html>");
        outlierRadiusLabel.setHorizontalAlignment (JLabel.RIGHT);
        outlierRadiusLabel.setFont(p12);
        outlierRemovalPanel.add(outlierRadiusLabel);
        
        outlierRadiusModel = new SpinnerNumberModel(outlierRadius, 2, null, 1);
        outlierRadiusSpinner = new JSpinner(outlierRadiusModel);
        JSpinner.NumberEditor outlierRadiusNumberEditor = new JSpinner.NumberEditor(outlierRadiusSpinner, "#" );
        outlierRadiusSpinner.setEditor( outlierRadiusNumberEditor );        
        outlierRadiusSpinner.setPreferredSize (coeffBoxSize);
        outlierRadiusSpinner.setToolTipText("<html>The radius in pixels used to determine the median when removing outliers.<br>"+
                                                "NOTE: Outlier removal may affect photometric accuracy. Use this feature with<br>"+
                                                "extreme caution when extracting photometry from processed images.<br>"+
                                                "NOTE: Outlier removal is applied to science images only.</html>");
        outlierRadiusSpinner.setFont(p12);
        outlierRadiusSpinner.addChangeListener (this);
        outlierRadiusSpinner.addMouseWheelListener(this);
        outlierRemovalPanel.add(outlierRadiusSpinner);

        
        outlierThresholdLabel = new JLabel("    Threshold:");
        outlierThresholdLabel.setToolTipText("<html>The threshold (in ADU) above the median value used to filter outliers.<br>"+
                                                "NOTE: Outlier removal may affect photometric accuracy. Use this feature with<br>"+
                                                "extreme caution when extracting photometry from processed images.<br>"+
                                                "NOTE: Outlier removal is applied to science images only.</html>");
        outlierThresholdLabel.setHorizontalAlignment (JLabel.RIGHT);
        outlierThresholdLabel.setFont(p12);
        outlierRemovalPanel.add(outlierThresholdLabel);
        
        outlierThresholdModel = new SpinnerNumberModel(outlierThreshold, 1, null, 10);
        outlierThresholdSpinner = new JSpinner(outlierThresholdModel);
        JSpinner.NumberEditor outlierThresholdNumberEditor = new JSpinner.NumberEditor(outlierThresholdSpinner, "#" );
        outlierThresholdSpinner.setEditor(outlierThresholdNumberEditor );        
        outlierThresholdSpinner.setPreferredSize (coeffBoxSize);
        outlierThresholdSpinner.setToolTipText("<html>The threshold (in ADU) above the median value used to filter outliers.<br>"+
                                                "NOTE: Outlier removal may affect photometric accuracy. Use this feature with<br>"+
                                                "extreme caution when extracting photometry from processed images.<br>"+
                                                "NOTE: Outlier removal is applied to science images only.</html>");
        outlierThresholdSpinner.setFont(p12);
        outlierThresholdSpinner.addChangeListener (this);
        outlierThresholdSpinner.addMouseWheelListener(this);
        outlierRemovalPanel.add(outlierThresholdSpinner); 
        
        
        JLabel outlierDummyLabel = new JLabel("");
        outlierDummyLabel.setPreferredSize(directoryBoxSize);
        outlierRemovalPanel.add(outlierDummyLabel);        
        
        SpringUtil.makeCompactGrid (outlierRemovalPanel, 1, outlierRemovalPanel.getComponentCount(), 0,0,2,2);
        linearityPanel.add (outlierRemovalPanel);        
        

        SpringUtil.makeCompactGrid (linearityPanel, linearityPanel.getComponentCount(),1, 9,0,7,2);
        mainPanel.add (linearityPanel);





        JPanel FITSHeaderPanel = new JPanel(new SpringLayout());
        FITSHeaderPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(mainBorderColor, 1), "FITS Header Updates", TitledBorder.LEFT, TitledBorder.TOP, b12, Color.DARK_GRAY));

        JPanel calcPanel = new JPanel(new SpringLayout());

//        calcAirmassBox = new JCheckBox("<html>Enable<br>General</html>",calcHeaders);
		calcAirmassBox = new JCheckBox("General",calcHeaders);
        calcAirmassBox.setFont(p12);
        calcAirmassBox.setPreferredSize (checkBoxSize);
        calcAirmassBox.setToolTipText("<html>Calculate the astrophysical quantities defined in the<br>"+
                                      "'FITS Header Settings' panel and save the values to the science image FITS headers.<br>"+
                                      "Click the 'tool' icon to the right to access the 'FITS Header Settings' panel.<br>"+
                                      "Set up 'DP Coorindate Converter' per the DPCC->Help->Help... instructions.<br>"+
                                      "Grayed-out quantites will be set automatically from the target and observatory<br>"+
                                      "source locations specified in the two pull-down selections to the right.</html>");
		calcAirmassBox.addItemListener (this);
		calcPanel.add (calcAirmassBox);

//        JPanel FITSOptionPanel = new JPanel(new SpringLayout());
//        FITSOptionPanel.setPreferredSize(radioSize);
//        plateSolveBox = new JCheckBox("<html>Plate<br>Solve</html>",plateSolve);
		plateSolveBox = new JCheckBox("Plate Solve",plateSolve);
        plateSolveBox.setFont(p12);
        plateSolveBox.setPreferredSize (radioSize);
        plateSolveBox.setToolTipText("<html>Plate solve using network connection to astrometry.net web interface.<br>"+
                                     "Only source coorindates are transfered (not the image) to minimize network traffic.<br>"+
                                     "WCS FITS headers are added to the calibrated image, if solve is successful.<br>"+
                                     "Click the icon to the right to access the 'Astrometry Settings' panel.</html>");
		plateSolveBox.addItemListener (this);
//		FITSOptionPanel.add (plateSolveBox); 
//        
//        JLabel plateSolveDummyLabel = new JLabel(" ");
//        FITSOptionPanel.add (plateSolveDummyLabel); 
//        
//        SpringUtil.makeCompactGrid (FITSOptionPanel, 1, FITSOptionPanel.getComponentCount(), 0,0,0,0);
        calcPanel.add(plateSolveBox);

        JPanel fitsToolPanel = new JPanel(new SpringLayout());
                
        astrometrySetupButton = new JButton(astrometrySetupIcon);
        astrometrySetupButton.setToolTipText("<html>Open plate solve settings panel (Astrometry Settings)</html>");
        astrometrySetupButton.setPreferredSize(iconButtonSize);
        astrometrySetupButton.addActionListener (this);
        fitsToolPanel.add(astrometrySetupButton);        
        
        fitsHeaderToolButton = new JButton(fitsHeaderToolIcon);
        fitsHeaderToolButton.setToolTipText("<html>Open 'General FITS Header Settings' panel</html>");
        fitsHeaderToolButton.setPreferredSize(iconButtonSize);
        fitsHeaderToolButton.addActionListener (this);
        fitsToolPanel.add(fitsHeaderToolButton);

        ImageIcon displayCCIcon = createImageIcon("images/coordinate_converter.png", "Display coordinate converter window");
        displayCCButton = new JButton(displayCCIcon);
        displayCCButton.setPreferredSize(iconButtonSize);
		displayCCButton.addActionListener (this);
        displayCCButton.setToolTipText("<html>Toggle display of DP Coordinate Converter window</html>");
		fitsToolPanel.add (displayCCButton);

        SpringUtil.makeCompactGrid (fitsToolPanel,1,fitsToolPanel.getComponentCount(), 0,14,4,4);
        calcPanel.add(fitsToolPanel);

        JPanel objectCoordinateSourcePanel = new JPanel(new SpringLayout());
        objectCoordinateSourcePanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY, 1), "Target Coordinate Source", TitledBorder.LEFT, TitledBorder.TOP, p12, Color.DARK_GRAY));

		objectCoordinateSourceCombo = new JComboBox(objectCoordinateSources);
//		observatoryLocationSourceCombo.setPreferredSize(new Dimension((int)(coordSize.width*3.0),coordSize.height+2));
        objectCoordinateSourceCombo.setFont(p12);
        objectCoordinateSourceCombo.setSelectedIndex(selectedObjectCoordinateSource<objectCoordinateSources.length?selectedObjectCoordinateSource:0);
//		observatoryIDComboBox.setHorizontalAlignment(JTextField.LEFT);
        objectCoordinateSourceCombo.setToolTipText("<html>"+"Select source of target coordinates"+"</html>");
        objectCoordinateSourceCombo.addActionListener(this);
        objectCoordinateSourcePanel.add (objectCoordinateSourceCombo);
        if (objectCoordinateSourceCombo.getSelectedIndex()==0)
            {
            acc.setEnableObjectEntry(true);
            }
        else
            {
            acc.setEnableObjectEntry(false);
            }

        SpringUtil.makeCompactGrid (objectCoordinateSourcePanel,1,objectCoordinateSourcePanel.getComponentCount(), 0,0,0,0);
        calcPanel.add(objectCoordinateSourcePanel);


        JPanel observatoryLocationSourcePanel = new JPanel(new SpringLayout());
        observatoryLocationSourcePanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY, 1), "Observatory Location Source", TitledBorder.LEFT, TitledBorder.TOP, p12, Color.DARK_GRAY));

		observatoryLocationSourceCombo = new JComboBox(observatoryLocationSources);
//		observatoryLocationSourceCombo.setPreferredSize(new Dimension((int)(coordSize.width*3.0),coordSize.height+2));
        observatoryLocationSourceCombo.setFont(p12);
        observatoryLocationSourceCombo.setSelectedIndex(selectedObservatoryLocationSource<observatoryLocationSources.length?selectedObservatoryLocationSource:0);
//		observatoryIDComboBox.setHorizontalAlignment(JTextField.LEFT);
        observatoryLocationSourceCombo.setToolTipText("<html>"+"Select source of observatory geographic location"+"</html>");
        observatoryLocationSourceCombo.addActionListener(this);
        observatoryLocationSourcePanel.add (observatoryLocationSourceCombo);
        if (observatoryLocationSourceCombo.getSelectedIndex()==0)
            {
            acc.setEnableObservatoryEntry(true);
            }
        else
            {
            acc.setEnableObservatoryEntry(false);
            }
        SpringUtil.makeCompactGrid (observatoryLocationSourcePanel,1,observatoryLocationSourcePanel.getComponentCount(), 0,0,0,0);
        calcPanel.add(observatoryLocationSourcePanel);

        JPanel FITSNumPanel = new JPanel(new SpringLayout());
        FITSNumPanel.setPreferredSize(validFilesSize);
        SpringUtil.makeCompactGrid (FITSNumPanel, 1, FITSNumPanel.getComponentCount(), 0,0,0,0);
        calcPanel.add(FITSNumPanel);

        SpringUtil.makeCompactGrid (calcPanel, 1, calcPanel.getComponentCount(), 0,0,2,0);
        FITSHeaderPanel.add (calcPanel);

        SpringUtil.makeCompactGrid (FITSHeaderPanel, FITSHeaderPanel.getComponentCount(),1, 9,0,7,0);
        mainPanel.add (FITSHeaderPanel);

        
        JPanel outputFilePanel = new JPanel(new SpringLayout());
        outputFilePanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(mainBorderColor, 1), "Save Calibrated Images", TitledBorder.LEFT, TitledBorder.TOP, b12, Color.DARK_GRAY));

        JPanel outputPanel = new JPanel(new SpringLayout());

        saveProcessedDataBox = new JCheckBox("Enable",saveProcessedData);
        saveProcessedDataBox.setFont(p12);
        saveProcessedDataBox.setToolTipText("Save the calibrated images using the settings to the right.");
        saveProcessedDataBox.setPreferredSize (checkBoxSize);
		saveProcessedDataBox.addItemListener (this);
        outputPanel.add(saveProcessedDataBox);


        JPanel saveRadioPanelPanel = new JPanel(new SpringLayout());
        saveRadioPanelPanel.setPreferredSize(radioSize);

        saveRadioGroup = new ButtonGroup ();

        saveIntegerRadio = new JRadioButton("16  ");
        saveIntegerRadio.setToolTipText("select for 16-bit integer output");
        if (!saveFloatingPoint) saveIntegerRadio.setSelected(true);
        else saveIntegerRadio.setSelected(false);
        saveRadioPanelPanel.add (saveIntegerRadio);
        saveFloatingPointRadio = new JRadioButton("32  ");
        saveFloatingPointRadio.setToolTipText("select for 32-bit floating point output");
        if (saveFloatingPoint) saveFloatingPointRadio.setSelected(true);
        else saveFloatingPointRadio.setSelected(false);
        saveRadioPanelPanel.add (saveFloatingPointRadio);
        saveRadioGroup.add(saveIntegerRadio);
        saveRadioGroup.add(saveFloatingPointRadio);
        saveIntegerRadio.addActionListener(this);
        saveFloatingPointRadio.addActionListener(this);

        SpringUtil.makeCompactGrid (saveRadioPanelPanel, 1, saveRadioPanelPanel.getComponentCount(), 0,0,0,0);
        outputPanel.add(saveRadioPanelPanel);

        subDirLabel = new JLabel("  Sub-dir:");
        subDirLabel.setFont(p12);
		subDirLabel.setHorizontalAlignment (JTextField.RIGHT);
        outputPanel.add (subDirLabel);

		saveDirField = new JTextField (saveDir);
		saveDirField.setPreferredSize (fileBoxSize);
        saveDirField.setFont(p12);
        saveDirField.setToolTipText("<html>"+"Calibrated science images are stored in this subdirectory of the science image directory."+"<br>"+
                                             "Leave blank to save in science directory." +"</html>");
		saveDirField.setHorizontalAlignment (JTextField.LEFT);
        saveDirField.getDocument().addDocumentListener(new thisDocumentListener());
		outputPanel.add (saveDirField);

        suffixLabel = new JLabel("  Suffix:");
        suffixLabel.setFont(p12);
		suffixLabel.setHorizontalAlignment (JTextField.RIGHT);
        outputPanel.add (suffixLabel);

		saveSuffixField = new JTextField (saveSuffix);
        saveSuffixField.setFont(p12);
		saveSuffixField.setPreferredSize (radioSize);
        saveSuffixField.setToolTipText("<html>"+"Append this string to all calibrated image"+"<br>"+
                                                "output filenames (before the filetype)."+"<br>"+
                                                "Leave blank for no suffix." +"</html>");
		saveSuffixField.setHorizontalAlignment (JTextField.LEFT);
        saveSuffixField.getDocument().addDocumentListener(new thisDocumentListener());
		outputPanel.add (saveSuffixField);

        saveFormatLabel = new JLabel("  Format:");
        saveFormatLabel.setFont(p12);
		saveFormatLabel.setHorizontalAlignment (JTextField.RIGHT);
        outputPanel.add (saveFormatLabel);

		saveFormatField = new JTextField (saveFormat);
        saveFormatField.setFont(p12);
		saveFormatField.setPreferredSize (radioSize);
        saveFormatField.setToolTipText("<html>"+"Enter the file save format:"+"<br>"+
                                    "-Leave blank to use the input format"+"<br>"+
                                    "-FITS format valid designations are:"+"<br>"+
                                    "--- .fits .FITS .fit, .FIT .fts .FTS"+"<br>"+
                                    "-Other formats include all native ImageJ formats such as:"+"<br>"+
                                    "--- .tif .jpg .gif .bmp .png .raw .avi .zip"+"</html>");
		saveFormatField.setHorizontalAlignment (JTextField.LEFT);
        saveFormatField.getDocument().addDocumentListener(new thisDocumentListener());
		outputPanel.add (saveFormatField);
        
        compressBox = new JCheckBox("GZIP",compress);
        compressBox.setFont(p12);
        compressBox.setToolTipText("Compress outfile file in GZIP format (compresses FITS header information also).");
		compressBox.addItemListener (this);
        outputPanel.add(compressBox);        

        JPanel outputNumPanel = new JPanel(new SpringLayout());
        outputNumPanel.setPreferredSize(validFilesSize);

        SpringUtil.makeCompactGrid (outputNumPanel, 1, outputNumPanel.getComponentCount(), 0,0,0,0);
        outputPanel.add(outputNumPanel);

        SpringUtil.makeCompactGrid (outputPanel, 1, outputPanel.getComponentCount(), 0,0,2,0);
        outputFilePanel.add (outputPanel);

        SpringUtil.makeCompactGrid (outputFilePanel, outputFilePanel.getComponentCount(),1, 9,0,7,2);
        mainPanel.add (outputFilePanel);


        JPanel postProcessPanel = new JPanel(new SpringLayout());
        postProcessPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(mainBorderColor, 1), "Post Processing", TitledBorder.LEFT, TitledBorder.TOP, b12, Color.DARK_GRAY));

        JPanel postPanel = new JPanel(new SpringLayout());
        
		runMultiApertureBox = new JCheckBox("M-Ap  ",runMultiAperture);
        runMultiApertureBox.setFont(p12);
        runMultiApertureBox.setToolTipText("<html>Run Multi-Aperture after each science image has been calibrated.<br>"+
                                           "The processing order of enabled options is:<br>"+
                                           "Image calibration, Macro 1, Outlier Removal, Macro 2, Plate Solve, Save processed images, <br>"+
                                           "Multi-aperture, Multi-plot, Save image display, Save plot display</html>");
//        runMultiApertureBox.setPreferredSize (checkBoxSize);
		runMultiApertureBox.addItemListener (this);
		postPanel.add (runMultiApertureBox);
        
        JPanel saveImagePanel = new JPanel(new SpringLayout());
        saveImagePanel.setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY));        
        

        saveImageBox = new JCheckBox("Save Image",saveImage);
        saveImageBox.setFont(p12);
        saveImageBox.setEnabled(showScience);
        saveImageBox.setToolTipText("<html>Save calibrated science image display (with aperture and other overlays) to a specified fixed file<br>"+
                                         "(e.g. to supply an image to update a webpage as observations are runnning).<br>"+
                                         "The image can be saved as either PNG (use .png filename suffix) or JPEG (use .jpg filename suffix).<br>"+
                                         "'Show science images while processing' must be enabled in 'View' menu to use this feature.</html>");
        saveImageBox.setPreferredSize (radioSize);
		saveImageBox.addItemListener (this);
        saveImagePanel.add(saveImageBox);  
        
        JPanel saveImageTextPanel = new JPanel(new SpringLayout());

		saveImageTextField = new JTextField (saveImagePath);
        saveImageTextField.setEnabled(showScience);
        saveImageTextField.setFont(p12);
        saveImageTextField.setToolTipText("<html>Enter path to save science image display or click the file opener to the right to select a file.<br>"+
                                                "'Show science images while processing' must be enabled in 'View' menu to use this feature.</html>");
        saveImageTextField.getDocument().addDocumentListener(new thisDocumentListener());
		saveImageTextField.setPreferredSize (macroBoxSize);
		saveImageTextField.setHorizontalAlignment (JTextField.LEFT);
		saveImageTextPanel.add (saveImageTextField);
        
        SpringUtil.makeCompactGrid (saveImageTextPanel, 1, saveImageTextPanel.getComponentCount(), 0,3,0,2);
        saveImagePanel.add(saveImageTextPanel);         
        
        saveImagePathButton = new JButton(fileOpenIcon);
        saveImagePathButton.setPreferredSize(folderIconSize);
        saveImagePathButton.setOpaque(false);
        saveImagePathButton.setEnabled(showScience);
        saveImagePathButton.setFocusPainted(false);
        saveImagePathButton.setBorderPainted(false);
        saveImagePathButton.setContentAreaFilled(false);
        saveImagePathButton.setBorder(BorderFactory.createEmptyBorder(0,0,0,folderIconRightMargin));
        saveImagePathButton.setToolTipText("<html>Click to set science image display save file name.<br>"+
                                                 "'Show science images while processing' must be enabled in 'View' menu to use this feature.</html>");
		saveImagePathButton.addActionListener (this);
		saveImagePanel.add (saveImagePathButton); 
        
        SpringUtil.makeCompactGrid (saveImagePanel, 1, saveImagePanel.getComponentCount(), 0,0,2,0);
        postPanel.add(saveImagePanel);         
        
       
        JPanel preMacroPanel = new JPanel(new SpringLayout());
        preMacroPanel.setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY));         

		runPreMacroBox = new JCheckBox("Macro 1",runPreMacro);
        runPreMacroBox.setToolTipText("<html>Enable Macro 1.<br>"+
                                      "The processing order of enabled options is:<br>"+
                                      "Image calibration, Macro 1, Outlier Removal, Macro 2, Plate Solve, Save processed images, <br>"+
                                      "Multi-aperture, Multi-plot, Save image display, Save plot display</html>");
        runPreMacroBox.setFont(p12);
//        runPreMacroBox.setPreferredSize (radioSize);
		runPreMacroBox.addItemListener (this);
        preMacroPanel.add(runPreMacroBox);
        
        JPanel preMacroTextPanel = new JPanel(new SpringLayout());

		preMacroText = new JTextField (preMacroPath);
        preMacroText.setFont(p12);
        preMacroText.setToolTipText("Enter path to a valid macro file or click the file opener to the right to select a macro file.");
        preMacroText.getDocument().addDocumentListener(new thisDocumentListener());
		preMacroText.setPreferredSize (macroBoxSize);
		preMacroText.setHorizontalAlignment (JTextField.LEFT);
		preMacroTextPanel.add (preMacroText);
        
        SpringUtil.makeCompactGrid (preMacroTextPanel, 1, preMacroTextPanel.getComponentCount(),  0,3,0,2);
        preMacroPanel.add(preMacroTextPanel);  

		preMacroButton = new JButton (fileOpenIcon);
        preMacroButton.setPreferredSize(folderIconSize);
        preMacroButton.setOpaque(false);
        preMacroButton.setFocusPainted(false);
        preMacroButton.setBorderPainted(false);
        preMacroButton.setContentAreaFilled(false);
        preMacroButton.setBorder(BorderFactory.createEmptyBorder(0,0,0,folderIconRightMargin));
        preMacroButton.setToolTipText("<html>Click to select an AstroImageJ macro file.</html>");
		preMacroButton.addActionListener (this);
		preMacroPanel.add (preMacroButton);

        validMacro1FilesLabel = new JLabel ("");
        validMacro1FilesLabel.setToolTipText("The number of macro files in the specified directory matching the filename (0 or 1)");
        validMacro1FilesLabel.setHorizontalAlignment(JLabel.RIGHT);
        validMacro1FilesLabel.setFont(b12);
		validMacro1FilesLabel.setPreferredSize (validFilesSize);
        preMacroPanel.add (validMacro1FilesLabel);
        
        SpringUtil.makeCompactGrid (preMacroPanel, 1, preMacroPanel.getComponentCount(), 0,0,3,0);
        postPanel.add(preMacroPanel);         

        
        runMultiPlotBox = new JCheckBox("M-Plot",runMultiPlot);
        runMultiPlotBox.setFont(p12);
        runMultiPlotBox.setToolTipText("<html>Run Multi-Plot after each science image has been calibrated.<br>"+
                                       "The processing order of enabled options is:<br>"+
                                       "Image calibration, Macro 1, Outlier Removal, Macro 2, Plate Solve, Save processed images, <br>"+
                                       "Multi-aperture, Multi-plot, Save image display, Save plot display</html>");
        runMultiPlotBox.setPreferredSize (checkBoxSize);
		runMultiPlotBox.addItemListener (this);
        postPanel.add(runMultiPlotBox);
        
        JPanel savePlotPanel = new JPanel(new SpringLayout());
        savePlotPanel.setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY));
        
        savePlotBox = new JCheckBox("Save Plot",savePlot);
        savePlotBox.setFont(p12);
        savePlotBox.setEnabled(runMultiPlot);
        savePlotBox.setToolTipText("<html>Save plot image to a specified fixed file after multi-aperture runs on each calibrated science image<br>"+
                                         "(e.g. to supply a plot image to update a webpage as observations are runnning).<br>"+
                                         "The plot can be saved as either PNG (use .png filename suffix) or JPEG (use .jpg filename suffix).<br>"+
                                         "'M-Plot' must be enabled to use this feature.</html>");
        savePlotBox.setPreferredSize (radioSize);
		savePlotBox.addItemListener (this);
        savePlotPanel.add(savePlotBox);  
        
        JPanel savePlotTextPanel = new JPanel(new SpringLayout());

		savePlotTextField = new JTextField (savePlotPath);
        savePlotTextField.setFont(p12);
        savePlotTextField.setToolTipText("<html>Enter path to save plot image or click the file opener to the right to select a file.<br>"+
                                         "'M-Plot' must be enabled to use this feature.</html>");
        savePlotTextField.getDocument().addDocumentListener(new thisDocumentListener());
		savePlotTextField.setPreferredSize (macroBoxSize);
		savePlotTextField.setHorizontalAlignment (JTextField.LEFT);
		savePlotTextPanel.add (savePlotTextField);
        
        SpringUtil.makeCompactGrid (savePlotTextPanel, 1, savePlotTextPanel.getComponentCount(), 0,3,0,2);
        savePlotPanel.add(savePlotTextPanel);         
        
        savePlotPathButton = new JButton(fileOpenIcon);
        savePlotPathButton.setPreferredSize(folderIconSize);
        savePlotPathButton.setOpaque(false);
        savePlotPathButton.setEnabled(runMultiPlot);
        savePlotPathButton.setFocusPainted(false);
        savePlotPathButton.setBorderPainted(false);
        savePlotPathButton.setContentAreaFilled(false);
        savePlotPathButton.setBorder(BorderFactory.createEmptyBorder(0,0,0,folderIconRightMargin));
        savePlotPathButton.setToolTipText("<html>Click to set plot image save file name.<br>"+
                                         "'M-Plot' must be enabled to use this feature.</html>");
		savePlotPathButton.addActionListener (this);
		savePlotPanel.add (savePlotPathButton); 
        
        SpringUtil.makeCompactGrid (savePlotPanel, 1, savePlotPanel.getComponentCount(), 0,0,2,0);
        postPanel.add(savePlotPanel);        
        
        JPanel postMacroPanel = new JPanel(new SpringLayout());
        postMacroPanel.setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY));        

		runPostMacroBox = new JCheckBox("Macro 2",runPostMacro);
        runPostMacroBox.setToolTipText("<html>Enable Macro 2.<br>"+
                                       "The processing order of enabled options is:<br>"+
                                       "Image calibration, Macro 1, Outlier Removal, Macro 2, Plate Solve, Save processed images, <br>"+
                                       "Multi-aperture, Multi-plot, Save image display, Save plot display</html>");
        runPostMacroBox.setFont(p12);
//        runPostMacroBox.setPreferredSize (radioSize);
		runPostMacroBox.addItemListener (this);
        postMacroPanel.add(runPostMacroBox);
        
        JPanel postMacroTextPanel = new JPanel(new SpringLayout());

		postMacroText = new JTextField (postMacroPath);
        postMacroText.setFont(p12);
        postMacroText.setToolTipText("Enter path to a valid macro file or click the file opener to the right to select a macro file.");
        postMacroText.getDocument().addDocumentListener(new thisDocumentListener());
		postMacroText.setPreferredSize (macroBoxSize);
		postMacroText.setHorizontalAlignment (JTextField.LEFT);
		postMacroTextPanel.add (postMacroText);
        
        SpringUtil.makeCompactGrid (postMacroTextPanel, 1, postMacroTextPanel.getComponentCount(), 0,3,0,2);
        postMacroPanel.add(postMacroTextPanel);         

		postMacroButton = new JButton (fileOpenIcon);
        postMacroButton.setPreferredSize(folderIconSize);
        postMacroButton.setOpaque(false);
        postMacroButton.setFocusPainted(false);
        postMacroButton.setBorderPainted(false);
        postMacroButton.setContentAreaFilled(false);
        postMacroButton.setBorder(BorderFactory.createEmptyBorder(0,0,0,folderIconRightMargin));
        postMacroButton.setToolTipText("<html>Click to select an AstroImageJ macro file.</html>");
		postMacroButton.addActionListener (this);
		postMacroPanel.add (postMacroButton);
      

        validMacro2FilesLabel = new JLabel ("");
        validMacro2FilesLabel.setToolTipText("The number of macro files in the specified directory matching the filename (0 or 1)");
        validMacro2FilesLabel.setHorizontalAlignment(JLabel.RIGHT);
        validMacro2FilesLabel.setFont(b12);
		validMacro2FilesLabel.setPreferredSize (validFilesSize);
        postMacroPanel.add (validMacro2FilesLabel);
                
        SpringUtil.makeCompactGrid (postMacroPanel, 1, postMacroPanel.getComponentCount(), 1,1,3,1);
        postPanel.add(postMacroPanel);          

//        JPanel macro2NumPanel = new JPanel(new SpringLayout());
//        macro2NumPanel.setPreferredSize(validFilesSize);
//        SpringUtil.makeCompactGrid (macro2NumPanel, 1, macro2NumPanel.getComponentCount(), 0,0,0,0);
//        postPanel.add(macro2NumPanel);

        SpringUtil.makeCompactGrid (postPanel, 2, postPanel.getComponentCount()/2, 0,0,2,2);
        postProcessPanel.add (postPanel);

        SpringUtil.makeCompactGrid (postProcessPanel, postProcessPanel.getComponentCount(),1, 9,0,7,2);
        mainPanel.add (postProcessPanel);




        JPanel controlProcessingPanel = new JPanel(new SpringLayout());
        controlProcessingPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(mainBorderColor, 1), "Control Panel", TitledBorder.LEFT, TitledBorder.TOP, b12, Color.DARK_GRAY));

        JPanel controlPanel = new JPanel();
        controlPanel.setLayout(new BoxLayout(controlPanel,BoxLayout.X_AXIS));

        ImageIcon setApertureIcon = createImageIcon("images/setaperture.png", "Set aperture options");
        ImageIcon changeAperturesIcon = createImageIcon("images/changeapertures.png", "Change multi-aperture settings the next time it runs");
        ImageIcon clearTableIcon = createImageIcon("images/cleartable.png", "Clear measurements table data");

        JPanel pollingPanel = new JPanel();//new SpringLayout());
        pollingPanel.setLayout(new BoxLayout(pollingPanel,BoxLayout.X_AXIS));
        pollingPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY, 1), "Polling Interval", TitledBorder.CENTER, TitledBorder.TOP, p12, Color.DARK_GRAY));
//        pollingPanel.setMaximumSize(radioSize);
        
        pollingIntervalModel = new SpinnerNumberModel(pollingInterval, 0, null, 1);
        pollingIntervalSpinner = new JSpinner(pollingIntervalModel);
        pollingIntervalSpinner.setMaximumSize(new Dimension(radioWidth+20,textboxHeight));
        pollingIntervalSpinner.setPreferredSize(new Dimension(radioWidth+20,textboxHeight));
        pollingIntervalSpinner.setToolTipText("<html>"+"Set polling interval to 0 to disable polling (processes all existing files one time and stops)."+"<br>"+
                                   "For polling intervals > 0, the science image directory will be rescanned at that interval (in secs)."+"</html>");
        pollingIntervalSpinner.addChangeListener (this);
        pollingIntervalSpinner.addMouseWheelListener(this);
        pollingPanel.add(pollingIntervalSpinner);
//        SpringUtil.makeCompactGrid (pollingPanel, pollingPanel.getComponentCount(),1, 2,0,2,2);
        controlPanel.add (pollingPanel);
        controlPanel.add(Box.createHorizontalStrut(60));

        setApertureButton = new JButton(setApertureIcon);
        setApertureButton.setPreferredSize(iconButtonSize);
		setApertureButton.addActionListener (this);
        setApertureButton.setToolTipText("Change aperture settings");
		controlPanel.add (setApertureButton);
        controlPanel.add(Box.createHorizontalStrut(10));

        changeAperturesButton = new JButton(changeAperturesIcon);
        changeAperturesButton.setPreferredSize(iconButtonSize);
		changeAperturesButton.addActionListener (this);
        changeAperturesButton.setToolTipText("Display Multi-Aperture settings the next time Multi-Aperture runs");
		controlPanel.add (changeAperturesButton);
        controlPanel.add(Box.createHorizontalStrut(10));

        clearTableButton = new JButton(clearTableIcon);
		clearTableButton.addActionListener (this);
        clearTableButton.setToolTipText("Clear Measurements table data");
		controlPanel.add (clearTableButton);
        controlPanel.add(Box.createHorizontalStrut(10));

//        controlPanel.setBorder(BorderFactory.createEmptyBorder());
		startButton = new JButton ("START");
        startButton.setToolTipText("<html>Click START to begin processing all files. While files are being<br>"+
                                   "processed, this button shows RUNNING. If PAUSE is pressed while RUNNING,<br>"+
                                   "this button shows CONTINUE. Pressing CONTINUE resumes processing at the<br>"+
                                   "point where PAUSE was pressed.</html>");
        startButton.setMargin(new Insets(5,25,5,25));
        startButton.setPreferredSize(new Dimension(125, 30));
        startButton.setFont(b12);
		startButton.addActionListener (this);
		controlPanel.add (startButton);
        controlPanel.add(Box.createHorizontalStrut(10));

		pauseButton = new JButton ("PAUSE");
		pauseButton.addActionListener (this);
        pauseButton.setToolTipText("<html>Click to PAUSE processing of files while in RUNNING state. Click CONTINUE<br>"+
                                   "to resume processing files at the point where PAUSE was pressed, or<br>"+
                                   "press RESET and then START to start processing all files again.</html>");
        pauseButton.setMargin(new Insets(5,25,5,25));
        pauseButton.setFont(b12);
        pauseButton.setPreferredSize(new Dimension(125, 30));
		controlPanel.add (pauseButton);
        controlPanel.add(Box.createHorizontalStrut(30));

        clearButton = new JButton("RESET");
        clearButton.setMargin(new Insets(5,25,5,25));
        clearButton.setFont(b12);
        clearButton.setPreferredSize(new Dimension(125, 30));
        clearButton.addActionListener (this);
        clearButton.setToolTipText("<html>Resets the file processing queue. If in the RUNNING state, press PAUSE first, then RESET.<br>"+
                                   "Pressing START after RESET causes all files to be processed again (if 'Process only new...'<br>"+
                                   "is UNchecked in the Preferences menu).</html>");
        controlPanel.add(clearButton);
        controlPanel.add(Box.createHorizontalStrut(30));

        controlPanel.add(Box.createHorizontalGlue());

        JPanel statusPanel = new JPanel(new SpringLayout());

        JLabel processedLabel = new JLabel("  Processed:");
        processedLabel.setFont(b12);
		processedLabel.setHorizontalAlignment (JTextField.RIGHT);
        statusPanel.add (processedLabel);

        processedNumLabel = new JLabel ("0");
        processedNumLabel.setToolTipText("The number of science files processed");
        processedNumLabel.setHorizontalAlignment(JLabel.RIGHT);
        processedNumLabel.setFont(b12);
		processedNumLabel.setPreferredSize (validFilesSize);
        statusPanel.add (processedNumLabel);
        
        JLabel ignoredLabel = new JLabel("  Remaining:");
        ignoredLabel.setFont(b12);
		ignoredLabel.setHorizontalAlignment (JTextField.RIGHT);
        statusPanel.add (ignoredLabel);

        remainingNumLabel = new JLabel ("0");
        remainingNumLabel.setToolTipText("The number of science files left in the queue to process (as of last poll)");
        remainingNumLabel.setHorizontalAlignment(JLabel.RIGHT);
        remainingNumLabel.setFont(b12);
		remainingNumLabel.setPreferredSize (validFilesSize);
        statusPanel.add (remainingNumLabel);

        SpringUtil.makeCompactGrid (statusPanel, 2, statusPanel.getComponentCount()/2, 0,0,2,0);
        controlPanel.add(statusPanel);

        controlProcessingPanel.add(controlPanel);
        SpringUtil.makeCompactGrid (controlProcessingPanel, controlProcessingPanel.getComponentCount(),1, 2,0,7,2);
        mainPanel.add (controlProcessingPanel);

		SpringUtil.makeCompactGrid (mainPanel,mainPanel.getComponentCount(), 1, 0,0,0,0);
        dialogFrame.setJMenuBar(menuBar);
        dialogFrame.add (mainScrollPane);

		dialogFrame.pack();
		dialogFrame.setResizable (true);
        
        IJU.setFrameSizeAndLocation(dialogFrame, dialogFrameLocationX, dialogFrameLocationY, 0, 0);
        setEnableControls();
        ToolTipManager.sharedInstance().setEnabled(showToolTips);
		dialogFrame.setVisible (true);

        if (calcHeaders) acc.showPanel(true);
        
        countValidFiles();
        
        if (autoRunAndClose)
            {
            if (timer != null) timer.cancel();
            if (task != null) task.cancel();
            if (onlyNew && !startButton.getText().equals("CONTINUE")) firstRun=true;
            startButton.setText("RUNNING");
            startButton.setForeground(Color.red);
            startButton.repaint();
            length = 0;
            running = true;
            astrometryCanceledByUser = false;
            active = true;
            requestStop = false;
            startTimer();
            }
		}
    


    public void saveAndClose()
            {
            Prefs.set("multiaperture.canceled",true);
            if (timer != null) timer.cancel();
            if (task != null) task.cancel();
            running = false;
            active = false;
            savePrefs();
            if (astrometrySetup != null) astrometrySetup.exit();
            if (astrometry != null) astrometry.setAstrometryCanceled();
            acc.saveAndClose();
            fitsHeaderFrame.dispose();
            dialogFrame.dispose();
            }

    public void stateChanged(ChangeEvent ev)
        {
        if (ev.getSource() == minFileNumberSpinner)
            {
            minFileNumber = (Long) minFileNumberSpinner.getValue();
            Prefs.set("dataproc.minFileNumber", minFileNumber);
            countValidFiles();
            }
        else if (ev.getSource() == maxFileNumberSpinner)
            {
            maxFileNumber = (Long) maxFileNumberSpinner.getValue();
            if (maxFileNumber < minFileNumber) maxFileNumberSpinner.setValue((Long)minFileNumber);
            Prefs.set("dataproc.maxFileNumber", maxFileNumber);
            countValidFiles();
            }
        else if (ev.getSource() == pollingIntervalSpinner)
            {
            pollingInterval = (Integer) pollingIntervalSpinner.getValue();
            Prefs.set("dataproc.pollingInterval", pollingInterval);
            }
        else if (ev.getSource() == outlierRadiusSpinner)
            {
            outlierRadius = (Integer) outlierRadiusSpinner.getValue();
            Prefs.set("dataproc.outlierRadius", outlierRadius);
            }  
        else if (ev.getSource() == outlierThresholdSpinner)
            {
            outlierThreshold = (Integer) outlierThresholdSpinner.getValue();
            Prefs.set("dataproc.outlierThreshold", outlierThreshold);
            }         
        else if (ev.getSource() == coeffASpinner)
            {
            coeffA = (Double) coeffASpinner.getValue();
            Prefs.set("dataproc.coeffA", coeffA);
            }
        else if (ev.getSource() == coeffBSpinner)
            {
            coeffB = (Double) coeffBSpinner.getValue();
            Prefs.set("dataproc.coeffB", coeffB);
            }
        else if (ev.getSource() == coeffCSpinner)
            {
            coeffC = (Double) coeffCSpinner.getValue();
            Prefs.set("dataproc.coeffC", coeffC);
            }
        else if (ev.getSource() == coeffDSpinner)
            {
            coeffD = (Double) coeffDSpinner.getValue();
            Prefs.set("dataproc.coeffD", coeffD);
            }        
        }


    public void mouseWheelMoved( MouseWheelEvent e )
        {
        if (e.getSource() == minFileNumberSpinner && minFileNumberSpinner.isEnabled())
            {
            long newValue = (Long) minFileNumberSpinner.getValue() - e.getWheelRotation();
            if (newValue >= 0) minFileNumberSpinner.setValue(newValue);
            }
        else if (e.getSource() == maxFileNumberSpinner && maxFileNumberSpinner.isEnabled())
            {
            long newValue = (Long) maxFileNumberSpinner.getValue() - e.getWheelRotation();
            if (newValue >= 0) maxFileNumberSpinner.setValue(newValue>(Long)minFileNumberSpinner.getValue()?newValue:(Long)minFileNumberSpinner.getValue());
            }
        else if (e.getSource() == pollingIntervalSpinner)
            {
            int newValue = (Integer) pollingIntervalSpinner.getValue() - e.getWheelRotation();
            if (newValue > 0) pollingIntervalSpinner.setValue(newValue);
            }
        else if (e.getSource() == outlierRadiusSpinner && outlierRadiusSpinner.isEnabled())
            {
            int newValue = (Integer) outlierRadiusSpinner.getValue() - e.getWheelRotation();
            if (newValue > 1) outlierRadiusSpinner.setValue(newValue);
            }  
        else if (e.getSource() == outlierThresholdSpinner && outlierThresholdSpinner.isEnabled())
            {
            int newValue = (Integer) outlierThresholdSpinner.getValue() - 10 * e.getWheelRotation();
            if (newValue > 0) outlierThresholdSpinner.setValue(newValue);
            }         
        else if (e.getSource() == coeffASpinner)
            {
            coeffASpinner.setValue((Double) coeffASpinner.getValue()
                    - e.getWheelRotation()*0.0 );
            }
        else if (e.getSource() == coeffBSpinner)
            {
            coeffBSpinner.setValue((Double) coeffBSpinner.getValue()
                    - e.getWheelRotation()*0.0 );
            }
        else if (e.getSource() == coeffCSpinner)
            {
            coeffCSpinner.setValue((Double) coeffCSpinner.getValue()
                    - e.getWheelRotation()*0.0 );
            }
        else if (e.getSource() == coeffDSpinner)
            {
            coeffDSpinner.setValue((Double) coeffDSpinner.getValue()
                    - e.getWheelRotation()*0.0 );
            }        
        }


	public void itemStateChanged (ItemEvent e)
		{
		Object source = e.getItemSelectable();
        boolean selectedState = e.getStateChange() == ItemEvent.SELECTED;

        if (source == onlyNewCB)
            onlyNew = selectedState;
        else if (source == runPreMacroBox)
            runPreMacro = selectedState;
        else if (source == runPostMacroBox)
            runPostMacro = selectedState;
        else if (source == usepreMacro1AutoLevelCB)
            preMacro1AutoLevel = selectedState;
        else if (source == postMacro1AutoLevelCB)
            postMacro1AutoLevel = selectedState;
        else if (source == postMacro2AutoLevelCB)
            postMacro2AutoLevel = selectedState;
        else if (source == runMultiApertureBox)
            runMultiAperture = selectedState;
        else if (source == saveImageBox)
            {
            saveImage = selectedState;  
            if (saveImage) saveStaticImage();
            }            
        else if (source == runMultiPlotBox)
            runMultiPlot = selectedState;
        else if (source == savePlotBox)
            {
            savePlot = selectedState;  
            if (savePlot) saveStaticPlot();
            }
        else if (source == useBeepCB)
            useBeep = selectedState;
        else if (source == autoRunAndCloseCB)
            {
            autoRunAndClose = selectedState;
            Prefs.set ("dataproc.autoRunAndClose",autoRunAndClose);
            }
        else if (source == useScienceProcessingBox)
            useScienceProcessing = selectedState;
        else if (source == sortNumericallyBox)
            sortNumerically = selectedState;
        else if (source == enableFileNumberFilteringBox)
            enableFileNumberFiltering = selectedState;
        else if (source == useBiasBox)
            useBias = selectedState;
        else if (source == createBiasBox)
            createBias = selectedState;
        else if (source == useDarkBox)
            useDark = selectedState;
        else if (source == createDarkBox)
            createDark = selectedState;
        else if (source == useFlatBox)
            useFlat = selectedState;
        else if (source == createFlatBox)
            createFlat = selectedState;
        else if (source == useNLCBox)
            useNLC = selectedState;
        else if (source == scaleExpTimeBox)
            scaleExpTime = selectedState;
        else if (source == deBiasMasterDarkBox)
            deBiasMasterDark = selectedState;            
        else if (source == saveProcessedDataBox)
            saveProcessedData = selectedState;
        else if (source == compressBox)
            compress = selectedState;            
        else if (source == showMasterImagesCB)
            showMasters = selectedState;
        else if (source == showRawCalsCB)
            showRawCals = selectedState;                        
        else if (source == showScienceCB)
            showScience = selectedState;
        else if (source == useShowLogCB)
            showLog = selectedState;
        else if (source == showLogDateTimeCB)
            showLogDateTime = selectedState;            
        else if (source == gradientRemovalCB)
            useGradientRemoval = selectedState;
        else if (source == removeBrightOutliersCB)
            removeBrightOutliers = selectedState; 
        else if (source == removeDarkOutliersCB)
            removeDarkOutliers = selectedState;        
        else if (source == cosmicRemovalCB)
            useCosmicRemoval = selectedState;
        else if (source == calcAirmassBox)
            {
            calcHeaders = selectedState;
            acc.showPanel(calcHeaders);
            }
        else if (source == plateSolveBox)
            {
            plateSolve = selectedState;
            }            
        else if (source == showToolTipsCB)
            showToolTips = selectedState;
        else if (source == autoWildcardCB)
            autoWildcard = selectedState;
        else if (source == rawCalCommonDirCB)
            rawCalCommonDir = selectedState;
        else if (source == masterCalCommonDirCB)
            masterCalCommonDir = selectedState;
        else if (source == saveObjectRAJ2000CB)
            saveObjectRAJ2000 = selectedState;
        else if (source == saveObjectDecJ2000CB)
            saveObjectDecJ2000 = selectedState;
        else if (source == saveObjectRAEODCB)
            saveObjectRAEOD = selectedState;
        else if (source == saveObjectDecEODCB)
            saveObjectDecEOD = selectedState;
        else if (source == saveObjectAltitudeCB)
            saveObjectAltitude = selectedState;
        else if (source == saveObjectAzimuthCB)
            saveObjectAzimuth = selectedState;
        else if (source == saveObjectHourAngleCB)
            saveObjectHourAngle = selectedState;  
        else if (source == saveObjectZenithDistanceCB)
            saveObjectZenithDistance = selectedState;            
        else if (source == saveObjectAirmassCB)
            saveObjectAirmass = selectedState;
        else if (source == saveJD_SOBSCB)
            saveJD_SOBS = selectedState;
        else if (source == saveJD_MOBSCB)
            saveJD_MOBS = selectedState;
        else if (source == saveHJD_MOBSCB)
            saveHJD_MOBS = selectedState;
        else if (source == saveBJD_MOBSCB)
            saveBJD_MOBS = selectedState;
        else if (source == saveObservatoryLatCB)
            saveObservatoryLat = selectedState;
        else if (source == saveObservatoryLonCB)
            saveObservatoryLon = selectedState;
        else if (source == raInDegreesCB)
            raInDegrees = selectedState;
        else if (source == latNegateCB)
            latNegate = selectedState;
        else if (source == lonNegateCB)
            lonNegate = selectedState;

        setEnableControls();
        countValidFiles();
        ToolTipManager.sharedInstance().setEnabled(showToolTips);
        Prefs.set ("astroIJ.showToolTips",showToolTips);
		}
       
    void setEnableControls()
        {
        sortNumericallyBox.setEnabled(useScienceProcessing);
        dirText.setEnabled(useScienceProcessing);
        dirButton.setEnabled(useScienceProcessing);
        filenamePatternText.setEnabled(useScienceProcessing);
        fileButton.setEnabled(useScienceProcessing);
        
        enableFileNumberFilteringBox.setEnabled(useScienceProcessing);
        minFileNumberLabel.setEnabled(enableFileNumberFiltering && useScienceProcessing);
        minFileNumberSpinner.setEnabled(enableFileNumberFiltering && useScienceProcessing);
        maxFileNumberLabel.setEnabled(enableFileNumberFiltering && useScienceProcessing);
        maxFileNumberSpinner.setEnabled(enableFileNumberFiltering && useScienceProcessing);
        numPrefixField.setEnabled(enableFileNumberFiltering && useScienceProcessing);
        
        biasAverageRadio.setEnabled(createBias);
        biasMedianRadio.setEnabled(createBias);
        biasRawDirField.setEnabled(createBias);
        biasBaseDirButton.setEnabled(createBias);
        biasBaseField.setEnabled(createBias);
        biasBaseButton.setEnabled(createBias);
        
        biasMasterDirField.setEnabled(createBias || useBias);
        biasMasterDirButton.setEnabled(createBias || useBias);
        biasMasterField.setEnabled(createBias || useBias);
        biasMasterButton.setEnabled(createBias || useBias);
        
        darkAverageRadio.setEnabled(createDark);
        darkMedianRadio.setEnabled(createDark);
        darkRawDirField.setEnabled(createDark);
        darkBaseDirButton.setEnabled(createDark);
        darkBaseField.setEnabled(createDark);
        darkBaseButton.setEnabled(createDark);
        
        scaleExpTimeBox.setEnabled(useBias && useDark);
        deBiasMasterDarkBox.setEnabled(useBias && (createDark || useDark));
        darkMasterDirField.setEnabled(createDark || useDark);
        darkMasterDirButton.setEnabled(createDark || useDark);
        darkMasterField.setEnabled(createDark || useDark);
        darkMasterButton.setEnabled(createDark || useDark);
        
        flatAverageRadio.setEnabled(createFlat);
        flatMedianRadio.setEnabled(createFlat);
        flatRawDirField.setEnabled(createFlat);
        flatBaseDirButton.setEnabled(createFlat);
        flatBaseField.setEnabled(createFlat);
        flatBaseButton.setEnabled(createFlat);
        
        gradientRemovalCB.setEnabled(createFlat);
        flatMasterDirField.setEnabled(createFlat || useFlat);
        flatMasterDirButton.setEnabled(createFlat || useFlat);
        flatMasterField.setEnabled(createFlat || useFlat);
        flatMasterButton.setEnabled(createFlat || useFlat);
        
        enableNLCBoxes = useNLC && ((!useBias && !useDark && !useFlat) || useBias);
        useNLCBox.setEnabled((!useBias && !useDark && !useFlat) || useBias);
        NLCLabel.setEnabled(enableNLCBoxes);
        coeffASpinner.setEnabled(enableNLCBoxes);
        coeffBLabel.setEnabled(enableNLCBoxes);
        coeffBSpinner.setEnabled(enableNLCBoxes);
        coeffCLabel.setEnabled(enableNLCBoxes);
        coeffCSpinner.setEnabled(enableNLCBoxes);
        coeffDLabel.setEnabled(enableNLCBoxes);
        coeffDSpinner.setEnabled(enableNLCBoxes);
        NLCFinalLabel.setEnabled(enableNLCBoxes);
        
        cosmicRemovalCB.setEnabled(useScienceProcessing);
        outlierRadiusLabel.setEnabled(useScienceProcessing && useCosmicRemoval);
        outlierRadiusSpinner.setEnabled(useScienceProcessing && useCosmicRemoval);
        outlierThresholdLabel.setEnabled(useScienceProcessing && useCosmicRemoval);        
        outlierThresholdSpinner.setEnabled(useScienceProcessing && useCosmicRemoval);
        removeBrightOutliersCB.setEnabled(useScienceProcessing && useCosmicRemoval);
        removeDarkOutliersCB.setEnabled(useScienceProcessing && useCosmicRemoval);
                
        calcAirmassBox.setEnabled(useScienceProcessing);
        plateSolveBox.setEnabled(useScienceProcessing);
        
        astrometrySetupButton.setEnabled(useScienceProcessing && plateSolve);
        fitsHeaderToolButton.setEnabled(useScienceProcessing && calcHeaders);
        displayCCButton.setEnabled(useScienceProcessing && calcHeaders);
        objectCoordinateSourceCombo.setEnabled(useScienceProcessing && calcHeaders);
        observatoryLocationSourceCombo.setEnabled(useScienceProcessing && calcHeaders);
        
        saveProcessedDataBox.setEnabled(useScienceProcessing);
        saveIntegerRadio.setEnabled(useScienceProcessing && saveProcessedData);
        saveFloatingPointRadio.setEnabled(useScienceProcessing && saveProcessedData);
        subDirLabel.setEnabled(useScienceProcessing && saveProcessedData);
        saveDirField.setEnabled(useScienceProcessing && saveProcessedData);
        suffixLabel.setEnabled(useScienceProcessing && saveProcessedData);
        saveSuffixField.setEnabled(useScienceProcessing && saveProcessedData);
        saveFormatLabel.setEnabled(useScienceProcessing && saveProcessedData);
        saveFormatField.setEnabled(useScienceProcessing && saveProcessedData);
        compressBox.setEnabled(useScienceProcessing && saveProcessedData);
        
        runMultiApertureBox.setEnabled(useScienceProcessing && showScience);
        saveImageBox.setEnabled(useScienceProcessing && showScience);
        saveImageTextField.setEnabled(useScienceProcessing && showScience && saveImage);
        saveImagePathButton.setEnabled(useScienceProcessing && showScience && saveImage); 
        runPreMacroBox.setEnabled(useScienceProcessing && showScience);
        preMacroText.setEnabled(useScienceProcessing && showScience && runPreMacro);
        preMacroButton.setEnabled(useScienceProcessing && showScience && runPreMacro);
        
        runMultiPlotBox.setEnabled(useScienceProcessing && showScience && runMultiAperture);
        savePlotBox.setEnabled(useScienceProcessing && showScience && runMultiAperture && runMultiPlot);
        savePlotTextField.setEnabled(useScienceProcessing && showScience && runMultiAperture && runMultiPlot && savePlot);
        savePlotPathButton.setEnabled(useScienceProcessing && showScience && runMultiAperture && runMultiPlot && savePlot); 
        runPostMacroBox.setEnabled(useScienceProcessing && showScience);
        postMacroText.setEnabled(useScienceProcessing && showScience && runPostMacro);
        postMacroButton.setEnabled(useScienceProcessing && showScience && runPostMacro);
        }

	/**
	 * Response to buttons being pushed.
	 */
	public void actionPerformed (ActionEvent ae)
		{
        Object source = ae.getSource();
//        getTextFields();
        if (source != pauseButton) countValidFiles();
        if (source == saveIntegerRadio)
                saveFloatingPoint = false;
        else if (source == saveFloatingPointRadio)
                saveFloatingPoint = true;
        else if (source == flatAverageRadio)
                flatMedian = false;
        else if (source == flatMedianRadio)
                flatMedian = true;
        else if (source == darkAverageRadio)
                darkMedian = false;
        else if (source == darkMedianRadio)
                darkMedian = true;
        else if (source == biasAverageRadio)
                biasMedian = false;
        else if (source == biasMedianRadio)
                biasMedian = true;
		else if(source == dirButton)
			{
			if (running)
				{
				IJ.beep();
				return;
				}
			if (mainDir.equals(""))
				mainDir = IJ.getDirectory ("home");

            DirectoryChooser.setDefaultDirectory(mainDir);
			DirectoryChooser od = new DirectoryChooser ("Select primary directory containing science files");

			if (od.getDirectory() != null)
				{
				mainDir = od.getDirectory();
				dirText.setText (mainDir);
                resetAndUpdate();
				}
			}
		else if(source == fileButton)
			{
			if (running)
				{
				IJ.beep();
				return;
				}
            filepath = mainDir;
			if (filepath.equals(""))
				filepath = IJ.getDirectory ("home");
            OpenDialog.setDefaultDirectory(filepath);
            OpenDialog of = new OpenDialog("Select a file", "");

			if (of.getDirectory() != null)
				{
				mainDir = of.getDirectory();
				dirText.setText (mainDir);
				}
			if (of.getFileName() != null)
				{
				filenamePattern = of.getFileName();
                if (autoWildcard)
                    {
                    filenamePattern = new StringBuffer(filenamePattern).reverse().toString().replaceFirst("\\..*?_", ".*_");
                    filenamePattern = new StringBuffer(filenamePattern).reverse().toString();
                    }
				filenamePatternText.setText (filenamePattern);
                }
            if (of.getDirectory() != null || of.getFileName() != null)
                {
                resetAndUpdate();
				}
			}
        else if(source == biasBaseDirButton)
			{
			if (running)
				{
				IJ.beep();
				return;
				}

            String startDir = getPath(biasRawDirField, rawCalCommonDir);
            File startDirectory = new File (startDir);
            if (startDirectory == null || !startDirectory.exists()) startDir = mainDir;
            else startDir = getCanonicalPath(startDirectory);
            String currentDir = getPath(biasRawDirField, false);
            File currentDirectory = new File (currentDir);
            if (currentDirectory == null || !currentDirectory.exists()) currentDir = "";
            else currentDir = getCanonicalPath(currentDirectory);
            DirectoryChooser.setDefaultDirectory(startDir);
			DirectoryChooser od = new DirectoryChooser ("Select directory containing raw bias files");
			if (od.getDirectory() != null)
				{
				biasRawDir = od.getDirectory();
                if (!biasRawDir.endsWith(slash)) biasRawDir += slash;
                if (!currentDir.endsWith(slash)) currentDir += slash;
                if (!currentDir.equals(biasRawDir)) biasRawDirField.setText(biasRawDir);
                resetAndUpdate();
				}
			}
		else if(source == biasBaseButton)
			{
			if (running)
				{
				IJ.beep();
				return;
				}
            String prevDirectory = OpenDialog.getDefaultDirectory();
            String startDir = getPath(biasRawDirField, rawCalCommonDir);
            File startDirectory = new File (startDir);
            if (startDirectory == null || !startDirectory.exists()) startDir = mainDir;
            else startDir = getCanonicalPath(startDirectory);
            String currentDir = getPath(biasRawDirField, false);
            File currentDirectory = new File (currentDir);
            if (currentDirectory == null || !currentDirectory.exists()) currentDir = "";
            else currentDir = getCanonicalPath(currentDirectory);
            OpenDialog.setDefaultDirectory(startDir);
            OpenDialog of = new OpenDialog("Select a bias file", "");

			if (of.getDirectory() != null)
				{
				biasRawDir = of.getDirectory();
                if (!biasRawDir.endsWith(slash)) biasRawDir += slash;
                if (!currentDir.endsWith(slash)) currentDir += slash;
                if (!currentDir.equals(biasRawDir)) biasRawDirField.setText(biasRawDir);
				}
			if (of.getFileName() != null)
				{
				biasBase = of.getFileName();
                if (autoWildcard)
                    {
                    biasBase = new StringBuffer(biasBase).reverse().toString().replaceFirst("\\..*?_", ".*_");
                    biasBase = new StringBuffer(biasBase).reverse().toString();
                    }
				biasBaseField.setText (biasBase);
                }
            if (of.getDirectory() != null || of.getFileName() != null)
                {
                resetAndUpdate();
				}
            OpenDialog.setDefaultDirectory(prevDirectory);
			}
        else if(source == biasMasterDirButton)
			{
			if (running)
				{
				IJ.beep();
				return;
				}

            String startDir = getPath(biasMasterDirField, masterCalCommonDir);
            File startDirectory = new File (startDir);
            if (startDirectory == null || !startDirectory.exists()) startDir = mainDir;
            else startDir = getCanonicalPath(startDirectory);
            String currentDir = getPath(biasMasterDirField, false);
            File currentDirectory = new File (currentDir);
            if (currentDirectory == null || !currentDirectory.exists()) currentDir = "";
            else currentDir = getCanonicalPath(currentDirectory);
            DirectoryChooser.setDefaultDirectory(startDir);
			DirectoryChooser od = new DirectoryChooser ("Select directory containing master bias file");
			if (od.getDirectory() != null)
				{
				biasMasterDir = od.getDirectory();
                if (!biasMasterDir.endsWith(slash)) biasMasterDir += slash;
                if (!currentDir.endsWith(slash)) currentDir += slash;
                if (!currentDir.equals(biasMasterDir)) biasMasterDirField.setText(biasMasterDir);
                resetAndUpdate();
				}
			}
		else if(source == biasMasterButton)
			{
			if (running)
				{
				IJ.beep();
				return;
				}
            String prevDirectory = OpenDialog.getDefaultDirectory();
            String startDir = getPath(biasMasterDirField, masterCalCommonDir);
            File startDirectory = new File (startDir);
            if (startDirectory == null || !startDirectory.exists()) startDir = mainDir;
            else startDir = getCanonicalPath(startDirectory);
            String currentDir = getPath(biasMasterDirField, false);
            File currentDirectory = new File (currentDir);
            if (currentDirectory == null || !currentDirectory.exists()) currentDir = "";
            else currentDir = getCanonicalPath(currentDirectory);
            OpenDialog.setDefaultDirectory(startDir);
            OpenDialog of = new OpenDialog("Select the master bias file", "");

			if (of.getDirectory() != null)
				{
				biasMasterDir = of.getDirectory();
                if (!biasMasterDir.endsWith(slash)) biasMasterDir += slash;
                if (!currentDir.endsWith(slash)) currentDir += slash;
                if (!currentDir.equals(biasMasterDir)) biasMasterDirField.setText(biasMasterDir);
				}
			if (of.getFileName() != null)
				{
				biasMaster = of.getFileName();
				biasMasterField.setText (biasMaster);
                }
            if (of.getDirectory() != null || of.getFileName() != null)
                {
                resetAndUpdate();
				}
            OpenDialog.setDefaultDirectory(prevDirectory);
			}
        else if(source == darkBaseDirButton)
			{
			if (running)
				{
				IJ.beep();
				return;
				}

            String startDir = getPath(darkRawDirField, rawCalCommonDir);
            File startDirectory = new File (startDir);
            if (startDirectory == null || !startDirectory.exists()) startDir = mainDir;
            else startDir = getCanonicalPath(startDirectory);
            String currentDir = getPath(darkRawDirField, false);
            File currentDirectory = new File (currentDir);
            if (currentDirectory == null || !currentDirectory.exists()) currentDir = "";
            else currentDir = getCanonicalPath(currentDirectory);
            DirectoryChooser.setDefaultDirectory(startDir);
			DirectoryChooser od = new DirectoryChooser ("Select directory containing raw dark files");
			if (od.getDirectory() != null)
				{
				darkRawDir = od.getDirectory();
                if (!darkRawDir.endsWith(slash)) darkRawDir += slash;
                if (!currentDir.endsWith(slash)) currentDir += slash;
                if (!currentDir.equals(darkRawDir)) darkRawDirField.setText(darkRawDir);
                resetAndUpdate();
				}
			}
		else if(source == darkBaseButton)
			{
			if (running)
				{
				IJ.beep();
				return;
				}
            String prevDirectory = OpenDialog.getDefaultDirectory();
            String startDir = getPath(darkRawDirField, rawCalCommonDir);
            File startDirectory = new File (startDir);
            if (startDirectory == null || !startDirectory.exists()) startDir = mainDir;
            else startDir = getCanonicalPath(startDirectory);
            String currentDir = getPath(darkRawDirField, false);
            File currentDirectory = new File (currentDir);
            if (currentDirectory == null || !currentDirectory.exists()) currentDir = "";
            else currentDir = getCanonicalPath(currentDirectory);
            OpenDialog.setDefaultDirectory(startDir);
            OpenDialog of = new OpenDialog("Select a dark file", "");

			if (of.getDirectory() != null)
				{
				darkRawDir = of.getDirectory();
                if (!darkRawDir.endsWith(slash)) darkRawDir += slash;
                if (!currentDir.endsWith(slash)) currentDir += slash;
                if (!currentDir.equals(darkRawDir)) darkRawDirField.setText(darkRawDir);
				}
			if (of.getFileName() != null)
				{
				darkBase = of.getFileName();
                if (autoWildcard)
                    {
                    darkBase = new StringBuffer(darkBase).reverse().toString().replaceFirst("\\..*?_", ".*_");
                    darkBase = new StringBuffer(darkBase).reverse().toString();
                    }
				darkBaseField.setText (darkBase);
                }
            if (of.getDirectory() != null || of.getFileName() != null)
                {
                resetAndUpdate();
				}
            OpenDialog.setDefaultDirectory(prevDirectory);
			}
        else if(source == darkMasterDirButton)
			{
			if (running)
				{
				IJ.beep();
				return;
				}

            String startDir = getPath(darkMasterDirField, masterCalCommonDir);
            File startDirectory = new File (startDir);
            if (startDirectory == null || !startDirectory.exists()) startDir = mainDir;
            else startDir = getCanonicalPath(startDirectory);
            String currentDir = getPath(darkMasterDirField, false);
            File currentDirectory = new File (currentDir);
            if (currentDirectory == null || !currentDirectory.exists()) currentDir = "";
            else currentDir = getCanonicalPath(currentDirectory);
            DirectoryChooser.setDefaultDirectory(startDir);
			DirectoryChooser od = new DirectoryChooser ("Select directory containing master dark file");
			if (od.getDirectory() != null)
				{
				darkMasterDir = od.getDirectory();
                if (!darkMasterDir.endsWith(slash)) darkMasterDir += slash;
                if (!currentDir.endsWith(slash)) currentDir += slash;
                if (!currentDir.equals(darkMasterDir)) darkMasterDirField.setText(darkMasterDir);
                resetAndUpdate();
				}
			}
		else if(source == darkMasterButton)
			{
			if (running)
				{
				IJ.beep();
				return;
				}
            String prevDirectory = OpenDialog.getDefaultDirectory();
            String startDir = getPath(darkMasterDirField, masterCalCommonDir);
            File startDirectory = new File (startDir);
            if (startDirectory == null || !startDirectory.exists()) startDir = mainDir;
            else startDir = getCanonicalPath(startDirectory);
            String currentDir = getPath(darkMasterDirField, false);
            File currentDirectory = new File (currentDir);
            if (currentDirectory == null || !currentDirectory.exists()) currentDir = "";
            else currentDir = getCanonicalPath(currentDirectory);
            OpenDialog.setDefaultDirectory(startDir);
            OpenDialog of = new OpenDialog("Select the master dark file", "");

			if (of.getDirectory() != null)
				{
				darkMasterDir = of.getDirectory();
                if (!darkMasterDir.endsWith(slash)) darkMasterDir += slash;
                if (!currentDir.endsWith(slash)) currentDir += slash;
                if (!currentDir.equals(darkMasterDir)) darkMasterDirField.setText(darkMasterDir);
				}
			if (of.getFileName() != null)
				{
				darkMaster = of.getFileName();
				darkMasterField.setText (darkMaster);
                }
            if (of.getDirectory() != null || of.getFileName() != null)
                {
                resetAndUpdate();
				}
            OpenDialog.setDefaultDirectory(prevDirectory);
			}

        else if(source == flatBaseDirButton)
			{
			if (running)
				{
				IJ.beep();
				return;
				}

            String startDir = getPath(flatRawDirField, rawCalCommonDir);
            File startDirectory = new File (startDir);
            if (startDirectory == null || !startDirectory.exists()) startDir = mainDir;
            else startDir = getCanonicalPath(startDirectory);
            String currentDir = getPath(flatRawDirField, false);
            File currentDirectory = new File (currentDir);
            if (currentDirectory == null || !currentDirectory.exists()) currentDir = "";
            else currentDir = getCanonicalPath(currentDirectory);
            DirectoryChooser.setDefaultDirectory(startDir);
			DirectoryChooser od = new DirectoryChooser ("Select directory containing raw flat files");
			if (od.getDirectory() != null)
				{
				flatRawDir = od.getDirectory();
                if (!flatRawDir.endsWith(slash)) flatRawDir += slash;
                if (!currentDir.endsWith(slash)) currentDir += slash;
                if (!currentDir.equals(flatRawDir)) flatRawDirField.setText(flatRawDir);
                resetAndUpdate();
				}
			}
		else if(source == flatBaseButton)
			{
			if (running)
				{
				IJ.beep();
				return;
				}
            String prevDirectory = OpenDialog.getDefaultDirectory();
            String startDir = getPath(flatRawDirField, rawCalCommonDir);
            File startDirectory = new File (startDir);
            if (startDirectory == null || !startDirectory.exists()) startDir = mainDir;
            else startDir = getCanonicalPath(startDirectory);
            String currentDir = getPath(flatRawDirField, false);
            File currentDirectory = new File (currentDir);
            if (currentDirectory == null || !currentDirectory.exists()) currentDir = "";
            else currentDir = getCanonicalPath(currentDirectory);
            OpenDialog.setDefaultDirectory(startDir);
            OpenDialog of = new OpenDialog("Select a flat file", "");

			if (of.getDirectory() != null)
				{
				flatRawDir = of.getDirectory();
                if (!flatRawDir.endsWith(slash)) flatRawDir += slash;
                if (!currentDir.endsWith(slash)) currentDir += slash;
                if (!currentDir.equals(flatRawDir)) flatRawDirField.setText(flatRawDir);
				}
			if (of.getFileName() != null)
				{
				flatBase = of.getFileName();
                if (autoWildcard)
                    {
                    flatBase = new StringBuffer(flatBase).reverse().toString().replaceFirst("\\..*?_", ".*_");
                    flatBase = new StringBuffer(flatBase).reverse().toString();
                    }
				flatBaseField.setText (flatBase);
                }
            if (of.getDirectory() != null || of.getFileName() != null)
                {
                resetAndUpdate();
				}
            OpenDialog.setDefaultDirectory(prevDirectory);
			}
        else if(source == flatMasterDirButton)
			{
			if (running)
				{
				IJ.beep();
				return;
				}

            String startDir = getPath(flatMasterDirField, masterCalCommonDir);
            File startDirectory = new File (startDir);
            if (startDirectory == null || !startDirectory.exists()) startDir = mainDir;
            else startDir = getCanonicalPath(startDirectory);
            String currentDir = getPath(flatMasterDirField, false);
            File currentDirectory = new File (currentDir);
            if (currentDirectory == null || !currentDirectory.exists()) currentDir = "";
            else currentDir = getCanonicalPath(currentDirectory);
            DirectoryChooser.setDefaultDirectory(startDir);
			DirectoryChooser od = new DirectoryChooser ("Select directory containing master flat file");
			if (od.getDirectory() != null)
				{
				flatMasterDir = od.getDirectory();
                if (!flatMasterDir.endsWith(slash)) flatMasterDir += slash;
                if (!currentDir.endsWith(slash)) currentDir += slash;
                if (!currentDir.equals(flatMasterDir)) flatMasterDirField.setText(flatMasterDir);
                resetAndUpdate();
				}
			}
		else if(source == flatMasterButton)
			{
			if (running)
				{
				IJ.beep();
				return;
				}
            String prevDirectory = OpenDialog.getDefaultDirectory();
            String startDir = getPath(flatMasterDirField, masterCalCommonDir);
            File startDirectory = new File (startDir);
            if (startDirectory == null || !startDirectory.exists()) startDir = mainDir;
            else startDir = getCanonicalPath(startDirectory);
            String currentDir = getPath(flatMasterDirField, false);
            File currentDirectory = new File (currentDir);
            if (currentDirectory == null || !currentDirectory.exists()) currentDir = "";
            else currentDir = getCanonicalPath(currentDirectory);
            OpenDialog.setDefaultDirectory(startDir);
            OpenDialog of = new OpenDialog("Select the master flat file", "");

			if (of.getDirectory() != null)
				{
				flatMasterDir = of.getDirectory();
                if (!flatMasterDir.endsWith(slash)) flatMasterDir += slash;
                if (!currentDir.endsWith(slash)) currentDir += slash;
                if (!currentDir.equals(flatMasterDir)) flatMasterDirField.setText(flatMasterDir);
				}
			if (of.getFileName() != null)
				{
				flatMaster = of.getFileName();
				flatMasterField.setText (flatMaster);
                }
            if (of.getDirectory() != null || of.getFileName() != null)
                {
                resetAndUpdate();
				}
            OpenDialog.setDefaultDirectory(prevDirectory);
			}
        else if(source == setApertureButton)
            {
            IJ.runPlugIn("Astronomy.Set_Aperture", "");
            }
        else if(source == changeAperturesButton)
            {
            changeApertures=true;
            }
        else if(source == clearTableButton)
            {
            if (MultiPlot_.mainFrame!=null)
                {
                MultiPlot_.clearPlot();
                }            
            if (MultiAperture_.table != null)
                {
                MultiAperture_.clearTable();
                }
            }
        else if(source == displayCCButton)
            {
            acc.showPanel(!acc.isShowing());
            }
        else if(source == fitsHeaderToolButton)
            {
            toggleFitsHeaderDisplay();
            }
        else if(source == astrometrySetupButton)
            {
            if (astrometrySetupButton.getIcon().equals(astrometrySetupIcon))
                {
                if (astrometrySetup == null )
                    {
                    astrometrySetupThread = new Thread()
                        {
                        public void run()
                            {
                            astrometrySetup = new AstrometrySetup();
                            astrometrySetup.start(1, 1, 1, "SAVE AND EXIT", acc, true);
                            astrometrySetup = null;
                            astrometrySetupThread.stop();
                            astrometrySetupThread = null;
                            }
                        };
                    astrometrySetupThread.start();  
                    }
                else
                    {
                    astrometrySetup.astrometrySetupFrame.setVisible(true);
                    }
                }
            else if (astrometry != null)
                {
                astrometryCanceledByUser = true;
                astrometry.setAstrometryCanceled(); 
                }
            }        

		else if(source == preMacroButton)
			{
			if (running)
				{
				IJ.beep();
				return;
				}
			if (preMacroPath.equals(""))
				preMacroPath = IJ.getDirectory ("home");
            int dirSize = preMacroPath.lastIndexOf(slash);
            OpenDialog.setDefaultDirectory(preMacroPath.substring(0, dirSize));
 			OpenDialog od = new OpenDialog ("Select Macro 1","");
			if (od.getDirectory() != null)
				{
				preMacroPath = od.getDirectory()+od.getFileName();
				preMacroText.setText (preMacroPath);
                countValidFiles();
				}
			}
		else if(source == postMacroButton)
			{
			if (running)
				{
				IJ.beep();
				return;
				}
			if (postMacroPath.equals(""))
				postMacroPath = IJ.getDirectory ("home");
            int dirSize = postMacroPath.lastIndexOf(slash);
            OpenDialog.setDefaultDirectory(postMacroPath.substring(0, dirSize));
 			OpenDialog od = new OpenDialog ("Select Macro 2","");
			if (od.getDirectory() != null)
				{
				postMacroPath = od.getDirectory()+od.getFileName();
				postMacroText.setText (postMacroPath);
                countValidFiles();
				}
			}
        else if(source == pauseButton)
			{
            pause();
			}
		else if(source == clearButton)
			{
			if (running)
				{
				IJ.beep();
				return;
				}
            else
                {
                Prefs.set("multiaperture.canceled",true);
                images.clear();
                firstRun = true;
                restart = true;
                loadNewCals = true;
                foundImages = 0;
                ignoredImages = 0;
                running = false;
                active = false;
                totalNumFilesInDir = 0;
                startButton.setText("START");
                startButton.setForeground(Color.black);
                startButton.repaint();
                countValidFiles();
                log(" ");
                log("************STOPPED BY USER************");
                log(" ");                
				}
			}
		else if(source == startButton)
			{
            if (running)
				{
				IJ.beep();
				return;
				}
            else if (astrometrySetupButton.isSelected()) 
                {
                IJ.beep();
                IJ.showMessage("Wait for plate solve to complete, or cancel plate solve\nby clicking the astrometry icon in the the 'FITS header Updates' section.");
                return;
                } 
            else
                {
                if (timer != null) timer.cancel();
                if (task != null) task.cancel();
                if (onlyNew && !startButton.getText().equals("CONTINUE")) firstRun=true;
                startButton.setText("RUNNING");
                startButton.setForeground(Color.red);
                startButton.repaint();
                length = 0;
                running = true;
                astrometryCanceledByUser = false;
                active = true;
                requestStop = false;
                startTimer();
                }
			}
        else if(source == observatoryLocationSourceCombo)
            {
            selectedObservatoryLocationSource = observatoryLocationSourceCombo.getSelectedIndex();
            oldObservatoryName = "";
            oldTargetName = "";
            if (selectedObservatoryLocationSource == 0)
                {
                acc.setEnableObservatoryEntry(true);
                }
            else
                {

                acc.setEnableObservatoryEntry(false);
                }
            }
        else if(source == objectCoordinateSourceCombo)
            {
            selectedObjectCoordinateSource = objectCoordinateSourceCombo.getSelectedIndex();
            oldObservatoryName = "";
            oldTargetName = "";
            if (selectedObjectCoordinateSource == 0)
                {
                acc.setEnableObjectEntry(true);
                }
            else
                {
                acc.setEnableObjectEntry(false);
                }
            }
        else if(source == saveImagePathButton)
            {
            updateImagePath();
            }        
        else if(source == savePlotPathButton)
            {
            updatePlotPath();
            }
		else if(source == exitMenuItem)
			{
            saveAndClose();
            }

		}

protected void resetAndUpdate()
    {
    images.clear();
    firstRun =true;
    restart = true;
    loadNewCals = true;
    foundImages = 0;
    ignoredImages = 0;
    totalNumFilesInDir = 0;
    changeApertures=true;
    countValidFiles();
    }

protected void updateImagePath()
    {
    String prevDirectory = OpenDialog.getDefaultDirectory();
    String imageDirName = "";
    String imageFileName = "";
    int lastSlash = saveImagePath.lastIndexOf(slash);
    if (lastSlash >= 0)
        {
        imageDirName = saveImagePath.substring(0, lastSlash);
        imageFileName = saveImagePath.substring(lastSlash+1);
        if (imageFileName == null || imageFileName.trim().equals("")) imageFileName = "DPimage.png";
        }
    else
        {
        imageDirName = prevDirectory;
        imageFileName = "DPimage.png";
        }

    OpenDialog.setDefaultDirectory(imageDirName);
    OpenDialog.setLastName(imageFileName);
    OpenDialog of = new OpenDialog("Select/enter image display file name (.jpg or .png)", imageDirName, imageFileName);
    if (of.getDirectory() == null || of.getFileName() == null)
        {
        OpenDialog.setDefaultDirectory(prevDirectory);
        return;
        }
    saveImagePath = of.getDirectory() + of.getFileName();
    if (!saveImagePath.toLowerCase().endsWith(".jpg") && !saveImagePath.toLowerCase().endsWith(".png"))
            {
            saveImagePath += ".png";
            }
    saveImageTextField.setText(saveImagePath);
    OpenDialog.setDefaultDirectory(prevDirectory);    
    }

protected void updatePlotPath()
    {
    String prevDirectory = OpenDialog.getDefaultDirectory();
    String plotDirName = "";
    String plotFileName = "";
    int lastSlash = savePlotPath.lastIndexOf(slash);
    if (lastSlash >= 0)
        {
        plotDirName = savePlotPath.substring(0, lastSlash);
        plotFileName = savePlotPath.substring(lastSlash+1);
        if (plotFileName == null || plotFileName.trim().equals("")) plotFileName = "DPplot.png";
        }
    else
        {
        plotDirName = prevDirectory;
        plotFileName = "DPplot.png";
        }

    OpenDialog.setDefaultDirectory(plotDirName);
    OpenDialog.setLastName(plotFileName);
    OpenDialog of = new OpenDialog("Select/enter file name to save plot (.jpg or .png)", plotDirName, plotFileName);
    if (of.getDirectory() == null || of.getFileName() == null)
        {
        OpenDialog.setDefaultDirectory(prevDirectory);
        return;
        }
    savePlotPath = of.getDirectory() + of.getFileName();
    if (!savePlotPath.toLowerCase().endsWith(".jpg") && !savePlotPath.toLowerCase().endsWith(".png"))
            {
            savePlotPath += ".png";
            }
    savePlotTextField.setText(savePlotPath);
    OpenDialog.setDefaultDirectory(prevDirectory);    
    }

protected String getPath(JTextField dirPathField, boolean useCommon) {
    String result = "";
    String dirPath = dirPathField.getText().trim();
    if (dirPath.equals("") || useCommon)
        result = mainDir;
    else if (dirPath.startsWith("."))
        result = mainDir+dirPath;
    else
        result = dirPath;
    if (!result.endsWith(slash)) result += slash;
    return result;
    }

protected String getCanonicalPath(File dirName)
    {
    String retval;
    try
        {
        retval = dirName.getCanonicalPath();
        }
    catch (IOException iox)
        {
        retval = mainDir;
        }
    return retval;
    }

void openDragAndDropFiles(java.io.File[] files)
        {
        if (files.length > 0 && files[0].isFile() && files[0].getName().endsWith(".apertures"))
            {
            OpenDialog.setDefaultDirectory(files[0].getParent());
            try {
                InputStream is = new BufferedInputStream(new FileInputStream(files[0].getCanonicalPath()));
                Prefs.ijPrefs.load(is);
                is.close();
                }
            catch (Exception e) {
                IJ.beep();
                IJ.showMessage("DragAndDrop: Error reading aperture file");
                }            
            } 
        }

void openDragAndDropFileNames(java.io.File[] files, JTextField dirTF, JTextField fileTF, boolean autoWild)
        {
        if (files.length > 0)
            {
            if (files[0].isFile())
                {
                dirTF.setText(files[0].getParent());
                if (autoWild)
                    {
                    String pattern = new StringBuffer(files[0].getName()).reverse().toString().replaceFirst("\\..*?_", ".*_");
                    fileTF.setText(new StringBuffer(pattern).reverse().toString());
                    }
                else
                    {
                    fileTF.setText(files[0].getName());
                    }
                getTextFields(null);
                countValidFiles();                
                }
            else if (files[0].isDirectory())
                {
                try
                    {
                    dirTF.setText(files[0].getCanonicalPath());
                    getTextFields(null);
                    countValidFiles();                    
                    }
                catch (Exception e) {}
                }
            } 
        }

public void propertyChange(PropertyChangeEvent e) {
    Object source = e.getSource();
//    if (source == raTextField)
//        {
//        ra = ((Number)raTextField.getValue()).doubleValue();
//        }
    }

protected MaskFormatter createFormatter(String s) {
    MaskFormatter formatter = null;
    try {
        formatter = new MaskFormatter(s);
        formatter.setOverwriteMode(true);
    } catch (java.text.ParseException exc) {
        System.err.println("formatter is bad: " + exc.getMessage());
        System.exit(-1);
    }
    return formatter;
    }


class thisDocumentListener implements DocumentListener
    {
    public void insertUpdate (DocumentEvent ev)
        {

        getTextFields(ev);
        countValidFiles();
        }
    public void removeUpdate (DocumentEvent ev)
        {

        getTextFields(ev);
        countValidFiles();
        }
    public void changedUpdate (DocumentEvent ev)
        {

        getTextFields(ev);
        countValidFiles();
        }

    }


/** Returns an ImageIcon, or null if the path was invalid. */
protected ImageIcon createImageIcon(String path, String description) {
    java.net.URL imgURL = getClass().getClassLoader().getResource(path);
    if (imgURL != null) {
        return new ImageIcon(imgURL, description);
    } else {
        log("Couldn't find icon file: " + path);
        return null;
    }
}


    void getTextFields(DocumentEvent ev)
        {
        //Document source = ev.getDocument();

//        if (source == dirText.getDocument())
//        else if (source == filenamePatternText.getDocument())

        mainDir = dirText.getText();
        oldFilenamePattern = filenamePattern;
        filenamePattern = filenamePatternText.getText();
        if (updateExclude && !filenamePattern.equals(oldFilenamePattern))
            {
            numPrefix = filenamePattern;
            numPrefixField.setText(numPrefix);
            }
        else
            {
            numPrefix = numPrefixField.getText();
            }
        preMacroPath = preMacroText.getText();
        postMacroPath = postMacroText.getText();
        savePlotPath = savePlotTextField.getText();
        saveImagePath = saveImageTextField.getText();
        biasRawDir = getPath(biasRawDirField, false);
        biasMasterDir = getPath(biasMasterDirField, false);
        biasBase = biasBaseField.getText();
        biasMaster = biasMasterField.getText();
        darkRawDir = getPath(darkRawDirField, false);
        darkMasterDir = getPath(darkMasterDirField, false);
        darkBase = darkBaseField.getText();
        darkMaster = darkMasterField.getText();
        flatRawDir = getPath(flatRawDirField, false);
        flatMasterDir = getPath(flatMasterDirField, false);
        flatBase = flatBaseField.getText();
        flatMaster = flatMasterField.getText();
        saveDir = saveDirField.getText();
        saveSuffix = saveSuffixField.getText();
        saveFormat = saveFormatField.getText();
        objectNameReadKeyword = objectNameReadKeywordTF.getText();
        objectRAJ2000ReadKeyword = objectRAJ2000ReadKeywordTF.getText();
        objectDecJ2000ReadKeyword = objectDecJ2000ReadKeywordTF.getText();
        observatoryNameReadKeyword = observatoryNameReadKeywordTF.getText();
        observatoryLatReadKeyword = observatoryLatReadKeywordTF.getText();
        observatoryLonReadKeyword = observatoryLonReadKeywordTF.getText();
        objectRAJ2000SaveKeyword = objectRAJ2000SaveKeywordTF.getText();
        objectDecJ2000SaveKeyword = objectDecJ2000SaveKeywordTF.getText();
        objectRAEODSaveKeyword = objectRAEODSaveKeywordTF.getText();
        objectDecEODSaveKeyword = objectDecEODSaveKeywordTF.getText();
        objectAltitudeSaveKeyword = objectAltitudeSaveKeywordTF.getText();
        objectAzimuthSaveKeyword = objectAzimuthSaveKeywordTF.getText();
        objectHourAngleSaveKeyword = objectHourAngleSaveKeywordTF.getText();
        objectZenithDistanceSaveKeyword = objectZenithDistanceSaveKeywordTF.getText();
        objectAirmassSaveKeyword = objectAirmassSaveKeywordTF.getText();
        JD_SOBSSaveKeyword = JD_SOBSSaveKeywordTF.getText();
        JD_MOBSSaveKeyword = JD_MOBSSaveKeywordTF.getText();
        HJD_MOBSSaveKeyword = HJD_MOBSSaveKeywordTF.getText();
        BJD_MOBSSaveKeyword = BJD_MOBSSaveKeywordTF.getText();
        observatoryLatSaveKeyword = observatoryLatSaveKeywordTF.getText();
        observatoryLonSaveKeyword = observatoryLonSaveKeywordTF.getText();

        loadNewCals = true;
        }

    double sexToDec(JTextField textField)
        {
        double value = 0.0;
        boolean isNegative = false;
        String text = textField.getText();
        String[] pieces = text.split("[0-9\\.]{1,}");
        if (pieces.length > 0 && pieces[0].endsWith("-")) isNegative = true;
        if (text.toUpperCase().contains("S")||text.toUpperCase().contains("W")) isNegative = !isNegative;
        pieces = text.replaceAll("[^0-9\\.]{1,}", " ").trim().split("[^0-9\\.]{1,}");
        try {if (pieces.length > 0) value += Double.parseDouble(pieces[0]);}
        catch (NumberFormatException e) { }
        try {if (pieces.length > 1) value += Double.parseDouble(pieces[1])/60.0;}
        catch (NumberFormatException e) { }
        try {if (pieces.length > 2) value += Double.parseDouble(pieces[2])/3600.0;}
        catch (NumberFormatException e) { }
        if (isNegative) value = -value;
        return value;
        }

	public String decToSex (double d, int fractionPlaces, Boolean showPlus)
		{
        DecimalFormat nf = new DecimalFormat();
        DecimalFormat nf2x =  new DecimalFormat();
		nf.setMinimumIntegerDigits(2);
        nf2x.setMinimumIntegerDigits(2);
        nf2x.setMinimumFractionDigits(0);
        nf2x.setMaximumFractionDigits(fractionPlaces);

		double dd = Math.abs(d);
        dd += 0.00000000000001;    //helps correct rounding issues when converting back to dms format

		int h = (int)dd;
		int m = (int)(60.0*(dd-(double)h));
		double s = 3600.0*(dd-(double)h-(double)m/60.0);

		String str = "";
		if (d < 0.0) str = "-";
        else if (showPlus) str = "+";
		str += ""+nf.format(h)+":"+nf.format(m)+":"+nf2x.format(s);
		return str;
		}


    boolean validateSelections()
        {
        mainDir = mainDir.trim();
        if (!mainDir.endsWith(slash))
            {
            mainDir += slash;
            dirText.setText(mainDir);
            }
        
        if (useScienceProcessing)
            {
            scienceDir = new File (mainDir);
            if (!scienceDir.isDirectory())
                {
                error("ERROR: Primary directory \""+mainDir+"\"not found.");
                return false;
                }
            }

        if (runPreMacro)
            {
            File preMacroFile = new File(preMacroText.getText().trim());
            if (!preMacroFile.isFile())
                {
                error("ERROR: Macro 1 \""+preMacroText.getText().trim()+"\" not found.");
                return false;
                }
            }

        if (runPostMacro)
            {
            File postMacroFile = new File(postMacroText.getText().trim());
            if (!postMacroFile.isFile())
                {
                error("ERROR: Macro 2 \""+postMacroText.getText().trim()+"\" not found.");
                return false;
                }
            }
        if (createBias)
            {
            biasPath = getPath(biasRawDirField, false);
            biasDirectory = new File (biasPath);
            if (!biasDirectory.isDirectory())
                {
                error("ERROR: Bias directory \""+biasPath+"\" not found.");
                return false;
                }
            }
        if (useBias && !createBias)
            {
            biasMasterPath = getPath(biasMasterDirField, false);
            mbiasPath = biasMasterPath + biasMaster.trim();
            biasMasterFile = new File (mbiasPath);
            if (!biasMasterFile.isFile())
                {
                error("ERROR: Master bias file \""+mbiasPath+"\" not found.");
                return false;
                }
            }
        if (createDark)
            {
            darkPath = getPath(darkRawDirField, false);
            darkDirectory = new File (darkPath);
            if (!darkDirectory.isDirectory())
                {
                error("ERROR: Dark directory \""+darkPath+"\" not found.");
                return false;
                }
            }
        if (useDark && !createDark)
            {
            darkMasterPath = getPath(darkMasterDirField, false);
            mdarkPath = darkMasterPath + darkMaster.trim();
            darkMasterFile = new File (mdarkPath);
            if (!darkMasterFile.isFile())
                {
                error("ERROR: Master dark file \""+mdarkPath+"\" not found.");
                return false;
                }
            }
        if (createFlat)
            {
            flatPath = getPath(flatRawDirField, false);
            if (!flatPath.endsWith(slash)) flatPath += slash;
            flatDirectory = new File (flatPath);
            if (!flatDirectory.isDirectory())
                {
                error("ERROR: Flat directory \""+flatPath+"\" not found.");
                return false;
                }
            }
        if (useFlat && !createFlat)
            {
            flatMasterPath = getPath(flatMasterDirField, false);
            mflatPath = flatMasterPath + flatMaster.trim();
            flatMasterFile = new File (mflatPath);
            if (!flatMasterFile.isFile())
                {
                error("ERROR: Master flat file \""+mflatPath+"\" not found.");
                return false;
                }
            }
        return true;
        }

    boolean createMasterBias()
        {
        ImageStack stack=null;
        ImagePlus imp=null, imp2=null;
        ImageProcessor mbiasIp;
        ImageProcessor[] slices = null;
        try
            {
            String bias;
//            double min = 0, max = 0;
            int count = 0;
            biasMasterPath = getPath(biasMasterDirField, false);
            mbiasPath = biasMasterPath + biasMaster.trim();
            biasPath = getPath(biasRawDirField, false);
            biasDirectory = new File (biasPath);
            if (!biasDirectory.isDirectory())
                {
                error("ERROR: Bias directory \""+biasPath+"\" not found.");
                return false;
                }
            File[] files = biasDirectory.listFiles ();
            String[] mbiasHeader = null;
            String[] filenames = new String[files.length];
            String[] validFilenames = new String[files.length];

            for (int i=0; i < files.length; i++)
                    {
                    filenames[i] = files[i].getName();
                    }

            if (sortNumerically) {filenames=sortFileList(filenames);}

            for (int i=0; i < files.length; i++)
                {
                bias = filenames[i];
                if (matchWildCard(biasBase, bias))
                    {
                    if (requestStop) {return false;}
                    count++;
                    if (useBeep) {IJ.beep();}
                    biasFilePath = biasPath+bias;
                    log("Loading raw bias file \""+biasFilePath+"\" ("+count+" of "+validBiasFiles+")");
                    imp = IJ.openImage(biasFilePath);
                    if (imp == null)
                        {
                        error("ERROR: Unable to open image \""+biasFilePath+"\".");
                        return false;
                        }
                    else
                        {
                        imp.setProcessor(getAdjustedIp(imp, biasFilePath, removePedestal, !CONVERTTOFLOAT));
                        if (count == 1) mbiasHeader = FitsJ.getHeader(imp);
                        if (stack==null)stack = imp.createEmptyStack();
                        
                        ImageProcessor ip = imp.getProcessor();
//                        if (ip.getMin()<min) min = ip.getMin();
//                        if (ip.getMax()>max) max = ip.getMax();
                        stack.addSlice(bias, ip);
                        FitsJ.putHeader(stack, FitsJ.getHeader(imp), count);

                        IJ.showStatus((stack.getSize()+1) + ": " + bias);
                        }
                    validFilenames[count-1] = filenames[i];
                    }
                }
            if (count == 0)
                {
                error("ERROR: No raw bias image matching \""+biasPath+biasBase.trim()+"\".");
                return false;
                }
            if (stack!=null)
                {
                imp2 = WindowManager.getImage("Bias Stack");
                if (imp2 != null) imp2.close();
                imp2 = new ImagePlus("Bias Stack", stack);
                imp2.setCalibration(imp.getCalibration());
//                if (imp2.getBitDepth()==16 || imp.getBitDepth()==32)
//                    imp2.getProcessor().setMinAndMax(min, max);
                if (showRawCals) imp2.show();


    //            stack = imp2.getStack();
                int sliceCount = stack.getSize();
                slices = new ImageProcessor[sliceCount];
                for (int i=0; i<sliceCount; i++)
                    slices[i] = stack.getProcessor(i+1);
                mbiasIp = slices[0].duplicate();
                mbiasIp = mbiasIp.convertToFloat();

                int width = mbiasIp.getWidth();
                int height = mbiasIp.getHeight();
                int inc = Math.max(height/30, 1);
                if (requestStop) {return false;}
                if (biasMedian)
                    {
                    IJ.showStatus("Calculating median...");
                    mbiasHeader = FitsJ.addHistory("Median master bias created from "+count+" images", mbiasHeader);
                    log("Calculating median master bias image \""+mbiasPath+"\".");
                    float[] values = new float[sliceCount];
                    for (int y=0; y<height; y++)
                        {
                        if (y%inc==0) {IJ.showProgress(y, height-1);}
                        for (int x=0; x<width; x++)
                            {
                            for (int i=0; i<sliceCount; i++){
                                values[i] = slices[i].getPixelValue(x, y);}
                            java.util.Arrays.sort(values);
                            if ((sliceCount % 2) == 0)      // even
                                {mbiasIp.putPixelValue(x, y, (values[sliceCount / 2] + values[sliceCount / 2 - 1]) / 2.0);}
                            else                        //odd
                                {mbiasIp.putPixelValue(x, y, values[sliceCount / 2]);}
                            }

                        }
                    }
                else
                    {

                    IJ.showStatus("Calculating average...");
                    mbiasHeader = FitsJ.addHistory("Average master bias created from "+count+" images", mbiasHeader);
                    log("Calculating average master bias image \""+mbiasPath+"\".");
                    double value;
                    for (int y=0; y<height; y++)
                        {
                        if (y%inc==0) IJ.showProgress(y, height-1);
                        for (int x=0; x<width; x++)
                            {
                            value = 0.0;
                            for (int i=0; i<sliceCount; i++)
                                value += slices[i].getPixelValue(x, y);
                            mbiasIp.putPixelValue(x, y, (float)(value / (double)sliceCount));
                            }
                        }
                    }

//                mbiasImp = WindowManager.getImage(biasMaster.trim());
//                if (mbiasImp != null) mbiasImp.close();
                mbiasImp = new ImagePlus(biasMaster.trim(), mbiasIp);
                mbiasHeader = FitsJ.addHistory("on "+f.format(new java.util.Date())+" (YYYY-MM-DD hh:mm:ss UT)", mbiasHeader);
                for (int i=0; i<count; i++)
                    mbiasHeader = FitsJ.addHistory("Image "+(i+1)+" = "+biasPath+validFilenames[i], mbiasHeader);
    //            mbiasHeader = FitsJ.removeCards("DATE", mbiasHeader);
    //            mbiasHeader = FitsJ.setCard("DATE", f.format(new java.util.Date()), "file creation date (YYYY-MM-DD hh:mm:ss UT)", mbiasHeader);

                FitsJ.putHeader(mbiasImp, mbiasHeader);
                if (showMasters) IJU.replaceImageInWindow(mbiasImp.duplicate(), "DUP_"+biasMaster.trim());
                if (requestStop) {return false;}
                saveProcessedFile(mbiasImp, biasMasterPath, mbiasPath, "master bias", "");
                }
            else
                {
                error("ERROR: Bias stack is empty.");
                return false;
                }
            countValidFiles();
            }
        catch (OutOfMemoryError e)
            {
            IJ.outOfMemory("Bias Stack");
            log("ERROR: Out of memory");
            return false;
            }
        return true;
        }

    boolean createMasterDark()
        {
        ImageStack stack=null;
        ImagePlus imp=null, imp2=null;
        ImageProcessor mdarkIp;
        ImageProcessor mbiasIp = null;
        ImageProcessor[] slices = null;
        try
            {
            String dark;
//            double min = 0, max = 0;
            int count = 0;
            darkMasterPath = getPath(darkMasterDirField, false);
            mdarkPath = darkMasterPath + darkMaster.trim();   
            darkPath = getPath(darkRawDirField, false);
            darkDirectory = new File (darkPath);
            if (!darkDirectory.isDirectory())
                {
                error("ERROR: Dark directory \""+darkPath+"\" not found.");
                return false;
                }
            File[] files = darkDirectory.listFiles ();
            String[] mdarkHeader = null;
            String[] filenames = new String[files.length];
            String[] validFilenames = new String[files.length];

            if (useBias && !loadMasterBiasFile())
                {
                return false;
                }
            if (useBias) mbiasIp = mbiasImp.getProcessor();
            for (int i=0; i < files.length; i++)
                    {
                    filenames[i] = files[i].getName();
                    }

            if (sortNumerically) filenames=sortFileList(filenames);

            for (int i=0; i < files.length; i++)
                {
                dark = filenames[i];
                if (matchWildCard(darkBase, dark))
                    {
                    if (requestStop) {return false;}
                    count++;
                    if (useBeep) IJ.beep();
                    darkFilePath = darkPath+dark;
                    log("Loading dark file \""+darkFilePath+"\" ("+count+" of "+validDarkFiles+")");
                    imp = IJ.openImage(darkFilePath);
                    if (imp == null)
                        {
                        error("ERROR: Unable to open image \""+darkFilePath+"\".");
                        return false;
                        }
                    else
                        {
                        imp.setProcessor(getAdjustedIp(imp, darkFilePath, removePedestal, !CONVERTTOFLOAT));
                        if (count == 1) mdarkHeader = FitsJ.getHeader(imp);
                        if (stack==null) stack = imp.createEmptyStack();
                        ImageProcessor ip = imp.getProcessor();
//                        if (ip.getMin()<min) min = ip.getMin();
//                        if (ip.getMax()>max) max = ip.getMax();
                        stack.addSlice(dark, ip);
                        FitsJ.putHeader(stack, FitsJ.getHeader(imp), count);

                        IJ.showStatus((stack.getSize()+1) + ": " + dark);

                        }
                    validFilenames[count-1] = filenames[i];
                    }
                }
            if (count == 0)
                {
                error("ERROR: No raw dark image matching \""+darkPath+darkBase.trim()+"\".");
                return false;
                }
            if (stack!=null)
                {
                imp2 = WindowManager.getImage("Dark Stack");
                if (imp2 != null) imp2.close();
                imp2 = new ImagePlus("Dark Stack", stack);
                imp2.setCalibration(imp.getCalibration());
//                if (imp2.getBitDepth()==16 || imp.getBitDepth()==32)
//                    imp2.getProcessor().setMinAndMax(min, max);
                if (showRawCals) imp2.show();


    //            stack = imp2.getStack();
                int sliceCount = stack.getSize();
                slices = new ImageProcessor[sliceCount];
                for (int i=0; i<sliceCount; i++)
                    slices[i] = stack.getProcessor(i+1);
                mdarkIp = slices[0].duplicate();
                mdarkIp = mdarkIp.convertToFloat();

                int width = mdarkIp.getWidth();
                int height = mdarkIp.getHeight();
                int inc = Math.max(height/30, 1);
                if (requestStop) {return false;}
                if (darkMedian)
                    {
                    IJ.showStatus("Calculating median...");
                    mdarkHeader = FitsJ.addHistory("Median master dark created from "+count+" images", mdarkHeader);
                    log("Calculating median master dark image \""+mdarkPath+"\".");
                    double[] values = new double[sliceCount];
                    double v2 = 0;
                    for (int y=0; y<height; y++)
                        {
                        if (y%inc==0) IJ.showProgress(y, height-1);
                        for (int x=0; x<width; x++)
                            {
                            for (int i=0; i<sliceCount; i++)
                                {
                                if (!useBias)
                                    {
                                    values[i] = (double)slices[i].getPixelValue(x, y);
                                    }
                                else if (useBias && useNLC)
                                    {
                                    values[i] = (double)slices[i].getPixelValue(x, y) - (double)mbiasIp.getPixelValue(x, y);
                                    if (values[i] > 0.0)
                                        {
                                        v2 = values[i] * values[i];
                                        values[i] = coeffA + values[i]*coeffB + v2*coeffC + v2*values[i]*coeffD;
                                        }
                                    if (!deBiasMasterDark) values[i] += (double)mbiasIp.getPixelValue(x, y); 
                                    }
                                else //useBias && !useNLC
                                    {
                                    if (deBiasMasterDark) 
                                        values[i] = (double)slices[i].getPixelValue(x, y) - (double)mbiasIp.getPixelValue(x, y);
                                    else 
                                        values[i] = (double)slices[i].getPixelValue(x, y);
                                    }
                                }
                            java.util.Arrays.sort(values);
                            if ((sliceCount % 2) == 0)      // even
                                mdarkIp.putPixelValue(x, y, (float)((values[sliceCount/2] + values[sliceCount/2 - 1]) / 2.0));
                            else                        //odd
                                mdarkIp.putPixelValue(x, y, (float)values[sliceCount/2]);
                            }

                        }
                    }
                else
                    {
                    IJ.showStatus("Calculating average...");
                    mdarkHeader = FitsJ.addHistory("Average master dark created from "+count+" images", mdarkHeader);
                    log("Calculating average master dark image \""+mdarkPath+"\".");
                    double value, pixel, p2;
                    for (int y=0; y<height; y++)
                        {
                        if (y%inc==0) IJ.showProgress(y, height-1);
                        for (int x=0; x<width; x++)
                            {
                            value = 0.0;
                            for (int i=0; i<sliceCount; i++)
                                {
                                if (!useBias)
                                    {
                                    value += (double)(slices[i].getPixelValue(x, y));
                                    }
                                else if(useBias && useNLC)
                                    {
                                    pixel = (double)slices[i].getPixelValue(x, y) - (double)mbiasIp.getPixelValue(x, y);
                                    if (pixel > 0.0)
                                        {
                                        p2 = pixel * pixel;
                                        value += coeffA + pixel*coeffB + p2*coeffC + p2*pixel*coeffD;
                                        }
                                    if (!deBiasMasterDark) value += (double)mbiasIp.getPixelValue(x, y); 
                                    }
                                else //useBias && !useNLC
                                    {
                                    if (deBiasMasterDark)
                                        value += (double)slices[i].getPixelValue(x, y) - (double)mbiasIp.getPixelValue(x, y); 
                                    else
                                        value += (double)(slices[i].getPixelValue(x, y));
                                    }
                                }
                            mdarkIp.putPixelValue(x, y, (float)(value / (double)sliceCount));
                            }
                        }
                    }

//                mdarkImp = WindowManager.getImage(darkMaster.trim());
//                if (mdarkImp != null) mdarkImp.close();
                mdarkImp = new ImagePlus(darkMaster.trim(), mdarkIp);
                mdarkHeader = FitsJ.addHistory("on "+f.format(new java.util.Date())+" (YYYY-MM-DD hh:mm:ss UT)", mdarkHeader);
                if (useBias && deBiasMasterDark)
                    {
                    mdarkHeader = FitsJ.addHistory("Bias corrected with "+biasMaster.trim(), mdarkHeader);
                    log("    Bias corrected with "+biasMaster.trim());
                    }
                if (useNLC & useBias)
                    {
                    mdarkHeader = FitsJ.addHistory("Non-linear corrected with coefficients:", mdarkHeader);
                    mdarkHeader = FitsJ.addHistory("a0 = "+coeffA, mdarkHeader);
                    mdarkHeader = FitsJ.addHistory("a1 = "+coeffB, mdarkHeader);
                    mdarkHeader = FitsJ.addHistory("a2 = "+coeffC, mdarkHeader);
                    mdarkHeader = FitsJ.addHistory("a3 = "+coeffD, mdarkHeader);
                    if (!deBiasMasterDark) mdarkHeader = FitsJ.addHistory("using master bias "+biasMaster.trim(), mdarkHeader);
                    log("    Non-linear corrected with coefficients: a0="+coeffA+" a1="+coeffB+" a2="+coeffC+" a3="+coeffD);
                    if (!deBiasMasterDark) log("        using master bias "+biasMaster.trim());
                    }
                for (int i=0; i<count; i++)
                    mdarkHeader = FitsJ.addHistory("Image "+(i+1)+" = "+darkPath+validFilenames[i], mdarkHeader);
    //            mdarkHeader = FitsJ.removeCards("DATE", mdarkHeader);
    //            mdarkHeader = FitsJ.setCard("DATE", f.format(new java.util.Date()), "file creation date (YYYY-MM-DD hh:mm:ss UT)", mdarkHeader);
                FitsJ.putHeader(mdarkImp, mdarkHeader);

                if (showMasters) IJU.replaceImageInWindow(mdarkImp.duplicate(), "DUP_"+darkMaster.trim());
                if (requestStop) {return false;}
                saveProcessedFile(mdarkImp, darkMasterPath, mdarkPath, "master dark", "");
                }
            else
                {
                error("ERROR: Dark stack is empty.");
                return false;
                }
            countValidFiles();
            }
        catch (OutOfMemoryError e)
            {
            IJ.outOfMemory("Dark Stack");
            log("ERROR: Out of memory");
            return false;
            }
        return true;
        }

    boolean createMasterFlat()
        {
        ImageStack stack=null;
        ImagePlus imp=null, imp2=null;
        ImageProcessor[] slices = null;
        ImageProcessor mbiasIp = null;
        ImageProcessor mdarkIp = null;
        ImageProcessor mflatIp = null;
        try
            {
            String flat;
//            double min = 0, max = 0;
            int count = 0;
            flatMasterPath = getPath(flatMasterDirField, false);
            mflatPath = flatMasterPath + flatMaster.trim();
            flatPath = getPath(flatRawDirField, false);
            flatDirectory = new File (flatPath);
            if (!flatDirectory.isDirectory())
                {
                error("ERROR: Flat directory \""+flatPath+"\" not found.");
                return false;
                }
            File[] files = flatDirectory.listFiles ();
            String[] mflatHeader = null;
            String[] fHeader = null;
            String[] dHeader = null;
            String[] filenames = new String[files.length];
            String[] validFilenames = new String[files.length];
            double[] expTimeFactor = new double[files.length];
            double dExpTime = 1;
            double fExpTime = 1;

            if (useBias && !loadMasterBiasFile())
                {
                return false;
                }
            if (useBias) mbiasIp = mbiasImp.getProcessor();
            if (requestStop) {return false;}
            if (useDark && !loadMasterDarkFile())
                {
                return false;
                }
            if (useDark) mdarkIp = mdarkImp.getProcessor();
            if (requestStop) {return false;}
            if (scaleExpTime && useDark)
                {
                dHeader = FitsJ.getHeader(mdarkImp);
                if (dHeader == null)
                    {
                    error("ERROR: Cannot extract FITS header from master dark image \""+mdarkPath+"\".");
                    return false;
                    }
                dExpTime = (double)FitsJ.getExposureTime(dHeader);
                if (Double.isNaN(dExpTime))
                    {
                    error("ERROR: Cannot extract exposure time from \""+mdarkPath+"\" master dark header!");
                    return false;
                    }
                }

            for (int i=0; i < files.length; i++)
                    {
                    filenames[i] = files[i].getName();
                    }

            if (sortNumerically) filenames=sortFileList(filenames);

            for (int i=0; i < files.length; i++)
                {
                flat = filenames[i];
                expTimeFactor[i] = 1;
                if (matchWildCard(flatBase, flat))
                    {
                    if (requestStop) {return false;}
                    count++;
                    if (useBeep) IJ.beep();
                    flatFilePath = flatPath+flat;
                    log("Loading flat file \""+flatFilePath+"\" ("+count+" of "+validFlatFiles+")");
                    imp = IJ.openImage(flatFilePath);
                    if (imp == null)
                        {
                        error("ERROR: Unable to open image \""+flatFilePath+"\".");
                        return false;
                        }
                    else
                        {
                        imp.setProcessor(getAdjustedIp(imp, flatFilePath, removePedestal, !CONVERTTOFLOAT));
                        if (count == 1) mflatHeader = FitsJ.getHeader(imp);
                        if (scaleExpTime && useBias && useDark)
                            {
                            fHeader = FitsJ.getHeader(imp);
                            if (fHeader == null)
                                {
                                error("ERROR: Cannot extract FITS header from flat image \""+flatFilePath+"\".");
                                return false;
                                }
                            fExpTime = (double)FitsJ.getExposureTime(fHeader);
                            if (Double.isNaN(fExpTime))
                                {
                                error("ERROR: Cannot extract exposure time from flat image header!");
                                return false;
                                }
                            expTimeFactor[count-1] = fExpTime/dExpTime;
                            }

                        if (stack==null) stack = imp.createEmptyStack();

                        ImageProcessor ip = imp.getProcessor();
                        ip = ip.convertToFloat();
//                        if (ip.getMin()<min) min = ip.getMin();
//                        if (ip.getMax()>max) max = ip.getMax();
                        stack.addSlice(flat, ip);
                        FitsJ.putHeader(stack, FitsJ.getHeader(imp), count);

                        IJ.showStatus((stack.getSize()+1) + ": " + flat);

                        }
                    validFilenames[count-1] = filenames[i];
                    }
                }
            if (count == 0)
                {
                error("ERROR: No raw flat image matching \""+flatPath+flatBase.trim()+"\".");
                return false;
                }
            if (stack!=null)
                {
                imp2 = WindowManager.getImage("Flat Stack");
                if (imp2 != null) imp2.close();
                imp2 = new ImagePlus("Flat Stack", stack);
                imp2.setCalibration(imp.getCalibration());
//                if (imp2.getBitDepth()==16 || imp.getBitDepth()==32)
//                    imp2.getProcessor().setMinAndMax(min, max);
                if (showRawCals) imp2.show();


    //            stack = imp2.getStack();
                int sliceCount = stack.getSize();
                slices = new ImageProcessor[sliceCount];
                for (int i=0; i<sliceCount; i++)
                    slices[i] = stack.getProcessor(i+1);
                mflatIp = slices[0].duplicate();
                mflatIp = mflatIp.convertToFloat();

                int width = mflatIp.getWidth();
                int height = mflatIp.getHeight();
                int inc = Math.max(height/30, 1);

                if (useBias || useDark)
                    {
                    String operations = "";
                    if (useBias) operations = "Bias";
                    if (useBias && useNLC)
                        {
                        if (useDark) operations += ", Nonlinearity,";
                        else operations += " and Nonlinearity ";
                        }
                    if (useBias && useDark) operations += " and ";
                    if (useBias && !useNLC && !useDark) operations += " ";
                    if (useDark) operations += "Dark ";
                    operations += "Correction on:";
                    IJ.showStatus(""+operations);
                    log("Performing "+ operations);
                    double value, v2;
                    for (int i=0; i<sliceCount; i++)
                        {
                        log("    "+validFilenames[i]+" using dark exposure scaling factor = "
                                +uptoFourPlaces.format(expTimeFactor[i]));
                        IJ.showProgress(i, sliceCount-1);
                        for (int y=0; y<height; y++)
                            {
                            for (int x=0; x<width; x++)
                                {
                                value = (double)(slices[i].getPixelValue(x, y));
                                if (useBias) value -= (double)mbiasIp.getPixelValue(x, y);
                                if (useBias && useNLC)
                                    {
                                    v2 = value*value;
                                    value = coeffA + value*coeffB + v2*coeffC + v2*value*coeffD;
                                    }
                                if (useDark) value -= (double)mdarkIp.getPixelValue(x, y)*expTimeFactor[i];
                                slices[i].putPixelValue(x, y, value);
                                }

                            }
                        }
                    }
                if (showRawCals) imp2.updateAndDraw();
                if (requestStop) {return false;}
                if (useGradientRemoval)
                    {
                    IJ.showStatus("Removing gradients ...");
                    for (int i=0; i<sliceCount; i++)
                        {
                        log("Removing gradient from calibrated flat "+validFilenames[i]+".");
                        IJ.showProgress(i, sliceCount-1);
                        slices[i] = removeGradient(slices[i]);
                        }
                    }
                if (showRawCals) imp2.updateAndDraw();
                if (requestStop) {return false;}
                IJ.showStatus("Normalizing images ...");
                for (int i=0; i<sliceCount; i++)
                    {
                    IJ.showProgress(i, sliceCount-1);
                    slices[i] = normalizeImage(slices[i], validFilenames[i]);
                    if (slices[i] == null)
                        {
                        error("ERROR: image mean value = 0 for "+validFilenames[i]+". Divide Error. Normalize Failed.");
                        return false;
                        }
                    }
                if (showRawCals) imp2.updateAndDraw();
                if (requestStop) {return false;}
                if (flatMedian)
                    {
                    IJ.showStatus("Calculating median...");
                    mflatHeader = FitsJ.addHistory("Median master flat created from "+count+" images", mflatHeader);
                    log("Calculating median master flat image \""+mflatPath+"\".");
                    double[] values = new double[sliceCount];
                    for (int y=0; y<height; y++)
                        {
                        if (y%inc==0) IJ.showProgress(y, height-1);
                        for (int x=0; x<width; x++)
                            {
                            for (int i=0; i<sliceCount; i++)
                                values[i] = (double)slices[i].getPixelValue(x, y);
                            java.util.Arrays.sort(values);
                            if ((sliceCount % 2) == 0)      // even
                                mflatIp.putPixelValue(x, y, (float)((values[sliceCount/2] + values[sliceCount/2 - 1]) / 2.0));
                            else                        //odd
                                mflatIp.putPixelValue(x, y, (float)values[sliceCount/2]);
                            }

                        }
                    }
                else
                    {
                    IJ.showStatus("Calculating average...");
                    mflatHeader = FitsJ.addHistory("Average master flat created from "+count+" images", mflatHeader);
                    log("Calculating average master flat image \""+mflatPath+"\".");
                    double value;
                    for (int y=0; y<height; y++)
                        {
                        if (y%inc==0) IJ.showProgress(y, height-1);
                        for (int x=0; x<width; x++)
                            {
                            value = 0.0;
                            for (int i=0; i<sliceCount; i++)
                                value += (double)(slices[i].getPixelValue(x, y));
                            mflatIp.putPixelValue(x, y, (float)(value / (double)sliceCount));
                            }
                        }
                    }

                mflatImp = WindowManager.getImage(flatMaster.trim());
                if (mflatImp != null) mflatImp.close();
                mflatImp = new ImagePlus(flatMaster.trim(), mflatIp);
                mflatHeader = FitsJ.addHistory("on "+f.format(new java.util.Date())+" (YYYY-MM-DD hh:mm:ss UT)", mflatHeader);
                if (useBias)
                    {
                    mflatHeader = FitsJ.addHistory("Bias corrected with "+biasMaster.trim(), mflatHeader);
                    log("    Bias corrected with "+biasMaster.trim());
                    }
                if (useNLC & useBias)
                    {
                    mflatHeader = FitsJ.addHistory("Non-linear corrected with coefficients:", mflatHeader);
                    mflatHeader = FitsJ.addHistory("a0 = "+coeffA, mflatHeader);
                    mflatHeader = FitsJ.addHistory("a1 = "+coeffB, mflatHeader);
                    mflatHeader = FitsJ.addHistory("a2 = "+coeffC, mflatHeader);
                    mflatHeader = FitsJ.addHistory("a3 = "+coeffD, mflatHeader);
                    log("    Non-linear corrected with coefficients: a0="+coeffA+" a1="+coeffB+" a2="+coeffC+" a3="+coeffD);
                    }
                if (useDark)
                    {
                    if (scaleExpTime && useBias)
                        {
                        mflatHeader = FitsJ.addHistory("Dark corrected with "+(!deBiasMasterDark?"(deBiased) ":"")+darkMaster.trim()+" with exposure time scaling", mflatHeader);
                        log("    Dark corrected with "+(!deBiasMasterDark?"(deBiased) ":"")+darkMaster.trim()+" with exposure time scaling");
                        }
                    else
                        {
                        mflatHeader = FitsJ.addHistory("Dark corrected with "+(useBias && !deBiasMasterDark?"(deBiased) ":"")+darkMaster.trim(), mflatHeader);
                        log("    Dark corrected with "+(useBias && !deBiasMasterDark?"(deBiased) ":"")+darkMaster.trim());
                        }
                    }
                if (useGradientRemoval)
                    {
                    mflatHeader = FitsJ.addHistory("Gradient removed from calibrated flats", mflatHeader);
                    log("    Gradient removed from calibrated flats");
                    }

                mflatHeader = FitsJ.addHistory("Normalized calibrated flats", mflatHeader);
                log("    Normalized calibrated flats");

                for (int i=0; i<count; i++)
                    mflatHeader = FitsJ.addHistory("Image "+(i+1)+" = "+flatPath+validFilenames[i], mflatHeader);
    //            mflatHeader = FitsJ.removeCards("DATE", mflatHeader);
    //            mflatHeader = FitsJ.setCard("DATE", f.format(new java.util.Date()), "file creation date (YYYY-MM-DD hh:mm:ss UT)", mflatHeader);
                FitsJ.putHeader(mflatImp, mflatHeader);
                IJ.showProgress(1.0);
                if (showMasters) IJU.replaceImageInWindow(mflatImp.duplicate(), "DUP_"+flatMaster.trim());
                if (requestStop) {return false;}
                saveProcessedFile(mflatImp, flatMasterPath, mflatPath, "master flat", "");
                }
            else
                {
                error("ERROR: Flat stack is empty.");
                 return false;
                }
            countValidFiles();
            }
        catch (OutOfMemoryError e)
            {
            IJ.outOfMemory("Flat Stack");
            log ("ERROR: Out of memory");
            return false;
            }
        return true;
        }



    boolean loadMasterBiasFile()
            {
            if (requestStop) return false;
//            mbiasImp = WindowManager.getImage(biasMaster.trim());
//            if (mbiasImp != null) 
//                {
//                mbiasImp.close();
//                }
            biasMasterPath = getPath(biasMasterDirField, false);
            mbiasPath = biasMasterPath + biasMaster.trim();
            log("Loading master bias file \""+mbiasPath+"\"");
            mbiasImp = IJ.openImage(mbiasPath);
            if (mbiasImp == null)
                {
                error("ERROR: Master bias file \""+mbiasPath+"\" open failed.");
                return false;
                }
            mbiasImp.setProcessor(getAdjustedIp(mbiasImp, mbiasPath, removePedestal, CONVERTTOFLOAT));
            if (showMasters) IJU.replaceImageInWindow(mbiasImp.duplicate(), "DUP_"+biasMaster.trim());
            return true;
            }


    boolean loadMasterDarkFile()
            {
            if (requestStop) return false;
//            mdarkImp = WindowManager.getImage(darkMaster.trim());
//            if (mdarkImp != null) mdarkImp.close();
            darkMasterPath = getPath(darkMasterDirField, false);
            mdarkPath = darkMasterPath + darkMaster.trim();
            log("Loading master dark file \""+mdarkPath+"\"");
            mdarkImp = IJ.openImage(mdarkPath);
            if (mdarkImp == null)
                {
                error("ERROR: Master dark file \""+mdarkPath+"\" open failed.");
                return false;
                }
            mdarkImp.setProcessor(getAdjustedIp(mdarkImp, mdarkPath, removePedestal, CONVERTTOFLOAT));
            if (showMasters) IJU.replaceImageInWindow(mdarkImp.duplicate(), "DUP_"+darkMaster.trim());
            if (useDark && useBias && !deBiasMasterDark) //if master dark is not debiased, do it here if bias subtraction is enabled
                {
                int height = mdarkImp.getHeight();
                int width = mdarkImp.getWidth();
                ImageProcessor mdarkIp = mdarkImp.getProcessor();
                ImageProcessor mbiasIp = mbiasImp.getProcessor();
                if (mbiasImp == null || height != mbiasImp.getHeight() || width != mbiasImp.getWidth()) return false;
                for (int y=0; y<height; y++)
                    {
                    for (int x=0; x<width; x++)
                        {
                        mdarkIp.putPixelValue(x, y, (float)mdarkIp.getPixelValue(x, y) - (float)mbiasIp.getPixelValue(x, y));
                        }
                    }           
                log("    Debiased with "+biasMaster.trim());
                mdarkImp.setProcessor(mdarkIp);
                }
            return true;
            }


    boolean loadMasterFlatFile()
            {
            if (requestStop) return false;
//            mflatImp = WindowManager.getImage(flatMaster.trim());
//            if (mflatImp != null) mflatImp.close();
            flatMasterPath = getPath(flatMasterDirField, false);
            if (!flatPath.endsWith(slash)) flatPath += slash;
            mflatPath = flatMasterPath + flatMaster.trim();
            log("Loading master flat file \""+mflatPath+"\"");
            mflatImp = IJ.openImage(mflatPath);
            if (mflatImp == null)
                {
                error("ERROR: Master flat file \""+mflatPath+"\" open failed.");
                return false;
                }
            ImageProcessor normFlatIp = normalizeImage(getAdjustedIp(mflatImp, mflatPath, removePedestal, CONVERTTOFLOAT), mflatPath);
            if (normFlatIp == null)
                {
                error("ERROR: Master flat mean value = 0 for file \""+mflatPath+"\". Divide Error. Normalize Failed.");
                return false;
                }            
            mflatImp.setProcessor(normFlatIp);
            if (showMasters) IJU.replaceImageInWindow(mflatImp.duplicate(), "DUP_"+flatMaster.trim());
            return true;
            }
    
    ImageProcessor getAdjustedIp(ImagePlus imp, String path, boolean removePedestal, boolean convertToFloat)
        {
        ImageProcessor ip = imp.getProcessor();
        ip.setCalibrationTable(imp.getCalibration().getCTable());
        if (convertToFloat)
            ip = ip.convertToFloat();

        String[] header = FitsJ.getHeader(imp);
        if ((header != null) && removePedestal)
            {
            int cardnum = FitsJ.findCardWithKey("PEDESTAL", header);
            if (cardnum != -1)
                {
                double pedestal = FitsJ.getCardDoubleValue(header[cardnum]);
                if (!Double.isNaN(pedestal) && pedestal != 0.0) 
                    {
                    float fpedestal = (float)pedestal;
                    int ipedestal = (int)pedestal;
                    int width = imp.getWidth();
                    int height = imp.getHeight();
                    if (ip.getBitDepth() == 32)
                        {
                        for (int y=0; y<height; y++)
                            {
                            for (int x=0; x<width; x++)
                                {
                                ip.putPixelValue(x, y, ip.getPixelValue(x, y) + fpedestal);
                                }
                            }
                        header = FitsJ.setCard("PEDESTAL", 0, null, header);
                        header = FitsJ.addHistory("Removed pedestal value of "+fpedestal+" from image "+IJU.getSliceFilename(imp), header);
                        log("    Removed pedestal value of "+fpedestal+" from image "+path);
                        }
                    else
                        {
                        for (int y=0; y<height; y++)
                            {
                            for (int x=0; x<width; x++)
                                {
                                ip.putPixelValue(x, y, ip.getPixelValue(x, y) + ipedestal);
                                }
                            }
                        header = FitsJ.setCard("PEDESTAL", 0, null, header);
                        header = FitsJ.addHistory("Removed pedestal value of "+ipedestal+" from image "+IJU.getSliceFilename(imp), header);
                        log("    Removed pedestal value of "+ipedestal+" from image "+path);
                        }                        
                    FitsJ.putHeader(imp, header);
                    }
                }
            }
        return ip;
        }
    


    boolean processData()
        {
        double pixel = 0.0;
        double expTimeFactor = 1.0;
        ImageProcessor scienceIp = getAdjustedIp(scienceImp, sciencePath, removePedestal, CONVERTTOFLOAT);

        ImageProcessor mbiasIp = null;
        if (useBias)
            {
            mbiasIp = mbiasImp.getProcessor();
            }

        ImageProcessor mdarkIp = null;
        if (useDark)
            {
            mdarkIp = mdarkImp.getProcessor();
            }

        ImageProcessor mflatIp = null;
        if (useFlat)
            {
            mflatIp = mflatImp.getProcessor();
            }

        scienceHeight = scienceIp.getHeight();
        scienceWidth = scienceIp.getWidth();
        length = scienceWidth * scienceHeight;
        if (useBias && mbiasIp.getHeight() != scienceHeight)
                {
                error("ERROR: Master bias height is not equal to \""+sciencePath+"\" height.");
                return false;
                }
        if (useBias && mbiasIp.getWidth() != scienceWidth)
                {
                error("ERROR: Master bias width is not equal to \""+sciencePath+"\" width.");
                return false;
                }
        if (useDark && mdarkIp.getHeight() != scienceHeight)
                {
                error("ERROR: Master dark height is not equal to \""+sciencePath+"\" height.");
                return false;
                }
        if (useDark && mdarkIp.getWidth() != scienceWidth)
                {
                error("ERROR: Master dark width is not equal to \""+sciencePath+"\" width.");
                return false;
                }
        if (useFlat && mflatIp.getHeight() != scienceHeight)
                {
                error("ERROR: Master flat height is not equal to \""+sciencePath+"\" height.");
                return false;
                }
        if (useFlat && mflatIp.getWidth() != scienceWidth)
                {
                error("ERROR: Master flat width is not equal to \""+sciencePath+"\" width.");
                return false;
                }

        scienceHeader = FitsJ.getHeader(scienceImp);
        if (scienceHeader == null)
            {
            IJ.log("WARNING: Cannot extract FITS header from science image \""+sciencePath+"\".");
            //return false;
            }
        else if (calcHeaders)
            {
            double jd = FitsJ.getMeanJD (scienceHeader);
            if (Double.isNaN(jd))
                {
                error("Could not extract exposure start time or exposure time from Fits header. Astrophysical calculations aborted.");
                return false;
                }
            acc.setTime(jd);
            if (selectedObservatoryLocationSource == 0)  // Manually selected observatory name
                {
                log("    Observatory name \"" + acc.getObservatory() + "\" manually selected");
                }
            else if (selectedObservatoryLocationSource == 1)  // FITS header observatory name
                {
                int cardnum = FitsJ.findCardWithKey(observatoryNameReadKeyword, scienceHeader);
                if (cardnum == -1)
                    {
                    error("ERROR: Cannot extract OBSERVATORY NAME from FITS header keyword \""+observatoryNameReadKeyword+
                                        "\" of science image \""+sciencePath+"\".");
                    return false;
                    }
                String cardtype = FitsJ.getCardType(scienceHeader[cardnum]);
                if (cardtype != "S")
                    {
                    error("ERROR: OBSERVATORY NAME FITS header keyword \""+observatoryNameReadKeyword+
                                        "\" of science image \""+sciencePath+"\" is not of type String.");
                    return false;
                    }
                String observatoryName = FitsJ.getCardStringValue(scienceHeader[cardnum]).trim();
                boolean newName = oldObservatoryName.equals("") || !observatoryName.equals(oldObservatoryName);
                if (newName && !acc.setObservatory(observatoryName))
                    {
                    error("ERROR: Failed to find observatory containing \""+observatoryName+"\".");
                    oldObservatoryName = "";
                    return false;
                    }
                log("    Observatory name \""+observatoryName+"\" from FITS header "+(newName?"":"previously ")+"resolved to \""+ acc.getObservatory()+"\"");
                oldObservatoryName = observatoryName;
                }
            else if (selectedObservatoryLocationSource == 2)  // FITS header observatory latitude and longitude
                {
                int latCardNum = FitsJ.findCardWithKey(observatoryLatReadKeyword, scienceHeader);
                if (latCardNum == -1)
                    {
                    error("ERROR: Cannot extract observatory latitude from FITS header keyword \""+observatoryLatReadKeyword+
                                        "\" of science image \""+sciencePath+"\".");
                    return false;
                    }
                int lonCardNum = FitsJ.findCardWithKey(observatoryLonReadKeyword, scienceHeader);
                if (lonCardNum == -1)
                    {
                    error("ERROR: Cannot extract observatory longitude from FITS header keyword \""+observatoryLonReadKeyword+
                                        "\" of science image \""+sciencePath+"\".");
                    return false;
                    }
                String latCardtype = FitsJ.getCardType(scienceHeader[latCardNum]);
                String lonCardtype = FitsJ.getCardType(scienceHeader[lonCardNum]);
                if (!(latCardtype.equals("S") || latCardtype.equals("R")))
                    {
                    error("ERROR: Observatory latitude FITS header keyword \""+observatoryLatReadKeyword+
                                        "\" of science image \""+sciencePath+"\" is not of type String or Real.");
                    return false;
                    }
                 if (!(lonCardtype.equals("S") || lonCardtype.equals("R")))
                    {
                    error("ERROR: Observatory longitude FITS header keyword \""+observatoryLonReadKeyword+
                                        "\" of science image \""+sciencePath+"\" is not of type String or Real.");
                    return false;
                    }
                String latString = "";
                String lonString = "";
                double lat = 0.0;
                double lon = 0.0;
                if (latCardtype.equals("S"))
                    {
                    latString = FitsJ.getCardStringValue(scienceHeader[latCardNum]).trim();
                    if (latString == null || latString.equals(""))
                        {
                        error("ERROR: Observatory latitude FITS header keyword \""+observatoryLatReadKeyword+
                                            "\" of science image \""+sciencePath+"\" does not contain a number.");
                        return false;
                        }
                    lat = acc.sexToDec(latString, 90);
                    if (Double.isNaN(lat))
                        {
                        error("ERROR: Observatory latitude FITS header keyword \""+observatoryLatReadKeyword+
                                            "\" of science image \""+sciencePath+"\" does not contain a number.");
                        return false;
                        }
                    }
                else
                    {
                    lat = FitsJ.getCardDoubleValue(scienceHeader[latCardNum]);
                    if (Double.isNaN(lat))
                        {
                        error("ERROR:  Observatory latitude FITS header keyword \""+observatoryLatReadKeyword+
                                            "\" of science image \""+sciencePath+"\" does not contain a number.");
                        return false;
                        }
                    }
                if (lonCardtype.equals("S"))
                    {
                    lonString = FitsJ.getCardStringValue(scienceHeader[lonCardNum]).trim();
                    if (lonString == null || lonString.equals(""))
                        {
                        error("ERROR: Observatory longitude FITS header keyword \""+observatoryLonReadKeyword+
                                            "\" of science image \""+sciencePath+"\" does not contain a number.");
                        return false;
                        }
                    lon = acc.sexToDec(lonString, 180);
                    if (Double.isNaN(lon))
                        {
                        error("ERROR: Observatory longitude FITS header keyword \""+observatoryLonReadKeyword+
                                            "\" of science image \""+sciencePath+"\" does not contain a number.");
                        return false;
                        }
                    }
                else
                    {
                    lon = FitsJ.getCardDoubleValue(scienceHeader[lonCardNum]);
                    if (Double.isNaN(lon))
                        {
                        error("ERROR: Observatory longitude FITS header keyword \""+observatoryLonReadKeyword+
                                            "\" of science image \""+sciencePath+"\" does not contain a number.");
                        return false;
                        }
                    }
                if (latNegate) lat *= -1.0;
                if (lonNegate) lon *= -1.0;
                acc.setLatLonAlt(lat, lon, 0.0);
                log("    Observatory location = "+acc.decToSex(acc.getObservatoryLatitude(), 2, 90, true)+" "+acc.decToSex(acc.getObservatoryLongitude(), 2, 180, true)+" (from FITS header latitude and longitude)");
                }


            if (selectedObjectCoordinateSource == 0)  //manual entry
                {
                int returncode = acc.processManualCoordinates();
                if (returncode == 3)
                    {
                    log("ERROR: Ohio State access failed when attempting to retrieve BJD(TDB).");
                    return false;
                    }
                log("    Target coordinates manually entered = "+acc.decToSex(acc.getRAJ2000(), 3, 24, false)+" "+acc.decToSex(acc.getDecJ2000(), 2, 90, true)+" (J2000)");
                }
            else if (selectedObjectCoordinateSource == 1 || selectedObjectCoordinateSource == 2)   //target name from FITS header
                {
                int cardnum = FitsJ.findCardWithKey(objectNameReadKeyword, scienceHeader);
                if (cardnum == -1)
                    {
                    error("ERROR: Cannot extract TARGET NAME from FITS header keyword \""+objectNameReadKeyword+
                                        "\" of science image \""+sciencePath+"\".");
                    return false;
                    }
                String cardtype = FitsJ.getCardType(scienceHeader[cardnum]);
                if (cardtype != "S")
                    {
                    error("ERROR: TARGET NAME FITS header keyword \""+objectNameReadKeyword+
                                        "\" of science image \""+sciencePath+"\" is not of type String.");
                    return false;
                    }
                String targetName = FitsJ.getCardStringValue(scienceHeader[cardnum]).trim();
                
                if (selectedObjectCoordinateSource == 2 && targetName.substring(targetName.length()-1).matches("[a-zA-Z]"))
                    {
                    targetName = targetName.substring(0, targetName.length()-1);
                    }
                boolean newName = oldTargetName.equals("") || !targetName.equals(oldTargetName);
                oldTargetName = targetName;
                int returncode = 0;
                if (newName) returncode = acc.processSimbadID(targetName);
                else returncode = acc.processRADECJ2000Coordinates();
                if (returncode == 1)
                    {
                    log("ERROR: SIMBAD access failed when attempting to resolve target name \""+targetName+"\".");
                    oldTargetName = "";
                    return false;
                    }
                else if (returncode == 2)
                    {
                    log("ERROR: SIMBAD could not resolve target name \""+targetName+"\".");
                    oldTargetName = "";
                    return false;
                    }
                else if (returncode == 3)
                    {
                    log("ERROR: Ohio State access failed when attempting to retrieve BJD(TDB).");
                    return false;
                    }
                log("    Target name \""+targetName+"\" from FITS header "+(newName?"":"previously ")+"resolved by SIMBAD to "+
                    acc.decToSex(acc.getRAJ2000(), 3, 24, false)+" "+acc.decToSex(acc.getDecJ2000(), 2, 90, true)+" (J2000)");
                }
            else if (selectedObjectCoordinateSource == 3 || selectedObjectCoordinateSource == 4)     // RA/DEC from fits header  (J2000 or EOD)
                {
                int raCardNum = FitsJ.findCardWithKey(objectRAJ2000ReadKeyword, scienceHeader);
                if (raCardNum == -1)
                    {
                    error("ERROR: Cannot extract TARGET RA from FITS header keyword \""+objectRAJ2000ReadKeyword+
                                        "\" of science image \""+sciencePath+"\".");
                    return false;
                    }
                int decCardNum = FitsJ.findCardWithKey(objectDecJ2000ReadKeyword, scienceHeader);
                if (decCardNum == -1)
                    {
                    error("ERROR: Cannot extract TARGET DEC from FITS header keyword \""+objectDecJ2000ReadKeyword+
                                        "\" of science image \""+sciencePath+"\".");
                    return false;
                    }
                String raCardtype = FitsJ.getCardType(scienceHeader[raCardNum]);
                String decCardtype = FitsJ.getCardType(scienceHeader[decCardNum]);
                if (!(raCardtype.equals("S") || raCardtype.equals("R")))
                    {
                    error("ERROR: TARGET RA FITS header keyword \""+objectRAJ2000ReadKeyword+
                                        "\" of science image \""+sciencePath+"\" is not of type String or Real.");
                    return false;
                    }
                 if (!(decCardtype.equals("S") || decCardtype.equals("R")))
                    {
                    error("ERROR: TARGET DEC FITS header keyword \""+objectDecJ2000ReadKeyword+
                                        "\" of science image \""+sciencePath+"\" is not of type String or Real.");
                    return false;
                    }
                String raString = "";
                String decString = "";
                double ra = 0.0;
                double dec = 0.0;
                if (raCardtype.equals("S"))
                    {
                    raString = FitsJ.getCardStringValue(scienceHeader[raCardNum]).trim();
                    if (raString == null || raString.equals(""))
                        {
                        error("ERROR: TARGET RA FITS header keyword \""+objectRAJ2000ReadKeyword+
                                            "\" of science image \""+sciencePath+"\" does not contain a number.");
                        return false;
                        }
                    ra = acc.sexToDec(raString, 24);
                    if (Double.isNaN(ra))
                        {
                        error("ERROR: TARGET RA FITS header keyword \""+objectRAJ2000ReadKeyword+
                                            "\" of science image \""+sciencePath+"\" does not contain a number.");
                        return false;
                        }
                    }
                else
                    {
                    ra = FitsJ.getCardDoubleValue(scienceHeader[raCardNum]);
                    if (Double.isNaN(ra))
                        {
                        error("ERROR: TARGET RA FITS header keyword \""+objectRAJ2000ReadKeyword+
                                            "\" of science image \""+sciencePath+"\" does not contain a number.");
                        return false;
                        }
                    }
                if (raInDegrees) ra /= 15.0;
                if (decCardtype.equals("S"))
                    {
                    decString = FitsJ.getCardStringValue(scienceHeader[decCardNum]).trim();
                    if (decString == null || decString.equals(""))
                        {
                        error("ERROR: TARGET DEC FITS header keyword \""+objectDecJ2000ReadKeyword+
                                            "\" of science image \""+sciencePath+"\" does not contain a number.");
                        return false;
                        }
                    dec = acc.sexToDec(decString, 90);
                    if (Double.isNaN(dec))
                        {
                        error("ERROR: TARGET DEC FITS header keyword \""+objectDecJ2000ReadKeyword+
                                            "\" of science image \""+sciencePath+"\" does not contain a number.");
                        return false;
                        }
                    }
                else
                    {
                    dec = FitsJ.getCardDoubleValue(scienceHeader[decCardNum]);
                    if (Double.isNaN(dec))
                        {
                        error("ERROR: TARGET DEC FITS header keyword \""+objectDecJ2000ReadKeyword+
                                            "\" of science image \""+sciencePath+"\" does not contain a number.");
                        return false;
                        }
                    }
                int returncode = 0;
                if (selectedObjectCoordinateSource == 3) acc.processRADECJ2000(ra, dec);
                else acc.processRADECEOD(ra, dec);
                if (returncode == 3)
                    {
                    log("ERROR: Ohio State access failed when attempting to retrieve BJD(TDB).");
                    return false;
                    }
                if (selectedObjectCoordinateSource == 3)
                    log("    Target coordinates = "+acc.decToSex(acc.getRAJ2000(), 3, 24, false)+" "+acc.decToSex(acc.getDecJ2000(), 2, 90, true)+" (J2000) (from FITS header RA and DEC)");
                else
                    log("    Target coordinates = "+acc.decToSex(acc.getRAEOI(), 3, 24, false)+" "+acc.decToSex(acc.getDecEOI(), 2, 90, true)+" (Epoch of Observation) (from FITS header RA and DEC)");

                }
            
            if (saveJD_SOBS)
                {
                scienceHeader = FitsJ.setCard(JD_SOBSSaveKeyword, FitsJ.getJD(scienceHeader), "Julian Date at start of exposure", scienceHeader);
                }
            if (saveJD_MOBS)
                {
                scienceHeader = FitsJ.setCard(JD_MOBSSaveKeyword, acc.getJD(), "Julian Date (UTC) at mid-exposure", scienceHeader);
                log("    JD = " + sixPlaces.format(jd)+" (mid-exp)");
                }
            if (saveHJD_MOBS)
                {
                scienceHeader = FitsJ.setCard(HJD_MOBSSaveKeyword, acc.getHJD(), "Heliocentric JD (UTC) at mid-exposure", scienceHeader);
                log("    HJD = "+sixPlaces.format(acc.getHJD())+" (mid-exp)    (correction = "+fourPlaces.format(acc.getHJDCorrection()*24*60)+" minutes)");
                }
            if (saveBJD_MOBS)
                {
                scienceHeader = FitsJ.setCard(BJD_MOBSSaveKeyword, acc.getBJD(), "Barycentric JD (TDB) at mid-exposure", scienceHeader);
                log("    BJD(TDB) = "+sixPlaces.format(acc.getBJD())+" (mid-exp)    (correction = "+fourPlaces.format(acc.getBJDCorrection()*24*60)+" minutes)");
                }
            if (saveObjectAltitude)
                {
                scienceHeader = FitsJ.setCard(objectAltitudeSaveKeyword, acc.getAltitude(), "Target altitude at mid-exposure", scienceHeader);
                log("    Altitude = "+twoPlaces.format(acc.getAltitude())+" (mid-exp)");
                }
            if (saveObjectAzimuth)
                {
                scienceHeader = FitsJ.setCard(objectAzimuthSaveKeyword, acc.getAzimuth(), "Target azimuth at mid-exposure", scienceHeader);
                log("    Azimuth = "+twoPlaces.format(acc.getAzimuth())+" (mid-exp)");
                }
            if (saveObjectHourAngle)
                {
                scienceHeader = FitsJ.setCard(objectHourAngleSaveKeyword, acc.getHourAngle(), "Target hour angle at mid-exposure", scienceHeader);
                log("    Hour Angle = "+twoPlaces.format(acc.getHourAngle())+" (mid-exp)");
                }   
            if (saveObjectZenithDistance)
                {
                scienceHeader = FitsJ.setCard(objectZenithDistanceSaveKeyword, acc.getZenithDistance(), "Target zenith distance at mid-exposure", scienceHeader);
                log("    Zenith Distance = "+twoPlaces.format(acc.getZenithDistance())+" (mid-exp)");
                }             
            if (saveObjectAirmass)
                {
                scienceHeader = FitsJ.setCard(objectAirmassSaveKeyword, acc.getAirmass(), "Target airmass at mid-exposure", scienceHeader);
                log("    Airmass = "+fourPlaces.format(acc.getAirmass())+" (mid-exp)");
                }

            if (saveObjectRAJ2000) scienceHeader = FitsJ.setCard(objectRAJ2000SaveKeyword, acc.getRAJ2000(), "J2000 right ascension of target (hours)", scienceHeader);
            if (saveObjectDecJ2000) scienceHeader = FitsJ.setCard(objectDecJ2000SaveKeyword, acc.getDecJ2000(), "J2000 declination of target (degrees)", scienceHeader);
            if (saveObjectRAEOD) scienceHeader = FitsJ.setCard(objectRAEODSaveKeyword, acc.getRAEOI(), "EOD right ascension of target (hours)", scienceHeader);
            if (saveObjectDecEOD) scienceHeader = FitsJ.setCard(objectDecEODSaveKeyword, acc.getDecEOI(), "EOD declination of target (degrees)", scienceHeader);
            if (saveObservatoryLat) scienceHeader = FitsJ.setCard(observatoryLatSaveKeyword, acc.getObservatoryLatitude(), "geographic latitude of observatory", scienceHeader);
            if (saveObservatoryLon) scienceHeader = FitsJ.setCard(observatoryLonSaveKeyword, acc.getObservatoryLongitude(), "geographic longitude of observatory", scienceHeader);
            }


        if (scaleExpTime && useBias && useDark)
            {
            scienceExpTime = (double)FitsJ.getExposureTime(scienceHeader);
            if (Double.isNaN(scienceExpTime))
                {
                error("ERROR: Cannot extract exposure time from science image header!");
                return false;
                }
            darkHeader = FitsJ.getHeader(mdarkImp);
            if (darkHeader == null)
                {
                error("ERROR: Cannot extract FITS header from master dark image \""+mdarkPath+"\".");
                return false;
                }
            darkExpTime = (double)FitsJ.getExposureTime(darkHeader);
            if (Double.isNaN(darkExpTime))
                {
                error("ERROR: Cannot extract exposure time from \""+mdarkPath+"\" master dark header!");
                return false;
                }
            expTimeFactor = scienceExpTime/darkExpTime;
            }
        
        double pixel2;
        float flatValue;
        for (int y = 0; y < scienceHeight; y++)
            {
            for (int x = 0; x < scienceWidth; x++)
                {            
                pixel = (double)scienceIp.getPixelValue(x, y);
                if (useBias)
                    {
                    pixel -= (double)mbiasIp.getPixelValue(x, y);
                    if (useNLC)
                        {
                        pixel2 = pixel*pixel;
                        pixel = coeffA + pixel*coeffB + pixel2*coeffC + pixel*pixel2*coeffD;
                        }
                    }
                if (useDark)
                    {
                    pixel -= (double)mdarkIp.getPixelValue(x, y)*expTimeFactor;
                    }
                if (useFlat)
                    {
                    flatValue = mflatIp.getPixelValue(x, y);
                    if (flatValue == 0.0f)
                        {
                        pixel = 65535.0;
                        pixel2 = pixel*pixel;
                        if (useNLC && useBias)
                            {
                            pixel = coeffA + pixel*coeffB + pixel2*coeffC + pixel*pixel2*coeffD;
                            }
                        }
                    else
                        {
                        pixel /= (double)flatValue;
                        }
                    }
                if (!useBias && !useDark && !useFlat && useNLC)  //do NLC for previously calibrated images
                    {
                    pixel2 = pixel*pixel;
                    pixel = coeffA + pixel*coeffB + pixel2*coeffC + pixel*pixel2*coeffD;                
                    }
                scienceIp.putPixelValue(x, y, (float)pixel);
                }
             }

//        if (!saveFloatingPoint)
//            {
//            scienceIp = scienceIp.convertToShort(false);
//            }

        savePath = mainDir + saveDir.trim();
        if (!savePath.endsWith(slash)) savePath += slash;
        saveDirPath = savePath;
        saveFileName = "Processed_"+s;

        if (saveProcessedData && (saveSuffix.trim().length()!=0 || saveDir.trim().length() != 0))
            {
            int dotIndex = s.lastIndexOf(".");
            if (s.endsWith(".gz") || s.endsWith(".fz") || s.endsWith(".zip"))
                {
                s = s.substring(0, dotIndex);
                dotIndex = s.lastIndexOf(".");
                }            
            if (dotIndex != -1) //a filetype exists
                {
                String filetype = s.substring(dotIndex);
                saveFileName = s.replace(filetype, ""+saveSuffix.trim()+filetype);
                }
            else
                saveFileName = s.concat(""+saveSuffix.trim()+".fits");
            }
        savePath += saveFileName;
        if (scienceHeader != null) scienceHeader = FitsJ.addHistory("Previous Filename = "+s, scienceHeader);
        s = saveFileName;
        scienceImp.setProcessor(s, scienceIp);

        if (scienceHeader != null)
            {
            if (useBias)
                {
                scienceHeader = FitsJ.addHistory("Bias corrected with "+biasMaster.trim(), scienceHeader);
                log("    Bias corrected with "+biasMaster.trim());
                }
            if (useNLC && ((!useBias && !useDark && !useFlat) || useBias))
                {
                scienceHeader = FitsJ.addHistory("Non-linear corrected with coefficients:", scienceHeader);
                scienceHeader = FitsJ.addHistory("a0 = "+coeffA, scienceHeader);
                scienceHeader = FitsJ.addHistory("a1 = "+coeffB, scienceHeader);
                scienceHeader = FitsJ.addHistory("a2 = "+coeffC, scienceHeader);
                scienceHeader = FitsJ.addHistory("a3 = "+coeffD, scienceHeader);
                log("    Non-linear corrected with coefficients: a0="+coeffA+" a1="+coeffB+" a2="+coeffC+" a3="+coeffD);
                }
            if (useDark)
                {
                scienceHeader = FitsJ.addHistory("Dark corrected with "+(useBias && !deBiasMasterDark?"(deBiased) ":"")+darkMaster.trim(), scienceHeader);
                if (useBias && scaleExpTime) 
                    {
                    scienceHeader = FitsJ.addHistory("and exposure time scaling factor = "+expTimeFactor, scienceHeader);
                    log("    Dark corrected with "+(!deBiasMasterDark?"(deBiased) ":"")+darkMaster.trim()+" and exposure time scaling factor "+scienceExpTime+"/"+darkExpTime+"="+expTimeFactor);
                    }
                else 
                    {
                    log("    Dark corrected with "+(useBias && !deBiasMasterDark?"(deBiased) ":"")+darkMaster.trim());
                    }
                }
            if (useFlat)
                {
                scienceHeader = FitsJ.addHistory("Flat corrected with " + flatMaster.trim(), scienceHeader);
                log("    Flat corrected with " + flatMaster.trim());
                }


            FitsJ.putHeader(scienceImp, scienceHeader);
            }

        IJ.wait(100);      //attempt to work around crash problem
        return true;
        }

    boolean saveProcessedFile(ImagePlus impLocal, String dirPath, String filePath, String name, String format)
        {
        boolean localCompress = compress;
        if (filePath.endsWith(".gz") || filePath.endsWith(".fz") || filePath.endsWith(".zip"))
            {
            int dotIndex = filePath.lastIndexOf(".");
            filePath = filePath.substring(0, dotIndex);
            if (!name.equals("processed science")) localCompress = true;
            }
        File saveDirectory;
        if (impLocal != null)
            {
            saveDirectory = new File (dirPath);
            if (saveDirectory.isFile())
                {
                error("ERROR: Save directory \""+dirPath+"\" is a file, not a directory.");
                return false;
                }
            if (!saveDirectory.isDirectory())
                {
                if (!saveDirectory.mkdir())
                    {
                    error("ERROR: Could not create save-to directory \""+dirPath+"\".");
                    return false;
                    }

                }
            if (!format.trim().equals("") && !format.trim().equals(null))
                {
                filePath = updateExtension(filePath, format);
                }

            if (filePath.toLowerCase().endsWith(".fits") || filePath.toLowerCase().endsWith(".fit") || filePath.toLowerCase().endsWith(".fts"))
                {
                IJ.runPlugIn(impLocal, "ij.plugin.FITS_Writer", filePath + (compress ? ".gz" : ""));
                }
            else
                {
                IJ.save((ImagePlus) impLocal.clone(), filePath);
                }
            log("    Saved "+name+" file \""+filePath+"\"");
            }
        else
            {
            error("ERROR: "+name+" image was null prior to save. Save aborted.");
            return false;
            }
        return true;
        }

    void error(String message)
        {
        log(message);
        IJ.beep();
        IJ.showMessage(message);
        }

    void log(String message)
        {
        if (showLog)
            {
            if (showLogDateTime)
                {
                cal = Calendar.getInstance();
                IJ.log("["+sdf.format(cal.getTime())+"]  "+message);
                }
            else
                {
                IJ.log(message);
                }
            }
        }

	static String updateExtension(String path, String extension)
        {
		if (path==null) return null;
		int dotIndex = path.lastIndexOf(".");
		int separatorIndex = path.lastIndexOf(File.separator);
		if (dotIndex>=0 && dotIndex>separatorIndex && (path.length()-dotIndex)<=5) {
			if (dotIndex+1<path.length() && Character.isDigit(path.charAt(dotIndex+1)))
				path += extension;
			else
				path = path.substring(0, dotIndex) + extension;
		} else
			path += extension;
		return path;
        }

    void pause()
        {
        unlock();
        requestStop = true;
        if (timer != null) timer.cancel();
        if (task != null) task.cancel();
        if (running)
            {
            startButton.setText("CONTINUE");
            startButton.setForeground(Color.orange);
            startButton.repaint();
            }
        running = false;
        }

    void reset()
        {
        String message;
        unlock();
        if (requestStop) message = "STOPPED";
        else message = "FINISHED";
        requestStop = true;
        if (timer != null) timer.cancel();
        if (task != null) task.cancel();
        running = false;
        active = false;
        images.clear();
        firstRun = true;
        restart = true;
        loadNewCals = true;
        foundImages = 0;
        ignoredImages = 0;
        totalNumFilesInDir = 0;
        startButton.setText("START");
        startButton.setForeground(Color.black);
        startButton.repaint();
        countValidFiles();
        log(" ");
        log("************"+message+"************");
        log(" ");
        }



    void countValidFiles()
        {
        validTextFilteredFiles = 0;
        validNumFilteredFiles = 0;
        
        mainDir = mainDir.trim();
        if (!mainDir.endsWith(slash))
            mainDir += slash;

        File sDir = new File (mainDir);
        if (useScienceProcessing && !onlyNew && sDir.isDirectory())
            {
            String fileName;
            File[] files = sDir.listFiles();
            if (files.length > 0)
                {
                for (int i=0; i < files.length; i++)
                    {
                    fileName = files[i].getName();
                    if (matchWildCard(filenamePattern, fileName))
                        {
                        validTextFilteredFiles++;
                        if (enableFileNumberFiltering && stringLongVal(fileName) >= minFileNumber && stringLongVal(fileName) <= maxFileNumber)
                            {
                            validNumFilteredFiles++;
                            }
                        }
                    }
                }
            }
        if (!enableFileNumberFiltering) validNumFilteredFiles = validTextFilteredFiles;
        if (!onlyNew)
            {
            validTextFilteredFilesLabel.setText (""+validTextFilteredFiles);
            validTextFilteredFilesLabel.repaint();
            validNumFilteredFilesLabel.setText (""+validNumFilteredFiles);
            validNumFilteredFilesLabel.repaint();
            remainingNumLabel.setText (""+(validNumFilteredFiles - foundImages));
            remainingNumLabel.repaint();
            processedNumLabel.setText (""+foundImages);
            processedNumLabel.repaint();
             }
        else
            {
            validTextFilteredFilesLabel.setText ("0");
            validTextFilteredFilesLabel.repaint();
            validNumFilteredFilesLabel.setText ("0");
            validNumFilteredFilesLabel.repaint();
            remainingNumLabel.setText (""+(totalNumFilesInDir - ignoredImages - foundImages));
            remainingNumLabel.repaint();
            processedNumLabel.setText (""+foundImages);
            processedNumLabel.repaint();
            }


        validBiasFiles = createBias ? cntFiles(getPath(biasRawDirField, false), biasBase) : 0;
        validBiasFilesLabel.setText (""+validBiasFiles);
        validBiasFilesLabel.repaint();

        validMasterBiasFiles = useBias ? cntFiles(getPath(biasMasterDirField, false), biasMaster) : 0;
        validMasterBiasFilesLabel.setText (""+validMasterBiasFiles);
        validMasterBiasFilesLabel.repaint();

        validDarkFiles = createDark ? cntFiles(getPath(darkRawDirField, false), darkBase) : 0;
        validDarkFilesLabel.setText (""+validDarkFiles);
        validDarkFilesLabel.repaint();

        validMasterDarkFiles = useDark ? cntFiles(getPath(darkMasterDirField, false), darkMaster) : 0;
        validMasterDarkFilesLabel.setText (""+validMasterDarkFiles);
        validMasterDarkFilesLabel.repaint();

        validFlatFiles = createFlat ? cntFiles(getPath(flatRawDirField, false), flatBase) : 0;
        validFlatFilesLabel.setText (""+validFlatFiles);
        validFlatFilesLabel.repaint();

        validMasterFlatFiles = useFlat ? cntFiles(getPath(flatMasterDirField, false), flatMaster) : 0;
        validMasterFlatFilesLabel.setText (""+validMasterFlatFiles);
        validMasterFlatFilesLabel.repaint();

        if (runPreMacro)
            {
            File preMacroFile = new File(preMacroText.getText().trim());
            validMacro1FilesLabel.setText(preMacroFile.isFile()?"1":"0");
            }
        else
            validMacro1FilesLabel.setText("0");

        if (runPostMacro)
            {
            File postMacroFile = new File(postMacroText.getText().trim());
            validMacro2FilesLabel.setText(postMacroFile.isFile()?"1":"0");
            }
        else
            validMacro2FilesLabel.setText("0");

        return;
        }
    
    int cntFiles(String calPath, String calBase)
        {
        if (calPath == null || calBase == null) return 0;
        String fileBase = calBase.trim();
        int cnt = 0;
        File calDir = new File (calPath.trim());
        if (calDir.isDirectory())
            {
            File[] files = calDir.listFiles();
            if (files == null) return 0;
            if (files.length > 0)
                {
                for (int i=0; i < files.length; i++)
                    {
                    if (matchWildCard(fileBase, files[i].getName()))
                        {
                        cnt++;
                        }
                    }
                }
            }
        return cnt;
        }

    void toggleFitsHeaderDisplay()
        {
        fitsHeadersDisplayed = !fitsHeadersDisplayed;
        IJU.setFrameSizeAndLocation(fitsHeaderFrame, dialogFrameLocationX, dialogFrameLocationY, 0, 0);
        fitsHeaderFrame.setLocation(dialogFrame.getX()+dialogFrame.getWidth()/2 - fitsHeaderFrame.getWidth()/2,
                                    dialogFrame.getY()+dialogFrame.getHeight()/2 - fitsHeaderFrame.getHeight()/2);
		fitsHeaderFrame.setVisible (fitsHeadersDisplayed);
        }

//    boolean matchWildCard(String wildCard, String filename)
//        {
//        return filename.startsWith(wildCard.trim()) && filename.endsWith(".fits");
//        }


	/**
	 * Starts timer which polls the target directory, looking for new images.
	 */
	protected void startTimer ()
		{
		try	{
			task = new TimerTask ()
				{
				public void run ()
					{
                    Prefs.set("multiaperture.canceled",false);
                    countValidFiles();
                    if (restart)
                        {
                        if (!validateSelections())
                            {
                            IJ.showProgress(1.1);
                            reset();
                            return;
                            }
                        IJ.showProgress(1.1);
                        if (createBias && !createMasterBias())
                            {
                            IJ.showProgress(1.1);
                            reset();
                            return;
                            }
                        IJ.showProgress(1.1);
                        if (createDark && !createMasterDark())
                            {
                            IJ.showProgress(1.1);
                            reset();
                            return;
                            }
                        IJ.showProgress(1.1);
                        if (createFlat && !createMasterFlat())
                            {
                            IJ.showProgress(1.1);
                            reset();
                            return;
                            }
                        IJ.showProgress(1.1);
                        restart = false;
                        }
                    if (useScienceProcessing && loadNewCals && validNumFilteredFiles > 0) //(showScience || saveProcessedData) && 
                        {
                        countValidFiles();
                        if (!validateSelections())
                            {
                            reset();
                            return;
                            }
                        if (useBias && !loadMasterBiasFile())
                            {
                            reset();
                            return;
                            }
                        if (useDark && !loadMasterDarkFile())
                            {
                            reset();
                            return;
                            }
                        if (useFlat && !loadMasterFlatFile())
                            {
                            reset();
                            return;
                            }
                        loadNewCals = false;
                        }


					if (useScienceProcessing && lockable()) //&& (showScience || saveProcessedData) 
						{
                        countValidFiles();
						// GET CURRENT LIST OF FILES IN DIRECTORY
                        File[] files = scienceDir.listFiles ();
                        totalNumFilesInDir = files.length;
                        String[] filenames = new String[files.length];
                        for (int i=0; i < files.length; i++)
                                {
                                filenames[i] = files[i].getName();
                                }

                        if (sortNumerically) filenames=sortFileList(filenames);

						for (int i=0; i < files.length; i++)
							{
							s = filenames[i];
                            sOriginal = s;

							// NEW IMAGE?

							if ( ! images.containsKey(s))
								{
								images.put (s,null);

								// IF THE FILE LOOKS LIKE A GOOD NEW IMAGE
                                reloadFilenames = false;
								if (matchWildCard(filenamePattern, s) && (!enableFileNumberFiltering || (stringLongVal(s) >= minFileNumber && stringLongVal(s) <= maxFileNumber)))
									{
									if (useBeep) IJ.beep();
									if (onlyNew && firstRun)
										ignoredImages++;
									else
										{
										foundImages++;

                                        /* IF POLLING:
                                         * CHECK THAT FILESIZE IS CONSTANT TO VERIFY THAT FILE WRITING HAS FINISHED.
                                         * IF FILESIZE IS NOT CONSTANT, THE TIMER IS EXITED
                                         * AND A NEW TIMER IS SET UP TO TRY AGAIN */
                                        if (pollingInterval > 0)
                                            {
                                            File file = new File(mainDir+slash+s);

                                            flen = file.length();
                                            IJ.wait(fileSizeChangeWaitTime);
                                            newflen = file.length();
                                        if (newflen != flen)
                                            {
                                            reloadFilenames = true;
                                            }
                                        }
                                    if (reloadFilenames)
                                            {
                                            images.remove(s);
                                            foundImages--;
                                            }
                                    else
                                            {
                                            if (loadNewCals)
                                                {
                                                if (!validateSelections())
                                                    {
                                                    pause();
                                                    return;
                                                    }
                                                if (useBias && !loadMasterBiasFile())
                                                    {
                                                    pause();
                                                    return;
                                                    }
                                                if (useDark && !loadMasterDarkFile())
                                                    {
                                                    pause();
                                                    return;
                                                    }
                                                if (useFlat && !loadMasterFlatFile())
                                                    {
                                                    pause();
                                                    return;
                                                    }
                                                loadNewCals = false;
                                                }
                                            sciencePath = mainDir+s;
                                            log("Loading science file \""+sciencePath+"\" ("+foundImages+" of "+(onlyNew?foundImages:validNumFilteredFiles)+")");
                                            scienceImp = IJ.openImage(sciencePath);
                                            if (Prefs.get("astrometry.DPSaveRawWithWCS", false))
                                                {
                                                rawScienceImp = scienceImp.duplicate();
                                                }
                                            else
                                                {
                                                rawScienceImp = null;
                                                }
                                            if (scienceImp == null)
                                                    IJ.showMessage ("Unable to open image "+mainDir+s);
                                            else
                                                    {
                                                    ImageProcessor scienceIp = scienceImp.getProcessor();

                                                    if (useBias || useDark || useFlat || calcHeaders || useNLC)
                                                        {
                                                        if (!processData())
                                                            {
                                                            pause();
                                                            return;
                                                            }
                                                        scienceIp = scienceImp.getProcessor();
                                                        }
                                                    else
                                                        {
                                                        savePath = mainDir + saveDir.trim();
                                                        if (!savePath.endsWith(s)) savePath += slash;
                                                        saveDirPath = savePath;
                                                        saveFileName = "Processed_"+s;

                                                        if (saveProcessedData && (saveSuffix.trim().length()!=0 || saveDir.trim().length() != 0))
                                                            {
                                                            int dotIndex = s.lastIndexOf(".");
                                                            if (s.endsWith(".gz") || s.endsWith(".zip"))
                                                                {
                                                                s = s.substring(0, dotIndex);
                                                                dotIndex = s.lastIndexOf(".");
                                                                }
                                                            if (dotIndex != -1) //a filetype exists
                                                                {
                                                                String filetype = s.substring(dotIndex);
                                                                saveFileName = s.replace(filetype, ""+saveSuffix.trim()+filetype);
                                                                }
                                                            else
                                                                saveFileName = s.concat(""+saveSuffix.trim()+".fits");

                                                            }
                                                        
                                                        savePath += saveFileName;
//                                                        log("savePath = "+savePath);
                                                        s = saveFileName;
                                                        scienceImp.setProcessor(s, scienceIp);
                                                        }

                                                    openImage = WindowManager.getImage(lastImageName);

                                                    if (openImage != null)
                                                        {
                                                        openFrame = WindowManager.getFrame(lastImageName);
                                                        usingNewWindow = false;
                                                        imf = scienceImp.getFileInfo();
                                                        openImage.setFileInfo(imf);
                                                        
                                                        //CLEAR PROPERTIES FROM OPENIMAGE
                                                        props = openImage.getProperties();
                                                        if (props != null)
                                                            {
                                                            enProps = props.propertyNames();
                                                            key = "";
                                                            while (enProps.hasMoreElements())
                                                                    {
                                                                    key = (String) enProps.nextElement();
                                                                    openImage.setProperty(key, null);
                                                                    }
                                                            }
                                                        // COPY NEW PROPERTIES TO OPEN WINDOW IMAGEPLUS
                                                        props = scienceImp.getProperties();
                                                        if (props != null)
                                                            {
                                                            enProps = props.propertyNames();
                                                            key = "";
                                                            while (enProps.hasMoreElements())
                                                                    {
                                                                    key = (String) enProps.nextElement();
                                                                    openImage.setProperty(key, props.getProperty(key));
                                                                    }
                                                            }
                                                        if (openImage.getType() == ImagePlus.COLOR_RGB)
                                                            {
                                                            openImage.setDisplayRange(0, 255);
                                                            scienceIp.snapshot();
                                                            }
                                                        if (!lastImageName.equals(s))
                                                            {
                                                            testImp = WindowManager.getImage(s);
                                                            if (testImp != null) testImp.close();
                                                            }
                                                        if (openFrame != null && openFrame instanceof astroj.AstroStackWindow)
                                                            {
                                                            astroj.AstroStackWindow asw = (astroj.AstroStackWindow)openFrame;
                                                            asw.setUpdatesEnabled(false);
                                                            IJ.wait(10);
                                                            }
                                                        openImage.setProcessor(s, scienceIp);
                                                        if (openFrame != null && openFrame instanceof astroj.AstroStackWindow)
                                                            {
                                                            astroj.AstroStackWindow asw = (astroj.AstroStackWindow)openFrame;
                                                            asw.setUpdatesEnabled(true);
                                                            asw.setAstroProcessor(true);
                                                            }                                                        
                                                        }
                                                    else
                                                        {
                                                        if (showScience)
                                                            {
                                                            usingNewWindow = true;
                                                            testImp = WindowManager.getImage(s);
                                                            if (testImp != null) testImp.close();
                                                            scienceImp.show();
                                                            if (scienceImp.getType() == ImagePlus.COLOR_RGB)
                                                                {
                                                                scienceImp.setDisplayRange(0, 255);
                                                                scienceIp.snapshot();
                                                                }
                                                            openImage = WindowManager.getImage(s);
                                                            openFrame = WindowManager.getFrame(s);
                                                            }
                                                        }

                                                    if (showScience)
                                                        {
                                                        imageWindowClass = openFrame.getClass();
                                                        if (preMacro1AutoLevel && imageWindowClass.getName().contains("AstroStackWindow"))
                                                            {
                                                            astroj.AstroStackWindow asw = (astroj.AstroStackWindow) openFrame;
                                                            asw.setAutoLevels(s);
                                                            }

                                                        if (runPreMacro)
                                                            {
                                                            IJ.showStatus ("Running Macro 1 on "+s+"...");
                                                            runner.runMacroFile (preMacroPath, s);
                                                            openImage = WindowManager.getImage(s);
                                                            openFrame = WindowManager.getFrame(s);
                                                            scienceIp = openImage.getProcessor();
                                                            }
                                                        imageWindowClass = openFrame.getClass();
                                                        if (postMacro1AutoLevel && imageWindowClass.getName().contains("AstroStackWindow"))
                                                            {
                                                            astroj.AstroStackWindow asw = (astroj.AstroStackWindow) openFrame;
                                                            asw.setAstroProcessor(true);
                                                            asw.setAutoLevels(s);
                                                            }
                                                        }

                                                    if (useCosmicRemoval)
                                                        {
                                                        if (removeBrightOutliers)
                                                            {
                                                            IJ.run(scienceImp,"Remove Outliers...","radius="+outlierRadius+" threshold="+outlierThreshold+" which=Bright stack");
                                                            scienceIp = scienceImp.getProcessor();
                                                            scienceHeader = FitsJ.getHeader(scienceImp);
                                                            if (scienceHeader != null) scienceHeader = FitsJ.addHistory("Bright outliers removed with radius="+outlierRadius+" and threshold="+outlierThreshold, scienceHeader);
                                                            log("    Bright outliers removed with radius="+outlierRadius+" and threshold="+outlierThreshold);
                                                            FitsJ.putHeader(scienceImp, scienceHeader);
                                                            if (showScience) FitsJ.putHeader(openImage, scienceHeader);
                                                            if (showScience)
                                                                {
                                                                imageWindowClass = openFrame.getClass();
                                                                if (imageWindowClass.getName().contains("AstroStackWindow"))
                                                                        {
                                                                        astroj.AstroStackWindow csw = (astroj.AstroStackWindow) openFrame;
                                                                        csw.setAstroProcessor(true);         
                                                                        }
                                                                }  
                                                            }
                                                        if (removeDarkOutliers)
                                                            {
                                                            IJ.run(scienceImp,"Remove Outliers...","radius="+outlierRadius+" threshold="+outlierThreshold+" which=Dark stack");
                                                            scienceIp = scienceImp.getProcessor();
                                                            scienceHeader = FitsJ.getHeader(scienceImp);
                                                            if (scienceHeader != null) scienceHeader = FitsJ.addHistory("Dark outliers removed with radius="+outlierRadius+" and threshold="+outlierThreshold, scienceHeader);
                                                            log("    Dark outliers removed with radius="+outlierRadius+" and threshold="+outlierThreshold);
                                                            if (scienceHeader != null) FitsJ.putHeader(scienceImp, scienceHeader);
                                                            if (showScience) FitsJ.putHeader(openImage, scienceHeader);
                                                            if (showScience)
                                                                {
                                                                imageWindowClass = openFrame.getClass();
                                                                if (imageWindowClass.getName().contains("AstroStackWindow"))
                                                                        {
                                                                        astroj.AstroStackWindow csw = (astroj.AstroStackWindow) openFrame;
                                                                        csw.setAstroProcessor(true);         
                                                                        }
                                                                }  
                                                            }                                                        
                                                        }

                                                    if (showScience)
                                                        {
                                                        if (runPostMacro)
                                                            {
                                                            IJ.showStatus ("Running Macro 2 on "+s+"...");
                                                            runner.runMacroFile (postMacroPath, s);
                                                            openImage = WindowManager.getImage(s);
                                                            openFrame = WindowManager.getFrame(s);
                                                            scienceIp = openImage.getProcessor();
                                                            }
                                                        imageWindowClass = openFrame.getClass();
                                                        if (postMacro2AutoLevel && imageWindowClass.getName().contains("AstroStackWindow"))
                                                            {
                                                            astroj.AstroStackWindow csw = (astroj.AstroStackWindow) openFrame;
                                                            csw.setAstroProcessor(true);
                                                            csw.setAutoLevels(s);
                                                            }
                                                        }
        
                                                    if (plateSolve)
                                                        {
                                                        astrometrySetupButton.setIcon(astrometryActiveIcon);
                                                        astrometrySetupButton.setToolTipText("Cancel plate solve for this image");
                                                        astrometrySetupButton.setSelected(true);
                                                        log("    Plate solve started");
                                                        int status = astrometry.solve(showScience?openImage:scienceImp, false, acc, true, showLog, showLogDateTime, rawScienceImp, sciencePath);
                                                        if (status == SUCCESS)
                                                            {
                                                            log("    Plate solve success");
                                                            if (showScience) imageWindowClass = openFrame.getClass();
                                                            if (showScience && imageWindowClass.getName().contains("AstroStackWindow"))
                                                                {
                                                                astroj.AstroStackWindow csw = (astroj.AstroStackWindow) openFrame;
                                                                csw.setAstroProcessor(true);                                                            
                                                                }
                                                            }
                                                        else if (status == CANCELED || astrometryCanceledByUser)
                                                            log("    Plate solve canceled by user");
                                                        else if (status == SKIPPED)
                                                            log("    Plate solve skipped (already has valid WCS headers)");                                                        
                                                        else if (status == FAILED)
                                                            log("***Plate solve failure***");
                                                        else
                                                            log("***Plate solve invalid return code***");
                                                        astrometrySetupButton.setToolTipText("<html>Open plate solve settings panel (Astrometry Settings)</html>");
                                                        astrometrySetupButton.setIcon(astrometrySetupIcon);
                                                        astrometrySetupButton.setSelected(false);
                                                        astrometryCanceledByUser = false;                                                        
                                                        }                                                    

                                                    if (saveProcessedData)
                                                        {
                                                        ImagePlus saveImp;
                                                        
                                                        if (saveFloatingPoint)
                                                            {
                                                            if (showScience)
                                                                {
                                                                saveImp = openImage;
                                                                }
                                                            else 
                                                                {
                                                                saveImp = scienceImp;
                                                                }
                                                            }
                                                        else 
                                                            {
                                                            if (showScience) 
                                                                {
                                                                saveImp = openImage.duplicate();
                                                                saveImp.setProcessor(saveImp.getProcessor().convertToShort(false));
                                                                }
                                                            else
                                                                {
                                                                saveImp = scienceImp;
                                                                saveImp.setProcessor(saveImp.getProcessor().convertToShort(false));
                                                                }
                                                            }
                                                        if (!saveProcessedFile(saveImp, saveDirPath, savePath, "processed science", saveFormat))
                                                            {
                                                            pause();
                                                            lastImageName = s;
                                                            }
                                                        }
                                                    
                                                    if (showScience && runMultiAperture)
                                                        {
                                                        Prefs.set("multiaperture.finished",false);
                                                        Prefs.set("multiaperture.macroImageName",s);
                                                        Prefs.set("multiaperture.useMacroImage",true);
                                                        if (changeApertures)
                                                            {
                                                            changeApertures = false;
                                                            Prefs.set("multiaperture.automode",false);
                                                            }
                                                        else
                                                            Prefs.set("multiaperture.automode",true);
                                                        IJ.runPlugIn("Astronomy.MultiAperture_", "");
                                                        Prefs.set("multiaperture.automode",false);
                                                        Prefs.set("multiaperture.useMacroImage",false);
                                                        boolean finished=Prefs.get("multiaperture.finished",true);
                                                        while (finished == false) {
                                                            IJ.wait(100);
                                                            finished = (Prefs.get("multiaperture.finished",true) || Prefs.get("multiaperture.canceled",false) || IJ.escapePressed());
                                                            }
                                                        MAcanceled = Prefs.get("multiaperture.canceled",false);
                                                        if (MAcanceled || IJ.escapePressed())
                                                            {
                                                            Prefs.set("multiaperture.canceled",false);
                                                            pause();
                                                            changeApertures = true;
                                                            images.remove(sOriginal);
                                                            foundImages--;
                                                            log("");
                                                            log("************MULTI-APERTURE CANCELED************");
                                                            log("");
                                                            }
                                                        }
                                                    
                                                    if (showScience && saveImage)
                                                        {
                                                        IJ.wait(100);
                                                        saveStaticImage();
                                                        }
                                                        
                                                    if (showScience && runMultiAperture && runMultiPlot && savePlot)
                                                        {
                                                        saveStaticPlot();
                                                        }                                                    
                                                    
                                                    
                                                    
                                                    
//                                                    if (runMultiPlot && !MAcanceled)
//                                                        {
//                                                        String tableName = "Measurements";
//                                                        Frame plotFrame = WindowManager.getFrame("Plot of "+tableName);
//                                                        if (plotFrame != null)
//                                                            {
//                                                            MultiPlot_.updatePlot();
//                                                            }
//                                                        else
//                                                            IJ.runPlugIn("MultiPlot_", "");
//                                                        }

                                                    remainingNumLabel.setText (""+(totalNumFilesInDir - ignoredImages - foundImages));
                                                    remainingNumLabel.repaint();
                                                    processedNumLabel.setText (""+foundImages);
                                                    processedNumLabel.repaint();
                                                    }
                                                lastImageName = s;
                                                }
                                            }
                                    }
                                else
                                    {
                                    ignoredImages++;
                                    }
                            }
                        countValidFiles();
                        if (!useScienceProcessing || requestStop) i = files.length - 1;
                        }
                    firstRun = false;
                    unlock();
                }
            if (autoRunAndClose)
                {
                saveAndClose();
                }
            if (!useScienceProcessing || (pollingInterval == 0 && foundImages == validNumFilteredFiles))
                {
                reset();
                return;
                }
            }
        };
    if (!requestStop)
        {
        timer = new Timer();
        if (pollingInterval != 0) timer.schedule (task,0,pollingInterval*1000);
        else timer.schedule (task,0,5000);
                                    }
    }
catch (Exception e)
    {
    IJ.showMessage ("Error starting timer : "+e.getMessage());
    }
}
   
    
void saveStaticImage()
    {
    if (saveImagePath == null || saveImagePath.trim().equals("")) return;
    String imageDirName = "";
    String imageFileName = "";
    int lastSlash = saveImagePath.lastIndexOf(slash);
    if (lastSlash >= 0)
        {
        imageDirName = saveImagePath.substring(0, lastSlash);
        if (!(new File(imageDirName).isDirectory())) return;
        imageFileName = saveImagePath.substring(lastSlash+1);
        if (imageFileName == null || imageFileName.trim().equals("")) return;
        }
    else
        {
        return;
        }    
    
    ImageWindow iw = null;
    ImageCanvas ic = null;
    AstroCanvas ac = null;
    Image img = null;
    BufferedImage imageDisplay = null;
    Graphics g = null;
    boolean imageFound = false;
    if (openImage != null)
        {
        iw = openImage.getWindow();
        if (iw != null && iw instanceof AstroStackWindow)
            {
            ic = openImage.getCanvas();
            ac = (AstroCanvas)ic;
            g = ic.getGraphics();
            img = ac.graphicsToImage(g);
            imageDisplay = (BufferedImage)(img);
            imageFound = true;
            }
        else 
            {
            return;
            }
        }
    else
        {
        return;
        }

    File saveImageFile = new File(saveImagePath);
    try {
        if (saveImagePath.toLowerCase().endsWith(".jpg"))
            {
            Iterator iter = ImageIO.getImageWritersByFormatName("jpeg");
            javax.imageio.ImageWriter writer = (javax.imageio.ImageWriter)iter.next();
            ImageWriteParam iwp = writer.getDefaultWriteParam();
            iwp.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
            iwp.setCompressionQuality(1.0f);   // an integer between 0 and 1 where 1 specifies minimum compression and maximum quality
            FileImageOutputStream output = new FileImageOutputStream(saveImageFile);
            writer.setOutput(output);
            IIOImage image = new IIOImage(imageDisplay, null, null);
            writer.write(null, image, iwp);
            output.close();
            writer.dispose();
            }
        else if (saveImagePath.toLowerCase().endsWith(".png"))
            {
            ImageIO.write(imageDisplay, "png", saveImageFile);
            }
        }
    catch (IOException ex)
        {
        IJ.error("File Write Error", "Error writing image display to file '"+saveImageFile.toString()+"'");
        } 
    }    
    
void saveStaticPlot()
    {
    if (savePlotPath == null || savePlotPath.trim().equals("")) return;
    String plotDirName = "";
    String plotFileName = "";
    int lastSlash = savePlotPath.lastIndexOf(slash);
    if (lastSlash >= 0)
        {
        plotDirName = savePlotPath.substring(0, lastSlash);
        if (!(new File(plotDirName).isDirectory())) return;
        plotFileName = savePlotPath.substring(lastSlash+1);
        if (plotFileName == null || plotFileName.trim().equals("")) return;
        }
    else
        {
        return;
        }    
    MeasurementTable table = MultiPlot_.getTable();
    if (table != null && savePlotPath.toLowerCase().endsWith(".jpg"))
        {
        ImagePlus image = WindowManager.getImage("Plot of "+MeasurementTable.shorterName(table.shortTitle()));
        if (image == null) return;
        IJ.runPlugIn(image, "ij.plugin.JpegWriter", savePlotPath);
        }
    else if (table != null && savePlotPath.toLowerCase().endsWith(".png"))
        {
        ImagePlus image = WindowManager.getImage("Plot of "+MeasurementTable.shorterName(table.shortTitle()));
        if (image == null) return;
        IJ.runPlugIn(image, "ij.plugin.PNG_Writer", savePlotPath);
        }  
    }

ImageProcessor normalizeImage(ImageProcessor ip, String filename)
    {
    double total = 0;
    double mean = 0;
    int height = ip.getHeight();
    int width = ip.getWidth();
    for (int y=0; y<height; y++)
        {
        for (int x=0; x<width; x++)
            {
            total += (double)(ip.getPixelValue(x, y));
            }
        }
    mean = total /(double)(width*height);
    if (mean == 0)
        {
        return null;
        }
    if (Math.abs(1.0 - mean) > 0.001)
        {
        log("Normalizing "+filename+" with mean = "+uptoFourPlaces.format(mean));
        for (int y=0; y<height; y++)
            {
            for (int x=0; x<width; x++)
                {
                ip.putPixelValue(x, y, (float)((double)ip.getPixelValue(x, y)/mean));
                }
            }  
        }
    return ip;
    }

ImageProcessor removeGradient(ImageProcessor ip) { //Removes the average gradient from an image
    //
    // Adapted from c-code written by John Kielkopf (University of Louisville)
    // as part of the alsvid image processing package - specifically imlevel.c
    //
    double sy2 = 0.0, sfx = 0.0, sxy = 0.0, sfy = 0.0, sx2 = 0.0;
    double sx  = 0.0, sy  = 0.0, sf  = 0.0;
    double a, b, c, d, pixel;
    int x, y, yfits;
    double totpix = 1.0;
    int height = ip.getHeight();
    int width = ip.getWidth();
    yfits = height;
    for (y = 0; y < height; y++)    // Process one row
        {
        yfits--;
        for (x = 0; x < width; x++)   // Process each pixel in a row
            {
            pixel = ip.getPixelValue(x, yfits);
            sy2 += y*y;         // Evaluate the parameters of the least squares image gradient
            sfx += x*pixel;
            sxy += x*y;
            sfy += y*pixel;
            sx2 += x*x;
            sx += x;
            sy += y;
            sf += pixel;
            }
        }

    totpix = width*height;
    sy2 /= totpix;
    sfx /= totpix;
    sxy /= totpix;
    sfy /= totpix;
    sx2 /= totpix;
    sx  /= totpix;
    sy  /= totpix;
    sf  /= totpix;

    // Compute the gradient

    d = (sxy -sx*sy)*(sxy -sx*sy) - (sy2 -sy*sy)*(sx2 - sx*sx);
    b = (sxy - sx*sy)*(sfy - sf*sy) - (sy2 -sy*sy)*(sfx -sf*sx);
    b /= d;
    c = (sxy - sx*sy)*(sfx - sf*sx) - (sx2 -sx*sx)*(sfy -sf*sy);
    c /= d;
    a = sf - b*sx -c*sy;

//    IJ.log("tot="+totpix);
//    IJ.log("sy2="+sy2);
//    IJ.log("sfx="+sfx);
//    IJ.log("sxy="+sxy);
//    IJ.log("sfy="+sfy);
//    IJ.log("sx2="+sx2);
//    IJ.log("sx="+sx);
//    IJ.log("sy="+sy);
//    IJ.log("sf="+sf);
    log("    with coefficients a="+uptoFourPlaces.format(a)+
            ", b="+uptoFourPlaces.format(b)+", c="+uptoFourPlaces.format(c)+
            ", and mean="+uptoFourPlaces.format(sf));


    yfits = height;
    for (y = 0; y < height; y++)     // Loop over rows
        {
        yfits--;
        for(x = 0; x < width; x++)    //Process each pixel in a row
            {
            ip.putPixelValue(x, yfits, (ip.getPixelValue(x, yfits)  - (a + b*x + c*y - sf)));   // Remove the gradient
            }
        }
    return ip;
    }

	/** Sorts the file names into numeric order. EXTRACTED FROM ij.plugin.FolderOpener*/
	String[] sortFileList(String[] list) {
		int listLength = list.length;
		int maxDigits = 30;
		String[] list2 = null;
		char ch;
		for (int i=0; i<listLength; i++) {
			int len = list[i].length();
			String num = "";
			for (int j=0; j<len; j++) {
				ch = list[i].charAt(j);
				if (ch>=48&&ch<=57) num += ch;
			}
			if (list2==null) list2 = new String[listLength];
			if (num.length()==0) num = "aaaaaa";
			num = "000000000000000000000000000000" + num; // prepend maxDigits leading zeroes
			num = num.substring(num.length()-maxDigits);
			list2[i] = num + list[i];
		}
		if (list2!=null) {
			ij.util.StringSorter.sort(list2);
			for (int i=0; i<listLength; i++)
				list2[i] = list2[i].substring(maxDigits);
			return list2;
		} else {
			ij.util.StringSorter.sort(list);
			return list;
		}
	}
    
    void setfileSizeChangeWaitTime()
        {
        GenericDialog gd = new GenericDialog ("Set time to wait to recheck if new file write has completed");

        gd.addMessage ("Before attempting to open a new file that is being written to disk,");
        gd.addMessage ("DP checks for the filesize to stop changing.");
        gd.addMessage ("Set the time to wait to recheck if the file size has stopped changing.");
        gd.addMessage ("The default time is 500 milli-seconds.");
		gd. addNumericField ("Wait time: ",fileSizeChangeWaitTime, 0, 10, "(milli-seconds)");
        gd.addMessage ("");

		gd.showDialog();
		if (gd.wasCanceled()) return;
		fileSizeChangeWaitTime = (int)gd.getNextNumber();
        Prefs.set("dataproc.fileSizeChangeWaitTime", fileSizeChangeWaitTime);
        }

    //extract the integer number within a string
    long stringLongVal(String s){
        long svalue = 0;
        String[] words = numPrefix.split("[\\*?]{1,}");
        String subs = s.substring(0);
        for (int i=0; i<words.length; i++)
            {
            subs = subs.replace(words[i], " ");
            }
        int len = subs.length();
		char ch;
		String numString = "";
        for (int j=0; j<len; j++) {
            ch = subs.charAt(j);
            if (ch>=48&&ch<=57) numString += ch;
        }
		if (numString.length()!=0) svalue = parseLong(numString);
        return svalue;
    }

	long parseLong(String s) {
		if (s==null) return 0L;
        Long value=Long.MAX_VALUE;
		try {
			value = Long.parseLong(s);
		} catch (NumberFormatException e) {}
		return value;
	}

	/**
	 * Checks to see if variables can be modified.
	 */
	synchronized protected boolean lockable ()
		{
		if (blocked == true)
			return false;
		else
			{
			blocked = true;
			return true;
			}
		}

	/**
	 * Releases control of variables.
	 */
	synchronized protected void unlock ()
		{
		blocked = false;
		}

    boolean matchWildCard(String pattern, String text)
        {
        String regex = pattern.replace("?", "[\\w\\-]").replace("*", ".*").replace("-", "\\-");
        return text.matches(regex);
        }

    public class Sexagesimal {
        private final int deg;
        private final int min;
        private final double sec;

        public Sexagesimal(int degrees, int minutes, double seconds) {
            deg = degrees;
            min = minutes;
            sec = seconds;
            }
        }
    
//        /** Handle the key typed event from the image canvas. */
//    public void keyTyped(KeyEvent e) {
//
//    }
//
//    /** Handle the key-pressed event from the image canvas. */
//    public void keyPressed(KeyEvent e) {
//        int keyCode = e.getKeyCode();
//        if (keyCode ==KeyEvent.VK_ESCAPE)
//            {
//            requestStop = true;
//            IJ.log("----escape key pressed----");
//            }
//
//    }
//
//    /** Handle the key-released event from the image canvas. */
//    public void keyReleased(KeyEvent e) {
//
//    }    
    
        void getPrefs()
        {
		mainDir = Prefs.get ("dataproc.mainDir",mainDir);
		filenamePattern = Prefs.get ("dataproc.filenamePattern",filenamePattern);
		preMacroPath = Prefs.get ("dataproc.preMacroPath",preMacroPath);
        postMacroPath = Prefs.get ("dataproc.postMacroPath",postMacroPath);
        runPreMacro = Prefs.get ("dataproc.runPreMacro",runPreMacro);
        runPostMacro = Prefs.get ("dataproc.runPostMacro",runPostMacro);
        deBiasMasterDark = Prefs.get ("dataproc.deBiasMasterDark",deBiasMasterDark);
        runMultiAperture = Prefs.get ("dataproc.runMultiAperture",runMultiAperture);
        runMultiPlot = Prefs.get ("dataproc.runMultiPlot",runMultiPlot);
        saveImage = Prefs.get ("dataproc.saveImage",saveImage);
        savePlot = Prefs.get ("dataproc.savePlot",savePlot);
        onlyNew = Prefs.get ("dataproc.onlyNew",onlyNew);
        useBeep = Prefs.get ("dataproc.useBeep",useBeep);
        showMasters = Prefs.get ("dataproc.showMasters",showMasters);
        showRawCals = Prefs.get ("dataproc.showRawCals",showRawCals);                
        showScience = Prefs.get ("dataproc.showScience",showScience);
        autoRunAndClose = Prefs.get ("dataproc.autoRunAndClose",autoRunAndClose);
        enableFileNumberFiltering =  Prefs.get ("dataproc.enableFileNumberFiltering",enableFileNumberFiltering);
        useGradientRemoval = Prefs.get ("dataproc.useGradientRemoval",useGradientRemoval);
        useCosmicRemoval = Prefs.get ("dataproc.useCosmicRemoval",useCosmicRemoval);
        showLog = Prefs.get ("dataproc.showLog",showLog);
        showLogDateTime = Prefs.get ("dataproc.showLogDateTime",showLogDateTime);
        rawCalCommonDir = Prefs.get ("dataproc.rawCalCommonDir",rawCalCommonDir);
        masterCalCommonDir = Prefs.get ("dataproc.masterCalCommonDir",masterCalCommonDir);
        pollingInterval = (int) Prefs.get ("dataproc.pollingInterval", pollingInterval);
        outlierRadius = (int) Prefs.get ("dataproc.outlierRadius", outlierRadius);
        outlierThreshold = (int) Prefs.get ("dataproc.outlierThreshold", outlierThreshold);
        dialogFrameLocationX = (int) Prefs.get ("dataproc.dialogFrameLocationX",dialogFrameLocationX);
        dialogFrameLocationY = (int) Prefs.get ("dataproc.dialogFrameLocationY",dialogFrameLocationY);
        selectedObjectCoordinateSource = (int) Prefs.get ("dataproc.selectedObjectCoordinateSource",selectedObjectCoordinateSource);
        selectedObservatoryLocationSource = (int) Prefs.get ("dataproc.selectedObservatoryLocationSource",selectedObservatoryLocationSource);
        fileSizeChangeWaitTime = (int) Prefs.get ("dataproc.fileSizeChangeWaitTime",fileSizeChangeWaitTime);
        sortNumerically = Prefs.get ("dataproc.sortNumerically",sortNumerically);
        preMacro1AutoLevel = Prefs.get("dataproc.preMacro1AutoLevel",preMacro1AutoLevel);
        postMacro1AutoLevel = Prefs.get("dataproc.postMacro1AutoLevel",postMacro1AutoLevel);
        postMacro2AutoLevel = Prefs.get("dataproc.postMacro2AutoLevel",postMacro2AutoLevel);
        minFileNumber = (long) Prefs.get ("dataproc.minFileNumber",minFileNumber);
        maxFileNumber = (long) Prefs.get ("dataproc.maxFileNumber",maxFileNumber);
        showToolTips = Prefs.get ("astroIJ.showToolTips",showToolTips);
        autoWildcard = Prefs.get ("astroIJ.autoWildcard",autoWildcard);
        removeBrightOutliers = Prefs.get ("dataproc.removeBrightOutliers",removeBrightOutliers);
        removeDarkOutliers = Prefs.get ("dataproc.removeDarkOutliers",removeDarkOutliers);
        useScienceProcessing  = Prefs.get ("dataproc.useScienceProcessing",useScienceProcessing);
        compress  = Prefs.get ("dataproc.compress",compress);
        useBias = Prefs.get ("dataproc.useBias",useBias);
        useDark = Prefs.get ("dataproc.useDark",useDark);
        useFlat = Prefs.get ("dataproc.useFlat",useFlat);
        useNLC = Prefs.get ("dataproc.useNLC",useNLC);
        scaleExpTime = Prefs.get ("dataproc.scaleExpTime",scaleExpTime);
        createBias = Prefs.get ("dataproc.createBias",createBias);
        createDark = Prefs.get ("dataproc.createDark",createDark);
        createFlat = Prefs.get ("dataproc.createFlat",createFlat);
        saveProcessedData = Prefs.get ("dataproc.saveProcessedData",saveProcessedData);
        biasMedian = Prefs.get ("dataproc.biasMedian",biasMedian);
        darkMedian = Prefs.get ("dataproc.darkMedian",darkMedian);
        flatMedian = Prefs.get ("dataproc.flatMedian",flatMedian);
        saveFloatingPoint = Prefs.get ("dataproc.saveFloatingPoint",saveFloatingPoint);
        biasRawDir = Prefs.get ("dataproc.biasRawDir",biasRawDir);
        darkRawDir = Prefs.get ("dataproc.darkRawDir",darkRawDir);
        flatRawDir = Prefs.get ("dataproc.flatRawDir",flatRawDir);
        biasMasterDir = Prefs.get ("dataproc.biasMasterDir",biasMasterDir);
        darkMasterDir = Prefs.get ("dataproc.darkMasterDir",darkMasterDir);
        flatMasterDir = Prefs.get ("dataproc.flatMasterDir",flatMasterDir);
        biasRawDirText = Prefs.get ("dataproc.biasRawDirText",biasRawDirText);
        darkRawDirText = Prefs.get ("dataproc.darkRawDirText",darkRawDirText);
        flatRawDirText = Prefs.get ("dataproc.flatRawDirText",flatRawDirText);
        biasMasterDirText = Prefs.get ("dataproc.biasMasterDirText",biasMasterDirText);
        darkMasterDirText = Prefs.get ("dataproc.darkMasterDirText",darkMasterDirText);
        flatMasterDirText = Prefs.get ("dataproc.flatMasterDirText",flatMasterDirText);
        saveImagePath = Prefs.get ("dataproc.saveImagePath",saveImagePath);
        savePlotPath = Prefs.get ("dataproc.savePlotPath",savePlotPath);
        saveDir = Prefs.get ("dataproc.saveDir",saveDir);
        biasBase = Prefs.get ("dataproc.biasBase",biasBase);
        darkBase = Prefs.get ("dataproc.darkBase",darkBase);
        flatBase = Prefs.get ("dataproc.flatBase",flatBase);
        numPrefix = Prefs.get ("dataproc.numPrefix",numPrefix);
        saveSuffix = Prefs.get ("dataproc.saveSuffix",saveSuffix);
        saveFormat = Prefs.get ("dataproc.saveFormat",saveFormat);
        coeffA = Prefs.get ("dataproc.coeffA",coeffA);
        coeffB = Prefs.get ("dataproc.coeffB",coeffB);
        coeffC = Prefs.get ("dataproc.coeffC",coeffC);
        coeffD = Prefs.get ("dataproc.coeffD",coeffD);
        biasMaster = Prefs.get ("dataproc.biasMaster",biasMaster);
        darkMaster = Prefs.get ("dataproc.darkMaster",darkMaster);
        flatMaster = Prefs.get ("dataproc.flatMaster",flatMaster);
        calcHeaders = Prefs.get ("dataproc.calcHeaders",calcHeaders);
        plateSolve = Prefs.get ("dataproc.plateSolve",plateSolve);
        objectNameReadKeyword = Prefs.get ("dataproc.objectNameReadKeyword",objectNameReadKeyword);
        objectRAJ2000ReadKeyword = Prefs.get ("dataproc.objectRAJ2000ReadKeyword",objectRAJ2000ReadKeyword);
        objectDecJ2000ReadKeyword = Prefs.get ("dataproc.objectDecJ2000ReadKeyword",objectDecJ2000ReadKeyword);
        observatoryNameReadKeyword = Prefs.get ("dataproc.observatoryNameReadKeyword",observatoryNameReadKeyword);
        observatoryLatReadKeyword = Prefs.get ("dataproc.observatoryLatReadKeyword",observatoryLatReadKeyword);
        observatoryLonReadKeyword = Prefs.get ("dataproc.observatoryLonReadKeyword",observatoryLonReadKeyword);
        objectRAJ2000SaveKeyword = Prefs.get ("dataproc.objectRAJ2000SaveKeyword",objectRAJ2000SaveKeyword);
        objectDecJ2000SaveKeyword = Prefs.get ("dataproc.objectDecJ2000SaveKeyword",objectDecJ2000SaveKeyword);
        objectRAEODSaveKeyword = Prefs.get ("dataproc.objectRAEODSaveKeyword",objectRAEODSaveKeyword);
        objectDecEODSaveKeyword = Prefs.get ("dataproc.objectDecEODSaveKeyword",objectDecEODSaveKeyword);
        objectAltitudeSaveKeyword = Prefs.get ("dataproc.objectAltitudeSaveKeyword",objectAltitudeSaveKeyword);
        objectAzimuthSaveKeyword = Prefs.get ("dataproc.objectAzimuthSaveKeyword",objectAzimuthSaveKeyword);
        objectHourAngleSaveKeyword = Prefs.get ("dataproc.objectHourAngleSaveKeyword",objectHourAngleSaveKeyword);
        objectZenithDistanceSaveKeyword = Prefs.get ("dataproc.objectZenithDistanceSaveKeyword",objectZenithDistanceSaveKeyword);
        objectAirmassSaveKeyword = Prefs.get ("dataproc.objectAirmassSaveKeyword",objectAirmassSaveKeyword);
        JD_SOBSSaveKeyword = Prefs.get ("dataproc.JD_SOBSSaveKeyword",JD_SOBSSaveKeyword);
        JD_MOBSSaveKeyword = Prefs.get ("dataproc.JD_MOBSSaveKeyword",JD_MOBSSaveKeyword);
        HJD_MOBSSaveKeyword = Prefs.get ("dataproc.HJD_MOBSSaveKeyword",HJD_MOBSSaveKeyword);
        BJD_MOBSSaveKeyword = Prefs.get ("dataproc.BJD_MOBSSaveKeyword",BJD_MOBSSaveKeyword);
        observatoryLatSaveKeyword = Prefs.get ("dataproc.observatoryLatSaveKeyword",observatoryLatSaveKeyword);
        observatoryLonSaveKeyword = Prefs.get ("dataproc.observatoryLonSaveKeyword",observatoryLonSaveKeyword);
        saveObjectRAJ2000 = Prefs.get ("dataproc.saveObjectRAJ2000",saveObjectRAJ2000);
        saveObjectDecJ2000 = Prefs.get ("dataproc.saveObjectDecJ2000",saveObjectDecJ2000);
        saveObjectRAEOD = Prefs.get ("dataproc.saveObjectRAEOD",saveObjectRAEOD);
        saveObjectDecEOD = Prefs.get ("dataproc.saveObjectDecEOD",saveObjectDecEOD);
        saveObjectAltitude = Prefs.get ("dataproc.saveObjectAltitude",saveObjectAltitude);
        saveObjectAzimuth = Prefs.get ("dataproc.saveObjectAzimuth",saveObjectAzimuth);
        saveObjectHourAngle = Prefs.get ("dataproc.saveObjectHourAngle",saveObjectHourAngle);
        saveObjectZenithDistance = Prefs.get ("dataproc.saveObjectZenithDistance",saveObjectZenithDistance);
        saveObjectAirmass = Prefs.get ("dataproc.saveObjectAirmass",saveObjectAirmass);
        saveJD_SOBS = Prefs.get ("dataproc.saveJD_SOBS",saveJD_SOBS);
        saveJD_MOBS = Prefs.get ("dataproc.saveJD_MOBS",saveJD_MOBS);
        saveHJD_MOBS = Prefs.get ("dataproc.saveHJD_MOBS",saveHJD_MOBS);
        saveBJD_MOBS = Prefs.get ("dataproc.saveBJD_MOBS",saveBJD_MOBS);
        saveObservatoryLat = Prefs.get ("dataproc.saveObservatoryLat",saveObservatoryLat);
        saveObservatoryLon = Prefs.get ("dataproc.saveObservatoryLon",saveObservatoryLon);
        raInDegrees = Prefs.get ("dataproc.raInDegrees",raInDegrees);
        latNegate = Prefs.get ("dataproc.latNegate",latNegate);
        lonNegate = Prefs.get ("dataproc.lonNegate",lonNegate);
        }

        void savePrefs() {

        Prefs.set ("dataproc.mainDir",mainDir);
        Prefs.set ("dataproc.filenamePattern",filenamePattern);
        Prefs.set ("dataproc.preMacroPath",preMacroPath);
        Prefs.set ("dataproc.postMacroPath",postMacroPath);
        Prefs.set ("dataproc.runPreMacro",runPreMacro);
        Prefs.set ("dataproc.runPostMacro",runPostMacro);
        Prefs.set ("dataproc.deBiasMasterDark",deBiasMasterDark);
        Prefs.set ("dataproc.runMultiAperture",runMultiAperture);
        Prefs.set ("dataproc.runMultiPlot",runMultiPlot);
        Prefs.set ("dataproc.savePlot",savePlot);
        Prefs.set ("dataproc.saveImage",saveImage);
        Prefs.set ("dataproc.onlyNew",onlyNew);
        Prefs.set ("dataproc.pollingInterval",pollingInterval);
        Prefs.set ("dataproc.outlierRadius", outlierRadius);
        Prefs.set ("dataproc.outlierThreshold", outlierThreshold);        
        Prefs.set ("dataproc.useBeep",useBeep);
        Prefs.set ("dataproc.showMasters",showMasters);
        Prefs.set ("dataproc.showRawCals",showRawCals);
        Prefs.set ("dataproc.showScience",showScience);
        Prefs.set ("dataproc.autoRunAndClose",autoRunAndClose);
        Prefs.set ("dataproc.useGradientRemoval",useGradientRemoval);
        Prefs.set ("dataproc.useCosmicRemoval",useCosmicRemoval);
        Prefs.set ("dataproc.showLog",showLog);
        Prefs.set ("dataproc.showLogDateTime",showLogDateTime);
        Prefs.set ("dataproc.sortNumerically",sortNumerically);
        Prefs.set ("dataproc.rawCalCommonDir",rawCalCommonDir);
        Prefs.set ("dataproc.masterCalCommonDir",masterCalCommonDir);
        Prefs.set ("dataproc.enableFileNumberFiltering",enableFileNumberFiltering);
        dialogFrameLocationX = dialogFrame.getLocation().x;
        dialogFrameLocationY = dialogFrame.getLocation().y;
        Prefs.set ("dataproc.dialogFrameLocationX",dialogFrameLocationX);
        Prefs.set ("dataproc.dialogFrameLocationY",dialogFrameLocationY);
        Prefs.set ("dataproc.selectedObjectCoordinateSource",selectedObjectCoordinateSource);
        Prefs.set ("dataproc.selectedObservatoryLocationSource",selectedObservatoryLocationSource);
        Prefs.set ("dataproc.fileSizeChangeWaitTime",fileSizeChangeWaitTime);
        Prefs.set ("dataproc.preMacro1AutoLevel",preMacro1AutoLevel);
        Prefs.set ("dataproc.postMacro1AutoLevel",postMacro1AutoLevel);
        Prefs.set ("dataproc.postMacro2AutoLevel",postMacro2AutoLevel);
        Prefs.set ("dataproc.minFileNumber",minFileNumber);
        Prefs.set ("dataproc.maxFileNumber",maxFileNumber);
        Prefs.set ("astroIJ.showToolTips",showToolTips);
        Prefs.set ("astroIJ.autoWildcard",autoWildcard);
        Prefs.set ("dataproc.removeBrightOutliers",removeBrightOutliers);
        Prefs.set ("dataproc.removeDarkOutliers",removeDarkOutliers);
        Prefs.set ("dataproc.useScienceProcessing",useScienceProcessing);
        Prefs.set ("dataproc.compress",compress);
        Prefs.set ("dataproc.useBias",useBias);
        Prefs.set ("dataproc.useDark",useDark);
        Prefs.set ("dataproc.useFlat",useFlat);
        Prefs.set ("dataproc.useNLC",useNLC);
        Prefs.set ("dataproc.scaleExpTime",scaleExpTime);
        Prefs.set ("dataproc.createBias",createBias);
        Prefs.set ("dataproc.createDark",createDark);
        Prefs.set ("dataproc.createFlat",createFlat);
        Prefs.set ("dataproc.saveProcessedData",saveProcessedData);
        Prefs.set ("dataproc.biasMedian",biasMedian);
        Prefs.set ("dataproc.darkMedian",darkMedian);
        Prefs.set ("dataproc.flatMedian",flatMedian);
        Prefs.set ("dataproc.saveFloatingPoint",saveFloatingPoint);
        Prefs.set ("dataproc.biasRawDir",biasRawDir);
        Prefs.set ("dataproc.darkRawDir",darkRawDir);
        Prefs.set ("dataproc.flatRawDir",flatRawDir);
        Prefs.set ("dataproc.biasMasterDir",biasMasterDir);
        Prefs.set ("dataproc.darkMasterDir",darkMasterDir);
        Prefs.set ("dataproc.flatMasterDir",flatMasterDir);
        Prefs.set ("dataproc.biasRawDirText",biasRawDirField.getText());
        Prefs.set ("dataproc.darkRawDirText",darkRawDirField.getText());
        Prefs.set ("dataproc.flatRawDirText",flatRawDirField.getText());
        Prefs.set ("dataproc.biasMasterDirText",biasMasterDirField.getText());
        Prefs.set ("dataproc.darkMasterDirText",darkMasterDirField.getText());
        Prefs.set ("dataproc.flatMasterDirText",flatMasterDirField.getText());
        Prefs.set ("dataproc.saveImagePath",saveImagePath);
        Prefs.set ("dataproc.savePlotPath",savePlotPath);
        Prefs.set ("dataproc.saveDir",saveDir);
        Prefs.set ("dataproc.biasBase",biasBase);
        Prefs.set ("dataproc.darkBase",darkBase);
        Prefs.set ("dataproc.flatBase",flatBase);
        Prefs.set ("dataproc.numPrefix",numPrefix);
        Prefs.set ("dataproc.saveSuffix",saveSuffix);
        Prefs.set ("dataproc.saveFormat",saveFormat);
        Prefs.set ("dataproc.coeffA",coeffA);
        Prefs.set ("dataproc.coeffB",coeffB);
        Prefs.set ("dataproc.coeffC",coeffC);
        Prefs.set ("dataproc.coeffD",coeffD);
        Prefs.set ("dataproc.biasMaster",biasMaster);
        Prefs.set ("dataproc.darkMaster",darkMaster);
        Prefs.set ("dataproc.flatMaster",flatMaster);
        Prefs.set ("dataproc.calcHeaders",calcHeaders);
        Prefs.set ("dataproc.plateSolve",plateSolve);
        Prefs.set ("dataproc.objectNameReadKeyword",objectNameReadKeyword);
        Prefs.set ("dataproc.objectRAJ2000ReadKeyword",objectRAJ2000ReadKeyword);
        Prefs.set ("dataproc.objectDecJ2000ReadKeyword",objectDecJ2000ReadKeyword);
        Prefs.set ("dataproc.observatoryNameReadKeyword",observatoryNameReadKeyword);
        Prefs.set ("dataproc.observatoryLatReadKeyword",observatoryLatReadKeyword);
        Prefs.set ("dataproc.observatoryLonReadKeyword",observatoryLonReadKeyword);
        Prefs.set ("dataproc.objectRAJ2000SaveKeyword",objectRAJ2000SaveKeyword);
        Prefs.set ("dataproc.objectDecJ2000SaveKeyword",objectDecJ2000SaveKeyword);
        Prefs.set ("dataproc.objectRAEODSaveKeyword",objectRAEODSaveKeyword);
        Prefs.set ("dataproc.objectDecEODSaveKeyword",objectDecEODSaveKeyword);
        Prefs.set ("dataproc.objectAltitudeSaveKeyword",objectAltitudeSaveKeyword);
        Prefs.set ("dataproc.objectAzimuthSaveKeyword",objectAzimuthSaveKeyword);
        Prefs.set ("dataproc.objectHourAngleSaveKeyword",objectHourAngleSaveKeyword);
        Prefs.set ("dataproc.objectZenithDistanceSaveKeyword",objectZenithDistanceSaveKeyword);        
        Prefs.set ("dataproc.objectAirmassSaveKeyword",objectAirmassSaveKeyword);
        Prefs.set ("dataproc.JD_SOBSSaveKeyword",JD_SOBSSaveKeyword);
        Prefs.set ("dataproc.JD_MOBSSaveKeyword",JD_MOBSSaveKeyword);
        Prefs.set ("dataproc.HJD_MOBSSaveKeyword",HJD_MOBSSaveKeyword);
        Prefs.set ("dataproc.BJD_MOBSSaveKeyword",BJD_MOBSSaveKeyword);
        Prefs.set ("dataproc.observatoryLatSaveKeyword",observatoryLatSaveKeyword);
        Prefs.set ("dataproc.observatoryLonSaveKeyword",observatoryLonSaveKeyword);
        Prefs.set ("dataproc.saveObjectRAJ2000",saveObjectRAJ2000);
        Prefs.set ("dataproc.saveObjectDecJ2000",saveObjectDecJ2000);
        Prefs.set ("dataproc.saveObjectRAEOD",saveObjectRAEOD);
        Prefs.set ("dataproc.saveObjectDecEOD",saveObjectDecEOD);
        Prefs.set ("dataproc.saveObjectAltitude",saveObjectAltitude);
        Prefs.set ("dataproc.saveObjectAzimuth",saveObjectAzimuth);
        Prefs.set ("dataproc.saveObjectHourAngle",saveObjectHourAngle);
        Prefs.set ("dataproc.saveObjectZenithDistance",saveObjectZenithDistance);        
        Prefs.set ("dataproc.saveObjectAirmass",saveObjectAirmass);
        Prefs.set ("dataproc.saveJD_SOBS",saveJD_SOBS);
        Prefs.set ("dataproc.saveJD_MOBS",saveJD_MOBS);
        Prefs.set ("dataproc.saveHJD_MOBS",saveHJD_MOBS);
        Prefs.set ("dataproc.saveBJD_MOBS",saveBJD_MOBS);
        Prefs.set ("dataproc.saveObservatoryLat",saveObservatoryLat);
        Prefs.set ("dataproc.saveObservatoryLon",saveObservatoryLon);
        Prefs.set ("dataproc.raInDegrees",raInDegrees);
        Prefs.set ("dataproc.latNegate",latNegate);
        Prefs.set ("dataproc.lonNegate",lonNegate);
        }

	}
