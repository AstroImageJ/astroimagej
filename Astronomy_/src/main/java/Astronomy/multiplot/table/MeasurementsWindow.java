package Astronomy.multiplot.table;

import astroj.MeasurementTable;
import ij.IJ;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.text.DecimalFormat;

public class MeasurementsWindow extends JFrame {
    private final JTable jTable;
    private final MeasurementsTableView tableView;
    private MeasurementTable table;
    public MeasurementsWindow(MeasurementTable table) {
        super(MeasurementTable.longerName(table.shortTitle()));
        this.table = table;
        tableView = new MeasurementsTableView();

        // Create a JTable instance
        jTable = new JTable(tableView/*, */);
        jTable.setDefaultRenderer(Double.class, new DoubleCellRenderer(6));
        jTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);

        jTable.setColumnSelectionAllowed(true);
        jTable.setRowSelectionAllowed(true);

        jTable.setFont(jTable.getFont().deriveFont(14f));
        jTable.getTableHeader().setFont(jTable.getTableHeader().getFont().deriveFont(14f));

        jTable.getTableHeader().setReorderingAllowed(false);
        jTable.getTableHeader().setResizingAllowed(true);

        jTable.doLayout();

        // Create a JScrollPane to add the table to
        JScrollPane scrollPane = new JScrollPane(jTable);
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);

        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        add(scrollPane);

        // Set up key bindings for F3
        InputMap inputMap = jTable.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap actionMap = jTable.getActionMap();
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_F3, 0), "gotoAction");
        actionMap.put("gotoAction", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                handleGotoEvent();
            }
        });
    }

    @Override
    public void setVisible(boolean b) {
        if (!isVisible() && b) {
            pack();
        }
        super.setVisible(b);
    }

    public MeasurementTable getTable() {
        return table;
    }

    public void setTable(MeasurementTable table) {
        this.table = table;
        update(true);
    }

    public void scrollToBottom() {
        jTable.scrollRectToVisible(jTable.getCellRect(tableView.getRowCount(), 0, true));
    }

    public void setSelection(int row1, int row2) {
        jTable.setRowSelectionInterval(row1, row2);
        jTable.setColumnSelectionInterval(0, jTable.getColumnCount()-1);
        jTable.scrollRectToVisible(jTable.getCellRect(row1, 0, true));
    }

    public int getLineCount() {
        return jTable.getRowCount();
    }

    public int getSelectionStart() {
        return jTable.getSelectedRow();
    }

    public int getSelectionEnd() {
        var a = jTable.getSelectedRows();
        return a.length > 0 ? a[a.length-1] : -1;
    }

    public void update() {
        update(false);
    }

    public void update(boolean columnChanged) {
        tableView.fireTableDataChanged();
        if (columnChanged) {
            tableView.fireTableStructureChanged();
        }
    }

    private void handleGotoEvent() {
        // Create the dropdown menu with column headings
        JComboBox<String> columnDropdown = new JComboBox<>(table.getHeadings());

        // Display the dialog with the dropdown menu and input field for row
        JPanel panel = new JPanel(new GridLayout(2, 2));
        panel.add(new JLabel("Column:"));
        panel.add(columnDropdown);
        panel.add(new JLabel("Row:"));
        JTextField rowField = new JTextField("0");
        panel.add(rowField);

        int result = JOptionPane.showConfirmDialog(MeasurementsWindow.this, panel, "Go To Cell", JOptionPane.OK_CANCEL_OPTION);
        if (result == JOptionPane.OK_OPTION) {
            // Retrieve the selected column index and row input
            int column = columnDropdown.getSelectedIndex();
            String rowInput = rowField.getText();

            if (!rowInput.isEmpty()) {
                int row = Integer.parseInt(rowInput.trim());

                if (row >= 0 && row < table.size() && column >= 0 && column < table.getLastColumn()) {
                    // Scroll to the specified cell
                    Rectangle cellRect = jTable.getCellRect(row, column, true);
                    jTable.scrollRectToVisible(cellRect);

                    jTable.setRowSelectionInterval(row, row);
                    jTable.setColumnSelectionInterval(column, column);
                } else {
                    IJ.beep();
                    JOptionPane.showMessageDialog(null, "Invalid cell coordinates.");
                }
            }
        }
    }

    private static class DoubleCellRenderer extends DefaultTableCellRenderer {
        private final DecimalFormat decimalFormat;

        public DoubleCellRenderer(int decimalPlaces) {
            super();
            decimalFormat = new DecimalFormat("#." + "#".repeat(decimalPlaces));
        }

        @Override
        protected void setValue(Object value) {
            if (value instanceof Double) {
                value = decimalFormat.format(value);
            }
            super.setValue(value);
        }
    }

    /**
     * 1-indexed
     */
    private class MeasurementsTableView extends AbstractTableModel {
        public MeasurementsTableView() {
            super();
        }

        @Override
        public int getRowCount() {
            return table.size();
        }

        @Override
        public int getColumnCount() {
            return table.getLastColumn() + (table.showRowNumbers() ? 2 : 1);
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            if (isLabelCol(columnIndex)) {
                return table.getLabel(rowIndex);
            }
            if (isRowNumCol(columnIndex)) {
                return rowIndex+table.getBaseRowNumber();
            }
            return table.getValue(table.getColumnHeading(offsetCol(columnIndex)), rowIndex);
        }

        @Override
        public String getColumnName(int column) {
            if (isLabelCol(column)) {
                return "Labels";
            }
            if (isRowNumCol(column)) {
                return "";
            }
            return table.getColumnHeading(offsetCol(column));
        }

        @Override
        public Class<?> getColumnClass(int columnIndex) {
            if (isLabelCol(columnIndex)) {
                return String.class;
            }
            if (isRowNumCol(columnIndex)) {
                return Integer.class;
            }
            return Double.class;
        }

        public boolean isLabelCol(int c) {
            return offsetCol(c) == -1;
        }

        public boolean isRowNumCol(int c) {
            return offsetCol(c) == -2;
        }

        public int offsetCol(int c) {
            if (table.showRowNumbers()) {
                return c-2;
            }

            return --c;
        }
    }
}
