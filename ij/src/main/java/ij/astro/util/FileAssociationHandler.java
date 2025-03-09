package ij.astro.util;

import ij.io.Opener;

import java.nio.file.Path;
import java.util.EnumSet;
import java.util.LinkedList;
import java.util.Set;
import java.util.function.BiConsumer;
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
    public static boolean handleFile(String path, Set<Opener.OpenOption> openOptions) {
        return handleFile(Path.of(path), openOptions);
    }

    /**
     * Checks registered file listeners for extra handling that must be done.
     * @param path The path to check and handle.
     * @return if the open operation should continue.
     */
    public static boolean handleFile(Path path, Set<Opener.OpenOption> openOptions) {
        synchronized (AssociationMapper.class) {
            for (AssociationMapper handler : handlers) {
                var m = handler.open(path, openOptions);
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
    public record AssociationMapper(Predicate<Path> associationPredicate, BiConsumer<Path, Set<Opener.OpenOption>> opener, boolean matchComplete) {
        public AssociationMapper(BiConsumer<Path, Set<Opener.OpenOption>> opener, boolean matchComplete, final String... fileExtensions) {
            this(p -> {
                var ps = p.toString();
                for (String fileExtension : fileExtensions) {
                    return ps.endsWith(fileExtension);
                }
                return false;
            }, opener, matchComplete);
        }

        public AssociationMapper(BiConsumer<Path, Set<Opener.OpenOption>> opener, final String... fileExtensions) {
            this(opener, false, fileExtensions);
        }

        public AssociationMapper(Predicate<Path> associationPredicate, BiConsumer<Path, Set<Opener.OpenOption>> opener) {
            this(associationPredicate, opener, false);
        }

        /**
         * @param path the path to check.
         * @return if the path matches the predicate and the opener was run.
         */
        MapperCase open(Path path, Set<Opener.OpenOption> openOptions) {
            if (associationPredicate.test(path)) {
                opener.accept(path, openOptions);
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
