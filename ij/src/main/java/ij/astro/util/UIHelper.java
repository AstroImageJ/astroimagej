package ij.astro.util;

import ij.IJ;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.text.DefaultEditorKit;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.io.IOException;

import static ij.IJ.showMessage;

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
            // See https://thebadprogrammer.com/swing-uimanager-keys/
            var fs = new String[]{
                    "TextField.focusInputMap", "FormattedTextField.focusInputMap", "TextArea.focusInputMap",
                    "TextPane.focusInputMap",
            };
            for (String f : fs) {
                InputMap im = (InputMap) UIManager.get(f);
                im.put(KeyStroke.getKeyStroke(KeyEvent.VK_C, KeyEvent.META_DOWN_MASK), DefaultEditorKit.copyAction);
                im.put(KeyStroke.getKeyStroke(KeyEvent.VK_C, KeyEvent.CTRL_DOWN_MASK), DefaultEditorKit.copyAction);
                im.put(KeyStroke.getKeyStroke(KeyEvent.VK_V, KeyEvent.META_DOWN_MASK), DefaultEditorKit.pasteAction);
                im.put(KeyStroke.getKeyStroke(KeyEvent.VK_V, KeyEvent.CTRL_DOWN_MASK), DefaultEditorKit.pasteAction);
                im.put(KeyStroke.getKeyStroke(KeyEvent.VK_X, KeyEvent.META_DOWN_MASK), DefaultEditorKit.cutAction);
                im.put(KeyStroke.getKeyStroke(KeyEvent.VK_X, KeyEvent.CTRL_DOWN_MASK), DefaultEditorKit.cutAction);
            }
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

    public static void setCenteredOnWindow(Container window, Container referenceWindow) {
        var ref = referenceWindow.getLocationOnScreen();
        ref.translate(referenceWindow.getWidth()/2, referenceWindow.getHeight()/2);
        ref.translate(-window.getWidth()/2, -window.getHeight()/2);
        window.setLocation(ref);
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

    /** Returns an ImageIcon, or null if the path was invalid. */
    public static ImageIcon createImageIcon(String path,int width, int height) {
        return new ImageIcon(UIHelper.createImage(path).getScaledInstance(width, height, Image.SCALE_SMOOTH));
    }

    /** Returns an ImageIcon, or null if the path was invalid. */
    public static ImageIcon createImageIcon(String path, String description) {
        return createImageIcon(IJ.getClassLoader(), path, description);
    }

    /** Returns an ImageIcon, or null if the path was invalid. */
    public static ImageIcon createImageIcon(Class<?> ref, String path, String description) {
        return createImageIcon(ref.getClassLoader(), path, description);
    }

    private static ImageIcon createImageIcon(ClassLoader loader, String path, String description) {
        java.net.URL imgURL = loader.getResource(path);
        if (imgURL != null) {
            return new ImageIcon(imgURL, description);
        } else {
            showMessage("Couldn't find icon file: " + path);
            return null;
        }
    }

    /** Returns an Image, or null if the path was invalid. */
    public static Image createImage(String path) {
        return createImage(IJ.getClassLoader(), path);
    }

    /** Returns an ImageIcon, or null if the path was invalid. */
    public static Image createImage(Class<?> ref, String path) {
        return createImage(ref.getClassLoader(), path);
    }

    /** Returns an ImageIcon, or null if the path was invalid. */
    private static Image createImage(ClassLoader loader, String path) {
        java.net.URL imgURL = loader.getResource(path);
        if (imgURL != null) {
            try {
                return ImageIO.read(imgURL);
            } catch (IOException e) {
                return null;
            }
        } else {
            showMessage("Couldn't find icon file: " + path);
            return null;
        }
    }

}
