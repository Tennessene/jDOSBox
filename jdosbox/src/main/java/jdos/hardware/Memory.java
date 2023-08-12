package jdos.hardware;

import jdos.Dosbox;
import jdos.cpu.CPU;
import jdos.cpu.CPU_Regs;
import jdos.cpu.Paging;
import jdos.hardware.pci.PCI_PageHandler;
import jdos.misc.Log;
import jdos.misc.setup.Config;
import jdos.misc.setup.Module_base;
import jdos.misc.setup.Section;
import jdos.misc.setup.Section_prop;
import jdos.types.LogSeverities;
import jdos.types.LogTypes;
import jdos.types.MachineType;
import jdos.util.IntRef;
import jdos.util.Ptr;
import jdos.util.StringHelper;

import java.util.Vector;

public class Memory extends Module_base {
    public static final int MEM_PAGESIZE = 4096;
    public static int MEM_SIZE = 0;
    static final int EXTRA_MEM = 8196;

    //private static final int MEMBASE = 1; // can't use zero
    static private int highwaterMark;
    
    public static int allocate(int size) {
        int result = highwaterMark;
        highwaterMark+=size;
        return result;
    }

    /*
    public static void var_write(Bit8u * var, Bit8u val) {
        host_writeb((HostPt)var, val);
    }

    public static void var_write(Bit16u * var, Bit16u val) {
        host_writew((HostPt)var, val);
    }

    public static void var_write(Bit32u * var, Bit32u val) {
        host_writed((HostPt)var, val);
    }
    */
    public static void phys_writes(/*PhysPt*/int addr,String s) {
        int i;
        byte[] b = s.getBytes();
        for (i=0;i<s.length();i++)
            RAM.writeb(addr + i, b[i]);
        RAM.writeb(addr + i, (byte) 0);
    }

    public static void phys_writeb(/*PhysPt*/int addr,/*Bit8u*/int val) {
        RAM.writeb(addr, (short) val);
    }
    public static void phys_writew(/*PhysPt*/int addr,/*Bit16u*/int val){
        RAM.writew(addr, val);
    }
    public static void phys_writed(/*PhysPt*/int addr,/*Bit32u*/int val){
        RAM.writed(addr, val);
    }

    public static /*Bit8u*/short phys_readb(/*PhysPt*/int addr) {
        return RAM.readb(addr);
    }
    public static /*Bit16u*/int phys_readw(/*PhysPt*/int addr){
        return RAM.readw(addr);
    }
    public static /*Bit32u*/int phys_readd(/*PhysPt*/int addr){
        return RAM.readd(addr);
    }

    /* The folowing functions are all shortcuts to the above functions using physical addressing */

    public static /*Bit8u*/int real_readb(/*Bit16u*/int seg,/*Bit16u*/int off) {
        return mem_readb((seg<<4)+off);
    }

    public static /*Bit16u*/int real_readw(/*Bit16u*/int seg,/*Bit16u*/int off) {
        return mem_readw((seg<<4)+off);
    }
    public static /*Bit32u*/int real_readd(/*Bit16u*/int seg,/*Bit16u*/int off) {
        return mem_readd((seg << 4) + off);
    }

    public static void real_writeb(/*Bit16u*/int seg,/*Bit16u*/int off,/*Bit8u*/int val) {
        mem_writeb(((seg<<4)+off),val);
    }
    public static void real_writew(/*Bit16u*/int seg,/*Bit16u*/int off,/*Bit16u*/int val) {
        mem_writew(((seg<<4)+off),val);
    }
    public static void real_writed(/*Bit16u*/int seg,/*Bit16u*/int off,/*Bit32u*/int val) {
        mem_writed(((seg<<4)+off),val);
    }

    public static /*Bit16u*/int RealSeg(/*RealPt*/int pt) {
        return (/*Bit16u*/int)((pt>>>16) & 0xFFFF);
    }

    public static /*Bit16u*/int RealOff(/*RealPt*/int pt) {
        return (/*Bit16u*/int)(pt & 0xffff);
    }

    public static /*PhysPt*/int Real2Phys(/*RealPt*/int pt) {
        return (RealSeg(pt)<<4) +RealOff(pt);
    }

    public static /*PhysPt*/int PhysMake(/*Bit16u*/int seg,/*Bit16u*/int off) {
        return (seg<<4)+off;
    }

