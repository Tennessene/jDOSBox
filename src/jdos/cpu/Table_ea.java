package jdos.cpu;

public class Table_ea extends Core {
    //typedef PhysPt (*EA_LookupHandler)(void);
    static public interface GetEAHandler {
        public /*PhysPt*/long call();
    }
    static public boolean EA16 = true;

    static public long  EA_00_n() {
        if (EA16)
            return base_ds+((reg_ebx.word()+(/*Bit16s*/short)reg_esi.word()) & 0xFFFF);
        return (base_ds+reg_eax.dword()) & 0xFFFFFFFFl;
    }
    static public long  EA_01_n() {
        if (EA16)
            return base_ds+((reg_ebx.word()+(/*Bit16s*/short)reg_edi.word()) & 0xFFFF);
        return (base_ds+reg_ecx.dword()) & 0xFFFFFFFFl;
    }
    static public long  EA_02_n() {
        if (EA16)
            return base_ss+((reg_ebp.word()+(/*Bit16s*/short)reg_esi.word()) & 0xFFFF);
        return (base_ds+reg_edx.dword()) & 0xFFFFFFFFl;
    }
    static public long  EA_03_n() {
        if (EA16)
            return base_ss+((reg_ebp.word()+(/*Bit16s*/short)reg_edi.word()) & 0xFFFF);
        return (base_ds+reg_ebx.dword()) & 0xFFFFFFFFl;
    }
    static public long  EA_04_n() {
        if (EA16)
            return base_ds+(reg_esi.word());
        return Sib(0);
    }
    static public long  EA_05_n() {
        if (EA16)
            return base_ds+(reg_edi.word());
        return (base_ds+Fetchd.call()) & 0xFFFFFFFFl;
    }
    static public long  EA_06_n() {
        if (EA16)
            return base_ds+(Fetchw.call());
        return (base_ds+reg_esi.dword()) & 0xFFFFFFFFl;
    }
    static public long  EA_07_n() {
        if (EA16)
            return base_ds+(reg_ebx.word());
        return (base_ds+reg_edi.dword()) & 0xFFFFFFFFl;
    }

    static public long  EA_40_n() {
        if (EA16)
            return base_ds+((reg_ebx.word()+(/*Bit16s*/short)reg_esi.word()+Fetchbs.call()) & 0xFFFF);
        return (base_ds+reg_eax.dword()+Fetchbs.call()) & 0xFFFFFFFFl;
    }
    static public long  EA_41_n() {
        if (EA16)
            return base_ds+((reg_ebx.word()+(/*Bit16s*/short)reg_edi.word()+Fetchbs.call()) & 0xFFFF);
        return (base_ds+reg_ecx.dword()+Fetchbs.call()) & 0xFFFFFFFFl;
    }
    static public long  EA_42_n() {
        if (EA16)
            return base_ss+((reg_ebp.word()+(/*Bit16s*/short)reg_esi.word()+Fetchbs.call()) & 0xFFFF);
        return (base_ds+reg_edx.dword()+Fetchbs.call()) & 0xFFFFFFFFl;
    }
    static public long  EA_43_n() {
        if (EA16)
            return base_ss+((reg_ebp.word()+(/*Bit16s*/short)reg_edi.word()+Fetchbs.call()) & 0xFFFF);
        return (base_ds+reg_ebx.dword()+Fetchbs.call()) & 0xFFFFFFFFl;
    }
    static public long  EA_44_n() {
        if (EA16)
            return base_ds+((reg_esi.word()+Fetchbs.call()) & 0xFFFF);
        return (Sib(1)+Fetchbs.call()) & 0xFFFFFFFFl;
    }
    static public long  EA_45_n() {
        if (EA16)
            return base_ds+((reg_edi.word()+Fetchbs.call()) & 0xFFFF);
        return (base_ss+reg_ebp.dword()+Fetchbs.call()) & 0xFFFFFFFFl;
    }
    static public long  EA_46_n() {
        if (EA16)
            return base_ss+((reg_ebp.word()+Fetchbs.call()) & 0xFFFF);
        return (base_ds+reg_esi.dword()+Fetchbs.call()) & 0xFFFFFFFFl;
    }
    static public long  EA_47_n() {
        if (EA16)
            return base_ds+((reg_ebx.word()+Fetchbs.call()) & 0xFFFF);
        return (base_ds+reg_edi.dword()+Fetchbs.call()) & 0xFFFFFFFFl;
    }

