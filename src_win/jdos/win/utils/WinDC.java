package jdos.win.utils;

import jdos.win.Win;

import java.awt.image.BufferedImage;

public class WinDC extends WinObject {
    WinBitmap bitmap;

    int address;
    int width;
    int height;
    int bpp;
    int[] palette;
    boolean addressOwner = false;

    public WinDC(int handle, int bpp, int address, int width, int height, int[] palette) {
        super(handle);
        this.bpp = bpp;
        this.address = address;
        this.width = width;
        this.height = height;
        this.palette = palette;
    }

    protected void onFree() {
        if (address != 0 && addressOwner) {
            WinSystem.getCurrentProcess().heap.free(address);
        }
        super.onFree();
    }

    public BufferedImage getImage() {
        return Pixel.createImage(address, bpp, palette, width, height);
    }

    public void writeImage(BufferedImage image) {
        Pixel.writeImage(address, image, bpp, width, height);
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
