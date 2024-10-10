package jdos.ints;

import jdos.Dosbox;
import jdos.cpu.CPU;
import jdos.cpu.CPU_Regs;
import jdos.cpu.Callback;
import jdos.cpu.Paging;
import jdos.dos.DOS_Device;
import jdos.dos.Dos_devices;
import jdos.dos.Dos_tables;
import jdos.hardware.DMA;
import jdos.hardware.IO;
import jdos.hardware.Memory;
import jdos.misc.Log;
import jdos.misc.setup.Module_base;
import jdos.misc.setup.Section;
import jdos.misc.setup.Section_prop;
import jdos.types.LogSeverities;
import jdos.types.LogTypes;
import jdos.types.MachineType;
import jdos.util.IntRef;
import jdos.util.LongRef;

public class EMS extends Module_base {
    static final private int EMM_PAGEFRAME = 0xE000;
    static final private int EMM_PAGEFRAME4K = ((EMM_PAGEFRAME*16)/4096);
    static final private int EMM_MAX_HANDLES = 200;				/* 255 Max */
    static final private int EMM_PAGE_SIZE = (16*1024);
    static final private int EMM_MAX_PAGES = (32 * 1024 / 16 );
    static final private int EMM_MAX_PHYS = 4;				/* 4 16kb pages in pageframe */

    static final private int EMM_VERSION = 0x40;
    static final private int EMM_MINOR_VERSION = 0x00;
    //static final private int EMM_MINOR_VERSION	0x30	// emm386 4.48
    static final private int GEMMIS_VERSION = 0x0001;	// Version 1.0

    static final private int EMM_SYSTEM_HANDLE = 0x0000;
    static final private int NULL_HANDLE = 0xffff;
    static final private int NULL_PAGE = 0xffff;

    static final private int ENABLE_VCPI = 1;
    static final private int ENABLE_V86_STARTUP = 0;


    /* EMM errors */
    static final private int EMM_NO_ERROR = 0x00;
    static final private int EMM_SOFT_MAL = 0x80;
    static final private int EMM_HARD_MAL = 0x81;
    static final private int EMM_INVALID_HANDLE = 0x83;
    static final private int EMM_FUNC_NOSUP = 0x84;
    static final private int EMM_OUT_OF_HANDLES = 0x85;
    static final private int EMM_SAVEMAP_ERROR = 0x86;
    static final private int EMM_OUT_OF_PHYS = 0x87;
    static final private int EMM_OUT_OF_LOG = 0x88;
    static final private int EMM_ZERO_PAGES = 0x89;
    static final private int EMM_LOG_OUT_RANGE = 0x8a;
    static final private int EMM_ILL_PHYS = 0x8b;
    static final private int EMM_PAGE_MAP_SAVED = 0x8d;
    static final private int EMM_NO_SAVED_PAGE_MAP = 0x8e;
    static final private int EMM_INVALID_SUB = 0x8f;
    static final private int EMM_FEAT_NOSUP = 0x91;
    static final private int EMM_MOVE_OVLAP = 0x92;
    static final private int EMM_MOVE_OVLAPI = 0x97;
    static final private int EMM_NOT_FOUND = 0xa0;


    static private class EMM_Mapping {
        static final int size = 4;
        int handle() {
            return (data[0] & 0xFF) | ((data[1] & 0xFF) << 8);
        }
        int page() {
            return (data[2] & 0xFF) | ((data[3] & 0xFF) << 8);
        }
        void handle(int val) {
            data[0]=(byte)(val & 0xFF);
	        data[1]=(byte)((val >> 8) & 0xFF);
        }
        void page(int val) {
            data[2]=(byte)(val & 0xFF);
	        data[3]=(byte)((val >> 8) & 0xFF);
        }
        byte[] data = new byte[4];
    }

    static private class EMM_Handle {
        public EMM_Handle() {
            for (int i=0;i<page_map.length;i++)
                page_map[i] = new EMM_Mapping();
        }
        /*Bit16u*/int pages;
        /*MemHandle*/int mem;
        String name="";
        boolean saved_page_map;
        EMM_Mapping[] page_map=new EMM_Mapping[EMM_MAX_PHYS];
    }

    private static int ems_type;

    private final static EMM_Handle[] emm_handles = new EMM_Handle[EMM_MAX_HANDLES];
    private final static EMM_Mapping[] emm_mappings = new EMM_Mapping[EMM_MAX_PHYS];
    private final static EMM_Mapping[] emm_segmentmappings = new EMM_Mapping[0x40];

    private static /*Bit16u*/int GEMMIS_seg;

    private static class device_EMM extends DOS_Device {
        public device_EMM(boolean is_emm386_avail) {
            is_emm386=is_emm386_avail;
            SetName("EMMXXXX0");
            GEMMIS_seg=0;
        }
        public boolean Read(byte[] data,/*Bit16u*/IntRef size) {
            return false;
        }
        public boolean Write(byte[] data,/*Bit16u*/IntRef size) {
            Log.log(LogTypes.LOG_IOCTL, LogSeverities.LOG_NORMAL,"EMS:Write to device");
            return false;
        }
        public boolean Seek(/*Bit32u*/LongRef pos,/*Bit32u*/int type) {
            return false;
        }
        public boolean Close() {
            return false;
        }
        public /*Bit16u*/int GetInformation() {
            return 0xc0c0;
        }
        public boolean ReadFromControlChannel(/*PhysPt*/int bufptr,/*Bit16u*/int size,/*Bit16u*/IntRef retcode) {
            /*Bitu*/int subfct= Memory.mem_readb(bufptr);
            switch (subfct) {
                case 0x00:
                    if (size!=6) return false;
                    Memory.mem_writew(bufptr+0x00,0x0023);		// ID
                    Memory.mem_writed(bufptr+0x02,0);			// private API entry point
                    retcode.value=6;
                    return true;
                case 0x01: {
                    if (!is_emm386) return false;
                    if (size!=6) return false;
                    if (GEMMIS_seg==0) GEMMIS_seg= Dos_tables.DOS_GetMemory(0x20);
                    /*PhysPt*/int GEMMIS_addr=Memory.PhysMake(GEMMIS_seg,0);

                    Memory.mem_writew(GEMMIS_addr+0x00,0x0004);			// flags
                    Memory.mem_writew(GEMMIS_addr+0x02,0x019d);			// size of this structure
                    Memory.mem_writew(GEMMIS_addr+0x04,GEMMIS_VERSION);	// version 1.0 (provide ems information only)
                    Memory.mem_writed(GEMMIS_addr+0x06,0);					// reserved

                    /* build non-EMS frames (0-0xe000) */
                    for (/*Bitu*/int frct=0; frct<EMM_PAGEFRAME4K/4; frct++) {
                        Memory.mem_writeb(GEMMIS_addr+0x0a+frct*6,0x00);	// frame type: NONE
                        Memory.mem_writeb(GEMMIS_addr+0x0b+frct*6,0xff);	// owner: NONE
                        Memory.mem_writew(GEMMIS_addr+0x0c+frct*6,0xffff);	// non-EMS frame
                        Memory.mem_writeb(GEMMIS_addr+0x0e + frct*6,0xff);	// EMS page number (NONE)
                        Memory.mem_writeb(GEMMIS_addr+0x0f+frct*6,0xaa);	// flags: direct mapping
                    }
                    /* build EMS page frame (0xe000-0xf000) */
                    for (/*Bitu*/int frct=0; frct<0x10/4; frct++) {
                        /*Bitu*/int frnr=(frct+EMM_PAGEFRAME4K/4)*6;
                        Memory.mem_writeb(GEMMIS_addr+0x0a+frnr,0x03);		// frame type: EMS frame in 64k page
                        Memory.mem_writeb(GEMMIS_addr+0x0b+frnr,0xff);		// owner: NONE
                        Memory.mem_writew(GEMMIS_addr+0x0c+frnr,0x7fff);	// no logical page number
                        Memory.mem_writeb(GEMMIS_addr+0x0e + frnr,(/*Bit8u*/short)(frct&0xff));		// physical EMS page number
                        Memory.mem_writeb(GEMMIS_addr+0x0f+frnr,0x00);		// EMS frame
                    }
                    /* build non-EMS ROM frames (0xf000-0x10000) */
                    for (/*Bitu*/int frct=(EMM_PAGEFRAME4K+0x10)/4; frct<0xf0/4; frct++) {
                        Memory.mem_writeb(GEMMIS_addr+0x0a+frct*6,0x00);	// frame type: NONE
                        Memory.mem_writeb(GEMMIS_addr+0x0b+frct*6,0xff);	// owner: NONE
                        Memory.mem_writew(GEMMIS_addr+0x0c+frct*6,0xffff);	// non-EMS frame
                        Memory.mem_writeb(GEMMIS_addr+0x0e + frct*6,0xff);	// EMS page number (NONE)
                        Memory.mem_writeb(GEMMIS_addr+0x0f+frct*6,0xaa);	// flags: direct mapping
                    }

                    Memory.mem_writeb(GEMMIS_addr+0x18a,0x74);			// ???
                    Memory.mem_writeb(GEMMIS_addr+0x18b,0x00);			// no UMB descriptors following
                    Memory.mem_writeb(GEMMIS_addr+0x18c,0x01);			// 1 EMS handle info recort
                    Memory.mem_writew(GEMMIS_addr+0x18d,0x0000);		// system handle
                    Memory.mem_writed(GEMMIS_addr+0x18f,0);			// handle name
                    Memory.mem_writed(GEMMIS_addr+0x193,0);			// handle name
                    if (emm_handles[EMM_SYSTEM_HANDLE].pages != NULL_HANDLE) {
                        Memory.mem_writew(GEMMIS_addr+0x197,(emm_handles[EMM_SYSTEM_HANDLE].pages+3)/4);
                        Memory.mem_writed(GEMMIS_addr+0x199,emm_handles[EMM_SYSTEM_HANDLE].mem<<12);	// physical address
                    } else {
                        Memory.mem_writew(GEMMIS_addr+0x197,0x0001);		// system handle
                        Memory.mem_writed(GEMMIS_addr+0x199,0x00110000);	// physical address
                    }

                    /* fill buffer with import structure */
                    Memory.mem_writed(bufptr+0x00,GEMMIS_seg<<4);
                    Memory.mem_writew(bufptr+0x04,GEMMIS_VERSION);
                    retcode.value=6;
                    return true;
                    }
                case 0x02:
                    if (!is_emm386) return false;
                    if (size!=2) return false;
                    Memory.mem_writeb(bufptr+0x00,EMM_VERSION>>4);		// version 4
                    Memory.mem_writeb(bufptr+0x01,EMM_MINOR_VERSION);
                    retcode.value=2;
                    return true;
                case 0x03:
                    if (!is_emm386) return false;
                    if (EMM_MINOR_VERSION < 0x2d) return false;
                    if (size!=4) return false;
                    Memory.mem_writew(bufptr+0x00,(/*Bit16u*/int)(Memory.MEM_TotalPages()*4));	// max size (kb)
                    Memory.mem_writew(bufptr+0x02,0x80);							// min size (kb)
                    retcode.value=2;
                    return true;
            }
            return false;
        }
        public boolean WriteToControlChannel(/*PhysPt*/int bufptr,/*Bit16u*/int size,/*Bit16u*/IntRef retcode) {
            return true;
        }
        private /*Bit8u*/short cache;
        private boolean is_emm386;
    }

