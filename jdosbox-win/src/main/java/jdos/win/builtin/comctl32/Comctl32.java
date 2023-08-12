package jdos.win.builtin.comctl32;

import jdos.win.loader.BuiltinModule;
import jdos.win.loader.Loader;

public class Comctl32 extends BuiltinModule {
    public Comctl32(Loader loader, int handle) {
        super(loader, "Comctrl32.dll", handle);

        add(Comctl32.class, "InitCommonControls", new String[0], 17);
    }
    // void InitCommonControls(void);
    public static void InitCommonControls() {
    }
}
