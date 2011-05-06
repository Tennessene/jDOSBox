package jdos.cpu.core_dynrec2;

import javassist.*;

import java.net.URLClassLoader;
import java.util.Hashtable;

import jdos.cpu.*;
import jdos.misc.Log;
import jdos.types.LogTypes;
import jdos.types.LogSeverities;

final public class Decoder extends Decoder_instructions {
    static public final CPU.Descriptor desc=new CPU.Descriptor();

    static protected final long[] AddrMaskTable={0x0000ffffl,0xffffffffl};

    protected static final int OPCODE_NONE=0x000;
    protected static final int OPCODE_0F=0x100;
    protected static final int OPCODE_SIZE=0x200;

    static void dyn_closeblock() {
        //Shouldn't create empty block normally but let's do it like this
        //dyn_fill_blocks();
        //cache_block_before_close();
        Cache.cache_closeblock();
        //cache_block_closing(decode.block->cache.start,decode.block->cache.size);
    }

    public static int count=0;

    static private ClassPool pool = ClassPool.getDefault();
    static java.security.MessageDigest md;
    static {
        pool.importPackage("jdos.cpu.core_dynrec2");
        pool.importPackage("jdos.cpu");
        pool.importPackage("jdos.debug");
        pool.importPackage("jdos.fpu");
        pool.importPackage("jdos.hardware");
        pool.importPackage("jdos.util");
        pool.importPackage("jdos.cpu.core_normal");
        pool.importPackage("jdos.cpu.core_share");
        try {
            md = java.security.MessageDigest.getInstance("MD5");
        } catch (Exception e) {

        }
    }

