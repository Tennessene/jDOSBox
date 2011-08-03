package jdos.cpu.core_dynamic;

import jdos.cpu.CPU;
import jdos.cpu.CPU_Regs;
import jdos.cpu.Core;
import jdos.cpu.core_share.Constants;
import jdos.cpu.core_share.SMC_Exception;
import jdos.debug.Debug;
import jdos.misc.setup.Config;

final public class DecodeBlock {
    Op op;

    public static boolean smc = false;
    public DecodeBlock(Op op) {
        this.op = op;
    }
    final public int call2() {
        Op o = op;
        int result = Constants.BR_Normal;
        Core.base_ds= CPU.Segs_DSphys;
        Core.base_ss=CPU.Segs_SSphys;
        Core.base_val_ds= CPU_Regs.ds;
        int cycles=0;
        try {
            while (true) {
                if (Config.DEBUG_LOG) {
                    if (o.c>=0) Debug.start(Debug.TYPE_CPU, o.c);
                    //System.out.println(count+":"+o.c);
                }
                result = o.call();
                cycles++;
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
                    break;
                }
            }
        } catch (SMC_Exception e) {
            System.out.println("SMC");
            CPU_Regs.reg_eip+=o.eip_count;
        }
        CPU.CPU_Cycles-=cycles;
        return result;
    }
}
