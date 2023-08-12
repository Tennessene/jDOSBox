package jdos.ints;

import jdos.Dosbox;
import jdos.cpu.CPU;
import jdos.cpu.CPU_Regs;
import jdos.cpu.Callback;
import jdos.dos.Dos;
import jdos.dos.Dos_programs;
import jdos.dos.Dos_tables;
import jdos.hardware.*;
import jdos.hardware.pci.PCI;
import jdos.misc.Log;
import jdos.misc.setup.Config;
import jdos.misc.setup.Module_base;
import jdos.misc.setup.Section;
import jdos.types.LogSeverities;
import jdos.types.LogTypes;
import jdos.types.MachineType;
import jdos.util.IntRef;

import java.util.Calendar;
import java.util.Vector;

public class Bios extends Module_base {
    static public boolean boot = false;

    static public final int BIOS_BASE_ADDRESS_COM1          =0x400;
    static public final int BIOS_BASE_ADDRESS_COM2          =0x402;
    static public final int BIOS_BASE_ADDRESS_COM3          =0x404;
    static public final int BIOS_BASE_ADDRESS_COM4          =0x406;
    static public final int BIOS_ADDRESS_LPT1               =0x408;
    static public final int BIOS_ADDRESS_LPT2               =0x40a;
    static public final int BIOS_ADDRESS_LPT3               =0x40c;
    /* 0x40e is reserved */
    static public final int BIOS_CONFIGURATION              =0x410;
    /* 0x412 is reserved */
    static public final int BIOS_MEMORY_SIZE                =0x413;
    static public final int BIOS_TRUE_MEMORY_SIZE           =0x415;
    /* static public final int bios_expansion_memory_size      (*(unsigned int   *) 0x415) */
    static public final int BIOS_KEYBOARD_STATE             =0x417;
    static public final int BIOS_KEYBOARD_FLAGS1            =BIOS_KEYBOARD_STATE;
    static public final int BIOS_KEYBOARD_FLAGS2            =0x418;
    static public final int BIOS_KEYBOARD_TOKEN             =0x419;
    /* used for keyboard input with Alt-Number */
    static public final int BIOS_KEYBOARD_BUFFER_HEAD       =0x41a;
    static public final int BIOS_KEYBOARD_BUFFER_TAIL       =0x41c;
    static public final int BIOS_KEYBOARD_BUFFER            =0x41e;
    /* static public final int bios_keyboard_buffer            (*(unsigned int   *) 0x41e) */
    static public final int BIOS_DRIVE_ACTIVE               =0x43e;
    static public final int BIOS_DRIVE_RUNNING              =0x43f;
    static public final int BIOS_DISK_MOTOR_TIMEOUT         =0x440;
    static public final int BIOS_DISK_STATUS                =0x441;
    /* static public final int bios_fdc_result_buffer          (*(unsigned short *) 0x442) */
    static public final int BIOS_VIDEO_MODE                 =0x449;
    static public final int BIOS_SCREEN_COLUMNS             =0x44a;
    static public final int BIOS_VIDEO_MEMORY_USED          =0x44c;
    static public final int BIOS_VIDEO_MEMORY_ADDRESS       =0x44e;
    static public final int BIOS_VIDEO_CURSOR_POS	        =0x450;


    static public final int BIOS_CURSOR_SHAPE               =0x460;
    static public final int BIOS_CURSOR_LAST_LINE           =0x460;
    static public final int BIOS_CURSOR_FIRST_LINE          =0x461;
    static public final int BIOS_CURRENT_SCREEN_PAGE        =0x462;
    static public final int BIOS_VIDEO_PORT                 =0x463;
    static public final int BIOS_VDU_CONTROL                =0x465;
    static public final int BIOS_VDU_COLOR_REGISTER         =0x466;
    /* 0x467-0x468 is reserved */
    static public final int BIOS_TIMER                      =0x46c;
    static public final int BIOS_24_HOURS_FLAG              =0x470;
    static public final int BIOS_KEYBOARD_FLAGS             =0x471;
    static public final int BIOS_CTRL_ALT_DEL_FLAG          =0x472;
    static public final int BIOS_HARDDISK_COUNT		        =0x475;
    /* 0x474, 0x476, 0x477 is reserved */
    static public final int BIOS_LPT1_TIMEOUT               =0x478;
    static public final int BIOS_LPT2_TIMEOUT               =0x479;
    static public final int BIOS_LPT3_TIMEOUT               =0x47a;
    /* 0x47b is reserved */
    static public final int BIOS_COM1_TIMEOUT               =0x47c;
    static public final int BIOS_COM2_TIMEOUT               =0x47d;
    static public final int BIOS_COM3_TIMEOUT               =0x47e;
    static public final int BIOS_COM4_TIMEOUT               =0x47f;
    /* 0x47e is reserved */ //<- why that?
    /* 0x47f-0x4ff is unknow for me */
    static public final int BIOS_KEYBOARD_BUFFER_START      =0x480;
    static public final int BIOS_KEYBOARD_BUFFER_END        =0x482;

    static public final int BIOS_ROWS_ON_SCREEN_MINUS_1     =0x484;
    static public final int BIOS_FONT_HEIGHT                =0x485;

    static public final int BIOS_VIDEO_INFO_0               =0x487;
    static public final int BIOS_VIDEO_INFO_1               =0x488;
    static public final int BIOS_VIDEO_INFO_2               =0x489;
    static public final int BIOS_VIDEO_COMBO                =0x48a;

    static public final int BIOS_KEYBOARD_FLAGS3            =0x496;
    static public final int BIOS_KEYBOARD_LEDS              =0x497;

    static public final int BIOS_WAIT_FLAG_POINTER          =0x498;
    static public final int BIOS_WAIT_FLAG_COUNT	        =0x49c;
    static public final int BIOS_WAIT_FLAG_ACTIVE			=0x4a0;
    static public final int BIOS_WAIT_FLAG_TEMP				=0x4a1;


    static public final int BIOS_PRINT_SCREEN_FLAG          =0x500;

    static public final int BIOS_VIDEO_SAVEPTR              =0x4a8;


    static public int BIOS_DEFAULT_HANDLER_LOCATION() {return Memory.RealMake(0xf000,0xff53);}
    static public int BIOS_DEFAULT_IRQ0_LOCATION()		{return Memory.RealMake(0xf000,0xfea5);}
    static public int BIOS_DEFAULT_IRQ1_LOCATION()		{return Memory.RealMake(0xf000,0xe987);}
    static public int BIOS_DEFAULT_IRQ2_LOCATION()		{return Memory.RealMake(0xf000,0xff55);}
    static public int BIOS_DEFAULT_RESET_LOCATION()		{return Memory.RealMake(0xf000,0xe05b);}

    /* maximum of scancodes handled by keyboard bios routines */
    static public final int MAX_SCAN_CODE =0x58;

    /* The Section handling Bios Disk Access */
    //static public final int BIOS_MAX_DISK 10

    //static public final int MAX_SWAPPABLE_DISKS 20

    /* if mem_systems 0 then size_extended is reported as the real size else
     * zero is reported. ems and xms can increase or decrease the other_memsystems
     * counter using the BIOS_ZeroExtendedSize call */
    static /*Bit16u*/int size_extended;
    static /*Bits*/int other_memsystems=0;

    private static Callback.Handler INT70_Handler = new Callback.Handler() {
        public String getName() {
            return "Bios.INT70_Handler";
        }
        public /*Bitu*/int call() {
            /* Acknowledge irq with cmos */
            IoHandler.IO_Write(0x70,0xc);
            IoHandler.IO_Read(0x71);
            if (Memory.mem_readb(BIOS_WAIT_FLAG_ACTIVE)!=0) {
                /*Bit32u*/long count=Memory.mem_readd(BIOS_WAIT_FLAG_COUNT) & 0xFFFFFFFFl;
                if (count>997) {
                    Memory.mem_writed(BIOS_WAIT_FLAG_COUNT,(int)count-997);
                } else {
                    Memory.mem_writed(BIOS_WAIT_FLAG_COUNT,0);
                    /*PhysPt*/int where=Memory.Real2Phys(Memory.mem_readd(BIOS_WAIT_FLAG_POINTER));
                    Memory.mem_writeb(where,(short)(Memory.mem_readb(where)|0x80));
                    Memory.mem_writeb(BIOS_WAIT_FLAG_ACTIVE,0);
                    Memory.mem_writed(BIOS_WAIT_FLAG_POINTER,Memory.RealMake(0,BIOS_WAIT_FLAG_TEMP));
                    IoHandler.IO_Write(0x70,0xb);
                    IoHandler.IO_Write(0x71,(IoHandler.IO_Read(0x71)&~0x40));
                }
            }
            /* Signal EOI to both pics */
            IoHandler.IO_Write(0xa0,0x20);
            IoHandler.IO_Write(0x20,0x20);
            return 0;
        }
    };

    static private Callback[] tandy_DAC_callback = new Callback[2];
    static private class Tandy_sb {
        /*Bit16u*/int port;
        /*Bit8u*/short irq;
        /*Bit8u*/short dma;
    }
    static private Tandy_sb tandy_sb;
    static private class Tandy_dac {
        /*Bit16u*/int port;
        /*Bit8u*/short irq;
        /*Bit8u*/short dma;
    }
    static private Tandy_dac tandy_dac;

    static private boolean Tandy_InitializeSB() {
        /* see if soundblaster module available and at what port/IRQ/DMA */
        /*Bitu*/IntRef sbport=new IntRef(0), sbirq=new IntRef(0), sbdma=new IntRef(0);
        if (SBlaster.SB_Get_Address(sbport, sbirq, sbdma)) {
            tandy_sb.port=(/*Bit16u*/int)(sbport.value&0xffff);
            tandy_sb.irq =(/*Bit8u*/short)(sbirq.value&0xff);
            tandy_sb.dma =(/*Bit8u*/short)(sbdma.value&0xff);
            return true;
        } else {
            /* no soundblaster accessible, disable Tandy DAC */
            tandy_sb.port=0;
            return false;
        }
    }

    static private boolean Tandy_InitializeTS() {
        /* see if Tandy DAC module available and at what port/IRQ/DMA */
        /*Bitu*/int tsport, tsirq, tsdma;
        // :TODO: :ADD:
//        if (TandySound.TS_Get_Address(tsport, tsirq, tsdma)) {
//            tandy_dac.port=(/*Bit16u*/int)(tsport&0xffff);
//            tandy_dac.irq =(/*Bit8u*/short)(tsirq&0xff);
//            tandy_dac.dma =(/*Bit8u*/short)(tsdma&0xff);
//            return true;
//        } else {
            /* no Tandy DAC accessible */
            tandy_dac.port=0;
            return false;
        //}
    }

