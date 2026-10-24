package astroj.astrometry;

import ai.tegmentum.wasmtime4j.WasmValue;
import ai.tegmentum.wasmtime4j.WasmValueType;
import ai.tegmentum.wasmtime4j.config.LinearMemory;
import ai.tegmentum.wasmtime4j.config.MemoryCreator;
import ai.tegmentum.wasmtime4j.exception.WasmException;
import ai.tegmentum.wasmtime4j.func.HostFunction;
import ai.tegmentum.wasmtime4j.type.FunctionType;
import ai.tegmentum.wasmtime4j.type.MemoryType;
import ij.IJ;

import java.io.IOException;
import java.lang.foreign.*;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.VarHandle;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.*;
import java.util.concurrent.atomic.AtomicIntegerArray;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;
import java.util.logging.Logger;

/// Host-side support for WASI mmap() emulation. The WASI side can only read a limited amount of a file at once,
/// so reading large index files through it in chunks is very slow. Instead the guest asks the host to map the
/// file straight into the guest's linear memory.
public final class HostMmap {
    /// [WASM Page Size](https://webassembly.github.io/spec/core/exec/runtime.html#memory-instances)
    private static final long GRANULE = 65536;
    private static final Logger LOG = Logger.getLogger(HostMmap.class.getName());
    /// Keep Windows file-mapping handles open until the memory is released (faster when the engine is reopened).
    private static final boolean CACHING = false;
    private static final boolean VERIFY = System.getenv("AN_VERIFY") != null;
    private static final boolean TRACE = System.getenv("AN_TRACE") != null;

    private HostMmap() {
    }

    static Mapper createMapper() {
        return IJ.isWindows() ? new WindowsMapper(CACHING) : new PosixMapper(IJ.isMacOSX());
    }

    /// Handle long paths on Windows
    /// [See Java version](https://github.com/openjdk/jdk/blob/master/src/java.base/windows/native/libjava/canonicalize_md.c#L390)
    private static String toWin32LongPath(String abs) {
        if (abs.length() < 260 - 1) {
            return abs;
        }
        // already extended / a device path
        if (abs.startsWith("\\\\?\\") || abs.startsWith("\\\\.\\")) {
            return abs;
        }
        // \\server\share\x -> \\?\UNC\server\share\x
        if (abs.startsWith("\\\\")) {
            return "\\\\?\\UNC\\" + abs.substring(2);
        }
        // C:\x -> \\?\C:\x
        return "\\\\?\\" + abs;
    }

    private static String cString(long addr) {
        return MemorySegment.ofAddress(addr).reinterpret(4096).getString(0, StandardCharsets.UTF_8);
    }

    /// Longest-prefix translation of a guest path using the preopens (host dir -> guest dir).
    public static Function<String, Path> resolver(Map<Path, String> preopens) {
        return guest -> {
            Path best = null;
            int bestLen = -1;
            for (Map.Entry<Path, String> e : preopens.entrySet()) {
                String g = e.getValue();
                if ((guest.equals(g) || guest.startsWith(g.endsWith("/") ? g : g + "/")) && g.length() > bestLen) {
                    String rel = guest.substring(g.length());
                    while (rel.startsWith("/")) rel = rel.substring(1);
                    best = rel.isEmpty() ? e.getKey() : e.getKey().resolve(rel);
                    bestLen = g.length();
                }
            }
            return best;
        };
    }

    /// Debug: compare mapped guest memory against the file (AN_VERIFY=1).
    private static void verify(long addr, long len, Path host, long off) throws IOException {
        byte[] expected = new byte[(int) len];
        try (FileChannel c = FileChannel.open(host)) {
            c.read(ByteBuffer.wrap(expected), off);
        }
        byte[] actual = MemorySegment.ofAddress(addr).reinterpret(len).toArray(ValueLayout.JAVA_BYTE);
        System.err.println("[verify] " + (Arrays.equals(expected, actual) ? "ok" : "MISMATCH") + " off=" + off);
    }

