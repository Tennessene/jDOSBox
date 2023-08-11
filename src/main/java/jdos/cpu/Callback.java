package jdos.cpu;

import jdos.Dosbox;
import jdos.hardware.Memory;
import jdos.misc.Log;
import jdos.misc.setup.Section;
import jdos.types.LogSeverities;
import jdos.types.LogTypes;

public class Callback {
    static public int inHandler=0;
    static public interface Handler {
        public /*Bitu*/int call();
        public String getName();
    }

    static public final int CB_RETN=0;
    static public final int CB_RETF=1;
    static public final int CB_RETF8=2;
    static public final int CB_IRET=3;
    static public final int CB_IRETD=4;
    static public final int CB_IRET_STI=5;
    static public final int CB_IRET_EOI_PIC1=6;
    static public final int CB_IRQ0=7;
    static public final int CB_IRQ1=8;
    static public final int CB_IRQ9=9;
    static public final int CB_IRQ12=10;
    static public final int CB_IRQ12_RET=11;
    static public final int CB_IRQ6_PCJR=12;
    static public final int CB_MOUSE=13;
	static public final int CB_INT29=14;
    static public final int CB_INT16=15;
    static public final int CB_HOOKABLE=16;
    static public final int CB_TDE_IRET=17;
    static public final int CB_IPXESR=18;
    static public final int CB_IPXESR_RET=19;
	static public final int CB_INT21=20;
    static public final int CB_INT13=21;

    static public final int CB_MAX = 128;
    static public final int CB_SIZE = 32;
    static public final int CB_SEG = 0xF000;
    static public final int CB_SOFFSET = 0x1000;

    static public final int CBRET_NONE=0;
    static public final int CBRET_STOP=1;

    static public Handler[] CallBack_Handlers = new Handler[CB_MAX];
    static public String[] CallBack_Description = new String[CB_MAX];
    
    public static /*RealPt*/int CALLBACK_RealPointer(/*Bitu*/int callback) {
    	return Memory.RealMake(CB_SEG,(CB_SOFFSET+callback*CB_SIZE));
    }

    public static /*PhysPt*/int CALLBACK_PhysPointer(/*Bitu*/int callback) {
        return Memory.PhysMake(CB_SEG,(CB_SOFFSET+callback*CB_SIZE));
    }

    public static /*PhysPt*/int CALLBACK_GetBase() {
        return (CB_SEG << 4) + CB_SOFFSET;
    }

    private static /*Bitu*/int call_stop,call_idle;
    public static /*Bitu*/int call_priv_io;

    public static /*Bitu*/int CALLBACK_Allocate() {
        for (int i=1;i<CallBack_Handlers.length;i++) {
            if (CallBack_Handlers[i] == illegal_handler) {
                CallBack_Handlers[i] = null;
                return i;
            }
        }
        Log.exit("CALLBACK:Can't allocate handler.");
        return 0;
    }

    private static void CALLBACK_DeAllocate(/*Bitu*/int in) {
        CallBack_Handlers[in]=illegal_handler;
    }

    private static Handler illegal_handler = new Handler() {
        public /*Bitu*/int call() {
            Log.exit("Illegal CallBack Called");
            return 1;
        }
        public String getName() {
            return "Callback.illegal_handler";
        }
    };
    public static void CALLBACK_Idle() {
    /* this makes the cpu execute instructions to handle irq's and then come back */
        /*Bitu*/int oldIF=CPU_Regs.GETFLAG(CPU_Regs.IF);
        CPU_Regs.SETFLAGBIT(CPU_Regs.IF,true);
        /*Bit16u*/int oldcs=CPU_Regs.reg_csVal.dword;
        /*Bit32u*/int oldeip=CPU_Regs.reg_eip;
        CPU_Regs.SegSet16CS(CB_SEG);
        CPU_Regs.reg_eip=CB_SOFFSET+call_idle*CB_SIZE;
        Dosbox.DOSBOX_RunMachine();
        CPU_Regs.reg_eip=oldeip;
        CPU_Regs.SegSet16CS(oldcs);
        CPU_Regs.SETFLAGBIT(CPU_Regs.IF,oldIF!=0);
        if (!CPU.CPU_CycleAutoAdjust && CPU.CPU_Cycles>0)
            CPU.CPU_Cycles=0;
    }

    private boolean installed = false;
    private /*Bitu*/int m_callback;
    private static final int NONE = 0;
    private static final int SETUP = 1;
    private static final int SETUPAT = 2;
    private int m_type = NONE;
    private class VectorHandler {
        /*RealPt*/int old_vector;
        /*Bit8u*/ int interrupt;
        boolean installed = false;
    }
    private VectorHandler vectorhandler = new VectorHandler();
    public Callback() {
    }

