package jdos.cpu.core_dynamic;

import jdos.cpu.*;
import jdos.cpu.core_share.Constants;
import jdos.cpu.core_share.Data;
import jdos.fpu.FPU;
import jdos.hardware.IO;
import jdos.hardware.Memory;
import jdos.hardware.Pic;
import jdos.misc.Log;
import jdos.types.LogSeverities;
import jdos.types.LogTypes;
import jdos.util.IntRef;

public class Inst1 extends Helper {
    final static public class Addb_reg extends Op {
        Reg e;
        Reg g;

        public Addb_reg(Reg e, Reg g) {
            this.e = e;
            this.g = g;
        }

        public int call() {
            e.set8(Instructions.ADDB(g.get8(), e.get8()));
            return Constants.BR_Normal;
        }
    }

    final static public class AddEbGb_mem extends Op {
        EaaBase e;
        Reg g;

        public AddEbGb_mem(EaaBase e, Reg g) {
            this.e = e;
            this.g = g;
        }
        public int call() {
            int eaa = e.call();
            int index = Paging.getDirectIndex(eaa);
            if (index>=0)
                Memory.host_writeb(index, Instructions.ADDB(g.get8(), Memory.host_readb(index)));
            else
                Memory.mem_writeb(eaa, Instructions.ADDB(g.get8(), Memory.mem_readb(eaa)));
            return Constants.BR_Normal;
        }
    }

    final static public class AddGbEb_mem extends Op {
        EaaBase g;
        Reg e;

        public AddGbEb_mem(Reg e, EaaBase g) {
            this.e = e;
            this.g = g;
        }
        public int call() {
            int eaa = g.call();
            e.set8(Instructions.ADDB(Memory.mem_readb(eaa), e.get8()));
            return Constants.BR_Normal;
        }
    }

    final static public class Addw_reg extends Op {
        Reg e;
        Reg g;

        public Addw_reg(Reg e, Reg g) {
            this.e = e;
            this.g = g;
        }

        public int call() {
            e.word(Instructions.ADDW(g.word(), e.word()));
            return Constants.BR_Normal;
        }
    }

    final static public class AddEwGw_mem extends Op {
        EaaBase e;
        Reg g;

        public AddEwGw_mem(EaaBase e, Reg g) {
            this.e = e;
            this.g = g;
        }
        public int call() {
            int eaa = e.call();
            if ((eaa & 0xFFF) < 0xFFF) {
                int index = Paging.getDirectIndex(eaa);
                if (index>=0) {
                    Memory.host_writew(index, Instructions.ADDW(g.word(), Memory.host_readw(index)));
                    return Constants.BR_Normal;
                }
            }
            Memory.mem_writew(eaa, Instructions.ADDW(g.word(), Memory.mem_readw(eaa)));
            return Constants.BR_Normal;
        }
    }

    final static public class AddGwEw_mem extends Op {
        EaaBase g;
        Reg e;

        public AddGwEw_mem(Reg e, EaaBase g) {
            this.e = e;
            this.g = g;
        }
        public int call() {
            int eaa = g.call();
            e.word(Instructions.ADDW(Memory.mem_readw(eaa), e.word()));
            return Constants.BR_Normal;
        }
    }

    final static public class AddAlIb extends Op {
        short i;

        public AddAlIb(short i) {
            this.i = i;
        }
        public int call() {
            reg_eax.low(Instructions.ADDB(i, reg_eax.low()));
            return Constants.BR_Normal;
        }
    }

    final static public class AddAxIw extends Op {
        int i;

        public AddAxIw(int i) {
            this.i = i;
        }
        public int call() {
            reg_eax.word(Instructions.ADDW(i, reg_eax.word()));
            return Constants.BR_Normal;
        }
    }

    final static public class Orb_reg extends Op {
        Reg e;
        Reg g;

        public Orb_reg(Reg e, Reg g) {
            this.e = e;
            this.g = g;
        }

        public int call() {
            e.set8(Instructions.ORB(g.get8(), e.get8()));
            return Constants.BR_Normal;
        }
    }

    final static public class OrEbGb_mem extends Op {
        EaaBase e;
        Reg g;

        public OrEbGb_mem(EaaBase e, Reg g) {
            this.e = e;
            this.g = g;
        }
        public int call() {
            int eaa = e.call();
            int index = Paging.getDirectIndex(eaa);
            if (index>=0)
                Memory.host_writeb(index, Instructions.ORB(g.get8(), Memory.host_readb(index)));
            else
                Memory.mem_writeb(eaa, Instructions.ORB(g.get8(), Memory.mem_readb(eaa)));
            return Constants.BR_Normal;
        }
    }

    final static public class OrGbEb_mem extends Op {
        EaaBase g;
        Reg e;

        public OrGbEb_mem(Reg e, EaaBase g) {
            this.e = e;
            this.g = g;
        }
        public int call() {
            int eaa = g.call();
            e.set8(Instructions.ORB(Memory.mem_readb(eaa), e.get8()));
            return Constants.BR_Normal;
        }
    }

    final static public class Orw_reg extends Op {
        Reg e;
        Reg g;

        public Orw_reg(Reg e, Reg g) {
            this.e = e;
            this.g = g;
        }

        public int call() {
            e.word(Instructions.ORW(g.word(), e.word()));
            return Constants.BR_Normal;
        }
    }

    final static public class OrEwGw_mem extends Op {
        EaaBase e;
        Reg g;

        public OrEwGw_mem(EaaBase e, Reg g) {
            this.e = e;
            this.g = g;
        }
        public int call() {
            int eaa = e.call();
            if ((eaa & 0xFFF) < 0xFFF) {
                int index = Paging.getDirectIndex(eaa);
                if (index>=0) {
                    Memory.host_writew(index, Instructions.ORW(g.word(), Memory.host_readw(index)));
                    return Constants.BR_Normal;
                }
            }
            Memory.mem_writew(eaa, Instructions.ORW(g.word(), Memory.mem_readw(eaa)));
            return Constants.BR_Normal;
        }
    }

    final static public class OrGwEw_mem extends Op {
        EaaBase g;
        Reg e;

        public OrGwEw_mem(Reg e, EaaBase g) {
            this.e = e;
            this.g = g;
        }
        public int call() {
            int eaa = g.call();
            e.word(Instructions.ORW(Memory.mem_readw(eaa), e.word()));
            return Constants.BR_Normal;
        }
    }

    final static public class OrAlIb extends Op {
        short i;

        public OrAlIb(short i) {
            this.i = i;
        }
        public int call() {
            reg_eax.low(Instructions.ORB(i, reg_eax.low()));
            return Constants.BR_Normal;
        }
    }

    final static public class OrAxIw extends Op {
        int i;

        public OrAxIw(int i) {
            this.i = i;
        }
        public int call() {
            reg_eax.word(Instructions.ORW(i, reg_eax.word()));
            return Constants.BR_Normal;
        }
    }

    final static public class Adcb_reg extends Op {
        Reg e;
        Reg g;

        public Adcb_reg(Reg e, Reg g) {
            this.e = e;
            this.g = g;
        }

        public int call() {
            e.set8(Instructions.ADCB(g.get8(), e.get8()));
            return Constants.BR_Normal;
        }
    }

    final static public class AdcEbGb_mem extends Op {
        EaaBase e;
        Reg g;

        public AdcEbGb_mem(EaaBase e, Reg g) {
            this.e = e;
            this.g = g;
        }
        public int call() {
            int eaa = e.call();
            int index = Paging.getDirectIndex(eaa);
            if (index>=0)
                Memory.host_writeb(index, Instructions.ADCB(g.get8(), Memory.host_readb(index)));
            else
                Memory.mem_writeb(eaa, Instructions.ADCB(g.get8(), Memory.mem_readb(eaa)));
            return Constants.BR_Normal;
        }
    }

    final static public class AdcGbEb_mem extends Op {
        EaaBase g;
        Reg e;

        public AdcGbEb_mem(Reg e, EaaBase g) {
            this.e = e;
            this.g = g;
        }
        public int call() {
            int eaa = g.call();
            e.set8(Instructions.ADCB(Memory.mem_readb(eaa), e.get8()));
            return Constants.BR_Normal;
        }
    }

    final static public class Adcw_reg extends Op {
        Reg e;
        Reg g;

        public Adcw_reg(Reg e, Reg g) {
            this.e = e;
            this.g = g;
        }

        public int call() {
            e.word(Instructions.ADCW(g.word(), e.word()));
            return Constants.BR_Normal;
        }
    }

    final static public class AdcEwGw_mem extends Op {
        EaaBase e;
        Reg g;

        public AdcEwGw_mem(EaaBase e, Reg g) {
            this.e = e;
            this.g = g;
        }
        public int call() {
            int eaa = e.call();
            if ((eaa & 0xFFF) < 0xFFF) {
                int index = Paging.getDirectIndex(eaa);
                if (index>=0) {
                    Memory.host_writew(index, Instructions.ADCW(g.word(), Memory.host_readw(index)));
                    return Constants.BR_Normal;
                }
            }
            Memory.mem_writew(eaa, Instructions.ADCW(g.word(), Memory.mem_readw(eaa)));
            return Constants.BR_Normal;
        }
    }

    final static public class AdcGwEw_mem extends Op {
        EaaBase g;
        Reg e;

        public AdcGwEw_mem(Reg e, EaaBase g) {
            this.e = e;
            this.g = g;
        }
        public int call() {
            int eaa = g.call();
            e.word(Instructions.ADCW(Memory.mem_readw(eaa), e.word()));
            return Constants.BR_Normal;
        }
    }

    final static public class AdcAlIb extends Op {
        short i;

        public AdcAlIb(short i) {
            this.i = i;
        }
        public int call() {
            reg_eax.low(Instructions.ADCB(i, reg_eax.low()));
            return Constants.BR_Normal;
        }
    }

    final static public class AdcAxIw extends Op {
        int i;

        public AdcAxIw(int i) {
            this.i = i;
        }
        public int call() {
            reg_eax.word(Instructions.ADCW(i, reg_eax.word()));
            return Constants.BR_Normal;
        }
    }
    
    final static public class Sbbb_reg extends Op {
        Reg e;
        Reg g;

        public Sbbb_reg(Reg e, Reg g) {
            this.e = e;
            this.g = g;
        }

        public int call() {
            e.set8(Instructions.SBBB(g.get8(), e.get8()));
            return Constants.BR_Normal;
        }
    }

    final static public class SbbEbGb_mem extends Op {
        EaaBase e;
        Reg g;

        public SbbEbGb_mem(EaaBase e, Reg g) {
            this.e = e;
            this.g = g;
        }
        public int call() {
            int eaa = e.call();
            int index = Paging.getDirectIndex(eaa);
            if (index>=0)
                Memory.host_writeb(index, Instructions.SBBB(g.get8(), Memory.host_readb(index)));
            else
                Memory.mem_writeb(eaa, Instructions.SBBB(g.get8(), Memory.mem_readb(eaa)));
            return Constants.BR_Normal;
        }
    }

    final static public class SbbGbEb_mem extends Op {
        EaaBase g;
        Reg e;

        public SbbGbEb_mem(Reg e, EaaBase g) {
            this.e = e;
            this.g = g;
        }
        public int call() {
            int eaa = g.call();
            e.set8(Instructions.SBBB(Memory.mem_readb(eaa), e.get8()));
            return Constants.BR_Normal;
        }
    }

    final static public class Sbbw_reg extends Op {
        Reg e;
        Reg g;

        public Sbbw_reg(Reg e, Reg g) {
            this.e = e;
            this.g = g;
        }

        public int call() {
            e.word(Instructions.SBBW(g.word(), e.word()));
            return Constants.BR_Normal;
        }
    }

    final static public class SbbEwGw_mem extends Op {
        EaaBase e;
        Reg g;

        public SbbEwGw_mem(EaaBase e, Reg g) {
            this.e = e;
            this.g = g;
        }
        public int call() {
            int eaa = e.call();
            if ((eaa & 0xFFF) < 0xFFF) {
                int index = Paging.getDirectIndex(eaa);
                if (index>=0) {
                    Memory.host_writew(index, Instructions.SBBW(g.word(), Memory.host_readw(index)));
                    return Constants.BR_Normal;
                }
            }
            Memory.mem_writew(eaa, Instructions.SBBW(g.word(), Memory.mem_readw(eaa)));
            return Constants.BR_Normal;
        }
    }

    final static public class SbbGwEw_mem extends Op {
        EaaBase g;
        Reg e;

        public SbbGwEw_mem(Reg e, EaaBase g) {
            this.e = e;
            this.g = g;
        }
        public int call() {
            int eaa = g.call();
            e.word(Instructions.SBBW(Memory.mem_readw(eaa), e.word()));
            return Constants.BR_Normal;
        }
    }

    final static public class SbbAlIb extends Op {
        short i;

        public SbbAlIb(short i) {
            this.i = i;
        }
        public int call() {
            reg_eax.low(Instructions.SBBB(i, reg_eax.low()));
            return Constants.BR_Normal;
        }
    }

    final static public class SbbAxIw extends Op {
        int i;

