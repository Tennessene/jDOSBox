package jdos.cpu.core_dynamic;

import jdos.cpu.core_share.Constants;
import jdos.hardware.Memory;
import jdos.cpu.*;

public class Helper extends CPU_Regs {
    static private Core_dynamic.CodePageHandlerDynRecRef codeRef = new Core_dynamic.CodePageHandlerDynRecRef();
    public static final DynDecode decode = new DynDecode();
    static protected boolean EA16 = false;
    static protected int prefixes = 0;
    static protected int opcode_index = 0;

    static protected final long[] AddrMaskTable={0x0000ffffl,0xffffffffl};
    
    protected static final int OPCODE_NONE=0x000;
    protected static final int OPCODE_0F=0x100;
    protected static final int OPCODE_SIZE=0x200;
    
    static final protected boolean CPU_TRAP_CHECK = true;
    static protected final boolean CPU_PIC_CHECK = false;

    public final static int RESULT_HANDLED = 0;
    public final static int RESULT_ILLEGAL_INSTRUCTION = 1;
    public final static int RESULT_CALLBACK = 2;
    public final static int RESULT_CONTINUE = 3;
    public final static int RESULT_RETURN = 4;
    public final static int RESULT_ANOTHER = 5;
    public final static int RESULT_JUMP = 6;
    public final static int RESULT_CONTINUE_SEG = 7;

    public static long iGETIP() {
        return decode.code - CPU.Segs_CSphys;
    }

    protected static int JumpCond16_b(boolean COND, long eip, int off) {
        reg_eip = eip;
        if (COND) {
            reg_ip(reg_ip()+off+1);
            return Constants.BR_Link1;
        }
        reg_ip(reg_ip()+1);
        return Constants.BR_Link2;
    }
    protected static int JumpCond16_w(boolean COND, long eip, int off) {
        reg_eip = eip;
        if (COND) {
            reg_ip(reg_ip()+off+2);
            return Constants.BR_Link1;
        }
        reg_ip(reg_ip()+2);
        return Constants.BR_Link2;
    }

    protected static int JumpCond32_b(boolean COND, long eip, int off) {
        reg_eip = eip;
        if (COND) {
            reg_eip(reg_eip()+off+1);
            return Constants.BR_Link1;
        }
        reg_eip(reg_eip()+1);
        return Constants.BR_Link2;
    }

    protected static int JumpCond32_d(boolean COND, long eip, long off) {
        reg_eip = eip;
        if (COND) {
            reg_eip(reg_eip()+off+4);
            return Constants.BR_Link1;
        }
        reg_eip(reg_eip()+4);
        return Constants.BR_Link2;
    }

    static void decode_advancepage() {
        // Advance to the next page
        decode.active_block.page.end=4095;
        // trigger possible page fault here
        decode.page.first++;
        /*Bitu*/long faddr=(long)decode.page.first << 12;
        Memory.mem_readb(faddr);
        codeRef.value = decode.page.code;
        Decoder_basic.MakeCodePage(faddr,codeRef);
        decode.page.code = codeRef.value;
        CacheBlockDynRec newblock=Cache.cache_getblock();
        decode.active_block.crossblock=newblock;
        newblock.crossblock=decode.active_block;
        decode.active_block=newblock;
        decode.active_block.page.start=0;
        decode.page.code.AddCrossBlock(decode.active_block);
        decode.page.wmap=decode.page.code.write_map;
        decode.page.invmap=decode.page.code.invalidation_map;
        decode.page.index=0;
    }

    // fetch the next byte of the instruction stream
    static /*Bit8u*/byte decode_fetchbs() {
        return (byte)decode_fetchb();
    }
    static /*Bit8u*/short decode_fetchb() {
        if (decode.page.index>=4096) {
            decode_advancepage();
        }
        if (decode.page.invmap!=null && decode.page.invmap.p[decode.page.index]>=4) {
            decode.modifiedAlot = true;
        }
        decode.page.wmap.p[decode.page.index]+=0x01;
        decode.page.index++;
        decode.code+=1;
        return Memory.mem_readb(decode.code-1);
    }
    static short decode_fetchws() {
        return (short)decode_fetchw();
    }
    // fetch the next word of the instruction stream
    static /*Bit16u*/int decode_fetchw() {
        if (decode.page.index>=4095) {
            /*Bit16u*/int val=decode_fetchb();
            val|=decode_fetchb() << 8;
            return val;
        }
        if (decode.page.invmap!=null && (decode.page.invmap.p[decode.page.index]>=4 || decode.page.invmap.p[decode.page.index+1]>=4)) {
            decode.modifiedAlot = true;
        }
        decode.page.wmap.p[decode.page.index]+=0x01;
        decode.page.wmap.p[decode.page.index+1]+=0x01;
        decode.code+=2;decode.page.index+=2;
        return Memory.mem_readw(decode.code-2);
    }
    static int decode_fetchds() {
        return (int)decode_fetchd();
    }
    // fetch the next dword of the instruction stream
    static /*Bit32u*/long decode_fetchd() {
        if (decode.page.index>=4093) {
            /*Bit32u*/long val=decode_fetchb();
            val|=decode_fetchb() << 8;
            val|=decode_fetchb() << 16;
            val|=decode_fetchb() << 24;
            return val;
            /* Advance to the next page */
        }
        if (decode.page.invmap!=null && (decode.page.invmap.p[decode.page.index]>=4 || decode.page.invmap.p[decode.page.index+1]>=4 || decode.page.invmap.p[decode.page.index+2]>=4 || decode.page.invmap.p[decode.page.index+3]>=4)) {
            decode.modifiedAlot = true;
        }
        decode.page.wmap.p[decode.page.index]+=0x01;
        decode.page.wmap.p[decode.page.index+1]+=0x01;
        decode.page.wmap.p[decode.page.index+2]+=0x01;
        decode.page.wmap.p[decode.page.index+3]+=0x01;
        decode.code+=4;decode.page.index+=4;
        return Memory.mem_readd(decode.code-4);
    }
}