    static private Handler default_handler = new Handler() {
        public /*Bitu*/int call() {
            if (Log.level<=LogSeverities.LOG_ERROR) Log.log(LogTypes.LOG_CPU, LogSeverities.LOG_ERROR,"Illegal Unhandled Interrupt Called "+Integer.toString(CPU.lastint,16));
            return CBRET_NONE;
        }
        public String getName() {
            return "Callback.default_handler";
        }
    };

    static private Handler stop_handler = new Handler() {
        public /*Bitu*/int call() {
            return CBRET_STOP;
        }
        public String getName() {
            return "Callback.stop_handler";
        }
    };

    public static void CALLBACK_RunRealFar(/*Bit16u*/int seg,/*Bit16u*/int off) {
        CPU_Regs.reg_esp.word(CPU_Regs.reg_esp.word()-4);
        Memory.mem_writew(CPU_Regs.reg_ssPhys.dword+CPU_Regs.reg_esp.word(),Memory.RealOff(CALLBACK_RealPointer(call_stop)));
        Memory.mem_writew(CPU_Regs.reg_ssPhys.dword+CPU_Regs.reg_esp.word()+2,Memory.RealSeg(CALLBACK_RealPointer(call_stop)));
        /*Bit32u*/int oldeip=CPU_Regs.reg_eip;
        /*Bit16u*/int oldcs=CPU_Regs.reg_csVal.dword;
        CPU_Regs.reg_eip=off;
        CPU_Regs.SegSet16CS(seg);
        Dosbox.DOSBOX_RunMachine();
        CPU_Regs.reg_eip=oldeip;
        CPU_Regs.SegSet16CS(oldcs);
    }

    public static void CALLBACK_RunRealInt(/*Bit8u*/int intnum) {
        /*Bit32u*/int oldeip=CPU_Regs.reg_eip;
        /*Bit16u*/int oldcs=CPU_Regs.reg_csVal.dword;
        CPU_Regs.reg_eip=CB_SOFFSET+(CB_MAX*CB_SIZE)+(intnum*6);
        CPU_Regs.SegSet16CS(CB_SEG);
        Dosbox.DOSBOX_RunMachine();
        CPU_Regs.reg_eip=oldeip;
        CPU_Regs.SegSet16CS(oldcs);
    }

    public static void CALLBACK_SZF(boolean val) {
        /*Bit16u*/int tempf = Memory.mem_readw(CPU_Regs.reg_ssPhys.dword+CPU_Regs.reg_esp.word()+4);
        if (val) tempf |= CPU_Regs.ZF;
        else tempf &= ~CPU_Regs.ZF;
        Memory.mem_writew(CPU_Regs.reg_ssPhys.dword+CPU_Regs.reg_esp.word()+4,tempf);
    }

    public static void CALLBACK_SCF(boolean val) {
        /*Bit16u*/int tempf = Memory.mem_readw(CPU_Regs.reg_ssPhys.dword+CPU_Regs.reg_esp.word()+4);
        if (val) tempf |= CPU_Regs.CF;
        else tempf &= ~CPU_Regs.CF;
        Memory.mem_writew(CPU_Regs.reg_ssPhys.dword+CPU_Regs.reg_esp.word()+4,tempf);
    }

    public static void CALLBACK_SIF(boolean val) {
        /*Bit16u*/int tempf = Memory.mem_readw(CPU_Regs.reg_ssPhys.dword+CPU_Regs.reg_esp.word()+4);
        if (val) tempf |= CPU_Regs.IF;
        else tempf &= ~CPU_Regs.IF;
        Memory.mem_writew(CPU_Regs.reg_ssPhys.dword+CPU_Regs.reg_esp.word()+4,tempf);
    }

    public static void CALLBACK_SetDescription(/*Bitu*/int nr, String descr) {
        CallBack_Description[nr] = descr;
    }

    public static String CALLBACK_GetDescription(/*Bitu*/int nr) {
        if (nr>=CB_MAX) return null;
        return CallBack_Description[nr];
    }

