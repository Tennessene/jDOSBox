package jdos.cpu;

import jdos.Dosbox;
import jdos.hardware.Memory;
import jdos.misc.Log;
import jdos.misc.setup.Config;
import jdos.misc.setup.Module_base;
import jdos.misc.setup.Section;
import jdos.types.LogSeverities;
import jdos.types.LogTypes;
import jdos.util.IntRef;
import jdos.util.LongRef;
import jdos.util.ShortRef;

public class Paging extends Module_base {
    public static final int MEM_PAGE_SIZE = 4096;
    public static final int XMS_START = 0x110;

    public static final int PFLAG_READABLE=		0x1;
    public static final int PFLAG_WRITEABLE=	0x2;
    public static final int PFLAG_HASROM=		0x4;
    public static final int PFLAG_HASCODE=		0x8;				//Page contains dynamic code
    public static final int PFLAG_NOCODE=		0x10;			//No dynamic code can be generated here
    public static final int PFLAG_INIT=			0x20;			//No dynamic code can be generated here

    static public final int PAGING_LINKS = (128*1024/4);
    static public final int TLB_SIZE;
    static public final int BANK_SHIFT = 28;
    static public final int BANK_MASK = 0xffff; // always the same as TLB_SIZE-1?
    static public final int TLB_BANKS;

    static public final int LINK_START = ((1024+64)/4); //Start right after the HMA
    static {
        if (Config.USE_FULL_TLB) {
            TLB_SIZE = (1024*1024);
        } else {
            TLB_SIZE = 65536;	// This must a power of 2 and greater then LINK_START
        }
        TLB_BANKS = ((1024*1024/TLB_SIZE)-1);
    }
    static public class PageHandler {
        public /*Bitu*/int readb(/*PhysPt*/long addr) {
            Log.exit("No byte handler for read from "+addr);
	        return 0;
        }
        public /*Bitu*/int readw(/*PhysPt*/long addr) {
            return readb(addr+0) | (readb(addr+1) << 8);
        }
        public /*Bitu*/long readd(/*PhysPt*/long addr) {
            return readb(addr+0) | (readb(addr+1) << 8) | (readb(addr+2) << 16) | (readb(addr+3) << 24);
        }
        public void writeb(/*PhysPt*/long addr,/*Bitu*/int val) {
            Log.exit("No byte handler for write to "+addr);
        }
        public void writew(/*PhysPt*/long addr,/*Bitu*/int val) {
            writeb(addr+0,(val));
	        writeb(addr+1,(val >> 8));
        }
        public void writed(/*PhysPt*/long addr,/*Bitu*/int val) {
            writeb(addr+0,(val));
	        writeb(addr+1,(val >> 8));
	        writeb(addr+2,(val >> 16));
	        writeb(addr+3,(val >> 24));
        }
        public /*HostPt*/long GetHostReadPt(/*Bitu*/int phys_page) {
            return 0;
        }
        public /*HostPt*/long GetHostWritePt(/*Bitu*/int phys_page) {
            return 0;
        }
        public boolean readb_checked(/*PhysPt*/long addr,/*Bit8u*/ShortRef val) {
            val.value=(short)readb(addr);
            return false;
        }
        public boolean readw_checked(/*PhysPt*/long addr,/*Bit16u*/IntRef val) {
            val.value=readw(addr);
            return false;
        }
        public boolean readd_checked(/*PhysPt*/long addr,/*Bit32u*/LongRef val) {
            val.value=readd(addr);
            return false;
        }
        public boolean writeb_checked(/*PhysPt*/long addr,/*Bitu*/int val) {
            writeb(addr,val);
            return false;
        }
        public boolean writew_checked(/*PhysPt*/long addr,/*Bitu*/int val) {
            writew(addr,val);
            return false;
        }
        public boolean writed_checked(/*PhysPt*/long addr,/*Bitu*/int val) {
            writed(addr,val);
            return false;
        }
        public /*Bitu*/int flags;
    }

    public static class X86_PageEntryBlock{
        /*Bit32u*/int		p;
        /*Bit32u*/int		wr;
        /*Bit32u*/int		us;
        /*Bit32u*/int		pwt;
        /*Bit32u*/int		pcd;
        /*Bit32u*/int		a;
        /*Bit32u*/int		d;
        /*Bit32u*/int		pat;
        /*Bit32u*/int		g;
        /*Bit32u*/int		avl;
        /*Bit32u*/int		base;
    }

    public static class X86PageEntry {

        public int load() {
            int load = 0;

            load |= block.p & 0x1;
            load |= (block.wr & 0x1) << 1;
            load |= (block.us & 0x1) << 2;
            load |= (block.pwt & 0x1) << 3;
            load |= (block.pcd & 0x1) << 4;
            load |= (block.a & 0x1) << 5;
            load |= (block.d & 0x1) << 6;
            load |= (block.pat & 0x1) << 7;
            load |= (block.g & 0x1) << 8;
            load |= (block.avl & 0x7) << 9;
            load |= (block.base & 0xFFFFF) << 12;
            return load;
        }
	    public void load(int value) {
            block.p = value & 0x1;
            block.wr = (value >> 1) & 0x1;
            block.us = (value >> 2) & 0x1;
            block.pwt = (value >> 3) & 0x1;
            block.pcd = (value >> 4) & 0x1;
            block.a = (value >> 5) & 0x1;
            block.d = (value >> 6) & 0x1;
            block.pat = (value >> 7) & 0x1;
            block.g = (value >> 8) & 0x1;
            block.avl = (value >> 9) & 0x7;
            block.base = (value >>> 12) & 0xFFFFF;
        }
    	X86_PageEntryBlock block = new X86_PageEntryBlock();
    }

