package jdos.win.builtin.gdi32;

import jdos.hardware.Memory;
import jdos.win.Win;
import jdos.win.builtin.WinAPI;
import jdos.win.builtin.user32.SysParams;
import jdos.win.system.JavaBitmap;
import jdos.win.system.StaticData;
import jdos.win.system.WinObject;
import jdos.win.system.WinRect;
import jdos.win.utils.Pixel;
import jdos.win.utils.StringUtil;

import java.awt.*;
import java.awt.font.FontRenderContext;
import java.awt.font.LineMetrics;
import java.awt.image.BufferedImage;
import java.util.Arrays;

public class WinDC extends WinObject {
    static WinFont defaultFont = WinFont.get(GdiObj.GetStockObject(DEVICE_DEFAULT_FONT));
    static WinPalette defaultPalette = WinPalette.create(JavaBitmap.getDefaultPalette());
    static WinPen defaultPen = WinPen.get(GdiObj.GetStockObject(BLACK_PEN));
    static WinBrush defaultBrush = WinBrush.get(GdiObj.GetStockObject(WHITE_BRUSH));

    static public WinDC create(JavaBitmap image, boolean owner) {
        return new WinDC(nextObjectId(), image, owner);
    }

    static public WinDC create() {
        return new WinDC(nextObjectId(), null, false);
    }

    static public WinDC get(int handle) {
        WinObject object = getObject(handle);
        if (object == null || !(object instanceof WinDC))
            return null;
        return (WinDC)object;
    }

    // int FillRect(HDC hDC, const RECT *lprc, HBRUSH hbr)
    static public int FillRect(int hDC, int lprc, int hbr) {
        int prev_brush;

        if (hbr <= COLOR_MAX + 1) hbr = SysParams.GetSysColorBrush(hbr - 1);

        prev_brush = SelectObject(hDC, hbr);
        if (prev_brush != 0) {
            WinRect rect = new WinRect(lprc);
            PatBlt(hDC, rect.left, rect.top, rect.right - rect.left, rect.bottom - rect.top, PATCOPY);
            SelectObject(hDC, prev_brush);
            return 1;
        }
        return 0;
    }

    // HDC CreateCompatibleDC(HDC hdc)
    static public int CreateCompatibleDC(int hdc) {
        WinDC dc = create();
        return dc.handle;
    }

    // HDC CreateDC(LPCTSTR lpszDriver, LPCTSTR lpszDevice, LPCTSTR lpszOutput, const DEVMODE *lpInitData)
    static public int CreateDCA(int driver, int device, int output, int initData) {
        WinDC dc = create();
        return dc.handle;
    }

    // BOOL DeleteDC(HDC hdc)
    static public int DeleteDC(int hdc) {
        WinDC dc = WinDC.get(hdc);
        if (dc == null)
            return FALSE;
        dc.close();
        return TRUE;
    }

    // int DrawText(HDC hDC, LPCTSTR lpchText, int nCount, LPRECT lpRect, UINT uFormat)
    static public int DrawTextA(int hDC, int lpchText, int nCount, int lpRect, int uFormat) {
        WinDC dc = WinDC.get(hDC);
        if (dc == null)
            return 0;

        WinRect rect = new WinRect(lpRect);
        String text = StringUtil.getString(lpchText, nCount);
        BufferedImage bi = dc.getImage();
        Graphics2D g = (Graphics2D)bi.getGraphics();
        FontRenderContext frc = g.getFontRenderContext();
        Font font = WinFont.get(dc.hFont).font;
        g.setFont(font);
        int sw = (int)font.getStringBounds(text, frc).getWidth();
        LineMetrics lm = font.getLineMetrics(text, frc);
        int sh = (int)(lm.getAscent() + lm.getDescent());

        int x = rect.left;
        int y = rect.top;
        if ((uFormat & DT_CENTER)!=0) {
            x = (rect.right-rect.left)/2 - sw/2;
        } else if ((uFormat & DT_RIGHT)!=0) {
            x = rect.right - sw;
        }
        if ((uFormat & DT_BOTTOM)!=0) {
            y = rect.bottom-sh;
        }
        TextOutA(hDC, x, y, lpchText, nCount);

        System.out.println("drawText not fully implemented");
        g.dispose();
        return sh;
    }

