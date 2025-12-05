package Astronomy.updater;

import java.io.IOException;
import java.net.URI;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import Astronomy.AstroImageJUpdaterV6;
import astroj.json.simple.JSONArray;
import astroj.json.simple.JSONObject;
import astroj.json.simple.parser.JSONParser;
import astroj.json.simple.parser.ParseException;
import ij.IJ;
import ij.astro.gui.ToolTipProvider;

public record MetaVersion(MetadataVersion version, List<VersionEntry> versions) {
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM);

    public MetaVersion {
        Objects.requireNonNull(version);
        Objects.requireNonNull(versions);
    }

    public static MetaVersion readJson(URI uri) {
        try (var reader = AstroImageJUpdaterV6.readerForUri(uri)) {
            var o = new JSONParser().parse(reader);

            if (o instanceof JSONObject object) {
                if (object.get("metaVersion") instanceof JSONObject meta) {
                    var mVersion = MetadataVersion.fromJson(meta);
                    if (mVersion.major() > 1) {
                        System.out.println("Unsupported meta version: " + mVersion);
                        return null;
                    }
                    if (object.get("versions") instanceof JSONArray array) {
                        var versions = new ArrayList<VersionEntry>();
                        for (Object o1 : array) {
                            if (o1 instanceof JSONObject v) {
                                versions.add(VersionEntry.fromJson(v));
                            }
                        }

                        return new MetaVersion(mVersion, versions);
                    }
                }
            }
        } catch (IOException | ParseException | InterruptedException e) {
            IJ.error("Updater", "Failed to read metadata: " + e.getMessage());
        }

        return null;
    }

    public record MetadataVersion(int major, int minor) {
        public static MetadataVersion fromJson(JSONObject object) {
            var major = 0;
            var minor = 0;
            if (object.get("major") instanceof Number i) {
                major = i.intValue();
            }

            if (object.get("minor") instanceof Number i) {
                minor = i.intValue();
            }
            return new MetadataVersion(major, minor);
        }
    }

    public record VersionEntry(SemanticVersion version, String url, ReleaseType releaseType, Instant releaseTime) implements ToolTipProvider {
        public VersionEntry {
            Objects.requireNonNull(version);
            Objects.requireNonNull(releaseType);
            Objects.requireNonNull(url);
        }

        public static VersionEntry fromJson(JSONObject object) {
            var type = ReleaseType.valueOf((String) object.get("type"));

            var url = object.get("url");

            Instant releaseTime = null;
            if (object.get("releaseTime") != null) {
                try {
                    releaseTime = Instant.parse((String) object.get("releaseTime"));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            return new VersionEntry(new SemanticVersion((String) object.get("version")), (String) url, type, releaseTime);
        }

        @Override
        public String toString() {
            var sb = new StringBuilder();
            sb.append(version);
            sb.append(" ");
            sb.append("(").append(releaseType).append(")");

            return sb.toString();
        }

        @Override
        public String getToolTip() {
            if (releaseTime != null) {
                return DATE_TIME_FORMATTER.format(releaseTime.atZone(ZoneId.systemDefault()));
            }

            return "No release date present";
        }
    }

    public enum ReleaseType {
        RELEASE("Release"),
        DAILY_BUILD("Daily Build"),
        ALPHA("Alpha"),
        BETA("Beta"),
        PRERELEASE("Prerelease");

        private final String name;

        ReleaseType(String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return name;
        }
    }
}
