package astroj.astrometry.index;

import astroj.astrometry.index.IndexOptions.CustomLocation;
import astroj.astrometry.index.data.IndexCatalog;
import astroj.astrometry.index.data.IndexSeries;
import astroj.astrometry.index.data.IndexSeries.IndexFamily;
import astroj.astrometry.index.data.IndexSeries.IndexFamily.IndexFile;
import astroj.json.simple.JSONArray;
import astroj.json.simple.JSONObject;
import astroj.json.simple.JSONValue;
import astroj.json.simple.parser.ParseException;
import ij.IJ;
import ij.astro.io.ConfigHandler;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;

public class AstrometryNetIndexManager {
    public static final String CATALOG_DIRECTORY = "catalog";
    private static final URI DEFAULT_META_URL = URI.create("https://astroimagej.com/meta/astrometry-indexes.json");
    private static final Duration DEFAULT_READ_TIMEOUT = Duration.ofSeconds(15);
    private static final int CONNECT_TIMEOUT_MS = 10_000;
    private static final int READ_TIMEOUT_MS = 15_000;

    private final Path optionsFile;
    private final IndexOptions options;
    private final ExecutorService loader = Executors.newSingleThreadExecutor(r -> {
        var thread = new Thread(r, "AstrometryNetIndexManager-loader");
        thread.setDaemon(true);
        loaderThread = thread;
        return thread;
    });
    private final Object queueLock = new Object();
    /// Held while rebuilding the series list, kept separate from queueLock so readers never wait on a scan's lock.
    private final Object rebuildLock = new Object();
    private volatile Thread loaderThread;
    /// The most recently queued load, the loader runs tasks in order so when this is done all earlier ones are too.
    private volatile CompletableFuture<Void> latest = CompletableFuture.completedFuture(null);
    private volatile Duration readTimeout = DEFAULT_READ_TIMEOUT;

    private volatile List<IndexSeries> remoteSeries = List.of();
    private volatile List<IndexSeries> series = List.of();
    private volatile List<IndexCatalog> catalogs = List.of();
    private volatile String lastFetchError;

    public AstrometryNetIndexManager() {
        this(ConfigHandler.getPath().resolveSibling("index-options.json"));
    }

    private AstrometryNetIndexManager(Path optionsFile) {
        this.optionsFile = optionsFile;
        this.options = IndexOptions.load(optionsFile, getDefaultIndexesDirectory());
    }

    /// Scans the download folder and fetches the remote listing in the background.
    /// Use [#enabledIndexDirectories()] when only the folders are needed.
    public CompletableFuture<Void> loadForGui() {
        return submit(() -> {
            // Local scan first so installed indexes are visible
            rebuild();
            fetchRemoteQuietly();
        });
    }

    private static Path getDefaultIndexesDirectory() {
        if (IJ.isMacOSX()) {
            return Path.of(System.getProperty("user.home"), "Library", "Application Support", "AstroImageJ", "Indexes");
        } else if (IJ.isWindows()) {
            return Path.of(System.getenv("LOCALAPPDATA"), "AstroImageJ", "Indexes");
        } else if (IJ.isLinux()) {
            return Path.of(System.getProperty("user.home"), ".local", "share", "astroimagej", "indexes");
        } else {
            return Path.of(System.getProperty("user.home"), "AstroImageJ-Indexes");
        }
    }

    private static URI metaUrl() {
        var property = System.getProperty("aij.meta.index.url");
        return property == null ? DEFAULT_META_URL : URI.create(property);
    }

    public IndexOptions options() {
        return options;
    }

    public void saveOptions() throws IOException {
        options.save(optionsFile);
    }

    private CompletableFuture<Void> submit(Runnable task) {
        synchronized (queueLock) {
            latest = CompletableFuture.runAsync(task, loader);
            return latest;
        }
    }

    /// @return true while a fetch or scan is queued or running
    public boolean isLoading() {
        return !latest.isDone();
    }

    /// How long readers wait for a pending load before returning the current snapshot.
    public Duration readTimeout() {
        return readTimeout;
    }

    public void setReadTimeout(Duration readTimeout) {
        this.readTimeout = Objects.requireNonNull(readTimeout);
    }

    /// Waits for everything queued so far.
    ///
    /// @return false if the timeout elapsed (or the thread was interrupted) first
    public boolean awaitReady(Duration timeout) {
        if (Thread.currentThread() == loaderThread) {
            return true;
        }

        var pending = latest;

        try {
            pending.get(timeout.toMillis(), TimeUnit.MILLISECONDS);
            return true;
        } catch (TimeoutException e) {
            return false;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        } catch (ExecutionException e) {
            return true;
        }
    }

