package jdos.cpu.core_dynamic;

import jdos.cpu.CPU;
import jdos.cpu.CPU_Regs;
import jdos.cpu.Core;
import jdos.cpu.Paging;
import jdos.hardware.Memory;
import jdos.misc.setup.Config;

public class Strings extends Core {
    final static public class Movsw32r extends Op {
        public static void doString() {
            int add_index= CPU.cpu.direction<<1;
            int count = CPU_Regs.reg_ecx.dword;
            int si_base = base_ds;
            int di_base = CPU.Segs_ESphys;
            if (Config.FAST_STRINGS) {
                while (count>1) {
                    int dst = di_base + reg_edi.dword;
                    int src = si_base + reg_esi.dword;
                    int dst_index = Paging.getDirectIndex(dst);
                    int src_index = Paging.getDirectIndexRO(src);
                    int src_len;
                    int dst_len;

                    if (dst_index<0 || src_index<0) {
                        break;
                    }
                    if (Math.abs(src_index-dst_index)<2) {
                        break; // don't support overlapping word moves
                    }
                    int len = count << 1;
                    if (add_index<0) {
                        if (Math.abs(src_index-dst_index)<len) {
                            break;// don't support overlapping in reverse direction
                        }
                        src_len = (src & 0xFFF)+1;
                        dst_len = (dst & 0xFFF)+1;
                    } else {
                        src_len = 0x1000-(src & 0xFFF);
                        dst_len = 0x1000-(dst & 0xFFF);
                    }
                    if (len>src_len)
                        len = src_len;
                    if (len>dst_len)
                        len = dst_len;
                    len = len & ~1;
                    if (len<=0) {
                        // Part of the read or write crosses a boundary
                        Memory.mem_writew(di_base + reg_edi.dword, Memory.mem_readw(si_base + reg_esi.dword));
                        reg_edi.dword+=add_index;
                        reg_esi.dword+=add_index;
                        reg_ecx.dword--;
                        count--;
                    } else {
                        int thisCount = (len>>1);
                        if (Math.abs(src_index-dst_index)<len) {
                            // Overlapping read/write
                            for (int i=0;i<thisCount;i++) {
                                Memory.host_writew(dst_index, Memory.host_readw(src_index));
                                dst_index+=add_index;
                                src_index+=add_index;
                            }
                        } else {
                            if (add_index<0) {
                                // On the reverse direction, start from the bottom
                                src_index-=len-2;
                                dst_index-=len-2;
                            }
                            Memory.host_memcpy(dst_index, src_index, len);
                        }
                        if (add_index<0) {
                            reg_edi.dword-=len;
                            reg_esi.dword-=len;
                        } else {
                            reg_edi.dword+=len;
                            reg_esi.dword+=len;
                        }
                        reg_ecx.dword-=thisCount;
                        count-=thisCount;
                    }
                }
            }
            for (;count>0;count--) {
                Memory.mem_writew(di_base + reg_edi.dword, Memory.mem_readw(si_base + reg_esi.dword));
                reg_edi.dword+=add_index;
                reg_esi.dword+=add_index;
                reg_ecx.dword--;
            }
        }
        public int call() {
            doString();
            CPU_Regs.reg_eip+=eip_count;return next.call();
        }

        public boolean throwsException() {return false;}
        public boolean accessesMemory() {return true;}
        public boolean usesEip() {return false;}
        public boolean setsEip() {return false;}
    }

    final static public class Movsw32 extends Op {
        public static void doString() {
            int add_index= CPU.cpu.direction<<1;
            Memory.mem_writew(CPU.Segs_ESphys + reg_edi.dword, Memory.mem_readw(base_ds + reg_esi.dword));
            reg_edi.dword+=add_index;
            reg_esi.dword+=add_index;
        }
        public int call() {
            doString();
            CPU_Regs.reg_eip+=eip_count;return next.call();
        }

        public boolean throwsException() {return false;}
        public boolean accessesMemory() {return true;}
        public boolean usesEip() {return false;}
        public boolean setsEip() {return false;}
    }

