package jdos.cpu;

public class Flags {
    // needs to come before creating LazyFlags
    static public GetFlags t_UNKNOWN = new GetFlags() {
        public boolean CF() {return CPU_Regs.GETFLAG(CPU_Regs.CF)!=0;}
        public boolean AF() {return CPU_Regs.GETFLAG(CPU_Regs.AF) != 0;}
        public boolean ZF() {return CPU_Regs.GETFLAG(CPU_Regs.ZF) != 0;}
        public boolean SF() {return CPU_Regs.GETFLAG(CPU_Regs.SF) != 0;}
        public boolean OF() {return CPU_Regs.GETFLAG(CPU_Regs.OF) != 0;}
        public boolean PF() {return CPU_Regs.GETFLAG(CPU_Regs.PF) != 0;}
    };

    public static LazyFlags lflags = new LazyFlags();

    public static short lf_var1b() {
        return (short)(lflags.var1 & 0xFF);
    }

    public static void lf_var1b(int b) {
        lflags.var1=lflags.var1 & ~0xFF;
        lflags.var1=lflags.var1 | b & 0xFF;
    }

    public static short lf_var2b() {
        return (short)(lflags.var2 & 0xFF);
    }

    public static void lf_var2b(int b) {
        lflags.var2=lflags.var2 & ~0xFF;
        lflags.var2=lflags.var2 | b & 0xFF;
    }

    public static short lf_resb() {
        return (short)(lflags.res & 0xFF);
    }

    public static void lf_resb(int b) {
        lflags.res = lflags.res & ~0xFF;
        lflags.res = lflags.res | b & 0xFF;
    }

    public static int lf_var1w() {
        return (int)(lflags.var1 & 0xFFFF);
    }

    public static void lf_var1w(int s) {
        lflags.var1=lflags.var1 & ~0xFFFF;
        lflags.var1=lflags.var1 | s & 0xFFFF;
    }

    public static int lf_var2w() {
        return (int)(lflags.var2 & 0xFFFF);
    }

    public static void lf_var2w(int s) {
        lflags.var2=lflags.var2 & ~0xFFFF;
        lflags.var2=lflags.var2 | s & 0xFFFF;
    }

    public static int lf_resw() {
        return (int)(lflags.res & 0xFFFF);
    }

    public static void lf_resw(int s) {
        lflags.res = lflags.res & ~0xFFFF;
        lflags.res = lflags.res | s & 0xFFFF;
    }

    static int lf_var1d() {
        return lflags.var1;
    }

    public static void lf_var1d(int v) {
        lflags.var1=v;
    }

    public static int lf_var2d() {
        return lflags.var2;
    }

    public static void lf_var2d(int v) {
        lflags.var2=v;
    }

    static int lf_resd() {
        return lflags.res;
    }

    public static void lf_resd(int v) {
        lflags.res=v;
    }

    public static void SETFLAGSb(int FLAGB) {
        CPU_Regs.SETFLAGBIT(CPU_Regs.OF,get_OF());
        lflags.type=t_UNKNOWN;
        CPU.CPU_SetFlags(FLAGB,CPU_Regs.FMASK_NORMAL & 0xff);
    }

    static public void LoadCF() {
        CPU_Regs.SETFLAGBIT(CPU_Regs.CF,get_CF());
    }
    static public void LoadZF() {
        CPU_Regs.SETFLAGBIT(CPU_Regs.ZF,get_ZF());
    }
    static public void LoadSF() {
        CPU_Regs.SETFLAGBIT(CPU_Regs.SF,get_SF());
    }
    static public void LoadOF() {
        CPU_Regs.SETFLAGBIT(CPU_Regs.OF,get_OF());
    }
    static public void LoadAF() {
        CPU_Regs.SETFLAGBIT(CPU_Regs.AF,get_AF());
    }

    static public interface GetFlags {
        public boolean CF();
        public boolean AF();
        public boolean ZF();
        public boolean SF();
        public boolean OF();
        public boolean PF();
    }

    static abstract public class ParityFlags implements GetFlags {
        public boolean PF() {return (parity_lookup[lf_resb()]) != 0;}
    }

    final static public ParityFlags t_INCb = new ParityFlags() {
        public boolean CF() {return CPU_Regs.GETFLAG(CPU_Regs.CF)!=0;}
        public boolean AF() {return (lf_resb() & 0x0f) == 0;}
        public boolean ZF() {return (lf_resb()==0);}
        public boolean SF() {return	(lf_resb()&0x80)!= 0;}
        public boolean OF() {return (lf_resb() == 0x80);}
    };

