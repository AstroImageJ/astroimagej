package ij.util;

import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.IntStream;

/**
 * Utility class for boxing n-dimensional arrays, primarily intended for use in dynamic handling of nom.tam.fits output.
 */
//todo document
//todo move to nom.tam.fits
//todo check accuracy of output
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
            return IntStream.range(0, array2.length)
                    .mapToObj(i -> ((float[]) array2)[i]).toArray(Float[]::new);
        }
        if (array instanceof byte[] array2) {
            return IntStream.range(0, array2.length)
                    .mapToObj(i -> ((byte[]) array2)[i]).toArray(Byte[]::new);
        }
        if (array instanceof double[] array2) {
            return Arrays.stream(array2, 0, array2.length).boxed().toArray(Double[]::new);
        }
        if (array instanceof int[] array2) {
            return Arrays.stream(array2, 0, array2.length).boxed().toArray(Integer[]::new);
        }
        if (array instanceof short[] array2) {
            return IntStream.range(0, array2.length)
                    .mapToObj(i -> ((short[]) array2)[i]).toArray(Short[]::new);
        }
        if (array instanceof long[] array2) {
            return Arrays.stream(array2, 0, array2.length).boxed().toArray(Long[]::new);
        }
        if (array instanceof boolean[] array2) {
            return IntStream.range(0, array2.length)
                    .mapToObj(i -> ((boolean[]) array2)[i]).toArray(Boolean[]::new);
        }
        if (array instanceof char[] array2) {
            return IntStream.range(0, array2.length)
                    .mapToObj(i -> ((char[]) array2)[i]).toArray(Character[]::new);
        }
        return null;
    }
}
