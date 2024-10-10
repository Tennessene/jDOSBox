package jdos.win.builtin.user32;

import jdos.win.builtin.WinAPI;
import jdos.win.system.WinRect;

public class NCCALCSIZE_PARAMS extends WinAPI {
    static final int SIZE = WinRect.SIZE*3+4;

    public NCCALCSIZE_PARAMS(WinRect rect1, WinRect rect2, WinRect rect3, int lppos) {
        rgrc[0] = rect1;
        rgrc[1] = rect2;
        rgrc[2] = rect3;
        this.lppos = lppos;
    }

    public int allocTemp() {
        int result = getTempBuffer(SIZE);
        rgrc[0].write(result);
        rgrc[1].write(result+WinRect.SIZE);
        rgrc[2].write(result+WinRect.SIZE*2);
        writed(result+WinRect.SIZE*3, lppos);
        return result;
    }

    WinRect[] rgrc = new WinRect[3];
    int lppos; // PWINDOWPOS
}
