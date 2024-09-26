package Astronomy.multiaperture;

import astroj.CustomPixelApertureRoi;
import astroj.OverlayCanvas;
import ij.IJ;
import ij.ImagePlus;
import ij.Prefs;
import ij.astro.io.prefs.Property;
import ij.astro.util.UIHelper;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ItemEvent;
import java.util.List;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.IntConsumer;

import static Astronomy.Aperture_.*;

public class CustomPixelApertureHandler {
    private final List<CustomPixelApertureRoi> customPixelApertureRois = new ArrayList<>();
    private int selectedAperture = 1;
    private JFrame controlPanel;
    private Runnable playCallback;
    private ImagePlus imp;
    private boolean invertBackground = false;
    private boolean copyBackground = false;
    private final boolean starOverlay = Prefs.get(AP_PREFS_STAROVERLAY, true);
    private final boolean skyOverlay = Prefs.get(AP_PREFS_SKYOVERLAY, true);
    private final boolean nameOverlay = Prefs.get(AP_PREFS_NAMEOVERLAY, true);
    private final boolean valueOverlay = Prefs.get(AP_PREFS_VALUEOVERLAY, true);
    private final boolean tempOverlay = Prefs.get(AP_PREFS_TEMPOVERLAY, true);
    private final boolean clearOverlay = Prefs.get(AP_PREFS_CLEAROVERLAY, false);
    private IntConsumer updateCount = i -> {};
    public static final Property<List<CustomPixelApertureRoi>> APS =
            new Property<>(new ArrayList<>(),
                    CustomPixelApertureHandler::serializeApertures, CustomPixelApertureHandler::deserializeApertures,
                    CustomPixelApertureHandler.class);
    private static final Icon ADD_ICON = UIHelper.createImageIcon("Astronomy/images/icons/multiaperture/add.png", 19, 19);
    private static final Icon REMOVE_ICON = UIHelper.createImageIcon("Astronomy/images/icons/multiaperture/remove.png", 19, 19);
    private static final Icon PLAY_ICON = UIHelper.createImageIcon("Astronomy/images/icons/multiaperture/play.png", 19, 19);
    private static final Icon TO_FRONT_ICON = UIHelper.createImageIcon("Astronomy/images/icons/multiaperture/flipToFront.png", 19, 19);
    private static final Icon TO_BACK_ICON = UIHelper.createImageIcon("Astronomy/images/icons/multiaperture/flipToBack.png", 19, 19);
    private static final Icon WARNING_ICON = UIHelper.createImageIcon("Astronomy/images/icons/multiaperture/warning.png", 19, 19);
    private static final Icon SOURCE_ICON = UIHelper.createImageIcon("Astronomy/images/icons/multiaperture/greenCircle.png", 19, 19);
    private static final Icon BACKGROUND_ICON = UIHelper.createImageIcon("Astronomy/images/icons/multiaperture/redCircle.png", 19, 19);
    private static final Icon COPY_ICON = UIHelper.createImageIcon("Astronomy/images/icons/multiaperture/copy.png", 19, 19);
    private static final Icon COPY_FULL_ICON = UIHelper.createImageIcon("Astronomy/images/icons/multiaperture/copyFull.png", 19, 19);
    private static final Icon AUTO_SKY_ICON = UIHelper.createImageIcon("Astronomy/images/icons/multiaperture/autoSky.png", 19, 19);

    public CustomPixelApertureHandler() {
        this.customPixelApertureRois.add(createNewAperture(false));
        updateApertureNames();
        customPixelApertureRois.get(0).setFocusedAperture(true);
        controlPanel = createControlPanel();
    }

    public CustomPixelApertureRoi currentAperture() {
        return customPixelApertureRois.get(selectedAperture - 1);
    }

    public int apCount() {
        return customPixelApertureRois.size();
    }

    public CustomPixelApertureRoi getAperture(int index) {
        return customPixelApertureRois.get(index);
    }

