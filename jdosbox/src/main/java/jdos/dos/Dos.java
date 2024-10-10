package jdos.dos;

import jdos.cpu.*;
import jdos.hardware.IO;
import jdos.hardware.Memory;
import jdos.hardware.Timer;
import jdos.hardware.serialport.Serialports;
import jdos.ints.Bios;
import jdos.misc.Log;
import jdos.misc.setup.Module_base;
import jdos.misc.setup.Section;
import jdos.types.LogSeverities;
import jdos.types.LogTypes;
import jdos.util.IntRef;
import jdos.util.LongRef;
import jdos.util.ShortRef;
import jdos.util.StringRef;

public class Dos extends Module_base {
    static final public int DOS_INFOBLOCK_SEG = 0x80;	// sysvars (list of lists)
    static final public int DOS_CONDRV_SEG = 0xa0;
    static final public int DOS_CONSTRING_SEG = 0xa8;
    static final public int DOS_SDA_SEG = 0xb2;		// dos swappable area
    static final public int DOS_SDA_OFS = 0;
    static final public int DOS_CDS_SEG = 0x108;
    static final public int DOS_FIRST_SHELL = 0x118;
    static final public int DOS_MEM_START = 0x16f;		//First Segment that DOS can use

    /* Dos Error Codes */
    static final public int DOSERR_NONE = 0;
    static final public int DOSERR_FUNCTION_NUMBER_INVALID = 1;
    static final public int DOSERR_FILE_NOT_FOUND = 2;
    static final public int DOSERR_PATH_NOT_FOUND = 3;
    static final public int DOSERR_TOO_MANY_OPEN_FILES = 4;
    static final public int DOSERR_ACCESS_DENIED = 5;
    static final public int DOSERR_INVALID_HANDLE = 6;
    static final public int DOSERR_MCB_DESTROYED = 7;
    static final public int DOSERR_INSUFFICIENT_MEMORY = 8;
    static final public int DOSERR_MB_ADDRESS_INVALID = 9;
    static final public int DOSERR_ENVIRONMENT_INVALID = 10;
    static final public int DOSERR_FORMAT_INVALID = 11;
    static final public int DOSERR_ACCESS_CODE_INVALID = 12;
    static final public int DOSERR_DATA_INVALID = 13;
    static final public int DOSERR_RESERVED = 14;
    static final public int DOSERR_FIXUP_OVERFLOW = 14;
    static final public int DOSERR_INVALID_DRIVE = 15;
    static final public int DOSERR_REMOVE_CURRENT_DIRECTORY = 16;
    static final public int DOSERR_NOT_SAME_DEVICE = 17;
    static final public int DOSERR_NO_MORE_FILES = 18;
    static final public int DOSERR_FILE_ALREADY_EXISTS = 80;

    static public Dos_Block dos = new Dos_Block();
    static public Dos_InfoBlock dos_infoblock = new Dos_InfoBlock();

    static final private int DOS_COPYBUFSIZE = 0x10000;
    static byte[] dos_copybuf = new byte[DOS_COPYBUFSIZE];

    public static void DOS_SetError(/*Bit16u*/int code) {
        dos.errorcode=code;
    }

    static final byte DOS_DATE_months[] = {
        0, 31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31
    };

    static void DOS_AddDays(short days) {
        dos.date.day += days;
        /*Bit8u*/byte monthlimit = DOS_DATE_months[dos.date.month];

        if(dos.date.day > monthlimit) {
            if((dos.date.year %4 == 0) && (dos.date.month==2)) {
                // leap year
                if(dos.date.day > 29) {
                    dos.date.month++;
                    dos.date.day -= 29;
                }
            } else {
                //not leap year
                dos.date.month++;
                dos.date.day -= monthlimit;
            }
            if(dos.date.month > 12) {
                // year over
                dos.date.month = 1;
                dos.date.year++;
            }
        }
    }

    static final private boolean DATA_TRANSFERS_TAKE_CYCLES  = true;
    static final private boolean DOS_OVERHEAD = true;

    static public /*Bit16u*/int DOS_PackTime(/*Bit16u*/int hour,/*Bit16u*/int min,/*Bit16u*/int sec) {
        return (hour&0x1f)<<11 | (min&0x3f) << 5 | ((sec/2)&0x1f);
    }

    static public /*Bit16u*/int DOS_PackDate(/*Bit16u*/int year,/*Bit16u*/int mon,/*Bit16u*/int day) {
        return ((year-1980)&0x7f)<<9 | (mon&0x3f) << 5 | (day&0x1f);
    }

    static public /*Bit8u*/int RealHandle(/*Bit16u*/int handle) {
        Dos_PSP psp=new Dos_PSP(dos.psp());
        return psp.GetFileHandle(handle);
    }
    
    static private void modify_cycles(/*Bits*/int value) {
        if (DATA_TRANSFERS_TAKE_CYCLES) {
            if((4*value+5) < CPU.CPU_Cycles) {
                CPU.CPU_Cycles -= 4*value;
                CPU.CPU_IODelayRemoved += 4*value;
            } else {
                CPU.CPU_IODelayRemoved += CPU.CPU_Cycles/*-5*/; //don't want to mess with negative
                CPU.CPU_Cycles = 5;
            }
        }
    }

    static private void overhead() {
        if (DOS_OVERHEAD)
            CPU_Regs.reg_ip(CPU_Regs.reg_ip()+2);
    }

