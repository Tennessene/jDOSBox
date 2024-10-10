package jdos.win.builtin.directx.ddraw;

import jdos.cpu.CPU;
import jdos.cpu.CPU_Regs;
import jdos.cpu.Callback;
import jdos.hardware.Memory;
import jdos.win.Win;
import jdos.win.builtin.HandlerBase;
import jdos.win.system.WinSystem;
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

    static final public int OFFSET_COLOR_DATA = 0;

    public static int createDefault() {
        int vtable = getVTable("IDirectDrawPalette");
        if (vtable == 0)
            vtable = createVTable();
        int result = allocate(vtable, 256*4, 0);
        int[] table = new int[256];
        table[0]=0x000000;
        table[1]=0x800000;
        table[2]=0x008000;
        table[3]=0x808000;
        table[4]=0x000080;
        table[5]=0x800080;
        table[6]=0x008080;
        table[7]=0xc0c0c0;
        table[8]=0xc0dcc0;
        table[9]=0xa6caf0;
        int pos = 10;
        for (int r = 0;r<256;r+=51) {
            for (int g = 0;r<256;r+=51) {
                for (int b = 0;r<256;r+=51) {
                    table[pos++] = (r<<16)|(g<<8)|b;
                }
            }
        }
        table[246]=0xfffbf0;
        table[247]=0xa0a0a4;
        table[248]=0x808080;
        table[249]=0xff0000;
        table[250]=0x00ff00;
        table[251]=0xffff00;
        table[252]=0x0000ff;
        table[253]=0xff00ff;
        table[254]=0x00ffff;
        table[255]=0xffffff;
        for (int i=0;i<table.length;i++)
            Memory.mem_writed(result + OFFSET_DATA_START + i*4, table[i]);
        return result;
    }

    public static int create(int flags, int lpColorTable) {
        int vtable = getVTable("IDirectDrawPalette");
        if (vtable == 0)
            vtable = createVTable();
        int result = allocate(vtable, 256*4, 0);
        //if ((flags & DDPCAPS_INITIALIZE)!=0) {
            if ((flags & DDPCAPS_8BIT)==0) {
                Win.panic("DirectDraw create palette without DDPCAPS_8BIT");
            }
            Memory.mem_memcpy(result+ OFFSET_DATA_START, lpColorTable, 256*4);
        //}
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
        public String getName() {
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
        public String getName() {
            return "IDirectDrawPalette.GetEntries";
        }
        public void onCall() {
            int This = CPU.CPU_Pop32();
            int dwFlags = CPU.CPU_Pop32();
            int dwBase = CPU.CPU_Pop32();
            int dwNumEntries = CPU.CPU_Pop32();
            int lpEntries = CPU.CPU_Pop32();
            for (int i=dwBase;i<dwNumEntries;i++) {
                Memory.mem_writed(lpEntries+i*4, getData(This, OFFSET_COLOR_DATA+i*4));
            }
            CPU_Regs.reg_eax.dword = Error.S_OK;
        }
    };

    // HRESULT Initialize(this, LPDIRECTDRAW lpDD, DWORD dwFlags, LPPALETTEENTRY lpDDColorTable)
    static private Callback.Handler Initialize = new HandlerBase() {
        public String getName() {
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
        public String getName() {
            return "IDirectDrawPalette.SetEntries";
        }
        public void onCall() {
            int This = CPU.CPU_Pop32();
            int dwFlags = CPU.CPU_Pop32();
            int dwStartingEntry = CPU.CPU_Pop32();
            int dwCount = CPU.CPU_Pop32();
            int lpEntries = CPU.CPU_Pop32();
            for (int i=0;i<dwCount;i++) {
                int color = Memory.mem_readd(lpEntries + i * 4);
//                if (WinAPI.LOG) {
//                    int oldColor = getData(This, OFFSET_COLOR_DATA+(dwStartingEntry+i)*4);
//                    if (color != oldColor) {
//                        System.out.println(i+". 0x"+ Ptr.toString(oldColor)+" -> 0x"+Ptr.toString(color));
//                    }
//                }
                setData(This, OFFSET_COLOR_DATA+(i+dwStartingEntry)*4, color);
            }
            IDirectDrawSurface.lastPaletteChange = WinSystem.getTickCount();
            CPU_Regs.reg_eax.dword = Error.S_OK;
        }
    };
}