    static private class Vcpi {
        boolean enabled;
        /*Bit16u*/int ems_handle;
        /*Bitu*/int pm_interface;
        /*MemHandle*/int private_area;
        /*Bit8u*/short pic1_remapping,pic2_remapping;
    }
    static private Vcpi vcpi = new Vcpi();

    private static class MoveRegion {
        /*Bit32u*/long bytes;
        /*Bit8u*/short src_type;
        /*Bit16u*/int src_handle;
        /*Bit16u*/int src_offset;
        /*Bit16u*/int src_page_seg;
        /*Bit8u*/short dest_type;
        /*Bit16u*/int dest_handle;
        /*Bit16u*/int dest_offset;
        /*Bit16u*/int dest_page_seg;
    }

    private static /*Bit16u*/int EMM_GetFreePages() {
        /*Bitu*/int count=Memory.MEM_FreeTotal()/4;
        if (count>0x7fff) count=0x7fff;
        return (/*Bit16u*/int)count;
    }

    private static boolean ValidHandle(/*Bit16u*/int handle) {
        if (handle>=EMM_MAX_HANDLES) return false;
        if (emm_handles[handle].pages==NULL_HANDLE) return false;
        return true;
    }

    private static /*Bit8u*/short EMM_AllocateMemory(/*Bit16u*/int pages, /*Bit16u*/IntRef dhandle, boolean can_allocate_zpages) {
        /* Check for 0 page allocation */
        if (pages==0) {
            if (!can_allocate_zpages) return EMM_ZERO_PAGES;
        }
        /* Check for enough free pages */
        if ((Memory.MEM_FreeTotal()/ 4) < pages) { return EMM_OUT_OF_LOG;}
        /*Bit16u*/int handle = 1;
        /* Check for a free handle */
        while (emm_handles[handle].pages != NULL_HANDLE) {
            if (++handle >= EMM_MAX_HANDLES) {return EMM_OUT_OF_HANDLES;}
        }
        /*MemHandle*/int mem = 0;
        if (pages!=0) {
            mem = Memory.MEM_AllocatePages(pages*4,false);
            if (mem==0) Log.exit("EMS:Memory allocation failure");
        }
        emm_handles[handle].pages = pages;
        emm_handles[handle].mem = mem;
        /* Change handle only if there is no error. */
        dhandle.value = handle;
        return EMM_NO_ERROR;
    }

    private static /*Bit8u*/short EMM_AllocateSystemHandle(/*Bit16u*/int pages) {
        /* Check for enough free pages */
        if ((Memory.MEM_FreeTotal()/ 4) < pages) { return EMM_OUT_OF_LOG;}
        /*Bit16u*/int handle = EMM_SYSTEM_HANDLE;	// emm system handle (reserved for OS usage)
        /* Release memory if already allocated */
        if (emm_handles[handle].pages != NULL_HANDLE) {
            Memory.MEM_ReleasePages(emm_handles[handle].mem);
        }
        /*MemHandle*/int mem = Memory.MEM_AllocatePages(pages*4,false);
        if (mem==0) Log.exit("EMS:System handle memory allocation failure");
        emm_handles[handle].pages = pages;
        emm_handles[handle].mem = mem;
        return EMM_NO_ERROR;
    }

    static /*Bit8u*/short EMM_ReallocatePages(/*Bit16u*/int handle,/*Bit16u*/int pages) {
        /* Check for valid handle */
        if (!ValidHandle(handle)) return EMM_INVALID_HANDLE;
        if (emm_handles[handle].pages != 0) {
            /* Check for enough pages */
            IntRef mem = new IntRef(emm_handles[handle].mem);
            if (!Memory.MEM_ReAllocatePages(mem,pages*4,false)) return EMM_OUT_OF_LOG;
            emm_handles[handle].mem = mem.value;
        } else {
            /*MemHandle*/int mem = Memory.MEM_AllocatePages(pages*4,false);
            if (mem==0) Log.exit("EMS:Memory allocation failure during reallocation");
            emm_handles[handle].mem = mem;
        }
        /* Update size */
        emm_handles[handle].pages=pages;
        return EMM_NO_ERROR;
    }

    private static /*Bit8u*/short EMM_MapPage(/*Bitu*/int phys_page,/*Bit16u*/int handle,/*Bit16u*/int log_page) {
    //	LOG_MSG("EMS MapPage handle %d phys %d log %d",handle,phys_page,log_page);
        /* Check for too high physical page */
        if (phys_page>=EMM_MAX_PHYS) return EMM_ILL_PHYS;

        /* unmapping doesn't need valid handle (as handle isn't used) */
        if (log_page==NULL_PAGE) {
            /* Unmapping */
            emm_mappings[phys_page].handle(NULL_HANDLE);
            emm_mappings[phys_page].page(NULL_PAGE);
            for (/*Bitu*/int i=0;i<4;i++)
                Paging.PAGING_MapPage(EMM_PAGEFRAME4K+phys_page*4+i,EMM_PAGEFRAME4K+phys_page*4+i);
            Paging.PAGING_ClearTLB();
            return EMM_NO_ERROR;
        }
        /* Check for valid handle */
        if (!ValidHandle(handle)) return EMM_INVALID_HANDLE;

        if (log_page<emm_handles[handle].pages) {
            /* Mapping it is */
            emm_mappings[phys_page].handle(handle);
            emm_mappings[phys_page].page(log_page);

            /*MemHandle*/int memh=Memory.MEM_NextHandleAt(emm_handles[handle].mem,log_page*4);;
            for (/*Bitu*/int i=0;i<4;i++) {
                Paging.PAGING_MapPage(EMM_PAGEFRAME4K+phys_page*4+i,memh);
                memh=Memory.MEM_NextHandle(memh);
            }
            Paging.PAGING_ClearTLB();
            return EMM_NO_ERROR;
        } else  {
            /* Illegal logical page it is */
            return EMM_LOG_OUT_RANGE;
        }
    }

    private static /*Bit8u*/short EMM_MapSegment(/*Bitu*/int segment,/*Bit16u*/int handle,/*Bit16u*/int log_page) {
    //	LOG_MSG("EMS MapSegment handle %d segment %d log %d",handle,segment,log_page);

        boolean valid_segment=false;

        if ((ems_type==1) || (ems_type==3)) {
            if (segment<0xf000+0x1000) valid_segment=true;
        } else {
            if ((segment>=0xa000) && (segment<0xb000)) {
                valid_segment=true;		// allow mapping of graphics memory
            }
            if ((segment>=EMM_PAGEFRAME) && (segment<EMM_PAGEFRAME+0x1000)) {
                valid_segment=true;		// allow mapping of EMS page frame
            }
    /*		if ((segment>=EMM_PAGEFRAME-0x1000) && (segment<EMM_PAGEFRAME)) {
                valid_segment=true;
            } */
        }

	    if (valid_segment) {
            /*Bit32s*/int tphysPage = ((/*Bit32s*/int)segment-EMM_PAGEFRAME)/(0x1000/EMM_MAX_PHYS);

            /* unmapping doesn't need valid handle (as handle isn't used) */
            if (log_page==NULL_PAGE) {
                /* Unmapping */
                if ((tphysPage>=0) && (tphysPage<EMM_MAX_PHYS)) {
                    emm_mappings[tphysPage].handle(NULL_HANDLE);
                    emm_mappings[tphysPage].page(NULL_PAGE);
                } else {
                    emm_segmentmappings[segment>>10].handle(NULL_HANDLE);
                    emm_segmentmappings[segment>>10].page(NULL_PAGE);
                }
                for (/*Bitu*/int i=0;i<4;i++)
                    Paging.PAGING_MapPage(segment*16/4096+i,segment*16/4096+i);
                Paging.PAGING_ClearTLB();
                return EMM_NO_ERROR;
            }
            /* Check for valid handle */
            if (!ValidHandle(handle)) return EMM_INVALID_HANDLE;

            if (log_page<emm_handles[handle].pages) {
                /* Mapping it is */
                if ((tphysPage>=0) && (tphysPage<EMM_MAX_PHYS)) {
                    emm_mappings[tphysPage].handle(handle);
                    emm_mappings[tphysPage].page(log_page);
                } else {
                    emm_segmentmappings[segment>>10].handle(handle);
                    emm_segmentmappings[segment>>10].page(log_page);
                }

                /*MemHandle*/int memh=Memory.MEM_NextHandleAt(emm_handles[handle].mem,log_page*4);
                for (/*Bitu*/int i=0;i<4;i++) {
                    Paging.PAGING_MapPage(segment*16/4096+i,memh);
                    memh=Memory.MEM_NextHandle(memh);
                }
                Paging.PAGING_ClearTLB();
                return EMM_NO_ERROR;
            } else  {
                /* Illegal logical page it is */
                return EMM_LOG_OUT_RANGE;
            }
        }

        return EMM_ILL_PHYS;
    }

    private static /*Bit8u*/short EMM_ReleaseMemory(/*Bit16u*/int handle) {
        /* Check for valid handle */
        if (!ValidHandle(handle)) return EMM_INVALID_HANDLE;

        // should check for saved_page_map flag here, returning an error if it's true
        // as apps are required to restore the pagemap beforehand; to be checked
    //	if (emm_handles[handle].saved_page_map) return EMM_SAVEMAP_ERROR;

        if (emm_handles[handle].pages != 0) {
            Memory.MEM_ReleasePages(emm_handles[handle].mem);
        }
        /* Reset handle */
        emm_handles[handle].mem=0;
        if (handle==0) {
            emm_handles[handle].pages=0;	// OS handle is NEVER deallocated
        } else {
            emm_handles[handle].pages=NULL_HANDLE;
        }
        emm_handles[handle].saved_page_map=false;
        emm_handles[handle].name="";
        return EMM_NO_ERROR;
    }

    private static /*Bit8u*/short EMM_SavePageMap(/*Bit16u*/int handle) {
        /* Check for valid handle */
        if (handle>=EMM_MAX_HANDLES || emm_handles[handle].pages==NULL_HANDLE) {
            if (handle!=0) return EMM_INVALID_HANDLE;
        }
        /* Check for previous save */
        if (emm_handles[handle].saved_page_map) return EMM_PAGE_MAP_SAVED;
        /* Copy the mappings over */
        for (/*Bitu*/int i=0;i<EMM_MAX_PHYS;i++) {
            emm_handles[handle].page_map[i].page(emm_mappings[i].page());
            emm_handles[handle].page_map[i].handle(emm_mappings[i].handle());
        }
        emm_handles[handle].saved_page_map=true;
        return EMM_NO_ERROR;
    }

