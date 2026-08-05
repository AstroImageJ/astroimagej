package Astronomy.multiplot;

import static Astronomy.MultiPlot_.checkForUT;
import static Astronomy.MultiPlot_.p11;
import static Astronomy.MultiPlot_.showMFMarkersCB;
import static Astronomy.MultiPlot_.updatePlot;
import static Astronomy.MultiPlot_.xStep;
import static Astronomy.MultiPlot_.xsteppopup;

import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.MouseAdapter;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.ButtonGroup;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;

import Astronomy.MultiPlot_;
import ij.astro.io.prefs.Property;

public class MeridianFlip {
    private double meridianFlip;
    private volatile boolean awaitingEdit;
    private final JLabel meridianFlipLabel = new JLabel("Meridian Flip");
    public static final Property<String> FLIP_COL = new Property<>("", "plot.", "", MeridianFlip.class);
    public static final Property<FlipType> FLIP_TYPE = new Property<>(FlipType.MANUAL, "plot.", "", MeridianFlip.class);

    public JComponent getDisplay() {
        var display = switch (FLIP_TYPE.get()) {
            case MANUAL -> "Manual " + meridianFlip;
            case COLUMN -> FLIP_COL.get() + " ➔ " + meridianFlip;
        };
        meridianFlipLabel.setText(display);
        meridianFlipLabel.setToolTipText(getDisplayTooltip());
        meridianFlipLabel.setPreferredSize(new Dimension(75, 25));
        meridianFlipLabel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                awaitingEdit = true;
                var d = new JDialog(SwingUtilities.getWindowAncestor(evt.getComponent()));
                d.setUndecorated(true);
                d.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
                d.getRootPane().setBorder(BorderFactory.createLineBorder(Color.DARK_GRAY));
                d.setFocusableWindowState(true);
                d.setContentPane(buildPopup());
                d.pack();
                d.setLocation(evt.getLocationOnScreen());

                d.addWindowFocusListener(new WindowAdapter() {
                    @Override
                    public void windowLostFocus(WindowEvent e) {
                        if (!d.isFocused()) {
                            d.dispose();

                            if (awaitingEdit) {
                                MultiPlot_.updatePlot();
                                updateDisplay();
                            }
                            awaitingEdit = false;
                        }
                    }
                });

                d.setVisible(true);
            }
        });
        return meridianFlipLabel;
    }

    public boolean isAwaitingEdit() {
        return awaitingEdit;
    }

    private String getDisplayTooltip() {
        var val = switch (FLIP_TYPE.get()) {
            case MANUAL -> "Manual " + meridianFlip;
            case COLUMN -> FLIP_COL.get() + " ➔ " + meridianFlip;
        };

        return """
                <html>
                Click to configure meridian flip.<br>
                %s
                </html>
                """.formatted(val);
    }

    private void updateDisplay() {
        var display = switch (FLIP_TYPE.get()) {
            case MANUAL -> "Manual " + meridianFlip;
            case COLUMN -> FLIP_COL.get() + " ➔ " + meridianFlip;
        };
        meridianFlipLabel.setText(display);
        meridianFlipLabel.setToolTipText(getDisplayTooltip());
    }

    public void setMeridianFlip(double meridianFlip) {
        this.meridianFlip = meridianFlip;
        //IO.println("Meridian Flip changed to " + meridianFlip);
        SwingUtilities.invokeLater(this::updateDisplay);
    }

    public double getMeridianFlip() {
        return meridianFlip;
    }

    public void setFlipType(FlipType flipType) {
        FLIP_TYPE.set(flipType);
    }

    public FlipType getFlipType() {
        return FLIP_TYPE.get();
    }

    private JComponent buildPopup() {
        awaitingEdit = true;
        var root = new JPanel();

        var flipGroup = new ButtonGroup();
        var typeSelection = Box.createHorizontalBox();
        typeSelection.add(new JLabel("Flip Type: "));
        var manualRadioButton = new JRadioButton("Manual");
        flipGroup.add(manualRadioButton);
        manualRadioButton.setSelected(FLIP_TYPE.get() == FlipType.MANUAL);
        manualRadioButton.addActionListener(_ -> FLIP_TYPE.set(FlipType.MANUAL));
        typeSelection.add(manualRadioButton);
        var columnRadioButton = new JRadioButton("Column");
        columnRadioButton.setSelected(FLIP_TYPE.get() == FlipType.COLUMN);
        columnRadioButton.addActionListener(_ -> FLIP_TYPE.set(FlipType.COLUMN));
        flipGroup.add(columnRadioButton);
        typeSelection.add(columnRadioButton);
        root.add(typeSelection);

        var input = new JPanel(new CardLayout());

        var manualPanel = new JPanel();
        var flipSpinner = getFlipSpinner();
        manualPanel.add(flipSpinner);
        input.add(manualPanel, FlipType.MANUAL.name());

        var columnPanel = new JPanel();
        columnPanel.add(new JLabel("Column: "));
        var colSelector = new JComboBox<>(MultiPlot_.columns);
        colSelector.setSelectedItem(FLIP_COL.get());
        colSelector.addActionListener(_ -> FLIP_COL.set((String) colSelector.getSelectedItem()));
        columnPanel.add(colSelector);
        input.add(columnPanel, FlipType.COLUMN.name());

        root.add(input);

        FLIP_COL.clearListeners();
        FLIP_TYPE.clearListeners();
        FLIP_TYPE.addListener(this, (_, v) -> {
            switch (v) {
                case MANUAL -> {
                    manualRadioButton.setSelected(true);
                    if (input.getLayout() instanceof CardLayout cardLayout) {
                        cardLayout.show(input, FlipType.MANUAL.name());
                    }
                }
                case COLUMN -> {
                    columnRadioButton.setSelected(true);
                    if (input.getLayout() instanceof CardLayout cardLayout) {
                        cardLayout.show(input, FlipType.COLUMN.name());
                    }
                }
            }
            /*if (!awaitingEdit) {
                MultiPlot_.updatePlot();
                updateDisplay();
            }*/
        });

        if (input.getLayout() instanceof CardLayout cardLayout) {
            cardLayout.show(input, FLIP_TYPE.get().name());
        }

        FLIP_COL.addListener(this, (_, v) -> {
            if (FLIP_TYPE.get() != FlipType.COLUMN) {
                return;
            }

            /*if (!awaitingEdit) {
                MultiPlot_.updatePlot();
            }*/
        });

        return root;
    }

    private JSpinner getFlipSpinner() {
        var mfmarker1spinnermodel = new SpinnerNumberModel(meridianFlip, null, null, xStep);
        var mfmarker1spinner = new JSpinner(mfmarker1spinnermodel);
        mfmarker1spinner.setFont(p11);
        mfmarker1spinner.setEditor(new JSpinner.NumberEditor(mfmarker1spinner, "########0.######"));
        mfmarker1spinner.setPreferredSize(new Dimension(75, 25));
        mfmarker1spinner.setEnabled(true);
        mfmarker1spinner.setComponentPopupMenu(xsteppopup);
        mfmarker1spinner.setToolTipText("""
                <html>
                Enter meridian flip time in x-axis units<br>or enter UT time in HH:MM or HH:MM:SS format and press 'Enter'<br>
                ---------------------------------------------<br>
                Right click to set spinner stepsize
                </html>
                """);
        mfmarker1spinner.addChangeListener(_ -> {
            showMFMarkersCB.setSelected(true);
            checkForUT(mfmarker1spinner);
            meridianFlip = (Double) mfmarker1spinner.getValue();
            updateDisplay();
            updatePlot();
        });
        mfmarker1spinner.addMouseWheelListener(e -> mfmarker1spinner.setValue((Double) mfmarker1spinner.getValue() - e.getWheelRotation() * xStep));
        return mfmarker1spinner;
    }

    public enum FlipType {
        MANUAL,
        COLUMN,
    }
}