    final static public class Movsw16r extends Op {
        public static void doString() {
            int add_index= CPU.cpu.direction<<1;
            int count = CPU_Regs.reg_ecx.word();
            int si_base = base_ds;
            int di_base = CPU.Segs_ESphys;
            if (Config.FAST_STRINGS) {
                while (count>1) {
                    int dst = di_base + reg_edi.word();
                    int src = si_base + reg_esi.word();
                    int dst_index = Paging.getDirectIndex(dst);
                    int src_index = Paging.getDirectIndexRO(src);
                    int src_len;
                    int dst_len;

                    if (dst_index<0 || src_index<0) {
                        break;
                    }
                    int len = count << 1;
                    if (add_index<0) {
                        src_len = (src & 0xFFF)+1;
                        dst_len = (dst & 0xFFF)+1;
                    } else {
                        src_len = 0x1000-(src & 0xFFF);
                        dst_len = 0x1000-(dst & 0xFFF);
                    }
                    if (len>src_len)
                        len = src_len;
                    if (len>dst_len)
                        len = dst_len;
                    if (add_index<0) {
                        int ediCount = reg_edi.word()+1;
                        int esiCount = reg_esi.word()+1;
                        if (len>ediCount)
                            len = ediCount;
                        if (len>esiCount)
                            len = esiCount;
                        len = len & ~1;
                    } else {
                        int ediCount = 0x10000 - reg_edi.word();
                        int esiCount = 0x10000 - reg_esi.word();
                        if (len>ediCount)
                            len = ediCount;
                        if (len>esiCount)
                            len = esiCount;
                        len = len & ~1;
                    }
                    if (len<=0) {
                        // Part of the read or write crosses a boundary
                        Memory.mem_writew(di_base + reg_edi.word(), Memory.mem_readw(si_base + reg_esi.word()));
                        reg_edi.word(reg_edi.word()+add_index);
                        reg_esi.word(reg_esi.word()+add_index);
                        reg_ecx.word_dec();
                        count--;
                    } else {
                        int thisCount = (len>>1);
                        if (Math.abs(src_index-dst_index)<len) {
                            // Overlapping read/write
                            for (int i=0;i<thisCount;i++) {
                                Memory.host_writew(dst_index, Memory.host_readw(src_index));
                                dst_index+=add_index;
                                src_index+=add_index;
                            }
                        } else {
                            if (add_index<0) {
                                // On the reverse direction, start from the bottom
                                src_index-=len-2;
                                dst_index-=len-2;
                            }
                            Memory.host_memcpy(dst_index, src_index, len);
                        }
                        if (add_index<0) {
                            reg_edi.word(reg_edi.word()-len);
                            reg_esi.word(reg_esi.word()-len);
                        } else {
                            reg_edi.word(reg_edi.word()+len);
                            reg_esi.word(reg_esi.word()+len);
                        }
                        reg_ecx.word(reg_ecx.word()-thisCount);
                        count-=thisCount;
                    }
                }
            }
            for (;count>0;count--) {
                Memory.mem_writew(di_base + reg_edi.word(), Memory.mem_readw(si_base + reg_esi.word()));
                reg_edi.word(reg_edi.word()+add_index);
                reg_esi.word(reg_esi.word()+add_index);
                reg_ecx.word_dec();
            }
        }
        public int call() {
            doString();
            CPU_Regs.reg_eip+=eip_count;return next.call();
        }

        public boolean throwsException() {return false;}
        public boolean accessesMemory() {return true;}
        public boolean usesEip() {return false;}
        public boolean setsEip() {return false;}
    }

    final static public class Movsw16 extends Op {
        public static void doString() {
            int add_index= CPU.cpu.direction*2;
            int si_base = base_ds;
            int di_base = CPU.Segs_ESphys;
            Memory.mem_writew(di_base + reg_edi.word(), Memory.mem_readw(si_base + reg_esi.word()));
            reg_edi.word(reg_edi.word()+add_index);
            reg_esi.word(reg_esi.word()+add_index);
        }
        public int call() {
            doString();
            CPU_Regs.reg_eip+=eip_count;return next.call();
        }

        public boolean throwsException() {return false;}
        public boolean accessesMemory() {return true;}
        public boolean usesEip() {return false;}
        public boolean setsEip() {return false;}
    }