    private static /*Bit8u*/short EMM_RestoreMappingTable() {
        /*Bit8u*/short result;
        /* Move through the mappings table and setup mapping accordingly */
        for (/*Bitu*/int i=0;i<0x40;i++) {
            /* Skip the pageframe */
            if ((i>=EMM_PAGEFRAME/0x400) && (i<(EMM_PAGEFRAME/0x400)+EMM_MAX_PHYS)) continue;
            result=EMM_MapSegment(i<<10,emm_segmentmappings[i].handle(),emm_segmentmappings[i].page());
        }
        for (/*Bitu*/int i=0;i<EMM_MAX_PHYS;i++) {
            result=EMM_MapPage(i,emm_mappings[i].handle(),emm_mappings[i].page());
        }
        return EMM_NO_ERROR;
    }
    private static /*Bit8u*/short EMM_RestorePageMap(/*Bit16u*/int handle) {
        /* Check for valid handle */
        if (handle>=EMM_MAX_HANDLES || emm_handles[handle].pages==NULL_HANDLE) {
            if (handle!=0) return EMM_INVALID_HANDLE;
        }
        /* Check for previous save */
        if (!emm_handles[handle].saved_page_map) return EMM_NO_SAVED_PAGE_MAP;
        /* Restore the mappings */
        emm_handles[handle].saved_page_map=false;
        for (/*Bitu*/int i=0;i<EMM_MAX_PHYS;i++) {
            emm_mappings[i].page(emm_handles[handle].page_map[i].page());
            emm_mappings[i].handle(emm_handles[handle].page_map[i].handle());
        }
        return EMM_RestoreMappingTable();
    }

    private static /*Bit8u*/short EMM_GetPagesForAllHandles(/*PhysPt*/int table,/*Bit16u*/IntRef handles) {
        handles.value=0;
        for (/*Bit16u*/int i=0;i<EMM_MAX_HANDLES;i++) {
            if (emm_handles[i].pages!=NULL_HANDLE) {
                handles.value++;
                Memory.mem_writew(table,i);
                Memory.mem_writew(table+2,emm_handles[i].pages);
                table+=4;
            }
        }
        return EMM_NO_ERROR;
    }

    private static /*Bit8u*/short EMM_PartialPageMapping() {
        /*PhysPt*/int list;/*Bit16u*/int count;
        int data;
        switch (CPU_Regs.reg_eax.low()) {
        case 0x00:	/* Save Partial Page Map */
            list = CPU_Regs.reg_dsPhys.dword+CPU_Regs.reg_esi.word();
            data = CPU_Regs.reg_esPhys.dword+CPU_Regs.reg_edi.word();
            count=Memory.mem_readw(list);list+=2;
            Memory.mem_writew(data,count);data+=2;
            for (;count>0;count--) {
                /*Bit16u*/int segment=Memory.mem_readw(list);list+=2;
                if ((segment>=EMM_PAGEFRAME) && (segment<EMM_PAGEFRAME+0x1000)) {
                    /*Bit16u*/int page = (segment-EMM_PAGEFRAME) / (EMM_PAGE_SIZE>>4);
                    Memory.mem_writew(data,segment);data+=2;
                    Memory.MEM_BlockWrite(data,emm_mappings[page].data,EMM_Mapping.size);
                    data+=EMM_Mapping.size;
                } else if ((ems_type==1) || (ems_type==3) || ((segment>=EMM_PAGEFRAME-0x1000) && (segment<EMM_PAGEFRAME)) || ((segment>=0xa000) && (segment<0xb000))) {
                    Memory.mem_writew(data,segment);data+=2;
                    Memory.MEM_BlockWrite(data,emm_segmentmappings[segment>>10].data,EMM_Mapping.size);
                    data+=EMM_Mapping.size;
                } else {
                    return EMM_ILL_PHYS;
                }
            }
            break;
        case 0x01:	/* Restore Partial Page Map */
            data = (int)CPU_Regs.reg_dsPhys.dword+CPU_Regs.reg_esi.word();
            count= Memory.mem_readw(data);data+=2;
            for (;count>0;count--) {
                /*Bit16u*/int segment=Memory.mem_readw(data);data+=2;
                if ((segment>=EMM_PAGEFRAME) && (segment<EMM_PAGEFRAME+0x1000)) {
                    /*Bit16u*/int page = (segment-EMM_PAGEFRAME) / (EMM_PAGE_SIZE>>4);
                    Memory.MEM_BlockRead(data,emm_mappings[page].data,EMM_Mapping.size);
                } else if ((ems_type==1) || (ems_type==3) || ((segment>=EMM_PAGEFRAME-0x1000) && (segment<EMM_PAGEFRAME)) || ((segment>=0xa000) && (segment<0xb000))) {
                    Memory.MEM_BlockRead(data,emm_segmentmappings[segment>>10].data,EMM_Mapping.size);
                } else {
                    return EMM_ILL_PHYS;
                }
                data+=EMM_Mapping.size;
            }
            return EMM_RestoreMappingTable();
        case 0x02:	/* Get Partial Page Map Array Size */
            CPU_Regs.reg_eax.low((/*Bit8u*/short)(2+CPU_Regs.reg_ebx.word()*(2+EMM_Mapping.size)));
            break;
        default:
            if (Log.level<=LogSeverities.LOG_ERROR) Log.log(LogTypes.LOG_MISC,LogSeverities.LOG_ERROR,"EMS:Call "+Integer.toString(CPU_Regs.reg_eax.high(), 16)+" Subfunction "+Integer.toString(CPU_Regs.reg_eax.low(), 16)+" not supported");
            return EMM_FUNC_NOSUP;
        }
        return EMM_NO_ERROR;
    }

    private static /*Bit8u*/short HandleNameSearch() {
        String name;
        /*Bit16u*/int handle=0;/*PhysPt*/int data;
        switch (CPU_Regs.reg_eax.low()) {
        case 0x00:	/* Get all handle names */
            CPU_Regs.reg_eax.low(0);data=CPU_Regs.reg_esPhys.dword+CPU_Regs.reg_edi.word();
            for (handle=0;handle<EMM_MAX_HANDLES;handle++) {
                if (emm_handles[handle].pages!=NULL_HANDLE) {
                    CPU_Regs.reg_eax.low(CPU_Regs.reg_eax.low()+1);
                    Memory.mem_writew(data,handle);
                    Memory.MEM_BlockWrite(data+2,emm_handles[handle].name,8);
                    data+=10;
                }
            }
            break;
        case 0x01: /* Search for a handle name */
            name = Memory.MEM_StrCopy(CPU_Regs.reg_dsPhys.dword+CPU_Regs.reg_esi.word(),8);
            for (handle=0;handle<EMM_MAX_HANDLES;handle++) {
                if (emm_handles[handle].pages!=NULL_HANDLE) {
                    if (name.equalsIgnoreCase(emm_handles[handle].name)) {
                        CPU_Regs.reg_edx.word(handle);
                        return EMM_NO_ERROR;
                    }
                }
            }
            return EMM_NOT_FOUND;
        case 0x02: /* Get Total number of handles */
            CPU_Regs.reg_ebx.word(EMM_MAX_HANDLES);
            break;
        default:
            if (Log.level<=LogSeverities.LOG_ERROR) Log.log(LogTypes.LOG_MISC,LogSeverities.LOG_ERROR,"EMS:Call "+Integer.toString(CPU_Regs.reg_eax.high(), 16)+" Subfunction "+Integer.toString(CPU_Regs.reg_eax.low(), 16)+" not supported");
            return EMM_INVALID_SUB;
        }
        return EMM_NO_ERROR;
    }

    private static /*Bit8u*/short GetSetHandleName() {
        /*Bit16u*/int handle=CPU_Regs.reg_edx.word();
        switch (CPU_Regs.reg_eax.low()) {
        case 0x00:	/* Get Handle Name */
            if (handle>=EMM_MAX_HANDLES || emm_handles[handle].pages==NULL_HANDLE) return EMM_INVALID_HANDLE;
            Memory.MEM_BlockWrite(CPU_Regs.reg_esPhys.dword+CPU_Regs.reg_edi.word(),emm_handles[handle].name,8);
            break;
        case 0x01:	/* Set Handle Name */
            if (handle>=EMM_MAX_HANDLES || emm_handles[handle].pages==NULL_HANDLE) return EMM_INVALID_HANDLE;
            emm_handles[handle].name=Memory.MEM_BlockRead(CPU_Regs.reg_esPhys.dword+CPU_Regs.reg_edi.word(),8);
            break;
        default:
            if (Log.level<=LogSeverities.LOG_ERROR) Log.log(LogTypes.LOG_MISC,LogSeverities.LOG_ERROR,"EMS:Call "+Integer.toString(CPU_Regs.reg_eax.high(), 16)+" Subfunction "+Integer.toString(CPU_Regs.reg_eax.low(), 16)+" not supported");
            return EMM_INVALID_SUB;
        }
        return EMM_NO_ERROR;

    }


    private static void LoadMoveRegion(/*PhysPt*/int data, MoveRegion region) {
        region.bytes=Memory.mem_readd(data + 0x0);

        region.src_type=(short)Memory.mem_readb(data+0x4);
        region.src_handle=Memory.mem_readw(data+0x5);
        region.src_offset=Memory.mem_readw(data+0x7);
        region.src_page_seg=Memory.mem_readw(data+0x9);

        region.dest_type=(short)Memory.mem_readb(data+0xb);
        region.dest_handle=Memory.mem_readw(data+0xc);
        region.dest_offset=Memory.mem_readw(data+0xe);
        region.dest_page_seg=Memory.mem_readw(data+0x10);
    }

