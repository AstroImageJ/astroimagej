package ij.astro.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumSet;
import java.util.LinkedList;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Predicate;

import ij.io.Opener;

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
                    if (ps.endsWith(fileExtension)) {
                        return true;
                    }
                }
                return false;
            }, opener, matchComplete);
        }

        public AssociationMapper(BiConsumer<Path, Set<Opener.OpenOption>> opener, boolean matchComplete, final FileType... fileTypes) {
            this(p -> {
                for (FileType fileType : fileTypes) {
                    if (fileType.matches(p)) {
                        return true;
                    }
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

    /**
     * @param extension the file extension to check for.
     * @param magicNumber the magic number to check for.
     * @param mismatchExpected whether the magic number is expected to be different from the file's magic number.
     */
    public record FileType(String extension, int[] magicNumber, boolean mismatchExpected) {
        public FileType(String extension) {
            this(extension, new int[0], false);
        }

        public boolean matches(Path path) {
            if (mismatchExpected) {
                return matchesExtension(path) && !matchesMagicNumber(path);
            }

            return matchesExtension(path) && matchesMagicNumber(path);
        }

        private boolean matchesExtension(Path path) {
            return path.toString().endsWith(extension);
        }

        private boolean matchesMagicNumber(Path path) {
            if (magicNumber.length > 0) {
                try (var is = Files.newInputStream(path)) {
                    var fileMagicNumber = new byte[magicNumber.length];
                    if (is.read(fileMagicNumber) != magicNumber.length) {
                        return false;
                    }

                    for (int i = 0; i < fileMagicNumber.length; i++) {
                        if (Byte.toUnsignedInt(fileMagicNumber[i]) != this.magicNumber[i]) {
                            return false;
                        }
                    }

                    return true;
                } catch (IOException e) {
                    e.printStackTrace();
                    return false;
                }
            }

            return true;
        }
    }

    enum MapperCase {
        MATCHES_AND_DONE,
        MATCHES,
        DOES_NOT_MATCH;
    }
}
