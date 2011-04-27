package jdos.cpu.core_dynamic;

import jdos.cpu.Core;

public class Eaa extends Helper {
    final static public class EA_16_00_n implements EaaBase {
        public /*PhysPt*/long call() { return Core.base_ds+((reg_ebx.word()+(/*Bit16s*/short)reg_esi.word()) & 0xFFFF); }
    }
    final static public class EA_16_01_n implements EaaBase {
        public /*PhysPt*/long call() { return Core.base_ds+((reg_ebx.word()+(/*Bit16s*/short)reg_edi.word()) & 0xFFFF); }
    }
    final static public class EA_16_02_n implements EaaBase {
        public /*PhysPt*/long call() { return Core.base_ss+((reg_ebp.word()+(/*Bit16s*/short)reg_esi.word()) & 0xFFFF); }
    }
    final static public class EA_16_03_n implements EaaBase {
        public /*PhysPt*/long call() { return Core.base_ss+((reg_ebp.word()+(/*Bit16s*/short)reg_edi.word()) & 0xFFFF); }
    }
    final static public class EA_16_04_n implements EaaBase {
        public /*PhysPt*/long call() { return Core.base_ds+(reg_esi.word()); }
    }
    final static public class EA_16_05_n implements EaaBase {
        public /*PhysPt*/long call() { return Core.base_ds+(reg_edi.word()); }
    }
    final static public class EA_16_06_n implements EaaBase {
        int i;
        public EA_16_06_n() {
            i = decode_fetchw();
        }
        public /*PhysPt*/long call() { return Core.base_ds+i;}
    }
    final static public class EA_16_07_n implements EaaBase {
        public /*PhysPt*/long call() { return Core.base_ds+(reg_ebx.word()); }
    }

    final static public class EA_16_40_n implements EaaBase {
        int i;
        public EA_16_40_n() {
            i = decode_fetchbs();
        }
        public /*PhysPt*/long call() { return Core.base_ds+((reg_ebx.word()+(/*Bit16s*/short)reg_esi.word()+i) & 0xFFFF); }
    }
    final static public class EA_16_41_n implements EaaBase {
        byte i;
        public EA_16_41_n() {
            i = decode_fetchbs();
        }
        public /*PhysPt*/long call() { return Core.base_ds+((reg_ebx.word()+(/*Bit16s*/short)reg_edi.word()+i) & 0xFFFF); }
    }
    final static public class EA_16_42_n implements EaaBase {
        int i;
        public EA_16_42_n() {
            i = decode_fetchbs();
        }
        public /*PhysPt*/long call() { return Core.base_ss+((reg_ebp.word()+(/*Bit16s*/short)reg_esi.word()+i) & 0xFFFF); }
    }
    final static public class EA_16_43_n implements EaaBase {
        int i;
        public EA_16_43_n() {
            i = decode_fetchbs();
        }
        public /*PhysPt*/long call() { return Core.base_ss+((reg_ebp.word()+(/*Bit16s*/short)reg_edi.word()+i) & 0xFFFF); }
    }
    final static public class EA_16_44_n implements EaaBase {
        int i;
        public EA_16_44_n() {
            i = decode_fetchbs();
        }
        public /*PhysPt*/long call() { return Core.base_ds+((reg_esi.word()+i) & 0xFFFF); }
    }
    final static public class EA_16_45_n implements EaaBase {
        int i;
        public EA_16_45_n() {
            i = decode_fetchbs();
        }
        public /*PhysPt*/long call() { return Core.base_ds+((reg_edi.word()+i) & 0xFFFF); }
    }
    final static public class EA_16_46_n implements EaaBase {
        int i;
        public EA_16_46_n() {
            i = decode_fetchbs();
        }
        public /*PhysPt*/long call() { return Core.base_ss+((reg_ebp.word()+i) & 0xFFFF); }
    }
    final static public class EA_16_47_n implements EaaBase {
        int i;
        public EA_16_47_n() {
            i = decode_fetchbs();
        }
        public /*PhysPt*/long call() { return Core.base_ds+((reg_ebx.word()+i) & 0xFFFF); }
    }

