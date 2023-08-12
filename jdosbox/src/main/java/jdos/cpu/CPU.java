package jdos.cpu;

import jdos.Dosbox;
import jdos.debug.Debug;
import jdos.gui.Main;
import jdos.gui.Mapper;
import jdos.hardware.Memory;
import jdos.misc.Log;
import jdos.misc.setup.*;
import jdos.sdl.JavaMapper;
import jdos.types.LogSeverities;
import jdos.types.LogTypes;
import jdos.util.IntRef;

import java.util.Hashtable;

public class CPU extends Module_base {
    public static final int CPU_AUTODETERMINE_NONE=0x00;
    public static final int CPU_AUTODETERMINE_CORE=0x01;
    public static final int CPU_AUTODETERMINE_CYCLES=0x02;

    public static final int CPU_AUTODETERMINE_SHIFT=0x02;
    public static final int CPU_AUTODETERMINE_MASK=0x03;

    public static final int CPU_CYCLES_LOWER_LIMIT=100;

    public static final int CPU_ARCHTYPE_MIXED=0xff;
    public static final int CPU_ARCHTYPE_386=0x35;
    public static final int CPU_ARCHTYPE_486OLD=0x40;
    public static final int CPU_ARCHTYPE_486NEW=0x45;
    public static final int CPU_ARCHTYPE_PENTIUM=0x50;
    public static final int CPU_ARCHTYPE_PENTIUM_PRO=0x55;

    public static interface CPU_Decoder {
        public /*Bits*/int call();
    }

    public static final int CPU_INT_SOFTWARE=0x1;
    public static final int CPU_INT_EXCEPTION=0x2;
    public static final int CPU_INT_HAS_ERROR=0x4;
    public static final int CPU_INT_NOIOPLCHECK=0x8;

    public static void CPU_HW_Interrupt(/*Bitu*/int num) {
        CPU_Interrupt(num,0,CPU_Regs.reg_eip);
    }
    public static void CPU_SW_Interrupt(/*Bitu*/int num,/*Bitu*/int oldeip) {
        CPU_Interrupt(num,CPU_INT_SOFTWARE,oldeip);
    }
    public static void CPU_SW_Interrupt_NoIOPLCheck(/*Bitu*/int num,/*Bitu*/int oldeip) {
        CPU_Interrupt(num,CPU_INT_SOFTWARE|CPU_INT_NOIOPLCHECK,oldeip);
    }

    public static final int EXCEPTION_UD=6;
    public static final int EXCEPTION_NM=7;
    public static final int EXCEPTION_TS=10;
    public static final int EXCEPTION_NP=11;
    public static final int EXCEPTION_SS=12;
    public static final int EXCEPTION_GP=13;
    public static final int EXCEPTION_PF=14;
    public static final int EXCEPTION_MF=16;

    public static final int CR0_PROTECTION=0x00000001;
    public static final int CR0_MONITORPROCESSOR=0x00000002;
    public static final int CR0_FPUEMULATION=0x00000004;
    public static final int CR0_TASKSWITCH=0x00000008;
    public static final int CR0_FPUPRESENT=0x00000010;
    public static final int CR0_NUMERICERROR=0x00000020;
    public static final int CR0_WRITEPROTECT=0x00010000;
    public static final int CR0_PAGING=0x80000000;

    public static final int CR4_VIRTUAL8086_MODE_EXTENSIONS = 0x1;
    public static final int CR4_PROTECTED_MODE_VIRTUAL_INTERRUPTS = 0x2;
    public static final int CR4_TIME_STAMP_DISABLE = 0x4;
    public static final int CR4_DEBUGGING_EXTENSIONS = 0x8;
    public static final int CR4_PAGE_SIZE_EXTENSIONS = 0x10;
    public static final int CR4_PHYSICAL_ADDRESS_EXTENSION = 0x20;
    public static final int CR4_MACHINE_CHECK_ENABLE = 0x40;
    public static final int CR4_PAGE_GLOBAL_ENABLE = 0x80;
    public static final int CR4_PERFORMANCE_MONITORING_COUNTER_ENABLE = 0x100;
    public static final int CR4_OS_SUPPORT_FXSAVE_FXSTORE = 0x200;
    public static final int CR4_OS_SUPPORT_UNMASKED_SIMD_EXCEPTIONS = 0x400;

    // *********************************************************************
    // Descriptor
    // *********************************************************************

    public static final int DESC_INVALID				=0x00;
    public static final int DESC_286_TSS_A				=0x01;
    public static final int DESC_LDT					=0x02;
    public static final int DESC_286_TSS_B				=0x03;
    public static final int DESC_286_CALL_GATE			=0x04;
    public static final int DESC_TASK_GATE				=0x05;
    public static final int DESC_286_INT_GATE			=0x06;
    public static final int DESC_286_TRAP_GATE			=0x07;

    public static final int DESC_386_TSS_A				=0x09;
    public static final int DESC_386_TSS_B				=0x0b;
    public static final int DESC_386_CALL_GATE			=0x0c;
    public static final int DESC_386_INT_GATE			=0x0e;
    public static final int DESC_386_TRAP_GATE			=0x0f;

    /* EU/ED Expand UP/DOWN RO/RW Read Only/Read Write NA/A Accessed */
    public static final int DESC_DATA_EU_RO_NA			=0x10;
    public static final int DESC_DATA_EU_RO_A			=0x11;
    public static final int DESC_DATA_EU_RW_NA			=0x12;
    public static final int DESC_DATA_EU_RW_A			=0x13;
    public static final int DESC_DATA_ED_RO_NA			=0x14;
    public static final int DESC_DATA_ED_RO_A			=0x15;
    public static final int DESC_DATA_ED_RW_NA			=0x16;
    public static final int DESC_DATA_ED_RW_A			=0x17;

    /* N/R Readable  NC/C Confirming A/NA Accessed */
    public static final int DESC_CODE_N_NC_A			=0x18;
    public static final int DESC_CODE_N_NC_NA			=0x19;
    public static final int DESC_CODE_R_NC_A			=0x1a;
    public static final int DESC_CODE_R_NC_NA			=0x1b;
    public static final int DESC_CODE_N_C_A				=0x1c;
    public static final int DESC_CODE_N_C_NA			=0x1d;
    public static final int DESC_CODE_R_C_A				=0x1e;
    public static final int DESC_CODE_R_C_NA			=0x1f;

    /*
    struct S_Descriptor {
    #ifdef WORDS_BIGENDIAN
        Bit32u base_0_15	:16;
        Bit32u limit_0_15	:16;
        Bit32u base_24_31	:8;
        Bit32u g			:1;
        Bit32u big			:1;
        Bit32u r			:1;
        Bit32u avl			:1;
        Bit32u limit_16_19	:4;
        Bit32u p			:1;
        Bit32u dpl			:2;
        Bit32u type			:5;
        Bit32u base_16_23	:8;
    #else
        Bit32u limit_0_15	:16;
        Bit32u base_0_15	:16;
        Bit32u base_16_23	:8;
        Bit32u type			:5;
        Bit32u dpl			:2;
        Bit32u p			:1;
        Bit32u limit_16_19	:4;
        Bit32u avl			:1;
        Bit32u r			:1;
        Bit32u big			:1;
        Bit32u g			:1;
        Bit32u base_24_31	:8;
    #endif
    }GCC_ATTRIBUTE(packed);
    */
    static private class S_Descriptor {
        static public final int TYPE_OFFSET = 40;

        public long fill;
        public int limit_0_15() {
            return (int)(fill & 0xFFFF);
        }
        public int base_0_15() {
            return (int)((fill >> 16) & 0xFFFF);
        }
        public int base_16_23() {
            return (int)((fill >> 32) & 0xFF);
        }
        public int type() {
            return (int)((fill >> 40) & 0x1F);
        }
        public int dpl() {
            return (int)((fill >> 45) & 0x3);
        }
        public int p() {
            return (int)((fill >> 47) & 0x1);
        }
        public int limit_16_19() {
            return (int)((fill >> 48) & 0xF);
        }
        public int avl() {
            return (int)((fill >> 52) & 0x1);
        }
        public int r() {
            return (int)((fill >> 53) & 0x1);
        }
        public int big() {
            return (int)((fill >> 54) & 0x1);
        }
        public int g() {
            return (int)((fill >> 55) & 0x1);
        }
        public int base_24_31() {
            return (int)((fill >>> 56) & 0xFF);
        }
    }
    /*
    struct G_Descriptor {
    #ifdef WORDS_BIGENDIAN
        Bit32u selector:	16;
        Bit32u offset_0_15	:16;
        Bit32u offset_16_31	:16;
        Bit32u p			:1;
        Bit32u dpl			:2;
        Bit32u type			:5;
        Bit32u reserved		:3;
        Bit32u paramcount	:5;
    #else
        Bit32u offset_0_15	:16;
        Bit32u selector		:16;
        Bit32u paramcount	:5;
        Bit32u reserved		:3;
        Bit32u type			:5;
        Bit32u dpl			:2;
        Bit32u p			:1;
        Bit32u offset_16_31	:16;
    #endif
    } GCC_ATTRIBUTE(packed);
    */

    static private class G_Descriptor {
        public long fill;
        public int offset_0_15() {
            return (int)(fill & 0xFFFF);
        }
        public int selector() {
            return (int)((fill >> 16) & 0xFFFF);
        }
        public int paramcount() {
            return (int)((fill >> 32) & 0x1F);
        }
        public int reserved() {
            return (int)((fill >> 37) & 0x7);
        }
        public int type() {
            return (int)((fill >> 40) & 0x1F);
        }
        public int dpl() {
            return (int)((fill >> 45) & 0x3);
        }
        public int p() {
            return (int)((fill >> 47) & 0x1);
        }
        public int offset_16_31() {
            return (int)((fill >> 48) & 0xFFFF);
        }
    }

//    private class TSS_16 {
//        /*Bit16u*/short back;                 /* Back link to other task */
//        /*Bit16u*/short sp0;				     /* The CK stack pointer */
//        /*Bit16u*/short ss0;					 /* The CK stack selector */
//        /*Bit16u*/short sp1;                  /* The parent KL stack pointer */
//        /*Bit16u*/short ss1;                  /* The parent KL stack selector */
//        /*Bit16u*/short sp2;                  /* Unused */
//        /*Bit16u*/short ss2;                  /* Unused */
//        /*Bit16u*/short ip;                   /* The instruction pointer */
//        /*Bit16u*/short flags;                /* The flags */
//        /*Bit16u*/short ax, cx, dx, bx;       /* The general purpose registers */
//        /*Bit16u*/short sp, bp, si, di;       /* The special purpose registers */
//        /*Bit16u*/short es;                   /* The extra selector */
//        /*Bit16u*/short cs;                   /* The code selector */
//        /*Bit16u*/short ss;                   /* The application stack selector */
//        /*Bit16u*/short ds;                   /* The data selector */
//        /*Bit16u*/short ldt;                  /* The local descriptor table */
//    }
    static private final int TSS_16_back_offset = 0;
    static private final int TSS_16_sp0_offset = 2;

//    private class TSS_32 {
//        /*Bit32u*/int back;                /* Back link to other task */
//        /*Bit32u*/int esp0;		         /* The CK stack pointer */
//        /*Bit32u*/int ss0;					 /* The CK stack selector */
//        /*Bit32u*/int esp1;                 /* The parent KL stack pointer */
//        /*Bit32u*/int ss1;                  /* The parent KL stack selector */
//        /*Bit32u*/int esp2;                 /* Unused */
//        /*Bit32u*/int ss2;                  /* Unused */
//        /*Bit32u*/int cr3;                  /* The page directory pointer */
//        /*Bit32u*/int eip;                  /* The instruction pointer */
//        /*Bit32u*/int eflags;               /* The flags */
//        /*Bit32u*/int eax, ecx, edx, ebx;   /* The general purpose registers */
//        /*Bit32u*/int esp, ebp, esi, edi;   /* The special purpose registers */
//        /*Bit32u*/int es;                   /* The extra selector */
//        /*Bit32u*/int cs;                   /* The code selector */
//        /*Bit32u*/int ss;                   /* The application stack selector */
//        /*Bit32u*/int ds;                   /* The data selector */
//        /*Bit32u*/int fs;                   /* And another extra selector */
//        /*Bit32u*/int gs;                   /* ... and another one */
//        /*Bit32u*/int ldt;                  /* The local descriptor table */
//    }
    static private final int TSS_32_back_offset = 0;
    static private final int TSS_32_esp0_offset = 4;
    //static private final int TSS_32_ss0_offset = 8;
    //static private final int TSS_32_esp1_offset = 12;
    //static private final int TSS_32_ss1_offset = 16;
    //static private final int TSS_32_esp2_offset = 20;
    //static private final int TSS_32_ss2_offset = 24;
    static private final int TSS_32_cr3_offset = 28;
    static private final int TSS_32_eip_offset = 32;
    static private final int TSS_32_eflags_offset = 36;
    static private final int TSS_32_eax_offset = 40;
    static private final int TSS_32_ecx_offset = 44;
    static private final int TSS_32_edx_offset = 48;
    static private final int TSS_32_ebx_offset = 52;
    static private final int TSS_32_esp_offset = 56;
    static private final int TSS_32_ebp_offset = 60;
    static private final int TSS_32_esi_offset = 64;
    static private final int TSS_32_edi_offset = 68;
    static private final int TSS_32_es_offset = 72;
    static private final int TSS_32_cs_offset = 76;
    static private final int TSS_32_ss_offset = 80;
    static private final int TSS_32_ds_offset = 84;
    static private final int TSS_32_fs_offset = 88;
    static private final int TSS_32_gs_offset = 92;
    static private final int TSS_32_ldt_offset = 96;

    static public class Descriptor {
        final public class Descriptor_union {
            S_Descriptor seg = new S_Descriptor();
            G_Descriptor gate = new G_Descriptor();
            final public void setType(int type) {
                seg.fill &= ~(0x1Fl << 40);
                seg.fill |= ((long)type << 40);
                gate.fill = seg.fill;
            }
            final public int getType() {
                return (int)((seg.fill >> 40) & 0x1F);
            }
            final public void fill(long l) {
                seg.fill = l;
                gate.fill = l;
            }
            final public int get_fill(int index) {
                if (index == 0) {
                    return (int)seg.fill;
                } else {
                    return (int)(seg.fill >>> 32);
                }
            }
        }
        final public void Load(/*PhysPt*/int address) {
            cpu.mpl=0;
            saved.fill((Memory.mem_readd(address) & 0xFFFFFFFFl) | (long)Memory.mem_readd(address + 4) << 32);
            cpu.mpl=3;
        }

        final public void Save(/*PhysPt*/int address) {
            cpu.mpl=0;
	        Memory.mem_writed(address,(int)saved.gate.fill);
	        Memory.mem_writed(address+4,(int)(saved.gate.fill >>> 32));
	        cpu.mpl=3;
        }

        final public /*PhysPt*/int GetBase() {
            return (saved.seg.base_24_31()<<24) | (saved.seg.base_16_23()<<16) | saved.seg.base_0_15();
        }
        final public /*Bitu*/long GetLimit () {
            /*Bitu*/long limit = (saved.seg.limit_16_19()<<16) | saved.seg.limit_0_15();
            if (saved.seg.g() != 0)	{
                limit = (limit<<12) | 0xFFF;            }
            return limit;
        }
        final public /*Bitu*/long GetOffset() {
            return ((long)saved.gate.offset_16_31() << 16) | saved.gate.offset_0_15();
        }
        final public /*Bitu*/int GetSelector() {
            return saved.gate.selector();
        }
        final public /*Bitu*/int Type() {
            return saved.seg.type();
        }
        final public /*Bitu*/int Conforming() {
            return saved.seg.type() & 8;
        }
        final public /*Bitu*/int DPL() {
            return saved.seg.dpl();
        }
        final public /*Bitu*/int Big() {
            return saved.seg.big();
        }
        final public Descriptor_union saved = new Descriptor_union();
    }

    static class DescriptorTable {
        final public /*PhysPt*/int GetBase() {
            return table_base;
        }
        final public /*Bitu*/int GetLimit() {
            return table_limit;
        }
        final public void SetBase(/*PhysPt*/int _base) {
            table_base = _base;
        }
        final public void SetLimit(/*Bitu*/int _limit) {
            table_limit = _limit;
        }

        public boolean GetDescriptor(/*Bitu*/int selector, Descriptor desc) {
            selector&=~7;
		    if (selector>=table_limit) return false;
            desc.Load(table_base+(selector));
		    return true;
        }
        protected /*PhysPt*/int table_base;
        protected /*Bitu*/int table_limit;
    }

    static public class GDTDescriptorTable extends DescriptorTable {
        public boolean GetDescriptor(/*Bitu*/int selector, Descriptor desc) {
            /*Bitu*/int address=selector & ~7;
            if ((selector & 4) != 0) {
                if (address>=ldt_limit) return false;
                desc.Load(ldt_base+address);
                return true;
            } else {
                if (address>=table_limit) return false;
                desc.Load(table_base+address);
                return true;
            }
        }
        public boolean SetDescriptor(/*Bitu*/int selector, Descriptor desc) {
            /*Bitu*/int address=selector & ~7;
            if ((selector & 4) != 0) {
                if (address>=ldt_limit) return false;
                desc.Save(ldt_base+address);
                return true;
            } else {
                if (address>=table_limit) return false;
                desc.Save(table_base+address);
                return true;
            }
        }
        public /*Bitu*/int SLDT()	{
            return ldt_value;
        }
        private final Descriptor desc_temp = new Descriptor();
        public boolean LLDT(/*Bitu*/int value)	{
            if ((value & 0xfffc)==0) {
                ldt_value=0;
                ldt_base=0;
                ldt_limit=0;
                return true;
            }

            if (!GetDescriptor(value,desc_temp)) return !CPU_PrepareException(EXCEPTION_GP,value);
            if (desc_temp.Type()!=DESC_LDT) return !CPU_PrepareException(EXCEPTION_GP,value);
            if (desc_temp.saved.seg.p() == 0) return !CPU_PrepareException(EXCEPTION_NP,value);
            ldt_base=desc_temp.GetBase();
            ldt_limit=desc_temp.GetLimit();
            ldt_value=value;
            return true;
        }
        private /*PhysPt*/int ldt_base;
        private /*Bitu*/long ldt_limit;
        private /*Bitu*/int ldt_value;
    }

    final private static class TSS_Descriptor extends Descriptor {
        final public /*Bitu*/int IsBusy() {
            return saved.seg.type() & 2;
        }
        final public /*Bitu*/int Is386() {
            return saved.seg.type() & 8;
        }
        final void SetBusy(boolean busy) {
            if (busy) {
                saved.setType(saved.getType()|2);
            } else {
                saved.setType(saved.getType()&~2);
            }
        }
    }

    final static public class CPUBlock {
        public /*Bitu*/int cpl;							/* Current Privilege */
        public /*Bitu*/int mpl;
        public /*Bitu*/int cr0;
        public /*Bitu*/int cr4;
        public boolean pmode;							/* Is Protected mode enabled */
        final public GDTDescriptorTable gdt = new GDTDescriptorTable();
        final public DescriptorTable idt = new DescriptorTable();
        final public class Stack {
            public /*Bitu*/int mask, notmask;
            public boolean big;
        }
        final public Stack stack = new Stack();
        final public class Code {
            public boolean big;
        }
        final public Code code = new Code();
        final public class Hlt {
            public /*Bitu*/int cs;
            public int eip;
            public CPU_Decoder old_decoder;
        }
        final public Hlt hlt = new Hlt();
        final public class Exception {
            public /*Bitu*/int which, error;
        }
        final public Exception exception = new Exception();
        public /*Bits*/int direction;
        public boolean trap_skip;
        final public /*Bit32u*/int[] drx=new int[8];
        final public /*Bit32u*/int[] trx=new int[8];
    }

    public static void CPU_SetFlagsd(/*Bitu*/int word) {
        /*Bitu*/int mask=cpu.cpl != 0 ? CPU_Regs.FMASK_NORMAL : CPU_Regs.FMASK_ALL;
        CPU_SetFlags(word,mask);
    }

    public static void CPU_SetFlagsw(/*Bitu*/int word) {
        /*Bitu*/int mask=(cpu.cpl != 0 ? CPU_Regs.FMASK_NORMAL : CPU_Regs.FMASK_ALL) & 0xffff;
        CPU_SetFlags(word,mask);
    }

    public static CPU_Regs regs = new CPU_Regs();
    public static CPUBlock cpu;