    private static /*Bit8u*/short MemoryRegion() {
        MoveRegion region=new MoveRegion();
        /*Bit8u*/byte[] buf_src=new byte[Paging.MEM_PAGE_SIZE];
        /*Bit8u*/byte[] buf_dest=new byte[Paging.MEM_PAGE_SIZE];
        if (CPU_Regs.reg_eax.low()>1) {
            if (Log.level<=LogSeverities.LOG_ERROR) Log.log(LogTypes.LOG_MISC,LogSeverities.LOG_ERROR,"EMS:Call "+Integer.toString(CPU_Regs.reg_eax.high(), 16)+" Subfunction "+Integer.toString(CPU_Regs.reg_eax.low(), 16)+" not supported");
            return EMM_FUNC_NOSUP;
        }
        LoadMoveRegion(CPU_Regs.reg_dsPhys.dword+CPU_Regs.reg_esi.word(),region);
        /* Parse the region for information */
        /*PhysPt*/int src_mem = 0,dest_mem = 0;
        /*MemHandle*/int src_handle = 0,dest_handle = 0;
        /*Bitu*/int src_off = 0,dest_off = 0 ;/*Bitu*/int src_remain = 0,dest_remain = 0;
        if (region.src_type==0) {
            src_mem=region.src_page_seg*16+region.src_offset;
        } else {
            if (!ValidHandle(region.src_handle)) return EMM_INVALID_HANDLE;
            if ((emm_handles[region.src_handle].pages*EMM_PAGE_SIZE) < ((region.src_page_seg*EMM_PAGE_SIZE)+region.src_offset+region.bytes)) return EMM_LOG_OUT_RANGE;
            src_handle=emm_handles[region.src_handle].mem;
            /*Bitu*/int pages=region.src_page_seg*4+(region.src_offset/Paging.MEM_PAGE_SIZE);
            for (;pages>0;pages--) src_handle=Memory.MEM_NextHandle(src_handle);
            src_off=region.src_offset&(Paging.MEM_PAGE_SIZE-1);
            src_remain=Paging.MEM_PAGE_SIZE-src_off;
        }
        if (region.dest_type==0) {
            dest_mem=region.dest_page_seg*16+region.dest_offset;
        } else {
            if (!ValidHandle(region.dest_handle)) return EMM_INVALID_HANDLE;
            if (emm_handles[region.dest_handle].pages*EMM_PAGE_SIZE < (region.dest_page_seg*EMM_PAGE_SIZE)+region.dest_offset+region.bytes) return EMM_LOG_OUT_RANGE;
            dest_handle=emm_handles[region.dest_handle].mem;
            /*Bitu*/int pages=region.dest_page_seg*4+(region.dest_offset/Paging.MEM_PAGE_SIZE);
            for (;pages>0;pages--) dest_handle=Memory.MEM_NextHandle(dest_handle);
            dest_off=region.dest_offset&(Paging.MEM_PAGE_SIZE-1);
            dest_remain=Paging.MEM_PAGE_SIZE-dest_off;
        }
        /*Bitu*/int toread;
        while (region.bytes>0) {
            if (region.bytes>Paging.MEM_PAGE_SIZE) toread=Paging.MEM_PAGE_SIZE;
            else toread=(int)region.bytes;
            /* Read from the source */
            if (region.src_type==0) {
                Memory.MEM_BlockRead(src_mem,buf_src,toread);
            } else {
                if (toread<src_remain) {
                    Memory.MEM_BlockRead((src_handle*Paging.MEM_PAGE_SIZE)+src_off,buf_src,toread);
                } else {
                    Memory.MEM_BlockRead((src_handle*Paging.MEM_PAGE_SIZE)+src_off,buf_src,src_remain);
                    Memory.MEM_BlockRead((Memory.MEM_NextHandle(src_handle)*Paging.MEM_PAGE_SIZE),buf_src,src_remain,toread-src_remain);
                }
            }
            /* Check for a move */
            if (CPU_Regs.reg_eax.low()==1) {
                /* Read from the destination */
                if (region.dest_type==0) {
                    Memory.MEM_BlockRead(dest_mem,buf_dest,toread);
                } else {
                    if (toread<dest_remain) {
                        Memory.MEM_BlockRead((dest_handle*Paging.MEM_PAGE_SIZE)+dest_off,buf_dest,toread);
                    } else {
                        Memory.MEM_BlockRead((dest_handle*Paging.MEM_PAGE_SIZE)+dest_off,buf_dest,dest_remain);
                        Memory.MEM_BlockRead((Memory.MEM_NextHandle(dest_handle)*Paging.MEM_PAGE_SIZE),buf_dest, dest_remain, toread-dest_remain);
                    }
                }
                /* Write to the source */
                if (region.src_type==0) {
                    Memory.MEM_BlockWrite(src_mem,buf_dest,toread);
                } else {
                    if (toread<src_remain) {
                        Memory.MEM_BlockWrite((src_handle*Paging.MEM_PAGE_SIZE)+src_off,buf_dest,toread);
                    } else {
                        Memory.MEM_BlockWrite((src_handle*Paging.MEM_PAGE_SIZE)+src_off,buf_dest,src_remain);
                        Memory.MEM_BlockWrite((Memory.MEM_NextHandle(src_handle)*Paging.MEM_PAGE_SIZE),buf_dest, src_remain,toread-src_remain);
                    }
                }
            }
            /* Write to the destination */
            if (region.dest_type==0) {
                Memory.MEM_BlockWrite(dest_mem,buf_src,toread);
            } else {
                if (toread<dest_remain) {
                    Memory.MEM_BlockWrite((dest_handle*Paging.MEM_PAGE_SIZE)+dest_off,buf_src,toread);
                } else {
                    Memory.MEM_BlockWrite((dest_handle*Paging.MEM_PAGE_SIZE)+dest_off,buf_src,dest_remain);
                    Memory.MEM_BlockWrite((Memory.MEM_NextHandle(dest_handle)*Paging.MEM_PAGE_SIZE),buf_src,dest_remain,toread-dest_remain);
                }
            }
            /* Advance the pointers */
            if (region.src_type==0) src_mem+=toread;
            else src_handle=Memory.MEM_NextHandle(src_handle);
            if (region.dest_type==0) dest_mem+=toread;
            else dest_handle=Memory.MEM_NextHandle(dest_handle);
            region.bytes-=toread;
        }
        return EMM_NO_ERROR;
    }

