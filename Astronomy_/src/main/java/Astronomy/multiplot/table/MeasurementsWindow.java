package Astronomy.multiplot.table;

import Astronomy.multiplot.table.util.FilterHandler;
import Astronomy.multiplot.table.util.SynchronizedSelectionModel;
import astroj.MeasurementTable;
import ij.IJ;
import ij.Menus;
import ij.astro.io.prefs.Property;
import ij.astro.io.prefs.PropertyKey;
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
import java.awt.event.*;
import java.text.DecimalFormat;
import java.util.Arrays;

public class MeasurementsWindow extends JFrame {
    private final JTable jTable;
    private final JTable rowHeadings;
    private final MeasurementsTableView tableView;
    private MeasurementTable table;
    private final TableRowSorter<MeasurementsTableView> rowSorter;
    @PropertyKey(ignoreAffixes = true, value = "results.loc")
    private static final Property<Point> windowLocation = new Property<>(new Point(), MeasurementsWindow.class);
    private static final Property<Boolean> monospaced = new Property<>(false, MeasurementsWindow.class);
    private static final Property<Boolean> antialiased = new Property<>(false, MeasurementsWindow.class);
    private static final Property<Float> fontSize = new Property<>(14f, MeasurementsWindow.class);
    private FilterHandler filterWindow;

