package jdos.dos;

import jdos.cpu.CPU;
import jdos.cpu.CPU_Regs;
import jdos.cpu.Callback;
import jdos.hardware.Memory;
import jdos.ints.Bios_disk;
import jdos.misc.Log;
import jdos.misc.setup.Section;
import jdos.types.LogSeverities;
import jdos.types.LogTypes;
import jdos.util.*;

public class DosMSCDEX {
    static private final int MSCDEX_VERSION_HIGH = 2;
    static private final int  MSCDEX_VERSION_LOW = 23;
    static private final int  MSCDEX_MAX_DRIVES	= 8;

    // Error Codes
    static private final int  MSCDEX_ERROR_INVALID_FUNCTION	= 1;
    static private final int  MSCDEX_ERROR_BAD_FORMAT = 11;
    static private final int  MSCDEX_ERROR_UNKNOWN_DRIVE = 15;
    static private final int  MSCDEX_ERROR_DRIVE_NOT_READY = 21;

    // Request Status
    static private final int 	REQUEST_STATUS_DONE = 0x0100;
    static private final int 	REQUEST_STATUS_ERROR = 0x8000;

    // Use cdrom Interface
    //static private int useCdromInterface	= CDROM_USE_SDL;
    static private int forceCD				= -1;

    static private class DOS_DeviceHeader extends MemStruct {
        public static final int size = 22;
        public DOS_DeviceHeader(/*PhysPt*/int ptr)				{ pt = ptr; };

        public void SetNextDeviceHeader(/*RealPt*/int ptr)	{ SaveIt(4, 0, ptr); } //sSave(sDeviceHeader,nextDeviceHeader,ptr); }
        public /*RealPt*/int GetNextDeviceHeader() { return GetIt(4, 0); } //sGet(sDeviceHeader,nextDeviceHeader); }
        public void SetAttribute(/*Bit16u*/int atr) { SaveIt(2,4,atr); } //sSave(sDeviceHeader,devAttributes,atr); }
        public void SetDriveLetter(/*Bit8u*/int letter) { SaveIt(1,20,letter); } //sSave(sDeviceHeader,driveLetter,letter); }
        public void SetNumSubUnits(/*Bit8u*/short num) { SaveIt(1,21,num); } //sSave(sDeviceHeader,numSubUnits,num); }
        public /*Bit8u*/short GetNumSubUnits() { return (short)GetIt(1, 21); } //sGet(sDeviceHeader,numSubUnits); }
        public void SetName(String _name) { Memory.MEM_BlockWrite(pt+10,_name,8); }
        public void SetInterrupt(/*Bit16u*/int ofs) { SaveIt(2, 8, ofs); } //sSave(sDeviceHeader,interrupt,ofs); }
        public void SetStrategy(/*Bit16u*/int ofs)  { SaveIt(2, 6, ofs); } //sSave(sDeviceHeader,strategy,ofs); }

//        struct sDeviceHeader{
// 0          RealPt	nextDeviceHeader;
// 4          Bit16u	devAttributes;
// 6          Bit16u	strategy;
// 8          Bit16u	interrupt;
// 10         Bit8u	name[8];
// 18         Bit16u  wReserved;
// 20         Bit8u	driveLetter;
// 21         Bit8u	numSubUnits;
// 22     };
    }

    static private class CMscdex {
        /*Bit16u*/int GetVersion() { return (MSCDEX_VERSION_HIGH<<8)+MSCDEX_VERSION_LOW; }
        /*Bit16u*/int GetNumDrives() { return numDrives; }
        /*Bit16u*/int GetFirstDrive() { return dinfo[0].drive; }

        /*Bit16u*/int numDrives;

        static public class TDriveInfo {
            /*Bit8u*/short drive;			// drive letter in dosbox
            /*Bit8u*/short physDrive;		// drive letter in system
            boolean	audioPlay;		 // audio playing active
            boolean	audioPaused;	// audio playing paused
            /*Bit32u*/long	audioStart;		// StartLoc for resume
            /*Bit32u*/long	audioEnd;		// EndLoc for resume
            boolean	locked;			// drive locked ?
            boolean	lastResult;		// last operation success ?
            /*Bit32u*/long	volumeSize;		// for media change
            Dos_cdrom.TCtrl audioCtrl = new Dos_cdrom.TCtrl();	// audio channel control
        }

        /*Bit16u*/int				defaultBufSeg;
        TDriveInfo[] dinfo = new TDriveInfo[MSCDEX_MAX_DRIVES];
        Dos_cdrom.CDROM_Interface[] cdrom = new Dos_cdrom.CDROM_Interface[MSCDEX_MAX_DRIVES];

        /*Bit16u*/int rootDriverHeaderSeg;

        CMscdex() {
            numDrives			= 0;
            rootDriverHeaderSeg	= 0;
            defaultBufSeg		= 0;

            for (/*Bit32u*/int i=0; i<MSCDEX_MAX_DRIVES; i++)
                dinfo[i] = new TDriveInfo();
        }

        void GetDrives(/*PhysPt*/int data)
        {
            for (/*Bit16u*/int i=0; i<GetNumDrives(); i++) Memory.mem_writeb(data+i,dinfo[i].drive);
        }

        boolean IsValidDrive(/*Bit16u*/int _drive)
        {
            _drive &= 0xff; //Only lowerpart (Ultimate domain)
            for (/*Bit16u*/int i=0; i<GetNumDrives(); i++) if (dinfo[i].drive==_drive) return true;
            return false;
        }

        /*Bit8u*/short GetSubUnit(/*Bit16u*/int _drive)
        {
            _drive &= 0xff; //Only lowerpart (Ultimate domain)
            for (/*Bit16u*/int i=0; i<GetNumDrives(); i++) if (dinfo[i].drive==_drive) return (/*Bit8u*/short)i;
            return 0xff;
        }

        int RemoveDrive(/*Bit16u*/int _drive)
        {
            /*Bit16u*/int idx = MSCDEX_MAX_DRIVES;
            for (/*Bit16u*/int i=0; i<GetNumDrives(); i++) {
                if (dinfo[i].drive == _drive) {
                    idx = i;
                    break;
                }
            }

            if (idx == MSCDEX_MAX_DRIVES || (idx!=0 && idx!=GetNumDrives()-1)) return 0;
            cdrom[idx].close();
            if (idx==0) {
                for (/*Bit16u*/int i=0; i<GetNumDrives(); i++) {
                    if (i == MSCDEX_MAX_DRIVES-1) {
                        cdrom[i] = null;
                        dinfo[i] = new TDriveInfo();
                    } else {
                        dinfo[i] = dinfo[i+1];
                        cdrom[i] = cdrom[i+1];
                    }
                }
            } else {
                cdrom[idx] = null;
                dinfo[idx] = new TDriveInfo();
            }
            numDrives--;

            if (GetNumDrives() == 0) {
                DOS_DeviceHeader devHeader = new DOS_DeviceHeader(Memory.PhysMake(rootDriverHeaderSeg,0));
                /*Bit16u*/int off = DOS_DeviceHeader.size;
                devHeader.SetStrategy(off+4);		// point to the RETF (To deactivate MSCDEX)
                devHeader.SetInterrupt(off+4);		// point to the RETF (To deactivate MSCDEX)
                devHeader.SetDriveLetter(0);
            } else if (idx==0) {
                DOS_DeviceHeader devHeader = new DOS_DeviceHeader((Memory.PhysMake(rootDriverHeaderSeg,0)));
                devHeader.SetDriveLetter(GetFirstDrive()+1);
            }
            return 1;
        }

