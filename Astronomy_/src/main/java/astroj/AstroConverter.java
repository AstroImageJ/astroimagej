// AstroConvertor.java
package astroj;

import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.Timer;
import java.util.TimerTask;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.border.Border;
import javax.swing.event.*;

import ij.*;
import java.text.*;
import java.net.*;
import java.io.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


import ij.gui.*;
import ij.util.*;

import util.BrowserOpener;


/**
 * This plugin converts sky coordinates in a variety of formats.
 * @author K.A. Collins, University of Louisville
 * @date 2011-12-27
 * @version 1.0
 * 
 */
public class AstroConverter implements ItemListener, ActionListener, ChangeListener, MouseListener   // PropertyChangeListener, MouseMotionListener, MouseWheelListener,
	{
    String      version = "3.4.0";
	TimerTask	task = null;
	Timer		timer = null;

	TimerTask	spinnertask = null;
	Timer		spinnertimer = null;

	TimerTask	scrollfinishedtask = null;
	Timer		scrollfinishedtimer = null;

	JFrame dialogFrame;
    int dialogFrameLocationX = -999999;
    int dialogFrameLocationY = -999999;
    int moonPhase = 0;

    public static final boolean FORWARD = true;
    public static final boolean REVERSE = false;

    SimpleDateFormat f = new SimpleDateFormat("yyyy-MM-dd   HH:mm:ss");

    DecimalFormat uptoTwoPlaces = new DecimalFormat("0.##", IJU.dfs);
    DecimalFormat noPlaces = new DecimalFormat("0", IJU.dfs);
    DecimalFormat onePlaces = new DecimalFormat("0.0", IJU.dfs);
    DecimalFormat twoPlaces = new DecimalFormat("0.00", IJU.dfs);
    DecimalFormat threePlaces = new DecimalFormat("0.000", IJU.dfs);
    DecimalFormat fourPlaces = new DecimalFormat("0.0000", IJU.dfs);
    DecimalFormat fivePlaces = new DecimalFormat("0.00000", IJU.dfs);
    DecimalFormat sixPlaces = new DecimalFormat("0.000000", IJU.dfs);
    DecimalFormat sevenPlaces = new DecimalFormat("0.0000000", IJU.dfs);
    DecimalFormat eightPlaces = new DecimalFormat("0.00000000", IJU.dfs);
    DecimalFormat uptoFourPlaces = new DecimalFormat("0.####", IJU.dfs);
    DecimalFormat uptoSixPlaces = new DecimalFormat("0.0#####", IJU.dfs);
    DecimalFormat upto20Places = new DecimalFormat("0.####################", IJU.dfs);
    DecimalFormat twoDigits = new DecimalFormat();
    DecimalFormat fourDigits = new DecimalFormat();
    
//    DecimalFormatSymbols symbols = fourPlaces.getDecimalFormatSymbols();
//    char decSep = symbols.getDecimalSeparator(); 
//    char thouSep = symbols.getGroupingSeparator();    

    double jdNow, lstNow, epochNow, newnum, bjdEOI, hjdEOI, hjdEOICorr, bjdEOICorr;
    double pmTwiJD, amTwiJD;
    double airmass = 1.0;
    double jdEOI=2451545.0, lstEOI, epochEOI, oldjdEOI;
    int jdEOIStep = GregorianCalendar.DAY_OF_MONTH;
    int jdEOIStepFactor = 1;
    double leapSec=0.0, dT = 0.0, g, TDB;
    double nowTimeZoneOffset = 0, eoiTimeZoneOffset = 0;
    double[] coords;
    double[][] ssbAngles = new double[9][2]; //0=Mercury, 1=Venus, 2=Moon, 3=Mars, 4=Jupiter, 5=Saturn, 6=Uranus, 7=Neptune, 8=Pluto; 0=objSSb distance, 1=SSB altitude



    double[] barycenter = new double[6];
    double[] sunxyz = new double[3];
    double[] sunxyzvel = new double[3];
    double[] planetbarycor = new double[6];
    double[][] planetxyz = new double[9][3];
    double[][] planetvelxyz = new double[9][3];
    double[] moonxyz = new double[3];
    double barytcor = 0.0;
    double baryvcor = 0.0;
    double jd_el = 0.0;
    double [] incl = {0.,0.,0.,0.,0.,0.,0.,0.,0.};  // inclination
    double [] Omega = {0.,0.,0.,0.,0.,0.,0.,0.,0.}; // longit of asc node
    double [] omega = {0.,0.,0.,0.,0.,0.,0.,0.,0.}; // longit of perihelion
    double [] a = {0.,0.,0.,0.,0.,0.,0.,0.,0.};     // semimajor axis
    double [] daily = {0.,0.,0.,0.,0.,0.,0.,0.,0.}; // mean daily motion, degr.
    double [] ecc = {0.,0.,0.,0.,0.,0.,0.,0.,0.};   // eccentricity
    double [] L_0 = {0.,0.,0.,0.,0.,0.,0.,0.,0.};   // starting longitude (?)


    double[] utDateNow = {0.0, 0.0, 0.0, 0.0};
    double[] locDateNow = {0.0, 0.0, 0.0, 0.0};
    double[] utDateJ2000 = {2000, 1, 1, 12}; //11.982171};
    double[] utDateEOI = {2000, 1, 1, 12};
    double[] locDateEOI = {2000, 1, 1, 12};
    double[] pmTwiDate = {2000, 1, 1, 12};
    double[] amTwiDate = {2000, 1, 1, 12};

    boolean J2000radecSource=true, J2000altazSource = false;
    boolean showSexagesimal=true, optionChanged = true, showToolTips=true, updateText = false, autoTimeZone = true, useAMPM = true;
    boolean usePM=true, usePrec=true, useNut=true, useAber=true, useRefr=true, useHarvard=true, useOhioState = false;
    boolean newSimbadData=false, newleapSecTableReady=false, ssObject=false, twiLocal = true, running = false;
    boolean SIMBADAccessFailed = false;
    boolean OSUAccessFailed = false;
    boolean validObjectID = false;
    boolean useCustomObservatoryList = false;
    boolean reportSSBDown = true;
    boolean useProxy = false;
    boolean useSkyMapBeta = true;
    
    String proxyAddress = "proxyserver.mydomain.com";
    int proxyPort = 8080;
    SocketAddress socketaddr = new InetSocketAddress(proxyAddress, proxyPort);
    Proxy proxy = new Proxy(Proxy.Type.HTTP, socketaddr);
    
   

    JScrollPane mainScrollPane;
    ImageIcon updateLeapSecTableIcon, jdEOIupIcon, jdEOIdownIcon, pmTwilightIcon, amTwilightIcon, simbadIcon, skymapIcon;
    ImageIcon[] moonIcon = new ImageIcon[56];
    ImageIcon currentMoonPhaseIcon;
    JMenuBar menuBar;
    JMenu fileMenu, prefsMenu, networkMenu, helpMenu;
    JMenuItem savePrefsMenuItem, exitMenuItem, showLeapSecondTableMenuItem, helpMenuItem, setProxyAddressMenuItem, setProxyPortMenuItem;
    JCheckBoxMenuItem showSexagesimalCB, useHarvardCB, useOhioStateCB, useSkyMapBetaCB, useProxyCB, autoTimeZoneCB, reportSSBDownCB;
    JCheckBoxMenuItem useAMPMCB, showLocalTwilightCB, showToolTipsCB, useCustomObservatoryListCB;
    JCheckBoxMenuItem usePMCB, usePrecCB, useNutCB, useAberCB, useRefrCB;
    JCheckBox useNowEpochCB, autoLeapSecCB, useOhioStateCheckBox;
    boolean useNowEpoch = false, autoLeapSec = true, jdEOIupMouseDown = false, jdEOIdownMouseDown = false, spinnerActive = false;
    Object mouseSource, textFieldSource;
    Double[] leapSecJD,  TAIminusUTC, baseMJD, baseMJDMultiplier;
//    String leapSource = " (default)";


    JTextField currentUTDateTextField, currentUTTimeTextField, currentEpochTextField, currentJDTextField, currentLSTTextField;
    JTextField currentLocDateTextField, currentLocTimeTextField;
    JTextField objectIDTextField, leapSecTextField, eoiBJDTextField, eoiBJDdTTextField, eoiHJDTextField, eoiHJDdTTextField;
//    JLabel leapSourceLabel;
    JComboBox  observatoryIDComboBox;
    JButton updateLeapSecTableButton, jdEOIupButton,jdEOIdownButton, nowEOIButton, pmTwilightButton, amTwilightButton;
    JButton jdLocEOIupButton,jdLocEOIdownButton, skyMapButton, simbadButton;
    JLabel moonPhaseLabel, OSULabel;
    JTextField eoiUTDateTextField, eoiUTTimeTextField, eoiJDTextField, eoiLSTTextField;
    JTextField UTDateJ2000TextField, epochJ2000TextField, jdJ2000TextField, lstJ2000TextField;
    JTextField eoiLocDateTextField, eoiLocTimeTextField, eoiPMTwilightTextField, eoiAMTwilightTextField;
    JTextField pmRATextField, pmDecTextField, latTextField, lonTextField, altTextField;
    JTextField raJ2000TextField, decJ2000TextField, eclLonJ2000TextField, eclLatJ2000TextField;
    JTextField raB1950TextField, decB1950TextField, galLonB1950TextField, galLatB1950TextField;
    JTextField raEOITextField, decEOITextField, eclLonEOITextField, eclLatEOITextField;
    JTextField haEOITextField, zdEOITextField, dirTextField, airmassTextField, altEOITextField, azEOITextField;

    JPanel moonPanel, mercuryPanel, venusPanel, marsPanel, jupiterPanel;
    JPanel saturnPanel, uranusPanel, neptunePanel, plutoPanel;
    JTextField moonAltTextField, mercuryAltTextField, venusAltTextField, marsAltTextField, jupiterAltTextField;
    JTextField saturnAltTextField, uranusAltTextField, neptuneAltTextField, plutoAltTextField;
    JTextField moonTextField, mercuryTextField, venusTextField, marsTextField, jupiterTextField;
    JTextField saturnTextField, uranusTextField, neptuneTextField, plutoTextField;

    JPanel[] ssbPanels = {mercuryPanel, venusPanel, moonPanel, marsPanel, jupiterPanel, saturnPanel, uranusPanel, neptunePanel, plutoPanel};
    JTextField[] ssbDistanceFields = {mercuryTextField, venusTextField, moonTextField, marsTextField, jupiterTextField, saturnTextField, uranusTextField, neptuneTextField, plutoTextField};
    JTextField[] ssbAltitudeFields = {mercuryAltTextField, venusAltTextField, moonAltTextField, marsAltTextField, jupiterAltTextField, saturnAltTextField, uranusAltTextField, neptuneAltTextField, plutoAltTextField};
    String[] ssbNames = {"Mercury", "Venus", "Moon", "Mars", "Jupiter", "Saturn", "Uranus", "Neptune", "Pluto"};

    JPopupMenu jdEOIsteppopup;
    SpinnerListModel jdEOIstepmodel;
    JSpinner jdEOIstepspinner, timeZoneOffsetSpinner;
    String[] spinnerscalelist = new String[]
                {
                "      1 second",
                "      1 minute",
                "      10 minutes",
                "          1 hour",
                "             1 day",
                "           1 week",
                "         1 month",
                "       1 year",
                "     10 years",
                "   100 years",
                " 1000 years"
                };

    String epochJ2000Text="2000.0", jdJ2000Text="2451545.0", lstJ2000Text="";
    String objectIDText="";
    int selectedObservatory = 0, numSavedLSEntries=0;
    String[] observatoryIDs =  {"Custom Lon, Lat, and Alt entry",
                                "Moore Observatory, Louisville, KY",
                                "Mt. Kent Observatory, Queensland, Australia"
                               };
    Double[] observatoryLats = {0.0,
                                38.344791,
                                -27.797861
                               };
    Double[] observatoryLons = {0.0,
                                -85.528476,
                                151.855417
                               };
    Double[] observatoryAlts = {0.0,
                                229.0,
                                682.0
                               };

    String raJ2000Text="enter ra", decJ2000Text="enter dec", eclLonJ2000Text="enter lon", eclLatJ2000Text ="enter lat";
    String raB1950Text="enter ra", decB1950Text="enter dec", galLonB1950Text="enter lon", galLatB1950Text="enter lat ";
    String raEOIText="enter ra", decEOIText="enter dec", eclLonEOIText="enter lon", eclLatEOIText ="enter lat";
    String haEOIText="hour angle", zdEOIText="zenith distance", dirText="---", altEOIText ="altitude", azEOIText="azimuth", airmassText="airmass";

    String frameTitle = "Coordinate Converter";
    String objectText;

    Border grayBorder = new CompoundBorder(BorderFactory.createLineBorder(Color.GRAY), new EmptyBorder(2,2,3,2));    //top,left,bottom,right
    Border greenBorder = new CompoundBorder(BorderFactory.createLineBorder(new Color(0,200,0)), new EmptyBorder(2,2,3,2));

    double[] caution = {5.0, 15.0, 40.0, 5.0, 5.0, 5.0, 5.0, 5.0, 5.0};
    double[] warning = {2.0, 5.0, 15.0, 2.0, 2.0, 2.0, 2.0, 2.0, 2.0};

    double epochJ2000=2000.0, jdJ2000=2451545.0;
    double lat=0.0, lon=0.0, alt = 0.0, pmRA=0.0, pmDec=0.0;

    double[] radecJ2000 = {0.0, 0.0};
    double[] elonlatJ2000 = {0.0, 0.0};
    double[] radecB1950 = {0.0, 0.0};
    double[] glonlatB1950 = {0.0, 0.0};

    double[] radecEOI = {0.0, 0.0};
    double[] elonlatEOI = {0.0, 0.0};
    double[] hazdEOI = {0.0, 0.0};
    double[] altazEOI = {0.0, 0.0};

    boolean showPanel = true;
    boolean dp = false;
    boolean ignoreObservatoryAction = false;
    boolean coordinateEntryEnabled = true;
    boolean timeEnabled = true;
    boolean newradecJ2000=true, newelonlatJ2000, newradecB1950, newglonlatB1950, newradecEOI, newaltazEOI, newelonlatEOI;

    String osname = System.getProperty("os.name");
    boolean isWin = osname.startsWith("Windows");
    boolean isLinux = osname.startsWith("Linux");
    boolean isMac = !isWin && osname.startsWith("Mac");
    String separator = System.getProperty("file.separator");

    Color mainBorderColor = new Color(118,142,229);
    Color leapRed = new Color(255,160,160);
    Color leapYellow = new Color(255,255,200);
    Color leapGreen = new Color(225,255,225);
    Color leapGray = new Color(240,240,240);

    Calendar utc, amutc, pmutc, local;

    String prefsDir = System.getProperty("user.dir");
    String homeDir;

    String fontName = "Dialog";
    Font p12 = new Font(fontName,Font.PLAIN,12);
    Font b12 = new Font(fontName,Font.BOLD,12);
    Font b11 = new Font(fontName,Font.BOLD,11);
    Font b14 = new Font(fontName,Font.BOLD,14);

    static final double J2000 =  2451545. ; // the julian epoch 2000
    static final double DEG_IN_RADIAN = 57.2957795130823;
    static final double HRS_IN_RADIAN = 3.81971863420549;
    static final double ARCSEC_IN_RADIAN = 206264.806247096;
    static final double PI_OVER_2 = 1.5707963267949;
    static final double PI = 3.14159265358979;
    static final double TWOPI = 6.28318530717959;
    static final double EARTHRAD_IN_AU = 23454.7910556298; // earth radii in 1 AU
    static final double EARTHRAD_IN_KM = 6378.1366; // equatorial
    static final double KMS_AUDAY = 1731.45683633; // 1731 km/sec = 1 AU/d
    static final double SPEED_OF_LIGHT = 299792.458; // exact, km/s.
    static final double SS_MASS = 1.00134198; // solar system mass, M_sun
    static final double ASTRO_UNIT = 1.4959787066e11; // 1 AU in meters
    static final double LIGHTSEC_IN_AU = 499.0047863852; // 1 AU in SI meters
    static final double OMEGA_EARTH = 7.292116e-5; // inertial ang vel of earth
    static final double SID_RATE = 1.0027379093;  // sidereal/solar ratio
    static final double FLATTEN = 0.003352813;  // flattening, 1/298.257
    static final double EQUAT_RAD = 6378137.;   // equatorial radius, meters
    static final double [] mass = {1.660137e-7, 2.447840e-6, 3.040433e-6,
          3.227149e-7,9.547907e-4,2.858776e-4,4.355401e-5,5.177591e-5,
          7.69e-9};  // IAU 1976 masses of planets in terms of solar mass
                     // used to weight barycenter calculation only.

    public AstroConverter(boolean showWindow, boolean dpControlled, String title)
        {
        Locale.setDefault(IJU.locale);
        showPanel = showWindow;
        dp = dpControlled;
        frameTitle = title;
        buildPanels();
        }


    public AstroConverter(boolean showPanel)
        {
        Locale.setDefault(IJU.locale);
        this.showPanel = showPanel;
        buildPanels();
        }


    void buildPanels(){
//        GraphicsEnvironment gee = GraphicsEnvironment.getLocalGraphicsEnvironment();
//        Font[] fonts = gee.getAllFonts();
//        for (Font ff : fonts)
//            {
//            IJ.log(ff.getFontName());
//            }

        initDefaultLeapSecs();
        getPrefs();
        if (dp) useNowEpoch = false;
        socketaddr = new InetSocketAddress(proxyAddress, proxyPort);
        proxy = new Proxy(Proxy.Type.HTTP, socketaddr);         
        getObservatories();

        if (isWin)
                {
                try {UIManager.setLookAndFeel("com.sun.java.swing.plaf.windows.WindowsLookAndFeel");}
//                try {UIManager.setLookAndFeel("javax.swing.plaf.metal.MetalLookAndFeel");}
//                try {UIManager.setLookAndFeel("com.sun.java.swing.plaf.windows.WindowsLookAndFeel");}
//                try {UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());}
                catch (Exception e) { }
                }
        else if (isLinux)
                {
                try {UIManager.setLookAndFeel("javax.swing.plaf.metal.MetalLookAndFeel");}
                catch (Exception e) { }
                }
        else
                {
                try {UIManager.setLookAndFeel("javax.swing.plaf.metal.MetalLookAndFeel");}
                catch (Exception e) { }
                }
//            try {UIManager.setLookAndFeel("com.sun.java.swing.plaf.windows.WindowsLookAndFeel");}
//            try {UIManager.setLookAndFeel("com.sun.java.swing.plaf.nimbus.NimbusLookAndFeel");}
//            try {UIManager.setLookAndFeel("com.sun.java.swing.plaf.motif.MotifLookAndFeel");}


//        try {UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());}
//        catch (Exception e) { }


        if (!isWin)
            {
            p12 = new Font(fontName, Font.PLAIN,11);
            b12 = new Font(fontName, Font.BOLD,11);
            b11 = new Font(fontName, Font.BOLD,10);
            b14 = new Font(fontName, Font.BOLD,13);
            }

        int iconWidth = 40;
        int iconHeight = 20;
        Dimension buttonSize = new Dimension(70,30);
        Dimension iconDimension = new Dimension(iconWidth, iconHeight);
        Insets buttonMargin = new Insets(0,0,0,0); //top,left,bottom,right
        twoDigits.setMinimumIntegerDigits(2);
        twoDigits.setMaximumFractionDigits(0);
        twoDigits.setDecimalFormatSymbols(IJU.dfs);
        fourDigits.setMinimumIntegerDigits(4);
        fourDigits.setGroupingSize(0);
        fourDigits.setDecimalFormatSymbols(IJU.dfs);

        Dimension coordSize = new Dimension(100, 21);
        Insets coordMargin = new Insets(2,2,2,2); //top,left,bottom,right

        ImageIcon dialogFrameIcon = createImageIcon("images/coordinate_converter.png", "CC_Icon");
        updateLeapSecTableIcon = createImageIcon("images/downloadleaps.png", "Download");
        jdEOIupIcon = createImageIcon("images/spinnerup.png", "jdEOISpinnerUp");
        jdEOIdownIcon = createImageIcon("images/spinnerdown.png", "jdEOISpinnerDown");
        pmTwilightIcon = createImageIcon("images/pmtwilight.png", "PM Twilight");
        amTwilightIcon = createImageIcon("images/amtwilight.png", "AM Twilight");
        simbadIcon = createImageIcon("images/simbad.png", "Simbad Link");
        skymapIcon = createImageIcon("images/skymap.png", "Sky-Map Link");

        for (int i=0; i<moonIcon.length; i++)
            {
            moonIcon[i] = createImageIcon("images/moonphases"+twoDigits.format(i+1)+".png", "moon phase");
            }

        if (this.getClass().toString().contains("Coordinate_Converter"))
            frameTitle = "AstroCC Coordinate Converter";
		dialogFrame = new JFrame (frameTitle);
        dialogFrame.setIconImage(dialogFrameIcon.getImage());
        if (!dp) dialogFrame.setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        dialogFrame.addWindowListener(new WindowAdapter(){
            @Override
            public void windowClosing(WindowEvent e){
            if (dp)
                dialogFrame.setVisible(false);
            else
                saveAndClose();
            }});
		JPanel mainPanel = new JPanel (new SpringLayout());
		mainPanel.setBorder (BorderFactory.createEmptyBorder(10,10,10,10));

        mainScrollPane = new JScrollPane(mainPanel);



        // MAKE MENUBAR and MENUS
        menuBar = new JMenuBar();

        fileMenu = new JMenu("File");

        showLeapSecondTableMenuItem = new JMenuItem("Show leap second table");
        showLeapSecondTableMenuItem.addActionListener(this);
        fileMenu.add(showLeapSecondTableMenuItem);

        fileMenu.addSeparator();

        String savePrefsText = "Save preferences to AstroImageJ memory";
        if (this.getClass().toString().contains("Coordinate_Converter"))
            savePrefsText = "Save preferences";

        savePrefsMenuItem = new JMenuItem(savePrefsText);
        savePrefsMenuItem.addActionListener(this);
        fileMenu.add(savePrefsMenuItem);

        fileMenu.addSeparator();

        exitMenuItem = new JMenuItem("Exit coordinate converter");
        exitMenuItem.addActionListener(this);
        fileMenu.add(exitMenuItem);

        menuBar.add(fileMenu);


        prefsMenu = new JMenu("Preferences");

        showSexagesimalCB = new JCheckBoxMenuItem("Show coordinates in sexagesimal format",showSexagesimal);
        showSexagesimalCB.addItemListener(this);
        prefsMenu.add(showSexagesimalCB);

        autoTimeZoneCB = new JCheckBoxMenuItem("Use computer time zone, deselect to manually enter offset from UTC",autoTimeZone);
        autoTimeZoneCB.addItemListener(this);
        prefsMenu.add(autoTimeZoneCB);

        useAMPMCB = new JCheckBoxMenuItem("Use 12-hour local clock format, deselect to use 24-hour format",useAMPM);
        useAMPMCB.addItemListener(this);
        prefsMenu.add(useAMPMCB);

        showLocalTwilightCB = new JCheckBoxMenuItem("Show local twilight time, deselect for UTC format",twiLocal);
        showLocalTwilightCB.addItemListener(this);
        prefsMenu.add(showLocalTwilightCB);

        reportSSBDownCB = new JCheckBoxMenuItem("Report SSBs as 'DOWN', deselect to show negative altitude",reportSSBDown);
        reportSSBDownCB.addItemListener(this);
        prefsMenu.add(reportSSBDownCB);

        showToolTipsCB = new JCheckBoxMenuItem("Show tool tips",showToolTips);
        showToolTipsCB.addItemListener(this);
        prefsMenu.add(showToolTipsCB);

        prefsMenu.addSeparator();

        useCustomObservatoryListCB = new JCheckBoxMenuItem("Use custom observatory list (restart of AstroCC required)",useCustomObservatoryList);
        useCustomObservatoryListCB.addItemListener(this);
        prefsMenu.add(useCustomObservatoryListCB);

        prefsMenu.addSeparator();

        usePMCB = new JCheckBoxMenuItem("Include proper motion correction",usePM);
        usePMCB.addItemListener(this);
        prefsMenu.add(usePMCB);

        usePrecCB = new JCheckBoxMenuItem("Include precession correction",usePrec);
        usePrecCB.addItemListener(this);
        prefsMenu.add(usePrecCB);

        useNutCB = new JCheckBoxMenuItem("Include nutation correction",useNut);
        useNutCB.addItemListener(this);
        prefsMenu.add(useNutCB);

        useAberCB = new JCheckBoxMenuItem("Include stellar aberration correction",useAber);
        useAberCB.addItemListener(this);
        prefsMenu.add(useAberCB);

        useRefrCB = new JCheckBoxMenuItem("Include atmospheric refraction correction to altitude",useRefr);
        useRefrCB.addItemListener(this);
        prefsMenu.add(useRefrCB);
        menuBar.add(prefsMenu);
        
        networkMenu = new JMenu("Network");
        
        useHarvardCB = new JCheckBoxMenuItem("Use Harvard SIMBAD server, deselect to use CDS Server",useHarvard);
        useHarvardCB.addItemListener(this);
        networkMenu.add(useHarvardCB);

        useOhioStateCB = new JCheckBoxMenuItem("Use Ohio State BJD server, deselect to calculate internally",useOhioState);
        useOhioStateCB.addItemListener(this);
        networkMenu.add(useOhioStateCB);  
        
        useSkyMapBetaCB = new JCheckBoxMenuItem("Use Sky-Map.org beta server, deselect to use standard site",useSkyMapBeta);
        useSkyMapBetaCB.addItemListener(this);
        networkMenu.add(useSkyMapBetaCB);        
        
        useProxyCB = new JCheckBoxMenuItem("Use proxy server for internet access",useProxy);
        useProxyCB.addItemListener(this);
        networkMenu.add(useProxyCB); 
        
        setProxyAddressMenuItem = new JMenuItem("Set proxy server address...");
        setProxyAddressMenuItem.addActionListener(this);
        networkMenu.add(setProxyAddressMenuItem);  
        
        setProxyPortMenuItem = new JMenuItem("Set proxy server port number...");
        setProxyPortMenuItem.addActionListener(this);
        networkMenu.add(setProxyPortMenuItem);        
        
        menuBar.add(networkMenu);

        helpMenu = new JMenu("Help");
        helpMenuItem = new JMenuItem("Help...");
        helpMenuItem.addActionListener(this);
        helpMenu.add(helpMenuItem);

        JMenuItem verMenuItem = new JMenuItem("AstroCC Version "+version);
        helpMenu.add(verMenuItem);

        menuBar.add(helpMenu);


        if (autoTimeZone)
            {
            TimeZone tz = TimeZone.getDefault();
            Calendar now = Calendar.getInstance(tz);
            nowTimeZoneOffset = tz.getOffset(now.ERA, now.YEAR, now.MONTH, now.DAY_OF_MONTH, now.DAY_OF_WEEK, now.MILLISECOND)/3600000.0;
            }

        JPanel currentTimePanel = new JPanel(new SpringLayout());
        currentTimePanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(mainBorderColor, 1), "Current UTC-based Time", TitledBorder.LEFT, TitledBorder.TOP, b12, Color.DARK_GRAY));

        JLabel currentUTDateLabel = new JLabel (" UTC:");
        currentUTDateLabel.setFont(p12);
        currentUTDateLabel.setToolTipText("Current UTC date and time");
        currentUTDateLabel.setHorizontalAlignment (JTextField.RIGHT);
		currentTimePanel.add (currentUTDateLabel);

		currentUTDateTextField = new JTextField("");
        currentUTDateTextField.setMargin(coordMargin);
        currentUTDateTextField.setFont(p12);
        currentUTDateTextField.setToolTipText("Current UTC date");
        currentUTDateTextField.setEditable(false);
        currentUTDateTextField.setBackground(leapGray);
		currentUTDateTextField.setPreferredSize(new Dimension((int)(coordSize.width*0.8),coordSize.height));
		currentUTDateTextField.setHorizontalAlignment(JTextField.LEFT);
        currentUTDateTextField.addActionListener(this);
        currentTimePanel.add (currentUTDateTextField);

		currentUTTimeTextField = new JTextField("");
        currentUTTimeTextField.setMargin(coordMargin);
        currentUTTimeTextField.setFont(p12);
        currentUTTimeTextField.setToolTipText("Current UTC time");
        currentUTTimeTextField.setEditable(false);
        currentUTTimeTextField.setBackground(leapGray);
		currentUTTimeTextField.setPreferredSize(new Dimension((int)(coordSize.width*0.6),coordSize.height));
		currentUTTimeTextField.setHorizontalAlignment(JTextField.LEFT);
        currentUTTimeTextField.addActionListener(this);
        currentTimePanel.add (currentUTTimeTextField);

        JLabel currentLocDateLabel = new JLabel (" Local:");
        currentLocDateLabel.setFont(p12);
        currentLocDateLabel.setToolTipText("Current local date and time");
        currentLocDateLabel.setHorizontalAlignment (JTextField.RIGHT);
		currentTimePanel.add (currentLocDateLabel);

		currentLocDateTextField = new JTextField("");
        currentLocDateTextField.setMargin(coordMargin);
        currentLocDateTextField.setFont(p12);
        currentLocDateTextField.setToolTipText("Current local date");
        currentLocDateTextField.setEditable(false);
        currentLocDateTextField.setBackground(leapGray);
		currentLocDateTextField.setPreferredSize(new Dimension((int)(coordSize.width*0.8),coordSize.height));
		currentLocDateTextField.setHorizontalAlignment(JTextField.LEFT);
        currentLocDateTextField.addActionListener(this);
        currentTimePanel.add (currentLocDateTextField);

		currentLocTimeTextField = new JTextField("");
        currentLocTimeTextField.setMargin(coordMargin);
        currentLocTimeTextField.setFont(p12);
        currentLocTimeTextField.setToolTipText("Current local time");
        currentLocTimeTextField.setEditable(false);
        currentLocTimeTextField.setBackground(leapGray);
		currentLocTimeTextField.setPreferredSize(new Dimension((int)(coordSize.width*0.8),coordSize.height));
		currentLocTimeTextField.setHorizontalAlignment(JTextField.LEFT);
        currentLocTimeTextField.addActionListener(this);
        currentTimePanel.add (currentLocTimeTextField);

//        JLabel currentEpochLabel = new JLabel ("   Epoch:");
//        currentEpochLabel.setFont(p12);
//        currentEpochLabel.setToolTipText("Current Epoch in UTC time");
//        currentEpochLabel.setHorizontalAlignment (JTextField.RIGHT);
//		currentTimePanel.add (currentEpochLabel);
//
//		currentEpochTextField = new JTextField("");
//        currentEpochTextField.setMargin(coordMargin);
//        currentEpochTextField.setFont(p12);
//        currentEpochTextField.setToolTipText("Current Epoch in UTC time");
//        currentEpochTextField.setEditable(false);
//		currentEpochTextField.setPreferredSize(coordSize);
//		currentEpochTextField.setHorizontalAlignment(JTextField.LEFT);
//        currentEpochTextField.addActionListener(this);
//        currentTimePanel.add (currentEpochTextField);

        JLabel currentJDLabel = new JLabel (" JD:");
        currentJDLabel.setFont(p12);
        currentJDLabel.setToolTipText("Current Julian Date in UTC time");
        currentJDLabel.setHorizontalAlignment (JTextField.RIGHT);
		currentTimePanel.add (currentJDLabel);

		currentJDTextField = new JTextField("");
        currentJDTextField.setMargin(coordMargin);
        currentJDTextField.setFont(p12);
        currentJDTextField.setToolTipText("Current Julian Date in UTC time");
        currentJDTextField.setEditable(false);
        currentJDTextField.setBackground(leapGray);
		currentJDTextField.setPreferredSize(new Dimension((int)(coordSize.width*1.05),coordSize.height));
		currentJDTextField.setHorizontalAlignment(JTextField.LEFT);
        currentJDTextField.addActionListener(this);
        currentTimePanel.add (currentJDTextField);

        JLabel currentLSTLabel = new JLabel (" LST:");
        currentLSTLabel.setFont(p12);
        currentLSTLabel.setToolTipText("Current local sidereal time");
        currentLSTLabel.setHorizontalAlignment (JTextField.RIGHT);
		currentTimePanel.add (currentLSTLabel);

		currentLSTTextField = new JTextField("");
        currentLSTTextField.setMargin(coordMargin);
        currentLSTTextField.setFont(p12);
        currentLSTTextField.setToolTipText("Current local sidereal time");
        currentLSTTextField.setEditable(false);
        currentLSTTextField.setBackground(leapGray);
		currentLSTTextField.setPreferredSize(new Dimension((int)(coordSize.width*0.63),coordSize.height));
		currentLSTTextField.setHorizontalAlignment(JTextField.LEFT);
        currentLSTTextField.addActionListener(this);
        currentTimePanel.add (currentLSTTextField);

        SpringUtil.makeCompactGrid (currentTimePanel,1,currentTimePanel.getComponentCount(), 2,2,2,2);
        mainPanel.add(currentTimePanel);

        JPanel objectObservatoryIDPanel = new JPanel(new SpringLayout());

        JPanel objectIDPanel = new JPanel(new SpringLayout());
        objectIDPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(mainBorderColor, 1), "SIMBAD Object ID (or SS Object)", TitledBorder.LEFT, TitledBorder.TOP, b12, Color.DARK_GRAY));

		objectIDTextField = new JTextField(objectIDText);
        objectIDTextField.setMargin(coordMargin);
        objectIDTextField.setFont(p12);
		objectIDTextField.setPreferredSize(new Dimension((int)(coordSize.width*2.0),coordSize.height));
		objectIDTextField.setHorizontalAlignment(JTextField.LEFT);
        objectIDTextField.setToolTipText("Enter SIMBAD object ID, planet name, moon, or sun and press 'Enter' to populate proper motion and coordinates");
        objectIDTextField.addActionListener(this);
        objectIDPanel.add (objectIDTextField);
        SpringUtil.makeCompactGrid (objectIDPanel,1,objectIDPanel.getComponentCount(), 2,2,2,2);
        objectObservatoryIDPanel.add(objectIDPanel);

        JPanel timeZonePanel = new JPanel (new SpringLayout());
        timeZonePanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(mainBorderColor, 1), "Time Zone", TitledBorder.LEFT, TitledBorder.TOP, b12, Color.DARK_GRAY));
        JLabel timeZoneLabel = new JLabel ("UTC offset:");
        timeZoneLabel.setHorizontalAlignment(JLabel.RIGHT);
        timeZoneLabel.setFont(p12);
        timeZoneLabel.setToolTipText("Set time zone offset from UTC");
        timeZonePanel.add (timeZoneLabel);
        SpinnerModel timeZoneSpinnerModel = new SpinnerNumberModel(new Double(nowTimeZoneOffset), new Double(-12), new Double(12), new Double(1.0));

        timeZoneOffsetSpinner = new JSpinner(timeZoneSpinnerModel);
        timeZoneOffsetSpinner.setEnabled(!autoTimeZone);
        timeZoneOffsetSpinner.setBackground(autoTimeZone?leapGray:Color.WHITE);
        timeZoneOffsetSpinner.setPreferredSize(new Dimension((int)(coordSize.width*0.45),coordSize.height));
        timeZoneOffsetSpinner.setToolTipText("Set time zone offset from UTC");
        timeZoneOffsetSpinner.addChangeListener (new ChangeListener()
            {
            public void stateChanged(ChangeEvent ev)
                {
                nowTimeZoneOffset = ((Double)timeZoneOffsetSpinner.getValue()).doubleValue();
                processAction(null);
                }
            });
        timeZoneOffsetSpinner.addMouseWheelListener( new MouseWheelListener()
            {
            public void mouseWheelMoved( MouseWheelEvent e )
                {
                if (!autoTimeZone)
                    {
                    double newValue = ((Double)timeZoneOffsetSpinner.getValue()).doubleValue() - e.getWheelRotation();
                    if (newValue < -12.0)
                        timeZoneOffsetSpinner.setValue(-12.0);
                    else if (newValue > 12.0)
                        timeZoneOffsetSpinner.setValue(12.0);
                    else
                        timeZoneOffsetSpinner.setValue(newValue);
                    }
                }
            });
        timeZonePanel.add (timeZoneOffsetSpinner);
        SpringUtil.makeCompactGrid (timeZonePanel, 1, timeZonePanel.getComponentCount(), 2,2,2,2);
        objectObservatoryIDPanel.add (timeZonePanel);


        JPanel observatoryIDPanel = new JPanel(new SpringLayout());
        observatoryIDPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(mainBorderColor, 1), "Observatory ID", TitledBorder.LEFT, TitledBorder.TOP, b12, Color.DARK_GRAY));

		observatoryIDComboBox = new JComboBox(observatoryIDs);
		observatoryIDComboBox.setPreferredSize(new Dimension((int)(coordSize.width*3.0),coordSize.height+2));
        observatoryIDComboBox.setFont(p12);
