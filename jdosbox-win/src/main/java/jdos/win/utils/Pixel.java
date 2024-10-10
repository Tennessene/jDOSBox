package jdos.win.utils;

import jdos.hardware.Memory;
import jdos.win.Win;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.*;

public class Pixel {
    static public int getPitch(int width, int bpp) {
        if (bpp>=8)
            return ((width * ((bpp + 7) / 8) + 3) / 4) * 4;
        return (((width * bpp / 8) + 3) / 4) * 4;
    }

    private static BufferedImage imageToBufferedImage(Image image) {

        BufferedImage bufferedImage = new BufferedImage(image.getWidth(null), image.getHeight(null), BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = bufferedImage.createGraphics();
        g2.drawImage(image, 0, 0, null);
        g2.dispose();

        return bufferedImage;

    }

    public static BufferedImage makeColorTransparent(BufferedImage im, final Color color) {
        ImageFilter filter = new RGBImageFilter() {

            // the color we are looking for... Alpha bits are set to opaque
            public int markerRGB = color.getRGB() | 0xFF000000;

            public final int filterRGB(int x, int y, int rgb) {
                if ((rgb | 0xFF000000) == markerRGB) {
                    // Mark the alpha bits as zero - transparent
                    return 0x00FFFFFF & rgb;
                } else {
                    // nothing to do
                    return rgb;
                }
            }
        };

        ImageProducer ip = new FilteredImageSource(im.getSource(), filter);
        return imageToBufferedImage(Toolkit.getDefaultToolkit().createImage(ip));
    }

    static public BufferedImage flipVertically(BufferedImage image) {
        AffineTransform tx = AffineTransform.getScaleInstance(1, -1);
        tx.translate(0, -image.getHeight());
        AffineTransformOp op = new AffineTransformOp(tx, AffineTransformOp.TYPE_NEAREST_NEIGHBOR);
        return op.filter(image, null);
    }

    static public BufferedImage createImage(byte[] bits, int bpp, boolean alpha, int[] srcPalette, int width, int height, boolean flip) {
        IndexColorModel cm = null;
        byte[] pixels = new byte[width * height];
        int pitch = getPitch(width, bpp);
        BufferedImage image = null;
        if (srcPalette != null && bpp<=8) {
            byte[] r = new byte[srcPalette.length];
            byte[] g = new byte[srcPalette.length];
            byte[] b = new byte[srcPalette.length];
            byte[] a = new byte[srcPalette.length];

            for (int i=0;i<srcPalette.length;i++) {
                r[i] = (byte)srcPalette[i];
                g[i] = (byte)(srcPalette[i] >> 8);
                b[i] = (byte)(srcPalette[i] >> 16);
                a[i] = (byte)(srcPalette[i] >>> 24);
            }
            if (alpha)
                cm = new IndexColorModel(bpp, srcPalette.length, r, g, b, a);
            else
                cm = new IndexColorModel(bpp, srcPalette.length, r, g, b);

            if (width != pitch*8/bpp) {
                Win.panic("createImage unaligned image width not test");
            }
            DataBuffer dataBuffer = new DataBufferByte(bits, bits.length);
            WritableRaster raster = WritableRaster.createPackedRaster(dataBuffer, width, height, bpp, null);
            image = new BufferedImage(cm, raster, false, null);
        } else {
            Win.panic("createImage with byte buffer has not been implemented for "+bpp+"bpp");
        }
        if (flip)
            image = flipVertically(image);
        // Main.drawImage(image);try {Thread.sleep(1000*5);} catch (Exception e) {}
        return image;
    }

    static public BufferedImage createImage(int src, int srcBpp, int[] srcPalette, int width, int height, boolean flip) {
        if (srcPalette != null && srcBpp<=8) {
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
            if (src != 0) {
                int pitch = getPitch(width, srcBpp);
                for (int y=0;y<height;y++) {
                    int address = src + pitch * (flip?height - y -1:y);
                    for (int x=0;x<width;x++) {
                        pixels[y*width+x] = (byte)Memory.mem_readb(address+x);
                    }
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
                // Main.drawImage(bi);try {Thread.sleep(1000*5);} catch (Exception e) {}
                return bi;
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            if (srcBpp == 16) {
                short[] pixels = new short[width * height];
                int pitch = getPitch(width, srcBpp);
                for (int y=0;y<height;y++) {
                    int address = src + pitch * y;
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
                try {
                    BufferedImage bi = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
                    int pitch = getPitch(width, srcBpp);
                    for (int y=0;y<height;y++) {
                        int address = src + pitch * y;
                        for (int x=0;x<width;x++) {
                            bi.setRGB(x, y, Memory.mem_readb(address + x * 3)|(Memory.mem_readb(address + x * 3+1)<<8)|(Memory.mem_readb(address + x * 3+2)<<16));
                        }
                    }

                    // Main.drawImage(bi);try {Thread.sleep(1000*60);} catch (Exception e) {}
                    return bi;
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else if (srcBpp == 32) {
                try {
                    BufferedImage bi = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
                    int pitch = getPitch(width, srcBpp);
                    if (src != 0) {
                        for (int y=0;y<height;y++) {
                            int address = src + pitch * y;
                            for (int x=0;x<width;x++) {
                                bi.setRGB(x, y, Memory.mem_readd(address + x * 4));
                            }
                        }
                    }
                    // Main.drawImage(bi);try {Thread.sleep(1000*60);} catch (Exception e) {}
                    return bi;
                } catch (Exception e) {
                    e.printStackTrace();
                }
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
            {
                DataBufferByte bb = (DataBufferByte)buffer;
                byte[] data = bb.getData();
                for (int y=0;y<height;y++) {
                    int address = dst+ pitch * y;
                    for (int x=0;x<width;x++) {
                        Memory.mem_writeb(address + x, data[y * width + x]);
                    }
                }
                break;
            }
            case DataBuffer.TYPE_USHORT:
            {
                DataBufferUShort sb = (DataBufferUShort)buffer;
                short[] data = sb.getData();
                for (int y=0;y<height;y++) {
                    int address = dst+ pitch * y;
                    for (int x=0;x<width;x++) {
                        Memory.mem_writew(address+x*2, data[y*width+x]);
                    }
                }
                break;
            }
            case DataBuffer.TYPE_INT:
                DataBufferInt ib = (DataBufferInt)buffer;
                int[] data = ib.getData();
                if (dstBpp==24) {
                    for (int y=0;y<height;y++) {
                        int address = dst+ pitch * y;
                        for (int x=0;x<width;x++) {
                            Memory.mem_writeb(address+x*3, data[y*width+x]);
                            Memory.mem_writeb(address+x*3+1, data[y*width+x] >> 8);
                            Memory.mem_writeb(address+x*3+2, data[y*width+x] >> 16);
                        }
                    }
                } else if (dstBpp==32) {
                    for (int y=0;y<height;y++) {
                        int address = dst+ pitch * y;
                        for (int x=0;x<width;x++) {
                            Memory.mem_writed(address+x*4, data[y*width+x]);
                        }
                    }
                }
                break;
        }
        // Debug what was written to the dst address
        // BufferedImage bi = createImage(dst, dstBpp, null, width, height);
        // Main.drawImage(biDest);try {Thread.sleep(1000*60);} catch (Exception e) {}
    }
    static public void copy(int src, int srcBpp, int[] srcPalette, int dst, int dstBpp, int[] dstPalette, int width, int height, boolean flip) {
        BufferedImage biSrc = createImage(src, srcBpp, srcPalette, width, height, flip);
        BufferedImage biDest;
        switch (dstBpp) {
            case 8:
                biDest = createImage(dst, dstBpp, dstPalette, width, height, false);
                break;
            case 15:
                biDest = new BufferedImage(width, height, BufferedImage.TYPE_USHORT_555_RGB);
                break;
            case 16:
                biDest = new BufferedImage(width, height, BufferedImage.TYPE_USHORT_565_RGB);
                break;
            case 24:
                biDest = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
                break;
            case 32:
                biDest = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
                break;
            default:
                Win.panic("Cannot create "+dstBpp+"-bit destination bitmap");
                biDest = null;
        }

        Graphics graphics = biDest.getGraphics();
        graphics.drawImage(biSrc, 0, 0, width, height, null);
        writeImage(dst, biDest, dstBpp, width, height);
        graphics.dispose();
    }

    static public int BGRtoRGB(int color) {
        return (color & 0xFF000000) | (color>>>16 & 0xFF) | ((color << 16) & 0xFF0000) | (color & 0x0000FF00);
    }

    private static int findNearestColor(int color, int[] palette) {
        int minDistanceSquared = 255*255 + 255*255 + 255*255 + 1;
        int bestIndex = 0;
        int r = (color >> 16) & 0xFF;
        int g = (color >> 8) & 0xFF;
        int b = color & 0xFF;
        for (int i = 0; i < palette.length; i++) {
            int Rdiff = r - ((palette[i] >> 16) & 0xff);
            int Gdiff = g - ((palette[i] >> 8) & 0xff);
            int Bdiff = b - (palette[i] & 0xff);
            int distanceSquared = Rdiff*Rdiff + Gdiff*Gdiff + Bdiff*Bdiff;
            if (distanceSquared < minDistanceSquared) {
                minDistanceSquared = distanceSquared;
                bestIndex = i;
            }
        }
        return bestIndex;
    }

    static public void blt8(int dst, int dstX, int dstY, int dstPitch, int src, int srcX, int srcY, int srcPitch, int width, int height) {
        for (int y=0;y<height;y++) {
            Memory.mem_memcpy(dst+dstPitch*(dstY+y)+dstX, src+srcPitch*(srcY+y)+srcX, width);
        }
    }

    static public void fill(int bpp, int dst, int dstX, int dstY, int dstPitch, int width, int height, int color) {

    }

    static public void copy2(int src, int srcBpp, int[] srcPalette, int dst, int dstBpp, int[] dstPalette, int width, int height) {
        for (int i=0;i<srcPalette.length;i++) {
            System.out.println(Integer.toHexString(srcPalette[i]) + " "+Integer.toHexString(dstPalette[i]));
        }
        for (int i=0;i<srcPalette.length;i++) {
            System.out.println(Integer.toHexString(srcPalette[i]) + " "+Integer.toHexString(dstPalette[srcPalette.length-i-1]));
        }
        // This will prevent dithering
        if (dstBpp == 8 && (srcBpp == 8 || srcBpp == 32)) {
            int dstPitch = getPitch(width, dstBpp);
            int srcPitch = getPitch(width, srcBpp);

            for (int y=0;y<height;y++) {
                int s = src + y*srcPitch;
                int d = dst + y*dstPitch;
                for (int x=0;x<width;x++) {
                    int srcColor;

                    int dstIndex = Memory.mem_readb(s);s++;
                    Memory.mem_writeb(d, dstIndex); d++;

//                    if (srcBpp == 8) {
//                        srcColor = srcPalette[Memory.mem_readb(s)];s++;
//                    } else {
//                        srcColor = Memory.mem_readd(s);s+=4;
//                    }
//                    int dstIndex = findNearestColor(srcColor, dstPalette);
//                    Memory.mem_writeb(d, dstIndex); d++;
                }
            }
        } else {
            copy(src, srcBpp, srcPalette, dst, dstBpp, dstPalette, width, height, false);
        }
    }
}
