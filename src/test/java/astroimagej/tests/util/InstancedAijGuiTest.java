package astroimagej.tests.util;

import ij.IJ;
import ij.ImageJ;
import org.assertj.swing.fixture.FrameFixture;

import static org.assertj.swing.launcher.ApplicationLauncher.application;

/**
 * Tests extending this class will restart AIJ for <b>each</b> test method.
 *
 * @see AbstractInstancedGuiTest
 * @see SequentialAijGuiTest
 */
public abstract class InstancedAijGuiTest extends AbstractInstancedGuiTest {
    protected FrameFixture window;

    @Override
    protected void onSetUp() throws Exception {
        application(ImageJ.class).start();

        window = new FrameFixture(robot(), IJ.getInstance());
    }
}
