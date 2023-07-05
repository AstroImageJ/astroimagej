package nom.tam.fits.compression.provider;

import nom.tam.fits.FitsException;
import nom.tam.fits.compression.algorithm.api.ICompressOption;
import nom.tam.fits.compression.algorithm.api.ICompressorControl;
import nom.tam.fits.compression.provider.param.api.ICompressParameters;
import org.junit.Assert;
import org.junit.Test;

public class CompressionProviderTest {

    @Test
    public void testNullOptions() {
        ICompressorControl compressor = CompressorProvider.findCompressorControl(null, "GZIP_1", byte.class);
        ICompressOption option = compressor.option();
        Assert.assertFalse(option.isLossyCompression());
        option.setParameters(null); // nothinh should happen ;-)
        Assert.assertNull(option.unwrap(String.class));
        Assert.assertSame(option, option.unwrap(ICompressOption.class));
    }

    @Test
    public void testNullParameters() throws FitsException {
        ICompressorControl compressor = CompressorProvider.findCompressorControl(null, "GZIP_1", byte.class);
        ICompressOption option = compressor.option();
        ICompressParameters parameters = option.getCompressionParameters();

        parameters.addColumnsToTable(null);// nothinh should happen ;-)
        Assert.assertSame(parameters, parameters.copy(option));
        parameters.setValuesInColumn(10000);// nothinh should happen ;-)
        parameters.setValuesInHeader(null);// nothinh should happen ;-)
    }
}