        public SbbAxIw(int i) {
            this.i = i;
        }
        public int call() {
            reg_eax.word(Instructions.SBBW(i, reg_eax.word()));
            return Constants.BR_Normal;
        }
    }

    final static public class Andb_reg extends Op {
        Reg e;
        Reg g;

        public Andb_reg(Reg e, Reg g) {
            this.e = e;
            this.g = g;
        }

        public int call() {
            e.set8(Instructions.ANDB(g.get8(), e.get8()));
            return Constants.BR_Normal;
        }
    }

    final static public class AndEbGb_mem extends Op {
        EaaBase e;
        Reg g;

        public AndEbGb_mem(EaaBase e, Reg g) {
            this.e = e;
            this.g = g;
        }
        public int call() {
            int eaa = e.call();
            int index = Paging.getDirectIndex(eaa);
            if (index>=0)
                Memory.host_writeb(index, Instructions.ANDB(g.get8(), Memory.host_readb(index)));
            else
                Memory.mem_writeb(eaa, Instructions.ANDB(g.get8(), Memory.mem_readb(eaa)));
            return Constants.BR_Normal;
        }
    }

    final static public class AndGbEb_mem extends Op {
        EaaBase g;
        Reg e;

        public AndGbEb_mem(Reg e, EaaBase g) {
            this.e = e;
            this.g = g;
        }
        public int call() {
            int eaa = g.call();
            e.set8(Instructions.ANDB(Memory.mem_readb(eaa), e.get8()));
            return Constants.BR_Normal;
        }
    }

    final static public class Andw_reg extends Op {
        Reg e;
        Reg g;

        public Andw_reg(Reg e, Reg g) {
            this.e = e;
            this.g = g;
        }

        public int call() {
            e.word(Instructions.ANDW(g.word(), e.word()));
            return Constants.BR_Normal;
        }
    }

    final static public class AndEwGw_mem extends Op {
        EaaBase e;
        Reg g;

        public AndEwGw_mem(EaaBase e, Reg g) {
            this.e = e;
            this.g = g;
        }
        public int call() {
            int eaa = e.call();
            if ((eaa & 0xFFF) < 0xFFF) {
                int index = Paging.getDirectIndex(eaa);
                if (index>=0) {
                    Memory.host_writew(index, Instructions.ANDW(g.word(), Memory.host_readw(index)));
                    return Constants.BR_Normal;
                }
            }
            Memory.mem_writew(eaa, Instructions.ANDW(g.word(), Memory.mem_readw(eaa)));
            return Constants.BR_Normal;
        }
    }

    final static public class AndGwEw_mem extends Op {
        EaaBase g;
        Reg e;

        public AndGwEw_mem(Reg e, EaaBase g) {
            this.e = e;
            this.g = g;
        }
        public int call() {
            int eaa = g.call();
            e.word(Instructions.ANDW(Memory.mem_readw(eaa), e.word()));
            return Constants.BR_Normal;
        }
    }

    final static public class AndAlIb extends Op {
        short i;

        public AndAlIb(short i) {
            this.i = i;
        }
        public int call() {
            reg_eax.low(Instructions.ANDB(i, reg_eax.low()));
            return Constants.BR_Normal;
        }
    }

    final static public class AndAxIw extends Op {
        int i;

        public AndAxIw(int i) {
            this.i = i;
        }
        public int call() {
            reg_eax.word(Instructions.ANDW(i, reg_eax.word()));
            return Constants.BR_Normal;
        }
    }

    final static public class Subb_reg extends Op {
        Reg e;
        Reg g;

        public Subb_reg(Reg e, Reg g) {
            this.e = e;
            this.g = g;
        }

        public int call() {
            e.set8(Instructions.SUBB(g.get8(), e.get8()));
            return Constants.BR_Normal;
        }
    }

    final static public class SubEbGb_mem extends Op {
        EaaBase e;
        Reg g;

        public SubEbGb_mem(EaaBase e, Reg g) {
            this.e = e;
            this.g = g;
        }
        public int call() {
            int eaa = e.call();
            int index = Paging.getDirectIndex(eaa);
            if (index>=0)
                Memory.host_writeb(index, Instructions.SUBB(g.get8(), Memory.host_readb(index)));
            else
                Memory.mem_writeb(eaa, Instructions.SUBB(g.get8(), Memory.mem_readb(eaa)));
            return Constants.BR_Normal;
        }
    }

    final static public class SubGbEb_mem extends Op {
        EaaBase g;
        Reg e;

        public SubGbEb_mem(Reg e, EaaBase g) {
            this.e = e;
            this.g = g;
        }
        public int call() {
            int eaa = g.call();
            e.set8(Instructions.SUBB(Memory.mem_readb(eaa), e.get8()));
            return Constants.BR_Normal;
        }
    }

    final static public class Subw_reg extends Op {
        Reg e;
        Reg g;

        public Subw_reg(Reg e, Reg g) {
            this.e = e;
            this.g = g;
        }

        public int call() {
            e.word(Instructions.SUBW(g.word(), e.word()));
            return Constants.BR_Normal;
        }
    }

    final static public class SubEwGw_mem extends Op {
        EaaBase e;
        Reg g;

        public SubEwGw_mem(EaaBase e, Reg g) {
            this.e = e;
            this.g = g;
        }
        public int call() {
            int eaa = e.call();
            if ((eaa & 0xFFF) < 0xFFF) {
                int index = Paging.getDirectIndex(eaa);
                if (index>=0) {
                    Memory.host_writew(index, Instructions.SUBW(g.word(), Memory.host_readw(index)));
                    return Constants.BR_Normal;
                }
            }
            Memory.mem_writew(eaa, Instructions.SUBW(g.word(), Memory.mem_readw(eaa)));
            return Constants.BR_Normal;
        }
    }

    final static public class SubGwEw_mem extends Op {
        EaaBase g;
        Reg e;

        public SubGwEw_mem(Reg e, EaaBase g) {
            this.e = e;
            this.g = g;
        }
        public int call() {
            int eaa = g.call();
            e.word(Instructions.SUBW(Memory.mem_readw(eaa), e.word()));
            return Constants.BR_Normal;
        }
    }

    final static public class SubAlIb extends Op {
        short i;

        public SubAlIb(short i) {
            this.i = i;
        }
        public int call() {
            reg_eax.low(Instructions.SUBB(i, reg_eax.low()));
            return Constants.BR_Normal;
        }
    }

    final static public class SubAxIw extends Op {
        int i;

        public SubAxIw(int i) {
            this.i = i;
        }
        public int call() {
            reg_eax.word(Instructions.SUBW(i, reg_eax.word()));
            return Constants.BR_Normal;
        }
    }

    final static public class Xorb_reg extends Op {
        Reg e;
        Reg g;

        public Xorb_reg(Reg e, Reg g) {
            this.e = e;
            this.g = g;
        }

        public int call() {
            e.set8(Instructions.XORB(g.get8(), e.get8()));
            return Constants.BR_Normal;
        }
    }

    final static public class XorEbGb_mem extends Op {
        EaaBase e;
        Reg g;

        public XorEbGb_mem(EaaBase e, Reg g) {
            this.e = e;
            this.g = g;
        }
        public int call() {
            int eaa = e.call();
            int index = Paging.getDirectIndex(eaa);
            if (index>=0)
                Memory.host_writeb(index, Instructions.XORB(g.get8(), Memory.host_readb(index)));
            else
                Memory.mem_writeb(eaa, Instructions.XORB(g.get8(), Memory.mem_readb(eaa)));
            return Constants.BR_Normal;
        }
    }

    final static public class XorGbEb_mem extends Op {
        EaaBase g;
        Reg e;

        public XorGbEb_mem(Reg e, EaaBase g) {
            this.e = e;
            this.g = g;
        }
        public int call() {
            int eaa = g.call();
            e.set8(Instructions.XORB(Memory.mem_readb(eaa), e.get8()));
            return Constants.BR_Normal;
        }
    }

    final static public class Xorw_reg extends Op {
        Reg e;
        Reg g;

        public Xorw_reg(Reg e, Reg g) {
            this.e = e;
            this.g = g;
        }

        public int call() {
            e.word(Instructions.XORW(g.word(), e.word()));
            return Constants.BR_Normal;
        }
    }

    final static public class XorEwGw_mem extends Op {
        EaaBase e;
        Reg g;

        public XorEwGw_mem(EaaBase e, Reg g) {
            this.e = e;
            this.g = g;
        }
        public int call() {
            int eaa = e.call();
            if ((eaa & 0xFFF) < 0xFFF) {
                int index = Paging.getDirectIndex(eaa);
                if (index>=0) {
                    Memory.host_writew(index, Instructions.XORW(g.word(), Memory.host_readw(index)));
                    return Constants.BR_Normal;
                }
            }
            Memory.mem_writew(eaa, Instructions.XORW(g.word(), Memory.mem_readw(eaa)));
            return Constants.BR_Normal;
        }
    }

    final static public class XorGwEw_mem extends Op {
        EaaBase g;
        Reg e;

        public XorGwEw_mem(Reg e, EaaBase g) {
            this.e = e;
            this.g = g;
        }
        public int call() {
            int eaa = g.call();
            e.word(Instructions.XORW(Memory.mem_readw(eaa), e.word()));
            return Constants.BR_Normal;
        }
    }

    final static public class XorAlIb extends Op {
        short i;

        public XorAlIb(short i) {
            this.i = i;
        }
        public int call() {
            reg_eax.low(Instructions.XORB(i, reg_eax.low()));
            return Constants.BR_Normal;
        }
    }

    final static public class XorAxIw extends Op {
        int i;

        public XorAxIw(int i) {
            this.i = i;
        }
        public int call() {
            reg_eax.word(Instructions.XORW(i, reg_eax.word()));
            return Constants.BR_Normal;
        }
    }

    final static public class Cmpb_reg extends Op {
        Reg e;
        Reg g;

        public Cmpb_reg(Reg e, Reg g) {
            this.e = e;
            this.g = g;
        }

        public int call() {
            Instructions.CMPB(g.get8(), e.get8());
            return Constants.BR_Normal;
        }
    }

    final static public class CmpEbGb_mem extends Op {
        EaaBase e;
        Reg g;

        public CmpEbGb_mem(EaaBase e, Reg g) {
            this.e = e;
            this.g = g;
        }
        public int call() {
            int eaa = e.call();
            Instructions.CMPB(g.get8(), Memory.mem_readb(eaa));
            return Constants.BR_Normal;
        }
    }

    final static public class CmpGbEb_mem extends Op {
        EaaBase g;
        Reg e;

        public CmpGbEb_mem(Reg e, EaaBase g) {
            this.e = e;
            this.g = g;
        }
        public int call() {
            int eaa = g.call();
            Instructions.CMPB(Memory.mem_readb(eaa), e.get8());
            return Constants.BR_Normal;
        }
    }

    final static public class Cmpw_reg extends Op {
        Reg e;
        Reg g;

        public Cmpw_reg(Reg e, Reg g) {
            this.e = e;
            this.g = g;
        }

        public int call() {
            Instructions.CMPW(g.word(), e.word());
            return Constants.BR_Normal;
        }
    }

    final static public class CmpEwGw_mem extends Op {
        EaaBase e;
        Reg g;

        public CmpEwGw_mem(EaaBase e, Reg g) {
            this.e = e;
            this.g = g;
        }
        public int call() {
            int eaa = e.call();
            Instructions.CMPW(g.word(), Memory.mem_readw(eaa));
            return Constants.BR_Normal;
        }
    }

    final static public class CmpGwEw_mem extends Op {
        EaaBase g;
        Reg e;

        public CmpGwEw_mem(Reg e, EaaBase g) {
            this.e = e;
            this.g = g;
        }
        public int call() {
            int eaa = g.call();
            Instructions.CMPW(Memory.mem_readw(eaa), e.word());
            return Constants.BR_Normal;
        }
    }

    final static public class CmpAlIb extends Op {
        short i;

        public CmpAlIb(short i) {
            this.i = i;
        }
        public int call() {
            Instructions.CMPB(i, reg_eax.low());
            return Constants.BR_Normal;
        }
    }

    final static public class CmpAxIw extends Op {
        int i;

        public CmpAxIw(int i) {
            this.i = i;
        }
        public int call() {
            Instructions.CMPW(i, reg_eax.word());
            return Constants.BR_Normal;
        }
    }

