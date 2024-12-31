package Astronomy.multiaperture.io.transformers;

import Astronomy.multiaperture.io.ApFile;
import Astronomy.multiaperture.io.Section;
import Astronomy.multiaperture.io.Transformer;
import Astronomy.multiaperture.io.Transformers;
import astroj.Aperture;
import ij.astro.logging.AIJLogger;
import ij.astro.types.MultiMap;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class ApertureFileTransformer implements Transformer<ApFile, Void> {
    public static final int maxSupportedMajor = 2;
    public static final int maxSupportedMinor = 1;

    @Override
    public ApFile load(Void params, Section section) {
        var view = section.createMapView();

        if (view.contains(Header.HEADER)) {
            var header = Transformers.read(Header.class, getUniqueSection(view, Header.HEADER));

            if (header.getMajorVersion() > maxSupportedMajor || header.getMinorVersion() > maxSupportedMinor) {
                AIJLogger.log("""
                        This apertures file contains a newer format (%s) than what is currently supported (%s).
                        Attempting to read anyway...
                        """.formatted(header.getMajorVersion() + "." + header.getMinorVersion(),
                        maxSupportedMajor + "." + maxSupportedMinor));
            }

            var apertures = new ArrayList<Aperture>();
            for (Section ap : view.get("ap")) {
                apertures.add(Transformers.read(Aperture.class, ap));
            }

            var maSettingsSec = getUniqueSection(view, "multiapertureSettings", false);

            Properties prefs = maSettingsSec != null ? Transformers.read(Properties.class, maSettingsSec) : new Properties();

            return new ApFile(header, apertures, prefs);
        } else { // Legacy Apertures File
            var prefs = Transformers.read(Properties.class, section);
            return new ApFile(new Header(), List.of(), prefs, true);
        }
    }

    @Override
    public Section write(Void params, ApFile apFile) {
        var s = new Section("root", true);

        s.addSubsection(Transformers.write(Header.class, apFile.header()));

        for (Aperture aperture : apFile.apertures()) {
            s.addSubsection(Transformers.write(Aperture.class, aperture));
        }

        if (apFile.prefs() != null) {
            s.addSubsection(Transformers.write(Properties.class, apFile.prefs()));
        }

        return s;
    }

    private Section getUniqueSection(MultiMap<String, Section> view, String name) {
        return getUniqueSection(view, name, true);
    }

    private Section getUniqueSection(MultiMap<String, Section> view, String name, boolean required) {
        var l = view.get(name);
        var c = l == null ? 0 : l.size();

        if ((required && c != 1) || (!required && c > 1)) {
            throw new IllegalStateException("Aperture File has %s %s(s)!".formatted(c, name));
        }

        return c == 0 ? null : l.get(0);
    }

    private List<Section> getRequiredSection(MultiMap<String, Section> view, String name) {
        if (!view.contains(name)) {
            throw new IllegalStateException("Aperture File missing required section: " + name);
        }

        return view.get(name);
    }

}
