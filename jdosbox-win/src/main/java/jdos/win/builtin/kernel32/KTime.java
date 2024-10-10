package jdos.win.builtin.kernel32;

import jdos.win.builtin.WinAPI;

public class KTime extends WinAPI {
    // LONG WINAPI CompareFileTime(const FILETIME *lpFileTime1, const FILETIME *lpFileTime2)
    static public int CompareFileTime(int lpFileTime1, int lpFileTime2) {
        if (lpFileTime1==0 || lpFileTime2==0) return -1;
        FILETIME x = new FILETIME(lpFileTime1);
        FILETIME y = new FILETIME(lpFileTime2);
        
        if (x.dwHighDateTime > y.dwHighDateTime)
            return 1;
        if (x.dwHighDateTime < y.dwHighDateTime)
            return -1;
        if (x.dwLowDateTime > y.dwLowDateTime)
            return 1;
        if (x.dwLowDateTime < y.dwLowDateTime)
            return -1;
        return 0;
    }
}
