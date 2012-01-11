package jdos.win.utils;

import jdos.hardware.Memory;
import jdos.win.Win;
import jdos.win.builtin.WinAPI;
import jdos.win.loader.winpe.LittleEndianFile;

import java.awt.*;
import java.awt.font.FontRenderContext;
import java.awt.font.LineMetrics;
import java.awt.image.BufferedImage;

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
    boolean addressOwner = false;
    int hPalette = 0;
    int bkMode = OPAQUE;
    BufferedImage image;

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
            default:
                Win.panic("GetDevice caps +"+nIndex+" not implemented yet.");
        }
        return 0;
    }

    public int setBkMode(int iBkMode) {
        int old = bkMode;
        bkMode = iBkMode;
        return old;
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

    public int getTextExtent(String text, int lpSize) {
        BufferedImage bi = getImage();
        Graphics2D g = (Graphics2D)bi.getGraphics();
        FontRenderContext frc = g.getFontRenderContext();
        Font font = g.getFont().deriveFont(16f);
        g.setFont(font);
        int sw = (int)font.getStringBounds(text, frc).getWidth();
        LineMetrics lm = font.getLineMetrics(text, frc);
        int sh = (int)(lm.getAscent() + lm.getDescent());
        Memory.mem_writed(lpSize, sw);
        Memory.mem_writed(lpSize+4, sh);
        return WinAPI.TRUE;
    }

    public int drawText(int pText, int count, int pRect, int flags) {
        WinRect rect = new WinRect(pRect);
        String text = new LittleEndianFile(pText).readCString(count);

        BufferedImage bi = getImage();
        Graphics2D g = (Graphics2D)bi.getGraphics();
        FontRenderContext frc = g.getFontRenderContext();
        Font font = g.getFont().deriveFont(16f);
        g.setFont(font);
        int sw = (int)font.getStringBounds(text, frc).getWidth();
        LineMetrics lm = font.getLineMetrics(text, frc);
        int sh = (int)(lm.getAscent() + lm.getDescent());

        if ((flags & 0x1)!=0) { // DT_CENTER
            int x = (rect.right-rect.left)/2 - sw/2;
            int y = rect.top;
            textOut(x, y, text);
        } else {
            textOut(rect.left, rect.top, text);
        }
        System.out.println("drawText not fully implemented");
        return sh;
    }

    public int textOut(int x, int y, String text) {
        BufferedImage bi = getImage();
        Graphics2D g = (Graphics2D)bi.getGraphics();

        FontRenderContext frc = g.getFontRenderContext();
        Font font = g.getFont().deriveFont(16f);
        g.setFont(font);
        int sw = (int)font.getStringBounds(text, frc).getWidth();
        LineMetrics lm = font.getLineMetrics(text, frc);
        int sh = (int)(lm.getAscent() + lm.getDescent());

        if (bkMode == OPAQUE) {
            g.setColor(new Color(bkColor));
            g.fillRect(x, y, sw, sh);
        }
        g.setColor(new Color(textColor));
        g.drawString(text, x, y+sh-(int)lm.getDescent());
        Pixel.writeImage(address, bi, bpp, width, height);
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
        } else {
            Win.panic("WinDC.select was not implemented for "+gdi);
        }
        if (old != null)
            return old.handle;
        return 0;
    }
}