    public static /*Bitu*/int CALLBACK_SetupExtra(/*Bitu*/int callback, /*Bitu*/int type, /*PhysPt*/int lphysAddress) {
        return CALLBACK_SetupExtra(callback, type, lphysAddress, true);
    }
    public static /*Bitu*/int CALLBACK_SetupExtra(/*Bitu*/int callback, /*Bitu*/int type, /*PhysPt*/int lphysAddress, boolean use_cb/*=true*/) {
        int physAddress = (int)lphysAddress;
        if (callback>=CB_MAX)
            return 0;
        switch (type) {
        case CB_RETN:
            if (use_cb) {
                Memory.phys_writeb(physAddress,0xFE);	//GRP 4
                Memory.phys_writeb(physAddress+0x01,0x38);	//Extra Callback instruction
                Memory.phys_writew(physAddress+0x02,callback);	//The immediate word
                physAddress+=4;
            }
            Memory.phys_writeb(physAddress,0xC3);		//A RETN Instruction
            return (use_cb?5:1);
        case CB_RETF:
            if (use_cb) {
                Memory.phys_writeb(physAddress,0xFE);	//GRP 4
                Memory.phys_writeb(physAddress+0x01,0x38);	//Extra Callback instruction
                Memory.phys_writew(physAddress+0x02,callback);	//The immediate word
                physAddress+=4;
            }
            Memory.phys_writeb(physAddress,0xCB);		//A RETF Instruction
            return (use_cb?5:1);
        case CB_RETF8:
            if (use_cb) {
                Memory.phys_writeb(physAddress,0xFE);	//GRP 4
                Memory.phys_writeb(physAddress+0x01,0x38);	//Extra Callback instruction
                Memory.phys_writew(physAddress+0x02,callback);	//The immediate word
                physAddress+=4;
            }
            Memory.phys_writeb(physAddress,0xCA);		//A RETF 8 Instruction
            Memory.phys_writew(physAddress+0x01,0x0008);
            return (use_cb?7:3);
        case CB_IRET:
            if (use_cb) {
                Memory.phys_writeb(physAddress,0xFE);	//GRP 4
                Memory.phys_writeb(physAddress+0x01,0x38);	//Extra Callback instruction
                Memory.phys_writew(physAddress+0x02,callback);		//The immediate word
                physAddress+=4;
            }
            Memory.phys_writeb(physAddress,0xCF);		//An IRET Instruction
            return (use_cb?5:1);
        case CB_IRETD:
            if (use_cb) {
                Memory.phys_writeb(physAddress,0xFE);	//GRP 4
                Memory.phys_writeb(physAddress+0x01,0x38);	//Extra Callback instruction
                Memory.phys_writew(physAddress+0x02,callback);		//The immediate word
                physAddress+=4;
            }
            Memory.phys_writeb(physAddress,0x66);		//An IRETD Instruction
            Memory.phys_writeb(physAddress+0x01,0xCF);
            return (use_cb?6:2);
        case CB_IRET_STI:
            Memory.phys_writeb(physAddress,0xFB);		//STI
            if (use_cb) {
                Memory.phys_writeb(physAddress+0x01,0xFE);	//GRP 4
                Memory.phys_writeb(physAddress+0x02,0x38);	//Extra Callback instruction
                Memory.phys_writew(physAddress+0x03,callback);	//The immediate word
                physAddress+=4;
            }
            Memory.phys_writeb(physAddress+0x01,0xCF);		//An IRET Instruction
            return (use_cb?6:2);
        case CB_IRET_EOI_PIC1:
            if (use_cb) {
                Memory.phys_writeb(physAddress,0xFE);	//GRP 4
                Memory.phys_writeb(physAddress+0x01,0x38);	//Extra Callback instruction
                Memory.phys_writew(physAddress+0x02,callback);		//The immediate word
                physAddress+=4;
            }
            Memory.phys_writeb(physAddress,0x50);		// push ax
            Memory.phys_writeb(physAddress+0x01,0xb0);		// mov al, 0x20
            Memory.phys_writeb(physAddress+0x02,0x20);
            Memory.phys_writeb(physAddress+0x03,0xe6);		// out 0x20, al
            Memory.phys_writeb(physAddress+0x04,0x20);
            Memory.phys_writeb(physAddress+0x05,0x58);		// pop ax
            Memory.phys_writeb(physAddress+0x06,0xcf);		//An IRET Instruction
            return (use_cb?0x0b:0x07);
        case CB_IRQ0:	// timer int8
            Memory.phys_writeb(physAddress,0xFB);		//STI
            if (use_cb) {
                Memory.phys_writeb(physAddress+0x01,0xFE);	//GRP 4
                Memory.phys_writeb(physAddress+0x02,0x38);	//Extra Callback instruction
                Memory.phys_writew(physAddress+0x03,callback);		//The immediate word
                physAddress+=4;
            }
            Memory.phys_writeb(physAddress+0x01,0x1e);		// push ds
            Memory.phys_writeb(physAddress+0x02,0x50);		// push ax
            Memory.phys_writeb(physAddress+0x03,0x52);		// push dx
            Memory.phys_writew(physAddress+0x04,0x1ccd);	// int 1c
            Memory.phys_writeb(physAddress+0x06,0xfa);		// cli
            Memory.phys_writew(physAddress+0x07,0x20b0);	// mov al, 0x20
            Memory.phys_writew(physAddress+0x09,0x20e6);	// out 0x20, al
            Memory.phys_writeb(physAddress+0x0b,0x5a);		// pop dx
            Memory.phys_writeb(physAddress+0x0c,0x58);		// pop ax
            Memory.phys_writeb(physAddress+0x0d,0x1f);		// pop ds
            Memory.phys_writeb(physAddress+0x0e,0xcf);		//An IRET Instruction
            return (use_cb?0x13:0x0f);
        case CB_IRQ1:	// keyboard int9
            Memory.phys_writeb(physAddress,0x50);			// push ax
            Memory.phys_writew(physAddress+0x01,0x60e4);		// in al, 0x60
            Memory.phys_writew(physAddress+0x03,0x4fb4);		// mov ah, 0x4f
            Memory.phys_writeb(physAddress+0x05,0xf9);			// stc
            Memory.phys_writew(physAddress+0x06,0x15cd);		// int 15
            if (use_cb) {
                Memory.phys_writew(physAddress+0x08,0x0473);	// jc skip
                Memory.phys_writeb(physAddress+0x0a,0xFE);		//GRP 4
                Memory.phys_writeb(physAddress+0x0b,0x38);		//Extra Callback instruction
                Memory.phys_writew(physAddress+0x0c,callback);			//The immediate word
                // jump here to (skip):
                physAddress+=6;
            }
            Memory.phys_writeb(physAddress+0x08,0xfa);			// cli
            Memory.phys_writew(physAddress+0x09,0x20b0);		// mov al, 0x20
            Memory.phys_writew(physAddress+0x0b,0x20e6);		// out 0x20, al
            Memory.phys_writeb(physAddress+0x0d,0x58);			// pop ax
            Memory.phys_writeb(physAddress+0x0e,0xcf);			//An IRET Instruction
            return (use_cb?0x15:0x0f);
        case CB_IRQ9:	// pic cascade interrupt
            if (use_cb) {
                Memory.phys_writeb(physAddress,0xFE);	//GRP 4
                Memory.phys_writeb(physAddress+0x01,0x38);	//Extra Callback instruction
                Memory.phys_writew(physAddress+0x02,callback);		//The immediate word
                physAddress+=4;
            }
            Memory.phys_writeb(physAddress,0x50);		// push ax
            Memory.phys_writew(physAddress+0x01,0x61b0);	// mov al, 0x61
            Memory.phys_writew(physAddress+0x03,0xa0e6);	// out 0xa0, al
            Memory.phys_writew(physAddress+0x05,0x0acd);	// int a
            Memory.phys_writeb(physAddress+0x07,0xfa);		// cli
            Memory.phys_writeb(physAddress+0x08,0x58);		// pop ax
            Memory.phys_writeb(physAddress+0x09,0xcf);		//An IRET Instruction
            return (use_cb?0x0e:0x0a);
        case CB_IRQ12:	// ps2 mouse int74
            if (!use_cb) Log.exit("int74 callback must implement a callback handler!");
            Memory.phys_writeb(physAddress,0x1e);		// push ds
            Memory.phys_writeb(physAddress+0x01,0x06);		// push es
            Memory.phys_writew(physAddress+0x02,0x6066);	// pushad
            Memory.phys_writeb(physAddress+0x04,0xfc);		// cld
            Memory.phys_writeb(physAddress+0x05,0xfb);		// sti
            Memory.phys_writeb(physAddress+0x06,0xFE);		//GRP 4
            Memory.phys_writeb(physAddress+0x07,0x38);		//Extra Callback instruction
            Memory.phys_writew(physAddress+0x08,callback);			//The immediate word
            return 0x0a;
        case CB_IRQ12_RET:	// ps2 mouse int74 return
            if (use_cb) {
                Memory.phys_writeb(physAddress,0xFE);	//GRP 4
                Memory.phys_writeb(physAddress+0x01,0x38);	//Extra Callback instruction
                Memory.phys_writew(physAddress+0x02,callback);		//The immediate word
                physAddress+=4;
            }
            Memory.phys_writeb(physAddress,0xfa);		// cli
            Memory.phys_writew(physAddress+0x01,0x20b0);	// mov al, 0x20
            Memory.phys_writew(physAddress+0x03,0xa0e6);	// out 0xa0, al
            Memory.phys_writew(physAddress+0x05,0x20e6);	// out 0x20, al
            Memory.phys_writew(physAddress+0x07,0x6166);	// popad
            Memory.phys_writeb(physAddress+0x09,0x07);		// pop es
            Memory.phys_writeb(physAddress+0x0a,0x1f);		// pop ds
            Memory.phys_writeb(physAddress+0x0b,0xcf);		//An IRET Instruction
            return (use_cb?0x10:0x0c);
        case CB_IRQ6_PCJR:	// pcjr keyboard interrupt
            Memory.phys_writeb(physAddress,0x50);			// push ax
            Memory.phys_writew(physAddress+0x01,0x60e4);		// in al, 0x60
            Memory.phys_writew(physAddress+0x03,0xe03c);		// cmp al, 0xe0
            if (use_cb) {
                Memory.phys_writew(physAddress+0x05,0x0674);	// je skip
                Memory.phys_writeb(physAddress+0x07,0xFE);		//GRP 4
                Memory.phys_writeb(physAddress+0x08,0x38);		//Extra Callback instruction
                Memory.phys_writew(physAddress+0x09,callback);			//The immediate word
                physAddress+=4;
            } else {
                Memory.phys_writew(physAddress+0x05,0x0274);	// je skip
            }
            Memory.phys_writew(physAddress+0x07,0x09cd);		// int 9
            // jump here to (skip):
            Memory.phys_writeb(physAddress+0x09,0xfa);			// cli
            Memory.phys_writew(physAddress+0x0a,0x20b0);		// mov al, 0x20
            Memory.phys_writew(physAddress+0x0c,0x20e6);		// out 0x20, al
            Memory.phys_writeb(physAddress+0x0e,0x58);			// pop ax
            Memory.phys_writeb(physAddress+0x0f,0xcf);			//An IRET Instruction
            return (use_cb?0x14:0x10);
        case CB_MOUSE:
            Memory.phys_writew(physAddress,0x07eb);		// jmp i33hd
            physAddress+=9;
            // jump here to (i33hd):
            if (use_cb) {
                Memory.phys_writeb(physAddress,0xFE);	//GRP 4
                Memory.phys_writeb(physAddress+0x01,0x38);	//Extra Callback instruction
                Memory.phys_writew(physAddress+0x02,callback);		//The immediate word
                physAddress+=4;
            }
            Memory.phys_writeb(physAddress,0xCF);		//An IRET Instruction
            return (use_cb?0x0e:0x0a);
        case CB_INT16:
            Memory.phys_writeb(physAddress,0xFB);		//STI
            if (use_cb) {
                Memory.phys_writeb(physAddress+0x01,0xFE);	//GRP 4
                Memory.phys_writeb(physAddress+0x02,0x38);	//Extra Callback instruction
                Memory.phys_writew(physAddress+0x03,callback);	//The immediate word
                physAddress+=4;
            }
            Memory.phys_writeb(physAddress+0x01,0xCF);		//An IRET Instruction
            for (/*Bitu*/int i=0;i<=0x0b;i++) Memory.phys_writeb(physAddress+0x02+i,0x90);
            Memory.phys_writew(physAddress+0x0e,0xedeb);	//jmp callback
            return (use_cb?0x10:0x0c);
        case CB_INT29:	// fast console output
            if (use_cb) {
                Memory.phys_writeb(physAddress,0xFE);	//GRP 4
                Memory.phys_writeb(physAddress+0x01,0x38);	//Extra Callback instruction
                Memory.phys_writew(physAddress+0x02,callback);		//The immediate word
                physAddress+=4;
            }
            Memory.phys_writeb(physAddress,0x50);	// push ax
            Memory.phys_writeb(physAddress+0x01,0x53);	// push bx
            Memory.phys_writew(physAddress+0x02,0x0eb4);	// mov ah, 0x0e
            Memory.phys_writeb(physAddress+0x04,0xbb);	// mov bx,
            Memory.phys_writew(physAddress+0x05,0x0007);	// 0x0007
            Memory.phys_writew(physAddress+0x07,0x10cd);	// int 10
            Memory.phys_writeb(physAddress+0x09,0x5b);	// pop bx
            Memory.phys_writeb(physAddress+0x0a,0x58);	// pop ax
            Memory.phys_writeb(physAddress+0x0b,0xcf);	//An IRET Instruction
            return (use_cb?0x10:0x0c);
        case CB_HOOKABLE:
            Memory.phys_writeb(physAddress,0xEB);		//jump near
            Memory.phys_writeb(physAddress+0x01,0x03);		//offset
            Memory.phys_writeb(physAddress+0x02,0x90);		//NOP
            Memory.phys_writeb(physAddress+0x03,0x90);		//NOP
            Memory.phys_writeb(physAddress+0x04,0x90);		//NOP
            if (use_cb) {
                Memory.phys_writeb(physAddress+0x05,0xFE);	//GRP 4
                Memory.phys_writeb(physAddress+0x06,0x38);	//Extra Callback instruction
                Memory.phys_writew(physAddress+0x07,callback);		//The immediate word
                physAddress+=4;
            }
            Memory.phys_writeb(physAddress+0x05,0xCB);		//A RETF Instruction
            return (use_cb?0x0a:0x06);
        case CB_TDE_IRET:	// TandyDAC end transfer
            if (use_cb) {
                Memory.phys_writeb(physAddress,0xFE);	//GRP 4
                Memory.phys_writeb(physAddress+0x01,0x38);	//Extra Callback instruction
                Memory.phys_writew(physAddress+0x02,callback);		//The immediate word
                physAddress+=4;
            }
            Memory.phys_writeb(physAddress,0x50);		// push ax
            Memory.phys_writeb(physAddress+0x01,0xb8);		// mov ax, 0x91fb
            Memory.phys_writew(physAddress+0x02,0x91fb);
            Memory.phys_writew(physAddress+0x04,0x15cd);	// int 15
            Memory.phys_writeb(physAddress+0x06,0xfa);		// cli
            Memory.phys_writew(physAddress+0x07,0x20b0);	// mov al, 0x20
            Memory.phys_writew(physAddress+0x09,0x20e6);	// out 0x20, al
            Memory.phys_writeb(physAddress+0x0b,0x58);		// pop ax
            Memory.phys_writeb(physAddress+0x0c,0xcf);		//An IRET Instruction
            return (use_cb?0x11:0x0d);
    /*	case CB_IPXESR:		// IPX ESR
            if (!use_cb) Log.exit("ipx esr must implement a callback handler!");
            Memory.phys_writeb(physAddress+0x00,(Bit8u)0x1e);		// push ds
            Memory.phys_writeb(physAddress+0x01,(Bit8u)0x06);		// push es
            Memory.phys_writew(physAddress+0x02,0xa00f);	// push fs
            Memory.phys_writew(physAddress+0x04,0xa80f);	// push gs
            Memory.phys_writeb(physAddress+0x06,(Bit8u)0x60);		// pusha
            Memory.phys_writeb(physAddress+0x07,(Bit8u)0xFE);		//GRP 4
            Memory.phys_writeb(physAddress+0x08,(Bit8u)0x38);		//Extra Callback instruction
            Memory.phys_writew(physAddress+0x09,callback);	//The immediate word
            Memory.phys_writeb(physAddress+0x0b,(Bit8u)0xCB);		//A RETF Instruction
            return 0x0c;
        case CB_IPXESR_RET:		// IPX ESR return
            if (use_cb) Log.exit("ipx esr return must not implement a callback handler!");
            Memory.phys_writeb(physAddress+0x00,(Bit8u)0xfa);		// cli
            Memory.phys_writew(physAddress+0x01,0x20b0);	// mov al, 0x20
            Memory.phys_writew(physAddress+0x03,0xa0e6);	// out 0xa0, al
            Memory.phys_writew(physAddress+0x05,0x20e6);	// out 0x20, al
            Memory.phys_writeb(physAddress+0x07,(Bit8u)0x61);		// popa
            Memory.phys_writew(physAddress+0x08,0xA90F);	// pop gs
            Memory.phys_writew(physAddress+0x0a,0xA10F);	// pop fs
            Memory.phys_writeb(physAddress+0x0c,(Bit8u)0x07);		// pop es
            Memory.phys_writeb(physAddress+0x0d,(Bit8u)0x1f);		// pop ds
            Memory.phys_writeb(physAddress+0x0e,(Bit8u)0xcf);		//An IRET Instruction
            return 0x0f; */
        case CB_INT21:
            Memory.phys_writeb(physAddress,0xFB);		//STI
            if (use_cb) {
                Memory.phys_writeb(physAddress+0x01,0xFE);	//GRP 4
                Memory.phys_writeb(physAddress+0x02,0x38);	//Extra Callback instruction
                Memory.phys_writew(physAddress+0x03,callback);	//The immediate word
                physAddress+=4;
            }
            Memory.phys_writeb(physAddress+0x01,0xCF);		//An IRET Instruction
            Memory.phys_writeb(physAddress+0x02,0xCB);		//A RETF Instruction
            Memory.phys_writeb(physAddress+0x03,0x51);		// push cx
            Memory.phys_writeb(physAddress+0x04,0xB9);		// mov cx,
            Memory.phys_writew(physAddress+0x05,0x0140);		// 0x140
            Memory.phys_writew(physAddress+0x07,0xFEE2);		// loop $-2
            Memory.phys_writeb(physAddress+0x09,0x59);		// pop cx
            Memory.phys_writeb(physAddress+0x0A,0xCF);		//An IRET Instruction
            return (use_cb?15:11);
        case CB_INT13:
            Memory.phys_writeb(physAddress,0xFB);		//STI
            if (use_cb) {
                Memory.phys_writeb(physAddress+0x01,0xFE);	//GRP 4
                Memory.phys_writeb(physAddress+0x02,0x38);	//Extra Callback instruction
                Memory.phys_writew(physAddress+0x03,callback);	//The immediate word
                physAddress+=4;
            }
            Memory.phys_writeb(physAddress+0x01,0xCF);		//An IRET Instruction
            Memory.phys_writew(physAddress+0x02,0x0ECD);		// int 0e
            Memory.phys_writeb(physAddress+0x04,0xCF);		//An IRET Instruction
            return (use_cb?9:5);
        default:
            Log.exit("CALLBACK:Setup:Illegal type "+type);
        }
        return 0;
    }

