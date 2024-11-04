package ij.astro.util;

public class SwingConstantUtil {
    private SwingConstantUtil() {}

    public static boolean hasModifier(int modifiers, int... masks) {
        var mask = 0;
        for (int i : masks) {
            mask |= i;
        }

        return (modifiers & mask) == mask;
    }
}