    static public class PagingBlock {
        public PagingBlock() {
            tlb = new Tlb();
        }
	    public /*Bitu*/long			cr3;
	    public /*Bitu*/long			cr2;
	    public class Base {
		    public /*Bitu*/long page;
		    public /*PhysPt*/long addr;
	    }
        public Base base = new Base();
        public class Tlb {
            public /*HostPt*/int[] read = new int[TLB_SIZE];
            public /*HostPt*/int[] write = new int[TLB_SIZE];
            public PageHandler[] readhandler = new PageHandler[TLB_SIZE];
            public PageHandler[] writehandler = new PageHandler[TLB_SIZE];
            public /*Bit32u*/int[] phys_page = new int[TLB_SIZE];
        }
        public Tlb tlb;

        public class Links {
            public /*Bitu*/int used;
            public /*Bit32u*/long[] entries = new long[PAGING_LINKS];
        }
        public Links links = new Links();
    	public /*Bit32u*/long[]	firstmb = new long[LINK_START];
	    public boolean enabled;
    }

    private static /*HostPt*/int get_tlb_read(/*PhysPt*/int address) {
        return paging.tlb.read[address>>>12];
    }
    private static /*HostPt*/int get_tlb_write(/*PhysPt*/int address) {
        return paging.tlb.write[address>>>12];
    }
    private static PageHandler get_tlb_readhandler(/*PhysPt*/int address) {
        return paging.tlb.readhandler[address>>>12];
    }
    private static PageHandler get_tlb_writehandler(/*PhysPt*/int address) {
        return paging.tlb.writehandler[address>>>12];
    }

    /* Use these helper functions to access linear addresses in readX/writeX functions */
    public static /*PhysPt*/long PAGING_GetPhysicalPage(/*PhysPt*/long linePage) {
        return (paging.tlb.phys_page[(int)(linePage>>12)]<<12);
    }

    public static /*PhysPt*/long PAGING_GetPhysicalAddress(/*PhysPt*/long linAddr) {
        return (paging.tlb.phys_page[(int)(linAddr>>12)]<<12)|(linAddr&0xfff);
    }

    /* Special inlined memory reading/writing */

    public static int getDirectIndex(long address) {
        address&=0xFFFFFFFFl;
        int a = (int)address;
        /*HostPt*/int tlb_addr=get_tlb_read(a);
        if (tlb_addr!=Integer.MIN_VALUE) return (int)(tlb_addr+address);
        get_tlb_readhandler(a).readb(address);
        tlb_addr=get_tlb_read(a);
        return (int)(tlb_addr+address);
    }

    public static /*Bit8u*/short mem_readb_inline(/*PhysPt*/long address) {
        address&=0xFFFFFFFFl; // truncate larger than int
        int a = (int)address;
        /*HostPt*/int tlb_addr=get_tlb_read(a);
        if (tlb_addr!=Integer.MIN_VALUE) return Memory.host_readb((int)(tlb_addr+address)); // a might be negative when paging so use original long value
        else return (short)(get_tlb_readhandler(a)).readb(address);
    }

    public static /*Bit16u*/int mem_readw_inline(/*PhysPt*/long address) {
        address&=0xFFFFFFFFl;
        int a = (int)address;
        if ((a & 0xfff)<0xfff) {
            /*HostPt*/int tlb_addr=get_tlb_read(a);
            if (tlb_addr!=Integer.MIN_VALUE) return Memory.host_readw((int)(tlb_addr+address));
            else return (get_tlb_readhandler(a)).readw(address);
        } else return Memory.mem_unalignedreadw(address);
    }

    public static /*Bit32u*/long mem_readd_inline(/*PhysPt*/long address) {
        address&=0xFFFFFFFFl;
        int a = (int)address;
        if ((a & 0xfff)<0xffd) {
            /*HostPt*/int tlb_addr=get_tlb_read(a);
            if (tlb_addr!=Integer.MIN_VALUE) return Memory.host_readd((int)(tlb_addr+address));
            else return (get_tlb_readhandler(a)).readd(address);
        } else return Memory.mem_unalignedreadd(address);
    }

    public static void mem_writeb_inline(/*PhysPt*/long address,/*Bit8u*/short val) {
        address&=0xFFFFFFFFl;
        int a = (int)address;
        /*HostPt*/int tlb_addr=get_tlb_write(a);
        if (tlb_addr!=Integer.MIN_VALUE) Memory.host_writeb((int)(tlb_addr+address),val);
        else (get_tlb_writehandler(a)).writeb(address,val);
    }

    public static void mem_writew_inline(/*PhysPt*/long address,/*Bit16u*/int val) {
        address&=0xFFFFFFFFl;
        int a = (int)address;
        if ((a & 0xfff)<0xfff) {
            /*HostPt*/int tlb_addr=get_tlb_write(a);
            if (tlb_addr!=Integer.MIN_VALUE) Memory.host_writew((int)(tlb_addr+address),val);
            else (get_tlb_writehandler(a)).writew(address,val);
        } else Memory.mem_unalignedwritew(address,val);
    }

    public static void mem_writed_inline(/*PhysPt*/long address,/*Bit32u*/long val) {
        address&=0xFFFFFFFFl;
        int a = (int)address;
        if ((a & 0xfff)<0xffd) {
            /*HostPt*/int tlb_addr=get_tlb_write(a);
            if (tlb_addr!=Integer.MIN_VALUE) Memory.host_writed((int)(tlb_addr+address),val);
            else (get_tlb_writehandler(a)).writed(address,(int)val);
        } else Memory.mem_unalignedwrited(address,val);
    }


    public static boolean mem_readb_checked(/*PhysPt*/long address, /*Bit8u*/ShortRef val) {
        int a = (int)address;
        /*HostPt*/int tlb_addr=get_tlb_read(a);
        if (tlb_addr!=Integer.MIN_VALUE) {
            val.value=Memory.host_readb((int)(tlb_addr+address));
            return false;
        } else return (get_tlb_readhandler(a)).readb_checked(address, val);
    }

