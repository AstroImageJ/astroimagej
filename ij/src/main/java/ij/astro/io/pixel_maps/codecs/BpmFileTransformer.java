package ij.astro.io.pixel_maps.codecs;

import ij.astro.io.aij.AijFileCodec;
import ij.astro.io.aij.Section;
import ij.astro.io.aij.Transformer;
import ij.astro.io.pixel_maps.BpmFile;
import ij.astro.io.pixel_maps.BpmHeader;
import ij.astro.logging.AIJLogger;
import ij.astro.util.PixelPatcher;

import java.util.HashSet;

public class BpmFileTransformer extends Transformer<BpmFile, Void> {
    private static final String PATCH_TYPE_KEY = "patchtype";
    private static final String PIXEL_KEY = "px";
    private static final Section.Parameter<PixelPatcher.PatchType.Type> PATCH_TYPE_PARAM =
            new Section.Parameter<>(PATCH_TYPE_KEY, 0, PixelPatcher.PatchType.Type.class);
    private static final Section.Parameter<Integer> INT_X_PARAMETER = new Section.Parameter<>("x", 0, Integer.TYPE);
    private static final Section.Parameter<Integer> INT_Y_PARAMETER = new Section.Parameter<>("y", 1, Integer.TYPE);
    private static final Section.Parameter<Integer> INTEGER_PARAMETER = new Section.Parameter<>("val", 0, Integer.TYPE);
    private static final Section.Parameter<Boolean> BOOLEAN_PARAMETER = new Section.Parameter<>("truthy", 0, Boolean.TYPE);
    private static final Section.Parameter<Double> DOUBLE_PARAMETER = new Section.Parameter<>("val", 0, Double.TYPE);
    private static final Section.Parameter<PixelPatcher.PatchType.NearestNeighbor.MergeType> MERGE_TYPE_PARAMETER =
            new Section.Parameter<>("val", 0, PixelPatcher.PatchType.NearestNeighbor.MergeType.class);

    public BpmFileTransformer(AijFileCodec codec) {
        super(codec);
    }

    @Override
    public BpmFile load(Void parameter, Section section) {
        var view = section.createMapView();

        if (!view.contains(BpmHeaderTransformer.HEADER)) {
            return null;
        }

        var header = codec.read(BpmHeader.class, getUniqueSection(view, BpmHeaderTransformer.HEADER));
        if (header.majorVersion() > BpmHeader.MAJOR_VERSION || header.minorVersion() > BpmHeader.MINOR_VERSION) {
            AIJLogger.log("""
                        This BPM file contains a newer format (%s) than what is currently supported (%s).
                        Attempting to read anyway...
                        """.formatted(header.majorVersion() + "." + header.minorVersion(),
                    BpmHeader.MAJOR_VERSION + "." + BpmHeader.MINOR_VERSION));
        }

        var bpmFile = new BpmFile(header);
        for (var patchSection : view.get(PATCH_TYPE_KEY)) {
            var patchType = patchSection.getParameter(PATCH_TYPE_PARAM);
            if (patchType == null) {
                AIJLogger.log("Unknown patch type: " + patchSection.getParameter(PATCH_TYPE_PARAM.index(), PATCH_TYPE_PARAM.name()) +
                        ". Skipping patch.");
                continue;
            }
            var patch = readPatchTypeOptions(patchType, patchSection);
            var patchView = patchSection.createMapView();
            bpmFile.patches().computeIfAbsent(patch, _ -> new HashSet<>());
            var pixelList = bpmFile.patches().get(patch);
            var pSecs = patchView.get(PIXEL_KEY);
            for (var pSec : pSecs) {
                pixelList.add(new PixelPatcher.BpmPixel.Pixel(pSec.getParameter(INT_X_PARAMETER), pSec.getParameter(INT_Y_PARAMETER)));
            }
        }

        return bpmFile;
    }

