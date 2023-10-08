package jdos.win.builtin.gdi32;

import jdos.win.builtin.WinAPI;

public class Mapping extends WinAPI {
    // BOOL OffsetViewportOrgEx(HDC hdc, int nXOffset, int nYOffset, LPPOINT lpPoint)
    static public void OffsetViewportOrgEx(int hdc, int nXOffset, int nYOffset, int lpPoint) {
        WinDC dc = WinDC.get(hdc);
        if (dc == null)
            return;
        if (lpPoint != 0) {
            writed(lpPoint, dc.x-dc.clipX);
            writed(lpPoint+4, dc.y-dc.clipY);
        }
        dc.x+=nXOffset;
        dc.y+=nYOffset;
    }
}
