package jdos.cpu.core_dynamic;

import jdos.cpu.CPU;
import jdos.cpu.CPU_Regs;
import jdos.cpu.Callback;
import jdos.cpu.Flags;
import jdos.cpu.core_share.Constants;
import jdos.cpu.core_share.Data;

abstract public class Op {
    static final public int EAX = 1;

    static final public int ES = 0x01;
    static final public int CS = 0x02;
    static final public int DS = 0x03;
    static final public int FS = 0x04;
    static final public int GS = 0x05;
    static final public int SS = 0x06;
    static final public int FROM_REG = 0x10;
    static final public int FROM_STACK = 0x20;
    static final public int FROM_MEMORY = 0x30;

    public int c=-1;
    public int eip_count=0;
    public Op next;
    public int cycle = 0;

    abstract public int call();

    public int RUNEXCEPTION() {
        CPU.CPU_Exception(CPU.cpu.exception.which, CPU.cpu.exception.error);
        return Constants.BR_Jump;
    }

    public int EXCEPTION(int blah) {
        CPU.CPU_Exception(blah);
        return Constants.BR_Jump;
    }

    public int DECODE_END(int count) {
        CPU_Regs.reg_eip += count;
        Flags.FillFlags();
        return CB_NONE();
    }

    public int CB_NONE() {
        Data.callback = Callback.CBRET_NONE;
        return Constants.BR_CallBack;
    }

    public int sets() { return 0; }
    public int gets() { return 0; }

    public boolean returnsIllegal() {return false;}
    public int setsSeg() {return 0;}
    public String description() {return this.toString();}

//    public boolean throwsException() {return true;}
//    public boolean accessesMemory() {return true;}
//    public  boolean usesEip() {return true;}
//    public  boolean setsEip() {return true;}
    public boolean throwsException() {return false;}
    public boolean accessesMemory() {return false;}
    public boolean usesEip() {return false;}
    public boolean setsEip() {return false;}
    public int getFlagType() {return FLAG_TYPE_NONE;}


    public static final int FLAG_TYPE_NONE = 0;
    public static final int FLAG_TYPE_INCB = 1;
    public static final int FLAG_TYPE_INCW = 2;
    public static final int FLAG_TYPE_INCD = 3;
    public static final int FLAG_TYPE_DECB = 4;
    public static final int FLAG_TYPE_DECW = 5;
    public static final int FLAG_TYPE_DECD = 6;
    public static final int FLAG_TYPE_ADDB = 7;
    public static final int FLAG_TYPE_ADDW = 8;
    public static final int FLAG_TYPE_ADDD = 9;
    public static final int FLAG_TYPE_ADCB = 10;
    public static final int FLAG_TYPE_ADCW = 11;
    public static final int FLAG_TYPE_ADCD = 12;
    public static final int FLAG_TYPE_ORB = 13;
    public static final int FLAG_TYPE_ORW = 14;
    public static final int FLAG_TYPE_ORD = 15;
    public static final int FLAG_TYPE_ANDB = 16;
    public static final int FLAG_TYPE_ANDW = 17;
    public static final int FLAG_TYPE_ANDD = 18;
    public static final int FLAG_TYPE_TESTB = 19;
    public static final int FLAG_TYPE_TESTW = 20;
    public static final int FLAG_TYPE_TESTD = 21;
    public static final int FLAG_TYPE_XORB = 22;
    public static final int FLAG_TYPE_XORW = 23;
    public static final int FLAG_TYPE_XORD = 24;
    public static final int FLAG_TYPE_SUBB = 25;
    public static final int FLAG_TYPE_SUBW = 26;
    public static final int FLAG_TYPE_SUBD = 27;
    public static final int FLAG_TYPE_CMPB = 28;
    public static final int FLAG_TYPE_CMPW = 29;
    public static final int FLAG_TYPE_CMPD = 30;
    public static final int FLAG_TYPE_SBBB = 31;
    public static final int FLAG_TYPE_SBBW = 32;
    public static final int FLAG_TYPE_SBBD = 33;
    public static final int FLAG_TYPE_SHLB = 34;
    public static final int FLAG_TYPE_SHLW = 35;
    public static final int FLAG_TYPE_SHLD = 36;
    public static final int FLAG_TYPE_SHRB = 37;
    public static final int FLAG_TYPE_SHRW = 38;
    public static final int FLAG_TYPE_SHRD = 39;
    public static final int FLAG_TYPE_SARB = 40;
    public static final int FLAG_TYPE_SARW = 41;
    public static final int FLAG_TYPE_SARD = 42;
    public static final int FLAG_TYPE_DSHLW = 43;
    public static final int FLAG_TYPE_DSHLD = 44;
    public static final int FLAG_TYPE_DSHRW = 45;
    public static final int FLAG_TYPE_DSHRD = 46;
    public static final int FLAG_TYPE_NEGB = 47;
    public static final int FLAG_TYPE_NEGW = 48;
    public static final int FLAG_TYPE_NEGD = 49;
}
