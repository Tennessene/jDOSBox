package jdos.win.builtin.ddraw;

import jdos.cpu.CPU;
import jdos.cpu.CPU_Regs;
import jdos.cpu.Callback;
import jdos.hardware.Memory;
import jdos.win.Console;
import jdos.win.Win;
import jdos.win.builtin.HandlerBase;
import jdos.win.kernel.VideoMemory;
import jdos.win.utils.Error;

public class IDirectDrawSurface extends IUnknown {
    static final private int DDSCAPS_ALPHA =                0x00000002; /* surface contains alpha information */
    static final private int DDSCAPS_BACKBUFFER =           0x00000004; /* this surface is a backbuffer */
    static final private int DDSCAPS_COMPLEX =              0x00000008; /* complex surface structure */
    static final private int DDSCAPS_FLIP =                 0x00000010; /* part of surface flipping structure */
    static final private int DDSCAPS_FRONTBUFFER =          0x00000020; /* this surface is the frontbuffer surface */
    static final private int DDSCAPS_OFFSCREENPLAIN =       0x00000040; /* this is a plain offscreen surface */
    static final private int DDSCAPS_OVERLAY =              0x00000080; /* overlay */
    static final private int DDSCAPS_PALETTE =              0x00000100; /* palette objects can be created and attached to us */
    static final private int DDSCAPS_PRIMARYSURFACE =       0x00000200; /* primary surface (the one the user looks at currently)(right eye)*/
    static final private int DDSCAPS_PRIMARYSURFACELEFT	=   0x00000400; /* primary surface for left eye */
    static final private int DDSCAPS_SYSTEMMEMORY =         0x00000800; /* surface exists in systemmemory */
    static final private int DDSCAPS_TEXTURE =              0x00001000; /* surface can be used as a texture */
    static final private int DDSCAPS_3DDEVICE =             0x00002000; /* surface may be destination for 3d rendering */
    static final private int DDSCAPS_VIDEOMEMORY =          0x00004000; /* surface exists in videomemory */
    static final private int DDSCAPS_VISIBLE =              0x00008000; /* surface changes immediately visible */
    static final private int DDSCAPS_WRITEONLY =            0x00010000; /* write only surface */
    static final private int DDSCAPS_ZBUFFER =              0x00020000; /* zbuffer surface */
    static final private int DDSCAPS_OWNDC =                0x00040000; /* has its own DC */
    static final private int DDSCAPS_LIVEVIDEO =            0x00080000; /* surface should be able to receive live video */
    static final private int DDSCAPS_HWCODEC =              0x00100000; /* should be able to have a hw codec decompress stuff into it */
    static final private int DDSCAPS_MODEX =                0x00200000; /* mode X (320x200 or 320x240) surface */
    static final private int DDSCAPS_MIPMAP =               0x00400000; /* one mipmap surface (1 level) */
    static final private int DDSCAPS_RESERVED2 =            0x00800000;
    static final private int DDSCAPS_ALLOCONLOAD =          0x04000000; /* memory allocation delayed until Load() */
    static final private int DDSCAPS_VIDEOPORT =            0x08000000; /* Indicates that the surface will receive data from a video port */
    static final private int DDSCAPS_LOCALVIDMEM =          0x10000000; /* surface is in local videomemory */
    static final private int DDSCAPS_NONLOCALVIDMEM =       0x20000000; /* surface is in nonlocal videomemory */
    static final private int DDSCAPS_STANDARDVGAMODE =      0x40000000; /* surface is a standard VGA mode surface (NOT ModeX) */
    static final private int DDSCAPS_OPTIMIZED =            0x80000000;

    private static int OFFSET_PALETTE = 0;
    private static int OFFSET_MEMORY = 4;
    private static int OFFSET_DESC = 8;

