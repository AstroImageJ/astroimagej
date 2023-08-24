package Astronomy.multiplot.table.util;

import javax.swing.*;
import java.awt.*;

public class Notification extends JComponent {
    private boolean isColumnFiltered;
    private boolean isRowFiltered;

    public Notification() {
        ToolTipManager.sharedInstance().registerComponent(this);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (isColumnFiltered && isRowFiltered) {
            g.setColor(Color.RED);
            g.fillRect(0, 0, getWidth() / 2, getHeight() / 2);
            g.setColor(Color.YELLOW);
            g.fillRect(getWidth() / 2, getHeight() / 2, getWidth(), getHeight());
        } else if (isColumnFiltered || isRowFiltered) {
            g.setColor(isColumnFiltered ? Color.RED : Color.YELLOW);
            g.fillRect(0, 0, getWidth(), getHeight());
        }
    }

    public void updateCol(boolean b) {
        if (b == isColumnFiltered) {
            return;
        }
        isColumnFiltered = b;
        repaint();
    }

    public void updateRow(boolean b) {
        if (b == isRowFiltered) {
            return;
        }
        isRowFiltered = b;
        repaint();
    }

    @Override
    public String getToolTipText() {
        var s = """
                <html>
                %s
                %s
                </html>
                """.formatted((isColumnFiltered ? "Columns Filtered" + (isRowFiltered ? "<br>" : "") : ""), (
                isRowFiltered ?
                        "Rows Filtered" :
                        ""));

        return !s.contains("Filtered") ? "No filters active" : s;
    }
}
