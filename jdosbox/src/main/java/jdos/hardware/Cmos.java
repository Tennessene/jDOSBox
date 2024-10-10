package jdos.hardware;

import jdos.ints.Bios;
import jdos.ints.Bios_disk;
import jdos.misc.Log;
import jdos.misc.setup.Module_base;
import jdos.misc.setup.Section;
import jdos.types.LogSeverities;
import jdos.types.LogTypes;
import jdos.util.StringHelper;

import java.util.Calendar;

public class Cmos extends Module_base {
    static private class CMOS {
        public byte[] regs = new byte[0x40];
        boolean nmi;
        boolean bcd;
        /*Bit8u*/byte reg;
        public static class Timer {
            boolean enabled;
            /*Bit8u*/byte div;
            float delay;
            boolean acknowledged;
        }
        public Timer timer = new Timer();
        public static class Last {
            double timer;
            double ended;
            double alarm;
        }
        public Last last = new Last();
        boolean update_ended;
    }
    static private CMOS cmos = new CMOS();

    static private Pic.PIC_EventHandler cmos_timerevent = new Pic.PIC_EventHandler() {
        public void call(/*Bitu*/int val) {
            if (cmos.timer.acknowledged) {
                cmos.timer.acknowledged=false;
                Pic.PIC_ActivateIRQ(8);
            }
            if (cmos.timer.enabled) {
                Pic.PIC_AddEvent(cmos_timerevent,cmos.timer.delay);
                cmos.regs[0xc] = (byte)0xC0;//Contraption Zack (music)
            }
        }
        public String toString() {
            return "cmos_timerevent";
        }
    };

    static private void cmos_checktimer() {
        Pic.PIC_RemoveEvents(cmos_timerevent);
        if (cmos.timer.div<=2) cmos.timer.div+=7;
        cmos.timer.delay=(1000.0f/(32768.0f / (1 << (cmos.timer.div - 1))));
        if (cmos.timer.div==0 || !cmos.timer.enabled) return;
        if (Log.level<=LogSeverities.LOG_NORMAL) Log.log(LogTypes.LOG_PIT, LogSeverities.LOG_NORMAL,"RTC Timer at "+ StringHelper.format(1000.0/cmos.timer.delay, 2)+" hz");
    //	PIC_AddEvent(cmos_timerevent,cmos.timer.delay);
        /* A rtc is always running */
        double remd=Pic.PIC_FullIndex() % (double)cmos.timer.delay;
        Pic.PIC_AddEvent(cmos_timerevent,(float)((double)cmos.timer.delay-remd)); //Should be more like a real pc. Check
    //	status reg A reading with this (and with other delays actually)
    }

    static private IoHandler.IO_WriteHandler cmos_selreg = new IoHandler.IO_WriteHandler() {
        public void call(/*Bitu*/int port, /*Bitu*/int val, /*Bitu*/int iolen) {
            cmos.reg=(byte)(val & 0x3f);
            cmos.nmi=(val & 0x80)>0;
        }
    };

    static private IoHandler.IO_WriteHandler cmos_writereg = new IoHandler.IO_WriteHandler() {
        public void call(/*Bitu*/int port, /*Bitu*/int val, /*Bitu*/int iolen) {
            switch (cmos.reg) {
            case 0x00:		/* Seconds */
            case 0x02:		/* Minutes */
            case 0x04:		/* Hours */
            case 0x06:		/* Day of week */
            case 0x07:		/* Date of month */
            case 0x08:		/* Month */
            case 0x09:		/* Year */
            case 0x32:              /* Century */
                /* Ignore writes to change alarm */
                break;
            case 0x01:		/* Seconds Alarm */
            case 0x03:		/* Minutes Alarm */
            case 0x05:		/* Hours Alarm */
                Log.log(LogTypes.LOG_BIOS,LogSeverities.LOG_NORMAL,"CMOS:Trying to set alarm");
                cmos.regs[cmos.reg]=(byte)val;
                break;
            case 0x0a:		/* Status reg A */
                cmos.regs[cmos.reg]=(byte)(val & 0x7f);
                if ((val & 0x70)!=0x20) Log.log(LogTypes.LOG_BIOS,LogSeverities.LOG_ERROR,"CMOS Illegal 22 stage divider value");
                cmos.timer.div=(byte)(val & 0xf);
                cmos_checktimer();
                break;
            case 0x0b:		/* Status reg B */
                cmos.bcd=!((val & 0x4)!=0);
                cmos.regs[cmos.reg]=(byte)(val & 0x7f);
                cmos.timer.enabled=(val & 0x40)>0;
                if ((val&0x10)!=0) Log.log(LogTypes.LOG_BIOS,LogSeverities.LOG_ERROR,"CMOS:Updated ended interrupt not supported yet");
                cmos_checktimer();
                break;
            case 0x0d:/* Status reg D */
                cmos.regs[cmos.reg]=(byte)(val & 0x80);	/*Bit 7=1:RTC Pown on*/
                break;
            case 0x0f:		/* Shutdown status byte */
                cmos.regs[cmos.reg]=(byte)(val & 0x7f);
                break;
            default:
                cmos.regs[cmos.reg]=(byte)(val & 0x7f);
                if (Log.level<=LogSeverities.LOG_ERROR) Log.log(LogTypes.LOG_BIOS,LogSeverities.LOG_ERROR,"CMOS:WRite to unhandled register "+Integer.toString(cmos.reg,16));
            }
        }
    };

