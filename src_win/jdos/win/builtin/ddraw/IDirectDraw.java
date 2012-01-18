package jdos.win.builtin.ddraw;

import jdos.cpu.CPU;
import jdos.cpu.CPU_Regs;
import jdos.cpu.Callback;
import jdos.gui.Main;
import jdos.hardware.Memory;
import jdos.win.Console;
import jdos.win.builtin.HandlerBase;
import jdos.win.loader.Module;
import jdos.win.system.WinSystem;
import jdos.win.utils.Error;

public class IDirectDraw extends IUnknown {
    static final int DDSCL_FULLSCREEN = 0x00000001;
    static final int DDSCL_ALLOWREBOOT = 0x00000002;
    static final int  DDSCL_EXCLUSIVE = 0x00000010;
    static final int DDSCL_ALLOWMODEX = 0x00000040;

    static final int VTABLE_SIZE = 20;

    private static int createVTable() {
        int address = allocateVTable("IDirectDraw", VTABLE_SIZE);
        addIDirectDraw(address);
        return address;
    }

    static int addIDirectDraw(int address) {
        address = addIUnknown(address);
        address = add(address, Compact);
        address = add(address, CreateClipper);
        address = add(address, CreatePalette);
        address = add(address, CreateSurface);
        address = add(address, DuplicateSurface);
        address = add(address, EnumDisplayModes);
        address = add(address, EnumSurfaces);
        address = add(address, FlipToGDISurface);
        address = add(address, GetCaps);
        address = add(address, GetDisplayMode);
        address = add(address, GetFourCCCodes);
        address = add(address, GetGDISurface);
        address = add(address, GetMonitorFrequency);
        address = add(address, GetScanLine);
        address = add(address, GetVerticalBlankStatus);
        address = add(address, Initialize);
        address = add(address, RestoreDisplayMode);
        address = add(address, SetCooperativeLevel);
        address = add(address, SetDisplayMode);
        address = add(address, WaitForVerticalBlank);
        return address;
    }

    static int FLAGS_CALLBACK2 = 0x00000001;
    static int FLAGS_DESC2 = 0x00000002;
    static int FLAGS_V7 = 0x00000004;

    static int OFFSET_FLAGS = 0;

    static final int OFFSET_CX = 4;
    static final int OFFSET_CY = 8;
    static final int OFFSET_BPP = 12;
    static final int OFFSET_PALETTE = 16;

    static final int DATA_SIZE = 20;

    public static int getWidth(int This) {
        return getData(This, OFFSET_CX);
    }
    public static int getHeight(int This) {
        return getData(This, OFFSET_CY);
    }
    public static int getBPP(int This) {
        return getData(This, OFFSET_BPP);
    }

    public static void setPalette(int This, int palette) {
        setData(This, OFFSET_PALETTE, palette);
    }

    public static int getPalette(int This) {
        int result = getData(This, OFFSET_PALETTE);
        if (result == 0) {
            result = IDirectDrawPalette.createDefault();
            setData(This, OFFSET_PALETTE, result);
        }
        return result;
    }

    public static int create() {
        return create("IDirectDraw", 0);
    }

    public static int create(String name, int flags) {
        int vtable = getVTable(name);
        if (vtable == 0)
            vtable = createVTable();
        int address = allocate(vtable, DATA_SIZE, 0);
        setData(address, OFFSET_FLAGS, flags);
        return address;
    }

    // HRESULT Compact(this)
    static private Callback.Handler Compact = new HandlerBase() {
        public java.lang.String getName() {
            return "IDirectDraw.Compact";
        }
        public void onCall() {
            int This = CPU.CPU_Pop32();
            notImplemented();
        }
    };

    // HRESULT CreateClipper(this, DWORD dwFlags, LPDIRECTDRAWCLIPPER *lplpDDClipper, IUnknown *pUnkOuter)
    static private Callback.Handler CreateClipper = new HandlerBase() {
        public java.lang.String getName() {
            return "IDirectDraw.CreateClipper";
        }
        public void onCall() {
            int This = CPU.CPU_Pop32();
            int dwFlags = CPU.CPU_Pop32();
            int lplpDDClipper = CPU.CPU_Pop32();
            int pUnkOuter = CPU.CPU_Pop32();
            if (lplpDDClipper == 0)
                CPU_Regs.reg_eax.dword = Error.E_POINTER;
            else {
                Memory.mem_writed(lplpDDClipper, IDirectDrawClipper.create());
                CPU_Regs.reg_eax.dword = Error.S_OK;
            }
        }
    };

