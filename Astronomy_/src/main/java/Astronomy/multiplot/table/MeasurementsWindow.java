package Astronomy.multiplot.table;

import Astronomy.Aperture_;
import Astronomy.multiplot.table.util.*;
import astroj.MeasurementTable;
import ij.IJ;
import ij.Menus;
import ij.Prefs;
import ij.WindowManager;
import ij.astro.accessors.ITableWindow;
import ij.astro.gui.GenericSwingDialog;
import ij.astro.io.prefs.Property;
import ij.astro.io.prefs.PropertyKey;
import ij.astro.logging.AIJLogger;
import ij.astro.util.UIHelper;
import ij.gui.GenericDialog;
import ij.gui.PlotContentsDialog;
import ij.measure.ResultsTableMacros;
import ij.plugin.Distribution;
import ij.plugin.FITS_Writer;
import ij.plugin.filter.Analyzer;
import ij.util.Java2;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.event.RowSorterEvent;
import javax.swing.event.TableModelEvent;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.awt.event.*;
import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.IntStream;

import static ij.measure.ResultsTable.COLUMN_IN_USE;
import static ij.measure.ResultsTable.COLUMN_NOT_FOUND;

public class MeasurementsWindow extends JFrame implements ITableWindow {
    private final JTable jTable;
    private final JTable rowHeadings;
    private final MeasurementsTableView tableView;
    private final JScrollPane scrollPane;
    private MeasurementTable table;
    private final TableRowSorter<MeasurementsTableView> rowSorter;
    @PropertyKey(ignoreAffixes = true, value = "results.loc")
    private static final Property<Point> windowLocation = new Property<>(new Point(), MeasurementsWindow.class);
    private static final Property<Dimension> windowSize = new Property<>(new Dimension(), MeasurementsWindow.class);
    private static final Property<Boolean> monospaced = new Property<>(false, MeasurementsWindow.class);
    private static final Property<Boolean> antialiased = new Property<>(false, MeasurementsWindow.class);
    private static final Property<Boolean> showSatWarning = new Property<>(true, MeasurementsWindow.class);
    private static final Property<Float> fontSize = new Property<>(14f, MeasurementsWindow.class);
    private static final Property<Double> newColDefaultValue = new Property<>(0d, MeasurementsWindow.class);
    private FilterHandler filterWindow;
    private FindHandler findWindow;
    private final Notification notification = new Notification();
    private RowFilter<? super MeasurementsWindow.MeasurementsTableView, ? super Integer> linearityFilter;

    public MeasurementsWindow(MeasurementTable table) {
        super(MeasurementTable.longerName(table.shortTitle()));
        this.table = table;

        UIHelper.setLookAndFeel();

        // Ensure row numbers are not shown, the table always displays them
        table.showRowNumbers(false);

        tableView = new MeasurementsTableView();
        var firstColumnView = new FirstColumnView(tableView);
        rowHeadings = new JTable(firstColumnView);
        tableView.addTableModelListener(firstColumnView::fireTableChanged);

        windowLocation.locationSavingWindow(this, null);

        // Create a JTable instance
        var hcm = new HiddenColumnModel();
        hcm.addFilterListener(notification::updateCol);
        jTable = new JTable(tableView, hcm);
        jTable.setAutoCreateColumnsFromModel(true);
        rowSorter = new TriSortStateMeasurementsSorter(tableView);
        rowSorter.addRowSorterListener(s -> {
            if (s.getType() == RowSorterEvent.Type.SORTED) {
                notification.updateRow(jTable.getRowCount() < table.size());
            }
        });
        jTable.setRowSorter(rowSorter);
        jTable.setDefaultRenderer(Double.class, new DoubleCellRenderer(6));
        jTable.setDefaultRenderer(String.class, new PaddedRenderer());
        jTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);

        jTable.setColumnSelectionAllowed(true);
        jTable.setRowSelectionAllowed(true);

        jTable.setFont(jTable.getFont().deriveFont(fontSize.get()));
        jTable.getTableHeader().setFont(jTable.getTableHeader().getFont().deriveFont(fontSize.get()));

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