    public MeasurementsWindow(MeasurementTable table) {
        super(MeasurementTable.longerName(table.shortTitle()));
        this.table = table;

        // Ensure row numbers are not shown, the table always displays them
        table.showRowNumbers(false);

        tableView = new MeasurementsTableView();
        var firstColumnView = new FirstColumnView(tableView);
        rowHeadings = new JTable(firstColumnView);
        tableView.addTableModelListener(firstColumnView::fireTableChanged);

        windowLocation.locationSavingWindow(this, null);

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

        // Align row numbers to the center
        rowHeadings.setDefaultRenderer(Integer.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                setHorizontalAlignment(CENTER);
                if (!isSelected) {
                    setBackground(Color.LIGHT_GRAY);
                    setForeground(Color.BLACK);
                }
                return this;
            }
        });

        // Make sure table rows are copied, not the headings
        rowHeadings.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                jTable.requestFocusInWindow();
            }
        });

        // Sync row selection, make entire rows selectable
        var smp = SynchronizedSelectionModel.createPair();
        jTable.setSelectionModel(smp.first());
        rowHeadings.setSelectionModel(smp.second());
        var sm = rowHeadings.getSelectionModel();
        sm.addListSelectionListener(e -> {
            if (e instanceof SynchronizedSelectionModel.OwnedListSelectionEvent o) {
                if (o.getOwner() == SynchronizedSelectionModel.Owner.ROW_HEADING) {
                    jTable.addColumnSelectionInterval(0, jTable.getColumnCount()-1);
                    //todo fire scroll event?
                }
            }
        });

        // Double click to fit width
        jTable.getTableHeader().addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() >= 2 && SwingUtilities.isLeftMouseButton(e)) {
                    if (jTable.getTableHeader().getResizingAllowed()) {
                        SwingUtilities.invokeLater(() -> {
                            var i = jTable.getTableHeader().columnAtPoint(e.getPoint());
                            var r = jTable.getTableHeader().getHeaderRect(i);

                            // Make sure we are on the edge of the column
                            r.grow(-3, 0);
                            if (r.contains(e.getPoint())) {
                                return;
                            }

                            int midPoint = r.x + r.width / 2;
                            int columnIndex;
                            if (getComponentOrientation().isLeftToRight()) {
                                columnIndex = (e.getPoint().x < midPoint) ? i - 1 : i;
                            } else {
                                columnIndex = (e.getPoint().x < midPoint) ? i : i - 1;
                            }

                            adjustWidth(columnIndex);
                        });
                    }
                }
            }
        });

        jTable.doLayout();

        // Create a JScrollPane to add the table to
        JScrollPane scrollPane = new JScrollPane(jTable);
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);

        //todo selecting rows and dragging offscreen only scrolls the header, not the main table. The inverse works
        scrollPane.setRowHeaderView(rowHeadings);

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

        pack();

        // Adjust widths on open
        for (int c = 0; c < jTable.getColumnCount(); c++) {
            adjustWidth(c);
        }

        scrollPane.getRowHeader()
                .setPreferredSize(new Dimension(adjustWidth(rowHeadings, 0, true), 0));
        firstColumnView.addTableModelListener(e -> scrollPane.getRowHeader()
                .setPreferredSize(new Dimension(adjustWidth(rowHeadings, 0, true), 0)));
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
        update(UpdateEvent.REBUILD);
    }

    public void scrollToBottom() {
        jTable.scrollRectToVisible(jTable.getCellRect(tableView.getRowCount(), 0, true));
    }

    public void setSelection(int rowStart, int rowEnd) {
        // Remove filter so all rows are visible
        rowSorter.setRowFilter(null);

        if (rowStart > rowEnd) {
            rowEnd = rowStart;
        }
        if (rowStart < 0) {
            rowStart = 0;
        }
        if (rowEnd < 0) {
            rowEnd = 0;
        }
        if (rowStart >= jTable.getRowCount()) {
            rowStart = jTable.getRowCount() - 1;
        }
        if (rowEnd >= jTable.getRowCount()) {
            rowEnd = jTable.getRowCount() - 1;
        }

        // Accommodate tables sorted such that the interval is no longer continuous
        jTable.clearSelection();
        for (int r = rowStart; r <= rowEnd; r++) {
            var rowToSelect = jTable.convertRowIndexToView(r);
            if (rowToSelect >= 0) {
                jTable.addRowSelectionInterval(rowToSelect, rowToSelect);
            }
        }
        jTable.scrollRectToVisible(jTable.getCellRect(jTable.convertRowIndexToView(rowStart), 0, true));
        jTable.setColumnSelectionInterval(0, jTable.getColumnCount()-1);
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


    public void update(UpdateEvent event) {
        if (event == UpdateEvent.REBUILD || event == UpdateEvent.DATA_CHANGED) {
            update(event, 0, 0);
        } else {
            throw new IllegalArgumentException("Given even requires coordinates");
        }
    }

    public void update(UpdateEvent event, int i1, int i2) {
        switch (event) {
            case REBUILD -> tableView.fireTableStructureChanged();
            case DATA_CHANGED -> tableView.fireTableDataChanged();
            case ROW_DELETED -> tableView.fireTableRowsDeleted(i1, i2);
            case ROW_INSERTED -> tableView.fireTableRowsInserted(i1, i2);
            case ROW_UPDATED -> tableView.fireTableRowsUpdated(i1, i2);
            case CELL_UPDATED -> {
                adjustWidthOnRow(i1, i2);
                tableView.fireTableCellUpdated(i1, i2);
            }
        }
    }

    private void adjustWidthOnRow(int rowIndex, int columnIndex) {
        if (rowIndex >= 10 || columnIndex < 0 || rowIndex < 0 || rowIndex >= jTable.getRowCount()) {
            return;
        }

        TableColumn c = jTable.getColumnModel().getColumn(columnIndex);

        if (c == null) {
            return;
        }

        // Fit the header, adapted from TableColumn#sizeWidthToFit
        var headerRenderer = c.getHeaderRenderer();
        if (headerRenderer == null) {
            headerRenderer = jTable.getTableHeader().getDefaultRenderer();
        }
        Component headerBox = headerRenderer.getTableCellRendererComponent(null,
                c.getHeaderValue(), false, false, 0, 0);

        // Find best width based on first 10 rows if they exist
        var width = headerBox.getPreferredSize().width;
        var ren = jTable.getCellRenderer(rowIndex, columnIndex);
        var comp = jTable.prepareRenderer(ren, rowIndex, columnIndex);
        width = Math.max(width, comp.getPreferredSize().width + 2);

        c.setPreferredWidth(width);
    }

    private void adjustWidth(int columnIndex) {
        adjustWidth(jTable, columnIndex, false);
    }

    private int adjustWidth(JTable table, int columnIndex, boolean reverse) {
        if (columnIndex < 0) {
            return 0;
        }

        TableColumn c = table.getColumnModel().getColumn(columnIndex);

        if (c == null) {
            return 0;
        }

        // Fit the header, adapted from TableColumn#sizeWidthToFit
        var headerRenderer = c.getHeaderRenderer();
        if (headerRenderer == null) {
            headerRenderer = table.getTableHeader().getDefaultRenderer();
        }
        Component headerBox = headerRenderer.getTableCellRendererComponent(null,
                c.getHeaderValue(), false, false, 0, 0);

        // Find best width based on first 10 rows if they exist
        var width = headerBox.getPreferredSize().width;
        if (reverse) {
            for (int row = table.getRowCount()-1; row > Math.max(table.getRowCount() - 10, 0); row--) {
                var ren = table.getCellRenderer(row, columnIndex);
                var comp = table.prepareRenderer(ren, row, columnIndex);
                width = Math.max(width, comp.getPreferredSize().width + 2);
            }
        } else {
            for (int row = 0; row < Math.min(table.getRowCount(), 10); row++) {
                var ren = table.getCellRenderer(row, columnIndex);
                var comp = table.prepareRenderer(ren, row, columnIndex);
                width = Math.max(width, comp.getPreferredSize().width + 2);
            }
        }

        c.setPreferredWidth(width);
        return width;
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
        i = new MenuItem("Clear");
        i.addActionListener($ -> {
            // Entire row selected
            if (jTable.getSelectedColumns().length == jTable.getColumnCount()) {
                var idx = jTable.getSelectedRows();
                // Needs to be sorted as we are removing rows in descending order
                idx = Arrays.stream(idx).map(jTable::convertRowIndexToModel).sorted().toArray();
                for (int i1 = idx.length - 1; i1 >= 0; i1--) {
                    table.deleteRow(idx[i1]);
                }
                if (idx.length > 0) {
                    table.updateRelatedPlot();
                }
            }
        });
        m.add(i);
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
            if (filterWindow == null) {
                filterWindow = new FilterHandler(this);
            }
            filterWindow.setVisible(true);
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
                rowHeadings.setFont(jTable.getFont());
            }
        });
        m.add(i);
        i = new MenuItem("Make Text Larger");
        i.addActionListener($ -> {
            var f = fontSize.get() + 2;
            fontSize.set(f);
            jTable.setFont(jTable.getFont().deriveFont(fontSize.get()));
            rowHeadings.setFont(jTable.getFont());
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
            rowHeadings.setFont(jTable.getFont());
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
            rowHeadings.setFont(jTable.getFont());
        });
        m.add(i);
        mb.add(m);

        m = new Menu("Results");
        i = new MenuItem("Clear All");
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

    public enum UpdateEvent {
        REBUILD,
        DATA_CHANGED,
        ROW_DELETED,
        ROW_INSERTED,
        ROW_UPDATED,
        CELL_UPDATED,
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

    public static class FirstColumnView extends AbstractTableModel {
        private final MeasurementsTableView view;

        public FirstColumnView(MeasurementsTableView view) {
            this.view = view;
        }

        @Override
        public int getRowCount() {
            return view.getRowCount();
        }

        @Override
        public int getColumnCount() {
            return 1;
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            return rowIndex + 1;
        }

        @Override
        public Class<?> getColumnClass(int columnIndex) {
            return Integer.class;
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
