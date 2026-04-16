package ij.astro.gui;

import java.awt.Component;
import java.awt.Container;
import java.awt.Font;
import java.awt.FontFormatException;
import java.awt.Frame;
import java.awt.GraphicsEnvironment;
import java.awt.MenuComponent;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Objects;

import javax.swing.JComponent;
import javax.swing.JTable;
import javax.swing.UIManager;
import javax.swing.border.TitledBorder;

import ij.astro.util.UIHelper;

public class FontLoader {
    private static final Font FLEX = registerFont("fonts/GoogleSansFlex.ttf");
    private static final Font CODE = registerFont("fonts/GoogleSansCode.ttf");
    public static final Font SERIF = FLEX;
    public static final Font SANSERIF = FLEX;
    public static final Font DIALOG = FLEX;
    public static final Font DIALOG_INPUT = FLEX;
    public static final Font MONOSPACED = CODE;
    private static final Map<String, Font> FONT_REPLACEMENT = Map.of(
            "Serif", SERIF,
            "SansSerif", SANSERIF,
            "SanSerif", SANSERIF,
            "Monospaced", MONOSPACED,
            "Dialog", DIALOG,
            "DialogInput", DIALOG_INPUT
    );
    private static boolean replacedSwingFonts = false;

    static {
        replaceSwingFonts();
    }

    private FontLoader() {
    }

    public static Font replaceFont(Font font) {
        var replacement = FONT_REPLACEMENT.get(font.getName());
        if (replacement != null) {
            if (font.getSize2D() == 12f) {
                return replacement;
            }

            return replacement.deriveFont(font.getSize2D());
        }

        return font;
    }

    public static void replaceFonts(Component c) {
        if (c == null) {
            return;
        }

        if (c instanceof Container container) {
            replaceFonts(container);
        }

        if (c.getFont() != null) {
            c.setFont(replaceFont(c.getFont()));
        }

        if (c instanceof Frame frame && frame.getMenuBar() != null && frame.getMenuBar().getFont() != null) {
            frame.getMenuBar().setFont(replaceFont(frame.getMenuBar().getFont()));
        }

        if (c instanceof JComponent jComponent) {
            var border = jComponent.getBorder();
            if (border instanceof TitledBorder titledBorder) {
                var font = titledBorder.getTitleFont();
                if (font != null) {
                    titledBorder.setTitleFont(replaceFont(font));
                }
            }

            replaceFonts(jComponent.getComponentPopupMenu());
        }

        if (c instanceof JTable jTable) {
            jTable.setFont(replaceFont(jTable.getFont()));
            replaceFonts(jTable.getTableHeader());
        }
    }

    private static void replaceFonts(Container c) {
        if (c == null) {
            return;
        }

        for (Component component : c.getComponents()) {
            replaceFonts(component);
        }
    }

    public static void replaceFonts(MenuComponent c) {
        if (c == null) {
            return;
        }

        var font = c.getFont();
        if (font != null) {
            c.setFont(replaceFont(font));
        }
    }

    private static void replaceSwingFonts() {
        if (!replacedSwingFonts) {
            UIHelper.setLookAndFeel();
            var d = UIManager.getLookAndFeelDefaults();
            for (var key : d.keySet()) {
                if (key != null && key.toString().endsWith(".font")) {
                    var v = d.get(key);
                    if (v instanceof Font f) {
                        d.put(key, replaceFont(f));
                    }
                }
            }

            var ud = UIManager.getDefaults();
            for (var key : ud.keySet()) {
                if (key != null && key.toString().endsWith(".font")) {
                    var v = ud.get(key);
                    if (!Objects.equals(v, d.get(key)) && v instanceof Font f) {
                        ud.put(key, replaceFont(f));
                    }
                }
            }

            replacedSwingFonts = true;
        }
    }

    private static Font registerFont(String font) {
        try (var in = Files.newInputStream(Path.of(font))) {
            Font base = Font.createFont(Font.TRUETYPE_FONT, in);
            GraphicsEnvironment.getLocalGraphicsEnvironment().registerFont(base);
            return base.deriveFont((float) 12.0);
        } catch (IOException | FontFormatException e) {
            System.err.println("Failed to load font " + font);
            e.printStackTrace();
        }

        return new Font("SansSerif", Font.PLAIN, 12);
    }
}
