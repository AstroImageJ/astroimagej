package Astronomy.multiplot.table.util;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;

public class PaddedRenderer extends DefaultTableCellRenderer {
    final Border padding = BorderFactory.createEmptyBorder(1, 3, 1, 3);

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        super.getTableCellRendererComponent(table, value, isSelected, hasFocus,
                row, column);

        setHorizontalAlignment(CENTER);
        setBorder(BorderFactory.createCompoundBorder(getBorder(), padding));

        return this;
    }
}
