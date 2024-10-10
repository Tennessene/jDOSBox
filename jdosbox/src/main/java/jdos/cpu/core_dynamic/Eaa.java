package jdos.cpu.core_dynamic;

import jdos.cpu.Core;

public class Eaa extends Helper {
    final static public class EA_16_00_n extends EaaBase {
        public /*PhysPt*/int call() { return Core.base_ds+((reg_ebx.word()+(/*Bit16s*/short)reg_esi.word()) & 0xFFFF); }
    }
    final static public class EA_16_01_n extends EaaBase {
        public /*PhysPt*/int call() { return Core.base_ds+((reg_ebx.word()+(/*Bit16s*/short)reg_edi.word()) & 0xFFFF); }
    }
    final static public class EA_16_02_n extends EaaBase {
        public /*PhysPt*/int call() { return Core.base_ss+((reg_ebp.word()+(/*Bit16s*/short)reg_esi.word()) & 0xFFFF); }
    }
    final static public class EA_16_03_n extends EaaBase {
        public /*PhysPt*/int call() { return Core.base_ss+((reg_ebp.word()+(/*Bit16s*/short)reg_edi.word()) & 0xFFFF); }
    }
    final static public class EA_16_04_n extends EaaBase {
        public /*PhysPt*/int call() { return Core.base_ds+(reg_esi.word()); }
    }
    final static public class EA_16_05_n extends EaaBase {
        public /*PhysPt*/int call() { return Core.base_ds+(reg_edi.word()); }
    }
    final static public class EA_16_06_n extends EaaBase {
        int i;
        public EA_16_06_n() {
            i = decode_fetchw();
        }
        public /*PhysPt*/int call() { return Core.base_ds+i;}
    }
    final static public class EA_16_07_n extends EaaBase {
        public /*PhysPt*/int call() { return Core.base_ds+(reg_ebx.word()); }
    }

    final static public class EA_16_40_n extends EaaBase {
        int i;
        public EA_16_40_n() {
            i = decode_fetchbs();
        }
        public /*PhysPt*/int call() { return Core.base_ds+((reg_ebx.word()+(/*Bit16s*/short)reg_esi.word()+i) & 0xFFFF); }
    }
    final static public class EA_16_41_n extends EaaBase {
        int i;
        public EA_16_41_n() {
            i = decode_fetchbs();
        }
        public /*PhysPt*/int call() { return Core.base_ds+((reg_ebx.word()+(/*Bit16s*/short)reg_edi.word()+i) & 0xFFFF); }
    }
    final static public class EA_16_42_n extends EaaBase {
        int i;
        public EA_16_42_n() {
            i = decode_fetchbs();
        }
        public /*PhysPt*/int call() { return Core.base_ss+((reg_ebp.word()+(/*Bit16s*/short)reg_esi.word()+i) & 0xFFFF); }
    }
    final static public class EA_16_43_n extends EaaBase {
        int i;
        public EA_16_43_n() {
            i = decode_fetchbs();
        }
        public /*PhysPt*/int call() { return Core.base_ss+((reg_ebp.word()+(/*Bit16s*/short)reg_edi.word()+i) & 0xFFFF); }
    }
    final static public class EA_16_44_n extends EaaBase {
        int i;
        public EA_16_44_n() {
            i = decode_fetchbs();
        }
        public /*PhysPt*/int call() { return Core.base_ds+((reg_esi.word()+i) & 0xFFFF); }
    }
    final static public class EA_16_45_n extends EaaBase {
        int i;
        public EA_16_45_n() {
            i = decode_fetchbs();
        }
        public /*PhysPt*/int call() { return Core.base_ds+((reg_edi.word()+i) & 0xFFFF); }
    }
    final static public class EA_16_46_n extends EaaBase {
        int i;
        public EA_16_46_n() {
            i = decode_fetchbs();
        }
        public /*PhysPt*/int call() { return Core.base_ss+((reg_ebp.word()+i) & 0xFFFF); }
    }
    final static public class EA_16_47_n extends EaaBase {
        int i;
        public EA_16_47_n() {
            i = decode_fetchbs();
        }
        public /*PhysPt*/int call() { return Core.base_ds+((reg_ebx.word()+i) & 0xFFFF); }
    }

