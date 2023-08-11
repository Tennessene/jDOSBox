package jdos.ints;

import jdos.cpu.CPU;
import jdos.cpu.CPU_Regs;
import jdos.cpu.Callback;
import jdos.dos.*;
import jdos.hardware.IoHandler;
import jdos.hardware.Memory;
import jdos.misc.Log;
import jdos.misc.setup.Module_base;
import jdos.misc.setup.Section;
import jdos.misc.setup.Section_prop;
import jdos.types.LogSeverities;
import jdos.types.LogTypes;
import jdos.util.IntRef;
import jdos.util.ShortRef;

public class XMS extends Module_base {
    static private final int XMS_HANDLES							=50;		/* 50 XMS Memory Blocks */
    static private final int XMS_VERSION    						=0x0300;	/* version 3.00 */
    static private final int XMS_DRIVER_VERSION					    =0x0301;	/* my driver version 3.01 */

    static private final int 	XMS_GET_VERSION						=0x00;
    static private final int 	XMS_ALLOCATE_HIGH_MEMORY			=0x01;
    static private final int 	XMS_FREE_HIGH_MEMORY				=0x02;
    static private final int 	XMS_GLOBAL_ENABLE_A20				=0x03;
    static private final int 	XMS_GLOBAL_DISABLE_A20				=0x04;
    static private final int 	XMS_LOCAL_ENABLE_A20				=0x05;
    static private final int 	XMS_LOCAL_DISABLE_A20				=0x06;
    static private final int 	XMS_QUERY_A20						=0x07;
    static private final int 	XMS_QUERY_FREE_EXTENDED_MEMORY		=0x08;
    static private final int 	XMS_ALLOCATE_EXTENDED_MEMORY		=0x09;
    static private final int 	XMS_FREE_EXTENDED_MEMORY			=0x0a;
    static private final int 	XMS_MOVE_EXTENDED_MEMORY_BLOCK		=0x0b;
    static private final int 	XMS_LOCK_EXTENDED_MEMORY_BLOCK		=0x0c;
    static private final int 	XMS_UNLOCK_EXTENDED_MEMORY_BLOCK	=0x0d;
    static private final int 	XMS_GET_EMB_HANDLE_INFORMATION		=0x0e;
    static private final int 	XMS_RESIZE_EXTENDED_MEMORY_BLOCK	=0x0f;
    static private final int 	XMS_ALLOCATE_UMB					=0x10;
    static private final int 	XMS_DEALLOCATE_UMB					=0x11;
    static private final int XMS_QUERY_ANY_FREE_MEMORY			    =0x88;
    static private final int XMS_ALLOCATE_ANY_MEMORY				=0x89;
    static private final int 	XMS_GET_EMB_HANDLE_INFORMATION_EXT	=0x8e;
    static private final int XMS_RESIZE_ANY_EXTENDED_MEMORY_BLOCK   =0x8f;

    static private final int 	XMS_FUNCTION_NOT_IMPLEMENTED		=0x80;
    static private final int 	HIGH_MEMORY_NOT_EXIST				=0x90;
    static private final int 	HIGH_MEMORY_IN_USE					=0x91;
    static private final int 	HIGH_MEMORY_NOT_ALLOCATED			=0x93;
    static private final int XMS_OUT_OF_SPACE					    =0xa0;
    static private final int XMS_OUT_OF_HANDLES					    =0xa1;
    static private final int XMS_INVALID_HANDLE					    =0xa2;
    static private final int XMS_INVALID_SOURCE_HANDLE			    =0xa3;
    static private final int XMS_INVALID_SOURCE_OFFSET			    =0xa4;
    static private final int XMS_INVALID_DEST_HANDLE				=0xa5;
    static private final int XMS_INVALID_DEST_OFFSET				=0xa6;
    static private final int XMS_INVALID_LENGTH					    =0xa7;
    static private final int XMS_BLOCK_NOT_LOCKED				    =0xaa;
    static private final int XMS_BLOCK_LOCKED					    =0xab;
    static private final int 	UMB_ONLY_SMALLER_BLOCK				=0xb0;
    static private final int 	UMB_NO_BLOCKS_AVAILABLE				=0xb1;