    // HRESULT CreatePalette(this, DWORD dwFlags, LPPALETTEENTRY lpColorTable, LPDIRECTDRAWPALETTE *lplpDDPalette, IUnknown *pUnkOuter)
    static private Callback.Handler CreatePalette = new HandlerBase() {
        public java.lang.String getName() {
            return "IDirectDraw.CreatePalette";
        }
        public void onCall() {
            int This = CPU.CPU_Pop32();
            int dwFlags = CPU.CPU_Pop32();
            int lpColorTable = CPU.CPU_Pop32();
            int lplpDDPalette = CPU.CPU_Pop32();
            int pUnkOuter = CPU.CPU_Pop32();
            if (lplpDDPalette == 0)
                CPU_Regs.reg_eax.dword = Error.E_POINTER;
            else {
                Memory.mem_writed(lplpDDPalette, IDirectDrawPalette.create(dwFlags, lpColorTable));
                CPU_Regs.reg_eax.dword = Error.S_OK;
            }
        }
    };

    // HRESULT CreateSurface(this, LPDDSURFACEDESC lpDDSurfaceDesc, LPDIRECTDRAWSURFACE *lplpDDSurface, IUnknown *pUnkOuter)
    static private Callback.Handler CreateSurface = new HandlerBase() {
        public java.lang.String getName() {
            return "IDirectDraw.CreateSurface";
        }
        public void onCall() {
            int This = CPU.CPU_Pop32();
            int lpDDSurfaceDesc = CPU.CPU_Pop32();
            int lplpDDSurface = CPU.CPU_Pop32();
            int pUnkOuter = CPU.CPU_Pop32();
            if (lplpDDSurface == 0)
                CPU_Regs.reg_eax.dword = Error.E_POINTER;
            else {
                int result;
                if ((getData(This, OFFSET_FLAGS) & FLAGS_V7)!=0) {
                    result = IDirectDrawSurface7.create(This, lpDDSurfaceDesc);
                } else {
                    result = IDirectDrawSurface.create(This, lpDDSurfaceDesc);
                }
                Memory.mem_writed(lplpDDSurface, result);
                CPU_Regs.reg_eax.dword = Error.S_OK;
            }
        }
    };

    // HRESULT DuplicateSurface(this, LPDIRECTDRAWSURFACE lpDDSurface, LPDIRECTDRAWSURFACE *lplpDupDDSurface)
    static private Callback.Handler DuplicateSurface = new HandlerBase() {
        public java.lang.String getName() {
            return "IDirectDraw.DuplicateSurface";
        }
        public void onCall() {
            int This = CPU.CPU_Pop32();
            int lpDDSurface = CPU.CPU_Pop32();
            int lplpDupDDSurface = CPU.CPU_Pop32();
            notImplemented();
        }
    };

    // HRESULT EnumDisplayModes(this, DWORD dwFlags, LPDDSURFACEDESC lpDDSurfaceDesc, LPVOID lpContext, LPDDENUMMODESCALLBACK lpEnumModesCallback)
    static private Callback.Handler EnumDisplayModes = new HandlerBase() {
        private int[][] mode = new int[][] {
                {320, 240, 8},
                {320, 240, 16},
                {320, 240, 32},
                {400, 300, 8},
                {400, 300, 16},
                {400, 300, 32},
                {512, 384, 8},
                {512, 384, 16},
                {512, 384, 32},
                {640, 480, 8},
                {640, 480, 16},
                {640, 480, 32},
                {800, 600, 8},
                {800, 600, 16},
                {800, 600, 32},
                {1024, 768, 8},
                {1024, 768, 16},
                {1024, 768, 32},
        };
        public java.lang.String getName() {
            return "IDirectDraw.EnumDisplayModes";
        }
        public void onCall() {
            int This = CPU.CPU_Pop32();
            int dwFlags = CPU.CPU_Pop32();
            int lpDDSurfaceDesc = CPU.CPU_Pop32();
            int lpContext = CPU.CPU_Pop32();
            int lpEnumModesCallback = CPU.CPU_Pop32();
            int address = WinSystem.getCurrentProcess().heap.alloc(DDSurfaceDesc.SIZE, false);
            DDSurfaceDesc desc = null;
            if (lpDDSurfaceDesc != 0)
                desc = new DDSurfaceDesc(lpDDSurfaceDesc, false);
            Memory.mem_zero(address, DDSurfaceDesc.SIZE);
            Memory.mem_writed(address, DDSurfaceDesc.SIZE);
            Memory.mem_writed(address+4, DDSurfaceDesc.DDSD_HEIGHT|DDSurfaceDesc.DDSD_WIDTH|DDSurfaceDesc.DDSD_PITCH|DDSurfaceDesc.DDSD_PIXELFORMAT);
            Memory.mem_writed(address+0x48, DDPixelFormat.SIZE);
            for (int i=0;i<mode.length;i++) {
                if (desc != null) {
                    if ((desc.dwFlags & DDSurfaceDesc.DDSD_WIDTH)!=0 && desc.dwWidth!=mode[i][0])
                        continue;
                    if ((desc.dwFlags & DDSurfaceDesc.DDSD_HEIGHT)!=0 && desc.dwHeight!=mode[i][1])
                        continue;
                    if ((desc.dwFlags & DDSurfaceDesc.DDSD_PIXELFORMAT)!=0 && desc.ddpfPixelFormat.dwRGBBitCount!=mode[i][2])
                        continue;
                }
                Memory.mem_writed(address+8, mode[i][1]);
                Memory.mem_writed(address+12, mode[i][0]);
                Memory.mem_writed(address+16, mode[i][1]*(mode[i][1]>>3));
                if (mode[i][2]>8)
                    Memory.mem_writed(address+0x48+4, DDPixelFormat.DDPF_RGB);
                else
                    Memory.mem_writed(address+0x48+4, DDPixelFormat.DDPF_RGB|DDPixelFormat.DDPF_PALETTEINDEXED8);
                Memory.mem_writed(address+0x48+0xC, mode[i][2]);
                // :TODO: what about pixel formats?
                WinSystem.call(lpEnumModesCallback, address, lpContext);
            }
            CPU_Regs.reg_eax.dword = Error.S_OK;
        }
    };

