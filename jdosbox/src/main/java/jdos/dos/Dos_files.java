package jdos.dos;

import jdos.cpu.CPU_Regs;
import jdos.dos.drives.Drive_virtual;
import jdos.hardware.Memory;
import jdos.ints.Bios;
import jdos.misc.Log;
import jdos.types.LogSeverities;
import jdos.types.LogTypes;
import jdos.util.*;

import java.util.Random;

public class Dos_files {
    public static final int DOS_FILES =127;
    public static final int DOS_DRIVES =26;
    
    public static final int DOS_FILESTART =4;

    public static final int FCB_SUCCESS     =0;
    public static final int FCB_READ_NODATA	=1;
    public static final int FCB_READ_PARTIAL =3;
    public static final int FCB_ERR_NODATA  =1;
    public static final int FCB_ERR_EOF     =3;
    public static final int FCB_ERR_WRITE   =1;

    public static final int OPEN_READ=0;
    public static final int OPEN_WRITE=1;
    public static final int OPEN_READWRITE=2;
    public static final int OPEN_READ_NO_MOD=4;
    public static final int DOS_NOT_INHERIT=128;

    public static final int DOS_SEEK_SET=0;
    public static final int DOS_SEEK_CUR=1;
    public static final int DOS_SEEK_END=2;

    public static final int STDIN=0;
    public static final int STDOUT=1;
    public static final int STDERR=2;
    public static final int STDAUX=3;
    public static final int STDPRN=4;

    public static DOS_File[] Files=new DOS_File[DOS_FILES];
    public static Dos_Drive[] Drives=new Dos_Drive[DOS_DRIVES];

    static public /*Bit8u*/short DOS_GetDefaultDrive() {
    //	return DOS_SDA(DOS_SDA_SEG,DOS_SDA_OFS).GetDrive();
        /*Bit8u*/short d = new Dos_SDA(Dos.DOS_SDA_SEG,Dos.DOS_SDA_OFS).GetDrive();
        if( d != Dos.dos.current_drive ) if (Log.level<=LogSeverities.LOG_ERROR) Log.log(LogTypes.LOG_DOSMISC, LogSeverities.LOG_ERROR,"SDA drive "+d+" not the same as dos.current_drive "+Dos.dos.current_drive);
        return Dos.dos.current_drive;
    }

    static public void DOS_SetDefaultDrive(/*Bit8u*/short drive) {
    //	if (drive<=DOS_DRIVES && ((drive<2) || Drives[drive])) DOS_SDA(Dos.DOS_SDA_SEG,Dos.DOS_SDA_OFS).SetDrive(drive);
        if (drive<DOS_DRIVES && ((drive<2) || Drives[drive]!=null)) {Dos.dos.current_drive = (byte)drive; new Dos_SDA(Dos.DOS_SDA_SEG,Dos.DOS_SDA_OFS).SetDrive(drive);}
    }

    public static boolean DOS_MakeName(String name, StringRef fullname,/*Bit8u*/ShortRef drive) {
        if(name == null || name.length() == 0 || name.startsWith(" ")) {
            /* Both \0 and space are seperators and
             * empty filenames report file not found */
            Dos.DOS_SetError(Dos.DOSERR_FILE_NOT_FOUND);
            return false;
        }
        String name_int = name;
        byte[] tempdir=new byte[Dos_system.DOS_PATHLENGTH];
        byte[] upname=new byte[Dos_system.DOS_PATHLENGTH];
        /*Bitu*/int r,w;
        drive.value = DOS_GetDefaultDrive();
        /* First get the drive */
        if (name.length()>1 && name_int.charAt(1)==':') {
            drive.value=(short)(((int)name_int.charAt(0) | 0x20)-'a');
            name_int = name_int.substring(2);
        }
        if (drive.value>=DOS_DRIVES || drive.value<0 || Drives[drive.value]==null) {
            Dos.DOS_SetError(Dos.DOSERR_PATH_NOT_FOUND);
            return false;
        }
        r=0;w=0;
        while (r<name_int.length() && (r<Dos_system.DOS_PATHLENGTH)) {
            /*Bit8u*/byte c=(byte)name_int.charAt(r++);
            if ((c>='a') && (c<='z')) {upname[w++]=(byte)(c-32);continue;}
            if ((c>='A') && (c<='Z')) {upname[w++]=c;continue;}
            if ((c>='0') && (c<='9')) {upname[w++]=c;continue;}
            switch (c & 0xFF) {
            case '/':
                upname[w++]='\\';
                break;
            case ' ': /* should be seperator */
                break;
            case '\\':	case '$':	case '#':	case '@':	case '(':	case ')':
            case '!':	case '%':	case '{':	case '}':	case '`':	case '~':
            case '_':	case '-':	case '.':	case '*':	case '?':	case '&':
            case '\'':	case '+':	case '^':	case 246:	case 255:	case 0xa0:
            case 0xe5:	case 0xbd: case 0x9d:
                upname[w++]=c;
                break;
            default:
                if (Log.level<=LogSeverities.LOG_NORMAL) Log.log(LogTypes.LOG_FILES,LogSeverities.LOG_NORMAL,"Makename encountered an illegal char "+String.valueOf((char)c)+" hex:"+Integer.toString(c, 16)+" in "+name+"!");
                Dos.DOS_SetError(Dos.DOSERR_PATH_NOT_FOUND);return false;
                //break;
            }
        }
        if (r>=Dos_system.DOS_PATHLENGTH) { Dos.DOS_SetError(Dos.DOSERR_PATH_NOT_FOUND);return false; }
        upname[w]=0;
        /* Now parse the new file name to make the final filename */
        if (upname[0]!='\\') fullname.value = Drives[drive.value].curdir;
        else fullname.value = "";
        /*Bit32u*/int lastdir=0;/*Bit32u*/int t=0;
        while (t<fullname.value.length()) {
            if ((fullname.value.charAt(t)=='\\') && t+1<fullname.value.length()) lastdir=t;
            t++;
        }
        r=0;w=0;
        tempdir[0]=0;
        boolean stop=false;
        while (!stop) {
            if (upname[r]==0) stop=true;
            if ((upname[r]=='\\') || (upname[r]==0)){
                tempdir[w]=0;
                if (tempdir[0]==0) { w=0;r++;continue;}
                if (tempdir[0]=='.' && tempdir[1]==0) {
                    tempdir[0]=0;
                    w=0;r++;
                    continue;
                }

                /*Bit32s*/int iDown;
                boolean dots = true;
                /*Bit32s*/int templen=(/*Bit32s*/int)StringHelper.strlen(tempdir);
                for(iDown=0;(iDown < templen) && dots;iDown++)
                    if(tempdir[iDown] != '.')
                        dots = false;

                // only dots?
                if (dots && (templen > 1)) {
                    /*Bit32s*/int cDots = templen - 1;
                    for(iDown=(/*Bit32s*/int)fullname.value.length()-1;iDown>=0;iDown--) {
                        if(fullname.value.charAt(iDown)=='\\' || iDown==0) {
                            lastdir = iDown;
                            cDots--;
                            if(cDots==0)
                                break;
                        }
                    }
                    fullname.value = fullname.value.substring(0,lastdir);
                    t=0;lastdir=0;
                    while (t<fullname.value.length()) {
                        if ((fullname.value.charAt(t)=='\\') && t+1<fullname.value.length()) lastdir=t;
                        t++;
                    }
                    tempdir[0]=0;
                    w=0;r++;
                    continue;
                }


                lastdir=fullname.value.length();

                if (lastdir!=0) fullname.value+="\\";
                String stempdir = new String(tempdir, 0, StringHelper.strlen(tempdir));
                int pos = stempdir.indexOf('.');
                String ext = null;
                if (pos >= 0) {
                    ext = stempdir.substring(pos);
                }
                if (ext!=null) {
                    if(ext.indexOf('.', 1)>=0) {
                    //another dot in the extension =>file not found
                    //Or path not found depending on wether
                    //we are still in dir check stage or file stage
                        if(stop)
                            Dos.DOS_SetError(Dos.DOSERR_FILE_NOT_FOUND);
                        else
                            Dos.DOS_SetError(Dos.DOSERR_PATH_NOT_FOUND);
                        return false;
                    }

                    ext = ext.substring(0, Math.min(4, ext.length()));
                    if((StringHelper.strlen(tempdir) - ext.length()) > 8) System.arraycopy(ext.getBytes(), 0, tempdir, 8, 4);

                } else tempdir[8]=0;

                if (fullname.value.length()+StringHelper.strlen(tempdir)>=Dos_system.DOS_PATHLENGTH) {
                    Dos.DOS_SetError(Dos.DOSERR_PATH_NOT_FOUND);return false;
                }

                fullname.value+=new String(tempdir, 0, StringHelper.strlen(tempdir));
                tempdir[0]=0;
                w=0;r++;
                continue;
            }
            tempdir[w++]=upname[r++];
        }
        return true;
    }

