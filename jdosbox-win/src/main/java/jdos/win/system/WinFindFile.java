package jdos.win.system;

import jdos.hardware.Memory;
import jdos.win.builtin.WinAPI;
import jdos.win.utils.FilePath;
import jdos.win.utils.StringUtil;

public class WinFindFile extends WinObject {
    static public WinFindFile create(FilePath[] results) {
        return new WinFindFile(nextObjectId(), results);
    }

    static public WinFindFile get(int handle) {
        WinObject object = getObject(handle);
        if (object == null || !(object instanceof WinFindFile))
            return null;
        return (WinFindFile)object;
    }

    FilePath[] results;
    int index = 0;

    private WinFindFile(int id, FilePath[] results) {
        super(id);
        this.results = results;
    }

    /*
    DWORD    dwFileAttributes;
    FILETIME ftCreationTime;
    FILETIME ftLastAccessTime;
    FILETIME ftLastWriteTime;
    DWORD    nFileSizeHigh;
    DWORD    nFileSizeLow;
    DWORD    dwReserved0;
    DWORD    dwReserved1;
    TCHAR    cFileName[MAX_PATH];
    TCHAR    cAlternateFileName[14];
    */

    public int getNextResult(int address) {
        if (index>=results.length) {
            Scheduler.getCurrentThread().setLastError(jdos.win.utils.Error.ERROR_NO_MORE_FILES);
            return WinAPI.FALSE;
        }
        FilePath file = results[index++];
        String name = file.getName();
        long timestamp = WinFile.millisToFiletime(file.lastModified());
        long size = file.length();

        Memory.mem_writed(address, 0x80);address+=4; // dwFileAttributes FILE_ATTRIBUTE_NORMAL=0x80
        WinFile.writeFileTime(address, timestamp);address+=8; // ftCreationTime
        WinFile.writeFileTime(address, timestamp);address+=8; // ftLastAccessTime
        WinFile.writeFileTime(address, timestamp);address+=8; // ftLastWriteTime
        Memory.mem_writed(address, (int)(size >>> 32));address+=4; // nFileSizeHigh
        Memory.mem_writed(address, (int)size);address+=4; // nFileSizeLow
        Memory.mem_writed(address, 0);address+=4; // dwReserved0
        Memory.mem_writed(address, 0);address+=4; // dwReserved1
        StringUtil.strcpy(address, name);address+=WinAPI.MAX_PATH; // cFileName
        StringUtil.strncpy(address, name, 14);address+=14; // cAlternateFileName
        return WinAPI.TRUE;
    }
}