    /* check if Tandy DAC is still playing */
    static private boolean Tandy_TransferInProgress() {
        if (Memory.real_readw(0x40,0xd0)!=0) return true;			/* not yet done */
        if (Memory.real_readb(0x40,0xd4)==0xff) return false;	/* still in init-state */

        /*Bit8u*/short tandy_dma = 1;
        if (tandy_sb.port!=0) tandy_dma = tandy_sb.dma;
        else if (tandy_dac.port!=0) tandy_dma = tandy_dac.dma;

        IoHandler.IO_Write(0x0c,0x00);
        /*Bit16u*/int datalen=(/*Bit8u*/short)(IO.IO_ReadB(tandy_dma*2+1)&0xff);
        datalen|=(IO.IO_ReadB(tandy_dma*2+1)<<8);
        if (datalen==0xffff) return false;	/* no DMA transfer */
        else if ((datalen<0x10) && (Memory.real_readb(0x40,0xd4)==0x0f) && (Memory.real_readw(0x40,0xd2)==0x1c)) {
            /* stop already requested */
            return false;
        }
        return true;
    }

    private static void Tandy_SetupTransfer(/*PhysPt*/int bufpt,boolean isplayback) {
        /*Bitu*/int length=Memory.real_readw(0x40,0xd0);
        if (length==0) return;	/* nothing to do... */

        if ((tandy_sb.port==0) && (tandy_dac.port==0)) return;

        /*Bit8u*/short tandy_irq = 7;
        if (tandy_sb.port!=0) tandy_irq = tandy_sb.irq;
        else if (tandy_dac.port!=0) tandy_irq = tandy_dac.irq;
        /*Bit8u*/short tandy_irq_vector = tandy_irq;
        if (tandy_irq_vector<8) tandy_irq_vector += 8;
        else tandy_irq_vector += (0x70-8);

        /* revector IRQ-handler if necessary */
        /*RealPt*/int current_irq=Memory.RealGetVec(tandy_irq_vector);
        if (current_irq!=tandy_DAC_callback[0].Get_RealPointer()) {
            Memory.real_writed(0x40,0xd6,current_irq);
            Memory.RealSetVec(tandy_irq_vector,tandy_DAC_callback[0].Get_RealPointer());
        }

        /*Bit8u*/short tandy_dma = 1;
        if (tandy_sb.port!=0) tandy_dma = tandy_sb.dma;
        else if (tandy_dac.port!=0) tandy_dma = tandy_dac.dma;

        if (tandy_sb.port!=0) {
            IoHandler.IO_Write(tandy_sb.port+0xc,0xd0);				/* stop DMA transfer */
            IoHandler.IO_Write(0x21,(IoHandler.IO_Read(0x21)&(~(1<<tandy_irq))));	/* unmask IRQ */
            IoHandler.IO_Write(tandy_sb.port+0xc,0xd1);				/* turn speaker on */
        } else {
            IoHandler.IO_Write(tandy_dac.port,(IoHandler.IO_Read(tandy_dac.port)&0x60));	/* disable DAC */
            IoHandler.IO_Write(0x21,(IoHandler.IO_Read(0x21)&(~(1<<tandy_irq))));			/* unmask IRQ */
        }

        IoHandler.IO_Write(0x0a,(0x04|tandy_dma));	/* mask DMA channel */
        IoHandler.IO_Write(0x0c,0x00);			/* clear DMA flipflop */
        if (isplayback) IoHandler.IO_Write(0x0b,(0x48|tandy_dma));
        else IoHandler.IO_Write(0x0b,0x44|tandy_dma);
        /* set physical address of buffer */
        /*Bit8u*/short bufpage=(/*Bit8u*/short)((bufpt>>16)&0xff);
        IoHandler.IO_Write(tandy_dma*2,(/*Bit8u*/short)(bufpt&0xff));
        IoHandler.IO_Write(tandy_dma*2,(/*Bit8u*/short)((bufpt>>8)&0xff));
        switch (tandy_dma) {
            case 0: IoHandler.IO_Write(0x87,bufpage); break;
            case 1: IoHandler.IO_Write(0x83,bufpage); break;
            case 2: IoHandler.IO_Write(0x81,bufpage); break;
            case 3: IoHandler.IO_Write(0x82,bufpage); break;
        }
        Memory.real_writeb(0x40,0xd4,bufpage);

        /* calculate transfer size (respects segment boundaries) */
        /*Bit32u*/long tlength=length;
        if (tlength+(bufpt&0xffff)>0x10000) tlength=0x10000-(bufpt&0xffff);
        Memory.real_writew(0x40,0xd0,(/*Bit16u*/int)(length-tlength));	/* remaining buffer length */
        tlength--;

        /* set transfer size */
        IoHandler.IO_Write(tandy_dma*2+1,(/*Bit8u*/short)(tlength&0xff));
        IoHandler.IO_Write(tandy_dma*2+1,(/*Bit8u*/short)((tlength>>8)&0xff));

        /*Bit16u*/int delay=(/*Bit16u*/int)(Memory.real_readw(0x40,0xd2)&0xfff);
        /*Bit8u*/short amplitude=(/*Bit8u*/short)((Memory.real_readw(0x40,0xd2)>>13)&0x7);
        if (tandy_sb.port!=0) {
            IoHandler.IO_Write(0x0a,tandy_dma);	/* enable DMA channel */
            /* set frequency */
            IoHandler.IO_Write(tandy_sb.port+0xc,0x40);
            IoHandler.IO_Write(tandy_sb.port+0xc,256-delay*100/358);
            /* set playback type to 8bit */
            if (isplayback) IoHandler.IO_Write(tandy_sb.port+0xc,0x14);
            else IoHandler.IO_Write(tandy_sb.port+0xc,0x24);
            /* set transfer size */
            IoHandler.IO_Write(tandy_sb.port+0xc,(/*Bit8u*/short)(tlength&0xff));
            IoHandler.IO_Write(tandy_sb.port+0xc,(/*Bit8u*/short)((tlength>>8)&0xff));
        } else {
            if (isplayback) IoHandler.IO_Write(tandy_dac.port,(IoHandler.IO_Read(tandy_dac.port)&0x7c) | 0x03);
            else IoHandler.IO_Write(tandy_dac.port,(IoHandler.IO_Read(tandy_dac.port)&0x7c) | 0x02);
            IoHandler.IO_Write(tandy_dac.port+2,(/*Bit8u*/short)(delay&0xff));
            IoHandler.IO_Write(tandy_dac.port+3,(/*Bit8u*/short)(((delay>>8)&0xf) | (amplitude<<5)));
            if (isplayback) IoHandler.IO_Write(tandy_dac.port,(IoHandler.IO_Read(tandy_dac.port)&0x7c) | 0x1f);
            else IoHandler.IO_Write(tandy_dac.port,(IoHandler.IO_Read(tandy_dac.port)&0x7c) | 0x1e);
            IoHandler.IO_Write(0x0a,tandy_dma);	/* enable DMA channel */
        }

        if (!isplayback) {
            /* mark transfer as recording operation */
            Memory.real_writew(0x40,0xd2,(/*Bit16u*/int)(delay|0x1000));
        }
    }

    private static Callback.Handler IRQ_TandyDAC = new Callback.Handler() {
        public String getName() {
            return "Bios.IRQ_TandyDAC";
        }
        public /*Bitu*/int call() {
            if (tandy_dac.port!=0) {
                IoHandler.IO_Read(tandy_dac.port);
            }
            if (Memory.real_readw(0x40,0xd0)!=0) {	/* play/record next buffer */
                /* acknowledge IRQ */
                IoHandler.IO_Write(0x20,0x20);
                if (tandy_sb.port!=0) {
                    IoHandler.IO_Read(tandy_sb.port+0xe);
                }

                /* buffer starts at the next page */
                /*Bit8u*/short npage=(short)(Memory.real_readb(0x40,0xd4)+1);
                Memory.real_writeb(0x40,0xd4,npage);

                /*Bitu*/int rb=Memory.real_readb(0x40,0xd3);
                if ((rb&0x10)!=0) {
                    /* start recording */
                    Memory.real_writeb(0x40,0xd3,rb&0xef);
                    Tandy_SetupTransfer(npage<<16,false);
                } else {
                    /* start playback */
                    Tandy_SetupTransfer(npage<<16,true);
                }
            } else {	/* playing/recording is finished */
                /*Bit8u*/short tandy_irq = 7;
                if (tandy_sb.port!=0) tandy_irq = tandy_sb.irq;
                else if (tandy_dac.port!=0) tandy_irq = tandy_dac.irq;
                /*Bit8u*/short tandy_irq_vector = tandy_irq;
                if (tandy_irq_vector<8) tandy_irq_vector += 8;
                else tandy_irq_vector += (0x70-8);

                Memory.RealSetVec(tandy_irq_vector,Memory.real_readd(0x40,0xd6));

                /* turn off speaker and acknowledge soundblaster IRQ */
                if (tandy_sb.port!=0) {
                    IoHandler.IO_Write(tandy_sb.port+0xc,0xd3);
                    IoHandler.IO_Read(tandy_sb.port+0xe);
                }

                /* issue BIOS tandy sound device busy callout */
                CPU_Regs.SegSet16CS(Memory.RealSeg(tandy_DAC_callback[1].Get_RealPointer()));
                CPU_Regs.reg_ip((short)Memory.RealOff(tandy_DAC_callback[1].Get_RealPointer()));
            }
            return Callback.CBRET_NONE;
        }
    };

    static void TandyDAC_Handler(/*Bit8u*/short tfunction) {
        if ((tandy_sb.port==0) && (tandy_dac.port==0)) return;
        switch (tfunction) {
        case 0x81:	/* Tandy sound system check */
            if (tandy_dac.port!=0) {
                CPU_Regs.reg_eax.word(tandy_dac.port);
            } else {
                CPU_Regs.reg_eax.word(0xc4);
            }
            Callback.CALLBACK_SCF(Tandy_TransferInProgress());
            break;
        case 0x82:	/* Tandy sound system start recording */
        case 0x83:	/* Tandy sound system start playback */
            if (Tandy_TransferInProgress()) {
                /* cannot play yet as the last transfer isn't finished yet */
                CPU_Regs.reg_eax.high(0x00);
                Callback.CALLBACK_SCF(true);
                break;
            }
            /* store buffer length */
            Memory.real_writew(0x40,0xd0,CPU_Regs.reg_ecx.word());
            /* store delay and volume */
            Memory.real_writew(0x40,0xd2,(CPU_Regs.reg_edx.word()&0xfff)|((CPU_Regs.reg_eax.low()&7)<<13));
            Tandy_SetupTransfer(Memory.PhysMake((int)CPU_Regs.reg_esVal.dword,CPU_Regs.reg_ebx.word()),CPU_Regs.reg_eax.high()==0x83);
            CPU_Regs.reg_eax.high(0x00);
            Callback.CALLBACK_SCF(false);
            break;
        case 0x84:	/* Tandy sound system stop playing */
            CPU_Regs.reg_eax.high(0x00);

            /* setup for a small buffer with silence */
            Memory.real_writew(0x40,0xd0,0x0a);
            Memory.real_writew(0x40,0xd2,0x1c);
            Tandy_SetupTransfer(Memory.PhysMake(0xf000,0xa084),true);
            Callback.CALLBACK_SCF(false);
            break;
        case 0x85:	/* Tandy sound system reset */
            if (tandy_dac.port!=0) {
                IoHandler.IO_Write(tandy_dac.port,(/*Bit8u*/short)(IoHandler.IO_Read(tandy_dac.port)&0xe0));
            }
            CPU_Regs.reg_eax.high(0x00);
            Callback.CALLBACK_SCF(false);
            break;
        }
    }

