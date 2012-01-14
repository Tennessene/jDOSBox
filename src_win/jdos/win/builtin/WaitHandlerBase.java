package jdos.win.builtin;

import jdos.cpu.CPU;
import jdos.cpu.CPU_Regs;
import jdos.win.system.WinSystem;

public abstract class WaitHandlerBase extends HandlerBase {
    private int eip;

    public boolean preCall() {
        eip = CPU_Regs.reg_eip-4; // -4 because the callback instruction called SAVEIP
        return true;
    }

    public void onCall() {
        int esp = CPU_Regs.reg_esp.dword;  // save so we can re-enter
        int result = onWait();
        if (result>0) {
            if (result == 1) {
                // come back to this handler
                // push the returning eip so that the handler will pop it again
                CPU_Regs.reg_esp.dword = esp;
                CPU.CPU_Push32(CPU_Regs.reg_eip);
                CPU_Regs.reg_eip = eip;
                WinSystem.scheduler.yield(WinSystem.getCurrentThread());
            } else if (result == 2) {
                WinSystem.scheduler.removeThread(WinSystem.getCurrentThread(), true);
            }
        }
    }

    abstract public int onWait();
}