    @Override
    public Section write(Void parameter, BpmFile obj) {
        var s = new Section("root", true);

        s.addSubsection(codec.write(BpmHeader.class, obj.header()));

        obj.patches().forEach((patchType, pixelList) -> {
            var patchSection = Section.createSection(PATCH_TYPE_KEY, PATCH_TYPE_PARAM, patchType.toType());
            addPatchTypeOptions(patchType, patchSection);
            for (var pixel : pixelList) {
                patchSection.addSubsection(Section.createSection(PIXEL_KEY, INT_X_PARAMETER, pixel.x(), INT_Y_PARAMETER, pixel.y()));
            }
            s.addSubsection(patchSection);
        });

        return s;
    }

    private void addPatchTypeOptions(PixelPatcher.PatchType patchType, Section patchSection) {
        switch (patchType) {
            case PixelPatcher.PatchType.AverageFill(int xRadius, int yRadius) -> {
                patchSection.addSubsection(
                        Section.createSection("radius", INT_X_PARAMETER, xRadius, INT_Y_PARAMETER, yRadius)
                );
            }
            case PixelPatcher.PatchType.ConstantValue(double value) -> {
                patchSection.addSubsection(
                        Section.createSection("constant",
                                DOUBLE_PARAMETER, value)
                );
            }
            case PixelPatcher.PatchType.FitGaussian(int minCount, int maxIter, double relErr, double absErr) -> {
                patchSection.addSubsection(
                        Section.createSection("minCount", INTEGER_PARAMETER, minCount)
                );
                patchSection.addSubsection(
                        Section.createSection("maxIter", INTEGER_PARAMETER, maxIter)
                );
                patchSection.addSubsection(
                        Section.createSection("relErr", DOUBLE_PARAMETER, relErr)
                );
                patchSection.addSubsection(
                        Section.createSection("absErr", DOUBLE_PARAMETER, absErr)
                );
            }
            case PixelPatcher.PatchType.FitMoffat(int minCount, int maxIter, double relErr, double absErr) -> {
                patchSection.addSubsection(
                        Section.createSection("minCount", INTEGER_PARAMETER, minCount)
                );
                patchSection.addSubsection(
                        Section.createSection("maxIter", INTEGER_PARAMETER, maxIter)
                );
                patchSection.addSubsection(
                        Section.createSection("relErr", DOUBLE_PARAMETER, relErr)
                );
                patchSection.addSubsection(
                        Section.createSection("absErr", DOUBLE_PARAMETER, absErr)
                );
            }
            case PixelPatcher.PatchType.FloodFill(boolean useMedian) -> {
                patchSection.addSubsection(
                        Section.createSection("median", BOOLEAN_PARAMETER, useMedian)
                );
            }
            case PixelPatcher.PatchType.MedianFill(int xRadius, int yRadius) -> {
                patchSection.addSubsection(
                        Section.createSection("radius", INT_X_PARAMETER, xRadius, INT_Y_PARAMETER, yRadius)
                );
            }
            case PixelPatcher.PatchType.NearestNeighbor(PixelPatcher.PatchType.NearestNeighbor.MergeType mergeType) -> {
                patchSection.addSubsection(
                        Section.createSection("mergeType", MERGE_TYPE_PARAMETER, mergeType)
                );
            }
            case PixelPatcher.PatchType.FitPlane _ -> {
            }
            case PixelPatcher.PatchType.PassThrough _ -> {
            }
        }
    }

