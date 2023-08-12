package jdos.win.builtin.gdi32;

import jdos.win.builtin.WinAPI;

import java.awt.*;

public class Dib extends WinAPI {
    // UINT GetDIBColorTable(HDC hdc, UINT uStartIndex, UINT cEntries, RGBQUAD *pColors)
    static public int GetDIBColorTable(int hdc, int uStartIndex, int cEntries, int pColors) {
        WinDC dc = WinDC.get(hdc);
        if (dc == null || dc.hBitmap == 0)
            return 0;
        WinBitmap bitmap = WinBitmap.get(dc.hBitmap);
        if (bitmap == null || bitmap.palette == null || bitmap.palette.length==0)
            return 0;
        if (uStartIndex+cEntries>bitmap.palette.length)
            cEntries = bitmap.palette.length - uStartIndex;
        for (int i=uStartIndex;i<uStartIndex+cEntries;i++) {
            writed(pColors+4*(i-uStartIndex), bitmap.palette[i]);
        }
        return cEntries;
    }

    // int SetDIBitsToDevice(HDC hdc, int XDest, int YDest, DWORD dwWidth, DWORD dwHeight, int XSrc, int YSrc, UINT uStartScan, UINT cScanLines, const VOID *lpvBits, const BITMAPINFO *lpbmi, UINT fuColorUse)
    static public int SetDIBitsToDevice(int hdc, int XDest, int YDest, int dwWidth, int dwHeight, int XSrc, int YSrc, int uStartScan, int cScanLines, int lpvBits, int lpbmi, int fuColorUse) {
        return StretchDIBits(hdc, XDest, YDest, dwWidth, dwHeight, XSrc, YSrc, dwWidth, dwHeight, lpvBits, lpbmi, fuColorUse, SRCCOPY);
    }

    // int StretchDIBits(HDC hdc, int XDest, int YDest, int nDestWidth, int nDestHeight, int XSrc, int YSrc, int nSrcWidth, int nSrcHeight, const VOID *lpBits, const BITMAPINFO *lpBitsInfo, UINT iUsage, DWORD dwRop)
    static public int StretchDIBits(int hdc, int XDest, int YDest, int nDestWidth, int nDestHeight, int XSrc, int YSrc, int nSrcWidth, int nSrcHeight, int lpBits, int lpBitsInfo, int iUsage, int dwRop) {
        WinDC dc = WinDC.get(hdc);
        if (dc == null)
            return 0;
        WinBitmap bitmap = new WinBitmap(0, lpBitsInfo, iUsage, dc.hPalette, false);
        bitmap.bits = lpBits;
        Graphics2D g = dc.getGraphics();
        BitBlt.StretchBlt2D(g, dc.x+XDest, dc.x+YDest, nDestWidth, nDestHeight, bitmap.createJavaBitmap(true).getImage(), XSrc, bitmap.height-YSrc-1-nSrcHeight, nSrcWidth, nSrcHeight, dwRop);
        g.dispose();
        return nDestHeight;
    }
}