    public static int create(int pDirectDraw, int pDesc) {
        int vtable = getVTable("IDirectDrawSurface");
        if (vtable == 0)
            vtable = createVTable();
        int result = allocate(vtable, DDSurfaceDesc.SIZE+8);
        Memory.mem_memcpy(result+OFFSET_IUNKNOWN+OFFSET_DESC, pDesc, DDSurfaceDesc.SIZE);

        DDSurfaceDesc d = new DDSurfaceDesc(pDesc);
        if ((d.dwFlags & DDSurfaceDesc.DDSD_BACKBUFFERCOUNT)!=0) {
            Win.panic("IDirectDraw.CreateSurface back buffers not supported yet");
        }
        if ((d.ddsCaps & DDSCAPS_PRIMARYSURFACE)!=0) {
            int width = IDirectDraw.getWidth(pDirectDraw);
            int height = IDirectDraw.getHeight(pDirectDraw);
            int bpp = IDirectDraw.getBPP(pDirectDraw);
            int amount = width*height*bpp/8;
            int memory = VideoMemory.mapVideoRAM(amount);
            setData(result, OFFSET_MEMORY, memory);
            setData(result, OFFSET_DESC+0x04, DDSurfaceDesc.DDSD_CAPS|DDSurfaceDesc.DDSD_HEIGHT|DDSurfaceDesc.DDSD_WIDTH|DDSurfaceDesc.DDSD_PITCH|DDSurfaceDesc.DDSD_PIXELFORMAT);
            setData(result, OFFSET_DESC+0x08, width);
            setData(result, OFFSET_DESC+0x0C, height);
            setData(result, OFFSET_DESC+0x10, width); // pitch

            int caps = d.ddsCaps | DDSCAPS_VIDEOMEMORY | DDSCAPS_VISIBLE | DDSCAPS_LOCALVIDMEM;
            int pfFlags = 0;

            if (bpp == 8 && (d.ddsCaps & DDSCAPS_PALETTE)==0) {
                caps|=DDSCAPS_PALETTE;
                pfFlags=DDPixelFormat.DDPF_PALETTEINDEXED8 | DDPixelFormat.DDPF_RGB;
            } else {
                Win.panic("IDirectDraw.CreateSurface currently only supports 8-bit");
            }

            setData(result, OFFSET_DESC+0x68, caps);

            setData(result, OFFSET_DESC+0x48, DDPixelFormat.SIZE);
            setData(result, OFFSET_DESC+0x48+0x04, pfFlags);
            setData(result, OFFSET_DESC+0x48+0x0C, 8); // dwRGBBitCount
        } else {
            Win.panic("IDirectDraw.CreateSurface currently only primary surface is supported");
        }

        return result;
    }

    static private int createVTable() {
        int address = allocateVTable("IDirectDrawSurface", 33);
        int result = address;
        address = addIUnknown(address);

        address = add(address, AddAttachedSurface);
        address = add(address, AddOverlayDirtyRect);
        address = add(address, Blt);
        address = add(address, BltBatch);
        address = add(address, BltFast);
        address = add(address, DeleteAttachedSurface);
        address = add(address, EnumAttachedSurfaces);
        address = add(address, EnumOverlayZOrders);
        address = add(address, Flip);
        address = add(address, GetAttachedSurface);
        address = add(address, GetBltStatus);
        address = add(address, GetCaps);
        address = add(address, GetClipper);
        address = add(address, GetColorKey);
        address = add(address, GetDC);
        address = add(address, GetFlipStatus);
        address = add(address, GetOverlayPosition);
        address = add(address, GetPalette);
        address = add(address, GetPixelFormat);
        address = add(address, GetSurfaceDesc);
        address = add(address, Initialize);
        address = add(address, IsLost);
        address = add(address, Lock);
        address = add(address, ReleaseDC);
        address = add(address, Restore);
        address = add(address, SetClipper);
        address = add(address, SetColorKey);
        address = add(address, SetOverlayPosition);
        address = add(address, SetPalette);
        address = add(address, Unlock);
        address = add(address, UpdateOverlay);
        address = add(address, UpdateOverlayDisplay);
        address = add(address, UpdateOverlayZOrder);
        return result;
    }
    // HRESULT AddAttachedSurface(this, LPDIRECTDRAWSURFACE lpDDSAttachedSurface)
    static private Callback.Handler AddAttachedSurface = new HandlerBase() {
        public java.lang.String getName() {
            return "IDirectDrawSurface.AddAttachedSurface";
        }
        public void onCall() {
            int This = CPU.CPU_Pop32();
            int lpDDSAttachedSurface = CPU.CPU_Pop32();
            notImplemented();
        }
    };
    
