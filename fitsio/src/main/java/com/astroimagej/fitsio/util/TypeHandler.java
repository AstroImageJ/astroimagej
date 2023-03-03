package com.astroimagej.fitsio.util;

import com.astroimagej.fitsio.bindings.Constants;
import com.astroimagej.fitsio.bindings.Constants.DataType;
import jnr.ffi.Pointer;

import java.util.Arrays;

import static com.astroimagej.fitsio.bindings.Constants.DataType.*;

public class TypeHandler {
    /**
     * For image reading.
     *
     * @param bitPix the equivalent bitpix
     * @return the {@link DataType} to request pixel values be read in, clamp-/converting to types that AIJ supports.
     */
    public static DataType fromEquivalentBitpix(Constants.BitPixDataTypes bitPix) {
        //todo what about the other T*types?
        //todo collapse cases
        return switch (bitPix) {
            case SIGNED_SHORT -> TSHORT;
            case SIGNED_INT_32 -> TINT;
            case SIGNED_LONG -> TFLOAT;//TLONGLONG;
            case UNSIGNED_BYTE -> TSBYTE;
            case FLOAT -> TFLOAT;
            case DOUBLE -> TFLOAT;//TDOUBLE;//todo AIJ has no DoubleProcessor
            case SIGNED_BYTE -> TSBYTE;
            case UNSIGNED_LONG -> TFLOAT; //todo AIJ/Java does not support BigInteger
            case UNSIGNED_SHORT -> TINT;
            case UNSIGNED_INT_32 -> TFLOAT;//TLONGLONG;
        };
    }

    /**
     * For reading images from a column.
     * <p>
     * Converts one {@link DataType} to another that AIJ can handle.
     */
    public static DataType toIJImageType(DataType dataType) {
        return switch (dataType) {
            // Java bytes are signed
            case TBIT, TBYTE, TLOGICAL -> TSBYTE;
            // Java shorts are signed
            case TUSHORT, TINT32BIT -> TINT;
            // IJ has no double or long processor, no support for unsigned long
            case TLONG, TDOUBLE, TLONGLONG, TULONGLONG, TULONG, TUINT -> TFLOAT;
            // todo how to handle these types?
            case TCOMPLEX, TDBLCOMPLEX -> throw new IllegalStateException("How to handle this type: " + dataType);
            case TSTRING -> throw new IllegalStateException("Images cannot be strings!");
            // These types map to themself
            case TSHORT, TINT, TFLOAT, TSBYTE -> dataType;
        };
    }

    /**
     * For reading non images from a column.
     * <p>
     * Converts one {@link DataType} to another that AIJ can handle.
     */
    public static DataType toIJNonImageType(DataType dataType) {
        return switch (dataType) {
            // Java bytes are signed
            case TBIT, TBYTE, TLOGICAL -> TSBYTE;
            // Java shorts are signed
            case TUSHORT, TINT32BIT -> TINT;
            case TUINT -> TLONGLONG;
            case TULONG, TULONGLONG -> TDOUBLE;
            case TCOMPLEX, TDBLCOMPLEX -> TSTRING; //todo untested
            // These types map to themself
            case TSBYTE, TSHORT, TINT, TFLOAT, TSTRING, TDOUBLE, TLONGLONG, TLONG -> dataType;
        };
    }


    /**
     * Extract an array from a pointer.
     * <p>
     * Possible output types: {@code short[]}, {@code float[]}, {@code int[]}, {@code byte[]}, {@code long[]},
     * {@code double[]}, {@code String[]}.
     *
     * @see TypeHandler#arrayFromDataType(Pointer, DataType, long, int)
     */
    public static Object arrayFromDataType(Pointer source, DataType dataType, int len) {
        return arrayFromDataType(source, dataType, 0, len);
    }

    /**
     * Extract an array from a pointer.
     * <p>
     * Possible output types: {@code short[]}, {@code float[]}, {@code int[]}, {@code byte[]}, {@code long[]},
     * {@code double[]}, {@code String[]}.
     */
    public static Object arrayFromDataType(Pointer source, DataType dataType, long offset, int len) {
        return switch (dataType) {
            case TSHORT -> {
                var o = new short[len];
                source.get(offset, o, 0, len);
                yield o;
            }
            case TFLOAT -> {
                var o = new float[len];
                source.get(offset, o, 0, len);
                yield o;
            }
            case TINT, TINT32BIT -> {
                var o = new int[len];
                source.get(offset, o, 0, len);
                yield o;
            }
            case TSBYTE -> {
                var o = new byte[len];
                source.get(offset, o, 0, len);
                yield o;
            }
            case TLONG, TLONGLONG -> {// LONG=LONGLONG on 64bit, which we use
                var o = new long[len];
                source.get(offset, o, 0, len);
                yield o;
            }
            case TDOUBLE -> {
                var o = new double[len];
                source.get(offset, o, 0, len);
                yield o;
            }
            case TSTRING ->  {
                var l = source.getNullTerminatedStringArray(offset);
                System.out.println(source.getString(0));
                System.out.println(Arrays.toString(l));//todo fix reading strings
                yield l;
            }
            default -> throw new IllegalStateException("Unexpected value: " + dataType);
        };
    }
}
