package Astronomy.multiplot;

import Astronomy.MultiPlot_;
import Astronomy.multiplot.settings.KeplerSplineSettings;
import astroj.IJU;
import com.astroimagej.bspline.KeplerSpline;
import com.astroimagej.bspline.util.Pair;
import flanagan.analysis.Smooth;
import ij.astro.gui.GenericSwingDialog;
import ij.astro.util.EmojiIcon;
import ij.astro.util.UIHelper;
import org.hipparchus.linear.MatrixUtils;
import org.hipparchus.linear.RealVector;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static Astronomy.MultiPlot_.*;

public class KeplerSplineControl {
    private final int curve;
    public final KeplerSplineSettings settings;
    private Window window = null;
    private static final LinkedHashMap<Integer, KeplerSplineControl> INSTANCES = new LinkedHashMap<>();
    private static final ExecutorService RUNNER = Executors.newSingleThreadExecutor();
    private final JTextField bicDisplay;
    private final JTextField bkSpaceDisplay;
    private final JTextField errorDisplay;
    static DecimalFormat FORMATTER = new DecimalFormat("######0.00", IJU.dfs);
    private final HashSet<FitListener> fitListeners = new HashSet<>();

    public KeplerSplineControl(int curve) {
        this.curve = curve;
        settings = new KeplerSplineSettings(curve);
        bkSpaceDisplay = new JTextField("N/A");
        bicDisplay = new JTextField("N/A");
        errorDisplay = new JTextField("N/A");
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
                s.settings.fixedKnotDensity.clearListeners();
                s.settings.displayType.clearListeners();
            }
        }));
    }

    public String ifTransitSmoothed(String s) {
        if (MultiPlot_.smooth[curve] &&
                settings.displayType.get() == KeplerSplineSettings.DisplayType.FLATTENED_LIGHT_CURVE) {
            return s;
        }

        return "";
    }

    private JFrame makePanel() {
        var window = new JFrame("Curve " + (curve + 1) + " Spline Smoothing");
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
        var doMask = new JCheckBox("Mask transit data in spline fit");
        doMask.setToolTipText("");
        doMask.setSelected(settings.maskTransit.get());
        doMask.addActionListener($ -> {
            settings.maskTransit.set(doMask.isSelected());
            updatePlot();
        });
        doMask.setToolTipText("<html>Causes the spline fit to ignore the data between the left and right marker. " +
                "<br>This option is useful when the location of the transit is known to prevent the transit from " +
                "<br>biasing the spline fit.</html>");
        panel.add(doMask, c);
        c.gridy++;
        c.gridy++;

        var doMaskData = new JCheckBox("Mask trimmed data in spline fit");
        doMaskData.setToolTipText("");
        doMaskData.setSelected(settings.maskTrimmedData.get());
        doMaskData.addActionListener($ -> {
            settings.maskTrimmedData.set(doMaskData.isSelected());
            updatePlot();
        });
        doMaskData.setToolTipText("<html>Causes the spline fit to ignore the data to the " +
                "<br>left of the left trim marker and/or the right of the right trim marker, if enabled.</html>");
        panel.add(doMaskData, c);
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
            if (settings.displayType.get() == displayType) {
                radio.setSelected(true);
            }
            settings.displayType.addListener((k, d) -> {
                if (d == displayType) {
                    radio.setSelected(true);
                }
            });
            radio.addActionListener($ -> {
                settings.displayType.set(displayType);
                if (displayType == KeplerSplineSettings.DisplayType.RAW_DATA) {
                    bkSpaceDisplay.setText("N/A");
                    bicDisplay.setText("N/A");
                    errorDisplay.setText("N/A");
                    errorDisplay.setDisabledTextColor(Color.BLACK);
                }
            });
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
        var radioFixedKnot = new JRadioButton("KeplerSpline fixed knot spacing:");
        densityGroup.add(radioFixedKnot);
        radioFixedKnot.addActionListener($ -> {
            settings.knotDensity.set(KeplerSplineSettings.KnotDensity.FIXED);
            updatePlot();
        });
        radioFixedKnot.setToolTipText("Uses keplerspline to fit a spline using a fixed user selected knot spacing. Default = 0.5");
        radioFixedKnot.setSelected(settings.knotDensity.get() == KeplerSplineSettings.KnotDensity.FIXED);
        panel.add(radioFixedKnot, c);
        var controlBox = Box.createHorizontalBox();
        var control = new JSpinner(new SpinnerNumberModel(settings.fixedKnotDensity.get().doubleValue(), 0.01, Double.MAX_VALUE, 0.1));
        settings.fixedKnotDensity.addListener(($, d) -> control.setValue(d));
        control.addChangeListener($ -> {
            settings.fixedKnotDensity.set(((Double) control.getValue()));
            if (settings.knotDensity.get() == KeplerSplineSettings.KnotDensity.FIXED) {
                updatePlot();
            }
        });
        control.setToolTipText("The user selected knot spacing of the spline fit.");
        modifySpinner(control);
        c.gridx = GridBagConstraints.RELATIVE;
        GenericSwingDialog.getTextFieldFromSpinner(control).ifPresent(f -> f.setColumns(5));
        controlBox.add(control);
        var label = new JLabel(" (days)");
        controlBox.add(label);
        panel.add(controlBox, c);
        c.gridx = 0;
        c.gridy++;
        var radio = new JRadioButton("KeplerSpline auto knot spacing");
        densityGroup.add(radio);
        radio.addActionListener($ -> {
            settings.knotDensity.set(KeplerSplineSettings.KnotDensity.AUTO);
            updatePlot();
        });
        radio.setToolTipText("<html>Uses keplerspline and finds the best fit spline knot spacing between Min and Max using N equally spaced steps. " +
                "<br>Defaults are Min = 0.5, Max = 20.0, N = 20.</html>");
        radio.setSelected(settings.knotDensity.get() == KeplerSplineSettings.KnotDensity.AUTO);
        panel.add(radio, c);
        c.gridx = 1;
        controlBox = Box.createHorizontalBox();
        var control1 = new JSpinner(new SpinnerNumberModel(settings.minKnotDensity.get().doubleValue(), 0.01, Double.MAX_VALUE, 0.1));
        control1.setToolTipText("The minimum spline knot spacing considered for the fit.");
        control1.addChangeListener($ -> {
            settings.minKnotDensity.set(((Double) control1.getValue()));
            if (settings.knotDensity.get() == KeplerSplineSettings.KnotDensity.AUTO) {
                updatePlot();
            }
        });
        modifySpinner(control1);
        GenericSwingDialog.getTextFieldFromSpinner(control1).ifPresent(f -> f.setColumns(5));
        controlBox.add(control1);
        label = new JLabel(" Min (days)");
        label.setToolTipText("The minimum spline knot spacing considered for the fit.");
        controlBox.add(label);
        panel.add(controlBox, c);
        controlBox = Box.createHorizontalBox();
        var control2 = new JSpinner(new SpinnerNumberModel(settings.maxKnotDensity.get().doubleValue(), 0.01, Double.MAX_VALUE, 0.1));
        control2.setToolTipText("The maximum spline knot spacing considered for the fit.");
        control2.addChangeListener($ -> {
            settings.maxKnotDensity.set(((Double) control2.getValue()));
            if (settings.knotDensity.get() == KeplerSplineSettings.KnotDensity.AUTO) {
                updatePlot();
            }
        });
        modifySpinner(control2);
        clampingSpinners(control1, control2);
        c.gridy++;
        GenericSwingDialog.getTextFieldFromSpinner(control2).ifPresent(f -> f.setColumns(5));
        controlBox.add(control2);
        label = new JLabel(" Max (days)");
        label.setToolTipText("The maximum spline knot spacing considered for the fit.");
        controlBox.add(label);
        panel.add(controlBox, c);
        controlBox = Box.createHorizontalBox();
        var control3 = new JSpinner(new SpinnerNumberModel(settings.knotDensitySteps.get().intValue(), 1, Integer.MAX_VALUE, 1));
        control3.setToolTipText("The number of knot spacings between min and max that are considered when finding the best spline fit.");
        control3.addChangeListener($ -> {
            settings.knotDensitySteps.set(((Integer) control3.getValue()));
            if (settings.knotDensity.get() == KeplerSplineSettings.KnotDensity.AUTO) {
                updatePlot();
            }
        });
        c.gridx = 0;
        var b = Box.createHorizontalBox();
        errorDisplay.setToolTipText("Indicates the success or failure of the spline fit. If failed, try a different knot spacing.");
        b.add(new JLabel("Fit Success: "));
        b.setToolTipText(errorDisplay.getToolTipText());
        b.add(errorDisplay);
        panel.add(b, c);
        c.gridx++;
        c.gridy++;
        GenericSwingDialog.getTextFieldFromSpinner(control3).ifPresent(f -> f.setColumns(5));
        modifySpinner(control3);
        controlBox.add(control3);
        c.gridx = 0;
        b = Box.createHorizontalBox();
        b.add(new JLabel("Knot Spacing: "));
        b.add(bkSpaceDisplay);
        b.setToolTipText("Indicates the fitted or fixed spline knot spacing.");
        bkSpaceDisplay.setToolTipText("Indicates the fitted or fixed spline knot spacing.");
        var copyButton = new JButton(new EmojiIcon("⮭", 19));
        copyButton.setMargin(new Insets(0, 3, 0, 3));
        copyButton.addActionListener($ -> {
            settings.fixedKnotDensity.set(Double.valueOf(bkSpaceDisplay.getText()));
            radioFixedKnot.setSelected(true);
            settings.knotDensity.set(KeplerSplineSettings.KnotDensity.FIXED);
            updatePlot();
        });
        copyButton.setToolTipText("Copy auto knot spacing to fixed.");
        b.add(copyButton);
        c.gridwidth = 3;
        panel.add(b, c);
        c.gridwidth = 1;
        c.gridx++;
        label = new JLabel(" Spline iterations");
        label.setToolTipText("The number of knot spacings between min and max that are considered when finding the best spline fit.");
        controlBox.add(label);
        panel.add(controlBox, c);
        c.gridx = 0;
        c.gridy++;
        radio = new JRadioButton("Legacy spline smoother:");
        radio.setToolTipText("<html>Uses the original AIJ spline smoothing. Keplerspline is more robust,<br>handles mixed FFI exposure times, multiple sectors, and is now preferred.</html>");
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
        control7.setToolTipText("<html>This is the smoothing length in units if data samples (not time).<br> Default is 31 for 30 min TESS FFIs, 93 for 10 min FFIs, and 279 for 200 sec FFIs.<br>Multiple sectors with breaks in the data and mixed FFI exposure times are not handled properly by legacy smoother.</html>");
        control7.addChangeListener($ -> {
            settings.smoothLength.set(((Number) control7.getValue()).intValue());
            if (settings.knotDensity.get() == KeplerSplineSettings.KnotDensity.LEGACY_SMOOTHER) {
                updatePlot();
            }
        });
        modifySpinner(control7);
        GenericSwingDialog.getTextFieldFromSpinner(control7).ifPresent(f -> f.setColumns(5));
        controlBox.add(control7);
        label = new JLabel(" N points");
        label.setToolTipText("<html>This is the smoothing length in units if data samples (not time).<br> Default is 31 for 30 min TESS FFIs, 93 for 10 min FFIs, and 279 for 200 sec FFIs.<br>Multiple sectors with breaks in the data and mixed FFI exposure times are not handled properly by legacy smoother.</html>");
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
        label.setToolTipText("<html>KeplerSpline chops the light curve into multiple segments if there<br>are breaks in the data (e.g. gaps between TESS sectors). " +
                "<br>This value sets the minimum gap width that is considered a break in the data. <br>The default is 0.2 days</html>");
        panel.add(label, c);
        c.gridx = GridBagConstraints.RELATIVE;
        controlBox = Box.createHorizontalBox();
        controlBox.setToolTipText("<html>KeplerSpline chops the light curve into multiple segments if there<br>are breaks in the data (e.g. gaps between TESS sectors). " +
                "<br>This value sets the minimum gap width that is considered a break in the data. <br>The default is 0.2 days</html>");
        var control4 = new JSpinner(new SpinnerNumberModel(settings.minGapWidth.get().doubleValue(), 0.01, Double.MAX_VALUE, 0.1));
        control4.addChangeListener($ -> {
            settings.minGapWidth.set(((Double) control4.getValue()));
            updatePlot();
        });
        modifySpinner(control4);
        control4.setToolTipText("<html>KeplerSpline chops the light curve into multiple segments if there<br>are breaks in the data (e.g. gaps between TESS sectors). " +
                "<br>This value sets the minimum gap width that is considered a break in the data. <br>The default is 0.2 days</html>");
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
        label.setToolTipText("<html>Smoothing masks “bad” data points from the spline fit that are more than " +
                "<br>N sigma from the fitted spline. The default is N = 3 sigma.<html>");
        panel.add(label, c);
        c.gridx = GridBagConstraints.RELATIVE;
        controlBox = Box.createHorizontalBox();
        var control5 = new JSpinner(new SpinnerNumberModel(settings.dataCleaningCoeff.get().doubleValue(), 0.01, Double.MAX_VALUE, 1));
        control5.addChangeListener($ -> {
            settings.dataCleaningCoeff.set(((Double) control5.getValue()));
            updatePlot();
        });
        modifySpinner(control5);
        control5.setToolTipText("<html>Smoothing masks “bad” data points from the spline fit that are more than " +
                "<br>N sigma from the fitted spline. The default is N = 3 sigma.</html>");
        GenericSwingDialog.getTextFieldFromSpinner(control5).ifPresent(f -> f.setColumns(5));
        controlBox.add(control5);
        label = new JLabel(" (sigma)");
        controlBox.add(label);
        panel.add(controlBox, c);
        c.gridx = 0;
        c.gridy++;
        label = new JLabel("Data Cleaning Iterations");
        label.setToolTipText("<html>Cleaning iterates spline fitting and data cleaning up to a maximum number of " +
                "iterations.<br>The default is a maximum of N = 5 iterations.</html>");
        panel.add(label, c);
        c.gridx = GridBagConstraints.RELATIVE;
        controlBox = Box.createHorizontalBox();
        var control6 = new JSpinner(new SpinnerNumberModel(settings.dataCleaningTries.get().intValue(), 1, Integer.MAX_VALUE, 1));
        control6.addChangeListener($ -> {
            settings.dataCleaningTries.set(((Number) control6.getValue()).intValue());
            updatePlot();
        });
        modifySpinner(control6);
        control6.setToolTipText("<html>Cleaning iterates spline fitting and data cleaning up to a maximum number of " +
                "iterations.<br>The default is a maximum of N = 5 iterations.</html>");
        GenericSwingDialog.getTextFieldFromSpinner(control6).ifPresent(f -> f.setColumns(5));
        controlBox.add(control6);
        panel.add(controlBox, c);
        c.gridy++;

        c.gridx = 0;
        var button = new JButton("Apply current spline settings to all curves");
        button.addActionListener($ -> duplicateSettings(settings));
        c.gridwidth = 2;
        panel.add(button, c);

        c.gridx++;
        c.anchor = GridBagConstraints.CENTER;
        var button1 = new JButton("Ok");
        button1.addActionListener($ -> window.setVisible(false));
        panel.add(button1, c);

        //var border = new BevelBorder(BevelBorder.LOWERED, MultiPlot_.color[curve], MultiPlot_.color[curve]);
        panel.setBorder(BorderFactory.createEmptyBorder(10,10,10,10));

        window.setIconImage(createImageIcon("astroj/images/plot.png", "Plot Icon").getImage());
        window.add(panel);

        settings.windowOpened.set(true);
        window.pack();

        if (settings.windowLocation.hasSaved()) {
            window.setLocation(settings.windowLocation.get());
        } else {
            UIHelper.setCenteredOnScreen(window, mainFrame);
        }

        return window;
    }

    public void addFitListener(FitListener listener) {
        fitListeners.add(listener);
    }

    public void smoothData(double[] x, double[] y, double[] yerr, int size, RealVector mask) {
        smoothData(x, y, yerr, size, mask, true);
    }

    public void smoothData(double[] x, double[] y, double[] yerr, int size, RealVector mask, boolean notify) {
        if (notify) {
            fitListeners.forEach(l -> l.accept(FitState.NO_FIT));
        }
        Pair<RealVector, KeplerSpline.SplineMetadata> ks = null;
        if (settings.displayType.get() != KeplerSplineSettings.DisplayType.RAW_DATA) {
            if (notify) {
                fitListeners.forEach(l -> l.accept(FitState.FITTING));
            }
            ks = makeSplineGenerator().fit(x, y, size, mask);
        }

        if (notify) {
            if (settings.displayType.get() == KeplerSplineSettings.DisplayType.RAW_DATA || ks == null) {
                bkSpaceDisplay.setText("N/A");
                bicDisplay.setText("N/A");
                errorDisplay.setText("N/A");
                errorDisplay.setDisabledTextColor(Color.BLACK);
            } else if (settings.knotDensity.get() == KeplerSplineSettings.KnotDensity.LEGACY_SMOOTHER) {
                bkSpaceDisplay.setText("N/A");
                bicDisplay.setText("N/A");
                errorDisplay.setText("N/A");
                errorDisplay.setDisabledTextColor(Color.BLACK);
            } else {
                var lastSplineFit = ks.second();
                if (lastSplineFit.bic == null) {
                    bkSpaceDisplay.setText("N/A");
                    bicDisplay.setText("N/A");
                    errorDisplay.setText("Failed");
                    errorDisplay.setDisabledTextColor(Color.RED);
                    fitListeners.forEach(l -> l.accept(FitState.FAILED));
                    return;
                } else {
                    errorDisplay.setText("Success");
                    errorDisplay.setDisabledTextColor(darkGreen);
                    bkSpaceDisplay.setText(FORMATTER.format(lastSplineFit.bkSpace));
                    bicDisplay.setText(FORMATTER.format(lastSplineFit.bic));
                }
            }
        }

        switch (settings.displayType.get()) {
            case FITTED_SPLINE -> {
                if (ks != null) {
                    for (int xx = 0; xx < size; xx++) {
                        y[xx] = ks.first().getEntry(xx);
                    }
                }
            }
            case FLATTENED_LIGHT_CURVE -> {
                if (ks != null) {
                    //var avg = Arrays.stream(y).limit(size).summaryStatistics().getAverage();
                    for (int xx = 0; xx < size; xx++) {
                        y[xx] = (y[xx] / ks.first().getEntry(xx)); // * avg;
                        yerr[xx] = (yerr[xx] / ks.first().getEntry(xx));
                    }
                }
            }
            case RAW_DATA -> {
                // Return the raw data as-is
            }
        }

        if (notify) {
            fitListeners.forEach(l -> l.accept(FitState.SUCCESS));
        }
    }

    private KeplerSplineApplicator makeSplineGenerator() {
        return switch (settings.knotDensity.get()) {
            case FIXED -> (xs, ys, size, mask) ->
                    KeplerSpline.keplerSplineV2(MatrixUtils.createRealVector(Arrays.copyOf(xs,size)),
                    MatrixUtils.createRealVector(Arrays.copyOf(ys, size)), settings.fixedKnotDensity.get(),
                            mask, settings.minGapWidth.get(), settings.dataCleaningTries.get(),
                            settings.dataCleaningCoeff.get(), true);
            case AUTO -> (xs, ys, size, mask) ->
                    KeplerSpline.chooseKeplerSplineV2(MatrixUtils.createRealVector(Arrays.copyOf(xs,size)),
                    MatrixUtils.createRealVector(Arrays.copyOf(ys, size)), settings.minKnotDensity.get(),
                    settings.maxKnotDensity.get(), settings.knotDensitySteps.get(), mask,
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
        updatePlot(true);
    }

    public void updatePlot() {
        updatePlot(false);
    }

    public void updatePlot(boolean updateAll) {
        bkSpaceDisplay.setText("---");
        errorDisplay.setDisabledTextColor(Color.BLACK);
        errorDisplay.setText("Running...");
        if (updateAll) {
            RUNNER.submit(() -> MultiPlot_.updatePlot());
        } else {
            RUNNER.submit(() -> MultiPlot_.updatePlot(curve));
        }
    }

    @FunctionalInterface
    interface KeplerSplineApplicator {
        com.astroimagej.bspline.util.Pair<org.hipparchus.linear.RealVector, KeplerSpline.SplineMetadata> fit(double[] xs, double[] ys, int size, RealVector mask);
    }

    @FunctionalInterface
    public interface FitListener {
        void accept(FitState fitState);
    }

    public enum FitState {
        NO_FIT(Color.BLACK),
        FITTING(Color.CYAN),
        FAILED(Color.RED),
        SUCCESS(Color.GREEN);

        public final Color color;

        FitState(Color color) {
            this.color = color;
        }
    }
}
