package jdos.win.builtin;

import jdos.cpu.CPU;
import jdos.cpu.CPU_Regs;
import jdos.cpu.Callback;
import jdos.win.loader.BuiltinModule;
import jdos.win.loader.Loader;

public class Msvfw32 extends BuiltinModule {
    public Msvfw32(Loader loader, int handle) {
        super(loader, "Msvfw32.dll", handle);

        add(ICInfo);
    }

    // BOOL ICInfo(DWORD fccType, DWORD fccHandler, ICINFO  *lpicinfo)
    private Callback.Handler ICInfo = new HandlerBase() {
        public java.lang.String getName() {
            return "Msvfw32.ICInfo";
        }
        public void onCall() {
            int fccType = CPU.CPU_Pop32();
            int fccHandler = CPU.CPU_Pop32();
            int lpicinfo = CPU.CPU_Pop32();
            System.out.println(getName()+" faked");
            CPU_Regs.reg_eax.dword = WinAPI.FALSE;
        }
    };
}
