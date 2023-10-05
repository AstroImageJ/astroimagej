package Astronomy.multiplot.macro.title.highlighting;

import Astronomy.multiplot.macro.title.parser.ASTHandler;

import javax.swing.plaf.TextUI;
import javax.swing.text.*;
import java.awt.*;
import java.awt.geom.Rectangle2D;

public class BoxHighlightPainter extends DefaultHighlighter.DefaultHighlightPainter {
    double delta = 0;
    final ASTHandler.HighlightInfo highlightInfo;

    public BoxHighlightPainter(Color borderColor, double delta, ASTHandler.HighlightInfo highlightInfo) {
        super(borderColor);
        this.delta = delta;
        this.highlightInfo = highlightInfo;
    }

    @Override
    public void paint(Graphics g, int offs0, int offs1, Shape bounds, JTextComponent c) {
        if (g instanceof Graphics2D g2) {
            Rectangle2D alloc = bounds.getBounds2D();
            try {
                // --- determine locations ---
                TextUI mapper = c.getUI();
                Rectangle2D p0 = mapper.modelToView2D(c, offs0, Position.Bias.Forward);
                Rectangle2D p1 = mapper.modelToView2D(c, offs1, Position.Bias.Forward);

                // --- render ---
                Color color = getColor();

                if (color == null) {
                    g.setColor(c.getSelectionColor());
                }
                else {
                    g.setColor(color);
                }

                if (p0.getY() == p1.getY()) {
                    // same line, render a rectangle
                    var r = new Rectangle2D.Double();
                    Rectangle2D.union(p0, p1, r);
                    applyDelta(r);
                    g2.draw(r);
                } else {
                    // different lines
                    var r = new Rectangle2D.Double();
                    Rectangle2D.union(p0, p1, r);
                    applyDelta(r);
                    var co = g2.getClip();
                    var cr = (Rectangle2D.Double)r.clone();
                    cr.width -= 1;
                    g2.setClip(cr);
                    g2.draw(r);
                    g2.setClip(co);
                }
            } catch (BadLocationException e) {
                e.printStackTrace();
            }
        }
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
                applyDelta(r);
                try {
                    Rectangle2D p0 = c.modelToView2D(offs0);
                    Rectangle2D p1 = c.modelToView2D(offs1);
                    /*AIJLogger.log(c.getText(offs0, offs1-offs0));
                    AIJLogger.multiLog(offs0, offs1);
                    AIJLogger.multiLog(Utilities.getRowStart(c, offs0), Utilities.getRowEnd(c, offs1));*/
                    if (p0.getY() == p1.getY()) {
                        if (offs0 == Utilities.getRowStart(c, offs0) &&
                                Utilities.getRowStart(c, highlightInfo.endIndex()) >= Utilities.getRowEnd(c, highlightInfo.beginIndex())) {
                            // different lines
                            var co = g2.getClip();
                            var cr = (Rectangle/*2D.Double*/)r.clone();
                            cr.x += 1;
                            cr.height += 1;
                            g2.setClip(cr);
                            g2.draw(r);
                            g2.setClip(co);
                        } else {
                            // same line, render a rectangle
                            g2.draw(r);
                        }
                    } else {
                        // different lines
                        var co = g2.getClip();
                        var cr = (Rectangle/*2D.Double*/)r.clone();
                        cr.width -= 1;
                        cr.height += 1;
                        g2.setClip(cr);
                        g2.draw(r);
                        g2.setClip(co);
                    }
                    return r;
                } catch (Exception e) {
                    e.printStackTrace();
                }
                g2.draw(r);
            }

            return r;
        }

        return null;
    }

    private void applyDelta(Rectangle2D r) {
        double centerX = r.getCenterX();
        double centerY = r.getCenterY();

        double newWidth = r.getWidth() + delta;
        double newHeight = r.getHeight() + delta;

        double newX = centerX - newWidth / 2D;
        double newY = centerY - newHeight / 2D;

        r.setRect(newX, newY, newWidth, newHeight);
    }
}