    public static boolean CALLBACK_Setup(/*Bitu*/int callback,Handler handler,/*Bitu*/int type,String descr) {
        if (callback>=CB_MAX) return false;
        CALLBACK_SetupExtra(callback,type,CALLBACK_PhysPointer(callback)+0,(handler!=null));
        CallBack_Handlers[callback]=handler;
        CALLBACK_SetDescription(callback,descr);
        return true;
    }

    public static /*Bitu*/int CALLBACK_Setup(/*Bitu*/int callback,Handler handler,/*Bitu*/int type,/*PhysPt*/int addr,String descr) {
        if (callback>=CB_MAX) return 0;
        /*Bitu*/int csize=CALLBACK_SetupExtra(callback,type,addr,(handler!=null));
        if (csize>0) {
            CallBack_Handlers[callback]=handler;
            CALLBACK_SetDescription(callback,descr);
        }
        return csize;
    }

    public static void CALLBACK_RemoveSetup(/*Bitu*/int callback) {
        for (/*Bitu*/int i = 0;i < CB_SIZE;i++) {
            Memory.phys_writeb((int)(CALLBACK_PhysPointer(callback)+i),0x00);
        }
    }

    public /*Bit16u*/int Get_callback() {
		return m_callback;
	}

    public /*RealPt*/int Get_RealPointer() {
		return CALLBACK_RealPointer(m_callback);
	}

