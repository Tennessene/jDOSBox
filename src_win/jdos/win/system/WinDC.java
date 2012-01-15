package jdos.win.system;

import jdos.hardware.Memory;
import jdos.win.Win;
import jdos.win.builtin.WinAPI;
import jdos.win.loader.winpe.LittleEndianFile;
import jdos.win.utils.Pixel;

import java.awt.*;
import java.awt.font.FontRenderContext;
import java.awt.font.LineMetrics;
import java.awt.image.BufferedImage;
import java.util.Arrays;

public class WinDC extends WinObject {
    static final private int TRANSPARENT = 1;
    static final private int OPAQUE = 2;

    WinBitmap bitmap;

    int address;
    int width;
    int height;
    int bpp;
    int bkColor = 0xFFFFFFFF;
    int textColor = 0xFF000000;
    int[] palette;
    WinFont font;
    boolean addressOwner = false;
    int hPalette = 0;
    int bkMode = OPAQUE;
    BufferedImage image;
    WinRegion clipRegion = null;
    WinPen pen = null;

    public WinDC(int handle, int bpp, int address, int width, int height, int[] palette) {
        super(handle);
        if (bpp == 8 && palette == null) {
            Win.panic("WinDC got a request for an 8-bit display but the palette was null");
        }
        this.bpp = bpp;
        this.address = address;
        this.width = width;
        this.height = height;
        this.palette = palette;
    }

