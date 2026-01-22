package Astronomy.shapes;

import java.awt.Rectangle;
import java.awt.geom.Area;
import java.awt.geom.Path2D;
import java.awt.geom.PathIterator;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import astroj.FitsJ;
import astroj.WCS;
import ij.IJ;
import ij.ImagePlus;
import ij.astro.logging.AIJLogger;

public record WcsShape(List<WCSCurve> curves) {
    public Area getArea(WCS wcs) {
        if (wcs == null) {
            return new Area();
        }

        var p = new Path2D.Double();

        for (WCSCurve curve : curves) {
            switch (curve) {
                case WCSCurve.WCSSegClose _ -> p.closePath();
                case WCSCurve.WCSMoveTo(var x, var y) -> {
                    var radec = wcs.wcs2pixels(new double[]{x, y});
                    p.moveTo(radec[0], radec[1]);
                }
                case WCSCurve.WCSLineTo(var x, var y) -> {
                    var radec = wcs.wcs2pixels(new double[]{x, y});
                    p.lineTo(radec[0], radec[1]);
                }
                case WCSCurve.WCSQuadTo(var x0, var y0, var x1, var y1) -> {
                    var radec0 = wcs.wcs2pixels(new double[]{x0, y0});
                    var radec1 = wcs.wcs2pixels(new double[]{x1, y1});
                    p.quadTo(radec0[0], radec0[1], radec1[0], radec1[1]);
                }
                case WCSCurve.WCSCubicTo(var x0, var y0, var x1, var y1, var x2, var y2) -> {
                    var radec0 = wcs.wcs2pixels(new double[]{x0, y0});
                    var radec1 = wcs.wcs2pixels(new double[]{x1, y1});
                    var radec2 = wcs.wcs2pixels(new double[]{x2, y2});
                    p.curveTo(radec0[0], radec0[1], radec1[0], radec1[1], radec2[0], radec2[1]);
                }
            }
        }

        return new Area(p);
    }

    public static WcsShape fromArea(WCS wcs, Area area) {
        if (wcs == null) {
            return new WcsShape(List.of());
        }

        var curves = new ArrayList<WCSCurve>();

        var iter = area.getPathIterator(null);
        var points = new double[6];
        while (!iter.isDone()) {
            var type = iter.currentSegment(points);

            switch (type) {
                case PathIterator.SEG_CLOSE ->
                        curves.add(new WCSCurve.WCSSegClose());
                case PathIterator.SEG_MOVETO ->
                        curves.add(new WCSCurve.WCSMoveTo(wcs.pixels2wcs(Arrays.copyOfRange(points, 0, 2))));
                case PathIterator.SEG_LINETO ->
                        curves.add(new WCSCurve.WCSLineTo(wcs.pixels2wcs(Arrays.copyOfRange(points, 0, 2))));
                case PathIterator.SEG_QUADTO ->
                        curves.add(
                                new WCSCurve.WCSQuadTo(
                                        wcs.pixels2wcs(Arrays.copyOfRange(points, 0, 2)),
                                        wcs.pixels2wcs(Arrays.copyOfRange(points, 3, 5))
                                ));
                case PathIterator.SEG_CUBICTO ->
                        curves.add(
                                new WCSCurve.WCSCubicTo(
                                        wcs.pixels2wcs(Arrays.copyOfRange(points, 0, 2)),
                                        wcs.pixels2wcs(Arrays.copyOfRange(points, 3, 5)),
                                        wcs.pixels2wcs(Arrays.copyOfRange(points, 5, 7))
                                ));
            }

            iter.next();
        }

        return new WcsShape(Collections.unmodifiableList(curves));
    }

    public static WcsShape createCommonRegion(ImagePlus imp) {
        return createCommonRegion(imp, 1, imp.getStackSize()+1);
    }

    public static WcsShape createCommonRegion(ImagePlus imp, int start, int end) {
        Area area = null;
        WCS initial = null;
        for (int i = start; i < end; i++) {
            if (imp.getImageStack().isVirtual()) {
                // Load WCS if not loaded
                var hdr = FitsJ.getHeader(imp, i);
                var wcs = new WCS(hdr);
                if (!wcs.hasWCS()) {
                    imp.setSliceWithoutUpdate(i);
                }
            }
            var hdr = FitsJ.getHeader(imp, i);
            var wcs = new WCS(hdr);

            if (wcs.hasWCS()) {
                if (area == null) {
                    initial = wcs;
                    area = new Area(new Rectangle(0, 0, imp.getWidth(), imp.getHeight()));
                    continue;
                }

                var current = convertToArea(initial, wcs, imp);

                area.intersect(current);

                if (area.isEmpty()) {
                    AIJLogger.log("WCS intersection became empty at slice " + i);
                    IJ.showProgress(1);
                    break;
                }

                IJ.showProgress(i / (float)(end - start));
            } else {
                AIJLogger.log("Skipping unplatesolved slice: " + i);
            }
        }

        if (imp.getImageStack().isVirtual()) {
            imp.setSliceWithoutUpdate(start);
        }
        
        if (area == null) {
            IJ.error("WCS Intersection failed to find any paltesolved images");
        }

        return WcsShape.fromArea(initial, area);
    }

    private static Area convertToArea(WCS initial, WCS current, ImagePlus imp) {
        var p0 = current.pixels2wcs(new double[]{0, 0});
        var p1 = current.pixels2wcs(new double[]{imp.getWidth(), 0});
        var p2 = current.pixels2wcs(new double[]{imp.getWidth(), imp.getHeight()});
        var p3 = current.pixels2wcs(new double[]{0, imp.getHeight()});

        var p = new Path2D.Double();

        var s = initial.wcs2pixels(p0);
        p.moveTo(s[0], s[1]);

        s = initial.wcs2pixels(p1);
        p.lineTo(s[0], s[1]);

        s = initial.wcs2pixels(p2);
        p.lineTo(s[0], s[1]);

        s = initial.wcs2pixels(p3);
        p.lineTo(s[0], s[1]);

        p.closePath();
        return new Area(p);
    }

    @Override
    public String toString() {
        return "WcsShape{" +
                "curves=" + curves +
                '}';
    }

    public sealed interface WCSCurve {
        record WCSMoveTo(double x, double y) implements WCSCurve {
            public WCSMoveTo(double[] point) {
                this(point[0], point[1]);
            }
        }
        record WCSLineTo(double x, double y) implements WCSCurve {
            public WCSLineTo(double[] point) {
                this(point[0], point[1]);
            }
        }
        record WCSQuadTo(double x1, double y1, double x2, double y2) implements WCSCurve {
            public WCSQuadTo(double[] point0, double[] point1) {
                this(point0[0], point0[1], point1[0], point1[1]);
            }
        }
        record WCSCubicTo(double x1, double y1, double x2, double y2, double x3, double y3) implements WCSCurve {
            public WCSCubicTo(double[] point0, double[] point1, double[] point2) {
                this(point0[0], point0[1], point1[0], point1[1], point2[0], point2[1]);
            }
        }
        record WCSSegClose() implements WCSCurve {}
    }
}
