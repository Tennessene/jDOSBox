package jdos.cpu;

import jdos.hardware.Memory;

public abstract class Core extends CPU_Regs {
    static public /*Bitu*/int opcode_index;
    static public /*PhysPt*/long cseip;
    static public int base_val_ds;
    static public boolean rep_zero;

    static public /*PhysPt*/long base_ds,base_ss;
    static public /*Bitu*/int prefixes;
    static public Table_ea.GetEAHandler[] ea_table;

    static public interface Fetchb_interface {
        public short call();
    }
    static public interface Fetchbs_interface {
        public byte call();
    }
    static public interface Fetchw_interface {
        public int call();
    }
    static public interface Fetchws_interface {
        public short call();
    }
    static public interface Fetchd_interface {
        public long call();
    }
    static public interface Fetchds_interface {
        public int call();
    }
    static void setupFetch(boolean full) {
        if (full) {
            Fetchb = new Fetchb_interface() {
                final public short call() {
                    return Memory.mem_readb(cseip++);
                }
            };
            Fetchbs = new Fetchbs_interface() {
                final public byte call() {
                    return (byte)Memory.mem_readb(cseip++);
                }
            };
            Fetchw = new Fetchw_interface() {
                final public int call() {
                    int temp = Memory.mem_readw(cseip);
                    cseip+=2;
                    return temp;
                }
            };
            Fetchws = new Fetchws_interface() {
                final public short call() {
                    short temp = (short)Memory.mem_readw(cseip);
                    cseip+=2;
                    return temp;
                }
            };
            Fetchd = new Fetchd_interface() {
                final public long call() {
                    long temp = Memory.mem_readd(cseip);
                    cseip+=4;
                    return temp;
                }
            };
            Fetchds = new Fetchds_interface() {
                final public int call() {
                    int temp = (int)Memory.mem_readd(cseip);
                    cseip+=4;
                    return temp;
                }
            };
        } else {
            Fetchb = new Fetchb_interface() {
                final public short call() {
                    return (short)(Memory.direct[(int)cseip++] & 0xFF);
                }
            };
            Fetchbs = new Fetchbs_interface() {
                final public byte call() {
                    return Memory.direct[(int)cseip++];
                }
            };
            Fetchw = new Fetchw_interface() {
                final public int call() {
                    int off = (int)cseip;
                    int temp = (Memory.direct[off] & 0xFF) | ((Memory.direct[off+1] & 0xFF) << 8);
                    cseip+=2;
                    return temp;
                }
            };
            Fetchws = new Fetchws_interface() {
                final public short call() {
                    int off = (int)cseip;
                    short temp = (short)((Memory.direct[off] & 0xFF) | ((Memory.direct[off+1] & 0xFF) << 8));
                    cseip+=2;
                    return temp;
                }
            };
            Fetchd = new Fetchd_interface() {
                final public long call() {
                    int off = (int)cseip;
                    long temp = (Memory.direct[off] & 0xFF) | ((Memory.direct[off+1] & 0xFF) << 8) | ((Memory.direct[off+2] & 0xFF) << 16) | ((long)(Memory.direct[off+3] & 0xFF) << 24);
                    cseip+=4;
                    return temp;
                }
            };
            Fetchds = new Fetchds_interface() {
                final public int call() {
                    int off = (int)cseip;
                    int temp = ((Memory.direct[off] & 0xFF) | ((Memory.direct[off+1] & 0xFF) << 8) | ((Memory.direct[off+2] & 0xFF) << 16) | ((Memory.direct[off+3] & 0xFF) << 24));
                    cseip+=4;
                    return temp;
                }
            };
        }
    }
    static public Fetchb_interface Fetchb;
    static public Fetchbs_interface Fetchbs;
    static public Fetchw_interface Fetchw;
    static public Fetchws_interface Fetchws;
    static public Fetchd_interface Fetchd;
    static public Fetchds_interface Fetchds;

    static public void DO_PREFIX_SEG_ES() {
        base_ds=CPU.Segs_ESphys;
        base_ss=CPU.Segs_ESphys;
        base_val_ds=CPU_Regs.es;
    }

    static public void DO_PREFIX_SEG_CS() {
        base_ds=CPU.Segs_CSphys;
        base_ss=CPU.Segs_CSphys;
        base_val_ds=CPU_Regs.cs;
    }

    static public void DO_PREFIX_SEG_SS() {
        base_ds=CPU.Segs_SSphys;
        base_ss=CPU.Segs_SSphys;
        base_val_ds=CPU_Regs.ss;
    }

    static public void DO_PREFIX_SEG_DS() {
        base_ds=CPU.Segs_DSphys;
        base_ss=CPU.Segs_DSphys;
        base_val_ds=CPU_Regs.ds;
    }

    static public void DO_PREFIX_SEG_FS() {
        base_ds=CPU.Segs_FSphys;
        base_ss=CPU.Segs_FSphys;
        base_val_ds=CPU_Regs.fs;
    }

    static public void DO_PREFIX_SEG_GS() {
        base_ds=CPU.Segs_GSphys;
        base_ss=CPU.Segs_GSphys;
        base_val_ds=CPU_Regs.gs;
    }

    final static public int PREFIX_ADDR = 0x1;
    final static public int PREFIX_REP = 0x2;
}
