package ij.astro.gui.nstate;

import ij.astro.gui.MergedIcon;
import ij.astro.util.EmojiIcon;

import javax.swing.*;
import java.awt.*;

public enum BiState implements NState<BiState> {
    FALSE("■", "☐", Color.WHITE),
    TRUE("■", "☑", Color.WHITE);

    private final Icon icon;

    BiState(String icon1, String icon2, Color color) {
        this.icon = new MergedIcon(new EmojiIcon(icon1, 19, color), new EmojiIcon(icon2, 19), -3, 2, true);
    }

    @Override
    public Icon icon() {
        return icon;
    }

    @Override
    public boolean isOn() {
        return this == TRUE;
    }

    @Override
    public BiState[] values0() {
        return BiState.values();
    }
}
