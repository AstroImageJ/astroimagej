package nom.tam.image.tile.operation.buffer;

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

import java.nio.Buffer;

import nom.tam.util.type.ElementType;

/**
 * (<i>for internal use</i>) Creates linear buffers for storing data of 2D image tiles.
 *
 * @see TileBuffer
 */
public final class TileBufferFactory {

    /**
     * @deprecated for internal use only
     */
    @SuppressWarnings("javadoc")
    public static TileBuffer createTileBuffer(ElementType<Buffer> baseType, int dataOffset, int imageWidth, int width,
            int height) {
        if (imageWidth > width) {
            return new TileBufferColumnBased(baseType, dataOffset, imageWidth, width, height);
        }
        return new TileBufferRowBased(baseType, dataOffset, width, height);
    }

    private TileBufferFactory() {
        // unused
    }
}
