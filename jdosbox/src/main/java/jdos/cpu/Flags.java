package jdos.cpu;

public class Flags {
    // needs to come before creating LazyFlags
    static public GetFlags t_UNKNOWN = new GetFlags() {
        public boolean CF() {return CPU_Regs.GETFLAG(CPU_Regs.CF)!=0;}
        public boolean AF() {return CPU_Regs.GETFLAG(CPU_Regs.AF) != 0;}
        public boolean ZF() {return CPU_Regs.GETFLAG(CPU_Regs.ZF) != 0;}
        public boolean SF() {return CPU_Regs.GETFLAG(CPU_Regs.SF) != 0;}
        public boolean OF() {return CPU_Regs.GETFLAG(CPU_Regs.OF) != 0;}
    };

    public static int var1;
    public static int var2;
    public static int res;
    public static boolean oldcf;
    public static Flags.GetFlags type = Flags.t_UNKNOWN;

    static public void copy(LazyFlags in) {
        var1 = in.var1;
        var2 = in.var2;
        res=in.res;
        type = in.type;
        oldcf = in.oldcf;
    }

    public static int lf_var1b() {
        return var1;
    }

    public static void lf_var1b(int b) {
        var1=b & 0xFF;
    }

    public static int lf_var2b() {
        return var2;
    }

    public static void lf_var2b(int b) {
        var2=b & 0xFF;
    }

    public static int lf_resb() {
        return res;
    }

    public static void lf_resb(int b) {
        res = b & 0xFF;
    }

    public static int lf_var1w() {
        return var1;
    }

    public static void lf_var1w(int s) {
        var1=s & 0xFFFF;
    }

    public static int lf_var2w() {
        return var2;
    }

//    public static void lf_var2w(int s) {
//        var2=var2 & ~0xFFFF;
//        var2=var2 | s & 0xFFFF;
//    }

    public static int lf_resw() {
        return res;
    }

    public static void lf_resw(int s) {
        res = s & 0xFFFF;
    }

    public static int lf_var1d() {
        return var1;
    }

    public static void lf_var1d(int v) {
        var1=v;
    }

    public static int lf_var2d() {
        return var2;
    }

    public static void lf_var2d(int v) {
        var2=v;
    }

    public static int lf_resd() {
        return res;
    }

    public static void lf_resd(int v) {
        res=v;
    }

    public static void SETFLAGSb(int FLAGB) {
        CPU_Regs.SETFLAGBIT(CPU_Regs.OF,get_OF());
        type=t_UNKNOWN;
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
    }

    final static public GetFlags t_INCb = new GetFlags() {
        public boolean CF() {return CPU_Regs.GETFLAG(CPU_Regs.CF)!=0;}
        public boolean AF() {return (lf_resb() & 0x0f) == 0;}
        public boolean ZF() {return (lf_resb()==0);}
        public boolean SF() {return	(lf_resb()&0x80)!= 0;}
        public boolean OF() {return (lf_resb() == 0x80);}
    };

    final static public GetFlags t_INCw = new GetFlags() {
        public boolean CF() {return CPU_Regs.GETFLAG(CPU_Regs.CF)!=0;}
        public boolean AF() {return (lf_resw() & 0x0f) == 0;}
        public boolean ZF() {return (lf_resw()==0);}
        public boolean SF() {return	(lf_resw()&0x8000)!=0;}
        public boolean OF() {return (lf_resw() == 0x8000);}
    };

    final static public GetFlags t_INCd = new GetFlags() {
        public boolean CF() {return CPU_Regs.GETFLAG(CPU_Regs.CF)!=0;}
        public boolean AF() {return (lf_resd() & 0x0f) == 0;}
        public boolean ZF() {return (lf_resd()==0);}
        public boolean SF() {return	(lf_resd()&0x80000000)!= 0;}
        public boolean OF() {return (lf_resd() == 0x80000000);}
    };