    final static public class PushES extends Op {
        public int call() {
            CPU.CPU_Push16((int) CPU.Segs_ESval);
            return Constants.BR_Normal;
        }
    }
    final static public class PopES extends Op {
        public int call() {
            if (CPU.CPU_PopSegES(false)) return RUNEXCEPTION();
            return Constants.BR_Normal;
        }
    }
    final static public class PushCS extends Op {
        public int call() {
            CPU.CPU_Push16((int) CPU.Segs_CSval);
            return Constants.BR_Normal;
        }
    }
    final static public class PushSS extends Op {
        public int call() {
            CPU.CPU_Push16((int) CPU.Segs_SSval);
            return Constants.BR_Normal;
        }
    }
    final static public class PopSS extends Op {
        public int call() {
            if (CPU.CPU_PopSegSS(false)) return RUNEXCEPTION();
            Core.base_ss=CPU.Segs_SSphys;
            return Constants.BR_Normal;
        }
    }
    final static public class PushDS extends Op {
        public int call() {
            CPU.CPU_Push16((int) CPU.Segs_DSval);
            return Constants.BR_Normal;
        }
    }
    final static public class PopDS extends Op {
        public int call() {
            if (CPU.CPU_PopSegDS(false)) return RUNEXCEPTION();
            Core.base_ds=CPU.Segs_DSphys;
            Core.base_val_ds= CPU_Regs.ds;
            return Constants.BR_Normal;
        }
    }

    final static public class SegES extends Op {
        public int call() {
            Core.DO_PREFIX_SEG_ES();
            return Constants.BR_Normal;
        }
    }

    final static public class SegCS extends Op {
        public int call() {
            Core.DO_PREFIX_SEG_CS();
            return Constants.BR_Normal;
        }
    }

    final static public class SegSS extends Op {
        public int call() {
            Core.DO_PREFIX_SEG_SS();
            return Constants.BR_Normal;
        }
    }

    final static public class SegDS extends Op {
        public int call() {
            Core.DO_PREFIX_SEG_DS();
            return Constants.BR_Normal;
        }
    }

    final static public class SegFS extends Op {
        public int call() {
            Core.DO_PREFIX_SEG_FS();
            return Constants.BR_Normal;
        }
    }

    final static public class SegGS extends Op {
        public int call() {
            Core.DO_PREFIX_SEG_GS();
            return Constants.BR_Normal;
        }
    }

    final static public class Daa extends Op {
        public int call() {
            Instructions.DAA();
            return Constants.BR_Normal;
        }
    }

    final static public class Das extends Op {
        public int call() {
            Instructions.DAS();
            return Constants.BR_Normal;
        }
    }

    final static public class Aaa extends Op {
        public int call() {
            Instructions.AAA();
            return Constants.BR_Normal;
        }
    }

    final static public class Aas extends Op {
        public int call() {
            Instructions.AAS();
            return Constants.BR_Normal;
        }
    }

    final static public class Incw extends Op {
        Reg reg;
        public Incw(Reg reg) {
            this.reg = reg;
        }
        public int call() {
            LoadCF();
            lf_var1w(reg.word());
            lf_resw(lf_var1w()+1);
            reg.word(lf_resw());
            lflags.type=t_INCw;
            return Constants.BR_Normal;
        }
    }

    final static public class Decw extends Op {
        Reg reg;
        public Decw(Reg reg) {
            this.reg = reg;
        }
        public int call() {
            LoadCF();
            lf_var1w(reg.word());
            lf_resw(lf_var1w()-1);
            reg.word(lf_resw());
            lflags.type=t_DECw;
            return Constants.BR_Normal;
        }
    }

    final static public class Pushw extends Op {
        Reg reg;
        public Pushw(Reg reg) {
            this.reg = reg;
        }
        public int call() {
            CPU.CPU_Push16(reg.word());
            return Constants.BR_Normal;
        }
    }

    final static public class Popw extends Op {
        Reg reg;
        public Popw(Reg reg) {
            this.reg = reg;
        }
        public int call() {
            reg.word(CPU.CPU_Pop16());
            return Constants.BR_Normal;
        }
    }

    final static public class DoStringException extends Op {
        int type;
        int width;
        int prefixes;
        boolean rep_zero;

        public DoStringException(int type, int width) {
            this.type = type;
            this.width = width;
            this.prefixes = Helper.prefixes;
            rep_zero = Core.rep_zero;
        }

        public int call() {
            if (CPU.CPU_IO_Exception(reg_edx.word(),width)) return RUNEXCEPTION();
            Core.rep_zero = rep_zero;
            StringOp.DoString(prefixes, type);
            return Constants.BR_Normal;
        }
    }

    final static public class DoString extends Op {
        int type;
        int prefixes;
        boolean rep_zero;

        public DoString(int type) {
            this.prefixes = Helper.prefixes;
            this.type = type;
            this.rep_zero = Core.rep_zero;
        }
        public int call() {
            Core.rep_zero = rep_zero;
            StringOp.DoString(prefixes, type);
            return Constants.BR_Normal;
        }
    }

    final static public class Pusha extends Op {
        public int call() {
            /*Bit16u*/int old_sp=reg_esp.word();
            CPU.CPU_Push16(reg_eax.word());CPU.CPU_Push16(reg_ecx.word());CPU.CPU_Push16(reg_edx.word());CPU.CPU_Push16(reg_ebx.word());
            CPU.CPU_Push16(old_sp);CPU.CPU_Push16(reg_ebp.word());CPU.CPU_Push16(reg_esi.word());CPU.CPU_Push16(reg_edi.word());
            return Constants.BR_Normal;
        }
    }

    final static public class Popa extends Op {
        public int call() {
            reg_edi.word(CPU.CPU_Pop16());reg_esi.word(CPU.CPU_Pop16());reg_ebp.word(CPU.CPU_Pop16());CPU.CPU_Pop16();//Don't save SP
            reg_ebx.word(CPU.CPU_Pop16());reg_edx.word(CPU.CPU_Pop16());reg_ecx.word(CPU.CPU_Pop16());reg_eax.word(CPU.CPU_Pop16());
            return Constants.BR_Normal;
        }
    }

    final static public class Bound extends Op {
        Reg reg;
        EaaBase get_eaa;
        public Bound() {
            int rm=decode_fetchb();
            reg = Mod.gw(rm);
            get_eaa = Mod.getEaa(rm);
        }
        public int call() {
            short bound_min, bound_max;
            int eaa=get_eaa.call();
            bound_min=(short)Memory.mem_readw(eaa);
            bound_max=(short)Memory.mem_readw(eaa+2);
            if ( (((short)reg.word()) < bound_min) || (((short)reg.word()) > bound_max) ) {
                return EXCEPTION(5);
            }
            return Constants.BR_Normal;
        }
    }

    final static public class ArplEwRw_reg extends Op {
        IntRef ref = new IntRef(0);
        Reg earw;
        Reg rw;

        public ArplEwRw_reg(int rm) {
            earw = Mod.ew(rm);
            rw = Mod.gw(rm);
        }
        public int call() {
            if ((CPU_Regs.flags & CPU_Regs.VM)!=0 || (!CPU.cpu.pmode)) return Constants.BR_Illegal;
            ref.value = earw.word();
            CPU.CPU_ARPL(ref,rw.word());
            earw.word(ref.value);
            return Constants.BR_Normal;
        }
    }

    final static public class ArplEwRw_mem extends Op {
        EaaBase get_eaa;
        IntRef ref = new IntRef(0);
        Reg rw;

        public ArplEwRw_mem(int rm) {
            get_eaa= Mod.getEaa(rm);
            rw = Mod.gw(rm);
        }
        public int call() {
            if ((CPU_Regs.flags & CPU_Regs.VM)!=0 || (!CPU.cpu.pmode)) return Constants.BR_Illegal;
            int eaa=get_eaa.call();

            if ((eaa & 0xFFF) < 0xFFF) {
                int index = Paging.getDirectIndex(eaa);
                if (index>=0) {
                    ref.value = Memory.host_readw(index);
                    CPU.CPU_ARPL(ref,rw.word());
                    Memory.host_writew(index,ref.value);
                    return Constants.BR_Normal;
                }
            }
            ref.value = Memory.mem_readw(eaa);
            CPU.CPU_ARPL(ref,rw.word());
            Memory.mem_writew(eaa,ref.value);


            return Constants.BR_Normal;
        }
    }

    final static public class Push16 extends Op {
        int value;
        public Push16(int value) {
            this.value = value;
        }
        public int call() {
            CPU.CPU_Push16(value);
            return Constants.BR_Normal;
        }
    }

    final static public class IMULGwEwIw_reg extends Op {
        int op3;
        Reg rw;
        Reg earw;

        public IMULGwEwIw_reg(int rm) {
            op3 = decode_fetchws();
            earw = Mod.ew(rm);
            rw = Mod.gw(rm);
        }

        public int call() {
            rw.word(Instructions.DIMULW(earw.word(),op3));
            return Constants.BR_Normal;
        }
    }

    final static public class IMULGwEwIw_mem extends Op {
        int op3;
        Reg rw;
        EaaBase get_eaa;

        public IMULGwEwIw_mem(int rm) {
            get_eaa= Mod.getEaa(rm);
            op3 = decode_fetchws();
            rw = Mod.gw(rm);
        }

        public int call() {
            int eaa = get_eaa.call();
            rw.word(Instructions.DIMULW(Memory.mem_readw(eaa),op3));
            return Constants.BR_Normal;
        }
    }

    final static public class IMULGwEwIb_reg extends Op {
        int op3;
        Reg rw;
        Reg earw;

        public IMULGwEwIb_reg(int rm) {
            op3 = decode_fetchbs();
            earw = Mod.ew(rm);
            rw = Mod.gw(rm);
        }

        public int call() {
            rw.word(Instructions.DIMULW(earw.word(),op3));
            return Constants.BR_Normal;
        }
    }

    final static public class IMULGwEwIb_mem extends Op {
        int op3;
        Reg rw;
        EaaBase get_eaa;

        public IMULGwEwIb_mem(int rm) {
            get_eaa= Mod.getEaa(rm);
            op3 = decode_fetchbs();
            rw = Mod.gw(rm);
        }

        public int call() {
            int eaa = get_eaa.call();
            rw.word(Instructions.DIMULW(Memory.mem_readw(eaa),op3));
            return Constants.BR_Normal;
        }
    }

    static abstract public class JumpCond16_b extends Op {
        int offset;
        public JumpCond16_b() {
            offset = decode_fetchbs();
        }

        final protected int jump(boolean COND, int off) {
            if (COND) {
                reg_ip(reg_ip()+off+(int)eip_count);
                return Constants.BR_Link1;
            }
            reg_ip(reg_ip()+(int)eip_count);
            return Constants.BR_Link2;
        }
    }

    final static public class JumpCond16_b_o extends JumpCond16_b {
        public int call() {
            return jump(Flags.TFLG_O(), offset);
        }
    }

    final static public class JumpCond16_b_no extends JumpCond16_b {
        public int call() {
            return jump(Flags.TFLG_NO(), offset);
        }
    }

    final static public class JumpCond16_b_b extends JumpCond16_b {
        public int call() {
            return jump(Flags.TFLG_B(), offset);
        }
    }

    final static public class JumpCond16_b_nb extends JumpCond16_b {
        public int call() {
            return jump(Flags.TFLG_NB(), offset);
        }
    }

    final static public class JumpCond16_b_z extends JumpCond16_b {
        public int call() {
            return jump(Flags.TFLG_Z(), offset);
        }
    }

    final static public class JumpCond16_b_nz extends JumpCond16_b {
        public int call() {
            return jump(Flags.TFLG_NZ(), offset);
        }
    }

    final static public class JumpCond16_b_be extends JumpCond16_b {
        public int call() {
            return jump(Flags.TFLG_BE(), offset);
        }
    }

    final static public class JumpCond16_b_nbe extends JumpCond16_b {
        public int call() {
            return jump(Flags.TFLG_NBE(), offset);
        }
    }

    final static public class JumpCond16_b_s extends JumpCond16_b {
        public int call() {
            return jump(Flags.TFLG_S(), offset);
        }
    }

    final static public class JumpCond16_b_ns extends JumpCond16_b {
        public int call() {
            return jump(Flags.TFLG_NS(), offset);
        }
    }

    final static public class JumpCond16_b_p extends JumpCond16_b {
        public int call() {
            return jump(Flags.TFLG_P(), offset);
        }
    }

    final static public class JumpCond16_b_np extends JumpCond16_b {
        public int call() {
            return jump(Flags.TFLG_NP(), offset);
        }
    }

    final static public class JumpCond16_b_l extends JumpCond16_b {
        public int call() {
            return jump(Flags.TFLG_L(), offset);
        }
    }

    final static public class JumpCond16_b_nl extends JumpCond16_b {
        public int call() {
            return jump(Flags.TFLG_NL(), offset);
        }
    }

    final static public class JumpCond16_b_le extends JumpCond16_b {
        public int call() {
            return jump(Flags.TFLG_LE(), offset);
        }
    }

    final static public class JumpCond16_b_nle extends JumpCond16_b {
        public int call() {
            return jump(Flags.TFLG_NLE(), offset);
        }
    }

    final static public class GrplEbIb_reg_add extends Op {
        Reg earb;
        short ib;

        public GrplEbIb_reg_add(int rm) {
            earb = Mod.eb(rm);
            ib=decode_fetchb();
        }
        public int call() {
            earb.set8(Instructions.ADDB(ib,earb.get8()));
            return Constants.BR_Normal;
        }
    }

    final static public class GrplEbIb_reg_or extends Op {
        Reg earb;
        short ib;

