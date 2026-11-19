// AstrometrySetup.java

package astroj;

import astroj.astrometry.index.AstrometryNetIndexManager;
import astroj.astrometry.index.gui.IndexManagerPanel;
import astroj.astrometry.solvers.PlateSolver;
import ij.IJ;
import ij.Prefs;
import ij.astro.io.prefs.Property;
import ij.astro.util.UIHelper;
import ij.gui.GUI;
import ij.util.Tools;
import util.prefs.RegionExclusion;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.event.*;
import java.awt.*;
import java.awt.event.*;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Locale;
import java.util.function.DoubleConsumer;


public class AstrometrySetup implements ActionListener, ItemListener, ChangeListener, FocusListener, MouseWheelListener {
    public static final Property<Point> INDEX_MANAGER_PANEL_LOC = new Property<>(new Point(), AstrometrySetup.class);
    static int MIN_MAX_STARS = 10;
    public JFrame astrometrySetupFrame;
    public static final AstrometryNetIndexManager ASTROMETRY_NET_INDEX_MANAGER = new AstrometryNetIndexManager();
    DecimalFormat sixPlaces = new DecimalFormat("0.000000", IJU.dfs);
    Color mainBorderColor = new Color(118, 142, 229);
    String fontName = "Dialog";
    Font p8 = new Font(fontName, Font.PLAIN, 8);
    Font p12 = new Font(fontName, Font.PLAIN, 12);
    Font b12 = new Font(fontName, Font.BOLD, 12);
    Font b11 = new Font(fontName, Font.BOLD, 11);
    Font p11 = new Font(fontName, Font.PLAIN, 11);
    Font b14 = new Font(fontName, Font.BOLD, 14);
    int frameLocationX = -9999999;
    int frameLocationY = -9999999;
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
    int maxOrder = 9;
    int fSlice = 1, cSlice = 1, lSlice = 1;
    boolean showSexagesimal = true;
    boolean useAlternateAstrometryServer = false;
    String defaultAstrometryUrlBase = "https://nova.astrometry.net";
    String alternateAstrometryUrlBase = "http://127.0.0.1:8080";
    JScrollPane scrollPane;
    JPanel astrometrySetupPanel;
    JTabbedPane solverTabs;
    JTextField keyTF, raTF, decTF, alternateAstrometryUrlBaseTF;
    JButton startButton, cancelButton;
    JCheckBox autoSaveCB, skipIfHasWCSCB, processStackCB, annotateCB, addAnnotationsToHeaderCB, useMedianFilterCB, useMaxPeakFindValueCB,
            centroidCB, scaleCB, raDecCB, showLogCB, useDistortionOrderCB, useAlternateAstrometryServerCB, compressBox, fpackBox;
    SpinnerNumberModel startSliceNumberModel, endSliceNumberModel, medianFilterRadiusNumberModel, distortionOrderNumberModel,
            noiseTolNumberModel, maxPeakFindNumberModel, maxNumStarsNumberModel,
            annotateRadiusNumberModel, apertureRadiusNumberModel, apertureBack1NumberModel, apertureBack2NumberModel,
            scaleEstimateNumberModel, scaleErrorNumberModel, raDecRadiusNumberModel;
    JSpinner startSliceSpinner, endSliceSpinner, medianFilterRadiusSpinner,
            noiseTolSpinner, maxPeakFindSpinner, maxNumStarsSpinner, annotateRadiusSpinner,
            apertureRadiusSpinner, apertureBack1Spinner, apertureBack2Spinner,
            scaleEstimateSpinner, scaleErrorSpinner, raDecRadiusSpinner, distortionOrderSpinner;
    Rectangle defaultScreenBounds;
    GraphicsDevice defaultScreen;
    private boolean fpack = false, compress = false;
    private JSpinner customPollSpinner;
    private boolean syncingAperture = false;
    private String startButtonLabel = "START";


    public AstrometrySetup() {
        Locale.setDefault(IJU.locale);
    }

