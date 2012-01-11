package jdos.win.builtin;

import jdos.win.loader.BuiltinModule;
import jdos.win.loader.Loader;

public class Ole32 extends BuiltinModule {
    public Ole32(Loader loader, int handle) {
        super(loader, "Ole32.dll", handle);
    }
}
