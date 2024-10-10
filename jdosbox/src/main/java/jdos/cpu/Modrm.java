package jdos.cpu;

public class Modrm {
    static public interface Getrb_interface {
        public int get();
        public void set(int value);
    }

    static final private Getrb_interface al = new Getrb_interface() {
        final public void set(int value) {
            CPU_Regs.reg_eax.low(value);
        }
        final public int get() {
            return CPU_Regs.reg_eax.low();
        }
    };

    static final private Getrb_interface cl = new Getrb_interface() {
        final public void set(int value) {
            CPU_Regs.reg_ecx.low(value);
        }
        final public int get() {
            return CPU_Regs.reg_ecx.low();
        }
    };

    static final private Getrb_interface dl = new Getrb_interface() {
        final public void set(int value) {
            CPU_Regs.reg_edx.low(value);
        }
        final public int get() {
            return CPU_Regs.reg_edx.low();
        }
    };

    static final private Getrb_interface bl = new Getrb_interface() {
        final public void set(int value) {
            CPU_Regs.reg_ebx.low(value);
        }
        final public int get() {
            return CPU_Regs.reg_ebx.low();
        }
    };

    static final private Getrb_interface ah = new Getrb_interface() {
        final public void set(int value) {
            CPU_Regs.reg_eax.high(value);
        }
        final public int get() {
            return CPU_Regs.reg_eax.high();
        }
    };

    static final private Getrb_interface ch = new Getrb_interface() {
        final public void set(int value) {
            CPU_Regs.reg_ecx.high(value);
        }
        final public int get() {
            return CPU_Regs.reg_ecx.high();
        }
    };

    static final private Getrb_interface dh = new Getrb_interface() {
        final public void set(int value) {
            CPU_Regs.reg_edx.high(value);
        }
        final public int get() {
            return CPU_Regs.reg_edx.high();
        }
    };

    static final private Getrb_interface bh = new Getrb_interface() {
        final public void set(int value) {
            CPU_Regs.reg_ebx.high(value);
        }
        final public int get() {
            return CPU_Regs.reg_ebx.high();
        }
    };
    final static public Getrb_interface[] Getrb = new Getrb_interface[] {
            al, al, al, al, al, al, al, al,
            cl, cl, cl, cl, cl, cl, cl, cl,
            dl, dl, dl, dl, dl, dl, dl, dl,
            bl, bl, bl, bl, bl, bl, bl, bl,
            ah, ah, ah, ah, ah, ah, ah, ah,
            ch, ch, ch, ch, ch, ch, ch, ch,
            dh, dh, dh, dh, dh, dh, dh, dh,
            bh, bh, bh, bh, bh, bh, bh, bh,

            al, al, al, al, al, al, al, al,
            cl, cl, cl, cl, cl, cl, cl, cl,
            dl, dl, dl, dl, dl, dl, dl, dl,
            bl, bl, bl, bl, bl, bl, bl, bl,
            ah, ah, ah, ah, ah, ah, ah, ah,
            ch, ch, ch, ch, ch, ch, ch, ch,
            dh, dh, dh, dh, dh, dh, dh, dh,
            bh, bh, bh, bh, bh, bh, bh, bh,

            al, al, al, al, al, al, al, al,
            cl, cl, cl, cl, cl, cl, cl, cl,
            dl, dl, dl, dl, dl, dl, dl, dl,
            bl, bl, bl, bl, bl, bl, bl, bl,
            ah, ah, ah, ah, ah, ah, ah, ah,
            ch, ch, ch, ch, ch, ch, ch, ch,
            dh, dh, dh, dh, dh, dh, dh, dh,
            bh, bh, bh, bh, bh, bh, bh, bh,

            al, al, al, al, al, al, al, al,
            cl, cl, cl, cl, cl, cl, cl, cl,
            dl, dl, dl, dl, dl, dl, dl, dl,
            bl, bl, bl, bl, bl, bl, bl, bl,
            ah, ah, ah, ah, ah, ah, ah, ah,
            ch, ch, ch, ch, ch, ch, ch, ch,
            dh, dh, dh, dh, dh, dh, dh, dh,
            bh, bh, bh, bh, bh, bh, bh, bh
    };
//    static public void Getrb(int rm, short value) {
//        switch ((rm / 8) % 8) {
//            case 0: CPU_Regs.reg_eax.low(value);break;
//            case 1: CPU_Regs.reg_ecx.low(value);break;
//            case 2: CPU_Regs.reg_edx.low(value);break;
//            case 3: CPU_Regs.reg_ebx.low(value);break;
//            case 4: CPU_Regs.reg_eax.high(value);break;
//            case 5: CPU_Regs.reg_ecx.high(value);break;
//            case 6: CPU_Regs.reg_edx.high(value);break;
//            case 7: CPU_Regs.reg_ebx.high(value);break;
//            default:
//                throw new RuntimeException("WTF "+rm);
//        }
//    }

//    static public short Getrb(int rm) {
//        switch ((rm / 8) % 8) {
//            case 0: return CPU_Regs.reg_eax.low();
//            case 1: return CPU_Regs.reg_ecx.low();
//            case 2: return CPU_Regs.reg_edx.low();
//            case 3: return CPU_Regs.reg_ebx.low();
//            case 4: return CPU_Regs.reg_eax.high();
//            case 5: return CPU_Regs.reg_ecx.high();
//            case 6: return CPU_Regs.reg_edx.high();
//            case 7: return CPU_Regs.reg_ebx.high();
//            default:
//                throw new RuntimeException("WTF "+rm);
//        }
//    }

