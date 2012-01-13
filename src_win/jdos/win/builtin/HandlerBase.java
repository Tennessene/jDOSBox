package jdos.win.builtin;

import jdos.cpu.CPU;
import jdos.cpu.CPU_Regs;
import jdos.cpu.Callback;
import jdos.win.Console;
import jdos.win.Win;
import jdos.win.loader.Module;
import jdos.win.utils.Error;
import jdos.win.utils.WinSystem;

abstract public class HandlerBase implements Callback.Handler {
    protected static boolean defaultLog;

    boolean resetError = true;
    public HandlerBase() {
    }
    public HandlerBase(boolean resetError) {
        this.resetError = resetError;
    }
    public int call() {
        if (resetError)
            WinSystem.getCurrentThread().setLastError(Error.ERROR_SUCCESS);
        if (Module.LOG)
            defaultLog = true;
        if (preCall()) {
            CPU_Regs.reg_eip = CPU.CPU_Pop32();
            onCall();
        }
        if (Module.LOG) {
            if (defaultLog)
                System.out.println(Integer.toHexString(CPU_Regs.reg_eip)+": "+getName());
        }
        return 0;
    }

    // This gives some handlers the chance to get the current eip before it is popped
    public boolean preCall() {
        return true;
    }
    abstract public void onCall();

    protected void notImplemented() {
        System.out.println(getName()+" not implemented yet.");
        Console.out(getName() + " not implemented yet.");
        Win.exit();
    }

    protected void log(String msg) {
        defaultLog = false;
        System.out.println(Integer.toString(CPU_Regs.reg_eip, 16)+": "+getName()+" "+msg);
    }

    protected void dumpRegs() {
        System.out.print("eax=");
        System.out.print(Long.toString(CPU_Regs.reg_eax.dword & 0xFFFFFFFFl, 16));
        System.out.print(" ecx=");
        System.out.print(Long.toString(CPU_Regs.reg_ecx.dword & 0xFFFFFFFFl, 16));
        System.out.print(" edx=");
        System.out.print(Long.toString(CPU_Regs.reg_edx.dword & 0xFFFFFFFFl, 16));
        System.out.print(" ebx=");
        System.out.print(Long.toString(CPU_Regs.reg_ebx.dword & 0xFFFFFFFFl, 16));
        System.out.print(" esp=");
        System.out.print(Long.toString(CPU_Regs.reg_esp.dword & 0xFFFFFFFFl, 16));
        System.out.print(" ebp=");
        System.out.print(Long.toString(CPU_Regs.reg_ebp.dword & 0xFFFFFFFFl, 16));
        System.out.print(" esi=");
        System.out.print(Long.toString(CPU_Regs.reg_esi.dword & 0xFFFFFFFFl, 16));
        System.out.print(" edi=");
        System.out.println(Long.toString(CPU_Regs.reg_edi.dword & 0xFFFFFFFFl, 16));
    }
}
