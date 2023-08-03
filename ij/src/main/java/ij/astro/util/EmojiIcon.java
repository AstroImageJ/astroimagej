package ij.astro.util;

import ij.astro.types.Pair;

import javax.swing.*;
import java.awt.*;
import java.awt.font.FontRenderContext;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.Objects;

public class EmojiIcon implements Icon {
    private final String text;
    private final int iconHeight;
    private final Color color;
    private float cachedFontSize = -1;

    public EmojiIcon(String text, int iconHeight) {
        this(text, iconHeight, Color.BLACK);
    }

    public EmojiIcon(String text, int iconHeight, Color color) {
        this.text = Objects.requireNonNull(text);
        this.iconHeight = iconHeight;
        this.color = Objects.requireNonNull(color);
    }

    @Override
    public void paintIcon(Component c, Graphics g, int x, int y) {
        Graphics2D g2d = (Graphics2D) g.create();

        Font defaultFont = UIManager.getFont("Button.font");
        float fontSize = getCachedFontSize(defaultFont);
        Font iconFont = defaultFont.deriveFont(Font.PLAIN, fontSize);
        g2d.setFont(iconFont);
        g2d.setColor(color);

        FontRenderContext frc = g2d.getFontRenderContext();

        String codePointString = getCodepointString(text, 0).first();
        Rectangle2D textBounds = iconFont.getStringBounds(codePointString, frc);

        int textX = x + (int) ((getIconWidth() - textBounds.getWidth()) / 2);
        int textY = y + (int) ((getIconHeight() - textBounds.getHeight()) / 2 - textBounds.getY());

        for (int i = 0; i < text.length(); i++) {
            var b = getCodepointString(text, i);

            g2d.drawString(b.first(), textX, textY);

            i += b.second() - 1;
        }

        g2d.dispose();
    }

    @Override
    public int getIconWidth() {
        Graphics2D g2d = createGraphics2D();
        Font defaultFont = UIManager.getFont("Button.font");
        float fontSize = getCachedFontSize(defaultFont);
        Font iconFont = defaultFont.deriveFont(Font.PLAIN, fontSize);
        g2d.setFont(iconFont);

        String codePointString = getCodepointString(text, 0).first();

        FontRenderContext frc = g2d.getFontRenderContext();
        Rectangle2D textBounds = iconFont.getStringBounds(codePointString, frc);

        g2d.dispose();

        return (int) Math.ceil(textBounds.getWidth());
    }

    @Override
    public int getIconHeight() {
        //return iconHeight;
        Graphics2D g2d = createGraphics2D();
        Font defaultFont = UIManager.getFont("Button.font");
        Font iconFont = defaultFont.deriveFont(Font.PLAIN, getCachedFontSize(defaultFont));
        g2d.setFont(iconFont);

        FontRenderContext frc = g2d.getFontRenderContext();
        Rectangle2D textBounds = iconFont.getStringBounds(text, frc);

        g2d.dispose();

        return (int) Math.ceil(textBounds.getHeight());
    }

    private float getCachedFontSize(Font defaultFont) {
        if (cachedFontSize == -1) {
            Graphics2D g2d = createGraphics2D();
            cachedFontSize = findFontSize(defaultFont, iconHeight, text, g2d);
            g2d.dispose();
        }
        return cachedFontSize;
    }

    private float findFontSize(Font defaultFont, int iconHeight, String text, Graphics2D g2d) {
        float fontSize = 1.0f;
        float step = 1.0f;

        FontRenderContext frc = g2d.getFontRenderContext();
        Rectangle2D textBounds;

        do {
            fontSize += step;
            Font iconFont = defaultFont.deriveFont(Font.PLAIN, fontSize);
            g2d.setFont(iconFont);
            textBounds = iconFont.getStringBounds(text, frc);

            if (textBounds.getHeight() > iconHeight) {
                fontSize -= step;
                step /= 2.0f;
            }
        } while (step >= 0.1f);

        return fontSize;
    }

    private Graphics2D createGraphics2D() {
        BufferedImage image = new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB);
        return image.createGraphics();
    }

    private static Pair.GenericPair<String, Integer> getCodepointString(String text, int i) {
        int codePoint = text.codePointAt(i);
        if (!Character.isValidCodePoint(codePoint)) {
            throw new IllegalStateException("Not a valid codepoint: " + text);
        }
        int charCount = Character.charCount(codePoint);

        return new Pair.GenericPair<>(text.substring(i, i + charCount), charCount);
    }
}