    /// Compare guest memory [memOff, memOff+len) with the file from fileOff on, byte for byte; bytes past the end of the file must be zero.
    private static long mismatches(MemorySegment mem, long memOff, Path f, long fileOff, long len) throws IOException {
        long bad = 0, first = -1, fileLen = Files.size(f);
        byte[] buf = new byte[1 << 20];
        try (FileChannel ch = FileChannel.open(f)) {
            for (long done = 0; done < len; ) {
                int k = (int) Math.min(buf.length, len - done);
                ByteBuffer bb = ByteBuffer.wrap(buf, 0, k);
                long pos = fileOff + done;
                int got = 0;
                while (got < k && pos + got < fileLen) {
                    int r = ch.read(bb, pos + got);
                    if (r <= 0) break;
                    got += r;
                }
                Arrays.fill(buf, got, k, (byte) 0); // beyond EOF: zeros, like mmap
                byte[] g = mem.asSlice(memOff + done, k).toArray(ValueLayout.JAVA_BYTE);
                int mismatch = Arrays.mismatch(g, 0, k, buf, 0, k);
                if (mismatch >= 0) {
                    if (first < 0) first = done + mismatch;
                    bad++;
                }
                done += k;
            }
        }
        if (bad > 0) System.out.println("  first mismatch at +" + first + " (" + bad + " chunks differ)");
        return bad;
    }

    private static boolean allZero(MemorySegment mem, long memOff, long len) {
        for (long i = 0; i < len; i += 1 << 20) {
            byte[] g = mem.asSlice(memOff + i, Math.min(1 << 20, len - i)).toArray(ValueLayout.JAVA_BYTE);
            for (byte b : g) if (b != 0) return false;
        }
        return true;
    }

    /// Test that memory mapping works
    static void main(String[] args) throws Throwable {
        Mapper m = createMapper();
        Path f = Path.of(args[0]);
        long total = 1L << 32, size = 2 * GRANULE;
        long base = m.reserve(total);
        m.commit(base, size);
        IO.println("reserved 4 GiB at 0x" + Long.toHexString(base) + ", committed 128 KiB");
        MemorySegment mem = MemorySegment.ofAddress(base).reinterpret(total);
        mem.set(ValueLayout.JAVA_BYTE, 5, (byte) 42);
        // grow in two steps like wasm memory.grow
        long at = size;
        m.commit(base + at, 16 * GRANULE);
        size = at + 16 * GRANULE;
        long fileLen = Files.size(f);
        boolean ok = true;
        long off = 4 * GRANULE, len = ((Math.min(fileLen, 1L << 30) + GRANULE - 1) / GRANULE) * GRANULE;
        if (off + len > size) {
            m.commit(base + size, off + len - size);
            size = off + len;
        }
        System.out.printf("file %s: %d bytes; mapping %d bytes (rounded up to 64 KiB) at guest offset %d%n", f, fileLen, len, off);
        long t0 = System.nanoTime();
        long n = m.mapFile(base + off, len, f, 0);
        System.out.printf("mapped %d bytes in %.3f ms%n", n, (System.nanoTime() - t0) / 1e6);
        if (n <= 0) {
            IO.println("file is smaller than 64 KiB: nothing to map (use a bigger file)");
            System.exit(2);
        }
        long bad = mismatches(mem, off, f, 0, len);
        IO.println("all " + len + " bytes compared with the file (zeros expected past its end): " + (bad == 0 ? "MATCH" : "FAILED"));
        ok &= bad == 0;
        boolean b42 = mem.get(ValueLayout.JAVA_BYTE, 5) == 42;
        IO.println("earlier committed byte still 42: " + b42);
        ok &= b42;
        m.restore(base + off, len);
        boolean z = allZero(mem, off, n);
        IO.println("after unmap the mapped span is all zero and writable: " + z);
        ok &= z;
        mem.set(ValueLayout.JAVA_BYTE, off + 10, (byte) 7);
        // a second mapping from a non-zero file offset (a multiple of 64 KiB), to check offsets
        long off2 = fileLen > 3 * GRANULE ? GRANULE * Math.min(5, (fileLen / GRANULE) - 1) : 0;
        long len2 = ((Math.min(fileLen - off2, 1L << 30) + GRANULE - 1) / GRANULE) * GRANULE;
        if (off + len2 > size) {
            m.commit(base + size, off + len2 - size);
        }
        n = m.mapFile(base + off, len2, f, off2);
        bad = mismatches(mem, off, f, off2, len2);
        IO.println("remapped from file offset " + off2 + ": " + n + " bytes mapped, all " + len2 + " bytes compared: " + (bad == 0 ? "MATCH" : "FAILED"));
        ok &= bad == 0;
        m.restore(base + off, len2);
        m.release(base, total);
        IO.println(ok ? "OK" : "FAILED");
        if (!ok) System.exit(1);
    }

    /// Address-space operations. All addresses are absolute host addresses; lengths are multiples of GRANULE unless noted.
    interface Mapper {
        /// inaccessible address space
        long reserve(long bytes) throws Throwable;
        /// make [addr,addr+len) zeroed read/write memory
        void commit(long addr, long len) throws Throwable;
        /// map file copy-on-write (read-only on Windows) at addr; returns bytes mapped (<= len), or <= 0 if nothing was mapped
        long mapFile(long addr, long len, Path file, long off) throws Throwable;
        /// make [addr,addr+len) inaccessible (test hook)
        default void protectNone(long addr, long len) throws Throwable {
            throw new UnsupportedOperationException("test hook: Linux/macOS only");
        }
        /// replace whatever is at [addr,addr+len) by zeroed read/write memory
        void restore(long addr, long len) throws Throwable;

