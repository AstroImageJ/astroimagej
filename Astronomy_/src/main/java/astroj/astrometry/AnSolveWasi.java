package astroj.astrometry;

import ai.tegmentum.wasmtime4j.*;
import ai.tegmentum.wasmtime4j.Module;
import ai.tegmentum.wasmtime4j.exception.I32ExitException;
import ai.tegmentum.wasmtime4j.factory.WasmRuntimeFactory;
import ai.tegmentum.wasmtime4j.func.HostFunction;
import ai.tegmentum.wasmtime4j.type.FunctionType;
import ai.tegmentum.wasmtime4j.wasi.WasiLinker;
import ai.tegmentum.wasmtime4j.wasi.WasiStdioConfig;
import ij.IJ;
import nom.tam.fits.BinaryTable;
import nom.tam.fits.Fits;
import nom.tam.util.FitsOutputStream;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.OverlappingFileLockException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.BooleanSupplier;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;

public class AnSolveWasi implements AutoCloseable {
    private static final boolean MIRROR_LOG = true;
    /// Guest path of the folder holding the config and one sub-folder per job; also the value of AN_HOST_JOBS
    private static final String GUEST_WORK = "/work";
    /// Name (inside the work folder) of the file whose appearance makes the engine cancel the running job
    private static final String CANCEL_FILE = "cancel";
    /// How often solve() checks the canceled supplier while the engine is working
    private static final long CANCEL_POLL_MS = 100;
    /// Status in {@link EngineSession#results}
    private static final int DONE = 0, FAILED = 1, ENDED = 2;
    private final WasmRuntime wasmRuntime;
    private final Engine wasmEngine;
    private final Module augmentModule, astrometryEngineModule;
    private final boolean useHostMemory;
    private final BooleanSupplier cancelled;
    private final Module kdTreeModule;
    /// Open kd-trees
    private final Map<String, KdSession> kdSessions = new HashMap<>();
    private Engine hostWasmEngine;
    private HostMmap.Creator creator;
    private EngineSession session;
    private WorkDir workDir;

    public AnSolveWasi(Path augmentWasm, Path engineWasm, Path kdTree, boolean useHostMemory, BooleanSupplier cancelled) throws Exception {
        this(Files.readAllBytes(augmentWasm), Files.readAllBytes(engineWasm), Files.readAllBytes(kdTree), useHostMemory, cancelled);
    }

    public AnSolveWasi(byte[] augmentWasm, byte[] engineWasm, byte[] kdTree, boolean useHostMemory, BooleanSupplier cancelled) throws Exception {
        this.cancelled = cancelled;
        // Disable wasmtime logging as it runs every time the engine is created
        Logger.getLogger("ai.tegmentum.wasmtime4j.factory.WasmRuntimeFactory").setLevel(Level.OFF);
        this.useHostMemory = useHostMemory;
        Module astrometryEngineLocal;
        wasmRuntime = WasmRuntimeFactory.create();
        wasmEngine = wasmRuntime.createEngine(Engine.builder().cacheConfigLoadDefault());
        augmentModule = compile("augment-xylist", augmentWasm);
        if (useHostMemory) {
            try {
                creator = new HostMmap.Creator();
                hostWasmEngine = wasmRuntime.createEngine(Engine.builder()
                        .memoryInitCow(false)//todo enable initcow?
                        .cacheConfigLoadDefault()
                        //.setProfiling(ProfilingStrategy.VTUNE)
                        .withHostMemory(creator)
                );
                astrometryEngineLocal = hostWasmEngine.compileModule(engineWasm);
            } catch (Throwable t) {
                hostWasmEngine = null;
                creator = null;
                astrometryEngineLocal = null;
            }
        } else {
            astrometryEngineLocal = compile("astrometry-engine", engineWasm);
        }
        astrometryEngineModule = astrometryEngineLocal;
        kdTreeModule = compile("kdtree", kdTree);
    }

