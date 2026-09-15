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
        for (var patch : view.get(PATCH_TYPE_KEY)) {
            var patchType = patch.getParameter(PATCH_TYPE_PARAM);
            if (patchType == null) {
                AIJLogger.log("Unknown patch type: " + patch.getParameter(PATCH_TYPE_PARAM.index(), PATCH_TYPE_PARAM.name()) +
                        ". Skipping patch.");
                continue;
            }
            readPatchTypeOptions(patchType.toPatchType(), patch);
            var patchView = patch.createMapView();
            bpmFile.patches().computeIfAbsent(patchType, _ -> new HashSet<>());
            var pixelList = bpmFile.patches().get(patchType);
            var pSecs = patchView.get(PIXEL_KEY);
            for (var pSec : pSecs) {
                pixelList.add(new PixelPatcher.Pixel(pSec.getParameter(INT_X_PARAMETER), pSec.getParameter(INT_Y_PARAMETER)));
            }
        }

        return bpmFile;
    }

    @Override
    public Section write(Void parameter, BpmFile obj) {
        var s = new Section("root", true);

        s.addSubsection(codec.write(BpmHeader.class, obj.header()));

        obj.patches().forEach((patchType, pixelList) -> {
            var patchSection = Section.createSection(PATCH_TYPE_KEY, PATCH_TYPE_PARAM, patchType);
            addPatchTypeOptions(patchType.toPatchType(), patchSection);
            for (var pixel : pixelList) {
                patchSection.addSubsection(Section.createSection(PIXEL_KEY, INT_X_PARAMETER, pixel.x(), INT_Y_PARAMETER, pixel.y()));
            }
            s.addSubsection(patchSection);
        });

        return s;
    }

    private void addPatchTypeOptions(PixelPatcher.PatchType patchType, Section patchSection) {
        switch (patchType) {
            case PixelPatcher.PatchType.AverageFill _ -> {
                patchSection.addSubsection(
                        Section.createSection("radius",
                                INT_X_PARAMETER, PixelPatcher.PatchType.AverageFill.X_RADIUS.get(),
                                INT_Y_PARAMETER, PixelPatcher.PatchType.AverageFill.Y_RADIUS.get())
                );
            }
            case PixelPatcher.PatchType.ConstantValue _ -> {
                patchSection.addSubsection(
                        Section.createSection("constant",
                                DOUBLE_PARAMETER, PixelPatcher.PatchType.ConstantValue.VALUE.get())
                );
            }
            case PixelPatcher.PatchType.FitGaussian _ -> {
                patchSection.addSubsection(
                        Section.createSection("minCount", INTEGER_PARAMETER, PixelPatcher.PatchType.FitGaussian.MIN_COUNT.get())
                );
                patchSection.addSubsection(
                        Section.createSection("maxIter", INTEGER_PARAMETER, PixelPatcher.PatchType.FitGaussian.MAX_ITER.get())
                );
                patchSection.addSubsection(
                        Section.createSection("relErr", DOUBLE_PARAMETER, PixelPatcher.PatchType.FitGaussian.REL_ERR.get())
                );
                patchSection.addSubsection(
                        Section.createSection("absErr", DOUBLE_PARAMETER, PixelPatcher.PatchType.FitGaussian.ABS_ERR.get())
                );
            }
            case PixelPatcher.PatchType.FitMoffat _ -> {
                patchSection.addSubsection(
                        Section.createSection("minCount", INTEGER_PARAMETER, PixelPatcher.PatchType.FitGaussian.MIN_COUNT.get())
                );
                patchSection.addSubsection(
                        Section.createSection("maxIter", INTEGER_PARAMETER, PixelPatcher.PatchType.FitMoffat.MAX_ITER.get())
                );
                patchSection.addSubsection(
                        Section.createSection("relErr", DOUBLE_PARAMETER, PixelPatcher.PatchType.FitMoffat.REL_ERR.get())
                );
                patchSection.addSubsection(
                        Section.createSection("absErr", DOUBLE_PARAMETER, PixelPatcher.PatchType.FitMoffat.ABS_ERR.get())
                );
            }
            case PixelPatcher.PatchType.FloodFill _ -> {
                patchSection.addSubsection(
                        Section.createSection("median", BOOLEAN_PARAMETER, PixelPatcher.PatchType.FloodFill.USE_MEDIAN.get())
                );
            }
            case PixelPatcher.PatchType.MedianFill _ -> {
                patchSection.addSubsection(
                        Section.createSection("radius",
                                INT_X_PARAMETER, PixelPatcher.PatchType.AverageFill.X_RADIUS.get(),
                                INT_Y_PARAMETER, PixelPatcher.PatchType.AverageFill.Y_RADIUS.get())
                );
            }
            case PixelPatcher.PatchType.NearestNeighbor _ -> {
                patchSection.addSubsection(
                        Section.createSection("mergeType", MERGE_TYPE_PARAMETER, PixelPatcher.PatchType.NearestNeighbor.MERGE_TYPE.get())
                );
            }
            case PixelPatcher.PatchType.FitPlane _ -> {
            }
            case PixelPatcher.PatchType.PassThrough _ -> {
            }
        }
    }

    private void readPatchTypeOptions(PixelPatcher.PatchType patchType, Section patchSection) {
        var view = patchSection.createMapView();
        switch (patchType) {
            case PixelPatcher.PatchType.AverageFill _ -> {
                var radius = getUniqueSection(view, "radius", false);
                if (radius != null) {
                    var x = radius.getParameter(INT_X_PARAMETER);
                    var y = radius.getParameter(INT_Y_PARAMETER);
                    PixelPatcher.PatchType.AverageFill.X_RADIUS.set(x);
                    PixelPatcher.PatchType.AverageFill.Y_RADIUS.set(y);
                }
            }
            case PixelPatcher.PatchType.ConstantValue _ -> {
                var cons = getUniqueSection(view, "constant", false);
                if (cons != null) {
                    PixelPatcher.PatchType.ConstantValue.VALUE.set(cons.getParameter(DOUBLE_PARAMETER));
                }
            }
            case PixelPatcher.PatchType.FitGaussian _ -> {
                var maxIter = getUniqueSection(view, "maxIter", false);
                if (maxIter != null) {
                    PixelPatcher.PatchType.FitGaussian.MAX_ITER.set(maxIter.getParameter(INTEGER_PARAMETER));
                }
                var minCount = getUniqueSection(view, "minCount", false);
                if (minCount != null) {
                    PixelPatcher.PatchType.FitGaussian.MIN_COUNT.set(minCount.getParameter(INTEGER_PARAMETER));
                }
                var relErr = getUniqueSection(view, "relErr", false);
                if (relErr != null) {
                    PixelPatcher.PatchType.FitGaussian.REL_ERR.set(relErr.getParameter(DOUBLE_PARAMETER));
                }
                var absErr = getUniqueSection(view, "absErr", false);
                if (absErr != null) {
                    PixelPatcher.PatchType.FitGaussian.ABS_ERR.set(absErr.getParameter(DOUBLE_PARAMETER));
                }
            }
            case PixelPatcher.PatchType.FitMoffat _ -> {
                var maxIter = getUniqueSection(view, "maxIter", false);
                if (maxIter != null) {
                    PixelPatcher.PatchType.FitMoffat.MAX_ITER.set(maxIter.getParameter(INTEGER_PARAMETER));
                }
                var minCount = getUniqueSection(view, "minCount", false);
                if (minCount != null) {
                    PixelPatcher.PatchType.FitMoffat.MIN_COUNT.set(minCount.getParameter(INTEGER_PARAMETER));
                }
                var relErr = getUniqueSection(view, "relErr", false);
                if (relErr != null) {
                    PixelPatcher.PatchType.FitMoffat.REL_ERR.set(relErr.getParameter(DOUBLE_PARAMETER));
                }
                var absErr = getUniqueSection(view, "absErr", false);
                if (absErr != null) {
                    PixelPatcher.PatchType.FitMoffat.ABS_ERR.set(absErr.getParameter(DOUBLE_PARAMETER));
                }
            }
            case PixelPatcher.PatchType.FloodFill _ -> {
                var med = getUniqueSection(view, "median", false);
                if (med != null) {
                    PixelPatcher.PatchType.FloodFill.USE_MEDIAN.set(med.getParameter(BOOLEAN_PARAMETER));
                }
            }
            case PixelPatcher.PatchType.MedianFill _ -> {
                var radius = getUniqueSection(view, "radius", false);
                if (radius != null) {
                    var x = radius.getParameter(INT_X_PARAMETER);
                    var y = radius.getParameter(INT_Y_PARAMETER);
                    PixelPatcher.PatchType.MedianFill.X_RADIUS.set(x);
                    PixelPatcher.PatchType.MedianFill.Y_RADIUS.set(y);
                }
            }
            case PixelPatcher.PatchType.NearestNeighbor _ -> {
                var type = getUniqueSection(view, "mergeType", false);
                if (type != null) {
                    PixelPatcher.PatchType.NearestNeighbor.MERGE_TYPE.set(type.getParameter(MERGE_TYPE_PARAMETER));
                }
            }
            case PixelPatcher.PatchType.FitPlane _ -> {
            }
            case PixelPatcher.PatchType.PassThrough _ -> {
            }
        }
    }
}