    static final public CPU_Regs.Reg[] Getrw = new CPU_Regs.Reg[] {
            CPU_Regs.reg_eax, CPU_Regs.reg_eax, CPU_Regs.reg_eax, CPU_Regs.reg_eax, CPU_Regs.reg_eax, CPU_Regs.reg_eax, CPU_Regs.reg_eax, CPU_Regs.reg_eax,
            CPU_Regs.reg_ecx, CPU_Regs.reg_ecx, CPU_Regs.reg_ecx, CPU_Regs.reg_ecx, CPU_Regs.reg_ecx, CPU_Regs.reg_ecx, CPU_Regs.reg_ecx, CPU_Regs.reg_ecx,
            CPU_Regs.reg_edx, CPU_Regs.reg_edx, CPU_Regs.reg_edx, CPU_Regs.reg_edx, CPU_Regs.reg_edx, CPU_Regs.reg_edx, CPU_Regs.reg_edx, CPU_Regs.reg_edx,
            CPU_Regs.reg_ebx, CPU_Regs.reg_ebx, CPU_Regs.reg_ebx, CPU_Regs.reg_ebx, CPU_Regs.reg_ebx, CPU_Regs.reg_ebx, CPU_Regs.reg_ebx, CPU_Regs.reg_ebx,
            CPU_Regs.reg_esp, CPU_Regs.reg_esp, CPU_Regs.reg_esp, CPU_Regs.reg_esp, CPU_Regs.reg_esp, CPU_Regs.reg_esp, CPU_Regs.reg_esp, CPU_Regs.reg_esp,
            CPU_Regs.reg_ebp, CPU_Regs.reg_ebp, CPU_Regs.reg_ebp, CPU_Regs.reg_ebp, CPU_Regs.reg_ebp, CPU_Regs.reg_ebp, CPU_Regs.reg_ebp, CPU_Regs.reg_ebp,
            CPU_Regs.reg_esi, CPU_Regs.reg_esi, CPU_Regs.reg_esi, CPU_Regs.reg_esi, CPU_Regs.reg_esi, CPU_Regs.reg_esi, CPU_Regs.reg_esi, CPU_Regs.reg_esi,
            CPU_Regs.reg_edi, CPU_Regs.reg_edi, CPU_Regs.reg_edi, CPU_Regs.reg_edi, CPU_Regs.reg_edi, CPU_Regs.reg_edi, CPU_Regs.reg_edi, CPU_Regs.reg_edi,

            CPU_Regs.reg_eax, CPU_Regs.reg_eax, CPU_Regs.reg_eax, CPU_Regs.reg_eax, CPU_Regs.reg_eax, CPU_Regs.reg_eax, CPU_Regs.reg_eax, CPU_Regs.reg_eax,
            CPU_Regs.reg_ecx, CPU_Regs.reg_ecx, CPU_Regs.reg_ecx, CPU_Regs.reg_ecx, CPU_Regs.reg_ecx, CPU_Regs.reg_ecx, CPU_Regs.reg_ecx, CPU_Regs.reg_ecx,
            CPU_Regs.reg_edx, CPU_Regs.reg_edx, CPU_Regs.reg_edx, CPU_Regs.reg_edx, CPU_Regs.reg_edx, CPU_Regs.reg_edx, CPU_Regs.reg_edx, CPU_Regs.reg_edx,
            CPU_Regs.reg_ebx, CPU_Regs.reg_ebx, CPU_Regs.reg_ebx, CPU_Regs.reg_ebx, CPU_Regs.reg_ebx, CPU_Regs.reg_ebx, CPU_Regs.reg_ebx, CPU_Regs.reg_ebx,
            CPU_Regs.reg_esp, CPU_Regs.reg_esp, CPU_Regs.reg_esp, CPU_Regs.reg_esp, CPU_Regs.reg_esp, CPU_Regs.reg_esp, CPU_Regs.reg_esp, CPU_Regs.reg_esp,
            CPU_Regs.reg_ebp, CPU_Regs.reg_ebp, CPU_Regs.reg_ebp, CPU_Regs.reg_ebp, CPU_Regs.reg_ebp, CPU_Regs.reg_ebp, CPU_Regs.reg_ebp, CPU_Regs.reg_ebp,
            CPU_Regs.reg_esi, CPU_Regs.reg_esi, CPU_Regs.reg_esi, CPU_Regs.reg_esi, CPU_Regs.reg_esi, CPU_Regs.reg_esi, CPU_Regs.reg_esi, CPU_Regs.reg_esi,
            CPU_Regs.reg_edi, CPU_Regs.reg_edi, CPU_Regs.reg_edi, CPU_Regs.reg_edi, CPU_Regs.reg_edi, CPU_Regs.reg_edi, CPU_Regs.reg_edi, CPU_Regs.reg_edi,

            CPU_Regs.reg_eax, CPU_Regs.reg_eax, CPU_Regs.reg_eax, CPU_Regs.reg_eax, CPU_Regs.reg_eax, CPU_Regs.reg_eax, CPU_Regs.reg_eax, CPU_Regs.reg_eax,
            CPU_Regs.reg_ecx, CPU_Regs.reg_ecx, CPU_Regs.reg_ecx, CPU_Regs.reg_ecx, CPU_Regs.reg_ecx, CPU_Regs.reg_ecx, CPU_Regs.reg_ecx, CPU_Regs.reg_ecx,
            CPU_Regs.reg_edx, CPU_Regs.reg_edx, CPU_Regs.reg_edx, CPU_Regs.reg_edx, CPU_Regs.reg_edx, CPU_Regs.reg_edx, CPU_Regs.reg_edx, CPU_Regs.reg_edx,
            CPU_Regs.reg_ebx, CPU_Regs.reg_ebx, CPU_Regs.reg_ebx, CPU_Regs.reg_ebx, CPU_Regs.reg_ebx, CPU_Regs.reg_ebx, CPU_Regs.reg_ebx, CPU_Regs.reg_ebx,
            CPU_Regs.reg_esp, CPU_Regs.reg_esp, CPU_Regs.reg_esp, CPU_Regs.reg_esp, CPU_Regs.reg_esp, CPU_Regs.reg_esp, CPU_Regs.reg_esp, CPU_Regs.reg_esp,
            CPU_Regs.reg_ebp, CPU_Regs.reg_ebp, CPU_Regs.reg_ebp, CPU_Regs.reg_ebp, CPU_Regs.reg_ebp, CPU_Regs.reg_ebp, CPU_Regs.reg_ebp, CPU_Regs.reg_ebp,
            CPU_Regs.reg_esi, CPU_Regs.reg_esi, CPU_Regs.reg_esi, CPU_Regs.reg_esi, CPU_Regs.reg_esi, CPU_Regs.reg_esi, CPU_Regs.reg_esi, CPU_Regs.reg_esi,
            CPU_Regs.reg_edi, CPU_Regs.reg_edi, CPU_Regs.reg_edi, CPU_Regs.reg_edi, CPU_Regs.reg_edi, CPU_Regs.reg_edi, CPU_Regs.reg_edi, CPU_Regs.reg_edi,

            CPU_Regs.reg_eax, CPU_Regs.reg_eax, CPU_Regs.reg_eax, CPU_Regs.reg_eax, CPU_Regs.reg_eax, CPU_Regs.reg_eax, CPU_Regs.reg_eax, CPU_Regs.reg_eax,
            CPU_Regs.reg_ecx, CPU_Regs.reg_ecx, CPU_Regs.reg_ecx, CPU_Regs.reg_ecx, CPU_Regs.reg_ecx, CPU_Regs.reg_ecx, CPU_Regs.reg_ecx, CPU_Regs.reg_ecx,
            CPU_Regs.reg_edx, CPU_Regs.reg_edx, CPU_Regs.reg_edx, CPU_Regs.reg_edx, CPU_Regs.reg_edx, CPU_Regs.reg_edx, CPU_Regs.reg_edx, CPU_Regs.reg_edx,
            CPU_Regs.reg_ebx, CPU_Regs.reg_ebx, CPU_Regs.reg_ebx, CPU_Regs.reg_ebx, CPU_Regs.reg_ebx, CPU_Regs.reg_ebx, CPU_Regs.reg_ebx, CPU_Regs.reg_ebx,
            CPU_Regs.reg_esp, CPU_Regs.reg_esp, CPU_Regs.reg_esp, CPU_Regs.reg_esp, CPU_Regs.reg_esp, CPU_Regs.reg_esp, CPU_Regs.reg_esp, CPU_Regs.reg_esp,
            CPU_Regs.reg_ebp, CPU_Regs.reg_ebp, CPU_Regs.reg_ebp, CPU_Regs.reg_ebp, CPU_Regs.reg_ebp, CPU_Regs.reg_ebp, CPU_Regs.reg_ebp, CPU_Regs.reg_ebp,
            CPU_Regs.reg_esi, CPU_Regs.reg_esi, CPU_Regs.reg_esi, CPU_Regs.reg_esi, CPU_Regs.reg_esi, CPU_Regs.reg_esi, CPU_Regs.reg_esi, CPU_Regs.reg_esi,
            CPU_Regs.reg_edi, CPU_Regs.reg_edi, CPU_Regs.reg_edi, CPU_Regs.reg_edi, CPU_Regs.reg_edi, CPU_Regs.reg_edi, CPU_Regs.reg_edi, CPU_Regs.reg_edi,
    };
//    static public void Getrw(short rm, int value) {
//        switch ((rm / 8) % 8) {
//            case 0: CPU_Regs.reg_eax.word(value);break;
//            case 1: CPU_Regs.reg_ecx.word(value);break;
//            case 2: CPU_Regs.reg_edx.word(value);break;
//            case 3: CPU_Regs.reg_ebx.word(value);break;
//            case 4: CPU_Regs.reg_esp.word(value);break;
//            case 5: CPU_Regs.reg_ebp.word(value);break;
//            case 6: CPU_Regs.reg_esi.word(value);break;
//            case 7: CPU_Regs.reg_edi.word(value);break;
//            default:
//                throw new RuntimeException("WTF "+rm);
//        }
//    }
//    static public int Getrw(short rm) {
//        switch ((rm / 8) % 8) {
//            case 0: return CPU_Regs.reg_eax.word();
//            case 1: return CPU_Regs.reg_ecx.word();
//            case 2: return CPU_Regs.reg_edx.word();
//            case 3: return CPU_Regs.reg_ebx.word();
//            case 4: return CPU_Regs.reg_esp.word();
//            case 5: return CPU_Regs.reg_ebp.word();
//            case 6: return CPU_Regs.reg_esi.word();
//            case 7: return CPU_Regs.reg_edi.word();
//            default:
//                throw new RuntimeException("WTF "+rm);
//        }
//    }

