package jdos.win.builtin.kernel32;

import jdos.win.builtin.WinAPI;
import jdos.win.system.WinSystem;
import jdos.win.utils.StringUtil;

public class Environ extends WinAPI {
    // BOOL WINAPI SetEnvironmentVariable(LPCTSTR lpName, LPCTSTR lpValue)
    static public int SetEnvironmentVariableA(int lpName, int lpValue) {
        if (lpValue != 0)
            WinSystem.getCurrentProcess().env.put(StringUtil.getString(lpName), StringUtil.getString(lpValue));
        else
            WinSystem.getCurrentProcess().env.remove(StringUtil.getString(lpName));
        return TRUE;
    }
}
