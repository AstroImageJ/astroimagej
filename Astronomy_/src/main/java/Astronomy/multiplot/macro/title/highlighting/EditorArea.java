package Astronomy.multiplot.macro.title.highlighting;

import Astronomy.MultiPlot_;
import Astronomy.multiplot.macro.title.PlotNameResolver;
import ij.astro.types.Pair;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import javax.swing.text.DefaultStyledDocument;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class EditorArea extends JPopupMenu {
    private final Supplier<Boolean> isProgram;
    private final Supplier<String> initText;
    private final Consumer<String> updateSetting;
    //private final Function<String, String> processTitle;
    private final JTextField displayField;
    private JTextArea input;
    private JTextArea render;
    private Pair.GenericPair<JTextArea, JScrollPane> inputB;
    private Pair.GenericPair<JTextArea, JScrollPane> renderB;

    public EditorArea(Supplier<Boolean> isProgram, Supplier<String> initText, Consumer<String> updateSetting,
                      /*Function<String, String> processTitle,*/ JTextField displayField) {
        this.isProgram = isProgram;
        this.initText = initText;
        this.updateSetting = updateSetting;
        //this.processTitle = processTitle;
        this.displayField = displayField;

        displayField.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (SwingUtilities.isLeftMouseButton(e)) {
                    Point pt = displayField.getPopupLocation(null);
                    if(pt == null) {
                        Rectangle vis = displayField.getVisibleRect();
                        pt = new Point(vis.x+vis.width/2,
                                vis.y+vis.height/2);
                    }
                    show(displayField, pt.x, pt.y);
                }
            }
        });

        displayField.setEditable(false);
        displayField.setBackground(Color.WHITE);

        setLightWeightPopupEnabled(false);
        addPopupMenuListener(new PopupMenuListener() {
            public void popupMenuCanceled(PopupMenuEvent popupMenuEvent) {
                EditorArea.this.setVisible(false);
            }

            public void popupMenuWillBecomeInvisible(PopupMenuEvent popupMenuEvent) {
                EditorArea.this.setVisible(true);
            }

            public void popupMenuWillBecomeVisible(PopupMenuEvent popupMenuEvent) {
                EditorArea.this.setVisible(false);
            }
        });
        build();
        pack();
    }

    private void build() {
        //var panel = new JPanel();

        inputB = textArea();
        renderB = textArea();
        input = inputB.first();
        render = renderB.first();

        input.setEditable(true);
        input.setText(initText.get());
        input.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                updateReceived();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                updateReceived();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                updateReceived();
            }

            private void updateReceived() {//todo pasting is weird when highlighting
                var text = input.getText();

                // Handle line continuations
                text = text.replaceAll("(?<!\\\\)\\\\\n", "");

                if (isProgram.get()) {
                    render.setText(PlotNameResolver.resolve(MultiPlot_.getTable(), text).state().first());
                }
                updateSetting.accept(text);
                displayField.setText(text.split("\n", 2)[0]);
            }
        });

        render.setEditable(false);

        inputB.second().setBorder(BorderFactory.createTitledBorder("Source"));
        renderB.second().setBorder(BorderFactory.createTitledBorder("Rendered Text"));

        add(inputB.second());
        add(renderB.second());
        //add(panel);
    }

    private void update() {
        if (input != null) {
            input.setText(initText.get());
        }

        if (!isProgram.get() && getComponentIndex(renderB.second()) >= 0) {
            remove(renderB.second());
            pack();
        } else if (isProgram.get() && getComponentIndex(renderB.second()) < 0) {
            add(renderB.second());
            pack();
        }
    }

    @Override
    public void setVisible(boolean b) {
        update();
        super.setVisible(b);
    }

    //todo add code completion https://docs.oracle.com/javase/tutorial/uiswing/components/textarea.html
    private Pair.GenericPair<JTextArea, JScrollPane> textArea() {
        var textArea = new JTextArea();
        textArea.setDocument(new DefaultStyledDocument());
        textArea.setLineWrap(true);
        textArea.setWrapStyleWord(true);
        JScrollPane areaScrollPane = new JScrollPane(textArea);
        areaScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        areaScrollPane.setPreferredSize(new Dimension(700, 150));
        return new Pair.GenericPair<>(textArea, areaScrollPane);
    }
}
