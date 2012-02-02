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
    static public WinBitmap create(int address) {
        return new WinBitmap(nextObjectId(), address);
    }

    static public WinBitmap get(int handle) {
        WinObject object = getObject(handle);
        if (object == null || !(object instanceof WinBitmap))
            return null;
        return (WinBitmap)object;
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
    int[] palette;

    public WinBitmap(int handle, int address) {
        super(handle);
        this.address = address;
        parseBitmap(address);
    }

    public JavaBitmap createJavaBitmap() {
        BufferedImage bi = Pixel.createImage(bits, bitCount, palette, width, height, true);
        return new JavaBitmap(bi, bitCount, width, height, palette);
    }

    public String toString() {
        return "BITMAP "+width+"x"+height+"@"+bitCount+"bpp";
    }
    private void parseBitmap(int address) {
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

        if (biSizeImage == 0) {
            biSizeImage = (bitCount + 7) / 8 * width * Math.abs(height);
        }
        bits = address+40;

        if (height < 0) {
            Win.panic("Top down bitmaps not supported yet");
        }
        if (bitCount == 8) {
            if (biClrUsed == 0)
                biClrUsed = 256;
        } else { // if (bitCount != 15 && bitCount != 16 && bitCount != 24) {
            Win.panic("Was not expecting to load a bitmap with "+bitCount+" bits per pixel");
        }
        bits+=4*biClrUsed;
        palette = new int[biClrUsed];
        for (int i=0;i<palette.length;i++) {
            palette[i] = Pixel.BGRtoRGB(is.readInt());
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