    public static /*RealPt*/int RealMake(/*Bit16u*/int seg,/*Bit16u*/int off) {
        return (seg<<16)+off;
    }

    public static void RealSetVec(/*Bit8u*/int vec,/*RealPt*/int pt) {
        mem_writed(vec<<2,pt);
    }

    public static void RealSetVec(/*Bit8u*/int vec,/*RealPt*/int pt, IntRef old) {
        old.value = mem_readd(vec << 2);
        mem_writed(vec<<2,pt);
    }

    public static int RealSetVec2(/*Bit8u*/int vec,/*RealPt*/int pt) {
        int ret = mem_readd(vec << 2);
        mem_writed(vec<<2,pt);
        return ret;
    }

    public static /*RealPt*/int RealGetVec(/*Bit8u*/int vec) {
        return mem_readd(vec << 2);
    }

    private static final int PAGES_IN_BLOCK = ((1024*1024)/ Paging.MEM_PAGE_SIZE);
    private static final int SAFE_MEMORY = 32;
    private static final int MAX_MEMORY	= 512;
    private static final int MAX_PAGE_ENTRIES = (MAX_MEMORY*1024*1024/4096);
    private static final int LFB_PAGES = 512;

    private static class LinkBlock {
        public /*Bitu*/int used;
        public /*Bit32u*/long[] pages = new long[((MEM_SIZE*1024/4)+4096)];
    }

    private static class MemoryBlock {
        public /*Bitu*/int pages;
        Paging.PageHandler[] phandlers;
        /*MemHandle*/int[] mhandles;
        LinkBlock links = new LinkBlock();
        public static class Lfb	{
            /*Bitu*/int		start_page;
            /*Bitu*/int		end_page;
            /*Bitu*/int		pages;
            Paging.PageHandler handler;
            Paging.PageHandler mmiohandler;
        }
        public Lfb lfb = new Lfb();
        public static class ROM {
            /*Bitu*/int		start_page;
            /*Bitu*/int		end_page;
            /*Bitu*/int		pages;
            Paging.PageHandler handler;
        }
        public Vector<PCI_PageHandler> pci = new Vector<PCI_PageHandler>();
        public Vector<ROM> roms = new Vector<ROM>();
        public static class A20 {
            boolean enabled;
            /*Bit8u*/short controlport;
        }
        A20 a20 = new A20();
    }
    static private MemoryBlock memory;

    static /*HostPt*/Ptr MemBase;

    static private class IllegalPageHandler extends Paging.PageHandler {
        public IllegalPageHandler() {
            flags=Paging.PFLAG_INIT|Paging.PFLAG_NOCODE;
        }
        static /*Bits*/int r_lcount=0;
        public /*Bitu*/int readb(/*PhysPt*/int addr) {
            if (Config.C_DEBUG)
                Log.log_msg(StringHelper.sprintf("Illegal read from %x, CS:IP %8x:%8x",new Object[] {new Integer(addr), new Integer(CPU_Regs.reg_csVal.dword),new Integer(CPU_Regs.reg_eip)}));
            else {
                if (r_lcount<1000) {
                    r_lcount++;
                    Log.log_msg(StringHelper.sprintf("Illegal read from %x, CS:IP %8x:%8x",new Object[] {new Integer(addr), new Integer(CPU_Regs.reg_csVal.dword),new Integer(CPU_Regs.reg_eip)}));
                }
            }
            return 0;
        }
        static /*Bits*/int w_lcount=0;
        public void writeb(/*PhysPt*/int addr,/*Bitu*/int val) {
            if (Config.C_DEBUG)
                Log.log_msg(StringHelper.sprintf("Illegal write to %x, CS:IP %8x:%8x",new Object[] {new Integer(addr), new Integer(CPU_Regs.reg_csVal.dword),new Integer(CPU_Regs.reg_eip)}));
            else {

                if (w_lcount<1000) {
                    w_lcount++;
                    Log.log_msg(StringHelper.sprintf("Illegal write to %x, CS:IP %8x:%8x",new Object[] {new Integer(addr), new Integer(CPU_Regs.reg_csVal.dword),new Integer(CPU_Regs.reg_eip)}));
                }
            }
        }
    }

