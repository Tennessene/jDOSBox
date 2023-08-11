package jdos.hardware.qemu;

public class PixelOps {
    static public int rgb_to_pixel8(int r, int g, int b)
    {
        return ((r >> 5) << 5) | ((g >> 5) << 2) | (b >> 6);
    }
    
    static public int rgb_to_pixel15(int r, int g, int b)
    {
        return ((r >> 3) << 10) | ((g >> 3) << 5) | (b >> 3);
    }
    
    static public int rgb_to_pixel15bgr(int r, int g, int b)
    {
        return ((b >> 3) << 10) | ((g >> 3) << 5) | (r >> 3);
    }
    
    static public int rgb_to_pixel16(int r, int g, int b)
    {
        return ((r >> 3) << 11) | ((g >> 2) << 5) | (b >> 3);
    }
    
    static public int rgb_to_pixel16bgr(int r, int g, int b)
    {
        return ((b >> 3) << 11) | ((g >> 2) << 5) | (r >> 3);
    }
    
    static public int rgb_to_pixel24(int r, int g, int b)
    {
        return (r << 16) | (g << 8) | b;
    }
    
    static public int rgb_to_pixel24bgr(int r, int g, int b)
    {
        return (b << 16) | (g << 8) | r;
    }
    
    static public int rgb_to_pixel32(int r, int g, int b)
    {
        return (r << 16) | (g << 8) | b;
    }
    
    static public int rgb_to_pixel32bgr(int r, int g, int b)
    {
        return (b << 16) | (g << 8) | r;
    }
}
