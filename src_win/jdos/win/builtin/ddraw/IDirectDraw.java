package jdos.win.builtin.ddraw;

import jdos.cpu.CPU;
import jdos.cpu.CPU_Regs;
import jdos.cpu.Callback;
import jdos.gui.Main;
import jdos.hardware.Memory;
import jdos.win.Console;
import jdos.win.builtin.HandlerBase;
import jdos.win.utils.Error;
import jdos.win.utils.WinSystem;

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
            WinSystem.screenPalette = result;
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
            notImplemented();
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
        public java.lang.String getName() {
            return "IDirectDraw.EnumDisplayModes";
        }
        public void onCall() {
            int This = CPU.CPU_Pop32();
            int dwFlags = CPU.CPU_Pop32();
            int lpDDSurfaceDesc = CPU.CPU_Pop32();
            int lpContext = CPU.CPU_Pop32();
            int lpEnumModesCallback = CPU.CPU_Pop32();
            notImplemented();
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
        public java.lang.String getName() {
            return "IDirectDraw.GetCaps";
        }
        public void onCall() {
            int This = CPU.CPU_Pop32();
            int lpDDDriverCaps = CPU.CPU_Pop32();
            int lpDDHELCaps = CPU.CPU_Pop32();
            notImplemented();
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
                Console.out("DDraw.SetCooperativeLevel: unsupported flags: "+Integer.toString(dwFlags, 16));
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
            WinSystem.screenBpp = dwBPP;
            WinSystem.screenHeight = dwHeight;
            WinSystem.screenWidth = dwWidth;
            Main.GFX_SetSize(dwWidth, dwHeight, false, false, false, 32);
            CPU_Regs.reg_eax.dword = Error.S_OK;
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