    static public interface Getrd_interface {
        public long get();
        public void set(long value);
    }

    static final public CPU_Regs.Reg[] Getrd = new CPU_Regs.Reg[] {
            CPU_Regs.reg_eax, CPU_Regs.reg_eax, CPU_Regs.reg_eax, CPU_Regs.reg_eax, CPU_Regs.reg_eax, CPU_Regs.reg_eax, CPU_Regs.reg_eax, CPU_Regs.reg_eax,
            CPU_Regs.reg_ecx, CPU_Regs.reg_ecx, CPU_Regs.reg_ecx, CPU_Regs.reg_ecx, CPU_Regs.reg_ecx, CPU_Regs.reg_ecx, CPU_Regs.reg_ecx, CPU_Regs.reg_ecx,
            CPU_Regs.reg_edx, CPU_Regs.reg_edx, CPU_Regs.reg_edx, CPU_Regs.reg_edx, CPU_Regs.reg_edx, CPU_Regs.reg_edx, CPU_Regs.reg_edx, CPU_Regs.reg_edx,
            CPU_Regs.reg_ebx, CPU_Regs.reg_ebx, CPU_Regs.reg_ebx, CPU_Regs.reg_ebx, CPU_Regs.reg_ebx, CPU_Regs.reg_ebx, CPU_Regs.reg_ebx, CPU_Regs.reg_ebx,
            CPU_Regs.reg_esp, CPU_Regs.reg_esp, CPU_Regs.reg_esp, CPU_Regs.reg_esp, CPU_Regs.reg_esp, CPU_Regs.reg_esp, CPU_Regs.reg_esp, CPU_Regs.reg_esp,
            CPU_Regs.reg_ebp, CPU_Regs.reg_ebp, CPU_Regs.reg_ebp, CPU_Regs.reg_ebp, CPU_Regs.reg_ebp, CPU_Regs.reg_ebp, CPU_Regs.reg_ebp, CPU_Regs.reg_ebp,
            CPU_Regs.reg_esi, CPU_Regs.reg_esi, CPU_Regs.reg_esi, CPU_Regs.reg_esi, CPU_Regs.reg_esi, CPU_Regs.reg_esi, CPU_Regs.reg_esi, CPU_Regs.reg_esi,
            CPU_Regs.reg_edi, CPU_Regs.reg_edi, CPU_Regs.reg_edi, CPU_Regs.reg_edi, CPU_Regs.reg_edi, CPU_Regs.reg_edi, CPU_Regs.reg_edi, CPU_Regs.reg_edi,

            CPU_Regs.reg_eax, CPU_Regs.reg_eax, CPU_Regs.reg_eax, CPU_Regs.reg_eax, CPU_Regs.reg_eax, CPU_Regs.reg_eax, CPU_Regs.reg_eax, CPU_Regs.reg_eax,
            CPU_Regs.reg_ecx, CPU_Regs.reg_ecx, CPU_Regs.reg_ecx, CPU_Regs.reg_ecx, CPU_Regs.reg_ecx, CPU_Regs.reg_ecx, CPU_Regs.reg_ecx, CPU_Regs.reg_ecx,
            CPU_Regs.reg_edx, CPU_Regs.reg_edx, CPU_Regs.reg_edx, CPU_Regs.reg_edx, CPU_Regs.reg_edx, CPU_Regs.reg_edx, CPU_Regs.reg_edx, CPU_Regs.reg_edx,
            CPU_Regs.reg_ebx, CPU_Regs.reg_ebx, CPU_Regs.reg_ebx, CPU_Regs.reg_ebx, CPU_Regs.reg_ebx, CPU_Regs.reg_ebx, CPU_Regs.reg_ebx, CPU_Regs.reg_ebx,
            CPU_Regs.reg_esp, CPU_Regs.reg_esp, CPU_Regs.reg_esp, CPU_Regs.reg_esp, CPU_Regs.reg_esp, CPU_Regs.reg_esp, CPU_Regs.reg_esp, CPU_Regs.reg_esp,
            CPU_Regs.reg_ebp, CPU_Regs.reg_ebp, CPU_Regs.reg_ebp, CPU_Regs.reg_ebp, CPU_Regs.reg_ebp, CPU_Regs.reg_ebp, CPU_Regs.reg_ebp, CPU_Regs.reg_ebp,
            CPU_Regs.reg_esi, CPU_Regs.reg_esi, CPU_Regs.reg_esi, CPU_Regs.reg_esi, CPU_Regs.reg_esi, CPU_Regs.reg_esi, CPU_Regs.reg_esi, CPU_Regs.reg_esi,
            CPU_Regs.reg_edi, CPU_Regs.reg_edi, CPU_Regs.reg_edi, CPU_Regs.reg_edi, CPU_Regs.reg_edi, CPU_Regs.reg_edi, CPU_Regs.reg_edi, CPU_Regs.reg_edi,

            CPU_Regs.reg_eax, CPU_Regs.reg_eax, CPU_Regs.reg_eax, CPU_Regs.reg_eax, CPU_Regs.reg_eax, CPU_Regs.reg_eax, CPU_Regs.reg_eax, CPU_Regs.reg_eax,
            CPU_Regs.reg_ecx, CPU_Regs.reg_ecx, CPU_Regs.reg_ecx, CPU_Regs.reg_ecx, CPU_Regs.reg_ecx, CPU_Regs.reg_ecx, CPU_Regs.reg_ecx, CPU_Regs.reg_ecx,
            CPU_Regs.reg_edx, CPU_Regs.reg_edx, CPU_Regs.reg_edx, CPU_Regs.reg_edx, CPU_Regs.reg_edx, CPU_Regs.reg_edx, CPU_Regs.reg_edx, CPU_Regs.reg_edx,
            CPU_Regs.reg_ebx, CPU_Regs.reg_ebx, CPU_Regs.reg_ebx, CPU_Regs.reg_ebx, CPU_Regs.reg_ebx, CPU_Regs.reg_ebx, CPU_Regs.reg_ebx, CPU_Regs.reg_ebx,
            CPU_Regs.reg_esp, CPU_Regs.reg_esp, CPU_Regs.reg_esp, CPU_Regs.reg_esp, CPU_Regs.reg_esp, CPU_Regs.reg_esp, CPU_Regs.reg_esp, CPU_Regs.reg_esp,
            CPU_Regs.reg_ebp, CPU_Regs.reg_ebp, CPU_Regs.reg_ebp, CPU_Regs.reg_ebp, CPU_Regs.reg_ebp, CPU_Regs.reg_ebp, CPU_Regs.reg_ebp, CPU_Regs.reg_ebp,
            CPU_Regs.reg_esi, CPU_Regs.reg_esi, CPU_Regs.reg_esi, CPU_Regs.reg_esi, CPU_Regs.reg_esi, CPU_Regs.reg_esi, CPU_Regs.reg_esi, CPU_Regs.reg_esi,
            CPU_Regs.reg_edi, CPU_Regs.reg_edi, CPU_Regs.reg_edi, CPU_Regs.reg_edi, CPU_Regs.reg_edi, CPU_Regs.reg_edi, CPU_Regs.reg_edi, CPU_Regs.reg_edi,

            CPU_Regs.reg_eax, CPU_Regs.reg_eax, CPU_Regs.reg_eax, CPU_Regs.reg_eax, CPU_Regs.reg_eax, CPU_Regs.reg_eax, CPU_Regs.reg_eax, CPU_Regs.reg_eax,
            CPU_Regs.reg_ecx, CPU_Regs.reg_ecx, CPU_Regs.reg_ecx, CPU_Regs.reg_ecx, CPU_Regs.reg_ecx, CPU_Regs.reg_ecx, CPU_Regs.reg_ecx, CPU_Regs.reg_ecx,
            CPU_Regs.reg_edx, CPU_Regs.reg_edx, CPU_Regs.reg_edx, CPU_Regs.reg_edx, CPU_Regs.reg_edx, CPU_Regs.reg_edx, CPU_Regs.reg_edx, CPU_Regs.reg_edx,
            CPU_Regs.reg_ebx, CPU_Regs.reg_ebx, CPU_Regs.reg_ebx, CPU_Regs.reg_ebx, CPU_Regs.reg_ebx, CPU_Regs.reg_ebx, CPU_Regs.reg_ebx, CPU_Regs.reg_ebx,
            CPU_Regs.reg_esp, CPU_Regs.reg_esp, CPU_Regs.reg_esp, CPU_Regs.reg_esp, CPU_Regs.reg_esp, CPU_Regs.reg_esp, CPU_Regs.reg_esp, CPU_Regs.reg_esp,
            CPU_Regs.reg_ebp, CPU_Regs.reg_ebp, CPU_Regs.reg_ebp, CPU_Regs.reg_ebp, CPU_Regs.reg_ebp, CPU_Regs.reg_ebp, CPU_Regs.reg_ebp, CPU_Regs.reg_ebp,
            CPU_Regs.reg_esi, CPU_Regs.reg_esi, CPU_Regs.reg_esi, CPU_Regs.reg_esi, CPU_Regs.reg_esi, CPU_Regs.reg_esi, CPU_Regs.reg_esi, CPU_Regs.reg_esi,
            CPU_Regs.reg_edi, CPU_Regs.reg_edi, CPU_Regs.reg_edi, CPU_Regs.reg_edi, CPU_Regs.reg_edi, CPU_Regs.reg_edi, CPU_Regs.reg_edi, CPU_Regs.reg_edi
    };

    
//    static public void Getrd(short rm, long value) {
//        switch ((rm / 8) % 8) {
//            case 0: CPU_Regs.reg_eax.dword(value);break;
//            case 1: CPU_Regs.reg_ecx.dword(value);break;
//            case 2: CPU_Regs.reg_edx.dword(value);break;
//            case 3: CPU_Regs.reg_ebx.dword(value);break;
//            case 4: CPU_Regs.reg_esp.dword(value);break;
//            case 5: CPU_Regs.reg_ebp.dword(value);break;
//            case 6: CPU_Regs.reg_esi.dword(value);break;
//            case 7: CPU_Regs.reg_edi.dword(value);break;
//            default:
//                throw new RuntimeException("WTF "+rm);
//        }
//    }
//
//    static public long Getrd(short rm) {
//        switch ((rm / 8) % 8) {
//            case 0: return CPU_Regs.reg_eax.dword();
//            case 1: return CPU_Regs.reg_ecx.dword();
//            case 2: return CPU_Regs.reg_edx.dword();
//            case 3: return CPU_Regs.reg_ebx.dword();
//            case 4: return CPU_Regs.reg_esp.dword();
//            case 5: return CPU_Regs.reg_ebp.dword();
//            case 6: return CPU_Regs.reg_esi.dword();
//            case 7: return CPU_Regs.reg_edi.dword();
//            default:
//                throw new RuntimeException("WTF "+rm);
//        }
//    }

