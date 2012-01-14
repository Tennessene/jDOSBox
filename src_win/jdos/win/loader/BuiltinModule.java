package jdos.win.loader;

import jdos.cpu.Callback;
import jdos.util.IntRef;
import jdos.util.LongRef;
import jdos.util.StringRef;
import jdos.win.builtin.HandlerBase;
import jdos.win.builtin.WinAPI;
import jdos.win.kernel.WinCallback;
import jdos.win.loader.winpe.HeaderImageImportDescriptor;
import jdos.win.loader.winpe.HeaderImageOptional;
import jdos.win.system.WinSystem;

import java.util.Hashtable;
import java.util.Vector;

public class BuiltinModule extends Module {
    private Hashtable functions = new Hashtable();
    private String fileName;
    private Hashtable registeredCallbacks = new Hashtable();
    public Loader loader;
    private Hashtable ordinalToName = new Hashtable();

    public BuiltinModule(Loader loader, String name, int handle) {
        super(handle);
        this.name = name.substring(0, name.lastIndexOf("."));
        this.fileName = name;
        this.loader = loader;
    }
    protected void add(Callback.Handler handler) {
        functions.put(handler.getName().substring(name.length() + 1), handler);
    }
    protected void add(Callback.Handler handler, int ordinal) {
        String name = handler.getName().substring(this.name.length() + 1);
        functions.put(name, handler);
        ordinalToName.put(new Integer(ordinal), name);
    }
    protected int addData(String name, int size) {
        int result = WinSystem.getCurrentProcess().heap.alloc(size, false);
        registeredCallbacks.put(name, new Integer(result));
        return result;
    }

    public int getProcAddress(final String functionName, boolean loadFake) {
        Integer result = (Integer)registeredCallbacks.get(functionName);
        if (result != null)
            return result.intValue();

        Callback.Handler handler = (Callback.Handler)functions.get(functionName);
        if (handler == null) {
            System.out.println("Unknown "+name+" function: "+functionName);
            if (loadFake) {
                handler = new HandlerBase() {
                    public void onCall() {
                        notImplemented();
                    }

                    public String getName() {
                        return name+" -> "+functionName;
                    }
                };
            }
        }
        if (handler != null) {
            int cb = WinCallback.addCallback(handler);
            int address =  loader.registerFunction(cb);
            registeredCallbacks.put(functionName, new Integer(address));
            return address;
        }
        return 0;
    }

    public String getFileName(boolean fullPath) {
        if (fullPath)
            return WinAPI.SYSTEM32_PATH+fileName;
        return fileName;
    }

    public void callDllMain(int dwReason) {
    }

    public void unload() {
    }

    public boolean RtlImageDirectoryEntryToData(int dir, LongRef address, LongRef size) {
        if (dir == HeaderImageOptional.IMAGE_DIRECTORY_ENTRY_EXPORT)
            return true;
        return false;
    }

    public Vector getImportDescriptors(long address) {
        return null;
    }

    public String getVirtualString(long address) {
        return null;
    }

    public long[] getImportList(HeaderImageImportDescriptor desc) {
        return null;
    }

    public long findNameExport(long exportAddress, long exportsSize, String name, int hint) {
        return getProcAddress(name, true);
    }

    public long findOrdinalExport(long exportAddress, long exportsSize, int ordinal) {
        String name = (String)ordinalToName.get(new Integer(ordinal));
        if (name != null)
            return getProcAddress(name, true);
        return 0;
    }

    public void getImportFunctionName(long address, StringRef name, IntRef hint) {
    }

    public void writeThunk(HeaderImageImportDescriptor desc, int index, long value) {
    }
}
