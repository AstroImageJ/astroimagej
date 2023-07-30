package ij.astro.util;

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
            im.put(KeyStroke.getKeyStroke(KeyEvent.VK_C, KeyEvent.CTRL_DOWN_MASK), DefaultEditorKit.copyAction);
            im.put(KeyStroke.getKeyStroke(KeyEvent.VK_V, KeyEvent.META_DOWN_MASK), DefaultEditorKit.pasteAction);
            im.put(KeyStroke.getKeyStroke(KeyEvent.VK_V, KeyEvent.CTRL_DOWN_MASK), DefaultEditorKit.pasteAction);
            im.put(KeyStroke.getKeyStroke(KeyEvent.VK_X, KeyEvent.META_DOWN_MASK), DefaultEditorKit.cutAction);
            im.put(KeyStroke.getKeyStroke(KeyEvent.VK_X, KeyEvent.CTRL_DOWN_MASK), DefaultEditorKit.cutAction);
//            InputMap in = (InputMap) UIManager.get("Spinner.focusInputMap");
//            in.put(KeyStroke.getKeyStroke(KeyEvent.VK_C, KeyEvent.META_DOWN_MASK), DefaultEditorKit.copyAction);
//            in.put(KeyStroke.getKeyStroke(KeyEvent.VK_C, KeyEvent.CTRL_DOWN_MASK), DefaultEditorKit.copyAction);
//            in.put(KeyStroke.getKeyStroke(KeyEvent.VK_V, KeyEvent.META_DOWN_MASK), DefaultEditorKit.pasteAction);
//            in.put(KeyStroke.getKeyStroke(KeyEvent.VK_V, KeyEvent.CTRL_DOWN_MASK), DefaultEditorKit.pasteAction);
//            in.put(KeyStroke.getKeyStroke(KeyEvent.VK_X, KeyEvent.META_DOWN_MASK), DefaultEditorKit.cutAction);
//            in.put(KeyStroke.getKeyStroke(KeyEvent.VK_X, KeyEvent.CTRL_DOWN_MASK), DefaultEditorKit.cutAction);
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

    public static void setCenteredOnScreen(Container window, Container referenceWindow) {
        setCenteredOnScreen(getScreen(referenceWindow), window);
    }

    public static void setCenteredOnScreen(int screen, Container window) {
        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        GraphicsDevice[] gd = ge.getScreenDevices();
        int width = 0, height = 0;
        if( screen > -1 && screen < gd.length ) {
            width = gd[screen].getDefaultConfiguration().getBounds().width;
            height = gd[screen].getDefaultConfiguration().getBounds().height;
            window.setLocation(
                    ((width / 2) - (window.getSize().width / 2)) + gd[screen].getDefaultConfiguration().getBounds().x,
                    ((height / 2) - (window.getSize().height / 2)) + gd[screen].getDefaultConfiguration().getBounds().y
            );
        } else {
            throw new RuntimeException( "No Screens Found" );
        }
    }

    public static void setLocationOnScreen(Window window, int screen, double x, double y) {
        GraphicsEnvironment g = GraphicsEnvironment.getLocalGraphicsEnvironment();
        GraphicsDevice[]    d = g.getScreenDevices();

        if ( screen >= d.length ) {
            screen = d.length - 1;
        }

        Rectangle bounds = d[screen].getDefaultConfiguration().getBounds();

        // Is double?
        if ( x == Math.floor(x) && !Double.isInfinite(x) ) {
            x *= bounds.x;  // Decimal -> percentage
        }
        if ( y == Math.floor(y) && !Double.isInfinite(y) ) {
            y *= bounds.y;  // Decimal -> percentage
        }

        x = bounds.x      + x;
        y = window.getY() + y;

        if ( x > bounds.x) x = bounds.x;
        if ( y > bounds.y) y = bounds.y;

        // If double we do want to floor the value either way
        window.setLocation((int)x, (int)y);
    }

    public static int getScreen(Container window) {
        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        var screens = ge.getScreenDevices();
        var screen = 0;
        for (int i = 0; i < screens.length; i++) {
            if (screens[i] == window.getGraphicsConfiguration().getDevice()) {
                screen = i;
                break;
            }
        }
        return screen;
    }
}
