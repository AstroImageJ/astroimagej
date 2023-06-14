package ij.astro.util;

import javax.swing.*;
import java.awt.*;
import java.awt.font.FontRenderContext;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;

public class EmojiIcon implements Icon {
    private final String text;
    private final int fontSize;

    public EmojiIcon(String text, int fontSize) {
        this.text = text;
        this.fontSize = fontSize;
    }

    @Override
    public void paintIcon(Component c, Graphics g, int x, int y) {
        Graphics2D g2d = (Graphics2D) g.create();

        Font defaultFont = UIManager.getFont("Button.font");
        Font iconFont = defaultFont.deriveFont(Font.PLAIN, fontSize);
        g2d.setFont(iconFont);
        g2d.setColor(Color.BLACK);

        FontRenderContext frc = g2d.getFontRenderContext();
        Rectangle2D textBounds = iconFont.getStringBounds(text, frc);

        int textX = x + (int) ((getIconWidth() - textBounds.getWidth()) / 2);
        int textY = y + (int) ((getIconHeight() - textBounds.getHeight()) / 2 - textBounds.getY());

        g2d.drawString(text, textX, textY);

        g2d.dispose();
    }

    @Override
    public int getIconWidth() {
        Graphics2D g2d = createGraphics2D();
        Font defaultFont = UIManager.getFont("Button.font");
        Font iconFont = defaultFont.deriveFont(Font.PLAIN, fontSize);
        g2d.setFont(iconFont);

        FontRenderContext frc = g2d.getFontRenderContext();
        Rectangle2D textBounds = iconFont.getStringBounds(text, frc);

        g2d.dispose();

        return (int) Math.ceil(textBounds.getWidth());
    }

    @Override
    public int getIconHeight() {
        Graphics2D g2d = createGraphics2D();
        Font defaultFont = UIManager.getFont("Button.font");
        Font iconFont = defaultFont.deriveFont(Font.PLAIN, fontSize);
        g2d.setFont(iconFont);

        FontRenderContext frc = g2d.getFontRenderContext();
        Rectangle2D textBounds = iconFont.getStringBounds(text, frc);

        g2d.dispose();

        return (int) Math.ceil(textBounds.getHeight());
    }

    private Graphics2D createGraphics2D() {
        BufferedImage image = new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB);
        return image.createGraphics();
    }
}