//        observatoryIDComboBox.setEditable(true);
        observatoryIDComboBox.setSelectedIndex(selectedObservatory<observatoryIDs.length?selectedObservatory:0);
        observatoryIDComboBox.setMaximumRowCount(20);
//		observatoryIDComboBox.setHorizontalAlignment(JTextField.LEFT);
        observatoryIDComboBox.setToolTipText("<html>"+"Select observatory to populate latitude, longitude, and altitude fields"+"<br>"+
            "--Or--"+"<br>"+
            "select 'Custom Lon, Lat, Alt entry' to enter the values directly"+"</html>");
        observatoryIDComboBox.addActionListener(this);
        observatoryIDPanel.add (observatoryIDComboBox);
        if (selectedObservatory>0)
            {
            if (selectedObservatory < observatoryLats.length) lat = observatoryLats[selectedObservatory];
            if (selectedObservatory < observatoryLons.length) lon = observatoryLons[selectedObservatory];
            if (selectedObservatory < observatoryAlts.length) alt = observatoryAlts[selectedObservatory];
            }
        SpringUtil.makeCompactGrid (observatoryIDPanel,1,observatoryIDPanel.getComponentCount(), 2,2,2,2);
        objectObservatoryIDPanel.add(observatoryIDPanel);

        SpringUtil.makeCompactGrid (objectObservatoryIDPanel,1,objectObservatoryIDPanel.getComponentCount(), 2,2,2,2);
        mainPanel.add(objectObservatoryIDPanel);

        JPanel pmlocalPanel = new JPanel(new SpringLayout());

        JPanel pmPanel = new JPanel(new SpringLayout());
        pmPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(mainBorderColor, 1), "Target Proper Motion (mas/yr)", TitledBorder.LEFT, TitledBorder.TOP, b12, Color.DARK_GRAY));

        JLabel pmRALabel = new JLabel (" pmRA:");
        pmRALabel.setFont(p12);
        pmRALabel.setHorizontalAlignment (JTextField.RIGHT);
		pmPanel.add (pmRALabel);

		pmRATextField = new JTextField(uptoFourPlaces.format(pmRA));
        pmRATextField.setMargin(coordMargin);
        pmRATextField.setFont(p12);
		pmRATextField.setPreferredSize(new Dimension((int)(coordSize.width*0.6),coordSize.height));
		pmRATextField.setHorizontalAlignment(JTextField.LEFT);
        pmRATextField.setToolTipText("Enter target RA proper motion in mas/yr (SIMBAD format) and press 'Enter'");
        pmRATextField.addActionListener(this);
        pmPanel.add (pmRATextField);

        JLabel pnDecLabel = new JLabel (" pmDec:");
        pnDecLabel.setFont(p12);
        pnDecLabel.setHorizontalAlignment (JTextField.RIGHT);
		pmPanel.add (pnDecLabel);

		pmDecTextField = new JTextField(uptoFourPlaces.format(pmDec));
        pmDecTextField.setMargin(coordMargin);
        pmDecTextField.setFont(p12);
		pmDecTextField.setPreferredSize(new Dimension((int)(coordSize.width*0.6),coordSize.height));
		pmDecTextField.setHorizontalAlignment(JTextField.LEFT);
        pmDecTextField.setToolTipText("Enter target DEC proper motion in mas/yr (SIMBAD format) and press 'Enter'");
        pmDecTextField.addActionListener(this);
        pmPanel.add (pmDecTextField);

        SpringUtil.makeCompactGrid (pmPanel,1,pmPanel.getComponentCount(), 2,2,2,2);
        pmlocalPanel.add(pmPanel);



        JPanel localPanel = new JPanel(new SpringLayout());
        localPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(mainBorderColor, 1), "Geographic Location of Observatory", TitledBorder.LEFT, TitledBorder.TOP, b12, Color.DARK_GRAY));


        JLabel longitudeLabel = new JLabel (" Lon:");
        longitudeLabel.setFont(p12);
        longitudeLabel.setHorizontalAlignment (JTextField.RIGHT);
		localPanel.add (longitudeLabel);

		lonTextField = new JTextField(showSexagesimal ? decToSex(lon, 2, 180, true): sixPlaces.format(lon));
        lonTextField.setMargin(coordMargin);
        lonTextField.setFont(p12);
		lonTextField.setPreferredSize(coordSize);
		lonTextField.setHorizontalAlignment(JTextField.LEFT);
        lonTextField.setToolTipText("<html>Enter observatory geographic longitude in degrees (east positive)<br>"+
                                    "in either decimal or sexagesimal format and press 'Enter'<br>"+
                                    "***Observatory ID must be set to 'Custom Lon, Lat, Alt entry' to enter the value directly***</html>");
        lonTextField.addActionListener(this);
        localPanel.add (lonTextField);

        JLabel latitudeLabel = new JLabel (" Lat:");
        latitudeLabel.setFont(p12);
        latitudeLabel.setHorizontalAlignment (JTextField.RIGHT);
		localPanel.add (latitudeLabel);

		latTextField = new JTextField(showSexagesimal ? decToSex(lat, 2, 90, true): sixPlaces.format(lat));
        latTextField.setMargin(coordMargin);
        latTextField.setFont(p12);
		latTextField.setPreferredSize(coordSize);
		latTextField.setHorizontalAlignment(JTextField.LEFT);
        latTextField.setToolTipText("<html>Enter observatory geographic latitude in degrees (north positive)<br>"+
                                    "in either decimal or sexagesimal format and press 'Enter'<br>"+
                                    "***Observatory ID must be set to 'Custom Lon, Lat, Alt entry' to enter the value directly***</html>");
        latTextField.addActionListener(this);
        localPanel.add (latTextField);

        JLabel altitudeLabel = new JLabel (" Alt:");
        altitudeLabel.setFont(p12);
        altitudeLabel.setHorizontalAlignment (JTextField.RIGHT);
		localPanel.add (altitudeLabel);

		altTextField = new JTextField(uptoTwoPlaces.format(alt));
        altTextField.setMargin(coordMargin);
        altTextField.setFont(p12);
		altTextField.setPreferredSize(new Dimension((int)(coordSize.width*0.6),coordSize.height));
		altTextField.setHorizontalAlignment(JTextField.LEFT);
        altTextField.setToolTipText("<html>Enter observatory altitude in meters above mean sea level and press 'Enter'<br>"+
                                    "***Observatory ID must be set to 'Custom Lon, Lat, Alt entry' to enter the value directly***</html>");
        altTextField.addActionListener(this);
        localPanel.add (altTextField);

        SpringUtil.makeCompactGrid (localPanel,1,localPanel.getComponentCount(), 2,2,2,2);
        pmlocalPanel.add(localPanel);


        SpringUtil.makeCompactGrid (pmlocalPanel,1,pmlocalPanel.getComponentCount(), 2,2,2,2);
        mainPanel.add(pmlocalPanel);


        JPanel J2000CoordPanel = new JPanel(new SpringLayout());
        J2000CoordPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(mainBorderColor, 1), "Standard Coordinates", TitledBorder.LEFT, TitledBorder.TOP, b12, Color.DARK_GRAY));

        Insets linkButtonMargin = new Insets(2,2,2,2);
        if (isLinux) linkButtonMargin = new Insets(0,0,0,0);

        JPanel simbadPanel = new JPanel(new SpringLayout());
        simbadPanel.setBorder(BorderFactory.createEmptyBorder(7, 0, 0, 0));  //top,left,bottom,right

        simbadButton = new JButton(simbadIcon);
        simbadButton.setMargin(linkButtonMargin);
        simbadButton.setToolTipText("Click to show the object/coordinates in Simbad");
        simbadButton.addActionListener(this);
        simbadPanel.add(simbadButton);

        SpringUtil.makeCompactGrid (simbadPanel,1,simbadPanel.getComponentCount(), 0,0,0,0);
        J2000CoordPanel.add(simbadPanel);


        JPanel J2000EquPanel = new JPanel(new SpringLayout());
        J2000EquPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY, 1), "J2000 Equatorial", TitledBorder.CENTER, TitledBorder.TOP, p12, Color.DARK_GRAY));

        JLabel J2000RaLabel = new JLabel ("   RA:");
        J2000RaLabel.setFont(p12);
        J2000RaLabel.setHorizontalAlignment (JTextField.RIGHT);
		J2000EquPanel.add (J2000RaLabel);

		raJ2000TextField = new JTextField(raJ2000Text);
        raJ2000TextField.setBorder(newradecJ2000?greenBorder:grayBorder);
        raJ2000TextField.setFont(p12);
        raJ2000TextField.setMargin(coordMargin);
		raJ2000TextField.setPreferredSize(coordSize);
		raJ2000TextField.setHorizontalAlignment(JTextField.LEFT);
        raJ2000TextField.setToolTipText("<html>"+"Enter target J2000 right ascension in hours in either decimal or sexagesimal format and press 'Enter'"+"<br>"+
            "--Or--"+"<br>"+
            "Enter J2000 RA (hours) and DEC (degrees) and press 'Enter' to populate both fields"+"</html>");
        raJ2000TextField.addActionListener(this);
        J2000EquPanel.add (raJ2000TextField);

        JLabel J2000DecLabel = new JLabel ("  Dec:");
        J2000DecLabel.setFont(p12);
        J2000DecLabel.setHorizontalAlignment (JTextField.RIGHT);
		J2000EquPanel.add (J2000DecLabel);

		decJ2000TextField = new JTextField (decJ2000Text);
        decJ2000TextField.setMargin(coordMargin);
        decJ2000TextField.setFont(p12);
		decJ2000TextField.setPreferredSize(coordSize);
        decJ2000TextField.setBorder(newradecJ2000?greenBorder:grayBorder);
		decJ2000TextField.setHorizontalAlignment(JTextField.LEFT);
        decJ2000TextField.setToolTipText("<html>"+"Enter target J2000 declination in degrees in either decimal or sexagesimal format and press 'Enter'"+"<br>"+
            "--Or--"+"<br>"+
            "Enter J2000 RA (hours) and DEC (degrees) and press 'Enter' to populate both fields"+"</html>");
        decJ2000TextField.addActionListener(this);
        J2000EquPanel.add (decJ2000TextField);

        SpringUtil.makeCompactGrid (J2000EquPanel,1,J2000EquPanel.getComponentCount(), 2,0,2,2);
        J2000CoordPanel.add(J2000EquPanel);



        JPanel J2000EclPanel = new JPanel(new SpringLayout());
        J2000EclPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY, 1), "J2000 Ecliptic", TitledBorder.CENTER, TitledBorder.TOP, p12, Color.DARK_GRAY));

        JLabel J2000eclLonLabel = new JLabel ("  Lon:");
        J2000eclLonLabel.setFont(p12);
        J2000eclLonLabel.setHorizontalAlignment (JTextField.RIGHT);
		J2000EclPanel.add (J2000eclLonLabel);

		eclLonJ2000TextField = new JTextField (eclLonJ2000Text);
        eclLonJ2000TextField.setMargin(coordMargin);
        eclLonJ2000TextField.setFont(p12);
		eclLonJ2000TextField.setPreferredSize(coordSize);
        eclLonJ2000TextField.setBorder(newelonlatJ2000?greenBorder:grayBorder);
		eclLonJ2000TextField.setHorizontalAlignment(JTextField.LEFT);
        eclLonJ2000TextField.setToolTipText("<html>"+"Enter target J2000 ecliptical longitude in degrees in either decimal or sexagesimal format and press 'Enter'"+"<br>"+
            "--Or--"+"<br>"+
            "Enter ecliptical Lon and Lat in degrees and press 'Enter' to populate both fields"+"</html>");
        eclLonJ2000TextField.addActionListener(this);
        J2000EclPanel.add (eclLonJ2000TextField);


        JLabel J2000eclLatLabel = new JLabel ("  Lat:");
        J2000eclLatLabel.setFont(p12);
        J2000eclLatLabel.setHorizontalAlignment (JTextField.RIGHT);
		J2000EclPanel.add (J2000eclLatLabel);

		eclLatJ2000TextField = new JTextField(eclLatJ2000Text);
        eclLatJ2000TextField.setMargin(coordMargin);
        eclLatJ2000TextField.setFont(p12);
		eclLatJ2000TextField.setPreferredSize(coordSize);
        eclLatJ2000TextField.setBorder(newelonlatJ2000?greenBorder:grayBorder);
		eclLatJ2000TextField.setHorizontalAlignment(JTextField.LEFT);
        eclLatJ2000TextField.setToolTipText("<html>"+"Enter target J2000 ecliptical latitude in degrees in either decimal or sexagesimal format and press 'Enter'"+"<br>"+
            "--Or--"+"<br>"+
            "Enter ecliptical Lon and Lat in degrees and press 'Enter' to populate both fields"+"</html>");
        eclLatJ2000TextField.addActionListener(this);
        J2000EclPanel.add (eclLatJ2000TextField);

        SpringUtil.makeCompactGrid (J2000EclPanel,1,J2000EclPanel.getComponentCount(), 2,0,2,2);
        J2000CoordPanel.add(J2000EclPanel);

        JPanel skyMapPanel = new JPanel(new SpringLayout());
        skyMapPanel.setBorder(BorderFactory.createEmptyBorder(7, 0, 0, 0));  //top,left,bottom,right

        skyMapButton = new JButton(skymapIcon);
        skyMapButton.setMargin(linkButtonMargin);
        skyMapButton.setToolTipText("Click to show the coordinates in Sky-Map");
        skyMapButton.addActionListener(this);
        skyMapPanel.add(skyMapButton);

        SpringUtil.makeCompactGrid (skyMapPanel,1,skyMapPanel.getComponentCount(), 0,0,0,0);
        J2000CoordPanel.add(skyMapPanel);

        JPanel B1950EquPanel = new JPanel(new SpringLayout());
        B1950EquPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY, 1), "B1950 Equatorial", TitledBorder.CENTER, TitledBorder.TOP, p12, Color.DARK_GRAY));

        JLabel B1950raLabel = new JLabel ("   RA:");
        B1950raLabel.setFont(p12);
        B1950raLabel.setHorizontalAlignment (JTextField.RIGHT);
		B1950EquPanel.add (B1950raLabel);

		raB1950TextField = new JTextField(raB1950Text);
        raB1950TextField.setMargin(coordMargin);
        raB1950TextField.setFont(p12);
		raB1950TextField.setPreferredSize(coordSize);
        raB1950TextField.setBorder(newradecB1950?greenBorder:grayBorder);
		raB1950TextField.setHorizontalAlignment(JTextField.LEFT);
        raB1950TextField.setToolTipText("<html>"+"Enter target B1950 right ascension in hours in either decimal or sexagesimal format and press 'Enter'"+"<br>"+
            "--Or--"+"<br>"+
            "Enter RA (hours) and DEC (degrees) and press 'Enter' to populate both fields"+"</html>");
        raB1950TextField.addActionListener(this);
        B1950EquPanel.add (raB1950TextField);

        JLabel B1950decLabel = new JLabel ("  Dec:");
        B1950decLabel.setFont(p12);
        B1950decLabel.setHorizontalAlignment (JTextField.RIGHT);
		B1950EquPanel.add (B1950decLabel);

		decB1950TextField = new JTextField (decB1950Text);
        decB1950TextField.setMargin(coordMargin);
        decB1950TextField.setFont(p12);
		decB1950TextField.setPreferredSize(coordSize);
        decB1950TextField.setBorder(newradecB1950?greenBorder:grayBorder);
		decB1950TextField.setHorizontalAlignment(JTextField.LEFT);
        decB1950TextField.setToolTipText("<html>"+"Enter target B1950 declination in degrees in either decimal or sexagesimal format and press 'Enter'"+"<br>"+
            "--Or--"+"<br>"+
            "Enter B1950 RA (hours) and DEC (degrees) and press 'Enter' to populate both fields"+"</html>");
        decB1950TextField.addActionListener(this);
        B1950EquPanel.add (decB1950TextField);

        SpringUtil.makeCompactGrid (B1950EquPanel,1,B1950EquPanel.getComponentCount(), 2,0,2,2);
        J2000CoordPanel.add(B1950EquPanel);


        JPanel B1950GalPanel = new JPanel(new SpringLayout());
        B1950GalPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY, 1), "Galactic", TitledBorder.CENTER, TitledBorder.TOP, p12, Color.DARK_GRAY));

        JLabel B1950GalLonLabel = new JLabel ("  Lon:");
        B1950GalLonLabel.setFont(p12);
        B1950GalLonLabel.setHorizontalAlignment (JTextField.RIGHT);
		B1950GalPanel.add (B1950GalLonLabel);

		galLonB1950TextField = new JTextField(galLonB1950Text);
        galLonB1950TextField.setMargin(coordMargin);
        galLonB1950TextField.setFont(p12);
		galLonB1950TextField.setPreferredSize(coordSize);
        galLonB1950TextField.setBorder(newglonlatB1950?greenBorder:grayBorder);
		galLonB1950TextField.setHorizontalAlignment(JTextField.LEFT);
        galLonB1950TextField.setToolTipText("<html>"+"Enter target galactic longitude in degrees in either decimal or sexagesimal format and press 'Enter'"+"<br>"+
            "--Or--"+"<br>"+
            "Enter galactic Lon and Lat in degrees and press 'Enter' to populate both fields"+"</html>");
        galLonB1950TextField.addActionListener(this);
        B1950GalPanel.add (galLonB1950TextField);

        JLabel J2000galLatLabel = new JLabel ("  Lat:");
        J2000galLatLabel.setFont(p12);
        J2000galLatLabel.setHorizontalAlignment (JTextField.RIGHT);
		B1950GalPanel.add (J2000galLatLabel);

		galLatB1950TextField = new JTextField (galLatB1950Text);
        galLatB1950TextField.setMargin(coordMargin);
        galLatB1950TextField.setFont(p12);
		galLatB1950TextField.setPreferredSize(coordSize);
        galLatB1950TextField.setBorder(newglonlatB1950?greenBorder:grayBorder);
		galLatB1950TextField.setHorizontalAlignment(JTextField.LEFT);
        galLatB1950TextField.setToolTipText("<html>"+"Enter target galactic latitude in degrees in either decimal or sexagesimal format and press 'Enter'"+"<br>"+
            "--Or--"+"<br>"+
            "Enter galactic Lon and Lat in degrees and press 'Enter' to populate both fields"+"</html>");
        galLatB1950TextField.addActionListener(this);
        B1950GalPanel.add (galLatB1950TextField);

        SpringUtil.makeCompactGrid (B1950GalPanel,1,B1950GalPanel.getComponentCount(), 2,0,2,2);
        J2000CoordPanel.add(B1950GalPanel);

        SpringUtil.makeCompactGrid (J2000CoordPanel,2,J2000CoordPanel.getComponentCount()/2, 2,0,2,2);
        mainPanel.add(J2000CoordPanel);




        JPanel eoiPanel = new JPanel(new SpringLayout());
        eoiPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(mainBorderColor, 1), "Epoch of Interest", TitledBorder.LEFT, TitledBorder.TOP, b12, Color.DARK_GRAY));

        JPanel eoiDateTimePanel = new JPanel(new SpringLayout());
        eoiDateTimePanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY, 1), "UTC-based Time", TitledBorder.LEFT, TitledBorder.TOP, p12, Color.DARK_GRAY));

        //JDEOI STEPSIZE POPUP
        JPanel utEOIsteppanel = new JPanel();
        jdEOIsteppopup = new JPopupMenu();
        jdEOIstepmodel = new SpinnerListModel(spinnerscalelist);
        jdEOIstepspinner = new JSpinner(jdEOIstepmodel);
        jdEOIstepspinner.setValue(convertToText(jdEOIStep, jdEOIStepFactor));
        jdEOIstepspinner.addChangeListener (new ChangeListener()
            {
            public void stateChanged(ChangeEvent ev)
                {
                jdEOIStepFactor = 1;
                String spinnerValue = ((String)jdEOIstepspinner.getValue()).trim();
                if      (spinnerValue.equals("1 second"))   jdEOIStep = GregorianCalendar.SECOND;
                else if (spinnerValue.equals("1 minute"))   jdEOIStep = GregorianCalendar.MINUTE;
                else if (spinnerValue.equals("10 minutes")) {
                                                            jdEOIStep = GregorianCalendar.MINUTE;
                                                            jdEOIStepFactor = 10;
                                                            }
                else if (spinnerValue.equals("1 hour"))     jdEOIStep = GregorianCalendar.HOUR_OF_DAY;
                else if (spinnerValue.equals("1 day"))      jdEOIStep = GregorianCalendar.DAY_OF_MONTH;
                else if (spinnerValue.equals("1 week"))     {
                                                            jdEOIStep = GregorianCalendar.DAY_OF_MONTH;
                                                            jdEOIStepFactor = 7;
                                                            }
                else if (spinnerValue.equals("1 month"))    jdEOIStep = GregorianCalendar.MONTH;
                else if (spinnerValue.equals("1 year"))     jdEOIStep = GregorianCalendar.YEAR;
                else if (spinnerValue.equals("10 years"))   {
                                                            jdEOIStep = GregorianCalendar.YEAR;
                                                            jdEOIStepFactor = 10;
                                                            }
                else if (spinnerValue.equals("100 years"))  {
                                                            jdEOIStep = GregorianCalendar.YEAR;
                                                            jdEOIStepFactor = 100;
                                                            }
                else if (spinnerValue.equals("1000 years")) {
                                                            jdEOIStep = GregorianCalendar.YEAR;
                                                            jdEOIStepFactor = 1000;
                                                            }
                else                                        jdEOIStep = GregorianCalendar.DAY_OF_MONTH;
                Prefs.set("coords.jdEOIStep",jdEOIStep);
                Prefs.set("coords.jdEOIStepFactor",jdEOIStepFactor);
                }
            });

        JLabel utEOIsteplabel = new JLabel ("Stepsize:");
        utEOIsteppanel.add(utEOIsteplabel);
        utEOIsteppanel.add(jdEOIstepspinner);
        jdEOIsteppopup.add(utEOIsteppanel);
//        jdEOIsteppopup.setLightWeightPopupEnabled(false);
        jdEOIsteppopup.setLocation(500, 500);
        jdEOIsteppopup.addPopupMenuListener (new PopupMenuListener()
            {
            public void popupMenuCanceled(PopupMenuEvent popupMenuEvent) {
                    jdEOIsteppopup.setVisible(false);
                    }

            public void popupMenuWillBecomeInvisible(PopupMenuEvent popupMenuEvent) {
                    jdEOIsteppopup.setVisible(true);
                    }

            public void popupMenuWillBecomeVisible(PopupMenuEvent popupMenuEvent) {
                    jdEOIsteppopup.setVisible(false);
                    }
            });


        JPanel nowPanel = new JPanel(new SpringLayout());
        nowPanel.setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY, 1));

        nowEOIButton = new JButton("Now");
        nowEOIButton.setToolTipText("Set the epoch of interest to the current UTC date and time");
        nowEOIButton.setFont(p12);
        nowEOIButton.setEnabled(!useNowEpoch && timeEnabled);
        nowEOIButton.addActionListener(this);
        nowPanel.add(nowEOIButton);

        useNowEpochCB = new JCheckBox("Lock", useNowEpoch);
        if (dp)
            useNowEpochCB.setToolTipText("Disabled when Coordinate Converter is started by Data Processor");
        else
            useNowEpochCB.setToolTipText("Enable to continuously lock the epoch of interest to the current UTC date and time");
        useNowEpochCB.addItemListener(this);
        useNowEpochCB.setEnabled(!dp);
        nowPanel.add (useNowEpochCB);

//        JLabel eoiNowLabel = new JLabel ("Lock ");
//        eoiNowLabel.setFont(p12);
//        eoiNowLabel.setToolTipText("Enable to continuously lock the epoch of interest to the current UTC date and time");
//        eoiNowLabel.setHorizontalAlignment (JTextField.LEFT);
//		nowPanel.add (eoiNowLabel);

        SpringUtil.makeCompactGrid (nowPanel,nowPanel.getComponentCount(),1, 0,0,0,0);
        eoiDateTimePanel.add(nowPanel);


        JPanel eoiClockPanel = new JPanel(new SpringLayout());
        eoiClockPanel.setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY, 1));
        JLabel eoiUTDateLabel = new JLabel (" UTC:");
        eoiUTDateLabel.setFont(p12);
        eoiUTDateLabel.setToolTipText("Enter the UTC date and time of interest in YYYY MM DD and HH:MM:SS (or 0.D) format and press Enter");
        eoiUTDateLabel.setHorizontalAlignment (JTextField.RIGHT);
		eoiClockPanel.add (eoiUTDateLabel);

		eoiUTDateTextField = new JTextField("");
        eoiUTDateTextField.setMargin(coordMargin);
        eoiUTDateTextField.setFont(p12);
        eoiUTDateTextField.setToolTipText("<html>Enter the UTC date of interest in YYYY MM DD format and press Enter<br>"+
                                          "or spin scroll wheel over YYYY, or MM, or DD to change the value</html>");
        eoiUTDateTextField.setEditable(!useNowEpoch && timeEnabled);
		eoiUTDateTextField.setPreferredSize(new Dimension((int)(coordSize.width*0.9),coordSize.height));
		eoiUTDateTextField.setHorizontalAlignment(JTextField.LEFT);
        eoiUTDateTextField.addActionListener(this);
        eoiUTDateTextField.addMouseWheelListener( new MouseWheelListener()
            {
            public void mouseWheelMoved( MouseWheelEvent e )
                {
                if (!useNowEpoch  && timeEnabled && !running)
                    {
                    if (scrollfinishedtimer != null) scrollfinishedtimer.cancel();
                    if (scrollfinishedtask != null) scrollfinishedtask.cancel();
                    int increment = 0;
                    int location = e.getX();
//                    IJ.log(""+location);
                    if (location < (isWin?40:42)) increment = GregorianCalendar.YEAR;
                    else if (location < (isWin?57:62)) increment = GregorianCalendar.MONTH;
                    else increment = GregorianCalendar.DAY_OF_MONTH;
                    updateJDEOI(increment, -e.getWheelRotation(), true);
                    startScrollFinishedTimer();
                    }
                }
            });
//        eoiUTDateTextField.setComponentPopupMenu(jdEOIsteppopup);
        eoiClockPanel.add (eoiUTDateTextField);

        JPanel utEOIPanel = new JPanel (new SpringLayout());

		eoiUTTimeTextField = new JTextField("");
        eoiUTTimeTextField.setMargin(coordMargin);
        eoiUTTimeTextField.setFont(p12);
        eoiUTTimeTextField.setToolTipText("<html>Enter the UTC time of interest in HH:MM:SS (or 0.D) format and press Enter<br>"+
                                          "or spin scroll wheel over HH, or MM, or SS to change the value</html>");
        eoiUTTimeTextField.setEditable(!useNowEpoch && timeEnabled);
		eoiUTTimeTextField.setPreferredSize(new Dimension((int)(coordSize.width*0.9),coordSize.height));
		eoiUTTimeTextField.setHorizontalAlignment(JTextField.LEFT);
        eoiUTTimeTextField.addActionListener(this);
        eoiUTTimeTextField.addMouseWheelListener( new MouseWheelListener()
            {
            public void mouseWheelMoved( MouseWheelEvent e )
                {
                if (!useNowEpoch && timeEnabled && !running)
                    {
                    if (scrollfinishedtimer != null) scrollfinishedtimer.cancel();
                    if (scrollfinishedtask != null) scrollfinishedtask.cancel();
                    int increment = 0;
                    int location = e.getX();
//                    IJ.log(""+location);
                    if (location < (isWin?22:24)) increment = GregorianCalendar.HOUR_OF_DAY;
                    else if (location < (isWin?37:40)) increment = GregorianCalendar.MINUTE;
                    else increment = GregorianCalendar.SECOND;
                    updateJDEOI(increment, -e.getWheelRotation(), true);
                    startScrollFinishedTimer();
                    }
                }
            });
//        eoiUTTimeTextField.setComponentPopupMenu(jdEOIsteppopup);
        utEOIPanel.add (eoiUTTimeTextField);

        JPanel spinnerPanel = new JPanel (new SpringLayout());
//        spinnerPanel.setBorder(BorderFactory.createEmptyBorder(1,1,1,1));//,BorderFactory.createLineBorder(Color.LIGHT_GRAY, 1)));
        jdEOIupButton = new JButton(jdEOIupIcon);
        jdEOIupButton.setMargin(buttonMargin);
        jdEOIupButton.setEnabled(!useNowEpoch && timeEnabled);
        jdEOIupButton.setToolTipText("<html>Left click to increment time by one step<br>"+
                                     "Right click to set step value</html>");
//        jdEOIupButton.addChangeListener(this);
        jdEOIupButton.addMouseListener(this);
        jdEOIupButton.setPreferredSize(new Dimension(15,10));
        jdEOIupButton.setComponentPopupMenu(jdEOIsteppopup);
        spinnerPanel.add(jdEOIupButton);
        jdEOIdownButton = new JButton(jdEOIdownIcon);
        jdEOIdownButton.setMargin(buttonMargin);
        jdEOIdownButton.setEnabled(!useNowEpoch && timeEnabled);
        jdEOIdownButton.setToolTipText("<html>Left click to decrement time by one step<br>"+
                                       "Right click to set step value</html>");
//        jdEOIdownButton.addChangeListener(this);
        jdEOIdownButton.addMouseListener(this);
        jdEOIdownButton.setPreferredSize(new Dimension(15,10));
        jdEOIdownButton.setComponentPopupMenu(jdEOIsteppopup);
        spinnerPanel.add(jdEOIdownButton);
        SpringUtil.makeCompactGrid (spinnerPanel, 2, 1, 0,0,0,0);
        utEOIPanel.add (spinnerPanel);

        SpringUtil.makeCompactGrid (utEOIPanel, 1, utEOIPanel.getComponentCount(), 0,0,0,0);
        eoiClockPanel.add (utEOIPanel);

        JLabel eoiLocDateLabel = new JLabel (" Local:");
        eoiLocDateLabel.setFont(p12);
        eoiLocDateLabel.setToolTipText("Enter the local date and time of interest in YYYY MM DD and HH:MM:SS (or 0.D) format and press Enter");
        eoiLocDateLabel.setHorizontalAlignment (JTextField.RIGHT);
		eoiClockPanel.add (eoiLocDateLabel);

		eoiLocDateTextField = new JTextField("");
        eoiLocDateTextField.setMargin(coordMargin);
        eoiLocDateTextField.setFont(p12);
        eoiLocDateTextField.setToolTipText("<html>Enter the local date of interest in YYYY MM DD format and press Enter<br>"+
                                          "or spin scroll wheel over YYYY, or MM, or DD to change the value</html>");
        eoiLocDateTextField.setEditable(!useNowEpoch && timeEnabled);
		eoiLocDateTextField.setPreferredSize(new Dimension((int)(coordSize.width*0.9),coordSize.height));
		eoiLocDateTextField.setHorizontalAlignment(JTextField.LEFT);
        eoiLocDateTextField.addActionListener(this);
        eoiLocDateTextField.addMouseWheelListener( new MouseWheelListener()
            {
            public void mouseWheelMoved( MouseWheelEvent e )
                {
                if (!useNowEpoch && timeEnabled && !running)
                    {
                    if (scrollfinishedtimer != null) scrollfinishedtimer.cancel();
                    if (scrollfinishedtask != null) scrollfinishedtask.cancel();
                    int increment = 0;
                    int location = e.getX();
//                    IJ.log(""+location);
                    if (location < (isWin?40:42)) increment = GregorianCalendar.YEAR;
                    else if (location < (isWin?57:62)) increment = GregorianCalendar.MONTH;
                    else increment = GregorianCalendar.DAY_OF_MONTH;
                    updateJDEOI(increment, -e.getWheelRotation(), true);
                    startScrollFinishedTimer();
                    }
                }
            });
//        eoiLocDateTextField.setComponentPopupMenu(jdEOIsteppopup);
        eoiClockPanel.add (eoiLocDateTextField);

        JPanel locEOIPanel = new JPanel (new SpringLayout());
		eoiLocTimeTextField = new JTextField("");
        eoiLocTimeTextField.setMargin(coordMargin);
        eoiLocTimeTextField.setFont(p12);
        eoiLocTimeTextField.setToolTipText("<html>Enter the local time of interest in HH:MM:SS (or 0.D) format and press Enter<br>"+
                                          "or spin scroll wheel over HH, or MM, or SS to change the value</html>");
        eoiLocTimeTextField.setEditable(!useNowEpoch && timeEnabled);
		eoiLocTimeTextField.setPreferredSize(new Dimension((int)(coordSize.width*0.9),coordSize.height));
		eoiLocTimeTextField.setHorizontalAlignment(JTextField.LEFT);
        eoiLocTimeTextField.addActionListener(this);
        eoiLocTimeTextField.addMouseWheelListener( new MouseWheelListener()
            {
            public void mouseWheelMoved( MouseWheelEvent e )
                {
                if (!useNowEpoch && timeEnabled && !running)
                    {
                    if (scrollfinishedtimer != null) scrollfinishedtimer.cancel();
                    if (scrollfinishedtask != null) scrollfinishedtask.cancel();
                    int increment = 0;
                    int location = e.getX();
//                    IJ.log(""+location);
                    if (location < (isWin?22:24)) increment = GregorianCalendar.HOUR_OF_DAY;
                    else if (location < (isWin?37:40)) increment = GregorianCalendar.MINUTE;
                    else increment = GregorianCalendar.SECOND;
                    updateJDEOI(increment, -e.getWheelRotation(), true);
                    startScrollFinishedTimer();
                    }
                }
            });
//        eoiLocTimeTextField.setComponentPopupMenu(jdEOIsteppopup);
        locEOIPanel.add (eoiLocTimeTextField);

        JPanel locSpinnerPanel = new JPanel (new SpringLayout());
