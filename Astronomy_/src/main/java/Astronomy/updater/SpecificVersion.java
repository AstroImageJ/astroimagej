package Astronomy.updater;

import java.io.IOException;
import java.net.URI;
import java.util.Locale;
import java.util.Objects;

import Astronomy.AstroImageJUpdaterV6;
import astroj.json.simple.JSONArray;
import astroj.json.simple.JSONObject;
import astroj.json.simple.parser.JSONParser;
import astroj.json.simple.parser.ParseException;
import ij.IJ;

public record SpecificVersion(SemanticVersion version, String message, FileEntry[] files) {
    public SpecificVersion {
        Objects.requireNonNull(version);
        Objects.requireNonNull(files);
    }

    public static SpecificVersion readJson(URI uri) {
        try (var reader = AstroImageJUpdaterV6.readerForUri(uri)) {
            var o = new JSONParser().parse(reader);

            if (o instanceof JSONObject object) {
                return new SpecificVersion(new SemanticVersion((String) object.get("version")), (String) object.get("message"),
                        FileEntry.fromJson((JSONArray) object.get("artifacts")));
            }
        } catch (IOException | ParseException | InterruptedException e) {
            e.printStackTrace();
        }

        return null;
    }

    public record FileEntry(String name, String url, String sha256, Os[] os, Arch[] arch, String signatureUrl, String signatureSha256) {
        public FileEntry {
            Objects.requireNonNull(name);
            Objects.requireNonNull(url);
            Objects.requireNonNull(sha256);
            Objects.requireNonNull(os);
            Objects.requireNonNull(arch);
            Objects.requireNonNull(signatureUrl);
            Objects.requireNonNull(signatureSha256);
        }

        public static FileEntry[] fromJson(JSONArray array) {
            var fileEntries = new FileEntry[array.size()];
            for (int i = 0; i < array.size(); i++) {
                fileEntries[i] = fromJson((JSONObject) array.get(i));
            }

            return fileEntries;
        }

        public static FileEntry fromJson(JSONObject object) {
            Os[] os = null;
            Arch[] arch = null;
            if (object.get("os") instanceof JSONArray array) {
                os = Os.fromJson(array);
            }

            if (object.get("arch") instanceof JSONArray array) {
                arch = Arch.fromJson(array);
            }

            return new FileEntry((String) object.get("name"),
                    (String) object.get("url"), (String) object.get("sha256"), os, arch,
                    (String) object.get("signatureUrl"), (String) object.get("signatureSha256"));
        }

        public boolean matchOs() {
            for (Os o : os) {
                if (IJ.isMacOSX() && o == Os.MAC) {
                    return true;
                }

                if (IJ.isLinux() && o == Os.LINUX) {
                    return true;
                }

                if (IJ.isWindows() && o == Os.WINDOWS) {
                    return true;
                }
            }

            return false;
        }

        public boolean matchArch() {
            for (Arch a : arch) {
                if (a == Arch.getArch()) {
                    return true;
                }
            }

            return false;
        }

        public boolean matchesSystem() {
            return matchArch() && matchOs();
        }
    }

    public enum Os {
        WINDOWS,
        LINUX,
        MAC,
        ;

        public static Os[] fromJson(JSONArray array) {
            if (array == null) {
                return new Os[0];
            }

            var os = new Os[array.size()];
            for (int i = 0; i < array.size(); i++) {
                os[i] = fromJson((String) array.get(i));
            }

            return os;
        }

        public static Os fromJson(String s) {
            return Os.valueOf(s.toUpperCase(Locale.ENGLISH));
        }
    }

    public enum Arch {
        X86,
        AMD64,
        ARM64;

        public static Arch getArch() {
            return switch (System.getProperty("os.arch").toLowerCase(Locale.ENGLISH)) {
                case "amd64", "x86_64", "x86-64", "x8664", "ia32e", "em64t", "x64" -> AMD64;
                case "aarch64", "arm" -> ARM64;
                case "x86", "i386", "i486", "i586", "i686", "x8632", "ia32", "x32" -> X86;

                default -> throw new UnsupportedOperationException("Unknown architecture: " + System.getProperty("os.arch"));
            };
        }

        public static Arch[] fromJson(JSONArray array) {
            if (array == null || array.isEmpty()) {
                throw new IllegalArgumentException("Architecture must have value");
            }

            var os = new Arch[array.size()];
            for (int i = 0; i < array.size(); i++) {
                os[i] = fromJson((String) array.get(i));
            }

            return os;
        }

        public static Arch fromJson(String s) {
            return Arch.valueOf(s.toUpperCase(Locale.ENGLISH));
        }
    }
}
