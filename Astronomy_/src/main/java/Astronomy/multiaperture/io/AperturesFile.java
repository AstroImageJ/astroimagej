package Astronomy.multiaperture.io;

import Astronomy.MultiAperture_;
import astroj.FreeformPixelApertureRoi;
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
        var apertures = new ArrayList<FreeformPixelApertureRoi>();
        var prefs = new Properties();
        if (contents.startsWith("AIJ APERTURES FILE")) {
            var ap = new AtomicReference<FreeformPixelApertureRoi>();
            AtomicBoolean inPrefsSection = new AtomicBoolean(false);
            var hasRBack1 = new AtomicBoolean();
            var hasRBack2 = new AtomicBoolean();
            contents.lines().skip(1).forEachOrdered(line -> {
                if (line.startsWith("ap\tcustom_pixel")) {
                    var old = ap.getAndSet(new FreeformPixelApertureRoi());
                    if (old != null) {
                        apertures.add(old);
                    }
                    hasRBack1.set(false);
                    hasRBack2.set(false);
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

                    if (line.startsWith("centroid")) {
                        var tSep = line.indexOf("\t");
                        ap.get().setIsCentroid(Boolean.parseBoolean(line.substring(tSep+1)));
                    }

                    if (line.startsWith("rBack1")) {
                        var r1Sep = line.indexOf("\t");
                        if (r1Sep < 0) {
                            throw new IllegalStateException("Missing r1Sep! " + line);
                        }

                        var r1 = Double.parseDouble(line.substring(r1Sep+1));

                        hasRBack1.set(true);
                        ap.get().setBack1(r1);
                    }

                    if (line.startsWith("rBack2")) {
                        var r2Sep = line.indexOf("\t");
                        if (r2Sep < 0) {
                            throw new IllegalStateException("Missing r2Sep! " + line);
                        }

                        var r2 = Double.parseDouble(line.substring(r2Sep+1));

                        hasRBack2.set(true);
                        ap.get().setBack2(r2);
                    }

                    if (hasRBack1.get() && hasRBack2.get()) {
                        ap.get().setHasAnnulus(true);
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

    public record Data(List<FreeformPixelApertureRoi> apertureRois, Properties prefs) {
        @Override
        public String toString() {
            var setting = new StringBuilder("AIJ APERTURES FILE");

            setting.append('\n').append('\t').append("majorVersion").append('\t').append(2);
            setting.append('\n').append('\t').append("minorVersion").append('\t').append(0);

            for (FreeformPixelApertureRoi aperture : apertureRois()) {
                setting.append("\nap\tcustom_pixel");
                setting.append('\n');
                setting.append('\t').append("isComp").append('\t').append(aperture.isComparisonStar());

                if (aperture.getIsCentroid()) {
                    setting.append('\n').append('\t');
                    setting.append("centroid").append('\t').append(aperture.getIsCentroid());
                }

                for (FreeformPixelApertureRoi.Pixel pixel : aperture.iterable()) {
                    setting.append('\n').append('\t');
                    setting.append("px\t").append(pixel.x()).append('\t').append(pixel.y()).append('\t')
                            .append(pixel.isBackground() ? "background" : "source");
                }

                if (aperture.hasAnnulus()) {
                    setting.append('\n').append('\t');
                    setting.append("rBack1").append('\t').append(aperture.getBack1());
                    setting.append('\n').append('\t');
                    setting.append("rBack2").append('\t').append(aperture.getBack2());
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