        public GrplEbIb_reg_or(int rm) {
            earb = Mod.eb(rm);
            ib=decode_fetchb();
        }
        public int call() {
            earb.set8(Instructions.ORB(ib,earb.get8()));
            return Constants.BR_Normal;
        }
    }

    final static public class GrplEbIb_reg_adc extends Op {
        Reg earb;
        short ib;

        public GrplEbIb_reg_adc(int rm) {
            earb = Mod.eb(rm);
            ib=decode_fetchb();
        }
        public int call() {
            earb.set8(Instructions.ADCB(ib,earb.get8()));
            return Constants.BR_Normal;
        }
    }

    final static public class GrplEbIb_reg_sbb extends Op {
        Reg earb;
        short ib;

        public GrplEbIb_reg_sbb(int rm) {
            earb = Mod.eb(rm);
            ib=decode_fetchb();
        }
        public int call() {
            earb.set8(Instructions.SBBB(ib,earb.get8()));
            return Constants.BR_Normal;
        }
    }

    final static public class GrplEbIb_reg_and extends Op {
        Reg earb;
        short ib;

        public GrplEbIb_reg_and(int rm) {
            earb = Mod.eb(rm);
            ib=decode_fetchb();
        }
        public int call() {
            earb.set8(Instructions.ANDB(ib,earb.get8()));
            return Constants.BR_Normal;
        }
    }

    final static public class GrplEbIb_reg_sub extends Op {
        Reg earb;
        short ib;

        public GrplEbIb_reg_sub(int rm) {
            earb = Mod.eb(rm);
            ib=decode_fetchb();
        }
        public int call() {
            earb.set8(Instructions.SUBB(ib,earb.get8()));
            return Constants.BR_Normal;
        }
    }

    final static public class GrplEbIb_reg_xor extends Op {
        Reg earb;
        short ib;

        public GrplEbIb_reg_xor(int rm) {
            earb = Mod.eb(rm);
            ib=decode_fetchb();
        }
        public int call() {
            earb.set8(Instructions.XORB(ib,earb.get8()));
            return Constants.BR_Normal;
        }
    }

    final static public class GrplEbIb_reg_cmp extends Op {
        Reg earb;
        short ib;

        public GrplEbIb_reg_cmp(int rm) {
            earb = Mod.eb(rm);
            ib=decode_fetchb();
        }
        public int call() {
            Instructions.CMPB(ib,earb.get8());
            return Constants.BR_Normal;
        }
    }

    final static public class GrplEbIb_mem_add extends Op {
        short ib;
        EaaBase get_eaa;

        public GrplEbIb_mem_add(int rm) {
            get_eaa= Mod.getEaa(rm);
            ib=decode_fetchb();
        }
        public int call() {
            int eaa = get_eaa.call();
            int index = Paging.getDirectIndex(eaa);
            if (index>=0)
                Memory.host_writeb(index, Instructions.ADDB(ib,Memory.host_readb(index)));
            else
                Memory.mem_writeb(eaa, Instructions.ADDB(ib,Memory.mem_readb(eaa)));
            return Constants.BR_Normal;
        }
    }

    final static public class GrplEbIb_mem_or extends Op {
        short ib;
        EaaBase get_eaa;

        public GrplEbIb_mem_or(int rm) {
            get_eaa= Mod.getEaa(rm);
            ib=decode_fetchb();
        }
        public int call() {
            int eaa = get_eaa.call();
            int index = Paging.getDirectIndex(eaa);
            if (index>=0)
                Memory.host_writeb(index, Instructions.ORB(ib,Memory.host_readb(index)));
            else
                Memory.mem_writeb(eaa, Instructions.ORB(ib,Memory.mem_readb(eaa)));
            return Constants.BR_Normal;
        }
    }

    final static public class GrplEbIb_mem_adc extends Op {
        short ib;
        EaaBase get_eaa;

        public GrplEbIb_mem_adc(int rm) {
            get_eaa= Mod.getEaa(rm);
            ib=decode_fetchb();
        }
        public int call() {
            int eaa = get_eaa.call();
            int index = Paging.getDirectIndex(eaa);
            if (index>=0)
                Memory.host_writeb(index, Instructions.ADCB(ib,Memory.host_readb(index)));
            else
                Memory.mem_writeb(eaa, Instructions.ADCB(ib,Memory.mem_readb(eaa)));
            return Constants.BR_Normal;
        }
    }

    final static public class GrplEbIb_mem_sbb extends Op {
        short ib;
        EaaBase get_eaa;

        public GrplEbIb_mem_sbb(int rm) {
            get_eaa= Mod.getEaa(rm);
            ib=decode_fetchb();
        }
        public int call() {
            int eaa = get_eaa.call();
            int index = Paging.getDirectIndex(eaa);
            if (index>=0)
                Memory.host_writeb(index, Instructions.SBBB(ib,Memory.host_readb(index)));
            else
                Memory.mem_writeb(eaa, Instructions.SBBB(ib,Memory.mem_readb(eaa)));
            return Constants.BR_Normal;
        }
    }

    final static public class GrplEbIb_mem_and extends Op {
        short ib;
        EaaBase get_eaa;

        public GrplEbIb_mem_and(int rm) {
            get_eaa= Mod.getEaa(rm);
            ib=decode_fetchb();
        }
        public int call() {
            int eaa = get_eaa.call();
            int index = Paging.getDirectIndex(eaa);
            if (index>=0)
                Memory.host_writeb(index, Instructions.ANDB(ib,Memory.host_readb(index)));
            else
                Memory.mem_writeb(eaa, Instructions.ANDB(ib,Memory.mem_readb(eaa)));
            return Constants.BR_Normal;
        }
    }

    final static public class GrplEbIb_mem_sub extends Op {
        short ib;
        EaaBase get_eaa;

        public GrplEbIb_mem_sub(int rm) {
            get_eaa= Mod.getEaa(rm);
            ib=decode_fetchb();
        }
        public int call() {
            int eaa = get_eaa.call();
            int index = Paging.getDirectIndex(eaa);
            if (index>=0)
                Memory.host_writeb(index, Instructions.SUBB(ib,Memory.host_readb(index)));
            else
                Memory.mem_writeb(eaa, Instructions.SUBB(ib,Memory.mem_readb(eaa)));
            return Constants.BR_Normal;
        }
    }

    final static public class GrplEbIb_mem_xor extends Op {
        short ib;
        EaaBase get_eaa;

        public GrplEbIb_mem_xor(int rm) {
            get_eaa= Mod.getEaa(rm);
            ib=decode_fetchb();
        }
        public int call() {
            int eaa = get_eaa.call();
            int index = Paging.getDirectIndex(eaa);
            if (index>=0)
                Memory.host_writeb(index, Instructions.XORB(ib,Memory.host_readb(index)));
            else
                Memory.mem_writeb(eaa, Instructions.XORB(ib,Memory.mem_readb(eaa)));
            return Constants.BR_Normal;
        }
    }

    final static public class GrplEbIb_mem_cmp extends Op {
        short ib;
        EaaBase get_eaa;

        public GrplEbIb_mem_cmp(int rm) {
            get_eaa= Mod.getEaa(rm);
            ib=decode_fetchb();
        }
        public int call() {
            int eaa = get_eaa.call();
            Instructions.CMPB(ib,Memory.mem_readb(eaa));
            return Constants.BR_Normal;
        }
    }

    final static public class GrplEwIw_reg_add extends Op {
        Reg earw;
        int ib;

        public GrplEwIw_reg_add(int rm, boolean signed) {
            earw = Mod.ew(rm);
            if (signed)
                ib = (((short)decode_fetchbs()) & 0xFFFF);
            else
                ib = decode_fetchw();
        }
        public int call() {
            earw.word(Instructions.ADDW(ib,earw.word()));
            return Constants.BR_Normal;
        }
    }

    final static public class GrplEwIw_reg_or extends Op {
        Reg earw;
        int ib;

        public GrplEwIw_reg_or(int rm, boolean signed) {
            earw = Mod.ew(rm);
            if (signed)
                ib = (((short)decode_fetchbs()) & 0xFFFF);
            else
                ib = decode_fetchw();
        }
        public int call() {
            earw.word(Instructions.ORW(ib,earw.word()));
            return Constants.BR_Normal;
        }
    }

    final static public class GrplEwIw_reg_adc extends Op {
        Reg earw;
        int ib;

        public GrplEwIw_reg_adc(int rm, boolean signed) {
            earw = Mod.ew(rm);
            if (signed)
                ib = (((short)decode_fetchbs()) & 0xFFFF);
            else
                ib = decode_fetchw();
        }
        public int call() {
            earw.word(Instructions.ADCW(ib,earw.word()));
            return Constants.BR_Normal;
        }
    }

    final static public class GrplEwIw_reg_sbb extends Op {
        Reg earw;
        int ib;

        public GrplEwIw_reg_sbb(int rm, boolean signed) {
            earw = Mod.ew(rm);
            if (signed)
                ib = (((short)decode_fetchbs()) & 0xFFFF);
            else
                ib = decode_fetchw();
        }
        public int call() {
            earw.word(Instructions.SBBW(ib,earw.word()));
            return Constants.BR_Normal;
        }
    }

    final static public class GrplEwIw_reg_and extends Op {
        Reg earw;
        int ib;

        public GrplEwIw_reg_and(int rm, boolean signed) {
            earw = Mod.ew(rm);
            if (signed)
                ib = (((short)decode_fetchbs()) & 0xFFFF);
            else
                ib = decode_fetchw();
        }
        public int call() {
            earw.word(Instructions.ANDW(ib,earw.word()));
            return Constants.BR_Normal;
        }
    }

    final static public class GrplEwIw_reg_sub extends Op {
        Reg earw;
        int ib;

        public GrplEwIw_reg_sub(int rm, boolean signed) {
            earw = Mod.ew(rm);
            if (signed)
                ib = (((short)decode_fetchbs()) & 0xFFFF);
            else
                ib = decode_fetchw();
        }
        public int call() {
            earw.word(Instructions.SUBW(ib,earw.word()));
            return Constants.BR_Normal;
        }
    }

    final static public class GrplEwIw_reg_xor extends Op {
        Reg earw;
        int ib;

        public GrplEwIw_reg_xor(int rm, boolean signed) {
            earw = Mod.ew(rm);
            if (signed)
                ib = (((short)decode_fetchbs()) & 0xFFFF);
            else
                ib = decode_fetchw();
        }
        public int call() {
            earw.word(Instructions.XORW(ib,earw.word()));
            return Constants.BR_Normal;
        }
    }

    final static public class GrplEwIw_reg_cmp extends Op {
        Reg earw;
        int ib;

        public GrplEwIw_reg_cmp(int rm, boolean signed) {
            earw = Mod.ew(rm);
            if (signed)
                ib = (((short)decode_fetchbs()) & 0xFFFF);
            else
                ib = decode_fetchw();
        }
        public int call() {
            Instructions.CMPW(ib,earw.word());
            return Constants.BR_Normal;
        }
    }

    final static public class GrplEwIw_mem_add extends Op {
        int ib;
        EaaBase get_eaa;

        public GrplEwIw_mem_add(int rm, boolean signed) {
            get_eaa= Mod.getEaa(rm);
            if (signed)
                ib = (((short)decode_fetchbs()) & 0xFFFF);
            else
                ib = decode_fetchw();
        }
        public int call() {
            int eaa = get_eaa.call();
            if ((eaa & 0xFFF)<0xFFF) {
                int index = Paging.getDirectIndex(eaa);
                if (index>=0) {
                    Memory.host_writew(index, Instructions.ADDW(ib,Memory.host_readw(index)));
                    return Constants.BR_Normal;
                }
            }
            Memory.mem_writew(eaa, Instructions.ADDW(ib,Memory.mem_readw(eaa)));
            return Constants.BR_Normal;
        }
    }

    final static public class GrplEwIw_mem_or extends Op {
        int ib;
        EaaBase get_eaa;

        public GrplEwIw_mem_or(int rm, boolean signed) {
            get_eaa= Mod.getEaa(rm);
            if (signed)
                ib = (((short)decode_fetchbs()) & 0xFFFF);
            else
                ib = decode_fetchw();
        }
        public int call() {
            int eaa = get_eaa.call();
            if ((eaa & 0xFFF)<0xFFF) {
                int index = Paging.getDirectIndex(eaa);
                if (index>=0) {
                    Memory.host_writew(index, Instructions.ORW(ib,Memory.host_readw(index)));
                    return Constants.BR_Normal;
                }
            }
            Memory.mem_writew(eaa, Instructions.ORW(ib,Memory.mem_readw(eaa)));
            return Constants.BR_Normal;
        }
    }

    final static public class GrplEwIw_mem_adc extends Op {
        int ib;
        EaaBase get_eaa;

        public GrplEwIw_mem_adc(int rm, boolean signed) {
            get_eaa= Mod.getEaa(rm);
            if (signed)
                ib = (((short)decode_fetchbs()) & 0xFFFF);
            else
                ib = decode_fetchw();
        }
        public int call() {
            int eaa = get_eaa.call();
            if ((eaa & 0xFFF)<0xFFF) {
                int index = Paging.getDirectIndex(eaa);
                if (index>=0) {
                    Memory.host_writew(index, Instructions.ADCW(ib,Memory.host_readw(index)));
                    return Constants.BR_Normal;
                }
            }
            Memory.mem_writew(eaa, Instructions.ADCW(ib,Memory.mem_readw(eaa)));
            return Constants.BR_Normal;
        }
    }

