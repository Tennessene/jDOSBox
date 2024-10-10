package jdos.cpu;

// Experimental core to test performance of a switch statement vs Core_dynamic's use of arrays of objects with virtual function calls
// Core_dynamic also uses a switch statment per block (ave 6 ops) to test the results.
//
// I would estimate that this core performance would be competitive with Core_dynamic if it could be compiled in the JIT.
// I need to do more research on JIT criteria for compiling.  Currently I suspect that the function would have to be less
// than 8k.  This might be achieved with ProGuard.
//
// Current state of the class
// 1) ops 0-255 are done, but there are bugs.  Civlization will start correctly but things go wrong after starting a new game.
import jdos.cpu.core_dynamic.*;
import jdos.cpu.core_share.Constants;
import jdos.cpu.core_share.Data;
import jdos.cpu.core_share.ModifiedDecode;
import jdos.cpu.core_switch.Decoder;
import jdos.cpu.core_switch.SwitchBlock;
import jdos.fpu.FPU;
import jdos.hardware.IO;
import jdos.hardware.Memory;
import jdos.hardware.Pic;
import jdos.util.Record;

public class Core_switch extends CPU_Regs {
    static public final int CACHE_MAXSIZE = 4096*2;
    static public final int CACHE_PAGES	= 512;
    static public final int CACHE_BLOCKS = 128*1024;
    static public final int CACHE_ALIGN = 16;
    static public final int DYN_HASH_SHIFT = 4;
    static public final int DYN_PAGE_HASH = 4096>>DYN_HASH_SHIFT;
    static public final int DYN_LINKS = 16;

    // identificator to signal self-modification of the currently executed block
    static public final int SMC_CURRENT_BLOCK = 0xffff;

    static public int instruction_count = 128;

    public static void CPU_Core_Switch_Init() {
    }

    static public void CPU_Core_Switch_Cache_Init(boolean enable_cache) {
        Cache.cache_init(enable_cache);
    }

    static public void CPU_Core_Switch_Cache_Close() {

    }

    public static final CPU.CPU_Decoder CPU_Core_Switch_Trap_Run = new CPU.CPU_Decoder() {
        public /*Bits*/int call() {
            /*Bits*/int oldCycles = CPU.CPU_Cycles;
            CPU.CPU_Cycles = 1;
            CPU.cpu.trap_skip = false;

            // let the normal core execute the next (only one!) instruction
            /*Bits*/int ret=Core_normal.CPU_Core_Normal_Run.call();

            // trap to int1 unless the last instruction deferred this
            // (allows hardware interrupts to be served without interaction)
            if (!CPU.cpu.trap_skip) CPU.CPU_HW_Interrupt(1);

            CPU.CPU_Cycles = oldCycles-1;
            // continue (either the trapflag was clear anyways, or the int1 cleared it)
            CPU.cpudecoder = CPU_Core_Switch_Run;

            return ret;
        }
    };

    private static CacheBlockDynRec LinkBlocks(CacheBlockDynRec running, /*BlockReturn*/int ret) {
        CacheBlockDynRec block=null;
        // the last instruction was a control flow modifying instruction
        /*Bitu*/int temp_ip=CPU_Regs.reg_csPhys.dword+reg_eip;
        Paging.PageHandler handler = Paging.get_tlb_readhandler(temp_ip);
        if (handler instanceof CodePageHandlerDynRec) {
            CodePageHandlerDynRec temp_handler=(CodePageHandlerDynRec)handler;
            if ((temp_handler.flags & Paging.PFLAG_HASCODE)!=0) {
                // see if the target is an already translated block
                block=temp_handler.FindCacheBlock((int)(temp_ip & 4095));
                if (block==null) return null;

                // found it, link the current block to
                running.LinkTo(ret==Constants.BR_Link2?1:0,block);
                return block;
            }
        }
        return null;
    }

    private static int ea32(SwitchBlock block) {
        return block.eaa_segPhys.dword+block.eaa_r1.dword+block.eaa_const;
    }

    private static int ea32sib(SwitchBlock block) {
        return block.eaa_segPhys.dword+block.eaa_r1.dword+block.eaa_const+(block.eaa_r2.dword << block.eaa_sib);
    }

    private static int ea16(SwitchBlock block) {
        return block.eaa_segPhys.dword+((block.eaa_r1.word()+((short)block.eaa_r2.word())+block.eaa_const) & 0xFFFF);
    }

    private static int eaSlow(SwitchBlock block) {
        if (block.eaa16)
            return ea16(block);
        if (block.eaa_sib>0)
            return ea32sib(block);
        return ea32(block);
    }

    static final CPU.Descriptor desc=new CPU.Descriptor();

    private static boolean RM5(SwitchBlock block) {
        if (CPU.cpu.pmode && !CPU.cpu.code.big) {
            CPU.cpu.gdt.GetDescriptor(block.eaa_segVal.dword, desc);
            if ((desc.Type() == CPU.DESC_CODE_R_NC_A) || (desc.Type() == CPU.DESC_CODE_R_NC_NA)) {
                CPU.CPU_Exception(CPU.EXCEPTION_GP, block.eaa_segVal.dword & 0xfffc);
                return true;
            }
        }
        return false;
    }

    private static void RUNEXCEPTION() {
        CPU.CPU_Exception(CPU.cpu.exception.which,CPU.cpu.exception.error);
    }

    private static CacheBlockDynRec link1(CacheBlockDynRec block) {
        if (CPU.CPU_Cycles>0) return null;
        CacheBlockDynRec next = block.link1.to;
        if (next == null)
            return LinkBlocks(block, Constants.BR_Link1);
        else
            return next;
    }
    private static CacheBlockDynRec jump16(CacheBlockDynRec block, boolean COND, int offset, int eip_count) {
        if (COND) {
            reg_ip(reg_ip()+offset+eip_count);

            CacheBlockDynRec next = block.link1.to;
            if (next == null)
                block=LinkBlocks(block, Constants.BR_Link1);
            else
                block = next;
        } else {
            reg_ip(reg_ip() + eip_count);

            CacheBlockDynRec next = block.link2.to;
            if (next == null)
                block = LinkBlocks(block, Constants.BR_Link2);
            else
                block = next;
        }
        if (CPU.CPU_Cycles<0) return null;
        return block;
    }
    private static CacheBlockDynRec jump32(CacheBlockDynRec block, boolean COND, int offset, int eip_count) {
        if (COND) {
            reg_eip+=+offset+eip_count;

            CacheBlockDynRec next = block.link1.to;
            if (next == null)
                block=LinkBlocks(block, Constants.BR_Link1);
            else
                block = next;
        } else {
            reg_eip+=eip_count;

            CacheBlockDynRec next = block.link2.to;
            if (next == null)
                block = LinkBlocks(block, Constants.BR_Link2);
            else
                block = next;
        }
        if (CPU.CPU_Cycles<0) return null;
        return block;
    }
    public static int count=1;