    final static public class Movsd32r extends Op {
        public static void doString() {
            int add_index= CPU.cpu.direction<<2;
            int count = CPU_Regs.reg_ecx.dword;
            int si_base = base_ds;
            int di_base = CPU.Segs_ESphys;
            if (Config.FAST_STRINGS) {
                while (count>1) {
                    int dst = di_base + reg_edi.dword;
                    int src = si_base + reg_esi.dword;
                    int dst_index = Paging.getDirectIndex(dst);
                    int src_index = Paging.getDirectIndexRO(src);
                    int src_len;
                    int dst_len;

                    if (dst_index<0 || src_index<0) {
                        break;
                    }
                    int len = count << 2;
                    if (add_index<0) {
                        src_len = (src & 0xFFF)+1;
                        dst_len = (dst & 0xFFF)+1;
                    } else {
                        src_len = 0x1000-(src & 0xFFF);
                        dst_len = 0x1000-(dst & 0xFFF);
                    }
                    if (len>src_len)
                        len = src_len;
                    if (len>dst_len)
                        len = dst_len;
                    len = len & ~3;
                    if (len<=0) {
                        // Part of the read or write crosses a boundary
                        Memory.mem_writed(di_base + reg_edi.dword, Memory.mem_readd(si_base + reg_esi.dword));
                        reg_edi.dword+=add_index;
                        reg_esi.dword+=add_index;
                        reg_ecx.dword--;
                        count--;
                    } else {
                        int thisCount = (len>>2);
                        if (Math.abs(src_index-dst_index)<len) {
                            // Overlapping read/write
                            for (int i=0;i<thisCount;i++) {
                                Memory.host_writed(dst_index, Memory.host_readd(src_index));
                                dst_index+=add_index;
                                src_index+=add_index;
                            }
                        } else {
                            // On the reverse direction, start from the bottom
                            if (add_index<0) {
                                src_index-=len-4;
                                dst_index-=len-4;
                            }
                            Memory.host_memcpy(dst_index, src_index, len);
                        }
                        if (add_index<0) {
                            reg_edi.dword-=len;
                            reg_esi.dword-=len;
                        } else {
                            reg_edi.dword+=len;
                            reg_esi.dword+=len;
                        }
                        reg_ecx.dword-=thisCount;
                        count-=thisCount;
                    }
                }
            }
            for (;count>0;count--) {
                Memory.mem_writed(di_base + reg_edi.dword, Memory.mem_readd(si_base + reg_esi.dword));
                reg_edi.dword+=add_index;
                reg_esi.dword+=add_index;
                reg_ecx.dword--;
            }
        }
        public int call() {
            doString();
            CPU_Regs.reg_eip+=eip_count;return next.call();
        }

        public boolean throwsException() {return false;}
        public boolean accessesMemory() {return true;}
        public boolean usesEip() {return false;}
        public boolean setsEip() {return false;}
    }

    final static public class Movsd32 extends Op {
        public static void doString() {
            int add_index= CPU.cpu.direction<<2;
            Memory.mem_writed(CPU.Segs_ESphys + reg_edi.dword, Memory.mem_readd(base_ds + reg_esi.dword));
            reg_edi.dword+=add_index;
            reg_esi.dword+=add_index;
        }
        public int call() {
            doString();
            CPU_Regs.reg_eip+=eip_count;return next.call();
        }

        public boolean throwsException() {return false;}
        public boolean accessesMemory() {return true;}
        public boolean usesEip() {return false;}
        public boolean setsEip() {return false;}
    }

    final static public class Movsd16r extends Op {
        public static void doString() {
            int add_index= CPU.cpu.direction*4;
            int count = CPU_Regs.reg_ecx.word();
            int si_base = base_ds;
            int di_base = CPU.Segs_ESphys;
            if (Config.FAST_STRINGS) {
                while (count>1) {
                    int dst = di_base + reg_edi.word();
                    int src = si_base + reg_esi.word();
                    int dst_index = Paging.getDirectIndex(dst);
                    int src_index = Paging.getDirectIndexRO(src);
                    int src_len;
                    int dst_len;

                    if (dst_index<0 || src_index<0) {
                        break;
                    }
                    int len = count << 2;
                    if (add_index<0) {
                        src_len = (src & 0xFFF)+1;
                        dst_len = (dst & 0xFFF)+1;
                    } else {
                        src_len = 0x1000-(src & 0xFFF);
                        dst_len = 0x1000-(dst & 0xFFF);
                    }
                    if (len>src_len)
                        len = src_len;
                    if (len>dst_len)
                        len = dst_len;
                    if (add_index<0) {
                        int ediCount = reg_edi.word()+1;
                        int esiCount = reg_esi.word()+1;
                        if (len>ediCount)
                            len = ediCount;
                        if (len>esiCount)
                            len = esiCount;
                        len = len & ~3;
                    } else {
                        int ediCount = 0x10000 - reg_edi.word();
                        int esiCount = 0x10000 - reg_esi.word();
                        if (len>ediCount)
                            len = ediCount;
                        if (len>esiCount)
                            len = esiCount;
                        len = len & ~3;
                    }
                    if (len<=0) {
                        // Part of the read or write crosses a boundary
                        Memory.mem_writed(di_base + reg_edi.word(), Memory.mem_readd(si_base + reg_esi.word()));
                        reg_edi.word(reg_edi.word()+add_index);
                        reg_esi.word(reg_esi.word()+add_index);
                        reg_ecx.word_dec();
                        count--;
                    } else {
                        int thisCount = (len>>2);
                        if (Math.abs(src_index-dst_index)<len) {
                            // Overlapping read/write
                            for (int i=0;i<thisCount;i++) {
                                Memory.host_writed(dst_index, Memory.host_readd(src_index));
                                dst_index+=add_index;
                                src_index+=add_index;
                            }
                        } else {
                            // On the reverse direction, start from the bottom
                            if (add_index<0) {
                                src_index-=len-4;
                                dst_index-=len-4;
                            }
                            Memory.host_memcpy(dst_index, src_index, len);
                        }
                        if (add_index<0) {
                            reg_edi.word(reg_edi.word()-len);
                            reg_esi.word(reg_esi.word()-len);
                        } else {
                            reg_edi.word(reg_edi.word()+len);
                            reg_esi.word(reg_esi.word()+len);
                        }
                        reg_ecx.word(reg_ecx.word()-thisCount);
                        count-=thisCount;
                    }
                }
            }
            for (;count>0;count--) {
                Memory.mem_writed(di_base + reg_edi.word(), Memory.mem_readd(si_base + reg_esi.word()));
                reg_edi.word(reg_edi.word()+add_index);
                reg_esi.word(reg_esi.word()+add_index);
                reg_ecx.word_dec();
            }
        }
        public int call() {
            doString();
            CPU_Regs.reg_eip+=eip_count;return next.call();
        }