        int AddDrive(/*Bit16u*/int _drive, String physicalPath, /*Bit8u*/ShortRef subUnit)
        {
            subUnit.value = 0;
            if (GetNumDrives()+1>=MSCDEX_MAX_DRIVES) return 4;
            if (GetNumDrives()!=0) {
                // Error check, driveletter have to be in a row
                if (dinfo[0].drive-1!=_drive && dinfo[numDrives-1].drive+1!=_drive)
                    return 1;
            }
            // Set return type to ok
            int result = 0;
            // Get Mounttype and init needed cdrom interface
            switch (Dos_cdrom.CDROM_GetMountType(physicalPath,forceCD)) {
            case 0x00: {
                if (Log.level<=LogSeverities.LOG_ERROR) Log.log(LogTypes.LOG_MISC, LogSeverities.LOG_ERROR,"MSCDEX: Mounting physical cdrom not supported: "+physicalPath);
//                Log.log(LogTypes.LOG_MISC,LogSeverities.LOG_NORMAL,"MSCDEX: Mounting physical cdrom: %s"	,physicalPath);
//        #if defined (WIN32)
//                // Check OS
//                OSVERSIONINFO osi;
//                osi.dwOSVersionInfoSize = sizeof(osi);
//                GetVersionEx(&osi);
//                if ((osi.dwPlatformId==VER_PLATFORM_WIN32_NT) && (osi.dwMajorVersion>4)) {
//                    // only WIN NT/200/XP
//                    if (useCdromInterface==CDROM_USE_IOCTL_DIO) {
//                        cdrom[numDrives] = new CDROM_Interface_Ioctl(CDROM_Interface_Ioctl::CDIOCTL_CDA_DIO);
//                        Log.log(LogTypes.LOG_MISC,LogSeverities.LOG_NORMAL,"MSCDEX: IOCTL Interface.");
//                        break;
//                    }
//                    if (useCdromInterface==CDROM_USE_IOCTL_DX) {
//                        cdrom[numDrives] = new CDROM_Interface_Ioctl(CDROM_Interface_Ioctl::CDIOCTL_CDA_DX);
//                        Log.log(LogTypes.LOG_MISC,LogSeverities.LOG_NORMAL,"MSCDEX: IOCTL Interface (digital audio extraction).");
//                        break;
//                    }
//                    if (useCdromInterface==CDROM_USE_IOCTL_MCI) {
//                        cdrom[numDrives] = new CDROM_Interface_Ioctl(CDROM_Interface_Ioctl::CDIOCTL_CDA_MCI);
//                        Log.log(LogTypes.LOG_MISC,LogSeverities.LOG_NORMAL,"MSCDEX: IOCTL Interface (media control interface).");
//                        break;
//                    }
//                }
//                if (useCdromInterface==CDROM_USE_ASPI) {
//                    // all Wins - ASPI
//                    cdrom[numDrives] = new CDROM_Interface_Aspi();
//                    Log.log(LogTypes.LOG_MISC,LogSeverities.LOG_NORMAL,"MSCDEX: ASPI Interface.");
//                    break;
//                }
//        #endif
//        #if defined (LINUX) || defined(OS2)
//                // Always use IOCTL in Linux or OS/2
//                cdrom[numDrives] = new CDROM_Interface_Ioctl();
//                Log.log(LogTypes.LOG_MISC,LogSeverities.LOG_NORMAL,"MSCDEX: IOCTL Interface.");
//        #else
//                // Default case windows and other oses
//                cdrom[numDrives] = new CDROM_Interface_SDL();
//                Log.log(LogTypes.LOG_MISC,LogSeverities.LOG_NORMAL,"MSCDEX: SDL Interface.");
//        #endif
                } break;
            case 0x01:	// iso cdrom interface
                if (Log.level<=LogSeverities.LOG_NORMAL) Log.log(LogTypes.LOG_MISC,LogSeverities.LOG_NORMAL,"MSCDEX: Mounting iso file as cdrom: "+physicalPath);
                cdrom[numDrives] = new CDROM_Interface_Image((/*Bit8u*/short)numDrives);
                break;
            case 0x02:	// fake cdrom interface (directories)
                cdrom[numDrives] = new CDROM_Interface_Fake();
                if (Log.level<=LogSeverities.LOG_NORMAL) Log.log(LogTypes.LOG_MISC,LogSeverities.LOG_NORMAL,"MSCDEX: Mounting directory as cdrom: "+physicalPath);
                if (Log.level<=LogSeverities.LOG_NORMAL) Log.log(LogTypes.LOG_MISC,LogSeverities.LOG_NORMAL,"MSCDEX: You wont have full MSCDEX support !");
                result = 5;
                break;
            default	:	// weird result
                return 6;
            }

            if (!cdrom[numDrives].SetDevice(physicalPath,forceCD)) {
        //		delete cdrom[numDrives] ; mount seems to delete it
                return 3;
            }


            if (rootDriverHeaderSeg==0) {

                /*Bit16u*/int driverSize = DOS_DeviceHeader.size + 10; // 10 = Bytes for 3 callbacks

                // Create Device Header
                /*Bit16u*/int seg = Dos_tables.DOS_GetMemory((driverSize+15)/16);
                DOS_DeviceHeader devHeader = new DOS_DeviceHeader(Memory.PhysMake(seg,0));
                devHeader.SetNextDeviceHeader(0xFFFFFFFF);
                devHeader.SetAttribute(0xc800);
                devHeader.SetDriveLetter(_drive+1);
                devHeader.SetNumSubUnits((short)1);
                devHeader.SetName("MSCD001 ");

                //Link it in the device chain
                /*Bit32u*/long start = Dos.dos_infoblock.GetDeviceChain() & 0xFFFFFFFFl;
                /*Bit16u*/int segm  = (/*Bit16u*/int)(start>>16);
                /*Bit16u*/int offm  = (/*Bit16u*/int)(start&0xFFFF);
                while(start != 0xFFFFFFFFl) {
                    segm  = (/*Bit16u*/int)(start>>>16);
                    offm  = (/*Bit16u*/int)(start&0xFFFF);
                    start = Memory.real_readd(segm,offm) & 0xFFFFFFFFl;
                }
                Memory.real_writed(segm,offm,seg<<16);

                // Create Callback Strategy
                /*Bit16u*/int off = DOS_DeviceHeader.size;
                /*Bit16u*/int call_strategy=Callback.CALLBACK_Allocate();
                Callback.CallBack_Handlers[call_strategy]=MSCDEX_Strategy_Handler;
                Memory.real_writeb(seg,off+0,(/*Bit8u*/short)0xFE);		//GRP 4
                Memory.real_writeb(seg,off+1,(/*Bit8u*/short)0x38);		//Extra Callback instruction
                Memory.real_writew(seg,off+2,call_strategy);	//The immediate word
                Memory.real_writeb(seg,off+4,(/*Bit8u*/short)0xCB);		//A RETF Instruction
                devHeader.SetStrategy(off);

                // Create Callback Interrupt
                off += 5;
                /*Bit16u*/int call_interrupt=Callback.CALLBACK_Allocate();
                Callback.CallBack_Handlers[call_interrupt]=MSCDEX_Interrupt_Handler;
                Memory.real_writeb(seg,off+0,(/*Bit8u*/short)0xFE);		//GRP 4
                Memory.real_writeb(seg,off+1,(/*Bit8u*/short)0x38);		//Extra Callback instruction
                Memory.real_writew(seg,off+2,call_interrupt);	//The immediate word
                Memory.real_writeb(seg,off+4,(/*Bit8u*/short)0xCB);		//A RETF Instruction
                devHeader.SetInterrupt(off);

                rootDriverHeaderSeg = seg;

            } else if (GetNumDrives() == 0) {
                DOS_DeviceHeader devHeader=new DOS_DeviceHeader(Memory.PhysMake(rootDriverHeaderSeg,0));
                /*Bit16u*/int off = DOS_DeviceHeader.size;
                devHeader.SetDriveLetter(_drive+1);
                devHeader.SetStrategy(off);
                devHeader.SetInterrupt(off+5);
            }

            // Set drive
            DOS_DeviceHeader devHeader = new DOS_DeviceHeader(Memory.PhysMake(rootDriverHeaderSeg,0));
            devHeader.SetNumSubUnits((short)(devHeader.GetNumSubUnits()+1));

            if (dinfo[0].drive-1==_drive) {
                Dos_cdrom.CDROM_Interface _cdrom = cdrom[numDrives];
                CDROM_Interface_Image _cdimg = CDROM_Interface_Image.images[numDrives];
                for (/*Bit16u*/int i=GetNumDrives(); i>0; i--) {
                    dinfo[i] = dinfo[i-1];
                    cdrom[i] = cdrom[i-1];
                    CDROM_Interface_Image.images[i] = CDROM_Interface_Image.images[i-1];
                }
                cdrom[0] = _cdrom;
                CDROM_Interface_Image.images[0] = _cdimg;
                dinfo[0].drive		= (/*Bit8u*/short)_drive;
                dinfo[0].physDrive	= (/*Bit8u*/short)physicalPath.toUpperCase().charAt(0);
                subUnit.value = 0;
            } else {
                dinfo[numDrives].drive		= (/*Bit8u*/short)_drive;
                dinfo[numDrives].physDrive	= (/*Bit8u*/short)physicalPath.toUpperCase().charAt(0);
                subUnit.value = (/*Bit8u*/short)numDrives;
            }
            numDrives++;
            // init channel control
            for (/*Bit8u*/int chan=0;chan<4;chan++) {
                dinfo[subUnit.value].audioCtrl.out[chan]=chan;
                dinfo[subUnit.value].audioCtrl.vol[chan]=0xff;
            }
            // stop audio
            StopAudio(subUnit.value);
            return result;
        }

        boolean HasDrive(/*Bit16u*/int drive) {
            return (GetSubUnit(drive) != 0xff);
        }