    public static boolean mem_readw_checked(/*PhysPt*/long address, /*Bit16u*/IntRef val) {
        int a = (int)address;
        if ((a & 0xfff)<0xfff) {
            /*HostPt*/int tlb_addr=get_tlb_read(a);
            if (tlb_addr!=Integer.MIN_VALUE) {
                val.value=Memory.host_readw((int)(tlb_addr+address));
                return false;
            } else return (get_tlb_readhandler(a)).readw_checked(address, val);
        } else return Memory.mem_unalignedreadw_checked(address, val);
    }

    public static boolean mem_readd_checked(/*PhysPt*/long address, /*Bit32u*/LongRef val) {
        int a = (int)address;
        if ((a & 0xfff)<0xffd) {
            /*HostPt*/int tlb_addr=get_tlb_read(a);
            if (tlb_addr!=Integer.MIN_VALUE) {
                val.value=Memory.host_readd((int)(tlb_addr+address));
                return false;
            } else return (get_tlb_readhandler(a)).readd_checked(address, val);
        } else return Memory.mem_unalignedreadd_checked(address, val);
    }

    public static boolean mem_writeb_checked(/*PhysPt*/long address,/*Bit8u*/short val) {
        int a = (int)address;
        /*HostPt*/int tlb_addr=get_tlb_write(a);
        if (tlb_addr!=Integer.MIN_VALUE) {
            Memory.host_writeb((int)(tlb_addr+address),val);
            return false;
        } else return (get_tlb_writehandler(a)).writeb_checked(address,val);
    }

    public static boolean mem_writew_checked(/*PhysPt*/long address,/*Bit16u*/int val) {
        int a = (int)address;
        if ((a & 0xfff)<0xfff) {
            /*HostPt*/int tlb_addr=get_tlb_write(a);
            if (tlb_addr!=Integer.MIN_VALUE) {
                Memory.host_writew((int)(tlb_addr+address),val);
                return false;
            } else return (get_tlb_writehandler(a)).writew_checked(address,val);
        } else return Memory.mem_unalignedwritew_checked(address,val);
    }

    public static boolean mem_writed_checked(/*PhysPt*/long address,/*Bit32u*/long val) {
        int a = (int)address;
        if ((a & 0xfff)<0xffd) {
            /*HostPt*/int tlb_addr=get_tlb_write(a);
            if (tlb_addr!=Integer.MIN_VALUE) {
                Memory.host_writed((int)(tlb_addr+address),val);
                return false;
            } else return (get_tlb_writehandler(a)).writed_checked(address,(int)val);
	} else return Memory.mem_unalignedwrited_checked(address,val);
}

    private static final int LINK_TOTAL = (64*1024);

    private static boolean USERWRITE_PROHIBITED() {
        return((CPU.cpu.cpl&CPU.cpu.mpl)==3);
    }

    static public PagingBlock paging;

    static private class PF_Entry {
        /*Bitu*/long cs;
        /*Bitu*/long eip;
        /*Bitu*/long page_addr;
        /*Bitu*/long mpl;
    }

    static private final int PF_QUEUESIZE = 16;
    static private class Pf_queue {
        public Pf_queue() {
            for (int i=0;i<entries.length;i++)
                entries[i] = new PF_Entry();
        }
        /*Bitu*/int used;
        PF_Entry[] entries = new PF_Entry[PF_QUEUESIZE];
    }
    static private Pf_queue pf_queue = new Pf_queue();

    static private CPU.CPU_Decoder PageFaultCore = new CPU.CPU_Decoder() {
        public /*Bits*/int call() {
            CPU.CPU_CycleLeft+=CPU.CPU_Cycles;
            CPU.CPU_Cycles=1;
            /*Bits*/int ret=Core_full.CPU_Core_Full_Run.call();
            CPU.CPU_CycleLeft+=CPU.CPU_Cycles;
            if (ret<0) Log.exit("Got a dosbox close machine in pagefault core?");
            if (ret!=0)
                return ret;
            if (pf_queue.used==0) Log.exit("PF Core without PF");
            PF_Entry entry=pf_queue.entries[pf_queue.used-1];
            X86PageEntry pentry = new X86PageEntry();
            pentry.load((int)Memory.phys_readd(entry.page_addr));
            if (pentry.block.p!=0 && entry.cs == CPU.Segs_CSval && entry.eip==CPU_Regs.reg_eip()) {
                CPU.cpu.mpl=entry.mpl;
                return -1;
            }
            return 0;
        }
    };

    static private boolean first=false;

    static private void PAGING_PageFault(/*PhysPt*/long lin_addr,/*Bitu*/long page_addr,/*Bitu*/int faultcode) {
        /* Save the state of the cpu cores */
        LazyFlags old_lflags = new LazyFlags(Flags.lflags);
        CPU.CPU_Decoder old_cpudecoder;
        old_cpudecoder=CPU.cpudecoder;
        CPU.cpudecoder=PageFaultCore;
        paging.cr2=lin_addr;
        PF_Entry entry=pf_queue.entries[pf_queue.used++];
        if (Log.level<=LogSeverities.LOG_NORMAL) Log.log(LogTypes.LOG_PAGING, LogSeverities.LOG_NORMAL,"PageFault at "+Long.toString(lin_addr, 16)+" type ["+Integer.toString(faultcode,16)+"] queue "+pf_queue.used);
//	LOG_MSG("EAX:%04X ECX:%04X EDX:%04X EBX:%04X",reg_eax,reg_ecx,reg_edx,reg_ebx);
//	LOG_MSG("CS:%04X EIP:%08X SS:%04x SP:%08X",SegValue(cs),reg_eip,SegValue(ss),reg_esp);
        entry.cs=CPU.Segs_CSval;
        entry.eip=CPU_Regs.reg_eip();
        entry.page_addr=page_addr;
        entry.mpl=CPU.cpu.mpl;
        CPU.cpu.mpl=3;

        CPU.CPU_Exception(CPU.EXCEPTION_PF,faultcode);
        if (Config.C_DEBUG) {
        //	DEBUG_EnableDebugger();
        }
        Core_full.pushState();
        Dosbox.DOSBOX_RunMachine();
        Core_full.popState();
        pf_queue.used--;
        if (Log.level<=LogSeverities.LOG_NORMAL) Log.log(LogTypes.LOG_PAGING,LogSeverities.LOG_NORMAL,"Left PageFault for "+Long.toString(lin_addr, 16)+" queue "+pf_queue.used);
        Flags.lflags.copy(old_lflags);
        CPU.cpudecoder=old_cpudecoder;
//	LOG_MSG("SS:%04x SP:%08X",SegValue(ss),reg_esp);
    }

