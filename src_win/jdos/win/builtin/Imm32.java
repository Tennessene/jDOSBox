package jdos.win.builtin;

import jdos.win.loader.BuiltinModule;
import jdos.win.loader.Loader;

public class Imm32 extends BuiltinModule {
    public Imm32(Loader loader, int handle) {
        super(loader, "Imm32.dll", handle);
    }
}