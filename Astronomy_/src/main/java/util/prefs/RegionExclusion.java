package util.prefs;

import java.awt.GridBagConstraints;
import java.awt.Rectangle;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JSpinner;
import javax.swing.SwingUtilities;

import Astronomy.shapes.WcsShape;
import astroj.AstroCanvas;
import ij.IJ;
import ij.ImagePlus;
import ij.WindowManager;
import ij.astro.gui.GenericSwingDialog;
import ij.astro.io.prefs.Property;
import ij.process.ImageProcessor;

public class RegionExclusion {
    public static final Property<Boolean> DISPLAY_EXCLUDED_REGIONS = new Property<>(false, RegionExclusion.class);
    public static final Property<Boolean> EXCLUDE_BORDERS = new Property<>(false, RegionExclusion.class);
    public static final Property<Boolean> EXCLUDE_UNCOMMON_REGION = new Property<>(true, RegionExclusion.class);
    public static final Property<Integer> BORDER_EXCLUSION_LEFT = new Property<>(0, RegionExclusion.class);
    public static final Property<Integer> BORDER_EXCLUSION_RIGHT = new Property<>(0, RegionExclusion.class);
    public static final Property<Integer> BORDER_EXCLUSION_TOP = new Property<>(0, RegionExclusion.class);
    public static final Property<Integer> BORDER_EXCLUSION_BOTTOM = new Property<>(0, RegionExclusion.class);
    public static final Property<Integer> BORDER_EXCLUSION_LEFT_STEP = new Property<>(5, RegionExclusion.class);
    public static final Property<Integer> BORDER_EXCLUSION_RIGHT_STEP = new Property<>(5, RegionExclusion.class);
    public static final Property<Integer> BORDER_EXCLUSION_TOP_STEP = new Property<>(5, RegionExclusion.class);
    public static final Property<Integer> BORDER_EXCLUSION_BOTTOM_STEP = new Property<>(5, RegionExclusion.class);
    public static final Property<Boolean> LOCK_BORDERS_TO_TOP = new Property<>(false, RegionExclusion.class);
    private static final GenericSwingDialog.Bounds BOUNDS = new GenericSwingDialog.Bounds(0, Integer.MAX_VALUE);
    public static final ExecutorService EXECUTOR_SERVICE = Executors.newSingleThreadExecutor();

    private RegionExclusion() {}