    static private Callback.Handler DOS_21Handler = new Callback.Handler() {
        long time_start = 0; //For emulating temporary time changes.

        public String getName() {
            String result = "";
            switch (CPU_Regs.reg_eax.high() & 0xFF) {
                case 0x00:
                    result = "Terminate Program";
                    break;
                case 0x01:
                    result = "Read character from STDIN, with echo";
                    break;
                case 0x02:
                    result = "Write character to STDOUT";
                    break;
                case 0x03:
                    result = "Read character from STDAUX";
                    break;
                case 0x04:
                    result = "Write Character to STDAUX";
                    break;
                case 0x05:
                    result = "Write Character to PRINTER";
                    break;
                case 0x06:
                    result = "Direct Console Output / Input";
                    break;
                case 0x07:
                    result = "Character Input, without echo";
                    break;
                case 0x08:
                    result = "Direct Character Input, without echo";
                    break;
                case 0x09:
                    result = "Write string to STDOUT";
                    break;
                case 0x0a:
                    result = "Buffered Input";
                    break;
                case 0x0b:
                    result = "Get STDIN Status";
                    break;
                case 0x0c:
                    result = "Flush Buffer and read STDIN call";
                    break;
                case 0x0d:
                    result = "Disk Reset";
                    break;
                case 0x0e:
                    result = "Select Default Drive";
                    break;
                case 0x0f:
                    result = "Open File using FCB";
                    break;
                case 0x10:
                    result = "Close File using FCB";
                    break;
                case 0x11:
                    result = "Find First Matching File using FCB";
                    break;
                case 0x12:
                    result = "Find Next Matching File using FCB";
                    break;
                case 0x13:
                    result = "Delete File using FCB";
                    break;
                case 0x14:
                    result = "Sequential read from FCB";
                    break;
                case 0x15:
                    result = "Sequential write to FCB";
                    break;
                case 0x16:
                    result = "Create or truncate file using FCB";
                    break;
                case 0x17:
                    result = "Rename file using FCB";
                    break;
                case 0x1b:
                    result = "Get allocation info for default drive";
                    break;
                case 0x1c:
                    result = "Get allocation info for specific drive";
                    break;
                case 0x21:
                    result = "Read random record from FCB";
                    break;
                case 0x22:
                    result = "Write random record to FCB";
                    break;
                case 0x23:
                    result = "Get file size for FCB";
                    break;
                case 0x24:
                    result = "Set Random Record number for FCB";
                    break;
                case 0x27:
                    result = "Random block read from FCB";
                    break;
                case 0x28:
                    result = "Random Block write to FCB";
                    break;
                case 0x29:
                    result = "Parse filename into FCB";
                    break;
                case 0x19:
                    result = "Get current default drive";
                    break;
                case 0x1a:
                    result = "Set Disk Transfer Area Address";
                    break;
                case 0x25:
                    result = "Set Interrupt Vector";
                    break;
                case 0x26:
                    result = "Create new PSP";
                    break;
                case 0x2a:
                    result = "Get System Date";
                    break;
                case 0x2b:
                    result = "Set System Date";
                    break;
                case 0x2c:
                    result = "Get System Time";
                    break;
                case 0x2d:
                    result = "Set System Time";
                    break;
                case 0x2e:
                    result = "Set Verify flag";
                    break;
                case 0x2f:
                    result = "Get Disk Transfer Area";
                    break;
                case 0x30:
                    result = "Get DOS Version";
                    break;
                case 0x31:
                    result = "Terminate and stay resident";
                    break;
                case 0x1f:
                    result = "Get drive parameter block for default drive";
                    break;
                case 0x32:
                    result = "Get drive parameter block for specific drive";
                    break;
                case 0x33:
                    result = "Extended Break Checking";
                    break;
                case 0x34:
                    result = "Get INDos Flag";
                    break;
                case 0x35:
                    result = "Get interrupt vector";
                    break;
                case 0x36:
                    result = "Get Free Disk Space";
                    break;
                case 0x37:
                    result = "Get/Set Switch char Get/Set Availdev thing";
                    break;
                case 0x38:
                    result = "Set Country Code";
                    break;
                case 0x39:
                    result = "MKDIR Create directory";
                    break;
                case 0x3a:
                    result = "RMDIR Remove directory";
                    break;
                case 0x3b:
                    result = "CHDIR Set current directory";
                    break;
                case 0x3c:
                    result = "CREATE Create of truncate file";
                    break;
                case 0x3d:
                    result = "OPEN Open existing file";
                    break;
                case 0x3e:
                    result = "CLOSE Close file";
                    break;
                case 0x3f:
                    result = "READ Read from file or device";
                    break;
                case 0x40:
                    result = "WRITE Write to file or device";
                    break;
                case 0x41:
                    result = "UNLINK Delete file";
                    break;
                case 0x42:
                    result = "LSEEK Set current file position";
                    break;
                case 0x43:
                    result = "Get/Set file attributes";
                    break;
                case 0x44:
                    result = "IOCTL Functions";
                    break;
                case 0x45:
                    result = "DUP Duplicate file handle";
                    break;
                case 0x46:
                    result = "DUP2,FORCEDUP Force duplicate file handle";
                    break;
                case 0x47:
                    result = "CWD Get current directory";
                    break;
                case 0x48:
                    result = "Allocate memory";
                    break;
                case 0x49:
                    result = "Free memory";
                    break;
                case 0x4a:
                    result = "Resize memory block";
                    break;
                case 0x4b:
                    result = "EXEC Load and/or execute program";
                    break;
                case 0x4c:
                    result = "EXIT Terminate with return code";
                    break;
                case 0x4d:
                    result = "Get Return code";
                    break;
                case 0x4e:
                    result = "FINDFIRST Find first matching file";
                    break;
                case 0x4f:
                    result = "FINDNEXT Find next matching file";
                    break;
                case 0x50:
                    result = "Set current PSP";
                    break;
                case 0x51:
                    result = "Get current PSP";
                    break;
                case 0x52:
                    result = "Get list of lists";
                    break;
                case 0x53:
                    result = "Translate BIOS parameter block to drive parameter block";
                    break;
                case 0x54:
                    result = "Get verify flag";
                    break;
                case 0x55:
                    result = "Create Child PSP";
                    break;
                case 0x56:
                    result = "RENAME Rename file";
                    break;
                case 0x57:
                    result = "Get/Set File's Date and Time";
                    break;
                case 0x58:
                    result = "Get/Set Memory allocation strategy";
                    break;
                case 0x59:
                    result = "Get Extended error information";
                    break;
                case 0x5a:
                    result = "Create temporary file";
                    break;
                case 0x5b:
                    result = "Create new file";
                    break;
                case 0x5c:
                    result = "FLOCK File region locking";
                    break;
                case 0x5d:
                    result = "Network Functions";
                    break;
                case 0x5f:
                    result = "Network redirection";
                    break;
                case 0x60:
                    result = "Canonicalize filename or path";
                    break;
                case 0x62:
                    result = "Get Current PSP Address";
                    break;
                case 0x63:
                    result = "DOUBLE BYTE CHARACTER SET";
                    break;
                case 0x64:
                    result = "Set device driver lookahead flag";
                    break;
                case 0x65:
                    result = "Get extented country information and a lot of other useless shit";
                    break;
                case 0x66:
                    result = "Get/Set global code page table";
                    break;
                case 0x67:
                    result = "Set handle count";
                    break;
                case 0x68:
                    result = "FFLUSH Commit file";
                    break;
                case 0x69:
                    result = "Get/Set disk serial number";
                    break;
                case 0x6c:
                    result = "Extended Open/Create";
                    break;
                case 0x71:
                    result = "Unknown probably 4dos detection";
                    break;
            }
            return "Dos.DOS_21Handler " + result;
        }

        public /*Bitu*/int call() {
            if (((CPU_Regs.reg_eax.high() != 0x50) && (CPU_Regs.reg_eax.high() != 0x51) && (CPU_Regs.reg_eax.high() != 0x62) && (CPU_Regs.reg_eax.high() != 0x64)) && (CPU_Regs.reg_eax.high()<0x6c)) {
                Dos_PSP psp = new Dos_PSP(dos.psp());
                psp.SetStack(Memory.RealMake(CPU_Regs.reg_ssVal.dword,CPU_Regs.reg_esp.word()-18));
            }

            switch (CPU_Regs.reg_eax.high() & 0xFF) {
            case 0x00:		/* Terminate Program */
                Dos_execute.DOS_Terminate(Memory.mem_readw(CPU_Regs.reg_ssPhys.dword+CPU_Regs.reg_esp.word()+2),false,0);
                break;
            case 0x01:		/* Read character from STDIN, with echo */
                {
                    /*Bit8u*/byte[] c=new byte[1];/*Bit16u*/IntRef n=new IntRef(1);
                    dos.echo=true;
                    Dos_files.DOS_ReadFile(Dos_files.STDIN,c,n);
                    CPU_Regs.reg_eax.low(c[0]);
                    dos.echo=false;
                }
                break;
            case 0x02:		/* Write character to STDOUT */
                {
                    /*Bit8u*/byte[] c=new byte[]{(byte)CPU_Regs.reg_edx.low()};/*Bit16u*/IntRef n=new IntRef(1);
                    Dos_files.DOS_WriteFile(Dos_files.STDOUT,c,n);
                    //Not in the official specs, but happens nonetheless. (last written character)
                    CPU_Regs.reg_eax.low(c[0]);// reg_al=(c==9)?0x20:c; //Officially: tab to spaces
                }
                break;
            case 0x03:		/* Read character from STDAUX */
                {
                    /*Bit16u*/int port = Memory.real_readw(0x40,0);
                    if(port!=0 && Serialports.serialports[0]!=null) {
                        /*Bit8u*/ShortRef status = new ShortRef();
                        ShortRef al = new ShortRef(CPU_Regs.reg_eax.low());
                        // RTS/DTR on
                        IO.IO_WriteB(port+4,0x3);
                        Serialports.serialports[0].Getchar(al, status, true, 0xFFFFFFFF);
                        CPU_Regs.reg_eax.low(al.value);
                    }
                }
                break;
            case 0x04:		/* Write Character to STDAUX */
                {
                    /*Bit16u*/int port = Memory.real_readw(0x40,0);
                    if(port!=0 && Serialports.serialports[0]!=null) {
                        // RTS/DTR on
                        IO.IO_WriteB(port+4,0x3);
                        Serialports.serialports[0].Putchar((short)CPU_Regs.reg_edx.low(),true,true, 0xFFFFFFFF);
                        // RTS off
                        IO.IO_WriteB(port+4,0x1);
                    }
                }
                break;
            case 0x05:		/* Write Character to PRINTER */
                Log.exit("DOS:Unhandled call "+Integer.toString(CPU_Regs.reg_eax.high(),16));
                break;
            case 0x06:		/* Direct Console Output / Input */
                switch (CPU_Regs.reg_edx.low() & 0xFF) {
                case 0xFF:	/* Input */
                    {
                        //Simulate DOS overhead for timing sensitive games
                        //MM1
                        overhead();
                        //TODO Make this better according to standards
                        if (!Dos_ioctl.DOS_GetSTDINStatus()) {
                            CPU_Regs.reg_eax.low(0);
                            Callback.CALLBACK_SZF(true);
                            break;
                        }
                        /*Bit8u*/byte[] c=new byte[1];/*Bit16u*/IntRef n=new IntRef(1);
                        Dos_files.DOS_ReadFile(Dos_files.STDIN,c,n);
                        CPU_Regs.reg_eax.low(c[0]);
                        Callback.CALLBACK_SZF(false);
                        break;
                    }
                default:
                    {
                        /*Bit8u*/byte[] c=new byte[] {(byte)CPU_Regs.reg_edx.low()};/*Bit16u*/IntRef n = new IntRef(1);
                        Dos_files.DOS_WriteFile(Dos_files.STDOUT,c,n);
                        CPU_Regs.reg_eax.low(CPU_Regs.reg_edx.low());
                    }
                    break;
                }
                break;
            case 0x07:		/* Character Input, without echo */
                {
                        /*Bit8u*/byte[] c=new byte[1];/*Bit16u*/IntRef n=new IntRef(1);
                        Dos_files.DOS_ReadFile (Dos_files.STDIN,c,n);
                        CPU_Regs.reg_eax.low(c[0]);
                        break;
                }
            case 0x08:		/* Direct Character Input, without echo (checks for breaks officially :)*/
                {
                        /*Bit8u*/byte[] c=new byte[1];/*Bit16u*/IntRef n=new IntRef(1);
                        Dos_files.DOS_ReadFile (Dos_files.STDIN,c,n);
                        CPU_Regs.reg_eax.low(c[0]);
                        break;
                }
            case 0x09:		/* Write string to STDOUT */
                {
                    /*Bit8u*/byte[] c=new byte[1];/*Bit16u*/IntRef n=new IntRef(1);
                    /*PhysPt*/int buf=CPU_Regs.reg_dsPhys.dword+CPU_Regs.reg_edx.word();
                    while ((c[0]=((byte)(Memory.mem_readb(buf++) & 0xFF)))!='$') {
                        Dos_files.DOS_WriteFile(Dos_files.STDOUT,c,n);
                    }
                }
                break;
            case 0x0a:		/* Buffered Input */
                {
                    //TODO ADD Break checkin in Dos_files.STDIN but can't care that much for it
                    /*PhysPt*/int data=CPU_Regs.reg_dsPhys.dword+CPU_Regs.reg_edx.word();
                    /*Bit8u*/int free=Memory.mem_readb(data);
                    /*Bit8u*/short read=0;/*Bit8u*/byte[] c=new byte[1];/*Bit16u*/IntRef n=new IntRef(1);
                    if (free==0) break;
                    free--;
                    for(;;) {
                        Dos_files.DOS_ReadFile(Dos_files.STDIN,c,n);
                        if (c[0] == 8) {			// Backspace
                            if (read!=0) {	//Something to backspace.
                                // Dos_files.STDOUT treats backspace as non-destructive.
                                         Dos_files.DOS_WriteFile(Dos_files.STDOUT,c,n);
                                c[0] = ' '; Dos_files.DOS_WriteFile(Dos_files.STDOUT,c,n);
                                c[0] = 8;   Dos_files.DOS_WriteFile(Dos_files.STDOUT,c,n);
                                --read;
                            }
                            continue;
                        }
                        if (read == free && c[0] != 13) {		// Keyboard buffer full
                            /*Bit8u*/byte[] bell=new byte[] {7};
                            Dos_files.DOS_WriteFile(Dos_files.STDOUT, bell, n);
                            continue;
                        }
                        Dos_files.DOS_WriteFile(Dos_files.STDOUT,c,n);
                        Memory.mem_writeb(data+read+2,c[0]);
                        if (c[0]==13)
                            break;
                        read++;
                    }
                    Memory.mem_writeb(data+1,read);
                    break;
                }
            case 0x0b:		/* Get STDIN Status */
                if (!Dos_ioctl.DOS_GetSTDINStatus()) {CPU_Regs.reg_eax.low(0x00);}
                else {CPU_Regs.reg_eax.low(0xFF);}
                //Simulate some overhead for timing issues
                //Tankwar menu (needs maybe even more)
                overhead();
                break;
            case 0x0c:		/* Flush Buffer and read STDIN call */
                {
                    /* flush buffer if STDIN is CON */
                    /*Bit8u*/int handle=RealHandle(Dos_files.STDIN);
                    if (handle!=0xFF && Dos_files.Files[handle]!=null && Dos_files.Files[handle].IsName("CON")) {
                        /*Bit8u*/byte[] c=new byte[1];IntRef n = new IntRef(0);
                        while (Dos_ioctl.DOS_GetSTDINStatus()) {
                            n.value=1;
                            Dos_files.DOS_ReadFile(Dos_files.STDIN,c,n);
                        }
                    }
                    switch (CPU_Regs.reg_eax.low()) {
                    case 0x1:
                    case 0x6:
                    case 0x7:
                    case 0x8:
                    case 0xa:
                        {
                            /*Bit8u*/int oldah=CPU_Regs.reg_eax.high();
                            CPU_Regs.reg_eax.high(CPU_Regs.reg_eax.low());
                            DOS_21Handler.call();
                            CPU_Regs.reg_eax.high(oldah);
                        }
                        break;
                    default:
    //				LOG_ERROR("DOS:0C:Illegal Flush STDIN Buffer call %d",CPU_Regs.reg_eax.low());
                        CPU_Regs.reg_eax.low(0);
                        break;
                    }
                }
                break;
    //TODO Find out the values for when CPU_Regs.reg_eax.low()!=0
    //TODO Hope this doesn't do anything special
            case 0x0d:		/* Disk Reset */
    //Sure let's reset a virtual disk
                break;
            case 0x0e:		/* Select Default Drive */
                Dos_files.DOS_SetDefaultDrive((short)CPU_Regs.reg_edx.low());
                CPU_Regs.reg_eax.low(Dos_files.DOS_DRIVES);
                break;
            case 0x0f:		/* Open File using FCB */
                if(Dos_files.DOS_FCBOpen((int)CPU_Regs.reg_dsVal.dword,CPU_Regs.reg_edx.word())){
                    CPU_Regs.reg_eax.low(0);
                }else{
                    CPU_Regs.reg_eax.low(0xff);
                }
                if (Log.level<=LogSeverities.LOG_NORMAL) Log.log(LogTypes.LOG_FCB, LogSeverities.LOG_NORMAL,"DOS:0x0f FCB-fileopen used, result:al="+CPU_Regs.reg_eax.low());
                break;
            case 0x10:		/* Close File using FCB */
                if(Dos_files.DOS_FCBClose((int)CPU_Regs.reg_dsVal.dword,CPU_Regs.reg_edx.word())){
                    CPU_Regs.reg_eax.low(0);
                }else{
                    CPU_Regs.reg_eax.low(0xff);
                }
                if (Log.level<=LogSeverities.LOG_NORMAL) Log.log(LogTypes.LOG_FCB,LogSeverities.LOG_NORMAL,"DOS:0x10 FCB-fileclose used, result:al="+CPU_Regs.reg_eax.low());
                break;
            case 0x11:		/* Find First Matching File using FCB */
                if(Dos_files.DOS_FCBFindFirst((int)CPU_Regs.reg_dsVal.dword,CPU_Regs.reg_edx.word())) CPU_Regs.reg_eax.low(0x00);
                else CPU_Regs.reg_eax.low(0xFF);
                if (Log.level<=LogSeverities.LOG_NORMAL) Log.log(LogTypes.LOG_FCB,LogSeverities.LOG_NORMAL,"DOS:0x11 FCB-FindFirst used, result:al="+CPU_Regs.reg_eax.low());
                break;
            case 0x12:		/* Find Next Matching File using FCB */
                if(Dos_files.DOS_FCBFindNext((int)CPU_Regs.reg_dsVal.dword,CPU_Regs.reg_edx.word())) CPU_Regs.reg_eax.low(0x00);
                else CPU_Regs.reg_eax.low(0xFF);
                if (Log.level<=LogSeverities.LOG_NORMAL) Log.log(LogTypes.LOG_FCB,LogSeverities.LOG_NORMAL,"DOS:0x12 FCB-FindNext used, result:al="+CPU_Regs.reg_eax.low());
                break;
            case 0x13:		/* Delete File using FCB */
                if (Dos_files.DOS_FCBDeleteFile((int)CPU_Regs.reg_dsVal.dword,CPU_Regs.reg_edx.word())) CPU_Regs.reg_eax.low(0x00);
                else CPU_Regs.reg_eax.low(0xFF);
                if (Log.level<=LogSeverities.LOG_NORMAL) Log.log(LogTypes.LOG_FCB,LogSeverities.LOG_NORMAL,"DOS:0x16 FCB-Delete used, result:al="+CPU_Regs.reg_eax.low());
                break;
            case 0x14:		/* Sequential read from FCB */
                CPU_Regs.reg_eax.low(Dos_files.DOS_FCBRead((int)CPU_Regs.reg_dsVal.dword,CPU_Regs.reg_edx.word(),0));
                if (Log.level<=LogSeverities.LOG_NORMAL) Log.log(LogTypes.LOG_FCB,LogSeverities.LOG_NORMAL,"DOS:0x14 FCB-Read used, result:al="+CPU_Regs.reg_eax.low());
                break;
            case 0x15:		/* Sequential write to FCB */
                CPU_Regs.reg_eax.low(Dos_files.DOS_FCBWrite((int)CPU_Regs.reg_dsVal.dword,CPU_Regs.reg_edx.word(),0));
                if (Log.level<=LogSeverities.LOG_NORMAL) Log.log(LogTypes.LOG_FCB,LogSeverities.LOG_NORMAL,"DOS:0x15 FCB-Write used, result:al="+CPU_Regs.reg_eax.low());
                break;
            case 0x16:		/* Create or truncate file using FCB */
                if (Dos_files.DOS_FCBCreate((int)CPU_Regs.reg_dsVal.dword,CPU_Regs.reg_edx.word())) CPU_Regs.reg_eax.low(0x00);
                else CPU_Regs.reg_eax.low(0xFF);
                if (Log.level<=LogSeverities.LOG_NORMAL) Log.log(LogTypes.LOG_FCB,LogSeverities.LOG_NORMAL,"DOS:0x16 FCB-Create used, result:al="+CPU_Regs.reg_eax.low());
                break;
            case 0x17:		/* Rename file using FCB */
                if (Dos_files.DOS_FCBRenameFile((int)CPU_Regs.reg_dsVal.dword,CPU_Regs.reg_edx.word())) CPU_Regs.reg_eax.low(0x00);
                else CPU_Regs.reg_eax.low(0xFF);
                break;
            case 0x1b:		/* Get allocation info for default drive */
            {
                IntRef cx = new IntRef(CPU_Regs.reg_ecx.word());
                IntRef dx = new IntRef(CPU_Regs.reg_edx.word());
                ShortRef al = new ShortRef(CPU_Regs.reg_eax.low());
                if (!Dos_files.DOS_GetAllocationInfo((short)0,cx,al,dx))
                    CPU_Regs.reg_eax.low(0xff);
                else {
                    CPU_Regs.reg_ecx.word(cx.value);
                    CPU_Regs.reg_edx.word(dx.value);
                    CPU_Regs.reg_eax.low(al.value);
                }
                break;
            }
            case 0x1c:		/* Get allocation info for specific drive */
            {
                IntRef cx = new IntRef(CPU_Regs.reg_ecx.word());
                IntRef dx = new IntRef(CPU_Regs.reg_edx.word());
                ShortRef al = new ShortRef(CPU_Regs.reg_eax.low());
                if (!Dos_files.DOS_GetAllocationInfo((short)CPU_Regs.reg_edx.low(),cx,al,dx))
                    CPU_Regs.reg_eax.low(0xff);
                else {
                    CPU_Regs.reg_ecx.word(cx.value);
                    CPU_Regs.reg_edx.word(dx.value);
                    CPU_Regs.reg_eax.low(al.value);
                }
                break;
            }
            case 0x21:		/* Read random record from FCB */
            {
                IntRef toRead = new IntRef(1);
                CPU_Regs.reg_eax.low(Dos_files.DOS_FCBRandomRead((int)CPU_Regs.reg_dsVal.dword,CPU_Regs.reg_edx.word(),toRead,true));
                if (Log.level<=LogSeverities.LOG_NORMAL) Log.log(LogTypes.LOG_FCB,LogSeverities.LOG_NORMAL,"DOS:0x21 FCB-Random read used, result:al="+CPU_Regs.reg_eax.low());
                break;
            }
            case 0x22:		/* Write random record to FCB */
            {
                IntRef toWrite = new IntRef(1);
                CPU_Regs.reg_eax.low(Dos_files.DOS_FCBRandomWrite((int)CPU_Regs.reg_dsVal.dword,CPU_Regs.reg_edx.word(),toWrite,true));
                if (Log.level<=LogSeverities.LOG_NORMAL) Log.log(LogTypes.LOG_FCB,LogSeverities.LOG_NORMAL,"DOS:0x22 FCB-Random write used, result:al="+CPU_Regs.reg_eax.low());
                break;
            }
            case 0x23:		/* Get file size for FCB */
                if (Dos_files.DOS_FCBGetFileSize((int)CPU_Regs.reg_dsVal.dword,CPU_Regs.reg_edx.word())) CPU_Regs.reg_eax.low(0x00);
                else CPU_Regs.reg_eax.low(0xFF);
                break;
            case 0x24:		/* Set Random Record number for FCB */
                Dos_files.DOS_FCBSetRandomRecord((int)CPU_Regs.reg_dsVal.dword,CPU_Regs.reg_edx.word());
                break;
            case 0x27:		/* Random block read from FCB */
            {
                IntRef toRead = new IntRef(CPU_Regs.reg_ecx.word());
                CPU_Regs.reg_eax.low(Dos_files.DOS_FCBRandomRead((int)CPU_Regs.reg_dsVal.dword,CPU_Regs.reg_edx.word(),toRead,false));
                CPU_Regs.reg_ecx.word(toRead.value);
                if (Log.level<=LogSeverities.LOG_NORMAL) Log.log(LogTypes.LOG_FCB,LogSeverities.LOG_NORMAL,"DOS:0x27 FCB-Random(block) read used, result:al="+CPU_Regs.reg_eax.low());
                break;
            }
            case 0x28:		/* Random Block write to FCB */
            {
                IntRef toWrite = new IntRef(CPU_Regs.reg_ecx.word());
                CPU_Regs.reg_eax.low(Dos_files.DOS_FCBRandomWrite((int)CPU_Regs.reg_dsVal.dword,CPU_Regs.reg_edx.word(),toWrite,false));
                CPU_Regs.reg_ecx.word(toWrite.value);
                if (Log.level<=LogSeverities.LOG_NORMAL) Log.log(LogTypes.LOG_FCB,LogSeverities.LOG_NORMAL,"DOS:0x28 FCB-Random(block) write used, result:al="+CPU_Regs.reg_eax.low());
                break;
            }
            case 0x29:		/* Parse filename into FCB */
                {
                    /*Bit8u*/ShortRef difference=new ShortRef();
                    String string;
                    string=Memory.MEM_StrCopy(CPU_Regs.reg_dsPhys.dword+CPU_Regs.reg_esi.word(),1023); // 1024 toasts the stack
                    CPU_Regs.reg_eax.low(Dos_files.FCB_Parsename((int)CPU_Regs.reg_esVal.dword,CPU_Regs.reg_edi.word(),(short)CPU_Regs.reg_eax.low() ,string, difference));
                    CPU_Regs.reg_esi.word(CPU_Regs.reg_esi.word()+difference.value);
                }
                if (Log.level<=LogSeverities.LOG_NORMAL) Log.log(LogTypes.LOG_FCB,LogSeverities.LOG_NORMAL,"DOS:29:FCB Parse Filename, result:al="+CPU_Regs.reg_eax.low());
                break;
            case 0x19:		/* Get current default drive */
                CPU_Regs.reg_eax.low(Dos_files.DOS_GetDefaultDrive());
                break;
            case 0x1a:		/* Set Disk Transfer Area Address */
                dos.dta((int)CPU_Regs.RealMakeSegDS(CPU_Regs.reg_edx.word()));
                break;
            case 0x25:		/* Set Interrupt Vector */
                Memory.RealSetVec(CPU_Regs.reg_eax.low(),CPU_Regs.RealMakeSegDS(CPU_Regs.reg_edx.word()));
                break;
            case 0x26:		/* Create new PSP */
                Dos_execute.DOS_NewPSP(CPU_Regs.reg_edx.word(),new Dos_PSP(dos.psp()).GetSize());
                CPU_Regs.reg_eax.low(0xf0);	/* al destroyed */
                break;
            case 0x2a:		/* Get System Date */
                {
                    CPU_Regs.reg_eax.word(0); // get time
			        Callback.CALLBACK_RunRealInt(0x1a);
			        if (CPU_Regs.reg_eax.low()!=0) DOS_AddDays((short)CPU_Regs.reg_eax.low());
                    int a = (14 - dos.date.month)/12;
                    int y = dos.date.year - a;
                    int m = dos.date.month + 12*a - 2;
                    CPU_Regs.reg_eax.low((dos.date.day+y+(y/4)-(y/100)+(y/400)+(31*m)/12) % 7);
                    CPU_Regs.reg_ecx.word(dos.date.year);
                    CPU_Regs.reg_edx.high(dos.date.month);
                    CPU_Regs.reg_edx.low(dos.date.day);
                }
                break;
            case 0x2b:		/* Set System Date */
                if (CPU_Regs.reg_ecx.word()<1980) { CPU_Regs.reg_eax.low(0xff);break;}
                if ((CPU_Regs.reg_edx.high()>12) || (CPU_Regs.reg_edx.high()==0))	{ CPU_Regs.reg_eax.low(0xff);break;}
                if (CPU_Regs.reg_edx.low()==0) { CPU_Regs.reg_eax.low(0xff);break;}
                if (CPU_Regs.reg_edx.low()>DOS_DATE_months[CPU_Regs.reg_edx.high()]) {
                    if(!((CPU_Regs.reg_edx.high()==2)&&(CPU_Regs.reg_ecx.word()%4 == 0)&&(CPU_Regs.reg_edx.low()==29))) // february pass
                    { CPU_Regs.reg_eax.low(0xff);break; }
                }
                dos.date.year=(short)CPU_Regs.reg_ecx.word();
                dos.date.month=(byte)CPU_Regs.reg_edx.high();
                dos.date.day=(byte)CPU_Regs.reg_edx.low();
                CPU_Regs.reg_eax.low(0);
                break;
            case 0x2c:		/* Get System Time */
                CPU_Regs.reg_eax.word(0); // get time
                Callback.CALLBACK_RunRealInt(0x1a);
                if(CPU_Regs.reg_eax.low()!=0) DOS_AddDays((short)CPU_Regs.reg_eax.low());
                CPU_Regs.reg_eax.high(0x2c);

                /*Bitu*/long ticks=((long)CPU_Regs.reg_ecx.word()<<16)|CPU_Regs.reg_edx.word();
                if(time_start<=ticks) ticks-=time_start;
                /*Bitu*/long time=(long)((100.0/((double) Timer.PIT_TICK_RATE/65536.0)) * (double)ticks);

                CPU_Regs.reg_edx.low((short)(time % 100)); // 1/100 seconds
                time/=100;
                CPU_Regs.reg_edx.high((short)(time % 60)); // seconds
                time/=60;
                CPU_Regs.reg_ecx.low((short)(time % 60)); // minutes
                time/=60;
                CPU_Regs.reg_ecx.high((short)(time % 24)); // hours
                //Simulate DOS overhead for timing-sensitive games
                //Robomaze 2
                overhead();
                break;
            case 0x2d:		/* Set System Time */
                Log.log(LogTypes.LOG_DOSMISC,LogSeverities.LOG_ERROR,"DOS:Set System Time not supported");
                //Check input parameters nonetheless
                if( CPU_Regs.reg_ecx.high() > 23 || CPU_Regs.reg_ecx.low() > 59 || CPU_Regs.reg_edx.high() > 59 || CPU_Regs.reg_edx.low() > 99 )
                    CPU_Regs.reg_eax.low(0xff);
                else { //Allow time to be set to zero. Restore the orginal time for all other parameters. (QuickBasic)
                    if (CPU_Regs.reg_ecx.word() == 0 && CPU_Regs.reg_edx.word() == 0) {time_start = (Memory.mem_readd(Bios.BIOS_TIMER) & 0xFFFFFFFFl);Log.log_msg("Warning: game messes with DOS time!");}
                    else time_start = 0;
                    CPU_Regs.reg_eax.low(0);
                }
                break;
            case 0x2e:		/* Set Verify flag */
                dos.verify=(CPU_Regs.reg_eax.low()==1);
                break;
            case 0x2f:		/* Get Disk Transfer Area */
                CPU_Regs.SegSet16ES(Memory.RealSeg(dos.dta()));
                CPU_Regs.reg_ebx.word(Memory.RealOff(dos.dta()));
                break;
            case 0x30:		/* Get DOS Version */
                if (CPU_Regs.reg_eax.low()==0) CPU_Regs.reg_ebx.high(0xFF);		/* Fake Microsoft DOS */
                if (CPU_Regs.reg_eax.low()==1) CPU_Regs.reg_ebx.high(0x10);		/* DOS is in HMA */
                CPU_Regs.reg_eax.low(dos.version.major);
                CPU_Regs.reg_eax.high(dos.version.minor);
                /* Serialnumber */
                CPU_Regs.reg_ebx.low(0x00);
                CPU_Regs.reg_ecx.word(0x0000);
                break;
            case 0x31:		/* Terminate and stay resident */
                // Important: This service does not set the carry flag!
            {
                IntRef dx = new IntRef(CPU_Regs.reg_edx.word());
                Dos_memory.DOS_ResizeMemory(dos.psp(),dx);
                Dos_execute.DOS_Terminate(dos.psp(),true,CPU_Regs.reg_eax.low());
                break;
            }
            case 0x1f: /* Get drive parameter block for default drive */
            case 0x32: /* Get drive parameter block for specific drive */
                {	/* Officially a dpb should be returned as well. The disk detection part is implemented */
                    /*Bit8u*/int drive=CPU_Regs.reg_edx.low();
                    if (drive==0 || CPU_Regs.reg_eax.high()==0x1f) drive = Dos_files.DOS_GetDefaultDrive();
                    else drive--;
                    if (Dos_files.Drives[drive]!=null) {
                        CPU_Regs.reg_eax.low(0x00);
                        CPU_Regs.SegSet16DS(dos.tables.dpb);
                        CPU_Regs.reg_ebx.word(drive);//Faking only the first entry (that is the driveletter)
                        Log.log(LogTypes.LOG_DOSMISC,LogSeverities.LOG_ERROR,"Get drive parameter block.");
                    } else {
                        CPU_Regs.reg_eax.low(0xff);
                    }
                }
                break;
            case 0x33:		/* Extended Break Checking */
                switch (CPU_Regs.reg_eax.low()) {
                    case 0:CPU_Regs.reg_edx.low(dos.breakcheck?1:0);break;			/* Get the breakcheck flag */
                    case 1:dos.breakcheck=(CPU_Regs.reg_edx.low()>0);break;		/* Set the breakcheck flag */
                    case 2:{boolean old=dos.breakcheck;dos.breakcheck=(CPU_Regs.reg_edx.low()>0);CPU_Regs.reg_edx.low(old?1:0);}break;
                    case 3: /* Get cpsw */
                        /* Fallthrough */
                    case 4: /* Set cpsw */
                        if (Log.level<=LogSeverities.LOG_ERROR) Log.log(LogTypes.LOG_DOSMISC,LogSeverities.LOG_ERROR,"Someone playing with cpsw "+Integer.toString(CPU_Regs.reg_eax.word(),16));
                        break;
                    case 5:CPU_Regs.reg_edx.low(3);break;//TODO should be z						/* Always boot from c: :) */
                    case 6:											/* Get true version number */
                        CPU_Regs.reg_ebx.low(dos.version.major);
                        CPU_Regs.reg_ebx.high(dos.version.minor);
                        CPU_Regs.reg_edx.low(dos.version.revision);
                        CPU_Regs.reg_edx.high(0x10);								/* Dos in HMA */
                        break;
                    default:
                        if (Log.level<=LogSeverities.LOG_ERROR) Log.log(LogTypes.LOG_DOSMISC,LogSeverities.LOG_ERROR,"Weird 0x33 call "+Integer.toString(CPU_Regs.reg_eax.low(), 16));
                        CPU_Regs.reg_eax.low(0xff);
				        break;
                }
                break;
            case 0x34:		/* Get INDos Flag */
                CPU_Regs.SegSet16ES(DOS_SDA_SEG);
                CPU_Regs.reg_ebx.word(DOS_SDA_OFS + 0x01);
                break;
            case 0x35:		/* Get interrupt vector */
                CPU_Regs.reg_ebx.word(Memory.real_readw(0,((/*Bit16u*/int)CPU_Regs.reg_eax.low())*4));
                CPU_Regs.SegSet16ES(Memory.real_readw(0,((/*Bit16u*/int)CPU_Regs.reg_eax.low())*4+2));
                break;
            case 0x36:		/* Get Free Disk Space */
                {
                    /*Bit16u*/IntRef bytes=new IntRef(0);
                    /*Bit8u*/ShortRef sectors=new ShortRef();IntRef clusters=new IntRef(0),free=new IntRef(0);
                    if (Dos_files.DOS_GetFreeDiskSpace((short)CPU_Regs.reg_edx.low(),bytes,sectors,clusters,free)) {
                        CPU_Regs.reg_eax.word(sectors.value);
                        CPU_Regs.reg_ebx.word(free.value);
                        CPU_Regs.reg_ecx.word(bytes.value);
                        CPU_Regs.reg_edx.word(clusters.value);
                    } else {
                        /*Bit8u*/int drive=CPU_Regs.reg_edx.low();
                        if (drive==0) drive=Dos_files.DOS_GetDefaultDrive();
                        else drive--;
                        if (drive<2) {
                            // floppy drive, non-present drivesdisks issue floppy check through int24
                            // (critical error handler); needed for Mixed up Mother Goose (hook)
    //					CALLBACK_RunRealInt(0x24);
                        }
                        CPU_Regs.reg_eax.word(0xffff);	// invalid drive specified
                    }
                }
                break;
            case 0x37:		/* Get/Set Switch char Get/Set Availdev thing */
    //TODO	Give errors for these functions to see if anyone actually uses this shit-
                switch (CPU_Regs.reg_eax.low()) {
                case 0:
                     CPU_Regs.reg_eax.low(0);CPU_Regs.reg_edx.low(0x2f);break;  /* always return '/' like dos 5.0+ */
                case 1:
                     CPU_Regs.reg_eax.low(0);break;
                case 2:
                     CPU_Regs.reg_eax.low(0);CPU_Regs.reg_edx.low(0x2f);break;
                case 3:
                     CPU_Regs.reg_eax.low(0);break;
                }
                Log.log(LogTypes.LOG_MISC,LogSeverities.LOG_ERROR,"DOS:0x37:Call for not supported switchchar");
                break;
            case 0x38:					/* Set Country Code */
                if (CPU_Regs.reg_eax.low()==0) {		/* Get country specidic information */
                    /*PhysPt*/int dest = CPU_Regs.reg_dsPhys.dword+CPU_Regs.reg_edx.word();
                    Memory.MEM_BlockWrite(dest,dos.tables.country,0x18);
                    CPU_Regs.reg_eax.word(0x01); CPU_Regs.reg_ebx.word(0x01);
                    Callback.CALLBACK_SCF(false);
                    break;
                } else {				/* Set country code */
                    Log.log(LogTypes.LOG_MISC,LogSeverities.LOG_ERROR,"DOS:Setting country code not supported");
                }
                Callback.CALLBACK_SCF(true);
                break;
            case 0x39:		/* MKDIR Create directory */
            {
                String name1 = Memory.MEM_StrCopy(CPU_Regs.reg_dsPhys.dword+CPU_Regs.reg_edx.word(),256);
                if (Dos_files.DOS_MakeDir(name1)) {
                    CPU_Regs.reg_eax.word(0x05);	/* ax destroyed */
                    Callback.CALLBACK_SCF(false);
                } else {
                    CPU_Regs.reg_eax.word(dos.errorcode);
                    Callback.CALLBACK_SCF(true);
                }
                break;
            }
            case 0x3a:		/* RMDIR Remove directory */
            {
                String name1 = Memory.MEM_StrCopy(CPU_Regs.reg_dsPhys.dword+CPU_Regs.reg_edx.word(),256);
                if  (Dos_files.DOS_RemoveDir(name1)) {
                    CPU_Regs.reg_eax.word(0x05);	/* ax destroyed */
                    Callback.CALLBACK_SCF(false);
                } else {
                    CPU_Regs.reg_eax.word(dos.errorcode);
                    Callback.CALLBACK_SCF(true);
                    if (Log.level<=LogSeverities.LOG_NORMAL) Log.log(LogTypes.LOG_MISC,LogSeverities.LOG_NORMAL,"Remove dir failed on "+name1+" with error "+Integer.toString(dos.errorcode));
                }
                break;
            }
            case 0x3b:		/* CHDIR Set current directory */
            {
                String name1 = Memory.MEM_StrCopy(CPU_Regs.reg_dsPhys.dword+CPU_Regs.reg_edx.word(),256);
                if  (Dos_files.DOS_ChangeDir(name1)) {
                    CPU_Regs.reg_eax.word(0x00);	/* ax destroyed */
                    Callback.CALLBACK_SCF(false);
                } else {
                    CPU_Regs.reg_eax.word(dos.errorcode);
                    Callback.CALLBACK_SCF(true);
                }
                break;
            }
            case 0x3c:		/* CREATE Create of truncate file */
            {
                String name1 = Memory.MEM_StrCopy(CPU_Regs.reg_dsPhys.dword+CPU_Regs.reg_edx.word(),256);
                IntRef ax = new IntRef(CPU_Regs.reg_eax.word());
                if (Dos_files.DOS_CreateFile(name1,CPU_Regs.reg_ecx.word(),ax)) {
                    CPU_Regs.reg_eax.word(ax.value);
                    Callback.CALLBACK_SCF(false);
                } else {
                    CPU_Regs.reg_eax.word(dos.errorcode);
                    Callback.CALLBACK_SCF(true);
                }
                break;
            }
            case 0x3d:		/* OPEN Open existing file */
            {
                String name1 = Memory.MEM_StrCopy(CPU_Regs.reg_dsPhys.dword+CPU_Regs.reg_edx.word(),256);
                IntRef ax = new IntRef(CPU_Regs.reg_eax.word());
                if (Dos_files.DOS_OpenFile(name1,CPU_Regs.reg_eax.low(),ax)) {
                    CPU_Regs.reg_eax.word(ax.value);
                    Callback.CALLBACK_SCF(false);
                } else {
                    CPU_Regs.reg_eax.word(dos.errorcode);
                    Callback.CALLBACK_SCF(true);
                }
                break;
            }
            case 0x3e:		/* CLOSE Close file */
                if (Dos_files.DOS_CloseFile(CPU_Regs.reg_ebx.word())) {
                    Callback.CALLBACK_SCF(false);
                } else {
                    CPU_Regs.reg_eax.word(dos.errorcode);
                    Callback.CALLBACK_SCF(true);
                }
                break;
            case 0x3f:		/* READ Read from file or device */
                {
                    /*Bit16u*/IntRef toread=new IntRef(CPU_Regs.reg_ecx.word());
                    dos.echo=true;
                    if (Dos_files.DOS_ReadFile(CPU_Regs.reg_ebx.word(),dos_copybuf,toread)) {
                        Memory.MEM_BlockWrite(CPU_Regs.reg_dsPhys.dword+CPU_Regs.reg_edx.word(),dos_copybuf,toread.value);
                        CPU_Regs.reg_eax.word(toread.value);
                        Callback.CALLBACK_SCF(false);
                    } else {
                        CPU_Regs.reg_eax.word(dos.errorcode);
                        Callback.CALLBACK_SCF(true);
                    }
                    modify_cycles(CPU_Regs.reg_eax.word());
                    dos.echo=false;
                    break;
                }
            case 0x40:					/* WRITE Write to file or device */
                {
                    /*Bit16u*/IntRef towrite=new IntRef(CPU_Regs.reg_ecx.word());
                    Memory.MEM_BlockRead(CPU_Regs.reg_dsPhys.dword+CPU_Regs.reg_edx.word(),dos_copybuf,towrite.value);
                    if (Dos_files.DOS_WriteFile(CPU_Regs.reg_ebx.word(),dos_copybuf,towrite)) {
                        CPU_Regs.reg_eax.word(towrite.value);
                        Callback.CALLBACK_SCF(false);
                    } else {
                        CPU_Regs.reg_eax.word(dos.errorcode);
                        Callback.CALLBACK_SCF(true);
                    }
                    modify_cycles(CPU_Regs.reg_eax.word());
                    break;
                }
            case 0x41:					/* UNLINK Delete file */
            {
                String name1 = Memory.MEM_StrCopy(CPU_Regs.reg_dsPhys.dword+CPU_Regs.reg_edx.word(),256);
                if (Dos_files.DOS_UnlinkFile(name1)) {
                    Callback.CALLBACK_SCF(false);
                } else {
                    CPU_Regs.reg_eax.word(dos.errorcode);
                    Callback.CALLBACK_SCF(true);
                }
                break;
            }
            case 0x42:					/* LSEEK Set current file position */
                {
                    /*Bit32u*/LongRef pos=new LongRef((((long)CPU_Regs.reg_ecx.word()<<16) + CPU_Regs.reg_edx.word()) & 0xFFFFFFFFl);
                    if (Dos_files.DOS_SeekFile(CPU_Regs.reg_ebx.word(),pos,CPU_Regs.reg_eax.low())) {
                        CPU_Regs.reg_edx.word((/*Bit16u*/int)(pos.value >>> 16));
                        CPU_Regs.reg_eax.word((/*Bit16u*/int)(pos.value & 0xFFFF));
                        Callback.CALLBACK_SCF(false);
                    } else {
                        CPU_Regs.reg_eax.word(dos.errorcode);
                        Callback.CALLBACK_SCF(true);
                    }
                    break;
                }
            case 0x43:					/* Get/Set file attributes */
            {
                String name1 = Memory.MEM_StrCopy(CPU_Regs.reg_dsPhys.dword+CPU_Regs.reg_edx.word(),256);
                switch (CPU_Regs.reg_eax.low()) {
                case 0x00:				/* Get */
                    {
                        /*Bit16u*/IntRef attr_val=new IntRef(CPU_Regs.reg_ecx.word());
                        if (Dos_files.DOS_GetFileAttr(name1,attr_val)) {
                            CPU_Regs.reg_ecx.word(attr_val.value);
                            CPU_Regs.reg_eax.word(attr_val.value); /* Undocumented */
                            Callback.CALLBACK_SCF(false);
                        } else {
                            Callback.CALLBACK_SCF(true);
                            CPU_Regs.reg_eax.word(dos.errorcode);
                        }
                        break;
                    }
                case 0x01:				/* Set */
                    if (Log.level<=LogSeverities.LOG_ERROR) Log.log(LogTypes.LOG_MISC,LogSeverities.LOG_ERROR,"DOS:Set File Attributes for "+name1+" not supported");
                    if (Dos_files.DOS_SetFileAttr(name1,CPU_Regs.reg_ecx.word())) {
                        CPU_Regs.reg_eax.word(0x202);	/* ax destroyed */
                        Callback.CALLBACK_SCF(false);
                    } else {
                        Callback.CALLBACK_SCF(true);
                        CPU_Regs.reg_eax.word(dos.errorcode);
                    }
                    break;
                default:
                    if (Log.level<=LogSeverities.LOG_ERROR) Log.log(LogTypes.LOG_MISC,LogSeverities.LOG_ERROR,"DOS:0x43:Illegal subfunction "+Integer.toString(CPU_Regs.reg_eax.low(),16));
                    CPU_Regs.reg_eax.word(1);
                    Callback.CALLBACK_SCF(true);
                    break;
                }
                break;
            }
            case 0x44:					/* IOCTL Functions */
                if (Dos_ioctl.DOS_IOCTL()) {
                    Callback.CALLBACK_SCF(false);
                } else {
                    CPU_Regs.reg_eax.word(dos.errorcode);
                    Callback.CALLBACK_SCF(true);
                }
                break;
            case 0x45:					/* DUP Duplicate file handle */
            {
                IntRef ax = new IntRef(CPU_Regs.reg_eax.word());
                if (Dos_files.DOS_DuplicateEntry(CPU_Regs.reg_ebx.word(),ax)) {
                    CPU_Regs.reg_eax.word(ax.value);
                    Callback.CALLBACK_SCF(false);
                } else {
                    CPU_Regs.reg_eax.word(dos.errorcode);
                    Callback.CALLBACK_SCF(true);
                }
                break;
            }
            case 0x46:					/* DUP2,FORCEDUP Force duplicate file handle */
                if (Dos_files.DOS_ForceDuplicateEntry(CPU_Regs.reg_ebx.word(),CPU_Regs.reg_ecx.word())) {
                    CPU_Regs.reg_eax.word(CPU_Regs.reg_ecx.word()); //Not all sources agree on it.
                    Callback.CALLBACK_SCF(false);
                } else {
                    CPU_Regs.reg_eax.word(dos.errorcode);
                    Callback.CALLBACK_SCF(true);
                }
                break;
            case 0x47:					/* CWD Get current directory */
            {
                StringRef name1 = new StringRef();
                if (Dos_files.DOS_GetCurrentDir((short)CPU_Regs.reg_edx.low(),name1)) {
                    Memory.MEM_BlockWrite(CPU_Regs.reg_dsPhys.dword+CPU_Regs.reg_esi.word(),name1.value,(/*Bitu*/int)(name1.value.length()+1));
                    CPU_Regs.reg_eax.word(0x0100);
                    Callback.CALLBACK_SCF(false);
                } else {
                    CPU_Regs.reg_eax.word(dos.errorcode);
                    Callback.CALLBACK_SCF(true);
                }
                break;
            }
            case 0x48:					/* Allocate memory */
                {
                    /*Bit16u*/IntRef size=new IntRef(CPU_Regs.reg_ebx.word());/*Bit16u*/IntRef seg=new IntRef(0);
                    if (Dos_memory.DOS_AllocateMemory(seg,size)) {
                        CPU_Regs.reg_eax.word(seg.value);
                        Callback.CALLBACK_SCF(false);
                    } else {
                        CPU_Regs.reg_eax.word(dos.errorcode);
                        CPU_Regs.reg_ebx.word(size.value);
                        Callback.CALLBACK_SCF(true);
                    }
                    break;
                }
            case 0x49:					/* Free memory */
                if (Dos_memory.DOS_FreeMemory((int)CPU_Regs.reg_esVal.dword)) {
                    Callback.CALLBACK_SCF(false);
                } else {
                    CPU_Regs.reg_eax.word(dos.errorcode);
                    Callback.CALLBACK_SCF(true);
                }
                break;
            case 0x4a:					/* Resize memory block */
                {
                    /*Bit16u*/IntRef size=new IntRef(CPU_Regs.reg_ebx.word());
                    if (Dos_memory.DOS_ResizeMemory((int)CPU_Regs.reg_esVal.dword,size)) {
                        CPU_Regs.reg_eax.word((int)CPU_Regs.reg_esVal.dword);
                        Callback.CALLBACK_SCF(false);
                    } else {
                        CPU_Regs.reg_eax.word(dos.errorcode);
                        CPU_Regs.reg_ebx.word(size.value);
                        Callback.CALLBACK_SCF(true);
                    }
                    break;
                }
            case 0x4b:					/* EXEC Load and/or execute program */
                {
                    String name1 = Memory.MEM_StrCopy(CPU_Regs.reg_dsPhys.dword+CPU_Regs.reg_edx.word(),256);
                    if (Log.level<=LogSeverities.LOG_ERROR) Log.log(LogTypes.LOG_EXEC,LogSeverities.LOG_ERROR,"Execute "+name1+" "+CPU_Regs.reg_eax.low());
                    if (!Dos_execute.DOS_Execute(name1,CPU_Regs.reg_esPhys.dword+CPU_Regs.reg_ebx.word(),(short)CPU_Regs.reg_eax.low())) {
                        CPU_Regs.reg_eax.word(dos.errorcode);
                        Callback.CALLBACK_SCF(true);
                    }
                }
                break;
    //TODO Check for use of execution state AL=5
            case 0x4c:					/* EXIT Terminate with return code */
                Dos_execute.DOS_Terminate(dos.psp(),false,CPU_Regs.reg_eax.low());
                break;
            case 0x4d:					/* Get Return code */
                CPU_Regs.reg_eax.low(dos.return_code);/* Officially read from SDA and clear when read */
                CPU_Regs.reg_eax.high(dos.return_mode);
                break;
            case 0x4e:					/* FINDFIRST Find first matching file */
            {
                String name1 = Memory.MEM_StrCopy(CPU_Regs.reg_dsPhys.dword+CPU_Regs.reg_edx.word(),256);
                if (Dos_files.DOS_FindFirst(name1,CPU_Regs.reg_ecx.word())) {
                    Callback.CALLBACK_SCF(false);
                    CPU_Regs.reg_eax.word(0);			/* Undocumented */
                } else {
                    CPU_Regs.reg_eax.word(dos.errorcode);
                    Callback.CALLBACK_SCF(true);
                }
                break;
            }
            case 0x4f:					/* FINDNEXT Find next matching file */
                if (Dos_files.DOS_FindNext()) {
                    Callback.CALLBACK_SCF(false);
                    /* CPU_Regs.reg_eax.word()=0xffff;*/			/* Undocumented */
                    CPU_Regs.reg_eax.word(0);				/* Undocumented:Qbix Willy beamish */
                } else {
                    CPU_Regs.reg_eax.word(dos.errorcode);
                    Callback.CALLBACK_SCF(true);
                }
                break;
            case 0x50:					/* Set current PSP */
                dos.psp(CPU_Regs.reg_ebx.word());
                break;
            case 0x51:					/* Get current PSP */
                CPU_Regs.reg_ebx.word(dos.psp());
                break;
            case 0x52: {				/* Get list of lists */
                /*RealPt*/int addr=dos_infoblock.GetPointer();
                CPU_Regs.SegSet16ES(Memory.RealSeg(addr));
                CPU_Regs.reg_ebx.word(Memory.RealOff(addr));
                Log.log(LogTypes.LOG_DOSMISC,LogSeverities.LOG_NORMAL,"Call is made for list of lists - let's hope for the best");
                break; }
    //TODO Think hard how shit this is gonna be
    //And will any game ever use this :)
            case 0x53:					/* Translate BIOS parameter block to drive parameter block */
                Log.exit("Unhandled Dos 21 call "+Integer.toString(CPU_Regs.reg_eax.high(),16));
                break;
            case 0x54:					/* Get verify flag */
                CPU_Regs.reg_eax.low(dos.verify?1:0);
                break;
            case 0x55:					/* Create Child PSP*/
                Dos_execute.DOS_ChildPSP(CPU_Regs.reg_edx.word(),CPU_Regs.reg_esi.word());
                dos.psp(CPU_Regs.reg_edx.word());
                CPU_Regs.reg_eax.low(0xf0);	/* al destroyed */
                break;
            case 0x56:					/* RENAME Rename file */
            {
                String name1 = Memory.MEM_StrCopy(CPU_Regs.reg_dsPhys.dword+CPU_Regs.reg_edx.word(),256);
                String name2 = Memory.MEM_StrCopy(CPU_Regs.reg_esPhys.dword+CPU_Regs.reg_edi.word(),256);
                if (Dos_files.DOS_Rename(name1,name2)) {
                    Callback.CALLBACK_SCF(false);
                } else {
                    CPU_Regs.reg_eax.word(dos.errorcode);
                    Callback.CALLBACK_SCF(true);
                }
                break;
            }
            case 0x57:					/* Get/Set File's Date and Time */
                if (CPU_Regs.reg_eax.low()==0x00) {
                    IntRef cx = new IntRef(CPU_Regs.reg_ecx.word());
                    IntRef dx = new IntRef(CPU_Regs.reg_edx.word());
                    if (Dos_files.DOS_GetFileDate(CPU_Regs.reg_ebx.word(),cx,dx)) {
                        CPU_Regs.reg_ecx.word(cx.value);
                        CPU_Regs.reg_edx.word(dx.value);
                        Callback.CALLBACK_SCF(false);
                    } else {
                        Callback.CALLBACK_SCF(true);
                    }
                } else if (CPU_Regs.reg_eax.low()==0x01) {
                    Log.log(LogTypes.LOG_DOSMISC,LogSeverities.LOG_ERROR,"DOS:57:Set File Date Time Faked");
                    Callback.CALLBACK_SCF(false);
                } else {
                    if (Log.level<=LogSeverities.LOG_ERROR) Log.log(LogTypes.LOG_DOSMISC,LogSeverities.LOG_ERROR,"DOS:57:Unsupported subtion "+Integer.toString(CPU_Regs.reg_eax.low(),16));
                }
                break;
            case 0x58:					/* Get/Set Memory allocation strategy */
                switch (CPU_Regs.reg_eax.low()) {
                case 0:					/* Get Strategy */
                    CPU_Regs.reg_eax.word(Dos_memory.DOS_GetMemAllocStrategy());
                    break;
                case 1:					/* Set Strategy */
                    if (Dos_memory.DOS_SetMemAllocStrategy(CPU_Regs.reg_ebx.word())) Callback.CALLBACK_SCF(false);
                    else {
                        CPU_Regs.reg_eax.word(1);
                        Callback.CALLBACK_SCF(true);
                    }
                    break;
                case 2:					/* Get UMB Link Status */
                    CPU_Regs.reg_eax.low(dos_infoblock.GetUMBChainState()&1);
                    Callback.CALLBACK_SCF(false);
                    break;
                case 3:					/* Set UMB Link Status */
                    if (Dos_memory.DOS_LinkUMBsToMemChain(CPU_Regs.reg_ebx.word())) Callback.CALLBACK_SCF(false);
                    else {
                        CPU_Regs.reg_eax.word(1);
                        Callback.CALLBACK_SCF(true);
                    }
                    break;
                default:
                    if (Log.level<=LogSeverities.LOG_ERROR) Log.log(LogTypes.LOG_DOSMISC,LogSeverities.LOG_ERROR,"DOS:58:Not Supported Set//Get memory allocation call "+Integer.toString(CPU_Regs.reg_eax.low(),16));
                    CPU_Regs.reg_eax.word(1);
                    Callback.CALLBACK_SCF(true);
                }
                break;
            case 0x59:					/* Get Extended error information */
                CPU_Regs.reg_eax.word(dos.errorcode);
                if (dos.errorcode==DOSERR_FILE_NOT_FOUND || dos.errorcode==DOSERR_PATH_NOT_FOUND) {
                    CPU_Regs.reg_ebx.high(8);	//Not Found error class (Road Hog)
                } else {
                    CPU_Regs.reg_ebx.high(0);	//Unspecified error class
                }
                CPU_Regs.reg_ebx.low(1);	//Retry retry retry
                CPU_Regs.reg_ecx.high(0);	//Unkown error locus
                break;
            case 0x5a:					/* Create temporary file */
                {
                    /*Bit16u*/IntRef handle=new IntRef(0);
                    StringRef name1 = new StringRef(Memory.MEM_StrCopy(CPU_Regs.reg_dsPhys.dword+CPU_Regs.reg_edx.word(),256));
                    if (Dos_files.DOS_CreateTempFile(name1,handle)) {
                        CPU_Regs.reg_eax.word(handle.value);
                        Memory.MEM_BlockWrite(CPU_Regs.reg_dsPhys.dword+CPU_Regs.reg_edx.word(),name1.value,(/*Bitu*/int)(name1.value.length()+1));
                        Callback.CALLBACK_SCF(false);
                    } else {
                        CPU_Regs.reg_eax.word(dos.errorcode);
                        Callback.CALLBACK_SCF(true);
                    }
                }
                break;
            case 0x5b:					/* Create new file */
                {
                    String name1 = Memory.MEM_StrCopy(CPU_Regs.reg_dsPhys.dword+CPU_Regs.reg_edx.word(),256);
                    /*Bit16u*/IntRef handle=new IntRef(0);
                    if (Dos_files.DOS_OpenFile(name1,0,handle)) {
                        Dos_files.DOS_CloseFile(handle.value);
                        DOS_SetError(DOSERR_FILE_ALREADY_EXISTS);
                        CPU_Regs.reg_eax.word(dos.errorcode);
                        Callback.CALLBACK_SCF(true);
                        break;
                    }
                    if (Dos_files.DOS_CreateFile(name1,CPU_Regs.reg_ecx.word(),handle)) {
                        CPU_Regs.reg_eax.word(handle.value);
                        Callback.CALLBACK_SCF(false);
                    } else {
                        CPU_Regs.reg_eax.word(dos.errorcode);
                        Callback.CALLBACK_SCF(true);
                    }
                    break;
                }
            case 0x5c:			/* FLOCK File region locking */
                DOS_SetError(DOSERR_FUNCTION_NUMBER_INVALID);
                CPU_Regs.reg_eax.word(dos.errorcode);
                Callback.CALLBACK_SCF(true);
                break;
            case 0x5d:					/* Network Functions */
                if(CPU_Regs.reg_eax.low() == 0x06) {
                    CPU_Regs.SegSet16DS(DOS_SDA_SEG);
                    CPU_Regs.reg_esi.word(DOS_SDA_OFS);
                    CPU_Regs.reg_ecx.word(0x80);  // swap if in dos
                    CPU_Regs.reg_edx.word(0x1a);  // swap always
                    Log.log(LogTypes.LOG_DOSMISC,LogSeverities.LOG_ERROR,"Get SDA, Let's hope for the best!");
                }
                break;
            case 0x5f:					/* Network redirection */
                CPU_Regs.reg_eax.word(0x0001);		//Failing it
                Callback.CALLBACK_SCF(true);
                break;
            case 0x60:					/* Canonicalize filename or path */
            {
                String name1 = Memory.MEM_StrCopy(CPU_Regs.reg_dsPhys.dword+CPU_Regs.reg_esi.word(),256);
                StringRef name2 = new StringRef();
                if (Dos_files.DOS_Canonicalize(name1,name2)) {
                        Memory.MEM_BlockWrite(CPU_Regs.reg_esPhys.dword+CPU_Regs.reg_edi.word(),name2.value,(/*Bitu*/int)name2.value.length()+1);
                        Callback.CALLBACK_SCF(false);
                    } else {
                        CPU_Regs.reg_eax.word(dos.errorcode);
                        Callback.CALLBACK_SCF(true);
                    }
                    break;
            }
            case 0x62:					/* Get Current PSP Address */
                CPU_Regs.reg_ebx.word(dos.psp());
                break;
            case 0x63:					/* DOUBLE BYTE CHARACTER SET */
                if(CPU_Regs.reg_eax.low() == 0) {
                    CPU_Regs.SegSet16DS(Memory.RealSeg(dos.tables.dbcs));
                    CPU_Regs.reg_esi.word(Memory.RealOff(dos.tables.dbcs));
                    CPU_Regs.reg_eax.low(0);
                    Callback.CALLBACK_SCF(false); //undocumented
                } else CPU_Regs.reg_eax.low(0xff); //Doesn't officially touch carry flag
                break;
            case 0x64:					/* Set device driver lookahead flag */
                Log.log(LogTypes.LOG_DOSMISC,LogSeverities.LOG_NORMAL,"set driver look ahead flag");
                break;
            case 0x65:					/* Get extented country information and a lot of other useless shit*/
                { /* Todo maybe fully support this for now we set it standard for USA */
                    if (Log.level<=LogSeverities.LOG_ERROR) Log.log(LogTypes.LOG_DOSMISC,LogSeverities.LOG_ERROR,"DOS:65:Extended country information call "+Integer.toString(CPU_Regs.reg_eax.word(),16));
                    if((CPU_Regs.reg_eax.low() <=  0x07) && (CPU_Regs.reg_ecx.word() < 0x05)) {
                        DOS_SetError(DOSERR_FUNCTION_NUMBER_INVALID);
                        Callback.CALLBACK_SCF(true);
                        break;
                    }
                    /*Bitu*/int len = 0; /* For 0x21 and 0x22 */
                    /*PhysPt*/int data=CPU_Regs.reg_esPhys.dword+CPU_Regs.reg_edi.word();
                    switch (CPU_Regs.reg_eax.low()) {
                    case 0x01:
                        Memory.mem_writeb(data + 0x00,CPU_Regs.reg_eax.low());
                        Memory.mem_writew(data + 0x01,0x26);
                        Memory.mem_writew(data + 0x03,1);
                        if(CPU_Regs.reg_ecx.word() > 0x06 ) Memory.mem_writew(data+0x05,dos.loaded_codepage);
                        if(CPU_Regs.reg_ecx.word() > 0x08 ) {
                            /*Bitu*/int amount = (CPU_Regs.reg_ecx.word()>=0x29)?0x22:(CPU_Regs.reg_ecx.word()-7);
                            Memory.MEM_BlockWrite(data + 0x07,dos.tables.country,amount);
                            CPU_Regs.reg_ecx.word((CPU_Regs.reg_ecx.word()>=0x29)?0x29:CPU_Regs.reg_ecx.word());
                        }
                        Callback.CALLBACK_SCF(false);
                        break;
                    case 0x05: // Get pointer to filename terminator table
                        Memory.mem_writeb(data + 0x00, CPU_Regs.reg_eax.low());
                        Memory.mem_writed(data + 0x01, dos.tables.filenamechar);
                        CPU_Regs.reg_ecx.word(5);
                        Callback.CALLBACK_SCF(false);
                        break;
                    case 0x02: // Get pointer to uppercase table
                        Memory.mem_writeb(data + 0x00, CPU_Regs.reg_eax.low());
                        Memory.mem_writed(data + 0x01, dos.tables.upcase);
                        CPU_Regs.reg_ecx.word(5);
                        Callback.CALLBACK_SCF(false);
                        break;
                    case 0x06: // Get pointer to collating sequence table
                        Memory.mem_writeb(data + 0x00, CPU_Regs.reg_eax.low());
                        Memory.mem_writed(data + 0x01, dos.tables.collatingseq);
                        CPU_Regs.reg_ecx.word(5);
                        Callback.CALLBACK_SCF(false);
                        break;
                    case 0x03: // Get pointer to lowercase table
                    case 0x04: // Get pointer to filename uppercase table
                    case 0x07: // Get pointer to double byte char set table
                        Memory.mem_writeb(data + 0x00, CPU_Regs.reg_eax.low());
                        Memory.mem_writed(data + 0x01, dos.tables.dbcs); //used to be 0
                        CPU_Regs.reg_ecx.word(5);
                        Callback.CALLBACK_SCF(false);
                        break;
                    case 0x20: /* Capitalize Character */
                        {
                            int in  = CPU_Regs.reg_edx.low();
                            int out = String.valueOf((char)in).toUpperCase().charAt(0);
                            CPU_Regs.reg_edx.low((/*Bit8u*/short)out);
                        }
                        Callback.CALLBACK_SCF(false);
                        break;
                    case 0x21: /* Capitalize String (cx=length) */
                    case 0x22: /* Capatilize ASCIZ string */
                        data = CPU_Regs.reg_dsPhys.dword + CPU_Regs.reg_edx.word();
                        if(CPU_Regs.reg_eax.low() == 0x21) len = CPU_Regs.reg_ecx.word();
                        else len = Memory.mem_strlen(data); /* Is limited to 1024 */

                        if(len > DOS_COPYBUFSIZE - 1) Log.exit("DOS:0x65 Buffer overflow");
                        if(len>0) {
                            Memory.MEM_BlockRead(data,dos_copybuf,len);
                            System.arraycopy(new String(dos_copybuf, 0, len).toUpperCase().getBytes(), 0, dos_copybuf, 0, len);
                            Memory.MEM_BlockWrite(data,dos_copybuf,len);
                        }
                        Callback.CALLBACK_SCF(false);
                        break;
                    default:
                        Log.exit("DOS:0x65:Unhandled country information call "+Integer.toString(CPU_Regs.reg_eax.low(),16));
                    }
                    break;
                }
            case 0x66:					/* Get/Set global code page table  */
                if (CPU_Regs.reg_eax.low()==1) {
                    Log.log(LogTypes.LOG_DOSMISC,LogSeverities.LOG_ERROR,"Getting global code page table");
                    CPU_Regs.reg_ebx.word(dos.loaded_codepage);CPU_Regs.reg_edx.word(dos.loaded_codepage);
                    Callback.CALLBACK_SCF(false);
                    break;
                }
                Log.log(LogTypes.LOG_DOSMISC,LogSeverities.LOG_NORMAL,"DOS:Setting code page table is not supported");
                break;
            case 0x67:					/* Set handle count */
                /* Weird call to increase amount of file handles needs to allocate memory if >20 */
                {
                    Dos_PSP psp=new Dos_PSP(dos.psp());
                    psp.SetNumFiles(CPU_Regs.reg_ebx.word());
                    Callback.CALLBACK_SCF(false);
                    break;
                }
            case 0x68:                  /* FFLUSH Commit file */
                if(Dos_files.DOS_FlushFile(CPU_Regs.reg_ebx.low())) {
                    Callback.CALLBACK_SCF(false);
                } else {
                    CPU_Regs.reg_eax.word(dos.errorcode);
                    Callback.CALLBACK_SCF(true);
                }
                break;
            case 0x69:					/* Get/Set disk serial number */
                {
                    switch(CPU_Regs.reg_eax.low())		{
                    case 0x00:				/* Get */
                        Log.log(LogTypes.LOG_DOSMISC,LogSeverities.LOG_ERROR,"DOS:Get Disk serial number");
                        Callback.CALLBACK_SCF(true);
                        break;
                    case 0x01:
                        Log.log(LogTypes.LOG_DOSMISC,LogSeverities.LOG_ERROR,"DOS:Set Disk serial number");
                    default:
                        Log.exit("DOS:Illegal Get Serial Number call "+Integer.toString(CPU_Regs.reg_eax.low(),16));
                    }
                    break;
                }
            case 0x6c:					/* Extended Open/Create */
            {
                String name1 = Memory.MEM_StrCopy(CPU_Regs.reg_dsPhys.dword+CPU_Regs.reg_esi.word(),256);
                IntRef ax = new IntRef(CPU_Regs.reg_eax.word());
                IntRef cx = new IntRef(CPU_Regs.reg_ecx.word());
                if (Dos_files.DOS_OpenFileExtended(name1,CPU_Regs.reg_ebx.word(),CPU_Regs.reg_ecx.word(),CPU_Regs.reg_edx.word(),ax,cx)) {
                    CPU_Regs.reg_eax.word(ax.value);
                    CPU_Regs.reg_ecx.word(cx.value);
                    Callback.CALLBACK_SCF(false);
                } else {
                    CPU_Regs.reg_eax.word(dos.errorcode);
                    Callback.CALLBACK_SCF(true);
                }
                break;
            }
            case 0x71:					/* Unknown probably 4dos detection */
                CPU_Regs.reg_eax.word(0x7100);
                Callback.CALLBACK_SCF(true);
                if (Log.level<=LogSeverities.LOG_NORMAL) Log.log(LogTypes.LOG_DOSMISC,LogSeverities.LOG_NORMAL,"DOS:Windows long file name support call "+Integer.toString(CPU_Regs.reg_eax.low(),16));
                break;

            case 0xE0:
            case 0x18:	            	/* NULL Function for CP/M compatibility or Extended rename FCB */
            case 0x1d:	            	/* NULL Function for CP/M compatibility or Extended rename FCB */
            case 0x1e:	            	/* NULL Function for CP/M compatibility or Extended rename FCB */
            case 0x20:	            	/* NULL Function for CP/M compatibility or Extended rename FCB */
            case 0x6b:		            /* NULL Function */
            case 0x61:		            /* UNUSED */
            case 0xEF:                  /* Used in Ancient Art Of War CGA */
            case 0x5e:					/* More Network Functions */
            default:
                if (Log.level<=LogSeverities.LOG_ERROR) Log.log(LogTypes.LOG_DOSMISC,LogSeverities.LOG_ERROR,"DOS:Unhandled call "+Integer.toString(CPU_Regs.reg_eax.high(), 16)+" al="+Integer.toString(CPU_Regs.reg_eax.low(), 16)+". Set al to default of 0");
                CPU_Regs.reg_eax.low(0x00); /* default value */
                break;
            }
            return Callback.CBRET_NONE;
        }
    };

