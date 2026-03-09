package ij.astro.gui;

import java.awt.CardLayout;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Point;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;

import ij.IJ;
import ij.astro.io.prefs.Property;
import ij.astro.util.PixelPatcher;
import ij.astro.util.UIHelper;

public class PixelPatcherOptionsDialog extends JDialog {
    private JPanel rootPanel;
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
    private JComboBox<PixelPatcher.PatchType.NearestNeighbor.MergeType> nearestNeighborPixelSource;
    private JComboBox<PixelPatcher.PatchType.Type> patchTypeSelector;
    private JPanel fitGuassianCard;
    private JSpinner gaussianMinCount;
    private JSpinner gaussianMaxIter;
    private JSpinner gaussianRelErr;
    private JSpinner gaussianAbsErr;
    private JPanel fitMoffatCard;
    private JSpinner moffatMinCount;
    private JSpinner moffatMaxIter;
    private JSpinner moffatRelErr;
    private JSpinner moffatAbsErr;
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

        setModal(true);

        setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        WINDOW_LOCATION.locationSavingWindow(this);

        if (WINDOW_LOCATION.get().distanceSq(0, 0) == 0) {
            UIHelper.setCenteredOnScreen(this, IJ.getInstance());
        }

        patchTypeSelector.addItemListener(e -> {
            if (e.getItem() instanceof PixelPatcher.PatchType.Type type) {
                switch (type) {
                    case AVERAGE_FILL -> {
                        if (optionPanel.getLayout() instanceof CardLayout cardLayout) {
                            cardLayout.show(optionPanel, "averageFillCard");
                            PixelPatcher.TYPE.set(PixelPatcher.PatchType.Type.AVERAGE_FILL);
                        }
                    }
                    case MEDIAN_FILL -> {
                        if (optionPanel.getLayout() instanceof CardLayout cardLayout) {
                            cardLayout.show(optionPanel, "medianFillCard");
                            PixelPatcher.TYPE.set(PixelPatcher.PatchType.Type.MEDIAN_FILL);
                        }
                    }
                    case FLOOD_FILL -> {
                        if (optionPanel.getLayout() instanceof CardLayout cardLayout) {
                            cardLayout.show(optionPanel, "floodFillCard");
                            PixelPatcher.TYPE.set(PixelPatcher.PatchType.Type.FLOOD_FILL);
                        }
                    }
                    case FIT_PLANE -> {
                        if (optionPanel.getLayout() instanceof CardLayout cardLayout) {
                            cardLayout.show(optionPanel, "fitPlaneCard");
                            PixelPatcher.TYPE.set(PixelPatcher.PatchType.Type.FIT_PLANE);
                        }
                    }
                    case CONSTANT_VALUE -> {
                        if (optionPanel.getLayout() instanceof CardLayout cardLayout) {
                            cardLayout.show(optionPanel, "constantValueCard");
                            PixelPatcher.TYPE.set(PixelPatcher.PatchType.Type.CONSTANT_VALUE);
                        }
                    }
                    case NEAREST_NEIGHBOR -> {
                        if (optionPanel.getLayout() instanceof CardLayout cardLayout) {
                            cardLayout.show(optionPanel, "nearestNeighborCard");
                            PixelPatcher.TYPE.set(PixelPatcher.PatchType.Type.NEAREST_NEIGHBOR);
                        }
                    }
                    case PASS_THROUGH -> {
                        if (optionPanel.getLayout() instanceof CardLayout cardLayout) {
                            cardLayout.show(optionPanel, "passThroughCard");
                            PixelPatcher.TYPE.set(PixelPatcher.PatchType.Type.PASS_THROUGH);
                        }
                    }
                    case FIT_GAUSSIAN -> {
                        if (optionPanel.getLayout() instanceof CardLayout cardLayout) {
                            cardLayout.show(optionPanel, "fitGaussianCard");
                            PixelPatcher.TYPE.set(PixelPatcher.PatchType.Type.FIT_GAUSSIAN);
                        }
                    }
                    case FIT_MOFFAT -> {
                        if (optionPanel.getLayout() instanceof CardLayout cardLayout) {
                            cardLayout.show(optionPanel, "fitMoffatCard");
                            PixelPatcher.TYPE.set(PixelPatcher.PatchType.Type.FIT_MOFFAT);
                        }
                    }
                }
            }
        });
        switch (PixelPatcher.TYPE.get()) {
            case FIT_PLANE -> {
                if (optionPanel.getLayout() instanceof CardLayout cardLayout) {
                    cardLayout.show(optionPanel, "fitPlaneCard");
                }
            }
            case NEAREST_NEIGHBOR -> {
                if (optionPanel.getLayout() instanceof CardLayout cardLayout) {
                    cardLayout.show(optionPanel, "nearestNeighborCard");
                }
            }
            case FLOOD_FILL -> {
                if (optionPanel.getLayout() instanceof CardLayout cardLayout) {
                    cardLayout.show(optionPanel, "floodFillCard");
                }
            }
            case CONSTANT_VALUE -> {
                if (optionPanel.getLayout() instanceof CardLayout cardLayout) {
                    cardLayout.show(optionPanel, "constantValueCard");
                }
            }
            case MEDIAN_FILL -> {
                if (optionPanel.getLayout() instanceof CardLayout cardLayout) {
                    cardLayout.show(optionPanel, "medianFillCard");
                }
            }
            case AVERAGE_FILL -> {
                if (optionPanel.getLayout() instanceof CardLayout cardLayout) {
                    cardLayout.show(optionPanel, "averageFillCard");
                }
            }
            case PASS_THROUGH -> {
                if (optionPanel.getLayout() instanceof CardLayout cardLayout) {
                    cardLayout.show(optionPanel, "passThroughCard");
                }
            }
            case FIT_GAUSSIAN -> {
                if (optionPanel.getLayout() instanceof CardLayout cardLayout) {
                    cardLayout.show(optionPanel, "fitGaussianCard");
                }
            }
            case FIT_MOFFAT -> {
                if (optionPanel.getLayout() instanceof CardLayout cardLayout) {
                    cardLayout.show(optionPanel, "fitMoffatCard");
                }
            }
        }
        patchTypeSelector.setSelectedItem(PixelPatcher.TYPE.get());
        floodFillUseMedianCheckbox.addItemListener(PixelPatcher.PatchType.FloodFill.USE_MEDIAN.toItemListener());
        okButton.addActionListener(_ -> setVisible(false));
        PixelPatcher.PatchType.AverageFill.X_RADIUS.registerChangeListener(averageXRadiusSpinner);
        PixelPatcher.PatchType.AverageFill.Y_RADIUS.registerChangeListener(averageYRadiusSpinner);
        PixelPatcher.PatchType.MedianFill.X_RADIUS.registerChangeListener(medianXRadiusSpinner);
        PixelPatcher.PatchType.MedianFill.Y_RADIUS.registerChangeListener(medianYRadiusSpinner);
        PixelPatcher.PatchType.FitGaussian.MIN_COUNT.registerChangeListener(gaussianMinCount);
        PixelPatcher.PatchType.FitGaussian.MAX_ITER.registerChangeListener(gaussianMaxIter);
        PixelPatcher.PatchType.FitGaussian.ABS_ERR.registerChangeListener(gaussianAbsErr);
        PixelPatcher.PatchType.FitGaussian.REL_ERR.registerChangeListener(gaussianRelErr);
        PixelPatcher.PatchType.FitMoffat.MIN_COUNT.registerChangeListener(moffatMinCount);
        PixelPatcher.PatchType.FitMoffat.MAX_ITER.registerChangeListener(moffatMaxIter);
        PixelPatcher.PatchType.FitMoffat.ABS_ERR.registerChangeListener(moffatAbsErr);
        PixelPatcher.PatchType.FitMoffat.REL_ERR.registerChangeListener(moffatRelErr);
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
        radioPanel.add(patchTypeSelector);
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
        fitGuassianCard = new JPanel();
        fitGuassianCard.setLayout(new GridBagLayout());
        optionPanel.add(fitGuassianCard, "fitGaussianCard");
        final JPanel panel6 = new JPanel();
        panel6.setLayout(new FlowLayout(FlowLayout.CENTER, 5, 5));
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        fitGuassianCard.add(panel6, gbc);
        final JLabel label9 = new JLabel();
        label9.setText("Min. Count");
        label9.setToolTipText("Minium amount of good pixels in the region to be fit");
        panel6.add(label9);
        panel6.add(gaussianMinCount);
        final JPanel panel7 = new JPanel();
        panel7.setLayout(new FlowLayout(FlowLayout.CENTER, 5, 5));
        gbc = new GridBagConstraints();
        gbc.gridx = 1;
        gbc.gridy = 0;
        fitGuassianCard.add(panel7, gbc);
        final JLabel label10 = new JLabel();
        label10.setText("Max. Iter.");
        panel7.add(label10);
        panel7.add(gaussianMaxIter);
        final JPanel panel8 = new JPanel();
        panel8.setLayout(new FlowLayout(FlowLayout.CENTER, 5, 5));
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 1;
        fitGuassianCard.add(panel8, gbc);
        final JLabel label11 = new JLabel();
        label11.setText("Max. Rel. Err.");
        panel8.add(label11);
        panel8.add(gaussianRelErr);
        final JPanel panel9 = new JPanel();
        panel9.setLayout(new FlowLayout(FlowLayout.CENTER, 5, 5));
        gbc = new GridBagConstraints();
        gbc.gridx = 1;
        gbc.gridy = 1;
        fitGuassianCard.add(panel9, gbc);
        final JLabel label12 = new JLabel();
        label12.setText("Max. Abs. Err.");
        panel9.add(label12);
        panel9.add(gaussianAbsErr);
        fitMoffatCard = new JPanel();
        fitMoffatCard.setLayout(new GridBagLayout());
        optionPanel.add(fitMoffatCard, "fitMoffatCard");
        final JPanel panel10 = new JPanel();
        panel10.setLayout(new FlowLayout(FlowLayout.CENTER, 5, 5));
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.fill = GridBagConstraints.BOTH;
        fitMoffatCard.add(panel10, gbc);
        final JLabel label13 = new JLabel();
        label13.setText("Min. Count");
        label13.setToolTipText("Minium amount of good pixels in the region to be fit");
        panel10.add(label13);
        panel10.add(moffatMinCount);
        final JPanel panel11 = new JPanel();
        panel11.setLayout(new FlowLayout(FlowLayout.CENTER, 5, 5));
        gbc = new GridBagConstraints();
        gbc.gridx = 1;
        gbc.gridy = 0;
        gbc.fill = GridBagConstraints.BOTH;
        fitMoffatCard.add(panel11, gbc);
        final JLabel label14 = new JLabel();
        label14.setText("Max. Iter.");
        panel11.add(label14);
        panel11.add(moffatMaxIter);
        final JPanel panel12 = new JPanel();
        panel12.setLayout(new FlowLayout(FlowLayout.CENTER, 5, 5));
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.fill = GridBagConstraints.BOTH;
        fitMoffatCard.add(panel12, gbc);
        final JLabel label15 = new JLabel();
        label15.setText("Max. Rel. Err.");
        panel12.add(label15);
        panel12.add(moffatRelErr);
        final JPanel panel13 = new JPanel();
        panel13.setLayout(new FlowLayout(FlowLayout.CENTER, 5, 5));
        gbc = new GridBagConstraints();
        gbc.gridx = 1;
        gbc.gridy = 1;
        gbc.fill = GridBagConstraints.BOTH;
        fitMoffatCard.add(panel13, gbc);
        final JLabel label16 = new JLabel();
        label16.setText("Max. Abs. Err.");
        panel13.add(label16);
        panel13.add(moffatAbsErr);
        final JPanel panel14 = new JPanel();
        panel14.setLayout(new FlowLayout(FlowLayout.RIGHT, 5, 5));
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.fill = GridBagConstraints.BOTH;
        rootPanel.add(panel14, gbc);
        okButton = new JButton();
        okButton.setText("Ok");
        panel14.add(okButton);
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
        gaussianMinCount =
                new JSpinner(new SpinnerNumberModel(
                        PixelPatcher.PatchType.FitGaussian.MIN_COUNT.get().intValue(),
                        6, Integer.MAX_VALUE, 1));
        gaussianMaxIter =
                new JSpinner(new SpinnerNumberModel(
                        PixelPatcher.PatchType.FitGaussian.MAX_ITER.get().intValue(),
                        10, Integer.MAX_VALUE, 1));
        gaussianAbsErr =
                new JSpinner(new SpinnerNumberModel(
                        PixelPatcher.PatchType.FitGaussian.ABS_ERR.get().doubleValue(),
                        1e-200, 1.0, 1e-10));
        gaussianRelErr =
                new JSpinner(new SpinnerNumberModel(
                        PixelPatcher.PatchType.FitGaussian.REL_ERR.get().doubleValue(),
                        1e-200, 1.0, 1e-10));
        var ne = new JSpinner.NumberEditor(gaussianRelErr, "0.######E0");
        gaussianRelErr.setEditor(ne);
        ne = new JSpinner.NumberEditor(gaussianAbsErr, "0.######E0");
        gaussianAbsErr.setEditor(ne);