    final static public ParityFlags t_INCw = new ParityFlags() {
        public boolean CF() {return CPU_Regs.GETFLAG(CPU_Regs.CF)!=0;}
        public boolean AF() {return (lf_resw() & 0x0f) == 0;}
        public boolean ZF() {return (lf_resw()==0);}
        public boolean SF() {return	(lf_resw()&0x8000)!=0;}
        public boolean OF() {return (lf_resw() == 0x8000);}
    };

    final static public ParityFlags t_INCd = new ParityFlags() {
        public boolean CF() {return CPU_Regs.GETFLAG(CPU_Regs.CF)!=0;}
        public boolean AF() {return (lf_resd() & 0x0f) == 0;}
        public boolean ZF() {return (lf_resd()==0);}
        public boolean SF() {return	(lf_resd()&0x80000000l)!= 0;}
        public boolean OF() {return (lf_resd() == 0x80000000l);}
    };

    final static public ParityFlags t_DECb = new ParityFlags() {
        public boolean CF() {return CPU_Regs.GETFLAG(CPU_Regs.CF)!=0;}
        public boolean AF() {return (lf_resb() & 0x0f) == 0x0f;}
        public boolean ZF() {return (lf_resb()==0);}
        public boolean SF() {return	(lf_resb()&0x80)!= 0;}
        public boolean OF() {return (lf_resb() == 0x7f);}
    };

    final static public ParityFlags t_DECw = new ParityFlags() {
        public boolean CF() {return CPU_Regs.GETFLAG(CPU_Regs.CF)!=0;}
        public boolean AF() {return (lf_resw() & 0x0f) == 0x0f;}
        public boolean ZF() {return (lf_resw()==0);}
        public boolean SF() {return	(lf_resw()&0x8000)!=0;}
        public boolean OF() {return (lf_resw() == 0x7fff);}
    };

    final static public ParityFlags t_DECd = new ParityFlags() {
        public boolean CF() {return CPU_Regs.GETFLAG(CPU_Regs.CF)!=0;}
        public boolean AF() {return (lf_resd() & 0x0f) == 0x0f;}
        public boolean ZF() {return (lf_resd()==0);}
        public boolean SF() {return	(lf_resd()&0x80000000l)!= 0;}
        public boolean OF() {return (lf_resd() == 0x7fffffff);}
    };

    final static public ParityFlags t_ADDb = new ParityFlags() {
        public boolean CF() {return (lf_resb()<lf_var1b());}
        public boolean AF() {return (((lf_var1b() ^ lf_var2b()) ^ lf_resb()) & 0x10) !=0;}
        public boolean ZF() {return (lf_resb()==0);}
        public boolean SF() {return	(lf_resb()&0x80)!= 0;}
        public boolean OF() {return (((lf_var1b() ^ lf_var2b() ^ 0x80) & (lf_resb() ^ lf_var2b())) & 0x80) != 0;}
    };

    final static public ParityFlags t_ADDw = new ParityFlags() {
        public boolean CF() {return (lf_resw()<lf_var1w());}
        public boolean AF() {return (((lf_var1w() ^ lf_var2w()) ^ lf_resw()) & 0x10) != 0;}
        public boolean ZF() {return (lf_resw()==0);}
        public boolean SF() {return	(lf_resw()&0x8000)!=0;}
        public boolean OF() {return (((lf_var1w() ^ lf_var2w() ^ 0x8000) & (lf_resw() ^ lf_var2w())) & 0x8000) != 0;}
    };

    final static public ParityFlags t_ADDd = new ParityFlags() {
        public boolean CF() {return ((lf_resd() & 0xFFFFFFFFl)<(lf_var1d() & 0xFFFFFFFFl));}
        public boolean AF() {return (((lf_var1d() ^ lf_var2d()) ^ lf_resd()) & 0x10) != 0;}
        public boolean ZF() {return (lf_resd()==0);}
        public boolean SF() {return	(lf_resd()&0x80000000l)!= 0;}
        public boolean OF() {return (((lf_var1d() ^ lf_var2d() ^ 0x80000000l) & (lf_resd() ^ lf_var2d())) & 0x80000000l) != 0;}
    };

    final static public ParityFlags t_ADCb = new ParityFlags() {
        public boolean CF() {return (lf_resb() < lf_var1b()) || (lflags.oldcf && (lf_resb() == lf_var1b()));}
        public boolean AF() {return (((lf_var1b() ^ lf_var2b()) ^ lf_resb()) & 0x10) !=0;}
        public boolean ZF() {return (lf_resb()==0);}
        public boolean SF() {return	(lf_resb()&0x80)!= 0;}
        public boolean OF() {return (((lf_var1b() ^ lf_var2b() ^ 0x80) & (lf_resb() ^ lf_var2b())) & 0x80) != 0;}
    };