    static final public Getrb_interface[] GetEArb = new Getrb_interface[] {
            /* 12 lines of 16*0 should give nice errors when used */
            null,	null,	null,	null,	null,	null,	null,	null,	null,	null,	null,	null,	null,	null,	null,	null,
            null,	null,	null,	null,	null,	null,	null,	null,	null,	null,	null,	null,	null,	null,	null,	null,
            null,	null,	null,	null,	null,	null,	null,	null,	null,	null,	null,	null,	null,	null,	null,	null,
            null,	null,	null,	null,	null,	null,	null,	null,	null,	null,	null,	null,	null,	null,	null,	null,
            null,	null,	null,	null,	null,	null,	null,	null,	null,	null,	null,	null,	null,	null,	null,	null,
            null,	null,	null,	null,	null,	null,	null,	null,	null,	null,	null,	null,	null,	null,	null,	null,
            null,	null,	null,	null,	null,	null,	null,	null,	null,	null,	null,	null,	null,	null,	null,	null,
            null,	null,	null,	null,	null,	null,	null,	null,	null,	null,	null,	null,	null,	null,	null,	null,
            null,	null,	null,	null,	null,	null,	null,	null,	null,	null,	null,	null,	null,	null,	null,	null,
            null,	null,	null,	null,	null,	null,	null,	null,	null,	null,	null,	null,	null,	null,	null,	null,
            null,	null,	null,	null,	null,	null,	null,	null,	null,	null,	null,	null,	null,	null,	null,	null,
            null,	null,	null,	null,	null,	null,	null,	null,	null,	null,	null,	null,	null,	null,	null,	null,
            al, cl, dl, bl, ah, ch, dh, bh,
            al, cl, dl, bl, ah, ch, dh, bh,
            al, cl, dl, bl, ah, ch, dh, bh,
            al, cl, dl, bl, ah, ch, dh, bh,
            al, cl, dl, bl, ah, ch, dh, bh,
            al, cl, dl, bl, ah, ch, dh, bh,
            al, cl, dl, bl, ah, ch, dh, bh,
            al, cl, dl, bl, ah, ch, dh, bh
    };
//    static public void GetEArb(short index, short value) {
//        /* 12 lines of 16*0 should give nice errors when used */
//        if (index<12*16) throw new NullPointerException();
//        switch (index % 8) {
//            case 0: CPU_Regs.reg_eax.low(value);break;
//            case 1: CPU_Regs.reg_ecx.low(value);break;
//            case 2: CPU_Regs.reg_edx.low(value);break;
//            case 3: CPU_Regs.reg_ebx.low(value);break;
//            case 4: CPU_Regs.reg_eax.high(value);break;
//            case 5: CPU_Regs.reg_ecx.high(value);break;
//            case 6: CPU_Regs.reg_edx.high(value);break;
//            case 7: CPU_Regs.reg_ebx.high(value);break;
//            default:
//                throw new RuntimeException("WTF "+index);
//        }
//    }
//
//    static public short GetEArb(short index) {
//        /* 12 lines of 16*0 should give nice errors when used */
//        if (index<12*16) throw new NullPointerException();
//        switch (index % 8) {
//            case 0: return CPU_Regs.reg_eax.low();
//            case 1: return CPU_Regs.reg_ecx.low();
//            case 2: return CPU_Regs.reg_edx.low();
//            case 3: return CPU_Regs.reg_ebx.low();
//            case 4: return CPU_Regs.reg_eax.high();
//            case 5: return CPU_Regs.reg_ecx.high();
//            case 6: return CPU_Regs.reg_edx.high();
//            case 7: return CPU_Regs.reg_ebx.high();
//            default:
//                throw new RuntimeException("WTF "+index);
//        }
//    }

