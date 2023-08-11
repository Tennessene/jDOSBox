package jdos.cpu.core_switch;

import jdos.cpu.CPU_Regs;

public class SwitchBlock {
    public Inst instruction;
    public int opCode;
    public int eipCount;
    public CPU_Regs.Reg r1;
    public CPU_Regs.Reg r2;
    public int value;

    public CPU_Regs.Reg eaa_r1;
    public CPU_Regs.Reg eaa_r2;
    public int eaa_sib; // reused by String as width
    public CPU_Regs.Reg eaa_segPhys;
    public CPU_Regs.Reg eaa_segVal;
    public int eaa_const; // reused by String as prefix

    public boolean eaa16;
    public boolean zero;
}
