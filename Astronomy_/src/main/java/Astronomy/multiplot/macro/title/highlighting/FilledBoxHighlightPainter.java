package Astronomy.multiplot.macro.title.highlighting;

import javax.swing.text.*;
import java.awt.*;
import java.awt.geom.Rectangle2D;

public class FilledBoxHighlightPainter extends DefaultHighlighter.DefaultHighlightPainter {

    public FilledBoxHighlightPainter() {
        super(new Color(57, 117, 49));
    }

    @Override
    public Shape paintLayer(Graphics g, int offs0, int offs1,
                            Shape bounds, JTextComponent c, View view) {
        Color color = getColor();

        if (g instanceof Graphics2D g2) {
            if (color == null) {
                g.setColor(c.getSelectionColor());
            }
            else {
                g.setColor(color);
            }

            Rectangle2D r;

            if (offs0 == view.getStartOffset() && offs1 == view.getEndOffset()) {
                // Contained in view, can just use bounds.
                if (bounds instanceof Rectangle2D) {
                    r = (Rectangle2D) bounds;
                } else {
                    r = bounds.getBounds2D();
                }
            } else {
                // Should only render part of View.
                try {
                    // --- determine locations ---
                    Shape shape = view.modelToView(offs0, Position.Bias.Forward,
                            offs1,Position.Bias.Backward,
                            bounds);
                    r = (shape instanceof Rectangle2D) ?
                            (Rectangle2D)shape : shape.getBounds();
                } catch (BadLocationException e) {
                    // can't render
                    r = null;
                }
            }

            if (r != null) {
                g2.fill(r);
            }

            return r;
        }

        return null;
    }
}