    private static class XMS_Block {
        /*Bitu*/int	size;
        /*MemHandle*/int mem;
        /*Bit8u*/short locked;
        boolean	free;
    }

//    private static class XMS_MemMove {
//0       Bit32u length;
//4       Bit16u src_handle;
//        union {
//6            RealPt realpt;
//6            Bit32u offset;
//        } src;
//10      Bit16u dest_handle;
//        union {
//12          RealPt realpt;
//12          Bit32u offset;
// 16     } dest;
//    }

    static private /*Bitu*/int XMS_EnableA20(boolean enable) {
        /*Bit8u*/int val = IoHandler.IO_Read(0x92);
        if (enable) IoHandler.IO_Write(0x92,val | 2);
        else		IoHandler.IO_Write(0x92,val & ~2);
        return 0;
    }

    static private /*Bitu*/int XMS_GetEnabledA20() {
        return (IoHandler.IO_Read(0x92)&2)>0?1:0;
    }

    private static /*RealPt*/int xms_callback;
    private static boolean umb_available;

    private static XMS_Block[] xms_handles=new XMS_Block[XMS_HANDLES];

    private static boolean InvalidHandle(/*Bitu*/int handle) {
        return (handle==0 || (handle>=XMS_HANDLES) || xms_handles[handle].free);
    }

    private static /*Bitu*/int XMS_QueryFreeMemory(/*Bit16u*/IntRef largestFree, /*Bit16u*/IntRef totalFree) {
        /* Scan the tree for free memory and find largest free block */
        totalFree.value=(Memory.MEM_FreeTotal()*4);
        largestFree.value=(Memory.MEM_FreeLargest()*4);
        if (totalFree.value==0) return XMS_OUT_OF_SPACE;
        return 0;
    }

    private static /*Bitu*/int XMS_AllocateMemory(/*Bitu*/int size, /*Bit16u*/IntRef handle) {	// size = kb
        /* Find free handle */
        /*Bit16u*/int index=1;
        while (!xms_handles[index].free) {
            if (++index>=XMS_HANDLES) return XMS_OUT_OF_HANDLES;
        }
        /*MemHandle*/int mem;
        if (size!=0) {
            /*Bitu*/int pages=(size/4) + ((size & 3)!=0 ? 1 : 0);
            mem=Memory.MEM_AllocatePages(pages,true);
            if (mem==0) return XMS_OUT_OF_SPACE;
        } else {
            mem=Memory.MEM_GetNextFreePage();
            if (mem==0) Log.log(LogTypes.LOG_MISC, LogSeverities.LOG_ERROR,"XMS:Allocate zero pages with no memory left");
        }
        xms_handles[index].free=false;
        xms_handles[index].mem=mem;
        xms_handles[index].locked=0;
        xms_handles[index].size=size;
        handle.value=index;
        return 0;
    }

    private static /*Bitu*/int XMS_FreeMemory(/*Bitu*/int handle) {
        if (InvalidHandle(handle)) return XMS_INVALID_HANDLE;
        Memory.MEM_ReleasePages(xms_handles[handle].mem);
        xms_handles[handle].mem=-1;
        xms_handles[handle].size=0;
        xms_handles[handle].free=true;
        return 0;
    }

    private static /*Bitu*/int XMS_MoveMemory(/*PhysPt*/int bpt) {
        /* Read the block with mem_read's */
        /*Bitu*/int length=Memory.mem_readd(bpt + 0/*offsetof(XMS_MemMove,length)*/);
        /*Bitu*/int src_handle=Memory.mem_readw(bpt+4/*offsetof(XMS_MemMove,src_handle)*/);
        int src;
        int dest;
        src=Memory.mem_readd(bpt + 6/*offsetof(XMS_MemMove,src.offset)*/);
        /*Bitu*/int dest_handle=Memory.mem_readw(bpt+10/*offsetof(XMS_MemMove,dest_handle)*/);
        dest=Memory.mem_readd(bpt + 12/*offsetof(XMS_MemMove,dest.offset)*/);
        /*PhysPt*/int srcpt,destpt;
        if (src_handle!=0) {
            if (InvalidHandle(src_handle)) {
                return XMS_INVALID_SOURCE_HANDLE;
            }
            if (src>=(xms_handles[src_handle].size*1024)) {
                return XMS_INVALID_SOURCE_OFFSET;
            }
            if (length>xms_handles[src_handle].size*1024-src) {
                return XMS_INVALID_LENGTH;
            }
            srcpt=(xms_handles[src_handle].mem*4096)+src;
        } else {
            srcpt=Memory.Real2Phys(src);
        }
        if (dest_handle!=0) {
            if (InvalidHandle(dest_handle)) {
                return XMS_INVALID_DEST_HANDLE;
            }
            if (dest>=(xms_handles[dest_handle].size*1024)) {
                return XMS_INVALID_DEST_OFFSET;
            }
            if (length>xms_handles[dest_handle].size*1024-dest) {
                return XMS_INVALID_LENGTH;
            }
            destpt=(xms_handles[dest_handle].mem*4096)+dest;
        } else {
            destpt=Memory.Real2Phys(dest);
        }
    //	LOG_MSG("XMS move src %X dest %X length %X",srcpt,destpt,length);
        Memory.mem_memcpy(destpt,srcpt,length);
        return 0;
    }

