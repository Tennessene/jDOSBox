package jdos.win.builtin.directx.ddraw;

import jdos.hardware.Memory;

public class DDBltFx {
    public final int size;                    // size of structure
    public final int dwDDFX;                  // FX operations
    public final int dwROP;                   // Win32 raster operations
    public final int dwDDROP;                 // Raster operations new for DirectDraw
    public final int dwRotationAngle;         // Rotation angle for blt
    public final int dwZBufferOpCode;         // ZBuffer compares
    public final int dwZBufferLow;            // Low limit of Z buffer
    public final int dwZBufferHigh;           // High limit of Z buffer
    public final int dwZBufferBaseDest;       // Destination base value
    public final int dwZDestConstBitDepth;    // Bit depth used to specify Z constant for destination
    public final int dwZDestConst;            // Constant to use as Z buffer for dest
                                        // Surface to use as Z buffer for dest
    public final int dwZSrcConstBitDepth;     // Bit depth used to specify Z constant for source
    public final int dwZSrcConst;             // Constant to use as Z buffer for src
                                        // Surface to use as Z buffer for src
    public final int dwAlphaEdgeBlendBitDepth;// Bit depth used to specify constant for alpha edge blend
    public final int dwAlphaEdgeBlend;        // Alpha for edge blending
    public final int dwReserved;
    public final int dwAlphaDestConstBitDepth;// Bit depth used to specify alpha constant for destination
    public final int dwAlphaDestConst;        // Constant to use as Alpha Channel
                                        // Surface to use as Alpha Channel
    public final int dwAlphaSrcConstBitDepth; // Bit depth used to specify alpha constant for source
    public final int dwAlphaSrcConst;         // Constant to use as Alpha Channel
                                        // Surface to use as Alpha Channel
    public final int dwFillColor;             // color in RGB or Palettized
                                        // depth value for z-buffer
                                        // pixel val for RGBA or RGBZ
                                        // Surface to use as pattern
    public final DDColorKey ddckDestColorkey; // DestColorkey override
    public final DDColorKey ddckSrcColorkey; // DestColorkey override

    public DDBltFx(int address) {
        size = Memory.mem_readd(address); address+=4;
        dwDDFX = Memory.mem_readd(address); address+=4;
        dwROP = Memory.mem_readd(address); address+=4;
        dwDDROP = Memory.mem_readd(address); address+=4;
        dwRotationAngle = Memory.mem_readd(address); address+=4;
        dwZBufferOpCode = Memory.mem_readd(address); address+=4;
        dwZBufferLow = Memory.mem_readd(address); address+=4;
        dwZBufferHigh = Memory.mem_readd(address); address+=4;
        dwZBufferBaseDest = Memory.mem_readd(address); address+=4;
        dwZDestConstBitDepth = Memory.mem_readd(address); address+=4;
        dwZDestConst = Memory.mem_readd(address); address+=4;
        dwZSrcConstBitDepth = Memory.mem_readd(address); address+=4;
        dwZSrcConst = Memory.mem_readd(address); address+=4;
        dwAlphaEdgeBlendBitDepth = Memory.mem_readd(address); address+=4;
        dwAlphaEdgeBlend = Memory.mem_readd(address); address+=4;
        dwReserved = Memory.mem_readd(address); address+=4;
        dwAlphaDestConstBitDepth = Memory.mem_readd(address); address+=4;
        dwAlphaDestConst = Memory.mem_readd(address); address+=4;
        dwAlphaSrcConstBitDepth = Memory.mem_readd(address); address+=4;
        dwAlphaSrcConst = Memory.mem_readd(address); address+=4;
        dwFillColor = Memory.mem_readd(address); address+=4;
        ddckDestColorkey = new DDColorKey(address); address+=DDColorKey.SIZE;
        ddckSrcColorkey = new DDColorKey(address);
    }
}
