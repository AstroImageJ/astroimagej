package Astronomy.multiaperture;

import astroj.Centroid;
import astroj.FreeformPixelApertureRoi;
import astroj.OverlayCanvas;
import ij.IJ;
import ij.ImagePlus;
import ij.Prefs;
import ij.astro.gui.GenericSwingDialog;
import ij.astro.io.prefs.Property;
import ij.astro.util.UIHelper;
import ij.gui.GUI;
import ij.gui.ImageCanvas;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ItemEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.List;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.DoubleConsumer;
import java.util.function.DoubleSupplier;
import java.util.function.IntConsumer;

import static Astronomy.Aperture_.*;

public class FreeformPixelApertureHandler {
    private final List<FreeformPixelApertureRoi> freeformPixelApertureRois = new ArrayList<>();
    private int selectedAperture = 1;
    private JFrame controlPanel;
    private Runnable playCallback;
    private Runnable shutdownCallback;
    private ImagePlus imp;
    private boolean invertBackground = false;
    private boolean copyBackground = false;
    private final boolean starOverlay = Prefs.get(AP_PREFS_STAROVERLAY, true);
    private final boolean skyOverlay = Prefs.get(AP_PREFS_SKYOVERLAY, true);
    private final boolean nameOverlay = Prefs.get(AP_PREFS_NAMEOVERLAY, true);
    private final boolean valueOverlay = Prefs.get(AP_PREFS_VALUEOVERLAY, true);
    private final boolean tempOverlay = Prefs.get(AP_PREFS_TEMPOVERLAY, true);
    private final boolean clearOverlay = Prefs.get(AP_PREFS_CLEAROVERLAY, false);
    private final boolean usePlane = Prefs.get(AP_PREFS_BACKPLANE, false);
    private final boolean removeStars = Prefs.get(AP_PREFS_REMOVEBACKSTARS, false);
    private IntConsumer updateCount = i -> {};
    public static final Property<List<FreeformPixelApertureRoi>> APS =
            new Property<>(new ArrayList<>(),
                    FreeformPixelApertureHandler::serializeApertures, FreeformPixelApertureHandler::deserializeApertures,
                    FreeformPixelApertureHandler.class);
    public static final Property<List<FreeformPixelApertureRoi>> IMPORTED_APS =
            new Property<>(new ArrayList<>(),
                    FreeformPixelApertureHandler::serializeApertures, FreeformPixelApertureHandler::deserializeApertures,
                    FreeformPixelApertureHandler.class);
    private static final Property<Point> WINDOW_LOCATION = new Property<>(new Point(), FreeformPixelApertureHandler.class);
    public static final Property<Boolean> SHOW_ESTIMATED_CIRCULAR_APERTURE = new Property<>(false, FreeformPixelApertureHandler.class);
    public static final Property<Boolean> SHOW_CENTROID_RADIUS = new Property<>(false, FreeformPixelApertureHandler.class);
    private static final Property<Boolean> ALWAYS_ON_TOP = new Property<>(true, FreeformPixelApertureHandler.class);
    private static final Property<Boolean> CENTROID_ON_COPY = new Property<>(false, FreeformPixelApertureHandler.class);
    private static final Property<Boolean> COPY_T1 = new Property<>(false, FreeformPixelApertureHandler.class);
    private static final int WIDTH = 25;
    private static final int HEIGHT = 25;
    private static final Icon ADD_ICON = UIHelper.createImageIcon("Astronomy/images/icons/multiaperture/add.png", WIDTH, HEIGHT);
    private static final Icon REMOVE_ICON = UIHelper.createImageIcon("Astronomy/images/icons/multiaperture/remove.png", WIDTH, HEIGHT);
    private static final Icon PLAY_ICON = UIHelper.createImageIcon("Astronomy/images/icons/multiaperture/play.png", WIDTH, HEIGHT);
    private static final Icon TO_FRONT_ICON = UIHelper.createImageIcon("Astronomy/images/icons/multiaperture/flipToFront.png", WIDTH, HEIGHT);
    private static final Icon TO_BACK_ICON = UIHelper.createImageIcon("Astronomy/images/icons/multiaperture/flipToBack.png", WIDTH, HEIGHT);
    private static final Icon WARNING_ICON = UIHelper.createImageIcon("Astronomy/images/icons/multiaperture/warning.png", WIDTH, HEIGHT);
    private static final Icon SOURCE_ICON = UIHelper.createImageIcon("Astronomy/images/icons/multiaperture/greenCircle.png", WIDTH, HEIGHT);
    private static final Icon BACKGROUND_ICON = UIHelper.createImageIcon("Astronomy/images/icons/multiaperture/redCircle.png", WIDTH, HEIGHT);
    private static final Icon COPY_ICON = UIHelper.createImageIcon("Astronomy/images/icons/multiaperture/copy.png", WIDTH, HEIGHT);
    private static final Icon COPY_FULL_ICON = UIHelper.createImageIcon("Astronomy/images/icons/multiaperture/copyFull.png", WIDTH, HEIGHT);
    private static final Icon AUTO_SKY_ICON = UIHelper.createImageIcon("Astronomy/images/icons/multiaperture/autoSky.png", WIDTH, HEIGHT);

