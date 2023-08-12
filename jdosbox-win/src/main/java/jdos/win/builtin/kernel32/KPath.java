package jdos.win.builtin.kernel32;

import jdos.win.Win;
import jdos.win.builtin.WinAPI;
import jdos.win.system.WinSystem;
import jdos.win.utils.StringUtil;

public class KPath extends WinAPI {
    // UINT WINAPI GetSystemDirectory(LPTSTR lpBuffer, UINT uSize)
    static public int GetSystemDirectoryA(int lpBuffer, int uSize) {
        return StringUtil.strncpy(lpBuffer, "C:\\Windows\\System32", uSize); // verified no trailing slash on WinXP
    }

    // DWORD WINAPI SearchPath(LPCTSTR lpPath, LPCTSTR lpFileName, LPCTSTR lpExtension, DWORD nBufferLength, LPTSTR lpBuffer, LPTSTR *lpFilePart)
    static public int SearchPathA(int lpPath, int lpFileName, int lpExtension, int nBufferLength, int lpBuffer, int lpFilePart) {
        Win.panic("SearchPathA not implemented yet");
        return 0;
    }

    // BOOL WINAPI SetCurrentDirectory(LPCTSTR lpPathName)
    static public int SetCurrentDirectoryA(int lpPathName) {
        WinSystem.getCurrentProcess().currentWorkingDirectory = StringUtil.getString(lpPathName);
        return TRUE;
    }
}
