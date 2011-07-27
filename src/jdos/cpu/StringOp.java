package jdos.cpu;

import jdos.cpu.core_normal.Prefix_helpers;
import jdos.hardware.IO;
import jdos.hardware.Memory;
import jdos.misc.Log;
import jdos.types.LogSeverities;
import jdos.types.LogTypes;

public class StringOp extends Prefix_helpers {
    static public final int R_OUTSB=1;
    static public final int R_OUTSW=2;
    static public final int R_OUTSD=3;
    static public final int R_INSB=4;
    static public final int R_INSW=5;
    static public final int R_INSD=6;
    static public final int R_MOVSB=7;
    static public final int R_MOVSW=8;
    static public final int R_MOVSD=9;
    static public final int R_LODSB=10;
    static public final int R_LODSW=11;
    static public final int R_LODSD=12;
    static public final int R_STOSB=13;
    static public final int R_STOSW=14;
    static public final int R_STOSD=15;
    static public final int R_SCASB=16;
    static public final int R_SCASW=17;
    static public final int R_SCASD=18;
    static public final int R_CMPSB=19;
    static public final int R_CMPSW=20;
    static public final int R_CMPSD=21;

    //#define LoadD(_BLAH) _BLAH

    static protected void DoString(int type) {
        DoString(prefixes, type);
    }

    static public int inString = 0;

