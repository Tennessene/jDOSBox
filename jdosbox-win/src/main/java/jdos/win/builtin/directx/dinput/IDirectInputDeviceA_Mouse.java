package jdos.win.builtin.directx.dinput;

import jdos.cpu.CPU;
import jdos.cpu.CPU_Regs;
import jdos.cpu.Callback;
import jdos.hardware.Memory;
import jdos.win.builtin.HandlerBase;
import jdos.win.builtin.directx.ddraw.IUnknown;
import jdos.win.utils.Error;

public class IDirectInputDeviceA_Mouse extends IUnknown {
    static final int VTABLE_SIZE = 15;

    static final int OFFSET_FLAGS = 0;
    static final int DATA_SIZE = 4;


    private static int createVTable() {
        int address = allocateVTable("IDirectInputDeviceA_Mouse", VTABLE_SIZE);
        addIDirectSound(address);
        return address;
    }

    static void addIDirectSound(int address) {
        address = addIUnknown(address);
        address = add(address, GetCapabilities);
        address = add(address, EnumObjects);
        address = add(address, GetProperty);
        address = add(address, SetProperty);
        address = add(address, Acquire);
        address = add(address, Unacquire);
        address = add(address, GetDeviceState);
        address = add(address, GetDeviceData);
        address = add(address, SetDataFormat);
        address = add(address, SetEventNotification);
        address = add(address, SetCooperativeLevel);
        address = add(address, GetObjectInfo);
        address = add(address, GetDeviceInfo);
        address = add(address, RunControlPanel);
        address = add(address, Initialize);
    }

    public static int create() {
        return create("IDirectInputDeviceA_Mouse", 0);
    }

    public static int create(String name, int flags) {
        int vtable = getVTable(name);
        if (vtable == 0)
            vtable = createVTable();
        int address = allocate(vtable, DATA_SIZE, 0);
        setData(address, OFFSET_FLAGS, flags);
        return address;
    }

    // HRESULT GetCapabilities(this, LPDIDEVCAPS lpDIDevCaps)
    static private final Callback.Handler GetCapabilities = new HandlerBase() {
        public java.lang.String getName() {
            return "IDirectInputDeviceA_Mouse.GetCapabilities";
        }
        public void onCall() {
            int This = CPU.CPU_Pop32();
            int lpDIDevCaps = CPU.CPU_Pop32();
            notImplemented();
        }
    };

    // HRESULT EnumObjects(this, LPDIENUMDEVICEOBJECTSCALLBACKA lpCallback, LPVOID pvRef, DWORD dwFlags)
    static private final Callback.Handler EnumObjects = new HandlerBase() {
        public java.lang.String getName() {
            return "IDirectInputDeviceA_Mouse.EnumObjects";
        }
        public void onCall() {
            int This = CPU.CPU_Pop32();
            int lpCallback = CPU.CPU_Pop32();
            int pvRef = CPU.CPU_Pop32();
            int dwFlags = CPU.CPU_Pop32();
            notImplemented();
        }
    };

    // HRESULT GetProperty(this, REFGUID rguidProp, LPDIPROPHEADER pdiph)
    static private final Callback.Handler GetProperty = new HandlerBase() {
        public java.lang.String getName() {
            return "IDirectInputDeviceA_Mouse.GetProperty";
        }
        public void onCall() {
            int This = CPU.CPU_Pop32();
            int rguidProp = CPU.CPU_Pop32();
            int pdiph = CPU.CPU_Pop32();
            notImplemented();
        }
    };

    // HRESULT SetProperty(this, REFGUID rguidProp, LPCDIPROPHEADER pdiph)
    static private final Callback.Handler SetProperty = new HandlerBase() {
        public java.lang.String getName() {
            return "IDirectInputDeviceA_Mouse.SetProperty";
        }
        public void onCall() {
            int This = CPU.CPU_Pop32();
            int rguidProp = CPU.CPU_Pop32();
            int pdiph = CPU.CPU_Pop32();
            notImplemented();
        }
    };

    // HRESULT Acquire(this)
    static private final Callback.Handler Acquire = new HandlerBase() {
        public java.lang.String getName() {
            return "IDirectInputDeviceA_Mouse.Acquire";
        }
        public void onCall() {
            int This = CPU.CPU_Pop32();
            CPU_Regs.reg_eax.dword = Error.S_OK;
        }
    };

    // HRESULT Unacquire(this)
    static private final Callback.Handler Unacquire = new HandlerBase() {
        public java.lang.String getName() {
            return "IDirectInputDeviceA_Mouse.Unacquire";
        }
        public void onCall() {
            int This = CPU.CPU_Pop32();
            CPU_Regs.reg_eax.dword = Error.S_OK;
        }
    };

