package jdos.cpu.core_dynrec2;

import jdos.hardware.Memory;
import jdos.cpu.*;
import jdos.misc.Log;
import jdos.types.LogSeverities;
import jdos.types.LogTypes;

public class Decoder_instructions {
    static private Core_dynrec2.CodePageHandlerDynRecRef codeRef = new Core_dynrec2.CodePageHandlerDynRecRef();
    public static final DynDecode decode = new DynDecode();
    static protected boolean EA16 = false;
    static protected int prefixes = 0;

    static final protected boolean CPU_TRAP_CHECK = true;
    static protected final boolean CPU_PIC_CHECK = false;

    public final static int RESULT_ILLEGAL_INSTRUCTION = 1;
    public final static int RESULT_CALLBACK = 2;
    public final static int RESULT_CONTINUE = 3;
    public final static int RESULT_RETURN = 4;
    public final static int RESULT_MODIFIED_INSTRUCTION = 5;

    static protected void SAVEIP(StringBuffer method) {
        method.append("CPU_Regs.reg_eip+=");method.append(decode.code-decode.code_start);method.append(";");
    }

    static protected void GETIP(StringBuffer method) {
        method.append("long eip = CPU_Regs.reg_eip+");method.append(decode.code-decode.code_start);method.append(";");
    }

    protected static void SETcc(StringBuffer method, String cc) {
        /*Bit8u*/short rm=decode_fetchb();
        if (rm >= 0xc0 ) {
            method.append(getearb(rm));method.append("((short)((");method.append(cc);method.append(") ? 1:0));");
        } else {
            method.append("{long eaa = ");getEaa(method, rm);
            method.append("Memory.mem_writeb(eaa, (");method.append(cc);method.append(")?1:0);}");
        }
    }

    protected static void JumpCond16_b(StringBuffer method, String COND) {
        SAVEIP(method);
        byte b = decode_fetchbs();
        method.append("if (");method.append(COND);method.append(") { CPU_Regs.reg_ip(CPU_Regs.reg_ip()+");method.append(1+b);method.append(");");
        returnLink1(method);
        method.append("} CPU_Regs.reg_ip(CPU_Regs.reg_ip()+1);");
        returnLink2(method);
    }

    protected static void JumpCond16_w(StringBuffer method, String COND) {
        SAVEIP(method);
        short b = decode_fetchws();
        method.append("if (");method.append(COND);method.append(") { CPU_Regs.reg_ip(CPU_Regs.reg_ip()+");method.append(2+b);method.append(");");
        returnLink1(method);
        method.append("} CPU_Regs.reg_ip(CPU_Regs.reg_ip()+2);");
        returnLink2(method);
    }
    protected static void JumpCond32_d(StringBuffer method, String COND) {
        SAVEIP(method);
        int b = decode_fetchds();
        method.append("if (");method.append(COND);method.append(") { CPU_Regs.reg_eip(CPU_Regs.reg_eip()+");method.append(4+b);method.append(");");
        returnLink1(method);
        method.append("} CPU_Regs.reg_eip(CPU_Regs.reg_eip()+4);");
        returnLink2(method);
    }
     protected static void JumpCond32_b(StringBuffer method, String COND) {
        SAVEIP(method);
        int b = decode_fetchbs();
        method.append("if (");method.append(COND);method.append(") { CPU_Regs.reg_eip(CPU_Regs.reg_eip()+");method.append(1+b);method.append(");");
        returnLink1(method);
        method.append("} CPU_Regs.reg_eip(CPU_Regs.reg_eip()+1);");
        returnLink2(method);
    }
    protected static void GetEADirect(StringBuffer method) {
        if ((prefixes & Core.PREFIX_ADDR)!=0) {
            method.append("((Core.base_ds+");method.append(decode_fetchd());method.append("l) & 0xFFFFFFFFl)");
        } else {
            method.append("((Core.base_ds+");method.append(decode_fetchw());method.append(") & 0xFFFFFFFFl)");
        }
    }