    private static byte[] fitsXylist(List<double[]> xy) throws IOException {
        try (var fits = new Fits(); var byteOut = new ByteArrayOutputStream(); var out = new FitsOutputStream(byteOut)) {
            var table = new BinaryTable();
            table.addColumn(BinaryTable.ColumnDesc.createForScalars(double.class).name("X"));
            table.addColumn(BinaryTable.ColumnDesc.createForScalars(double.class).name("Y"));
            for (double[] coords : xy) {
                table.addRowEntries(coords[0], coords[1]);
            }

            fits.addHDU(table.toHDU());
            fits.write(out);

            return byteOut.toByteArray();
        }
    }

    /// Get the exit code if available
    private static Integer exitCode(Throwable ex) {
        for (Throwable t = ex; t != null; t = t.getCause()) {
            if (t instanceof I32ExitException x) return x.getExitCode();
        }
        return null;
    }

    /// Calls an exported function that takes no arguments and returns an int.
    private static int callInt(Instance inst, String function) throws Exception {
        var r = inst.callFunction(function);
        return r != null && r.length > 0 ? r[0].asInt() : 0;
    }

    private static int callInt(Instance inst, String function, WasmValue... args) throws Exception {
        var r = inst.callFunction(function, args);
        return r != null && r.length > 0 ? r[0].asInt() : 0;
    }

    private static double callDouble(Instance inst, String function, WasmValue... args) throws Exception {
        return inst.callFunction(function, args)[0].asDouble();
    }

    private static void deleteTree(Path dir) {
        if (dir == null || !Files.exists(dir)) {
            return;
        }
        try (Stream<Path> s = Files.walk(dir)) {
            //noinspection ResultOfMethodCallIgnored
            s.sorted(Comparator.reverseOrder()).forEach(p -> p.toFile().delete());
        } catch (IOException _) {
        }
    }

    /// Deletes everything inside dir except the lock file, keeping the folder itself.
    private static void clearDir(Path dir) {
        try (Stream<Path> s = Files.list(dir)) {
            s.filter(p -> !p.getFileName().toString().equals(WorkDir.LOCK)).forEach(AnSolveWasi::deleteTree);
        } catch (IOException _) {
        }
    }

    private Module compile(String name, byte[] wasm) throws Exception {
        return compile(name, wasm, wasmEngine);
    }

    private Module compile(String name, byte[] wasm, Engine eng) throws Exception {
        return eng.compileModule(wasm);
    }

    /// Runs a WASI command module via calling its main method with argv and preopened folders.
    /// @return the program's exit code
    private int runWasi(String name, Module m, List<String> argv, Map<Path, String> preopens) throws Exception {
        try (WasiLinker linker = WasiLinker.create(wasmEngine);
             Store store = wasmEngine.createStore()) {
            if (MIRROR_LOG) {
                //todo have option to mirror to log
                linker.configureStdout(WasiStdioConfig.nulled());
                linker.configureStderr(WasiStdioConfig.nulled());
                // Verbose log seems to goes into stdin?
                linker.configureStdin(WasiStdioConfig.nulled());
            }
            linker.setArguments(argv);
            for (Map.Entry<Path, String> e : preopens.entrySet()) {
                if (Files.notExists(e.getKey())) {
                    Files.createDirectories(e.getKey());
                }
                if (Files.isDirectory(e.getKey())) {
                    linker.allowDirectoryAccess(e.getKey(), e.getValue());
                } else {
                    IO.println("%s is not a directory!".formatted(e.getKey()));
                }
            }
            Instance inst = linker.instantiate(store, m);
            int rc = 0;
            try {
                inst.callFunction("_start"); // returns without proc_exit: rc stays 0
            } catch (Exception ex) {
                var code = exitCode(ex);
                if (code == null) throw ex;
                rc = code;
            }
            return rc;
        }
    }