    static public void DoString(int prefixes, int type) {
        if (true) {
            try {
                inString++;
                if ((prefixes & PREFIX_ADDR)==0)
                    DoString16(prefixes, type);
                 else
                    DoString32(prefixes, type);
            } finally {
                inString--;
            }
            return;
        }
        inString++;
        try {
            /*PhysPt*/long  si_base,di_base;
            /*Bitu*/long	si_index,di_index;
            /*Bitu*/long	add_mask;
            /*Bitu*/long	not_add_mask;
            /*Bitu*/long	count,count_left=0;
            /*Bits*/int	add_index;

            si_base=base_ds;
            di_base=CPU.Segs_ESphys;
            add_mask=AddrMaskTable[prefixes & PREFIX_ADDR];
            not_add_mask = ~add_mask & 0xFFFFFFFFl;
            si_index=reg_esi.dword() & add_mask;
            di_index=reg_edi.dword() & add_mask;
            count=reg_ecx.dword() & add_mask;
            if ((prefixes & PREFIX_REP)==0) {
                count=1;
            } else {
                CPU.CPU_Cycles++;
                /* Calculate amount of ops to do before cycles run out */
//                if (CPU.cpudecoder == Core_normal.CPU_Core_Normal_Run) {
//                    // Some games, like the intro to Falcon 3, really need this
//                    if ((count>CPU.CPU_Cycles) && (type<R_SCASB)) {
//                        count_left=count-CPU.CPU_Cycles;
//                        count=CPU.CPU_Cycles;
//                        CPU.CPU_Cycles=0;
//                        LOADIP();		//RESET IP to the start
//                    } else {
//                        /* Won't interrupt scas and cmps instruction since they can interrupt themselves */
//                        if ((count<=1) && (CPU.CPU_Cycles<=1)) CPU.CPU_Cycles--;
//                        else if (type<R_SCASB) CPU.CPU_Cycles-=count;
//                        count_left=0;
//                    }
//                }
            }
            add_index=CPU.cpu.direction;
            if (count!=0) switch (type) {
            case R_OUTSB:
                for (;count>0;count--) {
                    IO.IO_WriteB(reg_edx.word(), Memory.mem_readb(si_base+si_index));
                    si_index=(si_index+add_index) & add_mask;
                }
                break;
            case R_OUTSW:
                add_index<<=1;
                for (;count>0;count--) {
                    IO.IO_WriteW(reg_edx.word(),Memory.mem_readw(si_base+si_index));
                    si_index=(si_index+add_index) & add_mask;
                }
                break;
            case R_OUTSD:
                add_index<<=2;
                for (;count>0;count--) {
                    IO.IO_WriteD(reg_edx.word(),Memory.mem_readd(si_base+si_index));
                    si_index=(si_index+add_index) & add_mask;
                }
                break;
            case R_INSB:
                for (;count>0;count--) {
                    Memory.mem_writeb(di_base+di_index,IO.IO_ReadB(reg_edx.word()));
                    di_index=(di_index+add_index) & add_mask;
                }
                break;
            case R_INSW:
                add_index<<=1;
                for (;count>0;count--) {
                    Memory.mem_writew(di_base+di_index,IO.IO_ReadW(reg_edx.word()));
                    di_index=(di_index+add_index) & add_mask;
                }
                break;
            case R_INSD:
                add_index<<=2;
                for (;count>0;count--) {
                    Memory.mem_writed(di_base+di_index,IO.IO_ReadD(reg_edx.word()));
                    di_index=(di_index+add_index) & add_mask;
                }
                break;
            case R_STOSB:
                for (;count>0;count--) {
                    Memory.mem_writeb(di_base+di_index,reg_eax.low());
                    di_index=(di_index+add_index) & add_mask;
                }
                break;
            case R_STOSW:
                add_index<<=1;
                for (;count>0;count--) {
                    Memory.mem_writew(di_base+di_index,reg_eax.word());
                    di_index=(di_index+add_index) & add_mask;
                }
                break;
            case R_STOSD:
                add_index<<=2;
                for (;count>0;count--) {
                    Memory.mem_writed(di_base+di_index,reg_eax.dword());
                    di_index=(di_index+add_index) & add_mask;
                }
                break;
            case R_MOVSB:
                for (;count>0;count--) {
                    Memory.mem_writeb(di_base+di_index,Memory.mem_readb(si_base+si_index));
                    di_index=(di_index+add_index) & add_mask;
                    si_index=(si_index+add_index) & add_mask;
                }
                break;
            case R_MOVSW:
                add_index<<=1;
                for (;count>0;count--) {
                    Memory.mem_writew(di_base+di_index,Memory.mem_readw(si_base+si_index));
                    di_index=(di_index+add_index) & add_mask;
                    si_index=(si_index+add_index) & add_mask;
                }
                break;
            case R_MOVSD:
    //            long dst = di_base+di_index;
    //            long dst_start = (dst & 0xFFFFF000l) + 3;
    //            long dst_stop = dst_start + 4096 - 3;
    //            int dst_index = (int)(di_base- Paging.getDirectIndex(dst));
    //            add_index<<=2;
    //            for (;count>0;count--) {
    //                dst = di_base+di_index;
    //                if (dst<dst_stop && dst>dst_stop) {
    //                    dst_index = Paging.getDirectIndex(dst);
    //                    dst_start = (dst & 0xFFFFF000l) + 3;
    //                    dst_stop = dst_start + 4096 - 3;
    //                }
    //                Memory.host_writed((int)(di_index+dst_index),Memory.mem_readd(si_base+si_index));
    //                di_index=(di_index+add_index) & add_mask;
    //                si_index=(si_index+add_index) & add_mask;
    //            }
                add_index<<=2;
                for (;count>0;count--) {
                    Memory.mem_writed(di_base+di_index,Memory.mem_readd(si_base+si_index));
                    di_index=(di_index+add_index) & add_mask;
                    si_index=(si_index+add_index) & add_mask;
                }
                break;
            case R_LODSB:
                for (;count>0;count--) {
                    reg_eax.low(Memory.mem_readb(si_base+si_index));
                    si_index=(si_index+add_index) & add_mask;
                }
                break;
            case R_LODSW:
                add_index<<=1;
                for (;count>0;count--) {
                    reg_eax.word(Memory.mem_readw(si_base+si_index));
                    si_index=(si_index+add_index) & add_mask;
                }
                break;
            case R_LODSD:
                add_index<<=2;
                for (;count>0;count--) {
                    reg_eax.dword(Memory.mem_readd(si_base+si_index));
                    si_index=(si_index+add_index) & add_mask;
                }
                break;
            case R_SCASB:
                {
                    /*Bit8u*/short val2=0;
                    for (;count>0;) {
                        count--;CPU.CPU_Cycles--;
                        val2=Memory.mem_readb(di_base+di_index);
                        di_index=(di_index+add_index) & add_mask;
                        if ((reg_eax.low()==val2)!=rep_zero) break;
                    }
                    CMPB(val2,reg_eax.low());
                }
                break;
            case R_SCASW:
                {
                    add_index<<=1;/*Bit16u*/int val2=0;
                    for (;count>0;) {
                        count--;CPU.CPU_Cycles--;
                        val2=Memory.mem_readw(di_base+di_index);
                        di_index=(di_index+add_index) & add_mask;
                        if ((reg_eax.word()==val2)!=rep_zero) break;
                    }
                    CMPW(val2,reg_eax.word());
                }
                break;
            case R_SCASD:
                {
                    add_index<<=2;/*Bit32u*/long val2=0;
                    for (;count>0;) {
                        count--;CPU.CPU_Cycles--;
                        val2=Memory.mem_readd(di_base+di_index);
                        di_index=(di_index+add_index) & add_mask;
                        if ((reg_eax.dword()==val2)!=rep_zero) break;
                    }
                    CMPD(val2,reg_eax.dword());
                }
                break;
            case R_CMPSB:
                {
                    /*Bit8u*/short val1=0,val2=0;
                    for (;count>0;) {
                        count--;CPU.CPU_Cycles--;
                        val1=Memory.mem_readb(si_base+si_index);
                        val2=Memory.mem_readb(di_base+di_index);
                        si_index=(si_index+add_index) & add_mask;
                        di_index=(di_index+add_index) & add_mask;
                        if ((val1==val2)!=rep_zero) break;
                    }
                    CMPB(val2,val1);
                }
                break;
            case R_CMPSW:
                {
                    add_index<<=1;/*Bit16u*/int val1=0,val2=0;
                    for (;count>0;) {
                        count--;CPU.CPU_Cycles--;
                        val1=Memory.mem_readw(si_base+si_index);
                        val2=Memory.mem_readw(di_base+di_index);
                        si_index=(si_index+add_index) & add_mask;
                        di_index=(di_index+add_index) & add_mask;
                        if ((val1==val2)!=rep_zero) break;
                    }
                    CMPW(val2,val1);
                }
                break;
            case R_CMPSD:
                {
                    add_index<<=2;/*Bit32u*/long val1=0,val2=0;
                    for (;count>0;) {
                        count--;CPU.CPU_Cycles--;
                        val1=Memory.mem_readd(si_base+si_index);
                        val2=Memory.mem_readd(di_base+di_index);
                        si_index=(si_index+add_index) & add_mask;
                        di_index=(di_index+add_index) & add_mask;
                        if ((val1==val2)!=rep_zero) break;
                    }
                    CMPD(val2,val1);
                }
                break;
            default:
                if (Log.level<=LogSeverities.LOG_ERROR) Log.log(LogTypes.LOG_CPU, LogSeverities.LOG_ERROR,"Unhandled string op "+type);
                Log.exit("Unhandled string op "+type);
            }
            /* Clean up after certain amount of instructions */
            reg_esi.dword(reg_esi.dword() & not_add_mask);
            reg_esi.dword(reg_esi.dword() | (si_index & add_mask));
            reg_edi.dword(reg_edi.dword() & not_add_mask);
            reg_edi.dword(reg_edi.dword() | (di_index & add_mask));
            if ((prefixes & PREFIX_REP)!=0) {
                count+=count_left;
                reg_ecx.dword(reg_ecx.dword() & not_add_mask);
                reg_ecx.dword(reg_ecx.dword() | (count & add_mask));
            }
        } finally {
            inString--;
        }
    }

