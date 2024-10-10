package jdos.dos;

import jdos.Dosbox;
import jdos.cpu.CPU_Regs;
import jdos.cpu.Callback;
import jdos.hardware.Memory;
import jdos.ints.Bios;
import jdos.ints.Bios_keyboard;
import jdos.ints.Int10;
import jdos.ints.Int10_char;
import jdos.misc.Log;
import jdos.types.LogSeverities;
import jdos.types.LogTypes;
import jdos.util.IntRef;
import jdos.util.LongRef;
import jdos.util.ShortRef;
import jdos.util.StringRef;

public class Dos_devices {
    static public final int DOS_DEVICES = 10;

    static public DOS_Device[] Devices;

    public static /*Bit8u*/short DOS_FindDevice(String name) {
        /* should only check for the names before the dot and spacepadded */
        StringRef fullname = new StringRef(); ShortRef drive = new ShortRef();
//	if(!name || !(*name)) return DOS_DEVICES; //important, but makename does it
        if (!Dos_files.DOS_MakeName(name,fullname,drive)) return DOS_DEVICES;

        int pos =fullname.value.lastIndexOf('\\');
        String name_part = null;
        if (pos>=0)
            name_part = fullname.value.substring(pos+1);
        if(name_part!=null) {
            //Check validity of leading directory.
            if(!Dos_files.Drives[drive.value].TestDir(fullname.value.substring(0, pos))) return DOS_DEVICES;
        } else name_part = fullname.value;

        pos = name_part.indexOf('.');
        if (pos>=0)
            name_part = name_part.substring(0, pos);//no ext checking

        final String com = "COM1";
        final String lpt = "LPT1";
        // AUX is alias for COM1 and PRN for LPT1
        // A bit of a hack. (but less then before).
        // no need for casecmp as makename returns uppercase
        if (name_part.equals("AUX")) name_part = com;
        if (name_part.equals("PRN")) name_part = lpt;

        /* loop through devices */
        for(/*Bit8u*/short index = 0;index < DOS_DEVICES;index++) {
            if (Devices[index]!=null) {
                if (Drives.WildFileCmp(name_part,Devices[index].name)) return index;
            }
        }
        return DOS_DEVICES;
    }


    public static void DOS_AddDevice(DOS_Device adddev) {
//Caller creates the device. We store a pointer to it
//TODO Give the Device a real handler in low memory that responds to calls
        for(/*Bitu*/int i = 0; i < DOS_DEVICES;i++) {
            if(Devices[i]==null){
                Devices[i] = adddev;
                Devices[i].SetDeviceNumber(i);
                return;
            }
        }
        Log.exit("DOS:Too many devices added");
    }

    static public void DOS_DelDevice(DOS_Device dev) {
// We will destroy the device if we find it in our list.
// TODO:The file table is not checked to see the device is opened somewhere!
        for (/*Bitu*/int i = 0; i <DOS_DEVICES;i++) {
            if(Devices[i]!=null && Devices[i].name.compareToIgnoreCase(dev.name)==0){
                Devices[i] = null;
                return;
            }
        }
    }

    static public void DOS_SetupDevices() {
        Devices = new DOS_Device[DOS_DEVICES];
        DOS_AddDevice(new device_CON());
        DOS_AddDevice(new device_NUL());
        DOS_AddDevice(new device_LPT1());
    }

    static private class device_NUL extends DOS_Device {
        public device_NUL() { SetName("NUL"); }
        public boolean Read(byte[] data,/*Bit16u*/IntRef size) {
            for(/*Bitu*/int i = 0; i < size.value;i++)
                data[i]=0;
            if (Log.level<=LogSeverities.LOG_NORMAL) Log.log(LogTypes.LOG_IOCTL, LogSeverities.LOG_NORMAL,GetName()+":READ");
            return true;
        }
        public boolean Write(byte[] data,/*Bit16u*/IntRef size) {
            if (Log.level<=LogSeverities.LOG_NORMAL) Log.log(LogTypes.LOG_IOCTL,LogSeverities.LOG_NORMAL,GetName()+":WRITE");
            return true;
        }
        public boolean Seek(/*Bit32u*/LongRef pos,/*Bit32u*/int type) {
            if (Log.level<=LogSeverities.LOG_NORMAL) Log.log(LogTypes.LOG_IOCTL,LogSeverities.LOG_NORMAL,GetName()+":SEEK");
            return true;
        }
        public boolean Close() { return true; }
        public /*Bit16u*/int GetInformation() { return 0x8084; }
        public boolean ReadFromControlChannel(/*PhysPt*/int bufptr,/*Bit16u*/int size,/*Bit16u*/IntRef retcode) {return false;}
        public boolean WriteToControlChannel(/*PhysPt*/int bufptr,/*Bit16u*/int size,/*Bit16u*/IntRef retcode) {return false;}
    }

