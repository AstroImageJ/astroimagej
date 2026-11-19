package astroj.astrometry.solvers;

import astroj.AstrometrySetup;
import astroj.astrometry.AnSolveWasi;
import ij.IJ;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Set;
import java.util.function.BooleanSupplier;
import java.util.stream.IntStream;

public final class WasmAstrometryNet implements PlateSolver {
    private final AnSolveWasi solver;
    private final int maxRetries;
    private final BooleanSupplier cancelled;
    private final Set<Path> indexes;

    private WasmAstrometryNet(int maxRetries, BooleanSupplier cancelled) {
        this.cancelled = cancelled != null ? cancelled : () -> false;
        try {
            solver = new AnSolveWasi(readLib("/astroj/wasm/augment-xylist.wasm"),
                    readLib("/astroj/wasm/astrometry-engine-hostmmap.wasm"), true, cancelled);
        } catch (Exception e) {
            IJ.error("PlateSolver", "Failed to initialize WasmAstrometryNet");
            throw new RuntimeException(e);
        }
        this.maxRetries = maxRetries;
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
        this(maxRetries, cancelled);
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
                        return new Result.Success(wcsString);
                    }
                    if (cancelled.getAsBoolean()) {
                        return new Result.Failure("Cancelled");
                    }
                }
                return new Result.Failure("Not solved");
            }
            return new Result.Success(wcsString);//todo annotations
        } catch (Exception e) {
            e.printStackTrace();
            return new Result.Failure(e.getMessage());
        }
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

    @Override
    public void close() throws Exception {
        solver.close();
    }
}