    public void Uninstall() {
        if(!installed) return;
        if(m_type == SETUP) {
            if(vectorhandler.installed){
                //See if we are the current handler. if so restore the old one
                if(Memory.RealGetVec(vectorhandler.interrupt) == Get_RealPointer()) {
                    Memory.RealSetVec(vectorhandler.interrupt,vectorhandler.old_vector);
                } else
                    if (Log.level<=LogSeverities.LOG_WARN) Log.log(LogTypes.LOG_MISC,LogSeverities.LOG_WARN,"Interrupt vector changed on "+Integer.toString(vectorhandler.interrupt, 16)+" "+CALLBACK_GetDescription(m_callback));
            }
            CALLBACK_RemoveSetup(m_callback);
        } else if(m_type == SETUPAT){
            Log.exit("Callback:SETUP at not handled yet.");
        } else if(m_type == NONE){
            //Do nothing. Merely DeAllocate the callback
        } else Log.exit("what kind of callback is this!");
        CallBack_Description[m_callback] = null;
        CALLBACK_DeAllocate(m_callback);
        installed = false;
    }

    public void Install(Handler handler,/*Bitu*/int type,String description){
        if(!installed) {
            installed=true;
            m_type=SETUP;
            m_callback=CALLBACK_Allocate();
            CALLBACK_Setup(m_callback,handler,type,description);
        } else Log.exit("Callback handler object already installed");
    }

