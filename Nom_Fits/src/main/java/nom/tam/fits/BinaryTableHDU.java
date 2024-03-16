package nom.tam.fits;

import nom.tam.fits.header.IFitsHeader;
import nom.tam.fits.header.Standard;
import nom.tam.util.ArrayDataOutput;
import nom.tam.util.ArrayFuncs;
import nom.tam.util.ColumnTable;
import nom.tam.util.Cursor;

import java.io.PrintStream;

import static nom.tam.fits.header.Standard.*;

/**
 * Binary table header/data unit.
 * 
 * @see BinaryTable
 * @see AsciiTableHDU
 */
@SuppressWarnings("deprecation")
public class BinaryTableHDU extends TableHDU<BinaryTable> {

    /** The standard column keywords for a binary table. */
    private static final IFitsHeader[] KEY_STEMS = {TTYPEn, TFORMn, TUNITn, TNULLn, TSCALn, TZEROn, TDISPn, TDIMn};

    /**
     * Creates a new binary table HDU from the specified FITS header and associated table data
     * 
     * @deprecated       (<i>for internal use</i>) Its visibility should be reduced to package level in the future.
     * 
     * @param      hdr   the FITS header describing the data and any user-specific keywords
     * @param      datum the corresponding data object
     */
    public BinaryTableHDU(Header hdr, BinaryTable datum) {
        super(hdr, datum);
    }

    /**
     * Wraps the specified table in an HDU, creating a header for it with the essential table description. Users may
     * want to complete the table description with optional FITS keywords such as <code>TTYPEn</code>,
     * <code>TUNITn</code> etc. It is strongly recommended that the table structure (rows or columns) isn't altered
     * after the table is encompassed in an HDU, since there is no guarantee that the header description will be kept in
     * sync.
     * 
     * @param  tab           the binary table to wrap into a new HDU
     * 
     * @return               A new HDU encompassing and describing the supplied table.
     * 
     * @throws FitsException if the table structure is invalid, and cannot be described in a header (should never really
     *                           happen, but we keep the possibility open to it).
     * 
     * @see                  BinaryTable#toHDU()
     * 
     * @since                1.18
     */
    public static BinaryTableHDU wrap(BinaryTable tab) throws FitsException {
        BinaryTableHDU hdu = new BinaryTableHDU(new Header(), tab);
        tab.fillHeader(hdu.myHeader);
        return hdu;
    }

    @Override
    protected final String getCanonicalXtension() {
        return Standard.XTENSION_BINTABLE;
    }

    /**
     * @deprecated               (<i>for internal use</i>) Use {@link BinaryTable#fromColumnMajor(Object[])} or
     *                               {@link BinaryTable#fromRowMajor(Object[][])} instead. Will reduce visibility in the
     *                               future.
     *
     * @return                   Encapsulate data in a BinaryTable data type
     *
     * @param      o             data to encapsulate
     *
     * @throws     FitsException if the type of the data is not usable as data
     */
    @Deprecated
    public static BinaryTable encapsulate(Object o) throws FitsException {
        if (o instanceof ColumnTable) {
            return new BinaryTable((ColumnTable<?>) o);
        }
        if (o instanceof Object[][]) {
            return BinaryTable.fromRowMajor((Object[][]) o);
        }
        if (o instanceof Object[]) {
            return BinaryTable.fromColumnMajor((Object[]) o);
        }
        throw new FitsException("Unable to encapsulate object of type:" + o.getClass().getName() + " as BinaryTable");
    }

    /**
     * Check if this data object is consistent with a binary table.
     *
     * @param      o a column table object, an Object[][], or an Object[]. This routine doesn't check that the
     *                   dimensions of arrays are properly consistent.
     * 
     * @return       <code>true</code> if the data object can be represented as a FITS binary table, otherwise
     *                   <code>false</code>.
     *
     * @deprecated   (<i>for internal use</i>) Will reduce visibility in the future
     */
    @Deprecated
    public static boolean isData(Object o) {
        return o instanceof nom.tam.util.ColumnTable || o instanceof Object[][] || o instanceof Object[];
    }

    /**
     * Check that this is a valid binary table header.
     *
     * @deprecated        (<i>for internal use</i>) Will reduce visibility in the future
     *
     * @param      header to validate.
     *
     * @return            <CODE>true</CODE> if this is a binary table header.
     */
    @Deprecated
    public static boolean isHeader(Header header) {
        String xten = header.getStringValue(XTENSION);
        if (xten == null) {
            return false;
        }
        xten = xten.trim();
        return xten.equals(Standard.XTENSION_BINTABLE) || xten.equals("A3DTABLE");
    }

    /**
     * Prepares a data object into which the actual data can be read from an input subsequently or at a later time.
     *
     * @deprecated               (<i>for internal use</i>) Will reduce visibility in the future
     *
     * @param      header        The FITS header that describes the data
     *
     * @return                   A data object that support reading content from a stream.
     *
     * @throws     FitsException if the data could not be prepared to prescriotion.
     */
    @Deprecated
    public static BinaryTable manufactureData(Header header) throws FitsException {
        return new BinaryTable(header);
    }