    final static public class EA_16_80_n extends EaaBase {
        int i;
        public EA_16_80_n() {
            i = decode_fetchws();
        }
        public /*PhysPt*/int call() { return Core.base_ds+((reg_ebx.word()+(/*Bit16s*/short)reg_esi.word()+i) & 0xFFFF); }
    }
    final static public class EA_16_81_n extends EaaBase {
        int i;
        public EA_16_81_n() {
            i = decode_fetchws();
        }
        public /*PhysPt*/int call() { return Core.base_ds+((reg_ebx.word()+(/*Bit16s*/short)reg_edi.word()+i) & 0xFFFF); }
    }
    final static public class EA_16_82_n extends EaaBase {
        int i;
        public EA_16_82_n() {
            i = decode_fetchws();
        }
        public /*PhysPt*/int call() { return Core.base_ss+((reg_ebp.word()+(/*Bit16s*/short)reg_esi.word()+i) & 0xFFFF); }
    }
    final static public class EA_16_83_n extends EaaBase {
        int i;
        public EA_16_83_n() {
            i = decode_fetchws();
        }
        public /*PhysPt*/int call() { return Core.base_ss+((reg_ebp.word()+(/*Bit16s*/short)reg_edi.word()+i) & 0xFFFF); }
    }
    final static public class EA_16_84_n extends EaaBase {
        int i;
        public EA_16_84_n() {
            i = decode_fetchws();
        }
        public /*PhysPt*/int call() { return Core.base_ds+((reg_esi.word()+i) & 0xFFFF); }
    }
    final static public class EA_16_85_n extends EaaBase {
        int i;
        public EA_16_85_n() {
            i = decode_fetchws();
        }
        public /*PhysPt*/int call() { return Core.base_ds+((reg_edi.word()+i) & 0xFFFF); }
    }
    final static public class EA_16_86_n extends EaaBase {
        int i;
        public EA_16_86_n() {
            i = decode_fetchws();
        }
        public /*PhysPt*/int call() { return Core.base_ss+((reg_ebp.word()+i) & 0xFFFF); }
    }
    final static public class EA_16_87_n extends EaaBase {
        int i;
        public EA_16_87_n() {
            i = decode_fetchws();
        }
        public /*PhysPt*/int call() { return Core.base_ds+((reg_ebx.word()+i) & 0xFFFF); }
    }

    final static public class EA_32_00_n extends EaaBase {
        public /*PhysPt*/int call() { return (Core.base_ds+reg_eax.dword); }
    }
    final static public class EA_32_01_n extends EaaBase {
        public /*PhysPt*/int call() { return (Core.base_ds+reg_ecx.dword); }
    }
    final static public class EA_32_02_n extends EaaBase {
        public /*PhysPt*/int call() { return (Core.base_ds+reg_edx.dword); }
    }
    final static public class EA_32_03_n extends EaaBase {
        public /*PhysPt*/int call() { return (Core.base_ds+reg_ebx.dword); }
    }
    final static public class EA_32_04_n extends EaaBase {
        boolean ds;
        Reg reg;
        Reg reg2;
        int sib;

