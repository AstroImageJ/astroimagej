package Astronomy.multiplot.macro.title.highlighting;

import javax.swing.text.*;
import java.awt.*;

public class TextColoringHighlighter extends DefaultHighlighter.DefaultHighlightPainter {
    private final Color textColor;

    public TextColoringHighlighter(Color color) {
        super(Color.YELLOW); // Set a default highlight color (e.g., yellow)
        this.textColor = color;
    }

    @Override
    public void paint(Graphics g, int offs0, int offs1, Shape bounds, JTextComponent c) {
        Document doc = c.getDocument();
        //String text = doc.getText(offs0, offs1 - offs0);
        if (c.getDocument() instanceof StyledDocument styledDocument) {
            AttributeSet attrs = styledDocument.getCharacterElement(offs0).getAttributes();

            if (StyleConstants.isBold(attrs)) {
                g.setColor(textColor);
                g.setFont(g.getFont().deriveFont(Font.BOLD));
            } else {
                g.setColor(Color.BLUE);
            }
        }

        // Call the parent class to draw the default highlight
        //super.paint(g, offs0, offs1, bounds, c);
    }

    @Override
    public Shape paintLayer(Graphics g, int offs0, int offs1, Shape bounds, JTextComponent c, View view) {
        //String text = doc.getText(offs0, offs1 - offs0);
        if (c.getDocument() instanceof StyledDocument styledDocument) {
            AttributeSet attrs = styledDocument.getCharacterElement(offs0).getAttributes();

            if (StyleConstants.isBold(attrs)) {
                g.setColor(textColor);
                g.setFont(g.getFont().deriveFont(Font.BOLD));
            } else {
                g.setColor(Color.BLUE);
            }
        }
        return super.paintLayer(g, offs0, offs1, bounds, c, view);
    }
}
