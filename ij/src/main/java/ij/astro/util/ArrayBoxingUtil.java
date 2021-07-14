package ij.astro.util;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Utility class for boxing n-dimensional arrays, primarily intended for use in dynamic handling of nom.tam.fits output.
 */
//todo document
//todo move to nom.tam.fits
//todo add boxed -> primitive
public class ArrayBoxingUtil {
    private static final Map<Class<?>, Class<?>> typeMapping = new HashMap<>();
    static {
        typeMapping.put(Boolean.TYPE, Boolean.class);
        typeMapping.put(Byte.TYPE, Byte.class);
        typeMapping.put(Character.TYPE, Character.class);
        typeMapping.put(Short.TYPE, Short.class);
        typeMapping.put(Integer.TYPE, Integer.class);
        typeMapping.put(Long.TYPE, Long.class);
        typeMapping.put(Double.TYPE, Double.class);
        typeMapping.put(Float.TYPE, Float.class);
    }

    // Example usage
    public static void main(String[] args) {
        Integer[][][] test = (Integer[][][]) boxArray(new int[][][] {{{1}, {1, 2, 3}}, {}});
    }

    /**
     * Generates the boxed n-dimensional array type from a primitive array's type.
     *
     */
    public static Class<?> toWrapperArrayType(final Class<?> primitiveArrayType) {
        int levels = 0;

        Class<?> component = primitiveArrayType.getComponentType();
        for (; component.isArray(); levels++) {
            component = component.getComponentType();
        }

        Class<?> boxedType = typeMapping.getOrDefault(component, component);
        for (int i = 0; i < levels; i++)
            boxedType = boxedType.arrayType();

        return boxedType;
    }

    /**
     * Converts an n-dimensional primitive array to its boxed counterpart based on the provided boxed type
     *
     */
    public static Object boxArray(final Object arr, final Class<?> boxedType) {
        Object b = box1DArray(arr);
        if (b != null) {
            return b;
        }
        if (!(arr instanceof Object[] objectArr))
            throw new IllegalArgumentException("arr was not an array during boxing");

        int length = Array.getLength(arr);
        Object[] newArr = (Object[]) Array.newInstance(boxedType, length);

        for (int i = 0; i < length; i++)
            newArr[i] = boxArray(objectArr[i], boxedType.getComponentType());

        return newArr;
    }

    /**
     * Converts an n-dimensional primitive array to its boxed counterpart.
     *
     */
    public static Object boxArray(final Object arr) {
        return boxArray(arr, toWrapperArrayType(arr.getClass()));
    }

    /**
     * Converts a 1-dimensional primitive array to an array of its boxed counterpart.
     *
     * @param array primitive array to convert to a boxed format
     * @return the array boxed
     */
    public static Object box1DArray(final Object array) {
        if (array instanceof float[] array2) {
            List<Float> list = new ArrayList<>();
            for (int i = 0; i < array2.length; i++) {
                Float aFloat = ((float[]) array2)[i];
                list.add(aFloat);
            }
            return list.toArray(new Float[0]);
        }
        if (array instanceof byte[] array2) {
            List<Byte> list = new ArrayList<>();
            for (int i = 0; i < array2.length; i++) {
                Byte aByte = ((byte[]) array2)[i];
                list.add(aByte);
            }
            return list.toArray(new Byte[0]);
        }
        if (array instanceof double[] array2) {
            List<Double> list = new ArrayList<>();
            int bound = array2.length;
            for (int i = 0; i < bound; i++) {
                Double aDouble = array2[i];
                list.add(aDouble);
            }
            return list.toArray(new Double[0]);
        }
        if (array instanceof int[] array2) {
            List<Integer> list = new ArrayList<>();
            int bound = array2.length;
            for (int j = 0; j < bound; j++) {
                Integer integer = array2[j];
                list.add(integer);
            }
            return list.toArray(new Integer[0]);
        }
        if (array instanceof short[] array2) {
            List<Short> list = new ArrayList<>();
            for (int i = 0; i < array2.length; i++) {
                Short aShort = ((short[]) array2)[i];
                list.add(aShort);
            }
            return list.toArray(new Short[0]);
        }
        if (array instanceof long[] array2) {
            List<Long> list = new ArrayList<>();
            int bound = array2.length;
            for (int i = 0; i < bound; i++) {
                Long aLong = array2[i];
                list.add(aLong);
            }
            return list.toArray(new Long[0]);
        }
        if (array instanceof boolean[] array2) {
            List<Boolean> list = new ArrayList<>();
            for (int i = 0; i < array2.length; i++) {
                Boolean aBoolean = ((boolean[]) array2)[i];
                list.add(aBoolean);
            }
            return list.toArray(new Boolean[0]);
        }
        if (array instanceof char[] array2) {
            List<Character> list = new ArrayList<>();
            for (int i = 0; i < array2.length; i++) {
                Character character = ((char[]) array2)[i];
                list.add(character);
            }
            return list.toArray(new Character[0]);
        }
        // Input is already a 1D boxed array
        if (array instanceof Object[] array2 && !array2.getClass().getComponentType().isArray()) {
            return array;
        }
        return null;
    }
}