    static public long  EA_80_n() {
        if (EA16)
            return base_ds+((reg_ebx.word()+(/*Bit16s*/short)reg_esi.word()+Fetchws.call()) & 0xFFFF);
        return (base_ds+reg_eax.dword()+Fetchds.call()) & 0xFFFFFFFFl;
    }
    static public long  EA_81_n() {
        if (EA16)
            return base_ds+((reg_ebx.word()+(/*Bit16s*/short)reg_edi.word()+Fetchws.call()) & 0xFFFF);
        return (base_ds+reg_ecx.dword()+Fetchds.call()) & 0xFFFFFFFFl;
    }
    static public long  EA_82_n() {
        if (EA16)
            return base_ss+((reg_ebp.word()+(/*Bit16s*/short)reg_esi.word()+Fetchws.call()) & 0xFFFF);
        return (base_ds+reg_edx.dword()+Fetchds.call()) & 0xFFFFFFFFl;
    }
    static public long  EA_83_n() {
        if (EA16)
            return base_ss+((reg_ebp.word()+(/*Bit16s*/short)reg_edi.word()+Fetchws.call()) & 0xFFFF);
        return (base_ds+reg_ebx.dword()+Fetchds.call()) & 0xFFFFFFFFl;

    }
    static public long  EA_84_n() {
        if (EA16)
            return base_ds+((reg_esi.word()+Fetchws.call()) & 0xFFFF);
        return (Sib(2)+Fetchds.call()) & 0xFFFFFFFFl;
    }
    static public long  EA_85_n() {
        if (EA16)
            return base_ds+((reg_edi.word()+Fetchws.call()) & 0xFFFF);
        return (base_ss+reg_ebp.dword()+Fetchds.call()) & 0xFFFFFFFFl;
    }
    static public long  EA_86_n() {
        if (EA16)
            return base_ss+((reg_ebp.word()+Fetchws.call()) & 0xFFFF);
        return (base_ds+reg_esi.dword()+Fetchds.call()) & 0xFFFFFFFFl;
    }
    static public long  EA_87_n() {
        if (EA16)
            return base_ds+((reg_ebx.word()+Fetchws.call()) & 0xFFFF);
        return (base_ds+reg_edi.dword()+Fetchds.call()) & 0xFFFFFFFFl;
    }
/* The MOD/RM Decoder for EA for this decoder's addressing modes */
    final static private GetEAHandler  EA_16_00_n= new GetEAHandler() { public /*PhysPt*/long call() { return base_ds+((reg_ebx.word()+(/*Bit16s*/short)reg_esi.word()) & 0xFFFF); } };
    final static private GetEAHandler  EA_16_01_n= new GetEAHandler() { public /*PhysPt*/long call() { return base_ds+((reg_ebx.word()+(/*Bit16s*/short)reg_edi.word()) & 0xFFFF); } };
    final static private GetEAHandler  EA_16_02_n= new GetEAHandler() { public /*PhysPt*/long call() { return base_ss+((reg_ebp.word()+(/*Bit16s*/short)reg_esi.word()) & 0xFFFF); } };
    final static private GetEAHandler  EA_16_03_n= new GetEAHandler() { public /*PhysPt*/long call() { return base_ss+((reg_ebp.word()+(/*Bit16s*/short)reg_edi.word()) & 0xFFFF); } };
    final static private GetEAHandler  EA_16_04_n= new GetEAHandler() { public /*PhysPt*/long call() { return base_ds+(reg_esi.word()); } };
    final static private GetEAHandler  EA_16_05_n= new GetEAHandler() { public /*PhysPt*/long call() { return base_ds+(reg_edi.word()); } };
    final static private GetEAHandler  EA_16_06_n= new GetEAHandler() { public /*PhysPt*/long call() { return base_ds+(Fetchw.call());}};
    final static private GetEAHandler  EA_16_07_n= new GetEAHandler() { public /*PhysPt*/long call() { return base_ds+(reg_ebx.word()); } };