    public void Install(Handler handler,/*Bitu*/int type,/*PhysPt*/int addr,String description){
        if(!installed) {
            installed=true;
            m_type=SETUP;
            m_callback=CALLBACK_Allocate();
            CALLBACK_Setup(m_callback,handler,type,addr,description);
        } else Log.exit("Callback handler object already installed");
    }

    public void Allocate(Handler handler,String description) {
        if(!installed) {
            installed=true;
            m_type=NONE;
            m_callback=CALLBACK_Allocate();
            CALLBACK_SetDescription(m_callback,description);
            CallBack_Handlers[m_callback]=handler;
        } else Log.exit("Callback handler object already installed");
    }

    public void Set_RealVec(/*Bit8u*/int vec){
        if(!vectorhandler.installed) {
            vectorhandler.installed=true;
            vectorhandler.interrupt=vec;
            vectorhandler.old_vector = Memory.RealSetVec2(vec,Get_RealPointer());
        } else Log.exit ("double usage of vector handler");
    }

    public static Section.SectionFunction CALLBACK_Init = new Section.SectionFunction() {
        public void call(Section section) {
            /*Bitu*/int i;
            for (i=0;i<CB_MAX;i++) {
                CallBack_Handlers[i]=illegal_handler;
            }

            /* Setup the Stop Handler */
            call_stop=CALLBACK_Allocate();
            CallBack_Handlers[call_stop]=stop_handler;
            CALLBACK_SetDescription(call_stop,"stop");
            Memory.phys_writeb((int)CALLBACK_PhysPointer(call_stop),0xFE);
            Memory.phys_writeb((int)(CALLBACK_PhysPointer(call_stop)+1),0x38);
            Memory.phys_writew((int)(CALLBACK_PhysPointer(call_stop)+2),call_stop);

            /* Setup the idle handler */
            call_idle=CALLBACK_Allocate();
            CallBack_Handlers[call_idle]=stop_handler;
            CALLBACK_SetDescription(call_idle,"idle");
            for (i=0;i<=11;i++) Memory.phys_writeb((int)(CALLBACK_PhysPointer(call_idle)+i),0x90);
            Memory.phys_writeb((int)(CALLBACK_PhysPointer(call_idle)+12),0xFE);
            Memory.phys_writeb((int)(CALLBACK_PhysPointer(call_idle)+13),0x38);
            Memory.phys_writew((int)(CALLBACK_PhysPointer(call_idle)+14),call_idle);

            /* Default handlers for unhandled interrupts that have to be non-null */
            int call_default=CALLBACK_Allocate();
            CALLBACK_Setup(call_default,default_handler,CB_IRET,"default");
            int call_default2=CALLBACK_Allocate();
            CALLBACK_Setup(call_default2,default_handler,CB_IRET,"default");

            /* Only setup default handler for first part of interrupt table */
            for (/*Bit16u*/int ct=0;ct<0x60;ct++) {
                Memory.real_writed(0,ct*4,CALLBACK_RealPointer(call_default));
            }
            for (/*Bit16u*/int ct=0x66;ct<0x70;ct++) {
                Memory.real_writed(0,ct*4,CALLBACK_RealPointer(call_default));
            }
            /* Setup block of 0xCD 0xxx instructions */
            /*PhysPt*/int rint_base=CALLBACK_GetBase()+CB_MAX*CB_SIZE;
            for (i=0;i<=0xff;i++) {
                Memory.phys_writeb(rint_base,0xCD);
                Memory.phys_writeb(rint_base+1,i);
                Memory.phys_writeb(rint_base+2,0xFE);
                Memory.phys_writeb(rint_base+3,0x38);
                Memory.phys_writew(rint_base+4,call_stop);
                rint_base+=6;

            }
            // setup a few interrupt handlers that point to bios IRETs by default
            //Memory.real_writed(0,0x0e*4,CALLBACK_RealPointer(call_default2));	//design your own railroad
            //Memory.real_writed(0,0x66*4,CALLBACK_RealPointer(call_default));	//war2d
            //Memory.real_writed(0,0x67*4,CALLBACK_RealPointer(call_default));
            //Memory.real_writed(0,0x68*4,CALLBACK_RealPointer(call_default));
            //Memory.real_writed(0,0x5c*4,CALLBACK_RealPointer(call_default));	//Network stuff
            //real_writed(0,0xf*4,0); some games don't like it

            call_priv_io=CALLBACK_Allocate();

            // virtualizable in-out opcodes
            Memory.phys_writeb((int)(CALLBACK_PhysPointer(call_priv_io)+0x00),0xec);	// in al, dx
            Memory.phys_writeb((int)(CALLBACK_PhysPointer(call_priv_io)+0x01),0xcb);	// retf
            Memory.phys_writeb((int)(CALLBACK_PhysPointer(call_priv_io)+0x02),0xed);	// in ax, dx
            Memory.phys_writeb((int)(CALLBACK_PhysPointer(call_priv_io)+0x03),0xcb);	// retf
            Memory.phys_writeb((int)(CALLBACK_PhysPointer(call_priv_io)+0x04),0x66);	// in eax, dx
            Memory.phys_writeb((int)(CALLBACK_PhysPointer(call_priv_io)+0x05),0xed);
            Memory.phys_writeb((int)(CALLBACK_PhysPointer(call_priv_io)+0x06),0xcb);	// retf

            Memory.phys_writeb((int)(CALLBACK_PhysPointer(call_priv_io)+0x08),0xee);	// out dx, al
            Memory.phys_writeb((int)(CALLBACK_PhysPointer(call_priv_io)+0x09),0xcb);	// retf
            Memory.phys_writeb((int)(CALLBACK_PhysPointer(call_priv_io)+0x0a),0xef);	// out dx, ax
            Memory.phys_writeb((int)(CALLBACK_PhysPointer(call_priv_io)+0x0b),0xcb);	// retf
            Memory.phys_writeb((int)(CALLBACK_PhysPointer(call_priv_io)+0x0c),0x66);	// out dx, eax
            Memory.phys_writeb((int)(CALLBACK_PhysPointer(call_priv_io)+0x0d),0xef);
            Memory.phys_writeb((int)(CALLBACK_PhysPointer(call_priv_io)+0x0e),0xcb);	// retf
        }
    };
}