    public static boolean DOS_GetCurrentDir(/*Bit8u*/short drive,StringRef buffer) {
        if (drive==0) drive=DOS_GetDefaultDrive();
        else drive--;
        if ((drive>=DOS_DRIVES) || (Drives[drive])==null) {
            Dos.DOS_SetError(Dos.DOSERR_INVALID_DRIVE);
            return false;
        }
        buffer.value = Drives[drive].curdir;
        return true;
    }

    public static boolean DOS_ChangeDir(String dir) {
        /*Bit8u*/ShortRef drive=new ShortRef();StringRef fulldir = new StringRef();
        String testdir=dir;
	    if (testdir.length()>1 && testdir.charAt(1)==':') testdir = testdir.substring(2);
        if (testdir.length()==0) {
            Dos.DOS_SetError(Dos.DOSERR_PATH_NOT_FOUND);
            return false;
        }
        if (!DOS_MakeName(dir,fulldir,drive)) return false;
        if (fulldir.value.length()>0 && (testdir.length()>1 && testdir.charAt(testdir.length()-1)=='\\')) {
	    	Dos.DOS_SetError(Dos.DOSERR_PATH_NOT_FOUND);
		    return false;
	    }
        if (Drives[drive.value].TestDir(fulldir.value)) {
            Drives[drive.value].curdir =  fulldir.value;
            return true;
        } else {
            Dos.DOS_SetError(Dos.DOSERR_PATH_NOT_FOUND);
        }
        return false;
    }

    public static boolean DOS_MakeDir(String dir) {
        /*Bit8u*/ShortRef drive=new ShortRef();StringRef fulldir = new StringRef();
        if(dir==null || dir.length()==0 || dir.endsWith("\\")) {
            Dos.DOS_SetError(Dos.DOSERR_PATH_NOT_FOUND);
            return false;
        }
        if (!DOS_MakeName(dir,fulldir,drive)) return false;
        if(Drives[drive.value].MakeDir(fulldir.value)) return true;

        /* Determine reason for failing */
        if(Drives[drive.value].TestDir(fulldir.value))
            Dos.DOS_SetError(Dos.DOSERR_ACCESS_DENIED);
        else
            Dos.DOS_SetError(Dos.DOSERR_PATH_NOT_FOUND);
        return false;
    }

    public static boolean DOS_RemoveDir(String dir) {
    /* We need to do the test before the removal as can not rely on
     * the host to forbid removal of the current directory.
     * We never change directory. Everything happens in the drives.
     */
        /*Bit8u*/ShortRef drive=new ShortRef();StringRef fulldir = new StringRef();
        if (!DOS_MakeName(dir,fulldir,drive)) return false;
        /* Check if exists */
        if(!Drives[drive.value].TestDir(fulldir.value)) {
            Dos.DOS_SetError(Dos.DOSERR_PATH_NOT_FOUND);
            return false;
        }
        /* See if it's current directory */
        StringRef currdir = new StringRef();
        DOS_GetCurrentDir((short)(drive.value + 1) ,currdir);
        if(currdir.equals(fulldir.value)) {
            Dos.DOS_SetError(Dos.DOSERR_REMOVE_CURRENT_DIRECTORY);
            return false;
        }

        if(Drives[drive.value].RemoveDir(fulldir.value)) return true;

        /* Failed. We know it exists and it's not the current dir */
        /* Assume non empty */
        Dos.DOS_SetError(Dos.DOSERR_ACCESS_DENIED);
        return false;
    }

    static public boolean DOS_Rename(String oldname,String newname) {
        /*Bit8u*/ShortRef driveold = new ShortRef();StringRef fullold = new StringRef();
        /*Bit8u*/ShortRef drivenew = new ShortRef();StringRef fullnew = new StringRef();
        if (!DOS_MakeName(oldname,fullold,driveold)) return false;
        if (!DOS_MakeName(newname,fullnew,drivenew)) return false;
        /* No tricks with devices */
        if ( (Dos_devices.DOS_FindDevice(oldname) != Dos_devices.DOS_DEVICES) ||
             (Dos_devices.DOS_FindDevice(newname) != Dos_devices.DOS_DEVICES) ) {
            Dos.DOS_SetError(Dos.DOSERR_FILE_NOT_FOUND);
            return false;
        }
        /* Must be on the same drive */
        if(driveold.value != drivenew.value) {
            Dos.DOS_SetError(Dos.DOSERR_NOT_SAME_DEVICE);
            return false;
        }
        /*Test if target exists => no access */
        /*Bit16u*/IntRef attr = new IntRef(0);
        if(Drives[drivenew.value].GetFileAttr(fullnew.value,attr)) {
            Dos.DOS_SetError(Dos.DOSERR_ACCESS_DENIED);
            return false;
        }
        /* Source must exist, check for path ? */
        if (!Drives[driveold.value].GetFileAttr( fullold.value, attr ) ) {
            Dos.DOS_SetError(Dos.DOSERR_FILE_NOT_FOUND);
            return false;
        }

        if (Drives[drivenew.value].Rename(fullold.value,fullnew.value)) return true;
        /* If it still fails, which error should we give ? PATH NOT FOUND or EACCESS */
        if (Log.level<=LogSeverities.LOG_NORMAL) Log.log(LogTypes.LOG_FILES,LogSeverities.LOG_NORMAL,"Rename fails for "+oldname+" to "+newname+", no proper errorcode returned.");
        Dos.DOS_SetError(Dos.DOSERR_FILE_NOT_FOUND);
        return false;
    }

    static public boolean DOS_FindFirst(String search,/*Bit16u*/int attr) {
        return DOS_FindFirst(search, attr, false);
    }
    static public boolean DOS_FindFirst(String search,/*Bit16u*/int attr,boolean fcb_findfirst/*=false*/) {
        Dos_DTA dta=new Dos_DTA(Dos.dos.dta());
        /*Bit8u*/ShortRef drive=new ShortRef();StringRef fullsearch = new StringRef();
        String dir;String pattern;
        if(search.length()>0 && search.endsWith("\\") && !( (search.length() > 2) && (search.charAt(search.length() - 2) == ':') && (attr == Dos_system.DOS_ATTR_VOLUME) )) {
            //Dark Forces installer, but c:\ is allright for volume labels(exclusively set)
            Dos.DOS_SetError(Dos.DOSERR_NO_MORE_FILES);
            return false;
        }
        if (!DOS_MakeName(search,fullsearch,drive)) return false;
        //Check for devices. FindDevice checks for leading subdir as well
        boolean device = (Dos_devices.DOS_FindDevice(search) != Dos_devices.DOS_DEVICES);

        /* Split the search in dir and pattern */
        int pos = fullsearch.value.lastIndexOf('\\');
        if (pos<0) {	/*No dir */
            pattern = fullsearch.value;
            dir = "";
        } else {
            dir = fullsearch.value.substring(0, pos);
            pattern = fullsearch.value.substring(pos+1);
        }

        dta.SetupSearch(drive.value,(/*Bit8u*/short)attr,pattern);

        if(device) {
            pos = pattern.indexOf('.');
            if (pos>=0) pattern = pattern.substring(0, pos);
            //TODO use current date and time
            dta.SetResult(pattern,0,0,0,(short)Dos_system.DOS_ATTR_DEVICE);
            if (Log.level<=LogSeverities.LOG_WARN) Log.log(LogTypes.LOG_DOSMISC,LogSeverities.LOG_WARN,"finding device "+pattern);
            return true;
        }

        if (Drives[drive.value].FindFirst(dir,dta,fcb_findfirst)) return true;

        return false;
    }

