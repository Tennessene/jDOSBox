package jdos.win.builtin.kernel32;

import jdos.win.Win;
import jdos.win.builtin.WinAPI;
import jdos.win.system.WinSystem;
import jdos.win.utils.Error;
import jdos.win.utils.FilePath;
import jdos.win.utils.StringUtil;

public class WinPath extends WinAPI {
    // BOOL WINAPI CreateDirectory(LPCTSTR lpPathName, LPSECURITY_ATTRIBUTES lpSecurityAttributes)
    public static int CreateDirectoryA(int lpPathName, int lpSecurityAttributes) {
        String path = StringUtil.getString(lpPathName);
        FilePath file = WinSystem.getCurrentProcess().getFile(path);
        if (!file.exists()) {
            return BOOL(file.mkdirs());
        }
        return FALSE;
    }

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

    // DWORD WINAPI GetShortPathName(LPCTSTR lpszLongPath, LPTSTR lpszShortPath, DWORD cchBuffer)
    public static int GetShortPathNameA(int lpszLongPath, int lpszShortPath, int cchBuffer) {
        faked();
        return StringUtil.strncpy(lpszShortPath, lpszLongPath, cchBuffer);
    }

    // BOOL WINAPI MoveFile(LPCTSTR lpExistingFileName, LPCTSTR lpNewFileName)
    public static int MoveFileA(int lpExistingFileName, int lpNewFileName) {
        if (lpExistingFileName == 0 || lpNewFileName == 0)
            return FALSE;
        String from = StringUtil.getString(lpExistingFileName);
        String to = StringUtil.getString(lpNewFileName);
        FilePath fileFrom = WinSystem.getCurrentProcess().getFile(from);
        FilePath fileTo = WinSystem.getCurrentProcess().getFile(to);
        if (!fileFrom.exists()) {
            SetLastError(Error.ERROR_PATH_NOT_FOUND);
            return FALSE;
        }
        if (fileTo.exists()) {
            SetLastError(Error.ERROR_ALREADY_EXISTS);
            return FALSE;
        }
        if (!fileFrom.renameTo(fileTo)) {
            SetLastError(Error.ERROR_ACCESS_DENIED);
            return FALSE;
        }
        return TRUE;
    }
}
