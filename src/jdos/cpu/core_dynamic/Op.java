package jdos.cpu.core_dynamic;

import jdos.cpu.CPU;
import jdos.cpu.CPU_Regs;
import jdos.cpu.Flags;
import jdos.cpu.core_share.Constants;

abstract public class Op {
    public int c=-1;
    public long eip_count=0;
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

    public int DECODE_END() {
        CPU_Regs.reg_eip += eip_count;
        Flags.FillFlags();
        return Constants.BR_CBRet_None;
    }
}