    final static public GetFlags t_DECb = new GetFlags() {
        public boolean CF() {return CPU_Regs.GETFLAG(CPU_Regs.CF)!=0;}
        public boolean AF() {return (lf_resb() & 0x0f) == 0x0f;}
        public boolean ZF() {return (lf_resb()==0);}
        public boolean SF() {return	(lf_resb()&0x80)!= 0;}
        public boolean OF() {return (lf_resb() == 0x7f);}
    };

    final static public GetFlags t_DECw = new GetFlags() {
        public boolean CF() {return CPU_Regs.GETFLAG(CPU_Regs.CF)!=0;}
        public boolean AF() {return (lf_resw() & 0x0f) == 0x0f;}
        public boolean ZF() {return (lf_resw()==0);}
        public boolean SF() {return	(lf_resw()&0x8000)!=0;}
        public boolean OF() {return (lf_resw() == 0x7fff);}
    };

    final static public GetFlags t_DECd = new GetFlags() {
        public boolean CF() {return CPU_Regs.GETFLAG(CPU_Regs.CF)!=0;}
        public boolean AF() {return (lf_resd() & 0x0f) == 0x0f;}
        public boolean ZF() {return (lf_resd()==0);}
        public boolean SF() {return	(lf_resd()&0x80000000l)!= 0;}
        public boolean OF() {return (lf_resd() == 0x7fffffff);}
    };

    final static public GetFlags t_ADDb = new GetFlags() {
        public boolean CF() {return (lf_resb()<lf_var1b());}
        public boolean AF() {return (((lf_var1b() ^ lf_var2b()) ^ lf_resb()) & 0x10) !=0;}
        public boolean ZF() {return (lf_resb()==0);}
        public boolean SF() {return	(lf_resb()&0x80)!= 0;}
        public boolean OF() {return (((lf_var1b() ^ lf_var2b() ^ 0x80) & (lf_resb() ^ lf_var2b())) & 0x80) != 0;}
    };

    final static public GetFlags t_ADDw = new GetFlags() {
        public boolean CF() {return (lf_resw()<lf_var1w());}
        public boolean AF() {return (((lf_var1w() ^ lf_var2w()) ^ lf_resw()) & 0x10) != 0;}
        public boolean ZF() {return (lf_resw()==0);}
        public boolean SF() {return	(lf_resw()&0x8000)!=0;}
        public boolean OF() {return (((lf_var1w() ^ lf_var2w() ^ 0x8000) & (lf_resw() ^ lf_var2w())) & 0x8000) != 0;}
    };

    final static public GetFlags t_ADDd = new GetFlags() {
        public boolean CF() {return ((lf_resd() & 0xFFFFFFFFl)<(lf_var1d() & 0xFFFFFFFFl));}
        public boolean AF() {return (((lf_var1d() ^ lf_var2d()) ^ lf_resd()) & 0x10) != 0;}
        public boolean ZF() {return (lf_resd()==0);}
        public boolean SF() {return	(lf_resd()&0x80000000)!= 0;}
        public boolean OF() {return (((lf_var1d() ^ lf_var2d() ^ 0x80000000) & (lf_resd() ^ lf_var2d())) & 0x80000000) != 0;}
    };

    final static public GetFlags t_ADCb = new GetFlags() {
        public boolean CF() {return (lf_resb() < lf_var1b()) || (oldcf && (lf_resb() == lf_var1b()));}
        public boolean AF() {return (((lf_var1b() ^ lf_var2b()) ^ lf_resb()) & 0x10) !=0;}
        public boolean ZF() {return (lf_resb()==0);}
        public boolean SF() {return	(lf_resb()&0x80)!= 0;}
        public boolean OF() {return (((lf_var1b() ^ lf_var2b() ^ 0x80) & (lf_resb() ^ lf_var2b())) & 0x80) != 0;}
    };

    final static public GetFlags t_ADCw = new GetFlags() {
        public boolean CF() {return (lf_resw() < lf_var1w()) || (oldcf && (lf_resw() == lf_var1w()));}
        public boolean AF() {return (((lf_var1w() ^ lf_var2w()) ^ lf_resw()) & 0x10) != 0;}
        public boolean ZF() {return (lf_resw()==0);}
        public boolean SF() {return	(lf_resw()&0x8000)!=0;}
        public boolean OF() {return (((lf_var1w() ^ lf_var2w() ^ 0x8000) & (lf_resw() ^ lf_var2w())) & 0x8000) != 0;}
    };