    final static public ParityFlags t_ADCw = new ParityFlags() {
        public boolean CF() {return (lf_resw() < lf_var1w()) || (lflags.oldcf && (lf_resw() == lf_var1w()));}
        public boolean AF() {return (((lf_var1w() ^ lf_var2w()) ^ lf_resw()) & 0x10) != 0;}
        public boolean ZF() {return (lf_resw()==0);}
        public boolean SF() {return	(lf_resw()&0x8000)!=0;}
        public boolean OF() {return (((lf_var1w() ^ lf_var2w() ^ 0x8000) & (lf_resw() ^ lf_var2w())) & 0x8000) != 0;}
    };

    final static public ParityFlags t_ADCd = new ParityFlags() {
        public boolean CF() {return ((lf_resd() & 0xFFFFFFFFl) < (lf_var1d() & 0xFFFFFFFFl)) || (lflags.oldcf && (lf_resd() == lf_var1d()));}
        public boolean AF() {return (((lf_var1d() ^ lf_var2d()) ^ lf_resd()) & 0x10) != 0;}
        public boolean ZF() {return (lf_resd()==0);}
        public boolean SF() {return	(lf_resd()&0x80000000l)!= 0;}
        public boolean OF() {return (((lf_var1d() ^ lf_var2d() ^ 0x80000000l) & (lf_resd() ^ lf_var2d())) & 0x80000000l) != 0;}
    };

    final static public ParityFlags t_ORb = new ParityFlags() {
        public boolean CF() {return false;}
        public boolean AF() {return false;}
        public boolean ZF() {return (lf_resb()==0);}
        public boolean SF() {return	(lf_resb()&0x80)!= 0;}
        public boolean OF() {return false;}
    };

    final static public ParityFlags t_ORw = new ParityFlags() {
        public boolean CF() {return false;}
        public boolean AF() {return false;}
        public boolean ZF() {return (lf_resw()==0);}
        public boolean SF() {return	(lf_resw()&0x8000)!=0;}
        public boolean OF() {return false;}
    };

    final static public ParityFlags t_ORd = new ParityFlags() {
        public boolean CF() {return false;}
        public boolean AF() {return false;}
        public boolean ZF() {return (lf_resd()==0);}
        public boolean SF() {return	(lf_resd()&0x80000000l)!= 0;}
        public boolean OF() {return false;}
    };

    final static public ParityFlags t_ANDb = new ParityFlags() {
        public boolean CF() {return false;}
        public boolean AF() {return false;}
        public boolean ZF() {return (lf_resb()==0);}
        public boolean SF() {return	(lf_resb()&0x80)!= 0;}
        public boolean OF() {return false;}
    };

    final static public ParityFlags t_ANDw = new ParityFlags() {
        public boolean CF() {return false;}
        public boolean AF() {return false;}
        public boolean ZF() {return (lf_resw()==0);}
        public boolean SF() {return	(lf_resw()&0x8000)!=0;}
        public boolean OF() {return false;}
    };

    final static public ParityFlags t_ANDd = new ParityFlags() {
        public boolean CF() {return false;}
        public boolean AF() {return false;}
        public boolean ZF() {return (lf_resd()==0);}
        public boolean SF() {return	(lf_resd()&0x80000000l)!= 0;}
        public boolean OF() {return false;}
    };

    final static public ParityFlags t_TESTb = new ParityFlags() {
        public boolean CF() {return false;}
        public boolean AF() {return false;}
        public boolean ZF() {return (lf_resb()==0);}
        public boolean SF() {return	(lf_resb()&0x80)!= 0;}
        public boolean OF() {return false;}
    };

    final static public ParityFlags t_TESTw = new ParityFlags() {
        public boolean CF() {return false;}
        public boolean AF() {return false;}
        public boolean ZF() {return (lf_resw()==0);}
        public boolean SF() {return	(lf_resw()&0x8000)!=0;}
        public boolean OF() {return false;}
    };

    final static public ParityFlags t_TESTd = new ParityFlags() {
        public boolean CF() {return false;}
        public boolean AF() {return false;}
        public boolean ZF() {return (lf_resd()==0);}
        public boolean SF() {return	(lf_resd()&0x80000000l)!= 0;}
        public boolean OF() {return false;}
    };

    final static public ParityFlags t_XORb = new ParityFlags() {
        public boolean CF() {return false;}
        public boolean AF() {return false;}
        public boolean ZF() {return (lf_resb()==0);}
        public boolean SF() {return	(lf_resb()&0x80)!= 0;}
        public boolean OF() {return false;}
    };

