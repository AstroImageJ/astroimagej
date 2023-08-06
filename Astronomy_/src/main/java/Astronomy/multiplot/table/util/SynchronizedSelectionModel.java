package Astronomy.multiplot.table.util;

import ij.astro.types.Pair;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

public class SynchronizedSelectionModel extends DefaultListSelectionModel {
    private final Owner owner;
    private final DefaultListSelectionModel common;

    private SynchronizedSelectionModel(Owner owner, DefaultListSelectionModel common) {
        this.owner = owner;
        this.common = common;

        common.addListSelectionListener(e ->
                fireValueChanged(e.getFirstIndex(), e.getLastIndex(), e.getValueIsAdjusting(), Owner.COMMON));
    }

    @Override
    protected void fireValueChanged(int firstIndex, int lastIndex, boolean isAdjusting) {
        fireValueChanged(firstIndex, lastIndex, isAdjusting, owner);
    }

    /**
     * We override the event firing to attach the owner type, see super impl. if this breaks.
     *
     * @see super#fireValueChanged(int, int, boolean) for impl.
     */
    private void fireValueChanged(int firstIndex, int lastIndex, boolean isAdjusting, Owner owner) {
        Object[] listeners = listenerList.getListenerList();
        ListSelectionEvent e = null;

        for (int i = listeners.length - 2; i >= 0; i -= 2) {
            if (listeners[i] == ListSelectionListener.class) {
                if (e == null) {
                    e = new OwnedListSelectionEvent(this, firstIndex, lastIndex, isAdjusting, owner);
                }
                ((ListSelectionListener)listeners[i+1]).valueChanged(e);
            }
        }
    }

    @Override
    public int getMinSelectionIndex() {
        return common.getMinSelectionIndex();
    }

    @Override
    public int getMaxSelectionIndex() {
        return common.getMaxSelectionIndex();
    }

    @Override
    public boolean getValueIsAdjusting() {
        return common.getValueIsAdjusting();
    }

    @Override
    public int getSelectionMode() {
        return common.getSelectionMode();
    }

    @Override
    public void setSelectionMode(int selectionMode) {
        common.setSelectionMode(selectionMode);
        super.setSelectionMode(selectionMode);
    }

    @Override
    public boolean isSelectedIndex(int index) {
        return common.isSelectedIndex(index);
    }

    @Override
    public boolean isSelectionEmpty() {
        return common.isSelectionEmpty();
    }

    @Override
    public void setLeadAnchorNotificationEnabled(boolean flag) {
        super.setLeadAnchorNotificationEnabled(flag);
        common.setLeadAnchorNotificationEnabled(flag);
    }

    @Override
    public boolean isLeadAnchorNotificationEnabled() {
        return common.isLeadAnchorNotificationEnabled();
    }

    @Override
    public void clearSelection() {
        super.clearSelection();
        common.clearSelection();
    }

    @Override
    public void setSelectionInterval(int index0, int index1) {
        super.setSelectionInterval(index0, index1);
        common.setSelectionInterval(index0, index1);
    }

    @Override
    public void addSelectionInterval(int index0, int index1) {
        super.addSelectionInterval(index0, index1);
        common.addSelectionInterval(index0, index1);
    }

    @Override
    public void removeSelectionInterval(int index0, int index1) {
        super.removeSelectionInterval(index0, index1);
        common.removeSelectionInterval(index0, index1);
    }

    @Override
    public void insertIndexInterval(int index, int length, boolean before) {
        super.insertIndexInterval(index, length, before);
        common.insertIndexInterval(index, length, before);
    }

    @Override
    public void removeIndexInterval(int index0, int index1) {
        super.removeIndexInterval(index0, index1);
        common.removeIndexInterval(index0, index1);
    }

    @Override
    public void setValueIsAdjusting(boolean isAdjusting) {
        super.setValueIsAdjusting(isAdjusting);
        common.setValueIsAdjusting(isAdjusting);
    }

    @Override
    public int getAnchorSelectionIndex() {
        return common.getAnchorSelectionIndex();
    }

    @Override
    public int getLeadSelectionIndex() {
        return common.getLeadSelectionIndex();
    }

    @Override
    public void setAnchorSelectionIndex(int anchorIndex) {
        super.setLeadSelectionIndex(anchorIndex);
        common.setAnchorSelectionIndex(anchorIndex);
    }

    @Override
    public void moveLeadSelectionIndex(int leadIndex) {
        super.moveLeadSelectionIndex(leadIndex);
        common.moveLeadSelectionIndex(leadIndex);
    }

    @Override
    public void setLeadSelectionIndex(int leadIndex) {
        super.setLeadSelectionIndex(leadIndex);
        common.setLeadSelectionIndex(leadIndex);
    }

    public static Pair.GenericPair<SynchronizedSelectionModel, SynchronizedSelectionModel> createPair() {
        var common = new DefaultListSelectionModel();
        return new Pair.GenericPair<>(new SynchronizedSelectionModel(Owner.MAIN, common),
                new SynchronizedSelectionModel(Owner.ROW_HEADING, common));
    }

    public enum Owner {
        MAIN,
        ROW_HEADING,
        COMMON,
    }

    public static class OwnedListSelectionEvent extends ListSelectionEvent {
        private final Owner owner;
        /**
         * Represents a change in selection status between {@code firstIndex} and
         * {@code lastIndex}, inclusive. {@code firstIndex} is less than or equal to
         * {@code lastIndex}. The selection of at least one index within the range will
         * have changed.
         *
         * @param source      the {@code Object} on which the event initially occurred
         * @param firstIndex  the first index in the range, &lt;= lastIndex
         * @param lastIndex   the last index in the range, &gt;= firstIndex
         * @param isAdjusting whether or not this is one in a series of
         *                    multiple events, where changes are still being made
         * @param owner
         */
        public OwnedListSelectionEvent(Object source, int firstIndex, int lastIndex, boolean isAdjusting, Owner owner) {
            super(source, firstIndex, lastIndex, isAdjusting);
            this.owner = owner;
        }

        public Owner getOwner() {
            return owner;
        }
    }
}