    final static public GetFlags t_ADCd = new GetFlags() {
        public boolean CF() {return ((lf_resd() & 0xFFFFFFFFl) < (lf_var1d() & 0xFFFFFFFFl)) || (oldcf && (lf_resd() == lf_var1d()));}
        public boolean AF() {return (((lf_var1d() ^ lf_var2d()) ^ lf_resd()) & 0x10) != 0;}
        public boolean ZF() {return (lf_resd()==0);}
        public boolean SF() {return	(lf_resd()&0x80000000)!= 0;}
        public boolean OF() {return (((lf_var1d() ^ lf_var2d() ^ 0x80000000) & (lf_resd() ^ lf_var2d())) & 0x80000000) != 0;}
    };

    final static public GetFlags t_ORb = new GetFlags() {
        public boolean CF() {return false;}
        public boolean AF() {return false;}
        public boolean ZF() {return (lf_resb()==0);}
        public boolean SF() {return	(lf_resb()&0x80)!= 0;}
        public boolean OF() {return false;}
    };

    final static public GetFlags t_ORw = new GetFlags() {
        public boolean CF() {return false;}
        public boolean AF() {return false;}
        public boolean ZF() {return (lf_resw()==0);}
        public boolean SF() {return	(lf_resw()&0x8000)!=0;}
        public boolean OF() {return false;}
    };

    final static public GetFlags t_ORd = new GetFlags() {
        public boolean CF() {return false;}
        public boolean AF() {return false;}
        public boolean ZF() {return (lf_resd()==0);}
        public boolean SF() {return	(lf_resd()&0x80000000)!= 0;}
        public boolean OF() {return false;}
    };

    final static public GetFlags t_ANDb = new GetFlags() {
        public boolean CF() {return false;}
        public boolean AF() {return false;}
        public boolean ZF() {return (lf_resb()==0);}
        public boolean SF() {return	(lf_resb()&0x80)!= 0;}
        public boolean OF() {return false;}
    };

    final static public GetFlags t_ANDw = new GetFlags() {
        public boolean CF() {return false;}
        public boolean AF() {return false;}
        public boolean ZF() {return (lf_resw()==0);}
        public boolean SF() {return	(lf_resw()&0x8000)!=0;}
        public boolean OF() {return false;}
    };

    final static public GetFlags t_ANDd = new GetFlags() {
        public boolean CF() {return false;}
        public boolean AF() {return false;}
        public boolean ZF() {return (lf_resd()==0);}
        public boolean SF() {return	(lf_resd()&0x80000000)!= 0;}
        public boolean OF() {return false;}
    };

    final static public GetFlags t_TESTb = new GetFlags() {
        public boolean CF() {return false;}
        public boolean AF() {return false;}
        public boolean ZF() {return (lf_resb()==0);}
        public boolean SF() {return	(lf_resb()&0x80)!= 0;}
        public boolean OF() {return false;}
    };

    final static public GetFlags t_TESTw = new GetFlags() {
        public boolean CF() {return false;}
        public boolean AF() {return false;}
        public boolean ZF() {return (lf_resw()==0);}
        public boolean SF() {return	(lf_resw()&0x8000)!=0;}
        public boolean OF() {return false;}
    };

    final static public GetFlags t_TESTd = new GetFlags() {
        public boolean CF() {return false;}
        public boolean AF() {return false;}
        public boolean ZF() {return (lf_resd()==0);}
        public boolean SF() {return	(lf_resd()&0x80000000)!= 0;}
        public boolean OF() {return false;}
    };

    final static public GetFlags t_XORb = new GetFlags() {
        public boolean CF() {return false;}
        public boolean AF() {return false;}
        public boolean ZF() {return (lf_resb()==0);}
        public boolean SF() {return	(lf_resb()&0x80)!= 0;}
        public boolean OF() {return false;}
    };

