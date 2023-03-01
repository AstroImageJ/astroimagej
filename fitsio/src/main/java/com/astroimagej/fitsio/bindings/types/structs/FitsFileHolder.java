package com.astroimagej.fitsio.bindings.types.structs;

import jnr.ffi.Runtime;
import jnr.ffi.Struct;

public class FitsFileHolder extends Struct {
    /** HDU position in file; 0 = first HDU */
    public Signed32 HDUposition = new Signed32();
    /** pointer to FITS file structure */
    public StructRef<FITSFile> Fptr = new StructRef<>(FITSFile.class);

    public FitsFileHolder(Runtime runtime) {
        super(runtime);
    }
}
