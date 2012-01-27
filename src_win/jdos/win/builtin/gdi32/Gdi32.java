package jdos.win.builtin.gdi32;

import jdos.win.loader.BuiltinModule;
import jdos.win.loader.Loader;

public class Gdi32 extends BuiltinModule {
    public Gdi32(Loader loader, int handle) {
        super(loader, "Gdi32.dll", handle);

        add(WinDC.class, "CreateCompatibleDC");
        add(WinFont.class, "CreateFontA");
        add(WinFont.class, "CreateFontIndirectA");
        add(WinPen.class, "CreatePen");
        add(WinPalette.class, "CreatePalette");
        add(WinRegion.class, "CreateRectRgn");
        add(WinBrush.class, "CreateSolidBrush");
        add(WinDC.class, "DeleteDC");
        add(GdiObj.class, "DeleteObject");
        add(GdiObj.class, "GdiSetBatchLimit");
        add(WinDC.class, "GetDeviceCaps");
        add(GdiObj.class, "GetObjectA");
        add(WinPalette.class, "GetPaletteEntries");
        add(WinDC.class, "GetPixel");
        add(GdiObj.class, "GetStockObject");
        add(WinDC.class, "GetSystemPaletteEntries");
        add(WinDC.class, "GetTextExtentPoint32A");
        add(WinDC.class, "GetTextExtentPointA");
        add(WinDC.class, "GetTextMetricsA");
        add(WinDC.class, "RealizePalette");
        add(WinDC.class, "SelectClipRgn");
        add(WinDC.class, "SelectObject");
        add(WinDC.class, "SelectPalette");
        add(WinDC.class, "SetBkColor");
        add(WinDC.class, "SetBkMode");
        add(WinPalette.class, "SetPaletteEntries");
        add(WinDC.class, "SetPixel");
        add(WinDC.class, "SetTextColor");
        add(BitBlt.class, "StretchBlt");
        add(WinDC.class, "TextOutA", new String[] {"hdc", "x", "y", "(STRINGN4)str", "count"});
    }
}
