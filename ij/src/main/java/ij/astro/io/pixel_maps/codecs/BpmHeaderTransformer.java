package ij.astro.io.pixel_maps.codecs;

import ij.astro.io.aij.AijFileCodec;
import ij.astro.io.aij.Section;
import ij.astro.io.aij.Transformer;
import ij.astro.io.pixel_maps.BpmHeader;

public class BpmHeaderTransformer extends Transformer<BpmHeader, Void> {
    public static final String HEADER = "AIJ BAD-PIXEL-MAP";
    private static final Section.Parameter<Integer> VERSION_PARAMETER = new Section.Parameter<>("version", 0, Integer.TYPE);

    public BpmHeaderTransformer(AijFileCodec codec) {
        super(codec);
    }

    @Override
    public BpmHeader load(Void params, Section section) {
        var view = section.createMapView();

        if (!view.contains("majorVersion")) {
            throw new IllegalStateException("Header missing majorVersion");
        }

        if (!view.contains("minorVersion")) {
            throw new IllegalStateException("Header missing minorVersion");
        }

        var majorVersionSection = view.get("majorVersion");
        var minorVersionSection = view.get("minorVersion");

        if (majorVersionSection.size() != 1) {
            throw new IllegalStateException("Header has %s majorVersions!".formatted(majorVersionSection.size()));
        }

        if (minorVersionSection.size() != 1) {
            throw new IllegalStateException("Header has %s minorVersions!".formatted(minorVersionSection.size()));
        }

        var majorVersion = majorVersionSection.getFirst().getParameter(VERSION_PARAMETER);
        var minorVersion = minorVersionSection.getFirst().getParameter(VERSION_PARAMETER);

        return new BpmHeader(majorVersion, minorVersion);
    }

    @Override
    public Section write(Void params, BpmHeader obj) {
        var headerSection = new Section(HEADER);

        headerSection.addSubsection(Section.createSection("majorVersion", VERSION_PARAMETER, obj.majorVersion()));
        headerSection.addSubsection(Section.createSection("minorVersion", VERSION_PARAMETER, obj.minorVersion()));

        return headerSection;
    }
}
