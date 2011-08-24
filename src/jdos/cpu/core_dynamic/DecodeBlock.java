package jdos.cpu.core_dynamic;

import jdos.cpu.CPU;
import jdos.cpu.CPU_Regs;
import jdos.cpu.Core;
import jdos.cpu.core_share.Constants;
import jdos.debug.Debug;
import jdos.misc.setup.Config;

final public class DecodeBlock extends Op {
    public Op op;
    public boolean active = true;
    public byte[] byteCode;
    public int codeStart;
    public int runCount = 0;
    static public int compileThreshold = 0;

    public static boolean smc = false;
    public DecodeBlock(Op op) {
        this.op = op;
        this.next = op; // simplifies the compiler
    }
    final public int call() {
        runCount++;
        if (runCount==compileThreshold) {
            jdos.cpu.core_dynamic.Compiler.compile(this);
        }
        Op o = op;
        int result = Constants.BR_Normal;
        Core.base_ds= CPU.Segs_DSphys;
        Core.base_ss=CPU.Segs_SSphys;
        Core.base_val_ds= CPU_Regs.ds;
        while (true) {
            if (Config.DEBUG_LOG) {
                if (o.c>=0) Debug.start(Debug.TYPE_CPU, o.c);
                //System.out.println(count+":"+o.c);
            }
            result = o.call();
            if (Config.DEBUG_LOG)
                if (o.c>=0) Debug.stop(Debug.TYPE_CPU, o.c);
            if (result == Constants.BR_Normal) {
                CPU_Regs.reg_eip+=o.eip_count;
                o = o.next;
            } else
                break;
            // :TODO: this is a temporary solution, the right solution would
            // be when this is detected to changed the current running block
            // so that the next op will return BR_Jump
            if (smc) {
                smc = false;
                System.out.println("SMC");
                CPU.CPU_Cycles-=op.cycle;
                return Constants.BR_Jump;
            }
        }
        CPU.CPU_Cycles-=op.cycle;
        return result;
    }
}
