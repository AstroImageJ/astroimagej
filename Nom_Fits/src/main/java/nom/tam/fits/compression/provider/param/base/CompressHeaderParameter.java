package nom.tam.fits.compression.provider.param.base;

import nom.tam.fits.HeaderCard;
import nom.tam.fits.compression.provider.param.api.ICompressHeaderParameter;
import nom.tam.fits.compression.provider.param.api.IHeaderAccess;

import static nom.tam.fits.header.Compression.ZNAMEn;
import static nom.tam.fits.header.Compression.ZVALn;

/**
 * (<i>for internal use</i>) Visibility may be reduced to protected.
 * 
 * @param <OPTION> The generic type of the compression option for which this parameter is used.
 */
@SuppressWarnings("javadoc")
public abstract class CompressHeaderParameter<OPTION> extends CompressParameter<OPTION>
        implements ICompressHeaderParameter {

    protected CompressHeaderParameter(String name, OPTION option) {
        super(name, option);
    }

    public HeaderCard findZVal(IHeaderAccess header) {
        int nval = 1;
        HeaderCard card = header.findCard(ZNAMEn.n(nval));
        while (card != null) {
            if (card.getValue().equals(getName())) {
                return header.findCard(ZVALn.n(nval));
            }
            card = header.findCard(ZNAMEn.n(++nval));
        }
        return null;
    }

    public int nextFreeZVal(IHeaderAccess header) {
        int nval = 1;
        HeaderCard card = header.findCard(ZNAMEn.n(nval));
        while (card != null) {
            card = header.findCard(ZNAMEn.n(++nval));
        }
        return nval;
    }
}