    final static public ParityFlags t_XORw = new ParityFlags() {
        public boolean CF() {return false;}
        public boolean AF() {return false;}
        public boolean ZF() {return (lf_resw()==0);}
        public boolean SF() {return	(lf_resw()&0x8000)!=0;}
        public boolean OF() {return false;}
    };

    final static public ParityFlags t_XORd = new ParityFlags() {
        public boolean CF() {return false;}
        public boolean AF() {return false;}
        public boolean ZF() {return (lf_resd()==0);}
        public boolean SF() {return	(lf_resd()&0x80000000l)!= 0;}
        public boolean OF() {return false;}
    };

    final static public ParityFlags t_SUBb = new ParityFlags() {
        public boolean CF() {return (lf_var1b()<lf_var2b());}
        public boolean AF() {return (((lf_var1b() ^ lf_var2b()) ^ lf_resb()) & 0x10) !=0;}
        public boolean ZF() {return (lf_resb()==0);}
        public boolean SF() {return	(lf_resb()&0x80)!= 0;}
        public boolean OF() {return (((lf_var1b() ^ lf_var2b()) & (lf_var1b() ^ lf_resb())) & 0x80) != 0;}
    };

    final static public ParityFlags t_SUBw = new ParityFlags() {
        public boolean CF() {return (lf_var1w()<lf_var2w());}
        public boolean AF() {return (((lf_var1w() ^ lf_var2w()) ^ lf_resw()) & 0x10) != 0;}
        public boolean ZF() {return (lf_resw()==0);}
        public boolean SF() {return	(lf_resw()&0x8000)!=0;}
        public boolean OF() {return (((lf_var1w() ^ lf_var2w()) & (lf_var1w() ^ lf_resw())) & 0x8000) != 0;}
    };

    final static public ParityFlags t_SUBd = new ParityFlags() {
        public boolean CF() {return ((lf_var1d() & 0xFFFFFFFFl)<(lf_var2d() & 0xFFFFFFFFl));}
        public boolean AF() {return (((lf_var1d() ^ lf_var2d()) ^ lf_resd()) & 0x10) != 0;}
        public boolean ZF() {return (lf_resd()==0);}
        public boolean SF() {return	(lf_resd()&0x80000000l)!= 0;}
        public boolean OF() {return (((lf_var1d() ^ lf_var2d()) & (lf_var1d() ^ lf_resd())) & 0x80000000l) != 0;}
    };

    final static public ParityFlags t_CMPb = new ParityFlags() {
        public boolean CF() {return (lf_var1b()<lf_var2b());}
        public boolean AF() {return (((lf_var1b() ^ lf_var2b()) ^ lf_resb()) & 0x10) !=0;}
        public boolean ZF() {return (lf_resb()==0);}
        public boolean SF() {return	(lf_resb()&0x80)!= 0;}
        public boolean OF() {return (((lf_var1b() ^ lf_var2b()) & (lf_var1b() ^ lf_resb())) & 0x80) != 0;}
    };

    final static public ParityFlags t_CMPw = new ParityFlags() {
        public boolean CF() {return (lf_var1w()<lf_var2w());}
        public boolean AF() {return (((lf_var1w() ^ lf_var2w()) ^ lf_resw()) & 0x10) != 0;}
        public boolean ZF() {return (lf_resw()==0);}
        public boolean SF() {return	(lf_resw()&0x8000)!=0;}
        public boolean OF() {return (((lf_var1w() ^ lf_var2w()) & (lf_var1w() ^ lf_resw())) & 0x8000) != 0;}
    };

    final static public ParityFlags t_CMPd = new ParityFlags() {
        public boolean CF() {return ((lf_var1d() & 0xFFFFFFFFl)<(lf_var2d() & 0xFFFFFFFFl));}
        public boolean AF() {return (((lf_var1d() ^ lf_var2d()) ^ lf_resd()) & 0x10) != 0;}
        public boolean ZF() {return (lf_resd()==0);}
        public boolean SF() {return	(lf_resd()&0x80000000l)!= 0;}
        public boolean OF() {return (((lf_var1d() ^ lf_var2d()) & (lf_var1d() ^ lf_resd())) & 0x80000000l) != 0;}
    };

