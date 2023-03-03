package com.astroimagej.fitsio.fits;

import com.astroimagej.fitsio.bindings.Constants;
import com.astroimagej.fitsio.util.NativeCalling;
import com.astroimagej.fitsio.util.TypeHandler;
import jnr.ffi.Memory;
import jnr.ffi.byref.IntByReference;

import java.util.LinkedList;

public class TableHDU extends HDU {
    //todo handle ascii vs table
    /**
     * NAXIS2
     */
    public final long rows;
    public final LinkedList<ColInfo> colInfos;
    public TableHDU(Fits owner, Constants.HDUType hduType, long rows, LinkedList<ColInfo> colInfos) {
        super(owner, hduType);
        this.rows = rows;
        this.colInfos = colInfos;
    }

    public int getColCount() {
        return colInfos.size();
    }

    public ColHolder<?> readCol(String ofLabel) {
        for (int i = 0; i < colInfos.size(); i++) {
            if (ofLabel.equalsIgnoreCase(colInfos.get(i).label())) {
                return readCol(i);
            }
        }

        //todo don't throw, return null?
        throw new IllegalArgumentException("No column with name " + ofLabel);
    }

    public ColHolder<?> readCol(int i) {
        ensureCurrentHdu();
        if (i < 0 || i > getColCount()) {
            throw new IndexOutOfBoundsException(i);
        }

        if (rows >= Integer.MAX_VALUE) {
            throw new UnsupportedOperationException("Cannot handle FITS table data with more than 1 int of rows!");
        }

        var ci = colInfos.get(i);
        var nulVal = Memory.allocate(NativeCalling.RUNTIME, ci.dataType().byteSize());
        var array = Memory.allocateDirect(NativeCalling.RUNTIME, ci.byteSize()*rows);
        var anyNull = new IntByReference();
        NativeCalling.FITS_IO.ffgcv(owner.fptr, ci.dataType(), i+1, 1, 1, ci.len()*rows,
                nulVal, array, anyNull, owner.status);

        Object col;
        if (ci.axes().length == 1) {
            col = TypeHandler.arrayFromDataType(array, ci.dataType(), 0L, (int) rows);
        } else {
            var colA = new Object[(int) rows];
            for (int r = 0; r < rows; r++) {
                colA[r] = TypeHandler.arrayFromDataType(array, ci.dataType(), r * ci.byteSize(),
                        (int) ci.len());
            }
            col = colA;
        }

        return new ColHolder<>(col, ci, ci.isColOfImages());
    }


    @Override
    public String toString() {
        return "TableHDU{" +
                ", rows=" + rows +
                ", colInfo=" + colInfos +
                ", hduType=" + hduType +
                '}';
    }
}
