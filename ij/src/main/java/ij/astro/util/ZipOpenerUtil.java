package ij.astro.util;

import ij.Prefs;
import ij.gui.GenericDialog;
import ij.plugin.FolderOpener;

import java.io.IOException;
import java.util.Arrays;
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

    public static String[] getFilePathsInZip(String path) {
        return InternalZipFile.getPaths(getFilesInZip(path));
    }

    public static InternalZipFile[] getFilesInZip(String path) {
        InternalZipFile[] x = null;
        try {
            x = getFilesInZipImpl(path);
        } catch (IOException ignored) {}
        if (x == null) return new InternalZipFile[0];
        return x;
    }

    private static InternalZipFile[] getFilesInZipImpl(String path) throws IOException {
        if (!path.contains(".zip")) return null;
        var s = path.split("\\.zip");
        var zip = new ZipFile(s[0] + ".zip");
        var entryPathStream = zip.stream().map(InternalZipFile::buildFromEntry).filter(s1 -> !s1.path.contains("__MACOSX"));
        var out = entryPathStream.toArray(InternalZipFile[]::new);
        zip.close();
        return out;
    }

    public record InternalZipFile(String path, long uncompressedSizeInBytes) {
        public static InternalZipFile buildFromEntry(ZipEntry entry) {
            return new InternalZipFile(entry.getName(), entry.getSize());
        }

        public static String[] getPaths(InternalZipFile[] a) {
            if (a == null || a.length == 0) return new String[0];
            return Arrays.stream(a).map(InternalZipFile::path).toArray(String[]::new);
        }

        public static long getUncompressedSizeInBytes(InternalZipFile[] a) {
            if (a == null || a.length == 0) return 0;
            return Arrays.stream(a).mapToLong(InternalZipFile::uncompressedSizeInBytes).sum();
        }
    }
}