    // BOOL ExtTextOut(HDC hdc, int X, int Y, UINT fuOptions, const RECT *lprc, LPCTSTR lpString, UINT cbCount, const INT *lpDx)
    static public int ExtTextOutA(int hdc, int X, int Y, int fuOptions, int lprc, int lpString, int cbCount, int lpDx) {
        log("ExtTextOutA not fully implemented yet");
        WinDC dc = WinDC.get(hdc);
        if (dc == null) {
            return FALSE;
        }
        BufferedImage bi = dc.getImage();
        Graphics2D g = (Graphics2D)bi.getGraphics();
        String text = StringUtil.getString(lpString, cbCount);

        FontRenderContext frc = g.getFontRenderContext();
        Font font = WinFont.get(dc.hFont).font;
        g.setFont(font);

        int sw = (int)font.getStringBounds(text, frc).getWidth();
        LineMetrics lm = font.getLineMetrics(text, frc);
        int sh = (int)(lm.getAscent() + lm.getDescent());

        if (dc.bkMode == OPAQUE) {
            g.setColor(new Color(dc.bkColor));
            g.fillRect(X, Y, sw, sh);
        }
        g.setColor(new Color(dc.textColor | 0xFF000000));
        g.drawString(text, X, Y+sh-(int)lm.getDescent());
        g.dispose();
        return WinAPI.TRUE;
    }

    // int GetClipBox(HDC hdc,  LPRECT lprc)
    static public int GetClipBox(int hdc, int rect)  {
        WinDC dc = WinDC.get(hdc);
        if (dc==null) return ERROR;
        if (dc.hClipRgn != 0)
            return WinRegion.GetRgnBox(dc.hClipRgn, rect);
        new WinRect(0, 0, dc.cx-1, dc.cy-1).write(rect);
        return SIMPLEREGION;
    }

    // int GetDeviceCaps(HDC hdc, int nIndex)
    static public int GetDeviceCaps(int hdc, int nIndex) {
        WinDC dc = WinDC.get(hdc);
        if (dc == null)
            return 0;

        switch (nIndex) {
            case RASTERCAPS:
                int result = 0x0001|0x0008|0x0800; // RC_BITBLT | RC_BITMAP64 | RC_STRETCHBLT
                if (dc.image.getBpp()<=8)
                    result |= 0x0100; //RC_PALETTE
                return result;
            case BITSPIXEL:
                return dc.image.getBpp();
            case PLANES:
                return 1;
            case NUMCOLORS:
                if (dc.image.getBpp()<=8)
                    return 1 << dc.image.getBpp();
                return -1;
            default:
                Win.panic("GetDevice caps "+nIndex+" not implemented yet.");
        }
        return 0;
    }

    // UINT GetSystemPaletteEntries(HDC hdc, UINT iStartIndex, UINT nEntries, LPPALETTEENTRY lppe)
    static public int GetSystemPaletteEntries(int hdc, int iStartIndex, int nEntries, int lppe) {
        WinDC dc = WinDC.get(hdc);
        if (dc == null)
            return 0;

        int[] palette = dc.image.getPalette();
        if (palette != null) {
            for (int i=0;i<nEntries;i++) {
                Memory.mem_writed(lppe+i*4, palette[i+iStartIndex]);
            }
            return nEntries;
        }
        return 0;
    }

    // COLORREF GetPixel(HDC hdc, int nXPos, int nYPos)
    static public int GetPixel(int hdc, int nXPos, int nYPos) {
        WinDC dc = WinDC.get(hdc);
        if (dc == null)
            return CLR_INVALID;
        BufferedImage bi = dc.getImage();
        return bi.getRGB(nXPos, nYPos);
    }

    // BOOL GetTextExtentPoint32(HDC hdc, LPCTSTR lpString, int c, LPSIZE lpSize)
    static public int GetTextExtentPoint32A(int hdc, int lpString, int cbString, int lpSize) {
        return GetTextExtentPointA(hdc, lpString, cbString, lpSize);
    }