    private static Callback.Handler INT1A_Handler = new Callback.Handler() {
        public String getName() {
            return "Bios.INT1A_Handler 0x"+Integer.toHexString(CPU_Regs.reg_eax.high());
        }
        public /*Bitu*/int call() {
            switch (CPU_Regs.reg_eax.high() & 0xFF) {
            case 0x00:	/* Get System time */
                {
                    /*Bit32u*/int ticks=Memory.mem_readd(BIOS_TIMER);
                    CPU_Regs.reg_eax.low(Memory.mem_readb(BIOS_24_HOURS_FLAG));
			        Memory.mem_writeb(BIOS_24_HOURS_FLAG,0); // reset the "flag"
                    CPU_Regs.reg_ecx.word(ticks >> 16);
                    CPU_Regs.reg_edx.word(ticks & 0xffff);
                    break;
                }
            case 0x01:	/* Set System time */
                Memory.mem_writed(BIOS_TIMER,(CPU_Regs.reg_ecx.word()<<16)|CPU_Regs.reg_edx.word());
                break;
            case 0x02:	/* GET REAL-TIME CLOCK TIME (AT,XT286,PS) */
                IoHandler.IO_Write(0x70,0x04);		//Hours
                CPU_Regs.reg_ecx.high(IoHandler.IO_Read(0x71));
                IoHandler.IO_Write(0x70,0x02);		//Minutes
                CPU_Regs.reg_ecx.low(IoHandler.IO_Read(0x71));
                IoHandler.IO_Write(0x70,0x00);		//Seconds
                CPU_Regs.reg_edx.high(IoHandler.IO_Read(0x71));
                CPU_Regs.reg_edx.low(0);			//Daylight saving disabled
                Callback.CALLBACK_SCF(false);
                break;
            case 0x04:	/* GET REAL-TIME ClOCK DATE  (AT,XT286,PS) */
                IoHandler.IO_Write(0x70,0x32);		//Centuries
                CPU_Regs.reg_ecx.high(IoHandler.IO_Read(0x71));
                IoHandler.IO_Write(0x70,0x09);		//Years
                CPU_Regs.reg_ecx.low(IoHandler.IO_Read(0x71));
                IoHandler.IO_Write(0x70,0x08);		//Months
                CPU_Regs.reg_edx.high(IoHandler.IO_Read(0x71));
                IoHandler.IO_Write(0x70,0x07);		//Days
                CPU_Regs.reg_edx.low(IoHandler.IO_Read(0x71));
                Callback.CALLBACK_SCF(false);
                break;
            case 0x80:	/* Pcjr Setup Sound Multiplexer */
                if (Log.level<=LogSeverities.LOG_ERROR) Log.log(LogTypes.LOG_BIOS, LogSeverities.LOG_ERROR,"INT1A:80:Setup tandy sound multiplexer to "+Integer.toString(CPU_Regs.reg_eax.low()));
                break;
            case 0x81:	/* Tandy sound system check */
            case 0x82:	/* Tandy sound system start recording */
            case 0x83:	/* Tandy sound system start playback */
            case 0x84:	/* Tandy sound system stop playing */
            case 0x85:	/* Tandy sound system reset */
                TandyDAC_Handler((short)CPU_Regs.reg_eax.high());
                break;
            case 0xb1:		/* PCI Bios Calls */
                if (Log.level<=LogSeverities.LOG_WARN) Log.log(LogTypes.LOG_BIOS,LogSeverities.LOG_WARN,"INT1A:PCI bios call "+Integer.toString(CPU_Regs.reg_eax.low(),16));
                if (!Config.PCI_FUNCTIONALITY_ENABLED) {
                    Callback.CALLBACK_SCF(true);
                } else {
                    switch (CPU_Regs.reg_eax.low()) {
                        case 0x01:	// installation check
                            if (PCI.PCI_IsInitialized()) {
                                CPU_Regs.reg_eax.high(0x00);
                                CPU_Regs.reg_eax.low(0x01);	// cfg space mechanism 1 supported
                                CPU_Regs.reg_ebx.word(0x0210);	// ver 2.10
                                CPU_Regs.reg_ecx.word(0x0000);	// only one PCI bus
                                CPU_Regs.reg_edx.word(0x20494350);
                                CPU_Regs.reg_edi.dword=PCI.PCI_GetPModeInterface();
                                Callback.CALLBACK_SCF(false);
                            } else {
                                Callback.CALLBACK_SCF(true);
                            }
                            break;
                        case 0x02: {	// find device
                            /*Bitu*/int devnr=0;
                            /*Bitu*/int count=0x100;
                            /*Bit32u*/int devicetag=((CPU_Regs.reg_ecx.word())<<16)|CPU_Regs.reg_edx.dword;
                            /*Bits*/int found=-1;
                            for (/*Bitu*/int i=0; i<=count; i++) {
                                IO.IO_WriteD(0xcf8,0x80000000|(i<<8));	// query unique device/subdevice entries
                                if (IO.IO_ReadD(0xcfc)==devicetag) {
                                    if (devnr==CPU_Regs.reg_esi.word()) {
                                        found=i;
                                        break;
                                    } else {
                                        // device found, but not the SIth device
                                        devnr++;
                                    }
                                }
                            }
                            if (found>=0) {
                                CPU_Regs.reg_eax.high(0x00);
                                CPU_Regs.reg_ebx.high(0x00);	// bus 0
                                CPU_Regs.reg_ebx.low(found&0xff);
                                Callback.CALLBACK_SCF(false);
                            } else {
                                CPU_Regs.reg_eax.high(0x86);	// device not found
                                Callback.CALLBACK_SCF(true);
                            }
                            }
                            break;
                        case 0x03: {	// find device by class code
                            /*Bitu*/int devnr=0;
                            /*Bitu*/int count=0x100;
                            /*Bit32u*/int classtag=CPU_Regs.reg_ecx.dword & 0xffffff;
                            /*Bits*/int found=-1;
                            for (/*Bitu*/int i=0; i<=count; i++) {
                                IO.IO_WriteD(0xcf8,0x80000000|(i<<8));	// query unique device/subdevice entries
                                if (IO.IO_ReadD(0xcfc)!=0xffffffff) {
                                    IO.IO_WriteD(0xcf8,0x80000000|(i<<8)|0x08);
                                    if ((IO.IO_ReadD(0xcfc)>>>8)==classtag) {
                                        if (devnr==CPU_Regs.reg_esi.word()) {
                                            found=i;
                                            break;
                                        } else {
                                            // device found, but not the SIth device
                                            devnr++;
                                        }
                                    }
                                }
                            }
                            if (found>=0) {
                                CPU_Regs.reg_eax.high(0x00);
                                CPU_Regs.reg_ebx.high(0x00);	// bus 0
                                CPU_Regs.reg_ebx.low(found&0xff);
                                Callback.CALLBACK_SCF(false);
                            } else {
                                CPU_Regs.reg_eax.high(0x86);	// device not found
                                Callback.CALLBACK_SCF(true);
                            }
                            }
                            break;
                        case 0x08:	// read configuration byte
                            IO.IO_WriteD(0xcf8,0x80000000|(CPU_Regs.reg_ebx.word()<<8)|(CPU_Regs.reg_edi.word()&0xfc));
                            CPU_Regs.reg_ecx.low(IO.IO_ReadB(0xcfc+(CPU_Regs.reg_edi.word()&3)));
                            Callback.CALLBACK_SCF(false);
                            break;
                        case 0x09:	// read configuration word
                            IO.IO_WriteD(0xcf8,0x80000000|(CPU_Regs.reg_ebx.word()<<8)|(CPU_Regs.reg_edi.word()&0xfc));
                            CPU_Regs.reg_ecx.word(IO.IO_ReadW(0xcfc+(CPU_Regs.reg_edi.word()&2)));
                            Callback.CALLBACK_SCF(false);
                            break;
                        case 0x0a:	// read configuration dword
                            IO.IO_WriteD(0xcf8,0x80000000|(CPU_Regs.reg_ebx.word()<<8)|(CPU_Regs.reg_edi.word()&0xfc));
                            CPU_Regs.reg_ecx.dword=IO.IO_ReadD(0xcfc+(CPU_Regs.reg_edi.word()&3));
                            Callback.CALLBACK_SCF(false);
                            break;
                        case 0x0b:	// write configuration byte
                            IO.IO_WriteD(0xcf8,0x80000000|(CPU_Regs.reg_ebx.word()<<8)|(CPU_Regs.reg_edi.word()&0xfc));
                            IO.IO_WriteB(0xcfc+(CPU_Regs.reg_edi.word()&3),CPU_Regs.reg_ecx.low());
                            Callback.CALLBACK_SCF(false);
                            break;
                        case 0x0c:	// write configuration word
                            IO.IO_WriteD(0xcf8,0x80000000|(CPU_Regs.reg_ebx.word()<<8)|(CPU_Regs.reg_edi.word()&0xfc));
                            IO.IO_WriteW(0xcfc+(CPU_Regs.reg_edi.word()&2),CPU_Regs.reg_ecx.word());
                            Callback.CALLBACK_SCF(false);
                            break;
                        case 0x0d:	// write configuration dword
                            IO.IO_WriteD(0xcf8,0x80000000|(CPU_Regs.reg_ebx.word()<<8)|(CPU_Regs.reg_edi.word()&0xfc));
                            IO.IO_WriteD(0xcfc+(CPU_Regs.reg_edi.word() & 3), CPU_Regs.reg_ecx.dword);
                            Callback.CALLBACK_SCF(false);
                            break;
                        case 0x0e: /// Get IRQ Routine Information
                        default:
                            if (Log.level<=LogSeverities.LOG_ERROR) Log.log(LogTypes.LOG_BIOS,LogSeverities.LOG_ERROR,"INT1A:PCI BIOS: unknown function "+Integer.toString(CPU_Regs.reg_eax.word(), 16)+" ("+Integer.toString(CPU_Regs.reg_ebx.word(), 16)+" "+Integer.toString(CPU_Regs.reg_ecx.word(), 16)+" "+Integer.toString(CPU_Regs.reg_edx.word(), 16)+")");
                            Callback.CALLBACK_SCF(true);
                            break;
                    }
                }

                break;
            default:
                if (Log.level<=LogSeverities.LOG_ERROR) Log.log(LogTypes.LOG_BIOS,LogSeverities.LOG_ERROR,"INT1A:Undefined call "+Integer.toString(CPU_Regs.reg_eax.high(),16));
            }
            return Callback.CBRET_NONE;
        }
    };

