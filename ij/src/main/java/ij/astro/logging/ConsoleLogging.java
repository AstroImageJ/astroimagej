package ij.astro.logging;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;

public class ConsoleLogging {
    public static void duplicateConsole2File() {
        var p = Path.of("aij.log");

        try {
            Files.deleteIfExists(p);
            var duplicatingOut = new DuplicatingPrintStream(p.toFile(), System.out);
            var duplicatingErr = new DuplicatingPrintStream(p.toFile(), System.err);
            System.setErr(duplicatingErr);
            System.setOut(duplicatingOut);
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

        @Override
        public void write(byte[] buf, int off, int len) {
            super.write(buf, off, len);
            output.forEach(o -> o.write(buf, off, len));
        }

        @Override
        public void write(int b) {
            super.write(b);
            output.forEach(o -> o.write(b));
        }

        @Override
        public void flush() {
            super.flush();
            output.forEach(PrintStream::flush);
        }
    }

}