    final static public class EA_16_80_n implements EaaBase {
        int i;
        public EA_16_80_n() {
            i = decode_fetchws();
        }
        public /*PhysPt*/long call() { return Core.base_ds+((reg_ebx.word()+(/*Bit16s*/short)reg_esi.word()+i) & 0xFFFF); }
    }
    final static public class EA_16_81_n implements EaaBase {
        int i;
        public EA_16_81_n() {
            i = decode_fetchws();
        }
        public /*PhysPt*/long call() { return Core.base_ds+((reg_ebx.word()+(/*Bit16s*/short)reg_edi.word()+i) & 0xFFFF); }
    }
    final static public class EA_16_82_n implements EaaBase {
        int i;
        public EA_16_82_n() {
            i = decode_fetchws();
        }
        public /*PhysPt*/long call() { return Core.base_ss+((reg_ebp.word()+(/*Bit16s*/short)reg_esi.word()+i) & 0xFFFF); }
    }
    final static public class EA_16_83_n implements EaaBase {
        int i;
        public EA_16_83_n() {
            i = decode_fetchws();
        }
        public /*PhysPt*/long call() { return Core.base_ss+((reg_ebp.word()+(/*Bit16s*/short)reg_edi.word()+i) & 0xFFFF); }
    }
    final static public class EA_16_84_n implements EaaBase {
        int i;
        public EA_16_84_n() {
            i = decode_fetchws();
        }
        public /*PhysPt*/long call() { return Core.base_ds+((reg_esi.word()+i) & 0xFFFF); }
    }
    final static public class EA_16_85_n implements EaaBase {
        int i;
        public EA_16_85_n() {
            i = decode_fetchws();
        }
        public /*PhysPt*/long call() { return Core.base_ds+((reg_edi.word()+i) & 0xFFFF); }
    }
    final static public class EA_16_86_n implements EaaBase {
        int i;
        public EA_16_86_n() {
            i = decode_fetchws();
        }
        public /*PhysPt*/long call() { return Core.base_ss+((reg_ebp.word()+i) & 0xFFFF); }
    }
    final static public class EA_16_87_n implements EaaBase {
        int i;
        public EA_16_87_n() {
            i = decode_fetchws();
        }
        public /*PhysPt*/long call() { return Core.base_ds+((reg_ebx.word()+i) & 0xFFFF); }
    }

    final static public class EA_32_00_n implements EaaBase {
        public /*PhysPt*/long call() { return (Core.base_ds+reg_eax.dword()) & 0xFFFFFFFFl; }
    }
    final static public class EA_32_01_n implements EaaBase {
        public /*PhysPt*/long call() { return (Core.base_ds+reg_ecx.dword()) & 0xFFFFFFFFl; }
    }
    final static public class EA_32_02_n implements EaaBase {
        public /*PhysPt*/long call() { return (Core.base_ds+reg_edx.dword()) & 0xFFFFFFFFl; }
    }
    final static public class EA_32_03_n implements EaaBase {
        public /*PhysPt*/long call() { return (Core.base_ds+reg_ebx.dword()) & 0xFFFFFFFFl; }
    }
    final static public class EA_32_04_n implements EaaBase {
        int sib;
        long inst=0;

        public EA_32_04_n() {
            sib = decode_fetchb();
            if ((sib & 7)==5) {
                inst = decode_fetchd();
            }
        }
        public /*PhysPt*/long call() { return Sib0(sib, inst);}
    }
    final static public class EA_32_05_n implements EaaBase {
        long i;
        public EA_32_05_n() {
            i = decode_fetchd();
        }
        public /*PhysPt*/long call() { return (Core.base_ds+i) & 0xFFFFFFFFl; }
    }
    final static public class EA_32_06_n implements EaaBase {
        public /*PhysPt*/long call() { return (Core.base_ds+reg_esi.dword()) & 0xFFFFFFFFl; }
    }
    final static public class EA_32_07_n implements EaaBase {
        public /*PhysPt*/long call() { return (Core.base_ds+reg_edi.dword()) & 0xFFFFFFFFl; }
    }

    final static public class EA_32_40_n implements EaaBase {
        int i;
        public EA_32_40_n() {
            i = decode_fetchbs();
        }
        public /*PhysPt*/long call() { return (Core.base_ds+reg_eax.dword()+i) & 0xFFFFFFFFl; }
    }
    final static public class EA_32_41_n implements EaaBase {
        int i;
        public EA_32_41_n() {
            i = decode_fetchbs();
        }
        public /*PhysPt*/long call() { return (Core.base_ds+reg_ecx.dword()+i) & 0xFFFFFFFFl; }
    }
    final static public class EA_32_42_n implements EaaBase {
        int i;
        public EA_32_42_n() {
            i = decode_fetchbs();
        }
        public /*PhysPt*/long call() { return (Core.base_ds+reg_edx.dword()+i) & 0xFFFFFFFFl; }
    }
    final static public class EA_32_43_n implements EaaBase {
        int i;
        public EA_32_43_n() {
            i = decode_fetchbs();
        }
        public /*PhysPt*/long call() { return (Core.base_ds+reg_ebx.dword()+i) & 0xFFFFFFFFl; }
    }
    final static public class EA_32_44_n implements EaaBase {
        int i;
        int sib;
        public EA_32_44_n() {
            sib = decode_fetchb();
            i = decode_fetchbs();
        }
        public /*PhysPt*/long call() { return (Sib(sib)+i) & 0xFFFFFFFFl;}
    }
    final static public class EA_32_45_n implements EaaBase {
        int i;
        public EA_32_45_n() {
            i = decode_fetchbs();
        }
        public /*PhysPt*/long call() { return (Core.base_ss+reg_ebp.dword()+i) & 0xFFFFFFFFl; }
    }
    final static public class EA_32_46_n implements EaaBase {
        int i;
        public EA_32_46_n() {
            i = decode_fetchbs();
        }
        public /*PhysPt*/long call() { return (Core.base_ds+reg_esi.dword()+i) & 0xFFFFFFFFl; }
    }
    final static public class EA_32_47_n implements EaaBase {
        int i;
        public EA_32_47_n() {
            i = decode_fetchbs();
        }
        public /*PhysPt*/long call() { return (Core.base_ds+reg_edi.dword()+i) & 0xFFFFFFFFl; }
    }

