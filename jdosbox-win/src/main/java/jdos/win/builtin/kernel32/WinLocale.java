package jdos.win.builtin.kernel32;

import jdos.win.utils.StringUtil;

public class WinLocale {
    // UINT GetOEMCP(void);
    static public int GetOEMCP() {
        return 437;
    }

    // int WINAPI lstrcmp(LPCTSTR lpString1, LPCTSTR lpString2)
    public static int lstrcmpA(int lpString1, int lpString2) {
        return StringUtil.strcmp(lpString1, lpString2);
    }
}