    // HRESULT EnumSurfaces(this, DWORD dwFlags, LPDDSURFACEDESC lpDDSD, LPVOID lpContext, LPDDENUMSURFACESCALLBACK lpEnumSurfacesCallback)
    static private Callback.Handler EnumSurfaces = new HandlerBase() {
        public java.lang.String getName() {
            return "IDirectDraw.EnumSurfaces";
        }
        public void onCall() {
            int This = CPU.CPU_Pop32();
            int dwFlags = CPU.CPU_Pop32();
            int lpDDSD = CPU.CPU_Pop32();
            int lpContext = CPU.CPU_Pop32();
            int lpEnumSurfacesCallback = CPU.CPU_Pop32();
            notImplemented();
        }
    };

    // HRESULT FlipToGDISurface(this)
    static private Callback.Handler FlipToGDISurface = new HandlerBase() {
        public java.lang.String getName() {
            return "IDirectDraw.FlipToGDISurface";
        }
        public void onCall() {
            int This = CPU.CPU_Pop32();
            notImplemented();
        }
    };

    // HRESULT GetCaps(this, LPDDCAPS lpDDDriverCaps, LPDDCAPS lpDDHELCaps)
    static private Callback.Handler GetCaps = new HandlerBase() {
        static private final int DDCKEYCAPS_DESTBLT =               0x00000001;
        static private final int DDCKEYCAPS_DESTBLTCLRSPACE =       0x00000002;
        static private final int DDCKEYCAPS_DESTBLTCLRSPACEYUV =    0x00000004;
        static private final int DDCKEYCAPS_DESTBLTYUV =            0x00000008;
        static private final int DDCKEYCAPS_DESTOVERLAY =           0x00000010;
        static private final int DDCKEYCAPS_DESTOVERLAYCLRSPACE =   0x00000020;
        static private final int DDCKEYCAPS_DESTOVERLAYCLRSPACEYUV =0x00000040;
        static private final int DDCKEYCAPS_DESTOVERLAYONEACTIVE =  0x00000080;
        static private final int DDCKEYCAPS_DESTOVERLAYYUV =        0x00000100;
        static private final int DDCKEYCAPS_SRCBLT =                0x00000200;
        static private final int DDCKEYCAPS_SRCBLTCLRSPACE =        0x00000400;
        static private final int DDCKEYCAPS_SRCBLTCLRSPACEYUV =     0x00000800;
        static private final int DDCKEYCAPS_SRCBLTYUV =             0x00001000;
        static private final int DDCKEYCAPS_SRCOVERLAY =            0x00002000;
        static private final int DDCKEYCAPS_SRCOVERLAYCLRSPACE =    0x00004000;
        static private final int DDCKEYCAPS_SRCOVERLAYCLRSPACEYUV = 0x00008000;
        static private final int DDCKEYCAPS_SRCOVERLAYONEACTIVE =   0x00010000;
        static private final int DDCKEYCAPS_SRCOVERLAYYUV =         0x00020000;
        static private final int DDCKEYCAPS_NOCOSTOVERLAY =         0x00040000;
        
        static private final int DDPCAPS_4BIT =                     0x00000001;
        static private final int DDPCAPS_8BITENTRIES =              0x00000002;
        static private final int DDPCAPS_8BIT =                     0x00000004;
        static private final int DDPCAPS_INITIALIZE =               0x00000008;
        static private final int DDPCAPS_PRIMARYSURFACE =           0x00000010;
        static private final int DDPCAPS_PRIMARYSURFACELEFT =       0x00000020;
        static private final int DDPCAPS_ALLOW256 =                 0x00000040;
        static private final int DDPCAPS_VSYNC =                    0x00000080;
        static private final int DDPCAPS_1BIT =                     0x00000100;
        static private final int DDPCAPS_2BIT =                     0x00000200;
        static private final int DDPCAPS_ALPHA =                    0x00000400;

//        /*  0*/ DWORD	dwSize;			// size of the DDDRIVERCAPS structure
//        /*  4*/ DWORD	dwCaps;			// driver specific capabilities
//        /*  8*/ DWORD	dwCaps2;		// more driver specific capabilites
//        /*  c*/ DWORD	dwCKeyCaps;		// color key capabilities of the surface
//        /* 10*/ DWORD	dwFXCaps;		// driver specific stretching and effects capabilites
//        /* 14*/ DWORD	dwFXAlphaCaps;		// alpha driver specific capabilities
//        /* 18*/ DWORD	dwPalCaps;		// palette capabilities
//        /* 1c*/ DWORD	dwSVCaps;		// stereo vision capabilities
//        /* 20*/ DWORD	dwAlphaBltConstBitDepths;	// DDBD_2,4,8
//        /* 24*/ DWORD	dwAlphaBltPixelBitDepths;	// DDBD_1,2,4,8
//        /* 28*/ DWORD	dwAlphaBltSurfaceBitDepths;	// DDBD_1,2,4,8
//        /* 2c*/ DWORD	dwAlphaOverlayConstBitDepths;	// DDBD_2,4,8
//        /* 30*/ DWORD	dwAlphaOverlayPixelBitDepths;	// DDBD_1,2,4,8
//        /* 34*/ DWORD	dwAlphaOverlaySurfaceBitDepths; // DDBD_1,2,4,8
//        /* 38*/ DWORD	dwZBufferBitDepths;		// DDBD_8,16,24,32
//        /* 3c*/ DWORD	dwVidMemTotal;		// total amount of video memory
//        /* 40*/ DWORD	dwVidMemFree;		// amount of free video memory
//        /* 44*/ DWORD	dwMaxVisibleOverlays;	// maximum number of visible overlays
//        /* 48*/ DWORD	dwCurrVisibleOverlays;	// current number of visible overlays
//        /* 4c*/ DWORD	dwNumFourCCCodes;	// number of four cc codes
//        /* 50*/ DWORD	dwAlignBoundarySrc;	// source rectangle alignment
//        /* 54*/ DWORD	dwAlignSizeSrc;		// source rectangle byte size
//        /* 58*/ DWORD	dwAlignBoundaryDest;	// dest rectangle alignment
//        /* 5c*/ DWORD	dwAlignSizeDest;	// dest rectangle byte size
//        /* 60*/ DWORD	dwAlignStrideAlign;	// stride alignment
//        /* 64*/ DWORD	dwRops[DD_ROP_SPACE];	// ROPS supported
//        /* 84*/ DDSCAPS	ddsCaps;		// DDSCAPS structure has all the general capabilities
//        /* 88*/ DWORD	dwMinOverlayStretch;	// minimum overlay stretch factor multiplied by 1000, eg 1000 == 1.0, 1300 == 1.3
//        /* 8c*/ DWORD	dwMaxOverlayStretch;	// maximum overlay stretch factor multiplied by 1000, eg 1000 == 1.0, 1300 == 1.3
//        /* 90*/ DWORD	dwMinLiveVideoStretch;	// minimum live video stretch factor multiplied by 1000, eg 1000 == 1.0, 1300 == 1.3
//        /* 94*/ DWORD	dwMaxLiveVideoStretch;	// maximum live video stretch factor multiplied by 1000, eg 1000 == 1.0, 1300 == 1.3
//        /* 98*/ DWORD	dwMinHwCodecStretch;	// minimum hardware codec stretch factor multiplied by 1000, eg 1000 == 1.0, 1300 == 1.3
//        /* 9c*/ DWORD	dwMaxHwCodecStretch;	// maximum hardware codec stretch factor multiplied by 1000, eg 1000 == 1.0, 1300 == 1.3
//        /* a0*/ DWORD	dwReserved1;		// reserved
//        /* a4*/ DWORD	dwReserved2;		// reserved
//        /* a8*/ DWORD	dwReserved3;		// reserved
//        /* ac*/ DWORD	dwSVBCaps;		// driver specific capabilities for System->Vmem blts
//        /* b0*/ DWORD	dwSVBCKeyCaps;		// driver color key capabilities for System->Vmem blts
//        /* b4*/ DWORD	dwSVBFXCaps;		// driver FX capabilities for System->Vmem blts
//        /* b8*/ DWORD	dwSVBRops[DD_ROP_SPACE];// ROPS supported for System->Vmem blts
//        /* d8*/ DWORD	dwVSBCaps;		// driver specific capabilities for Vmem->System blts
//        /* dc*/ DWORD	dwVSBCKeyCaps;		// driver color key capabilities for Vmem->System blts
//        /* e0*/ DWORD	dwVSBFXCaps;		// driver FX capabilities for Vmem->System blts
//        /* e4*/ DWORD	dwVSBRops[DD_ROP_SPACE];// ROPS supported for Vmem->System blts
//        /*104*/ DWORD	dwSSBCaps;		// driver specific capabilities for System->System blts
//        /*108*/ DWORD	dwSSBCKeyCaps;		// driver color key capabilities for System->System blts
//        /*10c*/ DWORD	dwSSBFXCaps;		// driver FX capabilities for System->System blts
//        /*110*/ DWORD	dwSSBRops[DD_ROP_SPACE];// ROPS supported for System->System blts
//        #if       DIRECTDRAW_VERSION >= 0x0500
//        /*130*/ DWORD	dwMaxVideoPorts;	// maximum number of usable video ports
//        /*134*/ DWORD	dwCurrVideoPorts;	// current number of video ports used
//        /*138*/ DWORD	dwSVBCaps2;		// more driver specific capabilities for System->Vmem blts
//        /*13c*/ DWORD	dwNLVBCaps;		  // driver specific capabilities for non-local->local vidmem blts
//        /*140*/ DWORD	dwNLVBCaps2;		  // more driver specific capabilities non-local->local vidmem blts
//        /*144*/ DWORD	dwNLVBCKeyCaps;		  // driver color key capabilities for non-local->local vidmem blts
//        /*148*/ DWORD	dwNLVBFXCaps;		  // driver FX capabilities for non-local->local blts
//        /*14c*/ DWORD	dwNLVBRops[DD_ROP_SPACE]; // ROPS supported for non-local->local blts
//        #else  /* DIRECTDRAW_VERSION >= 0x0500 */
//        /*130*/ DWORD	dwReserved4;		// reserved
//        /*134*/ DWORD	dwReserved5;		// reserved
//        /*138*/ DWORD	dwReserved6;		// reserved
//        #endif /* DIRECTDRAW_VERSION >= 0x0500 */

        public java.lang.String getName() {
            return "IDirectDraw.GetCaps";
        }
        public void onCall() {
            int This = CPU.CPU_Pop32();
            int lpDDDriverCaps = CPU.CPU_Pop32();
            int lpDDHELCaps = CPU.CPU_Pop32();
            int bpp = 0x00000D00; // 8 16 and 32
            int size = Memory.mem_readd(lpDDDriverCaps);lpDDDriverCaps+=4;
            Memory.mem_zero(lpDDDriverCaps+4, size-4);
            Memory.mem_writed(lpDDDriverCaps, 0xFFFFFFFF);lpDDDriverCaps+=4;//   4 dwCaps
            Memory.mem_writed(lpDDDriverCaps, 0xFFFFFFFF);lpDDDriverCaps+=4;//   8 dwCaps2
            Memory.mem_writed(lpDDDriverCaps, 0xFFFFFFFF);lpDDDriverCaps+=4;//   C dwCKeyCaps
            Memory.mem_writed(lpDDDriverCaps, 0xFFFFFFFF);lpDDDriverCaps+=4;//  10 dwFXCaps
            Memory.mem_writed(lpDDDriverCaps, 0xFFFFFFFF);lpDDDriverCaps+=4;//  14 dwFXAlphaCaps
            Memory.mem_writed(lpDDDriverCaps, 0xFFFFFFFF);lpDDDriverCaps+=4;//  18 dwPalCaps
            Memory.mem_writed(lpDDDriverCaps, 0xFFFFFFFF);lpDDDriverCaps+=4;//  1C dwSVCaps
            Memory.mem_writed(lpDDDriverCaps, bpp);lpDDDriverCaps+=4;       //  20 dwAlphaBltConstBitDepths
            Memory.mem_writed(lpDDDriverCaps, bpp);lpDDDriverCaps+=4;       //  24 dwAlphaBltPixelBitDepths
            Memory.mem_writed(lpDDDriverCaps, bpp);lpDDDriverCaps+=4;       //  28 dwAlphaBltSurfaceBitDepths
            Memory.mem_writed(lpDDDriverCaps, bpp);lpDDDriverCaps+=4;       //  2C dwAlphaOverlayConstBitDepths
            Memory.mem_writed(lpDDDriverCaps, bpp);lpDDDriverCaps+=4;       //  30 dwAlphaOverlayPixelBitDepths
            Memory.mem_writed(lpDDDriverCaps, bpp);lpDDDriverCaps+=4;       //  34 dwAlphaOverlaySurfaceBitDepths
            Memory.mem_writed(lpDDDriverCaps, bpp);lpDDDriverCaps+=4;       //  38 dwZBufferBitDepths
            Memory.mem_writed(lpDDDriverCaps, 0x02000000);lpDDDriverCaps+=4;//  3C dwVidMemTotal
            Memory.mem_writed(lpDDDriverCaps, 0x02000000);lpDDDriverCaps+=4;//  40 dwVidMemFree
            Memory.mem_writed(lpDDDriverCaps, 0x00000020);lpDDDriverCaps+=4;//  44 dwMaxVisibleOverlays
            Memory.mem_writed(lpDDDriverCaps, 0x00000000);lpDDDriverCaps+=4;//  48 dwCurrVisibleOverlays
            Memory.mem_writed(lpDDDriverCaps, 0x00000003);lpDDDriverCaps+=4;//  4C dwNumFourCCCodes
//            Memory.mem_writed(lpDDDriverCaps, );lpDDDriverCaps+=4;          //  50 dwAlignBoundarySrc
//            Memory.mem_writed(lpDDDriverCaps, );lpDDDriverCaps+=4;          //  54 dwAlignSizeSrc
//            Memory.mem_writed(lpDDDriverCaps, );lpDDDriverCaps+=4;          //  58 dwAlignBoundaryDest
//            Memory.mem_writed(lpDDDriverCaps, );lpDDDriverCaps+=4;          //  5C dwAlignSizeDest
//            Memory.mem_writed(lpDDDriverCaps, );lpDDDriverCaps+=4;          //  60 dwAlignStrideAlign
//            Memory.mem_writed(lpDDDriverCaps, );lpDDDriverCaps+=32;         //  64 dwRops
//            Memory.mem_writed(lpDDDriverCaps, );lpDDDriverCaps+=4;          //  84 ddsCaps
//            Memory.mem_writed(lpDDDriverCaps, );lpDDDriverCaps+=4;          //  88 dwMinOverlayStretch
//            Memory.mem_writed(lpDDDriverCaps, );lpDDDriverCaps+=4;          //  8C dwMaxOverlayStretch
//            Memory.mem_writed(lpDDDriverCaps, );lpDDDriverCaps+=4;          //  90 dwMinLiveVideoStretch
//            Memory.mem_writed(lpDDDriverCaps, );lpDDDriverCaps+=4;          //  94 dwMaxLiveVideoStretch
//            Memory.mem_writed(lpDDDriverCaps, );lpDDDriverCaps+=4;          //  98 dwMinHwCodecStretch
//            Memory.mem_writed(lpDDDriverCaps, );lpDDDriverCaps+=4;          //  9C dwMaxHwCodecStretch
//            Memory.mem_writed(lpDDDriverCaps, 0);lpDDDriverCaps+=4;         //  A0 dwReserved1
//            Memory.mem_writed(lpDDDriverCaps, 0);lpDDDriverCaps+=4;         //  A4 dwReserved2
//            Memory.mem_writed(lpDDDriverCaps, 0);lpDDDriverCaps+=4;         //  A8 dwReserved3
//            Memory.mem_writed(lpDDDriverCaps, );lpDDDriverCaps+=4;          //  AC dwSVBCaps
//            Memory.mem_writed(lpDDDriverCaps, );lpDDDriverCaps+=4;          //  B0 dwSVBCKeyCaps
//            Memory.mem_writed(lpDDDriverCaps, );lpDDDriverCaps+=4;          //  B4 dwSVBFXCaps
//            Memory.mem_writed(lpDDDriverCaps, );lpDDDriverCaps+=32;         //  B8 dwSVBRops
//            Memory.mem_writed(lpDDDriverCaps, );lpDDDriverCaps+=4;          //  D8 dwVSBCaps
//            Memory.mem_writed(lpDDDriverCaps, );lpDDDriverCaps+=4;          //  DC dwVSBCKeyCaps
//            Memory.mem_writed(lpDDDriverCaps, );lpDDDriverCaps+=4;          //  E0 dwVSBFXCaps
//            Memory.mem_writed(lpDDDriverCaps, );lpDDDriverCaps+=32;         //  E4 dwVSBRops
//            Memory.mem_writed(lpDDDriverCaps, );lpDDDriverCaps+=4;          // 104 dwSSBCaps
//            Memory.mem_writed(lpDDDriverCaps, );lpDDDriverCaps+=4;          // 108 dwSSBCKeyCaps
//            Memory.mem_writed(lpDDDriverCaps, );lpDDDriverCaps+=4;          // 10C dwSSBFXCaps
//            Memory.mem_writed(lpDDDriverCaps, );lpDDDriverCaps+=32;         // 110 dwSSBRops
//            if (size == 0x142) {
//                Memory.mem_writed(lpDDDriverCaps, 0);lpDDDriverCaps+=4;     // 130 dwReserved4
//                Memory.mem_writed(lpDDDriverCaps, 0);lpDDDriverCaps+=4;     // 134 dwReserved5
//                Memory.mem_writed(lpDDDriverCaps, 0);lpDDDriverCaps+=4;     // 138 dwReserved6
//            } else if (size == 0x170) { // DirectX 5
//                Memory.mem_writed(lpDDDriverCaps, 0);lpDDDriverCaps+=4;     // 130 dwMaxVideoPorts
//                Memory.mem_writed(lpDDDriverCaps, 0);lpDDDriverCaps+=4;     // 134 dwCurrVideoPorts
//                Memory.mem_writed(lpDDDriverCaps, 0);lpDDDriverCaps+=4;     // 138 dwSVBCaps2
//                Memory.mem_writed(lpDDDriverCaps, 0);lpDDDriverCaps+=4;     // 13C dwNLVBCaps
//                Memory.mem_writed(lpDDDriverCaps, 0);lpDDDriverCaps+=4;     // 140 dwNLVBCaps2
//                Memory.mem_writed(lpDDDriverCaps, 0);lpDDDriverCaps+=4;     // 144 dwNLVBCKeyCaps
//                Memory.mem_writed(lpDDDriverCaps, 0);lpDDDriverCaps+=4;     // 148 dwNLVBFXCaps
//                Memory.mem_writed(lpDDDriverCaps, 0);lpDDDriverCaps+=32;    // 14C dwNLVBRops
//            }
        }
    };

