package jdos.win.builtin.ddraw;

import jdos.cpu.CPU;
import jdos.cpu.CPU_Regs;
import jdos.cpu.Callback;
import jdos.hardware.Memory;
import jdos.win.builtin.HandlerBase;
import jdos.win.kernel.WinCallback;
import jdos.win.utils.Error;
import jdos.win.utils.WinSystem;

import java.util.Hashtable;

public class IUnknown {
    // static private final int OFFSET_VTABLE = 0;
    static private final int OFFSET_REF = 4;
    static private final int OFFSET_CLEANUP = 8;
    static public final int OFFSET_DATA_START = 12;

    static private Hashtable vtables = new Hashtable();
    static private Hashtable names = new Hashtable();

    static protected int getVTable(String name) {
        Integer result =  (Integer)vtables.get(name);
        if (result == null)
            return 0;
        return result.intValue();
    }

    static protected int getData(int This, int offset) {
        return Memory.mem_readd(This+ OFFSET_DATA_START +offset);
    }

    static protected void setData(int This, int offset, int data) {
        Memory.mem_writed(This+ OFFSET_DATA_START +offset, data);
    }

    private static void setRefCount(int address, int i) {
        Memory.mem_writed(address + OFFSET_REF, i);
    }

    public static int getRefCount(int address) {
        return Memory.mem_readd(address+OFFSET_REF);
    }

    public static int getVTable(int address) {
        return Memory.mem_readd(address);
    }

    static protected int add(int address, Callback.Handler handler) {
        int cb = WinCallback.addCallback(handler);
        Memory.mem_writed(address, WinSystem.getCurrentProcess().loader.registerFunction(cb));
        return address+4;
    }

    static protected int allocateVTable(String name,  int functions) {
        int result = WinSystem.getCurrentProcess().heap.alloc((functions+3)*4, false);
        vtables.put(name, new Integer(result));
        names.put(new Integer(result), name);
        return result;
    }

    static protected int allocate(int vtable, int extra, int cleanup) {
        int result = WinSystem.getCurrentProcess().heap.alloc(OFFSET_DATA_START+extra, false);
        Memory.mem_zero(result, OFFSET_DATA_START+extra);
        Memory.mem_writed(result, vtable);
        Memory.mem_writed(result+OFFSET_CLEANUP, cleanup);
        setRefCount(result, 1);
        return result;
    }

    static protected int addIUnknown(int address) {
        address = add(address, QueryInterface);
        address = add(address, AddRef);
        address = add(address, Release);
        return address;
    }

    // HRESULT QueryInterface(this, REFIID riid, void** ppvObject)
    static private Callback.Handler QueryInterface = new HandlerBase() {
        public java.lang.String getName() {
            return "IUnknown.QueryInterface";
        }
        public void onCall() {
            int This = CPU.CPU_Pop32();
            int riid = CPU.CPU_Pop32();
            int ppvObject = CPU.CPU_Pop32();
            if (ppvObject == 0)
                CPU_Regs.reg_eax.dword = Error.E_POINTER;
            else
                CPU_Regs.reg_eax.dword = Error.E_NOINTERFACE;
        }
    };

    static public int AddRef(int This) {
        int refCount = getRefCount(This);
        refCount++;
        setRefCount(This, refCount);
        return refCount;
    }

    // ULONG AddRef(this)
    static private Callback.Handler AddRef = new HandlerBase() {
        public java.lang.String getName() {
            return "IUnknown.AddRef";
        }
        public void onCall() {
            int This = CPU.CPU_Pop32();
            CPU_Regs.reg_eax.dword = AddRef(This);
        }
    };

    static public int Release(int This) {
        System.out.println(names.get(new Integer(getVTable(This)))+".Release");
        int refCount = getRefCount(This);
        refCount--;
        setRefCount(This, refCount);
        if (refCount == 0) {
            System.out.println("    Freed");
            int cb = Memory.mem_readd(This+OFFSET_CLEANUP);
            if (cb != 0) {
                CPU.CPU_Push32(This);
                Callback.CallBack_Handlers[cb].call();
            }
            WinSystem.getCurrentProcess().heap.free(This);
        }
        return refCount;
    }

    // ULONG Release(this)
    static private Callback.Handler Release = new HandlerBase() {
        public java.lang.String getName() {
            return "IUnknown.Release";
        }
        public void onCall() {
            int This = CPU.CPU_Pop32();
            CPU_Regs.reg_eax.dword = Release(This);
        }
    };
}
