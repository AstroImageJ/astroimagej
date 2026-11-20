package astroj.astrometry.annotations;

import astroj.AstrometrySetup;
import nom.tam.fits.BinaryTableHDU;
import nom.tam.fits.Fits;
import nom.tam.fits.FitsException;

import java.io.IOException;
import java.lang.reflect.Array;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.Pattern;

public final class AnnotationCatalog {
    private static final int MAX_BRIGHT = 0; // 0 = no cap (plotann --nbright)
    private static final boolean HIP_LABEL = true;

    public static List<Entry> load() throws IOException, FitsException {
        var out = new ArrayList<Entry>();
        var aliases = new HashMap<String, List<String>>();

        var uzc = findCatalog("uzc2000.fits");
        if (uzc.isPresent()) {
            try (var f = new Fits(uzc.get().toFile())) {
                var t = firstTable(f);
                var ra = colAsDoubles(t, "ra");
                var dec = colAsDoubles(t, "dec");
                var name = colAsStrings(t, "zname");
                for (int i = 0; i < ra.length; i++) {
                    out.add(new Entry("uzc", List.of("UZC " + name[i].trim()), ra[i], dec[i], 0));
                }
            }
        }

        var hipcat = findCatalog("hip.fits");
        if (hipcat.isPresent() && HIP_LABEL) {
            try (var f = new Fits(hipcat.get().toFile())) {
                var t = firstTable(f);
                var ra = colAsDoubles(t, "ra");
                var dec = colAsDoubles(t, "dec");
                var vmags = colAsDoubles(t, "vmag");
                var hips = colAsInts(t, "hip");
                for (int i = 0; i < ra.length; i++) {
                    out.add(new Entry("hip", List.of("HIP %d (%.1f)".formatted(hips[i], vmags[i])), ra[i], dec[i], 0));
                }
            }
        }

        var abell = findCatalog("abell-all.fits");
        if (abell.isPresent()) {
            try (var f = new Fits(abell.get().toFile())) {
                var t = firstTable(f);
                var ra = colAsDoubles(t, "ra");
                var dec = colAsDoubles(t, "dec");
                var aco = colAsInts(t, "aco");
                for (int i = 0; i < ra.length; i++) {
                    out.add(new Entry("abell", List.of("Abell " + aco[i]), ra[i], dec[i], 0));
                }
            }
        }

        var namesPath = findCatalog("openngc-names.fits");
        if (namesPath.isPresent()) {
            try (var f = new Fits(namesPath.get().toFile())) {
                var t = firstTable(f);
                var name = colAsStrings(t, "Name");
                var obj = colAsStrings(t, "Object");
                for (int i = 0; i < name.length; i++) {
                    var n = name[i].trim();
                    if (n.isEmpty()) {
                        continue;
                    }
                    var m = Pattern.compile("(\\d+)").matcher(n);
                    if (!m.find()) {
                        continue;
                    }
                    var key = (!n.startsWith("I")) + ":" + Integer.parseInt(m.group(1));
                    aliases.computeIfAbsent(key, _ -> new ArrayList<>()).add(obj[i].trim());
                }
            }
        }

        for (String[] spec : new String[][]{{"NGC", "openngc-ngc.fits", "ngcnum"}, {"IC", "openngc-ic.fits", "icnum"}}) {
            var path = findCatalog(spec[1]);
            if (path.isEmpty()) {
                continue;
            }
            var isNgc = spec[0].equals("NGC");
            try (var f = new Fits(path.get().toFile())) {
                var t = firstTable(f);
                var hasRadius = hasCol(t, "radius");
                var ra = colAsDoubles(t, "ra");
                var dec = colAsDoubles(t, "dec");
                var num = colAsDoubles(t, spec[2]);
                var rad = hasRadius ? colAsDoubles(t, "radius") : new double[0];
                for (int i = 0; i < ra.length; i++) {
                    var names = new ArrayList<String>();
                    names.add(spec[0] + " " + (int) num[i]);
                    var more = aliases.get(isNgc + ":" + (int) num[i]);
                    if (more != null) {
                        names.addAll(more);
                    }
                    out.add(new Entry(spec[0].toLowerCase(), names, ra[i], dec[i], hasRadius ? rad[i] : 0));
                }
            }
        }

        var brightPath = findCatalog("brightstars.fits");
        if (brightPath.isPresent()) {
            try (var f = new Fits(brightPath.get().toFile())) {
                var t = firstTable(f);
                var ra = colAsDoubles(t, "ra");
                var dec = colAsDoubles(t, "dec");
                var vmag = colAsDoubles(t, "vmag");
                var n1 = colAsStrings(t, "name1");
                var n2 = colAsStrings(t, "name2");
                var order = new Integer[ra.length];
                for (int i = 0; i < order.length; i++) {
                    order[i] = i;
                }
                Arrays.sort(order, Comparator.comparingDouble(i -> vmag[i]));
                int kept = 0;
                for (int i : order) {
                    var names = new ArrayList<String>();
                    if (!n1[i].trim().isEmpty()) {
                        names.add(n1[i].trim());
                    }
                    if (!n2[i].trim().isEmpty()) {
                        names.add(n2[i].trim());
                    }
                    if (names.isEmpty()) {
                        continue;
                    }
                    out.add(new Entry("bright", names, ra[i], dec[i], 0));
                    if (MAX_BRIGHT > 0 && ++kept >= MAX_BRIGHT) {
                        break;
                    }
                }
            }
        }

        return out;
    }

    /// Finds a catalog file
    public static Optional<Path> findCatalog(String name) throws IOException {
        var root = AstrometrySetup.ASTROMETRY_NET_INDEX_MANAGER.catalogDirectory();
        try (var s = Files.list(root)) {
            return s.filter(p -> p.getFileName().toString().equals(name)).findFirst();
        }
    }

    private static BinaryTableHDU firstTable(Fits f) throws FitsException, IOException {
        f.read();
        for (int i = 0; i < f.getNumberOfHDUs(); i++) {
            if (f.getHDU(i) instanceof BinaryTableHDU t) {
                return t;
            }
        }
        throw new FitsException("No binary table found");
    }

    private static boolean hasCol(BinaryTableHDU t, String colName) {
        return t.findColumn(colName) >= 0;
    }

    private static double[] colAsDoubles(BinaryTableHDU t, String name) {
        var col = t.getColumn(name);
        int n = Array.getLength(col);
        var d = new double[n];
        for (int i = 0; i < n; i++) {
            Object v = Array.get(col, i);
            if (v != null && v.getClass().isArray()) {
                v = Array.get(v, 0);
            }
            if (v != null) {
                d[i] = ((Number) v).doubleValue();
            }
        }
        return d;
    }

    private static int[] colAsInts(BinaryTableHDU t, String name) {
        var col = t.getColumn(name);
        int n = Array.getLength(col);
        var d = new int[n];
        for (int i = 0; i < n; i++) {
            Object v = Array.get(col, i);
            if (v != null && v.getClass().isArray()) {
                v = Array.get(v, 0);
            }
            if (v != null) {
                d[i] = ((Number) v).intValue();
            }
        }
        return d;
    }

    private static String[] colAsStrings(BinaryTableHDU t, String name) {
        var col = t.getColumn(name);
        var n = Array.getLength(col);
        var s = new String[n];
        for (int i = 0; i < n; i++) {
            s[i] = String.valueOf(Array.get(col, i));
        }
        return s;
    }

    public record Entry(String type, List<String> names, double raDeg, double decDeg, double radiusDeg) {
    }
}