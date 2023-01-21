package Astronomy.multiplot.settings;

import ij.astro.io.prefs.Property;

import java.awt.*;
import java.lang.reflect.Field;

public class KeplerSplineSettings {
    public Property<DisplayType> displayType;
    public Property<KnotDensity> knotDensity;
    public Property<Double> fixedKnotDensity;
    public Property<Double> minKnotDensity;
    public Property<Double> maxKnotDensity;
    public Property<Integer> knotDensitySteps;
    public Property<Double> minGapWidth;
    public Property<Double> dataCleaningCoeff;
    public Property<Integer> dataCleaningTries;
    public Property<Integer> smoothLength;
    public Property<Boolean> maskTransit;
    public Property<Point> windowLocation;
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
        FLATTENED_LIGHT_CURVE,
        FITTED_SPLINE,
        RAW_DATA,
    }
}