    static public boolean DOS_FindNext() {
        Dos_DTA dta=new Dos_DTA(Dos.dos.dta());
        /*Bit8u*/short i = dta.GetSearchDrive();
        if(i >= DOS_DRIVES || Drives[i]==null) {
            /* Corrupt search. */
            Log.log(LogTypes.LOG_FILES,LogSeverities.LOG_ERROR,"Corrupt search!!!!");
            Dos.DOS_SetError(Dos.DOSERR_NO_MORE_FILES);
            return false;
        }
        if (Drives[i].FindNext(dta)) return true;
        return false;
    }


    public static boolean DOS_ReadFile(/*Bit16u*/int entry,/*Bit8u*/byte[] data,/*Bit16u*/IntRef amount) {
        /*Bit32u*/int handle=Dos.RealHandle(entry);
        if (handle>=DOS_FILES) {
            Dos.DOS_SetError(Dos.DOSERR_INVALID_HANDLE);
            return false;
        }
        if (Files[handle]==null || !Files[handle].IsOpen()) {
            Dos.DOS_SetError(Dos.DOSERR_INVALID_HANDLE);
            return false;
        }
    /*
        if ((Files[handle].flags & 0x0f) == OPEN_WRITE)) {
            Dos.DOS_SetError(DOSERR_INVALID_HANDLE);
            return false;
        }
    */
        boolean ret=Files[handle].Read(data,amount);
        return ret;
    }

    public static boolean DOS_WriteFile(/*Bit16u*/int entry,/*Bit8u*/byte[] data,/*Bit16u*/IntRef amount) {
        /*Bit32u*/int handle=Dos.RealHandle(entry);
        if (handle>=DOS_FILES) {
            Dos.DOS_SetError(Dos.DOSERR_INVALID_HANDLE);
            return false;
        }
        if (Files[handle]==null || !Files[handle].IsOpen()) {
            Dos.DOS_SetError(Dos.DOSERR_INVALID_HANDLE);
            return false;
        }
    /*
        if ((Files[handle].flags & 0x0f) == OPEN_READ)) {
            Dos.DOS_SetError(DOSERR_INVALID_HANDLE);
            return false;
        }
    */
        boolean ret=Files[handle].Write(data,amount);
        return ret;
    }

    static public boolean DOS_SeekFile(/*Bit16u*/int entry,/*Bit32u*/LongRef pos,/*Bit32u*/int type) {
        /*Bit32u*/int handle=Dos.RealHandle(entry);
        if (handle>=DOS_FILES) {
            Dos.DOS_SetError(Dos.DOSERR_INVALID_HANDLE);
            return false;
        }
        if (Files[handle]==null || !Files[handle].IsOpen()) {
            Dos.DOS_SetError(Dos.DOSERR_INVALID_HANDLE);
            return false;
        }
        return Files[handle].Seek(pos,type);
    }

    static public boolean DOS_CloseFile(/*Bit16u*/int entry) {
        /*Bit32u*/int handle=Dos.RealHandle(entry);
        if (handle>=DOS_FILES) {
            Dos.DOS_SetError(Dos.DOSERR_INVALID_HANDLE);
            return false;
        }
        if (Files[handle]==null || !Files[handle].IsOpen()) {
            Dos.DOS_SetError(Dos.DOSERR_INVALID_HANDLE);
            return false;
        }
        if (Files[handle].IsOpen()) {
            Files[handle].Close();
        }
        Dos_PSP psp=new Dos_PSP(Dos.dos.psp());
        psp.SetFileHandle(entry,0xff);
        if (Files[handle].RemoveRef()<=0) {
            Files[handle]=null;
        }
        return true;
    }

    static public boolean DOS_FlushFile(/*Bit16u*/int entry) {
        /*Bit32u*/int handle=Dos.RealHandle(entry);
        if (handle>=DOS_FILES) {
            Dos.DOS_SetError(Dos.DOSERR_INVALID_HANDLE);
            return false;
        }
        if (Files[handle]==null || !Files[handle].IsOpen()) {
            Dos.DOS_SetError(Dos.DOSERR_INVALID_HANDLE);
            return false;
        }
        Log.log(LogTypes.LOG_DOSMISC,LogSeverities.LOG_NORMAL,"FFlush used.");
        return true;
    }

    static private boolean PathExists(String name) {
        if(name.lastIndexOf('\\')<0) return true;
        if (name.lastIndexOf('\\')==0) return true;
        name = name.substring(0, name.lastIndexOf('\\'));
        /*Bit8u*/ShortRef drive=new ShortRef();StringRef fulldir=new StringRef();
        if (!DOS_MakeName(name,fulldir,drive)) return false;
        if(!Drives[drive.value].TestDir(fulldir.value)) return false;
        return true;
    }


    static public boolean DOS_CreateFile(String name,/*Bit16u*/int attributes,/*Bit16u*/IntRef entry) {
        // Creation of a device is the same as opening it
        // Tc201 installer
        if (Dos_devices.DOS_FindDevice(name) != Dos_devices.DOS_DEVICES)
            return DOS_OpenFile(name, OPEN_READ, entry);

        if (Log.level<=LogSeverities.LOG_NORMAL) Log.log(LogTypes.LOG_FILES,LogSeverities.LOG_NORMAL,"file create attributes "+Integer.toString(attributes, 16)+" file "+name);
        StringRef fullname = new StringRef();/*Bit8u*/ShortRef drive=new ShortRef();
        Dos_PSP psp = new Dos_PSP(Dos.dos.psp());
        if (!DOS_MakeName(name,fullname,drive)) return false;
        /* Check for a free file handle */
        /*Bit8u*/short handle=DOS_FILES;/*Bit8u*/short i;
        for (i=0;i<DOS_FILES;i++) {
            if (Files[i]==null) {
                handle=i;
                break;
            }
        }
        if (handle==DOS_FILES) {
            Dos.DOS_SetError(Dos.DOSERR_TOO_MANY_OPEN_FILES);
            return false;
        }
        /* We have a position in the main table now find one in the psp table */
        entry.value = psp.FindFreeFileEntry();
        if (entry.value==0xff) {
            Dos.DOS_SetError(Dos.DOSERR_TOO_MANY_OPEN_FILES);
            return false;
        }
        /* Don't allow directories to be created */
        if ((attributes & Dos_system.DOS_ATTR_DIRECTORY)!=0) {
            Dos.DOS_SetError(Dos.DOSERR_ACCESS_DENIED);
            return false;
        }
        Files[handle]=Drives[drive.value].FileCreate(fullname.value,attributes);
        boolean foundit=Files[handle]!=null;
        if (foundit) {
            Files[handle].SetDrive(drive.value);
            Files[handle].AddRef();
            psp.SetFileHandle(entry.value,handle);
            return true;
        } else {
            if(!PathExists(name)) Dos.DOS_SetError(Dos.DOSERR_PATH_NOT_FOUND);
            else Dos.DOS_SetError(Dos.DOSERR_FILE_NOT_FOUND);
            return false;
        }
    }