    static private void InitPageUpdateLink(/*Bitu*/int relink,/*PhysPt*/long addr) {
        if (relink==0) return;
        if (paging.links.used != 0) {
            if (paging.links.entries[paging.links.used-1]==(addr>>12)) {
                paging.links.used--;
                PAGING_UnlinkPages((int)(addr>>12),1);
            }
        }
        if (relink>1) PAGING_LinkPage_ReadOnly((int)(addr>>12),relink);
    }

    static private void InitPageCheckPresence(/*PhysPt*/long lin_addr,boolean writing,X86PageEntry table,X86PageEntry entry) {
        /*Bitu*/int lin_page=(int)(lin_addr >> 12);
        /*Bitu*/int d_index=lin_page >> 10;
        /*Bitu*/int t_index=lin_page & 0x3ff;
        /*Bitu*/long table_addr=(paging.base.page<<12)+d_index*4;
        table.load((int)Memory.phys_readd(table_addr));
        if (table.block.p==0) {
            Log.log(LogTypes.LOG_PAGING,LogSeverities.LOG_NORMAL,"NP Table");
            PAGING_PageFault(lin_addr,table_addr,
                (writing?0x02:0x00) | (((CPU.cpu.cpl & CPU.cpu.mpl)==0)?0x00:0x04));
            table.load((int)Memory.phys_readd(table_addr));
            if (table.block.p==0)
                Log.exit("Pagefault didn't correct table");
        }
        /*Bitu*/int entry_addr=(table.block.base<<12)+t_index*4;
        entry.load((int)Memory.phys_readd(entry_addr));
        if (entry.block.p==0) {
//		Log.log(LogTypes.LOG_PAGING,LogSeverities.LOG_NORMAL,"NP Page");
            PAGING_PageFault(lin_addr,entry_addr,
                (writing?0x02:0x00) | (((CPU.cpu.cpl & CPU.cpu.mpl)==0)?0x00:0x04));
            entry.load((int)Memory.phys_readd(entry_addr));
            if (entry.block.p==0)
                Log.exit("Pagefault didn't correct page");
        }
    }

    static private boolean InitPageCheckPresence_CheckOnly(/*PhysPt*/long lin_addr,boolean writing,X86PageEntry table,X86PageEntry entry) {
        /*Bitu*/int lin_page=(int)(lin_addr >> 12);
        /*Bitu*/int d_index=lin_page >> 10;
        /*Bitu*/int t_index=lin_page & 0x3ff;
        /*Bitu*/long table_addr=(paging.base.page<<12)+d_index*4;
        table.load((int)Memory.phys_readd(table_addr));
        if (table.block.p==0) {
            paging.cr2=lin_addr;
            CPU.cpu.exception.which=CPU.EXCEPTION_PF;
            CPU.cpu.exception.error=(writing?0x02:0x00) | (((CPU.cpu.cpl & CPU.cpu.mpl)==0)?0x00:0x04);
            return false;
        }
        /*Bitu*/int entry_addr=(table.block.base<<12)+t_index*4;
        entry.load((int)Memory.phys_readd(entry_addr));
        if (entry.block.p==0) {
            paging.cr2=lin_addr;
            CPU.cpu.exception.which=CPU.EXCEPTION_PF;
            CPU.cpu.exception.error=(writing?0x02:0x00) | (((CPU.cpu.cpl & CPU.cpu.mpl)==0)?0x00:0x04);
            return false;
        }
        return true;
    }

    // check if a user-level memory access would trigger a privilege page fault
    static private boolean InitPage_CheckUseraccess(/*Bitu*/int u1,/*Bitu*/int u2) {
        switch (CPU.CPU_ArchitectureType) {
        case CPU.CPU_ARCHTYPE_MIXED:
        case CPU.CPU_ARCHTYPE_386SLOW:
        case CPU.CPU_ARCHTYPE_386FAST:
        default:
            return ((u1)==0) && ((u2)==0);
        case CPU.CPU_ARCHTYPE_486OLDSLOW:
        case CPU.CPU_ARCHTYPE_486NEWSLOW:
        case CPU.CPU_ARCHTYPE_PENTIUMSLOW:
            return ((u1)==0) || ((u2)==0);
        }
    }


