package jdos.win.builtin.directx.dinput;

import jdos.cpu.CPU;
import jdos.cpu.Callback;
import jdos.win.builtin.HandlerBase;
import jdos.win.builtin.directx.ddraw.IUnknown;

public class IDirectInputDeviceA extends IUnknown {
    static final int VTABLE_SIZE = 15;

    static int OFFSET_FLAGS = 0;
    static final int DATA_SIZE = 4;


    private static int createVTable() {
        int address = allocateVTable("IDirectInputDeviceA", VTABLE_SIZE);
        addIDirectSound(address);
        return address;
    }

    static int addIDirectSound(int address) {
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
        return address;
    }

    public static int create() {
        return create("IDirectInputDeviceA", 0);
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
    static private Callback.Handler GetCapabilities = new HandlerBase() {
        public java.lang.String getName() {
            return "IDirectInputDeviceA.GetCapabilities";
        }
        public void onCall() {
            int This = CPU.CPU_Pop32();
            int lpDIDevCaps = CPU.CPU_Pop32();
            notImplemented();
        }
    };

    // HRESULT EnumObjects(this, LPDIENUMDEVICEOBJECTSCALLBACKA lpCallback, LPVOID pvRef, DWORD dwFlags)
    static private Callback.Handler EnumObjects = new HandlerBase() {
        public java.lang.String getName() {
            return "IDirectInputDeviceA.EnumObjects";
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
    static private Callback.Handler GetProperty = new HandlerBase() {
        public java.lang.String getName() {
            return "IDirectInputDeviceA.GetProperty";
        }
        public void onCall() {
            int This = CPU.CPU_Pop32();
            int rguidProp = CPU.CPU_Pop32();
            int pdiph = CPU.CPU_Pop32();
            notImplemented();
        }
    };

    // HRESULT SetProperty(this, REFGUID rguidProp, LPCDIPROPHEADER pdiph)
    static private Callback.Handler SetProperty = new HandlerBase() {
        public java.lang.String getName() {
            return "IDirectInputDeviceA.SetProperty";
        }
        public void onCall() {
            int This = CPU.CPU_Pop32();
            int rguidProp = CPU.CPU_Pop32();
            int pdiph = CPU.CPU_Pop32();
            notImplemented();
        }
    };

    // HRESULT Acquire(this)
    static private Callback.Handler Acquire = new HandlerBase() {
        public java.lang.String getName() {
            return "IDirectInputDeviceA.Acquire";
        }
        public void onCall() {
            int This = CPU.CPU_Pop32();
            notImplemented();
        }
    };

    // HRESULT Unacquire(this)
    static private Callback.Handler Unacquire = new HandlerBase() {
        public java.lang.String getName() {
            return "IDirectInputDeviceA.Unacquire";
        }
        public void onCall() {
            int This = CPU.CPU_Pop32();
            notImplemented();
        }
    };

    // HRESULT GetDeviceState(this, DWORD cbData, LPVOID lpvData)
    static private Callback.Handler GetDeviceState = new HandlerBase() {
        public java.lang.String getName() {
            return "IDirectInputDeviceA.GetDeviceState";
        }
        public void onCall() {
            int This = CPU.CPU_Pop32();
            int cbData = CPU.CPU_Pop32();
            int lpvData = CPU.CPU_Pop32();
            notImplemented();
        }
    };

    // HRESULT GetDeviceData(this, DWORD cbObjectData, LPDIDEVICEOBJECTDATA rgdod, LPDWORD pdwInOut, DWORD dwFlags)
    static private Callback.Handler GetDeviceData = new HandlerBase() {
        public java.lang.String getName() {
            return "IDirectInputDeviceA.GetDeviceData";
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
    static private Callback.Handler SetDataFormat = new HandlerBase() {
        public java.lang.String getName() {
            return "IDirectInputDeviceA.SetDataFormat";
        }
        public void onCall() {
            int This = CPU.CPU_Pop32();
            int lpdf = CPU.CPU_Pop32();
            notImplemented();
        }
    };

    // HRESULT SetEventNotification(this, HANDLE hEvent)
    static private Callback.Handler SetEventNotification = new HandlerBase() {
        public java.lang.String getName() {
            return "IDirectInputDeviceA.SetEventNotification";
        }
        public void onCall() {
            int This = CPU.CPU_Pop32();
            int hEvent = CPU.CPU_Pop32();
            notImplemented();
        }
    };

    // HRESULT SetCooperativeLevel(this, HWND hwnd, DWORD dwFlags)
    static private Callback.Handler SetCooperativeLevel = new HandlerBase() {
        public java.lang.String getName() {
            return "IDirectInputDeviceA.SetCooperativeLevel";
        }
        public void onCall() {
            int This = CPU.CPU_Pop32();
            int hwnd = CPU.CPU_Pop32();
            int dwFlags = CPU.CPU_Pop32();
            notImplemented();
        }
    };

    // HRESULT GetObjectInfo(this, LPDIDEVICEOBJECTINSTANCEA pdidoi, DWORD dwObj, DWORD dwHow)
    static private Callback.Handler GetObjectInfo = new HandlerBase() {
        public java.lang.String getName() {
            return "IDirectInputDeviceA.GetObjectInfo";
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
    static private Callback.Handler GetDeviceInfo = new HandlerBase() {
        public java.lang.String getName() {
            return "IDirectInputDeviceA.GetDeviceInfo";
        }
        public void onCall() {
            int This = CPU.CPU_Pop32();
            int pdidi = CPU.CPU_Pop32();
            notImplemented();
        }
    };

    // HRESULT RunControlPanel(this, HWND hwndOwner, DWORD dwFlags)
    static private Callback.Handler RunControlPanel = new HandlerBase() {
        public java.lang.String getName() {
            return "IDirectInputDeviceA.RunControlPanel";
        }
        public void onCall() {
            int This = CPU.CPU_Pop32();
            int hwndOwner = CPU.CPU_Pop32();
            int dwFlags = CPU.CPU_Pop32();
            notImplemented();
        }
    };

    // HRESULT Initialize(this, HINSTANCE hinst, DWORD dwVersion, REFGUID rguid)
    static private Callback.Handler Initialize = new HandlerBase() {
        public java.lang.String getName() {
            return "IDirectInputDeviceA.Initialize";
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
