package com.astroimagej.fitsio.bindings.types.structs;

import jnr.ffi.Runtime;
import jnr.ffi.Struct;
import jnr.ffi.byref.IntByReference;
import jnr.ffi.byref.LongLongByReference;
import jnr.ffi.byref.NativeLongByReference;
import jnr.ffi.byref.PointerByReference;

import static com.astroimagej.fitsio.bindings.Constants.MAX_COMPRESS_DIM;
import static com.astroimagej.fitsio.bindings.Constants.NIOBUF;

/**
 * FITSfile, fitsio.h
 */
public class FITSFile extends Struct {
    /** handle returned by the file open function */
    public Signed32 filehandle = new Signed32();
    /** defines which set of I/O drivers should be used */
    public Signed32 driver = new Signed32();
    /** number of opened 'fitsfiles' using this structure */
    public Signed32 open_count = new Signed32();
    /** file name */
    public AsciiStringRef filename = new AsciiStringRef();
    /** magic value used to verify that structure is valid */
    public Signed32 validcode = new Signed32();
    /** flag meaning only copy the specified extension */
    public Signed32 only_one = new Signed32();
    /** flag for file opened with request to ignore extended syntax */
    public Signed32 noextsyntax = new Signed32();
    /** current size of the physical disk file in bytes */
    public Signed64 filesize = new Signed64();
    /** logical size of file, including unflushed buffers */
    public Signed64 logfilesize = new Signed64();
    /** is this the last HDU in the file? 0 = no, else yes */
    public Signed32 lasthdu = new Signed32();
    /** current logical I/O pointer position in file */
    public Signed64 bytepos = new Signed64();
    /** current I/O pointer position in the physical file */
    public Signed64 io_pos = new Signed64();
    /** number of I/O buffer currently in use */
    public Signed32 curbuf = new Signed32();
    /** current HDU number; 0 = primary array */
    public Signed32 curhdu = new Signed32();
    /** 0 = primary array, 1 = ASCII table, 2 = binary table */
    public Signed32 hdutype = new Signed32();
    /** 0 = readonly, 1 = readwrite */
    public Signed32 writemode = new Signed32();
    /** highest numbered HDU known to exist in the file */
    public Signed32 maxhdu = new Signed32();
    /** dynamically allocated dimension of headstart array */
    public Signed32 MAXHDU = new Signed32();
    /** byte offset in file to start of each HDU */
    public LongLongByReference headstart = new LongLongByReference();
    /** byte offest in file to end of the current HDU label */
    public Signed64 headend = new Signed64();
    /** byte offest to where the END keyword was last written */
    public Signed64 ENDpos = new Signed64();
    /** byte offset in file to beginning of next keyword */
    public Signed64 nextkey = new Signed64();
    /** byte offset in file to start of the current data unit */
    public Signed64 datastart = new Signed64();
    /** dimension of image; cached for fast access */
    public Signed32 imgdim = new Signed32();
    /** length of each axis; cached for fast access */
    public Signed64[] imgnaxis = array(new Signed64[99]);
    /** number of fields in the table (primary array has 2 */
    public Signed32 tfield = new Signed32();
    /** used by ffgcnn to record starting column number */
    public Signed32 startcol = new Signed32();
    /** original number of rows (value of NAXIS2 keyword)  */
    public Signed64 origrows = new Signed64();
    /** number of rows in the table (dynamically updated) */
    public Signed64 numrows = new Signed64();
    /** length of a table row or image size (bytes) */
    public Signed64 rowlength = new Signed64();
    /** pointer to the table structure */
    public StructRef<tcolumn> tableptr = new StructRef<>(tcolumn.class);
    /** heap start byte relative to start of data unit */
    public Signed64 heapstart = new Signed64();
    /** size of the heap, in bytes */
    public Signed64 heapsize = new Signed64();

    /* the following elements are related to compressed images */

    /* these record the 'requested' options to be used when the image is compressed */
    /** requested image compression algorithm */
    public Signed32 request_compress_type = new Signed32();
    /** requested tiling size */
    public SignedLong[] request_tilesize = array(new SignedLong[MAX_COMPRESS_DIM]);
    /** requested quantize level */
    public Float request_quantize_level = new Float();
    /** requested  quantizing method */
    public Signed32 request_quantize_method = new Signed32();
    /** starting offset into the array of random dithering */
    public Signed32 request_dither_seed = new Signed32();
    /** lossy compress integer image as if float image? */
    public Signed32 request_lossy_int_compress = new Signed32();
    /** use '1Q' rather then '1P' variable length arrays */
    public Signed32 request_huge_hdu = new Signed32();
    /** requested HCOMPRESS scale factor */
    public Float request_hcomp_scale = new Float();
    /** requested HCOMPRESS smooth parameter */
    public Signed32 request_hcomp_smooth = new Signed32();

