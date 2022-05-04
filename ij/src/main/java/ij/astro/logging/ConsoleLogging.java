package ij.astro.logging;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;

public class ConsoleLogging {
    public static void duplicateConsole2File() {
        var p = Path.of("aij.log");

        try {
            Files.deleteIfExists(p);
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
