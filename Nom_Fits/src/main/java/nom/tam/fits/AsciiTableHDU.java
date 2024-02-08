package nom.tam.fits;

import nom.tam.fits.header.IFitsHeader;
import nom.tam.fits.header.Standard;
import nom.tam.util.ArrayFuncs;
import nom.tam.util.Cursor;

import java.io.PrintStream;

import static nom.tam.fits.header.Standard.*;

/**
 * ASCII table header/data unit. ASCII table HDUs were desgined for human readability, e.g. on a console, without any
 * special tools. However, they are far less flexible or compact than {@link BinaryTableHDU}. As such, users are
 * generally discouraged from using this type of HDU to encapsulate FITS table data.
 * {@link FitsFactory#setUseAsciiTables(boolean)} can be toggled to adjust whether {@link Fits#makeHDU(Object)} or
 * similar methods should construct ASCII tables when possible. (The default setting is to produce binary tables
 * always.)
 * 
 * @see AsciiTable
 * @see BinaryTableHDU
 */
public class AsciiTableHDU extends TableHDU<AsciiTable> {

    /**
     * The standard column stems for an ASCII table. Note that TBCOL is not included here -- it needs to be handled
     * specially since it does not simply shift.
     */
    private static final IFitsHeader[] KEY_STEMS = {TFORMn, TZEROn, TNULLn, TTYPEn, TUNITn};

    /**
     * Create an ASCII table header/data unit.
     * 
     * @deprecated   (<i>for internal use</i>) Its visibility should be reduced to package level in the future.
     *
     * @param      h the template specifying the ASCII table.
     * @param      d the FITS data structure containing the table data.
     */
    public AsciiTableHDU(Header h, AsciiTable d) {
        super(h, d);
    }

    @Override
    protected final String getCanonicalXtension() {
        return XTENSION_ASCIITABLE;
    }

    /**
     * @deprecated               (<i>for internal use</i>) Use {@link AsciiTable#fromColumnMajor(Object[])} instead.
     *                               Will reduce visibility in the future
     *
     * @return                   a ASCII table data structure from an array of objects representing the columns.
     *
     * @param      o             the array of object to create the ASCII table
     *
     * @throws     FitsException if the table could not be created.
     */
    @Deprecated
    public static AsciiTable encapsulate(Object o) throws FitsException {
        return AsciiTable.fromColumnMajor((Object[]) o);
    }

    /**
     * @deprecated   (<i>for internal use</i>) Will reduce visibility in the future
     *
     * @return       true if this data is usable as an ASCII table.
     *
     * @param      o object representing the data
     */
    @Deprecated
    public static boolean isData(Object o) {

        if (o instanceof Object[]) {
            for (Object element : (Object[]) o) {
                if (!(element instanceof String[]) && //
                        !(element instanceof int[]) && //
                        !(element instanceof long[]) && //
                        !(element instanceof float[]) && //
                        !(element instanceof double[])) {
                    return false;
                }
            }
            return true;
        }

        return false;
    }

    /**
     * Check that this is a valid ascii table header.
     *
     * @deprecated        (<i>for internal use</i>) Will reduce visibility in the future
     *
     * @param      header to validate.
     *
     * @return            <CODE>true</CODE> if this is an ascii table header.
     */
    @Deprecated
    public static boolean isHeader(Header header) {
        String xtension = header.getStringValue(XTENSION);
        xtension = xtension == null ? "" : xtension.trim();
        return XTENSION_ASCIITABLE.equals(xtension);
    }

    /**
     * Prepares a data object into which the actual data can be read from an input subsequently or at a later time.
     *
     * @deprecated               (<i>for internal use</i>) Will reduce visibility in the future
     *
     * @param      hdr           The FITS header that describes the data
     *
     * @return                   A data object that support reading content from a stream.
     *
     * @throws     FitsException if the data could not be prepared to prescriotion.
     */
    @Deprecated
    public static AsciiTable manufactureData(Header hdr) throws FitsException {
        return new AsciiTable(hdr);
    }

    /**
     * @deprecated               (<i>for internal use</i>) Will reduce visibility in the future
     *
     * @return                   a created header to match the input data.
     *
     * @param      d             data to create a header for
     *
     * @throws     FitsException if the header could not b e created
     */
    @Deprecated
    public static Header manufactureHeader(Data d) throws FitsException {
        Header hdr = new Header();
        d.fillHeader(hdr);
        hdr.iterator();
        return hdr;
    }

    @SuppressWarnings("deprecation")
    @Override
    public int addColumn(Object newCol) throws FitsException {
        Standard.context(AsciiTable.class);
        myData.addColumn(newCol);
        // Move the iterator to point after all the data describing
        // the previous column.

        Cursor<String, HeaderCard> iter = myHeader.positionAfterIndex(TBCOLn, myData.getNCols());

        int rowlen = myData.addColInfo(getNCols() - 1, iter);
        int oldRowlen = myHeader.getIntValue(NAXIS1);
        myHeader.setNaxis(1, rowlen + oldRowlen);

        super.addColumn(newCol);
        Standard.context(null);
        return getNCols();
    }

    @Override
    protected IFitsHeader[] columnKeyStems() {
        return KEY_STEMS;
    }

    @Override
    public void info(PrintStream stream) {
        stream.println("ASCII Table:");
        stream.println("  Header:");
        stream.println("    Number of fields:" + myHeader.getIntValue(TFIELDS));
        stream.println("    Number of rows:  " + myHeader.getIntValue(NAXIS2));
        stream.println("    Length of row:   " + myHeader.getIntValue(NAXIS1));
        stream.println("  Data:");
        Object[] data = (Object[]) getKernel();
        for (int i = 0; i < getNCols(); i++) {
            stream.println("      " + i + ":" + ArrayFuncs.arrayDescription(data[i]));
        }
    }

    /**
     * Checks if a table entry is <code>null</code>
     * 
     * @param  row row index of the element
     * @param  col column index of the element
     *
     * @return     <code>true</code> if the specified element is <code>null</code>
     * 
     * @see        #setNull(int, int, boolean)
     * @see        AsciiTable#isNull(int, int)
     */
    public boolean isNull(int row, int col) {
        return myData.isNull(row, col);
    }

    /**
     * Mark an entry as null.
     *
     * @param row  row index of the element
     * @param col  column index of the element
     * @param flag set to null or not
     * 
     * @see        #isNull(int, int)
     * @see        AsciiTable#setNull(int, int, boolean)
     */
    public void setNull(int row, int col, boolean flag) {

        if (flag) {
            String nullStr = myHeader.getStringValue(TNULLn.n(col + 1));
            if (nullStr == null) {
                setNullString(col, "NULL");
            }
        }
        myData.setNull(row, col, flag);
    }

    /**
     * Set the null string for a column.
     *
     * @param  col                      the column index
     * @param  newNull                  the String representing null
     *
     * @throws IllegalArgumentException if the string argument contains characters that are not allowed in FITS headers.
     *                                      That is if it contains characters outside the range of 0x20 thru 0x7E.
     */
    public void setNullString(int col, String newNull) throws IllegalArgumentException {
        myHeader.positionAfterIndex(TBCOLn, col + 1);
        HeaderCard card = HeaderCard.create(TNULLn.n(col + 1), newNull);
        myHeader.deleteKey(card.getKey());
        myHeader.addLine(card);
        myData.setNullString(col, newNull);
    }

}
