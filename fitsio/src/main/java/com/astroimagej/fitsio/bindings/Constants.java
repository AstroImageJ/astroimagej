package com.astroimagej.fitsio.bindings;

import jnr.ffi.util.EnumMapper;

/**
 * All constants are pulled from label/c-code of cfitsio
 */
public class Constants {
    //todo how to get these from the lib dynamically?
    public static final int MAX_COMPRESS_DIM = 6;
    public static final int NIOBUF = 40;

    /** max length of a filename  */
    public static final int FLEN_FILENAME = 1025;//todo check usage, input file name
    /** max length of a keyword (HIERARCH convention) */
    public static final int FLEN_KEYWORD = 75;
    /** length of a FITS label card */
    public static final int FLEN_CARD = 81;
    /** max length of a keyword value string */
    public static final int FLEN_VALUE = 71;
    /** max length of a keyword comment string */
    public static final int FLEN_COMMENT = 73;
    /** max length of a FITSIO error message */
    public static final int FLEN_ERRMSG = 81;
    /** max length of a FITSIO status text string */
    public static final int FLEN_STATUS = 31;

    //todo get more constants, such as image type and lengths
    //make type an enum?

    private Constants() {}

    public enum HDUType implements EnumMapper.IntegerEnum {
        /** Primary Array or IMAGE HDU */
        IMAGE_HDU  (0),
        /** ASCII table HDU  */
        ASCII_TBL  (1),
        /** Binary table HDU */
        BINARY_TBL (2),
        /** matches any HDU type */
        ANY_HDU   (-1);

        private final int value;

        HDUType(int value) {this.value = value;}

        @Override
        public int intValue() {return value;}

        public static HDUType fromInt(int i) {
            for (HDUType type : values()) {
                if (i == type.intValue())
                    return type;
            }

            throw new IllegalArgumentException("Unknown HDU type of %d".formatted(i));
        }
    }

    public enum BitPixDataTypes implements EnumMapper.IntegerEnum {//todo improve names
        UNSIGNED_BYTE(Byte.SIZE),
        SIGNED_SHORT(Short.SIZE),
        SIGNED_INT_32(Integer.SIZE),
        SIGNED_LONG(Long.SIZE),
        FLOAT(-Float.SIZE),
        DOUBLE(-Double.SIZE),

        /**
         * Not a real fits type, use {@link BitPixDataTypes#UNSIGNED_BYTE} and BZERO/SCALE options.
         */
        SIGNED_BYTE(10),
        /**
         * Not a real fits type, use {@link BitPixDataTypes#SIGNED_SHORT} and BZERO/SCALE options.
         */
        UNSIGNED_SHORT(20),
        /**
         * Not a real fits type, use {@link BitPixDataTypes#SIGNED_INT_32} and BZERO/SCALE options.
         */
        UNSIGNED_INT_32(40),
        /**
         * Not a real fits type, use {@link BitPixDataTypes#SIGNED_LONG} and BZERO/SCALE options.
         */
        UNSIGNED_LONG(80);

        private final int value;

        BitPixDataTypes(int value) {this.value = value;}

        @Override
        public int intValue() {return value;}

        public static BitPixDataTypes fromInt(int i) {
            for (BitPixDataTypes type : values()) {
                if (i == type.intValue())
                    return type;
            }

            throw new IllegalArgumentException("Unknown Bitpix type of %d".formatted(i));
        }
    }