    private static Callback.Handler INT67_Handler = new Callback.Handler() {
        public String getName() {
            return "EMS.INT67_Handler";
        }
        public /*Bitu*/int call() {
            /*Bitu*/int i;
            switch (CPU_Regs.reg_eax.high()) {
            case 0x40:		/* Get Status */
                CPU_Regs.reg_eax.high(EMM_NO_ERROR);
                break;
            case 0x41:		/* Get PageFrame Segment */
                CPU_Regs.reg_ebx.word(EMM_PAGEFRAME);
                CPU_Regs.reg_eax.high(EMM_NO_ERROR);
                break;
            case 0x42:		/* Get number of pages */
                CPU_Regs.reg_edx.word((/*Bit16u*/int)(Memory.MEM_TotalPages()/4));		//Not entirely correct but okay
                CPU_Regs.reg_ebx.word(EMM_GetFreePages());
                CPU_Regs.reg_eax.high(EMM_NO_ERROR);
                break;
            case 0x43:		/* Get Handle and Allocate Pages */
            {
                IntRef dx = new IntRef(CPU_Regs.reg_edx.word());
                CPU_Regs.reg_eax.high(EMM_AllocateMemory(CPU_Regs.reg_ebx.word(),dx,false));
                CPU_Regs.reg_edx.word(dx.value);
                break;
            }
            case 0x44:		/* Map Expanded Memory Page */
                CPU_Regs.reg_eax.high(EMM_MapPage(CPU_Regs.reg_eax.low(),CPU_Regs.reg_edx.word(),CPU_Regs.reg_ebx.word()));
                break;
            case 0x45:		/* Release handle and free pages */
                CPU_Regs.reg_eax.high(EMM_ReleaseMemory(CPU_Regs.reg_edx.word()));
                break;
            case 0x46:		/* Get EMM Version */
                CPU_Regs.reg_eax.high(EMM_NO_ERROR);
                CPU_Regs.reg_eax.low(EMM_VERSION);
                break;
            case 0x47:		/* Save Page Map */
                CPU_Regs.reg_eax.high(EMM_SavePageMap(CPU_Regs.reg_edx.word()));
                break;
            case 0x48:		/* Restore Page Map */
                CPU_Regs.reg_eax.high(EMM_RestorePageMap(CPU_Regs.reg_edx.word()));
                break;
            case 0x4b:		/* Get Handle Count */
                CPU_Regs.reg_ebx.word(0);
                for (i=0;i<EMM_MAX_HANDLES;i++) if (emm_handles[i].pages!=NULL_HANDLE) CPU_Regs.reg_ebx.word(CPU_Regs.reg_ebx.word()+1);
                CPU_Regs.reg_eax.high(EMM_NO_ERROR);
                break;
            case 0x4c:		/* Get Pages for one Handle */
                if (!ValidHandle(CPU_Regs.reg_edx.word())) {CPU_Regs.reg_eax.high(EMM_INVALID_HANDLE);break;}
                CPU_Regs.reg_ebx.word(emm_handles[CPU_Regs.reg_edx.word()].pages);
                CPU_Regs.reg_eax.high(EMM_NO_ERROR);
                break;
            case 0x4d:		/* Get Pages for all Handles */
                IntRef bx = new IntRef(CPU_Regs.reg_ebx.word());
                CPU_Regs.reg_eax.high(EMM_GetPagesForAllHandles(CPU_Regs.reg_esPhys.dword+CPU_Regs.reg_edi.word(),bx));
                CPU_Regs.reg_ebx.word(bx.value);
                break;
            case 0x4e:		/*Save/Restore Page Map */
                switch (CPU_Regs.reg_eax.low()) {
                case 0x00:	/* Save Page Map */
                {
                    int offset = CPU_Regs.reg_esPhys.dword+CPU_Regs.reg_edi.word();
                    for (int j=0;j<emm_mappings.length;j++)
                        Memory.MEM_BlockWrite(offset+j*EMM_Mapping.size,emm_mappings[j].data,EMM_Mapping.size);
                    CPU_Regs.reg_eax.high(EMM_NO_ERROR);
                    break;
                }
                case 0x01:	/* Restore Page Map */
                {
                    int offset = CPU_Regs.reg_dsPhys.dword+CPU_Regs.reg_esi.word();
                    for (int j=0;j<emm_mappings.length;j++)
                        Memory.MEM_BlockRead(offset+j*EMM_Mapping.size,emm_mappings[j].data,EMM_Mapping.size);
                    CPU_Regs.reg_eax.high(EMM_RestoreMappingTable());
                    break;
                }
                case 0x02:	/* Save and Restore Page Map */
                {
                    int offset = CPU_Regs.reg_esPhys.dword+CPU_Regs.reg_edi.word();
                    for (int j=0;j<emm_mappings.length;j++)
                        Memory.MEM_BlockWrite(offset+j*EMM_Mapping.size,emm_mappings[j].data,EMM_Mapping.size);

                    offset = CPU_Regs.reg_dsPhys.dword+CPU_Regs.reg_esi.word();
                    for (int j=0;j<emm_mappings.length;j++)
                        Memory.MEM_BlockRead(offset+j*EMM_Mapping.size,emm_mappings[j].data,EMM_Mapping.size);

                    CPU_Regs.reg_eax.high(EMM_RestoreMappingTable());
                    break;
                }
                case 0x03:	/* Get Page Map Array Size */
                    CPU_Regs.reg_eax.low(EMM_Mapping.size*emm_mappings.length);
                    CPU_Regs.reg_eax.high(EMM_NO_ERROR);
                    break;
                default:
                    if (Log.level<=LogSeverities.LOG_ERROR) Log.log(LogTypes.LOG_MISC,LogSeverities.LOG_ERROR,"EMS:Call "+Integer.toString(CPU_Regs.reg_eax.high(), 16)+" Subfunction "+Integer.toString(CPU_Regs.reg_eax.low(), 16)+" not supported");
                    CPU_Regs.reg_eax.high(EMM_INVALID_SUB);
                    break;
                }
                break;
            case 0x4f:	/* Save/Restore Partial Page Map */
                CPU_Regs.reg_eax.high(EMM_PartialPageMapping());
                break;
            case 0x50:	/* Map/Unmap multiple handle pages */
                CPU_Regs.reg_eax.high(EMM_NO_ERROR);
                switch (CPU_Regs.reg_eax.low()) {
                    case 0x00: // use physical page numbers
                        {	/*PhysPt*/int data = CPU_Regs.reg_dsPhys.dword+CPU_Regs.reg_esi.word();
                            for (i=0; i<CPU_Regs.reg_ecx.word(); i++) {
                                /*Bit16u*/int logPage	= Memory.mem_readw(data); data+=2;
                                /*Bit16u*/int physPage = Memory.mem_readw(data); data+=2;
                                CPU_Regs.reg_eax.high(EMM_MapPage(physPage,CPU_Regs.reg_edx.word(),logPage));
                                if (CPU_Regs.reg_eax.high()!=EMM_NO_ERROR) break;
                            }
                        } break;
                    case 0x01: // use segment address
                        {	/*PhysPt*/int data = CPU_Regs.reg_dsPhys.dword+CPU_Regs.reg_esi.word();
                            for (i=0; i<CPU_Regs.reg_ecx.word(); i++) {
                                /*Bit16u*/int logPage	= Memory.mem_readw(data); data+=2;
                                CPU_Regs.reg_eax.high(EMM_MapSegment(Memory.mem_readw(data),CPU_Regs.reg_edx.word(),logPage)); data+=2;
                                if (CPU_Regs.reg_eax.high()!=EMM_NO_ERROR) break;
                            }
                        }
                        break;
                    default:
                        if (Log.level<=LogSeverities.LOG_ERROR) Log.log(LogTypes.LOG_MISC,LogSeverities.LOG_ERROR,"EMS:Call "+Integer.toString(CPU_Regs.reg_eax.high(), 16)+" Subfunction "+Integer.toString(CPU_Regs.reg_eax.low(), 16)+" not supported");
                        CPU_Regs.reg_eax.high(EMM_INVALID_SUB);
                        break;
                }
                break;
            case 0x51:	/* Reallocate Pages */
                CPU_Regs.reg_eax.high(EMM_ReallocatePages(CPU_Regs.reg_edx.word(),CPU_Regs.reg_ebx.word()));
                break;
            case 0x53: // Set/Get Handlename
                CPU_Regs.reg_eax.high(GetSetHandleName());
                break;
            case 0x54:	/* Handle Functions */
                CPU_Regs.reg_eax.high(HandleNameSearch());
                break;
            case 0x57:	/* Memory region */
                CPU_Regs.reg_eax.high(MemoryRegion());
                if (CPU_Regs.reg_eax.high()!=0) Log.log(LogTypes.LOG_MISC,LogSeverities.LOG_ERROR,"EMS:Function 57 move failed");
                break;
            case 0x58: // Get mappable physical array address array
                if (CPU_Regs.reg_eax.low()==0x00) {
                    /*PhysPt*/int data = CPU_Regs.reg_esPhys.dword+CPU_Regs.reg_edi.word();
                    /*Bit16u*/int step = 0x1000 / EMM_MAX_PHYS;
                    for (i=0; i<EMM_MAX_PHYS; i++) {
                        Memory.mem_writew(data,EMM_PAGEFRAME+step*i);	data+=2;
                        Memory.mem_writew(data,i);						data+=2;
                    }
                }
                // Set number of pages
                CPU_Regs.reg_ecx.word(EMM_MAX_PHYS);
                CPU_Regs.reg_eax.high(EMM_NO_ERROR);
                break;
            case 0x5A:              /* Allocate standard/raw Pages */
                if (CPU_Regs.reg_eax.low()<=0x01) {
                    IntRef dx = new IntRef(CPU_Regs.reg_edx.word());
                    CPU_Regs.reg_eax.high(EMM_AllocateMemory(CPU_Regs.reg_ebx.word(),dx,true));	// can allocate 0 pages
                    CPU_Regs.reg_edx.word(dx.value);
                } else {
                    if (Log.level<=LogSeverities.LOG_ERROR) Log.log(LogTypes.LOG_MISC,LogSeverities.LOG_ERROR,"EMS:Call 5A subfct "+Integer.toString(CPU_Regs.reg_eax.low(), 16)+" not supported");
                    CPU_Regs.reg_eax.high(EMM_INVALID_SUB);
                }
                break;
            case 0xDE:		/* VCPI Functions */
                if (!vcpi.enabled) {
                    if (Log.level<=LogSeverities.LOG_ERROR) Log.log(LogTypes.LOG_MISC,LogSeverities.LOG_ERROR,"EMS:VCPI Call "+Integer.toString(CPU_Regs.reg_eax.low(), 16)+" not supported");
                    CPU_Regs.reg_eax.high(EMM_FUNC_NOSUP);
                } else {
                    switch (CPU_Regs.reg_eax.low()) {
                    case 0x00:		/* VCPI Installation Check */
                        if (((CPU_Regs.reg_ecx.word()==0) && (CPU_Regs.reg_edi.word()==0x0012)) || (CPU.cpu.pmode && (CPU_Regs.flags & CPU_Regs.VM)!=0)) {
                            /* JEMM detected or already in v86 mode */
                            CPU_Regs.reg_eax.high(EMM_NO_ERROR);
                            CPU_Regs.reg_ebx.word(0x100);
                        } else {
                            CPU_Regs.reg_eax.high(EMM_FUNC_NOSUP);
                        }
                        break;
                    case 0x01: {	/* VCPI Get Protected Mode Interface */
                        /*Bit16u*/int ct;
                        /* Set up page table buffer */
                        for (ct=0; ct<0xff; ct++) {
                            Memory.real_writeb((int)CPU_Regs.reg_esVal.dword,CPU_Regs.reg_edi.word()+ct*4+0x00,0x67);		// access bits
                            Memory.real_writew((int)CPU_Regs.reg_esVal.dword,CPU_Regs.reg_edi.word()+ct*4+0x01,ct*0x10);		// mapping
                            Memory.real_writeb((int)CPU_Regs.reg_esVal.dword,CPU_Regs.reg_edi.word()+ct*4+0x03,0x00);
                        }
                        for (ct=0xff; ct<0x100; ct++) {
                            Memory.real_writeb((int)CPU_Regs.reg_esVal.dword,CPU_Regs.reg_edi.word()+ct*4+0x00,0x67);		// access bits
                            Memory.real_writew((int)CPU_Regs.reg_esVal.dword,CPU_Regs.reg_edi.word()+ct*4+0x01,(ct-0xff)*0x10+0x1100);	// mapping
                            Memory.real_writeb((int)CPU_Regs.reg_esVal.dword,CPU_Regs.reg_edi.word()+ct*4+0x03,0x00);
                        }
                        /* adjust paging entries for page frame (if mapped) */
                        for (ct=0; ct<4; ct++) {
                            /*Bit16u*/int handle=emm_mappings[ct].handle();
                            if (handle!=0xffff) {
                                /*Bit16u*/int memh=(/*Bit16u*/int)Memory.MEM_NextHandleAt(emm_handles[handle].mem,emm_mappings[ct].page()*4);
                                /*Bit16u*/int entry_addr=CPU_Regs.reg_edi.word()+(EMM_PAGEFRAME>>6)+(ct*0x10);
                                Memory.real_writew((int)CPU_Regs.reg_esVal.dword,entry_addr+0x00+0x01,(memh+0)*0x10);		// mapping of 1/4 of page
                                Memory.real_writew((int)CPU_Regs.reg_esVal.dword,entry_addr+0x04+0x01,(memh+1)*0x10);		// mapping of 2/4 of page
                                Memory.real_writew((int)CPU_Regs.reg_esVal.dword,entry_addr+0x08+0x01,(memh+2)*0x10);		// mapping of 3/4 of page
                                Memory.real_writew((int)CPU_Regs.reg_esVal.dword,entry_addr+0x0c+0x01,(memh+3)*0x10);		// mapping of 4/4 of page
                            }
                        }
                        CPU_Regs.reg_edi.word(CPU_Regs.reg_edi.word()+0x400);		// advance pointer by 0x100*4

                        /* Set up three descriptor table entries */
                        /*Bit32u*/int cbseg_low=(Callback.CALLBACK_GetBase()&0xffff)<<16;
                        /*Bit32u*/int cbseg_high=(Callback.CALLBACK_GetBase()&0x1f0000)>>16;
                        /* Descriptor 1 (code segment, callback segment) */
                        Memory.real_writed((int)CPU_Regs.reg_dsVal.dword,CPU_Regs.reg_esi.word()+0x00,0x0000ffff|cbseg_low);
                        Memory.real_writed((int)CPU_Regs.reg_dsVal.dword,CPU_Regs.reg_esi.word()+0x04,0x00009a00|cbseg_high);
                        /* Descriptor 2 (data segment, full access) */
                        Memory.real_writed((int)CPU_Regs.reg_dsVal.dword,CPU_Regs.reg_esi.word()+0x08,0x0000ffff);
                        Memory.real_writed((int)CPU_Regs.reg_dsVal.dword,CPU_Regs.reg_esi.word()+0x0c,0x00009200);
                        /* Descriptor 3 (full access) */
                        Memory.real_writed((int)CPU_Regs.reg_dsVal.dword,CPU_Regs.reg_esi.word()+0x10,0x0000ffff);
                        Memory.real_writed((int)CPU_Regs.reg_dsVal.dword,CPU_Regs.reg_esi.word()+0x14,0x00009200);

                        CPU_Regs.reg_ebx.dword=vcpi.pm_interface&0xffff;
                        CPU_Regs.reg_eax.high(EMM_NO_ERROR);
                        break;
                        }
                    case 0x02:		/* VCPI Maximum Physical Address */
                        CPU_Regs.reg_edx.dword=((Memory.MEM_TotalPages()*Memory.MEM_PAGESIZE)-1)&0xfffff000;
                        CPU_Regs.reg_eax.high(EMM_NO_ERROR);
                        break;
                    case 0x03:		/* VCPI Get Number of Free Pages */
                        CPU_Regs.reg_edx.dword=Memory.MEM_FreeTotal();
                        CPU_Regs.reg_eax.high(EMM_NO_ERROR);
                        break;
                    case 0x04: {	/* VCPI Allocate one Page */
                        /*MemHandle*/int mem = Memory.MEM_AllocatePages(1,false);
                        if (mem!=0) {
                            CPU_Regs.reg_edx.dword=mem<<12;
                            CPU_Regs.reg_eax.high(EMM_NO_ERROR);
                        } else {
                            CPU_Regs.reg_eax.high(EMM_OUT_OF_LOG);
                        }
                        break;
                        }
                    case 0x05:		/* VCPI Free Page */
                        Memory.MEM_ReleasePages(CPU_Regs.reg_edx.dword>>>12);
                        CPU_Regs.reg_eax.high(EMM_NO_ERROR);
                        break;
                    case 0x06: {	/* VCPI Get Physical Address of Page in 1st MB */
                        if (((CPU_Regs.reg_ecx.word()<<8)>=EMM_PAGEFRAME) && ((CPU_Regs.reg_ecx.word()<<8)<EMM_PAGEFRAME+0x1000)) {
                            /* Page is in Pageframe, so check what EMS-page it is
                               and return the physical address */
                            /*Bit8u*/short phys_page;
                            /*Bit16u*/int mem_seg=CPU_Regs.reg_ecx.word()<<8;
                            if (mem_seg<EMM_PAGEFRAME+0x400) phys_page=0;
                            else if (mem_seg<EMM_PAGEFRAME+0x800) phys_page=1;
                            else if (mem_seg<EMM_PAGEFRAME+0xc00) phys_page=2;
                            else phys_page=3;
                            /*Bit16u*/int handle=emm_mappings[phys_page].handle();
                            if (handle==0xffff) {
                                CPU_Regs.reg_eax.high(EMM_ILL_PHYS);
                                break;
                            } else {
                                /*MemHandle*/int memh=Memory.MEM_NextHandleAt(
                                    emm_handles[handle].mem,
                                    emm_mappings[phys_page].page()*4);
                                CPU_Regs.reg_edx.dword=(memh+(CPU_Regs.reg_ecx.word()&3))<<12;
                            }
                        } else {
                            /* Page not in Pageframe, so just translate into physical address */
                            CPU_Regs.reg_edx.dword=CPU_Regs.reg_ecx.word()<<12;
                        }

                        CPU_Regs.reg_eax.high(EMM_NO_ERROR);
                        }
                        break;
                    case 0x0a:		/* VCPI Get PIC Vector Mappings */
                        CPU_Regs.reg_ebx.word(vcpi.pic1_remapping);		// master PIC
                        CPU_Regs.reg_ecx.word(vcpi.pic2_remapping);		// slave PIC
                        CPU_Regs.reg_eax.high(EMM_NO_ERROR);
                        break;
                    case 0x0b:		/* VCPI Set PIC Vector Mappings */
                        CPU_Regs.flags&=(~CPU_Regs.IF);
                        vcpi.pic1_remapping=(short)(CPU_Regs.reg_ebx.word()&0xff);
                        vcpi.pic2_remapping=(short)(CPU_Regs.reg_ecx.word()&0xff);
                        CPU_Regs.reg_eax.high(EMM_NO_ERROR);
                        break;
                    case 0x0c: {	/* VCPI Switch from V86 to Protected Mode */
                        CPU_Regs.flags&=(~CPU_Regs.IF);
                        CPU.CPU_SetCPL(0);

                        /* Read data from ESI (linear address) */
                        /*Bit32u*/int new_cr3=Memory.mem_readd(CPU_Regs.reg_esi.dword);
                        /*Bit32u*/int new_gdt_addr=Memory.mem_readd(CPU_Regs.reg_esi.dword + 4);
                        /*Bit32u*/int new_idt_addr=Memory.mem_readd(CPU_Regs.reg_esi.dword + 8);
                        /*Bit16u*/int new_ldt=Memory.mem_readw(CPU_Regs.reg_esi.dword +0x0c);
                        /*Bit16u*/int new_tr=Memory.mem_readw(CPU_Regs.reg_esi.dword +0x0e);
                        /*Bit32u*/int new_eip=Memory.mem_readd(CPU_Regs.reg_esi.dword + 0x10);
                        /*Bit16u*/int new_cs=Memory.mem_readw(CPU_Regs.reg_esi.dword +0x14);

                        /* Get GDT and IDT entries */
                        /*Bit16u*/int new_gdt_limit=Memory.mem_readw(new_gdt_addr);
                        /*Bit32u*/int new_gdt_base=Memory.mem_readd(new_gdt_addr + 2);
                        /*Bit16u*/int new_idt_limit=Memory.mem_readw(new_idt_addr);
                        /*Bit32u*/int new_idt_base=Memory.mem_readd(new_idt_addr + 2);

                        /* Switch to protected mode, paging enabled if necessary */
                        /*Bit32u*/long new_cr0=CPU.CPU_GET_CRX(0)|1;
                        if (new_cr3!=0) new_cr0|=0x80000000l;
                        CPU.CPU_SET_CRX(0, (int)new_cr0);
                        CPU.CPU_SET_CRX(3, (int)new_cr3);

                        /*PhysPt*/int tbaddr=new_gdt_base+(new_tr&0xfff8)+5;
                        /*Bit8u*/int tb=Memory.mem_readb(tbaddr);
                        Memory.mem_writeb(tbaddr, tb&0xfd);

                        /* Load tables and initialize segment registers */
                        CPU.CPU_LGDT(new_gdt_limit, new_gdt_base);
                        CPU.CPU_LIDT(new_idt_limit, new_idt_base);
                        if (CPU.CPU_LLDT(new_ldt)) Log.log_msg("VCPI:Could not load LDT with "+Integer.toString(new_ldt,16));
                        if (CPU.CPU_LTR(new_tr)) Log.log_msg("VCPI:Could not load TR with "+Integer.toString(new_tr, 16));

                        CPU.CPU_SetSegGeneralDS(0);
                        CPU.CPU_SetSegGeneralES(0);
                        CPU.CPU_SetSegGeneralFS(0);
                        CPU.CPU_SetSegGeneralGS(0);

        //				MEM_A20_Enable(true);

                        /* Switch to protected mode */
                        CPU_Regs.flags&=(~(CPU_Regs.VM|CPU_Regs.NT));
                        CPU_Regs.flags|=0x3000;
                        CPU.CPU_JMP(true, new_cs, (int)new_eip, 0);
                        }
                        break;
                    default:
                        if (Log.level<=LogSeverities.LOG_ERROR) Log.log(LogTypes.LOG_MISC,LogSeverities.LOG_ERROR,"EMS:VCPI Call "+Integer.toString(CPU_Regs.reg_eax.word(), 16)+" not supported");
                        CPU_Regs.reg_eax.high(EMM_FUNC_NOSUP);
                        break;
                    }
                }
                break;
            default:
                if (Log.level<=LogSeverities.LOG_ERROR) Log.log(LogTypes.LOG_MISC,LogSeverities.LOG_ERROR,"EMS:Call "+Integer.toString(CPU_Regs.reg_eax.high(), 16)+" not supported");
                CPU_Regs.reg_eax.high(EMM_FUNC_NOSUP);
                break;
            }
            return Callback.CBRET_NONE;
        }
    };