    final static public class GrplEwIw_mem_sbb extends Op {
        int ib;
        EaaBase get_eaa;

        public GrplEwIw_mem_sbb(int rm, boolean signed) {
            get_eaa= Mod.getEaa(rm);
            if (signed)
                ib = (((short)decode_fetchbs()) & 0xFFFF);
            else
                ib = decode_fetchw();
        }
        public int call() {
            int eaa = get_eaa.call();
            if ((eaa & 0xFFF)<0xFFF) {
                int index = Paging.getDirectIndex(eaa);
                if (index>=0) {
                    Memory.host_writew(index, Instructions.SBBW(ib,Memory.host_readw(index)));
                    return Constants.BR_Normal;
                }
            }
            Memory.mem_writew(eaa, Instructions.SBBW(ib,Memory.mem_readw(eaa)));
            return Constants.BR_Normal;
        }
    }

    final static public class GrplEwIw_mem_and extends Op {
        int ib;
        EaaBase get_eaa;

        public GrplEwIw_mem_and(int rm, boolean signed) {
            get_eaa= Mod.getEaa(rm);
            if (signed)
                ib = (((short)decode_fetchbs()) & 0xFFFF);
            else
                ib = decode_fetchw();
        }
        public int call() {
            int eaa = get_eaa.call();
            if ((eaa & 0xFFF)<0xFFF) {
                int index = Paging.getDirectIndex(eaa);
                if (index>=0) {
                    Memory.host_writew(index, Instructions.ANDW(ib,Memory.host_readw(index)));
                    return Constants.BR_Normal;
                }
            }
            Memory.mem_writew(eaa, Instructions.ANDW(ib,Memory.mem_readw(eaa)));
            return Constants.BR_Normal;
        }
    }

    final static public class GrplEwIw_mem_sub extends Op {
        int ib;
        EaaBase get_eaa;

        public GrplEwIw_mem_sub(int rm, boolean signed) {
            get_eaa= Mod.getEaa(rm);
            if (signed)
                ib = (((short)decode_fetchbs()) & 0xFFFF);
            else
                ib = decode_fetchw();
        }
        public int call() {
            int eaa = get_eaa.call();
            if ((eaa & 0xFFF)<0xFFF) {
                int index = Paging.getDirectIndex(eaa);
                if (index>=0) {
                    Memory.host_writew(index, Instructions.SUBW(ib,Memory.host_readw(index)));
                    return Constants.BR_Normal;
                }
            }
            Memory.mem_writew(eaa, Instructions.SUBW(ib,Memory.mem_readw(eaa)));
            return Constants.BR_Normal;
        }
    }

    final static public class GrplEwIw_mem_xor extends Op {
        int ib;
        EaaBase get_eaa;

        public GrplEwIw_mem_xor(int rm, boolean signed) {
            get_eaa= Mod.getEaa(rm);
            if (signed)
                ib = (((short)decode_fetchbs()) & 0xFFFF);
            else
                ib = decode_fetchw();
        }
        public int call() {
            int eaa = get_eaa.call();
            if ((eaa & 0xFFF)<0xFFF) {
                int index = Paging.getDirectIndex(eaa);
                if (index>=0) {
                    Memory.host_writew(index, Instructions.XORW(ib,Memory.host_readw(index)));
                    return Constants.BR_Normal;
                }
            }
            Memory.mem_writew(eaa, Instructions.XORW(ib,Memory.mem_readw(eaa)));
            return Constants.BR_Normal;
        }
    }

    final static public class GrplEwIw_mem_cmp extends Op {
        int ib;
        EaaBase get_eaa;

        public GrplEwIw_mem_cmp(int rm, boolean signed) {
            get_eaa= Mod.getEaa(rm);
            if (signed)
                ib = (((short)decode_fetchbs()) & 0xFFFF);
            else
                ib = decode_fetchw();
        }
        public int call() {
            int eaa = get_eaa.call();
            Instructions.CMPW(ib,Memory.mem_readw(eaa));
            return Constants.BR_Normal;
        }
    }

    final static public class TestEbGb_reg extends Op {
        Reg rb;
        Reg earb;

        public TestEbGb_reg(int rm) {
            rb = Mod.gb(rm);
            earb = Mod.eb(rm);
        }
        public int call() {
            Instructions.TESTB(rb.get8(),earb.get8());
            return Constants.BR_Normal;
        }
    }

    final static public class TestEbGb_mem extends Op {
        Reg rb;
        EaaBase get_eaa;

        public TestEbGb_mem(int rm) {
            rb = Mod.gb(rm);
            get_eaa= Mod.getEaa(rm);
        }
        public int call() {
            int eaa = get_eaa.call();
            Instructions.TESTB(rb.get8(),Memory.mem_readb(eaa));
            return Constants.BR_Normal;
        }
    }

    final static public class TestEwGw_reg extends Op {
        Reg rw;
        Reg earw;

        public TestEwGw_reg(int rm) {
            rw = Mod.gw(rm);
            earw = Mod.ew(rm);
        }
        public int call() {
            Instructions.TESTW(rw.word(),earw.word());
            return Constants.BR_Normal;
        }
    }

    final static public class TestEwGw_mem extends Op {
        Reg rw;
        EaaBase get_eaa;

        public TestEwGw_mem(int rm) {
            rw = Mod.gw(rm);
            get_eaa= Mod.getEaa(rm);
        }
        public int call() {
            int eaa = get_eaa.call();
            Instructions.TESTW(rw.word(),Memory.mem_readw(eaa));
            return Constants.BR_Normal;
        }
    }

    final static public class XchgEbGb_reg extends Op {
        Reg rb;
        Reg earb;

        public XchgEbGb_reg(int rm) {
            rb = Mod.gb(rm);
            earb = Mod.eb(rm);
        }

        public int call() {
            short oldrmrb=rb.get8();
            rb.set8(earb.get8());
            earb.set8(oldrmrb);
            return Constants.BR_Normal;
        }
    }

    final static public class XchgEbGb_mem extends Op {
        Reg rb;
        EaaBase get_eaa;
        public XchgEbGb_mem(int rm) {
            rb = Mod.gb(rm);
            get_eaa= Mod.getEaa(rm);
        }

        public int call() {
            short oldrmrb=rb.get8();
            int eaa = get_eaa.call();
            int index = Paging.getDirectIndex(eaa);
            if (index>=0) {
                short newrb = Memory.host_readb(index);
                Memory.host_writeb(index,oldrmrb);
                rb.set8(newrb);
            } else {
                short newrb = Memory.mem_readb(eaa);
                Memory.mem_writeb(eaa,oldrmrb);
                rb.set8(newrb);
            }
            return Constants.BR_Normal;
        }
    }

    final static public class XchgEwGw_reg extends Op {
        Reg rw;
        Reg earw;

        public XchgEwGw_reg(int rm) {
            rw = Mod.gw(rm);
            earw = Mod.ew(rm);
        }

        public int call() {
            int oldrmrw=rw.word();
            rw.word(earw.word());
            earw.word(oldrmrw);
            return Constants.BR_Normal;
        }
    }

    final static public class XchgEwGw_mem extends Op {
        Reg rw;
        EaaBase get_eaa;
        public XchgEwGw_mem(int rm) {
            rw = Mod.gw(rm);
            get_eaa= Mod.getEaa(rm);
        }

        public int call() {
            int oldrmrw=rw.word();
            int eaa = get_eaa.call();
            if ((eaa & 0xFFF) < 0xFFF) {
                int index = Paging.getDirectIndex(eaa);
                if (index>=0) {
                    int newrw = Memory.host_readw(index);
                    Memory.host_writew(index,oldrmrw);
                    rw.word(newrw);
                    return Constants.BR_Normal;
                }
            }
            int newrw = Memory.mem_readw(eaa);
            Memory.mem_writew(eaa,oldrmrw);
            rw.word(newrw);
            return Constants.BR_Normal;
        }
    }

    final static public class MovEbGb_reg extends Op {
        Reg rb;
        Reg earb;

        public MovEbGb_reg(int rm) {
            rb = Mod.gb(rm);
            earb = Mod.eb(rm);
        }

        public int call() {
            earb.set8(rb.get8());
            return Constants.BR_Normal;
        }
    }

    final static public class MovEbGb_mem extends Op {
        Reg rb;
        EaaBase get_eaa;
        public MovEbGb_mem(int rm) {
            rb = Mod.gb(rm);
            get_eaa= Mod.getEaa(rm);
        }

        public int call() {
            int eaa = get_eaa.call();
            Memory.mem_writeb(eaa,rb.get8());
            return Constants.BR_Normal;
        }
    }

    static final CPU.Descriptor desc=new CPU.Descriptor();

    final static public class MovEbGb_mem_5 extends Op {
        Reg rb;
        EaaBase get_eaa;
        public MovEbGb_mem_5(int rm) {
            rb = Mod.gb(rm);
            get_eaa= Mod.getEaa(rm);
        }

        public int call() {
            if (CPU.cpu.pmode && !CPU.cpu.code.big) {
                CPU.cpu.gdt.GetDescriptor((int)CPU.seg_value(Core.base_val_ds),desc);
                if ((desc.Type()==CPU.DESC_CODE_R_NC_A) || (desc.Type()==CPU.DESC_CODE_R_NC_NA)) {
                    CPU.CPU_Exception(CPU.EXCEPTION_GP,(int)CPU.seg_value(Core.base_val_ds) & 0xfffc);
                    return Constants.BR_Jump;
                }
            }
            int eaa = get_eaa.call();
            Memory.mem_writeb(eaa,rb.get8());
            return Constants.BR_Normal;
        }
    }

    final static public class MovEwGw_reg extends Op {
        Reg rw;
        Reg earw;

        public MovEwGw_reg(int rm) {
            rw = Mod.gw(rm);
            earw = Mod.ew(rm);
        }

        public int call() {
            earw.word(rw.word());
            return Constants.BR_Normal;
        }
    }

    final static public class MovEwGw_mem extends Op {
        Reg rw;
        EaaBase get_eaa;
        public MovEwGw_mem(int rm) {
            rw = Mod.gw(rm);
            get_eaa= Mod.getEaa(rm);
        }

        public int call() {
            int eaa = get_eaa.call();
            Memory.mem_writew(eaa,rw.word());
            return Constants.BR_Normal;
        }
    }

    final static public class MovGbEb_reg extends Op {
        Reg rb;
        Reg earb;

        public MovGbEb_reg(int rm) {
            rb = Mod.gb(rm);
            earb = Mod.eb(rm);
        }

        public int call() {
            rb.set8(earb.get8());
            return Constants.BR_Normal;
        }
    }

    final static public class MovGbEb_mem extends Op {
        Reg rb;
        EaaBase get_eaa;
        public MovGbEb_mem(int rm) {
            rb = Mod.gb(rm);
            get_eaa= Mod.getEaa(rm);
        }

        public int call() {
            int eaa = get_eaa.call();
            rb.set8(Memory.mem_readb(eaa));
            return Constants.BR_Normal;
        }
    }

    final static public class MovGwEw_reg extends Op {
        Reg rw;
        Reg earw;

        public MovGwEw_reg(int rm) {
            rw = Mod.gw(rm);
            earw = Mod.ew(rm);
        }

        public int call() {
            rw.word(earw.word());
            return Constants.BR_Normal;
        }
    }

    final static public class MovGwEw_mem extends Op {
        Reg rw;
        EaaBase get_eaa;
        public MovGwEw_mem(int rm) {
            rw = Mod.gw(rm);
            get_eaa= Mod.getEaa(rm);
        }

        public int call() {
            int eaa = get_eaa.call();
            rw.word(Memory.mem_readw(eaa));
            return Constants.BR_Normal;
        }
    }

    final static public class MovEwEs_reg extends Op {
        Reg earw;
        public MovEwEs_reg(int rm) {
            earw = Mod.ew(rm);
        }

        public int call() {
            earw.word((int)CPU.Segs_ESval);
            return Constants.BR_Normal;
        }
    }

    final static public class MovEwEs_mem extends Op {
        EaaBase get_eaa;
        public MovEwEs_mem(int rm) {
            get_eaa= Mod.getEaa(rm);
        }

        public int call() {
            int eaa = get_eaa.call();
            Memory.mem_writew(eaa,(int)CPU.Segs_ESval);
            return Constants.BR_Normal;
        }
    }

    final static public class MovEwCs_reg extends Op {
        Reg earw;
        public MovEwCs_reg(int rm) {
            earw = Mod.ew(rm);
        }

        public int call() {
            earw.word((int)CPU.Segs_CSval);
            return Constants.BR_Normal;
        }
    }

    final static public class MovEwCs_mem extends Op {
        EaaBase get_eaa;
        public MovEwCs_mem(int rm) {
            get_eaa= Mod.getEaa(rm);
        }

