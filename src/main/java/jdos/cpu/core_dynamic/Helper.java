package jdos.cpu.core_dynamic;

import jdos.cpu.CPU_Regs;
import jdos.cpu.PageFaultException;
import jdos.hardware.Memory;
import jdos.hardware.RAM;
import jdos.misc.Log;

public class Helper extends CPU_Regs {
    public static final DynDecode decode = new DynDecode();
    static protected boolean EA16 = false;
    static protected int prefixes = 0;
    static protected int opcode_index = 0;

    static protected final long[] AddrMaskTable={0x0000ffffl,0xffffffffl};
    static protected final int[] AddrMaskTable1={0x0000ffff,0xffffffff};
    
    protected static final int OPCODE_NONE=0x000;
    protected static final int OPCODE_0F=0x100;
    protected static final int OPCODE_SIZE=0x200;
    
    static final protected boolean CPU_TRAP_CHECK = true;
    static protected final boolean CPU_PIC_CHECK = true;

    public final static int RESULT_HANDLED = 0;
    public final static int RESULT_ILLEGAL_INSTRUCTION = 1;
    public final static int RESULT_CALLBACK = 2;
    public final static int RESULT_CONTINUE = 3;
    public final static int RESULT_RETURN = 4;
    public final static int RESULT_ANOTHER = 5;
    public final static int RESULT_JUMP = 6;
    public final static int RESULT_CONTINUE_SEG = 7;

    // fetch the next byte of the instruction stream
    static public int decode_fetchbs() {
        return (byte)decode_fetchb();
    }

    static public int decode_fetchb() {
        if (decode.page.index>=4096) {
            decode_advancepage();
        }
        if (decode.page.invmap!=null && decode.page.invmap.p[decode.page.index]>=4) {
            decode.modifiedAlot = true;
        }
        decode.page.wmap.p[decode.page.index]+=0x01;
        decode.page.index++;
        decode.code+=1;
        return RAM.readb(decode.tlb + decode.code - 1);
    }

    static void decode_advancepage() {
        // trigger possible page fault here
        int faddr=(decode.page.first+1) << 12;
        Memory.mem_readb(faddr);
    	// Advance to the next page
    	decode.active_block.page.end=4095;
    	decode.page.first++;
        decode.page.code = Decoder_basic.MakeCodePage(faddr);
    	CacheBlockDynRec newblock=Cache.cache_getblock();
    	decode.active_block.crossblock=newblock;
    	newblock.crossblock=decode.active_block;
    	decode.active_block=newblock;
    	decode.active_block.page.start=0;
    	decode.page.code.AddCrossBlock(decode.active_block);
    	decode.page.wmap=decode.page.code.write_map;
    	decode.page.invmap=decode.page.code.invalidation_map;
    	decode.page.index=0;
        decode.setTLB(faddr);
    }

    static public void decode_putback(int count) {
        for (int i=0;i<count;i++) {
            decode.page.index--;
            decode.code--;
            // :TODO: handle page change
            if (decode.page.index<0) {
                Log.exit("Dynamic Core:  Self modifying code across page boundries not implemented yet");
            }
            decode.page.wmap.p[decode.page.index]-=0x01;
        }
    }
    static public int decode_fetchws() {
        return (short)decode_fetchw();
    }
    // fetch the next word of the instruction stream
    static public int decode_fetchw() {
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
        return RAM.readw(decode.tlb + decode.code - 2);
    }
    static public int decode_fetchds() {
        return decode_fetchd();
    }

    // fetch the next dword of the instruction stream
    static public int decode_fetchd() {
        if (decode.page.index>=4093) {
            /*Bit32u*/int val=decode_fetchb();
            val|=decode_fetchb() << 8;
            val|=decode_fetchb() << 16;
            val|=(long)decode_fetchb() << 24;
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
        return RAM.readd(decode.tlb + decode.code - 4);
    }
}
