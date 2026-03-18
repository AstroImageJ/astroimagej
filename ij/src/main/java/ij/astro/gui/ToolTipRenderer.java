package ij.astro.gui;

import java.awt.Component;

import javax.swing.DefaultListCellRenderer;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JList;

public class ToolTipRenderer extends DefaultListCellRenderer {

    @Override
    public Component getListCellRendererComponent(JList list, Object value,
                                                  int index, boolean isSelected, boolean cellHasFocus) {
        JComponent component = (JComponent) super.getListCellRendererComponent(list, value, index, isSelected,
                cellHasFocus);
        String tip = null;
        if (value instanceof ImageIcon imageIcon) {
            tip = imageIcon.getDescription();
        }
        if (value instanceof ToolTipProvider ttp) {
            tip = ttp.getToolTip();
        }
        list.setToolTipText(tip);
        return component;
    }
}
