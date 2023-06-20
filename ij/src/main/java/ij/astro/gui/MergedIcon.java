package ij.astro.gui;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;


// Adapted rom https://stackoverflow.com/a/17088768
// Licensed under CC BY-SA 3.0
public class MergedIcon implements Icon {

    private final int m_iconWidth;
    private final int m_iconHeight;
    private final BufferedImage m_buffer;

    public MergedIcon(Icon backgroundImage, Icon topImage) {
        this(backgroundImage, topImage, 0, 0);
    }

    public MergedIcon(Image backgroundImage, Image topImage) {
        this(backgroundImage, topImage, 0, 0);
    }

    public MergedIcon(Icon backgroundImage, Icon topImage, int offsetX, int offsetY) {
        this(iconToImage(backgroundImage), iconToImage(topImage), offsetX, offsetY);
    }

    public MergedIcon(Image backgroundImage, Image topImage, int offsetX, int offsetY) {
        this(backgroundImage, topImage, offsetX, offsetY, false);
    }

    public MergedIcon(Icon backgroundImage, Icon topImage, int offsetX, int offsetY, boolean flipOffset) {
        this(iconToImage(backgroundImage), iconToImage(topImage), offsetX, offsetY, flipOffset);
    }

    public MergedIcon(Image backgroundImage, Image topImage, int offsetX, int offsetY, boolean flipOffset) {
        if (topImage != null) {
            m_iconWidth = Math.max(backgroundImage.getWidth(null), topImage.getWidth(null));
            m_iconHeight = Math.max(backgroundImage.getHeight(null), topImage.getHeight(null));
        } else {
            m_iconWidth = backgroundImage.getWidth(null);
            m_iconHeight = backgroundImage.getHeight(null);
        }

        m_buffer = new BufferedImage(m_iconWidth, m_iconHeight, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = (Graphics2D) m_buffer.getGraphics();
        g.drawImage(backgroundImage, flipOffset ? - offsetX: 0, flipOffset ? - offsetY : 0, null);
        if (topImage != null) {
            g.drawImage(topImage, flipOffset ? 0 : offsetX, flipOffset ? 0 : offsetY, null);
        }
    }

    @Override
    public int getIconHeight() {
        return m_iconHeight;
    }

    @Override
    public int getIconWidth() {
        return m_iconWidth;
    }

    @Override
    public void paintIcon(Component c, Graphics g, int x, int y) {
        g.drawImage(m_buffer, x, y, null);
    }

    public static Image iconToImage(Icon icon) {
        if (icon == null)
            return null;
        if (icon instanceof ImageIcon)
            return ((ImageIcon) icon).getImage();

        return iconToBufferedImage(icon);
    }

    public static BufferedImage iconToBufferedImage(Icon icon) {
        if (icon == null)
            return null;

        BufferedImage image = new BufferedImage(icon.getIconWidth(), icon.getIconHeight(), BufferedImage.TYPE_INT_ARGB);
        icon.paintIcon(null, image.getGraphics(), 0, 0);
        return image;
    }
}