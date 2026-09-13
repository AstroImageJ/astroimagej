package Astronomy.multiaperture.io.transformers;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import Astronomy.multiaperture.io.ApFile;
import astroj.Aperture;
import ij.astro.io.aij.AijFileCodec;
import ij.astro.io.aij.Section;
import ij.astro.io.aij.Transformer;
import ij.astro.logging.AIJLogger;

public class ApertureFileTransformer extends Transformer<ApFile, Void> {
    public static final int maxSupportedMajor = 2;
    public static final int maxSupportedMinor = 2;

    public ApertureFileTransformer(AijFileCodec codec) {
        super(codec);
    }

    @Override
    public ApFile load(Void params, Section section) {
        var view = section.createMapView();

        if (view.contains(ApertureHeader.HEADER)) {
            var header = codec.read(ApertureHeader.class, getUniqueSection(view, ApertureHeader.HEADER));

            if (header.getMajorVersion() > maxSupportedMajor || header.getMinorVersion() > maxSupportedMinor) {
                AIJLogger.log("""
                        This apertures file contains a newer format (%s) than what is currently supported (%s).
                        Attempting to read anyway...
                        """.formatted(header.getMajorVersion() + "." + header.getMinorVersion(),
                        maxSupportedMajor + "." + maxSupportedMinor));
            }

            var apertures = new ArrayList<Aperture>();
            for (Section ap : view.get("ap")) {
                apertures.add(codec.read(Aperture.class, ap));
            }

            var maSettingsSec = getUniqueSection(view, "multiapertureSettings", false);

            Properties prefs = maSettingsSec != null ? codec.read(Properties.class, maSettingsSec,
                    "multiapertureSettings") : new Properties();

            return new ApFile(header, apertures, prefs);
        } else { // Legacy Apertures File
            var prefs = codec.read(Properties.class, section, "");
            return new ApFile(new ApertureHeader(), List.of(), prefs, true);
        }
    }

    @Override
    public Section write(Void params, ApFile apFile) {
        var s = new Section("root", true);

        s.addSubsection(codec.write(ApertureHeader.class, apFile.header()));

        for (Aperture aperture : apFile.apertures()) {
            s.addSubsection(codec.write(Aperture.class, aperture));
        }

        if (apFile.prefs() != null) {
            s.addSubsection(codec.write(Properties.class, apFile.prefs(), "multiapertureSettings"));
        }

        return s;
    }
}
