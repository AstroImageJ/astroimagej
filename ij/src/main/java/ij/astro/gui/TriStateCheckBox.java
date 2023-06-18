package ij.astro.gui;

import ij.astro.util.EmojiIcon;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class TriStateCheckBox extends JButton {
    TriState state;

    public TriStateCheckBox() {
        this(TriState.DISABLED);
    }

    public TriStateCheckBox(TriState state) {
        super(state.icon);
        this.state = state;
        setMargin(new Insets(0, 0, 0, 4));
        setBackground(new Color(0, 0, 0, 0));
        setBorderPainted(false);
        setContentAreaFilled(false);
        setFocusPainted(false);
        setOpaque(false);

        // Allow for right click to go to the previous state
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (SwingUtilities.isRightMouseButton(e)) {
                    fireActionPerformed(new ActionEvent(TriStateCheckBox.this, ActionEvent.ACTION_FIRST, "decrement"));
                }
                super.mouseClicked(e);
            }
        });
    }

    @Override
    protected void fireActionPerformed(ActionEvent e) {
        if ("decrement".equals(e.getActionCommand())) {
            setStateWithoutUpdate(TriStateCheckBox.this.state.previous());
        } else {
            setStateWithoutUpdate(TriStateCheckBox.this.state.next());
        }

        super.fireActionPerformed(e);
    }

    public TriState getState() {
        return state;
    }

    public void setState(TriState state) {
        var newState = this.state != state;
        this.state = state;
        setIcon(this.state.icon);

        if (newState) {
            fireStateChanged();
        }
    }

    public void setStateWithoutUpdate(TriState state) {
        this.state = state;
        setIcon(this.state.icon);
    }

    public boolean isNotDisabled() {
        return state != TriState.DISABLED;
    }

    //⍻☐☑☒■□▣◽◾🗵🗷🗸◻◼🗹⮽█
    public enum TriState {
        DISABLED("☐"),
        INTERMEDIATE("☒"),
        ENABLED("☑");

        private final EmojiIcon icon;

        TriState(String icon) {
            this.icon = new EmojiIcon(icon, 19);
        }

        public TriState next() {
            var o = ordinal()+1;
            if (o >= values().length) {
                o = 0;
            }

            return values()[o];
        }

        public TriState previous() {
            var o = ordinal()-1;
            if (o < 0) {
                o = values().length-1;
            }

            return values()[o];
        }
    }
}
