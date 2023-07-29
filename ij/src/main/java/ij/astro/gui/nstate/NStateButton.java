package ij.astro.gui.nstate;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.HashMap;
import java.util.Objects;
import java.util.function.Function;

public class NStateButton<STATE extends Enum<STATE> & NState<STATE>> extends JButton {
    private final HashMap<STATE, String> tooltip = new HashMap<>();
    private final HashMap<STATE, Icon> iconOverrides = new HashMap<>();
    private final STATE[] values;
    private STATE state;

    public NStateButton(STATE state) {
        this(state, false);
    }

    public NStateButton(STATE state, boolean swapMiddleAndLeft) {
        super(Objects.requireNonNull(state).icon());
        values = state.getDeclaringClass().getEnumConstants();
        this.state = state;
        setMargin(new Insets(0, 0, 0, 4));
        setBackground(new Color(0, 0, 0, 0));
        setBorderPainted(false);
        setContentAreaFilled(false);
        setFocusPainted(false);
        setOpaque(false);
        setTooltips(state.getDefaultTooltips());

        // Remove normal left-click behavior
        if (swapMiddleAndLeft) {
            removeMouseListener(getMouseListeners()[0]);
        }

        // Handle other mouse actions
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (SwingUtilities.isRightMouseButton(e)) {
                    fireActionPerformed(new ActionEvent(NStateButton.this, ActionEvent.ACTION_FIRST, "decrement"));
                }
                if ((swapMiddleAndLeft && SwingUtilities.isLeftMouseButton(e)) ||
                        (!swapMiddleAndLeft && SwingUtilities.isMiddleMouseButton(e))) {
                    showModal();
                    return;
                }

                if (swapMiddleAndLeft && SwingUtilities.isMiddleMouseButton(e)) {
                    fireActionPerformed(new ActionEvent(NStateButton.this, ActionEvent.ACTION_FIRST, ""));
                    return;
                }

                super.mouseClicked(e);
            }
        });
    }

    @Override
    protected void fireActionPerformed(ActionEvent e) {
        if ("decrement".equals(e.getActionCommand())) {
            setStateWithoutUpdate(this.state.previous());
        } else if (!"modal".equals(e.getActionCommand())){
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

    private void showModal() {
        var panel = new JPanel(new GridLayout(values.length, 1));
        var group = new ButtonGroup();
        var map = new HashMap<ButtonModel, STATE>();
        for (STATE value : values) {
            var b = Box.createHorizontalBox();
            var r = new JRadioButton("",value == state);
            b.add(r);
            b.add(new JLabel(iconOverrides.getOrDefault(value, value.icon())));
            b.add(Box.createHorizontalStrut(7));
            b.add(new JLabel(value.name()));
            b.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    if (SwingUtilities.isLeftMouseButton(e)) {
                        r.setSelected(true);
                    }
                }
            });
            group.add(r);
            b.setToolTipText(tooltip.getOrDefault(value, null));
            panel.add(b);
            map.put(r.getModel(), value);
        }

        if (JOptionPane.showConfirmDialog(getParent(), panel, "Specify Value", JOptionPane.YES_NO_OPTION) ==
                JOptionPane.OK_OPTION) {
            setState(map.get(group.getSelection()));
            fireActionPerformed(new ActionEvent(NStateButton.this, ActionEvent.ACTION_FIRST, "modal"));
        }
    }
}