    /// Registers the an.next_job / an.job_done / an.job_failed imports that engine-main.c uses in host-driven mode.
    private static void registerEngineCallbacks(Linker<?> linker, EngineSession es) throws Exception {
        var i32 = WasmValueType.I32;
        var none = new WasmValueType[0];
        linker.defineHostFunction("an", "next_job", new FunctionType(none, new WasmValueType[]{i32}),
                (HostFunction) _ -> {
                    es.ready = true;
                    // Must not block: wasmtime4j counts a running callback as "in flight" in one static counter shared
                    // by all linkers, and closing any linker waits for that counter to reach 0.
                    var id = es.jobQueue.poll();
                    return new WasmValue[]{WasmValue.i32(id == null ? -1 : id)}; // -1 = none yet, 0 = stop
                });
        linker.defineHostFunction("an", "job_done", new FunctionType(new WasmValueType[]{i32}, none),
                (HostFunction) p -> {
                    es.results.add(new int[]{p[0].asInt(), DONE});
                    return new WasmValue[0];
                });
        linker.defineHostFunction("an", "job_failed", new FunctionType(new WasmValueType[]{i32}, none),
                (HostFunction) p -> {
                    es.results.add(new int[]{p[0].asInt(), FAILED});
                    return new WasmValue[0];
                });
    }

    /// Body of the engine thread: runs the engine's main(), which keeps calling an.next_job until told to stop.
    private void engineMain(EngineSession es, List<String> args) {
        Engine eng = useHostMemory ? hostWasmEngine : wasmEngine;
        HostMmap.Creator hm = useHostMemory ? creator : null;
        try (WasiLinker linker = WasiLinker.create(eng); Store store = eng.createStore()) {
            if (hm != null) {
                hm.resolver = HostMmap.resolver(es.mounts.dirs);
                hm.install(linker.getLinker());
            }
            registerEngineCallbacks(linker.getLinker(), es);
            if (MIRROR_LOG) {
                linker.configureStdout(WasiStdioConfig.nulled());
                linker.configureStderr(WasiStdioConfig.nulled());
                linker.configureStdin(WasiStdioConfig.nulled());
            }
            linker.setArguments(args);
            // Turns on host-driven jobs in engine-main.c;
            // the value is where the job folders are, as the guest sees them.
            linker.setEnvironmentVariable("AN_HOST_JOBS", GUEST_WORK);
            for (Map.Entry<Path, String> e : es.mounts.dirs.entrySet()) {
                if (Files.notExists(e.getKey())) {
                    Files.createDirectories(e.getKey());
                }
                if (Files.isDirectory(e.getKey())) {
                    linker.allowDirectoryAccess(e.getKey(), e.getValue());
                } else {
                    IO.println("%s is not a directory!".formatted(e.getKey()));
                }
            }
            Instance inst = linker.instantiate(store, astrometryEngineModule);
            try {
                inst.callFunction("_start");
            } catch (Exception ex) {
                Integer code = exitCode(ex);
                if (code == null) throw ex;
                if (code != 0) throw new IllegalStateException("astrometry-engine exited with " + code);
            }
        } catch (Throwable t) {
            es.failure = t;
        } finally {
            es.dead = true;
            es.results.add(new int[]{0, ENDED}); // wakes a solve() that is waiting on a dead engine
        }
    }

