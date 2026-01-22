package astroj.fits;

import java.awt.Rectangle;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.function.ToIntFunction;

import astroj.FittedPlane;
import com.google.auto.service.AutoService;
import ij.astro.util.PixelPatcher;
import ij.process.ImageProcessor;
import ij.util.ArrayUtil;

@AutoService(PixelPatcher.class)
public class PixelPatcherImpl implements PixelPatcher {
    @Override
    public void patch(ImageProcessor ip, ImageProcessor mask, PatchType patchType) {
        if (patchType instanceof PatchType.PassThrough) {
            return;
        }

        if (ip.getWidth() != mask.getWidth() || ip.getHeight() != mask.getHeight()) {
            throw new IllegalArgumentException("Mask must have same width and height!");
        }

        var visited = new boolean[ip.getHeight()][ip.getWidth()];//todo can we just modify the mask when we aren't displaying it?

        for (int y = 0; y < ip.getHeight(); y++) {
            for (int x = 0; x < ip.getWidth(); x++) {
                if (!(mask.getf(x, y) > 0)) {
                    continue;
                }

                if (visited[y][x]) {
                    continue;
                }

                switch (patchType) {
                    //noinspection DataFlowIssue
                    case PatchType.PassThrough() -> {}
                    case PatchType.ConstantValue(var val) -> ip.setf(x, y, (float) val);
                    case PatchType.AverageFill(int xRadius, int yRadius) -> {
                        var average = ArrayUtil.average(collect(ip, mask, x, y, xRadius, yRadius));

                        // No unmasked values present
                        if (Double.isNaN(average)) {
                            //continue;
                        }

                        ip.setf(x, y, (float) average);
                    }
                    case PatchType.MedianFill(int xRadius, int yRadius) -> {
                        var median = ArrayUtil.median(collect(ip, mask, x, y, xRadius, yRadius));

                        // No unmasked values present
                        if (Double.isNaN(median)) {
                            //continue;
                        }

                        ip.setf(x, y, (float) median);
                    }
                    case PatchType.FloodFill(boolean useMedian) -> {
                        var region = collectContinuousRegion(ip, mask, visited, x, y);
                        var borderValues = region.borderPixels().stream()
                                .mapToDouble(p -> ip.getf(p.x, p.y))
                                .toArray();

                        if (borderValues.length == 0) {
                            //todo how to handle
                            continue;
                        }

                        // Compute border value
                        double fillValue;
                        if (useMedian) {
                            Arrays.sort(borderValues);
                            int m = borderValues.length / 2;
                            if ((borderValues.length & 1) == 1) {
                                fillValue = borderValues[m];
                            } else {
                                fillValue = (borderValues[m - 1] + borderValues[m]) / 2.0;
                            }
                        } else {
                            var sum = 0.0D;
                            for (double v : borderValues) {
                                sum += v;
                            }
                            fillValue = sum / borderValues.length;
                        }

                        // Fill region
                        var fv = (float) fillValue;
                        for (var rp : region.pixels()) {
                            ip.setf(rp.x, rp.y, fv);
                        }
                    }
                    case PatchType.NearestNeighbor(var mergeType) -> {
                        var region = collectContinuousRegion(ip, mask, visited, x, y);
                        var borderPixels = region.borderPixels();
                        var badPixels = region.pixels();

                        if (borderPixels.isEmpty()) {
                            //todo how to handle
                            continue;
                        }

                        for (var badPixel : badPixels) {
                            if (mergeType == PatchType.NearestNeighbor.MergeType.NEAREST_NEIGHBOR) {
                                borderPixels.stream().min(
                                        Comparator.comparingDouble(
                                                bp -> Math.hypot(bp.x - badPixel.x, bp.y - badPixel.y)
                                        )
                                ).ifPresent(
                                        bp -> ip.setf(badPixel.x, badPixel.y, ip.getf(bp.x, bp.y))
                                );
                                return;
                            }

                            ToIntFunction<Pixel> distance = (bp) -> {
                                var dx = bp.x - badPixel.x;
                                var dy = bp.y - badPixel.y;
                                return dx*dx + dy*dy;
                            };

                            var nearestRounded = borderPixels.stream()
                                    .mapToInt(distance)
                                    .min()
                                    .orElseThrow();

                            var nearestBorderValues = borderPixels.stream()
                                    .filter(bp -> distance.applyAsInt(bp) == nearestRounded)
                                    .mapToDouble(bp -> ip.getf(bp.x, bp.y))
                                    .toArray();

                            double fillValue;
                            if (mergeType == PatchType.NearestNeighbor.MergeType.MEDIAN) {
                                Arrays.sort(nearestBorderValues);
                                int m = nearestBorderValues.length / 2;
                                if ((nearestBorderValues.length & 1) == 1) {
                                    fillValue = nearestBorderValues[m];
                                } else {
                                    fillValue = (nearestBorderValues[m - 1] + nearestBorderValues[m]) / 2.0;
                                }
                            } else {
                                var sum = 0.0D;
                                for (double v : nearestBorderValues) {
                                    sum += v;
                                }
                                fillValue = sum / nearestBorderValues.length;
                            }

                            // Fill region
                            var fv = (float) fillValue;
                            ip.setf(badPixel.x, badPixel.y, fv);
                        }
                    }
                    case PatchType.FitPlane() -> {
                        var region = collectContinuousRegion(ip, mask, visited, x, y);

                        var bounds = region.bounds();

                        var fitter = new FittedPlane(bounds.width * bounds.height);

                        for (int xi = bounds.x; xi < bounds.x + bounds.width; xi++) {
                            for (int yi = bounds.y; yi < bounds.y + bounds.height; yi++) {
                                if (!(mask.getf(x, y) > 0)) {
                                    fitter.addPoint(xi, yi, ip.getf(x, y));
                                }
                            }
                        }

                        if (!fitter.fitPlane()) {
                            //todo handle
                            continue;
                        }

                        for (Pixel pixel : region.pixels) {
                            ip.setf(pixel.x, pixel.y, (float) fitter.valueAt(pixel.x, pixel.y));
                        }
                    }
                }
            }
        }
    }

