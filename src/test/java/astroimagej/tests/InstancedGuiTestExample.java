package astroimagej.tests;

import astroimagej.tests.util.FileUtils;
import astroimagej.tests.util.InstancedAijGuiTest;
import ij.IJ;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

public class InstancedGuiTestExample extends InstancedAijGuiTest {

    @Test
    void hasAstronomyTool() {
        assertThat(window).isNotNull();//todo fill out
    }

    @Test
    @Disabled("Static elements of MP are not fully reset as the classes are still loaded, reset?")
    //https://stackoverflow.com/questions/20091075/reload-used-classes-at-runtime-java
    //todo how to reset static variables? forkEvery = 1?
    void canRunMultiPlot() {
        IJ.runPlugIn("Astronomy.MultiPlot_", "");
    }

    @Test
    void canOpenFile() {
        //assertEquals(2, 3);
        IJ.open(FileUtils.getTestFile(Path.of("io/fits").resolve("8bitTest.fits")).toString());

        //IJ.error("Wait!");
    }
}
