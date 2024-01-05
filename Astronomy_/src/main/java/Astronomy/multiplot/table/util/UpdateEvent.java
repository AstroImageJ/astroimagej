package Astronomy.multiplot.table.util;

public enum UpdateEvent {
    REBUILD(true),
    DATA_CHANGED(false),
    ROW_DELETED(true),
    ROW_INSERTED(true),
    ROW_UPDATED(false),
    CELL_UPDATED(false),
    COL_ADDED(true),
    COL_RENAMED(true),
    ;

    public final boolean structureModification;

    UpdateEvent(boolean structureModification) {
        this.structureModification = structureModification;
    }
}
