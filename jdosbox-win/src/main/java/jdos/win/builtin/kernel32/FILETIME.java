package jdos.win.builtin.kernel32;

import jdos.win.builtin.WinAPI;

public class FILETIME extends WinAPI {
    public FILETIME(int address) {
        dwLowDateTime = readd(address);address+=4;
        dwHighDateTime = readd(address);address+=4;
    }

    public int dwLowDateTime;
    public int dwHighDateTime;
}
