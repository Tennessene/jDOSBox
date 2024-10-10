package jdos.win.builtin.gdi32;

import jdos.gui.Main;
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

    // BOOL ExtTextOut(HDC hdc, int X, int Y, UINT fuOptions, const RECT *lprc, LPCTSTR lpString, UINT cbCount, const INT *lpDx)
    static public int ExtTextOutA(int hdc, int X, int Y, int fuOptions, int lprc, int lpString, int cbCount, int lpDx) {
        String text = StringUtil.getString(lpString, cbCount);
        return ExtTextOut(hdc, X, Y, fuOptions, lprc, text, lpDx);
    }
    static public int ExtTextOutW(int hdc, int X, int Y, int fuOptions, int lprc, int lpString, int cbCount, int lpDx) {
        String text = StringUtil.getStringW(lpString, cbCount);
        return ExtTextOut(hdc, X, Y, fuOptions, lprc, text, lpDx);
    }
    static public int ExtTextOut(int hdc, int X, int Y, int fuOptions, int lprc, String text, int lpDx) {
        log("ExtTextOutA not fully implemented yet");
        WinDC dc = WinDC.get(hdc);
        if (dc == null) {
            return FALSE;
        }
        Graphics2D g = dc.getGraphics();

        FontRenderContext frc = g.getFontRenderContext();
        Font font = WinFont.get(dc.hFont).font;
        g.setFont(font);

        int sw = (int)font.getStringBounds(text, frc).getWidth();
        LineMetrics lm = font.getLineMetrics(text, frc);
        int sh = (int)(lm.getAscent() + lm.getDescent());

        if (dc.bkMode == OPAQUE) {
            g.setColor(new Color(dc.bkColor));
            g.fillRect(dc.x+X, dc.y+Y, sw, sh);
        }
        g.setColor(new Color(dc.textColor | 0xFF000000));
        g.drawString(text, dc.x+X, dc.y+Y+sh-(int)lm.getDescent());
        g.dispose();

        if (dc.getImage() == StaticData.screen.getImage())
            Main.drawImage(dc.getImage());
        return WinAPI.TRUE;
    }

    // int GetClipBox(HDC hdc,  LPRECT lprc)
    static public int GetClipBox(int hdc, int rect)  {
        WinDC dc = WinDC.get(hdc);
        if (dc==null) return ERROR;
        if (dc.hClipRgn != 0)
            return WinRegion.GetRgnBox(dc.hClipRgn, rect);
        new WinRect(0, 0, dc.clipCx, dc.clipCy).write(rect);
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
            case LOGPIXELSX:
                return 96;
            case LOGPIXELSY:
                return 96;
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
        if (dc == null || (nXPos>=0 && nXPos<dc.clipCx && nYPos>=0 && nYPos<dc.clipCy))
            return CLR_INVALID;
        BufferedImage bi = dc.getImage();
        return bi.getRGB(dc.x+nXPos, dc.y+nYPos);
    }

    // COLORREF GetTextColor(HDC hdc)
    static public int GetTextColor(int hdc) {
        WinDC dc = WinDC.get(hdc);
        if (dc == null)
            return CLR_INVALID;
        return dc.textColor;
    }

    // BOOL PatBlt(HDC hdc, int nXLeft, int nYLeft, int nWidth, int nHeight, DWORD dwRop)
    static public int PatBlt(int hdc, int nXLeft, int nYLeft, int nWidth, int nHeight, int dwRop) {
        System.out.println("PatBlt not fully implemented yet");
        WinDC dc = WinDC.get(hdc);
        if (dc == null)
            return FALSE;

        int color = 0xFF000000 | WinBrush.get(dc.hBrush).color;
        Graphics graphics = dc.getGraphics();
        graphics.setColor(new Color(color));
        graphics.fillRect(nXLeft+dc.x, nYLeft+dc.y, nWidth, nHeight);
        graphics.dispose();
        if (dc.getImage() == StaticData.screen.getImage())
            Main.drawImage(dc.getImage());
        return TRUE;
    }

    // UINT RealizePalette(HDC hdc)
    static public int RealizePalette(int hdc) {
//        WinDC dc = WinDC.get(hdc);
//        if (dc == null)
//            return 0;
//        if (StaticData.currentPrimarySurface != 0) {
//            WinPalette winPalette = WinPalette.get(dc.hPalette);
//            if (winPalette != null) {
//                int palette = IDirectDrawSurface.getPalette(StaticData.currentPrimarySurface);
//                if (palette != 0) {
//                    int data = palette+IDirectDrawPalette.OFFSET_COLOR_DATA + IUnknown.OFFSET_DATA_START;
//
//                    for (int i=0;i<winPalette.palette.length;i++) {
//                        writed(data+i*4, Pixel.BGRtoRGB(winPalette.palette[i]));
//                    }
//                    IDirectDrawSurface.lastPaletteChange = WinSystem.getTickCount();
//                    return winPalette.palette.length;
//                }
//            }
//        }
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
            dc.image = ((WinBitmap)gdi).createJavaBitmap(true);
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
        if (dc == null || (X>=0 && X<dc.clipCx && Y>=0 && Y<dc.clipCy))
            return CLR_INVALID;
        BufferedImage bi = dc.getImage();
        bi.setRGB(dc.x+X, dc.y+Y, crColor);
        return bi.getRGB(dc.x+X, dc.y+Y);
    }

    // int SetROP2(HDC hdc, int fnDrawMode)
    static public int SetROP2(int hdc, int fnDrawMode) {
        WinDC dc = WinDC.get(hdc);
        if (dc == null)
            return 0;
        int result = dc.ROPmode;
        dc.ROPmode = fnDrawMode;
        return result;
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
    static public int TextOutW(int hdc, int x, int y, int str, int count) {
        return ExtTextOutW(hdc, x, y, 0, NULL, str, count, NULL);
    }
    static public int TextOut(int hdc, int x, int y, String text) {
        return ExtTextOut(hdc, x, y, 0, NULL, text, NULL);
    }

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
    public int hPen;
    public int hBrush;
    public int clipCx;
    public int clipCy;
    public int x;
    public int y;
    public int clipX;
    public int clipY;
    int ROPmode=R2_COPYPEN;
    float miterLimit = 10.0f; /* 10.0 is the default, from MSDN */

    public int CursPosX;
    public int CursPosY;

    public WinDC(int handle, JavaBitmap image, boolean owner) {
        super(handle);
        this.image = image;
        this.owner = owner;
        if (image != null) {
            clipCx = image.getWidth();
            clipCy = image.getHeight();
        } else {
            clipCx = 1;
            clipCy = 1;
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

    public void setOffset(int x, int y, int clipX, int clipY, int clipCx, int clipCy) {
        this.x = x;
        this.y = y;
        this.clipCx = clipCx;
        this.clipCy = clipCy;
        this.clipX = clipX;
        this.clipY = clipY;
    }

    public Graphics2D getGraphics() {
        BufferedImage image = getImage();
        Graphics2D g = image.createGraphics();
        // :TODO: merge clip
        if (clipCx>0 && clipCy>0)
            g.setClip(x+clipX, y+clipY, clipCx, clipCy);
        return g;
    }

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

    public boolean isScreen() {
        return (image == StaticData.screen);
    }
}
