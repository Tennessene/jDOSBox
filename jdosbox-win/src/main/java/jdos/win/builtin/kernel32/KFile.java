package jdos.win.builtin.kernel32;

import jdos.win.Win;
import jdos.win.builtin.WinAPI;
import jdos.win.system.Scheduler;
import jdos.win.system.WinFile;
import jdos.win.system.WinSystem;
import jdos.win.utils.Error;
import jdos.win.utils.FilePath;
import jdos.win.utils.StringUtil;

public class KFile extends WinAPI {
    // HFILE WINAPI OpenFile(LPCSTR lpFileName, LPOFSTRUCT lpReOpenBuff, UINT uStyle)
    static public int OpenFile(int lpFileName, int lpReOpenBuff, int uStyle) {
        String fileName = StringUtil.getString(lpFileName);
            FilePath file = WinSystem.getCurrentProcess().getFile(fileName);
            if ((uStyle & OF_EXIST)!=0) {
                return file.exists()?WinAPI.TRUE:WinAPI.FALSE;
            }
            if ((uStyle & OF_CREATE)!=0) {
                try {
                    file.delete();
                    file.createNewFile();
                } catch (Exception e) {
                    Scheduler.getCurrentThread().setLastError(jdos.win.utils.Error.ERROR_ACCESS_DENIED);
                    return WinAPI.INVALID_HANDLE_VALUE;
                }
            }
            if (!file.exists()) {
                if ((uStyle & OF_PROMPT)!=0)
                    Win.panic("OpenFile OF_PROMPT not implemented yet");
                Scheduler.getCurrentThread().setLastError(Error.ERROR_FILE_NOT_FOUND);
                return WinAPI.INVALID_HANDLE_VALUE;
            }
            if ((uStyle & OF_PARSE)!=0) {
                Win.panic("OpenFile OF_PARSE not implemented yet");
            }
            if ((uStyle & OF_REOPEN)!=0) {
                Win.panic("OpenFile OF_REOPEN not implemented yet");
            }

            if ((uStyle & OF_DELETE)!=0) {
                if (file.delete())
                    return WinAPI.TRUE;
                Scheduler.getCurrentThread().setLastError(Error.ERROR_ACCESS_DENIED);
                return WinAPI.INVALID_HANDLE_VALUE;
            }
            int share = WinFile.FILE_SHARE_READ|WinFile.FILE_SHARE_WRITE;

            if ((uStyle & OF_SHARE_DENY_NONE)==OF_SHARE_DENY_NONE) {
                share = WinFile.FILE_SHARE_READ|WinFile.FILE_SHARE_WRITE;
            } else if ((uStyle & OF_SHARE_DENY_WRITE)==OF_SHARE_DENY_WRITE) {
                share = WinFile.FILE_SHARE_READ;
            } else if ((uStyle & OF_SHARE_DENY_READ)==OF_SHARE_DENY_READ) {
                share = WinFile.FILE_SHARE_WRITE;
            } else if ((uStyle & OF_SHARE_EXCLUSIVE)==OF_SHARE_EXCLUSIVE) {
                share = WinFile.FILE_SHARE_NONE;
            }

            boolean write = false;
            if ((uStyle & OF_WRITE)!=0) {
                write = true;
            }
            if ((uStyle & OF_READWRITE)!=0) {
                write = true;
            }
            WinFile result = WinFile.create(file, write, share, 0);
            if (result != null)
                return result.getHandle();
            else {
                Scheduler.getCurrentThread().setLastError(Error.ERROR_ACCESS_DENIED);
                return WinAPI.INVALID_HANDLE_VALUE;
            }
    }
}