        rowHeadings.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                // Right-click menu
                if (SwingUtilities.isRightMouseButton(e)) {
                    var rowIndex = rowHeadings.rowAtPoint(e.getPoint());
                    if (rowIndex < 0 || rowIndex > table.getCounter()) {
                        return;
                    }

                    var popup = new JPopupMenu();
                    var pItem = new JMenuItem("Insert Row Above");
                    pItem.addActionListener($ -> {
                        table.insertRow(rowIndex);
                    });
                    popup.add(pItem);

                    pItem = new JMenuItem("Insert Row Below");
                    pItem.addActionListener($ -> {
                        table.insertRow(rowIndex+1);
                    });
                    popup.add(pItem);

                    popup.show(rowHeadings, e.getX(), e.getY());
                }
            }
        });

        jTable.getTableHeader().setToolTipText("""
                <html>
                Left-click to toggle sort view based on column.<br>
                Right-click to open column menu.<br>
                Middle-click or Right-click+alt/opt to select entire column.
                </html>
                """);

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

                if (SwingUtilities.isRightMouseButton(e)) {
                    var i = jTable.getTableHeader().columnAtPoint(e.getPoint());
                    if (i < 0) {
                        return;
                    }

                    var c = jTable.getColumnModel().getColumn(i);

                    var popup = new JPopupMenu("Column Options");

                    var item = new JMenuItem("Sort table based on column values");
                    item.setToolTipText("Run again to flip order");
                    item.addActionListener($ -> {
                        if (((String) c.getIdentifier()).equals("Label")) {
                            IJ.error("Cannot sort on Label column");
                            return;
                        }
                        table.sort(((String) c.getIdentifier()));
                        jTable.getRowSorter().setSortKeys(null);
                        table.updateRelatedPlot();
                    });
                    popup.add(item);

                    item = new JMenuItem("Remove all rows containing non-finite values in this column");
                    item.setToolTipText("See Filter for more filtering options");
                    item.addActionListener($ -> {
                        if (((String) c.getIdentifier()).equals("Label")) {
                            IJ.error("Cannot filter on Label column");
                            return;
                        }

                        table.setLock(true);

                        var cName = (String) c.getIdentifier();
                        var cIdx = table.getColumnIndex(cName);

                        if (cIdx == COLUMN_NOT_FOUND) {
                            IJ.error("Could not find column with name " + cName);
                            table.setLock(false);
                            return;
                        }

                        // Needs to be sorted as we are removing rows in descending order
                        var idx = IntStream.range(0, table.size())
                                .filter(row -> !Double.isFinite(table.getValueAsDouble(cIdx, row)))
                                .sorted().toArray();
                        table.deleteRows(idx);
                        table.setLock(false);
                        if (idx.length > 0) {
                            table.updateRelatedPlot();
                            IJ.showMessage("Row trimming", "Removed %s rows.".formatted(idx.length));
                        }
                    });
                    popup.add(item);

                    item = new JMenuItem("Apply mathematical operation to this column");
                    item.addActionListener($ -> {
                        if (((String) c.getIdentifier()).equals("Label")) {
                            IJ.error("Cannot perform operations on Labels");
                            return;
                        }

                        OperationsHandler.dialog(MeasurementsWindow.this, (String) c.getIdentifier());
                    });
                    popup.add(item);

                    item = new JMenuItem("Select Column");
                    item.addActionListener($ -> {
                        jTable.setColumnSelectionInterval(i, i);
                        jTable.setRowSelectionInterval(0, jTable.getRowCount()-1);
                    });
                    popup.add(item);

                    item = new JMenuItem("Delete column");
                    item.addActionListener($ -> {
                        if (IJ.showMessageWithCancel("Column Deletion", "Delete column '%s'?"
                                .formatted(c.getIdentifier()))) {
                            table.setLock(true);
                            table.deleteColumn((String) c.getIdentifier());
                            table.setLock(false);
                        }
                    });
                    popup.add(item);

                    item = new JMenuItem("Add column");
                    item.addActionListener($ -> {
                        var d = new GenericSwingDialog("Add Column", MeasurementsWindow.this);
                        var t = new JTextField(10);
                        d.setOverridePosition(true);
                        d.addGenericComponent(t);
                        d.addToSameRow();
                        d.addUnboundedNumericField("Initial Value", newColDefaultValue.get(), 1, 7, null, newColDefaultValue::set);

                        d.setOverridePosition(false);
                        d.addMessage("The column will be added to the end of this table");

                        d.centerDialog(true);
                        d.enableYesNoCancel();
                        d.showDialog();

                        if (d.wasOKed()) {
                            String heading = t.getText().trim();
                            var column = getTable().getFreeColumn(heading);
                            if (column == COLUMN_IN_USE) {
                                IJ.error("Column already exists");
                            }

                            var col = getTable().bulkGetColumnAsDoubles(column);
                            Arrays.fill(col, newColDefaultValue.get());
                            getTable().bulkSetColumnAsDoubles(heading, col);
                        }
                    });
                    popup.add(item);

                    popup.show(e.getComponent(), e.getX(), e.getY());
                    return;
                }

                if (SwingUtilities.isMiddleMouseButton(e) || (SwingUtilities.isRightMouseButton(e) && e.isAltDown())) {
                    var i = jTable.getTableHeader().columnAtPoint(e.getPoint());
                    if (i < 0) {
                        return;
                    }

                    jTable.setColumnSelectionInterval(i, i);
                    jTable.setRowSelectionInterval(0, jTable.getRowCount()-1);
                }
            }
        });
        jTable.getTableHeader().addMouseMotionListener(new MouseMotionAdapter() {
            public void mouseDragged(MouseEvent e1) {
                Rectangle r = new Rectangle(e1.getX(), e1.getY(), 1, 1);
                jTable.scrollRectToVisible(r);
            }
        });

        jTable.doLayout();

        // Create a JScrollPane to add the table to
        scrollPane = new JScrollPane(jTable);
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);

        //todo selecting rows and dragging offscreen only scrolls the header, not the main table. The inverse works
        scrollPane.setRowHeaderView(rowHeadings);
        scrollPane.setCorner(ScrollPaneConstants.UPPER_LEFT_CORNER, notification);

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

        setIconImage(UIHelper.createImage("Astronomy/images/icons/table/table.png"));

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                WindowManager.removeWindow(MeasurementsWindow.this);
            }

            @Override
            public void windowClosed(WindowEvent e) {
                WindowManager.removeWindow(MeasurementsWindow.this);
            }
        });
        WindowManager.addWindow(this);
    }

    @Override
    public void setVisible(boolean b) {
        if (!isVisible() && b) {
            pack();
            windowSize.sizeSavingWindow(this);
        }

        // Don't focus the window again, its already showing
        if (isVisible() && b) {
            return;
        }

        super.setVisible(b);
    }

    @Override
    public MeasurementTable getTable() {
        return table;
    }

    public JTable getJTable() {
        return jTable;
    }

    @Override
    public void rename(String name) {
        table.rename(name);
    }

    @Override
    public void close() {
        setVisible(false);
        dispose();
    }

    public TableRowSorter<MeasurementsTableView> getRowSorter() {
        return rowSorter;
    }

    public void setTable(MeasurementTable table) {
        this.table = table;
        update(UpdateEvent.REBUILD);
    }

    public void scrollToBottom() {
        if (SwingUtilities.isEventDispatchThread()) {
            var cellRect = jTable.getCellRect(jTable.getRowCount(), 0, true);
            var visibleRect = jTable.getVisibleRect();
            cellRect.x = visibleRect.x;
            jTable.scrollRectToVisible(cellRect);
        } else {
            SwingUtilities.invokeLater(() -> {
                var cellRect = jTable.getCellRect(jTable.getRowCount(), 0, true);
                var visibleRect = jTable.getVisibleRect();
                cellRect.x = visibleRect.x;
                jTable.scrollRectToVisible(cellRect);
            });
        }
    }

    public void setSelection(int rowStart1, int rowEnd1) {
        SwingUtilities.invokeLater(() -> {
            var rowEnd = rowEnd1;
            var rowStart = rowStart1;
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

            // Scroll vertically only
            var cellRect = jTable.getCellRect(jTable.convertRowIndexToView(rowStart), 0, true);
            var visibleRect = jTable.getVisibleRect();
            cellRect.x = visibleRect.x;
            jTable.scrollRectToVisible(cellRect);
            jTable.setColumnSelectionInterval(0, jTable.getColumnCount()-1);
        });
    }

    public int getLineCount() {
        return jTable.getRowCount();
    }

    public int getSelectionStart() {
        var i = jTable.getSelectedRow();
        if (i > -1) {
            return jTable.convertRowIndexToModel(i);
        }
        return -1;
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
            case REBUILD -> {
                tableView.fireTableStructureChanged();
                jTable.clearSelection();

                // Enabling this causes the model to rebuild
                jTable.setAutoCreateColumnsFromModel(true);
                jTable.setAutoCreateColumnsFromModel(false);

                // Update width
                for (int i = 0; i < jTable.getColumnCount(); i++) {
                    adjustWidth(i);
                }

                // Refilter
                if (jTable.getColumnModel() instanceof HiddenColumnModel hcm) {
                    // This is a terrible hack
                    SwingUtilities.invokeLater(hcm::refilter);
                }
            }
            case DATA_CHANGED -> {
                tableView.fireTableDataChanged();
                for (int i = 0; i < jTable.getColumnCount(); i++) {
                    adjustWidthOnRow(Math.max(tableView.getRowCount()-1, 0), i);
                }
            }
            case ROW_DELETED -> tableView.fireTableRowsDeleted(i1, i2);
            case ROW_INSERTED -> tableView.fireTableRowsInserted(i1, i2);
            case ROW_UPDATED -> tableView.fireTableRowsUpdated(i1, i2);
            case CELL_UPDATED -> {
                tableView.fireTableCellUpdated(i1, i2);
                adjustWidthOnRow(i1, i2);
            }
            case COL_ADDED -> {
                // Disable automatic column handling
                jTable.setAutoCreateColumnsFromModel(false);

                // manually add new column
                var model = jTable.getColumnModel();
                var newColumn = new TableColumn(i1+tableView.offsetForLabel());
                newColumn.setHeaderValue(table.getColumnHeading(i1));
                model.addColumn(newColumn);

                /*tableView.fireTableChanged(new TableModelEvent(tableView, TableModelEvent.HEADER_ROW,
                        TableModelEvent.HEADER_ROW, i1, TableModelEvent.INSERT));*/
                if (jTable.getColumnModel() instanceof HiddenColumnModel hcm) {
                    // This is a terrible hack
                    SwingUtilities.invokeLater(hcm::refilter);
                }
                adjustWidth(jTable.convertColumnIndexToView(i1));
            }
            case COL_RENAMED -> tableView.fireTableRowsUpdated(TableModelEvent.HEADER_ROW, TableModelEvent.HEADER_ROW);
        }
    }

    private void adjustWidthOnRow(int rowIndex, int columnIndex) {
        if (rowIndex >= 10 || columnIndex < 0 || rowIndex >= jTable.getRowCount()) {
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
                c.getHeaderValue(), false, false, -1, 0);

        // Find best width based on first 10 rows if they exist
        var width = headerBox.getPreferredSize().width;
        width = Math.max(width, headerBox.getWidth());
        if (rowIndex >= 0) {
            var ren = jTable.getCellRenderer(rowIndex, columnIndex);
            var comp = jTable.prepareRenderer(ren, rowIndex, columnIndex);
            width = Math.max(width, comp.getPreferredSize().width + 2);
        }

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
        width = Math.max(width, headerBox.getWidth());
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
        i = new MenuItem("Save As Fits...", new MenuShortcut(KeyEvent.VK_S, true));
        i.addActionListener($ -> FITS_Writer.saveMPTable(table, false, false, null, ".fits.fz"));
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
                table.setLock(true);
                table.deleteRows(idx);
                table.setLock(false);
                if (idx.length > 0) {
                    table.updateRelatedPlot();
                }
            } else {
                IJ.error("Table Row Deletion", "Please select any row(s) to be deleted. All columns must be selected.");
            }
        });
        m.add(i);
        i = new MenuItem("Select All", new MenuShortcut(KeyEvent.VK_A));
        i.addActionListener($ -> jTable.selectAll());
        m.add(i);
        m.addSeparator();
        i = new MenuItem("Clear All");
        i.addActionListener($ -> table.clearTable());
        m.add(i);
        m.addSeparator();
        i = new MenuItem("Find...", new MenuShortcut(KeyEvent.VK_F));
        i.addActionListener($ -> {
            if (findWindow == null) {
                findWindow = new FindHandler(this);
            }
            findWindow.setVisible(true);
        });
        m.add(i);
        //todo this shows as (and works with) Ctrl+F3, while F3 on its own is sufficient (and preferred)
        i = new MenuItem("Goto...", new MenuShortcut(KeyEvent.VK_F3));
        i.addActionListener($ -> handleGotoEvent());
        m.add(i);
        mb.add(m);

        m = new Menu("Font");
        i = new MenuItem("Make Text Smaller");
        i.addActionListener($ -> zoomTable(-2));
        m.add(i);
        i = new MenuItem("Make Text Larger");
        i.addActionListener($ -> zoomTable(2));
        m.add(i);
        m.addSeparator();
        final var monospaced1 = new CheckboxMenuItem("Monospaced", monospaced.get());
        monospaced1.addItemListener($ -> {
            if (monospaced1.getState()) {
                monospaced.set(true);
                jTable.setFont(new Font("Monospaced", Font.PLAIN, fontSize.get().intValue()));
            } else {
                monospaced.set(false);
                jTable.setFont(new Font("SansSerif", Font.PLAIN, fontSize.get().intValue()));
            }
            zoomTable(0);
        });
        m.add(monospaced1);
        final var antialiasing = new CheckboxMenuItem("Antialiasing", antialiased.get());
        antialiasing.addItemListener($ -> {
            antialiased.set(antialiasing.getState());
            Java2.setAntialiasedText(getGraphics(), antialiased.get());
            jTable.repaint();
        });
        m.add(antialiasing);
        m.addSeparator();
        i = new MenuItem("Default Settings");
        i.addActionListener($ -> {
            monospaced.set(false);
            monospaced1.setState(false);
            fontSize.set(14f);
            antialiased.set(false);
            antialiasing.setState(false);
            Java2.setAntialiasedText(getGraphics(), antialiased.get());
            jTable.setFont(new Font("SansSerif", Font.PLAIN, fontSize.get().intValue()));
            zoomTable(0);
        });
        m.add(i);
        mb.add(m);

        m = new Menu("Results");
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
        i = new MenuItem("Fit Curve...");
        i.addActionListener($ -> CurveFitterHandler.fitCurve(MeasurementsWindow.this));
        m.add(i);
        i = new MenuItem("Options...");
        i.addActionListener($ -> IJ.doCommand("Input/Output..."));
        m.add(i);
        var cbi = new CheckboxMenuItem("Show saturation warning", showSatWarning.get());
        CheckboxMenuItem finalCbi2 = cbi;
        cbi.addItemListener($ -> {
            showSatWarning.set(finalCbi2.getState());
            jTable.repaint();
        });
        m.add(cbi);
        mb.add(m);

        m = new Menu("Filter");
        i = new MenuItem("Filter...", new MenuShortcut(KeyEvent.VK_G));
        i.addActionListener($ -> {
            if (filterWindow == null) {
                filterWindow = new FilterHandler(this);
            }
            filterWindow.setVisible(true);
        });
        m.add(i);
        cbi = new CheckboxMenuItem("Show only rows with linearity warning");
        CheckboxMenuItem finalCbi3 = cbi;
        cbi.addItemListener($ -> {
            if (finalCbi3.getState()) {
                // Find Peak_AP columns
                var cols = IntStream.range(0, table.getLastColumn())
                        .filter(c -> table.getColumnHeading(c).startsWith("Peak_")).map(v -> v+1).toArray();
                linearityFilter = RowFilter.numberFilter(RowFilter.ComparisonType.AFTER,
                        Prefs.get(Aperture_.AP_PREFS_LINWARNLEVEL, 30000), cols);
            } else {
                linearityFilter = null;
            }
            if (filterWindow == null) {
                filterWindow = new FilterHandler(this);
            }
            filterWindow.rebuildRowFilter();
        });
        m.add(cbi);
        i = new MenuItem("Clear Filters");
        i.addActionListener($ -> {
            if (filterWindow == null) {
                filterWindow = new FilterHandler(this);
            }
            finalCbi3.setState(false);
            filterWindow.reset();
            rowSorter.setRowFilter(null);
        });
        m.add(i);
        mb.add(m);

        setMenuBar(mb);
    }

    private void zoomTable(float delta) {
        var f = fontSize.get() + delta;
        if (f <= 1) {
            return;
        }
        fontSize.set(f);
        jTable.setFont(jTable.getFont().deriveFont(fontSize.get()));
        jTable.getTableHeader().setFont(jTable.getTableHeader().getFont().deriveFont(fontSize.get()));
        rowHeadings.setFont(jTable.getFont());
        for (int c = 0; c < jTable.getColumnCount(); c++) {
            adjustWidth(c);
        }
        scrollPane.getRowHeader()
                .setPreferredSize(new Dimension(adjustWidth(rowHeadings, 0, true), 0));
        var s = jTable.prepareRenderer(jTable.getCellRenderer(0, 0), 0, 0).getPreferredSize();
        jTable.setRowHeight(s.height);
        rowHeadings.setRowHeight(s.height);
    }

    public RowFilter<? super MeasurementsTableView, ? super Integer> getLinearityFilter() {
        return linearityFilter;
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

        int result = JOptionPane.showConfirmDialog(MeasurementsWindow.this, panel, "Go To Cell",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE,
                UIHelper.createImageIcon("Astronomy/images/icons/table/goto.png", ""));
        if (result == JOptionPane.OK_OPTION) {
            // Retrieve the selected column index and row input
            int column = columnDropdown.getSelectedIndex();
            String rowInput = rowField.getText();

            if (!rowInput.isEmpty()) {
                int row = Integer.parseInt(rowInput.trim());

                if (row >= 0 && row <= jTable.getRowCount() && column >= 0 && column <= jTable.getColumnCount()) {
                    // Scroll to the specified cell
                    var cellRect = jTable.getCellRect(row, column, true);
                    var visibleRect = jTable.getVisibleRect();
                    cellRect.x += visibleRect.width - cellRect.width;
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

    private class DoubleCellRenderer extends PaddedRenderer {
        private final DecimalFormat decimalFormat;

        public DoubleCellRenderer(int decimalPlaces) {
            super();
            decimalFormat = new DecimalFormat("#." + "#".repeat(decimalPlaces));
        }

        @Override
        protected void setValue(Object value) {
            /*if (value instanceof Double) {
                value = decimalFormat.format(value);
            }*/
            super.setValue(value);
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            super.getTableCellRendererComponent(table, value, isSelected, hasFocus,
                    row, column);
            setHorizontalAlignment(LEFT);
            setToolTipText(null);
            if (value instanceof Double d) {
                if (table.getColumnName(column).equals("slice")) {
                    setHorizontalAlignment(RIGHT);
                    setText(String.valueOf(d.intValue()));
                }
            }
            if (!isSelected && showSatWarning.get()) {
                if (table.getColumnName(column).startsWith("Peak_")) {
                    if (value instanceof Double d) {
                        if (d >=saturationWarningLevel) {
                            setBackground(Color.RED);
                            setToolTipText("Saturated aperture based on aperture settings");
                        } else if (d >= linearityWarningLevel) {
                            setBackground(Color.YELLOW);
                            setToolTipText("Nonlinear aperture based on aperture settings");
                        }
                    }
                } else {
                    var m = AP_PATTERN.matcher(table.getColumnName(column));
                    if (m.matches()) {
                        var ap = m.group("AP");
                        if (ap != null) {
                            var i = MeasurementsWindow.this.table.getColumnIndex("Peak_" + ap);
                            if (i == COLUMN_NOT_FOUND) {
                                if (ap.startsWith("C")) {
                                    ap = ap.replace("C", "T");
                                } else {
                                    ap = ap.replace("T", "C");
                                }
                                i = MeasurementsWindow.this.table.getColumnIndex("Peak_" + ap);
                            }
                            if (i != COLUMN_NOT_FOUND) {
                                var d = MeasurementsWindow.this.table.getValueAsDouble(i, table.convertRowIndexToModel(row));
                                if (d >=saturationWarningLevel) {
                                    setBackground(Color.RED);
                                    setToolTipText("Saturated aperture based on aperture settings and the value in " + "Peak_" + ap);
                                } else if (d >= linearityWarningLevel) {
                                    setBackground(Color.YELLOW);
                                    setToolTipText("Nonlinear aperture based on aperture settings and the value in " + "Peak_" + ap);
                                }
                            }
                        }
                    }
                }
            }

            return this;
        }
    }

    public class PaddedRenderer extends DefaultTableCellRenderer {
        final double saturationWarningLevel = Prefs.get(Aperture_.AP_PREFS_SATWARNLEVEL, 55000);
        final double linearityWarningLevel = Prefs.get(Aperture_.AP_PREFS_LINWARNLEVEL, 30000);
        final Border padding = BorderFactory.createEmptyBorder(1, 3, 1, 3);
        static final Pattern AP_PATTERN = Pattern.compile(".+_(?<AP>[CT][0-9]+)");

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            super.getTableCellRendererComponent(table, value, isSelected, hasFocus,
                    row, column);

            setHorizontalAlignment(CENTER);
            setBorder(BorderFactory.createCompoundBorder(getBorder(), padding));

            if (!isSelected && showSatWarning.get()) {
                setBackground(table.getBackground());
                if (table.getColumnName(column).equals("Label") || table.getColumnName(column).equals("slice")) {
                    var isLin = false;
                    var isSat = false;
                    var colCount = MeasurementsWindow.this.table.getLastColumn();
                    for (int c = 0; c < colCount; c++) {
                        var heading = MeasurementsWindow.this.table.getColumnHeading(c);
                        if (heading == null) {
                            continue;
                        }
                        Matcher m;
                        String ap;
                        if (heading.startsWith("Peak_") && (m = AP_PATTERN.matcher(heading)).matches() && (ap = m.group("AP")) != null) {
                            var i = MeasurementsWindow.this.table.getColumnIndex("Peak_" + ap);
                            if (i == COLUMN_NOT_FOUND) {
                                if (ap.startsWith("C")) {
                                    ap = ap.replace("C", "T");
                                } else {
                                    ap = ap.replace("T", "C");
                                }
                                i = MeasurementsWindow.this.table.getColumnIndex("Peak_" + ap);
                            }
                            if (i != COLUMN_NOT_FOUND) {
                                var d = MeasurementsWindow.this.table.getValueAsDouble(i, table.convertRowIndexToModel(row));
                                if (d >= saturationWarningLevel) {
                                    isSat = true;
                                    break;
                                } else if (d >= linearityWarningLevel) {
                                    isLin = true;
                                }
                            }
                        }
                    }
                    if (isSat) {
                        setBackground(Color.RED);
                        setToolTipText("Slice contains a saturated aperture based on aperture settings and the peak value(s)");
                    } else if (isLin) {
                        setBackground(Color.YELLOW);
                        setToolTipText("Slice contains a nonlinear aperture based on aperture settings and the peak value(s)");
                    }
                }
            }

            return this;
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
        public int getColumnCount() {//todo support string columns
            if (table.getLastColumn() == -1) {
                return 0;
            }
            return table.getLastColumn() + (offsetForRowNumbers()) + 1;
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            if (isLabelCol(columnIndex)) {
                return table.getLabel(rowIndex);
            }
            if (isRowNumCol(columnIndex)) {
                return rowIndex+table.getBaseRowNumber();
            }
            //table.getStringValue() //todo if double == nan, try get string. What to do with col. type? check first value? not safe
            return table.getValue(table.getColumnHeading(offsetCol(columnIndex)), rowIndex);
        }

        @Override
        public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
            if (isLabelCol(columnIndex)) {
                if (aValue instanceof String s) {
                    table.setLabel(s, rowIndex);
                } else {
                    AIJLogger.log("Tried to write %s, which is not a string, to the label".formatted(aValue));
                    return;
                }
            } else {
                if (aValue instanceof Number n) {
                    table.setValue(offsetCol(columnIndex), rowIndex, n.doubleValue());
                } else {
                    AIJLogger.log("Tried to write %s, which is not a number, to a cell".formatted(aValue));
                    return;
                }
            }
            table.updateRelatedPlot();
        }

        @Override
        public boolean isCellEditable(int rowIndex, int columnIndex) {
            return !isRowNumCol(columnIndex) || isLabelCol(columnIndex);
        }

        @Override
        public String getColumnName(int column) {
            if (isLabelCol(column)) {
                return "Label";
            }
            if (isRowNumCol(column)) {
                return "";
            }

            var h = table.getColumnHeading(offsetCol(column));
            if (h != null) {
                return h;
            }

            return super.getColumnName(column);
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
            return offsetForLabel() != 0 && offsetCol(c) == -offsetForLabel();
        }

        public boolean isRowNumCol(int c) {
            return offsetForRowNumbers() != 0 && offsetCol(c) == -offsetForRowNumbers();
        }

        public int offsetCol(int c) {
            var o = offsetForLabel();
            if (table.showRowNumbers()) {
                o += 1;
            }

            return c - o;
        }

        private int offsetForLabel() {
            return table.hasRowLabels() ? 1 : 0;
        }

        private int offsetForRowNumbers() {
            return offsetForLabel() + (table.showRowNumbers() ? 1 : 0);
        }
    }
}