    final static public ParityFlags t_SBBb = new ParityFlags() {
        public boolean CF() {return (lf_var1b() < lf_resb()) || (lflags.oldcf && (lf_var2b()==0xff));}
        public boolean AF() {return (((lf_var1b() ^ lf_var2b()) ^ lf_resb()) & 0x10) !=0;}
        public boolean ZF() {return (lf_resb()==0);}
        public boolean SF() {return	(lf_resb()&0x80)!= 0;}
        public boolean OF() {return (((lf_var1b() ^ lf_var2b()) & (lf_var1b() ^ lf_resb())) & 0x80) != 0;}
    };

    final static public ParityFlags t_SBBw = new ParityFlags() {
        public boolean CF() {return (lf_var1w() < lf_resw()) || (lflags.oldcf && (lf_var2w()==0xffff));}
        public boolean AF() {return (((lf_var1w() ^ lf_var2w()) ^ lf_resw()) & 0x10) != 0;}
        public boolean ZF() {return (lf_resw()==0);}
        public boolean SF() {return	(lf_resw()&0x8000)!=0;}
        public boolean OF() {return (((lf_var1w() ^ lf_var2w()) & (lf_var1w() ^ lf_resw())) & 0x8000) != 0;}
    };

    final static public ParityFlags t_SBBd = new ParityFlags() {
        public boolean CF() {return ((lf_var1d() & 0xFFFFFFFFl) < (lf_resd() & 0xFFFFFFFFl)) || (lflags.oldcf && (lf_var2d()==0xffffffffl));}
        public boolean AF() {return (((lf_var1d() ^ lf_var2d()) ^ lf_resd()) & 0x10) != 0;}
        public boolean ZF() {return (lf_resd()==0);}
        public boolean SF() {return	(lf_resd()&0x80000000l)!= 0;}
        public boolean OF() {return (((lf_var1d() ^ lf_var2d()) & (lf_var1d() ^ lf_resd())) & 0x80000000l) != 0;}
    };

    final static public ParityFlags t_MUL = new ParityFlags() {
        public boolean CF() {return CPU_Regs.GETFLAG(CPU_Regs.CF)!=0;}
        public boolean AF() {return false;}
        public boolean ZF() {return false;}
        public boolean SF() {return false;}
        public boolean OF() {return CPU_Regs.GETFLAG(CPU_Regs.OF) != 0;}
    };

    final static public ParityFlags t_SHLb = new ParityFlags() {
        public boolean CF() {if (lf_var2b()>8) return false; else return ((lf_var1b() >> (8-lf_var2b())) & 1)!=0;}
        public boolean AF() {return (lf_var2b() & 0x1f) != 0;}
        public boolean ZF() {return (lf_resb()==0);}
        public boolean SF() {return	(lf_resb()&0x80)!= 0;}
        public boolean OF() {return ((lf_resb() ^ lf_var1b()) & 0x80) != 0;}
    };

    final static public ParityFlags t_SHLw = new ParityFlags() {
        public boolean CF() {if (lf_var2b()>16) return false; else return ((lf_var1w()) >> (16-lf_var2b()) & 1)!=0;}
        public boolean AF() {return (lf_var2w() & 0x1f) != 0;}
        public boolean ZF() {return (lf_resw()==0);}
        public boolean SF() {return	(lf_resw()&0x8000)!=0;}
        public boolean OF() {return ((lf_resw() ^ lf_var1w()) & 0x8000) != 0;}
    };

    final static public ParityFlags t_SHLd = new ParityFlags() {
        public boolean CF() {return ((lf_var1d() >>> (32 - lf_var2b())) & 1) != 0;}
        public boolean AF() {return (lf_var2d() & 0x1f) != 0;}
        public boolean ZF() {return (lf_resd()==0);}
        public boolean SF() {return	(lf_resd()&0x80000000l)!= 0;}
        public boolean OF() {return ((lf_resd() ^ lf_var1d()) & 0x80000000l) != 0;}
    };

    final static public ParityFlags t_SHRb = new ParityFlags() {
        public boolean CF() {return ((lf_var1b() >> (lf_var2b() - 1)) & 1) !=0;}
        public boolean AF() {return (lf_var2b() & 0x1f) != 0;}
        public boolean ZF() {return (lf_resb()==0);}
        public boolean SF() {return	(lf_resb()&0x80)!= 0;}
        public boolean OF() {if ((lf_var2b()&0x1f)==1) return (lf_var1b() > 0x80); else return false;}
    };

    final static public ParityFlags t_SHRw = new ParityFlags() {
        public boolean CF() {return ((lf_var1w() >> (lf_var2b() - 1)) & 1) !=0;}
        public boolean AF() {return (lf_var2w() & 0x1f) != 0;}
        public boolean ZF() {return (lf_resw()==0);}
        public boolean SF() {return	(lf_resw()&0x8000)!=0;}
        public boolean OF() {if ((lf_var2b()&0x1f)==1) return (lf_var1w() > 0x8000); else return false;}
    };