        public int call() {
            int eaa = get_eaa.call();
            Memory.mem_writew(eaa,(int)CPU.Segs_CSval);
            return Constants.BR_Normal;
        }
    }

    final static public class MovEwSs_reg extends Op {
        Reg earw;
        public MovEwSs_reg(int rm) {
            earw = Mod.ew(rm);
        }

        public int call() {
            earw.word((int)CPU.Segs_SSval);
            return Constants.BR_Normal;
        }
    }

    final static public class MovEwSs_mem extends Op {
        EaaBase get_eaa;
        public MovEwSs_mem(int rm) {
            get_eaa= Mod.getEaa(rm);
        }

        public int call() {
            int eaa = get_eaa.call();
            Memory.mem_writew(eaa,(int)CPU.Segs_SSval);
            return Constants.BR_Normal;
        }
    }

    final static public class MovEwDs_reg extends Op {
        Reg earw;
        public MovEwDs_reg(int rm) {
            earw = Mod.ew(rm);
        }

        public int call() {
            earw.word((int)CPU.Segs_DSval);
            return Constants.BR_Normal;
        }
    }

    final static public class MovEwDs_mem extends Op {
        EaaBase get_eaa;
        public MovEwDs_mem(int rm) {
            get_eaa= Mod.getEaa(rm);
        }

        public int call() {
            int eaa = get_eaa.call();
            Memory.mem_writew(eaa,(int)CPU.Segs_DSval);
            return Constants.BR_Normal;
        }
    }

    final static public class MovEwFs_reg extends Op {
        Reg earw;
        public MovEwFs_reg(int rm) {
            earw = Mod.ew(rm);
        }

        public int call() {
            earw.word((int)CPU.Segs_FSval);
            return Constants.BR_Normal;
        }
    }

    final static public class MovEwFs_mem extends Op {
        EaaBase get_eaa;
        public MovEwFs_mem(int rm) {
            get_eaa= Mod.getEaa(rm);
        }

        public int call() {
            int eaa = get_eaa.call();
            Memory.mem_writew(eaa,(int)CPU.Segs_FSval);
            return Constants.BR_Normal;
        }
    }

    final static public class MovEwGs_reg extends Op {
        Reg earw;
        public MovEwGs_reg(int rm) {
            earw = Mod.ew(rm);
        }

        public int call() {
            earw.word((int)CPU.Segs_GSval);
            return Constants.BR_Normal;
        }
    }

    final static public class MovEwGs_mem extends Op {
        EaaBase get_eaa;
        public MovEwGs_mem(int rm) {
            get_eaa= Mod.getEaa(rm);
        }

        public int call() {
            int eaa = get_eaa.call();
            Memory.mem_writew(eaa,(int)CPU.Segs_GSval);
            return Constants.BR_Normal;
        }
    }

    final static public class Illegal extends Op {
        String msg;
        public Illegal(String msg) {
            this.msg = msg;
        }
        public int call() {
            Log.log(LogTypes.LOG_CPU, LogSeverities.LOG_ERROR,msg);
            return Constants.BR_Illegal;
        }
    }

    final static public class LeaGw_16 extends Op {
        Reg rw;
        EaaBase get_eaa;

        public LeaGw_16(int rm) {
            rw = Mod.gw(rm);
            get_eaa= Mod.getEaa16(rm);
        }
        public int call() {
            //Little hack to always use segprefixed version
            Core.base_ds=Core.base_ss=0;
            int eaa = get_eaa.call();
            rw.word((int)(eaa));
            Core.base_ds=CPU.Segs_DSphys;
            Core.base_ss=CPU.Segs_SSphys;
            Core.base_val_ds= CPU_Regs.ds;
            return Constants.BR_Normal;
        }
    }

    final static public class LeaGw_32 extends Op {
        Reg rw;
        EaaBase get_eaa;

        public LeaGw_32(int rm) {
            rw = Mod.gw(rm);
            get_eaa= Mod.getEaa32(rm);
        }
        public int call() {
            //Little hack to always use segprefixed version
            Core.base_ds=Core.base_ss=0;
            int eaa = get_eaa.call();
            rw.word((int)(eaa));
            Core.base_ds=CPU.Segs_DSphys;
            Core.base_ss=CPU.Segs_SSphys;
            Core.base_val_ds= CPU_Regs.ds;
            return Constants.BR_Normal;
        }
    }

    final static public class MovEsEw_reg extends Op {
        Reg earw;
        public MovEsEw_reg(int rm) {
            earw = Mod.ew(rm);
        }

        public int call() {
            if (CPU.CPU_SetSegGeneralES(earw.word())) return RUNEXCEPTION();
            return Constants.BR_Normal;
        }
    }

    final static public class MovEsEw_mem extends Op {
        EaaBase get_eaa;
        public MovEsEw_mem(int rm) {
            get_eaa= Mod.getEaa(rm);
        }

        public int call() {
            int eaa = get_eaa.call();
            if (CPU.CPU_SetSegGeneralES(Memory.mem_readw(eaa))) return RUNEXCEPTION();
            return Constants.BR_Normal;
        }
    }

    final static public class MovSsEw_reg extends Op {
        Reg earw;
        public MovSsEw_reg(int rm) {
            earw = Mod.ew(rm);
        }

        public int call() {
            if (CPU.CPU_SetSegGeneralSS(earw.word())) return RUNEXCEPTION();
            Core.base_ss=CPU.Segs_SSphys;
            return Constants.BR_Normal;
        }
    }

    final static public class MovSsEw_mem extends Op {
        EaaBase get_eaa;
        public MovSsEw_mem(int rm) {
            get_eaa= Mod.getEaa(rm);
        }

        public int call() {
            int eaa = get_eaa.call();
            if (CPU.CPU_SetSegGeneralSS(Memory.mem_readw(eaa))) return RUNEXCEPTION();
            Core.base_ss=CPU.Segs_SSphys;
            return Constants.BR_Normal;
        }
    }

    final static public class MovDsEw_reg extends Op {
        Reg earw;
        public MovDsEw_reg(int rm) {
            earw = Mod.ew(rm);
        }

        public int call() {
            if (CPU.CPU_SetSegGeneralDS(earw.word())) return RUNEXCEPTION();
            Core.base_ds=CPU.Segs_DSphys;
            Core.base_val_ds= CPU_Regs.ds;
            return Constants.BR_Normal;
        }
    }

    final static public class MovDsEw_mem extends Op {
        EaaBase get_eaa;
        public MovDsEw_mem(int rm) {
            get_eaa= Mod.getEaa(rm);
        }

        public int call() {
            int eaa = get_eaa.call();
            if (CPU.CPU_SetSegGeneralDS(Memory.mem_readw(eaa))) return RUNEXCEPTION();
            Core.base_ds=CPU.Segs_DSphys;
            Core.base_val_ds= CPU_Regs.ds;
            return Constants.BR_Normal;
        }
    }

    final static public class MovFsEw_reg extends Op {
        Reg earw;
        public MovFsEw_reg(int rm) {
            earw = Mod.ew(rm);
        }

        public int call() {
            if (CPU.CPU_SetSegGeneralFS(earw.word())) return RUNEXCEPTION();
            return Constants.BR_Normal;
        }
    }

    final static public class MovFsEw_mem extends Op {
        EaaBase get_eaa;
        public MovFsEw_mem(int rm) {
            get_eaa= Mod.getEaa(rm);
        }

        public int call() {
            int eaa = get_eaa.call();
            if (CPU.CPU_SetSegGeneralFS(Memory.mem_readw(eaa))) return RUNEXCEPTION();
            return Constants.BR_Normal;
        }
    }

    final static public class MovGsEw_reg extends Op {
        Reg earw;
        public MovGsEw_reg(int rm) {
            earw = Mod.ew(rm);
        }

        public int call() {
            if (CPU.CPU_SetSegGeneralGS(earw.word())) return RUNEXCEPTION();
            return Constants.BR_Normal;
        }
    }

    final static public class MovGsEw_mem extends Op {
        EaaBase get_eaa;
        public MovGsEw_mem(int rm) {
            get_eaa= Mod.getEaa(rm);
        }

        public int call() {
            int eaa = get_eaa.call();
            if (CPU.CPU_SetSegGeneralGS(Memory.mem_readw(eaa))) return RUNEXCEPTION();
            return Constants.BR_Normal;
        }
    }

    final static public class PopEw_reg extends Op {
        Reg earw;
        public PopEw_reg(int rm) {
            earw = Mod.ew(rm);
        }
        public int call() {
            earw.word(CPU.CPU_Pop16());
            return Constants.BR_Normal;
        }
    }

    final static public class PopEw_mem extends Op {
        EaaBase get_eaa;
        public PopEw_mem(int rm) {
            get_eaa= Mod.getEaa(rm);
        }
        public int call() {
            int val = CPU.CPU_Pop16();
            int eaa = get_eaa.call();
            Memory.mem_writew(eaa, val);
            return Constants.BR_Normal;
        }
    }

    final static public class Noop extends Op {
        public int call() {
            return Constants.BR_Normal;
        }
    }

    final static public class XchgAx extends Op {
        Reg reg;

        public XchgAx(Reg reg) {
            this.reg = reg;
        }

        public int call() {
            int old=reg.word();
            reg.word(reg_eax.word());
            reg_eax.word(old);
            return Constants.BR_Normal;
        }
    }

    final static public class CallAp extends Op {
        int newcs;
        int newip;

        public CallAp(int newcs, int newip) {
            this.newcs = newcs;
            this.newip = newip;
        }

        public int call() {
            Flags.FillFlags();
            CPU.CPU_CALL(false,newcs,newip,reg_eip+eip_count);
            if (CPU_TRAP_CHECK) {
                if (GETFLAG(TF)!=0) {
                    CPU.cpudecoder= Core_dynamic.CPU_Core_Dynrec_Trap_Run;
                    return CB_NONE();
                }
            }
            return Constants.BR_Jump;
        }
    }

    final static public class PushF extends Op {
        public int call() {
            if (CPU.CPU_PUSHF(false)) return RUNEXCEPTION();
            return Constants.BR_Normal;
        }
    }

    final static public class PopF extends Op {
        public int call() {
            if (CPU.CPU_POPF(false)) return RUNEXCEPTION();
            if (CPU_TRAP_CHECK) {
                    if (GETFLAG(TF)!=0) {
                        CPU.cpudecoder= Core_dynamic.CPU_Core_Dynrec_Trap_Run;
                        return DECODE_END();
                    }
            }
            if (CPU_PIC_CHECK)
                if (GETFLAG(IF)!=0 && Pic.PIC_IRQCheck!=0) return DECODE_END();
            return Constants.BR_Normal;
        }
    }

    final static public class Sahf extends Op {
        public int call() {
            Flags.SETFLAGSb(reg_eax.high());
            return Constants.BR_Normal;
        }
    }

    final static public class Lahf extends Op {
        public int call() {
            Flags.FillFlags();
            reg_eax.high(CPU_Regs.flags & 0xff);
            return Constants.BR_Normal;
        }
    }

    abstract static public class GetEADirect extends Op {
        int value;

        public GetEADirect() {
            if ((prefixes & Core.PREFIX_ADDR)!=0) {
                value = decode_fetchd();
            } else {
                value = decode_fetchw();
            }
        }
    }
    final static public class MovALOb extends GetEADirect {
        public int call() {
            int eaa = (Core.base_ds+value);
            reg_eax.low(Memory.mem_readb(eaa));
            return Constants.BR_Normal;
        }
    }

    final static public class MovAXOw extends GetEADirect {
        public int call() {
            int eaa = (Core.base_ds+value);
            reg_eax.word(Memory.mem_readw(eaa));
            return Constants.BR_Normal;
        }
    }

    final static public class MovObAL extends GetEADirect {
        public int call() {
            int eaa = (Core.base_ds+value);
            Memory.mem_writeb(eaa, reg_eax.low());
            return Constants.BR_Normal;
        }
    }

    final static public class MovOwAX extends GetEADirect {
        public int call() {
            int eaa = (Core.base_ds+value);
            Memory.mem_writew(eaa, reg_eax.word());
            return Constants.BR_Normal;
        }
    }

    final static public class TestAlIb extends Op {
        short ib;
        public TestAlIb() {
            ib = decode_fetchb();
        }
        public int call() {
            Instructions.TESTB(ib,reg_eax.low());
            return Constants.BR_Normal;
        }
    }

    final static public class TestAxIw extends Op {
        int iw;
        public TestAxIw() {
            iw = decode_fetchw();
        }
        public int call() {
            Instructions.TESTW(iw,reg_eax.word());
            return Constants.BR_Normal;
        }
    }

    final static public class MovIb extends Op {
        short ib;
        Reg reg;
        public MovIb(Reg reg) {
            ib = decode_fetchb();
            this.reg = reg;
        }
        public int call() {
            reg.set8(ib);
            return Constants.BR_Normal;
        }
    }