        public EA_32_04_n() {
            sib = decode_fetchb();
            ds = true;
            switch (sib&7) {
            case 0:	/* EAX Base */
                reg = reg_eax;break;
            case 1:	/* ECX Base */
                reg = reg_ecx;break;
            case 2:	/* EDX Base */
                reg = reg_edx;break;
            case 3:	/* EBX Base */
                reg = reg_ebx;break;
            case 4:	/* ESP Base */
                ds = false;
                reg = reg_esp;break;
            case 5:	/* #1 Base */
                reg = new Reg();
                reg.dword= decode_fetchd();
                break;
            case 6:	/* ESI Base */
                reg = reg_esi;break;
            case 7:	/* EDI Base */
                reg = reg_edi;break;
            }
            int index =(sib >> 3) & 7;
            switch (index) {
                case 0:
                    reg2 = reg_eax;
                    break;
                case 1:
                    reg2 = reg_ecx;
                    break;
                case 2:
                    reg2 = reg_edx;
                    break;
                case 3:
                    reg2 = reg_ebx;
                    break;
                case 4:
                    reg2 = new Reg();
                    reg2.dword=0;
                    break;
                case 5:
                    reg2 = reg_ebp;
                    break;
                case 6:
                    reg2 = reg_esi;
                    break;
                case 7:
                    reg2 = reg_edi;
                    break;
            }
            sib = sib >> 6;
        }
        public /*PhysPt*/int call() {
            if (ds)
                return (Core.base_ds+reg.dword+(reg2.dword << sib));
            return (Core.base_ss+reg.dword+(reg2.dword << sib));
        }
    }
    final static public class EA_32_05_n extends EaaBase {
        int i;
        public EA_32_05_n() {
            i = decode_fetchd();
        }
        public /*PhysPt*/int call() { return (Core.base_ds+i); }
    }
    final static public class EA_32_06_n extends EaaBase {
        public /*PhysPt*/int call() { return (Core.base_ds+reg_esi.dword); }
    }
    final static public class EA_32_07_n extends EaaBase {
        public /*PhysPt*/int call() { return (Core.base_ds+reg_edi.dword); }
    }

    final static public class EA_32_40_n extends EaaBase {
        int i;
        public EA_32_40_n() {
            i = decode_fetchbs();
        }
        public /*PhysPt*/int call() { return (Core.base_ds+reg_eax.dword+i); }
    }
    final static public class EA_32_41_n extends EaaBase {
        int i;
        public EA_32_41_n() {
            i = decode_fetchbs();
        }
        public /*PhysPt*/int call() { return (Core.base_ds+reg_ecx.dword+i); }
    }
    final static public class EA_32_42_n extends EaaBase {
        int i;
        public EA_32_42_n() {
            i = decode_fetchbs();
        }
        public /*PhysPt*/int call() { return (Core.base_ds+reg_edx.dword+i); }
    }
    final static public class EA_32_43_n extends EaaBase {
        int i;
        public EA_32_43_n() {
            i = decode_fetchbs();
        }
        public /*PhysPt*/int call() { return (Core.base_ds+reg_ebx.dword+i); }
    }
    final static public class EA_32_44_n extends EaaBase {
        int i;

        boolean ds;
        Reg reg;
        Reg reg2;
        int sib;

        public EA_32_44_n() {
            sib = decode_fetchb();
            i = decode_fetchbs();
            ds = true;
            switch (sib&7) {
            case 0:	/* EAX Base */
                reg = reg_eax;break;
            case 1:	/* ECX Base */
                reg = reg_ecx;break;
            case 2:	/* EDX Base */
                reg = reg_edx;break;
            case 3:	/* EBX Base */
                reg = reg_ebx;break;
            case 4:	/* ESP Base */
                ds = false;
                reg = reg_esp;break;
            case 5:	/* #1 Base */
                ds = false;
                reg = reg_ebp;break;
            case 6:	/* ESI Base */
                reg = reg_esi;break;
            case 7:	/* EDI Base */
                reg = reg_edi;break;
            }
            int index =(sib >> 3) & 7;
            switch (index) {
                case 0:
                    reg2 = reg_eax;
                    break;
                case 1:
                    reg2 = reg_ecx;
                    break;
                case 2:
                    reg2 = reg_edx;
                    break;
                case 3:
                    reg2 = reg_ebx;
                    break;
                case 4:
                    reg2 = new Reg();
                    reg2.dword=0;
                    break;
                case 5:
                    reg2 = reg_ebp;
                    break;
                case 6:
                    reg2 = reg_esi;
                    break;
                case 7:
                    reg2 = reg_edi;
                    break;
            }
            sib = sib >> 6;
        }
        public /*PhysPt*/int call() {
            if (ds)
                return (Core.base_ds+reg.dword+(reg2.dword << sib)+i);
            return (Core.base_ss+reg.dword+(reg2.dword << sib)+i);
        }
    }
    final static public class EA_32_45_n extends EaaBase {
        int i;
        public EA_32_45_n() {
            i = decode_fetchbs();
        }
        public /*PhysPt*/int call() { return (Core.base_ss+reg_ebp.dword+i); }
    }
    final static public class EA_32_46_n extends EaaBase {
        int i;
        public EA_32_46_n() {
            i = decode_fetchbs();
        }
        public /*PhysPt*/int call() { return (Core.base_ds+reg_esi.dword+i); }
    }
    final static public class EA_32_47_n extends EaaBase {
        int i;
        public EA_32_47_n() {
            i = decode_fetchbs();
        }
        public /*PhysPt*/int call() { return (Core.base_ds+reg_edi.dword+i); }
    }

