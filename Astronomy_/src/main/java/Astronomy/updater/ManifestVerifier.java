package Astronomy.updater;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

import astroj.json.simple.JSONArray;
import astroj.json.simple.JSONObject;
import astroj.json.simple.parser.JSONParser;
import ij.IJ;

public class ManifestVerifier {
    private static Manifest readManifest(Path baseDirPath, Path manifestPath) throws Exception {
        var parser = new JSONParser();
        try (Reader r = Files.newBufferedReader(manifestPath)) {
            var root = (JSONObject) parser.parse(r);
            var arr = (JSONArray) root.get("entries");
            var list = new ArrayList<ManifestEntry>();
            if (arr != null) {
                for (Object o : arr) {
                    if (!(o instanceof JSONObject jo)) continue;
                    var p = jo.get("path");
                    var m = jo.get("md5");
                    var path = (p != null) ? p.toString() : null;
                    var md5 = (m != null) ? m.toString() : null;
                    if (path != null) {
                        list.add(new ManifestEntry(path, md5 != null ? md5.toUpperCase(Locale.US) : ""));
                    }
                }
            }

            return new Manifest(baseDirPath, list);
        }
    }

    public static boolean check(Path baseDirPath, Path manifestPath, int maxChanges) {
        try {
            var changes = checkManifest(baseDirPath, manifestPath, maxChanges);
            if (changes.isEmpty()) {
                return true;
            }

            var sb = new StringBuilder();
            for (Change change : changes) {
                sb.append(change).append('\n');
            }

            return IJ.showMessageWithCancel("Updater", """
                   AstroImageJ found at least %s change(s) to the installation directory.
                   AstroImageJ update will OVERWRITE or REMOVE the files in its install directory.
                   Please make sure AIJ has a dedicated install directory, not shared with other software.
                   \s
                   Install directory: %s
                   Changes:
                   %s
                   """.formatted(changes.size(), baseDirPath.toAbsolutePath(), sb.toString()));
        } catch (Exception e) {
            return IJ.showMessageWithCancel("Updater", """
                   Failed to verify installation was not modified.
                   AstroImageJ update will OVERWRITE or REMOVE the files in its install directory.
                   Please make sure AIJ has a dedicated install directory, not shared with other software.
                   \s
                   Install directory: %s
                   """.formatted(baseDirPath.toAbsolutePath()));
        }
    }

    /**
     * Check baseDir against manifest. Paths in manifest use "/" as the separator.
     *
     * @param baseDirPath base install directory to scan
     * @param manifestPath path to manifest.json file
     * @param maxChanges stop early when unknown+modified reaches this threshold
     */
    private static List<Change> checkManifest(Path baseDirPath, Path manifestPath, int maxChanges) throws Exception {
        if (maxChanges <= 0) throw new IllegalArgumentException("maxChanges must be >= 1");
        Objects.requireNonNull(manifestPath);

        var manifest = readManifest(baseDirPath, manifestPath);

        // walk the directory and use iterator so we can break early
        try (var walk = Files.walk(baseDirPath)) {
            return walk.parallel().filter(Files::isRegularFile)
                    .filter(p -> {
                        try {
                            if (Files.isHidden(p)) {
                                return false;
                            }
                        } catch (IOException e) {
                            return true;
                        }

                        if (IJ.isMacOSX()) {
                            if (p.endsWith(".DS_Store")) {
                                return false;
                            }

                            // Ignore files add by signing
                            if (p.toString().contains("_CodeSignature")) {
                                return false;
                            }

                            if (Files.isExecutable(p)) {
                                // Mac signing modifies the executable
                                return false;
                            }
                        }

                        return !p.endsWith("manifest.json") && !p.endsWith(".package");
                    })
                    // Relativize the paths to match what is in the manifest
                    .map(baseDirPath::relativize)
                    .map(manifest::checkEntry)
                    .filter(Change::isProblem)
                    .limit(maxChanges)
                    .toList();
        }

        // any manifest entries not seen are missing
        /*for (var manifestKey : manifestMap.keySet()) {
            if (!seenManifestPaths.contains(manifestKey)) {
                report.missingFiles.add(manifestKey);
            }
        }*/
    }

    private static Path toPath(String p) {
        var el = p.split("/");
        if (el.length > 1) {
            return Path.of(el[0], Arrays.copyOfRange(el, 1, el.length));
        } else {
            return Path.of(el[0]);
        }
    }

    private static String md5HexOfFile(Path p) throws IOException {
        try (InputStream in = Files.newInputStream(p)) {
            MessageDigest md = MessageDigest.getInstance("MD5");
            try (DigestInputStream dis = new DigestInputStream(in, md)) {
                byte[] buffer = new byte[8192];
                while (dis.read(buffer) != -1) {
                    // consuming stream to update digest
                }
            }
            byte[] digest = md.digest();
            return toHexUpper(digest);
        } catch (Exception e) {
            throw new IOException("Failed to compute MD5 for " + p, e);
        }
    }

    private static String toHexUpper(byte[] data) {
        return HexFormat.of().withUpperCase().formatHex(data);
    }

    record ManifestEntry(String path, String md5) {}

    record Manifest(Path baseDirPath, List<ManifestEntry> entries, Map<Path, String> map) {
        Manifest(Path baseDirPath, List<ManifestEntry> entries) {
            this(baseDirPath, entries, toMap(entries));
        }

        public Change checkEntry(Path path) {
            String expectedMd5 = map.get(path);
            if (expectedMd5 == null) {
                return new Change.Unknown(path);
            }

            // compute MD5
            String actualMd5;
            try {
                actualMd5 = md5HexOfFile(baseDirPath.resolve(path));
            } catch (IOException ex) {
                ex.printStackTrace();
                return new Change.Modified(path);
            }

            if (!expectedMd5.equalsIgnoreCase(actualMd5)) {
                return new Change.Modified(path);
            }

            return new Change.Unchanged();
        }

        private static Map<Path, String> toMap(List<ManifestEntry> entries) {
            var manifestMap = new HashMap<Path, String>();
            for (ManifestEntry e : entries) {
                if (e == null || e.path == null) continue;
                manifestMap.put(toPath(e.path), e.md5 != null ? e.md5.toUpperCase(Locale.US) : "");
            }

            return manifestMap;
        }
    }

    sealed interface Change {
        record Unchanged() implements Change {
            @Override
            public boolean isProblem() {
                return false;
            }
        }
        record Unknown(Path path) implements Change {}
        record Modified(Path path) implements Change {}
        record Missing(Path path) implements Change {}

        default boolean isProblem() {
            return true;
        }
    }
}
