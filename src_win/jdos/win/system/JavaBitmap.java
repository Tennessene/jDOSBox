package jdos.win.system;

import jdos.cpu.Paging;
import jdos.hardware.Memory;
import jdos.win.Win;
import jdos.win.builtin.WinAPI;
import jdos.win.builtin.kernel32.WinProcess;

import java.awt.image.*;

public class JavaBitmap {
    public BufferedImage image;
    private int width;
    private int height;
    private int bpp;
    private int[] palette;
    private int address;
    private BytePageHandler handler;

    public JavaBitmap(BufferedImage image, int bpp, int width, int height, int[] palette) {
        this.image = image;
        this.bpp = bpp;
        this.width = width;
        this.height = height;
        this.palette = palette;
    }

    public void close() {
        palette = null;
        image = null;
        cachedImageColorKey = null;
        cachedColorKey = -1;
        if (address != 0)
            unmap();
    }

    public int[] getPalette() {
        return palette;
    }

    public BufferedImage getImage() {
        return image;
    }

    public int getBpp() {
        return bpp;
    }

    public int getHeight() {
        return height;
    }

    public int getWidth() {
        return width;
    }

    public void setPalette(int address) {
        for (int i=0;i<256;i++) {
            palette[i] = 0xFF000000 | Memory.mem_readd(address);
            address+=4;
        }
        WritableRaster raster = image.getRaster();
        ColorModel cm = createColorModel(bpp, palette, false);
        image = new BufferedImage(cm, raster, false, null);
        cachedImageColorKey = null;
        cachedColorKey = -1;
    }

    private int cachedColorKey = -1;
    private BufferedImage cachedImageColorKey = null;

    public BufferedImage getImageColorKey(int colorKey) {
        colorKey = palette[colorKey];
        if (colorKey != cachedColorKey) {
            int[] p = (int[])palette.clone();
            for (int i=0;i<p.length;i++) {
                if ((p[i] & 0xFFFFFF) == (colorKey & 0xFFFFFF))
                    p[i]&=0xFFFFFF;
                else
                    p[i]|=0xFF000000;
            }
            WritableRaster raster = image.getRaster();
            ColorModel cm = createColorModel(bpp, p, true);
            cachedImageColorKey = new BufferedImage(cm, raster, false, null);
            cachedColorKey = colorKey;
        }
        return cachedImageColorKey;
    }

    static private class BytePageHandler extends Paging.PageHandler {
        byte[] data;
        int address;

        public BytePageHandler(int address) {
            this.address = address;
        }

        public /*Bitu*/int readb(/*PhysPt*/int addr) {
            int index = addr - address;
            return data[index] & 0xFF;
        }

        public /*Bitu*/int readw(/*PhysPt*/int addr) {
            int index = addr - address;
            return (data[index] & 0xFF) | ((data[index+1] & 0xFF) << 8);
        }

        public /*Bitu*/int readd(/*PhysPt*/int addr) {
            int index = addr - address;
            return (data[index] & 0xFF) | ((data[index+1] & 0xFF) << 8) | ((data[index+2] & 0xFF) << 16) | ((data[index+3] & 0xFF) << 24);
        }

        public void writeb(/*PhysPt*/int addr,/*Bitu*/int val) {
            int index = addr - address;
            data[index] = (byte)val;
        }

        public void writew(/*PhysPt*/int addr,/*Bitu*/int val) {
            int index = addr - address;
            data[index] = (byte)val;
            data[index+1] = (byte)(val >> 8);
        }

        public void writed(/*PhysPt*/int addr,/*Bitu*/int val) {
            int index = addr - address;
            data[index] = (byte)val;
            data[index+1] = (byte)(val >> 8);
            data[index+2] = (byte)(val >> 16);
            data[index+3] = (byte)(val >> 24);
        }
    }

    public void lock() {
        handler.data = ((DataBufferByte)image.getRaster().getDataBuffer()).getData();
    }

    public void unlock() {
        handler.data = null;
    }

    public int map() {
        if (address != 0) {
            Win.panic("Not allowed to map JavaBitmap more than once");
        }
        int size = width*height;
        address = (int)WinSystem.getCurrentProcess().addressSpace.getNextAddress(WinProcess.ADDRESS_VIDEO_BITMAP_START, size, true);
        WinSystem.getCurrentProcess().addressSpace.alloc(address, size);
        int frameCount = (size + 0xFFF) >> 12;
        int frame = address >>> 12;
        int frameStart = frame;
        handler = new BytePageHandler(address);

        for (int i=0;i<frameCount;i++) {
            Paging.paging.tlb.readhandler[frame] = handler;
            Paging.paging.tlb.writehandler[frame++] = handler;
        }
        if (WinAPI.LOG) {
            System.out.println("JavaBitmap.map address=0x"+Long.toString(address & 0xFFFFFFFFl, 16)+" size="+size+" frames="+frameStart+"-"+frame+" handler="+handler+" this="+this);
        }
        return address;
    }

    public void unmap() {
        int size = width*height;
        int frameCount = (size + 0xFFF) >> 12;
        int frame = address >>> 12;

        for (int i=0;i<frameCount;i++) {
            Paging.paging.tlb.readhandler[frame] = null;
            Paging.paging.tlb.writehandler[frame++] = null;
        }
        WinSystem.getCurrentProcess().addressSpace.free(address);
        if (WinAPI.LOG) {
            System.out.println("JavaBitmap.unmap address=0x"+Long.toString(address & 0xFFFFFFFFl, 16));
        }
        address = 0;
        handler.data = null;
        handler = null;
    }

    static public IndexColorModel createColorModel(int bpp, int[] palette, boolean alpha) {
        byte[] r = new byte[palette.length];
        byte[] g = new byte[palette.length];
        byte[] b = new byte[palette.length];
        byte[] a = new byte[palette.length];

        for (int i=0;i<palette.length;i++) {
            r[i] = (byte)palette[i];
            g[i] = (byte)(palette[i] >> 8);
            b[i] = (byte)(palette[i] >> 16);
            a[i] = (byte)(palette[i] >>> 24);
        }
        if (alpha)
            return new IndexColorModel(bpp, palette.length, r, g, b, a);
        else
            return new IndexColorModel(bpp, palette.length, r, g, b);
    }

    static public int[] getDefaultPalette() {
        int[] table = new int[256];
        table[0]=0x000000;
        table[1]=0x800000;
        table[2]=0x008000;
        table[3]=0x808000;
        table[4]=0x000080;
        table[5]=0x800080;
        table[6]=0x008080;
        table[7]=0xc0c0c0;
        table[8]=0xc0dcc0;
        table[9]=0xa6caf0;
        int pos = 10;
        for (int r = 0;r<256;r+=51) {
            for (int g = 0;r<256;r+=51) {
                for (int b = 0;r<256;r+=51) {
                    table[pos++] = (r<<16)|(g<<8)|b;
                }
            }
        }
        table[246]=0xfffbf0;
        table[247]=0xa0a0a4;
        table[248]=0x808080;
        table[249]=0xff0000;
        table[250]=0x00ff00;
        table[251]=0xffff00;
        table[252]=0x0000ff;
        table[253]=0xff00ff;
        table[254]=0x00ffff;
        table[255]=0xffffff;
        return table;
    }
}
