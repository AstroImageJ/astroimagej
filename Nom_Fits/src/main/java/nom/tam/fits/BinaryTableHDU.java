package nom.tam.fits;

import nom.tam.fits.header.IFitsHeader;
import nom.tam.fits.header.Standard;
import nom.tam.util.ArrayDataOutput;
import nom.tam.util.ArrayFuncs;

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

    @Override
    protected final String getCanonicalXtension() {
        return Standard.XTENSION_BINTABLE;
    }

    /**
     * @deprecated               (<i>for internal use</i>) Will reduce visibility in the future
     *
     * @return                   Encapsulate data in a BinaryTable data type
     *
     * @param      o             data to encapsulate
     *
     * @throws     FitsException if the type of the data is not usable as data
     */
    @Deprecated
    public static BinaryTable encapsulate(Object o) throws FitsException {
        if (o instanceof nom.tam.util.ColumnTable) {
            return new BinaryTable((nom.tam.util.ColumnTable<?>) o);
        }
        if (o instanceof Object[][]) {
            return new BinaryTable((Object[][]) o);
        }
        if (o instanceof Object[]) {
            return new BinaryTable((Object[]) o);
        }
        throw new FitsException("Unable to encapsulate object of type:" + o.getClass().getName() + " as BinaryTable");
    }

    /**
     * Check if this data object is consistent with a binary table.
     *
     * @param      o a column table object, an Object[][], or an Object[]. This routine doesn't check that the
     *                   dimensions of arrays are properly consistent.
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
        myData.addColumn(data);
        myData.pointToColumn(getNCols() - 1, myHeader);
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
        stream.println("          Heap size is: " + myData.getHeapSize() + " bytes");

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
     * Checks if a column contains complex-valued data (rather than just regular float or double arrays)
     * 
     * @param  index the column index
     * 
     * @return       <code>true</code> if the column contains complex valued data (as floats or doubles), otherwise
     *                   <code>false</code>
     * 
     * @since        1.18
     * 
     * @see          BinaryTable#isComplexColumn(int)
     * @see          #setComplexColumn(int)
     */
    public final boolean isComplexColumn(int index) {
        return myData.isComplexColumn(index);
    }

    /**
     * Convert a column in the table to complex. Only tables with appropriate types and dimensionalities can be
     * converted. It is legal to call this on a column that is already complex.
     *
     * @param  index         The 0-based index of the column to be converted.
     *
     * @return               Whether the column can be converted
     *
     * @throws FitsException if the header could not be adapted
     * 
     * @see                  BinaryTableHDU#setComplexColumn(int)
     */
    public boolean setComplexColumn(int index) throws FitsException {

        if (!myData.setComplexColumn(index)) {
            return false;
        }

        Standard.context(BinaryTable.class);

        // No problem with the data. Make sure the header
        // is right.
        BinaryTable.ColumnDesc colDesc = myData.getDescriptor(index);
        int dim = 1;
        String tdim = "";
        String sep = "";
        // Don't loop over all values.
        // The last is the [2] for the complex data.
        int[] dimens = colDesc.getDimens();
        for (int i = 0; i < dimens.length - 1; i++) {
            dim *= dimens[i];
            tdim = dimens[i] + sep + tdim;
            sep = ",";
        }
        String suffix = "C"; // For complex
        // Update the TFORMn keyword.

        if (colDesc.getBase() == double.class) {
            suffix = "M";
        }
        // Worry about variable length columns.
        String prefix = "";
        if (myData.getDescriptor(index).isVarying()) {
            prefix = "P";
            dim = 1;
            if (myData.getDescriptor(index).isLongVary()) {
                prefix = "Q";
            }
        }
        // Now update the header.
        myHeader.card(TFORMn.n(index + 1)).value(dim + prefix + suffix).comment("converted to complex");
        if (tdim.length() > 0) {
            myHeader.addValue(TDIMn.n(index + 1), "(" + tdim + ")");
        } else {
            // Just in case there used to be a TDIM card that's no longer
            // needed.
            myHeader.deleteKey(TDIMn.n(index + 1));
        }

        Standard.context(null);
        return true;
    }

    // Need to tell header about the Heap before writing.
    @Override
    public void write(ArrayDataOutput ado) throws FitsException {

        int oldSize = myHeader.getIntValue(PCOUNT);
        if (oldSize != myData.getHeapSize()) {
            myHeader.addValue(PCOUNT, myData.getHeapSize());
        }

        if (myHeader.getIntValue(PCOUNT) == 0) {
            myHeader.deleteKey(THEAP);
        } else {
            myHeader.getIntValue(TFIELDS);
            int offset = myHeader.getIntValue(NAXIS1) * myHeader.getIntValue(NAXIS2) + myData.getHeapOffset();
            myHeader.addValue(THEAP, offset);
        }

        super.write(ado);
    }
}