    public void refresh(int address, int width, int height, int[] palette) {
        this.address = address;
        this.width = width;
        this.height = height;
        this.palette = palette;
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
    
    public int getCaps(int nIndex) {
        switch (nIndex) {
            case RASTERCAPS:
                int result = 0x0001|0x0008|0x0800; // RC_BITBLT | RC_BITMAP64 | RC_STRETCHBLT
                if (bpp<=8)
                    result |= 0x0100; //RC_PALETTE
                return result;
            case BITSPIXEL:
                return bpp;
            default:
                Win.panic("GetDevice caps "+nIndex+" not implemented yet.");
        }
        return 0;
    }

    public int selectClipRgn(WinRegion region) {
        clipRegion = region;
        if (region.rects.size() == 1)
            return 2; // SIMPLEREGION
        if (region.rects.size() == 0)
            return 1; // NULLREGION
        return 3; // COMPLEXREGION
    }

    public int fillRect(int lprc, int hbr) {
        int color;
        if (hbr < 64) {
            color = WinSystem.getSystemColor(hbr - 1);
        } else {
            color = ((WinBrush)WinSystem.getObject(hbr)).color;
        }
        WinRect rect = new WinRect(lprc);
        BufferedImage image = getImage();
        Graphics graphics = image.getGraphics();
        graphics.setColor(new Color(color));
        graphics.fillRect(rect.left, rect.right, rect.width(), rect.height());
        graphics.dispose();
        return WinAPI.TRUE;
    }

    public int setBkMode(int iBkMode) {
        int old = bkMode;
        bkMode = iBkMode;
        return old;
    }

    public int getPaletteEntries(int iStartIndex, int nEntries, int lppe) {
        if (palette != null) {
            for (int i=0;i<nEntries;i++) {
                Memory.mem_writed(lppe+i*4, palette[i+iStartIndex]);
            }
            return nEntries;
        }
        return 0;
    }

    public int setBkColor(int color) {
        int oldColor = Pixel.BGRtoRGB(bkColor);
        bkColor = Pixel.BGRtoRGB(color);
        return oldColor;
    }

    public int setTextColor(int color) {
        int oldColor = Pixel.BGRtoRGB(textColor);
        textColor = Pixel.BGRtoRGB(color);
        return oldColor;
    }

    public int getTextMetrics(int lptm) {
        BufferedImage bi = getImage();
        Graphics2D g = (Graphics2D)bi.getGraphics();
        Font font;
        if (this.font == null) {
            font = g.getFont().deriveFont(16f);
        } else {
            font = this.font.font;
        }
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

    public int getTextExtent(String text, int lpSize) {
        BufferedImage bi = getImage();
        Graphics2D g = (Graphics2D)bi.getGraphics();
        FontRenderContext frc = g.getFontRenderContext();
        Font font;
        if (this.font == null) {
            font = g.getFont().deriveFont(16f);

        } else {
            font = this.font.font;
        }
        g.setFont(font);
        int sw = (int)font.getStringBounds(text, frc).getWidth();
        LineMetrics lm = font.getLineMetrics(text, frc);
        int sh = (int)(lm.getAscent() + lm.getDescent());
        Memory.mem_writed(lpSize, sw);
        Memory.mem_writed(lpSize+4, sh);
        g.dispose();
        return WinAPI.TRUE;
    }

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

    public int drawText(int pText, int count, int pRect, int flags) {
        WinRect rect = new WinRect(pRect);
        String text;

        if (count == -1)
            text = new LittleEndianFile(pText).readCString();
        else
            text = new LittleEndianFile(pText).readCString(count);

        BufferedImage bi = getImage();
        Graphics2D g = (Graphics2D)bi.getGraphics();
        FontRenderContext frc = g.getFontRenderContext();
        Font font;
        if (this.font == null) {
            font = g.getFont().deriveFont(16f);

        } else {
            font = this.font.font;
        }
        g.setFont(font);
        int sw = (int)font.getStringBounds(text, frc).getWidth();
        LineMetrics lm = font.getLineMetrics(text, frc);
        int sh = (int)(lm.getAscent() + lm.getDescent());

        int x = rect.left;
        int y = rect.top;
        if ((flags & DT_CENTER)!=0) {
            x = (rect.right-rect.left)/2 - sw/2;
        } else if ((flags & DT_RIGHT)!=0) {
            x = rect.right - sw;
        }
        if ((flags & DT_BOTTOM)!=0) {
            y = rect.bottom-sh;
        }
        textOut(x, y, text);

        System.out.println("drawText not fully implemented");
        g.dispose();
        return sh;
    }

    public int textOut(int x, int y, String text) {
        BufferedImage bi = getImage();
        Graphics2D g = (Graphics2D)bi.getGraphics();

        FontRenderContext frc = g.getFontRenderContext();
        Font font;
        if (this.font == null) {
            font = g.getFont().deriveFont(16f);
        } else {
            font = this.font.font;
        }
        g.setFont(font);
        int sw = (int)font.getStringBounds(text, frc).getWidth();
        LineMetrics lm = font.getLineMetrics(text, frc);
        int sh = (int)(lm.getAscent() + lm.getDescent());

        if (bkMode == OPAQUE) {
            g.setColor(new Color(bkColor));
            g.fillRect(x, y, sw, sh);
        }
        g.setColor(new Color(textColor | 0xFF000000));
        g.drawString(text, x, y+sh-(int)lm.getDescent());
        Pixel.writeImage(address, bi, bpp, width, height);
        g.dispose();
        return WinAPI.TRUE;
    }

    public int getPixel(int x, int y) {
        BufferedImage bi = getImage();
        return bi.getRGB(x, y);
    }

    public int setPixel(int x, int y, int color) {
        BufferedImage bi = getImage();
        bi.setRGB(x, y, color);
        return bi.getRGB(x, y);
    }

    protected void onFree() {
        if (address != 0 && addressOwner) {
            WinSystem.getCurrentProcess().heap.free(address);
        }
        if (image != null)
            writeImage(image);
        super.onFree();
    }

    public void releaseImage() {
        if (image != null)
            writeImage(image);
        image = null;
    }

    public BufferedImage getImage() {
        if (image == null)
            image = Pixel.createImage(address, bpp, palette, width, height, false);
        return image;
    }

    public void writeImage(BufferedImage image) {
        Pixel.writeImage(address, image, bpp, width, height);
        this.image = null;
    }

    public int selectPalette(WinPalette palette, boolean bForceBackground) {
        int oldPalette = hPalette;
        hPalette = palette.getHandle();
        return oldPalette;
    }

    public int realizePalette() {
        if (hPalette != 0) {
            WinObject object = WinSystem.getObject(hPalette);
            if (object instanceof WinPalette) {
                return ((WinPalette)object).palette.length;
            }
        }
        return 0;
    }

    public int select(WinGDI gdi) {
        WinGDI old = null;

        if (gdi instanceof WinBitmap) {
            if (address != 0 && !addressOwner) {
                Win.panic("Tried to select a bitmap into a dc that is already backed by video memory");
            }
            old = bitmap;
            bitmap = (WinBitmap)gdi;
            width = bitmap.width;
            height = bitmap.height;
            if (address != 0)
                WinSystem.getCurrentProcess().heap.free(address);
            address = bitmap.createCompatibleCopy(bpp, palette);
            addressOwner = true;
        } else if (gdi instanceof WinFont) {
            font = (WinFont)gdi;
        } else if (gdi instanceof WinRegion) {
            clipRegion = (WinRegion)gdi;
        } else if (gdi instanceof WinPen) {
            pen = (WinPen)gdi;
        } else {
            Win.panic("WinDC.select was not implemented for "+gdi);
        }
        if (old != null)
            return old.handle;
        return 0;
    }
}