    private static Callback.Handler VCPI_PM_Handler = new Callback.Handler() {
        public String getName() {
            return "EMS.VCPI_PM_Handler";
        }
        public /*Bitu*/int call() {
        //	LOG_MSG("VCPI PMODE handler, function %x",reg_ax);
            switch (CPU_Regs.reg_eax.word()) {
            case 0xDE03:		/* VCPI Get Number of Free Pages */
                CPU_Regs.reg_edx.dword=Memory.MEM_FreeTotal();
                CPU_Regs.reg_eax.high(EMM_NO_ERROR);
                break;
            case 0xDE04: {		/* VCPI Allocate one Page */
                /*MemHandle*/int mem = Memory.MEM_AllocatePages(1,false);
                if (mem!=0) {
                    CPU_Regs.reg_edx.dword=mem<<12;
                    CPU_Regs.reg_eax.high(EMM_NO_ERROR);
                } else {
                    CPU_Regs.reg_eax.high(EMM_OUT_OF_LOG);
                }
                break;
                }
            case 0xDE05:		/* VCPI Free Page */
                Memory.MEM_ReleasePages(CPU_Regs.reg_edx.dword>>>12);
                CPU_Regs.reg_eax.high(EMM_NO_ERROR);
                break;
            case 0xDE0C: {		/* VCPI Switch from Protected Mode to V86 */
                CPU_Regs.flags&=(~CPU_Regs.IF);

                /* Flags need to be filled in, VM=true, IOPL=3 */
                Memory.mem_writed(CPU_Regs.reg_ssPhys.dword + (CPU_Regs.reg_esp.dword & CPU.cpu.stack.mask)+0x10, 0x23002);

                /* Disable Paging */
                CPU.CPU_SET_CRX(0, CPU.CPU_GET_CRX(0)&0x7ffffff7);
                CPU.CPU_SET_CRX(3, 0);

                /*PhysPt*/int tbaddr=vcpi.private_area+0x0000+(0x10&0xfff8)+5;
                /*Bit8u*/int tb=Memory.mem_readb(tbaddr);
                Memory.mem_writeb(tbaddr, tb&0xfd);

                /* Load descriptor table registers */
                CPU.CPU_LGDT(0xff, vcpi.private_area+0x0000);
                CPU.CPU_LIDT(0x7ff, vcpi.private_area+0x2000);
                if (CPU.CPU_LLDT(0x08)) Log.log_msg("VCPI:Could not load LDT");
                if (CPU.CPU_LTR(0x10)) Log.log_msg("VCPI:Could not load TR");

                CPU_Regs.flags&=(~CPU_Regs.NT);
                CPU_Regs.reg_esp.dword+=8;		// skip interrupt return information
        //		MEM_A20_Enable(false);

                /* Switch to v86-task */
                CPU.CPU_IRET(true,0);
                }
                break;
            default:
                if (Log.level<=LogSeverities.LOG_WARN) Log.log(LogTypes.LOG_MISC,LogSeverities.LOG_WARN,"Unhandled VCPI-function "+Integer.toString(CPU_Regs.reg_eax.low(), 16)+" in protected mode");
                break;
            }
            return Callback.CBRET_NONE;
        }
    };

