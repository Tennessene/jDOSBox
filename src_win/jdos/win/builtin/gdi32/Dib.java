package jdos.win.builtin.gdi32;

import jdos.win.builtin.WinAPI;

import java.awt.*;

public class Dib extends WinAPI {
    // int StretchDIBits(HDC hdc, int XDest, int YDest, int nDestWidth, int nDestHeight, int XSrc, int YSrc, int nSrcWidth, int nSrcHeight, const VOID *lpBits, const BITMAPINFO *lpBitsInfo, UINT iUsage, DWORD dwRop)
    static public int StretchDIBits(int hdc, int XDest, int YDest, int nDestWidth, int nDestHeight, int XSrc, int YSrc, int nSrcWidth, int nSrcHeight, int lpBits, int lpBitsInfo, int iUsage, int dwRop) {
        WinDC dc = WinDC.get(hdc);
        if (dc == null)
            return 0;
        WinBitmap bitmap = new WinBitmap(0, lpBitsInfo, iUsage, dc.hPalette, false);
        bitmap.bits = lpBits;
        Graphics2D g = dc.getGraphics();
        BitBlt.StretchBlt2D(g, XDest, YDest, nDestWidth, nDestHeight, bitmap.createJavaBitmap(true).getImage(), XSrc, bitmap.height-YSrc-1-nSrcHeight, nSrcWidth, nSrcHeight, dwRop);
        g.dispose();
        return nDestHeight;
    }
}