    final static public class MovIb_mem extends Op {
        short ib;
        EaaBase get_eaa;
        public MovIb_mem(int rm) {
            get_eaa= Mod.getEaa(rm);
            ib = decode_fetchb();
        }
        public int call() {
            int eaa = get_eaa.call();
            Memory.mem_writeb(eaa, ib);
            return Constants.BR_Normal;
        }
    }

    final static public class MovIw extends Op {
        int ib;
        Reg reg;
        public MovIw(Reg reg) {
            ib = decode_fetchw();
            this.reg = reg;
        }
        public int call() {
            reg.word(ib);
            return Constants.BR_Normal;
        }
    }

    final static public class MovIw_mem extends Op {
        int ib;
        EaaBase get_eaa;
        public MovIw_mem(int rm) {
            get_eaa= Mod.getEaa(rm);
            ib = decode_fetchw();
        }
        public int call() {
            int eaa = get_eaa.call();
            Memory.mem_writew(eaa, ib);
            return Constants.BR_Normal;
        }
    }

    final static public class RetnIw extends Op {
        int offset;

        public RetnIw() {
            offset = decode_fetchw();
        }
        public int call() {
            reg_eip=CPU.CPU_Pop16();
            reg_esp.dword=reg_esp.dword+offset;
            return Constants.BR_Jump;
        }
    }

    final static public class Retn extends Op {
        public int call() {
            reg_eip=CPU.CPU_Pop16();
            return Constants.BR_Jump;
        }
    }

    final static public class Les extends Op {
        EaaBase get_eaa;
        Reg rw;
        public Les(int rm) {
            get_eaa= Mod.getEaa(rm);
            rw = Mod.gw(rm);
        }
        public int call() {
            int eaa=get_eaa.call();
            if (CPU.CPU_SetSegGeneralES(Memory.mem_readw(eaa+2))) return RUNEXCEPTION();
            rw.word(Memory.mem_readw(eaa));
            return Constants.BR_Normal;
        }
    }

    final static public class Lds extends Op {
        EaaBase get_eaa;
        Reg rw;
        public Lds(int rm) {
            get_eaa= Mod.getEaa(rm);
            rw = Mod.gw(rm);
        }
        public int call() {
            int eaa=get_eaa.call();
            if (CPU.CPU_SetSegGeneralDS(Memory.mem_readw(eaa+2))) return RUNEXCEPTION();
            rw.word(Memory.mem_readw(eaa));
            Core.base_ds=CPU.Segs_DSphys;
            Core.base_val_ds= CPU_Regs.ds;
            return Constants.BR_Normal;
        }
    }

    final static public class EnterIwIb extends Op {
        int bytes;
        int level;
        public EnterIwIb() {
            bytes=decode_fetchw();
            level=decode_fetchb();
        }
        public int call() {
            CPU.CPU_ENTER(false,bytes,level);
            return Constants.BR_Normal;
        }
    }

    final static public class Leave extends Op {
        public int call() {
            reg_esp.dword&=CPU.cpu.stack.notmask;
            reg_esp.dword|=(reg_ebp.dword & CPU.cpu.stack.mask);
            reg_ebp.word(CPU.CPU_Pop16());
            return Constants.BR_Normal;
        }
    }

    final static public class RetfIw extends Op {
        int words;

        public RetfIw() {
            words = decode_fetchw();
        }
        public int call() {
            Flags.FillFlags();
            CPU.CPU_RET(false,words,reg_eip+eip_count);
            return Constants.BR_Jump;
        }
    }

    final static public class Retf extends Op {
        public int call() {
            Flags.FillFlags();
            CPU.CPU_RET(false,0,reg_eip+eip_count);
            return Constants.BR_Jump;
        }
    }

    final static public class Int3 extends Op {
        public int call() {
            CPU.CPU_SW_Interrupt_NoIOPLCheck(3,reg_eip+eip_count);
            if (CPU_TRAP_CHECK)
                CPU.cpu.trap_skip=true;
            return Constants.BR_Jump;
        }
    }

    final static public class IntIb extends Op {
        int num;
        public IntIb() {
            num = decode_fetchb();
        }
        public int call() {
            CPU.CPU_SW_Interrupt(num,reg_eip+eip_count);
            if (CPU_TRAP_CHECK)
                CPU.cpu.trap_skip=true;
            return Constants.BR_Jump;
        }
    }

    final static public class Int0 extends Op {
        public int call() {
            if (Flags.get_OF()) {
                CPU.CPU_SW_Interrupt(4,reg_eip+eip_count);
                if (CPU_TRAP_CHECK)
                    CPU.cpu.trap_skip=true;
                return Constants.BR_Jump;
            }
            return Constants.BR_Normal;
        }
    }

    final static public class IRet extends Op {
        public int call() {
            CPU.CPU_IRET(false, reg_eip+eip_count);
            if (CPU_TRAP_CHECK) {
                if (GETFLAG(TF)!=0) {
                    CPU.cpudecoder= Core_dynamic.CPU_Core_Dynrec_Trap_Run;
                    return CB_NONE();
                }
            }
            if (CPU_PIC_CHECK)
                if (GETFLAG(IF)!=0 && Pic.PIC_IRQCheck!=0) return CB_NONE();
            return Constants.BR_Jump;
        }
    }

    final static public class AamIb extends Op {
        int ib;
        public AamIb() {
            ib=decode_fetchb();
        }
        public int call() {
            return Instructions.AAMr(this, ib);
        }
    }

    final static public class AadIb extends Op {
        int ib;
        public AadIb() {
            ib=decode_fetchb();
        }
        public int call() {
            Instructions.AAD(ib);
            return Constants.BR_Normal;
        }
    }

    final static public class Salc extends Op {
        public int call() {
            reg_eax.low(Flags.get_CF() ? 0xFF : 0);
            return Constants.BR_Normal;
        }
    }

    final static public class Xlat32 extends Op {
        public int call() {
            reg_eax.low(Memory.mem_readb(Core.base_ds+reg_ebx.dword+reg_eax.low()));
            return Constants.BR_Normal;
        }
    }

    final static public class Xlat16 extends Op {
        public int call() {
            reg_eax.low(Memory.mem_readb(Core.base_ds+((reg_ebx.word()+reg_eax.low()) & 0xFFFF)));
            return Constants.BR_Normal;
        }
    }

    final static public class FPU0_normal extends Op {
        int rm;
        public FPU0_normal(int rm) {
            this.rm = rm;
        }
        public int call() {
            FPU.FPU_ESC0_Normal(rm);
            return Constants.BR_Normal;
        }
    }

    final static public class FPU0_ea extends Op {
        int rm;
        EaaBase get_eaa;

        public FPU0_ea(int rm) {
            this.rm = rm;
                get_eaa= Mod.getEaa(rm);
        }
        public int call() {
            FPU.FPU_ESC0_EA(rm,get_eaa.call());
            return Constants.BR_Normal;
        }
    }

    final static public class FPU1_normal extends Op {
        int rm;
        public FPU1_normal(int rm) {
            this.rm = rm;
        }
        public int call() {
            FPU.FPU_ESC1_Normal(rm);
            return Constants.BR_Normal;
        }
    }

    final static public class FPU1_ea extends Op {
        int rm;
        EaaBase get_eaa;

        public FPU1_ea(int rm) {
            this.rm = rm;
                get_eaa= Mod.getEaa(rm);
        }
        public int call() {
            FPU.FPU_ESC1_EA(rm,get_eaa.call());
            return Constants.BR_Normal;
        }
    }

    final static public class FPU2_normal extends Op {
        int rm;
        public FPU2_normal(int rm) {
            this.rm = rm;
        }
        public int call() {
            FPU.FPU_ESC2_Normal(rm);
            return Constants.BR_Normal;
        }
    }

    final static public class FPU2_ea extends Op {
        int rm;
        EaaBase get_eaa;

        public FPU2_ea(int rm) {
            this.rm = rm;
                get_eaa= Mod.getEaa(rm);
        }
        public int call() {
            FPU.FPU_ESC2_EA(rm,get_eaa.call());
            return Constants.BR_Normal;
        }
    }

    final static public class FPU3_normal extends Op {
        int rm;
        public FPU3_normal(int rm) {
            this.rm = rm;
        }
        public int call() {
            FPU.FPU_ESC3_Normal(rm);
            return Constants.BR_Normal;
        }
    }

    final static public class FPU3_ea extends Op {
        int rm;
        EaaBase get_eaa;

        public FPU3_ea(int rm) {
            this.rm = rm;
                get_eaa= Mod.getEaa(rm);
        }
        public int call() {
            FPU.FPU_ESC3_EA(rm,get_eaa.call());
            return Constants.BR_Normal;
        }
    }

    final static public class FPU4_normal extends Op {
        int rm;
        public FPU4_normal(int rm) {
            this.rm = rm;
        }
        public int call() {
            FPU.FPU_ESC4_Normal(rm);
            return Constants.BR_Normal;
        }
    }

    final static public class FPU4_ea extends Op {
        int rm;
        EaaBase get_eaa;

        public FPU4_ea(int rm) {
            this.rm = rm;
                get_eaa= Mod.getEaa(rm);
        }
        public int call() {
            FPU.FPU_ESC4_EA(rm,get_eaa.call());
            return Constants.BR_Normal;
        }
    }

    final static public class FPU5_normal extends Op {
        int rm;
        public FPU5_normal(int rm) {
            this.rm = rm;
        }
        public int call() {
            FPU.FPU_ESC5_Normal(rm);
            return Constants.BR_Normal;
        }
    }

    final static public class FPU5_ea extends Op {
        int rm;
        EaaBase get_eaa;

        public FPU5_ea(int rm) {
            this.rm = rm;
                get_eaa= Mod.getEaa(rm);
        }
        public int call() {
            FPU.FPU_ESC5_EA(rm,get_eaa.call());
            return Constants.BR_Normal;
        }
    }

    final static public class FPU6_normal extends Op {
        int rm;
        public FPU6_normal(int rm) {
            this.rm = rm;
        }
        public int call() {
            FPU.FPU_ESC6_Normal(rm);
            return Constants.BR_Normal;
        }
    }

    final static public class FPU6_ea extends Op {
        int rm;
        EaaBase get_eaa;

        public FPU6_ea(int rm) {
            this.rm = rm;
                get_eaa= Mod.getEaa(rm);
        }
        public int call() {
            FPU.FPU_ESC6_EA(rm,get_eaa.call());
            return Constants.BR_Normal;
        }
    }

    final static public class FPU7_normal extends Op {
        int rm;
        public FPU7_normal(int rm) {
            this.rm = rm;
        }
        public int call() {
            FPU.FPU_ESC7_Normal(rm);
            return Constants.BR_Normal;
        }
    }

    final static public class FPU7_ea extends Op {
        int rm;
        EaaBase get_eaa;

        public FPU7_ea(int rm) {
            this.rm = rm;
                get_eaa= Mod.getEaa(rm);
        }
        public int call() {
            FPU.FPU_ESC7_EA(rm,get_eaa.call());
            return Constants.BR_Normal;
        }
    }

    final static public class Loopnz32 extends JumpCond16_b {
        public int call() {
            reg_ecx.dword--;
            return jump(reg_ecx.dword!=0 && !Flags.get_ZF(), offset);
        }
    }

    final static public class Loopnz16 extends JumpCond16_b {
        public int call() {
            reg_ecx.word(reg_ecx.word()-1);
            return jump(reg_ecx.word()!=0 && !Flags.get_ZF(), offset);
        }
    }

    final static public class Loopz32 extends JumpCond16_b {
        public int call() {
            reg_ecx.dword--;
            return jump(reg_ecx.dword!=0 && Flags.get_ZF(), offset);
        }
    }

    final static public class Loopz16 extends JumpCond16_b {
        public int call() {
            reg_ecx.word(reg_ecx.word()-1);
            return jump(reg_ecx.word()!=0 && Flags.get_ZF(), offset);
        }
    }

    final static public class Loop32 extends JumpCond16_b {
        public int call() {
            reg_ecx.dword--;
            return jump(reg_ecx.dword!=0, offset);
        }
    }

    final static public class Loop16 extends JumpCond16_b {
        public int call() {
            reg_ecx.word(reg_ecx.word()-1);
            return jump(reg_ecx.word()!=0, offset);
        }
    }

    final static public class Jcxz extends JumpCond16_b {
        int mask;
        public Jcxz(int mask) {
            this.mask = mask;
        }
        public int call() {
            return jump((reg_ecx.dword & mask)==0, offset);
        }
    }

    final static public class InAlIb extends Op {
        int port;
        public InAlIb() {
            port=decode_fetchb();
        }
        public int call() {
            if (CPU.CPU_IO_Exception(port,1)) return RUNEXCEPTION();
            reg_eax.low(IO.IO_ReadB(port));
            return Constants.BR_Normal;
        }
    }

    final static public class InAxIb extends Op {
        int port;
        public InAxIb() {
            port=decode_fetchb();
        }
        public int call() {
            if (CPU.CPU_IO_Exception(port,2)) return RUNEXCEPTION();
            reg_eax.word(IO.IO_ReadW(port));
            return Constants.BR_Normal;
        }
    }