    static public void DoString16(int prefixes, int type) {
        /*PhysPt*/long  si_base,di_base;
        /*Bitu*/long	count;
        /*Bits*/int	add_index;

        si_base=base_ds;
        di_base=CPU.Segs_ESphys;
        count=reg_ecx.word();
        if ((prefixes & PREFIX_REP)==0) {
            count=1;
        } else {
            CPU.CPU_Cycles++;
            /* Calculate amount of ops to do before cycles run out */
//            if (CPU.cpudecoder == Core_normal.CPU_Core_Normal_Run) {
//                // Some games, like the intro to Falcon 3, really need this
//                if ((count>CPU.CPU_Cycles) && (type<R_SCASB)) {
//                    count=CPU.CPU_Cycles;
//                    CPU.CPU_Cycles=0;
//                    LOADIP();		//RESET IP to the start
//                } else {
//                    /* Won't interrupt scas and cmps instruction since they can interrupt themselves */
//                    if ((count<=1) && (CPU.CPU_Cycles<=1)) CPU.CPU_Cycles--;
//                    else if (type<R_SCASB) CPU.CPU_Cycles-=count;
//                }
//            }
        }
        add_index=CPU.cpu.direction;
        if (count!=0) switch (type) {
        case R_OUTSB:
            for (;count>0;count--) {
                IO.IO_WriteB(reg_edx.word(), Memory.mem_readb(si_base+reg_esi.word()));
                reg_esi.word(reg_esi.word()+add_index);
                if ((prefixes & PREFIX_REP)!=0) reg_ecx.word_dec();
            }
            break;
        case R_OUTSW:
            add_index<<=1;
            for (;count>0;count--) {
                IO.IO_WriteW(reg_edx.word(),Memory.mem_readw(si_base+reg_esi.word()));
                reg_esi.word(reg_esi.word()+add_index);
                if ((prefixes & PREFIX_REP)!=0) reg_ecx.word_dec();
            }
            break;
        case R_OUTSD:
            add_index<<=2;
            for (;count>0;count--) {
                IO.IO_WriteD(reg_edx.word(),Memory.mem_readd(si_base+reg_esi.word()));
                reg_esi.word(reg_esi.word()+add_index);
                if ((prefixes & PREFIX_REP)!=0) reg_ecx.word_dec();
            }
            break;
        case R_INSB:
            for (;count>0;count--) {
                Memory.mem_writeb(di_base+reg_edi.word(),IO.IO_ReadB(reg_edx.word()));
                reg_edi.word(reg_edi.word()+add_index);
                if ((prefixes & PREFIX_REP)!=0) reg_ecx.word_dec();
            }
            break;
        case R_INSW:
            add_index<<=1;
            for (;count>0;count--) {
                Memory.mem_writew(di_base+reg_edi.word(),IO.IO_ReadW(reg_edx.word()));
                reg_edi.word(reg_edi.word()+add_index);
                if ((prefixes & PREFIX_REP)!=0) reg_ecx.word_dec();
            }
            break;
        case R_INSD:
            add_index<<=2;
            for (;count>0;count--) {
                Memory.mem_writed(di_base+reg_edi.word(),IO.IO_ReadD(reg_edx.word()));
                reg_edi.word(reg_edi.word()+add_index);
                if ((prefixes & PREFIX_REP)!=0) reg_ecx.word_dec();
            }
            break;
        case R_STOSB:
            for (;count>0;count--) {
                Memory.mem_writeb(di_base+reg_edi.word(),reg_eax.low());
                reg_edi.word(reg_edi.word()+add_index);
                if ((prefixes & PREFIX_REP)!=0) reg_ecx.word_dec();
            }
            break;
        case R_STOSW:
            add_index<<=1;
            for (;count>0;count--) {
                Memory.mem_writew(di_base+reg_edi.word(),reg_eax.word());
                reg_edi.word(reg_edi.word()+add_index);
                if ((prefixes & PREFIX_REP)!=0) reg_ecx.word_dec();
            }
            break;
        case R_STOSD:
            add_index<<=2;
            for (;count>0;count--) {
                Memory.mem_writed(di_base+reg_edi.word(),reg_eax.dword());
                reg_edi.word(reg_edi.word()+add_index);
                if ((prefixes & PREFIX_REP)!=0) reg_ecx.word_dec();
            }
            break;
        case R_MOVSB:
            for (;count>0;count--) {
                Memory.mem_writeb(di_base+reg_edi.word(),Memory.mem_readb(si_base+reg_esi.word()));
                reg_edi.word(reg_edi.word()+add_index);
                reg_esi.word(reg_esi.word()+add_index);
                if ((prefixes & PREFIX_REP)!=0) reg_ecx.word_dec();
            }
            break;
        case R_MOVSW:
            add_index<<=1;
            for (;count>0;count--) {
                Memory.mem_writew(di_base+reg_edi.word(),Memory.mem_readw(si_base+reg_esi.word()));
                reg_edi.word(reg_edi.word()+add_index);
                reg_esi.word(reg_esi.word()+add_index);
                if ((prefixes & PREFIX_REP)!=0) reg_ecx.word_dec();
            }
            break;
        case R_MOVSD:
//            long dst = di_base+di_index;
//            long dst_start = (dst & 0xFFFFF000l) + 3;
//            long dst_stop = dst_start + 4096 - 3;
//            int dst_index = (int)(di_base- Paging.getDirectIndex(dst));
//            add_index<<=2;
//            for (;count>0;count--) {
//                dst = di_base+di_index;
//                if (dst<dst_stop && dst>dst_stop) {
//                    dst_index = Paging.getDirectIndex(dst);
//                    dst_start = (dst & 0xFFFFF000l) + 3;
//                    dst_stop = dst_start + 4096 - 3;
//                }
//                Memory.host_writed((int)(di_index+dst_index),Memory.mem_readd(si_base+si_index));
//                di_index=(di_index+add_index) & add_mask;
//                si_index=(si_index+add_index) & add_mask;
//            }
            add_index<<=2;
            for (;count>0;count--) {
                Memory.mem_writed(di_base+reg_edi.word(),Memory.mem_readd(si_base+reg_esi.word()));
                reg_edi.word(reg_edi.word()+add_index);
                reg_esi.word(reg_esi.word()+add_index);
                if ((prefixes & PREFIX_REP)!=0) reg_ecx.word_dec();
            }
            break;
        case R_LODSB:
            for (;count>0;count--) {
                reg_eax.low(Memory.mem_readb(si_base+reg_esi.word()));
                reg_esi.word(reg_esi.word()+add_index);
                if ((prefixes & PREFIX_REP)!=0) reg_ecx.word_dec();
            }
            break;
        case R_LODSW:
            add_index<<=1;
            for (;count>0;count--) {
                reg_eax.word(Memory.mem_readw(si_base+reg_esi.word()));
                reg_esi.word(reg_esi.word()+add_index);
                if ((prefixes & PREFIX_REP)!=0) reg_ecx.word_dec();
            }
            break;
        case R_LODSD:
            add_index<<=2;
            for (;count>0;count--) {
                reg_eax.dword(Memory.mem_readd(si_base+reg_esi.word()));
                reg_esi.word(reg_esi.word()+add_index);
                if ((prefixes & PREFIX_REP)!=0) reg_ecx.word_dec();
            }
            break;
        case R_SCASB:
            {
                /*Bit8u*/short val2=0;
                for (;count>0;) {
                    count--;CPU.CPU_Cycles--;
                    val2=Memory.mem_readb(di_base+reg_edi.word());
                    reg_edi.word(reg_edi.word()+add_index);
                    if ((prefixes & PREFIX_REP)!=0) reg_ecx.word_dec();
                    if ((reg_eax.low()==val2)!=rep_zero) break;
                }
                CMPB(val2,reg_eax.low());
            }
            break;
        case R_SCASW:
            {
                add_index<<=1;/*Bit16u*/int val2=0;
                for (;count>0;) {
                    count--;CPU.CPU_Cycles--;
                    val2=Memory.mem_readw(di_base+reg_edi.word());
                    reg_edi.word(reg_edi.word()+add_index);
                    if ((prefixes & PREFIX_REP)!=0) reg_ecx.word_dec();
                    if ((reg_eax.word()==val2)!=rep_zero) break;
                }
                CMPW(val2,reg_eax.word());
            }
            break;
        case R_SCASD:
            {
                add_index<<=2;/*Bit32u*/long val2=0;
                for (;count>0;) {
                    count--;CPU.CPU_Cycles--;
                    val2=Memory.mem_readd(di_base+reg_edi.word());
                    reg_edi.word(reg_edi.word()+add_index);
                    if ((prefixes & PREFIX_REP)!=0) reg_ecx.word_dec();
                    if ((reg_eax.dword()==val2)!=rep_zero) break;
                }
                CMPD(val2,reg_eax.dword());
            }
            break;
        case R_CMPSB:
            {
                /*Bit8u*/short val1=0,val2=0;
                for (;count>0;) {
                    count--;CPU.CPU_Cycles--;
                    val1=Memory.mem_readb(si_base+reg_esi.word());
                    val2=Memory.mem_readb(di_base+reg_edi.word());
                    reg_esi.word(reg_esi.word()+add_index);
                    reg_edi.word(reg_edi.word()+add_index);
                    if ((prefixes & PREFIX_REP)!=0) reg_ecx.word_dec();
                    if ((val1==val2)!=rep_zero) break;
                }
                CMPB(val2,val1);
            }
            break;
        case R_CMPSW:
            {
                add_index<<=1;/*Bit16u*/int val1=0,val2=0;
                for (;count>0;) {
                    count--;CPU.CPU_Cycles--;
                    val1=Memory.mem_readw(si_base+reg_esi.word());
                    val2=Memory.mem_readw(di_base+reg_edi.word());
                    reg_esi.word(reg_esi.word()+add_index);
                    reg_edi.word(reg_edi.word()+add_index);
                    if ((prefixes & PREFIX_REP)!=0) reg_ecx.word_dec();
                    if ((val1==val2)!=rep_zero) break;
                }
                CMPW(val2,val1);
            }
            break;
        case R_CMPSD:
            {
                add_index<<=2;/*Bit32u*/long val1=0,val2=0;
                for (;count>0;) {
                    count--;CPU.CPU_Cycles--;
                    val1=Memory.mem_readd(si_base+reg_esi.word());
                    val2=Memory.mem_readd(di_base+reg_edi.word());
                    reg_esi.word(reg_esi.word()+add_index);
                    reg_edi.word(reg_edi.word()+add_index);
                    if ((prefixes & PREFIX_REP)!=0) reg_ecx.word_dec();
                    if ((val1==val2)!=rep_zero) break;
                }
                CMPD(val2,val1);
            }
            break;
        default:
            if (Log.level<=LogSeverities.LOG_ERROR) Log.log(LogTypes.LOG_CPU, LogSeverities.LOG_ERROR,"Unhandled string op "+type);
            Log.exit("Unhandled string op "+type);
        }
    }

