package jdos.win.builtin.directx.ddraw;

import jdos.cpu.CPU;
import jdos.cpu.Callback;
import jdos.win.builtin.HandlerBase;

public class IDirectDrawSurface7 extends IUnknown {
    public static int create(int pDirectDraw, int pDesc) {
        int vtable = getVTable("IDirectDrawSurface7");
        if (vtable == 0)
            createVTable();
        return IDirectDrawSurface.create("IDirectDrawSurface7", pDirectDraw, pDesc, IDirectDrawSurface.FLAGS_CAPS2 | IDirectDrawSurface.FLAGS_DESC2);
    }

    static private int createVTable() {
        int address = allocateVTable("IDirectDrawSurface7", IDirectDrawSurface.VTABLE_COUNT+13);
        int result = address;
        address = IDirectDrawSurface.addIDirectDrawSurface(address);

        /* added in v2 */
        address = add(address, GetDDInterface);
        address = add(address, PageLock);
        address = add(address, PageUnlock);
        /* added in v3 */
        address = add(address, SetSurfaceDesc);
        /* added in v4 */
        address = add(address, SetPrivateData);
        address = add(address, GetPrivateData);
        address = add(address, SetSurfaceDesc);
        address = add(address, GetUniquenessValue);
        address = add(address, ChangeUniquenessValue);
        /* added in v7 */
        address = add(address, SetPriority);
        address = add(address, GetPriority);
        address = add(address, SetLOD);
        address = add(address, GetLOD);
        return result;
    }

    /* added in v2 */
    // HRESULT GetDDInterface(this, LPVOID *lplpDD)
    static private Callback.Handler GetDDInterface = new HandlerBase() {
        public java.lang.String getName() {
            return "IDirectDrawSurface7.GetDDInterface";
        }
        public void onCall() {
            int This = CPU.CPU_Pop32();
            int lplpDD = CPU.CPU_Pop32();
            notImplemented();
        }
    };
    
    // HRESULT PageLock(this, DWORD dwFlags)
    static private Callback.Handler PageLock = new HandlerBase() {
        public java.lang.String getName() {
            return "IDirectDrawSurface7.PageLock";
        }
        public void onCall() {
            int This = CPU.CPU_Pop32();
            int dwFlags = CPU.CPU_Pop32();
            notImplemented();
        }
    };
    
    // HRESULT PageUnlock(this, DWORD dwFlags)
    static private Callback.Handler PageUnlock = new HandlerBase() {
        public java.lang.String getName() {
            return "IDirectDrawSurface7.PageUnlock";
        }
        public void onCall() {
            int This = CPU.CPU_Pop32();
            int dwFlags = CPU.CPU_Pop32();
            notImplemented();
        }
    };
    
    /* added in v3 */
    // HRESULT SetSurfaceDesc(this, LPDDSURFACEDESC2 lpDDSD, DWORD dwFlags)
    static private Callback.Handler SetSurfaceDesc = new HandlerBase() {
        public java.lang.String getName() {
            return "IDirectDrawSurface7.SetSurfaceDesc";
        }
        public void onCall() {
            int This = CPU.CPU_Pop32();
            int lpDDSD = CPU.CPU_Pop32();
            int dwFlags = CPU.CPU_Pop32();
            notImplemented();
        }
    };
    
    /* added in v4 */
    // HRESULT SetPrivateData(this, REFGUID tag, LPVOID pData, DWORD cbSize, DWORD dwFlags)
    static private Callback.Handler SetPrivateData = new HandlerBase() {
        public java.lang.String getName() {
            return "IDirectDrawSurface7.SetPrivateData";
        }
        public void onCall() {
            int This = CPU.CPU_Pop32();
            int tag = CPU.CPU_Pop32();
            int pData = CPU.CPU_Pop32();
            int cbSize = CPU.CPU_Pop32();
            int dwFlags = CPU.CPU_Pop32();
            notImplemented();
        }
    };
    
    // HRESULT GetPrivateData(this, REFGUID tag, LPVOID pBuffer, LPDWORD pcbBufferSize)
    static private Callback.Handler GetPrivateData = new HandlerBase() {
        public java.lang.String getName() {
            return "IDirectDrawSurface7.GetPrivateData";
        }
        public void onCall() {
            int This = CPU.CPU_Pop32();
            int tag = CPU.CPU_Pop32();
            int pBuffer = CPU.CPU_Pop32();
            int pcbBufferSize = CPU.CPU_Pop32();
            notImplemented();
        }
    };
    
    // HRESULT FreePrivateData(this, REFGUID tag)
    static private Callback.Handler FreePrivateData = new HandlerBase() {
        public java.lang.String getName() {
            return "IDirectDrawSurface7.FreePrivateData";
        }
        public void onCall() {
            int This = CPU.CPU_Pop32();
            int tag = CPU.CPU_Pop32();
            notImplemented();
        }
    };
    
    // HRESULT GetUniquenessValue(this, LPDWORD pValue)
    static private Callback.Handler GetUniquenessValue = new HandlerBase() {
        public java.lang.String getName() {
            return "IDirectDrawSurface7.GetUniquenessValue";
        }
        public void onCall() {
            int This = CPU.CPU_Pop32();
            int pValue = CPU.CPU_Pop32();
            notImplemented();
        }
    };
    
    // HRESULT ChangeUniquenessValue(this)
    static private Callback.Handler ChangeUniquenessValue = new HandlerBase() {
        public java.lang.String getName() {
            return "IDirectDrawSurface7.ChangeUniquenessValue";
        }
        public void onCall() {
            int This = CPU.CPU_Pop32();
            notImplemented();
        }
    };
    
    /* added in v7 */
    // HRESULT SetPriority(this, DWORD prio)
    static private Callback.Handler SetPriority = new HandlerBase() {
        public java.lang.String getName() {
            return "IDirectDrawSurface7.SetPriority";
        }
        public void onCall() {
            int This = CPU.CPU_Pop32();
            int prio = CPU.CPU_Pop32();
            notImplemented();
        }
    };
    
    // HRESULT GetPriority(this, LPDWORD prio)
    static private Callback.Handler GetPriority = new HandlerBase() {
        public java.lang.String getName() {
            return "IDirectDrawSurface7.GetPriority";
        }
        public void onCall() {
            int This = CPU.CPU_Pop32();
            int prio = CPU.CPU_Pop32();
            notImplemented();
        }
    };
    
    // HRESULT SetLOD(this, DWORD lod)
    static private Callback.Handler SetLOD = new HandlerBase() {
        public java.lang.String getName() {
            return "IDirectDrawSurface7.SetLOD";
        }
        public void onCall() {
            int This = CPU.CPU_Pop32();
            int lod = CPU.CPU_Pop32();
            notImplemented();
        }
    };
    
    // HRESULT GetLOD(this, LPDWORD lod)
    static private Callback.Handler GetLOD = new HandlerBase() {
        public java.lang.String getName() {
            return "IDirectDrawSurface7.GetLOD";
        }
        public void onCall() {
            int This = CPU.CPU_Pop32();
            int lod = CPU.CPU_Pop32();
            notImplemented();
        }
    };
}
