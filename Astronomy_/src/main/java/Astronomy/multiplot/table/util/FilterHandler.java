package Astronomy.multiplot.table.util;

import Astronomy.multiplot.table.MeasurementsWindow;
import ij.measure.ResultsTable;

import javax.swing.*;
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
            Pattern.compile("(\\((?<COLUMN>c(?:[0-9]+|[a-zA-Z0-9_]+))?\\s*(?<FILTER>(?:r[\\w\\d]+|[<>=!][0-9.]+))\\)\\s*(?<AND>&)?)");
    final MeasurementsWindow window;

    public FilterHandler(MeasurementsWindow window) {
        super(window, "Filter...", false);
        this.window = window;
        setModalityType(ModalityType.MODELESS);

        addComponents();

        pack();
    }

    protected void addComponents() {
        var input = new JTextField(30);
        input.addActionListener($ -> {
            window.getRowSorter().setRowFilter(buildFilterFromInput(input.getText()));
        });
        //input.getDocument().addDocumentListener($ -> {});
        add(input);
    }

    // todo should default be AND?
    private RowFilter<? super MeasurementsWindow.MeasurementsTableView, ? super Integer> buildFilterFromInput(String input) {
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
