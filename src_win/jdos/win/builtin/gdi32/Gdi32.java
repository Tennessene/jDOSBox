package jdos.win.builtin.gdi32;

import jdos.win.loader.BuiltinModule;
import jdos.win.loader.Loader;

public class Gdi32 extends BuiltinModule {
    public Gdi32(Loader loader, int handle) {
        super(loader, "Gdi32.dll", handle);

        add(BitBlt.class, "BitBlt", new String[] {"hdcDest", "nXDest", "nYDest", "nWidth", "nHeight", "hdcSrc", "nXSrc", "nYSrc", "dwRop", "(BOOL)result"});
        add(WinBitmap.class, "CreateBitmap", new String[] {"nWidth", "nHeight", "cPlanes", "cBitsPerPel", "(HEX)lpvBits"});
        add(WinDC.class, "CreateCompatibleDC", new String[] {"hdc"});
        add(WinFont.class, "CreateFontA", new String[] {"nHeight", "nWidth", "nEscapement", "nOrientation", "fnWeight", "fdwItalic", "fdwUnderline", "fdwStrikeOut", "fdwCharSet", "fdwOutputPrecision", "fdwClipPrecision", "fdwQuality", "fdwPitchAndFamily", "(STRING)lpszFace"});
        add(WinFont.class, "CreateFontW", new String[] {"nHeight", "nWidth", "nEscapement", "nOrientation", "fnWeight", "fdwItalic", "fdwUnderline", "fdwStrikeOut", "fdwCharSet", "fdwOutputPrecision", "fdwClipPrecision", "fdwQuality", "fdwPitchAndFamily", "(STRINGW)lpszFace"});
        add(WinFont.class, "CreateFontIndirectA", new String[] {"(LOGFONT)lplf"});
        add(WinPen.class, "CreatePen", new String[] {"(HEX)fnPenStyle", "nWidth", "(HEX)crColor"});
        add(WinPalette.class, "CreatePalette", new String[] {"(HEX)lplgpl"});
        add(WinRegion.class, "CreateRectRgn", new String[] {"left", "top", "right", "bottom"});
        add(WinBrush.class, "CreateSolidBrush", new String[] {"(HEX)crColor"});
        add(WinDC.class, "DeleteDC", new String[] {"hdc", "(BOOL)result"});
        add(GdiObj.class, "DeleteObject", new String[] {"(GDI)obj"});
        add(WinPen.class, "ExtCreatePen", new String[] {"(HEX)dwPenStyle", "dwWidth", "(HEX)lplb", "dwStyleCount", "(HEX)lpStyle"});
        add(GdiObj.class, "GdiSetBatchLimit", new String[] {"dwLimit"});
        add(Clipping.class, "GetClipRgn", new String[] {"hdc", "hrgn"});
        add(GdiObj.class, "GetCurrentObject", new String[] {"hdc", "uObjectType"});
        add(WinDC.class, "GetDeviceCaps", new String[] {"hdc", "nIndex"});
        add(GdiObj.class, "GetObjectA", new String[] {"(GDI)hgdiobj", "cbBuffer", "(HEX)lpvObject"});
        add(WinPalette.class, "GetPaletteEntries", new String[] {"(GDI)hpal", "iStartIndex", "nEntries", "(HEX)lppe"});
        add(WinDC.class, "GetPixel", new String[] {"hdc", "nXPos", "nYPos", "(HEX)result"});
        add(GdiObj.class, "GetStockObject", new String[] {"fnObject", "(GDI)result"});
        add(WinDC.class, "GetSystemPaletteEntries", new String[] {"hdc", "iStartIndex", "nEntries", "lppe"});
        add(WinPalette.class, "GetSystemPaletteUse", new String[] {"hdc"});
        add(WinDC.class, "GetTextColor", new String[] {"hdc", "(HEX)result"});
        add(WinFont.class, "GetTextExtentExPointA", new String[] {"hdc", "(STRINGN2)lpszStr", "cchString", "nMaxExtent", "(HEX)lpnFit", "(HEX)alpDx", "(HEX)lpSize", "(BOO)result", "06(SIZE)lpSize"});
        add(WinFont.class, "GetTextExtentPoint32A", new String[] {"hdc", "(STRINGN2)lpString", "cbString", "(HEX)lpSize", "result", "03(SIZE)lpSize"});
        add(WinFont.class, "GetTextExtentPointA", new String[] {"hdc", "(STRINGN2)lpString", "cbString", "(HEX)lpSize", "result", "03(SIZE)lpSize"});
        add(WinFont.class, "GetTextMetricsA", new String[] {"hdc", "(HEX)lptm", "(BOOL)result", "01(TM)lptm"});
        add(Clipping.class, "IntersectClipRect", new String[] {"hdc", "nLeftRect", "nTopRect", "nRightRect", "nBottomRect"});
        add(PaintingGDI.class, "LineTo", new String[] {"hdc", "nXEnd", "nYEnd", "(BOOL)result"});
        add(PaintingGDI.class, "MoveToEx", new String[] {"hdc", "X", "Y", "(HEX)lpPoint", "(BOOL)result", "03(POINT)lpPoint"});
        add(Mapping.class, "OffsetViewportOrgEx", new String[] {"hdc", "nXOffset", "nYOffset", "(HEX)lpPoint", "(BOOL)result", "03(POINT)lpPoint"});
        add(PaintingGDI.class, "Polygon", new String[] {"hdc", "(POINT)lpPoints", "nCount", "(BOOL)result"});
        add(WinDC.class, "RealizePalette", new String[] {"hdc"});
        add(WinDC.class, "SelectClipRgn", new String[] {"hdc", "(GDI)hrgn", "result"});
        add(WinDC.class, "SelectObject", new String[] {"hdc", "(GDI)hgdiobj", "(GDI)result"});
        add(WinDC.class, "SelectPalette", new String[] {"hdc", "(GDI)hpal", "(BOOL)bForceBackground", "(GDI)result"});
        add(WinDC.class, "SetBkColor", new String[] {"hdc", "(HEX)crColor"});
        add(WinDC.class, "SetBkMode", new String[] {"hdc", "(HEX)iBkMode", "(HEX)result"});
        add(WinPalette.class, "SetPaletteEntries", new String[] {"(GDI)hpal", "iStart", "cbEntries", "(HEX)lppe"});
        add(WinDC.class, "SetPixel", new String[] {"hdc", "X", "Y", "(HEX)crColor", "(HEX)result"});
        add(WinDC.class, "SetROP2", new String[] {"hdc", "fnDrawMode"});
        add(WinDC.class, "SetTextColor", new String[] {"hdc", "(HEX)crColor", "(HEX)result"});
        add(BitBlt.class, "StretchBlt", new String[] {"hdcDest", "nXOriginDest", "nYOriginDest", "nWidthDest", "nHeightDest", "hdcSrc", "nXOriginSrc", "nYOriginSrc", "nWidthSrc", "nHeightSrc", "(HEX)dwRop"});
        add(WinDC.class, "TextOutA", new String[] {"hdc", "x", "y", "(STRINGN4)str", "count"});
    }
}
