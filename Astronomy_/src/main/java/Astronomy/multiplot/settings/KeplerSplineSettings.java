package Astronomy.multiplot.settings;

public class KeplerSplineSettings {
    private DisplayType displayType = DisplayType.FLATTENED_LIGHT_CURVE;
    private KnotDensity knotDensity = KnotDensity.AUTO;
    private double fixedKnotDensity = 1.5;
    private double minKnotDensity = 0.5;
    private double maxKnotDensity = 20;
    private int knotDensitySteps = 20;
    private double minGapWidth = 0.2;
    private double dataCleaningCoeff = 3;
    private int dataCleaningTries = 5;
    private boolean maskTransit = true;
    private final int curve;

    public KeplerSplineSettings(int curve) {
        this.curve = curve;
    }

    public DisplayType getDisplayType() {
        return displayType;
    }

    public void setDisplayType(DisplayType displayType) {
        this.displayType = displayType;
    }

    public KnotDensity getKnotDensity() {
        return knotDensity;
    }

    public void setKnotDensity(KnotDensity knotDensity) {
        this.knotDensity = knotDensity;
    }

    public double getFixedKnotDensity() {
        return fixedKnotDensity;
    }

    public void setFixedKnotDensity(double fixedKnotDensity) {
        this.fixedKnotDensity = fixedKnotDensity;
    }

    public double getMinKnotDensity() {
        return minKnotDensity;
    }

    public void setMinKnotDensity(double minKnotDensity) {
        this.minKnotDensity = minKnotDensity;
    }

    public double getMaxKnotDensity() {
        return maxKnotDensity;
    }

    public void setMaxKnotDensity(double maxKnotDensity) {
        this.maxKnotDensity = maxKnotDensity;
    }

    public int getKnotDensitySteps() {
        return knotDensitySteps;
    }

    public void setKnotDensitySteps(int knotDensitySteps) {
        this.knotDensitySteps = knotDensitySteps;
    }

    public double getMinGapWidth() {
        return minGapWidth;
    }

    public void setMinGapWidth(double minGapWidth) {
        this.minGapWidth = minGapWidth;
    }

    public double getDataCleaningCoeff() {
        return dataCleaningCoeff;
    }

    public void setDataCleaningCoeff(double dataCleaningCoeff) {
        this.dataCleaningCoeff = dataCleaningCoeff;
    }

    public int getDataCleaningTries() {
        return dataCleaningTries;
    }

    public void setDataCleaningTries(int dataCleaningTries) {
        this.dataCleaningTries = dataCleaningTries;
    }

    public boolean getMaskTransit() {
        return maskTransit;
    }

    public void setMaskTransit(boolean maskTransit) {
        this.maskTransit = maskTransit;
    }

    public enum KnotDensity {
        AUTO,
        FIXED,
    }

    public enum DisplayType {
        FLATTENED_LIGHT_CURVE,
        FITTED_SPLINE,
        RAW_DATA,
    }
}
