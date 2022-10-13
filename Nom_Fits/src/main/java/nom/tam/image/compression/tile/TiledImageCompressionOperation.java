package nom.tam.image.compression.tile;

/*
 * #%L
 * nom.tam FITS library
 * %%
 * Copyright (C) 1996 - 2021 nom-tam-fits
 * %%
 * This is free and unencumbered software released into the public domain.
 * 
 * Anyone is free to copy, modify, publish, use, compile, sell, or
 * distribute this software, either in source code form or as a compiled
 * binary, for any purpose, commercial or non-commercial, and by any
 * means.
 * 
 * In jurisdictions that recognize copyright laws, the author or authors
 * of this software dedicate any and all copyright interest in the
 * software to the public domain. We make this dedication for the benefit
 * of the public at large and to the detriment of our heirs and
 * successors. We intend this dedication to be an overt act of
 * relinquishment in perpetuity of all present and future rights to this
 * software under copyright law.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
 * IN NO EVENT SHALL THE AUTHORS BE LIABLE FOR ANY CLAIM, DAMAGES OR
 * OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE,
 * ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 * #L%
 */

import nom.tam.fits.*;
import nom.tam.fits.compression.algorithm.api.ICompressOption;
import nom.tam.fits.compression.algorithm.api.ICompressorControl;
import nom.tam.fits.compression.provider.CompressorProvider;
import nom.tam.fits.compression.provider.param.api.HeaderAccess;
import nom.tam.fits.compression.provider.param.api.HeaderCardAccess;
import nom.tam.image.compression.tile.mask.ImageNullPixelMask;
import nom.tam.image.tile.operation.AbstractTiledImageOperation;
import nom.tam.image.tile.operation.TileArea;
import nom.tam.util.type.ElementType;

import java.lang.reflect.Array;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.util.concurrent.ExecutorService;

import static nom.tam.fits.header.Compression.*;
import static nom.tam.fits.header.Standard.TTYPEn;
import static nom.tam.image.compression.tile.TileCompressionType.*;

/**
 * This class represents a complete tiledImageOperation of tileOperations
 * describing an image ordered from left to right and top down. the
 * tileOperations all have the same geometry only the tileOperations at the
 * right side and the bottom side can have different sizes.
 */
public class TiledImageCompressionOperation extends AbstractTiledImageOperation<TileCompressionOperation> {

    /**
     * ZCMPTYPE name of the algorithm that was used to compress
     */
    private String compressAlgorithm;

    private final BinaryTable binaryTable;

    private ByteBuffer compressedWholeArea;

    // Note: field is initialized lazily: use getter within class!
    private ICompressorControl compressorControl;

    // Note: field is initialized lazily: use getter within class!
    private ICompressorControl gzipCompressorControl;

    /**
     * ZQUANTIZ name of the algorithm that was used to quantize
     */
    private String quantAlgorithm;

    private ICompressOption imageOptions;

    private ImageNullPixelMask imageNullPixelMask;

    private static void addColumnToTable(BinaryTableHDU hdu, Object column, String columnName) throws FitsException {
        if (column != null) {
            hdu.setColumnName(hdu.addColumn(column) - 1, columnName, null);
        }
    }

    private static void setNullEntries(Object column, Object defaultValue) {
        if (column != null) {
            for (int index = 0; index < Array.getLength(column); index++) {
                if (Array.get(column, index) == null) {
                    Array.set(column, index, defaultValue);
                }
            }
        }
    }

    /**
     * create a TiledImageCompressionOperation based on a compressed image data.
     *
     * @param binaryTable
     *            the compressed image data.
     */
    public TiledImageCompressionOperation(BinaryTable binaryTable) {
        super(TileCompressionOperation.class);
        this.binaryTable = binaryTable;
    }

    public void compress(BinaryTableHDU hdu) throws FitsException {
        processAllTiles();
        writeColumns(hdu);
        writeHeader(hdu.getHeader());
    }

    @Override
    public ICompressOption compressOptions() {
        initializeCompressionControl();
        return this.imageOptions;
    }

    public Buffer decompress() {
        Buffer decompressedWholeArea = getBaseType().newBuffer(getBufferSize());
        for (TileCompressionOperation tileOperation : getTileOperations()) {
            tileOperation.setWholeImageBuffer(decompressedWholeArea);
        }
        processAllTiles();
        decompressedWholeArea.rewind();
        return decompressedWholeArea;
    }

    public void forceNoLoss(int x, int y, int width, int heigth) {
        TileArea tileArea = new TileArea().start(x, y).end(x + width, y + heigth);
        for (TileCompressionOperation operation : getTileOperations()) {
            if (operation.getArea().intersects(tileArea)) {
                operation.forceNoLoss(true);
            }
        }
    }

    @Override
    public ByteBuffer getCompressedWholeArea() {
        return this.compressedWholeArea;
    }

