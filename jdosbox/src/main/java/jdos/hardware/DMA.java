package jdos.hardware;

import jdos.Dosbox;
import jdos.cpu.Paging;
import jdos.misc.Log;
import jdos.misc.setup.Module_base;
import jdos.misc.setup.Section;
import jdos.types.LogSeverities;
import jdos.types.LogTypes;

public class DMA extends Module_base {
    static public final class DMAEvent {
        public static final int DMA_REACHED_TC=0;
        public static final int DMA_MASKED=1;
        public static final int DMA_UNMASKED=2;
        public static final int DMA_TRANSFEREND=3;
    }

    static public interface DMA_CallBack {
        public void call(DmaChannel chan, int event);
    }

    static public class DmaChannel {
        public /*Bit32u*/int pagebase;
        public /*Bit16u*/int baseaddr;
        public /*Bit32u*/int curraddr;
        public /*Bit16u*/int basecnt;
        public /*Bit16u*/int currcnt;
        public /*Bit8u*/short channum;
        public /*Bit8u*/short pagenum;
        public /*Bit8u*/short DMA16;
        public int mode;
        public boolean increment;
        public boolean autoinit;
        public /*Bit8u*/short trantype;
        public boolean masked;
        public boolean tcount;
        public boolean request;
        public DMA_CallBack callback;

        public DmaChannel(/*Bit8u*/int num, boolean dma16) {
            masked = true;
            callback = null;
            if(num == 4) return;
            channum = (short)num;
            DMA16 = (short)(dma16 ? 0x1 : 0x0);
            pagenum = 0;
            pagebase = 0;
            baseaddr = 0;
            curraddr = 0;
            basecnt = 0;
            currcnt = 0;
            increment = true;
            autoinit = false;
            tcount = false;
            request = false;
        }
        public void DoCallBack(int event) {
            if (callback!=null) callback.call(this,event);
        }
        public void SetMask(boolean _mask) {
            masked=_mask;
            DoCallBack(masked ? DMAEvent.DMA_MASKED : DMAEvent.DMA_UNMASKED);
        }
        public void Register_Callback(DMA_CallBack _cb) {
            callback = _cb;
            SetMask(masked);
            if (callback!=null) Raise_Request();
            else Clear_Request();
        }
        public void ReachedTC() {
            tcount=true;
            DoCallBack(DMAEvent.DMA_REACHED_TC);
        }
        public void SetPage(/*Bit8u*/short val) {
            pagenum=val;
            pagebase=(pagenum >> DMA16) << (16+DMA16);
        }
        public void Raise_Request() {
            request=true;
        }
        public void Clear_Request() {
            request=false;
        }
        public /*Bitu*/int Read(/*Bitu*/int want, /*Bit8u*/byte[] buffer, int bufferOffset) {
            /*Bitu*/int done=0;
            curraddr &= dma_wrapping;
            while (true) {
                /*Bitu*/int left=(currcnt+1);
                if (want<left) {
                    DMA_BlockRead(pagebase,curraddr,buffer,bufferOffset,want);
                    done+=want;
                    curraddr+=want;
                    currcnt-=want;
                } else {
                    DMA_BlockRead(pagebase,curraddr,buffer,bufferOffset,want);
                    bufferOffset+=left;
                    want-=left;
                    done+=left;
                    ReachedTC();
                    if (autoinit) {
                        currcnt=basecnt;
                        curraddr=baseaddr;
                        if (want!=0) continue;
                        UpdateEMSMapping();
                    } else {
                        curraddr+=left;
                        currcnt=0xffff;
                        masked=true;
                        UpdateEMSMapping();
                        DoCallBack(DMAEvent.DMA_TRANSFEREND);
                    }
                }
                break;
            }
            return done;
        }
        public /*Bitu*/int Read(/*Bitu*/int want, /*Bit8u*/short[] buffer, int bufferOffset) {
            /*Bitu*/int done=0;
            curraddr &= dma_wrapping;
            while (true) {
                /*Bitu*/int left=(currcnt+1);
                if (want<left) {
                    DMA_BlockRead(pagebase,curraddr,buffer,bufferOffset,want);
                    done+=want;
                    curraddr+=want;
                    currcnt-=want;
                } else {
                    DMA_BlockRead(pagebase,curraddr,buffer,bufferOffset,want);
                    bufferOffset+=left;
                    want-=left;
                    done+=left;
                    ReachedTC();
                    if (autoinit) {
                        currcnt=basecnt;
                        curraddr=baseaddr;
                        if (want!=0) continue;
                        UpdateEMSMapping();
                    } else {
                        curraddr+=left;
                        currcnt=0xffff;
                        masked=true;
                        UpdateEMSMapping();
                        DoCallBack(DMAEvent.DMA_TRANSFEREND);
                    }
                }
                break;
            }
            return done;
        }
        public /*Bitu*/int Write(/*Bitu*/int want, /*Bit8u*/byte[] buffer, int offset) {
            /*Bitu*/int done=0;
            curraddr &= dma_wrapping;
            while (true) {
                /*Bitu*/int left=(currcnt+1);
                if (want<left) {
                    DMA_BlockWrite(pagebase, curraddr, buffer, offset, want, DMA16);
                    done+=want;
                    curraddr+=want;
                    currcnt-=want;
                } else {
                    DMA_BlockWrite(pagebase,curraddr,buffer,offset, left,DMA16);
                    offset+=(left << DMA16);
                    want-=left;
                    done+=left;
                    ReachedTC();
                    if (autoinit) {
                        currcnt=basecnt;
                        curraddr=baseaddr;
                        if (want!=0) continue;
                        UpdateEMSMapping();
                    } else {
                        curraddr+=left;
                        currcnt=0xffff;
                        masked=true;
                        UpdateEMSMapping();
                        DoCallBack(DMAEvent.DMA_TRANSFEREND);
                    }
                }
                break;
            }
            return done;
        }
    }

