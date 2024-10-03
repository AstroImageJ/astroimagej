package Astronomy.multiaperture.io;

import Astronomy.MultiAperture_;
import astroj.CustomPixelApertureRoi;
import ij.Prefs;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public class AperturesFile {

    public static Data read(Path path) {
        try {
            var s = Files.readString(Path.of("./test.apertures"));
            return read(s);
        } catch (IOException e) {
            return null;
        }
    }

    public static Data read(String contents) {
        var apertures = new ArrayList<CustomPixelApertureRoi>();
        var prefs = new Properties();
        if (contents.startsWith("AIJ APERTURES FILE")) {
            var ap = new AtomicReference<CustomPixelApertureRoi>();
            AtomicBoolean inPrefsSection = new AtomicBoolean(false);
            contents.lines().skip(1).forEachOrdered(line -> {
                if (line.startsWith("ap\tcustom_pixel")) {
                    var old = ap.getAndSet(new CustomPixelApertureRoi());
                    if (old != null) {
                        apertures.add(old);
                    }
                    return;
                }

                if (line.startsWith("multiapertureSettings")) {
                    inPrefsSection.set(true);
                    return;
                }

                if (line.startsWith("\t")) {
                    line = line.substring(1);
                }

                if (!inPrefsSection.get()) {
                    if (ap.get() == null) {
                        return;
                    }

                    if (line.startsWith("px")) {
                        var xSep = line.indexOf("\t");
                        if (xSep < 0) {
                            throw new IllegalStateException("Missing xSep! " + line);
                        }

                        var ySep = line.indexOf("\t", xSep+1);
                        if (ySep < 0) {
                            throw new IllegalStateException("Missing ySep! " + line);
                        }

                        var tSep = line.indexOf("\t", ySep+1);
                        if (tSep < 0) {
                            throw new IllegalStateException("Missing tSep! " + line);
                        }

                        var x = Integer.parseInt(line.substring(xSep+1, ySep));
                        var y = Integer.parseInt(line.substring(ySep+1, tSep));
                        ap.get().addPixel(x, y, "background".equals(line.substring(tSep+1)));
                    }

                    if (line.startsWith("isComp")) {
                        var tSep = line.indexOf("\t");
                        ap.get().setComparisonStar(Boolean.parseBoolean(line.substring(tSep+1)));
                    }
                } else {
                    var s = line.split("\t", 2);
                    if (s.length == 2) {
                        prefs.put(s[0], s[1]);
                    }
                }
            });

            if (ap.get() != null) {
                apertures.add(ap.get());
            }
        }

        if (apertures.isEmpty() && prefs.isEmpty()) {
            return null;
        }

        return new Data(apertures, prefs);
    }

    public record Data(List<CustomPixelApertureRoi> apertureRois, Properties prefs) {
        @Override
        public String toString() {
            var setting = new StringBuilder("AIJ APERTURES FILE");

            setting.append('\n').append('\t').append("majorVersion").append('\t').append(2);
            setting.append('\n').append('\t').append("minorVersion").append('\t').append(0);

            for (CustomPixelApertureRoi aperture : apertureRois()) {
                setting.append("\nap\tcustom_pixel");
                setting.append('\n');
                setting.append('\t').append("isComp").append('\t').append(aperture.isComparisonStar());

                for (CustomPixelApertureRoi.Pixel pixel : aperture.iterable()) {
                    setting.append('\n').append('\t');
                    setting.append("px\t").append(pixel.x()).append('\t').append(pixel.y()).append('\t')
                            .append(pixel.isBackground() ? "background" : "source");
                }
            }

            setting.append('\n');
            setting.append("multiapertureSettings");
            for (String apertureKey : MultiAperture_.getApertureKeys()) {
                if (Prefs.containsKey(apertureKey.substring(1)) && !MultiAperture_.getCircularApertureKeys().contains(apertureKey)) {
                    setting.append('\n').append('\t')
                            .append(apertureKey).append('\t').append(Prefs.getString(apertureKey));
                }
            }

            return setting.toString();
        }
    }
}