    static private /*Bitu*/int XMS_LockMemory(/*Bitu*/int handle, /*Bit32u*/IntRef address) {
        if (InvalidHandle(handle)) return XMS_INVALID_HANDLE;
        if (xms_handles[handle].locked<255) xms_handles[handle].locked++;
        address.value = xms_handles[handle].mem*4096;
        return 0;
    }

    static private /*Bitu*/int XMS_UnlockMemory(/*Bitu*/int handle) {
        if (InvalidHandle(handle)) return XMS_INVALID_HANDLE;
        if (xms_handles[handle].locked!=0) {
            xms_handles[handle].locked--;
            return 0;
        }
        return XMS_BLOCK_NOT_LOCKED;
    }

    static private /*Bitu*/int XMS_GetHandleInformation(/*Bitu*/int handle, /*Bit8u*/ShortRef lockCount, /*Bit8u*/ShortRef numFree, /*Bit16u*/IntRef size) {
        if (InvalidHandle(handle)) return XMS_INVALID_HANDLE;
        lockCount.value = xms_handles[handle].locked;
        /* Find available blocks */
        numFree.value=0;
        for (/*Bitu*/int i=1;i<XMS_HANDLES;i++) {
            if (xms_handles[i].free) numFree.value++;
        }
        size.value=(/*Bit16u*/int)(xms_handles[handle].size);
        return 0;
    }

    static private /*Bitu*/int XMS_ResizeMemory(/*Bitu*/int handle, /*Bitu*/int newSize) {
        if (InvalidHandle(handle)) return XMS_INVALID_HANDLE;
        // Block has to be unlocked
        if (xms_handles[handle].locked>0) return XMS_BLOCK_LOCKED;
        /*Bitu*/int pages=newSize/4 + ((newSize & 3)!=0 ? 1 : 0);
        IntRef p = new IntRef(xms_handles[handle].mem);
        if (Memory.MEM_ReAllocatePages(p,pages,true)) {
            xms_handles[handle].mem = p.value;
            xms_handles[handle].size = newSize;
            return 0;
        } else return XMS_OUT_OF_SPACE;
    }

    static private Dos_system.MultiplexHandler multiplex_xms = new Dos_system.MultiplexHandler() {
        public boolean call() {
            switch (CPU_Regs.reg_eax.word()) {
            case 0x4300:					/* XMS installed check */
                    CPU_Regs.reg_eax.low(0x80);
                    return true;
            case 0x4310:					/* XMS handler seg:offset */
                    CPU_Regs.SegSet16ES(Memory.RealSeg(xms_callback));
                    CPU_Regs.reg_ebx.word(Memory.RealOff(xms_callback));
                    return true;
            }
            return false;
        }
    };

    static private void SET_RESULT(/*Bitu*/int res) {
        SET_RESULT(res, true);
    }

    static private void SET_RESULT(/*Bitu*/int res,boolean touch_bl_on_succes/*=true*/) {
        if(touch_bl_on_succes || res!=0) CPU_Regs.reg_ebx.low(res);
        CPU_Regs.reg_eax.word((res==0)?1:0);
    }

