package jdos.win.builtin.ddraw;

import jdos.cpu.CPU;
import jdos.cpu.CPU_Regs;
import jdos.cpu.Callback;
import jdos.hardware.Memory;
import jdos.win.Win;
import jdos.win.builtin.HandlerBase;
import jdos.win.utils.Error;

public class IDirectDrawPalette extends IUnknown {
    static final private int DDPCAPS_4BIT =                 0x00000001;
    static final private int DDPCAPS_8BITENTRIES =          0x00000002;
    static final private int DDPCAPS_8BIT =                 0x00000004;
    static final private int DDPCAPS_INITIALIZE =           0x00000008;
    static final private int DDPCAPS_PRIMARYSURFACE =       0x00000010;
    static final private int DDPCAPS_PRIMARYSURFACELEFT	=   0x00000020;
    static final private int DDPCAPS_ALLOW256 =             0x00000040;
    static final private int DDPCAPS_VSYNC =                0x00000080;
    static final private int DDPCAPS_1BIT =                 0x00000100;
    static final private int DDPCAPS_2BIT =                 0x00000200;
    static final private int DDPCAPS_ALPHA =                0x00000400;

    public static int create(int flags, int lpColorTable) {
        int vtable = getVTable("IDirectDrawPalette");
        if (vtable == 0)
            vtable = createVTable();
        int result = allocate(vtable, 256*4);
        if ((flags & DDPCAPS_INITIALIZE)!=0) {
            if ((flags & DDPCAPS_8BIT)==0) {
                Win.panic("DirectDraw create palette without DDPCAPS_8BIT");
            }
            Memory.mem_memcpy(result+OFFSET_IUNKNOWN, lpColorTable, 256*4);
        }
        return result;
    }

    static private int createVTable() {
        int address = allocateVTable("IDirectDrawPalette", 4);
        int result = address;
        address = addIUnknown(address);

        address = add(address, GetCaps);
        address = add(address, GetEntries);
        address = add(address, Initialize);
        address = add(address, SetEntries);
        return result;
    }

    // HRESULT GetCaps(this, LPDWORD lpdwCaps)
    static private Callback.Handler GetCaps = new HandlerBase() {
        public java.lang.String getName() {
            return "IDirectDrawPalette.GetCaps";
        }
        public void onCall() {
            int This = CPU.CPU_Pop32();
            int lpdwCaps = CPU.CPU_Pop32();
            if (lpdwCaps == 0)
                CPU_Regs.reg_eax.dword = Error.E_POINTER;
            else {
                Memory.mem_writed(lpdwCaps, DDPCAPS_8BIT|DDPCAPS_ALLOW256);
                CPU_Regs.reg_eax.dword = Error.S_OK;
            }
        }
    };

    // HRESULT GetEntries(this, DWORD dwFlags, DWORD dwBase, DWORD dwNumEntries, LPPALETTEENTRY lpEntries)
    static private Callback.Handler GetEntries = new HandlerBase() {
        public java.lang.String getName() {
            return "IDirectDrawPalette.GetEntries";
        }
        public void onCall() {
            int This = CPU.CPU_Pop32();
            int dwFlags = CPU.CPU_Pop32();
            int dwBase = CPU.CPU_Pop32();
            int dwNumEntries = CPU.CPU_Pop32();
            int lpEntries = CPU.CPU_Pop32();
            notImplemented();
        }
    };

    // HRESULT Initialize(this, LPDIRECTDRAW lpDD, DWORD dwFlags, LPPALETTEENTRY lpDDColorTable)
    static private Callback.Handler Initialize = new HandlerBase() {
        public java.lang.String getName() {
            return "IDirectDrawPalette.Initialize";
        }
        public void onCall() {
            int This = CPU.CPU_Pop32();
            int lpDD = CPU.CPU_Pop32();
            int dwFlags = CPU.CPU_Pop32();
            int lpDDColorTable = CPU.CPU_Pop32();
            notImplemented();
        }
    };

    // HRESULT SetEntries(this, DWORD dwFlags, DWORD dwStartingEntry, DWORD dwCount, LPPALETTEENTRY lpEntries)
    static private Callback.Handler SetEntries = new HandlerBase() {
        public java.lang.String getName() {
            return "IDirectDrawPalette.SetEntries";
        }
        public void onCall() {
            int This = CPU.CPU_Pop32();
            int dwFlags = CPU.CPU_Pop32();
            int dwStartingEntry = CPU.CPU_Pop32();
            int dwCount = CPU.CPU_Pop32();
            int lpEntries = CPU.CPU_Pop32();
            notImplemented();
        }
    };
}