    private static Callback.Handler INT11_Handler = new Callback.Handler() {
        public String getName() {
            return "Bios.INT11_Handler";
        }
        public /*Bitu*/int call() {
            CPU_Regs.reg_eax.word(Memory.mem_readw(BIOS_CONFIGURATION));
            return Callback.CBRET_NONE;
        }
    };
    /*
     * Define the following define to 1 if you want dosbox to check
     * the system time every 5 seconds and adjust 1/2 a second to sync them.
     */
//    #ifndef DOSBOX_CLOCKSYNC
//    #define DOSBOX_CLOCKSYNC 0
//    #endif

    static void BIOS_HostTimeSync() {
        /* Setup time and date */
        Calendar calendar = Calendar.getInstance();

        Dos.dos.date.day=(byte)calendar.get(Calendar.DAY_OF_MONTH);
        Dos.dos.date.month=(byte)(calendar.get(Calendar.MONTH)+1);
        Dos.dos.date.year=(short)calendar.get(Calendar.YEAR);

        /*Bit32u*/long ticks=(long)(((double)(
            calendar.get(Calendar.HOUR_OF_DAY)*3600*1000+
            calendar.get(Calendar.MINUTE)*60*1000+
            calendar.get(Calendar.SECOND)*1000+
            calendar.get(Calendar.MILLISECOND)))*(((double)Timer.PIT_TICK_RATE/65536.0)/1000.0));
        Memory.mem_writed(BIOS_TIMER, (int)ticks);
    }

    private static Callback.Handler INT8_Handler = new Callback.Handler() {
        public String getName() {
            return "Bios.INT8_Handler";
        }
        public /*Bitu*/int call() {
            /* Increase the bios tick counter */
            /*Bit32u*/int value = Memory.mem_readd(BIOS_TIMER) + 1;
            if(value >= 0x1800B0) {
		        // time wrap at midnight
		        Memory.mem_writeb(BIOS_24_HOURS_FLAG, Memory.mem_readb(BIOS_24_HOURS_FLAG) + 1);
		        value=0;
	        }
    //    #if DOSBOX_CLOCKSYNC
    //        static boolean check = false;
    //        if((value %50)==0) {
    //            if(((value %100)==0) && check) {
    //                check = false;
    //                time_t curtime;struct tm *loctime;
    //                curtime = time (NULL);loctime = localtime (&curtime);
    //                /*Bit32u*/long ticksnu = (/*Bit32u*/long)((loctime.tm_hour*3600+loctime.tm_min*60+loctime.tm_sec)*(float)PIT_TICK_RATE/65536.0);
    //                Bit32s bios = value;Bit32s tn = ticksnu;
    //                Bit32s diff = tn - bios;
    //                if(diff>0) {
    //                    if(diff < 18) { diff  = 0; } else diff = 9;
    //                } else {
    //                    if(diff > -18) { diff = 0; } else diff = -9;
    //                }
    //
    //                value += diff;
    //            } else if((value%100)==50) check = true;
    //        }
    //    #endif
            Memory.mem_writed(BIOS_TIMER,value);

            /* decrease floppy motor timer */
            /*Bit8u*/int val = Memory.mem_readb(BIOS_DISK_MOTOR_TIMEOUT);
            if (val!=0) Memory.mem_writeb(BIOS_DISK_MOTOR_TIMEOUT,(short)(val-1));
            /* and running drive */
            Memory.mem_writeb(BIOS_DRIVE_RUNNING,Memory.mem_readb(BIOS_DRIVE_RUNNING) & 0xF0);
            return Callback.CBRET_NONE;
        }
};
//    #undef DOSBOX_CLOCKSYNC


    private static Callback.Handler INT1C_Handler = new Callback.Handler() {
        public String getName() {
            return "Bios.INT1C_Handler";
        }
        public /*Bitu*/int call() {
            return Callback.CBRET_NONE;
        }
    };

    private static Callback.Handler INT12_Handler = new Callback.Handler() {
        public String getName() {
            return "Bios.INT12_Handler";
        }
        public /*Bitu*/int call() {
            CPU_Regs.reg_eax.word(Memory.mem_readw(BIOS_MEMORY_SIZE));
            return Callback.CBRET_NONE;
        }
    };

    private static Callback.Handler INT17_Handler = new Callback.Handler() {
        public String getName() {
            return "Bios.INT17_Handler";
        }
        public /*Bitu*/int call() {
            if (Log.level<=LogSeverities.LOG_NORMAL) Log.log(LogTypes.LOG_BIOS,LogSeverities.LOG_NORMAL,"INT17:Function "+Integer.toString(CPU_Regs.reg_eax.high(),16));
            switch(CPU_Regs.reg_eax.high()) {
            case 0x00:		/* PRINTER: Write Character */
                CPU_Regs.reg_eax.high(1);	/* Report a timeout */
                break;
            case 0x01:		/* PRINTER: Initialize port */
                break;
            case 0x02:		/* PRINTER: Get Status */
                CPU_Regs.reg_eax.high(0);
                break;
            case 0x20:		/* Some sort of printerdriver install check*/
                break;
            default:
                Log.exit("Unhandled INT 17 call "+Integer.toString(CPU_Regs.reg_eax.high(),16));
            }
            return Callback.CBRET_NONE;
        }
    };

    private static boolean INT14_Wait(/*Bit16u*/int port, /*Bit8u*/short mask, /*Bit8u*/short timeout, IntRef retval) {
        double starttime = Pic.PIC_FullIndex();
        double timeout_f = timeout * 1000.0;
        while (((retval.value = IO.IO_ReadB(port)) & mask) != mask) {
            if (starttime < (Pic.PIC_FullIndex() - timeout_f)) {
                return false;
            }
            Callback.CALLBACK_Idle();
        }
        return true;
    }

    private static Callback.Handler INT14_Handler = new Callback.Handler() {
        public String getName() {
            return "Bios.INT14_Handler";
        }
        public /*Bitu*/int call() {
            if (CPU_Regs.reg_eax.high() > 0x3 || CPU_Regs.reg_edx.word() > 0x3) {	// 0-3 serial port functions
                                                // and no more than 4 serial ports
                Log.log_msg("BIOS INT14: Unhandled call AH="+Integer.toString(CPU_Regs.reg_eax.high(), 16)+" DX="+Integer.toString(CPU_Regs.reg_edx.word(),16));
                return Callback.CBRET_NONE;
            }

            /*Bit16u*/int port = Memory.real_readw(0x40,CPU_Regs.reg_edx.word()*2); // DX is always port number
            /*Bit8u*/short timeout = (short)Memory.mem_readb(BIOS_COM1_TIMEOUT + CPU_Regs.reg_edx.word());
            if (port==0)	{
                if (Log.level<=LogSeverities.LOG_NORMAL) Log.log(LogTypes.LOG_BIOS,LogSeverities.LOG_NORMAL,"BIOS INT14: port "+CPU_Regs.reg_edx.word()+" does not exist.");
                return Callback.CBRET_NONE;
            }
            switch (CPU_Regs.reg_eax.high())	{
            case 0x00:	{
                // Initialize port
                // Parameters:				Return:
                // AL: port parameters		AL: modem status
                //							AH: line status

                // set baud rate
                /*Bitu*/int baudrate = 9600;
                /*Bit16u*/int baudresult;
                /*Bitu*/int rawbaud=CPU_Regs.reg_eax.low()>>5;

                if (rawbaud==0){ baudrate=110;}
                else if (rawbaud==1){ baudrate=150;}
                else if (rawbaud==2){ baudrate=300;}
                else if (rawbaud==3){ baudrate=600;}
                else if (rawbaud==4){ baudrate=1200;}
                else if (rawbaud==5){ baudrate=2400;}
                else if (rawbaud==6){ baudrate=4800;}
                else if (rawbaud==7){ baudrate=9600;}

                baudresult = (/*Bit16u*/int)(115200 / baudrate);

                IO.IO_WriteB(port+3, 0x80);	// enable divider access
                IO.IO_WriteB(port, (/*Bit8u*/short)baudresult&0xff);
                IO.IO_WriteB(port+1, (/*Bit8u*/short)(baudresult>>8));

                // set line parameters, disable divider access
                IO.IO_WriteB(port+3, CPU_Regs.reg_eax.low()&0x1F); // LCR

                // disable interrupts
                IO.IO_WriteB(port+1, 0); // IER

                // get result
                CPU_Regs.reg_eax.high(IO.IO_ReadB(port+5)&0xff);
                CPU_Regs.reg_eax.low(IO.IO_ReadB(port+6)&0xff);
                Callback.CALLBACK_SCF(false);
                break;
            }
            case 0x01: { // Transmit character
                // Parameters:				Return:
                // AL: character			AL: unchanged
                // AH: 0x01					AH: line status from just before the char was sent
                //								(0x80 | unpredicted) in case of timeout
                //						[undoc]	(0x80 | line status) in case of tx timeout
                //						[undoc]	(0x80 | modem status) in case of dsr/cts timeout

                // set DTR & RTS on
                IO.IO_WriteB(port+4,0x3);

                // wait for DSR & CTS
                IntRef result = new IntRef(CPU_Regs.reg_eax.high());
                if (INT14_Wait(port+6, (short)0x30, timeout, result)) {
                    // wait for TX buffer empty
                    if (INT14_Wait(port+5, (short)0x20, timeout, result)) {
                        CPU_Regs.reg_eax.high(result.value);
                        // fianlly send the character
                        IO.IO_WriteB(port,CPU_Regs.reg_eax.low());
                    } else {
                        CPU_Regs.reg_eax.high(result.value |= 0x80);
                    }
                } else { // timed out
                    CPU_Regs.reg_eax.high(result.value |= 0x80);
                }
                Callback.CALLBACK_SCF(false);
                break;
            }
            case 0x02: // Read character
                // Parameters:				Return:
                // AH: 0x02					AL: received character
                //						[undoc]	will be trashed in case of timeout
                //							AH: (line status & 0x1E) in case of success
                //								(0x80 | unpredicted) in case of timeout
                //						[undoc]	(0x80 | line status) in case of rx timeout
                //						[undoc]	(0x80 | modem status) in case of dsr timeout

                // set DTR on
                IO.IO_WriteB(port+4,0x1);

                // wait for DSR
                IntRef result = new IntRef(CPU_Regs.reg_eax.high());
                if (INT14_Wait(port+6, (short)0x20, timeout, result)) {
                    // wait for character to arrive
                    if (INT14_Wait(port+5, (short)0x01, timeout, result)) {
                        CPU_Regs.reg_eax.high(result.value & 0x1E);
                        CPU_Regs.reg_eax.low(IO.IO_ReadB(port));
                    } else {
                        CPU_Regs.reg_eax.high(result.value |= 0x80);
                    }
                } else {
                    CPU_Regs.reg_eax.high(result.value |= 0x80);
                }
                Callback.CALLBACK_SCF(false);
                break;
            case 0x03: // get status
                CPU_Regs.reg_eax.high((IO.IO_ReadB(port+5)&0xff));
                CPU_Regs.reg_eax.low((IO.IO_ReadB(port+6)&0xff));
                Callback.CALLBACK_SCF(false);
                break;

            }
            return Callback.CBRET_NONE;
        }
    };