    public static void editSettings() {
        DISPLAY_EXCLUDED_REGIONS.set(true);

        var gd = new GenericSwingDialog("Region Exclusion");

        var cbs = gd.addCheckboxGroup(2, 1, new String[]{"Exclude Borders", "Exclude non-common image regions (requires WCS)"},
                new boolean[]{EXCLUDE_BORDERS.get(), EXCLUDE_UNCOMMON_REGION.get()},
                List.of(EXCLUDE_BORDERS::set, EXCLUDE_UNCOMMON_REGION::set));

        cbs.subComponents().get(1).setToolTipText("""
                Include only the image region that is common to all images. This option only affects the region used for auto comp star selection.
                """);

        gd.addCheckbox("Lock all region values to Top", LOCK_BORDERS_TO_TOP);

        gd.setOverridePosition(true);
        gd.setNewPosition(GridBagConstraints.CENTER);
        var top = gd.addBoundedNumericField("Top", BOUNDS, BORDER_EXCLUSION_TOP_STEP.get(), 7, "px", BORDER_EXCLUSION_TOP);
        gd.setOverridePosition(false);

        var left = gd.addBoundedNumericField("Left", BOUNDS, BORDER_EXCLUSION_LEFT_STEP.get(), 7, "px", BORDER_EXCLUSION_LEFT);
        gd.addToSameRow();
        var right = gd.addBoundedNumericField("Right", BOUNDS, BORDER_EXCLUSION_RIGHT_STEP.get(), 7, "px", BORDER_EXCLUSION_RIGHT);

        gd.setOverridePosition(true);
        gd.setNewPosition(GridBagConstraints.CENTER);
        gd.setWidth(2);
        var bottom = gd.addBoundedNumericField("Bottom", BOUNDS, BORDER_EXCLUSION_BOTTOM_STEP.get(), 7, "px", BORDER_EXCLUSION_BOTTOM);

        BORDER_EXCLUSION_TOP.addListener((_, n) -> {
            if (cbs.subComponents().getFirst() instanceof JCheckBox cb) {
                cb.setSelected(true);
                EXCLUDE_BORDERS.set(true);
            }
            if (LOCK_BORDERS_TO_TOP.get()) {
                ((JSpinner) left.c1()).setValue(n.doubleValue());
                ((JSpinner) right.c1()).setValue(n.doubleValue());
                ((JSpinner) bottom.c1()).setValue(n.doubleValue());
            }
        });
        BORDER_EXCLUSION_RIGHT.addListener((_, _) -> {
            if (cbs.subComponents().getFirst() instanceof JCheckBox cb) {
                cb.setSelected(true);
                EXCLUDE_BORDERS.set(true);
            }
        });
        BORDER_EXCLUSION_LEFT.addListener((_, _) -> {
            if (cbs.subComponents().getFirst() instanceof JCheckBox cb) {
                cb.setSelected(true);
                EXCLUDE_BORDERS.set(true);
            }
        });
        BORDER_EXCLUSION_BOTTOM.addListener((_, _) -> {
            if (cbs.subComponents().getFirst() instanceof JCheckBox cb) {
                cb.setSelected(true);
                EXCLUDE_BORDERS.set(true);
            }
        });
        LOCK_BORDERS_TO_TOP.addListener((_, b) -> {
            left.c1().setEnabled(!b);
            right.c1().setEnabled(!b);
            bottom.c1().setEnabled(!b);
            if (b) {
                ((JSpinner) left.c1()).setValue(BORDER_EXCLUSION_TOP.get().doubleValue());
                ((JSpinner) right.c1()).setValue(BORDER_EXCLUSION_TOP.get().doubleValue());
                ((JSpinner) bottom.c1()).setValue(BORDER_EXCLUSION_TOP.get().doubleValue());
            }
        });

        if (LOCK_BORDERS_TO_TOP.get()) {
            left.c1().setEnabled(false);
            right.c1().setEnabled(false);
            bottom.c1().setEnabled(false);
        }

        gd.setOverridePosition(false);
        gd.addMessage("""
                Options are in reference to IJ coordinates, eg. an unflipped image.
                See X/Y axis display in top left of stack window.""");

        gd.addSingleSpaceLineSeparator();

        var b = Box.createHorizontalBox();
        var generate = new JButton("Generate Common WCS Region");
        var clear = new JButton("Clear Common WCS Region");

        generate.addActionListener(_ -> EXECUTOR_SERVICE.execute(() -> {
            var imp = WindowManager.getCurrentImage();
            if (imp == null) {
                IJ.error("No image found!");
                return;
            }

            var region = WcsShape.createCommonRegion(imp, gd);

            SwingUtilities.invokeLater(() -> {
                if (imp.getCanvas() instanceof AstroCanvas ac) {
                    ac.setPerformDraw(true);
                    ac.setWcsShape(region);
                    ac.repaint();
                }
            });
        }));

        clear.addActionListener(_ -> {
            var imp = WindowManager.getCurrentImage();
            if (imp == null) {
                IJ.error("No image found!");
                return;
            }

            if (imp.getCanvas() instanceof AstroCanvas ac) {
                ac.setPerformDraw(true);
                ac.setWcsShape(null);
                ac.repaint();
            }
        });

        b.add(generate);
        b.add(clear);
        gd.addGenericComponent(b);

        gd.centerDialog(true);
        gd.showDialog();
    }

    public static Rectangle restrict(ImagePlus imp) {
        return restrict(imp.getProcessor());
    }

    public static Rectangle restrict(ImageProcessor ip) {
        return restrict(ip.getRoi());
    }

    public static Rectangle restrict(Rectangle r) {
        if (RegionExclusion.EXCLUDE_BORDERS.get()) {
            r.width -= (RegionExclusion.BORDER_EXCLUSION_LEFT.get() + RegionExclusion.BORDER_EXCLUSION_RIGHT.get());
            r.height -= (RegionExclusion.BORDER_EXCLUSION_TOP.get() + RegionExclusion.BORDER_EXCLUSION_BOTTOM.get());
            r.translate(RegionExclusion.BORDER_EXCLUSION_LEFT.get(), RegionExclusion.BORDER_EXCLUSION_TOP.get());
        }

        return r;
    }
}