    private static Callback.Handler V86_Monitor = new Callback.Handler() {
        public String getName() {
            return "EMS.V86_Monitor";
        }
        public /*Bitu*/int call() {
            /* Calculate which interrupt did occur */
            /*Bitu*/int int_num=(Memory.mem_readw(CPU_Regs.reg_ssPhys.dword+(CPU_Regs.reg_esp.dword & CPU.cpu.stack.mask))-0x2803);

            /* See if Exception 0x0d and not Interrupt 0x0d */
            if ((int_num==(0x0d*4)) && ((CPU_Regs.reg_esp.word()&0xffff)!=0x1fda)) {
                /* Protection violation during V86-execution,
                   needs intervention by monitor (depends on faulting opcode) */

                CPU_Regs.reg_esp.dword+=6;		// skip ip of CALL and error code of EXCEPTION 0x0d

                /* Get address of faulting instruction */
                /*Bit16u*/int v86_cs=Memory.mem_readw(CPU_Regs.reg_ssPhys.dword+((CPU_Regs.reg_esp.dword +4) & CPU.cpu.stack.mask));
                /*Bit16u*/int v86_ip=Memory.mem_readw(CPU_Regs.reg_ssPhys.dword+((CPU_Regs.reg_esp.dword) & CPU.cpu.stack.mask));
                /*Bit8u*/int v86_opcode=Memory.mem_readb((v86_cs<<4)+v86_ip);
        //		LOG_MSG("v86 monitor caught protection violation at %x:%x, opcode=%x",v86_cs,v86_ip,v86_opcode);
                switch (v86_opcode) {
                    case 0x0f:		// double byte opcode
                        v86_opcode=Memory.mem_readb((v86_cs<<4)+v86_ip+1);
                        switch (v86_opcode) {
                            case 0x20: {	// mov reg,CRx
                                /*Bitu*/int rm_val=Memory.mem_readb((v86_cs<<4)+v86_ip+2);
                                /*Bitu*/int which=(rm_val >> 3) & 7;
                                if ((rm_val<0xc0) || (rm_val>=0xe8))
                                    Log.exit("Invalid opcode 0x0f 0x20 "+Integer.toString(rm_val, 16)+" caused a protection fault!");
                                /*Bit32u*/int crx=CPU.CPU_GET_CRX(which);
                                switch (rm_val&7) {
                                    case 0:	CPU_Regs.reg_eax.dword=crx;	break;
                                    case 1:	CPU_Regs.reg_ecx.dword=crx;	break;
                                    case 2:	CPU_Regs.reg_edx.dword=crx;	break;
                                    case 3:	CPU_Regs.reg_ebx.dword=crx;	break;
                                    case 4:	CPU_Regs.reg_esp.dword=crx;	break;
                                    case 5:	CPU_Regs.reg_ebp.dword=crx;	break;
                                    case 6:	CPU_Regs.reg_esi.dword=crx;	break;
                                    case 7:	CPU_Regs.reg_edi.dword=crx;	break;
                                }
                                Memory.mem_writew(CPU_Regs.reg_ssPhys.dword+((CPU_Regs.reg_esp.dword) & CPU.cpu.stack.mask),v86_ip+3);
                                }
                                break;
                            case 0x22: {	// mov CRx,reg
                                /*Bitu*/int rm_val=Memory.mem_readb((v86_cs<<4)+v86_ip+2);
                                /*Bitu*/int which=(rm_val >> 3) & 7;
                                if ((rm_val<0xc0) || (rm_val>=0xe8))
                                    Log.exit("Invalid opcode 0x0f 0x22 "+Integer.toString(rm_val, 16)+" caused a protection fault!");
                                /*Bit32u*/int crx=0;
                                switch (rm_val&7) {
                                    case 0:	crx= CPU_Regs.reg_eax.dword;	break;
                                    case 1:	crx= CPU_Regs.reg_ecx.dword;	break;
                                    case 2:	crx= CPU_Regs.reg_edx.dword;	break;
                                    case 3:	crx= CPU_Regs.reg_ebx.dword;	break;
                                    case 4:	crx= CPU_Regs.reg_esp.dword;	break;
                                    case 5:	crx= CPU_Regs.reg_ebp.dword;	break;
                                    case 6:	crx= CPU_Regs.reg_esi.dword;	break;
                                    case 7:	crx= CPU_Regs.reg_edi.dword;	break;
                                }
                                if (which==0) crx|=1;	// protection bit always on
                                CPU.CPU_SET_CRX(which,crx);
                                Memory.mem_writew(CPU_Regs.reg_ssPhys.dword+((CPU_Regs.reg_esp.dword) & CPU.cpu.stack.mask),v86_ip+3);
                                }
                                break;
                            default:
                                Log.exit("Unhandled opcode 0x0f "+Integer.toString(v86_opcode, 16)+" caused a protection fault!");
                        }
                        break;
                    case 0xe4:		// IN AL,Ib
                        CPU_Regs.reg_eax.low((/*Bit8u*/short)(IO.IO_ReadB(Memory.mem_readb((v86_cs<<4)+v86_ip+1))&0xff));
                        Memory.mem_writew(CPU_Regs.reg_ssPhys.dword+((CPU_Regs.reg_esp.dword) & CPU.cpu.stack.mask),v86_ip+2);
                        break;
                    case 0xe5:		// IN AX,Ib
                        CPU_Regs.reg_eax.word((/*Bit16u*/int)(IO.IO_ReadW(Memory.mem_readb((v86_cs<<4)+v86_ip+1))&0xffff));
                        Memory.mem_writew(CPU_Regs.reg_ssPhys.dword+((CPU_Regs.reg_esp.dword) & CPU.cpu.stack.mask),v86_ip+2);
                        break;
                    case 0xe6:		// OUT Ib,AL
                        IO.IO_WriteB(Memory.mem_readb((v86_cs<<4)+v86_ip+1),CPU_Regs.reg_eax.low());
                        Memory.mem_writew(CPU_Regs.reg_ssPhys.dword+((CPU_Regs.reg_esp.dword) & CPU.cpu.stack.mask),v86_ip+2);
                        break;
                    case 0xe7:		// OUT Ib,AX
                        IO.IO_WriteW(Memory.mem_readb((v86_cs<<4)+v86_ip+1),CPU_Regs.reg_eax.word());
                        Memory.mem_writew(CPU_Regs.reg_ssPhys.dword+((CPU_Regs.reg_esp.dword) & CPU.cpu.stack.mask),v86_ip+2);
                        break;
                    case 0xec:		// IN AL,DX
                        CPU_Regs.reg_eax.low((/*Bit8u*/short)(IO.IO_ReadB(CPU_Regs.reg_edx.word())&0xff));
                        Memory.mem_writew(CPU_Regs.reg_ssPhys.dword+((CPU_Regs.reg_esp.dword) & CPU.cpu.stack.mask),v86_ip+1);
                        break;
                    case 0xed:		// IN AX,DX
                        CPU_Regs.reg_eax.word((/*Bit16u*/int)(IO.IO_ReadW(CPU_Regs.reg_edx.word())&0xffff));
                        Memory.mem_writew(CPU_Regs.reg_ssPhys.dword+((CPU_Regs.reg_esp.dword) & CPU.cpu.stack.mask),v86_ip+1);
                        break;
                    case 0xee:		// OUT DX,AL
                        IO.IO_WriteB(CPU_Regs.reg_edx.word(),CPU_Regs.reg_eax.low());
                        Memory.mem_writew(CPU_Regs.reg_ssPhys.dword+((CPU_Regs.reg_esp.dword) & CPU.cpu.stack.mask),v86_ip+1);
                        break;
                    case 0xef:		// OUT DX,AX
                        IO.IO_WriteW(CPU_Regs.reg_edx.word(),CPU_Regs.reg_eax.word());
                        Memory.mem_writew(CPU_Regs.reg_ssPhys.dword+((CPU_Regs.reg_esp.dword) & CPU.cpu.stack.mask),v86_ip+1);
                        break;
                    case 0xf0:		// LOCK prefix
                        Memory.mem_writew(CPU_Regs.reg_ssPhys.dword+((CPU_Regs.reg_esp.dword) & CPU.cpu.stack.mask),v86_ip+1);
                        break;
                    case 0xf4:		// HLT
                        CPU_Regs.flags|=CPU_Regs.IF;
                        CPU.CPU_HLT(CPU_Regs.reg_eip);
                        Memory.mem_writew(CPU_Regs.reg_ssPhys.dword+((CPU_Regs.reg_esp.dword) & CPU.cpu.stack.mask),v86_ip+1);
                        break;
                    default:
                        Log.exit("Unhandled opcode "+Integer.toString(v86_opcode, 16)+" caused a protection fault!");
                }
                return Callback.CBRET_NONE;
            }

            /* Get address to interrupt handler */
            /*Bit16u*/int vint_vector_seg=Memory.mem_readw(CPU_Regs.reg_dsVal.dword+int_num+2);
            /*Bit16u*/int vint_vector_ofs=Memory.mem_readw(int_num);
            if (CPU_Regs.reg_esp.word()!=0x1fda) CPU_Regs.reg_esp.dword+=2+3*4;	// Interrupt from within protected mode
            else CPU_Regs.reg_esp.dword+=2;

            /* Read entries that were pushed onto the stack by the interrupt */
            /*Bit16u*/int return_ip=Memory.mem_readw(CPU_Regs.reg_ssPhys.dword+(CPU_Regs.reg_esp.dword & CPU.cpu.stack.mask));
            /*Bit16u*/int return_cs=Memory.mem_readw(CPU_Regs.reg_ssPhys.dword+((CPU_Regs.reg_esp.dword +4) & CPU.cpu.stack.mask));
            /*Bit32u*/int return_eflags=Memory.mem_readd(CPU_Regs.reg_ssPhys.dword + ((CPU_Regs.reg_esp.dword + 8) & CPU.cpu.stack.mask));

            /* Modify stack to call v86-interrupt handler */
            Memory.mem_writed(CPU_Regs.reg_ssPhys.dword+(CPU_Regs.reg_esp.dword & CPU.cpu.stack.mask),vint_vector_ofs);
            Memory.mem_writed(CPU_Regs.reg_ssPhys.dword+((CPU_Regs.reg_esp.dword +4) & CPU.cpu.stack.mask),vint_vector_seg);
            Memory.mem_writed(CPU_Regs.reg_ssPhys.dword+((CPU_Regs.reg_esp.dword +8) & CPU.cpu.stack.mask),return_eflags&(~(CPU_Regs.IF|CPU_Regs.TF)));

            /* Adjust SP of v86-stack */
            /*Bit16u*/int v86_ss=Memory.mem_readw(CPU_Regs.reg_ssPhys.dword+((CPU_Regs.reg_esp.dword +0x10) & CPU.cpu.stack.mask));
            /*Bit16u*/int v86_sp=(Memory.mem_readw(CPU_Regs.reg_ssPhys.dword+((CPU_Regs.reg_esp.dword +0x0c) & CPU.cpu.stack.mask))-6) & 0xFFFF;
            Memory.mem_writew(CPU_Regs.reg_ssPhys.dword+((CPU_Regs.reg_esp.dword +0x0c) & CPU.cpu.stack.mask),v86_sp);

            /* Return to original code after v86-interrupt handler */
            Memory.mem_writew((v86_ss<<4)+v86_sp+0,return_ip);
            Memory.mem_writew((v86_ss<<4)+v86_sp+2,return_cs);
            Memory.mem_writew((v86_ss<<4)+v86_sp+4,(/*Bit16u*/int)(return_eflags&0xffff));
            return Callback.CBRET_NONE;
        }
    };

    private static void SetupVCPI() {
        vcpi.enabled=false;

        vcpi.ems_handle=0;	// use EMM system handle for VCPI data

        vcpi.enabled=true;

        vcpi.pic1_remapping=0x08;	// master PIC base
        vcpi.pic2_remapping=0x70;	// slave PIC base

        vcpi.private_area=emm_handles[vcpi.ems_handle].mem<<12;

        /* GDT */
        Memory.mem_writed(vcpi.private_area+0x0000,0x00000000);	// descriptor 0
        Memory.mem_writed(vcpi.private_area+0x0004,0x00000000);	// descriptor 0

        /*Bit32u*/int ldt_address=(vcpi.private_area+0x1000);
        /*Bit16u*/int ldt_limit=0xff;
        /*Bit32u*/int ldt_desc_part=((ldt_address&0xffff)<<16)|ldt_limit;
        Memory.mem_writed(vcpi.private_area+0x0008,ldt_desc_part);	// descriptor 1 (LDT)
        ldt_desc_part=((ldt_address&0xff0000)>>16)|(ldt_address&0xff000000)|0x8200;
        Memory.mem_writed(vcpi.private_area+0x000c,ldt_desc_part);	// descriptor 1

        /*Bit32u*/int tss_address=(vcpi.private_area+0x3000);
        /*Bit32u*/int tss_desc_part=((tss_address&0xffff)<<16)|(0x0068+0x200);
        Memory.mem_writed(vcpi.private_area+0x0010,tss_desc_part);	// descriptor 2 (TSS)
        tss_desc_part=((tss_address&0xff0000)>>16)|(tss_address&0xff000000)|0x8900;
        Memory.mem_writed(vcpi.private_area+0x0014,tss_desc_part);	// descriptor 2

        /* LDT */
        Memory.mem_writed(vcpi.private_area+0x1000,0x00000000);	// descriptor 0
        Memory.mem_writed(vcpi.private_area+0x1004,0x00000000);	// descriptor 0
        /*Bit32u*/int cs_desc_part=((vcpi.private_area&0xffff)<<16)|0xffff;
        Memory.mem_writed(vcpi.private_area+0x1008,cs_desc_part);	// descriptor 1 (code)
        cs_desc_part=((vcpi.private_area&0xff0000)>>16)|(vcpi.private_area&0xff000000)|0x9a00;
        Memory.mem_writed(vcpi.private_area+0x100c,cs_desc_part);	// descriptor 1
        /*Bit32u*/int ds_desc_part=((vcpi.private_area&0xffff)<<16)|0xffff;
        Memory.mem_writed(vcpi.private_area+0x1010,ds_desc_part);	// descriptor 2 (data)
        ds_desc_part=((vcpi.private_area&0xff0000)>>16)|(vcpi.private_area&0xff000000)|0x9200;
        Memory.mem_writed(vcpi.private_area+0x1014,ds_desc_part);	// descriptor 2

        /* IDT setup */
        for (/*Bit16u*/int int_ct=0; int_ct<0x100; int_ct++) {
            /* build a CALL NEAR V86MON, the value of IP pushed by the
                CALL is used to identify the interrupt number */
            Memory.mem_writeb(vcpi.private_area+0x2800+int_ct*4+0,0xe8);	// call
            Memory.mem_writew(vcpi.private_area+0x2800+int_ct*4+1,0x05fd-(int_ct*4));
            Memory.mem_writeb(vcpi.private_area+0x2800+int_ct*4+3,0xcf);	// iret (dummy)

            /* put a Gate-Descriptor into the IDT */
            Memory.mem_writed(vcpi.private_area+0x2000+int_ct*8+0,0x000c0000|(0x2800+int_ct*4));
            Memory.mem_writed(vcpi.private_area+0x2000+int_ct*8+4,0x0000ee00);
        }

        /* TSS */
        for (/*Bitu*/int tse_ct=0; tse_ct<0x68+0x200; tse_ct++) {
            /* clear the TSS as most entries are not used here */
            Memory.mem_writeb(vcpi.private_area+0x3000,0);
        }
        /* Set up the ring0-stack */
        Memory.mem_writed(vcpi.private_area+0x3004,0x00002000);	// esp
        Memory.mem_writed(vcpi.private_area+0x3008,0x00000014);	// ss

        Memory.mem_writed(vcpi.private_area+0x3066,0x0068);		// io-map base (map follows, all zero)
    }