    // HRESULT GetDisplayMode(this, LPDDSURFACEDESC lpDDSurfaceDesc)
    static private Callback.Handler GetDisplayMode = new HandlerBase() {
        public java.lang.String getName() {
            return "IDirectDraw.GetDisplayMode";
        }
        public void onCall() {
            int This = CPU.CPU_Pop32();
            int lpDDSurfaceDesc = CPU.CPU_Pop32();
            notImplemented();
        }
    };

    // HRESULT GetFourCCCodes(this, LPDWORD lpNumCodes, LPDWORD lpCodes)
    static private Callback.Handler GetFourCCCodes = new HandlerBase() {
        public java.lang.String getName() {
            return "IDirectDraw.GetFourCCCodes";
        }
        public void onCall() {
            int This = CPU.CPU_Pop32();
            int lpNumCodes = CPU.CPU_Pop32();
            int lpCodes = CPU.CPU_Pop32();
            notImplemented();
        }
    };

    // HRESULT GetGDISurface(this, LPDIRECTDRAWSURFACE *lplpGDIDDSurface)
    static private Callback.Handler GetGDISurface = new HandlerBase() {
        public java.lang.String getName() {
            return "IDirectDraw.GetGDISurface";
        }
        public void onCall() {
            int This = CPU.CPU_Pop32();
            int lplpGDIDDSurface = CPU.CPU_Pop32();
            notImplemented();
        }
    };

