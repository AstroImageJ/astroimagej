package com.astroimagej.fitsio.fits;

import com.astroimagej.fitsio.bindings.Constants;

public record ColHolder<T>(T colArray, Constants.DataType dataType, boolean isImageArray) {
}