    public FreeformPixelApertureHandler() {
        UIHelper.setLookAndFeel();
        this.freeformPixelApertureRois.add(createNewAperture(false));
        updateApertureNames();
        freeformPixelApertureRois.get(0).setFocusedAperture(true);
        controlPanel = createControlPanel();
    }

    public FreeformPixelApertureRoi currentAperture() {
        return freeformPixelApertureRois.get(selectedAperture - 1);
    }

    public int apCount() {
        return freeformPixelApertureRois.size();
    }

    public FreeformPixelApertureRoi getAperture(int index) {
        return freeformPixelApertureRois.get(index);
    }

    public void addPixel(int x, int y, boolean isBackground) {
        if (COPY_T1.get() && !currentAperture().hasSource() && selectedAperture > 1) {
            var t1 = freeformPixelApertureRois.get(0);
            if (t1.hasSource()) {
                t1.copyPixels(currentAperture(), false);
                currentAperture().moveTo(x, y);

                if (CENTROID_ON_COPY.get()) {
                    var c = new Centroid();

                    // Centroid T1 to get offset
                    var dx = 0d;
                    var dy = 0d;
                    if (c.measure(imp, t1, true, usePlane, removeStars)) {
                        dx = t1.getXpos() - c.x();
                        dy = t1.getYpos() - c.y();
                    }

                    if (c.measure(imp, currentAperture(), true, usePlane, removeStars)) {
                        currentAperture().moveTo((int) (c.x() + dx), (int) (c.y() + dy));
                    }
                }

                return;
            }
        }

        addPixel(x, y, isBackground, true);
    }

    public void addPixel(int x, int y, boolean isBackground, boolean updateCenter) {
        currentAperture().addPixel(x, y, invertBackground != isBackground, updateCenter);
    }

    public void removePixel(int x, int y) {
        removePixel(x, y, true);
    }

    public void removePixel(int x, int y, boolean updateCenter) {
        currentAperture().removePixel(x, y, updateCenter);
    }

    public void showControls() {
        if (controlPanel != null) {
            if (!controlPanel.isVisible()) {
                controlPanel.setVisible(true);
            }
        } else {
            controlPanel = createControlPanel();
            controlPanel.setVisible(true);
        }
    }

    public void hideControls() {
        closeControl();
    }

    public void setPlayCallback(Runnable callback) {
        playCallback = callback;
    }

    public void setExitCallback(Runnable callback) {
        shutdownCallback = callback;
    }

    public boolean validateApertures() {
        var invalidCount = 0;
        var message = new StringBuilder();

        for (FreeformPixelApertureRoi roi : freeformPixelApertureRois) {
            if (roi.hasPixels()) {
                if (!roi.hasSource()) {
                    invalidCount++;
                    message.append("%s has no source\n".formatted(roi.getName()));
                }

                if (!roi.hasBackground() && !roi.hasAnnulus()) {
                    invalidCount++;
                    message.append("%s has no background\n".formatted(roi.getName()));
                }

                if (roi.hasAnnulus() && roi.getBack1() >= roi.getBack2()) {
                    invalidCount++;
                    message.append("%s's annulus' radii are inverted\n");
                }
            } else {
                invalidCount++;
                message.append("%s has no pixels\n".formatted(roi.getName()));
            }
        }

        if (invalidCount > 0) {
            message.insert(0, "%s aperture(s) are incomplete!\n".formatted(invalidCount));

            var proceed = IJ.showMessageWithCancel("Custom Aperture Handler", message.toString());

            if (proceed) {
                APS.set(freeformPixelApertureRois);
            }

            return proceed;
        }

        APS.set(freeformPixelApertureRois);

        return true;
    }

