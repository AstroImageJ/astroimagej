package com.astroimagej.fitsio.util;

import com.astroimagej.fitsio.bindings.Constants;
import jnr.ffi.byref.IntByReference;

public class Logger extends NativeCalling {
    private static final StringBuffer errText = new StringBuffer(Constants.FLEN_STATUS);

    public static void logFitsio(IntByReference status) {
        logFitsio(status.intValue());
    }

    public static void logFitsio(int status) {
        FITS_IO.ffgerr(status, errText);
        System.out.println(errText);
    }
}