    static private class device_LPT1 extends device_NUL {
        public device_LPT1() { SetName("LPT1");}
        public /*Bit16u*/int GetInformation() { return 0x80A0; }
    }


    static private final int NUMBER_ANSI_DATA = 10;

    static private class device_CON extends DOS_Device {
        public device_CON() {
            SetName("CON");
            readcache=0;
            lastwrite=0;
            ansi.enabled=false;
            ansi.attr=0x7;
            ansi.ncols=Memory.real_readw(Int10.BIOSMEM_SEG,Int10.BIOSMEM_NB_COLS); //should be updated once set/reset mode is implemented
            ansi.nrows=Memory.real_readb(Int10.BIOSMEM_SEG,Int10.BIOSMEM_NB_ROWS) + 1;
            ansi.saverow=0;
            ansi.savecol=0;
            ansi.warned=false;
            ClearAnsi();
        }
        public boolean Read(byte[] data,/*Bit16u*/IntRef size) {
            /*Bit16u*/int oldax= CPU_Regs.reg_eax.word();
            /*Bit16u*/int count=0;
            if (readcache!=0 && size.value!=0) {
                data[count++]=readcache;
                if(Dos.dos.echo) Int10_char.INT10_TeletypeOutput(readcache,7);
                readcache=0;
            }
            while (size.value>count) {
                CPU_Regs.reg_eax.high((Dosbox.IS_EGAVGA_ARCH())?0x10:0x0);
                Callback.CALLBACK_RunRealInt(0x16);
                switch(CPU_Regs.reg_eax.low() & 0xFF) {
                case 13:
                    data[count++]=0x0D;
                    if (size.value>count) data[count++]=0x0A;    // it's only expanded if there is room for it. (NO cache)
                    size.value=count;
                    CPU_Regs.reg_eax.word(oldax);
                    if(Dos.dos.echo) {
                        Int10_char.INT10_TeletypeOutput(13,7); //maybe don't do this ( no need for it actually ) (but it's compatible)
                        Int10_char.INT10_TeletypeOutput(10,7);
                    }
                    return true;
                case 8:
                    if(size.value==1) data[count++]=(byte)CPU_Regs.reg_eax.low();  //one char at the time so give back that BS
                    else if(count!=0) {                    //Remove data if it exists (extended keys don't go right)
                        data[count--]=0;
                        Int10_char.INT10_TeletypeOutput(8,7);
                        Int10_char.INT10_TeletypeOutput(' ',7);
                    } else {
                        continue;                       //no data read yet so restart whileloop.
                    }
                    break;
                case 0xe0: /* Extended keys in the  int 16 0x10 case */
                    if(CPU_Regs.reg_eax.high()==0) { /*extended key if CPU_Regs.reg_eax.high() isn't 0 */
                        data[count++] = (byte)CPU_Regs.reg_eax.low();
                    } else {
                        data[count++] = 0;
                        if (size.value>count) data[count++] = (byte)CPU_Regs.reg_eax.high();
                        else readcache = (byte)CPU_Regs.reg_eax.high();
                    }
                    break;
                case 0: /* Extended keys in the int 16 0x0 case */
                    data[count++]=(byte)CPU_Regs.reg_eax.low();
                    if (size.value>count) data[count++]=(byte)CPU_Regs.reg_eax.high();
                    else readcache=(byte)CPU_Regs.reg_eax.high();
                    break;
                default:
                    data[count++]=(byte)CPU_Regs.reg_eax.low();
                    break;
                }
                if(Dos.dos.echo) { //what to do if size.value==1 and character is BS ?????
                    Int10_char.INT10_TeletypeOutput(CPU_Regs.reg_eax.low(),7);
                }
            }
            size.value=count;
            CPU_Regs.reg_eax.word(oldax);
            return true;
        }
        public boolean Write(byte[] data,/*Bit16u*/IntRef size) {
            /*Bit16u*/int count=0;
            /*Bitu*/int i;
            /*Bit8u*/short col,row;
            /*Bit8u*/short tempdata;
            while (size.value>count) {
                if (!ansi.esc){
                    if(data[count]=='\033') {
                        /*clear the datastructure */
                        ClearAnsi();
                        /* start the sequence */
                        ansi.esc=true;
                        count++;
                        continue;
                    } else {
                        /* Some sort of "hack" now that \n doesn't set col to 0 (int10_char.cpp old chessgame) */
                        if((data[count] == '\n') && (lastwrite != '\r')) Int10_char.INT10_TeletypeOutputAttr('\r',ansi.attr,ansi.enabled);
                        /* pass attribute only if ansi is enabled */
                        Int10_char.INT10_TeletypeOutputAttr((data[count] & 0xFF),ansi.attr,ansi.enabled);
                        lastwrite = data[count++];
                        continue;
                }
            }

            if(!ansi.sci){

                switch(data[count]){
                case '[':
                    ansi.sci=true;
                    break;
                case '7': /* save cursor pos + attr */
                case '8': /* restore this  (Wonder if this is actually used) */
                case 'D':/* scrolling DOWN*/
                case 'M':/* scrolling UP*/
                default:
                    if (Log.level<=LogSeverities.LOG_NORMAL) Log.log(LogTypes.LOG_IOCTL,LogSeverities.LOG_NORMAL,"ANSI: unknown char "+String.valueOf((char)data[count])+" after a esc"); /*prob () */
                    ClearAnsi();
                    break;
                }
                count++;
                continue;
            }
            /*ansi.esc and ansi.sci are true */
            /*Bit8u*/short page = (short)Memory.real_readb(Int10.BIOSMEM_SEG,Int10.BIOSMEM_CURRENT_PAGE);
            switch(data[count]){
                case '0':
                case '1':
                case '2':
                case '3':
                case '4':
                case '5':
                case '6':
                case '7':
                case '8':
                case '9':
                    ansi.data[ansi.numberofarg]=(byte)(10*ansi.data[ansi.numberofarg]+(data[count]-'0'));
                    break;
                case ';': /* till a max of NUMBER_ANSI_DATA */
                    ansi.numberofarg++;
                    break;
                case 'm':               /* SGR */
                    for(i=0;i<=ansi.numberofarg;i++){
                        ansi.enabled=true;
                        switch(ansi.data[i]){
                        case 0: /* normal */
                            ansi.attr=0x07;//Real ansi does this as well. (should do current defaults)
                            ansi.enabled=false;
                            break;
                        case 1: /* bold mode on*/
                            ansi.attr|=0x08;
                            break;
                        case 4: /* underline */
                            Log.log(LogTypes.LOG_IOCTL,LogSeverities.LOG_NORMAL,"ANSI:no support for underline yet");
                            break;
                        case 5: /* blinking */
                            ansi.attr|=0x80;
                            break;
                        case 7: /* reverse */
                            ansi.attr=0x70;//Just like real ansi. (should do use current colors reversed)
                            break;
                        case 30: /* fg color black */
                            ansi.attr&=0xf8;
                            ansi.attr|=0x0;
                            break;
                        case 31:  /* fg color red */
                            ansi.attr&=0xf8;
                            ansi.attr|=0x4;
                            break;
                        case 32:  /* fg color green */
                            ansi.attr&=0xf8;
                            ansi.attr|=0x2;
                            break;
                        case 33: /* fg color yellow */
                            ansi.attr&=0xf8;
                            ansi.attr|=0x6;
                            break;
                        case 34: /* fg color blue */
                            ansi.attr&=0xf8;
                            ansi.attr|=0x1;
                            break;
                        case 35: /* fg color magenta */
                            ansi.attr&=0xf8;
                            ansi.attr|=0x5;
                            break;
                        case 36: /* fg color cyan */
                            ansi.attr&=0xf8;
                            ansi.attr|=0x3;
                            break;
                        case 37: /* fg color white */
                            ansi.attr&=0xf8;
                            ansi.attr|=0x7;
                            break;
                        case 40:
                            ansi.attr&=0x8f;
                            ansi.attr|=0x0;
                            break;
                        case 41:
                            ansi.attr&=0x8f;
                            ansi.attr|=0x40;
                            break;
                        case 42:
                            ansi.attr&=0x8f;
                            ansi.attr|=0x20;
                            break;
                        case 43:
                            ansi.attr&=0x8f;
                            ansi.attr|=0x60;
                            break;
                        case 44:
                            ansi.attr&=0x8f;
                            ansi.attr|=0x10;
                            break;
                        case 45:
                            ansi.attr&=0x8f;
                            ansi.attr|=0x50;
                            break;
                        case 46:
                            ansi.attr&=0x8f;
                            ansi.attr|=0x30;
                            break;
                        case 47:
                            ansi.attr&=0x8f;
                            ansi.attr|=0x70;
                            break;
                        default:
                            break;
                        }
                    }
                    ClearAnsi();
                    break;
                case 'f':
                case 'H':/* Cursor Pos*/
                    if(!ansi.warned) { //Inform the debugger that ansi is used.
                        ansi.warned = true;
                        Log.log(LogTypes.LOG_IOCTL,LogSeverities.LOG_WARN,"ANSI SEQUENCES USED");
                    }
                    /* Turn them into positions that are on the screen */
                    if(ansi.data[0] == 0) ansi.data[0] = 1;
                    if(ansi.data[1] == 0) ansi.data[1] = 1;
                    if(ansi.data[0] > ansi.nrows) ansi.data[0] = (/*Bit8u*/byte)ansi.nrows;
                    if(ansi.data[1] > ansi.ncols) ansi.data[1] = (/*Bit8u*/byte)ansi.ncols;
                    Int10_char.INT10_SetCursorPos(--(ansi.data[0]),--(ansi.data[1]),page); /*ansi=1 based, int10 is 0 based */
                    ClearAnsi();
                    break;
                    /* cursor up down and forward and backward only change the row or the col not both */
                case 'A': /* cursor up*/
                    col=Int10.CURSOR_POS_COL(page) ;
                    row=Int10.CURSOR_POS_ROW(page) ;
                    tempdata = (ansi.data[0]!=0? ansi.data[0] : 1);
                    if(tempdata > row) { row=0; }
                    else { row-=tempdata;}
                    Int10_char.INT10_SetCursorPos(row,col,page);
                    ClearAnsi();
                    break;
                case 'B': /*cursor Down */
                    col=Int10.CURSOR_POS_COL(page) ;
                    row=Int10.CURSOR_POS_ROW(page) ;
                    tempdata = (ansi.data[0]!=0? ansi.data[0] : 1);
                    if(tempdata + row >= ansi.nrows)
                        { row = (short)(ansi.nrows - 1);}
                    else	{ row += tempdata; }
                    Int10_char.INT10_SetCursorPos(row,col,page);
                    ClearAnsi();
                    break;
                case 'C': /*cursor forward */
                    col=Int10.CURSOR_POS_COL(page);
                    row=Int10.CURSOR_POS_ROW(page);
                    tempdata=(ansi.data[0]!=0? ansi.data[0] : 1);
                    if(tempdata + col >= ansi.ncols)
                        { col = (short)(ansi.ncols - 1);}
                    else	{ col += tempdata;}
                    Int10_char.INT10_SetCursorPos(row,col,page);
                    ClearAnsi();
                    break;
                case 'D': /*Cursor Backward  */
                    col=Int10.CURSOR_POS_COL(page);
                    row=Int10.CURSOR_POS_ROW(page);
                    tempdata=(ansi.data[0]!=0? ansi.data[0] : 1);
                    if(tempdata > col) {col = 0;}
                    else { col -= tempdata;}
                    Int10_char.INT10_SetCursorPos(row,col,page);
                    ClearAnsi();
                    break;
                case 'J': /*erase screen and move cursor home*/
                    if(ansi.data[0]==0) ansi.data[0]=2;
                    if(ansi.data[0]!=2) {/* every version behaves like type 2 */
                        if (Log.level<=LogSeverities.LOG_NORMAL) Log.log(LogTypes.LOG_IOCTL,LogSeverities.LOG_NORMAL,"ANSI: esc["+ansi.data[0]+"J called : not supported handling as 2");
                    }
                    Int10_char.INT10_ScrollWindow((short)0,(short)0,(short)255,(short)255,(byte)0,ansi.attr,page);
                    ClearAnsi();
                    Int10_char.INT10_SetCursorPos((short)0,(short)0,page);
                    break;
                case 'h': /* SET   MODE (if code =7 enable linewrap) */
                case 'I': /* RESET MODE */
                    Log.log(LogTypes.LOG_IOCTL,LogSeverities.LOG_NORMAL,"ANSI: set/reset mode called(not supported)");
                    ClearAnsi();
                    break;
                case 'u': /* Restore Cursor Pos */
                    Int10_char.INT10_SetCursorPos(ansi.saverow,ansi.savecol,page);
                    ClearAnsi();
                    break;
                case 's': /* SAVE CURSOR POS */
                    ansi.savecol=(byte)Int10.CURSOR_POS_COL(page);
                    ansi.saverow=(byte)Int10.CURSOR_POS_ROW(page);
                    ClearAnsi();
                    break;
                case 'K': /* erase till end of line (don't touch cursor) */
                    col = Int10.CURSOR_POS_COL(page);
                    row = Int10.CURSOR_POS_ROW(page);
                    Int10_char.INT10_WriteChar((short)' ',ansi.attr,page,ansi.ncols-col,true); //Use this one to prevent scrolling when end of screen is reached
                    //for(i = col;i<(Bitu) ansi.ncols; i++) Int10_char.INT10_TeletypeOutputAttr(' ',ansi.attr,true);
                    Int10_char.INT10_SetCursorPos(row,col,page);
                    ClearAnsi();
                    break;
                case 'M': /* delete line (NANSI) */
                    col = Int10.CURSOR_POS_COL(page);
                    row = Int10.CURSOR_POS_ROW(page);
                    Int10_char.INT10_ScrollWindow(row,(short)0,(short)(ansi.nrows-1),(short)(ansi.ncols-1),(byte)(ansi.data[0]!=0? -ansi.data[0] : -1),ansi.attr,(short)0xFF);
                    ClearAnsi();
                    break;
                case 'l':/* (if code =7) disable linewrap */
                case 'p':/* reassign keys (needs strings) */
                case 'i':/* printer stuff */
                default:
                    if (Log.level<=LogSeverities.LOG_NORMAL) Log.log(LogTypes.LOG_IOCTL,LogSeverities.LOG_NORMAL,"ANSI: unhandled char "+String.valueOf((char)data[count])+" in esc[");
                    ClearAnsi();
                    break;
                }
                count++;
            }
            size.value=count;
            return true;
        }
        public boolean Seek(/*Bit32u*/LongRef pos,/*Bit32u*/int type) {
            // seek is valid
            pos.value = 0;
            return true;
        }
        public boolean Close() {
            return true;
        }
        public /*Bit16u*/int GetInformation() {
            synchronized (Bios_keyboard.lock) {
                /*Bit16u*/int head=Memory.mem_readw(Bios.BIOS_KEYBOARD_BUFFER_HEAD);
                /*Bit16u*/int tail=Memory.mem_readw(Bios.BIOS_KEYBOARD_BUFFER_TAIL);

                if ((head==tail) && readcache==0) return 0x80D3;	/* No Key Available */
                if (readcache!=0 || Memory.real_readw(0x40,head)!=0) return 0x8093;		/* Key Available */

                /* remove the zero from keyboard buffer */
                /*Bit16u*/int start=Memory.mem_readw(Bios.BIOS_KEYBOARD_BUFFER_START);
                /*Bit16u*/int end	=Memory.mem_readw(Bios.BIOS_KEYBOARD_BUFFER_END);
                head+=2;
                if (head>=end) head=start;
                Memory.mem_writew(Bios.BIOS_KEYBOARD_BUFFER_HEAD,head);
            }
            return 0x80D3; /* No Key Available */
        }
        public boolean ReadFromControlChannel(/*PhysPt*/int bufptr,/*Bit16u*/int size,/*Bit16u*/IntRef retcode) {
            return false;
        }
        public boolean WriteToControlChannel(/*PhysPt*/int bufptr,/*Bit16u*/int size,/*Bit16u*/IntRef retcode) {
            return false;
        }
        public void ClearAnsi() {
            for(/*Bit8u*/short i=0; i<NUMBER_ANSI_DATA;i++) ansi.data[i]=0;
            ansi.esc=false;
            ansi.sci=false;
            ansi.numberofarg=0;
        }

        private /*Bit8u*/byte readcache;
        private /*Bit8u*/byte lastwrite;
        private static class Ansi { /* should create a constructor, which would fill them with the appropriate values */
            boolean esc;
            boolean sci;
            boolean enabled;
            /*Bit8u*/short attr;
            /*Bit8u*/byte[] data=new byte[NUMBER_ANSI_DATA];
            /*Bit8u*/short numberofarg;
            /*Bit16u*/int nrows;
            /*Bit16u*/int ncols;
            /*Bit8s*/byte savecol;
            /*Bit8s*/byte saverow;
            boolean warned;
        }
        private Ansi ansi = new Ansi();
    }
}