    public static class DmaController {
        private /*Bit8u*/short ctrlnum;
        private boolean flipflop;
        private DmaChannel[] DmaChannels=new DmaChannel[4];

        public IoHandler.IO_ReadHandleObject[] DMA_ReadHandler=new IoHandler.IO_ReadHandleObject[0x11];
        public IoHandler.IO_WriteHandleObject[] DMA_WriteHandler=new IoHandler.IO_WriteHandleObject[0x11];
        public DmaController(/*Bit8u*/int num) {
            flipflop = false;
            ctrlnum = (short)num;		/* first or second DMA controller */
            for(/*Bit8u*/short i=0;i<4;i++) {
                DmaChannels[i] = new DmaChannel(i+ctrlnum*4,ctrlnum==1);
            }
            for (int i=0;i<DMA_ReadHandler.length;i++)
                DMA_ReadHandler[i] = new IoHandler.IO_ReadHandleObject();
            for (int i=0;i<DMA_WriteHandler.length;i++)
                DMA_WriteHandler[i] = new IoHandler.IO_WriteHandleObject();
        }
        public DmaChannel GetChannel(/*Bit8u*/int chan) {
            if (chan<4) return DmaChannels[chan];
            else return null;
        }
        public void WriteControllerReg(/*Bitu*/int reg,/*Bitu*/int val,/*Bitu*/int len) {
            DmaChannel chan;
            switch (reg) {
            /* set base address of DMA transfer (1st byte low part, 2nd byte high part) */
            case 0x0:case 0x2:case 0x4:case 0x6:
                UpdateEMSMapping();
                chan=GetChannel((/*Bit8u*/short)(reg >> 1));
                flipflop=!flipflop;
                if (flipflop) {
                    chan.baseaddr=(chan.baseaddr&0xff00)|val;
                    chan.curraddr=(chan.curraddr&0xff00)|val;
                } else {
                    chan.baseaddr=(chan.baseaddr&0x00ff)|(val << 8);
                    chan.curraddr=(chan.curraddr&0x00ff)|(val << 8);
                }
                break;
            /* set DMA transfer count (1st byte low part, 2nd byte high part) */
            case 0x1:case 0x3:case 0x5:case 0x7:
                UpdateEMSMapping();
                chan=GetChannel((/*Bit8u*/short)(reg >> 1));
                flipflop=!flipflop;
                if (flipflop) {
                    chan.basecnt=(chan.basecnt&0xff00)|val;
                    chan.currcnt=(chan.currcnt&0xff00)|val;
                } else {
                    chan.basecnt=(chan.basecnt&0x00ff)|(val << 8);
                    chan.currcnt=(chan.currcnt&0x00ff)|(val << 8);
                }
                break;
            case 0x8:		/* Comand reg not used */
                break;
            case 0x9:		/* Request registers, memory to memory */
                //TODO Warning?
                break;
            case 0xa:		/* Mask Register */
                if ((val & 0x4)==0) UpdateEMSMapping();
                chan=GetChannel((short)(val & 3));
                chan.SetMask((val & 0x4)>0);
                break;
            case 0xb:		/* Mode Register */
                UpdateEMSMapping();
                chan=GetChannel((short)(val & 3));
                chan.autoinit=(val & 0x10) > 0;
                chan.increment=(val & 0x20) > 0;
                chan.mode = val;
                //TODO Maybe other bits?
                break;
            case 0xc:		/* Clear Flip/Flip */
                flipflop=false;
                break;
            case 0xd:		/* Master Clear/Reset */
                for (/*Bit8u*/short ct=0;ct<4;ct++) {
                    chan=GetChannel(ct);
                    chan.SetMask(true);
                    chan.tcount=false;
                }
                flipflop=false;
                break;
            case 0xe:		/* Clear Mask register */
                UpdateEMSMapping();
                for (/*Bit8u*/short ct=0;ct<4;ct++) {
                    chan=GetChannel(ct);
                    chan.SetMask(false);
                }
                break;
            case 0xf:		/* Multiple Mask register */
                UpdateEMSMapping();
                for (/*Bit8u*/short ct=0;ct<4;ct++) {
                    chan=GetChannel(ct);
                    chan.SetMask((val & 1)!=0?true:false);
                    val>>=1;
                }
                break;
            }
        }
        public /*Bitu*/int ReadControllerReg(/*Bitu*/int reg,/*Bitu*/int len) {
            DmaChannel chan;/*Bitu*/int ret;
            switch (reg) {
            /* read base address of DMA transfer (1st byte low part, 2nd byte high part) */
            case 0x0:case 0x2:case 0x4:case 0x6:
                chan=GetChannel((/*Bit8u*/short)(reg >> 1));
                flipflop=!flipflop;
                if (flipflop) {
                    return (int)(chan.curraddr & 0xff);
                } else {
                    return (int)((chan.curraddr >> 8) & 0xff);
                }
            /* read DMA transfer count (1st byte low part, 2nd byte high part) */
            case 0x1:case 0x3:case 0x5:case 0x7:
                chan=GetChannel((/*Bit8u*/short)(reg >> 1));
                flipflop=!flipflop;
                if (flipflop) {
                    return chan.currcnt & 0xff;
                } else {
                    return (chan.currcnt >> 8) & 0xff;
                }
            case 0x8:		/* Status Register */
                ret=0;
                for (/*Bit8u*/short ct=0;ct<4;ct++) {
                    chan=GetChannel(ct);
                    if (chan.tcount) ret|=1 << ct;
                    chan.tcount=false;
                    if (chan.request) ret|=1 << (4+ct);
                }
                return ret;
            default:
                if (Log.level<=LogSeverities.LOG_NORMAL) Log.log(LogTypes.LOG_DMACONTROL, LogSeverities.LOG_NORMAL,"Trying to read undefined DMA port "+Integer.toString(reg,16));
                break;
            }
            return 0xffffffff;
        }
    }

