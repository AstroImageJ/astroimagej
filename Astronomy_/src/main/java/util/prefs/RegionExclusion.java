package util.prefs;

import java.util.List;

import javax.swing.Box;

import ij.astro.gui.GenericSwingDialog;
import ij.astro.io.prefs.Property;

public class RegionExclusion {
    public static final Property<Boolean> DISPLAY_EXCLUDED_BORDERS = new Property<>(true, RegionExclusion.class);
    public static final Property<Boolean> EXCLUDE_BORDERS = new Property<>(false, RegionExclusion.class);
    public static final Property<Integer> BORDER_EXCLUSION_LEFT = new Property<>(10, RegionExclusion.class);
    public static final Property<Integer> BORDER_EXCLUSION_RIGHT = new Property<>(10, RegionExclusion.class);
    public static final Property<Integer> BORDER_EXCLUSION_TOP = new Property<>(10, RegionExclusion.class);
    public static final Property<Integer> BORDER_EXCLUSION_BOTTOM = new Property<>(10, RegionExclusion.class);
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

        gd.addBoundedNumericField("Left", BOUNDS, BORDER_EXCLUSION_LEFT_STEP.get(), 4, "px", BORDER_EXCLUSION_LEFT);
        gd.addToSameRow();
        gd.addBoundedNumericField("Top", BOUNDS, BORDER_EXCLUSION_TOP_STEP.get(), 4, "px", BORDER_EXCLUSION_TOP);
        gd.addToSameRow();
        gd.addBoundedNumericField("Right", BOUNDS, BORDER_EXCLUSION_RIGHT_STEP.get(), 4, "px", BORDER_EXCLUSION_RIGHT);

        gd.addGenericComponent(Box.createHorizontalStrut(0));
        gd.addToSameRow();
        gd.addBoundedNumericField("Bottom", BOUNDS, BORDER_EXCLUSION_BOTTOM_STEP.get(), 4, "px", BORDER_EXCLUSION_BOTTOM);
        gd.addToSameRow();
        gd.addGenericComponent(Box.createHorizontalStrut(0));

        gd.centerDialog(true);
        gd.showDialog();
    }
}