    private static /*Bit16u*/int biosConfigSeg=0;
    private static boolean apm_realmode_connected = false;
    static private class E820 {
        public E820(int base, int len, boolean reserved) {
            this.base = base;
            this.len = len;
            this.reserved = reserved;
        }
        int base;
        int len;
        boolean reserved;
    }
    private static Vector<E820> e820table = new Vector<E820>();

    {
        e820table.add(new E820(0, 0x09F000, false));
        e820table.add(new E820(0xf0000, 0x010000, true));
        e820table.add(new E820(0x100000, (Memory.MEM_TotalPages()*4096)-0x100000, false));
         /* return a minimalist list:
                                 *
                                 *    0) 0x000000-0x09EFFF       Free memory
                                 *    1) 0x0C0000-0x0FFFFF       Reserved
                                 *    2) 0x100000-...            Free memory (no ACPI tables) */
    }
    private static Callback.Handler INT15_Handler = new Callback.Handler() {
        public String getName() {
            return "Bios.INT15_Handler "+Integer.toHexString(CPU_Regs.reg_eax.high() & 0xFF);
        }
        public /*Bitu*/int call() {
            switch (CPU_Regs.reg_eax.high() & 0xFF) {
            case 0x06:
                Log.log(LogTypes.LOG_BIOS,LogSeverities.LOG_NORMAL,"INT15 Unkown Function 6");
                break;
            case 0xC1:
                CPU_Regs.reg_eax.high(0x80);
                Callback.CALLBACK_SCF(true);
                break;
            case 0xC0:	/* Get Configuration*/
                {
                    if (biosConfigSeg==0) biosConfigSeg = Dos_tables.DOS_GetMemory(1); //We have 16 bytes
                    /*PhysPt*/int data	= Memory.PhysMake(biosConfigSeg,0);
                    Memory.mem_writew(data,8);						// 8 Bytes following
                    if (Dosbox.IS_TANDY_ARCH()) {
                        if (Dosbox.machine== MachineType.MCH_TANDY) {
                            // Model ID (Tandy)
                            Memory.mem_writeb(data+2,0xFF);
                        } else {
                            // Model ID (PCJR)
                            Memory.mem_writeb(data+2,0xFD);
                        }
                        Memory.mem_writeb(data+3,0x0A);					// Submodel ID
                        Memory.mem_writeb(data+4,0x10);					// Bios Revision
                        /* Tandy doesn't have a 2nd PIC, left as is for now */
                        Memory.mem_writeb(data+5,(1<<6)|(1<<5)|(1<<4));	// Feature Byte 1
                    } else {
                        Memory.mem_writeb(data+2,0xFC);					// Model ID (PC)
                        Memory.mem_writeb(data+3,0x00);					// Submodel ID
                        Memory.mem_writeb(data+4,0x01);					// Bios Revision
                        Memory.mem_writeb(data+5,(1<<6)|(1<<5)|(1<<4));	// Feature Byte 1
                    }
                    Memory.mem_writeb(data+6,(1<<6));				// Feature Byte 2
                    Memory.mem_writeb(data+7,0);					// Feature Byte 3
                    Memory.mem_writeb(data+8,0);					// Feature Byte 4
                    Memory.mem_writeb(data+9,0);					// Feature Byte 5
                    CPU.CPU_SetSegGeneralES(biosConfigSeg);
                    CPU_Regs.reg_ebx.word(0);
                    CPU_Regs.reg_eax.high(0);
                    Callback.CALLBACK_SCF(false);
                } break;
            case 0x4f:	/* BIOS - Keyboard intercept */
                /* Carry should be set but let's just set it just in case */
                Callback.CALLBACK_SCF(true);
                break;
            case 0x83:	/* BIOS - SET EVENT WAIT INTERVAL */
                {
                    if(CPU_Regs.reg_eax.low() == 0x01) { /* Cancel it */
                        Memory.mem_writeb(BIOS_WAIT_FLAG_ACTIVE,0);
                        IoHandler.IO_Write(0x70,0xb);
                        IoHandler.IO_Write(0x71,IoHandler.IO_Read(0x71)&~0x40);
                        Callback.CALLBACK_SCF(false);
                        break;
                    }
                    if (Memory.mem_readb(BIOS_WAIT_FLAG_ACTIVE)!=0) {
                        CPU_Regs.reg_eax.high(0x80);
                        Callback.CALLBACK_SCF(true);
                        break;
                    }
                    /*Bit32u*/long count=(CPU_Regs.reg_ecx.word()<<16)|CPU_Regs.reg_edx.word();
                    Memory.mem_writed(BIOS_WAIT_FLAG_POINTER,Memory.RealMake(CPU_Regs.reg_esVal.dword,CPU_Regs.reg_ebx.word()));
                    Memory.mem_writed(BIOS_WAIT_FLAG_COUNT,(int)count);
                    Memory.mem_writeb(BIOS_WAIT_FLAG_ACTIVE,1);
                    /* Reprogram RTC to start */
                    IoHandler.IO_Write(0x70,0xb);
                    IoHandler.IO_Write(0x71,IoHandler.IO_Read(0x71)|0x40);
                    Callback.CALLBACK_SCF(false);
                }
                break;
            case 0x84:	/* BIOS - JOYSTICK SUPPORT (XT after 11/8/82,AT,XT286,PS) */
                if (CPU_Regs.reg_edx.word() == 0x0000) {
                    // Get Joystick button status
                    if (Joystick.JOYSTICK_IsEnabled(0) || Joystick.JOYSTICK_IsEnabled(1)) {
                        CPU_Regs.reg_eax.low(IO.IO_ReadB(0x201)&0xf0);
                        Callback.CALLBACK_SCF(false);
                    } else {
                        // dos values
                        CPU_Regs.reg_eax.word(0x00f0); CPU_Regs.reg_edx.word(0x0201);
                        Callback.CALLBACK_SCF(true);
                    }
                } else if (CPU_Regs.reg_edx.word() == 0x0001) {
                    if (Joystick.JOYSTICK_IsEnabled(0)) {
                        CPU_Regs.reg_eax.word((/*Bit16u*/int)(Joystick.JOYSTICK_GetMove_X(0)*127+128));
                        CPU_Regs.reg_ebx.word((/*Bit16u*/int)(Joystick.JOYSTICK_GetMove_Y(0)*127+128));
                        if(Joystick.JOYSTICK_IsEnabled(1)) {
                            CPU_Regs.reg_ecx.word((/*Bit16u*/int)(Joystick.JOYSTICK_GetMove_X(1)*127+128));
                            CPU_Regs.reg_edx.word((/*Bit16u*/int)(Joystick.JOYSTICK_GetMove_Y(1)*127+128));
                        }
                        else {
                            CPU_Regs.reg_ecx.word(0);CPU_Regs.reg_edx.word(0);
                        }
                        Callback.CALLBACK_SCF(false);
                    } else if (Joystick.JOYSTICK_IsEnabled(1)) {
                        CPU_Regs.reg_eax.word(0); CPU_Regs.reg_ebx.word(0);
                        CPU_Regs.reg_ecx.word((/*Bit16u*/int)(Joystick.JOYSTICK_GetMove_X(1)*127+128));
                        CPU_Regs.reg_edx.word((/*Bit16u*/int)(Joystick.JOYSTICK_GetMove_Y(1)*127+128));
                        Callback.CALLBACK_SCF(false);
                    } else {
                        CPU_Regs.reg_eax.word(0); CPU_Regs.reg_ebx.word(0); CPU_Regs.reg_ecx.word(0); CPU_Regs.reg_edx.word(0);
                        Callback.CALLBACK_SCF(true);
                    }
                } else {
                    Log.log(LogTypes.LOG_BIOS,LogSeverities.LOG_ERROR,"INT15:84:Unknown Bios Joystick functionality.");
                }
                break;
            case 0x86:	/* BIOS - WAIT (AT,PS) */
                {
                    if (Memory.mem_readb(BIOS_WAIT_FLAG_ACTIVE)!=0) {
                        CPU_Regs.reg_eax.high(0x83);
                        Callback.CALLBACK_SCF(true);
                        break;
                    }
                    /*Bit32u*/long count=(CPU_Regs.reg_ecx.word()<<16)|CPU_Regs.reg_edx.word();
                    Memory.mem_writed(BIOS_WAIT_FLAG_POINTER,Memory.RealMake(0,BIOS_WAIT_FLAG_TEMP));
                    Memory.mem_writed(BIOS_WAIT_FLAG_COUNT,(int)count);
                    Memory.mem_writeb(BIOS_WAIT_FLAG_ACTIVE,1);
                    /* Reprogram RTC to start */
                    IoHandler.IO_Write(0x70,0xb);
                    IoHandler.IO_Write(0x71,IoHandler.IO_Read(0x71)|0x40);
                    while (Memory.mem_readd(BIOS_WAIT_FLAG_COUNT)!=0) {
                        Callback.CALLBACK_Idle();
                    }
                    Callback.CALLBACK_SCF(false);
                    break;
                }
            case 0x87:	/* Copy extended memory */
                {
                    boolean enabled = Memory.MEM_A20_Enabled();
                    Memory.MEM_A20_Enable(true);
                    /*Bitu*/int   bytes	= CPU_Regs.reg_ecx.word() * 2;
                    /*PhysPt*/int data		= CPU_Regs.reg_esPhys.dword+CPU_Regs.reg_esi.word();
                    /*PhysPt*/int source	= ((Memory.mem_readd(data + 0x12) & 0x00FFFFFF) + (Memory.mem_readb(data+0x16)<<24));
                    /*PhysPt*/int dest		= ((Memory.mem_readd(data + 0x1A) & 0x00FFFFFF) + (Memory.mem_readb(data+0x1E)<<24));
                    Memory.MEM_BlockCopy(dest,source,bytes);
                    CPU_Regs.reg_eax.word(0x00);
                    Memory.MEM_A20_Enable(enabled);
                    Callback.CALLBACK_SCF(false);
                    break;
                }
                case 0xE8:
                    if (false) {
                        CPU_Regs.reg_eax.word(other_memsystems!=0?0:size_extended);
                        Callback.CALLBACK_SCF(false);
                        break;
                    }
                    switch (CPU_Regs.reg_eax.low()) {
                        case 0x01: {
                            /* E801: memory size */
                            int sz = Memory.MEM_TotalPages() * 4;
                            if (sz >= 1024)
                                sz -= 1024;
                            else
                                sz = 0;
                            int t = (sz > 0x3C00) ? 0x3C00 : sz;
                            CPU_Regs.reg_eax.word(t); /* extended memory between 1MB and 16MB in KBs */
                            CPU_Regs.reg_ecx.word(t); /* extended memory between 1MB and 16MB in KBs */
                            sz -= t;
                            sz /= 64;    /* extended memory size from 16MB in 64KB blocks */
                            if (sz > 65535) sz = 65535;
                            CPU_Regs.reg_edx.word(sz);
                            CPU_Regs.reg_ebx.word(sz);
                            Callback.CALLBACK_SCF(false);
                        }
                        break;
                        case 0x20: { /* E820: MEMORY LISTING */
                            if (CPU_Regs.reg_edx.dword == 0x534D4150 && CPU_Regs.reg_ecx.dword >= 20 && (Memory.MEM_TotalPages() * 4) >= 24000) {
                                if (CPU_Regs.reg_ebx.dword<e820table.size()) {
                                    E820 e820 = e820table.elementAt(CPU_Regs.reg_ebx.dword);

                                    /* write to ES:DI */
                                    Memory.real_writed(CPU_Regs.reg_esVal.dword, CPU_Regs.reg_edi.word() + 0x00, e820.base);
                                    Memory.real_writed(CPU_Regs.reg_esVal.dword, CPU_Regs.reg_edi.word() + 0x04, 0);
                                    Memory.real_writed(CPU_Regs.reg_esVal.dword, CPU_Regs.reg_edi.word() + 0x08, e820.len);
                                    Memory.real_writed(CPU_Regs.reg_esVal.dword, CPU_Regs.reg_edi.word() + 0x0C, 0);
                                    Memory.real_writed(CPU_Regs.reg_esVal.dword, CPU_Regs.reg_edi.word() + 0x10, e820.reserved?2:1);
                                    CPU_Regs.reg_ecx.dword = 20;

                                    /* return EBX pointing to next entry. wrap around, as most BIOSes do.
                                    * the program is supposed to stop on CF=1 or when we return EBX == 0 */
                                    if (++CPU_Regs.reg_ebx.dword >= e820table.size()) CPU_Regs.reg_ebx.dword = 0;
                                } else {
                                    Callback.CALLBACK_SCF(true);
                                }
                                CPU_Regs.reg_eax.dword = 0x534D4150;
                            } else {
                                CPU_Regs.reg_eax.dword = 0x8600;
                                Callback.CALLBACK_SCF(true);
                            }
                        }
                        break;
                        default:
                            if (Log.level<=LogSeverities.LOG_ERROR) Log.log(LogTypes.LOG_BIOS,LogSeverities.LOG_ERROR,"INT15:Unknown call "+Integer.toString(CPU_Regs.reg_eax.word(),16));
                            CPU_Regs.reg_eax.high(0x86);
                            Callback.CALLBACK_SCF(true);
                            if ((Dosbox.IS_EGAVGA_ARCH()) || (Dosbox.machine == MachineType.MCH_CGA)/* || (Dosbox.machine == MachineType.MCH_AMSTRAD)*/) {
                                /* relict from comparisons, as int15 exits with a retf2 instead of an iret */
                                Callback.CALLBACK_SZF(false);
                            }
                    }
                    break;
            case 0x88:	/* SYSTEM - GET EXTENDED MEMORY SIZE (286+) */
                CPU_Regs.reg_eax.word(other_memsystems!=0?0:size_extended);
                if (Log.level<=LogSeverities.LOG_NORMAL) Log.log(LogTypes.LOG_BIOS,LogSeverities.LOG_NORMAL,"INT15:Function 0x88 Remaining "+Integer.toString(CPU_Regs.reg_eax.word(), 16)+" kb");
                Callback.CALLBACK_SCF(false);
                break;
            case 0x89:	/* SYSTEM - SWITCH TO PROTECTED MODE */
                {
                    IoHandler.IO_Write(0x20,0x10);IoHandler.IO_Write(0x21,CPU_Regs.reg_ebx.high());IoHandler.IO_Write(0x21,0);
                    IoHandler.IO_Write(0xA0,0x10);IoHandler.IO_Write(0xA1,CPU_Regs.reg_ebx.low());IoHandler.IO_Write(0xA1,0);
                    Memory.MEM_A20_Enable(true);
                    /*PhysPt*/int table=CPU_Regs.reg_esPhys.dword+CPU_Regs.reg_esi.word();
                    CPU.CPU_LGDT(Memory.mem_readw(table+0x8),Memory.mem_readd(table + 0x8 + 0x2) & 0xFFFFFF);
                    CPU.CPU_LIDT(Memory.mem_readw(table+0x10),Memory.mem_readd(table + 0x10 + 0x2) & 0xFFFFFF);
                    CPU.CPU_SET_CRX(0,CPU.CPU_GET_CRX(0)|1);
                    CPU.CPU_SetSegGeneralDS(0x18);
                    CPU.CPU_SetSegGeneralES(0x20);
                    CPU.CPU_SetSegGeneralSS(0x28);
                    CPU_Regs.reg_esp.word(CPU_Regs.reg_esp.word()+6);			//Clear stack of interrupt frame
                    CPU.CPU_SetFlags(0,CPU_Regs.FMASK_ALL);
                    CPU_Regs.reg_eax.word(0);
                    CPU.CPU_JMP(false,0x30,CPU_Regs.reg_ecx.word(),0);
                }
                break;
            case 0x90:	/* OS HOOK - DEVICE BUSY */
                Callback.CALLBACK_SCF(false);
                CPU_Regs.reg_eax.high(0);
                break;
            case 0x91:	/* OS HOOK - DEVICE POST */
                Callback.CALLBACK_SCF(false);
                CPU_Regs.reg_eax.high(0);
                break;
            case 0xc2:	/* BIOS PS2 Pointing Device Support */
                switch (CPU_Regs.reg_eax.low()) {
                case 0x00:		// enable/disable
                    if (CPU_Regs.reg_ebx.high()==0) {	// disable
                        Mouse.Mouse_SetPS2State(false);
                        CPU_Regs.reg_eax.high(0);
                        Callback.CALLBACK_SCF(false);
                    } else if (CPU_Regs.reg_ebx.high()==0x01) {	//enable
                        if (!Mouse.Mouse_SetPS2State(true)) {
                            CPU_Regs.reg_eax.high(5);
                            Callback.CALLBACK_SCF(true);
                            break;
                        }
                        CPU_Regs.reg_eax.high(0);
                        Callback.CALLBACK_SCF(false);
                    } else {
                        Callback.CALLBACK_SCF(true);
                        CPU_Regs.reg_eax.high(1);
                    }
                    break;
                case 0x01:		// reset
                    CPU_Regs.reg_ebx.word(0x00aa);	// mouse
                    // fall through
                case 0x05:		// initialize
                    Mouse.Mouse_SetPS2State(false);
                    Callback.CALLBACK_SCF(false);
                    CPU_Regs.reg_eax.high(0);
                    break;
                case 0x02:		// set sampling rate
                case 0x03:		// set resolution
                    Callback.CALLBACK_SCF(false);
                    CPU_Regs.reg_eax.high(0);
                    break;
                case 0x04:		// get type
                    CPU_Regs.reg_ebx.high(0);	// ID
                    Callback.CALLBACK_SCF(false);
                    CPU_Regs.reg_eax.high(0);
                    break;
                case 0x06:		// extended commands
                    if ((CPU_Regs.reg_ebx.high()==0x01) || (CPU_Regs.reg_ebx.high()==0x02)) {
                        Callback.CALLBACK_SCF(false);
                        CPU_Regs.reg_eax.high(0);
                    } else {
                        Callback.CALLBACK_SCF(true);
                        CPU_Regs.reg_eax.high(1);
                    }
                    break;
                case 0x07:		// set callback
                    Mouse.Mouse_ChangePS2Callback((int)CPU_Regs.reg_esVal.dword,CPU_Regs.reg_ebx.word());
                    Callback.CALLBACK_SCF(false);
                    CPU_Regs.reg_eax.high(0);
                    break;
                default:
                    Callback.CALLBACK_SCF(true);
                    CPU_Regs.reg_eax.high(1);
                    break;
                }
                break;
            case 0xc3:      /* set carry flag so BorlandRTM doesn't assume a VECTRA/PS2 */
                CPU_Regs.reg_eax.high(0x86);
                Callback.CALLBACK_SCF(true);
                break;
            case 0xc4:	/* BIOS POS Programm option Select */
                if (Log.level<=LogSeverities.LOG_NORMAL) Log.log(LogTypes.LOG_BIOS,LogSeverities.LOG_NORMAL,"INT15:Function "+Integer.toString(CPU_Regs.reg_eax.high(), 16)+" called, bios mouse not supported");
                Callback.CALLBACK_SCF(true);
                break;
            case 0x53: // APM BIOS
                switch(CPU_Regs.reg_eax.low()) {
                case 0x00: // installation check
                    CPU_Regs.reg_eax.high(1);			// APM 1.2
                    CPU_Regs.reg_eax.low(2);
                    CPU_Regs.reg_ebx.word(0x504d);	// 'PM'
                    CPU_Regs.reg_ecx.word(0);			// about no capabilities
                    // 32-bit interface seems to be needed for standby in win95
                    break;
                case 0x01: // connect real mode interface
                    if(CPU_Regs.reg_ebx.word() != 0x0) {
                        CPU_Regs.reg_eax.high(0x09);	// unrecognized device ID
                        Callback.CALLBACK_SCF(true);
                        break;
                    }
                    if(!apm_realmode_connected) { // not yet connected
                        Callback.CALLBACK_SCF(false);
                        apm_realmode_connected=true;
                    } else {
                        CPU_Regs.reg_eax.high(0x02);	// interface connection already in effect
                        Callback.CALLBACK_SCF(true);
                    }
                    break;
                case 0x04: // DISCONNECT INTERFACE
                    if(CPU_Regs.reg_ebx.word() != 0x0) {
                        CPU_Regs.reg_eax.high(0x09);	// unrecognized device ID
                        Callback.CALLBACK_SCF(true);
                        break;
                    }
                    if(apm_realmode_connected) {
                        Callback.CALLBACK_SCF(false);
                        apm_realmode_connected=false;
                    } else {
                        CPU_Regs.reg_eax.high(0x03);	// interface not connected
                        Callback.CALLBACK_SCF(true);
                    }
                    break;
                case 0x07:
                    if(CPU_Regs.reg_ebx.word() != 0x1) {
                        CPU_Regs.reg_eax.high(0x09);	// wrong device ID
                        Callback.CALLBACK_SCF(true);
                        break;
                    }
                    if(!apm_realmode_connected) {
                        CPU_Regs.reg_eax.high(0x03);
                        Callback.CALLBACK_SCF(true);
                        break;
                    }
                    switch(CPU_Regs.reg_ecx.word()) {
                    case 0x3: // power off
                        Log.exit("Power Off");
                        break;
                    default:
                        CPU_Regs.reg_eax.high(0x0A); // invalid parameter value in CX
                        Callback.CALLBACK_SCF(true);
                        break;
                    }
                    break;
                case 0x08: // ENABLE/DISABLE POWER MANAGEMENT
                    if(CPU_Regs.reg_ebx.word() != 0x0 && CPU_Regs.reg_ebx.word() != 0x1) {
                        CPU_Regs.reg_eax.high(0x09);	// unrecognized device ID
                        Callback.CALLBACK_SCF(true);
                        break;
                    } else if(!apm_realmode_connected) {
                        CPU_Regs.reg_eax.high(0x03);
                        Callback.CALLBACK_SCF(true);
                        break;
                    }
                    if(CPU_Regs.reg_ecx.word()==0x0) Log.log_msg("disable APM for device "+Integer.toString(CPU_Regs.reg_ebx.word(),16));
                    else if(CPU_Regs.reg_ecx.word()==0x1) Log.log_msg("enable APM for device "+Integer.toString(CPU_Regs.reg_ebx.word(),16));
                    else {
                        CPU_Regs.reg_eax.high(0x0A); // invalid parameter value in CX
                        Callback.CALLBACK_SCF(true);
                    }
                    break;
                case 0x0e:
                    if(CPU_Regs.reg_ebx.word() != 0x0) {
                        CPU_Regs.reg_eax.high(0x09);	// unrecognized device ID
                        Callback.CALLBACK_SCF(true);
                        break;
                    } else if(!apm_realmode_connected) {
                        CPU_Regs.reg_eax.high(0x03);	// interface not connected
                        Callback.CALLBACK_SCF(true);
                        break;
                    }
                    if(CPU_Regs.reg_eax.high() < 1) CPU_Regs.reg_eax.high(1);
                    if(CPU_Regs.reg_eax.low() < 2) CPU_Regs.reg_eax.low(2);
                    Callback.CALLBACK_SCF(false);
                    break;
                case 0x0f:
                    if(CPU_Regs.reg_ebx.word() != 0x0 && CPU_Regs.reg_ebx.word() != 0x1) {
                        CPU_Regs.reg_eax.high(0x09);	// unrecognized device ID
                        Callback.CALLBACK_SCF(true);
                        break;
                    } else if(!apm_realmode_connected) {
                        CPU_Regs.reg_eax.high(0x03);
                        Callback.CALLBACK_SCF(true);
                        break;
                    }
                    if(CPU_Regs.reg_ecx.word()==0x0) Log.log_msg("disengage APM for device "+Integer.toString(CPU_Regs.reg_ebx.word(),16));
                    else if(CPU_Regs.reg_ecx.word()==0x1) Log.log_msg("engage APM for device "+Integer.toString(CPU_Regs.reg_ebx.word(),16));
                    else {
                        CPU_Regs.reg_eax.high(0x0A); // invalid parameter value in CX
                        Callback.CALLBACK_SCF(true);
                    }
                    break;
                default:
                    if (Log.level<=LogSeverities.LOG_NORMAL) Log.log(LogTypes.LOG_BIOS,LogSeverities.LOG_NORMAL,"unknown APM BIOS call "+Integer.toString(CPU_Regs.reg_eax.word(),16));
                    break;
                }
                Callback.CALLBACK_SCF(false);
                break;
            default:
                if (Log.level<=LogSeverities.LOG_ERROR) Log.log(LogTypes.LOG_BIOS,LogSeverities.LOG_ERROR,"INT15:Unknown call "+Integer.toString(CPU_Regs.reg_eax.word(),16));
                CPU_Regs.reg_eax.high(0x86);
                Callback.CALLBACK_SCF(true);
                if ((Dosbox.IS_EGAVGA_ARCH()) || (Dosbox.machine==MachineType.MCH_CGA)) {
                    /* relict from comparisons, as int15 exits with a retf2 instead of an iret */
                    Callback.CALLBACK_SZF(false);
                }
            }
            return Callback.CBRET_NONE;
        }
    };