    static private /*Bit32u*/long dma_wrapping = 0xffff;

    static private DmaController[] DmaControllers=new DmaController[2];

    static final private int EMM_PAGEFRAME4K = ((0xE000*16)/4096);

    static private /*Bit32u*/long[] ems_board_mapping=new long[Paging.LINK_START];

    static private void UpdateEMSMapping() {
        /* if EMS is not present, this will result in a 1:1 mapping */
        /*Bitu*/int i;
        for (i=0;i<0x10;i++) {
            ems_board_mapping[EMM_PAGEFRAME4K+i]=Paging.firstmb[EMM_PAGEFRAME4K+i];
        }
    }

    /* read a block from physical memory */
    static private void DMA_BlockRead(/*PhysPt*/int spage,/*PhysPt*/int offset,short[] data, int dataOffset, /*Bitu*/int size) {
        /*Bitu*/int highpart_addr_page = (int)(spage>>12);
        int dma16=1;
        size <<= dma16;
        offset <<= dma16;
        /*Bit32u*/long dma_wrap = ((0xffff<<dma16)+dma16) | dma_wrapping;
        boolean left = true;
        for ( ; size!=0 ; size--, offset++) {
            if (offset>(dma_wrapping<<dma16)) {
			    Log.log_msg("DMA segbound wrapping (read): "+Long.toString(spage, 16)+":"+Integer.toString(offset,16)+" size "+Integer.toString(size, 16)+" [1] wrap "+Long.toString(dma_wrapping,16));
		    }
            offset &= dma_wrap;
            /*Bitu*/int page = highpart_addr_page+(offset >>> 12);
            /* care for EMS pageframe etc. */
            if (page < EMM_PAGEFRAME4K) page = (int)Paging.firstmb[page];
            else if (page < EMM_PAGEFRAME4K+0x10) page = (int)ems_board_mapping[page];
            else if (page < Paging.LINK_START) page = (int)Paging.firstmb[page];
            if (left) {
                data[dataOffset] = (short)Memory.phys_readb(page*4096 + (offset & 4095));
            } else {
                data[dataOffset++] |= (Memory.phys_readb(page*4096 + (offset & 4095)) << 8);
            }
            left=!left;
        }
    }