    final static public ParityFlags t_SHRd = new ParityFlags() {
        public boolean CF() {return ((lf_var1d() >>> (lf_var2b() - 1)) & 1) != 0;}
        public boolean AF() {return (lf_var2d() & 0x1f) != 0;}
        public boolean ZF() {return (lf_resd()==0);}
        public boolean SF() {return	(lf_resd()&0x80000000l)!= 0;}
        public boolean OF() {if ((lf_var2b()&0x1f)==1) return ((lf_var1d() & 0xFFFFFFFFl) > 0x80000000l); else return false;}
    };

    final static public ParityFlags t_SARb = new ParityFlags() {
        public boolean CF() {return (((lf_var1b()) >> (lf_var2b() - 1)) & 1) != 0;}
        public boolean AF() {return (lf_var2b() & 0x1f) != 0;}
        public boolean ZF() {return (lf_resb()==0);}
        public boolean SF() {return	(lf_resb()&0x80)!= 0;}
        public boolean OF() {return false;}
    };

    final static public ParityFlags t_SARw = new ParityFlags() {
        public boolean CF() {return (((lf_var1w()) >> (lf_var2b() - 1)) & 1) != 0;}
        public boolean AF() {return (lf_var2w() & 0x1f) != 0;}
        public boolean ZF() {return (lf_resw()==0);}
        public boolean SF() {return	(lf_resw()&0x8000)!=0;}
        public boolean OF() {return false;}
    };

    final static public ParityFlags t_SARd = new ParityFlags() {
        public boolean CF() {return (((lf_var1d()) >> (lf_var2b() - 1)) & 1) != 0;}
        public boolean AF() {return (lf_var2d() & 0x1f) != 0;}
        public boolean ZF() {return (lf_resd()==0);}
        public boolean SF() {return	(lf_resd()&0x80000000l)!= 0;}
        public boolean OF() {return false;}
    };

    final static public ParityFlags t_DSHLw = new ParityFlags() {
        public boolean CF() {return ((lf_var1d() >>> (32 - lf_var2b())) & 1) != 0;} /* Hmm this is not correct for shift higher than 16 */
        public boolean AF() {return false;}
        public boolean ZF() {return (lf_resw()==0);}
        public boolean SF() {return	(lf_resw()&0x8000)!=0;}
        public boolean OF() {return ((lf_resw() ^ lf_var1w()) & 0x8000) != 0;}
    };

    final static public ParityFlags t_DSHLd = new ParityFlags() {
        public boolean CF() {return ((lf_var1d() >>> (32 - lf_var2b())) & 1) != 0;}
        public boolean AF() {return false;}
        public boolean ZF() {return (lf_resd()==0);}
        public boolean SF() {return	(lf_resd()&0x80000000l)!= 0;}
        public boolean OF() {return ((lf_resd() ^ lf_var1d()) & 0x80000000l) != 0;}
    };

    final static public ParityFlags t_DSHRw = new ParityFlags() {
        public boolean CF() {return ((lf_var1d() >>> (lf_var2b() - 1)) & 1) != 0;} /* Hmm this is not correct for shift higher than 16 */
        public boolean AF() {return false;}
        public boolean ZF() {return (lf_resw()==0);}
        public boolean SF() {return	(lf_resw()&0x8000)!=0;}
        public boolean OF() {return ((lf_resw() ^ lf_var1w()) & 0x8000) != 0;}
    };

    final static public ParityFlags t_DSHRd = new ParityFlags() {
        public boolean CF() {return ((lf_var1d() >>> (lf_var2b() - 1)) & 1) != 0;}
        public boolean AF() {return false;}
        public boolean ZF() {return (lf_resd()==0);}
        public boolean SF() {return	(lf_resd()&0x80000000l)!= 0;}
        public boolean OF() {return ((lf_resd() ^ lf_var1d()) & 0x80000000l) != 0;}
    };

    final static public ParityFlags t_DIV = new ParityFlags() {
        public boolean CF() {return false;}
        public boolean AF() {return false;}
        public boolean ZF() {return false;}
        public boolean SF() {return false;}
        public boolean OF() {return false;}
    };

    final static public ParityFlags t_NEGb = new ParityFlags() {
        public boolean CF() {return lf_var1b() != 0;}
        public boolean AF() {return (lf_var1b() & 0x0f) != 0;}
        public boolean ZF() {return (lf_resb()==0);}
        public boolean SF() {return	(lf_resb()&0x80)!= 0;}
        public boolean OF() {return (lf_var1b() == 0x80);}
    };

