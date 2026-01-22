package ij.astro.gui;

import java.awt.CardLayout;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Point;
import java.awt.event.ItemEvent;

import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;

import ij.IJ;
import ij.astro.io.prefs.Property;
import ij.astro.util.PixelPatcher;
import ij.astro.util.UIHelper;

public class PixelPatcherOptionsDialog extends JDialog {
    private JPanel rootPanel;
    private JRadioButton averageFillRadioButton;
    private JRadioButton medianFillRadioButton;
    private JRadioButton floodFillRadioButton;
    private JRadioButton nearestNeighborRadioButton;
    private JRadioButton fitPlaneRadioButton;
    private JRadioButton constantValueRadioButton;
    private JPanel averageFillCard;
    private JPanel medianFillCard;
    private JPanel floodFillCard;
    private JPanel nearestNeighborCard;
    private JPanel fitPlaneCard;
    private JPanel constantValueCard;
    private JPanel optionPanel;
    private JPanel radioPanel;
    private JSpinner averageXRadiusSpinner;
    private JSpinner averageYRadiusSpinner;
    private JSpinner medianYRadiusSpinner;
    private JSpinner medianXRadiusSpinner;
    private JCheckBox floodFillUseMedianCheckbox;
    private JSpinner constantValueSpinner;
    private JButton okButton;
    private JPanel passThroughCard;
    private JRadioButton passThroughRadioButton;
    private JComboBox<PixelPatcher.PatchType.NearestNeighbor.MergeType> nearestNeighborPixelSource;
    private static final Property<Point> WINDOW_LOCATION = new Property<>(new Point(), PixelPatcherOptionsDialog.class);

    static void main() {
        showDialog();
    }

    public static void showDialog() {
        var dialog = new PixelPatcherOptionsDialog();

        dialog.add(dialog.rootPanel);
        dialog.pack();

        dialog.setVisible(true);
    }