    static private void DMA_BlockRead(/*PhysPt*/int spage,/*PhysPt*/int offset,byte[] data, int dataOffset, /*Bitu*/int size) {
        /*Bitu*/int highpart_addr_page = spage >>> 12;
        /*Bit32u*/long dma_wrap = 0xffff;
        for ( ; size!=0 ; size--, offset++) {
            if (offset>(dma_wrapping)) {
			    Log.log_msg("DMA segbound wrapping (read): "+Long.toString(spage, 16)+":"+Integer.toString(offset,16)+" size "+Integer.toString(size, 16)+" [0] wrap "+Long.toString(dma_wrapping,16));
		    }
            offset &= dma_wrap;
            /*Bitu*/int page = highpart_addr_page+(offset >>> 12);
            /* care for EMS pageframe etc. */
            if (page < EMM_PAGEFRAME4K) page = (int)Paging.firstmb[page];
            else if (page < EMM_PAGEFRAME4K+0x10) page = (int)ems_board_mapping[page];
            else if (page < Paging.LINK_START) page = (int)Paging.firstmb[page];
            data[dataOffset++]=RAM.readbs(page*4096 + (offset & 4095));
        }
    }

    /* write a block into physical memory */
    static void DMA_BlockWrite(/*PhysPt*/int spage,/*PhysPt*/int offset,byte[] data, int data_offset, /*Bitu*/int size,/*Bit8u*/short dma16) {
        /*Bitu*/int highpart_addr_page = (int)(spage>>12);
        size <<= dma16;
        offset <<= dma16;
        /*Bit32u*/long dma_wrap = ((0xffff<<dma16)+dma16) | dma_wrapping;
        for (int i=0; size!=0 ; size--, offset++, i++) {
            if (offset>(dma_wrapping<<dma16)) {
			    Log.log_msg("DMA segbound wrapping (write): "+Long.toString(spage, 16)+":"+Long.toString(offset,16)+" size "+Integer.toString(size, 16)+" ["+dma16+"] wrap "+Long.toString(dma_wrapping,16));
		    }
            offset &= dma_wrap;
            /*Bitu*/int page = highpart_addr_page+(offset >>> 12);
            /* care for EMS pageframe etc. */
            if (page < EMM_PAGEFRAME4K) page = (int)Paging.firstmb[page];
            else if (page < EMM_PAGEFRAME4K+0x10) page = (int)ems_board_mapping[page];
            else if (page < Paging.LINK_START) page = (int)Paging.firstmb[page];
            Memory.phys_writeb(page*4096 + (offset & 4095), data[data_offset+i] & 0xFF);
        }
    }

    public static DmaChannel GetDMAChannel(/*Bit8u*/int chan) {
        if (chan<4) {
            /* channel on first DMA controller */
            if (DmaControllers[0]!=null) return DmaControllers[0].GetChannel(chan);
        } else if (chan<8) {
            /* channel on second DMA controller */
            if (DmaControllers[1]!=null) return DmaControllers[1].GetChannel(chan-4);
        }
        return null;
    }

    /* remove the second DMA controller (ports are removed automatically) */
    public static void CloseSecondDMAController() {
        if (DmaControllers[1]!=null) {
            DmaControllers[1]=null;
        }
    }

    /* check availability of second DMA controller, needed for SB16 */
    public static boolean SecondDMAControllerAvailable() {
        if (DmaControllers[1]!=null) return true;
        else return false;
    }

