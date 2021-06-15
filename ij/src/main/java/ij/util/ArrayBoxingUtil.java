package ij.util;

import ij.astro.AstroImageJ;

import java.util.*;
import java.util.function.Function;
import java.util.stream.*;

//todo document
//todo move to nom.tam.fits
//todo see if 2d or 3d or 4d or nd outputs are needed
//todo check accuracy of output
@AstroImageJ(reason = "Utility for working with nom.tam.fits")
public class ArrayBoxingUtil {
    List<TypeConverter<?>> typeMap = new ArrayList<>();
    Function<? extends Object[], Stream<?>> streamGen = Arrays::stream;

    public static Number[][][] convert2Boxed(Object o) {
        if (o instanceof short[][][]) {
            return Arrays.stream(((short[][][]) o)).map(a -> Arrays.stream(a).map(b -> Arrays.stream(box1DArray(b)).toArray(Number[]::new)).toArray(Number[][]::new)).toArray(Number[][][]::new);
        }
        if (o instanceof long[][][]) {
            return Arrays.stream(((long[][][]) o)).map(a -> Arrays.stream(a).map(b -> Arrays.stream(box1DArray(b)).toArray(Number[]::new)).toArray(Number[][]::new)).toArray(Number[][][]::new);
        }
        if (o instanceof double[][][]) {
            return Arrays.stream(((double[][][]) o)).map(a -> Arrays.stream(a).map(b -> Arrays.stream(box1DArray(b)).toArray(Number[]::new)).toArray(Number[][]::new)).toArray(Number[][][]::new);
        }
        if (o instanceof int[][][]) {
            return Arrays.stream(((int[][][]) o)).map(a -> Arrays.stream(a).map(b -> Arrays.stream(b).boxed().toArray(Integer[]::new)).toArray(Integer[][]::new)).toArray(Integer[][][]::new);
        }
        if (o instanceof float[][][]) {
            return Arrays.stream(((float[][][]) o)).map(a -> Arrays.stream(a).map(b -> Arrays.stream(box1DArray(b)).toArray(Number[]::new)).toArray(Number[][]::new)).toArray(Number[][][]::new);
        }
        if (o instanceof byte[][][]) {
            return Arrays.stream(((byte[][][]) o)).map(a -> Arrays.stream(a).map(b -> Arrays.stream(box1DArray(b)).toArray(Number[]::new)).toArray(Number[][]::new)).toArray(Number[][][]::new);
        }

        return null;
    }

    public static Number[] box1DArray(Object array) {
        if (array instanceof float[]) {
            float[] array2 = (float[])array;
            return IntStream.range(0, array2.length)
                    .mapToDouble(i -> array2[i]).boxed().toArray(Double[]::new);
        }
        if (array instanceof byte[]) {
            byte[] array2 = (byte[])array;
            return IntStream.range(0, array2.length)
                    .mapToObj(i -> ((byte[]) array2)[i]).toArray(Byte[]::new);
        }
        if (array instanceof double[]) {
            double[] array2 = (double[])array;
            return Arrays.stream(array2, 0, array2.length).boxed().toArray(Double[]::new);
        }
        if (array instanceof int[]) {
            int[] array2 = (int[])array;
            return Arrays.stream(array2, 0, array2.length).boxed().toArray(Integer[]::new);
        }
        if (array instanceof short[]) {
            short[] array2 = (short[])array;
            return IntStream.range(0, array2.length)
                    .mapToObj(i -> ((short[]) array2)[i]).toArray(Short[]::new);
        }
        if (array instanceof long[]) {
            long[] array2 = (long[])array;
            return Arrays.stream(array2, 0, array2.length).boxed().toArray(Long[]::new);
        }
        return null;
    }

    /*ArrayBoxingUtil() {
        typeMap.add(new TypeConverter<Integer>(Short.TYPE, short[][][].class, IntStream::of));
        typeMap.add(new TypeConverter<Integer>(Integer.TYPE, int[][][].class, IntStream::of));
        typeMap.add(new TypeConverter<Double>(Float.TYPE, float[][][].class, DoubleStream::of));
        typeMap.add(new TypeConverter<Double>(Double.TYPE, double[][][].class, DoubleStream::of));
        typeMap.add(new TypeConverter<Long>(Long.TYPE, long[][][].class, LongStream::of));
        typeMap.add(new TypeConverter<Integer>(Byte.TYPE, byte[][][].class, IntStream::of));
    }

    public Number[][][] convertToBoxed(Object data) {
        Class<?> baseType = data.getClass().getComponentType();
        while (baseType != null && baseType.isArray() && baseType.getComponentType() != null) {
            baseType = baseType.getComponentType();
        }

        for (TypeConverter<?> typeConverter : typeMap) {
            if (typeConverter.boxedClass.equals(baseType)) {
                streamGen.apply(data).map()
            }
        }
    }

    private BaseStream<?, ?> arrayStreamer(Object o, TypeConverter<?> tc) {
        if (o.getClass().isArray()) {
            return arrayStreamer()
        } else {
            return tc.genericStream(o);
        }
        return null;
    }*/

    /*private Number[] box1DimArray(Object o) {
        if (!o.getClass().isArray()) return null;

        typeMap.forEach((t, bt) -> {
            if (t.equals(o.getClass().getComponentType())) {
                Arrays.stream(((Object[]) o)).map(a -> bt.)
            }
        });
        //Arrays.stream(((int[][][]) o)).map(a -> Arrays.stream(a).map(b -> Arrays.stream(b).boxed().toArray(Integer[]::new)).toArray(Integer[][]::new)).toArray(Integer[][][]::new);
    }*/

    class TypeConverter<T extends Number> {
        final Class<T> boxedClass;
        final Class<?> nDimArrayClass;
        final Function<T, BaseStream<T, ?>> stream;

        public TypeConverter(Class<T> boxedClass, Class<?> nDimArrayClass,
                             Function<T, BaseStream<T, ?>> stream) {
            this.boxedClass = boxedClass;
            this.nDimArrayClass = nDimArrayClass;
            this.stream = stream;
        }

        public BaseStream<T, ?> genericStream(Object o) {
            return stream.apply(boxedClass.cast(o));
        }
    }
}