    /**
     * Load apertures from preferences, clears existing apertures.
     */
    public void loadAperturesFromPrefs(boolean loadOnlyOne, boolean useImport) {
        freeformPixelApertureRois.clear();
        var setting = useImport ? IMPORTED_APS : APS;
        if (loadOnlyOne && !setting.get().isEmpty()) {
            freeformPixelApertureRois.add(setting.get().get(0));
        } else {
            freeformPixelApertureRois.addAll(setting.get());
        }

        updateDisplay(true);

        if (imp != null) {
            for (FreeformPixelApertureRoi roi : freeformPixelApertureRois) {
                roi.setImage(imp);
                if (OverlayCanvas.hasOverlayCanvas(imp)) {
                    OverlayCanvas.getOverlayCanvas(imp).add(roi);
                }
            }
            (OverlayCanvas.hasOverlayCanvas(imp) ? OverlayCanvas.getOverlayCanvas(imp) : imp.getCanvas()).repaint();
        }

        freeformPixelApertureRois.get(0).setFocusedAperture(true);
        updateCount.accept(freeformPixelApertureRois.size());
    }

    private JFrame createControlPanel() {
        var frame = new JFrame("Aperture Control Panel");
        frame.setIconImage(UIHelper.createImage("astronomy_icon.png"));
        var p = Box.createVerticalBox();
        var firstRow = Box.createHorizontalBox();
        var secondRow = Box.createHorizontalBox();
        var thirdRow = Box.createHorizontalBox();
        var forthRow = Box.createHorizontalBox();
        var fifthRow = Box.createHorizontalBox();
        var sixthRow = Box.createHorizontalBox();
        var seventhRow = Box.createHorizontalBox();

        var compButton = new JToggleButton(SOURCE_ICON, freeformPixelApertureRois.get(selectedAperture-1).isComparisonStar());
        compButton.setSelectedIcon(BACKGROUND_ICON);
        var deleteAp = new JButton(REMOVE_ICON);
        var selectorModel = new SpinnerNumberModel(selectedAperture, 1, Math.max(freeformPixelApertureRois.size(), 1), 1);
        var apLabel = new JLabel("AP:");
        var apSelector = new JSpinner(selectorModel);
        var addAp = new JButton(ADD_ICON);
        var beginButton = new JButton(PLAY_ICON);
        var invertBackgroundControl = new JToggleButton(TO_FRONT_ICON, invertBackground);
        invertBackgroundControl.setSelectedIcon(TO_BACK_ICON);
        var helpPanel = new JTextArea();
        var copyBackground = new JToggleButton(COPY_ICON, this.copyBackground);
        copyBackground.setSelectedIcon(COPY_FULL_ICON);
        var backgroundFinder = new JButton(AUTO_SKY_ICON);
        var showEstimatedCircularAperture = new JCheckBox("Show estimated circular aperture", SHOW_ESTIMATED_CIRCULAR_APERTURE.get());
        var showCentroidRadius = new JCheckBox("Show estimated circular aperture", SHOW_CENTROID_RADIUS.get());
        var alwaysOnTop = new JCheckBox("Show panel always on top", ALWAYS_ON_TOP.get());
        var copyShape = new JCheckBox("Copy T1 shape", COPY_T1.get());
        var centroidShape = new JCheckBox("Centroid on copying of aperture shape", CENTROID_ON_COPY.get());
        var useAnnulus = new JCheckBox("Use annulus", currentAperture().hasAnnulus());
        var copyR1AsCentroidRadius = new JButton("Copy radius for centroid");
        var centroidRadius = createNumericSlider("Centroid radius", 0.5, 100, 1,
                currentAperture().getRadius(), radC -> {
                    currentAperture().setCentroidRadius(radC);
                    currentAperture().update();
                });
        var back1Radius = createNumericSlider("Inner radius", 0.5, 100, 1,
                currentAperture().getBack1(), rad2 -> {
                    currentAperture().setBack1(rad2);
                    currentAperture().update();
                });
        var back2Radius = createNumericSlider("Outer radius", 0.5, 100, 1,
                currentAperture().getBack2(), rad3 -> {
                    currentAperture().setBack2(rad3);
                    currentAperture().update();
                });

        configureButton(deleteAp);
        configureButton(compButton);
        configureButton(addAp);
        configureButton(beginButton);
        configureButton(invertBackgroundControl);
        configureButton(copyBackground);
        configureButton(backgroundFinder);

        if (apSelector.getEditor() instanceof JSpinner.DefaultEditor editor) {
            editor.getTextField().setColumns(5);
        }

        helpPanel.setWrapStyleWord(true);
        helpPanel.setEditable(false);

        Runnable updateHelp = () -> {
            helpPanel.setText(
                """
                <Left-click> to add $pType pixel to $ap.
                <Shift-left-click-drag> to add a region of $pType pixels to $ap.
                <Alt-left-click> to add $sType pixel to $ap.
                <Shift-alt-left-click-drag> to add a region of $sType pixels to $ap.
                <Right-click> to remove pixel from $ap.
                <Shift-right-click-drag> to remove a region of pixels from $ap.
                <Left-click-drag> inside of $ap source to move the source of $ap.
                <Enter> to run photometry.\
                """
                .replace("$ap", currentAperture().getName())
                .replace("$pType", invertBackground ? "background" : "source")
                .replace("$sType", !invertBackground ? "background" : "source")
            );
        };

        updateHelp.run();

        invertBackgroundControl.addItemListener(l -> {
            invertBackground = l.getStateChange() == ItemEvent.SELECTED;
            updateHelp.run();
        });

        compButton.addItemListener(l -> {
            freeformPixelApertureRois.get(selectedAperture-1).setComparisonStar(l.getStateChange() == ItemEvent.SELECTED);
            updateDisplay();
            updateHelp.run();
        });

        apSelector.addChangeListener($ -> {
            selectedAperture = ((Number) apSelector.getValue()).intValue();
            compButton.setSelected(freeformPixelApertureRois.get(selectedAperture-1).isComparisonStar());
            useAnnulus.setSelected(currentAperture().hasAnnulus());
            centroidRadius.setter().accept(currentAperture().getCentroidRadius());
            back1Radius.setter().accept(currentAperture().getBack1());
            back2Radius.setter().accept(currentAperture().getBack2());
            toggleComponents(back1Radius.box(), useAnnulus.isSelected());
            toggleComponents(back2Radius.box(), useAnnulus.isSelected());
            setFocusedAperture();
            updateDisplay(false);
            updateHelp.run();
        });

        deleteAp.addActionListener($ -> {
            if (apCount() == 1) {
                removeAperture(freeformPixelApertureRois.remove(0));
                freeformPixelApertureRois.add(createNewAperture(compButton.isSelected()));
                useAnnulus.setSelected(false);
                selectorModel.setMaximum(1);
                useAnnulus.setSelected(currentAperture().hasAnnulus());
                toggleComponents(back1Radius.box(), useAnnulus.isSelected());
                toggleComponents(back2Radius.box(), useAnnulus.isSelected());
                updateDisplay();
                return;
            }

            removeAperture(freeformPixelApertureRois.remove(selectedAperture-1));

            if (selectedAperture > freeformPixelApertureRois.size()) {
                selectedAperture = freeformPixelApertureRois.size();
                selectorModel.setValue(selectedAperture);
            }

            selectorModel.setMaximum(freeformPixelApertureRois.size());

            // Update state
            useAnnulus.setSelected(currentAperture().hasAnnulus());
            compButton.setSelected(currentAperture().isComparisonStar());
            useAnnulus.setSelected(currentAperture().hasAnnulus());
            centroidRadius.setter().accept(currentAperture().getCentroidRadius());
            back1Radius.setter().accept(currentAperture().getBack1());
            back2Radius.setter().accept(currentAperture().getBack2());
            toggleComponents(back1Radius.box(), useAnnulus.isSelected());
            toggleComponents(back2Radius.box(), useAnnulus.isSelected());
            updateDisplay();
            updateHelp.run();
        });

        addAp.addActionListener($ -> {
            freeformPixelApertureRois.add(selectedAperture, createNewAperture(compButton.isSelected()));
            selectorModel.setMaximum(freeformPixelApertureRois.size());
            selectorModel.setValue(++selectedAperture);
            useAnnulus.setSelected(currentAperture().hasAnnulus());
            currentAperture().setCentroidRadius(centroidRadius.getter.getAsDouble());
            back1Radius.setter().accept(currentAperture().getBack1());
            back2Radius.setter().accept(currentAperture().getBack2());
            toggleComponents(back1Radius.box(), useAnnulus.isSelected());
            toggleComponents(back2Radius.box(), useAnnulus.isSelected());
            updateDisplay();
            updateHelp.run();
        });

        copyR1AsCentroidRadius.addActionListener($ -> {
            var d = currentAperture().getRadius();
            if (d >= 0.5) {
                centroidRadius.setter().accept(d);
            }
        });

        beginButton.addActionListener($ -> playCallback.run());

        copyBackground.addItemListener(l -> {
            this.copyBackground = l.getStateChange() == ItemEvent.SELECTED;
        });

        backgroundFinder.addActionListener($ -> {
            try {
                this.findBackground();
                if (selectedAperture == 1) {
                    copyBackground.setSelected(true);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        showEstimatedCircularAperture.addActionListener($ -> {
            SHOW_ESTIMATED_CIRCULAR_APERTURE.set(showEstimatedCircularAperture.isSelected());
            updateDisplay(false);
        });

        showCentroidRadius.addActionListener($ -> {
            SHOW_CENTROID_RADIUS.set(showCentroidRadius.isSelected());
            updateDisplay(false);
        });

        alwaysOnTop.addActionListener($ -> {
            ALWAYS_ON_TOP.set(alwaysOnTop.isSelected());
            frame.setAlwaysOnTop(ALWAYS_ON_TOP.get());
        });

        copyShape.addActionListener($ -> {
            COPY_T1.set(copyShape.isSelected());
        });

        centroidShape.addActionListener($ -> {
            CENTROID_ON_COPY.set(centroidShape.isSelected());
        });

        useAnnulus.addActionListener($ -> {
            currentAperture().setHasAnnulus(useAnnulus.isSelected());
            currentAperture().setBack1(back1Radius.getter().getAsDouble());
            currentAperture().setBack2(back2Radius.getter().getAsDouble());
            toggleComponents(back1Radius.box(), useAnnulus.isSelected());
            toggleComponents(back2Radius.box(), useAnnulus.isSelected());
            updateDisplay();
        });

        toggleComponents(back1Radius.box(), useAnnulus.isSelected());
        toggleComponents(back2Radius.box(), useAnnulus.isSelected());

        frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);

        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                if (shutdownCallback != null) {
                    shutdownCallback.run();
                }
            }
        });

        deleteAp.setToolTipText("Delete current aperture");
        apSelector.setToolTipText("Select active aperture");
        addAp.setToolTipText("Insert/Add new aperture and set active");
        compButton.setToolTipText("Toggles between target (green) and comparison (red) star aperture");
        invertBackgroundControl.setToolTipText("Toggle between source and background pixel placement");
        copyBackground.setToolTipText("Toggles if creating a new aperture should copy the current aperture's background");
        beginButton.setToolTipText("Run photometry");
        backgroundFinder.setToolTipText("Automatic background pixel selection based on current image for the active aperture");
        copyShape.setToolTipText("Copy T1 source pixels when creating a new aperture");
        centroidShape.setToolTipText("Centroid copied source pixels from T1");

        updateCount = selectorModel::setMaximum;

        var selector = Box.createHorizontalBox();
        selector.add(apLabel);
        selector.add(apSelector);

        firstRow.add(deleteAp);
        firstRow.add(selector);
        firstRow.add(addAp);
        firstRow.add(compButton);
        firstRow.add(invertBackgroundControl);
        firstRow.add(backgroundFinder);
        firstRow.add(copyBackground);
        firstRow.add(beginButton);
        firstRow.add(Box.createHorizontalGlue());

        secondRow.add(showEstimatedCircularAperture);
        secondRow.add(alwaysOnTop);
        secondRow.add(copyShape);
        secondRow.add(centroidShape);

        thirdRow.add(showCentroidRadius);
        thirdRow.add(useAnnulus);
        thirdRow.add(copyR1AsCentroidRadius);

        forthRow.add(centroidRadius.box());

        fifthRow.add(back1Radius.box());

        sixthRow.add(back2Radius.box());

        p.add(firstRow);
        p.add(secondRow);
        p.add(secondRow);
        p.add(thirdRow);
        p.add(forthRow);
        p.add(fifthRow);
        p.add(sixthRow);
        p.add(seventhRow);
        p.add(helpPanel);
        frame.add(p);
        frame.pack();

        WINDOW_LOCATION.locationSavingWindow(frame, imp == null ? null : imp.getWindow());

        if (WINDOW_LOCATION.get().distanceSq(0,0) == 0) {
            if (imp != null && imp.getWindow() != null) {
                UIHelper.setCenteredOnWindow(frame, imp.getWindow());
            } else {
                UIHelper.setCenteredOnScreen(frame, IJ.getInstance());
            }
        }

        frame.setResizable(false);
        frame.setAlwaysOnTop(ALWAYS_ON_TOP.get());

        return frame;
    }