    static private Callback.Handler XMS_Handler = new Callback.Handler() {
        public String getName() {
            return "XMS.XMS_Handler";
        }
        public /*Bitu*/int call() {
        	if (Log.level<=LogSeverities.LOG_NORMAL) Log.log(LogTypes.LOG_MISC,LogSeverities.LOG_NORMAL,"XMS: CALL "+Integer.toString(CPU_Regs.reg_eax.high(),16));
            switch (CPU_Regs.reg_eax.high() & 0xFF) {
            case XMS_GET_VERSION:										/* 00 */
                CPU_Regs.reg_eax.word(XMS_VERSION);
                CPU_Regs.reg_ebx.word(XMS_DRIVER_VERSION);
                CPU_Regs.reg_edx.word(0);	/* No we don't have HMA */
                break;
            case XMS_ALLOCATE_HIGH_MEMORY:								/* 01 */
                CPU_Regs.reg_eax.word(0);
                CPU_Regs.reg_ebx.low(HIGH_MEMORY_NOT_EXIST);
                break;
            case XMS_FREE_HIGH_MEMORY:									/* 02 */
                CPU_Regs.reg_eax.word(0);
                CPU_Regs.reg_ebx.low(HIGH_MEMORY_NOT_EXIST);
                break;

            case XMS_GLOBAL_ENABLE_A20:									/* 03 */
            case XMS_LOCAL_ENABLE_A20:									/* 05 */
                SET_RESULT(XMS_EnableA20(true));
                break;
            case XMS_GLOBAL_DISABLE_A20:								/* 04 */
            case XMS_LOCAL_DISABLE_A20:									/* 06 */
                SET_RESULT(XMS_EnableA20(false));
                break;
            case XMS_QUERY_A20:											/* 07 */
                CPU_Regs.reg_eax.word(XMS_GetEnabledA20());
                CPU_Regs.reg_ebx.low(0);
                break;
            case XMS_QUERY_FREE_EXTENDED_MEMORY:						/* 08 */
            {
                IntRef ax = new IntRef(CPU_Regs.reg_eax.dword);
                IntRef dx = new IntRef(CPU_Regs.reg_edx.dword);
                CPU_Regs.reg_ebx.low(XMS_QueryFreeMemory(ax,dx));
                if (ax.value > 65535) /* cap sizes for older DOS programs. newer ones use function 0x88 */
                    ax.value = 65535;
                if (dx.value > 65535)
                    dx.value = 65535;
                CPU_Regs.reg_eax.word(ax.value);
                CPU_Regs.reg_edx.word(dx.value);
            }
                break;
            case XMS_ALLOCATE_ANY_MEMORY:								/* 89 */
                {
                /*Bit16u*/IntRef handle = new IntRef(0);
                SET_RESULT(XMS_AllocateMemory(CPU_Regs.reg_edx.dword,handle));
                CPU_Regs.reg_edx.word(handle.value);
                } break;
            case XMS_ALLOCATE_EXTENDED_MEMORY:							/* 09 */
                {
                /*Bit16u*/IntRef handle = new IntRef(0);
                SET_RESULT(XMS_AllocateMemory(CPU_Regs.reg_edx.word(),handle));
                CPU_Regs.reg_edx.word(handle.value);
                } break;
            case XMS_FREE_EXTENDED_MEMORY:								/* 0a */
                SET_RESULT(XMS_FreeMemory(CPU_Regs.reg_edx.word()));
                break;
            case XMS_MOVE_EXTENDED_MEMORY_BLOCK:						/* 0b */
                SET_RESULT(XMS_MoveMemory(CPU_Regs.reg_dsPhys.dword+CPU_Regs.reg_esi.word()),false);
                break;
            case XMS_LOCK_EXTENDED_MEMORY_BLOCK: {						/* 0c */
                /*Bit32u*/IntRef address=new IntRef(0);
                /*Bitu*/int res = XMS_LockMemory(CPU_Regs.reg_edx.word(), address);
                if(res!=0) CPU_Regs.reg_ebx.low(res);
                CPU_Regs.reg_eax.word((res==0)?1:0);
                if (res==0) { // success
                    CPU_Regs.reg_ebx.word((/*Bit16u*/int)(address.value & 0xFFFF));
                    CPU_Regs.reg_edx.word((/*Bit16u*/int)(address.value >>> 16));
                }
                } break;
            case XMS_UNLOCK_EXTENDED_MEMORY_BLOCK:						/* 0d */
                SET_RESULT(XMS_UnlockMemory(CPU_Regs.reg_edx.word()));
                break;
            case XMS_GET_EMB_HANDLE_INFORMATION:  						/* 0e */
            {
                ShortRef bh = new ShortRef(CPU_Regs.reg_ebx.high());
                ShortRef bl = new ShortRef(CPU_Regs.reg_ebx.low());
                IntRef dx = new IntRef(CPU_Regs.reg_edx.word());
                int res = XMS_GetHandleInformation(CPU_Regs.reg_edx.word(),bh,bl,dx);
                CPU_Regs.reg_ebx.high(bh.value);
                CPU_Regs.reg_ebx.low(bl.value);
                CPU_Regs.reg_edx.word(dx.value);
                SET_RESULT(res,false);
            }
                break;
            case XMS_RESIZE_ANY_EXTENDED_MEMORY_BLOCK:					/* 0x8f */
                if ((CPU_Regs.reg_ebx.dword & 0xFFFFFFFFl) > CPU_Regs.reg_ebx.word()) Log.log_msg("64MB memory limit!");
                //fall through
            case XMS_RESIZE_EXTENDED_MEMORY_BLOCK:						/* 0f */
                SET_RESULT(XMS_ResizeMemory(CPU_Regs.reg_edx.word(), CPU_Regs.reg_ebx.word()));
                break;
            case XMS_ALLOCATE_UMB: {									/* 10 */
                if (!umb_available) {
                    CPU_Regs.reg_eax.word(0);
                    CPU_Regs.reg_ebx.low(XMS_FUNCTION_NOT_IMPLEMENTED);
                    break;
                }
                /*Bit16u*/int umb_start= Dos.dos_infoblock.GetStartOfUMBChain();
                if (umb_start==0xffff) {
                    CPU_Regs.reg_eax.word(0);
                    CPU_Regs.reg_ebx.low(UMB_NO_BLOCKS_AVAILABLE);
                    CPU_Regs.reg_edx.word(0);	// no upper memory available
                    break;
                }
                /* Save status and linkage of upper UMB chain and link upper
                   memory to the regular MCB chain */
                /*Bit8u*/short umb_flag=Dos.dos_infoblock.GetUMBChainState();
                if ((umb_flag&1)==0) Dos_memory.DOS_LinkUMBsToMemChain(1);
                /*Bit8u*/int old_memstrat=Dos_memory.DOS_GetMemAllocStrategy()&0xff;
                Dos_memory.DOS_SetMemAllocStrategy(0x40);	// search in UMBs only

                /*Bit16u*/IntRef size=new IntRef(CPU_Regs.reg_edx.word());/*Bit16u*/IntRef seg=new IntRef(0);
                if (Dos_memory.DOS_AllocateMemory(seg,size)) {
                    CPU_Regs.reg_eax.word(1);
                    CPU_Regs.reg_ebx.word(seg.value);
                } else {
                    CPU_Regs.reg_eax.word(0);
                    if (size.value==0) CPU_Regs.reg_ebx.low(UMB_NO_BLOCKS_AVAILABLE);
                    else CPU_Regs.reg_ebx.low(UMB_ONLY_SMALLER_BLOCK);
                    CPU_Regs.reg_edx.word(size.value);	// size of largest available UMB
                }

                /* Restore status and linkage of upper UMB chain */
                /*Bit8u*/short current_umb_flag=Dos.dos_infoblock.GetUMBChainState();
                if ((current_umb_flag&1)!=(umb_flag&1)) Dos_memory.DOS_LinkUMBsToMemChain(umb_flag);
                Dos_memory.DOS_SetMemAllocStrategy(old_memstrat);
                }
                break;
            case XMS_DEALLOCATE_UMB:									/* 11 */
                if (!umb_available) {
                    CPU_Regs.reg_eax.word(0);
                    CPU_Regs.reg_ebx.low(XMS_FUNCTION_NOT_IMPLEMENTED);
                    break;
                }
                if (Dos.dos_infoblock.GetStartOfUMBChain()!=0xffff) {
                    if (Dos_memory.DOS_FreeMemory(CPU_Regs.reg_edx.word())) {
                        CPU_Regs.reg_eax.word(0x0001);
                        break;
                    }
                }
                CPU_Regs.reg_eax.word(0x0000);
                CPU_Regs.reg_ebx.low(UMB_NO_BLOCKS_AVAILABLE);
                break;
            case XMS_QUERY_ANY_FREE_MEMORY:								/* 88 */
            {
                IntRef ax = new IntRef(CPU_Regs.reg_eax.word());
                IntRef dx = new IntRef(CPU_Regs.reg_edx.word());
                CPU_Regs.reg_ebx.low(XMS_QueryFreeMemory(ax,dx));
                CPU_Regs.reg_eax.word(ax.value);
                CPU_Regs.reg_edx.word(dx.value);
                //CPU_Regs.reg_eax.dword&=0xffff;
                //CPU_Regs.reg_edx.dword&=0xffff;
                CPU_Regs.reg_ecx.dword=(Memory.MEM_TotalPages()*Memory.MEM_PAGESIZE)-1;			// highest known physical memory address
                break;
            }
            case XMS_GET_EMB_HANDLE_INFORMATION_EXT: {					/* 8e */
                /*Bit8u*/ShortRef free_handles=new ShortRef(0);
                ShortRef bh = new ShortRef(CPU_Regs.reg_ebx.high());
                IntRef dx = new IntRef(CPU_Regs.reg_edx.word());
                /*Bitu*/int result = XMS_GetHandleInformation(CPU_Regs.reg_edx.word(),bh,free_handles,dx);
                CPU_Regs.reg_ebx.high(bh.value);
                CPU_Regs.reg_edx.word(dx.value);
                if (result != 0) CPU_Regs.reg_ebx.low(result);
                else {
                    //CPU_Regs.reg_edx.dword&=0xffff;
                    CPU_Regs.reg_ecx.word(free_handles.value);
                }
                CPU_Regs.reg_eax.word((result==0)?1:0);
                } break;
            default:
                if (Log.level<=LogSeverities.LOG_ERROR) Log.log(LogTypes.LOG_MISC,LogSeverities.LOG_ERROR,"XMS: unknown function "+Integer.toString(CPU_Regs.reg_eax.high(),16));
                CPU_Regs.reg_eax.word(0);
                CPU_Regs.reg_ebx.low(XMS_FUNCTION_NOT_IMPLEMENTED);
            }
        //	Log.log(LogTypes.LOG_MISC,LogSeverities.LOG_ERROR,"XMS: CALL Result: %02X",reg_bl);
            return Callback.CBRET_NONE;
        }
    };

