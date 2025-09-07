package Astronomy.updater;

import java.util.Comparator;
import java.util.List;

public record SemanticVersion(int[] core, String[] prerelease) implements Comparable<SemanticVersion> {
    public SemanticVersion(String version) {
        this(getCore(version), getPrerelease(version));
    }

    @Override
    public int compareTo(SemanticVersion o) {
        for (int i = 0; i < Math.max(this.core.length, o.core.length); i++) {
            var thisVal = i < this.core.length ? this.core[i] : 0;
            var otherVal = i < o.core.length ? o.core[i] : 0;

            if (thisVal != otherVal) {
                return Integer.compare(thisVal, otherVal);
            }
        }

        if (this.prerelease.length == 0 && o.prerelease.length > 0) {
            return 1;
        }

        if (this.prerelease.length > 0 && o.prerelease.length == 0) {
            return -1;
        }

        for (int i = 0; i < Math.max(this.prerelease.length, o.prerelease.length); i++) {
            if (i >= this.prerelease.length) {
                return -1;
            }

            if (i >= o.prerelease.length) {
                return 1;
            }

            var a = this.prerelease[i];
            var b = o.prerelease[i];

            var aNumeric = a.matches("\\d+");
            var bNumeric = b.matches("\\d+");

            if (aNumeric && bNumeric) {
                int aNum = Integer.parseInt(a);
                int bNum = Integer.parseInt(b);
                if (aNum != bNum) return Integer.compare(aNum, bNum);
            } else if (aNumeric) {
                return -1;
            } else if (bNumeric) {
                return 1;
            } else {
                int cmp = a.compareTo(b);
                if (cmp != 0) {
                    return cmp;
                }
            }
        }

        return 0;
    }

    @Override
    public String toString() {
        var sb = new StringBuilder();
        for (int i = 0; i < core.length; i++) {
            // Add leading 0 for AIJ daily builds
            if (i == 3) {
                sb.append('0');
            }

            sb.append(core[i]);
            if (i < core.length - 1) {
                sb.append('.');
            }
        }

        if (prerelease.length > 0) {
            sb.append('-');
        }

        for (int i = 0; i < prerelease.length; i++) {
            sb.append(prerelease[i]);
            if (i < prerelease.length - 1) {
                sb.append('.');
            }
        }

        return sb.toString();
    }

    private static int[] getCore(String s) {
        var start = s.indexOf('+');
        if (start != -1) s = s.substring(0, start);
        var end = s.indexOf('-');
        if (end == -1) end = s.length();

        var parts = s.substring(0, end).split("\\.");

        var core = new int[parts.length];
        for (int i = 0; i < parts.length; i++) {
            core[i] = Integer.parseInt(parts[i]);
        }

        return core;
    }

    private static String[] getPrerelease(String s) {
        var start = s.indexOf('+');
        if (start != -1) s = s.substring(0, start);
        start = s.indexOf('-');
        if (start == -1) return new String[0];

        return s.substring(start + 1).split("\\.");
    }

    public static void main(String[] args) {
        var list = List.of(
                new SemanticVersion("1.0.0-alpha"),
                new SemanticVersion("1.0.0-alpha.1"),
                new SemanticVersion("1.0.0-alpha.beta"),
                new SemanticVersion("1.0.0-beta"),
                new SemanticVersion("1.0.0-beta.2"),
                new SemanticVersion("1.0.0-beta.11"),
                new SemanticVersion("1.0.0-rc.1"),
                new SemanticVersion("1.0.0"),
                new SemanticVersion("1.0.0.01"),
                new SemanticVersion("1.0.1"),
                new SemanticVersion("1.0.1.01"),
                new SemanticVersion("1.1.0"),
                new SemanticVersion("2.0.0"),
                new SemanticVersion("1.0.1.02")
        );

        list.stream().sorted().forEachOrdered(System.out::println);

        System.out.println("Max:");
        list.stream().max(Comparator.naturalOrder()).ifPresent(System.out::println);
    }
}
