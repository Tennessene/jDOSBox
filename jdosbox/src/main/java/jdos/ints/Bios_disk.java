package jdos.ints;

import jdos.Dosbox;
import jdos.cpu.CPU;
import jdos.cpu.CPU_Regs;
import jdos.cpu.Callback;
import jdos.dos.Dos_DTA;
import jdos.dos.Dos_files;
import jdos.dos.DriveManager;
import jdos.gui.Mapper;
import jdos.hardware.Cmos;
import jdos.hardware.Memory;
import jdos.misc.Log;
import jdos.sdl.JavaMapper;
import jdos.types.LogSeverities;
import jdos.types.LogTypes;
import jdos.types.MachineType;
import jdos.util.FileIO;
import jdos.util.LongRef;

public class Bios_disk {
    /* The Section handling Bios Disk Access */
    static public final int BIOS_MAX_DISK = 10;

    static public final int MAX_SWAPPABLE_DISKS = 20;

    static public class diskGeo {
        public diskGeo(long ksize, int secttrack, int headscyl, int cylcount, int biosval) {
            this.ksize = ksize;
            this.secttrack = secttrack;
            this.headscyl = headscyl;
            this.cylcount = cylcount;
            this.biosval = biosval;
        }
        public /*Bit32u*/long ksize;  /* Size in kilobytes */
        public /*Bit16u*/int secttrack; /* Sectors per track */
        public /*Bit16u*/int headscyl;  /* Heads per cylinder */
        public /*Bit16u*/int cylcount;  /* Cylinders per side */
        public /*Bit16u*/int biosval;   /* Type to return from BIOS */
    }

    static public class imageDisk  {
        public boolean hardDrive;
        public boolean active;
        public FileIO diskimg;
        public String diskname;
        public /*Bit8u*/short floppytype;

        public /*Bit32u*/long sector_size;
        public /*Bit32u*/long heads,cylinders,sectors;

