package nom.tam.fits.compression.provider.param.base;

import nom.tam.fits.Header;
import nom.tam.fits.HeaderCard;
import nom.tam.fits.HeaderCardException;
import nom.tam.fits.compression.provider.param.api.ICompressHeaderParameter;
import nom.tam.fits.compression.provider.param.api.IHeaderAccess;

import static nom.tam.fits.header.Compression.ZNAMEn;
import static nom.tam.fits.header.Compression.ZVALn;

/**
 * (<i>for internal use</i>) Visibility may be reduced to protected.
 * 
 * @param <OPTION> The generic type of the compression option for which this parameter is used.
 */
@SuppressWarnings({"javadoc", "deprecation"})
public abstract class CompressHeaderParameter<OPTION> extends CompressParameter<OPTION>
        implements ICompressHeaderParameter {

    protected CompressHeaderParameter(String name, OPTION option) {
        super(name, option);
    }

    /**
     * @deprecated Use {@link #findZVal(Header)} instead.
     */
    public HeaderCard findZVal(IHeaderAccess header) {
        return findZVal(header.getHeader());
    }

    /**
     * @deprecated Use {@link #nextFreeZVal(Header)} instead.
     */
    public int nextFreeZVal(IHeaderAccess header) {
        return nextFreeZVal(header.getHeader());
    }

    /**
     * Finds the ZVAL header value corresponding to this compression parameter
     * 
     * @param  header              The compressed HDU header
     * 
     * @return                     the header card containing the ZVAL for this compression parameter
     * 
     * @throws HeaderCardException if there was an issue accessing the header
     */
    public HeaderCard findZVal(Header header) throws HeaderCardException {
        int nval = 1;
        HeaderCard card = header.getCard(ZNAMEn.n(nval));
        while (card != null) {
            if (card.getValue().equals(getName())) {
                return header.getCard(ZVALn.n(nval));
            }
            card = header.getCard(ZNAMEn.n(++nval));
        }
        return null;
    }

    /**
     * <p>
     * Finds the next available (or previously used) the ZNAME / ZVAL index in the header that we can use to store this
     * parameter.
     * </p>
     * <p>
     * Unfortunately, the way it was implemented, using this repeatedly on the same header and compression parameter
     * keeps adding new entries, rather than updating the existing one. As of 1.19, the behavior is changed to update
     * existing values -- resulting in a more predictable behavior.
     * </p>
     * 
     * @param  header              The compressed HDU header
     * 
     * @return                     the ZNAME / ZVAL index we might use to store a new parameter
     * 
     * @throws HeaderCardException if there was an issue accessing the header
     */
    public int nextFreeZVal(Header header) throws HeaderCardException {
        for (int n = 1;; n++) {
            HeaderCard card = header.getCard(ZNAMEn.n(n));
            if (card == null) {
                return n;
            }
            if (getName().equals(card.getValue())) {
                return n;
            }
        }
    }
}