    @Override
    public ICompressorControl getCompressorControl() {
        initializeCompressionControl();
        return this.compressorControl;
    }

    @Override
    public ICompressorControl getGzipCompressorControl() {
        if (this.gzipCompressorControl == null) {
            this.gzipCompressorControl = CompressorProvider.findCompressorControl(null, ZCMPTYPE_GZIP_1, getBaseType().primitiveClass());
        }
        return this.gzipCompressorControl;
    }

    public TiledImageCompressionOperation prepareUncompressedData(final Buffer buffer) throws FitsException {
        this.compressedWholeArea = ByteBuffer.wrap(new byte[getBaseType().size() * getBufferSize()]);
        createTiles(new TileCompressorInitialisation(this, buffer));
        this.compressedWholeArea.rewind();
        return this;
    }

    /**
     * preserve null values, where the value representing null is specified as a
     * parameter. This parameter is ignored for floating point values where NaN
     * is used as null value.
     *
     * @param nullValue
     *            the value representing null for byte/short and integer pixel
     *            values
     * @param compressionAlgorithm
     *            compression algorithm to use for the null pixel mask
     * @return the created null pixel mask
     */
    public ImageNullPixelMask preserveNulls(long nullValue, String compressionAlgorithm) {
        this.imageNullPixelMask = new ImageNullPixelMask(getTileOperations().length, nullValue, compressionAlgorithm);
        for (TileCompressionOperation tileOperation : getTileOperations()) {
            tileOperation.createImageNullPixelMask(getImageNullPixelMask());
        }
        return this.imageNullPixelMask;
    }

    public TiledImageCompressionOperation read(final Header header) throws FitsException {
        readPrimaryHeaders(header);
        setCompressAlgorithm(header.findCard(ZCMPTYPE));
        setQuantAlgorithm(header.findCard(ZQUANTIZ));
        createTiles(new TileDecompressorInitialisation(this, //
                getNullableColumn(header, Object[].class, UNCOMPRESSED_DATA_COLUMN), //
                getNullableColumn(header, Object[].class, COMPRESSED_DATA_COLUMN), //
                getNullableColumn(header, Object[].class, GZIP_COMPRESSED_DATA_COLUMN), //
                new HeaderAccess(header)));
        byte[][] nullPixels = getNullableColumn(header, byte[][].class, NULL_PIXEL_MASK_COLUMN);
        if (nullPixels != null) {
            preserveNulls(0L, header.getStringValue(ZMASKCMP)).setColumn(nullPixels);
        }
        readCompressionHeaders(header);
        return this;
    }

    public void readPrimaryHeaders(Header header) throws FitsException {
        readBaseType(header);
        readAxis(header);
        readTileAxis(header);
    }

    public TiledImageCompressionOperation setCompressAlgorithm(HeaderCard compressAlgorithmCard) {
        this.compressAlgorithm = compressAlgorithmCard.getValue();
        return this;
    }

    public TiledImageCompressionOperation setQuantAlgorithm(HeaderCard quantAlgorithmCard) {
        if (quantAlgorithmCard != null) {
            this.quantAlgorithm = quantAlgorithmCard.getValue();
        } else {
            this.quantAlgorithm = null;
        }
        return this;
    }

    private <T> T getNullableColumn(Header header, Class<T> class1, String columnName) throws FitsException {
        for (int i = 1; i <= this.binaryTable.getNCols(); i++) {
            String val = header.getStringValue(TTYPEn.n(i));
            if (val != null && val.trim().equals(columnName)) {
                return class1.cast(this.binaryTable.getColumn(i - 1));
            }
        }
        return null;
    }

    private void initializeCompressionControl() {
        if (this.compressorControl == null) {
            this.compressorControl = CompressorProvider.findCompressorControl(this.quantAlgorithm, this.compressAlgorithm, getBaseType().primitiveClass());
            if (this.compressorControl == null) {
                throw new IllegalStateException("Found no compressor control for compression algorithm:" + this.compressAlgorithm + //
                        " (quantize algorithm = " + this.quantAlgorithm + ", base type = " + getBaseType().primitiveClass() + ")");
            }
        }
        if (imageOptions == null) {
            initImageOptions();
        }
    }

    private void initImageOptions() {
        this.imageOptions = this.compressorControl.option();
        initializeQuantAlgorithm();
        this.imageOptions.getCompressionParameters().initializeColumns(getNumberOfTileOperations());
    }

    private void processAllTiles() {
        ExecutorService threadPool = FitsFactory.threadPool();
        for (TileCompressionOperation tileOperation : getTileOperations()) {
            tileOperation.execute(threadPool);
        }
        for (TileCompressionOperation tileOperation : getTileOperations()) {
            tileOperation.waitForResult();
        }
    }