    static public int seg_value(int index) {
        switch (index) {
            case CPU_Regs.es:
                return CPU_Regs.reg_esVal.dword;
            case CPU_Regs.cs:
                return CPU_Regs.reg_csVal.dword;
            case CPU_Regs.ss:
                return CPU_Regs.reg_ssVal.dword;
            case CPU_Regs.ds:
                return CPU_Regs.reg_dsVal.dword;
            case CPU_Regs.fs:
                return CPU_Regs.reg_fsVal.dword;
            case CPU_Regs.gs:
                return CPU_Regs.reg_gsVal.dword;
            default:
                Log.exit("Unknown segment");
                return 0;
        }
    }

    public static /*Bit32s*/int CPU_Cycles = 0;
    public static /*Bit32s*/int CPU_CycleLeft = 3000;
    public static /*Bit32s*/int CPU_CycleMax = 3000;
    public static /*Bit32s*/int CPU_OldCycleMax = 3000;
    public static /*Bit32s*/int CPU_CyclePercUsed = 100;
    public static /*Bit32s*/int CPU_CycleLimit = -1;
    public static /*Bit32s*/int CPU_CycleUp = 0;
    public static /*Bit32s*/int CPU_CycleDown = 0;
    public static /*Bit32s*/int CPU_IODelayRemoved = 0;
    public static CPU_Decoder cpudecoder;
    public static boolean CPU_CycleAutoAdjust = false;
    public static boolean CPU_SkipCycleAutoAdjust = false;
    public static /*Bitu*/int CPU_AutoDetermineMode = 0;

    public static /*Bitu*/int CPU_ArchitectureType = CPU_ARCHTYPE_MIXED;

    public static /*Bitu*/int CPU_extflags_toggle=0;	// ID and AC flags may be toggled depending on emulated CPU architecture

    public static /*Bitu*/int CPU_PrefetchQueueSize=0;

    /* In debug mode exceptions are tested and dosbox exits when
     * a unhandled exception state is detected.
     * USE CHECK_EXCEPT to raise an exception in that case to see if that exception
     * solves the problem.
     *
     * In non-debug mode dosbox doesn't do detection (and hence doesn't crash at
     * that point). (game might crash later due to the unhandled exception) */
    
//    private static final boolean CPU_CHECK_IGNORE = Config.C_DEBUG;

    public static void CPU_Push16(/*Bitu*/int value) {
        /*Bit32u*/int new_esp=(CPU_Regs.reg_esp.dword & cpu.stack.notmask) | ((CPU_Regs.reg_esp.dword - 2) & cpu.stack.mask);
        Memory.mem_writew(CPU_Regs.reg_ssPhys.dword + (new_esp & cpu.stack.mask) ,value);
        CPU_Regs.reg_esp.dword=new_esp;
    }

    public static int CPU_Push16(int esp, /*Bitu*/int value) {
        /*Bit32u*/int new_esp=(esp & cpu.stack.notmask) | ((esp - 2) & cpu.stack.mask);
        Memory.mem_writew(CPU_Regs.reg_ssPhys.dword + (new_esp & cpu.stack.mask) ,value);
        return new_esp;
    }

    public static void CPU_Push32(/*Bitu*/int value) {
        /*Bit32u*/int new_esp=(CPU_Regs.reg_esp.dword & cpu.stack.notmask)|((CPU_Regs.reg_esp.dword - 4) & cpu.stack.mask);
        Memory.mem_writed(CPU_Regs.reg_ssPhys.dword + (new_esp & cpu.stack.mask) ,value);
        CPU_Regs.reg_esp.dword=new_esp;
    }

    public static int CPU_Push32(int esp, /*Bitu*/int value) {
        /*Bit32u*/int new_esp=(esp & cpu.stack.notmask)|((esp - 4) & cpu.stack.mask);
        Memory.mem_writed(CPU_Regs.reg_ssPhys.dword + (new_esp & cpu.stack.mask) ,value);
        return new_esp;
    }

    public static /*Bitu*/int CPU_Pop16() {
        /*Bitu*/int val=Memory.mem_readw(CPU_Regs.reg_ssPhys.dword + (CPU_Regs.reg_esp.dword & cpu.stack.mask));
        CPU_Regs.reg_esp.dword=(CPU_Regs.reg_esp.dword & cpu.stack.notmask)|((CPU_Regs.reg_esp.dword + 2 )& cpu.stack.mask);
        return val;
    }

    public static /*Bitu*/int CPU_Peek16(int index) {
        return Memory.mem_readw(CPU_Regs.reg_ssPhys.dword + (CPU_Regs.reg_esp.dword+index*2 & cpu.stack.mask));
    }

    public static int CPU_Peek32(int index) {
        return Memory.mem_readd(CPU_Regs.reg_ssPhys.dword + (CPU_Regs.reg_esp.dword+index*4 & cpu.stack.mask));
    }

    public static /*Bitu*/int CPU_Pop32() {
        /*Bitu*/int val=Memory.mem_readd(CPU_Regs.reg_ssPhys.dword + (CPU_Regs.reg_esp.dword & cpu.stack.mask));
        CPU_Regs.reg_esp.dword=(CPU_Regs.reg_esp.dword & cpu.stack.notmask)|((CPU_Regs.reg_esp.dword + 4)&cpu.stack.mask);
        return val;
    }

//    private static /*PhysPt*/int SelBase(/*Bitu*/int sel) {
//        if ((cpu.cr0 & CR0_PROTECTION) != 0) {
//            Descriptor desc = new Descriptor();
//            cpu.gdt.GetDescriptor(sel,desc);
//            return desc.GetBase();
//        } else {
//            return (sel)<<4;
//        }
//    }

    public static void CPU_SetFlags(/*Bitu*/int word,/*Bitu*/int mask) {
        mask|=CPU_extflags_toggle;	// ID-flag and AC-flag can be toggled on CPUID-supporting CPUs
        CPU_Regs.flags=(CPU_Regs.flags & ~mask)|(word & mask)|2;
        cpu.direction=1-((CPU_Regs.flags & CPU_Regs.DF) >> 9);
    }

    static public boolean CPU_PrepareException(/*Bitu*/int which,/*Bitu*/int error) {
        cpu.exception.which=which;
        cpu.exception.error=error;
        return true;
    }

    static public boolean CPU_CLI() {
        if (cpu.pmode && ((CPU_Regs.GETFLAG(CPU_Regs.VM)==0 && (CPU_Regs.GETFLAG_IOPL()<cpu.cpl)) || (CPU_Regs.GETFLAG(CPU_Regs.VM)!=0 && (CPU_Regs.GETFLAG_IOPL()<3)))) {
            return CPU_PrepareException(EXCEPTION_GP,0);
        } else {
            CPU_Regs.SETFLAGBIT(CPU_Regs.IF,false);
            return false;
        }
    }

    static public boolean CPU_STI() {
        if (cpu.pmode && ((CPU_Regs.GETFLAG(CPU_Regs.VM)==0 && (CPU_Regs.GETFLAG_IOPL()<cpu.cpl)) || (CPU_Regs.GETFLAG(CPU_Regs.VM)!=0 && (CPU_Regs.GETFLAG_IOPL()<3)))) {
            return CPU_PrepareException(EXCEPTION_GP,0);
        } else {
             CPU_Regs.SETFLAGBIT(CPU_Regs.IF,true);
            return false;
        }
    }

    static public boolean CPU_POPF(/*Bitu*/boolean use32) {
        if (cpu.pmode && CPU_Regs.GETFLAG(CPU_Regs.VM)!=0 && (CPU_Regs.GETFLAG(CPU_Regs.IOPL)!=CPU_Regs.IOPL)) {
            /* Not enough privileges to execute POPF */
            return CPU_PrepareException(EXCEPTION_GP,0);
        }
        /*Bitu*/int mask=CPU_Regs.FMASK_ALL;
        /* IOPL field can only be modified when CPL=0 or in real mode: */
        if (cpu.pmode && (cpu.cpl>0)) mask &= (~CPU_Regs.IOPL);
        if (cpu.pmode && CPU_Regs.GETFLAG(CPU_Regs.VM)==0 && (CPU_Regs.GETFLAG_IOPL()<cpu.cpl)) mask &= (~CPU_Regs.IF);
        if (use32)
            CPU_SetFlags(CPU_Pop32(),mask);
        else CPU_SetFlags(CPU_Pop16(),mask & 0xffff);
        Flags.DestroyConditionFlags();
        return false;
    }

    static public boolean CPU_PUSHF(/*Bitu*/boolean use32) {
        if (cpu.pmode && CPU_Regs.GETFLAG(CPU_Regs.VM)!=0 && (CPU_Regs.GETFLAG(CPU_Regs.IOPL)!=CPU_Regs.IOPL)) {
            /* Not enough privileges to execute PUSHF */
            return CPU_PrepareException(EXCEPTION_GP,0);
        }
        Flags.FillFlags();
        if (use32)
            CPU_Push32(CPU_Regs.flags & 0xfcffff);
        else CPU_Push16(CPU_Regs.flags);
        return false;
    }

    static Descriptor desc_temp_1 = new Descriptor();
    static private void CPU_CheckSegments() {
        boolean needs_invalidation=false;

        if (!cpu.gdt.GetDescriptor(CPU_Regs.reg_esVal.dword,desc_temp_1)) needs_invalidation=true;
        else switch (desc_temp_1.Type()) {
            case DESC_DATA_EU_RO_NA:	case DESC_DATA_EU_RO_A:	case DESC_DATA_EU_RW_NA:	case DESC_DATA_EU_RW_A:
            case DESC_DATA_ED_RO_NA:	case DESC_DATA_ED_RO_A:	case DESC_DATA_ED_RW_NA:	case DESC_DATA_ED_RW_A:
            case DESC_CODE_N_NC_A:	case DESC_CODE_N_NC_NA:	case DESC_CODE_R_NC_A:	case DESC_CODE_R_NC_NA:
                if (cpu.cpl>desc_temp_1.DPL()) needs_invalidation=true; break;
            default: break;	}
        if (needs_invalidation) CPU_SetSegGeneralES(0);

        needs_invalidation=false;
        if (!cpu.gdt.GetDescriptor(CPU_Regs.reg_dsVal.dword,desc_temp_1)) needs_invalidation=true;
        else switch (desc_temp_1.Type()) {
            case DESC_DATA_EU_RO_NA:	case DESC_DATA_EU_RO_A:	case DESC_DATA_EU_RW_NA:	case DESC_DATA_EU_RW_A:
            case DESC_DATA_ED_RO_NA:	case DESC_DATA_ED_RO_A:	case DESC_DATA_ED_RW_NA:	case DESC_DATA_ED_RW_A:
            case DESC_CODE_N_NC_A:	case DESC_CODE_N_NC_NA:	case DESC_CODE_R_NC_A:	case DESC_CODE_R_NC_NA:
                if (cpu.cpl>desc_temp_1.DPL()) needs_invalidation=true; break;
            default: break;	}
        if (needs_invalidation) CPU_SetSegGeneralDS(0);

        needs_invalidation=false;
        if (!cpu.gdt.GetDescriptor(CPU_Regs.reg_fsVal.dword,desc_temp_1)) needs_invalidation=true;
        else switch (desc_temp_1.Type()) {
            case DESC_DATA_EU_RO_NA:	case DESC_DATA_EU_RO_A:	case DESC_DATA_EU_RW_NA:	case DESC_DATA_EU_RW_A:
            case DESC_DATA_ED_RO_NA:	case DESC_DATA_ED_RO_A:	case DESC_DATA_ED_RW_NA:	case DESC_DATA_ED_RW_A:
            case DESC_CODE_N_NC_A:	case DESC_CODE_N_NC_NA:	case DESC_CODE_R_NC_A:	case DESC_CODE_R_NC_NA:
                if (cpu.cpl>desc_temp_1.DPL()) needs_invalidation=true; break;
            default: break;	}
        if (needs_invalidation) CPU_SetSegGeneralFS(0);

        needs_invalidation=false;
        if (!cpu.gdt.GetDescriptor(CPU_Regs.reg_gsVal.dword,desc_temp_1)) needs_invalidation=true;
        else switch (desc_temp_1.Type()) {
            case DESC_DATA_EU_RO_NA:	case DESC_DATA_EU_RO_A:	case DESC_DATA_EU_RW_NA:	case DESC_DATA_EU_RW_A:
            case DESC_DATA_ED_RO_NA:	case DESC_DATA_ED_RO_A:	case DESC_DATA_ED_RW_NA:	case DESC_DATA_ED_RW_A:
            case DESC_CODE_N_NC_A:	case DESC_CODE_N_NC_NA:	case DESC_CODE_R_NC_A:	case DESC_CODE_R_NC_NA:
                if (cpu.cpl>desc_temp_1.DPL()) needs_invalidation=true; break;
            default: break;	}
        if (needs_invalidation) CPU_SetSegGeneralGS(0);
    }


    final static private class TaskStateSegment {
        public TaskStateSegment() {
            valid=false;
        }
        final public boolean IsValid() {
            return valid;
        }
        final /*Bitu*/int Get_back() {
            cpu.mpl=0;
            /*Bit16u*/int backlink=Memory.mem_readw(base);
            cpu.mpl=3;
            return backlink;
        }
        final void SaveSelector() {
            cpu.gdt.SetDescriptor(selector,desc);
        }
        final void Get_SSx_ESPx( /*Bitu*/int level, /*Bitu*/IntRef _ss, /*Bitu*/IntRef _esp) {
            cpu.mpl=0;
            if (is386 != 0) {
                /*PhysPt*/int where=base+TSS_32_esp0_offset+level*8;
                _esp.value=Memory.mem_readd(where);
                _ss.value=Memory.mem_readw(where+4);
            } else {
                 /*PhysPt*/int where=base+TSS_16_sp0_offset+level*4;
                _esp.value=Memory.mem_readw(where);
                _ss.value=Memory.mem_readw(where+2);
            }
            cpu.mpl=3;
        }
        final boolean SetSelector( /*Bitu*/int new_sel) {
            valid=false;
            if ((new_sel & 0xfffc)==0) {
                selector=0;
                base=0;
                limit=0;
                is386=1;
                return true;
            }
            if ((new_sel & 4)!=0) return false;
            if (!cpu.gdt.GetDescriptor(new_sel,desc)) return false;
            switch (desc.Type()) {
                case DESC_286_TSS_A:		case DESC_286_TSS_B:
                case DESC_386_TSS_A:		case DESC_386_TSS_B:
                    break;
                default:
                    return false;
            }
            if (desc.saved.seg.p()==0) return false;
            selector=new_sel;
            valid=true;
            base=desc.GetBase();
            limit=desc.GetLimit();
            is386=desc.Is386();
            return true;
        }
        TSS_Descriptor desc = new TSS_Descriptor();
        /*Bitu*/int selector;
        /*PhysPt*/int base;
        /*Bitu*/long limit;
        /*Bitu*/int is386;
        boolean valid;
    }

    final private static TaskStateSegment cpu_tss = new TaskStateSegment();

    static final class TSwitchType {
        static public final int TSwitch_JMP=0;
        static public final int TSwitch_CALL_INT=1;
        static public final int TSwitch_IRET=2;
    }

    static private final TaskStateSegment new_tss_temp=new TaskStateSegment();
    static private final Descriptor cs_desc_temp = new Descriptor();
    static private boolean CPU_SwitchTask( /*Bitu*/int new_tss_selector,int tstype, /*Bitu*/int old_eip) {
        Flags.FillFlags();

        if (!new_tss_temp.SetSelector(new_tss_selector))
            Log.exit("Illegal TSS for switch, selector="+Integer.toString(new_tss_selector, 16)+", switchtype="+Integer.toString(tstype, 16));
        if (tstype==TSwitchType.TSwitch_IRET) {
            if (new_tss_temp.desc.IsBusy()==0)
                Log.exit("TSS not busy for IRET");
        } else {
            if (new_tss_temp.desc.IsBusy()!=0)
                Log.exit("TSS busy for JMP/CALL/INT");
        }
         /*Bitu*/int new_cr3;
         /*Bitu*/int new_eax,new_ebx,new_ecx,new_edx,new_esp,new_ebp,new_esi,new_edi;
         /*Bitu*/int new_es,new_cs,new_ss,new_ds,new_fs,new_gs;
         /*Bitu*/int new_eip;
        int new_eflags,new_ldt;
        /* Read new context from new TSS */
        if (new_tss_temp.is386!=0) {
            new_cr3=Memory.mem_readd(new_tss_temp.base + TSS_32_cr3_offset);
            new_eip=Memory.mem_readd(new_tss_temp.base + TSS_32_eip_offset);
            new_eflags=Memory.mem_readd(new_tss_temp.base + TSS_32_eflags_offset);
            new_eax=Memory.mem_readd(new_tss_temp.base + TSS_32_eax_offset);
            new_ecx=Memory.mem_readd(new_tss_temp.base + TSS_32_ecx_offset);
            new_edx=Memory.mem_readd(new_tss_temp.base + TSS_32_edx_offset);
            new_ebx=Memory.mem_readd(new_tss_temp.base + TSS_32_ebx_offset);
            new_esp=Memory.mem_readd(new_tss_temp.base + TSS_32_esp_offset);
            new_ebp=Memory.mem_readd(new_tss_temp.base + TSS_32_ebp_offset);
            new_edi=Memory.mem_readd(new_tss_temp.base + TSS_32_edi_offset);
            new_esi=Memory.mem_readd(new_tss_temp.base + TSS_32_esi_offset);

            new_es=Memory.mem_readw(new_tss_temp.base+TSS_32_es_offset);
            new_cs=Memory.mem_readw(new_tss_temp.base+TSS_32_cs_offset);
            new_ss=Memory.mem_readw(new_tss_temp.base+TSS_32_ss_offset);
            new_ds=Memory.mem_readw(new_tss_temp.base+TSS_32_ds_offset);
            new_fs=Memory.mem_readw(new_tss_temp.base+TSS_32_fs_offset);
            new_gs=Memory.mem_readw(new_tss_temp.base+TSS_32_gs_offset);
            new_ldt=Memory.mem_readw(new_tss_temp.base+TSS_32_ldt_offset);
        } else {
            Log.exit("286 task switch");
            new_cr3=0;
            new_eip=0;
            new_eflags=0;
            new_eax=0;	new_ecx=0;	new_edx=0;	new_ebx=0;
            new_esp=0;	new_ebp=0;	new_edi=0;	new_esi=0;

            new_es=0;	new_cs=0;	new_ss=0;	new_ds=0;	new_fs=0;	new_gs=0;
            new_ldt=0;
        }

        /* Check if we need to clear busy bit of old TASK */
        if (tstype==TSwitchType.TSwitch_JMP || tstype==TSwitchType.TSwitch_IRET) {
            cpu_tss.desc.SetBusy(false);
            cpu_tss.SaveSelector();
        }
        /*Bit32u*/int old_flags = CPU_Regs.flags;
        if (tstype==TSwitchType.TSwitch_IRET) old_flags &= (~CPU_Regs.NT);

        /* Save current context in current TSS */
        if (cpu_tss.is386!=0) {
            Memory.mem_writed(cpu_tss.base+TSS_32_eflags_offset,old_flags);
            Memory.mem_writed(cpu_tss.base+TSS_32_eip_offset,old_eip);

            Memory.mem_writed(cpu_tss.base+TSS_32_eax_offset,CPU_Regs.reg_eax.dword);
            Memory.mem_writed(cpu_tss.base+TSS_32_ecx_offset,CPU_Regs.reg_ecx.dword);
            Memory.mem_writed(cpu_tss.base+TSS_32_edx_offset,CPU_Regs.reg_edx.dword);
            Memory.mem_writed(cpu_tss.base+TSS_32_ebx_offset,CPU_Regs.reg_ebx.dword);
            Memory.mem_writed(cpu_tss.base+TSS_32_esp_offset,CPU_Regs.reg_esp.dword);
            Memory.mem_writed(cpu_tss.base+TSS_32_ebp_offset,CPU_Regs.reg_ebp.dword);
            Memory.mem_writed(cpu_tss.base+TSS_32_esi_offset,CPU_Regs.reg_esi.dword);
            Memory.mem_writed(cpu_tss.base+TSS_32_edi_offset,CPU_Regs.reg_edi.dword);

            Memory.mem_writed(cpu_tss.base+TSS_32_es_offset,CPU_Regs.reg_esVal.dword);
            Memory.mem_writed(cpu_tss.base+TSS_32_cs_offset,CPU_Regs.reg_csVal.dword);
            Memory.mem_writed(cpu_tss.base+TSS_32_ss_offset,CPU_Regs.reg_ssVal.dword);
            Memory.mem_writed(cpu_tss.base+TSS_32_ds_offset,CPU_Regs.reg_dsVal.dword);
            Memory.mem_writed(cpu_tss.base+TSS_32_fs_offset,CPU_Regs.reg_fsVal.dword);
            Memory.mem_writed(cpu_tss.base+TSS_32_gs_offset,CPU_Regs.reg_gsVal.dword);
        } else {
            Log.exit("286 task switch");
        }

        /* Setup a back link to the old TSS in new TSS */
        if (tstype==TSwitchType.TSwitch_CALL_INT) {
            if (new_tss_temp.is386 != 0) {
                Memory.mem_writed(new_tss_temp.base+TSS_32_back_offset,cpu_tss.selector);
            } else {
                Memory.mem_writew(new_tss_temp.base+TSS_16_back_offset,cpu_tss.selector);
            }
            /* And make the new task's eflag have the nested task bit */
            new_eflags|=CPU_Regs.NT;
        }
        /* Set the busy bit in the new task */
        if (tstype==TSwitchType.TSwitch_JMP || tstype==TSwitchType.TSwitch_CALL_INT) {
            new_tss_temp.desc.SetBusy(true);
            new_tss_temp.SaveSelector();
        }

//	cpu.cr0|=CR0_TASKSWITCHED;
        if (new_tss_selector == cpu_tss.selector) {
            CPU_Regs.reg_eip=old_eip;
            new_cs = CPU_Regs.reg_csVal.dword;
            new_ss = CPU_Regs.reg_ssVal.dword;
            new_ds = CPU_Regs.reg_dsVal.dword;
            new_es = CPU_Regs.reg_esVal.dword;
            new_fs = CPU_Regs.reg_esVal.dword;
            new_gs = CPU_Regs.reg_gsVal.dword;
        } else {

            /* Setup the new cr3 */
            if (Paging.cr3 != new_cr3) {
			    // if they are the same it is not flushed
			    // according to the 386 manual
			    Paging.PAGING_SetDirBase(new_cr3);
            }

            /* Load new context */
            if (new_tss_temp.is386 != 0) {
                CPU_Regs.reg_eip=new_eip;
                CPU_SetFlags(new_eflags,CPU_Regs.FMASK_ALL | CPU_Regs.VM);
                CPU_Regs.reg_eax.dword=new_eax;
                CPU_Regs.reg_ecx.dword=new_ecx;
                CPU_Regs.reg_edx.dword=new_edx;
                CPU_Regs.reg_ebx.dword=new_ebx;
                CPU_Regs.reg_esp.dword=new_esp;
                CPU_Regs.reg_ebp.dword=new_ebp;
                CPU_Regs.reg_edi.dword=new_edi;
                CPU_Regs.reg_esi.dword=new_esi;

//			new_cs=mem_readw(new_tss.base+TSS_32_cs));
            } else {
                Log.exit("286 task switch");
            }
        }
        /* Load the new selectors */
        if ((CPU_Regs.flags & CPU_Regs.VM) != 0) {
            CPU_Regs.SegSet16CS((int)new_cs);
            cpu.code.big=false;
            CPU_SetCPL(3);			//We don't have segment caches so this will do
        } else {
            /* Protected mode task */
            if (new_ldt!=0) CPU_LLDT(new_ldt);
            /* Load the new CS*/

            CPU_SetCPL((int)(new_cs & 3));
            if (!cpu.gdt.GetDescriptor((int)new_cs,cs_desc_temp))
                Log.exit("Task switch with CS beyond limits");
            if (cs_desc_temp.saved.seg.p()==0)
                Log.exit("Task switch with non present code-segment");
            switch (cs_desc_temp.Type()) {
            case DESC_CODE_N_NC_A:		case DESC_CODE_N_NC_NA:
            case DESC_CODE_R_NC_A:		case DESC_CODE_R_NC_NA:
                if (cpu.cpl != cs_desc_temp.DPL()) Log.exit("Task CS RPL != DPL");
                //goto doconforming;
                CPU_Regs.reg_csPhys.dword=cs_desc_temp.GetBase();
                cpu.code.big=cs_desc_temp.Big()>0;
                CPU_Regs.reg_csVal.dword=new_cs;
                break;
            case DESC_CODE_N_C_A:		case DESC_CODE_N_C_NA:
            case DESC_CODE_R_C_A:		case DESC_CODE_R_C_NA:
                if (cpu.cpl < cs_desc_temp.DPL()) Log.exit("Task CS RPL < DPL");
            //doconforming:
                CPU_Regs.reg_csPhys.dword=cs_desc_temp.GetBase();
                cpu.code.big=cs_desc_temp.Big()>0;
                CPU_Regs.reg_csVal.dword=new_cs;
                break;
            default:
                Log.exit("Task switch CS Type "+cs_desc_temp.Type());
            }
        }
        CPU_SetSegGeneralES((int)new_es);
        CPU_SetSegGeneralSS((int)new_ss);
        CPU_SetSegGeneralDS((int)new_ds);
        CPU_SetSegGeneralFS((int)new_fs);
        CPU_SetSegGeneralGS((int)new_gs);
        if (!cpu_tss.SetSelector(new_tss_selector)) {
            if (Log.level<=LogSeverities.LOG_NORMAL) Log.log(LogTypes.LOG_CPU, LogSeverities.LOG_NORMAL, "TaskSwitch: set tss selector "+Integer.toString(new_tss_selector, 16)+" failed");
        }
//	cpu_tss.desc.SetBusy(true);
//	cpu_tss.SaveSelector();
//	LOG_MSG("Task CPL %X CS:%X IP:%X SS:%X SP:%X eflags %x",cpu.cpl,SegValue(cs),reg_eip,SegValue(ss),reg_esp,reg_flags);
        return true;
    }

