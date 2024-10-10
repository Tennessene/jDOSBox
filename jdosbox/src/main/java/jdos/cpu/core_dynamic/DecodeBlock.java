package jdos.cpu.core_dynamic;

import jdos.Dosbox;
import jdos.cpu.CPU;
import jdos.cpu.CPU_Regs;
import jdos.cpu.Core;
import jdos.cpu.Paging;
import jdos.cpu.core_share.Constants;
import jdos.hardware.Memory;
import jdos.hardware.RAM;

final public class DecodeBlock extends Op {
    public Op op;
    public boolean active = true;
    public int codeStart;
    public int codeLen;
    public int runCount = 0;
    static public int compileThreshold = 0;

    public static boolean smc = false;
    private boolean compiled = false;
    public CacheBlockDynRec parent;
    public Op compiledOp = null;

    public boolean throwsException() {return false;}
    public boolean accessesMemory() {return false;}
    public boolean usesEip() {return false;}
    public boolean setsEip() {return false;}

    static private byte[] getOpCode(int start, int len) {
        byte[] opCode = new byte[len];
        int src = Paging.getDirectIndexRO(start);
        if (src>=0)
            RAM.memcpy(opCode, 0, src, len);
        else
            Memory.MEM_BlockRead(start, opCode, len);
        return opCode;
    }

    public DecodeBlock(CacheBlockDynRec parent, Op op, int start, int len) {
        this.parent = parent;
        this.op = op;
        this.next = op; // simplifies the compiler
        this.codeStart = start;
        this.codeLen = len;
        if (Loader.isLoaded()) {
            Op o = Loader.load(codeStart, getOpCode(codeStart, codeLen));
            if (o != null) {
                this.op = o;
                this.next = o;
                this.compiled = true;
            }
        }
    }
    final public int call() {
        if (Compiler.ENABLED) {
            runCount++;
            if (runCount==compileThreshold && !compiled && Dosbox.allPrivileges) {
                compiled = true;
                jdos.cpu.core_dynamic.Compiler.compile(this);
            }
            if (compiledOp!=null) {
                parent.code = compiledOp;
                return compiledOp.call();
            }
        }
        Core.base_ds= CPU_Regs.reg_dsPhys.dword;
        Core.base_ss=CPU_Regs.reg_ssPhys.dword;
        Core.base_val_ds= CPU_Regs.ds;
        CPU.CPU_Cycles-=op.cycle;
        try {
            return op.call();
        } catch (NullPointerException e) {
            if (smc) {
                System.out.println("SMC");
                smc = false;
                return Constants.BR_Jump;
            }
            throw e;
        }
    }
}