    // HRESULT AddOverlayDirtyRect(this, LPRECT lpRect)
    static private Callback.Handler AddOverlayDirtyRect = new HandlerBase() {
        public java.lang.String getName() {
            return "IDirectDrawSurface.AddOverlayDirtyRect";
        }
        public void onCall() {
            int This = CPU.CPU_Pop32();
            int lpRect = CPU.CPU_Pop32();
            notImplemented();
        }
    };

    // HRESULT Blt(this, LPRECT lpDestRect, LPDIRECTDRAWSURFACE lpDDSrcSurface, LPRECT lpSrcRect, DWORD dwFlags, LPDDBLTFX lpDDBltFx)
    static private Callback.Handler Blt = new HandlerBase() {
        public java.lang.String getName() {
            return "IDirectDrawSurface.Blt";
        }
        public void onCall() {
            int This = CPU.CPU_Pop32();
            int lpDestRect = CPU.CPU_Pop32();
            int lpDDSrcSurface = CPU.CPU_Pop32();
            int lpSrcRect = CPU.CPU_Pop32();
            int dwFlags = CPU.CPU_Pop32();
            int lpDDBltFx = CPU.CPU_Pop32();
            notImplemented();
        }
    };

    // HRESULT BltBatch(this, LPDDBLTBATCH lpDDBltBatch, DWORD dwCount, DWORD dwFlags)
    static private Callback.Handler BltBatch = new HandlerBase() {
        public java.lang.String getName() {
            return "IDirectDrawSurface.BltBatch";
        }
        public void onCall() {
            int This = CPU.CPU_Pop32();
            int lpDDBltBatch = CPU.CPU_Pop32();
            int dwCount = CPU.CPU_Pop32();
            int dwFlags = CPU.CPU_Pop32();
            notImplemented();
        }
    };

    // HRESULT BltFast(this, DWORD dwX, DWORD dwY, LPDIRECTDRAWSURFACE lpDDSrcSurface, LPRECT lpSrcRect, DWORD dwTrans)
    static private Callback.Handler BltFast = new HandlerBase() {
        public java.lang.String getName() {
            return "IDirectDrawSurface.BltFast";
        }
        public void onCall() {
            int This = CPU.CPU_Pop32();
            int dwX = CPU.CPU_Pop32();
            int dwY = CPU.CPU_Pop32();
            int lpDDSrcSurface = CPU.CPU_Pop32();
            int lpSrcRect = CPU.CPU_Pop32();
            int dwTrans = CPU.CPU_Pop32();
            notImplemented();
        }
    };

    // HRESULT DeleteAttachedSurface(this, DWORD dwFlags, LPDIRECTDRAWSURFACE lpDDSAttachedSurface)
    static private Callback.Handler DeleteAttachedSurface = new HandlerBase() {
        public java.lang.String getName() {
            return "IDirectDrawSurface.DeleteAttachedSurface";
        }
        public void onCall() {
            int This = CPU.CPU_Pop32();
            int dwFlags = CPU.CPU_Pop32();
            int lpDDSAttachedSurface = CPU.CPU_Pop32();
            notImplemented();
        }
    };