    // BOOL GetTextExtentPoint(HDC hdc, LPCTSTR lpString, int cbString, LPSIZE lpSize)
    static public int GetTextExtentPointA(int hdc, int lpString, int cbString, int lpSize) {
        WinDC dc = WinDC.get(hdc);
        if (dc == null)
            return WinAPI.FALSE;
        String text = StringUtil.getString(lpString, cbString);

        BufferedImage bi = dc.getImage();
        Graphics2D g = (Graphics2D)bi.getGraphics();
        FontRenderContext frc = g.getFontRenderContext();
        Font font = WinFont.get(dc.hFont).font;
        g.setFont(font);
        int sw = (int)font.getStringBounds(text, frc).getWidth();
        LineMetrics lm = font.getLineMetrics(text, frc);
        int sh = (int)(lm.getAscent() + lm.getDescent());
        Memory.mem_writed(lpSize, sw);
        Memory.mem_writed(lpSize+4, sh);
        g.dispose();
        return WinAPI.TRUE;
    }

    // BOOL GetTextMetrics(HDC hdc, LPTEXTMETRIC lptm)
    static public int GetTextMetricsA(int hdc, int lptm) {
        WinDC dc = WinDC.get(hdc);
        if (dc == null)
            return FALSE;
        BufferedImage bi = dc.getImage();
        Graphics2D g = (Graphics2D)bi.getGraphics();
        Font font = WinFont.get(dc.hFont).font;
        g.setFont(font);
        FontMetrics metrics = g.getFontMetrics();
        g.dispose();
        Memory.mem_writed(lptm, WinFont.JAVA_TO_WIN(font.getSize()));lptm+=4; // tmHeight - Windows defaults to 96 dpi, java uses 72
        Memory.mem_writed(lptm, WinFont.JAVA_TO_WIN(metrics.getAscent()));lptm+=4; // tmAscent
        Memory.mem_writed(lptm, WinFont.JAVA_TO_WIN(metrics.getDescent()));lptm+=4; // tmDescent
        Memory.mem_writed(lptm, WinFont.JAVA_TO_WIN(metrics.getLeading()));lptm+=4; // tmInternalLeading
        Memory.mem_writed(lptm, 0);lptm+=4; // tmExternalLeading
        int[] width = metrics.getWidths();
        Arrays.sort(width);
        Memory.mem_writed(lptm, WinFont.JAVA_TO_WIN(width[200])/2);lptm+=4; // tmAveCharWidth
        Memory.mem_writed(lptm, WinFont.JAVA_TO_WIN(width[255]));lptm+=4; // tmMaxCharWidth
        Memory.mem_writed(lptm, font.isBold()?700:400);lptm+=4; // tmWeight FW_NORMAL=400 FW_BOLD=700
        Memory.mem_writed(lptm, 0);lptm+=4; // tmOverhang
        Memory.mem_writed(lptm, 96);lptm+=4; // tmDigitizedAspectX
        Memory.mem_writed(lptm, 96);lptm+=4; // tmDigitizedAspectY
        Memory.mem_writeb(lptm, 32);lptm+=1; // tmFirstChar
        Memory.mem_writeb(lptm, 256);lptm+=1; // tmLastChar
        Memory.mem_writeb(lptm, 32);lptm+=1; // tmDefaultChar
        Memory.mem_writeb(lptm, 32);lptm+=1; // tmBreakChar
        Memory.mem_writeb(lptm, font.isItalic() ? 1 : 0);lptm+=1; // tmItalic
        Memory.mem_writeb(lptm, 0);lptm+=1; // tmUnderlined
        Memory.mem_writeb(lptm, 0);lptm+=1; // tmStruckOut
        Memory.mem_writeb(lptm, 0x06);lptm+=1; // tmPitchAndFamily TMPF_FIXED_PITCH=0x01 TMPF_VECTOR=0x02 TMPF_DEVICE=0x08 TMPF_TRUETYPE=0x04
        Memory.mem_writeb(lptm, 0);lptm+=1; // tmCharSet 0=ANSI_CHARSET
        return WinAPI.TRUE;
    }

    // BOOL PatBlt(HDC hdc, int nXLeft, int nYLeft, int nWidth, int nHeight, DWORD dwRop)
    static public int PatBlt(int hdc, int nXLeft, int nYLeft, int nWidth, int nHeight, int dwRop) {
        System.out.println("PatBlt not fully implemented yet");
        WinDC dc = WinDC.get(hdc);
        if (dc == null)
            return FALSE;

        int color = 0xFF000000 | WinBrush.get(dc.hBrush).color;
        BufferedImage image = dc.getImage();
        Graphics graphics = image.getGraphics();
        graphics.setColor(new Color(color));
        graphics.fillRect(nXLeft, nYLeft, nWidth, nHeight);
        graphics.dispose();
        return TRUE;
    }