    static boolean doexception(int port) {
        cpu.mpl=3;
        if (Log.level<=LogSeverities.LOG_NORMAL) Log.log(LogTypes.LOG_CPU,LogSeverities.LOG_NORMAL, "IO Exception port "+Integer.toString(port, 16));
        return CPU_PrepareException(EXCEPTION_GP,0);
    }

    public static boolean CPU_IO_Exception( /*Bitu*/int port, /*Bitu*/int size) {
        if (cpu.pmode && ((CPU_Regs.GETFLAG_IOPL()<cpu.cpl) || CPU_Regs.GETFLAG(CPU_Regs.VM)!=0)) {
            cpu.mpl=0;
            if (cpu_tss.is386==0) return doexception(port);
             /*PhysPt*/int bwhere=cpu_tss.base+0x66;
             /*Bitu*/int ofs=Memory.mem_readw(bwhere);
            if (ofs>cpu_tss.limit) return doexception(port);
            bwhere=cpu_tss.base+ofs+(port/8);
             /*Bitu*/int map=Memory.mem_readw(bwhere);
             /*Bitu*/int mask=(0xffff>>(16-size)) << (port&7);
            if ((map & mask) != 0) return doexception(port);
            cpu.mpl=3;
        }
        return false;
    }

    public static void CPU_Exception( /*Bitu*/int which) {
        CPU_Exception(which, 0);
    }
    public static void CPU_Exception( /*Bitu*/int which, /*Bitu*/int error ) {
//	LOG_MSG("Exception %d error %x",which,error);
        cpu.exception.error=error;
        int flags = CPU_INT_EXCEPTION;
        if (which == 0x8 || which >= 0xA && which <= 0xE) {
            flags |= CPU_INT_HAS_ERROR;
        }
        CPU_Interrupt(which,flags,CPU_Regs.reg_eip);
    }

    static private boolean CPU_CHECK_COND(boolean cond, String msg, int exc, int sel) {
        if (cond) {
            // Log.exit(msg+" "+exc+" "+sel);
            if (Log.level<=LogSeverities.LOG_WARN) Log.log(LogTypes.LOG_CPU,LogSeverities.LOG_WARN, msg);
            CPU_Exception(exc, sel);
            return true;
        }
        return false;
    }

    static public/*Bit8u*/int lastint;
    static private final Descriptor gate_temp_1 = new Descriptor();
    static private final Descriptor cs_desc_temp_1 = new Descriptor();
    static private final Descriptor n_ss_desc_temp_1 = new Descriptor();
    static private final /*Bitu*/IntRef n_ss_1=new IntRef(0);
    static private final IntRef n_esp_1=new IntRef(0);
    static private final /*Bitu*/IntRef o_ss_1=new IntRef(0);

    static private void CPU_Interrupt(/*Bitu*/int num, /*Bitu*/int type, /*Bitu*/int oldeip) {
        lastint=num;
        Flags.FillFlags();
        if (Config.C_DEBUG) {
            switch (num) {
            case 0xcd:
                if (Config.C_HEAVY_DEBUG) {
                    Log.log(LogTypes.LOG_CPU,LogSeverities.LOG_ERROR, "Call to interrupt 0xCD this is BAD");
                    Debug.DEBUG_HeavyWriteLogInstruction();
                    Log.exit("Call to interrupt 0xCD this is BAD");
                }
                break;
            case 0x03:
                if (Debug.DEBUG_Breakpoint()) {
                    CPU_Cycles=0;
                    return;
                }
            }
        }

        if (!cpu.pmode) {
            /* Save everything on a 16-bit stack */
            int esp = CPU_Push16(CPU_Regs.reg_esp.dword, CPU_Regs.flags & 0xffff);
            esp = CPU_Push16(esp, CPU_Regs.reg_csVal.dword);
            esp = CPU_Push16(esp, oldeip & 0xFFFF);

            /* Get the new CS:IP from vector table */
             /*PhysPt*/int base=cpu.idt.GetBase();
            int eip=Memory.mem_readw(base+(num << 2));

            // do writes now since PF can not happen after this read
            CPU_Regs.SegSet16CS(Memory.mem_readw(base+(num << 2)+2));
            cpu.code.big=false;

            CPU_Regs.SETFLAGBIT(CPU_Regs.IF,false);
            CPU_Regs.SETFLAGBIT(CPU_Regs.TF,false);
            CPU_Regs.reg_esp.dword = esp;
            CPU_Regs.reg_eip = eip;
            return;
        } else {
            /* Protected Mode Interrupt */
            if ((CPU_Regs.flags & CPU_Regs.VM) != 0 && (type & CPU_INT_SOFTWARE) != 0 && (type&CPU_INT_NOIOPLCHECK) == 0) {
//			LOG_MSG("Software int in v86, AH %X IOPL %x",reg_ah,(reg_flags & CPU_Regs.IOPL) >>12);
                if ((CPU_Regs.flags & CPU_Regs.IOPL)!=CPU_Regs.IOPL) {
                    CPU_Exception(EXCEPTION_GP,0);
                    return;
                }
            }


            if (!cpu.idt.GetDescriptor(num<<3, gate_temp_1)) {
                // zone66
                CPU_Exception(EXCEPTION_GP,num*8+2+(((type&CPU_INT_SOFTWARE)!=0)?0:1));
                return;
            }

            if ((type&CPU_INT_SOFTWARE)!=0 && (gate_temp_1.DPL()<cpu.cpl)) {
                // zone66, win3.x e
                CPU_Exception(EXCEPTION_GP,num*8+2);
                return;
            }


            switch (gate_temp_1.Type()) {
            case DESC_286_INT_GATE:		case DESC_386_INT_GATE:
            case DESC_286_TRAP_GATE:	case DESC_386_TRAP_GATE:
                {
                    if (CPU_CHECK_COND(gate_temp_1.saved.seg.p()==0, "INT:Gate segment not present", EXCEPTION_NP,num*8+2+(((type&CPU_INT_SOFTWARE)!=0)?0:1)))
                        return;

                     /*Bitu*/int gate_sel= gate_temp_1.GetSelector();
                     /*Bitu*/int gate_off= (int)gate_temp_1.GetOffset();
                    if (CPU_CHECK_COND((gate_sel & 0xfffc)==0, "INT:Gate with CS zero selector", EXCEPTION_GP,((type&CPU_INT_SOFTWARE)!=0)?0:1))
                        return;
                    boolean success = cpu.gdt.GetDescriptor(gate_sel,cs_desc_temp_1);
                    if (CPU_CHECK_COND(!success, "INT:Gate with CS beyond limit", EXCEPTION_GP,(gate_sel & 0xfffc)+(((type&CPU_INT_SOFTWARE)!=0)?0:1)))
                        return;

                     /*Bitu*/int cs_dpl=cs_desc_temp_1.DPL();
                    if (CPU_CHECK_COND(cs_dpl>cpu.cpl, "Interrupt to higher privilege", EXCEPTION_GP,(gate_sel & 0xfffc)+(((type&CPU_INT_SOFTWARE)!=0)?0:1)))
                        return;
                    switch (cs_desc_temp_1.Type()) {
                    case DESC_CODE_N_NC_A:	case DESC_CODE_N_NC_NA:
                    case DESC_CODE_R_NC_A:	case DESC_CODE_R_NC_NA:
                        if (cs_dpl<cpu.cpl) {
                            /* Prepare for gate to inner level */
                            if (CPU_CHECK_COND(cs_desc_temp_1.saved.seg.p()==0, "INT:Inner level:CS segment not present", EXCEPTION_NP,(gate_sel & 0xfffc)+(((type&CPU_INT_SOFTWARE)!=0)?0:1)))
                                return;
                            if (CPU_CHECK_COND((CPU_Regs.flags & CPU_Regs.VM)!=0 && (cs_dpl!=0), "V86 interrupt calling codesegment with DPL>0", EXCEPTION_GP,gate_sel & 0xfffc))
                                return;

                            o_ss_1.value=CPU_Regs.reg_ssVal.dword;
                            int o_esp=CPU_Regs.reg_esp.dword;
                            cpu_tss.Get_SSx_ESPx(cs_dpl,n_ss_1,n_esp_1);
                            if (CPU_CHECK_COND((n_ss_1.value & 0xfffc)==0, "INT:Gate with SS zero selector", EXCEPTION_TS,((type&CPU_INT_SOFTWARE)!=0)?0:1))
                                return;

                            success = cpu.gdt.GetDescriptor(n_ss_1.value,n_ss_desc_temp_1);
                            if (CPU_CHECK_COND(!success, "INT:Gate with SS beyond limit", EXCEPTION_TS,(n_ss_1.value & 0xfffc)+(((type&CPU_INT_SOFTWARE)!=0)?0:1)))
                                return;
                            if (CPU_CHECK_COND(((n_ss_1.value & 3)!=cs_dpl) || (n_ss_desc_temp_1.DPL()!=cs_dpl), "INT:Inner level with CS_DPL!=SS_DPL and SS_RPL", EXCEPTION_TS,(n_ss_1.value & 0xfffc)+(((type&CPU_INT_SOFTWARE)!=0)?0:1)))
                                return;

                            // check if stack segment is a writable data segment
                            switch (n_ss_desc_temp_1.Type()) {
                            case DESC_DATA_EU_RW_NA:		case DESC_DATA_EU_RW_A:
                            case DESC_DATA_ED_RW_NA:		case DESC_DATA_ED_RW_A:
                                break;
                            default:
                                Log.exit("INT:Inner level:Stack segment not writable.");		// or #TS(ss_sel+EXT)
                            }
                            if (CPU_CHECK_COND(n_ss_desc_temp_1.saved.seg.p()==0, "INT:Inner level with nonpresent SS", EXCEPTION_SS,(n_ss_1.value & 0xfffc)+(((type&CPU_INT_SOFTWARE)!=0)?0:1)))
                                return;

                            // commit point
                            CPU_Regs.reg_ssPhys.dword=n_ss_desc_temp_1.GetBase();
                            CPU_Regs.reg_ssVal.dword=n_ss_1.value;
                            if (n_ss_desc_temp_1.Big()!=0) {
                                cpu.stack.big=true;
                                cpu.stack.mask=0xffffffff;
                                cpu.stack.notmask=0;
                                CPU_Regs.reg_esp.dword=n_esp_1.value;
                            } else {
                                cpu.stack.big=false;
                                cpu.stack.mask=0xffff;
                                cpu.stack.notmask=0xffff0000;
                                CPU_Regs.reg_esp.word((int)(n_esp_1.value & 0xffff));
                            }

                            CPU_SetCPL(cs_dpl);
                            if ((gate_temp_1.Type() & 0x8)!=0) {	/* 32-bit Gate */
                                if ((CPU_Regs.flags & CPU_Regs.VM)!=0) {
                                    CPU_Push32(CPU_Regs.reg_gsVal.dword);CPU_Regs.SegSet16GS(0x0);
                                    CPU_Push32(CPU_Regs.reg_fsVal.dword);CPU_Regs.SegSet16FS(0x0);
                                    CPU_Push32(CPU_Regs.reg_dsVal.dword);CPU_Regs.SegSet16DS(0x0);
                                    CPU_Push32(CPU_Regs.reg_esVal.dword);CPU_Regs.SegSet16ES(0x0);
                                }
                                CPU_Push32(o_ss_1.value);
                                CPU_Push32(o_esp);
                            } else {					/* 16-bit Gate */
                                if ((CPU_Regs.flags & CPU_Regs.VM)!=0) Log.exit("V86 to 16-bit gate");
                                CPU_Push16(o_ss_1.value);
                                CPU_Push16(o_esp);
                            }
//						LOG_MSG("INT:Gate to inner level SS:%X SP:%X",n_ss,n_esp);
                            //goto do_interrupt;
                            if ((gate_temp_1.Type() & 0x8)!=0) {	/* 32-bit Gate */
                                CPU_Push32(CPU_Regs.flags);
                                CPU_Push32(CPU_Regs.reg_csVal.dword);
                                CPU_Push32(oldeip);
                                if ((type & CPU_INT_HAS_ERROR)!=0) CPU_Push32(cpu.exception.error);
                            } else {					/* 16-bit gate */
                                CPU_Push16(CPU_Regs.flags & 0xffff);
                                CPU_Push16(CPU_Regs.reg_csVal.dword);
                                CPU_Push16(oldeip & 0xFFFF);
                                if ((type & CPU_INT_HAS_ERROR)!=0) CPU_Push16(cpu.exception.error);
                            }
                            break;
                        }
                        if (cs_dpl!=cpu.cpl)
                            Log.exit("Non-conforming intra privilege INT with DPL!=CPL");
                    case DESC_CODE_N_C_A:	case DESC_CODE_N_C_NA:
                    case DESC_CODE_R_C_A:	case DESC_CODE_R_C_NA:
                        /* Prepare stack for gate to same priviledge */
                        if (CPU_CHECK_COND(cs_desc_temp_1.saved.seg.p()==0, "INT:Same level:CS segment not present", EXCEPTION_NP,(gate_sel & 0xfffc)+(((type&CPU_INT_SOFTWARE)!=0)?0:1)))
                            return;
                        if ((CPU_Regs.flags & CPU_Regs.VM)!=0 && (cs_dpl<cpu.cpl))
                            Log.exit("V86 interrupt doesn't change to pl0");	// or #GP(cs_sel)

                        // commit point
    //do_interrupt:
                        if ((gate_temp_1.Type() & 0x8)!=0) {	/* 32-bit Gate */
                            CPU_Push32(CPU_Regs.flags);
                            CPU_Push32(CPU_Regs.reg_csVal.dword);
                            CPU_Push32(oldeip);
                            if ((type & CPU_INT_HAS_ERROR)!=0) CPU_Push32(cpu.exception.error);
                        } else {					/* 16-bit gate */
                            CPU_Push16(CPU_Regs.flags & 0xffff);
                            CPU_Push16(CPU_Regs.reg_csVal.dword);
                            CPU_Push16(oldeip & 0xFFFF);
                            if ((type & CPU_INT_HAS_ERROR)!=0) CPU_Push16(cpu.exception.error);
                        }
                        break;
                    default:
                        Log.exit("INT:Gate Selector points to illegal descriptor with type "+Integer.toString(cs_desc_temp_1.Type(),16));
                    }

                    CPU_Regs.reg_csVal.dword=(gate_sel&0xfffc) | cpu.cpl;
                    CPU_Regs.reg_csPhys.dword=cs_desc_temp_1.GetBase();
                    cpu.code.big=cs_desc_temp_1.Big()>0;
                    CPU_Regs.reg_eip=gate_off;

                    if ((gate_temp_1.Type()&1)==0) {
                        CPU_Regs.SETFLAGBIT(CPU_Regs.IF,false);
                    }
                    CPU_Regs.SETFLAGBIT(CPU_Regs.TF,false);
                    CPU_Regs.SETFLAGBIT(CPU_Regs.NT,false);
                    CPU_Regs.SETFLAGBIT(CPU_Regs.VM,false);
                    if (Log.level<=LogSeverities.LOG_NORMAL) Log.log(LogTypes.LOG_CPU,LogSeverities.LOG_NORMAL,"INT:Gate to "+Integer.toString(gate_sel, 16)+":"+Long.toString(gate_off, 16)+" big "+cs_desc_temp_1.Big()+" "+((gate_temp_1.Type() & 0x8) != 0 ? "386" : "286"));
                    return;
                }
            case DESC_TASK_GATE:
                if (CPU_CHECK_COND(gate_temp_1.saved.seg.p()==0, "INT:Gate segment not present", EXCEPTION_NP,num*8+2+(((type&CPU_INT_SOFTWARE)!=0)?0:1)))
                    return;

                CPU_SwitchTask(gate_temp_1.GetSelector(),TSwitchType.TSwitch_CALL_INT,oldeip);
                if ((type & CPU_INT_HAS_ERROR)!=0) {
                    //TODO Be sure about this, seems somewhat unclear
                    if (cpu_tss.is386!=0) CPU_Push32(cpu.exception.error);
                    else CPU_Push16(cpu.exception.error);
                }
                return;
            default:
                Log.exit("Illegal descriptor type "+Integer.toString(gate_temp_1.Type(), 16)+" for int "+Integer.toString(num,16));
            }
        }
        throw new RuntimeException();
    }