    public static final CPU.CPU_Decoder CPU_Core_Switch_Run = new CPU.CPU_Decoder() {
        public /*Bits*/int call() {
            while (CPU.CPU_Cycles>0) {
                // Determine the linear address of CS:EIP
                /*PhysPt*/int ip_point=CPU_Regs.reg_csPhys.dword + reg_eip;

                Paging.PageHandler handler=Paging.get_tlb_readhandler(ip_point);
                CodePageHandlerDynRec chandler=null;
                int page_ip_point = ip_point & 4095;

                if (handler != null && handler instanceof CodePageHandlerDynRec)
                    chandler = (CodePageHandlerDynRec)handler;
                if (chandler == null) {
                    // see if the current page is present and contains code
                    chandler = Decoder_basic.MakeCodePage(ip_point);
                }
                // page doesn't contain code or is special
                if (chandler==null)
                    return Core_normal.CPU_Core_Normal_Run.call();

                // find correct Dynamic Block to run
                CacheBlockDynRec block=chandler.FindCacheBlock(page_ip_point);
                if (block==null) {
                    // no block found, thus translate the instruction stream
                    // unless the instruction is known to be modified
                    if (chandler.invalidation_map==null || (chandler.invalidation_map.p[page_ip_point]<4)) {
                        // translate up to 32 instructions
                        block = Decoder.CreateCacheBlock(chandler, ip_point, instruction_count);
                    } else {
                        // let the normal core handle this instruction to avoid zero-sized blocks
                        /*Bitu*/int old_cycles=CPU.CPU_Cycles;
                        CPU.CPU_Cycles=1;
                        /*Bits*/int nc_retcode=Core_normal.CPU_Core_Normal_Run.call();
                        if (nc_retcode==0) {
                            CPU.CPU_Cycles=old_cycles-1;
                            continue;
                        }
                        CPU.CPU_CycleLeft+=old_cycles;
                        return nc_retcode;
                    }
                }

                //run_block:
                int eaa;
                int tmp;
                long tmpl;
                int tmp2;
                boolean cf;
                while (block!=null && CPU.CPU_Cycles>0) {
                    CPU.CPU_Cycles-=block.inst.length;
                    for (SwitchBlock b : block.inst) {
//                        Record.op(b.opCode);
//                        if (count>0) {
//                            System.out.println(String.format("%d %06x:%08x %3s %-8s EAX=%08x ECX=%08x EDX=%08x EBX=%08x ESP=%08x EBP=%08x ESI=%08x EDI=%08x FLAGS=%04x", count, CPU_Regs.reg_csVal.dword, CPU_Regs.reg_eip, Integer.toHexString(b.opCode), (Core_normal.desc.length > b.opCode ? " " + Core_normal.desc[b.opCode] : ""), CPU_Regs.reg_eax.dword, CPU_Regs.reg_ecx.dword, CPU_Regs.reg_edx.dword, CPU_Regs.reg_ebx.dword, CPU_Regs.reg_esp.dword, CPU_Regs.reg_ebp.dword, CPU_Regs.reg_esi.dword, CPU_Regs.reg_edi.dword, CPU_Regs.flags));
//                            count++;
//                            if ((count%1000)==0) {
//                                int ii=0;
//                            }
//                        }

                        switch (b.instruction) {
                            case ADD_R8: b.r1.set8(Instructions.ADDB(b.value, b.r1.get8())); reg_eip+=b.eipCount;continue;
                            case ADD_E8: eaa = eaSlow(b); Memory.mem_writeb(eaa, Instructions.ADDB(b.value, Memory.mem_readb(eaa))); reg_eip+=b.eipCount;continue;
                            case ADD_R8_R8: b.r1.set8(Instructions.ADDB(b.r2.get8(), b.r1.get8())); reg_eip+=b.eipCount;continue;
                            case ADD_E8_R8: eaa = eaSlow(b); Memory.mem_writeb(eaa, Instructions.ADDB(b.r1.get8(), Memory.mem_readb(eaa))); reg_eip+=b.eipCount;continue;
                            case ADD_R8_E8: b.r1.set8(Instructions.ADDB(Memory.mem_readb(eaSlow(b)), b.r1.get8())); reg_eip+=b.eipCount;continue;

                            case ADD_R16: b.r1.word(Instructions.ADDW(b.value, b.r1.word())); reg_eip+=b.eipCount;continue;
                            case ADD_E16: eaa = eaSlow(b); Memory.mem_writew(eaa, Instructions.ADDW(b.value, Memory.mem_readw(eaa))); reg_eip+=b.eipCount;continue;
                            case ADD_R16_R16: b.r1.word(Instructions.ADDW(b.r2.word(), b.r1.word())); reg_eip+=b.eipCount;continue;
                            case ADD_E16_R16: eaa = eaSlow(b); Memory.mem_writew(eaa, Instructions.ADDW(b.r1.word(), Memory.mem_readw(eaa))); reg_eip+=b.eipCount;continue;
                            case ADD_R16_E16: b.r1.word(Instructions.ADDW(Memory.mem_readw(eaSlow(b)), b.r1.word())); reg_eip+=b.eipCount;continue;

                            case ADD_R32: b.r1.dword=Instructions.ADDD(b.value, b.r1.dword); reg_eip+=b.eipCount;continue;
                            case ADD_E32: eaa = eaSlow(b); Memory.mem_writed(eaa, Instructions.ADDD(b.value, Memory.mem_readd(eaa))); reg_eip+=b.eipCount;continue;
                            case ADD_R32_R32: b.r1.dword=Instructions.ADDD(b.r2.dword, b.r1.dword); reg_eip+=b.eipCount;continue;
                            case ADD_E32_R32: eaa = eaSlow(b); Memory.mem_writed(eaa, Instructions.ADDD(b.r1.dword, Memory.mem_readd(eaa))); reg_eip+=b.eipCount;continue;
                            case ADD_R32_E32: b.r1.dword=Instructions.ADDD(Memory.mem_readd(eaSlow(b)), b.r1.dword); reg_eip+=b.eipCount;continue;

                            case OR_R8: b.r1.set8(Instructions.ORB(b.value, b.r1.get8())); reg_eip+=b.eipCount;continue;
                            case OR_E8: eaa = eaSlow(b); Memory.mem_writeb(eaa, Instructions.ORB(b.value, Memory.mem_readb(eaa))); reg_eip+=b.eipCount;continue;
                            case OR_R8_R8: b.r1.set8(Instructions.ORB(b.r2.get8(), b.r1.get8())); reg_eip+=b.eipCount;continue;
                            case OR_E8_R8: eaa = eaSlow(b); Memory.mem_writeb(eaa, Instructions.ORB(b.r1.get8(), Memory.mem_readb(eaa))); reg_eip+=b.eipCount;continue;
                            case OR_R8_E8: b.r1.set8(Instructions.ORB(Memory.mem_readb(eaSlow(b)), b.r1.get8())); reg_eip+=b.eipCount;continue;

                            case OR_R16: b.r1.word(Instructions.ORW(b.value, b.r1.word())); reg_eip+=b.eipCount;continue;
                            case OR_E16: eaa = eaSlow(b); Memory.mem_writew(eaa, Instructions.ORW(b.value, Memory.mem_readw(eaa))); reg_eip+=b.eipCount;continue;
                            case OR_R16_R16: b.r1.word(Instructions.ORW(b.r2.word(), b.r1.word())); reg_eip+=b.eipCount;continue;
                            case OR_E16_R16: eaa = eaSlow(b); Memory.mem_writew(eaa, Instructions.ORW(b.r1.word(), Memory.mem_readw(eaa))); reg_eip+=b.eipCount;continue;
                            case OR_R16_E16: b.r1.word(Instructions.ORW(Memory.mem_readw(eaSlow(b)), b.r1.word())); reg_eip+=b.eipCount;continue;

                            case OR_R32: b.r1.dword=Instructions.ORD(b.value, b.r1.dword); reg_eip+=b.eipCount;continue;
                            case OR_E32: eaa = eaSlow(b); Memory.mem_writed(eaa, Instructions.ORD(b.value, Memory.mem_readd(eaa))); reg_eip+=b.eipCount;continue;
                            case OR_R32_R32: b.r1.dword=Instructions.ORD(b.r2.dword, b.r1.dword); reg_eip+=b.eipCount;continue;
                            case OR_E32_R32: eaa = eaSlow(b); Memory.mem_writed(eaa, Instructions.ORD(b.r1.dword, Memory.mem_readd(eaa))); reg_eip+=b.eipCount;continue;
                            case OR_R32_E32: b.r1.dword=Instructions.ORD(Memory.mem_readd(eaSlow(b)), b.r1.dword); reg_eip+=b.eipCount;continue;

                            case ADC_R8: b.r1.set8(Instructions.ADCB(b.value, b.r1.get8())); reg_eip+=b.eipCount;continue;
                            case ADC_E8: eaa = eaSlow(b); Memory.mem_writeb(eaa, Instructions.ADCB(b.value, Memory.mem_readb(eaa))); reg_eip+=b.eipCount;continue;
                            case ADC_R8_R8: b.r1.set8(Instructions.ADCB(b.r2.get8(), b.r1.get8())); reg_eip+=b.eipCount;continue;
                            case ADC_E8_R8: eaa = eaSlow(b); Memory.mem_writeb(eaa, Instructions.ADCB(b.r1.get8(), Memory.mem_readb(eaa))); reg_eip+=b.eipCount;continue;
                            case ADC_R8_E8: b.r1.set8(Instructions.ADCB(Memory.mem_readb(eaSlow(b)), b.r1.get8())); reg_eip+=b.eipCount;continue;

                            case ADC_R16: b.r1.word(Instructions.ADCW(b.value, b.r1.word())); reg_eip+=b.eipCount;continue;
                            case ADC_E16: eaa = eaSlow(b); Memory.mem_writew(eaa, Instructions.ADCW(b.value, Memory.mem_readw(eaa))); reg_eip+=b.eipCount;continue;
                            case ADC_R16_R16: b.r1.word(Instructions.ADCW(b.r2.word(), b.r1.word())); reg_eip+=b.eipCount;continue;
                            case ADC_E16_R16: eaa = eaSlow(b); Memory.mem_writew(eaa, Instructions.ADCW(b.r1.word(), Memory.mem_readw(eaa))); reg_eip+=b.eipCount;continue;
                            case ADC_R16_E16: b.r1.word(Instructions.ADCW(Memory.mem_readw(eaSlow(b)), b.r1.word())); reg_eip+=b.eipCount;continue;

                            case ADC_R32: b.r1.dword=Instructions.ADCD(b.value, b.r1.dword); reg_eip+=b.eipCount;continue;
                            case ADC_E32: eaa = eaSlow(b); Memory.mem_writed(eaa, Instructions.ADCD(b.value, Memory.mem_readd(eaa))); reg_eip+=b.eipCount;continue;
                            case ADC_R32_R32: b.r1.dword=Instructions.ADCD(b.r2.dword, b.r1.dword); reg_eip+=b.eipCount;continue;
                            case ADC_E32_R32: eaa = eaSlow(b); Memory.mem_writed(eaa, Instructions.ADCD(b.r1.dword, Memory.mem_readd(eaa))); reg_eip+=b.eipCount;continue;
                            case ADC_R32_E32: b.r1.dword=Instructions.ADCD(Memory.mem_readd(eaSlow(b)), b.r1.dword); reg_eip+=b.eipCount;continue;

                            case SBB_R8: b.r1.set8(Instructions.SBBB(b.value, b.r1.get8())); reg_eip+=b.eipCount;continue;
                            case SBB_E8: eaa = eaSlow(b); Memory.mem_writeb(eaa, Instructions.SBBB(b.value, Memory.mem_readb(eaa))); reg_eip+=b.eipCount;continue;
                            case SBB_R8_R8: b.r1.set8(Instructions.SBBB(b.r2.get8(), b.r1.get8())); reg_eip+=b.eipCount;continue;
                            case SBB_E8_R8: eaa = eaSlow(b); Memory.mem_writeb(eaa, Instructions.SBBB(b.r1.get8(), Memory.mem_readb(eaa))); reg_eip+=b.eipCount;continue;
                            case SBB_R8_E8: b.r1.set8(Instructions.SBBB(Memory.mem_readb(eaSlow(b)), b.r1.get8())); reg_eip+=b.eipCount;continue;

                            case SBB_R16: b.r1.word(Instructions.SBBW(b.value, b.r1.word())); reg_eip+=b.eipCount;continue;
                            case SBB_E16: eaa = eaSlow(b); Memory.mem_writew(eaa, Instructions.SBBW(b.value, Memory.mem_readw(eaa))); reg_eip+=b.eipCount;continue;
                            case SBB_R16_R16: b.r1.word(Instructions.SBBW(b.r2.word(), b.r1.word())); reg_eip+=b.eipCount;continue;
                            case SBB_E16_R16: eaa = eaSlow(b); Memory.mem_writew(eaa, Instructions.SBBW(b.r1.word(), Memory.mem_readw(eaa))); reg_eip+=b.eipCount;continue;
                            case SBB_R16_E16: b.r1.word(Instructions.SBBW(Memory.mem_readw(eaSlow(b)), b.r1.word())); reg_eip+=b.eipCount;continue;

                            case SBB_R32: b.r1.dword=Instructions.SBBD(b.value, b.r1.dword); reg_eip+=b.eipCount;continue;
                            case SBB_E32: eaa = eaSlow(b); Memory.mem_writed(eaa, Instructions.SBBD(b.value, Memory.mem_readd(eaa))); reg_eip+=b.eipCount;continue;
                            case SBB_R32_R32: b.r1.dword=Instructions.SBBD(b.r2.dword, b.r1.dword); reg_eip+=b.eipCount;continue;
                            case SBB_E32_R32: eaa = eaSlow(b); Memory.mem_writed(eaa, Instructions.SBBD(b.r1.dword, Memory.mem_readd(eaa))); reg_eip+=b.eipCount;continue;
                            case SBB_R32_E32: b.r1.dword=Instructions.SBBD(Memory.mem_readd(eaSlow(b)), b.r1.dword); reg_eip+=b.eipCount;continue;

                            case AND_R8: b.r1.set8(Instructions.ANDB(b.value, b.r1.get8())); reg_eip+=b.eipCount;continue;
                            case AND_E8: eaa = eaSlow(b); Memory.mem_writeb(eaa, Instructions.ANDB(b.value, Memory.mem_readb(eaa))); reg_eip+=b.eipCount;continue;
                            case AND_R8_R8: b.r1.set8(Instructions.ANDB(b.r2.get8(), b.r1.get8())); reg_eip+=b.eipCount;continue;
                            case AND_E8_R8: eaa = eaSlow(b); Memory.mem_writeb(eaa, Instructions.ANDB(b.r1.get8(), Memory.mem_readb(eaa))); reg_eip+=b.eipCount;continue;
                            case AND_R8_E8: b.r1.set8(Instructions.ANDB(Memory.mem_readb(eaSlow(b)), b.r1.get8())); reg_eip+=b.eipCount;continue;

                            case AND_R16: b.r1.word(Instructions.ANDW(b.value, b.r1.word())); reg_eip+=b.eipCount;continue;
                            case AND_E16: eaa = eaSlow(b); Memory.mem_writew(eaa, Instructions.ANDW(b.value, Memory.mem_readw(eaa))); reg_eip+=b.eipCount;continue;
                            case AND_R16_R16: b.r1.word(Instructions.ANDW(b.r2.word(), b.r1.word())); reg_eip+=b.eipCount;continue;
                            case AND_E16_R16: eaa = eaSlow(b); Memory.mem_writew(eaa, Instructions.ANDW(b.r1.word(), Memory.mem_readw(eaa))); reg_eip+=b.eipCount;continue;
                            case AND_R16_E16: b.r1.word(Instructions.ANDW(Memory.mem_readw(eaSlow(b)), b.r1.word())); reg_eip+=b.eipCount;continue;

                            case AND_R32: b.r1.dword=Instructions.ANDD(b.value, b.r1.dword); reg_eip+=b.eipCount;continue;
                            case AND_E32: eaa = eaSlow(b); Memory.mem_writed(eaa, Instructions.ANDD(b.value, Memory.mem_readd(eaa))); reg_eip+=b.eipCount;continue;
                            case AND_R32_R32: b.r1.dword=Instructions.ANDD(b.r2.dword, b.r1.dword); reg_eip+=b.eipCount;continue;
                            case AND_E32_R32: eaa = eaSlow(b); Memory.mem_writed(eaa, Instructions.ANDD(b.r1.dword, Memory.mem_readd(eaa))); reg_eip+=b.eipCount;continue;
                            case AND_R32_E32: b.r1.dword=Instructions.ANDD(Memory.mem_readd(eaSlow(b)), b.r1.dword); reg_eip+=b.eipCount;continue;

                            case SUB_R8: b.r1.set8(Instructions.SUBB(b.value, b.r1.get8())); reg_eip+=b.eipCount;continue;
                            case SUB_E8: eaa = eaSlow(b); Memory.mem_writeb(eaa, Instructions.SUBB(b.value, Memory.mem_readb(eaa))); reg_eip+=b.eipCount;continue;
                            case SUB_R8_R8: b.r1.set8(Instructions.SUBB(b.r2.get8(), b.r1.get8())); reg_eip+=b.eipCount;continue;
                            case SUB_E8_R8: eaa = eaSlow(b); Memory.mem_writeb(eaa, Instructions.SUBB(b.r1.get8(), Memory.mem_readb(eaa))); reg_eip+=b.eipCount;continue;
                            case SUB_R8_E8: b.r1.set8(Instructions.SUBB(Memory.mem_readb(eaSlow(b)), b.r1.get8())); reg_eip+=b.eipCount;continue;

                            case SUB_R16: b.r1.word(Instructions.SUBW(b.value, b.r1.word())); reg_eip+=b.eipCount;continue;
                            case SUB_E16: eaa = eaSlow(b); Memory.mem_writew(eaa, Instructions.SUBW(b.value, Memory.mem_readw(eaa))); reg_eip+=b.eipCount;continue;
                            case SUB_R16_R16: b.r1.word(Instructions.SUBW(b.r2.word(), b.r1.word())); reg_eip+=b.eipCount;continue;
                            case SUB_E16_R16: eaa = eaSlow(b); Memory.mem_writew(eaa, Instructions.SUBW(b.r1.word(), Memory.mem_readw(eaa))); reg_eip+=b.eipCount;continue;
                            case SUB_R16_E16: b.r1.word(Instructions.SUBW(Memory.mem_readw(eaSlow(b)), b.r1.word())); reg_eip+=b.eipCount;continue;

                            case SUB_R32: b.r1.dword=Instructions.SUBD(b.value, b.r1.dword); reg_eip+=b.eipCount;continue;
                            case SUB_E32: eaa = eaSlow(b); Memory.mem_writed(eaa, Instructions.SUBD(b.value, Memory.mem_readd(eaa))); reg_eip+=b.eipCount;continue;
                            case SUB_R32_R32: b.r1.dword=Instructions.SUBD(b.r2.dword, b.r1.dword); reg_eip+=b.eipCount;continue;
                            case SUB_E32_R32: eaa = eaSlow(b); Memory.mem_writed(eaa, Instructions.SUBD(b.r1.dword, Memory.mem_readd(eaa))); reg_eip+=b.eipCount;continue;
                            case SUB_R32_E32: b.r1.dword=Instructions.SUBD(Memory.mem_readd(eaSlow(b)), b.r1.dword); reg_eip+=b.eipCount;continue;

                            case XOR_R8: b.r1.set8(Instructions.XORB(b.value, b.r1.get8())); reg_eip+=b.eipCount;continue;
                            case XOR_E8: eaa = eaSlow(b); Memory.mem_writeb(eaa, Instructions.XORB(b.value, Memory.mem_readb(eaa))); reg_eip+=b.eipCount;continue;
                            case XOR_R8_R8: b.r1.set8(Instructions.XORB(b.r2.get8(), b.r1.get8())); reg_eip+=b.eipCount;continue;
                            case XOR_E8_R8: eaa = eaSlow(b); Memory.mem_writeb(eaa, Instructions.XORB(b.r1.get8(), Memory.mem_readb(eaa))); reg_eip+=b.eipCount;continue;
                            case XOR_R8_E8: b.r1.set8(Instructions.XORB(Memory.mem_readb(eaSlow(b)), b.r1.get8())); reg_eip+=b.eipCount;continue;

                            case XOR_R16: b.r1.word(Instructions.XORW(b.value, b.r1.word())); reg_eip+=b.eipCount;continue;
                            case XOR_E16: eaa = eaSlow(b); Memory.mem_writew(eaa, Instructions.XORW(b.value, Memory.mem_readw(eaa))); reg_eip+=b.eipCount;continue;
                            case XOR_R16_R16: b.r1.word(Instructions.XORW(b.r2.word(), b.r1.word())); reg_eip+=b.eipCount;continue;
                            case XOR_E16_R16: eaa = eaSlow(b); Memory.mem_writew(eaa, Instructions.XORW(b.r1.word(), Memory.mem_readw(eaa))); reg_eip+=b.eipCount;continue;
                            case XOR_R16_E16: b.r1.word(Instructions.XORW(Memory.mem_readw(eaSlow(b)), b.r1.word())); reg_eip+=b.eipCount;continue;

                            case XOR_R32: b.r1.dword=Instructions.XORD(b.value, b.r1.dword); reg_eip+=b.eipCount;continue;
                            case XOR_E32: eaa = eaSlow(b); Memory.mem_writed(eaa, Instructions.XORD(b.value, Memory.mem_readd(eaa))); reg_eip+=b.eipCount;continue;
                            case XOR_R32_R32: b.r1.dword=Instructions.XORD(b.r2.dword, b.r1.dword); reg_eip+=b.eipCount;continue;
                            case XOR_E32_R32: eaa = eaSlow(b); Memory.mem_writed(eaa, Instructions.XORD(b.r1.dword, Memory.mem_readd(eaa))); reg_eip+=b.eipCount;continue;
                            case XOR_R32_E32: b.r1.dword=Instructions.XORD(Memory.mem_readd(eaSlow(b)), b.r1.dword); reg_eip+=b.eipCount;continue;
                                
                            case CMP_R8: Instructions.CMPB(b.value, b.r1.get8()); reg_eip+=b.eipCount;continue;
                            case CMP_E8: Instructions.CMPB(b.value, Memory.mem_readb(eaSlow(b))); reg_eip+=b.eipCount;continue;
                            case CMP_R8_R8: Instructions.CMPB(b.r2.get8(), b.r1.get8()); reg_eip+=b.eipCount;continue;
                            case CMP_E8_R8: Instructions.CMPB(b.r1.get8(), Memory.mem_readb(eaSlow(b))); reg_eip+=b.eipCount;continue;
                            case CMP_R8_E8: Instructions.CMPB(Memory.mem_readb(eaSlow(b)), b.r1.get8()); reg_eip+=b.eipCount;continue;

                            case CMP_R16: Instructions.CMPW(b.value, b.r1.word()); reg_eip+=b.eipCount;continue;
                            case CMP_E16: Instructions.CMPW(b.value, Memory.mem_readw(eaSlow(b))); reg_eip+=b.eipCount;continue;
                            case CMP_R16_R16: Instructions.CMPW(b.r2.word(), b.r1.word()); reg_eip+=b.eipCount;continue;
                            case CMP_E16_R16: Instructions.CMPW(b.r1.word(), Memory.mem_readw(eaSlow(b))); reg_eip+=b.eipCount;continue;
                            case CMP_R16_E16: Instructions.CMPW(Memory.mem_readw(eaSlow(b)), b.r1.word()); reg_eip+=b.eipCount;continue;

                            case CMP_R32: Instructions.CMPD(b.value, b.r1.dword); reg_eip+=b.eipCount;continue;
                            case CMP_E32: Instructions.CMPD(b.value, Memory.mem_readd(eaSlow(b))); reg_eip+=b.eipCount;continue;
                            case CMP_R32_R32: Instructions.CMPD(b.r2.dword, b.r1.dword); reg_eip+=b.eipCount;continue;
                            case CMP_E32_R32: Instructions.CMPD(b.r1.dword, Memory.mem_readd(eaSlow(b))); reg_eip+=b.eipCount;continue;
                            case CMP_R32_E32: Instructions.CMPD(Memory.mem_readd(eaSlow(b)), b.r1.dword); reg_eip+=b.eipCount;continue;

                            case PUSH16_ES: CPU.CPU_Push16(CPU_Regs.reg_esVal.dword); reg_eip+=b.eipCount;continue;
                            case POP16_ES: if (CPU.CPU_PopSegES(false)) {RUNEXCEPTION(); block=null; break;} else {reg_eip+=b.eipCount;continue;}
                            case PUSH16_CS: CPU.CPU_Push16(CPU_Regs.reg_csVal.dword); reg_eip+=b.eipCount;continue;
                            case PUSH16_SS: CPU.CPU_Push16(CPU_Regs.reg_ssVal.dword); reg_eip+=b.eipCount;continue;
                            case POP16_SS: if (CPU.CPU_PopSegSS(false)) {RUNEXCEPTION(); block=null; break;} else {reg_eip+=b.eipCount;continue;}
                            case PUSH16_DS: CPU.CPU_Push16(CPU_Regs.reg_dsVal.dword); reg_eip+=b.eipCount;continue;
                            case POP16_DS: if (CPU.CPU_PopSegDS(false)) {RUNEXCEPTION(); block=null; break;} else {reg_eip+=b.eipCount;continue;}
                            case PUSH16_FS: CPU.CPU_Push16(CPU_Regs.reg_fsVal.dword); reg_eip+=b.eipCount;continue;
                            case POP16_FS: if (CPU.CPU_PopSegFS(false)) {RUNEXCEPTION(); block=null; break;} else {reg_eip+=b.eipCount;continue;}
                            case PUSH16_GS: CPU.CPU_Push16(CPU_Regs.reg_gsVal.dword); reg_eip+=b.eipCount;continue;
                            case POP16_GS: if (CPU.CPU_PopSegGS(false)) {RUNEXCEPTION(); block=null; break;} else {reg_eip+=b.eipCount;continue;}

                            case PUSH32_ES: CPU.CPU_Push32(CPU_Regs.reg_esVal.dword); reg_eip+=b.eipCount;continue;
                            case POP32_ES: if (CPU.CPU_PopSegES(true)) {RUNEXCEPTION(); block=null; break;} else {reg_eip+=b.eipCount;continue;}
                            case PUSH32_CS: CPU.CPU_Push32(CPU_Regs.reg_csVal.dword); reg_eip+=b.eipCount;continue;
                            case PUSH32_SS: CPU.CPU_Push32(CPU_Regs.reg_ssVal.dword); reg_eip+=b.eipCount;continue;
                            case POP32_SS: if (CPU.CPU_PopSegSS(true)) {RUNEXCEPTION(); block=null; break;} else {reg_eip+=b.eipCount;continue;}
                            case PUSH32_DS: CPU.CPU_Push32(CPU_Regs.reg_dsVal.dword); reg_eip+=b.eipCount;continue;
                            case POP32_DS: if (CPU.CPU_PopSegDS(true)) {RUNEXCEPTION(); block=null; break;} else {reg_eip+=b.eipCount;continue;}
                            case PUSH32_FS: CPU.CPU_Push32(CPU_Regs.reg_fsVal.dword); reg_eip+=b.eipCount;continue;
                            case POP32_FS: if (CPU.CPU_PopSegFS(true)) {RUNEXCEPTION(); block=null; break;} else {reg_eip+=b.eipCount;continue;}
                            case PUSH32_GS: CPU.CPU_Push32(CPU_Regs.reg_gsVal.dword); reg_eip+=b.eipCount;continue;
                            case POP32_GS: if (CPU.CPU_PopSegGS(true)) {RUNEXCEPTION(); block=null; break;} else {reg_eip+=b.eipCount;continue;}
                                
                            case NOP: reg_eip+=b.eipCount;continue;
                            case DAA: Instructions.DAA(); reg_eip+=b.eipCount;continue;
                            case DAS: Instructions.DAS(); reg_eip+=b.eipCount;continue;
                            case AAA: Instructions.AAA(); reg_eip+=b.eipCount;continue;
                            case AAS: Instructions.AAS(); reg_eip+=b.eipCount;continue;

                            case INC_R8: b.r1.set8(Instructions.INCB(b.r1.get8())); reg_eip+=b.eipCount;continue;
                            case INC_R16: b.r1.word(Instructions.INCW(b.r1.word())); reg_eip+=b.eipCount;continue;
                            case INC_R32: b.r1.dword=Instructions.INCD(b.r1.dword); reg_eip+=b.eipCount;continue;
                            case DEC_R8: b.r1.set8(Instructions.DECB(b.r1.get8())); reg_eip+=b.eipCount;continue;
                            case DEC_R16: b.r1.word(Instructions.DECW(b.r1.word())); reg_eip+=b.eipCount;continue;
                            case DEC_R32: b.r1.dword=Instructions.DECD(b.r1.dword); reg_eip+=b.eipCount;continue;

                            case PUSH16: CPU.CPU_Push16(b.value); reg_eip+=b.eipCount;continue;
                            case PUSH16_R16: CPU.CPU_Push16(b.r1.word()); reg_eip+=b.eipCount;continue;
                            case PUSH16_E16: CPU.CPU_Push16(Memory.mem_readw(eaSlow(b))); reg_eip+=b.eipCount;continue;
                            case PUSH32: CPU.CPU_Push32(b.value); reg_eip+=b.eipCount;continue;
                            case PUSH32_R32: CPU.CPU_Push32(b.r1.dword); reg_eip+=b.eipCount;continue;
                            case PUSH32_E32: CPU.CPU_Push32(Memory.mem_readd(eaSlow(b))); reg_eip+=b.eipCount;continue;

                            case POP16_R16: b.r1.word(CPU.CPU_Pop16()); reg_eip+=b.eipCount;continue;
                            case POP16_E16: Memory.mem_writew(eaSlow(b), CPU.CPU_Pop16()); reg_eip+=b.eipCount;continue;
                            case POP32_R32: b.r1.dword=CPU.CPU_Pop32(); reg_eip+=b.eipCount;continue;
                            case POP32_E32: Memory.mem_writed(eaSlow(b), CPU.CPU_Pop32()); reg_eip+=b.eipCount;continue;

                            case PUSH16A: {
                                /*Bit16u*/int old_sp=reg_esp.word();
                                int esp = reg_esp.dword;
                                esp = CPU.CPU_Push16(esp, reg_eax.word());
                                esp = CPU.CPU_Push16(esp, reg_ecx.word());
                                esp = CPU.CPU_Push16(esp, reg_edx.word());
                                esp = CPU.CPU_Push16(esp, reg_ebx.word());
                                esp = CPU.CPU_Push16(esp, old_sp);
                                esp = CPU.CPU_Push16(esp, reg_ebp.word());
                                esp = CPU.CPU_Push16(esp, reg_esi.word());
                                esp = CPU.CPU_Push16(esp, reg_edi.word());
                                // Don't store ESP until all the memory writes are done in case of a PF so that this op can be reentrant
                                reg_esp.word(esp);
                                reg_eip+=b.eipCount;
                                continue;
                            }
                            case POP16A:
                                reg_edi.word(CPU.CPU_Peek16(0));reg_esi.word(CPU.CPU_Peek16(1));reg_ebp.word(CPU.CPU_Peek16(2));//Don't save SP
                                reg_ebx.word(CPU.CPU_Peek16(4));reg_edx.word(CPU.CPU_Peek16(5));reg_ecx.word(CPU.CPU_Peek16(6));reg_eax.word(CPU.CPU_Peek16(7));
                                CPU_Regs.reg_esp.dword=(CPU_Regs.reg_esp.dword & CPU.cpu.stack.notmask)|((CPU_Regs.reg_esp.dword+16) & CPU.cpu.stack.mask);
                                reg_eip+=b.eipCount;
                                continue;
                            case PUSH32A: {
                                /*Bit16u*/int old_sp=reg_esp.dword;
                                int esp = reg_esp.dword;
                                esp = CPU.CPU_Push32(esp, reg_eax.dword);
                                esp = CPU.CPU_Push32(esp, reg_ecx.dword);
                                esp = CPU.CPU_Push32(esp, reg_edx.dword);
                                esp = CPU.CPU_Push32(esp, reg_ebx.dword);
                                esp = CPU.CPU_Push32(esp, old_sp);
                                esp = CPU.CPU_Push32(esp, reg_ebp.dword);
                                esp = CPU.CPU_Push32(esp, reg_esi.dword);
                                esp = CPU.CPU_Push32(esp, reg_edi.dword);
                                // Don't store ESP until all the memory writes are done in case of a PF so that this op can be reentrant
                                reg_esp.dword=esp;
                                reg_eip+=b.eipCount;
                                continue;
                            }
                            case POP32A:
                                reg_edi.dword=CPU.CPU_Peek32(0);reg_esi.dword=CPU.CPU_Peek32(1);reg_ebp.dword=CPU.CPU_Peek32(2);//Don't save SP
                                reg_ebx.dword=CPU.CPU_Peek32(4);reg_edx.dword=CPU.CPU_Peek32(5);reg_ecx.dword=CPU.CPU_Peek32(6);reg_eax.dword=CPU.CPU_Peek32(7);
                                CPU_Regs.reg_esp.dword=(CPU_Regs.reg_esp.dword & CPU.cpu.stack.notmask)|((CPU_Regs.reg_esp.dword+16) & CPU.cpu.stack.mask);
                                reg_eip+=b.eipCount;
                                continue;
                                
                            case BOUND16: {
                                eaa = eaSlow(b);
                                short bound_min = (short) Memory.mem_readw(eaa);
                                short bound_max = (short) Memory.mem_readw(eaa + 2);
                                short value = (short) b.r1.word();
                                if ((value < bound_min) || (value > bound_max)) {
                                    CPU.CPU_Exception(5);
                                    block = null;
                                    break;
                                }
                                reg_eip+=b.eipCount;
                                continue;
                            }
                            case BOUND32: {
                                eaa=eaSlow(b);
                                int bound_min=Memory.mem_readd(eaa);
                                int bound_max=Memory.mem_readd(eaa + 4);
                                int rmrd = b.r1.dword;
                                if (rmrd < bound_min || rmrd > bound_max) {
                                    CPU.CPU_Exception(5);
                                    block = null;
                                    break;
                                }
                                reg_eip+=b.eipCount;
                                continue;
                            }
                            case ARPL_R16_R16:
                                if ((CPU_Regs.flags & CPU_Regs.VM)!=0 || (!CPU.cpu.pmode)) {
                                    CPU.CPU_Exception(6,0);
                                    block = null;
                                    break;
                                }
                                b.r1.word(CPU.CPU_ARPL(b.r1.word(), b.r2.word()));
                                reg_eip+=b.eipCount;
                                continue;
                            case ARPL_R16_E16: {
                                if ((CPU_Regs.flags & CPU_Regs.VM)!=0 || (!CPU.cpu.pmode)) {
                                    CPU.CPU_Exception(6,0);
                                    block = null;
                                    break;
                                }
                                eaa=eaSlow(b);
                                Memory.mem_writew(eaa,CPU.CPU_ARPL(Memory.mem_readw(eaa),b.r1.word()));
                                reg_eip+=b.eipCount;
                                continue;
                            }
                            case ARPL_R32_R32:
                                if (((CPU.cpu.pmode) && (CPU_Regs.flags & CPU_Regs.VM)!=0) || (!CPU.cpu.pmode)) {
                                    CPU.CPU_Exception(6,0);
                                    block = null;
                                    break;
                                }
                                b.r1.dword=CPU.CPU_ARPL(b.r1.dword, b.r2.word());
                                reg_eip+=b.eipCount;
                                continue;
                            case ARPL_R32_E32: {
                                if (((CPU.cpu.pmode) && (CPU_Regs.flags & CPU_Regs.VM)!=0) || (!CPU.cpu.pmode)) {
                                    CPU.CPU_Exception(6,0);
                                    block = null;
                                    break;
                                }
                                eaa=eaSlow(b);
                                Memory.mem_writed(eaa,CPU.CPU_ARPL(Memory.mem_readd(eaa),b.r1.word()));
                                reg_eip+=b.eipCount;
                                continue;
                            }
                            case IMUL_R16_R16: b.r2.word(Instructions.DIMULW(b.r1.word(), b.value)); reg_eip+=b.eipCount; continue;
                            case IMUL_R16_E16: b.r1.word(Instructions.DIMULW(Memory.mem_readw(eaSlow(b)), b.value)); reg_eip+=b.eipCount; continue;

                            case STRING_EXCEPTION: {
                                if (CPU.CPU_IO_Exception(reg_edx.word(), b.eaa_sib)) {
                                    RUNEXCEPTION();
                                    block = null;
                                    break;
                                }
                                Core.rep_zero = b.zero;
                                Core.base_ds = b.eaa_segPhys.dword;
                                StringOp.DoString(b.eaa_const, b.value);
                                reg_eip+=b.eipCount;
                                continue;
                            }
                            case STRING: {
                                Core.rep_zero = b.zero;
                                Core.base_ds = b.eaa_segPhys.dword;
                                StringOp.DoString(b.eaa_const, b.value);
                                reg_eip+=b.eipCount;
                                continue;
                            }
                            case JUMP16_JO: block = jump16(block, Flags.TFLG_O(), b.value, b.eipCount); break;
                            case JUMP16_NJO: block = jump16(block, Flags.TFLG_NO(), b.value, b.eipCount); break;
                            case JUMP16_B: block = jump16(block, Flags.TFLG_B(), b.value, b.eipCount); break;
                            case JUMP16_NB: block = jump16(block, Flags.TFLG_NB(), b.value, b.eipCount); break;
                            case JUMP16_Z: block = jump16(block, Flags.TFLG_Z(), b.value, b.eipCount); break;
                            case JUMP16_NZ: block = jump16(block, Flags.TFLG_NZ(), b.value, b.eipCount); break;
                            case JUMP16_BE: block = jump16(block, Flags.TFLG_BE(), b.value, b.eipCount); break;
                            case JUMP16_NBE: block = jump16(block, Flags.TFLG_NBE(), b.value, b.eipCount); break;
                            case JUMP16_S: block = jump16(block, Flags.TFLG_S(), b.value, b.eipCount); break;
                            case JUMP16_NS: block = jump16(block, Flags.TFLG_NS(), b.value, b.eipCount); break;
                            case JUMP16_P: block = jump16(block, Flags.TFLG_P(), b.value, b.eipCount); break;
                            case JUMP16_NP: block = jump16(block, Flags.TFLG_NP(), b.value, b.eipCount); break;
                            case JUMP16_L: block = jump16(block, Flags.TFLG_L(), b.value, b.eipCount); break;
                            case JUMP16_NL: block = jump16(block, Flags.TFLG_NL(), b.value, b.eipCount); break;
                            case JUMP16_LE: block = jump16(block, Flags.TFLG_LE(), b.value, b.eipCount); break;
                            case JUMP16_NLE: block = jump16(block, Flags.TFLG_NLE(), b.value, b.eipCount); break;

                            case TEST_R8: Instructions.TESTB(b.value, b.r1.get8()); reg_eip+=b.eipCount;continue;
                            case TEST_E8: Instructions.TESTB(b.value, Memory.mem_readb(eaSlow(b))); reg_eip+=b.eipCount;continue;
                            case TEST_R8_R8: Instructions.TESTB(b.r2.get8(), b.r1.get8()); reg_eip+=b.eipCount;continue;
                            case TEST_E8_R8: Instructions.TESTB(Memory.mem_readb(eaSlow(b)), b.r1.get8()); reg_eip+=b.eipCount;continue;

                            case TEST_R16: Instructions.TESTW(b.value, b.r1.word()); reg_eip+=b.eipCount;continue;
                            case TEST_E16: Instructions.TESTW(b.value, Memory.mem_readw(eaSlow(b))); reg_eip+=b.eipCount;continue;
                            case TEST_R16_R16: Instructions.TESTW(b.r2.word(), b.r1.word()); reg_eip+=b.eipCount;continue;
                            case TEST_E16_R16: Instructions.TESTW(Memory.mem_readw(eaSlow(b)), b.r1.word()); reg_eip+=b.eipCount;continue;

                            case TEST_R32: Instructions.TESTD(b.value, b.r1.dword); reg_eip+=b.eipCount;continue;
                            case TEST_E32: Instructions.TESTD(b.value, Memory.mem_readd(eaSlow(b))); reg_eip+=b.eipCount;continue;
                            case TEST_R32_R32: Instructions.TESTD(b.r2.dword, b.r1.dword); reg_eip+=b.eipCount;continue;
                            case TEST_E32_R32: Instructions.TESTD(Memory.mem_readd(eaSlow(b)), b.r1.dword); reg_eip+=b.eipCount;continue;

                            case XCHG_R8_R8: tmp=b.r1.get8();b.r1.set8(b.r2.get8());b.r2.set8(tmp); reg_eip+=b.eipCount;continue;
                            case XCHG_E8_R8: eaa=eaSlow(b);tmp=b.r1.get8();b.r1.set8(Memory.mem_readb(eaa));Memory.mem_writeb(eaa, tmp); reg_eip+=b.eipCount;continue;
                            case XCHG_R16_R16: tmp=b.r1.word();b.r1.word(b.r2.word());b.r2.word(tmp); reg_eip+=b.eipCount;continue;
                            case XCHG_E16_R16: eaa=eaSlow(b);tmp=b.r1.word();b.r1.word(Memory.mem_readw(eaa));Memory.mem_writew(eaa, tmp); reg_eip+=b.eipCount;continue;
                            case XCHG_R32_R32: tmp=b.r1.dword;b.r1.dword=b.r2.dword;b.r2.dword=tmp; reg_eip+=b.eipCount;continue;
                            case XCHG_E32_R32: eaa=eaSlow(b);tmp=b.r1.dword;b.r1.dword=Memory.mem_readd(eaa);Memory.mem_writed(eaa, tmp); reg_eip+=b.eipCount;continue;

                            case MOV_R8: b.r1.set8(b.value); reg_eip+=b.eipCount;continue;
                            case MOV_E8: Memory.mem_writeb(eaSlow(b), b.value); reg_eip+=b.eipCount;continue;
                            case MOV_R8_R8: b.r1.set8(b.r2.get8()); reg_eip+=b.eipCount;continue;
                            case MOV_E8_R8: Memory.mem_writeb(eaSlow(b), b.r1.get8()); reg_eip+=b.eipCount;continue;
                            case MOV_R8_E8: b.r1.set8(Memory.mem_readb(eaSlow(b))); reg_eip+=b.eipCount;continue;
                            case MOV_E8_R8_RM5: if (RM5(b)) {block=null;break;} Memory.mem_writeb(eaSlow(b), b.r1.get8()); reg_eip+=b.eipCount;continue;
                            case MOV_R16: b.r1.word(b.value); reg_eip+=b.eipCount;continue;
                            case MOV_E16: Memory.mem_writew(eaSlow(b), b.value); reg_eip+=b.eipCount;continue;
                            case MOV_R16_R16: b.r1.word(b.r2.word()); reg_eip+=b.eipCount;continue;
                            case MOV_E16_R16: Memory.mem_writew(eaSlow(b), b.r1.word()); reg_eip+=b.eipCount;continue;
                            case MOV_R16_E16: b.r1.word(Memory.mem_readw(eaSlow(b))); reg_eip+=b.eipCount;continue;

                            case MOV_R32: b.r1.dword = b.value; reg_eip+=b.eipCount;continue;
                            case MOV_E32: Memory.mem_writed(eaSlow(b), b.value); reg_eip+=b.eipCount;continue;
                            case MOV_R32_R32: b.r1.dword=b.r2.dword; reg_eip+=b.eipCount;continue;
                            case MOV_E32_R32: Memory.mem_writed(eaSlow(b), b.r1.dword); reg_eip+=b.eipCount;continue;
                            case MOV_R32_E32: b.r1.dword=Memory.mem_readd(eaSlow(b)); reg_eip+=b.eipCount;continue;

                            case ILLEGAL: CPU.CPU_Exception(6,0); block = null; break;
                            case LEA_R16: b.r1.word(eaSlow(b)); reg_eip+=b.eipCount;continue;
                            case LEA_R32: b.r1.dword=eaSlow(b); reg_eip+=b.eipCount;continue;

                            case MOV_ES_R16: if (CPU.CPU_SetSegGeneralES(b.r1.word())) {RUNEXCEPTION(); block=null; break;} reg_eip+=b.eipCount;continue;
                            case MOV_ES_E16: if (CPU.CPU_SetSegGeneralES(Memory.mem_readw(eaSlow(b)))) {RUNEXCEPTION(); block=null; break;} reg_eip+=b.eipCount;continue;
                            case MOV_SS_R16: if (CPU.CPU_SetSegGeneralSS(b.r1.word())) {RUNEXCEPTION(); block=null; break;} reg_eip+=b.eipCount;continue;
                            case MOV_SS_E16: if (CPU.CPU_SetSegGeneralSS(Memory.mem_readw(eaSlow(b)))) {RUNEXCEPTION(); block=null; break;} reg_eip+=b.eipCount;continue;
                            case MOV_DS_R16: if (CPU.CPU_SetSegGeneralDS(b.r1.word())) {RUNEXCEPTION(); block=null; break;} reg_eip+=b.eipCount;continue;
                            case MOV_DS_E16: if (CPU.CPU_SetSegGeneralDS(Memory.mem_readw(eaSlow(b)))) {RUNEXCEPTION(); block=null; break;} reg_eip+=b.eipCount;continue;
                            case MOV_FS_R16: if (CPU.CPU_SetSegGeneralFS(b.r1.word())) {RUNEXCEPTION(); block=null; break;} reg_eip+=b.eipCount;continue;
                            case MOV_FS_E16: if (CPU.CPU_SetSegGeneralFS(Memory.mem_readw(eaSlow(b)))) {RUNEXCEPTION(); block=null; break;} reg_eip+=b.eipCount;continue;
                            case MOV_GS_R16: if (CPU.CPU_SetSegGeneralGS(b.r1.word())) {RUNEXCEPTION(); block=null; break;} reg_eip+=b.eipCount;continue;
                            case MOV_GS_E16: if (CPU.CPU_SetSegGeneralGS(Memory.mem_readw(eaSlow(b)))) {RUNEXCEPTION(); block=null; break;} reg_eip+=b.eipCount;continue;

                            case CBW: reg_eax.word((byte)reg_eax.low()); reg_eip+=b.eipCount;continue;
                            case CWD: if ((reg_eax.word() & 0x8000)!=0) reg_edx.word(0xffff);else reg_edx.word(0); reg_eip+=b.eipCount;continue;

                            case CALL16_AP:
                                Flags.FillFlags();
                                CPU.CPU_CALL(false,b.eaa_const,b.value,(reg_eip+b.eipCount) & 0xFFFF);
                                if (GETFLAG(TF)!=0) {
                                    CPU.cpudecoder=CPU_Core_Switch_Trap_Run;
                                    return Callback.CBRET_NONE;
                                }
                                block = null;
                                break;
                            case CALL16_EP:
                                Flags.FillFlags();
                                eaa = eaSlow(b);
                                CPU.CPU_CALL(false,Memory.mem_readw(eaa+2),Memory.mem_readw(eaa),(reg_eip+b.eipCount) & 0xFFFF);
                                if (GETFLAG(TF)!=0) {
                                    CPU.cpudecoder=CPU_Core_Switch_Trap_Run;
                                    return Callback.CBRET_NONE;
                                }
                                block = null;
                                break;
                            case CALL32_AP:
                                Flags.FillFlags();
                                CPU.CPU_CALL(true,b.eaa_const,b.value,reg_eip+b.eipCount);
                                if (GETFLAG(TF)!=0) {
                                    CPU.cpudecoder=CPU_Core_Switch_Trap_Run;
                                    return Callback.CBRET_NONE;
                                }
                                block = null;
                                break;
                            case CALL32_EP:
                                Flags.FillFlags();
                                eaa = eaSlow(b);
                                CPU.CPU_CALL(false,Memory.mem_readw(eaa+4),Memory.mem_readd(eaa),reg_eip+b.eipCount);
                                if (GETFLAG(TF)!=0) {
                                    CPU.cpudecoder=CPU_Core_Switch_Trap_Run;
                                    return Callback.CBRET_NONE;
                                }
                                block = null;
                                break;
                            case PUSHF: if (CPU.CPU_PUSHF(!b.eaa16)) {RUNEXCEPTION(); block=null; break;} reg_eip+=b.eipCount; continue;
                            case POPF:
                                if (CPU.CPU_POPF(!b.eaa16)) {RUNEXCEPTION(); block=null; break;}
                                reg_eip+=b.eipCount;
                                if (GETFLAG(TF)!=0) {
                                    CPU.cpudecoder=CPU_Core_Switch_Trap_Run;
                                    return Callback.CBRET_NONE;
                                }
                                if (GETFLAG(IF)!=0 && Pic.PIC_IRQCheck!=0) return Callback.CBRET_NONE;
                                continue;
                            case SAHF:Flags.SETFLAGSb(reg_eax.high());reg_eip+=b.eipCount;continue;
                            case LAHF:Flags.FillFlags();reg_eax.high(CPU_Regs.flags&0xff);reg_eip+=b.eipCount;continue;
                            case MOV_AL_0b: reg_eax.low(Memory.mem_readb(b.eaa_segPhys.dword+b.value));reg_eip+=b.eipCount;continue;
                            case MOV_0b_AL: Memory.mem_writeb(b.eaa_segPhys.dword+b.value,reg_eax.low());reg_eip+=b.eipCount;continue;
                            case MOV_AX_0w: reg_eax.word(Memory.mem_readw(b.eaa_segPhys.dword+b.value));reg_eip+=b.eipCount;continue;
                            case MOV_0w_AX: Memory.mem_writew(b.eaa_segPhys.dword+b.value,reg_eax.word());reg_eip+=b.eipCount;continue;
                            case MOVSB16: Strings.Movsb16.doString(b.eaa_segPhys.dword);reg_eip+=b.eipCount;continue;
                            case MOVSB16r: Strings.Movsb16r.doString(b.eaa_segPhys.dword);reg_eip+=b.eipCount;continue;
                            case MOVSB32: Strings.Movsb32.doString(b.eaa_segPhys.dword);reg_eip+=b.eipCount;continue;
                            case MOVSB32r: Strings.Movsb32r.doString(b.eaa_segPhys.dword);reg_eip+=b.eipCount;continue;
                            case MOVSW16: Strings.Movsw16.doString(b.eaa_segPhys.dword);reg_eip+=b.eipCount;continue;
                            case MOVSW16r: Strings.Movsw16r.doString(b.eaa_segPhys.dword);reg_eip+=b.eipCount;continue;
                            case MOVSW32: Strings.Movsw32.doString(b.eaa_segPhys.dword);reg_eip+=b.eipCount;continue;
                            case MOVSW32r: Strings.Movsw32r.doString(b.eaa_segPhys.dword);reg_eip+=b.eipCount;continue;
                                
                            case ROLB_0_flags: {
                                int value = b.r1.get8();
                                FillFlagsNoCFOF();
                                CPU_Regs.SETFLAGBIT(CPU_Regs.CF, (value & 1) != 0);
                                CPU_Regs.SETFLAGBIT(CPU_Regs.OF, ((value & 1) ^ (value >>> 7)) != 0);
                                reg_eip += b.eipCount;
                                continue;
                            }
                            case ROLB_E8_0_flags: {
                                int value = Memory.mem_readb(eaSlow(b));
                                FillFlagsNoCFOF();
                                CPU_Regs.SETFLAGBIT(CPU_Regs.CF, (value & 1) != 0);
                                CPU_Regs.SETFLAGBIT(CPU_Regs.OF, ((value & 1) ^ (value >>> 7)) != 0);
                                reg_eip += b.eipCount;
                                continue;
                            }

                            case ROLB_R8: b.r1.set8(Instructions.do_ROLB(b.value, b.r1.get8()));reg_eip += b.eipCount;continue;
                            case ROLB_E8: eaa=eaSlow(b);Memory.mem_writeb(eaa, Instructions.do_ROLB(b.value, Memory.mem_readb(eaa)));reg_eip += b.eipCount;continue;

                            case RORB_0_flags: {
                                int value = b.r1.get8();
                                FillFlagsNoCFOF();
                                CPU_Regs.SETFLAGBIT(CPU_Regs.CF,(value>>7)!=0);
                                CPU_Regs.SETFLAGBIT(CPU_Regs.OF,((value>>7) ^ ((value>>>6) & 1))!=0);
                                reg_eip += b.eipCount;
                                continue;
                            }
                            case RORB_E8_0_flags: {
                                int value = Memory.mem_readb(eaSlow(b));
                                FillFlagsNoCFOF();
                                CPU_Regs.SETFLAGBIT(CPU_Regs.CF,(value>>7)!=0);
                                CPU_Regs.SETFLAGBIT(CPU_Regs.OF,((value>>7) ^ ((value>>>6) & 1))!=0);
                                reg_eip += b.eipCount;
                                continue;
                            }

                            // (Eb >>> Ib) | (Eb << (8-Ib))
                            case RORB_R8: b.r1.set8(Instructions.do_RORB(b.value, b.r1.get8()));reg_eip += b.eipCount;continue;
                            case RORB_E8: eaa=eaSlow(b);Memory.mem_writeb(eaa, Instructions.do_RORB(b.value, Memory.mem_readb(eaa)));reg_eip += b.eipCount;continue;

                            // (Eb << Ib) |(cf << (Ib-1)) | (Eb >>> (9-Ib));
                            case RCLB_R8: b.r1.set8(Instructions.do_RCLB(b.value, b.r1.get8()));reg_eip += b.eipCount;continue;
                            case RCLB_E8: eaa=eaSlow(b);Memory.mem_writeb(eaa, Instructions.do_RCLB(b.value, Memory.mem_readb(eaa)));reg_eip += b.eipCount;continue;

                            // (Eb >>> Ib) | (cf << (8-Ib)) | (Eb << (9-Ib))
                            case RCRB_R8: b.r1.set8(Instructions.do_RCRB(b.value, b.r1.get8()));reg_eip += b.eipCount;continue;
                            case RCRB_E8: eaa=eaSlow(b);Memory.mem_writeb(eaa, Instructions.do_RCRB(b.value, Memory.mem_readb(eaa)));reg_eip += b.eipCount;continue;

                            // Eb << Ib
                            case SHLB_R8: b.r1.set8(Instructions.do_SHLB(b.value, b.r1.get8()));reg_eip += b.eipCount;continue;
                            case SHLB_E8: eaa=eaSlow(b);Memory.mem_writeb(eaa, Instructions.do_SHLB(b.value, Memory.mem_readb(eaa)));reg_eip += b.eipCount;continue;

                            // Eb >>> Ib
                            case SHRB_R8: b.r1.set8(Instructions.do_SHRB(b.value, b.r1.get8()));reg_eip += b.eipCount;continue;
                            case SHRB_E8: eaa=eaSlow(b);Memory.mem_writeb(eaa, Instructions.do_SHRB(b.value, Memory.mem_readb(eaa)));reg_eip += b.eipCount;continue;

                            // Eb >> Ib
                            case SARB_R8: b.r1.set8(Instructions.do_SARB(b.value, b.r1.get8()));reg_eip += b.eipCount;continue;
                            case SARB_E8: eaa=eaSlow(b);Memory.mem_writeb(eaa, Instructions.do_SARB(b.value, Memory.mem_readb(eaa)));reg_eip += b.eipCount;continue;

                            case ROLW_0_flags: {
                                int value = b.r1.word();
                                FillFlagsNoCFOF();
                                CPU_Regs.SETFLAGBIT(CPU_Regs.CF, (value & 1) != 0);
                                CPU_Regs.SETFLAGBIT(CPU_Regs.OF, ((value & 1) ^ (value >>> 15)) != 0);
                                reg_eip += b.eipCount;
                                continue;
                            }
                            case ROLW_E16_0_flags: {
                                int value = Memory.mem_readw(eaSlow(b));
                                FillFlagsNoCFOF();
                                CPU_Regs.SETFLAGBIT(CPU_Regs.CF, (value & 1) != 0);
                                CPU_Regs.SETFLAGBIT(CPU_Regs.OF, ((value & 1) ^ (value >>> 15)) != 0);
                                reg_eip += b.eipCount;
                                continue;
                            }

                            // (Ew << Ib) | (Ew >>> (16-Ib)
                            case ROLW_R16: b.r1.word(Instructions.do_ROLW(b.value, b.r1.word()));reg_eip += b.eipCount;continue;
                            case ROLW_E16: eaa=eaSlow(b);Memory.mem_writew(eaa, Instructions.do_ROLW(b.value, Memory.mem_readw(eaa)));reg_eip += b.eipCount;continue;

                            case RORW_0_flags: {
                                int value = b.r1.word();
                                FillFlagsNoCFOF();
                                CPU_Regs.SETFLAGBIT(CPU_Regs.CF,(value>>15)!=0);
                                CPU_Regs.SETFLAGBIT(CPU_Regs.OF,((value>>15) ^ ((value>>>14) & 1))!=0);
                                reg_eip += b.eipCount;
                                continue;
                            }
                            case RORW_E16_0_flags: {
                                int value = Memory.mem_readw(eaSlow(b));
                                FillFlagsNoCFOF();
                                CPU_Regs.SETFLAGBIT(CPU_Regs.CF,(value>>15)!=0);
                                CPU_Regs.SETFLAGBIT(CPU_Regs.OF,((value>>15) ^ ((value>>>14) & 1))!=0);
                                reg_eip += b.eipCount;
                                continue;
                            }

                            // (Ew >>> Ib) | (Ew << (16-Ib))
                            case RORW_R16: b.r1.word(Instructions.do_RORW(b.value, b.r1.word()));reg_eip += b.eipCount;continue;
                            case RORW_E16: eaa=eaSlow(b);Memory.mem_writew(eaa, Instructions.do_RORW(b.value, Memory.mem_readw(eaa)));reg_eip += b.eipCount;continue;

                                // (Ew << Ib) |(cf << (Ib-1)) | (Ew >>> (17-Ib));
                            case RCLW_R16: b.r1.word(Instructions.do_RCLW(b.value, b.r1.word()));reg_eip += b.eipCount;continue;
                            case RCLW_E16: eaa=eaSlow(b);Memory.mem_writew(eaa, Instructions.do_RCLW(b.value, Memory.mem_readw(eaa)));reg_eip += b.eipCount;continue;

                                // (Ew >>> Ib) | (cf << (16-Ib)) | (Ew << (17-Ib))
                            case RCRW_R16: b.r1.word(Instructions.do_RCRW(b.value, b.r1.word()));reg_eip += b.eipCount;continue;
                            case RCRW_E16: eaa=eaSlow(b);Memory.mem_writew(eaa, Instructions.do_RCRW(b.value, Memory.mem_readw(eaa)));reg_eip += b.eipCount;continue;

                                // Ew << Ib
                            case SHLW_R16: b.r1.word(Instructions.do_SHLW(b.value, b.r1.word()));reg_eip += b.eipCount;continue;
                            case SHLW_E16: eaa=eaSlow(b);Memory.mem_writew(eaa, Instructions.do_SHLW(b.value, Memory.mem_readw(eaa)));reg_eip += b.eipCount;continue;

                                // Ew >>> Ib
                            case SHRW_R16: b.r1.word(Instructions.do_SHRW(b.value, b.r1.word()));reg_eip += b.eipCount;continue;
                            case SHRW_E16: eaa=eaSlow(b);Memory.mem_writew(eaa, Instructions.do_SHRW(b.value, Memory.mem_readw(eaa)));reg_eip += b.eipCount;continue;

                                // Ew >> Ib
                            case SARW_R16: b.r1.word(Instructions.do_SARW(b.value, b.r1.word()));reg_eip += b.eipCount;continue;
                            case SARW_E16: eaa=eaSlow(b);Memory.mem_writew(eaa, Instructions.do_SARW(b.value, Memory.mem_readw(eaa)));reg_eip += b.eipCount;continue;

                            case RETN16_Iw: reg_eip=CPU.CPU_Pop16(); reg_esp.dword+=b.value; block=null; break;
                            case RETN16: reg_eip=CPU.CPU_Pop16(); block=null; break;

                            case LES16: eaa=eaSlow(b);tmp=Memory.mem_readw(eaa);if (CPU.CPU_SetSegGeneralES(Memory.mem_readw(eaa+2))) {RUNEXCEPTION(); block=null; break;} b.r1.word(tmp);reg_eip += b.eipCount;continue;
                            case LES32: eaa=eaSlow(b);tmp=Memory.mem_readd(eaa);if (CPU.CPU_SetSegGeneralES(Memory.mem_readw(eaa+2))) {RUNEXCEPTION(); block=null; break;} b.r1.dword=tmp;reg_eip += b.eipCount;continue;

                            case LDS16: eaa=eaSlow(b);tmp=Memory.mem_readw(eaa);if (CPU.CPU_SetSegGeneralDS(Memory.mem_readw(eaa + 2))) {RUNEXCEPTION(); block=null; break;} b.r1.word(tmp);reg_eip += b.eipCount;continue;
                            case LDS32: eaa=eaSlow(b);tmp=Memory.mem_readd(eaa);if (CPU.CPU_SetSegGeneralDS(Memory.mem_readw(eaa + 2))) {RUNEXCEPTION(); block=null; break;} b.r1.dword=tmp;reg_eip += b.eipCount;continue;

                            case ENTER: CPU.CPU_ENTER(!b.eaa16,b.value,b.eaa_const);reg_eip += b.eipCount;continue;
                            case LEAVE16:reg_esp.dword&=CPU.cpu.stack.notmask;reg_esp.dword|=(reg_ebp.dword & CPU.cpu.stack.mask);reg_ebp.word(CPU.CPU_Pop16());reg_eip += b.eipCount;continue;
                            case RETF_Iw:Flags.FillFlags();CPU.CPU_RET(!b.eaa16,b.value,reg_eip+b.eipCount);block=null;break;
                            case INT3:CPU.CPU_SW_Interrupt_NoIOPLCheck(3,reg_eip+b.eipCount);CPU.cpu.trap_skip=true;block=null;break;
                            case INTIb:CPU.CPU_SW_Interrupt(b.value,reg_eip+b.eipCount);CPU.cpu.trap_skip=true;block=null;break;
                            case INTO:if (Flags.get_OF()) {CPU.CPU_SW_Interrupt(4,reg_eip+b.eipCount);CPU.cpu.trap_skip=true;block=null;}break;
                            case IRET:
                                CPU.CPU_IRET(!b.eaa16, reg_eip+b.eipCount);
                                if (GETFLAG(TF)!=0) {
                                    CPU.cpudecoder=CPU_Core_Switch_Trap_Run;
                                    return Callback.CBRET_NONE;
                                }
                                if (GETFLAG(IF)!=0 && Pic.PIC_IRQCheck!=0) return Callback.CBRET_NONE;
                                block=null;
                                break;

                            // (Eb << Ib) | (Eb >>> (8-Ib)
                            case ROLB_R8_CL: tmp2=(reg_ecx.dword & 0x1f);if (Instructions.valid_ROLB(b.r1.get8(), tmp2)) b.r1.set8(Instructions.do_ROLB(tmp2, b.r1.get8()));reg_eip += b.eipCount;continue;
                            case ROLB_E8_CL: tmp2=(reg_ecx.dword & 0x1f); eaa=eaSlow(b); tmp=Memory.mem_readb(eaa); if (Instructions.valid_ROLB(tmp, tmp2)) Memory.mem_writeb(eaa, Instructions.do_ROLB(tmp2, tmp));reg_eip += b.eipCount;continue;

                            // (Eb >>> Ib) | (Eb << (8-Ib))
                            case RORB_R8_CL: tmp2=(reg_ecx.dword & 0x1f);if (Instructions.valid_RORB(b.r1.get8(), tmp2)) b.r1.set8(Instructions.do_RORB(tmp2, b.r1.get8()));reg_eip += b.eipCount;continue;
                            case RORB_E8_CL: tmp2=(reg_ecx.dword & 0x1f); eaa=eaSlow(b); tmp=Memory.mem_readb(eaa); if (Instructions.valid_RORB(tmp, tmp2)) Memory.mem_writeb(eaa, Instructions.do_RORB(tmp2, tmp));reg_eip += b.eipCount;continue;

                                // (Eb << Ib) |(cf << (Ib-1)) | (Eb >>> (9-Ib));
                            case RCLB_R8_CL: tmp2=(reg_ecx.dword & 0x1f);if (Instructions.valid_RCLB(tmp2)) b.r1.set8(Instructions.do_RCLB(tmp2, b.r1.get8()));reg_eip += b.eipCount;continue;
                            case RCLB_E8_CL: tmp2=(reg_ecx.dword & 0x1f);if (Instructions.valid_RCLB(tmp2)) {eaa=eaSlow(b);Memory.mem_writeb(eaa, Instructions.do_RCLB(tmp2, Memory.mem_readb(eaa)));}reg_eip += b.eipCount;continue;

                                // (Eb >>> Ib) | (cf << (8-Ib)) | (Eb << (9-Ib))
                            case RCRB_R8_CL: tmp2=(reg_ecx.dword & 0x1f);if (Instructions.valid_RCRB(tmp2)) b.r1.set8(Instructions.do_RCRB(tmp2, b.r1.get8()));reg_eip += b.eipCount;continue;
                            case RCRB_E8_CL: tmp2=(reg_ecx.dword & 0x1f);if (Instructions.valid_RCRB(tmp2)) {eaa=eaSlow(b);Memory.mem_writeb(eaa, Instructions.do_RCRB(tmp2, Memory.mem_readb(eaa)));}reg_eip += b.eipCount;continue;

                                // Eb << Ib
                            case SHLB_R8_CL: tmp2=(reg_ecx.dword & 0x1f);if (Instructions.valid_SHLB(tmp2)) {b.r1.set8(Instructions.do_SHLB(tmp2, b.r1.get8()));}reg_eip += b.eipCount;continue;
                            case SHLB_E8_CL: tmp2=(reg_ecx.dword & 0x1f);if (Instructions.valid_SHLB(tmp2)) {eaa=eaSlow(b);Memory.mem_writeb(eaa, Instructions.do_SHLB(tmp2, Memory.mem_readb(eaa)));}reg_eip += b.eipCount;continue;

                                // Eb >>> Ib
                            case SHRB_R8_CL: tmp2=(reg_ecx.dword & 0x1f);if (Instructions.valid_SHRB(tmp2)) {b.r1.set8(Instructions.do_SHRB(tmp2, b.r1.get8()));}reg_eip += b.eipCount;continue;
                            case SHRB_E8_CL: tmp2=(reg_ecx.dword & 0x1f);if (Instructions.valid_SHRB(tmp2)) {eaa=eaSlow(b);Memory.mem_writeb(eaa, Instructions.do_SHRB(tmp2, Memory.mem_readb(eaa)));}reg_eip += b.eipCount;continue;

                                // Eb >> Ib
                            case SARB_R8_CL: tmp2=(reg_ecx.dword & 0x1f);if (Instructions.valid_SARB(tmp2)) {b.r1.set8(Instructions.do_SARB(tmp2, b.r1.get8()));}reg_eip += b.eipCount;continue;
                            case SARB_E8_CL: tmp2=(reg_ecx.dword & 0x1f);if (Instructions.valid_SARB(tmp2)) {eaa=eaSlow(b);Memory.mem_writeb(eaa, Instructions.do_SARB(tmp2, Memory.mem_readb(eaa)));}reg_eip += b.eipCount;continue;

                                // (Ew << CL) | (Ew >>> (16-CL)
                            case ROLW_R16_CL: tmp2=(reg_ecx.dword & 0x1f);if (Instructions.valid_ROLW(b.r1.word(), tmp2)) b.r1.word(Instructions.do_ROLW(tmp2, b.r1.word()));reg_eip += b.eipCount;continue;
                            case ROLW_E16_CL: tmp2=(reg_ecx.dword & 0x1f); eaa=eaSlow(b); tmp=Memory.mem_readw(eaa); if (Instructions.valid_ROLW(tmp, tmp2)) Memory.mem_writew(eaa, Instructions.do_ROLW(tmp2, tmp));reg_eip += b.eipCount;continue;

                                // (Ew >>> CL) | (Ew << (16-CL))
                            case RORW_R16_CL: tmp2=(reg_ecx.dword & 0x1f);if (Instructions.valid_RORW(b.r1.word(), tmp2)) b.r1.word(Instructions.do_RORW(tmp2, b.r1.word()));reg_eip += b.eipCount;continue;
                            case RORW_E16_CL: tmp2=(reg_ecx.dword & 0x1f); eaa=eaSlow(b); tmp=Memory.mem_readw(eaa); if (Instructions.valid_RORW(tmp, tmp2)) Memory.mem_writew(eaa, Instructions.do_RORW(tmp2, tmp));reg_eip += b.eipCount;continue;

                                // (Ew << CL) |(cf << (CL-1)) | (Ew >>> (17-CL));
                            case RCLW_R16_CL: tmp2=(reg_ecx.dword & 0x1f);if (Instructions.valid_RCLW(tmp2)) b.r1.word(Instructions.do_RCLW(tmp2, b.r1.word()));reg_eip += b.eipCount;continue;
                            case RCLW_E16_CL: tmp2=(reg_ecx.dword & 0x1f);if (Instructions.valid_RCLW(tmp2)) {eaa=eaSlow(b);Memory.mem_writew(eaa, Instructions.do_RCLW(tmp2, Memory.mem_readw(eaa)));}reg_eip += b.eipCount;continue;

                                // (Ew >>> CL) | (cf << (16-CL)) | (Ew << (17-CL))
                            case RCRW_R16_CL: tmp2=(reg_ecx.dword & 0x1f);if (Instructions.valid_RCRW(tmp2)) b.r1.word(Instructions.do_RCRW(tmp2, b.r1.word()));reg_eip += b.eipCount;continue;
                            case RCRW_E16_CL: tmp2=(reg_ecx.dword & 0x1f);if (Instructions.valid_RCRW(tmp2)) {eaa=eaSlow(b);Memory.mem_writew(eaa, Instructions.do_RCRW(tmp2, Memory.mem_readw(eaa)));}reg_eip += b.eipCount;continue;

                                // Ew << CL
                            case SHLW_R16_CL: tmp2=(reg_ecx.dword & 0x1f);if (Instructions.valid_SHLW(tmp2)) {b.r1.word(Instructions.do_SHLW(tmp2, b.r1.word()));}reg_eip += b.eipCount;continue;
                            case SHLW_E16_CL: tmp2=(reg_ecx.dword & 0x1f);if (Instructions.valid_SHLW(tmp2)) {eaa=eaSlow(b);Memory.mem_writew(eaa, Instructions.do_SHLW(tmp2, Memory.mem_readw(eaa)));}reg_eip += b.eipCount;continue;

                                // Ew >>> CL
                            case SHRW_R16_CL: tmp2=(reg_ecx.dword & 0x1f);if (Instructions.valid_SHRW(tmp2)) {b.r1.word(Instructions.do_SHRW(tmp2, b.r1.word()));}reg_eip += b.eipCount;continue;
                            case SHRW_E16_CL: tmp2=(reg_ecx.dword & 0x1f);if (Instructions.valid_SHRW(tmp2)) {eaa=eaSlow(b);Memory.mem_writew(eaa, Instructions.do_SHRW(tmp2, Memory.mem_readw(eaa)));}reg_eip += b.eipCount;continue;

                                // Ew >> CL
                            case SARW_R16_CL: tmp2=(reg_ecx.dword & 0x1f);if (Instructions.valid_SARW(tmp2)) {b.r1.word(Instructions.do_SARW(tmp2, b.r1.word()));}reg_eip += b.eipCount;continue;
                            case SARW_E16_CL: tmp2=(reg_ecx.dword & 0x1f);if (Instructions.valid_SARW(tmp2)) {eaa=eaSlow(b);Memory.mem_writew(eaa, Instructions.do_SARW(tmp2, Memory.mem_readw(eaa)));}reg_eip += b.eipCount;continue;

                            case AAM: if (!Instructions.AAM(b.value)) {RUNEXCEPTION();block=null;break;}reg_eip += b.eipCount;continue;
                            case AAD: Instructions.AAD(b.value); reg_eip += b.eipCount;continue;
                            case SALC: reg_eax.low(Flags.get_CF() ? 0xFF : 0); reg_eip += b.eipCount;continue;
                            case XLAT16: reg_eax.low(Memory.mem_readb(b.eaa_segPhys.dword+((reg_ebx.word()+reg_eax.low()) & 0xFFFF))); reg_eip += b.eipCount;continue;
                            case XLAT32: reg_eax.low(Memory.mem_readb(b.eaa_segPhys.dword+(reg_ebx.dword+reg_eax.low()))); reg_eip += b.eipCount;continue;
                            case LOOPNZ16_CX: reg_ecx.word(reg_ecx.word()-1);block = jump16(block, reg_ecx.word()!=0 && !Flags.get_ZF(), b.value, b.eipCount);break;
                            case LOOPNZ16_ECX: reg_ecx.dword--;block = jump16(block, reg_ecx.dword!=0 && !Flags.get_ZF(), b.value, b.eipCount);break;
                            case LOOPZ16_CX: reg_ecx.word(reg_ecx.word()-1);block = jump16(block, reg_ecx.word()!=0 && Flags.get_ZF(), b.value, b.eipCount);break;
                            case LOOPZ16_ECX: reg_ecx.dword--;block = jump16(block, reg_ecx.dword!=0 && Flags.get_ZF(), b.value, b.eipCount);break;
                            case LOOP16_CX: reg_ecx.word(reg_ecx.word()-1);block = jump16(block, reg_ecx.word()!=0, b.value, b.eipCount);break;
                            case LOOP16_ECX: reg_ecx.dword--;block = jump16(block, reg_ecx.dword!=0, b.value, b.eipCount);break;
                            case JCXZ16_CX: block = jump16(block, reg_ecx.word()==0, b.value, b.eipCount);break;
                            case JCXZ16_ECX: block = jump16(block, reg_ecx.dword==0, b.value, b.eipCount);break;
                            case LOOPNZ32_CX: reg_ecx.word(reg_ecx.word()-1);block = jump32(block, reg_ecx.word() != 0 && !Flags.get_ZF(), b.value, b.eipCount);break;
                            case LOOPNZ32_ECX: reg_ecx.dword--;block = jump32(block, reg_ecx.dword != 0 && !Flags.get_ZF(), b.value, b.eipCount);break;
                            case LOOPZ32_CX: reg_ecx.word(reg_ecx.word()-1);block = jump32(block, reg_ecx.word() != 0 && Flags.get_ZF(), b.value, b.eipCount);break;
                            case LOOPZ32_ECX: reg_ecx.dword--;block = jump32(block, reg_ecx.dword != 0 && Flags.get_ZF(), b.value, b.eipCount);break;
                            case LOOP32_CX: reg_ecx.word(reg_ecx.word()-1);block = jump32(block, reg_ecx.word() != 0, b.value, b.eipCount);break;
                            case LOOP32_ECX: reg_ecx.dword--;block = jump32(block, reg_ecx.dword != 0, b.value, b.eipCount);break;
                            case JCXZ32_CX: block = jump32(block, reg_ecx.word() == 0, b.value, b.eipCount);break;
                            case JCXZ32_ECX: block = jump32(block, reg_ecx.dword == 0, b.value, b.eipCount);break;
                            case IN_AL_Ib:if (CPU.CPU_IO_Exception(b.value,1)) {RUNEXCEPTION();block=null;break;} reg_eax.low(IO.IO_ReadB(b.value));reg_eip += b.eipCount;continue;
                            case IN_AX_Ib:if (CPU.CPU_IO_Exception(b.value,2)) {RUNEXCEPTION();block=null;break;} reg_eax.word(IO.IO_ReadW(b.value));reg_eip += b.eipCount;continue;
                            case IN_EAX_Ib:if (CPU.CPU_IO_Exception(b.value,4)) {RUNEXCEPTION();block=null;break;} reg_eax.dword=IO.IO_ReadD(b.value);reg_eip += b.eipCount;continue;
                            case OUT_Ib_AL:if (CPU.CPU_IO_Exception(b.value,1)) {RUNEXCEPTION();block=null;break;} IO.IO_WriteB(b.value,reg_eax.low());reg_eip += b.eipCount;continue;
                            case OUT_Ib_AX:if (CPU.CPU_IO_Exception(b.value,2)) {RUNEXCEPTION();block=null;break;} IO.IO_WriteW(b.value, reg_eax.word());reg_eip += b.eipCount;continue;
                            case OUT_Ib_EAX:if (CPU.CPU_IO_Exception(b.value,4)) {RUNEXCEPTION();block=null;break;} IO.IO_WriteD(b.value, reg_eax.dword);reg_eip += b.eipCount;continue;
                            case CALL16_Jw:CPU.CPU_Push16(reg_eip+b.eipCount);reg_ip(reg_eip+b.eipCount+b.value); block=link1(block); break;
                            case CALL32_Jd:CPU.CPU_Push32(reg_eip + b.eipCount);reg_eip+=b.eipCount+b.value; block=link1(block); break;
                            case JMP16:reg_ip(reg_eip+b.eipCount+b.value); block=link1(block); break;
                            case JMP32:reg_eip+=b.eipCount+b.value; block=link1(block); break;
                            case JMP_AP:
                                Flags.FillFlags();
                                CPU.CPU_JMP(!b.eaa16,b.eaa_const,b.value,reg_eip+b.eipCount);
                                if (GETFLAG(TF)!=0) {
                                    CPU.cpudecoder=CPU_Core_Switch_Trap_Run;
                                    return Callback.CBRET_NONE;
                                }
                                block=null;
                                break;
                            case JMP16_EP:
                                Flags.FillFlags();
                                eaa=eaSlow(b);
                                CPU.CPU_JMP(false,Memory.mem_readw(eaa+2),Memory.mem_readw(eaa),reg_eip+b.eipCount);
                                if (GETFLAG(TF)!=0) {
                                    CPU.cpudecoder=CPU_Core_Switch_Trap_Run;
                                    return Callback.CBRET_NONE;
                                }
                                block=null;
                                break;
                            case JMP32_EP:
                                Flags.FillFlags();
                                eaa=eaSlow(b);
                                CPU.CPU_JMP(false,Memory.mem_readw(eaa+4),Memory.mem_readd(eaa),reg_eip+b.eipCount);
                                if (GETFLAG(TF)!=0) {
                                    CPU.cpudecoder=CPU_Core_Switch_Trap_Run;
                                    return Callback.CBRET_NONE;
                                }
                                block=null;
                                break;
                            case IN_AL_DX:if (CPU.CPU_IO_Exception(reg_edx.word(),1)) {RUNEXCEPTION();block=null;break;} reg_eax.low(IO.IO_ReadB(reg_edx.word()));reg_eip += b.eipCount;continue;
                            case IN_AX_DX:if (CPU.CPU_IO_Exception(reg_edx.word(),2)) {RUNEXCEPTION();block=null;break;} reg_eax.word(IO.IO_ReadW(reg_edx.word()));reg_eip += b.eipCount;continue;
                            case IN_EAX_DX:if (CPU.CPU_IO_Exception(reg_edx.word(),4)) {RUNEXCEPTION();block=null;break;} reg_eax.dword=IO.IO_ReadD(reg_edx.word());reg_eip += b.eipCount;continue;
                            case OUT_DX_AL:if (CPU.CPU_IO_Exception(reg_edx.word(),1)) {RUNEXCEPTION();block=null;break;} IO.IO_WriteB(reg_edx.word(),reg_eax.low());reg_eip += b.eipCount;continue;
                            case OUT_DX_AX:if (CPU.CPU_IO_Exception(reg_edx.word(),2)) {RUNEXCEPTION();block=null;break;} IO.IO_WriteW(reg_edx.word(),reg_eax.word());reg_eip += b.eipCount;continue;
                            case OUT_DX_EAX:if (CPU.CPU_IO_Exception(reg_edx.word(),4)) {RUNEXCEPTION();block=null;break;} IO.IO_WriteD(reg_edx.word(), reg_eax.dword);reg_eip += b.eipCount;continue;
                            case ICEBP:CPU.CPU_SW_Interrupt_NoIOPLCheck(1,reg_eip+b.eipCount);CPU.cpu.trap_skip=true;block=null;break;
                            case HLT: if (CPU.cpu.pmode && CPU.cpu.cpl!=0) {CPU.CPU_Exception(CPU.EXCEPTION_GP);block=null;break;}Flags.FillFlags();CPU.CPU_HLT(reg_eip+b.eipCount);return Callback.CBRET_NONE;
                            case CMC: Flags.FillFlags(); SETFLAGBIT(CF,(CPU_Regs.flags & CPU_Regs.CF)==0);reg_eip += b.eipCount;continue;
                            case NOT_R8: b.r1.set8(~b.r1.get8());reg_eip+=b.eipCount;continue;
                            case NOT_E8: eaa=eaSlow(b);Memory.mem_writeb(eaa, ~Memory.mem_readb(eaa));reg_eip+=b.eipCount;continue;
                            case NOT_R16: b.r1.word(~b.r1.word());reg_eip+=b.eipCount;continue;
                            case NOT_E16: eaa=eaSlow(b);Memory.mem_writew(eaa, ~Memory.mem_readw(eaa));reg_eip+=b.eipCount;continue;
                            case NOT_R32: b.r1.dword=~b.r1.dword;reg_eip+=b.eipCount;continue;
                            case NOT_E32: eaa=eaSlow(b);Memory.mem_writed(eaa, ~Memory.mem_readd(eaa));reg_eip+=b.eipCount;continue;

                            case NEG_R8: b.r1.set8(Instructions.Negb(b.r1.get8()));reg_eip+=b.eipCount;continue;
                            case NEG_E8: eaa=eaSlow(b);Memory.mem_writeb(eaa, Instructions.Negb(Memory.mem_readb(eaa)));reg_eip+=b.eipCount;continue;
                            case NEG_R16: b.r1.word(Instructions.Negw(b.r1.word()));reg_eip+=b.eipCount;continue;
                            case NEG_E16: eaa=eaSlow(b);Memory.mem_writew(eaa, Instructions.Negw(Memory.mem_readw(eaa)));reg_eip+=b.eipCount;continue;
                            case NEG_R32: b.r1.dword=Instructions.Negd(b.r1.dword);reg_eip+=b.eipCount;continue;
                            case NEG_E32: eaa=eaSlow(b);Memory.mem_writed(eaa, Instructions.Negd(Memory.mem_readd(eaa)));reg_eip+=b.eipCount;continue;

                            case MUL_R8: Instructions.MULB(b.r1.get8());reg_eip+=b.eipCount;continue;
                            case MUL_E8: Instructions.MULB(Memory.mem_readb(eaSlow(b)));reg_eip+=b.eipCount;continue;
                            case MUL_R16: Instructions.MULW(b.r1.word());reg_eip+=b.eipCount;continue;
                            case MUL_E16: Instructions.MULW(Memory.mem_readw(eaSlow(b)));reg_eip+=b.eipCount;continue;
                            case MUL_R32: Instructions.MULD(b.r1.dword);reg_eip+=b.eipCount;continue;
                            case MUL_E32: Instructions.MULD(Memory.mem_readd(eaSlow(b)));reg_eip+=b.eipCount;continue;

                            case IMUL_R8: Instructions.IMULB(b.r1.get8());reg_eip+=b.eipCount;continue;
                            case IMUL_E8: Instructions.IMULB(Memory.mem_readb(eaSlow(b)));reg_eip+=b.eipCount;continue;
                            case IMUL_R16: Instructions.IMULW(b.r1.word());reg_eip+=b.eipCount;continue;
                            case IMUL_E16: Instructions.IMULW(Memory.mem_readw(eaSlow(b)));reg_eip+=b.eipCount;continue;
                            case IMUL_R32: Instructions.IMULD(b.r1.dword);reg_eip+=b.eipCount;continue;
                            case IMUL_E32: Instructions.IMULD(Memory.mem_readd(eaSlow(b)));reg_eip+=b.eipCount;continue;

                            case DIV_R8: Instructions.DIVB(b.r1.get8());reg_eip+=b.eipCount;continue;
                            case DIV_E8: Instructions.DIVB(Memory.mem_readb(eaSlow(b)));reg_eip+=b.eipCount;continue;
                            case DIV_R16: Instructions.DIVW(b.r1.word());reg_eip+=b.eipCount;continue;
                            case DIV_E16: Instructions.DIVW(Memory.mem_readw(eaSlow(b)));reg_eip+=b.eipCount;continue;
                            case DIV_R32: Instructions.DIVD(b.r1.dword);reg_eip+=b.eipCount;continue;
                            case DIV_E32: Instructions.DIVD(Memory.mem_readd(eaSlow(b)));reg_eip+=b.eipCount;continue;

                            case IDIV_R8: Instructions.IDIVB(b.r1.get8());reg_eip+=b.eipCount;continue;
                            case IDIV_E8: Instructions.IDIVB(Memory.mem_readb(eaSlow(b)));reg_eip+=b.eipCount;continue;
                            case IDIV_R16: Instructions.IDIVW(b.r1.word());reg_eip+=b.eipCount;continue;
                            case IDIV_E16: Instructions.IDIVW(Memory.mem_readw(eaSlow(b)));reg_eip+=b.eipCount;continue;
                            case IDIV_R32: Instructions.IDIVD(b.r1.dword);reg_eip+=b.eipCount;continue;
                            case IDIV_E32: Instructions.IDIVD(Memory.mem_readd(eaSlow(b)));reg_eip+=b.eipCount;continue;

                            case CLC:Flags.FillFlags();SETFLAGBIT(CF,false);reg_eip+=b.eipCount;continue;
                            case STC:Flags.FillFlags();SETFLAGBIT(CF,true);reg_eip+=b.eipCount;continue;
                            case CLI:if (CPU.CPU_CLI()) {RUNEXCEPTION();block=null;break;}reg_eip+=b.eipCount;continue;
                            case STI:if (CPU.CPU_STI()) {RUNEXCEPTION();block=null;break;}reg_eip+=b.eipCount;if (GETFLAG(IF)!=0 && Pic.PIC_IRQCheck!=0) {Flags.FillFlags();return Callback.CBRET_NONE;} continue;
                            case CLD:SETFLAGBIT(DF,false); CPU.cpu.direction=1;reg_eip+=b.eipCount;continue;
                            case STD:SETFLAGBIT(DF,true); CPU.cpu.direction=-1;reg_eip+=b.eipCount;continue;

                            case INC_E8: eaa=eaSlow(b);Memory.mem_writeb(eaa, Instructions.INCB(Memory.mem_readb(eaa)));reg_eip+=b.eipCount;continue;
                            case INC_E16: eaa=eaSlow(b);Memory.mem_writew(eaa, Instructions.INCW(Memory.mem_readw(eaa)));reg_eip+=b.eipCount;continue;
                            case INC_E32: eaa=eaSlow(b);Memory.mem_writed(eaa, Instructions.INCD(Memory.mem_readd(eaa)));reg_eip+=b.eipCount;continue;

                            case DEC_E8: eaa=eaSlow(b);Memory.mem_writeb(eaa, Instructions.DECB(Memory.mem_readb(eaa)));reg_eip+=b.eipCount;continue;
                            case DEC_E16: eaa=eaSlow(b);Memory.mem_writew(eaa, Instructions.DECW(Memory.mem_readw(eaa)));reg_eip+=b.eipCount;continue;
                            case DEC_E32: eaa=eaSlow(b);Memory.mem_writed(eaa, Instructions.DECD(Memory.mem_readd(eaa)));reg_eip+=b.eipCount;continue;
                            case CALLBACK: reg_eip+=b.eipCount; return b.value;
                            case CALL16_R16: tmp = reg_eip+b.eipCount; CPU.CPU_Push16(tmp);reg_eip=b.r1.word();block=null;break;
                            case CALL16_E16: tmp = reg_eip+b.eipCount; tmp2=Memory.mem_readw(eaSlow(b));CPU.CPU_Push16(tmp);reg_eip=tmp2;block=null;break;
                            case CALL32_R16: tmp = reg_eip+b.eipCount; CPU.CPU_Push32(tmp);reg_eip=b.r1.word();block=null;break;
                            case CALL32_E16: tmp = reg_eip+b.eipCount; tmp2=Memory.mem_readd(eaSlow(b));CPU.CPU_Push32(tmp);reg_eip=tmp2;block=null;break;
                            case CALL16_EP_E16: eaa=eaSlow(b);CPU.CPU_CALL(false, Memory.mem_readw(eaa+2), Memory.mem_readw(eaa), reg_eip+b.eipCount);block=null;break;
                            case CALL32_EP_E32: eaa=eaSlow(b);CPU.CPU_CALL(true, Memory.mem_readw(eaa+4), Memory.mem_readd(eaa), reg_eip+b.eipCount);block=null;break;
                            case JMP16_R16: reg_eip=b.r1.word();block=null;break;
                            case JMP16_E16: reg_eip=Memory.mem_readw(eaSlow(b));block=null;break;
                            case JMP32_R32: reg_eip=b.r1.dword;block=null;break;
                            case JMP32_E32: reg_eip=Memory.mem_readd(eaSlow(b));block=null;break;

                            case IMUL_R32_R32: b.r2.dword=Instructions.DIMULD(b.r1.dword, b.value); reg_eip+=b.eipCount; continue;
                            case IMUL_R32_E32: b.r1.dword=Instructions.DIMULD(Memory.mem_readd(eaSlow(b)), b.value); reg_eip+=b.eipCount; continue;

                            case JUMP32_JO: block = jump32(block, Flags.TFLG_O(), b.value, b.eipCount); break;
                            case JUMP32_NJO: block = jump32(block, Flags.TFLG_NO(), b.value, b.eipCount); break;
                            case JUMP32_B: block = jump32(block, Flags.TFLG_B(), b.value, b.eipCount); break;
                            case JUMP32_NB: block = jump32(block, Flags.TFLG_NB(), b.value, b.eipCount); break;
                            case JUMP32_Z: block = jump32(block, Flags.TFLG_Z(), b.value, b.eipCount); break;
                            case JUMP32_NZ: block = jump32(block, Flags.TFLG_NZ(), b.value, b.eipCount); break;
                            case JUMP32_BE: block = jump32(block, Flags.TFLG_BE(), b.value, b.eipCount); break;
                            case JUMP32_NBE: block = jump32(block, Flags.TFLG_NBE(), b.value, b.eipCount); break;
                            case JUMP32_S: block = jump32(block, Flags.TFLG_S(), b.value, b.eipCount); break;
                            case JUMP32_NS: block = jump32(block, Flags.TFLG_NS(), b.value, b.eipCount); break;
                            case JUMP32_P: block = jump32(block, Flags.TFLG_P(), b.value, b.eipCount); break;
                            case JUMP32_NP: block = jump32(block, Flags.TFLG_NP(), b.value, b.eipCount); break;
                            case JUMP32_L: block = jump32(block, Flags.TFLG_L(), b.value, b.eipCount); break;
                            case JUMP32_NL: block = jump32(block, Flags.TFLG_NL(), b.value, b.eipCount); break;
                            case JUMP32_LE: block = jump32(block, Flags.TFLG_LE(), b.value, b.eipCount); break;
                            case JUMP32_NLE: block = jump32(block, Flags.TFLG_NLE(), b.value, b.eipCount); break;
                            case CBWE: reg_eax.dword=(short)reg_eax.word();reg_eip+=b.eipCount; continue;
                            case CDQ: if ((reg_eax.dword & 0x80000000)!=0) reg_edx.dword=0xffffffff; else reg_edx.dword=0;reg_eip+=b.eipCount; continue;
                            case MOV_EAX_0d: reg_eax.dword=Memory.mem_readd(b.eaa_segPhys.dword+b.value);reg_eip+=b.eipCount;continue;
                            case MOV_0d_EAX: Memory.mem_writed(b.eaa_segPhys.dword+b.value,reg_eax.dword);reg_eip+=b.eipCount;continue;
                            case MOVSD16: Strings.Movsd16.doString(b.eaa_segPhys.dword);reg_eip+=b.eipCount;continue;
                            case MOVSD16r: Strings.Movsd16r.doString(b.eaa_segPhys.dword);reg_eip+=b.eipCount;continue;
                            case MOVSD32: Strings.Movsd32.doString(b.eaa_segPhys.dword);reg_eip+=b.eipCount;continue;
                            case MOVSD32r: Strings.Movsd32r.doString(b.eaa_segPhys.dword);reg_eip+=b.eipCount;continue;

                            // (Ew << Ib) | (Ew >>> (32-Ib)
                            case ROLD_R32: b.r1.dword=Instructions.ROLD(b.value, b.r1.dword);reg_eip += b.eipCount;continue;
                            case ROLD_E32: eaa=eaSlow(b);Memory.mem_writed(eaa, Instructions.ROLD(b.value, Memory.mem_readd(eaa)));reg_eip += b.eipCount;continue;

                            // (Ew >>> Ib) | (Ew << (32-Ib))
                            case RORD_R32: b.r1.dword=Instructions.RORD(b.value, b.r1.dword);reg_eip += b.eipCount;continue;
                            case RORD_E32: eaa=eaSlow(b);Memory.mem_writed(eaa, Instructions.RORD(b.value, Memory.mem_readd(eaa)));reg_eip += b.eipCount;continue;

                                // (Ew << Ib) |(cf << (Ib-1)) | (Ew >>> (33-Ib));
                            case RCLD_R32: b.r1.dword=Instructions.RCLD(b.value, b.r1.dword);reg_eip += b.eipCount;continue;
                            case RCLD_E32: eaa=eaSlow(b);Memory.mem_writed(eaa, Instructions.RCLD(b.value, Memory.mem_readd(eaa)));reg_eip += b.eipCount;continue;

                                // (Ew >>> Ib) | (cf << (32-Ib)) | (Ew << (33-Ib))
                            case RCRD_R32: b.r1.dword=Instructions.RCRD(b.value, b.r1.dword);reg_eip += b.eipCount;continue;
                            case RCRD_E32: eaa=eaSlow(b);Memory.mem_writed(eaa, Instructions.RCRD(b.value, Memory.mem_readd(eaa)));reg_eip += b.eipCount;continue;

                                // Ew << Ib
                            case SHLD_R32: b.r1.dword=Instructions.SHLD(b.value, b.r1.dword);reg_eip += b.eipCount;continue;
                            case SHLD_E32: eaa=eaSlow(b);Memory.mem_writed(eaa, Instructions.SHLD(b.value, Memory.mem_readd(eaa)));reg_eip += b.eipCount;continue;

                                // Ew >>> Ib
                            case SHRD_R32: b.r1.dword=Instructions.SHRD(b.value, b.r1.dword);reg_eip += b.eipCount;continue;
                            case SHRD_E32: eaa=eaSlow(b);Memory.mem_writed(eaa, Instructions.SHRD(b.value, Memory.mem_readd(eaa)));reg_eip += b.eipCount;continue;

                                // Ew >> Ib
                            case SARD_R32: b.r1.dword=Instructions.SARD(b.value, b.r1.dword);reg_eip += b.eipCount;continue;
                            case SARD_E32: eaa=eaSlow(b);Memory.mem_writed(eaa, Instructions.SARD(b.value, Memory.mem_readd(eaa)));reg_eip += b.eipCount;continue;

                                // (Ew << CL) | (Ew >>> (32-CL)
                            case ROLD_R32_CL: tmp2=(reg_ecx.dword & 0x1f); if (tmp2!=0) {b.r1.dword=Instructions.ROLD(tmp2, b.r1.dword);}reg_eip += b.eipCount;continue;
                            case ROLD_E32_CL: tmp2=(reg_ecx.dword & 0x1f); if (tmp2!=0) {eaa=eaSlow(b); tmp=Memory.mem_readd(eaa); Memory.mem_writed(eaa, Instructions.ROLD(tmp2, tmp));}reg_eip += b.eipCount;continue;

                                // (Ew >>> CL) | (Ew << (32-CL))
                            case RORD_R32_CL: tmp2=(reg_ecx.dword & 0x1f); if (tmp2!=0) {b.r1.dword=Instructions.RORD(tmp2, b.r1.dword);}reg_eip += b.eipCount;continue;
                            case RORD_E32_CL: tmp2=(reg_ecx.dword & 0x1f); if (tmp2!=0) {eaa=eaSlow(b); tmp=Memory.mem_readd(eaa); Memory.mem_writed(eaa, Instructions.RORD(tmp2, tmp));}reg_eip += b.eipCount;continue;

                                // (Ew << CL) |(cf << (CL-1)) | (Ew >>> (33-CL));
                            case RCLD_R32_CL: tmp2=(reg_ecx.dword & 0x1f); if (tmp2!=0) {b.r1.dword=Instructions.RCLD(tmp2, b.r1.dword);}reg_eip += b.eipCount;continue;
                            case RCLD_E32_CL: tmp2=(reg_ecx.dword & 0x1f); if (tmp2!=0) {eaa=eaSlow(b);Memory.mem_writed(eaa, Instructions.RCLD(tmp2, Memory.mem_readd(eaa)));}reg_eip += b.eipCount;continue;

                                // (Ew >>> CL) | (cf << (32-CL)) | (Ew << (33-CL))
                            case RCRD_R32_CL: tmp2=(reg_ecx.dword & 0x1f); if (tmp2!=0) {b.r1.dword=Instructions.RCRD(tmp2, b.r1.dword);}reg_eip += b.eipCount;continue;
                            case RCRD_E32_CL: tmp2=(reg_ecx.dword & 0x1f); if (tmp2!=0) {eaa=eaSlow(b);Memory.mem_writed(eaa, Instructions.RCRD(tmp2, Memory.mem_readd(eaa)));}reg_eip += b.eipCount;continue;

                                // Ew << CL
                            case SHLD_R32_CL: tmp2=(reg_ecx.dword & 0x1f); if (tmp2!=0) {b.r1.dword=Instructions.SHLD(tmp2, b.r1.dword);}reg_eip += b.eipCount;continue;
                            case SHLD_E32_CL: tmp2=(reg_ecx.dword & 0x1f); if (tmp2!=0) {eaa=eaSlow(b);Memory.mem_writed(eaa, Instructions.SHLD(tmp2, Memory.mem_readd(eaa)));}reg_eip += b.eipCount;continue;

                                // Ew >>> CL
                            case SHRD_R32_CL: tmp2=(reg_ecx.dword & 0x1f); if (tmp2!=0) {b.r1.dword=Instructions.SHRD(tmp2, b.r1.dword);}reg_eip += b.eipCount;continue;
                            case SHRD_E32_CL: tmp2=(reg_ecx.dword & 0x1f); if (tmp2!=0) {eaa=eaSlow(b);Memory.mem_writed(eaa, Instructions.SHRD(tmp2, Memory.mem_readd(eaa)));}reg_eip += b.eipCount;continue;

                                // Ew >> CL
                            case SARD_R32_CL: tmp2=(reg_ecx.dword & 0x1f); if (tmp2!=0) {b.r1.dword=Instructions.SARD(tmp2, b.r1.dword);}reg_eip += b.eipCount;continue;
                            case SARD_E32_CL: tmp2=(reg_ecx.dword & 0x1f); if (tmp2!=0) {eaa=eaSlow(b);Memory.mem_writed(eaa, Instructions.SARD(tmp2, Memory.mem_readd(eaa)));}reg_eip += b.eipCount;continue;

                            case RETN32_Iw: reg_eip=CPU.CPU_Pop32(); reg_esp.dword+=b.value; block=null; break;
                            case RETN32: reg_eip=CPU.CPU_Pop32(); block=null; break;
                            case LEAVE32:reg_esp.dword&=CPU.cpu.stack.notmask;reg_esp.dword|=(reg_ebp.dword & CPU.cpu.stack.mask);reg_ebp.dword=CPU.CPU_Pop32();reg_eip += b.eipCount;continue;
                            case SLDT_R16: if ((CPU_Regs.flags & CPU_Regs.VM)!=0 || (!CPU.cpu.pmode)) {CPU.CPU_Exception(6,0); block = null; break;} b.r1.word(CPU.CPU_SLDT());reg_eip += b.eipCount;continue;
                            case SLDT_E16: if ((CPU_Regs.flags & CPU_Regs.VM)!=0 || (!CPU.cpu.pmode)) {CPU.CPU_Exception(6,0); block = null; break;} Memory.mem_writew(eaSlow(b), CPU.CPU_SLDT());reg_eip += b.eipCount;continue;
                            case STR_R16: if ((CPU_Regs.flags & CPU_Regs.VM)!=0 || (!CPU.cpu.pmode)) {CPU.CPU_Exception(6,0); block = null; break;} b.r1.word(CPU.CPU_STR());reg_eip += b.eipCount;continue;
                            case STR_E16: if ((CPU_Regs.flags & CPU_Regs.VM)!=0 || (!CPU.cpu.pmode)) {CPU.CPU_Exception(6,0); block = null; break;} Memory.mem_writew(eaSlow(b), CPU.CPU_STR());reg_eip += b.eipCount;continue;
                            case LLDT_R16: if ((CPU_Regs.flags & CPU_Regs.VM)!=0 || (!CPU.cpu.pmode)) {CPU.CPU_Exception(6,0); block = null; break;} if (CPU.cpu.cpl!=0) {CPU.CPU_Exception(CPU.EXCEPTION_GP);block=null;break;}if (CPU.CPU_LLDT(b.r1.word())) {RUNEXCEPTION();block=null;break;}reg_eip += b.eipCount;continue;
                            case LLDT_E16: if ((CPU_Regs.flags & CPU_Regs.VM)!=0 || (!CPU.cpu.pmode)) {CPU.CPU_Exception(6,0); block = null; break;} if (CPU.cpu.cpl!=0) {CPU.CPU_Exception(CPU.EXCEPTION_GP);block=null;break;}if (CPU.CPU_LLDT(Memory.mem_readw(eaSlow(b)))) {RUNEXCEPTION();block=null;break;}reg_eip += b.eipCount;continue;
                            case LTR_R16: if ((CPU_Regs.flags & CPU_Regs.VM)!=0 || (!CPU.cpu.pmode)) {CPU.CPU_Exception(6,0); block = null; break;} if (CPU.cpu.cpl!=0) {CPU.CPU_Exception(CPU.EXCEPTION_GP);block=null;break;}if (CPU.CPU_LTR(b.r1.word())) {RUNEXCEPTION();block=null;break;}reg_eip += b.eipCount;continue;
                            case LTR_E16: if ((CPU_Regs.flags & CPU_Regs.VM)!=0 || (!CPU.cpu.pmode)) {CPU.CPU_Exception(6,0); block = null; break;} if (CPU.cpu.cpl!=0) {CPU.CPU_Exception(CPU.EXCEPTION_GP);block=null;break;}if (CPU.CPU_LTR(Memory.mem_readw(eaSlow(b)))) {RUNEXCEPTION();block=null;break;}reg_eip += b.eipCount;continue;
                            case VERR_R16: if ((CPU_Regs.flags & CPU_Regs.VM)!=0 || (!CPU.cpu.pmode)) {CPU.CPU_Exception(6,0); block = null; break;} if (CPU.cpu.cpl!=0) {CPU.CPU_Exception(CPU.EXCEPTION_GP);block=null;break;}CPU.CPU_VERR(b.r1.word());reg_eip += b.eipCount;continue;
                            case VERR_E16: if ((CPU_Regs.flags & CPU_Regs.VM)!=0 || (!CPU.cpu.pmode)) {CPU.CPU_Exception(6,0); block = null; break;} if (CPU.cpu.cpl!=0) {CPU.CPU_Exception(CPU.EXCEPTION_GP);block=null;break;}CPU.CPU_VERR(Memory.mem_readw(eaSlow(b)));reg_eip += b.eipCount;continue;
                            case VERW_R16: if ((CPU_Regs.flags & CPU_Regs.VM)!=0 || (!CPU.cpu.pmode)) {CPU.CPU_Exception(6,0); block = null; break;} if (CPU.cpu.cpl!=0) {CPU.CPU_Exception(CPU.EXCEPTION_GP);block=null;break;}CPU.CPU_VERW(b.r1.word());reg_eip += b.eipCount;continue;
                            case VERW_E16: if ((CPU_Regs.flags & CPU_Regs.VM)!=0 || (!CPU.cpu.pmode)) {CPU.CPU_Exception(6,0); block = null; break;} if (CPU.cpu.cpl!=0) {CPU.CPU_Exception(CPU.EXCEPTION_GP);block=null;break;}CPU.CPU_VERW(Memory.mem_readw(eaSlow(b)));reg_eip += b.eipCount;continue;
                            case SGDT: eaa=eaSlow(b); Memory.mem_writew(eaa,CPU.CPU_SGDT_limit()); Memory.mem_writed(eaa+2,CPU.CPU_SGDT_base());reg_eip += b.eipCount;continue;
                            case SIDT: eaa=eaSlow(b); Memory.mem_writew(eaa,CPU.CPU_SIDT_limit()); Memory.mem_writed(eaa+2,CPU.CPU_SIDT_base());reg_eip += b.eipCount;continue;
                            case LGDT16: if (CPU.cpu.pmode && CPU.cpu.cpl!=0) {CPU.CPU_Exception(CPU.EXCEPTION_GP);block=null;break;} eaa=eaSlow(b); CPU.CPU_LGDT(Memory.mem_readw(eaa),Memory.mem_readd(eaa + 2) & 0xFFFFFF);reg_eip += b.eipCount;continue;
                            case LGDT32: if (CPU.cpu.pmode && CPU.cpu.cpl!=0) {CPU.CPU_Exception(CPU.EXCEPTION_GP);block=null;break;} eaa=eaSlow(b); CPU.CPU_LGDT(Memory.mem_readw(eaa),Memory.mem_readd(eaa + 2));reg_eip += b.eipCount;continue;
                            case LIDT16: if (CPU.cpu.pmode && CPU.cpu.cpl!=0) {CPU.CPU_Exception(CPU.EXCEPTION_GP);block=null;break;} eaa=eaSlow(b); CPU.CPU_LIDT(Memory.mem_readw(eaa), Memory.mem_readd(eaa + 2) & 0xFFFFFF);reg_eip += b.eipCount;continue;
                            case LIDT32: if (CPU.cpu.pmode && CPU.cpu.cpl!=0) {CPU.CPU_Exception(CPU.EXCEPTION_GP);block=null;break;} eaa=eaSlow(b); CPU.CPU_LIDT(Memory.mem_readw(eaa), Memory.mem_readd(eaa + 2));reg_eip += b.eipCount;continue;
                            case SMSW_E16: Memory.mem_writew(eaSlow(b),CPU.CPU_SMSW() & 0xFFFF);reg_eip += b.eipCount;continue;
                            case LMSW_E16: if (CPU.CPU_LMSW(Memory.mem_readw(eaSlow(b)))) {RUNEXCEPTION();block=null;break;}reg_eip += b.eipCount;continue;
                            case INVLPG: if (CPU.cpu.pmode && CPU.cpu.cpl!=0) {CPU.CPU_Exception(CPU.EXCEPTION_GP);block=null;break;} Paging.PAGING_ClearTLB();reg_eip += b.eipCount;continue;
                            case LGDT_R:
                            case LIDT_R: if (CPU.cpu.pmode && CPU.cpu.cpl!=0) {CPU.CPU_Exception(CPU.EXCEPTION_GP);block=null;break;} CPU.CPU_Exception(6,0); block = null; break;
                            case SMSW_R16: b.r1.word(CPU.CPU_SMSW() & 0xFFFF);reg_eip += b.eipCount;continue;
                            case LMSW_R16: if (CPU.CPU_LMSW(b.r1.word())) {RUNEXCEPTION();block=null;break;}reg_eip += b.eipCount;continue;
                            case SMSW_R32: b.r1.dword=CPU.CPU_SMSW();reg_eip += b.eipCount;continue;

                            case FPU0_normal: FPU.FPU_ESC0_Normal(b.value);reg_eip += b.eipCount;continue;
                            case FPU0_ea: FPU.FPU_ESC0_EA(b.value,eaSlow(b));reg_eip += b.eipCount;continue;
                            case FPU1_normal: FPU.FPU_ESC1_Normal(b.value);reg_eip += b.eipCount;continue;
                            case FPU1_ea: FPU.FPU_ESC1_EA(b.value,eaSlow(b));reg_eip += b.eipCount;continue;
                            case FPU2_normal: FPU.FPU_ESC2_Normal(b.value);reg_eip += b.eipCount;continue;
                            case FPU2_ea: FPU.FPU_ESC2_EA(b.value,eaSlow(b));reg_eip += b.eipCount;continue;
                            case FPU3_normal: FPU.FPU_ESC3_Normal(b.value);reg_eip += b.eipCount;continue;
                            case FPU3_ea: FPU.FPU_ESC3_EA(b.value,eaSlow(b));reg_eip += b.eipCount;continue;
                            case FPU4_normal: FPU.FPU_ESC4_Normal(b.value);reg_eip += b.eipCount;continue;
                            case FPU4_ea: FPU.FPU_ESC4_EA(b.value,eaSlow(b));reg_eip += b.eipCount;continue;
                            case FPU5_normal: FPU.FPU_ESC5_Normal(b.value);reg_eip += b.eipCount;continue;
                            case FPU5_ea: FPU.FPU_ESC5_EA(b.value,eaSlow(b));reg_eip += b.eipCount;continue;
                            case FPU6_normal: FPU.FPU_ESC6_Normal(b.value);reg_eip += b.eipCount;continue;
                            case FPU6_ea: FPU.FPU_ESC6_EA(b.value,eaSlow(b));reg_eip += b.eipCount;continue;
                            case FPU7_normal: FPU.FPU_ESC7_Normal(b.value);reg_eip += b.eipCount;continue;
                            case FPU7_ea: FPU.FPU_ESC7_EA(b.value,eaSlow(b));reg_eip += b.eipCount;continue;
                            case MODIFIED: tmp = ModifiedDecode.call();if (tmp==Constants.BR_CallBack) {Flags.FillFlags(); return Data.callback; } block=null;break;
                        }
                        break;
                    }
                }
            }
            Flags.FillFlags();
            return Callback.CBRET_NONE;
        }
    };
}