    // UINT RealizePalette(HDC hdc)
    static public int RealizePalette(int hdc) {
        // The display is 32 bits so every window can use its own palette
        return 0;
    }

    // int SelectClipRgn(HDC hdc, HRGN hrgn)
    static public int SelectClipRgn(int hdc, int hrgn) {
        WinDC dc = WinDC.get(hdc);
        if (dc == null)
            return 0;
        if (hrgn == 0) {
            if (dc.hClipRgn != 0)
                WinRegion.get(dc.hClipRgn).close();
            return NULLREGION;
        } else {
            return SelectObject(hdc, hrgn);
        }
    }

    // HGDIOBJ SelectObject(HDC hdc, HGDIOBJ hgdiobj)
    static public int SelectObject(int hdc, int obj) {
        WinDC dc = WinDC.get(hdc);
        WinGDI gdi = WinGDI.getGDI(obj);

        if (dc == null || gdi == null)
            return 0;
        int old = 0;
        if (gdi instanceof WinBitmap) {
            old = dc.hBitmap;
            dc.hBitmap = gdi.handle;
            if (dc.owner) {
                dc.image.close();
            }
            dc.image = ((WinBitmap)gdi).createJavaBitmap();
            dc.owner = true;
        } else if (gdi instanceof WinFont) {
            old = dc.hFont;
            dc.hFont = gdi.handle;
        } else if (gdi instanceof WinRegion) {
            if (dc.hClipRgn != 0)
                WinRegion.get(dc.hClipRgn).close();
            dc.hClipRgn = WinRegion.get(obj).copy().handle;
            old = WinRegion.get(dc.hClipRgn).getType();
        } else if (gdi instanceof WinPen) {
            old = dc.hPen;
            dc.hPen = gdi.handle;
        } else if (gdi instanceof WinBrush) {
            old = dc.hBrush;
            dc.hBrush = gdi.handle;
        } else if (gdi instanceof WinPalette) {
            old = dc.hPalette;
            dc.hPalette = gdi.handle;
        } else {
            Win.panic("WinDC.select was not implemented for "+gdi);
        }
        return old;
    }

    // HPALETTE SelectPalette(HDC hdc, HPALETTE hpal, BOOL bForceBackground)
    static public int SelectPalette(int hdc, int hpal, int bForceBackground) {
        // :TODO:
        return SelectObject(hdc, hpal);
    }

    // COLORREF SetBkColor(HDC hdc, COLORREF crColor)
    static public int SetBkColor(int hdc, int crColor) {
        WinDC dc = WinDC.get(hdc);
        if (dc == null)
            return CLR_INVALID;
        int result = dc.bkColor;
        dc.bkColor = crColor;
        return result;
    }

    // int SetBkMode(HDC hdc, int iBkMode)
    static public int SetBkMode(int hdc, int iBkMode) {
        WinDC dc = WinDC.get(hdc);
        if (dc == null)
            return 0;
        int old = dc.bkMode;
        dc.bkMode = iBkMode;
        return old;
    }

    // COLORREF SetPixel(HDC hdc, int X, int Y, COLORREF crColor)
    static public int SetPixel(int hdc, int X, int Y, int crColor) {
        WinDC dc = WinDC.get(hdc);
        if (dc == null)
            return CLR_INVALID;
        BufferedImage bi = dc.getImage();
        bi.setRGB(X, Y, crColor);
        return bi.getRGB(X, Y);
    }

    // COLORREF SetTextColor(HDC hdc, COLORREF crColor)
    static public int SetTextColor(int hdc, int crColor) {
        WinDC dc = WinDC.get(hdc);
        if (dc == null)
            return CLR_INVALID;
        int oldColor = Pixel.BGRtoRGB(dc.textColor);
        dc.textColor = Pixel.BGRtoRGB(crColor);
        return oldColor;
    }

    // BOOL WINAPI TextOutA( HDC hdc, INT x, INT y, LPCSTR str, INT count )
    static public int TextOutA(int hdc, int x, int y, int str, int count) {
        return ExtTextOutA(hdc, x, y, 0, NULL, str, count, NULL );
    }