        void ReplaceDrive(Dos_cdrom.CDROM_Interface newCdrom, /*Bit8u*/short subUnit) {
            cdrom[subUnit].close();
            cdrom[subUnit] = newCdrom;
            StopAudio(subUnit);
        }

        /*PhysPt*/int GetDefaultBuffer() {
            if (defaultBufSeg==0) {
                /*Bit16u*/int size = (2352*2+15)/16;
                defaultBufSeg = Dos_tables.DOS_GetMemory(size);
            }
            return Memory.PhysMake(defaultBufSeg,2352);
        }

        /*PhysPt*/int GetTempBuffer() {
            if (defaultBufSeg==0) {
                /*Bit16u*/int size = (2352*2+15)/16;
                defaultBufSeg = Dos_tables.DOS_GetMemory(size);
            }
            return Memory.PhysMake(defaultBufSeg,0);
        }

        void GetDriverInfo	(/*PhysPt*/int data) {
            for (/*Bit16u*/int i=0; i<GetNumDrives(); i++) {
                Memory.mem_writeb(data  ,(/*Bit8u*/short)i);	// subunit
                Memory.mem_writed(data+1,Memory.RealMake(rootDriverHeaderSeg,0));
                data+=5;
            }
        }

        boolean GetCDInfo(/*Bit8u*/short subUnit, /*Bit8u*/ShortRef tr1, /*Bit8u*/ShortRef tr2, Dos_cdrom.TMSF leadOut)
        {
            if (subUnit>=numDrives) return false;
            IntRef tr1i=new IntRef(0),tr2i=new IntRef(0);
            // Assume Media change
            cdrom[subUnit].InitNewMedia();
            dinfo[subUnit].lastResult = cdrom[subUnit].GetAudioTracks(tr1i,tr2i,leadOut);
            if (!dinfo[subUnit].lastResult) {
                tr1.value = tr2.value = 0;
                leadOut.clear();
            } else {
                tr1.value = (/*Bit8u*/short) tr1i.value;
                tr2.value = (/*Bit8u*/short) tr2i.value;
            }
            return dinfo[subUnit].lastResult;
        }

        boolean GetTrackInfo(/*Bit8u*/short subUnit, /*Bit8u*/short track, /*Bit8u*/ShortRef attr, Dos_cdrom.TMSF start)
        {
            if (subUnit>=numDrives) return false;
            dinfo[subUnit].lastResult = cdrom[subUnit].GetAudioTrackInfo(track,start,attr);
            if (!dinfo[subUnit].lastResult) {
                attr.value = 0;
                start.clear();
            }
            return dinfo[subUnit].lastResult;
        }

        boolean PlayAudioSector(/*Bit8u*/short subUnit, /*Bit32u*/long sector, /*Bit32u*/long length)
        {
            if (subUnit>=numDrives) return false;
            // If value from last stop is used, this is meant as a resume
            // better start using resume command
            if (dinfo[subUnit].audioPaused && (sector==dinfo[subUnit].audioStart) && (dinfo[subUnit].audioEnd!=0)) {
                dinfo[subUnit].lastResult = cdrom[subUnit].PauseAudio(true);
            } else
                dinfo[subUnit].lastResult = cdrom[subUnit].PlayAudioSector(sector,length);

            if (dinfo[subUnit].lastResult) {
                dinfo[subUnit].audioPlay	= true;
                dinfo[subUnit].audioPaused	= false;
                dinfo[subUnit].audioStart	= sector;
                dinfo[subUnit].audioEnd		= length;
            }
            return dinfo[subUnit].lastResult;
        }

        boolean PlayAudioMSF(/*Bit8u*/short subUnit, /*Bit32u*/long start, /*Bit32u*/long length)
        {
            if (subUnit>=numDrives) return false;
            /*Bit8u*/short min		= (/*Bit8u*/short)((start>>16) & 0xFF);
            /*Bit8u*/short sec		= (/*Bit8u*/short)((start>> 8) & 0xFF);
            /*Bit8u*/short fr		= (/*Bit8u*/short)((start>> 0) & 0xFF);
            /*Bit32u*/long sector	= min*60*75+sec*75+fr - 150;
            return dinfo[subUnit].lastResult = PlayAudioSector(subUnit,sector,length);
        }

        boolean GetSubChannelData(/*Bit8u*/short subUnit, /*Bit8u*/ShortRef attr, /*Bit8u*/ShortRef track, /*Bit8u*/ShortRef index, Dos_cdrom.TMSF rel, Dos_cdrom.TMSF abs)
        {
            if (subUnit>=numDrives) return false;
            dinfo[subUnit].lastResult = cdrom[subUnit].GetAudioSub(attr,track,index,rel,abs);
            if (!dinfo[subUnit].lastResult) {
                attr.value = track.value = index.value = 0;
                rel.clear();
                abs.clear();
            }
            return dinfo[subUnit].lastResult;
        }

        boolean GetAudioStatus(/*Bit8u*/short subUnit, BooleanRef playing, BooleanRef pause, Dos_cdrom.TMSF start, Dos_cdrom.TMSF end)
        {
            if (subUnit>=numDrives) return false;
            dinfo[subUnit].lastResult = cdrom[subUnit].GetAudioStatus(playing,pause);
            if (dinfo[subUnit].lastResult) {
                // Start
                /*Bit32u*/long addr	= dinfo[subUnit].audioStart + 150;
                start.fr	= (/*Bit8u*/short)(addr%75);	addr/=75;
                start.sec	= (/*Bit8u*/short)(addr%60);
                start.min	= (/*Bit8u*/short)(addr/60);
                // End
                addr		= dinfo[subUnit].audioEnd + 150;
                end.fr		= (/*Bit8u*/short)(addr%75);	addr/=75;
                end.sec		= (/*Bit8u*/short)(addr%60);
                end.min		= (/*Bit8u*/short)(addr/60);
            } else {
                playing.value = false;
                pause.value = false;
                start.clear();
                end.clear();
            }

            return dinfo[subUnit].lastResult;
        }

        boolean StopAudio(/*Bit8u*/short subUnit)
        {
            if (subUnit>=numDrives) return false;

            if (dinfo[subUnit].audioPlay) {
                // Check if audio is still playing....
                Dos_cdrom.TMSF start=new Dos_cdrom.TMSF(),end=new Dos_cdrom.TMSF();
                BooleanRef playing = new BooleanRef(), pause = new BooleanRef();
                if (GetAudioStatus(subUnit,playing,pause,start,end))
                    dinfo[subUnit].audioPlay = playing.value;
                else
                    dinfo[subUnit].audioPlay = false;
            }
            if (dinfo[subUnit].audioPlay)
                dinfo[subUnit].lastResult = cdrom[subUnit].PauseAudio(false);
            else
                dinfo[subUnit].lastResult = cdrom[subUnit].StopAudio();

            if (dinfo[subUnit].lastResult) {
                if (dinfo[subUnit].audioPlay) {
                    Dos_cdrom.TMSF pos = new Dos_cdrom.TMSF();
                    GetCurrentPos(subUnit,pos);
                    dinfo[subUnit].audioStart	= pos.min*60*75+pos.sec*75+pos.fr - 150;
                    dinfo[subUnit].audioPaused  = true;
                } else {
                    dinfo[subUnit].audioPaused  = false;
                    dinfo[subUnit].audioStart	= 0;
                    dinfo[subUnit].audioEnd		= 0;
                }
                dinfo[subUnit].audioPlay = false;
            }
            return dinfo[subUnit].lastResult;
        }

        boolean ResumeAudio(/*Bit8u*/short subUnit) {
            if (subUnit>=numDrives) return false;
            return dinfo[subUnit].lastResult = PlayAudioSector(subUnit,dinfo[subUnit].audioStart,dinfo[subUnit].audioEnd);
        }

        /*Bit32u*/long GetVolumeSize(/*Bit8u*/short subUnit) {
            if (subUnit>=numDrives) return 0;
            /*Bit8u*/ShortRef tr1=new ShortRef(0),tr2=new ShortRef(0);
            Dos_cdrom.TMSF leadOut = new Dos_cdrom.TMSF();
            dinfo[subUnit].lastResult = GetCDInfo(subUnit,tr1,tr2,leadOut);
            if (dinfo[subUnit].lastResult) return (leadOut.min*60*75)+(leadOut.sec*75)+leadOut.fr;
            return 0;
        }