    final static public ParityFlags t_NEGw = new ParityFlags() {
        public boolean CF() {return lf_var1w() != 0;}
        public boolean AF() {return (lf_var1w() & 0x0f) != 0;}
        public boolean ZF() {return (lf_resw()==0);}
        public boolean SF() {return	(lf_resw()&0x8000)!=0;}
        public boolean OF() {return (lf_var1w() == 0x8000);}
    };

    final static public ParityFlags t_NEGd = new ParityFlags() {
        public boolean CF() {return lf_var1d() != 0;}
        public boolean AF() {return (lf_var1d() & 0x0f) != 0;}
        public boolean ZF() {return (lf_resd()==0);}
        public boolean SF() {return	(lf_resd()&0x80000000l)!= 0;}
        public boolean OF() {return (lf_var1d() == 0x80000000l);}
    };
    /* CF     Carry Flag -- Set on high-order bit carry or borrow; cleared
          otherwise.
    */
    static public boolean get_CF() {
        return lflags.type.CF();
    }

    /* AF     Adjust flag -- Set on carry from or borrow to the low order
            four bits of   AL; cleared otherwise. Used for decimal
            arithmetic.
    */
    static public boolean get_AF() {
        return lflags.type.AF();
    }

    // ZF     Zero Flag -- Set if result is zero; cleared otherwise.
    static public boolean get_ZF() {
        return lflags.type.ZF();
    }

    /* SF     Sign Flag -- Set equal to high-order bit of result (0 is
            positive, 1 if negative).
    */
    static public boolean get_SF() {
        return lflags.type.SF();
    }

    static public boolean get_OF() {
        return lflags.type.OF();
    }

    static short[] parity_lookup = new short[] {
        CPU_Regs.PF, 0, 0, CPU_Regs.PF, 0, CPU_Regs.PF, CPU_Regs.PF, 0, 0, CPU_Regs.PF, CPU_Regs.PF, 0, CPU_Regs.PF, 0, 0, CPU_Regs.PF,
        0, CPU_Regs.PF, CPU_Regs.PF, 0, CPU_Regs.PF, 0, 0, CPU_Regs.PF, CPU_Regs.PF, 0, 0, CPU_Regs.PF, 0, CPU_Regs.PF, CPU_Regs.PF, 0,
        0, CPU_Regs.PF, CPU_Regs.PF, 0, CPU_Regs.PF, 0, 0, CPU_Regs.PF, CPU_Regs.PF, 0, 0, CPU_Regs.PF, 0, CPU_Regs.PF, CPU_Regs.PF, 0,
        CPU_Regs.PF, 0, 0, CPU_Regs.PF, 0, CPU_Regs.PF, CPU_Regs.PF, 0, 0, CPU_Regs.PF, CPU_Regs.PF, 0, CPU_Regs.PF, 0, 0, CPU_Regs.PF,
        0, CPU_Regs.PF, CPU_Regs.PF, 0, CPU_Regs.PF, 0, 0, CPU_Regs.PF, CPU_Regs.PF, 0, 0, CPU_Regs.PF, 0, CPU_Regs.PF, CPU_Regs.PF, 0,
        CPU_Regs.PF, 0, 0, CPU_Regs.PF, 0, CPU_Regs.PF, CPU_Regs.PF, 0, 0, CPU_Regs.PF, CPU_Regs.PF, 0, CPU_Regs.PF, 0, 0, CPU_Regs.PF,
        CPU_Regs.PF, 0, 0, CPU_Regs.PF, 0, CPU_Regs.PF, CPU_Regs.PF, 0, 0, CPU_Regs.PF, CPU_Regs.PF, 0, CPU_Regs.PF, 0, 0, CPU_Regs.PF,
        0, CPU_Regs.PF, CPU_Regs.PF, 0, CPU_Regs.PF, 0, 0, CPU_Regs.PF, CPU_Regs.PF, 0, 0, CPU_Regs.PF, 0, CPU_Regs.PF, CPU_Regs.PF, 0,
        0, CPU_Regs.PF, CPU_Regs.PF, 0, CPU_Regs.PF, 0, 0, CPU_Regs.PF, CPU_Regs.PF, 0, 0, CPU_Regs.PF, 0, CPU_Regs.PF, CPU_Regs.PF, 0,
        CPU_Regs.PF, 0, 0, CPU_Regs.PF, 0, CPU_Regs.PF, CPU_Regs.PF, 0, 0, CPU_Regs.PF, CPU_Regs.PF, 0, CPU_Regs.PF, 0, 0, CPU_Regs.PF,
        CPU_Regs.PF, 0, 0, CPU_Regs.PF, 0, CPU_Regs.PF, CPU_Regs.PF, 0, 0, CPU_Regs.PF, CPU_Regs.PF, 0, CPU_Regs.PF, 0, 0, CPU_Regs.PF,
        0, CPU_Regs.PF, CPU_Regs.PF, 0, CPU_Regs.PF, 0, 0, CPU_Regs.PF, CPU_Regs.PF, 0, 0, CPU_Regs.PF, 0, CPU_Regs.PF, CPU_Regs.PF, 0,
        CPU_Regs.PF, 0, 0, CPU_Regs.PF, 0, CPU_Regs.PF, CPU_Regs.PF, 0, 0, CPU_Regs.PF, CPU_Regs.PF, 0, CPU_Regs.PF, 0, 0, CPU_Regs.PF,
        0, CPU_Regs.PF, CPU_Regs.PF, 0, CPU_Regs.PF, 0, 0, CPU_Regs.PF, CPU_Regs.PF, 0, 0, CPU_Regs.PF, 0, CPU_Regs.PF, CPU_Regs.PF, 0,
        0, CPU_Regs.PF, CPU_Regs.PF, 0, CPU_Regs.PF, 0, 0, CPU_Regs.PF, CPU_Regs.PF, 0, 0, CPU_Regs.PF, 0, CPU_Regs.PF, CPU_Regs.PF, 0,
        CPU_Regs.PF, 0, 0, CPU_Regs.PF, 0, CPU_Regs.PF, CPU_Regs.PF, 0, 0, CPU_Regs.PF, CPU_Regs.PF, 0, CPU_Regs.PF, 0, 0, CPU_Regs.PF
        };

