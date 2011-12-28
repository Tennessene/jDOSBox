package jdos.win.builtin;

import jdos.win.loader.BuiltinModule;
import jdos.win.loader.Loader;

public class Version extends BuiltinModule {
    public Version(Loader loader, int handle) {
        super(loader, "Version.dll", handle);
    }
}
