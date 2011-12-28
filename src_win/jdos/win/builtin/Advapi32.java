package jdos.win.builtin;

import jdos.win.loader.BuiltinModule;
import jdos.win.loader.Loader;

public class Advapi32 extends BuiltinModule {
    public Advapi32(Loader loader, int handle) {
        super(loader, "advapi32.dll", handle);
    }
}
