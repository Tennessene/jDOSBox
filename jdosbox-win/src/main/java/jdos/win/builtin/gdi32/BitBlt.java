package jdos.win.builtin.gdi32;

import jdos.win.Win;
import jdos.win.builtin.WinAPI;

import java.awt.*;
import java.awt.image.BufferedImage;

public class BitBlt extends WinAPI {
    // BOOL BitBlt(HDC hdcDest, int nXDest, int nYDest, int nWidth, int nHeight, HDC hdcSrc, int nXSrc, int nYSrc, DWORD dwRop)
    static public int BitBlt(int hdcDest, int nXDest, int nYDest, int nWidth, int nHeight, int hdcSrc, int nXSrc, int nYSrc, int dwRop) {
        if (!rop_uses_src(dwRop)) return WinDC.PatBlt(hdcDest, nXDest, nYDest, nWidth, nHeight, dwRop);
        else return StretchBlt(hdcDest, nXDest, nYDest, nWidth, nHeight, hdcSrc, nXSrc, nYSrc, nWidth, nHeight, dwRop);
    }

    // BOOL StretchBlt(HDC hdcDest, int nXOriginDest, int nYOriginDest, int nWidthDest, int nHeightDest, HDC hdcSrc, int nXOriginSrc, int nYOriginSrc, int nWidthSrc, int nHeightSrc, DWORD dwRop)
    static public int StretchBlt(int hdcDest, int nXOriginDest, int nYOriginDest, int nWidthDest, int nHeightDest, int hdcSrc, int nXOriginSrc, int nYOriginSrc, int nWidthSrc, int nHeightSrc, int dwRop) {
        WinDC dest = WinDC.get(hdcDest);
        WinDC src = WinDC.get(hdcSrc);
        if (dest == null || src == null)
            return FALSE;

        if (dwRop != SRCCOPY) {
            Win.panic("StretchBlt only supports SRCCOPY");
        }
        Graphics2D g = dest.getGraphics();
        StretchBlt2D(g, dest.x+nXOriginDest, dest.y+nYOriginDest, nWidthDest, nHeightDest, src.getImage(), nXOriginSrc, nYOriginSrc, nWidthSrc, nHeightSrc, dwRop);
        g.dispose();
        return TRUE;
    }

    static public void StretchBlt2D(Graphics2D graphics, int nXOriginDest, int nYOriginDest, int nWidthDest, int nHeightDest, BufferedImage src, int nXOriginSrc, int nYOriginSrc, int nWidthSrc, int nHeightSrc, int dwRop) {
        if (dwRop != SRCCOPY) {
            Win.panic("StretchBlt only supports SRCCOPY");
        }
        graphics.drawImage(src, nXOriginDest, nYOriginDest, nXOriginDest+nWidthDest, nYOriginDest+nHeightDest, nXOriginSrc, nYOriginSrc, nXOriginSrc+nWidthSrc, nYOriginSrc+nHeightSrc, null);
    }

    static private boolean rop_uses_src(int rop) {
        return ((rop >> 2) & 0x330000) != (rop & 0x330000);
    }
}