    public void closeControl() {
        if (controlPanel != null) {
            controlPanel.setVisible(false);
            controlPanel.dispose();
            controlPanel = null;
        }
    }

    private void configureButton(AbstractButton button) {
        var insets = new Insets(2, 2, 2, 2);
        button.setMargin(insets);
        button.setBorderPainted(false);
        button.setContentAreaFilled(false);
        button.setFocusPainted(false);
        button.setOpaque(false);
    }

    private void removeAperture(FreeformPixelApertureRoi ap) {
        if (ap != null && imp != null && OverlayCanvas.hasOverlayCanvas(imp)) {
            OverlayCanvas.getOverlayCanvas(imp).removeRoi(ap);
        }
    }

    private void findBackground() {
        if (imp == null) {
            return;
        }

        final var nDarkest = 20000;

        var ip = imp.getProcessor();
        // These pull from the histogram sliders of ASW
        var max = ip.getMax();
        var min = ip.getMin();

        final var binCount = 256;
        final var intensityFrequency = new int[binCount];

        // Get max/min pixel from image
        /*var max = Float.MIN_VALUE;
        var min = Float.MAX_VALUE;
        for (int x = 0; x < ip.getWidth(); x++) {
            for (int y = 0; y < ip.getHeight(); y++) {
                var p = ip.getPixelValue(x, y);
                if (p > max) {
                    max = p;
                }
                if (p < min) {
                    min = p;
                }
            }
        }*/

        for (int x = 0; x < ip.getWidth(); x++) {
            for (int y = 0; y < ip.getHeight(); y++) {
                var p = ip.getPixelValue(x, y);
                if (p < min) {
                    continue;
                }
                if (p > max) {
                    continue;
                }
                int binIndex = (int) ((p - min) / (max - min) * (binCount - 1));
                intensityFrequency[binIndex]++;
            }
        }

        // Find the bin with the highest frequency
        int modeBin = 0;
        int maxFrequency = 0;
        for (int i = 0; i < intensityFrequency.length; i++) {
            if (intensityFrequency[i] > maxFrequency) {
                maxFrequency = intensityFrequency[i];
                modeBin = i;
            }
        }

        // Calculate the approximate mode pixel value (center of the bin)
        var binWidth = (max - min) / (float)binCount;
        var mode = min + (modeBin * binWidth) + (binWidth / 2f);

        record Pi(int x, int y, float val) {}

        final var darkestPixels = new PriorityQueue<Pi>(nDarkest, Comparator.comparingDouble(Pi::val));

        for (int x = 0; x < ip.getWidth(); x++) {
            for (int y = 0; y < ip.getHeight(); y++) {
                var p = ip.getPixelValue(x, y);

                if (p <= mode && p > min) {
                    if (darkestPixels.size() < nDarkest) {
                        darkestPixels.add(new Pi(x, y, p));
                    } else if (p < darkestPixels.peek().val) {
                        darkestPixels.poll(); // Remove the brightest of the dark pixels
                        darkestPixels.add(new Pi(x, y, p));
                    }
                }
            }
        }

        for (Pi pi : darkestPixels) {
            currentAperture().addPixel(pi.x, pi.y, true, false);
        }

        // Ensure aperture is added to image
        if (OverlayCanvas.hasOverlayCanvas(imp)) {
            currentAperture().setImage(imp);
            OverlayCanvas.getOverlayCanvas(imp).add(currentAperture());
        }

        currentAperture().update();
        ImageCanvas canvas = OverlayCanvas.hasOverlayCanvas(imp) ?
                OverlayCanvas.getOverlayCanvas(imp) :
                imp.getCanvas();
        if (canvas != null) {
            canvas.repaint();
        }
    }

