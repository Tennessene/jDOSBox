package jdos.win.builtin;

import jdos.win.loader.BuiltinModule;
import jdos.win.loader.Loader;

public class WinMM extends BuiltinModule {
    public WinMM(Loader loader, int handle) {
        super(loader, "WinMM.dll", handle);
    }
}
