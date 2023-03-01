package com.astroimagej.fitsio.fits;

import com.astroimagej.fitsio.bindings.Constants;
import com.astroimagej.fitsio.util.TypeHandler;
import jnr.ffi.Memory;
import jnr.ffi.byref.IntByReference;

import java.util.Arrays;

public class ImageHDU extends HDU {
    public final Constants.BitPixDataTypes equivalentBitpix;//todo redo this to internal data type, as some formats want something... better
    public final long[] axis;
    public ImageHDU(Fits owner, Constants.BitPixDataTypes equivalentBitpix, long... axis) {
        super(owner, Constants.HDUType.IMAGE_HDU);
        this.equivalentBitpix = equivalentBitpix;
        this.axis = axis;
    }

    public int getNAxis() {
        return axis.length;
    }

    /**
     * @param n 1-indexed
     */
    public long getNAxis(int n) {
        return axis[n-1];
    }

    /**
     * The primary (first) HDU is always an image HDU; however compressed fits files are tables,
     * so they have a null image HDU before the table.
     *
     * @return if this is not a null image HDU.
     */
    public boolean hasImages() {
        return axis.length > 0;
    }

    public Object[] readImages() {
        ensureCurrentHdu();

        if (axis.length > 3) {
            throw new UnsupportedOperationException("Cannot handle FITS image data with %s axis!".formatted(axis.length));
        }

        var dType = TypeHandler.fromEquivalentBitpix(equivalentBitpix);
        var nElem = 1L;
        for (long l : axis) {
            nElem *= l;
        }

        if (nElem >= Integer.MAX_VALUE) {
            throw new UnsupportedOperationException("Cannot handle FITS image data with more than 1 int of pixels!");
        }

        var nulVal = Memory.allocate(RUNTIME, dType.byteSize());
        var array = Memory.allocateDirect(RUNTIME, dType.byteSize() * nElem);
        var anyNull = new IntByReference();

        var firstPixel = Memory.allocateDirect(RUNTIME, Long.BYTES * getNAxis());//1-based
        for (int l = 0; l < getNAxis(); l++) {
            firstPixel.putLongLong((long) Long.BYTES * l, 1);
        }

        FITS_IO.ffgpxvll(owner.fptr, dType, firstPixel, nElem, nulVal, array, anyNull, owner.status);

        var imageCount = getNAxis() == 3 ? getNAxis(3) : 1L;
        var pxlCount = getNAxis(1) * getNAxis(2);
        var o = new Object[(int) imageCount];
        for (int image = 0; image < imageCount; image++) {
            o[image] = TypeHandler.arrayFromDataType(array, dType, (long) image * dType.byteSize(), (int) pxlCount);
        }

        return o;
    }

    @Override
    public String toString() {
        return "ImageHDU{" +
                "dataType=" + equivalentBitpix +
                ", nAxes=" + axis.length +
                ", axis=" + Arrays.toString(axis) +
                ", hduType=" + hduType +
                '}';
    }
}
