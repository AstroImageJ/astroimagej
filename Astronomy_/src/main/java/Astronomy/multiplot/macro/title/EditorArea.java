package Astronomy.multiplot.macro.title;

import Astronomy.MultiPlot_;
import Astronomy.multiplot.macro.title.highlighting.BoxHighlightPainter;
import Astronomy.multiplot.macro.title.highlighting.CircleHighlightPainter;
import Astronomy.multiplot.macro.title.highlighting.LineHighlightPainter;
import Astronomy.multiplot.macro.title.parser.ASTHandler;
import ij.astro.types.Pair;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultStyledDocument;
import javax.swing.undo.UndoManager;
import java.awt.*;
import java.awt.event.*;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;
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
    private DocumentListener listener;
    private UndoManager undoManager;

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
                    if (pt == null) {
                        Rectangle vis = displayField.getVisibleRect();
                        pt = new Point(vis.x + vis.width / 2,
                                vis.y + vis.height / 2);
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

        // Add undo/redo controls
        undoManager = new UndoManager();
        input.getDocument().addUndoableEditListener(undoManager);
        input.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_Z, InputEvent.CTRL_DOWN_MASK), "undo");
        input.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_Y, InputEvent.CTRL_DOWN_MASK), "redo");
        input.getActionMap().put("undo", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (undoManager.canUndo()) {
                    undoManager.undo();
                }
            }
        });
        input.getActionMap().put("redo", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (undoManager.canRedo()) {
                    undoManager.redo();
                }
            }
        });

        // Listener that updates the setting, is saved to avoid code update of field firing
        listener = new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                update();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                update();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                update();
            }

            private void update() {
                var text = input.getText();

                // Handle line continuations
                text = text.replaceAll("(?<!\\\\)\\\\\n", "");

                if (isProgram.get()) {
                    try {
                        var p = PlotNameResolver.resolve(MultiPlot_.getTable(), text);
                        render.setText(p.state().first());
                        highlight(input, p.highlightInfos());
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                // This repaint forces the box to correctly render the bottom face
                input.repaint();

                updateSetting.accept(text);
                displayField.setText(text.split("\n", 2)[0]);
            }
        };

        input.getDocument().addDocumentListener(listener);


        render.setEditable(false);

        inputB.second().setBorder(BorderFactory.createTitledBorder("Source"));
        renderB.second().setBorder(BorderFactory.createTitledBorder("Rendered Text"));

        add(inputB.second());
        add(renderB.second());
        //add(panel);
    }

    private void update() {
        if (input != null) {
            input.getDocument().removeDocumentListener(listener);
            input.getDocument().removeUndoableEditListener(undoManager);
            input.setText(initText.get());
            input.getDocument().addDocumentListener(listener);
            input.getDocument().addUndoableEditListener(undoManager);

            try {
                highlight(input, null);
            } catch (BadLocationException e) {
                e.printStackTrace();
            }
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

    private void highlight(JTextArea textArea, Set<ASTHandler.HighlightInfo> highlightInfos) throws BadLocationException {
        var highlighter = textArea.getHighlighter();
        var removedWhitespacePainter = new CircleHighlightPainter();
        var lineIndicator = new LineHighlightPainter();

        // Reset highlighter
        highlighter.removeAllHighlights();

        if (highlightInfos != null) {
            // Sort highlights by length, logically shorter sections will be inside large ones
            var sorted = highlightInfos.stream().sorted(Comparator.comparingInt(hi -> hi.endIndex() - hi.beginIndex())).toList();
            var alreadyHighlighted = new HashSet<ASTHandler.HighlightInfo>();

            for (ASTHandler.HighlightInfo info : sorted) {
                if (info.types().contains(ASTHandler.HighlightType.WHITESPACE)) {
                    if (info.types().contains(ASTHandler.HighlightType.MODIFIED_WHITESPACE)) {
                        highlighter.addHighlight(info.beginIndex(), info.beginIndex() + 1, removedWhitespacePainter);
                    }
                    continue;
                }
                if (info.types().contains(ASTHandler.HighlightType.FUNCTION)) {
                    var c = alreadyHighlighted.stream().filter(info::contains).count();
                    highlighter.addHighlight(info.beginIndex(), info.endIndex(), new BoxHighlightPainter(Color.BLACK, c * 3, info));
                    alreadyHighlighted.add(info);
                }
            }
        }

        // Add line continuation notification
        for (int i = 0; i < textArea.getLineCount(); i++) {
            var s = textArea.getLineStartOffset(i);
            highlighter.addHighlight(s, s+1, lineIndicator);
        }
    }

}