    static final public CPU_Regs.Reg[] GetEArw = new CPU_Regs.Reg[] {
            /* 12 lines of 16*0 should give nice errors when used */
            null,	null,	null,	null,	null,	null,	null,	null,	null,	null,	null,	null,	null,	null,	null,	null,
            null,	null,	null,	null,	null,	null,	null,	null,	null,	null,	null,	null,	null,	null,	null,	null,
            null,	null,	null,	null,	null,	null,	null,	null,	null,	null,	null,	null,	null,	null,	null,	null,
            null,	null,	null,	null,	null,	null,	null,	null,	null,	null,	null,	null,	null,	null,	null,	null,
            null,	null,	null,	null,	null,	null,	null,	null,	null,	null,	null,	null,	null,	null,	null,	null,
            null,	null,	null,	null,	null,	null,	null,	null,	null,	null,	null,	null,	null,	null,	null,	null,
            null,	null,	null,	null,	null,	null,	null,	null,	null,	null,	null,	null,	null,	null,	null,	null,
            null,	null,	null,	null,	null,	null,	null,	null,	null,	null,	null,	null,	null,	null,	null,	null,
            null,	null,	null,	null,	null,	null,	null,	null,	null,	null,	null,	null,	null,	null,	null,	null,
            null,	null,	null,	null,	null,	null,	null,	null,	null,	null,	null,	null,	null,	null,	null,	null,
            null,	null,	null,	null,	null,	null,	null,	null,	null,	null,	null,	null,	null,	null,	null,	null,
            null,	null,	null,	null,	null,	null,	null,	null,	null,	null,	null,	null,	null,	null,	null,	null,
            CPU_Regs.reg_eax, CPU_Regs.reg_ecx, CPU_Regs.reg_edx, CPU_Regs.reg_ebx, CPU_Regs.reg_esp, CPU_Regs.reg_ebp, CPU_Regs.reg_esi, CPU_Regs.reg_edi,
            CPU_Regs.reg_eax, CPU_Regs.reg_ecx, CPU_Regs.reg_edx, CPU_Regs.reg_ebx, CPU_Regs.reg_esp, CPU_Regs.reg_ebp, CPU_Regs.reg_esi, CPU_Regs.reg_edi,
            CPU_Regs.reg_eax, CPU_Regs.reg_ecx, CPU_Regs.reg_edx, CPU_Regs.reg_ebx, CPU_Regs.reg_esp, CPU_Regs.reg_ebp, CPU_Regs.reg_esi, CPU_Regs.reg_edi,
            CPU_Regs.reg_eax, CPU_Regs.reg_ecx, CPU_Regs.reg_edx, CPU_Regs.reg_ebx, CPU_Regs.reg_esp, CPU_Regs.reg_ebp, CPU_Regs.reg_esi, CPU_Regs.reg_edi,
            CPU_Regs.reg_eax, CPU_Regs.reg_ecx, CPU_Regs.reg_edx, CPU_Regs.reg_ebx, CPU_Regs.reg_esp, CPU_Regs.reg_ebp, CPU_Regs.reg_esi, CPU_Regs.reg_edi,
            CPU_Regs.reg_eax, CPU_Regs.reg_ecx, CPU_Regs.reg_edx, CPU_Regs.reg_ebx, CPU_Regs.reg_esp, CPU_Regs.reg_ebp, CPU_Regs.reg_esi, CPU_Regs.reg_edi,
            CPU_Regs.reg_eax, CPU_Regs.reg_ecx, CPU_Regs.reg_edx, CPU_Regs.reg_ebx, CPU_Regs.reg_esp, CPU_Regs.reg_ebp, CPU_Regs.reg_esi, CPU_Regs.reg_edi,
            CPU_Regs.reg_eax, CPU_Regs.reg_ecx, CPU_Regs.reg_edx, CPU_Regs.reg_ebx, CPU_Regs.reg_esp, CPU_Regs.reg_ebp, CPU_Regs.reg_esi, CPU_Regs.reg_edi
    };
//    static public void GetEArw(short index, int value) {
//        /* 12 lines of 16*0 should give nice errors when used */
//        if (index<12*16) throw new NullPointerException();
//        switch (index % 8) {
//            case 0: CPU_Regs.reg_eax.word(value);break;
//            case 1: CPU_Regs.reg_ecx.word(value);break;
//            case 2: CPU_Regs.reg_edx.word(value);break;
//            case 3: CPU_Regs.reg_ebx.word(value);break;
//            case 4: CPU_Regs.reg_esp.word(value);break;
//            case 5: CPU_Regs.reg_ebp.word(value);break;
//            case 6: CPU_Regs.reg_esi.word(value);break;
//            case 7: CPU_Regs.reg_edi.word(value);break;
//            default:
//                throw new RuntimeException("WTF "+index);
//        }
//    }
//
//    static public int GetEArw(short index) {
//        /* 12 lines of 16*0 should give nice errors when used */
//        if (index<12*16) throw new NullPointerException();
//        switch (index % 8) {
//            case 0: return CPU_Regs.reg_eax.word();
//            case 1: return CPU_Regs.reg_ecx.word();
//            case 2: return CPU_Regs.reg_edx.word();
//            case 3: return CPU_Regs.reg_ebx.word();
//            case 4: return CPU_Regs.reg_esp.word();
//            case 5: return CPU_Regs.reg_ebp.word();
//            case 6: return CPU_Regs.reg_esi.word();
//            case 7: return CPU_Regs.reg_edi.word();
//            default:
//                throw new RuntimeException("WTF "+index);
//        }
//    }