    public boolean awaitReady() {
        return awaitReady(readTimeout);
    }

    /// Rescans the download folder in the background. Call after changing the index root or after
    /// files change on disk. Readers wait for it.
    public CompletableFuture<Void> refreshLocal() {
        return submit(this::rebuild);
    }

    /// Re-fetches the remote listing in the background, then rescans.
    public CompletableFuture<Void> refreshRemote() {
        return submit(this::fetchRemoteQuietly);
    }

    /// Replaces the remote listing and rescans in the background.
    public CompletableFuture<Void> setRemoteSeries(Collection<IndexSeries> remote) {
        this.remoteSeries = remote == null ? List.of() : List.copyOf(remote);
        return refreshLocal();
    }

    /// The message of the last failed remote fetch, or null if the last fetch worked.
    public String lastFetchError() {
        return lastFetchError;
    }

    private void fetchRemoteQuietly() {
        try {
            fetchSeries(metaUrl());
            lastFetchError = null;
        } catch (IOException | ParseException | RuntimeException e) {
            lastFetchError = e.toString();
            IJ.log("Failed to fetch remote index metadata: " + e);
        }
    }

    /// Remote series merged with what is installed locally.
    public List<IndexSeries> series() {
        awaitReady();
        return series;
    }

    /// Series whose enable checkbox is on.
    public List<IndexSeries> enabledSeries() {
        return series().stream().filter(s -> options.isSeriesEnabled(s.id())).toList();
    }

    /// The catalogs from the remote listing.
    public List<IndexCatalog> catalogs() {
        awaitReady();
        return catalogs;
    }

    public Path catalogDirectory() {
        return options.indexRoot().resolve(CATALOG_DIRECTORY);
    }

    public Optional<Path> locate(IndexCatalog catalog, IndexFile file) {
        return Optional.of(catalogDirectory().resolve(file.name())).filter(Files::isRegularFile);
    }

    public boolean isInstalled(IndexCatalog catalog) {
        return !catalog.files().isEmpty() && catalog.files().stream().allMatch(f -> locate(catalog, f).isPresent());
    }

    private void rebuild() {
        synchronized (rebuildLock) {
            rebuildLocked();
        }
    }

    private void rebuildLocked() {
        var remote = remoteSeries;
        var merged = new ArrayList<IndexSeries>();
        var known = new HashSet<String>();

        for (var s : remote) {
            known.add(s.id());
            var families = new ArrayList<>(s.families());
            var remoteIds = new HashSet<Integer>();
            s.families().forEach(f -> remoteIds.add(f.id()));

            for (var local : IndexFamily.scanDirectory(seriesDirectory(s.id()))) {
                if (!remoteIds.contains(local.id())) {
                    families.add(local);
                }
            }

            merged.add(new IndexSeries(s.id(), s.name(), s.description(), families));
        }

        var root = options.indexRoot();
        if (Files.isDirectory(root)) {
            try (var dirs = Files.list(root)) {
                dirs.filter(Files::isDirectory)
                        .filter(d -> !d.getFileName().toString().equals(CATALOG_DIRECTORY))
                        .filter(d -> !known.contains(d.getFileName().toString()))
                        .filter(d -> !options.hasCustomLocation(d.getFileName().toString()))
                        .sorted()
                        .forEach(d -> {
                            var families = IndexFamily.scanDirectory(d);
                            if (!families.isEmpty()) {
                                var id = d.getFileName().toString();
                                merged.add(new IndexSeries(id, id, "Found locally, not in the remote list", families));
                            }
                        });
            } catch (IOException ignored) {
                // Unreadable folder, show what we have
            }
        }

        this.series = List.copyOf(merged);
    }

    /// Each series gets its own subfolder, as lite 5200 shares file names with the heavy version.
    public Path seriesDirectory(String seriesId) {
        return options.indexRoot().resolve(seriesId);
    }

    public Path seriesDirectory(IndexSeries series) {
        return seriesDirectory(series.id());
    }

    public Optional<Path> locate(IndexSeries series, IndexFile file) {
        return Optional.of(seriesDirectory(series).resolve(file.name())).filter(Files::isRegularFile);
    }

    public boolean isInstalled(IndexSeries series, IndexFamily family) {
        return !family.files().isEmpty() && family.files().stream().allMatch(f -> locate(series, f).isPresent());
    }

    /// Downloaded families follow their series and cannot be switched individually.
    public boolean canToggleFamily(IndexSeries series, IndexFamily family) {
        return !isInstalled(series, family);
    }