    public void addPixel(int x, int y, boolean isBackground) {
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
        }
    }

    public void hideControls() {
        if (controlPanel != null) {
            controlPanel.setVisible(false);
        }
    }

    public void setPlayCallback(Runnable callback) {
        playCallback = callback;
    }

    public boolean validateApertures() {
        var invalidCount = 0;
        var message = new StringBuilder();

        for (CustomPixelApertureRoi roi : customPixelApertureRois) {
            if (roi.hasPixels()) {
                if (!roi.hasSource()) {
                    invalidCount++;
                    message.append("%s has no source\n".formatted(roi.getName()));
                }

                if (!roi.hasBackground()) {
                    invalidCount++;
                    message.append("%s has no background\n".formatted(roi.getName()));
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
                APS.set(customPixelApertureRois);
            }

            return proceed;
        }

        APS.set(customPixelApertureRois);

        return true;
    }

    /**
     * Load apertures from preferences, clears existing apertures.
     */
    public void loadAperturesFromPrefs(boolean loadOnlyOne) {
        customPixelApertureRois.clear();
        if (loadOnlyOne && !APS.get().isEmpty()) {
            customPixelApertureRois.add(APS.get().get(0));
        } else {
            customPixelApertureRois.addAll(APS.get());
        }

        if (imp != null) {
            for (CustomPixelApertureRoi roi : customPixelApertureRois) {
                roi.setImage(imp);
                if (OverlayCanvas.hasOverlayCanvas(imp)) {
                    OverlayCanvas.getOverlayCanvas(imp).add(roi);
                }
            }
            (OverlayCanvas.hasOverlayCanvas(imp) ? OverlayCanvas.getOverlayCanvas(imp) : imp.getCanvas()).repaint();
        }

        customPixelApertureRois.get(0).setFocusedAperture(true);
        updateCount.accept(customPixelApertureRois.size());
        updateDisplay(true);
    }

    private JFrame createControlPanel() {
        var frame = new JFrame("Aperture Control Panel");
        var p = Box.createVerticalBox();
        var b = Box.createHorizontalBox();

        var compButton = new JToggleButton(SOURCE_ICON, customPixelApertureRois.get(selectedAperture-1).isComparisonStar());
        compButton.setSelectedIcon(BACKGROUND_ICON);
        var deleteAp = new JButton(REMOVE_ICON);
        var selectorModel = new SpinnerNumberModel(selectedAperture, 1, Math.max(customPixelApertureRois.size(), 1), 1);
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
            customPixelApertureRois.get(selectedAperture-1).setComparisonStar(l.getStateChange() == ItemEvent.SELECTED);
            updateDisplay();
            updateHelp.run();
        });

        apSelector.addChangeListener($ -> {
            selectedAperture = ((Number) apSelector.getValue()).intValue();
            compButton.setSelected(customPixelApertureRois.get(selectedAperture-1).isComparisonStar());
            setFocusedAperture();
            updateDisplay(false);
            updateHelp.run();
        });

        deleteAp.addActionListener($ -> {
            if (apCount() == 1) {
                removeAperture(customPixelApertureRois.remove(0));
                customPixelApertureRois.add(createNewAperture(compButton.isSelected()));
                selectorModel.setMaximum(1);
                updateDisplay();
                return;
            }

            removeAperture(customPixelApertureRois.remove(selectedAperture-1));

            if (selectedAperture > customPixelApertureRois.size()) {
                selectedAperture = customPixelApertureRois.size();
                selectorModel.setValue(selectedAperture);
            }

            selectorModel.setMaximum(customPixelApertureRois.size());

            // Update state
            compButton.setSelected(customPixelApertureRois.get(selectedAperture-1).isComparisonStar());
            updateDisplay();
            updateHelp.run();
        });

        addAp.addActionListener($ -> {
            customPixelApertureRois.add(selectedAperture, createNewAperture(compButton.isSelected()));
            selectorModel.setMaximum(customPixelApertureRois.size());
            selectorModel.setValue(++selectedAperture);
            updateDisplay();
            updateHelp.run();
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

        frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);

        deleteAp.setToolTipText("Delete current aperture");
        apSelector.setToolTipText("Select active aperture");
        addAp.setToolTipText("Insert/Add new aperture and set active");
        compButton.setToolTipText("Toggles between target (green) and comparison (red) star aperture");
        invertBackgroundControl.setToolTipText("Toggle between source and background pixel placement");
        copyBackground.setToolTipText("Toggles if creating a new aperture should copy the current aperture's background");
        beginButton.setToolTipText("Run photometry");
        backgroundFinder.setToolTipText("Automatic background pixel selection based on current image for the active aperture");

        updateCount = selectorModel::setMaximum;

        var selector = Box.createHorizontalBox();
        selector.add(apLabel);
        selector.add(apSelector);

        b.add(deleteAp);
        b.add(selector);
        b.add(addAp);
        b.add(compButton);
        b.add(invertBackgroundControl);
        b.add(backgroundFinder);
        b.add(copyBackground);
        b.add(beginButton);
        b.add(Box.createHorizontalGlue());

        p.add(b);
        p.add(helpPanel);
        frame.add(p);
        frame.pack();

        if (imp != null && imp.getWindow() != null) {
            UIHelper.setCenteredOnWindow(frame, imp.getWindow());
        } else {
            UIHelper.setCenteredOnScreen(frame, IJ.getInstance());
        }

        frame.setResizable(false);
        frame.setAlwaysOnTop(true);

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

    private void removeAperture(CustomPixelApertureRoi ap) {
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
        (OverlayCanvas.hasOverlayCanvas(imp) ? OverlayCanvas.getOverlayCanvas(imp) : imp.getCanvas()).repaint();
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
            (OverlayCanvas.hasOverlayCanvas(imp) ? OverlayCanvas.getOverlayCanvas(imp) : imp.getCanvas()).repaint();
        }
    }

    private void setFocusedAperture() {
        for (int i = 0; i < customPixelApertureRois.size(); i++) {
            customPixelApertureRois.get(i).setFocusedAperture(i == selectedAperture - 1);
        }
    }

    private CustomPixelApertureRoi createNewAperture(boolean comparisonStar) {
        var ap = new CustomPixelApertureRoi();
        ap.setComparisonStar(comparisonStar);
        ap.setImage(imp);
        ap.setAppearance(starOverlay, false, skyOverlay, nameOverlay, valueOverlay, null,
                (ap.isComparisonStar() ? "C" : "T") + (selectedAperture), Double.NaN);

        if (copyBackground) {
            for (CustomPixelApertureRoi.Pixel pixel : currentAperture().iterable()) {
                if (pixel.isBackground()) {
                    ap.addPixel(pixel);
                }
            }
        }

        return ap;
    }

    private void updateApertureNames() {
        for (int i = 0; i < customPixelApertureRois.size(); i++) {
            var ap = customPixelApertureRois.get(i);
            ap.setName((ap.isComparisonStar() ? "C" : "T") + (i+1));
        }
    }

    public void setImp(ImagePlus imp) {
        if (this.imp != imp && imp != null) {
            for (CustomPixelApertureRoi roi : customPixelApertureRois) {
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

    private static List<CustomPixelApertureRoi> deserializeApertures(String setting) {
        var decoder = Base64.getDecoder();
        setting = new String(decoder.decode(setting));

        var apertures = new ArrayList<CustomPixelApertureRoi>();
        if (setting.startsWith("handlerApertures")) {
            var ap = new AtomicReference<CustomPixelApertureRoi>();
            setting.lines().skip(1).forEachOrdered(line -> {
                if (line.startsWith("ap customPixel")) {
                    var old = ap.getAndSet(new CustomPixelApertureRoi());
                    if (old != null) {
                        apertures.add(old);
                    }
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
            });

            if (ap.get() != null) {
                apertures.add(ap.get());
            }
        }

        return apertures;
    }

    private static String serializeApertures(List<CustomPixelApertureRoi> apertures) {
        var encoder = Base64.getEncoder();

        var setting = new StringBuilder("handlerApertures");

        for (CustomPixelApertureRoi aperture : apertures) {
            setting.append("\nap customPixel");
            setting.append('\n');
            setting.append('\t').append("isComp").append('\t').append(aperture.isComparisonStar());

            for (CustomPixelApertureRoi.Pixel pixel : aperture.iterable()) {
                setting.append('\n').append('\t');
                setting.append("px\t").append(pixel.x()).append('\t').append(pixel.y()).append('\t')
                        .append(pixel.isBackground() ? "background" : "source");
            }
        }

        return encoder.encodeToString(setting.toString().getBytes());
    }
}