    public static boolean DOS_OpenFile(String name,/*Bit8u*/int flags,/*Bit16u*/IntRef entry) {
        /* First check for devices */
        if (flags>2) if (Log.level<=LogSeverities.LOG_ERROR) Log.log(LogTypes.LOG_FILES,LogSeverities.LOG_ERROR,"Special file open command "+Integer.toString(flags, 16)+" file "+name);
        else if (Log.level<=LogSeverities.LOG_NORMAL) Log.log(LogTypes.LOG_FILES,LogSeverities.LOG_NORMAL,"file open command "+Integer.toString(flags, 16)+" file "+name);

        Dos_PSP psp = new Dos_PSP(Dos.dos.psp());
        /*Bit16u*/IntRef attr = new IntRef(0);
        /*Bit8u*/short devnum = Dos_devices.DOS_FindDevice(name);
        boolean device = (devnum != Dos_devices.DOS_DEVICES);
        if(!device && DOS_GetFileAttr(name,attr)) {
        //DON'T ALLOW directories to be openened.(skip test if file is device).
            if((attr.value & Dos_system.DOS_ATTR_DIRECTORY)!=0 || (attr.value & Dos_system.DOS_ATTR_VOLUME)!=0){
                Dos.DOS_SetError(Dos.DOSERR_ACCESS_DENIED);
                return false;
            }
        }

        StringRef fullname=new StringRef();/*Bit8u*/ShortRef drive=new ShortRef();/*Bit8u*/short i;
        /* First check if the name is correct */
        if (!DOS_MakeName(name,fullname,drive)) return false;
        /*Bit8u*/short handle=255;
        /* Check for a free file handle */
        for (i=0;i<DOS_FILES;i++) {
            if (Files[i]==null) {
                handle=i;
                break;
            }
        }
        if (handle==255) {
            Dos.DOS_SetError(Dos.DOSERR_TOO_MANY_OPEN_FILES);
            return false;
        }
        /* We have a position in the main table now find one in the psp table */
        entry.value = psp.FindFreeFileEntry();

        if (entry.value==0xff) {
            Dos.DOS_SetError(Dos.DOSERR_TOO_MANY_OPEN_FILES);
            return false;
        }
        boolean exists=false;
        if (device) {
            Files[handle]=new DOS_Device(Dos_devices.Devices[devnum]);
        } else {
            Files[handle]=Drives[drive.value].FileOpen(fullname.value,flags);
            exists=Files[handle]!=null;
            if (exists) Files[handle].SetDrive(drive.value);
        }
        if (exists || device ) {
            Files[handle].AddRef();
            psp.SetFileHandle(entry.value,handle);
            return true;
        } else {
            //Test if file exists, but opened in read-write mode (and writeprotected)
            if(((flags&3) != OPEN_READ) && Drives[drive.value].FileExists(fullname.value))
                Dos.DOS_SetError(Dos.DOSERR_ACCESS_DENIED);
            else {
                if(!PathExists(name)) Dos.DOS_SetError(Dos.DOSERR_PATH_NOT_FOUND);
                else Dos.DOS_SetError(Dos.DOSERR_FILE_NOT_FOUND);
            }
            return false;
        }
    }

    static public boolean DOS_OpenFileExtended(String name, /*Bit16u*/int flags, /*Bit16u*/int createAttr, /*Bit16u*/int action, /*Bit16u*/IntRef entry, /*Bit16u*/IntRef status) {
    // FIXME: Not yet supported : Bit 13 of flags (int 0x24 on critical error)
        /*Bit16u*/int result = 0;
        if (action==0) {
            // always fail setting
            Dos.DOS_SetError(Dos.DOSERR_FUNCTION_NUMBER_INVALID);
            return false;
        } else {
            if (((action & 0x0f)>2) || ((action & 0xf0)>0x10)) {
                // invalid action parameter
                Dos.DOS_SetError(Dos.DOSERR_FUNCTION_NUMBER_INVALID);
                return false;
            }
        }
        if (DOS_OpenFile(name, (/*Bit8u*/short)(flags&0xff), entry)) {
            // File already exists
            switch (action & 0x0f) {
                case 0x00:		// failed
                    Dos.DOS_SetError(Dos.DOSERR_FILE_ALREADY_EXISTS);
                    return false;
                case 0x01:		// file open (already done)
                    result = 1;
                    break;
                case 0x02:		// replace
                    DOS_CloseFile(entry.value);
                    if (!DOS_CreateFile(name, createAttr, entry)) return false;
                    result = 3;
                    break;
                default:
                    Dos.DOS_SetError(Dos.DOSERR_FUNCTION_NUMBER_INVALID);
                    Log.exit("DOS: OpenFileExtended: Unknown action.");
                    break;
            }
        } else {
            // File doesn't exist
            if ((action & 0xf0)==0) {
                // uses error code from failed open
                return false;
            }
            // Create File
            if (!DOS_CreateFile(name, createAttr, entry)) {
                // uses error code from failed create
                return false;
            }
            result = 2;
        }
        // success
        status.value = result;
        return true;
    }

    static public boolean DOS_UnlinkFile(String name) {
        StringRef fullname=new StringRef();/*Bit8u*/ShortRef drive=new ShortRef();
        if (!DOS_MakeName(name,fullname,drive)) return false;
        if(Drives[drive.value].FileUnlink(fullname.value)){
            return true;
        } else {
            Dos.DOS_SetError(Dos.DOSERR_FILE_NOT_FOUND);
            return false;
        }
    }

    public static boolean DOS_GetFileAttr(String name,/*Bit16u*/IntRef attr) {
        StringRef fullname=new StringRef();/*Bit8u*/ShortRef drive=new ShortRef();
        if (!DOS_MakeName(name,fullname,drive)) return false;
        if (Drives[drive.value].GetFileAttr(fullname.value,attr)) {
            return true;
        } else {
            Dos.DOS_SetError(Dos.DOSERR_FILE_NOT_FOUND);
            return false;
        }
    }

    public static boolean DOS_SetFileAttr(String name,/*Bit16u*/int attr)
    // this function does not change the file attributs
    // it just does some tests if file is available
    // returns false when using on cdrom (stonekeep)
    {
        /*Bit16u*/IntRef attrTemp = new IntRef(0);
        StringRef fullname=new StringRef();/*Bit8u*/ShortRef drive=new ShortRef();
        if (!DOS_MakeName(name,fullname,drive)) return false;
        if (Drives[drive.value].GetInfo().startsWith("CDRom ") || Drives[drive.value].GetInfo().startsWith("isoDrive ")) {
            Dos.DOS_SetError(Dos.DOSERR_ACCESS_DENIED);
            return false;
        }
        return Drives[drive.value].GetFileAttr(fullname.value,attrTemp);
    }

    static public boolean DOS_Canonicalize(String name,StringRef big) {
    //TODO Add Better support for devices and shit but will it be needed i doubt it :)
        StringRef fullname=new StringRef();/*Bit8u*/ShortRef drive=new ShortRef();
        if (!DOS_MakeName(name,fullname,drive)) return false;
        big.value = String.valueOf((char)(drive.value+'A'));
        big.value+=':';
        big.value+='\\';
        big.value+=fullname.value;
        return true;
    }

    static public boolean DOS_GetFreeDiskSpace(/*Bit8u*/short drive,/*Bit16u*/IntRef bytes,/*Bit8u*/ShortRef sectors,/*Bit16u*/IntRef clusters,/*Bit16u*/IntRef free) {
        if (drive==0) drive=DOS_GetDefaultDrive();
        else drive--;
        if ((drive>=DOS_DRIVES) || Drives[drive]==null) {
            Dos.DOS_SetError(Dos.DOSERR_INVALID_DRIVE);
            return false;
        }
        return Drives[drive].AllocationInfo(bytes,sectors,clusters,free);
    }

