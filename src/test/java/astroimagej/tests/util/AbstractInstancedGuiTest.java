package astroimagej.tests.util;

import org.assertj.swing.core.BasicRobot;
import org.assertj.swing.core.Robot;
import org.assertj.swing.edt.FailOnThreadViolationRepaintManager;
import org.assertj.swing.testing.AssertJSwingTestCaseTemplate;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;

/**
 * Based on {@link AssertJSwingTestCaseTemplate}.
 */
abstract class AbstractInstancedGuiTest {
    private Robot robot = null;

    /**
     * Installs a <code>{@link FailOnThreadViolationRepaintManager}</code> to catch violations of Swing threading rules.
     */
    @BeforeAll
    public static void setUpOnce() {
        if (Boolean.getBoolean("enforceEDTValidation")) {
            FailOnThreadViolationRepaintManager.install();
        }
    }

    /**
     * Sets up this test's fixture, starting from creation of a new <code>{@link org.assertj.swing.core.Robot}</code>.
     *
     * @see #setUpRobot()
     * @see #onSetUp()
     * @throws Exception when the set up of the test fails which results in the complete test fails
     */
    @BeforeEach
    public final void setUp() throws Exception {
        setUpRobot();
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
    }

    /**
     * Cleans up any resources used in this test. After calling <code>{@link #onTearDown()}</code>, this method cleans up
     * resources used by this test's <code>{@link org.assertj.swing.core.Robot}</code>.
     *
     * @see #cleanUp()
     * @see #onTearDown()
     * @throws Exception when the tear down of the test fails which results in that the test will not be cleaned up properly
     */
    @AfterEach
    public final void tearDown() throws Exception {
        try {
            onTearDown();
        } finally {
            cleanUp();
        }
    }

    /**
     * Subclasses need to clean up resources in this method. This method is called <strong>before</strong> executing
     * <code>{@link #tearDown()}</code>.
     *
     * @throws Exception when the tear down of the test fails which results in that the test will not be cleaned up properly
     */
    protected void onTearDown() throws Exception {}

    /**
     * Creates this test's {@link Robot} using a new AWT hierarchy.
     */
    protected void setUpRobot() {
        robot = BasicRobot.robotWithNewAwtHierarchy();
    }

    /**
     * Cleans up resources used by this test's {@link Robot}.
     */
    protected void cleanUp() {
        robot.cleanUp();
    }

    /**
     * @return this test's {@link Robot}
     */
    protected final Robot robot() {
        return robot;
    }
}
