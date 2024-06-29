package ij.astro.gui;

import javax.swing.*;
import java.awt.*;

public class ToolTipRenderer extends DefaultListCellRenderer {

    @Override
    public Component getListCellRendererComponent(JList list, Object value,
                                                  int index, boolean isSelected, boolean cellHasFocus) {
        JComponent component = (JComponent) super.getListCellRendererComponent(list, value, index, isSelected,
                cellHasFocus);
        String tip = null;
        if (value instanceof ToolTipProvider ttp) {
            tip = ttp.getToolTip();
        }
        list.setToolTipText(tip);
        return component;
    }
}
