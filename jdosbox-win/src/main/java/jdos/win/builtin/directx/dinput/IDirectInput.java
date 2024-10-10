package jdos.win.builtin.directx.dinput;

import jdos.cpu.CPU;
import jdos.cpu.CPU_Regs;
import jdos.cpu.Callback;
import jdos.hardware.Memory;
import jdos.win.builtin.HandlerBase;
import jdos.win.builtin.directx.Guid;
import jdos.win.builtin.directx.ddraw.IUnknown;

public class IDirectInput extends IUnknown {
    static final int VTABLE_SIZE = 5;

    static int OFFSET_FLAGS = 0;
    static final int DATA_SIZE = 4;

    static Guid GUID_SysMouse = new Guid(0x6F1D2B60,0xD5A0,0x11CF,0xBF,0xC7,0x44,0x45,0x53,0x54,0x00,0x00);
    static Guid GUID_SysKeyboard = new Guid(0x6F1D2B61,0xD5A0,0x11CF,0xBF,0xC7,0x44,0x45,0x53,0x54,0x00,0x00);
    static Guid GUID_Joystick = new Guid(0x6F1D2B70,0xD5A0,0x11CF,0xBF,0xC7,0x44,0x45,0x53,0x54,0x00,0x00);
    static Guid GUID_SysMouseEm = new Guid(0x6F1D2B80,0xD5A0,0x11CF,0xBF,0xC7,0x44,0x45,0x53,0x54,0x00,0x00);
    static Guid GUID_SysMouseEm2 = new Guid(0x6F1D2B81,0xD5A0,0x11CF,0xBF,0xC7,0x44,0x45,0x53,0x54,0x00,0x00);
    static Guid GUID_SysKeyboardEm = new Guid(0x6F1D2B82,0xD5A0,0x11CF,0xBF,0xC7,0x44,0x45,0x53,0x54,0x00,0x00);
    static Guid GUID_SysKeyboardEm2 = new Guid(0x6F1D2B83,0xD5A0,0x11CF,0xBF,0xC7,0x44,0x45,0x53,0x54,0x00,0x00);
    
    private static int createVTable() {
        int address = allocateVTable("IDirectInput", VTABLE_SIZE);
        addIDirectSound(address);
        return address;
    }

    static int addIDirectSound(int address) {
        address = addIUnknown(address);
        address = add(address, CreateDevice);
        address = add(address, EnumDevices);
        address = add(address, GetDeviceStatus);
        address = add(address, RunControlPanel);
        address = add(address, Initialize);
        return address;
    }

    public static int create() {
        return create("IDirectInput", 0);
    }

    public static int create(String name, int flags) {
        int vtable = getVTable(name);
        if (vtable == 0)
            vtable = createVTable();
        int address = allocate(vtable, DATA_SIZE, 0);
        setData(address, OFFSET_FLAGS, flags);
        return address;
    }

    // HRESULT CreateDevice(this, REFGUID rguid, LPDIRECTINPUTDEVICEA * lplpDirectInputDevice, LPUNKNOWN)
    static private Callback.Handler CreateDevice = new HandlerBase() {
        public java.lang.String getName() {
            return "IDirectInput.CreateDevice";
        }
        public void onCall() {
            int This = CPU.CPU_Pop32();
            int rguid = CPU.CPU_Pop32();
            int lplpDirectInputDevice = CPU.CPU_Pop32();
            int pUnkOuter = CPU.CPU_Pop32();
            if (GUID_SysMouse.IsEqual(rguid))
                Memory.mem_writed(lplpDirectInputDevice, IDirectInputDeviceA_Mouse.create());
            else
                notImplemented();
            CPU_Regs.reg_eax.dword = jdos.win.utils.Error.S_OK;
        }
    };

    // HRESULT EnumDevices(this, DWORD dwDevType, LPDIENUMDEVICESCALLBACKA lpCallback, LPVOID pvRef, DWORD dwFlags)
    static private Callback.Handler EnumDevices = new HandlerBase() {
        public java.lang.String getName() {
            return "IDirectInput.EnumDevices";
        }
        public void onCall() {
            int This = CPU.CPU_Pop32();
            int dwDevType = CPU.CPU_Pop32();
            int lpCallback = CPU.CPU_Pop32();
            int pvRef = CPU.CPU_Pop32();
            int dwFlags = CPU.CPU_Pop32();
            notImplemented();
        }
    };

    // HRESULT GetDeviceStatus(this, REFGUID rguid)
    static private Callback.Handler GetDeviceStatus = new HandlerBase() {
        public java.lang.String getName() {
            return "IDirectInput.GetDeviceStatus";
        }
        public void onCall() {
            int This = CPU.CPU_Pop32();
            int rguid = CPU.CPU_Pop32();
            notImplemented();
        }
    };

    // HRESULT RunControlPanel(this, HWND hwndOwner, DWORD dwFlags)
    static private Callback.Handler RunControlPanel = new HandlerBase() {
        public java.lang.String getName() {
            return "IDirectInput.RunControlPanel";
        }
        public void onCall() {
            int This = CPU.CPU_Pop32();
            int hwndOwner = CPU.CPU_Pop32();
            int dwFlags = CPU.CPU_Pop32();
            notImplemented();
        }
    };

    // HRESULT Initialize(this, HINSTANCE hinst, DWORD dwVersion)
    static private Callback.Handler Initialize = new HandlerBase() {
        public java.lang.String getName() {
            return "IDirectInput.Initialize";
        }
        public void onCall() {
            int This = CPU.CPU_Pop32();
            int hinst = CPU.CPU_Pop32();
            int dwVersion = CPU.CPU_Pop32();
            notImplemented();
        }
    };
}