    private void updateDisplay() {
        updateDisplay(true);
    }

    private void updateDisplay(boolean updateNames) {
        if (updateNames) {
            updateApertureNames();
        }

        setFocusedAperture();

        if (imp != null) {
            currentAperture().setImage(imp);
            ImageCanvas canvas = OverlayCanvas.hasOverlayCanvas(imp) ?
                    OverlayCanvas.getOverlayCanvas(imp) :
                    imp.getCanvas();
            if (canvas != null) {
                canvas.repaint();
            }
        }
    }

    private void setFocusedAperture() {
        for (int i = 0; i < freeformPixelApertureRois.size(); i++) {
            freeformPixelApertureRois.get(i).setFocusedAperture(i == selectedAperture - 1);
        }
    }

    private NumericSlider createNumericSlider(String label, double min, double max, double step, double current, DoubleConsumer pref) {
        var b = Box.createHorizontalBox();

        if (Double.isNaN(current)) {
            current = 0;
        }

        current = Math.min(max, Math.max(current, min));

        var scale = step <= 1 ? 1D / step : 1D;

        var l = new JLabel(label);
        var slider = new Scrollbar(Scrollbar.HORIZONTAL, (int) (current * scale), 1, (int) (min * scale), (int) (max * scale));
        var spinner = new JSpinner(new SpinnerNumberModel(current, min, max, step));
        var tf = GenericSwingDialog.modifySpinner(spinner, true);
        GUI.fixScrollbar(slider);
        slider.setUnitIncrement(1);
        if (tf != null) {
            tf.setColumns(5);
        }

        slider.addMouseWheelListener(e -> {
            var delta = e.getPreciseWheelRotation() * ((SpinnerNumberModel) spinner.getModel()).getStepSize().doubleValue();

            var newValue = (Double) spinner.getValue() -
                    delta * ((SpinnerNumberModel) spinner.getModel()).getStepSize().doubleValue();

            if (newValue < ((Number) ((SpinnerNumberModel) spinner.getModel()).getMinimum()).doubleValue()) {
                newValue = ((Number) ((SpinnerNumberModel) spinner.getModel()).getMinimum()).doubleValue();
            } else if (newValue > ((Number) ((SpinnerNumberModel) spinner.getModel()).getMaximum()).doubleValue()) {
                newValue = ((Number) ((SpinnerNumberModel) spinner.getModel()).getMaximum()).doubleValue();
            }

            spinner.setValue(newValue);
        });

        slider.addAdjustmentListener($ -> {
            var d = slider.getValue() / scale;
            pref.accept(d);
            spinner.setValue(d);
        });

        spinner.addChangeListener($ -> {
            var i = (int) (((Number) spinner.getValue()).doubleValue() * scale);
            pref.accept(((Number) spinner.getValue()).doubleValue());
            slider.setValue(i);
            updateDisplay();
        });

        b.add(l);
        b.add(slider);
        b.add(spinner);

        return new NumericSlider(b, d -> {
            if (d >= min && d <= max) {
                spinner.setValue(d);
            }
        }, () -> ((Number) spinner.getValue()).doubleValue());
    }