    static private class RAMPageHandler extends Paging.PageHandler {
        public RAMPageHandler() {
            flags=Paging.PFLAG_READABLE|Paging.PFLAG_WRITEABLE;
        }
        public /*HostPt*/int GetHostReadPt(/*Bitu*/int phys_page) {
            return phys_page*MEM_PAGESIZE;
        }
        public /*HostPt*/int GetHostWritePt(/*Bitu*/int phys_page) {
            return phys_page*MEM_PAGESIZE;
        }
    }

    static private class ROMPageHandler extends RAMPageHandler {
        public ROMPageHandler() {
            flags=Paging.PFLAG_READABLE|Paging.PFLAG_HASROM;
        }
        public void writeb(/*PhysPt*/int addr,/*Bitu*/int val){
            if (Log.level<=LogSeverities.LOG_ERROR) Log.log(LogTypes.LOG_CPU,LogSeverities.LOG_ERROR,"Write "+Integer.toString(val, 16)+" to rom at "+Integer.toString(addr,16));
        }
        public void writew(/*PhysPt*/int addr,/*Bitu*/int val){
            if (Log.level<=LogSeverities.LOG_ERROR) Log.log(LogTypes.LOG_CPU,LogSeverities.LOG_ERROR,"Write "+Integer.toString(val, 16)+" to rom at "+Integer.toString(addr,16));
        }
        public void writed(/*PhysPt*/int addr,/*Bitu*/int val){
            if (Log.level<=LogSeverities.LOG_ERROR) Log.log(LogTypes.LOG_CPU,LogSeverities.LOG_ERROR,"Write "+Integer.toString(val, 16)+" to rom at "+Integer.toString(addr,16));
        }
    }



    private static IllegalPageHandler illegal_page_handler = new IllegalPageHandler();
    private static RAMPageHandler ram_page_handler = new RAMPageHandler();
    private static ROMPageHandler rom_page_handler = new ROMPageHandler();

    private static class ROMDataPageHandler extends Paging.PageHandler {
        byte[] data;
        int address;
        public ROMDataPageHandler(byte[] data, int address) {
            this.data = data;
            this.address = address;
        }
        public /*Bitu*/int readb(/*PhysPt*/int addr) {
            return data[addr-address] & 0xFF;
        }

        public void writeb(/*PhysPt*/int addr,/*Bitu*/int val) {
        }

        public void writew(/*PhysPt*/int addr,/*Bitu*/int val) {
        }

        public void writed(/*PhysPt*/int addr,/*Bitu*/int val) {
        }

        public /*HostPt*/int GetHostReadPt(/*Bitu*/int phys_page) {
            return -1;
        }

        public /*HostPt*/int GetHostWritePt(/*Bitu*/int phys_page) {
            return -1;
        }
    }
    static public void MEM_AddROM(/*Bitu*/int page, /*Bitu*/int pages, byte[] data) {
        MemoryBlock.ROM rom = new MemoryBlock.ROM();
        rom.pages = pages;
        rom.start_page = page;
        rom.end_page = page+pages;
        rom.handler = new ROMDataPageHandler(data, page << 12);
        memory.roms.add(rom);
    }

    static public void MEM_AddPCIPageHandler(PCI_PageHandler handler) {
        memory.pci.add(handler);
    }

    static public void MEM_SetLFB(/*Bitu*/int page, /*Bitu*/int pages, Paging.PageHandler handler, Paging.PageHandler mmiohandler) {
        memory.lfb.handler=handler;
        memory.lfb.mmiohandler=mmiohandler;
        memory.lfb.start_page=page;
        memory.lfb.end_page=page+pages;
        memory.lfb.pages=pages;
        Paging.PAGING_ClearTLB();
    }

    static public Paging.PageHandler MEM_GetPageHandler(/*Bitu*/int phys_page) {
        if (phys_page<memory.pages) {
            return memory.phandlers[phys_page];
        } else if ((phys_page>=memory.lfb.start_page) && (phys_page<memory.lfb.end_page)) {
            return memory.lfb.handler;
        } else if ((phys_page>=memory.lfb.start_page+0x01000000/4096) &&
                    (phys_page<memory.lfb.start_page+0x01000000/4096+16)) {
            return memory.lfb.mmiohandler;
        } else if (VBE.initialized && phys_page >= 0xE0000 && phys_page < 0xE0000 + VBE.pageCount) {
            return VBE.handler;
        }
        for (int i=0;i<memory.roms.size();i++) {
            MemoryBlock.ROM rom = memory.roms.elementAt(i);
            if (phys_page>=rom.start_page && phys_page<rom.end_page) {
                return rom.handler;
            }
        }
        for (int i=0;i<memory.pci.size();i++) {
            PCI_PageHandler handler = memory.pci.elementAt(i);
            if (phys_page>=handler.start_page && phys_page<handler.stop_page)
                return handler;
        }
        return illegal_page_handler;
    }

