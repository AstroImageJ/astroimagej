package nom.tam.fits;

import org.junit.Assert;
import org.junit.Test;

import java.util.Calendar;
import java.util.TimeZone;

public class FitsDateTest {
    private static long REF_TIME_MS = 1543407194000L;

    @Test
    public void testIsoDateParsing() throws Exception {
        Assert.assertEquals(REF_TIME_MS, new FitsDate("2018-11-28T12:13:14").toDate().getTime());
        Assert.assertEquals(REF_TIME_MS + 100, new FitsDate("2018-11-28T12:13:14.1").toDate().getTime());
        Assert.assertEquals(REF_TIME_MS + 120, new FitsDate("2018-11-28T12:13:14.12").toDate().getTime());
        Assert.assertEquals(REF_TIME_MS + 123, new FitsDate("2018-11-28T12:13:14.123").toDate().getTime());
        Assert.assertEquals(REF_TIME_MS + 123, new FitsDate("2018-11-28T12:13:14.1234").toDate().getTime());
        Assert.assertEquals(REF_TIME_MS + 124, new FitsDate("2018-11-28T12:13:14.1236").toDate().getTime());
        Assert.assertEquals(REF_TIME_MS + 123, new FitsDate("2018-11-28T12:13:14.12345").toDate().getTime());
        Assert.assertEquals(REF_TIME_MS + 124, new FitsDate("2018-11-28T12:13:14.123567").toDate().getTime());
        Assert.assertEquals(REF_TIME_MS + 10, new FitsDate("2018-11-28T12:13:14.01").toDate().getTime());
        Assert.assertEquals(REF_TIME_MS + 1, new FitsDate("2018-11-28T12:13:14.001").toDate().getTime());
        Assert.assertEquals(REF_TIME_MS, new FitsDate("2018-11-28T12:13:14.0001").toDate().getTime());
    }

    @Test
    public void testFitsDateCompare() throws Exception {
        Assert.assertEquals(new FitsDate("2018-11-28T12:13:14.15"), new FitsDate("2018-11-28T12:13:14.15"));
        Assert.assertNotEquals(new FitsDate("2018-11-28T12:13:14.151"), new FitsDate("2018-11-28T12:13:14.15"));
        Assert.assertNotEquals(new FitsDate("2018-11-28T12:13:13.15"), new FitsDate("2018-11-28T12:13:14.15"));
        Assert.assertNotEquals(new FitsDate("2018-11-28T12:12:14.15"), new FitsDate("2018-11-28T12:13:14.15"));
        Assert.assertNotEquals(new FitsDate("2018-11-28T11:13:14.15"), new FitsDate("2018-11-28T12:13:14.15"));
        Assert.assertNotEquals(new FitsDate("2018-11-27T12:13:14.15"), new FitsDate("2018-11-28T12:13:14.15"));
        Assert.assertNotEquals(new FitsDate("2018-10-28T12:13:14.15"), new FitsDate("2018-11-28T12:13:14.15"));
        Assert.assertNotEquals(new FitsDate("2017-11-28T12:13:14.15"), new FitsDate("2018-11-28T12:13:14.15"));
    }

    @Test
    public void testDateOnly() throws Exception {
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        cal.set(2023, 4 - 1, 3, 13, 9, 33);

        Assert.assertEquals("2023-04-03", FitsDate.getFitsDateString(cal.getTime(), false));
    }

    @Test
    public void testNullDate() throws Exception {
        Assert.assertNull(new FitsDate(null).toDate());
    }

    @Test
    public void testEmptyDate() throws Exception {
        Assert.assertNull(new FitsDate("").toDate());
    }

}
