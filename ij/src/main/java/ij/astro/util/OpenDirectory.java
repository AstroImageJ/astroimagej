package ij.astro.util;

import java.awt.Desktop;
import java.io.IOException;
import java.nio.file.Path;

import ij.Prefs;
import ij.astro.io.ConfigHandler;
import ij.astro.logging.ConsoleLogging;
import ij.plugin.PlugIn;

public class OpenDirectory implements PlugIn {
    @Override
    public void run(String arg) {
        var path = switch (arg) {
            case "log" -> ConsoleLogging.getLogPath().getParent();
            case "prefs" -> Path.of(Prefs.getPrefsDir());
            case "config" -> ConfigHandler.getPath().getParent();
            default -> null;
        };

        if (path == null) {
            return;
        }

        try {
            if (Desktop.isDesktopSupported()) {
                var desktop = Desktop.getDesktop();
                desktop.open(path.toFile());
                return;
            }
        } catch (SecurityException | IOException e) {
            e.printStackTrace();
        }

        // Could not open via Desktop, try different approach...

    }
}