    // HRESULT GetMonitorFrequency(this, LPDWORD lpdwFrequency)
    static private Callback.Handler GetMonitorFrequency = new HandlerBase() {
        public java.lang.String getName() {
            return "IDirectDraw.GetMonitorFrequency";
        }
        public void onCall() {
            int This = CPU.CPU_Pop32();
            int lpdwFrequency = CPU.CPU_Pop32();
            notImplemented();
        }
    };

    // HRESULT GetScanLine(this, LPDWORD lpdwScanLine)
    static private Callback.Handler GetScanLine = new HandlerBase() {
        public java.lang.String getName() {
            return "IDirectDraw.GetScanLine";
        }
        public void onCall() {
            int This = CPU.CPU_Pop32();
            int lpdwScanLine = CPU.CPU_Pop32();
            notImplemented();
        }
    };

    // HRESULT GetVerticalBlankStatus(this, BOOL *lpbIsInVB)
    static private Callback.Handler GetVerticalBlankStatus = new HandlerBase() {
        public java.lang.String getName() {
            return "IDirectDraw.GetVerticalBlankStatus";
        }
        public void onCall() {
            int This = CPU.CPU_Pop32();
            int lpbIsInVB = CPU.CPU_Pop32();
            notImplemented();
        }
    };

    // HRESULT Initialize(this, GUID *lpGUID)
    static private Callback.Handler Initialize = new HandlerBase() {
        public java.lang.String getName() {
            return "IDirectDraw.Initialize";
        }
        public void onCall() {
            int This = CPU.CPU_Pop32();
            int lpGUID = CPU.CPU_Pop32();
            notImplemented();
        }
    };

