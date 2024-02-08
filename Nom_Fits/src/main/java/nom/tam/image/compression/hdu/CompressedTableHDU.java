package nom.tam.image.compression.hdu;

import nom.tam.fits.*;
import nom.tam.fits.header.Compression;
import nom.tam.fits.header.Standard;
import nom.tam.util.Cursor;
import nom.tam.util.type.ElementType;

import static nom.tam.fits.header.Compression.ZTABLE;

/**
 * A header-data unit (HDU) containing a compressed binary table. A compressed table is still a binary table but with
 * some additional constraints. The original table is divided into groups of rows (tiles) and each tile is compressed on
 * its own. The compressed data is then stored in the 3 data columns of this binary table (compressed, gzipped and
 * uncompressed) depending on the compression type used in the tile. Additional data columns may contain specific
 * compression options for each tile (i.e. compressed table row) individually. Table keywords, which conflict with those
 * in the original table are 'saved' under standard alternative names, so they may be restored with the original table
 * as appropriate.
 * <p>
 * Compressing a table HDU is typically a two-step process:
 * </p>
 * <ol>
 * <li>Create a <code>CompressedTableHDU</code>, e.g. with {@link #fromBinaryTableHDU(BinaryTableHDU, int, String...)},
 * using the specified number of table rows per compressed block, and compression algorithm(s)</li>
 * <li>Perform the compression via {@link #compress()}</li>
 * </ol>
 * <p>
 * For example to compress a binary table:
 * </p>
 * 
 * <pre>
 *   BinaryTableHDU table = ...
 *   
 *   // 1. Create compressed HDU with the
 *   CompressedTableHDU compressed = CompressedTableHDU.fromBinaryTableHDU(table, 4, Compression.ZCMPTYPE_RICE_1);
 *   
 *   // 2. Perform the compression.
 *   compressed.compress();
 * </pre>
 * <p>
 * which of course you can compact into a single line as:
 * </p>
 * 
 * <pre>
 * CompressedTableHDU compressed = CompressedTableHDU.fromBinaryTableHDU(table, 4, Compression.ZCMPTYPE_RICE_1).compress();
 * </pre>
 * <p>
 * The two step process (as opposed to a single-step one) was probably chosen because it mimics that of
 * {@link CompressedImageHDU}, where further configuration steps may be inserted in-between. After the compression, the
 * compressed table HDU can be handled just like any other HDU, and written to a file or stream, for example.
 * </p>
 * <p>
 * The reverse process is simply via the {@link #asBinaryTableHDU()} method. E.g.:
 * </p>
 * 
 * <pre>
 *    CompressedTableHDU compressed = ...
 *    BinaryTableHDU table = compressed.asBinaryTableHDU();
 * </pre>
 *
 * @see CompressedTableData
 */
@SuppressWarnings("deprecation")
public class CompressedTableHDU extends BinaryTableHDU {

    /**
     * Prepare a compressed binary table HDU for the specified binary table. When the tile row size is specified with
     * -1, the value will be set ti the number of rows in the table. The table will be compressed in "rows" that are
     * defined by the tile size. Next step would be to set the compression options into the HDU and then compress it.
     *
     * @param  binaryTableHDU              the binary table to compress
     * @param  tileRows                    the number of rows that should be compressed per tile.
     * @param  columnCompressionAlgorithms the compression algorithms to use for the columns (optional default
     *                                         compression will be used if a column has no compression specified). You
     *                                         should typically use one or more of the enum values defined in
     *                                         {@link Compression}.
     *
     * @return                             the prepared compressed binary table HDU.
     *
     * @throws FitsException               if the binary table could not be used to create a compressed binary table.
     */
    public static CompressedTableHDU fromBinaryTableHDU(BinaryTableHDU binaryTableHDU, int tileRows,
            String... columnCompressionAlgorithms) throws FitsException {
        Header header = new Header();
        CompressedTableData compressedData = new CompressedTableData();

        int rowsPerTile = tileRows > 0 ? tileRows : binaryTableHDU.getData().getNRows();
        compressedData.setRowsPerTile(rowsPerTile);
        compressedData.fillHeader(header);

        Cursor<String, HeaderCard> headerIterator = header.iterator();
        Cursor<String, HeaderCard> imageIterator = binaryTableHDU.getHeader().iterator();
        while (imageIterator.hasNext()) {
            HeaderCard card = imageIterator.next();
            CompressedCard.restore(card, headerIterator);
        }
        CompressedTableHDU compressedImageHDU = new CompressedTableHDU(header, compressedData);
        compressedData.setColumnCompressionAlgorithms(columnCompressionAlgorithms);
        compressedData.prepareUncompressedData(binaryTableHDU.getData().getData());
        return compressedImageHDU;
    }

