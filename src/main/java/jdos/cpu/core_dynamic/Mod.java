package jdos.cpu.core_dynamic;

public class Mod extends Helper {
    static public Reg eb(int rm) {
        switch (rm & 7) {
            case 0: return reg_eax;
            case 1: return reg_ecx;
            case 2: return reg_edx;
            case 3: return reg_ebx;
            case 4: return reg_ah;
            case 5: return reg_ch;
            case 6: return reg_dh;
            case 7: return reg_bh;
        }
        return null;
    }

    static public Reg gb(int rm) {
        switch ((rm >> 3) & 7) {
            case 0: return reg_eax;
            case 1: return reg_ecx;
            case 2: return reg_edx;
            case 3: return reg_ebx;
            case 4: return reg_ah;
            case 5: return reg_ch;
            case 6: return reg_dh;
            case 7: return reg_bh;
        }
        return null;
    }

    static public Reg ew(int rm) {
        switch (rm & 7) {
            case 0: return reg_eax;
            case 1: return reg_ecx;
            case 2: return reg_edx;
            case 3: return reg_ebx;
            case 4: return reg_esp;
            case 5: return reg_ebp;
            case 6: return reg_esi;
            case 7: return reg_edi;
        }
        return null;
    }

    static public Reg gw(int rm) {
        switch ((rm >> 3) & 7) {
            case 0: return reg_eax;
            case 1: return reg_ecx;
            case 2: return reg_edx;
            case 3: return reg_ebx;
            case 4: return reg_esp;
            case 5: return reg_ebp;
            case 6: return reg_esi;
            case 7: return reg_edi;
        }
        return null;
    }

    static public Reg ed(int rm) {
        switch (rm & 7) {
            case 0: return reg_eax;
            case 1: return reg_ecx;
            case 2: return reg_edx;
            case 3: return reg_ebx;
            case 4: return reg_esp;
            case 5: return reg_ebp;
            case 6: return reg_esi;
            case 7: return reg_edi;
        }
        return null;
    }

    static public Reg gd(int rm) {
        switch ((rm >> 3) & 7) {
            case 0: return reg_eax;
            case 1: return reg_ecx;
            case 2: return reg_edx;
            case 3: return reg_ebx;
            case 4: return reg_esp;
            case 5: return reg_ebp;
            case 6: return reg_esi;
            case 7: return reg_edi;
        }
        return null;
    }

    static public EaaBase getEaa32(int rm) {
        if (rm<0x40) {
            switch (rm & 7) {
                case 0x00: return new Eaa.EA_32_00_n();
                case 0x01: return new Eaa.EA_32_01_n();
                case 0x02: return new Eaa.EA_32_02_n();
                case 0x03: return new Eaa.EA_32_03_n();
                case 0x04: return new Eaa.EA_32_04_n();
                case 0x05: return new Eaa.EA_32_05_n();
                case 0x06: return new Eaa.EA_32_06_n();
                case 0x07: return new Eaa.EA_32_07_n();
            }
        } else if (rm<0x80) {
            switch (rm & 7) {
                case 0x00: return new Eaa.EA_32_40_n();
                case 0x01: return new Eaa.EA_32_41_n();
                case 0x02: return new Eaa.EA_32_42_n();
                case 0x03: return new Eaa.EA_32_43_n();
                case 0x04: return new Eaa.EA_32_44_n();
                case 0x05: return new Eaa.EA_32_45_n();
                case 0x06: return new Eaa.EA_32_46_n();
                case 0x07: return new Eaa.EA_32_47_n();
            }
        } else {
            switch (rm & 7) {
                case 0x00: return new Eaa.EA_32_80_n();
                case 0x01: return new Eaa.EA_32_81_n();
                case 0x02: return new Eaa.EA_32_82_n();
                case 0x03: return new Eaa.EA_32_83_n();
                case 0x04: return new Eaa.EA_32_84_n();
                case 0x05: return new Eaa.EA_32_85_n();
                case 0x06: return new Eaa.EA_32_86_n();
                case 0x07: return new Eaa.EA_32_87_n();
            }
        }
        return null;
    }

    static public EaaBase getEaa16(int rm) {
        if (rm<0x40) {
            switch (rm & 7) {
                case 0x00: return new Eaa.EA_16_00_n();
                case 0x01: return new Eaa.EA_16_01_n();
                case 0x02: return new Eaa.EA_16_02_n();
                case 0x03: return new Eaa.EA_16_03_n();
                case 0x04: return new Eaa.EA_16_04_n();
                case 0x05: return new Eaa.EA_16_05_n();
                case 0x06: return new Eaa.EA_16_06_n();
                case 0x07: return new Eaa.EA_16_07_n();
            }
        } else if (rm<0x80) {
            switch (rm & 7) {
                case 0x00: return new Eaa.EA_16_40_n();
                case 0x01: return new Eaa.EA_16_41_n();
                case 0x02: return new Eaa.EA_16_42_n();
                case 0x03: return new Eaa.EA_16_43_n();
                case 0x04: return new Eaa.EA_16_44_n();
                case 0x05: return new Eaa.EA_16_45_n();
                case 0x06: return new Eaa.EA_16_46_n();
                case 0x07: return new Eaa.EA_16_47_n();
            }
        } else {
            switch (rm & 7) {
                case 0x00: return new Eaa.EA_16_80_n();
                case 0x01: return new Eaa.EA_16_81_n();
                case 0x02: return new Eaa.EA_16_82_n();
                case 0x03: return new Eaa.EA_16_83_n();
                case 0x04: return new Eaa.EA_16_84_n();
                case 0x05: return new Eaa.EA_16_85_n();
                case 0x06: return new Eaa.EA_16_86_n();
                case 0x07: return new Eaa.EA_16_87_n();
            }
        }
        return null;
    }

    static public EaaBase getEaa(int rm) {
        if (EA16) return getEaa16(rm);
        return getEaa32(rm);
    }
}
