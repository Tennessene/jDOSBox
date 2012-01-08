package jdos.win.builtin;

import jdos.cpu.CPU_Regs;
import jdos.cpu.Callback;
import jdos.win.loader.BuiltinModule;
import jdos.win.loader.Loader;
import jdos.win.utils.WinSystem;

public class WinMM extends BuiltinModule {
    public WinMM(Loader loader, int handle) {
        super(loader, "WinMM.dll", handle);
        add(timeGetTime);
    }

    // DWORD timeGetTime(void)
    private Callback.Handler timeGetTime = new HandlerBase() {
        public java.lang.String getName() {
            return "WinMM.timeGetTime";
        }
        public void onCall() {
            CPU_Regs.reg_eax.dword = WinSystem.getTickCount();
        }
    };
}