    static private class InitPageHandler extends PageHandler {
        public InitPageHandler() {
            flags=PFLAG_INIT|PFLAG_NOCODE;
        }
        public /*Bitu*/int readb(/*PhysPt*/long addr) {
            /*Bitu*/int needs_reset=InitPage(addr,false);
            /*Bit8u*/short val=Memory.mem_readb(addr);
            InitPageUpdateLink(needs_reset,addr);
            return val;
        }
        public /*Bitu*/int readw(/*PhysPt*/long addr) {
            /*Bitu*/int needs_reset=InitPage(addr,false);
            /*Bit16u*/int val=Memory.mem_readw(addr);
            InitPageUpdateLink(needs_reset,addr);
            return val;
        }
        public /*Bitu*/long readd(/*PhysPt*/long addr) {
            /*Bitu*/int needs_reset=InitPage(addr,false);
            /*Bit32u*/long val=Memory.mem_readd(addr);
            InitPageUpdateLink(needs_reset,addr);
            return val;
        }
        public void writeb(/*PhysPt*/long addr,/*Bitu*/int val) {
            /*Bitu*/int needs_reset=InitPage(addr,true);
            Memory.mem_writeb(addr,val);
            InitPageUpdateLink(needs_reset,addr);
        }
        public void writew(/*PhysPt*/long addr,/*Bitu*/int val) {
            /*Bitu*/int needs_reset=InitPage(addr,true);
            Memory.mem_writew(addr,val);
            InitPageUpdateLink(needs_reset,addr);
        }
        public void writed(/*PhysPt*/long addr,/*Bitu*/int val) {
            /*Bitu*/int needs_reset=InitPage(addr,true);
            Memory.mem_writed(addr,val);
            InitPageUpdateLink(needs_reset,addr);
        }
        public boolean readb_checked(/*PhysPt*/long addr, ShortRef val) {
            if (InitPageCheckOnly(addr,false)) {
                val.value=Memory.mem_readb(addr);
                return false;
            } else return true;
        }
        public boolean readw_checked(/*PhysPt*/long addr, IntRef val) {
            if (InitPageCheckOnly(addr,false)){
                val.value=Memory.mem_readw(addr);
                return false;
            } else return true;
        }
        public boolean readd_checked(/*PhysPt*/long addr, LongRef val) {
            if (InitPageCheckOnly(addr,false)) {
                val.value=Memory.mem_readd(addr);
                return false;
            } else return true;
        }
        public boolean writeb_checked(/*PhysPt*/long addr,/*Bitu*/int val) {
            if (InitPageCheckOnly(addr,true)) {
                Memory.mem_writeb(addr,val);
                return false;
            } else return true;
        }
        public boolean writew_checked(/*PhysPt*/long addr,/*Bitu*/int val) {
            if (InitPageCheckOnly(addr,true)) {
                Memory.mem_writew(addr,val);
                return false;
            } else return true;
        }
        public boolean writed_checked(/*PhysPt*/long addr,/*Bitu*/int val) {
            if (InitPageCheckOnly(addr,true)) {
                Memory.mem_writed(addr,val);
                return false;
            } else return true;
        }
        public /*Bitu*/int InitPage(/*Bitu*/long lin_addr,boolean writing) {
            /*Bitu*/int lin_page=(int)(lin_addr >>> 12);
            /*Bitu*/int phys_page;
            if (paging.enabled) {
                X86PageEntry table=new X86PageEntry();
                X86PageEntry entry=new X86PageEntry();
                InitPageCheckPresence(lin_addr,writing,table,entry);

                // 0: no action
                // 1: can (but currently does not) fail a user-level access privilege check
                // 2: can (but currently does not) fail a write privilege check
                // 3: fails a privilege check
                /*Bitu*/int priv_check=0;
                if (InitPage_CheckUseraccess(entry.block.us,table.block.us)) {
                    if ((CPU.cpu.cpl & CPU.cpu.mpl)==3) priv_check=3;
                    else {
                        switch (CPU.CPU_ArchitectureType) {
                        case CPU.CPU_ARCHTYPE_MIXED:
                        case CPU.CPU_ARCHTYPE_386FAST:
                        default:
//						priv_check=0;	// default
                            break;
                        case CPU.CPU_ARCHTYPE_386SLOW:
                        case CPU.CPU_ARCHTYPE_486OLDSLOW:
                        case CPU.CPU_ARCHTYPE_486NEWSLOW:
                        case CPU.CPU_ARCHTYPE_PENTIUMSLOW:
                            priv_check=1;
                            break;
                        }
                    }
                }
                if ((entry.block.wr==0) || (table.block.wr==0)) {
                    // page is write-protected for user mode
                    if (priv_check==0) {
                        switch (CPU.CPU_ArchitectureType) {
                        case CPU.CPU_ARCHTYPE_MIXED:
                        case CPU.CPU_ARCHTYPE_386FAST:
                        default:
//						priv_check=0;	// default
                            break;
                        case CPU.CPU_ARCHTYPE_386SLOW:
                        case CPU.CPU_ARCHTYPE_486OLDSLOW:
                        case CPU.CPU_ARCHTYPE_486NEWSLOW:
                        case CPU.CPU_ARCHTYPE_PENTIUMSLOW:
                            priv_check=2;
                            break;
                        }
                    }
                    // check if actually failing the write-protected check
                    if (writing && USERWRITE_PROHIBITED()) priv_check=3;
                }
                if (priv_check==3) {
                    if (Log.level<=LogSeverities.LOG_NORMAL)
                        Log.log(LogTypes.LOG_PAGING,LogSeverities.LOG_NORMAL,"Page access denied: cpl="+CPU.cpu.cpl+", "+Integer.toString(entry.block.us, 16)+":"+Integer.toString(table.block.us, 16)+":"+Integer.toString(entry.block.wr, 16)+":"+Integer.toString(table.block.wr, 16));
                    PAGING_PageFault(lin_addr,(table.block.base<<12)+(lin_page & 0x3ff)*4,0x05 | (writing?0x02:0x00));
                    priv_check=0;
                }

                if (table.block.a==0) {
                    table.block.a=1;		// set page table accessed
                    Memory.phys_writed((paging.base.page<<12)+(lin_page >> 10)*4,table.load());
                }
                if ((entry.block.a)==0 || (entry.block.d)==0) {
                    entry.block.a=1;		// set page accessed

                    // page is dirty if we're writing to it, or if we're reading but the
                    // page will be fully linked so we can't track later writes
                    if (writing || (priv_check==0)) entry.block.d=1;		// mark page as dirty

                    Memory.phys_writed((table.block.base<<12)+(lin_page & 0x3ff)*4,entry.load());
                }

                phys_page=entry.block.base;

                // now see how the page should be linked best, if we need to catch privilege
                // checks later on it should be linked as read-only page
                if (priv_check==0) {
                    // if reading we could link the page as read-only to later cacth writes,
                    // will slow down pretty much but allows catching all dirty events
                    PAGING_LinkPage(lin_page,phys_page);
                } else {
                    if (priv_check==1) {
                        PAGING_LinkPage(lin_page,phys_page);
                        return 1;
                    } else if (writing) {
                        PageHandler handler=Memory.MEM_GetPageHandler(phys_page);
                        PAGING_LinkPage(lin_page,phys_page);
                        if ((handler.flags & PFLAG_READABLE)==0) return 1;
                        if ((handler.flags & PFLAG_WRITEABLE)==0) return 1;
                        int a = (int)lin_addr;
                        if (get_tlb_read(a)!=get_tlb_write(a)) return 1;
                        if (phys_page>1) return phys_page;
                        else return 1;
                    } else {
                        PAGING_LinkPage_ReadOnly(lin_page,phys_page);
                    }
                }
            } else {
                if (lin_page<LINK_START) phys_page=(int)paging.firstmb[lin_page];
                else phys_page=lin_page;
                PAGING_LinkPage(lin_page,phys_page);
            }
            return 0;
        }
        public boolean InitPageCheckOnly(/*Bitu*/long lin_addr,boolean writing) {
            /*Bitu*/int lin_page=(int)(lin_addr >> 12);
            if (paging.enabled) {
                X86PageEntry table=new X86PageEntry();
                X86PageEntry entry=new X86PageEntry();
                if (!InitPageCheckPresence_CheckOnly(lin_addr,writing,table,entry)) return false;

                if (!USERWRITE_PROHIBITED()) return true;

                if (InitPage_CheckUseraccess(entry.block.us,table.block.us) ||
                        (((entry.block.wr==0) || (table.block.wr==0)) && writing)) {
                    if (Log.level<=LogSeverities.LOG_NORMAL)
                        Log.log(LogTypes.LOG_PAGING,LogSeverities.LOG_NORMAL,"Page access denied: cpl="+CPU.cpu.cpl+", "+Integer.toString(entry.block.us, 16)+":"+Integer.toString(table.block.us, 16)+":"+Integer.toString(entry.block.wr, 16)+":"+Integer.toString(table.block.wr, 16));
                    paging.cr2=lin_addr;
                    CPU.cpu.exception.which=CPU.EXCEPTION_PF;
                    CPU.cpu.exception.error=0x05 | (writing?0x02:0x00);
                    return false;
                }
            } else {
                /*Bitu*/int phys_page;
                if (lin_page<LINK_START) phys_page=(int)paging.firstmb[lin_page];
                else phys_page=lin_page;
                PAGING_LinkPage(lin_page,phys_page);
            }
            return true;
        }
        public void InitPageForced(/*Bitu*/long lin_addr) {
            /*Bitu*/int lin_page=(int)(lin_addr >> 12);
            /*Bitu*/int phys_page;
            if (paging.enabled) {
                X86PageEntry table=new X86PageEntry();
                X86PageEntry entry=new X86PageEntry();
                InitPageCheckPresence(lin_addr,false,table,entry);

                if (table.block.a==0) {
                    table.block.a=1;		//Set access
                    Memory.phys_writed((paging.base.page<<12)+(lin_page >> 10)*4,table.load());
                }
                if (entry.block.a==0) {
                    entry.block.a=1;					//Set access
                    Memory.phys_writed((table.block.base<<12)+(lin_page & 0x3ff)*4,entry.load());
                }
                phys_page=entry.block.base;
                // maybe use read-only page here if possible
            } else {
                if (lin_page<LINK_START) phys_page=(int)paging.firstmb[lin_page];
                else phys_page=lin_page;
            }
            PAGING_LinkPage(lin_page,phys_page);
        }
    }

