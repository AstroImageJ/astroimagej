package Astronomy.multiplot;

import Astronomy.MultiPlot_;
import Astronomy.multiplot.settings.KeplerSplineSettings;
import astroj.IJU;
import com.astroimagej.bspline.KeplerSpline;
import com.astroimagej.bspline.util.Pair;
import flanagan.analysis.Smooth;
import org.hipparchus.linear.MatrixUtils;
import org.hipparchus.linear.RealVector;
import util.GenericSwingDialog;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static Astronomy.MultiPlot_.*;

public class KeplerSplineControl {
    private final int curve;
    private final KeplerSplineSettings settings;
    private Window window = null;
    private static final LinkedHashMap<Integer, KeplerSplineControl> INSTANCES = new LinkedHashMap<>();
    private static final ExecutorService RUNNER = Executors.newSingleThreadExecutor();
    private final JTextField bicDisplay;
    private final JTextField bkSpaceDisplay;
    private final JTextField errorDisplay;
    static DecimalFormat FORMATTER = new DecimalFormat("######0.00", IJU.dfs);

    public KeplerSplineControl(int curve) {
        this.curve = curve;
        settings = new KeplerSplineSettings(curve);
        bkSpaceDisplay = new JTextField("N/A");
        bicDisplay = new JTextField("N/A");
        errorDisplay = new JTextField("Spline fit");
        bicDisplay.setEnabled(false);
        bkSpaceDisplay.setEnabled(false);
        errorDisplay.setEnabled(false);
        bkSpaceDisplay.setHorizontalAlignment(JTextField.CENTER);
        bicDisplay.setHorizontalAlignment(JTextField.CENTER);
        bkSpaceDisplay.setColumns(8);
        bicDisplay.setColumns(8);
        errorDisplay.setColumns(8);
    }

    public static KeplerSplineControl getInstance(int curve) {
        INSTANCES.putIfAbsent(curve, new KeplerSplineControl(curve));

        return INSTANCES.get(curve);
    }

    public void displayPanel() {
        SwingUtilities.invokeLater(() -> {
            if (window != null) {
                if (window.isShowing()) {
                    window.requestFocus();
                } else {
                    window.setVisible(true);
                    window.requestFocus();
                }
                return;
            }
            var p = makePanel();
            p.pack();
            p.setVisible(true);
            window = p;
        });
    }

    public void recallOpenState() {
        if (settings.windowOpened.get()) {
            displayPanel();
        }
    }

    public static void closePanels() {
        SwingUtilities.invokeLater(() -> INSTANCES.forEach(($, s) -> {
            if (s.window != null) {
                s.window.setVisible(false);
                s.window.dispose();
            }
        }));
    }