        void release(long base, long bytes);
    }

    /// The guest's linear memory, in address space we reserved.
    static final class Memory implements LinearMemory {
        final Mapper mapper;
        final long base, capacity, total;
        long size;

        Memory(Mapper mapper, long minBytes, long reserved, long guard) throws Throwable {
            this.mapper = mapper;
            this.capacity = reserved;
            this.total = reserved + guard;
            this.base = mapper.reserve(total);
            if (minBytes > 0) mapper.commit(base, minBytes);
            this.size = minBytes;
        }

        @Override
        public long byteSize() {
            return size;
        }

        @Override
        public long byteCapacity() {
            return capacity;
        }

        @Override
        public void growTo(long n) throws WasmException {
            try {
                if (n > capacity) throw new IllegalStateException("grow beyond reserved " + capacity);
                if (n > size) {
                    mapper.commit(base + size, n - size);
                    size = n;
                }
            } catch (Throwable t) {
                throw new WasmException("growTo failed: " + t);
            }
        }

        @Override
        public long basePointer() {
            return base;
        }

        @Override
        public void close() {
            mapper.release(base, total);
        }
    }

    /// MemoryCreator for the engine; remembers the most recent memory (one instance runs at a time) and resolves guest paths.
    public static final class Creator implements MemoryCreator {
        private static final int FAIL_BAD_ARGS = 1, FAIL_NO_FILE = 2, FAIL_NOT_MAPPED = 3, FAIL_EXCEPTION = 4;

        // statistics only, nothing depends on them
        /// failure counts, indexed by FAIL_*
        public final AtomicIntegerArray fail = new AtomicIntegerArray(5);
        public final AtomicLong noaccess = new AtomicLong();
        public final AtomicLong mapped = new AtomicLong();
        public final AtomicLong unmapped = new AtomicLong();
        public final AtomicLong mapNanos = new AtomicLong();

        /// guest address -> {host path, file offset, length}; only used for tracing/verification
        private final Map<Long, Object[]> rec = new HashMap<>();
        /// guest path -> host path
        public volatile Function<String, Path> resolver = p -> null;
        volatile Memory current;

        public void resetStats() {
            mapped.set(0);
            unmapped.set(0);
            mapNanos.set(0);
        }

        @Override
        public LinearMemory newMemory(MemoryType type, long min, OptionalLong max, OptionalLong reserved, long guard) {
            try {
                long cap = reserved.orElseGet(() -> max.orElse(4L << 30));
                long r = Math.min(cap, 4L << 30);
                //noinspection PointlessBitwiseExpression
                r = (r + GRANULE - 1) & ~(GRANULE - 1);// keep it page-aligned
                Memory m = new Memory(createMapper(), min, r, guard);
                current = m;
                return m;
            } catch (Throwable t) {
                throw new RuntimeException("reserving guest memory failed", t);
            }
        }

        /// Registers the an.map / an.noaccess / an.unmap host functions on a linker (e.g. WasiLinker.getLinker()).
        public void install(ai.tegmentum.wasmtime4j.Linker<?> linker) throws WasmException {
            WasmValueType I32 = WasmValueType.I32, I64 = WasmValueType.I64;
            linker.defineHostFunction("an", "map", new FunctionType(new WasmValueType[]{I32, I32, I32, I64}, new WasmValueType[]{I32}),
                    (HostFunction) p -> {
                        try {
                            return new WasmValue[]{WasmValue.i32(map(Integer.toUnsignedLong(p[0].asInt()),
                                    Integer.toUnsignedLong(p[1].asInt()), Integer.toUnsignedLong(p[2].asInt()), p[3].asLong()))};
                        } catch (Throwable t) {
                            t.printStackTrace();
                            return new WasmValue[]{WasmValue.i32(-5)};
                        }
                    });
            linker.defineHostFunction("an", "noaccess", new FunctionType(new WasmValueType[]{I32, I32}, new WasmValueType[]{I32}),
                    (HostFunction) p -> {
                        try {
                            Memory m = current;
                            m.mapper.protectNone(m.base + Integer.toUnsignedLong(p[0].asInt()), Integer.toUnsignedLong(p[1].asInt()));
                            noaccess.incrementAndGet();
                            return new WasmValue[]{WasmValue.i32(0)};
                        } catch (Throwable t) {
                            return new WasmValue[]{WasmValue.i32(-1)};
                        }
                    });
            linker.defineHostFunction("an", "unmap", new FunctionType(new WasmValueType[]{I32, I32}, new WasmValueType[]{I32}),
                    (HostFunction) p -> {
                        try {
                            return new WasmValue[]{WasmValue.i32(unmap(Integer.toUnsignedLong(p[0].asInt()), Integer.toUnsignedLong(p[1].asInt())))};
                        } catch (Throwable t) {
                            t.printStackTrace();
                            return new WasmValue[]{WasmValue.i32(-5)};
                        }
                    });
        }

