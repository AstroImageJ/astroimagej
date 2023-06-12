package nom.tam.util;

import java.io.IOException;
import java.io.OutputStream;

/**
 * @deprecated Use {@link FitsOutputStream}, which provides the exact same functionality but with a less misleading
 *                 name, or else use {@link ArrayOutputStream} as a base for an implementation with any (non-FITS)
 *                 encoding.
 */
@Deprecated
public class BufferedDataOutputStream extends FitsOutputStream {

    /**
     * Instantiates a new output stream for FITS data.
     *
     * @param o         the underlying output stream
     * @param bufLength the size of the buffer to use in bytes.
     */
    public BufferedDataOutputStream(OutputStream o, int bufLength) {
        super(o, bufLength);
    }

    /**
     * Instantiates a new output stream for FITS data, using a default buffer size.
     *
     * @param o the underlying output stream
     */
    public BufferedDataOutputStream(OutputStream o) {
        super(o);
    }

    /**
     * @deprecated             No longer used internally, but kept for back compatibility (and it does exactly nothing)
     *
     * @param      need        the number of consecutive bytes we need in the buffer for the next write.
     *
     * @throws     IOException if there was an IO error flushing the buffer to the output to make avaiable the space
     *                             required in the buffer.
     */
    @Deprecated
    protected void checkBuf(int need) throws IOException {
    }

}
