package jdos.win.builtin;

import jdos.cpu.CPU;
import jdos.cpu.CPU_Regs;
import jdos.cpu.Callback;
import jdos.hardware.Memory;
import jdos.win.Console;
import jdos.win.Win;
import jdos.win.loader.BuiltinModule;
import jdos.win.loader.Loader;
import jdos.win.loader.winpe.LittleEndianFile;
import jdos.win.utils.Error;
import jdos.win.utils.*;

import java.awt.*;
import java.awt.image.BufferedImage;

public class Gdi32 extends BuiltinModule {
    public Gdi32(Loader loader, int handle) {
        super(loader, "Gdi32.dll", handle);

        add(CreateCompatibleDC);
        add(CreatePalette);
        add(DeleteDC);
        add(DeleteObject);
        add(GetObjectA);
        add(GdiSetBatchLimit);
        add(GetStockObject);
        add(GetTextExtentPointA);
        add(RealizePalette);
        add(SelectObject);
        add(SelectPalette);
        add(SetBkColor);
        add(SetPaletteEntries);
        add(SetTextColor);
        add(StretchBlt);
        add(TextOutA);
    }

    // HDC CreateCompatibleDC(HDC hdc)
    private Callback.Handler CreateCompatibleDC = new HandlerBase() {
        public java.lang.String getName() {
            return "Gdi32.CreateCompatibleDC";
        }
        public void onCall() {
            int hdc = CPU.CPU_Pop32();
            WinDC dc = null;
            if (hdc != 0) {
                WinObject object = WinSystem.getObject(hdc);
                if (object == null || !(object instanceof WinDC)) {
                    CPU_Regs.reg_eax.dword = 0;
                    WinSystem.getCurrentThread().setLastError(Error.ERROR_INVALID_HANDLE);
                    return;
                }
                dc = (WinDC)object;
            }
            CPU_Regs.reg_eax.dword = WinSystem.createDC(dc, 0, 0, 0, null).getHandle();
        }
    };

    // HPALETTE CreatePalette(const LOGPALETTE *lplgpl)
    private Callback.Handler CreatePalette = new HandlerBase() {
        public java.lang.String getName() {
            return "Gdi32.CreatePalette";
        }
        public void onCall() {
            int lplgpl = CPU.CPU_Pop32();
            if (lplgpl == 0) {
                WinSystem.getCurrentThread().setLastError(Error.ERROR_INVALID_PARAMETER);
                CPU_Regs.reg_eax.dword = 0;
                return;
            }
            int count = Memory.mem_readw(lplgpl+2);
            int[] palette = new int[count];
            for (int i=0;i<count;i++) {
                int address = lplgpl+4+4*i;
                palette[i] = Memory.mem_readd(address) & 0xFFFFFF; // strip out the flag
            }
            CPU_Regs.reg_eax.dword = WinSystem.createPalette(palette).getHandle();
        }
    };

    // BOOL DeleteDC(HDC hdc)
    private Callback.Handler DeleteDC = new HandlerBase() {
        public java.lang.String getName() {
            return "Gdi32.DeleteDC";
        }
        public void onCall() {
            int hdc = CPU.CPU_Pop32();
            if (hdc == 0) {
                CPU_Regs.reg_eax.dword = WinAPI.FALSE;
            } else {
                WinDC dc = null;
                WinObject object = WinSystem.getObject(hdc);
                if (object == null || !(object instanceof WinDC)) {
                    CPU_Regs.reg_eax.dword = WinAPI.FALSE;
                    WinSystem.getCurrentThread().setLastError(Error.ERROR_INVALID_HANDLE);
                    return;
                }
                dc = (WinDC)object;
                dc.close();
                CPU_Regs.reg_eax.dword = WinAPI.TRUE;
            }
        }
    };

    // BOOL DeleteObject(HGDIOBJ hObject)
    private Callback.Handler DeleteObject = new HandlerBase() {
        public java.lang.String getName() {
            return "Gdi32.DeleteObject";
        }
        public void onCall() {
            int hObject = CPU.CPU_Pop32();
            if (hObject == 0) {
                CPU_Regs.reg_eax.dword = WinAPI.FALSE;
            } else {
                WinGDI gdi = null;
                WinObject object = WinSystem.getObject(hObject);
                if (object == null || !(object instanceof WinGDI)) {
                    CPU_Regs.reg_eax.dword = WinAPI.FALSE;
                    WinSystem.getCurrentThread().setLastError(Error.ERROR_INVALID_HANDLE);
                    return;
                }
                gdi = (WinGDI)object;
                gdi.close();
                CPU_Regs.reg_eax.dword = WinAPI.TRUE;
            }
        }
    };