    static private final Descriptor n_cs_desc_2 = new Descriptor();
    static private final Descriptor n_ss_desc_2 = new Descriptor();
    static public boolean iret = false;
    static public void CPU_IRET(boolean use32, /*Bitu*/int oldeip) {
        iret = true;
        if (!cpu.pmode) {					/* RealMode IRET */
            if (use32) {
                CPU_Regs.reg_eip=CPU_Pop32();
                CPU_Regs.SegSet16CS(CPU_Pop32());
                CPU_SetFlags(CPU_Pop32(),CPU_Regs.FMASK_ALL);
            } else {
                CPU_Regs.reg_eip=CPU_Pop16();
                CPU_Regs.SegSet16CS(CPU_Pop16());
                CPU_SetFlags(CPU_Pop16(),CPU_Regs.FMASK_ALL & 0xffff);
            }
            cpu.code.big=false;
            Flags.DestroyConditionFlags();
        } else {	/* Protected mode IRET */
            if ((CPU_Regs.flags & CPU_Regs.VM)!=0) {
                if ((CPU_Regs.flags & CPU_Regs.IOPL)!=CPU_Regs.IOPL) {
                    // win3.x e
                    CPU_Exception(EXCEPTION_GP,0);
                    return;
                } else {
                    if (use32) {
                        /*Bit32u*/int new_eip=Memory.mem_readd(CPU_Regs.reg_ssPhys.dword + (CPU_Regs.reg_esp.dword & cpu.stack.mask));
                        /*Bit32u*/int tempesp=(CPU_Regs.reg_esp.dword & cpu.stack.notmask)|((CPU_Regs.reg_esp.dword+4)&cpu.stack.mask);
                        /*Bit32u*/int new_cs=Memory.mem_readd(CPU_Regs.reg_ssPhys.dword + (tempesp & cpu.stack.mask));
                        tempesp=(tempesp&cpu.stack.notmask)|((tempesp+4)&cpu.stack.mask);
                        /*Bit32u*/int new_flags=Memory.mem_readd(CPU_Regs.reg_ssPhys.dword + (tempesp & cpu.stack.mask));
                        CPU_Regs.reg_esp.dword=(tempesp&cpu.stack.notmask)|((tempesp+4)&cpu.stack.mask);

                        CPU_Regs.reg_eip=new_eip;
                        CPU_Regs.SegSet16CS((new_cs&0xffff));
                        /* IOPL can not be modified in v86 mode by IRET */
                        CPU_SetFlags(new_flags,CPU_Regs.FMASK_NORMAL|CPU_Regs.NT);
                    } else {
                        /*Bit16u*/int new_eip=Memory.mem_readw(CPU_Regs.reg_ssPhys.dword + (CPU_Regs.reg_esp.dword & cpu.stack.mask));
                        /*Bit32u*/int tempesp=(CPU_Regs.reg_esp.dword & cpu.stack.notmask)|((CPU_Regs.reg_esp.dword + 2)&cpu.stack.mask);
                        /*Bit16u*/int new_cs=Memory.mem_readw(CPU_Regs.reg_ssPhys.dword + (tempesp & cpu.stack.mask));
                        tempesp=(tempesp&cpu.stack.notmask)|((tempesp+2)&cpu.stack.mask);
                        /*Bit16u*/int new_flags=Memory.mem_readw(CPU_Regs.reg_ssPhys.dword + (tempesp & cpu.stack.mask));
                        CPU_Regs.reg_esp.dword=(tempesp&cpu.stack.notmask)|((tempesp+2)&cpu.stack.mask);

                        CPU_Regs.reg_eip=new_eip;
                        CPU_Regs.SegSet16CS(new_cs);
                        /* IOPL can not be modified in v86 mode by IRET */
                        CPU_SetFlags(new_flags,CPU_Regs.FMASK_NORMAL|CPU_Regs.NT);
                    }
                    cpu.code.big=false;
                    Flags.DestroyConditionFlags();
                    return;
                }
            }
            /* Check if this is task IRET */
            if (CPU_Regs.GETFLAG(CPU_Regs.NT)!=0) {
                if (CPU_Regs.GETFLAG(CPU_Regs.VM)!=0) Log.exit("Pmode IRET with VM bit set");
                if (CPU_CHECK_COND(!cpu_tss.IsValid(), "TASK Iret without valid TSS", EXCEPTION_TS,cpu_tss.selector & 0xfffc))
                    return;
                if (cpu_tss.desc.IsBusy()==0) {
                    Log.log(LogTypes.LOG_CPU,LogSeverities.LOG_ERROR,"TASK Iret:TSS not busy");
                }
                 /*Bitu*/int back_link=cpu_tss.Get_back();
                CPU_SwitchTask(back_link,TSwitchType.TSwitch_IRET,oldeip);
                return;
            }
            int n_cs_sel, n_flags;
             /*Bitu*/int n_eip;
            /*Bit32u*/int tempesp;
            if (use32) {
                n_eip=Memory.mem_readd(CPU_Regs.reg_ssPhys.dword + (CPU_Regs.reg_esp.dword & cpu.stack.mask));
                tempesp=(CPU_Regs.reg_esp.dword & cpu.stack.notmask)|((CPU_Regs.reg_esp.dword + 4) & cpu.stack.mask);
                n_cs_sel=Memory.mem_readd(CPU_Regs.reg_ssPhys.dword + (tempesp & cpu.stack.mask)) & 0xffff;
                tempesp=(tempesp&cpu.stack.notmask)|((tempesp+4)&cpu.stack.mask);
                n_flags=Memory.mem_readd(CPU_Regs.reg_ssPhys.dword + (tempesp & cpu.stack.mask));
                tempesp=(tempesp&cpu.stack.notmask)|((tempesp+4)&cpu.stack.mask);

                if ((n_flags & CPU_Regs.VM) != 0 && (cpu.cpl==0)) {
                    // commit point
                    CPU_Regs.reg_esp.dword=tempesp;
                    CPU_Regs.reg_eip=n_eip & 0xffff;
                     /*Bitu*/int n_ss,n_esp,n_es,n_ds,n_fs,n_gs;
                    n_esp=CPU_Pop32();
                    n_ss=CPU_Pop32() & 0xffff;
                    n_es=CPU_Pop32() & 0xffff;
                    n_ds=CPU_Pop32() & 0xffff;
                    n_fs=CPU_Pop32() & 0xffff;
                    n_gs=CPU_Pop32() & 0xffff;

                    CPU_SetFlags(n_flags,CPU_Regs.FMASK_ALL | CPU_Regs.VM);
                    Flags.DestroyConditionFlags();
                    CPU_SetCPL(3);

                    CPU_SetSegGeneralSS(n_ss);
                    CPU_SetSegGeneralES(n_es);
                    CPU_SetSegGeneralDS(n_ds);
                    CPU_SetSegGeneralFS(n_fs);
                    CPU_SetSegGeneralGS(n_gs);
                    CPU_Regs.reg_esp.dword=n_esp;
                    cpu.code.big=false;
                    CPU_Regs.SegSet16CS(n_cs_sel);
                    if (Log.level<=LogSeverities.LOG_NORMAL) Log.log(LogTypes.LOG_CPU,LogSeverities.LOG_NORMAL, "IRET:Back to V86: CS:"+Integer.toString(CPU_Regs.reg_csVal.dword, 16)+" IP "+Integer.toString(CPU_Regs.reg_eip, 16)+" SS:"+Integer.toString(CPU_Regs.reg_ssVal.dword, 16)+" SP "+Integer.toString(CPU_Regs.reg_esp.dword, 16)+" FLAGS:%X"+Integer.toString(CPU_Regs.flags,16));
                    return;
                }
                if ((n_flags & CPU_Regs.VM)!=0) Log.exit("IRET from pmode to v86 with CPL!=0");
            } else {
                n_eip=Memory.mem_readw(CPU_Regs.reg_ssPhys.dword + (CPU_Regs.reg_esp.dword & cpu.stack.mask));
                tempesp=(CPU_Regs.reg_esp.dword & cpu.stack.notmask)|((CPU_Regs.reg_esp.dword + 2) & cpu.stack.mask);
                n_cs_sel=Memory.mem_readw(CPU_Regs.reg_ssPhys.dword + (tempesp & cpu.stack.mask));
                tempesp=(tempesp&cpu.stack.notmask)|((tempesp+2)&cpu.stack.mask);
                n_flags=Memory.mem_readw(CPU_Regs.reg_ssPhys.dword + (tempesp & cpu.stack.mask));
                n_flags|=(CPU_Regs.flags & 0xffff0000);
                tempesp=(tempesp&cpu.stack.notmask)|((tempesp+2)&cpu.stack.mask);

                if ((n_flags & CPU_Regs.VM)!=0) Log.exit("VM Flag in 16-bit iret");
            }
            if (CPU_CHECK_COND((n_cs_sel & 0xfffc)==0, "IRET:CS selector zero", EXCEPTION_GP,0))
                return;
             /*Bitu*/int n_cs_rpl=n_cs_sel & 3;

            boolean success = cpu.gdt.GetDescriptor(n_cs_sel,n_cs_desc_2);
            if (CPU_CHECK_COND(!success, "IRET:CS selector beyond limits", EXCEPTION_GP,(n_cs_sel & 0xfffc))) {
                return;
            }
            if (CPU_CHECK_COND(n_cs_rpl<cpu.cpl, "IRET to lower privilege", EXCEPTION_GP,(n_cs_sel & 0xfffc))) {
                return;
            }

            switch (n_cs_desc_2.Type()) {
            case DESC_CODE_N_NC_A:	case DESC_CODE_N_NC_NA:
            case DESC_CODE_R_NC_A:	case DESC_CODE_R_NC_NA:
                if (CPU_CHECK_COND(n_cs_rpl!=n_cs_desc_2.DPL(), "IRET:NC:DPL!=RPL", EXCEPTION_GP,(n_cs_sel & 0xfffc)))
                    return;
                break;
            case DESC_CODE_N_C_A:	case DESC_CODE_N_C_NA:
            case DESC_CODE_R_C_A:	case DESC_CODE_R_C_NA:
                if (CPU_CHECK_COND(n_cs_desc_2.DPL()>n_cs_rpl, "IRET:C:DPL>RPL", EXCEPTION_GP,(n_cs_sel & 0xfffc)))
                    return;
                break;
            default:
                Log.exit("IRET:Illegal descriptor type "+Integer.toString(n_cs_desc_2.Type(),16));
            }
            if (CPU_CHECK_COND(n_cs_desc_2.saved.seg.p()==0, "IRET with nonpresent code segment",EXCEPTION_NP,(n_cs_sel & 0xfffc)))
                return;

            if (n_cs_rpl==cpu.cpl) {
                /* Return to same level */

                // commit point
                CPU_Regs.reg_esp.dword=tempesp;
                CPU_Regs.reg_csPhys.dword=n_cs_desc_2.GetBase();
                cpu.code.big=n_cs_desc_2.Big()>0;
                CPU_Regs.reg_csVal.dword=n_cs_sel;
                CPU_Regs.reg_eip=n_eip;

                 /*Bitu*/int mask=cpu.cpl !=0 ? (CPU_Regs.FMASK_NORMAL | CPU_Regs.NT) : CPU_Regs.FMASK_ALL;
                if (CPU_Regs.GETFLAG_IOPL()<cpu.cpl) mask &= (~CPU_Regs.IF);
                CPU_SetFlags(n_flags,mask);
                Flags.DestroyConditionFlags();
                //Log.log(LogTypes.LOG_CPU,LogSeverities.LOG_NORMAL,"IRET:Same level:%X:%X big %b",n_cs_sel,n_eip,cpu.code.big);
            } else {
                /* Return to outer level */
                 /*Bitu*/int n_ss;
                int n_esp;
                if (use32) {
                    n_esp=Memory.mem_readd(CPU_Regs.reg_ssPhys.dword + (tempesp & cpu.stack.mask));
                    tempesp=(tempesp&cpu.stack.notmask)|((tempesp+4)&cpu.stack.mask);
                    n_ss=Memory.mem_readd(CPU_Regs.reg_ssPhys.dword + (tempesp & cpu.stack.mask)) & 0xffff;
                } else {
                    n_esp=Memory.mem_readw(CPU_Regs.reg_ssPhys.dword + (tempesp & cpu.stack.mask));
                    tempesp=(tempesp&cpu.stack.notmask)|((tempesp+2)&cpu.stack.mask);
                    n_ss=Memory.mem_readw(CPU_Regs.reg_ssPhys.dword + (tempesp & cpu.stack.mask));
                }
                if (CPU_CHECK_COND((n_ss & 0xfffc)==0, "IRET:Outer level:SS selector zero", EXCEPTION_GP,0))
                    return;
                if (CPU_CHECK_COND((n_ss & 3)!=n_cs_rpl, "IRET:Outer level:SS rpl!=CS rpl", EXCEPTION_GP,n_ss & 0xfffc))
                    return;

                success = cpu.gdt.GetDescriptor(n_ss,n_ss_desc_2);
                if (CPU_CHECK_COND(!success, "IRET:Outer level:SS beyond limit", EXCEPTION_GP,n_ss & 0xfffc))
                    return;
                if (CPU_CHECK_COND(n_ss_desc_2.DPL()!=n_cs_rpl, "IRET:Outer level:SS dpl!=CS rpl", EXCEPTION_GP,n_ss & 0xfffc))
                    return;

                // check if stack segment is a writable data segment
                switch (n_ss_desc_2.Type()) {
                case DESC_DATA_EU_RW_NA:		case DESC_DATA_EU_RW_A:
                case DESC_DATA_ED_RW_NA:		case DESC_DATA_ED_RW_A:
                    break;
                default:
                    Log.exit("IRET:Outer level:Stack segment not writable");		// or #GP(ss_sel)
                }
                if (CPU_CHECK_COND(n_ss_desc_2.saved.seg.p()==0, "IRET:Outer level:Stack segment not present", EXCEPTION_NP,n_ss & 0xfffc))
                    return;

                // commit point

                CPU_Regs.reg_csPhys.dword=n_cs_desc_2.GetBase();
                cpu.code.big=n_cs_desc_2.Big()>0;
                CPU_Regs.reg_csVal.dword=n_cs_sel;

                 /*Bitu*/int mask=cpu.cpl!=0 ? (CPU_Regs.FMASK_NORMAL | CPU_Regs.NT) : CPU_Regs.FMASK_ALL;
                if (CPU_Regs.GETFLAG_IOPL()<cpu.cpl) mask &= (~CPU_Regs.IF);
                CPU_SetFlags(n_flags,mask);
                Flags.DestroyConditionFlags();

                CPU_SetCPL(n_cs_rpl);
                CPU_Regs.reg_eip=n_eip;

                CPU_Regs.reg_ssVal.dword=n_ss;
                CPU_Regs.reg_ssPhys.dword=n_ss_desc_2.GetBase();
                if (n_ss_desc_2.Big()!=0) {
                    cpu.stack.big=true;
                    cpu.stack.mask=0xffffffff;
                    cpu.stack.notmask=0;
                    CPU_Regs.reg_esp.dword=n_esp;
                } else {
                    cpu.stack.big=false;
                    cpu.stack.mask=0xffff;
                    cpu.stack.notmask=0xffff0000;
                    CPU_Regs.reg_esp.word((int)(n_esp & 0xffffl));
                }

                // borland extender, zrdx
                CPU_CheckSegments();

                if (Log.level<=LogSeverities.LOG_NORMAL) Log.log(LogTypes.LOG_CPU,LogSeverities.LOG_NORMAL, "IRET:Outer level:"+Integer.toString(n_cs_sel, 16)+":"+Long.toString(n_eip, 16)+" big "+cpu.code.big);
            }
        }
    }

    static private final Descriptor desc_3 = new Descriptor();

    public static void CPU_JMP(boolean use32, /*Bitu*/int selector, /*Bitu*/int offset, /*Bitu*/int oldeip) {
        if (!cpu.pmode || (CPU_Regs.flags & CPU_Regs.VM)!=0) {
            if (!use32) {
                CPU_Regs.reg_eip=offset&0xffff;
            } else {
                CPU_Regs.reg_eip=offset;
            }
            CPU_Regs.SegSet16CS(selector);
            cpu.code.big=false;
        } else {
            if (CPU_CHECK_COND((selector & 0xfffc)==0, "JMP:CS selector zero", EXCEPTION_GP,0))
                return;
             /*Bitu*/int rpl=selector & 3;

            boolean success = cpu.gdt.GetDescriptor(selector,desc_3);
            if (CPU_CHECK_COND(!success, "JMP:CS beyond limits", EXCEPTION_GP,selector & 0xfffc))
                return;
            switch (desc_3.Type()) {
            case DESC_CODE_N_NC_A:		case DESC_CODE_N_NC_NA:
            case DESC_CODE_R_NC_A:		case DESC_CODE_R_NC_NA:
                if (CPU_CHECK_COND(rpl>cpu.cpl, "JMP:NC:RPL>CPL", EXCEPTION_GP,selector & 0xfffc))
                    return;
                if (CPU_CHECK_COND(cpu.cpl!=desc_3.DPL(), "JMP:NC:RPL != DPL", EXCEPTION_GP,selector & 0xfffc))
                    return;
                if (Log.level<=LogSeverities.LOG_NORMAL) Log.log(LogTypes.LOG_CPU,LogSeverities.LOG_NORMAL,"JMP:Code:NC to "+Integer.toString(selector, 16)+":"+Integer.toString(offset, 16)+" big "+desc_3.Big());
                //goto CODE_jmp;
                if (desc_3.saved.seg.p()==0) {
                    // win
                    CPU_Exception(EXCEPTION_NP,selector & 0xfffc);
                    return;
                }

                /* Normal jump to another selector:offset */
                CPU_Regs.reg_csPhys.dword=desc_3.GetBase();
                cpu.code.big=desc_3.Big()>0;
                CPU_Regs.reg_csVal.dword=(selector & 0xfffc) | cpu.cpl;
                CPU_Regs.reg_eip=offset;
                return;
            case DESC_CODE_N_C_A:		case DESC_CODE_N_C_NA:
            case DESC_CODE_R_C_A:		case DESC_CODE_R_C_NA:
                if (Log.level<=LogSeverities.LOG_NORMAL) Log.log(LogTypes.LOG_CPU,LogSeverities.LOG_NORMAL,"JMP:Code:C to "+Integer.toString(selector, 16)+":"+Integer.toString(offset, 16)+" big "+desc_3.Big());
                if (CPU_CHECK_COND(cpu.cpl<desc_3.DPL(), "JMP:C:CPL < DPL", EXCEPTION_GP,selector & 0xfffc))
                    return;
    //CODE_jmp:
                if (desc_3.saved.seg.p()==0) {
                    // win
                    CPU_Exception(EXCEPTION_NP,selector & 0xfffc);
                    return;
                }

                /* Normal jump to another selector:offset */
                CPU_Regs.reg_csPhys.dword=desc_3.GetBase();
                cpu.code.big=desc_3.Big()>0;
                CPU_Regs.reg_csVal.dword=(selector & 0xfffc) | cpu.cpl;
                CPU_Regs.reg_eip=offset;
                return;
            case DESC_386_TSS_A:
                if (CPU_CHECK_COND(desc_3.DPL()<cpu.cpl, "JMP:TSS:dpl<cpl", EXCEPTION_GP,selector & 0xfffc))
                    return;
                if (CPU_CHECK_COND(desc_3.DPL()<rpl, "JMP:TSS:dpl<rpl", EXCEPTION_GP,selector & 0xfffc))
                    return;
                if (Log.level<=LogSeverities.LOG_NORMAL) Log.log(LogTypes.LOG_CPU,LogSeverities.LOG_NORMAL,"JMP:TSS to "+Integer.toString(selector,16));
                CPU_SwitchTask(selector,TSwitchType.TSwitch_JMP,oldeip);
                break;
            default:
                Log.exit("JMP Illegal descriptor type "+Integer.toString(desc_3.Type(),16));
            }
        }
    }

