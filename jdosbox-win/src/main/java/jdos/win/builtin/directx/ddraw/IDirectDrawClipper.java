package jdos.win.builtin.directx.ddraw;

import jdos.cpu.CPU;
import jdos.cpu.CPU_Regs;
import jdos.cpu.Callback;
import jdos.hardware.Memory;
import jdos.win.builtin.HandlerBase;
import jdos.win.utils.Error;

public class IDirectDrawClipper extends IUnknown {
    static final int VTABLE_SIZE = 6;

    static final int OFFSET_HWND = 0;
    static final int DATA_SIZE = 4;

    public static int create() {
        int vtable = getVTable("IDirectDrawClipper");
        if (vtable == 0)
            vtable = createVTable();
        int result = allocate(vtable, DATA_SIZE, 0);
        return result;
    }

    static private int createVTable() {
        int address = allocateVTable("IDirectDrawPalette", VTABLE_SIZE);
        int result = address;
        address = addIUnknown(address);

        address = add(address, GetClipList);
        address = add(address, GetHWnd);
        address = add(address, Initialize);
        address = add(address, IsClipListChanged);
        address = add(address, SetClipList);
        address = add(address, SetHWnd);
        return result;
    }
    
    // HRESULT GetClipList(this, LPRECT lpRect, LPRGNDATA lpClipList, LPDWORD lpdwSize)
    static private Callback.Handler GetClipList = new HandlerBase() {
        public java.lang.String getName() {
            return "IDirectDrawClipper.GetClipList";
        }
        public void onCall() {
            int This = CPU.CPU_Pop32();
            int lpRect = CPU.CPU_Pop32();
            int lpClipList = CPU.CPU_Pop32();
            int lpdwSize = CPU.CPU_Pop32();
            notImplemented();
        }
    };

    // HRESULT GetHWnd(this, HWND *lphWnd)
    static private Callback.Handler GetHWnd = new HandlerBase() {
        public java.lang.String getName() {
            return "IDirectDrawClipper.GetHWnd";
        }
        public void onCall() {
            int This = CPU.CPU_Pop32();
            int lphWnd = CPU.CPU_Pop32();
            if (lphWnd == 0) {
                CPU_Regs.reg_eax.dword = Error.E_POINTER;
            } else {
                Memory.mem_writed(lphWnd, getData(This, OFFSET_HWND));
                CPU_Regs.reg_eax.dword = Error.S_OK;
            }
        }
    };

    // HRESULT Initialize(this, LPDIRECTDRAW lpDD, DWORD dwFlags)
    static private Callback.Handler Initialize = new HandlerBase() {
        public java.lang.String getName() {
            return "IDirectDrawClipper.Initialize";
        }
        public void onCall() {
            int This = CPU.CPU_Pop32();
            int lpDD = CPU.CPU_Pop32();
            int dwFlags = CPU.CPU_Pop32();
            notImplemented();
        }
    };

    // HRESULT IsClipListChanged(this, BOOL *lpbChanged)
    static private Callback.Handler IsClipListChanged = new HandlerBase() {
        public java.lang.String getName() {
            return "IDirectDrawClipper.IsClipListChanged";
        }
        public void onCall() {
            int This = CPU.CPU_Pop32();
            int lpbChanged = CPU.CPU_Pop32();
            notImplemented();
        }
    };

    // HRESULT SetClipList(this, LPRGNDATA lpClipList, DWORD dwFlags)
    static private Callback.Handler SetClipList = new HandlerBase() {
        public java.lang.String getName() {
            return "IDirectDrawClipper.SetClipList";
        }
        public void onCall() {
            int This = CPU.CPU_Pop32();
            int lpClipList = CPU.CPU_Pop32();
            int dwFlags = CPU.CPU_Pop32();
            notImplemented();
        }
    };

    // HRESULT SetHWnd(this, DWORD dwFlags, HWND hWnd)
    static private Callback.Handler SetHWnd = new HandlerBase() {
        public java.lang.String getName() {
            return "IDirectDrawClipper.SetHWnd";
        }
        public void onCall() {
            int This = CPU.CPU_Pop32();
            int dwFlags = CPU.CPU_Pop32();
            int hWnd = CPU.CPU_Pop32();
            setData(This, OFFSET_HWND, hWnd);
            CPU_Regs.reg_eax.dword = Error.S_OK;
        }
    };
}
