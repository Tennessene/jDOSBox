package jdos.win.builtin.gdi32;

import jdos.win.builtin.WinAPI;

public class Clipping extends WinAPI {
    // int GetClipRgn(HDC hdc, HRGN hrgn)
    static public int GetClipRgn(int hdc, int hrgn) {
        WinDC dc = WinDC.get(hdc);
        if (dc == null)
            return -1;
        if (dc.hClipRgn == 0)
            return 0;
        if (WinRegion.CombineRgn(hrgn, dc.hClipRgn, 0, RGN_COPY)!=ERROR) {
            return 1;
        }
        return -1;
    }

    // int IntersectClipRect(HDC hdc, int nLeftRect, int nTopRect, int nRightRect, int nBottomRect)
    static public int IntersectClipRect(int hdc, int nLeftRect, int nTopRect, int nRightRect, int nBottomRect) {
        WinDC dc = WinDC.get(hdc);
        if (dc == null)
            return ERROR;
        int rgn = WinRegion.CreateRectRgn(nLeftRect, nTopRect, nRightRect, nBottomRect);
        if (rgn == ERROR)
            return ERROR;
        if (dc.hClipRgn == 0) {
            dc.hClipRgn = rgn;
            return SIMPLEREGION;
        }
        int result = WinRegion.CombineRgn(dc.hClipRgn, dc.hClipRgn, rgn, RGN_AND);
        GdiObj.DeleteObject(rgn);
        return result;
    }
}
