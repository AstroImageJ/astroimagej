package Astronomy.multiplot.table.util;

import Astronomy.multiplot.table.MeasurementsWindow;
import ij.astro.util.UIHelper;
import ij.measure.ResultsTable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.util.HashSet;
import java.util.regex.Pattern;

public class FilterHandler extends JDialog {
    /**
     * COLUMN (optional)
     * 	(?<COLUMN>c(?:[0-9]+|[a-zA-Z0-9_]+))
     * 	is there an issue with whitespace in column names?
     * 		add col number as tooltip?
     * FILTER
     * 	(?<FILTER>(?:r[\w\d]+|[<>=!][0-9.]+))
     * 	not equal = !
     * 	allows string/regex filter by prefixing with r
     * AND (optional)
     * 	(?<AND>&)?
     * 	if the filter should be ANDed with the next one
     */
    //todo add support for quotes to allow whitespace in col. name
    //todo  add another group that handles basic regex input
    private static final Pattern FILTER_PATTERN =
            Pattern.compile("(\\((?<COLUMN>c(?:[0-9]+|[a-zA-Z0-9_\\.\\-\\/\\(\\)]+))?\\s*(?<FILTER>(?:r[\\w\\d]+|[<>=!][0-9.]+))\\)\\s*(?<AND>&)?)");
    final MeasurementsWindow window;
    private boolean regex;

    public FilterHandler(MeasurementsWindow window) {
        super(window, "Filter...", false);
        this.window = window;
        setModalityType(ModalityType.MODELESS);

        addComponents();

        pack();
        UIHelper.setCenteredOnWindow(this, window);
        setIconImage(UIHelper.createImage("Astronomy/images/icons/table/filter.png"));
    }

    @Override
    public void setVisible(boolean b) {
        if (b && !isVisible()) {
            UIHelper.setCenteredOnWindow(this, window);
        }
        super.setVisible(b);
    }

    protected void addComponents() {
        var c = new GridBagConstraints();
        var p = new JPanel(new GridBagLayout());
        p.setFont(new Font("Monospaced", Font.PLAIN, 14));
        var l = new JLabel("Row Filter:");
        l.setFont(p.getFont());
        p.add(l, c);
        var rowInput = new JTextField(30);
        rowInput.addActionListener($ -> {
            window.getRowSorter().setRowFilter(buildRowFilterFromInput(rowInput.getText()));
        });
        p.add(rowInput, c);
        c.gridy = 1;
        c.gridx = 0;
        l = new JLabel("Col Filter:");
        l.setFont(p.getFont());
        p.add(l, c);
        c.gridx++;
        var columnInput = new JTextField(30);
        ActionListener cFilterL = $ -> {
            if (window.getJTable().getColumnModel() instanceof HiddenColumnModel cm) {
                if (columnInput.getText().isBlank()) {
                    cm.restoreColumns();
                    return;
                }
                var pattern = Pattern.compile(columnInput.getText());
                cm.filterColumns(column -> {
                    if (column.getIdentifier() instanceof String name) {
                        return regex ? pattern.matcher(name).find() : name.contains(columnInput.getText());
                    }

                    return false;
                });
            }
        };
        columnInput.addActionListener(cFilterL);
        p.add(columnInput, c);
        c.gridx++;

        var regexButton = new JCheckBox("Regex");
        regexButton.setToolTipText("Use Regex for column filtering.");
        regexButton.addActionListener(e -> {
            regex = !regex;
            cFilterL.actionPerformed(e);
        });
        p.add(regexButton, c);

        c.gridx = 0;
        c.gridy++;

        var b = new JButton(UIHelper.createImageIcon(FilterHandler.class, "astroj/images/help.png", "Help_Icon"));
        b.addActionListener($ -> showHelpWindow());
        p.add(b, c);
        c.gridx++;
        var b2 = new JButton("Clear");
        b2.addActionListener($ -> {
            if (window.getJTable().getColumnModel() instanceof HiddenColumnModel cm) {
                cm.restoreColumns();
            }
            window.getRowSorter().setRowFilter(null);
        });
        p.add(b2, c);

        //input.getDocument().addDocumentListener($ -> {});
        add(p);
    }

