package nom.tam.fits.compression.algorithm.hcompress;

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

import nom.tam.fits.compression.algorithm.api.ICompressor;
import nom.tam.fits.compression.algorithm.quant.QuantizeProcessor.DoubleQuantCompressor;
import nom.tam.fits.compression.algorithm.quant.QuantizeProcessor.FloatQuantCompressor;
import nom.tam.util.ArrayFuncs;

import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;

/**
 * (<i>for internal use</i>) Data compressor using the HCompress algorithm.
 * 
 * @param <T> The generic type of buffer that accessed the type of elements needed for the compression
 */
@SuppressWarnings("javadoc")
public abstract class HCompressor<T extends Buffer> implements ICompressor<T> {

    public static class ByteHCompressor extends HCompressor<ByteBuffer> {

        private static final long BYTE_MASK_FOR_LONG = 0xFFL;

        /**
         * HCompress of byte streams with the default scale parameter of 0 and no smoothing (lossless compression).
         * 
         * @since  1.19.1
         * 
         * @author Attila Kovacs
         */
        public ByteHCompressor() {
            this(new HCompressorOption());
        }

        public ByteHCompressor(HCompressorOption options) {
            super(options);
        }

        @Override
        public boolean compress(ByteBuffer buffer, ByteBuffer compressed) {
            byte[] byteArray = new byte[buffer.limit()];
            buffer.get(byteArray);
            long[] longArray = new long[byteArray.length];
            for (int index = 0; index < longArray.length; index++) {
                longArray[index] = byteArray[index] & BYTE_MASK_FOR_LONG;
            }
            compress(longArray, compressed);
            return true;
        }

        @Override
        public void decompress(ByteBuffer compressed, ByteBuffer buffer) {
            long[] longArray = new long[buffer.limit()];
            decompress(compressed, longArray);
            for (long element : longArray) {
                buffer.put((byte) element);
            }
        }

    }

    public static class DoubleHCompressor extends DoubleQuantCompressor {

        @SuppressWarnings("deprecation")
        public DoubleHCompressor(HCompressorQuantizeOption options) {
            super(options, new IntHCompressor((HCompressorOption) options.getCompressOption()));
        }
    }

    public static class FloatHCompressor extends FloatQuantCompressor {

        @SuppressWarnings("deprecation")
        public FloatHCompressor(HCompressorQuantizeOption options) {
            super(options, new IntHCompressor((HCompressorOption) options.getCompressOption()));
        }
    }

    public static class IntHCompressor extends HCompressor<IntBuffer> {

        /**
         * HCompress of 32-bit integer streams with the default scale parameter of 0 and no smoothing for lossless
         * compression.
         * 
         * @since  1.19.1
         * 
         * @author Attila Kovacs
         */
        public IntHCompressor() {
            this(new HCompressorOption());
        }

        public IntHCompressor(HCompressorOption options) {
            super(options);
        }

        @Override
        public boolean compress(IntBuffer buffer, ByteBuffer compressed) {
            int[] intArray = new int[buffer.limit()];
            buffer.get(intArray);
            long[] longArray = new long[intArray.length];
            ArrayFuncs.copyInto(intArray, longArray);
            compress(longArray, compressed);
            return true;
        }

        @Override
        public void decompress(ByteBuffer compressed, IntBuffer buffer) {
            long[] longArray = new long[buffer.limit()];
            decompress(compressed, longArray);
            for (long element : longArray) {
                buffer.put((int) element);
            }
        }

    }

    public static class ShortHCompressor extends HCompressor<ShortBuffer> {

        /**
         * HCompress of 16-bit integer streams with the default scale parameter of 0 (lossless compression).
         * 
         * @since  1.19.1
         * 
         * @author Attila Kovacs
         */
        public ShortHCompressor() {
            this(new HCompressorOption());
        }

        public ShortHCompressor(HCompressorOption options) {
            super(options);
        }

        @Override
        public boolean compress(ShortBuffer buffer, ByteBuffer compressed) {
            short[] shortArray = new short[buffer.limit()];
            buffer.get(shortArray);
            long[] longArray = new long[shortArray.length];
            ArrayFuncs.copyInto(shortArray, longArray);
            compress(longArray, compressed);
            return true;
        }

        @Override
        public void decompress(ByteBuffer compressed, ShortBuffer buffer) {
            long[] longArray = new long[buffer.limit()];
            decompress(compressed, longArray);
            for (long element : longArray) {
                buffer.put((short) element);
            }
        }
    }

    private final HCompress compress;

    private final HDecompress decompress;

    private final HCompressorOption options;

    public HCompressor(HCompressorOption options) {
        this.options = options;
        compress = new HCompress();
        decompress = new HDecompress();
    }

    private HCompress compress() {
        return compress;
    }

    protected void compress(long[] longArray, ByteBuffer compressed) {
        compress().compress(longArray, options.getTileHeight(), options.getTileWidth(), options.getScale(), compressed);
    }

    private HDecompress decompress() {
        return decompress;
    }

    protected void decompress(ByteBuffer compressed, long[] aa) {
        decompress().decompress(compressed, options.isSmooth(), aa);
    }
}