    // DWORD GdiSetBatchLimit(DWORD dwLimit)
    private Callback.Handler GdiSetBatchLimit = new HandlerBase() {
        public java.lang.String getName() {
            return "Gdi32.GdiSetBatchLimit";
        }
        public void onCall() {
            int dwLimit = CPU.CPU_Pop32();
            System.out.println("Faking "+getName());
            CPU_Regs.reg_eax.dword = 0;
        }
    };

    // int GetObject(HGDIOBJ hgdiobj, int cbBuffer, LPVOID lpvObject)
    private Callback.Handler GetObjectA = new HandlerBase() {
        public java.lang.String getName() {
            return "Gdi32.GetObjectA";
        }
        public void onCall() {
            int hgdiobj = CPU.CPU_Pop32();
            int cbBuffer = CPU.CPU_Pop32();
            int lpvObject = CPU.CPU_Pop32();
            WinObject object = WinSystem.getObject(hgdiobj);
            if (object == null || !(object instanceof WinGDI)) {
                WinSystem.getCurrentThread().setLastError(Error.ERROR_INVALID_HANDLE);
                CPU_Regs.reg_eax.dword = 0;
            } else if (object instanceof WinBitmap) {
                WinBitmap bitmap = (WinBitmap)object;
                CPU_Regs.reg_eax.dword = bitmap.get(lpvObject, cbBuffer);
            } else {
                Win.panic(getName()+" not implemented yet for type "+object);
            }
        }
    };

    // HGDIOBJ GetStockObject(int fnObject)
    private Callback.Handler GetStockObject = new HandlerBase() {
        public java.lang.String getName() {
            return "Gdi32.GetStockObject";
        }
        public void onCall() {
            int fnObject = CPU.CPU_Pop32();
            switch (fnObject) {
                case 0: // WHITE_BRUSH
                case 1: // LTGRAY_BRUSH
                case 2: // GRAY_BRUSH
                case 3: // DKGRAY_BRUSH
                case 4: // BLACK_BRUSH
                case 5: // NULL_BRUSH
                case 6: // WHITE_PEN
                case 7: // BLACK_PEN
                case 8: // NULL_PEN
                case 10: // OEM_FIXED_FONT
                case 11: // ANSI_FIXED_FONT
                case 12: // ANSI_VAR_FONT
                case 13: // SYSTEM_FONT
                case 14: // DEVICE_DEFAULT_FONT
                case 15: // DEFAULT_PALETTE
                case 16: // SYSTEM_FIXED_FONT
                    Console.out("GetStockObject faked");
                    break;
                default:
                    Console.out("Unknown GetStockObject "+fnObject);
                    notImplemented();
            }
            CPU_Regs.reg_eax.dword = 1;
        }
    };

    // BOOL GetTextExtentPoint(HDC hdc, LPCTSTR lpString, int cbString, LPSIZE lpSize)
    private Callback.Handler GetTextExtentPointA = new HandlerBase() {
        public java.lang.String getName() {
            return "Gdi32.GetTextExtentPointA";
        }
        public void onCall() {
            int hdc = CPU.CPU_Pop32();
            int lpString = CPU.CPU_Pop32();
            int cbString = CPU.CPU_Pop32();
            int lpSize = CPU.CPU_Pop32();
            WinObject object = WinSystem.getObject(hdc);
            if (object == null || !(object instanceof WinDC)) {
                WinSystem.getCurrentThread().setLastError(Error.ERROR_INVALID_PARAMETER);
                CPU_Regs.reg_eax.dword = 0;
                return;
            }
            CPU_Regs.reg_eax.dword = ((WinDC)object).gtTextExtent(new LittleEndianFile(lpString).readCString(cbString), lpSize);
        }
    };