    // HRESULT EnumAttachedSurfaces(this, LPVOID lpContext, LPDDENUMSURFACESCALLBACK lpEnumSurfacesCallback)
    static private Callback.Handler EnumAttachedSurfaces = new HandlerBase() {
        public java.lang.String getName() {
            return "IDirectDrawSurface.EnumAttachedSurfaces";
        }
        public void onCall() {
            int This = CPU.CPU_Pop32();
            int lpContext = CPU.CPU_Pop32();
            int lpEnumSurfacesCallback = CPU.CPU_Pop32();
            notImplemented();
        }
    };

    // HRESULT EnumOverlayZOrders(this, DWORD dwFlags, LPVOID lpContext, LPDDENUMSURFACESCALLBACK lpfnCallback)
    static private Callback.Handler EnumOverlayZOrders = new HandlerBase() {
        public java.lang.String getName() {
            return "IDirectDrawSurface.EnumOverlayZOrders";
        }
        public void onCall() {
            int This = CPU.CPU_Pop32();
            int dwFlags = CPU.CPU_Pop32();
            int lpContext = CPU.CPU_Pop32();
            int lpfnCallback = CPU.CPU_Pop32();
            notImplemented();
        }
    };

    // HRESULT Flip(this, LPDIRECTDRAWSURFACE lpDDSurfaceTargetOverride, DWORD dwFlags)
    static private Callback.Handler Flip = new HandlerBase() {
        public java.lang.String getName() {
            return "IDirectDrawSurface.Flip";
        }
        public void onCall() {
            int This = CPU.CPU_Pop32();
            int lpDDSurfaceTargetOverride = CPU.CPU_Pop32();
            int dwFlags = CPU.CPU_Pop32();
            notImplemented();
        }
    };

    // HRESULT GetAttachedSurface(this, LPDDSCAPS lpDDSCaps, LPDIRECTDRAWSURFACE *lplpDDAttachedSurface)
    static private Callback.Handler GetAttachedSurface = new HandlerBase() {
        public java.lang.String getName() {
            return "IDirectDrawSurface.AddAttachedSurface";
        }
        public void onCall() {
            int This = CPU.CPU_Pop32();
            int lpDDSCaps = CPU.CPU_Pop32();
            int lplpDDAttachedSurface = CPU.CPU_Pop32();
            notImplemented();
        }
    };

    // HRESULT GetBltStatus(this, DWORD dwFlags)
    static private Callback.Handler GetBltStatus = new HandlerBase() {
        public java.lang.String getName() {
            return "IDirectDrawSurface.GetBltStatus";
        }
        public void onCall() {
            int This = CPU.CPU_Pop32();
            int dwFlags = CPU.CPU_Pop32();
            notImplemented();
        }
    };

    // HRESULT GetCaps(this, LPDDSCAPS lpDDSCaps)
    static private Callback.Handler GetCaps = new HandlerBase() {
        public java.lang.String getName() {
            return "IDirectDrawSurface.GetCaps";
        }
        public void onCall() {
            int This = CPU.CPU_Pop32();
            int lpDDSCaps = CPU.CPU_Pop32();
            if (lpDDSCaps == 0) {
                CPU_Regs.reg_eax.dword = Error.E_POINTER;
            } else {
                Memory.mem_writed(lpDDSCaps, Memory.mem_readd(This+OFFSET_IUNKNOWN+OFFSET_DESC+0x68));
                CPU_Regs.reg_eax.dword = Error.S_OK;
            }
        }
    };

    // HRESULT GetClipper(this, LPDIRECTDRAWCLIPPER *lplpDDClipper)
    static private Callback.Handler GetClipper = new HandlerBase() {
        public java.lang.String getName() {
            return "IDirectDrawSurface.GetClipper";
        }
        public void onCall() {
            int This = CPU.CPU_Pop32();
            int lplpDDClipper = CPU.CPU_Pop32();
            notImplemented();
        }
    };

