package jdos.win.builtin;

import jdos.win.loader.BuiltinModule;
import jdos.win.loader.Loader;

public class Lz32 extends BuiltinModule {
    public Lz32(Loader loader, int handle) {
        super(loader, "Lz32.dll", handle);
    }
}
