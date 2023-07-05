package nom.tam.fits.compress;

import nom.tam.fits.FitsException;

import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

import static nom.tam.util.LoggerHelper.getLogger;

/**
 * (<i>for internal use</i>) UNIX compressed (<code>.Z</code>) input stream decompression with a preference for using an
 * external system command. You can use this class to decompress files that have been compressed with the UNIX
 * <b>compress</b> tool (or via <b>gzip</b>) and have the characteristic <code>.Z</code> file name extension. It
 * effectively provides the same functionality as {@link ZCompressionProvider}, but has a preference for calling on the
 * system <b>uncompress</b> command first to do the lifting. If that fails it will call on {@link CompressionManager} to
 * provide a suitable decompressor (which will give it {@link ZCompressionProvider}). Since the <b>compress</b> tool is
 * UNIX-specific, it is not entirely portable. As a result, you are probably better off relying on those other classes
 * directly.
 * 
 * @see        CompressionManager
 * 
 * @deprecated Use {@link ZCompressionProvider}. or the more generic {@link CompressionManager} with a preference toward
 *                 using the system command if possible. instead.
 */
@Deprecated
public class BasicCompressProvider implements ICompressProvider {

    private static final int PRIORITY = 10;

    private static final int COMPRESS_MAGIC_BYTE1 = 0x1f;

    private static final int COMPRESS_MAGIC_BYTE2 = 0x9d;

    private static final Logger LOG = getLogger(BasicCompressProvider.class);

    private InputStream compressInputStream(final InputStream compressed) throws IOException, FitsException {
        try {
            Process proc = new ProcessBuilder("uncompress", "-c").start();
            return new CloseIS(proc, compressed);
        } catch (Exception e) {
            ICompressProvider next = CompressionManager.nextCompressionProvider(COMPRESS_MAGIC_BYTE1, COMPRESS_MAGIC_BYTE2,
                    this);
            if (next != null) {
                LOG.log(Level.WARNING,
                        "Error initiating .Z decompression: " + e.getMessage() + " trying alternative decompressor", e);
                return next.decompress(compressed);
            }
            throw new FitsException("Unable to read .Z compressed stream.\nIs 'uncompress' in the path?", e);
        }
    }

    @Override
    public InputStream decompress(InputStream in) throws IOException, FitsException {
        return compressInputStream(in);
    }

    @Override
    public int priority() {
        return PRIORITY;
    }

    @Override
    public boolean provides(int mag1, int mag2) {
        return mag1 == COMPRESS_MAGIC_BYTE1 && mag2 == COMPRESS_MAGIC_BYTE2;
    }
}