    static void decode_advancepage() {
        // Advance to the next page
        decode.active_block.page.end=4095;
        // trigger possible page fault here
        decode.page.first++;
        /*Bitu*/long faddr=decode.page.first << 12;
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

    static private void Sib(StringBuffer method, int mode) {
        /*Bit8u*/short sib=decode_fetchb();
        switch (sib&7) {
        case 0:	/* EAX Base */
            method.append("Core.base_ds+CPU_Regs.reg_eax.dword()");break;
        case 1:	/* ECX Base */
            method.append("Core.base_ds+CPU_Regs.reg_ecx.dword()");break;
        case 2:	/* EDX Base */
            method.append("Core.base_ds+CPU_Regs.reg_edx.dword()");break;
        case 3:	/* EBX Base */
            method.append("Core.base_ds+CPU_Regs.reg_ebx.dword()");break;
        case 4:	/* ESP Base */
            method.append("Core.base_ss+CPU_Regs.reg_esp.dword()");break;
        case 5:	/* #1 Base */
            if (mode==0) {
                method.append("Core.base_ds+");method.append(decode_fetchd());method.append("l");break;
            } else {
                method.append("Core.base_ss+CPU_Regs.reg_ebp.dword()");break;
            }
        case 6:	/* ESI Base */
            method.append("Core.base_ds+CPU_Regs.reg_esi.dword()");break;
        case 7:	/* EDI Base */
            method.append("Core.base_ds+CPU_Regs.reg_edi.dword()");break;
        }
        int index =(sib >> 3) & 7;
        switch (index) {
            case 0:
                method.append("+(CPU_Regs.reg_eax.dword() << ");method.append((sib >> 6));method.append(")");
                break;
            case 1:
                method.append("+(CPU_Regs.reg_ecx.dword() << ");method.append((sib >> 6));method.append(")");
                break;
            case 2:
                method.append("+(CPU_Regs.reg_edx.dword() << ");method.append((sib >> 6));method.append(")");
                break;
            case 3:
                method.append("+(CPU_Regs.reg_ebx.dword() << ");method.append((sib >> 6));method.append(")");
                break;
            case 4:
                //base+=SIBZero << (sib >> 6);
                break;
            case 5:
                method.append("+(CPU_Regs.reg_ebp.dword() << ");method.append((sib >> 6));method.append(")");
                break;
            case 6:
                method.append("+(CPU_Regs.reg_esi.dword() << ");method.append((sib >> 6));method.append(")");
                break;
            case 7:
                method.append("+(CPU_Regs.reg_edi.dword() << ");method.append((sib >> 6));method.append(")");
                break;
        }
    }
    static protected void getEaa32(StringBuffer method, int rm) {
        if (rm<0x40) {
            switch (rm & 7) {
                case 0x00: method.append("(Core.base_ds+CPU_Regs.reg_eax.dword()) & 0xFFFFFFFFl");break;
                case 0x01: method.append("(Core.base_ds+CPU_Regs.reg_ecx.dword()) & 0xFFFFFFFFl");break;
                case 0x02: method.append("(Core.base_ds+CPU_Regs.reg_edx.dword()) & 0xFFFFFFFFl");break;
                case 0x03: method.append("(Core.base_ds+CPU_Regs.reg_ebx.dword()) & 0xFFFFFFFFl");break;
                case 0x04: Sib(method, 0);break;
                case 0x05: method.append("(Core.base_ds+");method.append(decode_fetchd());method.append("l) & 0xFFFFFFFFl");break;
                case 0x06: method.append("(Core.base_ds+CPU_Regs.reg_esi.dword()) & 0xFFFFFFFFl");break;
                case 0x07: method.append("(Core.base_ds+CPU_Regs.reg_edi.dword()) & 0xFFFFFFFFl");break;
            }
        } else if (rm<0x80) {
            switch (rm & 7) {
                case 0x00: method.append("(Core.base_ds+CPU_Regs.reg_eax.dword()+");method.append(decode_fetchbs());method.append(") & 0xFFFFFFFFl");break;
                case 0x01: method.append("(Core.base_ds+CPU_Regs.reg_ecx.dword()+");method.append(decode_fetchbs());method.append(") & 0xFFFFFFFFl");break;
                case 0x02: method.append("(Core.base_ds+CPU_Regs.reg_edx.dword()+");method.append(decode_fetchbs());method.append(") & 0xFFFFFFFFl");break;
                case 0x03: method.append("(Core.base_ds+CPU_Regs.reg_ebx.dword()+");method.append(decode_fetchbs());method.append(") & 0xFFFFFFFFl");break;
                case 0x04: method.append("(");Sib(method, 1);method.append("+");method.append(decode_fetchbs());method.append(") & 0xFFFFFFFFl");break;
                case 0x05: method.append("(Core.base_ss+CPU_Regs.reg_ebp.dword()+");method.append(decode_fetchbs());method.append(") & 0xFFFFFFFFl");break;
                case 0x06: method.append("(Core.base_ds+CPU_Regs.reg_esi.dword()+");method.append(decode_fetchbs());method.append(") & 0xFFFFFFFFl");break;
                case 0x07: method.append("(Core.base_ds+CPU_Regs.reg_edi.dword()+");method.append(decode_fetchbs());method.append(") & 0xFFFFFFFFl");break;
            }
        } else {
            switch (rm & 7) {
                case 0x00: method.append("(Core.base_ds+CPU_Regs.reg_eax.dword()+");method.append(decode_fetchds());method.append(") & 0xFFFFFFFFl");break;
                case 0x01: method.append("(Core.base_ds+CPU_Regs.reg_ecx.dword()+");method.append(decode_fetchds());method.append(") & 0xFFFFFFFFl");break;
                case 0x02: method.append("(Core.base_ds+CPU_Regs.reg_edx.dword()+");method.append(decode_fetchds());method.append(") & 0xFFFFFFFFl");break;
                case 0x03: method.append("(Core.base_ds+CPU_Regs.reg_ebx.dword()+");method.append(decode_fetchds());method.append(") & 0xFFFFFFFFl");break;
                case 0x04: method.append("(");Sib(method, 2);method.append("+");method.append(decode_fetchds());method.append(") & 0xFFFFFFFFl");break;
                case 0x05: method.append("(Core.base_ss+CPU_Regs.reg_ebp.dword()+");method.append(decode_fetchds());method.append(") & 0xFFFFFFFFl");break;
                case 0x06: method.append("(Core.base_ds+CPU_Regs.reg_esi.dword()+");method.append(decode_fetchds());method.append(") & 0xFFFFFFFFl");break;
                case 0x07: method.append("(Core.base_ds+CPU_Regs.reg_edi.dword()+");method.append(decode_fetchds());method.append(") & 0xFFFFFFFFl");break;
            }
        }
    }

    static protected long getEaa16(StringBuffer method, int rm) {
        if (rm<0x40) {
            switch (rm & 7) {
                case 0x00: method.append("Core.base_ds+((CPU_Regs.reg_ebx.word()+(short)CPU_Regs.reg_esi.word()) & 0xFFFF)");break;
                case 0x01: method.append("Core.base_ds+((CPU_Regs.reg_ebx.word()+(short)CPU_Regs.reg_edi.word()) & 0xFFFF)");break;
                case 0x02: method.append("Core.base_ss+((CPU_Regs.reg_ebp.word()+(short)CPU_Regs.reg_esi.word()) & 0xFFFF)");break;
                case 0x03: method.append("Core.base_ss+((CPU_Regs.reg_ebp.word()+(short)CPU_Regs.reg_edi.word()) & 0xFFFF)");break;
                case 0x04: method.append("Core.base_ds+(CPU_Regs.reg_esi.word())");break;
                case 0x05: method.append("Core.base_ds+(CPU_Regs.reg_edi.word())");break;
                case 0x06: method.append("Core.base_ds+");method.append(decode_fetchw());break;
                case 0x07: method.append("Core.base_ds+(CPU_Regs.reg_ebx.word())");break;
            }
        } else if (rm<0x80) {
            switch (rm & 7) {
                case 0x00: method.append("Core.base_ds+((CPU_Regs.reg_ebx.word()+(short)CPU_Regs.reg_esi.word()+");method.append(decode_fetchbs());method.append(") & 0xFFFF)");break;
                case 0x01: method.append("Core.base_ds+((CPU_Regs.reg_ebx.word()+(short)CPU_Regs.reg_edi.word()+");method.append(decode_fetchbs());method.append(") & 0xFFFF)");break;
                case 0x02: method.append("Core.base_ss+((CPU_Regs.reg_ebp.word()+(short)CPU_Regs.reg_esi.word()+");method.append(decode_fetchbs());method.append(") & 0xFFFF)");break;
                case 0x03: method.append("Core.base_ss+((CPU_Regs.reg_ebp.word()+(short)CPU_Regs.reg_edi.word()+");method.append(decode_fetchbs());method.append(") & 0xFFFF)");break;
                case 0x04: method.append("Core.base_ds+((CPU_Regs.reg_esi.word()+");method.append(decode_fetchbs());method.append(") & 0xFFFF)");break;
                case 0x05: method.append("Core.base_ds+((CPU_Regs.reg_edi.word()+");method.append(decode_fetchbs());method.append(") & 0xFFFF)");break;
                case 0x06: method.append("Core.base_ss+((CPU_Regs.reg_ebp.word()+");method.append(decode_fetchbs());method.append(") & 0xFFFF)");break;
                case 0x07: method.append("Core.base_ds+((CPU_Regs.reg_ebx.word()+");method.append(decode_fetchbs());method.append(") & 0xFFFF)");break;
            }
        } else {
            switch (rm & 7) {
                case 0x00: method.append("Core.base_ds+((CPU_Regs.reg_ebx.word()+(short)CPU_Regs.reg_esi.word()+");method.append(decode_fetchws());method.append(") & 0xFFFF)");break;
                case 0x01: method.append("Core.base_ds+((CPU_Regs.reg_ebx.word()+(short)CPU_Regs.reg_edi.word()+");method.append(decode_fetchws());method.append(") & 0xFFFF)");break;
                case 0x02: method.append("Core.base_ss+((CPU_Regs.reg_ebp.word()+(short)CPU_Regs.reg_esi.word()+");method.append(decode_fetchws());method.append(") & 0xFFFF)");break;
                case 0x03: method.append("Core.base_ss+((CPU_Regs.reg_ebp.word()+(short)CPU_Regs.reg_edi.word()+");method.append(decode_fetchws());method.append(") & 0xFFFF)");break;
                case 0x04: method.append("Core.base_ds+((CPU_Regs.reg_esi.word()+");method.append(decode_fetchws());method.append(") & 0xFFFF)");break;
                case 0x05: method.append("Core.base_ds+((CPU_Regs.reg_edi.word()+");method.append(decode_fetchws());method.append(") & 0xFFFF)");break;
                case 0x06: method.append("Core.base_ss+((CPU_Regs.reg_ebp.word()+");method.append(decode_fetchws());method.append(") & 0xFFFF)");break;
                case 0x07: method.append("Core.base_ds+((CPU_Regs.reg_ebx.word()+");method.append(decode_fetchws());method.append(") & 0xFFFF)");break;
            }
        }
        return 0;
    }

    static public void getEaa(StringBuffer method, int rm) {
        if (EA16)
            getEaa16(method, rm);
        else
            getEaa32(method, rm);
        method.append(";");
    }

    static String getrb(int rm) {
        return getearb((rm >> 3) & 7);
    }
    static String getearb(int rm) {
        switch (rm & 7) {
            case 0: return "CPU_Regs.reg_eax.low";
            case 1: return "CPU_Regs.reg_ecx.low";
            case 2: return "CPU_Regs.reg_edx.low";
            case 3: return "CPU_Regs.reg_ebx.low";
            case 4: return "CPU_Regs.reg_eax.high";
            case 5: return "CPU_Regs.reg_ecx.high";
            case 6: return "CPU_Regs.reg_edx.high";
            case 7: return "CPU_Regs.reg_ebx.high";
            default: Log.exit("Oops");return null;
        }
    }

    static String getrw(int rm) {
        return getearw((rm >> 3) & 7);
    }
    static String getearw(int rm) {
        switch (rm & 7) {
            case 0: return "CPU_Regs.reg_eax.word";
            case 1: return "CPU_Regs.reg_ecx.word";
            case 2: return "CPU_Regs.reg_edx.word";
            case 3: return "CPU_Regs.reg_ebx.word";
            case 4: return "CPU_Regs.reg_esp.word";
            case 5: return "CPU_Regs.reg_ebp.word";
            case 6: return "CPU_Regs.reg_esi.word";
            case 7: return "CPU_Regs.reg_edi.word";
            default: Log.exit("Oops");return null;
        }
    }

    static String getrd(int rm) {
        return geteard((rm >> 3) & 7);
    }
    static String geteard(int rm) {
        switch (rm & 7) {
            case 0: return "CPU_Regs.reg_eax.dword";
            case 1: return "CPU_Regs.reg_ecx.dword";
            case 2: return "CPU_Regs.reg_edx.dword";
            case 3: return "CPU_Regs.reg_ebx.dword";
            case 4: return "CPU_Regs.reg_esp.dword";
            case 5: return "CPU_Regs.reg_ebp.dword";
            case 6: return "CPU_Regs.reg_esi.dword";
            case 7: return "CPU_Regs.reg_edi.dword";
            default: Log.exit("Oops");return null;
        }
    }
    static String geteard_raw(int rm) {
        switch (rm & 7) {
            case 0: return "CPU_Regs.reg_eax";
            case 1: return "CPU_Regs.reg_ecx";
            case 2: return "CPU_Regs.reg_edx";
            case 3: return "CPU_Regs.reg_ebx";
            case 4: return "CPU_Regs.reg_esp";
            case 5: return "CPU_Regs.reg_ebp";
            case 6: return "CPU_Regs.reg_esi";
            case 7: return "CPU_Regs.reg_edi";
            default: Log.exit("Oops");return null;
        }
    }

    protected static void gded(StringBuffer method, String inst, boolean set) {
        short rm=decode_fetchb();
        if (rm >= 0xc0 ) {
            if (set) {
                method.append(getrd(rm));method.append("(");
            }
            method.append(inst);method.append("(");method.append(geteard(rm));
            method.append("(),");method.append(getrd(rm));method.append("())");
            if (set) {
                method.append(")");
            }
            method.append(";");
        } else {
            method.append("long eaa = ");getEaa(method, rm);
             if (set) {
                method.append(getrd(rm));method.append("(");
            }
            method.append(inst);method.append("(Memory.mem_readd(eaa), ");
            method.append(getrd(rm));method.append("())");
            if (set) {
                method.append(")");
            }
            method.append(";");
        }
    }

    protected static void eaxid(StringBuffer method, String inst, boolean set) {      
        if (set) {
            method.append("CPU_Regs.reg_eax.dword(");
        }
        method.append(inst);method.append("(");
        method.append(decode_fetchd());
        method.append("l, CPU_Regs.reg_eax.dword())");
        if (set) {
            method.append(")");
        }
        method.append(";");
    }
    protected static void edgd(StringBuffer method, String inst, boolean set) {
        short rm=decode_fetchb();
        if (rm >= 0xc0 ) {
            if (set) {
                method.append(geteard(rm));method.append("(");
            }
            method.append(inst);method.append("(");method.append(getrd(rm));
            method.append("(),");method.append(geteard(rm));method.append("())");
            if (set) {
                method.append(")");
            }
            method.append(";");
        }
        else {
            method.append("{long eaa = ");getEaa(method, rm);
            if (set) {
                method.append("if ((eaa & 0xFFF)<0xFFD) {");
                    method.append("int index = Paging.getDirectIndex(eaa);");
                    method.append("if (index>=0) {");
                        method.append("Memory.host_writed(index, ");method.append(inst);method.append("(");
                        method.append(getrd(rm));method.append("(),Memory.host_readd(index)));");
                    method.append("} else {");
                        method.append("Memory.mem_writed(eaa, ");method.append(inst);method.append("(");
                        method.append(getrd(rm));method.append("(),Memory.mem_readd(eaa)));");
                    method.append("}");
                method.append("} else {");
                    method.append("Memory.mem_writed(eaa, ");method.append(inst);method.append("(");
                    method.append(getrd(rm));method.append("(),Memory.mem_readd(eaa)));");
                method.append("}");
            } else {
                method.append(inst);method.append("(");
                method.append(getrd(rm));method.append("(),Memory.mem_readd(eaa));");
            }
            method.append("}");
        }
    }

    static void ebgb(StringBuffer method, int rm, String op, boolean set) {
        if (rm >= 0xc0 ) {
            if (set) {
                method.append(getearb(rm));
                method.append("(");
            }
            method.append(op);
            method.append("(");
            method.append(getrb(rm));
            method.append("(),");
            method.append(getearb(rm));
            method.append("())");
            if (set)
                method.append(")");
            method.append(";");
        }
        else {
            method.append("{long eaa = ");getEaa(method, rm);
            method.append("int addr = Paging.getDirectIndex(eaa);");
            method.append("if (addr>=0)");
            if (set)
                method.append("Memory.host_writeb(addr, ");
            method.append(op);method.append("(");method.append(getrb(rm));method.append("(),Memory.host_readb(addr))");
            if (set)
                method.append(")");
            method.append(";");
            method.append("else ");
            if (set)
                method.append("Memory.mem_writeb(eaa, ");
            method.append(op);method.append("(");method.append(getrb(rm));method.append("(),Memory.mem_readb(eaa))");
            if (set)
                method.append(")");
            method.append(";}");
        }
    }
    static void gbeb(StringBuffer method, int rm, String op, boolean set) {
        if (rm >= 0xc0 ) {
            if (set) {
                method.append(getrb(rm));
                method.append("(");
            }
            method.append(op);
            method.append("(");
            method.append(getearb(rm));
            method.append("(),");
            method.append(getrb(rm));
            method.append("())");
            if (set)
                method.append(")");
            method.append(";");
        }
        else {
            method.append("{long eaa = ");getEaa(method, rm);
            if (set) {
                method.append(getrb(rm));
                method.append("(");
            }
            method.append(op);
            method.append("(Memory.mem_readb(eaa),");
            method.append(getrb(rm));
            method.append("())");
            if (set)
                method.append(")");
            method.append(";}");
        }
    }
    static void ewgw(StringBuffer method, int rm, String op, boolean set) {
        if (rm >= 0xc0 ) {
            if (set) {
                method.append(getearw(rm));
                method.append("(");
            }
            method.append(op);
            method.append("(");
            method.append(getrw(rm));
            method.append("(),");
            method.append(getearw(rm));
            method.append("())");
            if (set)
                method.append(")");
            method.append(";");
        }
        else {
            method.append("{long eaa = ");getEaa(method, rm);
            method.append("int addr = Paging.getDirectIndex(eaa);");
            method.append("if (addr>=0)");
            if (set)
                method.append("Memory.host_writew(addr, ");
            method.append(op);method.append("(");method.append(getrw(rm));method.append("(),Memory.host_readw(addr))");
            if (set)
                method.append(")");
            method.append(";");
            method.append("else ");
            if (set)
                method.append("Memory.mem_writew(eaa, ");
            method.append(op);method.append("(");method.append(getrw(rm));method.append("(),Memory.mem_readw(eaa))");
            if (set)
                method.append(")");
            method.append(";}");
        }
    }
    static void gwew(StringBuffer method, int rm, String op, boolean set) {
        if (rm >= 0xc0 ) {
            if (set) {
                method.append(getrw(rm));
                method.append("(");
            }
            method.append(op);
            method.append("(");
            method.append(getearw(rm));
            method.append("(),");
            method.append(getrw(rm));
            method.append("())");
            if (set)
                method.append(")");
            method.append(";");
        }
        else {
            method.append("{long eaa = ");getEaa(method, rm);
            if (set) {
                method.append(getrw(rm));
                method.append("(");
            }
            method.append(op);

            method.append("(Memory.mem_readw(eaa),");
            method.append(getrw(rm));
            method.append("())");
            if (set)
                method.append(")");
            method.append(";}");
        }
    }
    static void eb(StringBuffer method, int rm, String op) {
        if (rm>=0xc0) {
            method.append(getearb(rm));
            method.append("(");
            method.append(op);
            method.append("(");
            method.append(getearb(rm));
            method.append("()));");
        } else {
            method.append("{long eaa = ");getEaa(method, rm);
            method.append("int addr = Paging.getDirectIndex(eaa);");
            method.append("if (addr>=0)");
            method.append("Memory.host_writeb(addr, ");method.append(op);method.append("(Memory.host_readb(addr)));");
            method.append("else ");
            method.append("Memory.mem_writeb(eaa, ");method.append(op);method.append("(Memory.mem_readb(eaa)));}");
        }
    }
    static void ew(StringBuffer method, int rm, String op) {
        if (rm>=0xc0) {
            method.append(getearw(rm));
            method.append("(");
            method.append(op);
            method.append("(");
            method.append(getearw(rm));
            method.append("()));");
        } else {
            method.append("{long eaa = ");getEaa(method, rm);
            method.append("if ((eaa & 0xFFF)<0xFFF) {");
            method.append("int addr = Paging.getDirectIndex(eaa);");
            method.append("if (addr>=0) {");
            method.append("Memory.host_writew(addr, ");method.append(op);method.append("(Memory.host_readw(addr)));");
            method.append("} else {");
            method.append("Memory.mem_writew(eaa, ");method.append(op);method.append("(Memory.mem_readw(eaa)));}");
            method.append("} else { ");
            method.append("Memory.mem_writew(eaa, ");method.append(op);method.append("(Memory.mem_readw(eaa)));}}");
        }
    }
    static void ed(StringBuffer method, int rm, String op) {
        if (rm>=0xc0) {
            method.append(geteard(rm));
            method.append("(");
            method.append(op);
            method.append("(");
            method.append(geteard(rm));
            method.append("()));");
        } else {
            method.append("{long eaa = ");getEaa(method, rm);
            method.append("if ((eaa & 0xFFF)<0xFFD) {");
            method.append("int addr = Paging.getDirectIndex(eaa);");
            method.append("if (addr>=0) {");
            method.append("Memory.host_writed(addr, ");method.append(op);method.append("(Memory.host_readd(addr)));");
            method.append("} else {");
            method.append("Memory.mem_writed(eaa, ");method.append(op);method.append("(Memory.mem_readd(eaa)));}");
            method.append("} else { ");
            method.append("Memory.mem_writed(eaa, ");method.append(op);method.append("(Memory.mem_readd(eaa)));}}");
        }
    }
    static void alib(StringBuffer method, String op, boolean set) {
        if (set)
            method.append("CPU_Regs.reg_eax.low(");
        method.append(op);
        method.append("((short)");
        method.append(decode_fetchb());
        method.append(",CPU_Regs.reg_eax.low())");
        if (set)
            method.append(")");
        method.append(";");
    }
    static void awiw(StringBuffer method, String op, boolean set) {
        if (set)
            method.append("CPU_Regs.reg_eax.word(");
        method.append(op);
        method.append("(");
        method.append(decode_fetchw());
        method.append(",CPU_Regs.reg_eax.word())");
        if (set)
            method.append(")");
        method.append(";");
    }
    static void EXCEPTION(StringBuffer method, int value) {
        method.append("{CPU.CPU_Cycles-=");method.append(decode.cycles);method.append(";");
        method.append("CPU_Regs.reg_eip+=");method.append(decode.op_start-decode.code_start);method.append(";");
        method.append("CPU.CPU_Exception(");
        method.append(value);
        method.append(");");
        method.append("return Constants.BR_Normal;}");
    }
    static void RUNEXCEPTION(StringBuffer method) {
        method.append("{CPU.CPU_Cycles-=");method.append(decode.cycles);method.append(";");
        method.append("CPU_Regs.reg_eip+=");method.append(decode.op_start-decode.code_start);method.append(";");
        method.append("CPU.CPU_Exception(CPU.cpu.exception.which,CPU.cpu.exception.error);");
        method.append("return Constants.BR_Normal;}");
    }
    static void incw(StringBuffer method, int rm) {
        method.append("Flags.LoadCF();");
        method.append("Flags.lflags.var1 = ");method.append(getearw(rm));method.append("();");
        method.append("Flags.lflags.res = (Flags.lflags.var1+1) & 0xFFFF;");
        method.append(getearw(rm));method.append("((int)Flags.lflags.res);");
        method.append("Flags.lflags.type=Flags.t_INCw;");
    }
    static void decw(StringBuffer method, int rm) {
        method.append("Flags.LoadCF();");
        method.append("Flags.lflags.var1 = ");method.append(getearw(rm));method.append("();");
        method.append("Flags.lflags.res = (Flags.lflags.var1-1) & 0xFFFF;");
        method.append(getearw(rm));method.append("((int)Flags.lflags.res);");
        method.append("Flags.lflags.type=Flags.t_DECw;");
    }
    static void incd(StringBuffer method, int rm) {
        method.append("Flags.LoadCF();");
        method.append("Flags.lflags.var1 = ");method.append(geteard(rm));method.append("();");
        method.append("Flags.lflags.res = (Flags.lflags.var1+1) & 0xFFFFFFFFl;");
        method.append(geteard(rm));method.append("(Flags.lflags.res);");
        method.append("Flags.lflags.type=Flags.t_INCd;");
    }
    static void decd(StringBuffer method, int rm) {
        method.append("Flags.LoadCF();");
        method.append("Flags.lflags.var1 = ");method.append(geteard(rm));method.append("();");
        method.append("Flags.lflags.res = (Flags.lflags.var1-1) & 0xFFFFFFFFl;");
        method.append(geteard(rm));method.append("(Flags.lflags.res);");
        method.append("Flags.lflags.type=Flags.t_DECd;");
    }
    static void exchangew(StringBuffer method, String op1, String op2) {
        method.append("int temp=CPU_Regs.reg_");
        method.append(op1);
        method.append(".word();CPU_Regs.reg_");
        method.append(op1);
        method.append(".word(CPU_Regs.reg_");
        method.append(op2);
        method.append(".word());CPU_Regs.reg_");
        method.append(op2);
        method.append(".word(temp);");
    }

    static public void ROLB(StringBuffer method, short op2, String load, String save) {
        if ((op2&0x7)==0) {
            if ((op2&0x18)!=0) {
                method.append("Flags.FillFlagsNoCFOF();");
                method.append("CPU_Regs.SETFLAGBIT(CPU_Regs.CF,(");method.append(load);method.append(" & 1)!=0);");
                method.append("CPU_Regs.SETFLAGBIT(CPU_Regs.OF,((");method.append(load);method.append(" & 1) ^ (");method.append(load);method.append(" >> 7))!=0);");
            }
            return;
        }
        method.append("Flags.FillFlagsNoCFOF();");
        method.append("Flags.lflags.var1 = ");method.append(load);method.append(";");
        method.append("Flags.lflags.var2 = ");method.append(op2&0x07);method.append(";");
        method.append("Flags.lflags.res = ((Flags.lflags.var1 << ");method.append(op2&0x07);method.append(") | (Flags.lflags.var1 >> (8-");method.append(op2&0x07);method.append("))) & 0xFF;");
        method.append(save);
        method.append("CPU_Regs.SETFLAGBIT(CPU_Regs.CF,(Flags.lflags.res & 1)!=0);");
        method.append("CPU_Regs.SETFLAGBIT(CPU_Regs.OF,((Flags.lflags.res & 1) ^ (Flags.lflags.res >> 7))!=0);");
    }

    static public void ROLB(StringBuffer method, String op2, String load, String save) {
        method.append("{int op2 = ");method.append(op2);method.append(";");
        method.append("if ((op2 & 0x7)==0) {");
            method.append("if ((op2 & 0x18)!=0) {");
                method.append("Flags.FillFlagsNoCFOF();");
                method.append("CPU_Regs.SETFLAGBIT(CPU_Regs.CF,(");method.append(load);method.append(" & 1)!=0);");
                method.append("CPU_Regs.SETFLAGBIT(CPU_Regs.OF,((");method.append(load);method.append(" & 1) ^ (");method.append(load);method.append(" >> 7))!=0);");
            method.append("}");
        method.append("} else {");
        method.append("op2 = ");method.append(op2);method.append(" & 0x7;");
        method.append("Flags.FillFlagsNoCFOF();");
        method.append("Flags.lflags.var1 = ");method.append(load);method.append(";");
        method.append("Flags.lflags.var2 = op2;");
        method.append("Flags.lflags.res = ((Flags.lflags.var1 << op2) | (Flags.lflags.var1 >> (8-op2))) & 0xFF;");
        method.append(save);
        method.append("CPU_Regs.SETFLAGBIT(CPU_Regs.CF,(Flags.lflags.res & 1)!=0);");
        method.append("CPU_Regs.SETFLAGBIT(CPU_Regs.OF,((Flags.lflags.res & 1) ^ (Flags.lflags.res >> 7))!=0);");
        method.append("}}");
    }

    static public void ROLW(StringBuffer method, int op2, String load, String save) {
        if ((op2&0xf)==0) {
            if ((op2&0x10)!=0) {
                method.append("Flags.FillFlagsNoCFOF();");
                method.append("CPU_Regs.SETFLAGBIT(CPU_Regs.CF,(");method.append(load);method.append(" & 1)!=0);");
                method.append("CPU_Regs.SETFLAGBIT(CPU_Regs.OF,((");method.append(load);method.append(" & 1) ^ (");method.append(load);method.append(" >> 15))!=0);");
            }
            return;
        }
        method.append("Flags.FillFlagsNoCFOF();");
        method.append("Flags.lflags.var1 = ");method.append(load);method.append(";");
        method.append("Flags.lflags.var2 = ");method.append(op2&0xf);method.append(";");
        method.append("Flags.lflags.res = ((Flags.lflags.var1 << ");method.append(op2&0xf);method.append(") | (Flags.lflags.var1 >> ");method.append(16-(op2&0xf));method.append(")) & 0xFFFF;");
        method.append(save);
        method.append("CPU_Regs.SETFLAGBIT(CPU_Regs.CF,(Flags.lflags.res & 1)!=0);");
        method.append("CPU_Regs.SETFLAGBIT(CPU_Regs.OF,((Flags.lflags.res & 1) ^ (Flags.lflags.res >> 15))!=0);");
    }

    static public void ROLW(StringBuffer method, String op2, String load, String save) {
        method.append("{int op2 = ");method.append(op2);method.append(";");
        method.append("if ((op2&0xf)==0) {");
            method.append("if ((op2&0x10)!=0) {");
                method.append("Flags.FillFlagsNoCFOF();");
                method.append("CPU_Regs.SETFLAGBIT(CPU_Regs.CF,(");method.append(load);method.append(" & 1)!=0);");
                method.append("CPU_Regs.SETFLAGBIT(CPU_Regs.OF,((");method.append(load);method.append(" & 1) ^ (");method.append(load);method.append(" >> 15))!=0);");
            method.append("}");
        method.append("} else {");
        method.append("op2 = op2 & 0x0f;");
        method.append("Flags.FillFlagsNoCFOF();");
        method.append("Flags.lflags.var1 = ");method.append(load);method.append(";");
        method.append("Flags.lflags.var2 = op2;");
        method.append("Flags.lflags.res = ((Flags.lflags.var1 << op2) | (Flags.lflags.var1 >> (16-op2))) & 0xFFFF;");
        method.append(save);
        method.append("CPU_Regs.SETFLAGBIT(CPU_Regs.CF,(Flags.lflags.res & 1)!=0);");
        method.append("CPU_Regs.SETFLAGBIT(CPU_Regs.OF,((Flags.lflags.res & 1) ^ (Flags.lflags.res >> 15))!=0);");
        method.append("}}");
    }

    static public void ROLD(StringBuffer method, int op2, String load, String save) {
        method.append("Flags.FillFlagsNoCFOF();");
        method.append("Flags.lflags.var1 = ");method.append(load);method.append(";");
        method.append("Flags.lflags.var2 = ");method.append(op2);method.append(";");
        method.append("Flags.lflags.res=((Flags.lflags.var1 << ");method.append(op2);method.append(") | (Flags.lflags.var1 >> ");method.append(32-op2);method.append(")) & 0xFFFFFFFFl;");
        method.append("CPU_Regs.SETFLAGBIT(CPU_Regs.CF,(Flags.lflags.res & 1)!=0);");
        method.append("CPU_Regs.SETFLAGBIT(CPU_Regs.OF,((Flags.lflags.res & 1) ^ (Flags.lflags.res >> 31))!=0);");
        method.append(save);
    }

    static public void ROLD(StringBuffer method, String op2, String load, String save) {
        method.append("Flags.FillFlagsNoCFOF();");
        method.append("Flags.lflags.var1 = ");method.append(load);method.append(";");
        method.append("Flags.lflags.var2 = ");method.append(op2);method.append(";");
        method.append("Flags.lflags.res=((Flags.lflags.var1 << (int)Flags.lflags.var2) | (Flags.lflags.var1 >> (32-(int)Flags.lflags.var2))) & 0xFFFFFFFFl;");
        method.append("CPU_Regs.SETFLAGBIT(CPU_Regs.CF,(Flags.lflags.res & 1)!=0);");
        method.append("CPU_Regs.SETFLAGBIT(CPU_Regs.OF,((Flags.lflags.res & 1) ^ (Flags.lflags.res >> 31))!=0);");
        method.append(save);
    }

    static public void RORB(StringBuffer method, short op2, String load, String save) {
        if ((op2&0x7)==0) {
            if ((op2&0x18)!=0) {
                method.append("Flags.FillFlagsNoCFOF();");
                method.append("CPU_Regs.SETFLAGBIT(CPU_Regs.CF,(");method.append(load);method.append(">>7)!=0);");
                method.append("CPU_Regs.SETFLAGBIT(CPU_Regs.OF,((");method.append(load);method.append(">>7) ^ ((");method.append(load);method.append(">>6) & 1))!=0);");
            }
        }
        method.append("Flags.FillFlagsNoCFOF();");
        method.append("Flags.lflags.var1 = ");method.append(load);method.append(";");
        method.append("Flags.lflags.var2 = ");method.append(op2&0x07);method.append(";");
        method.append("Flags.lflags.res = ((Flags.lflags.var1 >> ");method.append(op2&0x07);method.append(") | (Flags.lflags.var1 << ");method.append(8-(op2&0x07));method.append(")) & 0xFF;");
        method.append(save);
        method.append("CPU_Regs.SETFLAGBIT(CPU_Regs.CF,(Flags.lflags.res & 0x80)!=0);");
        method.append("CPU_Regs.SETFLAGBIT(CPU_Regs.OF,((Flags.lflags.res ^ (Flags.lflags.res<<1)) & 0x80)!=0);");
    }

    static public void RORB(StringBuffer method, String op2, String load, String save) {
        method.append("{int op2 = ");method.append(op2);method.append(";");
        method.append("if ((op2&0x7)==0) {");
            method.append("if ((op2&0x18)!=0) {");
                method.append("Flags.FillFlagsNoCFOF();");
                method.append("CPU_Regs.SETFLAGBIT(CPU_Regs.CF,(");method.append(load);method.append(">>7)!=0);");
                method.append("CPU_Regs.SETFLAGBIT(CPU_Regs.OF,((");method.append(load);method.append(">>7) ^ ((");method.append(load);method.append(">>6) & 1))!=0);");
            method.append("}");
        method.append("} else {");
        method.append("op2 = ");method.append(op2);method.append(" & 0x7;");
        method.append("Flags.FillFlagsNoCFOF();");
        method.append("Flags.lflags.var1 = ");method.append(load);method.append(";");
        method.append("Flags.lflags.var2 = op2;");
        method.append("Flags.lflags.res = ((Flags.lflags.var1 >> op2) | (Flags.lflags.var1 << (8-op2))) & 0xFF;");
        method.append(save);
        method.append("CPU_Regs.SETFLAGBIT(CPU_Regs.CF,(Flags.lflags.res & 0x80)!=0);");
        method.append("CPU_Regs.SETFLAGBIT(CPU_Regs.OF,((Flags.lflags.res ^ (Flags.lflags.res<<1)) & 0x80)!=0);");
        method.append("}}");
    }

    static public void RORW(StringBuffer method, int op2, String load, String save) {
        if ((op2&0xf)==0) {
            if ((op2&0x10)!=0) {
                method.append("Flags.FillFlagsNoCFOF();");
                method.append("CPU_Regs.SETFLAGBIT(CPU_Regs.CF,(");method.append(load);method.append(">>15)!=0);");
                method.append("CPU_Regs.SETFLAGBIT(CPU_Regs.OF,((");method.append(load);method.append(">>15) ^ ((");method.append(load);method.append(">>14) & 1))!=0);");
            }
        }
        method.append("Flags.FillFlagsNoCFOF();");
        method.append("Flags.lflags.var1 = ");method.append(load);method.append(";");
        method.append("Flags.lflags.var2 = ");method.append(op2&0x0f);method.append(";");
        method.append("Flags.lflags.res = ((Flags.lflags.var1 >> ");method.append(op2&0x0f);method.append(") | (Flags.lflags.var1 << ");method.append(16-(op2&0x0f));method.append(")) & 0xFFFF;");
        method.append(save);
        method.append("CPU_Regs.SETFLAGBIT(CPU_Regs.CF,(Flags.lflags.res & 0x8000)!=0);");
        method.append("CPU_Regs.SETFLAGBIT(CPU_Regs.OF,((Flags.lflags.res ^ (Flags.lflags.res<<1)) & 0x8000)!=0);");
    }

    static public void RORW(StringBuffer method, String op2, String load, String save) {
        method.append("{int op2 = ");method.append(op2);method.append(";");
        method.append("if ((op2&0xf)==0) {");
            method.append("if ((op2&0x10)!=0) {");
                method.append("Flags.FillFlagsNoCFOF();");
                method.append("CPU_Regs.SETFLAGBIT(CPU_Regs.CF,(");method.append(load);method.append(">>15)!=0);");
                method.append("CPU_Regs.SETFLAGBIT(CPU_Regs.OF,((");method.append(load);method.append(">>15) ^ ((");method.append(load);method.append(">>14) & 1))!=0);");
            method.append("}");
        method.append("} else {");
        method.append("op2 = op2 & 0x0f;");
        method.append("Flags.FillFlagsNoCFOF();");
        method.append("Flags.lflags.var1 = ");method.append(load);method.append(";");
        method.append("Flags.lflags.var2 = op2;");
        method.append("Flags.lflags.res = ((Flags.lflags.var1 >> op2) | (Flags.lflags.var1 << (16-op2))) & 0xFFFF;");
        method.append(save);
        method.append("CPU_Regs.SETFLAGBIT(CPU_Regs.CF,(Flags.lflags.res & 0x8000)!=0);");
        method.append("CPU_Regs.SETFLAGBIT(CPU_Regs.OF,((Flags.lflags.res ^ (Flags.lflags.res<<1)) & 0x8000)!=0);");
        method.append("}");
    }
    static public void RORD(StringBuffer method, int op2, String load, String save) {
        method.append("Flags.FillFlagsNoCFOF();");
        method.append("Flags.lflags.var1 = ");method.append(load);method.append(";");
        method.append("Flags.lflags.var2 = ");method.append(op2);method.append(";");
        method.append("Flags.lflags.res=((Flags.lflags.var1 >> ");method.append(op2);method.append(") | (Flags.lflags.var1 << ");method.append(32-op2);method.append(")) & 0xFFFFFFFFl;");
        method.append("CPU_Regs.SETFLAGBIT(CPU_Regs.CF,(Flags.lflags.res & 0x80000000l)!=0);");
        method.append("CPU_Regs.SETFLAGBIT(CPU_Regs.OF,((Flags.lflags.res ^ (Flags.lflags.res<<1)) & 0x80000000l)!=0);");
        method.append(save);
    }
    static public void RORD(StringBuffer method, String op2, String load, String save) {
        method.append("Flags.FillFlagsNoCFOF();");
        method.append("Flags.lflags.var1 = ");method.append(load);method.append(";");
        method.append("Flags.lflags.var2 = ");method.append(op2);method.append(";");
        method.append("Flags.lflags.res=((Flags.lflags.var1 >> (int)Flags.lflags.var2) | (Flags.lflags.var1 << (32-(int)Flags.lflags.var2))) & 0xFFFFFFFFl;");
        method.append("CPU_Regs.SETFLAGBIT(CPU_Regs.CF,(Flags.lflags.res & 0x80000000l)!=0);");
        method.append("CPU_Regs.SETFLAGBIT(CPU_Regs.OF,((Flags.lflags.res ^ (Flags.lflags.res<<1)) & 0x80000000l)!=0);");
        method.append(save);
    }

    static public void RCLB(StringBuffer method, short op2, String load, String save) {
        if ((op2%9)==0) return;
        method.append("int cf=Flags.FillFlags()&0x1;");
        method.append("Flags.lflags.var1 = ");method.append(load);method.append(";");
        op2=(short)(op2%9);
        method.append("Flags.lflags.var2 = ");method.append(op2);method.append(";");
        method.append("Flags.lflags.res = ((Flags.lflags.var1 << ");method.append(op2);method.append(") | (cf << ");method.append(op2-1);method.append(") | (Flags.lflags.var1 >> ");method.append(9-op2);method.append(")) & 0xFF;");
        method.append(save);
        method.append("CPU_Regs.SETFLAGBIT(CPU_Regs.CF,(((Flags.lflags.var1 >> ");method.append(8-op2);method.append(") & 1))!=0);");
        method.append("CPU_Regs.SETFLAGBIT(CPU_Regs.OF,((CPU_Regs.flags & 1) ^ (Flags.lflags.res >> 7))!=0);");
    }

    static public void RCLB(StringBuffer method, String op2, String load, String save) {
        method.append("{int op2 = ");method.append(op2);method.append(" % 9;");
        method.append("if (op2!=0) {");
        method.append("int cf=Flags.FillFlags()&0x1;");
        method.append("Flags.lflags.var1 = ");method.append(load);method.append(";");
        method.append("Flags.lflags.var2 = ");method.append(op2);method.append(";");
        method.append("Flags.lflags.res = ((Flags.lflags.var1 << ");method.append(op2);method.append(") | (cf << (op2-1)) | (Flags.lflags.var1 >> (9-op2))) & 0xFF;");
        method.append(save);
        method.append("CPU_Regs.SETFLAGBIT(CPU_Regs.CF,(((Flags.lflags.var1 >> (8-op2)) & 1))!=0);");
        method.append("CPU_Regs.SETFLAGBIT(CPU_Regs.OF,((CPU_Regs.flags & 1) ^ (Flags.lflags.res >> 7))!=0);");
        method.append("}}");
    }

    static public void RCLW(StringBuffer method, int op2, String load, String save) {
        if ((op2%17)==0) return;
        method.append("int cf=Flags.FillFlags()&0x1;");
        method.append("Flags.lflags.var1 = ");method.append(load);method.append(";");
        op2=op2%17;
        method.append("Flags.lflags.var2 = ");method.append(op2);method.append(";");
        method.append("Flags.lflags.res = ((Flags.lflags.var1 << ");method.append(op2);method.append(") | (cf << ");method.append(op2-1);method.append(") | (Flags.lflags.var1 >> ");method.append(17-op2);method.append(")) & 0xFFFF;");
        method.append(save);
        method.append("CPU_Regs.SETFLAGBIT(CPU_Regs.CF,(((Flags.lflags.var1 >> ");method.append(16-op2);method.append(") & 1))!=0);");
        method.append("CPU_Regs.SETFLAGBIT(CPU_Regs.OF,((CPU_Regs.flags & 1) ^ (Flags.lflags.res >> 15))!=0);");
    }

    static public void RCLW(StringBuffer method, String op2, String load, String save) {
        method.append("{int op2 = ");method.append(op2);method.append(" % 17;");
        method.append("if (op2!=0) {");
        method.append("int cf=Flags.FillFlags()&0x1;");
        method.append("Flags.lflags.var1 = ");method.append(load);method.append(";");
        method.append("Flags.lflags.var2 = ");method.append(op2);method.append(";");
        method.append("Flags.lflags.res = ((Flags.lflags.var1 << ");method.append(op2);method.append(") | (cf << (op2-1)) | (Flags.lflags.var1 >> (17-op2))) & 0xFFFF;");
        method.append(save);
        method.append("CPU_Regs.SETFLAGBIT(CPU_Regs.CF,(((Flags.lflags.var1 >> (16-op2)) & 1))!=0);");
        method.append("CPU_Regs.SETFLAGBIT(CPU_Regs.OF,((CPU_Regs.flags & 1) ^ (Flags.lflags.res >> 15))!=0);");
        method.append("}");
    }

    static public void RCLD(StringBuffer method, String op2, String load, String save) {
        method.append("int cf=Flags.FillFlags()&0x1;");
        method.append("Flags.lflags.var1 = ");method.append(load);method.append(";");
        method.append("Flags.lflags.var2 = ");method.append(op2);method.append(";");
        method.append("if (Flags.lflags.var2 == 1)");
        method.append("Flags.lflags.res = ((Flags.lflags.var1 << 1) | cf) & 0xFFFFFFFFl;");
        method.append(" else ");
        method.append("Flags.lflags.res = ((Flags.lflags.var1 << (int)Flags.lflags.var2) |(cf << ((int)Flags.lflags.var2-1)) | (Flags.lflags.var1 >> (33-(int)Flags.lflags.var2))) & 0xFFFFFFFFl;");
        method.append("CPU_Regs.SETFLAGBIT(CPU_Regs.CF,(((Flags.lflags.var1 >> (32-(int)Flags.lflags.var2)) & 1))!=0);");
        method.append("CPU_Regs.SETFLAGBIT(CPU_Regs.OF,((CPU_Regs.flags & 1) ^ (Flags.lflags.res >> 31))!=0);");
        method.append(save);
    }

    static public void RCLD(StringBuffer method, int op2, String load, String save) {
        method.append("int cf=Flags.FillFlags()&0x1;");
        method.append("Flags.lflags.var1 = ");method.append(load);method.append(";");
        method.append("Flags.lflags.var2 = ");method.append(op2);method.append(";");
        method.append("if (Flags.lflags.var2 == 1)");
        method.append("Flags.lflags.res = ((Flags.lflags.var1 << 1) | cf) & 0xFFFFFFFFl;");
        method.append(" else ");
        method.append("Flags.lflags.res = ((Flags.lflags.var1 << ");method.append(op2);method.append(") |(cf << ");method.append(op2-1);method.append(") | (Flags.lflags.var1 >> ");method.append(33-op2);method.append(")) & 0xFFFFFFFFl;");
        method.append("CPU_Regs.SETFLAGBIT(CPU_Regs.CF,(((Flags.lflags.var1 >> ");method.append(32-op2);method.append(") & 1))!=0);");
        method.append("CPU_Regs.SETFLAGBIT(CPU_Regs.OF,((CPU_Regs.flags & 1) ^ (Flags.lflags.res >> 31))!=0);");
        method.append(save);
    }

    static public void RCRB(StringBuffer method, short op2, String load, String save) {
        if ((op2%9)==0) return;
        method.append("int cf=Flags.FillFlags()&0x1;");
        method.append("Flags.lflags.var1 = ");method.append(load);method.append(";");
        op2 = (short)(op2 % 9);
        method.append("Flags.lflags.var2 = ");method.append(op2);method.append(";");
        method.append("Flags.lflags.res = ((Flags.lflags.var1 >> ");method.append(op2);method.append(") | (cf << ");method.append(8-op2);method.append(") | (Flags.lflags.var1 << ");method.append(9-op2);method.append(")) & 0xFF;");
        method.append(save);
        method.append("CPU_Regs.SETFLAGBIT(CPU_Regs.CF,(((Flags.lflags.var1 >> ");method.append(op2-1);method.append(") & 1))!=0);");
        method.append("CPU_Regs.SETFLAGBIT(CPU_Regs.OF,((Flags.lflags.res ^ (Flags.lflags.res << 1)) & 0x80)!=0);");
    }

    static public void RCRB(StringBuffer method, String op2, String load, String save) {
        method.append("{int op2 = ");method.append(op2);method.append(" % 9;");
        method.append("if (op2!=0) {");
        method.append("int cf=Flags.FillFlags()&0x1;");
        method.append("Flags.lflags.var1 = ");method.append(load);method.append(";");
        method.append("Flags.lflags.var2 = ");method.append(op2);method.append(";");
        method.append("Flags.lflags.res = ((Flags.lflags.var1 >> ");method.append(op2);method.append(") | (cf << (8-op2)) | (Flags.lflags.var1 << (9-op2))) & 0xFF;");
        method.append(save);
        method.append("CPU_Regs.SETFLAGBIT(CPU_Regs.CF,(((Flags.lflags.var1 >> (op2-1)) & 1))!=0);");
        method.append("CPU_Regs.SETFLAGBIT(CPU_Regs.OF,((Flags.lflags.res ^ (Flags.lflags.res << 1)) & 0x80)!=0);");
        method.append("}}");
    }

    static public void RCRW(StringBuffer method, int op2, String load, String save) {
        if ((op2%17)==0) return;
        method.append("int cf=Flags.FillFlags()&0x1;");
        method.append("Flags.lflags.var1 = ");method.append(load);method.append(";");
        op2=op2%17;
        method.append("Flags.lflags.var2 = ");method.append(op2);method.append(";");
        method.append("Flags.lflags.res = ((Flags.lflags.var1 >> ");method.append(op2);method.append(") | (cf << ");method.append(16-op2);method.append(") | (Flags.lflags.var1 << ");method.append(17-op2);method.append(")) & 0xFFFF;");
        method.append(save);
        method.append("CPU_Regs.SETFLAGBIT(CPU_Regs.CF,(((Flags.lflags.var1 >> ");method.append(op2-1);method.append(") & 1))!=0);");
        method.append("CPU_Regs.SETFLAGBIT(CPU_Regs.OF,((Flags.lflags.res ^ (Flags.lflags.res << 1)) & 0x8000)!=0);");
    }

    static public void RCRW(StringBuffer method, String op2, String load, String save) {
        method.append("{int op2 = ");method.append(op2);method.append(" % 17;");
        method.append("if (op2!=0) {");
        method.append("int cf=Flags.FillFlags()&0x1;");
        method.append("Flags.lflags.var1 = ");method.append(load);method.append(";");
        method.append("Flags.lflags.var2 = ");method.append(op2);method.append(";");
        method.append("Flags.lflags.res = ((Flags.lflags.var1 >> ");method.append(op2);method.append(") | (cf << (16-op2)) | (Flags.lflags.var1 << (17-op2))) & 0xFFFF;");
        method.append(save);
        method.append("CPU_Regs.SETFLAGBIT(CPU_Regs.CF,(((Flags.lflags.var1 >> (op2-1)) & 1))!=0);");
        method.append("CPU_Regs.SETFLAGBIT(CPU_Regs.OF,((Flags.lflags.res ^ (Flags.lflags.res << 1)) & 0x8000)!=0);");
        method.append("}");
    }

    static public void RCRD(StringBuffer method, int op2, String load, String save) {
        method.append("int cf=Flags.FillFlags()&0x1;");
        method.append("Flags.lflags.var1 = ");method.append(load);method.append(";");
        method.append("Flags.lflags.var2 = ");method.append(op2);method.append(";");
        method.append("if (Flags.lflags.var1 == 1)");
        method.append("Flags.lflags.res = (Flags.lflags.var1 >> 1 | cf << 31) & 0xFFFFFFFFl;");
        method.append(" else ");
        method.append("Flags.lflags.res = ((Flags.lflags.var1 >> ");method.append(op2);method.append(") | (cf << ");method.append(32-op2);method.append(") | (Flags.lflags.var1 << (33-");method.append(op2);method.append("))) & 0xFFFFFFFFl;");
        method.append("CPU_Regs.SETFLAGBIT(CPU_Regs.CF,((Flags.lflags.var1 >> ");method.append(op2-1);method.append(") & 1)!=0);");
        method.append("CPU_Regs.SETFLAGBIT(CPU_Regs.OF,((Flags.lflags.res ^ (Flags.lflags.res<<1)) & 0x80000000)!=0);");
        method.append(save);
    }

    static public void RCRD(StringBuffer method, String op2, String load, String save) {
        method.append("int cf=Flags.FillFlags()&0x1;");
        method.append("Flags.lflags.var1 = ");method.append(load);method.append(";");
        method.append("Flags.lflags.var2 = ");method.append(op2);method.append(";");
        method.append("if (Flags.lflags.var1 == 1)");
        method.append("Flags.lflags.res = (Flags.lflags.var1 >> 1 | cf << 31) & 0xFFFFFFFFl;");
        method.append(" else ");
        method.append("Flags.lflags.res = ((Flags.lflags.var1 >> (int)Flags.lflags.var2) | (cf << (32-(int)Flags.lflags.var2)) | (Flags.lflags.var1 << (int)(33-Flags.lflags.var2))) & 0xFFFFFFFFl;");
        method.append("CPU_Regs.SETFLAGBIT(CPU_Regs.CF,((Flags.lflags.var1 >> ((int)Flags.lflags.var2-1)) & 1)!=0);");
        method.append("CPU_Regs.SETFLAGBIT(CPU_Regs.OF,((Flags.lflags.res ^ (Flags.lflags.res<<1)) & 0x80000000)!=0);");
        method.append(save);
    }

    static public void SHLB(StringBuffer method, short op2, String load, String save) {
        if (op2==0) return;
        method.append("Flags.lflags.var1 = ");method.append(load);method.append(";");
        method.append("Flags.lflags.var2 = ");method.append(op2);method.append(";");
        method.append("Flags.lflags.res = (Flags.lflags.var1 << ");method.append(op2);method.append(") & 0xFF;");
        method.append(save);
        method.append("Flags.lflags.type=Flags.t_SHLb;");
    }

    static public void SHLB(StringBuffer method, String op2, String load, String save) {
        method.append("{int op2 = ");method.append(op2);method.append(";");
        method.append("if (op2!=0) {");
        method.append("Flags.lflags.var1 = ");method.append(load);method.append(";");
        method.append("Flags.lflags.var2 = ");method.append(op2);method.append(";");
        method.append("Flags.lflags.res = (Flags.lflags.var1 << ");method.append(op2);method.append(") & 0xFF;");
        method.append(save);
        method.append("Flags.lflags.type=Flags.t_SHLb;");
        method.append("}}");
    }

    static public void SHLW(StringBuffer method, int op2, String load, String save) {
        if (op2==0) return;
        method.append("Flags.lflags.var1 = ");method.append(load);method.append(";");
        method.append("Flags.lflags.var2 = ");method.append(op2);method.append(";");
        method.append("Flags.lflags.res = (Flags.lflags.var1 << ");method.append(op2);method.append(") & 0xFFFF;");
        method.append(save);
        method.append("Flags.lflags.type=Flags.t_SHLw;");
    }

    static public void SHLW(StringBuffer method, String op2, String load, String save) {
        method.append("{int op2 = ");method.append(op2);method.append(";");
        method.append("if (op2!=0) {");
        method.append("Flags.lflags.var1 = ");method.append(load);method.append(";");
        method.append("Flags.lflags.var2 = ");method.append(op2);method.append(";");
        method.append("Flags.lflags.res = (Flags.lflags.var1 << op2) & 0xFFFF;");
        method.append(save);
        method.append("Flags.lflags.type=Flags.t_SHLw;");
        method.append("}}");
    }

    static public void SHLD(StringBuffer method, String op2, String load, String save) {
        method.append("Flags.lflags.var1 = ");method.append(load);method.append(";");
        method.append("Flags.lflags.var2 = ");method.append(op2);method.append(";");
        method.append("Flags.lflags.res = (Flags.lflags.var1 << (int)Flags.lflags.var2) & 0xFFFFFFFFl;");
        method.append("Flags.lflags.type=Flags.t_SHLd;");
        method.append(save);
    }
    static public void SHLD(StringBuffer method, int op2, String load, String save) {
        method.append("Flags.lflags.var1 = ");method.append(load);method.append(";");
        method.append("Flags.lflags.var2 = ");method.append(op2);method.append(";");
        method.append("Flags.lflags.res = (Flags.lflags.var1 << ");method.append(op2);method.append(") & 0xFFFFFFFFl;");
        method.append("Flags.lflags.type=Flags.t_SHLd;");
        method.append(save);
    }
    static public void SHRB(StringBuffer method, short op2, String load, String save) {
        if (op2==0) return;
        method.append("Flags.lflags.var1 = ");method.append(load);method.append(";");
        method.append("Flags.lflags.var2 = ");method.append(op2);method.append(";");
        method.append("Flags.lflags.res = (Flags.lflags.var1 >> ");method.append(op2);method.append(") & 0xFF;");
        method.append(save);
        method.append("Flags.lflags.type=Flags.t_SHRb;");
    }

    static public void SHRB(StringBuffer method, String op2, String load, String save) {
        method.append("{int op2 = ");method.append(op2);method.append(";");
        method.append("if (op2!=0) {");
        method.append("Flags.lflags.var1 = ");method.append(load);method.append(";");
        method.append("Flags.lflags.var2 = ");method.append(op2);method.append(";");
        method.append("Flags.lflags.res = (Flags.lflags.var1 >> ");method.append(op2);method.append(") & 0xFF;");
        method.append(save);method.append("(Flags.lflags.res);");
        method.append("Flags.lflags.type=Flags.t_SHRb;");
        method.append("}}");
    }

    static public void SHRW(StringBuffer method, int op2, String load, String save) {
        if (op2==0) return;
        method.append("Flags.lflags.var1 = ");method.append(load);method.append(";");
        method.append("Flags.lflags.var2 = ");method.append(op2);method.append(";");
        method.append("Flags.lflags.res = (Flags.lflags.var1 >> ");method.append(op2);method.append(") & 0xFFFF;");
        method.append(save);
        method.append("Flags.lflags.type=Flags.t_SHRw;");
    }

    static public void SHRW(StringBuffer method, String op2, String load, String save) {
        method.append("{int op2 = ");method.append(op2);method.append(";");
        method.append("if (op2!=0) {");
        method.append("Flags.lflags.var1 = ");method.append(load);method.append(";");
        method.append("Flags.lflags.var2 = ");method.append(op2);method.append(";");
        method.append("Flags.lflags.res = (Flags.lflags.var1 >> ");method.append(op2);method.append(") & 0xFFFF;");
        method.append(save);
        method.append("Flags.lflags.type=Flags.t_SHRw;");
        method.append("}}");
    }

    static public void SHRD(StringBuffer method, String op2, String load, String save) {
        method.append("Flags.lflags.var1 = ");method.append(load);method.append(";");
        method.append("Flags.lflags.var2 = ");method.append(op2);method.append(";");
        method.append("Flags.lflags.res = (Flags.lflags.var1 >> (int)Flags.lflags.var2);");
        method.append(save);
        method.append("Flags.lflags.type=Flags.t_SHRd;");
    }
    static public void SHRD(StringBuffer method, int op2, String load, String save) {
        method.append("Flags.lflags.var1 = ");method.append(load);method.append(";");
        method.append("Flags.lflags.var2 = ");method.append(op2);method.append(";");
        method.append("Flags.lflags.res = (Flags.lflags.var1 >> ");method.append(op2);method.append(");");
        method.append(save);
        method.append("Flags.lflags.type=Flags.t_SHRd;");
    }
    static public void SARB(StringBuffer method, short op2, String load, String save) {
        if (op2==0) return;
        method.append("Flags.lflags.var1 = ");method.append(load);method.append(";");
        if (op2>8) op2 = 8;
        method.append("Flags.lflags.var2 = ");method.append(op2);method.append(";");
        method.append("if ((Flags.lflags.var1 & 0x80)!=0) {");
        method.append("Flags.lflags.res = ((Flags.lflags.var1 >> ");method.append(op2);method.append(") | (0xFF << ");method.append(8-op2);method.append("))  & 0xFF;");
        method.append("} else {");
        method.append("Flags.lflags.res = (Flags.lflags.var1 >> ");method.append(op2);method.append(") & 0xFF;");
        method.append("}");
        method.append(save);
        method.append("Flags.lflags.type=Flags.t_SARb;");
    }

    static public void SARB(StringBuffer method, String op2, String load, String save) {
        method.append("{int op2 = ");method.append(op2);method.append(";");
        method.append("if (op2!=0) {");
        method.append("Flags.lflags.var1 = ");method.append(load);method.append(";");
        method.append("if (op2>8) op2=8;");
        method.append("Flags.lflags.var2 = ");method.append(op2);method.append(";");
        method.append("if ((Flags.lflags.var1 & 0x80)!=0) {");
        method.append("Flags.lflags.res = ((Flags.lflags.var1 >> op2) | (0xFF << (8-op2)))  & 0xFF;");
        method.append("} else {");
        method.append("Flags.lflags.res = (Flags.lflags.var1 >> op2) & 0xFF;");
        method.append("}");
        method.append(save);method.append("(Flags.lflags.res);");
        method.append("Flags.lflags.type=Flags.t_SARb;");
        method.append("}}");
    }

    static public void SARW(StringBuffer method, int op2, String load, String save) {
        if (op2==0) return;
        method.append("Flags.lflags.var1 = ");method.append(load);method.append(";");
        if (op2>16) op2 = 16;
        method.append("Flags.lflags.var2 = ");method.append(op2);method.append(";");
        method.append("if ((Flags.lflags.var1 & 0x8000)!=0) {");
        method.append("Flags.lflags.res = ((Flags.lflags.var1 >> ");method.append(op2);method.append(") | (0xFFFF << ");method.append(16-op2);method.append("))  & 0xFFFF;");
        method.append("} else {");
        method.append("Flags.lflags.res = (Flags.lflags.var1 >> ");method.append(op2);method.append(") & 0xFFFF;");
        method.append("}");
        method.append(save);
        method.append("Flags.lflags.type=Flags.t_SARw;");
    }

    static public void SARW(StringBuffer method, String op2, String load, String save) {
        method.append("{int op2 = ");method.append(op2);method.append(";");
        method.append("if (op2!=0) {");
        method.append("Flags.lflags.var1 = ");method.append(load);method.append(";");
        method.append("if (op2>16) op2=16;");
        method.append("Flags.lflags.var2 = ");method.append(op2);method.append(";");
        method.append("if ((Flags.lflags.var1 & 0x8000)!=0) {");
        method.append("Flags.lflags.res = ((Flags.lflags.var1 >> op2) | (0xFFFF << (16-op2)))  & 0xFFFF;");
        method.append("} else {");
        method.append("Flags.lflags.res = (Flags.lflags.var1 >> op2) & 0xFFFF;");
        method.append("}");
        method.append(save);
        method.append("Flags.lflags.type=Flags.t_SARw;}}");
    }

    static public void SARD(StringBuffer method, String op2, String load, String save) {
        method.append("Flags.lflags.var1 = ");method.append(load);method.append(";");
        method.append("Flags.lflags.var2 = ");method.append(op2);method.append(";");
        method.append("Flags.lflags.res = (int)Flags.lflags.var1 >> (int)Flags.lflags.var2;");
        method.append(save);
        method.append("Flags.lflags.type=Flags.t_SARd;");
    }
    static public void SARD(StringBuffer method, int op2, String load, String save) {
        method.append("Flags.lflags.var1 = ");method.append(load);method.append(";");
        method.append("Flags.lflags.var2 = ");method.append(op2);method.append(";");
        method.append("Flags.lflags.res = (int)Flags.lflags.var1 >> ");method.append(op2);method.append(";");
        method.append(save);
        method.append("Flags.lflags.type=Flags.t_SARd;");
    }
    static protected void GRP2B(StringBuffer method, int blah) {
        int rm = decode_fetchb();
        /*Bitu*/int which=(rm>>3)&7;
        String l;
        String s;
        /*Bit8u*/short val=(short)(blah & 0x1f);
        method.append("{");
        if (rm >= 0xc0) {
            l = getearb(rm)+"()";
            s = getearb(rm)+"((short)Flags.lflags.res);";

        } else {
            method.append("long eaa = "); getEaa(method, rm);
            method.append("int index=Paging.getDirectIndex(eaa);");
            method.append("if (index>=0) {");
            l = "Memory.host_readb(index)";
            s = "Memory.host_writeb(index, (short)Flags.lflags.res);";
            switch (which)	{
            case 0x00:ROLB(method, val,l, s);break;
            case 0x01:RORB(method, val,l, s);break;
            case 0x02:RCLB(method, val,l, s);break;
            case 0x03:RCRB(method, val,l, s);break;
            case 0x04:/* SHL and SAL are the same */
            case 0x06:SHLB(method, val,l, s);break;
            case 0x05:SHRB(method, val,l, s);break;
            case 0x07:SARB(method, val,l, s);break;
            }
            method.append("} else {");
            l = "Memory.mem_readb(eaa)";
            s = "Memory.mem_writeb(eaa, (short)Flags.lflags.res);";
        }
        switch (which)	{
        case 0x00:ROLB(method, val,l, s);break;
        case 0x01:RORB(method, val,l, s);break;
        case 0x02:RCLB(method, val,l, s);break;
        case 0x03:RCRB(method, val,l, s);break;
        case 0x04:/* SHL and SAL are the same */
        case 0x06:SHLB(method, val,l, s);break;
        case 0x05:SHRB(method, val,l, s);break;
        case 0x07:SARB(method, val,l, s);break;
        }
        if (rm < 0xc0)
            method.append("}");
        method.append("}");
    }

    static protected void GRP2B(StringBuffer method, String blah) {
        int rm = decode_fetchb();
        /*Bitu*/int which=(rm>>3)&7;
        String l;
        String s;
        if (rm >= 0xc0) {
            method.append("{short val=(short)(");method.append(blah);method.append(" & 0x1f);");
            l = getearb(rm)+"()";
            s = getearb(rm)+"((short)Flags.lflags.res);";
        } else {
            method.append("{long eaa = "); getEaa(method, rm);
            method.append("short val=(short)(");method.append(blah);method.append(" & 0x1f);");
            method.append("int index=Paging.getDirectIndex(eaa);");
            method.append("if (index>=0) {");
            l = "Memory.host_readb(index)";
            s = "Memory.host_writeb(index, (short)Flags.lflags.res);";
            switch (which)	{
            case 0x00:ROLB(method, "val",l, s);break;
            case 0x01:RORB(method, "val",l, s);break;
            case 0x02:RCLB(method, "val",l, s);break;
            case 0x03:RCRB(method, "val",l, s);break;
            case 0x04:/* SHL and SAL are the same */
            case 0x06:SHLB(method, "val",l, s);break;
            case 0x05:SHRB(method, "val",l, s);break;
            case 0x07:SARB(method, "val",l, s);break;
            }
            method.append("} else {");
            l = "Memory.mem_readb(eaa)";
            s = "Memory.mem_writeb(eaa, (short)Flags.lflags.res);";
        }
        switch (which)	{
        case 0x00:ROLB(method, "val",l, s);break;
        case 0x01:RORB(method, "val",l, s);break;
        case 0x02:RCLB(method, "val",l, s);break;
        case 0x03:RCRB(method, "val",l, s);break;
        case 0x04:/* SHL and SAL are the same */
        case 0x06:SHLB(method, "val",l, s);break;
        case 0x05:SHRB(method, "val",l, s);break;
        case 0x07:SARB(method, "val",l, s);break;
        }
        if (rm < 0xc0)
            method.append("}");
        method.append("}");
    }

    static protected void GRP2B_fetchb(StringBuffer method) {
        int rm = decode_fetchb();
        /*Bitu*/int which=(rm>>3)&7;
        String l;
        String s;
        /*Bit8u*/short val;
        method.append("{");
        if (rm >= 0xc0) {
            short blah = decode_fetchb();
            val=(short)(blah & 0x1f);
            l = getearb(rm)+"()";
            s = getearb(rm)+"((short)Flags.lflags.res);";
        } else {
            method.append("long eaa = "); getEaa(method, rm);
            short blah = decode_fetchb();
            val=(short)(blah & 0x1f);
            method.append("int index=Paging.getDirectIndex(eaa);");
            method.append("if (index>=0) {");
            l = "Memory.host_readb(index)";
            s = "Memory.host_writeb(index, (short)Flags.lflags.res);";
            switch (which)	{
            case 0x00:ROLB(method, val,l, s);break;
            case 0x01:RORB(method, val,l, s);break;
            case 0x02:RCLB(method, val,l, s);break;
            case 0x03:RCRB(method, val,l, s);break;
            case 0x04:/* SHL and SAL are the same */
            case 0x06:SHLB(method, val,l, s);break;
            case 0x05:SHRB(method, val,l, s);break;
            case 0x07:SARB(method, val,l, s);break;
            }
            method.append("} else {");
            l = "Memory.mem_readb(eaa)";
            s = "Memory.mem_writeb(eaa, (short)Flags.lflags.res);";
        }
        switch (which)	{
        case 0x00:ROLB(method, val,l, s);break;
        case 0x01:RORB(method, val,l, s);break;
        case 0x02:RCLB(method, val,l, s);break;
        case 0x03:RCRB(method, val,l, s);break;
        case 0x04:/* SHL and SAL are the same */
        case 0x06:SHLB(method, val,l, s);break;
        case 0x05:SHRB(method, val,l, s);break;
        case 0x07:SARB(method, val,l, s);break;
        }
        if (rm < 0xc0)
            method.append("}");
        method.append("}");
    }

    static protected void GRP2W(StringBuffer method, String blah) {
        int rm = decode_fetchb();
        /*Bitu*/int which=(rm>>3)&7;
        String l;
        String s;
        if (rm >= 0xc0) {
            method.append("{int val=");method.append(blah);method.append(" & 0x1f;");
            l = getearw(rm)+"()";
            s = getearw(rm)+"((int)Flags.lflags.res);";
        } else {
            method.append("{long eaa = "); getEaa(method, rm);
            method.append("int val=");method.append(blah);method.append(" & 0x1f;");
            method.append("int index;");
            method.append("if ((eaa & 0xFFF)<0xFFF && (index=Paging.getDirectIndex(eaa))>=0) {");
            l = "Memory.host_readw(index)";
            s = "Memory.host_writew(index, (int)Flags.lflags.res);";
            switch (which)	{
            case 0x00:ROLW(method, "val",l, s);break;
            case 0x01:RORW(method, "val",l, s);break;
            case 0x02:RCLW(method, "val",l, s);break;
            case 0x03:RCRW(method, "val",l, s);break;
            case 0x04:/* SHL and SAL are the same */
            case 0x06:SHLW(method, "val",l, s);break;
            case 0x05:SHRW(method, "val",l, s);break;
            case 0x07:SARW(method, "val",l, s);break;
            }
            method.append("} else {");
            l = "Memory.mem_readw(eaa)";
            s = "Memory.mem_writew(eaa, (int)Flags.lflags.res);";
        }
        switch (which)	{
        case 0x00:ROLW(method, "val",l, s);break;
        case 0x01:RORW(method, "val",l, s);break;
        case 0x02:RCLW(method, "val",l, s);break;
        case 0x03:RCRW(method, "val",l, s);break;
        case 0x04:/* SHL and SAL are the same */
        case 0x06:SHLW(method, "val",l, s);break;
        case 0x05:SHRW(method, "val",l, s);break;
        case 0x07:SARW(method, "val",l, s);break;
        }
        if (rm < 0xc0)
            method.append("}");
        method.append("}");
    }

    static protected void GRP2W(StringBuffer method, int blah) {
        int rm = decode_fetchb();
        /*Bitu*/int which=(rm>>3)&7;
        String l;
        String s;
        /*Bit8u*/int val=blah & 0x1f;
        method.append("{");
        if (rm >= 0xc0) {
            l = getearw(rm)+"()";
            s = getearw(rm)+"((int)Flags.lflags.res);";

        } else {
            method.append("long eaa = "); getEaa(method, rm);
            method.append("int index;");
            method.append("if ((eaa & 0xFFF)<0xFFF && (index=Paging.getDirectIndex(eaa))>=0) {");
            l = "Memory.host_readw(index)";
            s = "Memory.host_writew(index, (int)Flags.lflags.res);";
            switch (which)	{
            case 0x00:ROLW(method, val,l, s);break;
            case 0x01:RORW(method, val,l, s);break;
            case 0x02:RCLW(method, val,l, s);break;
            case 0x03:RCRW(method, val,l, s);break;
            case 0x04:/* SHL and SAL are the same */
            case 0x06:SHLW(method, val,l, s);break;
            case 0x05:SHRW(method, val,l, s);break;
            case 0x07:SARW(method, val,l, s);break;
            }
            method.append("} else {");
            l = "Memory.mem_readw(eaa)";
            s = "Memory.mem_writew(eaa, (int)Flags.lflags.res);";
        }
        switch (which)	{
        case 0x00:ROLW(method, val,l, s);break;
        case 0x01:RORW(method, val,l, s);break;
        case 0x02:RCLW(method, val,l, s);break;
        case 0x03:RCRW(method, val,l, s);break;
        case 0x04:/* SHL and SAL are the same */
        case 0x06:SHLW(method, val,l, s);break;
        case 0x05:SHRW(method, val,l, s);break;
        case 0x07:SARW(method, val,l, s);break;
        }
        if (rm < 0xc0)
            method.append("}");
        method.append("}");
    }

    static protected void GRP2W_fetchb(StringBuffer method) {
         int rm = decode_fetchb();
        /*Bitu*/int which=(rm>>3)&7;
        String l;
        String s;
        int val;
        method.append("{");
        if (rm >= 0xc0) {
            short blah = decode_fetchb();
            val=blah & 0x1f;
            l = getearw(rm)+"()";
            s = getearw(rm)+"((int)Flags.lflags.res);";
        } else {
            method.append("long eaa = "); getEaa(method, rm);
            short blah = decode_fetchb();
            val=blah & 0x1f;
            method.append("int index;");
            method.append("if ((eaa & 0xFFF)<0xFFF && (index=Paging.getDirectIndex(eaa))>=0) {");
            l = "Memory.host_readw(index)";
            s = "Memory.host_writew(index, (int)Flags.lflags.res);";
            switch (which)	{
            case 0x00:ROLW(method, val,l, s);break;
            case 0x01:RORW(method, val,l, s);break;
            case 0x02:RCLW(method, val,l, s);break;
            case 0x03:RCRW(method, val,l, s);break;
            case 0x04:/* SHL and SAL are the same */
            case 0x06:SHLW(method, val,l, s);break;
            case 0x05:SHRW(method, val,l, s);break;
            case 0x07:SARW(method, val,l, s);break;
            }
            method.append("} else {");
            l = "Memory.mem_readw(eaa)";
            s = "Memory.mem_writew(eaa, (int)Flags.lflags.res);";
        }
        switch (which)	{
        case 0x00:ROLW(method, val,l, s);break;
        case 0x01:RORW(method, val,l, s);break;
        case 0x02:RCLW(method, val,l, s);break;
        case 0x03:RCRW(method, val,l, s);break;
        case 0x04:/* SHL and SAL are the same */
        case 0x06:SHLW(method, val,l, s);break;
        case 0x05:SHRW(method, val,l, s);break;
        case 0x07:SARW(method, val,l, s);break;
        }
        if (rm < 0xc0)
            method.append("}");
        method.append("}");
    }

    static protected void GRP2D(StringBuffer method, String blah) {
        int rm = decode_fetchb();
        /*Bitu*/int which=(rm>>3)&7;
        String l;
        String s;
        if (rm >= 0xc0) {
            method.append("{int val=");method.append(blah);method.append(" & 0x1f;");
            l = geteard(rm)+"()";
            s = geteard(rm)+"(Flags.lflags.res);";
        } else {
            method.append("{long eaa = "); getEaa(method, rm);
            method.append("int val=");method.append(blah);method.append(" & 0x1f;");
            method.append("int index;");
            method.append("if ((eaa & 0xFFF)<0xFFD && (index=Paging.getDirectIndex(eaa))>=0) {");
            l = "Memory.host_readd(index)";
            s = "Memory.host_writed(index, Flags.lflags.res);";
            switch (which)	{
            case 0x00:ROLD(method, "val",l, s);break;
            case 0x01:RORD(method, "val",l, s);break;
            case 0x02:RCLD(method, "val",l, s);break;
            case 0x03:RCRD(method, "val",l, s);break;
            case 0x04:/* SHL and SAL are the same */
            case 0x06:SHLD(method, "val",l, s);break;
            case 0x05:SHRD(method, "val",l, s);break;
            case 0x07:SARD(method, "val",l, s);break;
            }
            method.append("} else {");
            l = "Memory.mem_readd(eaa)";
            s = "Memory.mem_writed(eaa, Flags.lflags.res);";
        }
        switch (which)	{
        case 0x00:ROLD(method, "val",l, s);break;
        case 0x01:RORD(method, "val",l, s);break;
        case 0x02:RCLD(method, "val",l, s);break;
        case 0x03:RCRD(method, "val",l, s);break;
        case 0x04:/* SHL and SAL are the same */
        case 0x06:SHLD(method, "val",l, s);break;
        case 0x05:SHRD(method, "val",l, s);break;
        case 0x07:SARD(method, "val",l, s);break;
        }
        if (rm < 0xc0)
            method.append("}");
        method.append("}");
    }

    static protected void GRP2D(StringBuffer method, int blah) {
        int rm = decode_fetchb();
        /*Bitu*/int which=(rm>>3)&7;
        String l;
        String s;
        /*Bit8u*/short val=(short)(blah & 0x1f);
        method.append("{");
        if (rm >= 0xc0) {
            l = geteard(rm)+"()";
            s = geteard(rm)+"(Flags.lflags.res);";

        } else {
            method.append("long eaa = "); getEaa(method, rm);
            method.append("int index;");
            method.append("if ((eaa & 0xFFF)<0xFFD && (index=Paging.getDirectIndex(eaa))>=0) {");
            l = "Memory.host_readd(index)";
            s = "Memory.host_writed(index, Flags.lflags.res);";
            switch (which)	{
            case 0x00:ROLD(method, val,l, s);break;
            case 0x01:RORD(method, val,l, s);break;
            case 0x02:RCLD(method, val,l, s);break;
            case 0x03:RCRD(method, val,l, s);break;
            case 0x04:/* SHL and SAL are the same */
            case 0x06:SHLD(method, val,l, s);break;
            case 0x05:SHRD(method, val,l, s);break;
            case 0x07:SARD(method, val,l, s);break;
            }
            method.append("} else {");
            l = "Memory.mem_readd(eaa)";
            s = "Memory.mem_writed(eaa, Flags.lflags.res);";
        }
        switch (which)	{
        case 0x00:ROLD(method, val,l, s);break;
        case 0x01:RORD(method, val,l, s);break;
        case 0x02:RCLD(method, val,l, s);break;
        case 0x03:RCRD(method, val,l, s);break;
        case 0x04:/* SHL and SAL are the same */
        case 0x06:SHLD(method, val,l, s);break;
        case 0x05:SHRD(method, val,l, s);break;
        case 0x07:SARD(method, val,l, s);break;
        }
        if (rm < 0xc0)
            method.append("}");
        method.append("}");
    }

    static protected void grplb(StringBuffer method) {
        int rm=decode_fetchb();
        int which=(rm>>3)&7;
        String op = "";
        switch (which) {
        case 0x00:op = "ADDB";break;
        case 0x01:op = "ORB";break;
        case 0x02:op = "ADCB";break;
        case 0x03:op = "SBBB";break;
        case 0x04:op = "ANDB";break;
        case 0x05:op = "SUBB";break;
        case 0x06:op = "XORB";break;
        case 0x07:op = "CMPB";break;
        }
        if (rm>= 0xc0) {
            int ib=decode_fetchb();
            if (which != 7) {
                method.append(getearb(rm));
                method.append("(");
            }
            method.append("Instructions.");
            method.append(op);
            method.append("((short)");method.append(ib);method.append(",");method.append(getearb(rm));method.append("())");
            if (which !=7)
                method.append(")");
            method.append(";");
        } else {
            method.append("{long eaa = ");getEaa(method, rm);

            int ib=decode_fetchb();
            method.append("int index = Paging.getDirectIndex(eaa);");
            method.append("if (index>=0) {");
            if (which != 7) {
                method.append("Memory.host_writeb(index, ");
            }
            method.append("Instructions.");
            method.append(op);
            method.append("((short)");
            method.append(ib);
            method.append(",Memory.host_readb(index))");
            if (which != 7) {
                method.append(")");
            }
            method.append(";");
            method.append("} else {");
            if (which != 7) {
                method.append("Memory.mem_writeb(eaa, ");
            }
            method.append("Instructions.");
            method.append(op);
            method.append("((short)");
            method.append(ib);
            method.append(",Memory.mem_readb(eaa))");
            if (which != 7) {
                method.append(")");
            }
            method.append(";}}");
        }
    }
    static protected void grplw(StringBuffer method, boolean signed) {
        int rm=decode_fetchb();
        int which=(rm>>3)&7;
        String op = "";
        switch (which) {
        case 0x00:op = "ADDW";break;
        case 0x01:op = "ORW";break;
        case 0x02:op = "ADCW";break;
        case 0x03:op = "SBBW";break;
        case 0x04:op = "ANDW";break;
        case 0x05:op = "SUBW";break;
        case 0x06:op = "XORW";break;
        case 0x07:op = "CMPW";break;
        }
        if (rm>= 0xc0) {
            int ib;
            if (signed)
                ib = (((short)decode_fetchbs()) & 0xFFFF);
            else
                ib=decode_fetchw();
            if (which != 7) {
                method.append(getearw(rm));
                method.append("(");
            }
            method.append("Instructions.");
            method.append(op);
            method.append("(");method.append(ib);method.append(",");method.append(getearw(rm));method.append("())");
            if (which !=7)
                method.append(")");
            method.append(";");
        } else {
            method.append("{long eaa = ");getEaa(method, rm);
            int ib;
            if (signed)
                ib = (((short)decode_fetchbs()) & 0xFFFF);
            else
                ib=decode_fetchw();
            method.append("int index = Paging.getDirectIndex(eaa);");
            method.append("if ((eaa & 0xFFF)<0xFFF && index>=0) {");
            if (which != 7) {
                method.append("Memory.host_writew(index, ");
            }
            method.append("Instructions.");
            method.append(op);
            method.append("(");
            method.append(ib);
            method.append(",Memory.host_readw(index))");
            if (which != 7) {
                method.append(")");
            }
            method.append(";");
            method.append("} else {");
            if (which != 7) {
                method.append("Memory.mem_writew(eaa, ");
            }
            method.append("Instructions.");
            method.append(op);
            method.append("(");
            method.append(ib);
            method.append(",Memory.mem_readw(eaa))");
            if (which != 7) {
                method.append(")");
            }
            method.append(";}}");
        }
    }

    static protected void DoString(StringBuffer method, String type) {
        method.append("StringOp.DoString(");method.append(prefixes);method.append(",StringOp.");method.append(type);method.append(");");
    }

    static protected void bound(StringBuffer method) {
        method.append("short bound_min, bound_max;");
        int rm = decode_fetchb();
        method.append("{long eaa=");getEaa(method, rm);
        method.append("bound_min=(short)Memory.mem_readw(eaa);");
        method.append("bound_max=(short)Memory.mem_readw(eaa+2);");
        method.append("if ((((short)");method.append(getrw(rm));method.append("())<bound_min) || (((short)");
        method.append(getrw(rm));method.append("()) > bound_max))");
        EXCEPTION(method, 5);
        method.append("}");
    }

    static protected void ARPLEwRw(StringBuffer method) {
        method.append("if ((CPU_Regs.flags & CPU_Regs.VM)!=0 || (!CPU.cpu.pmode)) return Constants.BR_Illegal;");
        short rm=decode_fetchb();
        if (rm >= 0xc0 ) {
            method.append("int value = ");method.append(getearw(rm));method.append("();");
        } else {
            method.append("{long eaa=");getEaa(method, rm);
            method.append("int value=");method.append("Memory.mem_readw(eaa);}");
        }
        method.append("int src_sel = "); method.append(getrw(rm));method.append("();");
        method.append("Flags.FillFlags();");
        method.append("if ((value & 3) < (src_sel & 3)) {");
        method.append("value=(value & 0xfffc) + (src_sel & 3);");
        method.append("CPU_Regs.SETFLAGBIT(CPU_Regs.ZF,true);");
        if (rm >= 0xc0) {
            method.append(getearw(rm));method.append("(value);");
        } else {
            method.append("Memory.mem_writew(eaa, value);");
        }
        method.append("} else {CPU_Regs.SETFLAGBIT(CPU_Regs.ZF,false);}");
    }

    static protected void IMULGwEwIw(StringBuffer method) {
        int rm=decode_fetchb();
        if (rm>=0xc0) {
            int op3 = decode_fetchws();
            method.append(getrw(rm));method.append("(Instructions.DIMULW(");
            method.append(getearw(rm));method.append("(),");method.append(op3);method.append("));");
        } else {
            method.append("{long eaa = ");getEaa(method, rm);
            int op3 = decode_fetchws();
            method.append(getrw(rm));method.append("(Instructions.DIMULW(Memory.mem_readw(eaa),");
            method.append(op3);method.append("));}");
        }
    }

    static protected void IMULGwEwIb(StringBuffer method) {
        int rm=decode_fetchb();
        if (rm>=0xc0) {
            int op3 = decode_fetchbs();
            method.append(getrw(rm));method.append("(Instructions.DIMULW(");
            method.append(getearw(rm));method.append("(),");method.append(op3);method.append("));");
        } else {
            method.append("{long eaa = ");getEaa(method, rm);
            int op3 = decode_fetchbs();
            method.append(getrw(rm));method.append("(Instructions.DIMULW(Memory.mem_readw(eaa),");
            method.append(op3);method.append("));}");
        }
    }

    static protected void IMULGwEw(StringBuffer method) {
        int rm=decode_fetchb();
        if (rm>=0xc0) {
            method.append(getrw(rm));method.append("(Instructions.DIMULW(");
            method.append(getearw(rm));method.append("(),");method.append(getrw(rm));method.append("()));");
        } else {
            method.append("{long eaa = ");getEaa(method, rm);
            method.append(getrw(rm));method.append("(Instructions.DIMULW(Memory.mem_readw(eaa),");
            method.append(getrw(rm));method.append("()));}");
        }
    }

    static protected void TestEbGb(StringBuffer method) {
        int rm=decode_fetchb();
        if (rm >= 0xc0 ) {
            method.append("Instructions.TESTB(");
            method.append(getrb(rm));
            method.append("(),");
            method.append(getearb(rm));
            method.append("());");
        }
        else {
            method.append("{long eaa = ");getEaa(method, rm);
            method.append("Instructions.TESTB(");
            method.append(getrb(rm));
            method.append("(),");
            method.append("Memory.mem_readb(eaa));}");
        }
    }

    static protected void TestEwGw(StringBuffer method) {
        int rm=decode_fetchb();
        if (rm >= 0xc0 ) {
            method.append("Instructions.TESTW(");
            method.append(getrw(rm));
            method.append("(),");
            method.append(getearw(rm));
            method.append("());");
        }
        else {
            method.append("{long eaa = ");getEaa(method, rm);
            method.append("Instructions.TESTW(");
            method.append(getrw(rm));
            method.append("(),");
            method.append("Memory.mem_readw(eaa));}");
        }
    }

    static protected int Grp7(StringBuffer method) {
        int result = 0;
        int rm=decode_fetchb();
        int which=(rm>>3)&7;
        if (rm < 0xc0)	{ //First ones all use EA
            method.append("{long eaa=");getEaa(method, rm);
            switch (which) {
            case 0x00:										/* SGDT */
                method.append("Memory.mem_writew(eaa,CPU.CPU_SGDT_limit());");
                method.append("Memory.mem_writed(eaa+2,CPU.CPU_SGDT_base());");
                break;
            case 0x01:										/* SIDT */
                method.append("Memory.mem_writew(eaa,CPU.CPU_SIDT_limit());");
                method.append("Memory.mem_writed(eaa+2,CPU.CPU_SIDT_base());");
                break;
            case 0x02:										/* LGDT */
                method.append("if (CPU.cpu.pmode && CPU.cpu.cpl!=0) ");EXCEPTION(method, CPU.EXCEPTION_GP);
                method.append("long v1 = (Memory.mem_readd(eaa+2) & 0xFFFFFFl);");
                method.append("int v0 = Memory.mem_readw(eaa);");
                method.append("CPU.CPU_LGDT(v0,v1);");
                break;
            case 0x03:										/* LIDT */
                method.append("if (CPU.cpu.pmode && CPU.cpu.cpl!=0) ");EXCEPTION(method, CPU.EXCEPTION_GP);
                method.append("long v1 = (Memory.mem_readd(eaa+2) & 0xFFFFFFl);");
                method.append("int v0 = Memory.mem_readw(eaa);");
                method.append("CPU.CPU_LIDT(v0,v1);");
                break;
            case 0x04:										/* SMSW */
                method.append("Memory.mem_writew(eaa,(int)(CPU.CPU_SMSW() & 0xFFFFl));");
                break;
            case 0x06:										/* LMSW */
                method.append("int limit=Memory.mem_readw(eaa);");
                method.append("if (CPU.CPU_LMSW(limit)) "); RUNEXCEPTION(method);
                break;
            case 0x07:										/* INVLPG */
                method.append("if (CPU.cpu.pmode && CPU.cpu.cpl!=0) "); EXCEPTION(method, CPU.EXCEPTION_GP);
                method.append("Paging.PAGING_ClearTLB();");
                break;
            }
            method.append("}");
        } else {
            switch (which) {
            case 0x02:										/* LGDT */
                method.append("if (CPU.cpu.pmode && CPU.cpu.cpl!=0) ");EXCEPTION(method, CPU.EXCEPTION_GP);
                method.append("return Constants.BR_Illegal;");
                result = RESULT_RETURN;
                break;
            case 0x03:										/* LIDT */
                method.append("if (CPU.cpu.pmode && CPU.cpu.cpl!=0) ");EXCEPTION(method, CPU.EXCEPTION_GP);
                method.append("return Constants.BR_Illegal;");
                result = RESULT_RETURN;
                break;
            case 0x04:										/* SMSW */
                method.append(getearw(rm));method.append("((int)(CPU.CPU_SMSW() & 0xFFFF));");
                break;
            case 0x06:										/* LMSW */
                method.append("if (CPU.CPU_LMSW(");method.append(getearw(rm));method.append("())) ");RUNEXCEPTION(method);
                break;
            default:
                method.append("return Constants.BR_Illegal;");
                result = RESULT_RETURN;
                break;
            }
        }
        return result;
    }
    static protected void Grp6(StringBuffer method) {
        method.append("if ((CPU_Regs.flags & CPU_Regs.VM)!=0 || (!CPU.cpu.pmode)) return Constants.BR_Illegal;");
        /*Bit8u*/short rm=decode_fetchb();
        /*Bitu*/int which=(rm>>3)&7;
        switch (which) {
        case 0x00:	/* SLDT */
        case 0x01:	/* STR */
            {
                method.append("int saveval=");
                if (which==0) method.append("CPU.CPU_SLDT();");
                else method.append("CPU.CPU_STR();");
                if (rm >= 0xc0) {
                    method.append(getearw(rm));method.append("(saveval);");
                } else {
                    method.append("long eaa=");getEaa(method, rm);
                    method.append("Memory.mem_writew(eaa,saveval);");
                }
            }
            break;
        case 0x02:case 0x03:case 0x04:case 0x05:
            {
                if (rm >= 0xc0 ) {method.append("int loadval = ");method.append(getearw(rm));method.append("();");}
                else {method.append("long eaa=");getEaa(method, rm);method.append("int loadval=Memory.mem_readw(eaa);");}
                switch (which) {
                case 0x02:
                    method.append("if (CPU.cpu.cpl!=0) ");EXCEPTION(method, CPU.EXCEPTION_GP);
                    method.append("if (CPU.CPU_LLDT(loadval)) ");RUNEXCEPTION(method);
                    break;
                case 0x03:
                    method.append("if (CPU.cpu.cpl!=0) ");EXCEPTION(method, CPU.EXCEPTION_GP);
                    method.append("if (CPU.CPU_LTR(loadval)) ");RUNEXCEPTION(method);
                    break;
                case 0x04:
                    method.append("CPU.CPU_VERR(loadval);");
                    break;
                case 0x05:
                    method.append("CPU.CPU_VERW(loadval);");
                    break;
                }
            }
            break;
        default:
            Log.exit("Oops");
        }
    }
    static protected int Grp5Ew(StringBuffer method) {
        int result = 0;
        int rm = decode_fetchb();
        int which=(rm>>3)&7;
        switch (which) {
        case 0x00:										/* INC Ew */
            ew(method, rm, "Instructions.INCW");
            break;
        case 0x01:										/* DEC Ew */
            ew(method, rm, "Instructions.DECW");
            break;
        case 0x02:										/* CALL Ev */
            method.append("{");
            method.append("long eip = CPU_Regs.reg_eip;");
            if (rm >= 0xc0 ) {
                method.append("CPU_Regs.reg_eip((long)");method.append(getearw(rm));method.append("());");
            } else {
                method.append("{long eaa=");getEaa(method, rm);
                method.append("CPU_Regs.reg_eip((long)Memory.mem_readw(eaa));}");
            }
            method.append("eip += ");method.append(decode.code-decode.code_start);method.append(";");
            method.append("CPU.CPU_Push16((int)(eip & 0xFFFFl));}");
            result = RESULT_CONTINUE;
            break;
        case 0x03:										/* CALL Ep */
            {
                if (rm >= 0xc0) Log.exit("Oops");
                method.append("{long eaa=");getEaa(method, rm);
                method.append("long newip=Memory.mem_readw(eaa);");
                method.append("int newcs=Memory.mem_readw(eaa+2);");
                method.append("Flags.FillFlags();");
                GETIP(method);
                method.append("CPU.CPU_CALL(false,newcs,newip,eip);");
                if (CPU_TRAP_CHECK) {
                    method.append("if (CPU_Regs.GETFLAG(CPU_Regs.TF)!=0) {");
                    returnTrap(method);
                    method.append("}");
                }
                method.append("}");
                result = RESULT_CONTINUE;
                break;
            }
        case 0x04:										/* JMP Ev */
            if (rm >= 0xc0 ) {
                method.append("CPU_Regs.reg_eip((long)");method.append(getearw(rm));method.append("());");
            } else {
                method.append("{long eaa=");getEaa(method, rm);
                method.append("CPU_Regs.reg_eip((long)Memory.mem_readw(eaa));}");
            }
            result = RESULT_CONTINUE;
            break;
        case 0x05:										/* JMP Ep */
            {
                if (rm >= 0xc0) Log.exit("Oops");
                method.append("{long eaa=");getEaa(method, rm);
                method.append("int newip=Memory.mem_readw(eaa);");
                method.append("int newcs=Memory.mem_readw(eaa+2);");
                method.append("Flags.FillFlags();");
                GETIP(method);
                method.append("CPU.CPU_JMP(false,newcs,newip,eip);");
                if (CPU_TRAP_CHECK) {
                    method.append("if (CPU_Regs.GETFLAG(CPU_Regs.TF)!=0) {");
                    returnTrap(method);
                    method.append("}");
                }
                method.append("}");
                result = RESULT_CONTINUE;
                break;
            }
        case 0x06:										/* PUSH Ev */
            if (rm >= 0xc0 ) {
                method.append("CPU.CPU_Push16(");method.append(getearw(rm));method.append("());");
            } else {
                method.append("{long eaa=");getEaa(method, rm);
                method.append("CPU.CPU_Push16(Memory.mem_readw(eaa));}");
            }
            break;
        default:
            Log.exit("CPU:GRP5:Illegal Call "+Integer.toString(which,16));
        }
        return result;
    }
    static protected void Grp3EwIw(StringBuffer method) {
        int rm=decode_fetchb();
        int which=(rm>>3)&7;
        switch (which) {
        case 0x00:											/* TEST Ew,Iw */
        case 0x01:											/* TEST Ew,Iw Undocumented*/
            {
                if (rm >= 0xc0 ) {
                    method.append("Instructions.TESTW(");method.append(decode_fetchw());method.append(",");method.append(getearw(rm));method.append("());");
                }
                else {
                    method.append("{long eaa = ");getEaa(method, rm);
                    method.append("Instructions.TESTW(");method.append(decode_fetchw());method.append(",Memory.mem_readw(eaa));}");
                }
                break;
            }
        case 0x02:											/* NOT Ew */
            {
                if (rm >= 0xc0 ) {
                    method.append(getearw(rm));method.append("(~");method.append(getearw(rm));method.append("());");
                } else {
                    method.append("{long eaa=");getEaa(method, rm);
                    method.append("if ((eaa & 0xFFF)<0xFFF) {");
                        method.append("int index = Paging.getDirectIndex(eaa);");
                        method.append("if (index>=0) {");
                            method.append("Memory.host_writew(index,~Memory.host_readw(index));");
                        method.append("} else {");
                            method.append("Memory.mem_writew(eaa,~Memory.mem_readw(eaa));");
                        method.append("}");
                    method.append("} else {");
                        method.append("Memory.mem_writew(eaa,~Memory.mem_readw(eaa));");
                    method.append("}}");
                }
                break;
            }
        case 0x03:											/* NEG Ew */
            {
                method.append("Flags.lflags.type=Flags.t_NEGw;");
                if (rm >= 0xc0 ) {
                    method.append("Flags.lflags.var1 = ");method.append(getearw(rm));method.append("();");
                    method.append("Flags.lflags.res = (0-Flags.lflags.var1) & 0xFFFF;");
                    method.append(getearw(rm));method.append("((int)Flags.lflags.res);");
                } else {
                    method.append("{long eaa=");getEaa(method, rm);
                    method.append("if ((eaa & 0xFFF)<0xFFF) {");
                        method.append("int index = Paging.getDirectIndex(eaa);");
                        method.append("if (index>=0) {");
                            method.append("Flags.lflags.var1 = Memory.host_readw(index);");
                            method.append("Flags.lflags.res = (0-Flags.lflags.var1) & 0xFFFF;");
                            method.append("Memory.host_writew(index,(int)Flags.lflags.res);");
                        method.append("} else {");
                            method.append("Flags.lflags.var1 = Memory.mem_readw(eaa);");
                            method.append("Flags.lflags.res = (0-Flags.lflags.var1) & 0xFFFF;");
                            method.append("Memory.mem_writew(eaa,(int)Flags.lflags.res);");
                        method.append("}");
                    method.append("} else {");
                        method.append("Flags.lflags.var1 = Memory.mem_readw(eaa);");
                        method.append("Flags.lflags.res = (0-Flags.lflags.var1) & 0xFFFF;");
                        method.append("Memory.mem_writew(eaa,(int)Flags.lflags.res);");
                    method.append("}}");
                }
                break;
            }
        case 0x04:											/* MUL AX,Ew */
            if (rm >= 0xc0 ) {
                method.append("Instructions.MULW(");method.append(getearw(rm));method.append("());");
            }
            else {
                method.append("{long eaa = ");getEaa(method, rm);
                method.append("Instructions.MULW(Memory.mem_readw(eaa));}");
            }
            break;
        case 0x05:											/* IMUL AX,Ew */
            if (rm >= 0xc0 ) {
                method.append("Instructions.IMULW(");method.append(getearw(rm));method.append("());");
            }
            else {
                method.append("{long eaa = ");getEaa(method, rm);
                method.append("Instructions.IMULW(Memory.mem_readw(eaa));}");
            }
            break;
        case 0x06:											/* DIV Ew */
            if (rm >= 0xc0 ) {
                method.append("Instructions.DIVW(");method.append(getearw(rm));method.append("());");
            }
            else {
                method.append("{long eaa = ");getEaa(method, rm);
                method.append("Instructions.DIVW(Memory.mem_readw(eaa));}");
            }
            break;
        case 0x07:											/* IDIV Ew */
            if (rm >= 0xc0 ) {
                method.append("Instructions.IDIVW(");method.append(getearw(rm));method.append("());");
            }
            else {
                method.append("{long eaa = ");getEaa(method, rm);
                method.append("Instructions.IDIVW(Memory.mem_readw(eaa));}");
            }
            break;
        }
    }
    static protected void Grp3EdId(StringBuffer method) {
        int rm=decode_fetchb();
        int which=(rm>>3)&7;
        switch (which) {
        case 0x00:											/* TEST Ed,Id */
        case 0x01:											/* TEST Ed,Id Undocumented*/
            {
                if (rm >= 0xc0 ) {
                    method.append("Instructions.TESTD(");method.append(decode_fetchd());method.append("l,");method.append(geteard(rm));method.append("());");
                }
                else {
                    method.append("{long eaa = ");getEaa(method, rm);
                    method.append("Instructions.TESTD(");method.append(decode_fetchd());method.append("l,Memory.mem_readd(eaa));}");
                }
                break;
            }
        case 0x02:											/* NOT Ed */
            {
                if (rm >= 0xc0 ) {
                    method.append(geteard(rm));method.append("(~");method.append(geteard(rm));method.append("());");
                }
                else {
                    method.append("{long eaa=");getEaa(method, rm);
                    method.append("if ((eaa & 0xFFF)<0xFFD) {");
                        method.append("int index = Paging.getDirectIndex(eaa);");
                        method.append("if (index>=0) {");
                            method.append("Memory.host_writed(index,~Memory.host_readd(index));");
                        method.append("} else {");
                            method.append("Memory.mem_writed(eaa,~Memory.mem_readd(eaa));");
                        method.append("}");
                    method.append("} else {");
                        method.append("Memory.mem_writed(eaa,~Memory.mem_readd(eaa));");
                    method.append("}}");
                }
                break;
            }
        case 0x03:											/* NEG Ed */
            {
                method.append("Flags.lflags.type=Flags.t_NEGd;");
                if (rm >= 0xc0 ) {
                    method.append("Flags.lflags.var1 = ");method.append(geteard(rm));method.append("();");
                    method.append("Flags.lflags.res = (0-Flags.lflags.var1) & 0xFFFFFFFFl;");
                    method.append(geteard(rm));method.append("(Flags.lflags.res);");
                } else {
                    method.append("{long eaa=");getEaa(method, rm);
                    method.append("if ((eaa & 0xFFF)<0xFFD) {");
                        method.append("int index = Paging.getDirectIndex(eaa);");
                        method.append("if (index>=0) {");
                            method.append("Flags.lflags.var1 = Memory.host_readd(index);");
                            method.append("Flags.lflags.res = (0-Flags.lflags.var1) & 0xFFFFFFFFl;");
                            method.append("Memory.host_writed(index,Flags.lflags.res);");
                        method.append("} else {");
                            method.append("Flags.lflags.var1 = Memory.mem_readd(eaa);");
                            method.append("Flags.lflags.res = (0-Flags.lflags.var1) & 0xFFFFFFFFl;");
                            method.append("Memory.mem_writed(eaa,Flags.lflags.res);");
                        method.append("}");
                    method.append("} else {");
                        method.append("Flags.lflags.var1 = Memory.mem_readd(eaa);");
                        method.append("Flags.lflags.res = (0-Flags.lflags.var1) & 0xFFFFFFFFl;");
                        method.append("Memory.mem_writed(eaa,Flags.lflags.res);");
                    method.append("}}");
                }
                break;
            }
        case 0x04:											/* MUL EAX,Ed */
            if (rm >= 0xc0 ) {
                method.append("Instructions.MULD(");method.append(geteard(rm));method.append("());");
            }
            else {
                method.append("{long eaa = ");getEaa(method, rm);
                method.append("Instructions.MULD(Memory.mem_readd(eaa));}");
            }
            break;
        case 0x05:											/* IMUL EAX,Ed */
            if (rm >= 0xc0 ) {
                method.append("Instructions.IMULD(");method.append(geteard(rm));method.append("());");
            }
            else {
                method.append("{long eaa = ");getEaa(method, rm);
                method.append("Instructions.IMULD(Memory.mem_readd(eaa));}");
            }
            break;
        case 0x06:											/* DIV Ed */
            if (rm >= 0xc0 ) {
                method.append("Instructions.DIVD(");method.append(geteard(rm));method.append("());");
            }
            else {
                method.append("{long eaa = ");getEaa(method, rm);
                method.append("Instructions.DIVD(Memory.mem_readd(eaa));}");
            }
            break;
        case 0x07:											/* IDIV Ed */
            if (rm >= 0xc0 ) {
                method.append("Instructions.IDIVD(");method.append(geteard(rm));method.append("());");
            }
            else {
                method.append("{long eaa = ");getEaa(method, rm);
                method.append("Instructions.IDIVD(Memory.mem_readd(eaa));}");
            }
            break;
        }
    }
    static protected void Grp3EbIb(StringBuffer method) {
        int rm=decode_fetchb();
        int which=(rm>>3)&7;
        switch (which) {
        case 0x00:											/* TEST Eb,Ib */
        case 0x01:											/* TEST Eb,Ib Undocumented*/
            {
                if (rm >= 0xc0 ) {
                    method.append("Instructions.TESTB((short)");method.append(decode_fetchb());method.append(",");method.append(getearb(rm));method.append("());");
                }
                else {
                    method.append("{long eaa = ");getEaa(method, rm);
                    method.append("Instructions.TESTB((short)");method.append(decode_fetchb());method.append(",Memory.mem_readb(eaa));}");
                }
                break;
            }
        case 0x02:											/* NOT Eb */
            {
                if (rm >= 0xc0 ) {
                    method.append(getearb(rm));method.append("(~");method.append(getearb(rm));method.append("());");
                }
                else {
                    method.append("{long eaa=");getEaa(method, rm);
                    method.append("int index = Paging.getDirectIndex(eaa);");
                    method.append("if (index>=0)");
                        method.append("Memory.direct[index]=(byte)~Memory.direct[index];");
                    method.append(" else ");
                        method.append("Memory.mem_writeb(eaa, ~Memory.mem_readb(eaa));");
                    method.append("}");
                }
                break;
            }
        case 0x03:											/* NEG Eb */
            {
                method.append("Flags.lflags.type=Flags.t_NEGb;");
                if (rm >= 0xc0 ) {
                    method.append("Flags.lflags.var1 = ");method.append(getearb(rm));method.append("();");
                    method.append("Flags.lflags.res = (0-Flags.lflags.var1) & 0xFF;");
                    method.append(getearb(rm));method.append("((short)Flags.lflags.res);");
                } else {
                    method.append("{long eaa=");getEaa(method, rm);
                    method.append("Flags.lflags.var1 = Memory.mem_readb(eaa);");
                    method.append("Flags.lflags.res = (0-Flags.lflags.var1) & 0xFF;");
                    method.append("Memory.mem_writeb(eaa,(short)Flags.lflags.res);}");
                }
                break;
            }
        case 0x04:											/* MUL AL,Eb */
            if (rm >= 0xc0 ) {
                method.append("Instructions.MULB(");method.append(getearb(rm));method.append("());");
            }
            else {
                method.append("{long eaa = ");getEaa(method, rm);
                method.append("Instructions.MULB(Memory.mem_readb(eaa));}");
            }
            break;
        case 0x05:											/* IMUL AL,Eb */
            if (rm >= 0xc0 ) {
                method.append("Instructions.IMULB(");method.append(getearb(rm));method.append("());");
            }
            else {
                method.append("{long eaa = ");getEaa(method, rm);
                method.append("Instructions.IMULB(Memory.mem_readb(eaa));}");
            }
            break;
        case 0x06:											/* DIV Eb */
            if (rm >= 0xc0 ) {
                method.append("Instructions.DIVB(");method.append(getearb(rm));method.append("());");
            }
            else {
                method.append("{long eaa = ");getEaa(method, rm);
                method.append("Instructions.DIVB(Memory.mem_readb(eaa));}");
            }
            break;
        case 0x07:											/* IDIV Eb */
            if (rm >= 0xc0 ) {
                method.append("Instructions.IDIVB(");method.append(getearb(rm));method.append("());");
            }
            else {
                method.append("{long eaa = ");getEaa(method, rm);
                method.append("Instructions.IDIVB(Memory.mem_readb(eaa));}");
            }
            break;
        }
    }

    static public void DSHLW(StringBuffer method, String op2,int op3, String l, String s) {
        /*Bit8u*/short val=(short)(op3 & 0x1F);
        if (val==0) return;
        method.append("{Flags.lflags.var2 = ");method.append(val);method.append(";");
        method.append("Flags.lflags.var1 = (");method.append(l);method.append(" << 16) | ");method.append(op2);method.append("();");
        method.append("long tempd=Flags.lflags.var1 << ");method.append(val);method.append(";");
        if (val>16) {method.append("tempd |= (");method.append(op2);method.append("() << ");method.append(val-16);method.append(");");}
        method.append("Flags.lflags.res = ((int)(tempd >>> 16)) & 0xFFFF;");
        method.append("Flags.lflags.type=Flags.t_DSHLw;");
        method.append(s);
        method.append("}");
    }

    static public void DSHLW(StringBuffer method, String op2,String op3, String l, String s) {
        method.append("short val=(short)(");method.append(op3);method.append(" & 0x1F);");
        method.append("if (val!=0) {");
        method.append("Flags.lflags.var2 = val;");
        method.append("Flags.lflags.var1 = (");method.append(l);method.append(" << 16) | ");method.append(op2);method.append("();");
        method.append("long tempd=Flags.lflags.var1 << val;");
        method.append("if (val>16) tempd |= (");method.append(op2);method.append("() << (val - 16));");
        method.append("Flags.lflags.res = ((int)(tempd >>> 16)) & 0xFFFF;");
        method.append("Flags.lflags.type=Flags.t_DSHLw;");
        method.append(s);
        method.append("}");
    }

    static public void DSHRW(StringBuffer method, String op2,int op3, String l, String s) {
        /*Bit8u*/short val=(short)(op3 & 0x1F);
        if (val==0) return;
        method.append("{Flags.lflags.var2 = ");method.append(val);method.append(";");
        method.append("Flags.lflags.var1 = (");method.append(op2);method.append("() << 16) | ");method.append(l);method.append(";");
        method.append("long tempd=Flags.lflags.var1 >> ");method.append(val);method.append(";");
        if (val>16) {method.append("tempd |= (");method.append(op2);method.append("() << (32-");method.append(val);method.append("));");}
        method.append("Flags.lflags.res = tempd & 0xFFFF;");
        method.append("Flags.lflags.type=Flags.t_DSHRw;");
        method.append(s);
        method.append("}");
    }

    static public void DSHRW(StringBuffer method, String op2,String op3, String l, String s) {
        method.append("short val=(short)(");method.append(op3);method.append(" & 0x1F);");
        method.append("if (val!=0) {");
        method.append("Flags.lflags.var2 = val;");
        method.append("Flags.lflags.var1 = (");method.append(op2);method.append("() << 16) | ");method.append(l);method.append(";");
        method.append("long tempd=Flags.lflags.var1 >> val;");
        method.append("if (val>16) tempd |= (");method.append(op2);method.append("() << (32-val ));");
        method.append("Flags.lflags.res = tempd & 0xFFFF;");
        method.append("Flags.lflags.type=Flags.t_DSHRw;");
        method.append(s);
        method.append("}");
    }

    static public void Grp8EdIb(StringBuffer method) {
        method.append("Flags.FillFlags();");
        int rm=decode_fetchb();
        if (rm >= 0xc0 ) {
            long mask=1 << (decode_fetchb() & 31);
            //Reg eard = Modrm.GetEArd[rm];
            method.append("CPU_Regs.SETFLAGBIT(CPU_Regs.CF,(");method.append(geteard(rm));method.append("() & ");method.append(mask);method.append(")!=0);");
            switch (rm & 0x38) {
            case 0x20:											/* BT */
                break;
            case 0x28:											/* BTS */
                method.append(geteard(rm));method.append("(");method.append(geteard(rm));method.append("()|");method.append(mask);method.append(");");
                break;
            case 0x30:											/* BTR */
                method.append(geteard(rm));method.append("(");method.append(geteard(rm));method.append("()&~");method.append(mask);method.append(");");
                break;
            case 0x38:											/* BTC */
                method.append("if (CPU_Regs.GETFLAG(CPU_Regs.CF)!=0)");
                method.append(geteard(rm));method.append("(");method.append(geteard(rm));method.append("()&~");method.append(mask);method.append(");");
                method.append("else ");
                method.append(geteard(rm));method.append("(");method.append(geteard(rm));method.append("()|");method.append(mask);method.append(");");
                break;
            default:
                Log.exit("CPU:66:0F:BA:Illegal subfunction "+Integer.toString(rm & 0x38,16));
            }
        } else {
            method.append("long eaa = ");getEaa(method, rm);
            method.append("long old=Memory.mem_readd(eaa);");
            long mask=1 << (decode_fetchb() & 31);
            method.append("CPU_Regs.SETFLAGBIT(CPU_Regs.CF,(old & ");method.append(mask);method.append(")!=0);");
            switch (rm & 0x38) {
            case 0x20:											/* BT */
                break;
            case 0x28:											/* BTS */
                method.append("Memory.mem_writed(eaa,old|");method.append(mask);method.append(");");
                break;
            case 0x30:											/* BTR */
                method.append("Memory.mem_writed(eaa,old & ~");method.append(mask);method.append(");");
                break;
            case 0x38:											/* BTC */
                method.append("if (CPU_Regs.GETFLAG(CPU_Regs.CF)!=0) old&=~");method.append(mask);method.append(";");
                method.append("else old|=");method.append(mask);method.append(";");
                method.append("Memory.mem_writed(eaa,old);");
                break;
            default:
                Log.exit("CPU:66:0F:BA:Illegal subfunction "+Integer.toString(rm & 0x38,16));
            }
        }
    }

    static public void Grp8EwIb(StringBuffer method) {
        method.append("Flags.FillFlags();");
        int rm=decode_fetchb();
        if (rm >= 0xc0 ) {
            int mask=1 << (decode_fetchb() & 31);
            method.append("CPU_Regs.SETFLAGBIT(CPU_Regs.CF,(");method.append(getearw(rm));method.append("() & ");method.append(mask);method.append(")!=0);");
            switch (rm & 0x38) {
            case 0x20:											/* BT */
                break;
            case 0x28:											/* BTS */
                method.append(getearw(rm));method.append("(");method.append(getearw(rm));method.append("()|");method.append(mask);method.append(");");
                break;
            case 0x30:											/* BTR */
                method.append(getearw(rm));method.append("(");method.append(getearw(rm));method.append("()&~");method.append(mask);method.append(");");
                break;
            case 0x38:											/* BTC */
                method.append("if (CPU_Regs.GETFLAG(CPU_Regs.CF)!=0)");
                method.append(getearw(rm));method.append("(");method.append(getearw(rm));method.append("()&~");method.append(mask);method.append(");");
                method.append("else ");
                method.append(getearw(rm));method.append("(");method.append(getearw(rm));method.append("()|");method.append(mask);method.append(");");
                break;
            default:
                Log.exit("CPU:66:0F:BA:Illegal subfunction "+Integer.toString(rm & 0x38,16));
            }
        } else {
            method.append("{long eaa = ");getEaa(method, rm);
            method.append("int old=Memory.mem_readw(eaa);");
            int mask=1 << (decode_fetchb() & 31);
            method.append("CPU_Regs.SETFLAGBIT(CPU_Regs.CF,(old & ");method.append(mask);method.append(")!=0);");
            switch (rm & 0x38) {
            case 0x20:											/* BT */
                break;
            case 0x28:											/* BTS */
                method.append("Memory.mem_writew(eaa,old|");method.append(mask);method.append(");");
                break;
            case 0x30:											/* BTR */
                method.append("Memory.mem_writew(eaa,old & ~");method.append(mask);method.append(");");
                break;
            case 0x38:											/* BTC */
                method.append("if (CPU_Regs.GETFLAG(CPU_Regs.CF)!=0) old&=~");method.append(mask);method.append(";");
                method.append("else old|=");method.append(mask);method.append(";");
                method.append("Memory.mem_writew(eaa,old);");
                break;
            default:
                Log.exit("CPU:66:0F:BA:Illegal subfunction "+Integer.toString(rm & 0x38,16));
            }
            method.append("}");
        }
    }
    static protected void GRP2D_fetchb(StringBuffer method) {
        int rm = decode_fetchb();
        /*Bitu*/int which=(rm>>3)&7;
        String l;
        String s;
        int val;
        method.append("{");
        if (rm >= 0xc0) {
            short blah = decode_fetchb();
            val=(short)(blah & 0x1f);
            l = geteard(rm)+"()";
            s = geteard(rm)+"(Flags.lflags.res);";
        } else {
            method.append("long eaa = "); getEaa(method, rm);
            short blah = decode_fetchb();
            val=(short)(blah & 0x1f);
            l = "Memory.mem_readd(eaa)";
            s = "Memory.mem_writed(eaa, Flags.lflags.res);";
        }
        switch (which)	{
        case 0x00:ROLD(method, val,l, s);break;
        case 0x01:RORD(method, val,l, s);break;
        case 0x02:RCLD(method, val,l, s);break;
        case 0x03:RCRD(method, val,l, s);break;
        case 0x04:/* SHL and SAL are the same */
        case 0x06:SHLD(method, val,l, s);break;
        case 0x05:SHRD(method, val,l, s);break;
        case 0x07:SARD(method, val,l, s);break;
        }
        method.append("}");
    }

    protected static int grp6(StringBuffer method) {
        int rm = decode_fetchb();
        int which=(rm>>3)&7;
        method.append("if ((CPU_Regs.flags & CPU_Regs.VM)!=0 || (!CPU.cpu.pmode)) return Constants.BR_Illegal;");
        switch (which) {
        case 0x00:	/* SLDT */
        case 0x01:	/* STR */
            {
                method.append("{int saveval=");
                if (which==0) method.append("CPU.CPU_SLDT();");
                else method.append("CPU.CPU_STR();");
                if (rm >= 0xc0) {
                    method.append(getearw(rm));method.append("(saveval);");
                } else {
                    method.append("long eaa = ");getEaa(method, rm);
                    method.append("Memory.mem_writew(eaa,saveval);");
                }
                method.append("}");
            }
            break;
        case 0x02:case 0x03:case 0x04:case 0x05:
            {
                /* Just use 16-bit loads since were only using selectors */
                if (rm >= 0xc0 ) {
                    method.append("{int loadval=");method.append(getearw(rm));method.append("();");
                } else {
                    method.append("{long eaa = ");getEaa(method, rm);
                    method.append("int loadval=Memory.mem_readw(eaa);");
                }
                switch (which) {
                case 0x02:
                    method.append("if (CPU.cpu.cpl!=0) ");EXCEPTION(method, CPU.EXCEPTION_GP);
                    method.append("if (CPU.CPU_LLDT(loadval))");RUNEXCEPTION(method);
                    break;
                case 0x03:
                    method.append("if (CPU.cpu.cpl!=0)");EXCEPTION(method, CPU.EXCEPTION_GP);
                    method.append("if (CPU.CPU_LTR(loadval))");RUNEXCEPTION(method);
                    break;
                case 0x04:
                    method.append("CPU.CPU_VERR(loadval);");
                    break;
                case 0x05:
                    method.append("CPU.CPU_VERW(loadval);");
                    break;
                }
                method.append("}");
            }
            break;
        default:
            if (Log.level<=LogSeverities.LOG_ERROR) Log.log(LogTypes.LOG_CPU, LogSeverities.LOG_ERROR,"GRP6:Illegal call "+Integer.toString(which,16));
            return RESULT_ILLEGAL_INSTRUCTION;
        }
        return 0;
    }

    protected static int grp7ed(StringBuffer method) {
        int rm = decode_fetchb();
        int which=(rm>>3)&7;
        if (rm < 0xc0)	{ //First ones all use EA
            method.append("{long eaa = ");getEaa(method, rm);
            switch (which) {
            case 0x00:										/* SGDT */
                method.append("Memory.mem_writew(eaa,CPU.CPU_SGDT_limit());");
                method.append("Memory.mem_writed(eaa+2,CPU.CPU_SGDT_base());");
                break;
            case 0x01:										/* SIDT */
                method.append("Memory.mem_writew(eaa,CPU.CPU_SIDT_limit());");
                method.append("Memory.mem_writed(eaa+2,CPU.CPU_SIDT_base());");
                break;
            case 0x02:										/* LGDT */
                method.append("if (CPU.cpu.pmode && CPU.cpu.cpl!=0)");EXCEPTION(method, CPU.EXCEPTION_GP);
                method.append("CPU.CPU_LGDT(Memory.mem_readw(eaa),Memory.mem_readd(eaa+2));");
                break;
            case 0x03:										/* LIDT */
                method.append("if (CPU.cpu.pmode && CPU.cpu.cpl!=0)");EXCEPTION(method, CPU.EXCEPTION_GP);
                method.append("CPU.CPU_LIDT(Memory.mem_readw(eaa),Memory.mem_readd(eaa+2));");
                break;
            case 0x04:										/* SMSW */
                method.append("Memory.mem_writew(eaa,(int)(CPU.CPU_SMSW() & 0xFFFFl));");
                break;
            case 0x06:										/* LMSW */
                method.append("int limit=Memory.mem_readw(eaa);");
                method.append("if (CPU.CPU_LMSW(limit))");RUNEXCEPTION(method);
                break;
            case 0x07:										/* INVLPG */
                method.append("if (CPU.cpu.pmode && CPU.cpu.cpl!=0)");EXCEPTION(method, CPU.EXCEPTION_GP);
                method.append("Paging.PAGING_ClearTLB();");
                break;
            }
            method.append("}");
        } else {
            switch (which) {
            case 0x02:										/* LGDT */
                method.append("if (CPU.cpu.pmode && CPU.cpu.cpl!=0)");EXCEPTION(method, CPU.EXCEPTION_GP);
                method.append("return Constants.BR_Illegal;");
            case 0x03:										/* LIDT */
                method.append("if (CPU.cpu.pmode && CPU.cpu.cpl!=0)");EXCEPTION(method, CPU.EXCEPTION_GP);
                method.append("return Constants.BR_Illegal;");
            case 0x04:										/* SMSW */
                method.append(geteard(rm));method.append("(CPU.CPU_SMSW());");
                break;
            case 0x06:										/* LMSW */
                method.append("if (CPU.CPU_LMSW((int)");method.append(geteard(rm));method.append("()))");RUNEXCEPTION(method);
                break;
            default:
                if (Log.level<=LogSeverities.LOG_ERROR) Log.log(LogTypes.LOG_CPU,LogSeverities.LOG_ERROR,"Illegal group 7 RM subfunction "+which);
                return RESULT_ILLEGAL_INSTRUCTION;
            }
        }
        return 0;
    }

    protected static int grp5ed(StringBuffer method) {
        int result = 0;
        int rm = decode_fetchb();
        int which=(rm>>3)&7;
        switch (which) {
        case 0x00:											/* INC Ed */
            ed(method, rm, "Instructions.INCD");
            break;
        case 0x01:											/* DEC Ed */
            ed(method, rm, "Instructions.DECD");
            break;
        case 0x02:											/* CALL NEAR Ed */
            method.append("{long eip = CPU_Regs.reg_eip;");
            if (rm >= 0xc0 ) {
                method.append("CPU_Regs.reg_eip(");method.append(geteard(rm));method.append("());");
            } else {
                method.append("long eaa=");getEaa(method,rm);
                method.append("CPU_Regs.reg_eip(Memory.mem_readd(eaa));");
            }
            method.append("eip += ");method.append(decode.code-decode.code_start);method.append(";");
            method.append("CPU.CPU_Push32(eip);}");
            result = RESULT_CONTINUE;
            break;
        case 0x03:											/* CALL FAR Ed */
            {
                if (rm >= 0xc0) return RESULT_ILLEGAL_INSTRUCTION;
                method.append("{long eaa=");getEaa(method, rm);
                method.append("long newip=Memory.mem_readd(eaa);");
                method.append("int newcs=Memory.mem_readw(eaa+4);");
                method.append("Flags.FillFlags();");
                GETIP(method);
                method.append("CPU.CPU_CALL(true,newcs,newip,eip);");
                if (CPU_TRAP_CHECK) {
                    method.append("if (CPU_Regs.GETFLAG(CPU_Regs.TF)!=0) {");
                    returnTrap(method);
                    method.append("}");
                }
                method.append("}");
                result = RESULT_CONTINUE;
                break;
            }
        case 0x04:											/* JMP NEAR Ed */
            if (rm >= 0xc0 ) {
                method.append("CPU_Regs.reg_eip(");method.append(geteard(rm));method.append("());");
            } else {
                method.append("long eaa=");getEaa(method, rm);
                method.append("CPU_Regs.reg_eip(Memory.mem_readd(eaa));");
            }
            result = RESULT_CONTINUE;
            break;
        case 0x05:											/* JMP FAR Ed */
            {
                if (rm >= 0xc0) return RESULT_ILLEGAL_INSTRUCTION;
                method.append("{long eaa=");getEaa(method, rm);
                method.append("long newip=Memory.mem_readd(eaa);");
                method.append("int newcs=Memory.mem_readw(eaa+4);");
                method.append("Flags.FillFlags();");
                GETIP(method);
                method.append("CPU.CPU_JMP(true,newcs,(int)newip,eip);");
                 if (CPU_TRAP_CHECK) {
                    method.append("if (CPU_Regs.GETFLAG(CPU_Regs.TF)!=0) {");
                     returnTrap(method);
                    method.append("}");
                }
                method.append("}");
                result = RESULT_CONTINUE;
                break;
            }
        case 0x06:											/* Push Ed */
            if (rm >= 0xc0 ) {
                method.append("CPU.CPU_Push32(");method.append(geteard(rm));method.append("());");
            } else {
                method.append("{long eaa=");getEaa(method, rm);
                method.append("CPU.CPU_Push32(Memory.mem_readd(eaa));}");
            }
            break;
        default:
            if (Log.level<=LogSeverities.LOG_ERROR) Log.log(LogTypes.LOG_CPU,LogSeverities.LOG_ERROR,"CPU:66:GRP5:Illegal call "+Integer.toString(which,16));
            return RESULT_ILLEGAL_INSTRUCTION;
        }
        return result;
    }

    protected static void GrplEd(StringBuffer method, boolean signed) {
        int rm = decode_fetchb();
        int which=(rm>>3)&7;
        if (rm >= 0xc0) {
            /*Bit32u*/long id;
            if (signed)
                id = ((int)decode_fetchbs()) & 0xFFFFFFFFl;
            else
                id = decode_fetchd();
            switch (which) {
                case 0x00: method.append(geteard(rm));method.append("(Instructions.ADDD(");method.append(id);method.append("l, ");method.append(geteard(rm));method.append("()));");break;
                case 0x01: method.append(geteard(rm));method.append("(Instructions.ORD(");method.append(id);method.append("l, ");method.append(geteard(rm));method.append("()));");break;
                case 0x02: method.append(geteard(rm));method.append("(Instructions.ADCD(");method.append(id);method.append("l, ");method.append(geteard(rm));method.append("()));");break;
                case 0x03: method.append(geteard(rm));method.append("(Instructions.SBBD(");method.append(id);method.append("l, ");method.append(geteard(rm));method.append("()));");break;
                case 0x04: method.append(geteard(rm));method.append("(Instructions.ANDD(");method.append(id);method.append("l, ");method.append(geteard(rm));method.append("()));");break;
                case 0x05: method.append(geteard(rm));method.append("(Instructions.SUBD(");method.append(id);method.append("l, ");method.append(geteard(rm));method.append("()));");break;
                case 0x06: method.append(geteard(rm));method.append("(Instructions.XORD(");method.append(id);method.append("l, ");method.append(geteard(rm));method.append("()));");break;
                case 0x07: method.append("Instructions.CMPD(");method.append(id);method.append("l, ");method.append(geteard(rm));method.append("());");break;
            }
        } else {
            method.append("{long eaa = ");getEaa(method, rm);

            /*Bit32u*/long id;
            if (signed)
                id = ((int)decode_fetchbs()) & 0xFFFFFFFFl;
            else
                id = decode_fetchd();
            method.append("if ((eaa & 0xFFF)<0xFFD) {");
                method.append("int index = Paging.getDirectIndex(eaa);");
                method.append("if (index>=0) {");
                    switch (which) {
                    case 0x00:method.append("Memory.host_writed(index, Instructions.ADDD(");method.append(id);method.append("l,Memory.host_readd(index)));");break;
                    case 0x01: method.append("Memory.host_writed(index, Instructions.ORD(");method.append(id);method.append("l,Memory.host_readd(index)));");break;
                    case 0x02:method.append("Memory.host_writed(index, Instructions.ADCD(");method.append(id);method.append("l,Memory.host_readd(index)));");break;
                    case 0x03:method.append("Memory.host_writed(index, Instructions.SBBD(");method.append(id);method.append("l,Memory.host_readd(index)));");break;
                    case 0x04:method.append("Memory.host_writed(index, Instructions.ANDD(");method.append(id);method.append("l,Memory.host_readd(index)));");break;
                    case 0x05:method.append("Memory.host_writed(index, Instructions.SUBD(");method.append(id);method.append("l,Memory.host_readd(index)));");break;
                    case 0x06:method.append("Memory.host_writed(index, Instructions.XORD(");method.append(id);method.append("l,Memory.host_readd(index)));");break;
                    case 0x07:method.append("Instructions.CMPD(");method.append(id);method.append("l,Memory.host_readd(index));");break;
                    }
                method.append("} else {");
                    switch (which) {
                    case 0x00:method.append("Memory.mem_writed(eaa, Instructions.ADDD(");method.append(id);method.append("l,Memory.mem_readd(eaa)));");break;
                    case 0x01: method.append("Memory.mem_writed(eaa, Instructions.ORD(");method.append(id);method.append("l,Memory.mem_readd(eaa)));");break;
                    case 0x02:method.append("Memory.mem_writed(eaa, Instructions.ADCD(");method.append(id);method.append("l,Memory.mem_readd(eaa)));");break;
                    case 0x03:method.append("Memory.mem_writed(eaa, Instructions.SBBD(");method.append(id);method.append("l,Memory.mem_readd(eaa)));");break;
                    case 0x04:method.append("Memory.mem_writed(eaa, Instructions.ANDD(");method.append(id);method.append("l,Memory.mem_readd(eaa)));");break;
                    case 0x05:method.append("Memory.mem_writed(eaa, Instructions.SUBD(");method.append(id);method.append("l,Memory.mem_readd(eaa)));");break;
                    case 0x06:method.append("Memory.mem_writed(eaa, Instructions.XORD(");method.append(id);method.append("l,Memory.mem_readd(eaa)));");break;
                    case 0x07:method.append("Instructions.CMPD(");method.append(id);method.append("l,Memory.mem_readd(eaa));");break;
                    }
                method.append("}");
            method.append("} else {");
                switch (which) {
                case 0x00:method.append("Memory.mem_writed(eaa, Instructions.ADDD(");method.append(id);method.append("l,Memory.mem_readd(eaa)));");break;
                case 0x01: method.append("Memory.mem_writed(eaa, Instructions.ORD(");method.append(id);method.append("l,Memory.mem_readd(eaa)));");break;
                case 0x02:method.append("Memory.mem_writed(eaa, Instructions.ADCD(");method.append(id);method.append("l,Memory.mem_readd(eaa)));");break;
                case 0x03:method.append("Memory.mem_writed(eaa, Instructions.SBBD(");method.append(id);method.append("l,Memory.mem_readd(eaa)));");break;
                case 0x04:method.append("Memory.mem_writed(eaa, Instructions.ANDD(");method.append(id);method.append("l,Memory.mem_readd(eaa)));");break;
                case 0x05:method.append("Memory.mem_writed(eaa, Instructions.SUBD(");method.append(id);method.append("l,Memory.mem_readd(eaa)));");break;
                case 0x06:method.append("Memory.mem_writed(eaa, Instructions.XORD(");method.append(id);method.append("l,Memory.mem_readd(eaa)));");break;
                case 0x07:method.append("Instructions.CMPD(");method.append(id);method.append("l,Memory.mem_readd(eaa));");break;
                }
            method.append("}}");
        }
    }

    protected static void exchanged(StringBuffer method, String r1, String r2) {
        method.append("{long temp = CPU_Regs.reg_");method.append(r1);method.append(".dword();");
        method.append("CPU_Regs.reg_");method.append(r1);method.append(".dword(CPU_Regs.reg_");method.append(r2);method.append(".dword());");
        method.append("CPU_Regs.reg_");method.append(r2);method.append(".dword(temp);}");
    }

    protected static void decodeEnd(StringBuffer method) {
        method.append("CPU.CPU_Cycles-=");method.append(decode.cycles);method.append(";");
        SAVEIP(method);
        Flags.FillFlags();
        method.append("return Constants.BR_CBRet_None;");
    }

    protected static void returnNone(StringBuffer method) {
        method.append("CPU.CPU_Cycles-=");method.append(decode.cycles);method.append(";");
        method.append("return Constants.BR_CBRet_None;");
    }

    protected static void returnLink1(StringBuffer method) {
        method.append("CPU.CPU_Cycles-=");method.append(decode.cycles);method.append(";");
        method.append("return Constants.BR_Link1;");
    }

    protected static void returnLink2(StringBuffer method) {
        method.append("CPU.CPU_Cycles-=");method.append(decode.cycles);method.append(";");
        method.append("return Constants.BR_Link2;");
    }


    protected static void returnNormal(StringBuffer method) {
        method.append("CPU.CPU_Cycles-=");method.append(decode.cycles);method.append(";");
        method.append("return Constants.BR_Normal;");
    }

    protected static void returnTrap(StringBuffer method) {
        method.append("CPU.cpudecoder=Core_dynrec2.CPU_Core_Dynrec_Trap_Run;");
        returnNone(method);
    }

    protected static void returnIllegal(StringBuffer method) {
        method.append("{CPU_Regs.reg_eip+=");method.append(decode.code-decode.op_start);method.append(";");
        method.append("CPU.CPU_Cycles-=");method.append(decode.cycles);method.append(";");
        method.append("return Constants.BR_Illegal;}");
    }
}