//        spinnerPanel.setBorder(BorderFactory.createEmptyBorder(1,1,1,1));//,BorderFactory.createLineBorder(Color.LIGHT_GRAY, 1)));
        jdLocEOIupButton = new JButton(jdEOIupIcon);
        jdLocEOIupButton.setMargin(buttonMargin);
        jdLocEOIupButton.setEnabled(!useNowEpoch && timeEnabled);
        jdLocEOIupButton.setToolTipText("<html>Left click to increment time by one step<br>"+
                                     "Right click to set step value</html>");
//        jdEOIupButton.addChangeListener(this);
        jdLocEOIupButton.addMouseListener(this);
        jdLocEOIupButton.setPreferredSize(new Dimension(15,10));
        jdLocEOIupButton.setComponentPopupMenu(jdEOIsteppopup);
        locSpinnerPanel.add(jdLocEOIupButton);
        jdLocEOIdownButton = new JButton(jdEOIdownIcon);
        jdLocEOIdownButton.setMargin(buttonMargin);
        jdLocEOIdownButton.setEnabled(!useNowEpoch && timeEnabled);
        jdLocEOIdownButton.setToolTipText("<html>Left click to decrement time by one step<br>"+
                                       "Right click to set step value</html>");
//        jdEOIdownButton.addChangeListener(this);
        jdLocEOIdownButton.addMouseListener(this);
        jdLocEOIdownButton.setPreferredSize(new Dimension(15,10));
        jdLocEOIdownButton.setComponentPopupMenu(jdEOIsteppopup);
        locSpinnerPanel.add(jdLocEOIdownButton);
        SpringUtil.makeCompactGrid (locSpinnerPanel, 2, 1, 0,0,0,0);
        locEOIPanel.add (locSpinnerPanel);

        SpringUtil.makeCompactGrid (locEOIPanel, 1, locEOIPanel.getComponentCount(), 0,0,0,0);
        eoiClockPanel.add (locEOIPanel);

        SpringUtil.makeCompactGrid (eoiClockPanel, 2, eoiClockPanel.getComponentCount()/2, 2,2,2,2);
        eoiDateTimePanel.add(eoiClockPanel);

        JPanel twilightPanel = new JPanel(new SpringLayout());
        twilightPanel.setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY, 1));

        pmTwilightButton = new JButton(pmTwilightIcon);
        pmTwilightButton.setPreferredSize(new Dimension(19,20));
        pmTwilightButton.setToolTipText("Click to set the epoch of interest to the end of evening nautical twilight");
        pmTwilightButton.setEnabled(!useNowEpoch && timeEnabled);
        pmTwilightButton.addActionListener(this);
        twilightPanel.add(pmTwilightButton);

        eoiPMTwilightTextField = new JTextField("");
        eoiPMTwilightTextField.setMargin(coordMargin);
        eoiPMTwilightTextField.setFont(p12);
        eoiPMTwilightTextField.setToolTipText("<html>The end of evening nautical twilight on the epoch of interest.<br>"+
                                              "Green background indicates epoch of interest is during dark time and is grey otherwise.<br>"+
                                              "Click to transfer this time to epoch of interest.</html>");
        eoiPMTwilightTextField.setEditable(false);
		eoiPMTwilightTextField.setPreferredSize(new Dimension((int)(coordSize.width*0.7),coordSize.height));
		eoiPMTwilightTextField.setHorizontalAlignment(JTextField.LEFT);
        eoiPMTwilightTextField.addActionListener(this);
        eoiPMTwilightTextField.addMouseListener(this);
        twilightPanel.add (eoiPMTwilightTextField);


        amTwilightButton = new JButton(amTwilightIcon);
        amTwilightButton.setPreferredSize(new Dimension(19,20));
        amTwilightButton.setToolTipText("Click to set the epoch of interest to the beginning of morning nautical twilight");
        amTwilightButton.setEnabled(!useNowEpoch && timeEnabled);
        amTwilightButton.addActionListener(this);
        twilightPanel.add(amTwilightButton);

        eoiAMTwilightTextField = new JTextField("");
        eoiAMTwilightTextField.setMargin(coordMargin);
        eoiAMTwilightTextField.setFont(p12);
        eoiAMTwilightTextField.setToolTipText("<html>The beginning of morning nautical twilight on the epoch of interest.<br>"+
                                              "Green background indicates epoch of interest is during dark time and is grey otherwise.<br>"+
                                              "Click to transfer this time to epoch of interest.</html>");
        eoiAMTwilightTextField.setEditable(false);
		eoiAMTwilightTextField.setPreferredSize(new Dimension((int)(coordSize.width*0.7),coordSize.height));
		eoiAMTwilightTextField.setHorizontalAlignment(JTextField.LEFT);
        eoiAMTwilightTextField.addActionListener(this);
        eoiAMTwilightTextField.addMouseListener(this);
        twilightPanel.add (eoiAMTwilightTextField);

        SpringUtil.makeCompactGrid (twilightPanel,2,twilightPanel.getComponentCount()/2, 2,2,2,2);
        eoiDateTimePanel.add(twilightPanel);


        JPanel eoiDateTimeRightPanel = new JPanel(new SpringLayout());
        eoiDateTimeRightPanel.setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY, 1));

//        JPanel eoiDateTimeUpperPanel = new JPanel(new SpringLayout());
//        eoiDateTimeUpperPanel.setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY, 1));

        JLabel eoiJDLabel = new JLabel ("JD:");
        eoiJDLabel.setFont(p12);
        eoiJDLabel.setToolTipText("<html>Enter the UTC Julian date of interest and press Enter<br>"+
                                  "Right click to set spinner and scroll wheel step value</html>");
        eoiJDLabel.setHorizontalAlignment (JTextField.RIGHT);
        eoiJDLabel.setComponentPopupMenu(jdEOIsteppopup);
		eoiDateTimeRightPanel.add (eoiJDLabel);

		eoiJDTextField = new JTextField("");
        eoiJDTextField.setMargin(coordMargin);
        eoiJDTextField.setFont(p12);
        eoiJDTextField.setToolTipText("<html>Enter the UTC Julian date of interest and press Enter<br>"+
                                      "Right click to set spinner and scroll wheel step value</html>");
        eoiJDTextField.setEditable(!useNowEpoch && timeEnabled);
		eoiJDTextField.setPreferredSize(new Dimension((int)(coordSize.width*1.1),coordSize.height));
		eoiJDTextField.setHorizontalAlignment(JTextField.LEFT);
        eoiJDTextField.addActionListener(this);
        eoiJDTextField.addMouseWheelListener( new MouseWheelListener()
            {
            public void mouseWheelMoved( MouseWheelEvent e )
                {
                if (!useNowEpoch && timeEnabled && !running)
                    {
                    if (scrollfinishedtimer != null) scrollfinishedtimer.cancel();
                    if (scrollfinishedtask != null) scrollfinishedtask.cancel();
                    updateJDEOI(jdEOIStep, -jdEOIStepFactor*e.getWheelRotation(), true);
                    startScrollFinishedTimer();
                    }
                }
            });
        eoiJDTextField.setComponentPopupMenu(jdEOIsteppopup);
        eoiDateTimeRightPanel.add (eoiJDTextField);

        JLabel eoiLSTLabel = new JLabel ("LST:");
        eoiLSTLabel.setFont(p12);
        eoiLSTLabel.setToolTipText("Local sidereal time at epoch of interest");
        eoiLSTLabel.setHorizontalAlignment (JTextField.RIGHT);
		eoiDateTimeRightPanel.add (eoiLSTLabel);

		eoiLSTTextField = new JTextField("");
        eoiLSTTextField.setMargin(coordMargin);
        eoiLSTTextField.setFont(p12);
        eoiLSTTextField.setToolTipText("Local sidereal time at epoch of interest");
        eoiLSTTextField.setEditable(false);
        eoiLSTTextField.setBackground(leapGray);
		eoiLSTTextField.setPreferredSize(new Dimension((int)(coordSize.width*0.7),coordSize.height));
		eoiLSTTextField.setHorizontalAlignment(JTextField.LEFT);
        eoiLSTTextField.addActionListener(this);
        eoiDateTimeRightPanel.add (eoiLSTTextField);

//        SpringUtil.makeCompactGrid (eoiDateTimeUpperPanel,1,eoiDateTimeUpperPanel.getComponentCount(), 2,2,2,2);
//        eoiDateTimeRightPanel.add(eoiDateTimeUpperPanel);

//        JPanel eoiDateTimeLowerPanel = new JPanel(new SpringLayout());

//        JLabel eoiDateTimeLowerPanelDummyLabel = new JLabel ("");
//        eoiDateTimeLowerPanelDummyLabel.setFont(p12);
//        eoiDateTimeLowerPanelDummyLabel.setPreferredSize(new Dimension((int)(coordSize.width*0.1),coordSize.height));
//        eoiDateTimeLowerPanel.add(eoiDateTimeLowerPanelDummyLabel);


//        JPanel eoiHJDPanel = new JPanel(new SpringLayout());
//        eoiHJDPanel.setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY, 1));

        JLabel eoiHJDLabel = new JLabel ("HJD:");
        eoiHJDLabel.setFont(p12);
        eoiHJDLabel.setToolTipText("Heliocentric Julian Date in UTC Time [HJD(UTC)]");
        eoiHJDLabel.setHorizontalAlignment (JTextField.RIGHT);
		eoiDateTimeRightPanel.add (eoiHJDLabel);

		eoiHJDTextField = new JTextField("0.0");
        eoiHJDTextField.setMargin(coordMargin);
        eoiHJDTextField.setFont(p12);
        eoiHJDTextField.setToolTipText("Heliocentric Julian Date in UTC Time [HJD(UTC)]");
        eoiHJDTextField.setEditable(!useNowEpoch && timeEnabled);
		eoiHJDTextField.setPreferredSize(new Dimension((int)(coordSize.width*1.1),coordSize.height));
		eoiHJDTextField.setHorizontalAlignment(JTextField.LEFT);
        eoiHJDTextField.addActionListener(this);
        eoiDateTimeRightPanel.add (eoiHJDTextField);

        JLabel eoiHJDDTLabel = new JLabel ("dT:");
        eoiHJDDTLabel.setFont(p12);
        eoiHJDDTLabel.setToolTipText("HJD(UTC) - JD(UTC) in h:m:s");
        eoiHJDDTLabel.setHorizontalAlignment (JTextField.RIGHT);
		eoiDateTimeRightPanel.add (eoiHJDDTLabel);

		eoiHJDdTTextField = new JTextField("0.0");
        eoiHJDdTTextField.setMargin(coordMargin);
        eoiHJDdTTextField.setFont(p12);
        eoiHJDdTTextField.setToolTipText("HJD(UTC) - JD(UTC) in h:m:s");
        eoiHJDdTTextField.setEditable(false);
        eoiHJDdTTextField.setBackground(leapGray);
		eoiHJDdTTextField.setPreferredSize(new Dimension((int)(coordSize.width*0.7),coordSize.height));
		eoiHJDdTTextField.setHorizontalAlignment(JTextField.LEFT);
        eoiHJDdTTextField.addActionListener(this);
        eoiDateTimeRightPanel.add (eoiHJDdTTextField);

//        JLabel eoiMinuteLabel = new JLabel ("(min)");
//        eoiMinuteLabel.setFont(p12);
//        eoiMinuteLabel.setToolTipText("BJD(TDB) - JD(UTC) in minutes");
//        eoiMinuteLabel.setHorizontalAlignment (JTextField.LEFT);
//		eoiDeltaTPanel.add (eoiMinuteLabel);

//        SpringUtil.makeCompactGrid (eoiHJDPanel,1,eoiHJDPanel.getComponentCount(), 2,2,2,2);
//        eoiDateTimeRightPanel.add(eoiHJDPanel);

//        SpringUtil.makeCompactGrid (eoiDateTimeLowerPanel,1,eoiDateTimeLowerPanel.getComponentCount(), 2,2,2,2);
//        eoiDateTimeRightPanel.add(eoiDateTimeLowerPanel);

        SpringUtil.makeCompactGrid (eoiDateTimeRightPanel,2,eoiDateTimeRightPanel.getComponentCount()/2, 2,2,2,2);
        eoiDateTimePanel.add(eoiDateTimeRightPanel);

        SpringUtil.makeCompactGrid (eoiDateTimePanel, 1, eoiDateTimePanel.getComponentCount(), 2,2,2,2);
        eoiPanel.add(eoiDateTimePanel);

        JPanel eoiDeltaTPanel = new JPanel(new SpringLayout());
        eoiDeltaTPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY, 1), "Dynamical Time", TitledBorder.LEFT, TitledBorder.TOP, p12, Color.DARK_GRAY));

        JPanel eoiLeapSecPanel = new JPanel(new SpringLayout());
        eoiLeapSecPanel.setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY, 1));

        JLabel updateleapSecLabel = new JLabel ("Update");
        updateleapSecLabel.setFont(p12);
        updateleapSecLabel.setToolTipText("Leapsecs are now updated via the Earth Rotation Center as USNO Leapsec server is no longer available.\n Leap seconds before 2017 are provided by AIJ."); //("Update leap second table from USNO website");
        updateleapSecLabel.setHorizontalAlignment (JTextField.RIGHT);
        updateleapSecLabel.setEnabled(true);
		eoiLeapSecPanel.add (updateleapSecLabel);

        updateLeapSecTableButton = new JButton(updateLeapSecTableIcon);
        updateLeapSecTableButton.setToolTipText("Leapsecs are now updated via the Earth Rotation Center as USNO Leapsec server is no longer available.\n Leap seconds before 2017 are provided by AIJ."); //("Update leap second table from USNO website");
        updateLeapSecTableButton.setMargin(buttonMargin);
        updateLeapSecTableButton.addActionListener(this);
        updateLeapSecTableButton.setPreferredSize(iconDimension);
        eoiLeapSecPanel.add(updateLeapSecTableButton);
        updateLeapSecTableButton.setEnabled(true);

        JLabel autoLeapSecLabel = new JLabel ("  Auto");
        autoLeapSecLabel.setFont(p12);
        autoLeapSecLabel.setToolTipText("Enable to automatically calculate leap seconds for epoch of interest, disable to manually enter leap seconds");
        autoLeapSecLabel.setHorizontalAlignment (JTextField.RIGHT);
		eoiLeapSecPanel.add (autoLeapSecLabel);

        autoLeapSecCB = new JCheckBox("", autoLeapSec);
        autoLeapSecCB.setToolTipText("Enable to automatically calculate leap seconds for epoch of interest, disable to manually enter leap seconds");
        autoLeapSecCB.addItemListener(this);
        eoiLeapSecPanel.add (autoLeapSecCB);

        JLabel leapSecLabel = new JLabel ("Leap-secs:");
        leapSecLabel.setFont(p12);
        eoiLeapSecPanel.setToolTipText("<html>The number of leap seconds for the epoch of interest, where leap seconds = UTC - TAI<br>"+
                                        "Background color indication:<br>"+
                                        "Green: value from AstroImageJ leap second table<br>"+
                                        "Yellow: calculated value from Espenak and Meeus 2006<br>"+
                                        "Red: outside the -1999 to +3000 range of Espenak and Meeus 2006<br>"+
                                        "White: editable manual entry</html>");
        leapSecLabel.setHorizontalAlignment (JTextField.RIGHT);
		eoiLeapSecPanel.add (leapSecLabel);

		leapSecTextField = new JTextField(onePlaces.format(leapSec));
        leapSecTextField.setMargin(coordMargin);
        leapSecTextField.setFont(p12);
        leapSecTextField.setToolTipText("<html>The number of leap seconds for the epoch of interest, where leap seconds = UTC - TAI<br>"+
                                        "Background color indication:<br>"+
                                        "Green: value from AstroImageJ leap second table<br>"+
                                        "Yellow: calculated value from Espenak and Meeus 2006<br>"+
                                        "Red: outside the -1999 to +3000 range of Espenak and Meeus 2006<br>"+
                                        "White: editable manual entry</html>");
        leapSecTextField.setEditable(!autoLeapSec);
		leapSecTextField.setPreferredSize(new Dimension((int)(coordSize.width*0.89),coordSize.height));
		leapSecTextField.setHorizontalAlignment(JTextField.LEFT);
        leapSecTextField.addActionListener(this);
        eoiLeapSecPanel.add (leapSecTextField);

        SpringUtil.makeCompactGrid (eoiLeapSecPanel,1,eoiLeapSecPanel.getComponentCount(), 2,2,2,2);
        eoiDeltaTPanel.add(eoiLeapSecPanel);

//        JLabel leapDummyLabel = new JLabel ("");
//        leapDummyLabel.setFont(p12);
//        leapDummyLabel.setPreferredSize(new Dimension((int)(coordSize.width*0.05),coordSize.height));
//        eoiDeltaTPanel.add(leapDummyLabel);

        JPanel eoiBJDPanel = new JPanel(new SpringLayout());
        eoiBJDPanel.setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY, 1));

        JPanel OSUPanel = new JPanel(new SpringLayout());

        OSULabel = new JLabel ("OSU/internal");
        OSULabel.setFont(p12);
        OSULabel.setToolTipText("Use Ohio State BJD server, deselect to calculate internally");
        OSULabel.setHorizontalAlignment (JTextField.RIGHT);
		OSUPanel.add (OSULabel);

        useOhioStateCheckBox = new JCheckBox("", useOhioState);
        useOhioStateCheckBox.setToolTipText("Use Ohio State BJD server, deselect to calculate internally");
        useOhioStateCheckBox.addItemListener(this);
        OSUPanel.add(useOhioStateCheckBox);

        SpringUtil.makeCompactGrid (OSUPanel,1,OSUPanel.getComponentCount(), 0,0,0,0);
        eoiBJDPanel.add(OSUPanel);

        JLabel eoiBJDLabel = new JLabel ("BJD:");
        eoiBJDLabel.setFont(p12);
        eoiBJDLabel.setPreferredSize(new Dimension((int)(coordSize.width*0.32),coordSize.height));
        eoiBJDLabel.setToolTipText("Barycentric Julian Date in Barycentric Dynamical Time [BJD(TDB)]");
        eoiBJDLabel.setHorizontalAlignment (JTextField.RIGHT);
		eoiBJDPanel.add (eoiBJDLabel);

		eoiBJDTextField = new JTextField("0.0");
        eoiBJDTextField.setMargin(coordMargin);
        eoiBJDTextField.setFont(p12);
        eoiBJDTextField.setToolTipText("Barycentric Julian Date in Barycentric Dynamical Time [BJD(TDB)]");
        eoiBJDTextField.setEditable(!useNowEpoch && timeEnabled);
		eoiBJDTextField.setPreferredSize(new Dimension((int)(coordSize.width*1.1),coordSize.height));
		eoiBJDTextField.setHorizontalAlignment(JTextField.LEFT);
        eoiBJDTextField.addActionListener(this);
        eoiBJDPanel.add (eoiBJDTextField);

        JLabel eoiDTLabel = new JLabel ("  dT:");
        eoiDTLabel.setFont(p12);
        eoiDTLabel.setToolTipText("BJD(TDB) - JD(UTC) in h:m:s");
        eoiDTLabel.setHorizontalAlignment (JTextField.RIGHT);
		eoiBJDPanel.add (eoiDTLabel);

		eoiBJDdTTextField = new JTextField("0.0");
        eoiBJDdTTextField.setMargin(coordMargin);
        eoiBJDdTTextField.setFont(p12);
        eoiBJDdTTextField.setToolTipText("BJD(TDB) - JD(UTC) in h:m:s");
        eoiBJDdTTextField.setEditable(false);
        eoiBJDdTTextField.setBackground(leapGray);
		eoiBJDdTTextField.setPreferredSize(new Dimension((int)(coordSize.width*0.7),coordSize.height));
		eoiBJDdTTextField.setHorizontalAlignment(JTextField.LEFT);
        eoiBJDdTTextField.addActionListener(this);
        eoiBJDPanel.add (eoiBJDdTTextField);

//        JLabel eoiMinuteLabel = new JLabel ("(min)");
//        eoiMinuteLabel.setFont(p12);
//        eoiMinuteLabel.setToolTipText("BJD(TDB) - JD(UTC) in minutes");
//        eoiMinuteLabel.setHorizontalAlignment (JTextField.LEFT);
//		eoiDeltaTPanel.add (eoiMinuteLabel);

        SpringUtil.makeCompactGrid (eoiBJDPanel,1,eoiBJDPanel.getComponentCount(), 1,2,1,2);
        eoiDeltaTPanel.add(eoiBJDPanel);

        SpringUtil.makeCompactGrid (eoiDeltaTPanel,1,eoiDeltaTPanel.getComponentCount(), 2,2,5,2);
        eoiPanel.add(eoiDeltaTPanel);



        JPanel eoiCoordPanel = new JPanel(new SpringLayout());


        JPanel ObsEquPanel = new JPanel(new SpringLayout());
        ObsEquPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY, 1), "Equatorial", TitledBorder.CENTER, TitledBorder.TOP, p12, Color.DARK_GRAY));

        JLabel obsRaLabel = new JLabel ("   RA:");
        obsRaLabel.setFont(p12);
        obsRaLabel.setHorizontalAlignment (JTextField.RIGHT);
		ObsEquPanel.add (obsRaLabel);

		raEOITextField = new JTextField(raEOIText);
        raEOITextField.setMargin(coordMargin);
        raEOITextField.setFont(p12);
		raEOITextField.setPreferredSize(coordSize);
        raEOITextField.setBorder(newradecEOI?greenBorder:grayBorder);
		raEOITextField.setHorizontalAlignment(JTextField.LEFT);
        raEOITextField.setToolTipText("<html>"+"Enter target right ascension in hours in either decimal or sexagesimal format and press 'Enter'"+"<br>"+
            "--Or--"+"<br>"+
            "Enter RA (hours) and DEC (degrees) and press 'Enter' to populate both fields"+"</html>");
        raEOITextField.addActionListener(this);
        ObsEquPanel.add (raEOITextField);

        JLabel obsDecLabel = new JLabel ("   Dec:");
        obsDecLabel.setFont(p12);
        obsDecLabel.setHorizontalAlignment (JTextField.RIGHT);
		ObsEquPanel.add (obsDecLabel);

		decEOITextField = new JTextField (decEOIText);
        decEOITextField.setMargin(coordMargin);
        decEOITextField.setFont(p12);
		decEOITextField.setPreferredSize(coordSize);
        decEOITextField.setBorder(newradecEOI?greenBorder:grayBorder);
		decEOITextField.setHorizontalAlignment(JTextField.LEFT);
        decEOITextField.setToolTipText("<html>"+"Enter target declination in degrees in either decimal or sexagesimal format and press 'Enter'"+"<br>"+
            "--Or--"+"<br>"+
            "Enter RA (hours) and DEC (degrees) and press 'Enter' to populate both fields"+"</html>");
        decEOITextField.addActionListener(this);
        ObsEquPanel.add (decEOITextField);

        SpringUtil.makeCompactGrid (ObsEquPanel,1,ObsEquPanel.getComponentCount(), 2,0,2,2);
        eoiCoordPanel.add(ObsEquPanel);

        JPanel elatlonEOIPanel = new JPanel(new SpringLayout());
        elatlonEOIPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY, 1), "Ecliptic", TitledBorder.CENTER, TitledBorder.TOP, p12, Color.DARK_GRAY));

        JLabel elonEOILabel = new JLabel ("   Lon:");
        elonEOILabel.setFont(p12);
        elonEOILabel.setHorizontalAlignment (JTextField.RIGHT);
		elatlonEOIPanel.add (elonEOILabel);

		eclLonEOITextField = new JTextField(eclLonEOIText);
        eclLonEOITextField.setMargin(coordMargin);
        eclLonEOITextField.setFont(p12);
		eclLonEOITextField.setPreferredSize(coordSize);
        eclLonEOITextField.setBorder(newelonlatEOI?greenBorder:grayBorder);
		eclLonEOITextField.setHorizontalAlignment(JTextField.LEFT);
        eclLonEOITextField.setToolTipText("<html>"+"Enter target ecliptic longitude in degrees in either decimal or sexagesimal format and press 'Enter'"+"<br>"+
            "--Or--"+"<br>"+
            "Enter ecliptic longitude and latitude in degrees and press 'Enter' to populate both fields"+"</html>");
        eclLonEOITextField.addActionListener(this);
        elatlonEOIPanel.add (eclLonEOITextField);

        JLabel elatEOILabel = new JLabel ("   Lat:");
        elatEOILabel.setFont(p12);
        elatEOILabel.setHorizontalAlignment (JTextField.RIGHT);
		elatlonEOIPanel.add (elatEOILabel);

		eclLatEOITextField = new JTextField (eclLatEOIText);
        eclLatEOITextField.setMargin(coordMargin);
        eclLatEOITextField.setFont(p12);
		eclLatEOITextField.setPreferredSize(coordSize);
        eclLatEOITextField.setBorder(newelonlatEOI?greenBorder:grayBorder);
		eclLatEOITextField.setHorizontalAlignment(JTextField.LEFT);
        eclLatEOITextField.setToolTipText("<html>"+"Enter target ecliptic latitude in degrees in either decimal or sexagesimal format and press 'Enter'"+"<br>"+
            "--Or--"+"<br>"+
            "Enter ecliptic longitude and latitude in degrees and press 'Enter' to populate both fields"+"</html>");
        eclLatEOITextField.addActionListener(this);
        elatlonEOIPanel.add (eclLatEOITextField);

        SpringUtil.makeCompactGrid (elatlonEOIPanel,1,elatlonEOIPanel.getComponentCount(), 2,0,2,2);
        eoiCoordPanel.add(elatlonEOIPanel);



        JPanel ObsHorPanel = new JPanel(new SpringLayout());
        ObsHorPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY, 1), "Horizontal", TitledBorder.CENTER, TitledBorder.TOP, p12, Color.DARK_GRAY));

        JLabel obsAltLabel = new JLabel ("   Alt:");
        obsAltLabel.setFont(p12);
        obsAltLabel.setHorizontalAlignment (JTextField.RIGHT);
		ObsHorPanel.add (obsAltLabel);

		altEOITextField = new JTextField(altEOIText);
        altEOITextField.setMargin(coordMargin);
        altEOITextField.setFont(p12);
		altEOITextField.setPreferredSize(coordSize);
        altEOITextField.setBorder(newaltazEOI?greenBorder:grayBorder);
		altEOITextField.setHorizontalAlignment(JTextField.LEFT);
        altEOITextField.setToolTipText("<html>"+"Enter target altitude in degrees in either decimal or sexagesimal format and press 'Enter'"+"<br>"+
            "--Or--"+"<br>"+
            "Enter altitude and azimuth in degrees and press 'Enter' to populate both fields"+"</html>");
        altEOITextField.addActionListener(this);
        ObsHorPanel.add (altEOITextField);

        JLabel obsAzLabel = new JLabel ("      Az:");
        obsAzLabel.setFont(p12);
        obsAzLabel.setHorizontalAlignment (JTextField.RIGHT);
		ObsHorPanel.add (obsAzLabel);

		azEOITextField = new JTextField (azEOIText);
        azEOITextField.setMargin(coordMargin);
        azEOITextField.setFont(p12);
		azEOITextField.setPreferredSize(coordSize);
        azEOITextField.setBorder(newaltazEOI?greenBorder:grayBorder);
		azEOITextField.setHorizontalAlignment(JTextField.LEFT);
        azEOITextField.setToolTipText("<html>"+"Enter target azimuth in degrees in either decimal or sexagesimal format and press 'Enter'"+"<br>"+
            "--Or--"+"<br>"+
            "Enter altitude and azimuth in degrees and press 'Enter' to populate both fields"+"</html>");
        azEOITextField.addActionListener(this);
        ObsHorPanel.add (azEOITextField);

        SpringUtil.makeCompactGrid (ObsHorPanel,1,ObsHorPanel.getComponentCount(), 2,0,2,2);
        eoiCoordPanel.add(ObsHorPanel);



        JPanel hazdEOIPanel = new JPanel(new SpringLayout());
        hazdEOIPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY, 1), "Direction - Hour Angle - Zenith Distance - Airmass", TitledBorder.CENTER, TitledBorder.TOP, p12, Color.DARK_GRAY));

        JLabel dirEOILabel = new JLabel ("Dir:");
        dirEOILabel.setFont(p12);
        dirEOILabel.setHorizontalAlignment (JTextField.RIGHT);
		hazdEOIPanel.add (dirEOILabel);

        dirTextField = new JTextField (dirText);
        dirTextField.setMargin(coordMargin);
        dirTextField.setFont(p12);
		dirTextField.setPreferredSize(new Dimension(35,coordSize.height));
        dirTextField.setEditable(false);
        dirTextField.setBackground(leapGray);
		dirTextField.setHorizontalAlignment(JTextField.CENTER);
        dirTextField.setToolTipText("Direction of target on the sky");
        hazdEOIPanel.add (dirTextField);

        JLabel haEOILabel = new JLabel (" HA:");
        haEOILabel.setFont(p12);
        haEOILabel.setHorizontalAlignment (JTextField.RIGHT);
		hazdEOIPanel.add (haEOILabel);

		haEOITextField = new JTextField(haEOIText);
        haEOITextField.setMargin(coordMargin);
        haEOITextField.setFont(p12);
		haEOITextField.setPreferredSize(new Dimension((int)(coordSize.width*0.6),coordSize.height));
        haEOITextField.setEditable(false);
        haEOITextField.setBackground(leapGray);
		haEOITextField.setHorizontalAlignment(JTextField.LEFT);
        haEOITextField.setToolTipText("Hour angle of target (HA = LST - RA)");
//        haEOITextField.addActionListener(this);
        hazdEOIPanel.add (haEOITextField);

        JLabel zdEOILabel = new JLabel (" ZD:");
        zdEOILabel.setFont(p12);
        zdEOILabel.setHorizontalAlignment (JTextField.RIGHT);
		hazdEOIPanel.add (zdEOILabel);

		zdEOITextField = new JTextField (zdEOIText);
        zdEOITextField.setMargin(coordMargin);
        zdEOITextField.setFont(p12);
		zdEOITextField.setPreferredSize(new Dimension((int)(coordSize.width*0.6),coordSize.height));
        zdEOITextField.setEditable(false);
        zdEOITextField.setBackground(leapGray);
		zdEOITextField.setHorizontalAlignment(JTextField.LEFT);
        zdEOITextField.setToolTipText("Zenith Distance of target (ZD = 90 - ALT)");
//        zdEOITextField.addActionListener(this);
        hazdEOIPanel.add (zdEOITextField);

        JLabel airmassEOILabel = new JLabel (" AM:");
        airmassEOILabel.setFont(p12);
        airmassEOILabel.setHorizontalAlignment (JTextField.RIGHT);
		hazdEOIPanel.add (airmassEOILabel);

		airmassTextField = new JTextField (airmassText);
        airmassTextField.setMargin(coordMargin);
        airmassTextField.setFont(p12);
		airmassTextField.setPreferredSize(new Dimension((int)(coordSize.width*0.55),coordSize.height));
        airmassTextField.setEditable(false);
        airmassTextField.setBackground(leapGray);
		airmassTextField.setHorizontalAlignment(JTextField.LEFT);
        airmassTextField.setToolTipText("Airmass of target");
//        zdEOITextField.addActionListener(this);
        hazdEOIPanel.add (airmassTextField);

        SpringUtil.makeCompactGrid (hazdEOIPanel,1,hazdEOIPanel.getComponentCount(), 2,0,2,2);
        eoiCoordPanel.add(hazdEOIPanel);

        SpringUtil.makeCompactGrid (eoiCoordPanel,2,eoiCoordPanel.getComponentCount()/2, 2,0,2,2);
        eoiPanel.add(eoiCoordPanel);


        JPanel ssbPanel = new JPanel(new SpringLayout());
        ssbPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY, 1), "Phase - Altitude - Proximity", TitledBorder.LEFT, TitledBorder.TOP, p12, Color.DARK_GRAY));

        moonPhaseLabel = new JLabel(moonIcon[0]);
        moonPhaseLabel.setToolTipText("Moon phase at epoch of interest");
