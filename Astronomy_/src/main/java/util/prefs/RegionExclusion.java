package util.prefs;

import astroj.AstrometrySetup;
import ij.astro.io.prefs.Property;

public class RegionExclusion {
    public static Property<Boolean> DISPLAY_EXCLUDED_BORDERS = new Property<>(true, AstrometrySetup.class);
    public static Property<Boolean> EXCLUDE_BORDERS = new Property<>(false, AstrometrySetup.class);
    public static Property<Integer> BORDER_EXCLUSION_LEFT = new Property<>(10, AstrometrySetup.class);
    public static Property<Integer> BORDER_EXCLUSION_RIGHT = new Property<>(10, AstrometrySetup.class);
    public static Property<Integer> BORDER_EXCLUSION_TOP = new Property<>(10, AstrometrySetup.class);
    public static Property<Integer> BORDER_EXCLUSION_BOTTOM = new Property<>(10, AstrometrySetup.class);
    public static Property<Integer> BORDER_EXCLUSION_LEFT_STEP = new Property<>(5, AstrometrySetup.class);
    public static Property<Integer> BORDER_EXCLUSION_RIGHT_STEP = new Property<>(5, AstrometrySetup.class);
    public static Property<Integer> BORDER_EXCLUSION_TOP_STEP = new Property<>(5, AstrometrySetup.class);
    public static Property<Integer> BORDER_EXCLUSION_BOTTOM_STEP = new Property<>(5, AstrometrySetup.class);

    private RegionExclusion() {}
}
