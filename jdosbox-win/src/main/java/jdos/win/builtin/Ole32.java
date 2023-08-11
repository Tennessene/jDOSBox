package jdos.win.builtin;

import jdos.win.loader.BuiltinModule;
import jdos.win.loader.Loader;
import jdos.win.utils.Error;

public class Ole32 extends BuiltinModule {
    public Ole32(Loader loader, int handle) {
        super(loader, "Ole32.dll", handle);
        add(Ole32.class, "CoInitialize", new String[] {"pvReserved"});
        add(Ole32.class, "CoUninitialize", new String[0]);
    }

    // HRESULT CoInitialize(LPVOID pvReserved)
    static public int CoInitialize(int pvReserved) {
        return Error.S_OK;
    }

    // void CoUninitialize(void)
    static public void CoUninitialize() {
    }
}