    public PixelPatcherOptionsDialog() {
        $$$setupUI$$$();

        UIHelper.setLookAndFeel();

        setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        WINDOW_LOCATION.locationSavingWindow(this);

        if (WINDOW_LOCATION.get().distanceSq(0, 0) == 0) {
            UIHelper.setCenteredOnScreen(this, IJ.getInstance());
        }

        averageFillRadioButton.addItemListener(e -> {
            if (e.getStateChange() == ItemEvent.SELECTED) {
                if (optionPanel.getLayout() instanceof CardLayout cardLayout) {
                    cardLayout.show(optionPanel, "averageFillCard");
                    PixelPatcher.TYPE.set(PixelPatcher.PatchType.Type.AVERAGE_FILL);
                }
            }
        });
        medianFillRadioButton.addItemListener(e -> {
            if (e.getStateChange() == ItemEvent.SELECTED) {
                if (optionPanel.getLayout() instanceof CardLayout cardLayout) {
                    cardLayout.show(optionPanel, "medianFillCard");
                    PixelPatcher.TYPE.set(PixelPatcher.PatchType.Type.MEDIAN_FILL);
                }
            }
        });
        floodFillRadioButton.addItemListener(e -> {
            if (e.getStateChange() == ItemEvent.SELECTED) {
                if (optionPanel.getLayout() instanceof CardLayout cardLayout) {
                    cardLayout.show(optionPanel, "floodFillCard");
                    PixelPatcher.TYPE.set(PixelPatcher.PatchType.Type.FLOOD_FILL);
                }
            }
        });
        nearestNeighborRadioButton.addItemListener(e -> {
            if (e.getStateChange() == ItemEvent.SELECTED) {
                if (optionPanel.getLayout() instanceof CardLayout cardLayout) {
                    cardLayout.show(optionPanel, "nearestNeighborCard");
                    PixelPatcher.TYPE.set(PixelPatcher.PatchType.Type.NEAREST_NEIGHBOR);
                }
            }
        });
        fitPlaneRadioButton.addItemListener(e -> {
            if (e.getStateChange() == ItemEvent.SELECTED) {
                if (optionPanel.getLayout() instanceof CardLayout cardLayout) {
                    cardLayout.show(optionPanel, "fitPlaneCard");
                    PixelPatcher.TYPE.set(PixelPatcher.PatchType.Type.FIT_PLANE);
                }
            }
        });
        constantValueRadioButton.addItemListener(e -> {
            if (e.getStateChange() == ItemEvent.SELECTED) {
                if (optionPanel.getLayout() instanceof CardLayout cardLayout) {
                    cardLayout.show(optionPanel, "constantValueCard");
                    PixelPatcher.TYPE.set(PixelPatcher.PatchType.Type.CONSTANT_VALUE);
                }
            }
        });
        passThroughRadioButton.addItemListener(e -> {
            if (e.getStateChange() == ItemEvent.SELECTED) {
                if (optionPanel.getLayout() instanceof CardLayout cardLayout) {
                    cardLayout.show(optionPanel, "passThroughCard");
                    PixelPatcher.TYPE.set(PixelPatcher.PatchType.Type.PASS_THROUGH);
                }
            }
        });
        switch (PixelPatcher.TYPE.get()) {
            case FIT_PLANE -> {
                fitPlaneRadioButton.setSelected(true);
                if (optionPanel.getLayout() instanceof CardLayout cardLayout) {
                    cardLayout.show(optionPanel, "fitPlaneCard");
                }
            }
            case NEAREST_NEIGHBOR -> {
                nearestNeighborRadioButton.setSelected(true);
                if (optionPanel.getLayout() instanceof CardLayout cardLayout) {
                    cardLayout.show(optionPanel, "nearestNeighborCard");
                }
            }
            case FLOOD_FILL -> {
                floodFillRadioButton.setSelected(true);
                if (optionPanel.getLayout() instanceof CardLayout cardLayout) {
                    cardLayout.show(optionPanel, "floodFillCard");
                }
            }
            case CONSTANT_VALUE -> {
                constantValueRadioButton.setSelected(true);
                if (optionPanel.getLayout() instanceof CardLayout cardLayout) {
                    cardLayout.show(optionPanel, "constantValueCard");
                }
            }
            case MEDIAN_FILL -> {
                medianFillRadioButton.setSelected(true);
                if (optionPanel.getLayout() instanceof CardLayout cardLayout) {
                    cardLayout.show(optionPanel, "medianFillCard");
                }
            }
            case AVERAGE_FILL -> {
                averageFillRadioButton.setSelected(true);
                if (optionPanel.getLayout() instanceof CardLayout cardLayout) {
                    cardLayout.show(optionPanel, "averageFillCard");
                }
            }
            case PASS_THROUGH -> {
                passThroughRadioButton.setSelected(true);
                if (optionPanel.getLayout() instanceof CardLayout cardLayout) {
                    cardLayout.show(optionPanel, "passThroughCard");
                }
            }
        }
        floodFillUseMedianCheckbox.addItemListener(PixelPatcher.PatchType.FloodFill.USE_MEDIAN.toItemListener());
        okButton.addActionListener(_ -> setVisible(false));
        PixelPatcher.PatchType.AverageFill.X_RADIUS.registerChangeListener(averageXRadiusSpinner);
        PixelPatcher.PatchType.AverageFill.Y_RADIUS.registerChangeListener(averageYRadiusSpinner);
        PixelPatcher.PatchType.MedianFill.X_RADIUS.registerChangeListener(medianXRadiusSpinner);
        PixelPatcher.PatchType.MedianFill.Y_RADIUS.registerChangeListener(medianYRadiusSpinner);
        nearestNeighborPixelSource.addItemListener(PixelPatcher.PatchType.NearestNeighbor.MERGE_TYPE.toItemListener());
    }

