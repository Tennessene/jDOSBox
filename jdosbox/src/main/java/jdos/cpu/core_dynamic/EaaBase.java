package jdos.cpu.core_dynamic;

import jdos.cpu.Core;

public abstract class EaaBase {
    abstract public int call();
    public String description8() {return "b"+"@0x"+Integer.toHexString(call());}
    public String description16() {return "w"+"@0x"+Integer.toHexString(call());}
    public String description32() {return "d"+"@0x"+Integer.toHexString(call());}
    public String descriptionZero() {
        int ds = Core.base_ds;
        int ss = Core.base_ss;
        Core.base_ds = 0;
        Core.base_ss = 0;
        String result = "@0x"+Integer.toHexString(call());
        Core.base_ds = ds;
        Core.base_ss = ss;
        return result;
    }
}
