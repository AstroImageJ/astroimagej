package ij.astro.logging;

import ij.IJ;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;

public class ConsoleLogging {
    public static void duplicateConsole2File() {
        var p = getLogPath();
        System.out.println("Logging to " + p);

        try {
            Files.deleteIfExists(p);
            Files.createDirectories(p.getParent());
            Files.createFile(p);
            var os = Files.newOutputStream(p);
            var duplicatingOut = new DuplicatingPrintStream(os, System.out);
            var duplicatingErr = new DuplicatingPrintStream(os, System.err);

            System.setErr(duplicatingErr);
            System.setOut(duplicatingOut);

            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                duplicatingErr.close();
                duplicatingOut.close();
            }));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static Path getLogPath() {
        if (IJ.isWindows()) {
            // Windows convention: %LOCALAPPDATA%\AstroImageJ\Logs\aij.log
            return Path.of(System.getenv("LOCALAPPDATA")).resolve("AstroImageJ").resolve("Logs").resolve("aij.log");
        } else if (IJ.isMacOSX()) {
            // macOS: ~/Library/Logs/AstroImageJ/aij.log
            Path home = Path.of(System.getProperty("user.home", "."));
            return home.resolve("Library").resolve("Logs").resolve("AstroImageJ").resolve("aij.log");
        } else if (IJ.isLinux()){
            // Assume Linux/Unix. Use XDG_STATE_HOME if available, else ~/.local/state
            String xdgState = System.getenv("XDG_STATE_HOME");
            Path stateBase = (xdgState != null && !xdgState.isBlank())
                    ? Path.of(xdgState)
                    : Path.of(System.getProperty("user.home", ".")).resolve(".local").resolve("state");
            // Convention: ~/.local/state/astroiamgej/logs/aij.log
            return stateBase.resolve("astroimagej").resolve("logs").resolve("aij.log");
        } else {
            throw new UnsupportedOperationException("Unsupported platform: " + System.getProperty("os.name"));
        }
    }

    static class DuplicatingPrintStream extends PrintStream {
        private final ArrayList<PrintStream> output = new ArrayList<>();

        public DuplicatingPrintStream(File file, PrintStream... streams) throws FileNotFoundException {
            super(file);

            if (streams != null) {
                Collections.addAll(output, streams);
            }
        }

        public DuplicatingPrintStream(OutputStream outputStream, PrintStream... streams) {
            super(outputStream);

            if (streams != null) {
                Collections.addAll(output, streams);
            }
        }

        @Override
        public void write(byte[] buf, int off, int len) {
            synchronized (this) {
                super.write(buf, off, len);
                output.forEach(o -> o.write(buf, off, len));
            }
        }

        @Override
        public void write(int b) {
            synchronized (this) {
                super.write(b);
                output.forEach(o -> o.write(b));
            }
        }

        @Override
        public void flush() {
            synchronized (this) {
                super.flush();
                output.forEach(PrintStream::flush);
            }
        }

        @Override
        public void close() {
            flush();
            super.close();
        }
    }

}
