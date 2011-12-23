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

import java.util.Hashtable;
import java.util.Vector;

public class BuiltinModule extends Module {
    private Hashtable functions = new Hashtable();
    private String name;
    private String fileName;
    private Hashtable registeredCallbacks = new Hashtable();

    public BuiltinModule(String name, int handle) {
        super(handle);
        this.name = name.substring(0, name.lastIndexOf("."));
        this.fileName = name;
    }
    protected void add(Callback.Handler handler) {
        functions.put(handler.getName().substring(name.length() + 1), handler);
    }

    public int getProcAddress(final String functionName, boolean loadFake) {
        if (functionName.startsWith("GetVersion")) {
            int ii=0;
        }
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
            int address =  Loader.registerFunction(cb);
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

    public void unload() {
        functions.clear();
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
        return 0;
    }

    public void getImportFunctionName(long address, StringRef name, IntRef hint) {
    }

    public void writeThunk(HeaderImageImportDescriptor desc, int index, long value) {
    }
}