    static private int MAKE_RETURN(int _VAL) {
        return (cmos.bcd ? ((((_VAL) / 10) << 4) | ((_VAL) % 10)) : (_VAL));
    }

    static private IoHandler.IO_ReadHandler cmos_readreg = new IoHandler.IO_ReadHandler() {
        public /*Bitu*/int call(/*Bitu*/int port, /*Bitu*/int iolen) {
            if (cmos.reg>0x3f) {
                if (Log.level<=LogSeverities.LOG_ERROR) Log.log(LogTypes.LOG_BIOS,LogSeverities.LOG_ERROR,"CMOS:Read from illegal register "+Integer.toString(cmos.reg,16));
                return 0xff;
            }
            /*Bitu*/int drive_a, drive_b;
            /*Bit8u*/short hdparm;

            Calendar c = Calendar.getInstance();
            switch (cmos.reg) {
            case 0x00:		/* Seconds */
                return 	MAKE_RETURN(c.get(Calendar.SECOND));
            case 0x02:		/* Minutes */
                return 	MAKE_RETURN(c.get(Calendar.MINUTE));
            case 0x04:		/* Hours */
                return 	MAKE_RETURN(c.get(Calendar.HOUR));
            case 0x06:		/* Day of week */
                return 	MAKE_RETURN(c.get(Calendar.DAY_OF_WEEK));
            case 0x07:		/* Date of month */
                return 	MAKE_RETURN(c.get(Calendar.DAY_OF_MONTH));
            case 0x08:		/* Month */
                return 	MAKE_RETURN(c.get(Calendar.MONTH) + 1);
            case 0x09:		/* Year */
                return 	MAKE_RETURN(c.get(Calendar.YEAR) % 100);
            case 0x32:		/* Century */
                return 	MAKE_RETURN(c.get(Calendar.YEAR) / 100);
            case 0x01:		/* Seconds Alarm */
            case 0x03:		/* Minutes Alarm */
            case 0x05:		/* Hours Alarm */
                return cmos.regs[cmos.reg];
            case 0x0a:		/* Status register A */
                if (Pic.PIC_TickIndex()<0.002) {
                    return (cmos.regs[0x0a]&0x7f) | 0x80;
                } else {
                    return (cmos.regs[0x0a]&0x7f);
                }
            case 0x0c:		/* Status register C */
                cmos.timer.acknowledged=true;
                if (cmos.timer.enabled) {
                    /* In periodic interrupt mode only care for those flags */
                    /*Bit8u*/byte val=cmos.regs[0xc];
                    cmos.regs[0xc]=0;
                    return val;
                } else {
                    /* Give correct values at certain times */
                    /*Bit8u*/byte val=0;
                    double index=Pic.PIC_FullIndex();
                    if (index>=(cmos.last.timer+cmos.timer.delay)) {
                        cmos.last.timer=index;
                        val|=0x40;
                    }
                    if (index>=(cmos.last.ended+1000)) {
                        cmos.last.ended=index;
                        val|=0x10;
                    }
                    return val;
                }
            case 0x10:		/* Floppy size */
                drive_a = 0;
                drive_b = 0;
                if(Bios_disk.imageDiskList[0] != null) drive_a = Bios_disk.imageDiskList[0].GetBiosType();
                if(Bios_disk.imageDiskList[1] != null) drive_b = Bios_disk.imageDiskList[1].GetBiosType();
                return ((drive_a << 4) | (drive_b));
            /* First harddrive info */
            case 0x12:
                if (Bios.boot)
                    return cmos.regs[cmos.reg] & 0xFF;
                hdparm = 0;
                if(Bios_disk.imageDiskList[3] != null) hdparm |= 0xf;
                if(Bios_disk.imageDiskList[2] != null) hdparm |= 0xf0;
                return hdparm;
            case 0x19:
                if (Bios.boot)
                    return cmos.regs[cmos.reg] & 0xFF;
                if(Bios_disk.imageDiskList[2] != null) return 47; /* User defined type */
                return 0;
            case 0x1b:
                if (Bios.boot)
                    return cmos.regs[cmos.reg] & 0xFF;
                if(Bios_disk.imageDiskList[2] != null) return (int)(Bios_disk.imageDiskList[2].cylinders & 0xff);
                return 0;
            case 0x1c:
                if (Bios.boot)
                    return cmos.regs[cmos.reg] & 0xFF;
                if(Bios_disk.imageDiskList[2] != null) return (int)((Bios_disk.imageDiskList[2].cylinders & 0xff00)>>8);
                return 0;
            case 0x1d:
                if (Bios.boot)
                    return cmos.regs[cmos.reg] & 0xFF;
                if(Bios_disk.imageDiskList[2] != null) return (int)(Bios_disk.imageDiskList[2].heads);
                return 0;
            case 0x1e:
                if (Bios.boot)
                    return cmos.regs[cmos.reg] & 0xFF;
                if(Bios_disk.imageDiskList[2] != null) return 0xff;
                return 0;
            case 0x1f:
                if (Bios.boot)
                    return cmos.regs[cmos.reg] & 0xFF;
                if(Bios_disk.imageDiskList[2] != null) return 0xff;
                return 0;
            case 0x20:
                if (Bios.boot)
                    return cmos.regs[cmos.reg] & 0xFF;
                if(Bios_disk.imageDiskList[2] != null) return (int)(0xc0 | ((((Bios_disk.imageDiskList[2].heads) > 8)?1:0) << 3));
                return 0;
            case 0x21:
                if (Bios.boot)
                    return cmos.regs[cmos.reg] & 0xFF;
                if(Bios_disk.imageDiskList[2] != null) return (int)(Bios_disk.imageDiskList[2].cylinders & 0xff);
                return 0;
            case 0x22:
                if (Bios.boot)
                    return cmos.regs[cmos.reg] & 0xFF;
                if(Bios_disk.imageDiskList[2] != null) return (int)((Bios_disk.imageDiskList[2].cylinders & 0xff00)>>8);
                return 0;
            case 0x23:
                if (Bios.boot)
                    return cmos.regs[cmos.reg] & 0xFF;
                if(Bios_disk.imageDiskList[2] != null) return (int)(Bios_disk.imageDiskList[2].sectors);
                return 0;
            /* Second harddrive info */
            case 0x1a:
                if (Bios.boot)
                    return cmos.regs[cmos.reg] & 0xFF;
                if(Bios_disk.imageDiskList[3] != null) return 47; /* User defined type */
                return 0;
            case 0x24:
                if (Bios.boot)
                    return cmos.regs[cmos.reg] & 0xFF;
                if(Bios_disk.imageDiskList[3] != null) return (int)(Bios_disk.imageDiskList[3].cylinders & 0xff);
                return 0;
            case 0x25:
                if (Bios.boot)
                    return cmos.regs[cmos.reg] & 0xFF;
                if(Bios_disk.imageDiskList[3] != null) return (int)((Bios_disk.imageDiskList[3].cylinders & 0xff00)>>8);
                return 0;
            case 0x26:
                if (Bios.boot)
                    return cmos.regs[cmos.reg] & 0xFF;
                if(Bios_disk.imageDiskList[3] != null) return (int)(Bios_disk.imageDiskList[3].heads);
                return 0;
            case 0x27:
                if (Bios.boot)
                    return cmos.regs[cmos.reg] & 0xFF;
                if(Bios_disk.imageDiskList[3] != null) return 0xff;
                return 0;
            case 0x28:
                if (Bios.boot)
                    return cmos.regs[cmos.reg] & 0xFF;
                if(Bios_disk.imageDiskList[3] != null) return 0xff;
                return 0;
            case 0x29:
                if (Bios.boot)
                    return cmos.regs[cmos.reg] & 0xFF;
                if(Bios_disk.imageDiskList[3] != null) return (int)(0xc0 | (((Bios_disk.imageDiskList[3].heads) > 8)?1:0 << 3));
                return 0;
            case 0x2a:
                if (Bios.boot)
                    return cmos.regs[cmos.reg] & 0xFF;
                if(Bios_disk.imageDiskList[3] != null) return (int)(Bios_disk.imageDiskList[3].cylinders & 0xff);
                return 0;
            case 0x2b:
                if (Bios.boot)
                    return cmos.regs[cmos.reg] & 0xFF;
                if(Bios_disk.imageDiskList[3] != null) return (int)((Bios_disk.imageDiskList[3].cylinders & 0xff00)>>8);
                return 0;
            case 0x2c:
                if (Bios.boot)
                    return cmos.regs[cmos.reg] & 0xFF;
                if(Bios_disk.imageDiskList[3] != null) return (int)(Bios_disk.imageDiskList[3].sectors);
                return 0;
            case 0x39:
                if (Bios.boot)
                    return cmos.regs[cmos.reg] & 0xFF;
                return 0;
            case 0x3a:
                return 0;
            case 0x37: // Password Seed and Color Option
                return 0x20;
            case 0x3d:      /* boot order */
                return cmos.regs[cmos.reg] & 0xFF;
            case 0x34:      /* extended memory over 64MB */
                return cmos.regs[cmos.reg] & 0xFF;
            case 0x35:      /* extended memory over 64MB */
                return cmos.regs[cmos.reg] & 0xFF;
            case 0x0b:		/* Status register B */
            case 0x0d:		/* Status register D */
            case 0x0f:		/* Shutdown status byte */
            case 0x14:		/* Equipment */
            case 0x15:		/* Base Memory KB Low Byte */
            case 0x16:		/* Base Memory KB High Byte */
            case 0x17:		/* Extended memory in KB Low Byte */
            case 0x18:		/* Extended memory in KB High Byte */
            case 0x30:		/* Extended memory in KB Low Byte */
            case 0x31:		/* Extended memory in KB High Byte */
        //		Log.log(LogTypes.LOG_BIOS,LogSeverities.LOG_NORMAL,"CMOS:Read from reg %X : %04X",cmos.reg,cmos.regs[cmos.reg]);
                return cmos.regs[cmos.reg] & 0xFF;
            default:
                if (Log.level<=LogSeverities.LOG_NORMAL) Log.log(LogTypes.LOG_BIOS,LogSeverities.LOG_NORMAL,"CMOS:Read from reg "+Integer.toString(cmos.reg,16));
                return cmos.regs[cmos.reg];
            }
        }
    };