    final static private GetEAHandler  EA_16_40_n= new GetEAHandler() { public /*PhysPt*/long call() { return base_ds+((reg_ebx.word()+(/*Bit16s*/short)reg_esi.word()+Fetchbs.call()) & 0xFFFF); } };
    final static private GetEAHandler  EA_16_41_n= new GetEAHandler() { public /*PhysPt*/long call() { return base_ds+((reg_ebx.word()+(/*Bit16s*/short)reg_edi.word()+Fetchbs.call()) & 0xFFFF); } };
    final static private GetEAHandler  EA_16_42_n= new GetEAHandler() { public /*PhysPt*/long call() { return base_ss+((reg_ebp.word()+(/*Bit16s*/short)reg_esi.word()+Fetchbs.call()) & 0xFFFF); } };
    final static private GetEAHandler  EA_16_43_n= new GetEAHandler() { public /*PhysPt*/long call() { return base_ss+((reg_ebp.word()+(/*Bit16s*/short)reg_edi.word()+Fetchbs.call()) & 0xFFFF); } };
    final static private GetEAHandler  EA_16_44_n= new GetEAHandler() { public /*PhysPt*/long call() { return base_ds+((reg_esi.word()+Fetchbs.call()) & 0xFFFF); } };
    final static private GetEAHandler  EA_16_45_n= new GetEAHandler() { public /*PhysPt*/long call() { return base_ds+((reg_edi.word()+Fetchbs.call()) & 0xFFFF); } };
    final static private GetEAHandler  EA_16_46_n= new GetEAHandler() { public /*PhysPt*/long call() { return base_ss+((reg_ebp.word()+Fetchbs.call()) & 0xFFFF); } };
    final static private GetEAHandler  EA_16_47_n= new GetEAHandler() { public /*PhysPt*/long call() { return base_ds+((reg_ebx.word()+Fetchbs.call()) & 0xFFFF); } };

    final static private GetEAHandler  EA_16_80_n= new GetEAHandler() { public /*PhysPt*/long call() { return base_ds+((reg_ebx.word()+(/*Bit16s*/short)reg_esi.word()+Fetchws.call()) & 0xFFFF); } };
    final static private GetEAHandler  EA_16_81_n= new GetEAHandler() { public /*PhysPt*/long call() { return base_ds+((reg_ebx.word()+(/*Bit16s*/short)reg_edi.word()+Fetchws.call()) & 0xFFFF); } };
    final static private GetEAHandler  EA_16_82_n= new GetEAHandler() { public /*PhysPt*/long call() { return base_ss+((reg_ebp.word()+(/*Bit16s*/short)reg_esi.word()+Fetchws.call()) & 0xFFFF); } };
    final static private GetEAHandler  EA_16_83_n= new GetEAHandler() { public /*PhysPt*/long call() { return base_ss+((reg_ebp.word()+(/*Bit16s*/short)reg_edi.word()+Fetchws.call()) & 0xFFFF); } };
    final static private GetEAHandler  EA_16_84_n= new GetEAHandler() { public /*PhysPt*/long call() { return base_ds+((reg_esi.word()+Fetchws.call()) & 0xFFFF); } };
    final static private GetEAHandler  EA_16_85_n= new GetEAHandler() { public /*PhysPt*/long call() { return base_ds+((reg_edi.word()+Fetchws.call()) & 0xFFFF); } };
    final static private GetEAHandler  EA_16_86_n= new GetEAHandler() { public /*PhysPt*/long call() { return base_ss+((reg_ebp.word()+Fetchws.call()) & 0xFFFF); } };
    final static private GetEAHandler  EA_16_87_n= new GetEAHandler() { public /*PhysPt*/long call() { return base_ds+((reg_ebx.word()+Fetchws.call()) & 0xFFFF); } };

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

