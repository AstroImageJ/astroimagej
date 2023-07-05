package nom.tam.fits.test;

import nom.tam.fits.BasicHDU;
import nom.tam.fits.Fits;
import nom.tam.fits.FitsException;
import nom.tam.fits.Header;
import nom.tam.fits.header.*;
import nom.tam.fits.header.extra.*;
import org.junit.Assert;
import org.junit.Test;

import java.lang.reflect.Constructor;
import java.net.URL;
import java.util.Arrays;

import static nom.tam.fits.header.InstrumentDescription.FILTER;
import static nom.tam.fits.header.Standard.INSTRUME;
import static nom.tam.fits.header.Standard.NAXISn;
import static nom.tam.fits.header.extra.NOAOExt.WATn_nnn;
import static org.junit.Assert.*;

/**
 * Check out header manipulation.
 */
public class EnumHeaderTest {

    public Header createHeader() throws FitsException {
        byte[][] bimg = new byte[20][20];
        BasicHDU hdu = Fits.makeHDU(bimg);
        Header hdr = hdu.getHeader();
        return hdr;
    }

    @Test
    public void exampleHeaderEnums() throws Exception {
        Header hdr = createHeader();

        // now some simple keywords
        hdr.addValue(INSTRUME, "My very big telescope");
        hdr.addValue(FILTER, "meade #25A Red");

        // and check if the simple keywords reached there destination.
        Assert.assertEquals("My very big telescope", hdr.getStringValue(INSTRUME.name()));
        Assert.assertEquals("meade #25A Red", hdr.getStringValue(FILTER.name()));
    }

    @Test
    public void multiyHeaderIndexes() throws Exception {
        Header hdr = createHeader();

        // now we take a header with multiple indexes
        hdr.addValue(WATn_nnn.n(9, 2, 3, 4), "50");

        // lets check is the keyword was correctly cearted
        Assert.assertEquals("50", hdr.getStringValue("WAT9_234"));
    }

    @Test
    public void simpleHeaderIndexes() throws Exception {
        Header hdr = createHeader();

        // ok the header NAXISn has a index, the 'n' in the keyword
        hdr.addValue(NAXISn.n(1), 10);
        hdr.addValue(NAXISn.n(2), 20);

        // lets check if the right values where set when we ask for the keyword
        // by String
        Assert.assertEquals(10, hdr.getIntValue("NAXIS1"));
        Assert.assertEquals(20, hdr.getIntValue("NAXIS2"));

        hdr.addValue(NAXISn.n(1), 10L);
        hdr.addValue(NAXISn.n(2), 20L);

        Assert.assertEquals(10, hdr.getIntValue("NAXIS1"));
        Assert.assertEquals(20, hdr.getIntValue("NAXIS2"));

    }

    @Test
    public void testAllHeaders() throws Exception {
        Class<?>[] classes = new Class<?>[] {Checksum.class, CXCExt.class, CXCStclSharedExt.class, DataDescription.class,
                HierarchicalGrouping.class, InstrumentDescription.class, MaxImDLExt.class, NOAOExt.class, NonStandard.class,
                ObservationDescription.class, ObservationDurationDescription.class, SBFitsExt.class, Standard.class,
                STScIExt.class, Compression.class};
        for (Class<?> class1 : classes) {
            for (Object enumConst : class1.getEnumConstants()) {
                Assert.assertNotNull(class1.getMethod("valueOf", String.class).invoke(class1,
                        enumConst.getClass().getMethod("name").invoke(enumConst)));
                IFitsHeader iFitsHeader = (IFitsHeader) enumConst;
                if (class1 != Standard.class) {
                    Assert.assertNotNull(iFitsHeader.comment());
                }
                String key = iFitsHeader.key();
                Assert.assertNotNull(key);
                Assert.assertNotNull(iFitsHeader.status());
                Assert.assertNotNull(iFitsHeader.valueType());
                Assert.assertNotNull(iFitsHeader.hdu());
                String url = iFitsHeader.status().url();
                if (url != null) {
                    new URL(url);
                }
                int nCount = 0;
                int offset = 0;
                while ((offset = key.indexOf('n', offset)) > 0) {
                    nCount++;
                    offset++;
                }
                int[] n = new int[nCount];
                Arrays.fill(n, 9);
                Assert.assertTrue(iFitsHeader.n(n).key().indexOf('n') < 0);
            }
        }

    }

    @Test
    public void testSynonyms() throws Exception {
        Assert.assertEquals(Standard.EQUINOX, Synonyms.primaryKeyword(Standard.EPOCH));
        Assert.assertEquals("EQUINOX", Synonyms.primaryKeyword("EPOCH"));
        Assert.assertArrayEquals(new IFitsHeader[] {Standard.EPOCH}, Synonyms.EQUINOX.getSynonyms());

        Assert.assertEquals(Standard.SIMPLE, Synonyms.primaryKeyword(Standard.SIMPLE));
        Assert.assertEquals("SIMPLE", Synonyms.primaryKeyword("SIMPLE"));
    }

    @Test
    public void testGenericKeyPrivate() throws Exception {
        Constructor<?>[] constrs = GenericKey.class.getDeclaredConstructors();
        assertEquals(constrs.length, 1);
        assertFalse(constrs[0].isAccessible());
        constrs[0].setAccessible(true);
        constrs[0].newInstance();
    }

    @Test
    public void testReuseStandard() throws Exception {
        IFitsHeader[] result = GenericKey.create(new String[] {"BITPIX", "SIMPLE", "UNKOWN"});
        assertSame(Standard.BITPIX, result[0]);
        assertSame(Standard.SIMPLE, result[1]);
        assertTrue(result[2] instanceof FitsHeaderImpl);
    }

    @Test
    public void testLookup() throws Exception {
        assertSame(Standard.BITPIX, GenericKey.lookup(Standard.BITPIX.key()));
        assertSame(Standard.NAXISn, GenericKey.lookup(Standard.NAXISn.n(99).key()));

    }

    @Test
    public void testIFitsHeader() throws Exception {
        assertEquals(9, IFitsHeader.HDU.values().length);
        assertSame(IFitsHeader.HDU.ANY, IFitsHeader.HDU.valueOf(IFitsHeader.HDU.ANY.name()));
        assertEquals(14, IFitsHeader.SOURCE.values().length);
        assertSame(IFitsHeader.SOURCE.UNKNOWN, IFitsHeader.SOURCE.valueOf(IFitsHeader.SOURCE.UNKNOWN.name()));
        assertEquals(7, IFitsHeader.VALUE.values().length);
        assertSame(IFitsHeader.VALUE.ANY, IFitsHeader.VALUE.valueOf(IFitsHeader.VALUE.ANY.name()));
        assertEquals(4, Synonyms.values().length);
    }
}