    private Callback callbackhandler = new Callback();

    public XMS(Section configuration) {
        super(configuration);
        Section_prop section=(Section_prop)configuration;
        umb_available=false;
        if (!section.Get_bool("xms")) return;
        /*Bitu*/int i;
        Bios.BIOS_ZeroExtendedSize(true);
        Dos_misc.DOS_AddMultiplexHandler(multiplex_xms);

        /* place hookable callback in writable memory area */
        xms_callback=Memory.RealMake(Dos_tables.DOS_GetMemory(0x1)-1,0x10);
        callbackhandler.Install(XMS_Handler,Callback.CB_HOOKABLE,Memory.Real2Phys(xms_callback),"XMS Handler");
        // pseudocode for CB_HOOKABLE:
        //	jump near skip
        //	nop,nop,nop
        //	label skip:
        //	callback XMS_Handler
        //	retf

        for (i=0;i<XMS_HANDLES;i++) {
            xms_handles[i] = new XMS_Block();
            xms_handles[i].free=true;
            xms_handles[i].mem=-1;
            xms_handles[i].size=0;
            xms_handles[i].locked=0;
        }
        /* Disable the 0 handle */
        xms_handles[0].free	= false;

        /* Set up UMB chain */
        umb_available=section.Get_bool("umb");
        boolean ems_available = EMS.GetEMSType(section)>0;
        Dos_memory.DOS_BuildUMBChain(section.Get_bool("umb"),ems_available);
    }