//        moonPhaseLabel.setMargin(buttonMargin);
        moonPhaseLabel.setBackground(Color.BLACK);
        moonPhaseLabel.setPreferredSize(new Dimension(57, 57));
        ssbPanel.add(moonPhaseLabel);

        for (int i : new int[]{2,0,1,3,4,5,6,7,8})
            {
            ssbPanels[i] = new JPanel(new SpringLayout());
            ssbPanels[i].setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY, 1), ssbNames[i], TitledBorder.CENTER, TitledBorder.TOP, p12, Color.DARK_GRAY));
            ssbPanels[i].addMouseListener(this);

            Insets ssbTopMargin = new Insets(0, 0, 0, 0);  //top,left,bottom,right
            Insets ssbBotMargin = new Insets(0, 0, 0, 0);  //top,left,bottom,right

            ssbAltitudeFields[i] = new JTextField ("000.00");
            ssbAltitudeFields[i].setMargin(ssbTopMargin);
            ssbAltitudeFields[i].setBorder(BorderFactory.createEmptyBorder());
            ssbAltitudeFields[i].setFont(p12);
            ssbAltitudeFields[i].setEditable(false);
            ssbAltitudeFields[i].setHorizontalAlignment(JTextField.CENTER);
            ssbAltitudeFields[i].setToolTipText("Altitude of "+ssbNames[i]+" from the horizon in degrees");
            ssbAltitudeFields[i].addMouseListener(this);
            ssbPanels[i].add(ssbAltitudeFields[i]);

            ssbDistanceFields[i] = new JTextField ("000.00");
            ssbDistanceFields[i].setMargin(ssbBotMargin);
            ssbDistanceFields[i].setBorder(BorderFactory.createEmptyBorder());
            ssbDistanceFields[i].setFont(p12);
            ssbDistanceFields[i].setEditable(false);
            ssbDistanceFields[i].setHorizontalAlignment(JTextField.CENTER);
            ssbDistanceFields[i].setToolTipText("<html>Distance from object to "+ssbNames[i]+" in degrees of arc<br>"+
                                                "green background indicates more than "+caution[i]+" degrees separation<br>"+
                                                "yellow background indicates less than "+caution[i]+" degrees separation<br>"+
                                                "red background indicates less than "+warning[i]+" degrees separation</html>");
            ssbDistanceFields[i].addMouseListener(this);
            ssbPanels[i].add(ssbDistanceFields[i]);

            SpringUtil.makeCompactGrid (ssbPanels[i], ssbPanels[i].getComponentCount(),1, 0,0,0,0);
            ssbPanel.add(ssbPanels[i]);
            }

        SpringUtil.makeCompactGrid (ssbPanel,1,ssbPanel.getComponentCount(), 0,0,0,0);
        eoiPanel.add(ssbPanel);


        SpringUtil.makeCompactGrid (eoiPanel,eoiPanel.getComponentCount(),1, 2,0,2,2);
        mainPanel.add(eoiPanel);

        oldjdEOI = jdEOI;

		SpringUtil.makeCompactGrid (mainPanel,mainPanel.getComponentCount(), 1, 6,6,6,6);

        enableLonLatAlt(selectedObservatory == 0);

        dialogFrame.setJMenuBar(menuBar);
		dialogFrame.add (mainScrollPane);
		dialogFrame.pack();
		dialogFrame.setResizable (true);
        
        IJU.setFrameSizeAndLocation(dialogFrame, dialogFrameLocationX, dialogFrameLocationY, 0, 0);

		dialogFrame.setVisible (showPanel);

        ToolTipManager.sharedInstance().setEnabled(showToolTips);

        computeCoordinates();

        startTimer();
		}


     public boolean isLocationOnScreen(Point loc) {
        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        GraphicsDevice[] gds = ge.getScreenDevices();
        Rectangle bounds = new Rectangle();
        for (int j = 0; j < gds.length; j++) {
          GraphicsDevice gd = gds[j];
          bounds.setRect(gd.getDefaultConfiguration().getBounds());
          if (bounds.contains(loc.x, loc.y)) {
            return true;
          }
        }
        return false;
	}

    public void saveAndClose()
            {
            if (timer != null) timer.cancel();
            if (task != null) task.cancel();
            if (spinnertimer != null) spinnertimer.cancel();
            if (spinnertask != null) spinnertask.cancel();
            if (scrollfinishedtimer != null) scrollfinishedtimer.cancel();
            if (scrollfinishedtask != null) scrollfinishedtask.cancel();
            savePrefs();
            dialogFrame.dispose();
            }


	public void itemStateChanged (ItemEvent e)
		{
		Object source = e.getItemSelectable();
		if (e.getStateChange() == ItemEvent.SELECTED)
			{
			if (source == showSexagesimalCB)
                {
                optionChanged = true;
				showSexagesimal = true;
                }
            else if (source == useCustomObservatoryListCB)
                {
                useCustomObservatoryList = true;
                }
            else if(source == showToolTipsCB)
				showToolTips = true;
            else if(source == useHarvardCB)
				useHarvard = true;
            else if(source == useOhioStateCB || source == useOhioStateCheckBox)
                {
				useOhioState = true;
                useOhioStateCB.setSelected(useOhioState);
                useOhioStateCheckBox.setSelected(useOhioState);
                }
            else if (source == useSkyMapBetaCB)
                {
                useSkyMapBeta = true;
                }
            else if(source == useProxyCB)
				useProxy = true;            
            else if(source == usePMCB)
				usePM = true;
            else if(source == usePrecCB)
				usePrec = true;
            else if(source == useNutCB)
				useNut = true;
            else if(source == useAberCB)
				useAber = true;
            else if(source == useRefrCB)
				useRefr = true;
            else if(source == useNowEpochCB)
                {
				useNowEpoch = true;
                updateTimeEditable();
                }
            else if(source == autoLeapSecCB)
                {
				autoLeapSec = true;
                leapSecTextField.setEditable(!autoLeapSec);
                }
            else if(source == autoTimeZoneCB)
                {
				autoTimeZone = true;
                timeZoneOffsetSpinner.setEnabled(!autoTimeZone);
                timeZoneOffsetSpinner.setBackground(leapGray);
                }
            else if(source == useAMPMCB)
                {
				useAMPM = true;
                }
            else if(source == showLocalTwilightCB)
                {
				twiLocal = true;
                }
            else if(source == reportSSBDownCB)
                {
				reportSSBDown = true;
                }
			}
		else if (e.getStateChange() == ItemEvent.DESELECTED)
			{
			if (source == showSexagesimalCB)
                {
                optionChanged = true;
				showSexagesimal = false;
                }
            else if (source == useCustomObservatoryListCB)
                {
                useCustomObservatoryList = false;
                }
            else if(source == useHarvardCB)
				useHarvard = false;
            else if(source == useOhioStateCB || source == useOhioStateCheckBox)
                {
				useOhioState = false;
                useOhioStateCB.setSelected(useOhioState);
                useOhioStateCheckBox.setSelected(useOhioState);
                }
            else if (source == useSkyMapBetaCB)
                {
                useSkyMapBeta = false;
                }            
            else if(source == useProxyCB)
				useProxy = false;            
            else if(source == showToolTipsCB)
				showToolTips = false;
            else if(source == usePMCB)
				usePM = false;
            else if(source == usePrecCB)
				usePrec = false;
            else if(source == useNutCB)
				useNut = false;
            else if(source == useAberCB)
				useAber = false;
            else if(source == useRefrCB)
				useRefr = false;
            else if(source == useNowEpochCB)
                {
				useNowEpoch = false;
                updateTimeEditable();
                }
            else if(source == autoLeapSecCB)
                {
				autoLeapSec = false;
                leapSecTextField.setEditable(!autoLeapSec);
                leapSecTextField.setBackground(Color.WHITE);
                }
            else if(source == autoTimeZoneCB)
                {
				autoTimeZone = false;
                timeZoneOffsetSpinner.setEnabled(!autoTimeZone);
                timeZoneOffsetSpinner.setBackground(Color.WHITE);
                }
            else if(source == useAMPMCB)
                {
				useAMPM = false;
                }
            else if(source == showLocalTwilightCB)
                {
				twiLocal = false;
                }
            else if(source == reportSSBDownCB)
                {
				reportSSBDown = false;
                }

            }
        Prefs.set ("astroIJ.showToolTips",showToolTips);
        ToolTipManager.sharedInstance().setEnabled(showToolTips);
        computeCoordinates();
		}

    public void mousePressed(MouseEvent e)
        {
        if (((e.getModifiers() & MouseEvent.BUTTON1_MASK) != 0) && !useNowEpoch && timeEnabled)
            {
            mouseSource = e.getSource();
            jdEOIupMouseDown = true;
            jdEOIdownMouseDown = true;
            if (mouseSource == jdEOIupButton || mouseSource == jdLocEOIupButton)
                {
                updateJDEOI(jdEOIStep, jdEOIStepFactor, true);
                }
            else if (mouseSource == jdEOIdownButton || mouseSource == jdLocEOIdownButton)
                {
                updateJDEOI(jdEOIStep, -jdEOIStepFactor, true);
                }
            startSpinnerTimer();
            }
        }

    public void mouseReleased(MouseEvent e)
        {
        if (e.getButton() == MouseEvent.BUTTON1)
            {
            jdEOIupMouseDown = false;
            jdEOIdownMouseDown = false;
            spinnerActive = false;
            spinnertimer.cancel();
            spinnertask.cancel();
            processAction(null);
            }
        }
    public void mouseEntered(MouseEvent e)
        {
        }

    public void mouseExited(MouseEvent e)
        {
        }

    public void mouseClicked(MouseEvent e)
        {
        Object source = e.getSource();
        if (!useNowEpoch && timeEnabled && source == eoiPMTwilightTextField)
            {
            jdEOI = computeNauticalTwilight(jdEOI, leapSec, lat, lon, alt, true);
            computeCoordinates();
            }
        else if (!useNowEpoch && timeEnabled && source == eoiAMTwilightTextField)
            {
            jdEOI = computeNauticalTwilight(jdEOI, leapSec, lat, lon, alt, false);
            computeCoordinates();
            }
        else if (coordinateEntryEnabled)
            {
            for (int i=0; i<ssbPanels.length; i++)
                {
                if (source == ssbPanels[i] || source == ssbAltitudeFields[i] || source == ssbDistanceFields[i])
                    {
                    objectIDTextField.setText(ssbNames[i]);
                    processAction(objectIDTextField);
                    return;
                    }
                }
            }
        }



    public void stateChanged(ChangeEvent e)
        {
//        Object source = e.getSource();
//        if (source == jdEOIupButton && jdEOIupMouseDown)
//            {
//            jdEOI += jdEOIStep;
//            eoiJDTextField.setText(sixPlaces.format(jdEOI));
//            computeCoordinates();
//            }
//        else if (source == jdEOIdownButton && jdEOIdownMouseDown)
//            {
//            jdEOI = jdEOI-jdEOIStep<0?0.0:jdEOI-jdEOIStep;
//            eoiJDTextField.setText(sixPlaces.format(jdEOI));
//            computeCoordinates();
//            }


        }


	/**
	 * Response to pressing enter after changing text field.
	 */
    public void actionPerformed (ActionEvent ae)
        {
        Object source = ae.getSource();
        processAction(source);
        }

    public void processAction(Object source)
        {
        if (source == skyMapButton)
            {
            showInSkyMap();
            return;
            }
        if (source == simbadButton)
            {
            showInSIMBAD();
            return;
            }

        running = true;
        if(source == observatoryIDComboBox)
            {
            selectedObservatory = observatoryIDComboBox.getSelectedIndex();
            enableLonLatAlt(selectedObservatory == 0);
            if (selectedObservatory > 0)
                {
                if (selectedObservatory < observatoryLats.length) lat = observatoryLats[selectedObservatory];
                if (selectedObservatory < observatoryLons.length) lon = observatoryLons[selectedObservatory];
                if (selectedObservatory < observatoryAlts.length) alt = observatoryAlts[selectedObservatory];
                latTextField.setText(showSexagesimal ? decToSex(lat, 2, 90, true) : sixPlaces.format(lat));
                lonTextField.setText(showSexagesimal ? decToSex(lon, 2, 180, true) : sixPlaces.format(lon));
                altTextField.setText(uptoTwoPlaces.format(alt));
                if (dp && ignoreObservatoryAction) return;
                }
            }
		else if (source == savePrefsMenuItem)
			{
            savePrefs();
            }
		else if (source == exitMenuItem)
			{
            saveAndClose();
            }
		else if (source == helpMenuItem)
			{
            openHelpPanel();
            }
		else if (source == setProxyAddressMenuItem)
			{
            String inputString = JOptionPane.showInputDialog(decJ2000TextField, "Enter the proxy server address:", proxyAddress); 
            if (inputString != null && !inputString.equals("")) 
                {
                proxyAddress = inputString;
                socketaddr = new InetSocketAddress(proxyAddress, proxyPort);
                proxy = new Proxy(Proxy.Type.HTTP, socketaddr);
                }
            }        
		else if (source == setProxyPortMenuItem)
			{
            String inputString = JOptionPane.showInputDialog(decJ2000TextField, "Enter the proxy server port number (0-65535):", proxyPort); 
            if (inputString == null) return;
            int value = -1;
            try {
                value = Integer.parseInt(inputString);
                if (value >= 0 && value <= 65535)
                    {
                    proxyPort = value;
                    socketaddr = new InetSocketAddress(proxyAddress, proxyPort);
                    proxy = new Proxy(Proxy.Type.HTTP, socketaddr);                    
                    }
                else JOptionPane.showMessageDialog(decJ2000TextField, "ERROR: Proxy port must be in range 0 - 65535");                
                }
            catch (NumberFormatException nfe)
                {
                JOptionPane.showMessageDialog(decJ2000TextField, "ERROR: Bad number format. Proxy port must be in range 0 - 65535");
                }
            }         
        else if (source == updateLeapSecTableButton)
            {
            if (getUSNOLeapSecTable())
                {
                displayLeapSecondTable(true);
                }
            }
        else if (source == showLeapSecondTableMenuItem)
            {
            displayLeapSecondTable(false);
            }
        else if(source == latTextField || source == lonTextField || source == altTextField)
            {
            getLatLonAlt();
            }
        else if (source == pmRATextField || source == pmDecTextField)
            {
            getProperMotion();
            }
        else if (source == leapSecTextField)
            {
            newnum = Tools.parseDouble(leapSecTextField.getText().replaceAll("[^0-9\\.\\-]{1,}", " ").trim());
            if (!Double.isNaN(newnum))
                {
                leapSec = newnum;
//                leapSource = "(custom)";
                leapSecTextField.setBackground(Color.WHITE);
                }
            leapSecTextField.setText(onePlaces.format(leapSec));
//            leapSourceLabel.setText(leapSource);
            }

        else if (source == eoiUTDateTextField || source == eoiUTTimeTextField)
            {
            utDateEOI = getEOIDateAndTime(false);
            jdEOI = SkyAlgorithms.CalcJD(utDateEOI);
            }
        else if (source == eoiLocDateTextField || source == eoiLocTimeTextField)
            {
            locDateEOI = getEOIDateAndTime(true);
            jdEOI = localEOIDateToJD(locDateEOI);
            }
        else if (source == eoiJDTextField)
            {
            double jd = Tools.parseDouble(eoiJDTextField.getText().replaceAll("[^0-9\\.]{1,}", " ").trim());
            if (!Double.isNaN(jd))
                {
                jdEOI = jd < 0.0 ? 0.0 : jd;
                }
            }
        else if (source == nowEOIButton)
            {
            jdEOI = jdNow;
            }
        else if (source == pmTwilightButton)
            {
            jdEOI = computeNauticalTwilight(jdEOI, leapSec, lat, lon, alt, true);
            }
        else if (source == amTwilightButton)
            {
            jdEOI = computeNauticalTwilight(jdEOI, leapSec, lat, lon, alt, false);
            }
        else if (source == eoiHJDTextField)
            {
            double hjd = Tools.parseDouble(eoiHJDTextField.getText().replaceAll("[^0-9\\.]{1,}", " ").trim());
            if (!Double.isNaN(hjd))
                {
                double[] radec = SkyAlgorithms.Convert(2000.0, hjd, radecJ2000[0], radecJ2000[1], pmRA, pmDec, leapSec, FORWARD, usePM, usePrec, useNut, useAber);
                double[] utDate = SkyAlgorithms.UTDateFromJD(hjd);
                if (autoLeapSec)
                    {
                    if (!getTAIminusUTC())
                        {
                        showMessage("Leap second data corrupt, restoring default leap second data");
                        getTAIminusUTC(); //try a second time with default leap second arrays if current leap second arrays are null or different lengths
                        }
                    leapSecTextField.setText(onePlaces.format(leapSec));
                    }
                dT=leapSec+32.184;
                double lst = SkyAlgorithms.CalcLST((int)utDate[0], (int)utDate[1], (int)utDate[2], utDate[3], lon, leapSec);
                hjdEOICorr = calculateHJDCorrection(hjd, dT, radec[0], radec[1], lst, lat, alt);
                jdEOI = hjd-hjdEOICorr;
                }
            }
        else if (source == eoiBJDTextField)
            {
            double bjd = Tools.parseDouble(eoiBJDTextField.getText().replaceAll("[^0-9\\.]{1,}", " ").trim());
            if (!Double.isNaN(bjd))
                {
                jdEOI = bjd;
                utDateEOI = SkyAlgorithms.UTDateFromJD(jdEOI);
                if (autoLeapSec)
                    {
                    if (!getTAIminusUTC())
                        {
                        showMessage("Leap second data corrupt, restoring default leap second data");
                        getTAIminusUTC(); //try a second time with default leap second arrays if current leap second arrays are null or different lengths
                        }
                    leapSecTextField.setText(onePlaces.format(leapSec));
                    }
                dT=leapSec+32.184;
                double[] radec = SkyAlgorithms.Convert(2000.0, bjd, radecJ2000[0], radecJ2000[1], pmRA, pmDec, leapSec, FORWARD, usePM, usePrec, useNut, useAber);
                double[] utDate = SkyAlgorithms.UTDateFromJD(bjd);
                double lst = SkyAlgorithms.CalcLST((int)utDate[0], (int)utDate[1], (int)utDate[2], utDate[3], lon, leapSec);
                double gg = (357.53+0.9856003*(bjd-2451545.0))/DEG_IN_RADIAN;
                double jdtest = 0;
                if (useOhioState)
                    {
                    double del = bjd - getBJDTDB(bjd, radecJ2000[0], radecJ2000[1]);
                    jdtest = bjd + del;
                    }
                else
                    jdtest = bjd - ((dT+0.001658*Math.sin(gg)+0.000014*Math.sin(2*gg))/86400.0 + calculateBJDCorrection(bjd, dT, radec[0], radec[1], lst, lat, alt));
                jdtest = Tools.parseDouble(sixPlaces.format(jdtest),0.0);  //round to six places
                gg = (357.53+0.9856003*(jdtest-2451545.0))/DEG_IN_RADIAN;
                double bjdtest = 0;
                if (useOhioState)
                    {
                    bjdtest = getBJDTDB(jdtest, radecJ2000[0], radecJ2000[1]);
                    }
                else
                    bjdtest = jdtest + ((dT+0.001658*Math.sin(gg)+0.000014*Math.sin(2*gg))/86400.0 + calculateBJDCorrection(jdtest, dT, radec[0], radec[1], lst, lat, alt));
                bjdtest = Tools.parseDouble(sixPlaces.format(bjdtest),0.0);  //round to six places
                jdEOI = jdtest + (bjd - bjdtest);
                }
            }


        if (ssObject || source == objectIDTextField)
            {
            if (checkSSObjects())
                {
                validObjectID = true;
                processTextFields(raEOITextField);
                }
            else if(!ssObject && !objectIDTextField.getText().trim().equals("") && getSimbadData())
                {
                validObjectID = true;
                processTextFields(raJ2000TextField);
                }
            }
        else
            processTextFields(source);
        }

    void getProperMotion()
        {
        newnum = IJU.getTextFieldDoubleValue(pmRATextField);
        pmRA = Double.isNaN(newnum) ? pmRA : newnum;
        pmRATextField.setText(uptoFourPlaces.format(pmRA));

        newnum = IJU.getTextFieldDoubleValue(pmDecTextField);
        pmDec = Double.isNaN(newnum) ? pmDec : newnum;
        pmDecTextField.setText(uptoFourPlaces.format(pmDec));
        }

    void getLatLonAlt()
        {
        newnum = getLatLon(latTextField);
        lat = Double.isNaN(newnum) ? lat : newnum;
        latTextField.setText(showSexagesimal ? decToSex(lat, 2, 90, true) : sixPlaces.format(lat));

        newnum = getLatLon(lonTextField);
        lon = Double.isNaN(newnum) ? lon : newnum;
        lonTextField.setText(showSexagesimal ? decToSex(lon, 2, 180, true) : sixPlaces.format(lon));

        newnum = IJU.getTextFieldDoubleValue(altTextField);
        alt = Double.isNaN(newnum) ? alt : newnum;
        altTextField.setText(uptoTwoPlaces.format(alt));
        }

    public void processTextFields(Object source)
        {
        if (source == raJ2000TextField || source == decJ2000TextField || source == eclLonJ2000TextField || source == eclLatJ2000TextField ||
            source == raB1950TextField || source == decB1950TextField || source == galLonB1950TextField || source == galLatB1950TextField ||
            source == raEOITextField || source == decEOITextField || source == eclLonEOITextField || source == eclLatEOITextField ||
            source == altEOITextField || source == azEOITextField )
            {
            clearActiveBox();
            if (validObjectID)
                {
                validObjectID = false;
                }
            else if (coordinateEntryEnabled)
                {
                if (!objectIDTextField.getText().trim().equals(""))
                    {
                    pmRATextField.setText("0");
                    pmDecTextField.setText("0");
                    pmRA = 0.0;
                    pmDec = 0.0;
                    }
                objectIDTextField.setText("");
                objectIDText = "";
                }
            }

        if (newradecJ2000 || source == raJ2000TextField || source == decJ2000TextField)
            {
            updateText = source == raJ2000TextField || source == decJ2000TextField;
            coords = processCoordinatePair(raJ2000TextField, 3, 24, false, decJ2000TextField, 2, 90, true, source==raJ2000TextField?true:false, updateText);
            if (!Double.isNaN(coords[0])) radecJ2000[0] = coords[0];
            if (!Double.isNaN(coords[1])) radecJ2000[1] = coords[1];
            newradecJ2000 = true;
            }
        else if (newelonlatJ2000 || source == eclLonJ2000TextField || source == eclLatJ2000TextField)
            {
            updateText = source == eclLonJ2000TextField || source == eclLatJ2000TextField;
            coords = processCoordinatePair(eclLonJ2000TextField, 2, 360, false, eclLatJ2000TextField, 2, 90, false, source==eclLonJ2000TextField?true:false, updateText);
            if (!Double.isNaN(coords[0])) elonlatJ2000[0] = coords[0];
            if (!Double.isNaN(coords[1])) elonlatJ2000[1] = coords[1];
            radecJ2000 = SkyAlgorithms.EclipticalToCelestial(jdJ2000, elonlatJ2000[0], elonlatJ2000[1], leapSec);
            newelonlatJ2000 = true;
            }
        else if (newradecB1950 || source == raB1950TextField || source == decB1950TextField)
            {
            updateText = source == raB1950TextField || source == decB1950TextField;
            coords = processCoordinatePair(raB1950TextField, 3, 24, false, decB1950TextField, 2, 90, true, source==raB1950TextField?true:false, updateText);
            if (!Double.isNaN(coords[0])) radecB1950[0] = coords[0];
            if (!Double.isNaN(coords[1])) radecB1950[1] = coords[1];
            radecJ2000 = SkyAlgorithms.B1950toJ2000(radecB1950[0], radecB1950[1], pmRA, pmDec, usePM);
            newradecB1950 = true;
            }
        else if (newglonlatB1950 || source == galLonB1950TextField || source == galLatB1950TextField)
            {
            updateText = source == galLonB1950TextField || source == galLatB1950TextField;
            coords = processCoordinatePair(galLonB1950TextField, 2, 360, false, galLatB1950TextField, 2, 90, false, source==galLonB1950TextField?true:false, updateText);
            if (!Double.isNaN(coords[0])) glonlatB1950[0] = coords[0];
            if (!Double.isNaN(coords[1])) glonlatB1950[1] = coords[1];
            radecJ2000 = SkyAlgorithms.GaltoJ2000(glonlatB1950[0], glonlatB1950[1], pmRA, pmDec, usePM);
            newglonlatB1950 = true;
            }

        else if(newradecEOI || source == raEOITextField || source == decEOITextField)
            {
            updateText = source == raEOITextField || source == decEOITextField;
            coords = processCoordinatePair(raEOITextField, 3, 24, false, decEOITextField, 2, 90, true, source==raEOITextField?true:false, updateText);
            if (!Double.isNaN(coords[0])) radecEOI[0] = coords[0];
            if (!Double.isNaN(coords[1])) radecEOI[1] = coords[1];
            radecJ2000 = SkyAlgorithms.Convert(2000.0, jdEOI, radecEOI[0], radecEOI[1], pmRA, pmDec, leapSec, REVERSE, usePM, usePrec, useNut, useAber);
            newradecEOI = true;
            }
        else if (newelonlatEOI || source == eclLonEOITextField || source == eclLatEOITextField)
            {
            updateText = source == eclLonEOITextField || source == eclLatEOITextField;
            coords = processCoordinatePair(eclLonEOITextField, 2, 360, false, eclLatEOITextField, 2, 90, false, source==eclLonEOITextField?true:false, updateText);
            if (!Double.isNaN(coords[0])) elonlatEOI[0] = coords[0];
            if (!Double.isNaN(coords[1])) elonlatEOI[1] = coords[1];
            radecEOI = SkyAlgorithms.EclipticalToCelestial(jdEOI, elonlatEOI[0], elonlatEOI[1], leapSec);
            radecJ2000 = SkyAlgorithms.Convert(2000.0, jdEOI, radecEOI[0], radecEOI[1], pmRA, pmDec, leapSec, REVERSE, usePM, usePrec, useNut, useAber);
            newelonlatEOI = true;
            }
        else if (newaltazEOI || source == altEOITextField || source == azEOITextField)
            {
            updateText = source == altEOITextField || source == azEOITextField;
            coords = processCoordinatePair(altEOITextField, 2, 90, false, azEOITextField, 2, 360, false, source==altEOITextField?true:false, updateText);
            if (!Double.isNaN(coords[0])) altazEOI[0] = coords[0];
            if (!Double.isNaN(coords[1])) altazEOI[1] = coords[1];
            radecEOI = SkyAlgorithms.HorizontalToCelestial(altazEOI[0], altazEOI[1], lat, lon, utDateEOI, leapSec, useRefr);
            radecJ2000 = SkyAlgorithms.Convert(2000.0, jdEOI, radecEOI[0], radecEOI[1], pmRA, pmDec, leapSec, REVERSE, usePM, usePrec, useNut, useAber);
            newaltazEOI = true;
            }


        computeCoordinates();
        }





void computeCoordinates()
        {
            running = true;
            getLatLonAlt();
            getProperMotion();

            if (!newradecJ2000 || optionChanged)
                {
                raJ2000TextField.setText(showSexagesimal ? decToSex(radecJ2000[0], 3, 24, false) : sixPlaces.format(radecJ2000[0]));
                decJ2000TextField.setText(showSexagesimal ? decToSex(radecJ2000[1], 2, 90, true) : sixPlaces.format(radecJ2000[1]));
                }
            if (!newelonlatJ2000 || optionChanged)
                {
                elonlatJ2000 = SkyAlgorithms.CelestialToEcliptical(jdJ2000, radecJ2000[0], radecJ2000[1], leapSec);
                eclLonJ2000TextField.setText(showSexagesimal ? decToSex(elonlatJ2000[0], 2 , 360,false) : sixPlaces.format(elonlatJ2000[0]));
                eclLatJ2000TextField.setText(showSexagesimal ? decToSex(elonlatJ2000[1], 2 , 90, false) : sixPlaces.format(elonlatJ2000[1]));
                }
            if (!newradecB1950 || optionChanged)
                {
                radecB1950 = SkyAlgorithms.J2000toB1950(radecJ2000[0], radecJ2000[1], pmRA, pmDec, usePM);
                raB1950TextField.setText(showSexagesimal ? decToSex(radecB1950[0], 3 , 24, false) : sixPlaces.format(radecB1950[0]));
                decB1950TextField.setText(showSexagesimal ? decToSex(radecB1950[1], 2 , 90,true) : sixPlaces.format(radecB1950[1]));
                }

            if (!newglonlatB1950 || optionChanged)
                {
                glonlatB1950 = SkyAlgorithms.J2000toGal(radecJ2000[0], radecJ2000[1], pmRA, pmDec, usePM);
                galLonB1950TextField.setText(showSexagesimal ? decToSex(glonlatB1950[0], 2 , 360, false) : sixPlaces.format(glonlatB1950[0]));
                galLatB1950TextField.setText(showSexagesimal ? decToSex(glonlatB1950[1], 2 , 90, false) : sixPlaces.format(glonlatB1950[1]));
                }

            utDateEOI = SkyAlgorithms.UTDateFromJD(jdEOI);
            epochEOI = 2000.0 + (jdEOI - 2451545.0)/365.25;
            lstEOI = SkyAlgorithms.CalcLST((int)utDateEOI[0], (int)utDateEOI[1], (int)utDateEOI[2], utDateEOI[3], lon, leapSec);
            eoiUTDateTextField.setText(((int)utDateEOI[0]<0 ? "" : " ")+fourDigits.format((int)utDateEOI[0])+"-"+twoDigits.format((int)utDateEOI[1])+"-"+twoDigits.format((int)utDateEOI[2]));
            eoiUTTimeTextField.setText(decToSex(utDateEOI[3],0, 24, false)+" UT");

            utc = GregorianCalendar.getInstance(TimeZone.getTimeZone("UTC"));
            utc.set(utc.ERA, (int)utDateEOI[0]<0?GregorianCalendar.BC:GregorianCalendar.AD);
            double mins = ((utDateEOI[3]-(int)utDateEOI[3])*60.0);
            int intmins = (int)((utDateEOI[3]-(int)utDateEOI[3])*60.0);
            int intsecs = Math.round((float)((mins - intmins)*60.0));
            utc.set(Math.abs((int)utDateEOI[0]), (int)utDateEOI[1]-1, (int)utDateEOI[2], (int)utDateEOI[3], intmins, intsecs);
            int offsetHours = (int)nowTimeZoneOffset;
            int offsetMins = (int)((nowTimeZoneOffset-(int)nowTimeZoneOffset)*60.0);
            local = new GregorianCalendar(autoTimeZone?TimeZone.getDefault():
                             TimeZone.getTimeZone("GMT"+(nowTimeZoneOffset<0?"-":"+")+twoDigits.format(Math.abs(offsetHours))+
                             ":"+twoDigits.format(Math.abs(offsetMins))));
            local.setTimeInMillis(utc.getTimeInMillis());
            eoiLocDateTextField.setText(""+(local.get(local.ERA)==GregorianCalendar.BC?"-":" ")+fourDigits.format(local.get(local.YEAR))+
                                        "-"+twoDigits.format(local.get(local.MONTH)+1)+"-"+twoDigits.format(local.get(local.DAY_OF_MONTH)));
            if (useAMPM)
                {
                eoiLocTimeTextField.setText(""+twoDigits.format(local.get(local.HOUR)==0?12:local.get(local.HOUR))+":"+
                                               twoDigits.format(local.get(local.MINUTE))+":"+
                                               twoDigits.format(local.get(local.SECOND))+
                                               (local.get(local.AM_PM)==local.AM?" AM":" PM"));
                }
            else
                {
                eoiLocTimeTextField.setText(""+twoDigits.format(local.get(local.HOUR_OF_DAY))+":"+
                                               twoDigits.format(local.get(local.MINUTE))+":"+
                                               twoDigits.format(local.get(local.SECOND)));
                }

            eoiJDTextField.setText(sixPlaces.format(jdEOI));
            eoiLSTTextField.setText(decToSex(lstEOI,0, 24, false));

            pmTwiJD = computeNauticalTwilight(jdEOI, leapSec, lat, lon, alt, true);
            pmTwiDate = SkyAlgorithms.UTDateFromJD(pmTwiJD);
            pmutc = GregorianCalendar.getInstance(TimeZone.getTimeZone("UTC"));
            pmutc.set(pmutc.ERA, (int)pmTwiDate[0]<0?GregorianCalendar.BC:GregorianCalendar.AD);
            mins = ((pmTwiDate[3]-(int)pmTwiDate[3])*60.0);
            intmins = (int)((pmTwiDate[3]-(int)pmTwiDate[3])*60.0);
            intsecs = Math.round((float)((mins - intmins)*60.0));
            pmutc.set(Math.abs((int)pmTwiDate[0]), (int)pmTwiDate[1]-1, (int)pmTwiDate[2], (int)pmTwiDate[3], intmins, intsecs);
            if (twiLocal)
                {
                Calendar pmlocal = new GregorianCalendar(autoTimeZone?TimeZone.getDefault():
                                 TimeZone.getTimeZone("GMT"+(nowTimeZoneOffset<0?"-":"+")+twoDigits.format(Math.abs(offsetHours))+
                                 ":"+twoDigits.format(Math.abs(offsetMins))));
                pmlocal.setTimeInMillis(pmutc.getTimeInMillis());
                if (useAMPM)
                    {
                    eoiPMTwilightTextField.setText(""+twoDigits.format(pmlocal.get(pmlocal.HOUR)==0?12:pmlocal.get(pmlocal.HOUR))+":"+
                                                   twoDigits.format(pmlocal.get(pmlocal.MINUTE) + pmlocal.get(pmlocal.SECOND)/60.0)+
                                                   (pmlocal.get(pmlocal.AM_PM)==pmlocal.AM?" AM":" PM"));
                    }
                else
                    {
                    eoiPMTwilightTextField.setText(""+twoDigits.format(pmlocal.get(pmlocal.HOUR_OF_DAY))+":"+
                                                   twoDigits.format(pmlocal.get(pmlocal.MINUTE) + pmlocal.get(pmlocal.SECOND)/60.0));
                    }
                }
            else
                {
                eoiPMTwilightTextField.setText(""+twoDigits.format(pmutc.get(pmutc.HOUR_OF_DAY))+":"+
                                               twoDigits.format(pmutc.get(pmutc.MINUTE) + pmutc.get(pmutc.SECOND)/60.0)+" UT");
                }



            amTwiJD = computeNauticalTwilight(jdEOI, leapSec, lat, lon, alt, false);
            amTwiDate = SkyAlgorithms.UTDateFromJD(amTwiJD);
            amutc = GregorianCalendar.getInstance(TimeZone.getTimeZone("UTC"));
            amutc.set(amutc.ERA, (int)amTwiDate[0]<0?GregorianCalendar.BC:GregorianCalendar.AD);
            mins = ((amTwiDate[3]-(int)amTwiDate[3])*60.0);
            intmins = (int)((amTwiDate[3]-(int)amTwiDate[3])*60.0);
            intsecs = Math.round((float)((mins - intmins)*60.0));
            amutc.set(Math.abs((int)amTwiDate[0]), (int)amTwiDate[1]-1, (int)amTwiDate[2], (int)amTwiDate[3], intmins, intsecs);
            if (twiLocal)
                {
                Calendar amlocal = new GregorianCalendar(autoTimeZone?TimeZone.getDefault():
                                 TimeZone.getTimeZone("GMT"+(nowTimeZoneOffset<0?"-":"+")+twoDigits.format(Math.abs(offsetHours))+
                                 ":"+twoDigits.format(Math.abs(offsetMins))));
                amlocal.setTimeInMillis(amutc.getTimeInMillis());
                if (useAMPM)
                    {
                    eoiAMTwilightTextField.setText(""+twoDigits.format(amlocal.get(amlocal.HOUR)==0?12:amlocal.get(amlocal.HOUR))+":"+
                                                   twoDigits.format(amlocal.get(amlocal.MINUTE)+amlocal.get(amlocal.SECOND)/60.0)+
                                                   (amlocal.get(amlocal.AM_PM)==amlocal.AM?" AM":" PM"));
                    }
                else
                    {
                    eoiAMTwilightTextField.setText(""+twoDigits.format(amlocal.get(amlocal.HOUR_OF_DAY))+":"+
                                                   twoDigits.format(amlocal.get(amlocal.MINUTE)+amlocal.get(amlocal.SECOND)/60.0));
                    }
                }
            else
                {
                eoiAMTwilightTextField.setText(""+twoDigits.format(amutc.get(amutc.HOUR_OF_DAY))+":"+
                                               twoDigits.format(amutc.get(amutc.MINUTE) + amutc.get(amutc.SECOND)/60.0)+" UT");
                }





            if (autoLeapSec)
                {
                if (!getTAIminusUTC())
                    {
                    showMessage("Leap second data corrupt, restoring default leap second data");
                    getTAIminusUTC(); //try a second time with default leap second arrays if current leap second arrays are null or different lengths
                    }
                leapSecTextField.setText(onePlaces.format(leapSec));
                }

            if (!newradecEOI || optionChanged)
                {
                radecEOI = SkyAlgorithms.Convert(2000.0, jdEOI, radecJ2000[0], radecJ2000[1], pmRA, pmDec, leapSec, FORWARD, usePM, usePrec, useNut, useAber);
                raEOITextField.setText(showSexagesimal ? decToSex(radecEOI[0], 3, 24, false) : sixPlaces.format(radecEOI[0]));
                decEOITextField.setText(showSexagesimal ? decToSex(radecEOI[1], 2, 90, true) : sixPlaces.format(radecEOI[1]));
                }
            if (!newelonlatEOI || optionChanged)
                {
                elonlatEOI = SkyAlgorithms.CelestialToEcliptical(jdEOI, radecEOI[0], radecEOI[1], leapSec);
                eclLonEOITextField.setText(showSexagesimal ? decToSex(elonlatEOI[0], 2 , 360,false) : sixPlaces.format(elonlatEOI[0]));
                eclLatEOITextField.setText(showSexagesimal ? decToSex(elonlatEOI[1], 2 , 90, false) : sixPlaces.format(elonlatEOI[1]));
                }
            if (!newaltazEOI || optionChanged)
                {
                altazEOI = SkyAlgorithms.CelestialToHorizontal(radecEOI[0], radecEOI[1], lat, lon, utDateEOI, leapSec, useRefr);
                altEOITextField.setText(showSexagesimal ? decToSex(altazEOI[0], 2 , 90, false) : sixPlaces.format(altazEOI[0]));
                azEOITextField.setText(showSexagesimal ? decToSex(altazEOI[1], 2, 360, false) : sixPlaces.format(altazEOI[1]));
                }
            hazdEOI[0] = (lstEOI-radecEOI[0])%24;
            hazdEOI[0] = hazdEOI[0]>0 ? (hazdEOI[0]>12 ? -24+hazdEOI[0]:hazdEOI[0]) : (hazdEOI[0]<-12 ? 24+hazdEOI[0]:hazdEOI[0]);
            hazdEOI[1] = 90 - altazEOI[0];
            haEOITextField.setText(showSexagesimal ? decToSex(hazdEOI[0], 0 , 12, false) : sixPlaces.format(hazdEOI[0]));
            zdEOITextField.setText(showSexagesimal ? decToSex(hazdEOI[1], 0, 180, false) : sixPlaces.format(hazdEOI[1]));
            if      (altazEOI[1]>=337.5 || altazEOI[1]< 22.5) dirTextField.setText("N");
            else if (altazEOI[1]>= 22.5 && altazEOI[1]< 67.5) dirTextField.setText("NE");
            else if (altazEOI[1]>= 67.5 && altazEOI[1]<112.5) dirTextField.setText("E");
            else if (altazEOI[1]>=112.5 && altazEOI[1]<157.5) dirTextField.setText("SE");
            else if (altazEOI[1]>=157.5 && altazEOI[1]<202.5) dirTextField.setText("S");
            else if (altazEOI[1]>=202.5 && altazEOI[1]<247.5) dirTextField.setText("SW");
            else if (altazEOI[1]>=247.5 && altazEOI[1]<292.5) dirTextField.setText("W");
            else if (altazEOI[1]>=292.5 && altazEOI[1]<337.5) dirTextField.setText("NW");

            airmass = getAirmass(altazEOI[0]);
            airmassTextField.setText(airmass>0.0?(airmass<10.0?fourPlaces.format(airmass):onePlaces.format(airmass)):"N/A");

            dT=leapSec+32.184;
            g = (357.53+0.9856003*(jdEOI-2451545.0))/DEG_IN_RADIAN;
            TDB=jdEOI+(dT+0.001658*Math.sin(g)+0.000014*Math.sin(2*g))/86400.0;

            hjdEOICorr = calculateHJDCorrection(jdEOI, dT, radecEOI[0], radecEOI[1], lstEOI, lat, alt);
            hjdEOI = jdEOI + hjdEOICorr;
            eoiHJDTextField.setText(sixPlaces.format(hjdEOI));
            eoiHJDdTTextField.setText(decToSex((hjdEOICorr)*24.0, 0, 24, false));

            if (useOhioState && !useNowEpoch && !spinnerActive)
                bjdEOI = getBJDTDB(jdEOI, radecJ2000[0], radecJ2000[1]);
            else
                bjdEOI = TDB + calculateBJDCorrection(jdEOI, dT, radecEOI[0], radecEOI[1], lstEOI, lat, alt);
            bjdEOICorr = bjdEOI-jdEOI;
            eoiBJDTextField.setText(sixPlaces.format(bjdEOI));
            eoiBJDdTTextField.setText(decToSex((bjdEOI-jdEOI)*24.0, 0, 24, false));

//            IJ.log("pmTwiJD="+pmTwiJD+"     jdEOI="+jdEOI+"    amTwiJD"+amTwiJD);

            if ((pmTwiJD%1.0 < amTwiJD%1.0 && jdEOI%1.0 >= pmTwiJD%1.0 && jdEOI%1.0 <= amTwiJD%1.0) ||
                (pmTwiJD%1.0 > amTwiJD%1.0 && (jdEOI%1.0 >= pmTwiJD%1.0 || jdEOI%1.0 <= amTwiJD%1.0)))
                {
                eoiAMTwilightTextField.setBackground(leapGreen);
                eoiPMTwilightTextField.setBackground(leapGreen);
                }
            else
                {
                eoiAMTwilightTextField.setBackground(leapGray);
                eoiPMTwilightTextField.setBackground(leapGray);
                }

            moonPhase = getMoonPhase(jdEOI);
            if (moonPhase < 0) moonPhase = 0;
            if (moonPhase > 55) moonPhase = 55;
            moonPhaseLabel.setIcon(moonIcon[moonPhase]);

            ssbAngles = radecToSSBodyAngles(radecEOI, jdEOI, utDateEOI, dT, lon, lat, alt, lstEOI);
            updateSSBFields();

            optionChanged = false;
            running = false;
            }