    private Region collectContinuousRegion(ImageProcessor ip, ImageProcessor mask, boolean[][] visited, int startX, int startY) {
        var stack = new ArrayDeque<Pixel>();
        var region = new Region();

        stack.push(new Pixel(startX, startY));
        visited[startY][startX] = true;

        while (!stack.isEmpty()) {
            var p = stack.pop();
            int px = p.x, py = p.y;
            region.addToRegion(p);

            // Explore neighbors
            for (int dy = -1; dy <= 1; dy++) {
                for (int dx = -1; dx <= 1; dx++) {
                    if (dx == 0 && dy == 0) {
                        continue;
                    }

                    int nx = px + dx, ny = py + dy;

                    if (nx < 0 || nx >= ip.getWidth() || ny < 0 || ny >= ip.getHeight()) {
                        continue;
                    }

                    if (mask.getf(nx, ny) > 0) {
                        if (!visited[ny][nx]) {
                            visited[ny][nx] = true;
                            stack.push(new Pixel(nx, ny));
                        }
                    } else {
                        region.addBorderPixel(nx, ny);
                    }
                }
            }
        }

        return region;
    }

    /**
     * Collects good pixel values in the region
     */
    private double[] collect(ImageProcessor ip, ImageProcessor mask, int xCenter, int yCenter, int xRadius, int yRadius) {
        var pixels = new double[4 * xRadius * yRadius];
        var index = 0;
        for (int j = Math.max(0, yCenter - yRadius); j < Math.min(ip.getHeight(), yCenter + yRadius); j++) {
            for (int i = Math.max(0, xCenter - xRadius); i < Math.min(ip.getWidth(), xCenter + xRadius); i++) {
                // Filter out bad pixels
                if (mask.getf(i, j) > 0) {
                    continue;
                }

                pixels[index++] = ip.getf(i, j);
            }
        }

        return Arrays.copyOf(pixels, index);
    }

    private record Pixel(int x, int y) {}

    private record Region(Rectangle bounds, List<Pixel> pixels, List<Pixel> borderPixels) {
        Region() {
            this(new Rectangle(), new ArrayList<>(), new ArrayList<>());
        }

        void addToRegion(int x, int y) {
            addToRegion(new Pixel(x, y));
        }

        void addToRegion(Pixel p) {
            pixels.add(p);
            bounds.width = Math.max(bounds.width, p.x - bounds.x);
            bounds.height = Math.max(bounds.height, p.y - bounds.y);
            bounds.x = Math.min(bounds.x, p.x);
            bounds.y = Math.min(bounds.y, p.y);
        }

        void addBorderPixel(int x, int y) {
            addBorderPixel(new Pixel(x, y));
        }

        void addBorderPixel(Pixel p) {
            borderPixels.add(p);
        }
    }
}
