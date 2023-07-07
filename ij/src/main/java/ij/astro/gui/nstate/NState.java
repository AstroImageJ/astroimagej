package ij.astro.gui.nstate;

import javax.swing.*;

public interface NState<STATE extends Enum<STATE> & NState<STATE>> {
    Icon icon();

    boolean isOn();

    int ordinal();

    default STATE next() {
        var o = ordinal() + 1;
        if (o >= values0().length) {
            o = 0;
        }

        return values0()[o];
    }

    default STATE previous() {
        var o = ordinal() - 1;
        if (o < 0) {
            o = values0().length - 1;
        }

        return values0()[o];
    }

    default STATE fromString(String s) {
        try {
            return Enum.valueOf((Class<STATE>) getClass(), s);
        } catch (IllegalArgumentException e) {
            return values0()[0];
        }
    }

    /**
     * Not needed, but better to override to skip cloning the array each call
     */
    @SuppressWarnings("unchecked")
    @Deprecated()
    default STATE[] values0() {
        return (STATE[]) this.getClass().getEnumConstants();
    }
}
