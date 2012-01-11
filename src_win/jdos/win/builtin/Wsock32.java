package jdos.win.builtin;

import jdos.win.loader.BuiltinModule;
import jdos.win.loader.Loader;

public class Wsock32 extends BuiltinModule {
    public Wsock32(Loader loader, int handle) {
        super(loader, "Wsock32.dll", handle);
    }
}