    static public void MEM_SetPageHandler(/*Bitu*/int phys_page,/*Bitu*/int pages, Paging.PageHandler handler) {
        for (;pages>0;pages--) {
            memory.phandlers[phys_page]=handler;
            phys_page++;
        }
    }

    static public void MEM_ResetPageHandler(/*Bitu*/int phys_page, /*Bitu*/int pages) {
        for (;pages>0;pages--) {
            memory.phandlers[phys_page]=ram_page_handler;
            phys_page++;
        }
    }

    static public /*Bitu*/int mem_strlen(/*PhysPt*/int pt) {
        /*Bitu*/int x=0;
        while (x<1024) {
            if (Paging.mem_readb_inline(pt+x)==0) return x;
            x++;
        }
        return 0;		//Hope this doesn't happen
    }

    static private void mem_strcpy(/*PhysPt*/int dest,/*PhysPt*/int src) {
        /*Bit8u*/int r;
        while ( (r = mem_readb(src++))!=0 ) Paging.mem_writeb_inline(dest++,r);
        Paging.mem_writeb_inline(dest,(short)0);
    }

    static public void mem_memmove(/*PhysPt*/int dest,/*PhysPt*/int src,/*Bitu*/int size) {
        while (size-- !=0) Paging.mem_writeb_inline(dest++,Paging.mem_readb_inline(src++));
    }

    static public void mem_memcpy(/*PhysPt*/int dest,/*PhysPt*/int src,/*Bitu*/int size) {
        while (size-- !=0) Paging.mem_writeb_inline(dest++,Paging.mem_readb_inline(src++));
    }

    static public void mem_memcpy(byte[] dest, int destOffset, /*PhysPt*/int src,/*Bitu*/int size) {
        while (size-- !=0) dest[destOffset++]=(byte)Paging.mem_readb_inline(src++);
    }

    static public void mem_memcpy(/*PhysPt*/int dest, byte[] src, int srcOffset, /*Bitu*/int size) {
        while (size-- !=0) Paging.mem_writeb_inline(dest++, src[srcOffset++]);
    }

    static public void mem_zero(int dest, int len) {
        while (len-- != 0) Paging.mem_writeb_inline(dest++, 0);
    }

    static public void mem_memset(int dest, int value, int len) {
        while (len-- != 0) Paging.mem_writeb_inline(dest++, value);
    }

    static public void phys_zero(int dest, int len) {
        while (len-- != 0) RAM.writeb(dest++, (short) 0);
    }

    static public void phys_memcpy(int dest, byte[] buffer, int offset, int len) {
        while (len-- != 0) RAM.writeb(dest++, (short) buffer[offset++]);
    }

    static public void MEM_BlockRead(/*PhysPt*/int pt,short[] data,int offset, /*Bitu*/int size) {
        for (int i=0;i<size;i++) {
            int v1 = Paging.mem_readb_inline(pt++);
            int v2 = Paging.mem_readb_inline(pt++);
            data[i+offset]=(short)((v1 & 0xFF) | ((v2 & 0xFF) << 16));
        }
    }
    static public void MEM_BlockRead16u(/*PhysPt*/int pt,int[] data,int offset, /*Bitu*/int size) {
        for (int i=0;i<size;i++) {
            int v1 = Paging.mem_readb_inline(pt++);
            int v2 = Paging.mem_readb_inline(pt++);
            data[i+offset]=((v1 & 0xFF) | ((v2 & 0xFF) << 16));
        }
    }
    static public void MEM_BlockRead(/*PhysPt*/int pt,short[] data,/*Bitu*/int size) {
        for (int i=0;i<size;i++) {
            int v1 = Paging.mem_readb_inline(pt++);
            int v2 = Paging.mem_readb_inline(pt++);
            data[i]=(short)((v1 & 0xFF) | ((v2 & 0xFF) << 16));
        }
    }

