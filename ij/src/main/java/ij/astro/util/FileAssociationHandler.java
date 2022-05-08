package ij.astro.util;

import java.nio.file.Path;
import java.util.LinkedList;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * Handles the registration and execution of file handlers.
 */
public final class FileAssociationHandler {
    private static final LinkedList<AssociationMapper> handlers = new LinkedList<>();

    /**
     * Checks registered file listeners for extra handling that must be done.
     * @param path The path to check and handle.
     */
    public static void handleFile(String path) {
        handleFile(Path.of(path));
    }

    /**
     * Checks registered file listeners for extra handling that must be done.
     * @param path The path to check and handle.
     */
    public static void handleFile(Path path) {
        synchronized (AssociationMapper.class) {
            for (AssociationMapper handler : handlers) {
                if (handler.open(path)) return;
            }
        }
    }

    /**
     * @param associationMapper the file association handler to be added,
     *                          taking precedence over previously added handlers.
     */
    public static void registerAssociation(AssociationMapper associationMapper) {
        if (handlers.contains(associationMapper)) {
            return;
        }
        handlers.addFirst(associationMapper);
    }

    /**
     * @param associationMapper the file association handler to be removed.
     */
    public static void removeAssociation(AssociationMapper associationMapper) {
        handlers.remove(associationMapper);
    }

    /**
     * Handles the checking and consumption of files.
     */
    public record AssociationMapper(Predicate<Path> associationPredicate, Consumer<Path> opener) {
        public AssociationMapper(Consumer<Path> opener, final String... fileExtensions) {
            this(p -> {
                var ps = p.toString();
                for (String fileExtension : fileExtensions) {
                    return ps.endsWith(fileExtension);
                }
                return false;
            }, opener);
        }

        /**
         * @param path the path to check.
         * @return if the path matches the predicate and the opener was run.
         */
        public boolean open(Path path) {
            if (associationPredicate.test(path)) {
                opener.accept(path);
                return true;
            }
            return false;
        }
    }
}