    static final public CPU_Regs.Reg[] GetEArd = new CPU_Regs.Reg[] {
            /* 12 lines of 16*0 should give nice errors when used */
            null,	null,	null,	null,	null,	null,	null,	null,	null,	null,	null,	null,	null,	null,	null,	null,
            null,	null,	null,	null,	null,	null,	null,	null,	null,	null,	null,	null,	null,	null,	null,	null,
            null,	null,	null,	null,	null,	null,	null,	null,	null,	null,	null,	null,	null,	null,	null,	null,
            null,	null,	null,	null,	null,	null,	null,	null,	null,	null,	null,	null,	null,	null,	null,	null,
            null,	null,	null,	null,	null,	null,	null,	null,	null,	null,	null,	null,	null,	null,	null,	null,
            null,	null,	null,	null,	null,	null,	null,	null,	null,	null,	null,	null,	null,	null,	null,	null,
            null,	null,	null,	null,	null,	null,	null,	null,	null,	null,	null,	null,	null,	null,	null,	null,
            null,	null,	null,	null,	null,	null,	null,	null,	null,	null,	null,	null,	null,	null,	null,	null,
            null,	null,	null,	null,	null,	null,	null,	null,	null,	null,	null,	null,	null,	null,	null,	null,
            null,	null,	null,	null,	null,	null,	null,	null,	null,	null,	null,	null,	null,	null,	null,	null,
            null,	null,	null,	null,	null,	null,	null,	null,	null,	null,	null,	null,	null,	null,	null,	null,
            null,	null,	null,	null,	null,	null,	null,	null,	null,	null,	null,	null,	null,	null,	null,	null,
            CPU_Regs.reg_eax, CPU_Regs.reg_ecx, CPU_Regs.reg_edx, CPU_Regs.reg_ebx, CPU_Regs.reg_esp, CPU_Regs.reg_ebp, CPU_Regs.reg_esi, CPU_Regs.reg_edi,
            CPU_Regs.reg_eax, CPU_Regs.reg_ecx, CPU_Regs.reg_edx, CPU_Regs.reg_ebx, CPU_Regs.reg_esp, CPU_Regs.reg_ebp, CPU_Regs.reg_esi, CPU_Regs.reg_edi,
            CPU_Regs.reg_eax, CPU_Regs.reg_ecx, CPU_Regs.reg_edx, CPU_Regs.reg_ebx, CPU_Regs.reg_esp, CPU_Regs.reg_ebp, CPU_Regs.reg_esi, CPU_Regs.reg_edi,
            CPU_Regs.reg_eax, CPU_Regs.reg_ecx, CPU_Regs.reg_edx, CPU_Regs.reg_ebx, CPU_Regs.reg_esp, CPU_Regs.reg_ebp, CPU_Regs.reg_esi, CPU_Regs.reg_edi,
            CPU_Regs.reg_eax, CPU_Regs.reg_ecx, CPU_Regs.reg_edx, CPU_Regs.reg_ebx, CPU_Regs.reg_esp, CPU_Regs.reg_ebp, CPU_Regs.reg_esi, CPU_Regs.reg_edi,
            CPU_Regs.reg_eax, CPU_Regs.reg_ecx, CPU_Regs.reg_edx, CPU_Regs.reg_ebx, CPU_Regs.reg_esp, CPU_Regs.reg_ebp, CPU_Regs.reg_esi, CPU_Regs.reg_edi,
            CPU_Regs.reg_eax, CPU_Regs.reg_ecx, CPU_Regs.reg_edx, CPU_Regs.reg_ebx, CPU_Regs.reg_esp, CPU_Regs.reg_ebp, CPU_Regs.reg_esi, CPU_Regs.reg_edi,
            CPU_Regs.reg_eax, CPU_Regs.reg_ecx, CPU_Regs.reg_edx, CPU_Regs.reg_ebx, CPU_Regs.reg_esp, CPU_Regs.reg_ebp, CPU_Regs.reg_esi, CPU_Regs.reg_edi
    };

