package jdos.win.builtin;

import jdos.win.loader.BuiltinModule;

public class Shell32 extends BuiltinModule {
    public Shell32(int handle) {
        super("Shell32.dll", handle);
    }
}
