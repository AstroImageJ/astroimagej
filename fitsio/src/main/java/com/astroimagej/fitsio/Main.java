package com.astroimagej.fitsio;

import com.astroimagej.fitsio.bindings.CFitsIo;
import com.astroimagej.fitsio.bindings.Constants;
import com.astroimagej.fitsio.bindings.types.structs.FitsFileHolder;
import com.astroimagej.fitsio.fits.*;
import com.astroimagej.fitsio.util.Logger;
import jnr.ffi.Runtime;
import jnr.ffi.*;
import jnr.ffi.byref.*;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;

import static com.astroimagej.fitsio.bindings.Constants.ErrorCodes.END_OF_FILE;

public class Main {
    //todo move loading to proper class
    //todo bundle cfitsio in this jar, how to load? does it need to be extracted?
    //todo build with thread safety
    //https://www.praj.in/posts/2021/exploring-jnr-part-1/
    public static void main(String[] args) {
        test3();
        /*try {
            openMemfile2();
        } catch (Exception e) {
            e.printStackTrace();
        }*/
    }

    private static void test3() {
        try (Fits f = new Fits(Files.readAllBytes(Path.of("tess-s0012-2-3_245.267484_-59.510528_51x51_astrocut.fits")))) {
            /*f.hdus.stream().filter(h -> h instanceof ImageHDU)
                    .map(h -> (ImageHDU) h).peek(h -> h.readImages());*/
            System.out.println(f.hdus);
            //if (true)return;
            //todo reading values from w/ memopen crashes, possibly a memory issue
            f.hdus.stream().filter(h -> h instanceof TableHDU)
                    .map(h -> (TableHDU) h).peek(h -> h.readCol(4)).findFirst();//todo must have terminal operation
            System.out.println(((TableHDU) f.hdus.get(1)).colInfos);
            System.out.println(((TableHDU) f.hdus.get(1)).readCol(0));
            System.out.println(((TableHDU) f.hdus.get(1)).readCol(3));
            System.out.println(((TableHDU) f.hdus.get(1)).readCol(4));
            System.out.println(((TableHDU) f.hdus.get(1)).readCol("FFI_FILE"));//todo broken
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static void openMemfile2() throws IOException {
        var lib = LibraryLoader.create(CFitsIo.class)
                .failImmediately()
                .option(LibraryOption.PreferCustomPaths, true)
                .search("D:\\Programming\\Astro\\cfitsio jdk19\\cfitdebug\\Debug")
                .search("packageFiles/common/cfitsio/windows/")
                .library("zlib")//dep of cfitsio - todo handle bundling of this
                //.load("packageFiles/common/cfitsio/windows/cfitsio");
                .load("D:\\Programming\\Astro\\cfitsio jdk19\\cfitdebug\\Debug\\cfitsio");

        var runtime = Runtime.getRuntime(lib);

        var file = Files.readAllBytes(Path.of("tess-s0012-2-3_245.267484_-59.510528_51x51_astrocut.fits"));

        var status = new IntByReference();
        var fptr = new FitsFileHolder(runtime);
        var adr = new PointerByReference();
        var size = new NativeLongByReference(file.length);

        var p = Memory.allocateDirect(runtime, file.length*8);
        p.put(0, file, 0, file.length);

        if (size.intValue() != file.length) {
            throw new IllegalStateException("bad size");
        }

        var beforeFromPointer = new byte[size.intValue()];
        p.get(0, beforeFromPointer, 0, size.intValue());

        //todo mem read isn't getting all hdus
        //it hits end of file, but sometimes complains about first key
        //is it an issue with sbyte->byte, or is the memory getting freed somewhere?
        //todo giving it the tess name crashes

        //System.out.println(file.length);
        var bp = new PointerByReference(p);
        //todo +2/3 at end of file name fixes the bug?! +1 crashes?
        // or not, +1 works - use this to workround the bug?
        //  no, with +1 it is only finding 2 HDUs (n+1?).
        //       what is going on with the memory? to cause this?
        //   bad workaround: do +n, then keep trying till with +n-1 no longer "past end of file" code
        //todo this works, Fits needs works, likely an issue with the memory being freed
        var rt = lib.ffomem(adr, "meh.fits+1", 0, bp, size, 0, /*NativeCalling::resize, */null, status);
        if (rt == 0) {
            fptr.useMemory(adr.getValue());
            Logger.logErrMsg();
            System.out.println(fptr.Fptr.get().headstart.longValue());
            System.out.println(fptr.Fptr.get().logfilesize.longValue());
            var hduCount = new IntByReference();
            lib.ffthdu(fptr, hduCount, status);
            Logger.logFitsio(status);
            System.out.println("HDU Count: " + hduCount.intValue());
            if (hduCount.intValue() != 3) {
                //throw new IllegalStateException("Missing some HDUs! %s".formatted(hduCount.intValue()));
            }

            var hdus = buildFitsStructure(lib, fptr, runtime);
            System.out.println(hdus);
            if (status.intValue() == END_OF_FILE) status = new IntByReference();

            lib.ffclos(fptr, status);
        }

        Logger.logFitsio(rt);
        //System.out.println(status.intValue());
        Logger.logFitsio(status);

        var afterFromPointer = new byte[size.intValue()];
        p.get(0, afterFromPointer, 0, size.intValue());

        Logger.logErrMsg();
        if (!Arrays.equals(file, beforeFromPointer)) {
            throw new IllegalStateException("file and before from pointer do not match");
        }

        //todo io mode = 0 now crashes (was from debug stuff)
        System.err.println(Arrays.compare(file, afterFromPointer));
        if (!Arrays.equals(file, afterFromPointer)) {//todo this is triggered (was from debug stuff?)
            throw new IllegalStateException("file and after from pointer do not match");
        }
    }

    //todo try writing ffmahd in java and using with memopen

    // Read file from memory
    private static void openMemfile() throws IOException {
        System.out.println("Hello world!");
        Path currentRelativePath = Paths.get("");
        System.out.println(Platform.getNativePlatform().locateLibrary("cfitsio", Collections.singletonList(Path.of("packageFiles/common/cfitsio/windows").toString())));
        //todo how to get this to prefer cfitsio we ship w/ windows?
        //System.setProperty("jnr.ffi.library.path", Path.of("packageFiles/common/cfitsio/windows/").toAbsolutePath().toString());
        var lib = LibraryLoader.create(CFitsIo.class)
                .failImmediately()
                .option(LibraryOption.PreferCustomPaths, true)
                //.search("D:\\Programming\\Astro\\cfitsio jdk19\\cfitdebug\\Release")//todo remove, testing path
                .search("packageFiles/common/cfitsio/windows")
                //.load(Path.of("packageFiles/common/cfitsio/windows/cfitsio.dll").toAbsolutePath().toString());
                .library("zlib")//dep of cfitsio - todo handle bundling of this
                .load("packageFiles/common/cfitsio/windows/cfitsio");//todo need relative path to get precedence over system
                //.load("cfitsio");

        //todo add version check to enforce cfitsio version?
        //var lib = LibraryLoader.loadLibrary(CFitsIo.class, null, Path.of("packageFiles/common/cfitsio/windows").toAbsolutePath().toString());
        var runtime = Runtime.getRuntime(lib);

        var f = new FloatByReference();
        lib.ffvers(f);
        System.out.println("Using cfitsio: " + f.floatValue());

        var nKeys = new IntByReference();//todo should this be long?
        var cardString = new StringBuffer(81);//todo check size. Want StringBuilder, but data is mangled?

        var file = Files.readAllBytes(Path.of("tess-s0012-2-3_245.267484_-59.510528_51x51_astrocut.fits"));
        var buffer = ByteBuffer.wrap(file);
        //var bufPtr = Pointer.wrap(runtime, buffer);
        var bufPtr = new PointerByReference(Pointer.wrap(runtime, buffer));

        var status = new IntByReference();
        var fptr = new FitsFileHolder(runtime);
        var adr = new PointerByReference();
        var size = new NativeLongByReference(buffer.capacity());

        var p = Memory.allocateDirect(runtime, file.length);
        p.put(0, file, 0, file.length);
        var bp = new PointerByReference(p);
        var rt = lib.ffomem(adr, "tess-s0012-2-3_245.267484_-59.510528_51x51_astrocut.fits", 0, bp,
                size, 3000, null, status);
        if (rt == 0) {
            fptr.useMemory(adr.getValue());
            Logger.logFitsio(status);

            var hduCount = new IntByReference();
            lib.ffthdu(fptr, hduCount, status);
            System.out.println("HDU Count: " + hduCount.intValue());
            Logger.logFitsio(status);

            var hduPosR = new IntByReference();
            lib.ffghdn(fptr, hduPosR);
            var hduPos = hduPosR.intValue();
            while (status.intValue() == 0) {
                lib.ffghsp(fptr, nKeys, null, status);
                System.out.println("nKeys: " + nKeys.intValue());
                Logger.logFitsio(status);

                for (int i = 0; i < nKeys.intValue(); i++) {
                    lib.ffgrec(fptr, i, cardString, status);
                    System.out.println(cardString);
                    Logger.logFitsio(status);
                }

                lib.ffmrhd(fptr, 1, null, status);
                Logger.logFitsio(status);
                hduPos++;
            }

            if (status.intValue() == END_OF_FILE) status = new IntByReference();

            lib.ffclos(fptr, status);
        }

        System.out.println(rt);
        System.out.println(status.intValue());
        Logger.logFitsio(status);
    }

    public static LinkedList<HDU> buildFitsStructure(CFitsIo FITS_IO, FitsFileHolder fptr, Runtime RUNTIME) {
        var hduCount = new IntByReference();
        var status = new IntByReference();
        FITS_IO.ffthdu(fptr, hduCount, status);
        System.out.println("HDU Count: " + hduCount.intValue());//todo is this 1 indexed?
        System.out.println("HDU Get:");

        var hduPosR = new IntByReference();
        var hduType = new IntByReference();
        var nAxis = new IntByReference();
        var bitpix = new IntByReference();
        var axes = new long[10];//todo handle more?
        var nCols = new IntByReference();
        var nRows = new LongLongByReference();
        var colNum = new IntByReference();
        var typeCode = new IntByReference();
        var repeat = new LongLongByReference();
        var width = new LongLongByReference();
        var axisPointer = Memory.allocate(RUNTIME, Long.BYTES*10);

        FITS_IO.ffmahd(fptr, 1, new IntByReference(), status);//todo forcing back for memopen testing
        FITS_IO.ffghdn(fptr, hduPosR);
        var hduPos = hduPosR.intValue();
        var hdus = new LinkedList<HDU>();
        while (status.intValue() == 0) {
            switch (Constants.HDUType.fromInt(hduType.intValue())) {
                case IMAGE_HDU -> {
                    FITS_IO.ffgiet(fptr, bitpix, status);//eq. type

                    FITS_IO.ffgipr(fptr, 10, bitpix, nAxis, axes, status);
                    //todo check status - if bad, don't add HDU or a bad hdu type?
                    hdus.add(hduPos-1, new ImageHDU(null, Constants.BitPixDataTypes.fromInt(bitpix.intValue()), Arrays.copyOfRange(axes, 0, nAxis.intValue())));
                }
                case ASCII_TBL, BINARY_TBL -> {
                    //todo use fits iterator? rec. by docs
                    FITS_IO.ffgnrwll(fptr, nRows, status);
                    FITS_IO.ffgncl(fptr, nCols, status);

                    var colInfos = new LinkedList<ColInfo>();

                    for (int i = 0; i < nCols.intValue(); i++) {
                        var s = new StringBuffer(30);//todo what size? don't make in loop
                        FITS_IO.ffgcnn(fptr, Constants.CaseSensitivity.CASE_INSEN, "" + (i+1), s, colNum, status);
                        //todo what to do if there is no col. heading? what is reported from this method?
                        FITS_IO.ffeqtyll(fptr, i+1, typeCode, repeat, width, status);//todo null on repeat/width?

                        //todo only get axis if repeat/width is >1 && binary table?
                        FITS_IO.ffgtdmll(fptr, i+1, 10, nAxis, axisPointer, status);
                        var a = new long[nAxis.intValue()];
                        axisPointer.get(0, a, 0, a.length);

                        colInfos.add(new ColInfo(s.toString(), Constants.DataType.fromInt(typeCode.intValue()), a));
                    }

                    hdus.add(hduPos-1, new TableHDU(null, Constants.HDUType.fromInt(hduType.intValue()), nRows.longValue(), colInfos));
                }
                case ANY_HDU -> {
                    throw new IllegalStateException("Why are we here when reading?");
                }
            }

            hduPos++;
            FITS_IO.ffmrhd(fptr, 1, hduType, status);//todo don't do this for files with 1 HDU, just break
            System.out.println("Move forward:");
        }

        return hdus;
    }
}