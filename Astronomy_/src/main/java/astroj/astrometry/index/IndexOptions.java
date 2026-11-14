package astroj.astrometry.index;

import astroj.json.simple.JSONArray;
import astroj.json.simple.JSONObject;
import astroj.json.simple.JSONValue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public final class IndexOptions {
    private final List<CustomLocation> customLocations = new ArrayList<>();
    private final Set<String> disabledSeries = new LinkedHashSet<>();
    private Path indexRoot;

    public IndexOptions(Path indexRoot) {
        this.indexRoot = indexRoot;
    }

    public static IndexOptions load(Path file, Path defaultIndexRoot) {
        var options = new IndexOptions(defaultIndexRoot);
        if (!Files.isRegularFile(file)) {
            return options;
        }

        try {
            var parsed = JSONValue.parse(Files.readString(file, StandardCharsets.UTF_8));
            if (!(parsed instanceof JSONObject root)) {
                return options;
            }

            if (root.get("indexRoot") instanceof String s && !s.isBlank()) {
                options.indexRoot = Path.of(s);
            }

            if (root.get("customLocations") instanceof JSONArray arr) {
                for (var o : arr) {
                    if (o instanceof JSONObject entry
                            && entry.get("id") instanceof String id
                            && entry.get("path") instanceof String path && !path.isBlank()) {
                        options.addCustomLocation(id, Path.of(path));
                    }
                }
            }

            if (root.get("disabledSeries") instanceof JSONArray arr) {
                for (var o : arr) {
                    if (o instanceof String s) {
                        options.disabledSeries.add(s);
                    }
                }
            }
        } catch (IOException | RuntimeException _) {

        }

        return options;
    }

    public Path indexRoot() {
        return indexRoot;
    }

    public void setIndexRoot(Path indexRoot) {
        this.indexRoot = indexRoot;
    }

    public List<CustomLocation> customLocations() {
        return Collections.unmodifiableList(customLocations);
    }

    public boolean hasCustomLocation(String id) {
        return customLocations.stream().anyMatch(c -> c.id().equals(id));
    }

    /// @return false if the id is blank or already used by another custom location
    public boolean addCustomLocation(String id, Path path) {
        id = id == null ? "" : id.trim();
        if (id.isEmpty() || id.contains("/") || hasCustomLocation(id)) {
            return false;
        }

        return customLocations.add(new CustomLocation(id, path.toAbsolutePath().normalize()));
    }

    public boolean removeCustomLocation(String id) {
        disabledSeries.remove(id);
        return customLocations.removeIf(c -> c.id().equals(id));
    }

    public boolean isSeriesEnabled(String id) {
        return !disabledSeries.contains(id);
    }

    public void setSeriesEnabled(String id, boolean enabled) {
        if (enabled) {
            disabledSeries.remove(id);
        } else {
            disabledSeries.add(id);
        }
    }

    public JSONObject toJSON() {
        var obj = new JSONObject();
        obj.put("indexRoot", indexRoot.toString());

        var custom = new JSONArray();
        for (var c : customLocations) {
            var entry = new JSONObject();
            entry.put("id", c.id());
            entry.put("path", c.path().toString());
            custom.add(entry);
        }
        obj.put("customLocations", custom);

        var series = new JSONArray();
        series.addAll(disabledSeries);
        obj.put("disabledSeries", series);

        return obj;
    }

    public void save(Path file) throws IOException {
        var parent = file.toAbsolutePath().getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }

        Files.writeString(file, toJSON().toJSONString(), StandardCharsets.UTF_8);
    }

    /// A user supplied folder of index files. Its contents are not inspected for families.
    /// @param id   user chosen id, unique among all series and custom locations
    /// @param path the folder holding the index files
    public record CustomLocation(String id, Path path) {
    }
}