    final static public class OutAlIb extends Op {
        int port;
        public OutAlIb() {
            port=decode_fetchb();
        }
        public int call() {
            if (CPU.CPU_IO_Exception(port,1)) return RUNEXCEPTION();
            IO.IO_WriteB(port,reg_eax.low());
            return Constants.BR_Normal;
        }
    }

    final static public class OutAxIb extends Op {
        int port;
        public OutAxIb() {
            port=decode_fetchb();
        }
        public int call() {
            if (CPU.CPU_IO_Exception(port,2)) return RUNEXCEPTION();
            IO.IO_WriteW(port,reg_eax.word());
            return Constants.BR_Normal;
        }
    }

    final static public class CallJw extends Op {
        int addip;
        public CallJw() {
            addip=decode_fetchws();
        }
        public int call() {
            CPU.CPU_Push16(reg_eip+eip_count);
            reg_ip(reg_eip+eip_count+addip);
            return Constants.BR_Link1;
        }
    }

    final static public class JmpJw extends Op {
        int addip;
        public JmpJw() {
            addip=decode_fetchws();
        }
        public int call() {
            reg_eip+=eip_count;
            reg_ip(reg_eip+addip);
            return Constants.BR_Link1;
        }
    }

    final static public class JmpAp extends Op {
        int newip;
        int newcs;
        public JmpAp() {
            newip=decode_fetchw();
            newcs=decode_fetchw();
        }
        public int call() {
            Flags.FillFlags();
            CPU.CPU_JMP(false,newcs,newip,reg_eip+eip_count);
            if (CPU_TRAP_CHECK) {
                if (GETFLAG(TF)!=0) {
                    CPU.cpudecoder= Core_dynamic.CPU_Core_Dynrec_Trap_Run;
                    return CB_NONE();
                }
            }
            return Constants.BR_Jump;
        }
    }

    final static public class JmpJb extends Op {
        int addip;
        public JmpJb() {
            addip=decode_fetchbs();
        }
        public int call() {
            reg_eip+=eip_count;
            reg_ip(reg_eip+addip);
            return Constants.BR_Link1;
        }
    }

    final static public class InAlDx extends Op {
        public int call() {
            if (CPU.CPU_IO_Exception(reg_edx.word(),1)) return RUNEXCEPTION();
            reg_eax.low(IO.IO_ReadB(reg_edx.word()));
            return Constants.BR_Normal;
        }
    }

    final static public class InAxDx extends Op {
        public int call() {
            if (CPU.CPU_IO_Exception(reg_edx.word(),2)) return RUNEXCEPTION();
            reg_eax.word(IO.IO_ReadW(reg_edx.word()));
            return Constants.BR_Normal;
        }
    }

    final static public class OutAlDx extends Op {
        public int call() {
            if (CPU.CPU_IO_Exception(reg_edx.word(),1)) return RUNEXCEPTION();
            IO.IO_WriteB(reg_edx.word(),reg_eax.low());
            return Constants.BR_Normal;
        }
    }

    final static public class OutAxDx extends Op {
        public int call() {
            if (CPU.CPU_IO_Exception(reg_edx.word(),2)) return RUNEXCEPTION();
            IO.IO_WriteW(reg_edx.word(),reg_eax.word());
            return Constants.BR_Normal;
        }
    }

    final static public class Lock extends Op {
        public int call() {
            Log.log(LogTypes.LOG_CPU,LogSeverities.LOG_NORMAL,"CPU:LOCK"); /* FIXME: see case D_LOCK in core_full/new Instructions.load()h */
            return Constants.BR_Normal;
        }
    }

    final static public class Icebp extends Op {
        public int call() {
            CPU.CPU_SW_Interrupt_NoIOPLCheck(1,reg_eip+eip_count);
            if (CPU_TRAP_CHECK)
                CPU.cpu.trap_skip=true;
            return Constants.BR_Jump;
        }
    }

    final static public class Hlt extends Op {
        public int call() {
             if (CPU.cpu.pmode && CPU.cpu.cpl!=0) return EXCEPTION(CPU.EXCEPTION_GP);
            Flags.FillFlags();
            CPU.CPU_HLT(reg_eip+eip_count);
            return CB_NONE();
        }
    }

    final static public class Cmc extends Op {
        public int call() {
            Flags.FillFlags();
            SETFLAGBIT(CF,(CPU_Regs.flags & CPU_Regs.CF)==0);
            return Constants.BR_Normal;
        }
    }

    final static public class Clc extends Op {
        public int call() {
            Flags.FillFlags();
            SETFLAGBIT(CF,false);
            return Constants.BR_Normal;
        }
    }

    final static public class Stc extends Op {
        public int call() {
            Flags.FillFlags();
            SETFLAGBIT(CF,true);
            return Constants.BR_Normal;
        }
    }

    final static public class Cli extends Op {
        public int call() {
            if (CPU.CPU_CLI()) return RUNEXCEPTION();
            return Constants.BR_Normal;
        }
    }

    final static public class Sti extends Op {
        public int call() {
            if (CPU.CPU_STI()) return RUNEXCEPTION();
            if (CPU_PIC_CHECK)
                if (GETFLAG(IF)!=0 && Pic.PIC_IRQCheck!=0) return DECODE_END();
            return Constants.BR_Normal;
        }
    }

    final static public class Cld extends Op {
        public int call() {
            SETFLAGBIT(DF,false);
            CPU.cpu.direction=1;
            return Constants.BR_Normal;
        }
    }

    final static public class Std extends Op {
        public int call() {
            SETFLAGBIT(DF,true);
            CPU.cpu.direction=-1;
            return Constants.BR_Normal;
        }
    }

    final static public class Incb_reg extends Op {
        Reg reg;
        public Incb_reg(int rm) {
            reg = Mod.eb(rm);
        }
        public int call() {
            reg.set8(Instructions.INCB(reg.get8()));
            return Constants.BR_Normal;
        }
    }

    final static public class Incb_mem extends Op {
        EaaBase get_eaa;
        public Incb_mem(int rm) {
            this.get_eaa = Mod.getEaa(rm);
        }
        public int call() {
            int eaa = get_eaa.call();
            int index = Paging.getDirectIndex(eaa);
            if (index>=0)
                Memory.host_writeb(index, Instructions.INCB(Memory.host_readb(index)));
            else
                Memory.mem_writeb(eaa, Instructions.INCB(Memory.mem_readb(eaa)));
            return Constants.BR_Normal;
        }
    }

    final static public class Decb_reg extends Op {
        Reg reg;
        public Decb_reg(int rm) {
            reg = Mod.eb(rm);
        }
        public int call() {
            reg.set8(Instructions.DECB(reg.get8()));
            return Constants.BR_Normal;
        }
    }

    final static public class Decb_mem extends Op {
        EaaBase get_eaa;
        public Decb_mem(int rm) {
            this.get_eaa = Mod.getEaa(rm);
        }
        public int call() {
            int eaa = get_eaa.call();
            int index = Paging.getDirectIndex(eaa);
            if (index>=0)
                Memory.host_writeb(index, Instructions.DECB(Memory.host_readb(index)));
            else
                Memory.mem_writeb(eaa, Instructions.DECB(Memory.mem_readb(eaa)));
            return Constants.BR_Normal;
        }
    }

    final static public class Callback extends Op {
        int val;
        public Callback(int val) {
            this.val = val;
        }
        public int call() {
            reg_eip+=eip_count;
            Data.callback = val;
            return Constants.BR_CallBack;
        }
    }

    final static public class Incw_reg extends Op {
        Reg reg;
        public Incw_reg(int rm) {
            reg = Mod.ew(rm);
        }
        public int call() {
            reg.word(Instructions.INCW(reg.word()));
            return Constants.BR_Normal;
        }
    }

    final static public class Incw_mem extends Op {
        EaaBase get_eaa;
        public Incw_mem(int rm) {
            this.get_eaa = Mod.getEaa(rm);
        }
        public int call() {
            int eaa = get_eaa.call();
            if ((eaa & 0xFFF)<0xFFF) {
                int index = Paging.getDirectIndex(eaa);
                if (index>=0) {
                    Memory.host_writew(index, Instructions.INCW(Memory.host_readw(index)));
                    return Constants.BR_Normal;
                }
            }
            Memory.mem_writew(eaa, Instructions.INCW(Memory.mem_readw(eaa)));
            return Constants.BR_Normal;
        }
    }

    final static public class Decw_reg extends Op {
        Reg reg;
        public Decw_reg(int rm) {
            reg = Mod.ew(rm);
        }
        public int call() {
            reg.word(Instructions.DECW(reg.word()));
            return Constants.BR_Normal;
        }
    }

    final static public class Decw_mem extends Op {
        EaaBase get_eaa;
        public Decw_mem(int rm) {
            this.get_eaa = Mod.getEaa(rm);
        }
        public int call() {
            int eaa = get_eaa.call();
            if ((eaa & 0xFFF)<0xFFF) {
                int index = Paging.getDirectIndex(eaa);
                if (index>=0) {
                    Memory.host_writew(index, Instructions.DECW(Memory.host_readw(index)));
                    return Constants.BR_Normal;
                }
            }
            Memory.mem_writew(eaa, Instructions.DECW(Memory.mem_readw(eaa)));
            return Constants.BR_Normal;
        }
    }

    final static public class CallEv_reg extends Op {
        Reg earw;
        public CallEv_reg(int rm) {
            earw = Mod.ew(rm);
        }
        public int call() {
            int old = reg_eip+eip_count;
            CPU.CPU_Push16(old & 0xFFFF);
            reg_eip=earw.word();
            return Constants.BR_Jump;
        }
    }

    final static public class CallEv_mem extends Op {
        EaaBase get_eaa;
        public CallEv_mem(int rm) {
            this.get_eaa = Mod.getEaa(rm);
        }
        public int call() {
            int eaa = get_eaa.call();
            int old = reg_eip+eip_count;
            int eip = Memory.mem_readw(eaa);
            CPU.CPU_Push16(old & 0xFFFF);
            reg_eip = eip;
            return Constants.BR_Jump;
        }
    }

    final static public class CallEp extends Op {
        EaaBase get_eaa;
        public CallEp(int rm) {
            this.get_eaa = Mod.getEaa(rm);
        }
        public int call() {
            int eaa = get_eaa.call();
            int newip=Memory.mem_readw(eaa);
            int newcs=Memory.mem_readw(eaa+2);
            Flags.FillFlags();
            CPU.CPU_CALL(false,newcs,newip,(reg_eip+eip_count) & 0xFFFF);
            if (CPU_TRAP_CHECK) {
                if (GETFLAG(TF)!=0) {
                    CPU.cpudecoder= Core_dynamic.CPU_Core_Dynrec_Trap_Run;
                    return CB_NONE();
                }
            }
            return Constants.BR_Jump;
        }
    }

    final static public class JmpEv_reg extends Op {
        Reg earw;

        public JmpEv_reg(int rm) {
            earw = Mod.ew(rm);
        }
        public int call() {
            reg_eip=earw.word();
            return Constants.BR_Jump;
        }
    }

    final static public class JmpEv_mem extends Op {
        EaaBase get_eaa;

        public JmpEv_mem(int rm) {
            this.get_eaa = Mod.getEaa(rm);
        }
        public int call() {
            int eaa = get_eaa.call();
            reg_eip=Memory.mem_readw(eaa);
            return Constants.BR_Jump;
        }
    }

    final static public class JmpEp extends Op {
        EaaBase get_eaa;
        public JmpEp(int rm) {
            this.get_eaa = Mod.getEaa(rm);
        }
        public int call() {
            int eaa = get_eaa.call();
            int newip=Memory.mem_readw(eaa);
            int newcs=Memory.mem_readw(eaa+2);
            Flags.FillFlags();
            CPU.CPU_JMP(false,newcs,newip,(reg_eip+eip_count) & 0xFFFF);
            if (CPU_TRAP_CHECK) {
                if (GETFLAG(TF)!=0) {
                    CPU.cpudecoder= Core_dynamic.CPU_Core_Dynrec_Trap_Run;
                    return CB_NONE();
                }
            }
            return Constants.BR_Jump;
        }
    }

    final static public class PushEv_reg extends Op {
        Reg earw;
        public PushEv_reg(int rm) {
            earw = Mod.ew(rm);
        }
        public int call() {
            CPU.CPU_Push16(earw.word());
            return Constants.BR_Normal;
        }
    }

    final static public class PushEv_mem extends Op {
        EaaBase get_eaa;
        public PushEv_mem(int rm) {
            this.get_eaa = Mod.getEaa(rm);
        }
        public int call() {
            int eaa = get_eaa.call();
            CPU.CPU_Push16(Memory.mem_readw(eaa));
            return Constants.BR_Normal;
        }
    }

    final static public class Cbw extends Op {
        public int call() {
            reg_eax.word((byte)reg_eax.low());
            return Constants.BR_Normal;
        }
    }

    final static public class Cwd extends Op {
        public int call() {
            if ((reg_eax.word() & 0x8000)!=0) reg_edx.word(0xffff);else reg_edx.word(0);
            return Constants.BR_Normal;
        }
    }
}
