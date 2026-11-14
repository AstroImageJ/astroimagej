package astroj.astrometry.index.data;

import nom.tam.fits.Fits;
import nom.tam.fits.FitsException;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;

/// @param id          folder subpath
/// @param name        series name
/// @param description series description
public record IndexSeries(String id, String name, String description, List<IndexFamily> families) {
    /// @param id         the unique index id
    /// @param scaleLower the lower plate scale (radians)
    /// @param scaleUpper the upper plate scale (radians)
    /// @param files      the index files in this family
    public record IndexFamily(int id, double scaleLower, double scaleUpper, List<IndexFile> files) {
        public static String formatScale(double radians) {
            var degrees = Math.toDegrees(radians);
            var absDegrees = Math.abs(degrees);
            var useSymbols = true;

            if (absDegrees >= 1.0) {
                return formatNumber(degrees, 3) + (useSymbols ? "°" : " degrees");
            }

            var arcmin = degrees * 60.0;
            if (Math.abs(arcmin) >= 1.0) {
                return formatNumber(arcmin, 2) + (useSymbols ? "′" : " arcmins");
            }

            var arcsec = arcmin * 60.0;
            return formatNumber(arcsec, 2) + (useSymbols ? "″" : " arcsecs");
        }

        private static String formatNumber(double value, int maxDecimals) {
            var format = new DecimalFormat("0." + "#".repeat(maxDecimals));
            return format.format(value);
        }

        /// Finds the families present in a single folder (not recursive), reading only the FITS headers.
        /// Used to show what is installed without needing the remote listing, so the files carry
        /// an empty hash. Unreadable files are skipped.
        public static List<IndexFamily> scanDirectory(Path dir) {
            if (!Files.isDirectory(dir)) {
                return List.of();
            }

            try (var paths = Files.list(dir)) {
                var grouped = new LinkedHashMap<Integer, List<ParsedIndex>>();
                paths.filter(Files::isRegularFile)
                        .filter(p -> p.getFileName().toString().endsWith(".fits"))
                        .sorted()
                        .forEach(p -> {
                            try {
                                var parsed = readIndex(p, false);
                                grouped.computeIfAbsent(parsed.id(), _ -> new ArrayList<>()).add(parsed);
                            } catch (UncheckedIOException ignored) {
                                // Not a readable index, leave it out
                            }
                        });

                var families = new ArrayList<IndexFamily>(grouped.size());
                grouped.forEach((id, indexes) -> {
                    var first = indexes.getFirst();
                    families.add(new IndexFamily(id, first.scaleLower(), first.scaleUpper(),
                            indexes.stream().map(ParsedIndex::file).toList()));
                });
                return families;
            } catch (IOException e) {
                return List.of();
            }
        }

        private static ParsedIndex readIndex(Path path, boolean hash) {
            try (var fits = new Fits(path.toFile())) {
                var header = fits.getHDU(0).getHeader();

                var id = header.getIntValue("INDEXID");
                var scaleLower = header.getDoubleValue("SCALE_L");
                var scaleUpper = header.getDoubleValue("SCALE_U");

                var file = new IndexFile(
                        path.getFileName().toString(),
                        hash ? sha256(path) : "",
                        path.toUri(),
                        Files.size(path)
                );

                return new ParsedIndex(id, scaleLower, scaleUpper, file);
            } catch (FitsException | IOException e) {
                throw new UncheckedIOException(
                        new IOException("Failed to read index file: " + path, e)
                );
            }
        }

        public static String sha256(Path path) throws IOException {
            try (var in = Files.newInputStream(path)) {
                var digest = MessageDigest.getInstance("SHA-256");
                var buffer = new byte[64 * 1024];

                for (int n; (n = in.read(buffer)) != -1; ) {
                    digest.update(buffer, 0, n);
                }

                return HexFormat.of().formatHex(digest.digest());
            } catch (NoSuchAlgorithmException e) {
                throw new AssertionError(e);
            }
        }

        @Override
        public String toString() {
            return id + " with scale " + formatScale(scaleLower) + " to " + formatScale(scaleUpper);
        }

        /// @return the combined size in bytes of every file in this family
        public long totalSize() {
            return files.stream().mapToLong(IndexFile::size).sum();
        }

        record ParsedIndex(int id, double scaleLower, double scaleUpper, IndexFile file) {
        }

        /// @param size file size in bytes
        public record IndexFile(String name, String sha256, URI source, long size) {
        }
    }
}