    final static private GetEAHandler  EA_32_00_n= new GetEAHandler() { public /*PhysPt*/long call() { return (base_ds+reg_eax.dword()) & 0xFFFFFFFFl; } };
    final static private GetEAHandler  EA_32_01_n= new GetEAHandler() { public /*PhysPt*/long call() { return (base_ds+reg_ecx.dword()) & 0xFFFFFFFFl; } };
    final static private GetEAHandler  EA_32_02_n= new GetEAHandler() { public /*PhysPt*/long call() { return (base_ds+reg_edx.dword()) & 0xFFFFFFFFl; } };
    final static private GetEAHandler  EA_32_03_n= new GetEAHandler() { public /*PhysPt*/long call() { return (base_ds+reg_ebx.dword()) & 0xFFFFFFFFl; } };
    final static private GetEAHandler  EA_32_04_n= new GetEAHandler() { public /*PhysPt*/long call() { return Sib(0);}};
    final static private GetEAHandler  EA_32_05_n= new GetEAHandler() { public /*PhysPt*/long call() { return (base_ds+Fetchd.call()) & 0xFFFFFFFFl; } };
    final static private GetEAHandler  EA_32_06_n= new GetEAHandler() { public /*PhysPt*/long call() { return (base_ds+reg_esi.dword()) & 0xFFFFFFFFl; } };
    final static private GetEAHandler  EA_32_07_n= new GetEAHandler() { public /*PhysPt*/long call() { return (base_ds+reg_edi.dword()) & 0xFFFFFFFFl; } };

    final static private GetEAHandler  EA_32_40_n= new GetEAHandler() { public /*PhysPt*/long call() { return (base_ds+reg_eax.dword()+Fetchbs.call()) & 0xFFFFFFFFl; } };
    final static private GetEAHandler  EA_32_41_n= new GetEAHandler() { public /*PhysPt*/long call() { return (base_ds+reg_ecx.dword()+Fetchbs.call()) & 0xFFFFFFFFl; } };
    final static private GetEAHandler  EA_32_42_n= new GetEAHandler() { public /*PhysPt*/long call() { return (base_ds+reg_edx.dword()+Fetchbs.call()) & 0xFFFFFFFFl; } };
    final static private GetEAHandler  EA_32_43_n= new GetEAHandler() { public /*PhysPt*/long call() { return (base_ds+reg_ebx.dword()+Fetchbs.call()) & 0xFFFFFFFFl; } };
    final static private GetEAHandler  EA_32_44_n= new GetEAHandler() { public /*PhysPt*/long call() { return (Sib(1)+Fetchbs.call()) & 0xFFFFFFFFl;} };
//static private GetEAHandler  EA_32_44_n= new GetEAHandler() { public /*PhysPt*/long call() { return Sib(1)+Fetchbs.call();}
    final static private GetEAHandler  EA_32_45_n= new GetEAHandler() { public /*PhysPt*/long call() { return (base_ss+reg_ebp.dword()+Fetchbs.call()) & 0xFFFFFFFFl; } };
    final static private GetEAHandler  EA_32_46_n= new GetEAHandler() { public /*PhysPt*/long call() { return (base_ds+reg_esi.dword()+Fetchbs.call()) & 0xFFFFFFFFl; } };
    final static private GetEAHandler  EA_32_47_n= new GetEAHandler() { public /*PhysPt*/long call() { return (base_ds+reg_edi.dword()+Fetchbs.call()) & 0xFFFFFFFFl; } };