    private static void toggleComponents(Component component, boolean enabled) {
        component.setEnabled(enabled);
        if (component instanceof Container) {
            for (Component child : ((Container) component).getComponents()) {
                toggleComponents(child, enabled);
            }
        }
    }

    private FreeformPixelApertureRoi createNewAperture(boolean comparisonStar) {
        var ap = new FreeformPixelApertureRoi();
        ap.setComparisonStar(comparisonStar);
        ap.setImage(imp);
        ap.setAppearance(starOverlay, false, skyOverlay, nameOverlay, valueOverlay, null,
                (ap.isComparisonStar() ? "C" : "T") + (selectedAperture), Double.NaN);

        if (copyBackground) {
            for (FreeformPixelApertureRoi.Pixel pixel : currentAperture().iterable()) {
                if (pixel.isBackground()) {
                    ap.addPixel(pixel);
                }
            }
        }

        return ap;
    }

    private void updateApertureNames() {
        for (int i = 0; i < freeformPixelApertureRois.size(); i++) {
            var ap = freeformPixelApertureRois.get(i);
            ap.setName((ap.isComparisonStar() ? "C" : "T") + (i+1));
        }
    }

    public void setImp(ImagePlus imp) {
        if (this.imp != imp && imp != null) {
            for (FreeformPixelApertureRoi roi : freeformPixelApertureRois) {
                roi.setImage(imp);
                if (OverlayCanvas.hasOverlayCanvas(imp)) {
                    OverlayCanvas.getOverlayCanvas(imp).add(roi);
                }
            }
        }
        this.imp = imp;
    }

