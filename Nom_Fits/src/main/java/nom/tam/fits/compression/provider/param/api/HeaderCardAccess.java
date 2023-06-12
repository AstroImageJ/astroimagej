package nom.tam.fits.compression.provider.param.api;

import nom.tam.fits.Header;
import nom.tam.fits.HeaderCard;
import nom.tam.fits.HeaderCardException;
import nom.tam.fits.header.IFitsHeader;

/**
 * <p>
 * (<i>for internal use</i>) Access to a specific FITS header card with runtime exceptions only. Regular modifications
 * to {@link HeaderCard} may throw {@link HeaderCardException}s, which are hard exceptions. They really should have been
 * softer runtime exceptions from the start, but unfortunately that was choice this library made a very long time ago,
 * and we therefore stick to it, at least until the next major code revision (major version 2 at the earliest). So this
 * class provides an alternative access to a header card converting any <code>HeaderCardException</code>s to
 * {@link IllegalArgumentException}.
 * </p>
 * <p>
 * Unlike {@link HeaderAccess} this class operates on single cards. Methods that specify a keywords are applied to the
 * selected card if and only if the keyword matches that of the card's keyword.
 * </p>
 * 
 * @see Header
 */
public class HeaderCardAccess implements IHeaderAccess {

    private final HeaderCard headerCard;

    /**
     * <p>
     * Creates a new access to modifying a {@link HeaderCard} without the hard exceptions that <code>HeaderCard</code>
     * may throw.
     * </p>
     * <p>
     * Unlike {@link HeaderAccess} this class operates on single cards. Methods that specify a keywords are applied to
     * the selected card if and only if the keyword matches that of the card's keyword.
     * </p>
     * 
     * @param headerCard the FITS keyword of the card we will provide access to
     * @param value      the initial string value for the card (assuming the keyword allows string values).
     */
    public HeaderCardAccess(IFitsHeader headerCard, String value) {
        try {
            this.headerCard = new HeaderCard(headerCard.key(), value, null);
        } catch (HeaderCardException e) {
            throw new IllegalArgumentException("header card could not be created");
        }
    }

    @Override
    public void addValue(IFitsHeader key, int value) {
        if (headerCard.getKey().equals(key.key())) {
            headerCard.setValue(value);
        }
    }

    @Override
    public void addValue(IFitsHeader key, String value) {
        if (headerCard.getKey().equals(key.key())) {
            headerCard.setValue(value);
        }
    }

    @Override
    public HeaderCard findCard(IFitsHeader key) {
        return findCard(key.key());
    }

    @Override
    public HeaderCard findCard(String key) {
        if (headerCard.getKey().equals(key)) {
            return headerCard;
        }
        return null;
    }
}