    static private class InitPageUserROHandler extends PageHandler {
        public InitPageUserROHandler() {
            flags=PFLAG_INIT|PFLAG_NOCODE;
        }
        public void writeb(/*PhysPt*/long addr,/*Bitu*/int val) {
            InitPage(addr,(val&0xff));
            Memory.host_writeb((int)(get_tlb_read((int)addr)+addr),(short)(val&0xff));
        }
        public void writew(/*PhysPt*/long addr,/*Bitu*/int val) {
            InitPage(addr,(val&0xffff));
            Memory.host_writew((int)(get_tlb_read((int)addr)+addr),(val&0xffff));
        }
        public void writed(/*PhysPt*/long addr,/*Bitu*/int val) {
            InitPage(addr,val);
            Memory.host_writed((int)(get_tlb_read((int)addr)+addr),val);
        }
        public boolean writeb_checked(/*PhysPt*/long addr,/*Bitu*/int val) {
            /*Bitu*/int writecode=InitPageCheckOnly(addr,(val&0xff));
            if (writecode!=0) {
                /*HostPt*/int tlb_addr;
                if (writecode>1) tlb_addr=get_tlb_read((int)addr);
                else tlb_addr=get_tlb_write((int)addr);
                Memory.host_writeb((int)(tlb_addr+addr),(short)(val&0xff));
                return false;
            }
            return true;
        }
        public boolean writew_checked(/*PhysPt*/long addr,/*Bitu*/int val) {
            /*Bitu*/int writecode=InitPageCheckOnly(addr,(val&0xffff));
            if (writecode!=0) {
                /*HostPt*/int tlb_addr;
                if (writecode>1) tlb_addr=get_tlb_read((int)addr);
                else tlb_addr=get_tlb_write((int)addr);
                Memory.host_writew((int)(tlb_addr+addr),(val&0xffff));
                return false;
            }
            return true;
        }
        public boolean writed_checked(/*PhysPt*/long addr,/*Bitu*/int val) {
            /*Bitu*/int writecode=InitPageCheckOnly(addr,val);
            if (writecode!=0) {
                /*HostPt*/int tlb_addr;
                if (writecode>1) tlb_addr=get_tlb_read((int)addr);
                else tlb_addr=get_tlb_write((int)addr);
                Memory.host_writed((int)(tlb_addr+addr),val);
                return false;
            }
            return true;
        }
        public void InitPage(/*Bitu*/long lin_addr,/*Bitu*/int val) {
            /*Bitu*/int lin_page=(int)(lin_addr >> 12);
            /*Bitu*/int phys_page;
            if (paging.enabled) {
                if (!USERWRITE_PROHIBITED()) return;

                X86PageEntry table=new X86PageEntry();
                X86PageEntry entry=new X86PageEntry();
                InitPageCheckPresence(lin_addr,true,table,entry);

                if (Log.level<=LogSeverities.LOG_NORMAL)
                        Log.log(LogTypes.LOG_PAGING,LogSeverities.LOG_NORMAL,"Page access denied: cpl="+CPU.cpu.cpl+", "+Integer.toString(entry.block.us, 16)+":"+Integer.toString(table.block.us, 16)+":"+Integer.toString(entry.block.wr, 16)+":"+Integer.toString(table.block.wr, 16));
                PAGING_PageFault(lin_addr,(table.block.base<<12)+(lin_page & 0x3ff)*4,0x07);

                if (table.block.a==0) {
                    table.block.a=1;		//Set access
                    Memory.phys_writed((paging.base.page<<12)+(lin_page >> 10)*4,table.load());
                }
                if ((entry.block.a)==0 || (entry.block.d)==0) {
                    entry.block.a=1;	//Set access
                    entry.block.d=1;	//Set dirty
                    Memory.phys_writed((table.block.base<<12)+(lin_page & 0x3ff)*4,entry.load());
                }
                phys_page=entry.block.base;
                PAGING_LinkPage(lin_page,phys_page);
            } else {
                if (lin_page<LINK_START) phys_page=(int)paging.firstmb[lin_page];
                else phys_page=lin_page;
                PAGING_LinkPage(lin_page,phys_page);
            }
        }
        public /*Bitu*/int InitPageCheckOnly(/*Bitu*/long lin_addr,/*Bitu*/int val) {
            /*Bitu*/int lin_page=(int)(lin_addr >> 12);
            if (paging.enabled) {
                if (!USERWRITE_PROHIBITED()) return 2;

                X86PageEntry table=new X86PageEntry();
                X86PageEntry entry=new X86PageEntry();
                if (!InitPageCheckPresence_CheckOnly(lin_addr,true,table,entry)) return 0;

                if (InitPage_CheckUseraccess(entry.block.us,table.block.us) || (((entry.block.wr==0) || (table.block.wr==0)))) {
                    if (Log.level<=LogSeverities.LOG_NORMAL)
                        Log.log(LogTypes.LOG_PAGING,LogSeverities.LOG_NORMAL,"Page access denied: cpl="+CPU.cpu.cpl+", "+Integer.toString(entry.block.us, 16)+":"+Integer.toString(table.block.us, 16)+":"+Integer.toString(entry.block.wr, 16)+":"+Integer.toString(table.block.wr, 16));
                    paging.cr2=lin_addr;
                    CPU.cpu.exception.which=CPU.EXCEPTION_PF;
                    CPU.cpu.exception.error=0x07;
                    return 0;
                }
                PAGING_LinkPage(lin_page,entry.block.base);
            } else {
                /*Bitu*/int phys_page;
                if (lin_page<LINK_START) phys_page=(int)paging.firstmb[lin_page];
                else phys_page=lin_page;
                PAGING_LinkPage(lin_page,phys_page);
            }
            return 1;
        }
        public void InitPageForced(/*Bitu*/long lin_addr) {
            /*Bitu*/int lin_page=(int)(lin_addr >> 12);
            /*Bitu*/int phys_page;
            if (paging.enabled) {
                X86PageEntry table=new X86PageEntry();
                X86PageEntry entry=new X86PageEntry();
                InitPageCheckPresence(lin_addr,true,table,entry);

                if (table.block.a==0) {
                    table.block.a=1;		//Set access
                    Memory.phys_writed((paging.base.page<<12)+(lin_page >> 10)*4,table.load());
                }
                if (entry.block.a==0) {
                    entry.block.a=1;	//Set access
                    Memory.phys_writed((table.block.base<<12)+(lin_page & 0x3ff)*4,entry.load());
                }
                phys_page=entry.block.base;
            } else {
                if (lin_page<LINK_START) phys_page=(int)paging.firstmb[lin_page];
                else phys_page=lin_page;
            }
            PAGING_LinkPage(lin_page,phys_page);
        }
    }