        boolean ReadVTOC(/*Bit16u*/int drive, /*Bit16u*/int volume, /*PhysPt*/int data, /*Bit16u*/IntRef error) {
            /*Bit8u*/short subunit = GetSubUnit(drive);
        /*	if (subunit>=numDrives) {
                error=MSCDEX_ERROR_UNKNOWN_DRIVE;
                return false;
            } */
            if (!ReadSectors(subunit,false,16+volume,1,data)) {
                error.value=MSCDEX_ERROR_DRIVE_NOT_READY;
                return false;
            }
            byte[] id = new byte[5];
            Memory.MEM_BlockRead(data + 1, id, 5);
            if (!"CD001".equals(new String(id))) {
                error.value = MSCDEX_ERROR_BAD_FORMAT;
                return false;
            }
            /*Bit8u*/int type = Memory.mem_readb(data);
            error.value = (type == 1) ? 1 : (type == 0xFF) ? 0xFF : 0;
            return true;
        }

        boolean GetVolumeName(/*Bit8u*/short subUnit, StringRef data) {
            if (subUnit>=numDrives) return false;
            /*Bit16u*/int drive = dinfo[subUnit].drive;

            /*Bit16u*/IntRef error = new IntRef(0);
            boolean success = false;
            /*PhysPt*/int ptoc = GetTempBuffer();
            success = ReadVTOC(drive,0x00,ptoc,error);
            if (success) {
                data.value = Memory.MEM_StrCopy(ptoc+40,31);
                data.value = data.value.trim();
            }
            return success;
        }

        boolean GetCopyrightName(/*Bit16u*/int drive, /*PhysPt*/int data) {
            /*Bit16u*/IntRef error=new IntRef(0);
            boolean success = false;
            /*PhysPt*/int ptoc = GetTempBuffer();
            success = ReadVTOC(drive,0x00,ptoc,error);
            if (success) {
                /*Bitu*/int len;
                for (len=0;len<37;len++) {
                    /*Bit8u*/int c=Memory.mem_readb(ptoc+702+len);
                    if (c==0 || c==0x20) break;
                }
                Memory.MEM_BlockCopy(data,ptoc+702,len);
                Memory.mem_writeb(data+len,0);
            }
            return success;
        }

        boolean GetAbstractName(/*Bit16u*/int drive, /*PhysPt*/int data) {
            /*Bit16u*/IntRef error = new IntRef(0);
            boolean success = false;
            /*PhysPt*/int ptoc = GetTempBuffer();
            success = ReadVTOC(drive,0x00,ptoc,error);
            if (success) {
                /*Bitu*/int len;
                for (len=0;len<37;len++) {
                    /*Bit8u*/int c=Memory.mem_readb(ptoc+739+len);
                    if (c==0 || c==0x20) break;
                }
                Memory.MEM_BlockCopy(data,ptoc+739,len);
                Memory.mem_writeb(data+len,0);
            }
            return success;
        }

        boolean GetDocumentationName(/*Bit16u*/int drive, /*PhysPt*/int data) {
            /*Bit16u*/IntRef error=new IntRef(0);
            boolean success = false;
            /*PhysPt*/int ptoc = GetTempBuffer();
            success = ReadVTOC(drive,0x00,ptoc,error);
            if (success) {
                /*Bitu*/int len;
                for (len=0;len<37;len++) {
                    /*Bit8u*/int c=Memory.mem_readb(ptoc+776+len);
                    if (c==0 || c==0x20) break;
                }
                Memory.MEM_BlockCopy(data,ptoc+776,len);
                Memory.mem_writeb(data+len,0);
            }
            return success;
        }

        boolean GetUPC(/*Bit8u*/short subUnit, /*Bit8u*/ShortRef attr, StringRef upc)
        {
            if (subUnit>=numDrives) return false;
            return dinfo[subUnit].lastResult = cdrom[subUnit].GetUPC(attr,upc);
        }

        boolean ReadSectors(/*Bit8u*/short subUnit, boolean raw, /*Bit32u*/long sector, /*Bit16u*/int num, /*PhysPt*/int data) {
            if (subUnit>=numDrives) return false;
            if ((4*num*2048+5) < CPU.CPU_Cycles) CPU.CPU_Cycles -= 4*num*2048;
            else CPU.CPU_Cycles = 5;
            dinfo[subUnit].lastResult = cdrom[subUnit].ReadSectors(data,raw,sector,num);
            return dinfo[subUnit].lastResult;
        }

        boolean ReadSectorsMSF(/*Bit8u*/short subUnit, boolean raw, /*Bit32u*/long start, /*Bit16u*/int num, /*PhysPt*/int data) {
            if (subUnit>=numDrives) return false;
            /*Bit8u*/short min		= (/*Bit8u*/short)((start>>16) & 0xFF);
            /*Bit8u*/short sec		= (/*Bit8u*/short)((start>> 8) & 0xFF);
            /*Bit8u*/short fr		= (/*Bit8u*/short)((start>> 0) & 0xFF);
            /*Bit32u*/long sector	= min*60*75+sec*75+fr - 150;
            return ReadSectors(subUnit,raw,sector,num,data);
        }

        // Called from INT 2F
        boolean ReadSectors(/*Bit16u*/int drive, /*Bit32u*/long sector, /*Bit16u*/int num, /*PhysPt*/int data) {
            return ReadSectors(GetSubUnit(drive),false,sector,num,data);
        }

        boolean GetDirectoryEntry(/*Bit16u*/int drive, boolean copyFlag, /*PhysPt*/int pathname, /*PhysPt*/int buffer, /*Bit16u*/IntRef error)
        {
            String volumeID;
            String searchName;
            String entryName;
            boolean	foundComplete = false;
            boolean	foundName;
            boolean	nextPart = true;
            String useName="";
            /*Bitu*/int entryLength,nameLength;
            // clear error
            error.value = 0;
            searchName = Memory.MEM_StrCopy(pathname+1,Memory.mem_readb(pathname)).toUpperCase();
            String searchPos = searchName;

            //strip of tailing . (XCOM APOCALYPSE)
            int searchlen = searchName.length();
            if (searchlen > 1 && searchName.indexOf("..")>=0)
                if (searchName.charAt(searchlen-1) =='.')  searchName = searchName.substring(0, searchlen-1);

            //LOG(LOG_MISC,LOG_ERROR)("MSCDEX: Get DirEntry : Find : %s",searchName);
            // read vtoc
            /*PhysPt*/int defBuffer = GetDefaultBuffer();
            if (!ReadSectors(GetSubUnit(drive),false,16,1,defBuffer)) return false;
            // TODO: has to be iso 9960
            volumeID = Memory.MEM_StrCopy(defBuffer+1,5);
            boolean iso = ("CD001".equals(volumeID));
            if (!iso) Log.exit("MSCDEX: GetDirEntry: Not an ISO 9960 CD.");
            // get directory position
            /*Bitu*/int dirEntrySector = Memory.mem_readd(defBuffer + 156 + 2);
            /*Bits*/int dirSize	= Memory.mem_readd(defBuffer + 156 + 10);
            /*Bitu*/int index;
            while (dirSize>0) {
                index = 0;
                if (!ReadSectors(GetSubUnit(drive),false,dirEntrySector,1,defBuffer)) return false;
                // Get string part
                foundName	= false;
                if (nextPart) {
                    if (searchPos.length()>0) {
                        useName = searchPos;
                        int pos = searchPos.indexOf("\\");
                        if (pos>=0)
                            searchPos = searchPos.substring(pos+1);
                        else
                            searchPos="";
                    }

                    if (searchPos.length() == 0)
                        foundComplete = true;
                }
                do {
                    entryLength = Memory.mem_readb(defBuffer+index);
                    if (entryLength==0) break;
                    nameLength  = Memory.mem_readb(defBuffer+index+32);
                    entryName = Memory.MEM_StrCopy(defBuffer+index+33,nameLength);
                    if (entryName.equals(useName)) {
                        //LOG(LOG_MISC,LOG_ERROR)("MSCDEX: Get DirEntry : Found : %s",useName);
                        foundName = true;
                        break;
                    }
                    /* Xcom Apocalipse searches for MUSIC. and expects to find MUSIC;1
                     * All Files on the CDROM are of the kind blah;1
                     */
                    int longername = entryName.indexOf(';');
                    if(longername>=0) {
                        if (entryName.substring(0, longername).equals(useName)) {
                            //LOG(LOG_MISC,LOG_ERROR)("MSCDEX: Get DirEntry : Found : %s",useName);
                            foundName = true;
                            break;
                        }
                    }
                    index += entryLength;
                } while (index+33<=2048);

                if (foundName) {
                    if (foundComplete) {
                        if (copyFlag) {
                            Log.log(LogTypes.LOG_MISC,LogSeverities.LOG_WARN,"MSCDEX: GetDirEntry: Copyflag structure not entirely accurate maybe");
                            /*Bit8u*/byte[] readBuf = new byte[256];
                            /*Bit8u*/byte[] writeBuf = new byte[256];
                            if (entryLength > 256)
                                return false;
                            Memory.MEM_BlockRead( defBuffer+index, readBuf, entryLength );
                            writeBuf[0] = readBuf[1];						// 00h	BYTE	length of XAR in Logical Block Numbers
                            System.arraycopy(readBuf, 0x2, writeBuf, 1, 4); // 01h	DWORD	Logical Block Number of file start
                            writeBuf[5] = 0;writeBuf[6] = 8;				// 05h	WORD	size of disk in logical blocks
                            System.arraycopy(readBuf, 0xa, writeBuf, 7, 4); // 07h	DWORD	file length in bytes
                            System.arraycopy(readBuf, 0x12, writeBuf, 0xb, 7);// 0bh	DWORD	date and time
                            writeBuf[0x12] = readBuf[0x19];					// 12h	BYTE	bit flags
                            writeBuf[0x13] = readBuf[0x1a];					// 13h	BYTE	interleave size
                            writeBuf[0x14] = readBuf[0x1b];					// 14h	BYTE	interleave skip factor
                            System.arraycopy(readBuf, 0x1c, writeBuf, 0x15, 2);// 15h	WORD	volume set sequence number
                            writeBuf[0x17] = readBuf[0x20];
                            System.arraycopy(readBuf, 0x21, writeBuf, 0x18, readBuf[0x20] <= 38 ? readBuf[0x20] : 38); // :TODO: changed 21 to 0x21
                            Memory.MEM_BlockWrite( buffer, writeBuf, 0x18 + 40 );
                        } else {
                            // Direct copy
                            Memory.MEM_BlockCopy(buffer,defBuffer+index,entryLength);
                        }
                        error.value = iso ? 1:0;
                        return true;
                    }
                    // change directory
                    dirEntrySector = Memory.mem_readd(defBuffer + index + 2);
                    dirSize	= Memory.mem_readd(defBuffer + index + 10);
                    nextPart = true;
                } else {
                    // continue search in next sector
                    dirSize -= 2048;
                    dirEntrySector++;
                    nextPart = false;
                }
            }
            error.value = 2; // file not found
            return false; // not found
        }

