package jdos.win.builtin.directx.ddraw;

import jdos.hardware.Memory;

public class DDBltFx {
    public int size;                    // size of structure
    public int dwDDFX;                  // FX operations
    public int dwROP;                   // Win32 raster operations
    public int dwDDROP;                 // Raster operations new for DirectDraw
    public int dwRotationAngle;         // Rotation angle for blt
    public int dwZBufferOpCode;         // ZBuffer compares
    public int dwZBufferLow;            // Low limit of Z buffer
    public int dwZBufferHigh;           // High limit of Z buffer
    public int dwZBufferBaseDest;       // Destination base value
    public int dwZDestConstBitDepth;    // Bit depth used to specify Z constant for destination
    public int dwZDestConst;            // Constant to use as Z buffer for dest
                                        // Surface to use as Z buffer for dest
    public int dwZSrcConstBitDepth;     // Bit depth used to specify Z constant for source
    public int dwZSrcConst;             // Constant to use as Z buffer for src
                                        // Surface to use as Z buffer for src
    public int dwAlphaEdgeBlendBitDepth;// Bit depth used to specify constant for alpha edge blend
    public int dwAlphaEdgeBlend;        // Alpha for edge blending
    public int dwReserved;
    public int dwAlphaDestConstBitDepth;// Bit depth used to specify alpha constant for destination
    public int dwAlphaDestConst;        // Constant to use as Alpha Channel
                                        // Surface to use as Alpha Channel
    public int dwAlphaSrcConstBitDepth; // Bit depth used to specify alpha constant for source
    public int dwAlphaSrcConst;         // Constant to use as Alpha Channel
                                        // Surface to use as Alpha Channel
    public int dwFillColor;             // color in RGB or Palettized
                                        // depth value for z-buffer
                                        // pixel val for RGBA or RGBZ
                                        // Surface to use as pattern
    public DDColorKey ddckDestColorkey; // DestColorkey override
    public DDColorKey ddckSrcColorkey; // DestColorkey override

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
        ddckSrcColorkey = new DDColorKey(address); address+=DDColorKey.SIZE;
    }
}