    private static Callback.Handler Reboot_Handler = new Callback.Handler() {
        public String getName() {
            return "Bios.Reboot_Handler";
        }
        public /*Bitu*/int call() {
            // switch to text mode, notify user (let's hope INT10 still works)
            byte[] text = "\n\n   Reboot requested, quitting now.".getBytes();
            CPU_Regs.reg_eax.word(0);
            Callback.CALLBACK_RunRealInt(0x10);
            CPU_Regs.reg_eax.high(0xe);
            CPU_Regs.reg_ebx.word(0);
            for(/*Bitu*/int i = 0; i < text.length;i++) {
                CPU_Regs.reg_eax.low(text[i]);
                Callback.CALLBACK_RunRealInt(0x10);
            }
            Log.log_msg(new String(text));
            double start = Pic.PIC_FullIndex();
            while((Pic.PIC_FullIndex()-start)<3000) Callback.CALLBACK_Idle();
            throw new Dos_programs.RebootException();
        }
    };

    static public void BIOS_ZeroExtendedSize(boolean in) {
        if(in) other_memsystems++;
        else other_memsystems--;
        if(other_memsystems < 0) other_memsystems=0;
    }

    private Callback[] callback=new Callback[11];
    public Bios(Section configuration) {
        super(configuration);
        for (int i=0;i<callback.length;i++)
            callback[i] = new Callback();                
        /* tandy DAC can be requested in tandy_sound.cpp by initializing this field */
        boolean use_tandyDAC=(Memory.real_readb(0x40,0xd4)==0xff);

        /* Clear the Bios Data Area (0x400-0x5ff, 0x600- is accounted to DOS) */
        for (/*Bit16u*/int i=0;i<0x200;i++) Memory.real_writeb(0x40,i,0);

        /* Setup all the interrupt handlers the bios controls */

        /* INT 8 Clock IRQ Handler */
        /*Bitu*/int call_irq0=Callback.CALLBACK_Allocate();
        Callback.CALLBACK_Setup(call_irq0,INT8_Handler,Callback.CB_IRQ0,Memory.Real2Phys(BIOS_DEFAULT_IRQ0_LOCATION()),"IRQ 0 Clock");
        Memory.RealSetVec(0x08,BIOS_DEFAULT_IRQ0_LOCATION());
        // pseudocode for CB_IRQ0:
        //	callback INT8_Handler
        //	push ax,dx,ds
        //	int 0x1c
        //	cli
        //	pop ds,dx
        //	mov al, 0x20
        //	out 0x20, al
        //	pop ax
        //	iret

        Memory.mem_writed(BIOS_TIMER,0);			//Calculate the correct time

        /* INT 11 Get equipment list */
        callback[1].Install(INT11_Handler,Callback.CB_IRET,"Int 11 Equipment");
        callback[1].Set_RealVec(0x11);

        /* INT 12 Memory Size default at 640 kb */
        callback[2].Install(INT12_Handler,Callback.CB_IRET,"Int 12 Memory");
        callback[2].Set_RealVec(0x12);
        if (Dosbox.IS_TANDY_ARCH()) {
            /* reduce reported memory size for the Tandy (32k graphics memory
               at the end of the conventional 640k) */
            if (Dosbox.machine==MachineType.MCH_TANDY) Memory.mem_writew(BIOS_MEMORY_SIZE,624);
            else Memory.mem_writew(BIOS_MEMORY_SIZE,640);
            Memory.mem_writew(BIOS_TRUE_MEMORY_SIZE,640);
        } else Memory.mem_writew(BIOS_MEMORY_SIZE,640);

        /* INT 13 Bios Disk Support */
        Bios_disk.BIOS_SetupDisks();

        /* INT 14 Serial Ports */
        callback[3].Install(INT14_Handler,Callback.CB_IRET_STI,"Int 14 COM-port");
        callback[3].Set_RealVec(0x14);

        /* INT 15 Misc Calls */
        callback[4].Install(INT15_Handler,Callback.CB_IRET,"Int 15 Bios");
        callback[4].Set_RealVec(0x15);

        /* INT 16 Keyboard handled in another file */
        Bios_keyboard.BIOS_SetupKeyboard();

        /* INT 17 Printer Routines */
        callback[5].Install(INT17_Handler,Callback.CB_IRET_STI,"Int 17 Printer");
        callback[5].Set_RealVec(0x17);

        /* INT 1A TIME and some other functions */
        callback[6].Install(INT1A_Handler,Callback.CB_IRET_STI,"Int 1a Time");
        callback[6].Set_RealVec(0x1A);

        /* INT 1C System Timer tick called from INT 8 */
        callback[7].Install(INT1C_Handler,Callback.CB_IRET,"Int 1c Timer");
        callback[7].Set_RealVec(0x1C);

        /* IRQ 8 RTC Handler */
        callback[8].Install(INT70_Handler,Callback.CB_IRET,"Int 70 RTC");
        callback[8].Set_RealVec(0x70);

        /* Irq 9 rerouted to irq 2 */
        callback[9].Install(null,Callback.CB_IRQ9,"irq 9 bios");
        callback[9].Set_RealVec(0x71);

        /* Reboot */
        // This handler is an exit for more than only reboots, since we
		// don't handle these cases
        callback[10].Install(Reboot_Handler,Callback.CB_IRET,"reboot");

        // INT 18h: Enter BASIC
		// Non-IBM BIOS would display "NO ROM BASIC" here
        callback[10].Set_RealVec(0x18);
        /*RealPt*/int rptr = callback[10].Get_RealPointer();

        // INT 19h: Boot function
		// This is not a complete reboot as it happens after the POST
		// We don't handle it, so use the reboot function as exit.
        Memory.RealSetVec(0x19,rptr);

		// The farjump at the processor reset entry point (jumps to POST routine)
		Memory.phys_writeb(0xFFFF0,0xEA);		// FARJMP
		Memory.phys_writew(0xFFFF1,Memory.RealOff(BIOS_DEFAULT_RESET_LOCATION()));	// offset
		Memory.phys_writew(0xFFFF3,Memory.RealSeg(BIOS_DEFAULT_RESET_LOCATION()));	// segment

        // Compatible POST routine location: jump to the callback
		Memory.phys_writeb(Memory.Real2Phys(BIOS_DEFAULT_RESET_LOCATION())+0,0xEA);				// FARJMP
		Memory.phys_writew(Memory.Real2Phys(BIOS_DEFAULT_RESET_LOCATION())+1,Memory.RealOff(rptr));	// offset
		Memory.phys_writew(Memory.Real2Phys(BIOS_DEFAULT_RESET_LOCATION())+3,Memory.RealSeg(rptr));	// segment

        /* Irq 2 */
        /*Bitu*/int call_irq2=Callback.CALLBACK_Allocate();
        Callback.CALLBACK_Setup(call_irq2,null,Callback.CB_IRET_EOI_PIC1,Memory.Real2Phys(BIOS_DEFAULT_IRQ2_LOCATION()),"irq 2 bios");
        Memory.RealSetVec(0x0a,BIOS_DEFAULT_IRQ2_LOCATION());

        /* Some hardcoded vectors */
        Memory.phys_writeb((int)Memory.Real2Phys(BIOS_DEFAULT_HANDLER_LOCATION()),0xcf);	/* bios default interrupt vector location . IRET */
        Memory.phys_writew((int)Memory.Real2Phys(Memory.RealGetVec(0x12))+0x12,0x20); //Hack for Jurresic

        if (Dosbox.machine==MachineType.MCH_TANDY) Memory.phys_writeb(0xffffe,0xff)	;	/* Tandy model */
        else if (Dosbox.machine==MachineType.MCH_PCJR) Memory.phys_writeb(0xffffe,0xfd);	/* PCJr model */
        else Memory.phys_writeb(0xffffe,0xfc);	/* PC */

        // System BIOS identification
        byte[] b_type =
            "IBM COMPATIBLE 486 BIOS COPYRIGHT The DOSBox Team.".getBytes();
        for(/*Bitu*/int i = 0; i < b_type.length; i++) Memory.phys_writeb(0xfe00e + i,b_type[i]);

        // System BIOS version
        byte[] b_vers =
            "DOSBox FakeBIOS v1.0".getBytes();
        for(/*Bitu*/int i = 0; i < b_vers.length; i++) Memory.phys_writeb(0xfe061+i,b_vers[i]);

        // write system BIOS date
        byte[] b_date = "01/01/92".getBytes();
        for(/*Bitu*/int i = 0; i < b_date.length; i++) Memory.phys_writeb(0xffff5+i,b_date[i]);
        Memory.phys_writeb(0xfffff,0x55); // signature

        byte[] ident = "ISA".getBytes();
        for(/*Bitu*/int i = 0; i < ident.length; i++) Memory.phys_writeb(0xfffd9+i,ident[i]);

        tandy_sb.port=0;
        tandy_dac.port=0;
        if (use_tandyDAC) {
            /* tandy DAC sound requested, see if soundblaster device is available */
            /*Bitu*/int tandy_dac_type = 0;
            if (Tandy_InitializeSB()) {
                tandy_dac_type = 1;
            } else if (Tandy_InitializeTS()) {
                tandy_dac_type = 2;
            }
            if (tandy_dac_type!=0) {
                Memory.real_writew(0x40,0xd0,0x0000);
                Memory.real_writew(0x40,0xd2,0x0000);
                Memory.real_writeb(0x40,0xd4,0xff);	/* tandy DAC init value */
                Memory.real_writed(0x40,0xd6,0x00000000);
                /* install the DAC callback handler */
                tandy_DAC_callback[0]=new Callback();
                tandy_DAC_callback[1]=new Callback();
                tandy_DAC_callback[0].Install(IRQ_TandyDAC,Callback.CB_IRET,"Tandy DAC IRQ");
                tandy_DAC_callback[1].Install(null,Callback.CB_TDE_IRET,"Tandy DAC end transfer");
                // pseudocode for CB_TDE_IRET:
                //	push ax
                //	mov ax, 0x91fb
                //	int 15
                //	cli
                //	mov al, 0x20
                //	out 0x20, al
                //	pop ax
                //	iret

                /*Bit8u*/short tandy_irq = 7;
                if (tandy_dac_type==1) tandy_irq = tandy_sb.irq;
                else if (tandy_dac_type==2) tandy_irq = tandy_dac.irq;
                /*Bit8u*/short tandy_irq_vector = tandy_irq;
                if (tandy_irq_vector<8) tandy_irq_vector += 8;
                else tandy_irq_vector += (0x70-8);

                /*RealPt*/int current_irq=Memory.RealGetVec(tandy_irq_vector);
                Memory.real_writed(0x40,0xd6,current_irq);
                for (/*Bit16u*/int i=0; i<0x10; i++) Memory.phys_writeb((int)Memory.PhysMake(0xf000,0xa084+i),0x80);
            } else Memory.real_writeb(0x40,0xd4,0x00);
        }

        /* Setup some stuff in 0x40 bios segment */

        // port timeouts
        // always 1 second even if the port does not exist
        Memory.mem_writeb(BIOS_LPT1_TIMEOUT,1);
        Memory.mem_writeb(BIOS_LPT2_TIMEOUT,1);
        Memory.mem_writeb(BIOS_LPT3_TIMEOUT,1);
        Memory.mem_writeb(BIOS_COM1_TIMEOUT,1);
        Memory.mem_writeb(BIOS_COM2_TIMEOUT,1);
        Memory.mem_writeb(BIOS_COM3_TIMEOUT,1);
        Memory.mem_writeb(BIOS_COM4_TIMEOUT,1);

        /* detect parallel ports */
        /*Bitu*/int ppindex=0; // number of lpt ports
        if ((IoHandler.IO_Read(0x378)!=0xff)|(IoHandler.IO_Read(0x379)!=0xff)) {
            // this is our LPT1
            Memory.mem_writew(BIOS_ADDRESS_LPT1,0x378);
            ppindex++;
            if((IoHandler.IO_Read(0x278)!=0xff)|(IoHandler.IO_Read(0x279)!=0xff)) {
                // this is our LPT2
                Memory.mem_writew(BIOS_ADDRESS_LPT2,0x278);
                ppindex++;
                if((IoHandler.IO_Read(0x3bc)!=0xff)|(IoHandler.IO_Read(0x3be)!=0xff)) {
                    // this is our LPT3
                    Memory.mem_writew(BIOS_ADDRESS_LPT3,0x3bc);
                    ppindex++;
                }
            } else if((IoHandler.IO_Read(0x3bc)!=0xff)|(IoHandler.IO_Read(0x3be)!=0xff)) {
                // this is our LPT2
                Memory.mem_writew(BIOS_ADDRESS_LPT2,0x3bc);
                ppindex++;
            }
        } else if((IoHandler.IO_Read(0x3bc)!=0xff)|(IoHandler.IO_Read(0x3be)!=0xff)) {
            // this is our LPT1
            Memory.mem_writew(BIOS_ADDRESS_LPT1,0x3bc);
            ppindex++;
            if((IoHandler.IO_Read(0x278)!=0xff)|(IoHandler.IO_Read(0x279)!=0xff)) {
                // this is our LPT2
                Memory.mem_writew(BIOS_ADDRESS_LPT2,0x278);
                ppindex++;
            }
        } else if((IoHandler.IO_Read(0x278)!=0xff)|(IoHandler.IO_Read(0x279)!=0xff)) {
            // this is our LPT1
            Memory.mem_writew(BIOS_ADDRESS_LPT1,0x278);
            ppindex++;
        }

        /* Setup equipment list */
        // look http://www.bioscentral.com/misc/bda.htm

        ///*Bit16u*/int config=0x4400;	//1 Floppy, 2 serial and 1 parallel
        /*Bit16u*/int config = 0x0;

        // set number of parallel ports
        // if(ppindex == 0) config |= 0x8000; // looks like 0 ports are not specified
        //else if(ppindex == 1) config |= 0x0000;
        if(ppindex == 2) config |= 0x4000;
        else config |= 0xc000;	// 3 ports
        if (Config.C_FPU) {
                //FPU
                config|=0x2;
        }
        switch (Dosbox.machine) {
        case MachineType.MCH_HERC:
            //Startup monochrome
            config|=0x30;
            break;
        // EGAVGA_ARCH_CASE
        case MachineType.MCH_EGA:
        case MachineType.MCH_VGA:
        case MachineType.MCH_CGA:
        case MachineType.MCH_TANDY:
	    case MachineType.MCH_PCJR: //TANDY_ARCH_CASE:
            //Startup 80x25 color
            config|=0x20;
            break;
        default:
            //EGA VGA
            config|=0;
            break;
        }
        // PS2 mouse
        config |= 0x04;
        // Gameport
        config |= 0x1000;
        Memory.mem_writew(BIOS_CONFIGURATION,config);
        Cmos.CMOS_SetRegister(0x14,(/*Bit8u*/short)(config&0xff)); //Should be updated on changes
        /* Setup extended memory size */
        IoHandler.IO_Write(0x70,0x30);
        size_extended=IoHandler.IO_Read(0x71);
        IoHandler.IO_Write(0x70,0x31);
        size_extended|=(IoHandler.IO_Read(0x71) << 8);
        BIOS_HostTimeSync();
    }