    /* these record the actual options that were used when the image was compressed */

    /** type of compression algorithm */
    public Signed32 compress_type = new Signed32();
    /** size of compression tiles */
    public SignedLong[] tilesize = array(new SignedLong[MAX_COMPRESS_DIM]);
    /** floating point quantization level */
    public Float quantize_level = new Float();
    /** floating point pixel quantization algorithm */
    public Signed32 quantize_method = new Signed32();
    /** starting offset into the array of random dithering */
    public Signed32 dither_seed = new Signed32();

    /* other compression parameters */

    /** 1 if HDU contains a compressed image, else 0 */
    public Signed32 compressimg = new Signed32();
    /** compression type string */
    public AsciiString zcmptype = new AsciiString(12);
    /** FITS data type of image (BITPIX) */
    public Signed32 zbitpix = new Signed32();
    /** dimension of image */
    public Signed32 zndim = new Signed32();
    /** length of each axis */
    public SignedLong[] znaxis = array(new SignedLong[MAX_COMPRESS_DIM]);
    /** max number of pixels in each image tile */
    public SignedLong maxtilelen = new SignedLong();
    /** maximum byte length of tile compressed arrays */
    public SignedLong maxelem = new SignedLong();

    /** column number for COMPRESSED_DATA column */
    public Signed32 cn_compressed = new Signed32();
    /** column number for UNCOMPRESSED_DATA column */
    public Signed32 cn_uncompressed = new Signed32();
    /** column number for GZIP2 lossless compressed data */
    public Signed32 cn_gzip_data = new Signed32();
    /** column number for ZSCALE column */
    public Signed32 cn_zscale = new Signed32();
    /** column number for ZZERO column */
    public Signed32 cn_zzero = new Signed32();
    /** column number for the ZBLANK column */
    public Signed32 cn_zblank = new Signed32();

    /** scaling value, if same for all tiles */
    public Double zscale = new Double();
    /** zero pt, if same for all tiles */
    public Double zzero = new Double();
    /** value of the BSCALE keyword in label */
    public Double cn_bscale = new Double();
    /** value of the BZERO keyword (may be reset) */
    public Double cn_bzero = new Double();
    /** actual value of the BZERO keyword  */
    public Double cn_actual_bzero = new Double();
    /** value for null pixels, if not a column */
    public Signed32 zblank = new Signed32();

    /** first compression parameter: Rice pixels/block */
    public Signed32 rice_blocksize = new Signed32();
    /** 2nd compression parameter:   Rice bytes/pixel */
    public Signed32 rice_bytepix = new Signed32();
    /** 1st hcompress compression parameter */
    public Float hcomp_scale = new Float();
    /** 2nd hcompress compression parameter */
    public Signed32 hcomp_smooth = new Signed32();

    /** row number of the array of uncompressed tiledata */
    public IntByReference tilerow = new IntByReference();
    /** length of the array of tile data in bytes */
    public NativeLongByReference tiledatasize = new NativeLongByReference();
    /** datatype of the array of tile (TINT, TSHORT, etc) */
    public IntByReference tiletype = new IntByReference();
    /** array of uncompressed tile of data, for row *tilerow */
    public PointerByReference tiledata = new PointerByReference();
    /** array of optional array of null value flags */
    public PointerByReference tilenullarray = new PointerByReference();
    /** anynulls in the array of tile? */
    public IntByReference tileanynull = new IntByReference();

    /** pointer to FITS file I/O buffers. Originally of type char* */
    public Pointer iobuffer = new Pointer();
    /** file record number of each of the buffers */
    public SignedLong[] bufrecnum = array(new SignedLong[NIOBUF]);
    /** has the corresponding buffer been modified? */
    public Signed32[] dirty = array(new Signed32[NIOBUF]);
    /** relative age of each buffer */
    public Signed32[] ageindex = array(new Signed32[NIOBUF]);

    public FITSFile(Runtime runtime) {
        super(runtime);
    }
}
