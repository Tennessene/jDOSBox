package jdos.win.builtin.gdi32;

import jdos.win.Win;
import jdos.win.builtin.WinAPI;

import java.awt.*;
import java.awt.image.BufferedImage;

public class BitBlt extends WinAPI {
    // BOOL StretchBlt(HDC hdcDest, int nXOriginDest, int nYOriginDest, int nWidthDest, int nHeightDest, HDC hdcSrc, int nXOriginSrc, int nYOriginSrc, int nWidthSrc, int nHeightSrc, DWORD dwRop)
    static public int StretchBlt(int hdcDest, int nXOriginDest, int nYOriginDest, int nWidthDest, int nHeightDest, int hdcSrc, int nXOriginSrc, int nYOriginSrc, int nWidthSrc, int nHeightSrc, int dwRop) {
        WinDC dest = WinDC.get(hdcDest);
        WinDC src = WinDC.get(hdcSrc);
        if (dest == null || src == null)
            return FALSE;

        if (dwRop != SRCCOPY) {
            Win.panic("StretchBlt only supports SRCCOPY");
        }
        BufferedImage d = dest.getImage();
        BufferedImage s = src.getImage();
        Graphics g = d.getGraphics();
        g.drawImage(s, nXOriginDest, nYOriginDest, nXOriginDest+nWidthDest, nYOriginDest+nHeightDest, nXOriginSrc, nYOriginSrc, nXOriginSrc+nWidthSrc, nYOriginSrc+nHeightSrc, null);
        g.dispose();
        return TRUE;
    }
}