    final static private GetEAHandler  EA_32_80_n= new GetEAHandler() { public /*PhysPt*/long call() { return (base_ds+reg_eax.dword()+Fetchds.call()) & 0xFFFFFFFFl; } };
    final static private GetEAHandler  EA_32_81_n= new GetEAHandler() { public /*PhysPt*/long call() { return (base_ds+reg_ecx.dword()+Fetchds.call()) & 0xFFFFFFFFl; } };
    final static private GetEAHandler  EA_32_82_n= new GetEAHandler() { public /*PhysPt*/long call() { return (base_ds+reg_edx.dword()+Fetchds.call()) & 0xFFFFFFFFl; } };
    final static private GetEAHandler  EA_32_83_n= new GetEAHandler() { public /*PhysPt*/long call() { return (base_ds+reg_ebx.dword()+Fetchds.call()) & 0xFFFFFFFFl; } };
    final static private GetEAHandler  EA_32_84_n= new GetEAHandler() { public /*PhysPt*/long call() { return (Sib(2)+Fetchds.call()) & 0xFFFFFFFFl;} };
//static private GetEAHandler  EA_32_84_n= new GetEAHandler() { public /*PhysPt*/long call() { return Sib(2)+Fetchds.call();}
    final static private GetEAHandler  EA_32_85_n= new GetEAHandler() { public /*PhysPt*/long call() { return (base_ss+reg_ebp.dword()+Fetchds.call()) & 0xFFFFFFFFl; } };
    final static private GetEAHandler  EA_32_86_n= new GetEAHandler() { public /*PhysPt*/long call() { return (base_ds+reg_esi.dword()+Fetchds.call()) & 0xFFFFFFFFl; } };
    final static private GetEAHandler  EA_32_87_n= new GetEAHandler() { public /*PhysPt*/long call() { return (base_ds+reg_edi.dword()+Fetchds.call()) & 0xFFFFFFFFl; } };