    static private boolean PAGING_MakePhysPage(/*Bitu*/IntRef page) {
        if (paging.enabled) {
            /*Bitu*/int d_index=page.value >> 10;
            /*Bitu*/int t_index=page.value & 0x3ff;
            X86PageEntry table=new X86PageEntry();
            table.load((int)(Memory.phys_readd((paging.base.page<<12)+d_index*4)));
            if (table.block.p==0) return false;
            X86PageEntry entry=new X86PageEntry();
            entry.load((int)(Memory.phys_readd((table.block.base<<12)+t_index*4)));
            if (entry.block.p==0) return false;
            page.value=entry.block.base;
        } else {
            if (page.value<LINK_START) page.value=(int)paging.firstmb[page.value];
            //Else keep it the same
        }
        return true;
    }

    static private InitPageHandler init_page_handler=new InitPageHandler();
    static private InitPageUserROHandler init_page_handler_userro=new InitPageUserROHandler();


    static public /*Bitu*/long PAGING_GetDirBase() {
        return paging.cr3;
    }

//    private static boolean PAGING_ForcePageInit(/*Bitu*/long lin_addr) {
//        PageHandler handler=get_tlb_readhandler(lin_addr);
//        if (handler==init_page_handler) {
//            init_page_handler.InitPageForced(lin_addr);
//            return true;
//        } else if (handler==init_page_handler_userro) {
//            PAGING_UnlinkPages((int)(lin_addr>>12),1);
//            init_page_handler_userro.InitPageForced(lin_addr);
//            return true;
//        }
//        return false;
//    }

    private static void PAGING_InitTLB() {
        for (/*Bitu*/int i=0;i<TLB_SIZE;i++) {
            paging.tlb.read[i]=Integer.MIN_VALUE;
            paging.tlb.write[i]=Integer.MIN_VALUE;
            paging.tlb.readhandler[i]=init_page_handler;
            paging.tlb.writehandler[i]=init_page_handler;
        }
        paging.links.used=0;
    }