    static public boolean DOS_DuplicateEntry(/*Bit16u*/int entry,/*Bit16u*/IntRef newentry) {
        // Dont duplicate console handles
    /*	if (entry<=STDPRN) {
            *newentry = entry;
            return true;
        };
    */
        /*Bit8u*/int handle=Dos.RealHandle(entry);
        if (handle>=DOS_FILES) {
            Dos.DOS_SetError(Dos.DOSERR_INVALID_HANDLE);
            return false;
        };
        if (Files[handle]==null || !Files[handle].IsOpen()) {
            Dos.DOS_SetError(Dos.DOSERR_INVALID_HANDLE);
            return false;
        };
        Dos_PSP psp = new Dos_PSP(Dos.dos.psp());
        newentry.value = psp.FindFreeFileEntry();
        if (newentry.value==0xff) {
            Dos.DOS_SetError(Dos.DOSERR_TOO_MANY_OPEN_FILES);
            return false;
        }
        Files[handle].AddRef();
        psp.SetFileHandle(newentry.value,handle);
        return true;
    }

    static public boolean DOS_ForceDuplicateEntry(/*Bit16u*/int entry,/*Bit16u*/int newentry) {
        if(entry == newentry) {
            Dos.DOS_SetError(Dos.DOSERR_INVALID_HANDLE);
            return false;
        }
        /*Bit8u*/int orig = Dos.RealHandle(entry);
        if (orig >= DOS_FILES) {
            Dos.DOS_SetError(Dos.DOSERR_INVALID_HANDLE);
            return false;
        }
        if (Files[orig]==null || !Files[orig].IsOpen()) {
            Dos.DOS_SetError(Dos.DOSERR_INVALID_HANDLE);
            return false;
        }
        /*Bit8u*/int newone = Dos.RealHandle(newentry);
        if (newone < DOS_FILES && Files[newone]!=null) {
            DOS_CloseFile(newentry);
        }
        Dos_PSP psp = new Dos_PSP(Dos.dos.psp());
        Files[orig].AddRef();
        psp.SetFileHandle(newentry,orig);
        return true;
    }


    static public boolean DOS_CreateTempFile(StringRef name,/*Bit16u*/IntRef entry) {
        if (name.value.length()==0) {
            // temp file created in root directory
            name.value="\\";
        } else {
            if (!name.value.endsWith("\\") && !name.value.endsWith("/"))
                name.value+="\\";
        }
        Dos.dos.errorcode=0;
        /* add random crap to the end of the name and try to open */
        Random r = new Random();
        StringBuffer tempname;

        do {
            /*Bit32u*/int i;
            tempname = new StringBuffer();
            for (i=0;i<8;i++) {
                tempname.append((char)(Math.abs(r.nextInt()%26)+'A'));
            }
        } while ((!DOS_CreateFile(name.value+tempname.toString(),0,entry)) && (Dos.dos.errorcode==Dos.DOSERR_FILE_ALREADY_EXISTS));
        name.value+=tempname;
        if (Dos.dos.errorcode!=0) return false;
        return true;
    }

    static private final String FCB_SEP = ":.;,=+";
    static private final String ILLEGAL = ":.;,=+ \t/\"[]<>|";

    static private boolean isvalid(char in){
        return (in>0x1F) && ILLEGAL.indexOf(in)<0;
    }

    static private final int PARSE_SEP_STOP          = 0x01;
    static private final int PARSE_DFLT_DRIVE        = 0x02;
    static private final int PARSE_BLNK_FNAME        = 0x04;
    static private final int PARSE_BLNK_FEXT         = 0x08;

    static private final int PARSE_RET_NOWILD        = 0;
    static private final int PARSE_RET_WILD          = 1;
    static private final int PARSE_RET_BADDRIVE      = 0xff;

    public static /*Bit8u*/short FCB_Parsename(/*Bit16u*/int seg,/*Bit16u*/int offset,/*Bit8u*/short parser ,String string, /*Bit8u*/ShortRef change) {
        int begin_len = string.length();
        /*Bit8u*/short ret=0;
        if ((parser & PARSE_DFLT_DRIVE)==0) {
            // default drive forced, this intentionally invalidates an extended FCB
            Memory.mem_writeb(Memory.PhysMake(seg,offset),0);
        }
        Dos_FCB fcb = new Dos_FCB(seg,offset,false);	// always a non-extended FCB
        boolean hasdrive,hasname,hasext,finished;
        hasdrive=hasname=hasext=finished=false;
        /*Bitu*/int index=0;
        /*Bit8u*/short fill=' ';
        StringRef fcb_name = new StringRef();
        /* Get the old information from the previous fcb */
        fcb.GetName(fcb_name);
        char drive = (char)(fcb_name.value.charAt(0)-'A'+1);
        byte[] name = new byte[8];

        byte[] b = fcb_name.value.substring(2, fcb_name.value.lastIndexOf('.')).getBytes();
        System.arraycopy(b, 0, name, 0, Math.min(b.length, 8));
        byte[] ext = new byte[3];
        b = fcb_name.value.substring(fcb_name.value.lastIndexOf('.')+1).getBytes();
        System.arraycopy(b, 0, ext, 0, Math.min(b.length, ext.length));
        /* Strip of the leading sepetaror */
        if((parser & PARSE_SEP_STOP)!=0 && string.length()>0)  {       //ignore leading seperator
            if (FCB_SEP.indexOf(string.charAt(0))>=0) string = string.substring(1);
        }
        /* strip leading spaces */
        while(string.length()>0 && ((string.charAt(0)==' ')||(string.charAt(0)=='\t'))) string=string.substring(1);
        /* Check for a drive */
        if (string.length()>1 && string.charAt(1)==':') {
            drive = 0;
            hasdrive=true;
            char d = string.substring(0,1).toUpperCase().charAt(0);
            if (d>='A' && d<='Z' && Drives[(int)(d-'A')]!=null) {
                drive=(char)(d-'A'+1);
            } else ret=0xff;
            string = string.substring(2);
        }
        // :TODO: I added this, otherwise the filename could contain the middle of a long full path
        int p = string.lastIndexOf("\\");
        if (p>=0)
            string = string.substring(p+1);
        boolean skipext = false;
        /* Special checks for . and .. */
        if (string.length()>0 && string.charAt(0)=='.') {
            string=string.substring(1);
            if (string.length()==0)	{
                hasname=true;
                ret=PARSE_RET_NOWILD;
                name=".       ".getBytes();
                skipext = true;
            }
            else if (string.charAt(0)=='.' && string.length()==1)	{ // :TODO was else if (string[1]=='.' && !string[1])	{
                string="";
                hasname=true;
                ret=PARSE_RET_NOWILD;
                name = "..      ".getBytes();
                skipext = true;
            }
        } else {
            /* Copy the name */
            hasname=true;finished=false;fill=' ';index=0;
            while (index<8) {
                if (!finished && string.length()>0) {
                    if (string.charAt(0)=='*') {fill='?';name[index]='?';if (ret==0) ret=1;finished=true;}
                    else if (string.charAt(0)=='?') {name[index]='?';if (ret==0) ret=1;}
                    else if (isvalid(string.charAt(0))) {name[index]=(byte)string.toUpperCase().charAt(0);}
                    else { finished=true;continue; }
                    string=string.substring(1);
                } else {
                    name[index]=(byte)fill;
                }
                index++;
            }
            if (string.length()>0) {
                if (string.charAt(0)!='.') skipext = true;
                else string=string.substring(1);
            }
        }
        if (!skipext) {
            /* Copy the extension */
            hasext=true;finished=false;fill=' ';index=0;
            while (index<3) {
                if (!finished && string.length()>0) {
                    if (string.charAt(0)=='*') {fill='?';ext[index]='?';finished=true;}
                    else if (string.charAt(0)=='?') {ext[index]='?';if (ret==0) ret=1;}
                    else if (isvalid(string.charAt(0))) {ext[index]=(byte)string.toUpperCase().charAt(0);}
                    else { finished=true;continue; }
                    string=string.substring(1);
                } else {
                    ext[index]=(byte)fill;
                }
                index++;
            }
        }
        if (!hasdrive & (parser & PARSE_DFLT_DRIVE)==0) drive=0;
        if (!hasname & (parser & PARSE_BLNK_FNAME)==0) name="        ".getBytes();
        if (!hasext & (parser & PARSE_BLNK_FEXT)==0) ext="   ".getBytes();
        fcb.SetName((short)drive,new String(name),new String(ext));
        change.value=(short)(begin_len-string.length());
        return ret;
    }

