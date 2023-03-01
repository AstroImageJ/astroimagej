package com.astroimagej.fitsio.bindings.types.structs;

import jnr.ffi.Runtime;
import jnr.ffi.Struct;

public class tcolumn extends Struct {
    /** column name = FITS TTYPEn keyword; */
    public AsciiString ttype = new AsciiString(70);
    /** offset in row to first byte of each column */
    public SignedLong tbcol = new SignedLong();
    /** datatype code of each column */
    public Signed32  tdatatype = new Signed32();
    /** repeat count of column; number of elements */
    public SignedLong trepeat = new SignedLong();
    /** FITS TSCALn linear scaling factor */
    public Double tscale = new Double();
    /** FITS TZEROn linear scaling zero point */
    public Double tzero = new Double();
    /** FITS null value for int image or binary table cols */
    public SignedLong tnull = new SignedLong();
    /** FITS null value string for ASCII table columns */
    public AsciiString strnull = new AsciiString(20);
    /** FITS tform keyword value  */
    public AsciiString tform = new AsciiString(10);
    /** width of each ASCII table column */
    public SignedLong twidth = new SignedLong();

    public tcolumn(Runtime runtime) {
        super(runtime);
    }
}
