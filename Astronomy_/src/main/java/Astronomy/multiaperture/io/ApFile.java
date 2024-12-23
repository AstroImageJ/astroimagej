package Astronomy.multiaperture.io;

import Astronomy.MultiAperture_;
import Astronomy.multiaperture.io.transformers.ApertureFileTransformer;
import Astronomy.multiaperture.io.transformers.Header;
import astroj.Aperture;
import ij.Prefs;

import java.util.List;
import java.util.Properties;

public record ApFile(Header header, List<? extends Aperture> apertures, Properties prefs) {
    public ApFile(List<? extends Aperture> apertures) {
        this(apertures, getMaPrefs());
    }

    public ApFile(List<? extends Aperture> apertures, Properties prefs) {
        this(new Header(ApertureFileTransformer.maxSupportedMajor, ApertureFileTransformer.maxSupportedMinor), apertures, prefs);
    }

    public <T extends Aperture> List<T> getAperturesOfType(Class<T> clazz, Aperture.ApertureShape apertureShape) {
        return apertures().stream()
                .filter(aperture -> aperture.getApertureShape() == apertureShape)
                .map(aperture -> (T) aperture)
                .toList();
    }

    private static Properties getMaPrefs() {
        var p = new Properties();
        for (String apertureKey : MultiAperture_.getApertureKeys()) {
            if (Prefs.containsKey(apertureKey.substring(1)) && !MultiAperture_.getCircularApertureKeys().contains(apertureKey)) {
                p.put(apertureKey, Prefs.getString(apertureKey));
            }
        }

        return p;
    }
}
