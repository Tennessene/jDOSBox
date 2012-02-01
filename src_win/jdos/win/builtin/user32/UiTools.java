package jdos.win.builtin.user32;

import jdos.hardware.Memory;
import jdos.win.builtin.WinAPI;
import jdos.win.system.WinRect;

public class UiTools extends WinAPI {
    // BOOL CopyRect(LPRECT lprcDst, const RECT *lprcSrc)
    static public int CopyRect(int lprcDst, int lprcSrc) {
        if (lprcDst == 0 || lprcSrc==0)
            return FALSE;
        Memory.mem_memcpy(lprcDst, lprcSrc, WinRect.SIZE);
        return TRUE;
    }

    // BOOL OffsetRect(LPRECT lprc, int dx, int dy)
    static public int OffsetRect(int lprc, int dx, int dy) {
        WinRect rect = new WinRect(lprc);
        rect.left+=dx;
        rect.right+=dx;
        rect.top+=dy;
        rect.bottom+=dy;
        rect.write(lprc);
        return TRUE;
    }

    // BOOL SetRect(LPRECT lprc, int xLeft, int yTop, int xRight, int yBottom)
    static public int SetRect(int lprc, int xLeft, int yTop, int xRight, int yBottom) {
        WinRect.write(lprc, xLeft, yTop, xRight, yBottom);
        return TRUE;
    }
}