    static public void CMOS_SetRegister(/*Bitu*/int regNr, /*Bit8u*/int val) {
        cmos.regs[regNr] = (byte)val;
    }


    private static IoHandler.IO_ReadHandleObject[] ReadHandler=new IoHandler.IO_ReadHandleObject[2];
	private static IoHandler.IO_WriteHandleObject[] WriteHandler=new IoHandler.IO_WriteHandleObject[2];

    public Cmos(Section configuration) {
        super(configuration);
        WriteHandler[0].Install(0x70,cmos_selreg,IoHandler.IO_MB);
		WriteHandler[1].Install(0x71,cmos_writereg,IoHandler.IO_MB);
		ReadHandler[0].Install(0x71,cmos_readreg,IoHandler.IO_MB);
		cmos.timer.enabled=false;
		cmos.timer.acknowledged=true;
		cmos.reg=0xa;
		cmos_writereg.call(0x71,0x26,1);
		cmos.reg=0xb;
		cmos_writereg.call(0x71,0x2,1);	//Struct tm *loctime is of 24 hour format,
		cmos.reg=0xd;
		cmos_writereg.call(0x71,0x80,1); /* RTC power on */
		// Equipment is updated from bios.cpp and bios_disk.cpp
		/* Fill in base memory size, it is 640K always */
		cmos.regs[0x15]=(byte)0x80;
		cmos.regs[0x16]=(byte)0x02;
		/* Fill in extended memory size */
		/*Bitu*/int exsize=(Memory.MEM_TotalPages()*4)-1024;
        if (exsize>65535)
            exsize = 65535;
		cmos.regs[0x17]=(byte)exsize;
		cmos.regs[0x18]=(byte)(exsize >> 8);
		cmos.regs[0x30]=(byte)exsize;
		cmos.regs[0x31]=(byte)(exsize >> 8);

        int val = 0;
        int ramSize = (Memory.MEM_TotalPages()*4096);
        if (ramSize > (16 * 1024 * 1024))
            val = (ramSize / 65536) - ((16 * 1024 * 1024) / 65536);
        if (val > 65535)
            val = 65535;

        cmos.regs[0x34]= (byte)val;
        cmos.regs[0x35]= (byte)(val >> 8);
    }

    static Cmos test;

    private static Section.SectionFunction CMOS_Destroy = new Section.SectionFunction() {
        public void call(Section section) {
            test = null;
            for (int i=0;i<ReadHandler.length;i++)
                ReadHandler[i] = null;
            for (int i=0;i<WriteHandler.length;i++)
                WriteHandler[i] = null;
        }
    };

    public static Section.SectionFunction CMOS_Init = new Section.SectionFunction() {
        public void call(Section section) {
            for (int i=0;i<ReadHandler.length;i++)
                ReadHandler[i] = new IoHandler.IO_ReadHandleObject();
            for (int i=0;i<WriteHandler.length;i++)
                WriteHandler[i]=new IoHandler.IO_WriteHandleObject();
            test = new Cmos(section);
	        section.AddDestroyFunction(CMOS_Destroy,true);
        }
    };
}
