package com.astroimagej.fitsio;

import com.astroimagej.fitsio.bindings.CFitsIo;
import com.astroimagej.fitsio.bindings.types.structs.FitsFileHolder;
import com.astroimagej.fitsio.fits.Fits;
import com.astroimagej.fitsio.fits.TableHDU;
import com.astroimagej.fitsio.util.Logger;
import jnr.ffi.Runtime;
import jnr.ffi.*;
import jnr.ffi.byref.FloatByReference;
import jnr.ffi.byref.IntByReference;
import jnr.ffi.byref.NativeLongByReference;
import jnr.ffi.byref.PointerByReference;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;

import static com.astroimagej.fitsio.bindings.Constants.ErrorCodes.END_OF_FILE;

public class Main {
    //todo move loading to proper class
    //todo bundle cfitsio in this jar, how to load? does it need to be extracted?
    //todo build with thread safety
    //https://www.praj.in/posts/2021/exploring-jnr-part-1/
    public static void main(String[] args) {
        //test3();
        try {
            openMemfile2();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void test3() {
        //todo memopen sometimes works, sometimes fails - maybe test if fail is already open?
        try (Fits f = new Fits(Files.readAllBytes(Path.of("M13_DSS_1.fits.fz")))) {
            /*f.hdus.stream().filter(h -> h instanceof ImageHDU)
                    .map(h -> (ImageHDU) h).peek(h -> h.readImages());*/
            System.out.println(f.hdus);
            if (true)return;
            f.hdus.stream().filter(h -> h instanceof TableHDU).peek(h -> System.out.println(h instanceof TableHDU))
                    .map(h -> (TableHDU) h).peek(TableHDU::toString).peek(h -> h.readCol(4)).findFirst();//todo must have terminal operation
            System.out.println(((TableHDU) f.hdus.get(1)).colInfos);
            System.out.println(((TableHDU) f.hdus.get(1)).readCol(0));
            System.out.println(((TableHDU) f.hdus.get(1)).readCol(3));
            System.out.println(((TableHDU) f.hdus.get(1)).readCol(4));
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

        //todo mem read isn't getting all hdus
        //it hits end of file, but sometimes complains about first key
        //is it an issue with sbyte->byte, or is the memory getting freed somewhere?
        //todo giving it the tess name crashes

        //System.out.println(file.length);
        var bp = new PointerByReference(p);
        var rt = lib.ffomem(adr, "meh", 1, bp, size, 0, /*NativeCalling::resize, */null, status);
        if (rt == 0) {
            fptr.useMemory(adr.getValue());
            System.out.println(fptr.Fptr.get().headstart.longValue());
            System.out.println(fptr.Fptr.get().logfilesize.longValue());
            var hduCount = new IntByReference();
            lib.ffthdu(fptr, hduCount, status);
            Logger.logFitsio(status);
            System.out.println("HDU Count: " + hduCount.intValue());
            if (hduCount.intValue() != 3) {
                throw new IllegalStateException("Missing some HDUs! %s".formatted(hduCount.intValue()));
            }

            if (status.intValue() == END_OF_FILE) status = new IntByReference();

            lib.ffclos(fptr, status);
        }

        Logger.logFitsio(rt);
        //System.out.println(status.intValue());
        Logger.logFitsio(status);
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
}