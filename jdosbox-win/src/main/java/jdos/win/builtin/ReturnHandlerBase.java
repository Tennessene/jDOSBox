package jdos.win.builtin;

import jdos.cpu.CPU_Regs;

abstract public class ReturnHandlerBase extends HandlerBase {
    public void onCall() {
        CPU_Regs.reg_eax.dword = processReturn();
    }
    public abstract int processReturn();
}