        public void close() {
            if (diskimg != null) {
                try {
                    diskimg.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        public /*Bit8u*/short Read_Sector(/*Bit32u*/long head,/*Bit32u*/long cylinder,/*Bit32u*/long sector,byte[] data) {
            /*Bit32u*/long sectnum;

            sectnum = ( (cylinder * heads + head) * sectors ) + sector - 1L;

            return Read_AbsoluteSector(sectnum, data, 0);
        }

        public /*Bit8u*/short Read_AbsoluteSector(/*Bit32u*/long sectnum, byte[] data, int off) {
            /*Bit32u*/long bytenum;

            bytenum = sectnum * sector_size;

            try {
                diskimg.seek(bytenum);
	            /*size_t*/int ret=diskimg.read(data, off, (int)sector_size);
            } catch (Exception e) {
                e.printStackTrace();
            }
            return 0x00;
        }

        public /*Bit8u*/short Write_Sector(/*Bit32u*/long head,/*Bit32u*/long cylinder,/*Bit32u*/long sector,byte[] data) {
            /*Bit32u*/long sectnum;

            sectnum = ( (cylinder * heads + head) * sectors ) + sector - 1L;

            return Write_AbsoluteSector(sectnum, data, 0);

        }


        public /*Bit8u*/short Write_AbsoluteSector(/*Bit32u*/long sectnum, byte[] data, int off) {
            /*Bit32u*/long bytenum;

            bytenum = sectnum * sector_size;

            //LOG_MSG("Writing sectors to %ld at bytenum %d", sectnum, bytenum);
            int ret = (int)sector_size;
            try {
                diskimg.seek(bytenum);
                diskimg.write(data, off, (int)sector_size);
            } catch (Exception e) {
                ret = 0;
                e.printStackTrace();
            }
            return (short)(((ret>0)?0x00:0x05));
        }

        public imageDisk(FileIO imgFile, String imgName, /*Bit32u*/long imgSizeK, boolean isHardDisk) {
            heads = 0;
            cylinders = 0;
            sectors = 0;
            sector_size = 512;
            diskimg = imgFile;
            try {diskimg.seek(0);} catch (Exception e) {}
            diskname = imgName;

            active = false;
            hardDrive = isHardDisk;
            if(!isHardDisk) {
                /*Bit8u*/short i=0;
                boolean founddisk = false;
                while (DiskGeometryList[i].ksize!=0x0) {
                    if ((DiskGeometryList[i].ksize==imgSizeK) ||
                        (DiskGeometryList[i].ksize+1==imgSizeK)) {
                        if (DiskGeometryList[i].ksize!=imgSizeK)
                            Log.log_msg("ImageLoader: image file with additional data, might not load!");
                        founddisk = true;
                        active = true;
                        floppytype = i;
                        heads = DiskGeometryList[i].headscyl;
                        cylinders = DiskGeometryList[i].cylcount;
                        sectors = DiskGeometryList[i].secttrack;
                        break;
                    }
                    i++;
                }
                if(!founddisk) {
                    active = false;
                } else {
                    incrementFDD();
                }
            }
        }

        public void Set_Geometry(/*Bit32u*/long setHeads, /*Bit32u*/long setCyl, /*Bit32u*/long setSect, /*Bit32u*/long setSectSize) {
            heads = setHeads;
            cylinders = setCyl;
            sectors = setSect;
            sector_size = setSectSize;
            active = true;
        }

        public void Get_Geometry(/*Bit32u*/LongRef getHeads, /*Bit32u*/LongRef getCyl, /*Bit32u*/LongRef getSect, /*Bit32u*/LongRef getSectSize) {
            getHeads.value = heads;
            getCyl.value = cylinders;
            getSect.value = sectors;
            getSectSize.value = sector_size;
        }

        public /*Bit8u*/short GetBiosType() {
            if(!hardDrive) {
                return (/*Bit8u*/short)DiskGeometryList[floppytype].biosval;
            } else return 0;
        }

        public /*Bit32u*/long getSectSize() {
            return sector_size;
        }
    }

    static private final int MAX_HDD_IMAGES = 2;

    static private final int MAX_DISK_IMAGES = 4;

    static private diskGeo DiskGeometryList[] = {
        new diskGeo( 160,  8, 1, 40, 0),
        new diskGeo( 180,  9, 1, 40, 0),
        new diskGeo( 200, 10, 1, 40, 0),
        new diskGeo( 320,  8, 2, 40, 1),
        new diskGeo( 360,  9, 2, 40, 1),
        new diskGeo( 400, 10, 2, 40, 1),
        new diskGeo( 720,  9, 2, 80, 3),
        new diskGeo(1200, 15, 2, 80, 2),
        new diskGeo(1440, 18, 2, 80, 4),
        new diskGeo(2880, 36, 2, 80, 6),
        new diskGeo(0, 0, 0, 0, 0)
    };

    static private /*Bitu*/int call_int13;
    static private /*Bitu*/int diskparm0, diskparm1;
    static private /*Bit8u*/short last_status;
    static private/*Bit8u*/short last_drive;
    static public /*Bit16u*/int imgDTASeg;
    static public /*RealPt*/int imgDTAPtr;
    static public Dos_DTA imgDTA;
    static private boolean killRead;
    static private boolean swapping_requested;

    /* 2 floppys and 2 harddrives, max */
    static public imageDisk[] imageDiskList;
    static public imageDisk[] diskSwap;
    static public/*Bits*/int swapPosition;

    static public void updateDPT() {
        /*Bit32u*/LongRef tmpheads=new LongRef(0), tmpcyl=new LongRef(0), tmpsect=new LongRef(0), tmpsize=new LongRef(0);
        if(imageDiskList[2] != null) {
            /*PhysPt*/int dp0physaddr= (int)Callback.CALLBACK_PhysPointer(diskparm0);
            imageDiskList[2].Get_Geometry(tmpheads, tmpcyl, tmpsect, tmpsize);
            Memory.phys_writew(dp0physaddr,(/*Bit16u*/int)tmpcyl.value);
            Memory.phys_writeb(dp0physaddr+0x2,(/*Bit8u*/short)tmpheads.value);
            Memory.phys_writew(dp0physaddr+0x3,0);
            Memory.phys_writew(dp0physaddr+0x5,(/*Bit16u*/int)-1);
            Memory.phys_writeb(dp0physaddr+0x7,(short)0);
            Memory.phys_writeb(dp0physaddr+0x8,(short)((0xc0 | (((imageDiskList[2].heads) > 8)?1:0 << 3))));
            Memory.phys_writeb(dp0physaddr+0x9,(short)0);
            Memory.phys_writeb(dp0physaddr+0xa,(short)0);
            Memory.phys_writeb(dp0physaddr+0xb,(short)0);
            Memory.phys_writew(dp0physaddr+0xc,(/*Bit16u*/int)tmpcyl.value);
            Memory.phys_writeb(dp0physaddr+0xe,(/*Bit8u*/short)tmpsect.value);
        }
        if(imageDiskList[3] != null) {
            /*PhysPt*/int dp1physaddr=(int)Callback.CALLBACK_PhysPointer(diskparm1);
            imageDiskList[3].Get_Geometry(tmpheads, tmpcyl, tmpsect, tmpsize);
            Memory.phys_writew(dp1physaddr,(/*Bit16u*/int)tmpcyl.value);
            Memory.phys_writeb(dp1physaddr+0x2,(/*Bit8u*/short)tmpheads.value);
            Memory.phys_writeb(dp1physaddr+0xe,(/*Bit8u*/short)tmpsect.value);
        }
    }

    static public void incrementFDD() {
        /*Bit16u*/int equipment= Memory.mem_readw(Bios.BIOS_CONFIGURATION);
        if((equipment&1)!=0) {
            /*Bitu*/int numofdisks = (equipment>>6)&3;
            numofdisks++;
            if(numofdisks > 1) numofdisks=1;//max 2 floppies at the moment
            equipment&=~0x00C0;
            equipment|=(numofdisks<<6);
        } else equipment|=1;
        Memory.mem_writew(Bios.BIOS_CONFIGURATION,equipment);
        Cmos.CMOS_SetRegister(0x14, (/*Bit8u*/short)(equipment&0xff));
    }

    static public void swapInDisks() {
        boolean allNull = true;
        /*Bits*/int diskcount = 0;
        /*Bits*/int swapPos = swapPosition;
        int i;

        /* Check to make sure there is at least one setup image */
        for(i=0;i<MAX_SWAPPABLE_DISKS;i++) {
            if(diskSwap[i]!=null) {
                allNull = false;
                break;
            }
        }

        /* No disks setup... fail */
        if (allNull) return;

        /* If only one disk is loaded, this loop will load the same disk in dive A and drive B */
        while(diskcount<2) {
            if(diskSwap[swapPos] != null) {
                Log.log_msg("Loaded disk "+diskcount+" from swaplist position "+swapPos+" - \""+diskSwap[swapPos].diskname+"\"");
                imageDiskList[diskcount] = diskSwap[swapPos];
                diskcount++;
            }
            swapPos++;
            if(swapPos>=MAX_SWAPPABLE_DISKS) swapPos=0;
        }
    }

    public static boolean getSwapRequest() {
        boolean sreq=swapping_requested;
        swapping_requested = false;
        return sreq;
    }

    static private Mapper.MAPPER_Handler swapInNextDisk = new Mapper.MAPPER_Handler() {
        public void call(boolean pressed) {
            if (!pressed)
                return;
            DriveManager.CycleAllDisks();
            /* Hack/feature: rescan all disks as well */
            Log.log_msg("Diskcaching reset for normal mounted drives.");
            for(/*Bitu*/int i=0;i<Dos_files.DOS_DRIVES;i++) {
                if (Dos_files.Drives[i]!=null) Dos_files.Drives[i].EmptyCache();
            }
            swapPosition++;
            if(diskSwap[swapPosition] == null) swapPosition = 0;
            swapInDisks();
            swapping_requested = true;
        }
    };

    static private /*Bitu*/int GetDosDriveNumber(/*Bitu*/int biosNum) {
        switch(biosNum) {
            case 0x0:
                return 0x0;
            case 0x1:
                return 0x1;
            case 0x80:
                return 0x2;
            case 0x81:
                return 0x3;
            case 0x82:
                return 0x4;
            case 0x83:
                return 0x5;
            default:
                return 0x7f;
        }
    }

    static boolean driveInactive(/*Bitu*/int driveNum) {
        if(driveNum>=(2 + MAX_HDD_IMAGES)) {
            if (Log.level<=LogSeverities.LOG_ERROR) Log.log(LogTypes.LOG_BIOS,LogSeverities.LOG_ERROR,"Disk "+driveNum+" non-existant");
            last_status = 0x01;
            Callback.CALLBACK_SCF(true);
            return true;
        }
        if(imageDiskList[driveNum] == null) {
            if (Log.level<=LogSeverities.LOG_ERROR) Log.log(LogTypes.LOG_BIOS,LogSeverities.LOG_ERROR,"Disk "+driveNum+" not active");
            last_status = 0x01;
            Callback.CALLBACK_SCF(true);
            return true;
        }
        if(!imageDiskList[driveNum].active) {
            if (Log.level<=LogSeverities.LOG_ERROR) Log.log(LogTypes.LOG_BIOS, LogSeverities.LOG_ERROR,"Disk "+driveNum+" not active");
            last_status = 0x01;
            Callback.CALLBACK_SCF(true);
            return true;
        }
        return false;
    }


    static private Callback.Handler INT13_DiskHandler = new Callback.Handler() {
        public String getName() {
            return "Bios.INT13_DiskHandler";
        }
        public /*Bitu*/int call() {
            /*Bit16u*/int segat, bufptr;
            /*Bit8u*/byte[] sectbuf=new byte[512];
            /*Bitu*/int  drivenum;
            /*Bitu*/int  i,t;
            last_drive = (short)CPU_Regs.reg_edx.low();
            drivenum = GetDosDriveNumber(last_drive);
            boolean any_images = false;
            for(i = 0;i < MAX_DISK_IMAGES;i++) {
                if(imageDiskList[i]!=null) any_images=true;
            }

            // unconditionally enable the interrupt flag
            Callback.CALLBACK_SIF(true);

            //drivenum = 0;
            //LOG_MSG("INT13: Function %x called on drive %x (dos drive %d)", reg_ah,  reg_dl, drivenum);
            switch(CPU_Regs.reg_eax.high()) {
            case 0x0: /* Reset disk */
                {
                    /* if there aren't any diskimages (so only localdrives and virtual drives)
                     * always succeed on reset disk. If there are diskimages then and only then
                     * do real checks
                     */
                    if (any_images && driveInactive(drivenum)) {
                        /* driveInactive sets carry flag if the specified drive is not available */
                        if ((Dosbox.machine== MachineType.MCH_CGA) || (Dosbox.machine==MachineType.MCH_PCJR)) {
                            /* those bioses call floppy drive reset for invalid drive values */
                            if (((imageDiskList[0]!=null) && (imageDiskList[0].active)) || ((imageDiskList[1]!=null) && (imageDiskList[1].active))) {
                                if (CPU_Regs.reg_edx.low()<0x80) CPU_Regs.reg_ip(CPU_Regs.reg_ip()+1);
                                last_status = 0x00;
                                Callback.CALLBACK_SCF(false);
                            }
                        }
                        return Callback.CBRET_NONE;
                    }
                    if (CPU_Regs.reg_edx.low()<0x80) CPU_Regs.reg_ip(CPU_Regs.reg_ip()+1);
                    last_status = 0x00;
                    Callback.CALLBACK_SCF(false);
                }
                break;
            case 0x1: /* Get status of last operation */

                if(last_status != 0x00) {
                    CPU_Regs.reg_eax.high(last_status);
                    Callback.CALLBACK_SCF(true);
                } else {
                    CPU_Regs.reg_eax.high(0x00);
                    Callback.CALLBACK_SCF(false);
                }
                break;
            case 0x2: /* Read sectors */
                if (CPU_Regs.reg_eax.low()==0) {
                    CPU_Regs.reg_eax.high(0x01);
                    Callback.CALLBACK_SCF(true);
                    return Callback.CBRET_NONE;
                }
                if (!any_images) {
                    // Inherit the Earth cdrom (uses it as disk test)
                    if (((CPU_Regs.reg_edx.low()&0x80)==0x80) && (CPU_Regs.reg_edx.high()==0) && ((CPU_Regs.reg_ecx.low()&0x3f)==1)) {
                        CPU_Regs.reg_eax.high(0);
                        Callback.CALLBACK_SCF(false);
                        return Callback.CBRET_NONE;
                    }
                }
                if (driveInactive(drivenum)) {
                    CPU_Regs.reg_eax.high(0xff);
                    Callback.CALLBACK_SCF(true);
                    return Callback.CBRET_NONE;
                }

                segat = (int)CPU_Regs.reg_esVal.dword;
                bufptr = CPU_Regs.reg_ebx.word();
                for(i=0;i<CPU_Regs.reg_eax.low();i++) {
                    last_status = imageDiskList[drivenum].Read_Sector((/*Bit32u*/long)CPU_Regs.reg_edx.high(), (/*Bit32u*/long)(CPU_Regs.reg_ecx.high() | ((CPU_Regs.reg_ecx.low() & 0xc0)<< 2)), (/*Bit32u*/long)((CPU_Regs.reg_ecx.low() & 63)+i), sectbuf);
                    if((last_status != 0x00) || (killRead)) {
                        Log.log_msg("Error in disk read");
                        killRead = false;
                        CPU_Regs.reg_eax.high(0x04);
                        Callback.CALLBACK_SCF(true);
                        return Callback.CBRET_NONE;
                    }
                    for(t=0;t<512;t++) {
                        Memory.real_writeb(segat,bufptr,sectbuf[t]);
                        bufptr++;
                    }
                }
                CPU_Regs.reg_eax.high(0x00);
                Callback.CALLBACK_SCF(false);
                break;
            case 0x3: /* Write sectors */

                if(driveInactive(drivenum)) {
                    CPU_Regs.reg_eax.high(0xff);
                    Callback.CALLBACK_SCF(true);
                    return Callback.CBRET_NONE;
                }


                bufptr = CPU_Regs.reg_ebx.word();
                for(i=0;i<CPU_Regs.reg_eax.low();i++) {
                    for(t=0;t<imageDiskList[drivenum].getSectSize();t++) {
                        sectbuf[t] = (byte)Memory.real_readb((int)CPU_Regs.reg_esVal.dword,bufptr);
                        bufptr++;
                    }

                    last_status = imageDiskList[drivenum].Write_Sector((/*Bit32u*/long)CPU_Regs.reg_edx.high(), (/*Bit32u*/long)(CPU_Regs.reg_ecx.high() | ((CPU_Regs.reg_ecx.low() & 0xc0) << 2)), (/*Bit32u*/long)((CPU_Regs.reg_ecx.low() & 63) + i), sectbuf);
                    if(last_status != 0x00) {
                    Callback.CALLBACK_SCF(true);
                        return Callback.CBRET_NONE;
                    }
                }
                CPU_Regs.reg_eax.high(0x00);
                Callback.CALLBACK_SCF(false);
                break;
            case 0x04: /* Verify sectors */
                if (CPU_Regs.reg_eax.low()==0) {
                    CPU_Regs.reg_eax.high(0x01);
                    Callback.CALLBACK_SCF(true);
                    return Callback.CBRET_NONE;
                }
                if(driveInactive(drivenum)) return Callback.CBRET_NONE;

                /* TODO: Finish coding this section */
    //            segat = SegValue(es);
    //            bufptr = CPU_Regs.reg_ebx.word();
    //            for(i=0;i<CPU_Regs.reg_eax.low();i++) {
    //                last_status = imageDiskList[drivenum].Read_Sector((/*Bit32u*/long)CPU_Regs.reg_edx.high(), (/*Bit32u*/long)(CPU_Regs.reg_ecx.high() | ((CPU_Regs.reg_ecx.low() & 0xc0)<< 2)), (/*Bit32u*/long)((CPU_Regs.reg_ecx.low() & 63)+i), sectbuf);
    //                if(last_status != 0x00) {
    //                    Log.log_msg("Error in disk read");
    //                    Callback.CALLBACK_SCF(true);
    //                    return Callback.CBRET_NONE;
    //                }
    //                for(t=0;t<512;t++) {
    //                    Memory.real_writeb(segat,bufptr,sectbuf[t]);
    //                    bufptr++;
    //                }
    //            }
                CPU_Regs.reg_eax.high(0x00);
                //Qbix: The following codes don't match my specs. al should be number of sector verified
                //CPU_Regs.reg_eax.low() = 0x10; /* CRC verify failed */
                //CPU_Regs.reg_eax.low() = 0x00; /* CRC verify succeeded */
                Callback.CALLBACK_SCF(false);

                break;
            case 0x08: /* Get drive parameters */
                if(driveInactive(drivenum)) {
                    last_status = 0x07;
                    CPU_Regs.reg_eax.high(last_status);
                    Callback.CALLBACK_SCF(true);
                    return Callback.CBRET_NONE;
                }
                CPU_Regs.reg_eax.word(0x00);
                CPU_Regs.reg_ebx.low(imageDiskList[drivenum].GetBiosType());
                /*Bit32u*/LongRef tmpheads=new LongRef(0), tmpcyl=new LongRef(0), tmpsect=new LongRef(0), tmpsize=new LongRef(0);
                imageDiskList[drivenum].Get_Geometry(tmpheads, tmpcyl, tmpsect, tmpsize);
                if (tmpcyl.value==0) Log.log(LogTypes.LOG_BIOS,LogSeverities.LOG_ERROR,"INT13 DrivParm: cylinder count zero!");
                else tmpcyl.value--;		// cylinder count . max cylinder
                if (tmpheads.value==0) Log.log(LogTypes.LOG_BIOS,LogSeverities.LOG_ERROR,"INT13 DrivParm: head count zero!");
                else tmpheads.value--;	// head count . max head
                CPU_Regs.reg_ecx.high((/*Bit8u*/int)(tmpcyl.value & 0xff));
                CPU_Regs.reg_ecx.low((/*Bit8u*/int)(((tmpcyl.value >> 2) & 0xc0) | (tmpsect.value & 0x3f)));
                CPU_Regs.reg_edx.high((/*Bit8u*/int)tmpheads.value);
                last_status = 0x00;
                if ((CPU_Regs.reg_edx.low()&0x80)!=0) {	// harddisks
                    CPU_Regs.reg_edx.low(0);
                    if(imageDiskList[2] != null) CPU_Regs.reg_edx.low((CPU_Regs.reg_edx.low()+1));
                    if(imageDiskList[3] != null) CPU_Regs.reg_edx.low((CPU_Regs.reg_edx.low()+1));
                } else {		// floppy disks
                    CPU_Regs.reg_edx.low(0);
                    if(imageDiskList[0] != null) CPU_Regs.reg_edx.low((CPU_Regs.reg_edx.low()+1));
                    if(imageDiskList[1] != null) CPU_Regs.reg_edx.low((CPU_Regs.reg_edx.low()+1));
                }
                Callback.CALLBACK_SCF(false);
                break;
            case 0x11: /* Recalibrate drive */
                CPU_Regs.reg_eax.high(0x00);
                Callback.CALLBACK_SCF(false);
                break;
            case 0x17: /* Set disk type for format */
                /* Pirates! needs this to load */
                killRead = true;
                CPU_Regs.reg_eax.high(0x00);
                Callback.CALLBACK_SCF(false);
                break;
            default:
                if (Log.level<=LogSeverities.LOG_ERROR) Log.log(LogTypes.LOG_BIOS,LogSeverities.LOG_ERROR,"INT13: Function "+Integer.toString(CPU_Regs.reg_eax.high(), 16)+" called on drive "+Integer.toString(CPU_Regs.reg_edx.low(), 16)+" (dos drive "+drivenum+")");
                CPU_Regs.reg_eax.high(0xff);
                Callback.CALLBACK_SCF(true);
            }
            return Callback.CBRET_NONE;
        }
    };

    static public void BIOS_CloseDisks() {
        imageDiskList = null;
        diskSwap = null;
    }
    static public void BIOS_SetupDisks() {
        imageDiskList=new imageDisk[MAX_DISK_IMAGES];
        diskSwap=new imageDisk[MAX_SWAPPABLE_DISKS];

    /* TODO Start the time correctly */
        call_int13=Callback.CALLBACK_Allocate();
        Callback.CALLBACK_Setup(call_int13,INT13_DiskHandler,Callback.CB_INT13,"Int 13 Bios disk");
        Memory.RealSetVec(0x13,Callback.CALLBACK_RealPointer(call_int13));
        int i;
        for(i=0;i<4;i++) {
            imageDiskList[i] = null;
        }

        for(i=0;i<MAX_SWAPPABLE_DISKS;i++) {
            diskSwap[i] = null;
        }

        diskparm0 = Callback.CALLBACK_Allocate();
        diskparm1 = Callback.CALLBACK_Allocate();
        swapPosition = 0;

        Memory.RealSetVec(0x41,Callback.CALLBACK_RealPointer(diskparm0));
        Memory.RealSetVec(0x46,Callback.CALLBACK_RealPointer(diskparm1));

        /*PhysPt*/int dp0physaddr=Callback.CALLBACK_PhysPointer(diskparm0);
        /*PhysPt*/int dp1physaddr=Callback.CALLBACK_PhysPointer(diskparm1);
        for(i=0;i<16;i++) {
            Memory.phys_writeb((int)dp0physaddr+i,(short)0);
            Memory.phys_writeb((int)dp1physaddr+i,(short)0);
        }

        imgDTASeg = 0;

    /* Setup the Bios Area */
        Memory.mem_writeb(Bios.BIOS_HARDDISK_COUNT,2);

        JavaMapper.MAPPER_AddHandler(swapInNextDisk, Mapper.MapKeys.MK_f4, Mapper.MMOD1, "swapimg", "Swap Image");
        killRead = false;
        swapping_requested = false;
    }

}