    final static public GetFlags t_XORw = new GetFlags() {
        public boolean CF() {return false;}
        public boolean AF() {return false;}
        public boolean ZF() {return (lf_resw()==0);}
        public boolean SF() {return	(lf_resw()&0x8000)!=0;}
        public boolean OF() {return false;}
    };

    final static public GetFlags t_XORd = new GetFlags() {
        public boolean CF() {return false;}
        public boolean AF() {return false;}
        public boolean ZF() {return (lf_resd()==0);}
        public boolean SF() {return	(lf_resd()&0x80000000)!= 0;}
        public boolean OF() {return false;}
    };

    final static public GetFlags t_SUBb = new GetFlags() {
        public boolean CF() {return (lf_var1b()<lf_var2b());}
        public boolean AF() {return (((lf_var1b() ^ lf_var2b()) ^ lf_resb()) & 0x10) !=0;}
        public boolean ZF() {return (lf_resb()==0);}
        public boolean SF() {return	(lf_resb()&0x80)!= 0;}
        public boolean OF() {return (((lf_var1b() ^ lf_var2b()) & (lf_var1b() ^ lf_resb())) & 0x80) != 0;}
    };

    final static public GetFlags t_SUBw = new GetFlags() {
        public boolean CF() {return (lf_var1w()<lf_var2w());}
        public boolean AF() {return (((lf_var1w() ^ lf_var2w()) ^ lf_resw()) & 0x10) != 0;}
        public boolean ZF() {return (lf_resw()==0);}
        public boolean SF() {return	(lf_resw()&0x8000)!=0;}
        public boolean OF() {return (((lf_var1w() ^ lf_var2w()) & (lf_var1w() ^ lf_resw())) & 0x8000) != 0;}
    };

    final static public GetFlags t_SUBd = new GetFlags() {
        public boolean CF() {return ((lf_var1d() & 0xFFFFFFFFl)<(lf_var2d() & 0xFFFFFFFFl));}
        public boolean AF() {return (((lf_var1d() ^ lf_var2d()) ^ lf_resd()) & 0x10) != 0;}
        public boolean ZF() {return (lf_resd()==0);}
        public boolean SF() {return	(lf_resd()&0x80000000)!= 0;}
        public boolean OF() {return (((lf_var1d() ^ lf_var2d()) & (lf_var1d() ^ lf_resd())) & 0x80000000) != 0;}
    };

    final static public GetFlags t_CMPb = new GetFlags() {
        public boolean CF() {return (lf_var1b()<lf_var2b());}
        public boolean AF() {return (((lf_var1b() ^ lf_var2b()) ^ lf_resb()) & 0x10) !=0;}
        public boolean ZF() {return (lf_resb()==0);}
        public boolean SF() {return	(lf_resb()&0x80)!= 0;}
        public boolean OF() {return (((lf_var1b() ^ lf_var2b()) & (lf_var1b() ^ lf_resb())) & 0x80) != 0;}
    };

    final static public GetFlags t_CMPw = new GetFlags() {
        public boolean CF() {return (lf_var1w()<lf_var2w());}
        public boolean AF() {return (((lf_var1w() ^ lf_var2w()) ^ lf_resw()) & 0x10) != 0;}
        public boolean ZF() {return (lf_resw()==0);}
        public boolean SF() {return	(lf_resw()&0x8000)!=0;}
        public boolean OF() {return (((lf_var1w() ^ lf_var2w()) & (lf_var1w() ^ lf_resw())) & 0x8000) != 0;}
    };

    final static public GetFlags t_CMPd = new GetFlags() {
        public boolean CF() {return ((lf_var1d() & 0xFFFFFFFFl)<(lf_var2d() & 0xFFFFFFFFl));}
        public boolean AF() {return (((lf_var1d() ^ lf_var2d()) ^ lf_resd()) & 0x10) != 0;}
        public boolean ZF() {return (lf_resd()==0);}
        public boolean SF() {return	(lf_resd()&0x80000000)!= 0;}
        public boolean OF() {return (((lf_var1d() ^ lf_var2d()) & (lf_var1d() ^ lf_resd())) & 0x80000000) != 0;}
    };