    static final private Descriptor call_4=new Descriptor();
    static final private Descriptor n_cs_desc_4 = new Descriptor();
    static final private Descriptor n_ss_desc_4 = new Descriptor();
    static final private /*Bitu*/IntRef n_ss_sel_4 = new IntRef(0);
    static final private IntRef n_esp_4 = new IntRef(0);

    public static void CPU_CALL(boolean use32, /*Bitu*/int selector, /*Bitu*/int offset, /*Bitu*/int oldeip) {
        if (!cpu.pmode || (CPU_Regs.flags & CPU_Regs.VM)!=0) {
            int esp = CPU_Regs.reg_esp.dword;
            if (!use32) {
                esp = CPU_Push16(esp, CPU_Regs.reg_csVal.dword);
                esp = CPU_Push16(esp, oldeip & 0xFFFF);
                CPU_Regs.reg_eip=offset & 0xffff;
            } else {
                esp = CPU_Push32(esp, CPU_Regs.reg_csVal.dword);
                esp = CPU_Push32(esp, oldeip);
                CPU_Regs.reg_eip=offset;
            }
            CPU_Regs.reg_esp.dword = esp; // don't set ESP until we are done with Memory Writes / CPU_Push so that we are reentrant
            cpu.code.big=false;
            CPU_Regs.SegSet16CS(selector);
        } else {
            if (CPU_CHECK_COND((selector & 0xfffc)==0, "CALL:CS selector zero", EXCEPTION_GP,0))
                return;
             /*Bitu*/int rpl=selector & 3;

            boolean success = cpu.gdt.GetDescriptor(selector,call_4);
            int esp;

            if (CPU_CHECK_COND(!success, "CALL:CS beyond limits", EXCEPTION_GP,selector & 0xfffc))
                return;
            /* Check for type of far call */
            switch (call_4.Type()) {
            case DESC_CODE_N_NC_A:case DESC_CODE_N_NC_NA:
            case DESC_CODE_R_NC_A:case DESC_CODE_R_NC_NA:
                if (CPU_CHECK_COND(rpl>cpu.cpl, "CALL:CODE:NC:RPL>CPL", EXCEPTION_GP,selector & 0xfffc))
                    return;
                if (CPU_CHECK_COND(call_4.DPL()!=cpu.cpl, "CALL:CODE:NC:DPL!=CPL", EXCEPTION_GP,selector & 0xfffc))
                    return;
                if (Log.level<=LogSeverities.LOG_NORMAL) Log.log(LogTypes.LOG_CPU,LogSeverities.LOG_NORMAL,"CALL:CODE:NC to "+Integer.toString(selector, 16)+":"+Long.toString(offset,16));
                //goto call_code;
                if (call_4.saved.seg.p()==0) {
                    // borland extender (RTM)
                    CPU_Exception(EXCEPTION_NP,selector & 0xfffc);
                    return;
                }
                esp = CPU_Regs.reg_esp.dword;
                // commit point
                if (!use32) {
                    esp = CPU_Push16(esp, CPU_Regs.reg_csVal.dword);
                    esp = CPU_Push16(esp, oldeip);
                    CPU_Regs.reg_eip=offset & 0xffff;
                } else {
                    esp = CPU_Push32(esp, CPU_Regs.reg_csVal.dword);
                    esp = CPU_Push32(esp, oldeip);
                    CPU_Regs.reg_eip=offset;
                }
                CPU_Regs.reg_esp.dword = esp; // don't set ESP until we are done with Memory Writes / CPU_Push so that we are reentrant
                CPU_Regs.reg_csPhys.dword=call_4.GetBase();
                cpu.code.big=call_4.Big()>0;
                CPU_Regs.reg_csVal.dword=(selector & 0xfffc) | cpu.cpl;
                return;
            case DESC_CODE_N_C_A:case DESC_CODE_N_C_NA:
            case DESC_CODE_R_C_A:case DESC_CODE_R_C_NA:
                if (CPU_CHECK_COND(call_4.DPL()>cpu.cpl, "CALL:CODE:C:DPL>CPL", EXCEPTION_GP,selector & 0xfffc))
                    return;
                if (Log.level<=LogSeverities.LOG_NORMAL) Log.log(LogTypes.LOG_CPU,LogSeverities.LOG_NORMAL,"CALL:CODE:C to "+Integer.toString(selector, 16)+":"+Long.toString(offset,16));
    //call_code:
                if (call_4.saved.seg.p()==0) {
                    // borland extender (RTM)
                    CPU_Exception(EXCEPTION_NP,selector & 0xfffc);
                    return;
                }
                esp = CPU_Regs.reg_esp.dword;
                // commit point
                if (!use32) {
                    esp = CPU_Push16(esp, CPU_Regs.reg_csVal.dword);
                    esp = CPU_Push16(esp, oldeip);
                    CPU_Regs.reg_eip=offset & 0xffff;
                } else {
                    esp = CPU_Push32(esp, CPU_Regs.reg_csVal.dword);
                    esp = CPU_Push32(esp, oldeip);
                    CPU_Regs.reg_eip=offset;
                }
                CPU_Regs.reg_esp.dword = esp; // don't set ESP until we are done with Memory Writes / CPU_Push so that we are reentrant
                CPU_Regs.reg_csPhys.dword=call_4.GetBase();
                cpu.code.big=call_4.Big()>0;
                CPU_Regs.reg_csVal.dword=(selector & 0xfffc) | cpu.cpl;
                return;
            case DESC_386_CALL_GATE:
            case DESC_286_CALL_GATE:
                {
                    if (CPU_CHECK_COND(call_4.DPL()<cpu.cpl, "CALL:Gate:Gate DPL<CPL", EXCEPTION_GP,selector & 0xfffc))
                        return;
                    if (CPU_CHECK_COND(call_4.DPL()<rpl, "CALL:Gate:Gate DPL<RPL", EXCEPTION_GP,selector & 0xfffc))
                        return;
                    if (CPU_CHECK_COND(call_4.saved.seg.p()==0, "CALL:Gate:Segment not present", EXCEPTION_NP,selector & 0xfffc))
                        return;

                     /*Bitu*/int n_cs_sel=call_4.GetSelector();

                    if (CPU_CHECK_COND((n_cs_sel & 0xfffc)==0, "CALL:Gate:CS selector zero", EXCEPTION_GP,0))
                        return;
                    success = cpu.gdt.GetDescriptor(n_cs_sel,n_cs_desc_4);
                    if (CPU_CHECK_COND(!success, "CALL:Gate:CS beyond limits", EXCEPTION_GP,n_cs_sel & 0xfffc))
                        return;
                     /*Bitu*/int n_cs_dpl	= n_cs_desc_4.DPL();
                    if (CPU_CHECK_COND(n_cs_dpl>cpu.cpl, "CALL:Gate:CS DPL>CPL", EXCEPTION_GP,n_cs_sel & 0xfffc))
                        return;
                    if (CPU_CHECK_COND(n_cs_desc_4.saved.seg.p()==0, "CALL:Gate:CS not present", EXCEPTION_NP,n_cs_sel & 0xfffc))
                        return;

                     /*Bitu*/int n_eip = (int)call_4.GetOffset();
                    switch (n_cs_desc_4.Type()) {
                    case DESC_CODE_N_NC_A:case DESC_CODE_N_NC_NA:
                    case DESC_CODE_R_NC_A:case DESC_CODE_R_NC_NA:
                        /* Check if we goto inner priviledge */
                        if (n_cs_dpl < cpu.cpl) {
                            /* Get new SS:ESP out of TSS */
                            cpu_tss.Get_SSx_ESPx(n_cs_dpl,n_ss_sel_4,n_esp_4);
                            if (CPU_CHECK_COND((n_ss_sel_4.value & 0xfffc)==0, "CALL:Gate:NC:SS selector zero", EXCEPTION_TS,0))
                                return;
                            success = cpu.gdt.GetDescriptor(n_ss_sel_4.value,n_ss_desc_4);
                            if (CPU_CHECK_COND(!success, "CALL:Gate:Invalid SS selector", EXCEPTION_TS,n_ss_sel_4.value & 0xfffc))
                                return;
                            if (CPU_CHECK_COND(((n_ss_sel_4.value & 3)!=n_cs_desc_4.DPL()) || (n_ss_desc_4.DPL()!=n_cs_desc_4.DPL()), "CALL:Gate:Invalid SS selector privileges", EXCEPTION_TS,n_ss_sel_4.value & 0xfffc))
                                return;

                            switch (n_ss_desc_4.Type()) {
                            case DESC_DATA_EU_RW_NA:		case DESC_DATA_EU_RW_A:
                            case DESC_DATA_ED_RW_NA:		case DESC_DATA_ED_RW_A:
                                // writable data segment
                                break;
                            default:
                                Log.exit("Call:Gate:SS no writable data segment");	// or #TS(ss_sel)
                            }
                            if (CPU_CHECK_COND(n_ss_desc_4.saved.seg.p()==0, "CALL:Gate:Stack segment not present", EXCEPTION_SS,n_ss_sel_4.value & 0xfffc))
                                return;

                            /* Load the new SS:ESP and save data on it */
                             /*Bitu*/int o_esp		= CPU_Regs.reg_esp.dword;
                             /*Bitu*/int o_ss		= CPU_Regs.reg_ssVal.dword;
                             /*PhysPt*/int o_stack  = CPU_Regs.reg_ssPhys.dword+(CPU_Regs.reg_esp.dword & cpu.stack.mask);


                            // catch pagefaults
                            int paramCount = call_4.saved.gate.paramcount() & 31;
                            paramCount+=4; //o_ss, o_esp, params, oldcs, oldip
                            if (call_4.Type()==DESC_386_CALL_GATE) {
                                for (/*Bits*/int i=paramCount-1;i>=0;i--)
                                    Memory.mem_readd(o_stack + i * 4);
                            } else {
                                for (/*Bits*/int i=paramCount-1;i>=0;i--)
                                    Memory.mem_readw(o_stack+i*2);
                            }

                            // commit point
                            CPU_Regs.reg_ssVal.dword=n_ss_sel_4.value;
                            CPU_Regs.reg_ssPhys.dword=n_ss_desc_4.GetBase();
                            if (n_ss_desc_4.Big()!=0) {
                                cpu.stack.big=true;
                                cpu.stack.mask=0xffffffff;
                                cpu.stack.notmask=0;
                                CPU_Regs.reg_esp.dword=n_esp_4.value;
                            } else {
                                cpu.stack.big=false;
                                cpu.stack.mask=0xffff;
                                cpu.stack.notmask=0xffff0000;
                                CPU_Regs.reg_esp.word((int)(n_esp_4.value & 0xffffl));
                            }

                            CPU_SetCPL(n_cs_desc_4.DPL());
                            /*Bit16u*/int oldcs    = CPU_Regs.reg_csVal.dword;
                            /* Switch to new CS:EIP */
                            CPU_Regs.reg_csPhys.dword=n_cs_desc_4.GetBase();
                            CPU_Regs.reg_csVal.dword	= (n_cs_sel & 0xfffc) | cpu.cpl;
                            cpu.code.big	= n_cs_desc_4.Big()>0;
                            CPU_Regs.reg_eip=n_eip;
                            if (!use32)	CPU_Regs.reg_eip=CPU_Regs.reg_eip & 0xffff;

                            if (call_4.Type()==DESC_386_CALL_GATE) {
                                CPU_Push32(o_ss);		//save old stack
                                CPU_Push32(o_esp);
                                if ((call_4.saved.gate.paramcount() & 31)!= 0)
                                    for (/*Bits*/int i=(call_4.saved.gate.paramcount() & 31)-1;i>=0;i--)
                                        CPU_Push32(Memory.mem_readd(o_stack + i * 4));
                                CPU_Push32(oldcs);
                                CPU_Push32(oldeip);
                            } else {
                                CPU_Push16(o_ss);		//save old stack
                                CPU_Push16(o_esp & 0xFFFF);
                                if ((call_4.saved.gate.paramcount() & 31)!=0)
                                    for (/*Bits*/int i=(call_4.saved.gate.paramcount() & 31)-1;i>=0;i--)
                                        CPU_Push16(Memory.mem_readw(o_stack+i*2));
                                CPU_Push16(oldcs);
                                CPU_Push16(oldeip & 0xFFFF);
                            }

                            break;
                        } else if (n_cs_dpl > cpu.cpl)
                            Log.exit("CALL:GATE:CS DPL>CPL");		// or #GP(sel)
                    case DESC_CODE_N_C_A:case DESC_CODE_N_C_NA:
                    case DESC_CODE_R_C_A:case DESC_CODE_R_C_NA:
                        // zrdx extender

                        esp = CPU_Regs.reg_esp.dword;
                        if (call_4.Type()==DESC_386_CALL_GATE) {
                            esp = CPU_Push32(esp, CPU_Regs.reg_csVal.dword);
                            esp = CPU_Push32(esp, oldeip);
                        } else {
                            esp = CPU_Push16(esp, CPU_Regs.reg_csVal.dword);
                            esp = CPU_Push16(esp, oldeip & 0xFFFF);
                        }
                        CPU_Regs.reg_esp.dword = esp; // don't set ESP until we are done with Memory Writes / CPU_Push so that we are reentrant

                        /* Switch to new CS:EIP */
                        CPU_Regs.reg_csPhys.dword = n_cs_desc_4.GetBase();
                        CPU_Regs.reg_csVal.dword	= (n_cs_sel & 0xfffc) | cpu.cpl;
                        cpu.code.big = n_cs_desc_4.Big()>0;
                        CPU_Regs.reg_eip=n_eip;
                        if (!use32)	CPU_Regs.reg_eip=CPU_Regs.reg_eip & 0xffff;
                        break;
                    default:
                        Log.exit("CALL:GATE:CS no executable segment");
                    }
                }			/* Call Gates */
                break;
            case DESC_386_TSS_A:
                if (CPU_CHECK_COND(call_4.DPL()<cpu.cpl, "CALL:TSS:dpl<cpl", EXCEPTION_GP,selector & 0xfffc))
                    return;
                if (CPU_CHECK_COND(call_4.DPL()<rpl, "CALL:TSS:dpl<rpl", EXCEPTION_GP,selector & 0xfffc))
                    return;
                if (CPU_CHECK_COND(call_4.saved.seg.p()==0, "CALL:TSS:Segment not present", EXCEPTION_NP,selector & 0xfffc))
                    return;

                if (Log.level<=LogSeverities.LOG_NORMAL) Log.log(LogTypes.LOG_CPU,LogSeverities.LOG_NORMAL,"CALL:TSS to "+Integer.toString(selector,16));
                CPU_SwitchTask(selector,TSwitchType.TSwitch_CALL_INT,oldeip);
                break;
            case DESC_DATA_EU_RW_NA:	// vbdos
            case DESC_INVALID:			// used by some installers
                CPU_Exception(EXCEPTION_GP,selector & 0xfffc);
                return;
            default:
                Log.exit("CALL:Descriptor type "+Integer.toString(call_4.Type(), 16)+" unsupported");
            }
        }
    }

    static final private Descriptor desc_5 = new Descriptor();
    static final private Descriptor n_ss_desc_5 = new Descriptor();
    static public void CPU_RET(boolean use32, /*Bitu*/int bytes, /*Bitu*/int oldeip) {
        if (!cpu.pmode || (CPU_Regs.flags & CPU_Regs.VM) != 0) {
             /*Bitu*/int new_ip;
            int new_cs;
            if (!use32) {
                new_ip=CPU_Pop16();
                new_cs=CPU_Pop16();
            } else {
                new_ip=CPU_Pop32();
                new_cs=CPU_Pop32() & 0xffff;
            }
            CPU_Regs.reg_esp.dword+=bytes;
            CPU_Regs.SegSet16CS(new_cs);
            CPU_Regs.reg_eip=new_ip;
            cpu.code.big=false;
        } else {
             /*Bitu*/int offset,selector;
            if (!use32) selector = Memory.mem_readw(CPU_Regs.reg_ssPhys.dword + (CPU_Regs.reg_esp.dword & cpu.stack.mask) + 2);
            else selector = (Memory.mem_readd(CPU_Regs.reg_ssPhys.dword + (CPU_Regs.reg_esp.dword & cpu.stack.mask) + 4) & 0xffff);

             /*Bitu*/int rpl=selector & 3;
            if(rpl < cpu.cpl) {
                // win setup
                CPU_Exception(EXCEPTION_GP,selector & 0xfffc);
                return;
            }

            if (CPU_CHECK_COND((selector & 0xfffc)==0, "RET:CS selector zero", EXCEPTION_GP,0))
                return;
            boolean success = cpu.gdt.GetDescriptor(selector,desc_5);
            if (CPU_CHECK_COND(!success, "RET:CS beyond limits", EXCEPTION_GP,selector & 0xfffc))
                return;

            if (cpu.cpl==rpl) {
                /* Return to same level */
                switch (desc_5.Type()) {
                case DESC_CODE_N_NC_A:case DESC_CODE_N_NC_NA:
                case DESC_CODE_R_NC_A:case DESC_CODE_R_NC_NA:
                    if (CPU_CHECK_COND(cpu.cpl!=desc_5.DPL(), "RET to NC segment of other privilege", EXCEPTION_GP,selector & 0xfffc))
                        return;
                    // goto RET_same_level;
                    if (desc_5.saved.seg.p()==0) {
                        // borland extender (RTM)
                        CPU_Exception(EXCEPTION_NP,selector & 0xfffc);
                        return;
                    }

                    // commit point
                    if (!use32) {
                        offset=CPU_Pop16();
                        selector=CPU_Pop16();
                    } else {
                        offset=CPU_Pop32();
                        selector=CPU_Pop32() & 0xffff;
                    }

                    CPU_Regs.reg_csPhys.dword=desc_5.GetBase();
                    cpu.code.big=desc_5.Big()>0;
                    CPU_Regs.reg_csVal.dword=selector;
                    CPU_Regs.reg_eip=offset;
                    if (cpu.stack.big) {
                        CPU_Regs.reg_esp.dword+=bytes;
                    } else {
                        CPU_Regs.reg_esp.word(CPU_Regs.reg_esp.word()+bytes);
                    }
                    //Log.log(LogTypes.LOG_CPU,LogSeverities.LOG_NORMAL,"RET - Same level to %X:%X RPL %X DPL %X",selector,offset,rpl,desc.DPL());
                    return;
                case DESC_CODE_N_C_A:case DESC_CODE_N_C_NA:
                case DESC_CODE_R_C_A:case DESC_CODE_R_C_NA:
                    if (CPU_CHECK_COND(desc_5.DPL()>cpu.cpl, "RET to C segment of higher privilege", EXCEPTION_GP,selector & 0xfffc))
                        return;
                    break;
                default:
                    Log.exit("RET from illegal descriptor type "+Integer.toString(desc_5.Type(),16));
                }
    //RET_same_level:
                if (desc_5.saved.seg.p()==0) {
                    // borland extender (RTM)
                    CPU_Exception(EXCEPTION_NP,selector & 0xfffc);
                    return;
                }

                // commit point
                if (!use32) {
                    offset=CPU_Pop16();
                    selector=CPU_Pop16();
                } else {
                    offset=CPU_Pop32();
                    selector=CPU_Pop32() & 0xffff;
                }

                CPU_Regs.reg_csPhys.dword=desc_5.GetBase();
                cpu.code.big=desc_5.Big()>0;
                CPU_Regs.reg_csVal.dword=selector;
                CPU_Regs.reg_eip=offset;
                if (cpu.stack.big) {
                    CPU_Regs.reg_esp.dword+=bytes;
                } else {
                    CPU_Regs.reg_esp.word(CPU_Regs.reg_esp.word()+bytes);
                }
                //Log.log(LogTypes.LOG_CPU,LogSeverities.LOG_NORMAL,"RET - Same level to %X:%X RPL %X DPL %X",selector,offset,rpl,desc.DPL());
            } else {
                /* Return to outer level */
                switch (desc_5.Type()) {
                case DESC_CODE_N_NC_A:case DESC_CODE_N_NC_NA:
                case DESC_CODE_R_NC_A:case DESC_CODE_R_NC_NA:
                    if (CPU_CHECK_COND(desc_5.DPL()!=rpl, "RET to outer NC segment with DPL!=RPL", EXCEPTION_GP,selector & 0xfffc))
                        return;
                    break;
                case DESC_CODE_N_C_A:case DESC_CODE_N_C_NA:
                case DESC_CODE_R_C_A:case DESC_CODE_R_C_NA:
                    if (CPU_CHECK_COND(desc_5.DPL()>rpl, "RET to outer C segment with DPL>RPL", EXCEPTION_GP,selector & 0xfffc))
                        return;
                    break;
                default:
                    Log.exit("RET from illegal descriptor type "+Integer.toString(desc_5.Type(),16));		// or #GP(selector)
                }

                if (CPU_CHECK_COND(desc_5.saved.seg.p()==0, "RET:Outer level:CS not present", EXCEPTION_NP,selector & 0xfffc))
                    return;

                // commit point
                 /*Bitu*/int n_ss;
                int n_esp;
                if (use32) {
                    offset=CPU_Pop32();
                    selector=CPU_Pop32() & 0xffff;
                    CPU_Regs.reg_esp.dword+=bytes;
                    n_esp = CPU_Pop32();
                    n_ss = CPU_Pop32() & 0xffff;
                } else {
                    offset=CPU_Pop16();
                    selector=CPU_Pop16();
                    CPU_Regs.reg_esp.dword+=bytes;
                    n_esp = CPU_Pop16();
                    n_ss = CPU_Pop16();
                }

                if (CPU_CHECK_COND((n_ss & 0xfffc)==0, "RET to outer level with SS selector zero", EXCEPTION_GP,0))
                    return;

                boolean sucess = cpu.gdt.GetDescriptor(n_ss,n_ss_desc_5);
                if (CPU_CHECK_COND(!success, "RET:SS beyond limits", EXCEPTION_GP,n_ss & 0xfffc))
                    return;
                if (CPU_CHECK_COND(((n_ss & 3)!=rpl) || (n_ss_desc_5.DPL()!=rpl), "RET to outer segment with invalid SS privileges", EXCEPTION_GP,n_ss & 0xfffc))
                    return;

                switch (n_ss_desc_5.Type()) {
                case DESC_DATA_EU_RW_NA:		case DESC_DATA_EU_RW_A:
                case DESC_DATA_ED_RW_NA:		case DESC_DATA_ED_RW_A:
                    break;
                default:
                    Log.exit("RET:SS selector type no writable data segment");	// or #GP(selector)
                }
                if (CPU_CHECK_COND(n_ss_desc_5.saved.seg.p()==0, "RET:Stack segment not present", EXCEPTION_SS,n_ss & 0xfffc))
                    return;

                CPU_SetCPL(rpl);
                CPU_Regs.reg_csPhys.dword=desc_5.GetBase();
                cpu.code.big=desc_5.Big()>0;
                CPU_Regs.reg_csVal.dword=(selector&0xfffc) | cpu.cpl;
                CPU_Regs.reg_eip=offset;

                CPU_Regs.reg_ssVal.dword=n_ss;
                CPU_Regs.reg_ssPhys.dword=n_ss_desc_5.GetBase();
                if (n_ss_desc_5.Big()!=0) {
                    cpu.stack.big=true;
                    cpu.stack.mask=0xffffffff;
                    cpu.stack.notmask=0;
                    CPU_Regs.reg_esp.dword=n_esp+bytes;
                } else {
                    cpu.stack.big=false;
                    cpu.stack.mask=0xffff;
                    cpu.stack.notmask=0xffff0000;
                    CPU_Regs.reg_esp.word((n_esp & 0xffff)+bytes);
                }

                CPU_CheckSegments();

//			LOG(LOG_MISC,LOG_ERROR)("RET - Higher level to %X:%X RPL %X DPL %X",selector,offset,rpl,desc.DPL());
//                return;
            }
            //Log.log(LogTypes.LOG_CPU,LogSeverities.LOG_NORMAL,"Prot ret %X:%X",selector,offset);
            //return;
        }
        //throw new RuntimeException();
    }


