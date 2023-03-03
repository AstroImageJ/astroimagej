package com.astroimagej.fitsio.fits;

public record ColHolder<T>(T colArray, ColInfo colInfo, boolean isImageArray) {
}
