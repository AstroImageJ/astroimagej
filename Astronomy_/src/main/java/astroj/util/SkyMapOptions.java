package astroj.util;

import java.awt.Frame;

import ij.astro.gui.GenericSwingDialog;
import ij.astro.io.prefs.Property;

public class SkyMapOptions {
    private static final SkyMapOptions INSTANCE = new SkyMapOptions();
    public static Property<Boolean> SHOW_GAIA = new Property<>(false, SkyMapOptions.class);
    public static Property<Boolean> SHOW_BLEND = new Property<>(true, SkyMapOptions.class);
    public static Property<Boolean> SHOW_TIC = new Property<>(false, SkyMapOptions.class);
    public static Property<Boolean> SHOW_EBs = new Property<>(true, SkyMapOptions.class);
    public static Property<Boolean> SHOW_VARS = new Property<>(true, SkyMapOptions.class);
    public static Property<Boolean> SHOW_MISMATCHES = new Property<>(false, SkyMapOptions.class);
    public static Property<Boolean> SHOW_FOV = new Property<>(true, SkyMapOptions.class);
    public static Property<Double> FOV_HEIGHT = new Property<>(26D, SkyMapOptions.class);
    public static Property<Double> FOV_WIDTH = new Property<>(26D, SkyMapOptions.class);
    public static Property<Double> FOV_PA = new Property<>(0D, SkyMapOptions.class);
    public static Property<Double> GAIA_DIST_THRESH = new Property<>(0.5, SkyMapOptions.class);
    public static Property<Double> GAIA_MAG_THRESH = new Property<>(0.5, SkyMapOptions.class);

    public void dialog(Frame owner) {
        var gd = new GenericSwingDialog("Aladín FOV Options", owner);
        gd.addCheckbox("Show Blends", SHOW_BLEND)
                .setToolTipText("""
                        Must enter transit depth on webpage to show.
                        """);
        gd.addCheckbox("Show Gaia", SHOW_GAIA);
        gd.addCheckbox("Show TIC", SHOW_TIC);
        gd.addCheckbox("Show Gaia EBs", SHOW_EBs);
        gd.addCheckbox("Show Gaia Vars", SHOW_VARS);
        gd.addCheckbox("Show Gaia DR2 vs DR3 Mismatches", SHOW_MISMATCHES);
        gd.addCheckbox("Show FOV", SHOW_FOV);

        gd.addBoundedNumericField("FOV Width", new GenericSwingDialog.Bounds(0, 360),
                1D, 5, "Degrees", true, FOV_WIDTH);
        gd.addBoundedNumericField("FOV Height", new GenericSwingDialog.Bounds(0, 360),
                1D, 5, "Degrees", true, FOV_HEIGHT);
        gd.addBoundedNumericField("FOV PA", new GenericSwingDialog.Bounds(),
                1D, 5, "Degrees", true, FOV_PA);
        gd.addBoundedNumericField("GAIA Mismatch Dist Thresh", new GenericSwingDialog.Bounds(),
                1D, 5, "arcsec    ", true, GAIA_DIST_THRESH);
        gd.addBoundedNumericField("GAIA Mismatch Mag Thresh", new GenericSwingDialog.Bounds(0, 25),
                1D, 5, "                ", true, GAIA_MAG_THRESH);

        gd.centerDialog(true);
        gd.showDialog();
    }

    public static int normalize(Property<Boolean> p) {
        return p.get() ? 1 : 0;
    }

    public static void showDialog(Frame owner) {
        INSTANCE.dialog(owner);
    }
}
