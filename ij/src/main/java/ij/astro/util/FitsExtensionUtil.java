package ij.astro.util;

import java.util.EnumSet;
import java.util.regex.Pattern;

public class FitsExtensionUtil {
    public static final Pattern UNCOMPRESSED_FITS_EXTENSION =
            Pattern.compile(".*(\\.[Ff][Ii]?[Tt][Ss]?$)");
    public static final Pattern COMPRESSED_FITS_EXTENSION =
            Pattern.compile("(?<FILENAME>.*)(\\.[Ff][Ii]?[Tt][Ss]?(?<FPACK>\\.[Ff][Zz])?(?<GZIP>\\.[Gg][Zz])?$)");

    public static boolean isFitsFile(String path) {
        return isFitsFile(path, true);
    }

    public static boolean isFitsFile(String path, boolean allowCompressed) {
        return allowCompressed ? COMPRESSED_FITS_EXTENSION.matcher(path).find() :
                UNCOMPRESSED_FITS_EXTENSION.matcher(path).find();
    }

    /**
     * @return the file name or path without the FITS extension, or the unmodified {@param file}.
     */
    public static String fileNameWithoutExt(String file) {
        var matcher = COMPRESSED_FITS_EXTENSION.matcher(file);

        if (matcher.matches()) {
            return matcher.group("FILENAME");
        }

        return file;
    }

    public static String makeFitsSave(String file, boolean fpack, boolean gzip) {
        return makeFitsSave(file, fpack ? CompressionMode.FPACK : null, gzip ? CompressionMode.GZIP : null);
    }

    public static String makeFitsSave(String file, CompressionMode... modes) {
        EnumSet<CompressionMode> modeSet = EnumSet.noneOf(CompressionMode.class);;
        if (modes != null && modes.length > 0) {
            for (CompressionMode mode : modes) {
                if (mode == null) continue;
                modeSet.add(mode);
            }
        }

        var s = fileNameWithoutExt(file);

        return s + ".fits" + (modeSet.contains(CompressionMode.FPACK) ? ".fz" : "") +
                (modeSet.contains(CompressionMode.GZIP) ? ".gz" : "");
    }

    public static EnumSet<CompressionMode> compressionModes(String file) {
        var result = COMPRESSED_FITS_EXTENSION.matcher(file);
        var o = EnumSet.noneOf(CompressionMode.class);

        if (result.matches()) {
            if (result.group("FPACK") != null) {
                o.add(CompressionMode.FPACK);
            }

            if (result.group("GZIP") != null) {
                o.add(CompressionMode.GZIP);
            }
        }

        return o;
    }

    public enum CompressionMode {
        FPACK,
        GZIP;
    }
}
