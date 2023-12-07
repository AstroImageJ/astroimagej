package Astronomy.multiplot.table.util;

public enum UpdateEvent {
    REBUILD(true),
    DATA_CHANGED(false),
    ROW_DELETED(false),
    ROW_INSERTED(false),
    ROW_UPDATED(false),
    CELL_UPDATED(true),
    COL_ADDED(true),
    ;

    public final boolean structureModification;

    UpdateEvent(boolean structureModification) {
        this.structureModification = structureModification;
    }
}
