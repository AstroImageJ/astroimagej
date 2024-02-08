package nom.tam.image.compression.tile.mask;

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

import java.nio.ByteBuffer;
import java.nio.DoubleBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.nio.ShortBuffer;

import nom.tam.fits.compression.algorithm.api.ICompressorControl;
import nom.tam.image.tile.operation.buffer.TileBuffer;
import nom.tam.util.type.ElementType;

/**
 * (<i>for internal use</i>) Preserves blank (<code>null</code>) values in
 * compressed images. This class overwrites the pixels specified in the mask
 * with null values. Where the null value can be defined separately.
 * 
 * @see nom.tam.image.compression.hdu.CompressedImageHDU
 */
public class NullPixelMaskPreserver extends AbstractNullPixelMask {

    /**
     * Creates a new instance for preserving the null values in a specific image
     * tile when compressing the tile.
     * 
     * @param tileBuffer
     *            the buffer containing the serialized tile data (still
     *            uncompressed)
     * @param tileIndex
     *            the sequential tile index
     * @param nullValue
     *            the blank value used in integer type images (this is ignored
     *            for floating-point images where NaN is used always)
     * @param compressorControl
     *            the class that performs the compresion of tiles, and which
     *            will be used to compress the null pixel mask also.
     */
    public NullPixelMaskPreserver(TileBuffer tileBuffer, int tileIndex, long nullValue, ICompressorControl compressorControl) {
        super(tileBuffer, tileIndex, nullValue, compressorControl);
    }

    /**
     * Creates a compressed mask to store along with the compressed tile image
     * to indicate where the blanking values appear in in the original image.
     */
    public void preserveNull() {
        if (getTileBuffer().getBaseType().is(ElementType.DOUBLE)) {
            preserveNullDoubles();
        } else if (getTileBuffer().getBaseType().is(ElementType.FLOAT)) {
            preserveNullFloats();
        } else if (getTileBuffer().getBaseType().is(ElementType.LONG)) {
            preserveNullLongs();
        } else if (getTileBuffer().getBaseType().is(ElementType.INT)) {
            preserveNullInts();
        } else if (getTileBuffer().getBaseType().is(ElementType.SHORT)) {
            preserveNullShorts();
        } else if (getTileBuffer().getBaseType().is(ElementType.BYTE)) {
            preserveNullBytes();
        }
        if (getMask() != null) {
            ByteBuffer compressed = ByteBuffer.allocate(getTileBuffer().getPixelSize());
            if (!getCompressorControl().compress(getMask(), compressed, getCompressorControl().option())) {
                throw new IllegalStateException("could not compress the null pixel mask");
            }
            setMask(compressed);
        }
    }

    private void preserveNullBytes() {
        ByteBuffer buffer = (ByteBuffer) getTileBuffer().getBuffer();
        byte nullValue = (byte) getNullValue();
        int size = buffer.remaining();
        for (int index = 0; index < size; index++) {
            if (nullValue == buffer.get(index)) {
                initializedMask(size).put(index, NULL_INDICATOR);
            }
        }
    }

    private void preserveNullDoubles() {
        DoubleBuffer buffer = (DoubleBuffer) getTileBuffer().getBuffer();
        int size = getTileBuffer().getPixelSize();
        for (int index = 0; index < size; index++) {
            if (Double.isNaN(buffer.get(index))) {
                initializedMask(size).put(index, NULL_INDICATOR);
            }
        }
    }

    private void preserveNullFloats() {
        FloatBuffer buffer = (FloatBuffer) getTileBuffer().getBuffer();
        int size = getTileBuffer().getPixelSize();
        for (int index = 0; index < size; index++) {
            if (Float.isNaN(buffer.get(index))) {
                initializedMask(size).put(index, NULL_INDICATOR);
            }
        }
    }

    private void preserveNullInts() {
        IntBuffer buffer = (IntBuffer) getTileBuffer().getBuffer();
        int nullValue = (int) getNullValue();
        int size = getTileBuffer().getPixelSize();
        for (int index = 0; index < size; index++) {
            if (nullValue == buffer.get(index)) {
                initializedMask(size).put(index, NULL_INDICATOR);
            }
        }
    }

    private void preserveNullLongs() {
        LongBuffer buffer = (LongBuffer) getTileBuffer().getBuffer();
        long nullValue = getNullValue();
        int size = getTileBuffer().getPixelSize();
        for (int index = 0; index < size; index++) {
            if (nullValue == buffer.get(index)) {
                initializedMask(size).put(index, NULL_INDICATOR);
            }
        }
    }

    private void preserveNullShorts() {
        ShortBuffer buffer = (ShortBuffer) getTileBuffer().getBuffer();
        short nullValue = (short) getNullValue();
        int size = getTileBuffer().getPixelSize();
        for (int index = 0; index < size; index++) {
            if (nullValue == buffer.get(index)) {
                initializedMask(size).put(index, NULL_INDICATOR);
            }
        }
    }

}