    // UINT RealizePalette(HDC hdc)
    private Callback.Handler RealizePalette = new HandlerBase() {
        public java.lang.String getName() {
            return "Gdi32.RealizePalette";
        }
        public void onCall() {
            int hdc = CPU.CPU_Pop32();
            WinObject object = WinSystem.getObject(hdc);
            if (object == null || !(object instanceof WinDC)) {
                WinSystem.getCurrentThread().setLastError(Error.ERROR_INVALID_PARAMETER);
                CPU_Regs.reg_eax.dword = 0;
                return;
            }
            CPU_Regs.reg_eax.dword = ((WinDC)object).realizePalette();
        }
    };

    // HGDIOBJ SelectObject(HDC hdc, HGDIOBJ hgdiobj)
    private Callback.Handler SelectObject = new HandlerBase() {
        public java.lang.String getName() {
            return "Gdi32.SelectObject";
        }
        public void onCall() {
            int hdc = CPU.CPU_Pop32();
            int hgdiobj = CPU.CPU_Pop32();
            WinObject object = WinSystem.getObject(hdc);
            if (object == null || !(object instanceof WinDC)) {
                WinSystem.getCurrentThread().setLastError(Error.ERROR_INVALID_PARAMETER);
                CPU_Regs.reg_eax.dword = 0;
                return;
            }
            WinDC dc = (WinDC)object;
            object = WinSystem.getObject(hgdiobj);
            if (object == null || !(object instanceof WinGDI)) {
                WinSystem.getCurrentThread().setLastError(Error.ERROR_INVALID_PARAMETER);
                CPU_Regs.reg_eax.dword = 0;
                return;
            }
            WinGDI gdi = (WinGDI)object;
            CPU_Regs.reg_eax.dword = dc.select(gdi);
        }
    };

    // HPALETTE SelectPalette(HDC hdc, HPALETTE hpal, BOOL bForceBackground)
    private Callback.Handler SelectPalette = new HandlerBase() {
        public java.lang.String getName() {
            return "Gdi32.SelectPalette";
        }
        public void onCall() {
            int hdc = CPU.CPU_Pop32();
            int hpal = CPU.CPU_Pop32();
            int bForceBackground = CPU.CPU_Pop32();

            WinObject object = WinSystem.getObject(hdc);
            if (object == null || !(object instanceof WinDC)) {
                WinSystem.getCurrentThread().setLastError(Error.ERROR_INVALID_PARAMETER);
                CPU_Regs.reg_eax.dword = 0;
                return;
            }
            WinDC dc = (WinDC)object;
            object = WinSystem.getObject(hpal);
            if (object == null || !(object instanceof WinPalette)) {
                WinSystem.getCurrentThread().setLastError(Error.ERROR_INVALID_PARAMETER);
                CPU_Regs.reg_eax.dword = 0;
                return;
            }
            System.out.println(getName()+" faked");
            CPU_Regs.reg_eax.dword = dc.selectPalette((WinPalette)object, bForceBackground!=0);
        }
    };

    // COLORREF SetBkColor(HDC hdc, COLORREF crColor)
    private Callback.Handler SetBkColor = new HandlerBase() {
        public java.lang.String getName() {
            return "Gdi32.SetBkColor";
        }
        public void onCall() {
            int hdc = CPU.CPU_Pop32();
            int crColor = CPU.CPU_Pop32();

            WinObject object = WinSystem.getObject(hdc);
            if (object == null || !(object instanceof WinDC)) {
                WinSystem.getCurrentThread().setLastError(Error.ERROR_INVALID_HANDLE);
                CPU_Regs.reg_eax.dword = WinAPI.CLR_INVALID;
                return;
            }
            CPU_Regs.reg_eax.dword = ((WinDC)object).setBkColor(crColor);
        }
    };

    // UINT SetPaletteEntries(HPALETTE hpal, UINT iStart, UINT cEntries, const PALETTEENTRY *lppe)
    private Callback.Handler SetPaletteEntries = new HandlerBase() {
        public java.lang.String getName() {
            return "Gdi32.SetPaletteEntries";
        }
        public void onCall() {
            int hpal = CPU.CPU_Pop32();
            int iStart = CPU.CPU_Pop32();
            int cEntries = CPU.CPU_Pop32();
            int lppe = CPU.CPU_Pop32();

            WinObject object = WinSystem.getObject(hpal);
            if (object == null || !(object instanceof WinPalette)) {
                WinSystem.getCurrentThread().setLastError(Error.ERROR_INVALID_HANDLE);
                CPU_Regs.reg_eax.dword = WinAPI.CLR_INVALID;
                return;
            }
            CPU_Regs.reg_eax.dword = ((WinPalette)object).setEntries(iStart, cEntries, lppe);
        }
    };

