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
            for (var pixel : pixelList) {
                patchSection.addSubsection(Section.createSection(PIXEL_KEY, INT_X_PARAMETER, pixel.x(), INT_Y_PARAMETER, pixel.y()));
            }
            s.addSubsection(patchSection);
        });

        return s;
    }
}
