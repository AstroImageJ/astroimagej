package nom.tam.fits;

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

import nom.tam.util.ArrayDataInput;
import nom.tam.util.ArrayDataOutput;

import java.io.IOException;

/**
 * I/O interface for various FITS file elements.
 */
public interface FitsElement {

    /**
     * Returns the byte offset at which this element starts ina file. If the
     * element was not obtained from an input, then 0 is returned.
     * 
     * @return the byte at which this element begins. This is only available if
     *         the data is originally read from a random access medium.
     *         Otherwise 0 is returned.
     */
    long getFileOffset();

    /**
     * Returns the size of this elements in the FITS representation. This may
     * include padding if the element (such as a header or data segment) is
     * expected to complete a FITS block of 2880 bytes.
     * 
     * @return The size of this element in bytes, or 0 if the element is empty
     *         or invalid.
     */
    long getSize();

    /**
     * Read a FITS element from the input, starting at the current position.
     * Ater the read, the implementations should leave the input position
     * aligned to the start of the next FITS block.
     * 
     * @param in
     *            The input data stream
     * @throws FitsException
     *             if the read was unsuccessful.
     * @throws IOException
     *             if the read was unsuccessful.
     */
    void read(ArrayDataInput in) throws FitsException, IOException;

    /**
     * Reset the input stream to point to the beginning of this element
     * 
     * @return True if the reset succeeded.
     */
    boolean reset();

    /**
     * Rewrite the contents of the element in place. The data must have been
     * originally read from a random access device, and the size of the element
     * may not have changed.
     * 
     * @throws FitsException
     *             if the rewrite was unsuccessful.
     * @throws IOException
     *             if the rewrite was unsuccessful.
     */
    void rewrite() throws FitsException, IOException;

    /**
     * Checks if we can write this element back to its source. An element can
     * only be written back if it is associated to a random accessible input and
     * the current size FITS within the old block size.
     * 
     * @return <code>true</code> if this element can be rewritten?
     */
    boolean rewriteable();

    /**
     * Writes the contents of the element to a data sink, adding padding as
     * necessary if the element (such as a header or data segment) is expected
     * to complete the FITS block of 2880 bytes.
     * 
     * @param out
     *            The data sink.
     * @throws FitsException
     *             if the write was unsuccessful.
     */
    void write(ArrayDataOutput out) throws FitsException;
}
