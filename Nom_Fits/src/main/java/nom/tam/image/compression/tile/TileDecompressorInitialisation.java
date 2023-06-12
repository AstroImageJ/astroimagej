package nom.tam.image.compression.tile;

import nom.tam.fits.FitsException;
import nom.tam.fits.compression.provider.param.api.IHeaderAccess;
import nom.tam.image.tile.operation.ITileOperationInitialisation;
import nom.tam.image.tile.operation.TileArea;

import static nom.tam.image.compression.tile.TileCompressionType.*;

final class TileDecompressorInitialisation implements ITileOperationInitialisation<TileCompressionOperation> {

    private final Object[] uncompressed;

    private final Object[] compressed;

    private final Object[] gzipCompressed;

    private final IHeaderAccess header;

    private final TiledImageCompressionOperation imageTilesOperation;

    private int compressedOffset = 0;

    protected TileDecompressorInitialisation(TiledImageCompressionOperation imageTilesOperation, Object[] uncompressed,
            Object[] compressed, Object[] gzipCompressed, IHeaderAccess header) {
        this.imageTilesOperation = imageTilesOperation;
        this.uncompressed = uncompressed;
        this.compressed = compressed;
        this.gzipCompressed = gzipCompressed;
        this.header = header;
    }

    @Override
    public TileCompressionOperation createTileOperation(int tileIndex, TileArea area) {
        return new TileDecompressor(imageTilesOperation, tileIndex, area);
    }

    @Override
    public void init(TileCompressionOperation tileOperation) {
        tileOperation.setCompressedOffset(compressedOffset)//
                .setCompressed(compressed != null ? compressed[tileOperation.getTileIndex()] : null, COMPRESSED)//
                .setCompressed(uncompressed != null ? uncompressed[tileOperation.getTileIndex()] : null, UNCOMPRESSED)//
                .setCompressed(gzipCompressed != null ? gzipCompressed[tileOperation.getTileIndex()] : null,
                        GZIP_COMPRESSED);
        tileOperation.createImageNullPixelMask(imageTilesOperation.getImageNullPixelMask());
        compressedOffset += tileOperation.getPixelSize();
    }

    @Override
    public void tileCount(int tileCount) throws FitsException {
        imageTilesOperation.compressOptions().getCompressionParameters().initializeColumns(header,
                imageTilesOperation.getBinaryTable(), tileCount);
    }
}
