package jdos.cpu;

public class Table_ea extends Core {
    //typedef PhysPt (*EA_LookupHandler)(void);
    static public interface GetEAHandler {
        public /*PhysPt*/int call();
    }
    static public boolean EA16 = true;

    static public int getEaa32(int rm) {
        if (rm<0x40) {
            switch (rm & 7) {
                case 0x00: return (base_ds+reg_eax.dword);
                case 0x01: return (base_ds+reg_ecx.dword);
                case 0x02: return (base_ds+reg_edx.dword);
                case 0x03: return (base_ds+reg_ebx.dword);
                case 0x04: return Sib(0);
                case 0x05: return (base_ds+ Fetchd());
                case 0x06: return (base_ds+reg_esi.dword);
                case 0x07: return (base_ds+reg_edi.dword);
            }
        } else if (rm<0x80) {
            switch (rm & 7) {
                case 0x00: return (base_ds+reg_eax.dword+Fetchbs());
                case 0x01: return (base_ds+reg_ecx.dword+Fetchbs());
                case 0x02: return (base_ds+reg_edx.dword+Fetchbs());
                case 0x03: return (base_ds+reg_ebx.dword+Fetchbs());
                case 0x04: int temp = Sib(1); return (temp+Fetchbs());
                case 0x05: return (base_ss+reg_ebp.dword+Fetchbs());
                case 0x06: return (base_ds+reg_esi.dword+Fetchbs());
                case 0x07: return (base_ds+reg_edi.dword+Fetchbs());
            }
        } else {
            switch (rm & 7) {
                case 0x00: return (base_ds+reg_eax.dword+Fetchds());
                case 0x01: return (base_ds+reg_ecx.dword+Fetchds());
                case 0x02: return (base_ds+reg_edx.dword+Fetchds());
                case 0x03: return (base_ds+reg_ebx.dword+Fetchds());
                case 0x04: int temp = Sib(2); return (temp+Fetchds());
                case 0x05: return (base_ss+reg_ebp.dword+Fetchds());
                case 0x06: return (base_ds+reg_esi.dword+Fetchds());
                case 0x07: return (base_ds+reg_edi.dword+Fetchds());
            }
        }
        return 0;
    }

    static public int getEaa16(int rm) {
        if (rm<0x40) {
            switch (rm & 7) {
                case 0x00: return base_ds+((reg_ebx.word()+(/*Bit16s*/short)reg_esi.word()) & 0xFFFF);
                case 0x01: return base_ds+((reg_ebx.word()+(/*Bit16s*/short)reg_edi.word()) & 0xFFFF);
                case 0x02: return base_ss+((reg_ebp.word()+(/*Bit16s*/short)reg_esi.word()) & 0xFFFF);
                case 0x03: return base_ss+((reg_ebp.word()+(/*Bit16s*/short)reg_edi.word()) & 0xFFFF);
                case 0x04: return base_ds+(reg_esi.word());
                case 0x05: return base_ds+(reg_edi.word());
                case 0x06: return base_ds+(Fetchw());
                case 0x07: return base_ds+(reg_ebx.word());
            }
        } else if (rm<0x80) {
            switch (rm & 7) {
                case 0x00: return base_ds+((reg_ebx.word()+(/*Bit16s*/short)reg_esi.word()+Fetchbs()) & 0xFFFF);
                case 0x01: return base_ds+((reg_ebx.word()+(/*Bit16s*/short)reg_edi.word()+Fetchbs()) & 0xFFFF);
                case 0x02: return base_ss+((reg_ebp.word()+(/*Bit16s*/short)reg_esi.word()+Fetchbs()) & 0xFFFF);
                case 0x03: return base_ss+((reg_ebp.word()+(/*Bit16s*/short)reg_edi.word()+Fetchbs()) & 0xFFFF);
                case 0x04: return base_ds+((reg_esi.word()+Fetchbs()) & 0xFFFF);
                case 0x05: return base_ds+((reg_edi.word()+Fetchbs()) & 0xFFFF);
                case 0x06: return base_ss+((reg_ebp.word()+Fetchbs()) & 0xFFFF);
                case 0x07: return base_ds+((reg_ebx.word()+Fetchbs()) & 0xFFFF);
            }
        } else {
            switch (rm & 7) {
                case 0x00: return base_ds+((reg_ebx.word()+(/*Bit16s*/short)reg_esi.word()+Fetchws()) & 0xFFFF);
                case 0x01: return base_ds+((reg_ebx.word()+(/*Bit16s*/short)reg_edi.word()+Fetchws()) & 0xFFFF);
                case 0x02: return base_ss+((reg_ebp.word()+(/*Bit16s*/short)reg_esi.word()+Fetchws()) & 0xFFFF);
                case 0x03: return base_ss+((reg_ebp.word()+(/*Bit16s*/short)reg_edi.word()+Fetchws()) & 0xFFFF);
                case 0x04: return base_ds+((reg_esi.word()+Fetchws()) & 0xFFFF);
                case 0x05: return base_ds+((reg_edi.word()+Fetchws()) & 0xFFFF);
                case 0x06: return base_ss+((reg_ebp.word()+Fetchws()) & 0xFFFF);
                case 0x07: return base_ds+((reg_ebx.word()+Fetchws()) & 0xFFFF);
            }
        }
        return 0;
    }

    static public int getEaa(int rm) {
        if (EA16) return getEaa16(rm);
        return getEaa32(rm);            
    }

    static final /*Bit32u*/long SIBZero=0;

    static private /*PhysPt*/int Sib(/*Bitu*/int mode) {
        /*Bit8u*/int sib=Fetchb();
        /*PhysPt*/int base=0;
        switch (sib&7) {
        case 0:	/* EAX Base */
            base=base_ds+reg_eax.dword;break;
        case 1:	/* ECX Base */
            base=base_ds+reg_ecx.dword;break;
        case 2:	/* EDX Base */
            base=base_ds+reg_edx.dword;break;
        case 3:	/* EBX Base */
            base=base_ds+reg_ebx.dword;break;
        case 4:	/* ESP Base */
            base=base_ss+reg_esp.dword;break;
        case 5:	/* #1 Base */
            if (mode==0) {
                base=base_ds+ Fetchd();break;
            } else {
                base=base_ss+reg_ebp.dword;break;
            }
        case 6:	/* ESI Base */
            base=base_ds+reg_esi.dword;break;
        case 7:	/* EDI Base */
            base=base_ds+reg_edi.dword;break;
        }
        int index =(sib >> 3) & 7;
        switch (index) {
            case 0:
                base+=reg_eax.dword << (sib >> 6);
                break;
            case 1:
                base+=reg_ecx.dword << (sib >> 6);
                break;
            case 2:
                base+=reg_edx.dword << (sib >> 6);
                break;
            case 3:
                base+=reg_ebx.dword << (sib >> 6);
                break;
            case 4:
                base+=SIBZero << (sib >> 6);
                break;
            case 5:
                base+=reg_ebp.dword << (sib >> 6);
                break;
            case 6:
                base+=reg_esi.dword << (sib >> 6);
                break;
            case 7:
                base+=reg_edi.dword << (sib >> 6);
                break;
        }
        return base;
    }

    protected static int GetEADirect() {
        if ((prefixes & PREFIX_ADDR)!=0) {
            return (base_ds+ Fetchd());
        } else {
            return (base_ds+Fetchw());
        }
    }
}
