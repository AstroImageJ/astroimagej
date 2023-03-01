package com.astroimagej.fitsio.util;

import com.astroimagej.fitsio.bindings.Constants;
import com.astroimagej.fitsio.bindings.Constants.DataType;
import jnr.ffi.Pointer;

import static com.astroimagej.fitsio.bindings.Constants.DataType.*;

public class TypeHandler {
    //todo move this?
    public static DataType fromEquivalentBitpix(Constants.BitPixDataTypes bitPix) {
        //todo what about the other T*types?
        //todo collapse cases
        return switch (bitPix) {
            case SIGNED_SHORT -> TSHORT;
            case SIGNED_INT_32 -> TINT;
            case SIGNED_LONG -> TFLOAT;//TLONGLONG;
            case UNSIGNED_BYTE -> TSBYTE; //todo is this right? Java only has signed bytes
            case FLOAT -> TFLOAT;
            case DOUBLE -> TFLOAT;//TDOUBLE;//todo AIJ has no DoubleProcessor
            case SIGNED_BYTE -> TSBYTE;
            case UNSIGNED_LONG -> TFLOAT; //todo AIJ/Java does not support BigInteger
            case UNSIGNED_SHORT -> TINT; //todo no unsigned short
            case UNSIGNED_INT_32 -> TFLOAT;//TLONGLONG;//todo no unsigned int
        };
    }

    /**
     * Compacts types into the few IJ an handle for images.
     */
    public static DataType toIJImageType(DataType dataType) {
        return switch (dataType) {
            case TBIT, TBYTE -> TSBYTE;
            //case TSBYTE -> null;
            //case TLOGICAL -> null;
            //case TSTRING -> null;
            case TUSHORT, TINT32BIT -> TINT;
            //case TSHORT -> null;
            //case TINT -> null;
            case TLONG, TDOUBLE, TLONGLONG, TULONGLONG, TULONG, TUINT -> TFLOAT;
            //case TFLOAT -> null;
            case TCOMPLEX, TDBLCOMPLEX -> throw new IllegalStateException("How to handle this type: " + dataType);//todo convert to string?
            default -> dataType;
        };
    }


    public static Object arrayFromDataType(Pointer source, DataType dataType, int len) {
        return arrayFromDataType(source, dataType, 0, len);
    }

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
            default -> throw new IllegalStateException("Unexpected value: " + dataType);
        };
    }
}