    // HRESULT GetDeviceState(this, DWORD cbData, LPVOID lpvData)
    static private final Callback.Handler GetDeviceState = new HandlerBase() {
        public java.lang.String getName() {
            return "IDirectInputDeviceA_Mouse.GetDeviceState";
        }
        public void onCall() {
            int This = CPU.CPU_Pop32();
            int cbData = CPU.CPU_Pop32();
            int lpvData = CPU.CPU_Pop32();
            //if (cbData == 16) { // DIMOUSESTATE
                Memory.mem_zero(lpvData, cbData);
            //}
            CPU_Regs.reg_eax.dword = Error.S_OK;
        }
    };

    // HRESULT GetDeviceData(this, DWORD cbObjectData, LPDIDEVICEOBJECTDATA rgdod, LPDWORD pdwInOut, DWORD dwFlags)
    static private final Callback.Handler GetDeviceData = new HandlerBase() {
        public java.lang.String getName() {
            return "IDirectInputDeviceA_Mouse.GetDeviceData";
        }
        public void onCall() {
            int This = CPU.CPU_Pop32();
            int cbObjectData = CPU.CPU_Pop32();
            int rgdod = CPU.CPU_Pop32();
            int pdwInOut = CPU.CPU_Pop32();
            int dwFlags = CPU.CPU_Pop32();
            notImplemented();
        }
    };

    // HRESULT SetDataFormat(this, LPCDIDATAFORMAT lpdf)
    static private final Callback.Handler SetDataFormat = new HandlerBase() {
        public java.lang.String getName() {
            return "IDirectInputDeviceA_Mouse.SetDataFormat";
        }
        public void onCall() {
            int This = CPU.CPU_Pop32();
            int lpdf = CPU.CPU_Pop32();
            DIDataFormat dataFormat = new DIDataFormat(lpdf);
            CPU_Regs.reg_eax.dword = Error.S_OK;
        }
    };

    // HRESULT SetEventNotification(this, HANDLE hEvent)
    static private final Callback.Handler SetEventNotification = new HandlerBase() {
        public java.lang.String getName() {
            return "IDirectInputDeviceA_Mouse.SetEventNotification";
        }
        public void onCall() {
            int This = CPU.CPU_Pop32();
            int hEvent = CPU.CPU_Pop32();
            notImplemented();
        }
    };

    // HRESULT SetCooperativeLevel(this, HWND hwnd, DWORD dwFlags)
    static private final Callback.Handler SetCooperativeLevel = new HandlerBase() {
        public java.lang.String getName() {
            return "IDirectInputDeviceA_Mouse.SetCooperativeLevel";
        }
        public void onCall() {
            int This = CPU.CPU_Pop32();
            int hwnd = CPU.CPU_Pop32();
            int dwFlags = CPU.CPU_Pop32();
            CPU_Regs.reg_eax.dword = Error.S_OK;
        }
    };

    // HRESULT GetObjectInfo(this, LPDIDEVICEOBJECTINSTANCEA pdidoi, DWORD dwObj, DWORD dwHow)
    static private final Callback.Handler GetObjectInfo = new HandlerBase() {
        public java.lang.String getName() {
            return "IDirectInputDeviceA_Mouse.GetObjectInfo";
        }
        public void onCall() {
            int This = CPU.CPU_Pop32();
            int pdidoi = CPU.CPU_Pop32();
            int dwObj = CPU.CPU_Pop32();
            int dwHow = CPU.CPU_Pop32();
            notImplemented();
        }
    };

    // HRESULT GetDeviceInfo(this, LPDIDEVICEINSTANCEA pdidi)
    static private final Callback.Handler GetDeviceInfo = new HandlerBase() {
        public java.lang.String getName() {
            return "IDirectInputDeviceA_Mouse.GetDeviceInfo";
        }
        public void onCall() {
            int This = CPU.CPU_Pop32();
            int pdidi = CPU.CPU_Pop32();
            notImplemented();
        }
    };

    // HRESULT RunControlPanel(this, HWND hwndOwner, DWORD dwFlags)
    static private final Callback.Handler RunControlPanel = new HandlerBase() {
        public java.lang.String getName() {
            return "IDirectInputDeviceA_Mouse.RunControlPanel";
        }
        public void onCall() {
            int This = CPU.CPU_Pop32();
            int hwndOwner = CPU.CPU_Pop32();
            int dwFlags = CPU.CPU_Pop32();
            notImplemented();
        }
    };

    // HRESULT Initialize(this, HINSTANCE hinst, DWORD dwVersion, REFGUID rguid)
    static private final Callback.Handler Initialize = new HandlerBase() {
        public java.lang.String getName() {
            return "IDirectInputDeviceA_Mouse.Initialize";
        }
        public void onCall() {
            int This = CPU.CPU_Pop32();
            int hinst = CPU.CPU_Pop32();
            int dwVersion = CPU.CPU_Pop32();
            int rguid = CPU.CPU_Pop32();
            notImplemented();
        }
    };
}