    static public String MEM_BlockRead(/*PhysPt*/int pt,/*Bitu*/int size) {
        byte[] b = new byte[size];
        MEM_BlockRead(pt, b, size);
        return new String(b, 0, StringHelper.strlen(b));
    }

    static public void MEM_BlockRead(/*PhysPt*/int pt,byte[] data,/*Bitu*/int size) {
        for (int i=0;i<size;i++) {
            data[i]=(byte)(Paging.mem_readb_inline(pt++) & 0xFF);
        }
    }
    static public void MEM_BlockRead(/*PhysPt*/int pt,byte[] data, int offset, /*Bitu*/int size) {
        for (int i=0;i<size;i++) {
            data[i+offset]=(byte)(Paging.mem_readb_inline(pt++) & 0xFF);
        }
    }

    static public void MEM_BlockWrite(/*PhysPt*/int pt,byte[] read,/*Bitu*/int size) {
        int i;
        for (i=0;i<size && i<read.length;i++) {
            Paging.mem_writeb_inline(pt++,read[i]);
        }
        for (;i<size;i++) {
            Paging.mem_writeb_inline(pt++,(byte)0);
        }
    }
    static public void MEM_BlockWrite(/*PhysPt*/int pt,byte[] read, int offset, /*Bitu*/int size) {
        int i;
        for (i=0;i<size && i<read.length;i++) {
            Paging.mem_writeb_inline(pt++,read[i+offset]);
        }
        for (;i<size;i++) {
            Paging.mem_writeb_inline(pt++,(byte)0);
        }
    }
    static public void MEM_BlockWrite(/*PhysPt*/int pt,String data,/*Bitu*/int size) {
        byte[] read = data.getBytes();
        MEM_BlockWrite(pt, read, size);
    }

    static public void MEM_BlockCopy(/*PhysPt*/int dest,/*PhysPt*/int src,/*Bitu*/int size) {
        mem_memcpy(dest,src,size);
    }

    static public String MEM_StrCopy(/*PhysPt*/int pt,/*Bitu*/int size) {
        StringBuffer buf = new StringBuffer();
        for (int i=0;i<size;i++) {
            int r=Paging.mem_readb_inline(pt++);
            if (r==0) break;
            buf.append((char)r);
        }
        return buf.toString();
    }

    static public /*Bitu*/int MEM_TotalPages() {
        return memory.pages;
    }

    static public /*Bitu*/int MEM_ExtraPages() {
        return highwaterMark*4/4096 - memory.pages;
    }

    static public /*Bitu*/int MEM_FreeLargest() {
        /*Bitu*/int size=0;/*Bitu*/int largest=0;
        /*Bitu*/int index=Paging.XMS_START;
        while (index<memory.pages) {
            if (memory.mhandles[index]==0) {
                size++;
            } else {
                if (size>largest) largest=size;
                size=0;
            }
            index++;
        }
        if (size>largest) largest=size;
        return largest;
    }

    static public /*Bitu*/int MEM_FreeTotal() {
        /*Bitu*/int free=0;
        /*Bitu*/int index=Paging.XMS_START;
        while (index<memory.pages) {
            if (memory.mhandles[index]==0) free++;
            index++;
        }
        return free;
    }

    static private /*Bitu*/int MEM_AllocatedPages(/*MemHandle*/int handle) {
        /*Bitu*/int pages = 0;
        while (handle>0) {
            pages++;
            handle=memory.mhandles[handle];
        }
        return pages;
    }

    //TODO Maybe some protection for this whole allocation scheme

    static private /*Bitu*/int BestMatch(/*Bitu*/int size) {
        /*Bitu*/int index=Paging.XMS_START;
        /*Bitu*/int first=0;
        /*Bitu*/int best=0xfffffff;
        /*Bitu*/int best_first=0;
        while (index<memory.pages) {
            /* Check if we are searching for first free page */
            if (first==0) {
                /* Check if this is a free page */
                if (memory.mhandles[index]==0) {
                    first=index;
                }
            } else {
                /* Check if this still is used page */
                if (memory.mhandles[index]!=0) {
                    /*Bitu*/int pages=index-first;
                    if (pages==size) {
                        return first;
                    } else if (pages>size) {
                        if (pages<best) {
                            best=pages;
                            best_first=first;
                        }
                    }
                    first=0;			//Always reset for new search
                }
            }
            index++;
        }
        /* Check for the final block if we can */
        if (first!=0 && (index-first>=size) && (index-first<best)) {
            return first;
        }
        return best_first;
    }

