package util;

import com.sun.jna.Native;
import com.sun.jna.platform.win32.Kernel32;
import com.sun.jna.platform.win32.WinNT;
import ij.IJ;

public class WinMutexHandler {
    /**
     * This mutex is used by the (un)installer on Windows to ensure that AIJ is closed while they run.
     */
    public static void createMutex() {
        try {
            final WinNT.HANDLE handle;

            if ((handle = Kernel32.INSTANCE.CreateMutex(null, false, "AstroImageJ")) == null) {
                IJ.error("Failed to create Mutex with error " + Native.getLastError());
            }

            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                    try {
                        if (Kernel32.INSTANCE.ReleaseMutex(handle)) {
                            System.err.println("Failed to release Mutex during shutdown " + Native.getLastError());
                        }
                    } catch (Exception | UnsatisfiedLinkError e) {
                        e.printStackTrace();
                    }
            }, "AIJ Win Mutex Freer"));
        } catch (UnsatisfiedLinkError e) {
            e.printStackTrace();
        }
    }
}
