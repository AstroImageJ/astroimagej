package nom.tam.fits.compression.provider.param.api;

import nom.tam.fits.Header;
import nom.tam.fits.HeaderCard;
import nom.tam.fits.HeaderCardException;
import nom.tam.fits.header.IFitsHeader;

/**
 * <p>
 * (<i>for internal use</i>) Interface for accessing FITS header values with
 * runtime exceptions only. Regular header access throws
 * {@link HeaderCardException}s, which are hard exceptions. They really should
 * have been softer runtime exceptions from the start, but unfortunately that
 * was choice this library made a very long time ago, and we therefore stick to
 * it, at least until the next major code revision (major version 2 at the
 * earliest). So this class provides an alternative access to headers converting
 * any <code>HeaderCardException</code>s to {@link IllegalArgumentException}.
 * </p>
 * <p>
 * This is really just a rusty rail implementation, and rather incopmlete at it
 * too. It has very limited support for header access, geared very specifically
 * towards supporting the compression classes of this library, and not mean for
 * use beyond.
 * </p>
 * 
 * @see Header
 */
public interface IHeaderAccess {

    /**
     * Sets a new integer value for the specified FITS keyword, adding it to the
     * FITS header if necessary.
     * 
     * @param key
     *            the standard or conventional FITS header keyword
     * @param value
     *            the integer value to assign to the keyword
     * @throws IllegalArgumentException
     *             if the value could not be set as requested.
     */
    void addValue(IFitsHeader key, int value) throws IllegalArgumentException;

    /**
     * Sets a new string value for the specified FITS keyword, adding it to the
     * FITS header if necessary.
     * 
     * @param key
     *            the standard or conventional FITS header keyword
     * @param value
     *            the string value to assign to the keyword
     * @throws IllegalArgumentException
     *             if the value could not be set as requested.
     */
    void addValue(IFitsHeader key, String value) throws IllegalArgumentException;

    /**
     * Returns the FITS header card for the given FITS keyword.
     * 
     * @param key
     *            the standard or conventional FITS header keyword
     * @return the matching FITS header card, or <code>null</code> if there is
     *         no such card within out grasp.
     */
    HeaderCard findCard(IFitsHeader key);

    /**
     * Returns the FITS header card for the given FITS keyword.
     * 
     * @param key
     *            the FITS header keyword
     * @return the matching FITS header card, or <code>null</code> if there is
     *         no such card within out grasp.
     */
    HeaderCard findCard(String key);

}