    final static public class EA_32_80_n extends EaaBase {
        int i;
        public EA_32_80_n() {
            i = decode_fetchds();
        }
        public /*PhysPt*/int call() { return (Core.base_ds+reg_eax.dword+i); }
    }
    final static public class EA_32_81_n extends EaaBase {
        int i;
        public EA_32_81_n() {
            i = decode_fetchds();
        }
        public /*PhysPt*/int call() { return (Core.base_ds+reg_ecx.dword+i); }
    }
    final static public class EA_32_82_n extends EaaBase {
        int i;
        public EA_32_82_n() {
            i = decode_fetchds();
        }
        public /*PhysPt*/int call() { return (Core.base_ds+reg_edx.dword+i); }
    }
    final static public class EA_32_83_n extends EaaBase {
        int i;
        public EA_32_83_n() {
            i = decode_fetchds();
        }
        public /*PhysPt*/int call() { return (Core.base_ds+reg_ebx.dword+i); }
    }
    final static public class EA_32_84_n extends EaaBase {
        int i;

        boolean ds;
        Reg reg;
        Reg reg2;
        int sib;

        public EA_32_84_n() {
            sib = decode_fetchb();
            i = decode_fetchds();
            ds = true;
            switch (sib&7) {
            case 0:	/* EAX Base */
                reg = reg_eax;break;
            case 1:	/* ECX Base */
                reg = reg_ecx;break;
            case 2:	/* EDX Base */
                reg = reg_edx;break;
            case 3:	/* EBX Base */
                reg = reg_ebx;break;
            case 4:	/* ESP Base */
                ds = false;
                reg = reg_esp;break;
            case 5:	/* #1 Base */
                ds = false;
                reg = reg_ebp;break;
            case 6:	/* ESI Base */
                reg = reg_esi;break;
            case 7:	/* EDI Base */
                reg = reg_edi;break;
            }
            int index =(sib >> 3) & 7;
            switch (index) {
                case 0:
                    reg2 = reg_eax;
                    break;
                case 1:
                    reg2 = reg_ecx;
                    break;
                case 2:
                    reg2 = reg_edx;
                    break;
                case 3:
                    reg2 = reg_ebx;
                    break;
                case 4:
                    reg2 = new Reg();
                    reg2.dword=0;
                    break;
                case 5:
                    reg2 = reg_ebp;
                    break;
                case 6:
                    reg2 = reg_esi;
                    break;
                case 7:
                    reg2 = reg_edi;
                    break;
            }
            sib = sib >> 6;
        }
        public /*PhysPt*/int call() {
            if (ds)
                return (Core.base_ds+reg.dword+(reg2.dword << sib)+i);
            return (Core.base_ss+reg.dword+(reg2.dword << sib)+i);
        }
    }
    final static public class EA_32_85_n extends EaaBase {
        int i;
        public EA_32_85_n() {
            i = decode_fetchds();
        }
        public /*PhysPt*/int call() { return (Core.base_ss+reg_ebp.dword+i); }
    }
    final static public class EA_32_86_n extends EaaBase {
        int i;
        public EA_32_86_n() {
            i = decode_fetchds();
        }
        public /*PhysPt*/int call() { return (Core.base_ds+reg_esi.dword+i); }
    }
    final static public class EA_32_87_n extends EaaBase {
        int i;
        public EA_32_87_n() {
            i = decode_fetchds();
        }
        public /*PhysPt*/int call() { return (Core.base_ds+reg_edi.dword+i); }
    }
}
