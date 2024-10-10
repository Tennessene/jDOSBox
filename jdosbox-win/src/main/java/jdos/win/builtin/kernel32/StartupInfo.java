package jdos.win.builtin.kernel32;

import jdos.win.builtin.WinAPI;
import jdos.win.utils.StringUtil;

class StartupInfo extends WinAPI {
    static final int SIZE = 68;

    public StartupInfo() {
        cb = SIZE;
    }

    public StartupInfo(int address) {
        cb = readd(address); address+=4;
        lpReserved = readd(address); address+=4;
        lpDesktop = readd(address); address+=4;
        lpTitle = readd(address); address+=4;
        dwX = readd(address); address+=4;
        dwY = readd(address); address+=4;
        dwXSize = readd(address); address+=4;
        dwYSize = readd(address); address+=4;
        dwXCountChars = readd(address); address+=4;
        dwYCountChars = readd(address); address+=4;
        dwFillAttribute = readd(address); address+=4;
        dwFlags = readd(address); address+=4;
        wShowWindow = readw(address); address+=2;
        cbReserved2 = readw(address); address+=2;
        lpReserved2 = readd(address); address+=4;
        hStdInput = readd(address); address+=4;
        hStdOutput = readd(address); address+=4;
        hStdError = readd(address); address+=4;
        if (lpDesktop != 0)
            desktop = StringUtil.getString(lpDesktop);
        if (lpTitle != 0)
            title = StringUtil.getString(lpTitle);
    }

    public void write(int address) {
        writed(address, cb);address+=4;
        writed(address, lpReserved);address+=4;
        writed(address, lpDesktop);address+=4;
        writed(address, lpTitle);address+=4;
        writed(address, dwX);address+=4;
        writed(address, dwY);address+=4;
        writed(address, dwXSize);address+=4;
        writed(address, dwYSize);address+=4;
        writed(address, dwXCountChars);address+=4;
        writed(address, dwYCountChars);address+=4;
        writed(address, dwFillAttribute);address+=4;
        writed(address, dwFlags);address+=4;
        writew(address, wShowWindow);address+=2;
        writew(address, cbReserved2);address+=2;
        writed(address, lpReserved2);address+=4;
        writed(address, hStdInput);address+=4;
        writed(address, hStdOutput);address+=4;
        writed(address, hStdError);address+=4;
    }

    public int allocTemp() {
        int p = getTempBuffer(SIZE);
        write(p);
        return p;
    }

    public String desktop;
    public String title;

    public int cb;
    public int lpReserved;
    public int lpDesktop;
    public int lpTitle;
    public int dwX;
    public int dwY;
    public int dwXSize;
    public int dwYSize;
    public int dwXCountChars;
    public int dwYCountChars;
    public int dwFillAttribute;
    public int dwFlags;
    public int wShowWindow;
    public int cbReserved2;
    public int lpReserved2;
    public int hStdInput;
    public int hStdOutput;
    public int hStdError;
}