    public static CacheBlockDynRec CreateCacheBlock(CodePageHandlerDynRec codepage,/*PhysPt*/long start,/*Bitu*/int max_opcodes) {
        // initialize a load of variables
        decode.code_start=start;
        decode.code=start;
        decode.page.code=codepage;
        decode.page.index=(int)(start&4095);
        decode.page.wmap=codepage.write_map;
        decode.page.invmap=codepage.invalidation_map;
        decode.page.first=(int)(start >> 12);
        decode.active_block=decode.block=Cache.cache_openblock();
        decode.block.page.start=decode.page.index;
        codepage.AddCacheBlock(decode.block);

        Decoder_basic.InitFlagsOptimization();

        StringBuffer method = new StringBuffer();
        try {
            method.append("public int call() {");
            decode.cycles = 0;
            int result = 0;
            int callback = 0;
            int opcode_index = 0;
            boolean segChanged = false;

            if (CPU.cpu.code.big) {
                opcode_index=0x200;
                prefixes=1;
                EA16 = false;
            } else {
                opcode_index=0;
                prefixes=0;
                EA16 = true;
            }

            decode.modifiedAlot = false;
            while (max_opcodes-->0 && result==0) {
                // Init prefixes
                decode.big_addr=CPU.cpu.code.big;
                decode.big_op=CPU.cpu.code.big;
                decode.seg_prefix=0;
                decode.seg_prefix_used=false;
                decode.rep=Decoder_basic.REP_NONE;
                decode.cycles++;
                decode.op_start=decode.code;

                if (decode.modifiedAlot) {
                    result = RESULT_MODIFIED_INSTRUCTION;
                    break;
                }

                int opcode=opcode_index+decode_fetchb();
                //method.append("System.out.println(");method.append(opcode);method.append(");");
//                method.append("try { Debug.start(Debug.TYPE_CPU, ");method.append(opcode);method.append(");");
//                try {
                switch (opcode) {
                    /* ADD Eb,Gb */
                    case 0x00:
                    case 0x200:
                        ebgb(method, decode_fetchb(), "Instructions.ADDB", true);
                        break;
                    /* ADD Ew,Gw */
                    case 0x01:
                        ewgw(method, decode_fetchb(), "Instructions.ADDW", true);
                        break;
                    /* ADD Gb,Eb */
                    case 0x02:
                    case 0x202:
                        gbeb(method, decode_fetchb(), "Instructions.ADDB", true);
                        break;
                    /* ADD Gw,Ew */
                    case 0x03:
                        gwew(method, decode_fetchb(), "Instructions.ADDW", true);
                        break;
                    /* ADD AL,Ib */
                    case 0x04:
                    case 0x204:
                        alib(method, "Instructions.ADDB", true);
                        break;
                    /* ADD AX,Iw */
                    case 0x05:
                        awiw(method, "Instructions.ADDW", true);
                        break;
                    /* PUSH ES */
                    case 0x06:
                        method.append("CPU.CPU_Push16((int)CPU.Segs_ESval);");
                        break;
                    /* POP ES */
                    case 0x07:
                        method.append("if (CPU.CPU_PopSegES(false)) ");
                        RUNEXCEPTION(method);
                        break;
                    /* OR Eb,Gb */
                    case 0x08:
                    case 0x208:
                        ebgb(method, decode_fetchb(), "Instructions.ORB", true);
                        break;
                    /* OR Ew,Gw */
                    case 0x09:
                        ewgw(method, decode_fetchb(), "Instructions.ORW", true);
                        break;
                    /* OR Gb,Eb */
                    case 0x0a:
                    case 0x20a:
                        gbeb(method, decode_fetchb(), "Instructions.ORB", true);
                        break;
                    /* OR Gw,Ew */
                    case 0x0b:
                        gwew(method, decode_fetchb(), "Instructions.ORW", true);
                        break;
                    /* OR AL,Ib */
                    case 0x0c:
                    case 0x20c:
                        alib(method, "Instructions.ORB", true);
                        break;
                    /* OR AX,Iw */
                    case 0x0d:
                        awiw(method, "Instructions.ORW", true);
                        break;
                    /* PUSH CS */
                    case 0x0e:
                        method.append("CPU.CPU_Push16((int)CPU.Segs_CSval);");
                        break;
                    /* 2 byte opcodes*/
                    case 0x0f:
                    case 0x20f:
                        opcode_index|=OPCODE_0F;
                        max_opcodes++;
                        continue;
                    /* ADC Eb,Gb */
                    case 0x10:
                    case 0x210:
                        ebgb(method, decode_fetchb(), "Instructions.ADCB", true);
                        break;
                    /* ADC Ew,Gw */
                    case 0x11:
                        ewgw(method, decode_fetchb(), "Instructions.ADCW", true);
                        break;
                    /* ADC Gb,Eb */
                    case 0x12:
                    case 0x212:
                        gbeb(method, decode_fetchb(), "Instructions.ADCB", true);
                        break;
                    /* ADC Gw,Ew */
                    case 0x13:
                        gwew(method, decode_fetchb(), "Instructions.ADCW", true);
                        break;
                    /* ADC AL,Ib */
                    case 0x14:
                    case 0x214:
                        alib(method, "Instructions.ADCB", true);
                        break;
                    /* ADC AX,Iw */
                    case 0x15:
                        awiw(method, "Instructions.ADCW", true);
                        break;
                    /* PUSH SS */
                    case 0x16:
                        method.append("CPU.CPU_Push16((int)CPU.Segs_SSval);");
                        break;
                    /* POP SS */
                    case 0x17:
                        method.append("if (CPU.CPU_PopSegSS(false)) ");
                        RUNEXCEPTION(method);
                        //Always do another instruction
                        max_opcodes++;
                        segChanged = true;
                        break;
                    /* SBB Eb,Gb */
                    case 0x18:
                    case 0x218:
                        ebgb(method, decode_fetchb(), "Instructions.SBBB", true);
                        break;
                    /* SBB Ew,Gw */
                    case 0x19:
                        ewgw(method, decode_fetchb(), "Instructions.SBBW", true);
                        break;
                    /* SBB Gb,Eb */
                    case 0x1a:
                    case 0x21a:
                        gbeb(method, decode_fetchb(), "Instructions.SBBB", true);
                        break;
                    /* SBB Gw,Ew */
                    case 0x1b:
                        gwew(method, decode_fetchb(), "Instructions.SBBW", true);
                        break;
                    /* SBB AL,Ib */
                    case 0x1c:
                    case 0x21c:
                        alib(method, "Instructions.SBBB", true);
                        break;
                    /* SBB AX,Iw */
                    case 0x1d:
                        awiw(method, "Instructions.SBBW", true);
                        break;
                    /* PUSH DS */
                    case 0x1e:
                        method.append("CPU.CPU_Push16((int)CPU.Segs_DSval);");
                        break;
                    /* POP DS */
                    case 0x1f:
                        method.append("if (CPU.CPU_PopSegDS(false)) ");
                        RUNEXCEPTION(method);
                        segChanged = true;
                        break;
                    /* AND Eb,Gb */
                    case 0x20:
                    case 0x220:
                        ebgb(method, decode_fetchb(), "Instructions.ANDB", true);
                        break;
                    /* AND Ew,Gw */
                    case 0x21:
                        ewgw(method, decode_fetchb(), "Instructions.ANDW", true);
                        break;
                    /* AND Gb,Eb */
                    case 0x22:
                    case 0x222:
                        gbeb(method, decode_fetchb(), "Instructions.ANDB", true);
                        break;
                    /* AND Gw,Ew */
                    case 0x23:
                        gwew(method, decode_fetchb(), "Instructions.ANDW", true);
                        break;
                    /* AND AL,Ib */
                    case 0x24:
                    case 0x224:
                        alib(method, "Instructions.ANDB", true);
                        break;
                    /* AND AX,Iw */
                    case 0x25:
                        awiw(method, "Instructions.ANDW", true);
                        break;
                    /* SEG ES: */
                    case 0x26:
                    case 0x226:
                        method.append("Core.DO_PREFIX_SEG_ES();");
                        segChanged = true;
                        max_opcodes++;
                        continue;
                    /* DAA */
                    case 0x27:
                    case 0x227:
                        method.append("Instructions.DAA();");
                        break;
                    /* SUB Eb,Gb */
                    case 0x28:
                    case 0x228:
                        ebgb(method, decode_fetchb(), "Instructions.SUBB", true);
                        break;
                    /* SUB Ew,Gw */
                    case 0x29:
                        ewgw(method, decode_fetchb(), "Instructions.SUBW", true);
                        break;
                    /* SUB Gb,Eb */
                    case 0x2a:
                    case 0x22a:
                        gbeb(method, decode_fetchb(), "Instructions.SUBB", true);
                        break;
                    /* SUB Gw,Ew */
                    case 0x2b:
                        gwew(method, decode_fetchb(), "Instructions.SUBW", true);
                        break;
                    /* SUB AL,Ib */
                    case 0x2c:
                    case 0x22c:
                        alib(method, "Instructions.SUBB", true);
                        break;
                    /* SUB AX,Iw */
                    case 0x2d:
                        awiw(method, "Instructions.SUBW", true);
                        break;
                    /* SEG CS: */
                    case 0x2e:
                    case 0x22e:
                        method.append("Core.DO_PREFIX_SEG_CS();");
                        segChanged = true;
                        max_opcodes++;
                        continue;
                    /* DAS */
                    case 0x2f:
                    case 0x22f:
                        method.append("Instructions.DAS();");
                        break;
                    /* XOR Eb,Gb */
                    case 0x30:
                    case 0x230:
                        ebgb(method, decode_fetchb(), "Instructions.XORB", true);
                        break;
                    /* XOR Ew,Gw */
                    case 0x31:
                        ewgw(method, decode_fetchb(), "Instructions.XORW", true);
                        break;
                    /* XOR Gb,Eb */
                    case 0x32:
                    case 0x232:
                        gbeb(method, decode_fetchb(), "Instructions.XORB", true);
                        break;
                    /* XOR Gw,Ew */
                    case 0x33:
                        gwew(method, decode_fetchb(), "Instructions.XORW", true);
                        break;
                    /* XOR AL,Ib */
                    case 0x34:
                    case 0x234:
                        alib(method, "Instructions.XORB", true);
                        break;
                    /* XOR AX,Iw */
                    case 0x35:
                        awiw(method, "Instructions.XORW", true);
                        break;
                    /* SEG SS: */
                    case 0x36:
                    case 0x236:
                        method.append("Core.DO_PREFIX_SEG_SS();");
                        segChanged = true;
                        max_opcodes++;
                        continue;
                    /* AAA */
                    case 0x37:
                    case 0x237:
                        method.append("Instructions.AAA();");
                        break;
                    /* CMP Eb,Gb */
                    case 0x38:
                    case 0x238:
                        ebgb(method, decode_fetchb(), "Instructions.CMPB", false);
                        break;
                    /* CMP Ew,Gw */
                    case 0x39:
                        ewgw(method, decode_fetchb(), "Instructions.CMPW", false);
                        break;
                    /* CMP Gb,Eb */
                    case 0x3a:
                    case 0x23a:
                        gbeb(method, decode_fetchb(), "Instructions.CMPB", false);
                        break;
                    /* CMP Gw,Ew */
                    case 0x3b:
                        gwew(method, decode_fetchb(), "Instructions.CMPW", false);
                        break;
                    /* CMP AL,Ib */
                    case 0x3c:
                    case 0x23c:
                        alib(method, "Instructions.CMPB", false);
                        break;
                    /* CMP AX,Iw */
                    case 0x3d:
                        awiw(method, "Instructions.CMPW", false);
                        break;
                    /* SEG DS: */
                    case 0x3e:
                    case 0x23e:
                        method.append("Core.DO_PREFIX_SEG_DS();");
                        segChanged = true;
                        max_opcodes++;
                        continue;
                    /* AAS */
                    case 0x3f:
                    case 0x23f:
                        method.append("Instructions.AAS();");
                        break;
                    /* INC AX */
                    case 0x40:
                        incw(method, 0);
                        break;
                    /* INC CX */
                    case 0x41:
                        incw(method, 1);
                        break;
                    /* INC DX */
                    case 0x42:
                        incw(method, 2);
                        break;
                    /* INC BX */
                    case 0x43:
                        incw(method, 3);
                        break;
                    /* INC SP */
                    case 0x44:
                        incw(method, 4);
                        break;
                    /* INC BP */
                    case 0x45:
                        incw(method, 5);
                        break;
                    /* INC SI */
                    case 0x46:
                        incw(method, 6);
                        break;
                    /* INC DI */
                    case 0x47:
                        incw(method, 7);
                        break;
                    /* DEC AX */
                    case 0x48:
                        decw(method, 0);
                        break;
                    /* DEC CX */
                    case 0x49:
                        decw(method, 1);
                        break;
                    /* DEC DX */
                    case 0x4a:
                        decw(method, 2);
                        break;
                    /* DEC BX */
                    case 0x4b:
                        decw(method, 3);
                        break;
                    /* DEC SP */
                    case 0x4c:
                        decw(method, 4);
                        break;
                    /* DEC BP */
                    case 0x4d:
                        decw(method, 5);
                        break;
                    /* DEC SI */
                    case 0x4e:
                        decw(method, 6);
                        break;
                    /* DEC DI */
                    case 0x4f:
                        decw(method, 7);
                        break;
                    /* PUSH AX */
                    case 0x50:
                        method.append("CPU.CPU_Push16(CPU_Regs.reg_eax.word());");
                        break;
                    /* PUSH CX */
                    case 0x51:
                        method.append("CPU.CPU_Push16(CPU_Regs.reg_ecx.word());");
                        break;
                    /* PUSH DX */
                    case 0x52:
                        method.append("CPU.CPU_Push16(CPU_Regs.reg_edx.word());");
                        break;
                    /* PUSH BX */
                    case 0x53:
                        method.append("CPU.CPU_Push16(CPU_Regs.reg_ebx.word());");
                        break;
                    /* PUSH SP */
                    case 0x54:
                        method.append("CPU.CPU_Push16(CPU_Regs.reg_esp.word());");
                        break;
                    /* PUSH BP */
                    case 0x55:
                        method.append("CPU.CPU_Push16(CPU_Regs.reg_ebp.word());");
                        break;
                    /* PUSH SI */
                    case 0x56:
                        method.append("CPU.CPU_Push16(CPU_Regs.reg_esi.word());");
                        break;
                    /* PUSH DI */
                    case 0x57:
                        method.append("CPU.CPU_Push16(CPU_Regs.reg_edi.word());");
                        break;
                    /* POP AX */
                    case 0x58:
                        method.append("CPU_Regs.reg_eax.word(CPU.CPU_Pop16());");
                        break;
                    /* POP CX */
                    case 0x59:
                        method.append("CPU_Regs.reg_ecx.word(CPU.CPU_Pop16());");
                        break;
                    /* POP DX */
                    case 0x5a:
                        method.append("CPU_Regs.reg_edx.word(CPU.CPU_Pop16());");
                        break;
                    /* POP BX */
                    case 0x5b:
                        method.append("CPU_Regs.reg_ebx.word(CPU.CPU_Pop16());");
                        break;
                    /* POP SP */
                    case 0x5c:
                        method.append("CPU_Regs.reg_esp.word(CPU.CPU_Pop16());");
                        break;
                    /* POP BP */
                    case 0x5d:
                        method.append("CPU_Regs.reg_ebp.word(CPU.CPU_Pop16());");
                        break;
                    /* POP SI */
                    case 0x5e:
                        method.append("CPU_Regs.reg_esi.word(CPU.CPU_Pop16());");
                        break;
                    /* POP DI */
                    case 0x5f:
                        method.append("CPU_Regs.reg_edi.word(CPU.CPU_Pop16());");
                        break;
                    /* PUSHA */
                    case 0x60:
                        method.append("{int old_sp=CPU_Regs.reg_esp.word();");
                        method.append("CPU.CPU_Push16(CPU_Regs.reg_eax.word());CPU.CPU_Push16(CPU_Regs.reg_ecx.word());CPU.CPU_Push16(CPU_Regs.reg_edx.word());CPU.CPU_Push16(CPU_Regs.reg_ebx.word());");
                        method.append("CPU.CPU_Push16(old_sp);CPU.CPU_Push16(CPU_Regs.reg_ebp.word());CPU.CPU_Push16(CPU_Regs.reg_esi.word());CPU.CPU_Push16(CPU_Regs.reg_edi.word());}");
                        break;
                    /* POPA */
                    case 0x61:
                        method.append("CPU_Regs.reg_edi.word(CPU.CPU_Pop16());CPU_Regs.reg_esi.word(CPU.CPU_Pop16());CPU_Regs.reg_ebp.word(CPU.CPU_Pop16());CPU.CPU_Pop16();");//Don't save SP
                        method.append("CPU_Regs.reg_ebx.word(CPU.CPU_Pop16());CPU_Regs.reg_edx.word(CPU.CPU_Pop16());CPU_Regs.reg_ecx.word(CPU.CPU_Pop16());CPU_Regs.reg_eax.word(CPU.CPU_Pop16());");
                        break;
                    /* BOUND */
                    case 0x62:
                        bound(method);
                        break;
                    /* ARPL Ew,Rw */
                    case 0x63:
                        ARPLEwRw(method);
                        break;
                    /* SEG FS: */
                    case 0x64:
                    case 0x264:
                        method.append("Core.DO_PREFIX_SEG_FS();");
                        segChanged = true;
                        max_opcodes++;
                        continue;
                    /* SEG GS: */
                    case 0x65:
                    case 0x265:
                        method.append("Core.DO_PREFIX_SEG_GS();");
                        segChanged = true;
                        max_opcodes++;
                        continue;
                    /* Operand Size Prefix */
                    case 0x66:
                    case 0x266:
                        opcode_index=(CPU.cpu.code.big?0:512);
                        max_opcodes++;
                        continue;
                    /* Address Size Prefix */
                    case 0x67:
                    case 0x267:
                        prefixes=(prefixes & ~Core.PREFIX_ADDR) |(CPU.cpu.code.big?0:1);
                        EA16 = (prefixes&1)==0;
                        max_opcodes++;
                        continue;
                    /* PUSH Iw */
                    case 0x68:
                        method.append("CPU.CPU_Push16(");method.append(decode_fetchw());method.append(");");
                        break;
                    /* IMUL Gw,Ew,Iw */
                    case 0x69:
                        IMULGwEwIw(method);
                        break;
                    /* PUSH Ib */
                    case 0x6a:
                        method.append("CPU.CPU_Push16(");method.append(decode_fetchbs());method.append(");");
                        break;
                    /* IMUL Gw,Ew,Ib */
                    case 0x6b:
                        IMULGwEwIb(method);
                        break;
                    /* INSB */
                    case 0x6c:
                    case 0x26c:
                        method.append("if (CPU.CPU_IO_Exception(CPU_Regs.reg_edx.word(),1)) ");
                        RUNEXCEPTION(method);
                        DoString(method, "R_INSB");
                        break;
                    /* INSW */
                    case 0x6d:
                        method.append("if (CPU.CPU_IO_Exception(CPU_Regs.reg_edx.word(),2)) ");
                        RUNEXCEPTION(method);
                        DoString(method, "R_INSW");
                        break;
                    /* OUTSB */
                    case 0x6e:
                    case 0x26e:
                        method.append("if (CPU.CPU_IO_Exception(CPU_Regs.reg_edx.word(),1)) ");
                        RUNEXCEPTION(method);
                        DoString(method, "R_OUTSB");
                        break;
                    /* OUTSW */
                    case 0x6f:
                        method.append("if (CPU.CPU_IO_Exception(CPU_Regs.reg_edx.word(),2)) ");
                        RUNEXCEPTION(method);
                        DoString(method, "R_OUTSW");
                        break;
                    /* JO */
                    case 0x70:
                        JumpCond16_b(method, "Flags.TFLG_O()");
                        result = RESULT_RETURN;
                        break;
                    /* JNO */
                    case 0x71:
                        JumpCond16_b(method, "Flags.TFLG_NO()");
                        result = RESULT_RETURN;
                        break;
                    /* JB */
                    case 0x72:
                        JumpCond16_b(method, "Flags.TFLG_B()");
                        result = RESULT_RETURN;
                        break;
                    /* JNB */
                    case 0x73:
                        JumpCond16_b(method, "Flags.TFLG_NB()");
                        result = RESULT_RETURN;
                        break;
                    /* JZ */
                    case 0x74:
                        JumpCond16_b(method, "Flags.TFLG_Z()");
                        result = RESULT_RETURN;
                        break;
                    /* JNZ */
                    case 0x75:
                        JumpCond16_b(method, "Flags.TFLG_NZ()");
                        result = RESULT_RETURN;
                        break;
                    /* JBE */
                    case 0x76:
                        JumpCond16_b(method, "Flags.TFLG_BE()");
                        result = RESULT_RETURN;
                        break;
                    /* JNBE */
                    case 0x77:
                        JumpCond16_b(method, "Flags.TFLG_NBE()");
                        result = RESULT_RETURN;
                        break;
                    /* JS */
                    case 0x78:
                        JumpCond16_b(method, "Flags.TFLG_S()");
                        result = RESULT_RETURN;
                        break;
                    /* JNS */
                    case 0x79:
                        JumpCond16_b(method, "Flags.TFLG_NS()");
                        result = RESULT_RETURN;
                        break;
                    /* JP */
                    case 0x7a:
                        JumpCond16_b(method, "Flags.TFLG_P()");
                        result = RESULT_RETURN;
                        break;
                    /* JNP */
                    case 0x7b:
                        JumpCond16_b(method, "Flags.TFLG_NP()");
                        result = RESULT_RETURN;
                        break;
                    /* JL */
                    case 0x7c:
                        JumpCond16_b(method, "Flags.TFLG_L()");
                        result = RESULT_RETURN;
                        break;
                    /* JNL */
                    case 0x7d:
                        JumpCond16_b(method, "Flags.TFLG_NL()");
                        result = RESULT_RETURN;
                        break;
                    /* JLE */
                    case 0x7e:
                        JumpCond16_b(method, "Flags.TFLG_LE()");
                        result = RESULT_RETURN;
                        break;
                    /* JNLE */
                    case 0x7f:
                        JumpCond16_b(method, "Flags.TFLG_NLE()");
                        result = RESULT_RETURN;
                        break;
                    /* Grpl Eb,Ib */
                    case 0x82:
                    case 0x80:
                    case 0x280:
                    case 0x282:
                        grplb(method);
                        break;
                    /* Grpl Ew,Iw */
                    case 0x81:
                        grplw(method, false);
                        break;
                    /* Grpl Ew,Ix */
                    case 0x83:
                        grplw(method, true);
                        break;
                    /* TEST Eb,Gb */
                    case 0x84:
                    case 0x284:
                        TestEbGb(method);
                        break;
                    /* TEST Ew,Gw */
                    case 0x85:
                        TestEwGw(method);
                        break;
                    /* XCHG Eb,Gb */
                    case 0x86:
                    case 0x286:
                    {
                        int rm=decode_fetchb();
                        method.append("{short oldrmrb=");method.append(getrb(rm));method.append("();");
                        if (rm >= 0xC0) {
                            method.append(getrb(rm));method.append("(");method.append(getearb(rm));method.append("());");
                            method.append(getearb(rm));method.append("(oldrmrb);");
                        } else {
                            method.append("{long eaa  = ");getEaa(method, rm);
                            method.append("int index = Paging.getDirectIndex(eaa);");
                            method.append("if (index>=0) {");
                                method.append(getrb(rm));method.append("(Memory.host_readb(index));");
                                method.append("Memory.host_writeb(index,oldrmrb);");
                            method.append("} else {");
                                method.append(getrb(rm));method.append("(Memory.mem_readb(eaa));");
                                method.append("Memory.mem_writeb(eaa,oldrmrb);");
                            method.append("}}");
                        }
                        method.append("}");
                        break;
                    }
                    /* XCHG Ew,Gw */
                    case 0x87:
                    {
                        int rm=decode_fetchb();
                        method.append("{int oldrmrw=");method.append(getrw(rm));method.append("();");
                        if (rm >= 0xC0) {
                            method.append(getrw(rm));method.append("(");method.append(getearw(rm));method.append("());");
                            method.append(getearw(rm));method.append("(oldrmrw);");
                        } else {
                            method.append("{long eaa  = ");getEaa(method, rm);
                            method.append("if ((eaa & 0xFFF)<0xFFF) {");
                              method.append("int index = Paging.getDirectIndex(eaa);");
                              method.append("if (index>=0) {");
                                method.append(getrw(rm));method.append("(Memory.host_readw(index));");
                                method.append("Memory.host_writew(index,oldrmrw);");
                              method.append("} else {");
                                method.append(getrw(rm));method.append("(Memory.mem_readw(eaa));");
                                method.append("Memory.mem_writew(eaa,oldrmrw);");
                              method.append("}");
                            method.append("} else {");
                              method.append(getrw(rm));method.append("(Memory.mem_readw(eaa));");
                              method.append("Memory.mem_writew(eaa,oldrmrw);");
                            method.append("}}");
                        }
                        method.append("}");
                        break;
                    }
                    /* MOV Eb,Gb */
                    case 0x88:
                    case 0x288:
                    {
                        int rm = decode_fetchb();
                        if (rm >= 0xc0) {
                            method.append(getearb(rm));method.append("(");method.append(getrb(rm));method.append("());");
                        } else {
                            if (rm==5) {
                                method.append("if (CPU.cpu.pmode && !CPU.cpu.code.big) {");
                                method.append("    CPU.cpu.gdt.GetDescriptor((int)CPU.seg_value(Core.base_val_ds),Decoder.desc);");
                                method.append("    if ((Decoder.desc.Type()==CPU.DESC_CODE_R_NC_A) || (Decoder.desc.Type()==CPU.DESC_CODE_R_NC_NA)) {");
                                method.append("        CPU.CPU_Exception(CPU.EXCEPTION_GP,(int)CPU.seg_value(Core.base_val_ds) & 0xfffc);");
                                method.append("        CPU.CPU_Cycles-="+decode.cycles+";");
                                method.append("        return Constants.BR_Normal;");
                                method.append("    }");
                                method.append("}");
                            }
                            method.append("{long eaa  = ");getEaa(method, rm);
                            method.append("Memory.mem_writeb(eaa, ");method.append(getrb(rm));method.append("());}");
                        }
                        break;
                    }
                    /* MOV Ew,Gw */
                    case 0x89:
                    {
                        int rm = decode_fetchb();
                        if (rm >= 0xc0) {
                            method.append(getearw(rm));method.append("(");method.append(getrw(rm));method.append("());");
                        } else {
                            method.append("{long eaa  = ");getEaa(method, rm);
                            method.append("Memory.mem_writew(eaa, ");method.append(getrw(rm));method.append("());}");
                        }
                        break;
                    }
                    /* MOV Gb,Eb */
                    case 0x8a:
                    case 0x28a:
                    {
                        int rm = decode_fetchb();
                        if (rm >= 0xc0) {
                            method.append(getrb(rm));method.append("(");method.append(getearb(rm));method.append("());");
                        } else {
                            method.append("{long eaa  = ");getEaa(method, rm);
                            method.append(getrb(rm));method.append("(Memory.mem_readb(eaa));}");
                        }
                        break;
                    }
                    /* MOV Gw,Ew */
                    case 0x8b:
                    {
                        int rm = decode_fetchb();
                        if (rm >= 0xc0) {
                            method.append(getrw(rm));method.append("(");method.append(getearw(rm));method.append("());");
                        } else {
                            method.append("{long eaa  = ");getEaa(method, rm);
                            method.append(getrw(rm));method.append("(Memory.mem_readw(eaa));}");
                        }
                        break;
                    }
                    /* Mov Ew,Sw */
                    case 0x8c:
                    {
                        int rm = decode_fetchb();
                        int which=(rm>>3)&7;
                        String val = null;
                        switch (which) {
                        case 0x00:					/* MOV Ew,ES */
                            val="CPU.Segs_ESval";break;
                        case 0x01:					/* MOV Ew,CS */
                            val="CPU.Segs_CSval";break;
                        case 0x02:					/* MOV Ew,SS */
                            val="CPU.Segs_SSval";break;
                        case 0x03:					/* MOV Ew,DS */
                            val="CPU.Segs_DSval";break;
                        case 0x04:					/* MOV Ew,FS */
                            val="CPU.Segs_FSval";break;
                        case 0x05:					/* MOV Ew,GS */
                            val="CPU.Segs_GSval";break;
                        default:
                            Log.log(LogTypes.LOG_CPU, LogSeverities.LOG_ERROR,"CPU:8c:Illegal RM Byte");
                            result = RESULT_ILLEGAL_INSTRUCTION;
                        }
                        if (val != null) {
                            if (rm >= 0xc0) {
                                method.append(getearw(rm));method.append("((int)");method.append(val);method.append(");");
                            } else {
                                method.append("{long eaa  = ");getEaa(method, rm);
                                method.append("Memory.mem_writew(eaa, (int)");method.append(val);method.append(");}");
                            }
                        }
                        break;
                    }
                    /* LEA Gw */
                    case 0x8d:
                    {
                        //Little hack to always use segprefixed version
                        segChanged = true;
                        method.append("Core.base_ds=Core.base_ss=0;");
                        int rm = decode_fetchb();
                        if ((prefixes & Core.PREFIX_ADDR)!=0) {
                            method.append(getrw(rm));method.append("((int)(");getEaa32(method, rm);method.append("));");
                        } else {
                            method.append(getrw(rm));method.append("((int)(");getEaa16(method, rm);method.append("));");
                        }
                        break;
                    }
                    /* MOV Sw,Ew */
                    case 0x8e:
                    case 0x28e:
                    {
                        int rm = decode_fetchb();
                        method.append("{int val;");
                        /*Bitu*/int which=(rm>>3)&7;
                        if (rm >= 0xc0 ) {
                            method.append("val=");method.append(getearw(rm));method.append("();");
                        }
                        else {
                            method.append("long eaa=");getEaa(method, rm);
                            method.append("val=Memory.mem_readw(eaa);");
                        }
                        switch (which) {
                        case 0x00:					/* MOV ES,Ew */
                            method.append("CPU.CPU_SetSegGeneralES(val);");
                            break;
                        case 0x02:					/* MOV SS,Ew */
                            max_opcodes++; //Always do another instruction
                            method.append("CPU.CPU_SetSegGeneralSS(val);");
                            segChanged = true;
                            break;
                        case 0x03:					/* MOV DS,Ew */
                            method.append("CPU.CPU_SetSegGeneralDS(val);");
                            segChanged = true;
                            break;
                        case 0x05:					/* MOV GS,Ew */
                            method.append("CPU.CPU_SetSegGeneralGS(val);");
                            break;
                        case 0x04:					/* MOV FS,Ew */
                            method.append("if (CPU.CPU_SetSegGeneralFS(val)) ");
                            RUNEXCEPTION(method);
                            break;
                        default:
                            Log.exit("Oops");
                        }
                        method.append("}");
                        break;
                    }
                    /* POP Ew */
                    case 0x8f:
                    {
                        int rm = decode_fetchb();
                        if (rm >= 0xc0 ) {
                            method.append(getearw(rm));method.append("(CPU.CPU_Pop16());");
                        } else {
                            method.append("{long eaa=");getEaa(method,rm);
                            method.append("Memory.mem_writew(eaa,CPU.CPU_Pop16());}");
                        }
                        break;
                    }
                    /* NOP */
                    case 0x90:
                    case 0x290:
                        break;
                    /* XCHG CX,AX */
                    case 0x91:
                        exchangew(method, "eax", "ecx");
                        break;
                    /* XCHG DX,AX */
                    case 0x92:
                        exchangew(method, "eax", "edx");
                        break;
                    /* XCHG BX,AX */
                    case 0x93:
                        exchangew(method, "eax", "ebx");
                        break;
                    /* XCHG SP,AX */
                    case 0x94:
                        exchangew(method, "eax", "esp");
                        break;
                    /* XCHG BP,AX */
                    case 0x95:
                        exchangew(method, "eax", "ebp");
                        break;
                    /* XCHG SI,AX */
                    case 0x96:
                        exchangew(method, "eax", "esi");
                        break;
                    /* XCHG DI,AX */
                    case 0x97:
                        exchangew(method, "eax", "edi");
                        break;
                    /* CBW */
                    case 0x98:
                        method.append("CPU_Regs.reg_eax.word((int)((byte)CPU_Regs.reg_eax.low()));");
                        break;
                    /* CWD */
                    case 0x99:
                        method.append("if ((CPU_Regs.reg_eax.word() & 0x8000)!=0) CPU_Regs.reg_edx.word(0xffff);else CPU_Regs.reg_edx.word(0);");
                        break;
                    /* CALL Ap */
                    case 0x9a:
                    {
                        method.append("{Flags.FillFlags();");
                        int newip=decode_fetchw();
                        int newcs=decode_fetchw();
                        GETIP(method);
                        method.append("CPU.CPU_CALL(false,");method.append(newcs);method.append(",");method.append(newip);method.append("l,eip);");
                        if (CPU_TRAP_CHECK) {
                            method.append("if (CPU_Regs.GETFLAG(CPU_Regs.TF)!=0) {");
                            returnTrap(method);
                            method.append("}");
                        }
                        method.append("} return Constants.BR_Link1;");
                        result = RESULT_RETURN;
                        break;
                    }
                    /* WAIT */
                    case 0x9b:
                    case 0x29b:
                        break;
                    /* PUSHF */
                    case 0x9c:
                        method.append("if (CPU.CPU_PUSHF(false)) ");
                        RUNEXCEPTION(method);
                        break;
                    /* POPF */
                    case 0x9d:
                        method.append("if (CPU.CPU_POPF(false)) ");
                        RUNEXCEPTION(method);
                        if (CPU_TRAP_CHECK) {
                            method.append("if (CPU_Regs.GETFLAG(CPU_Regs.TF)!=0) {");
                            method.append("CPU.cpudecoder=Core_dynrec2.CPU_Core_Dynrec_Trap_Run;");
                            decodeEnd(method);                            
                            method.append("}");
                        }
                        if (CPU_PIC_CHECK) {
                            method.append("if (CPU_Regs.GETFLAG(CPU_Regs.IF)!=0 && Pic.PIC_IRQCheck!=0) {");
                            decodeEnd(method);
                            method.append("}");
                        }
                        break;
                    /* SAHF */
                    case 0x9e:
                    case 0x29e:
                        method.append("Flags.SETFLAGSb(CPU_Regs.reg_eax.high());");
                        break;
                    /* LAHF */
                    case 0x9f:
                    case 0x29f:
                        method.append("Flags.FillFlags();");
                        method.append("CPU_Regs.reg_eax.high(CPU_Regs.flags&0xff);");
                        break;
                    /* MOV AL,Ob */
                    case 0xA0:
                    case 0x2A0:
                        method.append("CPU_Regs.reg_eax.low(Memory.mem_readb(");GetEADirect(method);method.append("));");
                        break;
                    /* MOV AX,Ow */
                    case 0xA1:
                        method.append("CPU_Regs.reg_eax.word(Memory.mem_readw(");GetEADirect(method);method.append("));");
                        break;
                    /* MOV Ob,AL */
                    case 0xA2:
                    case 0x2A2:
                        method.append("Memory.mem_writeb(");GetEADirect(method);method.append(",CPU_Regs.reg_eax.low());");
                        break;
                    /* MOV Ow,AX */
                    case 0xA3:
                        method.append("Memory.mem_writew(");GetEADirect(method);method.append(",CPU_Regs.reg_eax.word());");
                        break;
                    /* MOVSB */
                    case 0xA4:
                    case 0x2A4:
                        DoString(method, "R_MOVSB");
                        break;
                    /* MOVSW */
                    case 0xA5:
                        DoString(method, "R_MOVSW");
                        break;
                    /* CMPSB */
                    case 0xA6:
                    case 0x2A6:
                        DoString(method, "R_CMPSB");
                        break;
                    /* CMPSW */
                    case 0xA7:
                        DoString(method, "R_CMPSW");
                        break;
                    /* TEST AL,Ib */
                    case 0xA8:
                    case 0x2A8:
                        method.append("Instructions.TESTB((short)");method.append(decode_fetchb());method.append(",CPU_Regs.reg_eax.low());");
                        break;
                    /* TEST AX,Iw */
                    case 0xA9:
                        method.append("Instructions.TESTW((short)");method.append(decode_fetchw());method.append(",CPU_Regs.reg_eax.word());");
                        break;
                    /* STOSB */
                    case 0xAA:
                    case 0x2AA:
                        DoString(method, "R_STOSB");
                        break;
                    /* STOSW */
                    case 0xAB:
                        DoString(method, "R_STOSW");
                        break;
                    /* LODSB */
                    case 0xAC:
                    case 0x2AC:
                        DoString(method, "R_LODSB");
                        break;
                    /* LODSW */
                    case 0xAD:
                        DoString(method, "R_LODSW");
                        break;
                    /* SCASB */
                    case 0xAE:
                    case 0x2AE:
                        DoString(method, "R_SCASB");
                        break;
                    /* SCASW */
                    case 0xAF:
                        DoString(method, "R_SCASW");
                        break;
                    /* MOV AL,Ib */
                    case 0xB0:
                    case 0x2B0:
                        method.append("CPU_Regs.reg_eax.low(");method.append(decode_fetchb());method.append(");");
                        break;
                    /* MOV CL,Ib */
                    case 0xB1:
                    case 0x2B1:
                        method.append("CPU_Regs.reg_ecx.low(");method.append(decode_fetchb());method.append(");");
                        break;
                    /* MOV DL,Ib */
                    case 0xB2:
                    case 0x2B2:
                        method.append("CPU_Regs.reg_edx.low(");method.append(decode_fetchb());method.append(");");
                        break;
                    /* MOV BL,Ib */
                    case 0xB3:
                    case 0x2B3:
                        method.append("CPU_Regs.reg_ebx.low(");method.append(decode_fetchb());method.append(");");
                        break;
                    /* MOV AH,Ib */
                    case 0xB4:
                    case 0x2B4:
                        method.append("CPU_Regs.reg_eax.high(");method.append(decode_fetchb());method.append(");");
                        break;
                    /* MOV CH,Ib */
                    case 0xB5:
                    case 0x2B5:
                        method.append("CPU_Regs.reg_ecx.high(");method.append(decode_fetchb());method.append(");");
                        break;
                    /* MOV DH,Ib */
                    case 0xB6:
                    case 0x2B6:
                        method.append("CPU_Regs.reg_edx.high(");method.append(decode_fetchb());method.append(");");
                        break;
                    /* MOV BH,Ib */
                    case 0xB7:
                    case 0x2B7:
                        method.append("CPU_Regs.reg_ebx.high(");method.append(decode_fetchb());method.append(");");
                        break;
                    /* MOV AX,Iw */
                    case 0xB8:
                        method.append("CPU_Regs.reg_eax.word(");method.append(decode_fetchw());method.append(");");
                        break;
                    /* MOV CX,Iw */
                    case 0xB9:
                        method.append("CPU_Regs.reg_ecx.word(");method.append(decode_fetchw());method.append(");");
                        break;
                    /* MOV DX,Iw */
                    case 0xBA:
                        method.append("CPU_Regs.reg_edx.word(");method.append(decode_fetchw());method.append(");");
                        break;
                    /* MOV BX,Iw */
                    case 0xBB:
                        method.append("CPU_Regs.reg_ebx.word(");method.append(decode_fetchw());method.append(");");
                        break;
                    /* MOV SP,Iw */
                    case 0xBC:
                        method.append("CPU_Regs.reg_esp.word(");method.append(decode_fetchw());method.append(");");
                        break;
                    /* MOV BP,Iw */
                    case 0xBD:
                        method.append("CPU_Regs.reg_ebp.word(");method.append(decode_fetchw());method.append(");");
                        break;
                    /* MOV SI,Iw */
                    case 0xBE:
                        method.append("CPU_Regs.reg_esi.word(");method.append(decode_fetchw());method.append(");");
                        break;
                    /* MOV DI,Iw */
                    case 0xBF:
                        method.append("CPU_Regs.reg_edi.word(");method.append(decode_fetchw());method.append(");");
                        break;
                    /* GRP2 Eb,Ib */
                    case 0xC0:
                    case 0x2C0:
                        GRP2B_fetchb(method);
                        break;
                    /* GRP2 Ew,Ib */
                    case 0xC1:
                        GRP2W_fetchb(method);
                        break;
                    /* RETN Iw */
                    case 0xC2:
                        method.append("CPU_Regs.reg_eip((long)CPU.CPU_Pop16());");
                        method.append("CPU_Regs.reg_esp.dword(CPU_Regs.reg_esp.dword()+");method.append(decode_fetchw());method.append(");");
                        result = RESULT_CONTINUE;
                        break;
                    /* RETN */
                    case 0xC3:
                        method.append("CPU_Regs.reg_eip((long)CPU.CPU_Pop16());");
                        result = RESULT_CONTINUE;
                        break;
                    /* LES */
                    case 0xC4:
                    {
                        int rm=decode_fetchb();
                        if (rm >= 0xc0) Log.exit("Illegal");
                        method.append("{long eaa=");getEaa(method, rm);
                        method.append("if (CPU.CPU_SetSegGeneralES(Memory.mem_readw(eaa+2))) ");
                        RUNEXCEPTION(method);
                        method.append(getrw(rm));
                        method.append("(Memory.mem_readw(eaa));}");
                        break;
                    }
                    /* LDS */
                    case 0xC5:
                    {
                        int rm=decode_fetchb();
                        if (rm >= 0xc0) Log.exit("Illegal");
                        method.append("{long eaa=");getEaa(method, rm);
                        method.append("if (CPU.CPU_SetSegGeneralDS(Memory.mem_readw(eaa+2))) ");
                        RUNEXCEPTION(method);
                        method.append(getrw(rm));
                        method.append("(Memory.mem_readw(eaa));}");
                        segChanged = true;
                        break;
                    }
                    /* MOV Eb,Ib */
                    case 0xC6:
                    case 0x2C6:
                    {
                        int rm = decode_fetchb();
                        if (rm >= 0xc0) {
                            short ib = decode_fetchb();
                            method.append(getearb(rm));method.append("(");method.append(ib);method.append(");");
                        } else {
                            method.append("{long eaa=");getEaa(method, rm);
                            short ib = decode_fetchb();
                            method.append("Memory.mem_writeb(eaa,");method.append(ib);method.append(");}");
                        }
                        break;
                    }
                    /* MOV EW,Iw */
                    case 0xC7:
                    {
                        int rm = decode_fetchb();
                        if (rm >= 0xc0) {
                            int iw = decode_fetchw();
                            method.append(getearw(rm));method.append("(");method.append(iw);method.append(");");
                        } else {
                            method.append("{long eaa=");getEaa(method, rm);
                            int iw = decode_fetchw();
                            method.append("Memory.mem_writew(eaa,");method.append(iw);method.append(");}");
                        }
                        break;
                    }
                    /* ENTER Iw,Ib */
                    case 0xC8:
                    {
                        int bytes=decode_fetchw();
                        int level=decode_fetchb();
                        method.append("CPU.CPU_ENTER(false,");method.append(bytes);method.append(",");method.append(level);method.append(");");
                        break;
                    }
                    /* LEAVE */
                    case 0xC9:
                        method.append("CPU_Regs.reg_esp.dword(CPU_Regs.reg_esp.dword() & CPU.cpu.stack.notmask);");
                        method.append("CPU_Regs.reg_esp.dword(CPU_Regs.reg_esp.dword() | (CPU_Regs.reg_ebp.dword() & CPU.cpu.stack.mask));");
                        method.append("CPU_Regs.reg_ebp.word(CPU.CPU_Pop16());");
                        break;
                    /* RETF Iw */
                    case 0xCA:
                    {
                        /*Bitu*/int words=decode_fetchw();
                        method.append("{Flags.FillFlags();");
                        GETIP(method);
                        method.append("CPU.CPU_RET(false,");method.append(words);method.append(",eip);}");
                        result = RESULT_CONTINUE;
                        break;
                    }
                    /* RETF */
                    case 0xCB:
                        method.append("{Flags.FillFlags();");
                        GETIP(method);
                        method.append("CPU.CPU_RET(false,0,eip);}");
                        result = RESULT_CONTINUE;
                        break;
                    /* INT3 */
                    case 0xCC:
                    case 0x2CC:
                        method.append("{");
                        GETIP(method);
                        method.append("CPU.CPU_SW_Interrupt_NoIOPLCheck(3,eip);");
                        if (CPU_TRAP_CHECK)
                            method.append("CPU.cpu.trap_skip=true;");
                        method.append("}");
                        result = RESULT_CONTINUE;
                        break;
                    /* INT Ib */
                    case 0xCD:
                    case 0x2CD:
                        /*Bit8u*/short num=decode_fetchb();
                        method.append("{");
                        GETIP(method);
                        method.append("CPU.CPU_SW_Interrupt(");
                        method.append(num);
                        method.append(", eip);");
                        if (CPU_TRAP_CHECK)
                            method.append("CPU.cpu.trap_skip=true;");
                        method.append("}");
                        result = RESULT_CONTINUE;
                        break;
                    /* INTO */
                    case 0xCE:
                    case 0x2CE:
                        method.append("if (Flags.get_OF()) {");
                            GETIP(method);
                            method.append("CPU.CPU_SW_Interrupt(4,eip);");
                            if (CPU_TRAP_CHECK)
                                method.append("CPU.cpu.trap_skip=true;");
                            returnNormal(method);
                        method.append("}");
                        break;
                    /* IRET */
                    case 0xCF:
                        method.append("{");
                        GETIP(method);
                        method.append("CPU.CPU_IRET(false,eip);");
                        if (CPU_TRAP_CHECK) {
                            method.append("if (CPU_Regs.GETFLAG(CPU_Regs.TF)!=0) {");
                                returnTrap(method);
                            method.append("}");
                        }
                        if (CPU_PIC_CHECK) {
                            method.append("if (CPU_Regs.GETFLAG(CPU_Regs.IF)!=0 && Pic.PIC_IRQCheck!=0)");
                            returnNone(method);
                        }
                        method.append("}");
                        result = RESULT_CONTINUE;
                        break;
                    /* GRP2 Eb,1 */
                    case 0xD0:
                    case 0x2D0:
                        GRP2B(method, 1);
                        break;
                    /* GRP2 Ew,1 */
                    case 0xD1:
                        GRP2W(method, 1);
                        break;
                    /* GRP2 Eb,CL */
                    case 0xD2:
                    case 0x2D2:
                        GRP2B(method, "CPU_Regs.reg_ecx.low()");
                        break;
                    /* GRP2 Ew,CL */
                    case 0xD3:
                        GRP2W(method, "CPU_Regs.reg_ecx.low()");
                        break;
                    /* AAM Ib */
                    case 0xD4:
                    case 0x2D4:
                        method.append("Instructions.AAM(");method.append(decode_fetchb());method.append(");");
                        break;
                    /* AAD Ib */
                    case 0xD5:
                    case 0x2D5:
                        method.append("Instructions.AAD(");method.append(decode_fetchb());method.append(");");
                        break;
                    /* SALC */
                    case 0xD6:
                    case 0x2D6:
                        method.append("CPU_Regs.reg_eax.low(Flags.get_CF() ? 0xFF : 0);");
                        break;
                    /* XLAT */
                    case 0xD7:
                    case 0x2D7:
                        if ((prefixes & Core.PREFIX_ADDR)!=0) {
                            method.append("CPU_Regs.reg_eax.low(Memory.mem_readb(Core.base_ds+(CPU_Regs.reg_ebx.dword()+CPU_Regs.reg_eax.low())));");
                        } else {
                            method.append("CPU_Regs.reg_eax.low(Memory.mem_readb(Core.base_ds+(CPU_Regs.reg_ebx.word()+CPU_Regs.reg_eax.low())));");
                        }
                        break;
                    /* FPU ESC 0 */
                    case 0xD8:
                    case 0x2D8:
                    {
                        int rm=decode_fetchb();
                        if (rm >= 0xc0) {
                            method.append("FPU.FPU_ESC0_Normal(");method.append(rm);method.append(");");
                        } else {
                            method.append("{long eaa=");getEaa(method, rm);
                            method.append("FPU.FPU_ESC0_EA(");method.append(rm);method.append(",eaa);}");
                        }
                        break;
                    }
                    /* FPU ESC 1 */
                    case 0xD9:
                    case 0x2D9:
                    {
                        int rm=decode_fetchb();
                        if (rm >= 0xc0) {
                            method.append("FPU.FPU_ESC1_Normal(");method.append(rm);method.append(");");
                        } else {
                            method.append("{long eaa=");getEaa(method, rm);
                            method.append("FPU.FPU_ESC1_EA(");method.append(rm);method.append(",eaa);}");
                        }
                        break;
                    }
                    /* FPU ESC 2 */
                    case 0xDA:
                    case 0x2DA:
                    {
                        int rm=decode_fetchb();
                        if (rm >= 0xc0) {
                            method.append("FPU.FPU_ESC2_Normal(");method.append(rm);method.append(");");
                        } else {
                            method.append("{long eaa=");getEaa(method, rm);
                            method.append("FPU.FPU_ESC2_EA(");method.append(rm);method.append(",eaa);}");
                        }
                        break;
                    }
                    /* FPU ESC 3 */
                    case 0xDB:
                    case 0x2DB:
                    {
                        int rm=decode_fetchb();
                        if (rm >= 0xc0) {
                            method.append("FPU.FPU_ESC3_Normal(");method.append(rm);method.append(");");
                        } else {
                            method.append("{long eaa=");getEaa(method, rm);
                            method.append("FPU.FPU_ESC3_EA(");method.append(rm);method.append(",eaa);}");
                        }
                        break;
                    }
                    /* FPU ESC 4 */
                    case 0xDC:
                    case 0x2DC:
                    {
                        int rm=decode_fetchb();
                        if (rm >= 0xc0) {
                            method.append("FPU.FPU_ESC4_Normal(");method.append(rm);method.append(");");
                        } else {
                            method.append("{long eaa=");getEaa(method, rm);
                            method.append("FPU.FPU_ESC4_EA(");method.append(rm);method.append(",eaa);}");
                        }
                        break;
                    }
                    /* FPU ESC 5 */
                    case 0xDD:
                    case 0x2DD:
                    {
                        int rm=decode_fetchb();
                        if (rm >= 0xc0) {
                            method.append("FPU.FPU_ESC5_Normal(");method.append(rm);method.append(");");
                        } else {
                            method.append("{long eaa=");getEaa(method, rm);
                            method.append("FPU.FPU_ESC5_EA(");method.append(rm);method.append(",eaa);}");
                        }
                        break;
                    }
                    /* FPU ESC 6 */
                    case 0xDE:
                    case 0x2DE:
                    {
                        int rm=decode_fetchb();
                        if (rm >= 0xc0) {
                            method.append("FPU.FPU_ESC6_Normal(");method.append(rm);method.append(");");
                        } else {
                            method.append("{long eaa=");getEaa(method, rm);
                            method.append("FPU.FPU_ESC6_EA(");method.append(rm);method.append(",eaa);}");
                        }
                        break;
                    }
                    /* FPU ESC 7 */
                    case 0xDF:
                    case 0x2DF:
                    {
                        int rm=decode_fetchb();
                        if (rm >= 0xc0) {
                            method.append("FPU.FPU_ESC7_Normal(");method.append(rm);method.append(");");
                        } else {
                            method.append("{long eaa=");getEaa(method, rm);
                            method.append("FPU.FPU_ESC7_EA(");method.append(rm);method.append(",eaa);}");
                        }
                        break;
                    }
                    /* LOOPNZ */
                    case 0xE0:
                        if ((prefixes & Core.PREFIX_ADDR)!=0) {
                            method.append("CPU_Regs.reg_ecx.dword(CPU_Regs.reg_ecx.dword()-1);");
                            JumpCond16_b(method, "CPU_Regs.reg_ecx.dword()!=0 && !Flags.get_ZF()");
                        } else {
                            method.append("CPU_Regs.reg_ecx.word(CPU_Regs.reg_ecx.word()-1);");
                            JumpCond16_b(method, "CPU_Regs.reg_ecx.word()!=0 && !Flags.get_ZF()");
                        }
                        result = RESULT_RETURN;
                        break;
                    /* LOOPZ */
                    case 0xE1:
                        if ((prefixes & Core.PREFIX_ADDR)!=0) {
                            method.append("CPU_Regs.reg_ecx.dword(CPU_Regs.reg_ecx.dword()-1);");
                            JumpCond16_b(method, "CPU_Regs.reg_ecx.dword()!=0 && Flags.get_ZF()");
                        } else {
                            method.append("CPU_Regs.reg_ecx.word(CPU_Regs.reg_ecx.word()-1);");
                            JumpCond16_b(method, "CPU_Regs.reg_ecx.word()!=0 && Flags.get_ZF()");
                        }
                        result = RESULT_RETURN;
                        break;
                    /* LOOP */
                    case 0xE2:
                        if ((prefixes & Core.PREFIX_ADDR)!=0) {
                            method.append("CPU_Regs.reg_ecx.dword(CPU_Regs.reg_ecx.dword()-1);");
                            JumpCond16_b(method, "CPU_Regs.reg_ecx.dword()!=0");
                        } else {
                            method.append("CPU_Regs.reg_ecx.word(CPU_Regs.reg_ecx.word()-1);");
                            JumpCond16_b(method, "CPU_Regs.reg_ecx.word()!=0");
                        }
                        result = RESULT_RETURN;
                        break;
                    /* JCXZ */
                    case 0xE3:
                        JumpCond16_b(method, "(CPU_Regs.reg_ecx.dword() & "+AddrMaskTable[prefixes& Core.PREFIX_ADDR]+")==0");
                        result = RESULT_RETURN;
                        break;
                    /* IN AL,Ib */
                    case 0xE4:
                    case 0x2E4:
                    {
                        int port=decode_fetchb();
                        method.append("if (CPU.CPU_IO_Exception(");method.append(port);method.append(",1)) ");
                        RUNEXCEPTION(method);
                        method.append("CPU_Regs.reg_eax.low(IO.IO_ReadB(");method.append(port);method.append("));");
                        break;
                    }
                    /* IN AX,Ib */
                    case 0xE5:
                    {
                        int port=decode_fetchb();
                        method.append("if (CPU.CPU_IO_Exception(");method.append(port);method.append(",2)) ");
                        RUNEXCEPTION(method);
                        method.append("CPU_Regs.reg_eax.word(IO.IO_ReadW(");method.append(port);method.append("));");
                        break;
                    }
                    /* OUT Ib,AL */
                    case 0xE6:
                    case 0x2E6:
                    {
                        int port=decode_fetchb();
                        method.append("if (CPU.CPU_IO_Exception(");method.append(port);method.append(",1)) ");
                        RUNEXCEPTION(method);
                        method.append("IO.IO_WriteB(");method.append(port);method.append(",CPU_Regs.reg_eax.low());");
                        break;
                    }
                    /* OUT Ib,AX */
                    case 0xE7:
                    {
                        int port=decode_fetchb();
                        method.append("if (CPU.CPU_IO_Exception(");method.append(port);method.append(",2)) ");
                        RUNEXCEPTION(method);
                        method.append("IO.IO_WriteW(");method.append(port);method.append(",CPU_Regs.reg_eax.word());");
                        break;
                    }
                    /* CALL Jw */
                    case 0xE8:
                    {
                        int addip=decode_fetchws();
                        SAVEIP(method);
                        method.append("CPU.CPU_Push16((int)(CPU_Regs.reg_eip() & 0xFFFFl));");
                        method.append("CPU_Regs.reg_eip((CPU_Regs.reg_eip()+");method.append(addip);method.append(") & 0xFFFFl);");
                        method.append("return Constants.BR_Link1;");
                        result = RESULT_RETURN;
                        break;
                    }
                    /* JMP Jw */
                    case 0xE9:
                    {
                        int addip=decode_fetchws();
                        SAVEIP(method);
                        method.append("CPU_Regs.reg_eip((CPU_Regs.reg_eip()+");method.append(addip);method.append(") & 0xFFFFl);");
                        method.append("return Constants.BR_Link1;");
                        result = RESULT_RETURN;
                        break;
                    }
                    /* JMP Ap */
                    case 0xEA:
                    {
                        int newip=decode_fetchw();
                        int newcs=decode_fetchw();
                        method.append("{Flags.FillFlags();");
                        GETIP(method);
                        method.append("CPU.CPU_JMP(false,");method.append(newcs);method.append(",");method.append(newip);method.append(",eip);");
                        if (CPU_TRAP_CHECK) {
                            method.append("if (CPU_Regs.GETFLAG(CPU_Regs.TF)!=0) {");
                            returnTrap(method);
                            method.append("}");
                        }
                        method.append("} return Constants.BR_Link1;");
                        result = RESULT_RETURN;
                        break;
                    }
                    /* JMP Jb */
                    case 0xEB:
                    {
                        short addip=decode_fetchbs();
                        SAVEIP(method);
                        method.append("CPU_Regs.reg_eip((CPU_Regs.reg_eip()+");method.append(addip);method.append(") & 0xFFFF);");
                        method.append("return Constants.BR_Link1;");
                        result = RESULT_RETURN;
                        break;
                    }
                    /* IN AL,DX */
                    case 0xEC:
                    case 0x2EC:
                        method.append("if (CPU.CPU_IO_Exception(CPU_Regs.reg_edx.word(),1)) ");
                        RUNEXCEPTION(method);
                        method.append("CPU_Regs.reg_eax.low(IO.IO_ReadB(CPU_Regs.reg_edx.word()));");
                        break;
                    /* IN AX,DX */
                    case 0xED:
                        method.append("if (CPU.CPU_IO_Exception(CPU_Regs.reg_edx.word(),2)) ");
                        RUNEXCEPTION(method);
                        method.append("CPU_Regs.reg_eax.word(IO.IO_ReadW(CPU_Regs.reg_edx.word()));");
                        break;
                    /* OUT DX,AL */
                    case 0xEE:
                    case 0x2EE:
                        method.append("if (CPU.CPU_IO_Exception(CPU_Regs.reg_edx.word(),1)) ");
                        RUNEXCEPTION(method);
                        method.append("IO.IO_WriteB(CPU_Regs.reg_edx.word(),CPU_Regs.reg_eax.low());");
                        break;
                    /* OUT DX,AX */
                    case 0xEF:
                        method.append("if (CPU.CPU_IO_Exception(CPU_Regs.reg_edx.word(),2)) ");
                        RUNEXCEPTION(method);
                        method.append("IO.IO_WriteW(CPU_Regs.reg_edx.word(),CPU_Regs.reg_eax.word());");
                        break;
                    /* LOCK */
                    case 0xF0:
                    case 0x2F0:
                        Log.log(LogTypes.LOG_CPU,LogSeverities.LOG_NORMAL,"CPU:LOCK"); /* FIXME: see case D_LOCK in core_full/new Instructions.load()h */
                        break;
                    /* ICEBP */
                    case 0xF1:
                    case 0x2F1:
                        method.append("{");
                        GETIP(method);
                        method.append("CPU.CPU_SW_Interrupt_NoIOPLCheck(1,eip);");
                        if (CPU_TRAP_CHECK)
                            method.append("CPU.cpu.trap_skip=true;");
                        method.append("}");
                        result = RESULT_CONTINUE;
                        break;
                    /* REPNZ */
                    case 0xF2:
                    case 0x2F2:
                        prefixes|=Core.PREFIX_REP;
                        method.append("Core.rep_zero=false;");
                        max_opcodes++;
                        continue;
                    /* REPZ */
                    case 0xF3:
                    case 0x2F3:
                        prefixes|=Core.PREFIX_REP;
                        method.append("Core.rep_zero=true;");
                        max_opcodes++;
                        continue;
                    /* HLT */
                    case 0xF4:
                    case 0x2F4:
                        method.append("if (CPU.cpu.pmode && CPU.cpu.cpl!=0)");
                        EXCEPTION(method, CPU.EXCEPTION_GP);
                        method.append("Flags.FillFlags();{");
                        GETIP(method);
                        method.append("CPU.CPU_HLT(eip);}");
                        returnNone(method);
                        break;
                    /* CMC */
                    case 0xF5:
                    case 0x2F5:
                        method.append("Flags.FillFlags();");
                        method.append("CPU_Regs.SETFLAGBIT(CPU_Regs.CF,(CPU_Regs.flags & CPU_Regs.CF)==0);");
                        break;
                    /* GRP3 Eb(,Ib) */
                    case 0xF6:
                    case 0x2F6:
                        Grp3EbIb(method);
                        break;
                    /* GRP3 Ew(,Iw) */
                    case 0xF7:
                        Grp3EwIw(method);
                        break;
                    /* CLC */
                    case 0xF8:
                    case 0x2F8:
                        method.append("Flags.FillFlags();");
                        method.append("CPU_Regs.SETFLAGBIT(CPU_Regs.CF,false);");
                        break;
                    /* STC */
                    case 0xF9:
                    case 0x2F9:
                        method.append("Flags.FillFlags();");
                        method.append("CPU_Regs.SETFLAGBIT(CPU_Regs.CF,true);");
                        break;
                    /* CLI */
                    case 0xFA:
                    case 0x2FA:
                        method.append("if (CPU.CPU_CLI()) ");
                        RUNEXCEPTION(method);
                        break;
                    /* STI */
                    case 0xFB:
                    case 0x2FB:
                        method.append("if (CPU.CPU_STI()) ");
                        RUNEXCEPTION(method);
                        if (CPU_PIC_CHECK) {
                            method.append("if (CPU_Regs.GETFLAG(CPU_Regs.IF)!=0 && Pic.PIC_IRQCheck!=0) {");
                            decodeEnd(method);
                            method.append("}");
                        }
                        break;
                    /* CLD */
                    case 0xFC:
                    case 0x2FC:
                        method.append("CPU_Regs.SETFLAGBIT(CPU_Regs.DF,false);");
                        method.append("CPU.cpu.direction=1;");
                        break;
                    /* STD */
                    case 0xFD:
                    case 0x2FD:
                        method.append("CPU_Regs.SETFLAGBIT(CPU_Regs.DF,true);");
                        method.append("CPU.cpu.direction=-1;");
                        break;
                    /* GRP4 Eb */
                    case 0xFE:
                    case 0x2FE:
                    {
                        int rm = decode_fetchb();
                        int which=(rm>>3)&7;
                        switch (which) {
                            case 0x00:										/* INC Eb */
                                eb(method, rm, "Instructions.INCB");
                                break;
                            case 0x01:										/* DEC Eb */
                                eb(method, rm, "Instructions.DECB");
                                break;
                            case 0x07:										/* CallBack */
                                callback = decode_fetchw();
                                result = RESULT_CALLBACK;
                                break;
                            default:
                                Log.exit("Illegal GRP4 Call "+((rm>>3) & 7));
                                break;
                            }
                        break;
                    }
                    /* GRP5 Ew */
                    case 0xFF:
                        result = Grp5Ew(method);
                        break;
                    /* GRP 6 Exxx */
                    case 0x100:
                        Grp6(method);
                        break;
                    /* Group 7 Ew */
                    case 0x101:
                        result = Grp7(method);
                        break;
                    /* LAR Gw,Ew */
                    case 0x102:
                    {
                        method.append("if ((CPU_Regs.flags & CPU_Regs.VM)!=0 || (!CPU.cpu.pmode))");returnIllegal(method);
                        short rm=decode_fetchb();
                        method.append("{IntRef rw = new IntRef(");method.append(getrw(rm));method.append("());");
                        if (rm >= 0xc0) {
                            method.append("CPU.CPU_LAR(");method.append(getearw(rm));method.append("(),rw);");
                        } else {
                            method.append("{long eaa=");getEaa(method, rm);
                            method.append("CPU.CPU_LAR(Memory.mem_readw(eaa),rw);}");
                        }
                        method.append(getrw(rm));method.append("(rw.value);}");
                        break;
                    }
                    /* LSL Gw,Ew */
                    case 0x103:
                    {
                        method.append("if ((CPU_Regs.flags & CPU_Regs.VM)!=0 || (!CPU.cpu.pmode))");returnIllegal(method);
                        short rm=decode_fetchb();
                        method.append("{IntRef int_ref_1 = new IntRef(");method.append(getrw(rm));method.append("());");
                        if (rm >= 0xc0) {
                            method.append("CPU.CPU_LSL(");method.append(getearw(rm));method.append("(),int_ref_1);");
                        } else {
                            method.append("{long eaa=");getEaa(method, rm);
                            method.append("CPU.CPU_LSL(Memory.mem_readw(eaa),int_ref_1);}");
                        }
                        method.append(getrw(rm));method.append("(int_ref_1.value);}");
                        break;
                    }
                    /* CLTS */
                    case 0x106:
                    case 0x306:
                        method.append("if (CPU.cpu.pmode && CPU.cpu.cpl!=0)");EXCEPTION(method, CPU.EXCEPTION_GP);
                        method.append("CPU.cpu.cr0&=(~");method.append(CPU.CR0_TASKSWITCH);method.append(");");
                        break;
                    /* INVD */
                    case 0x108:
                    case 0x109:
                    case 0x308:
                    case 0x309:
                        if (CPU.CPU_ArchitectureType<CPU.CPU_ARCHTYPE_486OLDSLOW) {
                            result = RESULT_ILLEGAL_INSTRUCTION;
                        } else {
                            method.append("if (CPU.cpu.pmode && CPU.cpu.cpl!=0)");EXCEPTION(method, CPU.EXCEPTION_GP);
                        }
                        break;
                    /* MOV Rd.CRx */
                    case 0x120:
                    case 0x320:
                    {
                        int rm=decode_fetchb();
                        /*Bitu*/int which=(rm >> 3) & 7;
                        if (rm < 0xc0 ) {
                            rm |= 0xc0;
                            if (Log.level<=LogSeverities.LOG_ERROR) Log.log(LogTypes.LOG_CPU, LogSeverities.LOG_ERROR,"MOV XXX,CR"+which+" with non-register");
                        }
                        method.append("if (CPU.CPU_READ_CRX(");method.append(which);method.append(",");method.append(geteard_raw(rm));method.append("))");RUNEXCEPTION(method);
                        break;
                    }
                    /* MOV Rd,DRx */
                    case 0x121:
                    case 0x321:
                    {
                        int rm=decode_fetchb();
                        /*Bitu*/int which=(rm >> 3) & 7;
                        if (rm < 0xc0 ) {
                            rm |= 0xc0;
                            if (Log.level<=LogSeverities.LOG_ERROR) Log.log(LogTypes.LOG_CPU,LogSeverities.LOG_ERROR,"MOV XXX,DR"+which+" with non-register");
                        }
                        method.append("if (CPU.CPU_READ_DRX(");method.append(which);method.append(",");method.append(geteard_raw(rm));method.append("))");RUNEXCEPTION(method);
                        break;
                    }
                    /* MOV CRx,Rd */
                    case 0x122:
                    case 0x322:
                    {
                        int rm=decode_fetchb();
                        /*Bitu*/int which=(rm >> 3) & 7;
                        if (rm < 0xc0 ) {
                            rm |= 0xc0;
                            if (Log.level<=LogSeverities.LOG_ERROR) Log.log(LogTypes.LOG_CPU,LogSeverities.LOG_ERROR,"MOV XXX,CR"+which+" with non-register");
                        }
                        method.append("if (CPU.CPU_WRITE_CRX(");method.append(which);method.append(",");method.append(geteard(rm));method.append("()))");RUNEXCEPTION(method);
                        break;
                    }
                    /* MOV DRx,Rd */
                    case 0x123:
                    case 0x323:
                    {
                        int rm=decode_fetchb();
                        /*Bitu*/int which=(rm >> 3) & 7;
                        if (rm < 0xc0 ) {
                            rm |= 0xc0;
                            if (Log.level<=LogSeverities.LOG_ERROR) Log.log(LogTypes.LOG_CPU,LogSeverities.LOG_ERROR,"MOV DR"+which+",XXX with non-register");
                        }
                        method.append("if (CPU.CPU_WRITE_DRX(");method.append(which);method.append(",");method.append(geteard(rm));method.append("()))");RUNEXCEPTION(method);
                        break;
                    }
                    /* MOV Rd,TRx */
                    case 0x124:
                    case 0x324:
                    {
                        int rm=decode_fetchb();
                        /*Bitu*/int which=(rm >> 3) & 7;
                        if (rm < 0xc0 ) {
                            rm |= 0xc0;
                            if (Log.level<=LogSeverities.LOG_ERROR) Log.log(LogTypes.LOG_CPU,LogSeverities.LOG_ERROR,"MOV XXX,TR"+which+" with non-register");
                        }
                        method.append("if (CPU.CPU_READ_TRX(");method.append(which);method.append(",");method.append(geteard_raw(rm));method.append("))");RUNEXCEPTION(method);
                        break;
                    }
                    /* MOV TRx,Rd */
                    case 0x126:
                    case 0x326:
                    {
                        int rm=decode_fetchb();
                        /*Bitu*/int which=(rm >> 3) & 7;
                        if (rm < 0xc0 ) {
                            rm |= 0xc0;
                            if (Log.level<=LogSeverities.LOG_ERROR) Log.log(LogTypes.LOG_CPU,LogSeverities.LOG_ERROR,"MOV TR"+which+",XXX with non-register");
                        }
                        method.append("if (CPU.CPU_WRITE_TRX(");method.append(which);method.append(",");method.append(geteard(rm));method.append("()))");RUNEXCEPTION(method);
                        break;
                    }
                    /* RDTSC */
                    case 0x131:
                    case 0x331:
                        if (CPU.CPU_ArchitectureType<CPU.CPU_ARCHTYPE_PENTIUMSLOW) {
                            result = RESULT_ILLEGAL_INSTRUCTION;
                        } else {
                            method.append("{long tsc=(long)(Pic.PIC_FullIndex()*(double)CPU.CPU_CycleMax);");
                            method.append("CPU_Regs.reg_edx.dword((tsc>>>32));");
                            method.append("CPU_Regs.reg_eax.dword((tsc&0xffffffffl));}");
                        }
                        break;
                    /* JO */
                    case 0x180:
                        JumpCond16_w(method, "Flags.TFLG_O()");
                        result = RESULT_RETURN;
                        break;
                    /* JNO */
                    case 0x181:
                        JumpCond16_w(method, "Flags.TFLG_NO()");
                        result = RESULT_RETURN;
                        break;
                    /* JB */
                    case 0x182:
                        JumpCond16_w(method, "Flags.TFLG_B()");
                        result = RESULT_RETURN;
                        break;
                    /* JNB */
                    case 0x183:
                        JumpCond16_w(method, "Flags.TFLG_NB()");
                        result = RESULT_RETURN;
                        break;
                    /* JZ */
                    case 0x184:
                        JumpCond16_w(method, "Flags.TFLG_Z()");
                        result = RESULT_RETURN;
                        break;
                    /* JNZ */
                    case 0x185:
                        JumpCond16_w(method, "Flags.TFLG_NZ()");
                        result = RESULT_RETURN;
                        break;
                    /* JBE */
                    case 0x186:
                        JumpCond16_w(method, "Flags.TFLG_BE()");
                        result = RESULT_RETURN;
                        break;
                    /* JNBE */
                    case 0x187:
                        JumpCond16_w(method, "Flags.TFLG_NBE()");
                        result = RESULT_RETURN;
                        break;
                    /* JS */
                    case 0x188:
                        JumpCond16_w(method, "Flags.TFLG_S()");
                        result = RESULT_RETURN;
                        break;
                    /* JNS */
                    case 0x189:
                        JumpCond16_w(method, "Flags.TFLG_NS()");
                        result = RESULT_RETURN;
                        break;
                    /* JP */
                    case 0x18a:
                        JumpCond16_w(method, "Flags.TFLG_P()");
                        result = RESULT_RETURN;
                        break;
                    /* JNP */
                    case 0x18b:
                        JumpCond16_w(method, "Flags.TFLG_NP()");
                        result = RESULT_RETURN;
                        break;
                    /* JL */
                    case 0x18c:
                        JumpCond16_w(method, "Flags.TFLG_L()");
                        result = RESULT_RETURN;
                        break;
                    /* JNL */
                    case 0x18d:
                        JumpCond16_w(method, "Flags.TFLG_NL()");
                        result = RESULT_RETURN;
                        break;
                    /* JLE */
                    case 0x18e:
                        JumpCond16_w(method, "Flags.TFLG_LE()");
                        result = RESULT_RETURN;
                        break;
                    /* JNLE */
                    case 0x18f:
                        JumpCond16_w(method, "Flags.TFLG_NLE()");
                        result = RESULT_RETURN;
                        break;
                    /* SETO */
                    case 0x190:
                    case 0x390:
                        SETcc(method, "Flags.TFLG_O()");
                        break;
                    /* SETNO */
                    case 0x191:
                    case 0x391:
                        SETcc(method, "Flags.TFLG_NO()");
                        break;
                    /* SETB */
                    case 0x192:
                    case 0x392:
                        SETcc(method, "Flags.TFLG_B()");
                        break;
                    /* SETNB */
                    case 0x193:
                    case 0x393:
                        SETcc(method, "Flags.TFLG_NB()");
                        break;
                    /* SETZ */
                    case 0x194:
                    case 0x394:
                        SETcc(method, "Flags.TFLG_Z()");
                        break;
                    /* SETNZ */
                    case 0x195:
                    case 0x395:
                        SETcc(method, "Flags.TFLG_NZ()");
                        break;
                    /* SETBE */
                    case 0x196:
                    case 0x396:
                        SETcc(method, "Flags.TFLG_BE()");
                        break;
                    /* SETNBE */
                    case 0x197:
                    case 0x397:
                        SETcc(method, "Flags.TFLG_NBE()");
                        break;
                    /* SETS */
                    case 0x198:
                    case 0x398:
                        SETcc(method, "Flags.TFLG_S()");
                        break;
                    /* SETNS */
                    case 0x199:
                    case 0x399:
                        SETcc(method, "Flags.TFLG_NS()");
                        break;
                    /* SETP */
                    case 0x19a:
                    case 0x39a:
                        SETcc(method, "Flags.TFLG_P()");
                        break;
                    /* SETNP */
                    case 0x19b:
                    case 0x39b:
                        SETcc(method, "Flags.TFLG_NP()");
                        break;
                    /* SETL */
                    case 0x19c:
                    case 0x39c:
                        SETcc(method, "Flags.TFLG_L()");
                        break;
                    /* SETNL */
                    case 0x19d:
                    case 0x39d:
                        SETcc(method, "Flags.TFLG_NL()");
                        break;
                    /* SETLE */
                    case 0x19e:
                    case 0x39e:
                        SETcc(method, "Flags.TFLG_LE()");
                        break;
                    /* SETNLE */
                    case 0x19f:
                    case 0x39f:
                        SETcc(method, "Flags.TFLG_NLE()");
                        break;
                    /* PUSH FS */
                    case 0x1a0:
                        method.append("CPU.CPU_Push16((int)CPU.Segs_FSval);");
                        break;
                    /* POP FS */
                    case 0x1a1:
                        method.append("if (CPU.CPU_PopSegFS(false))"); RUNEXCEPTION(method);
                        break;
                    /* CPUID */
                    case 0x1a2:
                    case 0x3a2:
                        method.append("if (!CPU.CPU_CPUID())");returnIllegal(method);
                        break;
                    /* BT Ew,Gw */
                    case 0x1a3:
                    {
                        method.append("Flags.FillFlags();");
                        short rm=decode_fetchb();
                        method.append("{int rw = ");method.append(getrw(rm));method.append("();");
                        method.append("int mask=1 << (rw & 15);");
                        if (rm >= 0xc0 ) {
                            method.append("CPU_Regs.SETFLAGBIT(CPU_Regs.CF,(");method.append(getearw(rm));method.append("() & mask)!=0);");
                        } else {
                            method.append("long eaa=");getEaa(method, rm);
                            method.append("eaa+=(((short)rw)>>4)*2;");
                            method.append("int old=Memory.mem_readw(eaa);");
                            method.append("CPU_Regs.SETFLAGBIT(CPU_Regs.CF,(old & mask)!=0);");
                        }
                        method.append("}");
                        break;
                    }
                    /* SHLD Ew,Gw,Ib */
                    case 0x1a4:
                    {
                        short rm=decode_fetchb();
                        if (rm >= 0xc0 ) {
                            int op3 = decode_fetchb();
                            DSHLW(method, getrw(rm),op3,getearw(rm)+"()",getearw(rm)+"((int)Flags.lflags.res);");
                        }
                        else {
                            method.append("{long eaa= ");getEaa(method, rm);
                            int op3 = decode_fetchb();
                            DSHLW(method, getrw(rm),op3,"Memory.mem_readw(eaa)","Memory.mem_writew(eaa, (int)Flags.lflags.res);");
                            method.append("}");
                        }
                        break;
                    }
                    /* SHLD Ew,Gw,CL */
                    case 0x1a5:
                    {
                        short rm=decode_fetchb();
                        if (rm >= 0xc0 ) {
                            DSHLW(method, getrw(rm),"CPU_Regs.reg_ecx.low()",getearw(rm)+"()",getearw(rm)+"((int)Flags.lflags.res);");
                        }
                        else {
                            method.append("{long eaa= ");getEaa(method, rm);
                            DSHLW(method, getrw(rm),"CPU_Regs.reg_ecx.low()","Memory.mem_readw(eaa)","Memory.mem_writew(eaa, (int)Flags.lflags.res);");
                            method.append("}");
                        }
                        break;
                    }
                    /* PUSH GS */
                    case 0x1a8:
                        method.append("CPU.CPU_Push16((int)CPU.Segs_GSval);");
                        break;
                    /* POP GS */
                    case 0x1a9:
                        method.append("if (CPU.CPU_PopSegGS(false))");RUNEXCEPTION(method);
                        break;
                    /* BTS Ew,Gw */
                    case 0x1ab:
                    {
                        method.append("Flags.FillFlags();");
                        int rm=decode_fetchb();
                        method.append("{int mask=1 << (");method.append(getrw(rm));method.append("() & 15);");
                        if (rm >= 0xc0 ) {
                            method.append("CPU_Regs.SETFLAGBIT(CPU_Regs.CF,(");method.append(getearw(rm));method.append("() & mask)!=0);");
                            method.append(getearw(rm));method.append("(");method.append(getearw(rm));method.append("() | mask);");
                        } else {
                            method.append("long eaa=");getEaa(method, rm);
                            method.append("eaa+=(((short)");method.append(getrw(rm));method.append("())>>4)*2;");
                            method.append("int old=Memory.mem_readw(eaa);");
                            method.append("CPU_Regs.SETFLAGBIT(CPU_Regs.CF,(old & mask)!=0);");
                            method.append("Memory.mem_writew(eaa,old | mask);");
                        }
                        method.append("}");
                        break;
                    }
                    /* SHRD Ew,Gw,Ib */
                    case 0x1ac:
                     {
                        short rm=decode_fetchb();
                        if (rm >= 0xc0 ) {
                            int op3 = decode_fetchb();
                            DSHRW(method, getrw(rm),op3,getearw(rm)+"()",getearw(rm)+"((int)Flags.lflags.res);");
                        }
                        else {
                            method.append("{long eaa= ");getEaa(method, rm);
                            int op3 = decode_fetchb();
                            DSHRW(method, getrw(rm),op3,"Memory.mem_readw(eaa)","Memory.mem_writew(eaa, (int)Flags.lflags.res);");
                            method.append("}");
                        }
                        break;
                    }
                    /* SHRD Ew,Gw,CL */
                    case 0x1ad:
                    {
                        short rm=decode_fetchb();
                        if (rm >= 0xc0 ) {
                            DSHRW(method, getrw(rm),"CPU_Regs.reg_ecx.low()",getearw(rm)+"()",getearw(rm)+"((int)Flags.lflags.res);");
                        }
                        else {
                            method.append("{long eaa= ");getEaa(method, rm);
                            DSHRW(method, getrw(rm),"CPU_Regs.reg_ecx.low()","Memory.mem_readw(eaa)","Memory.mem_writew(eaa, (int)Flags.lflags.res);");
                            method.append("}");
                        }
                        break;
                    }
                    /* IMUL Gw,Ew */
                    case 0x1af:
                        IMULGwEw(method);
                        break;
                    /* cmpxchg Eb,Gb */
                    case 0x1b0:
                    case 0x3b0:
                    {
                        if (CPU.CPU_ArchitectureType<CPU.CPU_ARCHTYPE_486OLDSLOW)  {
                            result = RESULT_ILLEGAL_INSTRUCTION;
                        } else {
                            method.append("Flags.FillFlags();");
                            short rm=decode_fetchb();
                            if (rm >= 0xc0 ) {
                                method.append("if (CPU_Regs.reg_eax.low() == ");method.append(getearb(rm));method.append("()) {");
                                method.append(getearb(rm));method.append("(");method.append(getrb(rm));method.append("());");
                                method.append("CPU_Regs.SETFLAGBIT(CPU_Regs.ZF,true);");
                                method.append("} else {");
                                method.append("CPU_Regs.reg_eax.low(");method.append(getearb(rm));method.append("());");
                                method.append("CPU_Regs.SETFLAGBIT(CPU_Regs.ZF,false);}");
                            } else {
                                method.append("{long eaa=");getEaa(method, rm);
                                method.append("short val = Memory.mem_readb(eaa);");
                                method.append("if (reg_eax.low() == val) {");
                                method.append("Memory.mem_writeb(eaa,");method.append(getrb(rm));method.append("());");
                                method.append("CPU_Regs.SETFLAGBIT(CPU_Regs.ZF,true);");
                                method.append("} else {");
                                method.append("Memory.mem_writeb(eaa,val);");	// cmpxchg always issues a write
                                method.append("CPU_Regs.reg_eax.low(val);");
                                method.append("CPU_Regs.SETFLAGBIT(CPU_Regs.ZF,false);}");
                            }
                        }
                        break;
                    }
                    /* cmpxchg Ew,Gw */
                    case 0x1b1:
                    {
                        if (CPU.CPU_ArchitectureType<CPU.CPU_ARCHTYPE_486OLDSLOW)  {
                            result = RESULT_ILLEGAL_INSTRUCTION;
                        } else {
                            method.append("Flags.FillFlags();");
                            short rm=decode_fetchb();
                            if (rm >= 0xc0 ) {
                                method.append("if (CPU_Regs.reg_eax.word() == ");method.append(getearw(rm));method.append("()) {");
                                method.append(getearw(rm));method.append("(");method.append(getrw(rm));method.append("());");
                                method.append("CPU_Regs.SETFLAGBIT(CPU_Regs.ZF,true);");
                                method.append("} else {");
                                method.append("CPU_Regs.reg_eax.word(");method.append(getearw(rm));method.append("());");
                                method.append("CPU_Regs.SETFLAGBIT(CPU_Regs.ZF,false);}");
                            } else {
                                method.append("{long eaa=");getEaa(method, rm);
                                method.append("int val = Memory.mem_readw(eaa);");
                                method.append("if (reg_eax.word() == val) {");
                                method.append("Memory.mem_writew(eaa,");method.append(getrw(rm));method.append("());");
                                method.append("CPU_Regs.SETFLAGBIT(CPU_Regs.ZF,true);");
                                method.append("} else {");
                                method.append("Memory.mem_writew(eaa,val);");	// cmpxchg always issues a write
                                method.append("CPU_Regs.reg_eax.word(val);");
                                method.append("CPU_Regs.SETFLAGBIT(CPU_Regs.ZF,false);}");
                            }
                        }
                        break;
                    }
                    /* LSS Ew */
                    case 0x1b2:
                    {
                        int rm=decode_fetchb();
                        if (rm >= 0xc0) {
                            result = RESULT_ILLEGAL_INSTRUCTION;
                        } else {
                            method.append("{long eaa=");getEaa(method, rm);
                            method.append("if (CPU.CPU_SetSegGeneralSS(Memory.mem_readw(eaa+2))) ");RUNEXCEPTION(method);
                            method.append(getrw(rm));method.append("(Memory.mem_readw(eaa));}");
                            segChanged = true;
                        }
                        break;
                    }
                    /* BTR Ew,Gw */
                    case 0x1b3:
                    {
                        method.append("Flags.FillFlags();");
                        int rm=decode_fetchb();
                        method.append("{int mask=1 << (");method.append(getrw(rm));method.append("() & 15);");
                        if (rm >= 0xc0 ) {
                            method.append("CPU_Regs.SETFLAGBIT(CPU_Regs.CF,(");method.append(getearw(rm));method.append("() & mask)!=0);");
                            method.append(getearw(rm));method.append("(");method.append(getearw(rm));method.append("() & ~mask);");
                        } else {
                            method.append("long eaa=");getEaa(method, rm);
                            method.append("eaa+=(((/*Bit16s*/short)");method.append(getrw(rm));method.append("())>>4)*2;");
                            method.append("int old=Memory.mem_readw(eaa);");
                            method.append("CPU_Regs.SETFLAGBIT(CPU_Regs.CF,(old & mask)!=0);");
                            method.append("Memory.mem_writew(eaa,old & ~mask);");
                        }
                        method.append("}");
                        break;
                    }
                    /* LFS Ew */
                    case 0x1b4:
                    {
                        int rm=decode_fetchb();
                        if (rm >= 0xc0) {
                            result = RESULT_ILLEGAL_INSTRUCTION;
                        } else {
                            method.append("{long eaa=");getEaa(method, rm);
                            method.append("if (CPU.CPU_SetSegGeneralFS(Memory.mem_readw(eaa+2)))");RUNEXCEPTION(method);
                            method.append(getrw(rm));method.append("(Memory.mem_readw(eaa));}");
                        }
                        break;
                    }
                    /* LGS Ew */
                    case 0x1b5:
                        {
                        int rm=decode_fetchb();
                        if (rm >= 0xc0) {
                            result = RESULT_ILLEGAL_INSTRUCTION;
                        } else {
                            method.append("{long eaa=");getEaa(method, rm);
                            method.append("if (CPU.CPU_SetSegGeneralGS(Memory.mem_readw(eaa+2)))");RUNEXCEPTION(method);
                            method.append(getrw(rm));method.append("(Memory.mem_readw(eaa));}");
                        }
                        break;
                    }
                    /* MOVZX Gw,Eb */
                    case 0x1b6:
                    {
                        int rm=decode_fetchb();
                        if (rm >= 0xc0 ) {
                            method.append(getrw(rm));method.append("((int)");method.append(getearb(rm));method.append("());");
                        } else {
                            method.append("{long eaa=");getEaa(method, rm);
                            method.append(getrw(rm));method.append("((int)Memory.mem_readb(eaa));}");
                        }
                        break;
                    }
                    /* MOVZX Gw,Ew */
                    case 0x1b7:
                    case 0x1bf:
                    {
                        int rm=decode_fetchb();
                        if (rm >= 0xc0 ) {
                            method.append(getrw(rm));method.append("(");method.append(getearw(rm));method.append("());");
                        } else {
                            method.append("{long eaa=");getEaa(method, rm);
                            method.append(getrw(rm));method.append("(Memory.mem_readw(eaa));}");
                        }
                        break;
                    }
                    /* GRP8 Ew,Ib */
                    case 0x1ba:
                       Grp8EwIb(method);
                        break;
                    /* BTC Ew,Gw */
                    case 0x1bb:
                    {
                        method.append("Flags.FillFlags();");
                        int rm=decode_fetchb();
                        method.append("{int mask=1 << (");method.append(getrw(rm));method.append("() & 15);");
                        if (rm >= 0xc0 ) {
                            method.append("CPU_Regs.SETFLAGBIT(CPU_Regs.CF,(");method.append(getearw(rm));method.append("() & mask)!=0);");
                            method.append(getearw(rm));method.append("(");method.append(getearw(rm));method.append("()^mask);");
                        } else {
                            method.append("long eaa=");getEaa(method, rm);
                            method.append("eaa+=(((short)");method.append(getrw(rm));method.append("())>>4)*2;");
                            method.append("int old=Memory.mem_readw(eaa);");
                            method.append("CPU_Regs.SETFLAGBIT(CPU_Regs.CF,(old & mask)!=0);");
                            method.append("Memory.mem_writew(eaa,old ^ mask);");
                        }
                        method.append("}");
                        break;
                    }
                    /* BSF Gw,Ew */
                    case 0x1bc:
                    {
                        int rm=decode_fetchb();
                        method.append("{int value;");
                        if (rm >= 0xc0) {
                            method.append("value=");method.append(getearw(rm));method.append("();");
                        } else {
                            method.append("long eaa=");getEaa(method, rm);
                            method.append("value=Memory.mem_readw(eaa);");
                        }
                        method.append("if (value==0) {");
                            method.append("CPU_Regs.SETFLAGBIT(CPU_Regs.ZF,true);");
                        method.append("} else {");
                            method.append("int result = 0;");
                            method.append("while ((value & 0x01)==0) { result++; value>>=1; }");
                            method.append("CPU_Regs.SETFLAGBIT(CPU_Regs.ZF,false);");
                            method.append(getrw(rm));method.append("(result);");
                        method.append("}");
                        method.append("Flags.lflags.type=Flags.t_UNKNOWN;}");
                        break;
                    }
                    /* BSR Gw,Ew */
                    case 0x1bd:
                    {
                        int rm=decode_fetchb();
                        method.append("{int value;");
                        if (rm >= 0xc0) {
                            method.append("value=");method.append(getearw(rm));method.append("();");
                        } else {
                            method.append("long eaa=");getEaa(method, rm);
                            method.append("value=Memory.mem_readw(eaa);");
                        }
                        method.append("if (value==0) {");
                            method.append("CPU_Regs.SETFLAGBIT(CPU_Regs.ZF,true);");
                        method.append("} else {");
                            method.append("result = 15;");	// Operandsize-1
                            method.append("while ((value & 0x8000)==0) { result--; value<<=1; }");
                            method.append("CPU_Regs.SETFLAGBIT(CPU_Regs.ZF,false);");
                            method.append(getrw(rm));method.append("(result);");
                        method.append("}");
                        method.append("Flags.lflags.type=Flags.t_UNKNOWN;}");
                        break;
                    }
                    /* MOVSX Gw,Eb */
                    case 0x1be:
                    {
                        int rm=decode_fetchb();
                        if (rm >= 0xc0 ) {
                            method.append(getrw(rm));method.append("((int)((byte)");method.append(getearb(rm));method.append("()));");
                        } else {
                            method.append("long eaa=");getEaa(method, rm);
                            method.append(getrw(rm));method.append("((int)((byte)Memory.mem_readb(eaa)));");
                        }
                        break;
                    }
                    /* XADD Gb,Eb */
                    case 0x1c0:
                    {
                        if (CPU.CPU_ArchitectureType<CPU.CPU_ARCHTYPE_486OLDSLOW) {
                            result = RESULT_ILLEGAL_INSTRUCTION;
                        } else {
                            int rm=decode_fetchb();
                            method.append("{short oldrmrb=");method.append(getrb(rm));method.append("();");
                            if (rm >= 0xc0 ) {
                                method.append(getrb(rm));method.append("(");method.append(getearb(rm));method.append("());");
                                method.append(getearb(rm));method.append("((short)");method.append(getearb(rm));method.append("()+oldrmrb));");
                            } else {
                                method.append("long eaa=");getEaa(method, rm);
                                method.append(getrb(rm));method.append("(Memory.mem_readb(eaa));");
                                method.append("Memory.mem_writeb(eaa,Memory.mem_readb(eaa)+oldrmrb);");
                            }
                            method.append("}");
                        }
                        break;
                    }
                    /* XADD Gw,Ew */
                    case 0x1c1:
                    {
                        if (CPU.CPU_ArchitectureType<CPU.CPU_ARCHTYPE_486OLDSLOW) {
                            result = RESULT_ILLEGAL_INSTRUCTION;
                        } else {
                            int rm=decode_fetchb();
                            method.append("{int oldrmrw=");method.append(getrw(rm));method.append("();");
                            if (rm >= 0xc0 ) {
                                method.append(getrw(rm));method.append("(");method.append(getearw(rm));method.append("());");
                                method.append(getearw(rm));method.append("(");method.append(getearw(rm));method.append("()+oldrmrw);");
                            } else {
                                method.append("long eaa=");getEaa(method, rm);
                                method.append(getrw(rm));method.append("(Memory.mem_readw(eaa));");
                                method.append("Memory.mem_writew(eaa,Memory.mem_readw(eaa)+oldrmrw);");
                            }
                            method.append("}");
                        }
                        break;
                    }
                    /* BSWAP AX */
                    case 0x1c8:
                        if (CPU.CPU_ArchitectureType<CPU.CPU_ARCHTYPE_486OLDSLOW) {
                            result = RESULT_ILLEGAL_INSTRUCTION;
                        } else {
                            method.append("CPU_Regs.reg_eax.word(Instructions.BSWAPW(CPU_Regs.reg_eax.word()));");
                        }
                        break;
                    /* BSWAP CX */
                    case 0x1c9:
                        if (CPU.CPU_ArchitectureType<CPU.CPU_ARCHTYPE_486OLDSLOW) {
                            result = RESULT_ILLEGAL_INSTRUCTION;
                        } else {
                            method.append("CPU_Regs.reg_ecx.word(Instructions.BSWAPW(CPU_Regs.reg_ecx.word()));");
                        }
                        break;
                    /* BSWAP DX */
                    case 0x1ca:
                        if (CPU.CPU_ArchitectureType<CPU.CPU_ARCHTYPE_486OLDSLOW) {
                            result = RESULT_ILLEGAL_INSTRUCTION;
                        } else {
                            method.append("CPU_Regs.reg_edx.word(Instructions.BSWAPW(CPU_Regs.reg_edx.word()));");
                        }
                        break;
                    /* BSWAP BX */
                    case 0x1cb:
                        if (CPU.CPU_ArchitectureType<CPU.CPU_ARCHTYPE_486OLDSLOW) {
                            result = RESULT_ILLEGAL_INSTRUCTION;
                        } else {
                            method.append("CPU_Regs.reg_ebx.word(Instructions.BSWAPW(CPU_Regs.reg_ebx.word()));");
                        }
                        break;
                    /* BSWAP SP */
                    case 0x1cc:
                        if (CPU.CPU_ArchitectureType<CPU.CPU_ARCHTYPE_486OLDSLOW) {
                            result = RESULT_ILLEGAL_INSTRUCTION;
                        } else {
                            method.append("CPU_Regs.reg_esp.word(Instructions.BSWAPW(CPU_Regs.reg_esp.word()));");
                        }
                        break;
                    /* BSWAP BP */
                    case 0x1cd:
                        if (CPU.CPU_ArchitectureType<CPU.CPU_ARCHTYPE_486OLDSLOW) {
                            result = RESULT_ILLEGAL_INSTRUCTION;
                        } else {
                            method.append("CPU_Regs.reg_ebp.word(Instructions.BSWAPW(CPU_Regs.reg_ebp.word()));");
                        }
                        break;
                    /* BSWAP SI */
                    case 0x1ce:
                        if (CPU.CPU_ArchitectureType<CPU.CPU_ARCHTYPE_486OLDSLOW) {
                            result = RESULT_ILLEGAL_INSTRUCTION;
                        } else {
                            method.append("CPU_Regs.reg_esi.word(Instructions.BSWAPW(CPU_Regs.reg_esi.word()));");
                        }
                        break;
                    /* BSWAP DI */
                    case 0x1cf:
                        if (CPU.CPU_ArchitectureType<CPU.CPU_ARCHTYPE_486OLDSLOW) {
                            result = RESULT_ILLEGAL_INSTRUCTION;
                        } else {
                            method.append("CPU_Regs.reg_edi.word(Instructions.BSWAPW(CPU_Regs.reg_edi.word()));");
                        }
                        break;
                    /* ADD Ed,Gd */
                    case 0x201:
                        edgd(method, "Instructions.ADDD", true);
                        break;
                    /* ADD Gd,Ed */
                    case 0x203:
                        gded(method, "Instructions.ADDD", true);
                        break;
                    /* ADD EAX,Id */
                    case 0x205:
                        eaxid(method, "Instructions.ADDD", true);
                        break;
                    /* PUSH ES */
                    case 0x206:
                        method.append("CPU.CPU_Push32(CPU.Segs_ESval);");
                        break;
                    /* POP ES */
                    case 0x207:
                        method.append("if (CPU.CPU_PopSegES(true))");RUNEXCEPTION(method);
                        break;
                    /* OR Ed,Gd */
                    case 0x209:
                        edgd(method, "Instructions.ORD", true);
                        break;
                    /* OR Gd,Ed */
                    case 0x20b:
                        gded(method, "Instructions.ORD", true);
                        break;
                    /* OR EAX,Id */
                    case 0x20d:
                        eaxid(method, "Instructions.ORD", true);
                        break;
                    /* PUSH CS */
                    case 0x20e:
                        method.append("CPU.CPU_Push32(CPU.Segs_CSval);");
                        break;
                    /* ADC Ed,Gd */
                    case 0x211:
                        edgd(method, "Instructions.ADCD", true);
                        break;
                    /* ADC Gd,Ed */
                    case 0x213:
                        gded(method, "Instructions.ADCD", true);
                        break;
                    /* ADC EAX,Id */
                    case 0x215:
                        eaxid(method, "Instructions.ADCD", true);
                        break;
                    /* PUSH SS */
                    case 0x216:
                        method.append("CPU.CPU_Push32(CPU.Segs_SSval);");
                        break;
                    /* POP SS */
                    case 0x217:
                        method.append("if (CPU.CPU_PopSegSS(true)) ");RUNEXCEPTION(method);
                        //Always do another instruction
                        max_opcodes++;
                        segChanged = true;
                        break;
                    /* SBB Ed,Gd */
                    case 0x219:
                        edgd(method, "Instructions.SBBD", true);
                        break;
                    /* SBB Gd,Ed */
                    case 0x21b:
                        gded(method, "Instructions.SBBD", true);
                        break;
                    /* SBB EAX,Id */
                    case 0x21d:
                        eaxid(method, "Instructions.SBBD", true);
                        break;
                    /* PUSH DS */
                    case 0x21e:
                        method.append("CPU.CPU_Push32(CPU.Segs_DSval);");
                        break;
                    /* POP DS */
                    case 0x21f:
                        method.append("if (CPU.CPU_PopSegDS(true)) ");RUNEXCEPTION(method);
                        //Always do another instruction
                        max_opcodes++;
                        segChanged = true;
                        break;
                    /* AND Ed,Gd */
                    case 0x221:
                        edgd(method, "Instructions.ANDD", true);
                        break;
                    /* AND Gd,Ed */
                    case 0x223:
                        gded(method, "Instructions.ANDD", true);
                        break;
                    /* AND EAX,Id */
                    case 0x225:
                        eaxid(method, "Instructions.ANDD", true);
                        break;
                    /* SUB Ed,Gd */
                    case 0x229:
                        edgd(method, "Instructions.SUBD", true);
                        break;
                    /* SUB Gd,Ed */
                    case 0x22b:
                        gded(method, "Instructions.SUBD", true);
                        break;
                    /* SUB EAX,Id */
                    case 0x22d:
                        eaxid(method, "Instructions.SUBD", true);
                        break;
                    /* XOR Ed,Gd */
                    case 0x231:
                        edgd(method, "Instructions.XORD", true);
                        break;
                    /* XOR Gd,Ed */
                    case 0x233:
                        gded(method, "Instructions.XORD", true);
                        break;
                    /* XOR EAX,Id */
                    case 0x235:
                        eaxid(method, "Instructions.XORD", true);
                        break;
                    /* CMP Ed,Gd */
                    case 0x239:
                        edgd(method, "Instructions.CMPD", false);
                        break;
                    /* CMP Gd,Ed */
                    case 0x23b:
                        gded(method, "Instructions.CMPD", false);
                        break;
                    /* CMP EAX,Id */
                    case 0x23d:
                        eaxid(method, "Instructions.CMPD", false);
                        break;
                    /* INC EAX */
                    case 0x240:
                        incd(method, 0);
                        break;
                    /* INC ECX */
                    case 0x241:
                        incd(method, 1);
                        break;
                    /* INC EDX */
                    case 0x242:
                        incd(method, 2);
                        break;
                    /* INC EBX */
                    case 0x243:
                        incd(method, 3);
                        break;
                    /* INC ESP */
                    case 0x244:
                        incd(method, 4);
                        break;
                    /* INC EBP */
                    case 0x245:
                        incd(method, 5);
                        break;
                    /* INC ESI */
                    case 0x246:
                        incd(method, 6);
                        break;
                    /* INC EDI */
                    case 0x247:
                        incd(method, 7);
                        break;
                    /* DEC EAX */
                    case 0x248:
                        decd(method, 0);
                        break;
                    /* DEC ECX */
                    case 0x249:
                        decd(method, 1);
                        break;
                    /* DEC EDX */
                    case 0x24a:
                        decd(method, 2);
                        break;
                    /* DEC EBX */
                    case 0x24b:
                        decd(method, 3);
                        break;
                    /* DEC ESP */
                    case 0x24c:
                        decd(method, 4);
                        break;
                    /* DEC EBP */
                    case 0x24d:
                        decd(method, 5);
                        break;
                    /* DEC ESI */
                    case 0x24e:
                        decd(method, 6);
                        break;
                    /* DEC EDI */
                    case 0x24f:
                        decd(method, 7);
                        break;
                    /* PUSH EAX */
                    case 0x250:
                        method.append("CPU.CPU_Push32(CPU_Regs.reg_eax.dword());");
                        break;
                    /* PUSH ECX */
                    case 0x251:
                        method.append("CPU.CPU_Push32(CPU_Regs.reg_ecx.dword());");
                        break;
                    /* PUSH EDX */
                    case 0x252:
                        method.append("CPU.CPU_Push32(CPU_Regs.reg_edx.dword());");
                        break;
                    /* PUSH EBX */
                    case 0x253:
                        method.append("CPU.CPU_Push32(CPU_Regs.reg_ebx.dword());");
                        break;
                    /* PUSH ESP */
                    case 0x254:
                        method.append("CPU.CPU_Push32(CPU_Regs.reg_esp.dword());");
                        break;
                    /* PUSH EBP */
                    case 0x255:
                        method.append("CPU.CPU_Push32(CPU_Regs.reg_ebp.dword());");
                        break;
                    /* PUSH ESI */
                    case 0x256:
                        method.append("CPU.CPU_Push32(CPU_Regs.reg_esi.dword());");
                        break;
                    /* PUSH EDI */
                    case 0x257:
                        method.append("CPU.CPU_Push32(CPU_Regs.reg_edi.dword());");
                        break;
                    /* POP EAX */
                    case 0x258:
                        method.append("CPU_Regs.reg_eax.dword(CPU.CPU_Pop32());");
                        break;
                    /* POP ECX */
                    case 0x259:
                        method.append("CPU_Regs.reg_ecx.dword(CPU.CPU_Pop32());");
                        break;
                    /* POP EDX */
                    case 0x25a:
                        method.append("CPU_Regs.reg_edx.dword(CPU.CPU_Pop32());");
                        break;
                    /* POP EBX */
                    case 0x25b:
                        method.append("CPU_Regs.reg_ebx.dword(CPU.CPU_Pop32());");
                        break;
                    /* POP ESP */
                    case 0x25c:
                        method.append("CPU_Regs.reg_esp.dword(CPU.CPU_Pop32());");
                        break;
                    /* POP EBP */
                    case 0x25d:
                        method.append("CPU_Regs.reg_ebp.dword(CPU.CPU_Pop32());");
                        break;
                    /* POP ESI */
                    case 0x25e:
                        method.append("CPU_Regs.reg_esi.dword(CPU.CPU_Pop32());");
                        break;
                    /* POP EDI */
                    case 0x25f:
                        method.append("CPU_Regs.reg_edi.dword(CPU.CPU_Pop32());");
                        break;
                    /* PUSHAD */
                    case 0x260:
                    {
                        method.append("{long tmpesp = CPU_Regs.reg_esp.dword();");
                        method.append("CPU.CPU_Push32(CPU_Regs.reg_eax.dword());");
                        method.append("CPU.CPU_Push32(CPU_Regs.reg_ecx.dword());");
                        method.append("CPU.CPU_Push32(CPU_Regs.reg_edx.dword());");
                        method.append("CPU.CPU_Push32(CPU_Regs.reg_ebx.dword());");
                        method.append("CPU.CPU_Push32(tmpesp);");
                        method.append("CPU.CPU_Push32(CPU_Regs.reg_ebp.dword());");
                        method.append("CPU.CPU_Push32(CPU_Regs.reg_esi.dword());");
                        method.append("CPU.CPU_Push32(CPU_Regs.reg_edi.dword());");
                        method.append("}");
                        break;
                    }            
                    /* POPAD */
                    case 0x261:
                        method.append("CPU_Regs.reg_edi.dword(CPU.CPU_Pop32());");
                        method.append("CPU_Regs.reg_esi.dword(CPU.CPU_Pop32());");
                        method.append("CPU_Regs.reg_ebp.dword(CPU.CPU_Pop32());");
                        method.append("CPU.CPU_Pop32();");//Don't save ESP
                        method.append("CPU_Regs.reg_ebx.dword(CPU.CPU_Pop32());");
                        method.append("CPU_Regs.reg_edx.dword(CPU.CPU_Pop32());");
                        method.append("CPU_Regs.reg_ecx.dword(CPU.CPU_Pop32());");
                        method.append("CPU_Regs.reg_eax.dword(CPU.CPU_Pop32());");
                        break;
                    /* BOUND Ed */
                    case 0x262:
                    {
                        method.append("{int bound_min, bound_max;");
                        int rm=decode_fetchb();
                        method.append("long eaa=");getEaa(method, rm);
                        method.append("bound_min=(int)Memory.mem_readd(eaa);");
                        method.append("bound_max=(int)Memory.mem_readd(eaa+4);");
                        method.append("int rmrd = (int)(");method.append(getrd(rm));method.append("() & 0xFFFFFFFFl);");
                        method.append("if (rmrd < bound_min || rmrd > bound_max)");EXCEPTION(method, 5);
                        method.append("}");
                        break;
                    }
                    /* ARPL Ed,Rd */
                    case 0x263:
                    {
                        method.append("if ((CPU_Regs.flags & CPU_Regs.VM)!=0 || (!CPU.cpu.pmode))");returnIllegal(method);
                        int rm=decode_fetchb();
                        if (rm >= 0xc0 ) {
                            method.append("{IntRef ref = new IntRef((int)");method.append(geteard(rm));method.append("());");
                            method.append("CPU.CPU_ARPL(ref,");method.append(getrw(rm));method.append("());");
                            method.append(geteard(rm));method.append("((long)ref.value);}");
                        } else {
                            method.append("{long eaa=");getEaa(method, rm);
                            method.append("IntRef ref = new IntRef(Memory.mem_readw(eaa));");
                            method.append("CPU.CPU_ARPL(ref,");method.append(getrw(rm));method.append("());");
                            method.append("Memory.mem_writed(eaa,(long)ref.value);}");
                        }
                        break;
                    }
                    /* PUSH Id */
                    case 0x268:
                        method.append("CPU.CPU_Push32(");method.append(decode_fetchd());method.append("l);");
                        break;
                    /* IMUL Gd,Ed,Id */
                    case 0x269:
                    {
                        int rm=decode_fetchb();
                        if (rm >= 0xc0 ) {
                            method.append(getrd(rm));method.append("(Instructions.DIMULD(");
                            method.append(geteard(rm));method.append("(),");method.append(decode_fetchds());method.append("l));");
                        }
                        else {
                            method.append("{long eaa = ");getEaa(method, rm);
                            method.append(getrd(rm));method.append("(Instructions.DIMULD(Memory.mem_readd(eaa),");
                            method.append(decode_fetchds());method.append("l));}");
                        }
                        break;
                    }
                    /* PUSH Ib */
                    case 0x26a:
                        method.append("CPU.CPU_Push32(");method.append(decode_fetchbs());method.append("l);");
                        break;
                    /* IMUL Gd,Ed,Ib */
                    case 0x26b:
                    {
                        int rm=decode_fetchb();
                        if (rm >= 0xc0 ) {
                            method.append(getrd(rm));method.append("(Instructions.DIMULD(");
                            method.append(geteard(rm));method.append("(),");method.append(decode_fetchbs());method.append("l));");
                        }
                        else {
                            method.append("{long eaa = ");getEaa(method, rm);
                            method.append(getrd(rm));method.append("(Instructions.DIMULD(Memory.mem_readd(eaa),");
                            method.append(decode_fetchbs());method.append("l));}");
                        }
                        break;
                    }
                    /* INSD */
                    case 0x26d:
                        method.append("if (CPU.CPU_IO_Exception(CPU_Regs.reg_edx.word(),4))"); RUNEXCEPTION(method);
                        DoString(method, "R_INSD");
                        break;
                    /* OUTSD */
                    case 0x26f:
                        method.append("if (CPU.CPU_IO_Exception(CPU_Regs.reg_edx.word(),4))"); RUNEXCEPTION(method);
                        DoString(method, "R_OUTSD");
                        break;
                    /* JO */
                    case 0x270:
                        JumpCond32_b(method,"Flags.TFLG_O()");
                        result = RESULT_RETURN;
                        break;
                    /* JNO */
                    case 0x271:
                        JumpCond32_b(method,"Flags.TFLG_NO()");
                        result = RESULT_RETURN;
                        break;
                    /* JB */
                    case 0x272:
                        JumpCond32_b(method,"Flags.TFLG_B()");
                        result = RESULT_RETURN;
                        break;
                    /* JNB */
                    case 0x273:
                        JumpCond32_b(method,"Flags.TFLG_NB()");
                        result = RESULT_RETURN;
                        break;
                    /* JZ */
                    case 0x274:
                        JumpCond32_b(method,"Flags.TFLG_Z()");
                        result = RESULT_RETURN;
                        break;
                    /* JNZ */
                    case 0x275:
                        JumpCond32_b(method,"Flags.TFLG_NZ()");
                        result = RESULT_RETURN;
                        break;
                    /* JBE */
                    case 0x276:
                        JumpCond32_b(method,"Flags.TFLG_BE()");
                        result = RESULT_RETURN;
                        break;
                    /* JNBE */
                    case 0x277:
                        JumpCond32_b(method,"Flags.TFLG_NBE()");
                        result = RESULT_RETURN;
                        break;
                    /* JS */
                    case 0x278:
                        JumpCond32_b(method,"Flags.TFLG_S()");
                        result = RESULT_RETURN;
                        break;
                    /* JNS */
                    case 0x279:
                        JumpCond32_b(method,"Flags.TFLG_NS()");
                        result = RESULT_RETURN;
                        break;
                    /* JP */
                    case 0x27a:
                        JumpCond32_b(method,"Flags.TFLG_P()");
                        result = RESULT_RETURN;
                        break;
                    /* JNP */
                    case 0x27b:
                        JumpCond32_b(method,"Flags.TFLG_NP()");
                        result = RESULT_RETURN;
                        break;
                    /* JL */
                    case 0x27c:
                        JumpCond32_b(method,"Flags.TFLG_L()");
                        result = RESULT_RETURN;
                        break;
                    /* JNL */
                    case 0x27d:
                        JumpCond32_b(method,"Flags.TFLG_NL()");
                        result = RESULT_RETURN;
                        break;
                    /* JLE */
                    case 0x27e:
                        JumpCond32_b(method,"Flags.TFLG_LE()");
                        result = RESULT_RETURN;
                        break;
                    /* JNLE */
                    case 0x27f:
                        JumpCond32_b(method,"Flags.TFLG_NLE()");
                        result = RESULT_RETURN;
                        break;
                    /* Grpl Ed,Id */
                    case 0x281:
                        GrplEd(method, false);
                        break;
                    /* Grpl Ed,Ix */
                    case 0x283:
                        GrplEd(method, true);
                        break;
                    /* TEST Ed,Gd */
                    case 0x285:
                    {
                        int rm=decode_fetchb();
                        if (rm >= 0xc0 ) {
                            method.append("Instructions.TESTD(");method.append(getrd(rm));method.append("(), ");method.append(geteard(rm));method.append("());");
                        }
                        else {
                            method.append("{long eaa = ");getEaa(method, rm);
                            method.append("Instructions.TESTD(");method.append(getrd(rm));method.append("(),Memory.mem_readd(eaa));}");
                        }
                        break;
                    }
                    /* XCHG Ed,Gd */
                    case 0x287:
                    {
                        int rm=decode_fetchb();
                        method.append("{long oldrmrd = ");method.append(getrd(rm));method.append("();");
                        if (rm >= 0xc0 ) {
                            method.append(getrd(rm));method.append("(");method.append(geteard(rm));method.append("());");
                            method.append(geteard(rm));method.append("(oldrmrd);");
                        } else {
                            method.append("long eaa=");getEaa(method, rm);
                            method.append("if ((eaa & 0xFFF)<0xFFD) {");
                                method.append("int index = Paging.getDirectIndex(eaa);");
                                method.append("if (index>=0) {");
                                    method.append(getrd(rm));method.append("(Memory.host_readd(index));");
                                    method.append("Memory.host_writed(index,oldrmrd);");
                                method.append("} else {");
                                    method.append(getrd(rm));method.append("(Memory.mem_readd(eaa));");
                                    method.append("Memory.mem_writed(eaa,oldrmrd);");
                                method.append("}");
                            method.append("} else {");
                                method.append(getrd(rm));method.append("(Memory.mem_readd(eaa));");
                                method.append("Memory.mem_writed(eaa,oldrmrd);");
                            method.append("}");
                        }
                        method.append("}");
                        break;
                    }
                    /* MOV Ed,Gd */
                    case 0x289:
                    {
                        int rm=decode_fetchb();
                        if (rm >= 0xc0 ) {
                            method.append(geteard(rm));method.append("(");method.append(getrd(rm));method.append("());");
                        } else {
                            method.append("{long eaa=");getEaa(method,rm);

                            method.append("Memory.mem_writed(eaa, ");method.append(getrd(rm));method.append("());}");
                        }
                        break;
                    }
                    /* MOV Gd,Ed */
                    case 0x28b:
                    {
                        int rm=decode_fetchb();
                        if (rm >= 0xc0 ) {
                            method.append(getrd(rm));method.append("(");method.append(geteard(rm));method.append("());");
                        } else {
                            method.append("{long eaa=");getEaa(method,rm);
                            method.append(getrd(rm));method.append("(Memory.mem_readd(eaa));}");
                        }
                        break;
                    }
                    /* Mov Ew,Sw */
                    case 0x28c:
                    {
                        int rm = decode_fetchb();
                        int which=(rm>>3)&7;
                        String val = null;
                        switch (which) {
                        case 0x00:					/* MOV Ew,ES */
                            val="CPU.Segs_ESval";break;
                        case 0x01:					/* MOV Ew,CS */
                            val="CPU.Segs_CSval";break;
                        case 0x02:					/* MOV Ew,SS */
                            val="CPU.Segs_SSval";break;
                        case 0x03:					/* MOV Ew,DS */
                            val="CPU.Segs_DSval";break;
                        case 0x04:					/* MOV Ew,FS */
                            val="CPU.Segs_FSval";break;
                        case 0x05:					/* MOV Ew,GS */
                            val="CPU.Segs_GSval";break;
                        default:
                            Log.log(LogTypes.LOG_CPU, LogSeverities.LOG_ERROR,"CPU:8c:Illegal RM Byte");
                            result = RESULT_ILLEGAL_INSTRUCTION;
                        }
                        if (val != null) {
                            if (rm >= 0xc0) {
                                // this dword assignment is intentional
                                method.append(geteard(rm));method.append("(");method.append(val);method.append(" & 0xFFFF);");
                            } else {
                                method.append("{long eaa  = ");getEaa(method, rm);
                                method.append("Memory.mem_writew(eaa, (int)");method.append(val);method.append(");}");
                            }
                        }
                        break;
                    }
                    /* LEA Gd */
                    case 0x28d:
                    {
                        //Little hack to always use segprefixed version
                        int rm=decode_fetchb();
                        method.append("Core.base_ds=Core.base_ss=0;");
                        segChanged = true;
                        if ((prefixes & Core.PREFIX_ADDR)!=0) {
                            method.append(getrd(rm));method.append("(");getEaa32(method, rm);method.append(");");
                        } else {
                            method.append(getrd(rm));method.append("(");getEaa16(method, rm);method.append(");");
                        }
                        break;
                    }
                    /* POP Ed */
                    case 0x28f:
                    {
                        int rm=decode_fetchb();
                        if (rm >= 0xc0 ) {
                            method.append(geteard(rm));method.append("(CPU.CPU_Pop32());");
                        } else {
                            method.append("{long eaa=");getEaa(method, rm);
                            method.append("Memory.mem_writed(eaa,CPU.CPU_Pop32());}");
                        }
                        break;
                    }
                    /* XCHG ECX,EAX */
                    case 0x291:
                        exchanged(method, "eax", "ecx");
                        break;
                    /* XCHG EDX,EAX */
                    case 0x292:
                        exchanged(method, "eax", "edx");
                        break;
                    /* XCHG EBX,EAX */
                    case 0x293:
                        exchanged(method, "eax", "ebx");
                        break;
                    /* XCHG ESP,EAX */
                    case 0x294:
                        exchanged(method, "eax", "esp");
                        break;
                    /* XCHG EBP,EAX */
                    case 0x295:
                        exchanged(method, "eax", "ebp");
                        break;
                    /* XCHG ESI,EAX */
                    case 0x296:
                        exchanged(method, "eax", "esi");
                        break;
                    /* XCHG EDI,EAX */
                    case 0x297:
                        exchanged(method, "eax", "edi");
                        break;
                    /* CWDE */
                    case 0x298:
                        method.append("CPU_Regs.reg_eax.dword((long)((short)CPU_Regs.reg_eax.word()));");
                        break;
                    /* CDQ */
                    case 0x299:
                        method.append("if ((CPU_Regs.reg_eax.dword() & 0x80000000l)!=0) CPU_Regs.reg_edx.dword(0xffffffffl);");
                        method.append("else CPU_Regs.reg_edx.dword(0l);");
                        break;
                    /* CALL FAR Ad */
                    case 0x29a:
                    {
                        long newip=decode_fetchd();
                        int newcs=decode_fetchw();
                        method.append("{Flags.FillFlags();");
                        GETIP(method);
                        method.append("CPU.CPU_CALL(true,");method.append(newcs);method.append(",");method.append(newip);method.append("l,eip);");
                        if (CPU_TRAP_CHECK) {
                            method.append("if (CPU_Regs.GETFLAG(CPU_Regs.TF)!=0) {");
                            returnTrap(method);
                            method.append("}");
                        }
                        method.append("} return Constants.BR_Link1;");
                        result = RESULT_RETURN;
                        break;
                    }
                    /* PUSHFD */
                    case 0x29c:
                        method.append("if (CPU.CPU_PUSHF(true))"); RUNEXCEPTION(method);
                        break;
                    /* POPFD */
                    case 0x29d:
                        method.append("if (CPU.CPU_POPF(true))");RUNEXCEPTION(method);
                        if (CPU_TRAP_CHECK) {
                            method.append("if (CPU_Regs.GETFLAG(CPU_Regs.TF)!=0) {");
                            method.append("CPU.cpudecoder=Core_dynrec2.CPU_Core_Dynrec_Trap_Run;");
                            decodeEnd(method);
                            method.append("}");
                        }
                        if (CPU_PIC_CHECK) {
                            method.append("if (CPU_Regs.GETFLAG(CPU_Regs.IF)!=0 && Pic.PIC_IRQCheck!=0) {");
                            SAVEIP(method);
                            decodeEnd(method);
                            method.append("}");
                        }
                        break;
                    /* MOV EAX,Od */
                    case 0x2a1:
                        method.append("CPU_Regs.reg_eax.dword(Memory.mem_readd(");GetEADirect(method);method.append("));");
                        break;
                    /* MOV Od,EAX */
                    case 0x2a3:
                        method.append("Memory.mem_writed(");GetEADirect(method);method.append(", CPU_Regs.reg_eax.dword());");
                        break;
                    /* MOVSD */
                    case 0x2a5:
                        DoString(method, "R_MOVSD");
                        break;
                    /* CMPSD */
                    case 0x2a7:
                        DoString(method, "R_CMPSD");
                        break;
                    /* TEST EAX,Id */
                    case 0x2a9:
                        method.append("Instructions.TESTD(");method.append(decode_fetchd());method.append("l,CPU_Regs.reg_eax.dword());");
                        break;
                    /* STOSD */
                    case 0x2ab:
                        DoString(method, "R_STOSD");
                        break;
                    /* LODSD */
                    case 0x2ad:
                        DoString(method, "R_LODSD");
                        break;
                    /* SCASD */
                    case 0x2af:
                        DoString(method, "R_SCASD");
                        break;
                    /* MOV EAX,Id */
                    case 0x2b8:
                        method.append("CPU_Regs.reg_eax.dword(");method.append(decode_fetchd());method.append("l);");
                        break;
                    /* MOV ECX,Id */
                    case 0x2b9:
                        method.append("CPU_Regs.reg_ecx.dword(");method.append(decode_fetchd());method.append("l);");
                        break;
                    /* MOV EDX,Id */
                    case 0x2ba:
                        method.append("CPU_Regs.reg_edx.dword(");method.append(decode_fetchd());method.append("l);");
                        break;
                    /* MOV EBX,Id */
                    case 0x2bb:
                        method.append("CPU_Regs.reg_ebx.dword(");method.append(decode_fetchd());method.append("l);");
                        break;
                    /* MOV ESP,Id */
                    case 0x2bc:
                        method.append("CPU_Regs.reg_esp.dword(");method.append(decode_fetchd());method.append("l);");
                        break;
                    /* MOV EBP,Id */
                    case 0x2bd:
                        method.append("CPU_Regs.reg_ebp.dword(");method.append(decode_fetchd());method.append("l);");
                        break;
                    /* MOV ESI,Id */
                    case 0x2be:
                        method.append("CPU_Regs.reg_esi.dword(");method.append(decode_fetchd());method.append("l);");
                        break;
                    /* MOV EDI,Id */
                    case 0x2bf:
                        method.append("CPU_Regs.reg_edi.dword(");method.append(decode_fetchd());method.append("l);");
                        break;
                    /* GRP2 Ed,Ib */
                    case 0x2c1:
                        GRP2D_fetchb(method);
                        break;
                    /* RETN Iw */
                    case 0x2c2:
                        method.append("CPU_Regs.reg_eip(CPU.CPU_Pop32());");
                        method.append("CPU_Regs.reg_esp.dword(CPU_Regs.reg_esp.dword() + ");method.append(decode_fetchw());method.append(");");
                        result = RESULT_CONTINUE;
                        break;
                    /* RETN */
                    case 0x2c3:
                        method.append("CPU_Regs.reg_eip(CPU.CPU_Pop32());");
                        result = RESULT_CONTINUE;
                        break;
                    /* LES */
                    case 0x2c4:
                    {
                        int rm=decode_fetchb();
                        if (rm >= 0xc0) result = RESULT_ILLEGAL_INSTRUCTION;
                        else {
                            method.append("{long eaa=");getEaa(method, rm);
                            method.append("if (CPU.CPU_SetSegGeneralES(Memory.mem_readw(eaa+4)))");RUNEXCEPTION(method);
                            method.append(getrd(rm));method.append("(Memory.mem_readd(eaa));}");
                        }
                        break;
                    }
                    /* LDS */
                    case 0x2c5:
                    {
                        int rm=decode_fetchb();
                        if (rm >= 0xc0) result = RESULT_ILLEGAL_INSTRUCTION;
                        else {
                            method.append("{long eaa=");getEaa(method, rm);
                            method.append("if (CPU.CPU_SetSegGeneralDS(Memory.mem_readw(eaa+4)))");RUNEXCEPTION(method);
                            method.append(getrd(rm));method.append("(Memory.mem_readd(eaa));}");
                            segChanged = true;
                        }
                        break;
                    }
                    /* MOV Ed,Id */
                    case 0x2c7:
                    {
                        int rm=decode_fetchb();
                        if (rm >= 0xc0) {
                            method.append(geteard(rm));method.append("(");method.append(decode_fetchd());method.append("l);");
                        } else {
                            method.append("{long eaa=");getEaa(method, rm);
                            method.append("Memory.mem_writed(eaa,");method.append(decode_fetchd());method.append("l);}");
                        }
                    }
                        break;
                    /* ENTER Iw,Ib */
                    case 0x2c8:
                    {
                        /*Bitu*/int bytes=decode_fetchw();
                        /*Bitu*/int level=decode_fetchb();
                        method.append("CPU.CPU_ENTER(true,");method.append(bytes);method.append(",");method.append(level);method.append(");");
                        break;
                    }
                    /* LEAVE */
                    case 0x2c9:
                        method.append("CPU_Regs.reg_esp.dword(CPU_Regs.reg_esp.dword() & CPU.cpu.stack.notmask);");
                        method.append("CPU_Regs.reg_esp.dword(CPU_Regs.reg_esp.dword() | (CPU_Regs.reg_ebp.dword() & CPU.cpu.stack.mask));");
                        method.append("CPU_Regs.reg_ebp.dword(CPU.CPU_Pop32());");
                        break;
                    /* RETF Iw */
                    case 0x2ca:
                    {
                        /*Bitu*/int words=decode_fetchw();
                        method.append("Flags.FillFlags();");
                        GETIP(method);
                        method.append("CPU.CPU_RET(true,");method.append(words);method.append(",eip);");
                        result = RESULT_CONTINUE;
                        break;
                    }
                    /* RETF */
                    case 0x2cb:
                        method.append("Flags.FillFlags();");
                        GETIP(method);
                        method.append("CPU.CPU_RET(true,0,eip);");
                        result = RESULT_CONTINUE;
                        break;
                    /* IRET */
                    case 0x2cf:
                        method.append("{");
                        GETIP(method);
                        method.append("CPU.CPU_IRET(true,eip);");
                        if (CPU_TRAP_CHECK) {
                            method.append("if (CPU_Regs.GETFLAG(CPU_Regs.TF)!=0) {");
                            returnTrap(method);
                            method.append("}");
                        }
                         if (CPU_PIC_CHECK) {
                            method.append("if (CPU_Regs.GETFLAG(CPU_Regs.IF)!=0 && Pic.PIC_IRQCheck!=0)");
                            returnNone(method);
                         }
                        method.append("}");
                        result = RESULT_CONTINUE;
                        break;
                    /* GRP2 Ed,1 */
                    case 0x2d1:
                        GRP2D(method, 1);
                        break;
                    /* GRP2 Ed,CL */
                    case 0x2d3:
                        GRP2D(method, "CPU_Regs.reg_ecx.low()");
                        break;
                    /* LOOPNZ */
                    case 0x2e0:
                        if ((prefixes & Core.PREFIX_ADDR)!=0) {
                            method.append("CPU_Regs.reg_ecx.dword(CPU_Regs.reg_ecx.dword() -1);");
                            JumpCond32_b(method, "CPU_Regs.reg_ecx.dword() !=0 && !Flags.get_ZF()");
                        } else {
                            method.append("CPU_Regs.reg_ecx.word(CPU_Regs.reg_ecx.word()-1);");
                            JumpCond32_b(method, "CPU_Regs.reg_ecx.word()!=0 && !Flags.get_ZF()");
                        }
                        result = RESULT_RETURN;
                        break;
                    /* LOOPZ */
                    case 0x2e1:
                        if ((prefixes & Core.PREFIX_ADDR)!=0) {
                            method.append("CPU_Regs.reg_ecx.dword(CPU_Regs.reg_ecx.dword() -1);");
                            JumpCond32_b(method, "CPU_Regs.reg_ecx.dword() !=0 && Flags.get_ZF()");
                        } else {
                            method.append("CPU_Regs.reg_ecx.word(CPU_Regs.reg_ecx.word()-1);");
                            JumpCond32_b(method, "CPU_Regs.reg_ecx.word()!=0 && Flags.get_ZF()");
                        }
                        result = RESULT_RETURN;
                        break;
                    /* LOOP */
                    case 0x2e2:
                        if ((prefixes & Core.PREFIX_ADDR)!=0) {
                            method.append("CPU_Regs.reg_ecx.dword(CPU_Regs.reg_ecx.dword() -1);");
                            JumpCond32_b(method, "CPU_Regs.reg_ecx.dword() !=0");
                        } else {
                            method.append("CPU_Regs.reg_ecx.word(CPU_Regs.reg_ecx.word()-1);");
                            JumpCond32_b(method, "CPU_Regs.reg_ecx.word()!=0");
                        }
                        result = RESULT_RETURN;
                        break;
                    /* JCXZ */
                    case 0x2e3:
                        if ((prefixes & Core.PREFIX_ADDR)==0)
                            JumpCond32_b(method, "(CPU_Regs.reg_ecx.dword() & 0x0000ffffl)==0");
                        else
                            JumpCond32_b(method, "(CPU_Regs.reg_ecx.dword() & 0xffffffffl)==0");
                        result = RESULT_RETURN;
                        break;
                    /* IN EAX,Ib */
                    case 0x2e5:
                    {
                        int port=decode_fetchb();
                        method.append("if (CPU.CPU_IO_Exception(");method.append(port);method.append(",4))");RUNEXCEPTION(method);
                        method.append("CPU_Regs.reg_eax.dword(IO.IO_ReadD(");method.append(port);method.append("));");
                        break;
                    }
                    /* OUT Ib,EAX */
                    case 0x2e7:
                    {
                        int port=decode_fetchb();
                        method.append("if (CPU.CPU_IO_Exception(");method.append(port);method.append(",4))");RUNEXCEPTION(method);
                        method.append("IO.IO_WriteD(");method.append(port);method.append(", CPU_Regs.reg_eax.dword());");
                        break;
                    }
                    /* CALL Jd */
                    case 0x2e8:
                    {
                        int addip=decode_fetchds();
                        SAVEIP(method);
                        method.append("CPU.CPU_Push32(CPU_Regs.reg_eip());");
                        method.append("CPU_Regs.reg_eip(CPU_Regs.reg_eip()+");method.append(addip);method.append(");");
                        method.append("return Constants.BR_Link1;");
                        result = RESULT_RETURN;
                        break;
                    }
                    /* JMP Jd */
                    case 0x2e9:
                    {
                        int addip=decode_fetchds();
                        SAVEIP(method);
                        method.append("CPU_Regs.reg_eip(CPU_Regs.reg_eip()+");method.append(addip);method.append(");");
                        method.append("return Constants.BR_Link1;");
                        result = RESULT_RETURN;
                        break;
                    }
                    /* JMP Ad */
                    case 0x2ea:
                    {
                        long newip=decode_fetchd();
                        int newcs=decode_fetchw();
                        method.append("{Flags.FillFlags();");
                        GETIP(method);
                        method.append("CPU.CPU_JMP(true,");method.append(newcs);method.append(",(int)");method.append(newip);method.append("l,eip);");
                        if (CPU_TRAP_CHECK) {
                            method.append("if (CPU_Regs.GETFLAG(CPU_Regs.TF)!=0) {");
                            returnTrap(method);
                            method.append("}");
                        }
                        method.append("} return Constants.BR_Link1;");
                        result = RESULT_RETURN;
                        break;
                    }
                    /* JMP Jb */
                    case 0x2eb:
                    {
                        int addip=decode_fetchbs();
                        SAVEIP(method);
                        method.append("CPU_Regs.reg_eip(CPU_Regs.reg_eip()+");method.append(addip);method.append(");");
                        method.append("return Constants.BR_Link1;");
                        result = RESULT_RETURN;
                        break;
                    }
                    /* IN EAX,DX */
                    case 0x2ed:
                        method.append("CPU_Regs.reg_eax.dword(IO.IO_ReadD(CPU_Regs.reg_edx.word()));");
                        break;
                    /* OUT DX,EAX */
                    case 0x2ef:
                        method.append("IO.IO_WriteD(CPU_Regs.reg_edx.word(),CPU_Regs.reg_eax.dword());");
                        break;
                    /* GRP3 Ed(,Id) */
                    case 0x2f7:
                        Grp3EdId(method);
                        break;
                    /* GRP 5 Ed */
                    case 0x2ff:
                        result = grp5ed(method);
                        break;
                    /* GRP 6 Exxx */
                    case 0x300:
                        result = grp6(method);
                        break;
                    /* Group 7 Ed */
                    case 0x301:
                        result = grp7ed(method);
                        break;
                    /* LAR Gd,Ed */
                    case 0x302:
                    {
                        method.append("if ((CPU_Regs.flags & CPU_Regs.VM)!=0 || (!CPU.cpu.pmode))");returnIllegal(method);
                        int rm=decode_fetchb();
                        method.append("{IntRef rd = new IntRef((int)");method.append(getrd(rm));method.append("());");
                        if (rm >= 0xc0) {
                            method.append("CPU.CPU_LAR(");method.append(getearw(rm));method.append("(),rd);");
                        } else {
                            method.append("long eaa=");getEaa(method, rm);
                            method.append("CPU.CPU_LAR(Memory.mem_readw(eaa),rd);");
                        }
                        method.append(getrd(rm));method.append("((long)rd.value);}");
                        break;
                    }
                    /* LSL Gd,Ew */
                    case 0x303:
                    {
                        method.append("if ((CPU_Regs.flags & CPU_Regs.VM)!=0 || (!CPU.cpu.pmode))");returnIllegal(method);
                        short rm=decode_fetchb();
                        method.append("{IntRef int_ref_1 = new IntRef((int)");method.append(getrd(rm));method.append("());");
                        if (rm >= 0xc0) {
                            method.append("CPU.CPU_LSL(");method.append(getearw(rm));method.append("(),int_ref_1);");
                        } else {
                            method.append("{long eaa=");getEaa(method, rm);
                            method.append("CPU.CPU_LSL(Memory.mem_readw(eaa),int_ref_1);}");
                        }
                        method.append(getrd(rm));method.append("((long)int_ref_1.value);}");
                        break;
                    }
                    /* JO */
                    case 0x380:
                        JumpCond32_d(method, "Flags.TFLG_O()");
                        result = RESULT_RETURN;
                        break;
                    /* JNO */
                    case 0x381:
                        JumpCond32_d(method, "Flags.TFLG_NO()");
                        result = RESULT_RETURN;
                        break;
                    /* JB */
                    case 0x382:
                        JumpCond32_d(method, "Flags.TFLG_B()");
                        result = RESULT_RETURN;
                        break;
                    /* JNB */
                    case 0x383:
                        JumpCond32_d(method, "Flags.TFLG_NB()");
                        result = RESULT_RETURN;
                        break;
                    /* JZ */
                    case 0x384:
                        JumpCond32_d(method, "Flags.TFLG_Z()");
                        result = RESULT_RETURN;
                        break;
                    /* JNZ */
                    case 0x385:
                        JumpCond32_d(method, "Flags.TFLG_NZ()");
                        result = RESULT_RETURN;
                        break;
                    /* JBE */
                    case 0x386:
                        JumpCond32_d(method, "Flags.TFLG_BE()");
                        result = RESULT_RETURN;
                        break;
                    /* JNBE */
                    case 0x387:
                        JumpCond32_d(method, "Flags.TFLG_NBE()");
                        result = RESULT_RETURN;
                        break;
                    /* JS */
                    case 0x388:
                        JumpCond32_d(method, "Flags.TFLG_S()");
                        result = RESULT_RETURN;
                        break;
                    /* JNS */
                    case 0x389:
                        JumpCond32_d(method, "Flags.TFLG_NS()");
                        result = RESULT_RETURN;
                        break;
                    /* JP */
                    case 0x38a:
                        JumpCond32_d(method, "Flags.TFLG_P()");
                        result = RESULT_RETURN;
                        break;
                    /* JNP */
                    case 0x38b:
                        JumpCond32_d(method, "Flags.TFLG_NP()");
                        result = RESULT_RETURN;
                        break;
                    /* JL */
                    case 0x38c:
                        JumpCond32_d(method, "Flags.TFLG_L()");
                        result = RESULT_RETURN;
                        break;
                    /* JNL */
                    case 0x38d:
                        JumpCond32_d(method, "Flags.TFLG_NL()");
                        result = RESULT_RETURN;
                        break;
                    /* JLE */
                    case 0x38e:
                        JumpCond32_d(method, "Flags.TFLG_LE()");
                        result = RESULT_RETURN;
                        break;
                    /* JNLE */
                    case 0x38f:
                        JumpCond32_d(method, "Flags.TFLG_NLE()");
                        result = RESULT_RETURN;
                        break;
                    /* PUSH FS */
                    case 0x3a0:
                        method.append("CPU.CPU_Push32(CPU.Segs_FSval);");
                        break;
                    /* POP FS */
                    case 0x3a1:
                        method.append("if (CPU.CPU_PopSegFS(true))");RUNEXCEPTION(method);
                        break;
                    /* BT Ed,Gd */
                    case 0x3a3:
                    {
                        method.append("Flags.FillFlags();");
                        int rm=decode_fetchb();
                        method.append("{long mask=1 << ((int)");method.append(getrd(rm));method.append("() & 31);");
                        if (rm >= 0xc0 ) {
                            method.append("CPU_Regs.SETFLAGBIT(CPU_Regs.CF,(");method.append(geteard(rm));method.append("() & mask)!=0);");
                        } else {
                            method.append("long eaa = ");getEaa(method, rm);
                            method.append("eaa+=(((int)");method.append(getrd(rm));method.append("())>>5)*4;");
                            method.append("long old=Memory.mem_readd(eaa);");
                            method.append("CPU_Regs.SETFLAGBIT(CPU_Regs.CF,(old & mask)!=0);");
                        }
                        method.append("}");
                        break;
                    }
                    /* SHLD Ed,Gd,Ib */
                    case 0x3a4:
                    {
                        int rm=decode_fetchb();
                        if (rm >= 0xc0 ) {
                            int op3 = decode_fetchb() & 0x1F;
                            if (op3!=0) {
                                method.append(geteard(rm));method.append("(Instructions.DSHLD(");
                                method.append(getrd(rm));method.append("(),");method.append(op3);method.append("l,");method.append(geteard(rm));method.append("()));");
                            }
                        }
                        else {
                            method.append("{long eaa = ");getEaa(method, rm);
                            int op3 = decode_fetchb() & 0x1F;
                            if (op3!=0) {
                                method.append("Memory.mem_writed(eaa, Instructions.DSHLD(");
                                method.append(getrd(rm));method.append("(),");method.append(op3);method.append("l,Memory.mem_readd(eaa)));");
                            }
                            method.append("}");
                        }
                        break;
                    }
                    /* SHLD Ed,Gd,CL */
                    case 0x3a5:
                    {
                        int rm=decode_fetchb();
                        method.append("{long val = CPU_Regs.reg_ecx.dword() & 0x1f;");
                        if (rm >= 0xc0 ) {
                            method.append("if (val != 0) ");
                                method.append(geteard(rm));method.append("(Instructions.DSHLD(");method.append(getrd(rm));method.append("(),val,");method.append(geteard(rm));method.append("()));");
                        }
                        else {
                            method.append("long eaa = ");getEaa(method, rm);
                            method.append("if (val != 0) ");
                                method.append("Memory.mem_writed(eaa, Instructions.DSHLD(");method.append(getrd(rm));method.append("(),val,Memory.mem_readd(eaa)));");
                        }
                        break;
                    }
                    /* PUSH GS */
                    case 0x3a8:
                        method.append("CPU.CPU_Push32(CPU.Segs_GSval);");
                        break;
                    /* POP GS */
                    case 0x3a9:
                        method.append("if (CPU.CPU_PopSegGS(true))");RUNEXCEPTION(method);
                        break;
                    /* BTS Ed,Gd */
                    case 0x3ab:
                    {
                        method.append("Flags.FillFlags();");
                        int rm=decode_fetchb();
                        method.append("{long mask=1 << ((int)");method.append(getrd(rm));method.append("() & 31);");
                        if (rm >= 0xc0 ) {
                            method.append("CPU_Regs.SETFLAGBIT(CPU_Regs.CF,(");method.append(geteard(rm));method.append("() & mask)!=0);");
                            method.append(geteard(rm));method.append("(");method.append(geteard(rm));method.append("()|mask);");
                        } else {
                            method.append("long eaa = ");getEaa(method, rm);
                            method.append("eaa+=(((int)");method.append(getrd(rm));method.append("())>>5)*4;");
                            method.append("long old=Memory.mem_readd(eaa);");
                            method.append("CPU_Regs.SETFLAGBIT(CPU_Regs.CF,(old & mask)!=0);");
                            method.append("Memory.mem_writed(eaa,old | mask);");
                        }
                        method.append("}");
                        break;
                    }
                    /* SHRD Ed,Gd,Ib */
                    case 0x3ac:
                    {
                         int rm=decode_fetchb();
                        if (rm >= 0xc0 ) {
                            int op3 = decode_fetchb() & 0x1F;
                            if (op3!=0) {
                                method.append(geteard(rm));method.append("(Instructions.DSHRD(");method.append(getrd(rm));method.append("(),");method.append(op3);method.append("l,");method.append(geteard(rm));method.append("()));");
                            }
                        }
                        else {
                            method.append("long eaa = ");getEaa(method, rm);
                            int op3 = decode_fetchb() & 0x1F;
                            if (op3!=0)
                                method.append("Memory.mem_writed(eaa, Instructions.DSHRD(");method.append(getrd(rm));method.append("(),");method.append(op3);method.append("l,Memory.mem_readd(eaa)));");
                        }
                        break;
                    }
                    /* SHRD Ed,Gd,CL */
                    case 0x3ad:
                    {
                        int rm=decode_fetchb();
                        method.append("{long val = CPU_Regs.reg_ecx.dword() & 0x1f;");
                        if (rm >= 0xc0 ) {
                            method.append("if (val != 0) {");
                                method.append(geteard(rm));method.append("(Instructions.DSHRD(");method.append(getrd(rm));method.append("(),val,");method.append(geteard(rm));method.append("()));");
                            method.append("}");
                        }
                        else {
                            method.append("long eaa = ");getEaa(method, rm);
                            method.append("if (val != 0)");
                                method.append("Memory.mem_writed(eaa, Instructions.DSHRD(");method.append(getrd(rm));method.append("(),val,Memory.mem_readd(eaa)));");
                        }
                        method.append("}");
                        break;
                    }
                    /* IMUL Gd,Ed */
                    case 0x3af:
                    {
                        int rm=decode_fetchb();
                        if (rm >= 0xc0 ) {
                            method.append(getrd(rm));method.append("(Instructions.DIMULD(");method.append(geteard(rm));method.append("(),");method.append(getrd(rm));method.append("()));");
                        }
                        else {
                            method.append("{long eaa = ");getEaa(method, rm);
                            method.append(getrd(rm));method.append("(Instructions.DIMULD(Memory.mem_readd(eaa),");method.append(getrd(rm));method.append("()));}");
                        }
                        break;
                    }
                    /* CMPXCHG Ed,Gd */
                    case 0x3b1:
                    {
                        if (CPU.CPU_ArchitectureType<CPU.CPU_ARCHTYPE_486OLDSLOW)
                            result = RESULT_ILLEGAL_INSTRUCTION;
                        else {
                            method.append("Flags.FillFlags();");
                            int rm=decode_fetchb();
                            if (rm >= 0xc0) {
                                method.append("if (");method.append(geteard(rm));method.append("()==CPU_Regs.reg_eax.dword()) {");
                                    method.append(geteard(rm));method.append("(");method.append(getrd(rm));method.append("());");
                                    method.append("CPU_Regs.SETFLAGBIT(CPU_Regs.ZF,true);");
                                method.append("} else {");
                                    method.append("CPU_Regs.reg_eax.dword(");method.append(geteard(rm));method.append("());");
                                    method.append("CPU_Regs.SETFLAGBIT(CPU_Regs.ZF,false);");
                                method.append("}");
                            } else {
                                method.append("{long eaa = ");getEaa(method, rm);
                                method.append("long val=Memory.mem_readd(eaa);");
                                method.append("if (val==CPU_Regs.reg_eax.dword()) {");
                                    method.append("Memory.mem_writed(eaa,");method.append(getrd(rm));method.append("());");
                                    method.append("CPU_Regs.SETFLAGBIT(CPU_Regs.ZF,true);");
                                method.append("} else {");
                                    method.append("Memory.mem_writed(eaa,val);");	// cmpxchg always issues a write
                                    method.append("CPU_Regs.reg_eax.dword(val);");
                                    method.append("CPU_Regs.SETFLAGBIT(CPU_Regs.ZF,false);");
                                method.append("}}");
                            }
                        }
                        break;
                    }
                    /* LSS Ed */
                    case 0x3b2:
                    {
                        int rm=decode_fetchb();
                        if (rm >= 0xc0) result = RESULT_ILLEGAL_INSTRUCTION;
                        else {
                            method.append("{long eaa = ");getEaa(method, rm);
                            method.append("if (CPU.CPU_SetSegGeneralSS(Memory.mem_readw(eaa+4)))");RUNEXCEPTION(method);
                            method.append(getrd(rm));method.append("(Memory.mem_readd(eaa));}");
                            segChanged = true;
                        }
                        break;
                    }
                    /* BTR Ed,Gd */
                    case 0x3b3:
                    {
                        method.append("Flags.FillFlags();");
                        int rm=decode_fetchb();
                        method.append("{long mask=1 << ((int)");method.append(getrd(rm));method.append("() & 31);");
                        if (rm >= 0xc0 ) {
                            method.append("CPU_Regs.SETFLAGBIT(CPU_Regs.CF,(");method.append(geteard(rm));method.append("() & mask)!=0);");
                            method.append(geteard(rm));method.append("(");method.append(geteard(rm));method.append("() & !mask);");
                        } else {
                            method.append("long eaa = ");getEaa(method, rm);
                            method.append("eaa+=(((int)");method.append(getrd(rm));method.append("())>>5)*4;");
                            method.append("long old=Memory.mem_readd(eaa);");
                            method.append("CPU_Regs.SETFLAGBIT(CPU_Regs.CF,(old & mask)!=0);");
                            method.append("Memory.mem_writed(eaa,old & ~mask);");
                        }
                        method.append("}");
                    }
                    /* LFS Ed */
                    case 0x3b4:
                    {
                        int rm=decode_fetchb();
                        if (rm >= 0xc0) result = RESULT_ILLEGAL_INSTRUCTION;
                        else {
                            method.append("{long eaa = ");getEaa(method, rm);
                            method.append("if (CPU.CPU_SetSegGeneralFS(Memory.mem_readw(eaa+4)))");RUNEXCEPTION(method);
                            method.append(getrd(rm));method.append("(Memory.mem_readd(eaa));}");
                        }
                        break;
                    }
                    /* LGS Ed */
                    case 0x3b5:
                    {
                        int rm=decode_fetchb();
                        if (rm >= 0xc0) result = RESULT_ILLEGAL_INSTRUCTION;
                        else {
                            method.append("{long eaa = ");getEaa(method, rm);
                            method.append("if (CPU.CPU_SetSegGeneralGS(Memory.mem_readw(eaa+4)))");RUNEXCEPTION(method);
                            method.append(getrd(rm));method.append("(Memory.mem_readd(eaa));}");
                        }
                        break;
                    }
                    /* MOVZX Gd,Eb */
                    case 0x3b6:
                    {
                        int rm=decode_fetchb();
                        if (rm >= 0xc0 ) {
                            method.append(getrd(rm));method.append("((long)");method.append(getearb(rm));method.append("());");
                        } else {
                            method.append("{long eaa=");getEaa(method, rm);
                            method.append(getrd(rm));method.append("((long)Memory.mem_readb(eaa));}");
                        }
                        break;
                    }
                    /* MOVXZ Gd,Ew */
                    case 0x3b7:
                    {
                        int rm=decode_fetchb();
                        if (rm >= 0xc0 ) {
                            method.append(getrd(rm));method.append("((long)");method.append(getearw(rm));method.append("());");
                        } else {
                            method.append("{long eaa = ");getEaa(method,rm);
                            method.append(getrd(rm));method.append("((long)Memory.mem_readw(eaa));}");
                        }
                        break;
                    }
                    /* GRP8 Ed,Ib */
                    case 0x3ba:
                        Grp8EdIb(method);
                        break;
                    /* BTC Ed,Gd */
                    case 0x3bb:
                    {
                        method.append("Flags.FillFlags();");
                        int rm=decode_fetchb();
                        method.append("{long mask=1 << ((int)");method.append(getrd(rm));method.append("() & 31);");
                        if (rm >= 0xc0 ) {
                            method.append("CPU_Regs.SETFLAGBIT(CPU_Regs.CF,(");method.append(geteard(rm));method.append("() & mask)!=0);");
                            method.append(geteard(rm));method.append("(");method.append(geteard(rm));method.append("()^mask);");
                        } else {
                            method.append("long eaa = ");getEaa(method, rm);
                            method.append("eaa+=(((int)");method.append(getrd(rm));method.append("())>>5)*4;");
                            method.append("long old=Memory.mem_readd(eaa);");
                            method.append("CPU_Regs.SETFLAGBIT(CPU_Regs.CF,(old & mask)!=0);");
                            method.append("Memory.mem_writed(eaa,old ^ mask);");
                        }
                        method.append("}");
                        break;
                    }
                    /* BSF Gd,Ed */
                    case 0x3bc:
                    {
                        int rm=decode_fetchb();
                        method.append("{long value;");
                        if (rm >= 0xc0) {
                            method.append("value=");method.append(geteard(rm));method.append("();");
                        } else {
                            method.append("long eaa = ");getEaa(method, rm);
                            method.append("value=Memory.mem_readd(eaa);");
                        }
                        method.append("if (value==0) {");
                            method.append("CPU_Regs.SETFLAGBIT(CPU_Regs.ZF,true);");
                        method.append("} else {");
                            method.append("long result = 0;");
                            method.append("while ((value & 0x01)==0) { result++; value>>=1; }");
                            method.append("CPU_Regs.SETFLAGBIT(CPU_Regs.ZF,false);");
                            method.append(getrd(rm));method.append("(result);");
                        method.append("}}");
                        method.append("Flags.lflags.type=Flags.t_UNKNOWN;");
                        break;
                    }
                    /* BSR Gd,Ed */
                    case 0x3bd:
                    {
                        int rm=decode_fetchb();
                        method.append("{long value;");
                        if (rm >= 0xc0) {
                            method.append("value=");method.append(geteard(rm));method.append("();");
                        } else {
                            method.append("long eaa = ");getEaa(method, rm);
                            method.append("value=Memory.mem_readd(eaa);");
                        }
                        method.append("if (value==0) {");
                            method.append("CPU_Regs.SETFLAGBIT(CPU_Regs.ZF,true);");
                        method.append("} else {");
                            method.append("long result = 31;");	// Operandsize-1
                            method.append("while ((value & 0x80000000l)==0) { result--; value<<=1; }");
                           method.append(getrd(rm));method.append("(result);");
                        method.append("}}");
                        method.append("Flags.lflags.type=Flags.t_UNKNOWN;");
                        break;
                    }
                    /* MOVSX Gd,Eb */
                    case 0x3be:
                    {
                        int rm=decode_fetchb();
                        if (rm >= 0xc0 ) {
                            method.append(getrd(rm));method.append("((long)((byte)");method.append(getearb(rm));method.append("()));");
                        } else {
                            method.append("long eaa = ");getEaa(method, rm);
                            method.append(getrd(rm));method.append("((long)((byte)Memory.mem_readb(eaa)));");
                        }
                        break;
                    }
                    /* MOVSX Gd,Ew */
                    case 0x3bf:
                    {
                        int rm=decode_fetchb();
                        if (rm >= 0xc0 ) {
                            method.append(getrd(rm));method.append("((long)((short)");method.append(getearw(rm));method.append("()));");
                        } else {
                            method.append("long eaa = ");getEaa(method, rm);
                            method.append(getrd(rm));method.append("((long)((short)Memory.mem_readw(eaa)));");  // Yes that short signed cast is intentional
                        }
                        break;
                    }
                    /* XADD Gd,Ed */
                    case 0x3c1:
                    {
                        if (CPU.CPU_ArchitectureType<CPU.CPU_ARCHTYPE_486OLDSLOW)
                            result = RESULT_ILLEGAL_INSTRUCTION;
                        else {
                            int rm=decode_fetchb();
                            method.append("{long oldrmrd=");method.append(getrd(rm));method.append("();");
                             if (rm >= 0xc0 ) {
                                method.append(getrd(rm));method.append("(");method.append(geteard(rm));method.append("());");
                                method.append(geteard(rm));method.append("(");method.append(geteard(rm));method.append("()+oldrmrd);");
                            } else {
                                method.append("long eaa=");getEaa(method, rm);
                                method.append(getrd(rm));method.append("(Memory.mem_readd(eaa));");
                                method.append("Memory.mem_writed(eaa,Memory.mem_readd(eaa)+oldrmrd);");
                            }
                        }
                        break;
                    }
                    /* BSWAP EAX */
                    case 0x3c8:
                        if (CPU.CPU_ArchitectureType<CPU.CPU_ARCHTYPE_486OLDSLOW)
                            result = RESULT_ILLEGAL_INSTRUCTION;
                        else
		                    method.append("CPU_Regs.reg_eax.dword(Instructions.BSWAPD(CPU_Regs.reg_eax.dword()));");
                        break;
                    /* BSWAP ECX */
                    case 0x3c9:
                        if (CPU.CPU_ArchitectureType<CPU.CPU_ARCHTYPE_486OLDSLOW)
                            result = RESULT_ILLEGAL_INSTRUCTION;
                        else
		                    method.append("CPU_Regs.reg_ecx.dword(Instructions.BSWAPD(CPU_Regs.reg_ecx.dword()));");
                        break;
                    /* BSWAP EDX */
                    case 0x3ca:
                        if (CPU.CPU_ArchitectureType<CPU.CPU_ARCHTYPE_486OLDSLOW)
                            result = RESULT_ILLEGAL_INSTRUCTION;
                        else
		                    method.append("CPU_Regs.reg_edx.dword(Instructions.BSWAPD(CPU_Regs.reg_edx.dword()));");
                        break;
                    /* BSWAP EBX */
                    case 0x3cb:
                        if (CPU.CPU_ArchitectureType<CPU.CPU_ARCHTYPE_486OLDSLOW)
                            result = RESULT_ILLEGAL_INSTRUCTION;
                        else
		                    method.append("CPU_Regs.reg_ebx.dword(Instructions.BSWAPD(CPU_Regs.reg_ebx.dword()));");
                        break;
                    /* BSWAP ESP */
                    case 0x3cc:
                        if (CPU.CPU_ArchitectureType<CPU.CPU_ARCHTYPE_486OLDSLOW)
                            result = RESULT_ILLEGAL_INSTRUCTION;
                        else
		                    method.append("CPU_Regs.reg_esp.dword(Instructions.BSWAPD(CPU_Regs.reg_esp.dword()));");
                        break;
                    /* BSWAP EBP */
                    case 0x3cd:
                        if (CPU.CPU_ArchitectureType<CPU.CPU_ARCHTYPE_486OLDSLOW)
                            result = RESULT_ILLEGAL_INSTRUCTION;
                        else
		                    method.append("CPU_Regs.reg_ebp.dword(Instructions.BSWAPD(CPU_Regs.reg_ebp.dword()));");
                        break;
                    /* BSWAP ESI */
                    case 0x3ce:
                        if (CPU.CPU_ArchitectureType<CPU.CPU_ARCHTYPE_486OLDSLOW)
                            result = RESULT_ILLEGAL_INSTRUCTION;
                        else
		                    method.append("CPU_Regs.reg_esi.dword(Instructions.BSWAPD(CPU_Regs.reg_esi.dword()));");
                        break;
                    /* BSWAP EDI */
                    case 0x3cf:
                        if (CPU.CPU_ArchitectureType<CPU.CPU_ARCHTYPE_486OLDSLOW)
                            result = RESULT_ILLEGAL_INSTRUCTION;
                        else
		                    method.append("CPU_Regs.reg_edi.dword(Instructions.BSWAPD(CPU_Regs.reg_edi.dword()));");
                        break;
                    default:
                        //System.out.println("Not implemented: "+Integer.toString(opcode, 16));
                        result = RESULT_ILLEGAL_INSTRUCTION;
//                        if (segChanged) {
//                            Log.exit("Unhandled instruction during segment change");
//                        }
                }
//                    } finally {
//                    method.append("} finally {Debug.stop(Debug.TYPE_CPU, ");method.append(opcode);method.append(");}");
//                }
                if (CPU.cpu.code.big) {
                    opcode_index=0x200;
                    prefixes=1;
                    EA16 = false;
                } else {
                    opcode_index=0;
                    prefixes=0;
                    EA16 = true;
                }
                if (result != RESULT_RETURN) {
                    if (segChanged) {
                        method.append("Core.base_ds=CPU.Segs_DSphys;");
                        method.append("Core.base_ss=CPU.Segs_SSphys;");
                        method.append("Core.base_val_ds=CPU_Regs.ds;");
                        segChanged = false;
                    }
                }
            }
            if (result != RESULT_RETURN)
                method.append("CPU.CPU_Cycles-="+decode.cycles+";");
            switch (result) {
//                case 0:
//                    Log.exit("Oops");
//                    method.append("CPU_Regs.reg_eip+="+(decode.op_start-decode.code_start)+";");
//                    method.append("CPU.CPU_Cycles-="+decode.cycles+";");
//	                gen_jmp_ptr(&decode.block->link[0].to,offsetof(CacheBlock,cache.start));
//	                dyn_closeblock();
                case RESULT_ILLEGAL_INSTRUCTION:
                    method.append("CPU_Regs.reg_eip+="+(decode.code - decode.code_start)+";");
                    method.append("return Constants.BR_Illegal;}");
                    dyn_closeblock();
                    break;
                case RESULT_CALLBACK:
                    SAVEIP(method);
                    method.append("Data.callback = ");method.append(callback);method.append(";");
                    method.append("return Constants.BR_CallBack;}");
                    dyn_closeblock();
                    break;
                case 0:
                    method.append("CPU_Regs.reg_eip+="+(decode.code - decode.code_start)+";");
                case RESULT_CONTINUE:
                    method.append("return Constants.BR_Normal;}");
                    dyn_closeblock();
                    break;
                case RESULT_RETURN:
                    method.append("}");
                    dyn_closeblock();
                    break;
                case RESULT_MODIFIED_INSTRUCTION:
                    method.append("CPU_Regs.reg_eip+="+(decode.code - decode.code_start)+";");
                    method.append("return ModifiedDecode.call(");
                    method.append(opcode_index);
                    method.append(",");
                    method.append(prefixes);
                    method.append(",");
                    method.append(EA16);
                    method.append(");}");
                    dyn_closeblock();
                    break;
            }
            String thedigest = toHexString(md.digest(method.toString().getBytes()));

            DynamicClass compiledCode = Cache.getCode(thedigest);
            if (compiledCode == null) {
                compiledCode = (DynamicClass)codeCache.get(thedigest);
            }
            if (compiledCode == null) {
                CtClass cacheBlock = pool.makeClass("CacheBlock"+(count++));
                cacheBlock.setInterfaces(new CtClass[]{pool.getCtClass("jdos.cpu.core_dynrec2.DynamicClass")});
                CtMethod m = CtNewMethod.make(method.toString(), cacheBlock);
                cacheBlock.addMethod(m);
                // Make the dynamic class belong to its own class loader so that when we
                // release the decoder block the class and class loader will be unloaded
                URLClassLoader cl = (URLClassLoader)cacheBlock.getClass().getClassLoader();
                cl = URLClassLoader.newInstance(cl.getURLs(), cl);
                Class clazz = cacheBlock.toClass(cl);
                compiledCode = (DynamicClass)clazz.newInstance();
                cacheBlock.detach();
                Cache.cache_save(cacheBlock.getName(), thedigest, cacheBlock.toBytecode());
                codeCache.put(thedigest, compiledCode);
                compiled++;
                if (compiled % 1000 == 0)
                    System.out.println("Compiled: "+compiled);
            }
            evaluated++;
            if (evaluated % 1000 == 0)
                System.out.println("Evaluated: "+evaluated);
            decode.block.code = compiledCode;
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println(method.toString());
        }
        decode.active_block.page.end=--decode.page.index;
        return decode.block;
    }
    static public int compiled = 0;
    static public int evaluated = 0;

    static Hashtable codeCache = new Hashtable();
    public static String toHexString(byte[] bytes) {
        char[] hexArray = {'0','1','2','3','4','5','6','7','8','9','A','B','C','D','E','F'};
        char[] hexChars = new char[bytes.length * 2];
        int v;
        for ( int j = 0; j < bytes.length; j++ ) {
            v = bytes[j] & 0xFF;
            hexChars[j*2] = hexArray[v/16];
            hexChars[j*2 + 1] = hexArray[v%16];
        }
        return new String(hexChars);
    }

}
