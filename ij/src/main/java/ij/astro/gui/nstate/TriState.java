package ij.astro.gui.nstate;

import ij.astro.gui.MergedIcon;
import ij.astro.util.EmojiIcon;

import javax.swing.*;
import java.awt.*;

//⍻☐☑☒■□▣◽◾🗵🗷🗸◻◼🗹⮽█
public enum TriState implements NState<TriState> {
    DISABLED("■", "☐", false, Color.WHITE),
    ENABLED("■", "☑", true, Color.WHITE),
    ALT_ENABLED("■", "☑", true, Color.LIGHT_GRAY);

    private final Icon icon;
    private final boolean isOn;

    TriState(String icon1, String icon2, boolean isOn, Color color) {
        this.icon = new MergedIcon(new EmojiIcon(icon1, 23, color), new EmojiIcon(icon2, 23), -3, 2, true);
        this.isOn = isOn;
    }

    @Override
    public Icon icon() {
        return icon;
    }

    public boolean isOn() {
        return isOn;
    }

    @Override
    public TriState[] values0() {
        return TriState.values();
    }
}
