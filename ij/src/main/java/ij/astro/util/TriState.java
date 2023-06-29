package ij.astro.util;

import ij.astro.gui.MergedIcon;

import javax.swing.*;
import java.awt.*;

//⍻☐☑☒■□▣◽◾🗵🗷🗸◻◼🗹⮽█
public enum TriState {
    DISABLED("■", "☐", false, Color.WHITE),
    ENABLED("■", "☑", true, Color.WHITE),
    ALT_ENABLED("■", "☑", true, Color.LIGHT_GRAY);

    public final Icon icon;
    private final boolean isOn;

    TriState(String icon1, String icon2, boolean isOn, Color color) {
        this.icon = new MergedIcon(new EmojiIcon(icon1, 19, color), new EmojiIcon(icon2, 19), -3, 2, true);
        this.isOn = isOn;
    }

    public TriState next() {
        var o = ordinal() + 1;
        if (o >= values().length) {
            o = 0;
        }

        return values()[o];
    }

    public TriState previous() {
        var o = ordinal() - 1;
        if (o < 0) {
            o = values().length - 1;
        }

        return values()[o];
    }

    public boolean isOn() {
        return isOn;
    }
}