    static public /*MemHandle*/int MEM_AllocatePages(/*Bitu*/int pages,boolean  sequence) {
        /*MemHandle*/int ret=-1;
        if (pages==0) return 0;
        //if (true) throw new RuntimeException("This needs to be traced and compared with original");
        if (sequence) {
            /*Bitu*/int index=BestMatch(pages);
            if (index==0) return 0;
            while (pages!=0) {
                if (ret==-1)
                    ret = index;
                else
                    memory.mhandles[index-1] = index;
                index++;pages--;
            }
            memory.mhandles[index-1] = -1;
        } else {
            if (MEM_FreeTotal()<pages) return 0;
            int lastIndex=-1;
            while (pages!=0) {
                /*Bitu*/int index=BestMatch(1);
                if (index==0) Log.exit("MEM:corruption during allocate");
                while (pages!=0 && (memory.mhandles[index]==0)) {
                    if (ret == -1)
                        ret = index;
                    else
                        memory.mhandles[lastIndex]=index;
                    lastIndex = index;
                    index++;pages--;
                }
                memory.mhandles[lastIndex]=-1;//Invalidate it in case we need another match
            }
        }
        return ret;
    }

    static public /*MemHandle*/int MEM_GetNextFreePage() {
        return BestMatch(1);
    }

    static public void MEM_ReleasePages(/*MemHandle*/int handle) {
        while (handle>0) {
            /*MemHandle*/int next=memory.mhandles[handle];
            memory.mhandles[handle]=0;
            handle=next;
        }
    }

    static public boolean MEM_ReAllocatePages(/*MemHandle*/IntRef handle,/*Bitu*/int pages,boolean  sequence) {
        if (handle.value<=0) {
            if (pages==0) return true;
            handle.value=MEM_AllocatePages(pages,sequence);
            return (handle.value>0);
        }
        if (pages==0) {
            MEM_ReleasePages(handle.value);
            handle.value=-1;
            return true;
        }
        /*MemHandle*/int index=handle.value;
        /*MemHandle*/int last=0;/*Bitu*/int old_pages=0;
        while (index>0) {
            old_pages++;
            last=index;
            index=memory.mhandles[index];
        }
        if (old_pages == pages) return true;
        if (old_pages > pages) {
            /* Decrease size */
            pages--;index=handle.value;old_pages--;
            while (pages!=0) {
                index=memory.mhandles[index];
                pages--;old_pages--;
            }
            /*MemHandle*/int next=memory.mhandles[index];
            memory.mhandles[index]=-1;
            index=next;
            while (old_pages!=0) {
                next=memory.mhandles[index];
                memory.mhandles[index]=0;
                index=next;
                old_pages--;
            }
            return true;
        } else {
            /* Increase size, check for enough free space */
            /*Bitu*/int need=pages-old_pages;
            if (sequence) {
                index=last+1;
                /*Bitu*/int free=0;
                while (index<memory.pages && memory.mhandles[index]==0) {
                    index++;free++;
                }
                if (free>=need) {
                    /* Enough space allocate more pages */
                    index=last;
                    while (need!=0) {
                        memory.mhandles[index]=index+1;
                        need--;index++;
                    }
                    memory.mhandles[index]=-1;
                    return true;
                } else {
                    /* Not Enough space allocate new block and copy */
                    /*MemHandle*/int newhandle=MEM_AllocatePages(pages,true);
                    if (newhandle==0) return false;
                    MEM_BlockCopy(newhandle*4096,handle.value*4096,old_pages*4096);
                    MEM_ReleasePages(handle.value);
                    handle.value=newhandle;
                    return true;
                }
            } else {
                /*MemHandle*/int rem=MEM_AllocatePages(need,false);
                if (rem==0) return false;
                memory.mhandles[last]=rem;
                return true;
            }
        }
    }

    public static /*MemHandle*/int MEM_NextHandle(/*MemHandle*/int handle) {
        return memory.mhandles[handle];
    }

    public static /*MemHandle*/int MEM_NextHandleAt(/*MemHandle*/int handle,/*Bitu*/int where) {
        while (where!=0) {
            where--;
            handle=memory.mhandles[handle];
        }
        return handle;
    }