    static public interface Move {
        public void call();
    }

//    static final public Move[] earb_to_rb = new Move[] {
//            null,	null,	null,	null,	null,	null,	null,	null,	null,	null,	null,	null,	null,	null,	null,	null,
//            null,	null,	null,	null,	null,	null,	null,	null,	null,	null,	null,	null,	null,	null,	null,	null,
//            null,	null,	null,	null,	null,	null,	null,	null,	null,	null,	null,	null,	null,	null,	null,	null,
//            null,	null,	null,	null,	null,	null,	null,	null,	null,	null,	null,	null,	null,	null,	null,	null,
//            null,	null,	null,	null,	null,	null,	null,	null,	null,	null,	null,	null,	null,	null,	null,	null,
//            null,	null,	null,	null,	null,	null,	null,	null,	null,	null,	null,	null,	null,	null,	null,	null,
//            null,	null,	null,	null,	null,	null,	null,	null,	null,	null,	null,	null,	null,	null,	null,	null,
//            null,	null,	null,	null,	null,	null,	null,	null,	null,	null,	null,	null,	null,	null,	null,	null,
//            null,	null,	null,	null,	null,	null,	null,	null,	null,	null,	null,	null,	null,	null,	null,	null,
//            null,	null,	null,	null,	null,	null,	null,	null,	null,	null,	null,	null,	null,	null,	null,	null,
//            null,	null,	null,	null,	null,	null,	null,	null,	null,	null,	null,	null,	null,	null,	null,	null,
//            null,	null,	null,	null,	null,	null,	null,	null,	null,	null,	null,	null,	null,	null,	null,	null,
//            new Move() {final public void call() { /*CPU_Regs.reg_eax.low(CPU_Regs.reg_eax.low());*/ }},
//            new Move() {final public void call() { CPU_Regs.reg_eax.low(CPU_Regs.reg_ecx.low()); }},
//            new Move() {final public void call() { CPU_Regs.reg_eax.low(CPU_Regs.reg_edx.low()); }},
//            new Move() {final public void call() { CPU_Regs.reg_eax.low(CPU_Regs.reg_ebx.low()); }},
//            new Move() {final public void call() { CPU_Regs.reg_eax.low(CPU_Regs.reg_eax.high()); }},
//            new Move() {final public void call() { CPU_Regs.reg_eax.low(CPU_Regs.reg_ecx.high()); }},
//            new Move() {final public void call() { CPU_Regs.reg_eax.low(CPU_Regs.reg_edx.high()); }},
//            new Move() {final public void call() { CPU_Regs.reg_eax.low(CPU_Regs.reg_ebx.high()); }},
//
//            new Move() {final public void call() { CPU_Regs.reg_ecx.low(CPU_Regs.reg_eax.low()); }},
//            new Move() {final public void call() { CPU_Regs.reg_ecx.low(CPU_Regs.reg_ecx.low()); }},
//            new Move() {final public void call() { CPU_Regs.reg_ecx.low(CPU_Regs.reg_edx.low()); }},
//            new Move() {final public void call() { CPU_Regs.reg_ecx.low(CPU_Regs.reg_ebx.low()); }},
//            new Move() {final public void call() { CPU_Regs.reg_ecx.low(CPU_Regs.reg_eax.high()); }},
//            new Move() {final public void call() { CPU_Regs.reg_ecx.low(CPU_Regs.reg_ecx.high()); }},
//            new Move() {final public void call() { CPU_Regs.reg_ecx.low(CPU_Regs.reg_edx.high()); }},
//            new Move() {final public void call() { CPU_Regs.reg_ecx.low(CPU_Regs.reg_ebx.high()); }},
//
//            new Move() {final public void call() { CPU_Regs.reg_edx.low(CPU_Regs.reg_eax.low()); }},
//            new Move() {final public void call() { CPU_Regs.reg_edx.low(CPU_Regs.reg_ecx.low()); }},
//            new Move() {final public void call() { CPU_Regs.reg_edx.low(CPU_Regs.reg_edx.low()); }},
//            new Move() {final public void call() { CPU_Regs.reg_edx.low(CPU_Regs.reg_ebx.low()); }},
//            new Move() {final public void call() { CPU_Regs.reg_edx.low(CPU_Regs.reg_eax.high()); }},
//            new Move() {final public void call() { CPU_Regs.reg_edx.low(CPU_Regs.reg_ecx.high()); }},
//            new Move() {final public void call() { CPU_Regs.reg_edx.low(CPU_Regs.reg_edx.high()); }},
//            new Move() {final public void call() { CPU_Regs.reg_edx.low(CPU_Regs.reg_ebx.high()); }},
//
//            new Move() {final public void call() { CPU_Regs.reg_ebx.low(CPU_Regs.reg_eax.low()); }},
//            new Move() {final public void call() { CPU_Regs.reg_ebx.low(CPU_Regs.reg_ecx.low()); }},
//            new Move() {final public void call() { CPU_Regs.reg_ebx.low(CPU_Regs.reg_edx.low()); }},
//            new Move() {final public void call() { CPU_Regs.reg_ebx.low(CPU_Regs.reg_ebx.low()); }},
//            new Move() {final public void call() { CPU_Regs.reg_ebx.low(CPU_Regs.reg_eax.high()); }},
//            new Move() {final public void call() { CPU_Regs.reg_ebx.low(CPU_Regs.reg_ecx.high()); }},
//            new Move() {final public void call() { CPU_Regs.reg_ebx.low(CPU_Regs.reg_edx.high()); }},
//            new Move() {final public void call() { CPU_Regs.reg_ebx.low(CPU_Regs.reg_ebx.high()); }},
//
//            new Move() {final public void call() { CPU_Regs.reg_eax.high(CPU_Regs.reg_eax.low()); }},
//            new Move() {final public void call() { CPU_Regs.reg_eax.high(CPU_Regs.reg_ecx.low()); }},
//            new Move() {final public void call() { CPU_Regs.reg_eax.high(CPU_Regs.reg_edx.low()); }},
//            new Move() {final public void call() { CPU_Regs.reg_eax.high(CPU_Regs.reg_ebx.low()); }},
//            new Move() {final public void call() { CPU_Regs.reg_eax.high(CPU_Regs.reg_eax.high()); }},
//            new Move() {final public void call() { CPU_Regs.reg_eax.high(CPU_Regs.reg_ecx.high()); }},
//            new Move() {final public void call() { CPU_Regs.reg_eax.high(CPU_Regs.reg_edx.high()); }},
//            new Move() {final public void call() { CPU_Regs.reg_eax.high(CPU_Regs.reg_ebx.high()); }},
//
//            new Move() {final public void call() { CPU_Regs.reg_ecx.high(CPU_Regs.reg_eax.low()); }},
//            new Move() {final public void call() { CPU_Regs.reg_ecx.high(CPU_Regs.reg_ecx.low()); }},
//            new Move() {final public void call() { CPU_Regs.reg_ecx.high(CPU_Regs.reg_edx.low()); }},
//            new Move() {final public void call() { CPU_Regs.reg_ecx.high(CPU_Regs.reg_ebx.low()); }},
//            new Move() {final public void call() { CPU_Regs.reg_ecx.high(CPU_Regs.reg_eax.high()); }},
//            new Move() {final public void call() { CPU_Regs.reg_ecx.high(CPU_Regs.reg_ecx.high()); }},
//            new Move() {final public void call() { CPU_Regs.reg_ecx.high(CPU_Regs.reg_edx.high()); }},
//            new Move() {final public void call() { CPU_Regs.reg_ecx.high(CPU_Regs.reg_ebx.high()); }},
//
//            new Move() {final public void call() { CPU_Regs.reg_edx.high(CPU_Regs.reg_eax.low()); }},
//            new Move() {final public void call() { CPU_Regs.reg_edx.high(CPU_Regs.reg_ecx.low()); }},
//            new Move() {final public void call() { CPU_Regs.reg_edx.high(CPU_Regs.reg_edx.low()); }},
//            new Move() {final public void call() { CPU_Regs.reg_edx.high(CPU_Regs.reg_ebx.low()); }},
//            new Move() {final public void call() { CPU_Regs.reg_edx.high(CPU_Regs.reg_eax.high()); }},
//            new Move() {final public void call() { CPU_Regs.reg_edx.high(CPU_Regs.reg_ecx.high()); }},
//            new Move() {final public void call() { CPU_Regs.reg_edx.high(CPU_Regs.reg_edx.high()); }},
//            new Move() {final public void call() { CPU_Regs.reg_edx.high(CPU_Regs.reg_ebx.high()); }},
//
//            new Move() {final public void call() { CPU_Regs.reg_ebx.high(CPU_Regs.reg_eax.low()); }},
//            new Move() {final public void call() { CPU_Regs.reg_ebx.high(CPU_Regs.reg_ecx.low()); }},
//            new Move() {final public void call() { CPU_Regs.reg_ebx.high(CPU_Regs.reg_edx.low()); }},
//            new Move() {final public void call() { CPU_Regs.reg_ebx.high(CPU_Regs.reg_ebx.low()); }},
//            new Move() {final public void call() { CPU_Regs.reg_ebx.high(CPU_Regs.reg_eax.high()); }},
//            new Move() {final public void call() { CPU_Regs.reg_ebx.high(CPU_Regs.reg_ecx.high()); }},
//            new Move() {final public void call() { CPU_Regs.reg_ebx.high(CPU_Regs.reg_edx.high()); }},
//            new Move() {final public void call() { CPU_Regs.reg_ebx.high(CPU_Regs.reg_ebx.high()); }},
//    };
    static final public Move[] eard_to_rd = new Move[] {
            null,	null,	null,	null,	null,	null,	null,	null,	null,	null,	null,	null,	null,	null,	null,	null,
            null,	null,	null,	null,	null,	null,	null,	null,	null,	null,	null,	null,	null,	null,	null,	null,
            null,	null,	null,	null,	null,	null,	null,	null,	null,	null,	null,	null,	null,	null,	null,	null,
            null,	null,	null,	null,	null,	null,	null,	null,	null,	null,	null,	null,	null,	null,	null,	null,
            null,	null,	null,	null,	null,	null,	null,	null,	null,	null,	null,	null,	null,	null,	null,	null,
            null,	null,	null,	null,	null,	null,	null,	null,	null,	null,	null,	null,	null,	null,	null,	null,
            null,	null,	null,	null,	null,	null,	null,	null,	null,	null,	null,	null,	null,	null,	null,	null,
            null,	null,	null,	null,	null,	null,	null,	null,	null,	null,	null,	null,	null,	null,	null,	null,
            null,	null,	null,	null,	null,	null,	null,	null,	null,	null,	null,	null,	null,	null,	null,	null,
            null,	null,	null,	null,	null,	null,	null,	null,	null,	null,	null,	null,	null,	null,	null,	null,
            null,	null,	null,	null,	null,	null,	null,	null,	null,	null,	null,	null,	null,	null,	null,	null,
            null,	null,	null,	null,	null,	null,	null,	null,	null,	null,	null,	null,	null,	null,	null,	null,
            new Move() {final public void call() { /*CPU_Regs.reg_eax.dword = CPU_Regs.reg_eax.dword;*/ }},
            new Move() {final public void call() { CPU_Regs.reg_eax.dword=CPU_Regs.reg_ecx.dword; }},
            new Move() {final public void call() { CPU_Regs.reg_eax.dword=CPU_Regs.reg_edx.dword; }},
            new Move() {final public void call() { CPU_Regs.reg_eax.dword=CPU_Regs.reg_ebx.dword; }},
            new Move() {final public void call() { CPU_Regs.reg_eax.dword=CPU_Regs.reg_esp.dword; }},
            new Move() {final public void call() { CPU_Regs.reg_eax.dword=CPU_Regs.reg_ebp.dword; }},
            new Move() {final public void call() { CPU_Regs.reg_eax.dword=CPU_Regs.reg_esi.dword; }},
            new Move() {final public void call() { CPU_Regs.reg_eax.dword=CPU_Regs.reg_edi.dword; }},

            new Move() {final public void call() { CPU_Regs.reg_ecx.dword=CPU_Regs.reg_eax.dword; }},
            new Move() {final public void call() { /*CPU_Regs.reg_ecx.dword = CPU_Regs.reg_ecx.dword;*/ }},
            new Move() {final public void call() { CPU_Regs.reg_ecx.dword=CPU_Regs.reg_edx.dword; }},
            new Move() {final public void call() { CPU_Regs.reg_ecx.dword=CPU_Regs.reg_ebx.dword; }},
            new Move() {final public void call() { CPU_Regs.reg_ecx.dword=CPU_Regs.reg_esp.dword; }},
            new Move() {final public void call() { CPU_Regs.reg_ecx.dword=CPU_Regs.reg_ebp.dword; }},
            new Move() {final public void call() { CPU_Regs.reg_ecx.dword=CPU_Regs.reg_esi.dword; }},
            new Move() {final public void call() { CPU_Regs.reg_ecx.dword=CPU_Regs.reg_edi.dword; }},

            new Move() {final public void call() { CPU_Regs.reg_edx.dword=CPU_Regs.reg_eax.dword; }},
            new Move() {final public void call() { CPU_Regs.reg_edx.dword=CPU_Regs.reg_ecx.dword; }},
            new Move() {final public void call() { /*CPU_Regs.reg_edx.dword = CPU_Regs.reg_edx.dword;*/ }},
            new Move() {final public void call() { CPU_Regs.reg_edx.dword=CPU_Regs.reg_ebx.dword; }},
            new Move() {final public void call() { CPU_Regs.reg_edx.dword=CPU_Regs.reg_esp.dword; }},
            new Move() {final public void call() { CPU_Regs.reg_edx.dword=CPU_Regs.reg_ebp.dword; }},
            new Move() {final public void call() { CPU_Regs.reg_edx.dword=CPU_Regs.reg_esi.dword; }},
            new Move() {final public void call() { CPU_Regs.reg_edx.dword=CPU_Regs.reg_edi.dword; }},

            new Move() {final public void call() { CPU_Regs.reg_ebx.dword=CPU_Regs.reg_eax.dword; }},
            new Move() {final public void call() { CPU_Regs.reg_ebx.dword=CPU_Regs.reg_ecx.dword; }},
            new Move() {final public void call() { CPU_Regs.reg_ebx.dword=CPU_Regs.reg_edx.dword; }},
            new Move() {final public void call() { /*CPU_Regs.reg_ebx.dword = CPU_Regs.reg_ebx.dword;*/ }},
            new Move() {final public void call() { CPU_Regs.reg_ebx.dword=CPU_Regs.reg_esp.dword; }},
            new Move() {final public void call() { CPU_Regs.reg_ebx.dword=CPU_Regs.reg_ebp.dword; }},
            new Move() {final public void call() { CPU_Regs.reg_ebx.dword=CPU_Regs.reg_esi.dword; }},
            new Move() {final public void call() { CPU_Regs.reg_ebx.dword=CPU_Regs.reg_edi.dword; }},

            new Move() {final public void call() { CPU_Regs.reg_esp.dword=CPU_Regs.reg_eax.dword; }},
            new Move() {final public void call() { CPU_Regs.reg_esp.dword=CPU_Regs.reg_ecx.dword; }},
            new Move() {final public void call() { CPU_Regs.reg_esp.dword=CPU_Regs.reg_edx.dword; }},
            new Move() {final public void call() { CPU_Regs.reg_esp.dword=CPU_Regs.reg_ebx.dword; }},
            new Move() {final public void call() { /*CPU_Regs.reg_esp.dword = CPU_Regs.reg_esp.dword;*/ }},
            new Move() {final public void call() { CPU_Regs.reg_esp.dword=CPU_Regs.reg_ebp.dword; }},
            new Move() {final public void call() { CPU_Regs.reg_esp.dword=CPU_Regs.reg_esi.dword; }},
            new Move() {final public void call() { CPU_Regs.reg_esp.dword=CPU_Regs.reg_edi.dword; }},

            new Move() {final public void call() { CPU_Regs.reg_ebp.dword=CPU_Regs.reg_eax.dword; }},
            new Move() {final public void call() { CPU_Regs.reg_ebp.dword=CPU_Regs.reg_ecx.dword; }},
            new Move() {final public void call() { CPU_Regs.reg_ebp.dword=CPU_Regs.reg_edx.dword; }},
            new Move() {final public void call() { CPU_Regs.reg_ebp.dword=CPU_Regs.reg_ebx.dword; }},
            new Move() {final public void call() { CPU_Regs.reg_ebp.dword=CPU_Regs.reg_esp.dword; }},
            new Move() {final public void call() { /*CPU_Regs.reg_ebp.dword = CPU_Regs.reg_ebp.dword;*/ }},
            new Move() {final public void call() { CPU_Regs.reg_ebp.dword=CPU_Regs.reg_esi.dword; }},
            new Move() {final public void call() { CPU_Regs.reg_ebp.dword=CPU_Regs.reg_edi.dword; }},

            new Move() {final public void call() { CPU_Regs.reg_esi.dword=CPU_Regs.reg_eax.dword; }},
            new Move() {final public void call() { CPU_Regs.reg_esi.dword=CPU_Regs.reg_ecx.dword; }},
            new Move() {final public void call() { CPU_Regs.reg_esi.dword=CPU_Regs.reg_edx.dword; }},
            new Move() {final public void call() { CPU_Regs.reg_esi.dword=CPU_Regs.reg_ebx.dword; }},
            new Move() {final public void call() { CPU_Regs.reg_esi.dword=CPU_Regs.reg_esp.dword; }},
            new Move() {final public void call() { CPU_Regs.reg_esi.dword=CPU_Regs.reg_ebp.dword; }},
            new Move() {final public void call() { /*CPU_Regs.reg_esi.dword = CPU_Regs.reg_esi.dword;*/ }},
            new Move() {final public void call() { CPU_Regs.reg_esi.dword=CPU_Regs.reg_edi.dword; }},

            new Move() {final public void call() { CPU_Regs.reg_edi.dword=CPU_Regs.reg_eax.dword; }},
            new Move() {final public void call() { CPU_Regs.reg_edi.dword=CPU_Regs.reg_ecx.dword; }},
            new Move() {final public void call() { CPU_Regs.reg_edi.dword=CPU_Regs.reg_edx.dword; }},
            new Move() {final public void call() { CPU_Regs.reg_edi.dword=CPU_Regs.reg_ebx.dword; }},
            new Move() {final public void call() { CPU_Regs.reg_edi.dword=CPU_Regs.reg_esp.dword; }},
            new Move() {final public void call() { CPU_Regs.reg_edi.dword=CPU_Regs.reg_ebp.dword; }},
            new Move() {final public void call() { CPU_Regs.reg_edi.dword=CPU_Regs.reg_esi.dword; }},
            new Move() {final public void call() { /*CPU_Regs.reg_edi.dword = CPU_Regs.reg_edi.dword;*/ }},
    };
//    static public void GetEArd(short index, long value) {
//        /* 12 lines of 16*0 should give nice errors when used */
//        if (index<12*16) throw new NullPointerException();
//        switch (index % 8) {
//            case 0: CPU_Regs.reg_eax.dword(value);break;
//            case 1: CPU_Regs.reg_ecx.dword(value);break;
//            case 2: CPU_Regs.reg_edx.dword(value);break;
//            case 3: CPU_Regs.reg_ebx.dword(value);break;
//            case 4: CPU_Regs.reg_esp.dword(value);break;
//            case 5: CPU_Regs.reg_ebp.dword(value);break;
//            case 6: CPU_Regs.reg_esi.dword(value);break;
//            case 7: CPU_Regs.reg_edi.dword(value);break;
//            default:
//                throw new RuntimeException("WTF "+index);
//        }
//    }
//
//    static public long GetEArd(short index) {
//        /* 12 lines of 16*0 should give nice errors when used */
//        if (index<12*16) throw new NullPointerException();
//        switch (index % 8) {
//            case 0: return CPU_Regs.reg_eax.dword();
//            case 1: return CPU_Regs.reg_ecx.dword();
//            case 2: return CPU_Regs.reg_edx.dword();
//            case 3: return CPU_Regs.reg_ebx.dword();
//            case 4: return CPU_Regs.reg_esp.dword();
//            case 5: return CPU_Regs.reg_ebp.dword();
//            case 6: return CPU_Regs.reg_esi.dword();
//            case 7: return CPU_Regs.reg_edi.dword();
//            default:
//                throw new RuntimeException("WTF "+index);
//        }
//    }
}
