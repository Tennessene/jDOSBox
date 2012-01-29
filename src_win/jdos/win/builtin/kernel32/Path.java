package jdos.win.builtin.kernel32;

import jdos.win.Win;
import jdos.win.builtin.WinAPI;
import jdos.win.utils.StringUtil;

public class Path extends WinAPI {
    // DWORD WINAPI GetFullPathName(LPCTSTR lpFileName, DWORD nBufferLength, LPTSTR lpBuffer, LPTSTR *lpFilePart)
    public static int GetFullPathNameA(int lpFileName, int nBufferLength, int lpBuffer, int lpFilePart) {
        String name = StringUtil.getString(lpFileName);
        if (name.charAt(1)!=':') {
            Win.panic("Kernel32.GetFullPathName wasn't expecting a relative path");
        }
        if (lpFilePart != 0) {
            int pos = name.lastIndexOf("\\");
            if (pos<0)
                Win.panic("Kernel32.GetFullPathNameA unexpected file part");
            writed(lpFilePart, lpBuffer+pos);
        }
        StringUtil.strncpy(lpBuffer, name, nBufferLength);
        return name.length();
    }
}
