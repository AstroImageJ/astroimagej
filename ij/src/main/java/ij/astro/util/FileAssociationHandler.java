package ij.astro.util;

import java.nio.file.Path;
import java.util.EnumSet;
import java.util.LinkedList;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * Handles the registration and execution of file handlers.
 */
public final class FileAssociationHandler {
    private static final LinkedList<AssociationMapper> handlers = new LinkedList<>();
    private static final EnumSet<MapperCase> match = EnumSet.of(MapperCase.MATCHES_AND_DONE, MapperCase.MATCHES);

    /**
     * Checks registered file listeners for extra handling that must be done.
     * @param path The path to check and handle.
     * @return if the open operation should continue.
     */
    public static boolean handleFile(String path) {
        return handleFile(Path.of(path));
    }

    /**
     * Checks registered file listeners for extra handling that must be done.
     * @param path The path to check and handle.
     * @return if the open operation should continue.
     */
    public static boolean handleFile(Path path) {
        synchronized (AssociationMapper.class) {
            for (AssociationMapper handler : handlers) {
                var m = handler.open(path);
                if (match.contains(m)) return m == MapperCase.MATCHES_AND_DONE;
            }
        }
        return false;
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
    public record AssociationMapper(Predicate<Path> associationPredicate, Consumer<Path> opener, boolean matchComplete) {
        public AssociationMapper(Consumer<Path> opener, boolean matchComplete, final String... fileExtensions) {
            this(p -> {
                var ps = p.toString();
                for (String fileExtension : fileExtensions) {
                    return ps.endsWith(fileExtension);
                }
                return false;
            }, opener, matchComplete);
        }

        public AssociationMapper(Consumer<Path> opener, final String... fileExtensions) {
            this(p -> {
                var ps = p.toString();
                for (String fileExtension : fileExtensions) {
                    return ps.endsWith(fileExtension);
                }
                return false;
            }, opener);
        }

        public AssociationMapper(Predicate<Path> associationPredicate, Consumer<Path> opener) {
            this(associationPredicate, opener, false);
        }

        /**
         * @param path the path to check.
         * @return if the path matches the predicate and the opener was run.
         */
        public MapperCase open(Path path) {
            if (associationPredicate.test(path)) {
                opener.accept(path);
                return matchComplete ? MapperCase.MATCHES_AND_DONE : MapperCase.MATCHES;
            }
            return MapperCase.DOES_NOT_MATCH;
        }
    }

    enum MapperCase {
        MATCHES_AND_DONE,
        MATCHES,
        DOES_NOT_MATCH;
    }
}
