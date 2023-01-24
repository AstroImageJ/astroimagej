package Astronomy.multiplot.settings;

import ij.astro.io.prefs.Property;

import java.awt.*;
import java.lang.reflect.Field;

public class KeplerSplineSettings {
    public final Property<DisplayType> displayType;
    public final Property<KnotDensity> knotDensity;
    public final Property<Double> fixedKnotDensity;
    public final Property<Double> minKnotDensity;
    public final Property<Double> maxKnotDensity;
    public final Property<Integer> knotDensitySteps;
    public final Property<Double> minGapWidth;
    public final Property<Double> dataCleaningCoeff;
    public final Property<Integer> dataCleaningTries;
    public final Property<Integer> smoothLength;
    public final Property<Boolean> maskTransit;
    public final Property<Point> windowLocation;
    private final int curve;

    public KeplerSplineSettings(int curve) {
        this.curve = curve;
        displayType = makeProperty(DisplayType.FLATTENED_LIGHT_CURVE);
        knotDensity = makeProperty(KnotDensity.AUTO);
        fixedKnotDensity = makeProperty(1.5);
        minKnotDensity = makeProperty(0.5);
        maxKnotDensity = makeProperty(20D);
        knotDensitySteps = makeProperty(20);
        minGapWidth = makeProperty(0.2);
        dataCleaningCoeff = makeProperty(3D);
        dataCleaningTries = makeProperty(5);
        smoothLength = makeProperty(31);
        maskTransit = makeProperty(true);
        windowLocation = new Property<>(new Point(), "", String.valueOf(curve), this);
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

    private <T> Property<T> makeProperty(T defaultValue) {
        return new Property<>(defaultValue, "plot.", String.valueOf(curve), this);
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
