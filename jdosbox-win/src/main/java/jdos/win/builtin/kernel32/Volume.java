package jdos.win.builtin.kernel32;

import jdos.win.builtin.WinAPI;

public class Volume extends WinAPI {
    // DWORD WINAPI GetLogicalDrives(void);
    static public int GetLogicalDrives() {
        log("GetLogicalDrives faked");
        return 0x4; // for now just drive c
    }
}