        private int map(long guestAddr, long len, long pathPtr, long off) {
            long t0 = System.nanoTime();
            try {
                Memory m = current;
                if (m == null || guestAddr % GRANULE != 0 || off % GRANULE != 0 || guestAddr + len > m.size) {
                    fail.incrementAndGet(FAIL_BAD_ARGS);
                    return -1;
                }
                Path host = resolver.apply(cString(m.base + pathPtr));
                if (host == null || !Files.isRegularFile(host)) {
                    fail.incrementAndGet(FAIL_NO_FILE);
                    LOG.warning("an.map: cannot resolve guest path to a host file: " + host);
                    return -2;
                }
                long n = m.mapper.mapFile(m.base + guestAddr, len, host, off);
                if (n <= 0) {
                    fail.incrementAndGet(FAIL_NOT_MAPPED);
                    return -3;
                }
                rec.put(guestAddr, new Object[]{host, off, len});
                mapped.incrementAndGet();
                mapNanos.addAndGet(System.nanoTime() - t0);
                if (VERIFY) verify(m.base + guestAddr, len, host, off);
                if (TRACE) System.err.println("[map] " + host.getFileName() + " off=" + off + " len=" + len + " at " + guestAddr);
                return 0;
            } catch (Throwable t) {
                fail.incrementAndGet(FAIL_EXCEPTION);
                LOG.warning("an.map failed, guest falls back to reading: " + t);
                return -4;
            }
        }

        private int unmap(long guestAddr, long len) {
            try {
                Memory m = current;
                Object[] r = rec.remove(guestAddr);
                if (TRACE) System.err.println("[unmap] " + guestAddr + " len=" + len + (r == null ? " (not recorded)" : ""));
                if (r != null && VERIFY) {
                    System.err.print("[unmap] ");
                    verify(m.base + guestAddr, (Long) r[2], (Path) r[0], (Long) r[1]);
                }
                m.mapper.restore(m.base + guestAddr, len);
                unmapped.incrementAndGet();
                return 0;
            } catch (Throwable t) {
                LOG.severe("an.unmap failed: " + t);
                return -1;
            }
        }
    }