    static private Callback.Handler DOS_20Handler = new Callback.Handler() {
        public String getName() {
            return "Dos.DOS_20Handler";
        }
        public /*Bitu*/int call() {
            CPU_Regs.reg_eax.high(0x00);
            DOS_21Handler.call();
            return Callback.CBRET_NONE;
        }
    };

    static private Callback.Handler DOS_27Handler = new Callback.Handler() {
        public String getName() {
            return "Dos.DOS_27Handler";
        }
        public /*Bitu*/int call() {
            // Terminate & stay resident
            /*Bit16u*/IntRef para = new IntRef((CPU_Regs.reg_edx.word()/16)+(((CPU_Regs.reg_edx.word() % 16)>0)?1:0));
            /*Bit16u*/int psp = dos.psp(); //mem_readw(CPU_Regs.SegPhys(ss)+CPU_Regs.reg_esp.word()+2);
            if (Dos_memory.DOS_ResizeMemory(psp,para)) Dos_execute.DOS_Terminate(psp,true,0);
            return Callback.CBRET_NONE;
        }
    };

    static private Callback.Handler DOS_25Handler = new Callback.Handler() {
        public String getName() {
            return "Dos.DOS_25Handler";
        }
        public /*Bitu*/int call() {
            int drive = CPU_Regs.reg_eax.low();
            if(drive>=Dos_files.Drives.length || Dos_files.Drives[CPU_Regs.reg_eax.low()]==null){
                CPU_Regs.reg_eax.word(0x8002);
                CPU_Regs.SETFLAGBIT(CPU_Regs.CF,true);
            }else{
                CPU_Regs.SETFLAGBIT(CPU_Regs.CF,false);
                if((CPU_Regs.reg_ecx.word() != 1) ||(CPU_Regs.reg_edx.word() != 1))
                    if (Log.level<=LogSeverities.LOG_NORMAL) Log.log(LogTypes.LOG_DOSMISC,LogSeverities.LOG_NORMAL,"int 25 called but not as diskdetection drive "+Integer.toString(CPU_Regs.reg_eax.low(),16));

               CPU_Regs.reg_eax.word(0);
            }
            return Callback.CBRET_NONE;
        }
    };

