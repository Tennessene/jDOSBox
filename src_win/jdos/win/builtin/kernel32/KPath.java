package jdos.win.builtin.kernel32;

import jdos.win.builtin.WinAPI;
import jdos.win.system.WinSystem;
import jdos.win.utils.StringUtil;

public class KPath extends WinAPI {
    // BOOL WINAPI SetCurrentDirectory(LPCTSTR lpPathName)
    static public int SetCurrentDirectoryA(int lpPathName) {
        WinSystem.getCurrentProcess().currentWorkingDirectory = StringUtil.getString(lpPathName);
        return TRUE;
    }
}
