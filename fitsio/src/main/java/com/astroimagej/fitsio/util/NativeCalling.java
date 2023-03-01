package com.astroimagej.fitsio.util;

import com.astroimagej.fitsio.bindings.CFitsIo;
import jnr.ffi.Runtime;
import jnr.ffi.*;

public class NativeCalling {
    protected static final CFitsIo FITS_IO;
    protected static final Runtime RUNTIME;

    static {//todo handle loading better than this, constructor?
        FITS_IO = LibraryLoader.create(CFitsIo.class)
                .failImmediately()
                .option(LibraryOption.PreferCustomPaths, true)
                //.search("D:\\Programming\\Astro\\cfitsio jdk19\\cfitdebug\\Release")
                .search("packageFiles/common/cfitsio/windows/")
                .library("zlib")//dep of cfitsio - todo handle bundling of this
                .load("packageFiles/common/cfitsio/windows/cfitsio");
                //.load("D:\\Programming\\Astro\\cfitsio jdk19\\cfitdebug\\Release\\cfitsio");//todo debug build
        RUNTIME = Runtime.getRuntime(FITS_IO);
    }

    public static Pointer resize(Pointer buffer, long newSize) {
        var p = Memory.allocateDirect(buffer.getRuntime(), newSize);
        buffer.transferTo(0, p, 0, buffer.size());
        return p;
    }
}
