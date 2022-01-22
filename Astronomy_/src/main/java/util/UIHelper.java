package util;

import ij.IJ;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.text.DefaultEditorKit;
import java.awt.*;
import java.awt.event.KeyEvent;

public class UIHelper {
    private static boolean lookAndFeelSet = false;

    public static void setLookAndFeel() {
        if (lookAndFeelSet) return;

        if (IJ.isWindows()) {
            try {UIManager.setLookAndFeel("com.sun.java.swing.plaf.windows.WindowsLookAndFeel");}
            catch (Exception ignored) {}
        } else if (IJ.isLinux()) {
            try {UIManager.setLookAndFeel("javax.swing.plaf.metal.MetalLookAndFeel");}
            catch (Exception ignored) {}
        }
        else {
            try {UIManager.setLookAndFeel("javax.swing.plaf.metal.MetalLookAndFeel");}
            catch (Exception ignored) {}
        }
        if (IJ.isMacOSX()) {
            InputMap im = (InputMap) UIManager.get("TextField.focusInputMap");
            im.put(KeyStroke.getKeyStroke(KeyEvent.VK_C, KeyEvent.META_DOWN_MASK), DefaultEditorKit.copyAction);
            im.put(KeyStroke.getKeyStroke(KeyEvent.VK_V, KeyEvent.META_DOWN_MASK), DefaultEditorKit.pasteAction);
            im.put(KeyStroke.getKeyStroke(KeyEvent.VK_X, KeyEvent.META_DOWN_MASK), DefaultEditorKit.cutAction);
        }

        lookAndFeelSet = true;
    }

    public static void recursiveFontSetter(Component component, Font font) {
        if (component instanceof Container container) {
            for (Component containerComponent : container.getComponents()) {
                recursiveFontSetter(containerComponent, font);
            }
        }
        if (component instanceof JComponent jComponent) {
            if (jComponent.getBorder() instanceof TitledBorder titledBorder) {
                titledBorder.setTitleFont(font);
            }
        }
        component.setFont(font);
    }
}
