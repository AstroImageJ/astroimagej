package Astronomy.multiplot;

import Astronomy.MultiPlot_;
import Astronomy.multiplot.settings.KeplerSplineSettings;
import astroj.IJU;
import com.astroimagej.bspline.KeplerSpline;
import org.hipparchus.linear.MatrixUtils;
import org.hipparchus.linear.RealVector;
import util.GenericSwingDialog;

import javax.swing.*;
import java.awt.*;
import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class KeplerSplineControl {
    private int curve;
    private KeplerSplineSettings settings;
    private KeplerSpline.SplineMetadata lastSplineFit = null;
    private static final LinkedList<KeplerSplineControl> INSTANCES = new LinkedList<>();
    private static final ExecutorService RUNNER = Executors.newSingleThreadExecutor();
    private JTextField bicDisplay;
    private JTextField bkSpaceDisplay;
    static DecimalFormat FORMATTER = new DecimalFormat("######0.00", IJU.dfs);

    public KeplerSplineControl(int curve) {
        this.curve = curve;
        settings = new KeplerSplineSettings(curve);
        bkSpaceDisplay = new JTextField("N/A");
        bicDisplay = new JTextField("N/A");
        bicDisplay.setEnabled(false);
        bkSpaceDisplay.setEnabled(false);
        bkSpaceDisplay.setHorizontalAlignment(JTextField.CENTER);
        bicDisplay.setHorizontalAlignment(JTextField.CENTER);
        bkSpaceDisplay.setColumns(8);
        bicDisplay.setColumns(8);
    }

    public static KeplerSplineControl getInstance(int curve) {
        if (INSTANCES.size() <= curve) {
            INSTANCES.add(new KeplerSplineControl(curve));
        }

        return INSTANCES.get(curve);
    }

    public void displayPanel() {
        //todo save location
        SwingUtilities.invokeLater(() -> {
            var p = makePanel();
            p.pack();
            p.setVisible(true);
        });
    }

    private JFrame makePanel() {
        var window = new JFrame("Curve " + curve + " Smoothing Settings");
        var panel = new JPanel(new GridBagLayout());
        var c = new GridBagConstraints();
        c.anchor = GridBagConstraints.WEST;

        c.gridwidth = GridBagConstraints.REMAINDER;
        var doMask = new JCheckBox("Ignore transit data in spline fit");
        doMask.setToolTipText("");
        doMask.setSelected(settings.getMaskTransit());
        doMask.addActionListener($ -> {
            settings.setMaskTransit(doMask.isSelected());
            updatePlot();
        });
        panel.add(doMask, c);
        c.gridwidth = 1;
        c.gridy++;

        // Display type
        var displayGroup = new ButtonGroup();
        for (KeplerSplineSettings.DisplayType displayType : KeplerSplineSettings.DisplayType.values()) {
            var radio = new JRadioButton(displayType.name());
            radio.addActionListener($ -> {
                settings.setDisplayType(displayType);
                updatePlot();
            });
            if (settings.getDisplayType() == displayType) {
                radio.setSelected(true);
            }
            displayGroup.add(radio);
            c.gridy++;
            panel.add(radio, c);
        }
        c.gridy++;
        c.fill = GridBagConstraints.BOTH;
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.weighty = 1;
        panel.add(new JSeparator(), c);
        c.fill = GridBagConstraints.NONE;
        c.gridwidth = 1;
        c.weighty = 0;
        c.gridy++;

        // Knot density
        var densityGroup = new ButtonGroup();
        var radio = new JRadioButton("Fixed knot spacing:");
        densityGroup.add(radio);
        radio.addActionListener($ -> {
            settings.setKnotDensity(KeplerSplineSettings.KnotDensity.FIXED);
            updatePlot();
        });
        radio.setSelected(settings.getKnotDensity() == KeplerSplineSettings.KnotDensity.FIXED);
        panel.add(radio, c);
        var control = new JSpinner(new SpinnerNumberModel(settings.getFixedKnotDensity(), 0.01, Double.MAX_VALUE, 1));
        control.addChangeListener($ -> {
            settings.setFixedKnotDensity(((Double) control.getValue()));
            updatePlot();
        });
        c.gridx = GridBagConstraints.RELATIVE;
        GenericSwingDialog.getTextFieldFromSpinner(control).ifPresent(f -> f.setColumns(5));
        panel.add(control, c);
        var label = new JLabel(" (days)");
        panel.add(label, c);
        c.gridx = 0;
        c.gridy++;
        radio = new JRadioButton("Auto knot spacing");
        densityGroup.add(radio);
        radio.addActionListener($ -> {
            settings.setKnotDensity(KeplerSplineSettings.KnotDensity.AUTO);
            updatePlot();
        });
        radio.setSelected(settings.getKnotDensity() == KeplerSplineSettings.KnotDensity.AUTO);
        panel.add(radio, c);
        c.gridx = 1;
        var control1 = new JSpinner(new SpinnerNumberModel(settings.getMinKnotDensity(), 0.01, Double.MAX_VALUE, 1));
        control1.addChangeListener($ -> {
            settings.setMinKnotDensity(((Double) control1.getValue()));
            updatePlot();
        });
        GenericSwingDialog.getTextFieldFromSpinner(control1).ifPresent(f -> f.setColumns(5));
        panel.add(control1, c);
        label = new JLabel(" Min (days)");
        c.gridx++;
        panel.add(label, c);
        c.gridx--;
        var control2 = new JSpinner(new SpinnerNumberModel(settings.getMaxKnotDensity(), 0.01, Double.MAX_VALUE, 1));
        control2.addChangeListener($ -> {
            settings.setMaxKnotDensity(((Double) control2.getValue()));
            updatePlot();
        });
        c.gridy++;
        GenericSwingDialog.getTextFieldFromSpinner(control2).ifPresent(f -> f.setColumns(5));
        panel.add(control2, c);
        label = new JLabel(" Max (days)");
        c.gridx++;
        panel.add(label, c);
        c.gridx--;
        var control3 = new JSpinner(new SpinnerNumberModel(settings.getKnotDensitySteps(), 1, Integer.MAX_VALUE, 1));
        control3.addChangeListener($ -> {
            settings.setKnotDensitySteps(((Integer) control3.getValue()));
            updatePlot();
        });
        c.gridy++;
        GenericSwingDialog.getTextFieldFromSpinner(control3).ifPresent(f -> f.setColumns(5));
        panel.add(control3, c);
        label = new JLabel(" Spline iterations");
        c.gridx++;
        panel.add(label, c);

        c.gridx = 0;
        c.gridy++;
        c.fill = GridBagConstraints.BOTH;
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.weighty = 1;
        panel.add(new JSeparator(), c);
        c.fill = GridBagConstraints.NONE;
        c.gridwidth = 1;
        c.weighty = 0;

        // Metadata display
        var box = Box.createHorizontalBox();
        box.add(new JLabel("Knot Spacing: "));
        box.add(bkSpaceDisplay);
        panel.add(box, c);
        box = Box.createHorizontalBox();
        c.gridx = 1;
        box.add(new JLabel("BIC: "));
        box.add(bicDisplay);
        panel.add(box, c);
        c.gridy++;
        c.gridx = 0;

        c.fill = GridBagConstraints.BOTH;
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.weighty = 1;
        panel.add(new JSeparator(), c);
        c.fill = GridBagConstraints.NONE;
        c.gridwidth = 1;
        c.weighty = 0;
        c.gridy++;
        label = new JLabel("Minimum Gap Width");
        panel.add(label, c);
        c.gridx = GridBagConstraints.RELATIVE;
        var control4 = new JSpinner(new SpinnerNumberModel(settings.getMinGapWidth(), 0.01, Double.MAX_VALUE, 1));
        control4.addChangeListener($ -> {
            settings.setMinGapWidth(((Double) control4.getValue()));
            updatePlot();
        });
        GenericSwingDialog.getTextFieldFromSpinner(control4).ifPresent(f -> f.setColumns(5));
        panel.add(control4, c);
        label = new JLabel(" (days)");
        panel.add(label, c);
        c.gridy++;
        c.fill = GridBagConstraints.BOTH;
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.weighty = 1;
        panel.add(new JSeparator(), c);
        c.fill = GridBagConstraints.NONE;
        c.gridwidth = 1;
        c.weighty = 0;
        c.gridy++;
        c.gridx = 0;
        label = new JLabel("Data Cleaning");
        panel.add(label, c);
        c.gridx = GridBagConstraints.RELATIVE;
        var control5 = new JSpinner(new SpinnerNumberModel(settings.getDataCleaningCoeff(), 0.01, Double.MAX_VALUE, 1));
        control5.addChangeListener($ -> {
            settings.setDataCleaningCoeff(((Double) control5.getValue()));
            updatePlot();
        });
        GenericSwingDialog.getTextFieldFromSpinner(control5).ifPresent(f -> f.setColumns(5));
        panel.add(control5, c);
        label = new JLabel(" (sigma)");
        panel.add(label, c);
        c.gridx = 0;
        c.gridy++;
        label = new JLabel("Data Cleaning Iterations");
        panel.add(label, c);
        c.gridx = GridBagConstraints.RELATIVE;
        var control6 = new JSpinner(new SpinnerNumberModel(settings.getDataCleaningTries(), 1, Integer.MAX_VALUE, 1));
        control6.addChangeListener($ -> {
            settings.setDataCleaningTries(((Integer) control6.getValue()));
            updatePlot();
        });
        GenericSwingDialog.getTextFieldFromSpinner(control6).ifPresent(f -> f.setColumns(5));
        panel.add(control6, c);
        c.gridy++;
        c.gridx = GridBagConstraints.RELATIVE;

        window.add(panel);
        return window;
    }

    public void transformData(double[] x, double[] y, int size, RealVector mask) {
        var ks = makeKs().fit(x, y, size, mask);

        lastSplineFit = ks.second();
        if (lastSplineFit.bic == null) {
            bkSpaceDisplay.setText("N/A");
            bicDisplay.setText("N/A");
            Arrays.fill(y, Double.NaN);
            return;
        }

        bkSpaceDisplay.setText(FORMATTER.format(lastSplineFit.bkSpace));
        bicDisplay.setText(FORMATTER.format(lastSplineFit.bic));

        switch (settings.getDisplayType()) {
            case FITTED_SPLINE -> {
                for (int xx = 0; xx < size; xx++) {
                    y[xx] = ks.first().getEntry(xx);
                }
            }
            case FLATTENED_LIGHT_CURVE -> {
                for (int xx = 0; xx < size; xx++) {
                    y[xx] /= ks.first().getEntry(xx);
                }
            }
        }
    }

    private KeplerSplineApplicator makeKs() {
        return switch (settings.getKnotDensity()) {
            case FIXED -> (xs, ys, size, mask) ->
                    KeplerSpline.keplerSplineV2(MatrixUtils.createRealVector(Arrays.copyOf(xs,size)),
                    MatrixUtils.createRealVector(Arrays.copyOf(ys, size)), settings.getFixedKnotDensity(),
                    mask, settings.getMinGapWidth(), settings.getDataCleaningTries(),
                            settings.getDataCleaningCoeff(),true);
            case AUTO -> (xs, ys, size, mask) ->
                    KeplerSpline.chooseKeplerSplineV2(MatrixUtils.createRealVector(Arrays.copyOf(xs,size)),
                    MatrixUtils.createRealVector(Arrays.copyOf(ys, size)), settings.getMinKnotDensity(),
                    settings.getMaxKnotDensity(), settings.getKnotDensitySteps(), mask,
                    settings.getMinGapWidth(), settings.getDataCleaningTries(), settings.getDataCleaningCoeff(),true);
        };
    }

    private void updatePlot() {
        RUNNER.submit(() -> MultiPlot_.updatePlot());
    }

    @FunctionalInterface
    interface KeplerSplineApplicator {
        com.astroimagej.bspline.util.Pair<org.hipparchus.linear.RealVector, KeplerSpline.SplineMetadata> fit(double[] xs, double[] ys, int size, RealVector mask);
    }
}