    public static void PAGING_ClearTLB() {
        for (int i=0;paging.links.used>0;paging.links.used--,i++) {
            /*Bitu*/int page=(int)paging.links.entries[i];
            paging.tlb.read[page]=Integer.MIN_VALUE;
            paging.tlb.write[page]=Integer.MIN_VALUE;
            paging.tlb.readhandler[page]=init_page_handler;
            paging.tlb.writehandler[page]=init_page_handler;
        }
        paging.links.used=0;
    }

    private static void PAGING_UnlinkPages(/*Bitu*/int lin_page,/*Bitu*/int pages) {
        for (;pages>0;pages--) {
            paging.tlb.read[lin_page]=Integer.MIN_VALUE;
            paging.tlb.write[lin_page]=Integer.MIN_VALUE;
            paging.tlb.readhandler[lin_page]=init_page_handler;
            paging.tlb.writehandler[lin_page]=init_page_handler;
            lin_page++;
        }
    }

    public static void PAGING_MapPage(/*Bitu*/int lin_page,/*Bitu*/int phys_page) {
        if (lin_page<LINK_START) {
            paging.firstmb[lin_page]=phys_page;
            paging.tlb.read[lin_page]=Integer.MIN_VALUE;
            paging.tlb.write[lin_page]=Integer.MIN_VALUE;
            paging.tlb.readhandler[lin_page]=init_page_handler;
            paging.tlb.writehandler[lin_page]=init_page_handler;
        } else {
            PAGING_LinkPage(lin_page,phys_page);
        }
    }

    static private void PAGING_LinkPage(/*Bitu*/int lin_page,/*Bitu*/int phys_page) {
        PageHandler handler=Memory.MEM_GetPageHandler(phys_page);
        /*Bitu*/int lin_base=lin_page << 12;
        if (lin_page>=TLB_SIZE || phys_page>=TLB_SIZE)
            Log.exit("Illegal page");

        if (paging.links.used>=PAGING_LINKS) {
            Log.log(LogTypes.LOG_PAGING,LogSeverities.LOG_NORMAL,"Not enough paging links, resetting cache");
            PAGING_ClearTLB();
        }

        paging.tlb.phys_page[lin_page]=phys_page;
        if ((handler.flags & PFLAG_READABLE)!=0) paging.tlb.read[lin_page]=(int)(handler.GetHostReadPt(phys_page)-lin_base);
        else paging.tlb.read[lin_page]=Integer.MIN_VALUE;
        if ((handler.flags & PFLAG_WRITEABLE)!=0) paging.tlb.write[lin_page]=(int)(handler.GetHostWritePt(phys_page)-lin_base);
        else paging.tlb.write[lin_page]=Integer.MIN_VALUE;

        paging.links.entries[paging.links.used++]=lin_page;
        paging.tlb.readhandler[lin_page]=handler;
        paging.tlb.writehandler[lin_page]=handler;
    }

    private static void PAGING_LinkPage_ReadOnly(/*Bitu*/int lin_page,/*Bitu*/int phys_page) {
        PageHandler handler=Memory.MEM_GetPageHandler(phys_page);
        /*Bitu*/int lin_base=lin_page << 12;
        if (lin_page>=TLB_SIZE || phys_page>=TLB_SIZE)
            Log.exit("Illegal page");

        if (paging.links.used>=PAGING_LINKS) {
            Log.log(LogTypes.LOG_PAGING,LogSeverities.LOG_NORMAL,"Not enough paging links, resetting cache");
            PAGING_ClearTLB();
        }

        paging.tlb.phys_page[lin_page]=phys_page;
        if ((handler.flags & PFLAG_READABLE)!=0) paging.tlb.read[lin_page]=(short)(handler.GetHostReadPt(phys_page)-lin_base);
        else paging.tlb.read[lin_page]=Integer.MIN_VALUE;
        paging.tlb.write[lin_page]=Integer.MIN_VALUE;

        paging.links.entries[paging.links.used++]=lin_page;
        paging.tlb.readhandler[lin_page]=handler;
        paging.tlb.writehandler[lin_page]=init_page_handler_userro;
    }

    public static void PAGING_SetDirBase(/*Bitu*/long cr3) {
        paging.cr3=cr3;

        paging.base.page=cr3 >>> 12;
        paging.base.addr=cr3 & ~4095;
//	Log.log(LogTypes.LOG_PAGING,LogSeverities.LOG_NORMAL,"CR3:%X Base %X",cr3,paging.base.page);
        if (paging.enabled) {
            PAGING_ClearTLB();
        }
    }

    public static void PAGING_Enable(boolean enabled) {
        /* If paging is disable we work from a default paging table */
        if (paging.enabled==enabled) return;
        paging.enabled=enabled;
        if (enabled) {
            Core.setupFetch(true);
//		Log.log(LogTypes.LOG_PAGING,LogSeverities.LOG_NORMAL,"Enabled");
            PAGING_SetDirBase(paging.cr3);
        }
        PAGING_ClearTLB();
    }

    public static boolean PAGING_Enabled() {
        return paging.enabled;
    }

    static private Paging test;

    public Paging(Section configuration) {
        super(configuration);
        paging = new PagingBlock();
        paging.enabled=false;
        Core.setupFetch(false);
        PAGING_InitTLB();
        /*Bitu*/int i;
        for (i=0;i<LINK_START;i++) {
            paging.firstmb[i]=i;
        }
        pf_queue.used=0;
    }

    public static Section.SectionFunction PAGING_ShutDown = new Section.SectionFunction() {
        public void call(Section section) {
            paging = null;
        }
    };

    public static Section.SectionFunction PAGING_Init = new Section.SectionFunction() {
        public void call(Section section) {
            test = new Paging(section);
            section.AddDestroyFunction(PAGING_ShutDown);
        }
    };
}
