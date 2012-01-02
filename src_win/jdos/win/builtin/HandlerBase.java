package jdos.win.builtin;

import jdos.cpu.CPU;
import jdos.cpu.CPU_Regs;
import jdos.cpu.Callback;
import jdos.win.Console;
import jdos.win.Win;
import jdos.win.utils.Error;
import jdos.win.utils.WinSystem;

abstract public class HandlerBase implements Callback.Handler {
    boolean resetError = true;
    public HandlerBase() {
    }
    public HandlerBase(boolean resetError) {
        this.resetError = resetError;
    }
    public int call() {
        CPU_Regs.reg_eip = CPU.CPU_Pop32();
        if (resetError)
            WinSystem.getCurrentThread().setLastError(Error.ERROR_SUCCESS);
        System.out.println(Integer.toHexString(CPU_Regs.reg_eip)+": "+getName());
        onCall();
        return 0;
    }

    abstract public void onCall();

    protected void notImplemented() {
        System.out.println(getName()+" not implemented yet.");
        Console.out(getName() + " not implemented yet.");
        Win.exit();
    }
}
