package util.prefs;

import java.awt.GridBagConstraints;
import java.awt.Rectangle;
import java.util.List;

import javax.swing.Box;
import javax.swing.JButton;

import Astronomy.shapes.WcsShape;
import astroj.AstroCanvas;
import ij.IJ;
import ij.ImagePlus;
import ij.WindowManager;
import ij.astro.gui.GenericSwingDialog;
import ij.astro.io.prefs.Property;
import ij.process.ImageProcessor;

public class RegionExclusion {
    public static final Property<Boolean> DISPLAY_EXCLUDED_BORDERS = new Property<>(false, RegionExclusion.class);
    public static final Property<Boolean> EXCLUDE_BORDERS = new Property<>(false, RegionExclusion.class);
    public static final Property<Integer> BORDER_EXCLUSION_LEFT = new Property<>(0, RegionExclusion.class);
    public static final Property<Integer> BORDER_EXCLUSION_RIGHT = new Property<>(0, RegionExclusion.class);
    public static final Property<Integer> BORDER_EXCLUSION_TOP = new Property<>(0, RegionExclusion.class);
    public static final Property<Integer> BORDER_EXCLUSION_BOTTOM = new Property<>(0, RegionExclusion.class);
    public static final Property<Integer> BORDER_EXCLUSION_LEFT_STEP = new Property<>(5, RegionExclusion.class);
    public static final Property<Integer> BORDER_EXCLUSION_RIGHT_STEP = new Property<>(5, RegionExclusion.class);
    public static final Property<Integer> BORDER_EXCLUSION_TOP_STEP = new Property<>(5, RegionExclusion.class);
    public static final Property<Integer> BORDER_EXCLUSION_BOTTOM_STEP = new Property<>(5, RegionExclusion.class);
    private static final GenericSwingDialog.Bounds BOUNDS = new GenericSwingDialog.Bounds(0, Integer.MAX_VALUE);

    private RegionExclusion() {}

    public static void editSettings() {
        EXCLUDE_BORDERS.set(true);
        DISPLAY_EXCLUDED_BORDERS.set(true);

        var gd = new GenericSwingDialog("Region Exclusion");

        gd.addCheckboxGroup(1, 2, new String[]{"Exclude Borders", "Display Excluded Borders"},
                new boolean[]{EXCLUDE_BORDERS.get(), DISPLAY_EXCLUDED_BORDERS.get()},
                List.of(EXCLUDE_BORDERS::set, DISPLAY_EXCLUDED_BORDERS::set));

        gd.addLineSeparator();

        gd.setOverridePosition(true);
        gd.setNewPosition(GridBagConstraints.CENTER);
        gd.addBoundedNumericField("Top", BOUNDS, BORDER_EXCLUSION_TOP_STEP.get(), 7, "px", BORDER_EXCLUSION_TOP);
        gd.setOverridePosition(false);

        gd.addBoundedNumericField("Left", BOUNDS, BORDER_EXCLUSION_LEFT_STEP.get(), 7, "px", BORDER_EXCLUSION_LEFT);
        gd.addToSameRow();
        gd.addBoundedNumericField("Right", BOUNDS, BORDER_EXCLUSION_RIGHT_STEP.get(), 7, "px", BORDER_EXCLUSION_RIGHT);

        gd.setOverridePosition(true);
        gd.setNewPosition(GridBagConstraints.CENTER);
        gd.setWidth(2);
        gd.addBoundedNumericField("Bottom", BOUNDS, BORDER_EXCLUSION_BOTTOM_STEP.get(), 7, "px", BORDER_EXCLUSION_BOTTOM);

        gd.setOverridePosition(false);
        gd.addMessage("""
                Options are in reference to IJ coordinates, eg. an unflipped image.
                See X/Y axis display in top left of stack window.""");

        gd.addSingleSpaceLineSeparator();

        var b = Box.createHorizontalBox();
        var generate = new JButton("Generate Common WCS Region");
        var clear = new JButton("Clear Common WCS Region");

        generate.addActionListener(_ -> {
            var imp = WindowManager.getCurrentImage();
            if (imp == null) {
                IJ.error("No image found!");
                return;
            }

            var region = WcsShape.createCommonRegion(imp);

            if (imp.getCanvas() instanceof AstroCanvas ac) {
                ac.setPerformDraw(true);
                ac.setWcsShape(region);
                ac.repaint();
            }
        });

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
