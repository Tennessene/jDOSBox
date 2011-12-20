package jdos.win.builtin;

import jdos.win.loader.BuiltinModule;

public class Advapi32 extends BuiltinModule {
    public Advapi32(int handle) {
        super("advapi32.dll", handle);
    }
}