    /**
     * Used for reading pixel values from ImageHDUs.
     */
    /* codes for FITS table data types */
    public enum DataType implements EnumMapper.IntegerEnum {//todo put in ImageHDU?
        TBIT          (1, Byte.BYTES),
        TBYTE        (11, Byte.BYTES),
        TSBYTE       (12, Byte.BYTES),
        TLOGICAL     (14, Byte.BYTES),
        TSTRING      (16, Byte.BYTES),//todo check byteSize const.
        TUSHORT      (20, Short.BYTES),
        TSHORT       (21, Short.BYTES),
        TUINT        (30, Integer.BYTES),
        TINT         (31, Integer.BYTES),
        TULONG       (40, Long.BYTES),
        TLONG        (41, Long.BYTES),
        /** used when returning datatype of a column */
        TINT32BIT    (41, Integer.BYTES),//todo how to handle duplicate value?
        TFLOAT       (42, Integer.BYTES),
        TULONGLONG   (80, Long.BYTES),
        TLONGLONG    (81, Long.BYTES),
        TDOUBLE      (82, Long.BYTES),
        TCOMPLEX     (83, Long.BYTES),
        TDBLCOMPLEX (163, Long.BYTES);

        private final int value;
        private final int byteSize;

        DataType(int value, int byteSize) {this.value = value;
            this.byteSize = byteSize;
        }

        @Override
        public int intValue() {return value;}

        public int byteSize() {
            return byteSize;
        }

        public static DataType fromInt(int i) {
            for (DataType type : values()) {
                if (i == type.intValue())
                    return type;
            }

            throw new IllegalArgumentException("Unknown eq. DataType type of %d".formatted(i));
        }
    }

    public enum CaseSensitivity implements EnumMapper.IntegerEnum {
        CASE_SEN (1),
        CASE_INSEN   (0);

        private final int value;

        CaseSensitivity(int value) {this.value = value;}

        @Override
        public int intValue() {return value;}
    }

    /**
     * Error codes, pulled from fitsio.h
     */
    public static final class ErrorCodes {
        /** create disk file, without extended filename syntax */
        public static final int CREATE_DISK_FILE = -106;
        /** open disk file, without extended filename syntax */
        public static final int OPEN_DISK_FILE   = -105;
        /** move to 1st image when opening file */
        public static final int SKIP_TABLE       = -104;
        /** move to 1st table when opening file */
        public static final int SKIP_IMAGE       = -103;
        /** skip null primary array when opening file */
        public static final int SKIP_NULL_PRIMARY = -102;
        /** use memory buffer when opening file */
        public static final int USE_MEM_BUFF     = -101;
        /** overflow during datatype conversion */
        public static final int OVERFLOW_ERR      = -11;
        /** used in ffiimg to insert new primary array */
        public static final int PREPEND_PRIMARY    = -9;
        /** input and output files are the same */
        public static final int SAME_FILE         = 101;
        /** tried to open too many FITS files */
        public static final int TOO_MANY_FILES    = 103;
        /** could not open the named file */
        public static final int FILE_NOT_OPENED   = 104;
        /** could not create the named file */
        public static final int FILE_NOT_CREATED  = 105;
        /** error writing to FITS file */
        public static final int WRITE_ERROR       = 106;
        /** tried to move past end of file */
        public static final int END_OF_FILE       = 107;
        /** error reading from FITS file */
        public static final int READ_ERROR        = 108;
        /** could not close the file */
        public static final int FILE_NOT_CLOSED   = 110;
        /** array dimensions exceed internal limit */
        public static final int ARRAY_TOO_BIG     = 111;
        /** Cannot write to readonly file */
        public static final int READONLY_FILE     = 112;
        /** Could not allocate memory */
        public static final int MEMORY_ALLOCATION = 113;
        /** invalid fitsfile pointer */
        public static final int BAD_FILEPTR       = 114;
        /** NULL input pointer to routine */
        public static final int NULL_INPUT_PTR    = 115;
        /** error seeking position in file */
        public static final int SEEK_ERROR        = 116;
        /** bad value for file download timeout setting */
        public static final int BAD_NETTIMEOUT    = 117;
        /** invalid URL prefix on file name */
        public static final int BAD_URL_PREFIX    = 121;
        /** tried to register too many IO drivers */
        public static final int TOO_MANY_DRIVERS  = 122;
        /** driver initialization failed */
        public static final int DRIVER_INIT_FAILED = 123;
        /** matching driver is not registered */
        public static final int NO_MATCHING_DRIVER = 124;
        /** failed to parse input file URL */
        public static final int URL_PARSE_ERROR    = 125;
        /** failed to parse input file URL */
        public static final int RANGE_PARSE_ERROR  = 126;

