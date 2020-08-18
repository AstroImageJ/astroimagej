// AstrometrySetup.java

package astroj;

import java.awt.*;
import java.util.*;

import ij.*;
import ij.util.Tools;
import java.awt.event.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;


import java.text.DecimalFormat;
import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.event.*;



public class AstrometrySetup implements ActionListener, ItemListener, ChangeListener, FocusListener, MouseWheelListener
	{
    
    DecimalFormat threeToLeft = new DecimalFormat("000", IJU.dfs);
    DecimalFormat twoPlaces = new DecimalFormat("0.00", IJU.dfs);
    DecimalFormat fourPlaces = new DecimalFormat("0.0000", IJU.dfs);
    DecimalFormat sixPlaces = new DecimalFormat("0.000000", IJU.dfs);
    DecimalFormat uptoFourPlaces = new DecimalFormat("0.####", IJU.dfs);
    DecimalFormat upto20Places = new DecimalFormat("0.####################", IJU.dfs);
    
//    DecimalFormatSymbols symbols = fourPlaces.getDecimalFormatSymbols();
//    char decSep = symbols.getDecimalSeparator(); 
//    char thouSep = symbols.getGroupingSeparator();     

    Color mainBorderColor = new Color(118,142,229);
    Color red = new Color(255,160,160);
    Color yellow = new Color(255,255,200);
    Color green = new Color(225,255,225);
    Color gray = new Color(240,240,240);
    Color subBorderColor = Color.LIGHT_GRAY;
    
    String fontName = "Dialog";
    Font p8 = new Font(fontName,Font.PLAIN,8);
    Font p12 = new Font(fontName,Font.PLAIN,12);
    Font b12 = new Font(fontName,Font.BOLD,12);
    Font b11 = new Font(fontName,Font.BOLD,11);
    Font p11 = new Font(fontName,Font.PLAIN,11);
    Font b14 = new Font(fontName,Font.BOLD,14);    
    
    int frameLocationX = -9999999;
    int frameLocationY = -9999999;
    int nlines = 0;
    
    String[] spinnerScaleList = new String[]
                {
                "      0.0000000001",
                "        0.000000001",
                "          0.00000001",
                "            0.0000001",
                "              0.000001",
                "                0.00001",
                "                  0.0001",
                "                    0.001",
                "                    0.010",
                "                    0.100",
                "                    0.500",
                "                    1.000",
                "                  10.000",
                "                100.000",
                "              1000.000",
                "            10000.000",
                "          100000.000",
                "        1000000.000",
                "      10000000.000",
                "    100000000.000",
                "1000000000.000",
                };    
    
    boolean notDP = false;
    boolean processStack = true;
    boolean done = false;
    boolean canceled = false;
    boolean useCCRaDec = false;
    int startSlice = 1;
    int endSlice = 1;    
    
    String userKey = "";
    
    boolean autoSave = false;
    boolean DPSaveRawWithWCS = false;
    
    boolean skipIfHasWCS = false;
    
    boolean annotate = true;
    double annotateRadius = 30;
    boolean addAnnotationsToHeader = true;
    
    boolean useMedianFilter = true;
    int medianFilterRadius = 2;
    
    double minPeakFindToleranceSTDEV = 1;
    boolean useMaxPeakFindValue = false;
    double maxPeakFindValue = 50000;
    
    int maxNumStars = 50;
    
    boolean useCentroid = false;
    double apertureRadius = 20;
    double apertureBack1 = 30;
    double apertureBack2 = 40;
    
    boolean useScale = false;
    double scaleEstimate = 0.5;  //arcsecperpixel
    double scaleError = 0.25;   //arcsecperpixel   

    
    boolean useRaDec = false;
    double ra = 0;  //degrees
    double dec = 0;  //degrees
    double raDecRadius = 40.0;  //minutes
    double raDecRadiusStep = 1.0;  //minutes
    
    boolean showLog = false;
    
    double annotateRadiusStep = 1.0;
    double noiseTolStep = 1.0;
    double maxPeakFindStep = 1000.0;
    double apertureStep = 1.0;
    double scaleStep = 0.1;
    
    boolean useDistortionOrder = true;
    int distortionOrder = 2;
    int minOrder = 2;
    int maxOrder = 5;
    
    int fSlice = 1, cSlice = 1, lSlice = 1;
    
    boolean showSexagesimal = true;
    
    boolean useAlternateAstrometryServer = false;
    String defaultAstrometryUrlBase = "http://nova.astrometry.net";
    String alternateAstrometryUrlBase = "http://127.0.0.1:8080";
        
    public JFrame astrometrySetupFrame;
    JScrollPane scrollPane;
    JPanel astrometrySetupPanel;
    JTextField keyTF, raTF, decTF, alternateAstrometryUrlBaseTF;
    JButton startButton, cancelButton;
    JLabel keyLabel3, keyLabel4;
    JCheckBox autoSaveCB, skipIfHasWCSCB, processStackCB, annotateCB, addAnnotationsToHeaderCB, useMedianFilterCB, useMaxPeakFindValueCB,
              centroidCB, scaleCB, raDecCB, showLogCB, useDistortionOrderCB, useAlternateAstrometryServerCB;
    SpinnerNumberModel startSliceNumberModel, endSliceNumberModel, medianFilterRadiusNumberModel, distortionOrderNumberModel,
                       noiseTolNumberModel, maxPeakFindNumberModel, maxNumStarsNumberModel,
                       annotateRadiusNumberModel, apertureRadiusNumberModel, apertureBack1NumberModel,apertureBack2NumberModel,
                       scaleEstimateNumberModel, scaleErrorNumberModel, raDecRadiusNumberModel;
    JSpinner startSliceSpinner, endSliceSpinner, medianFilterRadiusSpinner, noiseTolStepSpinner,
             noiseTolSpinner, maxPeakFindSpinner, maxPeakFindStepSpinner, maxNumStarsSpinner, annotateRadiusSpinner,
             annotateRadiusStepSpinner, apertureRadiusSpinner, apertureBack1Spinner, apertureBack2Spinner, apertureStepSpinner,
             scaleEstimateSpinner, scaleErrorSpinner, scaleStepSpinner, raDecRadiusSpinner, raDecRadiusStepSpinner, distortionOrderSpinner;
    JPopupMenu annotateRadiusStepPopup, noiseTolStepPopup, maxPeakFindStepPopup, apertureStepPopup, scaleStepPopup, raDecRadiusStepPopup;
    
    static int MIN_MAX_STARS = 10;
    
    Rectangle defaultScreenBounds;
    GraphicsDevice defaultScreen;    

    
	public AstrometrySetup ()
		{
        Locale.setDefault(IJU.locale);
		}

	public boolean start(int firstSlice, int currentSlice, int lastSlice, String startButtonLabel, AstroConverter astroCC, boolean useSexagesimal)
		{
        if (IJ.isWindows())
                {
//                        try {UIManager.setLookAndFeel("javax.swing.plaf.metal.MetalLookAndFeel");}
                try {UIManager.setLookAndFeel("com.sun.java.swing.plaf.windows.WindowsLookAndFeel");}
//                        try {UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());}
                catch (Exception e) { }
                }
        else if (IJ.isLinux())
                {
                try {UIManager.setLookAndFeel("javax.swing.plaf.metal.MetalLookAndFeel");}
                catch (Exception e) { }
                }
//                    try {UIManager.setLookAndFeel("com.sun.java.swing.plaf.windows.WindowsLookAndFeel");}
//                    try {UIManager.setLookAndFeel("com.sun.java.swing.plaf.nimbus.NimbusLookAndFeel");}
//                    try {UIManager.setLookAndFeel("com.sun.java.swing.plaf.motif.MotifLookAndFeel");}
        else if (IJ.isMacOSX())
                {

                try {UIManager.setLookAndFeel("javax.swing.plaf.metal.MetalLookAndFeel");}
//                        try {UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());}
                catch (Exception e) { }
                }
//                System.setProperty("apple.laf.useScreenMenuBar", "false");
//                System.setProperty("com.apple.macos.useScreenMenuBar", "false");

        GraphicsEnvironment localGfxEnvironment = GraphicsEnvironment.getLocalGraphicsEnvironment();
        defaultScreen = localGfxEnvironment.getDefaultScreenDevice();
        defaultScreenBounds = defaultScreen.getDefaultConfiguration().getBounds();          
        
        useCCRaDec = (astroCC != null);
        notDP = startButtonLabel.equals("START");
        getPrefs();
        
        if (useCCRaDec)
            {
            ra = astroCC.getRAJ2000();
            dec = astroCC.getDecJ2000();
            }
        
        startSlice = firstSlice;
        endSlice = lastSlice;  
        fSlice = firstSlice;
        cSlice = currentSlice;
        lSlice = lastSlice;
        showSexagesimal = useSexagesimal;

        if (!IJ.isWindows())
            {
            p8  = new Font(fontName,Font.PLAIN,8);
            p12 = new Font(fontName, Font.PLAIN,11);
            b12 = new Font(fontName, Font.BOLD,11);
            b11 = new Font(fontName, Font.BOLD,10);
            p11 = new Font(fontName, Font.PLAIN,10);
            b14 = new Font(fontName, Font.BOLD,13);
            }

        Dimension col1Size = new Dimension(140, 35);
        Dimension col1bSize = new Dimension(140, 20);
        Dimension col2Size = new Dimension(140, 25);
        Dimension col2bSize = new Dimension(140, 20);
        Dimension col3Size = new Dimension(140, 25);
        Dimension col4Size = new Dimension(140, 25);
        Dimension col5Size = new Dimension(140, 25);
        
        Insets fitsMargin = new Insets(2,2,2,2); //top,left,bottom,right
        Insets labelMargin = new Insets(4,2,4,2); //top,left,bottom,right

        int iconWidth = 40;
        int iconHeight = 22;
        Dimension iconSize = new Dimension(iconWidth, iconHeight);
        Insets buttonMargin = new Insets(0,0,0,0); //top,left,bottom,right        
        

        
        ImageIcon astrometrySetupIcon = createImageIcon("images/astrometry.png", "Astrometry Setup Icon");
        
        astrometrySetupFrame = new JFrame ((notDP ? "" : "DP ")+"Astrometry Settings");
        astrometrySetupFrame.setIconImage(astrometrySetupIcon.getImage());
        astrometrySetupFrame.addWindowListener(new WindowAdapter(){
            @Override
            public void windowClosing(WindowEvent e){
                canceled = true;
                exit();
                done = true;
                }});
        
        astrometrySetupPanel = new JPanel(new SpringLayout());
        
//-------------------------------------------------------------------
        nlines++;
        JLabel keyLabel = new JLabel ("User Key:");
        keyLabel.setPreferredSize(col1Size);
        keyLabel.setFont(p12);
        keyLabel.setHorizontalAlignment (JTextField.RIGHT);
		astrometrySetupPanel.add (keyLabel);

        JPanel keyPanel = new JPanel(new SpringLayout());
		keyTF = new JTextField (userKey);
        keyTF.setMargin(fitsMargin);
        keyTF.setFont(p12);
		keyTF.setPreferredSize(col2Size);
		keyTF.setHorizontalAlignment(JTextField.LEFT);
        keyTF.setToolTipText("<html>"+"Enter User Key for the astrometry.net website."+"<br>"+
                             "Go to <b>nova.astrometry.net</b> to get user key."+"<br>"+
                             "This is only required to be done once."+"</html>");
        keyTF.getDocument().addDocumentListener(new AstrometrySetup.thisDocumentListener());
        keyPanel.add(keyTF);
        SpringUtil.makeCompactGrid (keyPanel, 1,1, 2,4,2,4);
        astrometrySetupPanel.add (keyPanel);

        keyLabel3 = new JLabel ("<html><b>(Get key from: </html>");
        keyLabel3.setPreferredSize(col3Size);
        keyLabel3.setHorizontalAlignment(JLabel.RIGHT);
		astrometrySetupPanel.add (keyLabel3);
        
        keyLabel4 = new JLabel ("<html><b>nova.astrometry.net</b>)</html>");
        keyLabel4.setPreferredSize(col4Size);
		astrometrySetupPanel.add (keyLabel4);  
        
        JLabel keyLabel5 = new JLabel ("");
        keyLabel5.setPreferredSize(col5Size);
		astrometrySetupPanel.add (keyLabel5);          

//-------------------------------------------------------------------

        nlines++;
        JLabel astrometryServerLabel = new JLabel ("Use Custom Server:");
        astrometryServerLabel.setFont(p12);
        astrometryServerLabel.setPreferredSize(col1Size);
        astrometryServerLabel.setHorizontalAlignment (JTextField.RIGHT);
        astrometrySetupPanel.add (astrometryServerLabel);

        useAlternateAstrometryServerCB = new JCheckBox("Enable",useAlternateAstrometryServer);
        useAlternateAstrometryServerCB.setFont(p12);
        useAlternateAstrometryServerCB.setToolTipText("<html>Enable the use of a custom/local Astrometry.net server.</html>");

        useAlternateAstrometryServerCB.addItemListener (this);
        astrometrySetupPanel.add(useAlternateAstrometryServerCB);
        
        JPanel astronomyServerPanel = new JPanel(new SpringLayout());
		alternateAstrometryUrlBaseTF = new JTextField (useAlternateAstrometryServer?alternateAstrometryUrlBase:defaultAstrometryUrlBase);
        alternateAstrometryUrlBaseTF.setMargin(fitsMargin);
        alternateAstrometryUrlBaseTF.setFont(p12);
        alternateAstrometryUrlBaseTF.setEnabled(useAlternateAstrometryServer);
		alternateAstrometryUrlBaseTF.setPreferredSize(col2Size);
		alternateAstrometryUrlBaseTF.setHorizontalAlignment(JTextField.LEFT);
        alternateAstrometryUrlBaseTF.setToolTipText("<html>"+"Enter custom/local astrometry.net URL base address."+"<br>"+
                                                    "Example: <b>http://127.0.0.1:8080</b>"+"</html>");
        alternateAstrometryUrlBaseTF.getDocument().addDocumentListener(new AstrometrySetup.thisDocumentListener());
        astronomyServerPanel.add(alternateAstrometryUrlBaseTF);
        SpringUtil.makeCompactGrid (astronomyServerPanel, 1,1, 2,4,2,4);
        astrometrySetupPanel.add (astronomyServerPanel);        

        JLabel serverLabel4 = new JLabel ("");
        astrometrySetupPanel.add (serverLabel4);  

        JLabel serverLabel5 = new JLabel ("");
        astrometrySetupPanel.add (serverLabel5);                    
        
//-------------------------------------------------------------------

        nlines++;
        JLabel autoSaveLabel = new JLabel (notDP?"Auto Save:":"Re-save Raw Science:");
//        autoSaveLabel.setPreferredSize(labelSize);
        autoSaveLabel.setFont(p12);
        autoSaveLabel.setPreferredSize(col1Size);
        autoSaveLabel.setHorizontalAlignment (JTextField.RIGHT);
        astrometrySetupPanel.add (autoSaveLabel);

        autoSaveCB = new JCheckBox("Enable",notDP?autoSave:DPSaveRawWithWCS);
        autoSaveCB.setFont(p12);
        if (notDP)
            {
            autoSaveCB.setToolTipText("<html>Automatically saves image(s) with new headers to the original file(s).<br>"+
                                    "WARNING: this option will overwrite the existing image file(s).<br>"+
                                    "WARNING: any previous changes to an image will also be saved over the original image.</html>");
            }
        else
            {
            autoSaveCB.setToolTipText("<html>Re-saves raw uncalibrated science image with new WCS headers to the original file.<br>"+
                                    "WARNING: this option will re-write the original uncalibrated science image with new WCS headers.<br>"+
                                    "WCS headers will be added to calibrated science image independent of this setting.</html>");       
            }
//        autoSaveCB.setPreferredSize (checkBoxSize);
        autoSaveCB.addItemListener (this);
        astrometrySetupPanel.add(autoSaveCB);

        JLabel autoSaveLabel3 = new JLabel ("<html><b>IMPORTANT WARNING: </b></html>");
//        autoSaveLabel3.setPreferredSize(fitsDummySize);
        autoSaveLabel3.setHorizontalAlignment(JLabel.RIGHT);
        astrometrySetupPanel.add (autoSaveLabel3);

        JLabel autoSaveLabel4 = new JLabel (notDP?"<html><b>overwrites original image</b></html>":"<html><b>re-writes raw science file</b></html>");
//        autoSaveLabel4.setPreferredSize(fitsDummySize);
        astrometrySetupPanel.add (autoSaveLabel4);  

        JLabel autoSaveLabel5 = new JLabel (notDP?"":"<html><b>(with WCS headers)</b></html>");
//        autoSaveLabel4.setPreferredSize(fitsDummySize);
        astrometrySetupPanel.add (autoSaveLabel5);             

        
        
//-------------------------------------------------------------------
        
        nlines++;
        JLabel skipLabel = new JLabel("Skip Images With WCS:");
//        skipLabel.setPreferredSize(labelSize);
        skipLabel.setFont(p12);
        skipLabel.setPreferredSize(col1Size);
        skipLabel.setHorizontalAlignment (JTextField.RIGHT);
        astrometrySetupPanel.add (skipLabel);

        skipIfHasWCSCB = new JCheckBox("Enable",skipIfHasWCS);
        skipIfHasWCSCB.setFont(p12);
        skipIfHasWCSCB.setToolTipText("<html>If enabled, images/slices that already have valid FITS WCS headers will be skipped.<br>"+
                                      "A new plate solve will not be performed and the existing WCS headers will be retained.</html>");
//        autoSaveCB.setPreferredSize (checkBoxSize);
        skipIfHasWCSCB.addItemListener (this);
        astrometrySetupPanel.add(skipIfHasWCSCB);

        JLabel skipLabel3 = new JLabel ("");
//        skipLabel3.setPreferredSize(fitsDummySize);
        skipLabel3.setHorizontalAlignment(JLabel.RIGHT);
        astrometrySetupPanel.add (skipLabel3);

        JLabel skipLabel4 = new JLabel ("");
//        skipLabel4.setPreferredSize(fitsDummySize);
        astrometrySetupPanel.add (skipLabel4);  

        JLabel skipLabel5 = new JLabel ("");
//        skipLabel5.setPreferredSize(fitsDummySize);
        astrometrySetupPanel.add (skipLabel5);             
     
        
//-------------------------------------------------------------------

        nlines++;
        
        JPanel annotateLabelPanel = new JPanel(new SpringLayout());
        JLabel annotateLabel = new JLabel ("Annotate:");
//        autoSaveLabel.setPreferredSize(labelSize);
        annotateLabel.setToolTipText("<html>Label objects supported by astrometry.net after a successful plate solve</html>");
        annotateLabel.setFont(p12);
        annotateLabel.setHorizontalAlignment (JTextField.RIGHT);
        annotateLabel.setPreferredSize(col1bSize);
        annotateLabelPanel.add (annotateLabel);
        
        JLabel annotateHeaderLabel = new JLabel ("Add To Header:");
//        autoSaveLabel.setPreferredSize(labelSize);
        annotateHeaderLabel.setFont(p12);
        annotateHeaderLabel.setToolTipText("<html>Add annotations to FITS header</html>");
        annotateHeaderLabel.setHorizontalAlignment (JTextField.RIGHT);
        annotateHeaderLabel.setPreferredSize(col1bSize);
        annotateLabelPanel.add (annotateHeaderLabel); 
        
        SpringUtil.makeCompactGrid (annotateLabelPanel, 2,1, 0,0,0,0);
        astrometrySetupPanel.add (annotateLabelPanel);

        JPanel annotateCBPanel = new JPanel(new SpringLayout());
        annotateCB = new JCheckBox("Enable",annotate);
        annotateCB.setFont(p12);
        annotateCB.setToolTipText("<html>Label objects supported by astrometry.net after a successful plate solve</html>");
        annotateCB.setPreferredSize (col2bSize);
        annotateCB.addItemListener (this);
        annotateCBPanel.add(annotateCB);        

        addAnnotationsToHeaderCB = new JCheckBox("Enable",addAnnotationsToHeader);
        addAnnotationsToHeaderCB.setFont(p12);
        addAnnotationsToHeaderCB.setToolTipText("<html>Add annotations to FITS header</html>");
        addAnnotationsToHeaderCB.setPreferredSize (col2bSize);
        addAnnotationsToHeaderCB.addItemListener (this);
        annotateCBPanel.add(addAnnotationsToHeaderCB); 
        
        SpringUtil.makeCompactGrid (annotateCBPanel, 2,1, 0,0,0,0);
        astrometrySetupPanel.add(annotateCBPanel); 

        annotateRadiusStepPopup = new JPopupMenu();
        JPanel annotateRadiusStepPanel = new JPanel();
        SpinnerListModel annotateRadiusStepModel = new SpinnerListModel(spinnerScaleList);
        annotateRadiusStepSpinner = new JSpinner(annotateRadiusStepModel);
        annotateRadiusStepSpinner.setValue(convertToText(annotateRadiusStep));
        annotateRadiusStepSpinner.addChangeListener (new ChangeListener()
            {
            public void stateChanged(ChangeEvent ev)
                {
                double value = IJU.getTextSpinnerDoubleValue(annotateRadiusStepSpinner);
                if (Double.isNaN(value)) return;
                annotateRadiusStep = value;
                annotateRadiusSpinner.setModel(new SpinnerNumberModel(new Double(annotateRadius), new Double(1.0), null, new Double(annotateRadiusStep)));
//                annotateRadiusSpinner.setEditor(new JSpinner.NumberEditor(annotateRadiusSpinner, "0.00"));
//                IJU.setSpinnerFormat(annotateRadiusSpinner, "0.00"); 
                }
            });

        JLabel annotateRadiusStepLabel = new JLabel ("Stepsize:");
        annotateRadiusStepPanel.add(annotateRadiusStepLabel);
        annotateRadiusStepPanel.add(annotateRadiusStepSpinner);
        annotateRadiusStepPopup.add(annotateRadiusStepPanel);
        annotateRadiusStepPopup.setLightWeightPopupEnabled(false);
        annotateRadiusStepPopup.addPopupMenuListener (new PopupMenuListener()
            {
            public void popupMenuCanceled(PopupMenuEvent popupMenuEvent) {
                    annotateRadiusStepPopup.setVisible(false);
                    }

            public void popupMenuWillBecomeInvisible(PopupMenuEvent popupMenuEvent) {
                    annotateRadiusStepPopup.setVisible(true);
                    }

            public void popupMenuWillBecomeVisible(PopupMenuEvent popupMenuEvent) {
                    annotateRadiusStepPopup.setVisible(false);
                    }
            });

        JPanel annotateRadiusPanel = new JPanel(new SpringLayout());
        TitledBorder annotateRadiusTitle = BorderFactory.createTitledBorder(BorderFactory.createEmptyBorder(),"Radius (pixels)", TitledBorder.CENTER, TitledBorder.TOP, p11);
        annotateRadiusPanel.setBorder(annotateRadiusTitle);

        
        annotateRadiusNumberModel = new SpinnerNumberModel(new Double(annotateRadius), new Double(1.0), null, new Double(annotateRadiusStep));
        annotateRadiusSpinner = new JSpinner(annotateRadiusNumberModel);
        annotateRadiusSpinner.setEditor(new JSpinner.NumberEditor(annotateRadiusSpinner, "0.00"));
        annotateRadiusSpinner.setEnabled(annotate);
        annotateRadiusSpinner.setFont(p11);
//        noisetolspinner.setPreferredSize(new Dimension(100, 25));
        annotateRadiusSpinner.setComponentPopupMenu(annotateRadiusStepPopup);
        annotateRadiusSpinner.setToolTipText("<html>The radius of the circle used to identify the annotated object.<br>"+
                                             "Right click to set spinner stepsize.</html>");
        annotateRadiusSpinner.addChangeListener (new ChangeListener()
            {
            public void stateChanged(ChangeEvent ev)
                {
                annotateRadius = ((Double)annotateRadiusSpinner.getValue()).doubleValue();
                }
            });
        annotateRadiusSpinner.addMouseWheelListener( new MouseWheelListener()
            {
            public void mouseWheelMoved( MouseWheelEvent e )
                {
                if (annotate)
                    {
                    annotateRadius = ((Double)annotateRadiusSpinner.getValue()).doubleValue()- e.getWheelRotation()*annotateRadiusStep;
                    if (annotateRadius < 1.0) annotateRadius = 1.0;
                    annotateRadiusSpinner.setValue(annotateRadius);
                    }
                }
            });
        
        annotateRadiusPanel.add (annotateRadiusSpinner);
        SpringUtil.makeCompactGrid (annotateRadiusPanel, 1, 1, 0,0,0,0);
        astrometrySetupPanel.add (annotateRadiusPanel);    
       
        
        JLabel annotateLabel4 = new JLabel ("");
//        annotateLabel4.setPreferredSize(fitsDummySize);
        astrometrySetupPanel.add (annotateLabel4);  
        
        JLabel annotateLabel5 = new JLabel ("");
//        annotateLabel4.setPreferredSize(fitsDummySize);
        astrometrySetupPanel.add (annotateLabel5);         

       
  
        
//-------------------------------------------------------------------
        if (lastSlice>1)
            {
            nlines++;
            JLabel processStackLabel = new JLabel ("Process Stack:");
    //        processStackLabel.setPreferredSize(labelSize);
            processStackLabel.setFont(p12);
            processStackLabel.setHorizontalAlignment (JTextField.RIGHT);
            astrometrySetupPanel.add (processStackLabel);

            processStackCB = new JCheckBox("Enable",processStack);
            processStackCB.setFont(p12);
            processStackCB.setToolTipText("<html>Plate solve all images in stack.</html>");
    //        autoSaveCB.setPreferredSize (checkBoxSize);
            processStackCB.addItemListener (this);
            astrometrySetupPanel.add(processStackCB);

            JPanel startSlicePanel = new JPanel(new SpringLayout());
            TitledBorder startSliceTitle = BorderFactory.createTitledBorder(BorderFactory.createEmptyBorder(),"Start Slice", TitledBorder.CENTER, TitledBorder.TOP, p11);
            startSlicePanel.setBorder(startSliceTitle);
            startSliceNumberModel = new SpinnerNumberModel(fSlice, fSlice, lSlice, 1);
            startSliceSpinner = new JSpinner(startSliceNumberModel);
            JSpinner.NumberEditor startSliceNumberEditor = new JSpinner.NumberEditor(startSliceSpinner, "#" );
            startSliceSpinner.setEditor(startSliceNumberEditor);
    //        startSliceSpinner.setPreferredSize (new Dimension(60,subpanelHeight));
            startSliceSpinner.setFont(p12);
            startSliceSpinner.setToolTipText("Set the first slice number to process");
            startSliceSpinner.setEnabled(processStack);
            startSliceSpinner.addChangeListener (this);
            startSliceSpinner.addMouseWheelListener(this);
            startSlicePanel.add(startSliceSpinner);
            SpringUtil.makeCompactGrid (startSlicePanel, 1,1, 0,0,0,0);
            astrometrySetupPanel.add(startSlicePanel);
                    
            JPanel endSlicePanel = new JPanel(new SpringLayout());
            TitledBorder endSliceTitle = BorderFactory.createTitledBorder(BorderFactory.createEmptyBorder(),"End Slice", TitledBorder.CENTER, TitledBorder.TOP, p11);
            endSlicePanel.setBorder(endSliceTitle);
            
            endSliceNumberModel = new SpinnerNumberModel(lSlice, fSlice, lSlice, 1);
            endSliceSpinner = new JSpinner(endSliceNumberModel);
            JSpinner.NumberEditor endSliceNumberEditor = new JSpinner.NumberEditor(endSliceSpinner, "#" );
            endSliceSpinner.setEditor(endSliceNumberEditor);
    //        endSliceSpinner.setPreferredSize (new Dimension(60,subpanelHeight));
            endSliceSpinner.setFont(p12);
            endSliceSpinner.setToolTipText("Set the last slice number to process");
            endSliceSpinner.setEnabled(processStack);
            endSliceSpinner.addChangeListener (this);
            endSliceSpinner.addMouseWheelListener(this);
            endSlicePanel.add(endSliceSpinner); 
            SpringUtil.makeCompactGrid (endSlicePanel, 1,1, 0,0,0,0);
            astrometrySetupPanel.add(endSlicePanel); 
            
            
            JLabel sliceLabel5 = new JLabel ("");
    //        annotateLabel4.setPreferredSize(fitsDummySize);
            astrometrySetupPanel.add (sliceLabel5);             

            }    
        
        
//-------------------------------------------------------------------

        nlines++;
        JLabel useMedianFilterLabel = new JLabel ("Median Filter:");
//        useMedianFilterLabel.setPreferredSize(labelSize);
        useMedianFilterLabel.setFont(p12);
        useMedianFilterLabel.setHorizontalAlignment (JTextField.RIGHT);
        astrometrySetupPanel.add (useMedianFilterLabel);

        useMedianFilterCB = new JCheckBox("Enable",useMedianFilter);
        useMedianFilterCB.setFont(p12);
        useMedianFilterCB.setToolTipText("<html>Median filter image before finding peaks.<br>"+
                                         "NOTE: does not change user image.</html>");
//        autoSaveCB.setPreferredSize (checkBoxSize);
        useMedianFilterCB.addItemListener (this);
        astrometrySetupPanel.add(useMedianFilterCB);

        JPanel medianFilterRadiusPanel = new JPanel(new SpringLayout());
        TitledBorder medianFilterRadiusTitle = BorderFactory.createTitledBorder(BorderFactory.createEmptyBorder(),"Filter Radius (pixels)", TitledBorder.CENTER, TitledBorder.TOP, p11);
        medianFilterRadiusPanel.setBorder(medianFilterRadiusTitle);
        if (medianFilterRadius<2) medianFilterRadius = 2;
        medianFilterRadiusNumberModel = new SpinnerNumberModel(medianFilterRadius, 2, null, 1);
        medianFilterRadiusSpinner = new JSpinner(medianFilterRadiusNumberModel);
        JSpinner.NumberEditor medianFilterRadiusNumberEditor = new JSpinner.NumberEditor(medianFilterRadiusSpinner, "#" );
        medianFilterRadiusSpinner.setEditor(medianFilterRadiusNumberEditor);
//        medianFilterRadiusSpinner.setPreferredSize (new Dimension(60,subpanelHeight));
        medianFilterRadiusSpinner.setFont(p12);
        medianFilterRadiusSpinner.setToolTipText("<html>Set the radius of the median filter used before finding peaks (if enabled).<br>"+
                                                 "NOTE: does not change user image.</html>");
        medianFilterRadiusSpinner.setEnabled(useMedianFilter);
        medianFilterRadiusSpinner.addChangeListener (this);
        medianFilterRadiusSpinner.addMouseWheelListener(this);
        medianFilterRadiusPanel.add(medianFilterRadiusSpinner);
        SpringUtil.makeCompactGrid (medianFilterRadiusPanel, 1,1, 0,0,0,0);
        astrometrySetupPanel.add(medianFilterRadiusPanel);

        
        JPanel medianFilterRadius4Panel = new JPanel(new SpringLayout());
        TitledBorder medianFilterRadius4Title = BorderFactory.createTitledBorder(BorderFactory.createEmptyBorder()," ", TitledBorder.CENTER, TitledBorder.TOP, p11);
        medianFilterRadius4Panel.setBorder(medianFilterRadius4Title);        
        JLabel medianFilterRadius4 = new JLabel ("");
        medianFilterRadius4.setHorizontalAlignment (JTextField.LEFT);
        medianFilterRadius4.setFont(p12);
//        medianFilterRadius4.setPreferredSize(fitsDummySize);
        medianFilterRadius4Panel.add (medianFilterRadius4); 
        SpringUtil.makeCompactGrid (medianFilterRadius4Panel, 1,1, 0,0,0,0);
        astrometrySetupPanel.add(medianFilterRadius4Panel);        
        
        JLabel medianFilterRadius5 = new JLabel ("");
//        medianFilterRadius5.setPreferredSize(fitsDummySize);
        astrometrySetupPanel.add (medianFilterRadius5);         

    
        
//-------------------------------------------------------------------

        nlines++;
        JLabel peakFindOptionsLabel = new JLabel ("Peak Find Options:");
//        peakFindOptionsLabel.setPreferredSize(labelSize);
        peakFindOptionsLabel.setFont(p12);
        peakFindOptionsLabel.setHorizontalAlignment (JTextField.RIGHT);
        astrometrySetupPanel.add (peakFindOptionsLabel);

//        useMedianFilterCB = new JCheckBox("Enable",useMedianFilter);
//        useMedianFilterCB.setFont(p12);
//        useMedianFilterCB.setToolTipText("<html>Median filter image before finding peaks.<br>"+
//                                         "NOTE: does not change user image.</html>");
////        autoSaveCB.setPreferredSize (checkBoxSize);
//        useMedianFilterCB.addItemListener (this);
//        astrometrySetupPanel.add(useMedianFilterCB);
        
        useMaxPeakFindValueCB = new JCheckBox("Limit Max Peaks",useMaxPeakFindValue);
        useMaxPeakFindValueCB.setFont(p12);
        useMaxPeakFindValueCB.setToolTipText("<html>When enabled, peaks above the threshold set to the right are ignored</html>");
//        autoSaveCB.setPreferredSize (checkBoxSize);
        useMaxPeakFindValueCB.addItemListener (this);
        astrometrySetupPanel.add(useMaxPeakFindValueCB);  
        
        maxPeakFindStepPopup = new JPopupMenu();
        JPanel maxPeakFindStepPanel = new JPanel();
        SpinnerListModel maxPeakFindStepModel = new SpinnerListModel(spinnerScaleList);
        maxPeakFindStepSpinner = new JSpinner(maxPeakFindStepModel);
        maxPeakFindStepSpinner.setValue(convertToText(maxPeakFindStep));
        maxPeakFindStepSpinner.addChangeListener (new ChangeListener()
            {
            public void stateChanged(ChangeEvent ev)
                {
                double value = IJU.getTextSpinnerDoubleValue(maxPeakFindStepSpinner);
                if (Double.isNaN(value)) return;
                maxPeakFindStep = value;
                maxPeakFindSpinner.setModel(new SpinnerNumberModel(new Double(maxPeakFindValue), null, null, new Double(maxPeakFindStep)));
                maxPeakFindSpinner.setEditor(new JSpinner.NumberEditor(maxPeakFindSpinner, "0.######"));
                }
            });

        JLabel maxPeakFindStepLabel = new JLabel ("Stepsize:");
        maxPeakFindStepPanel.add(maxPeakFindStepLabel);
        maxPeakFindStepPanel.add(maxPeakFindStepSpinner);
        maxPeakFindStepPopup.add(maxPeakFindStepPanel);
        maxPeakFindStepPopup.setLightWeightPopupEnabled(false);
        maxPeakFindStepPopup.addPopupMenuListener (new PopupMenuListener()
            {
            public void popupMenuCanceled(PopupMenuEvent popupMenuEvent) {
                    maxPeakFindStepPopup.setVisible(false);
                    }

            public void popupMenuWillBecomeInvisible(PopupMenuEvent popupMenuEvent) {
                    maxPeakFindStepPopup.setVisible(true);
                    }

            public void popupMenuWillBecomeVisible(PopupMenuEvent popupMenuEvent) {
                    maxPeakFindStepPopup.setVisible(false);
                    }
            });

        JPanel maxPeakFindPanel = new JPanel(new SpringLayout());
        TitledBorder maxPeakFindTitle = BorderFactory.createTitledBorder(BorderFactory.createEmptyBorder(),"Max Peak (ADU)", TitledBorder.CENTER, TitledBorder.TOP, p11);
        maxPeakFindPanel.setBorder(maxPeakFindTitle);

        
        
        maxPeakFindNumberModel = new SpinnerNumberModel(new Double(maxPeakFindValue), null, null, new Double(maxPeakFindStep));
        maxPeakFindSpinner = new JSpinner(maxPeakFindNumberModel);
        maxPeakFindSpinner.setEnabled(useMaxPeakFindValue);
        maxPeakFindSpinner.setFont(p11);
        maxPeakFindSpinner.setEditor(new JSpinner.NumberEditor(maxPeakFindSpinner, "0.######"));
//        noisetolspinner.setPreferredSize(new Dimension(100, 25));
        maxPeakFindSpinner.setComponentPopupMenu(maxPeakFindStepPopup);
        maxPeakFindSpinner.setToolTipText("<html>If enabled, peaks above this ADU level are ignored.<br>"+
                                          "Right click to set spinner stepsize.</html>");
        maxPeakFindSpinner.addChangeListener (new ChangeListener()
            {
            public void stateChanged(ChangeEvent ev)
                {
                maxPeakFindValue = ((Double)maxPeakFindSpinner.getValue()).doubleValue();
                }
            });
        maxPeakFindSpinner.addMouseWheelListener( new MouseWheelListener()
            {
            public void mouseWheelMoved( MouseWheelEvent e )
                {
                double newValue = ((Double)maxPeakFindSpinner.getValue()).doubleValue()- e.getWheelRotation()*maxPeakFindStep;
                maxPeakFindSpinner.setValue(newValue);
                }
            });
        maxPeakFindPanel.add (maxPeakFindSpinner);
        SpringUtil.makeCompactGrid (maxPeakFindPanel, 1, 1, 0,0,0,0);
        astrometrySetupPanel.add (maxPeakFindPanel);        

        noiseTolStepPopup = new JPopupMenu();
        JPanel noiseTolStepPanel = new JPanel();
        SpinnerListModel noiseTolStepModel = new SpinnerListModel(spinnerScaleList);
        noiseTolStepSpinner = new JSpinner(noiseTolStepModel);
        noiseTolStepSpinner.setValue(convertToText(noiseTolStep));
        noiseTolStepSpinner.addChangeListener (new ChangeListener()
            {
            public void stateChanged(ChangeEvent ev)
                {
                double value = IJU.getTextSpinnerDoubleValue(noiseTolStepSpinner);
                if (Double.isNaN(value)) return;
                noiseTolStep = value;                
                noiseTolSpinner.setModel(new SpinnerNumberModel(new Double(minPeakFindToleranceSTDEV), new Double(0.0), null, new Double(noiseTolStep)));
                noiseTolSpinner.setEditor(new JSpinner.NumberEditor(noiseTolSpinner, "0.00######"));
                }
            });

        JLabel noisetolsteplabel = new JLabel ("Stepsize:");
        noiseTolStepPanel.add(noisetolsteplabel);
        noiseTolStepPanel.add(noiseTolStepSpinner);
        noiseTolStepPopup.add(noiseTolStepPanel);
        noiseTolStepPopup.setLightWeightPopupEnabled(false);
        noiseTolStepPopup.addPopupMenuListener (new PopupMenuListener()
            {
            public void popupMenuCanceled(PopupMenuEvent popupMenuEvent) {
                    noiseTolStepPopup.setVisible(false);
                    }

            public void popupMenuWillBecomeInvisible(PopupMenuEvent popupMenuEvent) {
                    noiseTolStepPopup.setVisible(true);
                    }

            public void popupMenuWillBecomeVisible(PopupMenuEvent popupMenuEvent) {
                    noiseTolStepPopup.setVisible(false);
                    }
            });

        JPanel minTolPanel = new JPanel(new SpringLayout());
        TitledBorder minTolTitle = BorderFactory.createTitledBorder(BorderFactory.createEmptyBorder(),"Noise Tol (StdDev)", TitledBorder.CENTER, TitledBorder.TOP, p11);
        minTolPanel.setBorder(minTolTitle);

        noiseTolNumberModel = new SpinnerNumberModel(new Double(minPeakFindToleranceSTDEV), new Double(0.0), null, new Double(noiseTolStep));
        noiseTolSpinner = new JSpinner(noiseTolNumberModel);
        noiseTolSpinner.setFont(p11);
        noiseTolSpinner.setEditor(new JSpinner.NumberEditor(noiseTolSpinner, "0.00#####"));
//        noisetolspinner.setPreferredSize(new Dimension(100, 25));
        noiseTolSpinner.setComponentPopupMenu(noiseTolStepPopup);
        noiseTolSpinner.setToolTipText("<html>Peaks below the image standard deviation times this factor are ignored.<br>"+
                                       "Generally a value of 1.0 works well.<br>"+
                                       "Right click to set spinner stepsize.</html>");
        noiseTolSpinner.addChangeListener (new ChangeListener()
            {
            public void stateChanged(ChangeEvent ev)
                {
                minPeakFindToleranceSTDEV = ((Double)noiseTolSpinner.getValue()).doubleValue();
                }
            });
        noiseTolSpinner.addMouseWheelListener( new MouseWheelListener()
            {
            public void mouseWheelMoved( MouseWheelEvent e )
                {
                double newValue = ((Double)noiseTolSpinner.getValue()).doubleValue()- e.getWheelRotation()*noiseTolStep;
                if (newValue < 0 ) newValue = 0;
                noiseTolSpinner.setValue(newValue);
                }
            });
        minTolPanel.add (noiseTolSpinner);
        SpringUtil.makeCompactGrid (minTolPanel, 1, 1, 0,0,0,0);
        astrometrySetupPanel.add (minTolPanel);

        
       
        
        JPanel maxNumStarsPanel = new JPanel(new SpringLayout());
        TitledBorder maxNumStarsTitle = BorderFactory.createTitledBorder(BorderFactory.createEmptyBorder(),"Max Num Stars", TitledBorder.CENTER, TitledBorder.TOP, p11);
        maxNumStarsPanel.setBorder(maxNumStarsTitle);
        if (maxNumStars<MIN_MAX_STARS) maxNumStars = MIN_MAX_STARS;
        maxNumStarsNumberModel = new SpinnerNumberModel(maxNumStars, MIN_MAX_STARS, null, 1);
        maxNumStarsSpinner = new JSpinner(maxNumStarsNumberModel);
        JSpinner.NumberEditor maxNumStarsNumberEditor = new JSpinner.NumberEditor(maxNumStarsSpinner, "#" );
        maxNumStarsSpinner.setEditor(maxNumStarsNumberEditor);
//        medianFilterRadiusSpinner.setPreferredSize (new Dimension(60,subpanelHeight));
        maxNumStarsSpinner.setFont(p12);
        maxNumStarsSpinner.setToolTipText("<html>The number of stars used for plate solving is limited to this value.<br>"+
                                          "The brightest stars that have peaks less than the max peak threshold (if enabled) are used first.<br>"+
                                          "Generally a value of 50 works well.</html>");
        maxNumStarsSpinner.addChangeListener (this);
        maxNumStarsSpinner.addMouseWheelListener(this);
        maxNumStarsPanel.add(maxNumStarsSpinner);
        SpringUtil.makeCompactGrid (maxNumStarsPanel, 1,1, 0,0,0,0);
        astrometrySetupPanel.add(maxNumStarsPanel);  
        
        
        
        
        
        
//-------------------------------------------------------------------

        nlines++;
        JLabel centroidLabel = new JLabel ("Centroid Near Peaks:");
//        peakFindOptionsLabel.setPreferredSize(labelSize);
        centroidLabel.setFont(p12);
        centroidLabel.setHorizontalAlignment (JTextField.RIGHT);
        astrometrySetupPanel.add (centroidLabel);
        
        centroidCB = new JCheckBox("Enable",useCentroid);
        centroidCB.setFont(p12);
        centroidCB.setToolTipText("<html>When enabled, the centroid location nearest each peak<br>"+
                                  "will be used in place of the peak location.<br>"+
                                  "This feature is useful for defocused images.<br>"+
                                  "However, multiple peaks within the centroid radius may merge to the brighter star.<br>"+
                                  "Astrometry.net is usually tolerant of a few erroneous star locations.</html>");
//        autoSaveCB.setPreferredSize (checkBoxSize);
        centroidCB.addItemListener (this);
        astrometrySetupPanel.add(centroidCB);  
        
        apertureStepPopup = new JPopupMenu();
        JPanel apertureStepPanel = new JPanel();
        SpinnerListModel apertureStepModel = new SpinnerListModel(spinnerScaleList);
        apertureStepSpinner = new JSpinner(apertureStepModel);
        apertureStepSpinner.setValue(convertToText(apertureStep));
        apertureStepSpinner.addChangeListener (new ChangeListener()
            {
            public void stateChanged(ChangeEvent ev)
                {
                double value = IJU.getTextSpinnerDoubleValue(apertureStepSpinner);
                if (Double.isNaN(value)) return;
                apertureStep = value;
                apertureRadiusSpinner.setModel(new SpinnerNumberModel(new Double(apertureRadius), new Double(1.0), null, new Double(apertureStep)));
                apertureBack1Spinner.setModel(new SpinnerNumberModel(new Double(apertureBack1), new Double(apertureRadius), null, new Double(apertureStep)));
                apertureBack2Spinner.setModel(new SpinnerNumberModel(new Double(apertureBack2), new Double(apertureBack1+1), null, new Double(apertureStep)));
                apertureRadiusSpinner.setEditor(new JSpinner.NumberEditor(apertureRadiusSpinner, "0.00"));
                apertureBack1Spinner.setEditor(new JSpinner.NumberEditor(apertureBack1Spinner, "0.00"));
                apertureBack2Spinner.setEditor(new JSpinner.NumberEditor(apertureBack2Spinner, "0.00"));
                }
            });

        JLabel apertureStepLabel = new JLabel ("Stepsize:");
        apertureStepPanel.add(apertureStepLabel);
        apertureStepPanel.add(apertureStepSpinner);
        apertureStepPopup.add(apertureStepPanel);
        apertureStepPopup.setLightWeightPopupEnabled(false);
        apertureStepPopup.addPopupMenuListener (new PopupMenuListener()
            {
            public void popupMenuCanceled(PopupMenuEvent popupMenuEvent) {
                    apertureStepPopup.setVisible(false);
                    }

            public void popupMenuWillBecomeInvisible(PopupMenuEvent popupMenuEvent) {
                    apertureStepPopup.setVisible(true);
                    }

            public void popupMenuWillBecomeVisible(PopupMenuEvent popupMenuEvent) {
                    apertureStepPopup.setVisible(false);
                    }
            });

        JPanel apertureRadiusPanel = new JPanel(new SpringLayout());
        TitledBorder apertureRadiusTitle = BorderFactory.createTitledBorder(BorderFactory.createEmptyBorder(),"Radius (pixels)", TitledBorder.CENTER, TitledBorder.TOP, p11);
        apertureRadiusPanel.setBorder(apertureRadiusTitle);

        apertureRadiusNumberModel = new SpinnerNumberModel(new Double(apertureRadius), new Double(1.0), null, new Double(apertureStep));
        apertureRadiusSpinner = new JSpinner(apertureRadiusNumberModel);
        apertureRadiusSpinner.setEnabled(useCentroid);
        apertureRadiusSpinner.setFont(p11);
        apertureRadiusSpinner.setEditor(new JSpinner.NumberEditor(apertureRadiusSpinner, "0.00"));
//        noisetolspinner.setPreferredSize(new Dimension(100, 25));
        apertureRadiusSpinner.setComponentPopupMenu(apertureStepPopup);
        apertureRadiusSpinner.setToolTipText("<html>The radius used to find the centroid nearest the peak location.<br>"+
                                             "Right click to set spinner stepsize.</html>");
        apertureRadiusSpinner.addChangeListener (new ChangeListener()
            {
            public void stateChanged(ChangeEvent ev)
                {
                apertureRadius = ((Double)apertureRadiusSpinner.getValue()).doubleValue();
                if (apertureBack1 < apertureRadius) apertureBack1 = apertureRadius;
                if (apertureBack2 < apertureBack1+1) apertureBack2 = apertureBack1+1;
                apertureBack1Spinner.setModel(new SpinnerNumberModel(new Double(apertureBack1), new Double(apertureRadius), null, new Double(apertureStep)));
                apertureBack2Spinner.setModel(new SpinnerNumberModel(new Double(apertureBack2), new Double(apertureBack1+1), null, new Double(apertureStep)));
                apertureBack1Spinner.setEditor(new JSpinner.NumberEditor(apertureBack1Spinner, "0.00"));
                apertureBack2Spinner.setEditor(new JSpinner.NumberEditor(apertureBack2Spinner, "0.00"));                
                }
            });
        apertureRadiusSpinner.addMouseWheelListener( new MouseWheelListener()
            {
            public void mouseWheelMoved( MouseWheelEvent e )
                {
                if (useCentroid)
                    {
                    apertureRadius = ((Double)apertureRadiusSpinner.getValue()).doubleValue()- e.getWheelRotation()*apertureStep;
                    if (apertureRadius < 1.0) apertureRadius = 1.0;
                    if (apertureBack1 < apertureRadius) apertureBack1 = apertureRadius;
                    if (apertureBack2 < apertureBack1+1) apertureBack2 = apertureBack1+1;                
                    apertureRadiusSpinner.setValue(apertureRadius);
                    apertureBack1Spinner.setModel(new SpinnerNumberModel(new Double(apertureBack1), new Double(apertureRadius), null, new Double(apertureStep)));
                    apertureBack2Spinner.setModel(new SpinnerNumberModel(new Double(apertureBack2), new Double(apertureBack1+1), null, new Double(apertureStep)));
                    apertureBack1Spinner.setEditor(new JSpinner.NumberEditor(apertureBack1Spinner, "0.00"));
                    apertureBack2Spinner.setEditor(new JSpinner.NumberEditor(apertureBack2Spinner, "0.00"));
                    }
                }
            });
        apertureRadiusPanel.add (apertureRadiusSpinner);
        SpringUtil.makeCompactGrid (apertureRadiusPanel, 1, 1, 0,0,0,0);
        astrometrySetupPanel.add (apertureRadiusPanel);    
        
        
        

        JPanel apertureBack1Panel = new JPanel(new SpringLayout());
        TitledBorder apertureBack1Title = BorderFactory.createTitledBorder(BorderFactory.createEmptyBorder(),"Sky Inner (pixels)", TitledBorder.CENTER, TitledBorder.TOP, p11);
        apertureBack1Panel.setBorder(apertureBack1Title);

        if (apertureBack1 < apertureRadius) apertureBack1 = apertureRadius;
        apertureBack1NumberModel = new SpinnerNumberModel(new Double(apertureBack1), new Double(apertureRadius), null, new Double(apertureStep));
        apertureBack1Spinner = new JSpinner(apertureBack1NumberModel);
        apertureBack1Spinner.setEnabled(useCentroid);
        apertureBack1Spinner.setFont(p11);
        apertureBack1Spinner.setEditor(new JSpinner.NumberEditor(apertureBack1Spinner, "0.00"));
//        noisetolspinner.setPreferredSize(new Dimension(100, 25));
        apertureBack1Spinner.setComponentPopupMenu(apertureStepPopup);
        apertureBack1Spinner.setToolTipText("<html>The radius of the inner edge of the sky background annulus<br>"+ 
                                            "used to find the centroid nearest the peak location.<br>"+
                                            "Right click to set spinner stepsize.</html>");
        apertureBack1Spinner.addChangeListener (new ChangeListener()
            {
            public void stateChanged(ChangeEvent ev)
                {
                apertureBack1 = ((Double)apertureBack1Spinner.getValue()).doubleValue();
                if (apertureBack1 < apertureRadius) apertureBack1 = apertureRadius;
                if (apertureBack2 < apertureBack1+1) apertureBack2 = apertureBack1+1;
                apertureBack1Spinner.setModel(new SpinnerNumberModel(new Double(apertureBack1), new Double(apertureRadius), null, new Double(apertureStep)));
                apertureBack2Spinner.setModel(new SpinnerNumberModel(new Double(apertureBack2), new Double(apertureBack1+1), null, new Double(apertureStep)));
                apertureBack1Spinner.setEditor(new JSpinner.NumberEditor(apertureBack1Spinner, "0.00"));
                apertureBack2Spinner.setEditor(new JSpinner.NumberEditor(apertureBack2Spinner, "0.00"));                
                }
            });
        apertureBack1Spinner.addMouseWheelListener( new MouseWheelListener()
            {
            public void mouseWheelMoved( MouseWheelEvent e )
                {
                if (useCentroid)
                    {                
                    apertureBack1 = ((Double)apertureBack1Spinner.getValue()).doubleValue()- e.getWheelRotation()*apertureStep;
                    if (apertureBack1 < apertureRadius) apertureBack1 = apertureRadius;
                    if (apertureBack2 < apertureBack1+1) apertureBack2 = apertureBack1+1;
                    apertureBack1Spinner.setModel(new SpinnerNumberModel(new Double(apertureBack1), new Double(apertureRadius), null, new Double(apertureStep)));
                    apertureBack2Spinner.setModel(new SpinnerNumberModel(new Double(apertureBack2), new Double(apertureBack1+1), null, new Double(apertureStep)));
                    apertureBack1Spinner.setEditor(new JSpinner.NumberEditor(apertureBack1Spinner, "0.00"));
                    apertureBack2Spinner.setEditor(new JSpinner.NumberEditor(apertureBack2Spinner, "0.00"));                  
                    }
                }
            });
        apertureBack1Panel.add (apertureBack1Spinner);
        SpringUtil.makeCompactGrid (apertureBack1Panel, 1, 1, 0,0,0,0);
        astrometrySetupPanel.add (apertureBack1Panel);  
        
        

        JPanel apertureBack2Panel = new JPanel(new SpringLayout());
        TitledBorder apertureBack2Title = BorderFactory.createTitledBorder(BorderFactory.createEmptyBorder(),"Sky Outer (pixels)", TitledBorder.CENTER, TitledBorder.TOP, p11);
        apertureBack2Panel.setBorder(apertureBack2Title);

        if (apertureBack2 < apertureBack1+1) apertureBack2 = apertureBack1+1;
        apertureBack2NumberModel = new SpinnerNumberModel(new Double(apertureBack2), new Double(apertureBack1+1), null, new Double(apertureStep));
        apertureBack2Spinner = new JSpinner(apertureBack2NumberModel);
        apertureBack2Spinner.setEnabled(useCentroid);
        apertureBack2Spinner.setFont(p11);
        apertureBack2Spinner.setEditor(new JSpinner.NumberEditor(apertureBack2Spinner, "0.00"));
//        noisetolspinner.setPreferredSize(new Dimension(100, 25));
        apertureBack2Spinner.setComponentPopupMenu(apertureStepPopup);
        apertureBack2Spinner.setToolTipText("<html>The radius of the outer edge of the sky background annulus<br>"+ 
                                            "used to find the centroid nearest the peak location.<br>"+
                                            "Right click to set spinner stepsize.</html>");
        apertureBack2Spinner.addChangeListener (new ChangeListener()
            {
            public void stateChanged(ChangeEvent ev)
                {
                apertureBack2 = ((Double)apertureBack2Spinner.getValue()).doubleValue();
                if (apertureBack2 < apertureBack1+1) apertureBack2 = apertureBack1+1;
                apertureBack2Spinner.setModel(new SpinnerNumberModel(new Double(apertureBack2), new Double(apertureBack1+1), null, new Double(apertureStep)));
                apertureBack2Spinner.setEditor(new JSpinner.NumberEditor(apertureBack2Spinner, "0.00"));                
                }
            });
        apertureBack2Spinner.addMouseWheelListener( new MouseWheelListener()
            {
            public void mouseWheelMoved( MouseWheelEvent e )
                {
                if (useCentroid)
                    {                
                    apertureBack2 = ((Double)apertureBack2Spinner.getValue()).doubleValue()- e.getWheelRotation()*apertureStep;
                    if (apertureBack2 < apertureBack1+1) apertureBack2 = apertureBack1+1;
                    apertureBack2Spinner.setModel(new SpinnerNumberModel(new Double(apertureBack2), new Double(apertureBack1+1), null, new Double(apertureStep)));
                    apertureBack2Spinner.setEditor(new JSpinner.NumberEditor(apertureBack2Spinner, "0.00"));                
                    }
                }
            });
        apertureBack2Panel.add (apertureBack2Spinner);
        SpringUtil.makeCompactGrid (apertureBack2Panel, 1, 1, 0,0,0,0);
        astrometrySetupPanel.add (apertureBack2Panel);    
        
        
        
        
//-------------------------------------------------------------------

        nlines++;
        JLabel scaleLabel = new JLabel ("Constrain Plate Scale:");
//        peakFindOptionsLabel.setPreferredSize(labelSize);
        scaleLabel.setFont(p12);
        scaleLabel.setHorizontalAlignment (JTextField.RIGHT);
        astrometrySetupPanel.add (scaleLabel);
        
        scaleCB = new JCheckBox("Enable",useScale);
        scaleCB.setFont(p12);
        scaleCB.setToolTipText("<html>When enabled, the astometry.net plate scale search is limited to<br>"+
                                  "the plate scale value +/- the plate scale tolerance specified.<br>"+
                                  "Specifying a plate scale range can significantly reduce solve time.</html>");
//        autoSaveCB.setPreferredSize (checkBoxSize);
        scaleCB.addItemListener (this);
        astrometrySetupPanel.add(scaleCB); 
        
        scaleStepPopup = new JPopupMenu();
        JPanel scaleStepPanel = new JPanel();
        SpinnerListModel scaleStepModel = new SpinnerListModel(spinnerScaleList);
        scaleStepSpinner = new JSpinner(scaleStepModel);
        scaleStepSpinner.setValue(convertToText(scaleStep));
        scaleStepSpinner.addChangeListener (new ChangeListener()
            {
            public void stateChanged(ChangeEvent ev)
                {
                double value = IJU.getTextSpinnerDoubleValue(scaleStepSpinner);
                if (Double.isNaN(value)) return;
                scaleStep = value;
                scaleEstimateSpinner.setModel(new SpinnerNumberModel(new Double(scaleEstimate), new Double(0.0), null, new Double(scaleStep)));
                }
            });

        JLabel scaleStepLabel = new JLabel ("Stepsize:");
        scaleStepPanel.add(scaleStepLabel);
        scaleStepPanel.add(scaleStepSpinner);
        scaleStepPopup.add(scaleStepPanel);
        scaleStepPopup.setLightWeightPopupEnabled(false);
        scaleStepPopup.addPopupMenuListener (new PopupMenuListener()
            {
            public void popupMenuCanceled(PopupMenuEvent popupMenuEvent) {
                    scaleStepPopup.setVisible(false);
                    }

            public void popupMenuWillBecomeInvisible(PopupMenuEvent popupMenuEvent) {
                    scaleStepPopup.setVisible(true);
                    }

            public void popupMenuWillBecomeVisible(PopupMenuEvent popupMenuEvent) {
                    scaleStepPopup.setVisible(false);
                    }
            });

        JPanel scaleEstimatePanel = new JPanel(new SpringLayout());
        TitledBorder scaleEstimateTitle = BorderFactory.createTitledBorder(BorderFactory.createEmptyBorder(),"Plate Scale (arcsec/pix)", TitledBorder.CENTER, TitledBorder.TOP, p11);
        scaleEstimatePanel.setBorder(scaleEstimateTitle);

        scaleEstimateNumberModel = new SpinnerNumberModel(new Double(scaleEstimate), new Double(0.0), null, new Double(scaleStep));
        scaleEstimateSpinner = new JSpinner(scaleEstimateNumberModel);
        scaleEstimateSpinner.setEnabled(useScale);
        scaleEstimateSpinner.setFont(p11);
        scaleEstimateSpinner.setEditor(new JSpinner.NumberEditor(scaleEstimateSpinner, "0.000##"));
//        noisetolspinner.setPreferredSize(new Dimension(100, 25));
        scaleEstimateSpinner.setComponentPopupMenu(scaleStepPopup);
        scaleEstimateSpinner.setToolTipText("<html>If enabled, this value is used as the nominal plate scale value by astometry.net.<br>"+
                                            "Specifying a plate scale value and tolerance can significantly reduce solve time.<br>"+
                                            "Right click to set spinner stepsize.</html>");
        scaleEstimateSpinner.addChangeListener (new ChangeListener()
            {
            public void stateChanged(ChangeEvent ev)
                {
                scaleEstimate = ((Double)scaleEstimateSpinner.getValue()).doubleValue();
                }
            });
        scaleEstimateSpinner.addMouseWheelListener( new MouseWheelListener()
            {
            public void mouseWheelMoved( MouseWheelEvent e )
                {
                if (useScale)
                    {                
                    scaleEstimate = ((Double)scaleEstimateSpinner.getValue()).doubleValue()- e.getWheelRotation()*scaleStep;
                    if (scaleEstimate < 0.0) scaleEstimate = 0.0;
                    scaleEstimateSpinner.setValue(scaleEstimate);
    //                scaleEstimateSpinner.setModel(new SpinnerNumberModel(new Double(scaleEstimate), new Double(0.0), null, new Double(scaleStep)));
                    }
                }
            });
        scaleEstimatePanel.add (scaleEstimateSpinner);
        SpringUtil.makeCompactGrid (scaleEstimatePanel, 1, 1, 0,0,0,0);
        astrometrySetupPanel.add (scaleEstimatePanel);    
        
        JPanel scaleErrorPanel = new JPanel(new SpringLayout());
        TitledBorder scaleErrorTitle = BorderFactory.createTitledBorder(BorderFactory.createEmptyBorder(),"Tolerance (arcsec/pix)", TitledBorder.CENTER, TitledBorder.TOP, p11);
        scaleErrorPanel.setBorder(scaleErrorTitle);

        scaleErrorNumberModel = new SpinnerNumberModel(new Double(scaleError), new Double(0.0), null, new Double(scaleStep));
        scaleErrorSpinner = new JSpinner(scaleErrorNumberModel);
        scaleErrorSpinner.setEnabled(useScale);
        scaleErrorSpinner.setFont(p11);
        scaleErrorSpinner.setEditor(new JSpinner.NumberEditor(scaleErrorSpinner, "0.000##"));
//        noisetolspinner.setPreferredSize(new Dimension(100, 25));
        scaleErrorSpinner.setComponentPopupMenu(scaleStepPopup);
        scaleErrorSpinner.setToolTipText("<html>If enabled, this value is used as the plate scale tolerance by astometry.net.<br>"+
                                            "Specifying a plate scale value and tolerance can significantly reduce solve time.<br>"+
                                            "Right click to set spinner stepsize.</html>");
        scaleErrorSpinner.addChangeListener (new ChangeListener()
            {
            public void stateChanged(ChangeEvent ev)
                {
                scaleError = ((Double)scaleErrorSpinner.getValue()).doubleValue();
                }
            });
        scaleErrorSpinner.addMouseWheelListener( new MouseWheelListener()
            {
            public void mouseWheelMoved( MouseWheelEvent e )
                {
                if (useScale)
                    {
                    scaleError = ((Double)scaleErrorSpinner.getValue()).doubleValue()- e.getWheelRotation()*scaleStep;
                    if (scaleError < 0.0) scaleError = 0.0;
                    scaleErrorSpinner.setValue(scaleError);
    //                scaleErrorSpinner.setModel(new SpinnerNumberModel(new Double(scaleError), new Double(0.0), null, new Double(scaleStep)));
                    }
                }
            });
        scaleErrorPanel.add (scaleErrorSpinner);
        SpringUtil.makeCompactGrid (scaleErrorPanel, 1, 1, 0,0,0,0);
        astrometrySetupPanel.add (scaleErrorPanel);         

        
        JLabel scaleLabel5 = new JLabel ("");
//        scaleLabel5.setPreferredSize(col5Size);
		astrometrySetupPanel.add (scaleLabel5);        
        
        
        
        
        
//-------------------------------------------------------------------

        nlines++;
        JLabel radecLabel = new JLabel ("Constrain Sky Location:");
//        peakFindOptionsLabel.setPreferredSize(labelSize);
        radecLabel.setFont(p12);
        radecLabel.setHorizontalAlignment(JTextField.RIGHT);
//        radecLabel.setVerticalAlignment(JLabel.BOTTOM);
        astrometrySetupPanel.add (radecLabel);
        
        raDecCB = new JCheckBox("Enable",useRaDec);
        raDecCB.setFont(p12);
        if (!useCCRaDec)
            {
            raDecCB.setToolTipText("<html>When enabled, the astometry.net sky location search is limited to<br>"+
                                    "the RA/Dec location +/- the specified radius.<br>"+
                                    "Constraining the field's location may reduce solve time.</html>");
            }
        else
            {
            raDecCB.setToolTipText("<html>When enabled, the astometry.net sky location search is limited to<br>"+
                                    "the RA/Dec location +/- the specified radius. When running Data Processor,<br>"+
                                    "RA and Dec are automatically populated from the 'Target Coordinate Source' specifed<br>"+
                                    "to the right. Constraining the field's location on the sky may reduce solve time.</html>");            
            }
//        autoSaveCB.setPreferredSize (checkBoxSize);
        raDecCB.addItemListener (this);
        astrometrySetupPanel.add(raDecCB); 
        
        
        JPanel raPanel = new JPanel(new SpringLayout());
        TitledBorder raTitle = BorderFactory.createTitledBorder(BorderFactory.createEmptyBorder(),"Center RA (Hours)", TitledBorder.CENTER, TitledBorder.TOP, p11);
        raPanel.setBorder(raTitle);        
        
		raTF = new JTextField (showSexagesimal ? decToSex(ra, 3, 24, false) : sixPlaces.format(ra));
        raTF.setMargin(fitsMargin);
        raTF.setFont(p12);
        raTF.setEnabled(useRaDec && !useCCRaDec);
//		raTF.setPreferredSize(col2Size);
		raTF.setHorizontalAlignment(JTextField.LEFT);
        if (!useCCRaDec)
            {
            raTF.setToolTipText("<html>"+"Enter the Right Ascension of the center of the search area and press <ENTER>."+"<br>"+
                                "Both sexegesimal (HH:MM:SS.ss) or decimal (HH.HHHH) hour formats are supported."+"<br>"+
                                "Both RA and Dec may be pasted into this field together. Dec will be parsed<br>"+
                                "and placed in the 'Dec' text box.</html>");
            }
        else
            {
            raTF.setToolTipText("<html>RA and Dec are automatically populated from the 'Target Coordinate Source'<br>"+
                                "specifed in the Data Processor panel.</html>");
            }
        raTF.addActionListener(this);
        raTF.addFocusListener(this);
        raPanel.add (raTF);
        SpringUtil.makeCompactGrid (raPanel, 1, 1, 0,0,0,0);
        astrometrySetupPanel.add (raPanel);         
        
        JPanel decPanel = new JPanel(new SpringLayout());
        TitledBorder decTitle = BorderFactory.createTitledBorder(BorderFactory.createEmptyBorder(),"Center Dec (Degrees)", TitledBorder.CENTER, TitledBorder.TOP, p11);
        decPanel.setBorder(decTitle);        
        
		decTF = new JTextField (showSexagesimal ? decToSex(dec, 2, 90, true) : sixPlaces.format(dec));
        decTF.setMargin(fitsMargin);
        decTF.setFont(p12);
        decTF.setEnabled(useRaDec && !useCCRaDec);
//		decTF.setPreferredSize(col2Size);
		decTF.setHorizontalAlignment(JTextField.LEFT);
        if (!useCCRaDec)
            {        
            decTF.setToolTipText("<html>"+"Enter the Declination of the center of the search area and press <ENTER>."+"<br>"+
                                "Both sexegesimal (DD:MM:SS.ss) or decimal (DD.DDDD) degree formats are supported."+"<br>"+
                                "Both RA and Dec may be pasted into this field together. RA will be parsed<br>"+
                                "and placed in the 'RA' text box.</html>");
            }
        else
            {
            decTF.setToolTipText("<html>RA and Dec are automatically populated from the 'Target Coordinate Source'<br>"+
                                "specifed in the Data Processor panel.</html>");
            }        
        decTF.addActionListener(this);    
        decTF.addFocusListener(this);
        decPanel.add (decTF);
        SpringUtil.makeCompactGrid (decPanel, 1, 1, 0,0,0,0);
        astrometrySetupPanel.add (decPanel);          
        
        raDecRadiusStepPopup = new JPopupMenu();
        JPanel raDecRadiusStepPanel = new JPanel();
        SpinnerListModel raDecStepModel = new SpinnerListModel(spinnerScaleList);
        raDecRadiusStepSpinner = new JSpinner(raDecStepModel);
        raDecRadiusStepSpinner.setValue(convertToText(raDecRadiusStep));
        raDecRadiusStepSpinner.addChangeListener (new ChangeListener()
            {
            public void stateChanged(ChangeEvent ev)
                {
                double value = IJU.getTextSpinnerDoubleValue(raDecRadiusStepSpinner);
                if (Double.isNaN(value)) return;
                raDecRadiusStep = value;
                raDecRadiusSpinner.setModel(new SpinnerNumberModel(new Double(raDecRadius), new Double(0.0), null, new Double(raDecRadiusStep)));
                }
            });

        JLabel raDecRadiusStepLabel = new JLabel ("Stepsize:");
        raDecRadiusStepPanel.add(raDecRadiusStepLabel);
        raDecRadiusStepPanel.add(raDecRadiusStepSpinner);
        raDecRadiusStepPopup.add(raDecRadiusStepPanel);
        raDecRadiusStepPopup.setLightWeightPopupEnabled(false);
        raDecRadiusStepPopup.addPopupMenuListener (new PopupMenuListener()
            {
            public void popupMenuCanceled(PopupMenuEvent popupMenuEvent) {
                    raDecRadiusStepPopup.setVisible(false);
                    }

            public void popupMenuWillBecomeInvisible(PopupMenuEvent popupMenuEvent) {
                    raDecRadiusStepPopup.setVisible(true);
                    }

            public void popupMenuWillBecomeVisible(PopupMenuEvent popupMenuEvent) {
                    raDecRadiusStepPopup.setVisible(false);
                    }
            });

        JPanel raDecRadiusPanel = new JPanel(new SpringLayout());
        TitledBorder raDecRadiusTitle = BorderFactory.createTitledBorder(BorderFactory.createEmptyBorder(),"Radius (arcmin)", TitledBorder.CENTER, TitledBorder.TOP, p11);
        raDecRadiusPanel.setBorder(raDecRadiusTitle);

        raDecRadiusNumberModel = new SpinnerNumberModel(new Double(raDecRadius), new Double(0.0), null, new Double(raDecRadiusStep));
        raDecRadiusSpinner = new JSpinner(raDecRadiusNumberModel);
        raDecRadiusSpinner.setEnabled(useRaDec);
        raDecRadiusSpinner.setFont(p11);
        raDecRadiusSpinner.setEditor(new JSpinner.NumberEditor(raDecRadiusSpinner, "0.0####"));
//        noisetolspinner.setPreferredSize(new Dimension(100, 25));
        raDecRadiusSpinner.setComponentPopupMenu(raDecRadiusStepPopup);
        raDecRadiusSpinner.setToolTipText("<html>If enabled, this value is used as the search radius by astometry.net.<br>"+
                                            "The radius (in minutes of arc) is centered on the RA and Dec values specified.<br>"+
                                            "Other parts of the sky will not be searched. Constraining the sky location and<br>"+
                                            "search radius may reduce solve time, or if specified incorrectly, may result in<br>"+
                                            "a plate solve failure.<br>"+
                                            "Right click to set spinner stepsize.<br>"+
                                            "NOTE: Radius should be at least the size of the largest CCD dimension.</html>");
        raDecRadiusSpinner.addChangeListener (new ChangeListener()
            {
            public void stateChanged(ChangeEvent ev)
                {
                raDecRadius = ((Double)raDecRadiusSpinner.getValue()).doubleValue();
                }
            });
        raDecRadiusSpinner.addMouseWheelListener( new MouseWheelListener()
            {
            public void mouseWheelMoved( MouseWheelEvent e )
                {
                if (useRaDec)
                    {                
                    raDecRadius = ((Double)raDecRadiusSpinner.getValue()).doubleValue()- e.getWheelRotation()*raDecRadiusStep;
                    if (raDecRadius < 0.0) raDecRadius = 0.0;
                    raDecRadiusSpinner.setValue(raDecRadius);
                    }
                }
            });
        raDecRadiusPanel.add (raDecRadiusSpinner);
        SpringUtil.makeCompactGrid (raDecRadiusPanel, 1, 1, 0,0,0,0);
        astrometrySetupPanel.add (raDecRadiusPanel);            
        
       
//-------------------------------------------------------------------

        nlines++;
        JLabel setDistortionOrderLabel = new JLabel ("SIP Distortion Correction:");
//        setDistortionCoeffcientsLabel.setPreferredSize(labelSize);
        setDistortionOrderLabel.setFont(p12);
        setDistortionOrderLabel.setHorizontalAlignment (JTextField.RIGHT);
        astrometrySetupPanel.add (setDistortionOrderLabel);

        useDistortionOrderCB = new JCheckBox("Enable",useDistortionOrder);
        useDistortionOrderCB.setFont(p12);
        useDistortionOrderCB.setToolTipText("<html>Enable to request SIP distortion correction as part of the plate solve.<br>"+
                                                  "NOTE: If disabled, astrometry.net will not use distortion correction<br>"+
                                                  "which may result in a non-optimal plate solve.</html>");
//        autoSaveCB.setPreferredSize (checkBoxSize);
        useDistortionOrderCB.addItemListener (this);
        astrometrySetupPanel.add(useDistortionOrderCB);

        JPanel distortionOrderPanel = new JPanel(new SpringLayout());
        TitledBorder distortionOrderTitle = BorderFactory.createTitledBorder(BorderFactory.createEmptyBorder(),"SIP Order", TitledBorder.CENTER, TitledBorder.TOP, p11);
        distortionOrderPanel.setBorder(distortionOrderTitle);
        if (distortionOrder<minOrder) distortionOrder = minOrder;
        if (distortionOrder>maxOrder) distortionOrder = maxOrder;
        distortionOrderNumberModel = new SpinnerNumberModel(distortionOrder, minOrder, maxOrder, 1);
        distortionOrderSpinner = new JSpinner(distortionOrderNumberModel);
        JSpinner.NumberEditor distortionOrderNumberEditor = new JSpinner.NumberEditor(distortionOrderSpinner, "#" );
        distortionOrderSpinner.setEditor(distortionOrderNumberEditor);
//        distortionOrderSpinner.setPreferredSize (new Dimension(60,subpanelHeight));
        distortionOrderSpinner.setFont(p12);
        distortionOrderSpinner.setToolTipText("<html>The SIP distortion correction order requested for the plate solve (if enabled).<br>"+
                                                 "NOTE: A SIP order of 2 is the default and is recommended for typical telescope images.<br>"+
                                                 "NOTE: Wide field images may require orders up to 5 for an accurate plate solve.</html>");
        distortionOrderSpinner.setEnabled(useDistortionOrder);
        distortionOrderSpinner.addChangeListener (this);
        distortionOrderSpinner.addMouseWheelListener(this);
        distortionOrderPanel.add(distortionOrderSpinner);
        SpringUtil.makeCompactGrid (distortionOrderPanel, 1,1, 0,0,0,0);
        astrometrySetupPanel.add(distortionOrderPanel);

      
        JLabel distortionOrder4 = new JLabel ("");
        astrometrySetupPanel.add(distortionOrder4);        
        
        JLabel distortionOrder5 = new JLabel ("");
        astrometrySetupPanel.add (distortionOrder5);            
        
        
        
        
       
//------------------------------------------------------------  
        nlines++;

        JLabel controlLabel1 = new JLabel (notDP?"Show Results Log:":"");
        controlLabel1.setFont(p12);
        controlLabel1.setHorizontalAlignment(JTextField.RIGHT);
//        controlLabel1.setPreferredSize(fitsDummySize);
		astrometrySetupPanel.add (controlLabel1); 

       
        showLogCB = new JCheckBox("Enable",showLog);
        showLogCB.setFont(p12);
        showLogCB.setToolTipText("<html>Enable to create a log window showing the astrometry.net results.</html>");
//        autoSaveCB.setPreferredSize (checkBoxSize);
        showLogCB.addItemListener (this);
        
        JLabel controlLabel2 = new JLabel ("");
//        controlLabel2.setPreferredSize(fitsDummySize);
		astrometrySetupPanel.add (notDP ? showLogCB : controlLabel2);        
        
		startButton = new JButton (startButtonLabel);
        if (notDP)
            startButton.setToolTipText("<html>Save new settings and start processing</html>");
        else
            startButton.setToolTipText("<html>Save new settings and close panel</html>");
        startButton.setMargin(new Insets(5,25,5,25));
        startButton.setPreferredSize(new Dimension(125, 30));
        startButton.setFont(b12);
		startButton.addActionListener(this);
		astrometrySetupPanel.add (startButton);

		cancelButton = new JButton (notDP?"CANCEL":"SAVE");
        if (notDP)
            {
            cancelButton.setToolTipText("<html>Close panel and abort plate solve request</html>");
            }
        else
            {
            cancelButton.setToolTipText("<html>Saving new settings</html>");
            }
        cancelButton.setMargin(new Insets(5,25,5,25));
        cancelButton.setPreferredSize(new Dimension(125, 30));
        cancelButton.setFont(b12);
        cancelButton.addActionListener(this);
		astrometrySetupPanel.add (cancelButton);
        
        JLabel fitsDummyLabel5 = new JLabel ("");
//        fitsDummyLabel5.setPreferredSize(fitsDummySize);
		astrometrySetupPanel.add (fitsDummyLabel5);
       
        SpringUtil.makeCompactGrid (astrometrySetupPanel, nlines, astrometrySetupPanel.getComponentCount()/nlines, 2,2,2,2);
        scrollPane = new JScrollPane(astrometrySetupPanel);
        astrometrySetupFrame.add(scrollPane);
		astrometrySetupFrame.pack();
		astrometrySetupFrame.setResizable (true); 
        
        IJU.setFrameSizeAndLocation(astrometrySetupFrame, frameLocationX, frameLocationY, 0, 0);
        
        astrometrySetupFrame.setVisible(true);
        
        while (!done)
            {
            IJ.wait(500);
            if (useCCRaDec)
                {
                ra = astroCC.getRAJ2000();
                raTF.setText(showSexagesimal ? decToSex(ra, 3, 24, false) : sixPlaces.format(ra));
                dec = astroCC.getDecJ2000();
                decTF.setText(showSexagesimal ? decToSex(dec, 2, 90, true) : sixPlaces.format(dec));
                }
            }
        return canceled;
        }
    
    public void getFields()
        {
        userKey = keyTF.getText().trim();
        if (useAlternateAstrometryServer) alternateAstrometryUrlBase = alternateAstrometryUrlBaseTF.getText().trim();
        }

   
	public void itemStateChanged (ItemEvent e)
		{
		Object source = e.getItemSelectable();
        boolean selected = (e.getStateChange() == ItemEvent.SELECTED);
        if (source.equals(autoSaveCB))
            {
            if (notDP)
                autoSave = selected;
            else
                DPSaveRawWithWCS = selected;
            }
        else if (source.equals(useAlternateAstrometryServerCB))
            {
            useAlternateAstrometryServer = selected;
            alternateAstrometryUrlBaseTF.setText(useAlternateAstrometryServer?alternateAstrometryUrlBase:defaultAstrometryUrlBase);
            alternateAstrometryUrlBaseTF.setEnabled(useAlternateAstrometryServer);
            }            
        else if (source.equals(skipIfHasWCSCB))
            {
            skipIfHasWCS = selected;
            }        
        else if (source.equals(processStackCB))
            {
            processStack = selected;
            startSliceSpinner.setEnabled(selected);
            endSliceSpinner.setEnabled(selected);
            }  
        else if (source.equals(annotateCB))
            {
            annotate = selected;
            annotateRadiusSpinner.setEnabled(selected);
            addAnnotationsToHeaderCB.setEnabled(selected);
            }         
        else if (source.equals(addAnnotationsToHeaderCB))
            {
            addAnnotationsToHeader = selected;
            }                             
        else if (source.equals(useMedianFilterCB))
            {
            useMedianFilter = selected;
            medianFilterRadiusSpinner.setEnabled(selected);
            }  
        else if (source.equals(useMaxPeakFindValueCB))
            {
            useMaxPeakFindValue = selected;
            maxPeakFindSpinner.setEnabled(selected);
            }
        else if (source.equals(centroidCB))
            {
            useCentroid = selected;
            apertureRadiusSpinner.setEnabled(selected);
            apertureBack1Spinner.setEnabled(selected);
            apertureBack2Spinner.setEnabled(selected);
            }       
        else if (source.equals(scaleCB))
            {
            useScale = selected;
            scaleEstimateSpinner.setEnabled(selected);
            scaleErrorSpinner.setEnabled(selected);
            }   
        else if (source.equals(raDecCB))
            {
            useRaDec = selected;
            raTF.setEnabled(selected && notDP);
            decTF.setEnabled(selected && notDP);
            raDecRadiusSpinner.setEnabled(selected);
            }    
        else if (source.equals(showLogCB))
            {
            showLog = selected;
            }  
        else if (source.equals(useDistortionOrderCB))
            {
            useDistortionOrder = selected;
            distortionOrderSpinner.setEnabled(selected);
            }           
        }    
        
	/**
	 * Response to buttons being pushed.
	 */
	public void actionPerformed (ActionEvent ae)
		{
        Object source = ae.getSource();
        if (source == startButton)
            {
            if (useRaDec && !useCCRaDec)
                {
                double[] coords = processCoordinatePair(raTF, 3, 24, false, decTF, 2, 90, true, true, true);
                if (!Double.isNaN(coords[0])) 
                    {
                    ra = coords[0];
                    raTF.setText(showSexagesimal ? decToSex(ra, 3, 24, false) : sixPlaces.format(ra));
                    }
                else 
                    {
                    IJ.showMessage("Bad RA Entry");
                    return;
                    }
                coords = processCoordinatePair(raTF, 3, 24, false, decTF, 2, 90, true, false, true);
                if (!Double.isNaN(coords[1])) 
                    {
                    dec = coords[1];
                    decTF.setText(showSexagesimal ? decToSex(dec, 2, 90, true) : sixPlaces.format(dec));
                    }
                else 
                    {
                    IJ.showMessage("Bad Dec Entry");
                    return;
                    }
                }
            canceled = false;
            exit();
            done = true;
            }
        else if (source == cancelButton)
            {
            if (notDP)
                {
                canceled = true;
                exit();
                done = true;
                }
            else
                {
                savePrefs();
                }
            }
        else if (source == raTF || source == decTF)
            {
            double[] coords = processCoordinatePair(raTF, 3, 24, false, decTF, 2, 90, true, source==raTF?true:false, true);
            if (!Double.isNaN(coords[0])) ra = coords[0];
            if (!Double.isNaN(coords[1])) dec = coords[1];            
            }
        }
    
    
    public void stateChanged(ChangeEvent ev)
        {
        if (ev.getSource() == startSliceSpinner)
            {
            startSlice = ((Integer)startSliceSpinner.getValue()).intValue();
            endSlice = ((Integer)endSliceSpinner.getValue()).intValue();
            if (startSlice < fSlice) startSlice = fSlice;
            if (startSlice > endSlice) startSlice = endSlice;
            startSliceNumberModel = new SpinnerNumberModel(startSlice, fSlice, endSlice, 1);
            startSliceSpinner.setModel(startSliceNumberModel);
            endSliceNumberModel = new SpinnerNumberModel(endSlice, startSlice, lSlice, 1);
            endSliceSpinner.setModel(endSliceNumberModel);
            } 
        else if (ev.getSource() == endSliceSpinner)
            {
            startSlice = ((Integer)startSliceSpinner.getValue()).intValue();
            endSlice = ((Integer)endSliceSpinner.getValue()).intValue();
            if (endSlice > lSlice) endSlice = lSlice;
            if (endSlice < startSlice) endSlice = startSlice;
            startSliceNumberModel = new SpinnerNumberModel(startSlice, fSlice, endSlice, 1);
            startSliceSpinner.setModel(startSliceNumberModel);
            endSliceNumberModel = new SpinnerNumberModel(endSlice, startSlice, lSlice, 1);
            endSliceSpinner.setModel(endSliceNumberModel);
            } 
        else if (ev.getSource() == medianFilterRadiusSpinner)
            {
            medianFilterRadius = ((Integer)medianFilterRadiusSpinner.getValue()).intValue();
            if (medianFilterRadius < 2) medianFilterRadius = 2;
            }
        else if (ev.getSource() == maxNumStarsSpinner)
            {
            maxNumStars = ((Integer)maxNumStarsSpinner.getValue()).intValue();
            if (maxNumStars < MIN_MAX_STARS) maxNumStars = MIN_MAX_STARS;
            }     
        else if (ev.getSource() == distortionOrderSpinner)
            {
            distortionOrder = ((Integer)distortionOrderSpinner.getValue()).intValue();
            if (distortionOrder < minOrder) distortionOrder = minOrder;
            if (distortionOrder > maxOrder) distortionOrder = maxOrder;
            }          
        
        }
    
    public void focusGained(FocusEvent ev) {
        }

    public void focusLost(FocusEvent ev) {
        if (ev.getSource() == raTF)
            {
            if (useRaDec && !useCCRaDec)
                {
                double[] coords = processCoordinatePair(raTF, 3, 24, false, decTF, 2, 90, true, true, true);
                if (!Double.isNaN(coords[0])) 
                    {
                    ra = coords[0];
                    raTF.setText(showSexagesimal ? decToSex(ra, 3, 24, false) : sixPlaces.format(ra));
                    }
                else 
                    {
                    IJ.showMessage("Bad RA Entry");
                    }
                }
            }
        if (ev.getSource() == decTF)
            {
            if (useRaDec && !useCCRaDec)
                {
                double[] coords = processCoordinatePair(raTF, 3, 24, false, decTF, 2, 90, true, false, true);
                if (!Double.isNaN(coords[1])) 
                    {
                    dec = coords[1];
                    decTF.setText(showSexagesimal ? decToSex(dec, 2, 90, true) : sixPlaces.format(dec));
                    }
                else 
                    {
                    IJ.showMessage("Bad Dec Entry");
                    return;
                    }
                }            
            }
        }

    
    public void mouseWheelMoved( MouseWheelEvent e )
        {
        if (e.getSource() == startSliceSpinner && startSliceSpinner.isEnabled())
            {
            int endValue = ((Integer)endSliceSpinner.getValue()).intValue();
            int newValue =  new Integer(((Integer)startSliceSpinner.getValue()).intValue() - e.getWheelRotation());
            if (newValue >= fSlice && newValue <= endValue) startSliceSpinner.setValue(newValue);
            else if (newValue < fSlice) startSliceSpinner.setValue(fSlice);
            else startSliceSpinner.setValue(endValue);
            }
        else if (e.getSource() == endSliceSpinner && endSliceSpinner.isEnabled())
            {
            int startValue = ((Integer)startSliceSpinner.getValue()).intValue();
            int newValue =  new Integer(((Integer)endSliceSpinner.getValue()).intValue() - e.getWheelRotation());
            if (newValue >= startValue && newValue <= lSlice) endSliceSpinner.setValue(newValue);
            else if (newValue < startValue) endSliceSpinner.setValue(startValue);
            else endSliceSpinner.setValue(lSlice);
            }     
        else if (e.getSource() == medianFilterRadiusSpinner && medianFilterRadiusSpinner.isEnabled())
            {
            medianFilterRadius =  new Integer(((Integer)medianFilterRadiusSpinner.getValue()).intValue() - e.getWheelRotation());
            if (medianFilterRadius < 2) medianFilterRadius = 2;
            medianFilterRadiusSpinner.setValue(medianFilterRadius);
            }   
        else if (e.getSource() == maxNumStarsSpinner)
            {
            maxNumStars =  new Integer(((Integer)maxNumStarsSpinner.getValue()).intValue() - e.getWheelRotation());
            if (maxNumStars < MIN_MAX_STARS) maxNumStars = MIN_MAX_STARS;
            maxNumStarsSpinner.setValue(maxNumStars);
            }  
        else if (e.getSource() == distortionOrderSpinner && distortionOrderSpinner.isEnabled())
            {
            distortionOrder =  new Integer(((Integer)distortionOrderSpinner.getValue()).intValue() - e.getWheelRotation());
            if (distortionOrder < minOrder) distortionOrder = minOrder;
            if (distortionOrder > maxOrder) distortionOrder = maxOrder;
            distortionOrderSpinner.setValue(distortionOrder);
            }        
        }
    
  
//    void toggleAstrometrySetupDisplay()
//        {
//        Dimension mainScreenSize = Toolkit.getDefaultToolkit().getScreenSize();
//        if (!Prefs.isLocationOnScreen(new Point(frameLocationX, frameLocationY)))
//            {
//            frameLocationX = mainScreenSize.width/2 - astrometrySetupFrame.getWidth()/2;
//            frameLocationY = mainScreenSize.height/2 - astrometrySetupFrame.getHeight()/2;
//            }
//        astrometrySetupFrame.setLocation(frameLocationX, frameLocationY); 
//		astrometrySetupFrame.setVisible (astrometrySetupDisplayed);
//        }   
    
    
    
    public void exit()
        {
        if (true)//!canceled)
            {
            savePrefs();
            }
        else
            {
            Prefs.set ("astrometry.frameLocationX",astrometrySetupFrame.getLocation().x);
            Prefs.set ("astrometry.frameLocationY",astrometrySetupFrame.getLocation().y);            
            }
        astrometrySetupFrame.dispose();
        }
    
  

    
    
    class thisDocumentListener implements DocumentListener
        {
        public void insertUpdate (DocumentEvent ev)
            {
            getFields();
            }
        public void removeUpdate (DocumentEvent ev)
            {
            getFields();
            }
        public void changedUpdate (DocumentEvent ev)
            {
            getFields();
            }

        }    
    
    
    
double[] processCoordinatePair(JTextField textFieldA, int decimalPlacesA, int baseA, boolean showSignA,
                               JTextField textFieldB, int decimalPlacesB, int baseB, boolean showSignB, boolean AIsSource, boolean update)
        {
        double X = Double.NaN;
        double Y = Double.NaN;
        int YstartPosition = 1;

        boolean XNegative = false;
        boolean YNegative = false;
        String text = AIsSource ? textFieldA.getText().trim() : textFieldB.getText().trim();
        String[] pieces = text.replaceAll("[\\-][^0-9\\.]{0,}", " \\-").replaceAll("[+][^0-9\\.]{0,}", " +").replaceAll("[^0-9\\.\\-+]{1,}", " ").trim().split("[^0-9\\.\\-+]{1,}");
//        for (int i=0; i<pieces.length; i++)
//            {
//            IJ.log(""+pieces[i]);
//            }

        if (pieces.length > 0)
            {
            X = Tools.parseDouble(pieces[0]);
            if (!Double.isNaN(X) &&  pieces[0].contains("-"))
                {
                X = -X;
                XNegative = true;
                }
            if (pieces.length > 1 && !pieces[1].contains("-") && !pieces[1].contains("+"))
                {
                X += Math.abs(Tools.parseDouble(pieces[1])) / 60.0;
                YstartPosition = 2;
                if (pieces.length > 2 && !pieces[2].contains("-") && !pieces[2].contains("+"))
                    {
                    X += Math.abs(Tools.parseDouble(pieces[2])) / 3600.0;
                    YstartPosition = 3;
                    }
                }
            }
        if (pieces.length > YstartPosition)
            {
            Y = Tools.parseDouble(pieces[YstartPosition]);
            if (!Double.isNaN(Y) && pieces[YstartPosition].contains("-"))
                {
                Y = -Y;
                YNegative = true;
                }
            if (pieces.length > YstartPosition+1) Y += Math.abs(Tools.parseDouble(pieces[YstartPosition+1]))/60.0;
            if (pieces.length > YstartPosition+2) Y += Math.abs(Tools.parseDouble(pieces[YstartPosition+2]))/3600.0;
            }
        else if (pieces.length > 0 && AIsSource)
            {
            text = textFieldB.getText().trim();
            pieces = text.replaceAll("[\\-][^0-9\\.]{0,}", " \\-").replaceAll("[+][^0-9\\.]{0,}", " +").replaceAll("[^0-9\\.\\-+]{1,}", " ").trim().split("[^0-9\\.\\-+]{1,}");
            if (pieces.length > 0)
                {
                Y = Tools.parseDouble(pieces[0]);
                if (!Double.isNaN(Y) && pieces[0].contains("-"))
                    {
                    Y = -Y;
                    YNegative = true;
                    }
                if (pieces.length > 1 && !pieces[1].contains("-") && !pieces[1].contains("+"))
                    {
                    Y += Math.abs(Tools.parseDouble(pieces[1])) / 60.0;
                    if (pieces.length > 2 && !pieces[2].contains("-") && !pieces[2].contains("+"))
                        {
                        Y += Math.abs(Tools.parseDouble(pieces[2]))/3600.0;
                        }
                    }
                }
            }
        else if (pieces.length > 0 && !AIsSource)
            {
            Y = X;
            YNegative = XNegative;
            X = Double.NaN;
            XNegative = false;
            text = textFieldA.getText().trim();
            pieces = text.replaceAll("[\\-][^0-9\\.]{0,}", " \\-").replaceAll("[+][^0-9\\.]{0,}", " +").replaceAll("[^0-9\\.\\-+]{1,}", " ").trim().split("[^0-9\\.\\-+]{1,}");
            if (pieces.length > 0)
                {
                X = Tools.parseDouble(pieces[0]);
                if (!Double.isNaN(X) && pieces[0].contains("-"))
                    {
                    X = -X;
                    XNegative = true;
                    }
                if (pieces.length > 1  && !pieces[1].contains("-") && !pieces[1].contains("+"))
                    {
                    X += Math.abs(Tools.parseDouble(pieces[1])) / 60.0;
                    if (pieces.length > 2  && !pieces[2].contains("-") && !pieces[2].contains("+"))
                        {
                        X += Math.abs(Tools.parseDouble(pieces[2])) / 3600.0;
                        }
                    }
                }
            }

        X = mapToBase(X, baseA, XNegative);
        if (!Double.isNaN(X) && update) textFieldA.setText(showSexagesimal ? decToSex(X, decimalPlacesA, baseA, showSignA) : sixPlaces.format(X));
        Y = mapToBase(Y, baseB, YNegative);
        if (!Double.isNaN(Y) && update) textFieldB.setText(showSexagesimal ? decToSex(Y, decimalPlacesB, baseB, showSignB) : sixPlaces.format(Y));
        return new double[] {X, Y};
        }

    public double sexToDec(String text, int base)
        {
        double X = Double.NaN;
        boolean XNegative = false;
        String[] pieces = text.replace("-", " -").replaceAll("[^0-9\\.\\-]{1,}", " ").trim().split("[^0-9\\.\\-]{1,}");
        if (pieces.length > 0)
            {
            X = Tools.parseDouble(pieces[0]);
            if (!Double.isNaN(X) &&  pieces[0].contains("-"))
                {
                X = -X;
                XNegative = true;
                }
            if (pieces.length > 1) X += Math.abs(Tools.parseDouble(pieces[1]))/60.0;
            if (pieces.length > 2) X += Math.abs(Tools.parseDouble(pieces[2]))/3600.0;
            }

        X = mapToBase(X, base, XNegative);
        return X;
        }
    
	public String decToSex (double d, int fractionPlaces, int base, Boolean showPlus)
		{
        DecimalFormat nf = new DecimalFormat();
        DecimalFormat nf2x =  new DecimalFormat();
        nf.setDecimalFormatSymbols(IJU.dfs);
        nf2x.setDecimalFormatSymbols(IJU.dfs);
		nf.setMinimumIntegerDigits(2);
        nf2x.setMinimumIntegerDigits(2);
        nf2x.setMinimumFractionDigits(0);
        nf2x.setMaximumFractionDigits(fractionPlaces);

        boolean ampm = false;
        boolean pm = false;
        if (base == 1224)
            {
//            base = 12;
            ampm = true;
            if (d >= 12.0)
                {
                d -= 12.0;
                pm = true;
                }
            }

		double dd = Math.abs(d);
//        dd += 0.0000001;

		int h = (int)dd;
		int m = (int)(60.0*(dd-(double)h));
		double s = 3600.0*(dd-(double)h-(double)m/60.0);

        if (Tools.parseDouble(nf2x.format(s).trim()) >= 60.0)
            {
            s = 0.0;
            m += 1;
            }
        if (m > 59)
            {
            m -= 60;
            h += 1;
            }
        if (d > 0 && h >= base)
            {
            if (base == 180 || (base == 12 && !ampm))
                {
                d = -d;
                if (s != 0)
                    {
                    s = 60 - s;
                    m = 59 - m;
                    h--;
                    }
                 else if (m != 0)
                    {
                    m = 59 - m;
                    h--;
                    }
                }
            else if (base == 12 && ampm)
                {
                h -= base;
                pm = !pm;
                }
            else if(base == 90)
                {
                h = 90;
                m = 0;
                s = 0;
                }
            else
                h -= base;
            }
        else if (base == 90 && d < -90)
            {
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
    
    
    double mapToBase(double num, int base, boolean negative)
        {
        double x = num;
        if (base==90)
            x = x>=90 ? (negative ? -90 : 90) : (negative ? -x : x);  //-89.999722 : 89.999722
        else if (base == 180 || base == 12)
            {
            x %= 2*base;
            x = x>base ? -2*base+x : x;
            x = negative ? -x : x;
            }
        else
            {
            x %= base;
            x = negative ? base-x : x;
            }
        return x;
        }    

    /** Returns an ImageIcon, or null if the path was invalid. */
    protected ImageIcon createImageIcon(String path, String description) 
        {
        java.net.URL imgURL = getClass().getClassLoader().getResource(path);
        if (imgURL != null) 
            {
            return new ImageIcon(imgURL, description);
            }
        else 
            {
            IJ.showMessage("Couldn't find icon file: " + path);
            return null;
            }
        }   
    
    String convertToText(double increment)
            {
            if      (increment == 0.0000000001) return "      0.0000000001";
            else if (increment == 0.000000001)  return "        0.000000001";
            else if (increment == 0.00000001)   return "          0.00000001";
            else if (increment == 0.0000001)    return "            0.0000001";
            else if (increment == 0.000001)     return "              0.000001";
            else if (increment == 0.00001)      return "                0.00001";
            else if (increment == 0.0001)       return "                  0.0001";
            else if (increment == 0.001)        return "                    0.001";
            else if (increment == 0.01)         return "                    0.010";
            else if (increment == 0.1)          return "                    0.100";
            else if (increment == 0.5)          return "                    0.500";
            else if (increment == 1.0)          return "                    1.000";
            else if (increment == 10.0)         return "                  10.000";
            else if (increment == 100.0)        return "                100.000";
            else if (increment == 1000.0)       return "              1000.000";
            else if (increment == 10000.0)      return "            10000.000";
            else if (increment == 100000.0)     return "          100000.000";
            else if (increment == 1000000.0)    return "        1000000.000";
            else if (increment == 10000000.0)   return "      10000000.000";
            else if (increment == 100000000.0)  return "    100000000.000";
            else if (increment == 1000000000.0) return "1000000000.000";
            else return "                    1.000";
            }

    public void getPrefs()
        {
        frameLocationX = (int)Prefs.get ("astrometry.frameLocationX",frameLocationX);
        frameLocationY = (int)Prefs.get ("astrometry.frameLocationY",frameLocationY);        
        
        userKey = Prefs.get ("astrometry.userKey",userKey);
        
        useAlternateAstrometryServer = Prefs.get ("astrometry.useAlternateAstrometryServer", useAlternateAstrometryServer);
        alternateAstrometryUrlBase = Prefs.get ("astrometry.alternateAstrometryUrlBase",alternateAstrometryUrlBase);
        
        processStack = Prefs.get ("astrometry.processStack",processStack);
        startSlice = (int)Prefs.get ("astrometry.startSlice",startSlice);
        endSlice = (int)Prefs.get ("astrometry.endSlice",endSlice);   

        autoSave = Prefs.get ("astrometry.autoSave", autoSave);
        DPSaveRawWithWCS = Prefs.get ("astrometry.DPSaveRawWithWCS", DPSaveRawWithWCS);
        skipIfHasWCS = Prefs.get ("astrometry.skipIfHasWCS", skipIfHasWCS);

        annotate = Prefs.get ("astrometry.annotate", annotate);
        annotateRadius = Prefs.get ("astrometry.annotateRadius",annotateRadius);
        annotateRadiusStep = Prefs.get ("astrometry.annotateRadiusStep",annotateRadiusStep);
        addAnnotationsToHeader = Prefs.get ("astrometry.addAnnotationsToHeader", addAnnotationsToHeader);

        useMedianFilter = Prefs.get ("astrometry.useMedianFilter", useMedianFilter);
        medianFilterRadius = (int)Prefs.get ("astrometry.medianFilterRadius",medianFilterRadius);   
        
        minPeakFindToleranceSTDEV = Prefs.get ("astrometry.minPeakFindToleranceSTDEV",minPeakFindToleranceSTDEV);
        useMaxPeakFindValue=Prefs.get ("astrometry.useMaxPeakFindValue", useMaxPeakFindValue);
        maxPeakFindValue = Prefs.get ("astrometry.maxPeakFindValue",maxPeakFindValue);

        maxNumStars = (int)Prefs.get ("astrometry.maxNumStars",maxNumStars); 
        useDistortionOrder = Prefs.get ("astrometry.useDistortionOrder", useDistortionOrder);
        distortionOrder = (int)Prefs.get ("astrometry.distortionOrder",distortionOrder); 

        useCentroid = Prefs.get ("astrometry.useCentroid", useCentroid);
        apertureRadius = Prefs.get ("astrometry.apertureRadius",apertureRadius);
        apertureBack1 = Prefs.get ("astrometry.apertureBack1",apertureBack1);
        apertureBack2 = Prefs.get ("astrometry.apertureBack2",apertureBack2);

        useScale = Prefs.get ("astrometry.useScale", useScale);
        scaleEstimate = Prefs.get ("astrometry.scaleEstimate",scaleEstimate);
        scaleError = Prefs.get ("astrometry.scaleError",scaleError);


        useRaDec = Prefs.get ("astrometry.useRaDec", useRaDec);
        showLog = Prefs.get ("astrometry.showLog", showLog);
        ra = Prefs.get ("astrometry.ra",ra);
        dec = Prefs.get ("astrometry.dec",dec);
        raDecRadius = Prefs.get ("astrometry.raDecRadius",raDecRadius);
        raDecRadiusStep = Prefs.get ("astrometry.raDecRadiusStep",raDecRadiusStep);
        
        noiseTolStep = Prefs.get ("astrometry.noiseTolStep",noiseTolStep);
        maxPeakFindStep = Prefs.get ("astrometry.maxPeakFindStep",maxPeakFindStep);
        apertureStep = Prefs.get ("astrometry.apertureStep",apertureStep);
        scaleStep = Prefs.get ("astrometry.scaleStep",scaleStep);
        }
        
    public void savePrefs() {

        frameLocationX = astrometrySetupFrame.getLocation().x;
        frameLocationY = astrometrySetupFrame.getLocation().y;        
        Prefs.set ("astrometry.frameLocationX",frameLocationX);
        Prefs.set ("astrometry.frameLocationY",frameLocationY);          
        Prefs.set ("astrometry.userKey",userKey);
        Prefs.set ("astrometry.useAlternateAstrometryServer", useAlternateAstrometryServer);
        Prefs.set ("astrometry.alternateAstrometryUrlBase",alternateAstrometryUrlBase);
        Prefs.set ("astrometry.processStack",processStack);
        Prefs.set ("astrometry.startSlice",startSlice);
        Prefs.set ("astrometry.endSlice",endSlice);   

        Prefs.set ("astrometry.autoSave", autoSave);
        Prefs.set ("astrometry.DPSaveRawWithWCS", DPSaveRawWithWCS);
        Prefs.set ("astrometry.skipIfHasWCS", skipIfHasWCS);

        Prefs.set ("astrometry.annotate", annotate);
        Prefs.set ("astrometry.annotateRadius",annotateRadius);
        Prefs.set ("astrometry.annotateRadiusStep",annotateRadiusStep); 
        Prefs.set ("astrometry.addAnnotationsToHeader", addAnnotationsToHeader);

        Prefs.set ("astrometry.useMedianFilter", useMedianFilter);
        Prefs.set ("astrometry.medianFilterRadius",medianFilterRadius);   

        Prefs.set ("astrometry.minPeakFindToleranceSTDEV",minPeakFindToleranceSTDEV);
        Prefs.set ("astrometry.useMaxPeakFindValue", useMaxPeakFindValue);
        Prefs.set ("astrometry.maxPeakFindValue",maxPeakFindValue);

        Prefs.set ("astrometry.maxNumStars",maxNumStars);
        
        Prefs.set ("astrometry.useDistortionOrder", useDistortionOrder);
        Prefs.set ("astrometry.distortionOrder",distortionOrder);

        Prefs.set ("astrometry.useCentroid", useCentroid);
        Prefs.set ("astrometry.apertureRadius",apertureRadius);
        Prefs.set ("astrometry.apertureBack1",apertureBack1);
        Prefs.set ("astrometry.apertureBack2",apertureBack2);

        Prefs.set ("astrometry.useScale", useScale);
        Prefs.set ("astrometry.scaleEstimate",scaleEstimate);
        Prefs.set ("astrometry.scaleError",scaleError);


        Prefs.set ("astrometry.useRaDec", useRaDec);
        if (!useCCRaDec) Prefs.set ("astrometry.ra",ra);
        if (!useCCRaDec) Prefs.set ("astrometry.dec",dec);
        Prefs.set ("astrometry.raDecRadius",raDecRadius);  
        Prefs.set ("astrometry.raDecRadiusStep",raDecRadiusStep);
        
        Prefs.set ("astrometry.noiseTolStep",noiseTolStep);
        Prefs.set ("astrometry.maxPeakFindStep",maxPeakFindStep);
        Prefs.set ("astrometry.apertureStep",apertureStep);
        Prefs.set ("astrometry.scaleStep",scaleStep);
        Prefs.set ("astrometry.showLog", showLog);
        }   

}
