package jdos.cpu.core_share;

import jdos.cpu.*;
import jdos.cpu.core_normal.Prefix_helpers;

public class ModifiedDecode {
    static public int call() {
        Core.cseip = CPU_Regs.reg_csPhys.dword + CPU_Regs.reg_eip;
        if (CPU.cpu.code.big) {
            Core.opcode_index = 0x200;
            Core.prefixes = 1;
            Table_ea.EA16 = false;
        } else {
            Core.opcode_index = 0;
            Core.prefixes = 0;
            Table_ea.EA16 = true;
        }
        while (true) {
            int c = Core.opcode_index + Core.Fetchb();
//                    last = c;
//                    Debug.start(Debug.TYPE_CPU, c);
//                    try {
            int result = jdos.cpu.core_normal.Prefix_none.ops[c].call();
            if (result == Prefix_helpers.HANDLED) {
                CPU_Regs.reg_eip = Core.cseip - CPU_Regs.reg_csPhys.dword;
                return Constants.BR_Jump;
            } else {
                if (result == Prefix_helpers.CONTINUE) {
                    break;
                } else if (result == Prefix_helpers.RETURN) {
                    Data.callback = jdos.cpu.core_normal.Prefix_none.returnValue;
                    return Constants.BR_CallBack;
                } else if (result == Prefix_helpers.RESTART) {
                    continue;
                } else if (result == Prefix_helpers.CBRET_NONE) {
                    Data.callback = Callback.CBRET_NONE;
                    return Constants.BR_CallBack;
                } else if (result == Prefix_helpers.DECODE_END) {
                    Prefix_helpers.SAVEIP();
                    Flags.FillFlags();
                    Data.callback = Callback.CBRET_NONE;
                    return Constants.BR_CallBack;
                } else if (result == Prefix_helpers.NOT_HANDLED || result == Prefix_helpers.ILLEGAL_OPCODE) {
                    CPU.CPU_Exception(6, 0);
                    break;
                }
            }
            break;
        }
        return Constants.BR_Jump;
    }
}