    // HRESULT RestoreDisplayMode(this)
    static private Callback.Handler RestoreDisplayMode = new HandlerBase() {
        public java.lang.String getName() {
            return "IDirectDraw.RestoreDisplayMode";
        }
        public void onCall() {
            int This = CPU.CPU_Pop32();
            notImplemented();
        }
    };

    // HRESULT SetCooperativeLevel(this, HWND hWnd, DWORD dwFlags)
    static private Callback.Handler SetCooperativeLevel = new HandlerBase() {
        public java.lang.String getName() {
            return "IDirectDraw.SetCooperativeLevel";
        }
        public void onCall() {
            int This = CPU.CPU_Pop32();
            int hWnd = CPU.CPU_Pop32();
            int dwFlags = CPU.CPU_Pop32();
            if ((dwFlags & ~(DDSCL_FULLSCREEN|DDSCL_ALLOWREBOOT|DDSCL_EXCLUSIVE|DDSCL_ALLOWMODEX))!=0) {
                Console.out("DDraw.SetCooperativeLevel: unsupported flags: " + Integer.toString(dwFlags, 16));
            }
            CPU_Regs.reg_eax.dword = Error.S_OK;
        }
    };

    // HRESULT SetDisplayMode(this, DWORD dwWidth, DWORD dwHeight, DWORD dwBPP)
    static private Callback.Handler SetDisplayMode = new HandlerBase() {
        public java.lang.String getName() {
            return "IDirectDraw.SetDisplayMode";
        }
        public void onCall() {
            int This = CPU.CPU_Pop32();
            int dwWidth = CPU.CPU_Pop32();
            int dwHeight = CPU.CPU_Pop32();
            int dwBPP = CPU.CPU_Pop32();
            if ((getData(This, OFFSET_FLAGS) & FLAGS_V7)!=0) {
                int dwRefreshRate = CPU.CPU_Pop32();
                int dwFlags = CPU.CPU_Pop32();
            }
            setData(This, OFFSET_CX, dwWidth);
            setData(This, OFFSET_CY, dwHeight);
            setData(This, OFFSET_BPP, dwBPP);
            WinSystem.setScreenSize(dwWidth, dwHeight, dwBPP);
            if (dwBPP<=8)
                getPalette(This); // set up default palette
            Main.GFX_SetSize(dwWidth, dwHeight, false, false, false, 32);
            CPU_Regs.reg_eax.dword = Error.S_OK;
            if (Module.LOG)
                log(dwWidth+"x"+dwHeight+" @ "+dwBPP+"bpp");
        }
    };

    // HRESULT WaitForVerticalBlank(this, DWORD dwFlags, HANDLE hEvent)
    static private Callback.Handler WaitForVerticalBlank = new HandlerBase() {
        static final public int DDWAITVB_BLOCKBEGIN =       0x00000001;
        static final public int DDWAITVB_BLOCKBEGINEVENT =  0x00000002;
        static final public int DDWAITVB_BLOCKEND =         0x00000004;

        public java.lang.String getName() {
            return "IDirectDraw.WaitForVerticalBlank";
        }
        public void onCall() {
            int This = CPU.CPU_Pop32();
            int dwFlags = CPU.CPU_Pop32();
            int hEvent = CPU.CPU_Pop32();
        }
    };
}