    private static IoHandler.IO_WriteHandler DMA_Write_Port = new IoHandler.IO_WriteHandler() {
        public void call(/*Bitu*/int port, /*Bitu*/int val, /*Bitu*/int iolen) {
            if (port<0x10) {
                /* write to the first DMA controller (channels 0-3) */
                DmaControllers[0].WriteControllerReg(port,val,1);
            } else if (port>=0xc0 && port <=0xdf) {
                /* write to the second DMA controller (channels 4-7) */
                DmaControllers[1].WriteControllerReg((port-0xc0) >> 1,val,1);
            } else {
                UpdateEMSMapping();
                switch (port) {
                    /* write DMA page register */
                    case 0x81:GetDMAChannel(2).SetPage((/*Bit8u*/short)val);break;
                    case 0x82:GetDMAChannel(3).SetPage((/*Bit8u*/short)val);break;
                    case 0x83:GetDMAChannel(1).SetPage((/*Bit8u*/short)val);break;
                    case 0x89:GetDMAChannel(6).SetPage((/*Bit8u*/short)val);break;
                    case 0x8a:GetDMAChannel(7).SetPage((/*Bit8u*/short)val);break;
                    case 0x8b:GetDMAChannel(5).SetPage((/*Bit8u*/short)val);break;
                }
            }
        }
    };

    static private IoHandler.IO_ReadHandler DMA_Read_Port = new IoHandler.IO_ReadHandler() {
        public /*Bitu*/int call(/*Bitu*/int port, /*Bitu*/int iolen) {
            if (port<0x10) {
                /* read from the first DMA controller (channels 0-3) */
                return DmaControllers[0].ReadControllerReg(port,iolen);
            } else if (port>=0xc0 && port <=0xdf) {
                /* read from the second DMA controller (channels 4-7) */
                return DmaControllers[1].ReadControllerReg((port-0xc0) >> 1,iolen);
            } else switch (port) {
                /* read DMA page register */
                case 0x81:return GetDMAChannel(2).pagenum;
                case 0x82:return GetDMAChannel(3).pagenum;
                case 0x83:return GetDMAChannel(1).pagenum;
                case 0x89:return GetDMAChannel(6).pagenum;
                case 0x8a:return GetDMAChannel(7).pagenum;
                case 0x8b:return GetDMAChannel(5).pagenum;
            }
            return 0;
        }
    };

    static public void DMA_SetWrapping(/*Bitu*/int wrap) {
        dma_wrapping = wrap;
    }

    public DMA(Section configuration) {
        super(configuration);
        /*Bitu*/int i;
        DmaControllers[0] = new DmaController(0);
        if (Dosbox.IS_EGAVGA_ARCH()) DmaControllers[1] = new DmaController(1);
        else DmaControllers[1] = null;

        for (i=0;i<0x10;i++) {
            /*Bitu*/int mask=IoHandler.IO_MB;
            if (i<8) mask|=IoHandler.IO_MW;
            /* install handler for first DMA controller ports */
            DmaControllers[0].DMA_WriteHandler[i].Install(i,DMA_Write_Port,mask);
            DmaControllers[0].DMA_ReadHandler[i].Install(i,DMA_Read_Port,mask);
            if (Dosbox.IS_EGAVGA_ARCH()) {
                /* install handler for second DMA controller ports */
                DmaControllers[1].DMA_WriteHandler[i].Install(0xc0+i*2,DMA_Write_Port,mask);
                DmaControllers[1].DMA_ReadHandler[i].Install(0xc0+i*2,DMA_Read_Port,mask);
            }
        }
        /* install handlers for ports 0x81-0x83 (on the first DMA controller) */
        DmaControllers[0].DMA_WriteHandler[0x10].Install(0x81,DMA_Write_Port,IoHandler.IO_MB,3);
        DmaControllers[0].DMA_ReadHandler[0x10].Install(0x81,DMA_Read_Port,IoHandler.IO_MB,3);

        if (Dosbox.IS_EGAVGA_ARCH()) {
            /* install handlers for ports 0x81-0x83 (on the second DMA controller) */
            DmaControllers[1].DMA_WriteHandler[0x10].Install(0x89,DMA_Write_Port,IoHandler.IO_MB,3);
            DmaControllers[1].DMA_ReadHandler[0x10].Install(0x89,DMA_Read_Port,IoHandler.IO_MB,3);
        }
    }

    static private DMA test;

    public static Section.SectionFunction DMA_Destroy = new Section.SectionFunction() {
        public void call(Section section) {
            test = null;
        }
    };

    public static Section.SectionFunction DMA_Init = new Section.SectionFunction() {
        public void call(Section section) {
            DMA_SetWrapping(0xffff);
            test = new DMA(section);
            section.AddDestroyFunction(DMA_Destroy);
            /*Bitu*/int i;
            for (i=0;i<Paging.LINK_START;i++) {
                ems_board_mapping[i]=i;
            }
        }
    };
}
