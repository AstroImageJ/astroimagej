package nom.tam.image.compression.tile.mask;

import java.nio.ByteBuffer;

import nom.tam.fits.compression.algorithm.api.ICompressorControl;
import nom.tam.fits.compression.provider.CompressorProvider;
import nom.tam.image.tile.operation.buffer.TileBuffer;

/*
 * #%L
 * nom.tam FITS library
 * %%
 * Copyright (C) 1996 - 2024 nom-tam-fits
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

/**
 * Support for blank (<code>null</code>) values in compressed images.
 */
public class ImageNullPixelMask {

    private final AbstractNullPixelMask[] nullPixelMasks;

    private final long nullValue;

    private final ICompressorControl compressorControl;

    private final String compressAlgorithm;

    public ImageNullPixelMask(int tileCount, long nullValue, String compressAlgorithm) {
        nullPixelMasks = new AbstractNullPixelMask[tileCount];
        this.nullValue = nullValue;
        this.compressAlgorithm = compressAlgorithm;
        compressorControl = CompressorProvider.findCompressorControl(null, this.compressAlgorithm, byte.class);
    }

    public NullPixelMaskPreserver createTilePreserver(TileBuffer tileBuffer, int tileIndex) {
        return add(new NullPixelMaskPreserver(tileBuffer, tileIndex, nullValue, compressorControl));
    }

    public NullPixelMaskRestorer createTileRestorer(TileBuffer tileBuffer, int tileIndex) {
        return add(new NullPixelMaskRestorer(tileBuffer, tileIndex, nullValue, compressorControl));
    }

    @SuppressWarnings("deprecation")
    public byte[][] getColumn() {
        byte[][] column = new byte[nullPixelMasks.length][];
        for (AbstractNullPixelMask tileMask : nullPixelMasks) {
            column[tileMask.getTileIndex()] = tileMask.getMaskBytes();
        }
        return column;
    }

    public String getCompressAlgorithm() {
        return compressAlgorithm;
    }

    public void setColumn(byte[][] nullPixels) {
        for (AbstractNullPixelMask tileMask : nullPixelMasks) {
            byte[] tileMaskBytes = nullPixels[tileMask.getTileIndex()];
            if (tileMaskBytes != null && tileMaskBytes.length > 0) {
                tileMask.setMask(ByteBuffer.wrap(tileMaskBytes));
            }
        }
    }

    private <T extends AbstractNullPixelMask> T add(T nullPixelMask) {
        nullPixelMasks[nullPixelMask.getTileIndex()] = nullPixelMask;
        return nullPixelMask;
    }

}