        moffatMinCount =
                new JSpinner(new SpinnerNumberModel(
                        PixelPatcher.PatchType.FitMoffat.MIN_COUNT.get().intValue(),
                        6, Integer.MAX_VALUE, 1));
        moffatMaxIter =
                new JSpinner(new SpinnerNumberModel(
                        PixelPatcher.PatchType.FitMoffat.MAX_ITER.get().intValue(),
                        10, Integer.MAX_VALUE, 1));
        moffatAbsErr =
                new JSpinner(new SpinnerNumberModel(
                        PixelPatcher.PatchType.FitMoffat.ABS_ERR.get().doubleValue(),
                        1e-200, 1.0, 1e-10));
        moffatRelErr =
                new JSpinner(new SpinnerNumberModel(
                        PixelPatcher.PatchType.FitMoffat.REL_ERR.get().doubleValue(),
                        1e-200, 1.0, 1e-10));
        ne = new JSpinner.NumberEditor(moffatRelErr, "0.######E0");
        moffatRelErr.setEditor(ne);
        ne = new JSpinner.NumberEditor(moffatAbsErr, "0.######E0");
        moffatAbsErr.setEditor(ne);

        nearestNeighborPixelSource = new JComboBox<>(PixelPatcher.PatchType.NearestNeighbor.MergeType.values());
        nearestNeighborPixelSource.setSelectedItem(PixelPatcher.PatchType.NearestNeighbor.MERGE_TYPE.get());
        nearestNeighborPixelSource.setRenderer(new ToolTipRenderer());

        patchTypeSelector = new JComboBox<>(PixelPatcher.PatchType.Type.values());
    }
}
