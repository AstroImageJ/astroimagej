package com.astroimagej.fitsio.fits;

import com.astroimagej.fitsio.Main;
import com.astroimagej.fitsio.bindings.types.structs.FitsFileHolder;
import com.astroimagej.fitsio.util.Logger;
import com.astroimagej.fitsio.util.NativeCalling;
import jnr.ffi.Memory;
import jnr.ffi.ObjectReferenceManager;
import jnr.ffi.byref.IntByReference;
import jnr.ffi.byref.LongLongByReference;
import jnr.ffi.byref.NativeLongByReference;
import jnr.ffi.byref.PointerByReference;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.LinkedList;

import static com.astroimagej.fitsio.bindings.Constants.*;
import static com.astroimagej.fitsio.bindings.Constants.ErrorCodes.END_OF_FILE;

public class Fits extends NativeCalling implements AutoCloseable {
    private static final int MAX_HANDLED_AXIS = 10;

    protected final FitsFileHolder fptr;
    public final LinkedList<HDU> hdus;
    private final ObjectReferenceManager<Object> referenceManager;
    private ByteBuffer buffer;
    protected IntByReference status;

    //todo how to handle failure to open/status failure?
    public Fits(Path file) throws FileNotFoundException {
        this();
        var adr = new PointerByReference();
        if (FITS_IO.ffopen(adr, file.toString(), 0, status) == 0) {
            fptr.useMemory(adr.getValue());
            buildFitsStructure();
        } else {
            //todo improve failure modes, get from err stack
            throw new FileNotFoundException("Failed to open fits file");
        }
    }

    public Fits(InputStream stream) throws IOException {
        this(stream.readAllBytes());
        stream.close();
    }

    public Fits(byte[] fileMemory) throws IOException {
        this();
        buffer = ByteBuffer.wrap(fileMemory);//field to try and keep memory alive?
        var fileDataPointer = Memory.allocateDirect(RUNTIME, fileMemory.length * Integer.BYTES);

        referenceManager.add(fileMemory);

        fileDataPointer.put(0, fileMemory, 0, fileMemory.length);
        var size = new NativeLongByReference(buffer.capacity());
        var bp = new PointerByReference(fileDataPointer);
        var adr = new PointerByReference();
        var rt = 0;

        var maxHdus = 10;

        for (int i = maxHdus; i >= 0; i--) {
            System.out.println("Try opening memory with: " + i);
            rt = FITS_IO.ffomem(adr, "memOpen.fits+"+i, 0, bp, size, 0, null, status);
            if (rt == 0) {
                fptr.useMemory(adr.getValue());
                //buildFitsStructure();
                System.out.println(Main.buildFitsStructure(FITS_IO, fptr, RUNTIME));
                System.out.println("Opening memory with: " + i);
                return;
            } else if (status.intValue() != END_OF_FILE) {
                //todo improve failure modes, get from err stack
                Logger.logFitsio(status);
                throw new IOException("Failed to open fits file: %s".formatted(rt));
            }
            Logger.logFitsio(status);
            status = new IntByReference();
        }

        throw new IOException("Failed to open fits file");
    }

    //todo fits reopen method to change open method to readwrite/readonly

    //todo method to get hdu, don't give access to hdus array - ensure that the structure was build/hdu exists

    public Fits() {
        fptr = new FitsFileHolder(RUNTIME);
        status = new IntByReference();
        hdus = new LinkedList<>();
        referenceManager = ObjectReferenceManager.newInstance(RUNTIME);
    }

    public int currentHdu() {
        return fptr.HDUposition.intValue();
    }

    public void setCurrentHdu(int hdu) {
        FITS_IO.ffmahd(fptr, hdu, new IntByReference(HDUType.ANY_HDU.intValue()), status);
    }

    @Override
    public void close() throws Exception {
        FITS_IO.ffclos(fptr, status);
        hdus.clear();
    }

    public void logStatus() {
        Logger.logFitsio(status);
    }

    //todo method to get first set of hdu with images (hdu/col)?

    //todo have HDU objects as inner class of this so they get the pointer and it gets cleaned up properly?
    //todo have cache for getting label/hdu, have chache for parsing key values/names from cards

    private void buildFitsStructure() {
        var hduCount = new IntByReference();
        FITS_IO.ffthdu(fptr, hduCount, status);
        System.out.println("HDU Count: " + hduCount.intValue());//todo is this 1 indexed?
        System.out.println("HDU Get:");
        logStatus();

        var hduPosR = new IntByReference();
        var hduType = new IntByReference();
        var nAxis = new IntByReference();
        var bitpix = new IntByReference();
        var axes = new long[MAX_HANDLED_AXIS];//todo handle more?
        var nCols = new IntByReference();
        var nRows = new LongLongByReference();
        var colNum = new IntByReference();
        var typeCode = new IntByReference();
        var repeat = new LongLongByReference();
        var width = new LongLongByReference();
        var axisPointer = Memory.allocate(RUNTIME, Long.BYTES*MAX_HANDLED_AXIS);

        FITS_IO.ffghdn(fptr, hduPosR);
        var hduPos = hduPosR.intValue();
        while (status.intValue() == 0) {
            switch (HDUType.fromInt(hduType.intValue())) {
                case IMAGE_HDU -> {
                    FITS_IO.ffgiet(fptr, bitpix, status);//eq. type

                    FITS_IO.ffgipr(fptr, MAX_HANDLED_AXIS, bitpix, nAxis, axes, status);
                    //todo check status - if bad, don't add HDU or a bad hdu type?
                    hdus.add(hduPos-1, new ImageHDU(this, BitPixDataTypes.fromInt(bitpix.intValue()), Arrays.copyOfRange(axes, 0, nAxis.intValue())));
                }
                case ASCII_TBL, BINARY_TBL -> {
                    //todo use fits iterator? rec. by docs
                    FITS_IO.ffgnrwll(fptr, nRows, status);
                    FITS_IO.ffgncl(fptr, nCols, status);

                    var colInfos = new LinkedList<ColInfo>();

                    for (int i = 0; i < nCols.intValue(); i++) {
                        var s = new StringBuffer(30);//todo what size? don't make in loop
                        FITS_IO.ffgcnn(fptr, CaseSensitivity.CASE_INSEN, "" + (i+1), s, colNum, status);
                        //todo what to do if there is no col. heading? what is reported from this method?
                        FITS_IO.ffeqtyll(fptr, i+1, typeCode, repeat, width, status);//todo null on repeat/width?

                        //todo only get axis if repeat/width is >1 && binary table?
                        FITS_IO.ffgtdmll(fptr, i+1, MAX_HANDLED_AXIS, nAxis, axisPointer, status);
                        var a = new long[nAxis.intValue()];
                        axisPointer.get(0, a, 0, a.length);

                        colInfos.add(new ColInfo(s.toString(), DataType.fromInt(typeCode.intValue()), a));
                    }

                    hdus.add(hduPos-1, new TableHDU(this, HDUType.fromInt(hduType.intValue()), nRows.longValue(), colInfos));
                }
                case ANY_HDU -> {
                    throw new IllegalStateException("Why are we here when reading?");
                }
            }

            hduPos++;
            FITS_IO.ffmrhd(fptr, 1, hduType, status);//todo don't do this for files with 1 HDU, just break
            System.out.println("Move forward:");
            logStatus();
        }

        System.out.println("Read structures finish:");
        logStatus();

        if (status.intValue() == END_OF_FILE) status = new IntByReference();
        //todo what about other error states?
    }
}
