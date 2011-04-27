package jdos.cpu;

public class Table_ea extends Core {
    //typedef PhysPt (*EA_LookupHandler)(void);
    static public interface GetEAHandler {
        public /*PhysPt*/long call();
    }
    static public boolean EA16 = true;

    static public long getEaa32(int rm) {
        if (rm<0x40) {
            switch (rm & 7) {
                case 0x00: return (base_ds+reg_eax.dword()) & 0xFFFFFFFFl;
                case 0x01: return (base_ds+reg_ecx.dword()) & 0xFFFFFFFFl;
                case 0x02: return (base_ds+reg_edx.dword()) & 0xFFFFFFFFl;
                case 0x03: return (base_ds+reg_ebx.dword()) & 0xFFFFFFFFl;
                case 0x04: return Sib(0);
                case 0x05: return (base_ds+Fetchd.call()) & 0xFFFFFFFFl;
                case 0x06: return (base_ds+reg_esi.dword()) & 0xFFFFFFFFl;
                case 0x07: return (base_ds+reg_edi.dword()) & 0xFFFFFFFFl;
            }
        } else if (rm<0x80) {
            switch (rm & 7) {
                case 0x00: return (base_ds+reg_eax.dword()+Fetchbs.call()) & 0xFFFFFFFFl;
                case 0x01: return (base_ds+reg_ecx.dword()+Fetchbs.call()) & 0xFFFFFFFFl;
                case 0x02: return (base_ds+reg_edx.dword()+Fetchbs.call()) & 0xFFFFFFFFl;
                case 0x03: return (base_ds+reg_ebx.dword()+Fetchbs.call()) & 0xFFFFFFFFl;
                case 0x04: long temp = Sib(1); return (temp+Fetchbs.call()) & 0xFFFFFFFFl;
                case 0x05: return (base_ss+reg_ebp.dword()+Fetchbs.call()) & 0xFFFFFFFFl;
                case 0x06: return (base_ds+reg_esi.dword()+Fetchbs.call()) & 0xFFFFFFFFl;
                case 0x07: return (base_ds+reg_edi.dword()+Fetchbs.call()) & 0xFFFFFFFFl;
            }
        } else {
            switch (rm & 7) {
                case 0x00: return (base_ds+reg_eax.dword()+Fetchds.call()) & 0xFFFFFFFFl;
                case 0x01: return (base_ds+reg_ecx.dword()+Fetchds.call()) & 0xFFFFFFFFl;
                case 0x02: return (base_ds+reg_edx.dword()+Fetchds.call()) & 0xFFFFFFFFl;
                case 0x03: return (base_ds+reg_ebx.dword()+Fetchds.call()) & 0xFFFFFFFFl;
                case 0x04: long temp = Sib(2); return (temp+Fetchds.call()) & 0xFFFFFFFFl;
                case 0x05: return (base_ss+reg_ebp.dword()+Fetchds.call()) & 0xFFFFFFFFl;
                case 0x06: return (base_ds+reg_esi.dword()+Fetchds.call()) & 0xFFFFFFFFl;
                case 0x07: return (base_ds+reg_edi.dword()+Fetchds.call()) & 0xFFFFFFFFl;
            }
        }
        return 0;
    }