        boolean GetCurrentPos(/*Bit8u*/short subUnit, Dos_cdrom.TMSF pos)
        {
            if (subUnit>=numDrives) return false;
            Dos_cdrom.TMSF rel=new Dos_cdrom.TMSF();
            /*Bit8u*/ShortRef attr=new ShortRef(0),track=new ShortRef(0),index=new ShortRef(0);
            dinfo[subUnit].lastResult = GetSubChannelData(subUnit, attr, track, index, rel, pos);
            if (!dinfo[subUnit].lastResult) pos.clear();
            return dinfo[subUnit].lastResult;
        }

        boolean GetMediaStatus(/*Bit8u*/short subUnit, BooleanRef media, BooleanRef changed, BooleanRef trayOpen)
        {
            if (subUnit>=numDrives) return false;
            dinfo[subUnit].lastResult = cdrom[subUnit].GetMediaTrayStatus(media,changed,trayOpen);
            return dinfo[subUnit].lastResult;
        }

        /*Bit32u*/long GetDeviceStatus(/*Bit8u*/short subUnit)
        {
            if (subUnit>=numDrives) return 0;
            BooleanRef media=new BooleanRef(),changed=new BooleanRef(),trayOpen=new BooleanRef();

            dinfo[subUnit].lastResult = GetMediaStatus(subUnit,media,changed,trayOpen);

            if (dinfo[subUnit].audioPlay) {
                // Check if audio is still playing....
                Dos_cdrom.TMSF start = new Dos_cdrom.TMSF(),end = new Dos_cdrom.TMSF();
                BooleanRef playing=new BooleanRef(),pause=new BooleanRef();
                if (GetAudioStatus(subUnit,playing,pause,start,end))
                    dinfo[subUnit].audioPlay = playing.value;
                else
                    dinfo[subUnit].audioPlay = false;
            }

            /*Bit32u*/long status = ((trayOpen.value?1:0) << 0)		|	// Drive is open ?
                            ((dinfo[subUnit].locked?1:0) << 1)		|	// Drive is locked ?
                            (1<<2)									|	// raw + cooked sectors
                            (1<<4)									|	// Can read sudio
                            (1<<8)									|	// Can control audio
                            (1<<9)									|	// Red book & HSG
                            ((dinfo[subUnit].audioPlay?1:0) << 10)	|	// Audio is playing ?
                            ((media.value?0:1) << 11);					// Drive is empty ?
            return status;
        }

        boolean GetMediaStatus(/*Bit8u*/short subUnit, /*Bit8u*/ShortRef status)
        {
            if (subUnit>=numDrives) return false;
        /*	boolean media,changed,open,result;
            result = GetMediaStatus(subUnit,media,changed,open);
            status = changed ? 0xFF : 0x01;
            return result; */
            status.value = (short)(Bios_disk.getSwapRequest() ? 0xFF : 0x01);
            return true;
        }

        boolean LoadUnloadMedia(/*Bit8u*/short subUnit, boolean unload)
        {
            if (subUnit>=numDrives) return false;
            dinfo[subUnit].lastResult = cdrom[subUnit].LoadUnloadMedia(unload);
            return dinfo[subUnit].lastResult;
        }

        boolean SendDriverRequest(/*Bit16u*/int drive, /*PhysPt*/int data)
        {
            /*Bit8u*/short subUnit = GetSubUnit(drive);
            if (subUnit>=numDrives) return false;
            // Get SubUnit
            Memory.mem_writeb(data+1,subUnit);
            // Call Strategy / Interrupt
            MSCDEX_Strategy_Handler.call();
            MSCDEX_Interrupt_Handler.call();
            return true;
        }

        /*Bit16u*/int GetStatusWord(/*Bit8u*/short subUnit,/*Bit16u*/int status)
        {
            if (subUnit>=numDrives) return REQUEST_STATUS_ERROR | 0x02; // error : Drive not ready

            if (dinfo[subUnit].lastResult)	status |= REQUEST_STATUS_DONE;				// ok
            else							status |= REQUEST_STATUS_ERROR;

            if (dinfo[subUnit].audioPlay) {
                // Check if audio is still playing....
                Dos_cdrom.TMSF start=new Dos_cdrom.TMSF(),end=new Dos_cdrom.TMSF();
                BooleanRef playing=new BooleanRef(),pause=new BooleanRef();
                if (GetAudioStatus(subUnit,playing,pause,start,end)) {
                    dinfo[subUnit].audioPlay = playing.value;
                } else
                    dinfo[subUnit].audioPlay = false;

                status |= ((dinfo[subUnit].audioPlay?1:0)<<9);
            }
            dinfo[subUnit].lastResult	= true;
            return status;
        }

        void InitNewMedia(/*Bit8u*/short subUnit) {
            if (subUnit<numDrives) {
                // Reopen new media
                cdrom[subUnit].InitNewMedia();
            }
        }

        boolean ChannelControl(/*Bit8u*/int subUnit, Dos_cdrom.TCtrl ctrl) {
            if (subUnit>=numDrives) return false;
            // adjust strange channel mapping
            if (ctrl.out[0]>1) ctrl.out[0]=0;
            if (ctrl.out[1]>1) ctrl.out[1]=1;
            dinfo[subUnit].audioCtrl=ctrl;
            cdrom[subUnit].ChannelControl(ctrl);
            return true;
        }

        boolean GetChannelControl(/*Bit8u*/int subUnit, Dos_cdrom.TCtrl ctrl) {
            if (subUnit>=numDrives) return false;
            ctrl.copy(dinfo[subUnit].audioCtrl);
            return true;
        }
    }

    private static CMscdex mscdex = null;
    private static /*PhysPt*/int curReqheaderPtr = 0;

