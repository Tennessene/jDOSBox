package jdos.win.builtin;

import jdos.win.loader.BuiltinModule;
import jdos.win.loader.Loader;

public class Comdlg32 extends BuiltinModule {
    public Comdlg32(Loader loader, int handle) {
        super(loader, "Comdlg32.dll", handle);
    }
}
