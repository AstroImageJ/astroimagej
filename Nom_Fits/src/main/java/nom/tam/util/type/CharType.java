package nom.tam.util.type;

/*
 * #%L
 * nom.tam FITS library
 * %%
 * Copyright (C) 2004 - 2024 nom-tam-fits
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

import nom.tam.fits.FitsFactory;

import java.nio.ByteBuffer;

/**
 * A FITS character element. Unlike Java characters, which use unicode, Java has only ASCII character type. For
 * historical reasons we store Java Unicode characters as 16-bit short values by default, but the more conventional FITS
 * standard is to store them as 8-bit ASCII. You can select which method to use to store <code>char[]</code> arrays in
 * FITS binary tables using {@link FitsFactory#setUseUnicodeChars(boolean)}.
 * 
 * @see FitsFactory#setUseUnicodeChars(boolean)
 */
class CharType extends ElementType<ByteBuffer> {

    protected CharType() {
        super(0, false, char.class, Character.class, null, 'C', 0);
    }

    @Override
    public int size() {
        return FitsFactory.isUseUnicodeChars() ? ElementType.SHORT.size() : ElementType.BYTE.size();
    }
}