    private PixelPatcher.PatchType readPatchTypeOptions(PixelPatcher.PatchType.Type patchType, Section patchSection) {
        var view = patchSection.createMapView();
        return switch (patchType) {
            case PixelPatcher.PatchType.Type.AVERAGE_FILL -> {
                var radius = getUniqueSection(view, "radius", false);
                if (radius != null) {
                    var x = radius.getParameter(INT_X_PARAMETER);
                    var y = radius.getParameter(INT_Y_PARAMETER);
                    yield new PixelPatcher.PatchType.AverageFill(x, y);
                }
                yield PixelPatcher.PatchType.Type.AVERAGE_FILL.toPatchType();
            }
            case PixelPatcher.PatchType.Type.CONSTANT_VALUE -> {
                var cons = getUniqueSection(view, "constant", false);
                if (cons != null) {
                    yield new PixelPatcher.PatchType.ConstantValue(cons.getParameter(DOUBLE_PARAMETER));
                }
                yield PixelPatcher.PatchType.Type.CONSTANT_VALUE.toPatchType();
            }
            case PixelPatcher.PatchType.Type.FIT_GAUSSIAN -> {
                var maxIter = PixelPatcher.PatchType.FitGaussian.MAX_ITER.get();
                var minCount = PixelPatcher.PatchType.FitGaussian.MIN_COUNT.get();
                var relErr = PixelPatcher.PatchType.FitGaussian.REL_ERR.get();
                var absErr = PixelPatcher.PatchType.FitGaussian.ABS_ERR.get();

                var maxIterSec = getUniqueSection(view, "maxIter", false);
                if (maxIterSec != null) {
                    maxIter = maxIterSec.getParameter(INTEGER_PARAMETER);
                }
                var minCountSec = getUniqueSection(view, "minCount", false);
                if (minCountSec != null) {
                    minCount = minCountSec.getParameter(INTEGER_PARAMETER);
                }
                var relErrSec = getUniqueSection(view, "relErr", false);
                if (relErrSec != null) {
                    relErr = relErrSec.getParameter(DOUBLE_PARAMETER);
                }
                var absErrSec = getUniqueSection(view, "absErr", false);
                if (absErrSec != null) {
                    absErr = absErrSec.getParameter(DOUBLE_PARAMETER);
                }

                yield new PixelPatcher.PatchType.FitGaussian(minCount, maxIter, relErr, absErr);
            }
            case PixelPatcher.PatchType.Type.FIT_MOFFAT -> {
                var maxIter = PixelPatcher.PatchType.FitMoffat.MAX_ITER.get();
                var minCount = PixelPatcher.PatchType.FitMoffat.MIN_COUNT.get();
                var relErr = PixelPatcher.PatchType.FitMoffat.REL_ERR.get();
                var absErr = PixelPatcher.PatchType.FitMoffat.ABS_ERR.get();

                var maxIterSec = getUniqueSection(view, "maxIter", false);
                if (maxIterSec != null) {
                    maxIter = maxIterSec.getParameter(INTEGER_PARAMETER);
                }
                var minCountSec = getUniqueSection(view, "minCount", false);
                if (minCountSec != null) {
                    minCount = minCountSec.getParameter(INTEGER_PARAMETER);
                }
                var relErrSec = getUniqueSection(view, "relErr", false);
                if (relErrSec != null) {
                    relErr = relErrSec.getParameter(DOUBLE_PARAMETER);
                }
                var absErrSec = getUniqueSection(view, "absErr", false);
                if (absErrSec != null) {
                    absErr = absErrSec.getParameter(DOUBLE_PARAMETER);
                }

                yield new PixelPatcher.PatchType.FitMoffat(minCount, maxIter, relErr, absErr);
            }
            case PixelPatcher.PatchType.Type.FLOOD_FILL -> {
                var med = getUniqueSection(view, "median", false);
                if (med != null) {
                    yield new PixelPatcher.PatchType.FloodFill(med.getParameter(BOOLEAN_PARAMETER));
                }
                yield new PixelPatcher.PatchType.FloodFill();
            }
            case PixelPatcher.PatchType.Type.MEDIAN_FILL -> {
                var radius = getUniqueSection(view, "radius", false);
                if (radius != null) {
                    var x = radius.getParameter(INT_X_PARAMETER);
                    var y = radius.getParameter(INT_Y_PARAMETER);
                    yield new PixelPatcher.PatchType.MedianFill(x, y);
                }
                yield new PixelPatcher.PatchType.MedianFill();
            }
            case PixelPatcher.PatchType.Type.NEAREST_NEIGHBOR -> {
                var type = getUniqueSection(view, "mergeType", false);
                if (type != null) {
                    yield new PixelPatcher.PatchType.NearestNeighbor(type.getParameter(MERGE_TYPE_PARAMETER));
                }
                yield new PixelPatcher.PatchType.NearestNeighbor();
            }
            case PixelPatcher.PatchType.Type.FIT_PLANE -> new PixelPatcher.PatchType.FitPlane();
            case PixelPatcher.PatchType.Type.PASS_THROUGH -> new PixelPatcher.PatchType.PassThrough();
        };
    }
}
