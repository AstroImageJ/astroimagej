package ij.astro.gui.nstate;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.HashMap;
import java.util.Objects;
import java.util.function.Function;

//todo not abstract? use directly, instead of subclasses?
public abstract class NStateButton<STATE extends Enum<STATE> & NState<STATE>> extends JButton {
    private final HashMap<STATE, String> tooltip = new HashMap<>();
    private final HashMap<STATE, Icon> iconOverrides = new HashMap<>();
    private final STATE[] values;
    private STATE state;

    public NStateButton(STATE state) {
        super(Objects.requireNonNull(state).icon());
        values = state.getDeclaringClass().getEnumConstants();
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
                    fireActionPerformed(new ActionEvent(NStateButton.this, ActionEvent.ACTION_FIRST, "decrement"));
                }
                super.mouseClicked(e);
            }
        });
    }

    @Override
    protected void fireActionPerformed(ActionEvent e) {
        if ("decrement".equals(e.getActionCommand())) {
            setStateWithoutUpdate(this.state.previous());
        } else {
            setStateWithoutUpdate(this.state.next());
        }

        super.fireActionPerformed(e);
    }

    public STATE getState() {
        return state;
    }

    public void setState(STATE state) {
        var newState = this.state != state;
        this.state = state;
        setIcon();

        if (newState) {
            fireStateChanged();
        }
    }

    public void setStateWithoutUpdate(STATE state) {
        this.state = state;
        setIcon();
    }

    public void setTooltips(Function<STATE, String> supplier) {
        for (STATE value : values) {
            tooltip.put(value, supplier.apply(value));
        }
    }

    public void setIconOverrides(Function<STATE, Icon> supplier) {
        for (STATE value : values) {
            var v = supplier.apply(value);
            if (v != null) {
                iconOverrides.put(value, supplier.apply(value));
            }
        }
        setIcon();
    }

    @Override
    public void paint(Graphics g) {
        super.paint(g);
        setToolTipText(tooltip.getOrDefault(state, null));
    }

    private void setIcon() {
        setIcon(iconOverrides.getOrDefault(state, state.icon()));
    }
}
