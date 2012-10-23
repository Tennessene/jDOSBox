package jdos.cpu.core_dynamic;

import jdos.cpu.CPU;
import jdos.cpu.CPU_Regs;
import jdos.cpu.Callback;
import jdos.cpu.Flags;
import jdos.cpu.core_share.Constants;
import jdos.cpu.core_share.Data;

abstract public class Op {
    static final public int EAX = 1;

    static final public int ES = 0x01;
    static final public int CS = 0x02;
    static final public int DS = 0x03;
    static final public int FS = 0x04;
    static final public int GS = 0x05;
    static final public int SS = 0x06;
    static final public int FROM_REG = 0x10;
    static final public int FROM_STACK = 0x20;
    static final public int FROM_MEMORY = 0x30;

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
    public int setsSeg() {return 0;}
    public String description() {return "";}

//    public boolean throwsException() {return true;}
//    public boolean accessesMemory() {return true;}
//    public  boolean usesEip() {return true;}
//    public  boolean setsEip() {return true;}
    public boolean throwsException() {return false;}
    public boolean accessesMemory() {return false;}
    public boolean usesEip() {return false;}
    public boolean setsEip() {return false;}
}
