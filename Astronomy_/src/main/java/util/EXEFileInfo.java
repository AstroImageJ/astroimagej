package util;

import com.sun.jna.Memory;
import com.sun.jna.Pointer;
import com.sun.jna.platform.win32.VerRsrc.VS_FIXEDFILEINFO;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.ptr.PointerByReference;
import ij.IJ;

// Based on https://stackoverflow.com/a/31642529/8753755 licensed as CC BY-SA 3.0
public class EXEFileInfo {
    public static int MAJOR = 0;
    public static int MINOR = 1;
    public static int BUILD = 2;
    public static int REVISION = 3;

    public static int getMajorVersionOfProgram(String path) {
        return getVersionInfo(path)[MAJOR];
    }

    public static int getMinorVersionOfProgram(String path) {
        return getVersionInfo(path)[MINOR];
    }

    public static int getBuildOfProgram(String path) {
        return getVersionInfo(path)[BUILD];
    }

    public static int getRevisionOfProgram(String path) {
        return 0;//getVersionInfo(path)[REVISION];
    }

    public static int[] getVersionInfo(String path) {
        if (!IJ.isWindows()) return new int[]{999, 999, 999};
        IntByReference dwDummy = new IntByReference();
        dwDummy.setValue(0);

        int versionlength = com.sun.jna.platform.win32.Version.INSTANCE.GetFileVersionInfoSize(path, dwDummy);

        if (versionlength == 0) return new int[]{999, 999, 999};

        byte[] bufferarray = new byte[versionlength];
        Pointer lpData = new Memory(bufferarray.length);
        PointerByReference lplpBuffer = new PointerByReference();
        IntByReference puLen = new IntByReference();
        // These must exist
        boolean fileInfoResult = com.sun.jna.platform.win32.Version.INSTANCE.GetFileVersionInfo(path, 0, versionlength, lpData);
        boolean verQueryVal = com.sun.jna.platform.win32.Version.INSTANCE.VerQueryValue(lpData, "\\", lplpBuffer, puLen);

        VS_FIXEDFILEINFO lplpBufStructure = new VS_FIXEDFILEINFO(lplpBuffer.getValue());
        lplpBufStructure.read();

        int v1 = (lplpBufStructure.dwProductVersionMS).intValue() >> 16;
        int v2 = (lplpBufStructure.dwProductVersionMS).intValue() & 0xffff;
        int v3 = (lplpBufStructure.dwProductVersionLS).intValue() >> 16;
        int v4 = (lplpBufStructure.dwProductVersionLS).intValue() & 0xffff;
        return new int[] { v1, v2, v3};
    }
}