    public static int savedApertureCount() {
        return APS.get().size();
    }

    public static int savedImportedApertureCount() {
        return IMPORTED_APS.get().size();
    }

    private static List<FreeformPixelApertureRoi> deserializeApertures(String setting) {
        var decoder = Base64.getDecoder();
        setting = new String(decoder.decode(setting));

        var apertures = new ArrayList<FreeformPixelApertureRoi>();
        if (setting.startsWith("handlerApertures")) {
            var ap = new AtomicReference<FreeformPixelApertureRoi>();
            var hasRBack1 = new AtomicBoolean();
            var hasRBack2 = new AtomicBoolean();
            setting.lines().skip(1).forEachOrdered(line -> {
                if (line.startsWith("ap customPixel")) {
                    var old = ap.getAndSet(new FreeformPixelApertureRoi());
                    if (old != null) {
                        apertures.add(old);
                    }
                    hasRBack1.set(false);
                    hasRBack2.set(false);
                    return;
                }

                if (line.startsWith("\t")) {
                    line = line.substring(1);
                }

                if (ap.get() == null) {
                    return;
                }

                if (line.startsWith("px")) {
                    var xSep = line.indexOf("\t");
                    if (xSep < 0) {
                        throw new IllegalStateException("Missing xSep! " + line);
                    }

                    var ySep = line.indexOf("\t", xSep+1);
                    if (ySep < 0) {
                        throw new IllegalStateException("Missing ySep! " + line);
                    }

                    var tSep = line.indexOf("\t", ySep+1);
                    if (tSep < 0) {
                        throw new IllegalStateException("Missing tSep! " + line);
                    }

                    var x = Integer.parseInt(line.substring(xSep+1, ySep));
                    var y = Integer.parseInt(line.substring(ySep+1, tSep));
                    ap.get().addPixel(x, y, "background".equals(line.substring(tSep+1)));
                }

                if (line.startsWith("isComp")) {
                    var tSep = line.indexOf("\t");
                    ap.get().setComparisonStar(Boolean.parseBoolean(line.substring(tSep+1)));
                }

                if (line.startsWith("rBack1")) {
                    var r1Sep = line.indexOf("\t");
                    if (r1Sep < 0) {
                        throw new IllegalStateException("Missing r1Sep! " + line);
                    }

                    var r1 = Double.parseDouble(line.substring(r1Sep+1));

                    hasRBack1.set(true);
                    ap.get().setBack1(r1);
                }

                if (line.startsWith("rBack2")) {
                    var r2Sep = line.indexOf("\t");
                    if (r2Sep < 0) {
                        throw new IllegalStateException("Missing r2Sep! " + line);
                    }

                    var r2 = Double.parseDouble(line.substring(r2Sep+1));

                    hasRBack2.set(true);
                    ap.get().setBack2(r2);
                }

                if (hasRBack1.get() && hasRBack2.get()) {
                    ap.get().setHasAnnulus(true);
                }
            });

            if (ap.get() != null) {
                apertures.add(ap.get());
            }
        }

        return apertures;
    }

    private static String serializeApertures(List<FreeformPixelApertureRoi> apertures) {
        var encoder = Base64.getEncoder();

        var setting = new StringBuilder("handlerApertures");

        for (FreeformPixelApertureRoi aperture : apertures) {
            setting.append("\nap customPixel");
            setting.append('\n');
            setting.append('\t').append("isComp").append('\t').append(aperture.isComparisonStar());

            for (FreeformPixelApertureRoi.Pixel pixel : aperture.iterable()) {
                setting.append('\n').append('\t');
                setting.append("px\t").append(pixel.x()).append('\t').append(pixel.y()).append('\t')
                        .append(pixel.isBackground() ? "background" : "source");
            }

            if (aperture.hasAnnulus()) {
                setting.append('\n').append('\t');
                setting.append("rBack1").append('\t').append(aperture.getBack1());
                setting.append('\n').append('\t');
                setting.append("rBack2").append('\t').append(aperture.getBack2());
            }
        }

        return encoder.encodeToString(setting.toString().getBytes());
    }

    private record NumericSlider(Box box, DoubleConsumer setter, DoubleSupplier getter) {}

}