    public boolean start(int firstSlice, int currentSlice, int lastSlice, String startButtonLabel, AstroConverter astroCC, boolean useSexagesimal) {
        UIHelper.setLookAndFeel();

        GraphicsEnvironment localGfxEnvironment = GraphicsEnvironment.getLocalGraphicsEnvironment();
        defaultScreen = localGfxEnvironment.getDefaultScreenDevice();
        defaultScreenBounds = defaultScreen.getDefaultConfiguration().getBounds();

        this.startButtonLabel = startButtonLabel;
        useCCRaDec = (astroCC != null);
        notDP = startButtonLabel.equals("START");
        getPrefs();

        if (useCCRaDec) {
            assert astroCC != null;
            ra = astroCC.getRAJ2000();
            dec = astroCC.getDecJ2000();
        }

        startSlice = firstSlice;
        endSlice = lastSlice;
        fSlice = firstSlice;
        cSlice = currentSlice;
        lSlice = lastSlice;
        showSexagesimal = useSexagesimal;

        if (!IJ.isWindows()) {
            p8 = new Font(fontName, Font.PLAIN, 8);
            p12 = new Font(fontName, Font.PLAIN, 11);
            b12 = new Font(fontName, Font.BOLD, 11);
            b11 = new Font(fontName, Font.BOLD, 10);
            p11 = new Font(fontName, Font.PLAIN, 10);
            b14 = new Font(fontName, Font.BOLD, 13);
        }

        ImageIcon astrometrySetupIcon = createImageIcon("images/astrometry.png", "Astrometry Setup Icon");

        astrometrySetupFrame = new JFrame((notDP ? "" : "DP ") + "Astrometry Settings");
        astrometrySetupFrame.setIconImage(astrometrySetupIcon.getImage());
        astrometrySetupFrame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                canceled = true;
                exit();
                done = true;
            }
        });

        astrometrySetupPanel = new JPanel(new GridBagLayout());
        astrometrySetupPanel.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));
        int row = 0;

        solverTabs = buildSolverTabs();
        addCell(astrometrySetupPanel, solverTabs, row++, 0, 2);

        // Small groups are laid out two per row; an odd one out spans the full width
        var small = new ArrayList<JPanel>();
        small.add(buildOutputPanel());
        small.add(buildAnnotationPanel());
        if (lSlice > 1) {
            small.add(buildStackPanel());
        }
        small.add(buildDistortionPanel());
        for (int i = 0; i < small.size(); i += 2) {
            if (i + 1 < small.size()) {
                addCell(astrometrySetupPanel, small.get(i), row, 0, 1);
                addCell(astrometrySetupPanel, small.get(i + 1), row, 1, 1);
            } else {
                addCell(astrometrySetupPanel, small.get(i), row, 0, 2);
            }
            row++;
        }

        // Wide groups take the full width
        addCell(astrometrySetupPanel, buildStarDetectionPanel(), row++, 0, 2);
        addCell(astrometrySetupPanel, buildSearchConstraintsPanel(), row++, 0, 2);

        // Layout
        var filler = new GridBagConstraints();
        filler.gridx = 0;
        filler.gridy = row;
        filler.gridwidth = 2;
        filler.weighty = 1.0;
        astrometrySetupPanel.add(Box.createVerticalGlue(), filler);

        scrollPane = new JScrollPane(astrometrySetupPanel);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);

        astrometrySetupFrame.getContentPane().setLayout(new BorderLayout());
        astrometrySetupFrame.getContentPane().add(scrollPane, BorderLayout.CENTER);
        astrometrySetupFrame.getContentPane().add(buildButtonBar(), BorderLayout.SOUTH);

        GUI.scaleFrame(astrometrySetupFrame);
        astrometrySetupFrame.pack();
        // keep window within the screen height; the scroll pane handles the rest
        int maxHeight = (int) (defaultScreenBounds.height * 0.9);
        if (astrometrySetupFrame.getHeight() > maxHeight) {
            astrometrySetupFrame.setSize(astrometrySetupFrame.getWidth() + scrollPane.getVerticalScrollBar().getPreferredSize().width, maxHeight);
        }
        astrometrySetupFrame.setResizable(true);

        IJU.setFrameSizeAndLocation(astrometrySetupFrame, frameLocationX, frameLocationY, 0, 0);

        astrometrySetupFrame.setVisible(true);

        while (!done) {
            IJ.wait(500);
            if (useCCRaDec) {
                assert astroCC != null;
                ra = astroCC.getRAJ2000();
                raTF.setText(showSexagesimal ? decToSex(ra, 3, 24, false) : sixPlaces.format(ra));
                dec = astroCC.getDecJ2000();
                decTF.setText(showSexagesimal ? decToSex(dec, 2, 90, true) : sixPlaces.format(dec));
            }
        }
        return canceled;
    }

    private JTabbedPane buildSolverTabs() {
        var tabs = new JTabbedPane();
        tabs.setFont(p12);

        var current = Astrometry.PLATE_SOLVER.get();
        var types = PlateSolver.SolverType.values();
        for (var type : types) {
            var comp = switch (type) {
                case ASTROMETRY_NET_HTTP -> buildWebApiTab();
                case ASTROMETRY_NET_WASM -> buildBundledTab();
            };
            tabs.addTab(type.toString(), null, comp, type.getToolTip());
            if (type == current) {
                tabs.setSelectedComponent(comp);
            }
        }

        tabs.addChangeListener(_ -> {
            int idx = tabs.getSelectedIndex();
            if (idx >= 0 && idx < types.length) Astrometry.PLATE_SOLVER.set(types[idx]);
        });

        return tabs;
    }

    private JPanel buildWebApiTab() {
        var p = new JPanel(new GridBagLayout());
        p.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));
        var c = new GridBagConstraints();
        c.insets = new Insets(3, 4, 3, 4);
        c.anchor = GridBagConstraints.WEST;

        // User key
        c.gridy = 0;
        c.gridx = 0;
        c.weightx = 0;
        c.fill = GridBagConstraints.NONE;
        p.add(label("User Key:"), c);

        keyTF = new JTextField(userKey, 28);
        keyTF.setMargin(new Insets(2, 2, 2, 2));
        keyTF.setFont(p12);
        keyTF.setToolTipText("""
                <html>Enter User Key for the astrometry.net website.<br>\
                Go to <b>nova.astrometry.net</b> to get user key.<br>\
                This is only required to be done once.</html>""");
        keyTF.getDocument().addDocumentListener(new thisDocumentListener());
        c.gridx = 1;
        c.weightx = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        p.add(keyTF, c);

        var keyHint = new JLabel("<html>(Get key from: <b>nova.astrometry.net</b>)</html>");
        keyHint.setFont(p11);
        c.gridx = 2;
        c.weightx = 0;
        c.fill = GridBagConstraints.NONE;
        p.add(keyHint, c);

        // Custom server + poll rate on one row
        c.gridy = 1;
        c.gridx = 0;
        c.fill = GridBagConstraints.NONE;
        useAlternateAstrometryServerCB = new JCheckBox("Use custom server:", useAlternateAstrometryServer);
        useAlternateAstrometryServerCB.setFont(p12);
        useAlternateAstrometryServerCB.setToolTipText("""
                <html>Enable the use of a custom/local Astrometry.net server.<br>\
                Windows users may install a free local server from https://adgsoftware.com/ansvr/</html>""");
        useAlternateAstrometryServerCB.addItemListener(this);
        p.add(useAlternateAstrometryServerCB, c);

        alternateAstrometryUrlBaseTF = new JTextField(useAlternateAstrometryServer ? alternateAstrometryUrlBase : defaultAstrometryUrlBase);
        alternateAstrometryUrlBaseTF.setMargin(new Insets(2, 2, 2, 2));
        alternateAstrometryUrlBaseTF.setFont(p12);
        alternateAstrometryUrlBaseTF.setEnabled(useAlternateAstrometryServer);
        alternateAstrometryUrlBaseTF.setToolTipText("""
                <html>Enter custom/local astrometry.net URL base address.<br>\
                Example: <b>http://127.0.0.1:8080</b></html>""");
        alternateAstrometryUrlBaseTF.getDocument().addDocumentListener(new thisDocumentListener());
        c.gridx = 1;
        c.weightx = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        p.add(alternateAstrometryUrlBaseTF, c);

        customPollSpinner = new JSpinner(new SpinnerNumberModel(
                useAlternateAstrometryServer ? Astrometry.CUSTOM_POLLING.get().intValue() : Astrometry.ASTRO_NET_POLLING.get().intValue(), 1, 10, 1));
        customPollSpinner.setFont(p12);
        customPollSpinner.setToolTipText("""
                <html>The polling rate (in seconds) for the plate solve server. \
                Increase if the server is responding with code 429 (too many requests)</html>""");
        customPollSpinner.addChangeListener(this);
        c.gridy = 2;
        c.gridx = 0;
        c.weightx = 0;
        c.fill = GridBagConstraints.NONE;
        p.add(label("Poll Rate (sec):"), c);

        setWidth(customPollSpinner, 70);
        c.gridx = 1;
        p.add(customPollSpinner, c);

        return wrapTop(p);
    }

    private JPanel buildBundledTab() {
        var p = new JPanel(new GridBagLayout());
        p.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));
        var b = new JButton("Manage Indexes...");
        b.addActionListener(_ -> {
            var frame = new JFrame("Index Manager");
            frame.setIconImage(createImageIcon("images/astrometry.png", "Astrometry Setup Icon").getImage());
            frame.add(new IndexManagerPanel(ASTROMETRY_NET_INDEX_MANAGER));
            frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
            frame.addWindowListener(new WindowAdapter() {
                @Override
                public void windowClosed(WindowEvent e) {
                    try {
                        ASTROMETRY_NET_INDEX_MANAGER.saveOptions();
                    } catch (IOException _) {
                    }
                }

                @Override
                public void windowClosing(WindowEvent e) {
                    try {
                        ASTROMETRY_NET_INDEX_MANAGER.saveOptions();
                    } catch (IOException _) {
                    }
                }
            });
            frame.pack();
            INDEX_MANAGER_PANEL_LOC.locationSavingWindow(frame);
            frame.setVisible(true);
        });
        p.add(b);
        return wrapTop(p);
    }

    private JPanel buildOutputPanel() {
        var g = group("Output");

        var r1 = row();
        autoSaveCB = new JCheckBox(notDP ? "Auto save" : "Re-save raw science", notDP ? autoSave : DPSaveRawWithWCS);
        autoSaveCB.setFont(p12);
        if (notDP) {
            autoSaveCB.setToolTipText("""
                    <html>Automatically saves image(s) with new headers to the original file(s).<br>\
                    WARNING: this option will overwrite the existing image file(s).<br>\
                    WARNING: any previous changes to an image will also be saved over the original image.</html>""");
        } else {
            autoSaveCB.setToolTipText("""
                    <html>Re-saves raw uncalibrated science image with new WCS headers to the original file.<br>\
                    WARNING: this option will re-write the original uncalibrated science image with new WCS headers.<br>\
                    WCS headers will be added to calibrated science image independent of this setting.</html>""");
        }
        autoSaveCB.addItemListener(this);
        r1.add(autoSaveCB);

        fpackBox = new JCheckBox("FPACK", fpack);
        fpackBox.setFont(p12);
        fpackBox.setToolTipText("Compress outfile file with FPACK format (compresses FITS data).");
        fpackBox.addItemListener(this);
        r1.add(fpackBox);

        compressBox = new JCheckBox("GZIP", compress);
        compressBox.setFont(p12);
        compressBox.setToolTipText("Compress outfile file in GZIP format (compresses FITS header information also).");
        compressBox.addItemListener(this);
        r1.add(compressBox);

        fpackBox.setEnabled(notDP ? autoSave : DPSaveRawWithWCS);
        compressBox.setEnabled(notDP ? autoSave : DPSaveRawWithWCS);

        var warn = new JLabel(notDP ? "<html><b>WARNING: may overwrite original image</b></html>"
                : "<html><b>WARNING: may re-write raw science file</b></html>");
        warn.setFont(p11);
        g.add(r1);

        var rw = row();
        rw.add(warn);
        g.add(rw);

        var r2 = row();
        skipIfHasWCSCB = new JCheckBox("Skip images with WCS", skipIfHasWCS);
        skipIfHasWCSCB.setFont(p12);
        skipIfHasWCSCB.setToolTipText("<html>If enabled, images/slices that already have valid FITS WCS headers will be skipped.<br>" +
                "A new plate solve will not be performed and the existing WCS headers will be retained.</html>");
        skipIfHasWCSCB.addItemListener(this);
        r2.add(skipIfHasWCSCB);

        if (notDP) {
            showLogCB = new JCheckBox("Show results log", showLog);
            showLogCB.setFont(p12);
            showLogCB.setToolTipText("<html>Enable to create a log window showing the astrometry.net results.</html>");
            showLogCB.addItemListener(this);
            r2.add(showLogCB);
        }
        g.add(r2);
        return g;
    }

    private JPanel buildStackPanel() {
        var g = group("Stack Processing");
        var r = row();

        processStackCB = new JCheckBox("Process stack", processStack);
        processStackCB.setFont(p12);
        processStackCB.setToolTipText("<html>Plate solve all images in stack.</html>");
        processStackCB.addItemListener(this);
        r.add(processStackCB);

        startSliceNumberModel = new SpinnerNumberModel(fSlice, fSlice, lSlice, 1);
        startSliceSpinner = new JSpinner(startSliceNumberModel);
        startSliceSpinner.setEditor(new JSpinner.NumberEditor(startSliceSpinner, "#"));
        startSliceSpinner.setFont(p12);
        startSliceSpinner.setToolTipText("Set the first slice number to process");
        startSliceSpinner.setEnabled(processStack);
        startSliceSpinner.addChangeListener(this);
        startSliceSpinner.addMouseWheelListener(this);
        setWidth(startSliceSpinner, 80);
        r.add(titled("Start Slice", startSliceSpinner));

        endSliceNumberModel = new SpinnerNumberModel(lSlice, fSlice, lSlice, 1);
        endSliceSpinner = new JSpinner(endSliceNumberModel);
        endSliceSpinner.setEditor(new JSpinner.NumberEditor(endSliceSpinner, "#"));
        endSliceSpinner.setFont(p12);
        endSliceSpinner.setToolTipText("Set the last slice number to process");
        endSliceSpinner.setEnabled(processStack);
        endSliceSpinner.addChangeListener(this);
        endSliceSpinner.addMouseWheelListener(this);
        setWidth(endSliceSpinner, 80);
        r.add(titled("End Slice", endSliceSpinner));

        g.add(r);
        return g;
    }

    private JPanel buildAnnotationPanel() {
        var g = group("Annotation");
        var r = row();

        annotateCB = new JCheckBox("Annotate", annotate);
        annotateCB.setFont(p12);
        annotateCB.setToolTipText("<html>Label objects supported by astrometry.net after a successful plate solve</html>");
        annotateCB.addItemListener(this);
        r.add(annotateCB);

        addAnnotationsToHeaderCB = new JCheckBox("Add to header", addAnnotationsToHeader);
        addAnnotationsToHeaderCB.setFont(p12);
        addAnnotationsToHeaderCB.setToolTipText("<html>Add annotations to FITS header</html>");
        addAnnotationsToHeaderCB.setEnabled(annotate);
        addAnnotationsToHeaderCB.addItemListener(this);
        r.add(addAnnotationsToHeaderCB);

        annotateRadiusNumberModel = new SpinnerNumberModel(annotateRadius, 1.0, null, annotateRadiusStep);
        annotateRadiusSpinner = new JSpinner(annotateRadiusNumberModel);
        annotateRadiusSpinner.setEditor(new JSpinner.NumberEditor(annotateRadiusSpinner, "0.00"));
        annotateRadiusSpinner.setEnabled(annotate);
        annotateRadiusSpinner.setFont(p11);
        annotateRadiusSpinner.setToolTipText("<html>The radius of the circle used to identify the annotated object.<br>" +
                "Right click to set spinner stepsize.</html>");
        annotateRadiusSpinner.addChangeListener(_ -> annotateRadius = value(annotateRadiusSpinner));
        installStepPopup(annotateRadiusStep, s -> {
            annotateRadiusStep = s;
            annotateRadiusNumberModel.setStepSize(s);
        }, annotateRadiusSpinner);
        addWheel(annotateRadiusSpinner, annotateRadiusNumberModel);
        setWidth(annotateRadiusSpinner, 100);
        r.add(titled("Radius (pixels)", annotateRadiusSpinner));

        g.add(r);
        return g;
    }

    private JPanel buildStarDetectionPanel() {
        var g = group("Star Detection");

        // Median filter
        var r1 = row();
        useMedianFilterCB = new JCheckBox("Median filter", useMedianFilter);
        useMedianFilterCB.setFont(p12);
        useMedianFilterCB.setToolTipText("<html>Median filter image before finding peaks.<br>" +
                "NOTE: does not change user image.</html>");
        useMedianFilterCB.addItemListener(this);
        r1.add(useMedianFilterCB);

        if (medianFilterRadius < 2) medianFilterRadius = 2;
        medianFilterRadiusNumberModel = new SpinnerNumberModel(medianFilterRadius, 2, null, 1);
        medianFilterRadiusSpinner = new JSpinner(medianFilterRadiusNumberModel);
        medianFilterRadiusSpinner.setEditor(new JSpinner.NumberEditor(medianFilterRadiusSpinner, "#"));
        medianFilterRadiusSpinner.setFont(p12);
        medianFilterRadiusSpinner.setToolTipText("<html>Set the radius of the median filter used before finding peaks (if enabled).<br>" +
                "NOTE: does not change user image.</html>");
        medianFilterRadiusSpinner.setEnabled(useMedianFilter);
        medianFilterRadiusSpinner.addChangeListener(this);
        medianFilterRadiusSpinner.addMouseWheelListener(this);
        setWidth(medianFilterRadiusSpinner, 100);
        r1.add(titled("Filter Radius (pixels)", medianFilterRadiusSpinner));
        g.add(r1);

        // Noise tolerance
        var r2 = row();
        noiseTolNumberModel = new SpinnerNumberModel(minPeakFindToleranceSTDEV, 0.0, null, noiseTolStep);
        noiseTolSpinner = new JSpinner(noiseTolNumberModel);
        noiseTolSpinner.setFont(p11);
        noiseTolSpinner.setEditor(new JSpinner.NumberEditor(noiseTolSpinner, "0.00#####"));
        noiseTolSpinner.setToolTipText("<html>Peaks below the image standard deviation times this factor are ignored.<br>" +
                "Generally a value of 1.0 works well.<br>" +
                "Right click to set spinner stepsize.</html>");
        noiseTolSpinner.addChangeListener(_ -> minPeakFindToleranceSTDEV = value(noiseTolSpinner));
        installStepPopup(noiseTolStep, s -> {
            noiseTolStep = s;
            noiseTolNumberModel.setStepSize(s);
        }, noiseTolSpinner);
        addWheel(noiseTolSpinner, noiseTolNumberModel);
        setWidth(noiseTolSpinner, 110);
        r2.add(titled("Noise Tol (StdDev)", noiseTolSpinner));
        g.add(r2);

        // Max number of stars
        var r2b = row();
        if (maxNumStars < MIN_MAX_STARS) maxNumStars = MIN_MAX_STARS;
        maxNumStarsNumberModel = new SpinnerNumberModel(maxNumStars, MIN_MAX_STARS, null, 1);
        maxNumStarsSpinner = new JSpinner(maxNumStarsNumberModel);
        maxNumStarsSpinner.setEditor(new JSpinner.NumberEditor(maxNumStarsSpinner, "#"));
        maxNumStarsSpinner.setFont(p12);
        maxNumStarsSpinner.setToolTipText("<html>The number of stars used for plate solving is limited to this value.<br>" +
                "The brightest stars that have peaks less than the max peak threshold (if enabled) are used first.<br>" +
                "Generally a value of 50 works well.</html>");
        maxNumStarsSpinner.addChangeListener(this);
        maxNumStarsSpinner.addMouseWheelListener(this);
        setWidth(maxNumStarsSpinner, 100);
        r2b.add(titled("Max Num Stars", maxNumStarsSpinner));
        g.add(r2b);

        // Max peak limit
        var r3 = row();
        useMaxPeakFindValueCB = new JCheckBox("Limit max peaks", useMaxPeakFindValue);
        useMaxPeakFindValueCB.setFont(p12);
        useMaxPeakFindValueCB.setToolTipText("<html>When enabled, peaks above the threshold set to the right are ignored</html>");
        useMaxPeakFindValueCB.addItemListener(this);
        r3.add(useMaxPeakFindValueCB);

        maxPeakFindNumberModel = new SpinnerNumberModel(maxPeakFindValue, null, null, maxPeakFindStep);
        maxPeakFindSpinner = new JSpinner(maxPeakFindNumberModel);
        maxPeakFindSpinner.setEnabled(useMaxPeakFindValue);
        maxPeakFindSpinner.setFont(p11);
        maxPeakFindSpinner.setEditor(new JSpinner.NumberEditor(maxPeakFindSpinner, "0.######"));
        maxPeakFindSpinner.setToolTipText("<html>If enabled, peaks above this ADU level are ignored.<br>" +
                "Right click to set spinner stepsize.</html>");
        maxPeakFindSpinner.addChangeListener(_ -> maxPeakFindValue = value(maxPeakFindSpinner));
        installStepPopup(maxPeakFindStep, s -> {
            maxPeakFindStep = s;
            maxPeakFindNumberModel.setStepSize(s);
        }, maxPeakFindSpinner);
        addWheel(maxPeakFindSpinner, maxPeakFindNumberModel);
        setWidth(maxPeakFindSpinner, 110);
        r3.add(titled("Max Peak (ADU)", maxPeakFindSpinner));

        g.add(r3);

        // Border exclusion
        var r4 = row();
        var excludeBorderCb = new JCheckBox("Exclude border regions", RegionExclusion.EXCLUDE_BORDERS.get());
        excludeBorderCb.setFont(p12);
        excludeBorderCb.setToolTipText("<html>When enabled, allows excluding border regions from plate solving. In IJ Coordinates.</html>");
        excludeBorderCb.addItemListener(RegionExclusion.EXCLUDE_BORDERS.toItemListener());
        RegionExclusion.EXCLUDE_BORDERS.addListener((_, n) -> {
            if (n) {
                RegionExclusion.DISPLAY_EXCLUDED_REGIONS.set(true);
            }
        });
        if (RegionExclusion.EXCLUDE_BORDERS.get()) {
            RegionExclusion.DISPLAY_EXCLUDED_REGIONS.set(true);
        }
        r4.add(excludeBorderCb);

        var excludeBorderEditButton = new JButton("Edit Regions");
        excludeBorderEditButton.setFont(p12);
        excludeBorderEditButton.addActionListener(_ -> RegionExclusion.editSettings());
        excludeBorderEditButton.setEnabled(RegionExclusion.EXCLUDE_BORDERS.get());
        r4.add(excludeBorderEditButton);

        excludeBorderCb.addItemListener(l ->
                excludeBorderEditButton.setEnabled(l.getStateChange() == ItemEvent.SELECTED));
        g.add(r4);

        // Centroiding options live with the star detection settings
        addCentroidRows(g);

        return g;
    }

    private void addCentroidRows(JPanel g) {
        var r = row();

        centroidCB = new JCheckBox("Centroid near peaks", useCentroid);
        centroidCB.setFont(p12);
        centroidCB.setToolTipText("<html>When enabled, the centroid location nearest each peak<br>" +
                "will be used in place of the peak location.<br>" +
                "This feature is useful for defocused images.<br>" +
                "However, multiple peaks within the centroid radius may merge to the brighter star.<br>" +
                "Astrometry.net is usually tolerant of a few erroneous star locations.</html>");
        centroidCB.addItemListener(this);
        r.add(centroidCB);

        if (apertureBack1 < apertureRadius) apertureBack1 = apertureRadius;
        if (apertureBack2 < apertureBack1 + 1) apertureBack2 = apertureBack1 + 1;

        apertureRadiusNumberModel = new SpinnerNumberModel(apertureRadius, 1.0, null, apertureStep);
        apertureRadiusSpinner = new JSpinner(apertureRadiusNumberModel);
        apertureBack1NumberModel = new SpinnerNumberModel(apertureBack1, apertureRadius, null, apertureStep);
        apertureBack1Spinner = new JSpinner(apertureBack1NumberModel);
        apertureBack2NumberModel = new SpinnerNumberModel(apertureBack2, apertureBack1 + 1, null, apertureStep);
        apertureBack2Spinner = new JSpinner(apertureBack2NumberModel);

        for (var s : new JSpinner[]{apertureRadiusSpinner, apertureBack1Spinner, apertureBack2Spinner}) {
            s.setEnabled(useCentroid);
            s.setFont(p11);
            s.setEditor(new JSpinner.NumberEditor(s, "0.00"));
            setWidth(s, 100);
        }

        apertureRadiusSpinner.setToolTipText("<html>The radius used to find the centroid nearest the peak location.<br>" +
                "Right click to set spinner stepsize.</html>");
        apertureBack1Spinner.setToolTipText("<html>The radius of the inner edge of the sky background annulus<br>" +
                "used to find the centroid nearest the peak location.<br>" +
                "Right click to set spinner stepsize.</html>");
        apertureBack2Spinner.setToolTipText("<html>The radius of the outer edge of the sky background annulus<br>" +
                "used to find the centroid nearest the peak location.<br>" +
                "Right click to set spinner stepsize.</html>");

        apertureRadiusSpinner.addChangeListener(_ -> {
            apertureRadius = value(apertureRadiusSpinner);
            syncApertureModels();
        });
        apertureBack1Spinner.addChangeListener(_ -> {
            apertureBack1 = value(apertureBack1Spinner);
            syncApertureModels();
        });
        apertureBack2Spinner.addChangeListener(_ -> {
            apertureBack2 = value(apertureBack2Spinner);
            syncApertureModels();
        });

        installStepPopup(apertureStep, s -> {
            apertureStep = s;
            apertureRadiusNumberModel.setStepSize(s);
            apertureBack1NumberModel.setStepSize(s);
            apertureBack2NumberModel.setStepSize(s);
        }, apertureRadiusSpinner, apertureBack1Spinner, apertureBack2Spinner);

        addWheel(apertureRadiusSpinner, apertureRadiusNumberModel);
        addWheel(apertureBack1Spinner, apertureBack1NumberModel);
        addWheel(apertureBack2Spinner, apertureBack2NumberModel);

        r.add(titled("Radius (pixels)", apertureRadiusSpinner));
        r.add(titled("Sky Inner (pixels)", apertureBack1Spinner));
        r.add(titled("Sky Outer (pixels)", apertureBack2Spinner));

        g.add(r);
    }

    private JPanel buildSearchConstraintsPanel() {
        var g = group("Search Constraints");

        // Plate scale
        var r1 = row();
        scaleCB = new JCheckBox("Constrain plate scale", useScale);
        scaleCB.setFont(p12);
        scaleCB.setToolTipText("<html>When enabled, the astometry.net plate scale search is limited to<br>" +
                "the plate scale value +/- the plate scale tolerance specified.<br>" +
                "Specifying a plate scale range can significantly reduce solve time.</html>");
        scaleCB.addItemListener(this);
        r1.add(scaleCB);

        scaleEstimateNumberModel = new SpinnerNumberModel(scaleEstimate, 0.0, null, scaleStep);
        scaleEstimateSpinner = new JSpinner(scaleEstimateNumberModel);
        scaleEstimateSpinner.setEnabled(useScale);
        scaleEstimateSpinner.setFont(p11);
        scaleEstimateSpinner.setEditor(new JSpinner.NumberEditor(scaleEstimateSpinner, "0.000##"));
        scaleEstimateSpinner.setToolTipText("<html>If enabled, this value is used as the nominal plate scale value by astometry.net.<br>" +
                "Specifying a plate scale value and tolerance can significantly reduce solve time.<br>" +
                "Right click to set spinner stepsize.</html>");
        scaleEstimateSpinner.addChangeListener(_ -> scaleEstimate = value(scaleEstimateSpinner));
        setWidth(scaleEstimateSpinner, 110);
        r1.add(titled("Plate Scale (arcsec/pix)", scaleEstimateSpinner));

        scaleErrorNumberModel = new SpinnerNumberModel(scaleError, 0.0, null, scaleStep);
        scaleErrorSpinner = new JSpinner(scaleErrorNumberModel);
        scaleErrorSpinner.setEnabled(useScale);
        scaleErrorSpinner.setFont(p11);
        scaleErrorSpinner.setEditor(new JSpinner.NumberEditor(scaleErrorSpinner, "0.000##"));
        scaleErrorSpinner.setToolTipText("<html>If enabled, this value is used as the plate scale tolerance by astometry.net.<br>" +
                "Specifying a plate scale value and tolerance can significantly reduce solve time.<br>" +
                "Right click to set spinner stepsize.</html>");
        scaleErrorSpinner.addChangeListener(_ -> scaleError = value(scaleErrorSpinner));
        setWidth(scaleErrorSpinner, 110);
        r1.add(titled("Tolerance (arcsec/pix)", scaleErrorSpinner));

        installStepPopup(scaleStep, s -> {
            scaleStep = s;
            scaleEstimateNumberModel.setStepSize(s);
            scaleErrorNumberModel.setStepSize(s);
        }, scaleEstimateSpinner, scaleErrorSpinner);
        addWheel(scaleEstimateSpinner, scaleEstimateNumberModel);
        addWheel(scaleErrorSpinner, scaleErrorNumberModel);
        g.add(r1);

        // Sky location
        var r2 = row();
        raDecCB = new JCheckBox("Constrain sky location", useRaDec);
        raDecCB.setFont(p12);
        if (!useCCRaDec) {
            raDecCB.setToolTipText("<html>When enabled, the astometry.net sky location search is limited to<br>" +
                    "the RA/Dec location +/- the specified radius.<br>" +
                    "Constraining the field's location may reduce solve time.</html>");
        } else {
            raDecCB.setToolTipText("<html>When enabled, the astometry.net sky location search is limited to<br>" +
                    "the RA/Dec location +/- the specified radius. When running Data Processor,<br>" +
                    "RA and Dec are automatically populated from the 'Target Coordinate Source' specifed<br>" +
                    "to the right. Constraining the field's location on the sky may reduce solve time.</html>");
        }
        raDecCB.addItemListener(this);
        r2.add(raDecCB);

        raTF = new JTextField(showSexagesimal ? decToSex(ra, 3, 24, false) : sixPlaces.format(ra), 11);
        raTF.setMargin(new Insets(2, 2, 2, 2));
        raTF.setFont(p12);
        raTF.setEnabled(useRaDec && !useCCRaDec);
        raTF.setHorizontalAlignment(JTextField.LEFT);
        if (!useCCRaDec) {
            raTF.setToolTipText("<html>Enter the Right Ascension of the center of the search area and press &lt;Enter&gt;.<br>" +
                    "Both sexegesimal (HH:MM:SS.ss) or decimal (HH.HHHH) hour formats are supported.<br>" +
                    "Both RA and Dec may be pasted into this field together. Dec will be parsed<br>" +
                    "and placed in the 'Dec' text box.</html>");
        } else {
            raTF.setToolTipText("<html>RA and Dec are automatically populated from the 'Target Coordinate Source'<br>" +
                    "specifed in the Data Processor panel.</html>");
        }
        raTF.addActionListener(this);
        raTF.addFocusListener(this);
        r2.add(titled("Center RA (Hours)", raTF));

        decTF = new JTextField(showSexagesimal ? decToSex(dec, 2, 90, true) : sixPlaces.format(dec), 11);
        decTF.setMargin(new Insets(2, 2, 2, 2));
        decTF.setFont(p12);
        decTF.setEnabled(useRaDec && !useCCRaDec);
        decTF.setHorizontalAlignment(JTextField.LEFT);
        if (!useCCRaDec) {
            decTF.setToolTipText("<html>Enter the Declination of the center of the search area and press &lt;Enter&gt;.<br>" +
                    "Both sexegesimal (DD:MM:SS.ss) or decimal (DD.DDDD) degree formats are supported.<br>" +
                    "Both RA and Dec may be pasted into this field together. RA will be parsed<br>" +
                    "and placed in the 'RA' text box.</html>");
        } else {
            decTF.setToolTipText("<html>RA and Dec are automatically populated from the 'Target Coordinate Source'<br>" +
                    "specifed in the Data Processor panel.</html>");
        }
        decTF.addActionListener(this);
        decTF.addFocusListener(this);
        r2.add(titled("Center Dec (Degrees)", decTF));

        raDecRadiusNumberModel = new SpinnerNumberModel(raDecRadius, 0.0, null, raDecRadiusStep);
        raDecRadiusSpinner = new JSpinner(raDecRadiusNumberModel);
        raDecRadiusSpinner.setEnabled(useRaDec);
        raDecRadiusSpinner.setFont(p11);
        raDecRadiusSpinner.setEditor(new JSpinner.NumberEditor(raDecRadiusSpinner, "0.0####"));
        raDecRadiusSpinner.setToolTipText("<html>If enabled, this value is used as the search radius by astometry.net.<br>" +
                "The radius (in minutes of arc) is centered on the RA and Dec values specified.<br>" +
                "Other parts of the sky will not be searched. Constraining the sky location and<br>" +
                "search radius may reduce solve time, or if specified incorrectly, may result in<br>" +
                "a plate solve failure.<br>" +
                "Right click to set spinner stepsize.<br>" +
                "NOTE: Radius should be at least the size of the largest CCD dimension.</html>");
        raDecRadiusSpinner.addChangeListener(_ -> raDecRadius = value(raDecRadiusSpinner));
        installStepPopup(raDecRadiusStep, s -> {
            raDecRadiusStep = s;
            raDecRadiusNumberModel.setStepSize(s);
        }, raDecRadiusSpinner);
        addWheel(raDecRadiusSpinner, raDecRadiusNumberModel);
        setWidth(raDecRadiusSpinner, 100);
        r2.add(titled("Radius (arcmin)", raDecRadiusSpinner));

        // Reuse previous
        var r3 = row();
        var reuseRadec = new JCheckBox("Reuse previous RA/Dec");
        reuseRadec.setFont(p12);
        reuseRadec.setToolTipText("""
                If checked, the previous RA/Dec will be used as a starting point for the plate solve.
                This is useful if the image is a continuation of a previous image.
                Overrides "Constrain sky location" for subsequent images""");
        reuseRadec.setSelected(Astrometry.REUSE_PREVIOUS_RADEC.get());
        reuseRadec.addActionListener(_ -> Astrometry.REUSE_PREVIOUS_RADEC.set(reuseRadec.isSelected()));
        r3.add(reuseRadec);
        g.add(r2);
        g.add(r3);

        return g;
    }

    private JPanel buildDistortionPanel() {
        var g = group("Distortion Correction");
        var r = row();

        useDistortionOrderCB = new JCheckBox("SIP distortion correction", useDistortionOrder);
        useDistortionOrderCB.setFont(p12);
        useDistortionOrderCB.setToolTipText("""
                <html>Enable to request SIP distortion correction as part of the plate solve.<br>\
                NOTE: If disabled, astrometry.net will not use distortion correction<br>\
                which may result in a non-optimal plate solve.</html>""");
        useDistortionOrderCB.addItemListener(this);
        r.add(useDistortionOrderCB);

        if (distortionOrder < minOrder) distortionOrder = minOrder;
        if (distortionOrder > maxOrder) distortionOrder = maxOrder;
        distortionOrderNumberModel = new SpinnerNumberModel(distortionOrder, minOrder, maxOrder, 1);
        distortionOrderSpinner = new JSpinner(distortionOrderNumberModel);
        distortionOrderSpinner.setEditor(new JSpinner.NumberEditor(distortionOrderSpinner, "#"));
        distortionOrderSpinner.setFont(p12);
        distortionOrderSpinner.setToolTipText("""
                <html>The SIP distortion correction order requested for the plate solve (if enabled).<br>\
                NOTE: A SIP order of 2 is the default and is recommended for typical telescope images.<br>\
                NOTE: Wide field images may require orders up to 5 for an accurate plate solve.</html>""");
        distortionOrderSpinner.setEnabled(useDistortionOrder);
        distortionOrderSpinner.addChangeListener(this);
        distortionOrderSpinner.addMouseWheelListener(this);
        setWidth(distortionOrderSpinner, 80);
        r.add(titled("SIP Order", distortionOrderSpinner));

        g.add(r);
        return g;
    }

    private JPanel buildButtonBar() {
        var bar = new JPanel(new FlowLayout(FlowLayout.CENTER, 12, 8));

        startButton = new JButton(startButtonLabel);
        startButton.setToolTipText(notDP ? "<html>Save new settings and start processing</html>"
                : "<html>Save new settings and close panel</html>");
        startButton.setMargin(new Insets(5, 25, 5, 25));
        startButton.setPreferredSize(new Dimension(125, 30));
        startButton.setFont(b12);
        startButton.addActionListener(this);
        bar.add(startButton);

        cancelButton = new JButton(notDP ? "CANCEL" : "SAVE");
        cancelButton.setToolTipText(notDP ? "<html>Close panel and abort plate solve request</html>"
                : "<html>Saving new settings</html>");
        cancelButton.setMargin(new Insets(5, 25, 5, 25));
        cancelButton.setPreferredSize(new Dimension(125, 30));
        cancelButton.setFont(b12);
        cancelButton.addActionListener(this);
        bar.add(cancelButton);

        return bar;
    }

    private JPanel group(String title) {
        var g = new JPanel();
        g.setLayout(new BoxLayout(g, BoxLayout.Y_AXIS));
        TitledBorder tb = BorderFactory.createTitledBorder(BorderFactory.createLineBorder(mainBorderColor), title,
                TitledBorder.LEFT, TitledBorder.TOP, b12);
        g.setBorder(BorderFactory.createCompoundBorder(tb, BorderFactory.createEmptyBorder(0, 4, 2, 4)));
        return g;
    }

    private JPanel row() {
        var r = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 2));
        r.setAlignmentX(Component.LEFT_ALIGNMENT);
        return r;
    }

    private JPanel titled(String title, JComponent c) {
        var p = new JPanel(new BorderLayout()) {
            @Override
            public Dimension getPreferredSize() {
                Dimension d = super.getPreferredSize();
                Dimension b = ((TitledBorder) getBorder()).getMinimumSize(this);
                return new Dimension(Math.max(d.width, b.width + 16), d.height);
            }
        };
        p.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEmptyBorder(), title, TitledBorder.CENTER, TitledBorder.TOP, p11));
        p.add(c, BorderLayout.CENTER);
        return p;
    }

    private JLabel label(String text) {
        var l = new JLabel(text);
        l.setFont(p12);
        return l;
    }

    private void setWidth(JComponent c, int width) {
        var d = c.getPreferredSize();
        c.setPreferredSize(new Dimension(width, Math.max(d.height, 22)));
    }

    private void addCell(JPanel parent, JComponent c, int row, int col, int span) {
        var gbc = new GridBagConstraints();
        gbc.gridx = col;
        gbc.gridy = row;
        gbc.gridwidth = span;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.insets = new Insets(0, 0, 6, (col == 0 && span == 1) ? 6 : 0);
        parent.add(c, gbc);
    }

    private JPanel wrapTop(JPanel inner) {
        var outer = new JPanel(new BorderLayout());
        outer.add(inner, BorderLayout.NORTH);
        return outer;
    }

    private double value(JSpinner s) {
        return ((Number) s.getValue()).doubleValue();
    }

    private void addWheel(JSpinner spinner, SpinnerNumberModel model) {
        spinner.addMouseWheelListener(e -> {
            if (!spinner.isEnabled()) {
                return;
            }
            var v = value(spinner) - e.getWheelRotation() * model.getStepSize().doubleValue();
            if (model.getMinimum() instanceof Number min && v < min.doubleValue()) {
                v = min.doubleValue();
            }
            spinner.setValue(v);
        });
    }

    private void installStepPopup(double initialStep, DoubleConsumer onStepChange, JSpinner... targets) {
        var popup = new JPopupMenu();
        var panel = new JPanel();
        var stepSpinner = new JSpinner(new SpinnerListModel(spinnerScaleList));
        stepSpinner.setValue(convertToText(initialStep));
        stepSpinner.addChangeListener(_ -> {
            double v = IJU.getTextSpinnerDoubleValue(stepSpinner);
            if (Double.isNaN(v)) {
                return;
            }
            onStepChange.accept(v);
        });
        panel.add(new JLabel("Stepsize:"));
        panel.add(stepSpinner);
        popup.add(panel);
        popup.setLightWeightPopupEnabled(false);
        popup.addPopupMenuListener(new PopupMenuListener() {
            public void popupMenuCanceled(PopupMenuEvent e) {
                popup.setVisible(false);
            }

            public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
                popup.setVisible(true);
            }

            public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
                popup.setVisible(false);
            }
        });
        for (var t : targets) {
            t.setComponentPopupMenu(popup);
        }
    }

    private void syncApertureModels() {
        if (syncingAperture) return;
        syncingAperture = true;
        try {
            if (apertureBack1 < apertureRadius) apertureBack1 = apertureRadius;
            if (apertureBack2 < apertureBack1 + 1) apertureBack2 = apertureBack1 + 1;
            apertureBack1NumberModel.setMinimum(apertureRadius);
            apertureBack2NumberModel.setMinimum(apertureBack1 + 1);
            apertureBack1NumberModel.setValue(apertureBack1);
            apertureBack2NumberModel.setValue(apertureBack2);
        } finally {
            syncingAperture = false;
        }
    }

    public void getFields() {
        userKey = keyTF.getText().trim();
        if (useAlternateAstrometryServer) alternateAstrometryUrlBase = alternateAstrometryUrlBaseTF.getText().trim();
    }


    public void itemStateChanged(ItemEvent e) {
        Object source = e.getItemSelectable();
        boolean selected = (e.getStateChange() == ItemEvent.SELECTED);
        if (source.equals(autoSaveCB)) {
            if (notDP) {
                autoSave = selected;
            } else {
                DPSaveRawWithWCS = selected;
            }
            fpackBox.setEnabled(selected);
            compressBox.setEnabled(selected);
        } else if (source.equals(useAlternateAstrometryServerCB)) {
            useAlternateAstrometryServer = selected;
            alternateAstrometryUrlBaseTF.setText(useAlternateAstrometryServer ? alternateAstrometryUrlBase : defaultAstrometryUrlBase);
            alternateAstrometryUrlBaseTF.setEnabled(useAlternateAstrometryServer);
            customPollSpinner.setValue(selected ? Astrometry.CUSTOM_POLLING.get() : Astrometry.ASTRO_NET_POLLING.get());
        } else if (source.equals(skipIfHasWCSCB)) {
            skipIfHasWCS = selected;
        } else if (source.equals(compressBox)) {
            compress = selected;
        } else if (source.equals(fpackBox)) {
            fpack = selected;
        } else if (source.equals(processStackCB)) {
            processStack = selected;
            startSliceSpinner.setEnabled(selected);
            endSliceSpinner.setEnabled(selected);
        } else if (source.equals(annotateCB)) {
            annotate = selected;
            annotateRadiusSpinner.setEnabled(selected);
            addAnnotationsToHeaderCB.setEnabled(selected);
        } else if (source.equals(addAnnotationsToHeaderCB)) {
            addAnnotationsToHeader = selected;
        } else if (source.equals(useMedianFilterCB)) {
            useMedianFilter = selected;
            medianFilterRadiusSpinner.setEnabled(selected);
        } else if (source.equals(useMaxPeakFindValueCB)) {
            useMaxPeakFindValue = selected;
            maxPeakFindSpinner.setEnabled(selected);
        } else if (source.equals(centroidCB)) {
            useCentroid = selected;
            apertureRadiusSpinner.setEnabled(selected);
            apertureBack1Spinner.setEnabled(selected);
            apertureBack2Spinner.setEnabled(selected);
        } else if (source.equals(scaleCB)) {
            useScale = selected;
            scaleEstimateSpinner.setEnabled(selected);
            scaleErrorSpinner.setEnabled(selected);
        } else if (source.equals(raDecCB)) {
            useRaDec = selected;
            raTF.setEnabled(selected && notDP);
            decTF.setEnabled(selected && notDP);
            raDecRadiusSpinner.setEnabled(selected);
        } else if (source.equals(showLogCB)) {
            showLog = selected;
        } else if (source.equals(useDistortionOrderCB)) {
            useDistortionOrder = selected;
            distortionOrderSpinner.setEnabled(selected);
        }
    }

    /**
     * Response to buttons being pushed.
     */
    public void actionPerformed(ActionEvent ae) {
        Object source = ae.getSource();
        if (source == startButton) {
            if (useRaDec && !useCCRaDec) {
                double[] coords = processCoordinatePair(raTF, 3, 24, false, decTF, 2, 90, true, true, true);
                if (!Double.isNaN(coords[0])) {
                    ra = coords[0];
                    raTF.setText(showSexagesimal ? decToSex(ra, 3, 24, false) : sixPlaces.format(ra));
                } else {
                    IJ.showMessage("Bad RA Entry");
                    return;
                }
                coords = processCoordinatePair(raTF, 3, 24, false, decTF, 2, 90, true, false, true);
                if (!Double.isNaN(coords[1])) {
                    dec = coords[1];
                    decTF.setText(showSexagesimal ? decToSex(dec, 2, 90, true) : sixPlaces.format(dec));
                } else {
                    IJ.showMessage("Bad Dec Entry");
                    return;
                }
            }
            canceled = false;
            exit();
            done = true;
        } else if (source == cancelButton) {
            if (notDP) {
                canceled = true;
                exit();
                done = true;
            } else {
                savePrefs();
            }
        } else if (source == raTF || source == decTF) {
            double[] coords = processCoordinatePair(raTF, 3, 24, false, decTF, 2, 90, true, source == raTF, true);
            if (!Double.isNaN(coords[0])) ra = coords[0];
            if (!Double.isNaN(coords[1])) dec = coords[1];
        }
    }


    public void stateChanged(ChangeEvent ev) {
        if (ev.getSource() == startSliceSpinner) {
            startSlice = (Integer) startSliceSpinner.getValue();
            endSlice = (Integer) endSliceSpinner.getValue();
            if (startSlice < fSlice) startSlice = fSlice;
            if (startSlice > endSlice) startSlice = endSlice;
            startSliceNumberModel = new SpinnerNumberModel(startSlice, fSlice, endSlice, 1);
            startSliceSpinner.setModel(startSliceNumberModel);
            endSliceNumberModel = new SpinnerNumberModel(endSlice, startSlice, lSlice, 1);
            endSliceSpinner.setModel(endSliceNumberModel);
        } else if (ev.getSource() == endSliceSpinner) {
            startSlice = (Integer) startSliceSpinner.getValue();
            endSlice = (Integer) endSliceSpinner.getValue();
            if (endSlice > lSlice) endSlice = lSlice;
            if (endSlice < startSlice) endSlice = startSlice;
            startSliceNumberModel = new SpinnerNumberModel(startSlice, fSlice, endSlice, 1);
            startSliceSpinner.setModel(startSliceNumberModel);
            endSliceNumberModel = new SpinnerNumberModel(endSlice, startSlice, lSlice, 1);
            endSliceSpinner.setModel(endSliceNumberModel);
        } else if (ev.getSource() == medianFilterRadiusSpinner) {
            medianFilterRadius = (Integer) medianFilterRadiusSpinner.getValue();
            if (medianFilterRadius < 2) medianFilterRadius = 2;
        } else if (ev.getSource() == maxNumStarsSpinner) {
            maxNumStars = (Integer) maxNumStarsSpinner.getValue();
            if (maxNumStars < MIN_MAX_STARS) maxNumStars = MIN_MAX_STARS;
        } else if (ev.getSource() == distortionOrderSpinner) {
            distortionOrder = (Integer) distortionOrderSpinner.getValue();
            if (distortionOrder < minOrder) distortionOrder = minOrder;
            if (distortionOrder > maxOrder) distortionOrder = maxOrder;
        } else if (ev.getSource().equals(customPollSpinner)) {
            if (useAlternateAstrometryServer) {
                Astrometry.CUSTOM_POLLING.set((Integer) customPollSpinner.getValue());
            } else {
                Astrometry.ASTRO_NET_POLLING.set((Integer) customPollSpinner.getValue());
            }
        }

    }

    public void focusGained(FocusEvent ev) {
    }

    public void focusLost(FocusEvent ev) {
        if (ev.getSource() == raTF) {
            if (useRaDec && !useCCRaDec) {
                double[] coords = processCoordinatePair(raTF, 3, 24, false, decTF, 2, 90, true, true, true);
                if (!Double.isNaN(coords[0])) {
                    ra = coords[0];
                    raTF.setText(showSexagesimal ? decToSex(ra, 3, 24, false) : sixPlaces.format(ra));
                } else {
                    IJ.showMessage("Bad RA Entry");
                }
            }
        }
        if (ev.getSource() == decTF) {
            if (useRaDec && !useCCRaDec) {
                double[] coords = processCoordinatePair(raTF, 3, 24, false, decTF, 2, 90, true, false, true);
                if (!Double.isNaN(coords[1])) {
                    dec = coords[1];
                    decTF.setText(showSexagesimal ? decToSex(dec, 2, 90, true) : sixPlaces.format(dec));
                } else {
                    IJ.showMessage("Bad Dec Entry");
                }
            }
        }
    }


    public void mouseWheelMoved(MouseWheelEvent e) {
        if (e.getSource() == startSliceSpinner && startSliceSpinner.isEnabled()) {
            int endValue = (Integer) endSliceSpinner.getValue();
            int newValue = (Integer) startSliceSpinner.getValue() - e.getWheelRotation();
            if (newValue >= fSlice && newValue <= endValue) {
                startSliceSpinner.setValue(newValue);
            } else if (newValue < fSlice) {
                startSliceSpinner.setValue(fSlice);
            } else {
                startSliceSpinner.setValue(endValue);
            }
        } else if (e.getSource() == endSliceSpinner && endSliceSpinner.isEnabled()) {
            int startValue = (Integer) startSliceSpinner.getValue();
            int newValue = (Integer) endSliceSpinner.getValue() - e.getWheelRotation();
            if (newValue >= startValue && newValue <= lSlice) {
                endSliceSpinner.setValue(newValue);
            } else if (newValue < startValue) {
                endSliceSpinner.setValue(startValue);
            } else {
                endSliceSpinner.setValue(lSlice);
            }
        } else if (e.getSource() == medianFilterRadiusSpinner && medianFilterRadiusSpinner.isEnabled()) {
            medianFilterRadius = (Integer) medianFilterRadiusSpinner.getValue() - e.getWheelRotation();
            if (medianFilterRadius < 2) medianFilterRadius = 2;
            medianFilterRadiusSpinner.setValue(medianFilterRadius);
        } else if (e.getSource() == maxNumStarsSpinner) {
            maxNumStars = (Integer) maxNumStarsSpinner.getValue() - e.getWheelRotation();
            if (maxNumStars < MIN_MAX_STARS) maxNumStars = MIN_MAX_STARS;
            maxNumStarsSpinner.setValue(maxNumStars);
        } else if (e.getSource() == distortionOrderSpinner && distortionOrderSpinner.isEnabled()) {
            distortionOrder = (Integer) distortionOrderSpinner.getValue() - e.getWheelRotation();
            if (distortionOrder < minOrder) distortionOrder = minOrder;
            if (distortionOrder > maxOrder) distortionOrder = maxOrder;
            distortionOrderSpinner.setValue(distortionOrder);
        }
    }


    public void exit() {
        savePrefs();
        astrometrySetupFrame.dispose();
    }

    double[] processCoordinatePair(JTextField textFieldA, int decimalPlacesA, int baseA, boolean showSignA,
                                   JTextField textFieldB, int decimalPlacesB, int baseB, boolean showSignB, boolean AIsSource, boolean update) {
        double X = Double.NaN;
        double Y = Double.NaN;
        int YstartPosition = 1;

        boolean XNegative = false;
        boolean YNegative = false;
        String text = AIsSource ? textFieldA.getText().trim() : textFieldB.getText().trim();
        String[] pieces = text.replaceAll("[\\-][^0-9\\.]{0,}", " \\-").replaceAll("[+][^0-9\\.]{0,}", " +").replaceAll("[^0-9\\.\\-+]{1,}", " ").trim().split("[^0-9\\.\\-+]{1,}");

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
            if (pieces.length > YstartPosition + 2) {
                Y += Math.abs(Tools.parseDouble(pieces[YstartPosition + 2])) / 3600.0;
            }
        } else if (pieces.length > 0 && AIsSource) {
            text = textFieldB.getText().trim();
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
            text = textFieldA.getText().trim();
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
        if (!Double.isNaN(X) && update) {
            textFieldA.setText(showSexagesimal ? decToSex(X, decimalPlacesA, baseA, showSignA) : sixPlaces.format(X));
        }
        Y = mapToBase(Y, baseB, YNegative);
        if (!Double.isNaN(Y) && update) {
            textFieldB.setText(showSexagesimal ? decToSex(Y, decimalPlacesB, baseB, showSignB) : sixPlaces.format(Y));
        }
        return new double[] {X, Y};
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
        nf.setDecimalFormatSymbols(IJU.dfs);
        nf2x.setDecimalFormatSymbols(IJU.dfs);
        nf.setMinimumIntegerDigits(2);
        nf2x.setMinimumIntegerDigits(2);
        nf2x.setMinimumFractionDigits(0);
        nf2x.setMaximumFractionDigits(fractionPlaces);

        boolean ampm = false;
        boolean pm = false;
        if (base == 1224) {
            ampm = true;
            if (d >= 12.0) {
                d -= 12.0;
                pm = true;
            }
        }

        double dd = Math.abs(d);

        int h = (int) dd;
        int m = (int) (60.0 * (dd - (double) h));
        double s = 3600.0 * (dd - (double) h - (double) m / 60.0);

        if (Tools.parseDouble(nf2x.format(s).trim()) >= 60.0) {
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
            } else {
                h -= base;
            }
        } else if (base == 90 && d < -90) {
            h = 90;
            m = 0;
            s = 0;
        }

        if (ampm && h == 0) h = 12;
        String str = "";
        if (d < 0.0) {
            str = "-";
        } else if (showPlus) str = "+";
        str += nf.format(h) + ":" + nf.format(m) + ":" + nf2x.format(s);
        if (ampm) str += pm ? " PM" : " AM";
        return str;
    }

    double mapToBase(double num, int base, boolean negative) {
        double x = num;
        if (base == 90) {
            x = x >= 90 ? (negative ? -90 : 90) : (negative ? -x : x);  //-89.999722 : 89.999722
        } else if (base == 180 || base == 12) {
            x %= 2 * base;
            x = x > base ? -2 * base + x : x;
            x = negative ? -x : x;
        } else {
            x %= base;
            x = negative ? base - x : x;
        }
        return x;
    }

    /**
     * Returns an ImageIcon, or null if the path was invalid.
     */
    protected ImageIcon createImageIcon(String path, String description) {
        java.net.URL imgURL = getClass().getClassLoader().getResource(path);
        if (imgURL != null) {
            return new ImageIcon(imgURL, description);
        } else {
            IJ.showMessage("Couldn't find icon file: " + path);
            return null;
        }
    }

    String convertToText(double increment) {
        if (increment == 0.0000000001) {
            return "      0.0000000001";
        } else if (increment == 0.000000001) {
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
        } else {
            return "                    1.000";
        }
    }

    String convertToText(int increment) {
        if (increment == 1) {
            return "                    1";
        } else if (increment == 10) {
            return "                  10";
        } else if (increment == 100) {
            return "                100";
        } else if (increment == 1000) {
            return "              1000";
        } else if (increment == 10000) {
            return "            10000";
        } else if (increment == 100000) {
            return "          100000";
        } else if (increment == 1000000) {
            return "        1000000";
        } else if (increment == 10000000) {
            return "      10000000";
        } else if (increment == 100000000) {
            return "    100000000";
        } else if (increment == 1000000000) {
            return "1000000000";
        } else {
            return Integer.toString(increment);
        }
    }

    public void getPrefs() {
        frameLocationX = (int) Prefs.get("astrometry.frameLocationX", frameLocationX);
        frameLocationY = (int) Prefs.get("astrometry.frameLocationY", frameLocationY);

        userKey = Prefs.get("astrometry.userKey", userKey);

        useAlternateAstrometryServer = Prefs.get("astrometry.useAlternateAstrometryServer", useAlternateAstrometryServer);
        alternateAstrometryUrlBase = Prefs.get("astrometry.alternateAstrometryUrlBase", alternateAstrometryUrlBase);

        processStack = Prefs.get("astrometry.processStack", processStack);
        startSlice = (int) Prefs.get("astrometry.startSlice", startSlice);
        endSlice = (int) Prefs.get("astrometry.endSlice", endSlice);

        autoSave = Prefs.get("astrometry.autoSave", autoSave);
        DPSaveRawWithWCS = Prefs.get("astrometry.DPSaveRawWithWCS", DPSaveRawWithWCS);
        skipIfHasWCS = Prefs.get("astrometry.skipIfHasWCS", skipIfHasWCS);

        compress = Prefs.get("astrometry.gzip", compress);
        fpack = Prefs.get("astrometry.fpack", fpack);

        annotate = Prefs.get("astrometry.annotate", annotate);
        annotateRadius = Prefs.get("astrometry.annotateRadius", annotateRadius);
        annotateRadiusStep = Prefs.get("astrometry.annotateRadiusStep", annotateRadiusStep);
        addAnnotationsToHeader = Prefs.get("astrometry.addAnnotationsToHeader", addAnnotationsToHeader);

        useMedianFilter = Prefs.get("astrometry.useMedianFilter", useMedianFilter);
        medianFilterRadius = (int) Prefs.get("astrometry.medianFilterRadius", medianFilterRadius);

        minPeakFindToleranceSTDEV = Prefs.get("astrometry.minPeakFindToleranceSTDEV", minPeakFindToleranceSTDEV);
        useMaxPeakFindValue = Prefs.get("astrometry.useMaxPeakFindValue", useMaxPeakFindValue);
        maxPeakFindValue = Prefs.get("astrometry.maxPeakFindValue", maxPeakFindValue);

        maxNumStars = (int) Prefs.get("astrometry.maxNumStars", maxNumStars);
        useDistortionOrder = Prefs.get("astrometry.useDistortionOrder", useDistortionOrder);
        distortionOrder = (int) Prefs.get("astrometry.distortionOrder", distortionOrder);

        useCentroid = Prefs.get("astrometry.useCentroid", useCentroid);
        apertureRadius = Prefs.get("astrometry.apertureRadius", apertureRadius);
        apertureBack1 = Prefs.get("astrometry.apertureBack1", apertureBack1);
        apertureBack2 = Prefs.get("astrometry.apertureBack2", apertureBack2);

        useScale = Prefs.get("astrometry.useScale", useScale);
        scaleEstimate = Prefs.get("astrometry.scaleEstimate", scaleEstimate);
        scaleError = Prefs.get("astrometry.scaleError", scaleError);


        useRaDec = Prefs.get("astrometry.useRaDec", useRaDec);
        showLog = Prefs.get("astrometry.showLog", showLog);
        ra = Prefs.get("astrometry.ra", ra);
        dec = Prefs.get("astrometry.dec", dec);
        raDecRadius = Prefs.get("astrometry.raDecRadius", raDecRadius);
        raDecRadiusStep = Prefs.get("astrometry.raDecRadiusStep", raDecRadiusStep);

        noiseTolStep = Prefs.get("astrometry.noiseTolStep", noiseTolStep);
        maxPeakFindStep = Prefs.get("astrometry.maxPeakFindStep", maxPeakFindStep);
        apertureStep = Prefs.get("astrometry.apertureStep", apertureStep);
        scaleStep = Prefs.get("astrometry.scaleStep", scaleStep);
    }

    public void savePrefs() {

        frameLocationX = astrometrySetupFrame.getLocation().x;
        frameLocationY = astrometrySetupFrame.getLocation().y;
        Prefs.set("astrometry.frameLocationX", frameLocationX);
        Prefs.set("astrometry.frameLocationY", frameLocationY);
        Prefs.set("astrometry.userKey", userKey);
        Prefs.set("astrometry.useAlternateAstrometryServer", useAlternateAstrometryServer);
        Prefs.set("astrometry.alternateAstrometryUrlBase", alternateAstrometryUrlBase);
        Prefs.set("astrometry.processStack", processStack);
        Prefs.set("astrometry.startSlice", startSlice);
        Prefs.set("astrometry.endSlice", endSlice);

        Prefs.set("astrometry.autoSave", autoSave);
        Prefs.set("astrometry.DPSaveRawWithWCS", DPSaveRawWithWCS);
        Prefs.set("astrometry.skipIfHasWCS", skipIfHasWCS);

        Prefs.set("astrometry.gzip", compress);
        Prefs.set("astrometry.fpack", fpack);

        Prefs.set("astrometry.annotate", annotate);
        Prefs.set("astrometry.annotateRadius", annotateRadius);
        Prefs.set("astrometry.annotateRadiusStep", annotateRadiusStep);
        Prefs.set("astrometry.addAnnotationsToHeader", addAnnotationsToHeader);

        Prefs.set("astrometry.useMedianFilter", useMedianFilter);
        Prefs.set("astrometry.medianFilterRadius", medianFilterRadius);

        Prefs.set("astrometry.minPeakFindToleranceSTDEV", minPeakFindToleranceSTDEV);
        Prefs.set("astrometry.useMaxPeakFindValue", useMaxPeakFindValue);
        Prefs.set("astrometry.maxPeakFindValue", maxPeakFindValue);

        Prefs.set("astrometry.maxNumStars", maxNumStars);

        Prefs.set("astrometry.useDistortionOrder", useDistortionOrder);
        Prefs.set("astrometry.distortionOrder", distortionOrder);

        Prefs.set("astrometry.useCentroid", useCentroid);
        Prefs.set("astrometry.apertureRadius", apertureRadius);
        Prefs.set("astrometry.apertureBack1", apertureBack1);
        Prefs.set("astrometry.apertureBack2", apertureBack2);

        Prefs.set("astrometry.useScale", useScale);
        Prefs.set("astrometry.scaleEstimate", scaleEstimate);
        Prefs.set("astrometry.scaleError", scaleError);


        Prefs.set("astrometry.useRaDec", useRaDec);
        if (!useCCRaDec) Prefs.set("astrometry.ra", ra);
        if (!useCCRaDec) Prefs.set("astrometry.dec", dec);
        Prefs.set("astrometry.raDecRadius", raDecRadius);
        Prefs.set("astrometry.raDecRadiusStep", raDecRadiusStep);

        Prefs.set("astrometry.noiseTolStep", noiseTolStep);
        Prefs.set("astrometry.maxPeakFindStep", maxPeakFindStep);
        Prefs.set("astrometry.apertureStep", apertureStep);
        Prefs.set("astrometry.scaleStep", scaleStep);
        Prefs.set("astrometry.showLog", showLog);
    }

    class thisDocumentListener implements DocumentListener {
        public void insertUpdate(DocumentEvent ev) {
            getFields();
        }

        public void removeUpdate(DocumentEvent ev) {
            getFields();
        }

        public void changedUpdate(DocumentEvent ev) {
            getFields();
        }

    }

}
