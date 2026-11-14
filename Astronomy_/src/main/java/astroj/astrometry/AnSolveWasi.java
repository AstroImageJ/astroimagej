package astroj.astrometry;

import ai.tegmentum.wasmtime4j.*;
import ai.tegmentum.wasmtime4j.Module;
import ai.tegmentum.wasmtime4j.exception.I32ExitException;
import ai.tegmentum.wasmtime4j.factory.WasmRuntimeFactory;
import ai.tegmentum.wasmtime4j.wasi.WasiLinker;
import ai.tegmentum.wasmtime4j.wasi.WasiStdioConfig;
import ij.IJ;
import nom.tam.fits.BinaryTable;
import nom.tam.fits.Fits;
import nom.tam.util.FitsOutputStream;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;

public class AnSolveWasi implements AutoCloseable {
    private static final boolean MIRROR_LOG = true;
    private static final Collection<Path> CLEANUP = new HashSet<>();
    private final WasmRuntime wasmRuntime;
    private final Engine wasmEngine;
    private final Module augmentModule, astrometryEngineModule;
    private final boolean useHostMemory;
    private Engine hostWasmEngine;
    private HostMmap.Creator creator;
    private EngineSession session;

    static {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            for (Path path : CLEANUP) {
                try {
                    Files.deleteIfExists(path);
                } catch (IOException _) {
                }
            }
        }));
    }

    public AnSolveWasi(Path augmentWasm, Path engineWasm, boolean useHostMemory) throws Exception {
        this(Files.readAllBytes(augmentWasm), Files.readAllBytes(engineWasm), useHostMemory);
    }

    public AnSolveWasi(byte[] augmentWasm, byte[] engineWasm, boolean useHostMemory) throws Exception {
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
    }

    static byte[] fitsXylist(List<double[]> xy) throws IOException {
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
        for (Throwable t = ex; t != null; t = t.getCause())
            if (t instanceof I32ExitException x) return x.getExitCode();
        return null;
    }

    /// Calls an exported function that takes no arguments and returns an int.
    private static int callInt(Instance inst, String function) throws Exception {
        WasmValue[] r = inst.callFunction(function);
        return r != null && r.length > 0 ? r[0].asInt() : 0;
    }

    private static void deleteTree(Path dir) {
        if (dir == null || !Files.exists(dir)) return;
        try (Stream<Path> s = Files.walk(dir)) {
            //noinspection ResultOfMethodCallIgnored
            s.sorted(Comparator.reverseOrder()).forEach(p -> p.toFile().delete());
        } catch (IOException _) {
        }
        CLEANUP.add(dir);
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
        return runWasi(name, m, argv, preopens, wasmEngine, null);
    }

    private int runWasi(String name, Module m, List<String> argv, Map<Path, String> preopens, Engine eng, HostMmap.Creator hm) throws Exception {
        try (WasiLinker linker = WasiLinker.create(eng);
             Store store = eng.createStore()) {
            if (hm != null) {
                hm.resolver = HostMmap.resolver(preopens);
                hm.install(linker.getLinker());
            }
            if (MIRROR_LOG) {
                //todo have option to mirror to log
                linker.configureStdout(WasiStdioConfig.nulled());
                linker.configureStderr(WasiStdioConfig.nulled());
                // Verbose log goes into stdin
                linker.configureStdin(WasiStdioConfig.nulled());
            }
            linker.setArguments(argv);
            for (Map.Entry<Path, String> e : preopens.entrySet())
                linker.allowDirectoryAccess(e.getKey(), e.getValue()); // host folder -> guest path
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

    /// Opens the engine if it is not open.  The index folders / config are read by engine_setup(), so a call with
    /// different ones closes the engine and opens a new one.
    private synchronized EngineSession ensureEngine(Collection<Path> indexDirs, Path hostConfig) throws Exception {
        Object key = List.of(String.valueOf(indexDirs), String.valueOf(hostConfig));
        if (session != null && !session.key.equals(key)) closeEngine();
        if (session != null) return session;

        IJ.showStatus("Creating engine config...");
        EngineSession es = new EngineSession();
        es.key = key;
        es.work = Files.createTempDirectory("an-wasi");
        es.mounts = new Mounts();
        es.mounts.dirs.put(es.work.toAbsolutePath().normalize(), "/work");
        String cfg;
        if (hostConfig != null) {
            cfg = es.mounts.rewriteConfig(Files.readString(hostConfig));
        } else {
            StringBuilder sb = new StringBuilder();
            if (indexDirs != null) {
                for (Path d : indexDirs) sb.append("add_path ").append(es.mounts.guest(d, true)).append('\n');
            }
            cfg = sb.append("autoindex\n").toString();
        }
        Files.writeString(es.work.resolve("astrometry.cfg"), cfg);
        es.jobFile = es.work.resolve("next-job.txt");
        Files.writeString(es.jobFile, "\n"); // must exist when engine_setup() opens it for -f

        IJ.showStatus("Starting engine...");
        Engine eng = useHostMemory ? hostWasmEngine : wasmEngine;
        HostMmap.Creator hm = useHostMemory ? creator : null;
        try {
            es.linker = WasiLinker.create(eng);
            es.store = eng.createStore();
            if (hm != null) {
                hm.resolver = HostMmap.resolver(es.mounts.dirs);
                hm.install(es.linker.getLinker());
            }
            if (MIRROR_LOG) {
                es.linker.configureStdout(WasiStdioConfig.nulled());
                es.linker.configureStderr(WasiStdioConfig.nulled());
                es.linker.configureStdin(WasiStdioConfig.nulled());
            }
            List<String> engineArgs = new ArrayList<>(List.of("astrometry-engine", "-c", "/work/astrometry.cfg"));
            //if (log != null) engineArgs.add("-v");
            engineArgs.addAll(List.of("-f", "/work/next-job.txt"));
            es.linker.setArguments(engineArgs);
            for (Map.Entry<Path, String> e : es.mounts.dirs.entrySet())
                es.linker.allowDirectoryAccess(e.getKey(), e.getValue());
            es.inst = es.linker.instantiate(es.store, astrometryEngineModule);
            if (!es.inst.hasExport("an_engine_open"))
                throw new IllegalStateException("astrometry-engine is not built in reactor mode");
            if (es.inst.hasExport("_initialize"))
                es.inst.callFunction("_initialize");
            int rc;
            try {
                rc = callInt(es.inst, "an_engine_open");
            } catch (Exception ex) {
                var code = exitCode(ex);
                throw code == null ? ex : new IllegalStateException("astrometry-engine exited with " + code + " while starting");
            }
            if (rc != 0) throw new IllegalStateException("astrometry-engine setup failed (" + rc + ")");
        } catch (Throwable t) {
            session = es;
            closeEngine();
            throw t;
        }
        IJ.showStatus("Created engine.");
        session = es;
        return es;
    }

    private synchronized void closeEngine() {
        EngineSession es = session;
        if (es == null) return;
        session = null;
        if (es.inst != null) {
            try {
                es.inst.callFunction("an_engine_close");
            } catch (Throwable _) {
            }
        }
        try {
            if (es.store != null) es.store.close();
        } catch (Throwable _) {
        }
        try {
            if (es.linker != null) es.linker.close();
        } catch (Throwable _) {
        }
        deleteTree(es.work);
    }

    /// @param indexDirs  folders holding index-*.fits (host paths), used if hostConfig is null
    /// @param hostConfig optional astrometry.cfg as written for the host; its paths are translated
    /// @param scaleLo/Hi arcsec/pixel bounds: 0 -> none
    /// @param radius ra/dec/radius degrees: radius = 0 -> none
    /// @param tweakOrder 0 -> no tweak
    /// @return the WCS FITS header text (what nova stores as wcs.fits), or null if not solved
    public synchronized String solve(List<double[]> xy, int width, int height, double scaleLo, double scaleHi,
                                     double ra, double dec, double radius, boolean crpixCenter, int tweakOrder,
                                     Collection<Path> indexDirs, Path hostConfig) throws Exception {
        EngineSession es = ensureEngine(indexDirs, hostConfig);
        int n = ++es.jobs;
        String g = "/work/j" + n; // guest path of this solve's folder
        Path work = es.work.resolve("j" + n); // host path of the same folder
        Files.createDirectory(work);
        try {
            IJ.showStatus("Creating job...");
            Files.write(work.resolve("job.xy"), fitsXylist(xy));

            // the same arguments nova's dojob() builds for a source list (process_submissions.py:274-335)
            List<String> a = new ArrayList<>(List.of("augment-xylist", "--xylist", g + "/job.xy", "--out", g + "/job.axy",
                    "--width", "" + width, "--height", "" + height, "--objs", "1000", "--tag-all", "--wcs", g + "/job.wcs"));
            if (scaleLo > 0 || scaleHi > 0)
                a.addAll(List.of("--scale-units", "arcsecperpix", "--scale-low", "" + scaleLo, "--scale-high", "" + scaleHi));
            if (radius > 0) a.addAll(List.of("--ra", "" + ra, "--dec", "" + dec, "--radius", "" + radius));
            if (tweakOrder == 0) a.add("--no-tweak");
            else a.addAll(List.of("--tweak-order", "" + tweakOrder));
            if (crpixCenter) a.add("--crpix-center");
            // Remove lines and uniformize require python code that has not been ported
            a.addAll(List.of("--no-remove-lines", "--uniformize", "0", "--temp-dir", g));

            int rc1 = runWasi("augment-xylist", augmentModule, a, es.mounts.dirs);
            if (rc1 != 0) throw new IllegalStateException("augment-xylist exited with " + rc1);

            // Hand the job to the open engine: its name goes in the job-name file, then engine_process() runs it
            // The engine always reads from the start of the file in reactor mode -
            // a workaround for wasmtime4j not keeping stdin open
            Files.writeString(es.jobFile, g + "/job.axy\n");
            if (useHostMemory) {
                creator.resetStats();
            }
            IJ.showStatus("Running job...");
            int rc;
            try {
                rc = callInt(es.inst, "an_engine_run");
            } catch (Exception ex) {
                Integer code = exitCode(ex);
                if (code == null) throw ex;
                closeEngine();
                throw new IllegalStateException("astrometry-engine exited with " + code);
            }
            if (rc < -1) throw new IllegalStateException("astrometry-engine could not run the job (" + rc + ")");
            Path wcs = work.resolve("job.wcs");
            return Files.exists(wcs) ? Files.readString(wcs, StandardCharsets.US_ASCII) : null;
        } finally {
            deleteTree(work);
        }
    }

    @Override
    public void close() throws Exception {
        closeEngine();
        if (hostWasmEngine != null) hostWasmEngine.close();
        wasmEngine.close();
        wasmRuntime.close();
    }

    private static final class EngineSession {
        /// host folder mounted at /work: config, job-name file, one sub-folder per solve
        Path work;
        Mounts mounts;
        /// /work/next-job.txt: the engine reads the next job's name from its first line
        Path jobFile;
        WasiLinker linker;
        Store store;
        Instance inst;
        /// what the engine was set up with (index folders / config)
        Object key;
        int jobs;
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
}