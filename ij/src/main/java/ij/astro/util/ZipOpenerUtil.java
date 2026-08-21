package ij.astro.util;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import ij.Prefs;
import ij.gui.GenericDialog;
import ij.plugin.FolderOpener;

/**
 * Utilities for opening a zip file as a folder.
 */
public class ZipOpenerUtil {
    private static final Map<Path, FileSystem> ZIPS = Collections.synchronizedMap(new HashMap<>());
    private static final Map<String, Path> PATHS = Collections.synchronizedMap(new HashMap<>());

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
        try {
            InternalZipFile[] files = getFilesInZipImpl(path);
            return files != null ? files : new InternalZipFile[0];
        } catch (IOException e) {
            e.printStackTrace();
            return new InternalZipFile[0];
        }
    }

    /**
     * Opens a zip file and returns a list of its entries.
     * <p>
     * If the provided path represents a ZIP entry (e.g. "archive.zip/entry/folder"),
     * then only entries whose names start with the internal path are returned.
     * </p>
     *
     * @param path full path to a zip file or to an entry within a zip file
     * @return an array of InternalZipFile entries, or null if the path does not
     *         contain ".zip"
     */
    // The subfolder limitation is important for when we scan the zip a second time in #getFilePathsInZip
    private static InternalZipFile[] getFilesInZipImpl(String path) throws IOException {
        String lowerCasePath = path.toLowerCase();
        int zipIndex = lowerCasePath.indexOf(".zip");
        if (zipIndex < 0) return null;

        String zipFilePath = path.substring(0, zipIndex + 4);

        // Verify that the zip file exists
        File zipFileOnDisk = new File(zipFilePath);
        if (!zipFileOnDisk.exists() || !zipFileOnDisk.isFile()) {
            return null;
        }

        // Check if there is an internal path (i.e. a slash immediately after ".zip")
        String internalPath;
        if (path.length() > zipIndex + 4) {
            char nextChar = path.charAt(zipIndex + 4);
            if (nextChar == '/' || nextChar == '\\') {
                internalPath = path.substring(zipIndex + 5);
            } else {
                internalPath = "";
            }
        } else {
            internalPath = "";
        }

        var absPath = PATHS.computeIfAbsent(zipFilePath, _ -> zipFileOnDisk.toPath().toAbsolutePath());

        //noinspection resource
        ZIPS.computeIfAbsent(absPath, k -> {
            try {
                return FileSystems.newFileSystem(k);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });

        try (ZipFile zip = new ZipFile(zipFilePath)) {
            return zip.stream()
                    .map(InternalZipFile::buildFromEntry)
                    .filter(entry -> !entry.path.contains("__MACOSX"))
                    // If an internal path was specified, only include entries that start with it.
                    .filter(entry -> internalPath.isEmpty() || entry.path.startsWith(internalPath))
                    .toArray(InternalZipFile[]::new);
        }
    }

    /// @param path the path to the zip file
    /// @return the ZipFile
    public static FileSystem getZipFile(String path) {
        String lowerCasePath = path.toLowerCase();
        int zipIndex = lowerCasePath.indexOf(".zip");
        if (zipIndex < 0) {
            return null;
        }

        String zipFilePath = path.substring(0, zipIndex + 4);

        var absPath = PATHS.computeIfAbsent(zipFilePath, k -> Paths.get(k).toAbsolutePath());

        return ZIPS.computeIfAbsent(absPath, k -> {
            try {
                return FileSystems.newFileSystem(k);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }

    public static void closeZipFile(String path) {
        String lowerCasePath = path.toLowerCase();
        int zipIndex = lowerCasePath.indexOf(".zip");
        if (zipIndex < 0) return;

        String zipFilePath = path.substring(0, zipIndex + 4);

        var absPath = PATHS.computeIfAbsent(zipFilePath, k -> Paths.get(k).toAbsolutePath());

        try {
            ZIPS.remove(absPath).close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

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