    /*
        A20 line handling,
        Basically maps the 4 pages at the 1mb to 0mb in the default page directory
    */
    public static boolean MEM_A20_Enabled() {
        return memory.a20.enabled;
    }

    public static void MEM_A20_Enable(boolean  enabled) {
        /*Bitu*/int phys_base=enabled ? (1024/4) : 0;
        for (/*Bitu*/int i=0;i<16;i++) Paging.PAGING_MapPage((1024/4)+i,phys_base+i);
        memory.a20.enabled=enabled;
    }


    /* Memory access functions */
    public static /*Bit16u*/int mem_unalignedreadw(/*PhysPt*/int address) {
        int result = Paging.mem_readb_inline(address);
        result |= Paging.mem_readb_inline(address+1) << 8;
        return result;
    }

    public static /*Bit32u*/int mem_unalignedreadd(/*PhysPt*/int address) {
        int result = Paging.mem_readb_inline(address);
        result |= (Paging.mem_readb_inline(address+1) << 8);
        result |= (Paging.mem_readb_inline(address+2) << 16);
        result |= (Paging.mem_readb_inline(address+3) << 24);
        return result;
    }


    public static void mem_unalignedwritew(/*PhysPt*/int address,/*Bit16u*/int val) {
        Paging.mem_writeb_inline(address,(short)(val & 0xFF));val>>=8;
        Paging.mem_writeb_inline(address+1,(short)(val & 0xFF));
    }

    public static void mem_unalignedwrited(/*PhysPt*/int address,/*Bit32u*/int val) {
        Paging.mem_writeb_inline(address++,val);val>>=8;
        Paging.mem_writeb_inline(address++,val);val>>=8;
        Paging.mem_writeb_inline(address++,val);val>>=8;
        Paging.mem_writeb_inline(address,val);
    }

    public static /*Bit8u*/int mem_readb(/*PhysPt*/int address) {
        return Paging.mem_readb_inline(address);
    }

    public static /*Bit16u*/int mem_readw(/*PhysPt*/int address) {
        return Paging.mem_readw_inline(address);
    }

    public static /*Bit32u*/int mem_readd(/*PhysPt*/int address) {
        return Paging.mem_readd_inline(address);
    }
    public static /*Bit32u*/long mem_readq(/*PhysPt*/int address) {
        return (Paging.mem_readd_inline(address) & 0xFFFFFFFFl) | ((Paging.mem_readd_inline(address+4) & 0xFFFFFFFFl) << 32);
    }

    static public void mem_writeb(/*PhysPt*/int address,/*Bit8u*/int val) {
        Paging.mem_writeb_inline(address,(short)val);
    }

    static public void mem_writew(/*PhysPt*/int address,/*Bit16u*/int val) {
        Paging.mem_writew_inline(address,val);
    }

    static public void mem_writed(/*PhysPt*/int address,/*Bit32u*/int val) {
        Paging.mem_writed_inline(address,val);
    }

    static public void mem_writeq(/*PhysPt*/int address,long val) {
        Paging.mem_writed_inline(address,(int)val);
        Paging.mem_writed_inline(address+4,(int)(val>>>32));
    }

    static private IoHandler.IO_WriteHandler write_p92 = new IoHandler.IO_WriteHandler() {
        public void call(/*Bitu*/int port, /*Bitu*/int val, /*Bitu*/int iolen) {
            // Bit 0 = system reset (switch back to real mode)
            if ((val&1)!=0) Log.exit("XMS: CPU reset via port 0x92 not supported.");
            memory.a20.controlport = (short)(val & ~2);
            MEM_A20_Enable((val & 2)>0);
        }
    };

    static private IoHandler.IO_ReadHandler read_p92 = new IoHandler.IO_ReadHandler() {
        public /*Bitu*/int call(/*Bitu*/int port, /*Bitu*/int iolen) {
            return memory.a20.controlport | (memory.a20.enabled ? 0x02 : 0);
        }
    };

    public static void RemoveEMSPageFrame() {
        /* Setup rom at 0xe0000-0xf0000 */
        for (/*Bitu*/int ct=0xe0;ct<0xf0;ct++) {
            memory.phandlers[ct] = rom_page_handler;
        }
    }

