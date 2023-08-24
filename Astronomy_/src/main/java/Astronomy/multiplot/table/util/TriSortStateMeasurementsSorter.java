package Astronomy.multiplot.table.util;

import Astronomy.multiplot.table.MeasurementsWindow;

import javax.swing.*;
import javax.swing.table.TableRowSorter;
import java.util.List;

public class TriSortStateMeasurementsSorter extends TableRowSorter<MeasurementsWindow.MeasurementsTableView> {
    private SortKey lastSort;

    public TriSortStateMeasurementsSorter(MeasurementsWindow.MeasurementsTableView model) {
        super(model);
    }

    @Override
    public void setSortKeys(List<? extends SortKey> sortKeys) {
        if (sortKeys != null && !sortKeys.isEmpty()) {
            var p = sortKeys.get(0);
            if (lastSort != null && p != null &&
                    lastSort.getColumn() == p.getColumn() &&
                    lastSort.getSortOrder() != p.getSortOrder() &&
                    p.getSortOrder() == SortOrder.ASCENDING && lastSort.getSortOrder() == SortOrder.DESCENDING) {
                p = new SortKey(p.getColumn(), SortOrder.UNSORTED);
            }
            lastSort = p;
            try {
                ((List<SortKey>) sortKeys).set(0, p);
            } catch (Exception e) {
                System.out.println("Failed to modify sort keys");
            }
        }
        super.setSortKeys(sortKeys);
    }
}
