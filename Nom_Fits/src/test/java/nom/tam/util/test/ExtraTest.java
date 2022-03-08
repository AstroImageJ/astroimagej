package nom.tam.util.test;

import nom.tam.fits.HeaderCard;
import nom.tam.fits.utilities.FitsLineAppender;
import org.junit.Assert;
import org.junit.Test;

public class ExtraTest {

    @Test
    public void testExtraLineApender() {
        FitsLineAppender apender = new FitsLineAppender();
        apender.append("0123456789012345678901234567890123456789012345");
        apender.append("0123456789012345678901234567890123456789012345");
        Assert.assertEquals(80, apender.toString().length());
    }

    @Test
    public void testParseStringComment() {
        HeaderCard hc = HeaderCard.create("KEY = / ' test '");
        Assert.assertEquals("", hc.getValue());
        Assert.assertEquals("' test '", hc.getComment());
    }

}