    /**
     * @deprecated               (<i>for internal use</i>) Will reduce visibility in the future
     *
     * @return                   a newly created binary table HDU from the supplied data.
     *
     * @param      data          the data used to build the binary table. This is typically some kind of array of
     *                               objects.
     *
     * @throws     FitsException if there was a problem with the data.
     */
    @Deprecated
    public static Header manufactureHeader(Data data) throws FitsException {
        Header hdr = new Header();
        data.fillHeader(hdr);
        return hdr;
    }

    @Override
    public int addColumn(Object data) throws FitsException {
        int n = myData.addColumn(data);
        myHeader.addValue(Standard.NAXISn.n(1), myData.getRowBytes());
        Cursor<String, HeaderCard> c = myHeader.iterator();
        c.end();
        myData.fillForColumn(c, n - 1);
        return super.addColumn(data);
    }

    /**
     * For internal use. Returns the FITS header key stems to use for describing binary tables.
     * 
     * @return an array of standatd header colum knetwords stems.
     */
    protected static IFitsHeader[] binaryTableColumnKeyStems() {
        return KEY_STEMS;
    }

    @Override
    protected IFitsHeader[] columnKeyStems() {
        return BinaryTableHDU.KEY_STEMS;
    }

    @Override
    public void info(PrintStream stream) {
        stream.println("  Binary Table");
        stream.println("      Header Information:");

        int nhcol = myHeader.getIntValue(TFIELDS, -1);
        int nrow = myHeader.getIntValue(NAXIS2, -1);
        int rowsize = myHeader.getIntValue(NAXIS1, -1);

        stream.print("          " + nhcol + " fields");
        stream.println(", " + nrow + " rows of length " + rowsize);

        for (int i = 1; i <= nhcol; i++) {
            stream.print("           " + i + ":");
            prtField(stream, "Name", TTYPEn.n(i).key());
            prtField(stream, "Format", TFORMn.n(i).key());
            prtField(stream, "Dimens", TDIMn.n(i).key());
            stream.println("");
        }

        stream.println("      Data Information:");
        stream.println("          Number of rows=" + myData.getNRows());
        stream.println("          Number of columns=" + myData.getNCols());
        stream.println("          Heap size is: " + myData.getParameterSize() + " bytes");

        Object[] cols = myData.getFlatColumns();
        for (int i = 0; i < cols.length; i++) {
            stream.println("           " + i + ":" + ArrayFuncs.arrayDescription(cols[i]));
        }
    }

    /**
     * Check that this HDU has a valid header.
     *
     * @deprecated (<i>for internal use</i>) Will reduce visibility in the future
     *
     * @return     <CODE>true</CODE> if this HDU has a valid header.
     */
    @Deprecated
    public boolean isHeader() {
        return isHeader(myHeader);
    }

    private void prtField(PrintStream stream, String type, String field) {
        String val = myHeader.getStringValue(field);
        if (val != null) {
            stream.print(type + '=' + val + "; ");
        }
    }

    /**
     * Returns a copy of the column descriptor of a given column in this table
     * 
     * @param  col the zero-based column index
     * 
     * @return     a copy of the column's descriptor
     * 
     * @see        BinaryTable#getDescriptor(int)
     * 
     * @since      1.18
     */
    public BinaryTable.ColumnDesc getColumnDescriptor(int col) {
        return myData.getDescriptor(col);
    }

    /**
     * Converts a column from FITS logical values to bits. Null values (allowed in logical columns) will map to
     * <code>false</code>. It is legal to call this on a column that is already containing bits.
     *
     * @param  col           The zero-based index of the column to be reset.
     *
     * @return               Whether the conversion was possible. *
     * 
     * @throws FitsException if the header could not be updated
     * 
     * @since                1.18
     */
    public final boolean convertToBits(int col) throws FitsException {
        if (!myData.convertToBits(col)) {
            return false;
        }

        // Update TFORM keyword
        myHeader.getCard(Standard.TFORMn.n(col + 1)).setValue(getColumnDescriptor(col).getTFORM());

        return true;
    }

    /**
     * Convert a column in the table to complex. Only tables with appropriate types and dimensionalities can be
     * converted. It is legal to call this on a column that is already complex.
     *
     * @param  index         The zero-based index of the column to be converted.
     *
     * @return               Whether the column can be converted
     *
     * @throws FitsException if the header could not be updated
     * 
     * @see                  BinaryTableHDU#setComplexColumn(int)
     */
    public boolean setComplexColumn(int index) throws FitsException {
        if (!myData.setComplexColumn(index)) {
            return false;
        }

        // Update TFORM keyword
        myHeader.getCard(Standard.TFORMn.n(index + 1)).setValue(getColumnDescriptor(index).getTFORM());

        // Update or remove existing TDIM keyword
        if (myHeader.containsKey(Standard.TDIMn.n(index + 1))) {
            String tdim = getColumnDescriptor(index).getTDIM();
            if (tdim != null) {
                myHeader.getCard(Standard.TDIMn.n(index + 1)).setValue(tdim);
            } else {
                myHeader.deleteKey(Standard.TDIMn.n(index + 1));
            }
        }

        return true;
    }

    // Need to tell header about the Heap before writing.
    @Override
    public void write(ArrayDataOutput out) throws FitsException {
        myData.fillHeader(myHeader, false);
        super.write(out);
    }
}
