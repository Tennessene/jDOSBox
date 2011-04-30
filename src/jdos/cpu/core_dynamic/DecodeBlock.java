package jdos.cpu.core_dynamic;

import jdos.cpu.Core_dynamic;
import jdos.cpu.Core;
import jdos.cpu.CPU;
import jdos.cpu.CPU_Regs;
import jdos.cpu.core_share.Constants;
import jdos.misc.setup.Config;
import jdos.debug.Debug;

public class DecodeBlock {
    Op op;

    public DecodeBlock(Op op) {
        this.op = op;
    }
    public int call2() {
        Op o = op;
        int result = Constants.BR_Normal;
        Core.base_ds= CPU.Segs_DSphys;
        Core.base_ss=CPU.Segs_SSphys;
        Core.base_val_ds= CPU_Regs.ds;
        while (o != null && result == Constants.BR_Normal) {
            if (Config.DEBUG_LOG) {
                if (o.c>=0) Debug.start(Debug.TYPE_CPU, o.c);
                //System.out.println(count+":"+o.c);
            }
            result = o.call();
            CPU.CPU_Cycles--;
            if (Config.DEBUG_LOG)
                if (o.c>=0) Debug.stop(Debug.TYPE_CPU, o.c);
            o = o.next;
            if (result == Constants.BR_Continue) {
                result = Constants.BR_Normal;
                continue;
            }
            Core.base_ds=CPU.Segs_DSphys;
            Core.base_ss=CPU.Segs_SSphys;
            Core.base_val_ds= CPU_Regs.ds;
        }
        return result;
    }
}