    static public/*Bitu*/int CPU_SLDT() {
        return cpu.gdt.SLDT();
    }

    static public void CPU_SetCPL(/*Bitu*/int newcpl) {
        if (newcpl != cpu.cpl) {
            if (Paging.enabled) {
                if ( ((cpu.cpl < 3) && (newcpl == 3)) || ((cpu.cpl == 3) && (newcpl < 3)) )
                Paging.PAGING_SwitchCPL(newcpl == 3);
            }
            cpu.cpl = newcpl;
        }
    }

    static public boolean CPU_LLDT( /*Bitu*/int selector) {
        if (!cpu.gdt.LLDT(selector)) {
            if (Log.level<=LogSeverities.LOG_ERROR) Log.log(LogTypes.LOG_CPU,LogSeverities.LOG_ERROR,"LLDT failed, selector="+Integer.toString(selector,16));
            return true;
        }
        if (Log.level<=LogSeverities.LOG_NORMAL) Log.log(LogTypes.LOG_CPU,LogSeverities.LOG_NORMAL,"LDT Set to "+Integer.toString(selector, 16));
        return false;
    }

    static public /*Bitu*/int CPU_STR() {
        return cpu_tss.selector;
    }

    static final private TSS_Descriptor desc_6 = new TSS_Descriptor();
    static public boolean CPU_LTR( /*Bitu*/int selector) {
        if ((selector & 0xfffc)==0) {
            cpu_tss.SetSelector(selector);
            return false;
        }

        if ((selector & 4) != 0 || (!cpu.gdt.GetDescriptor(selector,desc_6))) {
            if (Log.level<=LogSeverities.LOG_ERROR) Log.log(LogTypes.LOG_CPU,LogSeverities.LOG_ERROR,"LTR failed, selector="+Integer.toString(selector,16));
            return CPU_PrepareException(EXCEPTION_GP,selector);
        }

        if ((desc_6.Type()==DESC_286_TSS_A) || (desc_6.Type()==DESC_386_TSS_A)) {
            if (desc_6.saved.seg.p()==0) {
                if (Log.level<=LogSeverities.LOG_ERROR) Log.log(LogTypes.LOG_CPU,LogSeverities.LOG_ERROR,"LTR failed, selector="+Integer.toString(selector, 16)+" (not present)");
                return CPU_PrepareException(EXCEPTION_NP,selector);
            }
            if (!cpu_tss.SetSelector(selector)) Log.exit("LTR failed, selector="+Integer.toString(selector,16));
            cpu_tss.desc.SetBusy(true);
            cpu_tss.SaveSelector();
        } else {
            /* Descriptor was no available TSS descriptor */
            if (Log.level<=LogSeverities.LOG_NORMAL) Log.log(LogTypes.LOG_CPU,LogSeverities.LOG_NORMAL,"LTR failed, selector="+Integer.toString(selector, 16)+" (type="+Integer.toString(desc_6.Type(), 16)+")");
            return CPU_PrepareException(EXCEPTION_GP,selector);
        }
        return false;
    }

    static public void CPU_LGDT( /*Bitu*/int limit, /*Bitu*/int base) {
        if (Log.level<=LogSeverities.LOG_NORMAL) Log.log(LogTypes.LOG_CPU,LogSeverities.LOG_NORMAL,"GDT Set to base:"+Long.toString(base, 16)+" limit:"+Integer.toString(limit,16));
        cpu.gdt.SetLimit(limit);
        cpu.gdt.SetBase(base);
    }

    static public void CPU_LIDT( /*Bitu*/int limit, /*Bitu*/int base) {
        if (Log.level<=LogSeverities.LOG_NORMAL) Log.log(LogTypes.LOG_CPU,LogSeverities.LOG_NORMAL,"IDT Set to base:"+Long.toString(base, 16)+" limit:"+Integer.toString(limit,16));
        cpu.idt.SetLimit(limit);
        cpu.idt.SetBase(base);
    }

    static public /*Bitu*/int CPU_SGDT_base() {
        return cpu.gdt.GetBase();
    }
    static public /*Bitu*/int CPU_SGDT_limit() {
        return cpu.gdt.GetLimit();
    }

    static public /*Bitu*/int CPU_SIDT_base() {
        return cpu.idt.GetBase();
    }
    static public /*Bitu*/int CPU_SIDT_limit() {
        return cpu.idt.GetLimit();
    }

    static boolean printed_cycles_auto_info = false;
    static public void CPU_SET_CRX( /*Bitu*/int cr, /*Bitu*/int value) {
        switch (cr) {
        case 0:
            {
                value|=CR0_FPUPRESENT;
                /*Bitu*/long changed=cpu.cr0 ^ value;
                if (changed==0) return;
                if ((changed & CR0_WRITEPROTECT)!=0) {
			    	if (CPU_ArchitectureType >= CPU_ARCHTYPE_486OLD)
				    	Paging.PAGING_SetWP((value & CR0_WRITEPROTECT) != 0);
    			}
                cpu.cr0=value;
                if ((value & CR0_PROTECTION)!=0) {
                    cpu.pmode=true;
                    if (Log.level<=LogSeverities.LOG_NORMAL) Log.log(LogTypes.LOG_CPU,LogSeverities.LOG_NORMAL,"Protected mode");
                    Paging.PAGING_Enable((value & CR0_PAGING)!=0);

                    if ((CPU_AutoDetermineMode & CPU_AUTODETERMINE_MASK)==0) break;

                    if ((CPU_AutoDetermineMode & CPU_AUTODETERMINE_CYCLES) != 0) {
                        CPU_CycleAutoAdjust=true;
                        CPU_CycleLeft=0;
                        CPU_Cycles=0;
                        CPU_OldCycleMax=CPU_CycleMax;
                        Main.GFX_SetTitle(CPU_CyclePercUsed,-1,false);
                        if(!printed_cycles_auto_info) {
                            printed_cycles_auto_info = true;
                            Log.log_msg("DOSBox switched to max cycles, because of the setting: cycles=auto. If the game runs too fast try a fixed cycles amount in DOSBox's options.");
                        }
                    } else {
                        Main.GFX_SetTitle(-1,-1,false);
                    }
                    if (Config.C_DYNAMIC) {
                        if ((CPU_AutoDetermineMode & CPU_AUTODETERMINE_CORE) !=0 ) {
                            Core_dynamic.CPU_Core_Dynamic_Cache_Init(true);
                            cpudecoder= Core_dynamic.CPU_Core_Dynamic_Run;
                        }
                    }
                    CPU_AutoDetermineMode<<=CPU_AUTODETERMINE_SHIFT;
                } else {
                    cpu.pmode=false;
                    if ((value & CR0_PAGING)!=0) Log.log_msg("Paging requested without PE=1");
                    Paging.PAGING_Enable(false);
                    if (Log.level<=LogSeverities.LOG_NORMAL) Log.log(LogTypes.LOG_CPU,LogSeverities.LOG_NORMAL,"Real mode");
                }
                break;
            }
        case 2:
            Paging.cr2=value;
            break;
        case 3:
            Paging.PAGING_SetDirBase(value);
            break;
        case 4:
            if (cpu.cr4 == value)
                return;
            cpu.cr4 = value;
            cpu.cr4 = (cpu.cr4 & ~0x5f) | (value & 0x5f);
            if ((cpu.cr4 & CR4_VIRTUAL8086_MODE_EXTENSIONS) != 0)
                if (Log.level<=LogSeverities.LOG_WARN) Log.log(LogTypes.LOG_CPU,LogSeverities.LOG_WARN,"Virtual-8086 mode extensions enabled in the processor");
            if ((cpu.cr4 & CR4_PROTECTED_MODE_VIRTUAL_INTERRUPTS) != 0)
                if (Log.level<=LogSeverities.LOG_WARN) Log.log(LogTypes.LOG_CPU,LogSeverities.LOG_WARN,"Protected mode virtual interrupts enabled in the processor");
            if ((cpu.cr4 & CR4_OS_SUPPORT_UNMASKED_SIMD_EXCEPTIONS) != 0)
                if (Log.level<=LogSeverities.LOG_WARN) Log.log(LogTypes.LOG_CPU,LogSeverities.LOG_WARN,"SIMD instruction support modified in the processor");
            if ((cpu.cr4 & CR4_OS_SUPPORT_FXSAVE_FXSTORE) != 0)
                if (Log.level<=LogSeverities.LOG_WARN) Log.log(LogTypes.LOG_CPU,LogSeverities.LOG_WARN,"FXSave and FXRStore enabled in the processor");
            if ((cpu.cr4 & CR4_DEBUGGING_EXTENSIONS) != 0)
                if (Log.level<=LogSeverities.LOG_NORMAL) Log.log(LogTypes.LOG_CPU,LogSeverities.LOG_WARN,"Debugging extensions enabled");
            if ((cpu.cr4 & CR4_TIME_STAMP_DISABLE) != 0)
                if (Log.level<=LogSeverities.LOG_WARN) Log.log(LogTypes.LOG_CPU,LogSeverities.LOG_WARN,"Timestamp restricted to CPL0");
            if ((cpu.cr4 & CR4_PHYSICAL_ADDRESS_EXTENSION) != 0) {
                Log.exit("36-bit addressing enabled");
            }
            Paging.PAGING_EnableGlobal((cpu.cr4 & CR4_PAGE_GLOBAL_ENABLE)!=0);
            break;
        default:
            if (Log.level<=LogSeverities.LOG_ERROR) Log.log(LogTypes.LOG_CPU,LogSeverities.LOG_ERROR,"Unhandled MOV CR"+cr+","+Integer.toString(value, 16));
            break;
        }
    }

    public static boolean CPU_WRITE_CRX( /*Bitu*/int cr, /*Bitu*/int value) {
        /* Check if privileged to access control registers */
        if (cpu.pmode && (cpu.cpl>0)) return CPU_PrepareException(EXCEPTION_GP,0);
        if ((cr==1) || (cr>4)) return CPU_PrepareException(EXCEPTION_UD,0);
        if (CPU_ArchitectureType<CPU_ARCHTYPE_486OLD) {
            if (cr==4) return CPU_PrepareException(EXCEPTION_UD,0);
        }
        CPU_SET_CRX(cr,value);
        return false;
    }

     public static /*Bitu*/int CPU_GET_CRX( /*Bitu*/int cr) {
        switch (cr) {
        case 0:
            if (CPU_ArchitectureType>=CPU_ARCHTYPE_PENTIUM) return cpu.cr0;
            else if (CPU_ArchitectureType>=CPU_ARCHTYPE_486OLD) return (cpu.cr0 & 0xe005003f);
            else return (cpu.cr0 | 0x7ffffff0);
        case 2:
            return Paging.cr2;
        case 3:
            return Paging.PAGING_GetDirBase() & 0xfffff000;
        case 4:
            return cpu.cr4;
        default:
            if (Log.level<=LogSeverities.LOG_ERROR) Log.log(LogTypes.LOG_CPU,LogSeverities.LOG_ERROR,"Unhandled MOV XXX, CR"+cr);
            break;
        }
        return 0;
    }

    public static boolean CPU_READ_CRX( /*Bitu*/int cr, CPU_Regs.Reg retvalue) {
        /* Check if privileged to access control registers */
        if (cpu.pmode && (cpu.cpl>0)) return CPU_PrepareException(EXCEPTION_GP,0);
        if ((cr==1) || (cr>4)) return CPU_PrepareException(EXCEPTION_UD,0);
        retvalue.dword=CPU_GET_CRX(cr);
        return false;
    }


    public static boolean CPU_WRITE_DRX( /*Bitu*/int dr, /*Bitu*/int value) {
        /* Check if privileged to access control registers */
        if (cpu.pmode && (cpu.cpl>0)) return CPU_PrepareException(EXCEPTION_GP,0);
        switch (dr) {
        case 0:
        case 1:
        case 2:
        case 3:
            cpu.drx[dr]=value;
            break;
        case 4:
        case 6:
            cpu.drx[6]=(value|0xffff0ff0) & 0xffffefff;
            break;
        case 5:
        case 7:
            if (CPU_ArchitectureType<CPU_ARCHTYPE_PENTIUM) {
                cpu.drx[7]=(value|0x400) & 0xffff2fff;
            } else {
                cpu.drx[7]=(value|0x400);
            }
            break;
        default:
            if (Log.level<=LogSeverities.LOG_ERROR) Log.log(LogTypes.LOG_CPU,LogSeverities.LOG_ERROR,"Unhandled MOV DR"+dr+","+Integer.toString(value,16));
            break;
        }
        return false;
    }

    public static boolean CPU_READ_DRX( /*Bitu*/int dr, CPU_Regs.Reg retvalue) {
        /* Check if privileged to access control registers */
        if (cpu.pmode && (cpu.cpl>0)) return CPU_PrepareException(EXCEPTION_GP,0);
        switch (dr) {
        case 0:
        case 1:
        case 2:
        case 3:
        case 6:
        case 7:
            retvalue.dword=cpu.drx[dr];
            break;
        case 4:
            retvalue.dword=cpu.drx[6];
            break;
        case 5:
            retvalue.dword=cpu.drx[7];
            break;
        default:
            if (Log.level<=LogSeverities.LOG_ERROR) Log.log(LogTypes.LOG_CPU,LogSeverities.LOG_ERROR,"Unhandled MOV XXX, DR"+dr);
            retvalue.dword=0;
            break;
        }
        return false;
    }

    public static boolean CPU_WRITE_TRX( /*Bitu*/int tr, /*Bitu*/int value) {
        /* Check if privileged to access control registers */
        if (cpu.pmode && (cpu.cpl>0)) return CPU_PrepareException(EXCEPTION_GP,0);
        switch (tr) {
//	case 3:
        case 6:
        case 7:
            cpu.trx[tr]=value;
            return false;
        default:
            if (Log.level<=LogSeverities.LOG_ERROR) Log.log(LogTypes.LOG_CPU,LogSeverities.LOG_ERROR,"Unhandled MOV TR"+tr+","+Long.toString(value,16));
            break;
        }
        return CPU_PrepareException(EXCEPTION_UD,0);
    }

    static public boolean CPU_READ_TRX( /*Bitu*/int tr, CPU_Regs.Reg retvalue) {
        /* Check if privileged to access control registers */
        if (cpu.pmode && (cpu.cpl>0)) return CPU_PrepareException(EXCEPTION_GP,0);
        switch (tr) {
//	case 3:
        case 6:
        case 7:
            retvalue.dword=cpu.trx[tr];
            return false;
        default:
            if (Log.level<=LogSeverities.LOG_ERROR) Log.log(LogTypes.LOG_CPU,LogSeverities.LOG_ERROR,"Unhandled MOV XXX, TR"+tr);
            break;
        }
        return CPU_PrepareException(EXCEPTION_UD,0);
    }


    public static /*Bitu*/int CPU_SMSW() {
        return cpu.cr0;
    }

    public static boolean CPU_LMSW( /*Bitu*/int word) {
        if (cpu.pmode && (cpu.cpl>0)) return CPU_PrepareException(EXCEPTION_GP,0);
        word&=0xf;
        if ((cpu.cr0 & 1)!=0) word|=1;
        word|=(cpu.cr0&0xfffffff0l);
        CPU_SET_CRX(0,word);
        return false;
    }

    public static int CPU_ARPL( /*Bitu*/int dest_sel, /*Bitu*/int src_sel) {
        Flags.FillFlags();
        if ((dest_sel & 3) < (src_sel & 3)) {
            dest_sel=(dest_sel & 0xfffc) + (src_sel & 3);
//		dest_sel|=0xff3f0000;
            CPU_Regs.SETFLAGBIT(CPU_Regs.ZF,true);
        } else {
            CPU_Regs.SETFLAGBIT(CPU_Regs.ZF,false);
        }
        return dest_sel;
    }

    static final private Descriptor desc_17 = new Descriptor();
    public static int CPU_LAR( /*Bitu*/int selector, /*Bitu*/int ar) {
        Flags.FillFlags();
        if (selector == 0) {
            CPU_Regs.SETFLAGBIT(CPU_Regs.ZF,false);
            return ar;
        }
        /*Bitu*/int rpl=selector & 3;
        if (!cpu.gdt.GetDescriptor(selector,desc_17)){
            CPU_Regs.SETFLAGBIT(CPU_Regs.ZF,false);
            return ar;
        }
        switch (desc_17.Type()){
        case DESC_CODE_N_C_A:	case DESC_CODE_N_C_NA:
        case DESC_CODE_R_C_A:	case DESC_CODE_R_C_NA:
            break;

        case DESC_286_INT_GATE:		case DESC_286_TRAP_GATE:
        case DESC_386_INT_GATE:		case DESC_386_TRAP_GATE:
            CPU_Regs.SETFLAGBIT(CPU_Regs.ZF,false);
            return ar;


        case DESC_LDT:
        case DESC_TASK_GATE:

        case DESC_286_TSS_A:		case DESC_286_TSS_B:
        case DESC_286_CALL_GATE:

        case DESC_386_TSS_A:		case DESC_386_TSS_B:
        case DESC_386_CALL_GATE:


        case DESC_DATA_EU_RO_NA:	case DESC_DATA_EU_RO_A:
        case DESC_DATA_EU_RW_NA:	case DESC_DATA_EU_RW_A:
        case DESC_DATA_ED_RO_NA:	case DESC_DATA_ED_RO_A:
        case DESC_DATA_ED_RW_NA:	case DESC_DATA_ED_RW_A:
        case DESC_CODE_N_NC_A:		case DESC_CODE_N_NC_NA:
        case DESC_CODE_R_NC_A:		case DESC_CODE_R_NC_NA:
            if (desc_17.DPL()<cpu.cpl || desc_17.DPL() < rpl) {
                CPU_Regs.SETFLAGBIT(CPU_Regs.ZF,false);
                return ar;
            }
            break;
        default:
            CPU_Regs.SETFLAGBIT(CPU_Regs.ZF,false);
            return ar;
        }
        /* Valid descriptor */
        ar = desc_17.saved.get_fill(1) & 0x00ffff00;
        CPU_Regs.SETFLAGBIT(CPU_Regs.ZF,true);
        return ar;
    }

