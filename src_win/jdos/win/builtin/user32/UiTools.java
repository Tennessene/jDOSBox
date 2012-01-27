package jdos.win.builtin.user32;

import jdos.win.builtin.WinAPI;
import jdos.win.system.WinRect;

public class UiTools extends WinAPI {
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