    /// Whether the family is in effect: its series is enabled and it is either downloaded or wanted.
    public boolean isFamilyEnabled(IndexSeries series, IndexFamily family) {
        return options.isSeriesEnabled(series.id()) && (isInstalled(series, family));
    }

    /// Enabled index files that exist locally in the series folders, and the index files of
    /// enabled custom locations (its `.fits` files, not inspected further).
    public List<Path> enabledIndexFiles() {
        var found = new LinkedHashSet<Path>();
        for (var dir : enabledIndexDirectories()) {
            try (var files = Files.list(dir)) {
                files.filter(Files::isRegularFile)
                        .filter(p -> p.getFileName().toString().endsWith(".fits"))
                        .sorted()
                        .forEach(found::add);
            } catch (IOException ignored) {
                // Treat an unreadable folder as empty
            }
        }

        return new ArrayList<>(found);
    }

    /// The existing folders containing enabled index files. This only lists the subfolders of the
    /// index root and checks them against the options.
    public Set<Path> enabledIndexDirectories() {
        var dirs = new LinkedHashSet<Path>();

        var root = options.indexRoot();
        if (Files.isDirectory(root)) {
            try (var subDirs = Files.list(root)) {
                subDirs.filter(Files::isDirectory)
                        .sorted()
                        .forEach(d -> {
                            var id = d.getFileName().toString();
                            if (!id.equals(CATALOG_DIRECTORY) &&
                                    !options.hasCustomLocation(id) && options.isSeriesEnabled(id)) {
                                dirs.add(d.toAbsolutePath());
                            }
                        });
            } catch (IOException ignored) {
                // Unreadable folder, treat as empty
            }
        }

        for (CustomLocation customLocation : options.customLocations()) {
            if (options.isSeriesEnabled(customLocation.id()) && Files.isDirectory(customLocation.path())) {
                dirs.add(customLocation.path().toAbsolutePath());
            }
        }

        return dirs;
    }

    /// @return true if no series or custom location already uses the id
    public boolean isIdAvailable(String id) {
        return id != null && !id.isBlank() && !id.contains("/") && !id.equals(CATALOG_DIRECTORY) &&
                !options.hasCustomLocation(id) && series().stream().noneMatch(s -> s.id().equals(id));
    }

    /// Fetches and parses the series listing, then rescans.
    public void fetchSeries(URI source) throws IOException, ParseException {
        var connection = source.toURL().openConnection();
        connection.setConnectTimeout(CONNECT_TIMEOUT_MS);
        connection.setReadTimeout(READ_TIMEOUT_MS);

        String text;
        try (var in = connection.getInputStream()) {
            text = new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }

        var parsed = parseListing(text);
        if (parsed != null) {
            this.catalogs = List.copyOf(parsed.catalogs());
            this.remoteSeries = List.copyOf(parsed.series());
            rebuild();
        }
    }

    /// Downloads any missing files of the family into its series folder, verifying each hash.
    /// Existing files with the wrong hash are replaced. Locally discovered files have no known
    /// hash and are left alone.
    public void download(IndexSeries series, IndexFamily family, DownloadListener listener) throws IOException {
        downloadFiles(seriesDirectory(series), family.files(), listener);
        refreshLocal();
    }

    /// Downloads any missing files of the catalog into the catalog folder, verifying each hash.
    public void downloadCatalog(IndexCatalog catalog, DownloadListener listener) throws IOException {
        downloadFiles(catalogDirectory(), catalog.files(), listener);
    }

    private void downloadFiles(Path dir, List<IndexFile> files, DownloadListener listener) throws IOException {
        Files.createDirectories(dir);

        for (var file : files) {
            var fileName = file.name();
            if (fileName.isEmpty() || fileName.equals(".") || fileName.equals("..")
                    || !fileName.equals(Path.of(fileName).getFileName().toString())) {
                throw new IOException("Invalid file name: " + fileName);
            }

            if (file.sha256().isEmpty() || file.source() == null) {
                continue;
            }

            if (listener != null) {
                listener.fileStarted(file);
            }

            var target = dir.resolve(file.name());
            if (Files.isRegularFile(target) && IndexFamily.sha256(target).equalsIgnoreCase(file.sha256())) {
                if (listener != null) {
                    listener.fileFinished(file);
                }

                continue;
            }

            var part = dir.resolve(file.name() + ".part");
            var connection = file.source().toURL().openConnection();
            connection.setConnectTimeout(CONNECT_TIMEOUT_MS);
            connection.setReadTimeout(READ_TIMEOUT_MS);
            long total = connection.getContentLengthLong();
            if (total <= 0) {
                total = file.size() > 0 ? file.size() : -1;
            }

            try (var in = connection.getInputStream(); var out = Files.newOutputStream(part,
                    StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE)) {
                var buffer = new byte[64 * 1024];
                long read = 0;
                int n;
                if (listener != null) {
                    listener.fileProgress(file, 0, total);
                }

                while ((n = in.read(buffer)) != -1) {
                    out.write(buffer, 0, n);
                    read += n;
                    if (listener != null) {
                        listener.fileProgress(file, read, total);
                    }
                }
            }

            if (!IndexFamily.sha256(part).equalsIgnoreCase(file.sha256())) {
                Files.deleteIfExists(part);
                throw new IOException("Checksum mismatch for " + file.name());
            }

            Files.move(part, target, StandardCopyOption.REPLACE_EXISTING);
            if (listener != null) {
                listener.fileFinished(file);
            }
        }

    }