    static public GetEAHandler[] EATable16 ={
/* 00 */
        EA_16_00_n,EA_16_01_n,EA_16_02_n,EA_16_03_n,EA_16_04_n,EA_16_05_n,EA_16_06_n,EA_16_07_n,
        EA_16_00_n,EA_16_01_n,EA_16_02_n,EA_16_03_n,EA_16_04_n,EA_16_05_n,EA_16_06_n,EA_16_07_n,
        EA_16_00_n,EA_16_01_n,EA_16_02_n,EA_16_03_n,EA_16_04_n,EA_16_05_n,EA_16_06_n,EA_16_07_n,
        EA_16_00_n,EA_16_01_n,EA_16_02_n,EA_16_03_n,EA_16_04_n,EA_16_05_n,EA_16_06_n,EA_16_07_n,
        EA_16_00_n,EA_16_01_n,EA_16_02_n,EA_16_03_n,EA_16_04_n,EA_16_05_n,EA_16_06_n,EA_16_07_n,
        EA_16_00_n,EA_16_01_n,EA_16_02_n,EA_16_03_n,EA_16_04_n,EA_16_05_n,EA_16_06_n,EA_16_07_n,
        EA_16_00_n,EA_16_01_n,EA_16_02_n,EA_16_03_n,EA_16_04_n,EA_16_05_n,EA_16_06_n,EA_16_07_n,
        EA_16_00_n,EA_16_01_n,EA_16_02_n,EA_16_03_n,EA_16_04_n,EA_16_05_n,EA_16_06_n,EA_16_07_n,
/* 01 */
        EA_16_40_n,EA_16_41_n,EA_16_42_n,EA_16_43_n,EA_16_44_n,EA_16_45_n,EA_16_46_n,EA_16_47_n,
        EA_16_40_n,EA_16_41_n,EA_16_42_n,EA_16_43_n,EA_16_44_n,EA_16_45_n,EA_16_46_n,EA_16_47_n,
        EA_16_40_n,EA_16_41_n,EA_16_42_n,EA_16_43_n,EA_16_44_n,EA_16_45_n,EA_16_46_n,EA_16_47_n,
        EA_16_40_n,EA_16_41_n,EA_16_42_n,EA_16_43_n,EA_16_44_n,EA_16_45_n,EA_16_46_n,EA_16_47_n,
        EA_16_40_n,EA_16_41_n,EA_16_42_n,EA_16_43_n,EA_16_44_n,EA_16_45_n,EA_16_46_n,EA_16_47_n,
        EA_16_40_n,EA_16_41_n,EA_16_42_n,EA_16_43_n,EA_16_44_n,EA_16_45_n,EA_16_46_n,EA_16_47_n,
        EA_16_40_n,EA_16_41_n,EA_16_42_n,EA_16_43_n,EA_16_44_n,EA_16_45_n,EA_16_46_n,EA_16_47_n,
        EA_16_40_n,EA_16_41_n,EA_16_42_n,EA_16_43_n,EA_16_44_n,EA_16_45_n,EA_16_46_n,EA_16_47_n,
/* 10 */
        EA_16_80_n,EA_16_81_n,EA_16_82_n,EA_16_83_n,EA_16_84_n,EA_16_85_n,EA_16_86_n,EA_16_87_n,
        EA_16_80_n,EA_16_81_n,EA_16_82_n,EA_16_83_n,EA_16_84_n,EA_16_85_n,EA_16_86_n,EA_16_87_n,
        EA_16_80_n,EA_16_81_n,EA_16_82_n,EA_16_83_n,EA_16_84_n,EA_16_85_n,EA_16_86_n,EA_16_87_n,
        EA_16_80_n,EA_16_81_n,EA_16_82_n,EA_16_83_n,EA_16_84_n,EA_16_85_n,EA_16_86_n,EA_16_87_n,
        EA_16_80_n,EA_16_81_n,EA_16_82_n,EA_16_83_n,EA_16_84_n,EA_16_85_n,EA_16_86_n,EA_16_87_n,
        EA_16_80_n,EA_16_81_n,EA_16_82_n,EA_16_83_n,EA_16_84_n,EA_16_85_n,EA_16_86_n,EA_16_87_n,
        EA_16_80_n,EA_16_81_n,EA_16_82_n,EA_16_83_n,EA_16_84_n,EA_16_85_n,EA_16_86_n,EA_16_87_n,
        EA_16_80_n,EA_16_81_n,EA_16_82_n,EA_16_83_n,EA_16_84_n,EA_16_85_n,EA_16_86_n,EA_16_87_n,
/* 11 These are illegal so make em 0 */
        null,	null,	null,	null,	null,	null,	null,	null,	null,	null,	null,	null,	null,	null,	null,	null,
        null,	null,	null,	null,	null,	null,	null,	null,	null,	null,	null,	null,	null,	null,	null,	null,
        null,	null,	null,	null,	null,	null,	null,	null,	null,	null,	null,	null,	null,	null,	null,	null,
        null,	null,	null,	null,	null,	null,	null,	null,	null,	null,	null,	null,	null,	null,	null,	null};

