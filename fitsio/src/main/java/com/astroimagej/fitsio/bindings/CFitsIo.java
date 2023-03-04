package com.astroimagej.fitsio.bindings;

import com.astroimagej.fitsio.bindings.types.structs.FitsFileHolder;
import jnr.ffi.Pointer;
import jnr.ffi.annotations.Delegate;
import jnr.ffi.annotations.In;
import jnr.ffi.annotations.Out;
import jnr.ffi.byref.*;

//todo properly decorate these methods
//todo how to name them something sensible
public interface CFitsIo {
    /**fits_open_file*/
    int ffopen(PointerByReference fptr, String filename, int ioMode, IntByReference status);//todo input arguments
    /**fits_open_mem_file*/
    int ffomem(PointerByReference fptr, String filename, int ioMode,
               //todo size_t for some of these, is NativeLong good enough? LongLong seems to crash at the dereference
               PointerByReference buffer, /*@size_t*/ NativeLongByReference memSize, /*@size_t*/ long deltaSize, MemRealloc resizer,
               IntByReference status);
    /**fits_create_memfile*/
    int ffimem(PointerByReference fptr, PointerByReference buffer, NativeLongByReference memSize, long deltaSize,
               MemRealloc resizer, IntByReference status);
    /**fits_get_errstatus*/
    void ffgerr(@In int status, StringBuffer errText);
    /**fits_get_version*/
    float ffvers(FloatByReference version);
    /**fits_get_hdu_num - get current HDU, 1-indexed*/
    int ffghdn(FitsFileHolder fptr, IntByReference currentHdu);
    /**fits_get_num_hdus*/
    int ffthdu(FitsFileHolder fptr, IntByReference currentHdu, IntByReference status);

    /**fits_close_file*/
    int ffclos(FitsFileHolder fptr, IntByReference status);
    /**fits_get_hdrspace*/
    int ffghsp(FitsFileHolder fptr, IntByReference nKeys, IntByReference nMore, IntByReference status);
    /**fits_read_record*/
    int ffgrec(FitsFileHolder fptr, int cardPosition, @Out StringBuffer nMore, IntByReference status);
    /**fits_movrel_hdu*/
    int ffmrhd(FitsFileHolder fptr, int moveN, @Out IntByReference extType, IntByReference status);//todo extType enum?
    /**fits_movabs_hdu*/
    int ffmahd(FitsFileHolder fptr, int hduNum, @Out IntByReference extType, IntByReference status);
    /**fits_get_img_param
     * Does not fully handle naxis of many dimensions
     * */
    int ffgipr(FitsFileHolder fptr, int maxDim, @Out IntByReference bitpix, @Out IntByReference nAxis, @Out long[] axisSizes, IntByReference status);
    /**fits_get_img_equivtype*/
    int ffgiet(FitsFileHolder fptr, @Out IntByReference bitpix, IntByReference status);
    /**ffits_get_num_rowsll*/
    int ffgnrwll(FitsFileHolder fptr, @Out LongLongByReference nRows, IntByReference status);
    /**fits_get_num_cols*/
    int ffgncl(FitsFileHolder fptr, @Out IntByReference nCol, IntByReference status);
    /**fits_get_colnum*/
    int ffgcno(FitsFileHolder fptr, @Out IntByReference nCol, IntByReference status);
    /**fits_get_colname*/
    int ffgcnn(FitsFileHolder fptr, Constants.CaseSensitivity sensitivity, String tmpl, @Out StringBuffer colName,
               @In IntByReference nCol, IntByReference status);
    /**fits_read_pixll*/
    int ffgpxvll(FitsFileHolder fptr, Constants.DataType dataType, Pointer firstPix, long nElements,
                 Pointer nulVal, Pointer imageArray, IntByReference anyNull, IntByReference status);
    /**fits_get_eqcoltypell*/
    int ffeqtyll(FitsFileHolder fptr, int colNum, IntByReference typeCode, LongLongByReference repeat,
                 LongLongByReference width, IntByReference status);
    /**fits_read_tdimll*/
    int ffgtdmll(FitsFileHolder fptr, int colNum, int maxDim,
                 @Out IntByReference nAxis, @Out Pointer nAxes, IntByReference status);
    /**fits_read_col*/
    int ffgcv(FitsFileHolder fptr, Constants.DataType dataType, int colNum, long firstRow, long firstElem, long nElements,
              Pointer nulVal, Pointer dataArray, IntByReference anyNull, IntByReference status);
    /**fits_read_errmsg*/
    void ffgmsg(StringBuffer errmsg);

    interface MemRealloc {
        @Delegate
        Pointer resize(Pointer p, long newSize);//todo size_t for some of these, but breaks things - we are only 64bit
    }
}