    static private void DTAExtendName(String name,StringRef filename,StringRef ext) {
        int pos = name.indexOf('.');
        if (pos>0) {
            ext.value = name.substring(pos+1);
            name = name.substring(0,pos);
        } else ext.value = "";
        filename.value=name;
        while (filename.value.length()<8) filename.value+=" ";
        while (ext.value.length()<3) ext.value+=" ";
    }

    static private void SaveFindResult(Dos_FCB find_fcb) {
        Dos_DTA find_dta=new Dos_DTA(Dos.dos.tables.tempdta);
        StringRef name=new StringRef();/*Bit32u*/LongRef size=new LongRef(0);/*Bit16u*/IntRef date=new IntRef(0);/*Bit16u*/IntRef time=new IntRef(0);/*Bit8u*/ShortRef attr=new ShortRef();/*Bit8u*/short drive;
        StringRef file_name=new StringRef();StringRef ext=new StringRef();
        find_dta.GetResult(name,size,date,time,attr);
        drive=(short)(find_fcb.GetDrive()+1);
        /*Bit8u*/ShortRef find_attr = new ShortRef(Dos_system.DOS_ATTR_ARCHIVE);
	    find_fcb.GetAttr(find_attr); /* Gets search attributes if extended */
        /* Create a correct file and extention */
        DTAExtendName(name.value,file_name,ext);
        Dos_FCB fcb=new Dos_FCB(Memory.RealSeg(Dos.dos.dta()),Memory.RealOff(Dos.dos.dta()));//TODO
        fcb.Create(find_fcb.Extended());
        fcb.SetName(drive,file_name.value,ext.value);
        fcb.SetAttr(find_attr.value);      /* Only adds attribute if fcb is extended */
	    fcb.SetResultAttr(attr.value);
        fcb.SetSizeDateTime(size.value,date.value,time.value);
    }

    public static boolean DOS_FCBCreate(/*Bit16u*/int seg,/*Bit16u*/int offset) {
        Dos_FCB fcb=new Dos_FCB(seg,offset);
        StringRef shortname=new StringRef();/*Bit16u*/IntRef handle=new IntRef(0);
        fcb.GetName(shortname);
        ShortRef attr = new ShortRef(Dos_system.DOS_ATTR_ARCHIVE);
	    fcb.GetAttr(attr);
        if (attr.value==0) attr.value = Dos_system.DOS_ATTR_ARCHIVE; //Better safe than sorry
        if (!DOS_CreateFile(shortname.value,attr.value,handle)) return false;
        fcb.FileOpen((/*Bit8u*/short)handle.value);
        return true;
    }

    public static boolean DOS_FCBOpen(/*Bit16u*/int seg,/*Bit16u*/int offset) {
        Dos_FCB fcb=new Dos_FCB(seg,offset);
        StringRef shortname=new StringRef();/*Bit16u*/IntRef handle=new IntRef(0);
        fcb.GetName(shortname);

        /* First check if the name is correct */
        /*Bit8u*/ShortRef drive=new ShortRef();
        StringRef fullname=new StringRef();
        if (!DOS_MakeName(shortname.value,fullname,drive)) return false;

        /* Check, if file is already opened */
        for (/*Bit8u*/short i=0;i<DOS_FILES;i++) {
            Dos_PSP psp = new Dos_PSP(Dos.dos.psp());
            if (Files[i]!=null && Files[i].IsOpen() && Files[i].IsName(fullname.value)) {
                handle.value = psp.FindEntryByHandle(i);
                if (handle.value==0xFF) {
                    // This shouldnt happen
                    if (Log.level<=LogSeverities.LOG_ERROR) Log.log(LogTypes.LOG_FILES,LogSeverities.LOG_ERROR,"DOS: File "+shortname+" is opened but has no psp entry.");
                    return false;
                }
                fcb.FileOpen((/*Bit8u*/short)handle.value);
                return true;
            }
        }

        if (!DOS_OpenFile(shortname.value,OPEN_READWRITE,handle)) return false;
        fcb.FileOpen((/*Bit8u*/short)handle.value);
        return true;
    }

    public static boolean DOS_FCBClose(/*Bit16u*/int seg,/*Bit16u*/int offset) {
        Dos_FCB fcb=new Dos_FCB(seg,offset);
        if(!fcb.Valid()) return false;
        /*Bit8u*/ShortRef fhandle=new ShortRef();
        fcb.FileClose(fhandle);
        DOS_CloseFile(fhandle.value);
        return true;
    }

    public static boolean DOS_FCBFindFirst(/*Bit16u*/int seg,/*Bit16u*/int offset) {
        Dos_FCB fcb=new Dos_FCB(seg,offset);
        /*RealPt*/int old_dta=Dos.dos.dta();Dos.dos.dta((int)Dos.dos.tables.tempdta);
        StringRef name=new StringRef();fcb.GetName(name);
        /*Bit8u*/ShortRef attr = new ShortRef(Dos_system.DOS_ATTR_ARCHIVE);
        fcb.GetAttr(attr); /* Gets search attributes if extended */
        boolean ret=DOS_FindFirst(name.value,attr.value,true);
        Dos.dos.dta((int)old_dta);
        if (ret) SaveFindResult(fcb);
        return ret;
    }

    public static boolean DOS_FCBFindNext(/*Bit16u*/int seg,/*Bit16u*/int offset) {
        Dos_FCB fcb=new Dos_FCB(seg,offset);
        /*RealPt*/int old_dta=Dos.dos.dta();Dos.dos.dta((int)Dos.dos.tables.tempdta);
        boolean ret=DOS_FindNext();
        Dos.dos.dta((int)old_dta);
        if (ret) SaveFindResult(fcb);
        return ret;
    }

    public static /*Bit8u*/short DOS_FCBRead(/*Bit16u*/int seg,/*Bit16u*/int offset,/*Bit16u*/int recno) {
        Dos_FCB fcb=new Dos_FCB(seg,offset);
        /*Bit8u*/ShortRef fhandle=new ShortRef(),cur_rec=new ShortRef();/*Bit16u*/IntRef cur_block=new IntRef(0),rec_size=new IntRef(0);
        fcb.GetSeqData(fhandle,rec_size);
        if (fhandle.value==0xff && rec_size.value!=0) {
            if (!DOS_FCBOpen(seg,offset)) return FCB_READ_NODATA;
            if (Log.level<=LogSeverities.LOG_WARN) Log.log(LogTypes.LOG_FCB,LogSeverities.LOG_WARN, "Reopened closed FCB");
            fcb.GetSeqData(fhandle,rec_size);
        }
        fcb.GetRecord(cur_block,cur_rec);
        /*Bit32u*/LongRef pos=new LongRef(((cur_block.value*128)+cur_rec.value)*rec_size.value);
        if (!DOS_SeekFile(fhandle.value,pos,DOS_SEEK_SET)) return FCB_READ_NODATA;
        /*Bit16u*/IntRef toread=new IntRef(rec_size.value);
        if (!DOS_ReadFile(fhandle.value,Dos.dos_copybuf,toread)) return FCB_READ_NODATA;
        if (toread.value==0) return FCB_READ_NODATA;
        if (toread.value < rec_size.value) { //Zero pad copybuffer to rec_size
            /*Bitu*/int i = toread.value;
            while(i < rec_size.value) Dos.dos_copybuf[i++] = 0;
        }
        Memory.MEM_BlockWrite(Memory.Real2Phys(Dos.dos.dta())+recno*rec_size.value,Dos.dos_copybuf,rec_size.value);
        if (++cur_rec.value>127) { cur_block.value++;cur_rec.value=0; }
        fcb.SetRecord(cur_block.value,cur_rec.value);
        if (toread.value==rec_size.value) return FCB_SUCCESS;
        if (toread.value==0) return FCB_READ_NODATA;
        return FCB_READ_PARTIAL;
    }