    static public GetEAHandler[] EATable32 ={
/* 00 */
        EA_32_00_n,EA_32_01_n,EA_32_02_n,EA_32_03_n,EA_32_04_n,EA_32_05_n,EA_32_06_n,EA_32_07_n,
        EA_32_00_n,EA_32_01_n,EA_32_02_n,EA_32_03_n,EA_32_04_n,EA_32_05_n,EA_32_06_n,EA_32_07_n,
        EA_32_00_n,EA_32_01_n,EA_32_02_n,EA_32_03_n,EA_32_04_n,EA_32_05_n,EA_32_06_n,EA_32_07_n,
        EA_32_00_n,EA_32_01_n,EA_32_02_n,EA_32_03_n,EA_32_04_n,EA_32_05_n,EA_32_06_n,EA_32_07_n,
        EA_32_00_n,EA_32_01_n,EA_32_02_n,EA_32_03_n,EA_32_04_n,EA_32_05_n,EA_32_06_n,EA_32_07_n,
        EA_32_00_n,EA_32_01_n,EA_32_02_n,EA_32_03_n,EA_32_04_n,EA_32_05_n,EA_32_06_n,EA_32_07_n,
        EA_32_00_n,EA_32_01_n,EA_32_02_n,EA_32_03_n,EA_32_04_n,EA_32_05_n,EA_32_06_n,EA_32_07_n,
        EA_32_00_n,EA_32_01_n,EA_32_02_n,EA_32_03_n,EA_32_04_n,EA_32_05_n,EA_32_06_n,EA_32_07_n,
/* 01 */
        EA_32_40_n,EA_32_41_n,EA_32_42_n,EA_32_43_n,EA_32_44_n,EA_32_45_n,EA_32_46_n,EA_32_47_n,
        EA_32_40_n,EA_32_41_n,EA_32_42_n,EA_32_43_n,EA_32_44_n,EA_32_45_n,EA_32_46_n,EA_32_47_n,
        EA_32_40_n,EA_32_41_n,EA_32_42_n,EA_32_43_n,EA_32_44_n,EA_32_45_n,EA_32_46_n,EA_32_47_n,
        EA_32_40_n,EA_32_41_n,EA_32_42_n,EA_32_43_n,EA_32_44_n,EA_32_45_n,EA_32_46_n,EA_32_47_n,
        EA_32_40_n,EA_32_41_n,EA_32_42_n,EA_32_43_n,EA_32_44_n,EA_32_45_n,EA_32_46_n,EA_32_47_n,
        EA_32_40_n,EA_32_41_n,EA_32_42_n,EA_32_43_n,EA_32_44_n,EA_32_45_n,EA_32_46_n,EA_32_47_n,
        EA_32_40_n,EA_32_41_n,EA_32_42_n,EA_32_43_n,EA_32_44_n,EA_32_45_n,EA_32_46_n,EA_32_47_n,
        EA_32_40_n,EA_32_41_n,EA_32_42_n,EA_32_43_n,EA_32_44_n,EA_32_45_n,EA_32_46_n,EA_32_47_n,
/* 10 */
        EA_32_80_n,EA_32_81_n,EA_32_82_n,EA_32_83_n,EA_32_84_n,EA_32_85_n,EA_32_86_n,EA_32_87_n,
        EA_32_80_n,EA_32_81_n,EA_32_82_n,EA_32_83_n,EA_32_84_n,EA_32_85_n,EA_32_86_n,EA_32_87_n,
        EA_32_80_n,EA_32_81_n,EA_32_82_n,EA_32_83_n,EA_32_84_n,EA_32_85_n,EA_32_86_n,EA_32_87_n,
        EA_32_80_n,EA_32_81_n,EA_32_82_n,EA_32_83_n,EA_32_84_n,EA_32_85_n,EA_32_86_n,EA_32_87_n,
        EA_32_80_n,EA_32_81_n,EA_32_82_n,EA_32_83_n,EA_32_84_n,EA_32_85_n,EA_32_86_n,EA_32_87_n,
        EA_32_80_n,EA_32_81_n,EA_32_82_n,EA_32_83_n,EA_32_84_n,EA_32_85_n,EA_32_86_n,EA_32_87_n,
        EA_32_80_n,EA_32_81_n,EA_32_82_n,EA_32_83_n,EA_32_84_n,EA_32_85_n,EA_32_86_n,EA_32_87_n,
        EA_32_80_n,EA_32_81_n,EA_32_82_n,EA_32_83_n,EA_32_84_n,EA_32_85_n,EA_32_86_n,EA_32_87_n,
/* 11 These are illegal so make em 0 */
        null,	null,	null,	null,	null,	null,	null,	null,	null,	null,	null,	null,	null,	null,	null,	null,
        null,	null,	null,	null,	null,	null,	null,	null,	null,	null,	null,	null,	null,	null,	null,	null,
        null,	null,	null,	null,	null,	null,	null,	null,	null,	null,	null,	null,	null,	null,	null,	null,
        null,	null,	null,	null,	null,	null,	null,	null,	null,	null,	null,	null,	null,	null,	null,	null
    };

    protected static long GetEADirect() {
        if ((prefixes & PREFIX_ADDR)!=0) {
            return base_ds+Fetchd.call();
        } else {
            return base_ds+Fetchw.call();
        }
    }
}