    /**
     * Check that this HDU has a valid header for this type.
     * 
     * @deprecated     (<i>for internal use</i>) Will reduce visibility in the future
     *
     * @param      hdr header to check
     *
     * @return         <CODE>true</CODE> if this HDU has a valid header.
     */
    @Deprecated
    public static boolean isHeader(Header hdr) {
        return hdr.getBooleanValue(ZTABLE, false);
    }

    /**
     * @deprecated (<i>for internal use</i>) Will reduce visibility in the future
     */
    @Deprecated
    public static CompressedTableData manufactureData(Header hdr) throws FitsException {
        return new CompressedTableData(hdr);
    }

    /**
     * Creates an new compressed table HDU with the specified header and compressed data.
     * 
     * @param hdr   the header
     * @param datum the compressed table data. The data may not be actually compressed at this point, int which case you
     *                  may need to call {@link #compress()} before writing the new compressed HDU to a stream.
     * 
     * @see         #compress()
     */
    public CompressedTableHDU(Header hdr, CompressedTableData datum) {
        super(hdr, datum);
    }

    /**
     * Restores the original binary table HDU by decompressing the data contained in this compresed table HDU.
     * 
     * @return               The uncompressed binary table HDU.
     * 
     * @throws FitsException If there was an issue with the decompression.
     * 
     * @see                  #asBinaryTableHDU(int, int)
     * @see                  #fromBinaryTableHDU(BinaryTableHDU, int, String...)
     */
    public BinaryTableHDU asBinaryTableHDU() throws FitsException {
        Header header = getTableHeader();
        BinaryTable data = BinaryTableHDU.manufactureData(header);
        BinaryTableHDU tableHDU = new BinaryTableHDU(header, data);
        getData().asBinaryTable(data, getHeader(), header);
        return tableHDU;
    }

    /**
     * Returns the number of table rows that are compressed in each table tile. This may be useful for figuring out what
     * tiles to decompress, e.g. via {@link #asBinaryTableHDU(int, int)}, when wanting to access select table rows only.
     * This value is stored under the FITS keyword ZTILELEN in the compressed header. Thus, this method simply provides
     * a user-friendly way to access it. Note that the last tile may contain fewer rows than the value indicated by this
     * 
     * @return               the number of table rows that are compressed into a tile.
     * 
     * @throws FitsException if the compressed header does not contain the required ZTILELEN keyword, or it is &lt;= 0.
     * 
     * @see                  #asBinaryTableHDU(int, int)
     * 
     * @since                1.19
     */
    public int getTileRows() throws FitsException {
        int n = getHeader().getIntValue(Compression.ZTILELEN, -1);
        if (n <= 0) {
            throw new FitsException("imnvalid or missing ZTILELEN header keyword");
        }
        return n;
    }

    /**
     * Returns the number of compressed tiles contained in this HDU.
     * 
     * @return the number of compressed tiles in this table. It is the same as the NAXIS2 value of the header, which is
     *             also returned by {@link #getNRows()} for this compressed table.
     * 
     * @see    #getTileRows()
     * 
     * @since  1.19
     */
    public int getTileCount() {
        return getNRows();
    }

    /**
     * Restores a section of the original binary table HDU by decompressing a selected range of compressed table tiles.
     * The returned section will start at row index <code>fromTile * getTileRows()</code> of the full table.
     * 
     * @param  fromTile                 Java index of first tile to decompress
     * @param  toTile                   Java index of last tile to decompress
     * 
     * @return                          The uncompressed binary table HDU from the selected compressed tiles.
     * 
     * @throws IllegalArgumentException If the tile range is out of bounds
     * @throws FitsException            If there was an issue with the decompression.
     * 
     * @see                             #getTileRows()
     * @see                             #getTileCount()
     * @see                             #asBinaryTableHDU()
     * @see                             #fromBinaryTableHDU(BinaryTableHDU, int, String...)
     */
    public BinaryTableHDU asBinaryTableHDU(int fromTile, int toTile) throws FitsException, IllegalArgumentException {
        Header header = getTableHeader();
        int tileSize = getTileRows();

        if (fromTile < 0 || toTile > getTileCount() || toTile <= fromTile) {
            throw new IllegalArgumentException(
                    "illegal tile range [" + fromTile + ", " + toTile + "] for " + getTileCount() + " tiles");
        }

        // Set the correct number of rows
        int rows = getHeader().getIntValue(Compression.ZNAXISn.n(2));
        header.addValue(Standard.NAXIS2, Integer.min(rows, toTile * tileSize) - fromTile * tileSize);

        BinaryTable data = BinaryTableHDU.manufactureData(header);
        BinaryTableHDU tableHDU = new BinaryTableHDU(header, data);
        getData().asBinaryTable(data, getHeader(), header, fromTile);

        return tableHDU;
    }

