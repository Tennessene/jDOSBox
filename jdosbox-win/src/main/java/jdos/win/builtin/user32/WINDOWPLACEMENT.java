package jdos.win.builtin.user32;

import jdos.win.builtin.WinAPI;
import jdos.win.system.WinPoint;
import jdos.win.system.WinRect;

public class WINDOWPLACEMENT extends WinAPI {
    static public final int SIZE = 44;
    public WINDOWPLACEMENT() {
    }

    public void write(int address) {
        writed(address, length);address+=4;
        writed(address, flags);address+=4;
        writed(address, showCmd);address+=4;
        ptMinPosition.write(address);address+=WinPoint.SIZE;
        ptMaxPosition.write(address);address+=WinPoint.SIZE;
        rcNormalPosition.write(address);
    }

    int  length = SIZE;
    int  flags;
    int  showCmd;
    WinPoint ptMinPosition = new WinPoint();
    WinPoint ptMaxPosition = new WinPoint();
    WinRect rcNormalPosition = new WinRect();
}