        public static final int SHARED_ERRBASE = (150);
        public static final int SHARED_BADARG = (SHARED_ERRBASE + 1);
        public static final int SHARED_NULPTR = (SHARED_ERRBASE + 2);
        public static final int SHARED_TABFULL = (SHARED_ERRBASE + 3);
        public static final int SHARED_NOTINIT = (SHARED_ERRBASE + 4);
        public static final int SHARED_IPCERR = (SHARED_ERRBASE + 5);
        public static final int SHARED_NOMEM = (SHARED_ERRBASE + 6);
        public static final int SHARED_AGAIN = (SHARED_ERRBASE + 7);
        public static final int SHARED_NOFILE = (SHARED_ERRBASE + 8);
        public static final int SHARED_NORESIZE = (SHARED_ERRBASE + 9);

        /** label already contains keywords */
        public static final int HEADER_NOT_EMPTY  = 201;
        /** keyword not found in label */
        public static final int KEY_NO_EXIST      = 202;
        /** keyword record number is out of bounds */
        public static final int KEY_OUT_BOUNDS    = 203;
        /** keyword value field is blank */
        public static final int VALUE_UNDEFINED   = 204;
        /** string is missing the closing quote */
        public static final int NO_QUOTE          = 205;
        /** illegal indexed keyword name */
        public static final int BAD_INDEX_KEY     = 206;
        /** illegal character in keyword name or card */
        public static final int BAD_KEYCHAR       = 207;
        /** required keywords out of order */
        public static final int BAD_ORDER         = 208;
        /** keyword value is not a positive integer */
        public static final int NOT_POS_INT       = 209;
        /** couldn't find END keyword */
        public static final int NO_END            = 210;
        /** illegal BITPIX keyword value*/
        public static final int BAD_BITPIX     =   211;
        /** illegal NAXIS keyword value */
        public static final int BAD_NAXIS         = 212;
        /** illegal NAXISn keyword value */
        public static final int BAD_NAXES         = 213;
        /** illegal PCOUNT keyword value */
        public static final int BAD_PCOUNT        = 214;
        /** illegal GCOUNT keyword value */
        public static final int BAD_GCOUNT        = 215;
        /** illegal TFIELDS keyword value */
        public static final int BAD_TFIELDS       = 216;
        /** negative table row size */
        public static final int NEG_WIDTH         = 217;
        /** negative number of rows in table */
        public static final int NEG_ROWS          = 218;
        /** column with this name not found in table */
        public static final int COL_NOT_FOUND     = 219;
        /** illegal value of SIMPLE keyword  */
        public static final int BAD_SIMPLE        = 220;
        /** Primary array doesn't start with SIMPLE */
        public static final int NO_SIMPLE         = 221;
        /** Second keyword not BITPIX */
        public static final int NO_BITPIX         = 222;
        /** Third keyword not NAXIS */
        public static final int NO_NAXIS          = 223;
        /** Couldn't find all the NAXISn keywords */
        public static final int NO_NAXES          = 224;
        /** HDU doesn't start with XTENSION keyword */
        public static final int NO_XTENSION       = 225;
        /** the CHDU is not an ASCII table extension */
        public static final int NOT_ATABLE        = 226;
        /** the CHDU is not a binary table extension */
        public static final int NOT_BTABLE        = 227;
        /** couldn't find PCOUNT keyword */
        public static final int NO_PCOUNT         = 228;
        /** couldn't find GCOUNT keyword */
        public static final int NO_GCOUNT         = 229;
        /** couldn't find TFIELDS keyword */
        public static final int NO_TFIELDS        = 230;
        /** couldn't find TBCOLn keyword */
        public static final int NO_TBCOL          = 231;
        /** couldn't find TFORMn keyword */
        public static final int NO_TFORM          = 232;
        /** the CHDU is not an IMAGE extension */
        public static final int NOT_IMAGE         = 233;
        /** TBCOLn keyword value < 0 or > rowlength */
        public static final int BAD_TBCOL         = 234;
        /** the CHDU is not a table */
        public static final int NOT_TABLE         = 235;
        /** column is too wide to fit in table */
        public static final int COL_TOO_WIDE      = 236;
        /** more than 1 column name matches template */
        public static final int COL_NOT_UNIQUE    = 237;
        /** sum of column widths not = NAXIS1 */
        public static final int BAD_ROW_WIDTH     = 241;
        /** unrecognizable FITS extension type */
        public static final int UNKNOWN_EXT       = 251;
        /** unrecognizable FITS record */
        public static final int UNKNOWN_REC       = 252;
        /** END keyword is not blank */
        public static final int END_JUNK          = 253;
        /** Header fill area not blank */
        public static final int BAD_HEADER_FILL   = 254;
        /** Data fill area not blank or zero */
        public static final int BAD_DATA_FILL     = 255;
        /** illegal TFORM format code */
        public static final int BAD_TFORM         = 261;
        /** unrecognizable TFORM datatype code */
        public static final int BAD_TFORM_DTYPE   = 262;
        /** illegal TDIMn keyword value */
        public static final int BAD_TDIM          = 263;
        /** invalid BINTABLE heap address */
        public static final int BAD_HEAP_PTR      = 264
                ;
        /** HDU number < 1 or > MAXHDU */
        public static final int BAD_HDU_NUM       = 301;
        /** column number < 1 or > tfields */
        public static final int BAD_COL_NUM       = 302;
        /** tried to move before beginning of file  */
        public static final int NEG_FILE_POS      = 304;
        /** tried to read or write negative bytes */
        public static final int NEG_BYTES         = 306;
        /** illegal starting row number in table */
        public static final int BAD_ROW_NUM       = 307;
        /** illegal starting element number in vector */
        public static final int BAD_ELEM_NUM      = 308;
        /** this is not an ASCII string column */
        public static final int NOT_ASCII_COL     = 309;
        /** this is not a logical datatype column */
        public static final int NOT_LOGICAL_COL   = 310;
        /** ASCII table column has wrong format */
        public static final int BAD_ATABLE_FORMAT = 311;
        /** Binary table column has wrong format */
        public static final int BAD_BTABLE_FORMAT = 312;
        /** null value has not been defined */
        public static final int NO_NULL           = 314;
        /** this is not a variable length column */
        public static final int NOT_VARI_LEN      = 317;
        /** illegal number of dimensions in array */
        public static final int BAD_DIMEN         = 320;
        /** first pixel number greater than last pixel */
        public static final int BAD_PIX_NUM       = 321;
        /** illegal BSCALE or TSCALn keyword = 0 */
        public static final int ZERO_SCALE        = 322;
        /** illegal axis length < 1 */
        public static final int NEG_AXIS          = 323;

