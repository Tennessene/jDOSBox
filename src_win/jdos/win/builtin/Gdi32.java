package jdos.win.builtin;

import jdos.win.loader.BuiltinModule;

public class Gdi32 extends BuiltinModule {
    public Gdi32(int handle) {
        super("Gdi32.dll", handle);
    }
}