    private static /*Bit16u*/int MSCDEX_IOCTL_Input(/*PhysPt*/int buffer,/*Bit8u*/short drive_unit) {
        /*Bitu*/int ioctl_fct = Memory.mem_readb(buffer);
        if (Log.level<=LogSeverities.LOG_NORMAL) Log.log(LogTypes.LOG_MISC,LogSeverities.LOG_NORMAL, "MSCDEX: IOCTL INPUT Subfunction "+Integer.toString(ioctl_fct,16));
        switch (ioctl_fct) {
            case 0x00 : /* Get Device Header address */
                        Memory.mem_writed(buffer+1,Memory.RealMake(mscdex.rootDriverHeaderSeg,0));
                        break;
            case 0x01 :{/* Get current position */
                        Dos_cdrom.TMSF pos=new Dos_cdrom.TMSF();
                        mscdex.GetCurrentPos(drive_unit,pos);
                        /*Bit8u*/int addr_mode = Memory.mem_readb(buffer+1);
                        if (addr_mode==0) {			// HSG
                            /*Bit32u*/long frames=pos.min*60*Dos_cdrom.CD_FPS+ pos.sec*Dos_cdrom.CD_FPS+pos.fr;
                            if (frames<150) if (Log.level<=LogSeverities.LOG_ERROR) Log.log(LogTypes.LOG_MISC,LogSeverities.LOG_ERROR, "MSCDEX: Get position: invalid position "+pos.min+":"+pos.sec+":"+pos.fr);
                            else frames-=150;
                            Memory.mem_writed(buffer+2,(int)frames);
                        } else if (addr_mode==1) {	// Red book
                            Memory.mem_writeb(buffer+2,pos.fr);
                            Memory.mem_writeb(buffer+3,pos.sec);
                            Memory.mem_writeb(buffer+4,pos.min);
                            Memory.mem_writeb(buffer+5,0x00);
                        } else {
                            if (Log.level<=LogSeverities.LOG_ERROR) Log.log(LogTypes.LOG_MISC,LogSeverities.LOG_ERROR, "MSCDEX: Get position: invalid address mode "+Integer.toString(addr_mode,16));
                            return 0x03;		// invalid function
                        }
                       }break;
            case 0x04 : /* Audio Channel control */
					Dos_cdrom.TCtrl ctrl = new Dos_cdrom.TCtrl();
					if (!mscdex.GetChannelControl(drive_unit,ctrl)) return 0x01;
					for (/*Bit8u*/int chan=0;chan<4;chan++) {
						Memory.mem_writeb(buffer + chan * 2 + 1, ctrl.out[chan]);
						Memory.mem_writeb(buffer + chan * 2 + 2, ctrl.vol[chan]);
					}
					break;
            case 0x06 : /* Get Device status */
                        Memory.mem_writed(buffer+1,(int)mscdex.GetDeviceStatus(drive_unit));
                        break;
            case 0x07 : /* Get sector size */
                        if (Memory.mem_readb(buffer+1)==0) Memory.mem_writed(buffer+2,2048);
                        else if (Memory.mem_readb(buffer+1)==1) Memory.mem_writed(buffer+2,2352);
                        else return 0x03;		// invalid function
                        break;
            case 0x08 : /* Get size of current volume */
                        Memory.mem_writed(buffer+1,(int)mscdex.GetVolumeSize(drive_unit));
                        break;
            case 0x09 : /* Media change ? */
                        /*Bit8u*/ShortRef status = new ShortRef();
                        if (!mscdex.GetMediaStatus(drive_unit,status)) {
                            status.value = 0;		// state unknown
                        }
                        Memory.mem_writeb(buffer+1,status.value);
                        break;
            case 0x0A : /* Get Audio Disk info */
                        /*Bit8u*/ShortRef tr1=new ShortRef(),tr2=new ShortRef(); Dos_cdrom.TMSF leadOut=new Dos_cdrom.TMSF();
                        if (!mscdex.GetCDInfo(drive_unit,tr1,tr2,leadOut)) return 0x05;
                        Memory.mem_writeb(buffer+1,tr1.value);
                        Memory.mem_writeb(buffer+2,tr2.value);
                        Memory.mem_writeb(buffer+3,leadOut.fr);
                        Memory.mem_writeb(buffer+4,leadOut.sec);
                        Memory.mem_writeb(buffer+5,leadOut.min);
                        Memory.mem_writeb(buffer+6,0x00);
                        break;
            case 0x0B :{/* Audio Track Info */
                        /*Bit8u*/ShortRef attr=new ShortRef(); Dos_cdrom.TMSF start=new Dos_cdrom.TMSF();
                        /*Bit8u*/int track = Memory.mem_readb(buffer+1);
                        mscdex.GetTrackInfo(drive_unit,(short)track,attr,start);
                        Memory.mem_writeb(buffer+2,start.fr);
                        Memory.mem_writeb(buffer+3,start.sec);
                        Memory.mem_writeb(buffer+4,start.min);
                        Memory.mem_writeb(buffer+5,0x00);
                        Memory.mem_writeb(buffer+6,attr.value);
                        break; }
            case 0x0C :{/* Get Audio Sub Channel data */
                        /*Bit8u*/ShortRef attr=new ShortRef(),track=new ShortRef(),index=new ShortRef();
                        Dos_cdrom.TMSF abs=new Dos_cdrom.TMSF(),rel=new Dos_cdrom.TMSF();
                        mscdex.GetSubChannelData(drive_unit,attr,track,index,rel,abs);
                        Memory.mem_writeb(buffer+1,attr.value);
                        Memory.mem_writeb(buffer+2,track.value);
                        Memory.mem_writeb(buffer+3,index.value);
                        Memory.mem_writeb(buffer+4,rel.min);
                        Memory.mem_writeb(buffer+5,rel.sec);
                        Memory.mem_writeb(buffer+6,rel.fr);
                        Memory.mem_writeb(buffer+7,0x00);
                        Memory.mem_writeb(buffer+8,abs.min);
                        Memory.mem_writeb(buffer+9,abs.sec);
                        Memory.mem_writeb(buffer+10,abs.fr);
                        break;
                       }
            case 0x0E :{ /* Get UPC */
                        /*Bit8u*/ShortRef attr=new ShortRef(); StringRef upc=new StringRef();
                        mscdex.GetUPC(drive_unit,attr,upc);
                        Memory.mem_writeb(buffer+1,attr.value);
                        for (int i=0; i<7; i++) Memory.mem_writeb(buffer+2+i,upc.value.charAt(i));
                        Memory.mem_writeb(buffer+9,0x00);
                        break;
                       }
            case 0x0F :{ /* Get Audio Status */
                        BooleanRef playing=new BooleanRef(),pause=new BooleanRef();
                        Dos_cdrom.TMSF resStart=new Dos_cdrom.TMSF(),resEnd=new Dos_cdrom.TMSF();
                        mscdex.GetAudioStatus(drive_unit,playing,pause,resStart,resEnd);
                        Memory.mem_writeb(buffer+1,pause.value?1:0);
                        Memory.mem_writeb(buffer+3,resStart.min);
                        Memory.mem_writeb(buffer+4,resStart.sec);
                        Memory.mem_writeb(buffer+5,resStart.fr);
                        Memory.mem_writeb(buffer+6,0x00);
                        Memory.mem_writeb(buffer+7,resEnd.min);
                        Memory.mem_writeb(buffer+8,resEnd.sec);
                        Memory.mem_writeb(buffer+9,resEnd.fr);
                        Memory.mem_writeb(buffer+10,0x00);
                        break;
                       }
            default :	if (Log.level<=LogSeverities.LOG_ERROR) Log.log(LogTypes.LOG_MISC,LogSeverities.LOG_ERROR,"MSCDEX: Unsupported IOCTL INPUT Subfunction "+Integer.toString(ioctl_fct,16));
                        return 0x03;	// invalid function
        }
        return 0x00;	// success
    }

    private static /*Bit16u*/int MSCDEX_IOCTL_Optput(/*PhysPt*/int buffer,/*Bit8u*/short drive_unit) {
        /*Bitu*/int ioctl_fct = Memory.mem_readb(buffer);
    //	Log.log(LogTypes.LOG_MISC,LogSeverities.LOG_ERROR,("MSCDEX: IOCTL OUTPUT Subfunction %02X",ioctl_fct);
        switch (ioctl_fct) {
            case 0x00 :	// Unload /eject media
                        if (!mscdex.LoadUnloadMedia(drive_unit,true)) return 0x02;
                        break;
            case 0x03: //Audio Channel control
                        Dos_cdrom.TCtrl ctrl = new Dos_cdrom.TCtrl();
                        for (/*Bit8u*/int chan=0;chan<4;chan++) {
                            ctrl.out[chan]=Memory.mem_readb(buffer+chan*2+1);
                            ctrl.vol[chan]=Memory.mem_readb(buffer+chan*2+2);
                        }
                        if (!mscdex.ChannelControl(drive_unit,ctrl)) return 0x01;
                        break;
            case 0x01 : // (un)Lock door
                        // do nothing . report as success
                        break;
            case 0x02 : // Reset Drive
                        Log.log(LogTypes.LOG_MISC,LogSeverities.LOG_WARN,"cdromDrive reset");
                        if (!mscdex.StopAudio(drive_unit))  return 0x02;
                        break;
            case 0x05 :	// load media
                        if (!mscdex.LoadUnloadMedia(drive_unit,false)) return 0x02;
                        break;
            default	:	if (Log.level<=LogSeverities.LOG_ERROR) Log.log(LogTypes.LOG_MISC,LogSeverities.LOG_ERROR,"MSCDEX: Unsupported IOCTL OUTPUT Subfunction "+Integer.toString(ioctl_fct,16));
                        return 0x03;	// invalid function
        }
        return 0x00;	// success
    }

