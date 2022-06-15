package ij.astro.util;

import ij.IJ;

public class TypeHelper {
    public static boolean isInstance(Object o, String className) {
        Class<?> clazz = null;
        try {
            clazz = Class.forName(className, true, IJ.getClassLoader());
        } catch (ClassNotFoundException e) {
            return false;
        }
        return clazz.isInstance(o);
    }
}
