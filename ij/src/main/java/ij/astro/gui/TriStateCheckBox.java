package ij.astro.gui;

import ij.astro.util.TriState;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.HashMap;
import java.util.function.Function;

public class TriStateCheckBox extends JButton {
    TriState state;
    private final HashMap<TriState, String> tooltip = new HashMap<>();

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
        //setToolTipText(tooltip.getOrDefault(state, null));

        if (newState) {
            fireStateChanged();
        }
    }

    public void setStateWithoutUpdate(TriState state) {
        this.state = state;
        setIcon(this.state.icon);
        //setToolTipText(tooltip.getOrDefault(state, null));
    }

    public void setTooltips(Function<TriState, String> supplier) {
        for (TriState value : TriState.values()) {
            tooltip.put(value, supplier.apply(value));
        }
    }

    @Override
    public void paint(Graphics g) {
        super.paint(g);
        setToolTipText(tooltip.getOrDefault(state, null));
    }

    public boolean isNotDisabled() {
        return state != TriState.DISABLED;
    }

}