    final static public GetFlags t_SBBb = new GetFlags() {
        public boolean CF() {return (lf_var1b() < lf_resb()) || (oldcf && (lf_var2b()==0xff));}
        public boolean AF() {return (((lf_var1b() ^ lf_var2b()) ^ lf_resb()) & 0x10) !=0;}
        public boolean ZF() {return (lf_resb()==0);}
        public boolean SF() {return	(lf_resb()&0x80)!= 0;}
        public boolean OF() {return (((lf_var1b() ^ lf_var2b()) & (lf_var1b() ^ lf_resb())) & 0x80) != 0;}
    };

    final static public GetFlags t_SBBw = new GetFlags() {
        public boolean CF() {return (lf_var1w() < lf_resw()) || (oldcf && (lf_var2w()==0xffff));}
        public boolean AF() {return (((lf_var1w() ^ lf_var2w()) ^ lf_resw()) & 0x10) != 0;}
        public boolean ZF() {return (lf_resw()==0);}
        public boolean SF() {return	(lf_resw()&0x8000)!=0;}
        public boolean OF() {return (((lf_var1w() ^ lf_var2w()) & (lf_var1w() ^ lf_resw())) & 0x8000) != 0;}
    };

    final static public GetFlags t_SBBd = new GetFlags() {
        public boolean CF() {return ((lf_var1d() & 0xFFFFFFFFl) < (lf_resd() & 0xFFFFFFFFl)) || (oldcf && (lf_var2d()==0xffffffff));}
        public boolean AF() {return (((lf_var1d() ^ lf_var2d()) ^ lf_resd()) & 0x10) != 0;}
        public boolean ZF() {return (lf_resd()==0);}
        public boolean SF() {return	(lf_resd()&0x80000000)!= 0;}
        public boolean OF() {return (((lf_var1d() ^ lf_var2d()) & (lf_var1d() ^ lf_resd())) & 0x80000000) != 0;}
    };

    final static public GetFlags t_MUL = new GetFlags() {
        public boolean CF() {return CPU_Regs.GETFLAG(CPU_Regs.CF)!=0;}
        public boolean AF() {return false;}
        public boolean ZF() {return false;}
        public boolean SF() {return false;}
        public boolean OF() {return CPU_Regs.GETFLAG(CPU_Regs.OF) != 0;}
    };

    final static public GetFlags t_SHLb = new GetFlags() {
        public boolean CF() {return lf_var2b()<=8 && ((lf_var1b() >> (8-lf_var2b())) & 1)!=0;}
        public boolean AF() {return (lf_var2b() & 0x1f) != 0;}
        public boolean ZF() {return (lf_resb()==0);}
        public boolean SF() {return	(lf_resb()&0x80)!= 0;}
        public boolean OF() {return ((lf_resb() ^ lf_var1b()) & 0x80) != 0;}
    };

    final static public GetFlags t_SHLw = new GetFlags() {
        public boolean CF() {return lf_var2b()<=16 && ((lf_var1w()) >> (16-lf_var2b()) & 1)!=0;}
        public boolean AF() {return (lf_var2w() & 0x1f) != 0;}
        public boolean ZF() {return (lf_resw()==0);}
        public boolean SF() {return	(lf_resw()&0x8000)!=0;}
        public boolean OF() {return ((lf_resw() ^ lf_var1w()) & 0x8000) != 0;}
    };

    final static public GetFlags t_SHLd = new GetFlags() {
        public boolean CF() {return ((lf_var1d() >>> (32 - lf_var2b())) & 1) != 0;}
        public boolean AF() {return (lf_var2d() & 0x1f) != 0;}
        public boolean ZF() {return (lf_resd()==0);}
        public boolean SF() {return	(lf_resd()&0x80000000)!= 0;}
        public boolean OF() {return ((lf_resd() ^ lf_var1d()) & 0x80000000) != 0;}
    };

    final static public GetFlags t_SHRb = new GetFlags() {
        public boolean CF() {return ((lf_var1b() >> (lf_var2b() - 1)) & 1) !=0;}
        public boolean AF() {return (lf_var2b() & 0x1f) != 0;}
        public boolean ZF() {return (lf_resb()==0);}
        public boolean SF() {return	(lf_resb()&0x80)!= 0;}
        public boolean OF() {return (lf_var2b()&0x1f)==1 && lf_var1b() > 0x80;}
    };

