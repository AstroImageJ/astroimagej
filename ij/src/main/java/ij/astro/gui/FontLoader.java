package ij.astro.gui;

import java.awt.*;
import java.io.IOException;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Objects;

import javax.swing.JComponent;
import javax.swing.JTable;
import javax.swing.UIManager;
import javax.swing.border.TitledBorder;

import ij.IJ;
import ij.astro.util.UIHelper;

public class FontLoader {
    private static final Font FLEX = new Font("Google Sans Flex", Font.PLAIN, 12);
    private static final Font CODE = new Font("Google Sans Code", Font.PLAIN, 12);
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
        registerFonts();
        replaceSwingFonts();
        // Failure to load the fonts will result in the family being set to "Dialog"
        //FONT_REPLACEMENT.forEach((k, v) -> System.out.println(k + " -> " + v));
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

            // Workaround for JDK-8349701 and JDK-8367873 causing fonts to render incorrectly on Mac with
            // the Metal backend
            if (IJ.isMacOSX()) {
                UIManager.put(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            }

            replacedSwingFonts = true;
        }
    }

    private static void registerFonts() {
        try (var stream = Files.walk(Path.of("fonts"), 2)) {
            stream.filter(p -> p.getFileName().toString().endsWith(".ttf"))
                    .forEach(FontLoader::registerFont);
        } catch (IOException e) {
            System.err.println("Failed to load fonts");
            e.printStackTrace();
        }
    }

    private static Font registerFont(String name) {
        return registerFont(Path.of(name));
    }

    private static Font registerFont(Path path) {
        try (var in = Files.newInputStream(path)) {
            Font base = Font.createFont(Font.TRUETYPE_FONT, in);
            GraphicsEnvironment.getLocalGraphicsEnvironment().registerFont(base);
            //IO.println(path + " registered as " + base);
            return base.deriveFont((float) 12.0);
        } catch (IOException | FontFormatException e) {
            System.err.println("Failed to load font " + path);
            e.printStackTrace();
        }

        // Fallback
        return new Font("SansSerif", Font.PLAIN, 12);
    }
}