    private void readAxis(Header header) throws FitsException {
        if (hasAxes()) {
            return;
        }
        int naxis = header.getIntValue(ZNAXIS);
        int[] axes = new int[naxis];
        for (int i = 1; i <= naxis; i++) {
            int axisValue = header.getIntValue(ZNAXISn.n(i), -1);
            if (axisValue == -1) {
                throw new FitsException("Required ZNAXISn not found");
            }
            axes[naxis - i] = axisValue;
        }
        setAxes(axes);
    }

    private void readBaseType(Header header) {
        if (getBaseType() == null) {
            int zBitPix = header.getIntValue(ZBITPIX);
            ElementType<Buffer> elementType = ElementType.forNearestBitpix(zBitPix);
            if (elementType == ElementType.UNKNOWN) {
                throw new IllegalArgumentException("illegal value for ZBITPIX " + zBitPix);
            }
            setBaseType(elementType);
        }
    }

    private void readCompressionHeaders(Header header) {
        compressOptions().getCompressionParameters().getValuesFromHeader(new HeaderAccess(header));
    }

    private void readTileAxis(Header header) throws FitsException {
        if (hasTileAxes()) {
            return;
        }
        
        int naxes = getNAxes();
        int[] tileAxes = new int[naxes]; 
        // TODO
        // The FITS default is to tile by row (Pence, W., et al. 2000, ASPC, 216, 551)
        // However, this library defaulted to full image size by default...
        for (int i = 1; i <= naxes; i++) {
            tileAxes[naxes - i] = header.getIntValue(ZTILEn.n(i), i == 1 ? header.getIntValue(ZNAXISn.n(1)) : 1);
        }

        setTileAxes(tileAxes);
    }

    private <T> Object setInColumn(Object column, boolean predicate, TileCompressionOperation tileOperation, Class<T> clazz, T value) {
        if (predicate) {
            if (column == null) {
                column = Array.newInstance(clazz, getNumberOfTileOperations());
            }
            Array.set(column, tileOperation.getTileIndex(), value);
        }
        return column;
    }

    private void writeColumns(BinaryTableHDU hdu) throws FitsException {
        Object compressedColumn = null;
        Object uncompressedColumn = null;
        Object gzipColumn = null;
        for (TileCompressionOperation tileOperation : getTileOperations()) {
            TileCompressionType compression = tileOperation.getCompressionType();
            byte[] compressedData = tileOperation.getCompressedData();

            compressedColumn = setInColumn(compressedColumn, compression == COMPRESSED, tileOperation, byte[].class, compressedData);
            gzipColumn = setInColumn(gzipColumn, compression == GZIP_COMPRESSED, tileOperation, byte[].class, compressedData);
            uncompressedColumn = setInColumn(uncompressedColumn, compression == UNCOMPRESSED, tileOperation, byte[].class, compressedData);
        }
        setNullEntries(compressedColumn, new byte[0]);
        setNullEntries(gzipColumn, new byte[0]);
        setNullEntries(uncompressedColumn, new byte[0]);
        addColumnToTable(hdu, compressedColumn, COMPRESSED_DATA_COLUMN);
        addColumnToTable(hdu, gzipColumn, GZIP_COMPRESSED_DATA_COLUMN);
        addColumnToTable(hdu, uncompressedColumn, UNCOMPRESSED_DATA_COLUMN);
        if (this.imageNullPixelMask != null) {
            addColumnToTable(hdu, this.imageNullPixelMask.getColumn(), NULL_PIXEL_MASK_COLUMN);
        }
        this.imageOptions.getCompressionParameters().addColumnsToTable(hdu);
        hdu.getData().fillHeader(hdu.getHeader());
    }

    private void writeHeader(Header header) throws FitsException {
        HeaderCardBuilder cardBuilder = header//
                .card(ZBITPIX).value(getBaseType().bitPix())//
                .card(ZCMPTYPE).value(this.compressAlgorithm);
        int[] tileAxes = getTileAxes();
        int naxes = tileAxes.length;
        for (int i = 1; i <= naxes; i++) {
            cardBuilder.card(ZTILEn.n(i)).value(tileAxes[naxes - i]);
        }
        compressOptions().getCompressionParameters().setValuesInHeader(new HeaderAccess(header));
        if (this.imageNullPixelMask != null) {
            cardBuilder.card(ZMASKCMP).value(this.imageNullPixelMask.getCompressAlgorithm());
        }
    }

    protected BinaryTable getBinaryTable() {
        return this.binaryTable;
    }

    protected ImageNullPixelMask getImageNullPixelMask() {
        return this.imageNullPixelMask;
    }

    protected void initializeQuantAlgorithm() {
        if (this.quantAlgorithm != null) {
            this.imageOptions.getCompressionParameters().getValuesFromHeader(new HeaderCardAccess(ZQUANTIZ, this.quantAlgorithm));
        }
    }

}
