package jdos.win.utils;

import jdos.hardware.Memory;
import jdos.win.Win;

import java.awt.*;
import java.awt.image.*;

public class Pixel {
    static public int getPitch(int width, int bpp) {
        return ((width * ((bpp + 7) / 8) + 3) / 4) * 4;
    }

    static public BufferedImage createImage(int src, int srcBpp, int[] srcPalette, int width, int height) {
        if (srcPalette != null) {
            byte[] r = new byte[srcPalette.length];
            byte[] g = new byte[srcPalette.length];
            byte[] b = new byte[srcPalette.length];

            for (int i=0;i<srcPalette.length;i++) {
                r[i] = (byte)srcPalette[i];
                g[i] = (byte)(srcPalette[i] >> 8);
                b[i] = (byte)(srcPalette[i] >> 16);
            }
            IndexColorModel sp = new IndexColorModel(8, srcPalette.length, r, g, b);

            byte[] pixels = new byte[width * height];
            int pitch = getPitch(width, srcBpp);
            for (int y=0;y<height;y++) {
                int address = src + pitch * (height-y-1); // Windows bitmaps are bottom up
                for (int x=0;x<width;x++) {
                    pixels[y*width+x] = (byte)Memory.mem_readb(address+x);
                }
            }
            try {
                DataBuffer dataBuffer = new DataBufferByte(pixels, width*height);
                SampleModel sampleModel = null;
                if (srcBpp == 8)
                    sampleModel = new SinglePixelPackedSampleModel(DataBuffer.TYPE_BYTE, width, height, new int[]{0xFF});
                else if (srcBpp == 4)
                    sampleModel = new SinglePixelPackedSampleModel(DataBuffer.TYPE_BYTE, width, height, new int[]{0xF});
                else
                    Win.panic("Currently only 24-bit, 16-bit, 8-bit and 4-bit bitmaps are supported");
                WritableRaster raster = Raster.createWritableRaster(sampleModel, dataBuffer, null);
                BufferedImage bi = new BufferedImage(sp, raster, false, null);
                //Main.drawImage(bi);
                //try {Thread.sleep(1000*60);} catch (Exception e) {}
                return bi;
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            if (srcBpp == 16) {
                short[] pixels = new short[width * height];
                int pitch = getPitch(width, srcBpp);
                for (int y=0;y<height;y++) {
                    int address = src + pitch * (height-y-1); // Windows bitmaps are bottom up
                    for (int x=0;x<width;x++) {
                        pixels[y*width+x] = (short)Memory.mem_readw(address + x * 2);
                    }
                }

                try {
                    DataBuffer dataBuffer = new DataBufferUShort(pixels, width*height);
                    WritableRaster raster = Raster.createPackedRaster(dataBuffer, width, height, width, new int[]{0xF800, 0x07E0, 0x001F}, null);
                    ColorModel colorModel = new DirectColorModel(16, 0xF800, 0x07E0, 0x001F);
                    BufferedImage bi = new BufferedImage(colorModel, raster, false, null);
                    //Main.drawImage(bi);try {Thread.sleep(1000*60);} catch (Exception e) {}
                    return bi;
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else if (srcBpp == 24) {
                Win.panic("24-bit bitmaps not implemented yet");
            } else {
                Win.panic("Currently only 24-bit, 16-bit, 8-bit and 4-bit bitmaps are supported");
            }
        }
        return null;
    }

    static public void writeImage(int dst, BufferedImage biDest, int dstBpp, int width, int height) {
        DataBuffer buffer = biDest.getData().getDataBuffer();
        int pitch = getPitch(width, dstBpp);

        switch (buffer.getDataType()) {
            case DataBuffer.TYPE_BYTE:
                DataBufferByte bb = (DataBufferByte)buffer;
                Win.panic("8-bit bitmap destination not implemented yet");
                break;
            case DataBuffer.TYPE_USHORT:
            {
                DataBufferUShort sb = (DataBufferUShort)buffer;
                short[] data = sb.getData();
                for (int y=0;y<height;y++) {
                    int address = dst+ pitch * (height-y-1); // Windows bitmaps are bottom up
                    for (int x=0;x<width;x++) {
                        Memory.mem_writew(address+x*2, data[y*width+x]);
                    }
                }
                break;
            }
            case DataBuffer.TYPE_INT:
                DataBufferInt ib = (DataBufferInt)buffer;
                Win.panic("24-bit bitmap destination not implemented yet");
                break;
        }
        // Debug what was written to the dst address
        // BufferedImage bi = createImage(dst, dstBpp, null, width, height);
        // Main.drawImage(bi);
        // try {Thread.sleep(1000*60);} catch (Exception e) {}
    }
    static public void copy(int src, int srcBpp, int[] srcPalette, int dst, int dstBpp, int[] dstPalette, int width, int height) {
        BufferedImage biSrc = createImage(src, srcBpp, srcPalette, width, height);
        BufferedImage biDest;
        switch (dstBpp) {
            case 8:
                biDest = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_INDEXED);
                break;
            case 15:
                biDest = new BufferedImage(width, height, BufferedImage.TYPE_USHORT_555_RGB);
                break;
            case 16:
                biDest = new BufferedImage(width, height, BufferedImage.TYPE_USHORT_565_RGB);
                break;
            case 24:
            case 32:
                biDest = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
                break;
            default:
                Win.panic("Cannot create "+dstBpp+"-bit destination bitmap");
                biDest = null;
        }

        Graphics graphics = biDest.getGraphics();
        graphics.drawImage(biSrc, 0, 0, width, height, null);
        writeImage(dst, biDest, dstBpp, width, height);
    }
}
