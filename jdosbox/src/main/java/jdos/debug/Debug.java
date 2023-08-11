package jdos.debug;

import jdos.cpu.CPU;
import jdos.cpu.CPU_Regs;
import jdos.misc.setup.Section;

import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.FileOutputStream;

public class Debug {
    public static final int INSTRUCTION = 1;
    public static final int FETCHB = 2;
    public static final int FETCHW = 3;
    public static final int FETCHD = 4;
    public static final int EAX = 5;
    public static final int EBX = 6;
    public static final int ECX = 7;
    public static final int EDX = 8;
    public static final int EDI = 9;
    public static final int EIP = 10;
    public static final int ESP = 11;
    public static final int EBP = 12;
    public static final int ESI = 13;
    public static final int FLAGS = 14;

    public static final int FPU_REG0 = 15;
    public static final int FPU_REG1 = 16;
    public static final int FPU_REG2 = 17;
    public static final int FPU_REG3 = 18;
    public static final int FPU_REG4 = 19;
    public static final int FPU_REG5 = 20;
    public static final int FPU_REG6 = 21;
    public static final int FPU_REG7 = 22;
    public static final int FPU_REG8 = 23;
    public static final int FPU_CW = 24;
    public static final int FPU_CW_MASK = 25;
    public static final int FPU_SW = 26;
    public static final int FPU_TOP = 27;
    public static final int FPU_ROUND = 28;
    public static final int ES = 29;
    public static final int CS = 30;
    public static final int SS = 31;
    public static final int DS = 32;
    public static final int FS = 33;
    public static final int GS = 34;

    public static final int READB = 35;
    public static final int READW = 36;
    public static final int READD = 37;
    public static final int WRITEB = 38;
    public static final int WRITEW = 39;
    public static final int WRITED = 40;
    public static final int PROG = 41;
    public static final int CALLBACK = 42;
    public static final int CSEIP1 = 43;
    public static final int CSEIP2 = 44;
    public static final int CSEIP3 = 45;
    public static final int CSEIP4 = 46;
    public static final int DONE = 47;
    public static final int INSTRUCTION_DONE = 48;

    static long[] last = new long[50];

    static DataOutputStream log = null;
    static public boolean logging = true;

    static {
        try {log = new DataOutputStream(new BufferedOutputStream(new FileOutputStream("debug.log")));} catch (Exception e){}
    }

    static public final int TYPE_CPU = 0x01;
    static public final int TYPE_INT10 = 0x02;

    static private int lastType;

    static public final int MASK = TYPE_CPU;
    static public void start(int type, int c) {
        lastType = type;
        if ((type & MASK)!=0) {
            Debug.log(Debug.INSTRUCTION, c);
            Debug.log(Debug.EAX, CPU_Regs.reg_eax.dword);
            Debug.log(Debug.EBX, CPU_Regs.reg_ebx.dword);
            Debug.log(Debug.ECX, CPU_Regs.reg_ecx.dword);
            Debug.log(Debug.EDX, CPU_Regs.reg_edx.dword);
            Debug.log(Debug.EDI, CPU_Regs.reg_edi.dword);
            Debug.log(Debug.EIP, CPU_Regs.reg_eip);
            Debug.log(Debug.ESP, CPU_Regs.reg_esp.dword);
            Debug.log(Debug.EBP, CPU_Regs.reg_ebp.dword);
            Debug.log(Debug.ESI, CPU_Regs.reg_esi.dword);
            Debug.log(Debug.FLAGS, CPU_Regs.flags);
            Debug.log(Debug.ES, CPU_Regs.reg_esPhys.dword);
            Debug.log(Debug.CS, CPU_Regs.reg_csPhys.dword);
            Debug.log(Debug.SS, CPU_Regs.reg_ssPhys.dword);
            Debug.log(Debug.DS, CPU_Regs.reg_dsPhys.dword);
            Debug.log(Debug.FS, CPU_Regs.reg_fsPhys.dword);
            Debug.log(Debug.GS, CPU_Regs.reg_gsPhys.dword);
            //FPU.log();
        }
    }
    static public void stop(int type, int c) {
        lastType = type;
        if ((type & MASK)!=0) {
            Debug.log(Debug.INSTRUCTION_DONE, c);
            Debug.log(Debug.EAX, CPU_Regs.reg_eax.dword);
            Debug.log(Debug.EBX, CPU_Regs.reg_ebx.dword);
            Debug.log(Debug.ECX, CPU_Regs.reg_ecx.dword);
            Debug.log(Debug.EDX, CPU_Regs.reg_edx.dword);
            Debug.log(Debug.EDI, CPU_Regs.reg_edi.dword);
            Debug.log(Debug.EIP, CPU_Regs.reg_eip);
            Debug.log(Debug.ESP, CPU_Regs.reg_esp.dword);
            Debug.log(Debug.EBP, CPU_Regs.reg_ebp.dword);
            Debug.log(Debug.ESI, CPU_Regs.reg_esi.dword);
            Debug.log(Debug.FLAGS, CPU_Regs.flags);
            Debug.log(Debug.ES, CPU_Regs.reg_esPhys.dword);
            Debug.log(Debug.CS, CPU_Regs.reg_csPhys.dword);
            Debug.log(Debug.SS, CPU_Regs.reg_ssPhys.dword);
            Debug.log(Debug.DS, CPU_Regs.reg_dsPhys.dword);
            Debug.log(Debug.FS, CPU_Regs.reg_fsPhys.dword);
            Debug.log(Debug.GS, CPU_Regs.reg_gsPhys.dword);
            //FPU.log();
            Debug.log(Debug.DONE, c);
        }
    }
    static public void close() {
        if (log != null) {
            try {log.close();} catch (Exception e) {}
        }
    }
    static public void log(int type, String value) {
        if (logging) {
            try {
                byte[] b = value.getBytes();
                log.writeByte(type);
                log.writeInt(b.length);
                log.write(b);
            } catch (Exception e) {

            }
        }
    }

    static public void log(int type, long value) {
        if (logging && (lastType & MASK)!=0) {
            try {
                if (type>34 || last[type]!=value) {
                    log.writeByte(type);
                    log.writeInt((int)value);
                    last[type]=value;
                }
            } catch (Exception e) {

            }
        }
    }

    static public void log_long(int type, long value) {
        if (logging && (lastType & MASK)!=0) {
            try {
                if (type>34 || last[type]!=value) {
                    log.writeByte(type);
                    log.writeLong(value);
                    last[type]=value;
                }
            } catch (Exception e) {

            }
        }
    }

    static public void log(int type, long value, long value1) {
        if (logging && (lastType & MASK)!=0) {
            try {
                log.writeByte(type);
                log.writeInt((int)value);
                log.writeInt((int)value1);
            } catch (Exception e) {

            }
        }
    }

    static public /*Bitu*/int cycle_count;
    static public /*Bitu*/int debugCallback;
    static public void DEBUG_HeavyWriteLogInstruction() {

    }
    static public boolean DEBUG_IntBreakpoint(/*Bit8u*/int intNum) {
        return false;
    }
    static public boolean DEBUG_HeavyIsBreakpoint() {
        return false;
    }
    static public boolean DEBUG_Breakpoint() {
        return false;
    }

    public static Section.SectionFunction DEBUG_Init = new Section.SectionFunction() {
        public void call(Section section) {
        }
    };

    public static boolean DEBUG_ExitLoop() {
        return false;
    }
}
