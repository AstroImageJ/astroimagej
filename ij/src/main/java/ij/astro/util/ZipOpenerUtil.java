package ij.astro.util;

import ij.Prefs;
import ij.gui.GenericDialog;
import ij.plugin.FolderOpener;

import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Utilities for opening a zip file as a folder.
 */
public class ZipOpenerUtil {

    public static boolean getOption() {
        var gd = new GenericDialog("Zip File Opener");
        gd.addCheckbox("Open as Virtual Stack", Prefs.get("folderopener.openAsVirtualStack", false));
        gd.setOKLabel("Open Image");
        gd.showDialog();
        var x = gd.getNextBoolean();
        Prefs.set("folderopener.openAsVirtualStack", x);
        FolderOpener.virtualIntended = x;
        if (gd.wasOKed()) return x;

        return false;
    }

    public static String[] getFilesInZip(String path) {
        String[] x = null;
        try {
            x = getFilesInZipImpl(path);
        } catch (IOException ignored) {}
        if (x == null) return new String[0];
        return x;
    }

    private static String[] getFilesInZipImpl(String path) throws IOException {
        if (!path.contains(".zip")) return null;
        var s = path.split("\\.zip");
        var zip = new ZipFile(s[0] + ".zip");
        var entryPathStream = zip.stream().map(ZipEntry::getName).filter(s1 -> !s1.contains("__MACOSX"));
        var out = entryPathStream.toArray(String[]::new);
        zip.close();
        return out;
    }
}
