package jdos.win.builtin;

import jdos.win.loader.BuiltinModule;
import jdos.win.loader.Loader;

public class Msvfw32 extends BuiltinModule {
    public Msvfw32(Loader loader, int handle) {
        super(loader, "Msvfw32.dll", handle);
    }
}