        public boolean throwsException() {return false;}
        public boolean accessesMemory() {return true;}
        public boolean usesEip() {return false;}
        public boolean setsEip() {return false;}
    }

    final static public class Movsd16 extends Op {
        public static void doString() {
            int add_index= CPU.cpu.direction<<2;
            int si_base = base_ds;
            int di_base = CPU.Segs_ESphys;
            Memory.mem_writed(di_base + reg_edi.word(), Memory.mem_readd(si_base + reg_esi.word()));
            reg_edi.word(reg_edi.word()+add_index);
            reg_esi.word(reg_esi.word()+add_index);
        }
        public int call() {
            doString();
            CPU_Regs.reg_eip+=eip_count;return next.call();
        }

        public boolean throwsException() {return false;}
        public boolean accessesMemory() {return true;}
        public boolean usesEip() {return false;}
        public boolean setsEip() {return false;}
    }

    final static public class Movsb32r extends Op {
        public static void doString() {
            int add_index= CPU.cpu.direction;
            int count = CPU_Regs.reg_ecx.dword;
            int si_base = base_ds;
            int di_base = CPU.Segs_ESphys;
            if (Config.FAST_STRINGS) {
                while (count>1) {
                    int dst = di_base + reg_edi.dword;
                    int src = si_base + reg_esi.dword;
                    int dst_index = Paging.getDirectIndex(dst);
                    int src_index = Paging.getDirectIndexRO(src);
                    int src_len;
                    int dst_len;

                    if (dst_index<0 || src_index<0) {
                        break;
                    }
                    int len = count;
                    if (add_index<0) {
                        src_len = (src & 0xFFF)+1;
                        dst_len = (dst & 0xFFF)+1;
                    } else {
                        src_len = 0x1000-(src & 0xFFF);
                        dst_len = 0x1000-(dst & 0xFFF);
                    }
                    if (len>src_len)
                        len = src_len;
                    if (len>dst_len)
                        len = dst_len;
                    if (Math.abs(src_index-dst_index)<len) {
                        // Overlapping read/write
                        for (int i=0;i<len;i++) {
                            Memory.host_writeb(dst_index, Memory.host_readb(src_index));
                            dst_index+=add_index;
                            src_index+=add_index;
                        }
                    } else {
                        // On the reverse direction, start from the bottom
                        if (add_index<0) {
                            src_index-=len-1;
                            dst_index-=len-1;
                        }
                        Memory.host_memcpy(dst_index, src_index, len);
                    }
                    if (add_index<0) {
                        reg_edi.dword-=len;
                        reg_esi.dword-=len;
                    } else {
                        reg_edi.dword+=len;
                        reg_esi.dword+=len;
                    }
                    reg_ecx.dword-=len;
                    count-=len;
                }
            }
            for (;count>0;count--) {
                Memory.mem_writeb(di_base + reg_edi.dword, Memory.mem_readb(si_base + reg_esi.dword));
                reg_edi.dword+=add_index;
                reg_esi.dword+=add_index;
                reg_ecx.dword--;
            }
        }
        public int call() {
            doString();
            CPU_Regs.reg_eip+=eip_count;return next.call();
        }

