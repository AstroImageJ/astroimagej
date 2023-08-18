package Astronomy.multiplot.table.util;

import Astronomy.multiplot.table.MeasurementsWindow;
import ij.astro.logging.AIJLogger;
import ij.astro.types.Pair;
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
    //(?:&)?\s*((?<COLUMN>(?:[0-9]+|[a-zA-Z0-9_\.\-\/\(\)]+|(?=["'])(?:"[^"\\]*(?:\\[\s\S][^"\\]*)*"|'[^'\\]*(?:\\[\s\S][^'\\]*)*')))?\s*(?<FILTER>(?:\*[\w\d]+|[<>=!]\s*[0-9.]+))\s*(?<AND>&)?)
    private static final Pattern FILTER_PATTERN =
            Pattern.compile("(?:&)?\\s*" + // Needed to prevent second filter seeing the column as the filter
                    // Match the column
                    "((?<COLUMN>(?:[0-9]+|" + // Column number
                    "[a-zA-Z0-9_\\.\\-\\/\\(\\)]+|" + // Column name, restricted characters
                    "(?=[\"'])(?:\"[^\"\\\\]*(?:\\\\[\\s\\S][^\"\\\\]*)*\"|'[^'\\\\]*(?:\\\\[\\s\\S][^'\\\\]*)*')))?\\s*" + // Column name, in escapable quotes, allows any character
                    // Match the filter
                    "(?<FILTER>(?:\\*[\\w\\d]+|" + // Regex filter
                    "[<>=!]\\s*[0-9.]+))\\s*" + // Numerical filter
                    "(?<AND>&)?)"); // And this with the next filter
    final MeasurementsWindow window;
    private boolean regex;
    private boolean showDebug;
    private String lastRowInput;

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
            lastRowInput = null;
            window.getRowSorter().setRowFilter(window.getLinearityFilter());
        });
        p.add(b2, c);
        c.gridx++;
        var debug = new JCheckBox("Debug");
        debug.setToolTipText("Log used row filters after parsing.");
        debug.addActionListener(e -> {
            showDebug = !showDebug;
        });
        p.add(debug, c);

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
                    Each filter is wrapped is separated by whitespace. Consecutive filters are ORed together,
                while filters separated by an & will be ANDed together. Must press <Enter> to apply filter.
                Some actions in MultiPlot, such as hover selection, will remove the filter.
                
                    A filter can apply to a specific column by beginning the filter with the column name or number.
                Case sensitive. Column names containing whitespace or other odd characters must be surrounded by quotes.
                Some whitespace is allowed between filter elements.
                
                    Values are filtered by the following: > (greater than), < (less than),
                = (equals), and ! (not equals). Regex is also supported by supplying the expression, starting with an *.
                
                    Eg.
                [<10                                     ]
                    Filters rows such that only rows containing a value less than 10 are displayed.
                    
                [slice<10                                ]
                    Filters rows such that only rows where values in the column "slice" are less
                than 10 are displayed.
                
                [slice<10 slice=10                       ]
                    Filters rows such that only rows where values in the column "slice" are less
                than 10, OR values in the column "slice" are 10 are displayed.
                
                [Saturated!0                             ]
                    Filters rows such that only rows where values in the column "Saturated" that
                are not equal to 0 are displayed.
                
                [rel_flux_T1<.48 & rel_flux_err_T1>.0006 ]
                    Filters columns such that only rows containing values in "rel_flux_T1" that are less than 0.48,
                AND values in "rel_flux_err_T1" that are greater than 0.0006 are displayed.
                
                Column Filtering:
                    Column filtering is a basic case sensitive text search of the column headers. Must press <Enter> to apply filter.
                    
                    Eg.
                [T1                                       ]
                    Show only columns whose name contains "T1"
                """);
        var s = new JScrollPane(tp);
        s.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        f.add(s);
        UIHelper.setCenteredOnScreen(f, this);
        f.pack();
        f.setVisible(true);
    }

    public void rebuildRowFilter() {
        window.getRowSorter().setRowFilter(buildRowFilterFromInput(lastRowInput));
    }

    private RowFilter<? super MeasurementsWindow.MeasurementsTableView, ? super Integer> buildRowFilterFromInput(String input) {
        if (input == null) {
            return window.getLinearityFilter();
        }

        lastRowInput = input;
        var matcher = FILTER_PATTERN.matcher(input);

        var andSet = new HashSet<RowFilter<? super MeasurementsWindow.MeasurementsTableView, ? super Integer>>();
        var orSet = new HashSet<RowFilter<? super MeasurementsWindow.MeasurementsTableView, ? super Integer>>();

        var anding = false;
        while (matcher.find()) {
            String columnValue = matcher.group("COLUMN");
            String filterValue = matcher.group("FILTER");
            String andValue = matcher.group("AND");

            var f = makeFilter(columnValue.trim(), filterValue.trim());
            if (anding || andValue != null) {
                andSet.add(f.first());
                if (andValue == null) {
                    orSet.add(RowFilter.andFilter(andSet));
                    andSet.clear();
                    debug.append(f.second());
                    debug.append("]");
                }

                if (!anding) {
                    debug.append("[");
                    debug.append(f.second()).append(" AND ");
                }

                if (andValue != null && anding) {
                    debug.append(" AND ");
                    debug.append(f.second());
                }
            } else {
                if (!orSet.isEmpty()) {
                    debug.append(" OR ");
                }
                debug.append(f.second());

                orSet.add(f.first());
            }

            anding = andValue != null;
        }

        if (orSet.isEmpty()) {
            return window.getLinearityFilter();
        }

        var out = RowFilter.orFilter(orSet);

        andSet = new HashSet<>();
        andSet.add(window.getLinearityFilter());
        andSet.add(out);

        debug.append(")").append(window.getLinearityFilter() != null ? " AND linearity filter" : "");

        if (showDebug) {
            AIJLogger.log(debug.toString());
        }

        return window.getLinearityFilter() != null ? RowFilter.andFilter(andSet) : out;
    }

    private Pair.GenericPair<RowFilter<? super MeasurementsWindow.MeasurementsTableView, ? super Integer>, String>
    makeFilter(String col, String filter) {
        if (filter == null) {
            throw new IllegalArgumentException("Filter must be present");
        }

        var cols = new int[0];
        if (col != null) {
            try {
                cols = new int[]{Integer.parseInt(col)};
            } catch (NumberFormatException e) {
                // Unwrap quoted column
                if (col.startsWith("\"") && col.endsWith("\"")) {//todo handle escaped quotes
                    col = col.substring(1, col.length()-1);
                }

                var i = window.getTable().getColumnIndex(col);
                if (i != ResultsTable.COLUMN_NOT_FOUND) {
                    // Add one to index as label is not a real col. in MT
                    cols = new int[]{i+1};
                } else {
                    // For debug logging
                    col = "COLUMN NOT FOUND ('%s')".formatted(col);
                }
            }
        }

        var notNumeric = false;
        var type = switch (filter.substring(0, 1)) {
            case ">" -> RowFilter.ComparisonType.AFTER;
            case "<" -> RowFilter.ComparisonType.BEFORE;
            case "=" -> RowFilter.ComparisonType.EQUAL;
            case "!" -> RowFilter.ComparisonType.NOT_EQUAL;
            case "*" -> {
                notNumeric = true;
                yield null;
            }
            default -> throw new IllegalStateException("Unexpected value in type: " + filter.charAt(0));
        };

        var debug = "{%s %s %s, in " + (cols.length == 0 ? "all columns}" : "column '%s' (#%s)}");

        if (notNumeric) {
            filter = filter.substring(1); // Remove preceding *
            return new Pair.GenericPair<>(RowFilter.regexFilter(filter, cols),
                    debug.formatted("Text", "matching", "regex of '" + filter + "'", col, cols[0]));
        }

        var d = 0D;

        try {
            d = Double.parseDouble(filter.substring(1).trim());
        } catch (NumberFormatException e) {
            throw new IllegalStateException("Unexpected value in double: " + filter.substring(1));
        }

        return new Pair.GenericPair<>(RowFilter.numberFilter(type, d, cols),
                debug.formatted("Values", switch (type) {
                    case BEFORE -> "less than";
                    case AFTER -> "greater than";
                    case EQUAL -> "equal to";
                    case NOT_EQUAL -> "not equal to";
                }, d, col, cols[0]));
    }
}