    final static public GetFlags t_SHRw = new GetFlags() {
        public boolean CF() {return ((lf_var1w() >> (lf_var2b() - 1)) & 1) !=0;}
        public boolean AF() {return (lf_var2w() & 0x1f) != 0;}
        public boolean ZF() {return (lf_resw()==0);}
        public boolean SF() {return	(lf_resw()&0x8000)!=0;}
        public boolean OF() {return (lf_var2b()&0x1f)==1 && lf_var1w() > 0x8000;}
    };

    final static public GetFlags t_SHRd = new GetFlags() {
        public boolean CF() {return ((lf_var1d() >>> (lf_var2b() - 1)) & 1) != 0;}
        public boolean AF() {return (lf_var2d() & 0x1f) != 0;}
        public boolean ZF() {return (lf_resd()==0);}
        public boolean SF() {return	(lf_resd()&0x80000000)!= 0;}
        public boolean OF() {return (lf_var2b()&0x1f)==1 && (lf_var1d() & 0xFFFFFFFFl) > 0x80000000l;}
    };

    final static public GetFlags t_SARb = new GetFlags() {
        public boolean CF() {return (((lf_var1b()) >> (lf_var2b() - 1)) & 1) != 0;}
        public boolean AF() {return (lf_var2b() & 0x1f) != 0;}
        public boolean ZF() {return (lf_resb()==0);}
        public boolean SF() {return	(lf_resb()&0x80)!= 0;}
        public boolean OF() {return false;}
    };

    final static public GetFlags t_SARw = new GetFlags() {
        public boolean CF() {return (((lf_var1w()) >> (lf_var2b() - 1)) & 1) != 0;}
        public boolean AF() {return (lf_var2w() & 0x1f) != 0;}
        public boolean ZF() {return (lf_resw()==0);}
        public boolean SF() {return	(lf_resw()&0x8000)!=0;}
        public boolean OF() {return false;}
    };

    final static public GetFlags t_SARd = new GetFlags() {
        public boolean CF() {return (((lf_var1d()) >> (lf_var2b() - 1)) & 1) != 0;}
        public boolean AF() {return (lf_var2d() & 0x1f) != 0;}
        public boolean ZF() {return (lf_resd()==0);}
        public boolean SF() {return	(lf_resd()&0x80000000)!= 0;}
        public boolean OF() {return false;}
    };

    final static public GetFlags t_DSHLw = new GetFlags() {
        public boolean CF() {return ((lf_var1d() >>> (32 - lf_var2b())) & 1) != 0;} /* Hmm this is not correct for shift higher than 16 */
        public boolean AF() {return false;}
        public boolean ZF() {return (lf_resw()==0);}
        public boolean SF() {return	(lf_resw()&0x8000)!=0;}
        public boolean OF() {return ((lf_resw() ^ lf_var1w()) & 0x8000) != 0;}
    };

    final static public GetFlags t_DSHLd = new GetFlags() {
        public boolean CF() {return ((lf_var1d() >>> (32 - lf_var2b())) & 1) != 0;}
        public boolean AF() {return false;}
        public boolean ZF() {return (lf_resd()==0);}
        public boolean SF() {return	(lf_resd()&0x80000000)!= 0;}
        public boolean OF() {return ((lf_resd() ^ lf_var1d()) & 0x80000000) != 0;}
    };

    final static public GetFlags t_DSHRw = new GetFlags() {
        public boolean CF() {return ((lf_var1d() >>> (lf_var2b() - 1)) & 1) != 0;} /* Hmm this is not correct for shift higher than 16 */
        public boolean AF() {return false;}
        public boolean ZF() {return (lf_resw()==0);}
        public boolean SF() {return	(lf_resw()&0x8000)!=0;}
        public boolean OF() {return ((lf_resw() ^ lf_var1w()) & 0x8000) != 0;}
    };