    private Listing parseListing(String text) throws ParseException {
        var json = JSONValue.parseWithException(text);

        if (!(json instanceof JSONObject root)) {
            return null;
        }

        if (!(root.get("series") instanceof JSONArray inSeries)) {
            return null;
        }

        var indexSeries = new ArrayList<IndexSeries>(inSeries.size());
        for (var o : inSeries) {
            if (!(o instanceof JSONObject entry)) {
                continue;
            }

            if (!(entry.get("id") instanceof String seriesId)) {
                continue;
            }

            if (!(entry.get("name") instanceof String seriesName)) {
                continue;
            }

            String seriesDesc = null;
            if (entry.get("description") instanceof String seriesDesc1) {
                seriesDesc = seriesDesc1;
            }

            if (!(entry.get("families") instanceof JSONArray seriesFamilies)) {
                continue;
            }

            var seriesIndexFamilies = new ArrayList<IndexFamily>(seriesFamilies.size());
            for (var seriesFamily : seriesFamilies) {
                if (!(seriesFamily instanceof JSONObject family)) {
                    continue;
                }

                if (!(family.get("id") instanceof Number seriesFamilyId)) {
                    continue;
                }

                if (!(family.get("scaleLower") instanceof Number scaleLower)) {
                    continue;
                }

                if (!(family.get("scaleUpper") instanceof Number scaleUpper)) {
                    continue;
                }

                if (!(family.get("files") instanceof JSONArray files)) {
                    continue;
                }

                var indexFiles = new ArrayList<IndexFile>(files.size());
                for (var file : files) {
                    if (!(file instanceof JSONObject filesData)) {
                        continue;
                    }

                    if (!(filesData.get("name") instanceof String name)) {
                        continue;
                    }

                    if (!(filesData.get("sha256") instanceof String hash)) {
                        continue;
                    }

                    if (!(filesData.get("source") instanceof String source)) {
                        continue;
                    }

                    var size = filesData.get("size") instanceof Number n ? n.longValue() : 0L;

                    indexFiles.add(new IndexFile(name, hash, URI.create(source), size));
                }

                seriesIndexFamilies.add(new IndexFamily(seriesFamilyId.intValue(), scaleLower.doubleValue(), scaleUpper.doubleValue(), indexFiles));
            }

            indexSeries.add(new IndexSeries(seriesId, seriesName, seriesDesc, seriesIndexFamilies));
        }

        return new Listing(indexSeries, parseCatalogs(root.get("catalog")));
    }

    private static List<IndexCatalog> parseCatalogs(Object value) {
        if (!(value instanceof JSONArray array)) {
            return List.of();
        }

        var out = new ArrayList<IndexCatalog>(array.size());
        for (var o : array) {
            if (!(o instanceof JSONObject entry)
                    || !(entry.get("file") instanceof String name)
                    || !(entry.get("hash") instanceof String hash)
                    || !(entry.get("url") instanceof String url)) {
                continue;
            }

            var size = entry.get("size") instanceof Number n ? n.longValue() : 0L;
            out.add(new IndexCatalog(name, null, List.of(new IndexFile(name, hash, URI.create(url), size))));
        }

        return out;
    }

    private record Listing(List<IndexSeries> series, List<IndexCatalog> catalogs) {
    }

    /// Receives download progress. Called from the downloading thread.
    public interface DownloadListener {
        /// A file is about to be processed.
        default void fileStarted(IndexFile file) {
        }

        /// Bytes received so far for the current file. `total` is -1 if unknown.
        default void fileProgress(IndexFile file, long bytesRead, long total) {
        }

        /// The file is done, either downloaded or already present and valid.
        default void fileFinished(IndexFile file) {
        }
    }
}