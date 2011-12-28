package jdos.win.builtin;

import jdos.win.loader.BuiltinModule;
import jdos.win.loader.Loader;

public class Crtdll extends BuiltinModule {
    public Crtdll(Loader loader, int handle) {
        super(loader, "Crtdll.dll", handle);
    }
}