    private static Callback.Handler INT4B_Handler = new Callback.Handler() {
        public String getName() {
            return "EMS.INT4B_Handler";
        }
        public /*Bitu*/int call() {
            switch (CPU_Regs.reg_eax.high()) {
            case 0x81:
                Callback.CALLBACK_SCF(true);
                CPU_Regs.reg_eax.word(0x1);
                break;
            default:
                if (Log.level<=LogSeverities.LOG_WARN) Log.log(LogTypes.LOG_MISC,LogSeverities.LOG_WARN,"Unhandled interrupt 4B function "+Integer.toString(CPU_Regs.reg_eax.high(),16));
                break;
            }
            return Callback.CBRET_NONE;
        }
    };

    private DOS_Device emm_device;
    /* location in protected unfreeable memory where the ems name and callback are
     * stored  32 bytes.*/
    private static /*Bit16u*/int ems_baseseg;
    private /*RealPt*/int old4b_pointer,old67_pointer;
    private Callback call_vdma=new Callback(),call_vcpi=new Callback(),call_v86mon=new Callback();
    /*Bitu*/int call_int67;

    static /*Bitu*/int GetEMSType(Section_prop section) {
        /*Bitu*/int rtype = 0;
        String emstypestr = section.Get_string("ems");
        if (emstypestr.equalsIgnoreCase("true")) {
            rtype = 1;	// mixed mode
        } else if (emstypestr.equalsIgnoreCase("emsboard")) {
            rtype = 2;
        } else if (emstypestr.equalsIgnoreCase("emm386")) {
            rtype = 3;
        } else {
            rtype = 0;
        }
        return rtype;
    }

    EMS(Section configuration) {
        super(configuration);
        emm_device=null;
		ems_type=0;

        /* Virtual DMA interrupt callback */
        call_vdma.Install(INT4B_Handler,Callback.CB_IRET,"Int 4b vdma");
        call_vdma.Set_RealVec(0x4b);

        vcpi.enabled=false;
        GEMMIS_seg=0;

        Section_prop section=(Section_prop)configuration;
        ems_type=GetEMSType(section);
		if (ems_type<=0) return;
        if (Dosbox.machine== MachineType.MCH_PCJR) {
            ems_type=0;
            Log.log_msg("EMS disabled for PCJr machine");
            return;
        }
        Bios.BIOS_ZeroExtendedSize(true);

        if (ems_baseseg==0) ems_baseseg=Dos_tables.DOS_GetMemory(2);	//We have 32 bytes

        /* Add a little hack so it appears that there is an actual ems device installed */
        String emsname="EMMXXXX0";
        Memory.MEM_BlockWrite(Memory.PhysMake(ems_baseseg,0xa),emsname,emsname.length()+1);

        call_int67=Callback.CALLBACK_Allocate();
        Callback.CALLBACK_Setup(call_int67,INT67_Handler,Callback.CB_IRET,Memory.PhysMake(ems_baseseg,4),"Int 67 ems");
        IntRef o = new IntRef(old67_pointer);
        Memory.RealSetVec(0x67,Memory.RealMake(ems_baseseg,4),o);
        old67_pointer = o.value;

        /* Register the ems device */
        //TODO MAYBE put it in the class.
        emm_device = new device_EMM(ems_type!=2);
		Dos_devices.DOS_AddDevice(emm_device);

        /* Clear handle and page tables */
        /*Bitu*/int i;
        for (i=0;i<EMM_MAX_HANDLES;i++) {
            emm_handles[i].mem=0;
            emm_handles[i].pages=NULL_HANDLE;
            emm_handles[i].name="";
        }
        for (i=0;i<EMM_MAX_PHYS;i++) {
            emm_mappings[i].page(NULL_PAGE);
            emm_mappings[i].handle(NULL_HANDLE);
        }
        for (i=0;i<0x40;i++) {
            emm_segmentmappings[i].page(NULL_PAGE);
            emm_segmentmappings[i].handle(NULL_HANDLE);
        }

        EMM_AllocateSystemHandle(8);	// allocate OS-dedicated handle (ems handle zero, 128kb)

        if (ems_type==3) {
            DMA.DMA_SetWrapping(0xffffffff);	// emm386-bug that disables dma wrapping
		}

        if (ENABLE_VCPI==0) return;

        if (ems_type!=2) {
            /* Install a callback that handles VCPI-requests in protected mode requests */
            call_vcpi.Install(VCPI_PM_Handler,Callback.CB_IRETD,"VCPI PM");
            vcpi.pm_interface=(call_vcpi.Get_callback())*Callback.CB_SIZE;

            /* Initialize private data area and set up descriptor tables */
            SetupVCPI();

            if (!vcpi.enabled) return;

            /* Install v86-callback that handles interrupts occuring
               in v86 mode, including protection fault exceptions */
            call_v86mon.Install(V86_Monitor,Callback.CB_IRET,"V86 Monitor");

            Memory.mem_writeb(vcpi.private_area+0x2e00,(/*Bit8u*/short)0xFE);       //GRP 4
            Memory.mem_writeb(vcpi.private_area+0x2e01,(/*Bit8u*/short)0x38);       //Extra Callback instruction
            Memory.mem_writew(vcpi.private_area+0x2e02,call_v86mon.Get_callback());		//The immediate word
            Memory.mem_writeb(vcpi.private_area+0x2e04,(/*Bit8u*/short)0x66);
            Memory.mem_writeb(vcpi.private_area+0x2e05,(/*Bit8u*/short)0xCF);       //A IRETD Instruction

            /* Testcode only, starts up dosbox in v86-mode */
            if (ENABLE_V86_STARTUP!=0) {
                /* Prepare V86-task */
                CPU.CPU_SET_CRX(0, 1);
                CPU.CPU_LGDT(0xff, vcpi.private_area+0x0000);
                CPU.CPU_LIDT(0x7ff, vcpi.private_area+0x2000);
                if (CPU.CPU_LLDT(0x08)) Log.log_msg("VCPI:Could not load LDT");
                if (CPU.CPU_LTR(0x10)) Log.log_msg("VCPI:Could not load TR");

                CPU.CPU_Push32(CPU_Regs.reg_gsVal.dword);
                CPU.CPU_Push32(CPU_Regs.reg_fsVal.dword);
                CPU.CPU_Push32(CPU_Regs.reg_dsVal.dword);
                CPU.CPU_Push32(CPU_Regs.reg_esVal.dword);
                CPU.CPU_Push32(CPU_Regs.reg_ssVal.dword);
                CPU.CPU_Push32(0x23002);
                CPU.CPU_Push32(CPU_Regs.reg_csVal.dword);
                CPU.CPU_Push32(CPU_Regs.reg_eip&0xffff);
                /* Switch to V86-mode */
                CPU.CPU_SetCPL(0);
                CPU.CPU_IRET(true,0);
            }
        }
    }

    void close() {
        if (ems_type<=0) return;

        /* Undo Biosclearing */
        Bios.BIOS_ZeroExtendedSize(false);

        /* Remove ems device */
        if (emm_device!=null) {
			Dos_devices.DOS_DelDevice(emm_device);
			emm_device=null;
		}
        GEMMIS_seg=0;

        /* Remove the emsname and callback hack */
        byte[] buf=new byte[32];
        Memory.MEM_BlockWrite(Memory.PhysMake(ems_baseseg,0),buf,32);
        Memory.RealSetVec(0x67,old67_pointer);

        /* Release memory allocated to system handle */
        if (emm_handles[EMM_SYSTEM_HANDLE].pages != NULL_HANDLE) {
            Memory.MEM_ReleasePages(emm_handles[EMM_SYSTEM_HANDLE].mem);
        }

        /* Clear handle and page tables */
        //TODO

        if ((ENABLE_VCPI==0) || (!vcpi.enabled)) return;

        if (CPU.cpu.pmode && CPU_Regs.GETFLAG(CPU_Regs.VM)!=0) {
            /* Switch back to real mode if in v86-mode */
            CPU.CPU_SET_CRX(0, 0);
            CPU.CPU_SET_CRX(3, 0);
            CPU_Regs.flags&=(~(CPU_Regs.IOPL|CPU_Regs.VM));
            CPU.CPU_LIDT(0x3ff, 0);
            CPU.CPU_SetCPL(0);
        }
    }

    static EMS test;

    public static Section.SectionFunction EMS_ShutDown = new Section.SectionFunction() {
        public void call(Section section) {
            test.close();
            test = null;
            for (int i=0;i<emm_handles.length;i++)
                emm_handles[i] = null;
            for (int i=0;i<emm_mappings.length;i++)
                emm_mappings[i] = null;
            for (int i=0;i<emm_segmentmappings.length;i++)
                emm_segmentmappings[i] = null;
        }
    };

    public static Section.SectionFunction EMS_Init = new Section.SectionFunction() {
        public void call(Section section) {
            for (int i=0;i<emm_handles.length;i++)
                emm_handles[i] = new EMM_Handle();
            for (int i=0;i<emm_mappings.length;i++)
                emm_mappings[i] = new EMM_Mapping();
            for (int i=0;i<emm_segmentmappings.length;i++)
                emm_segmentmappings[i] = new EMM_Mapping();
            test = new EMS(section);
            section.AddDestroyFunction(EMS_ShutDown,true);
        }
    };
}