    private void showHelpWindow() {
        var f = new JFrame("Filter Help");
        var tp = new JTextPane();
        tp.setFocusable(false);
        tp.setFont(new Font("Monospaced", Font.PLAIN, 12));
        tp.replaceSelection(
                """
                Filtering only effects the display of the table, it has no effect on the plot or the actual table data.
                
                Row Filtering:
                    Each filter is wrapped in (). Consecutive filters are ORed together,
                while filters separated by an & will be ANDed together. Must press <Enter> to apply filter.
                Some actions in MultiPlot, such as hover selection, will remove the filter.
                
                    A filter can apply to a specific column by beginning the filter with "c",
                followed by the column name or number. Case sensitive.
                
                    Values are filtered by the following: > (greater than), < (less than),
                = (equals), and ! (not equals). Regex is also supported by starting the filter with "r",
                followed by the expression.
                
                    Eg.
                [(<10)                                       ]
                    Filters rows such that only rows containing a value less than 10 are displayed.
                    
                [(cslice<10)                                 ]
                    Filters rows such that only rows where values in the column "slice" are less
                than 10 are displayed.
                
                [(cslice<10)(cslice=10)                      ]
                    Filters rows such that only rows where values in the column "slice" are less
                than 10, OR values in the column "slice" are 10 are displayed.
                
                [cSaturated!0                                ]
                    Filters rows such that only rows where values in the column "Saturated" that
                are not equal to 0 are displayed.
                
                [(crel_flux_T1<.48)&(crel_flux_err_T1>.0006) ]
                    Filters columns such that only rows containing values in "rel_flux_T1" that are less than 0.48,
                AND values in "rel_flux_err_T1" that are greater than 0.0006 are displayed.
                
                Column Filtering:
                    Column filtering is a basic case sensitive text search of the column headers. Must press <Enter> to apply filter.
                    
                    Eg.
                [T1                                           ]
                    Show only columns whose name contains "T1"
                """);
        var s = new JScrollPane(tp);
        s.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        f.add(s);
        UIHelper.setCenteredOnScreen(f, this);
        f.pack();
        f.setVisible(true);
    }

    // todo should default be AND?
    private RowFilter<? super MeasurementsWindow.MeasurementsTableView, ? super Integer> buildRowFilterFromInput(String input) {
        var matcher = FILTER_PATTERN.matcher(input);

        var andSet = new HashSet<RowFilter<? super MeasurementsWindow.MeasurementsTableView, ? super Integer>>();
        var orSet = new HashSet<RowFilter<? super MeasurementsWindow.MeasurementsTableView, ? super Integer>>();

        var anding = false;
        while (matcher.find()) {
            String columnValue = matcher.group("COLUMN");
            String filterValue = matcher.group("FILTER");
            String andValue = matcher.group("AND");

            var f = makeFilter(columnValue, filterValue);
            if (anding || andValue != null) {
                andSet.add(f);
                if (andValue == null) {
                    orSet.add(RowFilter.andFilter(andSet));
                    andSet.clear();
                }
            } else {
                orSet.add(f);
            }

            anding = andValue != null;

            /*System.out.println("Column: " + (columnValue != null ? columnValue : "N/A"));
            System.out.println("Filter: " + (filterValue != null ? filterValue : "N/A"));
            System.out.println("AND: " + (andValue != null ? andValue : "N/A"));
            System.out.println();*/
        }

        if (orSet.isEmpty()) {
            return null;
        }

        return RowFilter.orFilter(orSet);
    }

    private RowFilter<? super MeasurementsWindow.MeasurementsTableView, ? super Integer> makeFilter(String col, String filter) {
        if (filter == null) {
            throw new IllegalArgumentException("Filter must be present");
        }

        var cols = new int[0];
        if (col != null) {
            col = col.substring(1);
            try {
                cols = new int[]{Integer.parseInt(col)};
            } catch (NumberFormatException e) {
                var i = window.getTable().getColumnIndex(col);
                if (i != ResultsTable.COLUMN_NOT_FOUND) {
                    // Add one to index as label is not a real col. in MT
                    cols = new int[]{i+1};
                }
            }
        }

        if (filter.startsWith("r")) {
            filter = filter.substring(1);
            //System.out.printf("Regex filter: %s, %s%n", filter, Arrays.toString(cols));
            return RowFilter.regexFilter(filter, cols);
        } else {
            var type = switch (filter.substring(0, 1)) {
                case ">" -> RowFilter.ComparisonType.AFTER;
                case "<" -> RowFilter.ComparisonType.BEFORE;
                case "=" -> RowFilter.ComparisonType.EQUAL;
                case "!" -> RowFilter.ComparisonType.NOT_EQUAL;
                default -> throw new IllegalStateException("Unexpected value in type: " + filter.charAt(0));
            };

            var d = 0D;

            try {
                d = Double.parseDouble(filter.substring(1));
            } catch (NumberFormatException e) {
                throw new IllegalStateException("Unexpected value in double: " + filter.substring(1));
            }

            //System.out.printf("Number filter: %s, %s, %s%n", type, d, Arrays.toString(cols));
            return RowFilter.numberFilter(type, d, cols);
        }
    }
}
