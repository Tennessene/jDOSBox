package jdos.win.builtin;

import jdos.win.loader.BuiltinModule;
import jdos.win.loader.Loader;

public class Shell32 extends BuiltinModule {
    public Shell32(Loader loader, int handle) {
        super(loader, "Shell32.dll", handle);
    }
}
