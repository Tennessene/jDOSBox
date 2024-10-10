package jdos.win.builtin.gdi32;

import jdos.hardware.Memory;
import jdos.win.Win;
import jdos.win.builtin.user32.Resource;
import jdos.win.loader.winpe.LittleEndianFile;
import jdos.win.system.JavaBitmap;
import jdos.win.system.WinObject;
import jdos.win.system.WinSystem;
import jdos.win.utils.Pixel;

import java.awt.image.BufferedImage;

public class WinBitmap extends WinGDI {
    static public WinBitmap create(int address, boolean owner) {
        return new WinBitmap(nextObjectId(), address, DIB_RGB_COLORS, 0, owner);
    }
    static public WinBitmap create(int width, int height, int bpp, int data, int[] palette, boolean keepData) {
        return new WinBitmap(nextObjectId(), width, height, bpp, data, palette, keepData);
    }

    static public WinBitmap get(int handle) {
        WinObject object = getObject(handle);
        if (object == null || !(object instanceof WinBitmap))
            return null;
        return (WinBitmap)object;
    }

    // HBITMAP CreateBitmap(int nWidth, int nHeight, UINT cPlanes, UINT cBitsPerPel, const VOID *lpvBits)
    static public int CreateBitmap(int nWidth, int nHeight, int cPlanes, int cBitsPerPel, int lpvBits) {
        if (cPlanes != 1) {
            warn("CreateBitmap does not support "+cPlanes+" planes.");
            SetLastError(ERROR_INVALID_PARAMETER);
            return 0;
        }
        return create(nWidth, nHeight, cBitsPerPel, lpvBits, null, false).handle;
    }

    // HBITMAP CreateCompatibleBitmap(HDC hdc, int nWidth, int nHeight)
    static public int CreateCompatibleBitmap(int hdc, int nWidth, int nHeight) {
        WinDC dc = WinDC.get(hdc);
        if (dc == null) {
            return 0;
        }
        if (dc.hBitmap != 0) { // Memory DC
            WinBitmap bitmap = WinBitmap.get(dc.hBitmap);
            if (bitmap == null) {
                Win.panic("CreateCompatibleBitmap invalid bitmap was selected into a dc");
            }
            return WinBitmap.create(nWidth, nHeight, bitmap.bitCount, 0, bitmap.palette, true).handle;
        }
        return WinBitmap.create(nWidth, nHeight, dc.image.getBpp(), 0, dc.image.getPalette(), true).handle;
    }

    // HBITMAP LoadBitmap(HINSTANCE hInstance, LPCTSTR lpBitmapName)
    static public int LoadBitmapA(int hInstance, int lpBitmapName) {
        return Resource.LoadImageA(hInstance, lpBitmapName, IMAGE_BITMAP, 0, 0, 0);
    }

    int address;

    int width;
    int height;
    int bitCount;
    int planes;
    int bits;
    boolean bitsOwner = false;
    int[] palette;
    JavaBitmap cache;

    public WinBitmap(int handle, int address, int iUsuage, int hPalette, boolean owner) {
        super(handle);
        this.address = address;
        parseBitmap(address, iUsuage, hPalette);
        this.bitsOwner = owner;
    }

    public WinBitmap(int handle, int width, int height, int bpp, int data, int[] palette, boolean keepData) {
        super(handle);
        if (width<0)
            width = -width;
        if (height<0)
            height = -height;
        if (height==0 || width == 0) {
            height = 1;
            width = 1;
            data = 0;
        }
        this.bitCount = bpp;
        this.width = width;
        this.height = height;
        this.planes = 1;
        this.palette = palette;
        if (data != 0) {
            if (keepData) {
                bits = data;
            } else {
                int stride = (bpp * width / 8 + 3) & ~3;
                bits = WinSystem.getCurrentProcess().heap.alloc(stride*height, false);
                Memory.mem_memcpy(bits, data, stride*height);
            }
        } else {
            bits = WinSystem.getCurrentProcess().heap.alloc(4, false);
        }
        bitsOwner = true;
    }

