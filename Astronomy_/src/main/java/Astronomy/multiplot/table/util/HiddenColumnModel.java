package Astronomy.multiplot.table.util;

import javax.swing.event.TableColumnModelEvent;
import javax.swing.table.DefaultTableColumnModel;
import javax.swing.table.TableColumn;
import java.util.Vector;
import java.util.function.Predicate;

public class HiddenColumnModel extends DefaultTableColumnModel {
    private Predicate<TableColumn> lastPredicate;
    private boolean columnsModified = false;
    private final TrackingModel trackingModel = new TrackingModel();

    public HiddenColumnModel() {
        super();
    }

    public void filterColumns(Predicate<TableColumn> predicate) {
        if (columnsModified || !predicate.equals(lastPredicate)) {
            restoreColumns();
            if (predicate == null) {
                return;
            }
            lastPredicate = predicate;
            if (selectionModel != null) {
                selectionModel.clearSelection();
            }

            for (TableColumn tableColumn : tableColumns.stream().filter(predicate.negate()).toList()) {
                removeColumn0(tableColumn);
            }

            //tableColumns = new Vector<>();
            //fireColumnRemoved(new TableColumnModelEvent(this, 0, 0));
            columnsModified = false;
        }
    }

    public void restoreColumns() {
        tableColumns = trackingModel.getTableColumns();
        fireColumnAdded(new TableColumnModelEvent(this, 0, 0));
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
        columnsModified = true;
    }

    public void removeColumn0(TableColumn column) {
        trackingModel.removeColumn(column);
        super.removeColumn(column);
    }

    @Override
    public void moveColumn(int columnIndex, int newIndex) {
        trackingModel.moveColumn(columnIndex, newIndex);
        super.moveColumn(columnIndex, newIndex);
        columnsModified = true;
    }

    private static class TrackingModel extends DefaultTableColumnModel {
        public Vector<TableColumn> getTableColumns() {
            return ((Vector<TableColumn>) tableColumns.clone());
        }
    }
}
