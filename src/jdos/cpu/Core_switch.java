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
import jdos.cpu.core_switch.Decoder;
import jdos.cpu.core_switch.Inst;
import jdos.cpu.core_switch.SwitchBlock;
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

    private static int eaaSlow(SwitchBlock block) {
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
        if (CPU.CPU_Cycles>0) return null;
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
                    for (SwitchBlock switchBlock : block.inst) {
//                        Record.op(switchBlock.opCode);
//                        if (count>0) {
//                            System.out.println(String.format("%d %06x:%08x %3s %-8s EAX=%08x ECX=%08x EDX=%08x EBX=%08x ESP=%08x EBP=%08x ESI=%08x EDI=%08x FLAGS=%04x", count, CPU_Regs.reg_csVal.dword, CPU_Regs.reg_eip, Integer.toHexString(switchBlock.opCode), (Core_normal.desc.length > switchBlock.opCode ? " " + Core_normal.desc[switchBlock.opCode] : ""), CPU_Regs.reg_eax.dword, CPU_Regs.reg_ecx.dword, CPU_Regs.reg_edx.dword, CPU_Regs.reg_ebx.dword, CPU_Regs.reg_esp.dword, CPU_Regs.reg_ebp.dword, CPU_Regs.reg_esi.dword, CPU_Regs.reg_edi.dword, CPU_Regs.flags));
//                            count++;
//                            if ((count%1000)==0) {
//                                int ii=0;
//                            }
//                        }
                        CPU.CPU_Cycles-=block.inst.length;
                        if (count==56) {
                            int ii=0;
                        }

                        switch (switchBlock.instruction) {
                            case Inst.ADD_R8: switchBlock.r1.set8(Instructions.ADDB(switchBlock.value, switchBlock.r1.get8())); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.ADD_R8_fast: switchBlock.r1.set8(switchBlock.r1.get8()+switchBlock.value); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.ADD_E8_16: eaa = ea16(switchBlock); Memory.mem_writeb(eaa, Instructions.ADDB(switchBlock.value, Memory.mem_readb(eaa))); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.ADD_E8_16_fast: eaa = ea16(switchBlock); Memory.mem_writeb(eaa, Memory.mem_readb(eaa)+switchBlock.r1.get8()); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.ADD_E8_32: eaa = ea32(switchBlock); Memory.mem_writeb(eaa, Instructions.ADDB(switchBlock.value, Memory.mem_readb(eaa))); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.ADD_E8_32_fast: eaa = ea32(switchBlock); Memory.mem_writeb(eaa, Memory.mem_readb(eaa)+switchBlock.r1.get8()); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.ADD_E8_32_sib: eaa = ea32sib(switchBlock); Memory.mem_writeb(eaa, Instructions.ADDB(switchBlock.value, Memory.mem_readb(eaa))); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.ADD_E8_32_sib_fast: eaa = ea32sib(switchBlock); Memory.mem_writeb(eaa, Memory.mem_readb(eaa)+switchBlock.r1.get8()); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.ADD_R8_R8: switchBlock.r1.set8(Instructions.ADDB(switchBlock.r2.get8(), switchBlock.r1.get8())); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.ADD_R8_R8_fast: switchBlock.r1.set8(switchBlock.r1.get8()+switchBlock.r2.get8()); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.ADD_E8_R8_16: eaa = ea16(switchBlock); Memory.mem_writeb(eaa, Instructions.ADDB(switchBlock.r1.get8(), Memory.mem_readb(eaa))); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.ADD_E8_R8_16_fast: eaa = ea16(switchBlock); Memory.mem_writeb(eaa, Memory.mem_readb(eaa)+switchBlock.r1.get8()); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.ADD_E8_R8_32: eaa = ea32(switchBlock); Memory.mem_writeb(eaa, Instructions.ADDB(switchBlock.r1.get8(), Memory.mem_readb(eaa))); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.ADD_E8_R8_32_fast:eaa = ea32(switchBlock); Memory.mem_writeb(eaa, Memory.mem_readb(eaa)+switchBlock.r1.get8()); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.ADD_E8_R8_32_sib: eaa = ea32sib(switchBlock); Memory.mem_writeb(eaa, Instructions.ADDB(switchBlock.r1.get8(), Memory.mem_readb(eaa))); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.ADD_E8_R8_32_sib_fast:eaa = ea32sib(switchBlock); Memory.mem_writeb(eaa, Memory.mem_readb(eaa)+switchBlock.r1.get8()); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.ADD_R8_E8_16: switchBlock.r1.set8(Instructions.ADDB(Memory.mem_readb(ea16(switchBlock)), switchBlock.r1.get8())); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.ADD_R8_E8_16_fast: switchBlock.r1.set8(switchBlock.r1.get8()+Memory.mem_readb(ea16(switchBlock))); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.ADD_R8_E8_32: switchBlock.r1.set8(Instructions.ADDB(Memory.mem_readb(ea32(switchBlock)), switchBlock.r1.get8())); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.ADD_R8_E8_32_fast: switchBlock.r1.set8(switchBlock.r1.get8()+Memory.mem_readb(ea32(switchBlock))); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.ADD_R8_E8_32_sib: switchBlock.r1.set8(Instructions.ADDB(Memory.mem_readb(ea32sib(switchBlock)), switchBlock.r1.get8())); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.ADD_R8_E8_32_sib_fast: switchBlock.r1.set8(switchBlock.r1.get8()+Memory.mem_readb(ea32sib(switchBlock))); reg_eip+=switchBlock.eipCount;continue;

                            case Inst.ADD_R16: switchBlock.r1.word(Instructions.ADDW(switchBlock.value, switchBlock.r1.word())); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.ADD_R16_fast: switchBlock.r1.word(switchBlock.r1.word()+switchBlock.value); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.ADD_E16_16: eaa = ea16(switchBlock); Memory.mem_writew(eaa, Instructions.ADDW(switchBlock.value, Memory.mem_readw(eaa))); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.ADD_E16_16_fast: eaa = ea16(switchBlock); Memory.mem_writew(eaa, Memory.mem_readw(eaa)+switchBlock.r1.word()); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.ADD_E16_32: eaa = ea32(switchBlock); Memory.mem_writew(eaa, Instructions.ADDW(switchBlock.value, Memory.mem_readw(eaa))); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.ADD_E16_32_fast: eaa = ea32(switchBlock); Memory.mem_writew(eaa, Memory.mem_readw(eaa)+switchBlock.r1.word()); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.ADD_E16_32_sib: eaa = ea32sib(switchBlock); Memory.mem_writew(eaa, Instructions.ADDW(switchBlock.value, Memory.mem_readw(eaa))); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.ADD_E16_32_sib_fast: eaa = ea32sib(switchBlock); Memory.mem_writew(eaa, Memory.mem_readw(eaa)+switchBlock.r1.word()); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.ADD_R16_R16: switchBlock.r1.word(Instructions.ADDW(switchBlock.r2.word(), switchBlock.r1.word())); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.ADD_R16_R16_fast: switchBlock.r1.word(switchBlock.r1.word()+switchBlock.r2.word()); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.ADD_E16_R16_16: eaa = ea16(switchBlock); Memory.mem_writew(eaa, Instructions.ADDW(switchBlock.r1.word(), Memory.mem_readw(eaa))); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.ADD_E16_R16_16_fast: eaa = ea16(switchBlock); Memory.mem_writew(eaa, Memory.mem_readw(eaa)+switchBlock.r1.word()); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.ADD_E16_R16_32: eaa = ea32(switchBlock); Memory.mem_writew(eaa, Instructions.ADDW(switchBlock.r1.word(), Memory.mem_readw(eaa))); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.ADD_E16_R16_32_fast:eaa = ea32(switchBlock); Memory.mem_writew(eaa, Memory.mem_readw(eaa)+switchBlock.r1.word()); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.ADD_E16_R16_32_sib: eaa = ea32sib(switchBlock); Memory.mem_writew(eaa, Instructions.ADDW(switchBlock.r1.word(), Memory.mem_readw(eaa))); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.ADD_E16_R16_32_sib_fast:eaa = ea32sib(switchBlock); Memory.mem_writew(eaa, Memory.mem_readw(eaa)+switchBlock.r1.word()); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.ADD_R16_E16_16: switchBlock.r1.word(Instructions.ADDW(Memory.mem_readw(ea16(switchBlock)), switchBlock.r1.word())); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.ADD_R16_E16_16_fast: switchBlock.r1.word(switchBlock.r1.word()+Memory.mem_readw(ea16(switchBlock))); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.ADD_R16_E16_32: switchBlock.r1.word(Instructions.ADDW(Memory.mem_readw(ea32(switchBlock)), switchBlock.r1.word())); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.ADD_R16_E16_32_fast: switchBlock.r1.word(switchBlock.r1.word()+Memory.mem_readw(ea32(switchBlock))); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.ADD_R16_E16_32_sib: switchBlock.r1.word(Instructions.ADDW(Memory.mem_readw(ea32sib(switchBlock)), switchBlock.r1.word())); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.ADD_R16_E16_32_sib_fast: switchBlock.r1.word(switchBlock.r1.word()+Memory.mem_readw(ea32sib(switchBlock))); reg_eip+=switchBlock.eipCount;continue;

                            case Inst.ADD_R32: switchBlock.r1.dword=Instructions.ADDD(switchBlock.value, switchBlock.r1.dword); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.ADD_R32_fast: switchBlock.r1.dword=switchBlock.r1.dword+switchBlock.value; reg_eip+=switchBlock.eipCount;continue;
                            case Inst.ADD_E32_16: eaa = ea16(switchBlock); Memory.mem_writed(eaa, Instructions.ADDD(switchBlock.value, Memory.mem_readd(eaa))); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.ADD_E32_16_fast: eaa = ea16(switchBlock); Memory.mem_writed(eaa, Memory.mem_readd(eaa)+switchBlock.r1.dword); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.ADD_E32_32: eaa = ea32(switchBlock); Memory.mem_writed(eaa, Instructions.ADDD(switchBlock.value, Memory.mem_readd(eaa))); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.ADD_E32_32_fast: eaa = ea32(switchBlock); Memory.mem_writed(eaa, Memory.mem_readd(eaa)+switchBlock.r1.dword); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.ADD_E32_32_sib: eaa = ea32sib(switchBlock); Memory.mem_writed(eaa, Instructions.ADDD(switchBlock.value, Memory.mem_readd(eaa))); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.ADD_E32_32_sib_fast: eaa = ea32sib(switchBlock); Memory.mem_writed(eaa, Memory.mem_readd(eaa)+switchBlock.r1.dword); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.ADD_R32_R32: switchBlock.r1.dword=Instructions.ADDD(switchBlock.r2.dword, switchBlock.r1.dword); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.ADD_R32_R32_fast: switchBlock.r1.dword=switchBlock.r1.dword+switchBlock.r2.dword; reg_eip+=switchBlock.eipCount;continue;
                            case Inst.ADD_E32_R32_16: eaa = ea16(switchBlock); Memory.mem_writed(eaa, Instructions.ADDD(switchBlock.r1.dword, Memory.mem_readd(eaa))); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.ADD_E32_R32_16_fast: eaa = ea16(switchBlock); Memory.mem_writed(eaa, Memory.mem_readd(eaa)+switchBlock.r1.dword); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.ADD_E32_R32_32: eaa = ea32(switchBlock); Memory.mem_writed(eaa, Instructions.ADDD(switchBlock.r1.dword, Memory.mem_readd(eaa))); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.ADD_E32_R32_32_fast:eaa = ea32(switchBlock); Memory.mem_writed(eaa, Memory.mem_readd(eaa)+switchBlock.r1.dword); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.ADD_E32_R32_32_sib: eaa = ea32sib(switchBlock); Memory.mem_writed(eaa, Instructions.ADDD(switchBlock.r1.dword, Memory.mem_readd(eaa))); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.ADD_E32_R32_32_sib_fast:eaa = ea32sib(switchBlock); Memory.mem_writed(eaa, Memory.mem_readd(eaa)+switchBlock.r1.dword); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.ADD_R32_E32_16: switchBlock.r1.dword=Instructions.ADDD(Memory.mem_readd(ea16(switchBlock)), switchBlock.r1.dword); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.ADD_R32_E32_16_fast: switchBlock.r1.dword=switchBlock.r1.dword+Memory.mem_readd(ea16(switchBlock)); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.ADD_R32_E32_32: switchBlock.r1.dword=Instructions.ADDD(Memory.mem_readd(ea32(switchBlock)), switchBlock.r1.dword); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.ADD_R32_E32_32_fast: switchBlock.r1.dword=switchBlock.r1.dword+Memory.mem_readd(ea32(switchBlock)); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.ADD_R32_E32_32_sib: switchBlock.r1.dword=Instructions.ADDD(Memory.mem_readd(ea32sib(switchBlock)), switchBlock.r1.dword); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.ADD_R32_E32_32_sib_fast: switchBlock.r1.dword=switchBlock.r1.dword+Memory.mem_readd(ea32sib(switchBlock)); reg_eip+=switchBlock.eipCount;continue;

                            case Inst.OR_R8: switchBlock.r1.set8(Instructions.ORB(switchBlock.value, switchBlock.r1.get8())); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.OR_R8_fast: switchBlock.r1.set8(switchBlock.r1.get8()|switchBlock.value); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.OR_E8_16: eaa = ea16(switchBlock); Memory.mem_writeb(eaa, Instructions.ORB(switchBlock.value, Memory.mem_readb(eaa))); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.OR_E8_16_fast: eaa = ea16(switchBlock); Memory.mem_writeb(eaa, Memory.mem_readb(eaa)|switchBlock.r1.get8()); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.OR_E8_32: eaa = ea32(switchBlock); Memory.mem_writeb(eaa, Instructions.ORB(switchBlock.value, Memory.mem_readb(eaa))); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.OR_E8_32_fast: eaa = ea32(switchBlock); Memory.mem_writeb(eaa, Memory.mem_readb(eaa)|switchBlock.r1.get8()); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.OR_E8_32_sib: eaa = ea32sib(switchBlock); Memory.mem_writeb(eaa, Instructions.ORB(switchBlock.value, Memory.mem_readb(eaa))); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.OR_E8_32_sib_fast: eaa = ea32sib(switchBlock); Memory.mem_writeb(eaa, Memory.mem_readb(eaa)|switchBlock.r1.get8()); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.OR_R8_R8: switchBlock.r1.set8(Instructions.ORB(switchBlock.r2.get8(), switchBlock.r1.get8())); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.OR_R8_R8_fast: switchBlock.r1.set8(switchBlock.r1.get8()|switchBlock.r2.get8()); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.OR_E8_R8_16: eaa = ea16(switchBlock); Memory.mem_writeb(eaa, Instructions.ORB(switchBlock.r1.get8(), Memory.mem_readb(eaa))); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.OR_E8_R8_16_fast: eaa = ea16(switchBlock); Memory.mem_writeb(eaa, Memory.mem_readb(eaa)|switchBlock.r1.get8()); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.OR_E8_R8_32: eaa = ea32(switchBlock); Memory.mem_writeb(eaa, Instructions.ORB(switchBlock.r1.get8(), Memory.mem_readb(eaa))); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.OR_E8_R8_32_fast:eaa = ea32(switchBlock); Memory.mem_writeb(eaa, Memory.mem_readb(eaa)|switchBlock.r1.get8()); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.OR_E8_R8_32_sib: eaa = ea32sib(switchBlock); Memory.mem_writeb(eaa, Instructions.ORB(switchBlock.r1.get8(), Memory.mem_readb(eaa))); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.OR_E8_R8_32_sib_fast:eaa = ea32sib(switchBlock); Memory.mem_writeb(eaa, Memory.mem_readb(eaa)|switchBlock.r1.get8()); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.OR_R8_E8_16: switchBlock.r1.set8(Instructions.ORB(Memory.mem_readb(ea16(switchBlock)), switchBlock.r1.get8())); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.OR_R8_E8_16_fast: switchBlock.r1.set8(switchBlock.r1.get8()|Memory.mem_readb(ea16(switchBlock))); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.OR_R8_E8_32: switchBlock.r1.set8(Instructions.ORB(Memory.mem_readb(ea32(switchBlock)), switchBlock.r1.get8())); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.OR_R8_E8_32_fast: switchBlock.r1.set8(switchBlock.r1.get8()|Memory.mem_readb(ea32(switchBlock))); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.OR_R8_E8_32_sib: switchBlock.r1.set8(Instructions.ORB(Memory.mem_readb(ea32sib(switchBlock)), switchBlock.r1.get8())); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.OR_R8_E8_32_sib_fast: switchBlock.r1.set8(switchBlock.r1.get8()|Memory.mem_readb(ea32sib(switchBlock))); reg_eip+=switchBlock.eipCount;continue;

                            case Inst.OR_R16: switchBlock.r1.word(Instructions.ORW(switchBlock.value, switchBlock.r1.word())); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.OR_R16_fast: switchBlock.r1.word(switchBlock.r1.word()|switchBlock.value); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.OR_E16_16: eaa = ea16(switchBlock); Memory.mem_writew(eaa, Instructions.ORW(switchBlock.value, Memory.mem_readw(eaa))); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.OR_E16_16_fast: eaa = ea16(switchBlock); Memory.mem_writew(eaa, Memory.mem_readw(eaa)|switchBlock.r1.word()); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.OR_E16_32: eaa = ea32(switchBlock); Memory.mem_writew(eaa, Instructions.ORW(switchBlock.value, Memory.mem_readw(eaa))); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.OR_E16_32_fast: eaa = ea32(switchBlock); Memory.mem_writew(eaa, Memory.mem_readw(eaa)|switchBlock.r1.word()); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.OR_E16_32_sib: eaa = ea32sib(switchBlock); Memory.mem_writew(eaa, Instructions.ORW(switchBlock.value, Memory.mem_readw(eaa))); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.OR_E16_32_sib_fast: eaa = ea32sib(switchBlock); Memory.mem_writew(eaa, Memory.mem_readw(eaa)|switchBlock.r1.word()); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.OR_R16_R16: switchBlock.r1.word(Instructions.ORW(switchBlock.r2.word(), switchBlock.r1.word())); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.OR_R16_R16_fast: switchBlock.r1.word(switchBlock.r1.word()|switchBlock.r2.word()); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.OR_E16_R16_16: eaa = ea16(switchBlock); Memory.mem_writew(eaa, Instructions.ORW(switchBlock.r1.word(), Memory.mem_readw(eaa))); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.OR_E16_R16_16_fast: eaa = ea16(switchBlock); Memory.mem_writew(eaa, Memory.mem_readw(eaa)|switchBlock.r1.word()); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.OR_E16_R16_32: eaa = ea32(switchBlock); Memory.mem_writew(eaa, Instructions.ORW(switchBlock.r1.word(), Memory.mem_readw(eaa))); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.OR_E16_R16_32_fast:eaa = ea32(switchBlock); Memory.mem_writew(eaa, Memory.mem_readw(eaa)|switchBlock.r1.word()); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.OR_E16_R16_32_sib: eaa = ea32sib(switchBlock); Memory.mem_writew(eaa, Instructions.ORW(switchBlock.r1.word(), Memory.mem_readw(eaa))); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.OR_E16_R16_32_sib_fast:eaa = ea32sib(switchBlock); Memory.mem_writew(eaa, Memory.mem_readw(eaa)|switchBlock.r1.word()); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.OR_R16_E16_16: switchBlock.r1.word(Instructions.ORW(Memory.mem_readw(ea16(switchBlock)), switchBlock.r1.word())); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.OR_R16_E16_16_fast: switchBlock.r1.word(switchBlock.r1.word()|Memory.mem_readw(ea16(switchBlock))); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.OR_R16_E16_32: switchBlock.r1.word(Instructions.ORW(Memory.mem_readw(ea32(switchBlock)), switchBlock.r1.word())); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.OR_R16_E16_32_fast: switchBlock.r1.word(switchBlock.r1.word()|Memory.mem_readw(ea32(switchBlock))); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.OR_R16_E16_32_sib: switchBlock.r1.word(Instructions.ORW(Memory.mem_readw(ea32sib(switchBlock)), switchBlock.r1.word())); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.OR_R16_E16_32_sib_fast: switchBlock.r1.word(switchBlock.r1.word()|Memory.mem_readw(ea32sib(switchBlock))); reg_eip+=switchBlock.eipCount;continue;

                            case Inst.OR_R32: switchBlock.r1.dword=Instructions.ORD(switchBlock.value, switchBlock.r1.dword); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.OR_R32_fast: switchBlock.r1.dword=switchBlock.r1.dword|switchBlock.value; reg_eip+=switchBlock.eipCount;continue;
                            case Inst.OR_E32_16: eaa = ea16(switchBlock); Memory.mem_writed(eaa, Instructions.ORD(switchBlock.value, Memory.mem_readd(eaa))); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.OR_E32_16_fast: eaa = ea16(switchBlock); Memory.mem_writed(eaa, Memory.mem_readd(eaa)|switchBlock.r1.dword); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.OR_E32_32: eaa = ea32(switchBlock); Memory.mem_writed(eaa, Instructions.ORD(switchBlock.value, Memory.mem_readd(eaa))); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.OR_E32_32_fast: eaa = ea32(switchBlock); Memory.mem_writed(eaa, Memory.mem_readd(eaa)|switchBlock.r1.dword); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.OR_E32_32_sib: eaa = ea32sib(switchBlock); Memory.mem_writed(eaa, Instructions.ORD(switchBlock.value, Memory.mem_readd(eaa))); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.OR_E32_32_sib_fast: eaa = ea32sib(switchBlock); Memory.mem_writed(eaa, Memory.mem_readd(eaa)|switchBlock.r1.dword); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.OR_R32_R32: switchBlock.r1.dword=Instructions.ORD(switchBlock.r2.dword, switchBlock.r1.dword); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.OR_R32_R32_fast: switchBlock.r1.dword=switchBlock.r1.dword|switchBlock.r2.dword; reg_eip+=switchBlock.eipCount;continue;
                            case Inst.OR_E32_R32_16: eaa = ea16(switchBlock); Memory.mem_writed(eaa, Instructions.ORD(switchBlock.r1.dword, Memory.mem_readd(eaa))); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.OR_E32_R32_16_fast: eaa = ea16(switchBlock); Memory.mem_writed(eaa, Memory.mem_readd(eaa)|switchBlock.r1.dword); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.OR_E32_R32_32: eaa = ea32(switchBlock); Memory.mem_writed(eaa, Instructions.ORD(switchBlock.r1.dword, Memory.mem_readd(eaa))); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.OR_E32_R32_32_fast:eaa = ea32(switchBlock); Memory.mem_writed(eaa, Memory.mem_readd(eaa)|switchBlock.r1.dword); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.OR_E32_R32_32_sib: eaa = ea32sib(switchBlock); Memory.mem_writed(eaa, Instructions.ORD(switchBlock.r1.dword, Memory.mem_readd(eaa))); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.OR_E32_R32_32_sib_fast:eaa = ea32sib(switchBlock); Memory.mem_writed(eaa, Memory.mem_readd(eaa)|switchBlock.r1.dword); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.OR_R32_E32_16: switchBlock.r1.dword=Instructions.ORD(Memory.mem_readd(ea16(switchBlock)), switchBlock.r1.dword); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.OR_R32_E32_16_fast: switchBlock.r1.dword=switchBlock.r1.dword|Memory.mem_readd(ea16(switchBlock)); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.OR_R32_E32_32: switchBlock.r1.dword=Instructions.ORD(Memory.mem_readd(ea32(switchBlock)), switchBlock.r1.dword); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.OR_R32_E32_32_fast: switchBlock.r1.dword=switchBlock.r1.dword|Memory.mem_readd(ea32(switchBlock)); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.OR_R32_E32_32_sib: switchBlock.r1.dword=Instructions.ORD(Memory.mem_readd(ea32sib(switchBlock)), switchBlock.r1.dword); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.OR_R32_E32_32_sib_fast: switchBlock.r1.dword=switchBlock.r1.dword|Memory.mem_readd(ea32sib(switchBlock)); reg_eip+=switchBlock.eipCount;continue;

                            case Inst.ADC_R8: switchBlock.r1.set8(Instructions.ADCB(switchBlock.value, switchBlock.r1.get8())); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.ADC_R8_fast: switchBlock.r1.set8(switchBlock.r1.get8()+switchBlock.value+(Flags.get_CF()?1:0)); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.ADC_E8_16: eaa = ea16(switchBlock); Memory.mem_writeb(eaa, Instructions.ADCB(switchBlock.value, Memory.mem_readb(eaa))); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.ADC_E8_16_fast: eaa = ea16(switchBlock); Memory.mem_writeb(eaa, Memory.mem_readb(eaa)+switchBlock.r1.get8()+(Flags.get_CF()?1:0)); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.ADC_E8_32: eaa = ea32(switchBlock); Memory.mem_writeb(eaa, Instructions.ADCB(switchBlock.value, Memory.mem_readb(eaa))); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.ADC_E8_32_fast: eaa = ea32(switchBlock); Memory.mem_writeb(eaa, Memory.mem_readb(eaa)+switchBlock.r1.get8()+(Flags.get_CF()?1:0)); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.ADC_E8_32_sib: eaa = ea32sib(switchBlock); Memory.mem_writeb(eaa, Instructions.ADCB(switchBlock.value, Memory.mem_readb(eaa))); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.ADC_E8_32_sib_fast: eaa = ea32sib(switchBlock); Memory.mem_writeb(eaa, Memory.mem_readb(eaa)+switchBlock.r1.get8()+(Flags.get_CF()?1:0)); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.ADC_R8_R8: switchBlock.r1.set8(Instructions.ADCB(switchBlock.r2.get8(), switchBlock.r1.get8())); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.ADC_R8_R8_fast: switchBlock.r1.set8(switchBlock.r1.get8()+switchBlock.r2.get8()+(Flags.get_CF()?1:0)); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.ADC_E8_R8_16: eaa = ea16(switchBlock); Memory.mem_writeb(eaa, Instructions.ADCB(switchBlock.r1.get8(), Memory.mem_readb(eaa))); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.ADC_E8_R8_16_fast: eaa = ea16(switchBlock); Memory.mem_writeb(eaa, Memory.mem_readb(eaa)+switchBlock.r1.get8()+(Flags.get_CF()?1:0)); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.ADC_E8_R8_32: eaa = ea32(switchBlock); Memory.mem_writeb(eaa, Instructions.ADCB(switchBlock.r1.get8(), Memory.mem_readb(eaa))); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.ADC_E8_R8_32_fast:eaa = ea32(switchBlock); Memory.mem_writeb(eaa, Memory.mem_readb(eaa)+switchBlock.r1.get8()+(Flags.get_CF()?1:0)); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.ADC_E8_R8_32_sib: eaa = ea32sib(switchBlock); Memory.mem_writeb(eaa, Instructions.ADCB(switchBlock.r1.get8(), Memory.mem_readb(eaa))); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.ADC_E8_R8_32_sib_fast:eaa = ea32sib(switchBlock); Memory.mem_writeb(eaa, Memory.mem_readb(eaa)+switchBlock.r1.get8()+(Flags.get_CF()?1:0)); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.ADC_R8_E8_16: switchBlock.r1.set8(Instructions.ADCB(Memory.mem_readb(ea16(switchBlock)), switchBlock.r1.get8())); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.ADC_R8_E8_16_fast: switchBlock.r1.set8(switchBlock.r1.get8()+Memory.mem_readb(ea16(switchBlock))+(Flags.get_CF()?1:0)); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.ADC_R8_E8_32: switchBlock.r1.set8(Instructions.ADCB(Memory.mem_readb(ea32(switchBlock)), switchBlock.r1.get8())); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.ADC_R8_E8_32_fast: switchBlock.r1.set8(switchBlock.r1.get8()+Memory.mem_readb(ea32(switchBlock))+(Flags.get_CF()?1:0)); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.ADC_R8_E8_32_sib: switchBlock.r1.set8(Instructions.ADCB(Memory.mem_readb(ea32sib(switchBlock)), switchBlock.r1.get8())); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.ADC_R8_E8_32_sib_fast: switchBlock.r1.set8(switchBlock.r1.get8()+Memory.mem_readb(ea32sib(switchBlock))+(Flags.get_CF()?1:0)); reg_eip+=switchBlock.eipCount;continue;

                            case Inst.ADC_R16: switchBlock.r1.word(Instructions.ADCW(switchBlock.value, switchBlock.r1.word())); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.ADC_R16_fast: switchBlock.r1.word(switchBlock.r1.word()+switchBlock.value+(Flags.get_CF()?1:0)); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.ADC_E16_16: eaa = ea16(switchBlock); Memory.mem_writew(eaa, Instructions.ADCW(switchBlock.value, Memory.mem_readw(eaa))); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.ADC_E16_16_fast: eaa = ea16(switchBlock); Memory.mem_writew(eaa, Memory.mem_readw(eaa)+switchBlock.r1.word()+(Flags.get_CF()?1:0)); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.ADC_E16_32: eaa = ea32(switchBlock); Memory.mem_writew(eaa, Instructions.ADCW(switchBlock.value, Memory.mem_readw(eaa))); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.ADC_E16_32_fast: eaa = ea32(switchBlock); Memory.mem_writew(eaa, Memory.mem_readw(eaa)+switchBlock.r1.word()+(Flags.get_CF()?1:0)); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.ADC_E16_32_sib: eaa = ea32sib(switchBlock); Memory.mem_writew(eaa, Instructions.ADCW(switchBlock.value, Memory.mem_readw(eaa))); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.ADC_E16_32_sib_fast: eaa = ea32sib(switchBlock); Memory.mem_writew(eaa, Memory.mem_readw(eaa)+switchBlock.r1.word()+(Flags.get_CF()?1:0)); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.ADC_R16_R16: switchBlock.r1.word(Instructions.ADCW(switchBlock.r2.word(), switchBlock.r1.word())); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.ADC_R16_R16_fast: switchBlock.r1.word(switchBlock.r1.word()+switchBlock.r2.word()+(Flags.get_CF()?1:0)); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.ADC_E16_R16_16: eaa = ea16(switchBlock); Memory.mem_writew(eaa, Instructions.ADCW(switchBlock.r1.word(), Memory.mem_readw(eaa))); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.ADC_E16_R16_16_fast: eaa = ea16(switchBlock); Memory.mem_writew(eaa, Memory.mem_readw(eaa)+switchBlock.r1.word()+(Flags.get_CF()?1:0)); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.ADC_E16_R16_32: eaa = ea32(switchBlock); Memory.mem_writew(eaa, Instructions.ADCW(switchBlock.r1.word(), Memory.mem_readw(eaa))); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.ADC_E16_R16_32_fast:eaa = ea32(switchBlock); Memory.mem_writew(eaa, Memory.mem_readw(eaa)+switchBlock.r1.word()+(Flags.get_CF()?1:0)); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.ADC_E16_R16_32_sib: eaa = ea32sib(switchBlock); Memory.mem_writew(eaa, Instructions.ADCW(switchBlock.r1.word(), Memory.mem_readw(eaa))); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.ADC_E16_R16_32_sib_fast:eaa = ea32sib(switchBlock); Memory.mem_writew(eaa, Memory.mem_readw(eaa)+switchBlock.r1.word()+(Flags.get_CF()?1:0)); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.ADC_R16_E16_16: switchBlock.r1.word(Instructions.ADCW(Memory.mem_readw(ea16(switchBlock)), switchBlock.r1.word())); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.ADC_R16_E16_16_fast: switchBlock.r1.word(switchBlock.r1.word()+Memory.mem_readw(ea16(switchBlock))+(Flags.get_CF()?1:0)); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.ADC_R16_E16_32: switchBlock.r1.word(Instructions.ADCW(Memory.mem_readw(ea32(switchBlock)), switchBlock.r1.word())); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.ADC_R16_E16_32_fast: switchBlock.r1.word(switchBlock.r1.word()+Memory.mem_readw(ea32(switchBlock))+(Flags.get_CF()?1:0)); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.ADC_R16_E16_32_sib: switchBlock.r1.word(Instructions.ADCW(Memory.mem_readw(ea32sib(switchBlock)), switchBlock.r1.word())); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.ADC_R16_E16_32_sib_fast: switchBlock.r1.word(switchBlock.r1.word()+Memory.mem_readw(ea32sib(switchBlock))+(Flags.get_CF()?1:0)); reg_eip+=switchBlock.eipCount;continue;

                            case Inst.ADC_R32: switchBlock.r1.dword=Instructions.ADCD(switchBlock.value, switchBlock.r1.dword); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.ADC_R32_fast: switchBlock.r1.dword=switchBlock.r1.dword+switchBlock.value+(Flags.get_CF()?1:0); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.ADC_E32_16: eaa = ea16(switchBlock); Memory.mem_writed(eaa, Instructions.ADCD(switchBlock.value, Memory.mem_readd(eaa))); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.ADC_E32_16_fast: eaa = ea16(switchBlock); Memory.mem_writed(eaa, Memory.mem_readd(eaa)+switchBlock.r1.dword+(Flags.get_CF()?1:0)); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.ADC_E32_32: eaa = ea32(switchBlock); Memory.mem_writed(eaa, Instructions.ADCD(switchBlock.value, Memory.mem_readd(eaa))); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.ADC_E32_32_fast: eaa = ea32(switchBlock); Memory.mem_writed(eaa, Memory.mem_readd(eaa)+switchBlock.r1.dword+(Flags.get_CF()?1:0)); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.ADC_E32_32_sib: eaa = ea32sib(switchBlock); Memory.mem_writed(eaa, Instructions.ADCD(switchBlock.value, Memory.mem_readd(eaa))); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.ADC_E32_32_sib_fast: eaa = ea32sib(switchBlock); Memory.mem_writed(eaa, Memory.mem_readd(eaa)+switchBlock.r1.dword+(Flags.get_CF()?1:0)); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.ADC_R32_R32: switchBlock.r1.dword=Instructions.ADCD(switchBlock.r2.dword, switchBlock.r1.dword); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.ADC_R32_R32_fast: switchBlock.r1.dword=switchBlock.r1.dword+switchBlock.r2.dword+(Flags.get_CF()?1:0); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.ADC_E32_R32_16: eaa = ea16(switchBlock); Memory.mem_writed(eaa, Instructions.ADCD(switchBlock.r1.dword, Memory.mem_readd(eaa))); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.ADC_E32_R32_16_fast: eaa = ea16(switchBlock); Memory.mem_writed(eaa, Memory.mem_readd(eaa)+switchBlock.r1.dword+(Flags.get_CF()?1:0)); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.ADC_E32_R32_32: eaa = ea32(switchBlock); Memory.mem_writed(eaa, Instructions.ADCD(switchBlock.r1.dword, Memory.mem_readd(eaa))); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.ADC_E32_R32_32_fast:eaa = ea32(switchBlock); Memory.mem_writed(eaa, Memory.mem_readd(eaa)+switchBlock.r1.dword+(Flags.get_CF()?1:0)); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.ADC_E32_R32_32_sib: eaa = ea32sib(switchBlock); Memory.mem_writed(eaa, Instructions.ADCD(switchBlock.r1.dword, Memory.mem_readd(eaa))); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.ADC_E32_R32_32_sib_fast:eaa = ea32sib(switchBlock); Memory.mem_writed(eaa, Memory.mem_readd(eaa)+switchBlock.r1.dword+(Flags.get_CF()?1:0)); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.ADC_R32_E32_16: switchBlock.r1.dword=Instructions.ADCD(Memory.mem_readd(ea16(switchBlock)), switchBlock.r1.dword); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.ADC_R32_E32_16_fast: switchBlock.r1.dword=switchBlock.r1.dword+Memory.mem_readd(ea16(switchBlock))+(Flags.get_CF()?1:0); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.ADC_R32_E32_32: switchBlock.r1.dword=Instructions.ADCD(Memory.mem_readd(ea32(switchBlock)), switchBlock.r1.dword); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.ADC_R32_E32_32_fast: switchBlock.r1.dword=switchBlock.r1.dword+Memory.mem_readd(ea32(switchBlock))+(Flags.get_CF()?1:0); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.ADC_R32_E32_32_sib: switchBlock.r1.dword=Instructions.ADCD(Memory.mem_readd(ea32sib(switchBlock)), switchBlock.r1.dword); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.ADC_R32_E32_32_sib_fast: switchBlock.r1.dword=switchBlock.r1.dword+Memory.mem_readd(ea32sib(switchBlock))+(Flags.get_CF()?1:0); reg_eip+=switchBlock.eipCount;continue;

                            case Inst.SBB_R8: switchBlock.r1.set8(Instructions.SBBB(switchBlock.value, switchBlock.r1.get8())); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.SBB_R8_fast: switchBlock.r1.set8(switchBlock.r1.get8()-switchBlock.value-(Flags.get_CF()?1:0)); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.SBB_E8_16: eaa = ea16(switchBlock); Memory.mem_writeb(eaa, Instructions.SBBB(switchBlock.value, Memory.mem_readb(eaa))); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.SBB_E8_16_fast: eaa = ea16(switchBlock); Memory.mem_writeb(eaa, Memory.mem_readb(eaa)-switchBlock.r1.get8()-(Flags.get_CF()?1:0)); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.SBB_E8_32: eaa = ea32(switchBlock); Memory.mem_writeb(eaa, Instructions.SBBB(switchBlock.value, Memory.mem_readb(eaa))); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.SBB_E8_32_fast: eaa = ea32(switchBlock); Memory.mem_writeb(eaa, Memory.mem_readb(eaa)-switchBlock.r1.get8()-(Flags.get_CF()?1:0)); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.SBB_E8_32_sib: eaa = ea32sib(switchBlock); Memory.mem_writeb(eaa, Instructions.SBBB(switchBlock.value, Memory.mem_readb(eaa))); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.SBB_E8_32_sib_fast: eaa = ea32sib(switchBlock); Memory.mem_writeb(eaa, Memory.mem_readb(eaa)-switchBlock.r1.get8()-(Flags.get_CF()?1:0)); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.SBB_R8_R8: switchBlock.r1.set8(Instructions.SBBB(switchBlock.r2.get8(), switchBlock.r1.get8())); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.SBB_R8_R8_fast: switchBlock.r1.set8(switchBlock.r1.get8()-switchBlock.r2.get8()-(Flags.get_CF()?1:0)); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.SBB_E8_R8_16: eaa = ea16(switchBlock); Memory.mem_writeb(eaa, Instructions.SBBB(switchBlock.r1.get8(), Memory.mem_readb(eaa))); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.SBB_E8_R8_16_fast: eaa = ea16(switchBlock); Memory.mem_writeb(eaa, Memory.mem_readb(eaa)-switchBlock.r1.get8()-(Flags.get_CF()?1:0)); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.SBB_E8_R8_32: eaa = ea32(switchBlock); Memory.mem_writeb(eaa, Instructions.SBBB(switchBlock.r1.get8(), Memory.mem_readb(eaa))); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.SBB_E8_R8_32_fast:eaa = ea32(switchBlock); Memory.mem_writeb(eaa, Memory.mem_readb(eaa)-switchBlock.r1.get8()-(Flags.get_CF()?1:0)); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.SBB_E8_R8_32_sib: eaa = ea32sib(switchBlock); Memory.mem_writeb(eaa, Instructions.SBBB(switchBlock.r1.get8(), Memory.mem_readb(eaa))); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.SBB_E8_R8_32_sib_fast:eaa = ea32sib(switchBlock); Memory.mem_writeb(eaa, Memory.mem_readb(eaa)-switchBlock.r1.get8()-(Flags.get_CF()?1:0)); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.SBB_R8_E8_16: switchBlock.r1.set8(Instructions.SBBB(Memory.mem_readb(ea16(switchBlock)), switchBlock.r1.get8())); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.SBB_R8_E8_16_fast: switchBlock.r1.set8(switchBlock.r1.get8()-Memory.mem_readb(ea16(switchBlock))-(Flags.get_CF()?1:0)); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.SBB_R8_E8_32: switchBlock.r1.set8(Instructions.SBBB(Memory.mem_readb(ea32(switchBlock)), switchBlock.r1.get8())); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.SBB_R8_E8_32_fast: switchBlock.r1.set8(switchBlock.r1.get8()-Memory.mem_readb(ea32(switchBlock))-(Flags.get_CF()?1:0)); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.SBB_R8_E8_32_sib: switchBlock.r1.set8(Instructions.SBBB(Memory.mem_readb(ea32sib(switchBlock)), switchBlock.r1.get8())); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.SBB_R8_E8_32_sib_fast: switchBlock.r1.set8(switchBlock.r1.get8()-Memory.mem_readb(ea32sib(switchBlock))-(Flags.get_CF()?1:0)); reg_eip+=switchBlock.eipCount;continue;

                            case Inst.SBB_R16: switchBlock.r1.word(Instructions.SBBW(switchBlock.value, switchBlock.r1.word())); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.SBB_R16_fast: switchBlock.r1.word(switchBlock.r1.word()-switchBlock.value-(Flags.get_CF()?1:0)); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.SBB_E16_16: eaa = ea16(switchBlock); Memory.mem_writew(eaa, Instructions.SBBW(switchBlock.value, Memory.mem_readw(eaa))); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.SBB_E16_16_fast: eaa = ea16(switchBlock); Memory.mem_writew(eaa, Memory.mem_readw(eaa)-switchBlock.r1.word()-(Flags.get_CF()?1:0)); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.SBB_E16_32: eaa = ea32(switchBlock); Memory.mem_writew(eaa, Instructions.SBBW(switchBlock.value, Memory.mem_readw(eaa))); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.SBB_E16_32_fast: eaa = ea32(switchBlock); Memory.mem_writew(eaa, Memory.mem_readw(eaa)-switchBlock.r1.word()-(Flags.get_CF()?1:0)); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.SBB_E16_32_sib: eaa = ea32sib(switchBlock); Memory.mem_writew(eaa, Instructions.SBBW(switchBlock.value, Memory.mem_readw(eaa))); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.SBB_E16_32_sib_fast: eaa = ea32sib(switchBlock); Memory.mem_writew(eaa, Memory.mem_readw(eaa)-switchBlock.r1.word()-(Flags.get_CF()?1:0)); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.SBB_R16_R16: switchBlock.r1.word(Instructions.SBBW(switchBlock.r2.word(), switchBlock.r1.word())); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.SBB_R16_R16_fast: switchBlock.r1.word(switchBlock.r1.word()-switchBlock.r2.word()-(Flags.get_CF()?1:0)); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.SBB_E16_R16_16: eaa = ea16(switchBlock); Memory.mem_writew(eaa, Instructions.SBBW(switchBlock.r1.word(), Memory.mem_readw(eaa))); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.SBB_E16_R16_16_fast: eaa = ea16(switchBlock); Memory.mem_writew(eaa, Memory.mem_readw(eaa)-switchBlock.r1.word()-(Flags.get_CF()?1:0)); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.SBB_E16_R16_32: eaa = ea32(switchBlock); Memory.mem_writew(eaa, Instructions.SBBW(switchBlock.r1.word(), Memory.mem_readw(eaa))); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.SBB_E16_R16_32_fast:eaa = ea32(switchBlock); Memory.mem_writew(eaa, Memory.mem_readw(eaa)-switchBlock.r1.word()-(Flags.get_CF()?1:0)); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.SBB_E16_R16_32_sib: eaa = ea32sib(switchBlock); Memory.mem_writew(eaa, Instructions.SBBW(switchBlock.r1.word(), Memory.mem_readw(eaa))); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.SBB_E16_R16_32_sib_fast:eaa = ea32sib(switchBlock); Memory.mem_writew(eaa, Memory.mem_readw(eaa)-switchBlock.r1.word()-(Flags.get_CF()?1:0)); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.SBB_R16_E16_16: switchBlock.r1.word(Instructions.SBBW(Memory.mem_readw(ea16(switchBlock)), switchBlock.r1.word())); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.SBB_R16_E16_16_fast: switchBlock.r1.word(switchBlock.r1.word()-Memory.mem_readw(ea16(switchBlock))-(Flags.get_CF()?1:0)); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.SBB_R16_E16_32: switchBlock.r1.word(Instructions.SBBW(Memory.mem_readw(ea32(switchBlock)), switchBlock.r1.word())); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.SBB_R16_E16_32_fast: switchBlock.r1.word(switchBlock.r1.word()-Memory.mem_readw(ea32(switchBlock))-(Flags.get_CF()?1:0)); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.SBB_R16_E16_32_sib: switchBlock.r1.word(Instructions.SBBW(Memory.mem_readw(ea32sib(switchBlock)), switchBlock.r1.word())); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.SBB_R16_E16_32_sib_fast: switchBlock.r1.word(switchBlock.r1.word()-Memory.mem_readw(ea32sib(switchBlock))-(Flags.get_CF()?1:0)); reg_eip+=switchBlock.eipCount;continue;

                            case Inst.SBB_R32: switchBlock.r1.dword=Instructions.SBBD(switchBlock.value, switchBlock.r1.dword); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.SBB_R32_fast: switchBlock.r1.dword=switchBlock.r1.dword-switchBlock.value-(Flags.get_CF()?1:0); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.SBB_E32_16: eaa = ea16(switchBlock); Memory.mem_writed(eaa, Instructions.SBBD(switchBlock.value, Memory.mem_readd(eaa))); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.SBB_E32_16_fast: eaa = ea16(switchBlock); Memory.mem_writed(eaa, Memory.mem_readd(eaa)-switchBlock.r1.dword-(Flags.get_CF()?1:0)); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.SBB_E32_32: eaa = ea32(switchBlock); Memory.mem_writed(eaa, Instructions.SBBD(switchBlock.value, Memory.mem_readd(eaa))); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.SBB_E32_32_fast: eaa = ea32(switchBlock); Memory.mem_writed(eaa, Memory.mem_readd(eaa)-switchBlock.r1.dword-(Flags.get_CF()?1:0)); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.SBB_E32_32_sib: eaa = ea32sib(switchBlock); Memory.mem_writed(eaa, Instructions.SBBD(switchBlock.value, Memory.mem_readd(eaa))); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.SBB_E32_32_sib_fast: eaa = ea32sib(switchBlock); Memory.mem_writed(eaa, Memory.mem_readd(eaa)-switchBlock.r1.dword-(Flags.get_CF()?1:0)); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.SBB_R32_R32: switchBlock.r1.dword=Instructions.SBBD(switchBlock.r2.dword, switchBlock.r1.dword); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.SBB_R32_R32_fast: switchBlock.r1.dword=switchBlock.r1.dword-switchBlock.r2.dword-(Flags.get_CF()?1:0); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.SBB_E32_R32_16: eaa = ea16(switchBlock); Memory.mem_writed(eaa, Instructions.SBBD(switchBlock.r1.dword, Memory.mem_readd(eaa))); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.SBB_E32_R32_16_fast: eaa = ea16(switchBlock); Memory.mem_writed(eaa, Memory.mem_readd(eaa)-switchBlock.r1.dword-(Flags.get_CF()?1:0)); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.SBB_E32_R32_32: eaa = ea32(switchBlock); Memory.mem_writed(eaa, Instructions.SBBD(switchBlock.r1.dword, Memory.mem_readd(eaa))); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.SBB_E32_R32_32_fast:eaa = ea32(switchBlock); Memory.mem_writed(eaa, Memory.mem_readd(eaa)-switchBlock.r1.dword-(Flags.get_CF()?1:0)); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.SBB_E32_R32_32_sib: eaa = ea32sib(switchBlock); Memory.mem_writed(eaa, Instructions.SBBD(switchBlock.r1.dword, Memory.mem_readd(eaa))); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.SBB_E32_R32_32_sib_fast:eaa = ea32sib(switchBlock); Memory.mem_writed(eaa, Memory.mem_readd(eaa)-switchBlock.r1.dword-(Flags.get_CF()?1:0)); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.SBB_R32_E32_16: switchBlock.r1.dword=Instructions.SBBD(Memory.mem_readd(ea16(switchBlock)), switchBlock.r1.dword); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.SBB_R32_E32_16_fast: switchBlock.r1.dword=switchBlock.r1.dword-Memory.mem_readd(ea16(switchBlock))-(Flags.get_CF()?1:0); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.SBB_R32_E32_32: switchBlock.r1.dword=Instructions.SBBD(Memory.mem_readd(ea32(switchBlock)), switchBlock.r1.dword); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.SBB_R32_E32_32_fast: switchBlock.r1.dword=switchBlock.r1.dword-Memory.mem_readd(ea32(switchBlock))-(Flags.get_CF()?1:0); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.SBB_R32_E32_32_sib: switchBlock.r1.dword=Instructions.SBBD(Memory.mem_readd(ea32sib(switchBlock)), switchBlock.r1.dword); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.SBB_R32_E32_32_sib_fast: switchBlock.r1.dword=switchBlock.r1.dword-Memory.mem_readd(ea32sib(switchBlock))-(Flags.get_CF()?1:0); reg_eip+=switchBlock.eipCount;continue;

                            case Inst.AND_R8: switchBlock.r1.set8(Instructions.ANDB(switchBlock.value, switchBlock.r1.get8())); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.AND_R8_fast: switchBlock.r1.set8(switchBlock.r1.get8() & switchBlock.value); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.AND_E8_16: eaa = ea16(switchBlock); Memory.mem_writeb(eaa, Instructions.ANDB(switchBlock.value, Memory.mem_readb(eaa))); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.AND_E8_16_fast: eaa = ea16(switchBlock); Memory.mem_writeb(eaa, Memory.mem_readb(eaa) & switchBlock.r1.get8()); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.AND_E8_32: eaa = ea32(switchBlock); Memory.mem_writeb(eaa, Instructions.ANDB(switchBlock.value, Memory.mem_readb(eaa))); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.AND_E8_32_fast: eaa = ea32(switchBlock); Memory.mem_writeb(eaa, Memory.mem_readb(eaa) & switchBlock.r1.get8()); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.AND_E8_32_sib: eaa = ea32sib(switchBlock); Memory.mem_writeb(eaa, Instructions.ANDB(switchBlock.value, Memory.mem_readb(eaa))); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.AND_E8_32_sib_fast: eaa = ea32sib(switchBlock); Memory.mem_writeb(eaa, Memory.mem_readb(eaa) & switchBlock.r1.get8()); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.AND_R8_R8: switchBlock.r1.set8(Instructions.ANDB(switchBlock.r2.get8(), switchBlock.r1.get8())); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.AND_R8_R8_fast: switchBlock.r1.set8(switchBlock.r1.get8() & switchBlock.r2.get8()); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.AND_E8_R8_16: eaa = ea16(switchBlock); Memory.mem_writeb(eaa, Instructions.ANDB(switchBlock.r1.get8(), Memory.mem_readb(eaa))); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.AND_E8_R8_16_fast: eaa = ea16(switchBlock); Memory.mem_writeb(eaa, Memory.mem_readb(eaa) & switchBlock.r1.get8()); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.AND_E8_R8_32: eaa = ea32(switchBlock); Memory.mem_writeb(eaa, Instructions.ANDB(switchBlock.r1.get8(), Memory.mem_readb(eaa))); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.AND_E8_R8_32_fast:eaa = ea32(switchBlock); Memory.mem_writeb(eaa, Memory.mem_readb(eaa) & switchBlock.r1.get8()); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.AND_E8_R8_32_sib: eaa = ea32sib(switchBlock); Memory.mem_writeb(eaa, Instructions.ANDB(switchBlock.r1.get8(), Memory.mem_readb(eaa))); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.AND_E8_R8_32_sib_fast:eaa = ea32sib(switchBlock); Memory.mem_writeb(eaa, Memory.mem_readb(eaa) & switchBlock.r1.get8()); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.AND_R8_E8_16: switchBlock.r1.set8(Instructions.ANDB(Memory.mem_readb(ea16(switchBlock)), switchBlock.r1.get8())); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.AND_R8_E8_16_fast: switchBlock.r1.set8(switchBlock.r1.get8() & Memory.mem_readb(ea16(switchBlock))); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.AND_R8_E8_32: switchBlock.r1.set8(Instructions.ANDB(Memory.mem_readb(ea32(switchBlock)), switchBlock.r1.get8())); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.AND_R8_E8_32_fast: switchBlock.r1.set8(switchBlock.r1.get8() & Memory.mem_readb(ea32(switchBlock))); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.AND_R8_E8_32_sib: switchBlock.r1.set8(Instructions.ANDB(Memory.mem_readb(ea32sib(switchBlock)), switchBlock.r1.get8())); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.AND_R8_E8_32_sib_fast: switchBlock.r1.set8(switchBlock.r1.get8() & Memory.mem_readb(ea32sib(switchBlock))); reg_eip+=switchBlock.eipCount;continue;

                            case Inst.AND_R16: switchBlock.r1.word(Instructions.ANDW(switchBlock.value, switchBlock.r1.word())); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.AND_R16_fast: switchBlock.r1.word(switchBlock.r1.word() & switchBlock.value); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.AND_E16_16: eaa = ea16(switchBlock); Memory.mem_writew(eaa, Instructions.ANDW(switchBlock.value, Memory.mem_readw(eaa))); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.AND_E16_16_fast: eaa = ea16(switchBlock); Memory.mem_writew(eaa, Memory.mem_readw(eaa) & switchBlock.r1.word()); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.AND_E16_32: eaa = ea32(switchBlock); Memory.mem_writew(eaa, Instructions.ANDW(switchBlock.value, Memory.mem_readw(eaa))); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.AND_E16_32_fast: eaa = ea32(switchBlock); Memory.mem_writew(eaa, Memory.mem_readw(eaa) & switchBlock.r1.word()); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.AND_E16_32_sib: eaa = ea32sib(switchBlock); Memory.mem_writew(eaa, Instructions.ANDW(switchBlock.value, Memory.mem_readw(eaa))); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.AND_E16_32_sib_fast: eaa = ea32sib(switchBlock); Memory.mem_writew(eaa, Memory.mem_readw(eaa) & switchBlock.r1.word()); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.AND_R16_R16: switchBlock.r1.word(Instructions.ANDW(switchBlock.r2.word(), switchBlock.r1.word())); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.AND_R16_R16_fast: switchBlock.r1.word(switchBlock.r1.word() & switchBlock.r2.word()); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.AND_E16_R16_16: eaa = ea16(switchBlock); Memory.mem_writew(eaa, Instructions.ANDW(switchBlock.r1.word(), Memory.mem_readw(eaa))); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.AND_E16_R16_16_fast: eaa = ea16(switchBlock); Memory.mem_writew(eaa, Memory.mem_readw(eaa) & switchBlock.r1.word()); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.AND_E16_R16_32: eaa = ea32(switchBlock); Memory.mem_writew(eaa, Instructions.ANDW(switchBlock.r1.word(), Memory.mem_readw(eaa))); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.AND_E16_R16_32_fast:eaa = ea32(switchBlock); Memory.mem_writew(eaa, Memory.mem_readw(eaa) & switchBlock.r1.word()); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.AND_E16_R16_32_sib: eaa = ea32sib(switchBlock); Memory.mem_writew(eaa, Instructions.ANDW(switchBlock.r1.word(), Memory.mem_readw(eaa))); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.AND_E16_R16_32_sib_fast:eaa = ea32sib(switchBlock); Memory.mem_writew(eaa, Memory.mem_readw(eaa) & switchBlock.r1.word()); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.AND_R16_E16_16: switchBlock.r1.word(Instructions.ANDW(Memory.mem_readw(ea16(switchBlock)), switchBlock.r1.word())); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.AND_R16_E16_16_fast: switchBlock.r1.word(switchBlock.r1.word() & Memory.mem_readw(ea16(switchBlock))); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.AND_R16_E16_32: switchBlock.r1.word(Instructions.ANDW(Memory.mem_readw(ea32(switchBlock)), switchBlock.r1.word())); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.AND_R16_E16_32_fast: switchBlock.r1.word(switchBlock.r1.word() & Memory.mem_readw(ea32(switchBlock))); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.AND_R16_E16_32_sib: switchBlock.r1.word(Instructions.ANDW(Memory.mem_readw(ea32sib(switchBlock)), switchBlock.r1.word())); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.AND_R16_E16_32_sib_fast: switchBlock.r1.word(switchBlock.r1.word() & Memory.mem_readw(ea32sib(switchBlock))); reg_eip+=switchBlock.eipCount;continue;

                            case Inst.AND_R32: switchBlock.r1.dword=Instructions.ANDD(switchBlock.value, switchBlock.r1.dword); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.AND_R32_fast: switchBlock.r1.dword=switchBlock.r1.dword & switchBlock.value; reg_eip+=switchBlock.eipCount;continue;
                            case Inst.AND_E32_16: eaa = ea16(switchBlock); Memory.mem_writed(eaa, Instructions.ANDD(switchBlock.value, Memory.mem_readd(eaa))); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.AND_E32_16_fast: eaa = ea16(switchBlock); Memory.mem_writed(eaa, Memory.mem_readd(eaa) & switchBlock.r1.dword); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.AND_E32_32: eaa = ea32(switchBlock); Memory.mem_writed(eaa, Instructions.ANDD(switchBlock.value, Memory.mem_readd(eaa))); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.AND_E32_32_fast: eaa = ea32(switchBlock); Memory.mem_writed(eaa, Memory.mem_readd(eaa) & switchBlock.r1.dword); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.AND_E32_32_sib: eaa = ea32sib(switchBlock); Memory.mem_writed(eaa, Instructions.ANDD(switchBlock.value, Memory.mem_readd(eaa))); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.AND_E32_32_sib_fast: eaa = ea32sib(switchBlock); Memory.mem_writed(eaa, Memory.mem_readd(eaa) & switchBlock.r1.dword); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.AND_R32_R32: switchBlock.r1.dword=Instructions.ANDD(switchBlock.r2.dword, switchBlock.r1.dword); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.AND_R32_R32_fast: switchBlock.r1.dword=switchBlock.r1.dword & switchBlock.r2.dword; reg_eip+=switchBlock.eipCount;continue;
                            case Inst.AND_E32_R32_16: eaa = ea16(switchBlock); Memory.mem_writed(eaa, Instructions.ANDD(switchBlock.r1.dword, Memory.mem_readd(eaa))); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.AND_E32_R32_16_fast: eaa = ea16(switchBlock); Memory.mem_writed(eaa, Memory.mem_readd(eaa) & switchBlock.r1.dword); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.AND_E32_R32_32: eaa = ea32(switchBlock); Memory.mem_writed(eaa, Instructions.ANDD(switchBlock.r1.dword, Memory.mem_readd(eaa))); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.AND_E32_R32_32_fast:eaa = ea32(switchBlock); Memory.mem_writed(eaa, Memory.mem_readd(eaa) & switchBlock.r1.dword); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.AND_E32_R32_32_sib: eaa = ea32sib(switchBlock); Memory.mem_writed(eaa, Instructions.ANDD(switchBlock.r1.dword, Memory.mem_readd(eaa))); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.AND_E32_R32_32_sib_fast:eaa = ea32sib(switchBlock); Memory.mem_writed(eaa, Memory.mem_readd(eaa) & switchBlock.r1.dword); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.AND_R32_E32_16: switchBlock.r1.dword=Instructions.ANDD(Memory.mem_readd(ea16(switchBlock)), switchBlock.r1.dword); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.AND_R32_E32_16_fast: switchBlock.r1.dword=switchBlock.r1.dword & Memory.mem_readd(ea16(switchBlock)); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.AND_R32_E32_32: switchBlock.r1.dword=Instructions.ANDD(Memory.mem_readd(ea32(switchBlock)), switchBlock.r1.dword); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.AND_R32_E32_32_fast: switchBlock.r1.dword=switchBlock.r1.dword & Memory.mem_readd(ea32(switchBlock)); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.AND_R32_E32_32_sib: switchBlock.r1.dword=Instructions.ANDD(Memory.mem_readd(ea32sib(switchBlock)), switchBlock.r1.dword); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.AND_R32_E32_32_sib_fast: switchBlock.r1.dword=switchBlock.r1.dword & Memory.mem_readd(ea32sib(switchBlock)); reg_eip+=switchBlock.eipCount;continue;

                            case Inst.SUB_R8: switchBlock.r1.set8(Instructions.SUBB(switchBlock.value, switchBlock.r1.get8())); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.SUB_R8_fast: switchBlock.r1.set8(switchBlock.r1.get8() - switchBlock.value); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.SUB_E8_16: eaa = ea16(switchBlock); Memory.mem_writeb(eaa, Instructions.SUBB(switchBlock.value, Memory.mem_readb(eaa))); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.SUB_E8_16_fast: eaa = ea16(switchBlock); Memory.mem_writeb(eaa, Memory.mem_readb(eaa) - switchBlock.r1.get8()); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.SUB_E8_32: eaa = ea32(switchBlock); Memory.mem_writeb(eaa, Instructions.SUBB(switchBlock.value, Memory.mem_readb(eaa))); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.SUB_E8_32_fast: eaa = ea32(switchBlock); Memory.mem_writeb(eaa, Memory.mem_readb(eaa) - switchBlock.r1.get8()); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.SUB_E8_32_sib: eaa = ea32sib(switchBlock); Memory.mem_writeb(eaa, Instructions.SUBB(switchBlock.value, Memory.mem_readb(eaa))); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.SUB_E8_32_sib_fast: eaa = ea32sib(switchBlock); Memory.mem_writeb(eaa, Memory.mem_readb(eaa) - switchBlock.r1.get8()); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.SUB_R8_R8: switchBlock.r1.set8(Instructions.SUBB(switchBlock.r2.get8(), switchBlock.r1.get8())); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.SUB_R8_R8_fast: switchBlock.r1.set8(switchBlock.r1.get8() - switchBlock.r2.get8()); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.SUB_E8_R8_16: eaa = ea16(switchBlock); Memory.mem_writeb(eaa, Instructions.SUBB(switchBlock.r1.get8(), Memory.mem_readb(eaa))); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.SUB_E8_R8_16_fast: eaa = ea16(switchBlock); Memory.mem_writeb(eaa, Memory.mem_readb(eaa) - switchBlock.r1.get8()); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.SUB_E8_R8_32: eaa = ea32(switchBlock); Memory.mem_writeb(eaa, Instructions.SUBB(switchBlock.r1.get8(), Memory.mem_readb(eaa))); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.SUB_E8_R8_32_fast:eaa = ea32(switchBlock); Memory.mem_writeb(eaa, Memory.mem_readb(eaa) - switchBlock.r1.get8()); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.SUB_E8_R8_32_sib: eaa = ea32sib(switchBlock); Memory.mem_writeb(eaa, Instructions.SUBB(switchBlock.r1.get8(), Memory.mem_readb(eaa))); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.SUB_E8_R8_32_sib_fast:eaa = ea32sib(switchBlock); Memory.mem_writeb(eaa, Memory.mem_readb(eaa) - switchBlock.r1.get8()); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.SUB_R8_E8_16: switchBlock.r1.set8(Instructions.SUBB(Memory.mem_readb(ea16(switchBlock)), switchBlock.r1.get8())); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.SUB_R8_E8_16_fast: switchBlock.r1.set8(switchBlock.r1.get8() - Memory.mem_readb(ea16(switchBlock))); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.SUB_R8_E8_32: switchBlock.r1.set8(Instructions.SUBB(Memory.mem_readb(ea32(switchBlock)), switchBlock.r1.get8())); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.SUB_R8_E8_32_fast: switchBlock.r1.set8(switchBlock.r1.get8() - Memory.mem_readb(ea32(switchBlock))); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.SUB_R8_E8_32_sib: switchBlock.r1.set8(Instructions.SUBB(Memory.mem_readb(ea32sib(switchBlock)), switchBlock.r1.get8())); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.SUB_R8_E8_32_sib_fast: switchBlock.r1.set8(switchBlock.r1.get8() - Memory.mem_readb(ea32sib(switchBlock))); reg_eip+=switchBlock.eipCount;continue;

                            case Inst.SUB_R16: switchBlock.r1.word(Instructions.SUBW(switchBlock.value, switchBlock.r1.word())); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.SUB_R16_fast: switchBlock.r1.word(switchBlock.r1.word() - switchBlock.value); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.SUB_E16_16: eaa = ea16(switchBlock); Memory.mem_writew(eaa, Instructions.SUBW(switchBlock.value, Memory.mem_readw(eaa))); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.SUB_E16_16_fast: eaa = ea16(switchBlock); Memory.mem_writew(eaa, Memory.mem_readw(eaa) - switchBlock.r1.word()); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.SUB_E16_32: eaa = ea32(switchBlock); Memory.mem_writew(eaa, Instructions.SUBW(switchBlock.value, Memory.mem_readw(eaa))); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.SUB_E16_32_fast: eaa = ea32(switchBlock); Memory.mem_writew(eaa, Memory.mem_readw(eaa) - switchBlock.r1.word()); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.SUB_E16_32_sib: eaa = ea32sib(switchBlock); Memory.mem_writew(eaa, Instructions.SUBW(switchBlock.value, Memory.mem_readw(eaa))); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.SUB_E16_32_sib_fast: eaa = ea32sib(switchBlock); Memory.mem_writew(eaa, Memory.mem_readw(eaa) - switchBlock.r1.word()); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.SUB_R16_R16: switchBlock.r1.word(Instructions.SUBW(switchBlock.r2.word(), switchBlock.r1.word())); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.SUB_R16_R16_fast: switchBlock.r1.word(switchBlock.r1.word() - switchBlock.r2.word()); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.SUB_E16_R16_16: eaa = ea16(switchBlock); Memory.mem_writew(eaa, Instructions.SUBW(switchBlock.r1.word(), Memory.mem_readw(eaa))); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.SUB_E16_R16_16_fast: eaa = ea16(switchBlock); Memory.mem_writew(eaa, Memory.mem_readw(eaa) - switchBlock.r1.word()); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.SUB_E16_R16_32: eaa = ea32(switchBlock); Memory.mem_writew(eaa, Instructions.SUBW(switchBlock.r1.word(), Memory.mem_readw(eaa))); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.SUB_E16_R16_32_fast:eaa = ea32(switchBlock); Memory.mem_writew(eaa, Memory.mem_readw(eaa) - switchBlock.r1.word()); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.SUB_E16_R16_32_sib: eaa = ea32sib(switchBlock); Memory.mem_writew(eaa, Instructions.SUBW(switchBlock.r1.word(), Memory.mem_readw(eaa))); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.SUB_E16_R16_32_sib_fast:eaa = ea32sib(switchBlock); Memory.mem_writew(eaa, Memory.mem_readw(eaa) - switchBlock.r1.word()); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.SUB_R16_E16_16: switchBlock.r1.word(Instructions.SUBW(Memory.mem_readw(ea16(switchBlock)), switchBlock.r1.word())); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.SUB_R16_E16_16_fast: switchBlock.r1.word(switchBlock.r1.word() - Memory.mem_readw(ea16(switchBlock))); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.SUB_R16_E16_32: switchBlock.r1.word(Instructions.SUBW(Memory.mem_readw(ea32(switchBlock)), switchBlock.r1.word())); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.SUB_R16_E16_32_fast: switchBlock.r1.word(switchBlock.r1.word() - Memory.mem_readw(ea32(switchBlock))); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.SUB_R16_E16_32_sib: switchBlock.r1.word(Instructions.SUBW(Memory.mem_readw(ea32sib(switchBlock)), switchBlock.r1.word())); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.SUB_R16_E16_32_sib_fast: switchBlock.r1.word(switchBlock.r1.word() - Memory.mem_readw(ea32sib(switchBlock))); reg_eip+=switchBlock.eipCount;continue;

                            case Inst.SUB_R32: switchBlock.r1.dword=Instructions.SUBD(switchBlock.value, switchBlock.r1.dword); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.SUB_R32_fast: switchBlock.r1.dword=switchBlock.r1.dword - switchBlock.value; reg_eip+=switchBlock.eipCount;continue;
                            case Inst.SUB_E32_16: eaa = ea16(switchBlock); Memory.mem_writed(eaa, Instructions.SUBD(switchBlock.value, Memory.mem_readd(eaa))); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.SUB_E32_16_fast: eaa = ea16(switchBlock); Memory.mem_writed(eaa, Memory.mem_readd(eaa) - switchBlock.r1.dword); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.SUB_E32_32: eaa = ea32(switchBlock); Memory.mem_writed(eaa, Instructions.SUBD(switchBlock.value, Memory.mem_readd(eaa))); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.SUB_E32_32_fast: eaa = ea32(switchBlock); Memory.mem_writed(eaa, Memory.mem_readd(eaa) - switchBlock.r1.dword); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.SUB_E32_32_sib: eaa = ea32sib(switchBlock); Memory.mem_writed(eaa, Instructions.SUBD(switchBlock.value, Memory.mem_readd(eaa))); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.SUB_E32_32_sib_fast: eaa = ea32sib(switchBlock); Memory.mem_writed(eaa, Memory.mem_readd(eaa) - switchBlock.r1.dword); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.SUB_R32_R32: switchBlock.r1.dword=Instructions.SUBD(switchBlock.r2.dword, switchBlock.r1.dword); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.SUB_R32_R32_fast: switchBlock.r1.dword=switchBlock.r1.dword - switchBlock.r2.dword; reg_eip+=switchBlock.eipCount;continue;
                            case Inst.SUB_E32_R32_16: eaa = ea16(switchBlock); Memory.mem_writed(eaa, Instructions.SUBD(switchBlock.r1.dword, Memory.mem_readd(eaa))); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.SUB_E32_R32_16_fast: eaa = ea16(switchBlock); Memory.mem_writed(eaa, Memory.mem_readd(eaa) - switchBlock.r1.dword); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.SUB_E32_R32_32: eaa = ea32(switchBlock); Memory.mem_writed(eaa, Instructions.SUBD(switchBlock.r1.dword, Memory.mem_readd(eaa))); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.SUB_E32_R32_32_fast:eaa = ea32(switchBlock); Memory.mem_writed(eaa, Memory.mem_readd(eaa) - switchBlock.r1.dword); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.SUB_E32_R32_32_sib: eaa = ea32sib(switchBlock); Memory.mem_writed(eaa, Instructions.SUBD(switchBlock.r1.dword, Memory.mem_readd(eaa))); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.SUB_E32_R32_32_sib_fast:eaa = ea32sib(switchBlock); Memory.mem_writed(eaa, Memory.mem_readd(eaa) - switchBlock.r1.dword); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.SUB_R32_E32_16: switchBlock.r1.dword=Instructions.SUBD(Memory.mem_readd(ea16(switchBlock)), switchBlock.r1.dword); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.SUB_R32_E32_16_fast: switchBlock.r1.dword=switchBlock.r1.dword - Memory.mem_readd(ea16(switchBlock)); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.SUB_R32_E32_32: switchBlock.r1.dword=Instructions.SUBD(Memory.mem_readd(ea32(switchBlock)), switchBlock.r1.dword); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.SUB_R32_E32_32_fast: switchBlock.r1.dword=switchBlock.r1.dword - Memory.mem_readd(ea32(switchBlock)); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.SUB_R32_E32_32_sib: switchBlock.r1.dword=Instructions.SUBD(Memory.mem_readd(ea32sib(switchBlock)), switchBlock.r1.dword); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.SUB_R32_E32_32_sib_fast: switchBlock.r1.dword=switchBlock.r1.dword - Memory.mem_readd(ea32sib(switchBlock)); reg_eip+=switchBlock.eipCount;continue;

                            case Inst.XOR_R8: switchBlock.r1.set8(Instructions.XORB(switchBlock.value, switchBlock.r1.get8())); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.XOR_R8_fast: switchBlock.r1.set8(switchBlock.r1.get8() ^ switchBlock.value); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.XOR_E8_16: eaa = ea16(switchBlock); Memory.mem_writeb(eaa, Instructions.XORB(switchBlock.value, Memory.mem_readb(eaa))); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.XOR_E8_16_fast: eaa = ea16(switchBlock); Memory.mem_writeb(eaa, Memory.mem_readb(eaa) ^ switchBlock.r1.get8()); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.XOR_E8_32: eaa = ea32(switchBlock); Memory.mem_writeb(eaa, Instructions.XORB(switchBlock.value, Memory.mem_readb(eaa))); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.XOR_E8_32_fast: eaa = ea32(switchBlock); Memory.mem_writeb(eaa, Memory.mem_readb(eaa) ^ switchBlock.r1.get8()); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.XOR_E8_32_sib: eaa = ea32sib(switchBlock); Memory.mem_writeb(eaa, Instructions.XORB(switchBlock.value, Memory.mem_readb(eaa))); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.XOR_E8_32_sib_fast: eaa = ea32sib(switchBlock); Memory.mem_writeb(eaa, Memory.mem_readb(eaa) ^ switchBlock.r1.get8()); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.XOR_R8_R8: switchBlock.r1.set8(Instructions.XORB(switchBlock.r2.get8(), switchBlock.r1.get8())); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.XOR_R8_R8_fast: switchBlock.r1.set8(switchBlock.r1.get8() ^ switchBlock.r2.get8()); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.XOR_E8_R8_16: eaa = ea16(switchBlock); Memory.mem_writeb(eaa, Instructions.XORB(switchBlock.r1.get8(), Memory.mem_readb(eaa))); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.XOR_E8_R8_16_fast: eaa = ea16(switchBlock); Memory.mem_writeb(eaa, Memory.mem_readb(eaa) ^ switchBlock.r1.get8()); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.XOR_E8_R8_32: eaa = ea32(switchBlock); Memory.mem_writeb(eaa, Instructions.XORB(switchBlock.r1.get8(), Memory.mem_readb(eaa))); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.XOR_E8_R8_32_fast:eaa = ea32(switchBlock); Memory.mem_writeb(eaa, Memory.mem_readb(eaa) ^ switchBlock.r1.get8()); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.XOR_E8_R8_32_sib: eaa = ea32sib(switchBlock); Memory.mem_writeb(eaa, Instructions.XORB(switchBlock.r1.get8(), Memory.mem_readb(eaa))); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.XOR_E8_R8_32_sib_fast:eaa = ea32sib(switchBlock); Memory.mem_writeb(eaa, Memory.mem_readb(eaa) ^ switchBlock.r1.get8()); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.XOR_R8_E8_16: switchBlock.r1.set8(Instructions.XORB(Memory.mem_readb(ea16(switchBlock)), switchBlock.r1.get8())); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.XOR_R8_E8_16_fast: switchBlock.r1.set8(switchBlock.r1.get8() ^ Memory.mem_readb(ea16(switchBlock))); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.XOR_R8_E8_32: switchBlock.r1.set8(Instructions.XORB(Memory.mem_readb(ea32(switchBlock)), switchBlock.r1.get8())); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.XOR_R8_E8_32_fast: switchBlock.r1.set8(switchBlock.r1.get8() ^ Memory.mem_readb(ea32(switchBlock))); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.XOR_R8_E8_32_sib: switchBlock.r1.set8(Instructions.XORB(Memory.mem_readb(ea32sib(switchBlock)), switchBlock.r1.get8())); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.XOR_R8_E8_32_sib_fast: switchBlock.r1.set8(switchBlock.r1.get8() ^ Memory.mem_readb(ea32sib(switchBlock))); reg_eip+=switchBlock.eipCount;continue;

                            case Inst.XOR_R16: switchBlock.r1.word(Instructions.XORW(switchBlock.value, switchBlock.r1.word())); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.XOR_R16_fast: switchBlock.r1.word(switchBlock.r1.word() ^ switchBlock.value); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.XOR_E16_16: eaa = ea16(switchBlock); Memory.mem_writew(eaa, Instructions.XORW(switchBlock.value, Memory.mem_readw(eaa))); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.XOR_E16_16_fast: eaa = ea16(switchBlock); Memory.mem_writew(eaa, Memory.mem_readw(eaa) ^ switchBlock.r1.word()); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.XOR_E16_32: eaa = ea32(switchBlock); Memory.mem_writew(eaa, Instructions.XORW(switchBlock.value, Memory.mem_readw(eaa))); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.XOR_E16_32_fast: eaa = ea32(switchBlock); Memory.mem_writew(eaa, Memory.mem_readw(eaa) ^ switchBlock.r1.word()); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.XOR_E16_32_sib: eaa = ea32sib(switchBlock); Memory.mem_writew(eaa, Instructions.XORW(switchBlock.value, Memory.mem_readw(eaa))); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.XOR_E16_32_sib_fast: eaa = ea32sib(switchBlock); Memory.mem_writew(eaa, Memory.mem_readw(eaa) ^ switchBlock.r1.word()); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.XOR_R16_R16: switchBlock.r1.word(Instructions.XORW(switchBlock.r2.word(), switchBlock.r1.word())); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.XOR_R16_R16_fast: switchBlock.r1.word(switchBlock.r1.word() ^ switchBlock.r2.word()); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.XOR_E16_R16_16: eaa = ea16(switchBlock); Memory.mem_writew(eaa, Instructions.XORW(switchBlock.r1.word(), Memory.mem_readw(eaa))); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.XOR_E16_R16_16_fast: eaa = ea16(switchBlock); Memory.mem_writew(eaa, Memory.mem_readw(eaa) ^ switchBlock.r1.word()); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.XOR_E16_R16_32: eaa = ea32(switchBlock); Memory.mem_writew(eaa, Instructions.XORW(switchBlock.r1.word(), Memory.mem_readw(eaa))); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.XOR_E16_R16_32_fast:eaa = ea32(switchBlock); Memory.mem_writew(eaa, Memory.mem_readw(eaa) ^ switchBlock.r1.word()); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.XOR_E16_R16_32_sib: eaa = ea32sib(switchBlock); Memory.mem_writew(eaa, Instructions.XORW(switchBlock.r1.word(), Memory.mem_readw(eaa))); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.XOR_E16_R16_32_sib_fast:eaa = ea32sib(switchBlock); Memory.mem_writew(eaa, Memory.mem_readw(eaa) ^ switchBlock.r1.word()); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.XOR_R16_E16_16: switchBlock.r1.word(Instructions.XORW(Memory.mem_readw(ea16(switchBlock)), switchBlock.r1.word())); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.XOR_R16_E16_16_fast: switchBlock.r1.word(switchBlock.r1.word() ^ Memory.mem_readw(ea16(switchBlock))); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.XOR_R16_E16_32: switchBlock.r1.word(Instructions.XORW(Memory.mem_readw(ea32(switchBlock)), switchBlock.r1.word())); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.XOR_R16_E16_32_fast: switchBlock.r1.word(switchBlock.r1.word() ^ Memory.mem_readw(ea32(switchBlock))); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.XOR_R16_E16_32_sib: switchBlock.r1.word(Instructions.XORW(Memory.mem_readw(ea32sib(switchBlock)), switchBlock.r1.word())); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.XOR_R16_E16_32_sib_fast: switchBlock.r1.word(switchBlock.r1.word() ^ Memory.mem_readw(ea32sib(switchBlock))); reg_eip+=switchBlock.eipCount;continue;

                            case Inst.XOR_R32: switchBlock.r1.dword=Instructions.XORD(switchBlock.value, switchBlock.r1.dword); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.XOR_R32_fast: switchBlock.r1.dword=switchBlock.r1.dword ^ switchBlock.value; reg_eip+=switchBlock.eipCount;continue;
                            case Inst.XOR_E32_16: eaa = ea16(switchBlock); Memory.mem_writed(eaa, Instructions.XORD(switchBlock.value, Memory.mem_readd(eaa))); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.XOR_E32_16_fast: eaa = ea16(switchBlock); Memory.mem_writed(eaa, Memory.mem_readd(eaa) ^ switchBlock.r1.dword); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.XOR_E32_32: eaa = ea32(switchBlock); Memory.mem_writed(eaa, Instructions.XORD(switchBlock.value, Memory.mem_readd(eaa))); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.XOR_E32_32_fast: eaa = ea32(switchBlock); Memory.mem_writed(eaa, Memory.mem_readd(eaa) ^ switchBlock.r1.dword); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.XOR_E32_32_sib: eaa = ea32sib(switchBlock); Memory.mem_writed(eaa, Instructions.XORD(switchBlock.value, Memory.mem_readd(eaa))); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.XOR_E32_32_sib_fast: eaa = ea32sib(switchBlock); Memory.mem_writed(eaa, Memory.mem_readd(eaa) ^ switchBlock.r1.dword); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.XOR_R32_R32: switchBlock.r1.dword=Instructions.XORD(switchBlock.r2.dword, switchBlock.r1.dword); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.XOR_R32_R32_fast: switchBlock.r1.dword=switchBlock.r1.dword ^ switchBlock.r2.dword; reg_eip+=switchBlock.eipCount;continue;
                            case Inst.XOR_E32_R32_16: eaa = ea16(switchBlock); Memory.mem_writed(eaa, Instructions.XORD(switchBlock.r1.dword, Memory.mem_readd(eaa))); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.XOR_E32_R32_16_fast: eaa = ea16(switchBlock); Memory.mem_writed(eaa, Memory.mem_readd(eaa) ^ switchBlock.r1.dword); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.XOR_E32_R32_32: eaa = ea32(switchBlock); Memory.mem_writed(eaa, Instructions.XORD(switchBlock.r1.dword, Memory.mem_readd(eaa))); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.XOR_E32_R32_32_fast:eaa = ea32(switchBlock); Memory.mem_writed(eaa, Memory.mem_readd(eaa) ^ switchBlock.r1.dword); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.XOR_E32_R32_32_sib: eaa = ea32sib(switchBlock); Memory.mem_writed(eaa, Instructions.XORD(switchBlock.r1.dword, Memory.mem_readd(eaa))); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.XOR_E32_R32_32_sib_fast:eaa = ea32sib(switchBlock); Memory.mem_writed(eaa, Memory.mem_readd(eaa) ^ switchBlock.r1.dword); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.XOR_R32_E32_16: switchBlock.r1.dword=Instructions.XORD(Memory.mem_readd(ea16(switchBlock)), switchBlock.r1.dword); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.XOR_R32_E32_16_fast: switchBlock.r1.dword=switchBlock.r1.dword ^ Memory.mem_readd(ea16(switchBlock)); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.XOR_R32_E32_32: switchBlock.r1.dword=Instructions.XORD(Memory.mem_readd(ea32(switchBlock)), switchBlock.r1.dword); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.XOR_R32_E32_32_fast: switchBlock.r1.dword=switchBlock.r1.dword ^ Memory.mem_readd(ea32(switchBlock)); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.XOR_R32_E32_32_sib: switchBlock.r1.dword=Instructions.XORD(Memory.mem_readd(ea32sib(switchBlock)), switchBlock.r1.dword); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.XOR_R32_E32_32_sib_fast: switchBlock.r1.dword=switchBlock.r1.dword ^ Memory.mem_readd(ea32sib(switchBlock)); reg_eip+=switchBlock.eipCount;continue;

                            case Inst.CMP_R8: Instructions.CMPB(switchBlock.value, switchBlock.r1.get8()); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.CMP_E8_16: Instructions.CMPB(switchBlock.value, Memory.mem_readb(ea16(switchBlock))); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.CMP_E8_32: Instructions.CMPB(switchBlock.value, Memory.mem_readb(ea32(switchBlock))); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.CMP_E8_32_sib: Instructions.CMPB(switchBlock.value, Memory.mem_readb(ea32sib(switchBlock))); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.CMP_R8_R8: Instructions.CMPB(switchBlock.r2.get8(), switchBlock.r1.get8()); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.CMP_E8_R8_16: Instructions.CMPB(switchBlock.r1.get8(), Memory.mem_readb(ea16(switchBlock))); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.CMP_E8_R8_32: Instructions.CMPB(switchBlock.r1.get8(), Memory.mem_readb(ea32(switchBlock))); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.CMP_E8_R8_32_sib: Instructions.CMPB(switchBlock.r1.get8(), Memory.mem_readb(ea32sib(switchBlock))); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.CMP_R8_E8_16: Instructions.CMPB(Memory.mem_readb(ea16(switchBlock)), switchBlock.r1.get8()); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.CMP_R8_E8_32: Instructions.CMPB(Memory.mem_readb(ea32(switchBlock)), switchBlock.r1.get8()); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.CMP_R8_E8_32_sib: Instructions.CMPB(Memory.mem_readb(ea32sib(switchBlock)), switchBlock.r1.get8()); reg_eip+=switchBlock.eipCount;continue;

                            case Inst.CMP_R16: Instructions.CMPW(switchBlock.value, switchBlock.r1.word()); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.CMP_E16_16: Instructions.CMPW(switchBlock.value, Memory.mem_readw(ea16(switchBlock))); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.CMP_E16_32: Instructions.CMPW(switchBlock.value, Memory.mem_readw(ea32(switchBlock))); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.CMP_E16_32_sib: Instructions.CMPW(switchBlock.value, Memory.mem_readw(ea32sib(switchBlock))); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.CMP_R16_R16: Instructions.CMPW(switchBlock.r2.word(), switchBlock.r1.word()); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.CMP_E16_R16_16: Instructions.CMPW(switchBlock.r1.word(), Memory.mem_readw(ea16(switchBlock))); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.CMP_E16_R16_32: Instructions.CMPW(switchBlock.r1.word(), Memory.mem_readw(ea32(switchBlock))); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.CMP_E16_R16_32_sib: Instructions.CMPW(switchBlock.r1.word(), Memory.mem_readw(ea32sib(switchBlock))); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.CMP_R16_E16_16: Instructions.CMPW(Memory.mem_readw(ea16(switchBlock)), switchBlock.r1.word()); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.CMP_R16_E16_32: Instructions.CMPW(Memory.mem_readw(ea32(switchBlock)), switchBlock.r1.word()); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.CMP_R16_E16_32_sib: Instructions.CMPW(Memory.mem_readw(ea32sib(switchBlock)), switchBlock.r1.word()); reg_eip+=switchBlock.eipCount;continue;

                            case Inst.CMP_R32: Instructions.CMPD(switchBlock.value, switchBlock.r1.dword); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.CMP_E32_16: Instructions.CMPD(switchBlock.value, Memory.mem_readd(ea16(switchBlock))); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.CMP_E32_32: Instructions.CMPD(switchBlock.value, Memory.mem_readd(ea32(switchBlock))); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.CMP_E32_32_sib: Instructions.CMPD(switchBlock.value, Memory.mem_readd(ea32sib(switchBlock))); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.CMP_R32_R32: Instructions.CMPD(switchBlock.r2.dword, switchBlock.r1.dword); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.CMP_E32_R32_16: Instructions.CMPD(switchBlock.r1.dword, Memory.mem_readd(ea16(switchBlock))); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.CMP_E32_R32_32: Instructions.CMPD(switchBlock.r1.dword, Memory.mem_readd(ea32(switchBlock))); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.CMP_E32_R32_32_sib: Instructions.CMPD(switchBlock.r1.dword, Memory.mem_readd(ea32sib(switchBlock))); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.CMP_R32_E32_16: Instructions.CMPD(Memory.mem_readd(ea16(switchBlock)), switchBlock.r1.dword); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.CMP_R32_E32_32: Instructions.CMPD(Memory.mem_readd(ea32(switchBlock)), switchBlock.r1.dword); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.CMP_R32_E32_32_sib: Instructions.CMPD(Memory.mem_readd(ea32sib(switchBlock)), switchBlock.r1.dword); reg_eip+=switchBlock.eipCount;continue;

                            case Inst.PUSH16_ES: CPU.CPU_Push16(CPU_Regs.reg_esVal.dword); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.POP16_ES: if (CPU.CPU_PopSegES(false)) {RUNEXCEPTION(); block=null; break;} else {reg_eip+=switchBlock.eipCount;continue;}
                            case Inst.PUSH16_CS: CPU.CPU_Push16(CPU_Regs.reg_csVal.dword); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.PUSH16_SS: CPU.CPU_Push16(CPU_Regs.reg_ssVal.dword); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.POP16_SS: if (CPU.CPU_PopSegSS(false)) {RUNEXCEPTION(); block=null; break;} else {reg_eip+=switchBlock.eipCount;continue;}
                            case Inst.PUSH16_DS: CPU.CPU_Push16(CPU_Regs.reg_dsVal.dword); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.POP16_DS: if (CPU.CPU_PopSegDS(false)) {RUNEXCEPTION(); block=null; break;} else {reg_eip+=switchBlock.eipCount;continue;}
                            case Inst.PUSH16_FS: CPU.CPU_Push16(CPU_Regs.reg_fsVal.dword); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.POP16_FS: if (CPU.CPU_PopSegFS(false)) {RUNEXCEPTION(); block=null; break;} else {reg_eip+=switchBlock.eipCount;continue;}
                            case Inst.PUSH16_GS: CPU.CPU_Push16(CPU_Regs.reg_gsVal.dword); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.POP16_GS: if (CPU.CPU_PopSegGS(false)) {RUNEXCEPTION(); block=null; break;} else {reg_eip+=switchBlock.eipCount;continue;}

                            case Inst.NOP: reg_eip+=switchBlock.eipCount;continue;
                            case Inst.DAA: Instructions.DAA(); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.DAS: Instructions.DAS(); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.AAA: Instructions.AAA(); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.AAS: Instructions.AAS(); reg_eip+=switchBlock.eipCount;continue;

                            case Inst.INCB: switchBlock.r1.set8(Instructions.INCB(switchBlock.r1.get8())); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.INCB_fast: switchBlock.r1.set8(switchBlock.r1.get8()+1); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.INCW: switchBlock.r1.word(Instructions.INCW(switchBlock.r1.word())); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.INCW_fast: switchBlock.r1.word(switchBlock.r1.word()+1); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.INCD: switchBlock.r1.dword=Instructions.INCD(switchBlock.r1.dword); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.INCD_fast: switchBlock.r1.dword++; reg_eip+=switchBlock.eipCount;continue;
                            case Inst.DECB: switchBlock.r1.set8(Instructions.DECB(switchBlock.r1.get8())); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.DECB_fast: switchBlock.r1.set8(switchBlock.r1.get8()-1); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.DECW: switchBlock.r1.word(Instructions.DECW(switchBlock.r1.word())); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.DECW_fast: switchBlock.r1.word(switchBlock.r1.word()-1); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.DECD: switchBlock.r1.dword=Instructions.DECD(switchBlock.r1.dword); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.DECD_fast: switchBlock.r1.dword--; reg_eip+=switchBlock.eipCount;continue;

                            case Inst.PUSH16: CPU.CPU_Push16(switchBlock.value); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.PUSH16_R16: CPU.CPU_Push16(switchBlock.r1.word()); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.PUSH16_E16: CPU.CPU_Push16(Memory.mem_readw(ea16(switchBlock))); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.PUSH16_E32: CPU.CPU_Push16(Memory.mem_readw(ea32(switchBlock))); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.PUSH16_E32_sib: CPU.CPU_Push16(Memory.mem_readw(ea32sib(switchBlock))); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.PUSH32: CPU.CPU_Push32(switchBlock.value); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.PUSH32_R32: CPU.CPU_Push32(switchBlock.r1.dword); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.PUSH32_E16: CPU.CPU_Push32(Memory.mem_readd(ea16(switchBlock))); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.PUSH32_E32: CPU.CPU_Push32(Memory.mem_readd(ea32(switchBlock))); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.PUSH32_E32_sib: CPU.CPU_Push32(Memory.mem_readd(ea32sib(switchBlock))); reg_eip+=switchBlock.eipCount;continue;

                            case Inst.POP16_R16: switchBlock.r1.word(CPU.CPU_Pop16()); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.POP16_E16: Memory.mem_writew(ea16(switchBlock), CPU.CPU_Pop16()); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.POP16_E32: Memory.mem_writew(ea32(switchBlock), CPU.CPU_Pop16()); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.POP16_E32_sib: Memory.mem_writew(ea32sib(switchBlock), CPU.CPU_Pop16()); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.POP32_R32: switchBlock.r1.dword=CPU.CPU_Pop32(); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.POP32_E16: Memory.mem_writed(ea16(switchBlock), CPU.CPU_Pop32()); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.POP32_E32: Memory.mem_writed(ea32(switchBlock), CPU.CPU_Pop32()); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.POP32_E32_sib: Memory.mem_writed(ea32sib(switchBlock), CPU.CPU_Pop32()); reg_eip+=switchBlock.eipCount;continue;

                            case Inst.PUSH16A: {
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
                                reg_eip+=switchBlock.eipCount;
                                continue;
                            }
                            case Inst.POP16A:
                                reg_edi.word(CPU.CPU_Peek16(0));reg_esi.word(CPU.CPU_Peek16(1));reg_ebp.word(CPU.CPU_Peek16(2));//Don't save SP
                                reg_ebx.word(CPU.CPU_Peek16(4));reg_edx.word(CPU.CPU_Peek16(5));reg_ecx.word(CPU.CPU_Peek16(6));reg_eax.word(CPU.CPU_Peek16(7));
                                CPU_Regs.reg_esp.dword=(CPU_Regs.reg_esp.dword & CPU.cpu.stack.notmask)|((CPU_Regs.reg_esp.dword+16) & CPU.cpu.stack.mask);
                                reg_eip+=switchBlock.eipCount;
                                continue;

                            case Inst.BOUND16: {
                                eaa = eaaSlow(switchBlock);
                                short bound_min = (short) Memory.mem_readw(eaa);
                                short bound_max = (short) Memory.mem_readw(eaa + 2);
                                short value = (short) switchBlock.r1.word();
                                if ((value < bound_min) || (value > bound_max)) {
                                    CPU.CPU_Exception(5);
                                    block = null;
                                    break;
                                }
                                reg_eip+=switchBlock.eipCount;
                                continue;
                            }
                            case Inst.BOUND32: {
                                eaa=eaaSlow(switchBlock);
                                int bound_min=Memory.mem_readd(eaa);
                                int bound_max=Memory.mem_readd(eaa + 4);
                                int rmrd = switchBlock.r1.dword;
                                if (rmrd < bound_min || rmrd > bound_max) {
                                    CPU.CPU_Exception(5);
                                    block = null;
                                    break;
                                }
                                reg_eip+=switchBlock.eipCount;
                                continue;
                            }
                            case Inst.ARPL_R16_R16:
                                if ((CPU_Regs.flags & CPU_Regs.VM)!=0 || (!CPU.cpu.pmode)) {
                                    CPU.CPU_Exception(6,0);
                                    block = null;
                                    break;
                                }
                                switchBlock.r1.word(CPU.CPU_ARPL(switchBlock.r1.word(), switchBlock.r2.word()));
                                reg_eip+=switchBlock.eipCount;
                                continue;
                            case Inst.ARPL_R16_E16: {
                                if ((CPU_Regs.flags & CPU_Regs.VM)!=0 || (!CPU.cpu.pmode)) {
                                    CPU.CPU_Exception(6,0);
                                    block = null;
                                    break;
                                }
                                eaa=eaaSlow(switchBlock);
                                Memory.mem_writew(eaa,CPU.CPU_ARPL(Memory.mem_readw(eaa),switchBlock.r1.word()));
                                reg_eip+=switchBlock.eipCount;
                                continue;
                            }
                            case Inst.IMUL_R16_R16: switchBlock.r2.word(Instructions.DIMULW(switchBlock.r1.word(), switchBlock.value)); reg_eip+=switchBlock.eipCount; continue;
                            case Inst.IMUL_R16_R16_fast: switchBlock.r2.word(((short)switchBlock.r1.word())*switchBlock.value); reg_eip+=switchBlock.eipCount; continue;
                            case Inst.IMUL_R16_E16: switchBlock.r1.word(Instructions.DIMULW(Memory.mem_readw(ea16(switchBlock)), switchBlock.value)); reg_eip+=switchBlock.eipCount; continue;
                            case Inst.IMUL_R16_E16_fast: switchBlock.r1.word(((short)Memory.mem_readw(ea16(switchBlock)))*switchBlock.value); reg_eip+=switchBlock.eipCount; continue;
                            case Inst.IMUL_R16_E32: switchBlock.r1.word(Instructions.DIMULW(Memory.mem_readw(ea32(switchBlock)), switchBlock.value)); reg_eip+=switchBlock.eipCount; continue;
                            case Inst.IMUL_R16_E32_fast: switchBlock.r1.word(((short)Memory.mem_readw(ea32(switchBlock)))*switchBlock.value); reg_eip+=switchBlock.eipCount; continue;
                            case Inst.IMUL_R16_E32_sib: switchBlock.r1.word(Instructions.DIMULW(Memory.mem_readw(ea32sib(switchBlock)), switchBlock.value)); reg_eip+=switchBlock.eipCount; continue;
                            case Inst.IMUL_R16_E32_sib_fast: switchBlock.r1.word(((short)Memory.mem_readw(ea32sib(switchBlock)))*switchBlock.value); reg_eip+=switchBlock.eipCount; continue;

                            case Inst.STRING_EXCEPTION: {
                                if (CPU.CPU_IO_Exception(reg_edx.word(), switchBlock.eaa_sib)) {
                                    RUNEXCEPTION();
                                    block = null;
                                    break;
                                }
                                Core.rep_zero = switchBlock.zero;
                                Core.base_ds = switchBlock.eaa_segPhys.dword;
                                StringOp.DoString(switchBlock.eaa_const, switchBlock.value);
                                reg_eip+=switchBlock.eipCount;
                                continue;
                            }
                            case Inst.STRING: {
                                Core.rep_zero = switchBlock.zero;
                                Core.base_ds = switchBlock.eaa_segPhys.dword;
                                StringOp.DoString(switchBlock.eaa_const, switchBlock.value);
                                reg_eip+=switchBlock.eipCount;
                                continue;
                            }
                            case Inst.JUMP16_JO: block = jump16(block, Flags.TFLG_O(), switchBlock.value, switchBlock.eipCount); break;
                            case Inst.JUMP16_NJO: block = jump16(block, Flags.TFLG_NO(), switchBlock.value, switchBlock.eipCount); break;
                            case Inst.JUMP16_B: block = jump16(block, Flags.TFLG_B(), switchBlock.value, switchBlock.eipCount); break;
                            case Inst.JUMP16_NB: block = jump16(block, Flags.TFLG_NB(), switchBlock.value, switchBlock.eipCount); break;
                            case Inst.JUMP16_Z: block = jump16(block, Flags.TFLG_Z(), switchBlock.value, switchBlock.eipCount); break;
                            case Inst.JUMP16_NZ: block = jump16(block, Flags.TFLG_NZ(), switchBlock.value, switchBlock.eipCount); break;
                            case Inst.JUMP16_BE: block = jump16(block, Flags.TFLG_BE(), switchBlock.value, switchBlock.eipCount); break;
                            case Inst.JUMP16_NBE: block = jump16(block, Flags.TFLG_NBE(), switchBlock.value, switchBlock.eipCount); break;
                            case Inst.JUMP16_S: block = jump16(block, Flags.TFLG_S(), switchBlock.value, switchBlock.eipCount); break;
                            case Inst.JUMP16_NS: block = jump16(block, Flags.TFLG_NS(), switchBlock.value, switchBlock.eipCount); break;
                            case Inst.JUMP16_P: block = jump16(block, Flags.TFLG_P(), switchBlock.value, switchBlock.eipCount); break;
                            case Inst.JUMP16_NP: block = jump16(block, Flags.TFLG_NP(), switchBlock.value, switchBlock.eipCount); break;
                            case Inst.JUMP16_L: block = jump16(block, Flags.TFLG_L(), switchBlock.value, switchBlock.eipCount); break;
                            case Inst.JUMP16_NL: block = jump16(block, Flags.TFLG_NL(), switchBlock.value, switchBlock.eipCount); break;
                            case Inst.JUMP16_LE: block = jump16(block, Flags.TFLG_LE(), switchBlock.value, switchBlock.eipCount); break;
                            case Inst.JUMP16_NLE: block = jump16(block, Flags.TFLG_NLE(), switchBlock.value, switchBlock.eipCount); break;

                            case Inst.TEST_R8: Instructions.TESTB(switchBlock.value, switchBlock.r1.get8()); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.TEST_E8_16: Instructions.TESTB(switchBlock.value, Memory.mem_readb(ea16(switchBlock))); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.TEST_E8_32: Instructions.TESTB(switchBlock.value, Memory.mem_readb(ea32(switchBlock))); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.TEST_E8_32_sib: Instructions.TESTB(switchBlock.value, Memory.mem_readb(ea32sib(switchBlock))); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.TEST_R8_R8: Instructions.TESTB(switchBlock.r2.get8(), switchBlock.r1.get8()); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.TEST_R8_E8_16: Instructions.TESTB(Memory.mem_readb(ea16(switchBlock)), switchBlock.r1.get8()); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.TEST_R8_E8_32: Instructions.TESTB(Memory.mem_readb(ea32(switchBlock)), switchBlock.r1.get8()); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.TEST_R8_E8_32_sib: Instructions.TESTB(Memory.mem_readb(ea32sib(switchBlock)), switchBlock.r1.get8()); reg_eip+=switchBlock.eipCount;continue;

                            case Inst.TEST_R16: Instructions.TESTW(switchBlock.value, switchBlock.r1.word()); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.TEST_E16_16: Instructions.TESTW(switchBlock.value, Memory.mem_readw(ea16(switchBlock))); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.TEST_E16_32: Instructions.TESTW(switchBlock.value, Memory.mem_readw(ea32(switchBlock))); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.TEST_E16_32_sib: Instructions.TESTW(switchBlock.value, Memory.mem_readw(ea32sib(switchBlock))); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.TEST_R16_R16: Instructions.TESTW(switchBlock.r2.word(), switchBlock.r1.word()); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.TEST_R16_E16_16: Instructions.TESTW(Memory.mem_readw(ea16(switchBlock)), switchBlock.r1.word()); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.TEST_R16_E16_32: Instructions.TESTW(Memory.mem_readw(ea32(switchBlock)), switchBlock.r1.word()); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.TEST_R16_E16_32_sib: Instructions.TESTW(Memory.mem_readw(ea32sib(switchBlock)), switchBlock.r1.word()); reg_eip+=switchBlock.eipCount;continue;

                            case Inst.TEST_R32: Instructions.TESTD(switchBlock.value, switchBlock.r1.dword); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.TEST_E32_16: Instructions.TESTD(switchBlock.value, Memory.mem_readd(ea16(switchBlock))); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.TEST_E32_32: Instructions.TESTD(switchBlock.value, Memory.mem_readd(ea32(switchBlock))); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.TEST_E32_32_sib: Instructions.TESTD(switchBlock.value, Memory.mem_readd(ea32sib(switchBlock))); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.TEST_R32_R32: Instructions.TESTD(switchBlock.r2.dword, switchBlock.r1.dword); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.TEST_R32_E32_16: Instructions.TESTD(Memory.mem_readd(ea16(switchBlock)), switchBlock.r1.dword); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.TEST_R32_E32_32: Instructions.TESTD(Memory.mem_readd(ea32(switchBlock)), switchBlock.r1.dword); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.TEST_R32_E32_32_sib: Instructions.TESTD(Memory.mem_readd(ea32sib(switchBlock)), switchBlock.r1.dword); reg_eip+=switchBlock.eipCount;continue;

                            case Inst.XCHG_R8_R8: tmp=switchBlock.r1.get8();switchBlock.r1.set8(switchBlock.r2.get8());switchBlock.r2.set8(tmp); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.XCHG_R8_E8_16: eaa=ea16(switchBlock);tmp=switchBlock.r1.get8();switchBlock.r1.set8(Memory.mem_readb(eaa));Memory.mem_writeb(eaa, tmp); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.XCHG_R8_E8_32: eaa=ea32(switchBlock);tmp=switchBlock.r1.get8();switchBlock.r1.set8(Memory.mem_readb(eaa));Memory.mem_writeb(eaa, tmp); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.XCHG_R8_E8_32_sib: eaa=ea32sib(switchBlock);tmp=switchBlock.r1.get8();switchBlock.r1.set8(Memory.mem_readb(eaa));Memory.mem_writeb(eaa, tmp); reg_eip+=switchBlock.eipCount;continue;

                            case Inst.XCHG_R16_R16: tmp=switchBlock.r1.word();switchBlock.r1.word(switchBlock.r2.word());switchBlock.r2.word(tmp); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.XCHG_R16_E16_16: eaa=ea16(switchBlock);tmp=switchBlock.r1.word();switchBlock.r1.word(Memory.mem_readw(eaa));Memory.mem_writew(eaa, tmp); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.XCHG_R16_E16_32: eaa=ea32(switchBlock);tmp=switchBlock.r1.word();switchBlock.r1.word(Memory.mem_readw(eaa));Memory.mem_writew(eaa, tmp); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.XCHG_R16_E16_32_sib: eaa=ea32sib(switchBlock);tmp=switchBlock.r1.word();switchBlock.r1.word(Memory.mem_readw(eaa));Memory.mem_writew(eaa, tmp); reg_eip+=switchBlock.eipCount;continue;

                            case Inst.XCHG_R32_R32: tmp=switchBlock.r1.dword;switchBlock.r1.dword=switchBlock.r2.dword;switchBlock.r2.dword=tmp; reg_eip+=switchBlock.eipCount;continue;
                            case Inst.XCHG_R32_E32_16: eaa=ea16(switchBlock);tmp=switchBlock.r1.dword;switchBlock.r1.dword=Memory.mem_readd(eaa);Memory.mem_writed(eaa, tmp); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.XCHG_R32_E32_32: eaa=ea32(switchBlock);tmp=switchBlock.r1.dword;switchBlock.r1.dword=Memory.mem_readd(eaa);Memory.mem_writed(eaa, tmp); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.XCHG_R32_E32_32_sib: eaa=ea32sib(switchBlock);tmp=switchBlock.r1.dword;switchBlock.r1.dword=Memory.mem_readd(eaa);Memory.mem_writed(eaa, tmp); reg_eip+=switchBlock.eipCount;continue;

                            case Inst.MOV_R8: switchBlock.r1.set8(switchBlock.value); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.MOV_E8_16: Memory.mem_writeb(ea16(switchBlock), switchBlock.value); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.MOV_E8_32: Memory.mem_writeb(ea32(switchBlock), switchBlock.value); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.MOV_E8_32_sib: Memory.mem_writeb(ea32sib(switchBlock), switchBlock.value); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.MOV_R8_R8: switchBlock.r1.set8(switchBlock.r2.get8()); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.MOV_E8_R8_16: Memory.mem_writeb(ea16(switchBlock), switchBlock.r1.get8()); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.MOV_E8_R8_32: Memory.mem_writeb(ea32(switchBlock), switchBlock.r1.get8()); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.MOV_E8_R8_32_sib: Memory.mem_writeb(ea32sib(switchBlock), switchBlock.r1.get8()); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.MOV_R8_E8_16: switchBlock.r1.set8(Memory.mem_readb(ea16(switchBlock))); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.MOV_R8_E8_32: switchBlock.r1.set8(Memory.mem_readb(ea32(switchBlock))); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.MOV_R8_E8_32_sib: switchBlock.r1.set8(Memory.mem_readb(ea32sib(switchBlock))); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.MOV_R8_R8_RM5: if (RM5(switchBlock)) {block=null;break;} switchBlock.r1.set8(switchBlock.r2.get8()); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.MOV_E8_R8_16_RM5: if (RM5(switchBlock)) {block=null;break;} Memory.mem_writeb(ea16(switchBlock), switchBlock.r1.get8()); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.MOV_E8_R8_32_RM5: if (RM5(switchBlock)) {block=null;break;} Memory.mem_writeb(ea32(switchBlock), switchBlock.r1.get8()); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.MOV_E8_R8_32_sib_RM5: if (RM5(switchBlock)) {block=null;break;} Memory.mem_writeb(ea32sib(switchBlock), switchBlock.r1.get8()); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.MOV_R16: switchBlock.r1.word(switchBlock.value); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.MOV_E16_16: Memory.mem_writew(ea16(switchBlock), switchBlock.value); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.MOV_E16_32: Memory.mem_writew(ea32(switchBlock), switchBlock.value); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.MOV_E16_32_sib: Memory.mem_writew(ea32sib(switchBlock), switchBlock.value); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.MOV_R16_R16: switchBlock.r1.word(switchBlock.r2.word()); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.MOV_E16_R16_16: Memory.mem_writew(ea16(switchBlock), switchBlock.r1.word()); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.MOV_E16_R16_32: Memory.mem_writew(ea32(switchBlock), switchBlock.r1.word()); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.MOV_E16_R16_32_sib: Memory.mem_writew(ea32sib(switchBlock), switchBlock.r1.word()); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.MOV_R16_E16_16: switchBlock.r1.word(Memory.mem_readw(ea16(switchBlock))); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.MOV_R16_E16_32: switchBlock.r1.word(Memory.mem_readw(ea32(switchBlock))); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.MOV_R16_E16_32_sib: switchBlock.r1.word(Memory.mem_readw(ea32sib(switchBlock))); reg_eip+=switchBlock.eipCount;continue;

                            case Inst.MOV_R32: switchBlock.r1.dword = switchBlock.value; reg_eip+=switchBlock.eipCount;continue;
                            case Inst.MOV_E32_16: Memory.mem_writed(ea16(switchBlock), switchBlock.value); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.MOV_E32_32: Memory.mem_writed(ea32(switchBlock), switchBlock.value); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.MOV_E32_32_sib: Memory.mem_writed(ea32sib(switchBlock), switchBlock.value); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.MOV_R32_R32: switchBlock.r1.dword=switchBlock.r2.dword; reg_eip+=switchBlock.eipCount;continue;
                            case Inst.MOV_E32_R32_16: Memory.mem_writed(ea16(switchBlock), switchBlock.r1.dword); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.MOV_E32_R32_32: Memory.mem_writed(ea32(switchBlock), switchBlock.r1.dword); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.MOV_E32_R32_32_sib: Memory.mem_writed(ea32sib(switchBlock), switchBlock.r1.dword); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.MOV_R32_E32_16: switchBlock.r1.dword=Memory.mem_readd(ea16(switchBlock)); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.MOV_R32_E32_32: switchBlock.r1.dword=Memory.mem_readd(ea32(switchBlock)); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.MOV_R32_E32_32_sib: switchBlock.r1.dword=Memory.mem_readd(ea32sib(switchBlock)); reg_eip+=switchBlock.eipCount;continue;

                            case Inst.ILLEGAL: CPU.CPU_Exception(6,0); block = null; break;
                            case Inst.LEA_R16_16: switchBlock.r1.word(ea16(switchBlock)); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.LEA_R16_32: switchBlock.r1.word(ea32(switchBlock)); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.LEA_R16_32_sib: switchBlock.r1.word(ea32sib(switchBlock)); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.LEA_R32_16: switchBlock.r1.dword=ea16(switchBlock); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.LEA_R32_32: switchBlock.r1.dword=ea32(switchBlock); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.LEA_R32_32_sib: switchBlock.r1.dword=ea32sib(switchBlock); reg_eip+=switchBlock.eipCount;continue;

                            case Inst.MOV_ES_R16: if (CPU.CPU_SetSegGeneralES(switchBlock.r1.word())) {RUNEXCEPTION(); block=null; break;} reg_eip+=switchBlock.eipCount;continue;
                            case Inst.MOV_ES_E16: if (CPU.CPU_SetSegGeneralES(Memory.mem_readw(ea16(switchBlock)))) {RUNEXCEPTION(); block=null; break;} reg_eip+=switchBlock.eipCount;continue;
                            case Inst.MOV_ES_E32: if (CPU.CPU_SetSegGeneralES(Memory.mem_readw(ea32(switchBlock)))) {RUNEXCEPTION(); block=null; break;} reg_eip+=switchBlock.eipCount;continue;
                            case Inst.MOV_ES_E32_sib: if (CPU.CPU_SetSegGeneralES(Memory.mem_readw(ea32sib(switchBlock)))) {RUNEXCEPTION(); block=null; break;} reg_eip+=switchBlock.eipCount;continue;
                            case Inst.MOV_SS_R16: if (CPU.CPU_SetSegGeneralSS(switchBlock.r1.word())) {RUNEXCEPTION(); block=null; break;} reg_eip+=switchBlock.eipCount;continue;
                            case Inst.MOV_SS_E16: if (CPU.CPU_SetSegGeneralSS(Memory.mem_readw(ea16(switchBlock)))) {RUNEXCEPTION(); block=null; break;} reg_eip+=switchBlock.eipCount;continue;
                            case Inst.MOV_SS_E32: if (CPU.CPU_SetSegGeneralSS(Memory.mem_readw(ea32(switchBlock)))) {RUNEXCEPTION(); block=null; break;} reg_eip+=switchBlock.eipCount;continue;
                            case Inst.MOV_SS_E32_sib: if (CPU.CPU_SetSegGeneralSS(Memory.mem_readw(ea32sib(switchBlock)))) {RUNEXCEPTION(); block=null; break;} reg_eip+=switchBlock.eipCount;continue;
                            case Inst.MOV_DS_R16: if (CPU.CPU_SetSegGeneralDS(switchBlock.r1.word())) {RUNEXCEPTION(); block=null; break;} reg_eip+=switchBlock.eipCount;continue;
                            case Inst.MOV_DS_E16: if (CPU.CPU_SetSegGeneralDS(Memory.mem_readw(ea16(switchBlock)))) {RUNEXCEPTION(); block=null; break;} reg_eip+=switchBlock.eipCount;continue;
                            case Inst.MOV_DS_E32: if (CPU.CPU_SetSegGeneralDS(Memory.mem_readw(ea32(switchBlock)))) {RUNEXCEPTION(); block=null; break;} reg_eip+=switchBlock.eipCount;continue;
                            case Inst.MOV_DS_E32_sib: if (CPU.CPU_SetSegGeneralDS(Memory.mem_readw(ea32sib(switchBlock)))) {RUNEXCEPTION(); block=null; break;} reg_eip+=switchBlock.eipCount;continue;
                            case Inst.MOV_FS_R16: if (CPU.CPU_SetSegGeneralFS(switchBlock.r1.word())) {RUNEXCEPTION(); block=null; break;} reg_eip+=switchBlock.eipCount;continue;
                            case Inst.MOV_FS_E16: if (CPU.CPU_SetSegGeneralFS(Memory.mem_readw(ea16(switchBlock)))) {RUNEXCEPTION(); block=null; break;} reg_eip+=switchBlock.eipCount;continue;
                            case Inst.MOV_FS_E32: if (CPU.CPU_SetSegGeneralFS(Memory.mem_readw(ea32(switchBlock)))) {RUNEXCEPTION(); block=null; break;} reg_eip+=switchBlock.eipCount;continue;
                            case Inst.MOV_FS_E32_sib: if (CPU.CPU_SetSegGeneralFS(Memory.mem_readw(ea32sib(switchBlock)))) {RUNEXCEPTION(); block=null; break;} reg_eip+=switchBlock.eipCount;continue;
                            case Inst.MOV_GS_R16: if (CPU.CPU_SetSegGeneralGS(switchBlock.r1.word())) {RUNEXCEPTION(); block=null; break;} reg_eip+=switchBlock.eipCount;continue;
                            case Inst.MOV_GS_E16: if (CPU.CPU_SetSegGeneralGS(Memory.mem_readw(ea16(switchBlock)))) {RUNEXCEPTION(); block=null; break;} reg_eip+=switchBlock.eipCount;continue;
                            case Inst.MOV_GS_E32: if (CPU.CPU_SetSegGeneralGS(Memory.mem_readw(ea32(switchBlock)))) {RUNEXCEPTION(); block=null; break;} reg_eip+=switchBlock.eipCount;continue;
                            case Inst.MOV_GS_E32_sib: if (CPU.CPU_SetSegGeneralGS(Memory.mem_readw(ea32sib(switchBlock)))) {RUNEXCEPTION(); block=null; break;} reg_eip+=switchBlock.eipCount;continue;

                            case Inst.CBW: reg_eax.word((byte)reg_eax.low()); reg_eip+=switchBlock.eipCount;continue;
                            case Inst.CWD: if ((reg_eax.word() & 0x8000)!=0) reg_edx.word(0xffff);else reg_edx.word(0); reg_eip+=switchBlock.eipCount;continue;

                            case Inst.CALL16_AP:
                                Flags.FillFlags();
                                CPU.CPU_CALL(false,switchBlock.eaa_const,switchBlock.value,(reg_eip+switchBlock.eipCount) & 0xFFFF);
                                if (GETFLAG(TF)!=0) {
                                    CPU.cpudecoder=CPU_Core_Switch_Trap_Run;
                                    return Callback.CBRET_NONE;
                                }
                                block = null;
                                break;
                            case Inst.CALL16_EP:
                                Flags.FillFlags();
                                eaa = eaaSlow(switchBlock);
                                CPU.CPU_CALL(false,Memory.mem_readw(eaa+2),Memory.mem_readw(eaa),(reg_eip+switchBlock.eipCount) & 0xFFFF);
                                if (GETFLAG(TF)!=0) {
                                    CPU.cpudecoder=CPU_Core_Switch_Trap_Run;
                                    return Callback.CBRET_NONE;
                                }
                                block = null;
                                break;
                            case Inst.CALL32_AP:
                                Flags.FillFlags();
                                CPU.CPU_CALL(true,switchBlock.eaa_const,switchBlock.value,reg_eip+switchBlock.eipCount);
                                if (GETFLAG(TF)!=0) {
                                    CPU.cpudecoder=CPU_Core_Switch_Trap_Run;
                                    return Callback.CBRET_NONE;
                                }
                                block = null;
                                break;
                            case Inst.CALL32_EP:
                                Flags.FillFlags();
                                eaa = eaaSlow(switchBlock);
                                CPU.CPU_CALL(false,Memory.mem_readw(eaa+4),Memory.mem_readd(eaa),reg_eip+switchBlock.eipCount);
                                if (GETFLAG(TF)!=0) {
                                    CPU.cpudecoder=CPU_Core_Switch_Trap_Run;
                                    return Callback.CBRET_NONE;
                                }
                                block = null;
                                break;
                            case Inst.PUSHF: if (CPU.CPU_PUSHF(!switchBlock.eaa16)) {RUNEXCEPTION(); block=null; break;} reg_eip+=switchBlock.eipCount; continue;
                            case Inst.POPF:
                                if (CPU.CPU_POPF(!switchBlock.eaa16)) {RUNEXCEPTION(); block=null; break;}
                                reg_eip+=switchBlock.eipCount;
                                if (GETFLAG(TF)!=0) {
                                    CPU.cpudecoder=CPU_Core_Switch_Trap_Run;
                                    return Callback.CBRET_NONE;
                                }
                                if (GETFLAG(IF)!=0 && Pic.PIC_IRQCheck!=0) return Callback.CBRET_NONE;
                                continue;
                            case Inst.SAHF:Flags.SETFLAGSb(reg_eax.high());reg_eip+=switchBlock.eipCount;continue;
                            case Inst.LAHF:Flags.FillFlags();reg_eax.high(CPU_Regs.flags&0xff);reg_eip+=switchBlock.eipCount;continue;
                            case Inst.MOV_AL_0b: reg_eax.low(Memory.mem_readb(switchBlock.eaa_segPhys.dword+switchBlock.value));reg_eip+=switchBlock.eipCount;continue;
                            case Inst.MOV_0b_AL: Memory.mem_writeb(switchBlock.eaa_segPhys.dword+switchBlock.value,reg_eax.low());reg_eip+=switchBlock.eipCount;continue;
                            case Inst.MOV_AX_0w: reg_eax.word(Memory.mem_readw(switchBlock.eaa_segPhys.dword+switchBlock.value));reg_eip+=switchBlock.eipCount;continue;
                            case Inst.MOV_0w_AX: Memory.mem_writew(switchBlock.eaa_segPhys.dword+switchBlock.value,reg_eax.word());reg_eip+=switchBlock.eipCount;continue;
                            case Inst.MOVSB16: Strings.Movsb16.doString(switchBlock.eaa_segPhys.dword);reg_eip+=switchBlock.eipCount;continue;
                            case Inst.MOVSB16r: Strings.Movsb16r.doString(switchBlock.eaa_segPhys.dword);reg_eip+=switchBlock.eipCount;continue;
                            case Inst.MOVSB32: Strings.Movsb32.doString(switchBlock.eaa_segPhys.dword);reg_eip+=switchBlock.eipCount;continue;
                            case Inst.MOVSB32r: Strings.Movsb32r.doString(switchBlock.eaa_segPhys.dword);reg_eip+=switchBlock.eipCount;continue;
                            case Inst.MOVSW16: Strings.Movsw16.doString(switchBlock.eaa_segPhys.dword);reg_eip+=switchBlock.eipCount;continue;
                            case Inst.MOVSW16r: Strings.Movsw16r.doString(switchBlock.eaa_segPhys.dword);reg_eip+=switchBlock.eipCount;continue;
                            case Inst.MOVSW32: Strings.Movsw32.doString(switchBlock.eaa_segPhys.dword);reg_eip+=switchBlock.eipCount;continue;
                            case Inst.MOVSW32r: Strings.Movsw32r.doString(switchBlock.eaa_segPhys.dword);reg_eip+=switchBlock.eipCount;continue;
                                
                            case Inst.ROLB_0_flags: {
                                int value = switchBlock.r1.get8();
                                FillFlagsNoCFOF();
                                CPU_Regs.SETFLAGBIT(CPU_Regs.CF, (value & 1) != 0);
                                CPU_Regs.SETFLAGBIT(CPU_Regs.OF, ((value & 1) ^ (value >>> 7)) != 0);
                                reg_eip += switchBlock.eipCount;
                                continue;
                            }
                            case Inst.ROLB_0_flags_16: {
                                int value = Memory.mem_readb(ea16(switchBlock));
                                FillFlagsNoCFOF();
                                CPU_Regs.SETFLAGBIT(CPU_Regs.CF, (value & 1) != 0);
                                CPU_Regs.SETFLAGBIT(CPU_Regs.OF, ((value & 1) ^ (value >>> 7)) != 0);
                                reg_eip += switchBlock.eipCount;
                                continue;
                            }
                            case Inst.ROLB_0_flags_32: {
                                int value = Memory.mem_readb(ea32(switchBlock));
                                FillFlagsNoCFOF();
                                CPU_Regs.SETFLAGBIT(CPU_Regs.CF, (value & 1) != 0);
                                CPU_Regs.SETFLAGBIT(CPU_Regs.OF, ((value & 1) ^ (value >>> 7)) != 0);
                                reg_eip += switchBlock.eipCount;
                                continue;
                            }
                            case Inst.ROLB_0_flags_32_sib: {
                                int value = Memory.mem_readb(ea32sib(switchBlock));
                                FillFlagsNoCFOF();
                                CPU_Regs.SETFLAGBIT(CPU_Regs.CF, (value & 1) != 0);
                                CPU_Regs.SETFLAGBIT(CPU_Regs.OF, ((value & 1) ^ (value >>> 7)) != 0);
                                reg_eip += switchBlock.eipCount;
                                continue;
                            }
                            // (Eb << Ib) | (Eb >>> (8-Ib)
                            case Inst.ROLB: switchBlock.r1.set8(Instructions.do_ROLB(switchBlock.value, switchBlock.r1.get8()));reg_eip += switchBlock.eipCount;continue;
                            case Inst.ROLB_fast: tmp=switchBlock.r1.get8();switchBlock.r1.set8((tmp << switchBlock.value) | (tmp >>> (8-switchBlock.value)));reg_eip += switchBlock.eipCount;continue;
                            case Inst.ROLB_16: eaa=ea16(switchBlock);Memory.mem_writeb(eaa, Instructions.do_ROLB(switchBlock.value, Memory.mem_readb(eaa)));reg_eip += switchBlock.eipCount;continue;
                            case Inst.ROLB_16_fast: eaa=ea16(switchBlock); tmp=Memory.mem_readb(eaa);Memory.mem_writeb(eaa, (tmp << switchBlock.value) | (tmp >>> (8-switchBlock.value)));reg_eip += switchBlock.eipCount;continue;
                            case Inst.ROLB_32: eaa=ea32(switchBlock);Memory.mem_writeb(eaa, Instructions.do_ROLB(switchBlock.value, Memory.mem_readb(eaa)));reg_eip += switchBlock.eipCount;continue;
                            case Inst.ROLB_32_fast: eaa=ea32(switchBlock); tmp=Memory.mem_readb(eaa);Memory.mem_writeb(eaa, (tmp << switchBlock.value) | (tmp >>> (8-switchBlock.value)));reg_eip += switchBlock.eipCount;continue;
                            case Inst.ROLB_32_sib: eaa=ea32sib(switchBlock);Memory.mem_writeb(eaa, Instructions.do_ROLB(switchBlock.value, Memory.mem_readb(eaa)));reg_eip += switchBlock.eipCount;continue;
                            case Inst.ROLB_32_sib_fast: eaa=ea32sib(switchBlock); tmp=Memory.mem_readb(eaa);Memory.mem_writeb(eaa, (tmp << switchBlock.value) | (tmp >>> (8-switchBlock.value)));reg_eip += switchBlock.eipCount;continue;

                            case Inst.RORB_0_flags: {
                                int value = switchBlock.r1.get8();
                                FillFlagsNoCFOF();
                                CPU_Regs.SETFLAGBIT(CPU_Regs.CF,(value>>7)!=0);
                                CPU_Regs.SETFLAGBIT(CPU_Regs.OF,((value>>7) ^ ((value>>>6) & 1))!=0);
                                reg_eip += switchBlock.eipCount;
                                continue;
                            }
                            case Inst.RORB_0_flags_16: {
                                int value = Memory.mem_readb(ea16(switchBlock));
                                FillFlagsNoCFOF();
                                CPU_Regs.SETFLAGBIT(CPU_Regs.CF,(value>>7)!=0);
                                CPU_Regs.SETFLAGBIT(CPU_Regs.OF,((value>>7) ^ ((value>>>6) & 1))!=0);
                                reg_eip += switchBlock.eipCount;
                                continue;
                            }
                            case Inst.RORB_0_flags_32: {
                                int value = Memory.mem_readb(ea32(switchBlock));
                                FillFlagsNoCFOF();
                                CPU_Regs.SETFLAGBIT(CPU_Regs.CF,(value>>7)!=0);
                                CPU_Regs.SETFLAGBIT(CPU_Regs.OF,((value>>7) ^ ((value>>>6) & 1))!=0);
                                reg_eip += switchBlock.eipCount;
                                continue;
                            }
                            case Inst.RORB_0_flags_32_sib: {
                                int value = Memory.mem_readb(ea32sib(switchBlock));
                                FillFlagsNoCFOF();
                                CPU_Regs.SETFLAGBIT(CPU_Regs.CF,(value>>7)!=0);
                                CPU_Regs.SETFLAGBIT(CPU_Regs.OF,((value>>7) ^ ((value>>>6) & 1))!=0);
                                reg_eip += switchBlock.eipCount;
                                continue;
                            }
                            // (Eb >>> Ib) | (Eb << (8-Ib))
                            case Inst.RORB: switchBlock.r1.set8(Instructions.do_RORB(switchBlock.value, switchBlock.r1.get8()));reg_eip += switchBlock.eipCount;continue;
                            case Inst.RORB_fast: tmp=switchBlock.r1.get8();switchBlock.r1.set8((tmp >>> switchBlock.value) | (tmp << (8-switchBlock.value)));reg_eip += switchBlock.eipCount;continue;
                            case Inst.RORB_16: eaa=ea16(switchBlock);Memory.mem_writeb(eaa, Instructions.do_RORB(switchBlock.value, Memory.mem_readb(eaa)));reg_eip += switchBlock.eipCount;continue;
                            case Inst.RORB_16_fast: eaa=ea16(switchBlock); tmp=Memory.mem_readb(eaa);Memory.mem_writeb(eaa, (tmp >>> switchBlock.value) | (tmp << (8-switchBlock.value)));reg_eip += switchBlock.eipCount;continue;
                            case Inst.RORB_32: eaa=ea32(switchBlock);Memory.mem_writeb(eaa, Instructions.do_RORB(switchBlock.value, Memory.mem_readb(eaa)));reg_eip += switchBlock.eipCount;continue;
                            case Inst.RORB_32_fast: eaa=ea32(switchBlock); tmp=Memory.mem_readb(eaa);Memory.mem_writeb(eaa, (tmp >>> switchBlock.value) | (tmp << (8-switchBlock.value)));reg_eip += switchBlock.eipCount;continue;
                            case Inst.RORB_32_sib: eaa=ea32sib(switchBlock);Memory.mem_writeb(eaa, Instructions.do_RORB(switchBlock.value, Memory.mem_readb(eaa)));reg_eip += switchBlock.eipCount;continue;
                            case Inst.RORB_32_sib_fast: eaa=ea32sib(switchBlock); tmp=Memory.mem_readb(eaa);Memory.mem_writeb(eaa, (tmp >>> switchBlock.value) | (tmp << (8-switchBlock.value)));reg_eip += switchBlock.eipCount;continue;

                            // (Eb << Ib) |(cf << (Ib-1)) | (Eb >>> (9-Ib));
                            case Inst.RCLB: switchBlock.r1.set8(Instructions.do_RCLB(switchBlock.value, switchBlock.r1.get8()));reg_eip += switchBlock.eipCount;continue;
                            case Inst.RCLB_fast: cf=Flags.get_CF();tmp=switchBlock.r1.get8();tmp=(tmp<<switchBlock.value)|(tmp>>>(9-switchBlock.value));if (cf) tmp|=(1<<(switchBlock.value-1)); switchBlock.r1.set8(tmp);reg_eip += switchBlock.eipCount;continue;
                            case Inst.RCLB_16: eaa=ea16(switchBlock);Memory.mem_writeb(eaa, Instructions.do_RCLB(switchBlock.value, Memory.mem_readb(eaa)));reg_eip += switchBlock.eipCount;continue;
                            case Inst.RCLB_16_fast: cf=Flags.get_CF();eaa=ea16(switchBlock); tmp=Memory.mem_readb(eaa);tmp=(tmp<<switchBlock.value)|(tmp>>>(9-switchBlock.value));if (cf) tmp|=(1<<(switchBlock.value-1));Memory.mem_writeb(eaa, tmp);reg_eip += switchBlock.eipCount;continue;
                            case Inst.RCLB_32: eaa=ea32(switchBlock);Memory.mem_writeb(eaa, Instructions.do_RCLB(switchBlock.value, Memory.mem_readb(eaa)));reg_eip += switchBlock.eipCount;continue;
                            case Inst.RCLB_32_fast: cf=Flags.get_CF();eaa=ea32(switchBlock); tmp=Memory.mem_readb(eaa);tmp=(tmp<<switchBlock.value)|(tmp>>>(9-switchBlock.value));if (cf) tmp|=(1<<(switchBlock.value-1));Memory.mem_writeb(eaa, tmp);reg_eip += switchBlock.eipCount;continue;
                            case Inst.RCLB_32_sib: eaa=ea32sib(switchBlock);Memory.mem_writeb(eaa, Instructions.do_RCLB(switchBlock.value, Memory.mem_readb(eaa)));reg_eip += switchBlock.eipCount;continue;
                            case Inst.RCLB_32_sib_fast: cf=Flags.get_CF();eaa=ea32sib(switchBlock); tmp=Memory.mem_readb(eaa);tmp=(tmp<<switchBlock.value)|(tmp>>>(9-switchBlock.value));if (cf) tmp|=(1<<(switchBlock.value-1));Memory.mem_writeb(eaa, tmp);reg_eip += switchBlock.eipCount;continue;

                            // (Eb >>> Ib) | (cf << (8-Ib)) | (Eb << (9-Ib))
                            case Inst.RCRB: switchBlock.r1.set8(Instructions.do_RCRB(switchBlock.value, switchBlock.r1.get8()));reg_eip += switchBlock.eipCount;continue;
                            case Inst.RCRB_fast: cf=Flags.get_CF();tmp=switchBlock.r1.get8();tmp=(tmp>>>switchBlock.value)|(tmp<<(9-switchBlock.value));if (cf) tmp|=(1<<(8-switchBlock.value)); switchBlock.r1.set8(tmp);reg_eip += switchBlock.eipCount;continue;
                            case Inst.RCRB_16: eaa=ea16(switchBlock);Memory.mem_writeb(eaa, Instructions.do_RCRB(switchBlock.value, Memory.mem_readb(eaa)));reg_eip += switchBlock.eipCount;continue;
                            case Inst.RCRB_16_fast: cf=Flags.get_CF();eaa=ea16(switchBlock); tmp=Memory.mem_readb(eaa);tmp=(tmp>>>switchBlock.value)|(tmp<<(9-switchBlock.value));if (cf) tmp|=(1<<(8-switchBlock.value));Memory.mem_writeb(eaa, tmp);reg_eip += switchBlock.eipCount;continue;
                            case Inst.RCRB_32: eaa=ea32(switchBlock);Memory.mem_writeb(eaa, Instructions.do_RCRB(switchBlock.value, Memory.mem_readb(eaa)));reg_eip += switchBlock.eipCount;continue;
                            case Inst.RCRB_32_fast: cf=Flags.get_CF();eaa=ea32(switchBlock); tmp=Memory.mem_readb(eaa);tmp=(tmp>>>switchBlock.value)|(tmp<<(9-switchBlock.value));if (cf) tmp|=(1<<(8-switchBlock.value));Memory.mem_writeb(eaa, tmp);reg_eip += switchBlock.eipCount;continue;
                            case Inst.RCRB_32_sib: eaa=ea32sib(switchBlock);Memory.mem_writeb(eaa, Instructions.do_RCRB(switchBlock.value, Memory.mem_readb(eaa)));reg_eip += switchBlock.eipCount;continue;
                            case Inst.RCRB_32_sib_fast: cf=Flags.get_CF();eaa=ea32sib(switchBlock); tmp=Memory.mem_readb(eaa);tmp=(tmp>>>switchBlock.value)|(tmp<<(9-switchBlock.value));if (cf) tmp|=(1<<(8-switchBlock.value));Memory.mem_writeb(eaa, tmp);reg_eip += switchBlock.eipCount;continue;

                            // Eb << Ib
                            case Inst.SHLB: switchBlock.r1.set8(Instructions.do_SHLB(switchBlock.value, switchBlock.r1.get8()));reg_eip += switchBlock.eipCount;continue;
                            case Inst.SHLB_fast: switchBlock.r1.set8(switchBlock.r1.get8()<<switchBlock.value);reg_eip += switchBlock.eipCount;continue;
                            case Inst.SHLB_16: eaa=ea16(switchBlock);Memory.mem_writeb(eaa, Instructions.do_SHLB(switchBlock.value, Memory.mem_readb(eaa)));reg_eip += switchBlock.eipCount;continue;
                            case Inst.SHLB_16_fast: eaa=ea16(switchBlock); Memory.mem_writeb(eaa, Memory.mem_readb(eaa)<<switchBlock.value);reg_eip += switchBlock.eipCount;continue;
                            case Inst.SHLB_32: eaa=ea32(switchBlock);Memory.mem_writeb(eaa, Instructions.do_SHLB(switchBlock.value, Memory.mem_readb(eaa)));reg_eip += switchBlock.eipCount;continue;
                            case Inst.SHLB_32_fast: eaa=ea32(switchBlock); Memory.mem_writeb(eaa, Memory.mem_readb(eaa)<<switchBlock.value);reg_eip += switchBlock.eipCount;continue;
                            case Inst.SHLB_32_sib: eaa=ea32sib(switchBlock);Memory.mem_writeb(eaa, Instructions.do_SHLB(switchBlock.value, Memory.mem_readb(eaa)));reg_eip += switchBlock.eipCount;continue;
                            case Inst.SHLB_32_sib_fast: eaa=ea32sib(switchBlock); Memory.mem_writeb(eaa, Memory.mem_readb(eaa)<<switchBlock.value);reg_eip += switchBlock.eipCount;continue;

                            // Eb >>> Ib
                            case Inst.SHRB: switchBlock.r1.set8(Instructions.do_SHRB(switchBlock.value, switchBlock.r1.get8()));reg_eip += switchBlock.eipCount;continue;
                            case Inst.SHRB_fast: switchBlock.r1.set8(switchBlock.r1.get8()>>>switchBlock.value);reg_eip += switchBlock.eipCount;continue;
                            case Inst.SHRB_16: eaa=ea16(switchBlock);Memory.mem_writeb(eaa, Instructions.do_SHRB(switchBlock.value, Memory.mem_readb(eaa)));reg_eip += switchBlock.eipCount;continue;
                            case Inst.SHRB_16_fast: eaa=ea16(switchBlock); Memory.mem_writeb(eaa, Memory.mem_readb(eaa)>>>switchBlock.value);reg_eip += switchBlock.eipCount;continue;
                            case Inst.SHRB_32: eaa=ea32(switchBlock);Memory.mem_writeb(eaa, Instructions.do_SHRB(switchBlock.value, Memory.mem_readb(eaa)));reg_eip += switchBlock.eipCount;continue;
                            case Inst.SHRB_32_fast: eaa=ea32(switchBlock); Memory.mem_writeb(eaa, Memory.mem_readb(eaa)>>>switchBlock.value);reg_eip += switchBlock.eipCount;continue;
                            case Inst.SHRB_32_sib: eaa=ea32sib(switchBlock);Memory.mem_writeb(eaa, Instructions.do_SHRB(switchBlock.value, Memory.mem_readb(eaa)));reg_eip += switchBlock.eipCount;continue;
                            case Inst.SHRB_32_sib_fast: eaa=ea32sib(switchBlock); Memory.mem_writeb(eaa, Memory.mem_readb(eaa)>>>switchBlock.value);reg_eip += switchBlock.eipCount;continue;

                            // Eb >> Ib
                            case Inst.SARB: switchBlock.r1.set8(Instructions.do_SARB(switchBlock.value, switchBlock.r1.get8()));reg_eip += switchBlock.eipCount;continue;
                            case Inst.SARB_fast: switchBlock.r1.set8(((byte)switchBlock.r1.get8())>>switchBlock.value);reg_eip += switchBlock.eipCount;continue;
                            case Inst.SARB_16: eaa=ea16(switchBlock);Memory.mem_writeb(eaa, Instructions.do_SARB(switchBlock.value, Memory.mem_readb(eaa)));reg_eip += switchBlock.eipCount;continue;
                            case Inst.SARB_16_fast: eaa=ea16(switchBlock); Memory.mem_writeb(eaa, ((byte)Memory.mem_readb(eaa))>>switchBlock.value);reg_eip += switchBlock.eipCount;continue;
                            case Inst.SARB_32: eaa=ea32(switchBlock);Memory.mem_writeb(eaa, Instructions.do_SARB(switchBlock.value, Memory.mem_readb(eaa)));reg_eip += switchBlock.eipCount;continue;
                            case Inst.SARB_32_fast: eaa=ea32(switchBlock); Memory.mem_writeb(eaa, ((byte)Memory.mem_readb(eaa))>>switchBlock.value);reg_eip += switchBlock.eipCount;continue;
                            case Inst.SARB_32_sib: eaa=ea32sib(switchBlock);Memory.mem_writeb(eaa, Instructions.do_SARB(switchBlock.value, Memory.mem_readb(eaa)));reg_eip += switchBlock.eipCount;continue;
                            case Inst.SARB_32_sib_fast: eaa=ea32sib(switchBlock); Memory.mem_writeb(eaa, ((byte)Memory.mem_readb(eaa))>>switchBlock.value);reg_eip += switchBlock.eipCount;continue;

                            case Inst.ROLW_0_flags: {
                                int value = switchBlock.r1.word();
                                FillFlagsNoCFOF();
                                CPU_Regs.SETFLAGBIT(CPU_Regs.CF, (value & 1) != 0);
                                CPU_Regs.SETFLAGBIT(CPU_Regs.OF, ((value & 1) ^ (value >>> 7)) != 0);
                                reg_eip += switchBlock.eipCount;
                                continue;
                            }
                            case Inst.ROLW_0_flags_16: {
                                int value = Memory.mem_readw(ea16(switchBlock));
                                FillFlagsNoCFOF();
                                CPU_Regs.SETFLAGBIT(CPU_Regs.CF, (value & 1) != 0);
                                CPU_Regs.SETFLAGBIT(CPU_Regs.OF, ((value & 1) ^ (value >>> 7)) != 0);
                                reg_eip += switchBlock.eipCount;
                                continue;
                            }
                            case Inst.ROLW_0_flags_32: {
                                int value = Memory.mem_readw(ea32(switchBlock));
                                FillFlagsNoCFOF();
                                CPU_Regs.SETFLAGBIT(CPU_Regs.CF, (value & 1) != 0);
                                CPU_Regs.SETFLAGBIT(CPU_Regs.OF, ((value & 1) ^ (value >>> 7)) != 0);
                                reg_eip += switchBlock.eipCount;
                                continue;
                            }
                            case Inst.ROLW_0_flags_32_sib: {
                                int value = Memory.mem_readw(ea32sib(switchBlock));
                                FillFlagsNoCFOF();
                                CPU_Regs.SETFLAGBIT(CPU_Regs.CF, (value & 1) != 0);
                                CPU_Regs.SETFLAGBIT(CPU_Regs.OF, ((value & 1) ^ (value >>> 7)) != 0);
                                reg_eip += switchBlock.eipCount;
                                continue;
                            }
                            // (Ew << Ib) | (Ew >>> (16-Ib)
                            case Inst.ROLW: switchBlock.r1.word(Instructions.do_ROLW(switchBlock.value, switchBlock.r1.word()));reg_eip += switchBlock.eipCount;continue;
                            case Inst.ROLW_fast: tmp=switchBlock.r1.word();switchBlock.r1.word((tmp << switchBlock.value) | (tmp >>> (16-switchBlock.value)));reg_eip += switchBlock.eipCount;continue;
                            case Inst.ROLW_16: eaa=ea16(switchBlock);Memory.mem_writew(eaa, Instructions.do_ROLW(switchBlock.value, Memory.mem_readw(eaa)));reg_eip += switchBlock.eipCount;continue;
                            case Inst.ROLW_16_fast: eaa=ea16(switchBlock); tmp=Memory.mem_readw(eaa);Memory.mem_writew(eaa, (tmp << switchBlock.value) | (tmp >>> (16-switchBlock.value)));reg_eip += switchBlock.eipCount;continue;
                            case Inst.ROLW_32: eaa=ea32(switchBlock);Memory.mem_writew(eaa, Instructions.do_ROLW(switchBlock.value, Memory.mem_readw(eaa)));reg_eip += switchBlock.eipCount;continue;
                            case Inst.ROLW_32_fast: eaa=ea32(switchBlock); tmp=Memory.mem_readw(eaa);Memory.mem_writew(eaa, (tmp << switchBlock.value) | (tmp >>> (16-switchBlock.value)));reg_eip += switchBlock.eipCount;continue;
                            case Inst.ROLW_32_sib: eaa=ea32sib(switchBlock);Memory.mem_writew(eaa, Instructions.do_ROLW(switchBlock.value, Memory.mem_readw(eaa)));reg_eip += switchBlock.eipCount;continue;
                            case Inst.ROLW_32_sib_fast: eaa=ea32sib(switchBlock); tmp=Memory.mem_readw(eaa);Memory.mem_writew(eaa, (tmp << switchBlock.value) | (tmp >>> (16-switchBlock.value)));reg_eip += switchBlock.eipCount;continue;

                            case Inst.RORW_0_flags: {
                                int value = switchBlock.r1.word();
                                FillFlagsNoCFOF();
                                CPU_Regs.SETFLAGBIT(CPU_Regs.CF,(value>>7)!=0);
                                CPU_Regs.SETFLAGBIT(CPU_Regs.OF,((value>>7) ^ ((value>>>6) & 1))!=0);
                                reg_eip += switchBlock.eipCount;
                                continue;
                            }
                            case Inst.RORW_0_flags_16: {
                                int value = Memory.mem_readw(ea16(switchBlock));
                                FillFlagsNoCFOF();
                                CPU_Regs.SETFLAGBIT(CPU_Regs.CF,(value>>7)!=0);
                                CPU_Regs.SETFLAGBIT(CPU_Regs.OF,((value>>7) ^ ((value>>>6) & 1))!=0);
                                reg_eip += switchBlock.eipCount;
                                continue;
                            }
                            case Inst.RORW_0_flags_32: {
                                int value = Memory.mem_readw(ea32(switchBlock));
                                FillFlagsNoCFOF();
                                CPU_Regs.SETFLAGBIT(CPU_Regs.CF,(value>>7)!=0);
                                CPU_Regs.SETFLAGBIT(CPU_Regs.OF,((value>>7) ^ ((value>>>6) & 1))!=0);
                                reg_eip += switchBlock.eipCount;
                                continue;
                            }
                            case Inst.RORW_0_flags_32_sib: {
                                int value = Memory.mem_readw(ea32sib(switchBlock));
                                FillFlagsNoCFOF();
                                CPU_Regs.SETFLAGBIT(CPU_Regs.CF,(value>>7)!=0);
                                CPU_Regs.SETFLAGBIT(CPU_Regs.OF,((value>>7) ^ ((value>>>6) & 1))!=0);
                                reg_eip += switchBlock.eipCount;
                                continue;
                            }
                            // (Ew >>> Ib) | (Ew << (16-Ib))
                            case Inst.RORW: switchBlock.r1.word(Instructions.do_RORW(switchBlock.value, switchBlock.r1.word()));reg_eip += switchBlock.eipCount;continue;
                            case Inst.RORW_fast: tmp=switchBlock.r1.word();switchBlock.r1.word((tmp >>> switchBlock.value) | (tmp << (16-switchBlock.value)));reg_eip += switchBlock.eipCount;continue;
                            case Inst.RORW_16: eaa=ea16(switchBlock);Memory.mem_writew(eaa, Instructions.do_RORW(switchBlock.value, Memory.mem_readw(eaa)));reg_eip += switchBlock.eipCount;continue;
                            case Inst.RORW_16_fast: eaa=ea16(switchBlock); tmp=Memory.mem_readw(eaa);Memory.mem_writew(eaa, (tmp >>> switchBlock.value) | (tmp << (16-switchBlock.value)));reg_eip += switchBlock.eipCount;continue;
                            case Inst.RORW_32: eaa=ea32(switchBlock);Memory.mem_writew(eaa, Instructions.do_RORW(switchBlock.value, Memory.mem_readw(eaa)));reg_eip += switchBlock.eipCount;continue;
                            case Inst.RORW_32_fast: eaa=ea32(switchBlock); tmp=Memory.mem_readw(eaa);Memory.mem_writew(eaa, (tmp >>> switchBlock.value) | (tmp << (16-switchBlock.value)));reg_eip += switchBlock.eipCount;continue;
                            case Inst.RORW_32_sib: eaa=ea32sib(switchBlock);Memory.mem_writew(eaa, Instructions.do_RORW(switchBlock.value, Memory.mem_readw(eaa)));reg_eip += switchBlock.eipCount;continue;
                            case Inst.RORW_32_sib_fast: eaa=ea32sib(switchBlock); tmp=Memory.mem_readw(eaa);Memory.mem_writew(eaa, (tmp >>> switchBlock.value) | (tmp << (16-switchBlock.value)));reg_eip += switchBlock.eipCount;continue;

                                // (Ew << Ib) |(cf << (Ib-1)) | (Ew >>> (17-Ib));
                            case Inst.RCLW: switchBlock.r1.word(Instructions.do_RCLW(switchBlock.value, switchBlock.r1.word()));reg_eip += switchBlock.eipCount;continue;
                            case Inst.RCLW_fast: cf=Flags.get_CF();tmp=switchBlock.r1.word();tmp=(tmp<<switchBlock.value)|(tmp>>>(17-switchBlock.value));if (cf) tmp|=(1<<(switchBlock.value-1)); switchBlock.r1.word(tmp);reg_eip += switchBlock.eipCount;continue;
                            case Inst.RCLW_16: eaa=ea16(switchBlock);Memory.mem_writew(eaa, Instructions.do_RCLW(switchBlock.value, Memory.mem_readw(eaa)));reg_eip += switchBlock.eipCount;continue;
                            case Inst.RCLW_16_fast: cf=Flags.get_CF();eaa=ea16(switchBlock); tmp=Memory.mem_readw(eaa);tmp=(tmp<<switchBlock.value)|(tmp>>>(17-switchBlock.value));if (cf) tmp|=(1<<(switchBlock.value-1));Memory.mem_writew(eaa, tmp);reg_eip += switchBlock.eipCount;continue;
                            case Inst.RCLW_32: eaa=ea32(switchBlock);Memory.mem_writew(eaa, Instructions.do_RCLW(switchBlock.value, Memory.mem_readw(eaa)));reg_eip += switchBlock.eipCount;continue;
                            case Inst.RCLW_32_fast: cf=Flags.get_CF();eaa=ea32(switchBlock); tmp=Memory.mem_readw(eaa);tmp=(tmp<<switchBlock.value)|(tmp>>>(17-switchBlock.value));if (cf) tmp|=(1<<(switchBlock.value-1));Memory.mem_writew(eaa, tmp);reg_eip += switchBlock.eipCount;continue;
                            case Inst.RCLW_32_sib: eaa=ea32sib(switchBlock);Memory.mem_writew(eaa, Instructions.do_RCLW(switchBlock.value, Memory.mem_readw(eaa)));reg_eip += switchBlock.eipCount;continue;
                            case Inst.RCLW_32_sib_fast: cf=Flags.get_CF();eaa=ea32sib(switchBlock); tmp=Memory.mem_readw(eaa);tmp=(tmp<<switchBlock.value)|(tmp>>>(17-switchBlock.value));if (cf) tmp|=(1<<(switchBlock.value-1));Memory.mem_writew(eaa, tmp);reg_eip += switchBlock.eipCount;continue;

                                // (Ew >>> Ib) | (cf << (16-Ib)) | (Ew << (17-Ib))
                            case Inst.RCRW: switchBlock.r1.word(Instructions.do_RCRW(switchBlock.value, switchBlock.r1.word()));reg_eip += switchBlock.eipCount;continue;
                            case Inst.RCRW_fast: cf=Flags.get_CF();tmp=switchBlock.r1.word();tmp=(tmp>>>switchBlock.value)|(tmp<<(17-switchBlock.value));if (cf) tmp|=(1<<(16-switchBlock.value)); switchBlock.r1.word(tmp);reg_eip += switchBlock.eipCount;continue;
                            case Inst.RCRW_16: eaa=ea16(switchBlock);Memory.mem_writew(eaa, Instructions.do_RCRW(switchBlock.value, Memory.mem_readw(eaa)));reg_eip += switchBlock.eipCount;continue;
                            case Inst.RCRW_16_fast: cf=Flags.get_CF();eaa=ea16(switchBlock); tmp=Memory.mem_readw(eaa);tmp=(tmp>>>switchBlock.value)|(tmp<<(17-switchBlock.value));if (cf) tmp|=(1<<(16-switchBlock.value));Memory.mem_writew(eaa, tmp);reg_eip += switchBlock.eipCount;continue;
                            case Inst.RCRW_32: eaa=ea32(switchBlock);Memory.mem_writew(eaa, Instructions.do_RCRW(switchBlock.value, Memory.mem_readw(eaa)));reg_eip += switchBlock.eipCount;continue;
                            case Inst.RCRW_32_fast: cf=Flags.get_CF();eaa=ea32(switchBlock); tmp=Memory.mem_readw(eaa);tmp=(tmp>>>switchBlock.value)|(tmp<<(17-switchBlock.value));if (cf) tmp|=(1<<(16-switchBlock.value));Memory.mem_writew(eaa, tmp);reg_eip += switchBlock.eipCount;continue;
                            case Inst.RCRW_32_sib: eaa=ea32sib(switchBlock);Memory.mem_writew(eaa, Instructions.do_RCRW(switchBlock.value, Memory.mem_readw(eaa)));reg_eip += switchBlock.eipCount;continue;
                            case Inst.RCRW_32_sib_fast: cf=Flags.get_CF();eaa=ea32sib(switchBlock); tmp=Memory.mem_readw(eaa);tmp=(tmp>>>switchBlock.value)|(tmp<<(17-switchBlock.value));if (cf) tmp|=(1<<(16-switchBlock.value));Memory.mem_writew(eaa, tmp);reg_eip += switchBlock.eipCount;continue;

                                // Ew << Ib
                            case Inst.SHLW: switchBlock.r1.word(Instructions.do_SHLW(switchBlock.value, switchBlock.r1.word()));reg_eip += switchBlock.eipCount;continue;
                            case Inst.SHLW_fast: switchBlock.r1.word(switchBlock.r1.word()<<switchBlock.value);reg_eip += switchBlock.eipCount;continue;
                            case Inst.SHLW_16: eaa=ea16(switchBlock);Memory.mem_writew(eaa, Instructions.do_SHLW(switchBlock.value, Memory.mem_readw(eaa)));reg_eip += switchBlock.eipCount;continue;
                            case Inst.SHLW_16_fast: eaa=ea16(switchBlock); Memory.mem_writew(eaa, Memory.mem_readw(eaa)<<switchBlock.value);reg_eip += switchBlock.eipCount;continue;
                            case Inst.SHLW_32: eaa=ea32(switchBlock);Memory.mem_writew(eaa, Instructions.do_SHLW(switchBlock.value, Memory.mem_readw(eaa)));reg_eip += switchBlock.eipCount;continue;
                            case Inst.SHLW_32_fast: eaa=ea32(switchBlock); Memory.mem_writew(eaa, Memory.mem_readw(eaa)<<switchBlock.value);reg_eip += switchBlock.eipCount;continue;
                            case Inst.SHLW_32_sib: eaa=ea32sib(switchBlock);Memory.mem_writew(eaa, Instructions.do_SHLW(switchBlock.value, Memory.mem_readw(eaa)));reg_eip += switchBlock.eipCount;continue;
                            case Inst.SHLW_32_sib_fast: eaa=ea32sib(switchBlock); Memory.mem_writew(eaa, Memory.mem_readw(eaa)<<switchBlock.value);reg_eip += switchBlock.eipCount;continue;

                                // Ew >>> Ib
                            case Inst.SHRW: switchBlock.r1.word(Instructions.do_SHRW(switchBlock.value, switchBlock.r1.word()));reg_eip += switchBlock.eipCount;continue;
                            case Inst.SHRW_fast: switchBlock.r1.word(switchBlock.r1.word()>>>switchBlock.value);reg_eip += switchBlock.eipCount;continue;
                            case Inst.SHRW_16: eaa=ea16(switchBlock);Memory.mem_writew(eaa, Instructions.do_SHRW(switchBlock.value, Memory.mem_readw(eaa)));reg_eip += switchBlock.eipCount;continue;
                            case Inst.SHRW_16_fast: eaa=ea16(switchBlock); Memory.mem_writew(eaa, Memory.mem_readw(eaa)>>>switchBlock.value);reg_eip += switchBlock.eipCount;continue;
                            case Inst.SHRW_32: eaa=ea32(switchBlock);Memory.mem_writew(eaa, Instructions.do_SHRW(switchBlock.value, Memory.mem_readw(eaa)));reg_eip += switchBlock.eipCount;continue;
                            case Inst.SHRW_32_fast: eaa=ea32(switchBlock); Memory.mem_writew(eaa, Memory.mem_readw(eaa)>>>switchBlock.value);reg_eip += switchBlock.eipCount;continue;
                            case Inst.SHRW_32_sib: eaa=ea32sib(switchBlock);Memory.mem_writew(eaa, Instructions.do_SHRW(switchBlock.value, Memory.mem_readw(eaa)));reg_eip += switchBlock.eipCount;continue;
                            case Inst.SHRW_32_sib_fast: eaa=ea32sib(switchBlock); Memory.mem_writew(eaa, Memory.mem_readw(eaa)>>>switchBlock.value);reg_eip += switchBlock.eipCount;continue;

                                // Ew >> Ib
                            case Inst.SARW: switchBlock.r1.word(Instructions.do_SARW(switchBlock.value, switchBlock.r1.word()));reg_eip += switchBlock.eipCount;continue;
                            case Inst.SARW_fast: switchBlock.r1.word(((short)switchBlock.r1.word())>>switchBlock.value);reg_eip += switchBlock.eipCount;continue;
                            case Inst.SARW_16: eaa=ea16(switchBlock);Memory.mem_writew(eaa, Instructions.do_SARW(switchBlock.value, Memory.mem_readw(eaa)));reg_eip += switchBlock.eipCount;continue;
                            case Inst.SARW_16_fast: eaa=ea16(switchBlock); Memory.mem_writew(eaa, ((short)Memory.mem_readw(eaa))>>switchBlock.value);reg_eip += switchBlock.eipCount;continue;
                            case Inst.SARW_32: eaa=ea32(switchBlock);Memory.mem_writew(eaa, Instructions.do_SARW(switchBlock.value, Memory.mem_readw(eaa)));reg_eip += switchBlock.eipCount;continue;
                            case Inst.SARW_32_fast: eaa=ea32(switchBlock); Memory.mem_writew(eaa, ((short)Memory.mem_readw(eaa))>>switchBlock.value);reg_eip += switchBlock.eipCount;continue;
                            case Inst.SARW_32_sib: eaa=ea32sib(switchBlock);Memory.mem_writew(eaa, Instructions.do_SARW(switchBlock.value, Memory.mem_readw(eaa)));reg_eip += switchBlock.eipCount;continue;
                            case Inst.SARW_32_sib_fast: eaa=ea32sib(switchBlock); Memory.mem_writew(eaa, ((short)Memory.mem_readw(eaa))>>switchBlock.value);reg_eip += switchBlock.eipCount;continue;
                            case Inst.RETN16_Iw: reg_eip=CPU.CPU_Pop16(); reg_esp.dword+=switchBlock.value; block=null; break;
                            case Inst.RETN16: reg_eip=CPU.CPU_Pop16(); block=null; break;

                            case Inst.LES16_16: eaa=ea16(switchBlock);tmp=Memory.mem_readw(eaa);if (CPU.CPU_SetSegGeneralES(Memory.mem_readw(eaa+2))) {RUNEXCEPTION(); block=null; break;} switchBlock.r1.word(tmp);reg_eip += switchBlock.eipCount;continue;
                            case Inst.LES16_32: eaa=ea32(switchBlock);tmp=Memory.mem_readw(eaa);if (CPU.CPU_SetSegGeneralES(Memory.mem_readw(eaa+2))) {RUNEXCEPTION(); block=null; break;} switchBlock.r1.word(tmp);reg_eip += switchBlock.eipCount;continue;
                            case Inst.LES16_32_sib: eaa=ea32sib(switchBlock);tmp=Memory.mem_readw(eaa);if (CPU.CPU_SetSegGeneralES(Memory.mem_readw(eaa+2))) {RUNEXCEPTION(); block=null; break;} switchBlock.r1.word(tmp);reg_eip += switchBlock.eipCount;continue;
                            case Inst.LES32_16: eaa=ea16(switchBlock);tmp=Memory.mem_readd(eaa);if (CPU.CPU_SetSegGeneralES(Memory.mem_readw(eaa+2))) {RUNEXCEPTION(); block=null; break;} switchBlock.r1.dword=tmp;reg_eip += switchBlock.eipCount;continue;
                            case Inst.LES32_32: eaa=ea32(switchBlock);tmp=Memory.mem_readd(eaa);if (CPU.CPU_SetSegGeneralES(Memory.mem_readw(eaa+2))) {RUNEXCEPTION(); block=null; break;} switchBlock.r1.dword=tmp;reg_eip += switchBlock.eipCount;continue;
                            case Inst.LES32_32_sib: eaa=ea32sib(switchBlock);tmp=Memory.mem_readd(eaa);if (CPU.CPU_SetSegGeneralES(Memory.mem_readw(eaa+2))) {RUNEXCEPTION(); block=null; break;} switchBlock.r1.dword=tmp;reg_eip += switchBlock.eipCount;continue;

                            case Inst.LDS16_16: eaa=ea16(switchBlock);tmp=Memory.mem_readw(eaa);if (CPU.CPU_SetSegGeneralDS(Memory.mem_readw(eaa + 2))) {RUNEXCEPTION(); block=null; break;} switchBlock.r1.word(tmp);reg_eip += switchBlock.eipCount;continue;
                            case Inst.LDS16_32: eaa=ea32(switchBlock);tmp=Memory.mem_readw(eaa);if (CPU.CPU_SetSegGeneralDS(Memory.mem_readw(eaa + 2))) {RUNEXCEPTION(); block=null; break;} switchBlock.r1.word(tmp);reg_eip += switchBlock.eipCount;continue;
                            case Inst.LDS16_32_sib: eaa=ea32sib(switchBlock);tmp=Memory.mem_readw(eaa);if (CPU.CPU_SetSegGeneralDS(Memory.mem_readw(eaa + 2))) {RUNEXCEPTION(); block=null; break;} switchBlock.r1.word(tmp);reg_eip += switchBlock.eipCount;continue;
                            case Inst.LDS32_16: eaa=ea16(switchBlock);tmp=Memory.mem_readd(eaa);if (CPU.CPU_SetSegGeneralDS(Memory.mem_readw(eaa + 2))) {RUNEXCEPTION(); block=null; break;} switchBlock.r1.dword=tmp;reg_eip += switchBlock.eipCount;continue;
                            case Inst.LDS32_32: eaa=ea32(switchBlock);tmp=Memory.mem_readd(eaa);if (CPU.CPU_SetSegGeneralDS(Memory.mem_readw(eaa + 2))) {RUNEXCEPTION(); block=null; break;} switchBlock.r1.dword=tmp;reg_eip += switchBlock.eipCount;continue;
                            case Inst.LDS32_32_sib: eaa=ea32sib(switchBlock);tmp=Memory.mem_readd(eaa);if (CPU.CPU_SetSegGeneralDS(Memory.mem_readw(eaa + 2))) {RUNEXCEPTION(); block=null; break;} switchBlock.r1.dword=tmp;reg_eip += switchBlock.eipCount;continue;
                            case Inst.ENTER16: CPU.CPU_ENTER(false,switchBlock.value,switchBlock.eaa_const);reg_eip += switchBlock.eipCount;continue;
                            case Inst.LEAVE16:reg_esp.dword&=CPU.cpu.stack.notmask;reg_esp.dword|=(reg_ebp.dword & CPU.cpu.stack.mask);reg_ebp.word(CPU.CPU_Pop16());reg_eip += switchBlock.eipCount;continue;
                            case Inst.RETF16_Iw:Flags.FillFlags();CPU.CPU_RET(false,switchBlock.value,reg_eip+switchBlock.eipCount);block=null;break;
                            case Inst.INT3:CPU.CPU_SW_Interrupt_NoIOPLCheck(3,reg_eip+switchBlock.eipCount);CPU.cpu.trap_skip=true;block=null;break;
                            case Inst.INTIb:CPU.CPU_SW_Interrupt(switchBlock.value,reg_eip+switchBlock.eipCount);CPU.cpu.trap_skip=true;block=null;break;
                            case Inst.INTO:if (Flags.get_OF()) {CPU.CPU_SW_Interrupt(4,reg_eip+switchBlock.eipCount);CPU.cpu.trap_skip=true;block=null;}break;
                            case Inst.IRET16:
                                CPU.CPU_IRET(false, reg_eip+switchBlock.eipCount);
                                if (GETFLAG(TF)!=0) {
                                    CPU.cpudecoder=CPU_Core_Switch_Trap_Run;
                                    return Callback.CBRET_NONE;
                                }
                                if (GETFLAG(IF)!=0 && Pic.PIC_IRQCheck!=0) return Callback.CBRET_NONE;
                                block=null;
                                break;

                            // (Eb << Ib) | (Eb >>> (8-Ib)
                            case Inst.ROLB_CL: tmp2=(reg_ecx.dword & 0x1f);if (Instructions.valid_ROLB(switchBlock.r1.get8(), tmp2)) switchBlock.r1.set8(Instructions.do_ROLB(tmp2, switchBlock.r1.get8()));reg_eip += switchBlock.eipCount;continue;
                            case Inst.ROLB_CL_fast: tmp2=(reg_ecx.dword & 0x7); if (tmp2!=0) {tmp=switchBlock.r1.get8();switchBlock.r1.set8((tmp << tmp2) | (tmp >>> (8-tmp2)));}reg_eip += switchBlock.eipCount;continue;
                            case Inst.ROLB_CL_16: tmp2=(reg_ecx.dword & 0x1f); eaa=ea16(switchBlock); tmp=Memory.mem_readb(eaa); if (Instructions.valid_ROLB(tmp, tmp2)) Memory.mem_writeb(eaa, Instructions.do_ROLB(tmp2, tmp));reg_eip += switchBlock.eipCount;continue;
                            case Inst.ROLB_CL_16_fast: tmp2=(reg_ecx.dword & 0x7);if (tmp2!=0) {eaa=ea16(switchBlock); tmp=Memory.mem_readb(eaa);Memory.mem_writeb(eaa, (tmp << tmp2) | (tmp >>> (8-tmp2)));}reg_eip += switchBlock.eipCount;continue;
                            case Inst.ROLB_CL_32: tmp2=(reg_ecx.dword & 0x1f); eaa=ea32(switchBlock); tmp=Memory.mem_readb(eaa); if (Instructions.valid_ROLB(tmp, tmp2)) Memory.mem_writeb(eaa, Instructions.do_ROLB(tmp2, tmp));reg_eip += switchBlock.eipCount;continue;
                            case Inst.ROLB_CL_32_fast: tmp2=(reg_ecx.dword & 0x7);if (tmp2!=0) {eaa=ea32(switchBlock); tmp=Memory.mem_readb(eaa);Memory.mem_writeb(eaa, (tmp << tmp2) | (tmp >>> (8-tmp2)));}reg_eip += switchBlock.eipCount;continue;
                            case Inst.ROLB_CL_32_sib: tmp2=(reg_ecx.dword & 0x1f); eaa=ea32sib(switchBlock); tmp=Memory.mem_readb(eaa); if (Instructions.valid_ROLB(tmp, tmp2)) Memory.mem_writeb(eaa, Instructions.do_ROLB(tmp2, tmp));reg_eip += switchBlock.eipCount;continue;
                            case Inst.ROLB_CL_32_sib_fast: tmp2=(reg_ecx.dword & 0x7);if (tmp2!=0) {eaa=ea32sib(switchBlock); tmp=Memory.mem_readb(eaa);Memory.mem_writeb(eaa, (tmp << tmp2) | (tmp >>> (8-tmp2)));}reg_eip += switchBlock.eipCount;continue;

                            // (Eb >>> Ib) | (Eb << (8-Ib))
                            case Inst.RORB_CL: tmp2=(reg_ecx.dword & 0x1f);if (Instructions.valid_RORB(switchBlock.r1.get8(), tmp2)) switchBlock.r1.set8(Instructions.do_RORB(tmp2, switchBlock.r1.get8()));reg_eip += switchBlock.eipCount;continue;
                            case Inst.RORB_CL_fast: tmp2=(reg_ecx.dword & 0x7); if (tmp2!=0){tmp=switchBlock.r1.get8();switchBlock.r1.set8((tmp >>> tmp2) | (tmp << (8-tmp2)));}reg_eip += switchBlock.eipCount;continue;
                            case Inst.RORB_CL_16: tmp2=(reg_ecx.dword & 0x1f); eaa=ea16(switchBlock); tmp=Memory.mem_readb(eaa); if (Instructions.valid_RORB(tmp, tmp2)) Memory.mem_writeb(eaa, Instructions.do_RORB(tmp2, tmp));reg_eip += switchBlock.eipCount;continue;
                            case Inst.RORB_CL_16_fast: tmp2=(reg_ecx.dword & 0x7);if (tmp2!=0) {eaa=ea16(switchBlock); tmp=Memory.mem_readb(eaa);Memory.mem_writeb(eaa, (tmp >>> tmp2) | (tmp << (8-tmp2)));}reg_eip += switchBlock.eipCount;continue;
                            case Inst.RORB_CL_32: tmp2=(reg_ecx.dword & 0x1f); eaa=ea32(switchBlock); tmp=Memory.mem_readb(eaa); if (Instructions.valid_RORB(tmp, tmp2)) Memory.mem_writeb(eaa, Instructions.do_RORB(tmp2, tmp));reg_eip += switchBlock.eipCount;continue;
                            case Inst.RORB_CL_32_fast: tmp2=(reg_ecx.dword & 0x7);if (tmp2!=0) {eaa=ea32(switchBlock); tmp=Memory.mem_readb(eaa);Memory.mem_writeb(eaa, (tmp >>> tmp2) | (tmp << (8-tmp2)));}reg_eip += switchBlock.eipCount;continue;
                            case Inst.RORB_CL_32_sib: tmp2=(reg_ecx.dword & 0x1f); eaa=ea32sib(switchBlock); tmp=Memory.mem_readb(eaa); if (Instructions.valid_RORB(tmp, tmp2)) Memory.mem_writeb(eaa, Instructions.do_RORB(tmp2, tmp));reg_eip += switchBlock.eipCount;continue;
                            case Inst.RORB_CL_32_sib_fast: tmp2=(reg_ecx.dword & 0x7);if (tmp2!=0) {eaa=ea32sib(switchBlock); tmp=Memory.mem_readb(eaa);Memory.mem_writeb(eaa, (tmp >>> tmp2) | (tmp << (8-tmp2)));}reg_eip += switchBlock.eipCount;continue;

                                // (Eb << Ib) |(cf << (Ib-1)) | (Eb >>> (9-Ib));
                            case Inst.RCLB_CL: tmp2=(reg_ecx.dword & 0x1f);if (Instructions.valid_RCLB(tmp2)) switchBlock.r1.set8(Instructions.do_RCLB(tmp2, switchBlock.r1.get8()));reg_eip += switchBlock.eipCount;continue;
                            case Inst.RCLB_CL_fast: tmp2=(reg_ecx.dword % 9);if (tmp2!=0) {cf=Flags.get_CF();tmp=switchBlock.r1.get8();tmp=(tmp<<tmp2)|(tmp>>>(9-tmp2));if (cf) tmp|=(1<<(tmp2-1)); switchBlock.r1.set8(tmp);}reg_eip += switchBlock.eipCount;continue;
                            case Inst.RCLB_CL_16: tmp2=(reg_ecx.dword & 0x1f);if (Instructions.valid_RCLB(tmp2)) {eaa=ea16(switchBlock);Memory.mem_writeb(eaa, Instructions.do_RCLB(tmp2, Memory.mem_readb(eaa)));}reg_eip += switchBlock.eipCount;continue;
                            case Inst.RCLB_CL_16_fast: tmp2=(reg_ecx.dword % 9);if (tmp2!=0) {cf=Flags.get_CF();eaa=ea16(switchBlock); tmp=Memory.mem_readb(eaa);tmp=(tmp<<tmp2)|(tmp>>>(9-tmp2));if (cf) tmp|=(1<<(tmp2-1));Memory.mem_writeb(eaa, tmp);}reg_eip += switchBlock.eipCount;continue;
                            case Inst.RCLB_CL_32: tmp2=(reg_ecx.dword & 0x1f);if (Instructions.valid_RCLB(tmp2)) {eaa=ea32(switchBlock);Memory.mem_writeb(eaa, Instructions.do_RCLB(tmp2, Memory.mem_readb(eaa)));}reg_eip += switchBlock.eipCount;continue;
                            case Inst.RCLB_CL_32_fast: tmp2=(reg_ecx.dword % 9);if (tmp2!=0) {cf=Flags.get_CF();eaa=ea32(switchBlock); tmp=Memory.mem_readb(eaa);tmp=(tmp<<tmp2)|(tmp>>>(9-tmp2));if (cf) tmp|=(1<<(tmp2-1));Memory.mem_writeb(eaa, tmp);}reg_eip += switchBlock.eipCount;continue;
                            case Inst.RCLB_CL_32_sib: tmp2=(reg_ecx.dword & 0x1f);if (Instructions.valid_RCLB(tmp2)) {eaa=ea32sib(switchBlock);Memory.mem_writeb(eaa, Instructions.do_RCLB(tmp2, Memory.mem_readb(eaa)));}reg_eip += switchBlock.eipCount;continue;
                            case Inst.RCLB_CL_32_sib_fast: tmp2=(reg_ecx.dword % 9);if (tmp2!=0) {cf=Flags.get_CF();eaa=ea32sib(switchBlock); tmp=Memory.mem_readb(eaa);tmp=(tmp<<tmp2)|(tmp>>>(9-tmp2));if (cf) tmp|=(1<<(tmp2-1));Memory.mem_writeb(eaa, tmp);}reg_eip += switchBlock.eipCount;continue;

                                // (Eb >>> Ib) | (cf << (8-Ib)) | (Eb << (9-Ib))
                            case Inst.RCRB_CL: tmp2=(reg_ecx.dword & 0x1f);if (Instructions.valid_RCRB(tmp2)) switchBlock.r1.set8(Instructions.do_RCRB(tmp2, switchBlock.r1.get8()));reg_eip += switchBlock.eipCount;continue;
                            case Inst.RCRB_CL_fast: tmp2=(reg_ecx.dword % 9);if (tmp2!=0) {cf=Flags.get_CF();tmp=switchBlock.r1.get8();tmp=(tmp>>>tmp2)|(tmp<<(9-tmp2));if (cf) tmp|=(1<<(8-tmp2)); switchBlock.r1.set8(tmp);}reg_eip += switchBlock.eipCount;continue;
                            case Inst.RCRB_CL_16: tmp2=(reg_ecx.dword & 0x1f);if (Instructions.valid_RCRB(tmp2)) {eaa=ea16(switchBlock);Memory.mem_writeb(eaa, Instructions.do_RCRB(tmp2, Memory.mem_readb(eaa)));}reg_eip += switchBlock.eipCount;continue;
                            case Inst.RCRB_CL_16_fast: tmp2=(reg_ecx.dword % 9);if (tmp2!=0) {cf=Flags.get_CF();eaa=ea16(switchBlock); tmp=Memory.mem_readb(eaa);tmp=(tmp>>>tmp2)|(tmp<<(9-tmp2));if (cf) tmp|=(1<<(8-tmp2));Memory.mem_writeb(eaa, tmp);}reg_eip += switchBlock.eipCount;continue;
                            case Inst.RCRB_CL_32: tmp2=(reg_ecx.dword & 0x1f);if (Instructions.valid_RCRB(tmp2)) {eaa=ea32(switchBlock);Memory.mem_writeb(eaa, Instructions.do_RCRB(tmp2, Memory.mem_readb(eaa)));}reg_eip += switchBlock.eipCount;continue;
                            case Inst.RCRB_CL_32_fast: tmp2=(reg_ecx.dword % 9);if (tmp2!=0) {cf=Flags.get_CF();eaa=ea32(switchBlock); tmp=Memory.mem_readb(eaa);tmp=(tmp>>>tmp2)|(tmp<<(9-tmp2));if (cf) tmp|=(1<<(8-tmp2));Memory.mem_writeb(eaa, tmp);}reg_eip += switchBlock.eipCount;continue;
                            case Inst.RCRB_CL_32_sib: tmp2=(reg_ecx.dword & 0x1f);if (Instructions.valid_RCRB(tmp2)) {eaa=ea32sib(switchBlock);Memory.mem_writeb(eaa, Instructions.do_RCRB(tmp2, Memory.mem_readb(eaa)));}reg_eip += switchBlock.eipCount;continue;
                            case Inst.RCRB_CL_32_sib_fast: tmp2=(reg_ecx.dword % 9);if (tmp2!=0) {cf=Flags.get_CF();eaa=ea32sib(switchBlock); tmp=Memory.mem_readb(eaa);tmp=(tmp>>>tmp2)|(tmp<<(9-tmp2));if (cf) tmp|=(1<<(8-tmp2));Memory.mem_writeb(eaa, tmp);}reg_eip += switchBlock.eipCount;continue;

                                // Eb << Ib
                            case Inst.SHLB_CL: tmp2=(reg_ecx.dword & 0x1f);if (Instructions.valid_SHLB(tmp2)) {switchBlock.r1.set8(Instructions.do_SHLB(tmp2, switchBlock.r1.get8()));}reg_eip += switchBlock.eipCount;continue;
                            case Inst.SHLB_CL_fast: tmp2=(reg_ecx.dword & 0x1f); if (tmp2!=0) switchBlock.r1.set8(switchBlock.r1.get8()<<tmp2);reg_eip += switchBlock.eipCount;continue;
                            case Inst.SHLB_CL_16: tmp2=(reg_ecx.dword & 0x1f);if (Instructions.valid_SHLB(tmp2)) {eaa=ea16(switchBlock);Memory.mem_writeb(eaa, Instructions.do_SHLB(tmp2, Memory.mem_readb(eaa)));}reg_eip += switchBlock.eipCount;continue;
                            case Inst.SHLB_CL_16_fast: tmp2=(reg_ecx.dword & 0x1f); if (tmp2!=0) {eaa=ea16(switchBlock); Memory.mem_writeb(eaa, Memory.mem_readb(eaa)<<tmp2);}reg_eip += switchBlock.eipCount;continue;
                            case Inst.SHLB_CL_32: tmp2=(reg_ecx.dword & 0x1f);if (Instructions.valid_SHLB(tmp2)) {eaa=ea32(switchBlock);Memory.mem_writeb(eaa, Instructions.do_SHLB(tmp2, Memory.mem_readb(eaa)));}reg_eip += switchBlock.eipCount;continue;
                            case Inst.SHLB_CL_32_fast: tmp2=(reg_ecx.dword & 0x1f); if (tmp2!=0) {eaa=ea32(switchBlock); Memory.mem_writeb(eaa, Memory.mem_readb(eaa)<<tmp2);}reg_eip += switchBlock.eipCount;continue;
                            case Inst.SHLB_CL_32_sib: tmp2=(reg_ecx.dword & 0x1f);if (Instructions.valid_SHLB(tmp2)) {eaa=ea32sib(switchBlock);Memory.mem_writeb(eaa, Instructions.do_SHLB(tmp2, Memory.mem_readb(eaa)));}reg_eip += switchBlock.eipCount;continue;
                            case Inst.SHLB_CL_32_sib_fast: tmp2=(reg_ecx.dword & 0x1f); if (tmp2!=0) {eaa=ea32sib(switchBlock); Memory.mem_writeb(eaa, Memory.mem_readb(eaa)<<tmp2);}reg_eip += switchBlock.eipCount;continue;

                                // Eb >>> Ib
                            case Inst.SHRB_CL: tmp2=(reg_ecx.dword & 0x1f);if (Instructions.valid_SHRB(tmp2)) {switchBlock.r1.set8(Instructions.do_SHRB(tmp2, switchBlock.r1.get8()));}reg_eip += switchBlock.eipCount;continue;
                            case Inst.SHRB_CL_fast: tmp2=(reg_ecx.dword & 0x1f); if (tmp2!=0) switchBlock.r1.set8(switchBlock.r1.get8()>>>tmp2);reg_eip += switchBlock.eipCount;continue;
                            case Inst.SHRB_CL_16: tmp2=(reg_ecx.dword & 0x1f);if (Instructions.valid_SHRB(tmp2)) {eaa=ea16(switchBlock);Memory.mem_writeb(eaa, Instructions.do_SHRB(tmp2, Memory.mem_readb(eaa)));}reg_eip += switchBlock.eipCount;continue;
                            case Inst.SHRB_CL_16_fast: tmp2=(reg_ecx.dword & 0x1f); if (tmp2!=0) {eaa=ea16(switchBlock); Memory.mem_writeb(eaa, Memory.mem_readb(eaa)>>>tmp2);}reg_eip += switchBlock.eipCount;continue;
                            case Inst.SHRB_CL_32: tmp2=(reg_ecx.dword & 0x1f);if (Instructions.valid_SHRB(tmp2)) {eaa=ea32(switchBlock);Memory.mem_writeb(eaa, Instructions.do_SHRB(tmp2, Memory.mem_readb(eaa)));}reg_eip += switchBlock.eipCount;continue;
                            case Inst.SHRB_CL_32_fast: tmp2=(reg_ecx.dword & 0x1f); if (tmp2!=0) {eaa=ea32(switchBlock); Memory.mem_writeb(eaa, Memory.mem_readb(eaa)>>>tmp2);}reg_eip += switchBlock.eipCount;continue;
                            case Inst.SHRB_CL_32_sib: tmp2=(reg_ecx.dword & 0x1f);if (Instructions.valid_SHRB(tmp2)) {eaa=ea32sib(switchBlock);Memory.mem_writeb(eaa, Instructions.do_SHRB(tmp2, Memory.mem_readb(eaa)));}reg_eip += switchBlock.eipCount;continue;
                            case Inst.SHRB_CL_32_sib_fast: tmp2=(reg_ecx.dword & 0x1f); if (tmp2!=0) {eaa=ea32sib(switchBlock); Memory.mem_writeb(eaa, Memory.mem_readb(eaa)>>>tmp2);}reg_eip += switchBlock.eipCount;continue;

                                // Eb >> Ib
                            case Inst.SARB_CL: tmp2=(reg_ecx.dword & 0x1f);if (Instructions.valid_SARB(tmp2)) {switchBlock.r1.set8(Instructions.do_SARB(tmp2, switchBlock.r1.get8()));}reg_eip += switchBlock.eipCount;continue;
                            case Inst.SARB_CL_fast: tmp2=(reg_ecx.dword & 0x1f); if (tmp2!=0) switchBlock.r1.set8(((byte)switchBlock.r1.get8())>>tmp2);reg_eip += switchBlock.eipCount;continue;
                            case Inst.SARB_CL_16: tmp2=(reg_ecx.dword & 0x1f);if (Instructions.valid_SARB(tmp2)) {eaa=ea16(switchBlock);Memory.mem_writeb(eaa, Instructions.do_SARB(tmp2, Memory.mem_readb(eaa)));}reg_eip += switchBlock.eipCount;continue;
                            case Inst.SARB_CL_16_fast: tmp2=(reg_ecx.dword & 0x1f); if (tmp2!=0) {eaa=ea16(switchBlock); Memory.mem_writeb(eaa, ((byte)Memory.mem_readb(eaa))>>tmp2);}reg_eip += switchBlock.eipCount;continue;
                            case Inst.SARB_CL_32: tmp2=(reg_ecx.dword & 0x1f);if (Instructions.valid_SARB(tmp2)) {eaa=ea32(switchBlock);Memory.mem_writeb(eaa, Instructions.do_SARB(tmp2, Memory.mem_readb(eaa)));}reg_eip += switchBlock.eipCount;continue;
                            case Inst.SARB_CL_32_fast: tmp2=(reg_ecx.dword & 0x1f); if (tmp2!=0) {eaa=ea32(switchBlock); Memory.mem_writeb(eaa, ((byte)Memory.mem_readb(eaa))>>tmp2);}reg_eip += switchBlock.eipCount;continue;
                            case Inst.SARB_CL_32_sib: tmp2=(reg_ecx.dword & 0x1f);if (Instructions.valid_SARB(tmp2)) {eaa=ea32sib(switchBlock);Memory.mem_writeb(eaa, Instructions.do_SARB(tmp2, Memory.mem_readb(eaa)));}reg_eip += switchBlock.eipCount;continue;
                            case Inst.SARB_CL_32_sib_fast: tmp2=(reg_ecx.dword & 0x1f); if (tmp2!=0) {eaa=ea32sib(switchBlock); Memory.mem_writeb(eaa, ((byte)Memory.mem_readb(eaa))>>tmp2);}reg_eip += switchBlock.eipCount;continue;

                                // (Ew << CL) | (Ew >>> (16-CL)
                            case Inst.ROLW_CL: tmp2=(reg_ecx.dword & 0x1f);if (Instructions.valid_ROLW(switchBlock.r1.word(), tmp2)) switchBlock.r1.word(Instructions.do_ROLW(tmp2, switchBlock.r1.word()));reg_eip += switchBlock.eipCount;continue;
                            case Inst.ROLW_CL_fast: tmp2=(reg_ecx.dword & 0xF); if (tmp2!=0) {tmp=switchBlock.r1.word();switchBlock.r1.word((tmp << tmp2) | (tmp >>> (16-tmp2)));}reg_eip += switchBlock.eipCount;continue;
                            case Inst.ROLW_CL_16: tmp2=(reg_ecx.dword & 0x1f); eaa=ea16(switchBlock); tmp=Memory.mem_readw(eaa); if (Instructions.valid_ROLW(tmp, tmp2)) Memory.mem_writew(eaa, Instructions.do_ROLW(tmp2, tmp));reg_eip += switchBlock.eipCount;continue;
                            case Inst.ROLW_CL_16_fast: tmp2=(reg_ecx.dword & 0xF);if (tmp2!=0) {eaa=ea16(switchBlock); tmp=Memory.mem_readw(eaa);Memory.mem_writew(eaa, (tmp << tmp2) | (tmp >>> (16-tmp2)));}reg_eip += switchBlock.eipCount;continue;
                            case Inst.ROLW_CL_32: tmp2=(reg_ecx.dword & 0x1f); eaa=ea32(switchBlock); tmp=Memory.mem_readw(eaa); if (Instructions.valid_ROLW(tmp, tmp2)) Memory.mem_writew(eaa, Instructions.do_ROLW(tmp2, tmp));reg_eip += switchBlock.eipCount;continue;
                            case Inst.ROLW_CL_32_fast: tmp2=(reg_ecx.dword & 0xF);if (tmp2!=0) {eaa=ea32(switchBlock); tmp=Memory.mem_readw(eaa);Memory.mem_writew(eaa, (tmp << tmp2) | (tmp >>> (16-tmp2)));}reg_eip += switchBlock.eipCount;continue;
                            case Inst.ROLW_CL_32_sib: tmp2=(reg_ecx.dword & 0x1f); eaa=ea32sib(switchBlock); tmp=Memory.mem_readw(eaa); if (Instructions.valid_ROLW(tmp, tmp2)) Memory.mem_writew(eaa, Instructions.do_ROLW(tmp2, tmp));reg_eip += switchBlock.eipCount;continue;
                            case Inst.ROLW_CL_32_sib_fast: tmp2=(reg_ecx.dword & 0xF);if (tmp2!=0) {eaa=ea32sib(switchBlock); tmp=Memory.mem_readw(eaa);Memory.mem_writew(eaa, (tmp << tmp2) | (tmp >>> (16-tmp2)));}reg_eip += switchBlock.eipCount;continue;

                                // (Ew >>> CL) | (Ew << (16-CL))
                            case Inst.RORW_CL: tmp2=(reg_ecx.dword & 0x1f);if (Instructions.valid_RORW(switchBlock.r1.word(), tmp2)) switchBlock.r1.word(Instructions.do_RORW(tmp2, switchBlock.r1.word()));reg_eip += switchBlock.eipCount;continue;
                            case Inst.RORW_CL_fast: tmp2=(reg_ecx.dword & 0xF); if (tmp2!=0){tmp=switchBlock.r1.word();switchBlock.r1.word((tmp >>> tmp2) | (tmp << (16-tmp2)));}reg_eip += switchBlock.eipCount;continue;
                            case Inst.RORW_CL_16: tmp2=(reg_ecx.dword & 0x1f); eaa=ea16(switchBlock); tmp=Memory.mem_readw(eaa); if (Instructions.valid_RORW(tmp, tmp2)) Memory.mem_writew(eaa, Instructions.do_RORW(tmp2, tmp));reg_eip += switchBlock.eipCount;continue;
                            case Inst.RORW_CL_16_fast: tmp2=(reg_ecx.dword & 0xF);if (tmp2!=0) {eaa=ea16(switchBlock); tmp=Memory.mem_readw(eaa);Memory.mem_writew(eaa, (tmp >>> tmp2) | (tmp << (16-tmp2)));}reg_eip += switchBlock.eipCount;continue;
                            case Inst.RORW_CL_32: tmp2=(reg_ecx.dword & 0x1f); eaa=ea32(switchBlock); tmp=Memory.mem_readw(eaa); if (Instructions.valid_RORW(tmp, tmp2)) Memory.mem_writew(eaa, Instructions.do_RORW(tmp2, tmp));reg_eip += switchBlock.eipCount;continue;
                            case Inst.RORW_CL_32_fast: tmp2=(reg_ecx.dword & 0xF);if (tmp2!=0) {eaa=ea32(switchBlock); tmp=Memory.mem_readw(eaa);Memory.mem_writew(eaa, (tmp >>> tmp2) | (tmp << (16-tmp2)));}reg_eip += switchBlock.eipCount;continue;
                            case Inst.RORW_CL_32_sib: tmp2=(reg_ecx.dword & 0x1f); eaa=ea32sib(switchBlock); tmp=Memory.mem_readw(eaa); if (Instructions.valid_RORW(tmp, tmp2)) Memory.mem_writew(eaa, Instructions.do_RORW(tmp2, tmp));reg_eip += switchBlock.eipCount;continue;
                            case Inst.RORW_CL_32_sib_fast: tmp2=(reg_ecx.dword & 0xF);if (tmp2!=0) {eaa=ea32sib(switchBlock); tmp=Memory.mem_readw(eaa);Memory.mem_writew(eaa, (tmp >>> tmp2) | (tmp << (16-tmp2)));}reg_eip += switchBlock.eipCount;continue;

                                // (Ew << CL) |(cf << (CL-1)) | (Ew >>> (17-CL));
                            case Inst.RCLW_CL: tmp2=(reg_ecx.dword & 0x1f);if (Instructions.valid_RCLW(tmp2)) switchBlock.r1.word(Instructions.do_RCLW(tmp2, switchBlock.r1.word()));reg_eip += switchBlock.eipCount;continue;
                            case Inst.RCLW_CL_fast: tmp2=(reg_ecx.dword % 17);if (tmp2!=0) {cf=Flags.get_CF();tmp=switchBlock.r1.word();tmp=(tmp<<tmp2)|(tmp>>>(17-tmp2));if (cf) tmp|=(1<<(tmp2-1)); switchBlock.r1.word(tmp);}reg_eip += switchBlock.eipCount;continue;
                            case Inst.RCLW_CL_16: tmp2=(reg_ecx.dword & 0x1f);if (Instructions.valid_RCLW(tmp2)) {eaa=ea16(switchBlock);Memory.mem_writew(eaa, Instructions.do_RCLW(tmp2, Memory.mem_readw(eaa)));}reg_eip += switchBlock.eipCount;continue;
                            case Inst.RCLW_CL_16_fast: tmp2=(reg_ecx.dword % 17);if (tmp2!=0) {cf=Flags.get_CF();eaa=ea16(switchBlock); tmp=Memory.mem_readw(eaa);tmp=(tmp<<tmp2)|(tmp>>>(17-tmp2));if (cf) tmp|=(1<<(tmp2-1));Memory.mem_writew(eaa, tmp);}reg_eip += switchBlock.eipCount;continue;
                            case Inst.RCLW_CL_32: tmp2=(reg_ecx.dword & 0x1f);if (Instructions.valid_RCLW(tmp2)) {eaa=ea32(switchBlock);Memory.mem_writew(eaa, Instructions.do_RCLW(tmp2, Memory.mem_readw(eaa)));}reg_eip += switchBlock.eipCount;continue;
                            case Inst.RCLW_CL_32_fast: tmp2=(reg_ecx.dword % 17);if (tmp2!=0) {cf=Flags.get_CF();eaa=ea32(switchBlock); tmp=Memory.mem_readw(eaa);tmp=(tmp<<tmp2)|(tmp>>>(17-tmp2));if (cf) tmp|=(1<<(tmp2-1));Memory.mem_writew(eaa, tmp);}reg_eip += switchBlock.eipCount;continue;
                            case Inst.RCLW_CL_32_sib: tmp2=(reg_ecx.dword & 0x1f);if (Instructions.valid_RCLW(tmp2)) {eaa=ea32sib(switchBlock);Memory.mem_writew(eaa, Instructions.do_RCLW(tmp2, Memory.mem_readw(eaa)));}reg_eip += switchBlock.eipCount;continue;
                            case Inst.RCLW_CL_32_sib_fast: tmp2=(reg_ecx.dword % 17);if (tmp2!=0) {cf=Flags.get_CF();eaa=ea32sib(switchBlock); tmp=Memory.mem_readw(eaa);tmp=(tmp<<tmp2)|(tmp>>>(17-tmp2));if (cf) tmp|=(1<<(tmp2-1));Memory.mem_writew(eaa, tmp);}reg_eip += switchBlock.eipCount;continue;

                                // (Ew >>> CL) | (cf << (16-CL)) | (Ew << (17-CL))
                            case Inst.RCRW_CL: tmp2=(reg_ecx.dword & 0x1f);if (Instructions.valid_RCRW(tmp2)) switchBlock.r1.word(Instructions.do_RCRW(tmp2, switchBlock.r1.word()));reg_eip += switchBlock.eipCount;continue;
                            case Inst.RCRW_CL_fast: tmp2=(reg_ecx.dword % 17);if (tmp2!=0) {cf=Flags.get_CF();tmp=switchBlock.r1.word();tmp=(tmp>>>tmp2)|(tmp<<(17-tmp2));if (cf) tmp|=(1<<(16-tmp2)); switchBlock.r1.word(tmp);}reg_eip += switchBlock.eipCount;continue;
                            case Inst.RCRW_CL_16: tmp2=(reg_ecx.dword & 0x1f);if (Instructions.valid_RCRW(tmp2)) {eaa=ea16(switchBlock);Memory.mem_writew(eaa, Instructions.do_RCRW(tmp2, Memory.mem_readw(eaa)));}reg_eip += switchBlock.eipCount;continue;
                            case Inst.RCRW_CL_16_fast: tmp2=(reg_ecx.dword % 17);if (tmp2!=0) {cf=Flags.get_CF();eaa=ea16(switchBlock); tmp=Memory.mem_readw(eaa);tmp=(tmp>>>tmp2)|(tmp<<(17-tmp2));if (cf) tmp|=(1<<(16-tmp2));Memory.mem_writew(eaa, tmp);}reg_eip += switchBlock.eipCount;continue;
                            case Inst.RCRW_CL_32: tmp2=(reg_ecx.dword & 0x1f);if (Instructions.valid_RCRW(tmp2)) {eaa=ea32(switchBlock);Memory.mem_writew(eaa, Instructions.do_RCRW(tmp2, Memory.mem_readw(eaa)));}reg_eip += switchBlock.eipCount;continue;
                            case Inst.RCRW_CL_32_fast: tmp2=(reg_ecx.dword % 17);if (tmp2!=0) {cf=Flags.get_CF();eaa=ea32(switchBlock); tmp=Memory.mem_readw(eaa);tmp=(tmp>>>tmp2)|(tmp<<(17-tmp2));if (cf) tmp|=(1<<(16-tmp2));Memory.mem_writew(eaa, tmp);}reg_eip += switchBlock.eipCount;continue;
                            case Inst.RCRW_CL_32_sib: tmp2=(reg_ecx.dword & 0x1f);if (Instructions.valid_RCRW(tmp2)) {eaa=ea32sib(switchBlock);Memory.mem_writew(eaa, Instructions.do_RCRW(tmp2, Memory.mem_readw(eaa)));}reg_eip += switchBlock.eipCount;continue;
                            case Inst.RCRW_CL_32_sib_fast: tmp2=(reg_ecx.dword % 17);if (tmp2!=0) {cf=Flags.get_CF();eaa=ea32sib(switchBlock); tmp=Memory.mem_readw(eaa);tmp=(tmp>>>tmp2)|(tmp<<(17-tmp2));if (cf) tmp|=(1<<(16-tmp2));Memory.mem_writew(eaa, tmp);}reg_eip += switchBlock.eipCount;continue;

                                // Ew << CL
                            case Inst.SHLW_CL: tmp2=(reg_ecx.dword & 0x1f);if (Instructions.valid_SHLW(tmp2)) {switchBlock.r1.word(Instructions.do_SHLW(tmp2, switchBlock.r1.word()));}reg_eip += switchBlock.eipCount;continue;
                            case Inst.SHLW_CL_fast: tmp2=(reg_ecx.dword & 0x1f); if (tmp2!=0) switchBlock.r1.word(switchBlock.r1.word()<<tmp2);reg_eip += switchBlock.eipCount;continue;
                            case Inst.SHLW_CL_16: tmp2=(reg_ecx.dword & 0x1f);if (Instructions.valid_SHLW(tmp2)) {eaa=ea16(switchBlock);Memory.mem_writew(eaa, Instructions.do_SHLW(tmp2, Memory.mem_readw(eaa)));}reg_eip += switchBlock.eipCount;continue;
                            case Inst.SHLW_CL_16_fast: tmp2=(reg_ecx.dword & 0x1f); if (tmp2!=0) {eaa=ea16(switchBlock); Memory.mem_writew(eaa, Memory.mem_readw(eaa)<<tmp2);}reg_eip += switchBlock.eipCount;continue;
                            case Inst.SHLW_CL_32: tmp2=(reg_ecx.dword & 0x1f);if (Instructions.valid_SHLW(tmp2)) {eaa=ea32(switchBlock);Memory.mem_writew(eaa, Instructions.do_SHLW(tmp2, Memory.mem_readw(eaa)));}reg_eip += switchBlock.eipCount;continue;
                            case Inst.SHLW_CL_32_fast: tmp2=(reg_ecx.dword & 0x1f); if (tmp2!=0) {eaa=ea32(switchBlock); Memory.mem_writew(eaa, Memory.mem_readw(eaa)<<tmp2);}reg_eip += switchBlock.eipCount;continue;
                            case Inst.SHLW_CL_32_sib: tmp2=(reg_ecx.dword & 0x1f);if (Instructions.valid_SHLW(tmp2)) {eaa=ea32sib(switchBlock);Memory.mem_writew(eaa, Instructions.do_SHLW(tmp2, Memory.mem_readw(eaa)));}reg_eip += switchBlock.eipCount;continue;
                            case Inst.SHLW_CL_32_sib_fast: tmp2=(reg_ecx.dword & 0x1f); if (tmp2!=0) {eaa=ea32sib(switchBlock); Memory.mem_writew(eaa, Memory.mem_readw(eaa)<<tmp2);}reg_eip += switchBlock.eipCount;continue;

                                // Ew >>> CL
                            case Inst.SHRW_CL: tmp2=(reg_ecx.dword & 0x1f);if (Instructions.valid_SHRW(tmp2)) {switchBlock.r1.word(Instructions.do_SHRW(tmp2, switchBlock.r1.word()));}reg_eip += switchBlock.eipCount;continue;
                            case Inst.SHRW_CL_fast: tmp2=(reg_ecx.dword & 0x1f); if (tmp2!=0) switchBlock.r1.word(switchBlock.r1.word()>>>tmp2);reg_eip += switchBlock.eipCount;continue;
                            case Inst.SHRW_CL_16: tmp2=(reg_ecx.dword & 0x1f);if (Instructions.valid_SHRW(tmp2)) {eaa=ea16(switchBlock);Memory.mem_writew(eaa, Instructions.do_SHRW(tmp2, Memory.mem_readw(eaa)));}reg_eip += switchBlock.eipCount;continue;
                            case Inst.SHRW_CL_16_fast: tmp2=(reg_ecx.dword & 0x1f); if (tmp2!=0) {eaa=ea16(switchBlock); Memory.mem_writew(eaa, Memory.mem_readw(eaa)>>>tmp2);}reg_eip += switchBlock.eipCount;continue;
                            case Inst.SHRW_CL_32: tmp2=(reg_ecx.dword & 0x1f);if (Instructions.valid_SHRW(tmp2)) {eaa=ea32(switchBlock);Memory.mem_writew(eaa, Instructions.do_SHRW(tmp2, Memory.mem_readw(eaa)));}reg_eip += switchBlock.eipCount;continue;
                            case Inst.SHRW_CL_32_fast: tmp2=(reg_ecx.dword & 0x1f); if (tmp2!=0) {eaa=ea32(switchBlock); Memory.mem_writew(eaa, Memory.mem_readw(eaa)>>>tmp2);}reg_eip += switchBlock.eipCount;continue;
                            case Inst.SHRW_CL_32_sib: tmp2=(reg_ecx.dword & 0x1f);if (Instructions.valid_SHRW(tmp2)) {eaa=ea32sib(switchBlock);Memory.mem_writew(eaa, Instructions.do_SHRW(tmp2, Memory.mem_readw(eaa)));}reg_eip += switchBlock.eipCount;continue;
                            case Inst.SHRW_CL_32_sib_fast: tmp2=(reg_ecx.dword & 0x1f); if (tmp2!=0) {eaa=ea32sib(switchBlock); Memory.mem_writew(eaa, Memory.mem_readw(eaa)>>>tmp2);}reg_eip += switchBlock.eipCount;continue;

                                // Ew >> CL
                            case Inst.SARW_CL: tmp2=(reg_ecx.dword & 0x1f);if (Instructions.valid_SARW(tmp2)) {switchBlock.r1.word(Instructions.do_SARW(tmp2, switchBlock.r1.word()));}reg_eip += switchBlock.eipCount;continue;
                            case Inst.SARW_CL_fast: tmp2=(reg_ecx.dword & 0x1f); if (tmp2!=0) switchBlock.r1.word(((short)switchBlock.r1.word())>>tmp2);reg_eip += switchBlock.eipCount;continue;
                            case Inst.SARW_CL_16: tmp2=(reg_ecx.dword & 0x1f);if (Instructions.valid_SARW(tmp2)) {eaa=ea16(switchBlock);Memory.mem_writew(eaa, Instructions.do_SARW(tmp2, Memory.mem_readw(eaa)));}reg_eip += switchBlock.eipCount;continue;
                            case Inst.SARW_CL_16_fast: tmp2=(reg_ecx.dword & 0x1f); if (tmp2!=0) {eaa=ea16(switchBlock); Memory.mem_writew(eaa, ((short)Memory.mem_readw(eaa))>>tmp2);}reg_eip += switchBlock.eipCount;continue;
                            case Inst.SARW_CL_32: tmp2=(reg_ecx.dword & 0x1f);if (Instructions.valid_SARW(tmp2)) {eaa=ea32(switchBlock);Memory.mem_writew(eaa, Instructions.do_SARW(tmp2, Memory.mem_readw(eaa)));}reg_eip += switchBlock.eipCount;continue;
                            case Inst.SARW_CL_32_fast: tmp2=(reg_ecx.dword & 0x1f); if (tmp2!=0) {eaa=ea32(switchBlock); Memory.mem_writew(eaa, ((short)Memory.mem_readw(eaa))>>tmp2);}reg_eip += switchBlock.eipCount;continue;
                            case Inst.SARW_CL_32_sib: tmp2=(reg_ecx.dword & 0x1f);if (Instructions.valid_SARW(tmp2)) {eaa=ea32sib(switchBlock);Memory.mem_writew(eaa, Instructions.do_SARW(tmp2, Memory.mem_readw(eaa)));}reg_eip += switchBlock.eipCount;continue;
                            case Inst.SARW_CL_32_sib_fast: tmp2=(reg_ecx.dword & 0x1f); if (tmp2!=0) {eaa=ea32sib(switchBlock); Memory.mem_writew(eaa, ((short)Memory.mem_readw(eaa))>>tmp2);}reg_eip += switchBlock.eipCount;continue;

                            case Inst.AAM: if (!Instructions.AAM(switchBlock.value)) {RUNEXCEPTION();block=null;break;}reg_eip += switchBlock.eipCount;continue;
                            case Inst.AAD: Instructions.AAD(switchBlock.value); reg_eip += switchBlock.eipCount;continue;
                            case Inst.SALC: reg_eax.low(Flags.get_CF() ? 0xFF : 0); reg_eip += switchBlock.eipCount;continue;
                            case Inst.XLAT16: reg_eax.low(Memory.mem_readb(switchBlock.eaa_segPhys.dword+((reg_ebx.word()+reg_eax.low()) & 0xFFFF))); reg_eip += switchBlock.eipCount;continue;
                            case Inst.XLAT32: reg_eax.low(Memory.mem_readb(switchBlock.eaa_segPhys.dword+(reg_ebx.dword+reg_eax.low()))); reg_eip += switchBlock.eipCount;continue;
                            case Inst.LOOPNZ16_16: reg_ecx.word(reg_ecx.word()-1);block = jump16(block, reg_ecx.word()!=0 && !Flags.get_ZF(), switchBlock.value, switchBlock.eipCount);break;
                            case Inst.LOOPNZ16_32: reg_ecx.dword--;block = jump16(block, reg_ecx.dword!=0 && !Flags.get_ZF(), switchBlock.value, switchBlock.eipCount);break;
                            case Inst.LOOPZ16_16: reg_ecx.word(reg_ecx.word()-1);block = jump16(block, reg_ecx.word()!=0 && Flags.get_ZF(), switchBlock.value, switchBlock.eipCount);break;
                            case Inst.LOOPZ16_32: reg_ecx.dword--;block = jump16(block, reg_ecx.dword!=0 && Flags.get_ZF(), switchBlock.value, switchBlock.eipCount);break;
                            case Inst.LOOP16_16: reg_ecx.word(reg_ecx.word()-1);block = jump16(block, reg_ecx.word()!=0, switchBlock.value, switchBlock.eipCount);break;
                            case Inst.LOOP16_32: reg_ecx.dword--;block = jump16(block, reg_ecx.dword!=0, switchBlock.value, switchBlock.eipCount);break;
                            case Inst.JCXZ16_16: block = jump16(block, reg_ecx.word()==0, switchBlock.value, switchBlock.eipCount);break;
                            case Inst.JCXZ16_32: block = jump16(block, reg_ecx.dword==0, switchBlock.value, switchBlock.eipCount);break;
                            case Inst.IN_AL_Ib:if (CPU.CPU_IO_Exception(switchBlock.value,1)) {RUNEXCEPTION();block=null;break;} reg_eax.low(IO.IO_ReadB(switchBlock.value));reg_eip += switchBlock.eipCount;continue;
                            case Inst.IN_AX_Ib:if (CPU.CPU_IO_Exception(switchBlock.value,2)) {RUNEXCEPTION();block=null;break;} reg_eax.word(IO.IO_ReadW(switchBlock.value));reg_eip += switchBlock.eipCount;continue;
                            case Inst.OUT_Ib_AL:if (CPU.CPU_IO_Exception(switchBlock.value,1)) {RUNEXCEPTION();block=null;break;} IO.IO_WriteB(switchBlock.value,reg_eax.low());reg_eip += switchBlock.eipCount;continue;
                            case Inst.OUT_Ib_AX:if (CPU.CPU_IO_Exception(switchBlock.value,2)) {RUNEXCEPTION();block=null;break;} IO.IO_WriteB(switchBlock.value,reg_eax.low());reg_eip += switchBlock.eipCount;continue;
                            case Inst.CALL16_Jw:CPU.CPU_Push16(reg_eip+switchBlock.eipCount);reg_ip(reg_eip+switchBlock.eipCount+switchBlock.value); block=link1(block); break;
                            case Inst.JMP16:reg_ip(reg_eip+switchBlock.eipCount+switchBlock.value); block=link1(block); break;
                            case Inst.JMP16_AP:Flags.FillFlags();
                                CPU.CPU_JMP(false,switchBlock.eaa_const,switchBlock.value,reg_eip+switchBlock.eipCount);
                                if (GETFLAG(TF)!=0) {
                                    CPU.cpudecoder=CPU_Core_Switch_Trap_Run;
                                    return Callback.CBRET_NONE;
                                }
                                block=null;
                                break;
                            case Inst.IN_AL_DX:if (CPU.CPU_IO_Exception(reg_edx.word(),1)) {RUNEXCEPTION();block=null;break;} reg_eax.low(IO.IO_ReadB(reg_edx.word()));reg_eip += switchBlock.eipCount;continue;
                            case Inst.IN_AX_DX:if (CPU.CPU_IO_Exception(reg_edx.word(),2)) {RUNEXCEPTION();block=null;break;} reg_eax.word(IO.IO_ReadW(reg_edx.word()));reg_eip += switchBlock.eipCount;continue;
                            case Inst.OUT_DX_AL:if (CPU.CPU_IO_Exception(reg_edx.word(),1)) {RUNEXCEPTION();block=null;break;} IO.IO_WriteB(reg_edx.word(),reg_eax.low());reg_eip += switchBlock.eipCount;continue;
                            case Inst.OUT_DX_AX:if (CPU.CPU_IO_Exception(reg_edx.word(),2)) {RUNEXCEPTION();block=null;break;} IO.IO_WriteB(reg_edx.word(),reg_eax.low());reg_eip += switchBlock.eipCount;continue;
                            case Inst.ICEBP:CPU.CPU_SW_Interrupt_NoIOPLCheck(1,reg_eip+switchBlock.eipCount);CPU.cpu.trap_skip=true;block=null;break;
                            case Inst.HLT: if (CPU.cpu.pmode && CPU.cpu.cpl!=0) {CPU.CPU_Exception(CPU.EXCEPTION_GP);block=null;break;}Flags.FillFlags();CPU.CPU_HLT(reg_eip+switchBlock.eipCount);return Callback.CBRET_NONE;
                            case Inst.CMC: Flags.FillFlags(); SETFLAGBIT(CF,(CPU_Regs.flags & CPU_Regs.CF)==0);reg_eip += switchBlock.eipCount;continue;
                            case Inst.NOT_R8: switchBlock.r1.set8(~switchBlock.r1.get8());reg_eip+=switchBlock.eipCount;continue;
                            case Inst.NOT_E8_16: eaa=ea16(switchBlock);Memory.mem_writeb(eaa, ~Memory.mem_readb(eaa));reg_eip+=switchBlock.eipCount;continue;
                            case Inst.NOT_E8_32: eaa=ea32(switchBlock);Memory.mem_writeb(eaa, ~Memory.mem_readb(eaa));reg_eip+=switchBlock.eipCount;continue;
                            case Inst.NOT_E8_32_sib: eaa=ea32sib(switchBlock);Memory.mem_writeb(eaa, ~Memory.mem_readb(eaa));reg_eip+=switchBlock.eipCount;continue;
                            case Inst.NOT_R16: switchBlock.r1.word(~switchBlock.r1.word());reg_eip+=switchBlock.eipCount;continue;
                            case Inst.NOT_E16_16: eaa=ea16(switchBlock);Memory.mem_writew(eaa, ~Memory.mem_readw(eaa));reg_eip+=switchBlock.eipCount;continue;
                            case Inst.NOT_E16_32: eaa=ea32(switchBlock);Memory.mem_writew(eaa, ~Memory.mem_readw(eaa));reg_eip+=switchBlock.eipCount;continue;
                            case Inst.NOT_E16_32_sib: eaa=ea32sib(switchBlock);Memory.mem_writew(eaa, ~Memory.mem_readw(eaa));reg_eip+=switchBlock.eipCount;continue;
                            case Inst.NOT_R32: switchBlock.r1.dword=~switchBlock.r1.dword;reg_eip+=switchBlock.eipCount;continue;
                            case Inst.NOT_E32_16: eaa=ea16(switchBlock);Memory.mem_writed(eaa, ~Memory.mem_readd(eaa));reg_eip+=switchBlock.eipCount;continue;
                            case Inst.NOT_E32_32: eaa=ea32(switchBlock);Memory.mem_writed(eaa, ~Memory.mem_readd(eaa));reg_eip+=switchBlock.eipCount;continue;
                            case Inst.NOT_E32_32_sib: eaa=ea32sib(switchBlock);Memory.mem_writed(eaa, ~Memory.mem_readd(eaa));reg_eip+=switchBlock.eipCount;continue;

                            case Inst.NEG_R8: switchBlock.r1.set8(Instructions.Negb(switchBlock.r1.get8()));reg_eip+=switchBlock.eipCount;continue;
                            case Inst.NEG_R8_fast: switchBlock.r1.set8(-switchBlock.r1.get8());reg_eip+=switchBlock.eipCount;continue;
                            case Inst.NEG_E8_16: eaa=ea16(switchBlock);Memory.mem_writeb(eaa, Instructions.Negb(Memory.mem_readb(eaa)));reg_eip+=switchBlock.eipCount;continue;
                            case Inst.NEG_E8_16_fast: eaa=ea16(switchBlock);Memory.mem_writeb(eaa, -Memory.mem_readb(eaa));reg_eip+=switchBlock.eipCount;continue;
                            case Inst.NEG_E8_32: eaa=ea32(switchBlock);Memory.mem_writeb(eaa, Instructions.Negb(Memory.mem_readb(eaa)));reg_eip+=switchBlock.eipCount;continue;
                            case Inst.NEG_E8_32_fast: eaa=ea32(switchBlock);Memory.mem_writeb(eaa, -Memory.mem_readb(eaa));reg_eip+=switchBlock.eipCount;continue;
                            case Inst.NEG_E8_32_sib: eaa=ea32sib(switchBlock);Memory.mem_writeb(eaa, Instructions.Negb(Memory.mem_readb(eaa)));reg_eip+=switchBlock.eipCount;continue;
                            case Inst.NEG_E8_32_sib_fast: eaa=ea32sib(switchBlock);Memory.mem_writeb(eaa, -Memory.mem_readb(eaa));reg_eip+=switchBlock.eipCount;continue;
                            case Inst.NEG_R16: switchBlock.r1.word(Instructions.Negw(switchBlock.r1.word()));reg_eip+=switchBlock.eipCount;continue;
                            case Inst.NEG_R16_fast: switchBlock.r1.word(-switchBlock.r1.word());reg_eip+=switchBlock.eipCount;continue;
                            case Inst.NEG_E16_16: eaa=ea16(switchBlock);Memory.mem_writew(eaa, Instructions.Negw(Memory.mem_readw(eaa)));reg_eip+=switchBlock.eipCount;continue;
                            case Inst.NEG_E16_16_fast: eaa=ea16(switchBlock);Memory.mem_writew(eaa, -Memory.mem_readw(eaa));reg_eip+=switchBlock.eipCount;continue;
                            case Inst.NEG_E16_32: eaa=ea32(switchBlock);Memory.mem_writew(eaa, Instructions.Negw(Memory.mem_readw(eaa)));reg_eip+=switchBlock.eipCount;continue;
                            case Inst.NEG_E16_32_fast: eaa=ea32(switchBlock);Memory.mem_writew(eaa, -Memory.mem_readw(eaa));reg_eip+=switchBlock.eipCount;continue;
                            case Inst.NEG_E16_32_sib: eaa=ea32sib(switchBlock);Memory.mem_writew(eaa, Instructions.Negw(Memory.mem_readw(eaa)));reg_eip+=switchBlock.eipCount;continue;
                            case Inst.NEG_E16_32_sib_fast: eaa=ea32sib(switchBlock);Memory.mem_writew(eaa, -Memory.mem_readw(eaa));reg_eip+=switchBlock.eipCount;continue;
                            case Inst.NEG_R32: switchBlock.r1.dword=Instructions.Negd(switchBlock.r1.dword);reg_eip+=switchBlock.eipCount;continue;
                            case Inst.NEG_R32_fast: switchBlock.r1.dword=-switchBlock.r1.dword;reg_eip+=switchBlock.eipCount;continue;
                            case Inst.NEG_E32_16: eaa=ea16(switchBlock);Memory.mem_writed(eaa, Instructions.Negd(Memory.mem_readd(eaa)));reg_eip+=switchBlock.eipCount;continue;
                            case Inst.NEG_E32_16_fast: eaa=ea16(switchBlock);Memory.mem_writed(eaa, -Memory.mem_readd(eaa));reg_eip+=switchBlock.eipCount;continue;
                            case Inst.NEG_E32_32: eaa=ea32(switchBlock);Memory.mem_writed(eaa, Instructions.Negd(Memory.mem_readd(eaa)));reg_eip+=switchBlock.eipCount;continue;
                            case Inst.NEG_E32_32_fast: eaa=ea32(switchBlock);Memory.mem_writed(eaa, -Memory.mem_readd(eaa));reg_eip+=switchBlock.eipCount;continue;
                            case Inst.NEG_E32_32_sib: eaa=ea32sib(switchBlock);Memory.mem_writed(eaa, Instructions.Negd(Memory.mem_readd(eaa)));reg_eip+=switchBlock.eipCount;continue;
                            case Inst.NEG_E32_32_sib_fast: eaa=ea32sib(switchBlock);Memory.mem_writed(eaa, -Memory.mem_readd(eaa));reg_eip+=switchBlock.eipCount;continue;

                            case Inst.MUL_R8: Instructions.MULB(switchBlock.r1.get8());reg_eip+=switchBlock.eipCount;continue;
                            case Inst.MUL_R8_fast: reg_eax.word(reg_eax.low()*switchBlock.r1.get8());reg_eip+=switchBlock.eipCount;continue;
                            case Inst.MUL_E8_16: Instructions.MULB(Memory.mem_readb(ea16(switchBlock)));reg_eip+=switchBlock.eipCount;continue;
                            case Inst.MUL_E8_16_fast: reg_eax.word(reg_eax.low()*Memory.mem_readb(ea16(switchBlock)));reg_eip+=switchBlock.eipCount;continue;
                            case Inst.MUL_E8_32: Instructions.MULB(Memory.mem_readb(ea32(switchBlock)));reg_eip+=switchBlock.eipCount;continue;
                            case Inst.MUL_E8_32_fast: reg_eax.word(reg_eax.low()*Memory.mem_readb(ea32(switchBlock)));reg_eip+=switchBlock.eipCount;continue;
                            case Inst.MUL_E8_32_sib: Instructions.MULB(Memory.mem_readb(ea32sib(switchBlock)));reg_eip+=switchBlock.eipCount;continue;
                            case Inst.MUL_E8_32_sib_fast: reg_eax.word(reg_eax.low()*Memory.mem_readb(ea32sib(switchBlock)));reg_eip+=switchBlock.eipCount;continue;
                            case Inst.MUL_R16: Instructions.MULW(switchBlock.r1.word());reg_eip+=switchBlock.eipCount;continue;
                            case Inst.MUL_R16_fast: tmp=reg_eax.word()*switchBlock.r1.word();reg_eax.word(tmp);reg_edx.word(tmp>>>16);reg_eip+=switchBlock.eipCount;continue;
                            case Inst.MUL_E16_16: Instructions.MULW(Memory.mem_readw(ea16(switchBlock)));reg_eip+=switchBlock.eipCount;continue;
                            case Inst.MUL_E16_16_fast: tmp=reg_eax.word()*Memory.mem_readw(ea16(switchBlock));reg_eax.word(tmp);reg_edx.word(tmp>>>16);reg_eip+=switchBlock.eipCount;continue;
                            case Inst.MUL_E16_32: Instructions.MULW(Memory.mem_readw(ea32(switchBlock)));reg_eip+=switchBlock.eipCount;continue;
                            case Inst.MUL_E16_32_fast: tmp=reg_eax.word()*Memory.mem_readw(ea32(switchBlock));reg_eax.word(tmp);reg_edx.word(tmp>>>16);reg_eip+=switchBlock.eipCount;continue;
                            case Inst.MUL_E16_32_sib: Instructions.MULW(Memory.mem_readw(ea32sib(switchBlock)));reg_eip+=switchBlock.eipCount;continue;
                            case Inst.MUL_E16_32_sib_fast: tmp=reg_eax.word()*Memory.mem_readw(ea32sib(switchBlock));reg_eax.word(tmp);reg_edx.word(tmp>>>16);reg_eip+=switchBlock.eipCount;continue;
                            case Inst.MUL_R32: Instructions.MULD(switchBlock.r1.dword);reg_eip+=switchBlock.eipCount;continue;
                            case Inst.MUL_R32_fast: tmpl=(reg_eax.dword & 0xFFFFFFFFl)*(switchBlock.r1.dword & 0xFFFFFFFFl);reg_eax.dword=(int)tmpl;reg_edx.dword=(int)(tmpl>>>32);reg_eip+=switchBlock.eipCount;continue;
                            case Inst.MUL_E32_16: Instructions.MULD(Memory.mem_readd(ea16(switchBlock)));reg_eip+=switchBlock.eipCount;continue;
                            case Inst.MUL_E32_16_fast: tmpl=(reg_eax.dword & 0xFFFFFFFFl)*(Memory.mem_readd(ea16(switchBlock)) & 0xFFFFFFFFl);reg_eax.dword=(int)tmpl;reg_edx.dword=(int)(tmpl>>>32);reg_eip+=switchBlock.eipCount;continue;
                            case Inst.MUL_E32_32: Instructions.MULD(Memory.mem_readd(ea32(switchBlock)));reg_eip+=switchBlock.eipCount;continue;
                            case Inst.MUL_E32_32_fast: tmpl=(reg_eax.dword & 0xFFFFFFFFl)*(Memory.mem_readd(ea32(switchBlock)) & 0xFFFFFFFFl);reg_eax.dword=(int)tmpl;reg_edx.dword=(int)(tmpl>>>32);reg_eip+=switchBlock.eipCount;continue;
                            case Inst.MUL_E32_32_sib: Instructions.MULD(Memory.mem_readd(ea32sib(switchBlock)));reg_eip+=switchBlock.eipCount;continue;
                            case Inst.MUL_E32_32_sib_fast: tmpl=(reg_eax.dword & 0xFFFFFFFFl)*(Memory.mem_readd(ea32sib(switchBlock)) & 0xFFFFFFFFl);reg_eax.dword=(int)tmpl;reg_edx.dword=(int)(tmpl>>>32);reg_eip+=switchBlock.eipCount;continue;

                            case Inst.IMUL_R8: Instructions.IMULB(switchBlock.r1.get8());reg_eip+=switchBlock.eipCount;continue;
                            case Inst.IMUL_R8_fast: reg_eax.word(((byte)reg_eax.low())*((byte)switchBlock.r1.get8()));reg_eip+=switchBlock.eipCount;continue;
                            case Inst.IMUL_E8_16: Instructions.IMULB(Memory.mem_readb(ea16(switchBlock)));reg_eip+=switchBlock.eipCount;continue;
                            case Inst.IMUL_E8_16_fast: reg_eax.word(((byte)reg_eax.low())*((byte)Memory.mem_readb(ea16(switchBlock))));reg_eip+=switchBlock.eipCount;continue;
                            case Inst.IMUL_E8_32: Instructions.IMULB(Memory.mem_readb(ea32(switchBlock)));reg_eip+=switchBlock.eipCount;continue;
                            case Inst.IMUL_E8_32_fast: reg_eax.word(((byte)reg_eax.low())*((byte)Memory.mem_readb(ea32(switchBlock))));reg_eip+=switchBlock.eipCount;continue;
                            case Inst.IMUL_E8_32_sib: Instructions.IMULB(Memory.mem_readb(ea32sib(switchBlock)));reg_eip+=switchBlock.eipCount;continue;
                            case Inst.IMUL_E8_32_sib_fast: reg_eax.word(((byte)reg_eax.low())*((byte)Memory.mem_readb(ea32sib(switchBlock))));reg_eip+=switchBlock.eipCount;continue;
                            case Inst.IMUL_R16: Instructions.IMULW(switchBlock.r1.word());reg_eip+=switchBlock.eipCount;continue;
                            case Inst.IMUL_R16_fast: tmp=((short)reg_eax.word())*((short)switchBlock.r1.word());reg_eax.word(tmp);reg_edx.word(tmp>>>16);reg_eip+=switchBlock.eipCount;continue;
                            case Inst.IMUL_E16_16: Instructions.IMULW(Memory.mem_readw(ea16(switchBlock)));reg_eip+=switchBlock.eipCount;continue;
                            case Inst.IMUL_E16_16_fast: tmp=((short)reg_eax.word())*((short)Memory.mem_readw(ea16(switchBlock)));reg_eax.word(tmp);reg_edx.word(tmp>>>16);reg_eip+=switchBlock.eipCount;continue;
                            case Inst.IMUL_E16_32: Instructions.IMULW(Memory.mem_readw(ea32(switchBlock)));reg_eip+=switchBlock.eipCount;continue;
                            case Inst.IMUL_E16_32_fast: tmp=((short)reg_eax.word())*((short)Memory.mem_readw(ea32(switchBlock)));reg_eax.word(tmp);reg_edx.word(tmp>>>16);reg_eip+=switchBlock.eipCount;continue;
                            case Inst.IMUL_E16_32_sib: Instructions.IMULW(Memory.mem_readw(ea32sib(switchBlock)));reg_eip+=switchBlock.eipCount;continue;
                            case Inst.IMUL_E16_32_sib_fast: tmp=((short)reg_eax.word())*((short)Memory.mem_readw(ea32sib(switchBlock)));reg_eax.word(tmp);reg_edx.word(tmp>>>16);reg_eip+=switchBlock.eipCount;continue;
                            case Inst.IMUL_R32: Instructions.IMULD(switchBlock.r1.dword);reg_eip+=switchBlock.eipCount;continue;
                            case Inst.IMUL_R32_fast: tmpl=reg_eax.dword*switchBlock.r1.dword;reg_eax.dword=(int)tmpl;reg_edx.dword=(int)(tmpl>>>32);reg_eip+=switchBlock.eipCount;continue;
                            case Inst.IMUL_E32_16: Instructions.IMULD(Memory.mem_readd(ea16(switchBlock)));reg_eip+=switchBlock.eipCount;continue;
                            case Inst.IMUL_E32_16_fast: tmpl=reg_eax.dword*Memory.mem_readd(ea16(switchBlock));reg_eax.dword=(int)tmpl;reg_edx.dword=(int)(tmpl>>>32);reg_eip+=switchBlock.eipCount;continue;
                            case Inst.IMUL_E32_32: Instructions.IMULD(Memory.mem_readd(ea32(switchBlock)));reg_eip+=switchBlock.eipCount;continue;
                            case Inst.IMUL_E32_32_fast: tmpl=reg_eax.dword*Memory.mem_readd(ea32(switchBlock));reg_eax.dword=(int)tmpl;reg_edx.dword=(int)(tmpl>>>32);reg_eip+=switchBlock.eipCount;continue;
                            case Inst.IMUL_E32_32_sib: Instructions.IMULD(Memory.mem_readd(ea32sib(switchBlock)));reg_eip+=switchBlock.eipCount;continue;
                            case Inst.IMUL_E32_32_sib_fast: tmpl=reg_eax.dword*Memory.mem_readd(ea32sib(switchBlock));reg_eax.dword=(int)tmpl;reg_edx.dword=(int)(tmpl>>>32);reg_eip+=switchBlock.eipCount;continue;


                            case Inst.DIV_R8: Instructions.DIVB(switchBlock.r1.get8());reg_eip+=switchBlock.eipCount;continue;
                            case Inst.DIV_E8_16: Instructions.DIVB(Memory.mem_readb(ea16(switchBlock)));reg_eip+=switchBlock.eipCount;continue;
                            case Inst.DIV_E8_32: Instructions.DIVB(Memory.mem_readb(ea32(switchBlock)));reg_eip+=switchBlock.eipCount;continue;
                            case Inst.DIV_E8_32_sib: Instructions.DIVB(Memory.mem_readb(ea32sib(switchBlock)));reg_eip+=switchBlock.eipCount;continue;
                            case Inst.DIV_R16: Instructions.DIVW(switchBlock.r1.word());reg_eip+=switchBlock.eipCount;continue;
                            case Inst.DIV_E16_16: Instructions.DIVW(Memory.mem_readw(ea16(switchBlock)));reg_eip+=switchBlock.eipCount;continue;
                            case Inst.DIV_E16_32: Instructions.DIVW(Memory.mem_readw(ea32(switchBlock)));reg_eip+=switchBlock.eipCount;continue;
                            case Inst.DIV_E16_32_sib: Instructions.DIVW(Memory.mem_readw(ea32sib(switchBlock)));reg_eip+=switchBlock.eipCount;continue;
                            case Inst.DIV_R32: Instructions.DIVD(switchBlock.r1.dword);reg_eip+=switchBlock.eipCount;continue;
                            case Inst.DIV_E32_16: Instructions.DIVD(Memory.mem_readd(ea16(switchBlock)));reg_eip+=switchBlock.eipCount;continue;
                            case Inst.DIV_E32_32: Instructions.DIVD(Memory.mem_readd(ea32(switchBlock)));reg_eip+=switchBlock.eipCount;continue;
                            case Inst.DIV_E32_32_sib: Instructions.DIVD(Memory.mem_readd(ea32sib(switchBlock)));reg_eip+=switchBlock.eipCount;continue;

                            case Inst.IDIV_R8: Instructions.IDIVB(switchBlock.r1.get8());reg_eip+=switchBlock.eipCount;continue;
                            case Inst.IDIV_E8_16: Instructions.IDIVB(Memory.mem_readb(ea16(switchBlock)));reg_eip+=switchBlock.eipCount;continue;
                            case Inst.IDIV_E8_32: Instructions.IDIVB(Memory.mem_readb(ea32(switchBlock)));reg_eip+=switchBlock.eipCount;continue;
                            case Inst.IDIV_E8_32_sib: Instructions.IDIVB(Memory.mem_readb(ea32sib(switchBlock)));reg_eip+=switchBlock.eipCount;continue;
                            case Inst.IDIV_R16: Instructions.IDIVW(switchBlock.r1.word());reg_eip+=switchBlock.eipCount;continue;
                            case Inst.IDIV_E16_16: Instructions.IDIVW(Memory.mem_readw(ea16(switchBlock)));reg_eip+=switchBlock.eipCount;continue;
                            case Inst.IDIV_E16_32: Instructions.IDIVW(Memory.mem_readw(ea32(switchBlock)));reg_eip+=switchBlock.eipCount;continue;
                            case Inst.IDIV_E16_32_sib: Instructions.IDIVW(Memory.mem_readw(ea32sib(switchBlock)));reg_eip+=switchBlock.eipCount;continue;
                            case Inst.IDIV_R32: Instructions.IDIVD(switchBlock.r1.dword);reg_eip+=switchBlock.eipCount;continue;
                            case Inst.IDIV_E32_16: Instructions.IDIVD(Memory.mem_readd(ea16(switchBlock)));reg_eip+=switchBlock.eipCount;continue;
                            case Inst.IDIV_E32_32: Instructions.IDIVD(Memory.mem_readd(ea32(switchBlock)));reg_eip+=switchBlock.eipCount;continue;
                            case Inst.IDIV_E32_32_sib: Instructions.IDIVD(Memory.mem_readd(ea32sib(switchBlock)));reg_eip+=switchBlock.eipCount;continue;

                            case Inst.CLC:Flags.FillFlags();SETFLAGBIT(CF,false);reg_eip+=switchBlock.eipCount;continue;
                            case Inst.STC:Flags.FillFlags();SETFLAGBIT(CF,true);reg_eip+=switchBlock.eipCount;continue;
                            case Inst.CLI:if (CPU.CPU_CLI()) {RUNEXCEPTION();block=null;break;}reg_eip+=switchBlock.eipCount;continue;
                            case Inst.STI:if (CPU.CPU_STI()) {RUNEXCEPTION();block=null;break;}reg_eip+=switchBlock.eipCount;if (GETFLAG(IF)!=0 && Pic.PIC_IRQCheck!=0) {Flags.FillFlags();return Callback.CBRET_NONE;} continue;
                            case Inst.CLD:SETFLAGBIT(DF,false); CPU.cpu.direction=1;reg_eip+=switchBlock.eipCount;continue;
                            case Inst.STD:SETFLAGBIT(DF,true); CPU.cpu.direction=-1;reg_eip+=switchBlock.eipCount;continue;

                            case Inst.INC_E8_16: eaa=ea16(switchBlock);Memory.mem_writeb(eaa, Instructions.INCB(Memory.mem_readb(eaa)));reg_eip+=switchBlock.eipCount;continue;
                            case Inst.INC_E8_16_fast: eaa=ea16(switchBlock);Memory.mem_writeb(eaa, Memory.mem_readb(eaa)+1);reg_eip+=switchBlock.eipCount;continue;
                            case Inst.INC_E8_32: eaa=ea32(switchBlock);Memory.mem_writeb(eaa, Instructions.INCB(Memory.mem_readb(eaa)));reg_eip+=switchBlock.eipCount;continue;
                            case Inst.INC_E8_32_fast: eaa=ea32(switchBlock);Memory.mem_writeb(eaa, Memory.mem_readb(eaa)+1);reg_eip+=switchBlock.eipCount;continue;
                            case Inst.INC_E8_32_sib: eaa=ea32sib(switchBlock);Memory.mem_writeb(eaa, Instructions.INCB(Memory.mem_readb(eaa)));reg_eip+=switchBlock.eipCount;continue;
                            case Inst.INC_E8_32_sib_fast: eaa=ea32sib(switchBlock);Memory.mem_writeb(eaa, Memory.mem_readb(eaa)+1);reg_eip+=switchBlock.eipCount;continue;
                            case Inst.INC_E16_16: eaa=ea16(switchBlock);Memory.mem_writew(eaa, Instructions.INCW(Memory.mem_readw(eaa)));reg_eip+=switchBlock.eipCount;continue;
                            case Inst.INC_E16_16_fast: eaa=ea16(switchBlock);Memory.mem_writew(eaa, Memory.mem_readw(eaa)+1);reg_eip+=switchBlock.eipCount;continue;
                            case Inst.INC_E16_32: eaa=ea32(switchBlock);Memory.mem_writew(eaa, Instructions.INCW(Memory.mem_readw(eaa)));reg_eip+=switchBlock.eipCount;continue;
                            case Inst.INC_E16_32_fast: eaa=ea32(switchBlock);Memory.mem_writew(eaa, Memory.mem_readw(eaa)+1);reg_eip+=switchBlock.eipCount;continue;
                            case Inst.INC_E16_32_sib: eaa=ea32sib(switchBlock);Memory.mem_writew(eaa, Instructions.INCW(Memory.mem_readw(eaa)));reg_eip+=switchBlock.eipCount;continue;
                            case Inst.INC_E16_32_sib_fast: eaa=ea32sib(switchBlock);Memory.mem_writew(eaa, Memory.mem_readw(eaa)+1);reg_eip+=switchBlock.eipCount;continue;
                            case Inst.INC_E32_16: eaa=ea16(switchBlock);Memory.mem_writed(eaa, Instructions.INCD(Memory.mem_readd(eaa)));reg_eip+=switchBlock.eipCount;continue;
                            case Inst.INC_E32_16_fast: eaa=ea16(switchBlock);Memory.mem_writed(eaa, Memory.mem_readd(eaa)+1);reg_eip+=switchBlock.eipCount;continue;
                            case Inst.INC_E32_32: eaa=ea32(switchBlock);Memory.mem_writed(eaa, Instructions.INCD(Memory.mem_readd(eaa)));reg_eip+=switchBlock.eipCount;continue;
                            case Inst.INC_E32_32_fast: eaa=ea32(switchBlock);Memory.mem_writed(eaa, Memory.mem_readd(eaa)+1);reg_eip+=switchBlock.eipCount;continue;
                            case Inst.INC_E32_32_sib: eaa=ea32sib(switchBlock);Memory.mem_writed(eaa, Instructions.INCD(Memory.mem_readd(eaa)));reg_eip+=switchBlock.eipCount;continue;
                            case Inst.INC_E32_32_sib_fast: eaa=ea32sib(switchBlock);Memory.mem_writed(eaa, Memory.mem_readd(eaa)+1);reg_eip+=switchBlock.eipCount;continue;

                            case Inst.DEC_E8_16: eaa=ea16(switchBlock);Memory.mem_writeb(eaa, Instructions.DECB(Memory.mem_readb(eaa)));reg_eip+=switchBlock.eipCount;continue;
                            case Inst.DEC_E8_16_fast: eaa=ea16(switchBlock);Memory.mem_writeb(eaa, Memory.mem_readb(eaa)-1);reg_eip+=switchBlock.eipCount;continue;
                            case Inst.DEC_E8_32: eaa=ea32(switchBlock);Memory.mem_writeb(eaa, Instructions.DECB(Memory.mem_readb(eaa)));reg_eip+=switchBlock.eipCount;continue;
                            case Inst.DEC_E8_32_fast: eaa=ea32(switchBlock);Memory.mem_writeb(eaa, Memory.mem_readb(eaa)-1);reg_eip+=switchBlock.eipCount;continue;
                            case Inst.DEC_E8_32_sib: eaa=ea32sib(switchBlock);Memory.mem_writeb(eaa, Instructions.DECB(Memory.mem_readb(eaa)));reg_eip+=switchBlock.eipCount;continue;
                            case Inst.DEC_E8_32_sib_fast: eaa=ea32sib(switchBlock);Memory.mem_writeb(eaa, Memory.mem_readb(eaa)-1);reg_eip+=switchBlock.eipCount;continue;
                            case Inst.DEC_E16_16: eaa=ea16(switchBlock);Memory.mem_writew(eaa, Instructions.DECW(Memory.mem_readw(eaa)));reg_eip+=switchBlock.eipCount;continue;
                            case Inst.DEC_E16_16_fast: eaa=ea16(switchBlock);Memory.mem_writew(eaa, Memory.mem_readw(eaa)-1);reg_eip+=switchBlock.eipCount;continue;
                            case Inst.DEC_E16_32: eaa=ea32(switchBlock);Memory.mem_writew(eaa, Instructions.DECW(Memory.mem_readw(eaa)));reg_eip+=switchBlock.eipCount;continue;
                            case Inst.DEC_E16_32_fast: eaa=ea32(switchBlock);Memory.mem_writew(eaa, Memory.mem_readw(eaa)-1);reg_eip+=switchBlock.eipCount;continue;
                            case Inst.DEC_E16_32_sib: eaa=ea32sib(switchBlock);Memory.mem_writew(eaa, Instructions.DECW(Memory.mem_readw(eaa)));reg_eip+=switchBlock.eipCount;continue;
                            case Inst.DEC_E16_32_sib_fast: eaa=ea32sib(switchBlock);Memory.mem_writew(eaa, Memory.mem_readw(eaa)-1);reg_eip+=switchBlock.eipCount;continue;
                            case Inst.DEC_E32_16: eaa=ea16(switchBlock);Memory.mem_writed(eaa, Instructions.DECD(Memory.mem_readd(eaa)));reg_eip+=switchBlock.eipCount;continue;
                            case Inst.DEC_E32_16_fast: eaa=ea16(switchBlock);Memory.mem_writed(eaa, Memory.mem_readd(eaa)-1);reg_eip+=switchBlock.eipCount;continue;
                            case Inst.DEC_E32_32: eaa=ea32(switchBlock);Memory.mem_writed(eaa, Instructions.DECD(Memory.mem_readd(eaa)));reg_eip+=switchBlock.eipCount;continue;
                            case Inst.DEC_E32_32_fast: eaa=ea32(switchBlock);Memory.mem_writed(eaa, Memory.mem_readd(eaa)-1);reg_eip+=switchBlock.eipCount;continue;
                            case Inst.DEC_E32_32_sib: eaa=ea32sib(switchBlock);Memory.mem_writed(eaa, Instructions.DECD(Memory.mem_readd(eaa)));reg_eip+=switchBlock.eipCount;continue;
                            case Inst.DEC_E32_32_sib_fast: eaa=ea32sib(switchBlock);Memory.mem_writed(eaa, Memory.mem_readd(eaa)-1);reg_eip+=switchBlock.eipCount;continue;
                            case Inst.CALLBACK: reg_eip+=switchBlock.eipCount; return switchBlock.value;
                            case Inst.CALL16_R16: tmp = reg_eip+switchBlock.value; CPU.CPU_Push16(tmp);reg_eip=switchBlock.r1.word();block=null;break;
                            case Inst.CALL16_E16_16: tmp = reg_eip+switchBlock.value; tmp2=Memory.mem_readw(ea16(switchBlock));CPU.CPU_Push16(tmp);reg_eip=tmp2;block=null;break;
                            case Inst.CALL16_E16_32: tmp = reg_eip+switchBlock.value; tmp2=Memory.mem_readw(ea32(switchBlock));CPU.CPU_Push16(tmp);reg_eip=tmp2;block=null;break;
                            case Inst.CALL16_E16_32sib: tmp = reg_eip+switchBlock.value; tmp2=Memory.mem_readw(ea32sib(switchBlock));CPU.CPU_Push16(tmp);reg_eip=tmp2;block=null;break;
                            case Inst.CALL16_EP_E16: eaa=ea16(switchBlock);CPU.CPU_CALL(false, Memory.mem_readw(eaa+2), Memory.mem_readw(eaa), reg_eip+switchBlock.eipCount);block=null;break;
                            case Inst.CALL16_EP_E32: eaa=ea32(switchBlock);CPU.CPU_CALL(false, Memory.mem_readw(eaa+2), Memory.mem_readw(eaa), reg_eip+switchBlock.eipCount);block=null;break;
                            case Inst.CALL16_EP_E32sib: eaa=ea32sib(switchBlock);CPU.CPU_CALL(false, Memory.mem_readw(eaa+2), Memory.mem_readw(eaa), reg_eip+switchBlock.eipCount);block=null;break;
                            case Inst.JMP16_R16: reg_eip=switchBlock.r1.word();block=null;break;
                            case Inst.JMP16_E16_16: reg_eip=Memory.mem_readw(ea16(switchBlock));block=null;break;
                            case Inst.JMP16_E16_32: reg_eip=Memory.mem_readw(ea32(switchBlock));block=null;break;
                            case Inst.JMP16_E16_32sib: reg_eip=Memory.mem_readw(ea32sib(switchBlock));block=null;break;
                            case Inst.JMP16_EP_E16: eaa=ea16(switchBlock);CPU.CPU_JMP(false, Memory.mem_readw(eaa+2), Memory.mem_readw(eaa), reg_eip+switchBlock.eipCount);if (GETFLAG(TF)!=0) {CPU.cpudecoder=CPU_Core_Switch_Trap_Run;return Callback.CBRET_NONE;}block=null;break;
                            case Inst.JMP16_EP_E32: eaa=ea32(switchBlock);CPU.CPU_JMP(false, Memory.mem_readw(eaa+2), Memory.mem_readw(eaa), reg_eip+switchBlock.eipCount);if (GETFLAG(TF)!=0) {CPU.cpudecoder=CPU_Core_Switch_Trap_Run;return Callback.CBRET_NONE;}block=null;break;
                            case Inst.JMP16_EP_E32sib: eaa=ea32sib(switchBlock);CPU.CPU_JMP(false, Memory.mem_readw(eaa+2), Memory.mem_readw(eaa), reg_eip+switchBlock.eipCount);if (GETFLAG(TF)!=0) {CPU.cpudecoder=CPU_Core_Switch_Trap_Run;return Callback.CBRET_NONE;}block=null;break;
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