    static public long getEaa16(int rm) {
        if (rm<0x40) {
            switch (rm & 7) {
                case 0x00: return base_ds+((reg_ebx.word()+(/*Bit16s*/short)reg_esi.word()) & 0xFFFF);
                case 0x01: return base_ds+((reg_ebx.word()+(/*Bit16s*/short)reg_edi.word()) & 0xFFFF);
                case 0x02: return base_ss+((reg_ebp.word()+(/*Bit16s*/short)reg_esi.word()) & 0xFFFF);
                case 0x03: return base_ss+((reg_ebp.word()+(/*Bit16s*/short)reg_edi.word()) & 0xFFFF);
                case 0x04: return base_ds+(reg_esi.word());
                case 0x05: return base_ds+(reg_edi.word());
                case 0x06: return base_ds+(Fetchw.call());
                case 0x07: return base_ds+(reg_ebx.word());
            }
        } else if (rm<0x80) {
            switch (rm & 7) {
                case 0x00: return base_ds+((reg_ebx.word()+(/*Bit16s*/short)reg_esi.word()+Fetchbs.call()) & 0xFFFF);
                case 0x01: return base_ds+((reg_ebx.word()+(/*Bit16s*/short)reg_edi.word()+Fetchbs.call()) & 0xFFFF);
                case 0x02: return base_ss+((reg_ebp.word()+(/*Bit16s*/short)reg_esi.word()+Fetchbs.call()) & 0xFFFF);
                case 0x03: return base_ss+((reg_ebp.word()+(/*Bit16s*/short)reg_edi.word()+Fetchbs.call()) & 0xFFFF);
                case 0x04: return base_ds+((reg_esi.word()+Fetchbs.call()) & 0xFFFF);
                case 0x05: return base_ds+((reg_edi.word()+Fetchbs.call()) & 0xFFFF);
                case 0x06: return base_ss+((reg_ebp.word()+Fetchbs.call()) & 0xFFFF);
                case 0x07: return base_ds+((reg_ebx.word()+Fetchbs.call()) & 0xFFFF);
            }
        } else {
            switch (rm & 7) {
                case 0x00: return base_ds+((reg_ebx.word()+(/*Bit16s*/short)reg_esi.word()+Fetchws.call()) & 0xFFFF);
                case 0x01: return base_ds+((reg_ebx.word()+(/*Bit16s*/short)reg_edi.word()+Fetchws.call()) & 0xFFFF);
                case 0x02: return base_ss+((reg_ebp.word()+(/*Bit16s*/short)reg_esi.word()+Fetchws.call()) & 0xFFFF);
                case 0x03: return base_ss+((reg_ebp.word()+(/*Bit16s*/short)reg_edi.word()+Fetchws.call()) & 0xFFFF);
                case 0x04: return base_ds+((reg_esi.word()+Fetchws.call()) & 0xFFFF);
                case 0x05: return base_ds+((reg_edi.word()+Fetchws.call()) & 0xFFFF);
                case 0x06: return base_ss+((reg_ebp.word()+Fetchws.call()) & 0xFFFF);
                case 0x07: return base_ds+((reg_ebx.word()+Fetchws.call()) & 0xFFFF);
            }
        }
        return 0;
    }

    static public long getEaa(int rm) {
        if (EA16) return getEaa16(rm);
        return getEaa32(rm);            
    }

    static final /*Bit32u*/long SIBZero=0;

    static private /*PhysPt*/long Sib(/*Bitu*/int mode) {
        /*Bit8u*/short sib=Fetchb.call();
        /*PhysPt*/long base=0;
        switch (sib&7) {
        case 0:	/* EAX Base */
            base=base_ds+reg_eax.dword();break;
        case 1:	/* ECX Base */
            base=base_ds+reg_ecx.dword();break;
        case 2:	/* EDX Base */
            base=base_ds+reg_edx.dword();break;
        case 3:	/* EBX Base */
            base=base_ds+reg_ebx.dword();break;
        case 4:	/* ESP Base */
            base=base_ss+reg_esp.dword();break;
        case 5:	/* #1 Base */
            if (mode==0) {
                base=base_ds+Fetchd.call();break;
            } else {
                base=base_ss+reg_ebp.dword();break;
            }
        case 6:	/* ESI Base */
            base=base_ds+reg_esi.dword();break;
        case 7:	/* EDI Base */
            base=base_ds+reg_edi.dword();break;
        }
        int index =(sib >> 3) & 7;
        switch (index) {
            case 0:
                base+=reg_eax.dword() << (sib >> 6);
                break;
            case 1:
                base+=reg_ecx.dword() << (sib >> 6);
                break;
            case 2:
                base+=reg_edx.dword() << (sib >> 6);
                break;
            case 3:
                base+=reg_ebx.dword() << (sib >> 6);
                break;
            case 4:
                base+=SIBZero << (sib >> 6);
                break;
            case 5:
                base+=reg_ebp.dword() << (sib >> 6);
                break;
            case 6:
                base+=reg_esi.dword() << (sib >> 6);
                break;
            case 7:
                base+=reg_edi.dword() << (sib >> 6);
                break;
        }
        return base & 0xFFFFFFFFl;
    }

    protected static long GetEADirect() {
        if ((prefixes & PREFIX_ADDR)!=0) {
            return (base_ds+Fetchd.call() & 0xFFFFFFFFl);
        } else {
            return (base_ds+Fetchw.call() & 0xFFFFFFFFl);
        }
    }
}
