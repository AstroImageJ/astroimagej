package astroimagej.tests.util;

import java.nio.file.Path;

public class FileUtils {
    public static final Path TEST_DATA = Path.of("../repos/aijtestdata");

    public static Path getTestFile(String path) {
        return TEST_DATA.resolve(path).normalize().toAbsolutePath();
    }

    public static Path getTestFile(Path path) {
        return TEST_DATA.resolve(path).normalize().toAbsolutePath();
    }
}