    // HRESULT GetColorKey(this, DWORD dwFlags, LPDDCOLORKEY lpDDColorKey)
    static private Callback.Handler GetColorKey = new HandlerBase() {
        public java.lang.String getName() {
            return "IDirectDrawSurface.GetColorKey";
        }
        public void onCall() {
            int This = CPU.CPU_Pop32();
            int dwFlags = CPU.CPU_Pop32();
            int lpDDColorKey = CPU.CPU_Pop32();
            notImplemented();
        }
    };

    // HRESULT GetDC(this, HDC *lphDC)
    static private Callback.Handler GetDC = new HandlerBase() {
        public java.lang.String getName() {
            return "IDirectDrawSurface.GetDC";
        }
        public void onCall() {
            int This = CPU.CPU_Pop32();
            int lphDC = CPU.CPU_Pop32();
            notImplemented();
        }
    };

    // HRESULT GetFlipStatus(this, DWORD dwFlags)
    static private Callback.Handler GetFlipStatus = new HandlerBase() {
        public java.lang.String getName() {
            return "IDirectDrawSurface.GetFlipStatus";
        }
        public void onCall() {
            int This = CPU.CPU_Pop32();
            int dwFlags = CPU.CPU_Pop32();
            notImplemented();
        }
    };

    // HRESULT GetOverlayPosition(this, LPLONG lplX, LPLONG lplY)
    static private Callback.Handler GetOverlayPosition = new HandlerBase() {
        public java.lang.String getName() {
            return "IDirectDrawSurface.GetOverlayPosition";
        }
        public void onCall() {
            int This = CPU.CPU_Pop32();
            int lplX = CPU.CPU_Pop32();
            int lplY = CPU.CPU_Pop32();
            notImplemented();
        }
    };

    // HRESULT GetPalette(this, LPDIRECTDRAWPALETTE *lplpDDPalette)
    static private Callback.Handler GetPalette = new HandlerBase() {
        public java.lang.String getName() {
            return "IDirectDrawSurface.GetPalette";
        }
        public void onCall() {
            int This = CPU.CPU_Pop32();
            int lplpDDPalette = CPU.CPU_Pop32();
            notImplemented();
        }
    };

    // HRESULT GetPixelFormat(this, LPDDPIXELFORMAT lpDDPixelFormat)
    static private Callback.Handler GetPixelFormat = new HandlerBase() {
        public java.lang.String getName() {
            return "IDirectDrawSurface.GetPixelFormat";
        }
        public void onCall() {
            int This = CPU.CPU_Pop32();
            int lpDDPixelFormat = CPU.CPU_Pop32();
            notImplemented();
        }
    };

    // HRESULT GetSurfaceDesc(this, LPDDSURFACEDESC lpDDSurfaceDesc)
    static private Callback.Handler GetSurfaceDesc = new HandlerBase() {
        public java.lang.String getName() {
            return "IDirectDrawSurface.GetSurfaceDesc";
        }
        public void onCall() {
            int This = CPU.CPU_Pop32();
            int lpDDSurfaceDesc = CPU.CPU_Pop32();
            notImplemented();
        }
    };

    // HRESULT Initialize(this, LPDIRECTDRAW lpDD, LPDDSURFACEDESC lpDDSurfaceDesc)
    static private Callback.Handler Initialize = new HandlerBase() {
        public java.lang.String getName() {
            return "IDirectDrawSurface.Initialize";
        }
        public void onCall() {
            int This = CPU.CPU_Pop32();
            int lpDD = CPU.CPU_Pop32();
            int lpDDSurfaceDesc = CPU.CPU_Pop32();
            notImplemented();
        }
    };

    // HRESULT IsLost(this)
    static private Callback.Handler IsLost = new HandlerBase() {
        public java.lang.String getName() {
            return "IDirectDrawSurface.IsLost";
        }
        public void onCall() {
            int This = CPU.CPU_Pop32();
            notImplemented();
        }
    };