        public static final int  NOT_GROUP_TABLE    =     340;
        public static final int  HDU_ALREADY_MEMBER   =   341;
        public static final int  MEMBER_NOT_FOUND   =     342;
        public static final int  GROUP_NOT_FOUND    =     343;
        public static final int  BAD_GROUP_ID     =       344;
        public static final int  TOO_MANY_HDUS_TRACKED =  345;
        public static final int  HDU_ALREADY_TRACKED  =   346;
        public static final int  BAD_OPTION       =       347;
        public static final int  IDENTICAL_POINTERS  =    348;
        public static final int  BAD_GROUP_ATTACH    =    349;
        public static final int  BAD_GROUP_DETACH    =    360;

        /** bad int to formatted string conversion */
        public static final int BAD_I2C           = 401;
        /** bad float to formatted string conversion */
        public static final int BAD_F2C           = 402;
        /** can't interprete keyword value as integer */
        public static final int BAD_INTKEY        = 403;
        /** can't interprete keyword value as logical */
        public static final int BAD_LOGICALKEY    = 404;
        /** can't interprete keyword value as float */
        public static final int BAD_FLOATKEY      = 405;
        /** can't interprete keyword value as double */
        public static final int BAD_DOUBLEKEY     = 406;
        /** bad formatted string to int conversion */
        public static final int BAD_C2I           = 407;
        /** bad formatted string to float conversion */
        public static final int BAD_C2F           = 408;
        /** bad formatted string to double conversion */
        public static final int BAD_C2D           = 409;
        /** bad keyword datatype code */
        public static final int BAD_DATATYPE      = 410;
        /** bad number of decimal places specified */
        public static final int BAD_DECIM         = 411;
        /** overflow during datatype conversion */
        public static final int NUM_OVERFLOW      = 412
                ;
        /** error in imcompress routines */
        public static final int DATA_COMPRESSION_ERR = 413;
        /** error in imcompress routines */
        public static final int DATA_DECOMPRESSION_ERR = 414;
        /** compressed tile doesn't exist */
        public static final int NO_COMPRESSED_TILE  = 415
                ;
        /** error in date or time conversion */
        public static final int BAD_DATE          = 420
                ;
        /** syntax error in parser expression */
        public static final int PARSE_SYNTAX_ERR  = 431;
        /** expression did not evaluate to desired type */
        public static final int PARSE_BAD_TYPE    = 432;
        /** vector result too large to return in array */
        public static final int PARSE_LRG_VECTOR  = 433;
        /** data parser failed not sent an out column */
        public static final int PARSE_NO_OUTPUT   = 434;
        /** bad data encounter while parsing column */
        public static final int PARSE_BAD_COL     = 435;
        /** Output file not of proper type          */
        public static final int PARSE_BAD_OUTPUT  = 436
                ;
        /** celestial angle too large for projection */
        public static final int ANGLE_TOO_BIG     = 501;
        /** bad celestial coordinate or pixel value */
        public static final int BAD_WCS_VAL       = 502;
        /** error in celestial coordinate calculation */
        public static final int WCS_ERROR         = 503;
        /** unsupported type of celestial projection */
        public static final int BAD_WCS_PROJ      = 504;
        /** celestial coordinate keywords not found */
        public static final int NO_WCS_KEY        = 505;
        /** approximate WCS keywords were calculated */
        public static final int APPROX_WCS_KEY    = 506
                ;
        /** special value used internally to switch off */
        public static final int NO_CLOSE_ERROR    = 999;
        /* the error message from ffclos and ffchdu */
        ;
        /*------- following error codes are used in the grparser.c file -----------*/