void updateSSBFields()
            {

            for(int i : new int[]{2,0,1,3,4,5,6,7,8})
                {
                if (reportSSBDown && ssbAngles[i][1] < 0)
                    {
                    ssbAltitudeFields[i].setText("Down");
                    }
                else
                    {
                    ssbAltitudeFields[i].setText(twoPlaces.format(ssbAngles[i][1]));
                    }
                ssbDistanceFields[i].setText(twoPlaces.format(ssbAngles[i][0]));
                if (ssbAngles[i][0] < warning[i])
                    {
                    ssbPanels[i].setBackground(leapRed);
                    ssbAltitudeFields[i].setBackground(leapRed);
                    ssbDistanceFields[i].setBackground(leapRed);
                    }
                else if (ssbAngles[i][0] < caution[i])
                    {
                    ssbPanels[i].setBackground(leapYellow);
                    ssbAltitudeFields[i].setBackground(leapYellow);
                    ssbDistanceFields[i].setBackground(leapYellow);
                    }
                else
                    {
                    ssbPanels[i].setBackground(leapGreen);
                    ssbAltitudeFields[i].setBackground(leapGreen);
                    ssbDistanceFields[i].setBackground(leapGreen);
                    }
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
        textFieldA.setBorder(greenBorder);
        Y = mapToBase(Y, baseB, YNegative);
        if (!Double.isNaN(Y) && update) textFieldB.setText(showSexagesimal ? decToSex(Y, decimalPlacesB, baseB, showSignB) : sixPlaces.format(Y));
        textFieldB.setBorder(greenBorder);
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

    String convertToText(int increment, int factor)
                {
                if      (increment == GregorianCalendar.SECOND)
                    return "      1 second";
                else if (increment == GregorianCalendar.MINUTE && factor == 1)
                    return "      1 minute";
                else if (increment == GregorianCalendar.MINUTE && factor == 10)
                    return "      10 minutes";
                else if (increment == GregorianCalendar.HOUR_OF_DAY)
                    return "          1 hour";
                else if (increment == GregorianCalendar.DAY_OF_MONTH && factor == 1)
                    return "             1 day";
                else if (increment == GregorianCalendar.DAY_OF_MONTH && factor == 7)
                    return "           1 week";
                else if (increment == GregorianCalendar.MONTH)
                    return "         1 month";
                else if (increment == GregorianCalendar.YEAR && factor == 1)
                    return "       1 year";
                else if (increment == GregorianCalendar.YEAR && factor == 10)
                    return "     10 years";
                else if (increment == GregorianCalendar.YEAR && factor == 100)
                    return "   100 years";
                else if (increment == GregorianCalendar.YEAR && factor == 1000)
                    return " 1000 years";
                else
                    return "             1 day";

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

    public boolean getTAIminusUTC()
    {
        leapSec = 0.0;
//        leapSource = " (default)";
        leapSecTextField.setBackground(leapGreen);
        if (leapSecJD == null || TAIminusUTC == null || baseMJD == null || baseMJDMultiplier == null ||
            leapSecJD.length < 2 || TAIminusUTC.length < 2 || baseMJD.length < 2 || baseMJDMultiplier.length < 2 ||
            leapSecJD.length != TAIminusUTC.length || leapSecJD.length != baseMJD.length || leapSecJD.length != baseMJDMultiplier.length)
            {
            initDefaultLeapSecs();
            return false;
            }
        if (jdEOI<leapSecJD[0])
            {
            estimateLeapSecs();
            return true;
            }
        for (int i=1; i<leapSecJD.length;i++)
            {
            if (jdEOI>=leapSecJD[i-1] && jdEOI<leapSecJD[i])
                {
                leapSec = TAIminusUTC[i-1] + (jdEOI - 2400000 - baseMJD[i-1])*baseMJDMultiplier[i-1];
//                leapSource = "(actual)";
                leapSecTextField.setBackground(leapGreen);
                return true;
                }
            }
        if (jdEOI>=leapSecJD[leapSecJD.length-1] && jdEOI <= jdNow+365.0)
            {
            leapSec = TAIminusUTC[TAIminusUTC.length-1] + (jdEOI - 2400000 - baseMJD[TAIminusUTC.length-1])*baseMJDMultiplier[TAIminusUTC.length-1];
//            leapSource = "(actual)";
            leapSecTextField.setBackground(leapGreen);
            return true;
            }
        estimateLeapSecs();
        return true;
    }


    void estimateLeapSecs()
    {
        double y = utDateEOI[0] +(utDateEOI[1]-0.5)/12.0;
        double u = 0.0;
        double dt = 0.0;
        leapSec = 0.0;
//        leapSource = "(estimate)";
        leapSecTextField.setBackground(leapYellow);
        if (y<-1999)
            {
//            leapSource = "(out of range)";
            leapSecTextField.setBackground(leapRed);
            y = -1999;
            u = (y-1820)/100;
            dt = -20 + 32 * u*u;
            }
        else if(y >= -1999 && y < -500.0)
            {
            u = (y-1820)/100;
            dt = -20 + 32 * u*u;
            }
        else if (y>=-500 && y<500.0)
            {
            u = y/100;
            double u2 = u*u;
            double u3 = u2*u;
            double u4 = u3*u;
            double u5 = u4*u;
            double u6 = u5*u;
            dt = 10583.6 - 1014.41 * u + 33.78311 * u2 - 5.952053 * u3 - 0.1798452 * u4 + 0.022174192 * u5 + 0.0090316521 * u6;
            }
        else if (y>=500 && y<1600.0)
            {
            u = (y-1000)/100;
            double u2 = u*u;
            double u3 = u2*u;
            double u4 = u3*u;
            double u5 = u4*u;
            double u6 = u5*u;
            dt = 1574.2 - 556.01 * u + 71.23472 * u2 + 0.319781 * u3 - 0.8503463 * u4 - 0.005050998 * u5 + 0.0083572073 * u6;
            }
        else if (y>=1600 && y<1700.0)
            {
            u = y - 1600;
            double u2 = u*u;
            double u3 = u2*u;
            dt = 120 - 0.9808 * u - 0.01532 * u2 + u3 / 7129;
            }
        else if (y>=1700 && y<1800.0)
            {
            u = y - 1700;
            double u2 = u*u;
            double u3 = u2*u;
            double u4 = u3*u;
            dt = 8.83 + 0.1603 * u - 0.0059285 * u2 + 0.00013336 * u3 - u4 / 1174000;
            }
        else if (y>=1800 && y<1860.0)
            {
            u = y - 1800;
            double u2 = u*u;
            double u3 = u2*u;
            double u4 = u3*u;
            double u5 = u4*u;
            double u6 = u5*u;
            double u7 = u6*u;
            dt = 13.72 - 0.332447 * u + 0.0068612 * u2 + 0.0041116 * u3 - 0.00037436 * u4 + 0.0000121272 * u5 - 0.0000001699 * u6 + 0.000000000875 * u7;
            }
        else if (y>=1860 && y<1900.0)
            {
            u = y - 1860;
            double u2 = u*u;
            double u3 = u2*u;
            double u4 = u3*u;
            double u5 = u4*u;
            dt = 7.62 + 0.5737 * u - 0.251754 * u2 + 0.01680668 * u3 -0.0004473624 * u4 + u5 / 233174;
            }
        else if (y>=1900 && y<1920.0)
            {
            u = y - 1900;
            double u2 = u*u;
            double u3 = u2*u;
            double u4 = u3*u;
            dt = -2.79 + 1.494119 * u - 0.0598939 * u2 + 0.0061966 * u3 - 0.000197 * u4;
            }
        else if (y>=1920 && y<1941.0)
            {
            u = y - 1920;
            double u2 = u*u;
            double u3 = u2*u;
            dt = 21.20 + 0.84493*u - 0.076100 * u2 + 0.0020936 * u3;
            }
        else if (y>=1941 && y<1961.0)
            {
            u = y - 1950;
            double u2 = u*u;
            double u3 = u2*u;
            dt = 29.07 + 0.407*u - u2/233 + u3 / 2547;
            }
        else if (y>=1961 && y<1986.0)
            {
            u = y - 1975;
            double u2 = u*u;
            double u3 = u2*u;
            dt = 45.45 + 1.067*u - u2/260 - u3 / 718;
            }
        else if (y>=1986 && y<2005.0)
            {
            u = y - 2000;
            double u2 = u*u;
            double u3 = u2*u;
            double u4 = u3*u;
            double u5 = u4*u;
            dt = 63.86 + 0.3345 * u - 0.060374 * u2 + 0.0017275 * u3 + 0.000651814 * u4 + 0.00002373599 * u5;
            }
        else if (y>=2005 && y<2050.0)
            {
            u = y - 2000;
            double u2 = u*u;
            dt = 62.92 + 0.32217 * u + 0.005589 * u2;
            }
        else if (y>=2050 && y<2150.0)
            {
            dt = -20 + 32 * ((y-1820)/100)*((y-1820)/100) - 0.5628 * (2150 - y);
            }
        else if (y>=2150 && y<3000.0)
            {
            u = (y-1820)/100;
            double u2 = u*u;
            dt = -20 + 32 * u2;
            }
        else
            {
            y=3000.0;
            u = (y-1820)/100;
            double u2 = u*u;
            dt = -20 + 32 * u2;
//            leapSource = "(out of range)";
            leapSecTextField.setBackground(leapRed);
            }
        leapSec = dt - 32.184;
    }


    void initDefaultLeapSecs()
    {
    leapSecJD = new Double[]
                            {
                            2437300.5,
                            2437512.5,
                            2437665.5,
                            2438334.5,
                            2438395.5,
                            2438486.5,
                            2438639.5,
                            2438761.5,
                            2438820.5,
                            2438942.5,
                            2439004.5,
                            2439126.5,
                            2439887.5,
                            2441317.5,
                            2441499.5,
                            2441683.5,
                            2442048.5,
                            2442413.5,
                            2442778.5,
                            2443144.5,
                            2443509.5,
                            2443874.5,
                            2444239.5,
                            2444786.5,
                            2445151.5,
                            2445516.5,
                            2446247.5,
                            2447161.5,
                            2447892.5,
                            2448257.5,
                            2448804.5,
                            2449169.5,
                            2449534.5,
                            2450083.5,
                            2450630.5,
                            2451179.5,
                            2453736.5,
                            2454832.5,
                            2456109.5,
                            2457204.5,
                            2457754.5
                            };

    TAIminusUTC =  new Double[]
                            {
                             1.4228180,
                             1.3728180,
                             1.8458580,
                             1.9458580,
                             3.2401300,
                             3.3401300,
                             3.4401300,
                             3.5401300,
                             3.6401300,
                             3.7401300,
                             3.8401300,
                             4.3131700,
                             4.2131700,
                            10.0,
                            11.0,
                            12.0,
                            13.0,
                            14.0,
                            15.0,
                            16.0,
                            17.0,
                            18.0,
                            19.0,
                            20.0,
                            21.0,
                            22.0,
                            23.0,
                            24.0,
                            25.0,
                            26.0,
                            27.0,
                            28.0,
                            29.0,
                            30.0,
                            31.0,
                            32.0,
                            33.0,
                            34.0,
                            35.0,
                            36.0,
                            37.0
                            };



    baseMJD =  new Double[]
                            {
                            37300.0,
                            37300.0,
                            37665.0,
                            37665.0,
                            38761.0,
                            38761.0,
                            38761.0,
                            38761.0,
                            38761.0,
                            38761.0,
                            38761.0,
                            39126.0,
                            39126.0,
                            41317.0,
                            41317.0,
                            41317.0,
                            41317.0,
                            41317.0,
                            41317.0,
                            41317.0,
                            41317.0,
                            41317.0,
                            41317.0,
                            41317.0,
                            41317.0,
                            41317.0,
                            41317.0,
                            41317.0,
                            41317.0,
                            41317.0,
                            41317.0,
                            41317.0,
                            41317.0,
                            41317.0,
                            41317.0,
                            41317.0,
                            41317.0,
                            41317.0,
                            41317.0,
                            41317.0,
                            41317.0
                            };

    baseMJDMultiplier =  new Double[]
                            {
                            0.001296,
                            0.001296,
                            0.0011232,
                            0.0011232,
                            0.001296,
                            0.001296,
                            0.001296,
                            0.001296,
                            0.001296,
                            0.001296,
                            0.001296,
                            0.002592,
                            0.002592,
                            0.0,
                            0.0,
                            0.0,
                            0.0,
                            0.0,
                            0.0,
                            0.0,
                            0.0,
                            0.0,
                            0.0,
                            0.0,
                            0.0,
                            0.0,
                            0.0,
                            0.0,
                            0.0,
                            0.0,
                            0.0,
                            0.0,
                            0.0,
                            0.0,
                            0.0,
                            0.0,
                            0.0,
                            0.0,
                            0.0,
                            0.0,
                            0.0
                            };

    }

    void displayLeapSecondTable(boolean update)
    {
        String message = "";
        if (update)
            {
            message += "<html>LEAP SECOND TABLE SUCCESSFULLY UPDATED<br><br>ROW&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;JD"+
                       "&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;TAI-UTC<br>";
            }
        else
            {
            message += "<html>LEAP SECOND TABLE<br><br>ROW&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;JD"+
                       "&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;TAI-UTC<br>";
            }
        for (int i=0; i<leapSecJD.length;i++)
            {
            message += ""+(i+1);
            if (i<9) message += "&nbsp;&nbsp;";
            message += "&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;"+leapSecJD[i]+"&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;"+TAIminusUTC[i]+
                       " + (MJD - "+baseMJD[i]+") X "+baseMJDMultiplier[i]+"<br>";
            }
        message += "</html>";
        showMessage("Leap Second Table",message);
    }

    void clearActiveBox()
        {
        newradecJ2000 = false;
        newelonlatJ2000 = false;
        newradecB1950 = false;
        newglonlatB1950 = false;
        newradecEOI = false;
        newelonlatEOI = false;
        newaltazEOI = false;
        raJ2000TextField.setBorder(grayBorder);
        decJ2000TextField.setBorder(grayBorder);
        eclLonJ2000TextField.setBorder(grayBorder);
        eclLatJ2000TextField.setBorder(grayBorder);
        raB1950TextField.setBorder(grayBorder);
        decB1950TextField.setBorder(grayBorder);
        galLonB1950TextField.setBorder(grayBorder);
        galLatB1950TextField.setBorder(grayBorder);
        raEOITextField.setBorder(grayBorder);
        decEOITextField.setBorder(grayBorder);
        eclLonEOITextField.setBorder(grayBorder);
        eclLatEOITextField.setBorder(grayBorder);
        altEOITextField.setBorder(grayBorder);
        azEOITextField.setBorder(grayBorder);
        }

    void enableLonLatAlt(boolean enabled)
        {
        lonTextField.setEnabled(observatoryIDComboBox.isEnabled() && enabled);
        lonTextField.setBackground(observatoryIDComboBox.isEnabled() && enabled?Color.WHITE:leapGray);
        latTextField.setEnabled(observatoryIDComboBox.isEnabled() && enabled);
        latTextField.setBackground(observatoryIDComboBox.isEnabled() && enabled?Color.WHITE:leapGray);
        altTextField.setEnabled(observatoryIDComboBox.isEnabled() && enabled);
        altTextField.setBackground(observatoryIDComboBox.isEnabled() && enabled?Color.WHITE:leapGray);
        }

    void updateJDEOI(int step, int factor, boolean setSpinnerActive)
        {
        utc.add(step, factor);
        jdEOI = SkyAlgorithms.CalcJD((utc.get(GregorianCalendar.YEAR)+(utc.get(GregorianCalendar.ERA)==GregorianCalendar.BC?-1:0))*
                                         (utc.get(GregorianCalendar.ERA)==GregorianCalendar.BC?-1:1),
                                     utc.get(GregorianCalendar.MONTH)+1,
                                     utc.get(GregorianCalendar.DAY_OF_MONTH),
                                     utc.get(GregorianCalendar.HOUR_OF_DAY) + utc.get(GregorianCalendar.MINUTE)/60.0 +
                                         utc.get(GregorianCalendar.SECOND)/3600.0);
        if (jdEOI < 0) jdEOI = 0.0;
        eoiJDTextField.setText(sixPlaces.format(jdEOI));
        spinnerActive = setSpinnerActive?true:false;
        processAction(null);
        spinnerActive = false;
        }

    double getLatLon(JTextField textField)
        {
        double value = Double.NaN;
        boolean isNegative = false;
        String text = textField.getText().trim();
        String[] pieces = text.split("[0-9\\.]{1,}");
        if (pieces.length > 0 && pieces[0].endsWith("-")) isNegative = true;
        if (text.toUpperCase().contains("S")||text.toUpperCase().contains("W")) isNegative = !isNegative;
        pieces = text.replaceAll("[^0-9\\.]{1,}", " ").trim().split("[^0-9\\.]{1,}");
        try {if (pieces.length > 0) value = Double.parseDouble(pieces[0]);}
        catch (NumberFormatException e) { }
        try {if (pieces.length > 1) value += Double.parseDouble(pieces[1])/60.0;}
        catch (NumberFormatException e) { }
        try {if (pieces.length > 2) value += Double.parseDouble(pieces[2])/3600.0;}
        catch (NumberFormatException e) { }
        if (textField.equals(latTextField))
            value = value>90 ? 90.0 : value;
        else  //longitude
            {
            value %= 360;
            value = value>180.0 ? -360.0+value : value;
            }
        if (isNegative) value = -value;
        return value;
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
//        String secString="";
//        secString = nf2x.format(s);
//        if (Tools.parseDouble(secString) >= 60.0)  //correct rounding issues when converting to dms format
//            {
//            if      (fractionPlaces == 0) secString = "59";
//            else if (fractionPlaces == 1) secString = "59.9";
//            else if (fractionPlaces == 2) secString = "59.99";
//            else if (fractionPlaces == 3) secString = "59.999";
//            else if (fractionPlaces == 4) secString = "59.9999";
//            else if (fractionPlaces == 5) secString = "59.99999";
//            else if (fractionPlaces == 6) secString = "59.999999";
//            else if (fractionPlaces == 7) secString = "59.9999999";
//            else if (fractionPlaces == 8) secString = "59.99999999";
//            else if (fractionPlaces == 9) secString = "59.999999999";
//            else if (fractionPlaces == 10)secString = "59.9999999999";
//            }



        if (Tools.parseDouble(nf2x.format(s)) >= 60.0)
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



    protected double[] getEOIDateAndTime(boolean local)
        {
        double[] dateEOI;


        String date = "";
        String time = "";
        if (local)
            {
            date = eoiLocDateTextField.getText().trim();
            time = eoiLocTimeTextField.getText().trim();
            dateEOI = locDateEOI;
            }
        else
            {
            date = eoiUTDateTextField.getText().trim();
            time = eoiUTTimeTextField.getText().trim();
            dateEOI = utDateEOI;
            }
        double year, mon, day;
        double hour, min, sec;
        String[] datepieces = date.replace("-", " -").replaceAll("[^0-9\\.\\-]{1,}", " ").trim().split("[^0-9\\.\\-]{1,}");
        String[] timepieces = time.replaceAll("[^0-9\\.]{1,}", " ").trim().split("[^0-9\\.]{1,}");
        if (datepieces.length == 0 || timepieces.length == 0)
            return dateEOI;
        else
            {
            year = Tools.parseDouble(datepieces[0]);
            hour = Math.abs(Tools.parseDouble(timepieces[0]));
            if (Double.isNaN(year) || Double.isNaN(hour)) return dateEOI;
            }
        dateEOI[0] = year < 0 ? (int)(year+1) : (int)year;
        dateEOI[1] = 1;
        dateEOI[2] = 1;
        if (datepieces.length > 1)
            {
            mon = Math.abs(Math.abs(Tools.parseDouble(datepieces[1])) - 1);
            dateEOI[1] = Double.isNaN(mon) ? 1 : (int)(mon%12 + 1);
            }
        if (datepieces.length > 2)
            {
            day = Math.abs(Math.abs(Tools.parseDouble(datepieces[2])) - 1);
            dateEOI[2] = Double.isNaN(day) ? 1 : (int)(day%31 + 1);
            }


        if (timepieces.length == 1)
            {
            if (hour < 1.0)
                {
                dateEOI[3] = hour*24.0;   //assume fraction of days entered
                }
            else
                {
                if (local && useAMPM && (hour >= 12) && (hour < 13) && (!time.contains("p") && !time.contains("P")))
                    {
                    dateEOI[3] = hour - 12.0;
                    }
                else if (local && useAMPM && (hour < 12.0) && (time.contains("p") || time.contains("P")))
                    {
                    dateEOI[3] = hour + 12.0;
                    }
                else
                    {
                    dateEOI[3] = hour%24;     //assume hours.hours entered
                    }
                }
            return dateEOI;
            }

        dateEOI[3]=0;
        if (timepieces.length > 2)
            {
            sec = Math.abs(Tools.parseDouble(timepieces[2]));
            dateEOI[3] += Double.isNaN(sec) ? 0 : sec/3600.0;
            }
        if (timepieces.length > 1)
            {
            min = Math.abs(Tools.parseDouble(timepieces[1]));
            dateEOI[3] += Double.isNaN(min) ? 0 : min/60.0;
            }
        dateEOI[3] += hour;
        if (local && useAMPM && (hour >= 12) && (hour < 13) && (!time.contains("p") && !time.contains("P")))
            {
            dateEOI[3] -= 12.0;
            }
        else if (local && useAMPM && (hour < 12.0) && (time.contains("p") || time.contains("P")))
            {
            dateEOI[3] += 12.0;
            }
        dateEOI[3] %= 24;
        return dateEOI;
        }

    protected double localEOIDateToJD(double[] locDateEOI)
        {
        double[] utEOI = new double[4];
        int offsetHours = (int)nowTimeZoneOffset;
        int offsetMins = (int)((nowTimeZoneOffset-(int)nowTimeZoneOffset)*60.0);
        Calendar local = new GregorianCalendar(autoTimeZone?TimeZone.getDefault():
                         TimeZone.getTimeZone("GMT"+(nowTimeZoneOffset<0?"-":"+")+twoDigits.format(Math.abs(offsetHours))+
                         ":"+twoDigits.format(Math.abs(offsetMins))));
        local.set(GregorianCalendar.ERA, (int)locDateEOI[0]<0?GregorianCalendar.BC:GregorianCalendar.AD);
        double mins = ((locDateEOI[3]-(int)locDateEOI[3])*60.0);
        int intmins = (int)mins;
        float secs = ((float)mins - intmins)*60.0f;
        int intsecs = (int)secs;
        int intmillis = (int)(secs - intsecs)*1000;
        local.set(Math.abs((int)locDateEOI[0]), (int)locDateEOI[1]-1, (int)locDateEOI[2], (int)locDateEOI[3], intmins, Math.round(secs));
//        local.set(GregorianCalendar.MILLISECOND, intmillis);

        Calendar utc = GregorianCalendar.getInstance(TimeZone.getTimeZone("UTC"));
        utc.setTimeInMillis(local.getTimeInMillis());

        utEOI[0] = utc.get(utc.YEAR);
        if (utc.get(GregorianCalendar.ERA)==GregorianCalendar.BC) utEOI[0] *= -1;
        utEOI[1] = utc.get(GregorianCalendar.MONTH)+1;
        utEOI[2] = utc.get(GregorianCalendar.DAY_OF_MONTH);
        utEOI[3] = utc.get(GregorianCalendar.HOUR_OF_DAY) + utc.get(GregorianCalendar.MINUTE)/60.0 +
                   utc.get(GregorianCalendar.SECOND)/3600.0;// + utc.get(GregorianCalendar.MILLISECOND)/3600000.0;
        return SkyAlgorithms.CalcJD(utEOI);
        }

    protected void updateTimeEditable()
        {
        boolean enable = !useNowEpoch && timeEnabled;
        eoiUTDateTextField.setEditable(enable);
        eoiUTDateTextField.setBackground(enable?Color.WHITE:leapGray);
        eoiUTTimeTextField.setEditable(enable);
        eoiUTTimeTextField.setBackground(enable?Color.WHITE:leapGray);
        eoiLocDateTextField.setEditable(enable);
        eoiLocDateTextField.setBackground(enable?Color.WHITE:leapGray);
        eoiLocTimeTextField.setEditable(enable);
        eoiLocTimeTextField.setBackground(enable?Color.WHITE:leapGray);
        eoiJDTextField.setEditable(enable);
        eoiJDTextField.setBackground(enable?Color.WHITE:leapGray);
        jdEOIupButton.setEnabled(enable);
        jdLocEOIupButton.setEnabled(enable);
        jdEOIdownButton.setEnabled(enable);
        jdLocEOIdownButton.setEnabled(enable);
        eoiHJDTextField.setEditable(enable);
        eoiHJDTextField.setBackground(enable?Color.WHITE:leapGray);
        eoiBJDTextField.setEditable(enable);
        eoiBJDTextField.setBackground(enable?Color.WHITE:leapGray);
        pmTwilightButton.setEnabled(enable);
        amTwilightButton.setEnabled(enable);
        nowEOIButton.setEnabled(enable);
        useOhioStateCheckBox.setEnabled(!useNowEpoch);
        OSULabel.setEnabled(!useNowEpoch);
        useOhioStateCB.setEnabled(!useNowEpoch);
        }


   protected void wait(int millis)
        {
        try
           {
           Thread.sleep(millis);
           }
        catch (InterruptedException ex) {}
        }


    /** Returns an ImageIcon, or null if the path was invalid. */
    protected ImageIcon createImageIcon(String path, String description) {
        java.net.URL imgURL = getClass().getClassLoader().getResource(path);
        if (imgURL != null) {
            return new ImageIcon(imgURL, description);
        } else {
            showMessage("Couldn't find icon file: " + path);
            return null;
        }
    }

    boolean checkSSObjects()
        {
        ssObject = false;
        if (objectIDTextField.getText().trim().equalsIgnoreCase("moon") || objectIDTextField.getText().trim().equalsIgnoreCase("mercury") ||
            objectIDTextField.getText().trim().equalsIgnoreCase("venus") || objectIDTextField.getText().trim().equalsIgnoreCase("mars") ||
            objectIDTextField.getText().trim().equalsIgnoreCase("jupiter") || objectIDTextField.getText().trim().equalsIgnoreCase("saturn") ||
            objectIDTextField.getText().trim().equalsIgnoreCase("uranus") || objectIDTextField.getText().trim().equalsIgnoreCase("neptune") ||
            objectIDTextField.getText().trim().equalsIgnoreCase("pluto") || objectIDTextField.getText().trim().equalsIgnoreCase("sun"))
            {
            utDateEOI = SkyAlgorithms.UTDateFromJD(jdEOI);
            if (autoLeapSec)
                {
                if (!getTAIminusUTC())
                    {
                    showMessage("Leap second data corrupt, restoring default leap second data");
                    getTAIminusUTC(); //try a second time with default leap second arrays if current leap second arrays are null or different lengths
                    }
                leapSecTextField.setText(onePlaces.format(leapSec));
                }
            dT=leapSec+32.184;
            g = (357.53+0.9856003*(jdEOI-2451545.0))/DEG_IN_RADIAN;
            TDB=jdEOI+(dT+0.001658*Math.sin(g)+0.000014*Math.sin(2*g))/86400.0;
            lstEOI = SkyAlgorithms.CalcLST((int)utDateEOI[0], (int)utDateEOI[1], (int)utDateEOI[2], utDateEOI[3], lon, leapSec);

            if (objectIDTextField.getText().trim().equalsIgnoreCase("moon"))
                radecEOI = computeMoonRaDec(jdEOI, dT, lat, alt, lstEOI);
            else if (objectIDTextField.getText().trim().equalsIgnoreCase("mercury"))
                radecEOI = computePlanetRaDec(0, jdEOI);
            else if (objectIDTextField.getText().trim().equalsIgnoreCase("venus"))
                radecEOI = computePlanetRaDec(1, jdEOI);
            else if (objectIDTextField.getText().trim().equalsIgnoreCase("mars"))
                radecEOI = computePlanetRaDec(3, jdEOI);
            else if (objectIDTextField.getText().trim().equalsIgnoreCase("jupiter"))
                radecEOI = computePlanetRaDec(4, jdEOI);
            else if (objectIDTextField.getText().trim().equalsIgnoreCase("saturn"))
                radecEOI = computePlanetRaDec(5, jdEOI);
            else if (objectIDTextField.getText().trim().equalsIgnoreCase("uranus"))
                radecEOI = computePlanetRaDec(6, jdEOI);
            else if (objectIDTextField.getText().trim().equalsIgnoreCase("neptune"))
                radecEOI = computePlanetRaDec(7, jdEOI);
            else if (objectIDTextField.getText().trim().equalsIgnoreCase("pluto"))
                radecEOI = computePlanetRaDec(8, jdEOI);
            else if(objectIDTextField.getText().trim().equalsIgnoreCase("sun"))
                radecEOI = computeSunRaDec(jdEOI, dT, lat, alt, lstEOI);

            raEOITextField.setText(showSexagesimal ? decToSex(radecEOI[0], 3, 24, false) : sixPlaces.format(radecEOI[0]));
            decEOITextField.setText(showSexagesimal ? decToSex(radecEOI[1], 2, 90, true) : sixPlaces.format(radecEOI[1]));
            raEOITextField.setBorder(greenBorder);
            decEOITextField.setBorder(greenBorder);
            pmRA = 0.0;
            pmDec = 0.0;
            pmRATextField.setText(uptoFourPlaces.format(pmRA));
            pmDecTextField.setText(uptoFourPlaces.format(pmDec));
//            radecJ2000 = SkyAlgorithms.Convert(2000.0, jdEOI, radecEOI[0], radecEOI[1], pmRA, pmDec, leapSec, REVERSE, usePM, usePrec, useNut, useAber);
//            newradecEOI = true;
            ssObject = true;
            }

        return ssObject;
        }

    boolean getSimbadData()
        {
        objectText = objectIDTextField.getText().trim();
        Thread t = new Thread()
            {
            public void run()
                {
                objectIDTextField.setBackground(leapYellow);
                objectIDTextField.setText("accessing SIMBAD...");
                objectIDTextField.paint(objectIDTextField.getGraphics());
                }
            };
        t.start();
        Thread.yield();
        SIMBADAccessFailed = false;
        newSimbadData = false;
        try {
            String objectID = URLEncoder.encode(objectText,"UTF-8");
            URL simbad;
            if (useHarvard)
                simbad = new URL("http://simbad.cfa.harvard.edu/simbad/sim-id?Ident="+objectID+"&output.format=ASCII");
            else
                simbad = new URL("http://simbad.u-strasbg.fr/simbad/sim-id?Ident="+objectID+"&output.format=ASCII");
            URLConnection simbadCon;
            if (useProxy) simbadCon = simbad.openConnection(proxy);
            else simbadCon = simbad.openConnection();
            simbadCon.setConnectTimeout(10000);
            simbadCon.setReadTimeout(10000);
            BufferedReader in = new BufferedReader(new InputStreamReader(simbadCon.getInputStream()));
            String inputLine;

            while ((inputLine = in.readLine()) != null)
                {
                inputLine = inputLine.replaceAll("null", "");
                //IJ.log(inputLine);
                if (inputLine.startsWith("!!"))
                    {
                    showMessage("SIMBAD query error", inputLine.trim().replace("java.lang.Exception:", ""));
                    break;
                    }
                if (inputLine.contains("Number of objects :"))
                    {
                    showMessage("SIMBAD query error", "More than one object queried. Avoid ';' in object syntax");
                    break;
                    }
                if (inputLine.contains("Coordinates(ICRS,ep=J2000,eq=2000):"))
                    {
                    //IJ.log(inputLine.replaceAll("(.*)Coordinates\\(ICRS,ep=J2000,eq=2000\\):", "").trim().replaceAll("(\\(.*)", "").trim());
                    raJ2000TextField.setText(inputLine.replaceAll("(.*)Coordinates\\(ICRS,ep=J2000,eq=2000\\):", "").trim().replaceAll("(\\(.*)", "").trim());
                    pmRA = 0.0;
                    pmDec = 0.0;
                    newSimbadData = true;
                    }
                if (inputLine.startsWith("Proper motions:"))
                    {
                    String[] pieces = inputLine.replace("Proper motions:", "").trim().split(" ");
                    if (pieces.length>0)
                        {
                        newnum = Tools.parseDouble(pieces[0]);
                        pmRA = Double.isNaN(newnum) ? pmRA : newnum;
                        }
                    if (pieces.length>1)
                        {
                        newnum = Tools.parseDouble(pieces[1]);
                        pmDec = Double.isNaN(newnum) ? pmDec : newnum;
                        }
                    }

                }
            if (newSimbadData)
                {
                pmRATextField.setText(uptoFourPlaces.format(pmRA));
                pmDecTextField.setText(uptoFourPlaces.format(pmDec));
                }
            in.close();
            }
        catch (IOException ioe){
            showMessage("SIMBAD query error", "<html>"+"Could not open link to Simbad "+(useHarvard ? "at Harvard." : "in France.")+"<br>"+
                        "Check internet connection or proxy settings or"+"<br>"+
                        "try "+(useHarvard ? "France" : "Harvard")+" server (see Network menu)."+"</html>");
            SIMBADAccessFailed = true;
            }
        Thread t2 = new Thread()
            {
            public void run()
                {
                objectIDTextField.setText(objectText);
                objectIDTextField.setCaretPosition(objectText.length());
                objectIDTextField.setBackground(coordinateEntryEnabled?Color.WHITE:leapGray);
                objectIDTextField.paint(objectIDTextField.getGraphics());
                }
            };
        t2.start();
        Thread.yield();

        return newSimbadData;
        }

    void showInSkyMap()
        {
        try {

//            java.awt.Desktop desktop = java.awt.Desktop.getDesktop();
            String raText = URLEncoder.encode(raJ2000TextField.getText().replace(":", " ").trim(),"UTF-8");
            String decText = URLEncoder.encode(decJ2000TextField.getText().replace(":", " ").trim(),"UTF-8");
//            IJ.log("http://www.sky-map.org/?ra="+raText+"&de="+decText+"&zoom=8&show_box=1&show_grid=1&show_constellation_lines=1&show_constellation_boundaries=1&show_const_names=0&show_galaxies=1&img_source=DSS2");
            URI uri;
            if (useSkyMapBeta)
                uri = new java.net.URI("http://server1.sky-map.org/v2?ra="+raText+"&de="+decText+"&zoom=8&show_box=1&show_grid=1&show_constellation_lines=1&show_constellation_boundaries=1&show_const_names=0&show_galaxies=1&img_source=DSS2");
            else
                uri = new java.net.URI("http://www.sky-map.org/?ra="+raText+"&de="+decText+"&zoom=8&show_box=1&show_grid=1&show_constellation_lines=1&show_constellation_boundaries=1&show_const_names=0&show_galaxies=1&img_source=DSS2");

//            desktop.browse( uri );
            BrowserOpener.openURL(uri.toString());
            }
        catch ( Exception e )
            {
            showMessage("Sky-Map Access Error", "<html>"+"Could not open link to Sky-Map."+"<br>"+
                        "Check internet connection."+"</html>");
            }
        }


    void showInSIMBAD()
        {
        try {
            String queryType = "sim-fbasic";
            String object = "";
            objectText = objectIDTextField.getText().trim();
            String raText = raJ2000TextField.getText().trim();
            String decText = decJ2000TextField.getText().trim();
            if (!ssObject && !objectText.equals(""))
                {
                queryType = "sim-id?Ident=";
                object = URLEncoder.encode(objectText,"UTF-8");
                }
            else if (!raText.equals("") && !decText.equals(""))
                {
                queryType = "sim-coo?Coord=";
                object = URLEncoder.encode(decToSex(radecJ2000[0], 3, 24, false) + decToSex(radecJ2000[1], 2, 90, true),"UTF-8");
                object += "&Radius=2&Radius.unit=arcmin";
                }
            URI simbad;
            if (useHarvard)
                simbad = new URI("http://simbad.cfa.harvard.edu/simbad/"+queryType+object);
            else
                simbad = new URI("http://simbad.u-strasbg.fr/simbad/"+queryType+object);

            BrowserOpener.openURL(simbad.toString());
            }
        catch ( Exception e )
            {
            showMessage("SIMBAD access error", "<html>"+"Could not open link to Simbad "+(useHarvard ? "at Harvard." : "in France.")+"<br>"+
                        "Check internet connection or"+"<br>"+
                        "try "+(useHarvard ? "France" : "Harvard")+" server (see Preferences menu)."+"</html>");
            }
        }

    double getBJDTDB(double jd, double ra2000, double dec2000)
        {
        Thread t = new Thread()
            {
            public void run()
                {
                eoiBJDTextField.setBackground(leapYellow);
                eoiBJDTextField.setText("accessing OSU...");
                eoiBJDTextField.paint(eoiBJDTextField.getGraphics());
                }
            };

        t.start();
        Thread.yield();
        OSUAccessFailed = false;
        double bjd = bjdEOI;
        try {
//            String objectID = URLEncoder.encode(objectIDTextField.getText().trim(),"UTF-8");
            URL ohioState;
            ohioState = new URL("http://astroutils.astronomy.ohio-state.edu/time/utc2bjd.url.php?UTC="+jd+"&RA="+(ra2000*15)+"&DEC="+dec2000);
            URLConnection ohioStateCon;
            if (useProxy) ohioStateCon = ohioState.openConnection(proxy);
            else ohioStateCon = ohioState.openConnection();            
            ohioStateCon.setConnectTimeout(10000);
            ohioStateCon.setReadTimeout(10000);
            BufferedReader in = new BufferedReader(new InputStreamReader(ohioStateCon.getInputStream()));
            String inputLine;
            while ((inputLine = in.readLine()) != null)
                {
//                IJ.log(inputLine);
                if (inputLine.startsWith("<pre>"))
                    {
                    inputLine =inputLine.substring(5);
                    bjd = Tools.parseDouble(inputLine.trim(),0.0);
                    break;
                    }
                }
            in.close();
            }
            catch (IOException ioe){
                showMessage("Ohio State BJD query error", "<html>"+"Could not open link to Ohio State BJD calculation site."+"<br>"+
                            "Check internet connection or proxy settings (see Network menu) or"+"<br>"+
                            "use internal calculations (see Preferences menu)."+"</html>");
                OSUAccessFailed = true;
            }

        eoiBJDTextField.setBackground(!useNowEpoch && timeEnabled ? Color.WHITE : leapGray);
        return bjd;
        }



    boolean getUSNOLeapSecTable()
        {
        try {
            URL leapSecTable = new URL("https://hpiers.obspm.fr/iers/bul/bulc/Leap_Second.dat");
            URLConnection leapSecTableCon = leapSecTable.openConnection();
            leapSecTableCon.setConnectTimeout(10000);
            leapSecTableCon.setReadTimeout(10000);
            BufferedReader in = new BufferedReader(new InputStreamReader(leapSecTableCon.getInputStream()));
            ArrayList<Double> leapJD = new ArrayList<>(Arrays.asList(leapSecJD));
            ArrayList<Double> leapSEC = new ArrayList<>(Arrays.asList(TAIminusUTC));
            ArrayList<Double> leapbaseMJD = new ArrayList<>(Arrays.asList(baseMJD));
            ArrayList<Double> leapbaseMJDMultiplier = new ArrayList<>(Arrays.asList(baseMJDMultiplier));
            double jd = 0, leap = 0, base = 0, multiplier = 0;
            double oldjd = 0;
            String inputLine;
            newleapSecTableReady = false;
            while ((inputLine = in.readLine()) != null)
                {
                    if (inputLine.startsWith("#")) continue;
                    String[] parsed = Pattern.compile("(\\s+)").split(inputLine);
                    base = Double.parseDouble(parsed[1]);
                    jd = base + 2400000.5;
                    if (jd < oldjd) {
                        showMessage("Julian Date table values are not in increasing order");
                        break;
                    }
                    if (jd <= leapSecJD[leapSecJD.length - 1]) { // The current values are fine, so only want to get new leap seconds
                        continue;
                    }
                    leap = Double.parseDouble(parsed[5]);
                    leapJD.add(jd);
                    leapSEC.add(leap);
                    leapbaseMJD.add(41317.0);
                    leapbaseMJDMultiplier.add(0D);
                    oldjd = jd;
                }
            if (leapJD.size() > 0)
                {
                newleapSecTableReady = true;
                leapSecJD = leapJD.toArray(leapSecJD);
                TAIminusUTC = leapSEC.toArray(TAIminusUTC);
                baseMJD = leapbaseMJD.toArray(baseMJD);
                baseMJDMultiplier = leapbaseMJDMultiplier.toArray(baseMJDMultiplier);
                }

            in.close();
            }
            catch (IOException ioe){
                showMessage("USNO query error", "<html>"+"Could not open link to Earth Orientation Center at"+"<br>"+
                            "https://hpiers.obspm.fr/iers/bul/bulc/Leap_Second.dat."+"<br>"+
                            "Check internet connection."+"</html>");
                newleapSecTableReady = false;
                }
            return newleapSecTableReady;
        }


void getObservatories()
		{
        String filename = null;
        BufferedReader in = null;
        String line = "";
		try	{
//            showMessage(this.getClass().toString());
            URL astroccDir = this.getClass().getProtectionDomain().getCodeSource().getLocation();//.toURI().toString().substring(6);
            prefsDir = (new File(astroccDir.getFile())).getParent();
            prefsDir = URLDecoder.decode(prefsDir, "UTF-8");
            if (!this.getClass().toString().contains("Coordinate_Converter"))
                prefsDir = (new File(prefsDir)).getParent();

            File dir = new File(prefsDir);

            if (isWin && (!dir.exists() || prefsDir.endsWith("Desktop")))
                {
                prefsDir = System.getProperty("user.home");
                }
            else if (!isWin)
                {
                prefsDir = System.getProperty("user.home");  // Mac Preferences folder or Linux home dir
                if (isMac)
                    prefsDir += "/Library/Preferences";
                else
                    prefsDir += "/.astrocc";
                }

            filename = prefsDir+separator+"observatories.txt";

            if (useCustomObservatoryList)
                {
                File file = new File(filename);
                if (file.exists())
                    {
                    FileReader fr = new FileReader(file);     //open custom observatory file outside jar
                    in = new BufferedReader(fr);
                    line = in.readLine();
                    processObservatories(line, in);
                    }
                else
                    {

                    useCustomObservatoryList = false;
                    showMessage("<html>No custom observatory list found at:<br>"+
                                filename+"<br>"+
                                "Using internal observatory list.</html>");
                    getInternalObservatories(filename);
                    }

                }
            else
                {
                getInternalObservatories(filename);
                }
            }
        catch (IOException e)
            {
            useCustomObservatoryList = false;
            showMessage("Error opening observatory list");
            getInternalObservatories(filename);
            }
        }

void getInternalObservatories(String filename)
        {
        BufferedReader in = null;
        String line = "";
            try{
            InputStream fn = getClass().getClassLoader().getResourceAsStream("observatories.txt");     //get file from inside jar
            if (fn != null)
                {
                in = new BufferedReader(new InputStreamReader(fn));
                line = in.readLine();
                processObservatories(line, in);

                File file = new File(filename);
                if (!file.exists())
                    {
                    if (prefsDir.endsWith(".astrocc"))
                        {
                        File dir = new File(prefsDir);
                        if (!dir.exists()) dir.mkdir(); // create .astrocc directory
                        }

                    fn = getClass().getClassLoader().getResourceAsStream("observatories.txt");
                    in = new BufferedReader(new InputStreamReader(fn));

                    FileWriter fw = new FileWriter(filename);
                    BufferedWriter out = new BufferedWriter(fw);
                    line = in.readLine();
                    while (line != null)
                        {
                        out.write(line);
                        out.newLine();
                        line = in.readLine();
                        }
                    out.close();
                    in.close();
                    }
                }
            else
                {
                showMessage("ERROR: could not access internal observatory data");
                }

            }
        catch (IOException ee)
            {
            showMessage("ERROR: could not create observatories.txt file for user customization");
            }
        }

void processObservatories(String line, BufferedReader in)
        {
        try {
            java.util.ArrayList<String> observatory = new java.util.ArrayList<String>(0);
            java.util.ArrayList<Double> latitude = new java.util.ArrayList<Double>(0);
            java.util.ArrayList<Double> longitude = new java.util.ArrayList<Double>(0);
            java.util.ArrayList<Double> altitude = new java.util.ArrayList<Double>(0);
            observatory.add("Custom Lon, Lat, and Alt entry");
            latitude.add(0.0);
            longitude.add(0.0);
            altitude.add(0.0);

            while (line != null)
                {
                if (!line.startsWith("#") && line.length()>0)
                    {
                    String[] words = line.trim().split("[\t]{1,}");
                    if (words.length > 0)
                        {
                        observatory.add(words[0].trim());
                        latitude.add(words.length>1?Tools.parseDouble(words[1],0.0):0.0);
                        longitude.add(words.length>2?Tools.parseDouble(words[2],0.0):0.0);
                        altitude.add(words.length>3?Tools.parseDouble(words[3],0.0):0.0);
                        }
                    }
                line = in.readLine();
                }
            in.close();
            if (observatory.size() > 1)
                {
                observatoryIDs = observatory.toArray(observatoryIDs);
                observatoryLats = latitude.toArray(observatoryLats);
                observatoryLons = longitude.toArray(observatoryLons);
                observatoryAlts = altitude.toArray(observatoryAlts);
                }
            }
        catch (Exception exc) {}

		try { in.close(); } catch(Exception exc) {}
    }


//Convert text file with HTML coded data to tab delimited format
//
//		BufferedReader in2 = null;
//        java.util.ArrayList<String> observatory2 = new java.util.ArrayList<String>(0);
//        java.util.ArrayList<String> longitude2 = new java.util.ArrayList<String>(0);
//        java.util.ArrayList<String> latitude2 = new java.util.ArrayList<String>(0);
//        java.util.ArrayList<String> altitude2 = new java.util.ArrayList<String>(0);
//
//		try	{
//            String filename = IJ.getDirectory("imagej")+"observatories1.txt";
//			in2 = new BufferedReader(new FileReader(filename));
//
//			// READ HEADER LINE
//
//			String line = in2.readLine();
//            String lobs ="";
//            String llon = "0.0";
//            String llat = "0.0";
//            String lalt = "0.0";
//			while (line != null)
//				{
//                if (line.contains("<name>"))
//                    {
//                    lobs ="";
//                    llon = "0.0";
//                    llat = "0.0";
//                    lalt = "0.0";
//                    lobs = line.replace("<name>", "").replace("</name>", "").trim();
//                    line = in2.readLine();
//                    if (line.contains("Modern Astronomical Observatory"))
//                        {
//                        line = in2.readLine();
//                        line = in2.readLine();
//                        if (line.contains("<coordinates>"))
//                            {
//                            String[] pieces = line.replace("<coordinates>", "").replace("</coordinates>", "").trim().split(",");
//                            if (pieces.length>0) llon = pieces[0].trim();
//                            if (pieces.length>1) llat = pieces[1].trim();
//                            if (pieces.length>2) lalt = pieces[2].trim();
//                            observatory2.add(lobs);
//                            longitude2.add(llon);
//                            latitude2.add(llat);
//                            altitude2.add(lalt);
//                            }
//                        }
//                    }
//                line = in2.readLine();
//				}
//			in2.close();
//			}
//		catch (IOException e)
//			{
////			System.err.println ("Coordinate Converter: "+e.getMessage());
////			IJ.error("Coordinate Converter: "+e.getMessage());
//			}
//		try { in2.close(); } catch(Exception exc) {}
//
//        String path = IJ.getDirectory("imagej")+"observatories1_out.txt";
//
//        try{
//            PrintWriter pw = null;
//            FileOutputStream fos = new FileOutputStream(path);
//            BufferedOutputStream bos = new BufferedOutputStream(fos);
//            pw = new PrintWriter(bos);
//            for (int i=0; i<observatory2.size();i++)
//                {
//                pw.println(observatory2.get(i)+"\t"+latitude2.get(i)+"\t"+longitude2.get(i)+"\t"+altitude2.get(i));
//                }
//            pw.close();
//            }
//        catch (IOException e){IJ.log("Write failed");}
//		}

//	public void saveFile(String path) throws IOException {
//        path = IJ.getDirectory("imagej")+"observatories1_out.txt";
//		if (path==null || path.equals("")) {
//			SaveDialog sd = new SaveDialog("Save Results", "Results", Prefs.get("options.ext", ".xls"));
//			String file = sd.getFileName();
//			if (file==null) return;
//			path = sd.getDirectory() + file;
//		}
//		char delimiter = path.endsWith(".csv")?',':'\t';
//		PrintWriter pw = null;
//		FileOutputStream fos = new FileOutputStream(path);
//		BufferedOutputStream bos = new BufferedOutputStream(fos);
//		pw = new PrintWriter(bos);
//
//		pw.println("");
//		pw.close();
//	}


    protected void openHelpPanel()
        {
        String filename = "help/coord_conv_help.htm";
        new HelpPanel(filename, "Coordinate Converter");
//        helpPanel.setVisible(true);
        }

	/**
	 * Starts timer which updates real-time values every second.
	 */
	protected void startTimer ()
    {
		try	{
			task = new TimerTask ()
            {
				public void run ()
                {
                showToolTips = Prefs.get ("astroIJ.showToolTips",showToolTips);
                utDateNow = SkyAlgorithms.UTDateNow();
                jdNow = SkyAlgorithms.CalcJD((int)utDateNow[0], (int)utDateNow[1], (int)utDateNow[2], utDateNow[3]);
                if (autoTimeZone)
                    {
                    TimeZone tz = TimeZone.getDefault();
                    Calendar now = Calendar.getInstance(tz);
                    nowTimeZoneOffset = tz.getOffset(now.getTimeInMillis())/3600000.0;
                    timeZoneOffsetSpinner.setValue(nowTimeZoneOffset);
                    }
//                locDateNow = SkyAlgorithms.UTDateFromJD(jdNow+nowTimeZoneOffset/24.0);
                lstNow = SkyAlgorithms.CalcLST((int)utDateNow[0], (int)utDateNow[1], (int)utDateNow[2], utDateNow[3], lon, leapSec);
                epochNow = 2000.0 + (jdNow - 2451545.0)/365.25;
                currentUTDateTextField.setText(""+(int)utDateNow[0]+"-"+twoDigits.format((int)utDateNow[1])+"-"+twoDigits.format((int)utDateNow[2]));
                currentUTTimeTextField.setText(decToSex(utDateNow[3],0, 24, false));

                Calendar utc = GregorianCalendar.getInstance(TimeZone.getTimeZone("UTC"));
                utc.set(utc.ERA, (int)utDateNow[0]<0?GregorianCalendar.BC:GregorianCalendar.AD);
                double mins = ((utDateNow[3]-(int)utDateNow[3])*60.0);
                int intmins = (int)mins;
                int intsecs = Math.round((float)((mins - intmins)*60.0));
                utc.set(Math.abs((int)utDateNow[0]), (int)utDateNow[1]-1, (int)utDateNow[2], (int)utDateNow[3], intmins, intsecs);
                int offsetHours = (int)nowTimeZoneOffset;
                int offsetMins = (int)((nowTimeZoneOffset-(int)nowTimeZoneOffset)*60.0);
                Calendar local = new GregorianCalendar(autoTimeZone?TimeZone.getDefault():
                                 TimeZone.getTimeZone("GMT"+(nowTimeZoneOffset<0?"-":"+")+twoDigits.format(Math.abs(offsetHours))+
                                 ":"+twoDigits.format(Math.abs(offsetMins))));
                local.setTimeInMillis(utc.getTimeInMillis());
                currentLocDateTextField.setText(""+(local.get(local.ERA)==GregorianCalendar.BC?"-":"")+fourDigits.format(local.get(local.YEAR))+
                                            "-"+twoDigits.format(local.get(local.MONTH)+1)+"-"+twoDigits.format(local.get(local.DAY_OF_MONTH)));
                if (useAMPM)
                    {
                    currentLocTimeTextField.setText(""+twoDigits.format(local.get(local.HOUR)==0?12:local.get(local.HOUR))+":"+
                                                   twoDigits.format(local.get(local.MINUTE))+":"+
                                                   twoDigits.format(local.get(local.SECOND))+
                                                   (local.get(local.AM_PM)==local.AM?" AM":" PM"));
                    }
                else
                    {
                    currentLocTimeTextField.setText(""+twoDigits.format(local.get(local.HOUR_OF_DAY))+":"+
                                                   twoDigits.format(local.get(local.MINUTE))+":"+
                                                   twoDigits.format(local.get(local.SECOND)));
                    }

                currentJDTextField.setText(sixPlaces.format(jdNow));
                currentLSTTextField.setText(decToSex(lstNow,0,24,false));
                if (useNowEpoch)
                    {
                    jdEOI = jdNow;
                    utDateEOI = SkyAlgorithms.UTDateFromJD(jdEOI);
                    processAction(null);
//                    computeCoordinates();
                    }
                }
            };

        timer = new Timer();
        timer.schedule (task,0,1000);
        }
        catch (Exception e)
        {
            showMessage ("Error starting timer : "+e.getMessage());
        }
    }

	/**
	 * Starts timer which handles spinner arrow clicks and holds.
	 */
	protected void startSpinnerTimer ()
    {
		try	{
			spinnertask = new TimerTask ()
            {
				public void run ()
                {
                spinnerActive = true;
                if ((mouseSource == jdEOIupButton || mouseSource == jdLocEOIupButton) && jdEOIupMouseDown)
                    {
                    updateJDEOI(jdEOIStep, jdEOIStepFactor, true);
                    }
                else if ((mouseSource == jdEOIdownButton || mouseSource == jdLocEOIdownButton) && jdEOIdownMouseDown)
                    {
                    updateJDEOI(jdEOIStep, -jdEOIStepFactor, true);
                    }
                else
                    {
                    spinnerActive = false;
                    spinnertimer.cancel();
                    spinnertask.cancel();
                    }
                spinnerActive = false;
                }
            };
        spinnertimer = new Timer();
        spinnertimer.schedule (spinnertask,300,100);
        }
        catch (Exception e)
        {
            showMessage ("Error starting spinner timer : "+e.getMessage());
        }
    }


	/**
	 * Starts timer which generates a final set of calculations after scroll wheel spinning
     * has stopped for more than a fixed period  of time.
	 */
	protected void startScrollFinishedTimer ()
    {
		try	{
			scrollfinishedtask = new TimerTask ()
            {
				public void run ()
                {
                processAction(null);
                scrollfinishedtimer.cancel();
                scrollfinishedtask.cancel();
                }
            };
        scrollfinishedtimer = new Timer();
        scrollfinishedtimer.schedule (scrollfinishedtask,1000);
        }
        catch (Exception e)
        {
            showMessage ("Error starting scroll finished timer : "+e.getMessage());
        }
    }



/****************************************************************************************/

/* The following code is adapted from JSkyCalc written by
 * John Thorstensen of Dartmouth College.
 */

   public static double getAirmass(double alt) {
        /* returns the true airmass for a given altitude (degrees).  Ported from C. */
        /* The expression used is based on a tabulation of the mean KPNO
           atmosphere given by C. M. Snell & A. M. Heiser, 1968,
        PASP, 80, 336.  They tabulated the airmass at 5 degr
           intervals from z = 60 to 85 degrees; I fit the data with
           a fourth order poly for (secz - airmass) as a function of
           (secz - 1) using the IRAF curfit routine, then adjusted the
           zeroth order term to force (secz - airmass) to zero at
           z = 0.  The poly fit is very close to the tabulated points
        (largest difference is 3.2e-4) and appears smooth.
           This 85-degree point is at secz = 11.47, so for secz > 12
           I just return secz - 1.5 ... about the largest offset
           properly determined. */
        double secz, seczmin1;
        int i, ord = 3;
        double [] coef = {2.879465e-3, 3.033104e-3, 1.351167e-3, -4.716679e-5};
        double result = 0.;

        if(alt <= 0.) return (-1.);   /* out of range. */
        secz = 1. / Math.sin(alt / DEG_IN_RADIAN);
        seczmin1 = secz - 1.;
        if(secz > 12.) return (secz - 1.5);   // approx at extreme airmass
        for(i = ord; i >= 0; i--) result = (result + coef[i]) * seczmin1;
        result = secz - result;
        return result;
        }

   public double calculateHJDCorrection(double jd, double DT, double ra, double dec, double lst, double latitude, double altitude) {

      baryxyzvel(jd, DT, lst, latitude, altitude, false);  /* find the position and velocity of the observing site wrt the sun */
      double [] unitvec = cel_unitXYZ(ra, dec);
//      System.out.printf("Current: %s\n",current.checkstring());
      barytcor = 0.; baryvcor = 0.;
//      System.out.printf("Bary xyz %f %f %f \n",w.barycenter[0],
//             w.barycenter[1],w.barycenter[2]);
      for(int i = 0; i < 3; i++) {
         barytcor += unitvec[i] * barycenter[i];
         baryvcor += unitvec[i] * barycenter[i+3];
      }
//      System.out.printf("Bary %f sec %f km/s ...\n",
//          barytcor, baryvcor);
      return barytcor / 86400.;
   }

   public double calculateBJDCorrection(double jd, double DT, double ra, double dec, double lst, double latitude, double altitude) {

      baryxyzvel(jd, DT, lst, latitude, altitude, true);  /* find the position and velocity of the
                                 observing site wrt the solar system barycent */
      double [] unitvec = cel_unitXYZ(ra, dec);
//      System.out.printf("Current: %s\n",current.checkstring());
      barytcor = 0.; baryvcor = 0.;
//      System.out.printf("Bary xyz %f %f %f \n",w.barycenter[0],
//             w.barycenter[1],w.barycenter[2]);
      for(int i = 0; i < 3; i++) {
         barytcor += unitvec[i] * barycenter[i];
         baryvcor += unitvec[i] * barycenter[i+3];
      }
//      System.out.printf("Bary %f sec %f km/s ...\n",
//          barytcor, baryvcor);
      return barytcor / 86400.;
   }

  double [] computePlanetRaDec(int planet, double jd)
    {
        double [] earthxyz = {0.,0.,0.};
        double [] eclipt = {0.,0.,0.};
        double [] planetxyz  = {0.,0.,0.};
        double [] planetcel = {0.0, 0.0, 0.0};
        double [] topcel = {0.0, 0.0, 0.0};
        double [] radec = {0.0, 0.0};
        eclipt = planetxyz(2, jd);   // earth
        earthxyz = eclrot(jd,eclipt[0],eclipt[1],eclipt[2]);
        eclipt = planetxyz(planet, jd);  //other planet
        planetxyz = eclrot(jd,eclipt[0],eclipt[1],eclipt[2]);
        planetcel = earthview(earthxyz,planetxyz,jd);
        radec[0] = planetcel[0];
        radec[1] = planetcel[1];
        return radec;
    }


   double [] computeSunRaDec(double jd, double DT, double latitude, double altitude, double lst) {
       // need to avoid handing in a whenwhere or get circularity ...
        double [] retvals;
        double [] topopos;

       // System.out.printf("updating sun jd %f ... ",w.when.jd);
       retvals = computesun(jd, DT);
       double[] xyz = new double [3];
       xyz[0] = retvals[3]; xyz[1] = retvals[4]; xyz[2] = retvals[5];

       // ignoring topocentric part of helio time correction.

       topopos = topocorr(retvals[0], retvals[1], retvals[2], latitude, altitude, lst);
       // System.out.printf("topo radec %s %s\n",topopos.Alpha.RoundedRAString(2,":"),
         //        topopos.Delta.RoundedDecString(1,":"));
       double[] radec = {topopos[0], topopos[1]};
       return radec;
   }

   double computeNauticalTwilight(double jd, double leap, double latitude, double longitude, double altitude, boolean PM) {
   /**  finds the jd at which the sun is at altitude alt  */

      double jdGuess, lastjd, midnight;
      double deriv, err, del = 0.02;
      double alt1=-12.0, alt2, alt3;
      int i = 0;
      double[] sunradec, utDate, utDateGuess, sunaltaz;
      double lstGuess;

   /* Set up calculation, then walk in with Newton-Raphson scheme (guess and
      check using numerical derivatives). */

      midnight = (int)(jd + longitude/360.0) + 0.5 - longitude/360.0;
                 //integer "local" julian date + 0.5 (gets to midnight UT) and then adjust for longitude

      if (PM)
          jdGuess = midnight - 0.3;

      else
          jdGuess = midnight + 0.3;

//      IJ.log("jd="+jd+"   longitude="+longitude+"   jdGuess="+jdGuess);

      utDateGuess = SkyAlgorithms.UTDateFromJD(jdGuess);
      lstGuess = SkyAlgorithms.CalcLST((int)utDateGuess[0], (int)utDateGuess[1], (int)utDateGuess[2], utDateGuess[3], longitude, leap);
      sunradec=computeSunRaDec(jdGuess, leap+32.184, latitude, altitude, lstGuess);
      sunaltaz = SkyAlgorithms.CelestialToHorizontal(sunradec[0], sunradec[1], latitude, longitude, utDateGuess, leap, false);
      alt2 = sunaltaz[0];

//      IJ.log("SunAlt="+alt2+"   SunAz="+sunaltaz[1]);

//      System.out.printf("after alt2: w.when.jd %f alt2 %f\n",
 //                  w.when.jd,alt2);
      jdGuess += del;
      utDateGuess = SkyAlgorithms.UTDateFromJD(jdGuess);
      lstGuess = SkyAlgorithms.CalcLST((int)utDateGuess[0], (int)utDateGuess[1], (int)utDateGuess[2], utDateGuess[3], longitude, leap);
      sunradec=computeSunRaDec(jdGuess, leap+32.184, latitude, altitude, lstGuess);
      sunaltaz = SkyAlgorithms.CelestialToHorizontal(sunradec[0], sunradec[1], latitude, longitude, utDateGuess, leap, false);
      alt3 = sunaltaz[0];
//      IJ.log("SunAlt="+alt3+"   SunAz="+sunaltaz[1]);
      err = alt3 - alt1;
      deriv = (alt3 - alt2) / del;
//      System.out.printf("alt2 alt3 %f %f  err %f deriv %f\n",
 //             alt2,alt3,err,deriv);
      while((Math.abs(err) > 0.001) && (i < 100)) {
          lastjd = jdGuess;
          alt2 = alt3;       // save last guess
          jdGuess -= err/deriv;
          utDateGuess = SkyAlgorithms.UTDateFromJD(jdGuess);
          lstGuess = SkyAlgorithms.CalcLST((int)utDateGuess[0], (int)utDateGuess[1], (int)utDateGuess[2], utDateGuess[3], longitude, leap);
          sunradec=computeSunRaDec(jdGuess, leap+32.184, latitude, altitude, lstGuess);
          sunaltaz = SkyAlgorithms.CelestialToHorizontal(sunradec[0], sunradec[1], latitude, longitude, utDateGuess, leap, false);
          alt3 = sunaltaz[0];
//          IJ.log("SunAlt="+alt3+"   SunAz="+sunaltaz[1]);
//          System.out.printf("alt3 %f jdguess %f\n",alt3,jdguess);
          err = alt3 - alt1;
          i++;
          deriv = (alt3 - alt2) / (jdGuess - lastjd);

//          if(i == 99) IJ.log("jd_sun_alt did not converge.\n");

      }
      if (i >= 99) jdGuess = jd;

      return jdGuess;
//      return (int)jd+jdGuess-(int)jdGuess;
   }

   void baryxyzvel(double jd, double DT, double lst, double latitude, double altitude, boolean bary) {

        int i;
        double [] geopos;
        double [] earthxyz = {0.,0.,0.};
        double [] eclipt = {0.,0.,0.};
        double [] equat  = {0.,0.,0.};
        double [] ecliptvel   = {0.,0.,0.};
        double [] equatvel   = {0.,0.,0.};

        computesun(jd, DT);
       // need sunvel now so get it ...

 //      System.out.printf("into baryxyzvel, jd = %f\n",when.jd);
        sunvel(jd, DT);  // compute sun velocity  ...

//       System.out.printf("Helio Vxyz sun: %f %f %f  %f\n",
 //         s.xyzvel[0] * Const.KMS_AUDAY,
  //        s.xyzvel[1] * Const.KMS_AUDAY,
   //       s.xyzvel[2] * Const.KMS_AUDAY, when.jd);

        comp_el(jd);

        eclipt = planetxyz(2,jd);   // earth  ... do separately
        earthxyz = eclrot(jd,eclipt[0],eclipt[1],eclipt[2]);
        planetxyz[2][0] = earthxyz[0];
        planetxyz[2][1] = earthxyz[1];
        planetxyz[2][2] = earthxyz[2];
        ecliptvel = planetvel(2,jd);
        equatvel = eclrot(jd,ecliptvel[0],ecliptvel[1], ecliptvel[2]);
        planetvelxyz[2][0] = equatvel[0];
        planetvelxyz[2][1] = equatvel[1];
        planetvelxyz[2][2] = equatvel[2];

        for(i = 0; i < 9; i++) {
          if(i != 2)  {  // skip earth
             eclipt = planetxyz(i,jd);
             equat = eclrot(jd,eclipt[0],eclipt[1],eclipt[2]);
             // save xyz position of planet for barycentric correction.
             planetxyz[i][0] = equat[0];
             planetxyz[i][1] = equat[1];
             planetxyz[i][2] = equat[2];
             // and the velocities, too
             ecliptvel = planetvel(i,jd);
             equatvel = eclrot(jd,ecliptvel[0],ecliptvel[1], ecliptvel[2]);
             planetvelxyz[i][0] = equatvel[0];
             planetvelxyz[i][1] = equatvel[1];
             planetvelxyz[i][2] = equatvel[2];
           //  System.out.printf("i %d %s\n",i,planetpos[i].ha.RoundedHAString(0,":"));
          }
       }
       ComputeBaryCor(); // compute offset of barycenter from heliocenter

//       System.out.printf("sun xyz  %f %f %f\n",s.xyz[0],s.xyz[1],s.xyz[2]);
//       System.out.printf(" baryc   %f %f %f\n",p.barycor[0],p.barycor[1],p.barycor[2]);
       for(i = 0; i < 3; i++) {
          barycenter[i] = bary?(-1. * sunxyz[i] - planetbarycor[i]) * LIGHTSEC_IN_AU:(-1. * sunxyz[i]) * LIGHTSEC_IN_AU;
          barycenter[i+3] = bary?(-1. * sunxyzvel[i] - planetbarycor[i+3]) * KMS_AUDAY:(-1. * sunxyzvel[i]) * KMS_AUDAY;
//          System.out.printf("pre-topo: %d   %f   %f \n",i,
//              barycenter[i] / Const.LIGHTSEC_IN_AU,
//                                                barycenter[i+3]);
       }

       // add in the topocentric velocity ... note use of sidereal for longit

       geopos = Geocent(lst, latitude, altitude);

//       System.out.printf("Geopos: %f %f %f\n",geopos[0],geopos[1],geopos[2]);
//       System.out.printf("topo corrn vx vy %f %f\n",
//          Const.OMEGA_EARTH * geopos[1] * Const.EARTHRAD_IN_KM,
//          Const.OMEGA_EARTH * geopos[0] * Const.EARTHRAD_IN_KM);

       // rotation vel is vector omega crossed into posn vector

       barycenter[3] -= OMEGA_EARTH * geopos[1] * EARTHRAD_IN_KM ;
       barycenter[4] += OMEGA_EARTH * geopos[0] * EARTHRAD_IN_KM ;

   }

   void ComputeBaryCor () {
     // Using PREVIOUSLY COMPUTED xyz and xyzvel, computes the offset to the
     // barycenter.

       int i, j;


       planetbarycor[0] = 0.; planetbarycor[1] = 0.; planetbarycor[2] = 0.;
       planetbarycor[3] = 0.; planetbarycor[4] = 0.; planetbarycor[5] = 0.;

       for(i = 0; i < 9; i++) {
          for(j = 0; j < 3; j++) {
             planetbarycor[j] = planetbarycor[j] + planetxyz[i][j] * mass[i];
             planetbarycor[j+3] = planetbarycor[j+3] + planetvelxyz[i][j] * mass[i];
          }
       }
       for(j = 0; j < 3; j++) {
          planetbarycor[j] = planetbarycor[j] / SS_MASS;
          planetbarycor[j+3] = planetbarycor[j+3] / SS_MASS;
       }
   }

  double[] cel_unitXYZ(double ra, double dec) {
      /** Returns UNIT VECTOR {x,y,z}. */
      double[] retvals = {0.,0.,0.};
      double cosdec;

      cosdec = Math.cos(dec/DEG_IN_RADIAN);
      retvals[0] = Math.cos(ra/HRS_IN_RADIAN) * cosdec;
      retvals[1] = Math.sin(ra/HRS_IN_RADIAN) * cosdec;
      retvals[2] = Math.sin(dec/DEG_IN_RADIAN);

      return retvals;
   }

  double[] Geocent(double longitin, double latitin, double height) {
      // XYZ coordinates given geographic.
      // input is decimal hours, decimal degrees, and meters.
      // See 1992 Astr Almanac, p. K11.

      double denom, C_geo, S_geo;
      double geolat, coslat, sinlat, geolong, sinlong, coslong;
      double [] retval = {0.,0.,0.};


      //System.out.printf("lat long %f %f\n",latitin,longitin);
      geolat = latitin / DEG_IN_RADIAN;
      geolong = longitin / HRS_IN_RADIAN;
      //System.out.printf("radians %f %f \n",geolat,geolong);
      coslat = Math.cos(geolat);  sinlat = Math.sin(geolat);
      coslong = Math.cos(geolong); sinlong = Math.sin(geolong);

      denom = (1. - FLATTEN) * sinlat;
      denom = coslat * coslat + denom * denom;
      C_geo = 1./ Math.sqrt(denom);
      S_geo = (1. - FLATTEN) * (1. - FLATTEN) * C_geo;
      C_geo = C_geo + height / EQUAT_RAD;
      S_geo = S_geo + height / EQUAT_RAD;
      retval[0] = C_geo * coslat * coslong;
      retval[1] = C_geo * coslat * sinlong;
      retval[2] = S_geo * sinlat;

      return retval;
  }

   void comp_el(double jd_in) {
      /* Compute and load mean elements for the planets. */
      double T, Tsq, Tcb, d;
      double ups, P, Q, S, V, W, G, H, zeta, psi;  // Meeus p. 110 ff.
      double sinQ, sinZeta, cosQ, cosZeta, sinV, cosV, sin2Zeta, cos2Zeta;
      double sin2Q, cos2Q, sinH, sin2H, cosH, cos2H;

      jd_el = jd_in;
      d = jd_el - 2415020.;    // 1900
      T = d / 36525.;
      Tsq = T * T;
      Tcb = Tsq * T;

      // Mercury, Venus, and Mars from Explanatory Suppl. p. 113

      // Mercury = 0
      incl[0] = 7.002880 + 1.8608e-3 * T - 1.83e-5 * Tsq;
      Omega[0] = 47.14594 + 1.185208 * T + 1.74e-4 * Tsq;
      omega[0] = 75.899697 + 1.55549 * T + 2.95e-4 * Tsq;
      a[0] = 0.3870986;
      daily[0] = 4.0923388;
      ecc[0] = 0.20561421 + 0.00002046 * T;
      L_0[0] = 178.179078 + 4.0923770233 * d  +
	 0.0000226 * Math.pow((3.6525 * T),2.);

      // Venus = 1
      incl[1] = 3.39363 + 1.00583e-03 * T - 9.722e-7 * Tsq;
      Omega[1] = 75.7796472 + 0.89985 * T + 4.1e-4 * Tsq;
      omega[1] = 130.16383 + 1.4080 * T + 9.764e-4 * Tsq;
      a[1] = 0.723325;
      daily[1] = 1.60213049;
      ecc[1] = 0.00682069 - 0.00004774 * T;
      L_0[1] = 342.767053 + 1.6021687039 * 36525 * T +
	 0.000023212 * Math.pow((3.6525 * T),2.);

      // Earth = 2  ... elements from old Nautical Almanac
      ecc[2] = 0.01675104 - 0.00004180*T + 0.000000126*Tsq;
      incl[2] = 0.0;
      Omega[2] = 0.0;
      omega[2] = 101.22083 + 0.0000470684*d + 0.000453*Tsq + 0.000003*Tcb;
      a[2] = 1.0000007;;
      daily[2] = 0.985599;
      L_0[2] = 358.47583 + 0.9856002670*d - 0.000150*Tsq - 0.000003*Tcb +
	    omega[2];

      // Mars = 3
      incl[3] = 1.85033 - 6.75e-04 * T - 1.833e-5 * Tsq;
      Omega[3] = 48.786442 + .770992 * T + 1.39e-6 * Tsq;
      omega[3] = 334.218203 + 1.840758 * T + 1.299e-4 * Tsq;
      a[3] = 1.5236915;
      daily[3] = 0.5240329502 + 1.285e-9 * T;
      ecc[3] = 0.09331290 - 0.000092064 * T - 0.000000077 * Tsq;
      L_0[3] = 293.747628 + 0.5240711638 * d  +
	 0.000023287 * Math.pow((3.6525 * T),2.);

      // Outer planets from Jean Meeus, Astronomical Formulae for
      // Calculators, 3rd Edn, Willman-Bell; p. 100
      // Mutual interactions get pretty big; I'm including some of the
      // larger perturbation terms from Meeus' book.

      // Jupiter = 4

     incl[4] = 1.308736 - 0.0056961 * T + 0.0000039 * Tsq;
     Omega[4] = 99.443414 + 1.0105300 * T + 0.0003522 * Tsq
  		- 0.00000851 * Tcb;
     omega[4] = 12.720972 + 1.6099617 * T + 1.05627e-3 * Tsq
  	- 3.43e-6 * Tcb;
     a[4] = 5.202561;
     daily[4] = 0.08312941782;
     ecc[4] = .04833475  + 1.64180e-4 * T - 4.676e-7*Tsq -
  	1.7e-9 * Tcb;
     L_0[4] = 238.049257 + 3036.301986 * T + 0.0003347 * Tsq -
  	1.65e-6 * Tcb;

     ups = 0.2*T + 0.1;
     P = (237.47555 + 3034.9061 * T) / DEG_IN_RADIAN;
     Q = (265.91650 + 1222.1139 * T) / DEG_IN_RADIAN;
     S = (243.51721 + 428.4677 * T) / DEG_IN_RADIAN;
     V = 5*Q - 2*P;
     W = 2*P - 6*Q + 3*S;
     zeta = Q - P;
     psi = S - Q;
     sinQ = Math.sin(Q);  // compute some of the more popular ones ...
     cosQ = Math.cos(Q);
     sin2Q = Math.sin(2.*Q);
     cos2Q = Math.cos(2.*Q);
     sinV = Math.sin(V);
     cosV = Math.cos(V);
     sinZeta = Math.sin(zeta);
     cosZeta = Math.cos(zeta);
     sin2Zeta = Math.sin(2*zeta);
     cos2Zeta = Math.cos(2*zeta);

     L_0[4] = L_0[4]
	+ (0.331364 - 0.010281*ups - 0.004692*ups*ups)*sinV
	+ (0.003228 - 0.064436*ups + 0.002075*ups*ups)*cosV
	- (0.003083 + 0.000275*ups - 0.000489*ups*ups)*Math.sin(2*V)
	+ 0.002472 * Math.sin(W) + 0.013619 * sinZeta + 0.018472 * sin2Zeta
	+ 0.006717 * Math.sin(3*zeta)
	+ (0.007275  - 0.001253*ups) * sinZeta * sinQ
	+ 0.006417 * sin2Zeta * sinQ
	- (0.033839 + 0.001253 * ups) * cosZeta * sinQ
	- (0.035681 + 0.001208 * ups) * sinZeta * sinQ;
	/* only part of the terms, the ones first on the list and
	   selected larger-amplitude terms from farther down. */

     ecc[4] = ecc[4] + 1e-7 * (
	  (3606 + 130 * ups - 43 * ups*ups) * sinV
	+ (1289 - 580 * ups) * cosV - 6764 * sinZeta * sinQ
	- 1110 * sin2Zeta * sinQ
	+ (1284 + 116 * ups) * cosZeta * sinQ
	+ (1460 + 130 * ups) * sinZeta * cosQ
	+ 6074 * cosZeta * cosQ);

     omega[4] = omega[4]
	+ (0.007192 - 0.003147 * ups) * sinV
	+ ( 0.000197*ups*ups - 0.00675*ups - 0.020428) * cosV
	+ 0.034036 * cosZeta * sinQ + 0.037761 * sinZeta * cosQ;

     a[4] = a[4] + 1.0e-6 * (
	205 * cosZeta - 263 * cosV + 693 * cos2Zeta + 312 * Math.sin(3*zeta)
	+ 147 * Math.cos(4*zeta) + 299 * sinZeta * sinQ
	+ 181 * cos2Zeta * sinQ + 181 * cos2Zeta * sinQ
	+ 204 * sin2Zeta * cosQ + 111 * Math.sin(3*zeta) * cosQ
	- 337 * cosZeta * cosQ - 111 * cos2Zeta * cosQ
	);

     // Saturn = 5
      incl[5] = 2.492519 - 0.00034550*T - 7.28e-7*Tsq;
      Omega[5] = 112.790414 + 0.8731951*T - 0.00015218*Tsq - 5.31e-6*Tcb ;
      omega[5] = 91.098214 + 1.9584158*T + 8.2636e-4*Tsq;
      a[5] = 9.554747;
      daily[5] = 0.0334978749897;
      ecc[5] = 0.05589232 - 3.4550e-4 * T - 7.28e-7*Tsq;
      L_0[5] = 266.564377 + 1223.509884*T + 0.0003245*Tsq - 5.8e-6*Tcb
	+ (0.018150*ups - 0.814181 + 0.016714 * ups*ups) * sinV
	+ (0.160906*ups - 0.010497 - 0.004100 * ups*ups) * cosV
	+ 0.007581 * Math.sin(2*V) - 0.007986 * Math.sin(W)
	- 0.148811 * sinZeta - 0.040786*sin2Zeta
	- 0.015208 * Math.sin(3*zeta) - 0.006339 * Math.sin(4*zeta)
	- 0.006244 * sinQ
	+ (0.008931 + 0.002728 * ups) * sinZeta * sinQ
	- 0.016500 * sin2Zeta * sinQ
	- 0.005775 * Math.sin(3*zeta) * sinQ
	+ (0.081344 + 0.003206 * ups) * cosZeta * sinQ
	+ 0.015019 * cos2Zeta * sinQ
	+ (0.085581 + 0.002494 * ups) * sinZeta * cosQ
	+ (0.025328 - 0.003117 * ups) * cosZeta * cosQ
	+ 0.014394 * cos2Zeta * cosQ;   /* truncated here -- no
		      terms larger than 0.01 degrees, but errors may
		      accumulate beyond this.... */
      ecc[5] = ecc[5] + 1.0e-7 * (
	  (2458. * ups - 7927.) * sinV + (13381. + 1226. * ups) * cosV
	+ 12415. * sinQ + 26599. * cosZeta * sinQ
	- 4687. * cos2Zeta * sinQ - 12696. * sinZeta * cosQ
	- 4200. * sin2Zeta * cosQ +(2211. - 286*ups) * sinZeta*sin2Q
	- 2208. * sin2Zeta * sin2Q
	- 2780. * cosZeta * sin2Q + 2022. * cos2Zeta*sin2Q
	- 2842. * sinZeta * cos2Q - 1594. * cosZeta * cos2Q
	+ 2162. * cos2Zeta*cos2Q );  /* terms with amplitudes
	    > 2000e-7;  some secular variation ignored. */
      omega[5] = omega[5]
	+ (0.077108 + 0.007186 * ups - 0.001533 * ups*ups) * sinV
	+ (0.045803 - 0.014766 * ups - 0.000536 * ups*ups) * cosV
	- 0.075825 * sinZeta * sinQ - 0.024839 * sin2Zeta*sinQ
	- 0.072582 * cosQ - 0.150383 * cosZeta * cosQ +
	0.026897 * cos2Zeta * cosQ;  /* all terms with amplitudes
	    greater than 0.02 degrees -- lots of others! */
      a[5] = a[5] + 1.0e-6 * (
	2933. * cosV + 33629. * cosZeta - 3081. * cos2Zeta
	- 1423. * Math.cos(3*zeta) + 1098. * sinQ - 2812. * sinZeta * sinQ
	+ 2138. * cosZeta * sinQ  + 2206. * sinZeta * cosQ
	- 1590. * sin2Zeta*cosQ + 2885. * cosZeta * cosQ
	+ 2172. * cos2Zeta * cosQ);  /* terms with amplitudes greater
	   than 1000 x 1e-6 */

      // Uranus = 6
      incl[6] = 0.772464 + 0.0006253*T + 0.0000395*Tsq;
      Omega[6] = 73.477111 + 0.4986678*T + 0.0013117*Tsq;
      omega[6] = 171.548692 + 1.4844328*T + 2.37e-4*Tsq - 6.1e-7*Tcb;
      a[6] = 19.21814;
      daily[6] = 1.1769022484e-2;
      ecc[6] = 0.0463444 - 2.658e-5 * T;
      L_0[6] = 244.197470 + 429.863546*T + 0.000316*Tsq - 6e-7*Tcb;
      /* stick in a little bit of perturbation -- this one really gets
         yanked around.... after Meeus p. 116*/
      G = (83.76922 + 218.4901 * T)/DEG_IN_RADIAN;
      H = 2*G - S;

      sinH = Math.sin(H); sin2H = Math.sin(2.*H);
      cosH = Math.cos(H); cos2H = Math.cos(2.*H);

      L_0[6] = L_0[6] + (0.864319 - 0.001583 * ups) * sinH
   	+ (0.082222 - 0.006833 * ups) * cosH
   	+ 0.036017 * sin2H;
      omega[6] = omega[6] + 0.120303 * sinH
   	+ (0.019472 - 0.000947 * ups) * cosH
   	+ 0.006197 * sin2H;
      ecc[6] = ecc[6] + 1.0e-7 * (
   	20981. * cosH - 3349. * sinH + 1311. * cos2H);
      a[6] = a[6] - 0.003825 * cosH;

      /* other corrections to "true longitude" are ignored. */

      // Neptune = 7
      incl[7] = 1.779242 - 9.5436e-3 * T - 9.1e-6*Tsq;
      Omega[7] = 130.681389 + 1.0989350 * T + 2.4987e-4*Tsq - 4.718e-6*Tcb;
      omega[7] = 46.727364 + 1.4245744*T + 3.9082e-3*Tsq - 6.05e-7*Tcb;
      a[7] = 30.10957;
      daily[7] = 6.020148227e-3;
      ecc[7] = 0.00899704 + 6.33e-6 * T;
      L_0[7] = 84.457994 + 219.885914*T + 0.0003205*Tsq - 6e-7*Tcb;
      L_0[7] = L_0[7]
	- (0.589833 - 0.001089 * ups) * sinH
	- (0.056094 - 0.004658 * ups) * cosH
	- 0.024286 * sin2H;
      omega[7] = omega[7] + 0.024039 * sinH
	- 0.025303 * cosH;
      ecc[7] = ecc[7] + 1.0e-7 * (
	4389. * sinH + 1129. * sin2H
	+ 4262. * cosH + 1089. * cos2H);
      a[7] = a[7] + 8.189e-3 * cosH;

      // Pluto = 8; very approx elements, osculating for Sep 15 1992.
      d = jd_el - 2448880.5;  /* 1992 Sep 15 */
      T = d / 36525.;
      incl[8] = 17.1426;
      Omega[8] = 110.180;
      omega[8] = 223.782;
      a[8] = 39.7465;
      daily[8] = 0.00393329;
      ecc[8] = 0.253834;
      L_0[8] = 228.1027 + 0.00393329 * d;
  }

  double [] planetxyz(int p, double jd) {
  /** produces ecliptic X,Y,Z coords for planet number 'p' at date jd. */

     double M, omnotil, nu, r;
     double e, LL, Om, om, nuu, ii;
     double [] retvals = {0.,0.,0.};

  // 1992 Astronomical Almanac p. E4 has these formulae.

     ii = incl[p] / DEG_IN_RADIAN;
     e = ecc[p];

     LL = (daily[p] * (jd - jd_el) + L_0[p]) / DEG_IN_RADIAN;
     Om = Omega[p] / DEG_IN_RADIAN;
     om = omega[p] / DEG_IN_RADIAN;

     M = LL - om;
     omnotil = om - Om;
     // approximate formula for Kepler equation solution ...
     nu = M + (2.*e - 0.25 * Math.pow(e,3.)) * Math.sin(M) +
	     1.25 * e * e * Math.sin(2 * M) +
	     1.08333333 * Math.pow(e,3.) * Math.sin(3 * M);
     r = a[p] * (1. - e*e) / (1 + e * Math.cos(nu));

     retvals[0] = r *
         (Math.cos(nu + omnotil) * Math.cos(Om) - Math.sin(nu +  omnotil) *
               Math.cos(ii) * Math.sin(Om));
     retvals[1] = r *
         (Math.cos(nu +  omnotil) * Math.sin(Om) + Math.sin(nu +  omnotil) *
		Math.cos(ii) * Math.cos(Om));
     retvals[2] = r * Math.sin(nu +  omnotil) * Math.sin(ii);

     return retvals;

  }


  double [] planetvel(int p, double jd)  {

      /* numerically evaluates planet velocity by brute-force
      numerical differentiation. Very unsophisticated algorithm. */

        double dt; /* timestep */
        double x1,y1,z1,x2,y2,z2,r1,d1,r2,d2,ep1;
        double [] pos1 = {0.,0.,0.};
        double [] pos2 = {0.,0.,0.};
        double [] retval = {0.,0.,0.};
        int i;

        dt = 0.1 / daily[p]; /* time for mean motion of 0.1 degree */
        pos1 = planetxyz(p, (jd - dt));
        pos2 = planetxyz(p, (jd + dt));
        for (i = 0; i < 3; i ++) retval[i] = 0.5 * (pos2[i] - pos1[i]) / dt;
        return retval;
        /* answer should be in ecliptic coordinates, in AU per day.*/
   }



   double [] earthview(double [] earthxyz, double [] planxyz, double jd) {
        double [] dxyz = {0.,0.,0.};
        double [] retvals = {0.,0.,0.};
        double eq;
        int i;

        eq = 2000. + (jd - J2000) / 365.25;
        for(i = 0; i < 3; i++) {
           dxyz[i] = planxyz[i] - earthxyz[i];
        }
        retvals = XYZcel(dxyz[0],dxyz[1],dxyz[2]);

        return retvals;
   }

   double [] XYZcel(double x, double y, double z) {
   /** Given x, y, and z, returns {ra, dec, distance}.  */

      double mod;
      double xy;
      double raout, decout;
      double [] radecdist = {0.,0.,0.};

      mod = Math.sqrt(x*x + y*y + z*z);
      if(mod > 0.) {
         x = x / mod; y = y / mod; z = z / mod;
      }
      else {
         radecdist[0] = 0.; radecdist[1] = 0.; radecdist[2] = 0.;
         return radecdist;
      }

      xy = Math.sqrt(x * x + y * y);
      if(xy < 1.0e-11) {  // on the pole
         raout = 0.;      // ra is degenerate
         decout = PI_OVER_2;
         if(z < 0.) decout *= -1.;
      }
      else {
         raout = Math.atan2(y,x) * HRS_IN_RADIAN;
         if (raout<0.0) raout += 24.0;
         decout = Math.asin(z) * DEG_IN_RADIAN;
      }
      // System.out.printf("%f %f\n",raout,decout);
      radecdist[0] = raout;
      radecdist[1] = decout;
      radecdist[2] = mod;
      return radecdist;
   }

     double[][] radecToSSBodyAngles(double[] radec, double jd, double[] utDate, double dT, double longitude, double latitude, double altitude, double lst) {
        // returns angle in degrees between obj ra/dec and the planets and moon
        // the return values are 0=Mercury, 1=Venus, 2=Moon, 3=Mars, 4=Jupiter, 5=Saturn, 6=Uranus, 7=Neptune, 8=pluto
        // must run either calculateHJDCorrection or calculateBJDCorrection first
        // to calculate planet and moon xyz positions

        double[] objxyz = cel_unitXYZ(radec[0], radec[1]);
        double[] ssbradec = {0.0, 0.0};
        double[] ssbxyz = {0.0, 0.0, 0.0};
        double[][] theta = new double[9][2]; //object-planet distances and planet altitudes



        for (int i=0 ; i<9; i++)
            {
            if (i==2)
                ssbradec = computeMoonRaDec(jd, dT, latitude, altitude, lst);
            else
                ssbradec = computePlanetRaDec(i, jd);
            ssbxyz = cel_unitXYZ(ssbradec[0], ssbradec[1]);
            double cosAngle = objxyz[0]*ssbxyz[0]+objxyz[1]*ssbxyz[1]+objxyz[2]*ssbxyz[2];
            if (cosAngle > 1.0) cosAngle = 1.0;
            if (cosAngle < -1.0) cosAngle = -1.0;
            theta[i][0] = Math.acos(cosAngle)*DEG_IN_RADIAN;
            theta[i][1] = SkyAlgorithms.CelestialToHorizontal(ssbradec[0], ssbradec[1], latitude, longitude, utDate, dT-32.184, true)[0];
            }
        return theta;
        }



/* Implements Jean Meeus' solar ephemer is, from Astronomical
   Formulae for Calculators, pp. 79 ff.  Position is wrt *mean* equinox of
   date. */

   double [] computesun(double jd, double dT) {
       double xecl, yecl, zecl;
       double [] equatorial = {0.,0.,0.};
       double e, L, T, Tsq, Tcb;
       double M, Cent, nu, sunlong;
       double Lrad, Mrad, nurad, R;
       double A, B, C, D, E, H;
       double [] retvals = {0.,0.,0.,0.,0.,0.};
            // will be geora, geodec, geodist, x, y, z (geo)

       // correct jd to ephemeris time once we have that done ...

       jd += (dT) / 86400.0;
       T = (jd - 2415020.) / 36525.;  // Julian centuries since 1900
       Tsq = T*T;   Tcb = T*Tsq;

       L = 279.69668 + 36000.76892*T + 0.0003025*Tsq;
       M = 358.47583 + 35999.04975*T - 0.000150*Tsq - 0.0000033*Tcb;
       e = 0.01675104 - 0.0000418*T - 0.000000126*Tsq;

       A = (153.23 + 22518.7541 * T) / DEG_IN_RADIAN;  /* A, B due to Venus */
       B = (216.57 + 45037.5082 * T) / DEG_IN_RADIAN;
       C = (312.69 + 32964.3577 * T) / DEG_IN_RADIAN;  /* C due to Jupiter */
                /* D -- rough correction from earth-moon
                        barycenter to center of earth. */
       D = (350.74 + 445267.1142*T - 0.00144*Tsq) / DEG_IN_RADIAN;
       E = (231.19 + 20.20*T) / DEG_IN_RADIAN;
                       /* "inequality of long period .. */
       H = (353.40 + 65928.7155*T) / DEG_IN_RADIAN;  /* Jupiter. */

       L = L + 0.00134 * Math.cos(A)
             + 0.00154 * Math.cos(B)
	     + 0.00200 * Math.cos(C)
	     + 0.00179 * Math.sin(D)
	     + 0.00178 * Math.sin(E);

       Lrad = L/DEG_IN_RADIAN;
       Mrad = M/DEG_IN_RADIAN;

       Cent = (1.919460 - 0.004789*T -0.000014*Tsq)*Math.sin(Mrad)
	     + (0.020094 - 0.000100*T) * Math.sin(2.0*Mrad)
	     + 0.000293 * Math.sin(3.0*Mrad);
       sunlong = L + Cent;


       nu = M + Cent;
       nurad = nu / DEG_IN_RADIAN;

       R = (1.0000002 * (1 - e*e)) / (1. + e * Math.cos(nurad));
       R = R + 0.00000543 * Math.sin(A)
	      + 0.00001575 * Math.sin(B)
	      + 0.00001627 * Math.sin(C)
	      + 0.00003076 * Math.cos(D)
	      + 0.00000927 * Math.sin(H);
/*      printf("solar longitude: %10.5f  Radius vector %10.7f\n",sunlong,R);
	printf("eccentricity %10.7f  eqn of center %10.5f\n",e,Cent);   */

       sunlong = sunlong/DEG_IN_RADIAN;

       retvals[2] = R; // distance
       xecl = Math.cos(sunlong);  /* geocentric */
       yecl = Math.sin(sunlong);
       zecl = 0.;
       equatorial = eclrot(jd, xecl, yecl, zecl);

       retvals[0] = Math.atan2(equatorial[1],equatorial[0]) * HRS_IN_RADIAN;
       while(retvals[0] < 0.) retvals[0] = retvals[0] + 24.;
       retvals[1] = Math.asin(equatorial[2]) * DEG_IN_RADIAN;

       retvals[3] = equatorial[0] * R;  // xyz
       retvals[4] = equatorial[1] * R;
       retvals[5] = equatorial[2] * R;
//       System.out.printf("computesun XYZ %f %f %f  %f\n",
//          retvals[3],retvals[4],retvals[5],jd);
       sunxyz[0] = retvals[3]; sunxyz[1] = retvals[4]; sunxyz[2] = retvals[5];
       return(retvals);
    }

    void sunvel(double jd, double DT) {
       /* numerically differentiates sun xyz to get velocity. */
       double dt = 0.05; // days ... gives about 8 digits ...
       int i;

       double [] pos1 = computesun(jd - dt/2., DT);
       double [] pos2 = computesun(jd + dt/2., DT);
       for(i = 0; i < 3; i++) {
           sunxyzvel[i] = (pos2[i+3] - pos1[i+3]) / dt;  // AU/d, eq. of date.
       }
    }

  double [] eclrot(double jd, double x, double y, double z) {
  /** rotates x,y,z coordinates to equatorial x,y,z; all are
      in equinox of date. Returns [0] = x, [1] = y, [2] = z */
     double incl;
     double T;
     double [] retval = {0.,0.,0.};

     T = (jd - J2000) / 36525;
     incl = (23.439291 + T * (-0.0130042 - 0.00000016 * T))/DEG_IN_RADIAN;
             /* 1992 Astron Almanac, p. B18, dropping the
               cubic term, which is 2 milli-arcsec! */
     // System.out.printf("T incl %f %f\n",T,incl);
     retval[1] = Math.cos(incl) * y - Math.sin(incl) * z;
     retval[2] = Math.sin(incl) * y + Math.cos(incl) * z;
     retval[0] = x;
     return(retval);
  }

   double[] computeMoonRaDec(double jd, double dT, double latitude, double altitude, double lst) {
 /* Rather accurate lunar
   ephemeris, from Jean Meeus' *Astronomical Formulae For Calculators*,
   pub. Willman-Bell.  Includes all the terms given there. */
      double Lpr,M,Mpr,D,F,Om,T,Tsq,Tcb;
      double e,lambda,B,beta,om1,om2,dist;
      double sinx, x, y, z, l, m, n, pie;
      double [] retvals = {0.,0.};  //ra,dec
      double [] equatorial = {0.,0.,0.};

      jd += dT/86400.;
          /* approximate correction to ephemeris time */
      T = (jd - 2415020.) / 36525.;   /* this based around 1900 ... */
      Tsq = T * T;
      Tcb = Tsq * T;

      Lpr = 270.434164 + 481267.8831 * T - 0.001133 * Tsq
      		+ 0.0000019 * Tcb;
      M = 358.475833 + 35999.0498*T - 0.000150*Tsq
      		- 0.0000033*Tcb;
      Mpr = 296.104608 + 477198.8491*T + 0.009192*Tsq
      		+ 0.0000144*Tcb;
      D = 350.737486 + 445267.1142*T - 0.001436 * Tsq
      		+ 0.0000019*Tcb;
      F = 11.250889 + 483202.0251*T -0.003211 * Tsq
      		- 0.0000003*Tcb;
      Om = 259.183275 - 1934.1420*T + 0.002078*Tsq
      		+ 0.0000022*Tcb;

      Lpr = Lpr % 360.;  M = M % 360.;  Mpr = Mpr % 360.;
      D = D % 360.;  F = F % 360.;  Om = Om % 360.;

      sinx =  Math.sin((51.2 + 20.2 * T)/DEG_IN_RADIAN);
      Lpr = Lpr + 0.000233 * sinx;
      M = M - 0.001778 * sinx;
      Mpr = Mpr + 0.000817 * sinx;
      D = D + 0.002011 * sinx;

      sinx = 0.003964 * Math.sin((346.560+132.870*T -0.0091731*Tsq)/DEG_IN_RADIAN);

      Lpr = Lpr + sinx;
      Mpr = Mpr + sinx;
      D = D + sinx;
      F = F + sinx;


      sinx = Math.sin(Om/DEG_IN_RADIAN);
      Lpr = Lpr + 0.001964 * sinx;
      Mpr = Mpr + 0.002541 * sinx;
      D = D + 0.001964 * sinx;
      F = F - 0.024691 * sinx;
      F = F - 0.004328 * Math.sin((Om + 275.05 -2.30*T)/DEG_IN_RADIAN);

      e = 1 - 0.002495 * T - 0.00000752 * Tsq;

      M = M /DEG_IN_RADIAN;   /* these will all be arguments ... */
      Mpr = Mpr /DEG_IN_RADIAN;
      D = D /DEG_IN_RADIAN;
      F = F /DEG_IN_RADIAN;

      lambda = Lpr + 6.288750 * Math.sin(Mpr)
      	+ 1.274018 * Math.sin(2*D - Mpr)
      	+ 0.658309 * Math.sin(2*D)
      	+ 0.213616 * Math.sin(2*Mpr)
      	- e * 0.185596 * Math.sin(M)
      	- 0.114336 * Math.sin(2*F)
      	+ 0.058793 * Math.sin(2*D - 2*Mpr)
      	+ e * 0.057212 * Math.sin(2*D - M - Mpr)
      	+ 0.053320 * Math.sin(2*D + Mpr)
      	+ e * 0.045874 * Math.sin(2*D - M)
      	+ e * 0.041024 * Math.sin(Mpr - M)
      	- 0.034718 * Math.sin(D)
      	- e * 0.030465 * Math.sin(M+Mpr)
      	+ 0.015326 * Math.sin(2*D - 2*F)
      	- 0.012528 * Math.sin(2*F + Mpr)
      	- 0.010980 * Math.sin(2*F - Mpr)
      	+ 0.010674 * Math.sin(4*D - Mpr)
      	+ 0.010034 * Math.sin(3*Mpr)
      	+ 0.008548 * Math.sin(4*D - 2*Mpr)
      	- e * 0.007910 * Math.sin(M - Mpr + 2*D)
      	- e * 0.006783 * Math.sin(2*D + M)
      	+ 0.005162 * Math.sin(Mpr - D);

      	/* And furthermore.....*/

      lambda = lambda + e * 0.005000 * Math.sin(M + D)
      	+ e * 0.004049 * Math.sin(Mpr - M + 2*D)
      	+ 0.003996 * Math.sin(2*Mpr + 2*D)
      	+ 0.003862 * Math.sin(4*D)
      	+ 0.003665 * Math.sin(2*D - 3*Mpr)
      	+ e * 0.002695 * Math.sin(2*Mpr - M)
      	+ 0.002602 * Math.sin(Mpr - 2*F - 2*D)
      	+ e * 0.002396 * Math.sin(2*D - M - 2*Mpr)
      	- 0.002349 * Math.sin(Mpr + D)
      	+ e * e * 0.002249 * Math.sin(2*D - 2*M)
      	- e * 0.002125 * Math.sin(2*Mpr + M)
      	- e * e * 0.002079 * Math.sin(2*M)
      	+ e * e * 0.002059 * Math.sin(2*D - Mpr - 2*M)
      	- 0.001773 * Math.sin(Mpr + 2*D - 2*F)
      	- 0.001595 * Math.sin(2*F + 2*D)
      	+ e * 0.001220 * Math.sin(4*D - M - Mpr)
      	- 0.001110 * Math.sin(2*Mpr + 2*F)
      	+ 0.000892 * Math.sin(Mpr - 3*D)
      	- e * 0.000811 * Math.sin(M + Mpr + 2*D)
      	+ e * 0.000761 * Math.sin(4*D - M - 2*Mpr)
      	+ e * e * 0.000717 * Math.sin(Mpr - 2*M)
      	+ e * e * 0.000704 * Math.sin(Mpr - 2 * M - 2*D)
      	+ e * 0.000693 * Math.sin(M - 2*Mpr + 2*D)
      	+ e * 0.000598 * Math.sin(2*D - M - 2*F)
      	+ 0.000550 * Math.sin(Mpr + 4*D)
      	+ 0.000538 * Math.sin(4*Mpr)
      	+ e * 0.000521 * Math.sin(4*D - M)
      	+ 0.000486 * Math.sin(2*Mpr - D);

   /*              *eclongit = lambda;  */

      B = 5.128189 * Math.sin(F)
      	+ 0.280606 * Math.sin(Mpr + F)
      	+ 0.277693 * Math.sin(Mpr - F)
      	+ 0.173238 * Math.sin(2*D - F)
      	+ 0.055413 * Math.sin(2*D + F - Mpr)
      	+ 0.046272 * Math.sin(2*D - F - Mpr)
      	+ 0.032573 * Math.sin(2*D + F)
      	+ 0.017198 * Math.sin(2*Mpr + F)
      	+ 0.009267 * Math.sin(2*D + Mpr - F)
      	+ 0.008823 * Math.sin(2*Mpr - F)
      	+ e * 0.008247 * Math.sin(2*D - M - F)
      	+ 0.004323 * Math.sin(2*D - F - 2*Mpr)
      	+ 0.004200 * Math.sin(2*D + F + Mpr)
      	+ e * 0.003372 * Math.sin(F - M - 2*D)
      	+ 0.002472 * Math.sin(2*D + F - M - Mpr)
      	+ e * 0.002222 * Math.sin(2*D + F - M)
      	+ e * 0.002072 * Math.sin(2*D - F - M - Mpr)
      	+ e * 0.001877 * Math.sin(F - M + Mpr)
      	+ 0.001828 * Math.sin(4*D - F - Mpr)
      	- e * 0.001803 * Math.sin(F + M)
      	- 0.001750 * Math.sin(3*F)
      	+ e * 0.001570 * Math.sin(Mpr - M - F)
      	- 0.001487 * Math.sin(F + D)
      	- e * 0.001481 * Math.sin(F + M + Mpr)
      	+ e * 0.001417 * Math.sin(F - M - Mpr)
      	+ e * 0.001350 * Math.sin(F - M)
      	+ 0.001330 * Math.sin(F - D)
      	+ 0.001106 * Math.sin(F + 3*Mpr)
      	+ 0.001020 * Math.sin(4*D - F)
      	+ 0.000833 * Math.sin(F + 4*D - Mpr);
        /* not only that, but */
      B = B + 0.000781 * Math.sin(Mpr - 3*F)
      	+ 0.000670 * Math.sin(F + 4*D - 2*Mpr)
      	+ 0.000606 * Math.sin(2*D - 3*F)
      	+ 0.000597 * Math.sin(2*D + 2*Mpr - F)
      	+ e * 0.000492 * Math.sin(2*D + Mpr - M - F)
      	+ 0.000450 * Math.sin(2*Mpr - F - 2*D)
      	+ 0.000439 * Math.sin(3*Mpr - F)
      	+ 0.000423 * Math.sin(F + 2*D + 2*Mpr)
      	+ 0.000422 * Math.sin(2*D - F - 3*Mpr)
      	- e * 0.000367 * Math.sin(M + F + 2*D - Mpr)
      	- e * 0.000353 * Math.sin(M + F + 2*D)
      	+ 0.000331 * Math.sin(F + 4*D)
      	+ e * 0.000317 * Math.sin(2*D + F - M + Mpr)
      	+ e * e * 0.000306 * Math.sin(2*D - 2*M - F)
      	- 0.000283 * Math.sin(Mpr + 3*F);


      om1 = 0.0004664 * Math.cos(Om/DEG_IN_RADIAN);
      om2 = 0.0000754 * Math.cos((Om + 275.05 - 2.30*T)/DEG_IN_RADIAN);

      beta = B * (1. - om1 - om2);
   /*      *eclatit = beta; */

      pie = 0.950724
      	+ 0.051818 * Math.cos(Mpr)
      	+ 0.009531 * Math.cos(2*D - Mpr)
      	+ 0.007843 * Math.cos(2*D)
      	+ 0.002824 * Math.cos(2*Mpr)
      	+ 0.000857 * Math.cos(2*D + Mpr)
      	+ e * 0.000533 * Math.cos(2*D - M)
      	+ e * 0.000401 * Math.cos(2*D - M - Mpr)
      	+ e * 0.000320 * Math.cos(Mpr - M)
      	- 0.000271 * Math.cos(D)
      	- e * 0.000264 * Math.cos(M + Mpr)
      	- 0.000198 * Math.cos(2*F - Mpr)
      	+ 0.000173 * Math.cos(3*Mpr)
      	+ 0.000167 * Math.cos(4*D - Mpr)
      	- e * 0.000111 * Math.cos(M)
      	+ 0.000103 * Math.cos(4*D - 2*Mpr)
      	- 0.000084 * Math.cos(2*Mpr - 2*D)
      	- e * 0.000083 * Math.cos(2*D + M)
      	+ 0.000079 * Math.cos(2*D + 2*Mpr)
      	+ 0.000072 * Math.cos(4*D)
      	+ e * 0.000064 * Math.cos(2*D - M + Mpr)
      	- e * 0.000063 * Math.cos(2*D + M - Mpr)
      	+ e * 0.000041 * Math.cos(M + D)
      	+ e * 0.000035 * Math.cos(2*Mpr - M)
      	- 0.000033 * Math.cos(3*Mpr - 2*D)
      	- 0.000030 * Math.cos(Mpr + D)
      	- 0.000029 * Math.cos(2*F - 2*D)
      	- e * 0.000029 * Math.cos(2*Mpr + M)
      	+ e * e * 0.000026 * Math.cos(2*D - 2*M)
      	- 0.000023 * Math.cos(2*F - 2*D + Mpr)
      	+ e * 0.000019 * Math.cos(4*D - M - Mpr);

      // System.out.printf("beta lambda %f %f",beta,lambda);
      beta /=DEG_IN_RADIAN;
      lambda /=DEG_IN_RADIAN;
      dist = 1./Math.sin((pie)/DEG_IN_RADIAN);
//      System.out.printf("dist %f\n",dist);

      double distance = dist /EARTHRAD_IN_AU;

      l = Math.cos(lambda) * Math.cos(beta);
      m = Math.sin(lambda) * Math.cos(beta);
      n = Math.sin(beta);
      equatorial = eclrot(jd,l,m,n);
      moonxyz[0] = equatorial[0];
      moonxyz[1] = equatorial[1];
      moonxyz[2] = equatorial[2];
      double ra = Math.atan2(equatorial[1],equatorial[0]) * HRS_IN_RADIAN;
      double dec = Math.asin(equatorial[2]) * DEG_IN_RADIAN;
      retvals = topocorr(ra, dec, distance, latitude, altitude, lst);
      double[] radec = {retvals[0], retvals[1]};
      return radec;

   }

   double flmoon(int n, int nph) {
    /* Gives JD (+- 2 min) of phase nph during a given lunation n.
       Implements formulae in Meeus' Astronomical Formulae for Calculators,
       2nd Edn, publ. by Willman-Bell. */
    /* nph = 0 for new, 1 first qtr, 2 full, 3 last quarter. */

    double jd, cor;
	double M, Mpr, F;
	double T;
	double lun;

	lun = (double) n + (double) nph / 4.;
	T = lun / 1236.85;
	jd = 2415020.75933 + 29.53058868 * lun
		+ 0.0001178 * T * T
		- 0.000000155 * T * T * T
		+ 0.00033 * Math.sin((166.56 + 132.87 * T - 0.009173 * T * T)/DEG_IN_RADIAN);
	M = 359.2242 + 29.10535608 * lun - 0.0000333 * T * T - 0.00000347 * T * T * T;
	M /= DEG_IN_RADIAN;
	Mpr = 306.0253 + 385.81691806 * lun + 0.0107306 * T * T + 0.00001236 * T * T * T;
	Mpr /= DEG_IN_RADIAN;
	F = 21.2964 + 390.67050646 * lun - 0.0016528 * T * T - 0.00000239 * T * T * T;
	F /= DEG_IN_RADIAN;
	if((nph == 0) || (nph == 2)) {/* new or full */
		cor =   (0.1734 - 0.000393*T) * Math.sin(M)
			+ 0.0021 * Math.sin(2*M)
			- 0.4068 * Math.sin(Mpr)
			+ 0.0161 * Math.sin(2*Mpr)
			- 0.0004 * Math.sin(3*Mpr)
			+ 0.0104 * Math.sin(2*F)
			- 0.0051 * Math.sin(M + Mpr)
			- 0.0074 * Math.sin(M - Mpr)
			+ 0.0004 * Math.sin(2*F+M)
			- 0.0004 * Math.sin(2*F-M)
			- 0.0006 * Math.sin(2*F+Mpr)
			+ 0.0010 * Math.sin(2*F-Mpr)
			+ 0.0005 * Math.sin(M+2*Mpr);
		jd += cor;
	}
	else {
		cor = (0.1721 - 0.0004*T) * Math.sin(M)
			+ 0.0021 * Math.sin(2 * M)
			- 0.6280 * Math.sin(Mpr)
			+ 0.0089 * Math.sin(2 * Mpr)
			- 0.0004 * Math.sin(3 * Mpr)
			+ 0.0079 * Math.sin(2*F)
			- 0.0119 * Math.sin(M + Mpr)
			- 0.0047 * Math.sin(M - Mpr)
			+ 0.0003 * Math.sin(2 * F + M)
			- 0.0004 * Math.sin(2 * F - M)
			- 0.0006 * Math.sin(2 * F + Mpr)
			+ 0.0021 * Math.sin(2 * F - Mpr)
			+ 0.0003 * Math.sin(M + 2 * Mpr)
			+ 0.0004 * Math.sin(M - 2 * Mpr)
			- 0.0003 * Math.sin(2*M + Mpr);
		if(nph == 1) cor = cor + 0.0028 -
				0.0004 * Math.cos(M) + 0.0003 * Math.cos(Mpr);
		if(nph == 3) cor = cor - 0.0028 +
				0.0004 * Math.cos(M) - 0.0003 * Math.cos(Mpr);
		jd += cor;

	}
	return jd;
   }

   int lunation(double jd) {
       int n; // approximate lunation ...
       int nlast;
       double newjd, lastnewjd;
       int kount = 0;
       double x;

       nlast = (int) ((jd - 2415020.5) / 29.5307) - 1;

       lastnewjd = flmoon(nlast,0);
       nlast++;
       newjd = flmoon(nlast,0);
       // increment lunations until you're sure the last and next new
       // moons bracket your input value.
       while((newjd < jd) && (kount < 40)) {
           lastnewjd = newjd;
           nlast++; kount++;
           newjd = flmoon(nlast,0);
       }
       if(kount > 35) showMessage("didn't find lunation!\n");
       return (nlast - 1);
   }

   int getMoonPhase(double jd) {

       int n;
       int nlast, phase;
       double newjd, lastnewjd;
       double fqjd, fljd, lqjd;
       int kount = 0;
       double x;

       nlast = lunation(jd);
       lastnewjd = flmoon(nlast,0);
       x = jd - lastnewjd;
       phase = (int) (x / (29.530589/56.0));         // 3.69134);  // 1/8 month, truncated.
       return phase;  // 0 = new moon, 4 = full moon
    }
//       if(noctiles == 0) return
//          String.format(Locale.ENGLISH, "%3.1f days since new moon.",x);
//       else if (noctiles <= 2) {  // nearest first quarter ...
//          fqjd = flmoon(nlast,1);
//          x = jd - fqjd;
//          if(x < 0.)
//              return String.format(Locale.ENGLISH, "%3.1f days before first quarter.",(-1.*x));
//          else
//              return String.format(Locale.ENGLISH, "%3.1f days after first quarter.",x);
//       }
//       else if (noctiles <= 4) {  // nearest full ...
//          fljd = flmoon(nlast,2);
//          x = jd - fljd;
//          if(x < 0.)
//               return String.format(Locale.ENGLISH, "%3.1f days before full moon.",(-1.*x));
//          else
//              return String.format(Locale.ENGLISH, "%3.1f days after full moon.",x);
//       }
//       else if (noctiles <= 6) {  // nearest last quarter ...
//          lqjd = flmoon(nlast,3);
//          x = jd - lqjd;
//          if(x < 0.)
//               return String.format(Locale.ENGLISH, "%3.1f days before last quarter.",(-1.*x));
//          else
//              return String.format(Locale.ENGLISH, "%3.1f days after last quarter.",x);
//       }
//       else {
//          newjd = flmoon(nlast + 1,0);
//          x = jd - newjd;
//          return String.format(Locale.ENGLISH, "%3.1f days before new moon.",(-1.*x));
//       }
//   }


  double[] topocorr(double ra, double dec, double dist, double lat, double alt, double sidereal) {

      double x, y, z, x_geo, y_geo, z_geo, topodist;
      double [] retvals;

      x = Math.cos(ra/HRS_IN_RADIAN) * Math.cos(dec/DEG_IN_RADIAN) * dist;
      y = Math.sin(ra/HRS_IN_RADIAN) * Math.cos(dec/DEG_IN_RADIAN) * dist;
      z = Math.sin(dec/DEG_IN_RADIAN) * dist;

      retvals = Geocent(sidereal, lat, alt);
      x_geo = retvals[0] / EARTHRAD_IN_AU;
      y_geo = retvals[1] / EARTHRAD_IN_AU;
      z_geo = retvals[2] / EARTHRAD_IN_AU;

      x = x - x_geo;
      y = y - y_geo;
      z = z - z_geo;

      topodist = Math.sqrt(x*x + y*y + z*z);

      x /= topodist;
      y /= topodist;
      z /= topodist;

      retvals[0] = Math.atan2(y,x) * HRS_IN_RADIAN;
      if (retvals[0]<0.0) retvals[0] += 24.0;
      retvals[1] = Math.asin(z) * DEG_IN_RADIAN;

      return retvals;
   }

  /****************************************************************************************/




	/** Displays a message in a dialog box titled "Message".
		Writes the Java console if ImageJ is not present. */
	public void showMessage(String msg) {
		showMessage("Message", msg);
	}

	/** Displays a message in a dialog box with the specified title.
		Displays HTML formatted text if 'msg' starts with "<html>".
		There are examples at
		"http://imagej.nih.gov/ij/macros/HtmlDialogDemo.txt".
		Writes to the Java console if ImageJ is not present. */
	public void showMessage(String title, String msg) {

			if (msg!=null && msg.startsWith("<html>"))
				new HTMLDialog(title, msg);
			else
				new MessageDialog(dialogFrame, title, msg);

	}

/** A modal dialog box that displays information. Based on the
	InfoDialogclass from "Java in a Nutshell" by David Flanagan. */
public class MessageDialog extends Dialog implements ActionListener, KeyListener, WindowListener {
	protected Button button;
	protected MultiLineLabel label;
    String osname = System.getProperty("os.name");
    boolean isWindows = osname.startsWith("Windows");
    boolean isMac = !isWindows && osname.startsWith("Mac");
    boolean isLinux = osname.startsWith("Linux");
    boolean isVista = isWindows && (osname.indexOf("Vista")!=-1||osname.indexOf(" 7")!=-1);

	public MessageDialog(Frame parent, String title, String message) {
		super(parent, title, true);
		setLayout(new BorderLayout());
		if (message==null) message = "";
		label = new MultiLineLabel(message);
		if (!isLinux) label.setFont(new Font("SansSerif", Font.PLAIN, 14));
		Panel panel = new Panel();
		panel.setLayout(new FlowLayout(FlowLayout.CENTER, 15, 15));
		panel.add(label);
		add("Center", panel);
		button = new Button("  OK  ");
		button.addActionListener(this);
		button.addKeyListener(this);
		panel = new Panel();
		panel.setLayout(new FlowLayout());
		panel.add(button);
		add("South", panel);
		if (isMac)
			setResizable(false);
		pack();
		center(this);
		addWindowListener(this);
		show();
	}

	public void actionPerformed(ActionEvent e) {
		dispose();
	}

	public void keyPressed(KeyEvent e) {
		int keyCode = e.getKeyCode();
		if (keyCode==KeyEvent.VK_ENTER || keyCode==KeyEvent.VK_ESCAPE)
			dispose();
	}

	public void keyReleased(KeyEvent e) {
		int keyCode = e.getKeyCode();

	}

	public void keyTyped(KeyEvent e) {}

	public void windowClosing(WindowEvent e) {
		dispose();
	}

	public void windowActivated(WindowEvent e) {}
	public void windowOpened(WindowEvent e) {}
	public void windowClosed(WindowEvent e) {}
	public void windowIconified(WindowEvent e) {}
	public void windowDeiconified(WindowEvent e) {}
	public void windowDeactivated(WindowEvent e) {}

}

/** This is modal dialog box that displays HTML formated text. */
public class HTMLDialog extends JDialog implements ActionListener {

	public HTMLDialog(String title, String message) {
		super(dialogFrame, title, true);
//        try {UIManager.setLookAndFeel("javax.swing.plaf.metal.MetalLookAndFeel");}
//        catch (Exception e) { }
		Container container = getContentPane();
		container.setLayout(new BorderLayout());
		if (message==null) message = "";
		JLabel label = new JLabel(message);
		JPanel panel = new JPanel();
		panel.setLayout(new FlowLayout(FlowLayout.CENTER, 15, 15));
		panel.add(label);
		container.add(panel, "Center");
		JButton button = new JButton("OK");
		button.addActionListener(this);
		panel = new JPanel();
		panel.add(button);
		container.add(panel, "South");
		setForeground(Color.black);
		pack();
		center(this);
		show();
	}

	public void actionPerformed(ActionEvent e) {
		//setVisible(false);
		dispose();
	}
}

	/** Positions the specified window in the center of the screen. */
	public void center(Window w) {
        int left = 0;
        int top = 0;
        JFrame cc = dialogFrame;
        if (cc != null)
            {
            Dimension screen = cc.getSize();
            Dimension window = w.getSize();
            if (window.width==0)
                return;
            left = cc.getX()+screen.width/2-window.width/2;
            top = cc.getY()+screen.height/2-window.height/2;
            if (top<0) top = 0;
            }
		w.setLocation(left, top);
	}

    private Frame frame;

    /** Creates a white AWT Image image of the specified size. */
    public Image createBlankImage(int width, int height) {
        if (width==0 || height==0)
            throw new IllegalArgumentException("");
		if (frame==null) {
			frame = new Frame();
			frame.pack();
			frame.setBackground(Color.white);
		}
        Image img = frame.createImage(width, height);
        return img;
    }



    void getPrefs()
        {
        String prefix = dp ? "dpcoords." : "coords.";

        if (this.getClass().toString().contains("Coordinate_Converter")) Prefs.load(this.getClass(), null);
        dialogFrameLocationX  = (int) Prefs.get (prefix+"dialogFrameLocationX",dialogFrameLocationX);
        dialogFrameLocationY  = (int) Prefs.get (prefix+"dialogFrameLocationY",dialogFrameLocationY);
        selectedObservatory = (int) Prefs.get (prefix+"selectedObservatory",selectedObservatory);
        numSavedLSEntries = (int) Prefs.get (prefix+"numSavedLSEntries",numSavedLSEntries);
        useProxy = Prefs.get ("coords.useProxy",useProxy);
        useSkyMapBeta = Prefs.get ("coords.useSkyMapBeta",useSkyMapBeta);
        proxyAddress = Prefs.get ("coords.proxyAddress",proxyAddress);
        proxyPort = (int) Prefs.get ("coords.proxyPort",proxyPort);
        lat = Prefs.get (prefix+"lat",lat);
        lon = Prefs.get (prefix+"lon",lon);
        alt = Prefs.get (prefix+"alt",alt);
        nowTimeZoneOffset = Prefs.get (prefix+"nowTimeZoneOffset",nowTimeZoneOffset);
        objectIDText = Prefs.get (prefix+"objectIDText",objectIDText);
        showSexagesimal = Prefs.get (prefix+"showSexagesimal",showSexagesimal);
        useCustomObservatoryList = Prefs.get (prefix+"useCustomObservatoryList",useCustomObservatoryList);
        useHarvard = Prefs.get (prefix+"useHarvard",useHarvard);
        useOhioState = Prefs.get (prefix+"useOhioState",useOhioState);
        showToolTips = Prefs.get ("astroIJ.showToolTips",showToolTips);
        useNowEpoch = Prefs.get (prefix+"useNowEpoch",useNowEpoch);
        autoLeapSec = Prefs.get (prefix+"autoLeapSec",autoLeapSec);
        autoTimeZone = Prefs.get (prefix+"autoTimeZone",autoTimeZone);
        twiLocal = Prefs.get (prefix+"twiLocal",twiLocal);
        reportSSBDown = Prefs.get (prefix+"reportSSBDown",reportSSBDown);
        useAMPM = Prefs.get (prefix+"useAMPM",useAMPM);
        usePM = Prefs.get (prefix+"usePM",usePM);
        usePrec = Prefs.get (prefix+"usePrec",usePrec);
        useNut = Prefs.get (prefix+"useNut",useNut);
        useAber = Prefs.get (prefix+"useAber",useAber);
        useRefr = Prefs.get (prefix+"useRefr",useRefr);
        ssObject = Prefs.get (prefix+"ssObject",ssObject);
        pmRA = Prefs.get (prefix+"pmRA",pmRA);
        pmDec = Prefs.get (prefix+"pmDec",pmDec);
        leapSec = Prefs.get (prefix+"leapSec",leapSec);
        jdEOI = Prefs.get (prefix+"jdEOI",jdEOI);
        jdEOIStep = (int) Prefs.get (prefix+"jdEOIStep",jdEOIStep);
        jdEOIStepFactor = (int) Prefs.get (prefix+"jdEOIStepFactor",jdEOIStepFactor);
        newradecJ2000 = Prefs.get (prefix+"newradecJ2000",newradecJ2000);
        newelonlatJ2000 = Prefs.get (prefix+"newelonlatJ2000",newelonlatJ2000);
        newradecB1950 = Prefs.get (prefix+"newradecB1950",newradecB1950);
        newglonlatB1950 = Prefs.get (prefix+"newglonlatB1950",newglonlatB1950);
        newradecEOI = Prefs.get (prefix+"newradecEOI",newradecEOI);
        newaltazEOI = Prefs.get (prefix+"newaltazEOI",newaltazEOI);
        newelonlatEOI = Prefs.get (prefix+"newelonlatEOI",newelonlatEOI);
        radecJ2000[0] = Prefs.get (prefix+"raJ2000",radecJ2000[0]);
        radecJ2000[1] = Prefs.get (prefix+"decJ2000",radecJ2000[1]);
        if (numSavedLSEntries>leapSecJD.length)
            {
            Double[] leapSecJDtemp = new Double[numSavedLSEntries];
            Double[] TAIminusUTCtemp = new Double[numSavedLSEntries];
            Double[] baseMJDTemp = new Double[numSavedLSEntries];
            Double[] baseMJDMultiplierTemp = new Double[numSavedLSEntries];
            for (int i=0; i<numSavedLSEntries; i++)
                {
                leapSecJDtemp[i] = Prefs.get ("coords.leapSecJD"+i,i<leapSecJD.length?leapSecJD[i]:leapSecJD[leapSecJD.length-1]);
                TAIminusUTCtemp[i] = Prefs.get ("coords.TAIminusUTC"+i,i<TAIminusUTC.length?TAIminusUTC[i]:TAIminusUTC[TAIminusUTC.length-1]);
                baseMJDTemp[i] = Prefs.get ("coords.baseMJD"+i,i<baseMJD.length?baseMJD[i]:baseMJD[baseMJD.length-1]);
                baseMJDMultiplierTemp[i] = Prefs.get ("coords.baseMJDMultiplier"+i,i<baseMJDMultiplier.length?baseMJDMultiplier[i]:baseMJDMultiplier[baseMJDMultiplier.length-1]);
                }
            leapSecJD = leapSecJDtemp;
            TAIminusUTC = TAIminusUTCtemp;
            baseMJD = baseMJDTemp;
            baseMJDMultiplier = baseMJDMultiplierTemp;
            }
        }

    void savePrefs()
        {
        String prefix = dp ? "dpcoords." : "coords.";

        dialogFrameLocationX = dialogFrame.getLocation().x;
        dialogFrameLocationY = dialogFrame.getLocation().y;
        Prefs.set (prefix+"dialogFrameLocationX",dialogFrameLocationX);
        Prefs.set (prefix+"dialogFrameLocationY",dialogFrameLocationY);
        Prefs.set (prefix+"selectedObservatory",selectedObservatory);
        Prefs.set (prefix+"objectIDText",objectIDTextField.getText());
        Prefs.set (prefix+"numSavedLSEntries",leapSecJD.length);
        Prefs.set (prefix+"lat",lat);
        Prefs.set (prefix+"lon",lon);
        Prefs.set (prefix+"alt",alt);
        Prefs.set ("coords.useProxy",useProxy);
        Prefs.set ("coords.useSkyMapBeta",useSkyMapBeta);
        Prefs.set ("coords.proxyAddress",proxyAddress);
        Prefs.set ("coords.proxyPort",proxyPort);        
        Prefs.set (prefix+"nowTimeZoneOffset",nowTimeZoneOffset);
        Prefs.set (prefix+"showSexagesimal",showSexagesimal);
        Prefs.set (prefix+"useCustomObservatoryList",useCustomObservatoryList);
        Prefs.set (prefix+"useHarvard",useHarvard);
        Prefs.set (prefix+"useOhioState",useOhioState);
        Prefs.set ("astroIJ.showToolTips",showToolTips);
        Prefs.set (prefix+"useNowEpoch",useNowEpoch);
        Prefs.set (prefix+"autoLeapSec",autoLeapSec);
        Prefs.set (prefix+"autoTimeZone",autoTimeZone);
        Prefs.set (prefix+"twiLocal",twiLocal);
        Prefs.set (prefix+"reportSSBDown",reportSSBDown);
        Prefs.set (prefix+"useAMPM",useAMPM);
        Prefs.set (prefix+"usePM",usePM);
        Prefs.set (prefix+"usePrec",usePrec);
        Prefs.set (prefix+"useNut",useNut);
        Prefs.set (prefix+"useAber",useAber);
        Prefs.set (prefix+"useRefr",useRefr);
        Prefs.set (prefix+"ssObject",ssObject);
        Prefs.set (prefix+"pmRA",pmRA);
        Prefs.set (prefix+"pmDec",pmDec);
        Prefs.set (prefix+"jdEOI",jdEOI);
        Prefs.set (prefix+"jdEOIStep",jdEOIStep);
        Prefs.set (prefix+"jdEOIStepFactor",jdEOIStepFactor);
        Prefs.set (prefix+"leapSec",leapSec);
        Prefs.set (prefix+"newradecJ2000",newradecJ2000);
        Prefs.set (prefix+"newelonlatJ2000",newelonlatJ2000);
        Prefs.set (prefix+"newradecB1950",newradecB1950);
        Prefs.set (prefix+"newglonlatB1950",newglonlatB1950);
        Prefs.set (prefix+"newradecEOI",newradecEOI);
        Prefs.set (prefix+"newaltazEOI",newaltazEOI);
        Prefs.set (prefix+"newelonlatEOI",newelonlatEOI);
        Prefs.set (prefix+"raJ2000",radecJ2000[0]);
        Prefs.set (prefix+"decJ2000",radecJ2000[1]);
        if (leapSecJD.length>0)
            {
            for (int i=0; i<leapSecJD.length; i++)
                {
                Prefs.set ("coords.leapSecJD"+i,leapSecJD[i]);
                Prefs.set ("coords.TAIminusUTC"+i,TAIminusUTC[i]);
                Prefs.set ("coords.baseMJD"+i,baseMJD[i]);
                Prefs.set ("coords.baseMJDMultiplier"+i,baseMJDMultiplier[i]);
                }
            }
        if (this.getClass().toString().contains("Coordinate_Converter")) Prefs.savePreferences();
        }

    public void showPanel(boolean showWindow)
        {
        dialogFrame.setVisible(showWindow);
        }
    public boolean isShowing()
        {
        return dialogFrame.isShowing();
        }
    public void setEnableObservatoryEntry(boolean enabled)
        {
        observatoryIDComboBox.setEnabled(enabled);
        enableLonLatAlt(selectedObservatory == 0);
//        lonTextField.setEnabled(enabled);
//        lonTextField.setBackground(enabled?Color.WHITE:leapGray);
//        latTextField.setEnabled(enabled);
//        latTextField.setBackground(enabled?Color.WHITE:leapGray);
//        altTextField.setEnabled(enabled);
//        altTextField.setBackground(enabled?Color.WHITE:leapGray);
        }
    public void setEnableObjectEntry(boolean enabled)
        {
        coordinateEntryEnabled = enabled;
        if (!enabled)
            {
            objectIDTextField.setText("");
            pmRATextField.setText("0.0");
            pmRA = 0.0;
            pmDecTextField.setText("0.0");
            pmDec = 0.0;
            raJ2000TextField.setText("00:00:00.0 00:00:00.0");
            processAction(raJ2000TextField);
            }
        objectIDTextField.setEnabled(enabled);
        objectIDTextField.setBackground(enabled?Color.WHITE:leapGray);
        pmRATextField.setEnabled(enabled);
        pmRATextField.setBackground(enabled?Color.WHITE:leapGray);
        pmDecTextField.setEnabled(enabled);
        pmDecTextField.setBackground(enabled?Color.WHITE:leapGray);
        raJ2000TextField.setEnabled(enabled);
        raJ2000TextField.setBackground(enabled?Color.WHITE:leapGray);
        decJ2000TextField.setEnabled(enabled);
        decJ2000TextField.setBackground(enabled?Color.WHITE:leapGray);
        eclLatJ2000TextField.setEnabled(enabled);
        eclLatJ2000TextField.setBackground(enabled?Color.WHITE:leapGray);
        eclLonJ2000TextField.setEnabled(enabled);
        eclLonJ2000TextField.setBackground(enabled?Color.WHITE:leapGray);
        raB1950TextField.setEnabled(enabled);
        raB1950TextField.setBackground(enabled?Color.WHITE:leapGray);
        decB1950TextField.setEnabled(enabled);
        decB1950TextField.setBackground(enabled?Color.WHITE:leapGray);
        galLonB1950TextField.setEnabled(enabled);
        galLonB1950TextField.setBackground(enabled?Color.WHITE:leapGray);
        galLatB1950TextField.setEnabled(enabled);
        galLatB1950TextField.setBackground(enabled?Color.WHITE:leapGray);

        raEOITextField.setEnabled(enabled);
        raEOITextField.setBackground(enabled?Color.WHITE:leapGray);
        decEOITextField.setEnabled(enabled);
        decEOITextField.setBackground(enabled?Color.WHITE:leapGray);
        eclLonEOITextField.setEnabled(enabled);
        eclLonEOITextField.setBackground(enabled?Color.WHITE:leapGray);
        eclLatEOITextField.setEnabled(enabled);
        eclLatEOITextField.setBackground(enabled?Color.WHITE:leapGray);
        altEOITextField.setEnabled(enabled);
        altEOITextField.setBackground(enabled?Color.WHITE:leapGray);
        azEOITextField.setEnabled(enabled);
        azEOITextField.setBackground(enabled?Color.WHITE:leapGray);
        galLonB1950TextField.setEnabled(enabled);
        galLonB1950TextField.setBackground(enabled?Color.WHITE:leapGray);
        galLatB1950TextField.setEnabled(enabled);
        galLatB1950TextField.setBackground(enabled?Color.WHITE:leapGray);
        }

    public void setEnableTimeEntry(boolean enabled)
        {
        timeEnabled = enabled;
        updateTimeEditable();
        }

    public void setTime(double jd)
        {
        jdEOI = jd;
        }
                
    public int setHJDTime(double hjd)
        {
        eoiHJDTextField.setText(""+hjd);
        OSUAccessFailed =false;
        processAction(eoiHJDTextField);
        if (OSUAccessFailed) return 3;
        return 0;        
        }     
    
    public int setBJDTime(double bjd)
        {
        eoiBJDTextField.setText(""+bjd);
        OSUAccessFailed =false;
        processAction(eoiBJDTextField);
        if (OSUAccessFailed) return 3;
        return 0;         
        }     

    public void setLatLonAlt(double latitude, double longitude, double altitude)
        {
        lat = latitude;
        lon = longitude;
        alt = altitude;
        latTextField.setText(showSexagesimal ? decToSex(lat, 2, 90, true) : sixPlaces.format(lat));
        lonTextField.setText(showSexagesimal ? decToSex(lon, 2, 180, true) : sixPlaces.format(lon));
        altTextField.setText(uptoTwoPlaces.format(alt));
        selectedObservatory = 0;
        ignoreObservatoryAction = true;
        observatoryIDComboBox.setSelectedIndex(selectedObservatory);
        ignoreObservatoryAction = false;
        observatoryIDComboBox.paint(observatoryIDComboBox.getGraphics());
        }

    public boolean setObservatory(String name)
        {
        for (int i=0; i<observatoryIDComboBox.getItemCount(); i++)
            {
            if (observatoryIDComboBox.getItemAt(i).toString().toLowerCase().contains(name.replace("_", " ").toLowerCase().trim()))
                {
                selectedObservatory = i;
                ignoreObservatoryAction = true;
                observatoryIDComboBox.setSelectedIndex(selectedObservatory);
                ignoreObservatoryAction = false;
                observatoryIDComboBox.paint(observatoryIDComboBox.getGraphics());
                lat = selectedObservatory < observatoryLats.length ? observatoryLats[selectedObservatory] : 0.0;
                lon = selectedObservatory < observatoryLons.length ? observatoryLons[selectedObservatory] : 0.0;
                alt = selectedObservatory < observatoryAlts.length ? observatoryAlts[selectedObservatory] : 0.0;
                latTextField.setText(showSexagesimal ? decToSex(lat, 2, 90, true) : sixPlaces.format(lat));
                lonTextField.setText(showSexagesimal ? decToSex(lon, 2, 180, true) : sixPlaces.format(lon));
                altTextField.setText(uptoTwoPlaces.format(alt));
                return true;
                }
            }
        return false;
        }

    public int processManualCoordinates()
        {
        OSUAccessFailed =false;
        processAction(null);
        if (OSUAccessFailed) return 3;
        return 0;
        }

    public int processRADECJ2000Coordinates()
        {
        OSUAccessFailed =false;
        processAction(raJ2000TextField);
        if (OSUAccessFailed) return 3;
        return 0;
        }

    public int processRADECJ2000(double ra, double dec)
        {
        OSUAccessFailed = false;
        clearActiveBox();
        raJ2000TextField.setText(showSexagesimal ? decToSex(ra, 3, 24, false) : sixPlaces.format(ra));
        raJ2000TextField.setBorder(greenBorder);
        decJ2000TextField.setText(showSexagesimal ? decToSex(dec, 2, 90, true) : sixPlaces.format(dec));
        decJ2000TextField.setBorder(greenBorder);
        newradecJ2000 = true;
        processAction(null);
        if (OSUAccessFailed) return 3;
        return 0;
        }

    public int processRADECEOD(double ra, double dec)
        {
        OSUAccessFailed = false;
        clearActiveBox();
        raEOITextField.setText(showSexagesimal ? decToSex(ra, 3, 24, false) : sixPlaces.format(ra));
        raEOITextField.setBorder(greenBorder);
        decEOITextField.setText(showSexagesimal ? decToSex(dec, 2, 90, true) : sixPlaces.format(dec));
        decEOITextField.setBorder(greenBorder);
        newradecEOI = true;
        processAction(null);
        if (OSUAccessFailed) return 3;
        return 0;
        }

    public int processSimbadID(String simbadID)
        {

        objectIDTextField.setText(simbadID.replace("_", " ").toLowerCase().trim());
        SIMBADAccessFailed = false;
        OSUAccessFailed =false;
        processAction(objectIDTextField);
        if (SIMBADAccessFailed) return 1;
        if (!newSimbadData) return 2;
        if (OSUAccessFailed) return 3;
        return 0;
        }

    public String getObservatory()
        {
        return observatoryIDComboBox.getItemAt(selectedObservatory).toString();
        }

    public double getAltitude()
        {
        return altazEOI[0];
        }

    public double getAzimuth()
        {
        return altazEOI[1];
        }

    public double getAirmass()
        {
        return airmass;
        }

    public double getJD()
        {
        return jdEOI;
        }

    public double getHJD()
        {
        return hjdEOI;
        }

    public double getHJDCorrection()
        {
        return hjdEOICorr;
        }

    public double getBJD()
        {
        return bjdEOI;
        }

    public double getBJDCorrection()
        {
        return bjdEOICorr;
        }

    public double getRAEOI()
        {
        return radecEOI[0];
        }

    public double getDecEOI()
        {
        return radecEOI[1];
        }

    public double getRAJ2000()
        {
        return radecJ2000[0];
        }

    public double getDecJ2000()
        {
        return radecJ2000[1];
        }

    public double getObservatoryLatitude()
        {
        return lat;
        }

    public double getObservatoryLongitude()
        {
        return lon;
        }
    
    public double getHourAngle()
        {
        return hazdEOI[0];
        }
    
    public double getZenithDistance()
        {
        return hazdEOI[1];
        }
	}
