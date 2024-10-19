package util;

import com.sun.jna.Native;
import com.sun.jna.platform.win32.Kernel32;
import ij.IJ;

public class WinMutexHandler {
    /**
     * This mutex is used by the (un)installer on Windows to ensure that AIJ is closed while they run.
     */
    public static void createMutex() {
        try {
            if (Kernel32.INSTANCE.CreateMutex(null, false, "AstroImageJ") == null) {
                IJ.error("Failed to create Mutex with error " + Native.getLastError());
            }
        } catch (UnsatisfiedLinkError e) {
            e.printStackTrace();
        }
    }
}
