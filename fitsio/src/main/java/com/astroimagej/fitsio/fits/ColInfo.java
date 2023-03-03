package com.astroimagej.fitsio.fits;

import com.astroimagej.fitsio.bindings.Constants.DataType;
import com.astroimagej.fitsio.util.TypeHandler;

public record ColInfo(String label, DataType dataType, long[] axes) {
    public long byteSize() {
        return dataType().byteSize() * len();
    }

    public long len() {
        long p = 1;
        for (long l : axes) {
            p *= l;
        }
        return p;
    }

    public boolean isColOfImages() {
        return switch (dataType) {
            case TSTRING, TCOMPLEX, TDBLCOMPLEX, TLOGICAL -> false;
            default -> axes.length == 2;//todo fits cube in table?
        };
    }

    @Override
    public DataType dataType() {
        return isColOfImages() ? TypeHandler.toIJImageType(dataType) : TypeHandler.toIJNonImageType(dataType);
    }
}