    // HRESULT Lock(this, LPRECT lpDestRect, LPDDSURFACEDESC lpDDSurfaceDesc, DWORD dwFlags, HANDLE hEvent)
    static private Callback.Handler Lock = new HandlerBase() {
        static private final int DDLOCK_SURFACEMEMORYPTR =  0x00000000;
        static private final int DDLOCK_WAIT =              0x00000001;
        static private final int DDLOCK_EVENT =             0x00000002;
        static private final int DDLOCK_READONLY =          0x00000010;
        static private final int DDLOCK_WRITEONLY =         0x00000020;
        static private final int DDLOCK_NOSYSLOCK =         0x00000800;
        static private final int DDLOCK_NOOVERWRITE =       0x00001000;
        static private final int DDLOCK_DISCARDCONTENTS =   0x00002000;

        public java.lang.String getName() {
            return "IDirectDrawSurface.Lock";
        }
        public void onCall() {
            int This = CPU.CPU_Pop32();
            int lpDestRect = CPU.CPU_Pop32();
            int lpDDSurfaceDesc = CPU.CPU_Pop32();
            int dwFlags = CPU.CPU_Pop32();
            int hEvent = CPU.CPU_Pop32();
            if ((dwFlags & DDLOCK_EVENT)!=0) {
                Console.out(getName()+" flag DDLOCK_EVENT not implemented yet ");
                notImplemented();
            }
            if ((dwFlags & DDLOCK_NOSYSLOCK)!=0) {
                Console.out(getName()+" flag DDLOCK_NOSYSLOCK not implemented yet ");
                notImplemented();
            }
            if ((dwFlags & DDLOCK_NOOVERWRITE)!=0) {
                Console.out(getName()+" flag DDLOCK_NOOVERWRITE not implemented yet ");
                notImplemented();
            }
            if ((dwFlags & DDLOCK_DISCARDCONTENTS)!=0) {
                Console.out(getName()+" flag DDLOCK_DISCARDCONTENTS not implemented yet ");
                notImplemented();
            }
            if (lpDDSurfaceDesc == 0) {
                CPU_Regs.reg_eax.dword = Error.E_POINTER;
                return;
            }
            int address = getData(This, OFFSET_MEMORY);
            Memory.mem_memcpy(lpDDSurfaceDesc, This+OFFSET_IUNKNOWN+OFFSET_DESC, DDSurfaceDesc.SIZE);
            Memory.mem_writed(lpDDSurfaceDesc+0x24, address);
            CPU_Regs.reg_eax.dword = Error.S_OK;
        }
    };

    // HRESULT ReleaseDC(this, HDC hDC)
    static private Callback.Handler ReleaseDC = new HandlerBase() {
        public java.lang.String getName() {
            return "IDirectDrawSurface.ReleaseDC";
        }
        public void onCall() {
            int This = CPU.CPU_Pop32();
            int hDC = CPU.CPU_Pop32();
            notImplemented();
        }
    };

    // HRESULT Restore(this)
    static private Callback.Handler Restore = new HandlerBase() {
        public java.lang.String getName() {
            return "IDirectDrawSurface.Restore";
        }
        public void onCall() {
            int This = CPU.CPU_Pop32();
            notImplemented();
        }
    };

    // HRESULT SetClipper(this, LPDIRECTDRAWCLIPPER lpDDClipper)
    static private Callback.Handler SetClipper = new HandlerBase() {
        public java.lang.String getName() {
            return "IDirectDrawSurface.SetClipper";
        }
        public void onCall() {
            int This = CPU.CPU_Pop32();
            int LPDIRECTDRAWCLIPPER = CPU.CPU_Pop32();
            notImplemented();
        }
    };