    static public void PreparePCJRCartRom() {
        /* Setup rom at 0xd0000-0xe0000 */
        for (/*Bitu*/int ct=0xd0;ct<0xe0;ct++) {
            memory.phandlers[ct] = rom_page_handler;
        }
    }

    private IoHandler.IO_ReadHandleObject ReadHandler = new IoHandler.IO_ReadHandleObject();
    private IoHandler.IO_WriteHandleObject WriteHandler = new IoHandler.IO_WriteHandleObject();

    public static int videoCacheSize = 0;
    public Memory(Section configuration) {
        super(configuration);
        /*Bitu*/int i;
            Section_prop section=(Section_prop)configuration;

            /* Setup the Physical Page Links */
            /*Bitu*/int memsize=section.Get_int("memsize");

            if (memsize < 1) memsize = 1;

            if (memsize > MAX_MEMORY) {
                Log.log_msg("Maximum memory size is "+(MAX_MEMORY - 1)+" MB");
                memsize = MAX_MEMORY;
            }
            if (memsize > SAFE_MEMORY-1) {
                Log.log_msg("Memory sizes above "+(SAFE_MEMORY - 1)+" MB are NOT recommended.");
                Log.log_msg("Stick with the default values unless you are absolutely certain.");
            }
            MEM_SIZE = memsize;
            memory = new MemoryBlock();
            try {
                RAM.free();
                Runtime.getRuntime().gc();
                highwaterMark = memsize*1024*1024;
                int videosize = section.Get_int("vmemsize");
                videoCacheSize = section.Get_int("vmemcachesize");
                if (videosize==0) videosize=2;
                if (videosize<512)
                    videosize*=1024*1024;
                else
                    videosize*=1024;

                videoCacheSize*=1024;
                if (videoCacheSize==0) videoCacheSize = videosize*2;
                videosize+=videoCacheSize;
                System.out.println("About to allocate memory "+String.valueOf((highwaterMark+EXTRA_MEM+VGA_draw.TEMPLINE_SIZE+videosize)/1024)+"kb: "+String.valueOf(Runtime.getRuntime().freeMemory()/1024)+"kb free");
                RAM.alloc(highwaterMark + EXTRA_MEM + videosize + VGA_draw.TEMPLINE_SIZE + 3);
            } catch (java.lang.OutOfMemoryError e) {
                Log.exit("Can't allocate main memory of "+memsize+" MB");
            }
            memory.pages = (memsize*1024*1024)/4096;
            /* Allocate the data for the different page information blocks */
            memory.phandlers=new Paging.PageHandler[memory.pages];
            memory.mhandles=new /*MemHandle*/int[memory.pages];
            for (i = 0;i < memory.pages;i++) {
                memory.phandlers[i] = ram_page_handler;
                memory.mhandles[i] = 0;				//Set to 0 for memory allocation
            }
            /* Setup rom at 0xc0000-0xc8000 */
            for (i=0xc0;i<0xc8;i++) {
                memory.phandlers[i] = rom_page_handler;
            }
            /* Setup rom at 0xf0000-0x100000 */
            for (i=0xf0;i<0x100;i++) {
                memory.phandlers[i] = rom_page_handler;
            }
            if (Dosbox.machine== MachineType.MCH_PCJR) {
                /* Setup cartridge rom at 0xe0000-0xf0000 */
                for (i=0xe0;i<0xf0;i++) {
                    memory.phandlers[i] = rom_page_handler;
                }
            }
            /* Reset some links */
            memory.links.used = 0;
            // A20 Line - PS/2 system control port A
            WriteHandler.Install(0x92,write_p92,IoHandler.IO_MB);
            ReadHandler.Install(0x92,read_p92,IoHandler.IO_MB);
            MEM_A20_Enable(false);
    }

    static public void clear() {
        for (int i = 0;i < memory.pages;i++) {
            memory.phandlers[i] = ram_page_handler;
            memory.mhandles[i] = 0;				//Set to 0 for memory allocation
        }
        memory.links.used = 0;
    }
    static Memory test;
    public static Section.SectionFunction MEM_ShutDown = new Section.SectionFunction() {
        public void call(Section section) {
            RAM.free();
            test = null;
        }
    };
    public static Section.SectionFunction MEM_Init = new Section.SectionFunction() {
        public void call(Section section) {
            test = new Memory(section);
            section.AddDestroyFunction(MEM_ShutDown);
        }
    };
}
