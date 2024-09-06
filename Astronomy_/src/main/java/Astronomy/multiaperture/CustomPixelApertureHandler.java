package Astronomy.multiaperture;

import astroj.CustomPixelApertureRoi;
import astroj.OverlayCanvas;
import ij.IJ;
import ij.ImagePlus;
import ij.Prefs;
import ij.astro.util.UIHelper;

import javax.swing.*;
import java.awt.event.ItemEvent;
import java.util.ArrayList;
import java.util.List;

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
        currentAperture().addPixel(x, y, invertBackground != isBackground);
    }

    public void removePixel(int x, int y) {
        currentAperture().removePixel(x, y);
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
            return IJ.showMessageWithCancel("Custom Aperture Handler", message.toString());
        }

        return true;
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

        if (apSelector.getEditor() instanceof JSpinner.DefaultEditor editor) {
            editor.getTextField().setColumns(5);
        }

        helpPanel.setRows(6);
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

        beginButton.addActionListener($ -> {
            if (validateApertures()) {
                frame.setVisible(false);
                playCallback.run();
            }
        });

        copyBackground.addItemListener(l -> {
            this.copyBackground = l.getStateChange() == ItemEvent.SELECTED;
        });

        frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);

        deleteAp.setToolTipText("Delete current aperture");
        apSelector.setToolTipText("Select active aperture");
        addAp.setToolTipText("Insert/Add new aperture and set active");
        compButton.setToolTipText("Toggles if this aperture is a comparison star");
        invertBackgroundControl.setToolTipText("Invert default pixel type to place");
        copyBackground.setToolTipText("Toggles if creating a new aperture should copy the current aperture's background");
        beginButton.setToolTipText("Run photometry");

        var selector = Box.createHorizontalBox();
        selector.add(apLabel);
        selector.add(apSelector);

        b.add(deleteAp);
        b.add(selector);
        b.add(addAp);
        b.add(compButton);
        b.add(invertBackgroundControl);
        b.add(copyBackground);
        b.add(beginButton);

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

    private void removeAperture(CustomPixelApertureRoi ap) {
        if (ap != null && imp != null && OverlayCanvas.hasOverlayCanvas(imp)) {
            OverlayCanvas.getOverlayCanvas(imp).removeRoi(ap);
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
        this.imp = imp;
    }
}
