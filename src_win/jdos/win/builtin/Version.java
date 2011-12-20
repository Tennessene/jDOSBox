package jdos.win.builtin;

import jdos.win.loader.BuiltinModule;

public class Version extends BuiltinModule {
    public Version(int handle) {
        super("Version.dll", handle);
    }
}
