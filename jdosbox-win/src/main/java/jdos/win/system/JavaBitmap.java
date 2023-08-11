package jdos.win.system;

import jdos.cpu.Paging;
import jdos.hardware.Memory;
import jdos.win.Win;
import jdos.win.builtin.WinAPI;
import jdos.win.builtin.kernel32.WinProcess;

import java.awt.*;
import java.awt.image.*;

public class JavaBitmap {
    public BufferedImage image;
    private int width;
    private int height;
    private int bpp;
    private int[] palette;
    private int address;
    private PageHandler8 handler8;
    private PageHandler16 handler16;

    public JavaBitmap(BufferedImage image, int bpp, int width, int height, int[] palette) {
        this.image = image;
        this.bpp = bpp;
        this.width = width;
        this.height = height;
        this.palette = palette;
    }

    public void set(BufferedImage image, int bpp, int width, int height, int[] palette) {
        this.image = image;
        this.bpp = bpp;
        this.width = width;
        this.height = height;
        this.palette = palette;
    }

    public void set(JavaBitmap bitmap) {
        this.image = bitmap.image;
        this.bpp = bitmap.bpp;
        this.width = bitmap.width;
        this.height = bitmap.height;
        this.palette = bitmap.palette;
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
        if (bpp<=8)
            colorKey = palette[colorKey];
        if (colorKey != cachedColorKey) {
            if (bpp<=8) {
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
            } else {
                BufferedImage result = new BufferedImage(image.getWidth(), image.getHeight(), Transparency.BITMASK);
                int c = colorKey & 0xFFFFFF;
                for (int x=0;x<result.getWidth();x++) {
                    for (int y=0;y<result.getHeight();y++) {
                        int p = image.getRGB(x, y);
                        if ((p & 0xFFFFFF) == c)
                            result.setRGB(x, y, c);
                        else
                            result.setRGB(x, y, p);
                    }
                }
                cachedImageColorKey = result;
            }
            cachedColorKey = colorKey;
        }
        return cachedImageColorKey;
    }

    static private class PageHandler8 extends Paging.PageHandler {
        byte[] data;
        int address;

        public PageHandler8(int address) {
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

    static private class PageHandler16 extends Paging.PageHandler {
        short[] data;
        int address;

        public PageHandler16(int address) {
            this.address = address;
        }

        public /*Bitu*/int readb(/*PhysPt*/int addr) {
            int index = addr - address;
            return (data[(index >> 1)] >>> ((index & 0x1) << 3)) & 0xFF;
        }

        public /*Bitu*/int readw(/*PhysPt*/int addr) {
            int index = addr - address;
            int rem = (index & 0x1);
            if (rem == 0) {
              return data[index >>> 1];
            }
            short[] local = data;
            index = (index >>> 1);
            return local[index] >>> 8 | local[index+1] << 8;
        }

        public /*Bitu*/int readd(/*PhysPt*/int addr) {
            int index = addr - address;
            int rem = (index & 0x1);
            if (rem == 0) {
                index >>>=1;
                return (data[index] & 0xFFFF) | data[index+1] << 16;
            }
            return (readw(addr) & 0xFFFF) | readw(addr+2) << 16;
        }

        public void writeb(/*PhysPt*/int addr,/*Bitu*/int value) {
            int address = addr - this.address;
            int off = (address & 0x1) << 3;
            short[] local = data;
            int mask = ~(0xFF << off);
            int index = (address >>> 1);
            int val = local[index] & mask | (value & 0xFF) << off;
            local[index] = (short)val;
        }

        public void writew(/*PhysPt*/int addr,/*Bitu*/int val) {
            int address = addr - this.address;
            int rem = (address & 0x1);
            if (rem == 0) {
               data[address >>> 1] = (short)val;
            } else {
              int index = (address >>> 1);
              short[] local = data;
              int off = rem << 3;
              local[index] = (short)((local[index] & ~0xFF) | (val << off));
              index++;
              local[index] = (short)((local[index] & 0xFF) | (val >>> (32-off)));
            }
        }

        public void writed(/*PhysPt*/int addr,/*Bitu*/int val) {
            int address = addr - this.address;
            int rem = (address & 0x1);
            if (rem == 0) {
                int index = address >>> 1;
                data[index] = (short)val;
                data[index+1] = (short)(val >>> 16);
            } else {
                writew(addr, val & 0xFFFF);
                writew(addr+2, val >> 16);
            }
        }
    }

    public void lock() {
        if (bpp == 8)
            handler8.data = ((DataBufferByte)image.getRaster().getDataBuffer()).getData();
        else if (bpp == 16)
            handler16.data = ((DataBufferUShort)image.getRaster().getDataBuffer()).getData();
    }

    public void unlock() {

    }

    public int map() {
        if (address != 0) {
            return address;
        }
        int size = width*height*bpp/8;
        address = (int)WinSystem.getCurrentProcess().addressSpace.getNextAddress(WinProcess.ADDRESS_VIDEO_BITMAP_START, size, true);
        WinSystem.getCurrentProcess().addressSpace.alloc(address, size);
        int frameCount = (size + 0xFFF) >> 12;
        int frame = address >>> 12;
        int frameStart = frame;
        Paging.PageHandler handler;

        if (bpp == 8) {
            handler8 = new PageHandler8(address);
            handler = handler8;
        } else if (bpp == 16) {
            handler16 = new PageHandler16(address);
            handler = handler16;
        } else {
            Win.panic("BPP "+bpp+" not implemented yet in JavaBitmap");
            handler = null;
        }

        for (int i=0;i<frameCount;i++) {
            Paging.readhandler[frame] = handler;
            Paging.writehandler[frame++] = handler;
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
            Paging.readhandler[frame] = null;
            Paging.writehandler[frame++] = null;
        }
        WinSystem.getCurrentProcess().addressSpace.free(address);
        if (WinAPI.LOG) {
            System.out.println("JavaBitmap.unmap address=0x"+Long.toString(address & 0xFFFFFFFFl, 16));
        }
        address = 0;
        if (bpp == 8) {
            handler8.data = null;
            handler8 = null;
        } else if (bpp == 16) {
            handler16.data = null;
            handler16 = null;
        }
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