    private static final Descriptor desc_7 = new Descriptor();
    public static int CPU_LSL( /*Bitu*/int selector, /*Bitu*/int limit) {
        Flags.FillFlags();
        if (selector == 0) {
            CPU_Regs.SETFLAGBIT(CPU_Regs.ZF,false);
            return limit;
        }
        /*Bitu*/int rpl=selector & 3;
        if (!cpu.gdt.GetDescriptor(selector,desc_7)){
            CPU_Regs.SETFLAGBIT(CPU_Regs.ZF,false);
            return limit;
        }
        switch (desc_7.Type()){
        case DESC_CODE_N_C_A:	case DESC_CODE_N_C_NA:
        case DESC_CODE_R_C_A:	case DESC_CODE_R_C_NA:
            break;

        case DESC_LDT:
        case DESC_286_TSS_A:
        case DESC_286_TSS_B:

        case DESC_386_TSS_A:
        case DESC_386_TSS_B:

        case DESC_DATA_EU_RO_NA:	case DESC_DATA_EU_RO_A:
        case DESC_DATA_EU_RW_NA:	case DESC_DATA_EU_RW_A:
        case DESC_DATA_ED_RO_NA:	case DESC_DATA_ED_RO_A:
        case DESC_DATA_ED_RW_NA:	case DESC_DATA_ED_RW_A:

        case DESC_CODE_N_NC_A:		case DESC_CODE_N_NC_NA:
        case DESC_CODE_R_NC_A:		case DESC_CODE_R_NC_NA:
            if (desc_7.DPL()<cpu.cpl || desc_7.DPL() < rpl) {
                CPU_Regs.SETFLAGBIT(CPU_Regs.ZF,false);
                return limit;
            }
            break;
        default:
            CPU_Regs.SETFLAGBIT(CPU_Regs.ZF,false);
            return limit;
        }
        limit=(int)desc_7.GetLimit();
        CPU_Regs.SETFLAGBIT(CPU_Regs.ZF,true);
        return limit;
    }

    private static final Descriptor desc_9 = new Descriptor();
    public static void CPU_VERR( /*Bitu*/int selector) {
        Flags.FillFlags();
        if (selector == 0) {
            CPU_Regs.SETFLAGBIT(CPU_Regs.ZF,false);
            return;
        }
        /*Bitu*/int rpl=selector & 3;
        if (!cpu.gdt.GetDescriptor(selector,desc_9)){
            CPU_Regs.SETFLAGBIT(CPU_Regs.ZF,false);
            return;
        }
        switch (desc_9.Type()){
        case DESC_CODE_R_C_A:		case DESC_CODE_R_C_NA:
            //Conforming readable code segments can be always read
            break;
        case DESC_DATA_EU_RO_NA:	case DESC_DATA_EU_RO_A:
        case DESC_DATA_EU_RW_NA:	case DESC_DATA_EU_RW_A:
        case DESC_DATA_ED_RO_NA:	case DESC_DATA_ED_RO_A:
        case DESC_DATA_ED_RW_NA:	case DESC_DATA_ED_RW_A:

        case DESC_CODE_R_NC_A:		case DESC_CODE_R_NC_NA:
            if (desc_9.DPL()<cpu.cpl || desc_9.DPL() < rpl) {
                CPU_Regs.SETFLAGBIT(CPU_Regs.ZF,false);
                return;
            }
            break;
        default:
            CPU_Regs.SETFLAGBIT(CPU_Regs.ZF,false);
            return;
        }
        CPU_Regs.SETFLAGBIT(CPU_Regs.ZF,true);
    }

    private static final Descriptor desc_10=new Descriptor();
    public static void CPU_VERW( /*Bitu*/int selector) {
        Flags.FillFlags();
        if (selector == 0) {
            CPU_Regs.SETFLAGBIT(CPU_Regs.ZF,false);
            return;
        }
        /*Bitu*/int rpl=selector & 3;
        if (!cpu.gdt.GetDescriptor(selector,desc_10)){
            CPU_Regs.SETFLAGBIT(CPU_Regs.ZF,false);
            return;
        }
        switch (desc_10.Type()){
        case DESC_DATA_EU_RW_NA:	case DESC_DATA_EU_RW_A:
        case DESC_DATA_ED_RW_NA:	case DESC_DATA_ED_RW_A:
            if (desc_10.DPL()<cpu.cpl || desc_10.DPL() < rpl) {
                CPU_Regs.SETFLAGBIT(CPU_Regs.ZF,false);
                return;
            }
            break;
        default:
            CPU_Regs.SETFLAGBIT(CPU_Regs.ZF,false);
            return;
        }
        CPU_Regs.SETFLAGBIT(CPU_Regs.ZF,true);
    }

    static final private Descriptor desc_11 = new Descriptor();
    static public boolean CPU_SetSegGeneralES(/*Bitu*/int value) {
        value &= 0xffff;
        if (!cpu.pmode || (CPU_Regs.flags & CPU_Regs.VM) != 0) {
            CPU_Regs.SegSet16ES(value);
            return false;
        } else {

            if ((value & 0xfffc)==0) {
                CPU_Regs.reg_esVal.dword=value;
                CPU_Regs.reg_esPhys.dword=0;	// ??
                return false;
            }
            if (!cpu.gdt.GetDescriptor(value,desc_11)) {
                return CPU_PrepareException(EXCEPTION_GP,value & 0xfffc);
            }
            switch (desc_11.Type()) {
            case DESC_DATA_EU_RO_NA:		case DESC_DATA_EU_RO_A:
            case DESC_DATA_EU_RW_NA:		case DESC_DATA_EU_RW_A:
            case DESC_DATA_ED_RO_NA:		case DESC_DATA_ED_RO_A:
            case DESC_DATA_ED_RW_NA:		case DESC_DATA_ED_RW_A:
            case DESC_CODE_R_NC_A:			case DESC_CODE_R_NC_NA:
                if (((value & 3)>desc_11.DPL()) || (cpu.cpl>desc_11.DPL())) {
                    // extreme pinball
                    return CPU_PrepareException(EXCEPTION_GP,value & 0xfffc);
                }
                break;
            case DESC_CODE_R_C_A:			case DESC_CODE_R_C_NA:
                break;
            default:
                // gabriel knight
                return CPU_PrepareException(EXCEPTION_GP,value & 0xfffc);

            }
            if (desc_11.saved.seg.p()==0) {
                // win
                return CPU_PrepareException(EXCEPTION_NP,value & 0xfffc);
            }

            CPU_Regs.reg_esVal.dword=value;
            CPU_Regs.reg_esPhys.dword=desc_11.GetBase();
            return false;
        }
    }

    static final private Descriptor desc_12 = new Descriptor();
    static public boolean CPU_SetSegGeneralCS(/*Bitu*/int value) {
        value &= 0xffff;
        if (!cpu.pmode || (CPU_Regs.flags & CPU_Regs.VM) != 0) {
            CPU_Regs.SegSet16CS(value);
            return false;
        } else {

            if ((value & 0xfffc)==0) {
                CPU_Regs.reg_csVal.dword=value;
                CPU_Regs.reg_csPhys.dword=0;	// ??
                return false;
            }
            if (!cpu.gdt.GetDescriptor(value,desc_12)) {
                return CPU_PrepareException(EXCEPTION_GP,value & 0xfffc);
            }
            switch (desc_12.Type()) {
            case DESC_DATA_EU_RO_NA:		case DESC_DATA_EU_RO_A:
            case DESC_DATA_EU_RW_NA:		case DESC_DATA_EU_RW_A:
            case DESC_DATA_ED_RO_NA:		case DESC_DATA_ED_RO_A:
            case DESC_DATA_ED_RW_NA:		case DESC_DATA_ED_RW_A:
            case DESC_CODE_R_NC_A:			case DESC_CODE_R_NC_NA:
                if (((value & 3)>desc_12.DPL()) || (cpu.cpl>desc_12.DPL())) {
                    // extreme pinball
                    return CPU_PrepareException(EXCEPTION_GP,value & 0xfffc);
                }
                break;
            case DESC_CODE_R_C_A:			case DESC_CODE_R_C_NA:
                break;
            default:
                // gabriel knight
                return CPU_PrepareException(EXCEPTION_GP,value & 0xfffc);

            }
            if (desc_12.saved.seg.p()==0) {
                // win
                return CPU_PrepareException(EXCEPTION_NP,value & 0xfffc);
            }

            CPU_Regs.reg_csVal.dword=value;
            CPU_Regs.reg_csPhys.dword=desc_12.GetBase();
            return false;
        }
    }

    static final private Descriptor desc_13 = new Descriptor();
    static public boolean CPU_SetSegGeneralDS(/*Bitu*/int value) {
        value &= 0xffff;
        if (!cpu.pmode || (CPU_Regs.flags & CPU_Regs.VM) != 0) {
            CPU_Regs.SegSet16DS(value);
            return false;
        } else {

            if ((value & 0xfffc)==0) {
                CPU_Regs.reg_dsVal.dword=value;
                CPU_Regs.reg_dsPhys.dword=0;	// ??
                return false;
            }
            if (!cpu.gdt.GetDescriptor(value,desc_13)) {
                return CPU_PrepareException(EXCEPTION_GP,value & 0xfffc);
            }
            switch (desc_13.Type()) {
            case DESC_DATA_EU_RO_NA:		case DESC_DATA_EU_RO_A:
            case DESC_DATA_EU_RW_NA:		case DESC_DATA_EU_RW_A:
            case DESC_DATA_ED_RO_NA:		case DESC_DATA_ED_RO_A:
            case DESC_DATA_ED_RW_NA:		case DESC_DATA_ED_RW_A:
            case DESC_CODE_R_NC_A:			case DESC_CODE_R_NC_NA:
                if (((value & 3)>desc_13.DPL()) || (cpu.cpl>desc_13.DPL())) {
                    // extreme pinball
                    return CPU_PrepareException(EXCEPTION_GP,value & 0xfffc);
                }
                break;
            case DESC_CODE_R_C_A:			case DESC_CODE_R_C_NA:
                break;
            default:
                // gabriel knight
                return CPU_PrepareException(EXCEPTION_GP,value & 0xfffc);

            }
            if (desc_13.saved.seg.p()==0) {
                // win
                return CPU_PrepareException(EXCEPTION_NP,value & 0xfffc);
            }

            CPU_Regs.reg_dsVal.dword=value;
            CPU_Regs.reg_dsPhys.dword=desc_13.GetBase();
            return false;
        }
    }

    static private final Descriptor desc_14 = new Descriptor();
    static public boolean CPU_SetSegGeneralFS(/*Bitu*/int value) {
        value &= 0xffff;
        if (!cpu.pmode || (CPU_Regs.flags & CPU_Regs.VM) != 0) {
            CPU_Regs.SegSet16FS(value);
            return false;
        } else {

            if ((value & 0xfffc)==0) {
                CPU_Regs.reg_fsVal.dword=value;
                CPU_Regs.reg_fsPhys.dword=0;	// ??
                return false;
            }
            if (!cpu.gdt.GetDescriptor(value,desc_14)) {
                return CPU_PrepareException(EXCEPTION_GP,value & 0xfffc);
            }
            switch (desc_14.Type()) {
            case DESC_DATA_EU_RO_NA:		case DESC_DATA_EU_RO_A:
            case DESC_DATA_EU_RW_NA:		case DESC_DATA_EU_RW_A:
            case DESC_DATA_ED_RO_NA:		case DESC_DATA_ED_RO_A:
            case DESC_DATA_ED_RW_NA:		case DESC_DATA_ED_RW_A:
            case DESC_CODE_R_NC_A:			case DESC_CODE_R_NC_NA:
                if (((value & 3)>desc_14.DPL()) || (cpu.cpl>desc_14.DPL())) {
                    // extreme pinball
                    return CPU_PrepareException(EXCEPTION_GP,value & 0xfffc);
                }
                break;
            case DESC_CODE_R_C_A:			case DESC_CODE_R_C_NA:
                break;
            default:
                // gabriel knight
                return CPU_PrepareException(EXCEPTION_GP,value & 0xfffc);

            }
            if (desc_14.saved.seg.p()==0) {
                // win
                return CPU_PrepareException(EXCEPTION_NP,value & 0xfffc);
            }

            CPU_Regs.reg_fsVal.dword=value;
            CPU_Regs.reg_fsPhys.dword=desc_14.GetBase();
            return false;
        }
    }

    static final private Descriptor desc_15 = new Descriptor();
    static public boolean CPU_SetSegGeneralGS(/*Bitu*/int value) {
        value &= 0xffff;
        if (!cpu.pmode || (CPU_Regs.flags & CPU_Regs.VM) != 0) {
            CPU_Regs.SegSet16GS(value);
            return false;
        } else {

            if ((value & 0xfffc)==0) {
                CPU_Regs.reg_gsVal.dword=value;
                CPU_Regs.reg_gsPhys.dword=0;	// ??
                return false;
            }

            if (!cpu.gdt.GetDescriptor(value,desc_15)) {
                return CPU_PrepareException(EXCEPTION_GP,value & 0xfffc);
            }
            switch (desc_15.Type()) {
            case DESC_DATA_EU_RO_NA:		case DESC_DATA_EU_RO_A:
            case DESC_DATA_EU_RW_NA:		case DESC_DATA_EU_RW_A:
            case DESC_DATA_ED_RO_NA:		case DESC_DATA_ED_RO_A:
            case DESC_DATA_ED_RW_NA:		case DESC_DATA_ED_RW_A:
            case DESC_CODE_R_NC_A:			case DESC_CODE_R_NC_NA:
                if (((value & 3)>desc_15.DPL()) || (cpu.cpl>desc_15.DPL())) {
                    // extreme pinball
                    return CPU_PrepareException(EXCEPTION_GP,value & 0xfffc);
                }
                break;
            case DESC_CODE_R_C_A:			case DESC_CODE_R_C_NA:
                break;
            default:
                // gabriel knight
                return CPU_PrepareException(EXCEPTION_GP,value & 0xfffc);

            }
            if (desc_15.saved.seg.p()==0) {
                // win
                return CPU_PrepareException(EXCEPTION_NP,value & 0xfffc);
            }

            CPU_Regs.reg_gsVal.dword=value;
            CPU_Regs.reg_gsPhys.dword=desc_15.GetBase();
            return false;
        }
    }

    static private final Descriptor desc_16 = new Descriptor();
    static public boolean CPU_SetSegGeneralSS(/*Bitu*/int value) {
        value &= 0xffff;
        if (!cpu.pmode || (CPU_Regs.flags & CPU_Regs.VM) != 0) {
            CPU_Regs.SegSet16SS(value);
            cpu.stack.big=false;
            cpu.stack.mask=0xffff;
            cpu.stack.notmask=0xffff0000;
            return false;
        } else {
            // Stack needs to be non-zero
            if ((value & 0xfffc)==0) {
                Log.exit("CPU_SetSegGeneral: Stack segment zero");
//				return CPU_PrepareException(EXCEPTION_GP,0);
            }
            if (!cpu.gdt.GetDescriptor(value,desc_16)) {
                Log.exit("CPU_SetSegGeneral: Stack segment beyond limits");
//				return CPU_PrepareException(EXCEPTION_GP,value & 0xfffc);
            }
            if (((value & 3)!=cpu.cpl) || (desc_16.DPL()!=cpu.cpl)) {
                Log.exit("CPU_SetSegGeneral: Stack segment with invalid privileges");
//				return CPU_PrepareException(EXCEPTION_GP,value & 0xfffc);
            }

            switch (desc_16.Type()) {
            case DESC_DATA_EU_RW_NA:		case DESC_DATA_EU_RW_A:
            case DESC_DATA_ED_RW_NA:		case DESC_DATA_ED_RW_A:
                break;
            default:
                //Earth Siege 1
                return CPU_PrepareException(EXCEPTION_GP,value & 0xfffc);
            }

            if (desc_16.saved.seg.p() == 0) {
//				Log.exit("CPU_SetSegGeneral: Stack segment not present");	// or #SS(sel)
                return CPU_PrepareException(EXCEPTION_SS,value & 0xfffc);
            }

            CPU_Regs.reg_ssVal.dword=value;
            CPU_Regs.reg_ssPhys.dword=desc_16.GetBase();
            if (desc_16.Big()!=0) {
                cpu.stack.big=true;
                cpu.stack.mask=0xffffffff;
                cpu.stack.notmask=0;
            } else {
                cpu.stack.big=false;
                cpu.stack.mask=0xffff;
                cpu.stack.notmask=0xffff0000;
            }
            return false;
        }
    }

    static public boolean CPU_SetSegGeneral_index(int seg,/*Bitu*/int value) {
        switch (seg) {
            case CPU_Regs.es: return CPU_SetSegGeneralES(value);
            case CPU_Regs.cs: return CPU_SetSegGeneralCS(value);
            case CPU_Regs.ss: return CPU_SetSegGeneralSS(value);
            case CPU_Regs.ds: return CPU_SetSegGeneralDS(value);
            case CPU_Regs.fs: return CPU_SetSegGeneralFS(value);
            case CPU_Regs.gs: return CPU_SetSegGeneralGS(value);
            default:
                Log.exit("Unknown segment");
                return false;
        }
    }

    public static boolean CPU_PopSegES(boolean use32) {
         /*Bitu*/int val=Memory.mem_readw(CPU_Regs.reg_ssPhys.dword + (CPU_Regs.reg_esp.dword & cpu.stack.mask));
        if (CPU_SetSegGeneralES(val)) return true;
         /*Bitu*/int addsp=use32?0x04:0x02;
        CPU_Regs.reg_esp.dword=(CPU_Regs.reg_esp.dword & cpu.stack.notmask)|((CPU_Regs.reg_esp.dword+addsp) & cpu.stack.mask);
        return false;
    }

    public static boolean CPU_PopSegCS(boolean use32) {
        /*Bitu*/int val=Memory.mem_readw(CPU_Regs.reg_ssPhys.dword + (CPU_Regs.reg_esp.dword & cpu.stack.mask));
        if (CPU_SetSegGeneralCS(val)) return true;
         /*Bitu*/int addsp=use32?0x04:0x02;
        CPU_Regs.reg_esp.dword=(CPU_Regs.reg_esp.dword & cpu.stack.notmask)|((CPU_Regs.reg_esp.dword+addsp) & cpu.stack.mask);
        return false;
    }

    public static boolean CPU_PopSegSS(boolean use32) {
         /*Bitu*/int val=Memory.mem_readw(CPU_Regs.reg_ssPhys.dword + (CPU_Regs.reg_esp.dword & cpu.stack.mask));
        if (CPU_SetSegGeneralSS(val)) return true;
         /*Bitu*/int addsp=use32?0x04:0x02;
        CPU_Regs.reg_esp.dword=(CPU_Regs.reg_esp.dword & cpu.stack.notmask)|((CPU_Regs.reg_esp.dword+addsp) & cpu.stack.mask);
        return false;
    }

    public static boolean CPU_PopSegDS(boolean use32) {
         /*Bitu*/int val=Memory.mem_readw(CPU_Regs.reg_ssPhys.dword + (CPU_Regs.reg_esp.dword & cpu.stack.mask));
        if (CPU_SetSegGeneralDS(val)) return true;
         /*Bitu*/int addsp=use32?0x04:0x02;
        CPU_Regs.reg_esp.dword=(CPU_Regs.reg_esp.dword & cpu.stack.notmask)|((CPU_Regs.reg_esp.dword+addsp) & cpu.stack.mask);
        return false;
    }

    public static boolean CPU_PopSegFS(boolean use32) {
         /*Bitu*/int val=Memory.mem_readw(CPU_Regs.reg_ssPhys.dword + (CPU_Regs.reg_esp.dword & cpu.stack.mask));
        if (CPU_SetSegGeneralFS(val)) return true;
         /*Bitu*/int addsp=use32?0x04:0x02;
        CPU_Regs.reg_esp.dword=(CPU_Regs.reg_esp.dword & cpu.stack.notmask)|((CPU_Regs.reg_esp.dword+addsp) & cpu.stack.mask);
        return false;
    }

