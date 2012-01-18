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
        add(CreatePen);
        add(CreatePalette);
        add(CreateRectRgn);
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
    private Callback.Handler CreateCompatibleDC = new ReturnHandlerBase() {
        public java.lang.String getName() {
            return "Gdi32.CreateCompatibleDC";
        }
        public int processReturn() {
            int hdc = CPU.CPU_Pop32();
            WinDC dc = null;
            if (hdc != 0) {
                WinObject object = WinSystem.getObject(hdc);
                if (object == null || !(object instanceof WinDC)) {
                    WinSystem.getCurrentThread().setLastError(Error.ERROR_INVALID_HANDLE);
                    return 0;
                }
                dc = (WinDC)object;
            }
            return WinSystem.createDC(null, false).getHandle();
        }
    };

    abstract private static class FontHandlerBase extends ReturnHandlerBase {
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
            if (LOG)
                log(size+"pt "+fontName);
            return WinSystem.createFont(font).getHandle();
        }
    }

    // HFONT CreateFont(int nHeight, int nWidth, int nEscapement, int nOrientation, int fnWeight, DWORD fdwItalic, DWORD fdwUnderline, DWORD fdwStrikeOut, DWORD fdwCharSet, DWORD fdwOutputPrecision, DWORD fdwClipPrecision, DWORD fdwQuality, DWORD fdwPitchAndFamily, LPCTSTR lpszFace)
    private Callback.Handler CreateFontA = new FontHandlerBase() {
        public java.lang.String getName() {
            return "Gdi32.CreateFontA";
        }
        public int processReturn() {
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

            return createFont(lpszFace, nHeight, fnWeight, fdwItalic, fdwUnderline, fdwStrikeOut, fdwPitchAndFamily);
        }
    };

    // HFONT CreateFontIndirect(const LOGFONT *lplf)
    private Callback.Handler CreateFontIndirectA = new FontHandlerBase() {
        public java.lang.String getName() {
            return "Gdi32.CreateFontIndirectA";
        }
        public int processReturn() {
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

            return createFont(lpszFace, nHeight, fnWeight, fdwItalic, fdwUnderline, fdwStrikeOut, fdwPitchAndFamily);
        }
    };

    // HPEN CreatePen(int fnPenStyle, int nWidth, COLORREF crColor)
    private Callback.Handler CreatePen = new ReturnHandlerBase() {
        public java.lang.String getName() {
            return "Gdi32.CreatePen";
        }
        public int processReturn() {
            int fnPenStyle = CPU.CPU_Pop32();
            int nWidth = CPU.CPU_Pop32();
            int crColor = CPU.CPU_Pop32();
            return WinSystem.createPen(fnPenStyle, nWidth, crColor).getHandle();
        }
    };

    // HRGN CreateRectRgn(int nLeftRect, int nTopRect, int nRightRect, int nBottomRect)
    private Callback.Handler CreateRectRgn = new ReturnHandlerBase() {
        public java.lang.String getName() {
            return "Gdi32.CreateRectRgn";
        }
        public int processReturn() {
            int nLeftRect = CPU.CPU_Pop32();
            int nTopRect = CPU.CPU_Pop32();
            int nRightRect = CPU.CPU_Pop32();
            int nBottomRect = CPU.CPU_Pop32();
            WinRect winRect = new WinRect(nLeftRect, nTopRect, nRightRect, nBottomRect);
            return WinSystem.createRegion(winRect).getHandle();
        }
    };

    // HPALETTE CreatePalette(const LOGPALETTE *lplgpl)
    private Callback.Handler CreatePalette = new ReturnHandlerBase() {
        public java.lang.String getName() {
            return "Gdi32.CreatePalette";
        }
        public int processReturn() {
            int lplgpl = CPU.CPU_Pop32();
            if (lplgpl == 0) {
                WinSystem.getCurrentThread().setLastError(Error.ERROR_INVALID_PARAMETER);
                return 0;
            }
            int count = Memory.mem_readw(lplgpl+2);
            int[] palette = new int[count];
            for (int i=0;i<count;i++) {
                int address = lplgpl+4+4*i;
                palette[i] = Memory.mem_readd(address) & 0xFFFFFF; // strip out the flag
            }
            return WinSystem.createPalette(palette).getHandle();
        }
    };

    // BOOL DeleteDC(HDC hdc)
    private Callback.Handler DeleteDC = new ReturnHandlerBase() {
        public java.lang.String getName() {
            return "Gdi32.DeleteDC";
        }
        public int processReturn() {
            int hdc = CPU.CPU_Pop32();
            WinDC dc = null;
            WinObject object = WinSystem.getObject(hdc);
            if (object == null || !(object instanceof WinDC)) {
                WinSystem.getCurrentThread().setLastError(Error.ERROR_INVALID_HANDLE);
                return WinAPI.FALSE;
            }
            dc = (WinDC)object;
            dc.close();
            return WinAPI.TRUE;
        }
    };

    // BOOL DeleteObject(HGDIOBJ hObject)
    private Callback.Handler DeleteObject = new ReturnHandlerBase() {
        public java.lang.String getName() {
            return "Gdi32.DeleteObject";
        }
        public int processReturn() {
            int hObject = CPU.CPU_Pop32();
            WinGDI gdi = null;
            WinObject object = WinSystem.getObject(hObject);
            if (object == null || !(object instanceof WinGDI)) {
                CPU_Regs.reg_eax.dword = WinAPI.FALSE;
                WinSystem.getCurrentThread().setLastError(Error.ERROR_INVALID_HANDLE);
                return WinAPI.FALSE;
            }
            gdi = (WinGDI)object;
            gdi.close();
            return WinAPI.TRUE;
        }
    };

    // DWORD GdiSetBatchLimit(DWORD dwLimit)
    private Callback.Handler GdiSetBatchLimit = new ReturnHandlerBase() {
        public java.lang.String getName() {
            return "Gdi32.GdiSetBatchLimit";
        }
        public int processReturn() {
            int dwLimit = CPU.CPU_Pop32();
            System.out.println(getName()+" faked");
            return 0;
        }
    };
    // int GetDeviceCaps(HDC hdc, int nIndex)
    private Callback.Handler GetDeviceCaps = new ReturnHandlerBase() {
        public java.lang.String getName() {
            return "Gdi32.GetDeviceCaps";
        }
        public int processReturn() {
            int hdc = CPU.CPU_Pop32();
            int nIndex = CPU.CPU_Pop32();
            WinObject object = WinSystem.getObject(hdc);
            if (object == null || !(object instanceof WinDC)) {
                WinSystem.getCurrentThread().setLastError(Error.ERROR_INVALID_HANDLE);
                return 0;
            }
            return ((WinDC)object).getCaps(nIndex);
        }
    };

    // int GetObject(HGDIOBJ hgdiobj, int cbBuffer, LPVOID lpvObject)
    private Callback.Handler GetObjectA = new ReturnHandlerBase() {
        public java.lang.String getName() {
            return "Gdi32.GetObjectA";
        }
        public int processReturn() {
            int hgdiobj = CPU.CPU_Pop32();
            int cbBuffer = CPU.CPU_Pop32();
            int lpvObject = CPU.CPU_Pop32();
            WinObject object = WinSystem.getObject(hgdiobj);
            if (object == null || !(object instanceof WinGDI)) {
                WinSystem.getCurrentThread().setLastError(Error.ERROR_INVALID_HANDLE);
                return 0;
            } else if (object instanceof WinBitmap) {
                WinBitmap bitmap = (WinBitmap)object;
                return bitmap.get(lpvObject, cbBuffer);
            } else {
                Win.panic(getName()+" not implemented yet for type "+object);
                return 0;
            }
        }
    };

    // UINT GetPaletteEntries(HPALETTE hpal, UINT iStartIndex, UINT nEntries, LPPALETTEENTRY lppe)
    private Callback.Handler GetPaletteEntries = new ReturnHandlerBase() {
        public java.lang.String getName() {
            return "Gdi32.GetPaletteEntries";
        }
        public int processReturn() {
            int hpal = CPU.CPU_Pop32();
            int iStartIndex = CPU.CPU_Pop32();
            int nEntries = CPU.CPU_Pop32();
            int lppe = CPU.CPU_Pop32();
            WinObject object = WinSystem.getObject(hpal);
            if (object == null || !(object instanceof WinPalette)) {
                WinSystem.getCurrentThread().setLastError(Error.ERROR_INVALID_HANDLE);
                return 0;
            } else {
                return ((WinPalette)object).getEntries(iStartIndex, nEntries, lppe);
            }
        }
    };

    // COLORREF GetPixel(HDC hdc, int nXPos, int nYPos)
    private Callback.Handler GetPixel = new ReturnHandlerBase() {
        public java.lang.String getName() {
            return "Gdi32.GetPixel";
        }
        public int processReturn() {
            int hdc = CPU.CPU_Pop32();
            int nXPos = CPU.CPU_Pop32();
            int nYPos = CPU.CPU_Pop32();
            WinObject object = WinSystem.getObject(hdc);
            if (object == null || !(object instanceof WinDC)) {
                WinSystem.getCurrentThread().setLastError(Error.ERROR_INVALID_HANDLE);
                return WinAPI.CLR_INVALID;
            }
            return ((WinDC)object).getPixel(nXPos, nYPos);
        }
    };

    // UINT GetSystemPaletteEntries(HDC hdc, UINT iStartIndex, UINT nEntries, LPPALETTEENTRY lppe)
    private Callback.Handler GetSystemPaletteEntries = new ReturnHandlerBase() {
        public java.lang.String getName() {
            return "Gdi32.GetSystemPaletteEntries";
        }
        public int processReturn() {
            int hdc = CPU.CPU_Pop32();
            int iStartIndex = CPU.CPU_Pop32();
            int nEntries = CPU.CPU_Pop32();
            int lppe = CPU.CPU_Pop32();
            WinObject object = WinSystem.getObject(hdc);
            if (object == null || !(object instanceof WinDC)) {
                WinSystem.getCurrentThread().setLastError(Error.ERROR_INVALID_HANDLE);
                return WinAPI.CLR_INVALID;
            }
            return ((WinDC)object).getPaletteEntries(iStartIndex, nEntries, lppe);
        }
    };

    // HGDIOBJ GetStockObject(int fnObject)
    private Callback.Handler GetStockObject = new ReturnHandlerBase() {
        public java.lang.String getName() {
            return "Gdi32.GetStockObject";
        }
        public int processReturn() {
            int fnObject = CPU.CPU_Pop32();
            switch (fnObject) {
                case 0: // WHITE_BRUSH
                    return WinSystem.createBrush(0xFFFFFFFF).getHandle();
                case 1: // LTGRAY_BRUSH
                    return WinSystem.createBrush(0xFFD3D3D3).getHandle();
                case 2: // GRAY_BRUSH
                    return WinSystem.createBrush(0xFF808080).getHandle();
                case 3: // DKGRAY_BRUSH
                    return WinSystem.createBrush(0xFFA9A9A9).getHandle();
                case 4: // BLACK_BRUSH
                    return WinSystem.createBrush(0xFF000000).getHandle();
                case 5: // NULL_BRUSH
                    return WinSystem.createBrush(0x00000000).getHandle();
                case 6: // WHITE_PEN
                    return WinSystem.createPen(0, 1, 0xFFFFFFFF).getHandle();
                case 7: // BLACK_PEN
                    return WinSystem.createPen(0, 1, 0xFF000000).getHandle();
                case 8: // NULL_PEN
                    return WinSystem.createPen(0, 1, 0x00000000).getHandle();
                case 10: // OEM_FIXED_FONT
                case 11: // ANSI_FIXED_FONT
                case 12: // ANSI_VAR_FONT
                case 13: // SYSTEM_FONT
                case 14: // DEVICE_DEFAULT_FONT
                case 15: // DEFAULT_PALETTE
                case 16: // SYSTEM_FIXED_FONT
                    Console.out("GetStockObject faked");
                    notImplemented();
                    break;
                default:
                    Console.out("Unknown GetStockObject "+fnObject);
                    notImplemented();
            }
            return 1;
        }
    };

    // BOOL GetTextExtentPoint32(HDC hdc, LPCTSTR lpString, int c, LPSIZE lpSize)
    private Callback.Handler GetTextExtentPoint32A = new ReturnHandlerBase() {
        public java.lang.String getName() {
            return "Gdi32.GetTextExtentPoint32A";
        }
        public int processReturn() {
            int hdc = CPU.CPU_Pop32();
            int lpString = CPU.CPU_Pop32();
            int cbString = CPU.CPU_Pop32();
            int lpSize = CPU.CPU_Pop32();
            WinObject object = WinSystem.getObject(hdc);
            if (object == null || !(object instanceof WinDC)) {
                WinSystem.getCurrentThread().setLastError(Error.ERROR_INVALID_PARAMETER);
                return 0;
            }
            return ((WinDC)object).getTextExtent(new LittleEndianFile(lpString).readCString(cbString), lpSize);
        }
    };

    // BOOL GetTextExtentPoint(HDC hdc, LPCTSTR lpString, int cbString, LPSIZE lpSize)
    private Callback.Handler GetTextExtentPointA = new ReturnHandlerBase() {
        public java.lang.String getName() {
            return "Gdi32.GetTextExtentPointA";
        }
        public int processReturn() {
            int hdc = CPU.CPU_Pop32();
            int lpString = CPU.CPU_Pop32();
            int cbString = CPU.CPU_Pop32();
            int lpSize = CPU.CPU_Pop32();
            WinObject object = WinSystem.getObject(hdc);
            if (object == null || !(object instanceof WinDC)) {
                WinSystem.getCurrentThread().setLastError(Error.ERROR_INVALID_PARAMETER);
                return 0;
            }
            return ((WinDC)object).getTextExtent(new LittleEndianFile(lpString).readCString(cbString), lpSize);
        }
    };

    // BOOL GetTextMetrics(HDC hdc, LPTEXTMETRIC lptm)
    private Callback.Handler GetTextMetricsA = new ReturnHandlerBase() {
        public java.lang.String getName() {
            return "Gdi32.GetTextMetricsA";
        }
        public int processReturn() {
            int hdc = CPU.CPU_Pop32();
            int lptm = CPU.CPU_Pop32();
            WinObject object = WinSystem.getObject(hdc);
            if (object == null || !(object instanceof WinDC)) {
                WinSystem.getCurrentThread().setLastError(Error.ERROR_INVALID_PARAMETER);
                return 0;
            }
            return ((WinDC)object).getTextMetrics(lptm);
        }
    };

    // UINT RealizePalette(HDC hdc)
    private Callback.Handler RealizePalette = new ReturnHandlerBase() {
        public java.lang.String getName() {
            return "Gdi32.RealizePalette";
        }
        public int processReturn() {
            int hdc = CPU.CPU_Pop32();
            WinObject object = WinSystem.getObject(hdc);
            if (object == null || !(object instanceof WinDC)) {
                WinSystem.getCurrentThread().setLastError(Error.ERROR_INVALID_PARAMETER);
                return 0;
            }
            return ((WinDC)object).realizePalette();
        }
    };

    // int SelectClipRgn(HDC hdc, HRGN hrgn)
    private Callback.Handler SelectClipRgn = new ReturnHandlerBase() {
        public java.lang.String getName() {
            return "Gdi32.SelectClipRgn";
        }
        public int processReturn() {
            int hdc = CPU.CPU_Pop32();
            int hrgn = CPU.CPU_Pop32();
            WinObject object = WinSystem.getObject(hdc);
            if (object == null || !(object instanceof WinDC)) {
                WinSystem.getCurrentThread().setLastError(Error.ERROR_INVALID_PARAMETER);
                return 0;
            }
            WinDC dc = (WinDC)object;
            object = WinSystem.getObject(hrgn);
            if (object == null || !(object instanceof WinRegion)) {
                WinSystem.getCurrentThread().setLastError(Error.ERROR_INVALID_PARAMETER);
                return 0;
            }
            WinRegion region = (WinRegion)object;
            return dc.selectClipRgn(region);
        }
    };

    // HGDIOBJ SelectObject(HDC hdc, HGDIOBJ hgdiobj)
    private Callback.Handler SelectObject = new ReturnHandlerBase() {
        public java.lang.String getName() {
            return "Gdi32.SelectObject";
        }
        public int processReturn() {
            int hdc = CPU.CPU_Pop32();
            int hgdiobj = CPU.CPU_Pop32();
            WinObject object = WinSystem.getObject(hdc);
            if (object == null || !(object instanceof WinDC)) {
                WinSystem.getCurrentThread().setLastError(Error.ERROR_INVALID_PARAMETER);
                return 0;
            }
            WinDC dc = (WinDC)object;
            object = WinSystem.getObject(hgdiobj);
            if (object == null || !(object instanceof WinGDI)) {
                WinSystem.getCurrentThread().setLastError(Error.ERROR_INVALID_PARAMETER);
                return 0;
            }
            WinGDI gdi = (WinGDI)object;
            if (LOG)
                log("handle="+hgdiobj+" "+gdi.toString());
            return dc.select(gdi);
        }
    };

    // HPALETTE SelectPalette(HDC hdc, HPALETTE hpal, BOOL bForceBackground)
    private Callback.Handler SelectPalette = new ReturnHandlerBase() {
        public java.lang.String getName() {
            return "Gdi32.SelectPalette";
        }
        public int processReturn() {
            int hdc = CPU.CPU_Pop32();
            int hpal = CPU.CPU_Pop32();
            int bForceBackground = CPU.CPU_Pop32();

            WinObject object = WinSystem.getObject(hdc);
            if (object == null || !(object instanceof WinDC)) {
                WinSystem.getCurrentThread().setLastError(Error.ERROR_INVALID_PARAMETER);
                return 0;
            }
            WinDC dc = (WinDC)object;
            object = WinSystem.getObject(hpal);
            if (object == null || !(object instanceof WinPalette)) {
                WinSystem.getCurrentThread().setLastError(Error.ERROR_INVALID_PARAMETER);
                return 0;
            }
            return dc.selectPalette((WinPalette)object, bForceBackground!=0);
        }
    };

    // COLORREF SetBkColor(HDC hdc, COLORREF crColor)
    private Callback.Handler SetBkColor = new ReturnHandlerBase() {
        public java.lang.String getName() {
            return "Gdi32.SetBkColor";
        }
        public int processReturn() {
            int hdc = CPU.CPU_Pop32();
            int crColor = CPU.CPU_Pop32();

            WinObject object = WinSystem.getObject(hdc);
            if (object == null || !(object instanceof WinDC)) {
                WinSystem.getCurrentThread().setLastError(Error.ERROR_INVALID_HANDLE);
                return WinAPI.CLR_INVALID;
            }
            return ((WinDC)object).setBkColor(crColor);
        }
    };

    // int SetBkMode(HDC hdc, int iBkMode)
    private Callback.Handler SetBkMode = new ReturnHandlerBase() {
        public java.lang.String getName() {
            return "Gdi32.SetBkMode";
        }
        public int processReturn() {
            int hdc = CPU.CPU_Pop32();
            int iBkMode = CPU.CPU_Pop32();
            WinObject object = WinSystem.getObject(hdc);
            if (object == null || !(object instanceof WinDC)) {
                CPU_Regs.reg_eax.dword = 0;
                WinSystem.getCurrentThread().setLastError(Error.ERROR_INVALID_PARAMETER);
                return 0;
            } else {
                if (LOG)
                    log("hdc="+hdc+" iBkMode="+(iBkMode==1?"TRANSPARENT":"OPAQUE"));
                return ((WinDC)object).setBkMode(iBkMode);
            }
        }
    };

    // UINT SetPaletteEntries(HPALETTE hpal, UINT iStart, UINT cEntries, const PALETTEENTRY *lppe)
    private Callback.Handler SetPaletteEntries = new ReturnHandlerBase() {
        public java.lang.String getName() {
            return "Gdi32.SetPaletteEntries";
        }
        public int processReturn() {
            int hpal = CPU.CPU_Pop32();
            int iStart = CPU.CPU_Pop32();
            int cEntries = CPU.CPU_Pop32();
            int lppe = CPU.CPU_Pop32();

            WinObject object = WinSystem.getObject(hpal);
            if (object == null || !(object instanceof WinPalette)) {
                WinSystem.getCurrentThread().setLastError(Error.ERROR_INVALID_HANDLE);
                return WinAPI.CLR_INVALID;
            }
            return ((WinPalette)object).setEntries(iStart, cEntries, lppe);
        }
    };

    // COLORREF SetPixel(HDC hdc, int X, int Y, COLORREF crColor)
    private Callback.Handler SetPixel = new ReturnHandlerBase() {
        public java.lang.String getName() {
            return "Gdi32.SetPixel";
        }
        public int processReturn() {
            int hdc = CPU.CPU_Pop32();
            int nXPos = CPU.CPU_Pop32();
            int nYPos = CPU.CPU_Pop32();
            int crColor = CPU.CPU_Pop32();
            WinObject object = WinSystem.getObject(hdc);
            if (object == null || !(object instanceof WinDC)) {
                WinSystem.getCurrentThread().setLastError(Error.ERROR_INVALID_HANDLE);
                return WinAPI.CLR_INVALID;
            }
            return ((WinDC)object).setPixel(nXPos, nYPos, crColor);
        }
    };

    // COLORREF SetTextColor(HDC hdc, COLORREF crColor)
    private Callback.Handler SetTextColor = new ReturnHandlerBase() {
        public java.lang.String getName() {
            return "Gdi32.SetTextColor";
        }
        public int processReturn() {
            int hdc = CPU.CPU_Pop32();
            int crColor = CPU.CPU_Pop32();

            WinObject object = WinSystem.getObject(hdc);
            if (object == null || !(object instanceof WinDC)) {
                WinSystem.getCurrentThread().setLastError(Error.ERROR_INVALID_HANDLE);
                return WinAPI.CLR_INVALID;
            }
            if (LOG)
                log("0x"+Integer.toString(crColor, 16));
            return ((WinDC)object).setTextColor(crColor);
        }
    };

    // BOOL StretchBlt(HDC hdcDest, int nXOriginDest, int nYOriginDest, int nWidthDest, int nHeightDest, HDC hdcSrc, int nXOriginSrc, int nYOriginSrc, int nWidthSrc, int nHeightSrc, DWORD dwRop)
    private Callback.Handler StretchBlt = new ReturnHandlerBase() {
        public java.lang.String getName() {
            return "Gdi32.StretchBlt";
        }
        public int processReturn() {
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
                return WinAPI.FALSE;
            }
            WinDC dest = (WinDC)object;
            object = WinSystem.getObject(hdcSrc);
            if (object == null || !(object instanceof WinDC)) {
                WinSystem.getCurrentThread().setLastError(Error.ERROR_INVALID_PARAMETER);
                return WinAPI.FALSE;
            }
            WinDC src = (WinDC)object;

            if (dwRop != 0x00CC0020) { // SRCCOPY
                Win.panic(getName()+" only supports SRCCOPY");
            }
            BufferedImage d = dest.getImage();
            BufferedImage s = src.getImage();
            Graphics g = d.getGraphics();
            g.drawImage(s, nXOriginDest, nYOriginDest, nXOriginDest+nWidthDest, nYOriginDest+nHeightDest, nXOriginSrc, nYOriginSrc, nXOriginSrc+nWidthSrc, nYOriginSrc+nHeightSrc, null);
            return WinAPI.TRUE;
        }
    };

    // BOOL TextOut(HDC hdc, int nXStart, int nYStart, LPCTSTR lpString, int cchString)
    private Callback.Handler TextOutA = new ReturnHandlerBase() {
        public java.lang.String getName() {
            return "Gdi32.TextOutA";
        }
        public int processReturn() {
            int hdc = CPU.CPU_Pop32();
            int nXStart = CPU.CPU_Pop32();
            int nYStart = CPU.CPU_Pop32();
            int lpString = CPU.CPU_Pop32();
            int cchString = CPU.CPU_Pop32();

            WinObject object = WinSystem.getObject(hdc);
            if (object == null || !(object instanceof WinDC)) {
                WinSystem.getCurrentThread().setLastError(Error.ERROR_INVALID_HANDLE);
                return WinAPI.CLR_INVALID;
            }
            String value = new LittleEndianFile(lpString).readCString(cchString);
            if (LOG)
                log(value+" at "+nXStart+","+nYStart);
            return ((WinDC)object).textOut(nXStart, nYStart, value);
        }
    };
}
