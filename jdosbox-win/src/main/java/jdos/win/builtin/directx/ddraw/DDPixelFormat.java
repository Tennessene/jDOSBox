package jdos.win.builtin.directx.ddraw;

import jdos.hardware.Memory;

public class DDPixelFormat {
    static public final int DDPF_ALPHAPIXELS =      0x00000001; // The surface has alpha channel information in the pixel format.
    static public final int DDPF_ALPHA =            0x00000002; // The pixel format describes an alpha-only surface.
    static public final int DDPF_FOURCC =           0x00000004; // The FOURCC code is valid.
    static public final int DDPF_PALETTEINDEXED4 =  0x00000008; // The surface is 4-bit color indexed.
    static public final int DDPF_PALETTEINDEXEDTO8 =0x00000010; // The surface is 1-, 2-, or 4-bit color indexed to an 8-bit palette.
    static public final int DDPF_PALETTEINDEXED8 =  0x00000020; // The surface is 8-bit color indexed.
    static public final int DDPF_RGB =              0x00000040; // The RGB data in the pixel format structure is valid.
    static public final int DDPF_COMPRESSED =       0x00000080; // The surface accepts pixel data in the specified format and compresses it during the write operation.
    static public final int DDPF_RGBTOYUV =         0x00000100; // The surface accepts RGB data and translates it during the write operation to YUV data. The format of the data to be written is contained in the pixel format structure. The DDPF_RGB flag is set.
    static public final int DDPF_YUV =              0x00000200; // 	The YUV data in the pixel format structure is valid.
    static public final int DDPF_ZBUFFER =          0x00000400; // The pixel format describes a z-buffer-only surface.
    static public final int DDPF_PALETTEINDEXED1 =  0x00000800; // The surface is 1-bit color indexed.
    static public final int DDPF_PALETTEINDEXED2 =  0x00001000; // The surface is 2-bit color indexed.
    static public final int DDPF_ZPIXELS =          0x00002000; // The surface is in RGBZ format.
    static public final int DDPF_STENCILBUFFER =    0x00004000; // The surface encodes stencil and depth information in each pixel of the z-buffer.
    static public final int DDPF_ALPHAPREMULT =     0x00008000; // The color components in the pixel are premultiplied by the alpha value in the pixel. If this flag is set, the DDPF_ALPHAPIXELS flag must also be set. If this flag is not set but the DDPF_ALPHAPIXELS flag is set, the color components in the pixel format are not premultiplied by alpha. In this case, the color components must be multiplied by the alpha value at the time that an alpha-blending operation is performed.
    static public final int DDPF_LUMINANCE =        0x00020000; // Luminance data in the pixel format is valid. Use this flag for luminance-only or luminance-plus-alpha surfaces; the bit depth is then specified in the dwLuminanceBitCount member.
    static public final int DDPF_BUMPLUMINANCE =    0x00040000;
    static public final int DDPF_BUMPDUDV =         0x00080000; // Bump map dUdV data in the pixel format is valid.

    public static final int SIZE = 0x20;
    public int dwSize;                  /* 0: size of structure */
    public int dwFlags;                 /* 4: pixel format flags */
    public int dwFourCC;                /* 8: (FOURCC code) */
    // union
    public int dwRGBBitCount;           /* C: how many bits per pixel or dwYUVBitCount or dwZBufferBitDepth or dwAlphaBitDepth or dwLuminanceBitCount or dwBumpBitCount*/
    // union
    public int dwRBitMask;              /* 10: mask for red bit or or dwYBitMask or dwStencilBitDepth or dwLuminanceBitMask or dwBumpDuBitMask */
    // union
	public int dwGBitMask;              /* 14: mask for green bits or dwUBitMask or dwZBitMask or dwBumpDvBitMask */
    // union
    public int dwBBitMask;              /* 18: mask for blue bits or dwVBitMask or dwStencilBitMask or dwBumpLuminanceBitMask*/
    // union
    public int dwRGBAlphaBitMask;	    /* 1C: mask for alpha channel or dwYUVAlphaBitMask or dwLuminanceAlphaBitMask or dwRGBZBitMask or dwYUVZBitMask*/

    public DDPixelFormat(int address) {
        dwSize = Memory.mem_readd(address);
        dwFlags = Memory.mem_readd(address+4);
        dwFourCC = Memory.mem_readd(address+8);
        dwRGBBitCount = Memory.mem_readd(address+0x0C);
        dwRBitMask = Memory.mem_readd(address+0x10);
        dwGBitMask = Memory.mem_readd(address+0x14);
        dwBBitMask = Memory.mem_readd(address+0x18);
        dwRGBAlphaBitMask = Memory.mem_readd(address+0x1C);
    }
}