    public static boolean CPU_PopSegGS(boolean use32) {
         /*Bitu*/int val=Memory.mem_readw(CPU_Regs.reg_ssPhys.dword + (CPU_Regs.reg_esp.dword & cpu.stack.mask));
        if (CPU_SetSegGeneralGS(val)) return true;
         /*Bitu*/int addsp=use32?0x04:0x02;
        CPU_Regs.reg_esp.dword=(CPU_Regs.reg_esp.dword & cpu.stack.notmask)|((CPU_Regs.reg_esp.dword+addsp) & cpu.stack.mask);
        return false;
    }

    public static void CPU_CPUID() {
        switch (CPU_Regs.reg_eax.dword) {
        case 0:	/* Vendor ID String and maximum level? */
            CPU_Regs.reg_eax.dword=1;  /* Maximum level */
            CPU_Regs.reg_ebx.dword='G' | ('e' << 8) | ('n' << 16) | ('u'<< 24);
            CPU_Regs.reg_edx.dword='i' | ('n' << 8) | ('e' << 16) | ('I'<< 24);
            CPU_Regs.reg_ecx.dword='n' | ('t' << 8) | ('e' << 16) | ('l'<< 24);
            break;
        case 1:	/* get processor type/family/model/stepping and feature flags */
            if ((CPU_ArchitectureType==CPU_ARCHTYPE_486NEW) ||
                (CPU_ArchitectureType==CPU_ARCHTYPE_MIXED)) {
                CPU_Regs.reg_eax.dword=0x402;		/* intel 486dx */
                CPU_Regs.reg_ebx.dword=0;			/* Not Supported */
                CPU_Regs.reg_ecx.dword=0;			/* No features */
                CPU_Regs.reg_edx.dword=0x00000001;	/* FPU */
            } else if (CPU_ArchitectureType==CPU_ARCHTYPE_PENTIUM) {
                CPU_Regs.reg_eax.dword=0x513;		/* intel pentium */
                CPU_Regs.reg_ebx.dword=0;			/* Not Supported */
                CPU_Regs.reg_ecx.dword=0;			/* No features */
                CPU_Regs.reg_edx.dword=0x00000011;	/* FPU+TimeStamp/RDTSC */
                CPU_Regs.reg_edx.dword|= (1<<8);    /* CMPXCHG8B instruction */
                CPU_Regs.reg_edx.dword|= (1<<5);    /* MSR */
            } else if (CPU_ArchitectureType==CPU_ARCHTYPE_PENTIUM_PRO) {
                CPU_Regs.reg_eax.dword=0x611;		/* intel pentium pro */
                CPU_Regs.reg_ebx.dword=0;			/* Not Supported */
                CPU_Regs.reg_ecx.dword=0;			/* No features */
                CPU_Regs.reg_edx.dword=0x00000011;	/* FPU+TimeStamp/RDTSC */
                CPU_Regs.reg_edx.dword|= (1<<5);    /* MSR */
                CPU_Regs.reg_edx.dword|= (1<<15);   /* support CMOV instructions */
                CPU_Regs.reg_edx.dword|= (1<<13);   /* PTE Global Flag */
                CPU_Regs.reg_edx.dword|= (1<<8);    /* CMPXCHG8B instruction */
            }
            break;
        case 0x80000000:
            CPU_Regs.reg_eax.dword = 0;
            break;
        default:
            if (Log.level<=LogSeverities.LOG_ERROR) Log.log(LogTypes.LOG_CPU,LogSeverities.LOG_ERROR,"Unhandled CPUID Function "+Integer.toString(CPU_Regs.reg_eax.dword,16));
            CPU_Regs.reg_eax.dword=0;
            CPU_Regs.reg_ebx.dword=0;
            CPU_Regs.reg_ecx.dword=0;
            CPU_Regs.reg_edx.dword=0;
            break;
        }
    }

    final static private CPU_Decoder HLT_Decode = new CPU_Decoder() {
        public /*Bits*/int call() {
            /* Once an interrupt occurs, it should change cpu core */
            if (CPU_Regs.reg_eip!=cpu.hlt.eip || CPU_Regs.reg_csVal.dword != cpu.hlt.cs) {
                cpudecoder=cpu.hlt.old_decoder;
            } else {
                CPU_IODelayRemoved += CPU_Cycles;
                CPU_Cycles=0;
            }
            return 0;
        }
    };


    static public void CPU_HLT( /*Bitu*/int oldeip) {
        CPU_Regs.reg_eip=oldeip;
        CPU_IODelayRemoved += CPU_Cycles;
        CPU_Cycles=0;
        cpu.hlt.cs=CPU_Regs.reg_csVal.dword;
        cpu.hlt.eip=CPU_Regs.reg_eip;
        cpu.hlt.old_decoder=cpudecoder;
        cpudecoder=HLT_Decode;
    }

    static public void CPU_ENTER(boolean use32, /*Bitu*/int bytes, /*Bitu*/int level) {
        level&=0x1f;
         /*Bitu*/int sp_index=CPU_Regs.reg_esp.dword & cpu.stack.mask;
         /*Bitu*/int bp_index=CPU_Regs.reg_ebp.dword & cpu.stack.mask;
        if (!use32) {
            sp_index-=2;
            Memory.mem_writew(CPU_Regs.reg_ssPhys.dword+sp_index,CPU_Regs.reg_ebp.word());
            CPU_Regs.reg_ebp.word(CPU_Regs.reg_esp.dword-2);
            if (level!=0) {
                for ( /*Bitu*/int i=1;i<level;i++) {
                    sp_index-=2;bp_index-=2;
                    Memory.mem_writew(CPU_Regs.reg_ssPhys.dword+sp_index,Memory.mem_readw(CPU_Regs.reg_ssPhys.dword+bp_index));
                }
                sp_index-=2;
                Memory.mem_writew(CPU_Regs.reg_ssPhys.dword+sp_index,CPU_Regs.reg_ebp.word());
            }
        } else {
            sp_index-=4;
            Memory.mem_writed(CPU_Regs.reg_ssPhys.dword+sp_index,CPU_Regs.reg_ebp.dword);
            CPU_Regs.reg_ebp.dword=CPU_Regs.reg_esp.dword-4;
            if (level!=0) {
                for ( /*Bitu*/int i=1;i<level;i++) {
                    sp_index-=4;bp_index-=4;
                    Memory.mem_writed(CPU_Regs.reg_ssPhys.dword+sp_index,Memory.mem_readd(CPU_Regs.reg_ssPhys.dword + bp_index));
                }
                sp_index-=4;
                Memory.mem_writed(CPU_Regs.reg_ssPhys.dword+sp_index,CPU_Regs.reg_ebp.dword);
            }
        }
        sp_index-=bytes;
        CPU_Regs.reg_esp.dword=(CPU_Regs.reg_esp.dword & cpu.stack.notmask)|(sp_index & cpu.stack.mask);
    }

    final static private Mapper.MAPPER_Handler CPU_CycleIncrease = new Mapper.MAPPER_Handler() {
        public void call(boolean pressed) {
            if (!pressed) return;
            if (CPU_CycleAutoAdjust) {
                CPU_CyclePercUsed+=5;
                if (CPU_CyclePercUsed>105) CPU_CyclePercUsed=105;
                Log.log_msg("CPU speed: max "+CPU_CyclePercUsed+" percent.");
                Main.GFX_SetTitle(CPU_CyclePercUsed,-1,false);
            } else {
                /*Bit32s*/int old_cycles=CPU_CycleMax;
                if (CPU_CycleUp < 100) {
                    CPU_CycleMax = (/*Bit32s*/int)(CPU_CycleMax * (1 + (float)CPU_CycleUp / 100.0));
                } else {
                    CPU_CycleMax = (CPU_CycleMax + CPU_CycleUp);
                }

                CPU_CycleLeft=0;CPU_Cycles=0;
                if (CPU_CycleMax==old_cycles) CPU_CycleMax++;
                if(CPU_CycleMax > 15000 )
                    Log.log_msg("CPU speed: fixed "+CPU_CycleMax+" cycles. If you need more than 20000, try core=dynamic in DOSBox's options.");
                else
                    Log.log_msg("CPU speed: fixed "+CPU_CycleMax+" cycles.");
                Main.GFX_SetTitle(CPU_CycleMax,-1,false);
            }
        }
    };
    final static private Mapper.MAPPER_Handler CPU_CycleDecrease = new Mapper.MAPPER_Handler() {
        public void call(boolean pressed) {
            if (!pressed) return;
            if (CPU_CycleAutoAdjust) {
                CPU_CyclePercUsed-=5;
                if (CPU_CyclePercUsed<=0) CPU_CyclePercUsed=1;
                if(CPU_CyclePercUsed <=70)
                    Log.log_msg("CPU speed: max "+CPU_CyclePercUsed+" percent. If the game runs too fast, try a fixed cycles amount in DOSBox's options.");
                else
                    Log.log_msg("CPU speed: max "+CPU_CyclePercUsed+" percent.");
                Main.GFX_SetTitle(CPU_CyclePercUsed,-1,false);
            } else {
                if (CPU_CycleDown < 100) {
                    CPU_CycleMax = (/*Bit32s*/int)(CPU_CycleMax / (1 + (float)CPU_CycleDown / 100.0));
                } else {
                    CPU_CycleMax = (CPU_CycleMax - CPU_CycleDown);
                }
                CPU_CycleLeft=0;CPU_Cycles=0;
                if (CPU_CycleMax <= 0) CPU_CycleMax=1;
                Log.log_msg("CPU speed: fixed "+CPU_CycleMax+" cycles.");
                Main.GFX_SetTitle(CPU_CycleMax,-1,false);
            }
        }
    };

    void CPU_Enable_SkipAutoAdjust() {
        if (CPU_CycleAutoAdjust) {
            CPU_CycleMax /= 2;
            if (CPU_CycleMax < CPU_CYCLES_LOWER_LIMIT)
                CPU_CycleMax = CPU_CYCLES_LOWER_LIMIT;
        }
        CPU_SkipCycleAutoAdjust=true;
    }

    void CPU_Disable_SkipAutoAdjust() {
        CPU_SkipCycleAutoAdjust=false;
    }


    void CPU_Reset_AutoAdjust() {
        CPU_IODelayRemoved = 0;
        Dosbox.ticksDone = 0;
        Dosbox.ticksScheduled = 0;
    }

    private static Hashtable<Integer, Long> msrs = new Hashtable<Integer, Long>();

    public static long readMSR(int index) {
        Integer i = new Integer(index);
        Long result = msrs.get(i);
        if (result != null) {
            return result.longValue();
        }
        return 0;
    }

    public static void writeMSR(int index, long value) {
        Integer i = new Integer(index);
        msrs.put(i, new Long(value));
    }
    private static boolean inited = false;
    static public void initialize() {
        inited = false;
        cpu = new CPUBlock();
    }
    public CPU(Section configuration) {
        super(configuration);
        if(inited) {
            Change_Config(configuration);
            return;
        }
//		Section_prop * section=static_cast<Section_prop *>(configuration);
        inited=true;
        CPU_Regs.reg_eax.dword=0;
        CPU_Regs.reg_ebx.dword=0;
        CPU_Regs.reg_ecx.dword=0;
        CPU_Regs.reg_edx.dword=0;
        CPU_Regs.reg_edi.dword=0;
        CPU_Regs.reg_esi.dword=0;
        CPU_Regs.reg_ebp.dword=0;
        CPU_Regs.reg_esp.dword=0;

        CPU_Regs.SegSet16CS(0);
        CPU_Regs.SegSet16DS(0);
        CPU_Regs.SegSet16ES(0);
        CPU_Regs.SegSet16FS(0);
        CPU_Regs.SegSet16GS(0);
        CPU_Regs.SegSet16SS(0);

        CPU_SetFlags(CPU_Regs.IF,CPU_Regs.FMASK_ALL);		//Enable interrupts
        cpu.cr0=0xffffffff;
        CPU_SET_CRX(0,0);						//Initialize
        cpu.code.big=false;
        cpu.stack.mask=0xffff;
        cpu.stack.notmask=0xffff0000;
        cpu.stack.big=false;
        cpu.trap_skip=false;
        cpu.idt.SetBase(0);
        cpu.idt.SetLimit(1023);

        for (/*Bitu*/int i=0; i<7; i++) {
            cpu.drx[i]=0;
            cpu.trx[i]=0;
        }
        if (CPU_ArchitectureType>=CPU_ARCHTYPE_PENTIUM) {
            cpu.drx[6]=0xffff0ff0;
        } else {
            cpu.drx[6]=0xffff1ff0;
        }
        cpu.drx[7]=0x00000400;

        /* Init the cpu cores */
        Core_normal.CPU_Core_Normal_Init();
        Core_full.CPU_Core_Full_Init();
        if (Config.C_DYNAMIC)
            Core_dynamic.CPU_Core_Dynamic_Init();
        JavaMapper.MAPPER_AddHandler(CPU_CycleDecrease, Mapper.MapKeys.MK_f11, Mapper.MMOD1, "cycledown", "Dec Cycles");
        JavaMapper.MAPPER_AddHandler(CPU_CycleIncrease, Mapper.MapKeys.MK_f12, Mapper.MMOD1, "cycleup", "Inc Cycles");
        Change_Config(configuration);
        CPU_JMP(false,0,0,0);					//Setup the first cpu core
    }
    public boolean Change_Config(Section newconfig){
        Section_prop section=(Section_prop)newconfig;
        CPU_AutoDetermineMode=CPU_AUTODETERMINE_NONE;
        //CPU_CycleLeft=0;//needed ?
        CPU_Cycles=0;
        CPU_SkipCycleAutoAdjust=false;

        Prop_multival p = section.Get_multival("cycles");
        String type = p.GetSection().Get_string("type");
        String str;
        CommandLine cmd = new CommandLine(null, p.GetSection().Get_string("parameters"));
        if (type.equals("max")) {
            CPU_CycleMax=0;
            CPU_CyclePercUsed=100;
            CPU_CycleAutoAdjust=true;
            CPU_CycleLimit=-1;
            for (/*Bitu*/int cmdnum=1; cmdnum<=cmd.GetCount(); cmdnum++) {
                if ((str=cmd.FindCommand(cmdnum))!=null) {
                    if (str.endsWith("%")) {
                        try {
                            int percval=Integer.parseInt(str.substring(0, str.length()-1));
                            if ((percval>0) && (percval<=105)) CPU_CyclePercUsed=percval;
                        } catch (Exception e) {
                        }
                    } else if (str.equals("limit")) {
                        cmdnum++;
                        if ((str=cmd.FindCommand(cmdnum))!=null) {
                            try {
                                int cyclimit=Integer.parseInt(str);
                                if (cyclimit>0) CPU_CycleLimit=cyclimit;
                            } catch (Exception e) {
                            }
                        }
                    }
                }
            }
        } else {
            if (type.equals("auto")) {
                CPU_AutoDetermineMode|=CPU_AUTODETERMINE_CYCLES;
                CPU_CycleMax=3000;
                CPU_OldCycleMax=3000;
                CPU_CyclePercUsed=100;
                for (/*Bitu*/int cmdnum=0; cmdnum<=cmd.GetCount(); cmdnum++) {
                    if ((str=cmd.FindCommand(cmdnum))!=null) {
                        if (str.endsWith("%")) {
                            try {
                                int percval=Integer.parseInt(str.substring(0, str.length()-1));
                                if ((percval>0) && (percval<=105)) CPU_CyclePercUsed=percval;
                            } catch (Exception e) {
                            }
                        } else if (str.equals("limit")) {
                            cmdnum++;
                            if ((str=cmd.FindCommand(cmdnum))!=null) {
                                try {
                                    int cyclimit=Integer.parseInt(str);
                                    if (cyclimit>0) CPU_CycleLimit=cyclimit;
                                } catch (Exception e) {
                                }
                            }
                        } else {
                            try {
                                int rmdval=Integer.parseInt(str);
                                if (rmdval>0) {
                                    CPU_CycleMax=rmdval;
                                    CPU_OldCycleMax=rmdval;
                                }
                            } catch (Exception e) {
                            }
                        }
                    }
                }
            } else if(type.equals("fixed")) {
                str = cmd.FindCommand(1);
                try {CPU_CycleMax=Integer.parseInt(str);} catch (Exception e){}
            } else {
                int rmdval=0;
                try {
                    rmdval = Integer.parseInt(type);
                    if(rmdval!=0) CPU_CycleMax=rmdval;
                } catch (Exception e) {
                }
            }
            CPU_CycleAutoAdjust=false;
        }

        CPU_CycleUp=section.Get_int("cycleup");
        CPU_CycleDown=section.Get_int("cycledown");
        String core = section.Get_string("core");
        cpudecoder=Core_normal.CPU_Core_Normal_Run;
        if (core.equals("normal")) {
            cpudecoder=Core_normal.CPU_Core_Normal_Run;
        } else if (core.equals("full")) {
            cpudecoder=Core_full.CPU_Core_Full_Run;
        } else if (core.equals("auto")) {
            cpudecoder=Core_normal.CPU_Core_Normal_Run;
            if (Config.C_DYNAMIC || Config.C_DYNREC)
                CPU_AutoDetermineMode|=CPU_AUTODETERMINE_CORE;
        } else if (Config.C_DYNAMIC) {
            if (core.equals("dynamic")) {
                cpudecoder= Core_dynamic.CPU_Core_Dynamic_Run;
            }
        }

        if (Config.C_DYNAMIC)
            Core_dynamic.CPU_Core_Dynamic_Cache_Init(core.equals("dynamic"));

        CPU_ArchitectureType = CPU_ARCHTYPE_MIXED;
        String cputype = section.Get_string("cputype");
        if (cputype.equals("auto")) {
            CPU_ArchitectureType = CPU_ARCHTYPE_MIXED;
        } else if (cputype.equals("386")) {
            CPU_ArchitectureType = CPU_ARCHTYPE_386;
        } else if (cputype.equals("386_prefetch")) {
            CPU_ArchitectureType = CPU_ARCHTYPE_386;
            if (core.equals("normal")) {
                cpudecoder=Core_prefetch.CPU_Core_Prefetch_Run;
                CPU_PrefetchQueueSize = 16;
            } else if (core.equals("auto")) {
                cpudecoder=Core_prefetch.CPU_Core_Prefetch_Run;
                CPU_PrefetchQueueSize = 16;
                CPU_AutoDetermineMode&=(~CPU_AUTODETERMINE_CORE);
            } else {
                Log.exit("prefetch queue emulation requires the normal core setting.");
            }
        } else if (cputype.equals("486")) {
            CPU_ArchitectureType = CPU_ARCHTYPE_486NEW;
        } else if (cputype.equals("486_prefetch")) {
            CPU_ArchitectureType = CPU_ARCHTYPE_486NEW;
            if (core.equals("normal")) {
                cpudecoder=Core_prefetch.CPU_Core_Prefetch_Run;
                CPU_PrefetchQueueSize = 32;
            } else if (core.equals("auto")) {
                cpudecoder=Core_prefetch.CPU_Core_Prefetch_Run;
                CPU_PrefetchQueueSize = 32;
                CPU_AutoDetermineMode&=(~CPU_AUTODETERMINE_CORE);
            } else {
                Log.exit("prefetch queue emulation requires the normal core setting.");
            }
        } else if (cputype.equals("pentium")) {
            CPU_ArchitectureType = CPU_ARCHTYPE_PENTIUM;
        } else if (cputype.equals("p6")) {
            CPU_ArchitectureType = CPU_ARCHTYPE_PENTIUM_PRO;
        }

        if (CPU_ArchitectureType>=CPU_ARCHTYPE_486NEW) CPU_extflags_toggle=(CPU_Regs.ID|CPU_Regs.AC);
		else if (CPU_ArchitectureType>=CPU_ARCHTYPE_486OLD) CPU_extflags_toggle=CPU_Regs.AC;
		else CPU_extflags_toggle=0;

        if(CPU_CycleMax <= 0) CPU_CycleMax = 3000;
        if(CPU_CycleUp <= 0)   CPU_CycleUp = 500;
        if(CPU_CycleDown <= 0) CPU_CycleDown = 20;
        if (CPU_CycleAutoAdjust) Main.GFX_SetTitle(CPU_CyclePercUsed,-1,false);
        else Main.GFX_SetTitle(CPU_CycleMax,-1,false);
        return true;
    }

    static private CPU test;

    final private static Section.SectionFunction CPU_ShutDown = new Section.SectionFunction() {
        public void call (Section sec) {
            if (Config.C_DYNAMIC)
                Core_dynamic.CPU_Core_Dynamic_Cache_Close();
            test = null;
        }
    };
    final public static Section.SectionFunction CPU_Init = new Section.SectionFunction() {
        public void call(Section section) {
            test = new CPU(section);
            section.AddDestroyFunction(CPU_ShutDown,true);
        }
    };
}

