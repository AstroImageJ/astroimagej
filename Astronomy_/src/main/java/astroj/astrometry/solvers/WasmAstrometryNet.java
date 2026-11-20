package astroj.astrometry.solvers;

import astroj.AstrometrySetup;
import astroj.FitsJ;
import astroj.WCS;
import astroj.astrometry.AnSolveWasi;
import astroj.astrometry.annotations.AnnotationCatalog;
import ij.IJ;

import java.io.IOException;
import java.nio.file.Path;
import java.util.*;
import java.util.function.BooleanSupplier;
import java.util.stream.IntStream;

public final class WasmAstrometryNet implements PlateSolver {
    private final AnSolveWasi solver;
    private final int maxRetries;
    private final boolean annotate;
    private final BooleanSupplier cancelled;
    private final Set<Path> indexes;
    private List<AnnotationCatalog.Entry> annotationEntries;

    private WasmAstrometryNet(boolean annotate, int maxRetries, BooleanSupplier cancelled) {
        this.cancelled = cancelled != null ? cancelled : () -> false;
        try {
            solver = new AnSolveWasi(
                    readLib("/astroj/wasm/augment-xylist.wasm"),
                    readLib("/astroj/wasm/astrometry-engine-hostmmap.wasm"),
                    readLib("/astroj/wasm/kd-cone.wasm"),
                    true, cancelled);
        } catch (Exception e) {
            IJ.error("PlateSolver", "Failed to initialize WasmAstrometryNet");
            throw new RuntimeException(e);
        }
        this.maxRetries = maxRetries;
        this.annotate = annotate;
        indexes = AstrometrySetup.ASTROMETRY_NET_INDEX_MANAGER.enabledIndexDirectories();
        if (indexes == null || indexes.isEmpty()) {
            var msg = "No astrometry.net indexes are enabled. Open Astrometry Settings > Manage Indexes... to download/enable at least one.";
            IJ.log("Plate solve aborted due to missing indexes");
            IJ.error("PlateSolver", msg);
            throw new IllegalStateException(msg);
        }
    }

    //todo implement
    public WasmAstrometryNet(boolean annotate, BooleanSupplier cancelled, int maxRetries, int maxPolls) {
        this(annotate, maxRetries, cancelled);
    }

    @Override
    public Result solve(double[] xs, double[] ys, int imageWidth, int imageHeight,
                        double scaleLower, double scaleUpper,
                        double initialRa, double initialDec, double searchRadius,
                        boolean useDistortionOrder, double distortionOrder) {
        if (xs.length != ys.length)
            throw new IllegalArgumentException("xs and ys must be the same length");

        var points = IntStream.range(0, xs.length)
                        .mapToObj(i -> new double[]{xs[i], imageHeight - ys[i]})
                        .toList();

        var passedRa = false;
        if (Double.isFinite(initialRa) && Double.isFinite(initialDec)
                && initialDec >= -90 && initialDec <= 90
                && Double.isFinite(searchRadius) && searchRadius > 0) {
            initialRa = initialRa / 15;
            searchRadius = searchRadius / 60.0;
            passedRa = true;
        } else {
            initialRa = Double.NaN;
            initialDec = Double.NaN;
            searchRadius = Double.NaN;
        }
        try {
            var wcsString = solver.solve(points, imageWidth, imageHeight, scaleLower, scaleUpper, initialRa, initialDec, searchRadius,
                    true, useDistortionOrder ? Math.clamp((int) distortionOrder, 2, 9) : 0, indexes, null);
            if (wcsString == null) {
                if (cancelled.getAsBoolean()) {
                    return new Result.Failure("Cancelled");
                }
                if (passedRa && maxRetries > 0) {
                    IJ.log("Failed to solve with initial ra/dec, blind solving...");
                    wcsString = solver.solve(points, imageWidth, imageHeight, scaleLower, scaleUpper, 0, 0, 0,
                            true, useDistortionOrder ? Math.clamp((int) distortionOrder, 2, 9) : 0, indexes, null);
                    if (wcsString != null) {
                        return new Result.Success(wcsString, getAnnotations(wcsString, imageWidth, imageHeight));
                    }
                    if (cancelled.getAsBoolean()) {
                        return new Result.Failure("Cancelled");
                    }
                }
                return new Result.Failure("Not solved");
            }
            return new Result.Success(wcsString, getAnnotations(wcsString, imageWidth, imageHeight));
        } catch (Exception e) {
            e.printStackTrace();
            return new Result.Failure(e.getMessage());
        }
    }