    // COLORREF SetTextColor(HDC hdc, COLORREF crColor)
    private Callback.Handler SetTextColor = new HandlerBase() {
        public java.lang.String getName() {
            return "Gdi32.SetTextColor";
        }
        public void onCall() {
            int hdc = CPU.CPU_Pop32();
            int crColor = CPU.CPU_Pop32();

            WinObject object = WinSystem.getObject(hdc);
            if (object == null || !(object instanceof WinDC)) {
                WinSystem.getCurrentThread().setLastError(Error.ERROR_INVALID_HANDLE);
                CPU_Regs.reg_eax.dword = WinAPI.CLR_INVALID;
                return;
            }
            CPU_Regs.reg_eax.dword = ((WinDC)object).setTextColor(crColor);
        }
    };

    // BOOL StretchBlt(HDC hdcDest, int nXOriginDest, int nYOriginDest, int nWidthDest, int nHeightDest, HDC hdcSrc, int nXOriginSrc, int nYOriginSrc, int nWidthSrc, int nHeightSrc, DWORD dwRop)
    private Callback.Handler StretchBlt = new HandlerBase() {
        public java.lang.String getName() {
            return "Gdi32.StretchBlt";
        }
        public void onCall() {
            int hdcDest = CPU.CPU_Pop32();
            int nXOriginDest = CPU.CPU_Pop32();
            int nYOriginDest = CPU.CPU_Pop32();
            int nWidthDest = CPU.CPU_Pop32();
            int nHeightDest = CPU.CPU_Pop32();
            int hdcSrc = CPU.CPU_Pop32();
            int nXOriginSrc = CPU.CPU_Pop32();
            int nYOriginSrc = CPU.CPU_Pop32();
            int nWidthSrc = CPU.CPU_Pop32();
            int nHeightSrc = CPU.CPU_Pop32();
            int dwRop = CPU.CPU_Pop32();

            WinObject object = WinSystem.getObject(hdcDest);
            if (object == null || !(object instanceof WinDC)) {
                WinSystem.getCurrentThread().setLastError(Error.ERROR_INVALID_PARAMETER);
                CPU_Regs.reg_eax.dword = 0;
                return;
            }
            WinDC dest = (WinDC)object;
            object = WinSystem.getObject(hdcSrc);
            if (object == null || !(object instanceof WinDC)) {
                WinSystem.getCurrentThread().setLastError(Error.ERROR_INVALID_PARAMETER);
                CPU_Regs.reg_eax.dword = 0;
                return;
            }
            WinDC src = (WinDC)object;

            if (dwRop != 0x00CC0020) { // SRCCOPY
                Win.panic(getName()+" only supports SRCCOPY");
            }
            BufferedImage d = dest.getImage();
            BufferedImage s = src.getImage();
            Graphics g = d.getGraphics();
            g.drawImage(s, nXOriginDest, nYOriginDest, nXOriginDest+nWidthDest, nYOriginDest+nHeightDest, nXOriginSrc, nYOriginSrc, nXOriginSrc+nWidthSrc, nYOriginSrc+nHeightSrc, null);
            dest.writeImage(d);
            CPU_Regs.reg_eax.dword = WinAPI.TRUE;
        }
    };

    // BOOL TextOut(HDC hdc, int nXStart, int nYStart, LPCTSTR lpString, int cchString)
    private Callback.Handler TextOutA = new HandlerBase() {
        public java.lang.String getName() {
            return "Gdi32.TextOutA";
        }
        public void onCall() {
            int hdc = CPU.CPU_Pop32();
            int nXStart = CPU.CPU_Pop32();
            int nYStart = CPU.CPU_Pop32();
            int lpString = CPU.CPU_Pop32();
            int cchString = CPU.CPU_Pop32();

            WinObject object = WinSystem.getObject(hdc);
            if (object == null || !(object instanceof WinDC)) {
                WinSystem.getCurrentThread().setLastError(Error.ERROR_INVALID_HANDLE);
                CPU_Regs.reg_eax.dword = WinAPI.CLR_INVALID;
                return;
            }
            CPU_Regs.reg_eax.dword = ((WinDC)object).textOut(nXStart, nYStart, new LittleEndianFile(lpString).readCString(cchString));
        }
    };
}