    static private Callback.Handler DOS_26Handler = new Callback.Handler() {
        public String getName() {
            return "Dos.DOS_26Handler";
        }
        public /*Bitu*/int call() {
            Log.log(LogTypes.LOG_DOSMISC,LogSeverities.LOG_NORMAL,"int 26 called: hope for the best!");
            if(Dos_files.Drives[CPU_Regs.reg_eax.low()]==null){
                CPU_Regs.reg_eax.word(0x8002);
                CPU_Regs.SETFLAGBIT(CPU_Regs.CF,true);
            }else{
                CPU_Regs.SETFLAGBIT(CPU_Regs.CF,false);
                CPU_Regs.reg_eax.word(0);
            }
            return Callback.CBRET_NONE;
        }
    };

    private Callback[] callback=new Callback[7];

    private static Dos test;

    public static Section.SectionFunction DOS_ShutDown = new Section.SectionFunction() {
        public void call(Section section) {
            test=null;
            for (/*Bit16u*/int i=0;i<Dos_files.DOS_DRIVES;i++) {
                if (Dos_files.Drives[i]!=null) {
                    Dos_files.Drives[i].UnMount();
                    Dos_files.Drives[i]=null;
                }
            }
        }
    };
    
    public Dos(Section configuration) {
        super(configuration);
        for (int i=0;i<callback.length;i++)
            callback[i] = new Callback();
        callback[0].Install(DOS_20Handler,Callback.CB_IRET,"DOS Int 20");
        callback[0].Set_RealVec(0x20);

        callback[1].Install(DOS_21Handler,Callback.CB_INT21,"DOS Int 21");
        callback[1].Set_RealVec(0x21);
        //Pseudo code for int 21
        // sti
        // callback
        // iret
        // retf  <- int 21 4c jumps here to mimic a retf Cyber

        callback[2].Install(DOS_25Handler,Callback.CB_RETF,"DOS Int 25");
        callback[2].Set_RealVec(0x25);

        callback[3].Install(DOS_26Handler,Callback.CB_RETF,"DOS Int 26");
        callback[3].Set_RealVec(0x26);

        callback[4].Install(DOS_27Handler,Callback.CB_IRET,"DOS Int 27");
        callback[4].Set_RealVec(0x27);

        callback[5].Install(null,Callback.CB_IRET,"DOS Int 28");
        callback[5].Set_RealVec(0x28);

        callback[6].Install(null,Callback.CB_INT29,"CON Output Int 29");
        callback[6].Set_RealVec(0x29);
        // pseudocode for CB_INT29:
        //	push ax
        //	mov ah, 0x0e
        //	int 0x10
        //	pop ax
        //	iret

        Dos_files.DOS_SetupFiles();								/* Setup system File tables */
        Dos_devices.DOS_SetupDevices();							/* Setup dos devices */
        Dos_tables.DOS_SetupTables();
        Dos_memory.DOS_SetupMemory();								/* Setup first MCB */
        Dos_programs.DOS_SetupPrograms();
        Dos_misc.DOS_SetupMisc();							/* Some additional dos interrupts */
        new Dos_SDA(DOS_SDA_SEG,DOS_SDA_OFS).SetDrive((short)25); /* Else the next call gives a warning. */
        Dos_files.DOS_SetDefaultDrive((short)25);

        dos.version.major=5;
        dos.version.minor=0;
    }

    public static Section.SectionFunction DOS_Init = new Section.SectionFunction() {
        public void call(Section section) {
            test = new Dos(section);
            /* shutdown function */
            section.AddDestroyFunction(DOS_ShutDown,false);
        }
    };
}
