package nom.tam.image.compression.hdu;

import nom.tam.fits.BinaryTable;
import nom.tam.fits.FitsException;
import nom.tam.fits.FitsFactory;
import nom.tam.fits.Header;
import nom.tam.fits.header.Compression;
import nom.tam.fits.header.Standard;
import nom.tam.image.compression.bintable.BinaryTableTile;
import nom.tam.image.compression.bintable.BinaryTableTileCompressor;
import nom.tam.image.compression.bintable.BinaryTableTileDecompressor;
import nom.tam.util.ColumnTable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static nom.tam.fits.header.Standard.TFIELDS;
import static nom.tam.image.compression.bintable.BinaryTableTileDescription.tile;

/**
 * FITS representation of a compressed binary table. It itself is a binary table, but one in which each row represents
 * the compressed image of one or more rows of the original table.
 * 
 * @see CompressedTableHDU
 */
@SuppressWarnings("deprecation")
public class CompressedTableData extends BinaryTable {

    private int rowsPerTile;

    private List<BinaryTableTile> tiles;

    private String[] columnCompressionAlgorithms;

    /**
     * Creates a new empty compressed table data to be initialized at a later point
     */
    public CompressedTableData() {
    }

    /**
     * Creates a new compressed table data based on the prescription of the supplied header.
     * 
     * @param  header        The header that describes the compressed table
     * 
     * @throws FitsException If the header is invalid or could not be accessed.
     */
    public CompressedTableData(Header header) throws FitsException {
        super(header);
    }

    /**
     * (<i>for internal use</i>) This should only be called by {@link CompressedTableHDU}, and should have reduced
     * visibility accordingly.
     */
    @SuppressWarnings("javadoc")
    public void compress(Header header) throws FitsException {
        for (BinaryTableTile binaryTableTile : tiles) {
            binaryTableTile.execute(FitsFactory.threadPool());
        }
        for (BinaryTableTile binaryTableTile : tiles) {
            binaryTableTile.waitForResult();
            binaryTableTile.fillHeader(header);
        }
        fillHeader(header);
    }

    @Override
    public void fillHeader(Header h) throws FitsException {
        super.fillHeader(h);
        h.setNaxis(2, getData().getNRows());
        h.addValue(Compression.ZTABLE.key(), true, "this is a compressed table");
        long ztilelenValue = rowsPerTile > 0 ? rowsPerTile : h.getIntValue(Standard.NAXIS2);
        h.addValue(Compression.ZTILELEN.key(), ztilelenValue, "number of rows in each tile");
    }

    /**
     * (<i>for internal use</i>) This should only be called by {@link CompressedTableHDU}, and should have reduced
     * visibility accordingly.
     */
    @SuppressWarnings("javadoc")
    public void prepareUncompressedData(ColumnTable<SaveState> data) throws FitsException {
        int nrows = data.getNRows();
        int ncols = data.getNCols();
        if (rowsPerTile <= 0) {
            rowsPerTile = nrows;
        }
        if (columnCompressionAlgorithms.length < ncols) {
            columnCompressionAlgorithms = Arrays.copyOfRange(columnCompressionAlgorithms, 0, ncols);
        }
        tiles = new ArrayList<>();
        for (int column = 0; column < ncols; column++) {
            setCreateLongVary(true);
            addByteVaryingColumn();
            int tileIndex = 1;
            for (int rowStart = 0; rowStart < nrows; rowStart += rowsPerTile) {
                addRow(new byte[ncols][0]);
                tiles.add(new BinaryTableTileCompressor(this, data, tile()//
                        .rowStart(rowStart)//
                        .rowEnd(rowStart + rowsPerTile)//
                        .column(column)//
                        .tileIndex(tileIndex++)//
                        .compressionAlgorithm(columnCompressionAlgorithms[column])));
            }
        }
    }

    /**
     * This should only be called by {@link CompressedTableHDU}.
     */
    @SuppressWarnings("javadoc")
    protected BinaryTable asBinaryTable(BinaryTable dataToFill, Header compressedHeader, Header targetHeader)
            throws FitsException {
        int nrows = targetHeader.getIntValue(Standard.NAXIS2);
        int ncols = compressedHeader.getIntValue(TFIELDS);
        rowsPerTile = compressedHeader.getIntValue(Compression.ZTILELEN, nrows);
        tiles = new ArrayList<>();
        BinaryTable.createColumnDataFor(dataToFill);
        for (int column = 0; column < ncols; column++) {
            int tileIndex = 1;
            String compressionAlgorithm = compressedHeader.getStringValue(Compression.ZCTYPn.n(column + 1));
            for (int rowStart = 0; rowStart < nrows; rowStart += rowsPerTile) {
                BinaryTableTileDecompressor binaryTableTile = new BinaryTableTileDecompressor(this, dataToFill.getData(),
                        tile()//
                                .rowStart(rowStart)//
                                .rowEnd(rowStart + rowsPerTile)//
                                .column(column)//
                                .tileIndex(tileIndex++)//
                                .compressionAlgorithm(compressionAlgorithm));
                tiles.add(binaryTableTile);
                binaryTableTile.execute(FitsFactory.threadPool());
            }
        }
        for (BinaryTableTile binaryTableTile : tiles) {
            binaryTableTile.waitForResult();
        }
        return dataToFill;
    }

    /**
     * Returns the number of original (uncompressed) table rows that are cmopressed as a block into a single compressed
     * table row.
     * 
     * @return the number of table rows compressed together as a block.
     */
    protected int getRowsPerTile() {
        return rowsPerTile;
    }

    /**
     * This should only be called by {@link CompressedTableHDU}.
     */
    @SuppressWarnings("javadoc")
    protected void setColumnCompressionAlgorithms(String[] columnCompressionAlgorithms) {
        this.columnCompressionAlgorithms = columnCompressionAlgorithms;
    }

    /**
     * This should only be called by {@link CompressedTableHDU}.
     */
    @SuppressWarnings("javadoc")
    protected CompressedTableData setRowsPerTile(int value) {
        rowsPerTile = value;
        return this;
    }
}
