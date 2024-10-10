package jdos.win.builtin.directx.ddraw;

import jdos.gui.Main;
import jdos.hardware.Memory;
import jdos.win.Win;
import jdos.win.builtin.user32.WinPos;
import jdos.win.builtin.user32.WinWindow;
import jdos.win.system.StaticData;
import jdos.win.system.WinSystem;
import jdos.win.utils.Error;
import jdos.win.utils.Ptr;

public class IDirectDraw extends IUnknown {
    static final int DDSCL_FULLSCREEN = 0x00000001;
    static final int DDSCL_ALLOWREBOOT = 0x00000002;
    static final int  DDSCL_EXCLUSIVE = 0x00000010;
    static final int DDSCL_ALLOWMODEX = 0x00000040;

    static public final boolean LOGDD = true;

    static final int VTABLE_SIZE = 20;

    private static int createVTable() {
        int address = allocateVTable("IDirectDraw", VTABLE_SIZE);
        addIDirectDraw(address, false);
        return address;
    }

    static int addIDirectDraw(int address, boolean v7) {
        address = addIUnknown(address);
        address = add(address, IDirectDraw.class, "Compact", (LOGDD?new String[] {"(HEX)this", "(HRESULT)result"}:null));
        address = add(address, IDirectDraw.class, "CreateClipper", (LOGDD?new String[] {"(HEX)this", "(HEX)dwFlags", "(HEX)lplpDDClipper", "(HEX)pUnkOuter", "(HRESULT)result"}:null));
        address = add(address, IDirectDraw.class, "CreatePalette", (LOGDD?new String[] {"(HEX)this", "(HEX)dwFlags", "(HEX)lpColorTable", "(HEX)lplpDDPalette", "(HEX)pUnkOuter", "(HRESULT)result"}:null));
        address = add(address, IDirectDraw.class, "CreateSurface", (LOGDD?new String[] {"(HEX)this", "(HEX)lpDDSurfaceDesc", "(HEX)lplpDDSurface", "(HEX)pUnkOuter", "(HRESULT)result"}:null));
        address = add(address, IDirectDraw.class, "DuplicateSurface", (LOGDD?new String[] {"(HEX)this", "(HEX)lpDDSurface", "(HEX)lplpDupDDSurface", "(HRESULT)result"}:null));
        address = add(address, IDirectDraw.class, "EnumDisplayModes", (LOGDD?new String[] {"(HEX)this", "(HEX)dwFlags", "(HEX)lpDDSurfaceDesc", "(HEX)lpContext", "(HEX)lpEnumModesCallback", "(HRESULT)result"}:null));
        address = add(address, IDirectDraw.class, "EnumSurfaces", (LOGDD?new String[] {"(HEX)this", "(HEX)dwFlags", "(HEX)lpDDSD", "(HEX)lpContext", "(HEX)lpEnumSurfacesCallback", "(HRESULT)result"}:null));
        address = add(address, IDirectDraw.class, "FlipToGDISurface", (LOGDD?new String[] {"(HEX)this", "(HRESULT)result"}:null));
        address = add(address, IDirectDraw.class, "GetCaps", (LOGDD?new String[] {"(HEX)this", "(HEX)lpDDDriverCaps", "(HEX)lpDDHELCaps", "(HRESULT)result"}:null));
        address = add(address, IDirectDraw.class, "GetDisplayMode", (LOGDD?new String[] {"(HEX)this", "(HEX)lpDDSurfaceDesc", "(HRESULT)result"}:null));
        address = add(address, IDirectDraw.class, "GetFourCCCodes", (LOGDD?new String[] {"(HEX)this", "(HEX)lpNumCodes", "(HEX)lpCodes", "(HRESULT)result"}:null));
        address = add(address, IDirectDraw.class, "GetGDISurface", (LOGDD?new String[] {"(HEX)this", "(HEX)lplpGDIDDSurface", "(HRESULT)result"}:null));
        address = add(address, IDirectDraw.class, "GetMonitorFrequency", (LOGDD?new String[] {"(HEX)this", "(HEX)lpdwFrequency", "(HRESULT)result"}:null));
        address = add(address, IDirectDraw.class, "GetScanLine", (LOGDD?new String[] {"(HEX)this", "(HEX)lpdwScanLine", "(HRESULT)result"}:null));
        address = add(address, IDirectDraw.class, "GetVerticalBlankStatus", (LOGDD?new String[] {"(HEX)this", "(HEX)lpbIsInVB", "(HRESULT)result"}:null));
        address = add(address, IDirectDraw.class, "Initialize", (LOGDD?new String[] {"(HEX)this", "(GUID)lpGUID", "(HRESULT)result"}:null));
        address = add(address, IDirectDraw.class, "RestoreDisplayMode", (LOGDD?new String[] {"(HEX)this", "(HRESULT)result"}:null));
        address = add(address, IDirectDraw.class, "SetCooperativeLevel", (LOGDD?new String[] {"(HEX)this", "hWnd", "(HEX)dwFlags", "(HRESULT)result"}:null));
        if (v7)
            address = add(address, IDirectDraw.class, "SetDisplayMode7", (LOGDD?new String[] {"this", "dwWidth", "dwHeight", "dwBPP", "dwRefreshRate", "dwFlags", "(HRESULT)result"}:null));
        else
            address = add(address, IDirectDraw.class, "SetDisplayMode", (LOGDD?new String[] {"this", "dwWidth", "dwHeight", "dwBPP", "(HRESULT)result"}:null));
        address = add(address, IDirectDraw.class, "WaitForVerticalBlank", (LOGDD?new String[] {"(HEX)this", "(HEX)dwFlags", "hEvent", "(HRESULT)result"}:null));
        return address;
    }

