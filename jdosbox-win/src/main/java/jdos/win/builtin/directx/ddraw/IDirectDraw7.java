package jdos.win.builtin.directx.ddraw;

import jdos.cpu.CPU;
import jdos.cpu.Callback;
import jdos.win.builtin.HandlerBase;

public class IDirectDraw7 extends IUnknown {
    private static int createVTable() {
        int address = allocateVTable("IDirectDraw7", IDirectDraw.VTABLE_SIZE+7);
        int result = address;
        address = IDirectDraw.addIDirectDraw(address, true);

        /* added in v2 */
        address = add(address, GetAvailableVidMem);
        /* added in v4 */
        address = add(address, GetSurfaceFromDC);
        address = add(address, RestoreAllSurfaces);
        address = add(address, TestCooperativeLevel);
        address = add(address, GetDeviceIdentifier);
        /* added in v7 */
        address = add(address, StartModeTest);
        address = add(address, EvaluateMode);
        return result;
    }

    public static int create() {
        int vtable = getVTable("IDirectDraw7");
        if (vtable == 0)
            createVTable();
        return IDirectDraw.create("IDirectDraw7", IDirectDraw.FLAGS_CALLBACK2 | IDirectDraw.FLAGS_DESC2 | IDirectDraw.FLAGS_V7);
    }

    /* added in v2 */
    // HRESULT GetAvailableVidMem(this, LPDDSCAPS2 lpDDCaps, LPDWORD lpdwTotal, LPDWORD lpdwFree)
    static private Callback.Handler GetAvailableVidMem = new HandlerBase() {
        public java.lang.String getName() {
            return "IDirectDraw7.GetAvailableVidMem";
        }
        public void onCall() {
            int This = CPU.CPU_Pop32();
            int lpDDCaps = CPU.CPU_Pop32();
            int lpdwTotal = CPU.CPU_Pop32();
            int lpdwFree = CPU.CPU_Pop32();
            notImplemented();
        }
    };

    /* added in v4 */
    // HRESULT GetSurfaceFromDC(this HDC hdc, LPDIRECTDRAWSURFACE7 *pSurf)
    static private Callback.Handler GetSurfaceFromDC = new HandlerBase() {
        public java.lang.String getName() {
            return "IDirectDraw7.GetSurfaceFromDC";
        }
        public void onCall() {
            int This = CPU.CPU_Pop32();
            int hdc = CPU.CPU_Pop32();
            int pSurf = CPU.CPU_Pop32();
            notImplemented();
        }
    };

    // HRESULT RestoreAllSurfaces(this)
    static private Callback.Handler RestoreAllSurfaces = new HandlerBase() {
        public java.lang.String getName() {
            return "IDirectDraw7.RestoreAllSurfaces";
        }
        public void onCall() {
            int This = CPU.CPU_Pop32();
            notImplemented();
        }
    };

    // HRESULT TestCooperativeLevel(this)
    static private Callback.Handler TestCooperativeLevel = new HandlerBase() {
        public java.lang.String getName() {
            return "IDirectDraw7.TestCooperativeLevel";
        }
        public void onCall() {
            int This = CPU.CPU_Pop32();
            notImplemented();
        }
    };
    // HRESULT GetDeviceIdentifier(this, LPDDDEVICEIDENTIFIER2 pDDDI, DWORD dwFlags)
    static private Callback.Handler GetDeviceIdentifier = new HandlerBase() {
        public java.lang.String getName() {
            return "IDirectDraw7.GetDeviceIdentifier";
        }
        public void onCall() {
            int This = CPU.CPU_Pop32();
            int pDDDI = CPU.CPU_Pop32();
            int dwFlags = CPU.CPU_Pop32();
            notImplemented();
        }
    };

    /* added in v7 */
    // HRESULT StartModeTest(this, LPSIZE pModes, DWORD dwNumModes, DWORD dwFlags)
    static private Callback.Handler StartModeTest = new HandlerBase() {
        public java.lang.String getName() {
            return "IDirectDraw7.StartModeTest";
        }
        public void onCall() {
            int This = CPU.CPU_Pop32();
            int pModes = CPU.CPU_Pop32();
            int dwNumModes = CPU.CPU_Pop32();
            int dwFlags = CPU.CPU_Pop32();
            notImplemented();
        }
    };

    // HRESULT EvaluateMode(this, DWORD dwFlags, DWORD  *pTimeout)
    static private Callback.Handler EvaluateMode = new HandlerBase() {
        public java.lang.String getName() {
            return "IDirectDraw7.EvaluateMode";
        }
        public void onCall() {
            int This = CPU.CPU_Pop32();
            int dwFlags = CPU.CPU_Pop32();
            int pTimeout = CPU.CPU_Pop32();
            notImplemented();
        }
    };
}
