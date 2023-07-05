package nom.tam.fits.test;

import nom.tam.fits.*;
import nom.tam.fits.header.Standard;
import org.junit.Assert;
import org.junit.Test;

import java.math.BigDecimal;
import java.util.Date;

import static nom.tam.fits.header.Standard.*;

public class BuilderApiTest {

    public Header createHeader() throws FitsException {
        byte[][] bimg = new byte[20][20];
        BasicHDU<?> hdu = Fits.makeHDU(bimg);
        Header hdr = hdu.getHeader();
        return hdr;
    }

    @Test
    public void testBasicApi() throws Exception {
        Header header = createHeader();
        Date date = new FitsDate("2015-07-11T05:21:25.446").toDate();

        header.card(DATE_OBS).value(date).comment("observe date")//
                .card(INSTRUME).value("The very best")//
                .card(ORIGIN).value("private")//
                .card(COMMENT).comment("something to comment")//
                .card(THEAP).value(2L)//
                .card(DATAMIN).value(1)//
                .card(DATAMAX).value(2)//
                .card(Standard.BSCALE).value(3.0)//
                .scale(1)//
                .card(TZEROn.n(5)).value(5.55)//
                .card(Standard.BZERO).value(6.55)//
                .card(Standard.EQUINOX).value(new BigDecimal("5.55"))//
                .noScale()//
                .card(TZEROn.n(1)).value(1.99999)//
                .card(TZEROn.n(2)).value(new BigDecimal("1.99999"))//
                .card(AUTHOR).value(true);

        Assert.assertEquals("2015-07-11T05:21:25.446", header.getStringValue(DATE_OBS));
        Assert.assertEquals("observe date", header.findCard(DATE_OBS).getComment());
        Assert.assertEquals("The very best", header.getStringValue(INSTRUME));
        Assert.assertEquals("private", header.getStringValue(ORIGIN));
        Assert.assertEquals("something to comment", header.findCard(COMMENT).getComment());
        Assert.assertEquals(null, header.getStringValue(COMMENT));
        Assert.assertEquals(2L, header.getLongValue(THEAP));
        Assert.assertEquals(1, header.getIntValue(DATAMIN));
        Assert.assertEquals(2.0, header.getFloatValue(DATAMAX), 0.000001);
        Assert.assertEquals(3.0, header.getDoubleValue(Standard.BSCALE), 0.000001);
        Assert.assertEquals(6.6, header.getDoubleValue(Standard.BZERO), 0.11);
        Assert.assertEquals(5.6, header.getDoubleValue(Standard.EQUINOX), 0.11);
        Assert.assertEquals(5.6, header.getFloatValue(TZEROn.n(5)), 0.11);
        Assert.assertEquals(1.99999, header.getDoubleValue(TZEROn.n(1)), 0.000001);
        Assert.assertEquals(1.99999, header.getDoubleValue(TZEROn.n(2)), 0.000001);
        Assert.assertEquals(true, header.getBooleanValue(AUTHOR));

        date = new FitsDate("2015-07-12T05:21:25.446").toDate();
        BasicHDU<?> hdu = Fits.makeHDU(header);
        hdu.card(DATE_OBS).value(date).comment("observation date")//
                .card(INSTRUME).value("The very very best")//
                .card(ORIGIN).value("other")//
                .card(COMMENT).comment("something else to comment")//
                .card(THEAP).value(200L)//
                .card(DATAMIN).value(100)//
                .card(DATAMAX).value(200)//
                .card(Standard.BSCALE).value(300.0)//
                .precision(4)//
                .card(TZEROn.n(5)).value(50.55)//
                .card(Standard.BZERO).value(500.055f)//
                .card(Standard.EQUINOX).value(new BigDecimal("500.055"))//
                .card(TZEROn.n(3)).value(600.055f)//
                .autoPrecision()//
                .card(TZEROn.n(1)).value(100.99999d)//
                .card(TZEROn.n(2)).value(new BigDecimal("100.99999"))//
                .card(TZEROn.n(4)).value(101.999)//
                .card(AUTHOR).value(false);

        Assert.assertEquals("2015-07-12T05:21:25.446", header.getStringValue(DATE_OBS));
        Assert.assertEquals("observation date", header.findCard(DATE_OBS).getComment());
        Assert.assertEquals("The very very best", header.getStringValue(INSTRUME));
        Assert.assertEquals("other", header.getStringValue(ORIGIN));
        Assert.assertEquals("something else to comment", header.findCard(COMMENT).getComment());
        Assert.assertEquals(null, header.getStringValue(COMMENT));
        Assert.assertEquals(200L, header.getLongValue(THEAP));
        Assert.assertEquals(100, header.getIntValue(DATAMIN));
        Assert.assertEquals(200.0, header.getFloatValue(DATAMAX), 0.000001);
        Assert.assertEquals(300.0, header.getDoubleValue(Standard.BSCALE), 0.000001);
        Assert.assertEquals(500.06f, header.getFloatValue(Standard.BZERO), 0.011f);
        Assert.assertEquals(500.06, header.getDoubleValue(Standard.EQUINOX), 0.000001);
        Assert.assertEquals(100.99999, header.getDoubleValue(TZEROn.n(1)), 0.000001);
        Assert.assertEquals(100.99999, header.getDoubleValue(TZEROn.n(2)), 0.000001);
        Assert.assertEquals(600.06f, header.getFloatValue(TZEROn.n(3)), 0.011f);
        Assert.assertEquals(101.999, header.getFloatValue(TZEROn.n(4)), 0.000001);
        Assert.assertEquals(false, header.getBooleanValue(AUTHOR));
        Assert.assertEquals(50.55, header.getFloatValue(TZEROn.n(5)), 0.000001);

    }
}
