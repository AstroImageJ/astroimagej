package astroimagej.tests;

import ij.astro.util.FitsExtensionUtil;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Collections;
import java.util.EnumSet;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class FitsExtensionHandlingTest {
    @ParameterizedTest
    @ValueSource(strings = {"t1.fits.fz.gz", "t1.fts.fz.gz", "t1.fit.fz.gz"})
    public void canUnderstandDoubleCompression(String name) {
        assertThat(FitsExtensionUtil.compressionModes(name))
                .containsAll(EnumSet.allOf(FitsExtensionUtil.CompressionMode.class));
    }

    @ParameterizedTest
    @ValueSource(strings = {"t1.fits.fz", "t1.fts.fz", "t1.fit.fz"})
    public void canUnderstandSingleCompression(String name) {
        assertThat(FitsExtensionUtil.compressionModes(name))
                .containsAll(Collections.singleton(FitsExtensionUtil.CompressionMode.FPACK));
    }

    @ParameterizedTest
    @ValueSource(strings = {"t1.fits.fz.gz", "t1.fts.fz", "t1.fit.fz", "t1.fit.fz.gz", "t1.fts.gz", "t1.fits.fz"})
    public void canStripCompression(String name) {
        var stripped = FitsExtensionUtil.makeFitsSave(name, false, false);
        assertEquals(0, FitsExtensionUtil.compressionModes(stripped).size());
    }
}