    static private Callback.Handler MSCDEX_Strategy_Handler = new Callback.Handler() {
        public String getName() {
            return "MSCDEX_Strategy_Handler";
        }
        public /*Bitu*/int call() {
            curReqheaderPtr = Memory.PhysMake((int)CPU_Regs.reg_esVal.dword, CPU_Regs.reg_ebx.word());
        //	Log.log(LogTypes.LOG_MISC,LogSeverities.LOG_ERROR,("MSCDEX: Device Strategy Routine called, request header at %x",curReqheaderPtr);
            return Callback.CBRET_NONE;
        }
    };

    static private Callback.Handler MSCDEX_Interrupt_Handler = new Callback.Handler() {
        public String getName() {
            return "MSCDEX_Interrupt_Handler";
        }
        public /*Bitu*/int call() {
            if (curReqheaderPtr==0) {
                Log.log(LogTypes.LOG_MISC,LogSeverities.LOG_ERROR,"MSCDEX: invalid call to interrupt handler");
                return Callback.CBRET_NONE;
            }
            /*Bit8u*/short	subUnit		= (short)Memory.mem_readb(curReqheaderPtr+1);
            /*Bit8u*/short	funcNr		= (short)Memory.mem_readb(curReqheaderPtr+2);
            /*Bit16u*/int	errcode		= 0;
            /*PhysPt*/int	buffer		= 0;

            if (Log.level<=LogSeverities.LOG_NORMAL)Log.log(LogTypes.LOG_MISC,LogSeverities.LOG_NORMAL, "MSCDEX: Driver Function "+Integer.toString(funcNr,16));

            if ((funcNr==0x03) || (funcNr==0x0c) || (funcNr==0x80) || (funcNr==0x82)) {
                buffer = Memory.PhysMake(Memory.mem_readw(curReqheaderPtr+0x10),Memory.mem_readw(curReqheaderPtr+0x0E));
            }

            switch (funcNr) {
                case 0x03	: {	/* IOCTL INPUT */
                                /*Bit16u*/int error=MSCDEX_IOCTL_Input(buffer,subUnit);
                                if (error!=0) errcode = error;
                                break;
                              }
                case 0x0C	: {	/* IOCTL OUTPUT */
                                /*Bit16u*/int error=MSCDEX_IOCTL_Optput(buffer,subUnit);
                                if (error!=0) errcode = error;
                                break;
                              }
                case 0x0D	:	// device open
                case 0x0E	:	// device close - dont care :)
                                break;
                case 0x80	:	// Read long
                case 0x82	: { // Read long prefetch . both the same here :)
                                /*Bit32u*/long start = Memory.mem_readd(curReqheaderPtr + 0x14) & 0xFFFFFFFFl;
                                /*Bit16u*/int len	 = Memory.mem_readw(curReqheaderPtr+0x12);
                                boolean raw	 = (Memory.mem_readb(curReqheaderPtr+0x18)==1);
                                if (Memory.mem_readb(curReqheaderPtr+0x0D)==0x00) // HSG
                                    mscdex.ReadSectors(subUnit,raw,start,len,buffer);
                                else
                                    mscdex.ReadSectorsMSF(subUnit,raw,start,len,buffer);
                                break;
                              }
                case 0x83	:	// Seek - dont care :)
                                break;
                case 0x84	: {	/* Play Audio Sectors */
                                /*Bit32u*/long start = Memory.mem_readd(curReqheaderPtr + 0x0E) & 0xFFFFFFFFl;
                                /*Bit32u*/long len	 = Memory.mem_readd(curReqheaderPtr + 0x12) & 0xFFFFFFFFl;
                                if (Memory.mem_readb(curReqheaderPtr+0x0D)==0x00) // HSG
                                    mscdex.PlayAudioSector(subUnit,start,len);
                                else // RED BOOK
                                    mscdex.PlayAudioMSF(subUnit,start,len);
                                break;
                              }
                case 0x85	:	/* Stop Audio */
                                mscdex.StopAudio(subUnit);
                                break;
                case 0x88	:	/* Resume Audio */
                                mscdex.ResumeAudio(subUnit);
                                break;
                default		:	if (Log.level<=LogSeverities.LOG_ERROR) Log.log(LogTypes.LOG_MISC,LogSeverities.LOG_ERROR,"MSCDEX: Unsupported Driver Request "+Integer.toString(funcNr,16));
                                break;

            }

            // Set Statusword
            Memory.mem_writew(curReqheaderPtr+3,mscdex.GetStatusWord(subUnit,errcode));
            if (Log.level<=LogSeverities.LOG_NORMAL)Log.log(LogTypes.LOG_MISC,LogSeverities.LOG_NORMAL,"MSCDEX: Status : "+Integer.toString(Memory.mem_readw(curReqheaderPtr+3),16));
            return Callback.CBRET_NONE;
        }
    };