    private void destroy(){
        /* abort DAC playing */
        if (tandy_sb.port!=0) {
            IoHandler.IO_Write(tandy_sb.port+0xc,0xd3);
            IoHandler.IO_Write(tandy_sb.port+0xc,0xd0);
        }
        Memory.real_writeb(0x40,0xd4,0x00);
        if (tandy_DAC_callback[0]!=null) {
            /*Bit32u*/long orig_vector=Memory.real_readd(0x40,0xd6);
            if (orig_vector==tandy_DAC_callback[0].Get_RealPointer()) {
                /* set IRQ vector to old value */
                /*Bit8u*/short tandy_irq = 7;
                if (tandy_sb.port!=0) tandy_irq = tandy_sb.irq;
                else if (tandy_dac.port!=0) tandy_irq = tandy_dac.irq;
                /*Bit8u*/short tandy_irq_vector = tandy_irq;
                if (tandy_irq_vector<8) tandy_irq_vector += 8;
                else tandy_irq_vector += (0x70-8);

                Memory.RealSetVec(tandy_irq_vector,Memory.real_readd(0x40,0xd6));
                Memory.real_writed(0x40,0xd6,0x00000000);
            }
            tandy_DAC_callback[0] = null;
            tandy_DAC_callback[1] = null;
        }
        Bios_disk.BIOS_CloseDisks();
    }