    static public boolean get_PF() {
        return lflags.type.PF();
    }

    static public boolean TFLG_O() {
        return get_OF();
    }

    static public boolean TFLG_NO () {
        return !get_OF();
    }

    static public boolean TFLG_B() {
        return get_CF();
    }

    static public boolean TFLG_NB() {
        return !get_CF();
    }

    static public boolean TFLG_Z() {
        return get_ZF();
    }

    static public boolean TFLG_NZ() {
        return !get_ZF();
    }

    static public boolean TFLG_BE() {
        return (get_CF() || get_ZF());
    }

    static public boolean TFLG_NBE() {
        return (!get_CF() && !get_ZF());
    }

    static public boolean TFLG_S() {
        return get_SF();
    }

    static public boolean TFLG_NS() {
        return !get_SF();
    }

    static public boolean TFLG_P() {
        return get_PF();
    }

    static public boolean TFLG_NP() {
        return !get_PF();
    }

    static public boolean TFLG_L() {
        return ((get_SF()) != (get_OF()));
    }

    static public boolean TFLG_NL() {
        return ((get_SF()) == (get_OF()));
    }

    static public boolean TFLG_LE() {
        return (get_ZF()  || ((get_SF()) != (get_OF())));
    }

    static public boolean TFLG_NLE() {
        return (!get_ZF() && ((get_SF()) == (get_OF())));
    }

    static private void SET_FLAG(int flag, boolean set) {
        CPU_Regs.SETFLAGBIT(flag, set);
    }

    static public /*Bitu*/int FillFlags() {
        if (lflags.type != t_UNKNOWN) {
            SET_FLAG(CPU_Regs.CF,lflags.type.CF());
            SET_FLAG(CPU_Regs.AF,lflags.type.AF());
            SET_FLAG(CPU_Regs.ZF,lflags.type.ZF());
            SET_FLAG(CPU_Regs.SF,lflags.type.SF());
            SET_FLAG(CPU_Regs.OF,lflags.type.OF());
            SET_FLAG(CPU_Regs.PF,lflags.type.PF());
            lflags.type=t_UNKNOWN;
        }
        return CPU_Regs.flags;
    }

    public static void FillFlagsNoCFOF() {
        if (lflags.type != t_UNKNOWN) {
            SET_FLAG(CPU_Regs.AF,lflags.type.AF());
            SET_FLAG(CPU_Regs.ZF,lflags.type.ZF());
            SET_FLAG(CPU_Regs.SF,lflags.type.SF());
            SET_FLAG(CPU_Regs.OF,lflags.type.OF());
            SET_FLAG(CPU_Regs.PF,lflags.type.PF());
            lflags.type=t_UNKNOWN;
        }
    }

    static public void DestroyConditionFlags() {
        lflags.type=t_UNKNOWN;
    }

}