    /**
     * Restores a section of the original binary table HDU by decompressing a single compressed table tile. The returned
     * section will start at row index <code>tile * getTileRows()</code> of the full table.
     * 
     * @param  tile                     Java index of the table tile to decompress
     * 
     * @return                          The uncompressed binary table HDU from the selected compressed tile.
     * 
     * @throws IllegalArgumentException If the tile index is out of bounds
     * @throws FitsException            If there was an issue with the decompression.
     * 
     * @see                             #asBinaryTableHDU(int, int)
     * @see                             #getTileRows()
     * @see                             #getTileCount()
     */
    public final BinaryTableHDU asBinaryTableHDU(int tile) throws FitsException, IllegalArgumentException {
        return asBinaryTableHDU(tile, tile + 1);
    }

    /**
     * Returns a particular section of a decompressed data column.
     * 
     * @param  col                      the Java column index
     * 
     * @return                          The uncompressed column data as an array.
     * 
     * @throws IllegalArgumentException If the tile range is out of bounds
     * @throws FitsException            If there was an issue with the decompression.
     * 
     * @see                             #getColumnData(int, int, int)
     * @see                             #asBinaryTableHDU()
     * @see                             #fromBinaryTableHDU(BinaryTableHDU, int, String...)
     */
    public Object getColumnData(int col) throws FitsException, IllegalArgumentException {
        return getColumnData(col, 0, getTileCount());
    }

    /**
     * Returns a particular section of a decompressed data column.
     * 
     * @param  col                      the Java column index
     * @param  fromTile                 the Java index of first tile to decompress
     * @param  toTile                   the Java index of last tile to decompress
     * 
     * @return                          The uncompressed column data segment as an array.
     * 
     * @throws IllegalArgumentException If the tile range is out of bounds
     * @throws FitsException            If there was an issue with the decompression.
     * 
     * @see                             #getColumnData(int)
     * @see                             #asBinaryTableHDU()
     * @see                             #fromBinaryTableHDU(BinaryTableHDU, int, String...)
     */
    public Object getColumnData(int col, int fromTile, int toTile) throws FitsException, IllegalArgumentException {
        return getData().getColumnData(col, fromTile, toTile, getHeader(), getTableHeader());
    }

    /**
     * Performs the actual compression with the selected algorithm(s) and options. When creating a compressed table HDU,
     * e.g using the {@link #fromBinaryTableHDU(BinaryTableHDU, int, String...)} method, the HDU is merely prepared but
     * without actually performing the compression, and this method will have to be called to actually perform the
     * compression. The design would allow for setting options between creation and compressing, but in this case there
     * is really nothing of the sort.
     * 
     * @return               itself
     * 
     * @throws FitsException if the compression could not be performed
     * 
     * @see                  #fromBinaryTableHDU(BinaryTableHDU, int, String...)
     */
    public CompressedTableHDU compress() throws FitsException {
        getData().compress(getHeader());
        return this;
    }

    /**
     * Obtain a header representative of a decompressed TableHDU.
     *
     * @return                     Header with decompressed cards.
     *
     * @throws HeaderCardException if the card could not be copied
     *
     * @since                      1.18
     */
    public Header getTableHeader() throws HeaderCardException {
        Header header = new Header();

        header.addValue(Standard.XTENSION, Standard.XTENSION_BINTABLE);
        header.addValue(Standard.BITPIX, ElementType.BYTE.bitPix());
        header.addValue(Standard.NAXIS, 2);

        Cursor<String, HeaderCard> tableIterator = header.iterator();
        Cursor<String, HeaderCard> iterator = getHeader().iterator();

        while (iterator.hasNext()) {
            CompressedCard.backup(iterator.next(), tableIterator);
        }

        return header;
    }

    @Override
    public CompressedTableData getData() {
        return (CompressedTableData) super.getData();
    }

}
