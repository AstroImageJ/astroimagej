package Astronomy.multiplot.table;

import astroj.MeasurementTable;
import ij.IJ;
import ij.Menus;
import ij.astro.io.prefs.Property;
import ij.astro.logging.AIJLogger;
import ij.gui.GenericDialog;
import ij.gui.PlotContentsDialog;
import ij.measure.ResultsTableMacros;
import ij.plugin.Distribution;
import ij.plugin.filter.Analyzer;
import ij.util.Java2;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.text.DecimalFormat;

public class MeasurementsWindow extends JFrame {
    private final JTable jTable;
    private final MeasurementsTableView tableView;
    private MeasurementTable table;
    private final TableRowSorter<MeasurementsTableView> rowSorter;
    private static final Property<Boolean> monospaced = new Property<>(false, MeasurementsWindow.class);
    private static final Property<Boolean> antialiased = new Property<>(false, MeasurementsWindow.class);
    private static final Property<Float> fontSize = new Property<>(14f, MeasurementsWindow.class);

    public MeasurementsWindow(MeasurementTable table) {
        super(MeasurementTable.longerName(table.shortTitle()));
        this.table = table;
        tableView = new MeasurementsTableView();

        // Create a JTable instance
        jTable = new JTable(tableView/*, */);
        rowSorter = new TableRowSorter<>(tableView);
        jTable.setRowSorter(rowSorter);
        jTable.setDefaultRenderer(Double.class, new DoubleCellRenderer(6));
        jTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);

        jTable.setColumnSelectionAllowed(true);
        jTable.setRowSelectionAllowed(true);

        jTable.setFont(jTable.getFont().deriveFont(fontSize.get()));
        jTable.getTableHeader().setFont(jTable.getTableHeader().getFont().deriveFont(fontSize.get()));

        jTable.getTableHeader().setReorderingAllowed(false);
        jTable.getTableHeader().setResizingAllowed(true);

        jTable.doLayout();

