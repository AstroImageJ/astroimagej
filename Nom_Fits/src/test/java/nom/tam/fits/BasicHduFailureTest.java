package nom.tam.fits;

import nom.tam.util.FitsFile;
import nom.tam.util.SafeClose;
import nom.tam.util.test.ThrowAnyException;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;

import static nom.tam.fits.header.Standard.*;

public class BasicHduFailureTest {

    @Test
    public void testAxisFailuer() throws Exception {
        FitsException actual = null;
        try {
            BasicHDU<?> dummyHDU = BasicHDU.getDummyHDU();
            dummyHDU.getHeader().card(NAXIS).value(-1);
            dummyHDU.getAxes();
        } catch (FitsException e) {
            actual = e;
        }
        Assert.assertNotNull(actual);
        Assert.assertTrue(actual.getMessage().contains("NAXIS"));
        actual = null;
        try {
            BasicHDU<?> dummyHDU = BasicHDU.getDummyHDU();
            dummyHDU.getHeader().card(NAXIS).value(1001);
            dummyHDU.getAxes();
        } catch (FitsException e) {
            actual = e;
        }
        Assert.assertNotNull(actual);
        Assert.assertTrue(actual.getMessage().contains("NAXIS"));
    }

    @Test
    public void testBitPixFailuer() throws Exception {
        FitsException actual = null;
        try {
            BasicHDU<?> dummyHDU = BasicHDU.getDummyHDU();
            dummyHDU.getHeader().deleteKey(BITPIX);
            dummyHDU.getBitPix();
        } catch (FitsException e) {
            actual = e;
        }
        Assert.assertNotNull(actual);
        Assert.assertTrue(actual.getMessage().contains("BITPIX"));
    }

    @Test
    public void testBlankFailuer() throws Exception {
        FitsException actual = null;
        try {
            BasicHDU<?> dummyHDU = BasicHDU.getDummyHDU();
            dummyHDU.getHeader().deleteKey(BLANK);
            dummyHDU.getBlankValue();
        } catch (FitsException e) {
            actual = e;
        }
        Assert.assertNotNull(actual);
        Assert.assertTrue(actual.getMessage().contains("BLANK"));
    }

    @Test
    public void testCreationDateFailuer() throws Exception {
        BasicHDU<?> dummyHDU = BasicHDU.getDummyHDU();
        dummyHDU.getHeader().card(DATE).value("ABCDE");
        Assert.assertNull(dummyHDU.getCreationDate());
    }

    @Test
    public void testDefaultFileOffset() throws Exception {
        BasicHDU<?> dummyHDU = BasicHDU.getDummyHDU();
        FitsFile out = null;
        try {
            out = new FitsFile("target/BasicHduFailureTeststestDefaultFileOffset", "rw");
            dummyHDU.write(out);
        } finally {
            SafeClose.close(out);
        }
        Assert.assertEquals(0, dummyHDU.getFileOffset());
        FitsException actual = null;
        out = null;
        try {
            out = new FitsFile("target/BasicHduFailureTeststestDefaultFileOffset", "rw") {

                int count = 0;

                @Override
                public void flush() throws IOException {
                    count++;
                    // the 3e flush happens in the basic hdu write.
                    if (count == 3) {
                        throw new IOException("could not flush");
                    }
                    super.flush();
                }
            };
            dummyHDU.write(out);
        } catch (FitsException e) {
            actual = e;
        } finally {
            SafeClose.close(out);
        }

        Assert.assertNotNull(actual);
        Assert.assertTrue(actual.getMessage().contains("Error flushing at end of HDU"));
    }

    @Test
    public void testKernelFailuer() throws Exception {
        ImageData data = new ImageData((Object) null) {

            @Override
            public Object getData() {
                ThrowAnyException.throwFitsException("no data");
                return null;
            }
        };
        BasicHDU<?> dummyHDU = new ImageHDU(ImageHDU.manufactureHeader(data), data);
        Assert.assertNull(dummyHDU.getKernel());
    }

    @Test
    public void testObservationDateFailuer() throws Exception {
        BasicHDU<?> dummyHDU = BasicHDU.getDummyHDU();
        dummyHDU.getHeader().card(DATE_OBS).value("ABCDE");
        Assert.assertNull(dummyHDU.getObservationDate());
    }

    @Test(expected = FitsException.class)
    public void testRewriteFailuer() throws Exception {
        BasicHDU<?> dummyHDU = BasicHDU.getDummyHDU();
        dummyHDU.rewrite();
    }

    @Test(expected = FitsException.class)
    public void testSetPrimaryFailuer() throws Exception {
        UndefinedData data = new UndefinedData(new long[10]);
        BasicHDU<?> dummyHDU = new UndefinedHDU(UndefinedHDU.manufactureHeader(data), data);
        dummyHDU.setPrimaryHDU(true);
    }
}
