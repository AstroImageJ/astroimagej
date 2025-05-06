package Astronomy.updater;

import Astronomy.AstroImageJUpdaterV6;
import astroj.json.simple.JSONArray;
import astroj.json.simple.JSONObject;
import astroj.json.simple.parser.JSONParser;
import astroj.json.simple.parser.ParseException;

import java.io.IOException;
import java.net.URI;
import java.util.Locale;
import java.util.Objects;

public record SpecificVersion(SemanticVersion version, String message, FileEntry[] files, LaunchArg[] launchArgs) {
    public SpecificVersion {
        Objects.requireNonNull(version);
        Objects.requireNonNull(files);
        if (launchArgs == null) {
            launchArgs = new LaunchArg[0];
        }
    }

    public static SpecificVersion readJson(URI uri) {
        try (var reader = AstroImageJUpdaterV6.readerForUri(uri)) {
            var o = new JSONParser().parse(reader);

            if (o instanceof JSONObject object) {
                return new SpecificVersion(new SemanticVersion((String) object.get("version")), (String) object.get("message"),
                        FileEntry.fromJson((JSONArray) object.get("files")), LaunchArg.fromJson((JSONArray) object.get("launchArg")));
            }
        } catch (IOException | ParseException | InterruptedException e) {
            e.printStackTrace();
        }

        return null;
    }

    public record FileEntry(String name, String destination, String url, String md5, Os[] os) {
        public FileEntry {
            Objects.requireNonNull(name);
            Objects.requireNonNull(destination);
            Objects.requireNonNull(url);
            Objects.requireNonNull(md5);
            if (os == null) {
                os = new Os[0];
            }
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
            if (object.get("os") instanceof JSONArray array) {
                os = Os.fromJson(array);
            }

            return new FileEntry((String) object.get("name"), (String) object.get("destination"),
                    (String) object.get("url"), (String) object.get("md5"), os);
        }
    }

    public record LaunchArg(String arg, Os[] os) {
        public LaunchArg {
            Objects.requireNonNull(arg);
            if (os == null) {
                os = new Os[0];
            }
        }

        public static LaunchArg[] fromJson(JSONArray array) {
            if (array == null) {
                return new LaunchArg[0];
            }

            var args = new LaunchArg[array.size()];
            for (int i = 0; i < array.size(); i++) {
                args[i] = fromJson((JSONObject) array.get(i));
            }

            return args;
        }

        public static LaunchArg fromJson(JSONObject object) {
            Os[] os = null;
            if (object.get("os") instanceof JSONArray array) {
                os = Os.fromJson(array);
            }

            return new LaunchArg((String) object.get("arg"), os);
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
}