    // HRESULT SetColorKey(this, DWORD dwFlags, LPDDCOLORKEY lpDDColorKey)
    static private Callback.Handler SetColorKey = new HandlerBase() {
        public java.lang.String getName() {
            return "IDirectDrawSurface.SetColorKey";
        }
        public void onCall() {
            int This = CPU.CPU_Pop32();
            int dwFlags = CPU.CPU_Pop32();
            int lpDDColorKey = CPU.CPU_Pop32();
            notImplemented();
        }
    };

    // HRESULT SetOverlayPosition(this, LONG lX, LONG lY)
    static private Callback.Handler SetOverlayPosition = new HandlerBase() {
        public java.lang.String getName() {
            return "IDirectDrawSurface.SetOverlayPosition";
        }
        public void onCall() {
            int This = CPU.CPU_Pop32();
            int lX = CPU.CPU_Pop32();
            int lY = CPU.CPU_Pop32();
            notImplemented();
        }
    };

    // HRESULT SetPalette(this, LPDIRECTDRAWPALETTE lpDDPalette)
    static private Callback.Handler SetPalette = new HandlerBase() {
        public java.lang.String getName() {
            return "IDirectDrawSurface.SetPalette";
        }
        public void onCall() {
            int This = CPU.CPU_Pop32();
            int lpDDPalette = CPU.CPU_Pop32();
            if (lpDDPalette == 0) {
                CPU_Regs.reg_eax.dword = Error.E_POINTER;
            } else {
                // :TODO: not sure if I should copy the palette or reference it
                AddRef(lpDDPalette); // :TODO: this will be leaked
                Memory.mem_writed(This+OFFSET_IUNKNOWN+OFFSET_PALETTE, lpDDPalette);
                CPU_Regs.reg_eax.dword = Error.S_OK;
            }
        }
    };

    // HRESULT Unlock(this, LPVOID lpSurfaceData)
    static private Callback.Handler Unlock = new HandlerBase() {
        public java.lang.String getName() {
            return "IDirectDrawSurface.Unlock";
        }
        public void onCall() {
            int This = CPU.CPU_Pop32();
            int lpSurfaceData = CPU.CPU_Pop32();
            CPU_Regs.reg_eax.dword = Error.S_OK;
        }
    };

    // HRESULT UpdateOverlay(this, LPRECT lpSrcRect, LPDIRECTDRAWSURFACE lpDDDestSurface, LPRECT lpDestRect, DWORD dwFlags, LPDDOVERLAYFX lpDDOverlayFx)
    static private Callback.Handler UpdateOverlay = new HandlerBase() {
        public java.lang.String getName() {
            return "IDirectDrawSurface.UpdateOverlay";
        }
        public void onCall() {
            int This = CPU.CPU_Pop32();
            int lpSrcRect = CPU.CPU_Pop32();
            int lpDDDestSurface = CPU.CPU_Pop32();
            int lpDestRect = CPU.CPU_Pop32();
            int dwFlags = CPU.CPU_Pop32();
            int lpDDOverlayFx = CPU.CPU_Pop32();
            notImplemented();
        }
    };

    // HRESULT UpdateOverlayDisplay(this, DWORD dwFlags)
    static private Callback.Handler UpdateOverlayDisplay = new HandlerBase() {
        public java.lang.String getName() {
            return "IDirectDrawSurface.UpdateOverlayDisplay";
        }
        public void onCall() {
            int This = CPU.CPU_Pop32();
            int dwFlags = CPU.CPU_Pop32();
            notImplemented();
        }
    };

    // HRESULT UpdateOverlayZOrder(this, DWORD dwFlags, LPDIRECTDRAWSURFACE lpDDSReference)
    static private Callback.Handler UpdateOverlayZOrder = new HandlerBase() {
        public java.lang.String getName() {
            return "IDirectDrawSurface.UpdateOverlayZOrder";
        }
        public void onCall() {
            int This = CPU.CPU_Pop32();
            int dwFlags = CPU.CPU_Pop32();
            int lpDDSReference = CPU.CPU_Pop32();
            notImplemented();
        }
    };
}