    private Collection<AstrometryObject> getAnnotations(String wcsString, int imageWidth, int imageHeight) {
        if (!annotate) {
            return Collections.emptyList();
        }

        var out = new ArrayList<AstrometryObject>();
        var hdr = parseWcsHeader(wcsString);

        // WCS needs these
        FitsJ.setCard("IMAGEW", imageWidth, null, hdr);
        FitsJ.setCard("IMAGEH", imageHeight, null, hdr);
        FitsJ.setCard("NAXIS", 2, null, hdr);

        var wcs = new WCS(hdr);
        if (!wcs.hasWCS()) {
            return Collections.emptyList();
        }

        var entries = new ArrayList<AnnotationCatalog.Entry>();
        try {
            entries.addAll(annotationEntries());
        } catch (Exception ex) {
            ex.printStackTrace();
            IJ.log("Annotation catalogs failed: " + ex.getMessage());
        }
        try {
            entries.addAll(hdEntries(wcs));
        } catch (Exception ex) {
            ex.printStackTrace();
            IJ.log("HD annotations failed: " + ex.getMessage());
        }
        try {
            entries.addAll(tychoEntries(wcs));
        } catch (Exception ex) {
            ex.printStackTrace();
            IJ.log("Tycho-2 annotations failed: " + ex.getMessage());
        }

        // Look through catalog for stars whose center lies within the image
        var arcsecPerPix = wcs.getPixelScaleArcSec();
        for (var e : entries) {
            var p = wcs.radecToPixels(e.raDeg(), e.decDeg());
            if (p == null) {
                continue;
            }
            var x = p[0];
            var y = imageHeight - p[1];
            var radiusPx = e.radiusDeg() > 0 ? e.radiusDeg() * 3600.0 / arcsecPerPix : 0;
            if (!(x >= -0.5 && x <= imageWidth - 0.5 && y >= -0.5 && y <= imageHeight - 0.5)) {
                continue;
            }
            out.add(new AstrometryObject(radiusPx, e.type(), e.names(), x, y));
        }
        return out;
    }

    private synchronized List<AnnotationCatalog.Entry> annotationEntries() throws Exception {
        if (annotationEntries == null) {
            annotationEntries = AnnotationCatalog.load();
        }
        return annotationEntries;
    }

    private List<AnnotationCatalog.Entry> tychoEntries(WCS wcs) throws Exception {
        var tree = AnnotationCatalog.findCatalog("tycho2.kd");
        if (tree.isEmpty()) return List.of();
        var entries = new ArrayList<AnnotationCatalog.Entry>();
        var center = wcs.getCenterRadec();
        for (var hit : solver.catalog(tree.get(), center[0], center[1], wcs.getRadius(), "tyc1", "tyc2", "tyc3")) {
            double[] t = hit.tags();
            entries.add(new AnnotationCatalog.Entry("tycho2",
                    List.of("Tycho-2 %d-%d-%d".formatted((long) t[0], (long) t[1], (long) t[2])),
                    hit.raDeg(), hit.decDeg(), 0));
        }
        return entries;
    }

    private List<AnnotationCatalog.Entry> hdEntries(WCS wcs) throws Exception {
        var tree = AnnotationCatalog.findCatalog("hd.fits");
        if (tree.isEmpty()) return List.of();
        var center = wcs.getCenterRadec();
        var entries = new ArrayList<AnnotationCatalog.Entry>();
        for (var hit : solver.catalog(tree.get(), center[0], center[1], wcs.getRadius()))
            entries.add(new AnnotationCatalog.Entry("hd", List.of("HD " + (hit.index() + 1)),
                    hit.raDeg(), hit.decDeg(), 0));
        return entries;
    }

    @Override
    public boolean requiresAwait() {
        return false;
    }

    private static byte[] readLib(String path) {
        try (var i = WasmAstrometryNet.class.getResourceAsStream(path)) {
            return Objects.requireNonNull(i).readAllBytes();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Converts the header text returned by a {@link PlateSolver} into FITS cards.
     * Solvers may strip trailing padding, so the final card can be shorter than 80 characters.
     */
    private static FitsJ.Header parseWcsHeader(String text) {
        int count = (text.length() + 79) / 80;
        String[] cards = new String[count];
        for (int i = 0; i < count; i++) {
            cards[i] = String.format("%-80s", text.substring(i * 80, Math.min(text.length(), (i + 1) * 80)));
        }
        return FitsJ.Header.build(cards);
    }

    @Override
    public void close() throws Exception {
        solver.close();
    }
}
