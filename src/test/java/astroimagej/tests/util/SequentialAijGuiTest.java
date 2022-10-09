package astroimagej.tests.util;

import ij.IJ;
import ij.ImageJ;
import org.assertj.swing.core.BasicRobot;
import org.assertj.swing.core.Robot;
import org.assertj.swing.edt.FailOnThreadViolationRepaintManager;
import org.assertj.swing.fixture.FrameFixture;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;

import static org.assertj.swing.launcher.ApplicationLauncher.application;

/**
 * Tests extending this class will share an instance AIJ between <b>every</b> test method.
 *
 * @see InstancedAijGuiTest
 */
public abstract class SequentialAijGuiTest {

    protected static FrameFixture window = null;
    protected static Robot robot = null;

    /**
     * Installs a <code>{@link FailOnThreadViolationRepaintManager}</code> to catch violations of Swing threading rules.
     */
    @BeforeAll
    public static void setUpOnce() {
        if (Boolean.getBoolean("enforceEDTValidation")) {
            FailOnThreadViolationRepaintManager.install();
        }

        application(ImageJ.class).start();

        robot = BasicRobot.robotWithNewAwtHierarchy();

        window = new FrameFixture(robot, IJ.getInstance());
        //todo why don't these other methods work?
        //window = findFrame(ImageJ.class).using(robot);
        /*window = findFrame(new GenericTypeMatcher<Frame>(Frame.class) {
            protected boolean isMatching(Frame frame) {
                System.out.println(frame);
                IJ.getInstance()
                return frame instanceof ImageJ;
            }
        }).using(robot);*/
    }

    /**
     * Sets up this test's fixture.
     *
     * @see #onSetUp()
     * @throws Exception when the set up of the test fails which results in the complete test fails
     */
    @BeforeEach
    public final void setUp() throws Exception {
        onSetUp();
    }

    /**
     * Subclasses need set up their own test fixture in this method. This method is called <strong>after</strong>
     * executing <code>{@link #setUp()}</code>.
     *
     * @throws Exception when the set up of the test fails which results in the complete test fails
     */
    protected abstract void onSetUp() throws Exception;

    /**
     * Removes the <code>{@link FailOnThreadViolationRepaintManager}</code> again to allow EDT violating and EDT safe
     * tests in the same suite.
     */
    @AfterAll
    public static void tearDownOnce() {
        FailOnThreadViolationRepaintManager.uninstall();
        robot.cleanUp();
    }
}