    private JFrame makePanel() {
        var window = new JFrame("Curve " + (curve + 1) + " Smoothing Settings");
        window.setLocation(settings.windowLocation.get());
        window.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                settings.windowLocation.set(e.getWindow().getLocation());
                settings.windowOpened.set(false);
            }
        });
        window.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentMoved(ComponentEvent e) {
                settings.windowLocation.set(e.getComponent().getLocationOnScreen());
            }
        });
        var panel = new JPanel(new GridBagLayout());
        var c = new GridBagConstraints();
        c.anchor = GridBagConstraints.WEST;

        c.gridwidth = GridBagConstraints.REMAINDER;
        var doMask = new JCheckBox("Ignore transit data in spline fit");
        doMask.setToolTipText("");
        doMask.setSelected(settings.maskTransit.get());
        doMask.addActionListener($ -> {
            settings.maskTransit.set(doMask.isSelected());
            updatePlot();
        });
        doMask.setToolTipText("Causes the spline fit to ignore the data between the left and right marker. " +
                "This option is useful when the location of the transit is known to prevent the transit from " +
                "biasing the spline fit.");
        panel.add(doMask, c);
        c.gridy++;
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

        // Display type
        var displayGroup = new ButtonGroup();
        for (KeplerSplineSettings.DisplayType displayType : KeplerSplineSettings.DisplayType.values()) {
            var radio = new JRadioButton(displayType.displayName());
            radio.addActionListener($ -> {
                settings.displayType.set(displayType);
                updatePlot();
            });
            if (settings.displayType.get() == displayType) {
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
            settings.knotDensity.set(KeplerSplineSettings.KnotDensity.FIXED);
            updatePlot();
        });
        radio.setToolTipText("Fits a spline using a fixed user selected fitting length. Default = 5.0");
        radio.setSelected(settings.knotDensity.get() == KeplerSplineSettings.KnotDensity.FIXED);
        panel.add(radio, c);
        var controlBox = Box.createHorizontalBox();
        var control = new JSpinner(new SpinnerNumberModel(settings.fixedKnotDensity.get().doubleValue(), 0.01, Double.MAX_VALUE, 0.1));
        control.addChangeListener($ -> {
            settings.fixedKnotDensity.set(((Double) control.getValue()));
            updatePlot();
        });
        modifySpinner(control);
        c.gridx = GridBagConstraints.RELATIVE;
        GenericSwingDialog.getTextFieldFromSpinner(control).ifPresent(f -> f.setColumns(5));
        controlBox.add(control);
        var label = new JLabel(" (days)");
        controlBox.add(label);
        panel.add(controlBox, c);
        c.gridx = 0;
        c.gridy++;
        radio = new JRadioButton("Auto knot spacing");
        densityGroup.add(radio);
        radio.addActionListener($ -> {
            settings.knotDensity.set(KeplerSplineSettings.KnotDensity.AUTO);
            updatePlot();
        });
        radio.setToolTipText("Finds the best spline fitting length between Min and Max using N equally spaced steps. " +
                "Defaults are Min = 0.5, Max = 20.0, N = 20.");
        radio.setSelected(settings.knotDensity.get() == KeplerSplineSettings.KnotDensity.AUTO);
        panel.add(radio, c);
        c.gridx = 1;
        controlBox = Box.createHorizontalBox();
        var control1 = new JSpinner(new SpinnerNumberModel(settings.minKnotDensity.get().doubleValue(), 0.01, Double.MAX_VALUE, 0.1));
        control1.addChangeListener($ -> {
            settings.minKnotDensity.set(((Double) control1.getValue()));
            updatePlot();
        });
        modifySpinner(control1);
        GenericSwingDialog.getTextFieldFromSpinner(control1).ifPresent(f -> f.setColumns(5));
        controlBox.add(control1);
        label = new JLabel(" Min (days)");
        controlBox.add(label);
        panel.add(controlBox, c);
        controlBox = Box.createHorizontalBox();
        var control2 = new JSpinner(new SpinnerNumberModel(settings.maxKnotDensity.get().doubleValue(), 0.01, Double.MAX_VALUE, 0.1));
        control2.addChangeListener($ -> {
            settings.maxKnotDensity.set(((Double) control2.getValue()));
            updatePlot();
        });
        modifySpinner(control2);
        clampingSpinners(control1, control2);
        c.gridy++;
        GenericSwingDialog.getTextFieldFromSpinner(control2).ifPresent(f -> f.setColumns(5));
        controlBox.add(control2);
        label = new JLabel(" Max (days)");
        controlBox.add(label);
        panel.add(controlBox, c);
        controlBox = Box.createHorizontalBox();
        var control3 = new JSpinner(new SpinnerNumberModel(settings.knotDensitySteps.get().intValue(), 1, Integer.MAX_VALUE, 1));
        control3.addChangeListener($ -> {
            settings.knotDensitySteps.set(((Integer) control3.getValue()));
            updatePlot();
        });
        c.gridx = 0;
        panel.add(errorDisplay, c);
        c.gridx++;
        c.gridy++;
        GenericSwingDialog.getTextFieldFromSpinner(control3).ifPresent(f -> f.setColumns(5));
        modifySpinner(control3);
        controlBox.add(control3);
        label = new JLabel(" Spline iterations");
        controlBox.add(label);
        panel.add(controlBox, c);
        c.gridx = 0;
        c.gridy++;
        radio = new JRadioButton("Legacy Smoother:");
        densityGroup.add(radio);
        radio.addActionListener($ -> {
            settings.knotDensity.set(KeplerSplineSettings.KnotDensity.LEGACY_SMOOTHER);
            updatePlot();
        });
        radio.setSelected(settings.knotDensity.get() == KeplerSplineSettings.KnotDensity.LEGACY_SMOOTHER);
        panel.add(radio, c);
        c.gridx = 1;
        controlBox = Box.createHorizontalBox();
        var control7 = new JSpinner(new SpinnerNumberModel(settings.smoothLength.get().intValue(), 1, Integer.MAX_VALUE, 1));
        control7.addChangeListener($ -> {
            settings.smoothLength.set(((Number) control7.getValue()).intValue());
            updatePlot();
        });
        modifySpinner(control7);
        GenericSwingDialog.getTextFieldFromSpinner(control7).ifPresent(f -> f.setColumns(5));
        controlBox.add(control7);
        label = new JLabel(" N points");
        controlBox.add(label);
        panel.add(controlBox, c);

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
        controlBox = Box.createHorizontalBox();
        var control4 = new JSpinner(new SpinnerNumberModel(settings.minGapWidth.get().doubleValue(), 0.01, Double.MAX_VALUE, 0.1));
        control4.addChangeListener($ -> {
            settings.minGapWidth.set(((Double) control4.getValue()));
            updatePlot();
        });
        modifySpinner(control4);
        control4.setToolTipText("Smoothing chops the light curve into multiple segments if there are breaks in the data. " +
                "This value sets the minimum gap width that is considered a break in the data. The default is 0.2 days");
        GenericSwingDialog.getTextFieldFromSpinner(control4).ifPresent(f -> f.setColumns(5));
        controlBox.add(control4);
        label = new JLabel(" (days)");
        controlBox.add(label);
        panel.add(controlBox, c);
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
        controlBox = Box.createHorizontalBox();
        var control5 = new JSpinner(new SpinnerNumberModel(settings.dataCleaningCoeff.get().doubleValue(), 0.01, Double.MAX_VALUE, 1));
        control5.addChangeListener($ -> {
            settings.dataCleaningCoeff.set(((Double) control5.getValue()));
            updatePlot();
        });
        modifySpinner(control5);
        control5.setToolTipText("Smoothing masks “bad” data points from the spline fit that are more than the " +
                "N sigma from the fitted spline. The default is N = 3 sigma.");
        GenericSwingDialog.getTextFieldFromSpinner(control5).ifPresent(f -> f.setColumns(5));
        controlBox.add(control5);
        label = new JLabel(" (sigma)");
        controlBox.add(label);
        panel.add(controlBox, c);
        c.gridx = 0;
        c.gridy++;
        label = new JLabel("Data Cleaning Iterations");
        panel.add(label, c);
        c.gridx = GridBagConstraints.RELATIVE;
        controlBox = Box.createHorizontalBox();
        var control6 = new JSpinner(new SpinnerNumberModel(settings.dataCleaningTries.get().intValue(), 1, Integer.MAX_VALUE, 1));
        control6.addChangeListener($ -> {
            settings.dataCleaningTries.set(((Number) control6.getValue()).intValue());
            updatePlot();
        });
        modifySpinner(control6);
        control6.setToolTipText("Cleaning iterates spline fitting and data cleaning up to a maximum number of " +
                "iterations. The default is a maximum of N = 5 iterations");
        GenericSwingDialog.getTextFieldFromSpinner(control6).ifPresent(f -> f.setColumns(5));
        controlBox.add(control6);
        panel.add(controlBox, c);
        c.gridy++;

        c.gridx = 0;
        var button = new JButton("Apply current settings for all curves");
        button.addActionListener($ -> duplicateSettings(settings));
        panel.add(button, c);

        c.gridx++;
        c.anchor = GridBagConstraints.CENTER;
        var button1 = new JButton("Ok");
        button1.addActionListener($ -> window.setVisible(false));
        panel.add(button1, c);

        panel.getInsets().set(15, 15 ,15, 15);

        window.setIconImage(createImageIcon("astroj/images/plot.png", "Plot Icon").getImage());
        window.add(panel);

        settings.windowOpened.set(true);
        return window;
    }

    public void transformData(double[] x, double[] y, int size, RealVector mask) {
        var ks = makeKs().fit(x, y, size, mask);

        if (settings.knotDensity.get() == KeplerSplineSettings.KnotDensity.LEGACY_SMOOTHER) {
            bkSpaceDisplay.setText("N/A");
            bicDisplay.setText("N/A");
        } else {
            var lastSplineFit = ks.second();
            if (lastSplineFit.bic == null) {
                bkSpaceDisplay.setText("N/A");
                bicDisplay.setText("N/A");
                errorDisplay.setText("Fit failed");
                Arrays.fill(y, Double.NaN);
                return;
            }

            errorDisplay.setText("Fit success");
            bkSpaceDisplay.setText(FORMATTER.format(lastSplineFit.bkSpace));
            bicDisplay.setText(FORMATTER.format(lastSplineFit.bic));
        }

        switch (settings.displayType.get()) {
            case FITTED_SPLINE -> {
                for (int xx = 0; xx < size; xx++) {
                    y[xx] = ks.first().getEntry(xx);
                }
            }
            case FLATTENED_LIGHT_CURVE -> {
                var avg = Arrays.stream(y).limit(size).summaryStatistics().getAverage();
                for (int xx = 0; xx < size; xx++) {
                    y[xx] = (y[xx] - ks.first().getEntry(xx)) + avg;
                }
            }
        }
    }

    private KeplerSplineApplicator makeKs() {
        return switch (settings.knotDensity.get()) {
            case FIXED -> (xs, ys, size, mask) ->
                    KeplerSpline.keplerSplineV2(MatrixUtils.createRealVector(Arrays.copyOf(xs,size)),
                    MatrixUtils.createRealVector(Arrays.copyOf(ys, size)), settings.fixedKnotDensity.get(),
                            settings.maskTransit.ifProp(mask), settings.minGapWidth.get(), settings.dataCleaningTries.get(),
                            settings.dataCleaningCoeff.get(), true);
            case AUTO -> (xs, ys, size, mask) ->
                    KeplerSpline.chooseKeplerSplineV2(MatrixUtils.createRealVector(Arrays.copyOf(xs,size)),
                    MatrixUtils.createRealVector(Arrays.copyOf(ys, size)), settings.minKnotDensity.get(),
                    settings.maxKnotDensity.get(), settings.knotDensitySteps.get(), settings.maskTransit.ifProp(mask),
                    settings.minGapWidth.get(), settings.dataCleaningTries.get(), settings.dataCleaningCoeff.get(), true);
            case LEGACY_SMOOTHER -> (xs, ys, size, mask) -> {
                if (size <= 2*settings.smoothLength.get()) {
                    var o = MatrixUtils.createRealVector(size);
                    o.set(Double.NaN);
                    return new Pair<>(o, null);
                }
                double[] xphase = new double[size];
                double[] yphase = new double[size];
                double xfold;
                int nskipped = 0;
                double xmax = Double.NEGATIVE_INFINITY;
                double xmin = Double.POSITIVE_INFINITY;
                double halfPeriod = netPeriod / 2.0;
                for (int xx = 0; xx < size; xx++) {
                    xfold = ((xs[xx] - netT0) % netPeriod);
                    if (xfold > halfPeriod) { xfold -= netPeriod; } else if (xfold < -halfPeriod) xfold += netPeriod;
                    if (Math.abs(xfold) < duration / 48.0) {
                        nskipped++;
                    } else {
                        yphase[xx - nskipped] = ys[xx];
                        xphase[xx - nskipped] = xs[xx] - (int) xs[0];
                        if (xs[xx] > xmax) xmax = xs[xx];
                        if (xs[xx] < xmin) xmin = xs[xx];
                    }
                }

                var xl = new double[size - nskipped];
                var yl = new double[size - nskipped];
                for (int xx = 0; xx < size - nskipped; xx++) {
                    yl[xx] = yphase[xx];
                    xl[xx] = xphase[xx];
                }

                if (size - nskipped > 2 * settings.smoothLength.get()) {
                    Smooth csm = new Smooth(xl, yl);
                    csm.savitzkyGolay(settings.smoothLength.get());
                    csm.setSGpolyDegree(2);

                    double finalXmax = xmax;
                    double finalXmin = xmin;

                    return new Pair<>(MatrixUtils.createRealVector(Arrays.stream(xs).limit(size).map(x -> {
                        if (x > finalXmax) {
                            return csm.interpolateSavitzkyGolay(finalXmax - (int) xs[0]);
                        } else if (x < finalXmin) {
                            return csm.interpolateSavitzkyGolay(finalXmin - (int) xs[0]);
                        } else {
                            return csm.interpolateSavitzkyGolay(x - (int) xs[0]);
                        }
                    }).toArray()), null);
                }

                return new Pair<>(null, null);
            };
        };
    }

    private void modifySpinner(JSpinner spinner) {
        for (Component component : spinner.getComponents()) {
            if (component instanceof JSpinner.DefaultEditor editor) {
                editor.addMouseWheelListener(e -> {
                    if (spinner.getModel() instanceof SpinnerNumberModel spin) {
                        var delta = e.getPreciseWheelRotation() * spin.getStepSize().doubleValue();
                        var newValue = -delta + ((Number) spinner.getValue()).doubleValue();

                        if (newValue < ((Number) spin.getMinimum()).doubleValue()) {
                            newValue = ((Number) spin.getMinimum()).doubleValue();
                        } else if (newValue > ((Number) spin.getMaximum()).doubleValue()) {
                            newValue = ((Number) spin.getMaximum()).doubleValue();
                        }

                        spinner.setValue(spin.getMaximum() instanceof Integer ? (int) newValue : newValue);
                    }
                });

                editor.addKeyListener(new KeyAdapter() {
                    @Override
                    public void keyTyped(KeyEvent e) {
                        super.keyTyped(e);
                        if (e.getKeyCode() == KeyEvent.VK_UP) {
                            getModel(spinner).setValue(spinner.getNextValue());
                        }

                        if (e.getKeyCode() == KeyEvent.VK_DOWN) {
                            getModel(spinner).setValue(spinner.getPreviousValue());
                        }
                    }
                });
            }
        }
    }

    private void clampingSpinners(JSpinner min, JSpinner max) {
        var maxModel = getModel(max);
        var minModel = getModel(min);
        min.addChangeListener($ -> {
            if (minModel.getNumber().doubleValue() >= maxModel.getNumber().doubleValue()) {
                minModel.setValue(minModel.getPreviousValue());
            }
        });
        max.addChangeListener($ -> {
            if (minModel.getNumber().doubleValue() >= maxModel.getNumber().doubleValue()) {
                maxModel.setValue(maxModel.getNextValue());
            }
        });
        //todo right click
    }

    private SpinnerNumberModel getModel(JSpinner spinner) {
        if (spinner.getModel() instanceof SpinnerNumberModel spin) {
            return spin;
        }

        return null;
    }

    private void duplicateSettings(KeplerSplineSettings from) {
        INSTANCES.forEach((integer, keplerSplineControl) -> keplerSplineControl.settings.duplicateSettings(from));
        updatePlot();
    }

    private void updatePlot() {
        RUNNER.submit(() -> MultiPlot_.updatePlot());
    }

    @FunctionalInterface
    interface KeplerSplineApplicator {
        com.astroimagej.bspline.util.Pair<org.hipparchus.linear.RealVector, KeplerSpline.SplineMetadata> fit(double[] xs, double[] ys, int size, RealVector mask);
    }
}