    public static /*Bit8u*/short DOS_FCBWrite(/*Bit16u*/int seg,/*Bit16u*/int offset,/*Bit16u*/int recno) {
        Dos_FCB fcb=new Dos_FCB(seg,offset);
        /*Bit8u*/ShortRef fhandle=new ShortRef(),cur_rec=new ShortRef();/*Bit16u*/IntRef cur_block=new IntRef(0),rec_size=new IntRef(0);
        fcb.GetSeqData(fhandle,rec_size);
        if (fhandle.value==0xff && rec_size.value!=0) {
            if (!DOS_FCBOpen(seg,offset)) return FCB_READ_NODATA;
            if (Log.level<=LogSeverities.LOG_WARN) Log.log(LogTypes.LOG_FCB,LogSeverities.LOG_WARN, "Reopened closed FCB");
            fcb.GetSeqData(fhandle,rec_size);
        }
        fcb.GetRecord(cur_block,cur_rec);
        /*Bit32u*/LongRef pos=new LongRef(((cur_block.value*128)+cur_rec.value)*rec_size.value);
        if (!DOS_SeekFile(fhandle.value,pos,DOS_SEEK_SET)) return FCB_ERR_WRITE;
        Memory.MEM_BlockRead(Memory.Real2Phys(Dos.dos.dta())+recno*rec_size.value,Dos.dos_copybuf,rec_size.value);
        /*Bit16u*/IntRef towrite=new IntRef(rec_size.value);
        if (!DOS_WriteFile(fhandle.value,Dos.dos_copybuf,towrite)) return FCB_ERR_WRITE;
        /*Bit32u*/LongRef size=new LongRef(0);/*Bit16u*/IntRef date=new IntRef(0),time=new IntRef(0);
        fcb.GetSizeDateTime(size,date,time);
        if (pos.value+towrite.value>size.value) size.value=pos.value+towrite.value;
        //time doesn't keep track of endofday
        date.value = Dos.DOS_PackDate(Dos.dos.date.year,Dos.dos.date.month,Dos.dos.date.day);
        /*Bit32u*/long ticks = Memory.mem_readd(Bios.BIOS_TIMER) & 0xFFFFFFFFl;
        /*Bit32u*/long seconds = (ticks*10)/182;
        /*Bit16u*/int hour = (/*Bit16u*/int)(seconds/3600);
        /*Bit16u*/int min = (/*Bit16u*/int)((seconds % 3600)/60);
        /*Bit16u*/int sec = (/*Bit16u*/int)(seconds % 60);
        time.value = Dos.DOS_PackTime(hour,min,sec);
        /*Bit8u*/short temp=(short)Dos.RealHandle(fhandle.value);
        Files[temp].time=time.value;
        Files[temp].date=date.value;
        fcb.SetSizeDateTime(size.value,date.value,time.value);
        if (++cur_rec.value>127) { cur_block.value++;cur_rec.value=0; }
        fcb.SetRecord(cur_block.value,cur_rec.value);
        return FCB_SUCCESS;
    }

    public static /*Bit8u*/short DOS_FCBIncreaseSize(/*Bit16u*/int seg,/*Bit16u*/int offset) {
        Dos_FCB fcb=new Dos_FCB(seg,offset);
        /*Bit8u*/ShortRef fhandle=new ShortRef(),cur_rec=new ShortRef();/*Bit16u*/IntRef cur_block=new IntRef(0),rec_size=new IntRef(0);
        fcb.GetSeqData(fhandle,rec_size);
        fcb.GetRecord(cur_block,cur_rec);
        /*Bit32u*/LongRef pos=new LongRef(((cur_block.value*128)+cur_rec.value)*rec_size.value);
        if (!DOS_SeekFile(fhandle.value,pos,DOS_SEEK_SET)) return FCB_ERR_WRITE;
        /*Bit16u*/IntRef towrite=new IntRef(0);
        if (!DOS_WriteFile(fhandle.value,Dos.dos_copybuf,towrite)) return FCB_ERR_WRITE;
        /*Bit32u*/LongRef size=new LongRef(0);/*Bit16u*/IntRef date=new IntRef(0),time=new IntRef(0);
        fcb.GetSizeDateTime(size,date,time);
        if (pos.value+towrite.value>size.value) size.value=pos.value+towrite.value;
        //time doesn't keep track of endofday
        date.value = Dos.DOS_PackDate(Dos.dos.date.year,Dos.dos.date.month,Dos.dos.date.day);
        /*Bit32u*/long ticks = Memory.mem_readd(Bios.BIOS_TIMER) & 0xFFFFFFFFl;
        /*Bit32u*/long seconds = (ticks*10)/182;
        /*Bit16u*/int hour = (/*Bit16u*/int)(seconds/3600);
        /*Bit16u*/int min = (/*Bit16u*/int)((seconds % 3600)/60);
        /*Bit16u*/int sec = (/*Bit16u*/int)(seconds % 60);
        time.value = Dos.DOS_PackTime(hour,min,sec);
        /*Bit8u*/short temp=(short)Dos.RealHandle(fhandle.value);
        Files[temp].time=time.value;
        Files[temp].date=date.value;
        fcb.SetSizeDateTime(size.value,date.value,time.value);
        fcb.SetRecord(cur_block.value,cur_rec.value);
        return FCB_SUCCESS;
    }

    public static /*Bit8u*/short DOS_FCBRandomRead(/*Bit16u*/int seg,/*Bit16u*/int offset,/*Bit16u*/IntRef numRec,boolean restore) {
    /* if restore is true :random read else random blok read.
     * random read updates old block and old record to reflect the random data
     * before the read!!!!!!!!! and the random data is not updated! (user must do this)
     * Random block read updates these fields to reflect the state after the read!
     */

        Dos_FCB fcb=new Dos_FCB(seg,offset);
        /*Bit32u*/LongRef random=new LongRef(0);
        /*Bit16u*/IntRef old_block=new IntRef(0);
        /*Bit8u*/ShortRef old_rec=new ShortRef(0);
        /*Bit8u*/short error=0;
        /*Bit16u*/int count;

        /* Set the correct record from the random data */
        fcb.GetRandom(random);
        fcb.SetRecord((/*Bit16u*/int)(random.value / 128),(/*Bit8u*/short)(random.value & 127));
        if (restore) fcb.GetRecord(old_block,old_rec);//store this for after the read.
        // Read records
        for (count=0; count<numRec.value; count++) {
            error = DOS_FCBRead(seg,offset,count);
            if (error!=FCB_SUCCESS) break;
        }
        if (error==FCB_READ_PARTIAL) count++;	//partial read counts
        numRec.value=count;

        /*Bit16u*/IntRef new_block=new IntRef(0);/*Bit8u*/ShortRef new_rec=new ShortRef(0);
        fcb.GetRecord(new_block,new_rec);
        if (restore) fcb.SetRecord(old_block.value,old_rec.value);
        /* Update the random record pointer with new position only when restore is false*/
        if(!restore) fcb.SetRandom(new_block.value*128+new_rec.value);
        return error;
    }