        // Create a JScrollPane to add the table to
        JScrollPane scrollPane = new JScrollPane(jTable);
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);

        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        addMenuBar();
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

    public TableRowSorter<MeasurementsTableView> getRowSorter() {
        return rowSorter;
    }

    public void setTable(MeasurementTable table) {
        this.table = table;
        update(true);
    }

    public void scrollToBottom() {
        jTable.scrollRectToVisible(jTable.getCellRect(tableView.getRowCount(), 0, true));
    }

    public void setSelection(int row1, int row2) {
        rowSorter.setRowFilter(null);
        row1 = jTable.convertRowIndexToView(row1);
        row2 = jTable.convertRowIndexToView(row2);
        jTable.setRowSelectionInterval(row1, row2);
        jTable.setColumnSelectionInterval(0, jTable.getColumnCount()-1);
        jTable.scrollRectToVisible(jTable.getCellRect(row1, 0, true));
    }

    public int getLineCount() {
        return jTable.getRowCount();
    }

    public int getSelectionStart() {
        return jTable.convertRowIndexToModel(jTable.getSelectedRow());
    }

    public int getSelectionEnd() {
        var a = jTable.getSelectedRows();
        return a.length > 0 ? jTable.convertRowIndexToModel(a[a.length-1]) : -1;
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

    private void addMenuBar() {
        var mb = new MenuBar();
        if (Menus.getFontSize()!=0) {
            mb.setFont(Menus.getFont());
        }

        Menu m = new Menu("File");
        var i = new MenuItem("Save As...", new MenuShortcut(KeyEvent.VK_S));
        i.addActionListener($ -> table.save(null));
        m.add(i);
        i = new MenuItem("Rename...");
        i.addActionListener($ -> {
            GenericDialog gd = new GenericDialog("Rename", this);
            gd.addStringField("Title:", table.shortTitle(), 40);
            gd.showDialog();
            if (gd.wasCanceled())
                return;
            table.rename(gd.getNextString());
        });
        m.add(i);
        i = new MenuItem("Duplicate...");
        i.addActionListener($ -> {
            var n = table.clone();
            String title2 = IJ.getString("Title:", getTitle()+"_2");
            if (!title2.isEmpty()) {
                if (title2.equals("Measurements")) title2 = "Measurements in Measurements2";
                n.setShortName(title2);
                n.show();
            }
        });
        m.add(i);
        mb.add(m);

        m = new Menu("Edit");
        i = new MenuItem("Copy", new MenuShortcut(KeyEvent.VK_C));
        i.addActionListener($ -> {
            Action copyAction = jTable.getActionMap().get("copy");
            if (copyAction != null) {
                copyAction.actionPerformed(new ActionEvent(jTable, 0, "copy"));
            }
        });
        m.add(i);
        /*i = new MenuItem("Clear");
        i.addActionListener($ -> {
            //todo disabled as it deletes rows, can't select them currently
        });
        m.add(i);*/
        i = new MenuItem("Select All", new MenuShortcut(KeyEvent.VK_A));
        i.addActionListener($ -> jTable.selectAll());
        m.add(i);
        m.addSeparator();
        i = new MenuItem("Find...", new MenuShortcut(KeyEvent.VK_F));
        i.addActionListener($ -> {
            //todo imp
        });
        m.add(i);
        i = new MenuItem("Filter...", new MenuShortcut(KeyEvent.VK_G));
        i.addActionListener($ -> {
            //todo impl
            //rowSorter.setRowFilter(RowFilter.numberFilter());
            //new FilterWindow(this).setVisible(true);
        });
        m.add(i);
        mb.add(m);

        m = new Menu("Font");
        i = new MenuItem("Make Text Smaller");
        i.addActionListener($ -> {
            var f = fontSize.get() - 2;
            if (f > 1) {
                fontSize.set(f);
                jTable.setFont(jTable.getFont().deriveFont(fontSize.get()));
            }
        });
        m.add(i);
        i = new MenuItem("Make Text Larger");
        i.addActionListener($ -> {
            var f = fontSize.get() + 2;
            fontSize.set(f);
            jTable.setFont(jTable.getFont().deriveFont(fontSize.get()));
        });
        m.add(i);
        m.addSeparator();
        i = new CheckboxMenuItem("Monospaced", monospaced.get());
        MenuItem finalI = i;
        i.addActionListener($ -> {
            if (((CheckboxMenuItem) finalI).getState()) {
                monospaced.set(true);
                jTable.setFont(new Font("Monospaced", Font.PLAIN, fontSize.get().intValue()));
            } else {
                monospaced.set(false);
                jTable.setFont(new Font("SansSerif", Font.PLAIN, fontSize.get().intValue()));
            }
        });
        m.add(i);
        i = new CheckboxMenuItem("Antialiasing");
        MenuItem finalI1 = i;
        i.addActionListener($ -> {
            antialiased.set(((CheckboxMenuItem) finalI1).getState());
            Java2.setAntialiasedText(getGraphics(), antialiased.get());
        });
        m.add(i);
        m.addSeparator();
        i = new MenuItem("Default Settings");
        i.addActionListener($ -> {
            monospaced.set(false);
            fontSize.set(14f);
            antialiased.set(false);
            Java2.setAntialiasedText(getGraphics(), antialiased.get());
            jTable.setFont(new Font("SansSerif", Font.PLAIN, fontSize.get().intValue()));
        });
        m.add(i);
        mb.add(m);

        m = new Menu("Results");
        i = new MenuItem("Clear Results");
        i.addActionListener($ -> table.clearTable());
        m.add(i);
        i = new MenuItem("Summarize");
        i.addActionListener($ -> {
            Analyzer analyzer = new Analyzer(null, table);
            analyzer.summarize();
        });
        m.add(i);
        i = new MenuItem("Distribution...");
        i.addActionListener($ -> new Distribution().run(table));
        m.add(i);
        i = new MenuItem("Set Measurements...");
        i.addActionListener($ -> IJ.runPlugIn("Astronomy.Set_Aperture", ""));
        m.add(i);
        i = new MenuItem("Apply Macro...");
        i.addActionListener($ -> new ResultsTableMacros(table));
        m.add(i);
        i = new MenuItem("Plot...");
        i.addActionListener($ -> new PlotContentsDialog(getTitle(), table).showDialog(getParent() instanceof Frame ? (Frame)getParent() : null));
        m.add(i);
        i = new MenuItem("Options...");
        i.addActionListener($ -> IJ.doCommand("Input/Output..."));
        m.add(i);
        mb.add(m);

        setMenuBar(mb);
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
    public class MeasurementsTableView extends AbstractTableModel {
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