    static private Dos_system.MultiplexHandler MSCDEX_Handler = new Dos_system.MultiplexHandler() {
        public boolean call() {
            if(CPU_Regs.reg_eax.high() == 0x11) {
                if(CPU_Regs.reg_eax.low() == 0x00) {
                    /*PhysPt*/int check = Memory.PhysMake((int)CPU_Regs.reg_ssVal.dword,CPU_Regs.reg_esp.word());
                    if(Memory.mem_readw(check+6) == 0xDADA) {
                        //MSCDEX sets word on stack to ADAD if it DADA on entry.
                        Memory.mem_writew(check+6,0xADAD);
                    }
                    CPU_Regs.reg_eax.low(0xff);
                    return true;
                } else {
                    Log.log(LogTypes.LOG_MISC,LogSeverities.LOG_ERROR,"NETWORK REDIRECTOR USED!!!");
                    CPU_Regs.reg_eax.word(0x49);//NETWERK SOFTWARE NOT INSTALLED
                    Callback.CALLBACK_SCF(true);
                    return true;
                }
            }

            if (CPU_Regs.reg_eax.high()!=0x15) return false;		// not handled here, continue chain

            /*PhysPt*/int data = Memory.PhysMake((int)CPU_Regs.reg_esVal.dword,CPU_Regs.reg_ebx.word());
            if (Log.level<=LogSeverities.LOG_NORMAL) Log.log(LogTypes.LOG_MISC,LogSeverities.LOG_NORMAL,"MSCDEX: INT 2F "+Integer.toString(CPU_Regs.reg_eax.word(), 16)+" BX= "+Integer.toString(CPU_Regs.reg_ebx.word(), 16)+" CX="+Integer.toString(CPU_Regs.reg_ecx.word(),16));
            switch (CPU_Regs.reg_eax.word()) {

                case 0x1500:	/* Install check */
                                CPU_Regs.reg_ebx.word(mscdex.GetNumDrives());
                                if (CPU_Regs.reg_ebx.word()>0) CPU_Regs.reg_ecx.word(mscdex.GetFirstDrive());
                                CPU_Regs.reg_eax.low(0xff);
                                return true;
                case 0x1501:	/* Get cdrom driver info */
                                mscdex.GetDriverInfo(data);
                                return true;
                case 0x1502:	/* Get Copyright filename */
                                if (mscdex.GetCopyrightName(CPU_Regs.reg_ecx.word(),data)) {
                                    Callback.CALLBACK_SCF(false);
                                } else {
                                    CPU_Regs.reg_eax.word(MSCDEX_ERROR_UNKNOWN_DRIVE);
                                    Callback.CALLBACK_SCF(true);
                                }
                                return true;
                case 0x1503:	/* Get Abstract filename */
                                if (mscdex.GetAbstractName(CPU_Regs.reg_ecx.word(),data)) {
                                    Callback.CALLBACK_SCF(false);
                                } else {
                                    CPU_Regs.reg_eax.word(MSCDEX_ERROR_UNKNOWN_DRIVE);
                                    Callback.CALLBACK_SCF(true);
                                }
                                return true;
                case 0x1504:	/* Get Documentation filename */
                                if (mscdex.GetDocumentationName(CPU_Regs.reg_ecx.word(),data)) {
                                    Callback.CALLBACK_SCF(false);
                                } else {
                                    CPU_Regs.reg_eax.word(MSCDEX_ERROR_UNKNOWN_DRIVE);
                                    Callback.CALLBACK_SCF(true);
                                }
                                return true;
                case 0x1505: {	// read vtoc
                                /*Bit16u*/IntRef error = new IntRef(0);
                                if (mscdex.ReadVTOC(CPU_Regs.reg_ecx.word(),CPU_Regs.reg_edx.word(),data,error)) {
        //							CPU_Regs.reg_ax = error;	// return code
                                    Callback.CALLBACK_SCF(false);
                                } else {
                                    CPU_Regs.reg_eax.word(error.value);
                                    Callback.CALLBACK_SCF(true);
                                }
                             }
                                return true;
                case 0x1508: {	// read sectors
                                /*Bit32u*/long sector = (CPU_Regs.reg_esi.word()<<16)+CPU_Regs.reg_edi.word();
                                if (mscdex.ReadSectors(CPU_Regs.reg_ecx.word(),sector,CPU_Regs.reg_edx.word(),data)) {
                                    CPU_Regs.reg_eax.word(0);
                                    Callback.CALLBACK_SCF(false);
                                } else {
                                    // possibly: MSCDEX_ERROR_DRIVE_NOT_READY if sector is beyond total length
                                    CPU_Regs.reg_eax.word(MSCDEX_ERROR_UNKNOWN_DRIVE);
                                    Callback.CALLBACK_SCF(true);
                                }
                                return true;
                             }
                case 0x1509:	// write sectors - not supported
                                CPU_Regs.reg_eax.word(MSCDEX_ERROR_INVALID_FUNCTION);
                                Callback.CALLBACK_SCF(true);
                                return true;
                case 0x150B:	/* Valid CDROM drive ? */
                                CPU_Regs.reg_eax.word((mscdex.IsValidDrive(CPU_Regs.reg_ecx.word()) ? 0x5ad8 : 0x0000));
                                CPU_Regs.reg_ebx.word(0xADAD);
                                return true;
                case 0x150C:	/* Get MSCDEX Version */
                                CPU_Regs.reg_ebx.word(mscdex.GetVersion());
                                return true;
                case 0x150D:	/* Get drives */
                                mscdex.GetDrives(data);
                                return true;
                case 0x150E:	/* Get/Set Volume Descriptor Preference */
                                if (mscdex.IsValidDrive(CPU_Regs.reg_ecx.word())) {
                                    if (CPU_Regs.reg_ebx.word() == 0) {
                                        // get preference
                                        CPU_Regs.reg_edx.word(0x100);	// preference?
                                        Callback.CALLBACK_SCF(false);
                                    } else if (CPU_Regs.reg_ebx.word() == 1) {
                                        // set preference
                                        if (CPU_Regs.reg_edx.high() == 1) {
                                            // valid
                                            Callback.CALLBACK_SCF(false);
                                        } else {
                                            CPU_Regs.reg_eax.word(MSCDEX_ERROR_INVALID_FUNCTION);
                                            Callback.CALLBACK_SCF(true);
                                        }
                                    } else {
                                        CPU_Regs.reg_eax.word(MSCDEX_ERROR_INVALID_FUNCTION);
                                        Callback.CALLBACK_SCF(true);
                                    }
                                } else {
                                    CPU_Regs.reg_eax.word(MSCDEX_ERROR_UNKNOWN_DRIVE);
                                    Callback.CALLBACK_SCF(true);
                                }
                                return true;
                case 0x150F: {	// Get directory entry
                                /*Bit16u*/IntRef error = new IntRef(0);
                                boolean success = mscdex.GetDirectoryEntry(CPU_Regs.reg_ecx.low(),(CPU_Regs.reg_ecx.high()&1)!=0,data,Memory.PhysMake(CPU_Regs.reg_esi.word(),CPU_Regs.reg_edi.word()),error);
                                CPU_Regs.reg_eax.word(error.value);
                                Callback.CALLBACK_SCF(!success);
                             }	return true;
                case 0x1510:	/* Device driver request */
                                if (mscdex.SendDriverRequest(CPU_Regs.reg_ecx.word(),data)) {
                                    Callback.CALLBACK_SCF(false);
                                } else {
                                    CPU_Regs.reg_eax.word(MSCDEX_ERROR_UNKNOWN_DRIVE);
                                    Callback.CALLBACK_SCF(true);
                                }
                                return true;
            }
            if (Log.level<=LogSeverities.LOG_ERROR) Log.log(LogTypes.LOG_MISC,LogSeverities.LOG_ERROR,"MSCDEX: Unknwon call : "+Integer.toString(CPU_Regs.reg_eax.word(),16));
            return true;
        }
    };

    static private class device_MSCDEX extends DOS_Device {
        device_MSCDEX() { SetName("MSCD001"); }
        public boolean Read(byte[] data,/*Bit16u*/IntRef size) { return false;}
        public boolean Write(byte[] data,/*Bit16u*/IntRef size) {
            Log.log(LogTypes.LOG_ALL,LogSeverities.LOG_NORMAL,"Write to mscdex device");
            return false;
        }
        public boolean Seek(/*Bit32u*/LongRef pos,/*Bit32u*/int type) {return false;}
        public boolean Close() {return false;}
        public /*Bit16u*/int GetInformation() {return 0xc880;}
        public boolean ReadFromControlChannel(/*PhysPt*/int bufptr,/*Bit16u*/int size,/*Bit16u*/IntRef retcode) {
            if (MSCDEX_IOCTL_Input(bufptr,(short)0)==0) {
                retcode.value=size;
                return true;
            }
            return false;
        }
        public boolean WriteToControlChannel(/*PhysPt*/int bufptr,/*Bit16u*/int size,/*Bit16u*/IntRef retcode) {
            if (MSCDEX_IOCTL_Optput(bufptr,(short)0)==0) {
                retcode.value=size;
                return true;
            }
            return false;
        }
    }

    static public int MSCDEX_AddDrive(char driveLetter, String physicalPath, /*Bit8u*/ShortRef subUnit)
    {
        return mscdex.AddDrive(driveLetter-'A',physicalPath,subUnit);
    }

    static public int MSCDEX_RemoveDrive(char driveLetter)
    {
        if(mscdex==null) return 0;
        return mscdex.RemoveDrive(driveLetter-'A');
    }

    static public boolean MSCDEX_HasDrive(char driveLetter)
    {
        return mscdex.HasDrive(driveLetter-'A');
    }

    static public void MSCDEX_ReplaceDrive(Dos_cdrom.CDROM_Interface cdrom, /*Bit8u*/short subUnit)
    {
        mscdex.ReplaceDrive(cdrom, subUnit);
    }

    static public boolean MSCDEX_GetVolumeName(/*Bit8u*/short subUnit, StringRef name)
    {
        return mscdex.GetVolumeName(subUnit,name);
    }

    static private Dos_cdrom.TMSF[] leadOut = new Dos_cdrom.TMSF[MSCDEX_MAX_DRIVES];

    static {
        for (int i=0;i<leadOut.length;i++)
            leadOut[i] = new Dos_cdrom.TMSF();
    }

    static public boolean MSCDEX_HasMediaChanged(/*Bit8u*/short subUnit)
    {
        Dos_cdrom.TMSF leadnew=new Dos_cdrom.TMSF();
        /*Bit8u*/ShortRef tr1=new ShortRef(),tr2=new ShortRef();
        if (mscdex.GetCDInfo(subUnit,tr1,tr2,leadnew)) {
            boolean changed = (leadOut[subUnit].min!=leadnew.min) || (leadOut[subUnit].sec!=leadnew.sec) || (leadOut[subUnit].fr!=leadnew.fr);
            if (changed) {
                leadOut[subUnit].min = leadnew.min;
                leadOut[subUnit].sec = leadnew.sec;
                leadOut[subUnit].fr	 = leadnew.fr;
                mscdex.InitNewMedia(subUnit);
            }
            return changed;
        }
        if (subUnit<MSCDEX_MAX_DRIVES) {
            leadOut[subUnit].min = 0;
            leadOut[subUnit].sec = 0;
            leadOut[subUnit].fr	 = 0;
        }
        return true;
    }

    static public void MSCDEX_SetCDInterface(int intNr, int numCD) {
        //useCdromInterface = intNr;
        forceCD	= numCD;
    }

    public static Section.SectionFunction MSCDEX_ShutDown = new Section.SectionFunction() {
        public void call(Section section) {
            mscdex = null;
            curReqheaderPtr = 0;
        }
    };

    public static Section.SectionFunction MSCDEX_Init = new Section.SectionFunction() {
        public void call(Section section) {
            // AddDestroy func
            section.AddDestroyFunction(MSCDEX_ShutDown);
            /* Register the mscdex device */
            DOS_Device newdev = new device_MSCDEX();
            Dos_devices.DOS_AddDevice(newdev);
            curReqheaderPtr = 0;
            /* Add Multiplexer */
            Dos_misc.DOS_AddMultiplexHandler(MSCDEX_Handler);
            /* Create MSCDEX */
            mscdex = new CMscdex();
        }
    };
}