    static final private int TRANSPARENT = 1;
    static final private int OPAQUE = 2;

    WinBitmap bitmap;

    int bkColor = 0xFFFFFFFF;
    int textColor = 0xFF000000;
    int hFont;
    boolean owner = false;
    int hPalette = 0;
    int bkMode = OPAQUE;
    JavaBitmap image;
    int hBitmap;
    int hClipRgn;
    int hPen;
    int hBrush;
    int cx;
    int cy;

    public WinDC(int handle, JavaBitmap image, boolean owner) {
        super(handle);
        this.image = image;
        this.owner = owner;
        if (image != null) {
            cx = image.getWidth();
            cy = image.getHeight();
        } else {
            cx = 1;
            cy = 1;
            this.image = StaticData.screen; // :TODO:
            this.owner = false;
        }
        bkColor = 0xFFFFFFFF;
        textColor = 0xFF000000;
        hPen = defaultPen.handle;
        hBrush = defaultBrush.handle;
        hPalette = defaultPalette.handle;
        hFont = defaultFont.handle;
    }

    private static final int DRIVERVERSION =   0;
    private static final int TECHNOLOGY =      2;
    private static final int HORZSIZE =        4;
    private static final int VERTSIZE =        6;
    private static final int HORZRES =         8;
    private static final int VERTRES =         10;
    private static final int BITSPIXEL =       12;
    private static final int PLANES =          14;
    private static final int NUMBRUSHES =      16;
    private static final int NUMPENS =         18;
    private static final int NUMMARKERS =      20;
    private static final int NUMFONTS =        22;
    private static final int NUMCOLORS =       24;
    private static final int PDEVICESIZE =     26;
    private static final int CURVECAPS =       28;
    private static final int LINECAPS =        30;
    private static final int POLYGONALCAPS =   32;
    private static final int TEXTCAPS =        34;
    private static final int CLIPCAPS =        36;
    private static final int RASTERCAPS =      38;
    private static final int ASPECTX =         40;
    private static final int ASPECTY =         42;
    private static final int ASPECTXY =        44;
    private static final int LOGPIXELSX =      88;
    private static final int LOGPIXELSY =      90;
    private static final int CAPS1 =           94;
    private static final int SIZEPALETTE =     104;
    private static final int NUMRESERVED =     106;
    private static final int COLORRES =        108;
    
    private static final int PHYSICALWIDTH =   110;
    private static final int PHYSICALHEIGHT =  111;
    private static final int PHYSICALOFFSETX = 112;
    private static final int PHYSICALOFFSETY = 113;
    private static final int SCALINGFACTORX =  114;
    private static final int SCALINGFACTORY =  115;
    private static final int VREFRESH =        116;
    private static final int DESKTOPVERTRES =  117;
    private static final int DESKTOPHORZRES =  118;
    private static final int BLTALIGNMENT =    119;
    private static final int SHADEBLENDCAPS =  120;
    private static final int COLORMGMTCAPS =   121;

    static final int DT_TOP =             0x00000000;
    static final int DT_LEFT =            0x00000000;
    static final int DT_CENTER =          0x00000001;
    static final int DT_RIGHT =           0x00000002;
    static final int DT_VCENTER =         0x00000004;
    static final int DT_BOTTOM =          0x00000008;
    static final int DT_WORDBREAK =       0x00000010;
    static final int DT_SINGLELINE =      0x00000020;
    static final int DT_EXPANDTABS =      0x00000040;
    static final int DT_TABSTOP =         0x00000080;
    static final int DT_NOCLIP =          0x00000100;
    static final int DT_EXTERNALLEADING = 0x00000200;
    static final int DT_CALCRECT =        0x00000400;
    static final int DT_NOPREFIX =        0x00000800;
    static final int DT_INTERNAL =        0x00001000;
    static final int DT_EDITCONTROL =     0x00002000;
    static final int DT_PATH_ELLIPSIS =   0x00004000;
    static final int DT_END_ELLIPSIS =    0x00008000;
    static final int DT_MODIFYSTRING =    0x00010000;
    static final int DT_RTLREADING =      0x00020000;
    static final int DT_WORD_ELLIPSIS =   0x00040000;

    protected void onFree() {
        if (owner) {
            image.close();
        }
        image = null;
        super.onFree();
    }

    public BufferedImage getImage() {
        return image.getImage();
    }
}