    // set com port data in bios data area
    // parameter: array of 4 com port base addresses, 0 = none
    static public void BIOS_SetComPorts(/*Bit16u*/int baseaddr[]) {
        /*Bit16u*/int portcount=0;
        /*Bit16u*/int equipmentword;
        for(/*Bitu*/int i = 0; i < 4; i++) {
            if(baseaddr[i]!=0) portcount++;
            if(i==0)		Memory.mem_writew(BIOS_BASE_ADDRESS_COM1,baseaddr[i]);
            else if(i==1)	Memory.mem_writew(BIOS_BASE_ADDRESS_COM2,baseaddr[i]);
            else if(i==2)	Memory.mem_writew(BIOS_BASE_ADDRESS_COM3,baseaddr[i]);
            else			Memory.mem_writew(BIOS_BASE_ADDRESS_COM4,baseaddr[i]);
        }
        // set equipment word
        equipmentword = Memory.mem_readw(BIOS_CONFIGURATION);
        equipmentword &= (~0x0E00);
        equipmentword |= (portcount << 9);
        Memory.mem_writew(BIOS_CONFIGURATION,equipmentword);
        Cmos.CMOS_SetRegister(0x14,(/*Bit8u*/short)(equipmentword&0xff)); //Should be updated on changes
    }


    static Bios test;

    public static Section.SectionFunction BIOS_Destroy = new Section.SectionFunction() {
        public void call(Section section) {
            test.destroy();
            test = null;
            tandy_dac = null;
            tandy_sb = null;
        }
    };

    public static Section.SectionFunction BIOS_Init = new Section.SectionFunction() {
        public void call(Section section) {
            biosConfigSeg = 0;
            tandy_dac = new Tandy_dac();
            tandy_sb = new Tandy_sb();
            test = new Bios(section);
            section.AddDestroyFunction(BIOS_Destroy,false);
        }
    };
}