    final static public GetFlags t_DSHRd = new GetFlags() {
        public boolean CF() {return ((lf_var1d() >>> (lf_var2b() - 1)) & 1) != 0;}
        public boolean AF() {return false;}
        public boolean ZF() {return (lf_resd()==0);}
        public boolean SF() {return	(lf_resd()&0x80000000)!= 0;}
        public boolean OF() {return ((lf_resd() ^ lf_var1d()) & 0x80000000) != 0;}
    };

    final static public GetFlags t_DIV = new GetFlags() {
        public boolean CF() {return false;}
        public boolean AF() {return false;}
        public boolean ZF() {return false;}
        public boolean SF() {return false;}
        public boolean OF() {return false;}
    };

    final static public GetFlags t_NEGb = new GetFlags() {
        public boolean CF() {return lf_var1b() != 0;}
        public boolean AF() {return (lf_var1b() & 0x0f) != 0;}
        public boolean ZF() {return (lf_resb()==0);}
        public boolean SF() {return	(lf_resb()&0x80)!= 0;}
        public boolean OF() {return (lf_var1b() == 0x80);}
    };

    final static public GetFlags t_NEGw = new GetFlags() {
        public boolean CF() {return lf_var1w() != 0;}
        public boolean AF() {return (lf_var1w() & 0x0f) != 0;}
        public boolean ZF() {return (lf_resw()==0);}
        public boolean SF() {return	(lf_resw()&0x8000)!=0;}
        public boolean OF() {return (lf_var1w() == 0x8000);}
    };

    final static public GetFlags t_NEGd = new GetFlags() {
        public boolean CF() {return lf_var1d() != 0;}
        public boolean AF() {return (lf_var1d() & 0x0f) != 0;}
        public boolean ZF() {return (lf_resd()==0);}
        public boolean SF() {return	(lf_resd()&0x80000000)!= 0;}
        public boolean OF() {return (lf_var1d() == 0x80000000);}
    };
    /* CF     Carry Flag -- Set on high-order bit carry or borrow; cleared
          otherwise.
    */
    static public boolean get_CF() {
        return type.CF();
    }

    /* AF     Adjust flag -- Set on carry from or borrow to the low order
            four bits of   AL; cleared otherwise. Used for decimal
            arithmetic.
    */
    static public boolean get_AF() {
        return type.AF();
    }

    // ZF     Zero Flag -- Set if result is zero; cleared otherwise.
    static public boolean get_ZF() {
        return type.ZF();
    }

    /* SF     Sign Flag -- Set equal to high-order bit of result (0 is
            positive, 1 if negative).
    */
    static public boolean get_SF() {
        return type.SF();
    }

    static public boolean get_OF() {
        return type.OF();
    }

    public static int[] parity_lookup = new int[] {
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
        if (type != t_UNKNOWN) {
            return (parity_lookup[lf_resb() & 0xFF]) != 0;
        }
        return CPU_Regs.GETFLAG(CPU_Regs.PF)!=0;
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
        if (type != t_UNKNOWN) {
            SET_FLAG(CPU_Regs.CF,type.CF());
            SET_FLAG(CPU_Regs.AF,type.AF());
            SET_FLAG(CPU_Regs.ZF,type.ZF());
            SET_FLAG(CPU_Regs.SF,type.SF());
            SET_FLAG(CPU_Regs.OF,type.OF());
            SET_FLAG(CPU_Regs.PF,(parity_lookup[lf_resb() & 0xFF]) != 0);
            type=t_UNKNOWN;
        }
        return CPU_Regs.flags;
    }

    public static void FillFlagsNoCFOF() {
        if (type != t_UNKNOWN) {
            SET_FLAG(CPU_Regs.AF,type.AF());
            SET_FLAG(CPU_Regs.ZF,type.ZF());
            SET_FLAG(CPU_Regs.SF,type.SF());
            SET_FLAG(CPU_Regs.PF,(parity_lookup[lf_resb() & 0xFF]) != 0);
            type=t_UNKNOWN;
        }
    }

    static public void DestroyConditionFlags() {
        type=t_UNKNOWN;
    }

}
