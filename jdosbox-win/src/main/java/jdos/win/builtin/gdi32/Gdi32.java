package jdos.win.builtin.gdi32;

import jdos.win.loader.BuiltinModule;
import jdos.win.loader.Loader;

public class Gdi32 extends BuiltinModule {
    public Gdi32(Loader loader, int handle) {
        super(loader, "Gdi32.dll", handle);

        add(BitBlt.class, "BitBlt", LOG_GDI?new String[] {"hdcDest", "nXDest", "nYDest", "nWidth", "nHeight", "hdcSrc", "nXSrc", "nYSrc", "dwRop", "(BOOL)result"}:null);
        add(WinBitmap.class, "CreateBitmap", LOG_GDI?new String[] {"nWidth", "nHeight", "cPlanes", "cBitsPerPel", "(HEX)lpvBits"}:null);
        add(WinBitmap.class, "CreateCompatibleBitmap", LOG_GDI?new String[] {"hdc", "nWidth", "nHeight"}:null);
        add(WinDC.class, "CreateCompatibleDC", LOG_GDI?new String[] {"hdc"}:null);
        add(WinFont.class, "CreateFontA", LOG_GDI?new String[] {"nHeight", "nWidth", "nEscapement", "nOrientation", "fnWeight", "fdwItalic", "fdwUnderline", "fdwStrikeOut", "fdwCharSet", "fdwOutputPrecision", "fdwClipPrecision", "fdwQuality", "fdwPitchAndFamily", "(STRING)lpszFace"}:null);
        add(WinFont.class, "CreateFontW", LOG_GDI?new String[] {"nHeight", "nWidth", "nEscapement", "nOrientation", "fnWeight", "fdwItalic", "fdwUnderline", "fdwStrikeOut", "fdwCharSet", "fdwOutputPrecision", "fdwClipPrecision", "fdwQuality", "fdwPitchAndFamily", "(STRINGW)lpszFace"}:null);
        add(WinFont.class, "CreateFontIndirectA", LOG_GDI?new String[] {"(LOGFONT)lplf"}:null);
        add(WinPen.class, "CreatePen", LOG_GDI?new String[] {"(HEX)fnPenStyle", "nWidth", "(HEX)crColor"}:null);
        add(WinPalette.class, "CreatePalette", LOG_GDI?new String[] {"(HEX)lplgpl"}:null);
        add(WinRegion.class, "CreateRectRgn", LOG_GDI?new String[] {"left", "top", "right", "bottom"}:null);
        add(WinBrush.class, "CreateSolidBrush", LOG_GDI?new String[] {"(HEX)crColor"}:null);
        add(WinDC.class, "DeleteDC", LOG_GDI?new String[] {"hdc", "(BOOL)result"}:null);
        add(GdiObj.class, "DeleteObject", LOG_GDI?new String[] {"(GDI)obj"}:null);
        add(WinPen.class, "ExtCreatePen", LOG_GDI?new String[] {"(HEX)dwPenStyle", "dwWidth", "(HEX)lplb", "dwStyleCount", "(HEX)lpStyle"}:null);
        add(WinDC.class, "ExtTextOutA", LOG_GDI?new String[] {"hdc", "X", "Y", "(HEX)fuOptions", "(RECT)lprc", "(STRINGN6)lpString", "cbCount", "(HEX)lpDx", "(BOOL)result"}:null);
        add(GdiObj.class, "GdiSetBatchLimit", LOG_GDI?new String[] {"dwLimit"}:null);
        add(Clipping.class, "GetClipRgn", LOG_GDI?new String[] {"hdc", "hrgn"}:null);
        add(GdiObj.class, "GetCurrentObject", LOG_GDI?new String[] {"hdc", "uObjectType"}:null);
        add(WinDC.class, "GetDeviceCaps", LOG_GDI?new String[] {"hdc", "nIndex"}:null);
        add(Dib.class, "GetDIBColorTable", LOG_GDI?new String[] {"hdc", "uStartIndex", "cEntries", "(HEX)pColors"}:null);
        add(GdiObj.class, "GetObjectA", LOG_GDI?new String[] {"(GDI)hgdiobj", "cbBuffer", "(HEX)lpvObject"}:null);
        add(WinPalette.class, "GetPaletteEntries", LOG_GDI?new String[] {"(GDI)hpal", "iStartIndex", "nEntries", "(HEX)lppe"}:null);
        add(WinDC.class, "GetPixel", LOG_GDI?new String[] {"hdc", "nXPos", "nYPos", "(HEX)result"}:null);
        add(GdiObj.class, "GetStockObject", LOG_GDI?new String[] {"fnObject", "(GDI)result"}:null);
        add(WinDC.class, "GetSystemPaletteEntries", LOG_GDI?new String[] {"hdc", "iStartIndex", "nEntries", "lppe"}:null);
        add(WinPalette.class, "GetSystemPaletteUse", LOG_GDI?new String[] {"hdc"}:null);
        add(WinDC.class, "GetTextColor", LOG_GDI?new String[] {"hdc", "(HEX)result"}:null);
        add(WinFont.class, "GetTextExtentExPointA", LOG_GDI?new String[] {"hdc", "(STRINGN2)lpszStr", "cchString", "nMaxExtent", "(HEX)lpnFit", "(HEX)alpDx", "(HEX)lpSize", "(BOO)result", "06(SIZE)lpSize"}:null);
        add(WinFont.class, "GetTextExtentPoint32A", LOG_GDI?new String[] {"hdc", "(STRINGN2)lpString", "cbString", "(HEX)lpSize", "result", "03(SIZE)lpSize"}:null);
        add(WinFont.class, "GetTextExtentPointA", LOG_GDI?new String[] {"hdc", "(STRINGN2)lpString", "cbString", "(HEX)lpSize", "result", "03(SIZE)lpSize"}:null);
        add(WinFont.class, "GetTextMetricsA", LOG_GDI?new String[] {"hdc", "(HEX)lptm", "(BOOL)result", "01(TM)lptm"}:null);
        add(Clipping.class, "IntersectClipRect", LOG_GDI?new String[] {"hdc", "nLeftRect", "nTopRect", "nRightRect", "nBottomRect"}:null);
        add(PaintingGDI.class, "LineTo", LOG_GDI?new String[] {"hdc", "nXEnd", "nYEnd", "(BOOL)result"}:null);
        add(PaintingGDI.class, "MoveToEx", LOG_GDI?new String[] {"hdc", "X", "Y", "(HEX)lpPoint", "(BOOL)result", "03(POINT)lpPoint"}:null);
        add(Mapping.class, "OffsetViewportOrgEx", LOG_GDI?new String[] {"hdc", "nXOffset", "nYOffset", "(HEX)lpPoint", "(BOOL)result", "03(POINT)lpPoint"}:null);
        add(PaintingGDI.class, "Polygon", LOG_GDI?new String[] {"hdc", "(POINT)lpPoints", "nCount", "(BOOL)result"}:null);
        add(WinPalette.class, "ResizePalette", LOG_GDI?new String[] {"hpal", "nEntries", "(BOOL)result"}:null);
        add(WinDC.class, "RealizePalette", LOG_GDI?new String[] {"hdc"}:null);
        add(WinDC.class, "SelectClipRgn", LOG_GDI?new String[] {"hdc", "(GDI)hrgn", "result"}:null);
        add(WinDC.class, "SelectObject", LOG_GDI?new String[] {"hdc", "(GDI)hgdiobj", "(GDI)result"}:null);
        add(WinDC.class, "SelectPalette", LOG_GDI?new String[] {"hdc", "(GDI)hpal", "(BOOL)bForceBackground", "(GDI)result"}:null);
        add(WinDC.class, "SetBkColor", LOG_GDI?new String[] {"hdc", "(HEX)crColor"}:null);
        add(WinDC.class, "SetBkMode", LOG_GDI?new String[] {"hdc", "(HEX)iBkMode", "(HEX)result"}:null);
        add(Dib.class, "SetDIBitsToDevice", LOG_GDI?new String[] {"hdc", "XDest", "YDest", "nDestWidth", "nDestHeight", "XSrc", "YSrc", "uStartScan", "cScanLines", "(HEX)lpBits", "(HEX)lpBitsInfo", "iUsage"}:null);
        add(WinPalette.class, "SetPaletteEntries", LOG_GDI?new String[] {"(GDI)hpal", "iStart", "cbEntries", "(HEX)lppe"}:null);
        add(WinDC.class, "SetPixel", LOG_GDI?new String[] {"hdc", "X", "Y", "(HEX)crColor", "(HEX)result"}:null);
        add(WinDC.class, "SetROP2", LOG_GDI?new String[] {"hdc", "fnDrawMode"}:null);
        add(WinPalette.class, "SetSystemPaletteUse", LOG_GDI?new String[] {"hdc", "uUsage"}:null);
        add(WinDC.class, "SetTextColor", LOG_GDI?new String[] {"hdc", "(HEX)crColor", "(HEX)result"}:null);
        add(BitBlt.class, "StretchBlt", LOG_GDI?new String[] {"hdcDest", "nXOriginDest", "nYOriginDest", "nWidthDest", "nHeightDest", "hdcSrc", "nXOriginSrc", "nYOriginSrc", "nWidthSrc", "nHeightSrc", "(HEX)dwRop"}:null);
        add(Dib.class, "StretchDIBits", LOG_GDI?new String[] {"hdc", "XDest", "YDest", "nDestWidth", "nDestHeight", "XSrc", "YSrc", "nSrcWidth", "nSrcHeight", "(HEX)lpBits", "(HEX)lpBitsInfo", "iUsage", "(HEX)dwRop"}:null);
        add(WinDC.class, "TextOutA", LOG_GDI?new String[] {"hdc", "x", "y", "(STRINGN4)str", "count"}:null);
    }
}