        public static final int NGP_ERRBASE = (360);
        public static final int NGP_OK = (0);
        /** malloc failed */
        public static final int NGP_NO_MEMORY = (NGP_ERRBASE + 0);
        /** read error from file */
        public static final int NGP_READ_ERR = (NGP_ERRBASE + 1);
        /** null pointer passed as argument */
        public static final int NGP_NUL_PTR = (NGP_ERRBASE + 2);
        /** line read seems to be empty */
        public static final int NGP_EMPTY_CURLINE = (NGP_ERRBASE + 3);
        /** cannot unread more then 1 line (or single line twice) */
        public static final int NGP_UNREAD_QUEUE_FULL = (NGP_ERRBASE + 4);
        /** too deep include file nesting (inf. loop ?) */
        public static final int NGP_INC_NESTING = (NGP_ERRBASE + 5);
        /** fopen() failed, cannot open file */
        public static final int NGP_ERR_FOPEN = (NGP_ERRBASE + 6);
        /** end of file encountered */
        public static final int NGP_EOF = (NGP_ERRBASE + 7);
        /** bad arguments passed */
        public static final int NGP_BAD_ARG = (NGP_ERRBASE + 8);
        /** token not expected here */
        public static final int NGP_TOKEN_NOT_EXPECT = (NGP_ERRBASE + 9);
    }
}