    /**
     * Method generated by IntelliJ IDEA GUI Designer
     * >>> IMPORTANT!! <<<
     * DO NOT edit this method OR call it in your code!
     *
     * @noinspection ALL
     */
    private void $$$setupUI$$$() {
        createUIComponents();
        rootPanel = new JPanel();
        rootPanel.setLayout(new GridBagLayout());
        radioPanel = new JPanel();
        radioPanel.setLayout(new FlowLayout(FlowLayout.CENTER, 5, 5));
        GridBagConstraints gbc;
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.fill = GridBagConstraints.BOTH;
        rootPanel.add(radioPanel, gbc);
        averageFillRadioButton = new JRadioButton();
        averageFillRadioButton.setSelected(true);
        averageFillRadioButton.setText("Average Fill");
        radioPanel.add(averageFillRadioButton);
        medianFillRadioButton = new JRadioButton();
        medianFillRadioButton.setText("Median Fill");
        radioPanel.add(medianFillRadioButton);
        floodFillRadioButton = new JRadioButton();
        floodFillRadioButton.setText("Flood Fill");
        radioPanel.add(floodFillRadioButton);
        nearestNeighborRadioButton = new JRadioButton();
        nearestNeighborRadioButton.setText("Nearest Neighbor");
        radioPanel.add(nearestNeighborRadioButton);
        fitPlaneRadioButton = new JRadioButton();
        fitPlaneRadioButton.setText("Fit Plane");
        radioPanel.add(fitPlaneRadioButton);
        constantValueRadioButton = new JRadioButton();
        constantValueRadioButton.setText("Constant Value");
        radioPanel.add(constantValueRadioButton);
        passThroughRadioButton = new JRadioButton();
        passThroughRadioButton.setText("Pass Through");
        radioPanel.add(passThroughRadioButton);
        optionPanel = new JPanel();
        optionPanel.setLayout(new CardLayout(0, 0));
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.fill = GridBagConstraints.BOTH;
        rootPanel.add(optionPanel, gbc);
        medianFillCard = new JPanel();
        medianFillCard.setLayout(new FlowLayout(FlowLayout.CENTER, 5, 5));
        optionPanel.add(medianFillCard, "medianFillCard");
        final JPanel panel1 = new JPanel();
        panel1.setLayout(new FlowLayout(FlowLayout.CENTER, 5, 5));
        medianFillCard.add(panel1);
        final JLabel label1 = new JLabel();
        label1.setText("X Radius");
        panel1.add(label1);
        panel1.add(medianXRadiusSpinner);
        final JPanel panel2 = new JPanel();
        panel2.setLayout(new FlowLayout(FlowLayout.CENTER, 5, 5));
        medianFillCard.add(panel2);
        final JLabel label2 = new JLabel();
        label2.setText("Y Radius");
        panel2.add(label2);
        panel2.add(medianYRadiusSpinner);
        floodFillCard = new JPanel();
        floodFillCard.setLayout(new FlowLayout(FlowLayout.CENTER, 5, 5));
        optionPanel.add(floodFillCard, "floodFillCard");
        floodFillUseMedianCheckbox = new JCheckBox();
        floodFillUseMedianCheckbox.setText("Use Median");
        floodFillCard.add(floodFillUseMedianCheckbox);
        nearestNeighborCard = new JPanel();
        nearestNeighborCard.setLayout(new FlowLayout(FlowLayout.CENTER, 5, 5));
        optionPanel.add(nearestNeighborCard, "nearestNeighborCard");
        final JLabel label3 = new JLabel();
        label3.setText("Pixel Source");
        nearestNeighborCard.add(label3);
        nearestNeighborCard.add(nearestNeighborPixelSource);
        fitPlaneCard = new JPanel();
        fitPlaneCard.setLayout(new FlowLayout(FlowLayout.CENTER, 5, 5));
        optionPanel.add(fitPlaneCard, "fitPlaneCard");
        final JLabel label4 = new JLabel();
        label4.setText("No options can be specified for the patch mode.");
        fitPlaneCard.add(label4);
        constantValueCard = new JPanel();
        constantValueCard.setLayout(new FlowLayout(FlowLayout.CENTER, 5, 5));
        optionPanel.add(constantValueCard, "constantValueCard");
        final JPanel panel3 = new JPanel();
        panel3.setLayout(new FlowLayout(FlowLayout.CENTER, 5, 5));
        constantValueCard.add(panel3);
        final JLabel label5 = new JLabel();
        label5.setText("Fill Value");
        panel3.add(label5);
        panel3.add(constantValueSpinner);
        averageFillCard = new JPanel();
        averageFillCard.setLayout(new FlowLayout(FlowLayout.CENTER, 5, 5));
        optionPanel.add(averageFillCard, "averageFillCard");
        final JPanel panel4 = new JPanel();
        panel4.setLayout(new FlowLayout(FlowLayout.CENTER, 5, 5));
        averageFillCard.add(panel4);
        final JLabel label6 = new JLabel();
        label6.setText("X Radius");
        panel4.add(label6);
        panel4.add(averageXRadiusSpinner);
        final JPanel panel5 = new JPanel();
        panel5.setLayout(new FlowLayout(FlowLayout.CENTER, 5, 5));
        averageFillCard.add(panel5);
        final JLabel label7 = new JLabel();
        label7.setText("Y Radius");
        panel5.add(label7);
        panel5.add(averageYRadiusSpinner);
        passThroughCard = new JPanel();
        passThroughCard.setLayout(new FlowLayout(FlowLayout.CENTER, 5, 5));
        optionPanel.add(passThroughCard, "passThroughCard");
        final JLabel label8 = new JLabel();
        label8.setText("No options can be specified for the patch mode.");
        passThroughCard.add(label8);
        final JPanel panel6 = new JPanel();
        panel6.setLayout(new FlowLayout(FlowLayout.RIGHT, 5, 5));
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.fill = GridBagConstraints.BOTH;
        rootPanel.add(panel6, gbc);
        okButton = new JButton();
        okButton.setText("Ok");
        panel6.add(okButton);
        ButtonGroup buttonGroup;
        buttonGroup = new ButtonGroup();
        buttonGroup.add(averageFillRadioButton);
        buttonGroup.add(averageFillRadioButton);
        buttonGroup.add(medianFillRadioButton);
        buttonGroup.add(floodFillRadioButton);
        buttonGroup.add(nearestNeighborRadioButton);
        buttonGroup.add(fitPlaneRadioButton);
        buttonGroup.add(constantValueRadioButton);
        buttonGroup.add(passThroughRadioButton);
    }

