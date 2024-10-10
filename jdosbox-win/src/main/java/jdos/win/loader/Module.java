package jdos.win.loader;

import jdos.util.IntRef;
import jdos.util.LongRef;
import jdos.util.StringRef;
import jdos.win.builtin.WinAPI;
import jdos.win.loader.winpe.HeaderImageImportDescriptor;

import java.util.Vector;

public abstract class Module extends WinAPI {
    private int handle;
    public String name;
    protected boolean threadLibraryCalls = true;

    public Module(int handle) {
        this.handle = handle;
    }

    public int getHandle() {
        return handle;
    }

    public void disableThreadLibraryCalls() {
        threadLibraryCalls = false;
    }

    public static final int DLL_PROCESS_DETACH = 0;
    public static final int DLL_PROCESS_ATTACH = 1;
    public static final int DLL_THREAD_ATTACH = 2;
    public static final int DLL_THREAD_DETACH = 3;

    abstract public boolean RtlImageDirectoryEntryToData(int dir, LongRef address, LongRef size);
    abstract public Vector getImportDescriptors(long address);
    abstract public String getVirtualString(long address);
    abstract public long[] getImportList(HeaderImageImportDescriptor desc);
    abstract public long findNameExport(long exportAddress, long exportsSize, String name, int hint);
    abstract public long findOrdinalExport(long exportAddress, long exportsSize, int ordinal);
    abstract public void getImportFunctionName(long address, StringRef name, IntRef hint);
    abstract public void writeThunk(HeaderImageImportDescriptor desc, int index, long value);
    abstract public void unload();
    abstract public int getProcAddress(String name, boolean loadFake);
    abstract public String getFileName(boolean fullPath);
    abstract public void callDllMain(int dwReason);
}