    void ShutDown(){
        Section_prop section = (Section_prop)m_configuration;
        /* Remove upper memory information */
        Dos.dos_infoblock.SetStartOfUMBChain(0xffff);
        if (umb_available) {
            Dos.dos_infoblock.SetUMBChainState((short)0);
            umb_available=false;
        }

        if (!section.Get_bool("xms")) return;
        /* Undo biosclearing */
        Bios.BIOS_ZeroExtendedSize(false);

        /* Remove Multiplex */
        Dos_misc.DOS_DelMultiplexHandler(multiplex_xms);

        /* Free used memory while skipping the 0 handle */
        for (/*Bitu*/int i = 1;i<XMS_HANDLES;i++)
            if(!xms_handles[i].free) XMS_FreeMemory(i);
    }

    static XMS test;

    public static Section.SectionFunction XMS_ShutDown = new Section.SectionFunction() {
        public void call(Section section) {
            test.ShutDown();
            test = null;
            for (int i=0;i<XMS_HANDLES;i++) {
                xms_handles[i] = null;
            }
        }
    };

    public static Section.SectionFunction XMS_Init = new Section.SectionFunction() {
        public void call(Section section) {
            test = new XMS(section);
            section.AddDestroyFunction(XMS_ShutDown,true);
        }
    };
}