    static int FLAGS_CALLBACK2 = 0x00000001;
    static int FLAGS_DESC2 = 0x00000002;
    static int FLAGS_V7 = 0x00000004;

    static int OFFSET_FLAGS = 0;

    static final int OFFSET_PALETTE = 4;

    static final int DATA_SIZE = 8;

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
        StaticData.currentDirectDraw = address;
        return address;
    }

    // HRESULT Compact(this)
    public static int Compact(int This) {
        Win.panic("IDirectDraw.Compact not implemented yet");
        return Error.S_OK;
    }

    // HRESULT CreateClipper(this, DWORD dwFlags, LPDIRECTDRAWCLIPPER *lplpDDClipper, IUnknown *pUnkOuter)
    public static int CreateClipper(int This, int dwFlags, int lplpDDClipper, int pUnkOuter) {
        if (lplpDDClipper == 0)
            return Error.E_POINTER;
        else {
            writed(lplpDDClipper, IDirectDrawClipper.create());
            return Error.S_OK;
        }
    }

    // HRESULT CreatePalette(this, DWORD dwFlags, LPPALETTEENTRY lpColorTable, LPDIRECTDRAWPALETTE *lplpDDPalette, IUnknown *pUnkOuter)
    public static int CreatePalette(int This, int dwFlags, int lpColorTable, int lplpDDPalette, int pUnkOuter) {
        if (lplpDDPalette == 0)
            return Error.E_POINTER;
        else {
            writed(lplpDDPalette, IDirectDrawPalette.create(dwFlags, lpColorTable));
            return Error.S_OK;
        }
    }

    // HRESULT CreateSurface(this, LPDDSURFACEDESC lpDDSurfaceDesc, LPDIRECTDRAWSURFACE *lplpDDSurface, IUnknown *pUnkOuter)
    public static int CreateSurface(int This, int lpDDSurfaceDesc, int lplpDDSurface, int pUnkOuter) {
        if (lplpDDSurface == 0)
            return Error.E_POINTER;
        else {
            int result;
            if ((getData(This, OFFSET_FLAGS) & FLAGS_V7)!=0) {
                result = IDirectDrawSurface7.create(This, lpDDSurfaceDesc);
            } else {
                result = IDirectDrawSurface.create(This, lpDDSurfaceDesc);
            }
            Memory.mem_writed(lplpDDSurface, result);
            return Error.S_OK;
        }
    }

    // HRESULT DuplicateSurface(this, LPDIRECTDRAWSURFACE lpDDSurface, LPDIRECTDRAWSURFACE *lplpDupDDSurface)
    public static int DuplicateSurface(int This, int lpDDSurface, int lplpDupDDSurface) {
        Win.panic("IDirectDraw.DuplicateSurface not implemented yet");
        return Error.S_OK;
    }


    static private int[][] mode = new int[][] {
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
    // HRESULT EnumDisplayModes(this, DWORD dwFlags, LPDDSURFACEDESC lpDDSurfaceDesc, LPVOID lpContext, LPDDENUMMODESCALLBACK lpEnumModesCallback)
    public static int EnumDisplayModes(int This, int dwFlags, int lpDDSurfaceDesc, int lpContext, int  lpEnumModesCallback) {
        DDSurfaceDesc desc = null;
        if (lpDDSurfaceDesc != 0)
            desc = new DDSurfaceDesc(lpDDSurfaceDesc, false);
        int address = WinSystem.getCurrentProcess().getTemp(DDSurfaceDesc.SIZE);
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
        return Error.S_OK;
    }

    // HRESULT EnumSurfaces(this, DWORD dwFlags, LPDDSURFACEDESC lpDDSD, LPVOID lpContext, LPDDENUMSURFACESCALLBACK lpEnumSurfacesCallback)
    public static int EnumSurfaces(int This, int dwFlags, int lpDDSD, int lpContext, int lpEnumSurfacesCallback) {
        Win.panic("IDirectDraw.EnumSurfaces not implemented yet");
        return Error.S_OK;
    }

    // HRESULT FlipToGDISurface(this)
    public static int FlipToGDISurface(int This) {
        Win.panic("IDirectDraw.FlipToGDISurface not implemented yet");
        return Error.S_OK;
    }


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

    // HRESULT GetCaps(this, LPDDCAPS lpDDDriverCaps, LPDDCAPS lpDDHELCaps)
    public static int GetCaps(int This, int lpDDDriverCaps, int lpDDHELCaps) {
        int bpp = 0x00000D00; // 8 16 and 32
        int size = Memory.mem_readd(lpDDDriverCaps);lpDDDriverCaps+=4;
        Memory.mem_zero(lpDDDriverCaps, size-4);
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
        return Error.S_OK;
    }

    // HRESULT GetDisplayMode(this, LPDDSURFACEDESC lpDDSurfaceDesc)
    public static int GetDisplayMode(int This, int lpDDSurfaceDesc) {
        Win.panic("IDirectDraw.FlipToGDISurface not implemented yet");
        return Error.S_OK;
    }

    // HRESULT GetFourCCCodes(this, LPDWORD lpNumCodes, LPDWORD lpCodes)
    public static int GetFourCCCodes(int This, int lpNumCodes, int lpCodes) {
        Win.panic("IDirectDraw.GetFourCCCodes not implemented yet");
        return Error.S_OK;
    }

    // HRESULT GetGDISurface(this, LPDIRECTDRAWSURFACE *lplpGDIDDSurface)
    public static int GetGDISurface(int This, int lplpGDIDDSurface) {
        Win.panic("IDirectDraw.GetFourCCCodes not implemented yet");
        return Error.S_OK;
    }

    // HRESULT GetMonitorFrequency(this, LPDWORD lpdwFrequency)
    public static int GetMonitorFrequency(int This, int lpdwFrequency) {
        Win.panic("IDirectDraw.GetMonitorFrequency not implemented yet");
        return Error.S_OK;
    }

    // HRESULT GetScanLine(this, LPDWORD lpdwScanLine)
    public static int GetScanLine(int This, int lpdwScanLine) {
        Win.panic("IDirectDraw.GetScanLine not implemented yet");
        return Error.S_OK;
    }

    // HRESULT GetVerticalBlankStatus(this, BOOL *lpbIsInVB)
    public static int GetVerticalBlankStatus(int This, int lpbIsInVB) {
        Win.panic("IDirectDraw.GetVerticalBlankStatus not implemented yet");
        return Error.S_OK;
    }

    // HRESULT Initialize(this, GUID *lpGUID)
    public static int Initialize(int This, int lpGUID) {
        Win.panic("IDirectDraw.Initialize not implemented yet");
        return Error.S_OK;
    }

    // HRESULT RestoreDisplayMode(this)
    public static int RestoreDisplayMode(int This) {
        Win.panic("IDirectDraw.RestoreDisplayMode not implemented yet");
        return Error.S_OK;
    }

    // HRESULT SetCooperativeLevel(this, HWND hWnd, DWORD dwFlags)
    public static int SetCooperativeLevel(int This, int hWnd, int dwFlags) {
        if ((dwFlags & ~(DDSCL_FULLSCREEN|DDSCL_ALLOWREBOOT|DDSCL_EXCLUSIVE|DDSCL_ALLOWMODEX))!=0) {
            log("DDraw.SetCooperativeLevel: unsupported flags: " + Ptr.toString(dwFlags));
        }
        StaticData.ddrawWindow = hWnd;
        return Error.S_OK;
    }

    // HRESULT SetDisplayMode(this, DWORD dwWidth, DWORD dwHeight, DWORD dwBPP)
    public static int SetDisplayMode(int This, int dwWidth, int dwHeight, int dwBPP) {
        WinSystem.setScreenSize(dwWidth, dwHeight, dwBPP);
        if (dwBPP<=8)
            getPalette(This); // set up default palette
        if (StaticData.currentPrimarySurface != 0) {
            IDirectDrawSurface.setData(StaticData.currentPrimarySurface, IDirectDrawSurface.OFFSET_DESC+0x08, dwHeight);
            IDirectDrawSurface.setData(StaticData.currentPrimarySurface, IDirectDrawSurface.OFFSET_DESC+0x0C, dwWidth);
            IDirectDrawSurface.setData(StaticData.currentPrimarySurface, IDirectDrawSurface.OFFSET_DESC+0x10, dwWidth*dwBPP/8);
            IDirectDrawSurface.setData(StaticData.currentPrimarySurface, IDirectDrawSurface.OFFSET_DESC+0x54, dwBPP);
        }
        Main.GFX_SetSize(dwWidth, dwHeight, dwWidth, dwHeight, false, 32);
        WinPos.SetWindowPos(StaticData.ddrawWindow, 0, 0, 0, dwWidth, dwHeight, SWP_NOZORDER | SWP_NOACTIVATE);
        WinWindow.get(StaticData.desktopWindow).rectWindow.set(0, 0, dwWidth, dwHeight);
        WinWindow.get(StaticData.desktopWindow).rectClient.set(0, 0, dwWidth, dwHeight);
        return Error.S_OK;
    }

    // HRESULT SetDisplayMode(this, DWORD dwWidth, DWORD dwHeight, DWORD dwBPP, DWORD dwRefreshRate, DWORD dwFlags)
    public static int SetDisplayMode7(int This, int dwWidth, int dwHeight, int dwBPP, int dwRefreshRate, int dwFlags) {
        return SetDisplayMode(This, dwWidth, dwHeight, dwBPP);
    }

    static final public int DDWAITVB_BLOCKBEGIN =       0x00000001;
    static final public int DDWAITVB_BLOCKBEGINEVENT =  0x00000002;
    static final public int DDWAITVB_BLOCKEND =         0x00000004;

    // HRESULT WaitForVerticalBlank(this, DWORD dwFlags, HANDLE hEvent)
    static public int WaitForVerticalBlank(int This, int dwFlags, int hEvent) {
        return Error.S_OK;
    }
}