    /// Starts the engine thread if it is not running.  The index folders / config are read once, when the engine
    /// starts, so a call with different ones stops the engine and starts a new one.
    private synchronized EngineSession ensureEngine(Collection<Path> indexDirs, Path hostConfig) throws Exception {
        Object key = List.of(String.valueOf(indexDirs), String.valueOf(hostConfig));
        if (session != null && (!session.key.equals(key) || session.dead)) {
            closeEngine();
        }
        if (session != null) {
            return session;
        }

        IJ.showStatus("Creating engine config...");
        EngineSession es = new EngineSession();
        es.key = key;
        if (workDir == null) {
            workDir = WorkDir.acquire();
        }
        es.work = workDir.path;
        es.mounts = new Mounts();
        es.mounts.dirs.put(es.work.toAbsolutePath().normalize(), GUEST_WORK);
        String cfg;
        if (hostConfig != null) {
            cfg = es.mounts.rewriteConfig(Files.readString(hostConfig));
        } else {
            StringBuilder sb = new StringBuilder();
            if (indexDirs != null) {
                for (Path d : indexDirs) {
                    sb.append("add_path ").append(es.mounts.guest(d, true)).append('\n');
                }
            }
            cfg = sb.append("autoindex\n").toString();
        }
        Files.writeString(es.work.resolve("astrometry.cfg"), cfg);

        IJ.showStatus("Starting engine...");
        List<String> engineArgs = List.of("astrometry-engine",
                "-c", GUEST_WORK + "/astrometry.cfg", // The engine configuration (which lists indexes)
                "-C", GUEST_WORK + "/" + CANCEL_FILE // The engine stops solving when this file exists
        );
        es.thread = new Thread(() -> engineMain(es, engineArgs), "astrometry-engine");
        es.thread.setDaemon(true);
        es.thread.start();
        session = es; // startup errors surface through es.dead / es.failure in solve()

        // Loading many indexes can take a while and happens on the engine thread, so a ticker thread keeps the
        // status line / progress bar alive until the engine first asks for a job (es.ready) or dies.
        es.ticker = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "an-engine-startup-progress");
            t.setDaemon(true);
            return t;
        });
        es.ticker.scheduleWithFixedDelay(
                new Ticker(hostConfig == null ? countIndexFiles(indexDirs) : 0,
                        session, useHostMemory ? creator : null, System.nanoTime()
                ), 0, 500, TimeUnit.MILLISECONDS);
        return es;
    }

    /// Asks the engine to leave its job loop (after the running job, if any) and waits for its thread to end.
    private synchronized void closeEngine() {
        EngineSession es = session;
        if (es == null) {
            return;
        }
        session = null;
        if (es.ticker != null) {
            es.ticker.shutdownNow();
        }
        es.jobQueue.add(0); // an.next_job returns 0: main() frees the engine and returns
        try {
            es.thread.join(10_000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        clearDir(es.work); // not deleted: the next engine reuses the folder
    }

    /// @param indexDirs  folders holding index-*.fits (host paths), used if hostConfig is null
    /// @param hostConfig optional astrometry.cfg as written for the host; its paths are translated
    /// @param scaleLo/Hi arcsec/pixel bounds: 0 -> none
    /// @param radius     ra/dec/radius degrees: radius = 0 -> none
    /// @param tweakOrder 0 -> no tweak
    /// @return the WCS FITS header text (what nova stores as wcs.fits), or null if not solved
    public synchronized String solve(List<double[]> xy, int width, int height, double scaleLo, double scaleHi,
                                     double ra, double dec, double radius, boolean crpixCenter, int tweakOrder,
                                     Collection<Path> indexDirs, Path hostConfig) throws Exception {
        if (isCancelled()) {
            return null;
        }
        EngineSession es = ensureEngine(indexDirs, hostConfig);
        int n = ++es.jobs;
        String g = GUEST_WORK + "/j" + n; // guest path of this solve's folder
        Path work = es.work.resolve("j" + n); // host path of the same folder
        Path cancelFile = es.work.resolve(CANCEL_FILE);
        Files.createDirectory(work);
        Files.deleteIfExists(cancelFile); // Remove any existing cancel file
        try {
            IJ.showStatus("Creating job...");
            Files.write(work.resolve("job.xy"), fitsXylist(xy));

            // the same arguments nova's dojob() builds for a source list (process_submissions.py:274-335)
            List<String> a = new ArrayList<>(List.of("augment-xylist", "--xylist", g + "/job.xy", "--out", g + "/job.axy",
                    "--width", "" + width, "--height", "" + height, "--objs", "1000", "--tag-all", "--wcs", g + "/job.wcs"));
            if (scaleLo > 0 || scaleHi > 0) {
                a.addAll(List.of("--scale-units", "arcsecperpix", "--scale-low", "" + scaleLo, "--scale-high", "" + scaleHi));
            }
            if (radius > 0) {
                a.addAll(List.of("--ra", "" + ra, "--dec", "" + dec, "--radius", "" + radius));
            }
            if (tweakOrder == 0) {
                a.add("--no-tweak");
            } else {
                a.addAll(List.of("--tweak-order", "" + tweakOrder));
            }
            if (crpixCenter) {
                a.add("--crpix-center");
            }
            // Remove lines and uniformize require python code that has not been ported
            a.addAll(List.of("--no-remove-lines", "--uniformize", "0", "--temp-dir", g));

            int rc1 = runWasi("augment-xylist", augmentModule, a, es.mounts.dirs);
            if (rc1 != 0) throw new IllegalStateException("augment-xylist exited with " + rc1);
            if (isCancelled()) {
                return null;
            }

            // Hand the job to the running engine: it reads $AN_HOST_JOBS/j<n>/job.axy
            if (useHostMemory) {
                creator.resetStats();
            }
            es.jobQueue.add(n);
            int[] r = null;
            var cancelSent = false;
            while (r == null || (r[0] != n && r[1] != ENDED)) { // skip results left over from an earlier, interrupted solve
                if (es.ready) {
                    IJ.showStatus("Running job...");
                }
                r = es.results.poll(CANCEL_POLL_MS, TimeUnit.MILLISECONDS);
                if (r == null && !cancelSent && cancelled != null && cancelled.getAsBoolean()) {
                    Files.writeString(cancelFile, "cancel\n"); // the engine ends the job, then reports it done as usual
                    cancelSent = true;
                }
            }
            if (r[1] == ENDED) {
                var failure = es.failure;
                closeEngine();
                throw new IllegalStateException("astrometry-engine stopped", failure);
            }
            if (r[1] == FAILED) throw new IllegalStateException("astrometry-engine could not read the job file");
            Path wcs = work.resolve("job.wcs");
            return Files.exists(wcs) ? Files.readString(wcs, StandardCharsets.US_ASCII) : null;
        } finally {
            try {
                Files.deleteIfExists(cancelFile);
            } catch (IOException _) {
            }
            deleteTree(work);
        }
    }

    /// Cone search of a libkd star kd-tree
    ///
    /// @param raDeg      right ascension in degrees
    /// @param tagColumns tag-along columns to read for each hit (matched case-insensitively); returned in `tags`
    public synchronized List<CatalogHit> catalog(Path treeFile, double raDeg, double decDeg, double radiusDeg,
                                                 String... tagColumns) throws Exception {
        Path file = treeFile.toAbsolutePath().normalize();
        String key = file + "|" + String.join(",", tagColumns);
        KdSession ks = kdSessions.get(key);
        if (ks == null) {
            ks = openKd(file, tagColumns);
            kdSessions.put(key, ks);
        }

        int n = callInt(ks.inst, "kd_cone", WasmValue.f64(raDeg), WasmValue.f64(decDeg), WasmValue.f64(radiusDeg));
        if (n < 0) throw new IllegalStateException("kd_cone failed for " + file.getFileName() + " (" + n + ")");

        List<CatalogHit> hits = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            double[] tags = new double[ks.tags];
            for (int k = 0; k < tags.length; k++) {
                tags[k] = callDouble(ks.inst, "kd_tag", WasmValue.i32(i), WasmValue.i32(k));
            }
            hits.add(new CatalogHit(callInt(ks.inst, "kd_index", WasmValue.i32(i)),
                    callDouble(ks.inst, "kd_ra", WasmValue.i32(i)),
                    callDouble(ks.inst, "kd_dec", WasmValue.i32(i)), tags));
        }
        return hits;
    }

    private KdSession openKd(Path file, String[] tagColumns) throws Exception {
        KdSession ks = new KdSession();
        ks.tags = tagColumns.length;
        try {
            ks.linker = WasiLinker.create(wasmEngine);
            ks.store = wasmEngine.createStore();
            ks.linker.configureStdout(WasiStdioConfig.nulled());
            ks.linker.configureStderr(WasiStdioConfig.nulled());
            ks.linker.configureStdin(WasiStdioConfig.nulled());

            var args = new ArrayList<>(List.of("kd-cone", "/catalogs/" + file.getFileName()));
            args.addAll(List.of(tagColumns));
            ks.linker.setArguments(args);
            ks.linker.allowDirectoryAccess(file.getParent(), "/catalogs");

            ks.inst = ks.linker.instantiate(ks.store, kdTreeModule);
            if (ks.inst.hasExport("_initialize")) {
                ks.inst.callFunction("_initialize");
            }
            int rc = callInt(ks.inst, "kd_open");
            if (rc != 0) {
                var msg = switch (rc) {
                    case 1 -> "cannot open the file";
                    case 2 -> "not a double 3-D kd-tree";
                    case 3 -> "no tag-along data (or too many columns requested)";
                    default -> "tag-along column #" + (rc - 4) + " not found";
                };
                throw new IllegalStateException("kd_open failed for " + file.getFileName() + ": " + msg);
            }
        } catch (Throwable t) {
            closeKd(ks);
            throw t;
        }
        return ks;
    }

    private boolean isCancelled() {
        return cancelled != null && cancelled.getAsBoolean();
    }

    @Override
    public void close() throws Exception {
        synchronized (this) {
            for (KdSession ks : kdSessions.values()) {
                closeKd(ks);
            }
            kdSessions.clear();
        }
        closeEngine();
        if (hostWasmEngine != null) {
            hostWasmEngine.close();
        }
        wasmEngine.close();
        wasmRuntime.close();
        if (workDir != null) {
            workDir.close();
            workDir = null;
        }
    }

    private static void closeKd(KdSession ks) {
        try {
            if (ks.store != null) {
                ks.store.close();
            }
        } catch (Throwable _) {
        }
        try {
            if (ks.linker != null) {
                ks.linker.close();
            }
        } catch (Throwable _) {
        }
    }

    /// Number of index-*.fits files in the given folders (0 if unknown).
    private static int countIndexFiles(Collection<Path> dirs) {
        if (dirs == null) {
            return 0;
        }
        int n = 0;
        for (var d : dirs) {
            try (var s = Files.list(d)) {
                n += (int) s.filter(p -> {
                    var f = p.getFileName().toString().toLowerCase();
                    return f.startsWith("index-") && f.endsWith(".fits");
                }).count();
            } catch (IOException _) {
            }
        }
        return n;
    }

    /// One hit from a kd-tree cone search. `index` is the position in the original catalog,
    /// `tags` holds the requested tag-along columns, in the order they were requested.
    public record CatalogHit(int index, double raDeg, double decDeg, double[] tags) {
    }

    private static final class KdSession {
        WasiLinker linker;
        Store store;
        Instance inst;
        int tags;
    }

    private static final class EngineSession {
        /// host folder mounted at /work: config and one sub-folder per solve
        Path work;
        Mounts mounts;
        /// what the engine was set up with (index folders / config)
        Object key;
        /// runs the engine's main()
        Thread thread;
        int jobs;
        /// job numbers for an.next_job to hand out; 0 = shut down
        final BlockingQueue<Integer> jobQueue = new LinkedBlockingQueue<>();
        /// {job number, DONE | FAILED | ENDED} from the callbacks; ENDED (job 0) when the engine thread ends
        final BlockingQueue<int[]> results = new LinkedBlockingQueue<>();
        /// the engine finished setup and has asked for a job at least once
        volatile boolean ready;
        /// reports startup progress until the engine is ready or dead
        volatile ScheduledExecutorService ticker;
        /// the engine thread has ended
        volatile boolean dead;
        volatile Throwable failure;
    }

    /// Host folder -> guest folder mapping.
    static final class Mounts {
        final Map<Path, String> dirs = new LinkedHashMap<>();

        /// Guest path for a host file or folder; its folder is added as a preopen.
        String guest(Path host, boolean isDir) {
            Path dir = (isDir ? host : host.getParent()).toAbsolutePath().normalize();
            String g = dirs.computeIfAbsent(dir, _ -> "/mnt" + dirs.size());
            return isDir ? g : g + "/" + host.getFileName();
        }

        /// Rewrite an astrometry.cfg written for the host (Windows paths, CRLF) into one the guest understands.
        String rewriteConfig(String hostConfig) {
            StringBuilder out = new StringBuilder();
            for (String line : hostConfig.lines().toList()) {
                String t = line.strip();
                for (String key : new String[]{"add_path", "index", "multiindex"}) {
                    if (t.startsWith(key + " ") && Path.of(t.substring(key.length()).strip()).isAbsolute()) {
                        Path p = Path.of(t.substring(key.length()).strip());
                        t = key + " " + guest(p, key.equals("add_path"));
                    }
                }
                out.append(t).append('\n');
            }
            return out.toString();
        }
    }

    /// wasmtime4j does not release the root temp directory, preventing us from deleting it.
    /// This reuses an existing folder, if available, to avoid creating many empty directories
    private record WorkDir(Path path, FileChannel channel, FileLock lock) implements AutoCloseable {
        private static final String LOCK = "lock";
        private static final String PREFIX = "astroimagej-astrometry-net-";

        static synchronized WorkDir acquire() throws IOException {
            var tmp = Path.of(System.getProperty("java.io.tmpdir"));
            for (int i = 0; ; i++) {
                var dir = tmp.resolve(PREFIX + i);
                Files.createDirectories(dir);
                var ch = FileChannel.open(dir.resolve(LOCK), StandardOpenOption.CREATE, StandardOpenOption.WRITE);
                FileLock l = null;
                try {
                    l = ch.tryLock(); // null: another AIJ instance owns it
                } catch (OverlappingFileLockException _) { // another thread in this instance owns it
                }
                if (l == null) {
                    ch.close();
                    continue;
                }
                clearDir(dir); // Cleanup previous run
                return new WorkDir(dir, ch, l);
            }
        }

        @Override
        public void close() throws IOException {
            lock.release();
            channel.close();
        }
    }

    private record Ticker(int totalIndexes, EngineSession es, HostMmap.Creator hm, long startNanos) implements Runnable {
        Ticker {
            if (hm != null) {
                hm.resetStats();
            }
        }

        @Override
        public void run() {
            if (es.ready || es.dead) {
                IJ.showStatus(es.ready ? "Created engine." : "");
                IJ.showProgress(1.0);
                IJ.showProgress(-1.0);
                es.ticker.shutdown();
                return;
            }
            var secs = (System.nanoTime() - startNanos) / 1_000_000_000L;
            var mappedNow = hm != null ? hm.mappedFileCount() : 0;
            var msg = "Starting engine (" + secs + " s";
            if (totalIndexes > 0) {
                msg += ", " + totalIndexes + " index files";
            }
            if (mappedNow > 0) {
                msg += ", " + mappedNow + " loaded";
            }
            IJ.showStatus(msg + ")...");
            if (totalIndexes > 0 && mappedNow > 0) {
                IJ.showProgress(-1.0); // We have progress now
                IJ.showProgress(Math.min(0.99, (double) mappedNow / totalIndexes));
            } else {
                // unknown fraction: sweep the bar so it is visibly alive
                IJ.showProgress(-1.0 * ((secs % 10) + 1) / 10.0);
            }
        }
    }
}