    final static public class EA_32_80_n implements EaaBase {
        long i;
        public EA_32_80_n() {
            i = decode_fetchds();
        }
        public /*PhysPt*/long call() { return (Core.base_ds+reg_eax.dword()+i) & 0xFFFFFFFFl; }
    }
    final static public class EA_32_81_n implements EaaBase {
        long i;
        public EA_32_81_n() {
            i = decode_fetchds();
        }
        public /*PhysPt*/long call() { return (Core.base_ds+reg_ecx.dword()+i) & 0xFFFFFFFFl; }
    }
    final static public class EA_32_82_n implements EaaBase {
        long i;
        public EA_32_82_n() {
            i = decode_fetchds();
        }
        public /*PhysPt*/long call() { return (Core.base_ds+reg_edx.dword()+i) & 0xFFFFFFFFl; }
    }
    final static public class EA_32_83_n implements EaaBase {
        long i;
        public EA_32_83_n() {
            i = decode_fetchds();
        }
        public /*PhysPt*/long call() { return (Core.base_ds+reg_ebx.dword()+i) & 0xFFFFFFFFl; }
    }
    final static public class EA_32_84_n implements EaaBase {
        long i;
        int sib;
        public EA_32_84_n() {
            sib = decode_fetchb();
            i = decode_fetchds();
        }
        public /*PhysPt*/long call() { return (Sib(sib)+i) & 0xFFFFFFFFl;}
    }
    final static public class EA_32_85_n implements EaaBase {
        long i;
        public EA_32_85_n() {
            i = decode_fetchds();
        }
        public /*PhysPt*/long call() { return (Core.base_ss+reg_ebp.dword()+i) & 0xFFFFFFFFl; }
    }
    final static public class EA_32_86_n implements EaaBase {
        long i;
        public EA_32_86_n() {
            i = decode_fetchds();
        }
        public /*PhysPt*/long call() { return (Core.base_ds+reg_esi.dword()+i) & 0xFFFFFFFFl; }
    }
    final static public class EA_32_87_n implements EaaBase {
        long i;
        public EA_32_87_n() {
            i = decode_fetchds();
        }
        public /*PhysPt*/long call() { return (Core.base_ds+reg_edi.dword()+i) & 0xFFFFFFFFl; }
    }

    static private /*PhysPt*/long Sib0(int sib, long inst) {
        /*PhysPt*/long base=0;
        switch (sib&7) {
        case 0:	/* EAX Base */
            base=Core.base_ds+reg_eax.dword();break;
        case 1:	/* ECX Base */
            base=Core.base_ds+reg_ecx.dword();break;
        case 2:	/* EDX Base */
            base=Core.base_ds+reg_edx.dword();break;
        case 3:	/* EBX Base */
            base=Core.base_ds+reg_ebx.dword();break;
        case 4:	/* ESP Base */
            base=Core.base_ss+reg_esp.dword();break;
        case 5:	/* #1 Base */
            base=Core.base_ds+inst;break;
        case 6:	/* ESI Base */
            base=Core.base_ds+reg_esi.dword();break;
        case 7:	/* EDI Base */
            base=Core.base_ds+reg_edi.dword();break;
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
                //base+=SIBZero << (sib >> 6);
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

    static private /*PhysPt*/long Sib(/*Bitu*/int sib) {
        /*PhysPt*/long base=0;
        switch (sib&7) {
        case 0:	/* EAX Base */
            base=Core.base_ds+reg_eax.dword();break;
        case 1:	/* ECX Base */
            base=Core.base_ds+reg_ecx.dword();break;
        case 2:	/* EDX Base */
            base=Core.base_ds+reg_edx.dword();break;
        case 3:	/* EBX Base */
            base=Core.base_ds+reg_ebx.dword();break;
        case 4:	/* ESP Base */
            base=Core.base_ss+reg_esp.dword();break;
        case 5:	/* #1 Base */
            base=Core.base_ss+reg_ebp.dword();break;
        case 6:	/* ESI Base */
            base=Core.base_ds+reg_esi.dword();break;
        case 7:	/* EDI Base */
            base=Core.base_ds+reg_edi.dword();break;
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
                //base+=SIBZero << (sib >> 6);
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
}
