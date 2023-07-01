package Astronomy.multiplot.settings;

import ij.astro.gui.nstate.TriState;
import ij.astro.io.prefs.Property;

import java.awt.*;
import java.lang.reflect.Field;

public class KeplerSplineSettings {
    public final Property<DisplayType> displayType = makePlotProperty(DisplayType.RAW_DATA);
    public final Property<KnotDensity> knotDensity = makePlotProperty(KnotDensity.FIXED);
    public final Property<Double> fixedKnotDensity = makePlotProperty(0.5);
    public final Property<Double> minKnotDensity = makePlotProperty(0.5);
    public final Property<Double> maxKnotDensity = makePlotProperty(20D);
    public final Property<Integer> knotDensitySteps = makePlotProperty(20);
    public final Property<Double> minGapWidth = makePlotProperty(0.2);
    public final Property<Double> dataCleaningCoeff = makePlotProperty(3D);
    public final Property<Integer> dataCleaningTries = makePlotProperty(5);
    public final Property<Integer> smoothLength = makePlotProperty(31);
    public final Property<Boolean> maskTransit = makePlotProperty(true);
    public final Property<Boolean> windowOpened;
    public final Property<Point> windowLocation;
    private final int curve;

    public KeplerSplineSettings(int curve) {
        this.curve = curve;
        windowLocation = new Property<>(new Point(), "", String.valueOf(curve), this);
        windowOpened = new Property<>(false, "", String.valueOf(curve), this);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public void duplicateSettings(KeplerSplineSettings from) {
        for (Field field : getClass().getDeclaredFields()) {
            if (field.getType() != Property.class) {
                continue;
            }
            if (field.getName().equals("displayType") || field.getName().equals("windowLocation")) {
                continue;
            }
            try {
                ((Property) field.get(this)).set(((Property) field.get(from)).get());
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }
    }

    public TriState getTriStateDisplay() {
        return switch (displayType.get()) {
            case RAW_DATA -> TriState.DISABLED;
            case FITTED_SPLINE -> TriState.ALT_ENABLED;
            case FLATTENED_LIGHT_CURVE -> TriState.ENABLED;
        };
    }

    private <T> Property<T> makePlotProperty(T defaultValue) {
        return new Property<>(defaultValue, () -> "plot.", () -> String.valueOf(curve), this);
    }

    public enum KnotDensity {
        AUTO,
        FIXED,
        LEGACY_SMOOTHER,
    }

    public enum DisplayType {
        FLATTENED_LIGHT_CURVE {
            @Override
            public String displayName() {
                return "Plot smoothed light curve";
            }
        },
        FITTED_SPLINE {
            @Override
            public String displayName() {
                return "Plot spline fit";
            }
        },
        RAW_DATA {
            @Override
            public String displayName() {
                return "Plot original data";
            }
        };

        public abstract String displayName();
    }
}
