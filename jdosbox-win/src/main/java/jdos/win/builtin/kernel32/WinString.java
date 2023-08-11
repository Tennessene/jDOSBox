package jdos.win.builtin.kernel32;

import jdos.win.utils.StringUtil;

public class WinString {
    // LPTSTR WINAPI lstrcat(LPTSTR lpString1, LPTSTR lpString2)
    static public int lstrcatA(int lpString1, int lpString2) {
        StringUtil.strcat(lpString1, lpString2);
        return lpString1;
    }

    // LPTSTR WINAPI lstrcpyn(LPTSTR lpString1, LPCTSTR lpString2, int iMaxLength)
    static public int lstrcpynA(int lpString1, int lpString2, int iMaxLength) {
        StringUtil.strncpy(lpString1, lpString2, iMaxLength);
        return lpString1;
    }
}
