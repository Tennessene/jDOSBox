package jdos.win.builtin.kernel32;

import jdos.win.builtin.WinAPI;
import jdos.win.utils.StringUtil;

public class Profile extends WinAPI {
    // DWORD WINAPI GetPrivateProfileString(LPCTSTR lpAppName, LPCTSTR lpKeyName, LPCTSTR lpDefault, LPTSTR lpReturnedString, DWORD nSize, LPCTSTR lpFileName)
    static public int GetPrivateProfileStringA(int lpAppName, int lpKeyName, int lpDefault, int lpReturnedString, int nSize, int lpFileName) {
        if (lpDefault != 0)
            return StringUtil.strncpy(lpReturnedString, lpDefault, nSize);
        writeb(lpReturnedString, 0);
        return 0;
    }
}
