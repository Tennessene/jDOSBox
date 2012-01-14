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
import jdos.win.system.*;
import jdos.win.utils.Error;

import java.awt.*;
import java.awt.image.BufferedImage;

public class Gdi32 extends BuiltinModule {
    public Gdi32(Loader loader, int handle) {
        super(loader, "Gdi32.dll", handle);

        add(CreateCompatibleDC);
        add(CreateFontA);
        add(CreateFontIndirectA);
        add(CreateRectRgn);
        add(CreatePalette);
        add(DeleteDC);
        add(DeleteObject);
        add(GetDeviceCaps);
        add(GetObjectA);
        add(GetPaletteEntries);
        add(GetPixel);
        add(GdiSetBatchLimit);
        add(GetStockObject);
        add(GetSystemPaletteEntries);
        add(GetTextExtentPoint32A);
        add(GetTextExtentPointA);
        add(GetTextMetricsA);
        add(RealizePalette);
        add(SelectClipRgn);
        add(SelectObject);
        add(SelectPalette);
        add(SetBkColor);
        add(SetBkMode);
        add(SetPaletteEntries);
        add(SetPixel);
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

    abstract private static class FontHandlerBase extends HandlerBase {
        int createFont(int lpszFace, int nHeight, int fnWeight, int fdwItalic, int fdwUnderline, int fdwStrikeOut, int fdwPitchAndFamily) {
            String fontName = null;

            if (lpszFace != 0) {
                fontName = new LittleEndianFile(lpszFace).readCString();
            }
            int style = Font.PLAIN;
            if (fnWeight >= 600) // FW_SEMIBOLD
                style |= Font.BOLD;
            if (fdwItalic != 0)
                style |= Font.ITALIC;
            if (fdwUnderline != 0)
                System.out.println("Underline fonts not supported yet");
            if (fdwStrikeOut != 0)
                System.out.println("Strikeout fonts not supported yet");

            int size = 12;
            if (nHeight != 0)
                size = Math.abs(nHeight);
            size = WinFont.WIN_TO_JAVA(size);
            Font font = null;
            if (fontName != null) {
                font = new Font(fontName, style, size);
            }
            if (font == null) {
                if (fdwPitchAndFamily == 1) { // FIXED_PITCH
                    fontName = "Monospaced";
                } else {
                    fontName = null;
                }
                font = new Font(fontName, style, size);
            }
            if (font == null) {
                Win.panic(getName()+" unable to create font");
            }
            int result = WinSystem.createFont(font).getHandle();
            if (LOG)
                log("handle="+result+" "+size+" "+fontName);
            return result;
        }
    }

    // HFONT CreateFont(int nHeight, int nWidth, int nEscapement, int nOrientation, int fnWeight, DWORD fdwItalic, DWORD fdwUnderline, DWORD fdwStrikeOut, DWORD fdwCharSet, DWORD fdwOutputPrecision, DWORD fdwClipPrecision, DWORD fdwQuality, DWORD fdwPitchAndFamily, LPCTSTR lpszFace)
    private Callback.Handler CreateFontA = new FontHandlerBase() {
        public java.lang.String getName() {
            return "Gdi32.CreateFontA";
        }
        public void onCall() {
            int nHeight = CPU.CPU_Pop32();
            int nWidth = CPU.CPU_Pop32();
            int nEscapement = CPU.CPU_Pop32();
            int nOrientation = CPU.CPU_Pop32();
            int fnWeight = CPU.CPU_Pop32();
            int fdwItalic = CPU.CPU_Pop32();
            int fdwUnderline = CPU.CPU_Pop32();
            int fdwStrikeOut = CPU.CPU_Pop32();
            int fdwCharSet = CPU.CPU_Pop32();
            int fdwOutputPrecision = CPU.CPU_Pop32();
            int fdwClipPrecision = CPU.CPU_Pop32();
            int fdwQuality = CPU.CPU_Pop32();
            int fdwPitchAndFamily = CPU.CPU_Pop32();
            int lpszFace = CPU.CPU_Pop32();

            CPU_Regs.reg_eax.dword = createFont(lpszFace, nHeight, fnWeight, fdwItalic, fdwUnderline, fdwStrikeOut, fdwPitchAndFamily);
        }
    };

    // HFONT CreateFontIndirect(const LOGFONT *lplf)
    private Callback.Handler CreateFontIndirectA = new FontHandlerBase() {
        public java.lang.String getName() {
            return "Gdi32.CreateFontIndirectA";
        }
        public void onCall() {
            int address = CPU.CPU_Pop32();
            int nHeight = Memory.mem_readd(address);address+=4;
            int nWidth = Memory.mem_readd(address);address+=4;
            int nEscapement = Memory.mem_readd(address);address+=4;
            int nOrientation = Memory.mem_readd(address);address+=4;
            int fnWeight = Memory.mem_readd(address);address+=4;
            int fdwItalic = Memory.mem_readb(address);address+=1;
            int fdwUnderline = Memory.mem_readb(address);address+=1;
            int fdwStrikeOut = Memory.mem_readb(address);address+=1;
            int fdwCharSet = Memory.mem_readb(address);address+=1;
            int fdwOutputPrecision = Memory.mem_readb(address);address+=1;
            int fdwClipPrecision = Memory.mem_readb(address);address+=1;
            int fdwQuality = Memory.mem_readb(address);address+=1;
            int fdwPitchAndFamily = Memory.mem_readb(address);address+=1;
            int lpszFace = address;

            CPU_Regs.reg_eax.dword = createFont(lpszFace, nHeight, fnWeight, fdwItalic, fdwUnderline, fdwStrikeOut, fdwPitchAndFamily);
        }
    };

    // HRGN CreateRectRgn(int nLeftRect, int nTopRect, int nRightRect, int nBottomRect)
    private Callback.Handler CreateRectRgn = new HandlerBase() {
        public java.lang.String getName() {
            return "Gdi32.CreateRectRgn";
        }
        public void onCall() {
            int nLeftRect = CPU.CPU_Pop32();
            int nTopRect = CPU.CPU_Pop32();
            int nRightRect = CPU.CPU_Pop32();
            int nBottomRect = CPU.CPU_Pop32();
            WinRect winRect = new WinRect(nLeftRect, nTopRect, nRightRect, nBottomRect);
            CPU_Regs.reg_eax.dword = WinSystem.createRegion(winRect).getHandle();
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
    // int GetDeviceCaps(HDC hdc, int nIndex)
    private Callback.Handler GetDeviceCaps = new HandlerBase() {
        public java.lang.String getName() {
            return "Gdi32.GetDeviceCaps";
        }
        public void onCall() {
            int hdc = CPU.CPU_Pop32();
            int nIndex = CPU.CPU_Pop32();
            WinObject object = WinSystem.getObject(hdc);
            if (object == null || !(object instanceof WinDC)) {
                WinSystem.getCurrentThread().setLastError(Error.ERROR_INVALID_HANDLE);
                CPU_Regs.reg_eax.dword = 0;
                return;
            }
            CPU_Regs.reg_eax.dword = ((WinDC)object).getCaps(nIndex);
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

    // UINT GetPaletteEntries(HPALETTE hpal, UINT iStartIndex, UINT nEntries, LPPALETTEENTRY lppe)
    private Callback.Handler GetPaletteEntries = new HandlerBase() {
        public java.lang.String getName() {
            return "Gdi32.GetPaletteEntries";
        }
        public void onCall() {
            int hpal = CPU.CPU_Pop32();
            int iStartIndex = CPU.CPU_Pop32();
            int nEntries = CPU.CPU_Pop32();
            int lppe = CPU.CPU_Pop32();
            WinObject object = WinSystem.getObject(hpal);
            if (object == null || !(object instanceof WinPalette)) {
                CPU_Regs.reg_eax.dword = 0;
                WinSystem.getCurrentThread().setLastError(Error.ERROR_INVALID_HANDLE);
            } else {
                CPU_Regs.reg_eax.dword = ((WinPalette)object).getEntries(iStartIndex, nEntries, lppe);
            }
        }
    };

    // COLORREF GetPixel(HDC hdc, int nXPos, int nYPos)
    private Callback.Handler GetPixel = new HandlerBase() {
        public java.lang.String getName() {
            return "Gdi32.GetPixel";
        }
        public void onCall() {
            int hdc = CPU.CPU_Pop32();
            int nXPos = CPU.CPU_Pop32();
            int nYPos = CPU.CPU_Pop32();
            WinObject object = WinSystem.getObject(hdc);
            if (object == null || !(object instanceof WinDC)) {
                WinSystem.getCurrentThread().setLastError(Error.ERROR_INVALID_HANDLE);
                CPU_Regs.reg_eax.dword = WinAPI.CLR_INVALID;
                return;
            }
            CPU_Regs.reg_eax.dword = ((WinDC)object).getPixel(nXPos, nYPos);
        }
    };

    // UINT GetSystemPaletteEntries(HDC hdc, UINT iStartIndex, UINT nEntries, LPPALETTEENTRY lppe)
    private Callback.Handler GetSystemPaletteEntries = new HandlerBase() {
        public java.lang.String getName() {
            return "Gdi32.GetSystemPaletteEntries";
        }
        public void onCall() {
            int hdc = CPU.CPU_Pop32();
            int iStartIndex = CPU.CPU_Pop32();
            int nEntries = CPU.CPU_Pop32();
            int lppe = CPU.CPU_Pop32();
            WinObject object = WinSystem.getObject(hdc);
            if (object == null || !(object instanceof WinDC)) {
                WinSystem.getCurrentThread().setLastError(Error.ERROR_INVALID_HANDLE);
                CPU_Regs.reg_eax.dword = WinAPI.CLR_INVALID;
                return;
            }
            CPU_Regs.reg_eax.dword = ((WinDC)object).getPaletteEntries(iStartIndex, nEntries, lppe);
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
                    CPU_Regs.reg_eax.dword = WinSystem.createBrush(0xFFFFFFFF).getHandle();
                    return;
                case 1: // LTGRAY_BRUSH
                    CPU_Regs.reg_eax.dword = WinSystem.createBrush(0xFFD3D3D3).getHandle();
                    return;
                case 2: // GRAY_BRUSH
                    CPU_Regs.reg_eax.dword = WinSystem.createBrush(0xFF808080).getHandle();
                    return;
                case 3: // DKGRAY_BRUSH
                    CPU_Regs.reg_eax.dword = WinSystem.createBrush(0xFFA9A9A9).getHandle();
                    return;
                case 4: // BLACK_BRUSH
                    CPU_Regs.reg_eax.dword = WinSystem.createBrush(0xFF000000).getHandle();
                    return;
                case 5: // NULL_BRUSH
                    CPU_Regs.reg_eax.dword = WinSystem.createBrush(0x00000000).getHandle();
                    return;
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

    // BOOL GetTextExtentPoint32(HDC hdc, LPCTSTR lpString, int c, LPSIZE lpSize)
    private Callback.Handler GetTextExtentPoint32A = new HandlerBase() {
        public java.lang.String getName() {
            return "Gdi32.GetTextExtentPoint32A";
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
            CPU_Regs.reg_eax.dword = ((WinDC)object).getTextExtent(new LittleEndianFile(lpString).readCString(cbString), lpSize);
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
            CPU_Regs.reg_eax.dword = ((WinDC)object).getTextExtent(new LittleEndianFile(lpString).readCString(cbString), lpSize);
        }
    };

    // BOOL GetTextMetrics(HDC hdc, LPTEXTMETRIC lptm)
    private Callback.Handler GetTextMetricsA = new HandlerBase() {
        public java.lang.String getName() {
            return "Gdi32.GetTextMetricsA";
        }
        public void onCall() {
            int hdc = CPU.CPU_Pop32();
            int lptm = CPU.CPU_Pop32();
            WinObject object = WinSystem.getObject(hdc);
            if (object == null || !(object instanceof WinDC)) {
                WinSystem.getCurrentThread().setLastError(Error.ERROR_INVALID_PARAMETER);
                CPU_Regs.reg_eax.dword = 0;
                return;
            }
            CPU_Regs.reg_eax.dword = ((WinDC)object).getTextMetrics(lptm);
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

    // int SelectClipRgn(HDC hdc, HRGN hrgn)
    private Callback.Handler SelectClipRgn = new HandlerBase() {
        public java.lang.String getName() {
            return "Gdi32.SelectClipRgn";
        }
        public void onCall() {
            int hdc = CPU.CPU_Pop32();
            int hrgn = CPU.CPU_Pop32();
            WinObject object = WinSystem.getObject(hdc);
            if (object == null || !(object instanceof WinDC)) {
                WinSystem.getCurrentThread().setLastError(Error.ERROR_INVALID_PARAMETER);
                CPU_Regs.reg_eax.dword = 0;
                return;
            }
            WinDC dc = (WinDC)object;
            object = WinSystem.getObject(hrgn);
            if (object == null || !(object instanceof WinRegion)) {
                WinSystem.getCurrentThread().setLastError(Error.ERROR_INVALID_PARAMETER);
                CPU_Regs.reg_eax.dword = 0;
                return;
            }
            WinRegion region = (WinRegion)object;
            CPU_Regs.reg_eax.dword = dc.selectClipRgn(region);
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
            if (LOG)
                log("handle="+hgdiobj+" "+gdi.toString());
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

    // int SetBkMode(HDC hdc, int iBkMode)
    private Callback.Handler SetBkMode = new HandlerBase() {
        public java.lang.String getName() {
            return "Gdi32.SetBkMode";
        }
        public void onCall() {
            int hdc = CPU.CPU_Pop32();
            int iBkMode = CPU.CPU_Pop32();
            WinObject object = WinSystem.getObject(hdc);
            if (object == null || !(object instanceof WinDC)) {
                CPU_Regs.reg_eax.dword = 0;
                WinSystem.getCurrentThread().setLastError(Error.ERROR_INVALID_PARAMETER);
            } else {
                CPU_Regs.reg_eax.dword = ((WinDC)object).setBkMode(iBkMode);
            }
            if (LOG)
                log("hdc="+hdc+" iBkMode="+(iBkMode==1?"TRANSPARENT":"OPAQUE")+" result="+CPU_Regs.reg_eax.dword);
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

    // COLORREF SetPixel(HDC hdc, int X, int Y, COLORREF crColor)
    private Callback.Handler SetPixel = new HandlerBase() {
        public java.lang.String getName() {
            return "Gdi32.SetPixel";
        }
        public void onCall() {
            int hdc = CPU.CPU_Pop32();
            int nXPos = CPU.CPU_Pop32();
            int nYPos = CPU.CPU_Pop32();
            int crColor = CPU.CPU_Pop32();
            WinObject object = WinSystem.getObject(hdc);
            if (object == null || !(object instanceof WinDC)) {
                WinSystem.getCurrentThread().setLastError(Error.ERROR_INVALID_HANDLE);
                CPU_Regs.reg_eax.dword = WinAPI.CLR_INVALID;
                return;
            }
            CPU_Regs.reg_eax.dword = ((WinDC)object).setPixel(nXPos, nYPos, crColor);
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
            if (LOG)
                log("0x"+Integer.toString(crColor, 16));
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