    protected void onFree() {
        if (bitsOwner) {
            if (address!=0)
                WinSystem.getCurrentProcess().heap.free(address);
            else
                WinSystem.getCurrentProcess().heap.free(bits);
        }
        super.onFree();
    }

    public JavaBitmap createJavaBitmap(boolean flip) {
        if (cache == null) {
            BufferedImage bi = Pixel.createImage(bits, bitCount, palette, width, height, flip);
            cache = new JavaBitmap(bi, bitCount, width, height, palette);
        }
        return cache;
    }

    public String toString() {
        return "BITMAP "+width+"x"+height+"@"+bitCount+"bpp";
    }
    private void parseBitmap(int address, int iUsuage, int hPalette) {
        LittleEndianFile is = new LittleEndianFile(address);
        int biSize = is.readInt();
        width = is.readInt();
        height = is.readInt();
        planes = is.readUnsignedShort();
        bitCount = is.readUnsignedShort();
        int biCompression = is.readInt();
        int biSizeImage = is.readInt();
        int biXPelsPerMeter = is.readInt();
        int biYPelsPerMeter = is.readInt();
        int biClrUsed = is.readInt();
        int biClrImportant = is.readInt();
        int[] refPalette = null;

        if (iUsuage == DIB_PAL_COLORS)
            refPalette = WinPalette.get(hPalette).palette;

        if (biSizeImage == 0) {
            if (bitCount<8)
                biSizeImage = (((bitCount * width + 7) / 8 + 3) & ~3)* Math.abs(height);
            else
                biSizeImage = (((bitCount + 7) / 8 * width + 3) & ~3)* Math.abs(height);
        }
        bits = address+40;

        if (height < 0) {
            Win.panic("Top down bitmaps not supported yet");
        }
        if (bitCount == 8) {
            if (biClrUsed == 0)
                biClrUsed = 256;
        } else if (bitCount == 1) {
            if (biClrUsed == 0)
                biClrUsed = 2;
        } else { // if (bitCount != 15 && bitCount != 16 && bitCount != 24) {
            Win.panic("Was not expecting to load a bitmap with "+bitCount+" bits per pixel");
        }
        bits+=4*biClrUsed;
        palette = new int[biClrUsed];
        for (int i=0;i<palette.length;i++) {
            if (iUsuage == DIB_RGB_COLORS)
                palette[i] = Pixel.BGRtoRGB(is.readInt());
            else
                palette[i] = 0xFF000000|refPalette[is.readUnsignedShort()];
        }
    }
    /*
    typedef struct tagBITMAP {
      LONG   bmType;
      LONG   bmWidth;
      LONG   bmHeight;
      LONG   bmWidthBytes;
      WORD   bmPlanes;
      WORD   bmBitsPixel;
      LPVOID bmBits;
    }
     */
    public static final int BITMAP_SIZE = 24;

    public int get(int address, int size) {
        if (address == 0)
            return BITMAP_SIZE;
        if (size<BITMAP_SIZE) {
            Win.panic("GetObject for bitmap: "+size+" < "+BITMAP_SIZE);
        }
        Memory.mem_writed(address, 0);
        Memory.mem_writed(address+4, width);
        Memory.mem_writed(address+8, height);
        Memory.mem_writed(address+12, width * (((bitCount + 7) / 8)+3)/4);
        Memory.mem_writew(address + 16, planes);
        Memory.mem_writew(address + 18, bitCount);
        Memory.mem_writed(address+20, bits);
        return BITMAP_SIZE;
    }

    public int createCompatibleCopy(int bpp, int[] dstPalette) {
        int pitch = Pixel.getPitch(width, bpp);
        int size = pitch * height;
        int result = WinSystem.getCurrentProcess().heap.alloc(size, true);
        Pixel.copy(bits, bitCount, palette, result, bpp, dstPalette, width, height, true);
        return result;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }
}
