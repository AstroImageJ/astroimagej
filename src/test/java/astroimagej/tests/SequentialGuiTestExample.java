package astroimagej.tests;

import astroimagej.tests.util.SequentialAijGuiTest;
import ij.IJ;
import org.assertj.swing.core.GenericTypeMatcher;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import java.awt.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.swing.finder.WindowFinder.findFrame;

//todo pick order that makes sense, @Order annotation
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class SequentialGuiTestExample extends SequentialAijGuiTest {
    @Override
    protected void onSetUp() throws Exception {

    }

    @Test
    void hasAstronomyTool() {
        assertThat(window).isNotNull();//todo fill out
    }

    @Test
    void canRunMultiPlot() {
        IJ.runPlugIn("Astronomy.MultiPlot_", "");
        var mpMain = findFrame(new GenericTypeMatcher<Frame>(Frame.class) {
            protected boolean isMatching(Frame frame) {
                return "Multi-plot Main".equals(frame.getTitle());
            }
        }).using(robot);
        assertThat(mpMain).isNotNull();
    }
}