    static public void DoString32(int prefixes, int type) {
        /*PhysPt*/long  si_base,di_base;
        /*Bitu*/long	count;
        /*Bits*/int	add_index;

        si_base=base_ds;
        di_base=CPU.Segs_ESphys;
        count=reg_ecx.dword();
        if ((prefixes & PREFIX_REP)==0) {
            count=1;
        } else {
            CPU.CPU_Cycles++;
            /* Calculate amount of ops to do before cycles run out */
//            if (CPU.cpudecoder == Core_normal.CPU_Core_Normal_Run) {
//                // Some games, like the intro to Falcon 3, really need this
//                if ((count>CPU.CPU_Cycles) && (type<R_SCASB)) {
//                    count=CPU.CPU_Cycles;
//                    CPU.CPU_Cycles=0;
//                    LOADIP();		//RESET IP to the start
//                } else {
//                    /* Won't interrupt scas and cmps instruction since they can interrupt themselves */
//                    if ((count<=1) && (CPU.CPU_Cycles<=1)) CPU.CPU_Cycles--;
//                    else if (type<R_SCASB) CPU.CPU_Cycles-=count;
//                }
//            }
        }
        add_index=CPU.cpu.direction;
        if (count!=0) switch (type) {
        case R_OUTSB:
            for (;count>0;count--) {
                IO.IO_WriteB(reg_edx.word(), Memory.mem_readb(si_base+reg_esi.dword()));
                reg_esi.dword(reg_esi.dword()+add_index);
                if ((prefixes & PREFIX_REP)!=0) reg_ecx.dword_dec();
            }
            break;
        case R_OUTSW:
            add_index<<=1;
            for (;count>0;count--) {
                IO.IO_WriteW(reg_edx.word(),Memory.mem_readw(si_base+reg_esi.dword()));
                reg_esi.dword(reg_esi.dword()+add_index);
                if ((prefixes & PREFIX_REP)!=0) reg_ecx.dword_dec();
            }
            break;
        case R_OUTSD:
            add_index<<=2;
            for (;count>0;count--) {
                IO.IO_WriteD(reg_edx.word(),Memory.mem_readd(si_base+reg_esi.dword()));
                reg_esi.dword(reg_esi.dword()+add_index);
                if ((prefixes & PREFIX_REP)!=0) reg_ecx.dword_dec();
            }
            break;
        case R_INSB:
            for (;count>0;count--) {
                Memory.mem_writeb(di_base+reg_edi.dword(),IO.IO_ReadB(reg_edx.word()));
                reg_edi.dword(reg_edi.dword()+add_index);
                if ((prefixes & PREFIX_REP)!=0) reg_ecx.dword_dec();
            }
            break;
        case R_INSW:
            add_index<<=1;
            for (;count>0;count--) {
                Memory.mem_writew(di_base+reg_edi.dword(),IO.IO_ReadW(reg_edx.word()));
                reg_edi.dword(reg_edi.dword()+add_index);
                if ((prefixes & PREFIX_REP)!=0) reg_ecx.dword_dec();
            }
            break;
        case R_INSD:
            add_index<<=1;
            for (;count>0;count--) {
                Memory.mem_writed(di_base+reg_edi.dword(),IO.IO_ReadD(reg_edx.word()));
                reg_edi.dword(reg_edi.dword()+add_index);
                if ((prefixes & PREFIX_REP)!=0) reg_ecx.dword_dec();
            }
            break;
        case R_STOSB:
            for (;count>0;count--) {
                Memory.mem_writeb(di_base+reg_edi.dword(),reg_eax.low());
                reg_edi.dword(reg_edi.dword()+add_index);
                if ((prefixes & PREFIX_REP)!=0) reg_ecx.dword_dec();
            }
            break;
        case R_STOSW:
            add_index<<=1;
            for (;count>0;count--) {
                Memory.mem_writew(di_base+reg_edi.dword(),reg_eax.word());
                reg_edi.dword(reg_edi.dword()+add_index);
                if ((prefixes & PREFIX_REP)!=0) reg_ecx.dword_dec();
            }
            break;
        case R_STOSD:
            add_index<<=2;
            for (;count>0;count--) {
                Memory.mem_writed(di_base+reg_edi.dword(),reg_eax.dword());
                reg_edi.dword(reg_edi.dword()+add_index);
                if ((prefixes & PREFIX_REP)!=0) reg_ecx.dword_dec();
            }
            break;
        case R_MOVSB:
            for (;count>0;count--) {
                Memory.mem_writeb(di_base+reg_edi.dword(),Memory.mem_readb(si_base+reg_esi.dword()));
                reg_edi.dword(reg_edi.dword()+add_index);
                reg_esi.dword(reg_esi.dword()+add_index);
                if ((prefixes & PREFIX_REP)!=0) reg_ecx.dword_dec();
            }
            break;
        case R_MOVSW:
            add_index<<=1;
            for (;count>0;count--) {
                Memory.mem_writew(di_base+reg_edi.dword(),Memory.mem_readw(si_base+reg_esi.dword()));
                reg_edi.dword(reg_edi.dword()+add_index);
                reg_esi.dword(reg_esi.dword()+add_index);
                if ((prefixes & PREFIX_REP)!=0) reg_ecx.dword_dec();
            }
            break;
        case R_MOVSD:
//            long dst = di_base+di_index;
//            long dst_start = (dst & 0xFFFFF000l) + 3;
//            long dst_stop = dst_start + 4096 - 3;
//            int dst_index = (int)(di_base- Paging.getDirectIndex(dst));
//            add_index<<=2;
//            for (;count>0;count--) {
//                dst = di_base+di_index;
//                if (dst<dst_stop && dst>dst_stop) {
//                    dst_index = Paging.getDirectIndex(dst);
//                    dst_start = (dst & 0xFFFFF000l) + 3;
//                    dst_stop = dst_start + 4096 - 3;
//                }
//                Memory.host_writed((int)(di_index+dst_index),Memory.mem_readd(si_base+si_index));
//                di_index=(di_index+add_index) & add_mask;
//                si_index=(si_index+add_index) & add_mask;
//            }
            add_index<<=2;
            for (;count>0;count--) {
                Memory.mem_writed(di_base+reg_edi.dword(),Memory.mem_readd(si_base+reg_esi.dword()));
                reg_edi.dword(reg_edi.dword()+add_index);
                reg_esi.dword(reg_esi.dword()+add_index);
                if ((prefixes & PREFIX_REP)!=0) reg_ecx.dword_dec();
            }
            break;
        case R_LODSB:
            for (;count>0;count--) {
                reg_eax.low(Memory.mem_readb(si_base+reg_esi.dword()));
                reg_esi.dword(reg_esi.dword()+add_index);
                if ((prefixes & PREFIX_REP)!=0) reg_ecx.dword_dec();
            }
            break;
        case R_LODSW:
            add_index<<=1;
            for (;count>0;count--) {
                reg_eax.word(Memory.mem_readw(si_base+reg_esi.dword()));
                reg_esi.dword(reg_esi.dword()+add_index);
                if ((prefixes & PREFIX_REP)!=0) reg_ecx.dword_dec();
            }
            break;
        case R_LODSD:
            add_index<<=2;
            for (;count>0;count--) {
                reg_eax.dword(Memory.mem_readd(si_base+reg_esi.dword()));
                reg_esi.dword(reg_esi.dword()+add_index);
                if ((prefixes & PREFIX_REP)!=0) reg_ecx.dword_dec();
            }
            break;
        case R_SCASB:
            {
                /*Bit8u*/short val2=0;
                for (;count>0;) {
                    count--;CPU.CPU_Cycles--;
                    val2=Memory.mem_readb(di_base+reg_edi.dword());
                    reg_edi.dword(reg_edi.dword()+add_index);
                    if ((prefixes & PREFIX_REP)!=0) reg_ecx.dword_dec();
                    if ((reg_eax.low()==val2)!=rep_zero) break;
                }
                CMPB(val2,reg_eax.low());
            }
            break;
        case R_SCASW:
            {
                add_index<<=1;/*Bit16u*/int val2=0;
                for (;count>0;) {
                    count--;CPU.CPU_Cycles--;
                    val2=Memory.mem_readw(di_base+reg_edi.dword());
                    reg_edi.dword(reg_edi.dword()+add_index);
                    if ((prefixes & PREFIX_REP)!=0) reg_ecx.dword_dec();
                    if ((reg_eax.word()==val2)!=rep_zero) break;
                }
                CMPW(val2,reg_eax.word());
            }
            break;
        case R_SCASD:
            {
                add_index<<=2;/*Bit32u*/long val2=0;
                for (;count>0;) {
                    count--;CPU.CPU_Cycles--;
                    val2=Memory.mem_readd(di_base+reg_edi.dword());
                    reg_edi.dword(reg_edi.dword()+add_index);
                    if ((prefixes & PREFIX_REP)!=0) reg_ecx.dword_dec();
                    if ((reg_eax.dword()==val2)!=rep_zero) break;
                }
                CMPD(val2,reg_eax.dword());
            }
            break;
        case R_CMPSB:
            {
                /*Bit8u*/short val1=0,val2=0;
                for (;count>0;) {
                    count--;CPU.CPU_Cycles--;
                    val1=Memory.mem_readb(si_base+reg_esi.dword());
                    val2=Memory.mem_readb(di_base+reg_edi.dword());
                    reg_esi.dword(reg_esi.dword()+add_index);
                    reg_edi.dword(reg_edi.dword()+add_index);
                    if ((prefixes & PREFIX_REP)!=0) reg_ecx.dword_dec();
                    if ((val1==val2)!=rep_zero) break;
                }
                CMPB(val2,val1);
            }
            break;
        case R_CMPSW:
            {
                add_index<<=1;/*Bit16u*/int val1=0,val2=0;
                for (;count>0;) {
                    count--;CPU.CPU_Cycles--;
                    val1=Memory.mem_readw(si_base+reg_esi.dword());
                    val2=Memory.mem_readw(di_base+reg_edi.dword());
                    reg_esi.dword(reg_esi.dword()+add_index);
                    reg_edi.dword(reg_edi.dword()+add_index);
                    if ((prefixes & PREFIX_REP)!=0) reg_ecx.dword_dec();
                    if ((val1==val2)!=rep_zero) break;
                }
                CMPW(val2,val1);
            }
            break;
        case R_CMPSD:
            {
                add_index<<=2;/*Bit32u*/long val1=0,val2=0;
                for (;count>0;) {
                    count--;CPU.CPU_Cycles--;
                    val1=Memory.mem_readd(si_base+reg_esi.dword());
                    val2=Memory.mem_readd(di_base+reg_edi.dword());
                    reg_esi.dword(reg_esi.dword()+add_index);
                    reg_edi.dword(reg_edi.dword()+add_index);
                    if ((prefixes & PREFIX_REP)!=0) reg_ecx.dword_dec();
                    if ((val1==val2)!=rep_zero) break;
                }
                CMPD(val2,val1);
            }
            break;
        default:
            if (Log.level<=LogSeverities.LOG_ERROR) Log.log(LogTypes.LOG_CPU, LogSeverities.LOG_ERROR,"Unhandled string op "+type);
            Log.exit("Unhandled string op "+type);
        }
    }
}
