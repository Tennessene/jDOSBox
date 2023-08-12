package jdos.win.builtin;

import jdos.win.loader.BuiltinModule;
import jdos.win.loader.Loader;

public class Winspool extends BuiltinModule {
    public Winspool(Loader loader, int handle) {
        super(loader, "Winspool.drv", handle);
    }
}