    static final class PosixMapper implements Mapper {
        private static final int PROT_RW = 3, MAP_PRIVATE = 2, MAP_FIXED = 0x10;
        private final Linker linker = Linker.nativeLinker();
        private final SymbolLookup lookup = linker.defaultLookup();
        private final MethodHandle mmap = handle("mmap", FunctionDescriptor.of(ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.JAVA_LONG,
                ValueLayout.JAVA_INT, ValueLayout.JAVA_INT, ValueLayout.JAVA_INT, ValueLayout.JAVA_LONG));
        private final MethodHandle munmap = handle("munmap", FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.JAVA_LONG));
        private final MethodHandle mprotect = handle("mprotect", FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.JAVA_LONG, ValueLayout.JAVA_INT));
        private final MethodHandle open = handle("open", FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.JAVA_INT), Linker.Option.firstVariadicArg(2));
        private final MethodHandle close = handle("close", FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.JAVA_INT));
        private final int mapAnon, mapNoReserve;

        PosixMapper(boolean mac) {
            mapAnon = mac ? 0x1000 : 0x20;
            mapNoReserve = mac ? 0x40 : 0x4000;
        }

        private MethodHandle handle(String name, FunctionDescriptor d, Linker.Option... o) {
            return linker.downcallHandle(lookup.find(name).orElseThrow(), d, o);
        }

        private long mmap(long addr, long len, int prot, int flags, int fd, long off) throws Throwable {
            MemorySegment r = (MemorySegment) mmap.invokeExact(MemorySegment.ofAddress(addr), len, prot, flags, fd, off);
            if (r.address() == -1L)
                throw new IllegalStateException("mmap(" + Long.toHexString(addr) + ", " + len + ") failed");
            return r.address();
        }

        @Override
        public long reserve(long bytes) throws Throwable {
            return mmap(0, bytes, 0, MAP_PRIVATE | mapAnon | mapNoReserve, -1, 0);
        }

        @Override
        public void commit(long addr, long len) throws Throwable {
            int rc = (int) mprotect.invokeExact(MemorySegment.ofAddress(addr), len, PROT_RW);
            if (rc != 0) throw new IllegalStateException("mprotect failed");
        }

        @Override
        public long mapFile(long addr, long len, Path file, long off) throws Throwable {
            long n = Math.min(len, Files.size(file) - off);
            if (n <= 0) return -1;
            try (Arena a = Arena.ofConfined()) {
                int fd = (int) open.invokeExact(a.allocateFrom(file.toString()), 0);
                if (fd < 0) throw new IllegalStateException("open failed: " + file);
                try {
                    mmap(addr, n, PROT_RW, MAP_PRIVATE | MAP_FIXED, fd, off); // copy-on-write: the engine may write to its mapping
                    long end = (n + 4095) & ~4095L;                            // whole pages past the end of file read as zeros
                    if (end < len) mmap(addr + end, len - end, PROT_RW, MAP_PRIVATE | mapAnon | MAP_FIXED, -1, 0);
                } finally {
                    int _ = (int) close.invokeExact(fd);
                }
            }
            return n;
        }

        @Override
        public void protectNone(long addr, long len) throws Throwable {
            int rc = (int) mprotect.invokeExact(MemorySegment.ofAddress(addr), len, 0);
            if (rc != 0) throw new IllegalStateException("mprotect(PROT_NONE) failed");
        }

        @Override
        public void restore(long addr, long len) throws Throwable {
            mmap(addr, len, PROT_RW, MAP_PRIVATE | mapAnon | MAP_FIXED, -1, 0);
        }

        @Override
        public void release(long base, long bytes) {
            try {
                int _ = (int) munmap.invokeExact(MemorySegment.ofAddress(base), bytes);
            } catch (Throwable _) {
            }
        }
    }

    static final class WindowsMapper implements Mapper {
        private static final int MEM_COMMIT = 0x1000, MEM_RESERVE = 0x2000, MEM_REPLACE_PLACEHOLDER = 0x4000,
                MEM_RESERVE_PLACEHOLDER = 0x40000, MEM_RELEASE = 0x8000, MEM_PRESERVE_PLACEHOLDER = 0x2, MEM_COALESCE_PLACEHOLDERS = 0x1;
        private static final int PAGE_NOACCESS = 1, PAGE_READONLY = 2, PAGE_READWRITE = 4;
        /// GetCurrentProcess() pseudo-handle; UnmapViewOfFile2 (unlike MapViewOfFile3/VirtualAlloc2) rejects NULL with ERROR_INVALID_HANDLE (6)
        private static final MemorySegment SELF = MemorySegment.ofAddress(-1L);

        private final Linker linker = Linker.nativeLinker();
        private final SymbolLookup k32 = SymbolLookup.libraryLookup("kernel32.dll", Arena.global());
        private final SymbolLookup kernelBase = SymbolLookup.libraryLookup("kernelbase.dll", Arena.global());
        private final MethodHandle valloc2, vfree, mapView3, unmapView2, createFile, createMapping, closeHandle, vquery;
        private final Arena arena = Arena.ofShared();
        private final MemorySegment err = arena.allocate(Linker.Option.captureStateLayout());
        private final VarHandle lastError = Linker.Option.captureStateLayout().varHandle(MemoryLayout.PathElement.groupElement("GetLastError"));
        private final boolean cacheFileMappings;
        /// committed private allocations: start -> length
        private final TreeMap<Long, Long> chunks = new TreeMap<>();
        /// file views: start -> length
        private final TreeMap<Long, Long> views = new TreeMap<>();
        /// open file handles, only filled when cacheFileMappings is set
        private final Map<Path, FileMapping> fileMappings = new HashMap<>();
        /// after any failed mapping, host mapping stays off for this session (the guest falls back to reading)
        private volatile boolean broken;
        /// start of the still-uncommitted placeholder
        private long tailStart;

        WindowsMapper(boolean cacheFileMappings) {
            this.cacheFileMappings = cacheFileMappings;
            ValueLayout.OfInt I = ValueLayout.JAVA_INT;
            ValueLayout.OfLong J = ValueLayout.JAVA_LONG;
            ValueLayout A = ValueLayout.ADDRESS;
            Linker.Option cs = Linker.Option.captureCallState("GetLastError");
            valloc2 = find("VirtualAlloc2", FunctionDescriptor.of(A, A, A, J, I, I, A, I), cs);
            vfree = find("VirtualFree", FunctionDescriptor.of(I, A, J, I), cs);
            mapView3 = find("MapViewOfFile3", FunctionDescriptor.of(A, A, A, A, J, J, I, I, A, I), cs);
            unmapView2 = find("UnmapViewOfFile2", FunctionDescriptor.of(I, A, A, I), cs);
            createFile = find("CreateFileW", FunctionDescriptor.of(A, A, I, I, A, I, I, A), cs);
            createMapping = find("CreateFileMappingW", FunctionDescriptor.of(A, A, A, I, I, I, A), cs);
            closeHandle = find("CloseHandle", FunctionDescriptor.of(I, A));
            vquery = find("VirtualQuery", FunctionDescriptor.of(J, A, A, J), cs);
        }

        private static MemorySegment p(long a) {
            return MemorySegment.ofAddress(a);
        }

        private MethodHandle find(String n, FunctionDescriptor d, Linker.Option... o) {
            MemorySegment f = kernelBase.find(n)
                    .or(() -> k32.find(n))
                    .orElseThrow(() -> new IllegalStateException("Win32 function not found: " + n));
            return linker.downcallHandle(f, d, o);
        }

        private String lastErr() {
            return "GetLastError=" + (int) lastError.get(err, 0L);
        }

        private long valloc2(long addr, long len, int type, int prot) throws Throwable {
            MemorySegment r = (MemorySegment) valloc2.invokeExact(err, MemorySegment.NULL, p(addr), len, type, prot, MemorySegment.NULL, 0);
            if (r.address() == 0)
                throw new IllegalStateException("VirtualAlloc2(" + Long.toHexString(addr) + ", " + len + ", 0x" + Integer.toHexString(type) + ") failed, " + lastErr());
            return r.address();
        }

        private void vfree(long addr, long len, int type) throws Throwable {
            int ok = (int) vfree.invokeExact(err, p(addr), len, type);
            if (ok == 0)
                throw new IllegalStateException("VirtualFree(" + Long.toHexString(addr) + ", " + len + ", 0x" + Integer.toHexString(type) + ") failed, " + lastErr());
        }

        private void closeQuietly(MemorySegment handle) {
            try {
                int _ = (int) closeHandle.invokeExact(handle);
            } catch (Throwable _) {
            }
        }

        /// MEMORY_BASIC_INFORMATION of the region at addr: {regionStart, regionSize, state, type, protect}, or null.
        private long[] query(long addr) {
            try (Arena a = Arena.ofConfined()) {
                MemorySegment mbi = a.allocate(48, 8);
                long r = (long) vquery.invokeExact(err, p(addr), mbi, 48L);
                if (r == 0) return null;
                return new long[]{mbi.get(ValueLayout.JAVA_LONG, 0), mbi.get(ValueLayout.JAVA_LONG, 24), mbi.get(ValueLayout.JAVA_INT, 32) & 0xffffffffL,
                        mbi.get(ValueLayout.JAVA_INT, 40) & 0xffffffffL, mbi.get(ValueLayout.JAVA_INT, 36) & 0xffffffffL};
            } catch (Throwable t) {
                return null;
            }
        }

        /// Diagnostics: what Windows says is at [addr-64K, addr+len+64K).
        /// state 1000=commit 2000=reserve(placeholder) 10000=free; type 20000=private 40000=mapped.
        private String describe(long addr, long len) {
            StringBuilder sb = new StringBuilder("memory map:");
            long a = addr - GRANULE, end = addr + len + GRANULE;
            for (int n = 0; a < end && n < 24; n++) {
                long[] q = query(a);
                if (q == null) {
                    sb.append(" [query failed at 0x").append(Long.toHexString(a)).append(']');
                    break;
                }
                sb.append(String.format(" [0x%x+%d state=%x type=%x prot=%x]", q[0], q[1], q[2], q[3], q[4]));
                a = q[0] + q[1];
            }
            return sb.toString();
        }

        /// Make [addr,addr+len) ordinary read/write memory again, one 64 KiB unit at a time, whatever state it is in
        /// (placeholders of any size, already committed units).
        private void giveBack(long addr, long len) {
            for (long u = addr; u < addr + len; u += GRANULE) {
                long[] q = query(u);
                if (q != null && q[2] == MEM_COMMIT) continue; // already usable memory
                try {
                    try {
                        valloc2(u, GRANULE, MEM_RESERVE | MEM_COMMIT | MEM_REPLACE_PLACEHOLDER, PAGE_READWRITE);
                    } catch (IllegalStateException e) { // placeholder bigger than this unit: split it first
                        vfree(u, GRANULE, MEM_RELEASE | MEM_PRESERVE_PLACEHOLDER);
                        valloc2(u, GRANULE, MEM_RESERVE | MEM_COMMIT | MEM_REPLACE_PLACEHOLDER, PAGE_READWRITE);
                    }
                    chunks.put(u, GRANULE);
                } catch (Throwable t) {
                    System.err.println("host mmap (Windows): cannot give back 0x" + Long.toHexString(u) + ": " + t + " " + describe(u, GRANULE));
                }
            }
        }

        @Override
        public long reserve(long bytes) throws Throwable {
            long base = valloc2(0, bytes, MEM_RESERVE | MEM_RESERVE_PLACEHOLDER, PAGE_NOACCESS);
            tailStart = base;
            return base;
        }

        @Override
        public void commit(long addr, long len) throws Throwable {
            // [addr, addr+len) is the lowest part of the uncommitted placeholder
            if (addr != tailStart) throw new IllegalStateException("commit must extend the memory");
            vfree(addr, len, MEM_RELEASE | MEM_PRESERVE_PLACEHOLDER); // split it off the big placeholder
            valloc2(addr, len, MEM_RESERVE | MEM_COMMIT | MEM_REPLACE_PLACEHOLDER, PAGE_READWRITE);
            chunks.put(addr, len);
            tailStart = addr + len;
        }

        /// Turn [addr,addr+len) (all committed chunks) into ONE placeholder. Every placeholder that exists at any moment is
        /// recorded in ph, so that mapFile can hand it back as ordinary memory if a later step fails: the guest keeps using
        /// this memory, and a placeholder (inaccessible) left behind would crash the JVM when the guest touches it.
        private void carve(long addr, long len, List<long[]> ph) throws Throwable {
            long end = addr + len;
            Long floor = chunks.floorKey(addr);
            List<long[]> hit = new ArrayList<>();
            for (Map.Entry<Long, Long> e : chunks.subMap(floor == null ? addr : floor, end).entrySet()) {
                long cs = e.getKey(), ce = cs + e.getValue();
                if (ce > addr) hit.add(new long[]{cs, ce});
            }
            for (long[] c : hit) {
                long is = Math.max(c[0], addr), ie = Math.min(c[1], end);
                vfree(is, ie - is, MEM_RELEASE | MEM_PRESERVE_PLACEHOLDER); // committed -> placeholder
                chunks.remove(c[0]);
                if (c[0] < is) chunks.put(c[0], is - c[0]);
                if (ie < c[1]) chunks.put(ie, c[1] - ie);
                ph.add(new long[]{is, ie});
            }
            if (hit.size() > 1) { // Windows refuses to coalesce a single placeholder (error 487)
                vfree(addr, len, MEM_RELEASE | MEM_COALESCE_PLACEHOLDERS); // several -> one placeholder
                ph.clear();
                ph.add(new long[]{addr, end});
            }
        }

        /// Opens the file and a read-only section for it (the handles are returned, not cached).
        private FileMapping openFileMapping(Path file) throws Throwable {
            try (Arena a = Arena.ofConfined()) {
                MemorySegment wpath = a.allocateFrom(ValueLayout.JAVA_CHAR,
                        (toWin32LongPath(file.toAbsolutePath().toString()) + "\0").toCharArray());
                MemorySegment hFile = (MemorySegment) createFile.invokeExact(err, wpath, 0x80000000, 7, MemorySegment.NULL, 3, 0x80, MemorySegment.NULL);
                if (hFile.address() == -1L) throw new IllegalStateException("CreateFileW failed, " + lastErr());
                MemorySegment hMap = (MemorySegment) createMapping.invokeExact(err, hFile, MemorySegment.NULL, PAGE_READONLY, 0, 0, MemorySegment.NULL);
                if (hMap.address() == 0) {
                    String e = lastErr();
                    closeQuietly(hFile);
                    throw new IllegalStateException("CreateFileMappingW failed, " + e);
                }
                return new FileMapping(hFile, hMap);
            }
        }

        private FileMapping fileMapping(Path file) throws Throwable {
            if (!cacheFileMappings) return openFileMapping(file);
            Path key = file.toAbsolutePath().normalize();
            FileMapping fm = fileMappings.get(key);
            if (fm == null) {
                fm = openFileMapping(key);
                fileMappings.put(key, fm);
            }
            return fm;
        }

        private void closeFileMapping(FileMapping fm) {
            closeQuietly(fm.mapping());
            closeQuietly(fm.file());
        }

        @Override
        public long mapFile(long addr, long len, Path file, long off) throws Throwable {
            if (broken) return -1;
            if (len <= 0 || off < 0) return -1;
            long fileLen = Files.size(file);
            // Only whole 64 KiB units are mapped as a view; the last partial unit of
            // the file is copied into ordinary memory, the rest is zeros like mmap.
            long avail = Math.max(0, fileLen - off);
            @SuppressWarnings("PointlessBitwiseExpression")
            long viewLen = Math.min(len, avail & ~(GRANULE - 1));
            if (viewLen <= 0) return -1; // smaller than 64 KiB: the guest reads it
            long copyLen = Math.min(len - viewLen, avail - viewLen);
            List<long[]> ph = new ArrayList<>(); // placeholders this call holds right now
            String step = "start";
            boolean done = false;
            try {
                step = "carve (committed memory -> one placeholder)";
                carve(addr, len, ph);
                if (viewLen < len) { // split off the tail beyond the end of the file and give it back as memory
                    step = "split tail placeholder";
                    vfree(addr, viewLen, MEM_RELEASE | MEM_PRESERVE_PLACEHOLDER);
                    ph.clear();
                    ph.add(new long[]{addr, addr + viewLen});
                    ph.add(new long[]{addr + viewLen, addr + len});
                    step = "commit tail";
                    valloc2(addr + viewLen, len - viewLen, MEM_RESERVE | MEM_COMMIT | MEM_REPLACE_PLACEHOLDER, PAGE_READWRITE);
                    chunks.put(addr + viewLen, len - viewLen);
                    ph.remove(1);

                    if (copyLen > 0) {
                        step = "copy partial last unit";
                        try (FileChannel ch = FileChannel.open(file, StandardOpenOption.READ)) {
                            ByteBuffer bb = MemorySegment.ofAddress(addr + viewLen).reinterpret(copyLen).asByteBuffer();
                            long pos = off + viewLen;

                            while (bb.hasRemaining()) {
                                int r = ch.read(bb, pos);
                                if (r <= 0) break;
                                pos += r;
                            }
                        }
                    }
                }

                step = "open file mapping";
                FileMapping fm = fileMapping(file);
                try {
                    step = "MapViewOfFile3";
                    MemorySegment r = (MemorySegment) mapView3.invokeExact(err, fm.mapping(), MemorySegment.NULL, p(addr), off, viewLen,
                            MEM_REPLACE_PLACEHOLDER, PAGE_READONLY, MemorySegment.NULL, 0);
                    if (r.address() == 0)
                        throw new IllegalStateException("MapViewOfFile3(addr=0x" + Long.toHexString(addr) + ", off=" + off + ", viewLen=" + viewLen
                                + ", len=" + len + ", fileLen=" + fileLen + ") failed, " + lastErr());
                } finally {
                    // the view keeps the section alive, so uncached handles can be closed right away
                    if (!cacheFileMappings) closeFileMapping(fm);
                }
                views.put(addr, viewLen);
                ph.clear();
                done = true;
                return viewLen;
            } catch (Throwable t) {
                broken = true;
                throw new IllegalStateException("host mapping step '" + step + "' failed (host mapping is now off for this session): "
                        + t.getMessage() + " | " + describe(addr, len), t);
            } finally {
                // failed: the guest's memory must stay usable
                if (!done) giveBack(addr, len);
            }
        }

        @Override
        public void restore(long addr, long len) throws Throwable {
            Long viewLen = views.get(addr);
            if (viewLen == null) throw new IllegalStateException("not a mapped view: " + Long.toHexString(addr));
            int ok = (int) unmapView2.invokeExact(err, SELF, p(addr), MEM_PRESERVE_PLACEHOLDER);
            if (ok == 0) throw new IllegalStateException("UnmapViewOfFile2 failed, " + lastErr());
            views.remove(addr);
            valloc2(addr, viewLen, MEM_RESERVE | MEM_COMMIT | MEM_REPLACE_PLACEHOLDER, PAGE_READWRITE);
            chunks.put(addr, viewLen);
        }

        @Override
        public void release(long base, long bytes) {
            for (long a : new ArrayList<>(views.keySet())) {
                try {
                    int _ = (int) unmapView2.invokeExact(err, SELF, p(a), 0);
                } catch (Throwable _) {
                }
            }
            for (long a : new ArrayList<>(chunks.keySet())) {
                try {
                    vfree(a, 0, MEM_RELEASE);
                } catch (Throwable _) {
                }
            }
            try {
                if (tailStart != 0) vfree(tailStart, 0, MEM_RELEASE);
            } catch (Throwable _) {
            }
            fileMappings.values().forEach(this::closeFileMapping);
            fileMappings.clear();
            views.clear();
            chunks.clear();
            tailStart = 0;
        }

        private record FileMapping(MemorySegment file, MemorySegment mapping) {
        }
    }
}