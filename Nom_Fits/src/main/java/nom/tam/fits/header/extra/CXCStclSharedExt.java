package nom.tam.fits.header.extra;

import nom.tam.fits.header.DateTime;
import nom.tam.fits.header.FitsKey;
import nom.tam.fits.header.IFitsHeader;

/**
 * This is the file represents the common keywords between CXC and STSclExt. See e.g. the ASC keywords at
 * <a href="https://planet4589.org/astro/sds/asc/ps/SDS05.pdf">https://planet4589.org/astro/sds/asc/ps/SDS05.pdf</a> for
 * defititions of these. .
 * 
 * @deprecated These are available both in the {@link CXCExt} and {@link STScIExt} enums. This class may be removed in
 *                 the future.
 * 
 * @see        STScIExt
 * @see        CXCExt
 * 
 * @author     Attila Kovacs and Richard van Nieuwenhoven
 */
public enum CXCStclSharedExt implements IFitsHeader {

    /**
     * Same as {@link STScIExt#CLOCKAPP}.
     */
    CLOCKAPP(STScIExt.CLOCKAPP),

    /**
     * Same as {@link STScIExt#MJDREF}.
     */
    MJDREF(STScIExt.MJDREF),

    /**
     * Same as {@link STScIExt#TASSIGN}.
     */
    TASSIGN(STScIExt.TASSIGN),

    /**
     * Same as {@link DateTime#TIMEDEL}.
     */
    TIMEDEL(DateTime.TIMEDEL),

    /**
     * Same as {@link STScIExt#TIMEREF}.
     */
    TIMEREF(STScIExt.TIMEREF),

    /**
     * Same as {@link STScIExt#TIMEUNIT}.
     */
    TIMEUNIT(STScIExt.TIMEUNIT),

    /**
     * Same as {@link STScIExt#TIMVERSN}.
     */
    TIMVERSN(STScIExt.TIMVERSN),

    /**
     * Same as {@link STScIExt#TIMEZERO}.
     */
    TIMEZERO(STScIExt.TIMEZERO),

    /**
     * Same as {@link STScIExt#TSTART}.
     */
    TSTART(STScIExt.TSTART),

    /**
     * Same as {@link CXCStclSharedExt#TSTOP}.
     */
    TSTOP(STScIExt.TSTOP);

    private final FitsKey key;

    CXCStclSharedExt(IFitsHeader orig) {
        key = orig.impl();
    }

    @Override
    public final FitsKey impl() {
        return key;
    }

}
