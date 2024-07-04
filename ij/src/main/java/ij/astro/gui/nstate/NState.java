package ij.astro.gui.nstate;

import ij.astro.util.UIHelper;

import javax.swing.*;
import java.util.function.Function;

/**
 * An {@link Enum} representing variable state of fixed size.
 * <p>
 * For use in {@link ij.astro.io.prefs.Property}, values are stored and loaded based on
 * the value of {@link Enum#name()}, which should NOT change. Use {@link Enum#toString()} instead.
 */
public interface NState<STATE extends Enum<STATE> & NState<STATE>> {
    boolean isOn();

    int ordinal();

    default Icon icon() {
        return UIHelper.createImageIcon("Astronomy/images/icons/question_mark.png", 20 ,20);
    }

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

    default Function<STATE, String> getDefaultTooltips() {
        return state -> null;
    }
}
