package ij.astro.gui;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Frame;
import java.awt.GraphicsEnvironment;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.Insets;
import java.awt.Label;
import java.awt.Panel;
import java.awt.Point;
import java.awt.Scrollbar;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.AdjustmentEvent;
import java.awt.event.AdjustmentListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.InputEvent;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.TextEvent;
import java.awt.event.TextListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.awt.image.ImageProducer;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URL;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import javax.swing.AbstractButton;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFormattedTextField;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;
import javax.swing.text.DefaultFormatterFactory;
import javax.swing.text.NumberFormatter;

import ij.IJ;
import ij.ImageJ;
import ij.Macro;
import ij.Prefs;
import ij.WindowManager;
import ij.astro.gui.nstate.NState;
import ij.astro.gui.nstate.NStateButton;
import ij.astro.io.prefs.Property;
import ij.astro.util.UIHelper;
import ij.gui.GUI;
import ij.gui.GenericDialog;
import ij.gui.HTMLDialog;
import ij.gui.MultiLineLabel;
import ij.macro.Interpreter;
import ij.macro.MacroRunner;
import ij.plugin.ScreenGrabber;
import ij.plugin.frame.Recorder;

/**
 * A Swing implementation of {@link GenericDialog}.
 */
//todo missing objects
public class GenericSwingDialog extends JDialog implements ActionListener, TextListener, FocusListener, ItemListener,
        KeyListener, AdjustmentListener, WindowListener {
    private final GridBagConstraints c = new GridBagConstraints();
    private final JScrollPane scrollPane = new JScrollPane();
    private final JButton okay = new JButton("  OK  ");
    private final JButton cancel = new JButton("Cancel");
    private final JPanel basePanel = new JPanel(new GridBagLayout());
    private final String macroOptions;
    private final boolean macro;
    private AtomicInteger x = new AtomicInteger();
    private JButton no, help;
    private boolean wasOKed = false, wasCanceled = false;
    private boolean hideCancelButton = false;
    private boolean centerDialog = false;
    private boolean fontSizeSet = false;
    private String helpLabel = "Help";
    private String helpURL;
    private boolean addToSameRow;
    private boolean addToSameRowCalled;
    private boolean saveAndUseStepSize;
    private boolean overridePosition = false;
    private int anchor = 0;
    private boolean customAnchor = false;
    private int leftInset = 0;
    private int rightInset = 0;
    private LinkedHashMap<Object, Triple<String, Consumer<String>, Runnable>> labels;
    private boolean optionsRecorded;     // have dialogListeners been called to record options?
    private boolean recorderOn;          // whether recording is allowed (after the dialog is closed)
    private Class<?> activeSection;
    private final Map<Class<?>, SwappableSectionHolder<?>> swappableSections = new HashMap<>();
    private final Deque<StackComponent> rowBuilding = new ArrayDeque<>();
    private int gridwith;
    private int fill;

    public GenericSwingDialog(String title) {
        this(title, guessParentFrame());
    }

    public GenericSwingDialog(String title, int x, int y) {
        this(title, guessParentFrame());
        if (Prefs.isLocationOnScreen(new Point(x, y))) setLocation(x, y);
    }

    public GenericSwingDialog(String title, Frame owner) {
        super(owner, title, true);
        setupPaneLayout();
        UIHelper.setLookAndFeel();
        setIcon();
        macroOptions = Macro.getOptions();
        macro = macroOptions!=null;
        recorderOn = Recorder.record;
    }

    /**
     * Author: Michael Kaul, taken from {@link GenericDialog}
     */
    private static int digits(double d) {
        if (d == (int) d) return 0;
        String s = Double.toString(d);
        int ePos = s.indexOf("E");
        if (ePos == -1)
            ePos = s.indexOf("e");
        int dotPos = s.indexOf(".");
        int digits = 0;
        if (ePos == -1)
            digits = s.substring(dotPos + 1).length();
        else {
            String number = s.substring(dotPos + 1, ePos);
            if (!number.equals("0")) digits += number.length();
            digits = digits - Integer.parseInt(s.substring(ePos + 1));
        }
        return digits;
    }

    private static Frame guessParentFrame() {
        return WindowManager.getCurrentImage() != null ?
                WindowManager.getCurrentImage().getWindow() : null;
    }

    /**
     * @see Math#nextDown(double)
     */
    // Custom version to remove special case at 0
    public static double nextDown(double d) {
        if (Double.isNaN(d) || d == Double.NEGATIVE_INFINITY)
            return d;
        else {
            return Double.longBitsToDouble(Double.doubleToRawLongBits(d) +
                    ((d >= 0.0d) ? -1L : +1L));
        }
    }

    /**
     * @see Math#nextUp(double)
     */
    // Custom version to remove special case at 0
    public static double nextUp(double d) {
        // Use a single conditional and handle the likely cases first.
        if (d < Double.POSITIVE_INFINITY) {
            // Add +0.0 to get rid of a -0.0 (+0.0 + -0.0 => +0.0).
            final long transducer = Double.doubleToRawLongBits(d + 0.0D);
            return Double.longBitsToDouble(transducer + ((transducer >= 0L) ? 1L : -1L));
        } else { // d is NaN or +Infinity
            return d;
        }
    }

    public static int getSliderWidth(JPanel sliderPanel) {
        return getTextFieldFromSlider(sliderPanel).map(JTextField::getColumns).orElse(0);
    }

    public static void setSliderSpinnerColumns(JPanel sliderPanel, int columns) {
        if (sliderPanel == null) return;
        getTextFieldFromSlider(sliderPanel).ifPresent(jFormattedTextField -> jFormattedTextField.setColumns(columns));
    }

    public static Optional<JFormattedTextField> getTextFieldFromSlider(JPanel sliderPanel) {
        for (Component component : sliderPanel.getComponents()) {
            if (component instanceof JSpinner spinner) {
                for (Component spinnerComponent : spinner.getComponents()) {
                    if (spinnerComponent instanceof JSpinner.DefaultEditor editor) {
                        return Optional.of(editor.getTextField());
                    }
                }
            }
        }
        return Optional.empty();
    }

    public static Optional<JSpinner> getSpinnerFromSlider(JPanel sliderPanel) {
        for (Component component : sliderPanel.getComponents()) {
            if (component instanceof JSpinner spinner) {
                return Optional.of(spinner);
            }
        }
        return Optional.empty();
    }

    public static Optional<JTextField> getTextFieldFromSpinner(JSpinner spinner) {
        for (Component component : spinner.getComponents()) {
            if (component instanceof JSpinner.DefaultEditor editor) {
                return Optional.of(editor.getTextField());
            }
        }
        return Optional.empty();
    }

    public void setHideCancelButton(boolean hideCancelButton) {
        this.hideCancelButton = hideCancelButton;
    }

    public Box buildRow(BiConsumer<GenericSwingDialog, Box> rowBuilder) {
        var box = Box.createHorizontalBox();

        rowBuilding.push(new StackComponent(box));
        rowBuilder.accept(this, box);
        rowBuilding.pop();

        var c = getConstraints();
        c.gridx = 0;
        c.gridy++;
        c.anchor = GridBagConstraints.WEST;
        c.gridwidth = GridBagConstraints.REMAINDER;
        //c.fill = GridBagConstraints.HORIZONTAL;
        c.insets.left = 20;

        addLocal(box, c);

        c.fill = GridBagConstraints.NONE;
        c.gridwidth = 1;
        c.anchor = GridBagConstraints.CENTER;

        return box;
    }

    public JPanel buildColumn(BiConsumer<GenericSwingDialog, JPanel> columnBuilder) {
        var box = new JPanel(new GridBagLayout());

        rowBuilding.push(new StackComponent(box, new GridBagConstraints()));
        columnBuilder.accept(this, box);
        rowBuilding.pop();

        var c = getConstraints();
        c.gridx = 0;
        c.gridy++;
        //c.anchor = GridBagConstraints.WEST;
        c.anchor = GridBagConstraints.CENTER;
        //c.gridwidth = 2;
        c.weightx = 1;
        //c.gridwidth = GridBagConstraints.REMAINDER;
        //c.fill = GridBagConstraints.HORIZONTAL;
        c.insets.left = 20;

        useCustomPosition();
        addLocal(box, c);

        c.fill = GridBagConstraints.NONE;
        c.gridwidth = 1;
        c.anchor = GridBagConstraints.CENTER;
        c.weightx = 0;

        return box;
    }

    /**
     * Creates a swappable section for the dialog and adds a dropdown to control it.
     */
    public <T extends Enum<T> & NState<T>> JComponent addSwappableSection(String optionText, Property<T> property,
                                                                           BiConsumer<GenericSwingDialog, T> sectionBuilder) {
        return addSwappableSection(optionText, property.get(), property::set, sectionBuilder);
    }

    /**
     * Creates a swappable section for the dialog and adds a dropdown to control it.
     *
     * @param optionText
     * @param currentState   the default card to show.
     * @param sectionBuilder a consumer used to populate the components of the swappable panel. Use a switch for ease of use.
     */
    public <T extends Enum<T> & NState<T>> JComponent addSwappableSection(String optionText, T currentState, Consumer<T> consumer,
                                                                    BiConsumer<GenericSwingDialog, T> sectionBuilder) {
        Class<T> stateClass = currentState.getDeclaringClass();
        if (swappableSections.containsKey(currentState.getDeclaringClass())) {
            throw new IllegalArgumentException("GSD already contains section for type: " + stateClass);
        }

        var swappableSection = new SwappableSectionHolder<>(new SwappableSection<T>(currentState));

        final JComponent[] selection = new JComponent[1];
        if (optionText != null) {
            buildRow((g, box) -> {
                addMessage(optionText);
                //box.add(Box.createHorizontalGlue());
                selection[0] = addNStateDropdown(currentState, consumer.andThen(swappableSection::setCurrentState).andThen(_ -> {
                    pack();
                    revalidate();
                    repaint();
                }));
            });
        } else {
            //todo which one, pack will resize window, revalidate won't which can cause scrollbars to appear
            selection[0] = addNStateDropdown(currentState, consumer.andThen(swappableSection::setCurrentState).andThen(_ -> {
                pack();
                revalidate();
                repaint();
            }));
        }

        var c = getConstraints();
        c.gridx = 0;
        c.gridy++;
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets.left = 0;
        c.anchor = GridBagConstraints.WEST;
        c.weightx = 1;

        //swappableSection.getSectionPanel().setBorder(BorderFactory.createLineBorder(Color.RED));

        addLocal(swappableSection.getSectionPanel(), c);

        c.fill = GridBagConstraints.NONE;
        c.gridwidth = 1;
        c.weightx = 0;

        swappableSections.put(stateClass, swappableSection);

        activeSection = stateClass;
        for (T enumConstant : stateClass.getEnumConstants()) {
            resetPositionOverride();
            swappableSection.setCurrentState(enumConstant);
            //the design is to use switch expressions in the builder
            sectionBuilder.accept(this, enumConstant);
        }
        activeSection = null;

        return selection[0];
    }

    public <T extends Enum<T> & NState<T>> void addNewSwappableSectionPanel(Class<T> sectionType,
                                                                            BiConsumer<GenericSwingDialog, T> sectionBuilder) {
        if (!swappableSections.containsKey(Objects.requireNonNull(sectionType))) {
            throw new IllegalArgumentException("Cannot add new panel to nonexistent section: " + sectionType);
        }

        SwappableSectionHolder<T> swappableSection = (SwappableSectionHolder<T>) swappableSections.get(sectionType);

        var c = getConstraints();
        c.gridx = 0;
        c.gridy++;
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets.left = 0;
        c.anchor = GridBagConstraints.WEST;

        addLocal(swappableSection.addNewPanel().getSectionPanel(), c);

        c.fill = GridBagConstraints.NONE;
        c.gridwidth = 1;

        activeSection = sectionType;
        for (T enumConstant : sectionType.getEnumConstants()) {
            swappableSection.setCurrentState(enumConstant);
            sectionBuilder.accept(this, enumConstant);
        }
        activeSection = null;
    }

    /**
     * @param activeSection the section subsequent components will be added to
     */
    public void setActiveSection(Class<?> activeSection) {
        if (activeSection == null || swappableSections.containsKey(activeSection)) {
            this.activeSection = activeSection;
        } else {
            throw new IllegalArgumentException("GSD does not contain section for type: " + activeSection);
        }
    }

    private JComponent getPanel() {
        if (!rowBuilding.isEmpty()) {
            return rowBuilding.peekLast().component();
        }

        if (activeSection != null) {
            return swappableSections.get(activeSection).currentPanel();
        }

        return basePanel;
    }

    private GridBagConstraints getConstraints() {
        if (!rowBuilding.isEmpty()) {
            var cc = rowBuilding.peekLast().constraints();
            if (cc != null) {
                return rowBuilding.peekLast().constraints();
            }
        }

        if (activeSection != null) {
            return swappableSections.get(activeSection).currentConstrains();
        }

        return c;
    }

    private AtomicInteger getXPos() {
        if (activeSection != null) {
            return swappableSections.get(activeSection).currentXPos();
        }

        return x;
    }

    public JCheckBox addCheckbox(String label, Property<Boolean> property) {
        return addCheckbox(label, property.get(), property::set);
    }

    public JCheckBox addCheckbox(String label, boolean initValue, Consumer<Boolean> consumer) {
        var c = getConstraints();
        var x = getXPos();
        var b = Box.createHorizontalBox();
        final var box = new JCheckBox(label.replaceAll("_", " "));
        box.setSelected(initValue);
        box.addActionListener(_ -> consumer.accept(box.isSelected()));
        if (addToSameRow) {
            c.gridx = GridBagConstraints.RELATIVE;
            c.insets.left = 10;
            addToSameRow = false;
        } else {
            c.gridx = 0;
            c.gridy++;
            c.insets = new Insets(DialogBoxType.CHECKBOX.isPresent() ? 0 : 15, 20, 0, 0);
        }
        c.anchor = GridBagConstraints.WEST;
        c.gridwidth = 1;
        b.add(box);
        useCustomPosition();
        if (overridePosition) c.gridx = x.get();
        addLocal(b, c);
        x.getAndIncrement();
        c.insets.left = 0;

        if (Recorder.record || macro)
            saveLabel(box, label, s -> {
                box.setSelected(Boolean.parseBoolean(s));
                consumer.accept(Boolean.parseBoolean(s));
            }, () -> {
                if (recorderOn) {
                    recordOption(box, Boolean.toString(box.isSelected()));
                }
            });

        return box;
    }

    public ComponentPair addCheckboxGroup(int rows, int columns, String[] labels, boolean[] defaultValues, List<Consumer<Boolean>> consumers) {
        return addCheckboxGroup(rows, columns, labels, defaultValues, null, consumers);
    }

    public void addSingleSpaceLineSeparator() {
        addMessage("");
        addToSameRow();
        addLineSeparator();
    }

    public void addDoubleSpaceLineSeparator() {
        addMessage("");
        addLineSeparator();
        addMessage("");
    }

    public void addLineSeparator() {
        var sep = new JSeparator();
        sep.setPreferredSize(new Dimension(5,3));
        var c = new GridBagConstraints();
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 1;
        if (!addToSameRow) {
            getConstraints().gridy++;
        } else {
            addToSameRow = false;
        }
        c.gridy = getConstraints().gridy;
        c.gridx = 0;
        c.gridwidth = GridBagConstraints.REMAINDER;
        addLocal(sep, c);
    }

    public ComponentPair addCheckboxGroup(int rows, int columns, String[] labels, boolean[] defaultValues, String[] headings, List<Consumer<Boolean>> consumers) {
        var c = getConstraints();
        var x = getXPos();
        var panel = new JPanel();
        int nRows = headings != null ? rows + 1 : rows;
        panel.setLayout(new GridLayout(nRows, columns, 6, 0));
        if (headings != null) {
            Font font = new Font("SansSerif", Font.BOLD, 12);
            for (int i = 0; i < columns; i++) {
                if (i > headings.length - 1 || headings[i] == null)
                    panel.add(new Label(""));
                else {
                    Label label = new Label(headings[i]);
                    label.setFont(font);
                    panel.add(label);
                }
            }
        }
        int i1 = 0;
        var boxes = new LinkedList<JComponent>();
        //int[] index = new int[labels.length];
        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < columns; col++) {
                int i2 = col * rows + row;
                if (i2 >= labels.length) break;
                //index[i1] = i2;
                String label = labels[i1];
                if (label == null || label.length() == 0) {
                    Label lbl = new Label("");
                    panel.add(lbl);
                    i1++;
                    continue;
                }
                if (label.indexOf('_') != -1)
                    label = label.replace('_', ' ');
                var cb = new JCheckBox(label);
                cb.setSelected(defaultValues[i1]);
                final int finalI = i1;
                cb.addActionListener(_ -> {
                    consumers.get(finalI).accept(cb.isSelected());
                });
                if (Recorder.record || macro) {
                    saveLabel(cb, labels[i1], s -> {
                        cb.setSelected(Boolean.parseBoolean(s));
                        consumers.get(finalI).accept(Boolean.parseBoolean(s));
                    }, () -> {
                        if (recorderOn) {
                            recordOption(cb, Boolean.toString(cb.isSelected()));
                        }
                    });
                }
                boxes.add(cb);
                if (IJ.isLinux()) {
                    Panel panel2 = new Panel();
                    panel2.setLayout(new BorderLayout());
                    panel2.add("West", cb);
                    panel.add(panel2);
                } else
                    panel.add(cb);
                i1++;
            }
        }
        c.gridx = 0;
        c.gridy++;
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.anchor = GridBagConstraints.WEST;
        c.insets = new Insets(0, 20, 0, 0);
        //addToSameRow = false;
        if (overridePosition) c.gridx = x.get();
        //c.weightx = 1;
        addLocal(panel, c);
        x.getAndIncrement();

        return new ComponentPair(panel, null, boxes);
    }

    public ComponentPair addChoice(String label, String[] items, String defaultItem, Consumer<String> consumer) {
        var c = getConstraints();
        var x = getXPos();
        Box b = Box.createHorizontalBox();
        Label fieldLabel = makeLabel(label.replaceAll("_", " "));
        if (addToSameRow) {
            c.gridx = GridBagConstraints.RELATIVE;
            addToSameRow = false;
        } else {
            c.gridx = 0;
            c.gridy++;
            c.insets = DialogBoxType.CHOICE.isPresent() ? new Insets(5, 0, 5, 0) : new Insets(0, 0, 5, 0);
        }
        c.anchor = GridBagConstraints.EAST;
        c.gridwidth = 1;

        b.add(fieldLabel);
        //addLocal(fieldLabel, c);
        var thisChoice = new JComboBox<String>();
        thisChoice.addKeyListener(this);
        thisChoice.addItemListener(_ -> consumer.accept((String) thisChoice.getSelectedItem()));
        for (String item : items) thisChoice.addItem(item);
        if (defaultItem != null) {
            thisChoice.setSelectedItem(defaultItem);
        } else {
            thisChoice.setSelectedIndex(0);
        }
        c.gridx = GridBagConstraints.RELATIVE;
        c.anchor = GridBagConstraints.WEST;
        useCustomPosition();
        if (overridePosition) c.gridx = x.get();
        b.add(thisChoice);
        addLocal(b, c);
        x.getAndIncrement();

        if (Recorder.record || macro)
            saveLabel(thisChoice, label, s -> {
                thisChoice.setSelectedItem(s);
                consumer.accept(s);
            }, () -> {
                if (recorderOn) {
                    recordOption(thisChoice, ((String) thisChoice.getSelectedItem()));//todo use index?
                }
            });

        return new ComponentPair(b, thisChoice);
    }

    public Component addMessage(String text) {
        return addMessage(text, null, null);
    }

    public Component addMessage(String text, Font font) {
        return addMessage(text, font, null);
    }

    public Component addMessage(String text, Font font, Color color) {
        var c = getConstraints();
        var x = getXPos();
        Component theLabel = text.indexOf('\n') >= 0 ? new MultiLineLabel(text) : new Label(text);

        if (addToSameRow) {
            c.gridx = GridBagConstraints.RELATIVE;
            addToSameRow = false;
        } else {
            c.gridx = 0;
            c.gridy++;
            c.insets = new Insets("".equals(text) ? 0 : 10, 20, 0, 0);
        }

        c.gridwidth = GridBagConstraints.REMAINDER;
        c.anchor = GridBagConstraints.WEST;
        c.fill = GridBagConstraints.HORIZONTAL;
        if (font != null) {
            if (Prefs.getGuiScale() > 1.0)
                font = font.deriveFont((float) (font.getSize() * Prefs.getGuiScale()));
            theLabel.setFont(font);
        }
        if (color != null) theLabel.setForeground(color);
        if (overridePosition) c.gridx = x.get();
        useCustomPosition();
        if (!text.equals("")) addLocal(theLabel, c);
        x.getAndIncrement();
        c.fill = GridBagConstraints.NONE;

        return theLabel;
    }

    /**
     * Adds a slider (scroll bar) to the dialog box.
     * Floating point values are used if (maxValue-minValue)<=5.0
     * and either defaultValue or minValue are non-integer.
     *
     * @param label        the label
     * @param minValue     the minimum value of the slider
     * @param maxValue     the maximum value of the slider
     * @param defaultValue the initial value of the slider
     */
    public JPanel addSlider(String label, double minValue, double maxValue, double defaultValue, Consumer<Double> consumer) {
        return addSlider(label, minValue, maxValue, true, defaultValue, consumer);
    }

    /**
     * Adds a slider (scroll bar) to the dialog box.
     * Floating point values are used if (maxValue-minValue)<=5.0
     * and either defaultValue or minValue are non-integer.
     *
     * @param label        the label
     * @param minValue     the minimum value of the slider
     * @param maxValue     the maximum value of the slider
     * @param defaultValue the initial value of the slider
     */
    public JPanel addSlider(String label, double minValue, double maxValue, boolean clipMaxValue, double defaultValue, Consumer<Double> consumer) {
        if (defaultValue < minValue) defaultValue = minValue;
        if (defaultValue > maxValue) defaultValue = maxValue;
        int digits = 0;
        double scale = 1.0;
        if ((maxValue - minValue) <= 5.0 && (minValue != (int) minValue || maxValue != (int) maxValue || defaultValue != (int) defaultValue)) {
            scale = 50.0;
            minValue *= scale;
            maxValue *= scale;
            defaultValue *= scale;
            digits = 2;
        }

        return addSlider(label, minValue, maxValue, clipMaxValue, defaultValue, scale, digits, consumer);
    }

    public JPanel addSlider(String label, double minValue, double maxValue, double defaultValue, double stepSize, Consumer<Double> consumer) {
        return addSlider(label, minValue, maxValue, true, defaultValue, stepSize, consumer);
    }

    public JPanel addSlider(String label, double minValue, double maxValue, boolean clipMaxValue, double defaultValue, double stepSize, Consumer<Double> consumer) {
        if (stepSize <= 0) stepSize = 1;
        int digits = digits(stepSize);
        if (digits == 1 && "Angle:".equals(label)) digits = 2;
        double scale = 1.0 / Math.abs(stepSize);
        if (scale <= 0) scale = 1;
        if (defaultValue < minValue) defaultValue = minValue;
        if (defaultValue > maxValue) defaultValue = maxValue;
        minValue *= scale;
        maxValue *= scale;
        defaultValue *= scale;

        return addSlider(label, minValue, maxValue, clipMaxValue, defaultValue, scale, digits, consumer);
    }

    public JPanel addFloatSlider(String label, double minValue, double maxValue, double defaultValue, int digits, double stepSize, Consumer<Double> consumer) {
        return addFloatSlider(label, minValue, maxValue, true, defaultValue, digits, stepSize, consumer);
    }

    public JPanel addFloatSlider(String label, double minValue, double maxValue, boolean clipMaxValue, double defaultValue, int digits, double stepSize, Consumer<Double> consumer) {
        if (stepSize <= 0) stepSize = 1;
        double scale = 1.0 / Math.abs(stepSize);
        minValue *= scale;
        maxValue *= scale;
        defaultValue *= scale;

        return addSlider(label, minValue, maxValue, clipMaxValue, defaultValue, scale, digits, consumer);
    }

    private JPanel addSlider(String label, final double minValue, final double maxValue, boolean clipMaxValue, double defaultValue, final double scale, final int digits, Consumer<Double> consumer) {
        var c = getConstraints();
        var x = getXPos();
        Box b = Box.createHorizontalBox();
        int columns = 4 + digits + (IJ.isMacOSX() ? 0 : -2);
        if (columns < 4) columns = 4;
        if (minValue < 0.0) columns++;
        String mv = IJ.d2s(maxValue, 0);
        if (mv.length() > 4 && digits == 0) columns += mv.length() - 4;
        Label fieldLabel = makeLabel(label.replaceAll("_", " "));
        if (addToSameRow) {
            c.gridx = GridBagConstraints.RELATIVE;
            c.insets.bottom += 3;
            addToSameRow = false;
        } else {
            c.gridx = 0;
            c.gridy++;
            c.insets = new Insets(0, 0, 3, 0);
        }

        c.anchor = GridBagConstraints.EAST;
        c.gridwidth = 1;
        b.add(fieldLabel);
        b.add(Box.createHorizontalGlue());

        Scrollbar s = new Scrollbar(Scrollbar.HORIZONTAL, (int) defaultValue, 1, (int) minValue, (int) maxValue + 1);
        GUI.fixScrollbar(s);
        s.addAdjustmentListener(this);
        s.setUnitIncrement(1);
        if (IJ.isMacOSX()) s.addKeyListener(this);
        if (IJ.isWindows()) columns -= 2;
        if (columns < 1) columns = 1;

        var id = stepSizeId(label);

        if (defaultValue < minValue) defaultValue = minValue;
        if (defaultValue > maxValue) defaultValue = maxValue;

        var spinner = new JSpinner(new SpinnerNumberModel(defaultValue / scale, minValue / scale, clipMaxValue ? maxValue / scale : Double.MAX_VALUE, Prefs.get(id, 1 / scale)));
        if (IJ.isLinux()) spinner.setBackground(Color.white);
        ((SpinnerNumberModel) spinner.getModel()).setStepSize(1/scale);

        // Remove buttons and add right-click functionality
        var f = modifySpinner(spinner, true);
        if (f != null) f.setColumns(columns);

        spinner.addChangeListener(_ -> {
            consumer.accept((Double) spinner.getValue());
            s.setValue((int) ((Double) spinner.getValue() * scale));
        });
        s.addAdjustmentListener(e -> spinner.setValue(s.getValue() / scale));
        s.addMouseWheelListener(e -> {
            var delta = e.getPreciseWheelRotation() * s.getUnitIncrement();

            var newValue = (Double) spinner.getValue() -
                    delta * ((SpinnerNumberModel) spinner.getModel()).getStepSize().doubleValue();

            if (newValue < ((Double) ((SpinnerNumberModel) spinner.getModel()).getMinimum())) {
                newValue = ((Double) ((SpinnerNumberModel) spinner.getModel()).getMinimum());
            } else if (newValue > ((Double) ((SpinnerNumberModel) spinner.getModel()).getMaximum())) {
                newValue = ((Double) ((SpinnerNumberModel) spinner.getModel()).getMaximum());
            }

            spinner.setValue(newValue);
        });

        if (saveAndUseStepSize) {
            spinner.addPropertyChangeListener(_ ->
                    Prefs.set(id, ((SpinnerNumberModel) spinner.getModel()).getStepSize().doubleValue()));
        }

        JPanel panel = new JPanel();
        GridBagLayout pgrid = new GridBagLayout();
        GridBagConstraints pc = new GridBagConstraints();
        panel.setLayout(pgrid);
        pc.gridx = 0;
        pc.gridy = 0;
        pc.gridwidth = 1;
        pc.ipadx = 85;
        pc.anchor = GridBagConstraints.WEST;
        panel.add(s, pc);
        pc.ipadx = 0;  // reset
        // text field
        pc.gridx = 1;
        pc.insets = new Insets(5, 5, 0, 0);
        pc.anchor = GridBagConstraints.EAST;
        panel.add(spinner, pc);

        c.anchor = GridBagConstraints.EAST;
        b.add(panel);
        if (overridePosition) c.gridx = x.get();
        useCustomPosition();
        addLocal(b, c);
        x.getAndIncrement();

        if (Recorder.record || macro)
            saveLabel(spinner, label, s3 -> {
                getTextFieldFromSpinner(spinner).ifPresent(field -> field.setText(s3));
                consumer.accept(Double.parseDouble(s3));
            }, () -> {
                if (recorderOn) {
                    recordOption(spinner, getTextFieldFromSpinner(spinner).get().getText());
                }
            });

        return panel;
    }

    //todo macro support for button group

    private String stepSizeId(String label) {
        var classOptional = StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE)
                .walk(s1 -> s1.map(StackWalker.StackFrame::getDeclaringClass)
                        .filter(c -> !c.getName().contains("Thread") && !c.equals(GenericSwingDialog.class)).findFirst());
        var s = classOptional.map(Class::getName).orElse("NONAME");
        return "stepSize." + s + label.replaceAll("[\s:]", "");
    }

    public ComponentPair addBoundedNumericField(String label, Bounds bounds, double stepSize, int columns, String units, Property<Integer> property) {
        return addBoundedNumericField(label, bounds, property.get(), stepSize, columns, units, true, d -> property.set(d.intValue()));
    }

    public ComponentPair addBoundedNumericField(String label, Bounds bounds, double stepSize, int columns, String units, boolean useInt, Property<Double> property) {
        return addBoundedNumericField(label, bounds, property.get(), stepSize, columns, units, useInt, property::set);
    }

    public ComponentPair addUnboundedNumericField(String label, double defaultValue, double stepSize, int columns, String units, Consumer<Double> consumer) {
        return addBoundedNumericField(label, new Bounds(), defaultValue, stepSize, columns, units, consumer);
    }

    public ComponentPair addUnboundedNumericField(String label, double defaultValue, double stepSize, int columns, String units, boolean useInt, Consumer<Double> consumer) {
        return addBoundedNumericField(label, new Bounds(), defaultValue, stepSize, columns, units, useInt, consumer);
    }

    public ComponentPair addBoundedNumericField(String label, Bounds bounds, double defaultValue, double stepSize, int columns, String units, Consumer<Double> consumer) {
        return addBoundedNumericField(label, bounds, defaultValue, stepSize, columns, units, false, consumer);
    }

    public ComponentPair addBoundedNumericField(String label, Bounds bounds, double defaultValue, double stepSize, int columns, String units, final boolean useInt, Consumer<Double> consumer) {
        var c = getConstraints();
        var x = getXPos();
        Box b = Box.createHorizontalBox();
        Label fieldLabel = makeLabel(label.replaceAll("_", " "));
        if (addToSameRow) {
            c.gridx = GridBagConstraints.RELATIVE;
            c.insets.left = 10;
        } else {
            c.gridx = 0;
            c.gridy++;
            c.insets = new Insets(DialogBoxType.NUMERIC.isPresent() ? 0 : 5, 0, 3, 0);
        }

        c.anchor = GridBagConstraints.EAST;
        c.gridwidth = 1;
        b.add(Box.createHorizontalStrut(5));
        b.add(fieldLabel);

        if (addToSameRow) {
            c.insets.left = 0;
            addToSameRow = false;
        }

        if (IJ.isWindows()) columns -= 2;
        if (columns < 1) columns = 1;

        if (defaultValue < bounds.min()) {
            defaultValue = bounds.min();
        } else if (defaultValue > bounds.max()) {
            defaultValue = bounds.max();
        }

        var id = stepSizeId(label);

        var tf = new JSpinner(new SpinnerNumberModel(defaultValue, bounds.min(), bounds.max(), Prefs.get(id, stepSize)));
        if (IJ.isLinux()) tf.setBackground(Color.white);

        tf.addChangeListener(_ -> {
            var v = (Double) tf.getValue();
            if (v.compareTo(bounds.boxedMin()) < 0) {
                v = bounds.boxedMin();
                tf.setValue(v);
            } else if (v.compareTo(bounds.boxedMax()) > 0) {
                v = bounds.boxedMax();
                tf.setValue(v);
            }

            if (!useInt && !(v.compareTo(bounds.boxedMin()) == 0 || v.compareTo(bounds.boxedMax()) == 0)) {
                var bv = BigDecimal.valueOf(v);
                bv = bv.setScale(10, RoundingMode.HALF_EVEN);
                tf.setValue(bv.doubleValue());
            }

            consumer.accept(useInt ? Math.rint(v) : v);
        });

        if (saveAndUseStepSize) {
            tf.addPropertyChangeListener(_ ->
                    Prefs.set(id, ((SpinnerNumberModel) tf.getModel()).getStepSize().doubleValue()));
        }

        // Add right-click functionality
        var f = modifySpinner(tf, false);
        if (f != null) {
            // Setup formatter
            JFormattedTextField.AbstractFormatter format = new NumberFormatter() {
                @Override
                public Object stringToValue(String string) {
                    if (!f.isEnabled()) return bounds.min();
                    NumberFormat nF = NumberFormat.getNumberInstance();

                    try {
                        var d = nF.parse(string);
                        return useInt ? d.intValue() : d.doubleValue();
                    } catch (ParseException e) {
                        e.printStackTrace();
                    }

                    return bounds.min();
                }

                @Override
                public String valueToString(Object value) {
                    if (useInt && value instanceof Double d) {
                        return Integer.toString(d.intValue());
                    }
                    return value.toString();
                }

                @Override
                public Class<?> getValueClass() {
                    return useInt ? Integer.class : Double.class;
                }
            };

            f.setFormatterFactory(new DefaultFormatterFactory(format));

            f.setColumns(columns);
        }

        if (IJ.isLinux()) tf.setBackground(Color.white);

        JComponent out;
        if (units == null || units.equals("")) {
            b.add(tf);
            out = tf;
        } else {
            JPanel panel = new JPanel();
            panel.setLayout(new FlowLayout(FlowLayout.LEFT, 0, 0));
            panel.add(tf);
            panel.add(new Label(" " + units));
            b.add(panel);
            out = panel;
        }

        b.add(Box.createHorizontalStrut(10));

        b.add(Box.createHorizontalGlue());

        if (overridePosition) c.gridx = x.get();
        useCustomPosition();
        addLocal(b, c);
        x.getAndIncrement();

        if (Recorder.record || macro)
            saveLabel(tf, label, s -> {
                getTextFieldFromSpinner(tf).ifPresent(field -> field.setText(s));
                consumer.accept(Double.parseDouble(s));
            }, () -> {
                if (recorderOn) {
                    recordOption(tf, getTextFieldFromSpinner(tf).get().getText());
                }
            });

        return new ComponentPair(tf, out);
    }

    public Component addGenericComponent(Component b) {
        var c = getConstraints();
        var x = getXPos();
        if (addToSameRow) {
            c.gridx = GridBagConstraints.RELATIVE;
            c.insets.left = 10;
            addToSameRow = false;
        } else {
            c.gridx = 0;
            c.gridy++;
            c.anchor = GridBagConstraints.WEST;
            c.insets = new Insets(DialogBoxType.CHECKBOX.isPresent() ? 0 : 15, 20, 0, 0);
        }
        if (overridePosition) c.gridx = x.get();
        useCustomPosition();
        addLocal(b, c);
        x.getAndIncrement();
        return b;
    }

    public <T extends Enum<T> & NState<T>> NStateButton<T> addNStateButton(T defaultState, boolean swapButtons, Consumer<T> consumer) {
        var c = getConstraints();
        var x = getXPos();
        var b = new NStateButton<T>(defaultState, swapButtons);
        b.addActionListener(_ -> consumer.accept(b.getState()));
        if (addToSameRow) {
            c.gridx = GridBagConstraints.RELATIVE;
            c.insets.left = 10;
            addToSameRow = false;
        } else {
            c.gridx = 0;
            c.gridy++;
            c.insets = new Insets(DialogBoxType.CHECKBOX.isPresent() ? 0 : 15, 20, 0, 0);
        }
        c.anchor = GridBagConstraints.WEST;
        c.gridwidth = 1;
        useCustomPosition();
        if (overridePosition) c.gridx = x.get();
        addLocal(b, c);
        x.getAndIncrement();
        c.insets.left = 0;

        if (Recorder.record || macro) {
            saveLabel(b, defaultState.getDeclaringClass().getName(), s -> {
                var d = defaultState.fromString(s);
                b.setState(d);
                consumer.accept(d);
            }, () -> {
                if (recorderOn) {
                    recordOption(b, b.getState().name());
                }
            });
        }

        return b;
    }

    @SuppressWarnings("unchecked")
    public <T extends Enum<T> & NState<T>> JComboBox<T> addNStateDropdown(T defaultState, Consumer<T> consumer) {
        var c = getConstraints();
        var x = getXPos();
        var b = new JComboBox<>(defaultState.values0());
        b.setRenderer(new ToolTipRenderer());
        b.setSelectedItem(defaultState);
        b.addActionListener(_ -> consumer.accept((T) b.getSelectedItem()));
        if (addToSameRow) {
            c.gridx = GridBagConstraints.RELATIVE;
            c.insets.left = 10;
            addToSameRow = false;
        } else {
            c.gridx = 0;
            c.gridy++;
            c.insets = new Insets(DialogBoxType.CHECKBOX.isPresent() ? 0 : 15, 20, 0, 0);
        }
        c.anchor = GridBagConstraints.WEST;
        c.gridwidth = 1;
        useCustomPosition();
        if (overridePosition) c.gridx = x.get();
        addLocal(b, c);
        x.getAndIncrement();
        c.insets.left = 0;

        if (Recorder.record || macro) {
            saveLabel(b, defaultState.getDeclaringClass().getName(), s -> {
                var d = defaultState.fromString(s);
                b.setSelectedItem(d);
                consumer.accept(d);
            }, () -> {
                if (recorderOn) {
                    recordOption(b, ((T) b.getSelectedItem()).name());
                }
            });
        }

        return b;
    }

    public <T extends Enum<T> & RadioEnum> Map<T, JRadioButton> addRadioOptions(Class<T> tClass, Consumer<T> consumer, boolean addToGui) {
        var c = getConstraints();
        var x = getXPos();
        var group = new ButtonGroup();
        var buttons = new HashMap<T, JRadioButton>();

        var values = tClass.getEnumConstants();

        AtomicReference<T> val = new AtomicReference<>();
        for (T value : values) {
            var button = new JRadioButton(value.optionText());
            buttons.put(value, button);
            button.getModel().setGroup(group);
            button.setToolTipText(value.tooltip());

            button.addActionListener(_ -> {
                if (button.isSelected()) {
                    consumer.accept(value);
                    val.set(value);
                }
            });

            if (addToGui) {
                if (addToSameRow) {
                    c.gridx = GridBagConstraints.RELATIVE;
                    c.insets.left = 10;
                } else {
                    c.gridx = 0;
                    c.gridy++;
                    c.anchor = GridBagConstraints.WEST;
                    c.insets = new Insets(DialogBoxType.CHECKBOX.isPresent() ? 0 : 15, 20, 0, 0);
                }
                if (overridePosition) c.gridx = x.get();
                useCustomPosition();
                addLocal(button, c);
                x.getAndIncrement();
            }
        }

        if (Recorder.record || macro) {
            saveLabel(group, tClass.getName(), s -> {
                var d = Enum.valueOf(tClass, s);
                buttons.get(d).setSelected(true);
                consumer.accept(d);
            }, () -> {
                if (recorderOn) {
                    recordOption(group, val.get().name());
                }
            });
        }

        return buttons;
    }

    //todo figure out, only use the group
    public static JRadioButton makeRadioButton(GenericSwingDialog gd, String text, Consumer<Boolean> listener, ButtonGroup group) {
        var r = new JRadioButton(text);
        if (group != null) r.getModel().setGroup(group);
        r.addActionListener(_ -> {
            listener.accept(r.isSelected());
            /*if (gd.recorderOn) {
                gd.recordOption(text, );
            }*/
        });

        /*if (Recorder.record || gd.macro)
            saveLabel(text, s -> {
                group.;
            });*/
        return r;
    }

    public void resetPositionOverride() {
        getXPos().set(0);
    }

    public void setNewPosition(int anchor) {
        customAnchor = true;
        this.anchor = anchor;
    }

    public void setWidth(int gridwith) {
        customAnchor = true;
        this.gridwith = gridwith;
    }

    public void setFill(int fill) {
        customAnchor = true;
        this.fill = fill;
    }

    public void setLeftInset(int inset) {
        this.leftInset = inset;
    }

    public void setRightInset(int inset) {
        this.rightInset = inset;
    }

    private void useCustomPosition() {
        var c = getConstraints();
        if (customAnchor) {
            customAnchor = false;
            c.anchor = this.anchor;
            c.insets.left = leftInset;
            c.insets.right = rightInset;
            c.fill = fill;
            leftInset = 0;
            rightInset = 0;
            c.gridwidth = gridwith;
            gridwith = 1;
            fill = GridBagConstraints.NONE;
        }
    }

    public void setOverridePosition(boolean va) {
        if (va) resetPositionOverride();
        overridePosition = va;
    }


    private static void updateStepSize(JSpinner spinner) {
        final var model = spinner.getModel();
        if (model instanceof SpinnerNumberModel numberModel) {
            AtomicReference<Number> stepSize = new AtomicReference<>(numberModel.getStepSize());

            var stepSizeDialog = new GenericSwingDialog("Set Step Size");
            stepSizeDialog.addBoundedNumericField("Step Size", new Bounds(0, Double.MAX_VALUE), stepSize.get().doubleValue(), 1, 10, null, stepSize::set);
            //stepSizeDialog.enableYesNoCancel("Confirm", null);
            stepSizeDialog.centerDialog(true);
            stepSizeDialog.showDialog();
            if (stepSizeDialog.wasOKed()) {
                ((SpinnerNumberModel) model).setStepSize(stepSize.get());
            }
        }
    }

    public boolean saveAndUseStepSize() {
        return saveAndUseStepSize;
    }

    /**
     * If the dialog should save the step size values to preferences and use them later on
     */
    public void setSaveAndUseStepSize(boolean saveAndUseStepSize) {
        this.saveAndUseStepSize = saveAndUseStepSize;
    }

    public void setOKLabel(String label) {
        okay.setText(label);
    }

    /**
     * Sets a replacement label for the "Cancel" button.
     */
    public void setCancelLabel(String label) {
        cancel.setText(label);
    }

    /**
     * Make this a "Yes No Cancel" dialog.
     */
    public void enableYesNoCancel() {
        enableYesNoCancel(" Yes ", " No ");
    }

    public void enableYesNoCancel(String yesLabel, String noLabel) {
        okay.setText(yesLabel);
        if (no != null)
            no.setText(noLabel);
        else if (noLabel != null)
            no = new JButton(noLabel);
    }

    public boolean wasOKed() {
        return wasOKed || macro;
    }

    public void showDialog() {
        displayDialog(true);
    }

    /**
     * @return false, this is a stub method to preserve checking code in callers.
     * Valid numbers are now enforced by the component
     */
    public boolean invalidNumber() {
        return false;
    }

    public void displayDialog(boolean show) {
        swappableSections.forEach((k, s) -> {
            s.showDefault();
        });

        if (!show) return;
        if (macro) {
            labels.forEach(($, p) -> {
                var nv = Macro.getValue(macroOptions, p.a(), null);
                if (nv != null) {
                    if (nv.startsWith("&")) {
                        nv = nv.substring(1);
                    }
                    Interpreter interp = Interpreter.getInstance();
                    var s = interp!=null?interp.getVariableAsString(nv):null;
                    if (s != null) {
                        nv = s;
                    }
                    //AIJLogger.log("Setting option from macro: %s = %s".formatted(p.a(), nv));
                    p.b().accept(nv);//todo better error handling
                }
            });

            dispose();
            return;
        }
        //setupPaneLayout();
        setResizable(true);

        var screen = GraphicsEnvironment.getLocalGraphicsEnvironment().getScreenDevices()[UIHelper.getScreen(this)];
        var nominalBounds = screen.getDefaultConfiguration().getBounds();
        var taskbar = Toolkit.getDefaultToolkit().getScreenInsets(screen.getDefaultConfiguration());

        setMaximumSize(new Dimension(nominalBounds.width - (taskbar.left + taskbar.right), nominalBounds.height - (taskbar.bottom + taskbar.top)));
        var displayPane = new JPanel();
        displayPane.setLayout(new GridBagLayout());
        if (no != null) {
            scrollPane.validate();
            scrollPane.setMinimumSize(getLayout().minimumLayoutSize(scrollPane));
        }
        setLayout(new GridBagLayout());
        Panel buttons = new Panel();
        buttons.setLayout(new FlowLayout(FlowLayout.CENTER, 5, 0));
        okay.addActionListener(_ -> {
            wasOKed = true;
            dispose();
        });
        okay.addKeyListener(this);
        if (!hideCancelButton) {
            cancel.addActionListener(_ -> {
                wasCanceled = true;
                dispose();
            });
            cancel.addKeyListener(this);
        }
        if (no != null) {
            no.addActionListener(_ -> dispose());
            no.addKeyListener(this);
        }
        boolean addHelp = helpURL != null;
        if (addHelp) {
            help = new JButton(helpLabel);
            help.addActionListener(_ -> {
                if (hideCancelButton && (helpURL == null || helpURL.isEmpty())) {
                    wasOKed = true;
                }
                showHelp();
            });
            help.addKeyListener(this);
        }
        if (IJ.isWindows() || Prefs.dialogCancelButtonOnRight) {
            buttons.add(okay);
            if (no != null) buttons.add(no);
            if (!hideCancelButton) buttons.add(cancel);
            if (addHelp) buttons.add(help);
        } else {
            if (addHelp) buttons.add(help);
            if (no != null) buttons.add(no);
            if (!hideCancelButton) buttons.add(cancel);
            buttons.add(okay);
        }
        if (addToSameRow) {
            c.gridx = GridBagConstraints.RELATIVE;
        } else {
            c.gridx = 0;
            c.gridy++;
        }
        c.anchor = GridBagConstraints.SOUTHEAST;
        c.gridwidth = addToSameRowCalled ? GridBagConstraints.REMAINDER : 2;
        c.insets = new Insets(15, 0, 0, 0);
        displayPane.add(scrollPane);
        displayPane.add(buttons, c);
        displayPane.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        Font font = new Font("Dialog", Font.PLAIN,12);
        if (!fontSizeSet && font != null && Prefs.getGuiScale() != 1.0) {
            fontSizeSet = true;
            setFont(font.deriveFont((float) (font.getSize() * Prefs.getGuiScale())));
        }
        UIHelper.recursiveFontSetter(this, getFont());
        UIHelper.recursiveFontSetter(rootPane, getFont());
        UIHelper.recursiveFontSetter(scrollPane, getFont());
        UIHelper.recursiveFontSetter(displayPane, getFont());
        if (rootPane.getComponentCount() > 0) okay.requestFocusInWindow();
        scrollPane.validate();
        setMinimumSize(getLayout().minimumLayoutSize(scrollPane));
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setContentPane(displayPane);
        displayPane.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                super.componentResized(e);
                var currentBorders = displayPane.getSize();
                var buttonPanelSize = buttons.getSize();

                var h = currentBorders.height - buttonPanelSize.height - 25;
                var w = currentBorders.width;
                scrollPane.setSize(new Dimension(w, h));
                scrollPane.setMinimumSize(new Dimension(w, h));
                repaint();
                revalidate();
            }
        });
        validate();
        pack();

        if (centerDialog) {
            GUI.centerOnImageJScreen(this);
        }

        setVisible(true);
    }

    @Override
    public Dimension getPreferredSize() {
        Dimension dim=super.getPreferredSize();
        if (dim.width > getMaximumSize().width) dim.width = getMaximumSize().width;
        if (dim.height > getMaximumSize().height) dim.height = getMaximumSize().height;
        return dim;
    }

    public void centerDialog(boolean b) {
        centerDialog = b;
    }

    public void disableNo() {
        no = null;
    }

    public void setLocation(int x, int y) {
        super.setLocation(x, y);
        centerDialog = false;
    }

    public static JFormattedTextField modifySpinner(JSpinner spinner, boolean removeButtons) {
        for (Component component : spinner.getComponents()) {
            if (removeButtons && component instanceof AbstractButton) {
                spinner.remove(component);
            }
            if (component instanceof JSpinner.DefaultEditor editor) {
                editor.getTextField().addMouseListener(new NumericFieldStepSizeEditor(spinner));
                editor.addMouseWheelListener(e -> {
                    if (spinner.getModel() instanceof SpinnerNumberModel spin) {
                        var delta = e.getPreciseWheelRotation() * spin.getStepSize().doubleValue();
                        var newValue = -delta + ((Number) spinner.getValue()).doubleValue();

                        if (newValue < ((Number) spin.getMinimum()).doubleValue()) {
                            newValue = ((Number) spin.getMinimum()).doubleValue();
                        } else if (newValue > ((Number) spin.getMaximum()).doubleValue()) {
                            newValue = ((Number) spin.getMaximum()).doubleValue();
                        }

                        spinner.setValue(newValue);
                    }

                });
                return editor.getTextField();
            }
        }

        return null;
    }

    private Component addLocal(Component comp) {
        return getPanel().add(comp);
    }

    private void addLocal(Component comp, Object constraints) {
        getPanel().add(comp, constraints);
    }

    private Label makeLabel(String label) {
        if (IJ.isMacintosh()) label += " ";
        return new Label(label);
    }

    private void setupPaneLayout() {
        ImageJ ij = IJ.getInstance();
        if (ij != null) setFont(ij.getFont());
        setLayout(new BorderLayout(0, 0)); // Forces scrollpane to actually work
        //todo remove black box around basePanel
        basePanel.setLayout(new GridBagLayout());
        basePanel.validate();
        scrollPane.setViewportView(basePanel);
        addWindowListener(this);
        super.add(scrollPane);
    }

    public boolean wasCanceled() {
        return wasCanceled;
    }

    public void setHelpLabel(String label) {
        helpLabel = label;
    }

    public void addHelp(String url) {
        helpURL = url;
    }

    public JButton getOkay() {
        return okay;
    }

    public JButton getCancel() {
        return cancel;
    }

    public JButton getNo() {
        return no;
    }

    public void addToSameRow() {
        addToSameRow = true;
        addToSameRowCalled = true;
    }

    void showHelp() {
        if (helpURL.startsWith("<html>")) {
            String title = getTitle() + " " + helpLabel;
            /*if (this instanceof NonBlockingGenericDialog)
                new HTMLDialog(title, helpURL, false); // non blocking
            else*/
            new HTMLDialog(this, title, helpURL); //modal
        } else {
            String macro = "call('ij.plugin.BrowserLauncher.open', '" + helpURL + "');";
            new MacroRunner(macro); // open on separate thread using BrowserLauncher
        }
    }

    void setIcon() {
        URL url = this.getClass().getClassLoader().getResource("astronomy_icon.png");
        if (url == null) return;
        Image img = null;
        try {
            img = createImage((ImageProducer) url.getContent());
        } catch (IOException ignored) {
        }
        if (img != null) setIconImage(img);
    }

    @Override
    public void setFont(Font font) {
        super.setFont(!fontSizeSet && Prefs.getGuiScale() != 1.0 && font != null ? font.deriveFont((float) (font.getSize() * Prefs.getGuiScale())) : font);
        fontSizeSet = true;
    }

    /** Saves the label for given component, for macro recording and for accessing the component in macros. */
    private void saveLabel(Object component, String label, Consumer<String> consumer, Runnable finalizer) {
        if (labels==null)
            labels = new LinkedHashMap<>();
        if (label.length()>0)
            label = Macro.trimKey(label.trim());
        if (label.length()>0 && hasLabel(label)) {                      // not a unique label?
            label += "_0";
            for (int n=1; hasLabel(label); n++) {   // while still not a unique label
                label = label.substring(0, label.lastIndexOf('_')); //remove counter
                label += "_"+n;
            }
        }
        labels.put(component, new Triple<>(label, consumer, finalizer));
    }

    /** Returns whether the list of labels for macro recording or macro creation contains a given label. */
    private boolean hasLabel(String label) {
        return labels.values().stream().anyMatch(p -> p.a().equals(label));
    }

    private String trim(String value) {
        if (value.endsWith(".0"))
            value = value.substring(0, value.length()-2);
        if (value.endsWith(".00"))
            value = value.substring(0, value.length()-3);
        return value;
    }

    private void recordOption(Object c, String value) {
        var label = labels.get(c).a();
        if (value.equals("")) value = "[]";
        Recorder.recordOption(label, trim(value));
    }

    /**
     * Invoked when an action occurs.
     *
     * @param e the event to be processed
     */
    @Override
    public void actionPerformed(ActionEvent e) {

    }

    /**
     * Invoked when the value of the adjustable has changed.
     *
     * @param e the event to be processed
     */
    @Override
    public void adjustmentValueChanged(AdjustmentEvent e) {

    }

    /**
     * Invoked when a component gains the keyboard focus.
     *
     * @param e the event to be processed
     */
    @Override
    public void focusGained(FocusEvent e) {

    }

    /**
     * Invoked when a component loses the keyboard focus.
     *
     * @param e the event to be processed
     */
    @Override
    public void focusLost(FocusEvent e) {

    }

    /**
     * Invoked when an item has been selected or deselected by the user.
     * The code written for this method performs the operations
     * that need to occur when an item is selected (or deselected).
     *
     * @param e the event to be processed
     */
    @Override
    public void itemStateChanged(ItemEvent e) {

    }

    /**
     * Invoked when a key has been typed.
     * See the class description for {@link KeyEvent} for a definition of
     * a key typed event.
     *
     * @param e the event to be processed
     */
    @Override
    public void keyTyped(KeyEvent e) {

    }

    /**
     * Invoked when a key has been pressed.
     * See the class description for {@link KeyEvent} for a definition of
     * a key pressed event.
     *
     * @param e the event to be processed
     */
    @Override
    public void keyPressed(KeyEvent e) {
        Component component = e.getComponent();
        int keyCode = e.getKeyCode();
        IJ.setKeyDown(keyCode);
        if ((component instanceof Scrollbar) && (keyCode == KeyEvent.VK_LEFT || keyCode == KeyEvent.VK_RIGHT)) {
            Scrollbar sb = (Scrollbar) component;
            int value = sb.getValue();
            if (keyCode == KeyEvent.VK_RIGHT)
                sb.setValue(value + 1);
            else
                sb.setValue(value - 1);
            return;
        }
        if (keyCode == KeyEvent.VK_ENTER && /*textArea1==null &&*/ okay != null && okay.isEnabled()) {
            wasOKed = true;
            dispose();
        } else if (keyCode == KeyEvent.VK_ESCAPE) {
            wasCanceled = true;
            dispose();
            IJ.resetEscape();
        } else if (keyCode == KeyEvent.VK_W && (e.getModifiers() & Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()) != 0) {
            wasCanceled = true;
            dispose();
        }
    }

    /**
     * Invoked when a key has been released.
     * See the class description for {@link KeyEvent} for a definition of
     * a key released event.
     *
     * @param e the event to be processed
     */
    @Override
    public void keyReleased(KeyEvent e) {
        int keyCode = e.getKeyCode();
        IJ.setKeyUp(keyCode);
        int flags = e.getModifiers();
        boolean control = (flags & KeyEvent.CTRL_MASK) != 0;
        boolean meta = (flags & KeyEvent.META_MASK) != 0;
        boolean shift = (flags & InputEvent.SHIFT_MASK) != 0;
        if (keyCode == KeyEvent.VK_G && shift && (control || meta))
            new ScreenGrabber().run("");
    }

    /**
     * Invoked when the value of the text has changed.
     * The code written for this method performs the operations
     * that need to occur when text changes.
     *
     * @param e the event to be processed
     */
    @Override
    public void textValueChanged(TextEvent e) {

    }

    /**
     * Invoked the first time a window is made visible.
     *
     * @param e the event to be processed
     */
    @Override
    public void windowOpened(WindowEvent e) {

    }

    /**
     * Invoked when the user attempts to close the window
     * from the window's system menu.
     *
     * @param e the event to be processed
     */
    @Override
    public void windowClosing(WindowEvent e) {
        wasCanceled = true;
        dispose();
    }

    @Override
    public void dispose() {
        super.dispose();
        Prefs.savePreferences();
        if (wasOKed) {
            if (labels != null) {
                labels.forEach(($, p) -> p.c().run());
            }

            if (Recorder.record) {
                Recorder.saveCommand();
            }
        }
    }

    /**
     * Invoked when a window has been closed as the result
     * of calling dispose on the window.
     *
     * @param e the event to be processed
     */
    @Override
    public void windowClosed(WindowEvent e) {

    }

    /**
     * Invoked when a window is changed from a normal to a
     * minimized state. For many platforms, a minimized window
     * is displayed as the icon specified in the window's
     * iconImage property.
     *
     * @param e the event to be processed
     * @see Frame#setIconImage
     */
    @Override
    public void windowIconified(WindowEvent e) {

    }

    /**
     * Invoked when a window is changed from a minimized
     * to a normal state.
     *
     * @param e the event to be processed
     */
    @Override
    public void windowDeiconified(WindowEvent e) {

    }

    /**
     * Invoked when the Window is set to be the active Window. Only a Frame or
     * a Dialog can be the active Window. The native windowing system may
     * denote the active Window or its children with special decorations, such
     * as a highlighted title bar. The active Window is always either the
     * focused Window, or the first Frame or Dialog that is an owner of the
     * focused Window.
     *
     * @param e the event to be processed
     */
    @Override
    public void windowActivated(WindowEvent e) {

    }

    /**
     * Invoked when a Window is no longer the active Window. Only a Frame or a
     * Dialog can be the active Window. The native windowing system may denote
     * the active Window or its children with special decorations, such as a
     * highlighted title bar. The active Window is always either the focused
     * Window, or the first Frame or Dialog that is an owner of the focused
     * Window.
     *
     * @param e the event to be processed
     */
    @Override
    public void windowDeactivated(WindowEvent e) {

    }

    private static class SwappableSectionHolder<T extends Enum<T> & NState<T>> {
        List<SwappableSection<T>> swappableSections;
        int linkedSection;

        public SwappableSectionHolder(SwappableSection<T> section) {
            this.swappableSections = new ArrayList<>();
            swappableSections.add(section);
        }

        public SwappableSection<T> addNewPanel() {
            swappableSections.add(new SwappableSection<>(swappableSections.get(0).defaultState));
            linkedSection = swappableSections.size() - 1;
            return swappableSections.get(linkedSection);
        }

        public void setLinkedSection(int linkedSection) {
            this.linkedSection = linkedSection;
        }

        public JPanel getSectionPanel() {
            return swappableSections.get(linkedSection).getSectionPanel();
        }

        public void setCurrentState(T currentState) {
            for (SwappableSection<T> swappableSection : swappableSections) {
                swappableSection.setCurrentState(currentState);
            }
        }

        public JPanel currentPanel() {
            return swappableSections.get(linkedSection).currentPanel();
        }

        public GridBagConstraints currentConstrains() {
            return swappableSections.get(linkedSection).currentConstrains();
        }

        public AtomicInteger currentXPos() {
            return swappableSections.get(linkedSection).currentXPos();
        }

        public void showDefault() {
            for (SwappableSection<T> swappableSection : swappableSections) {
                swappableSection.showDefault();
            }
        }
    }

    private static class SwappableSection<T extends Enum<T> & NState<T>> {
        T currentState;
        final T defaultState;
        final CardLayout cardLayout;
        final JPanel sectionPanel;
        final Map<T, Panel> panelMap = new HashMap<>();

        public SwappableSection(T currentState) {
            this.currentState = currentState;
            this.defaultState = currentState;
            var states = currentState.getDeclaringClass().getEnumConstants();
            cardLayout = new ResizingCardLayout();
            sectionPanel = new JPanel(cardLayout);
            for (T state : states) {
                JPanel card = new JPanel(new GridBagLayout());
                //card.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(Color.MAGENTA), "Card"));
                var c = new GridBagConstraints();
                c.weightx = 1;
                panelMap.put(state, new Panel(card, c, new AtomicInteger()));
                sectionPanel.add(card, state.name());
            }
            //getSectionPanel().setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(Color.RED), "Section"));
        }

        public JPanel getSectionPanel() {
            return sectionPanel;
        }

        public void setCurrentState(T currentState) {
            this.currentState = currentState;
            cardLayout.show(sectionPanel, currentState.name());
        }

        public JPanel currentPanel() {
            return panelMap.get(currentState).panel();
        }

        public GridBagConstraints currentConstrains() {
            return panelMap.get(currentState).constraints();
        }

        public AtomicInteger currentXPos() {
            return panelMap.get(currentState).xPos();
        }

        public void showDefault() {
            setCurrentState(defaultState);
        }

        private record Panel(JPanel panel, GridBagConstraints constraints, AtomicInteger xPos) {}

        // Return size of current card rather than maximum of all cards
        private static class ResizingCardLayout extends CardLayout {
            @Override
            public Dimension preferredLayoutSize(Container parent) {
                Component current = findCurrentComponent(parent);
                if (current != null) {
                    Insets insets = parent.getInsets();
                    Dimension pref = current.getPreferredSize();
                    pref.width += insets.left + insets.right;
                    pref.height += insets.top + insets.bottom;
                    return pref;
                }
                return super.preferredLayoutSize(parent);
            }

            public Component findCurrentComponent(Container parent) {
                for (Component comp : parent.getComponents()) {
                    if (comp.isVisible()) {
                        return comp;
                    }
                }
                return null;
            }
        }
    }

    private enum DialogBoxType {
        CHECKBOX,
        SLIDER,
        CHOICE,
        NUMERIC;

        public int amount = 0;

        public boolean isPresent() {
            return amount++ > 0;
        }
    }

    /**
     * Bounds queries must be done with methods, not fields to handle inclusivity!
     */
    public record Bounds(double min, boolean minIsInclusive, double max, boolean maxIsInclusive) {
        public Bounds() {
            this(-Double.MAX_VALUE, Double.MAX_VALUE);
        }

        public Bounds(double min, double max) {
            this(min, true, max, true);
        }

        @Override
        public double max() {
            return maxIsInclusive ? max : nextDown(max);
        }

        @Override
        public double min() {
            return minIsInclusive ? min : nextUp(min);
        }

        public Double boxedMin() {
            return min();
        }

        public Double boxedMax() {
            return max();
        }
    }

    public record ComponentPair(Component c1, JComponent c2, LinkedList<JComponent> subComponents) {
        public ComponentPair(JComponent c1, JComponent c2) {
            this(c1, c2, null);
        }

        public JComponent asSwingComponent(Type type) {
            return switch (type) {
                case C1 -> ((JComponent) c1);
                case C2 -> c2;
            };
        }

        public enum Type {
            C1, C2;
        }
    }

    private static class NumericFieldStepSizeEditor implements MouseListener {
        private final JSpinner spinner;

        public NumericFieldStepSizeEditor(JSpinner spinner) {
            this.spinner = spinner;
        }

        @Override
        public void mouseClicked(MouseEvent e) {
        }

        @Override
        public void mousePressed(MouseEvent e) {
            if (SwingUtilities.isRightMouseButton(e)) {
                updateStepSize(spinner);
            }
        }

        @Override
        public void mouseReleased(MouseEvent e) {
        }

        @Override
        public void mouseEntered(MouseEvent e) {
        }

        @Override
        public void mouseExited(MouseEvent e) {
        }
    }

    public record Triple<T1, T2, T3>(T1 a, T2 b, T3 c) {
    }

    private record StackComponent(JComponent component, GridBagConstraints constraints) {
        public StackComponent(Box box) {
            this(box, null);
        }
    }
}
