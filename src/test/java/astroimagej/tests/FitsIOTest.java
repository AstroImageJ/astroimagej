package astroimagej.tests;

import astroimagej.tests.util.FileUtils;
import astroj.FitsJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.plugin.FITS_Reader;
import ij.process.ByteProcessor;
import org.assertj.core.api.Condition;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class FitsIOTest {
    @Test
    public void readWriteUncompressed() {
        var stack = readStack("8bitTest.fits");
        assertEquals(1, stack.size());
        assertThat(stack.getProcessor(1)).isInstanceOf(ByteProcessor.class);

        //todo write test
    }

    @Test
    void supportsAijAnnotations() {
        var imp = readImage("annotationTest.fit");

        assertThat(imp).has(new Condition<>(imagePlus -> {
            var header = FitsJ.getHeader(imagePlus);
            var oldLength = header.length;
            return FitsJ.removeAnnotateCards(header).length == oldLength - 4;
        }, "4 annotations"));
    }

    /**
     * Ensure that all LCO images, even those missing a TableHDU are only reading the first imageHDU.
     */
    @ParameterizedTest
    @ValueSource(strings = {"LCO Test With Table.fits.fz", "LCO Test Missing Table.fits.fz"})
    void canReadLco(String filename) {
        var stack = readStack(filename);
        assertEquals(1, stack.size());
    }

    //todo compressed options test

    //todo @RepeatedTest for compressed writing to stress the compression

    private static ImageStack readStack(String file) {
        var reader = new FITS_Reader();
        reader.run(FileUtils.getTestFile(Path.of("io/fits").resolve(file)).toString());

        return reader.getImageStack();
    }

    private static ImagePlus readImage(String file) {
        var reader = new FITS_Reader();
        reader.run(FileUtils.getTestFile(Path.of("io/fits").resolve(file)).toString());

        return reader.createImagePlus();
    }
}
