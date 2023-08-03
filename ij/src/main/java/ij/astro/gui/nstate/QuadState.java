package ij.astro.gui.nstate;

import ij.astro.gui.MergedIcon;
import ij.astro.util.EmojiIcon;

import javax.swing.*;
import java.awt.*;

//⍻☐☑☒■□▣◽◾🗵🗷🗸◻◼🗹⮽█
public enum QuadState implements NState<QuadState> {
    DISABLED("■", "☐", false, Color.WHITE),
    ENABLED("■", "☑", true, Color.WHITE),
    ALT_ENABLED_1("■", "☑", true, Color.LIGHT_GRAY),
    ALT_ENABLED_2("■", "☒", true, Color.WHITE);// todo better names

    private final Icon icon;
    private final boolean isOn;

    QuadState(String icon1, String icon2, boolean isOn, Color color) {
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
    public QuadState[] values0() {
        return QuadState.values();
    }
}