    public static /*Bit8u*/short DOS_FCBRandomWrite(/*Bit16u*/int seg,/*Bit16u*/int offset,/*Bit16u*/IntRef numRec,boolean restore) {
    /* see FCB_RandomRead */
        Dos_FCB fcb=new Dos_FCB(seg,offset);
        /*Bit32u*/LongRef random = new LongRef(0);
        /*Bit16u*/IntRef old_block=new IntRef(0);
        /*Bit8u*/ShortRef old_rec=new ShortRef(0);
        /*Bit8u*/short error=0;
        /*Bit16u*/int count;

        /* Set the correct record from the random data */
        fcb.GetRandom(random);
        fcb.SetRecord((/*Bit16u*/int)(random.value / 128),(/*Bit8u*/short)(random.value & 127));
        if (restore) fcb.GetRecord(old_block,old_rec);
        if (numRec.value>0) {
            /* Write records */
            for (count=0; count<numRec.value; count++) {
                error = DOS_FCBWrite(seg,offset,count);// dos_fcbwrite return 0 false when true...
                if (error!=FCB_SUCCESS) break;
            }
            numRec.value=count;
        } else {
            DOS_FCBIncreaseSize(seg,offset);
        }
        /*Bit16u*/IntRef new_block=new IntRef(0);/*Bit8u*/ShortRef new_rec=new ShortRef(0);
        fcb.GetRecord(new_block,new_rec);
        if (restore) fcb.SetRecord(old_block.value,old_rec.value);
        /* Update the random record pointer with new position only when restore is false */
        if(!restore) fcb.SetRandom(new_block.value*128+new_rec.value);
        return error;
    }

    public static boolean DOS_FCBGetFileSize(/*Bit16u*/int seg,/*Bit16u*/int offset) {
        StringRef shortname=new StringRef();/*Bit16u*/IntRef entry=new IntRef(0);/*Bit8u*/ShortRef handle=new ShortRef();/*Bit16u*/IntRef rec_size=new IntRef(0);
        Dos_FCB fcb=new Dos_FCB(seg,offset);
        fcb.GetName(shortname);
        if (!DOS_OpenFile(shortname.value,OPEN_READ,entry)) return false;
        handle.value = (short)Dos.RealHandle(entry.value);
        /*Bit32u*/LongRef size = new LongRef(0);
        Files[handle.value].Seek(size,DOS_SEEK_END);
        DOS_CloseFile(entry.value);fcb.GetSeqData(handle,rec_size);
        /*Bit32u*/long random=(size.value/rec_size.value);
        if ((size.value % rec_size.value)!=0) random++;
        fcb.SetRandom(random);
        return true;
    }

    public static boolean DOS_FCBDeleteFile(/*Bit16u*/int seg,/*Bit16u*/int offset){
    /* FCB DELETE honours wildcards. it will return true if one or more
     * files get deleted.
     * To get this: the dta is set to temporary dta in which found files are
     * stored. This can not be the tempdta as that one is used by fcbfindfirst
     */
        /*RealPt*/int old_dta=Dos.dos.dta();Dos.dos.dta((int)Dos.dos.tables.tempdta_fcbdelete);
        /*RealPt*/int new_dta=Dos.dos.dta();
        boolean nextfile = false;
        boolean return_value = false;
        nextfile = DOS_FCBFindFirst(seg,offset);
        Dos_FCB fcb=new Dos_FCB(Memory.RealSeg(new_dta),Memory.RealOff(new_dta));
        while(nextfile) {
            StringRef shortname = new StringRef();
            fcb.GetName(shortname);
            boolean res=DOS_UnlinkFile(shortname.value);
            if(!return_value && res) return_value = true; //at least one file deleted
            nextfile = DOS_FCBFindNext(seg,offset);
        }
        Dos.dos.dta((int)old_dta);  /*Restore dta */
        return  return_value;
    }

    public static boolean DOS_FCBRenameFile(/*Bit16u*/int seg, /*Bit16u*/int offset){
        Dos_FCB fcbold=new Dos_FCB(seg,offset);
        Dos_FCB fcbnew=new Dos_FCB(seg,offset+16);
        if(!fcbold.Valid()) return false;
        StringRef oldname=new StringRef();
        StringRef newname = new StringRef();
        fcbold.GetName(oldname);fcbnew.GetName(newname);

        /* Check, if sourcefile is still open. This was possible in DOS, but modern oses don't like this */
        ShortRef drive = new ShortRef(0); StringRef fullname = new StringRef();
        if (!DOS_MakeName(oldname.value,fullname,drive)) return false;

        Dos_PSP psp = new Dos_PSP(Dos.dos.psp());
        for (short i=0;i<DOS_FILES;i++) {
            if (Files[i]!=null && Files[i].IsOpen() && Files[i].IsName(fullname.value)) {
                int handle = psp.FindEntryByHandle(i);
                if (handle == 0xFF) {
                    // This shouldnt happen
                    if (Log.level<=LogSeverities.LOG_ERROR) Log.log(LogTypes.LOG_FILES,LogSeverities.LOG_ERROR,"DOS: File "+oldname.value+" is opened but has no psp entry.");
                    return false;
                }
                DOS_CloseFile(handle);
            }
        }
        /* Rename the file */
        return DOS_Rename(oldname.value,newname.value);
    }

    public static void DOS_FCBSetRandomRecord(/*Bit16u*/int seg, /*Bit16u*/int offset) {
        Dos_FCB fcb=new Dos_FCB(seg,offset);
        /*Bit16u*/IntRef block=new IntRef(0);/*Bit8u*/ShortRef rec=new ShortRef();
        fcb.GetRecord(block,rec);
        fcb.SetRandom(block.value*128+rec.value);
    }


    public static boolean DOS_FileExists(String name) {
        StringRef fullname=new StringRef();/*Bit8u*/ShortRef drive=new ShortRef();
        if (!DOS_MakeName(name,fullname,drive)) return false;
        return Drives[drive.value].FileExists(fullname.value);
    }

    public static boolean DOS_GetAllocationInfo(/*Bit8u*/short drive,/*Bit16u*/IntRef _bytes_sector,/*Bit8u*/ShortRef _sectors_cluster,/*Bit16u*/IntRef _total_clusters) {
        if (drive==0) drive = DOS_GetDefaultDrive();
        else drive--;
        if (drive >= DOS_DRIVES || Drives[drive]==null) {
            Dos.DOS_SetError(Dos.DOSERR_INVALID_DRIVE);
            return false;
        }
        /*Bit16u*/IntRef _free_clusters=new IntRef(0);
        Drives[drive].AllocationInfo(_bytes_sector,_sectors_cluster,_total_clusters,_free_clusters);
        CPU_Regs.SegSet16DS(Memory.RealSeg(Dos.dos.tables.mediaid));
        CPU_Regs.reg_ebx.word(Memory.RealOff(Dos.dos.tables.mediaid+drive*2));
        return true;
    }

    public static boolean DOS_SetDrive(/*Bit8u*/short drive) {
        if (Drives[drive]!=null) {
            DOS_SetDefaultDrive(drive);
            return true;
        } else {
            return false;
        }
    }

    public static boolean DOS_GetFileDate(/*Bit16u*/int entry, /*Bit16u*/IntRef otime, /*Bit16u*/IntRef odate) {
        /*Bit32u*/int handle=Dos.RealHandle(entry);
        if (handle>=DOS_FILES) {
            Dos.DOS_SetError(Dos.DOSERR_INVALID_HANDLE);
            return false;
        }
        if (Files[handle]==null || !Files[handle].IsOpen()) {
            Dos.DOS_SetError(Dos.DOSERR_INVALID_HANDLE);
            return false;
        }
        if (!Files[handle].UpdateDateTimeFromHost()) {
            Dos.DOS_SetError(Dos.DOSERR_INVALID_HANDLE);
            return false;
        }
        otime.value = Files[handle].time;
        odate.value = Files[handle].date;
        return true;
    }

    public static void DOS_SetupFiles () {
        /* Setup the File Handles */
        /*Bit32u*/int i;
        for (i=0;i<DOS_FILES;i++) {
            Files[i]=null;
        }
        /* Setup the Virtual Disk System */
        for (i=0;i<DOS_DRIVES;i++) {
            Drives[i]=null;
        }
        Drives[25]=new Drive_virtual();
    }

}
