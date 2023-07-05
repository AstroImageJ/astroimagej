package nom.tam.fits.test;

import nom.tam.fits.Fits;
import nom.tam.fits.Header;
import nom.tam.fits.HeaderCard;
import org.junit.Test;

import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.util.List;
import java.util.Set;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import static org.junit.Assert.*;

/**
 * Test adding a little junk after a valid image. We wish to test three scenarios: Junk at the beginning (should
 * continue to fail) Short (<80 byte) junk after valid HDU Long (>80 byte) junk after valid HDU The last two should
 * succeed after FitsFactory.setAllowTerminalJunk(true).
 */
public class DupTest {

    class LogCounter extends Handler {
        private int count = 0;

        @Override
        public void close() throws SecurityException {
        }

        @Override
        public void flush() {
        }

        @Override
        public synchronized void publish(LogRecord arg0) {
            count++;
            System.err.println("### MESSAGE: " + arg0.getMessage());
        }

        public synchronized int getCount() {
            return count;
        }
    }

    @Test
    public void test() throws Exception {

        Fits f = new Fits("src/test/resources/nom/tam/fits/test/test_dup.fits");
        Header hdr = f.readHDU().getHeader();
        assertEquals("Internal size:", 8640, hdr.getSize());
        assertEquals("External size:", 8640, hdr.getMinimumSize());
        assertTrue("Has duplicates:", hdr.hadDuplicates());
        List<HeaderCard> dups = hdr.getDuplicates();

        int nDups = dups.size();
        System.out.println("Number of duplicates:" + nDups);
        assertTrue("Has dups:", dups != null && dups.size() > 0);
        // AK: It is rewritable with preallocated blank space that is now supported!
        assertTrue("Not rewriteable:", hdr.rewriteable());

        DataOutputStream bf = new DataOutputStream(new FileOutputStream("target/created_dup.fits"));
        hdr.resetOriginalSize();
        assertEquals("External size, after reset", 2880, hdr.getMinimumSize());
        f.write(bf);
        bf.flush();
        bf.close();
        Fits g = new Fits("target/created_dup.fits");
        hdr = g.readHDU().getHeader();
        assertEquals("Internal size, after rewrite", 2880, hdr.getSize());
        assertEquals("External size, after rewrite", 2880, hdr.getMinimumSize());
        assertTrue("Now rewriteable", hdr.rewriteable());
        assertFalse("No duplicates", hdr.hadDuplicates());
        assertTrue("Dups is null", hdr.getDuplicates() == null);
    }

    private Logger getParserLogger() {
        return Logger.getLogger("nom.tam.fits.HeaderCardParser");
    }

    @Test
    public void dupesWarningsOn() throws Exception {
        Logger l = getParserLogger();
        l.setLevel(Level.WARNING); // Make sure we log warnings to Header

        LogCounter counter = new LogCounter();
        l.addHandler(counter);

        int initCount = counter.getCount();

        Header.setParserWarningsEnabled(true);
        assertTrue(Header.isParserWarningsEnabled()); // Check that warings are enabled

        Fits f = new Fits("src/test/resources/nom/tam/fits/test/test_dup.fits");
        Header h = f.readHDU().getHeader();

        assertTrue("Has dups:", h.hadDuplicates()); // Check that we did indeed have duplicates
        assertNotEquals(initCount, counter.getCount()); // Check that logger was called on them
    }

    @Test
    public void dupesWarningsOff() throws Exception {
        Logger l = getParserLogger();
        l.setLevel(Level.WARNING); // Make sure we log warnings to Header
        LogCounter counter = new LogCounter();
        l.addHandler(counter);

        int initCount = counter.getCount();

        Header.setParserWarningsEnabled(false);
        assertFalse(Header.isParserWarningsEnabled()); // Check that warings are enabled

        Fits f = new Fits("src/test/resources/nom/tam/fits/test/test_dup.fits");
        Header h = f.readHDU().getHeader();

        assertTrue("Has dups:", h.hadDuplicates()); // Check that we did indeed have duplicates
        assertEquals(initCount, counter.getCount()); // Check that logger was NOT called on them

        l.removeHandler(counter);
    }

    @Test
    public void dupesSetTest() throws Exception {
        Fits f = new Fits("src/test/resources/nom/tam/fits/test/test_dup.fits");
        Header h = f.readHDU().getHeader();

        Set<?> keys = h.getDuplicateKeySet();
        assertTrue(keys.contains("CARD"));
    }

}
