package nom.tam.fits.test;

import nom.tam.fits.FitsFactory;
import org.junit.Test;

import java.util.concurrent.ExecutorService;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class FitsFactoryTest {

    @Test
    public void testFitsSettings() throws Exception {

        FitsFactory.setAllowHeaderRepairs(true);
        assertEquals(true, FitsFactory.isAllowHeaderRepairs());

        FitsFactory.setAllowTerminalJunk(true);
        assertEquals(true, FitsFactory.getAllowTerminalJunk());

        FitsFactory.setCheckAsciiStrings(true);
        assertEquals(true, FitsFactory.getCheckAsciiStrings());

        FitsFactory.setLongStringsEnabled(true);
        assertEquals(true, FitsFactory.isLongStringsEnabled());

        FitsFactory.setSkipBlankAfterAssign(true);
        assertEquals(true, FitsFactory.isSkipBlankAfterAssign());

        FitsFactory.setUseAsciiTables(true);
        assertEquals(true, FitsFactory.getUseAsciiTables());

        FitsFactory.setUseHierarch(true);
        assertEquals(true, FitsFactory.getUseHierarch());

        FitsFactory.setAllowHeaderRepairs(false);
        assertEquals(false, FitsFactory.isAllowHeaderRepairs());

        FitsFactory.setAllowTerminalJunk(false);
        assertEquals(false, FitsFactory.getAllowTerminalJunk());

        FitsFactory.setCheckAsciiStrings(false);
        assertEquals(false, FitsFactory.getCheckAsciiStrings());

        FitsFactory.setLongStringsEnabled(false);
        assertEquals(false, FitsFactory.isLongStringsEnabled());

        FitsFactory.setSkipBlankAfterAssign(false);
        assertEquals(false, FitsFactory.isSkipBlankAfterAssign());

        FitsFactory.setUseAsciiTables(false);
        assertEquals(false, FitsFactory.getUseAsciiTables());

        FitsFactory.setUseHierarch(false);
        assertEquals(false, FitsFactory.getUseHierarch());

    }

    @Test
    public void testInitThreadPool() throws Exception {
        ExecutorService s = FitsFactory.threadPool();
        assertNotNull(s);
        assertEquals(s, FitsFactory.threadPool());
    }
}
