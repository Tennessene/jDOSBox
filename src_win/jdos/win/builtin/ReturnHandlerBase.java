package jdos.win.builtin;

import jdos.cpu.CPU_Regs;
import jdos.win.loader.Module;

abstract public class ReturnHandlerBase extends HandlerBase {
    public void onCall() {
        CPU_Regs.reg_eax.dword = processReturn();
        if (Module.LOG)
            log(" result="+CPU_Regs.reg_eax.dword + "(0x"+Integer.toString(CPU_Regs.reg_eax.dword, 16)+")");
    }
    public abstract int processReturn();
}
