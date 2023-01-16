package Astronomy.multiplot;

import Astronomy.multiplot.settings.KeplerSplineSettings;
import com.astroimagej.bspline.KeplerSpline;
import ij.astro.logging.AIJLogger;
import org.hipparchus.linear.MatrixUtils;
import org.hipparchus.linear.RealVector;
import util.GenericSwingDialog;

import javax.swing.*;
import java.util.Arrays;
import java.util.LinkedList;

public class KeplerSplineControl {
    private int curve;
    private KeplerSplineSettings settings;
    private static final LinkedList<KeplerSplineControl> INSTANCES = new LinkedList<>();

    public KeplerSplineControl(int curve) {
        this.curve = curve;
        settings = new KeplerSplineSettings(curve);
    }

    public static KeplerSplineControl getInstance(int curve) {
        if (INSTANCES.size() <= curve) {
            INSTANCES.add(new KeplerSplineControl(curve));
        }

        return INSTANCES.get(curve);
    }

    public void displayDialog() {
        //todo save location
        var gd = new GenericSwingDialog("Data " + curve + " Smoothing Settings");

        gd.addCheckbox("Ignore transit data in spline fin", settings.getMaskTransit(), b -> {
            settings.setMaskTransit(b);
        });

        gd.addLineSeparator();

        // Display type
        ButtonGroup displayGroup = new ButtonGroup();
        for (KeplerSplineSettings.DisplayType displayType : KeplerSplineSettings.DisplayType.values()) {
            var button = GenericSwingDialog.makeRadioButton(displayType.name(), b -> {
                if (b) settings.setDisplayType(displayType);
            }, displayGroup);

            if (settings.getDisplayType() == displayType) {
                button.setSelected(true);
            }

            gd.addGenericComponent(button);
        }

        gd.addDoubleSpaceLineSeparator();

        // Spline resolution
        ButtonGroup knotGroup = new ButtonGroup();
        var hasSame = true;
        for (KeplerSplineSettings.KnotDensity knotDensity : KeplerSplineSettings.KnotDensity.values()) {
            var button = GenericSwingDialog.makeRadioButton(knotDensity.name(), b -> {
                if (b) settings.setKnotDensity(knotDensity);
            }, knotGroup);

            if (settings.getKnotDensity() == knotDensity) {
                button.setSelected(true);
            }

            gd.addGenericComponent(button);

            if (knotDensity == KeplerSplineSettings.KnotDensity.FIXED) {
                gd.addToSameRow();
                gd.addBoundedNumericField("", new GenericSwingDialog.Bounds(0.01, Double.MAX_VALUE),
                        settings.getFixedKnotDensity(), 1, 4, "Days", d -> {
                            settings.setFixedKnotDensity(d);
                        });
            } else {
                if (hasSame) {
                    gd.addToSameRow();
                    hasSame = false;
                    gd.addBoundedNumericField("", new GenericSwingDialog.Bounds(0.01, Double.MAX_VALUE),
                            settings.getMinKnotDensity(), 1, 4, "(Min) Days", d -> {
                                settings.setMinKnotDensity(d);
                            });
                }
            }
        }

        gd.addBoundedNumericField("", new GenericSwingDialog.Bounds(0.01, Double.MAX_VALUE),
                settings.getMaxKnotDensity(), 1, 4, "(Max) Days", d -> {
                    settings.setMaxKnotDensity(d);
                });

        gd.addBoundedNumericField("", new GenericSwingDialog.Bounds(1, Double.MAX_VALUE),
                settings.getKnotDensitySteps(), 1, 4, "N Checked", true, d -> {
                    settings.setKnotDensitySteps(d.intValue());
                });

        gd.addLineSeparator();

        gd.addBoundedNumericField("Minimum gap width", new GenericSwingDialog.Bounds(0.01, Double.MAX_VALUE),
                settings.getMinGapWidth(), 1, 4, "Days", d -> {
            settings.setMinGapWidth(d);
        });

        gd.addBoundedNumericField("Data cleaning", new GenericSwingDialog.Bounds(0.01, Double.MAX_VALUE),
                settings.getDataCleaningCoeff(), 1, 4, "sigma", d -> {
            settings.setDataCleaningCoeff(d);
        });

        gd.addBoundedNumericField("Data cleaning iterations", new GenericSwingDialog.Bounds(1, Double.MAX_VALUE),
                settings.getKnotDensitySteps(), 1, 4, "", true, d -> {
            settings.setKnotDensitySteps(d.intValue());
        });

        gd.showDialog();
    }

    public void transformData(double[] x, double[] y, int size, RealVector mask) {
        switch (settings.getDisplayType()) {
            case FITTED_SPLINE -> {
                var ks = KeplerSpline.chooseKeplerSplineV2(MatrixUtils.createRealVector(Arrays.copyOf(x,size)),
                        MatrixUtils.createRealVector(Arrays.copyOf(y, size)), settings.getMinKnotDensity(),
                        settings.getMaxKnotDensity(), settings.getKnotDensitySteps(), mask,
                        settings.getMinGapWidth(), true);

                AIJLogger.log("BKSpace for curve " + curve + " is " + ks.second().bkSpace);
                AIJLogger.log("BIC for curve " + curve + " is " + ks.second().bic);

                for (int xx = 0; xx < size; xx++) {
                    y[xx] = ks.first().getEntry(xx);
                }
            }
            case FLATTENED_LIGHT_CURVE -> {
                var ks = KeplerSpline.chooseKeplerSplineV2(MatrixUtils.createRealVector(Arrays.copyOf(x,size)),
                        MatrixUtils.createRealVector(Arrays.copyOf(y, size)), settings.getMinKnotDensity(),
                        settings.getMaxKnotDensity(), settings.getKnotDensitySteps(), mask,
                        settings.getMinGapWidth(), true);

                AIJLogger.log("BKSpace for curve " + curve + " is " + ks.second().bkSpace);
                AIJLogger.log("BIC for curve " + curve + " is " + ks.second().bic);

                for (int xx = 0; xx < size; xx++) {
                    y[xx] /= ks.first().getEntry(xx);
                }
            }
        }
    }
}
