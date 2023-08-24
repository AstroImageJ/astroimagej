package Astronomy.multiplot.table.util;

import javax.swing.event.TableColumnModelEvent;
import javax.swing.table.DefaultTableColumnModel;
import javax.swing.table.TableColumn;
import java.util.HashSet;
import java.util.Set;
import java.util.Vector;
import java.util.function.Consumer;
import java.util.function.Predicate;

public class HiddenColumnModel extends DefaultTableColumnModel {
    private Predicate<TableColumn> lastPredicate;
    private boolean columnsModified = false;
    private final TrackingModel trackingModel = new TrackingModel();
    private final Set<Consumer<Boolean>> listeners = new HashSet<>();

    public HiddenColumnModel() {
        super();
    }

    public void filterColumns(Predicate<TableColumn> predicate) {
        if (columnsModified || (predicate != null && !predicate.equals(lastPredicate))) {
            restoreColumns(false);
            if (predicate == null) {
                return;
            }
            lastPredicate = predicate;
            if (selectionModel != null) {
                selectionModel.clearSelection();
            }

            var hasFiltered = false;
            for (TableColumn tableColumn : tableColumns.stream().filter(predicate.negate()).toList()) {
                removeColumn0(tableColumn);
                hasFiltered = true;
            }

            var ls = listeners.toArray();
            for (Object l : ls) {
                ((Consumer<Boolean>)l).accept(hasFiltered);
            }

            //tableColumns = new Vector<>();
            //fireColumnRemoved(new TableColumnModelEvent(this, 0, 0));
            columnsModified = false;
        }
    }

    public void restoreColumns() {
        restoreColumns(true);
    }

    public void removeFilter() {
        lastPredicate = null;
        refilter();
    }

    public void restoreColumns(boolean update) {
        tableColumns = trackingModel.getTableColumns();
        if (update) {
            fireColumnAdded(new TableColumnModelEvent(this, 0, 0));
            var ls = listeners.toArray();
            for (Object l : ls) {
                ((Consumer<Boolean>)l).accept(false);
            }
        }
    }

    public void refilter() {
        columnsModified = true;
        filterColumns(lastPredicate);
    }

    @Override
    public void addColumn(TableColumn aColumn) {
        trackingModel.addColumn(aColumn);
        super.addColumn(aColumn);
        columnsModified = true;
    }

    @Override
    public void removeColumn(TableColumn column) {
        removeColumn0(column);
        trackingModel.removeColumn(column);
        columnsModified = true;
    }

    public void removeColumn0(TableColumn column) {
        super.removeColumn(column);
    }

    @Override
    public void moveColumn(int columnIndex, int newIndex) {
        trackingModel.moveColumn(columnIndex, newIndex);
        super.moveColumn(columnIndex, newIndex);
        columnsModified = true;
    }

    public void addFilterListener(Consumer<Boolean> l) {
        listeners.add(l);
    }

    public void removeFilterListener(Consumer<Boolean> l) {
        listeners.remove(l);
    }

    private static class TrackingModel extends DefaultTableColumnModel {
        public Vector<TableColumn> getTableColumns() {
            return ((Vector<TableColumn>) tableColumns.clone());
        }
    }
}
