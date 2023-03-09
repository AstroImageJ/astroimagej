package com.astroimagej.fitsio.fits;

import com.astroimagej.fitsio.bindings.Constants;
import com.astroimagej.fitsio.util.NativeCalling;
import jnr.ffi.byref.IntByReference;

import java.util.ArrayList;

public class HDU extends NativeCalling {
    protected final Fits owner;//todo is this a good solution? make not final for writing?
    public final Constants.HDUType hduType;
    public ArrayList<String> header = null;

    public HDU(Fits owner, Constants.HDUType hduType) {
        this.owner = owner;
        this.hduType = hduType;
    }

    /**
     * Make sure the FITS file is pointing to this HDU.
     */
    protected void ensureCurrentHdu() {
        int hduPos;
        if (owner == null || (hduPos = owner.hdus.indexOf(this)) == -1) {
            throw new IllegalStateException("Trying to read image of orphaned HDU");
        }
        owner.setCurrentHdu(hduPos/*+1*/);//todo +1 breaks memopen, what about ffopen?
    }

    public ArrayList<String> getHeader() {
        if (header != null) {
            return header;
        }

        ensureCurrentHdu();

        var nKeys = new IntByReference();//todo should this be long?
        var cardString = new StringBuffer(Constants.FLEN_CARD);
        FITS_IO.ffghsp(owner.fptr, nKeys, null, owner.status);
        owner.logStatus();

        var header = new ArrayList<String>();

        for (int i = 0; i < nKeys.intValue(); i++) {
            FITS_IO.ffgrec(owner.fptr, i, cardString, owner.status);
            owner.logStatus();
            header.add(cardString.toString());
            //todo handle bad status?
        }

        header.add("END");
        this.header = header;
        return header/*.toArray(String[]::new)*/;
    }
}
