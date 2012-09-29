package jdos.cpu.core_dynamic;

import jdos.cpu.CPU;
import jdos.cpu.CPU_Regs;
import jdos.cpu.Callback;
import jdos.cpu.Flags;
import jdos.cpu.core_share.Constants;
import jdos.cpu.core_share.Data;

abstract public class Op {
    static final public int EAX = 1;

    public int c=-1;
    public int eip_count=0;
    public Op next;
    public int cycle = 0;

    abstract public int call();

    public int RUNEXCEPTION() {
        CPU.CPU_Exception(CPU.cpu.exception.which, CPU.cpu.exception.error);
        return Constants.BR_Jump;
    }

    public int EXCEPTION(int blah) {
        CPU.CPU_Exception(blah);
        return Constants.BR_Jump;
    }

    public int DECODE_END(int count) {
        CPU_Regs.reg_eip += count;
        Flags.FillFlags();
        return CB_NONE();
    }

    public int CB_NONE() {
        Data.callback = Callback.CBRET_NONE;
        return Constants.BR_CallBack;
    }

    public int sets() { return 0; }
    public int gets() { return 0; }

    public boolean returnsIllegal() {return false;}
//    public boolean throwsException() {return true;}
//    public boolean accessesMemory() {return true;}
//    public  boolean usesEip() {return true;}
//    public  boolean setsEip() {return true;}
    public abstract boolean throwsException();
    public abstract boolean accessesMemory();
    public abstract boolean usesEip();
    public abstract boolean setsEip();
}