        public boolean throwsException() {return false;}
        public boolean accessesMemory() {return true;}
        public boolean usesEip() {return false;}
        public boolean setsEip() {return false;}
    }

    final static public class Movsb32 extends Op {
        public static void doString() {
            int add_index= CPU.cpu.direction;
            int si_base = base_ds;
            int di_base = CPU.Segs_ESphys;
            Memory.mem_writeb(di_base+reg_edi.dword,Memory.mem_readb(si_base+reg_esi.dword));
            reg_edi.dword+=add_index;
            reg_esi.dword+=add_index;
        }
        public int call() {
            doString();
            CPU_Regs.reg_eip+=eip_count;return next.call();
        }
        public boolean throwsException() {return false;}
        public boolean accessesMemory() {return true;}
        public boolean usesEip() {return false;}
        public boolean setsEip() {return false;}
    }

    final static public class Movsb16r extends Op {
        public static void doString() {
            int add_index= CPU.cpu.direction;
            int count = CPU_Regs.reg_ecx.word();
            int si_base = base_ds;
            int di_base = CPU.Segs_ESphys;
            if (Config.FAST_STRINGS) {
                while (count>1) {
                    int dst = di_base + reg_edi.word();
                    int src = si_base + reg_esi.word();
                    int dst_index = Paging.getDirectIndex(dst);
                    int src_index = Paging.getDirectIndexRO(src);
                    int src_len;
                    int dst_len;

                    if (dst_index<0 || src_index<0) {
                        break;
                    }
                    int len = count;
                    if (add_index<0) {
                        src_len = (src & 0xFFF)+1;
                        dst_len = (dst & 0xFFF)+1;
                    } else {
                        src_len = 0x1000-(src & 0xFFF);
                        dst_len = 0x1000-(dst & 0xFFF);
                    }
                    if (len>src_len)
                        len = src_len;
                    if (len>dst_len)
                        len = dst_len;
                    if (add_index<0) {
                        int ediCount = reg_edi.word()+1;
                        int esiCount = reg_esi.word()+1;
                        if (len>ediCount)
                            len = ediCount;
                        if (len>esiCount)
                            len = esiCount;
                    } else {
                        int ediCount = 0x10000 - reg_edi.word();
                        int esiCount = 0x10000 - reg_esi.word();
                        if (len>ediCount)
                            len = ediCount;
                        if (len>esiCount)
                            len = esiCount;
                    }
                    if (Math.abs(src_index-dst_index)<len) {
                        // Overlapping read/write
                        for (int i=0;i<len;i++) {
                            Memory.host_writeb(dst_index, Memory.host_readb(src_index));
                            dst_index+=add_index;
                            src_index+=add_index;
                        }
                    } else {
                        // On the reverse direction, start from the bottom
                        if (add_index<0) {
                            src_index-=len-1;
                            dst_index-=len-1;
                        }
                        Memory.host_memcpy(dst_index, src_index, len);
                    }
                    if (add_index<0) {
                        reg_edi.word(reg_edi.word()-len);
                        reg_esi.word(reg_esi.word()-len);
                    } else {
                        reg_edi.word(reg_edi.word()+len);
                        reg_esi.word(reg_esi.word()+len);
                    }
                    reg_ecx.word(reg_ecx.word()-len);
                    count-=len;
                }
            }
            for (;count>0;count--) {
                Memory.mem_writeb(di_base + reg_edi.word(), Memory.mem_readb(si_base + reg_esi.word()));
                reg_edi.word(reg_edi.word()+add_index);
                reg_esi.word(reg_esi.word()+add_index);
                reg_ecx.word_dec();
            }
        }
        public int call() {
            doString();
            CPU_Regs.reg_eip+=eip_count;return next.call();
        }
        public boolean throwsException() {return false;}
        public boolean accessesMemory() {return true;}
        public boolean usesEip() {return false;}
        public boolean setsEip() {return false;}
    }

    final static public class Movsb16 extends Op {
        public static void doString() {
            int add_index= CPU.cpu.direction;
            int si_base = base_ds;
            int di_base = CPU.Segs_ESphys;
            Memory.mem_writeb(di_base+reg_edi.word(),Memory.mem_readb(si_base+reg_esi.word()));
            reg_edi.word(reg_edi.word()+add_index);
            reg_esi.word(reg_esi.word()+add_index);
        }
        public int call() {
            doString();
            CPU_Regs.reg_eip+=eip_count;return next.call();
        }
        public boolean throwsException() {return false;}
        public boolean accessesMemory() {return true;}
        public boolean usesEip() {return false;}
        public boolean setsEip() {return false;}
    }
}