    /**
     * @noinspection ALL
     */
    public JComponent $$$getRootComponent$$$() {
        return rootPanel;
    }

    private void createUIComponents() {
        averageXRadiusSpinner =
                new JSpinner(new SpinnerNumberModel(
                        PixelPatcher.PatchType.AverageFill.X_RADIUS.get().intValue(),
                        0, Integer.MAX_VALUE, 1));
        averageYRadiusSpinner =
                new JSpinner(new SpinnerNumberModel(
                        PixelPatcher.PatchType.AverageFill.Y_RADIUS.get().intValue(),
                        0, Integer.MAX_VALUE, 1));
        medianXRadiusSpinner =
                new JSpinner(new SpinnerNumberModel(
                        PixelPatcher.PatchType.MedianFill.X_RADIUS.get().intValue(),
                        0, Integer.MAX_VALUE, 1));
        medianYRadiusSpinner =
                new JSpinner(new SpinnerNumberModel(
                        PixelPatcher.PatchType.MedianFill.Y_RADIUS.get().intValue(),
                        0, Integer.MAX_VALUE, 1));
        constantValueSpinner = new JSpinner(new SpinnerNumberModel(Double.NaN, null, null, 1));

        nearestNeighborPixelSource = new JComboBox<>(PixelPatcher.PatchType.NearestNeighbor.MergeType.values());
        nearestNeighborPixelSource.setSelectedItem(PixelPatcher.PatchType.NearestNeighbor.MERGE_TYPE.get());
        nearestNeighborPixelSource.setRenderer(new ToolTipRenderer());
    }
}
