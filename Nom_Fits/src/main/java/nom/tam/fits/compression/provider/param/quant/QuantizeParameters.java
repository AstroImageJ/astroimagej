package nom.tam.fits.compression.provider.param.quant;

import nom.tam.fits.compression.algorithm.api.ICompressOption;
import nom.tam.fits.compression.algorithm.quant.QuantizeOption;
import nom.tam.fits.compression.provider.param.api.ICompressColumnParameter;
import nom.tam.fits.compression.provider.param.api.ICompressHeaderParameter;
import nom.tam.fits.compression.provider.param.base.CompressParameters;

/**
 * (<i>for internal use</i>) A set of compression parameters recorded in the FITS that describe the quantization of
 * floating point data. Quantization is the process of representing floating-point values by integers.
 *
 * @author Attila Kovacs
 */
public class QuantizeParameters extends CompressParameters {

    private ZQuantizeParameter quantz;

    private ZBlankParameter blank;

    private ZDither0Parameter seed;

    private ZBlankColumnParameter blankColumn;

    private ZZeroColumnParameter zero;

    private ZScaleColumnParameter scale;

    /**
     * Creates a set of compression parameters used for quantization of floating point data. Quantization is the process
     * of representing floating-point values by integers.
     *
     * @param option The compression option that is configured with the particular parameter values of this object.
     */
    @SuppressWarnings("deprecation")
    public QuantizeParameters(QuantizeOption option) {
        quantz = new ZQuantizeParameter(option);
        blank = new ZBlankParameter(option);
        seed = new ZDither0Parameter(option);
        blankColumn = new ZBlankColumnParameter(option);
        zero = new ZZeroColumnParameter(option);
        scale = new ZScaleColumnParameter(option);
    }

    @Override
    protected ICompressColumnParameter[] columnParameters() {
        return new ICompressColumnParameter[] {blankColumn, zero, scale};
    }

    @Override
    protected ICompressHeaderParameter[] headerParameters() {
        return new ICompressHeaderParameter[] {quantz, blank, seed};
    }

    @Override
    public void setTileIndex(int index) {
        seed.setTileIndex(index);
    }

    @Override
    public QuantizeParameters copy(ICompressOption option) {
        if (option instanceof QuantizeOption) {
            QuantizeOption qo = (QuantizeOption) option;

            QuantizeParameters p = (QuantizeParameters) super.clone();
            p.quantz = (ZQuantizeParameter) quantz.copy(qo);
            p.blank = (ZBlankParameter) blank.copy(qo);
            p.seed = (ZDither0Parameter) seed.copy(qo);
            p.blankColumn = (ZBlankColumnParameter) blankColumn.copy(qo);
            p.zero = (ZZeroColumnParameter) zero.copy(qo);
            p.scale = (ZScaleColumnParameter) scale.copy(qo);

            return p;
        }
        return null;
    }
}
