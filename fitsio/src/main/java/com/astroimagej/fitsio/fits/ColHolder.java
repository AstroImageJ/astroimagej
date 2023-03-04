package com.astroimagej.fitsio.fits;

public record ColHolder<T>(T colArray, ColInfo colInfo, boolean isImageArray) {
    //todo methods that cast and return the correct type for colArray
    //takes in a T/U and returns it?
}
